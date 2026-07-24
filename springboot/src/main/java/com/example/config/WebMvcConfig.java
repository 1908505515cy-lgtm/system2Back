package com.example.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    @Value("${file.upload-dir:./uploads}")
    private String uploadDir;

    private final LoginInterceptor loginInterceptor;
    private final RateLimitInterceptor rateLimitInterceptor;
    private final ModulePermInterceptor modulePermInterceptor;

    public WebMvcConfig(LoginInterceptor loginInterceptor,
                        RateLimitInterceptor rateLimitInterceptor,
                        ModulePermInterceptor modulePermInterceptor) {
        this.loginInterceptor = loginInterceptor;
        this.rateLimitInterceptor = rateLimitInterceptor;
        this.modulePermInterceptor = modulePermInterceptor;
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // 配置静态资源映射，让上传的文件可以通过 URL 访问
        registry.addResourceHandler("/uploads/**")
                .addResourceLocations("file:" + uploadDir + "/");
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        // 全局限流拦截器：保护所有 API 端点（排除静态资源和健康检查）
        registry.addInterceptor(rateLimitInterceptor)
                .addPathPatterns("/**")
                .excludePathPatterns(
                        "/uploads/**",
                        "/swagger-ui/**",
                        "/v3/api-docs/**",
                        "/swagger-ui.html",
                        "/actuator/**",
                        "/favicon.ico"
                );

        // 登录拦截器
        registry.addInterceptor(loginInterceptor)
                .addPathPatterns("/**")
                .excludePathPatterns(
                        "/login",
                        "/refresh",
                        "/register",
                        "/forgot-password/**",
                        "/admin/ai-update-name",
                        "/module/**",
                        "/log/**",
                        "/swagger-ui/**",
                        "/v3/api-docs/**",
                        "/swagger-ui.html"
                );

        // 模块权限拦截器：在登录拦截器之后执行
        registry.addInterceptor(modulePermInterceptor)
                .addPathPatterns("/**")
                .excludePathPatterns(
                        "/login",
                        "/module/**"
                );
    }
}
