package com.example.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.example.common.annotations.FieldMeta;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.time.LocalDateTime;

/**
 * 基础账号实体类（Account）
 * 作为所有客户、管理员、教练等账号的公共基类
 */
@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class Account {

    @TableId(type = IdType.AUTO)
    @FieldMeta(label = "ID", showInTable = false, showInForm = false)
    private Long id;

    @FieldMeta(label = "账号编码", searchable = true)
    private String accountCode;

    @FieldMeta(label = "登录用户名", searchable = true)
    private String username;

    @FieldMeta(label = "密码", type = "password", showInTable = false)
    private String password;

    @FieldMeta(label = "真实姓名", searchable = true)
    private String realName;

    @FieldMeta(label = "头像", type = "text")
    private String avatar;

    @FieldMeta(label = "性别", type = "select")
    private Integer gender;

    @FieldMeta(label = "手机号", searchable = true)
    private String mobile;

    @FieldMeta(label = "邮箱")
    private String email;

    @FieldMeta(label = "状态", type = "switch")
    private Integer status;

    @FieldMeta(label = "最后登录时间", type = "date", showInForm = false)
    private LocalDateTime lastLoginTime;

    @FieldMeta(label = "最后登录IP", showInForm = false)
    private String lastLoginIp;

    @FieldMeta(label = "登录次数", showInForm = false)
    private Integer loginCount;

    @FieldMeta(label = "账号类型", type = "select")
    private String role;

    @FieldMeta(label = "备注", type = "textarea")
    private String remark;

    private String securityQuestion;

    private String securityAnswer;

    @FieldMeta(label = "创建时间", type = "date", showInForm = false)
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @FieldMeta(label = "更新时间", type = "date", showInForm = false)
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    @TableLogic
    private Integer deleted;
}
