package com.haoyunlai.chatops.runtime;

import com.haoyunlai.chatops.model.plan.ExecutionPlan;

public record SuspensionContext(
        String executionId,
        ExecutionPlan plan,
        int nextStepIndex,
        String globalContext
) {
}
