package com.haoyunlai.chatops.controller;

import com.haoyunlai.chatops.agent.RouterAgent;
import com.haoyunlai.chatops.model.ChatRequest;
import com.haoyunlai.chatops.model.ChatResponse;
import com.haoyunlai.chatops.model.plan.ExecutionPlan;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/chatops")
public class ChatAgentController {

    private final RouterAgent routerAgent;

    public ChatAgentController(RouterAgent routerAgent) {
        this.routerAgent = routerAgent;
    }

    /**
     * 接收用户的自然语言管控指令
     */
    @PostMapping("/execute")
    public ChatResponse executeCommand(@RequestBody ChatRequest request) {
        log.info("📩 收到用户请求: [UserID: {}], 消息: {}", request.userId(), request.message());

        try {
            // 第一步：调用大脑 (RouterAgent) 拆解意图，生成 DAG 执行计划
            ExecutionPlan plan = routerAgent.generatePlan(request.message());

            // 第二步：根据计划，把任务派发给各个 Worker Agent 执行
            List<String> executionLogs = routerAgent.executePlan(plan);

            // 第三步：汇总结果返回给前端 (后续可让 LLM 再做一次自然语言总结)
            String finalReply = plan.isSafe()
                    ? "🎉 指令已拆解并执行完毕，请查看详细日志与计划树。"
                    : "🚨 风控拦截！指令存在高危风险，已拒绝执行。";

            return new ChatResponse(finalReply, plan, executionLogs);

        } catch (Exception e) {
            log.error("💥 执行过程中发生异常", e);
            return new ChatResponse("系统异常: " + e.getMessage(), null, List.of(e.getMessage()));
        }
    }
}