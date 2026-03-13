package com.haoyunlai.chatops.agent;

import com.haoyunlai.chatops.tools.service.MonitorToolService;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Component;

@RequiredArgsConstructor
@Component
public class MonitorWorkerAgent {

    private final ChatClient monitorChatClient;


    public String execute(String taskInstruction) {
        System.out.println("🤖 [Monitor-Agent] 正在执行子任务: " + taskInstruction);
        return monitorChatClient.prompt()
                .user(taskInstruction)
                .call()
                .content();
    }
}