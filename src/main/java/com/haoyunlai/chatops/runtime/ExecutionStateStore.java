package com.haoyunlai.chatops.runtime;

import java.util.List;

public interface ExecutionStateStore {

    void init(String executionId, String userMessage);

    void init(String executionId, String userMessage, int retryCount, String sourceExecutionId, String rootExecutionId);

    void markRunning(String executionId, String intent, int totalSteps, int currentStep, String message);

    void markSuspended(String executionId, int currentStep, String approvalToken, String message);

    void markBlocked(String executionId, String message);

    void markFailed(String executionId, int currentStep, String message);

    void markSucceeded(String executionId, int totalSteps, String message);

    boolean markCanceled(String executionId, String message);

    ExecutionSnapshot get(String executionId);

    List<ExecutionSnapshot> listRecent(int page, int size);

    long countAll();
}
