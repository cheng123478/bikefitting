package com.starrysky.aries.entity;

import com.starrysky.aries.notification.enums.NotificationCategory;
import com.starrysky.aries.notification.enums.NotificationCategoryConverter;
import com.starrysky.aries.notification.enums.NotificationType;
import com.starrysky.aries.notification.enums.NotificationTypeConverter;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Comment;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * UserNotification
 * 用户通知实体（映射 user_notification 表）
 *
 * @author adi
 * @version 1.0
 * @since 2026/06/16
 */
@Entity
@Table(name = "user_notification",
       indexes = {
           @Index(name = "idx_user_read_time", columnList = "user_id, is_read, created_at DESC"),
           @Index(name = "idx_expires_at", columnList = "expires_at")
       })
@Comment("用户通知表")
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class UserNotification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    @Comment("接收通知的用户")
    private Long userId;

    @Convert(converter = NotificationTypeConverter.class)
    @Column(nullable = false, length = 30)
    @Comment("消息类型: new-activity, member-join, like, comment 等")
    private NotificationType type;

    @Convert(converter = NotificationCategoryConverter.class)
    @Column(nullable = false, length = 20)
    @Comment("消息分类: team, interact, system")
    private NotificationCategory category;

    @Column(nullable = false, length = 100)
    @Comment("通知标题")
    private String title;

    @Column(nullable = false, length = 500)
    @Comment("通知内容")
    private String content;

    @Column(name = "team_id")
    @Comment("关联车队ID（车队相关通知时设置）")
    private Long teamId;

    @Column(name = "is_read", nullable = false)
    @Comment("是否已读: false未读 true已读")
    private Boolean isRead = false;

    @Column(name = "expires_at")
    @Comment("过期时间（定时清理任务依据此字段删除过期通知）")
    private LocalDateTime expiresAt;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    @Comment("创建时间")
    private LocalDateTime createdAt;
}
