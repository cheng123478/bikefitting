package com.starrysky.aries.advice;

import java.lang.annotation.*;

/**
 * 日志审计注解
 *
 * @author adi
 * @version 1.0
 * @since 2026/1/27 11:01
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface LogAudit {
    String value() default ""; // 操作描述，例如 "修改密码"
}
