package com.starrysky.aries.notification.event;

import com.starrysky.aries.notification.enums.NotificationType;
import lombok.Getter;
import org.springframework.context.ApplicationEvent;

import java.util.List;

/**
 * NotificationBatchEvent
 * 批量通知事件（事务提交后异步持久化）
 *
 * @author adi
 * @version 1.0
 * @since 2026/06/17
 */
@Getter
public class NotificationBatchEvent extends ApplicationEvent {

    private final List<Long> userIds;
    private final NotificationType type;
    private final String title;
    private final String content;
    private final Long teamId;
    private final Long linkId;

    public NotificationBatchEvent(Object source, List<Long> userIds, NotificationType type,
                                  String title, String content, Long teamId, Long linkId) {
        super(source);
        this.userIds = userIds;
        this.type = type;
        this.title = title;
        this.content = content;
        this.teamId = teamId;
        this.linkId = linkId;
    }
}
