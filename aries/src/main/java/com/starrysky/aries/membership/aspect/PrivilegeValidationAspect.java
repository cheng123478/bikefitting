package com.starrysky.aries.membership.aspect;

import com.starrysky.aries.entity.User;
import com.starrysky.aries.membership.annotation.RequirePrivilege;
import com.starrysky.aries.membership.exception.MembershipException;
import com.starrysky.aries.membership.service.UserMembershipService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;

/**
 * 用户权限、会员验证切面
 *
 * @author adi
 * @since 2025-12-23
 * @version 1.0
 */
@Aspect
@Component
@Slf4j
@RequiredArgsConstructor
public class PrivilegeValidationAspect {

    private final UserMembershipService userMembershipService;

    @Before("@annotation(com.starrysky.aries.membership.annotation.RequirePrivilege)")
    public void validatePrivilege(JoinPoint joinPoint) {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        RequirePrivilege requirePrivilege = method.getAnnotation(RequirePrivilege.class);

        if (requirePrivilege == null) {
            return;
        }

        Long userId = null;
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof User user) {
            userId = user.getId();
        }
        if (userId == null) {
            throw new MembershipException(401, "请先登录后再进行操作");
        }

        log.debug("AOP Validation -> UserId: {}, Privilege: {}", userId, requirePrivilege.value());
        
        // 调用底层服务进行校验和额度扣减
        userMembershipService.validateAndConsumePrivilege(userId, requirePrivilege.value());
    }
}
