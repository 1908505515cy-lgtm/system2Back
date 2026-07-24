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
@TableName("wf_task")
@ModuleMeta(name = "wfTask", title = "审批任务", icon = "Stamp",
        features = "")
public class WfTask {

    @TableId(type = IdType.AUTO)
    @FieldMeta(label = "ID", showInForm = false)
    private Long id;

    @FieldMeta(label = "流程实例ID", required = true, showInTable = false)
    private Long instanceId;

    @FieldMeta(label = "节点ID", showInTable = false, showInForm = false)
    private String nodeId;

    @FieldMeta(label = "节点名称", width = 140, showInForm = false)
    private String nodeName;

    @FieldMeta(label = "审批人", width = 100, showInForm = false)
    private String assignee;

    @FieldMeta(label = "状态", type = "tag", width = 100, showInForm = false,
            options = "待审批:pending,已通过:approved,已驳回:rejected,已转办:transferred")
    private String action;

    @FieldMeta(label = "审批意见", type = "textarea", showInTable = false)
    private String comment;

    @FieldMeta(label = "转办目标", showInTable = false, showInForm = false)
    private String transferTo;

    @FieldMeta(label = "创建时间", type = "date", showInForm = false, width = 160)
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @FieldMeta(label = "更新时间", type = "date", showInForm = false)
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    @TableLogic
    private Integer deleted;
}
