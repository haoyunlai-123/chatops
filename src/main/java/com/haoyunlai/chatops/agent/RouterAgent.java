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
     * 1. 生成计划 (新增 out 回调，实时推送思考过程)
     */
    public ExecutionPlan generatePlan(String userMessage, java.util.function.Consumer<String> out) {
        out.accept("🧠 [Router-Agent] 正在思考并分析用户意图...");
        log.info("🧠 开始分析用户意图...");

        org.springframework.ai.converter.BeanOutputConverter<ExecutionPlan> converter =
                new org.springframework.ai.converter.BeanOutputConverter<>(ExecutionPlan.class);
        String formatInstruction = converter.getFormat();

        String jsonResult = routerChatClient.prompt()
                .user(prompt -> prompt
                        .text("用户指令: {message}\n\n请分析并输出执行计划。必须严格遵循 JSON 格式：\n{format}")
                        .param("message", userMessage)
                        .param("format", formatInstruction)
                )
                .call()
                .content();

        ExecutionPlan plan = converter.convert(jsonResult);
        out.accept(String.format("📋 意图拆解完成: [%s]，共包含 %d 个步骤。", plan.intent(), plan.steps().size()));
        return plan;
    }

    /**
     * 2. 初次执行计划
     */
    public void executePlan(ExecutionPlan plan, java.util.function.Consumer<String> out) {
        runSteps(plan, 0, new StringBuilder("【全局上下文】\n"), out);
    }

    /**
     * 3. 审批通过后，恢复执行计划
     */
    public void resumePlan(String token, java.util.function.Consumer<String> out) {
        SuspendedContext context = suspendCache.remove(token);
        if (context == null) {
            out.accept("❌ 审批 Token 无效或已过期！");
            return;
        }
        out.accept(String.format("🔓 [HITL] 管理员审批通过！唤醒挂起的任务流，继续执行第 %d 步...", context.nextStepIndex() + 1));
        runSteps(context.plan(), context.nextStepIndex(), context.globalContext(), out);
    }

    /**
     * 4. 核心状态机驱动引擎 (加入实时 out 推送)
     */
    private void runSteps(ExecutionPlan plan, int startIndex, StringBuilder globalContext, java.util.function.Consumer<String> out) {
        if (!plan.isSafe()) {
            out.accept("🚨 风控拦截！指令存在高危风险。原因: " + plan.riskReason());
            return;
        }

        for (int i = startIndex; i < plan.steps().size(); i++) {
            Step step = plan.steps().get(i);

            // 【HITL 挂起逻辑】
            if (step.requireApproval()) {
                String suspendToken = "HITL-" + UUID.randomUUID().toString().substring(0, 8);
                out.accept(String.format("⏸️ 警告：步骤 %d [%s] 属于高危写操作！", step.order(), step.instruction()));
                out.accept("⏳ 流程已挂起，等待人工审批。");
                out.accept("🔑 审批凭证: " + suspendToken); // 前端可以根据这个凭证渲染一个“同意”按钮

                plan.steps().set(i, new Step(step.order(), step.targetAgent(), step.instruction(), false));
                suspendCache.put(suspendToken, new SuspendedContext(plan, i, globalContext, new ArrayList<>()));
                return; // 直接退出引擎，等待唤醒
            }

            // 【常规执行逻辑】
            out.accept(String.format("➡️ 正在执行步骤 %d: [%s] -> %s", step.order(), step.targetAgent(), step.instruction()));

            String basePrompt = String.format("【你的任务】: %s\n\n%s", step.instruction(), globalContext.toString());
            int maxRetries = 3;
            int currentRetry = 0;
            boolean stepSuccess = false;
            String workerResult = "";
            String lastErrorMessage = "";

            // 【反思重试逻辑】
            while (currentRetry < maxRetries && !stepSuccess) {
                try {
                    String dynamicPrompt = basePrompt;
                    if (currentRetry > 0) {
                        out.accept(String.format("🔄 捕获到业务异常 [%s]，正在进行第 %d 次自我反思与重试...", lastErrorMessage, currentRetry));
                        dynamicPrompt = String.format("⚠️ 上次执行失败！报错：[%s]\n请反思并修正参数重新执行：\n%s", lastErrorMessage, basePrompt);
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
                        out.accept("❌ 步骤 " + step.order() + " 经过多次反思重试依然失败。中止整个任务链路。");
                        return; // 彻底失败，退出引擎
                    }
                }
            }

            out.accept(String.format("✅ 步骤 %d 成功: %s", step.order(), workerResult));
            globalContext.append(String.format("- 步骤 %d 结果: %s\n", step.order(), workerResult));
        }

        out.accept("🎉 所有指令已拆解并完美执行完毕！");
    }
}