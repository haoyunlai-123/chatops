package com.haoyunlai.chatops.tools.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class RouterToolService {

    @Tool(description = "查询用户是否登录。")
    public String queryLogin(@ToolParam(description = "用户id") String userid) {
        log.info("查询用户登录状态，UserId: {}", userid);
        return String.format("用户ID [%s] 的登录状态: 已登录", userid);
    }

    @Tool(description = "查询用户是否有操作权限")
    public String queryUserInfo(@ToolParam(description = "用户ID") String userId
            , @ToolParam(description = "操作类型,0为后端业务操作，1为系统监控操作，2为调度中心操作") String operation
    ) {
        log.info("查询用户权限，UserId: {}, Operation: {}", userId, operation);
        return String.format("用户ID [%s] 的权限状态: 具有操作权限", userId);
    }
}