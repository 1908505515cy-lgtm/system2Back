package com.example.service;

import com.example.common.GenericService;
import com.example.dto.AdminDto;
import com.example.entity.Admin;
import com.example.vo.AdminVo;

/**
 * 管理员 Service 接口
 * 继承泛型接口，添加 Admin 特有方法
 */
public interface AdminService extends GenericService<Admin, AdminDto, AdminVo> {

    boolean updateAdminNameFromAi(AdminDto dto);

    void resetPassword(Long id);

    AdminVo getByUsername(String username);
}
