package com.starrysky.aries.config;

import org.apache.hc.client5.http.config.ConnectionConfig;
import org.apache.hc.client5.http.config.RequestConfig;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClientBuilder;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManager;
import org.apache.hc.core5.util.Timeout;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

import java.util.concurrent.TimeUnit;


/**
 * RestTemplateConfig
 * 用于调用发送短信、AI等
 *
 * @author adi
 * @version 1.0
 * @since 2025-08-08 11:19:57
 */
@Configuration
public class RestTemplateConfig {

    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate(clientHttpRequestFactory());
    }

    @Bean
    public ClientHttpRequestFactory clientHttpRequestFactory() {
        // 1. 配置连接层参数
        ConnectionConfig connectionConfig = ConnectionConfig.custom()
                // 建立TCP连接的超时时间
                .setConnectTimeout(Timeout.of(3000, TimeUnit.MILLISECONDS))
                // 读取数据的超时时间 (即原来的 SocketTimeout)
                .setSocketTimeout(Timeout.of(5000, TimeUnit.MILLISECONDS))
                .build();

        // 2. 配置连接管理器
        PoolingHttpClientConnectionManager connectionManager = new PoolingHttpClientConnectionManager();
        connectionManager.setMaxTotal(200);
        connectionManager.setDefaultMaxPerRoute(50);
        connectionManager.setDefaultConnectionConfig(connectionConfig);

        // 3. 配置请求层参数
        RequestConfig requestConfig = RequestConfig.custom()
                .setConnectionRequestTimeout(Timeout.of(1000, TimeUnit.MILLISECONDS))
                .build();

        // 4. 构建 HttpClient
        CloseableHttpClient httpClient = HttpClientBuilder.create()
                .setConnectionManager(connectionManager)
                .setDefaultRequestConfig(requestConfig)
                .evictExpiredConnections()
                .evictIdleConnections(Timeout.of(30, TimeUnit.SECONDS))
                .build();

        // 5. 封装
        return new HttpComponentsClientHttpRequestFactory(httpClient);
    }
}