package com.haoyunlai.chatops.tools.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class ScheduleToolService {

    @Tool(description = "在分布式调度中心创建一个定时任务(Cron Job)。必须提供任务名称(jobName)、Cron表达式(cron)以及要调用的目标服务(targetService)。")
    public String createScheduleJob(
            @ToolParam(description = "定时任务名称") String jobName,
            @ToolParam(description = "任务类型，fixedRate 或 cron") String jobType,
            @ToolParam(description = "任务间隔时间，单位为毫秒") String expression,
            @ToolParam(description = "定时任务参数，即要调用的任务url,如：\"handlerParam\": \"{\\\"url\\\":\\\"http://127.0.0.1:9002/health\\\",\\\"method\\\":\\\"GET\\\"}\"") String targetService
    ) {
        log.info("🛠️ [Schedule-Tool] 收到创建定时任务请求: 任务名={}, 任务类型：{}, 时间间隔={}, 参数={}", jobName, jobType,  expression, targetService);
        // TODO: 后期替换为向 Job 库插入记录并唤醒调度机
        Long mockJobId = (long) (Math.random() * 10000);
        return String.format("调度任务 [%s] 创建成功！JobId: %d，已进入 WAITING 状态机队列等待触发。", jobName, mockJobId);
    }

    @Tool(description = "向分布式调度中心触发一次定时任务,提供任务ID(jobId)即可。")
    public String triggerScheduleJob(
            @ToolParam(description = "定时任务ID") Long jobId
    ) {
        log.info("🛠️ [Schedule-Tool] 收到触发定时任务请求: JobId={}", jobId);
        return String.format("调度任务 JobId [%d] 已成功触发！", jobId);
    }
}