package com.starrysky.aries.advice;

import java.lang.annotation.*;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface LogAudit {
    String value() default ""; // 操作描述，例如 "修改密码"
}
