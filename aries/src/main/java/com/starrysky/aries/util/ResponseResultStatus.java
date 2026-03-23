package com.starrysky.aries.util;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * ResponseResultStatus
 * 响应结果状态
 *
 * @author adice
 * @version 1.0
 * @since 2025-07-01 11:07:49
 */
@Getter
@AllArgsConstructor
public enum ResponseResultStatus {
    SUCCESS(200, "Success"),

    BAD_REQUEST(400, "Bad Request"),
    UNAUTHORIZED(401, "Unauthorized"),
    TOKEN_EXPIRED(40101, "Token Expired"),
    TOKEN_INVALID(40102, "Token Invalid"),
    TOKEN_MISSING(40103, "Token Missing"),// redis中不存在
    TOO_MANY_REQUESTS(429, "Too Many Requests"),
    FORBIDDEN(403, "Forbidden"),
    NOT_FOUND(404, "Not Found"),
    METHOD_NOT_ALLOWED(405, "Method Not Allowed"),

    INTERNAL_SERVER_ERROR(500, "Internal Server Error"),
    SERVICE_UNAVAILABLE(503, "Service Unavailable"),

    // 业务错误,按需扩展
    BIZ_ERROR(10000, "Biz Error");


    private final Integer code;
    private final String message;
}
