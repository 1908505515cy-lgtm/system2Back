package com.example.controller;

import com.example.common.Result;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;

import java.io.*;
import java.nio.file.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/backup")
public class BackupController {

    private final JdbcTemplate jdbcTemplate;

    @Value("${backup.dir:./backups}")
    private String backupDir;

    public BackupController(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    /** 执行备份 */
    @PostMapping("/create")
    public Result createBackup() {
        try {
            Path dir = Paths.get(backupDir);
            if (!Files.exists(dir)) {
                Files.createDirectories(dir);
            }

            String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
            String filename = "backup_" + timestamp + ".sql";
            Path filePath = dir.resolve(filename);

            StringBuilder sql = new StringBuilder();
            sql.append("-- System2 Backup ").append(timestamp).append("\n");
            sql.append("SET FOREIGN_KEY_CHECKS=0;\n\n");

            // 获取所有表
            List<String> tables = jdbcTemplate.queryForList(
                    "SHOW TABLES", String.class);

            for (String table : tables) {
                // 获取建表语句
                List<Map<String, Object>> createResult = jdbcTemplate.queryForList("SHOW CREATE TABLE " + table);
                if (!createResult.isEmpty()) {
                    String createSql = (String) ((Map<?, ?>) createResult.get(0)).get("Create Table");
                    sql.append("DROP TABLE IF EXISTS `").append(table).append("`;\n");
                    sql.append(createSql).append(";\n\n");
                }

                // 获取数据
                List<Map<String, Object>> rows = jdbcTemplate.queryForList("SELECT * FROM `" + table + "`");
                if (!rows.isEmpty()) {
                    for (Map<String, Object> row : rows) {
                        sql.append("INSERT INTO `").append(table).append("` VALUES (");
                        List<String> values = new ArrayList<>();
                        for (Object val : row.values()) {
                            if (val == null) {
                                values.add("NULL");
                            } else if (val instanceof Number) {
                                values.add(val.toString());
                            } else {
                                String escaped = val.toString()
                                        .replace("\\", "\\\\")
                                        .replace("'", "\\'");
                                values.add("'" + escaped + "'");
                            }
                        }
                        sql.append(String.join(", ", values));
                        sql.append(");\n");
                    }
                    sql.append("\n");
                }
            }

            sql.append("SET FOREIGN_KEY_CHECKS=1;\n");

            Files.writeString(filePath, sql.toString());

            Map<String, Object> result = new HashMap<>();
            result.put("filename", filename);
            result.put("size", Files.size(filePath));
            result.put("tables", tables.size());
            return Result.success(result);
        } catch (Exception e) {
            return Result.error("备份失败: " + e.getMessage());
        }
    }

    /** 获取备份列表 */
    @GetMapping("/list")
    public Result listBackups() {
        try {
            Path dir = Paths.get(backupDir);
            if (!Files.exists(dir)) {
                return Result.success(Collections.emptyList());
            }

            List<Map<String, Object>> backups = Files.list(dir)
                    .filter(p -> p.toString().endsWith(".sql"))
                    .sorted(Comparator.comparing(Path::getFileName).reversed())
                    .map(p -> {
                        Map<String, Object> item = new LinkedHashMap<>();
                        item.put("filename", p.getFileName().toString());
                        try {
                            item.put("size", Files.size(p));
                            item.put("lastModified", Files.getLastModifiedTime(p).toInstant().toString());
                        } catch (IOException ignored) {}
                        return item;
                    })
                    .collect(Collectors.toList());

            return Result.success(backups);
        } catch (Exception e) {
            return Result.error("获取备份列表失败: " + e.getMessage());
        }
    }

    /** 恢复备份 */
    @PostMapping("/restore")
    public Result restoreBackup(@RequestParam String filename) {
        try {
            Path filePath = validateFilename(filename);
            if (!Files.exists(filePath)) {
                return Result.error("备份文件不存在");
            }

            String content = Files.readString(filePath);
            // 按分号分割执行（简单实现）
            String[] statements = content.split(";\n");
            int executed = 0;
            for (String stmt : statements) {
                String trimmed = stmt.trim();
                if (!trimmed.isEmpty() && !trimmed.startsWith("--") && !trimmed.startsWith("SET")) {
                    try {
                        jdbcTemplate.execute(trimmed);
                        executed++;
                    } catch (Exception e) {
                        // 跳过单条失败继续执行
                    }
                }
            }
            return Result.success("恢复完成，执行了 " + executed + " 条语句");
        } catch (Exception e) {
            return Result.error("恢复失败: " + e.getMessage());
        }
    }

    /** 删除备份 */
    @DeleteMapping("/{filename}")
    public Result deleteBackup(@PathVariable String filename) {
        try {
            Path filePath = validateFilename(filename);
            if (Files.exists(filePath)) {
                Files.delete(filePath);
                return Result.success("删除成功");
            }
            return Result.error("文件不存在");
        } catch (Exception e) {
            return Result.error("删除失败: " + e.getMessage());
        }
    }

    /** 校验文件名，防止路径遍历攻击 */
    private Path validateFilename(String filename) {
        if (filename == null || filename.contains("..") || filename.contains("/") || filename.contains("\\")) {
            throw new IllegalArgumentException("非法文件名");
        }
        Path dir = Paths.get(backupDir).toAbsolutePath().normalize();
        Path filePath = dir.resolve(filename).normalize();
        if (!filePath.startsWith(dir)) {
            throw new IllegalArgumentException("文件路径越界");
        }
        return filePath;
    }
}
