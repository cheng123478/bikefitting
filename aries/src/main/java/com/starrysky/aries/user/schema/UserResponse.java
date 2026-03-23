package com.starrysky.aries.user.schema;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;

import java.time.LocalDate;

/**
 * 用户模块-响应参数聚合
 *
 * @author adi
 * @version 1.0
 * @since 2026/2/2 15:36
 */
public class UserResponse {
    /**
     * 用户核心资料 (脱敏后)
     */
    public record Profile(
            @JsonSerialize(using = ToStringSerializer.class) // 防止长整型精度丢失
            Long id,

            String phone,
            String name,
            String introduction,
            String avatar,
            LocalDate birthday,
            Byte gender,
            String location
    ) {}

    /**
     * 登录或刷新令牌后的完整结果
     */
    public record LoginResult(
            String accessToken,
            String refreshToken,
            Profile user // 嵌套用户基础资料
    ) {}
}
