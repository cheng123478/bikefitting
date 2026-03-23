package com.starrysky.aries.security.wechat;

import lombok.Getter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;

import java.io.Serial;
import java.util.Collection;

/**
 * WeChatAuthenticationToken
 * 微信认证令牌
 *
 * @author adice
 * @version 1.0
 * @since 2026-03-20 12:34:49
 */
public class WeChatAuthenticationToken extends AbstractAuthenticationToken {
    @Serial
    private static final long serialVersionUID = 6982892786301770004L;

    private final Object principal; // 认证的用户对象
    @Getter
    private final String code; // 微信code
    @Getter
    private String loginType; // 登录类型：mini 或 web

    // 登录前使用：传入 code
    public WeChatAuthenticationToken(String code, String loginType) {
        super(null);
        this.code = code;
        this.loginType = loginType;
        this.principal = null;
        setAuthenticated(false);
    }

    // 登录后使用：传入 UserDetails
    public WeChatAuthenticationToken(Object principal, Collection<? extends GrantedAuthority> authorities) {
        super(authorities);
        this.principal = principal;
        this.code = null;
        setAuthenticated(true);
    }

    @Override
    public Object getCredentials() { return null; }

    @Override
    public Object getPrincipal() { return this.principal; }

}
