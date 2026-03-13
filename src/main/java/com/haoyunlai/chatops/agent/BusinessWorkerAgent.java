package com.haoyunlai.chatops.agent;

import com.haoyunlai.chatops.tools.service.BusinessToolService;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Component;

@Component
public class BusinessWorkerAgent {

    private final ChatClient chatClient;

    public BusinessWorkerAgent(ChatClient.Builder builder, BusinessToolService businessToolService) {
        this.chatClient = builder
                .defaultSystem("""
                        你是一个高级业务系统管控专家 (Business Agent)。
                        你的职责是执行关于用户、缓存、优惠券等后端业务系统操作。
                        你可以通过调用提供的工具来改变系统状态。
                        在执行删除/清理等危险操作时，务必在返回结果中提示已完成安全校验。
                        """)
                // 绑定业务工具
                .defaultTools(businessToolService)  // 直接传实例（推荐）
                // .defaultTools("businessToolService")  // 或者用 Bean 名称（
                .build();
    }

    public String execute(String taskInstruction) {
        System.out.println("🤖 [Business-Agent] 正在执行子任务: " + taskInstruction);
        return chatClient.prompt()
                .user(taskInstruction)
                .call()
                .content();
    }
}