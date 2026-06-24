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
     * 车队列表缓存（Spring Cache，key=分页+筛选条件，TTL=5min，写操作时 allEntries 清除）
     */
    public static final String PELOTON_TEAMS_LIST = "starrysky:aries:peloton:list";
    public static final int PELOTON_TEAMS_LIST_TTL = 5 * 60;

    /**
     * 我的车队缓存（Spring Cache，key=userId，TTL=5min，加入/退出时精确失效）
     */
    public static final String PELOTON_TEAMS_MY = "starrysky:aries:peloton:my";
    public static final int PELOTON_TEAMS_MY_TTL = 5 * 60;

    /**
     * 车队详情缓存（Spring Cache，key=teamId，TTL=10min，写操作时精确失效，支持空值防穿透）
     */
    public static final String PELOTON_TEAMS_DETAIL = "starrysky:aries:peloton:detail";
    public static final int PELOTON_TEAMS_DETAIL_TTL = 10 * 60;

    /**
     * 车队成员列表缓存（Spring Cache，key=teamId:page:size:keyword，TTL=5min，成员变动时清除）
     */
    public static final String PELOTON_TEAMS_MEMBERS = "starrysky:aries:peloton:members";
    public static final int PELOTON_TEAMS_MEMBERS_TTL = 5 * 60;

    /**
     * 车队车库缓存（Spring Cache，key=teamId，TTL=10min）
     */
    public static final String PELOTON_TEAMS_GARAGE = "starrysky:aries:peloton:garage";
    public static final int PELOTON_TEAMS_GARAGE_TTL = 10 * 60;

    /**
     * 车队成员数缓存（Redis String，key=teamId，value=成员数量）
     * 成员变动时主动删除，下次读取时从 DB 重新加载。TTL=1h 作为安全兜底。
     */
    public static final String PELOTON_TEAMS_MEMBER_COUNT = "starrysky:aries:peloton:memberCount:";
    /**
     * 车队成员数缓存过期时间（秒），1小时安全兜底
     */
    public static final int PELOTON_TEAMS_MEMBER_COUNT_TTL = 60 * 60;
}
