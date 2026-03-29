// src/main/java/com/example/service/AdminService.java
package com.example.service;

import com.example.dto.AdminDto;
import com.example.vo.AdminVo;
import com.example.common.PageResult;

/**
 * 系统管理员 Service 接口
 */
public interface AdminService {

    void add(AdminDto dto);

    void update(AdminDto dto);

    void delete(Long id);

    AdminVo getById(Long id);

    PageResult<AdminVo> pageList(String keyword, Integer status, Integer pageNum, Integer pageSize);

    void updateStatus(Long id, Integer status);
}