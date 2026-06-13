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
 * 字典数据实体
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@TableName("sys_dict_data")
@ModuleMeta(name = "dictData", title = "字典数据", icon = "List",
        features = "statusToggle,batchDelete")
public class DictData {

    @TableId(type = IdType.AUTO)
    @FieldMeta(label = "ID", showInTable = false, showInForm = false)
    private Long id;

    @FieldMeta(label = "字典类型编码", searchable = true, required = true, width = 150,
            placeholder = "请输入字典类型编码")
    private String dictTypeCode;

    @FieldMeta(label = "显示标签", required = true, width = 120,
            placeholder = "如：正常")
    private String label;

    @FieldMeta(label = "选项值", required = true, width = 100,
            placeholder = "如：1")
    private String value;

    @FieldMeta(label = "排序", type = "number", width = 80)
    private Integer sort;

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
