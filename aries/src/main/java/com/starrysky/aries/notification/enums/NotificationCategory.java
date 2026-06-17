package com.starrysky.aries.notification.enums;

import lombok.Getter;

/**
 * NotificationCategory
 * 通知分类枚举
 *
 * @author adi
 * @version 1.0
 * @since 2026/06/17
 */
@Getter
public enum NotificationCategory {

    /** 车队动态 */
    TEAM("team"),
    /** 互动消息 */
    INTERACT("interact"),
    /** 系统通知 */
    SYSTEM("system");

    private final String code;

    NotificationCategory(String code) {
        this.code = code;
    }
}
