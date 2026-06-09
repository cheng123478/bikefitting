package com.starrysky.aries.security;

import cn.hutool.crypto.digest.DigestUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.starrysky.aries.entity.User;
import com.starrysky.aries.redis.RedisConstant;
import com.starrysky.aries.redis.RedisUtil;
import com.starrysky.aries.security.jwt.JwtUtil;
import com.starrysky.aries.user.schema.UserResponse;
import com.starrysky.aries.user.service.LoginRecordService;
import com.starrysky.aries.user.service.UserService;
import com.starrysky.aries.util.Constant;
import com.starrysky.aries.util.IpUtil;
import com.starrysky.aries.util.ResponseResult;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.time.LocalDate;
import java.util.UUID;

/**
 * AriesAuthenticationSuccessHandler
 * 登录成功处理类
 *
 * @author adice
 * @version 1.0
 * @since 2025-07-01 11:07:49
 */
@Component
@Slf4j
public class AriesAuthenticationSuccessHandler implements AuthenticationSuccessHandler {
    @Resource
    private LoginRecordService loginRecordService;
    @Resource
    private UserService userService;
    @Resource
    private RedisUtil redisUtil;
    @Resource
    private ObjectMapper objectMapper;

    /**
     * 登录成功后，需实现：
     * 1. 若登录失败次数不是0，将登陆失败次数置为0
     * 2. 将账号、登录时间等加密存入JWT，发送给客户端
     *
     * @param request        请求
     * @param response       响应
     * @param authentication 验证实体
     * @throws IOException IO异常
     */
    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
            Authentication authentication) throws IOException {
        User user = (User) authentication.getPrincipal();
        String ip = IpUtil.getRequestIp(request);
        // 检查用户是否已注销
        if (Boolean.TRUE.equals(user.getIsDeleted()) && user.getDeleteTime() != null &&
                LocalDate.now()
                        .isBefore(user.getDeleteTime().toLocalDate().plusDays(Constant.EXPIRED_ACCOUNT_CLEANUP_DAYS))) {
            user.setIsDeleted(false);
            user.setDeleteTime(null);
            userService.editUser(user);
            log.info("已注销用户重新激活 | 用户标识: {}", user.getId());
        }
        
        // 异步记录登录成功 (如果是微信用户，用前缀标识代替手机号，避免超过数据库潜在字段长度)
        String requestPhone = StringUtils.hasText(user.getPhone()) ? user.getPhone() : ("W_" + user.getId());
        if (requestPhone.length() > 20) {
            requestPhone = requestPhone.substring(0, 20);
        }
        loginRecordService.addLoginRecord(requestPhone, ip, LoginRecordService.LOGIN_RECORD_STATE_SUCCESS);
        // 在保存到Redis之前，清空密码
        user.setPassword(null);
        String tokenVersion = UUID.randomUUID().toString().replace("-", "");
        redisUtil.getHashOperations().put(RedisConstant.AUTHENTICATION + user.getId(), "principal", user);
        redisUtil.getHashOperations().put(RedisConstant.AUTHENTICATION + user.getId(), "token_version", tokenVersion);
        redisUtil.expire(RedisConstant.AUTHENTICATION + user.getId(), RedisConstant.AUTHENTICATION_TIMEOUT);

        // 将用户信息、Token信息封装到JWT，发送给客户端
        String accessToken = "Bearer "
                + JwtUtil.createAccessToken(user, DigestUtil.sha256Hex(request.getHeader("User-Agent")));
        String refreshToken = "Bearer " + JwtUtil.createRefreshToken(user, tokenVersion);
        UserResponse.Profile profile = new UserResponse.Profile(
                user.getId(),
                user.getPhone(),
                user.getName(),
                user.getIntroduction(),
                user.getAvatar(),
                user.getBirthday(),
                user.getGender(),
                user.getLocation());
        UserResponse.LoginResult loginResult = new UserResponse.LoginResult(
                accessToken,
                refreshToken,
                profile);

        log.info("登录成功 | 手机/标识: {} | 用户名: {} | IP: {}", requestPhone, user.getId(), ip);
        // 返回标准响应格式
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        response.getWriter().print(objectMapper.writeValueAsString(ResponseResult.success(loginResult)));
    }
}
