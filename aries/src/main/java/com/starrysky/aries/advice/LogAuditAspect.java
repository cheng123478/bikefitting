package com.starrysky.aries.advice;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.starrysky.aries.entity.User;
import com.starrysky.aries.util.IpUtil;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.multipart.MultipartFile;

import java.util.Arrays;

/**
 * 操作审计切面
 *
 * @author adi
 * @version 1.0
 * @since 2026/1/30 16:34
 */
@Aspect
@Component
@Slf4j
public class LogAuditAspect {
    @Resource
    private ObjectMapper objectMapper;

    @Around("@annotation(logAudit)")
    public Object around(ProceedingJoinPoint point, LogAudit logAudit) throws Throwable {
        long startTime = System.currentTimeMillis();
        Object result;
        String errorMsg = null;

        try {
            // 执行目标方法
            result = point.proceed();
            return result;
        } catch (Throwable e) {
            errorMsg = e.getMessage();
            throw e;
        } finally {
            // 无论成功失败，都记录日志（异步记录更好，这里演示同步）
            recordLog(point, logAudit, System.currentTimeMillis() - startTime, errorMsg);
        }
    }
    private void recordLog(ProceedingJoinPoint point, LogAudit logAudit, long costTime, String errorMsg) {
        try {
            // 1. 获取请求上下文
            ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            HttpServletRequest request = attributes != null ? attributes.getRequest() : null;

            // 2. 获取当前用户
            String id = "Anonymous";
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication != null && authentication.getPrincipal() instanceof User) {
                id = ((User) authentication.getPrincipal()).getId().toString(); // 或者 getPhone()
            }

            // 3. 处理参数 (过滤掉文件流等无法序列化的对象)
            Object[] args = point.getArgs();
            String params;
            // 简单过滤：如果参数包含 MultipartFile，不序列化
            boolean hasFile = Arrays.stream(args).anyMatch(arg -> arg instanceof MultipartFile || arg instanceof MultipartFile[]);
            if (!hasFile) {
                try {
                    params = objectMapper.writeValueAsString(args);
                } catch (Exception e) {
                    params = "args serialization failed";
                }
            } else {
                params = "[File Upload]";
            }

            // 4. 打印格式化日志
            // 格式：[审计] | 用户 | IP | 操作 | 耗时 | 状态 | 参数
            log.info("[AUDIT] | User:{} | IP:{} | Action:{} | Cost:{}ms | Status:{} | Params:{}",
                    id,
                    IpUtil.getRequestIp(request),
                    logAudit.value(),
                    costTime,
                    errorMsg == null ? "Success" : "Fail: " + errorMsg,
                    params
            );

        } catch (Exception e) {
            log.error("审计日志记录异常", e);
        }
    }
}
