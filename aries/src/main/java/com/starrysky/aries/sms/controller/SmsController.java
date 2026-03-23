package com.starrysky.aries.sms.controller;

import cn.hutool.core.util.PhoneUtil;
import com.starrysky.aries.redis.RedisConstant;
import com.starrysky.aries.sms.record.SmsResult;
import com.starrysky.aries.sms.service.SmsService;
import com.starrysky.aries.util.ResponseResult;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.util.ObjectUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.TimeUnit;

/**
 * 短信控制器
 *
 * @author adi
 * @version 1.0
 * @since 2025/11/25 16:20
 */

@RestController
@RequestMapping("/sms")
@Slf4j
public class SmsController {

    @Resource
    private SmsService smsService;

    @Resource
    private RedisTemplate<String, Object> redisTemplate;

    /**
     * 发送短信验证码
     *
     * @param phone 用户手机号
     * @return 发送结果
     */
    @GetMapping("/send")
    public ResponseResult<SmsResult> send(@RequestParam String phone) {
        if (!PhoneUtil.isPhone(phone)) {
            return ResponseResult.failure(400, "手机号格式不正确");
        }
        phone = phone.trim();
        // 检查发送频率限制
        String lastSendKey = RedisConstant.ALI_SMS_CODE_CACHE + "last_send:" + phone;
        String dailyCountKey = RedisConstant.ALI_SMS_CODE_CACHE + "daily_count:" + phone + ":" + LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE);

        // 检查60秒间隔限制
        Object lastSendTimeObj = redisTemplate.opsForValue().get(lastSendKey);
        if (!ObjectUtils.isEmpty(lastSendTimeObj)) {
            return ResponseResult.failure(429, "获取验证码过于频繁，请60秒后再试");
        }

        // 检查每日7次限制
        int dailyCount = 0;
        Object dailyCountObj = redisTemplate.opsForValue().get(dailyCountKey);
        if (!ObjectUtils.isEmpty(dailyCountObj)) {
            dailyCount = ((Number) dailyCountObj).intValue();
        }
        if (dailyCount >= 7) {
            return ResponseResult.failure(429, "今日验证码获取次数已达上限，请明天再试");
        }

        try {
            SmsResult result = smsService.sendSms(phone.trim());
            if (result.success()) {
                // 记录发送时间和次数
                redisTemplate.opsForValue().set(lastSendKey, System.currentTimeMillis(), 1, TimeUnit.MINUTES);
                redisTemplate.opsForValue().increment(dailyCountKey);
                if (dailyCount == 0) {
                    redisTemplate.expire(dailyCountKey, 24, TimeUnit.HOURS);
                }
                return ResponseResult.success(result);
            }
            return ResponseResult.failure(500, "短信发送失败" + result.message());
        } catch (Exception ex) {
            log.error("短信发送系统异常 | 手机号: {}", phone, ex);
            return ResponseResult.failure(500, "短信服务响应异常");
        }
    }
}

