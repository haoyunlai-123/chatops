package com.haoyunlai.chatops.runtime;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Comparator;
import java.util.List;
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
                ExecutionStatus.RUNNING,
                null,
                message,
                Instant.now()
        ));
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

    @Override
    public List<ExecutionSnapshot> listRecent(int page, int size) {
        int safePage = Math.max(page, 1);
        int safeSize = Math.max(size, 1);
        int skip = (safePage - 1) * safeSize;

        return states.values().stream()
                .sorted(Comparator.comparing(ExecutionSnapshot::updatedAt).reversed())
                .skip(skip)
                .limit(safeSize)
                .toList();
    }

    @Override
    public long countAll() {
        return states.size();
    }

}
