package com.example.controller;

import com.example.common.GenericController;
import com.example.entity.Role;
import com.example.service.RoleService;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/role")
public class RoleController extends GenericController<Role, Role, Role> {

    private final RoleService roleService;

    public RoleController(RoleService roleService) {
        this.roleService = roleService;
    }

    @Override
    protected RoleService getService() {
        return roleService;
    }
}
