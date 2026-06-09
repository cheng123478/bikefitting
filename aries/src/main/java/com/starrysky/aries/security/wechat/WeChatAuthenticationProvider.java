package com.starrysky.aries.security.wechat;

import com.starrysky.aries.entity.User;
import com.starrysky.aries.user.service.UserService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.stereotype.Component;

/**
 * WeChatAuthenticationProvider
 * 微信验证
 *
 * @author adice
 * @version 1.0
 * @since 2026-03-20 12:34:49
 */
@Component
@Slf4j
public class WeChatAuthenticationProvider implements AuthenticationProvider {
    @Resource
    private UserService userService;
    @Resource
    private WeChatService weChatService;

    @Override
    public Authentication authenticate(Authentication authentication) throws AuthenticationException {
        WeChatAuthenticationToken token = (WeChatAuthenticationToken) authentication;
        String code = token.getCode();
        String loginType = token.getLoginType();
        
        // 1. 获取 unionId
        String unionId;
        if ("web".equals(loginType)) {
            unionId = weChatService.getUnionIdFromWeb(code);
        } else {
            unionId = weChatService.getUnionIdFromMiniProgram(code);
        }
        
        // 2. 数据库查询或注册逻辑：如果 unionId 不存在则自动注册
        User user = userService.registAndLoginByWeChat(unionId);
        // 3. 返回认证成功的 Token
        return new WeChatAuthenticationToken(user, user.getAuthorities());
    }

    @Override
    public boolean supports(Class<?> authentication) {
        return WeChatAuthenticationToken.class.isAssignableFrom(authentication);
    }
}
