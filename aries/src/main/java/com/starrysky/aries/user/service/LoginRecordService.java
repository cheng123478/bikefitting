package com.starrysky.aries.user.service;

import com.starrysky.aries.entity.User;
import com.starrysky.aries.entity.UserLoginRecord;
import com.starrysky.aries.user.repository.UserLoginRecordRepository;
import com.starrysky.aries.user.repository.UserRepository;
import jakarta.annotation.Resource;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * LoginRecordService
 * 用户登录业务类
 *
 * @author adice
 * @version 1.0
 * @since 2025-07-01 11:07:49
 */
@Service
@Transactional
public class LoginRecordService {
    public static final int LOGIN_RECORD_STATE_SUCCESS = 1;
    public static final int LOGIN_RECORD_STATE_FAILURE = 2;
    @Resource
    private UserLoginRecordRepository userLoginRecordRepository;
    @Resource
    private UserRepository userRepository;

    /**
     * 新增用户登录记录
     * @param phone 手机号
     * @param ip ip地址
     * @param loginRecordState 登录记录状态
     */
    @Async
    public void addLoginRecord(String phone, String ip, int loginRecordState) {
        UserLoginRecord loginRecord = new UserLoginRecord();
        loginRecord.setState(loginRecordState);
        loginRecord.setLoginTime(LocalDateTime.now());
        loginRecord.setIp(ip);
        loginRecord.setPhone(phone);
        userLoginRecordRepository.save(loginRecord);
        if (loginRecordState == LOGIN_RECORD_STATE_SUCCESS) {
            User user = userRepository.findByPhone(phone);
            if (user.getLoginFailureCount() > 0) {
                user.setLoginFailureCount(0);
                userRepository.save(user);
            }
        }
    }

}
