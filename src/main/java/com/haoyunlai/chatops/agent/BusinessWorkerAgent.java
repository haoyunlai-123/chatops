package com.haoyunlai.chatops.agent;

import com.haoyunlai.chatops.tools.service.BusinessToolService;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Component;

@RequiredArgsConstructor
@Component
public class BusinessWorkerAgent {

    private final ChatClient businessChatClient;

    public String execute(String taskInstruction) {
        System.out.println("🤖 [Business-Agent] 正在执行子任务: " + taskInstruction);
        return businessChatClient.prompt()
                .user(taskInstruction)
                .call()
                .content();
    }
}