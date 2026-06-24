package com.starrysky.aries.notification.event;

import com.starrysky.aries.notification.enums.NotificationType;
import lombok.Getter;
import org.springframework.context.ApplicationEvent;

/**
 * NotificationEvent
 * 单条通知事件（事务提交后异步持久化）
 *
 * @author adi
 * @version 1.0
 * @since 2026/06/17
 */
@Getter
public class NotificationEvent extends ApplicationEvent {

    private final Long userId;
    private final NotificationType type;
    private final String title;
    private final String content;
    private final Long teamId;
    private final Long linkId;

    public NotificationEvent(Object source, Long userId, NotificationType type,
                             String title, String content, Long teamId, Long linkId) {
        super(source);
        this.userId = userId;
        this.type = type;
        this.title = title;
        this.content = content;
        this.teamId = teamId;
        this.linkId = linkId;
    }
}
