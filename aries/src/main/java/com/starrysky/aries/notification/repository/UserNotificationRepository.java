package com.starrysky.aries.notification.repository;

import com.starrysky.aries.entity.UserNotification;
import com.starrysky.aries.notification.enums.NotificationCategory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

/**
 * UserNotificationRepository
 * 用户通知数据访问层（映射 user_notification 表）
 *
 * @author adi
 * @version 1.0
 * @since 2026/06/16
 */
public interface UserNotificationRepository extends JpaRepository<UserNotification, Long> {

    /**
     * 分页查询用户通知（按创建时间倒序）
     *
     * @param userId   用户ID
     * @param pageable 分页参数
     * @return 通知分页列表
     */
    Page<UserNotification> findByUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);

    /**
     * 按分类分页查询用户通知（按创建时间倒序）
     *
     * @param userId   用户ID
     * @param category 消息分类
     * @param pageable 分页参数
     * @return 通知分页列表
     */
    Page<UserNotification> findByUserIdAndCategoryOrderByCreatedAtDesc(
            Long userId, NotificationCategory category, Pageable pageable);

    /**
     * 按分类统计用户未读通知数量（GROUP BY 单条 SQL）
     *
     * @param userId 用户ID
     * @return 每行 [category, count]
     */
    @Query("SELECT n.category, COUNT(n) FROM UserNotification n " +
           "WHERE n.userId = :userId AND n.isRead = false GROUP BY n.category")
    List<Object[]> countUnreadGroupByCategory(@Param("userId") Long userId);

    /**
     * 判断通知是否存在（用于 markAsRead 幂等校验）
     *
     * @param id     通知ID
     * @param userId 用户ID
     * @return 是否存在
     */
    boolean existsByIdAndUserId(Long id, Long userId);

    /**
     * 将单条通知标记为已读（需验证所属用户）
     *
     * @param id     通知ID
     * @param userId 用户ID
     * @param isRead 目标已读状态
     * @return 受影响行数
     */
    @Modifying
    @Query("UPDATE UserNotification n SET n.isRead = :isRead WHERE n.id = :id AND n.userId = :userId AND n.isRead = false")
    int updateReadStatus(@Param("id") Long id, @Param("userId") Long userId, @Param("isRead") Boolean isRead);

    /**
     * 将用户所有未读通知批量标记为已读
     *
     * @param userId 用户ID
     * @return 受影响行数
     */
    @Modifying
    @Query("UPDATE UserNotification n SET n.isRead = true WHERE n.userId = :userId AND n.isRead = false")
    int markAllAsRead(@Param("userId") Long userId);

    /**
     * 分批删除过期通知（原生 SQL + LIMIT）
     *
     * @param deadline  过期截止时间
     * @param batchSize 每批删除数量
     * @return 删除行数
     */
    @Modifying
    @Query(value = "DELETE FROM user_notification WHERE expires_at < :deadline LIMIT :batchSize", nativeQuery = true)
    int deleteExpiredBatch(@Param("deadline") LocalDateTime deadline, @Param("batchSize") int batchSize);
}
