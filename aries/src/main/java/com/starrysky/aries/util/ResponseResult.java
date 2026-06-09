package com.starrysky.aries.util;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;

import java.io.Serial;
import java.io.Serializable;

/**
 * ResponseResult
 * 响应结果的封装类
 *
 * @author adice
 * @version 1.0
 * @since 2025-07-01 11:07:49
 */
@Builder
@Getter
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ResponseResult<T> implements Serializable {
    @Serial
    private static final long serialVersionUID = -8895225937625032267L;
    // 状态码
    private Integer code;
    // 提示信息
    private String message;
    // 数据
    private T data;
    // 响应时间戳
    @Builder.Default
    private long timestamp = System.currentTimeMillis();

    // ============================ 成功工厂方法 ============================

    public static ResponseResult<Void> success() {
        return success(null);
    }

    public static <T> ResponseResult<T> success(T data) {
        return ResponseResult.<T>builder()
                .code(ResponseResultStatus.SUCCESS.getCode())
                .message(ResponseResultStatus.SUCCESS.getMessage())
                .data(data)
                .build();
    }

    public static <T> ResponseResult<T> success(String message, T data) {
        return ResponseResult.<T>builder()
                .code(ResponseResultStatus.SUCCESS.getCode())
                .message(message)
                .data(data)
                .build();
    }

    // ============================ 失败工厂方法 ============================

    public static <T> ResponseResult<T> failure(ResponseResultStatus status) {
        return failure(status.getCode(), status.getMessage());
    }

    public static <T> ResponseResult<T> failure(Integer code, String message) {
        return ResponseResult.<T>builder()
                .code(code)
                .message(message)
                .build();
    }

    public static <T> ResponseResult<T> failure(ResponseResultStatus responseResultStatus, String message) {
        return ResponseResult.<T>builder()
                .code(responseResultStatus.getCode())
                .message(message)
                .build();
    }

    public static <T> ResponseResult<T> failure(Integer code, String message, T data) {
        return ResponseResult.<T>builder()
                .code(code)
                .message(message)
                .data(data)
                .build();
    }

    public static <T> ResponseResult<T> failure(ResponseResultStatus responseResultStatus, T data) {
        return ResponseResult.<T>builder()
                .code(responseResultStatus.getCode())
                .message(responseResultStatus.getMessage())
                .data(data)
                .build();
    }

}
