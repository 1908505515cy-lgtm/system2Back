package com.example.controller;

import com.alibaba.fastjson2.JSONObject;
import com.example.common.AiResult;
import com.example.common.Result;
import com.example.dto.AiActionDto;
import com.example.service.AiDispatchService;
import org.springframework.web.bind.annotation.*;

/**
 * 通用 AI 端点
 * 代理聊天请求到 Python，执行 AI 建议的操作
 */
@RestController
@RequestMapping("/ai")
public class AiController {

    private final AiDispatchService aiDispatchService;

    public AiController(AiDispatchService aiDispatchService) {
        this.aiDispatchService = aiDispatchService;
    }

    /**
     * 聊天代理入口（前端 AI 对话调用）
     */
    @PostMapping("/chat")
    public JSONObject chat(@RequestBody JSONObject body) {
        String rawText = body.getString("rawText");
        JSONObject response = new JSONObject();
        if (rawText == null || rawText.trim().isEmpty()) {
            response.put("code", 400);
            response.put("msg", "输入内容不能为空");
            return response;
        }
        try {
            return aiDispatchService.chat(rawText);
        } catch (Exception e) {
            response.put("code", 500);
            response.put("msg", e.getMessage());
            return response;
        }
    }

    /**
     * 执行 AI 建议的操作（前端确认后调用）
     */
    @PostMapping("/execute")
    public Result execute(@RequestBody AiActionDto dto) {
        AiResult aiResult = aiDispatchService.execute(dto);
        if (aiResult.isSuccess()) {
            return Result.success(aiResult.getMessage());
        }
        return Result.error(aiResult.getMessage());
    }

    /**
     * 确认执行（兼容旧流程，前端确认卡片调用）
     */
    @PostMapping("/confirm")
    public Result confirm(@RequestBody AiActionDto dto) {
        return execute(dto);
    }
}
