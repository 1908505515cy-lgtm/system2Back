package com.example.controller;

import com.alibaba.fastjson2.JSONObject;
import com.example.common.AiResult;
import com.example.common.Result;
import com.example.dto.AiActionDto;
import com.example.service.AiDispatchService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

/**
 * 通用 AI 端点
 * 代理聊天请求到 Python，执行 AI 建议的操作
 */
@RestController
@RequestMapping("/ai")
public class AiController {

    @Value("${python.agent.url}")
    private String pythonAgentUrl;

    @Value("${ollama.base-url:http://localhost:11434}")
    private String ollamaBaseUrl;

    @Value("${ollama.model:gemma4:latest}")
    private String ollamaModel;

    private final AiDispatchService aiDispatchService;
    private final RestTemplate restTemplate;

    public AiController(AiDispatchService aiDispatchService, RestTemplate restTemplate) {
        this.aiDispatchService = aiDispatchService;
        this.restTemplate = restTemplate;
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

    /**
     * 检查 Ollama 服务状态
     */
    @GetMapping("/ollama-status")
    public Result checkOllamaStatus() {
        Map<String, Object> data = new HashMap<>();
        try {
            String url = ollamaBaseUrl + "/api/tags";
            String response = restTemplate.getForObject(url, String.class);
            data.put("connected", true);
            data.put("model", ollamaModel);
        } catch (Exception e) {
            data.put("connected", false);
            data.put("model", "");
        }
        return Result.success(data);
    }

    /**
     * 检查 Python AI 服务状态
     */
    @GetMapping("/python-status")
    public Result checkPythonStatus() {
        Map<String, Object> data = new HashMap<>();
        try {
            String url = pythonAgentUrl.replace("/analyze", "/health");
            String response = restTemplate.getForObject(url, String.class);
            data.put("connected", true);
        } catch (Exception e) {
            data.put("connected", false);
        }
        return Result.success(data);
    }
}
