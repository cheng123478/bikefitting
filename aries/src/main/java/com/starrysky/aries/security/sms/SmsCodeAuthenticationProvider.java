package com.starrysky.aries.security.sms;

import com.starrysky.aries.entity.User;
import com.starrysky.aries.sms.service.SmsService;
import com.starrysky.aries.user.service.UserService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.stereotype.Component;

/**
 * SmsCodeAuthenticationProvider
 * 短信验证
 *
 * @author adice
 * @version 1.0
 * @since 2025-07-01 11:07:49
 */
@Component
@Slf4j
public class SmsCodeAuthenticationProvider implements AuthenticationProvider {
    @Resource
    private UserService userService;
    @Resource
    private SmsService smsService;

    @Override
    public Authentication authenticate(Authentication authentication) throws AuthenticationException {
        SmsCodeAuthenticationToken authenticationToken = (SmsCodeAuthenticationToken) authentication;
        String phone = (String) authenticationToken.getPrincipal();
        String code = (String) authenticationToken.getCredentials();
        // 验证短信验证码
        if (!smsService.validateSmsCode(phone, code)) {
            log.warn("短信验证码错误 | 手机号: {}", phone);
            throw new BadCredentialsException("短信验证码不正确");
        }
        // 根据手机号加载用户信息
        User user = userService.registAndLogin(phone);
        log.info("短信验证码登录成功 | 手机号: {}", phone);
        // 创建已认证的令牌
        SmsCodeAuthenticationToken authenticationResult = new SmsCodeAuthenticationToken(
                user, user.getAuthorities());
        authenticationResult.setDetails(authenticationToken.getDetails());
        return authenticationResult;
    }

    @Override
    public boolean supports(Class<?> authentication) {
        return SmsCodeAuthenticationToken.class.isAssignableFrom(authentication);
    }
}
