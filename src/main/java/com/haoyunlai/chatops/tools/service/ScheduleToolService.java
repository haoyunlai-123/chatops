package com.haoyunlai.chatops.tools.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class ScheduleToolService {

    @Tool(description = "在分布式调度中心创建一个定时任务(Cron Job)。必须提供任务名称(jobName)、Cron表达式(cron)以及要调用的目标服务(targetService)。")
    public String createScheduleJob(String jobName, String cron, String targetService) {
        log.info("🛠️ [Schedule-Tool] 收到创建定时任务请求: 任务名={}, Cron={}, 目标={}", jobName, cron, targetService);
        // TODO: 后期替换为向 Job 库插入记录并唤醒调度机
        Long mockJobId = (long) (Math.random() * 10000);
        return String.format("调度任务 [%s] 创建成功！JobId: %d，已进入 WAITING 状态机队列等待触发。", jobName, mockJobId);
    }
}