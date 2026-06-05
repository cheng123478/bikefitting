package com.starrysky.aries.advice;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.starrysky.aries.util.ResponseResult;
import jakarta.annotation.Resource;
import org.jetbrains.annotations.NotNull;
import org.springframework.core.MethodParameter;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyAdvice;

/**
 * ResponseResultAdvice
 * 响应的通知类
 * 实现ResponseBodyAdvice接口，实现对响应的数据包装
 *
 * @author adice
 * @version 1.0
 * @since 2025-07-01 11:07:49
 */
@RestControllerAdvice(basePackages = "com.starrysky.aries")
public class ResponseResultAdvice implements ResponseBodyAdvice<Object> {
    @Resource
    private ObjectMapper objectMapper;

    @Override
    public boolean supports(MethodParameter returnType, @NotNull Class<? extends HttpMessageConverter<?>> converterType) {
        // 如果返回值类型已经是 ResponseResult，不包装
        if (returnType.getParameterType().equals(ResponseResult.class)) {
            return false;
        }
        // 如果方法上加了 @NoResponseWrap，不包装
        if (returnType.hasMethodAnnotation(NoResponseWrap.class)) {
            return false;
        }
        // 如果Controller类上加了 @NoResponseWrap，不包装
        return !returnType.getContainingClass().isAnnotationPresent(NoResponseWrap.class);
    }

    @NotNull
    @Override
    public Object beforeBodyWrite(Object body, @NotNull MethodParameter returnType, @NotNull MediaType selectedContentType,
                                  @NotNull Class<? extends HttpMessageConverter<?>> selectedConverterType,
                                  @NotNull ServerHttpRequest request, @NotNull ServerHttpResponse response) {

        // 1. 如果返回值是 String，Spring 默认使用 StringHttpMessageConverter
        if (body instanceof String) {
            try {
                return objectMapper.writeValueAsString(ResponseResult.success(body));
            } catch (JsonProcessingException e) {
                throw new RuntimeException("响应结果序列化异常", e);
            }
        }

        // 2. 已经是 ResponseResult 类型（通常是异常处理器返回的），直接返回
        if (body instanceof ResponseResult) {
            return body;
        }

        // 3. 正常包装
        return ResponseResult.success(body);
    }
}
