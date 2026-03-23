package com.starrysky.aries.security.wechat;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationServiceException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.AbstractAuthenticationProcessingFilter;

import java.util.Map;

/**
 * WeChatAuthenticationFilter
 * 微信验证过滤器
 *
 * @author adice
 * @version 1.0
 * @since 2026-03-20 12:34:49
 */
@Setter
@Getter
@Slf4j
public class WeChatAuthenticationFilter extends AbstractAuthenticationProcessingFilter {

    private ObjectMapper objectMapper;

    public WeChatAuthenticationFilter() {
        super(request -> request.getRequestURI().equals("/user/login/wechat") && request.getMethod().equals("POST"));
    }

    @Override
    public Authentication attemptAuthentication(HttpServletRequest request,
                                                HttpServletResponse response) throws AuthenticationException {
        if (!request.getMethod().equals("POST")) {
            throw new AuthenticationServiceException("仅支持 POST 请求");
        }

        try {
            Map<String, String> data = objectMapper.readValue(request.getInputStream(), Map.class);
            String code = data.get("code"); // 获取前端传来的 code
            String type = data.getOrDefault("type", "web"); // 获取登录类型，默认为 web
            WeChatAuthenticationToken authRequest = new WeChatAuthenticationToken(code, type);
            return this.getAuthenticationManager().authenticate(authRequest);
        } catch (Exception e) {
            throw new AuthenticationServiceException("微信登录参数解析失败", e);
        }
    }
}
