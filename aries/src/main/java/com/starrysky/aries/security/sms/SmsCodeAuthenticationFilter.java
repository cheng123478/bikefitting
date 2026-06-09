package com.starrysky.aries.security.sms;

import cn.hutool.core.util.PhoneUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationServiceException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.AbstractAuthenticationProcessingFilter;

import java.io.IOException;
import java.util.Map;

/**
 * SmsCodeAuthenticationFilter
 * 短信验证过滤器
 *
 * @author adice
 * @version 1.0
 * @since 2025-07-01 11:07:49
 */
@Setter
@Getter
@Slf4j
public class SmsCodeAuthenticationFilter extends AbstractAuthenticationProcessingFilter {

    private ObjectMapper objectMapper;

    public static final String SPRING_SECURITY_PHONE_KEY = "phone";
    public static final String SPRING_SECURITY_CODE_KEY = "code";
    private String phoneParameter = SPRING_SECURITY_PHONE_KEY;
    private String codeParameter = SPRING_SECURITY_CODE_KEY;

    public SmsCodeAuthenticationFilter() {
        super(request -> request.getRequestURI().equals("/user/login/sms") && request.getMethod().equals("POST"));
    }

    @Override
    public Authentication attemptAuthentication(HttpServletRequest request,
                                                HttpServletResponse response) throws AuthenticationException {
        String phone;
        String code;
        if (request.getContentType() != null && (request.getContentType().contains(MediaType.APPLICATION_JSON_VALUE))) {
            try {
                Map<String, String> map = objectMapper.readValue(request.getInputStream(),
                        objectMapper.getTypeFactory().constructType(Map.class));
                phone = map.get(phoneParameter);
                code = map.get(codeParameter);
            } catch (IOException e) {
                log.warn("解析登录请求JSON失败", e);
                throw new AuthenticationServiceException("无法解析 JSON 请求体");
            }
        } else {
            phone = request.getParameter(phoneParameter);
            code = request.getParameter(codeParameter);
        }
        phone = phone == null ? "" : phone.trim();
        code = code == null ? "" : code.trim();
        if (!PhoneUtil.isPhone(phone)) {
            throw new AuthenticationServiceException("手机号格式不正确");
        }
        SmsCodeAuthenticationToken authRequest = new SmsCodeAuthenticationToken(phone, code);
        authRequest.setDetails(authenticationDetailsSource.buildDetails(request));
        return this.getAuthenticationManager().authenticate(authRequest);
    }
}
