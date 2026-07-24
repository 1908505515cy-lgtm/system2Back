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
 * 知识库文档实体
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@TableName("sys_document")
@ModuleMeta(name = "document", title = "知识库", icon = "Document",
        features = "batchDelete")
public class Document {

    @TableId(type = IdType.AUTO)
    @FieldMeta(label = "ID", showInTable = false, showInForm = false)
    private Long id;

    @FieldMeta(label = "标题", searchable = true, required = true, width = 200,
            placeholder = "请输入文档标题")
    private String title;

    @FieldMeta(label = "内容", type = "textarea", showInTable = false, required = true,
            placeholder = "请输入文档内容")
    private String content;

    @FieldMeta(label = "类型", type = "select", width = 100,
            options = "常见问题:faq,操作手册:manual,政策法规:policy,其他:other")
    private String docType;

    @FieldMeta(label = "状态", type = "switch", width = 80,
            options = "启用:1,禁用:0")
    private Integer status;

    @FieldMeta(label = "向量化", type = "tag", width = 80,
            options = "未处理:0,已向量化:1,失败:2")
    private Integer vectorStatus;

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
