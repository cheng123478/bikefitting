package com.starrysky.aries.notification.task;

import com.starrysky.aries.notification.repository.UserNotificationRepository;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * NotificationCleanupTask
 * 定时清理过期通知
 * <p>
 * 保留策略：所有通知保留 90 天，超过 90 天的通知物理删除。
 * 执行频率：每周一凌晨 3:00。
 * 分批删除：每批 500 条，避免长事务锁表。
 *
 * @author adi
 * @version 1.0
 * @since 2026/06/17
 */
@Slf4j
@Component
public class NotificationCleanupTask {

    /** 通知保留天数 */
    private static final int RETENTION_DAYS = 90;

    /** 每批删除数量 */
    private static final int BATCH_SIZE = 500;

    @Resource
    private UserNotificationRepository notificationRepository;

    @Scheduled(cron = "0 0 3 * * MON")
    public void cleanExpiredNotifications() {
        log.info(">>> 开始执行过期通知清理任务（保留 {} 天）...", RETENTION_DAYS);
        long startTime = System.currentTimeMillis();

        LocalDateTime deadline = LocalDateTime.now().minusDays(RETENTION_DAYS);
        int totalCount = 0;
        int deleted;

        do {
            try {
                deleted = notificationRepository.deleteExpiredBatch(deadline, BATCH_SIZE);
                totalCount += deleted;
                if (deleted > 0) {
                    log.info("已删除一批过期通知，数量: {}", deleted);
                }
            } catch (Exception e) {
                log.error("清理过期通知时发生异常，本轮终止", e);
                break;
            }

            // 释放数据库资源
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        } while (deleted >= BATCH_SIZE);

        log.info("<<< 过期通知清理任务结束，共删除 {} 条，耗时 {} ms",
                totalCount, System.currentTimeMillis() - startTime);
    }
}
