package com.haoyunlai.chatops.agent;

import com.haoyunlai.chatops.tools.service.ScheduleToolService;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Component;

@Component
public class ScheduleWorkerAgent {

    private final ChatClient chatClient;

    public ScheduleWorkerAgent(ChatClient.Builder builder, ScheduleToolService scheduleToolService) {
        this.chatClient = builder
                .defaultSystem("""
                        你是一个分布式任务调度中心管理员 (Schedule Agent)。
                        你的职责是在系统中动态创建、启停、删除定时任务。
                        严格根据用户的 Cron 表达式和意图，调用相应的工具配置调度中心。
                        """)
                // 绑定调度工具
                .defaultTools(scheduleToolService)  // 直接传实例
                // .defaultTools("scheduleToolService")  // 或者用 Bean 名称
                .build();
    }

    public String execute(String taskInstruction) {
        System.out.println("🤖 [Schedule-Agent] 正在执行子任务: " + taskInstruction);
        return chatClient.prompt()
                .user(taskInstruction)
                .call()
                .content();
    }
}