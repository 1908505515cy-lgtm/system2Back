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
@TableName("wf_instance")
@ModuleMeta(name = "wfInstance", title = "流程实例", icon = "Tickets",
        features = "batchDelete")
public class WfInstance {

    @TableId(type = IdType.AUTO)
    @FieldMeta(label = "ID", showInForm = false)
    private Long id;

    @FieldMeta(label = "流程定义ID", required = true, showInTable = false)
    private Long definitionId;

    @FieldMeta(label = "标题", required = true, searchable = true, width = 200,
            placeholder = "请输入实例标题")
    private String title;

    @FieldMeta(label = "发起人", width = 100, showInForm = false)
    private String initiator;

    @FieldMeta(label = "状态", type = "tag", width = 100, showInForm = false,
            options = "进行中:running,已完成:completed,已驳回:rejected,已撤回:cancelled")
    private String status;

    @FieldMeta(label = "业务类型", showInTable = false, showInForm = false)
    private String businessType;

    @FieldMeta(label = "业务ID", type = "number", showInTable = false, showInForm = false)
    private Long businessId;

    @FieldMeta(label = "当前节点", showInTable = false, showInForm = false)
    private String currentNodeId;

    @FieldMeta(label = "创建时间", type = "date", showInForm = false, width = 160)
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @FieldMeta(label = "更新时间", type = "date", showInForm = false)
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    @TableLogic
    private Integer deleted;
}
