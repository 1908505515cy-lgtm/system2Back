package com.example.common.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;

import java.util.Arrays;
import java.util.List;

/**
 * 跨域配置
 * 通过 CORS_ALLOWED_ORIGINS 环境变量配置允许的源，多个用逗号分隔
 * 默认允许所有源（开发模式），生产环境应配置具体域名
 */
@Configuration
public class CorsConfig {

    @Value("${cors.allowed-origins:*}")
    private String allowedOrigins;

    @Value("${cors.allow-credentials:false}")
    private boolean allowCredentials;

    @Value("${cors.max-age:3600}")
    private long maxAge;

    @Bean
    public CorsFilter corsFilter() {
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        CorsConfiguration config = new CorsConfiguration();

        List<String> origins = Arrays.asList(allowedOrigins.split(","));
        origins.forEach(config::addAllowedOrigin);

        config.addAllowedHeader("*");
        config.addAllowedMethod("*");
        config.setMaxAge(maxAge);

        // 允许携带凭证（如 Cookie）时不能用 * 作为 origin
        if (allowCredentials) {
            config.setAllowCredentials(true);
            // 如果 origins 包含 * 但启用了 credentials，需要特殊处理
            // 浏览器不允许 * + credentials，这里保持用户配置的责任
        }

        source.registerCorsConfiguration("/**", config);
        return new CorsFilter(source);
    }
}
