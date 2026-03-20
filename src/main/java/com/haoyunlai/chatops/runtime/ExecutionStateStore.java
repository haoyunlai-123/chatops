package com.haoyunlai.chatops.runtime;

public interface ExecutionStateStore {

    void init(String executionId, String userMessage);

    void markRunning(String executionId, String intent, int totalSteps, int currentStep, String message);

    void markSuspended(String executionId, int currentStep, String approvalToken, String message);

    void markBlocked(String executionId, String message);

    void markFailed(String executionId, int currentStep, String message);

    void markSucceeded(String executionId, int totalSteps, String message);

    ExecutionSnapshot get(String executionId);
}
