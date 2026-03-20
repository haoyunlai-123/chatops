package com.haoyunlai.chatops.model.task;

import com.haoyunlai.chatops.runtime.ExecutionSnapshot;

import java.util.List;

public record TaskPageResponse(
        int page,
        int size,
        long total,
        List<ExecutionSnapshot> items
) {
}
