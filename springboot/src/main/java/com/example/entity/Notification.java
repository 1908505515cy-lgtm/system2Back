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

@Data
@NoArgsConstructor
@AllArgsConstructor
@TableName("sys_notification")
@ModuleMeta(name = "notification", title = "系统通知", icon = "Bell", features = "batchDelete")
public class Notification {

    @TableId(type = IdType.AUTO)
    @FieldMeta(label = "ID", showInForm = false)
    private Long id;

    @FieldMeta(label = "标题", required = true, searchable = true)
    private String title;

    @FieldMeta(label = "内容", type = "textarea")
    private String content;

    @FieldMeta(label = "类型", type = "select",
            options = "通知:info,警告:warning,成功:success,工作流:workflow")
    private String type;

    @FieldMeta(label = "目标用户", placeholder = "留空表示全员通知")
    private String targetUser;

    @FieldMeta(label = "已读", type = "switch", showInForm = false)
    private Integer isRead;

    @FieldMeta(label = "创建时间", type = "date", showInForm = false)
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableLogic
    private Integer deleted;
}
