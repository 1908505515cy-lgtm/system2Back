package com.example.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.util.Map;

/**
 * AI 通用操作 DTO
 * 用于前端确认后执行 AI 建议的操作
 */
@Data
public class AiActionDto {

    /** 实体类型（如 "admin"） */
    @NotBlank(message = "实体类型不能为空")
    private String entityType;

    /** 操作类型（如 "update_field"、"update_status"、"delete"） */
    @NotBlank(message = "操作类型不能为空")
    private String action;

    /** 操作参数（由 AI 提取，格式取决于 action 类型） */
    private Map<String, Object> params;

    /** 原始自然语言输入 */
    private String rawText;
}
