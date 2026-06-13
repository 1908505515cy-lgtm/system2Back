package com.example.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.example.common.annotations.FieldMeta;
import com.example.common.annotations.ModuleMeta;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 角色实体
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@TableName("sys_role")
@ModuleMeta(name = "role", title = "角色管理", icon = "UserFilled",
        features = "statusToggle,batchDelete")
public class Role {

    @TableId(type = IdType.AUTO)
    @FieldMeta(label = "ID", showInTable = false, showInForm = false)
    private Long id;

    @FieldMeta(label = "角色名称", searchable = true, required = true, width = 150,
            placeholder = "请输入角色名称")
    private String name;

    @FieldMeta(label = "角色编码", searchable = true, required = true, width = 120,
            disabledOnEdit = true, placeholder = "如 admin, editor")
    private String code;

    @FieldMeta(label = "可访问模块", type = "textarea", showInTable = false,
            placeholder = "模块名用逗号分隔，如 admin,department")
    private String modulePerms;

    @FieldMeta(label = "状态", type = "switch", width = 80,
            options = "正常:1,禁用:0")
    private Integer status;

    @FieldMeta(label = "备注", type = "textarea", showInTable = false)
    private String remark;

    @FieldMeta(label = "创建时间", type = "date", showInForm = false, width = 160)
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @FieldMeta(label = "更新时间", type = "date", showInForm = false)
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    @TableLogic
    private Integer deleted;
}
