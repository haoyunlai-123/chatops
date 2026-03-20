package com.haoyunlai.chatops.agent;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Component;

@RequiredArgsConstructor
@Slf4j
@Component
public class ScheduleWorkerAgent {

    private final ChatClient scheduleChatClient;


    public String execute(String taskInstruction) {
        log.info("🤖 [Schedule-Agent] 正在执行子任务: {}", taskInstruction);
        return scheduleChatClient.prompt()
                .user(taskInstruction)
                .call()
                .content();
    }
}