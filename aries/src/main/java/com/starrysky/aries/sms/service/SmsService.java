package com.starrysky.aries.sms.service;

import cn.hutool.core.util.RandomUtil;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.starrysky.aries.redis.RedisConstant;
import com.starrysky.aries.redis.RedisUtil;
import com.starrysky.aries.sms.record.SmsResponse;
import com.starrysky.aries.sms.record.SmsResult;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * 短信相关业务逻辑
 *
 * @author adi
 * @version 1.0
 * @since 2026/1/26 11:27
 */
@Service
@Slf4j
public class SmsService {
    @Value("${sms.base-url}")
    private String SMS_URL;
    // 从配置文件注入API密钥
    @Value("${sms.key}")
    private String apiKey;
    // 从配置文件注入短信模板ID
    @Value("${sms.tpl-id}")
    private String tplId;
    @Resource
    private RestTemplate restTemplate;
    @Resource
    private RedisUtil redisUtil;
    @Resource
    private ObjectMapper objectMapper;

    /**
     * 发送短信验证码
     *
     * @param phone 手机号
     * @return 响应结果
     */
    public SmsResult sendSms(String phone) {
        // 构建完整的请求URL
        String url = SMS_URL + "/sms/send";
        String code = generateCode();

        try {
            // 创建请求参数
            MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
            params.add("key", apiKey);
            params.add("mobile", phone);
            params.add("tpl_id", tplId);
            params.add("vars", objectMapper.writeValueAsString(Map.of("code", code)));

            // 创建请求头，设置为x-www-form-urlencoded类型
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

            SmsResponse response = restTemplate.postForObject(url, new HttpEntity<>(params, headers), SmsResponse.class);
            if (!ObjectUtils.isEmpty(response) && response.isSuccess()) {
                log.info("短信发送成功 | 手机号: {}", phone);
                cacheSmsCode(phone, code);
                return new SmsResult(true, code, "发送成功");
            }else {
                String reason = !ObjectUtils.isEmpty(response) ? response.reason() : "第三方服务无响应";
                log.warn("短信发送失败 | 手机号: {} | 原因: {}", phone, reason);
                return new SmsResult(false, null, reason);
            }
        } catch (JsonProcessingException e) {
            log.error("短信参数序列化异常", e);
            return new SmsResult(false, null, "系统配置异常");
        } catch (RestClientException e) {
            log.error("短信服务网络调用异常 | 手机号: {}", phone, e);
            return new SmsResult(false, null, "第三方服务通信故障");
        }
    }

    /**
     * 生成6位随机数字验证码
     *
     * @return 验证码
     */
    private String generateCode() {
        return RandomUtil.randomNumbers(6);
    }


    /**
     * 验证短信验证码
     *
     * @param phone 手机号
     * @param code  验证码
     * @return 验证结果
     */
    public boolean validateSmsCode(String phone, String code) {
        if (!StringUtils.hasText(phone) || !StringUtils.hasText(code)) {
            return false;
        }

        String cacheKey = RedisConstant.ALI_SMS_CODE_CACHE + phone;
        String redisCode = redisUtil.getStringRedisTemplate().opsForValue().get(cacheKey);
        // 如果 Redis 中没有，或者验证码不匹配
        if (!StringUtils.hasText(redisCode) || !redisCode.equals(code)) {
            return false;
        }

        // 验证通过，立即删除（保证一次性有效）
        redisUtil.getStringRedisTemplate().delete(cacheKey);
        return true;
    }

    /**
     * 缓存短信验证码
     *
     * @param phone 手机号
     * @param code  验证码
     */
    private void cacheSmsCode(String phone, String code) {
        String cacheKey = RedisConstant.ALI_SMS_CODE_CACHE + phone;
        redisUtil.getStringRedisTemplate().opsForValue()
                .set(cacheKey, code, RedisConstant.ALI_SMS_CODE_TIMEOUT, TimeUnit.MINUTES);
    }
}
