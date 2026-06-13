package com.example.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.example.common.GenericServiceImpl;
import com.example.common.PageResult;
import com.example.dto.AdminDto;
import com.example.entity.Admin;
import com.example.exception.CustomException;
import com.example.mapper.AdminMapper;
import com.example.service.AdminService;
import com.example.vo.AdminVo;
import com.example.common.enums.ResultCodeEnum;
import com.example.common.Constants;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.stream.Collectors;

/**
 * 管理员 Service 实现类
 * 继承泛型基类，只保留 Admin 特有逻辑
 */
@Service
public class AdminServiceImpl extends GenericServiceImpl<Admin, AdminDto, AdminVo> implements AdminService {

    private final AdminMapper adminMapper;
    private final PasswordEncoder passwordEncoder;

    public AdminServiceImpl(AdminMapper adminMapper, PasswordEncoder passwordEncoder) {
        super(adminMapper);
        this.adminMapper = adminMapper;
        this.passwordEncoder = passwordEncoder;
    }

    // ==================== 覆写钩子方法 ====================

    @Override
    protected void beforeAdd(AdminDto dto) {
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
    }

    @Override
    protected void beforeUpdate(AdminDto dto) {
        if (dto.getId() == null) return;
        Long selfId = dto.getId();
        if (dto.getUsername() != null) {
            Admin existing = adminMapper.selectByUsername(dto.getUsername());
            if (existing != null && !existing.getId().equals(selfId)) {
                throw new CustomException(ResultCodeEnum.ADMIN_USERNAME_EXIST);
            }
        }
        if (dto.getEmail() != null) {
            Admin existing = adminMapper.selectOne(
                    new QueryWrapper<Admin>().eq("email", dto.getEmail()));
            if (existing != null && !existing.getId().equals(selfId)) {
                throw new CustomException(ResultCodeEnum.ADMIN_EMAIL_EXIST);
            }
        }
        if (dto.getMobile() != null && !dto.getMobile().trim().isEmpty()) {
            Admin existing = adminMapper.selectOne(
                    new QueryWrapper<Admin>().eq("mobile", dto.getMobile()));
            if (existing != null && !existing.getId().equals(selfId)) {
                throw new CustomException(ResultCodeEnum.ADMIN_MOBILE_EXIST);
            }
        }
    }

    @Override
    protected Admin toEntity(AdminDto dto) {
        Admin entity = super.toEntity(dto);

        // 密码处理：有值则加密，无值则置 null（updateById 不会更新 null 字段，由 MyBatis-Plus 策略控制）
        if (dto.getPassword() != null && !dto.getPassword().isEmpty()) {
            entity.setPassword(passwordEncoder.encode(dto.getPassword()));
        } else {
            // 显式设为 null，配合 MyBatis-Plus field-strategy=NOT_NULL 使 updateById 跳过该字段
            entity.setPassword(null);
        }

        // roleIds: List<Long> -> "1,2,3"
        if (dto.getRoleIds() != null && !dto.getRoleIds().isEmpty()) {
            String roleStr = dto.getRoleIds().stream()
                    .map(String::valueOf)
                    .collect(Collectors.joining(","));
            entity.setRoleIds(roleStr);
        } else if (dto.getId() == null) {
            // 仅新增时设置默认角色，更新时不覆盖
            entity.setRoleIds("1");
        }

        return entity;
    }

    @Override
    protected void buildKeywordCondition(QueryWrapper<Admin> wrapper, String keyword) {
        wrapper.and(w -> w
                .like("username", keyword)
                .or().like("real_name", keyword)
                .or().like("admin_code", keyword)
        );
    }

    // ==================== 覆写分页（使用自定义 XML 查询，避免泛型 QueryWrapper 问题） ====================

    @Override
    public PageResult<AdminVo> page(String keyword, Integer status, Integer pageNum, Integer pageSize) {
        Long offset = (long) (pageNum - 1) * pageSize;
        java.util.List<Admin> entityList = adminMapper.selectAdminPage(keyword, status, offset, (long) pageSize);
        long total = adminMapper.selectCount(keyword, status);
        java.util.List<AdminVo> voList = entityList.stream()
                .map(this::toVo)
                .collect(Collectors.toList());
        return new PageResult<>(voList, total, pageNum, pageSize);
    }

    // ==================== Admin 特有方法 ====================

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean updateAdminNameFromAi(AdminDto dto) {
        int rows = adminMapper.updateUserNameByAdminCode(dto.getAdminCode(), dto.getNewName());
        return rows > 0;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void resetPassword(Long id) {
        String encoded = passwordEncoder.encode(Constants.USER_DEFAULT_PASSWORD);
        if (adminMapper.updatePassword(id, encoded) == 0) {
            throw new CustomException(ResultCodeEnum.ADMIN_NOT_EXIST);
        }
    }

    @Override
    public AdminVo getByUsername(String username) {
        Admin entity = adminMapper.selectByUsername(username);
        if (entity == null) {
            return null;
        }
        return toVo(entity);
    }
}
