package com.starrysky.aries.notification.service;

import com.starrysky.aries.common.schema.PageResponse;
import com.starrysky.aries.entity.PelotonTeam;
import com.starrysky.aries.entity.UserNotification;
import com.starrysky.aries.exception.BizException;
import com.starrysky.aries.notification.enums.NotificationCategory;
import com.starrysky.aries.notification.enums.NotificationType;
import com.starrysky.aries.notification.event.NotificationBatchEvent;
import com.starrysky.aries.notification.event.NotificationEvent;
import com.starrysky.aries.notification.repository.UserNotificationRepository;
import com.starrysky.aries.notification.schema.UnreadCountVO;
import com.starrysky.aries.notification.schema.UserNotificationVO;
import com.starrysky.aries.peloton.repository.PelotonTeamRepository;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * NotificationService
 * 用户通知服务（查询、已读标记、通知发送）
 *
 * <h3>功能说明</h3>
 * <ul>
 *   <li>分页查询当前用户通知（支持按分类筛选）</li>
 *   <li>标记单条/全部为已读（幂等）</li>
 *   <li>获取各分类未读数量（单条 SQL 聚合）</li>
 *   <li>通过事件机制异步发送通知（事务隔离）</li>
 * </ul>
 *
 * @author adi
 * @version 2.0
 * @since 2026/06/16
 */
@Slf4j
@Service
public class NotificationService {

    /** 通知列表默认每页条数 */
    private static final int DEFAULT_PAGE_SIZE = 20;

    /** 通知保留天数（与清理任务对齐） */
    private static final int RETENTION_DAYS = 90;

    /** 批量插入分片大小 */
    private static final int SAVE_BATCH_SIZE = 100;

    /** title 字段数据库长度 */
    private static final int TITLE_MAX_LENGTH = 100;

    /** content 字段数据库长度 */
    private static final int CONTENT_MAX_LENGTH = 500;

    @Resource
    private UserNotificationRepository notificationRepository;

    @Resource
    private PelotonTeamRepository pelotonTeamRepository;

    @Resource
    private ApplicationEventPublisher eventPublisher;

    // ======================== 查询接口 ========================

    /**
     * 分页查询用户通知
     *
     * @param userId   当前用户ID
     * @param category 分类筛选（null 表示全部）
     * @param page     页码（从1开始）
     * @param size     每页条数
     * @return 通知分页列表
     */
    @Transactional(readOnly = true)
    public PageResponse<UserNotificationVO> getNotifications(
            Long userId, NotificationCategory category, Integer page, Integer size) {

        int currentPage = (page != null && page > 0) ? page : 1;
        int pageSize = (size != null && size > 0) ? Math.min(size, 50) : DEFAULT_PAGE_SIZE;
        PageRequest pageRequest = PageRequest.of(currentPage - 1, pageSize);

        Page<UserNotification> notificationPage;
        if (category != null) {
            notificationPage = notificationRepository
                    .findByUserIdAndCategoryOrderByCreatedAtDesc(userId, category, pageRequest);
        } else {
            notificationPage = notificationRepository
                    .findByUserIdOrderByCreatedAtDesc(userId, pageRequest);
        }

        // 批量获取车队名称（避免 N+1 查询）
        List<Long> teamIds = notificationPage.getContent().stream()
                .map(UserNotification::getTeamId)
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());
        Map<Long, String> teamNameMap = pelotonTeamRepository.findAllById(teamIds).stream()
                .collect(Collectors.toMap(PelotonTeam::getId, PelotonTeam::getName));

        List<UserNotificationVO> records = notificationPage.getContent().stream()
                .map(n -> convertToVO(n, teamNameMap.get(n.getTeamId())))
                .collect(Collectors.toList());

        return PageResponse.<UserNotificationVO>builder()
                .records(records)
                .total(notificationPage.getTotalElements())
                .page(currentPage)
                .size(pageSize)
                .pages(notificationPage.getTotalPages())
                .build();
    }

    /**
     * 获取各分类未读消息数量（单条 GROUP BY 查询）
     *
     * @param userId 当前用户ID
     * @return 各分类未读数
     */
    @Transactional(readOnly = true)
    public UnreadCountVO getUnreadCount(Long userId) {
        long teamCount = 0;
        long interactCount = 0;
        long systemCount = 0;

        List<Object[]> rows = notificationRepository.countUnreadGroupByCategory(userId);
        for (Object[] row : rows) {
            NotificationCategory cat = (NotificationCategory) row[0];
            long count = (Long) row[1];
            switch (cat) {
                case TEAM -> teamCount = count;
                case INTERACT -> interactCount = count;
                case SYSTEM -> systemCount = count;
            }
        }

        return UnreadCountVO.builder()
                .total(teamCount + interactCount + systemCount)
                .team(teamCount)
                .interact(interactCount)
                .system(systemCount)
                .build();
    }

    // ======================== 已读标记 ========================

    /**
     * 标记单条通知为已读（幂等）
     * <p>
     * 已读的通知重复标记时静默成功，不存在时抛 404。
     *
     * @param notificationId 通知ID
     * @param userId         当前用户ID
     */
    @Transactional(rollbackFor = Exception.class)
    public void markAsRead(Long notificationId, Long userId) {
        int updated = notificationRepository.updateReadStatus(notificationId, userId, true);
        if (updated == 0) {
            // 未更新：判断是“不存在”还是“已读”
            if (!notificationRepository.existsByIdAndUserId(notificationId, userId)) {
                throw new BizException(404, "通知不存在");
            }
            // 已读，幂等成功
        }
    }

    /**
     * 将用户所有未读通知标记为已读
     *
     * @param userId 当前用户ID
     * @return 标记数量
     */
    @Transactional(rollbackFor = Exception.class)
    public int markAllAsRead(Long userId) {
        return notificationRepository.markAllAsRead(userId);
    }

    // ======================== 事件驱动的发送方法（供其他 Service 调用） ========================

    /**
     * 发送单条通知（发布事件，事务提交后异步持久化）
     *
     * @param userId  接收用户ID
     * @param type    消息类型
     * @param title   标题
     * @param content 内容
     * @param teamId  关联车队ID（可为 null）
     */
    public void send(Long userId, NotificationType type,
                     String title, String content, Long teamId) {
        eventPublisher.publishEvent(
                new NotificationEvent(this, userId, type, truncateTitle(title), truncateContent(content), teamId));
    }

    /**
     * 批量发送通知给多个用户（发布事件，事务提交后异步持久化）
     *
     * @param userIds 接收用户ID列表
     * @param type    消息类型
     * @param title   标题
     * @param content 内容
     * @param teamId  关联车队ID（可为 null）
     */
    public void sendBatch(List<Long> userIds, NotificationType type,
                          String title, String content, Long teamId) {
        if (userIds == null || userIds.isEmpty()) {
            return;
        }
        eventPublisher.publishEvent(
                new NotificationBatchEvent(this, userIds, type, truncateTitle(title), truncateContent(content), teamId));
    }

    // ======================== 实际持久化方法（供 EventListener 调用） ========================

    /**
     * 实际写入单条通知（由 NotificationEventListener 调用）
     */
    @Transactional(rollbackFor = Exception.class)
    public void save(Long userId, NotificationType type,
                     String title, String content, Long teamId) {
        UserNotification notification = UserNotification.builder()
                .userId(userId)
                .type(type)
                .category(type.getCategory())
                .title(title)
                .content(content)
                .teamId(teamId)
                .isRead(false)
                .expiresAt(LocalDateTime.now().plusDays(RETENTION_DAYS))
                .build();
        notificationRepository.save(notification);
        log.debug("通知已持久化: userId={}, type={}, teamId={}", userId, type, teamId);
    }

    /**
     * 实际写入批量通知（由 NotificationEventListener 调用，分批插入避免大事务）
     */
    @Transactional(rollbackFor = Exception.class)
    public void saveBatch(List<Long> userIds, NotificationType type,
                          String title, String content, Long teamId) {
        if (userIds == null || userIds.isEmpty()) {
            return;
        }

        LocalDateTime expiresAt = LocalDateTime.now().plusDays(RETENTION_DAYS);
        List<UserNotification> notifications = userIds.stream()
                .map(uid -> UserNotification.builder()
                        .userId(uid)
                        .type(type)
                        .category(type.getCategory())
                        .title(title)
                        .content(content)
                        .teamId(teamId)
                        .isRead(false)
                        .expiresAt(expiresAt)
                        .build())
                .collect(Collectors.toList());

        // 分批写入，避免一次性大事务
        for (int i = 0; i < notifications.size(); i += SAVE_BATCH_SIZE) {
            int end = Math.min(i + SAVE_BATCH_SIZE, notifications.size());
            List<UserNotification> batch = notifications.subList(i, end);
            notificationRepository.saveAll(batch);
            notificationRepository.flush();
        }

        log.debug("批量通知已持久化: count={}, type={}, teamId={}", notifications.size(), type, teamId);
    }

    // ============================ 私有方法 ============================

    /**
     * 通知实体 -> VO
     */
    private UserNotificationVO convertToVO(UserNotification notification, String teamName) {
        String linkType = resolveLinkType(notification.getType());
        return UserNotificationVO.builder()
                .id(notification.getId())
                .type(notification.getType().getCode())
                .category(notification.getCategory().getCode())
                .title(notification.getTitle())
                .content(notification.getContent())
                .teamId(notification.getTeamId())
                .teamName(teamName)
                .read(notification.getIsRead())
                .linkType(linkType)
                .link(buildLink(notification, linkType))
                .createdAt(notification.getCreatedAt())
                .build();
    }

    /**
     * 根据通知类型解析链接类型
     */
    private String resolveLinkType(NotificationType type) {
        if (type == NotificationType.FITTING_COMPLETE) {
            return "lab";
        }
        // 所有车队相关通知
        if (type.getCategory() == NotificationCategory.TEAM) {
            return "team";
        }
        return null;
    }

    /**
     * 根据 linkType 和通知数据构建前端跳转路由
     */
    private String buildLink(UserNotification notification, String linkType) {
        if (linkType == null) {
            return null;
        }
        return switch (linkType) {
            case "lab" -> "/lab";
            case "team" -> notification.getTeamId() != null ? "/peloton/" + notification.getTeamId() : null;
            default -> null;
        };
    }

    /**
     * 截断标题，防止超出数据库字段长度
     */
    private String truncateTitle(String title) {
        return truncate(title, TITLE_MAX_LENGTH);
    }

    /**
     * 截断内容，防止超出数据库字段长度
     */
    private String truncateContent(String content) {
        return truncate(content, CONTENT_MAX_LENGTH);
    }

    private String truncate(String text, int maxLength) {
        if (text == null) {
            return null;
        }
        return text.length() > maxLength ? text.substring(0, maxLength) : text;
    }
}

