package com.haoyunlai.chatops.agent;

import com.haoyunlai.chatops.model.ChatResponse;
import com.haoyunlai.chatops.model.plan.ExecutionPlan;
import com.haoyunlai.chatops.model.plan.Step;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.converter.BeanOutputConverter;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@RequiredArgsConstructor
@Slf4j
@Component
public class RouterAgent {

    private final ChatClient routerChatClient; // 大总管的专用 ChatClient (不绑定具体工具，只负责分析和路由)
    private final MonitorWorkerAgent monitorAgent;
    private final BusinessWorkerAgent businessAgent;
    private final ScheduleWorkerAgent scheduleAgent;
    private final TroubleshootWorkerAgent troubleshootAgent;

    private final Map<String, SuspendedContext> suspendCache = new ConcurrentHashMap<>();


    // 【新增】：挂起上下文的数据结构
    public record SuspendedContext(
            ExecutionPlan plan,
            int nextStepIndex,
            StringBuilder globalContext,
            List<String> executionLogs
    ) {}

    /**
     * 核心编排方法
     */
    public ExecutionPlan generatePlan(String userMessage) {
        log.info("🧠 [Router-Agent] 开始分析用户意图并生成执行计划...");

        // 1. 定义期望大模型返回的 Java 类型 (Spring AI 的黑魔法)
        BeanOutputConverter<ExecutionPlan> converter = new BeanOutputConverter<>(ExecutionPlan.class);
        String formatInstruction = converter.getFormat(); // 获取强制输出 JSON 的 Prompt

        // 2. 调用大模型进行拆解
        String jsonResult = routerChatClient.prompt()
                .user(prompt -> prompt
                        .text("用户指令: {message}\n\n请分析指令并输出执行计划。必须严格遵循以下 JSON 格式：\n{format}")
                        .param("message", userMessage)
                        .param("format", formatInstruction)
                )
                .call()
                .content();

        // 3. 将大模型返回的 JSON 字符串反序列化为 Java 对象
        ExecutionPlan plan = converter.convert(jsonResult);
        log.info("📋 [Router-Agent] 意图拆解完成: \n{}", plan);
        return plan;
    }

    /**
     * 初次执行计划 (从第 0 步开始)
     */
    public ChatResponse executePlan(ExecutionPlan plan) {
        return runSteps(plan, 0, new StringBuilder("【全局上下文 / 前置步骤执行结果】\n"), new ArrayList<>());
    }

    /**
     * 审批通过后，恢复执行计划 (从断点步骤开始)
     */
    public ChatResponse resumePlan(String token) {
        SuspendedContext context = suspendCache.remove(token); // 从缓存/Redis中取出并删除
        if (context == null) {
            return new ChatResponse("❌ 审批 Token 无效或已过期！", null, List.of("恢复失败：无效的 Token"));
        }
        log.info("🔓 [HITL] 管理员审批通过！正在唤醒挂起的任务流，继续执行第 {} 步...", context.nextStepIndex() + 1);

        List<String> logs = context.executionLogs();
        logs.add("🔓 [HITL-Resume] 管理员已授权，挂起状态解除，恢复执行...");

        return runSteps(context.plan(), context.nextStepIndex(), context.globalContext(), logs);
    }

    /**
     * 核心驱动引擎：支持从任意 Step 索引开始执行 (状态机核心)
     */
    private ChatResponse runSteps(ExecutionPlan plan, int startIndex, StringBuilder globalContext, List<String> executionLogs) {
        if (!plan.isSafe()) {
            return new ChatResponse("🚨 风控拦截！指令存在高危风险。", plan, List.of("原因: " + plan.riskReason()));
        }

        // 从 startIndex 开始遍历执行 (断点续传)
        for (int i = startIndex; i < plan.steps().size(); i++) {
            Step step = plan.steps().get(i);

            // ==========================================
            // HITL 拦截机制：发现需要审批，立刻挂起！
            // ==========================================
            if (step.requireApproval()) {
                String suspendToken = "HITL-" + UUID.randomUUID().toString().substring(0, 8);
                String suspendLog = String.format("⏸️ 步骤 %d [%s] 属于高危操作，流程已挂起。等待人工审批...", step.order(), step.instruction());

                log.warn(suspendLog);
                executionLogs.add(suspendLog);

                // 将当前的所有状态打包存入缓存 (模拟存入 Redis)
                // 注意：nextStepIndex 传入的是当前的 i，因为这一步还没执行，等唤醒时还要执行它
                // 但为了安全，将该步骤的 requireApproval 强制改为 false，防止无限挂起
                plan.steps().set(i, new Step(step.order(), step.targetAgent(), step.instruction(), false)); // 伪代码逻辑，实际通过修改 Record 实现

                // 由于 Record 不可变，我们需要新建一个替换进去
                plan.steps().set(i, new Step(step.order(), step.targetAgent(), step.instruction(), false));

                suspendCache.put(suspendToken, new SuspendedContext(plan, i, globalContext, executionLogs));

                String finalReply = String.format("⚠️ 任务执行到第 %d 步时触发安全风控机制被挂起。\n操作内容: %s\n请管理员审核。审批凭证: %s",
                        step.order(), step.instruction(), suspendToken);

                return new ChatResponse(finalReply, plan, executionLogs); // 直接返回给前端，退出引擎
            }

            // --- 下面是反思重试机制 ---
            String stepLog = String.format("执行步骤 %d: [%s] -> %s", step.order(), step.targetAgent(), step.instruction());
            log.info("➡️ " + stepLog);
            executionLogs.add(stepLog);

            String basePrompt = String.format("【你的任务】: %s\n\n%s\n\n请调用工具完成。", step.instruction(), globalContext.toString());

            int maxRetries = 3;
            int currentRetry = 0;
            boolean stepSuccess = false;
            String workerResult = "";
            String lastErrorMessage = "";

            while (currentRetry < maxRetries && !stepSuccess) {
                try {
                    String dynamicPrompt = basePrompt;
                    if (currentRetry > 0) {
                        dynamicPrompt = String.format("⚠️ 警告：你上一次执行失败！报错：[%s]\n请反思并修正参数，重新执行：\n%s", lastErrorMessage, basePrompt);
                    }
                    switch (step.targetAgent().toUpperCase()) {
                        case "MONITOR" -> workerResult = monitorAgent.execute(dynamicPrompt);
                        case "BUSINESS" -> workerResult = businessAgent.execute(dynamicPrompt);
                        case "SCHEDULE" -> workerResult = scheduleAgent.execute(dynamicPrompt);
                        case "TROUBLESHOOT" -> workerResult = troubleshootAgent.execute(dynamicPrompt);
                        default -> throw new IllegalArgumentException("未知 Agent: " + step.targetAgent());
                    }
                    stepSuccess = true;
                } catch (Exception e) {
                    currentRetry++;
                    lastErrorMessage = e.getMessage() != null ? e.getMessage() : e.toString();
                    if (currentRetry >= maxRetries) {
                        executionLogs.add("❌ 步骤 " + step.order() + " 重试失败，中止链路。");
                        return new ChatResponse("执行失败，请查看日志。", plan, executionLogs); // 失败直接退出
                    }
                }
            }

            String successLog = "✅ 步骤 " + step.order() + " 成功: " + workerResult;
            log.info(successLog);
            executionLogs.add(successLog);
            globalContext.append(String.format("- 步骤 %d 结果: %s\n", step.order(), workerResult));
        }

        // 整个循环跑完，说明全部成功
        return new ChatResponse("🎉 所有指令已拆解并完美执行完毕！", plan, executionLogs);
    }
}