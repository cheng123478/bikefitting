package com.starrysky.aries.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.starrysky.aries.security.*;
import com.starrysky.aries.security.jwt.JwtFilter;
import com.starrysky.aries.security.sms.SmsCodeAuthenticationFilter;
import com.starrysky.aries.security.sms.SmsCodeAuthenticationProvider;
import com.starrysky.aries.security.wechat.WeChatAuthenticationFilter;
import jakarta.annotation.Resource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.annotation.web.configurers.HeadersConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.authentication.logout.LogoutFilter;

import java.util.List;

/**
 * SecurityConfig
 * SpringSecurity框架配置
 *
 * @author adice
 * @version 1.0
 * @since 2025-07-01 11:07:49
 */
@Configuration
public class SecurityConfig {

    @Resource
    private AriesAuthenticationSuccessHandler authenticationSuccessHandler;
    @Resource
    private AriesAuthenticationFailureHandler authenticationFailureHandler;
    @Resource
    private AriesLogoutSuccessHandler logoutSuccessHandler;
    @Resource
    private AriesAuthenticationEntryPoint authenticationEntryPoint;
    @Resource
    private AriesAccessDeniedHandler accessDeniedHandler;
    @Resource
    private JwtFilter jwtFilter;
    @Resource
    private AriesUserDetailsService userDetailsService;
    @Resource
    private ObjectMapper objectMapper;

    /**
     * 默认帐号密码登录 Provider
     */
    @Bean
    public DaoAuthenticationProvider authenticationProvider(PasswordEncoder passwordEncoder) {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider(userDetailsService);
        provider.setHideUserNotFoundExceptions(false);
        provider.setPasswordEncoder(passwordEncoder);
        return provider;
    }

    /**
     * Spring Security 6 核心配置
     * 注意：这里注入了 AuthenticationConfiguration
     */
    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http,
                                            DaoAuthenticationProvider daoAuthenticationProvider,
                                            SmsCodeAuthenticationProvider smsCodeAuthenticationProvider) throws Exception {

        // 获取全局 AuthenticationManager
        AuthenticationManager myAuthenticationManager = new ProviderManager(
                List.of(daoAuthenticationProvider, smsCodeAuthenticationProvider)
        );

        // 在方法内部创建 Filter 实例（不要用 @Bean），并手动设置 Manager
        RSAUsernamePasswordAuthenticationFilter rsaFilter = new RSAUsernamePasswordAuthenticationFilter();
        rsaFilter.setAuthenticationManager(myAuthenticationManager);
        rsaFilter.setAuthenticationSuccessHandler(authenticationSuccessHandler);
        rsaFilter.setAuthenticationFailureHandler(authenticationFailureHandler);
        rsaFilter.setObjectMapper(objectMapper);
        rsaFilter.setFilterProcessesUrl("/user/login");

        SmsCodeAuthenticationFilter smsFilter = new SmsCodeAuthenticationFilter();
        smsFilter.setAuthenticationManager(myAuthenticationManager);
        smsFilter.setAuthenticationSuccessHandler(authenticationSuccessHandler);
        smsFilter.setAuthenticationFailureHandler(authenticationFailureHandler);
        smsFilter.setObjectMapper(objectMapper);
        smsFilter.setFilterProcessesUrl("/user/login/sms");

        WeChatAuthenticationFilter wechatFilter = new WeChatAuthenticationFilter();
        wechatFilter.setAuthenticationManager(myAuthenticationManager);
        wechatFilter.setAuthenticationSuccessHandler(authenticationSuccessHandler);
        wechatFilter.setAuthenticationFailureHandler(authenticationFailureHandler);
        wechatFilter.setObjectMapper(objectMapper);
        wechatFilter.setFilterProcessesUrl("/user/login/wechat");

        http.authenticationProvider(daoAuthenticationProvider)
                .authenticationProvider(smsCodeAuthenticationProvider)
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/user/login", "/user/login/sms", "/user/login/wechat", "/user/refresh",
                                "/error", "/sms/send", "/resource/**", 
                                "/bike-model/showroom", "/bike-model/{id}", "/bike-model/model/**",
                                "/team/list", "/team/stats", "/team/{teamId}/members", "/team/{teamId}/garage").permitAll()
                        .requestMatchers(HttpMethod.GET, "/team/{teamId}").permitAll()
                        .requestMatchers("/user/logout").authenticated()
                        .anyRequest().authenticated()
                )
                .logout(logoutConfigurer -> logoutConfigurer
                        .logoutUrl("/user/logout")
                        .logoutSuccessHandler(logoutSuccessHandler)
                        .clearAuthentication(true)
                )
                // 将手动创建的 Filter 添加到链中
                .addFilterBefore(smsFilter, UsernamePasswordAuthenticationFilter.class)
                .addFilterBefore(wechatFilter, UsernamePasswordAuthenticationFilter.class)
                .addFilterAt(rsaFilter, UsernamePasswordAuthenticationFilter.class)
                .addFilterBefore(jwtFilter, LogoutFilter.class)

                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(sessionManagementConfigurer -> sessionManagementConfigurer
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                )
                .exceptionHandling(exceptionHandlingConfigurer -> exceptionHandlingConfigurer
                        .authenticationEntryPoint(authenticationEntryPoint)
                        .accessDeniedHandler(accessDeniedHandler)
                )
                .headers(headersConfigurer -> headersConfigurer
                        .frameOptions(HeadersConfigurer.FrameOptionsConfig::sameOrigin)
                );

        return http.build();
    }
}
