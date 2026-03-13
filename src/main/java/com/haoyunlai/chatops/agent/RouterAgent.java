package com.haoyunlai.chatops.agent;

import com.haoyunlai.chatops.model.plan.ExecutionPlan;
import com.haoyunlai.chatops.model.plan.Step;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.converter.BeanOutputConverter;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@RequiredArgsConstructor
@Slf4j
@Component
public class RouterAgent {

    private final ChatClient routerChatClient; // 大总管的专用 ChatClient (不绑定具体工具，只负责分析和路由)
    private final MonitorWorkerAgent monitorAgent;
    private final BusinessWorkerAgent businessAgent;
    private final ScheduleWorkerAgent scheduleAgent;

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
     * 根据计划按顺序调用对应的 Worker Agent (引入 Blackboard 上下文共享模式)
     */
    public List<String> executePlan(ExecutionPlan plan) {
        List<String> executionLogs = new ArrayList<>();

        if (!plan.isSafe()) {
            String warnMsg = "🚨 [风控拦截] 指令拒绝执行。原因: " + plan.riskReason();
            log.warn(warnMsg);
            executionLogs.add(warnMsg);
            return executionLogs;
        }

        log.info("🚀 [Router-Agent] 开始路由任务，共 {} 个步骤...", plan.steps().size());

        // 【核心新增】：全局上下文黑板，用于串联各个孤立的 Worker Agent
        StringBuilder globalContext = new StringBuilder("【全局上下文 / 前置步骤执行结果】\n");

        for (Step step : plan.steps()) {
            String stepLog = String.format("执行步骤 %d: [%s] -> %s", step.order(), step.targetAgent(), step.instruction());
            log.info("➡️ " + stepLog);
            executionLogs.add(stepLog);

            // 动态构建发给 Worker 的 Prompt：将用户的原始指令 + 前面步骤的执行结果一起发过去
            String dynamicPrompt = String.format("""
                    【你的任务】: %s
                    
                    %s
                    
                    请结合上述【全局上下文】中的信息(如需要)，调用你的工具完成【你的任务】。
                    """, step.instruction(), globalContext.toString());

            String workerResult = "";
            try {
                // 根据大模型指定的 Target 路由到具体的 Agent，传入增强后的 dynamicPrompt
                switch (step.targetAgent().toUpperCase()) {
                    case "MONITOR" -> workerResult = monitorAgent.execute(dynamicPrompt);
                    case "BUSINESS" -> workerResult = businessAgent.execute(dynamicPrompt);
                    case "SCHEDULE" -> workerResult = scheduleAgent.execute(dynamicPrompt);
                    default -> workerResult = "未知的目标 Agent: " + step.targetAgent();
                }

                String successLog = "✅ 步骤 " + step.order() + " 返回结果: " + workerResult;
                log.info(successLog);
                executionLogs.add(successLog);

                // 【核心新增】：将当前 Worker 的执行结果写入黑板，供下一个 Worker 使用
                globalContext.append(String.format("- 步骤 %d (%s) 结果: %s\n",
                        step.order(), step.targetAgent(), workerResult));

            } catch (Exception e) {
                String errorLog = "❌ 步骤 " + step.order() + " 执行失败: " + e.getMessage();
                log.error(errorLog, e);
                executionLogs.add(errorLog);
                break; // 中断后续链路
            }
        }
        return executionLogs;
    }
}