package com.starrysky.aries.user.schema;

import jakarta.validation.constraints.*;

import java.time.LocalDate;

/**
 * 用户模块-请求参数聚合
 *
 * @author adi
 * @version 1.0
 * @since 2026/2/2 15:32
 */
public final class UserRequest {
    /**
     * 注册
     * 手机号、验证码
     */
    public record Register(
            @NotBlank(message = "新手机号不能为空")
            @Pattern(regexp = "^1[3-9]\\d{9}$", message = "手机号格式错误")
            String newPhone,

            @NotBlank(message = "短信验证码不能为空")
            String code
    ) {
    }

    /**
     * 设置/重置密码 (通过验证码)
     */
    public record PasswordSet(
            @NotBlank(message = "验证码不能为空")
            String code,

            @NotBlank(message = "新密码不能为空")
            String password
    ) {
    }

    /**
     * 修改密码 (通过旧密码)
     */
    public record PasswordChange(
            @NotBlank(message = "旧密码不能为空")
            String oldPassword,

            @NotBlank(message = "新密码不能为空")
            @Size(min = 6, max = 20, message = "密码长度需在6-20位")
            String newPassword
    ) {
    }

    /**
     * 换绑手机号
     */
    public record PhoneChange(
            @NotBlank(message = "新手机号不能为空")
            @Pattern(regexp = "^1[3-9]\\d{9}$", message = "手机号格式错误")
            String newPhone,

            @NotBlank(message = "当前登录密码不能为空")
            String password,

            @NotBlank(message = "短信验证码不能为空")
            String code
    ) {
    }

    /**
     * 修改个人资料
     */
    public record ProfileUpdate(
            @Size(max = 20, message = "昵称过长")
            String name,

            @Size(max = 50, message = "简介过长")
            String introduction,

            @Past(message = "生日必须是过去的时间")
            LocalDate birthday,

            @Min(0) @Max(2)
            Byte gender,

            String location
    ) {
    }

    /**
     * 刷新Token
     */
    public record RefreshToken(
            @NotBlank(message = "刷新令牌不能为空")
            String refreshToken
    ) {
    }

}
