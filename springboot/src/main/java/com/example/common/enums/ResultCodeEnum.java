// src/main/java/com/example/common/enums/ResultCodeEnum.java
package com.example.common.enums;

/**
 * 全局返回码枚举
 */
public enum ResultCodeEnum {

    SUCCESS("200", "成功"),
    PARAM_ERROR("400", "参数异常"),
    TOKEN_INVALID_ERROR("401", "无效的token"),
    TOKEN_CHECK_ERROR("401", "token验证失败，请重新登录"),
    PARAM_LOST_ERROR("4001", "参数缺失"),

    SYSTEM_ERROR("500", "系统异常"),
    USER_EXIST_ERROR("5001", "用户名已存在"),
    USER_NOT_LOGIN("5002", "用户未登录"),
    USER_ACCOUNT_ERROR("5003", "账号或密码错误"),
    USER_NOT_EXIST_ERROR("5004", "用户不存在"),
    PARAM_PASSWORD_ERROR("5005", "原密码输入错误"),

    // ==================== 管理员模块专用 ====================
    ADMIN_CODE_EXIST("A001", "管理员编码已存在"),
    ADMIN_USERNAME_EXIST("A002", "用户名已存在"),
    ADMIN_EMAIL_EXIST("A003", "该邮箱已被注册"),
    ADMIN_ROLE_NOT_EXIST("A004", "默认角色不存在"),
    ADMIN_NOT_EXIST("A005", "管理员不存在或已被删除"),
    ADMIN_STATUS_ERROR("A006", "管理员状态异常"),
    ADMIN_MOBILE_EXIST("A007", "该手机号已被注册，请使用其他手机号");

    private final String code;
    private final String msg;

    ResultCodeEnum(String code, String msg) {
        this.code = code;
        this.msg = msg;
    }

    public String getCode() {
        return code;
    }

    public String getMsg() {
        return msg;
    }
}