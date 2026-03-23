package com.starrysky.aries.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.starrysky.aries.util.ResponseResult;
import com.starrysky.aries.util.ResponseResultStatus;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * AriesAccessDeniedHandler
 * 无权限访问时的处理
 *
 * @author adice
 * @version 1.0
 * @since 2025-07-01 11:07:49
 */
@Component
@Slf4j
public class AriesAccessDeniedHandler implements AccessDeniedHandler {
    @Resource
    private ObjectMapper objectMapper;

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response, AccessDeniedException accessDeniedException) throws IOException {
        log.warn("鉴权失败 | 路径: {} | 原因: {}", request.getRequestURI(), accessDeniedException.getMessage());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        response.getWriter().print(objectMapper.writeValueAsString(ResponseResult.failure(ResponseResultStatus.UNAUTHORIZED, "您无权访问，请更换账号或联系管理员！")));
    }
}
