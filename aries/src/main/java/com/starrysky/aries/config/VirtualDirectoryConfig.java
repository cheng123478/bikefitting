package com.starrysky.aries.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * VirtualDirectoryConfig
 * 虚拟路径配置
 *
 * @author adice
 * @version 1.0
 * @since 2025-07-01 11:07:49
 */
@Configuration
public class VirtualDirectoryConfig implements WebMvcConfigurer {
    @Value("${file.resource.location}")
    private String resourceLocation;
    /* 显示相对地址 */
    @Value("${file.resource.relative.path}")
    private String resourceRelativePath;

    /**
     * 配置静态资源映射时，务必注意：
     * 1. 生产环境建议通过 Nginx 等反向代理处理静态资源，而非直接映射。
     * 2. 必须确保操作系统层面对 resourceLocation 目录有严格的读写权限控制。
     * 3. 避免在 resourceLocation 及其子目录中存放敏感配置文件。
     */
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler(resourceRelativePath)
                .addResourceLocations("file:" + resourceLocation)
                .setCachePeriod(3600) // 设置缓存以减少磁盘 IO，间接提升安全性
                .resourceChain(true);
        WebMvcConfigurer.super.addResourceHandlers(registry);
    }
}
