package com.starrysky.aries.util;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;

import java.net.InetAddress;
import java.net.UnknownHostException;

/**
 * IpUtil
 * IP工具类
 *
 * @author adice
 * @version 1.0
 * @since 2025-07-01 11:07:49
 */
@Slf4j
public class IpUtil {
    /*
    如果你的应用部署在 **Nginx** 后面，请确保 Nginx 配置中包含了以下内容，否则后端无法获取到真实 IP：
    proxy_set_header X-Real-IP $remote_addr;
    proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
    */
    private static final String UNKNOWN = "unknown";
    private static final String LOCALHOST_IPV4 = "127.0.0.1";
    private static final String LOCALHOST_IPV6 = "0:0:0:0:0:0:0:1";

    /**
     * 获取请求真实IP
     *
     * @param request 请求
     * @return IP
     */
    public static String getRequestIp(HttpServletRequest request) {
        if (request == null) {
            return UNKNOWN;
        }
        String ip = request.getHeader("x-forwarded-for");
        if (isInvalid(ip)) {
            ip = request.getHeader("Proxy-Client-IP");
        }
        if (isInvalid(ip)) {
            ip = request.getHeader("WL-Proxy-Client-IP");
        }
        if (isInvalid(ip)) {
            ip = request.getHeader("X-Real-IP");
        }
        if (isInvalid(ip)) {
            ip = request.getRemoteAddr();
            // 如果是回环地址，获取本机的真实IP
            if (LOCALHOST_IPV4.equals(ip) || LOCALHOST_IPV6.equals(ip)) {
                ip = getLocalHostIp();
            }
        }

        // 对于通过多个代理的情况，第一个IP为客户端真实IP，多个IP按照','分割
        if (ip != null && ip.contains(",")) {
            ip = ip.split(",")[0].trim();
        }

        return ip == null ? UNKNOWN : ip;
    }

    /**
     * 判断IP是否无效
     *
     * @param ip IP
     * @return 是否无效
     */
    private static boolean isInvalid(String ip) {
        return ip == null || ip.isEmpty() || UNKNOWN.equalsIgnoreCase(ip);
    }

    /**
     * 获取本机IP
     *
     * @return 本机IP
     */
    private static String getLocalHostIp() {
        try {
            return InetAddress.getLocalHost().getHostAddress();
        } catch (UnknownHostException e) {
            log.error("获取本机IP失败", e);
            return LOCALHOST_IPV4;
        }
    }
}
