package com.example.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.example.common.GenericServiceImpl;
import com.example.entity.Department;
import com.example.mapper.DepartmentMapper;
import com.example.service.DepartmentService;
import org.springframework.stereotype.Service;

@Service
public class DepartmentServiceImpl extends GenericServiceImpl<Department, Department, Department>
        implements DepartmentService {

    public DepartmentServiceImpl(DepartmentMapper mapper) {
        super(mapper);
    }

    @Override
    protected void buildKeywordCondition(QueryWrapper<Department> wrapper, String keyword) {
        wrapper.and(w -> w
                .like("name", keyword)
                .or().like("dept_code", keyword)
                .or().like("leader", keyword)
        );
    }
}
