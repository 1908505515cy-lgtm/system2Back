package com.example.common.annotations;

import java.lang.annotation.*;

/**
 * 字段元数据注解
 * 标记在实体字段上，描述该字段的显示、搜索、表单属性
 */
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface FieldMeta {

    /** 字段显示名称（中文） */
    String label();

    /** 是否可被关键词搜索 */
    boolean searchable() default false;

    /** 是否在表格中显示 */
    boolean showInTable() default true;

    /** 是否在表单中显示 */
    boolean showInForm() default true;

    /** 字段类型：text / number / select / date / switch / textarea / password / tag */
    String type() default "text";

    /** 是否必填（前端表单校验） */
    boolean required() default false;

    /** 输入框占位提示文本 */
    String placeholder() default "";

    /** 表格列宽度（像素），0 表示自动 */
    int width() default 0;

    /** 编辑时是否禁用该字段 */
    boolean disabledOnEdit() default false;

    /** type="select" 时的选项列表，格式: "label1:value1,label2:value2" */
    String options() default "";

    /** 字典类型编码，从 sys_dict_data 表加载选项（优先级高于 options） */
    String dictCode() default "";

    /** 关联模块名，用于外键展示（如 deptId 关联 department 模块） */
    String refModule() default "";

    /** 关联模块中用于展示的字段名（如 "name"），配合 refModule 使用 */
    String refField() default "name";
}
