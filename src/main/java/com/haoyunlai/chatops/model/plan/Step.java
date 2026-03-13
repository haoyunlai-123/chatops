package com.haoyunlai.chatops.model.plan;

public record Step(
        int order,             // 执行顺序 (1, 2, 3...)
        String targetAgent,    // 目标处理 Agent (可选值: MONITOR, BUSINESS, SCHEDULE)
        String instruction     // 派发给子 Agent 的具体指令 (例如: "请调用监控工具查询网关组件健康状态")
) {}