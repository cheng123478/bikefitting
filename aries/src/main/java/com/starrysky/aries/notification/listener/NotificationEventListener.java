package com.starrysky.aries.notification.listener;

import com.starrysky.aries.notification.event.NotificationBatchEvent;
import com.starrysky.aries.notification.event.NotificationEvent;
import com.starrysky.aries.notification.service.NotificationService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * NotificationEventListener
 * 通知事件异步监听器
 * <p>
 * 在主事务提交后异步持久化通知，确保通知写入失败不会回滚业务事务。
 *
 * @author adi
 * @version 1.0
 * @since 2026/06/17
 */
@Slf4j
@Component
public class NotificationEventListener {

    @Resource
    private NotificationService notificationService;

    /**
     * 处理单条通知事件
     */
    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(rollbackFor = Exception.class, propagation = Propagation.REQUIRES_NEW)
    public void handleNotificationEvent(NotificationEvent event) {
        try {
            notificationService.save(event.getUserId(), event.getType(),
                    event.getTitle(), event.getContent(), event.getTeamId());
        } catch (Exception e) {
            log.error("通知持久化失败: userId={}, type={}", event.getUserId(), event.getType(), e);
        }
    }

    /**
     * 处理批量通知事件
     */
    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(rollbackFor = Exception.class, propagation = Propagation.REQUIRES_NEW)
    public void handleNotificationBatchEvent(NotificationBatchEvent event) {
        try {
            notificationService.saveBatch(event.getUserIds(), event.getType(),
                    event.getTitle(), event.getContent(), event.getTeamId());
        } catch (Exception e) {
            log.error("批量通知持久化失败: count={}, type={}", event.getUserIds().size(), event.getType(), e);
        }
    }
}
