package com.haoyunlai.chatops.agent;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Component;

@RequiredArgsConstructor
@Slf4j
@Component
public class MonitorWorkerAgent {

    private final ChatClient monitorChatClient;


    public String execute(String taskInstruction) {
        log.info("🤖 [Monitor-Agent] 正在执行子任务: {}", taskInstruction);
        return monitorChatClient.prompt()
                .user(taskInstruction)
                .call()
                .content();
    }
}