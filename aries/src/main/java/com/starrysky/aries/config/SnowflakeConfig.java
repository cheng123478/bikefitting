package com.starrysky.aries.config;

import cn.hutool.core.lang.Snowflake;
import cn.hutool.core.util.IdUtil;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 雪花算法配置类
 *
 * @author adi
 * @version 1.0
 * @since 2026/2/1 20:36
 */
@Configuration
public class SnowflakeConfig {
    @Value("${aries.snowflake.worker-id}")
    private long workerId;
    @Value("${aries.snowflake.datacenter-id}")
    private long dataCenterId;

    @Bean
    public Snowflake Snowflake() {
        return IdUtil.getSnowflake(workerId, dataCenterId);
    }
}
