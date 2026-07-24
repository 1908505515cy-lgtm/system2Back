package com.example.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.example.common.GenericServiceImpl;
import com.example.entity.Department;
import com.example.mapper.DepartmentMapper;
import com.example.service.DepartmentService;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class DepartmentServiceImpl extends GenericServiceImpl<Department, Department, Department>
        implements DepartmentService {

    public DepartmentServiceImpl(DepartmentMapper mapper, JdbcTemplate jdbcTemplate) {
        super(mapper, jdbcTemplate);
    }

    @Override
    protected void buildKeywordCondition(QueryWrapper<Department> wrapper, String keyword) {
        wrapper.and(w -> w
                .like("name", keyword)
                .or().like("dept_code", keyword)
                .or().like("leader", keyword)
        );
    }

    @Override
    protected void buildTrashKeywordCondition(StringBuilder whereClause, java.util.List<Object> params, String keyword) {
        whereClause.append(" AND (name LIKE ? OR dept_code LIKE ? OR leader LIKE ?)");
        String pattern = "%" + keyword + "%";
        params.add(pattern);
        params.add(pattern);
        params.add(pattern);
    }

    @Override
    public List<Map<String, Object>> getTree() {
        List<Department> all = mapper.selectList(new QueryWrapper<Department>().orderByAsc("id"));
        return buildTree(all, null);
    }

    private List<Map<String, Object>> buildTree(List<Department> all, Long parentId) {
        return all.stream()
                .filter(d -> Objects.equals(d.getParentId(), parentId))
                .map(d -> {
                    Map<String, Object> node = new LinkedHashMap<>();
                    node.put("id", d.getId());
                    node.put("label", d.getName());
                    List<Map<String, Object>> children = buildTree(all, d.getId());
                    if (!children.isEmpty()) {
                        node.put("children", children);
                    }
                    return node;
                })
                .collect(Collectors.toList());
    }
}
