package com.example.controller;

import com.example.common.JwtUtil;
import com.example.common.Result;
import com.example.dto.LoginDto;
import com.example.entity.Admin;
import com.example.mapper.AdminMapper;
import com.example.service.RoleService;
import com.example.vo.AdminVo;
import jakarta.validation.Valid;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
public class LoginController {

    private final AdminMapper adminMapper;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final RoleService roleService;

    public LoginController(AdminMapper adminMapper, PasswordEncoder passwordEncoder,
                           JwtUtil jwtUtil, RoleService roleService) {
        this.adminMapper = adminMapper;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtil = jwtUtil;
        this.roleService = roleService;
    }

    @PostMapping("/login")
    public Result login(@Valid @RequestBody LoginDto dto) {
        Admin admin = adminMapper.selectByUsername(dto.getUsername());
        if (admin == null) {
            return Result.error("5003", "账号或密码错误");
        }

        if (admin.getStatus() != null && admin.getStatus() == 0) {
            return Result.error("A006", "账号已被禁用");
        }

        if (!passwordEncoder.matches(dto.getPassword(), admin.getPassword())) {
            return Result.error("5003", "账号或密码错误");
        }

        String token = jwtUtil.generateToken(admin.getUsername());

        Map<String, Object> data = new HashMap<>();
        data.put("token", token);
        Map<String, Object> user = new HashMap<>();
        user.put("id", admin.getId());
        user.put("username", admin.getUsername());
        user.put("realName", admin.getRealName());
        user.put("avatar", admin.getAvatar());
        user.put("roleIds", admin.getRoleIds());

        // 解析角色ID，获取角色信息和模块权限
        List<Long> roleIdList = parseRoleIds(admin.getRoleIds());
        if (!roleIdList.isEmpty()) {
            List<String> modulePerms = roleService.getUserModulePerms(roleIdList);
            user.put("modulePerms", modulePerms);
        } else {
            user.put("modulePerms", List.of());
        }
        data.put("user", user);

        return Result.success(data);

    }

    private List<Long> parseRoleIds(String roleIds) {
        if (roleIds == null || roleIds.trim().isEmpty()) return List.of();
        return Arrays.stream(roleIds.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .map(Long::parseLong)
                .collect(Collectors.toList());
    }
}
