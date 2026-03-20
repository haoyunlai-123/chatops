package com.haoyunlai.chatops.runtime;

import java.time.Instant;

/**
 *  用于保存每次执行的快照信息，方便后续查询和展示
 * @param executionId
 * @param userMessage
 * @param intent
 * @param totalSteps
 * @param currentStep
 * @param status
 * @param approvalToken
 * @param lastMessage
 * @param updatedAt
 */
public record ExecutionSnapshot(
        String executionId,
        String userMessage,
        String intent,
        int totalSteps,
        int currentStep,
        ExecutionStatus status,
        String approvalToken,
        String lastMessage,
        Instant updatedAt
) {
}
