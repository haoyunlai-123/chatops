package com.haoyunlai.chatops.agent;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Component;

@RequiredArgsConstructor
@Slf4j
@Component
public class BusinessWorkerAgent {

    private final ChatClient businessChatClient;

    public String execute(String taskInstruction) {
        log.info("🤖 [Business-Agent] 正在执行子任务: {}", taskInstruction);
        return businessChatClient.prompt()
                .user(taskInstruction)
                .call()
                .content();
    }
}