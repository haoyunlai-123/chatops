package com.haoyunlai.chatops.service;

import com.haoyunlai.chatops.agent.RouterAgent;
import com.haoyunlai.chatops.model.ChatRequest;
import com.haoyunlai.chatops.model.plan.ExecutionPlan;
import com.haoyunlai.chatops.runtime.ExecutionStatus;
import com.haoyunlai.chatops.runtime.ExecutionSnapshot;
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
        return submitTaskByMessage(request.message());
    }

    public String retryTask(String sourceExecutionId) {
        ExecutionSnapshot source = executionStateStore.get(sourceExecutionId);
        if (source == null) {
            throw new IllegalArgumentException("源任务不存在: " + sourceExecutionId);
        }
        if (source.status() == null || source.status() == ExecutionStatus.RUNNING || source.status() == ExecutionStatus.PENDING) {
            throw new IllegalStateException("当前任务仍在执行中，不能重试: " + sourceExecutionId);
        }
        return submitTaskByMessage(source.userMessage());
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

    private String submitTaskByMessage(String userMessage) {
        String executionId = "EXE-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        executionStateStore.init(executionId, userMessage);
        CompletableFuture.runAsync(() -> executeTask(executionId, userMessage));
        return executionId;
    }

    private void logProgress(String message) {
        log.info("[TASK] {}", message);
    }
}
