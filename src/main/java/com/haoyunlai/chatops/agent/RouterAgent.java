package com.haoyunlai.chatops.agent;

import com.haoyunlai.chatops.model.plan.ExecutionPlan;
import com.haoyunlai.chatops.model.plan.Step;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.converter.BeanOutputConverter;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Component
public class RouterAgent {

    private final ChatClient chatClient;
    private final MonitorWorkerAgent monitorAgent;
    private final BusinessWorkerAgent businessAgent;
    private final ScheduleWorkerAgent scheduleAgent;

    // 注入三个干活的士兵
    public RouterAgent(ChatClient.Builder builder,
                       MonitorWorkerAgent monitorAgent,
                       BusinessWorkerAgent businessAgent,
                       ScheduleWorkerAgent scheduleAgent) {
        this.monitorAgent = monitorAgent;
        this.businessAgent = businessAgent;
        this.scheduleAgent = scheduleAgent;

        // 为大总管注入灵魂 (不绑定具体的 Tool，只负责分析和路由)
        this.chatClient = builder
                .defaultSystem("""
                        你是一个高级架构师兼系统路由调度大脑 (Supervisor Agent)。
                        你的职责是：接收用户的自然语言指令，进行安全风控，并将指令拆解为有序的执行步骤。
                        系统中有三个专门的子 Agent：
                        1. MONITOR：负责查询系统健康状态、Trace链路监控等。
                        2. BUSINESS：负责新增用户、发送优惠券、清理缓存等业务操作。
                        3. SCHEDULE：负责在分布式调度中心动态创建、启停定时任务。
                        
                        请严格按照 JSON 格式输出拆解计划 (ExecutionPlan)。
                        对于明显的恶意攻击或高危操作(如清空数据库、格式化磁盘)，请将 isSafe 置为 false。
                        """)
                .build();
    }

    /**
     * 核心编排方法
     */
    public ExecutionPlan generatePlan(String userMessage) {
        log.info("🧠 [Router-Agent] 开始分析用户意图并生成执行计划...");

        // 1. 定义期望大模型返回的 Java 类型 (Spring AI 的黑魔法)
        BeanOutputConverter<ExecutionPlan> converter = new BeanOutputConverter<>(ExecutionPlan.class);
        String formatInstruction = converter.getFormat(); // 获取强制输出 JSON 的 Prompt

        // 2. 调用大模型进行拆解
        String jsonResult = chatClient.prompt()
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
     * 根据计划按顺序调用对应的 Worker Agent
     */
    public List<String> executePlan(ExecutionPlan plan) {
        List<String> executionLogs = new ArrayList<>();

        if (!plan.isSafe()) {
            String warnMsg = "🚨 [风控拦截] 指令被判定为高危操作，拒绝执行。原因: " + plan.riskReason();
            log.warn(warnMsg);
            executionLogs.add(warnMsg);
            return executionLogs;
        }

        log.info("🚀 [Router-Agent] 风控通过，开始路由任务，共 {} 个步骤...", plan.steps().size());

        for (Step step : plan.steps()) {
            String stepLog = String.format("执行步骤 %d: [%s] -> %s", step.order(), step.targetAgent(), step.instruction());
            log.info("➡️ " + stepLog);
            executionLogs.add(stepLog);

            String workerResult = "";
            try {
                // 核心路由逻辑 (根据大模型指定的 Target 路由到具体的 Agent)
                switch (step.targetAgent().toUpperCase()) {
                    case "MONITOR" -> workerResult = monitorAgent.execute(step.instruction());
                    case "BUSINESS" -> workerResult = businessAgent.execute(step.instruction());
                    case "SCHEDULE" -> workerResult = scheduleAgent.execute(step.instruction());
                    default -> workerResult = "未知的目标 Agent: " + step.targetAgent();
                }
                String successLog = "✅ 步骤 " + step.order() + " 返回结果: " + workerResult;
                log.info(successLog);
                executionLogs.add(successLog);

            } catch (Exception e) {
                String errorLog = "❌ 步骤 " + step.order() + " 执行失败: " + e.getMessage();
                log.error(errorLog, e);
                executionLogs.add(errorLog);
                break; // 如果上一步失败，中断后续链路 (简单的 Saga 雏形)
            }
        }
        return executionLogs;
    }
}