package com.starrysky.aries.security.sms;

import cn.hutool.core.util.PhoneUtil;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;

import java.io.Serial;
import java.util.Collection;

/**
 * SmsCodeAuthenticationToken
 * 短信验证码认证令牌
 *
 * @author adice
 * @version 1.0
 * @since 2025-07-01 11:07:49
 */
public class SmsCodeAuthenticationToken extends AbstractAuthenticationToken {
    @Serial
    private static final long serialVersionUID = 8626802081217070007L;

    private final Object principal; // 手机号
    private Object credentials; // 验证码

    // 未认证时使用
    public SmsCodeAuthenticationToken(String phone, String verificationCode) {
        super(null);
        if(!PhoneUtil.isPhone(phone)){
            throw new IllegalArgumentException("手机号格式不正确");
        }
        this.principal = phone;
        this.credentials = verificationCode;
        setAuthenticated(false); // 初始未认证
    }

    // 认证成功后使用
    public SmsCodeAuthenticationToken(Object principal,
                                      Collection<? extends GrantedAuthority> authorities) {
        super(authorities);
        this.principal = principal;
        this.credentials = null;
        super.setAuthenticated(true); // 已认证
    }

    @Override
    public Object getCredentials() {
        return this.credentials;
    }

    @Override
    public Object getPrincipal() {
        return this.principal;
    }

    @Override
    public void setAuthenticated(boolean isAuthenticated) throws IllegalArgumentException {
        if (isAuthenticated) {
            throw new IllegalArgumentException(
                    "Cannot set this token to trusted - use constructor which takes a GrantedAuthority list instead");
        }
        super.setAuthenticated(false);
    }

    @Override
    public void eraseCredentials() {
        super.eraseCredentials();
        this.credentials = null;
    }
}
