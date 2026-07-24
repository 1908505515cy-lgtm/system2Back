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
@TableName("sys_dashboard")
@ModuleMeta(name = "dashboard", title = "仪表盘", icon = "DataBoard",
        features = "batchDelete")
public class Dashboard {

    @TableId(type = IdType.AUTO)
    @FieldMeta(label = "ID", showInForm = false)
    private Long id;

    @FieldMeta(label = "名称", required = true, searchable = true, width = 160,
            placeholder = "请输入仪表盘名称")
    private String name;

    @FieldMeta(label = "说明", type = "textarea", showInTable = false,
            placeholder = "请输入说明")
    private String description;

    @FieldMeta(label = "布局配置", type = "textarea", showInTable = false, showInForm = false)
    private String layoutJson;

    @FieldMeta(label = "默认", type = "switch", width = 80, options = "是:1,否:0")
    private Integer isDefault;

    @FieldMeta(label = "状态", type = "switch", width = 80, options = "启用:1,禁用:0")
    private Integer status;

    @FieldMeta(label = "创建时间", type = "date", showInForm = false, width = 160)
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @FieldMeta(label = "更新时间", type = "date", showInForm = false)
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    @TableLogic
    private Integer deleted;
}
