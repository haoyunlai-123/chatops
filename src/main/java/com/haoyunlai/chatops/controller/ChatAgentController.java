package com.haoyunlai.chatops.controller;

import com.haoyunlai.chatops.agent.RouterAgent;
import com.haoyunlai.chatops.model.ChatRequest;
import com.haoyunlai.chatops.model.ChatResponse;
import com.haoyunlai.chatops.model.plan.ExecutionPlan;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/chatops")
public class ChatAgentController {

    private final RouterAgent routerAgent;

    public ChatAgentController(RouterAgent routerAgent) {
        this.routerAgent = routerAgent;
    }

    /**
     * 1. 提交管控指令
     */
    @PostMapping("/execute")
    public ChatResponse executeCommand(@RequestBody ChatRequest request) {
        log.info("📩 收到用户请求: [UserID: {}], 消息: {}", request.userId(), request.message());
        try {
            // 生成计划
            ExecutionPlan plan = routerAgent.generatePlan(request.message());
            // 扔给状态机引擎执行 (可能会直接完成，也可能会返回挂起状态)
            return routerAgent.executePlan(plan);
        } catch (Exception e) {
            log.error("💥 执行期间发生异常", e);
            return new ChatResponse("系统异常: " + e.getMessage(), null, null);
        }
    }

    /**
     * 2. 【核心新增】人工审批放行接口 (恢复挂起的任务)
     */
    @GetMapping("/approve")
    public ChatResponse approveExecution(@RequestParam String token) {
        log.info("🔑 收到人工审批请求，Token: {}", token);
        try {
            // 将 Token 交给大脑，大脑会从缓存中恢复上下文并继续执行
            return routerAgent.resumePlan(token);
        } catch (Exception e) {
            log.error("💥 唤醒恢复期间发生异常", e);
            return new ChatResponse("恢复异常: " + e.getMessage(), null, null);
        }
    }
}