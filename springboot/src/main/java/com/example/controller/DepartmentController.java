package com.example.controller;

import com.example.common.GenericController;
import com.example.common.Result;
import com.example.entity.Department;
import com.example.service.DepartmentService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/department")
public class DepartmentController extends GenericController<Department, Department, Department> {

    private final DepartmentService departmentService;

    public DepartmentController(DepartmentService departmentService) {
        this.departmentService = departmentService;
    }

    @Override
    protected DepartmentService getService() {
        return departmentService;
    }

    /** 获取部门树形结构 */
    @GetMapping("/tree")
    public Result tree() {
        return Result.success(departmentService.getTree());
    }
}
