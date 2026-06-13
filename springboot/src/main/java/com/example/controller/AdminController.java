package com.example.controller;

import com.alibaba.fastjson2.JSONObject;
import com.example.common.GenericController;
import com.example.common.Result;
import com.example.dto.AdminDto;
import com.example.entity.Admin;
import com.example.service.AdminService;
import com.example.vo.AdminVo;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest;

/**
 * 管理员 Controller
 * 继承泛型基类获得标准 CRUD，只保留 Admin 特有端点
 */
@RestController
@RequestMapping("/admin")
public class AdminController extends GenericController<Admin, AdminDto, AdminVo> {

    private final AdminService adminService;

    @Value("${ai.callback.secret}")
    private String aiCallbackSecret;

    public AdminController(AdminService adminService) {
        this.adminService = adminService;
    }

    @Override
    protected AdminService getService() {
        return adminService;
    }

    // ==================== Admin 特有端点 ====================

    /**
     * AI 改名确认执行
     */
    @PostMapping("/ai-confirm")
    public Result confirmAiAction(@RequestBody AdminDto dto) {
        boolean success = adminService.updateAdminNameFromAi(dto);
        if (success) {
            return Result.success("确认执行成功，已将 " + dto.getAdminCode() + " 的登录名修改为 " + dto.getNewName());
        } else {
            return Result.error("执行失败，未影响任何行。请确认 adminCode 是否存在。");
        }
    }

    /**
     * Python 回调端点（排除在登录拦截器之外，需 API Key 认证）
     */
    @PutMapping("/ai-update-name")
    public JSONObject updateNameFromAgent(@RequestBody AdminDto dto, HttpServletRequest request) {
        JSONObject response = new JSONObject();
        // 校验 API Key
        String apiKey = request.getHeader("X-API-Key");
        if (aiCallbackSecret == null || !aiCallbackSecret.equals(apiKey)) {
            response.put("code", 401);
            response.put("msg", "API Key 无效");
            return response;
        }
        boolean success = adminService.updateAdminNameFromAi(dto);
        if (success) {
            response.put("code", 200);
            response.put("msg", "数据库已同步变更成功");
        } else {
            response.put("code", 500);
            response.put("msg", "未影响任何行，请确认 adminCode 是否存在");
        }
        return response;
    }

    /**
     * 重置密码
     */
    @PutMapping("/reset-password")
    public Result resetPassword(@RequestParam Long id) {
        adminService.resetPassword(id);
        return Result.success("密码已重置");
    }
}
