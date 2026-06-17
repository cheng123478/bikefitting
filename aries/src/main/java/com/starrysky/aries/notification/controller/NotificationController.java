package com.starrysky.aries.notification.controller;

import com.starrysky.aries.entity.User;
import com.starrysky.aries.notification.schema.UnreadCountVO;
import com.starrysky.aries.common.schema.PageResponse;
import com.starrysky.aries.notification.enums.NotificationCategory;
import com.starrysky.aries.notification.schema.UserNotificationVO;
import com.starrysky.aries.notification.service.NotificationService;
import com.starrysky.aries.util.ResponseResult;
import jakarta.annotation.Resource;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

/**
 * NotificationController
 * 消息中心控制器
 *
 * <h3>认证边界说明</h3>
 * <p>所有接口均需登录（用户只能查看/操作自己的消息）。</p>
 *
 * @author adi
 * @version 1.0
 * @since 2026/06/16
 */
@RestController
@RequestMapping("/notifications")
public class NotificationController {

    @Resource
    private NotificationService notificationService;

    /**
     * 获取通知列表（分页）
     *
     * @param category 分类筛选（team/interact/system，不传则全部）
     * @param page     页码（从1开始，默认1）
     * @param size     每页条数（默认20，最大50）
     * @param user     当前登录用户
     * @return 通知分页列表
     */
    @GetMapping
    public ResponseResult<PageResponse<UserNotificationVO>> getNotifications(
            @RequestParam(value = "category", required = false) String category,
            @RequestParam(value = "page", required = false) Integer page,
            @RequestParam(value = "size", required = false) Integer size,
            @AuthenticationPrincipal User user) {
        NotificationCategory cat = null;
        if (category != null && !category.isEmpty()) {
            cat = NotificationCategory.valueOf(category.toUpperCase());
        }
        return ResponseResult.success(
                notificationService.getNotifications(user.getId(), cat, page, size));
    }

    /**
     * 获取各分类未读消息数量
     *
     * @param user 当前登录用户
     * @return 各分类未读数
     */
    @GetMapping("/unread-count")
    public ResponseResult<UnreadCountVO> getUnreadCount(@AuthenticationPrincipal User user) {
        return ResponseResult.success(notificationService.getUnreadCount(user.getId()));
    }

    /**
     * 标记单条通知为已读
     *
     * @param notificationId 通知ID
     * @param user           当前登录用户
     * @return 操作结果
     */
    @PutMapping("/{notificationId}/read")
    public ResponseResult<Void> markAsRead(
            @PathVariable("notificationId") Long notificationId,
            @AuthenticationPrincipal User user) {
        notificationService.markAsRead(notificationId, user.getId());
        return ResponseResult.success();
    }

    /**
     * 全部标记为已读
     *
     * @param user 当前登录用户
     * @return 操作结果
     */
    @PutMapping("/read-all")
    public ResponseResult<Void> markAllAsRead(@AuthenticationPrincipal User user) {
        notificationService.markAllAsRead(user.getId());
        return ResponseResult.success();
    }
}
