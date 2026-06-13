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
 * 字典类型实体
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@TableName("sys_dict_type")
@ModuleMeta(name = "dictType", title = "字典类型", icon = "Collection",
        features = "statusToggle,batchDelete")
public class DictType {

    @TableId(type = IdType.AUTO)
    @FieldMeta(label = "ID", showInTable = false, showInForm = false)
    private Long id;

    @FieldMeta(label = "字典名称", searchable = true, required = true, width = 150,
            placeholder = "请输入字典名称")
    private String name;

    @FieldMeta(label = "字典编码", searchable = true, required = true, width = 150,
            disabledOnEdit = true, placeholder = "如 sys_status")
    private String code;

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
