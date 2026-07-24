package com.example.config;

import com.example.common.Result;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.PrintWriter;
import java.io.StringWriter;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class RateLimitInterceptorTest {

    private RateLimitInterceptor interceptor;
    private HttpServletRequest request;
    private HttpServletResponse response;
    private StringWriter responseWriter;

    @BeforeEach
    void setUp() throws Exception {
        interceptor = new RateLimitInterceptor();
        ReflectionTestUtils.setField(interceptor, "globalMaxRequests", 5);
        ReflectionTestUtils.setField(interceptor, "windowSeconds", 60);

        request = mock(HttpServletRequest.class);
        response = mock(HttpServletResponse.class);
        responseWriter = new StringWriter();
        when(request.getRemoteAddr()).thenReturn("127.0.0.1");
        when(request.getRequestURI()).thenReturn("/test");
        when(response.getWriter()).thenReturn(new PrintWriter(responseWriter));
    }

    @Test
    void allowsRequestsUnderLimit() throws Exception {
        for (int i = 0; i < 5; i++) {
            assertTrue(interceptor.preHandle(request, response, null));
        }
    }

    @Test
    void blocksRequestsOverLimit() throws Exception {
        for (int i = 0; i < 5; i++) {
            interceptor.preHandle(request, response, null);
        }
        // 第6个请求应该被拦截
        assertFalse(interceptor.preHandle(request, response, null));
        verify(response).setStatus(429);
    }

    @Test
    void differentIps_haveSeparateLimits() throws Exception {
        HttpServletRequest request2 = mock(HttpServletRequest.class);
        when(request2.getRemoteAddr()).thenReturn("192.168.1.1");
        when(request2.getRequestURI()).thenReturn("/test");

        // IP1 用完限额
        for (int i = 0; i < 5; i++) {
            interceptor.preHandle(request, response, null);
        }
        assertFalse(interceptor.preHandle(request, response, null));

        // IP2 仍然可用
        assertTrue(interceptor.preHandle(request2, response, null));
    }
}
