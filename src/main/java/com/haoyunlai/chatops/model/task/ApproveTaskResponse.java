package com.haoyunlai.chatops.model.task;

public record ApproveTaskResponse(
        String token,
        String status,
        String message
) {
}
