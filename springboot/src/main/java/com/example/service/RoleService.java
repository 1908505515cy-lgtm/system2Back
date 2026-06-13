package com.example.service;

import com.example.common.GenericService;
import com.example.entity.Role;

import java.util.List;

public interface RoleService extends GenericService<Role, Role, Role> {

    /** 根据角色ID列表获取角色列表 */
    List<Role> getByIds(List<Long> ids);

    /** 获取用户可访问的模块列表（合并所有角色的权限） */
    List<String> getUserModulePerms(List<Long> roleIds);
}
