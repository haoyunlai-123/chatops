package com.haoyunlai.chatops.tools.service;


import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Slf4j
@Service
public class MonitorToolService {

    /**
     * @Tool 注解的 description 就是给大模型的 Prompt。必须写清楚作用和参数含义！
     */
    @Tool(description = "查询指定系统组件的健康状态和运行指标。可用组件包括：网关(gateway)、调度中心(scheduler)、业务服务(business)。也可用于获取系统的全链路TraceId。")
    public String checkSystemHealth(String componentName) {
        log.info("🛠[Monitor-Tool] 收到监控查询请求，目标组件: {}", componentName);
        // TODO: 后期替换为调用网关/Eureka/ZK的真实接口
        String mockTraceId = "TRACE-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        return String.format("组件 [%s] 状态正常 (UP)。CPU: 45%%, Memory: 60%%, 节点熔断状态: 正常。全链路追踪 TraceId: %s",
                componentName, mockTraceId);
    }
}