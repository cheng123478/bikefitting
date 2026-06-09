package com.starrysky.aries.advice;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * NoResponseWrap
 * 标记在Controller类或方法上，表示该接口的返回值不需要被全局包装
 *
 * @author adi
 * @version 1.0
 * @since 2026/1/27 11:01
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface NoResponseWrap {
}