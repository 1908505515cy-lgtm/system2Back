package com.example.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.example.common.annotations.FieldMeta;
import com.example.common.annotations.ModuleMeta;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 审计日志实体
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@TableName("sys_audit_log")
@ModuleMeta(name = "auditLog", title = "操作日志", icon = "Document",
        features = "")
public class AuditLog {

    @TableId(type = IdType.AUTO)
    @FieldMeta(label = "ID", showInTable = false, showInForm = false)
    private Long id;

    @FieldMeta(label = "操作人", searchable = true, width = 100)
    private String operator;

    @FieldMeta(label = "操作类型", width = 100,
            options = "新增:create,修改:update,删除:delete")
    private String action;

    @FieldMeta(label = "模块", searchable = true, width = 100)
    private String module;

    @FieldMeta(label = "记录ID", width = 100)
    private Long recordId;

    @FieldMeta(label = "变更内容", type = "textarea", showInTable = false)
    private String detail;

    @FieldMeta(label = "操作时间", type = "date", showInForm = false, width = 160)
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime operateTime;

    @FieldMeta(label = "IP地址", width = 130)
    private String ip;
}
