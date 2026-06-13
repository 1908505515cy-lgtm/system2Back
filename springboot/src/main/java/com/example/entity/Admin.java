package com.example.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.example.common.annotations.FieldMeta;
import com.example.common.annotations.ModuleMeta;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

/**
 * 系统管理员实体类（Entity）
 * 与数据库表 sys_admin 一一对应
 */
@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = false)
@TableName("sys_admin")
@ModuleMeta(name = "admin", title = "管理员管理", icon = "User")
public class Admin extends Account {

    @FieldMeta(label = "管理员编码", searchable = true)
    private String adminCode;

    @FieldMeta(label = "部门", type = "select")
    private Long deptId;

    @FieldMeta(label = "角色", type = "tag")
    private String roleIds;
}
