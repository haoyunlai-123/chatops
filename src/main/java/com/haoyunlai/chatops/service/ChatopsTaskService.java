package com.haoyunlai.chatops.service;

import com.haoyunlai.chatops.agent.RouterAgent;
import com.haoyunlai.chatops.model.ChatRequest;
import com.haoyunlai.chatops.model.plan.ExecutionPlan;
import com.haoyunlai.chatops.runtime.ExecutionStateStore;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChatopsTaskService {

    private final RouterAgent routerAgent;
    private final ExecutionStateStore executionStateStore;

    public String submitTask(ChatRequest request) {
        String executionId = "EXE-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        executionStateStore.init(executionId, request.message());

        CompletableFuture.runAsync(() -> executeTask(executionId, request.message()));
        return executionId;
    }

    public void approveTask(String token) {
        CompletableFuture.runAsync(() -> {
            Consumer<String> out = this::logProgress;
            try {
                routerAgent.resumePlan(token, out);
            } catch (Exception e) {
                log.error("💥 审批恢复任务异常, token={}", token, e);
            }
        });
    }

    private void executeTask(String executionId, String userMessage) {
        Consumer<String> out = this::logProgress;
        try {
            ExecutionPlan plan = routerAgent.generatePlan(userMessage, out);
            routerAgent.executePlan(executionId, plan, out);
        } catch (Exception e) {
            log.error("💥 执行任务异常, executionId={}", executionId, e);
            executionStateStore.markFailed(executionId, 0, "执行异常: " + e.getMessage());
        }
    }

    private void logProgress(String message) {
        log.info("[TASK] {}", message);
    }
}
