package com.starrysky.aries.security;

import com.starrysky.aries.entity.User;
import com.starrysky.aries.user.repository.UserRepository;
import com.starrysky.aries.util.Constant;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.HashSet;
import java.util.Set;

/**
 * AriesUserDetailService
 * 帐号密码登录
 *
 * @author adice
 * @version 1.0
 * @since 2025-08-08 14:05:24
 */
@Service
@Slf4j
public class AriesUserDetailsService implements UserDetailsService {

    @Resource
    private UserRepository userRepository;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public UserDetails loadUserByUsername(String phone) throws RuntimeException{
        if(!StringUtils.hasText(phone)){
            throw new UsernameNotFoundException("手机号不能为空");
        }
        User user = userRepository.findByPhone(phone);
        if (user == null) {
            throw new UsernameNotFoundException("手机号错误");
        }
        // 超过7天，旧账号解绑，新账号注册
        if (user.getIsDeleted() && ChronoUnit.DAYS.between(user.getDeleteTime(), LocalDateTime.now()) > Constant.EXPIRED_ACCOUNT_CLEANUP_DAYS) {
            throw new LockedException("您的账户已注销，请重新注册！");
        }
        // 若用户锁定，且锁定日期间隔小于1天，则抛出异常
        if (!user.isAccountNonLocked() && ChronoUnit.DAYS.between(user.getLockTime(), LocalDateTime.now()) < 1) {
            throw new LockedException("您的账户被锁定，请明天再试！");
        }
        Set<SimpleGrantedAuthority> authorities = new HashSet<>();
        user.setAuthorities(authorities);
        return user;
    }

}
