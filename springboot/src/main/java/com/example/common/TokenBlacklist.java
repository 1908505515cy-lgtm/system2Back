package com.example.common;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

@Component
public class TokenBlacklist {

    // 使用 Caffeine 缓存，Token 24 小时后自动过期（与 JWT 过期时间一致）
    private final Cache<String, Boolean> blacklistedTokens = Caffeine.newBuilder()
            .expireAfterWrite(24, TimeUnit.HOURS)
            .maximumSize(10000)
            .build();

    public void add(String token) {
        blacklistedTokens.put(token, Boolean.TRUE);
    }

    public boolean contains(String token) {
        return blacklistedTokens.getIfPresent(token) != null;
    }
}
