package com.starrysky.aries.security;

import cn.hutool.core.util.PhoneUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.starrysky.aries.util.EncryptUtil;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationServiceException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.AbstractAuthenticationProcessingFilter;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.util.Map;

/**
 * 处理JSON格式的用户名密码登录，并进行RSA解密
 *
 * @author adi
 * @version 1.0
 * @since 2026/1/25 23:12
 */
@Setter
@Getter
@Slf4j
public class RSAUsernamePasswordAuthenticationFilter extends AbstractAuthenticationProcessingFilter {

    private ObjectMapper objectMapper;

    public static final String SPRING_SECURITY_PHONE_KEY = "phone";
    public static final String SPRING_SECURITY_PASSWORD_KEY = "password";
    private String phoneParameter = SPRING_SECURITY_PHONE_KEY;
    private String passwordParameter = SPRING_SECURITY_PASSWORD_KEY;

    public RSAUsernamePasswordAuthenticationFilter() {
        super(request -> request.getRequestURI().equals("/user/login") && request.getMethod().equals("POST"));
    }

    @Override
    public Authentication attemptAuthentication(HttpServletRequest request,
                                                HttpServletResponse response) throws AuthenticationException {
        String phone;
        String password;
        if (request.getContentType() != null && (request.getContentType().contains(MediaType.APPLICATION_JSON_VALUE))) {
            try {
                Map<String, String> map = objectMapper.readValue(request.getInputStream(),
                        objectMapper.getTypeFactory().constructType(Map.class));
                phone = map.get(phoneParameter);
                password = map.get(passwordParameter);
            } catch (IOException e) {
                log.warn("解析登录请求JSON失败", e);
                throw new AuthenticationServiceException("无法解析 JSON 请求体");
            }
        } else {
            phone = request.getParameter(phoneParameter);
            password = request.getParameter(passwordParameter);
        }
        phone = phone == null ? "" : phone.trim();
        password = password == null ? "" : password.trim();
        if (!PhoneUtil.isPhone(phone)) {
            throw new AuthenticationServiceException("手机号格式不正确");
        }
        if(StringUtils.isEmpty(password)){
            throw new AuthenticationServiceException("密码不能为空");
        }
        try {
            password = EncryptUtil.decrypt(password.trim());
        } catch (Exception e) {
            logger.warn("密码解密失败！", e);
        }
        UsernamePasswordAuthenticationToken authRequest = new UsernamePasswordAuthenticationToken(phone, password);
        authRequest.setDetails(authenticationDetailsSource.buildDetails(request));
        return this.getAuthenticationManager().authenticate(authRequest);
    }
}