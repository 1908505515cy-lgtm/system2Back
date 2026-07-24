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

@Data
@NoArgsConstructor
@AllArgsConstructor
@TableName("wf_instance_node")
@ModuleMeta(name = "wfInstanceNode", title = "流程节点", icon = "Share",
        features = "")
public class WfInstanceNode {

    @TableId(type = IdType.AUTO)
    @FieldMeta(label = "ID", showInTable = false, showInForm = false)
    private Long id;

    @FieldMeta(label = "实例ID", showInTable = false, showInForm = false)
    private Long instanceId;

    @FieldMeta(label = "节点ID", showInTable = false, showInForm = false)
    private String nodeId;

    @FieldMeta(label = "节点名称", width = 140)
    private String nodeName;

    @FieldMeta(label = "节点类型", type = "tag", width = 100,
            options = "开始:start,结束:end,审批:approval,通知:notify,网关:gateway")
    private String nodeType;

    @FieldMeta(label = "状态", type = "tag", width = 100,
            options = "待处理:pending,进行中:active,已完成:completed,已跳过:skipped")
    private String status;

    @FieldMeta(label = "处理人", width = 100)
    private String assignee;

    @FieldMeta(label = "创建时间", type = "date", showInForm = false, width = 160)
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @FieldMeta(label = "更新时间", type = "date", showInForm = false)
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
