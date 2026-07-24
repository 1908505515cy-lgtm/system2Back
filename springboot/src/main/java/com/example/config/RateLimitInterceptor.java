package com.example.config;

import com.example.common.Result;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * 全局 API 限流拦截器（Caffeine 滑动窗口）
 * <p>
 * 两级限流：
 * 1. 全局默认：每 IP 每分钟最多 120 次请求
 * 2. 端点特定：如 /login 每分钟最多 10 次
 * <p>
 * 使用 Caffeine 缓存自动过期，无内存泄漏。
 */
@Component
public class RateLimitInterceptor implements HandlerInterceptor {

    @Value("${rate-limit.global:120}")
    private int globalMaxRequests;

    @Value("${rate-limit.window-seconds:60}")
    private int windowSeconds;

    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * 端点特定限流配置：key = URL 路径模式, value = 最大请求数
     * 未列出的端点使用全局默认限流
     */
    private static final Map<String, Integer> ENDPOINT_LIMITS = Map.of(
            "/login", 10,
            "/refresh", 20,
            "/file/upload", 30,
            "/register", 5,
            "/forgot-password", 10
    );

    /**
     * 外层缓存：IP → (限流Key → 请求计数缓存)
     * 外层 10 分钟无访问自动驱逐（清理不活跃 IP）
     * 内层缓存每条记录 TTL = 窗口时间（滑动窗口效果）
     */
    private final Cache<String, Cache<String, Boolean>> rateLimits = Caffeine.newBuilder()
            .expireAfterAccess(10, TimeUnit.MINUTES)
            .maximumSize(10_000)
            .build();

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        String ip = getClientIp(request);
        String uri = request.getRequestURI();

        // 检查全局限流
        if (!checkRateLimit(ip, "global", globalMaxRequests)) {
            return reject(response, "请求过于频繁，请稍后再试");
        }

        // 检查端点特定限流
        for (Map.Entry<String, Integer> entry : ENDPOINT_LIMITS.entrySet()) {
            if (uri.startsWith(entry.getKey())) {
                if (!checkRateLimit(ip, entry.getKey(), entry.getValue())) {
                    return reject(response, "该接口请求过于频繁，请稍后再试");
                }
                break;
            }
        }

        return true;
    }

    /**
     * 检查指定 IP + 限流 Key 是否超出限制
     * 利用 Caffeine 缓存 TTL 实现滑动窗口效果：
     * 每次请求插入一条带 TTL 的记录，缓存中的记录数即为窗口内的请求数
     */
    private boolean checkRateLimit(String ip, String limitKey, int maxRequests) {
        String compositeKey = ip + ":" + limitKey;
        Cache<String, Boolean> requestCache = rateLimits.get(compositeKey, k ->
                Caffeine.newBuilder()
                        .expireAfterWrite(windowSeconds, TimeUnit.SECONDS)
                        .build()
        );

        // 用唯一 key 插入标记，利用 TTL 自动清理过期请求
        String requestKey = System.nanoTime() + ":" + Thread.currentThread().getId();
        requestCache.put(requestKey, Boolean.TRUE);

        return requestCache.estimatedSize() <= maxRequests;
    }

    private boolean reject(HttpServletResponse response, String message) throws Exception {
        response.setStatus(429);
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write(objectMapper.writeValueAsString(
                Result.error("429", message)
        ));
        return false;
    }

    private String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip != null && !ip.isEmpty()) {
            ip = ip.split(",")[0].trim();
        }
        if (ip == null || ip.isEmpty()) {
            ip = request.getHeader("X-Real-IP");
        }
        if (ip == null || ip.isEmpty()) {
            ip = request.getRemoteAddr();
        }
        return ip;
    }
}
