package com.starrysky.aries.notification.enums;

import lombok.Getter;

/**
 * NotificationType
 * 通知类型枚举（18 种通知场景）
 *
 * @author adi
 * @version 1.0
 * @since 2026/06/17
 */
@Getter
public enum NotificationType {

    // ==================== 车队动态 ====================
    /** 新活动发布 */
    NEW_ACTIVITY("new-activity", NotificationCategory.TEAM),
    /** 活动已编辑 */
    EDIT_ACTIVITY("edit-activity", NotificationCategory.TEAM),
    /** 活动已删除 */
    DELETE_ACTIVITY("delete-activity", NotificationCategory.TEAM),
    /** 新成员加入 */
    MEMBER_JOIN("member-join", NotificationCategory.TEAM),
    /** 成员退出 */
    MEMBER_LEAVE("member-leave", NotificationCategory.TEAM),
    /** 队长转让并退出 */
    CAPTAIN_LEAVE("captain-leave", NotificationCategory.TEAM),
    /** 成员被请离 */
    MEMBER_KICKED("member-kicked", NotificationCategory.TEAM),
    /** 管理员任命 */
    MEMBER_PROMOTE("member-promote", NotificationCategory.TEAM),
    /** 管理员撤销 */
    MEMBER_DEMOTE("member-demote", NotificationCategory.TEAM),
    /** 新公告发布 */
    NEW_NOTICE("new-notice", NotificationCategory.TEAM),
    /** 公告已编辑 */
    EDIT_NOTICE("edit-notice", NotificationCategory.TEAM),
    /** 入队申请 */
    JOIN_APPLY("join-apply", NotificationCategory.TEAM),
    /** 申请已通过 */
    JOIN_APPROVED("join-approved", NotificationCategory.TEAM),
    /** 申请已拒绝 */
    JOIN_REJECTED("join-rejected", NotificationCategory.TEAM),
    /** 车队已解散 */
    TEAM_DISBANDED("team-disbanded", NotificationCategory.TEAM),

    // ==================== 互动消息 ====================
    /** 获得点赞 */
    LIKE("like", NotificationCategory.INTERACT),
    /** 新评论 */
    COMMENT("comment", NotificationCategory.INTERACT),

    // ==================== 系统通知 ====================
    /** Fitting报告完成 */
    FITTING_COMPLETE("fitting-complete", NotificationCategory.SYSTEM);

    /** 数据库中存储的类型编码 */
    private final String code;

    /** 所属消息分类 */
    private final NotificationCategory category;

    NotificationType(String code, NotificationCategory category) {
        this.code = code;
        this.category = category;
    }
}
