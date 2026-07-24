package com.example.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.example.common.GenericController;
import com.example.common.GenericService;
import com.example.common.ModuleRegistry;
import com.example.common.PageResult;
import com.example.common.Result;
import com.example.entity.AuditLog;
import com.example.entity.Dashboard;
import com.example.mapper.AdminMapper;
import com.example.mapper.AuditLogMapper;
import com.example.service.DashboardService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;

@RestController
@RequestMapping("/dashboard")
public class DashboardController extends GenericController<Dashboard, Dashboard, Dashboard> {

    private final DashboardService dashboardService;
    private final ModuleRegistry moduleRegistry;
    private final AdminMapper adminMapper;
    private final AuditLogMapper auditLogMapper;
    private final Map<String, GenericService<?, ?, ?>> serviceMap;

    public DashboardController(DashboardService dashboardService,
                                ModuleRegistry moduleRegistry, AdminMapper adminMapper,
                                AuditLogMapper auditLogMapper,
                                Map<String, GenericService<?, ?, ?>> serviceMap) {
        this.dashboardService = dashboardService;
        this.moduleRegistry = moduleRegistry;
        this.adminMapper = adminMapper;
        this.auditLogMapper = auditLogMapper;
        this.serviceMap = serviceMap;
    }

    @Override
    protected DashboardService getService() {
        return dashboardService;
    }

    // ==================== 仪表盘管理 ====================

    /** 获取默认仪表盘 */
    @GetMapping("/default")
    public Result getDefault() {
        Dashboard d = dashboardService.getDefault();
        return d != null ? Result.success(d) : Result.error("未设置默认仪表盘");
    }

    /** 设为默认仪表盘 */
    @PostMapping("/set-default/{id}")
    public Result setDefault(@PathVariable Long id) {
        dashboardService.setDefault(id);
        return Result.success("已设为默认");
    }

    // ==================== 原有数据统计端点 ====================

    /** 首页统计数据 */
    @GetMapping("/stats")
    public Result stats() {
        Map<String, Object> data = new HashMap<>();
        data.put("moduleCount", moduleRegistry.list().size());
        data.put("adminCount", adminMapper.selectCount(null, null));

        LocalDateTime todayStart = LocalDateTime.of(LocalDate.now(), LocalTime.MIN);
        data.put("todayOps", auditLogMapper.selectCount(
                new QueryWrapper<AuditLog>().ge("operate_time", todayStart)));
        data.put("totalOps", auditLogMapper.selectCount(new QueryWrapper<>()));

        return Result.success(data);
    }

    /** 最近操作日志 */
    @GetMapping("/recent-logs")
    public Result recentLogs() {
        List<AuditLog> logs = auditLogMapper.selectList(
                new QueryWrapper<AuditLog>()
                        .orderByDesc("operate_time")
                        .last("LIMIT 10"));
        return Result.success(logs);
    }

    /** 操作类型分布（饼图数据） */
    @GetMapping("/action-distribution")
    public Result actionDistribution() {
        List<Map<String, Object>> grouped = auditLogMapper.selectActionDistribution();

        List<Map<String, Object>> result = new ArrayList<>();
        Map<String, String> labelMap = Map.of(
                "create", "新增", "update", "修改", "delete", "删除",
                "reset_password", "重置密码", "ai_confirm", "AI确认");
        Map<String, String> colorMap = Map.of(
                "create", "#67c23a", "update", "#409eff", "delete", "#f56c6c",
                "reset_password", "#e6a23c", "ai_confirm", "#5f56e7");

        for (var entry : grouped) {
            String action = (String) entry.get("action");
            Long count = ((Number) entry.get("count")).longValue();
            Map<String, Object> item = new HashMap<>();
            item.put("name", labelMap.getOrDefault(action != null ? action : "unknown", action));
            item.put("value", count);
            item.put("color", colorMap.getOrDefault(action != null ? action : "unknown", "#909399"));
            result.add(item);
        }
        return Result.success(result);
    }

    /** 各模块数据量统计（柱状图数据） */
    @GetMapping("/module-counts")
    public Result moduleCounts() {
        List<Map<String, Object>> result = new ArrayList<>();
        for (var info : moduleRegistry.list()) {
            GenericService<?, ?, ?> service = serviceMap.get(info.getName() + "ServiceImpl");
            if (service == null) service = serviceMap.get(info.getName() + "Service");
            if (service == null) continue;

            try {
                PageResult<?> page = service.page(null, null, 1, 1);
                Map<String, Object> item = new HashMap<>();
                item.put("name", info.getTitle());
                item.put("count", page.getTotal());
                result.add(item);
            } catch (Exception ignored) {
            }
        }
        return Result.success(result);
    }

    /** 近7天操作趋势（折线图数据） */
    @GetMapping("/daily-trend")
    public Result dailyTrend() {
        LocalDateTime since = LocalDateTime.of(LocalDate.now().minusDays(6), LocalTime.MIN);
        List<Map<String, Object>> raw = auditLogMapper.selectDailyTrend(since);

        Map<String, Long> dateCountMap = new LinkedHashMap<>();
        for (int i = 6; i >= 0; i--) {
            String date = LocalDate.now().minusDays(i).toString();
            dateCountMap.put(date, 0L);
        }
        for (var row : raw) {
            String date = row.get("date").toString();
            Long count = ((Number) row.get("count")).longValue();
            dateCountMap.put(date, count);
        }

        List<Map<String, Object>> result = new ArrayList<>();
        dateCountMap.forEach((date, count) -> {
            Map<String, Object> item = new HashMap<>();
            item.put("date", date.substring(5));
            item.put("count", count);
            result.add(item);
        });
        return Result.success(result);
    }

    // ==================== 通用数据聚合查询 ====================

    /** 通用数据聚合查询 */
    @PostMapping("/query")
    public Result query(@RequestBody Map<String, Object> params) {
        String moduleName = (String) params.get("module");
        String field = (String) params.get("field");
        String aggregation = (String) params.getOrDefault("aggregation", "count");

        if (moduleName == null || moduleName.isBlank()) {
            return Result.error("module 不能为空");
        }

        // 查找对应的 Service
        GenericService<?, ?, ?> service = serviceMap.get(moduleName + "ServiceImpl");
        if (service == null) service = serviceMap.get(moduleName + "Service");
        if (service == null) {
            return Result.error("模块不存在: " + moduleName);
        }

        try {
            switch (aggregation) {
                case "count": {
                    PageResult<?> page = service.page(null, null, 1, 1);
                    Map<String, Object> item = new HashMap<>();
                    item.put("name", "总数");
                    item.put("value", page.getTotal());
                    return Result.success(item);
                }
                case "group_count": {
                    if (field == null || field.isBlank()) {
                        return Result.error("group_count 需要 field 参数");
                    }
                    // 使用 page 获取全量数据后分组（简化实现）
                    PageResult<?> page = service.page(null, null, 1, 10000);
                    Map<Object, Long> grouped = new LinkedHashMap<>();
                    for (Object record : page.getRecords()) {
                        Object val = getFieldValue(record, field);
                        grouped.merge(val != null ? val : "未知", 1L, Long::sum);
                    }
                    List<Map<String, Object>> result = new ArrayList<>();
                    grouped.forEach((k, v) -> {
                        Map<String, Object> item = new HashMap<>();
                        item.put("name", String.valueOf(k));
                        item.put("value", v);
                        result.add(item);
                    });
                    return Result.success(result);
                }
                case "sum":
                case "avg":
                case "max":
                case "min": {
                    if (field == null || field.isBlank()) {
                        return Result.error(aggregation + " 需要 field 参数");
                    }
                    PageResult<?> page = service.page(null, null, 1, 10000);
                    double result = 0;
                    int count = 0;
                    double min = Double.MAX_VALUE;
                    double max = Double.MIN_VALUE;
                    for (Object record : page.getRecords()) {
                        Object val = getFieldValue(record, field);
                        if (val instanceof Number) {
                            double d = ((Number) val).doubleValue();
                            result += d;
                            count++;
                            min = Math.min(min, d);
                            max = Math.max(max, d);
                        }
                    }
                    Map<String, Object> item = new HashMap<>();
                    switch (aggregation) {
                        case "sum" -> { item.put("name", "合计"); item.put("value", result); }
                        case "avg" -> { item.put("name", "平均"); item.put("value", count > 0 ? result / count : 0); }
                        case "max" -> { item.put("name", "最大"); item.put("value", max); }
                        case "min" -> { item.put("name", "最小"); item.put("value", min); }
                    }
                    return Result.success(item);
                }
                default:
                    return Result.error("不支持的聚合类型: " + aggregation);
            }
        } catch (Exception e) {
            return Result.error("查询失败: " + e.getMessage());
        }
    }

    /** 通过反射获取字段值 */
    private Object getFieldValue(Object obj, String fieldName) {
        try {
            var field = obj.getClass().getDeclaredField(fieldName);
            field.setAccessible(true);
            return field.get(obj);
        } catch (Exception e) {
            return null;
        }
    }
}
