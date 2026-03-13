package com.haoyunlai.chatops.agent;

import com.haoyunlai.chatops.tools.service.ScheduleToolService;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Component;

@RequiredArgsConstructor
@Component
public class ScheduleWorkerAgent {

    private final ChatClient scheduleChatClient;


    public String execute(String taskInstruction) {
        System.out.println("🤖 [Schedule-Agent] 正在执行子任务: " + taskInstruction);
        return scheduleChatClient.prompt()
                .user(taskInstruction)
                .call()
                .content();
    }
}