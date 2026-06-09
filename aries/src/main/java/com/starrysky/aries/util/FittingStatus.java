package com.starrysky.aries.util;

/**
 * Fitting 状态枚举
 *
 * @author adi
 * @version 1.0
 * @since 2026/3/4 12:43
 */
public enum FittingStatus {
    ANALYSING(1, "待分析"),
    COMPLETED(2, "已完成"),
    FAILED(3, "失败");

    private final Integer code;
    private final String message;

    FittingStatus(Integer code, String message) {
        this.code = code;
        this.message = message;
    }

    public Integer getCode() {
        return code;
    }

    public String getMessage() {
        return message;
    }
}
