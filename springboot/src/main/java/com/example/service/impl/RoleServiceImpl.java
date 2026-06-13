package com.example.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.example.common.GenericServiceImpl;
import com.example.entity.Role;
import com.example.mapper.RoleMapper;
import com.example.service.RoleService;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class RoleServiceImpl extends GenericServiceImpl<Role, Role, Role>
        implements RoleService {

    private final RoleMapper roleMapper;

    public RoleServiceImpl(RoleMapper mapper) {
        super(mapper);
        this.roleMapper = mapper;
    }

    @Override
    protected void buildKeywordCondition(QueryWrapper<Role> wrapper, String keyword) {
        wrapper.and(w -> w
                .like("name", keyword)
                .or().like("code", keyword)
        );
    }

    @Override
    public List<Role> getByIds(List<Long> ids) {
        if (ids == null || ids.isEmpty()) return List.of();
        return roleMapper.selectBatchIds(ids);
    }

    @Override
    public List<String> getUserModulePerms(List<Long> roleIds) {
        List<Role> roles = getByIds(roleIds);
        return roles.stream()
                .filter(r -> r.getModulePerms() != null && !r.getModulePerms().isEmpty())
                .flatMap(r -> Arrays.stream(r.getModulePerms().split(",")))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .distinct()
                .collect(Collectors.toList());
    }
}
