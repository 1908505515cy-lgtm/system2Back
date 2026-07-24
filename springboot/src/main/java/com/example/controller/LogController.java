package com.example.controller;

import com.example.common.Result;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 前端错误日志接收端点
 */
@RestController
@RequestMapping("/log")
public class LogController {

    private static final Logger log = LoggerFactory.getLogger("frontend-error");

    @PostMapping("/report")
    public Result report(@RequestBody List<Map<String, Object>> errors) {
        for (Map<String, Object> error : errors) {
            String message = String.valueOf(error.getOrDefault("message", ""));
            String url = String.valueOf(error.getOrDefault("url", ""));
            String userId = String.valueOf(error.getOrDefault("userId", "anonymous"));
            String timestamp = String.valueOf(error.getOrDefault("timestamp", ""));
            String stack = String.valueOf(error.getOrDefault("stack", ""));

            log.error("[前端错误] user={}, url={}, time={}, msg={}, stack={}",
                    userId, url, timestamp, message,
                    stack.length() > 500 ? stack.substring(0, 500) + "..." : stack);
        }
        return Result.success("ok");
    }
}
