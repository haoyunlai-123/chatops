package com.haoyunlai.chatops.agent;

import com.haoyunlai.chatops.tools.service.MonitorToolService;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Component;

@Component
public class MonitorWorkerAgent {

    private final ChatClient chatClient;

    public MonitorWorkerAgent(ChatClient.Builder builder, MonitorToolService monitorToolService) {
        this.chatClient = builder
                .defaultSystem("""
            你是一个专业的系统监控与运维专家 (Monitor Agent)。
            你的职责是调用系统监控工具获取健康状态和链路日志。
            请根据用户的具体指令，调用相应的工具，并用简洁专业的运维话术返回结果。
            不要回答与系统监控无关的问题。
            """)
                // 【核心修改】这里改成 defaultTools，并传入包含 @Tool 注解方法的 Bean 实例（或 Bean name）
                .defaultTools(monitorToolService)  // ← 直接传实例（推荐）
                // 或者用 Bean 名称（如果 monitorToolService 是 @Bean 且有 @Tool 方法）
                // .defaultTools("monitorToolService")
                .build();
    }

    public String execute(String taskInstruction) {
        System.out.println("🤖 [Monitor-Agent] 正在执行子任务: " + taskInstruction);
        return chatClient.prompt()
                .user(taskInstruction)
                .call()
                .content();
    }
}