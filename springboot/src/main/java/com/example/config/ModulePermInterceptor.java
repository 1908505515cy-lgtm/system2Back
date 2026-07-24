package com.example.config;

import com.alibaba.fastjson2.JSON;
import com.example.common.Result;
import com.example.entity.Role;
import com.example.service.AdminService;
import com.example.service.RoleService;
import com.example.vo.AdminVo;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.Arrays;
import java.util.List;
import java.util.Set;

/**
 * 模块权限拦截器
 * 从请求路径提取模块名，检查用户是否有该模块的访问权限
 * 必须在 LoginInterceptor 之后执行（依赖 currentUsername attribute）
 */
@Component
public class ModulePermInterceptor implements HandlerInterceptor {

    private static final Set<String> WHITELIST_PREFIXES = Set.of(
            "/login", "/module", "/ai", "/test", "/home", "/AIchat", "/front", "/chat", "/dashboard", "/export", "/refresh", "/logout", "/register", "/forgot-password"
    );

    private final AdminService adminService;
    private final RoleService roleService;

    public ModulePermInterceptor(AdminService adminService, RoleService roleService) {
        this.adminService = adminService;
        this.roleService = roleService;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        String uri = request.getRequestURI();

        // 白名单路径跳过
        for (String prefix : WHITELIST_PREFIXES) {
            if (uri.startsWith(prefix)) {
                return true;
            }
        }

        // 提取模块名（第一段路径）
        String moduleName = extractModuleName(uri);
        if (moduleName == null || moduleName.isEmpty()) {
            return true;
        }

        // 从 request attribute 获取用户名（由 LoginInterceptor 设置）
        String username = (String) request.getAttribute("currentUsername");
        if (username == null) {
            return true; // 未登录由 LoginInterceptor 处理
        }

        // 获取用户权限（优先从 request 缓存读取）
        List<String> modulePerms = getUserModulePerms(request, username);

        // 空权限 = 全部允许
        if (modulePerms == null || modulePerms.isEmpty()) {
            return true;
        }

        // 检查是否有该模块的权限
        if (modulePerms.contains(moduleName)) {
            return true;
        }

        // 无权限，返回 403
        write403(response);
        return false;
    }

    @SuppressWarnings("unchecked")
    private List<String> getUserModulePerms(HttpServletRequest request, String username) {
        // 先检查缓存
        Object cached = request.getAttribute("userModulePerms");
        if (cached != null) {
            return (List<String>) cached;
        }

        try {
            AdminVo admin = adminService.getByUsername(username);
            if (admin == null || admin.getRoleIds() == null || admin.getRoleIds().isEmpty()) {
                request.setAttribute("userModulePerms", List.of());
                return List.of();
            }

            // 缓存用户信息供数据权限使用
            request.setAttribute("currentUserId", admin.getId());
            request.setAttribute("currentDeptId", admin.getDeptId());

            List<Long> roleIds = Arrays.stream(admin.getRoleIds().split(","))
                    .map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .map(Long::parseLong)
                    .toList();

            // 获取数据范围（取最大权限：1=全部 > 2=本部门 > 3=仅本人）
            int dataScope = 1;
            try {
                List<Role> roles = roleService.getByIds(roleIds);
                dataScope = roles.stream()
                        .map(r -> r.getDataScope() != null ? r.getDataScope() : 1)
                        .min(Integer::compareTo)
                        .orElse(1);
            } catch (Exception ignored) {}
            request.setAttribute("userDataScope", dataScope);

            List<String> perms = roleService.getUserModulePerms(roleIds);
            request.setAttribute("userModulePerms", perms);
            return perms;
        } catch (Exception e) {
            return List.of();
        }
    }

    private String extractModuleName(String uri) {
        // "/admin/page" -> "admin", "/department/add" -> "department"
        String path = uri.startsWith("/") ? uri.substring(1) : uri;
        int slashIdx = path.indexOf('/');
        return slashIdx > 0 ? path.substring(0, slashIdx) : path;
    }

    private void write403(HttpServletResponse response) throws Exception {
        response.setContentType("application/json;charset=utf-8");
        response.setStatus(403);
        Result result = Result.error("403", "无权访问该模块");
        response.getWriter().write(JSON.toJSONString(result));
    }
}
