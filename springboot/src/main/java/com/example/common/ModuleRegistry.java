package com.example.common;

import com.baomidou.mybatisplus.annotation.TableName;
import com.example.common.annotations.FieldMeta;
import com.example.common.annotations.ModuleMeta;
import com.example.service.DictDataService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.context.annotation.Lazy;
import org.springframework.core.type.filter.AnnotationTypeFilter;
import org.springframework.stereotype.Component;

import java.lang.reflect.Field;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * 模块注册表
 * 启动时自动扫描带 @ModuleMeta 注解的实体类，构建模块元数据
 */
@Component
public class ModuleRegistry implements InitializingBean {

    private static final Logger log = LoggerFactory.getLogger(ModuleRegistry.class);

    private final Map<String, ModuleInfo> modules = new ConcurrentHashMap<>();
    private final DictDataService dictDataService;

    private static final String ENTITY_PACKAGE = "com.example.entity";

    public ModuleRegistry(@Lazy DictDataService dictDataService) {
        this.dictDataService = dictDataService;
    }

    @Override
    public void afterPropertiesSet() {
        scanModules();
        resolveDictOptions();
    }

    private void scanModules() {
        ClassPathScanningCandidateComponentProvider scanner =
                new ClassPathScanningCandidateComponentProvider(false);
        scanner.addIncludeFilter(new AnnotationTypeFilter(ModuleMeta.class));

        for (var beanDef : scanner.findCandidateComponents(ENTITY_PACKAGE)) {
            try {
                Class<?> clazz = Class.forName(beanDef.getBeanClassName());
                ModuleMeta meta = clazz.getAnnotation(ModuleMeta.class);
                TableName tableName = clazz.getAnnotation(TableName.class);

                List<FieldInfo> fields = scanFields(clazz);

                // 解析 features
                List<String> features = Arrays.stream(meta.features().split(","))
                        .map(String::trim)
                        .filter(s -> !s.isEmpty())
                        .collect(Collectors.toList());

                ModuleInfo info = new ModuleInfo();
                info.setName(meta.name());
                info.setTitle(meta.title());
                info.setIcon(meta.icon());
                info.setApiBase("/" + meta.name());
                info.setTableName(tableName != null ? tableName.value() : "");
                info.setFields(fields);
                info.setFeatures(features);

                modules.put(meta.name(), info);
            } catch (Exception e) {
                log.error("[ModuleRegistry] 扫描模块失败: {}", beanDef.getBeanClassName(), e);
            }
        }
    }

    /** 递归扫描实体及其父类的字段 */
    private List<FieldInfo> scanFields(Class<?> clazz) {
        List<FieldInfo> fields = new ArrayList<>();
        Set<String> seen = new HashSet<>();

        Class<?> current = clazz;
        while (current != null && current != Object.class) {
            for (Field field : current.getDeclaredFields()) {
                if (seen.contains(field.getName())) continue;
                seen.add(field.getName());

                FieldMeta meta = field.getAnnotation(FieldMeta.class);
                if (meta == null) continue;

                // 驼峰转下划线
                String column = camelToUnderscore(field.getName());

                FieldInfo info = new FieldInfo();
                info.setProp(field.getName());
                info.setColumn(column);
                info.setLabel(meta.label());
                info.setType(meta.type());
                info.setSearchable(meta.searchable());
                info.setShowInTable(meta.showInTable());
                info.setShowInForm(meta.showInForm());
                info.setRequired(meta.required());
                info.setPlaceholder(meta.placeholder());
                info.setWidth(meta.width());
                info.setDisabledOnEdit(meta.disabledOnEdit());
                info.setOptions(parseOptions(meta.options()));
                info.setDictCode(meta.dictCode());
                fields.add(info);
            }
            current = current.getSuperclass();
        }
        return fields;
    }

    /** 解析选项字符串 "正常:1,禁用:0" 为 OptionItem 列表 */
    private List<OptionItem> parseOptions(String optionsStr) {
        if (optionsStr == null || optionsStr.trim().isEmpty()) return null;
        return Arrays.stream(optionsStr.split(","))
                .map(pair -> {
                    // limit=2 保证标签中含冒号时不会被错误分割（如 "时间:10:00:1"）
                    String[] parts = pair.trim().split(":", 2);
                    if (parts.length == 2) {
                        String label = parts[0].trim();
                        String valueStr = parts[1].trim();
                        Object value = valueStr;
                        try { value = Integer.parseInt(valueStr); } catch (NumberFormatException ignored) {}
                        return new OptionItem(label, value);
                    }
                    return null;
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    public ModuleInfo get(String name) {
        return modules.get(name);
    }

    public List<ModuleInfo> list() {
        return new ArrayList<>(modules.values());
    }

    /** 获取所有可搜索字段（供 AI 使用） */
    public List<FieldInfo> getSearchableFields(String moduleName) {
        ModuleInfo info = modules.get(moduleName);
        if (info == null) return Collections.emptyList();
        return info.getFields().stream()
                .filter(FieldInfo::isSearchable)
                .collect(Collectors.toList());
    }

    /** 为设置了 dictCode 的字段加载字典选项 */
    private void resolveDictOptions() {
        for (ModuleInfo mod : modules.values()) {
            for (FieldInfo field : mod.getFields()) {
                if (field.getDictCode() != null && !field.getDictCode().isEmpty()) {
                    try {
                        List<OptionItem> dictOptions = dictDataService.getOptionsByTypeCode(field.getDictCode());
                        if (dictOptions != null && !dictOptions.isEmpty()) {
                            field.setOptions(dictOptions);
                        }
                    } catch (Exception e) {
                        log.warn("[ModuleRegistry] 加载字典选项失败: {}", field.getDictCode(), e);
                    }
                }
            }
        }
    }

    private String camelToUnderscore(String camel) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < camel.length(); i++) {
            char c = camel.charAt(i);
            if (Character.isUpperCase(c)) {
                // 前一个字符是小写，或者后一个字符是小写（且前一个不是下划线）时插入分隔符
                boolean prevLower = i > 0 && Character.isLowerCase(camel.charAt(i - 1));
                boolean nextLower = i + 1 < camel.length() && Character.isLowerCase(camel.charAt(i + 1));
                if (i > 0 && (prevLower || nextLower)) {
                    sb.append('_');
                }
                sb.append(Character.toLowerCase(c));
            } else {
                sb.append(c);
            }
        }
        return sb.toString();
    }
}
