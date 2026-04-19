package com.starrysky.aries.security.jwt;

import cn.hutool.crypto.digest.DigestUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.starrysky.aries.entity.User;
import com.starrysky.aries.redis.RedisConstant;
import com.starrysky.aries.redis.RedisUtil;
import com.starrysky.aries.util.IpUtil;
import com.starrysky.aries.util.ResponseResult;
import com.starrysky.aries.util.ResponseResultStatus;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Resource;
import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.jose4j.jwt.consumer.InvalidJwtException;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;
import java.util.Map;

/**
 * JwtFilter
 * 白名单里的请求，直接通过
 * 白名单以外的请求，若没有获取到请求头里的token，直接返回响应，并提示未获取到令牌
 * 若获取到请求头里的token，则校验，成功追加userid到header，否则返回并提示
 *
 * @author adice
 * @version 1.0
 * @since 2025-07-01 11:07:49
 */
@Component
@Slf4j
public class JwtFilter extends OncePerRequestFilter {

    /**
     * 白名单
     */
    private final List<String> whiteList = List.of("/user/login", "/user/login/sms",
            "/user/login/wechat", "/user/refresh", "/error", "/sms/send", "/resource/**");

    @Resource
    private RedisUtil redisUtil;
    @Resource
    private ObjectMapper objectMapper;

    private final AntPathMatcher pathMatcher = new AntPathMatcher();

    /**
     * 决定是否跳过此过滤器
     * 返回 true = 跳过 (即白名单)
     */
    @Override
    protected boolean shouldNotFilter(@Nonnull HttpServletRequest request) {
        String path = request.getServletPath();
        for (String pattern : whiteList) {
            if (pathMatcher.match(pattern, path)) {
                return true;
            }
        }
        return false;
    }

    @Override
    protected void doFilterInternal(@Nonnull HttpServletRequest request, @Nonnull HttpServletResponse response,
            @Nonnull FilterChain filterChain) throws IOException {
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        // 1. 提取 Token
        String header = request.getHeader("Authorization");
        if (!StringUtils.hasText(header) || !header.startsWith("Bearer ")) {
            log.warn("未获取到令牌 | IP: {} | URI: {}", IpUtil.getRequestIp(request), request.getRequestURI());
            response.getWriter().write(objectMapper
                    .writeValueAsString(ResponseResult.failure(HttpStatus.UNAUTHORIZED.value(), "请先登录或注册！")));
            return;
        }
        try {
            // 校验 Token (签名 + 过期时间),如果过期，parseToken内部会直接抛出InvalidJwtException
            String token = header.substring(7);
            String id = JwtUtil.getIdFromToken(token);
            if (!StringUtils.hasText(id)) {
                log.warn("令牌解析失败 | IP: {} | URI: {}", IpUtil.getRequestIp(request), request.getRequestURI());
                response.getWriter().write(objectMapper.writeValueAsString(
                        ResponseResult.failure(ResponseResultStatus.TOKEN_INVALID.getCode(), "令牌解析失败")));
                return;
            }

            String userAgent = JwtUtil.getUserAgentFromToken(token);
            if (request.getHeader("User-Agent") == null
                    || !DigestUtil.sha256Hex(request.getHeader("User-Agent")).equals(userAgent)) {
                log.warn("设备环境异常 | IP: {} | URI: {}", IpUtil.getRequestIp(request), request.getRequestURI());
                response.getWriter().write(objectMapper.writeValueAsString(
                        ResponseResult.failure(ResponseResultStatus.TOKEN_INVALID.getCode(), "令牌解析失败")));
                return;
            }

            // 加载用户详情
            Map<Object, Object> map = redisUtil.getHashOperations().entries(RedisConstant.AUTHENTICATION + id);
            if (CollectionUtils.isEmpty(map) || map.get("principal") == null) {
                log.warn("Redis中未找到用户信息 | User: {} | IP: {} | URI: {}", id, IpUtil.getRequestIp(request),
                        request.getRequestURI());
                response.getWriter().write(objectMapper.writeValueAsString(
                        ResponseResult.failure(ResponseResultStatus.TOKEN_MISSING.getCode(), "会话已失效，请重新登录")));
                return;
            }
            User user = (User) map.get("principal");
            // 构建认证对象
            UsernamePasswordAuthenticationToken authenticationToken = new UsernamePasswordAuthenticationToken(user,
                    null, user.getAuthorities());
            authenticationToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
            SecurityContextHolder.getContext().setAuthentication(authenticationToken);
            // 滑动窗口续期Redis(仅当AccessToken有效时续期)
            redisUtil.expire(RedisConstant.AUTHENTICATION + id, RedisConstant.AUTHENTICATION_TIMEOUT);
            log.debug("用户认证通过 | User: {} | URI: {}", id, request.getRequestURI());

            filterChain.doFilter(request, response);
        } catch (InvalidJwtException e) {
            if (e.hasExpired()) {
                // 前端拦截器捕获401后，会携带Refresh Token调用/auth/refresh
                log.warn("令牌已过期 | IP: {} | URI: {}", IpUtil.getRequestIp(request), request.getRequestURI());
                response.getWriter().write(objectMapper.writeValueAsString(
                        ResponseResult.failure(ResponseResultStatus.TOKEN_EXPIRED.getCode(), "令牌已过期，请刷新")));
            } else {
                log.warn("令牌无效 | IP: {} | URI: {} | Reason: {}", IpUtil.getRequestIp(request), request.getRequestURI(),
                        e.getMessage());
                response.getWriter().write(objectMapper.writeValueAsString(
                        ResponseResult.failure(ResponseResultStatus.TOKEN_INVALID.getCode(), "令牌无效")));
            }
        } catch (Exception e) {
            log.error("JWT认证系统异常", e);
            response.getWriter().write(objectMapper
                    .writeValueAsString(ResponseResult.failure(HttpStatus.INTERNAL_SERVER_ERROR.value(), "系统认证异常")));
        }
    }
}
