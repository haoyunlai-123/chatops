package com.haoyunlai.chatops.tools.service;


import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Slf4j
@Service
public class MonitorToolService {

    /**
     * @Tool 注解的 description 就是给大模型的 Prompt。必须写清楚作用和参数含义！
     */
    @Tool(description = "查询指定系统组件的健康状态和运行指标。可用组件包括：网关(gateway)、调度中心(scheduler)、业务服务(business)。也可用于获取系统的全链路TraceId。")
    public String checkSystemHealth(@ToolParam(description = "目标组件名称") String componentName) {
        log.info("🛠[Monitor-Tool] 收到监控查询请求，目标组件: {}", componentName);
        // TODO: 后期替换为调用网关/Eureka/ZK的真实接口
        String mockTraceId = "TRACE-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        return String.format("组件 [%s] 状态正常 (UP)。CPU: 45%%, Memory: 60%%, 节点熔断状态: 正常。全链路追踪 TraceId: %s",
                componentName, mockTraceId);
    }

    @Tool(description = "监控系统健康，目标组件为网关(gateway)、调度中心(scheduler)、业务服务(business)等。")
    public String checkSystemHealthV2(@ToolParam(description = "目标组件名称") String componentName) {
        log.info("🛠[Monitor-Tool] 收到监控查询请求，目标组件: {}", componentName);
        return String.format("组件 [%s] 状态正常 (UP)。CPU: 45%%, Memory: 60%%, 节点熔断状态: 正常。全链路追踪 TraceId: %s",
                componentName, "TRACE-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());
    }

    @Tool(description = "查询系统全链路TraceId，便于在日志系统中追踪请求流转。")
    public String getTraceId() {
        log.info("🛠[Monitor-Tool] 收到获取TraceId请求");
        return "TRACE-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }

    @Tool(description = "传入链路TraceId，返回全链路执行日志")
    public String getTraceLogs(@ToolParam(description = "全链路TraceId") String traceId) {
        log.info("🛠[Monitor-Tool] 收到获取Trace日志请求，TraceId: {}", traceId);
        return String.format("TraceId [%s] 的全链路日志：\n1. 2024-06-01 10:00:00 - 网关接收请求，TraceId: %s\n2. 2024-06-01 10:00:01 - 调度中心处理请求，TraceId: %s\n3. 2024-06-01 10:00:02 - 业务服务执行操作，TraceId: %s\n4. 2024-06-01 10:00:03 - 请求完成，TraceId: %s",
                traceId, traceId, traceId, traceId, traceId);
    }

}