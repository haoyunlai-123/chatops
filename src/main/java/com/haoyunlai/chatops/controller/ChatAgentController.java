package com.haoyunlai.chatops.controller;

import com.haoyunlai.chatops.agent.RouterAgent;
import com.haoyunlai.chatops.model.ChatRequest;
import com.haoyunlai.chatops.model.plan.ExecutionPlan;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

@Slf4j
@RestController
@RequestMapping("/api/chatops")
public class ChatAgentController {

    private final RouterAgent routerAgent;

    public ChatAgentController(RouterAgent routerAgent) {
        this.routerAgent = routerAgent;
    }

    /**
     * 1. 提交管控指令 (流式输出 SSE)
     */
    @PostMapping(value = "/execute", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter executeCommandStream(@RequestBody ChatRequest request) {
        log.info("📩 收到用户请求: {}", request.message());

        // 设置超时时间为 10 分钟 (防止复杂的 Agent 思考太久断连)
        SseEmitter emitter = new SseEmitter(10 * 60 * 1000L);

        // 异步执行 Agent 逻辑，不阻塞 Tomcat 主线程
        CompletableFuture.runAsync(() -> {
            try {
                // 定义一个发送消息到前端的回调函数
                Consumer<String> out = msg -> sendToEmitter(emitter, msg);

                // 第一步：大脑拆解意图
                ExecutionPlan plan = routerAgent.generatePlan(request.message(), out);

                // 第二步：执行状态机
                routerAgent.executePlan(plan, out);

                // 执行结束，关闭流
                emitter.complete();
            } catch (Exception e) {
                log.error("💥 执行流发生异常", e);
                sendToEmitter(emitter, "💥 系统异常: " + e.getMessage());
                emitter.completeWithError(e);
            }
        });

        return emitter;
    }

    /**
     * 2. 人工审批放行接口 (流式输出 SSE)
     */
    @GetMapping(value = "/approve", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter approveExecutionStream(@RequestParam String token) {
        log.info("🔑 收到人工审批唤醒请求，Token: {}", token);

        SseEmitter emitter = new SseEmitter(10 * 60 * 1000L);

        CompletableFuture.runAsync(() -> {
            try {
                Consumer<String> out = msg -> sendToEmitter(emitter, msg);
                // 唤醒大脑继续执行
                routerAgent.resumePlan(token, out);
                emitter.complete();
            } catch (Exception e) {
                sendToEmitter(emitter, "💥 恢复流发生异常: " + e.getMessage());
                emitter.completeWithError(e);
            }
        });

        return emitter;
    }

    /**
     * 封装的发送工具方法
     */
    private void sendToEmitter(SseEmitter emitter, String message) {
        try {
            // 将普通文本包装为 SSE 事件发送
            emitter.send(SseEmitter.event().data(message + "\n\n"));
            // 顺便在控制台也打印一份，方便你本地看
            log.info("🌐 [SSE 推送] {}", message);
        } catch (IOException e) {
            log.warn("⚠️ 客户端已断开连接");
        }
    }
}