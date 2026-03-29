// src/main/java/com/example/controller/AdminController.java
package com.example.controller;

import com.example.dto.AdminDto;
import com.example.service.AdminService;
import com.example.vo.AdminVo;
import com.example.common.PageResult;
import com.example.common.Result;
import org.springframework.web.bind.annotation.*;

/**
 * 系统管理员 Controller
 */
@RestController
@RequestMapping("/admin")
public class AdminController {

    private final AdminService adminService;

    public AdminController(AdminService adminService) {
        this.adminService = adminService;
    }

    @PostMapping("/add")
    public Result add(@RequestBody AdminDto dto) {
        adminService.add(dto);
        return Result.success("新增成功");
    }

    @PutMapping("/update")
    public Result update(@RequestBody AdminDto dto) {
        adminService.update(dto);
        return Result.success("更新成功");
    }

    @DeleteMapping("/delete/{id}")
    public Result delete(@PathVariable Long id) {
        adminService.delete(id);
        return Result.success("删除成功");
    }

    @GetMapping("/get/{id}")
    public Result getById(@PathVariable Long id) {
        AdminVo vo = adminService.getById(id);
        return Result.success(vo);
    }

    @GetMapping("/page")
    public Result pageList(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Integer status,
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize) {

        PageResult<AdminVo> page = adminService.pageList(keyword, status, pageNum, pageSize);
        return Result.success(page);
    }

    @PutMapping("/status")
    public Result updateStatus(@RequestParam Long id, @RequestParam Integer status) {
        adminService.updateStatus(id, status);
        return Result.success("状态修改成功");
    }
}