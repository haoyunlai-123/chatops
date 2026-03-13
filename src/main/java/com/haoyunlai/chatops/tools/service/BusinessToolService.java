package com.haoyunlai.chatops.tools.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class BusinessToolService {

    @Tool(description = "新增业务系统用户。如果用户需要参与抢优惠券活动，请将 needCoupon 设置为 true。")
    public String addBusinessUser(
            @ToolParam(description = "用户姓名") String username,
            @ToolParam(description = "用户角色") String role,
            @ToolParam(description = "是否发卷") boolean needCoupon
    ) {
        log.info("🛠️ [Business-Tool] 收到新增用户请求: 姓名={}, 角色={}, 是否发券={}", username, role, needCoupon);
        // TODO: 后期替换为 RPC 泛化调用真实微服务
        String mockUserId = "UID-" + System.currentTimeMillis();
        if (needCoupon) {
            return String.format("成功创建用户 [%s], ID为 [%s]。并已触发异步发券逻辑(Saga事务已开启)。", username, mockUserId);
        }
        return String.format("成功创建用户 [%s], ID为 [%s]。", username, mockUserId);
    }

    @Tool(description = "清理或删除 Redis 业务缓存。此为危险操作！仅在修复数据不一致或系统维护时调用。")
    public String deleteRedisCache(@ToolParam(description = "redis缓存key") String cacheKey) {
        log.info("🛠️ [Business-Tool] 收到清理缓存请求，Key: {}", cacheKey);
        log.info("🚮 删除缓存：{}", cacheKey);
        return String.format("已成功向缓存集群下发清理指令，", cacheKey);
    }

    @Tool(description = "查询用户信息。提供用户ID(userId)即可获取用户的基本信息和状态。")
    public String queryUserInfo(@ToolParam(description = "用户ID") String userId) {
        log.info("🛠️ [Business-Tool] 收到查询用户信息请求，UserId: {}", userId);
        return String.format("查询结果：用户ID [%s]，姓名: 张三，角色: 普通用户，状态: 活跃", userId);
    }
}