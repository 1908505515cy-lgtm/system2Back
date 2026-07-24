package com.example.controller;

import com.alibaba.excel.EasyExcel;
import com.alibaba.excel.context.AnalysisContext;
import com.alibaba.excel.metadata.data.ReadCellData;
import com.alibaba.excel.read.listener.ReadListener;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.common.*;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.*;

@RestController
@RequestMapping("/export")
public class ExportController {

    private final ModuleRegistry moduleRegistry;
    private final Map<String, GenericService<?, ?, ?>> serviceMap;

    public ExportController(ModuleRegistry moduleRegistry,
                            Map<String, GenericService<?, ?, ?>> serviceMap) {
        this.moduleRegistry = moduleRegistry;
        this.serviceMap = serviceMap;
    }

    /** 导出模块数据为 Excel */
    @GetMapping("/{module}")
    public void export(@PathVariable String module,
                       @RequestParam(required = false) String keyword,
                       HttpServletResponse response) throws IOException {
        ModuleInfo info = moduleRegistry.get(module);
        if (info == null) {
            response.setStatus(404);
            response.getWriter().write("{\"code\":\"404\",\"msg\":\"模块不存在\"}");
            return;
        }

        GenericService<?, ?, ?> service = serviceMap.get(module + "ServiceImpl");
        if (service == null) {
            service = serviceMap.get(module + "Service");
        }
        if (service == null) {
            response.setStatus(404);
            response.getWriter().write("{\"code\":\"404\",\"msg\":\"服务不存在\"}");
            return;
        }

        // 查询所有数据
        PageResult<?> pageResult = service.page(keyword, null, 1, 10000);
        List<?> records = pageResult.getRecords();

        // 构建表头和数据
        List<FieldInfo> fields = info.getFields().stream()
                .filter(FieldInfo::isShowInTable)
                .toList();

        List<List<String>> headList = new ArrayList<>();
        for (FieldInfo f : fields) {
            headList.add(List.of(f.getLabel()));
        }

        List<List<Object>> dataList = new ArrayList<>();
        for (Object record : records) {
            List<Object> row = new ArrayList<>();
            for (FieldInfo f : fields) {
                row.add(getFieldValue(record, f.getProp()));
            }
            dataList.add(row);
        }

        // 设置响应头
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        String fileName = URLEncoder.encode(info.getTitle() + "_导出", StandardCharsets.UTF_8)
                .replace("+", "%20");
        response.setHeader("Content-Disposition", "attachment;filename=" + fileName + ".xlsx");

        EasyExcel.write(response.getOutputStream())
                .head(headList)
                .sheet(info.getTitle())
                .doWrite(dataList);
    }

    /** 下载导入模板 */
    @GetMapping("/{module}/template")
    public void downloadTemplate(@PathVariable String module,
                                  HttpServletResponse response) throws IOException {
        ModuleInfo info = moduleRegistry.get(module);
        if (info == null) {
            response.setStatus(404);
            response.getWriter().write("{\"code\":\"404\",\"msg\":\"模块不存在\"}");
            return;
        }

        List<FieldInfo> fields = info.getFields().stream()
                .filter(FieldInfo::isShowInForm)
                .toList();

        List<List<String>> headList = new ArrayList<>();
        for (FieldInfo f : fields) {
            headList.add(List.of(f.getLabel()));
        }

        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        String fileName = URLEncoder.encode(info.getTitle() + "_导入模板", StandardCharsets.UTF_8)
                .replace("+", "%20");
        response.setHeader("Content-Disposition", "attachment;filename=" + fileName + ".xlsx");

        // 只写表头，不写数据
        EasyExcel.write(response.getOutputStream())
                .head(headList)
                .sheet(info.getTitle())
                .doWrite(new ArrayList<>());
    }

    /** 导入 Excel 数据到模块 */
    @PostMapping("/{module}")
    public Result importData(@PathVariable String module,
                             @RequestParam("file") MultipartFile file) {
        ModuleInfo info = moduleRegistry.get(module);
        if (info == null) return Result.error("模块不存在");

        GenericService<?, ?, ?> service = serviceMap.get(module + "ServiceImpl");
        if (service == null) service = serviceMap.get(module + "Service");
        if (service == null) return Result.error("服务不存在");

        List<FieldInfo> fields = info.getFields().stream()
                .filter(FieldInfo::isShowInForm)
                .toList();

        try {
            List<Map<String, Object>> rows = new ArrayList<>();

            EasyExcel.read(file.getInputStream(), new ReadListener<Map<Integer, String>>() {
                private List<String> headerMap;

                @Override
                public void invokeHead(Map<Integer, ReadCellData<?>> headMap, AnalysisContext context) {
                    headerMap = new ArrayList<>();
                    for (int i = 0; i < headMap.size(); i++) {
                        ReadCellData<?> cell = headMap.get(i);
                        headerMap.add(cell != null ? cell.getStringValue() : "");
                    }
                }

                @Override
                public void invoke(Map<Integer, String> data, AnalysisContext context) {
                    if (headerMap == null) return;
                    Map<String, Object> row = new LinkedHashMap<>();
                    for (int i = 0; i < headerMap.size(); i++) {
                        String header = headerMap.get(i);
                        String value = data.getOrDefault(i, "");
                        // 匹配字段
                        for (FieldInfo f : fields) {
                            if (f.getLabel().equals(header)) {
                                row.put(f.getProp(), value);
                                break;
                            }
                        }
                    }
                    if (!row.isEmpty()) rows.add(row);
                }

                @Override
                public void doAfterAllAnalysed(AnalysisContext context) {}
            }).sheet().doRead();

            int success = 0;
            List<Map<String, Object>> failures = new ArrayList<>();
            for (int i = 0; i < rows.size(); i++) {
                try {
                    service.createFromMap(rows.get(i));
                    success++;
                } catch (Exception e) {
                    Map<String, Object> fail = new LinkedHashMap<>();
                    fail.put("row", i + 2); // Excel 行号从 2 开始（第 1 行是表头）
                    fail.put("reason", e.getMessage());
                    failures.add(fail);
                }
            }

            Map<String, Object> result = new LinkedHashMap<>();
            result.put("total", rows.size());
            result.put("success", success);
            result.put("failed", failures.size());
            if (!failures.isEmpty()) {
                result.put("failures", failures);
            }
            return Result.success(result);
        } catch (IOException e) {
            return Result.error("文件读取失败: " + e.getMessage());
        }
    }

    private Object getFieldValue(Object obj, String fieldName) {
        try {
            var field = findField(obj.getClass(), fieldName);
            if (field == null) return "";
            field.setAccessible(true);
            Object value = field.get(obj);
            return value != null ? value : "";
        } catch (Exception e) {
            return "";
        }
    }

    private java.lang.reflect.Field findField(Class<?> clazz, String name) {
        Class<?> current = clazz;
        while (current != null) {
            try {
                return current.getDeclaredField(name);
            } catch (NoSuchFieldException e) {
                current = current.getSuperclass();
            }
        }
        return null;
    }
}
