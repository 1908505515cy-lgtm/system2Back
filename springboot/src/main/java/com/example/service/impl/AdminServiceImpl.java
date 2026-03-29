// src/main/java/com/example/service/impl/AdminServiceImpl.java
package com.example.service.impl;

import com.example.dto.AdminDto;
import com.example.entity.Admin;
import com.example.exception.CustomException;
import com.example.mapper.AdminMapper;
import com.example.service.AdminService;
import com.example.vo.AdminVo;
import com.example.common.PageResult;
import com.example.common.enums.ResultCodeEnum;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 系统管理员 Service 实现类
 */
@Service
public class AdminServiceImpl implements AdminService {

    private final AdminMapper adminMapper;

    public AdminServiceImpl(AdminMapper adminMapper) {
        this.adminMapper = adminMapper;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void add(AdminDto dto) {

        // 1. 主动校验（逻辑保持不变）
        if (dto.getAdminCode() != null && adminMapper.countByAdminCode(dto.getAdminCode()) > 0) {
            throw new CustomException(ResultCodeEnum.ADMIN_CODE_EXIST);
        }
        if (dto.getUsername() != null && adminMapper.countByUsername(dto.getUsername()) > 0) {
            throw new CustomException(ResultCodeEnum.ADMIN_USERNAME_EXIST);
        }
        if (dto.getEmail() != null && adminMapper.countByEmail(dto.getEmail()) > 0) {
            throw new CustomException(ResultCodeEnum.ADMIN_EMAIL_EXIST);
        }
        if (dto.getMobile() != null && !dto.getMobile().trim().isEmpty()
                && adminMapper.countByMobile(dto.getMobile()) > 0) {
            throw new CustomException(ResultCodeEnum.ADMIN_MOBILE_EXIST);
        }

        // 2. 属性拷贝
        Admin entity = new Admin();
        // 注意：由于类型不匹配，dto 中的 List<Long> roleIds 不会被拷贝到 entity 中
        BeanUtils.copyProperties(dto, entity);

        // 3. 手动转换并设置 roleIds 字符串
        // 逻辑：[1, 2, 3] -> "1,2,3"
        if (dto.getRoleIds() != null && !dto.getRoleIds().isEmpty()) {
            String roleStr = dto.getRoleIds().stream()
                    .map(String::valueOf)                // Long 转 String
                    .collect(Collectors.joining(","));   // 用逗号拼接
            entity.setRoleIds(roleStr);
        } else {
            // 如果前端没传，设置一个默认角色 ID（字符串格式）
            entity.setRoleIds("1");
        }

        // 4. 执行插入
        int rows = adminMapper.insert(entity);
        if (rows == 0) {
            throw new CustomException(ResultCodeEnum.SYSTEM_ERROR);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void update(AdminDto dto) {
        if (dto.getId() == null) {
            throw new CustomException(ResultCodeEnum.PARAM_ERROR);
        }

        Admin entity = new Admin();
        BeanUtils.copyProperties(dto, entity);

        int rows = adminMapper.updateById(entity);
        if (rows == 0) {
            throw new CustomException(ResultCodeEnum.ADMIN_NOT_EXIST);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        int rows = adminMapper.deleteById(id);
        if (rows == 0) {
            throw new CustomException(ResultCodeEnum.ADMIN_NOT_EXIST);
        }
    }

    @Override
    public AdminVo getById(Long id) {
        Admin entity = adminMapper.selectById(id);
        if (entity == null) {
            throw new CustomException(ResultCodeEnum.ADMIN_NOT_EXIST);
        }

        AdminVo vo = new AdminVo();
        BeanUtils.copyProperties(entity, vo);
        return vo;
    }

    @Override
    public PageResult<AdminVo> pageList(String keyword, Integer status, Integer pageNum, Integer pageSize) {
        Long offset = (long) (pageNum - 1) * pageSize;

        List<Admin> entityList = adminMapper.selectAdminPage(keyword, status, offset, (long) pageSize);
        long total = adminMapper.selectCount(keyword, status);

        List<AdminVo> voList = entityList.stream()
                .map(entity -> {
                    AdminVo vo = new AdminVo();
                    BeanUtils.copyProperties(entity, vo);
                    return vo;
                })
                .collect(Collectors.toList());

        return new PageResult<>(voList, total, pageNum, pageSize);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateStatus(Long id, Integer status) {
        int rows = adminMapper.updateStatus(id, status);
        if (rows == 0) {
            throw new CustomException(ResultCodeEnum.ADMIN_NOT_EXIST);
        }
    }
}