package com.starrysky.aries.security;

import cn.hutool.core.date.LocalDateTimeUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.starrysky.aries.entity.User;
import com.starrysky.aries.user.service.LoginRecordService;
import com.starrysky.aries.user.service.UserService;
import com.starrysky.aries.util.Constant;
import com.starrysky.aries.util.IpUtil;
import com.starrysky.aries.util.ResponseResult;
import com.starrysky.aries.util.ResponseResultStatus;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

/**
 * AriesAuthenticationFailureHandler
 * 认证失败处理类
 *
 * @author adice
 * @version 1.0
 * @since 2025-07-01 11:07:49
 */
@Component
@Slf4j
public class AriesAuthenticationFailureHandler implements AuthenticationFailureHandler {
    @Resource
    private UserService userService;
    @Resource
    private LoginRecordService loginRecordService;
    @Resource
    private ObjectMapper objectMapper;

    @Override
    public void onAuthenticationFailure(HttpServletRequest request, HttpServletResponse response, AuthenticationException exception) throws IOException {
        // 登录失败超过规定次数，则锁定账号
        String phone = request.getParameter("phone");
        String errorMessage = exception.getLocalizedMessage();

        if (StringUtils.hasText(phone)) {
            if (exception instanceof BadCredentialsException) {
                User user = userService.getOne(phone);
                if (user != null) {
                    if(!user.isAccountNonLocked() && LocalDateTimeUtil.between(user.getLockTime(), LocalDateTime.now(), ChronoUnit.DAYS) > 1){
                        user.setLoginFailureCount(0);
                        user.setAccountNonLocked(true);
                        user.setLockTime(null);
                        log.info("用户账号自动解锁 | 手机号: {}", phone);
                    }
                    user.setLoginFailureCount(user.getLoginFailureCount() + 1);
                    errorMessage = "手机号或密码错误，还剩 " + (Constant.USER_LOCK_LOGIN_FAIL_COUNT - user.getLoginFailureCount()) + " 次尝试机会";
                    userService.editUser(user);
                }
            }
            loginRecordService.addLoginRecord(phone, IpUtil.getRequestIp(request), LoginRecordService.LOGIN_RECORD_STATE_FAILURE);
            log.warn("登录失败 | 手机号: {} | IP: {} | 原因: {}", phone, IpUtil.getRequestIp(request), exception.getMessage());
        } else {
            loginRecordService.addLoginRecord("WeChat_Login", IpUtil.getRequestIp(request), LoginRecordService.LOGIN_RECORD_STATE_FAILURE);
            log.warn("未知类型或微信登录失败 | IP: {} | 原因: {}", IpUtil.getRequestIp(request), exception.getMessage());
        }
        // 返回登录异常信息
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        response.getWriter().print(objectMapper.writeValueAsString(ResponseResult.failure(ResponseResultStatus.INTERNAL_SERVER_ERROR.getCode(), errorMessage)));
    }
}
