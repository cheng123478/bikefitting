package com.starrysky.aries.redis;

/**
 * RedisConstant
 * Redis常量配置
 *
 * @author adice
 * @version 1.0
 * @since 2025-07-01 11:07:49
 */
public class RedisConstant {
    public static final String AUTHENTICATION = "starrysky:aries:authen:";
    public static final int AUTHENTICATION_TIMEOUT = 60 * 60 * 24 * 7;
//    public static final String REFRESH_TOKEN = "starrysky:aries:refresh:";
//    public static final int REFRESH_TOKEN_TIMEOUT = 60 * 60 * 24 * 7;
    public static final String ALI_SMS_CODE_CACHE = "starrysky:aries:sms:";
    public static final int ALI_SMS_CODE_TIMEOUT = 60 * 5;

    public static final String CAPTCHA = "starrysky:aries:captcha:";

    // 分布式锁相关常量
    public static final String LOCK_PREFIX = "starrysky:aries:lock:";
    public static final int LOCK_EXPIRE_TIME = 30; // 锁过期时间（秒）
    public static final int LOCK_RETRY_TIMES = 3; // 锁重试次数
    public static final long LOCK_RETRY_INTERVAL = 100; // 锁重试间隔（毫秒）
}
