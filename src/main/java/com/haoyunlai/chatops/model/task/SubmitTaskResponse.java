package com.haoyunlai.chatops.model.task;

public record SubmitTaskResponse(
        String executionId,
        String status,
        String message
) {
}
