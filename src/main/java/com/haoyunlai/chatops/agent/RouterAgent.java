package com.haoyunlai.chatops.agent;

import com.haoyunlai.chatops.model.plan.ExecutionPlan;
import com.haoyunlai.chatops.model.plan.Step;
import com.haoyunlai.chatops.runtime.ExecutionSnapshot;
import com.haoyunlai.chatops.runtime.ExecutionStateStore;
import com.haoyunlai.chatops.runtime.ExecutionStatus;
import com.haoyunlai.chatops.runtime.SuspensionContext;
import com.haoyunlai.chatops.runtime.SuspensionStore;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.converter.BeanOutputConverter;
import org.springframework.stereotype.Component;

import java.util.Objects;
import java.util.UUID;

@RequiredArgsConstructor
@Slf4j
@Component
public class RouterAgent {

    private final ChatClient routerChatClient; // 大总管的专用 ChatClient (不绑定具体工具，只负责分析和路由)
    private final MonitorWorkerAgent monitorAgent;
    private final BusinessWorkerAgent businessAgent;
    private final ScheduleWorkerAgent scheduleAgent;
    private final TroubleshootWorkerAgent troubleshootAgent;
    private final ExecutionStateStore executionStateStore;
    private final SuspensionStore suspensionStore;

    /**
     * 1. 生成计划 (新增 out 回调，实时推送思考过程)
     */
    public ExecutionPlan generatePlan(String userMessage, java.util.function.Consumer<String> out) {
        out.accept("🧠 [Router-Agent] 正在思考并分析用户意图...");
        log.info("🧠 开始分析用户意图...");

        BeanOutputConverter<ExecutionPlan> converter = new BeanOutputConverter<>(ExecutionPlan.class);
        String formatInstruction = converter.getFormat();

        String jsonResult = routerChatClient.prompt()
                .user(prompt -> prompt
                        .text("用户指令: {message}\n\n请分析并输出执行计划。必须严格遵循 JSON 格式：\n{format}")
                        .param("message", userMessage)
                        .param("format", formatInstruction)
                )
                .call()
                .content();

        ExecutionPlan plan = converter.convert(Objects.requireNonNull(jsonResult, "Router planner returned null"));
        out.accept(String.format("📋 意图拆解完成: [%s]，共包含 %d 个步骤。", plan.intent(), plan.steps().size()));
        return plan;
    }

    /**
     * 2. 初次执行计划
     */
    public void executePlan(String executionId, ExecutionPlan plan, java.util.function.Consumer<String> out) {
        executionStateStore.markRunning(executionId, plan.intent(), plan.steps().size(), 0, "执行中");
        runSteps(executionId, plan, 0, new StringBuilder("【全局上下文】\n"), out);
    }

    /**
     * 3. 审批通过后，恢复执行计划
     */
    public void resumePlan(String token, java.util.function.Consumer<String> out) {
        SuspensionContext context = suspensionStore.remove(token);
        if (context == null) {
            out.accept("❌ 审批 Token 无效或已过期！");
            return;
        }
        ExecutionSnapshot snapshot = executionStateStore.get(context.executionId());
        if (snapshot != null && snapshot.status() == ExecutionStatus.CANCELED) {
            out.accept("🚫 当前任务已被取消，审批恢复已忽略。");
            return;
        }
        executionStateStore.markRunning(
                context.executionId(),
                context.plan().intent(),
                context.plan().steps().size(),
                context.nextStepIndex(),
                "审批通过，恢复执行"
        );
        out.accept(String.format("🔓 [HITL] 管理员审批通过！唤醒挂起的任务流，继续执行第 %d 步...", context.nextStepIndex() + 1));
        runSteps(context.executionId(), context.plan(), context.nextStepIndex(), new StringBuilder(context.globalContext()), out);
    }

    /**
     * 4. 核心状态机驱动引擎 (加入实时 out 推送)
     */
    private void runSteps(String executionId, ExecutionPlan plan, int startIndex, StringBuilder globalContext, java.util.function.Consumer<String> out) {
        if (!plan.isSafe()) {
            executionStateStore.markBlocked(executionId, "风控拦截: " + plan.riskReason());
            out.accept("🚨 风控拦截！指令存在高危风险。原因: " + plan.riskReason());
            return;
        }

        for (int i = startIndex; i < plan.steps().size(); i++) {
            ExecutionSnapshot snapshot = executionStateStore.get(executionId);
            if (snapshot != null && snapshot.status() == ExecutionStatus.CANCELED) {
                out.accept("🚫 检测到任务已取消，终止后续步骤执行。");
                return;
            }

            Step step = plan.steps().get(i);

            // 【HITL 挂起逻辑】
            if (step.requireApproval()) {
                String suspendToken = "HITL-" + UUID.randomUUID().toString().substring(0, 8);
                out.accept(String.format("⏸️ 警告：步骤 %d [%s] 属于高危写操作！", step.order(), step.instruction()));
                out.accept("⏳ 流程已挂起，等待人工审批。");
                out.accept("🔑 审批凭证: " + suspendToken); // 前端可以根据这个凭证渲染一个“同意”按钮

                plan.steps().set(i, new Step(step.order(), step.targetAgent(), step.instruction(), false));
                suspensionStore.save(suspendToken, new SuspensionContext(executionId, plan, i, globalContext.toString()));
                executionStateStore.markSuspended(executionId, i, suspendToken, "等待人工审批");
                return; // 直接退出引擎，等待唤醒
            }

            // 【常规执行逻辑】
            out.accept(String.format("➡️ 正在执行步骤 %d: [%s] -> %s", step.order(), step.targetAgent(), step.instruction()));
            executionStateStore.markRunning(executionId, plan.intent(), plan.steps().size(), i + 1, "执行步骤 " + step.order());

            String basePrompt = String.format("【你的任务】: %s\n\n%s", step.instruction(), globalContext.toString());
            int maxRetries = 3;
            boolean stepSuccess = false;
            String workerResult = "";
            String lastErrorMessage = "";

            // 【反思重试逻辑】
            for (int currentRetry = 0; currentRetry < maxRetries && !stepSuccess; currentRetry++) {
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
                    lastErrorMessage = e.getMessage() != null ? e.getMessage() : e.toString();
                    if (currentRetry >= maxRetries - 1) {
                        executionStateStore.markFailed(executionId, i + 1, "步骤失败: " + lastErrorMessage);
                        out.accept("❌ 步骤 " + step.order() + " 经过多次反思重试依然失败。中止整个任务链路。");
                        return; // 彻底失败，退出引擎
                    }
                }
            }

            out.accept(String.format("✅ 步骤 %d 成功: %s", step.order(), workerResult));
            globalContext.append(String.format("- 步骤 %d 结果: %s\n", step.order(), workerResult));
        }

        executionStateStore.markSucceeded(executionId, plan.steps().size(), "所有步骤执行完成");
        out.accept("🎉 所有指令已拆解并完美执行完毕！");
    }
}