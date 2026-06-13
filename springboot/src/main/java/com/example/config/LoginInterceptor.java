package com.example.config;

import com.alibaba.fastjson2.JSON;
import com.example.common.JwtUtil;
import com.example.common.Result;
import com.example.common.enums.ResultCodeEnum;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class LoginInterceptor implements HandlerInterceptor {

    private final JwtUtil jwtUtil;

    public LoginInterceptor(JwtUtil jwtUtil) {
        this.jwtUtil = jwtUtil;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        // OPTIONS 预检请求放行
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            return true;
        }

        String authHeader = request.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            write401(response);
            return false;
        }

        String token = authHeader.substring(7);
        try {
            String username = jwtUtil.parseToken(token);
            if (username == null) {
                write401(response);
                return false;
            }
            request.setAttribute("currentUsername", username);
            return true;
        } catch (Exception e) {
            write401(response);
            return false;
        }
    }

    private void write401(HttpServletResponse response) throws Exception {
        response.setContentType("application/json;charset=utf-8");
        response.setStatus(401);
        Result result = Result.error(ResultCodeEnum.TOKEN_INVALID_ERROR.getCode(), ResultCodeEnum.TOKEN_INVALID_ERROR.getMsg());
        response.getWriter().write(JSON.toJSONString(result));
    }
}
