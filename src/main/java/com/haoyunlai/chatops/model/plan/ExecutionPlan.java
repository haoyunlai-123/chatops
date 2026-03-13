package com.haoyunlai.chatops.model.plan;

import java.util.List;

public record ExecutionPlan(
        boolean isSafe,        // 安全风控：是否为安全的指令 (防删库跑路)
        String riskReason,     // 如果不安全，拒绝的原因
        String intent,         // 一句话总结用户的整体意图
        List<Step> steps       // 拆解出来的步骤链 (DAG的线性表示)
) {}