package com.starrysky.aries.config;

import com.starrysky.aries.redis.RedisConstant;
import org.springframework.boot.autoconfigure.cache.RedisCacheManagerBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;

import java.time.Duration;

/**
 * CacheConfig
 * Spring Cache Redis 差异化 TTL 配置
 * <p>
 * 为每个车队缓存分别设置过期时间，替代全局统一的 60s TTL，
 * 兼顾缓存命中率与数据新鲜度，同时通过 TTL 差异化天然防止缓存雪崩。
 *
 * @author adi
 * @version 1.0
 * @since 2026/06/24
 */
@Configuration
public class CacheConfig {

    /**
     * 按缓存名差异化配置 TTL
     * <ul>
     *   <li>list / my / members — 5 分钟（高频读 + 主动失效兜底）</li>
     *   <li>detail / garage — 10 分钟（访问量大 + 变更少）</li>
     *   <li>memberCount — 1 小时（RedisUtil 直写，此处仅作 Spring Cache 侧兜底声明）</li>
     * </ul>
     */
    @Bean
    public RedisCacheManagerBuilderCustomizer redisCacheManagerBuilderCustomizer() {
        return builder -> builder
                .withCacheConfiguration(RedisConstant.PELOTON_TEAMS_LIST,
                        RedisCacheConfiguration.defaultCacheConfig()
                                .entryTtl(Duration.ofSeconds(RedisConstant.PELOTON_TEAMS_LIST_TTL)))
                .withCacheConfiguration(RedisConstant.PELOTON_TEAMS_MY,
                        RedisCacheConfiguration.defaultCacheConfig()
                                .entryTtl(Duration.ofSeconds(RedisConstant.PELOTON_TEAMS_MY_TTL)))
                .withCacheConfiguration(RedisConstant.PELOTON_TEAMS_DETAIL,
                        RedisCacheConfiguration.defaultCacheConfig()
                                .entryTtl(Duration.ofSeconds(RedisConstant.PELOTON_TEAMS_DETAIL_TTL)))
                .withCacheConfiguration(RedisConstant.PELOTON_TEAMS_MEMBERS,
                        RedisCacheConfiguration.defaultCacheConfig()
                                .entryTtl(Duration.ofSeconds(RedisConstant.PELOTON_TEAMS_MEMBERS_TTL)))
                .withCacheConfiguration(RedisConstant.PELOTON_TEAMS_GARAGE,
                        RedisCacheConfiguration.defaultCacheConfig()
                                .entryTtl(Duration.ofSeconds(RedisConstant.PELOTON_TEAMS_GARAGE_TTL)));
    }
}
