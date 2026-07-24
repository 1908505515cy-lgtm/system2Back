package com.example.service;

import com.example.common.GenericService;
import com.example.entity.Department;

import java.util.List;
import java.util.Map;

public interface DepartmentService extends GenericService<Department, Department, Department> {

    /** 获取部门树形结构 */
    List<Map<String, Object>> getTree();
}
