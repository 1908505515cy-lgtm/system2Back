package com.example.config;

import com.alibaba.fastjson2.JSON;
import com.example.common.JwtUtil;
import com.example.common.Result;
import com.example.common.TokenBlacklist;
import com.example.common.enums.ResultCodeEnum;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class LoginInterceptor implements HandlerInterceptor {

    private final JwtUtil jwtUtil;
    private final TokenBlacklist tokenBlacklist;

    public LoginInterceptor(JwtUtil jwtUtil, TokenBlacklist tokenBlacklist) {
        this.jwtUtil = jwtUtil;
        this.tokenBlacklist = tokenBlacklist;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            return true;
        }

        String authHeader = request.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            write401(response);
            return false;
        }

        String token = authHeader.substring(7);

        // 检查黑名单
        if (tokenBlacklist.contains(token)) {
            write401(response, "Token 已失效，请重新登录");
            return false;
        }

        try {
            String username = jwtUtil.parseToken(token);
            if (username == null) {
                write401(response);
                return false;
            }
            request.setAttribute("currentUsername", username);
            // 注入 userId 到 MDC，供日志输出
            MDC.put("userId", username);
            return true;
        } catch (Exception e) {
            write401(response);
            return false;
        }
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response,
                                Object handler, Exception ex) {
        MDC.remove("userId");
    }

    private void write401(HttpServletResponse response) throws Exception {
        write401(response, null);
    }

    private void write401(HttpServletResponse response, String msg) throws Exception {
        response.setContentType("application/json;charset=utf-8");
        response.setStatus(401);
        Result result = Result.error(ResultCodeEnum.TOKEN_INVALID_ERROR.getCode(),
                msg != null ? msg : ResultCodeEnum.TOKEN_INVALID_ERROR.getMsg());
        response.getWriter().write(JSON.toJSONString(result));
    }
}
