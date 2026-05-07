package com.starrysky.aries.advice;

import com.starrysky.aries.exception.BizException;
import com.starrysky.aries.membership.exception.MembershipException;
import com.starrysky.aries.util.IpUtil;
import com.starrysky.aries.util.ResponseResult;
import com.starrysky.aries.util.ResponseResultStatus;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;

import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * 全局异常处理
 *
 * @author adi
 * @version 1.0
 * @since 2026/1/27 11:01
 */
@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {
    /**
     * 处理自定义业务异常
     * 业务异常通常是预期的，日志级别可以用 warn，不需要打印堆栈
     */
    @ExceptionHandler(value = BizException.class)
    public ResponseResult<Void> bizExceptionHandler(HttpServletRequest request, BizException e) {
        log.warn("[业务异常] URI: {} | Code: {} | Msg: {}",
                request.getRequestURI(), e.getCode(), e.getMessage());
        return ResponseResult.failure(e.getCode(), e.getMessage());
    }

    /**
     * 处理会员权限异常
     */
    @ExceptionHandler(value = MembershipException.class)
    public ResponseResult<Void> membershipExceptionHandler(HttpServletRequest request, MembershipException e) {
        log.warn("[权限不足异常] URI: {} | Code: {} | Msg: {}",
                request.getRequestURI(), e.getCode(), e.getMessage());
        return ResponseResult.failure(e.getCode() != null ? e.getCode() : 40301, e.getMessage());
    }

    /**
     * 处理参数校验异常(使用@Valid或@Validated触发)
     * 场景：手机号为空、密码长度不够
     * 日志策略：WARN，不打印堆栈
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseResult<Void> handleValidationExceptions(HttpServletRequest request, MethodArgumentNotValidException e) {
        String msg = e.getBindingResult().getAllErrors().get(0).getDefaultMessage();
        log.warn("[参数校验失败] URI: {} | Msg: {}", request.getRequestURI(), msg);
        return ResponseResult.failure(ResponseResultStatus.BAD_REQUEST.getCode(), msg);
    }

    /**
     * 处理系统未知异常
     * 必须打印堆栈信息 e，否则很难排查 bug
     */
    @ExceptionHandler(value = Exception.class)
    public ResponseResult<Void> exceptionHandler(HttpServletRequest request, Exception e) {
        log.error("[系统未捕获异常] URI: {} | Method: {} | IP: {}",
                request.getRequestURI(),
                request.getMethod(),
                IpUtil.getRequestIp(request),
                e);
        return ResponseResult.failure(ResponseResultStatus.INTERNAL_SERVER_ERROR.getCode(), "系统繁忙，请稍后重试");
    }
}
