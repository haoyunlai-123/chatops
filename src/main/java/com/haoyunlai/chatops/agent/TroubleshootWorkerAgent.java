package com.haoyunlai.chatops.agent;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Component;

@RequiredArgsConstructor
@Slf4j
@Component
public class TroubleshootWorkerAgent {

    private final ChatClient troubleshootChatClient;


    public String execute(String taskInstruction) {
        log.info("🤖 [Troubleshoot-Agent] 正在进行系统诊断/RAG检索: {}", taskInstruction);
        return troubleshootChatClient.prompt()
                .user(taskInstruction)
                .call()
                .content();
    }
}