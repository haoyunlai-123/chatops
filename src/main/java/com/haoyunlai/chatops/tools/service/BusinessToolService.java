package com.haoyunlai.chatops.tools.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class BusinessToolService {

    @Tool(description = "新增业务系统用户。如果用户需要参与抢优惠券活动，请将 needCoupon 设置为 true。")
    public String addBusinessUser(String username, String role, boolean needCoupon) {
        log.info("🛠️ [Business-Tool] 收到新增用户请求: 姓名={}, 角色={}, 是否发券={}", username, role, needCoupon);
        // TODO: 后期替换为 RPC 泛化调用真实微服务
        String mockUserId = "UID-" + System.currentTimeMillis();
        if (needCoupon) {
            return String.format("成功创建用户 [%s], ID为 [%s]。并已触发异步发券逻辑(Saga事务已开启)。", username, mockUserId);
        }
        return String.format("成功创建用户 [%s], ID为 [%s]。", username, mockUserId);
    }

    @Tool(description = "清理或删除 Redis 业务缓存。此为危险操作！仅在修复数据不一致或系统维护时调用。")
    public String deleteRedisCache(String cacheKeyPattern) {
        log.info("🛠️ [Business-Tool] 收到清理缓存请求，Key正则: {}", cacheKeyPattern);
        return String.format("已成功向缓存集群下发清理指令，匹配模式: %s", cacheKeyPattern);
    }
}