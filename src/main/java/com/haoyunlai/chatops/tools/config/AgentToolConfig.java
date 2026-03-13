package com.haoyunlai.chatops.tools.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Description;

import java.util.UUID;
import java.util.function.Function;

/**
 * 智能体工具注册中心 (模拟 MCP Server 暴露的 Tools)
 * 这里的每一个 @Bean 都会被注册为一个可供大模型调用的 Function
 */
@Slf4j
@Configuration
public class AgentToolConfig {

    // ==========================================
    // 1. 监控中心 (Monitor) Tools
    // ==========================================
    public record SystemHealthRequest(String componentName) {}
    public record SystemHealthResponse(String status, String details, String traceId) {}

    @Bean
    @Description("查询指定系统组件的健康状态和运行指标。可用组件包括：网关(gateway)、调度中心(scheduler)、业务服务(business)。也可用于获取系统的全链路TraceId。")
    public Function<SystemHealthRequest, SystemHealthResponse> checkSystemHealth() {
        return request -> {
            log.info("🛠 [Monitor-Tool] 收到监控查询请求，目标组件: {}", request.componentName());
            // TODO: 后期这里替换为调用你真实网关的被动健康检查 API 或 Promethus 接口
            String mockTraceId = "TRACE-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
            return new SystemHealthResponse("UP", "CPU: 45%, Memory: 60%, 节点熔断状态: 正常", mockTraceId);
        };
    }

    // ==========================================
    // 2. 业务服务 (Business) Tools
    // ==========================================
    public record AddUserRequest(String username, String role, boolean needCoupon) {}
    public record AddUserResponse(boolean success, String userId, String message) {}

    @Bean
    @Description("新增业务系统用户。如果用户需要抢优惠券，请将 needCoupon 设置为 true。")
    public Function<AddUserRequest, AddUserResponse> addBusinessUser() {
        return request -> {
            log.info("🛠 [Business-Tool] 收到新增用户请求: 姓名={}, 角色={}, 是否发券={}",
                    request.username(), request.role(), request.needCoupon());
            // TODO: 后期替换为通过自研 RPC 框架泛化调用真实的微服务接口
            String mockUserId = "UID-" + System.currentTimeMillis();
            String msg = request.needCoupon() ? "用户创建成功，已触发异步发券逻辑(结合Kafka)" : "用户创建成功";
            return new AddUserResponse(true, mockUserId, msg);
        };
    }

    public record DeleteCacheRequest(String cacheKeyPattern) {}
    public record DeleteCacheResponse(boolean success, String message) {}

    @Bean
    @Description("清理或删除 Redis 业务缓存。危险操作！通常在修复数据不一致时调用。")
    public Function<DeleteCacheRequest, DeleteCacheResponse> deleteRedisCache() {
        return request -> {
            log.info("🛠 [Business-Tool] 收到清理缓存请求，Key正则: {}", request.cacheKeyPattern());
            return new DeleteCacheResponse(true, "缓存清理指令已下发至各个节点");
        };
    }


    // ==========================================
    // 3. 调度中心 (Schedule) Tools
    // ==========================================
    public record CreateJobRequest(String jobName, String cronExpression, String targetServiceClass) {}
    public record CreateJobResponse(boolean success, Long jobId, String status) {}

    @Bean
    @Description("在分布式调度中心创建一个定时任务(Cron Job)。必须提供任务名称、Cron表达式以及要调用的目标接口全类名。")
    public Function<CreateJobRequest, CreateJobResponse> createScheduleJob() {
        return request -> {
            log.info("🛠 [Schedule-Tool] 收到创建定时任务请求: 任务名={}, Cron={}, 目标类={}",
                    request.jobName(), request.cronExpression(), request.targetServiceClass());
            // TODO: 后期替换为向 Job/JobInstance 数据库插入记录，并唤醒调度线程
            Long mockJobId = (long) (Math.random() * 10000);
            return new CreateJobResponse(true, mockJobId, "任务已创建并进入 WAITING 状态机");
        };
    }
}