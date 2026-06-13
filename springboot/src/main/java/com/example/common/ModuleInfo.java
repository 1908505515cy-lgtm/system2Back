package com.example.common;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 模块信息（供前端和 AI 查询）
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ModuleInfo {

    /** 模块标识 */
    private String name;

    /** 模块显示名称 */
    private String title;

    /** 图标名 */
    private String icon;

    /** API 路径前缀 */
    private String apiBase;

    /** 数据库表名 */
    private String tableName;

    /** 字段列表 */
    private List<FieldInfo> fields;

    /** 启用的功能特性列表 */
    private List<String> features;
}
