package com.haoyunlai.chatops.runtime;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
@ConditionalOnMissingBean(ExecutionStateStore.class)
public class InMemoryExecutionStateStore implements ExecutionStateStore {

    private final Map<String, ExecutionSnapshot> states = new ConcurrentHashMap<>();

    @Override
    public void init(String executionId, String userMessage) {
        states.put(executionId, new ExecutionSnapshot(
                executionId,
                userMessage,
                null,
                0,
                0,
                ExecutionStatus.PENDING,
                null,
                "任务已创建，等待执行",
                Instant.now()
        ));
    }

    @Override
    public void markRunning(String executionId, String intent, int totalSteps, int currentStep, String message) {
        upsert(executionId, ExecutionStatus.RUNNING, intent, totalSteps, currentStep, null, message);
    }

    @Override
    public void markSuspended(String executionId, int currentStep, String approvalToken, String message) {
        ExecutionSnapshot old = states.get(executionId);
        if (old == null) {
            return;
        }
        states.put(executionId, new ExecutionSnapshot(
                old.executionId(),
                old.userMessage(),
                old.intent(),
                old.totalSteps(),
                currentStep,
                ExecutionStatus.SUSPENDED,
                approvalToken,
                message,
                Instant.now()
        ));
    }

    @Override
    public void markBlocked(String executionId, String message) {
        ExecutionSnapshot old = states.get(executionId);
        if (old == null) {
            return;
        }
        states.put(executionId, new ExecutionSnapshot(
                old.executionId(),
                old.userMessage(),
                old.intent(),
                old.totalSteps(),
                old.currentStep(),
                ExecutionStatus.BLOCKED,
                old.approvalToken(),
                message,
                Instant.now()
        ));
    }

    @Override
    public void markFailed(String executionId, int currentStep, String message) {
        ExecutionSnapshot old = states.get(executionId);
        if (old == null) {
            return;
        }
        states.put(executionId, new ExecutionSnapshot(
                old.executionId(),
                old.userMessage(),
                old.intent(),
                old.totalSteps(),
                currentStep,
                ExecutionStatus.FAILED,
                old.approvalToken(),
                message,
                Instant.now()
        ));
    }

    @Override
    public void markSucceeded(String executionId, int totalSteps, String message) {
        ExecutionSnapshot old = states.get(executionId);
        if (old == null) {
            return;
        }
        states.put(executionId, new ExecutionSnapshot(
                old.executionId(),
                old.userMessage(),
                old.intent(),
                totalSteps,
                totalSteps,
                ExecutionStatus.SUCCEEDED,
                old.approvalToken(),
                message,
                Instant.now()
        ));
    }

    @Override
    public ExecutionSnapshot get(String executionId) {
        return states.get(executionId);
    }

    private void upsert(String executionId,
                        ExecutionStatus status,
                        String intent,
                        int totalSteps,
                        int currentStep,
                        String approvalToken,
                        String message) {
        ExecutionSnapshot old = states.get(executionId);
        if (old == null) {
            return;
        }
        states.put(executionId, new ExecutionSnapshot(
                old.executionId(),
                old.userMessage(),
                intent != null ? intent : old.intent(),
                totalSteps > 0 ? totalSteps : old.totalSteps(),
                currentStep,
                status,
                approvalToken,
                message,
                Instant.now()
        ));
    }
}
