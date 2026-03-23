package com.starrysky.aries.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.starrysky.aries.util.IpUtil;
import com.starrysky.aries.util.ResponseResult;
import com.starrysky.aries.util.ResponseResultStatus;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * AriesAuthenticationEntryPoint
 * 未登录状态的处理
 *
 * @author adice
 * @version 1.0
 * @since 2025-07-01 11:07:49
 */
@Component
@Slf4j
public class AriesAuthenticationEntryPoint implements AuthenticationEntryPoint {
    @Resource
    private ObjectMapper objectMapper;
    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response, AuthenticationException authException) throws IOException {
        log.debug("匿名访问被拒 | 路径: {} | IP: {}", request.getRequestURI(), IpUtil.getRequestIp(request));
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.getWriter().print(objectMapper.writeValueAsString(ResponseResult.failure(ResponseResultStatus.UNAUTHORIZED, "您未登录！")));
    }
}
