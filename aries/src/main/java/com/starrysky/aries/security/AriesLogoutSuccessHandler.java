package com.starrysky.aries.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.starrysky.aries.entity.User;
import com.starrysky.aries.redis.RedisConstant;
import com.starrysky.aries.redis.RedisUtil;
import com.starrysky.aries.util.ResponseResult;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.logout.LogoutSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * AriesLogoutSuccessHandler
 * 用户退出登录成功处理类
 *
 * @author adice
 * @version 1.0
 * @since 2025-07-01 11:07:49
 */
@Slf4j
@Component
public class AriesLogoutSuccessHandler implements LogoutSuccessHandler {
    @Resource
    private RedisUtil redisUtil;
    @Resource
    private ObjectMapper objectMapper;

    @Override
    public void onLogoutSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication) throws IOException {
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        try {
            if (authentication != null && authentication.getPrincipal() instanceof User user) {
                if (user.getId() != null) {
                    // 删除 Redis 中的认证信息
                    String redisKey = RedisConstant.AUTHENTICATION + user.getId();
                    redisUtil.del(redisKey);
                    log.info("用户退出登录成功 | 用户ID: {} | Redis Key: {}", user.getId(), redisKey);
                } else {
                    log.warn("用户退出登录，但用户ID为null");
                }
            } else {
                log.warn("用户退出登录，但未获取到用户信息");
            }
            
            SecurityContextHolder.clearContext();
            response.getWriter().print(objectMapper.writeValueAsString(ResponseResult.success("注销登录成功！")));
        } catch (Exception e) {
            log.error("用户退出登录失败", e);
            response.getWriter().print(objectMapper.writeValueAsString(ResponseResult.failure(500, "注销失败！")));
        }
    }
}
