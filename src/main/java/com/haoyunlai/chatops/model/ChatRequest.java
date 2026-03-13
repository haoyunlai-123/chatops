package com.haoyunlai.chatops.model;

public record ChatRequest(
        String message,  // 用户输入的自然语言指令
        String userId    // 模拟从网关透传过来的当前登录用户ID
) {}