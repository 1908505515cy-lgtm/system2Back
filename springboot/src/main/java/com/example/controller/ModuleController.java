package com.example.controller;

import com.example.common.ModuleInfo;
import com.example.common.ModuleRegistry;
import com.example.common.Result;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 模块元数据 API
 * 供前端和 AI 查询已注册的模块信息
 */
@RestController
@RequestMapping("/module")
public class ModuleController {

    private final ModuleRegistry moduleRegistry;

    public ModuleController(ModuleRegistry moduleRegistry) {
        this.moduleRegistry = moduleRegistry;
    }

    /** 获取所有已注册模块 */
    @GetMapping("/list")
    public Result list() {
        List<ModuleInfo> modules = moduleRegistry.list();
        return Result.success(modules);
    }

    /** 获取单个模块详情（含字段元数据） */
    @GetMapping("/info/{name}")
    public Result info(@PathVariable String name) {
        ModuleInfo info = moduleRegistry.get(name);
        if (info == null) {
            return Result.error("404", "模块不存在: " + name);
        }
        return Result.success(info);
    }
}
