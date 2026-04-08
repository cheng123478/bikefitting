package com.starrysky.aries.sms.record;

import com.fasterxml.jackson.annotation.JsonIgnore;

/**
 * 短信服务-响应结果聚合
 *
 * @param success 是否成功
 * @param code    错误码
 * @param message 错误信息
 */
public record SmsResult(boolean success, @JsonIgnore String code, String message) {
}
