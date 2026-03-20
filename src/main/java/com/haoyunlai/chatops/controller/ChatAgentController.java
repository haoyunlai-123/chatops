package com.haoyunlai.chatops.controller;

import com.haoyunlai.chatops.model.ChatRequest;
import com.haoyunlai.chatops.model.task.ApproveTaskRequest;
import com.haoyunlai.chatops.model.task.ApproveTaskResponse;
import com.haoyunlai.chatops.model.task.SubmitTaskResponse;
import com.haoyunlai.chatops.runtime.ExecutionSnapshot;
import com.haoyunlai.chatops.runtime.ExecutionStateStore;
import com.haoyunlai.chatops.service.ChatopsTaskService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@Slf4j
@RestController
@RequestMapping("/api/chatops")
public class ChatAgentController {

    private final ChatopsTaskService chatopsTaskService;
    private final ExecutionStateStore executionStateStore;

    public ChatAgentController(ChatopsTaskService chatopsTaskService, ExecutionStateStore executionStateStore) {
        this.chatopsTaskService = chatopsTaskService;
        this.executionStateStore = executionStateStore;
    }

    /**
     * 1) 提交任务 (异步)
     */
    @PostMapping("/tasks")
    public SubmitTaskResponse submitTask(@RequestBody ChatRequest request) {
        log.info("📩 收到异步任务提交: userId={}, message={}", request.userId(), request.message());
        String executionId = chatopsTaskService.submitTask(request);
        return new SubmitTaskResponse(executionId, "ACCEPTED", "任务已提交，正在后台执行");
    }

    /**
     * 2) 查询任务状态
     */
    @GetMapping("/tasks/{executionId}")
    public ExecutionSnapshot getTaskStatus(@PathVariable String executionId) {
        ExecutionSnapshot snapshot = executionStateStore.get(executionId);
        if (snapshot == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "执行任务不存在: " + executionId);
        }
        return snapshot;
    }

    /**
     * 3) 审批任务 (异步恢复执行)
     */
    @PostMapping("/tasks/approve")
    public ApproveTaskResponse approveTask(@RequestBody ApproveTaskRequest request) {
        if (request == null || request.token() == null || request.token().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "token 不能为空");
        }

        log.info("🔑 收到异步审批请求, token={}", request.token());
        chatopsTaskService.approveTask(request.token());
        return new ApproveTaskResponse(request.token(), "ACCEPTED", "审批请求已接收，任务将异步恢复执行");
    }
}