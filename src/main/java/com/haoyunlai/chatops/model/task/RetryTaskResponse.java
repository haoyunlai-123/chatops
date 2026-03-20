package com.haoyunlai.chatops.model.task;

public record RetryTaskResponse(
        String sourceExecutionId,
        String newExecutionId,
        String status,
        String message
) {
}
