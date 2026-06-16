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

    public static final String MEM_DYNAMIC_QUOTA = "starrysky:aries:mem:dynamic:quota:";

    public static final String ALI_SMS_CODE_CACHE = "starrysky:aries:sms:";
    public static final int ALI_SMS_CODE_TIMEOUT = 60 * 5;

    public static final String CAPTCHA = "starrysky:aries:captcha:";

    // 分布式锁相关常量
    public static final String LOCK_PREFIX = "starrysky:aries:lock:";
    public static final int LOCK_EXPIRE_TIME = 30; // 锁过期时间（秒）
    public static final int LOCK_RETRY_TIMES = 3; // 锁重试次数
    public static final long LOCK_RETRY_INTERVAL = 100; // 锁重试间隔（毫秒）

    // 推荐缓存相关常量
    /**
     * 推荐热度分缓存 key（Hash结构，field=车型名称，value=BikeRecommendCacheDTO的JSON）
     */
    public static final String RECOMMEND_CACHE = "starrysky:aries:recommend:cache";
    /**
     * 推荐缓存过期时间（秒），默认24小时
     */
    public static final int RECOMMEND_CACHE_TIMEOUT = 60 * 60 * 24;

    /**
     * 跨高数据缓存 key（String结构，value=Map<String, List<BikeStandOverCacheItem>>的JSON）
     * 纯静态几何数据，设7天TTL作为安全兜底，车型数据更新时主动清除
     */
    public static final String RECOMMEND_STAND_OVER_CACHE = "starrysky:aries:recommend:stand_over";
    /**
     * 跨高数据缓存过期时间（秒），7天安全兜底
     */
    public static final int RECOMMEND_STAND_OVER_CACHE_TTL = 60 * 60 * 24 * 7;

    // 车队缓存相关常量
    /**
     * 车队列表缓存（Spring Cache，key=分页+筛选条件，TTL=60s自动过期）
     */
    public static final String PELOTON_TEAMS_LIST = "peloton:teams:list";
    /**
     * 我的车队缓存（Spring Cache，key=userId）
     */
    public static final String PELOTON_TEAMS_MY = "peloton:teams:my";
    /**
     * 车队详情缓存（Spring Cache，key=teamId）
     */
    public static final String PELOTON_TEAMS_DETAIL = "peloton:teams:detail";
    /**
     * 车队成员列表缓存（Spring Cache，key=teamId:page:size:keyword）
     */
    public static final String PELOTON_TEAMS_MEMBERS = "peloton:teams:members";
    /**
     * 车队车库缓存（Spring Cache，key=teamId）
     */
    public static final String PELOTON_TEAMS_GARAGE = "peloton:teams:garage";
}
