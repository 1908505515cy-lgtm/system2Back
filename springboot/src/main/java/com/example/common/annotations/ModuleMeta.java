package com.example.common.annotations;

import java.lang.annotation.*;

/**
 * 模块元数据注解
 * 标记在实体类上，描述该实体对应的业务模块信息
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface ModuleMeta {

    /** 模块标识（英文，用于路由和 API 路径） */
    String name();

    /** 模块显示名称（中文） */
    String title();

    /** 图标名（Element Plus 图标名），默认 User */
    String icon() default "User";

    /** 启用的功能特性，逗号分隔。可选值: statusToggle, batchDelete, resetPassword */
    String features() default "statusToggle,batchDelete";
}
