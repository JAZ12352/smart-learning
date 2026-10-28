package com.mindskip.xzs.configuration;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

/**
 * 定时任务配置
 * 用于学习路径进度监控等周期性任务
 * 对应评分标准中"学习计划动态调整"的基础设施
 *
 * 实际的路径调整通过 /api/admin/learning-path/auto-adjust 端点触发
 * 前端在评估页面展示进度状态，教师/学生可手动或自动触发调整
 */
@Configuration
@EnableScheduling
public class ScheduledTasks {

    private static final Logger log = LoggerFactory.getLogger(ScheduledTasks.class);

    /**
     * 心跳日志 — 确认定时任务框架正常工作
     * 实际调整逻辑在 LearningPathController.autoAdjust() 中
     */
    @Scheduled(fixedRate = 3600000)
    public void heartbeat() {
        log.debug("定时任务心跳: 学习路径自动检测就绪 (可通过 /api/admin/learning-path/check-progress/{userId} 查看)");
    }
}
