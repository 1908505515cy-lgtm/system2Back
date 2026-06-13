package com.example.common;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 字段信息（供前端动态渲染表格、搜索、表单）
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class FieldInfo {

    /** 字段名（Java 属性名） */
    private String prop;

    /** 数据库列名（下划线格式） */
    private String column;

    /** 显示名称 */
    private String label;

    /** 字段类型 */
    private String type;

    /** 是否可搜索 */
    private boolean searchable;

    /** 是否在表格中显示 */
    private boolean showInTable;

    /** 是否在表单中显示 */
    private boolean showInForm;

    /** 是否必填（前端表单校验） */
    private boolean required;

    /** 输入框占位提示文本 */
    private String placeholder;

    /** 表格列宽度（像素），0 表示自动 */
    private int width;

    /** 编辑时是否禁用该字段 */
    private boolean disabledOnEdit;

    /** select 类型的选项列表 */
    private List<OptionItem> options;

    /** 字典类型编码（从字典表加载选项） */
    private String dictCode;
}
