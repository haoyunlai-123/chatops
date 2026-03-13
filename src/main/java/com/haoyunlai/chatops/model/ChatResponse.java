package com.haoyunlai.chatops.model;

import com.haoyunlai.chatops.model.plan.ExecutionPlan;
import java.util.List;

public record ChatResponse(
        String finalReply,       // 大模型最后总结给用户的话
        ExecutionPlan plan,      // 大模型拆解出来的执行计划 (方便前端展示DAG图)
        List<String> logs        // 执行过程中的详细日志
) {}