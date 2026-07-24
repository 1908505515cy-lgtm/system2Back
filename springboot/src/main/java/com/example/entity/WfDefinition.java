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
@TableName("wf_definition")
@ModuleMeta(name = "wfDefinition", title = "流程定义", icon = "Connection",
        features = "batchDelete")
public class WfDefinition {

    @TableId(type = IdType.AUTO)
    @FieldMeta(label = "ID", showInForm = false)
    private Long id;

    @FieldMeta(label = "流程名称", required = true, searchable = true, width = 160,
            placeholder = "请输入流程名称")
    private String name;

    @FieldMeta(label = "流程编码", required = true, width = 120,
            placeholder = "如 leave_approval", disabledOnEdit = true)
    private String code;

    @FieldMeta(label = "说明", type = "textarea", showInTable = false,
            placeholder = "请输入流程说明")
    private String description;

    @FieldMeta(label = "分类", type = "select", width = 100, dictCode = "wf_category")
    private String category;

    @FieldMeta(label = "节点定义", type = "textarea", showInTable = false, showInForm = false)
    private String nodesJson;

    @FieldMeta(label = "连线定义", type = "textarea", showInTable = false, showInForm = false)
    private String edgesJson;

    @FieldMeta(label = "状态", type = "switch", width = 80, options = "启用:1,禁用:0")
    private Integer status;

    @FieldMeta(label = "版本", type = "number", width = 80, showInForm = false)
    private Integer version;

    @FieldMeta(label = "创建时间", type = "date", showInForm = false, width = 160)
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @FieldMeta(label = "更新时间", type = "date", showInForm = false)
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    @TableLogic
    private Integer deleted;
}
