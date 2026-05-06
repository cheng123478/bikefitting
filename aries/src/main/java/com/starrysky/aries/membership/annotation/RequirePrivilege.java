package com.starrysky.aries.membership.annotation;

import com.starrysky.aries.membership.enums.PrivilegeTypeEnum;

import java.lang.annotation.*;

/**
 * 特权验证注解
 * 标记在 Controller 方法上，配合 AOP 切面进行自动权限及次数扣减校验
 *
 * @author adi
 * @since 2025-12-23
 * @version 1.0
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface RequirePrivilege {
    
    /**
     * 要求的特权类型
     */
    PrivilegeTypeEnum value();
}
