package com.example.service;

import com.example.common.AiResult;
import com.example.common.GenericService;
import com.example.common.ModuleRegistry;
import com.example.dto.AiActionDto;
import com.example.exception.CustomException;
import com.example.common.enums.ResultCodeEnum;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import com.alibaba.fastjson2.JSONObject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;

/**
 * AI 操作分发服务
 * 根据 entityType + action 路由到对应的 GenericService 执行
 */
@Service
public class AiDispatchService {

    private static final Logger log = LoggerFactory.getLogger(AiDispatchService.class);

    private final Map<String, GenericService<?, ?, ?>> serviceMap;
    private final ModuleRegistry moduleRegistry;
    private final RestTemplate restTemplate;

    @Value("${python.agent.url}")
    private String pythonAgentUrl;

    public AiDispatchService(Map<String, GenericService<?, ?, ?>> serviceMap,
                             ModuleRegistry moduleRegistry,
                             RestTemplate restTemplate) {
        this.serviceMap = serviceMap;
        this.moduleRegistry = moduleRegistry;
        this.restTemplate = restTemplate;
    }

    /**
     * 代理聊天请求到 Python AI
     */
    public JSONObject chat(String rawText) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        JSONObject payload = new JSONObject();
        payload.put("raw_text", rawText);

        HttpEntity<String> request = new HttpEntity<>(payload.toJSONString(), headers);

        try {
            ResponseEntity<String> response = restTemplate.postForEntity(pythonAgentUrl, request, String.class);
            return JSONObject.parseObject(response.getBody());
        } catch (Exception e) {
            log.error("[AI Dispatch] 无法连接到 Python: {}", e.getMessage(), e);
            throw new CustomException(ResultCodeEnum.SYSTEM_ERROR);
        }
    }

    /**
     * 执行 AI 建议的操作（通用）
     * 根据 entityType 找到对应的 Service，根据 action 执行操作
     */
    public AiResult execute(AiActionDto dto) {
        String entityType = dto.getEntityType();
        String action = dto.getAction();
        Map<String, Object> params = dto.getParams();

        GenericService<?, ?, ?> service = findService(entityType);
        if (service == null) {
            return AiResult.fail("不支持的实体类型: " + entityType);
        }

        try {
            switch (action) {
                case "update_field":
                    return executeUpdateField(service, params);
                case "update_status":
                    return executeUpdateStatus(service, params);
                case "delete":
                    return executeDelete(service, params);
                case "create":
                    return executeCreate(service, params);
                case "query":
                    return executeQuery(service, params);
                default:
                    return AiResult.fail("不支持的操作类型: " + action);
            }
        } catch (Exception e) {
            return AiResult.fail("执行失败: " + e.getMessage());
        }
    }

    private GenericService<?, ?, ?> findService(String entityType) {
        // 精确匹配 bean 名
        GenericService<?, ?, ?> service = serviceMap.get(entityType + "ServiceImpl");
        if (service == null) service = serviceMap.get(entityType + "Service");
        if (service == null) {
            // 仅匹配以 entityType 开头（忽略大小写）的 bean，避免 "a" 匹配到 "admin" 等
            String prefix = entityType.toLowerCase();
            for (Map.Entry<String, GenericService<?, ?, ?>> entry : serviceMap.entrySet()) {
                String beanName = entry.getKey().toLowerCase();
                if (beanName.startsWith(prefix) && (beanName.equals(prefix + "serviceimpl") || beanName.equals(prefix + "service"))) {
                    return entry.getValue();
                }
            }
        }
        return service;
    }

    @SuppressWarnings("unchecked")
    private AiResult executeUpdateField(GenericService<?, ?, ?> service, Map<String, Object> params) {
        Object targetId = params.get("target_id");
        String fieldName = (String) params.get("field_name");
        Object newValue = params.get("new_value");

        if (targetId == null || fieldName == null) {
            return AiResult.fail("参数缺失：需要 target_id 和 field_name");
        }

        Long id = toLong(targetId);
        if (id == null) {
            return AiResult.fail("参数格式错误：target_id 必须为数字 ID，当前值: " + targetId);
        }

        service.updateField(id, fieldName, newValue);
        return AiResult.ok("字段修改成功");
    }

    @SuppressWarnings("unchecked")
    private AiResult executeUpdateStatus(GenericService<?, ?, ?> service, Map<String, Object> params) {
        Long id = toLong(params.get("id"));
        Integer status = toInt(params.get("status"));

        if (id == null || status == null) {
            return AiResult.fail("参数缺失：需要 id 和 status");
        }

        service.updateStatus(id, status);
        return AiResult.ok("状态修改成功");
    }

    @SuppressWarnings("unchecked")
    private AiResult executeDelete(GenericService<?, ?, ?> service, Map<String, Object> params) {
        Long id = toLong(params.get("id"));
        if (id == null) {
            return AiResult.fail("参数缺失：需要 id");
        }

        service.delete(id);
        return AiResult.ok("删除成功");
    }

    @SuppressWarnings("unchecked")
    private AiResult executeCreate(GenericService<?, ?, ?> service, Map<String, Object> params) {
        if (params == null || params.isEmpty()) {
            return AiResult.fail("参数缺失：需要字段数据");
        }
        service.createFromMap(params);
        return AiResult.ok("创建成功");
    }

    @SuppressWarnings("unchecked")
    private AiResult executeQuery(GenericService<?, ?, ?> service, Map<String, Object> params) {
        if (params == null || params.isEmpty()) {
            return AiResult.fail("参数缺失：需要查询条件");
        }
        java.util.List<?> results = service.queryByFields(params);
        if (results.isEmpty()) {
            return AiResult.ok("未找到匹配的记录");
        }
        return AiResult.ok("找到 " + results.size() + " 条记录", results);
    }

    private Long toLong(Object value) {
        if (value == null) return null;
        if (value instanceof Long) return (Long) value;
        if (value instanceof Integer) return ((Integer) value).longValue();
        try { return Long.parseLong(value.toString()); } catch (Exception e) { return null; }
    }

    private Integer toInt(Object value) {
        if (value == null) return null;
        if (value instanceof Integer) return (Integer) value;
        try { return Integer.parseInt(value.toString()); } catch (Exception e) { return null; }
    }
}
