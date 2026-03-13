package com.haoyunlai.chatops.model.plan;

public record Step(
        int order,             // 执行顺序 (1, 2, 3...)
        String targetAgent,    // 目标处理 Agent (可选值: MONITOR, BUSINESS, SCHEDULE, TROUBLESHOOT)
        String instruction,    // 派发给子 Agent 的具体指令
        boolean requireApproval // 【核心新增】是否需要人工审批 (高危写操作必须为 true)
) {}