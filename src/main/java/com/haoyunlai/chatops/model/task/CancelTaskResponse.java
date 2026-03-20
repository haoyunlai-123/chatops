package com.haoyunlai.chatops.model.task;

public record CancelTaskResponse(
        String executionId,
        String status,
        String message
) {
}
