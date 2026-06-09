package com.starrysky.aries.membership.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 特权类型枚举
 *
 * @author adi
 */
@Getter
@AllArgsConstructor
public enum PrivilegeTypeEnum {
    DYNAMIC_FITTING("动态Fitting分析"),
    GARAGE_EXPANSION("车库上限扩展"),
    REALTIME_MONITOR("AI实时监护(无限时)"),
    HISTORY_SNAPSHOT("历史快照对比");

    private final String description;
}
