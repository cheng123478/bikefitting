package com.starrysky.aries.filter;

import jakarta.servlet.*;
import org.slf4j.MDC;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.UUID;

/**
 * 日志链路追踪过滤器
 * 优先级最高，确保请求一开始就有 traceId
 *
 * @author adi
 * @version 1.0
 * @since 2026/1/30 13:36
 */
@Component
@Order(-101) // 保证在 Spring Security 过滤器链之前执行
public class LogTraceFilter implements Filter {

    private static final String TRACE_ID = "traceId";

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        try {
            // 1. 生成或获取 Trace ID
            String traceId = UUID.randomUUID().toString().replace("-", "");
            // 2. 放入 MDC (Mapped Diagnostic Context)
            MDC.put(TRACE_ID, traceId);

            // 可选：把 traceId 放入 Header 返回给前端，方便前端反馈问题时提供 ID
            // ((HttpServletResponse) response).setHeader(TRACE_ID, traceId);

            chain.doFilter(request, response);
        } finally {
            // 3. 必须清除，防止线程池复用导致 Trace ID 污染
            MDC.clear();
        }
    }
}
