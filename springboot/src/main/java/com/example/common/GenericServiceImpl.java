package com.example.common;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.exception.CustomException;
import com.example.common.enums.ResultCodeEnum;
import org.springframework.beans.BeanUtils;
import org.springframework.jdbc.core.JdbcTemplate;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.HashSet;
import java.util.Arrays;

/**
 * 泛型 Service 实现基类
 * 提供标准 CRUD 逻辑，子类通过覆写钩子方法定制行为
 *
 * @param <E> 实体类型
 * @param <D> DTO 类型
 * @param <V> VO 类型
 */
public abstract class GenericServiceImpl<E, D, V> implements GenericService<E, D, V> {

    protected final BaseMapper<E> mapper;
    private final Class<E> entityClass;
    private final Class<V> voClass;
    private final JdbcTemplate jdbcTemplate;

    @SuppressWarnings("unchecked")
    public GenericServiceImpl(BaseMapper<E> mapper, JdbcTemplate jdbcTemplate) {
        this.mapper = mapper;
        this.jdbcTemplate = jdbcTemplate;
        ParameterizedType type = (ParameterizedType) getClass().getGenericSuperclass();
        this.entityClass = (Class<E>) type.getActualTypeArguments()[0];
        this.voClass = (Class<V>) type.getActualTypeArguments()[2];
    }

    // ==================== 标准 CRUD ====================

    @Override
    public void add(D dto) {
        beforeAdd(dto);
        E entity = toEntity(dto);
        if (mapper.insert(entity) == 0) {
            throw new CustomException(ResultCodeEnum.SYSTEM_ERROR);
        }
        afterAdd(entity);
    }

    @Override
    public void update(D dto) {
        beforeUpdate(dto);
        E entity = toEntity(dto);
        if (mapper.updateById(entity) == 0) {
            throw new CustomException(ResultCodeEnum.SYSTEM_ERROR);
        }
    }

    @Override
    public void delete(Long id) {
        if (mapper.deleteById(id) == 0) {
            throw new CustomException(ResultCodeEnum.NOT_FOUND);
        }
    }

    @Override
    public V getById(Long id) {
        E entity = mapper.selectById(id);
        if (entity == null) {
            throw new CustomException(ResultCodeEnum.NOT_FOUND);
        }
        return toVo(entity);
    }

    @Override
    public PageResult<V> page(String keyword, Integer status, Integer pageNum, Integer pageSize) {
        Page<E> page = new Page<>(pageNum, pageSize);
        QueryWrapper<E> wrapper = new QueryWrapper<>();

        if (keyword != null && !keyword.isEmpty()) {
            buildKeywordCondition(wrapper, keyword);
        }
        if (status != null) {
            wrapper.eq("status", status);
        }

        // 数据权限过滤
        applyDataScope(wrapper);

        // 高级筛选
        applyAdvancedFilter(wrapper);

        String orderColumn = getOrderColumn();
        if (orderColumn != null) {
            wrapper.orderByDesc(orderColumn);
        }

        Page<E> result = mapper.selectPage(page, wrapper);

        List<V> voList = result.getRecords().stream()
                .map(this::toVo)
                .collect(Collectors.toList());

        return new PageResult<>(voList, result.getTotal(), pageNum, pageSize);
    }

    @Override
    public void updateStatus(Long id, Integer status) {
        E entity = newEntity();
        setField(entity, "id", id);
        setField(entity, "status", status);
        if (mapper.updateById(entity) == 0) {
            throw new CustomException(ResultCodeEnum.SYSTEM_ERROR);
        }
    }

    /** 敏感字段黑名单，不允许通过 updateField 接口修改 */
    private static final Set<String> SENSITIVE_FIELDS = new HashSet<>(Arrays.asList(
            "password", "deleted", "roleIds", "role", "accountCode"
    ));

    @Override
    public void updateField(Long id, String fieldName, Object value) {
        if (SENSITIVE_FIELDS.contains(fieldName)) {
            throw new CustomException(ResultCodeEnum.PARAM_ERROR.getCode(),
                    "不允许通过此接口修改敏感字段: " + fieldName);
        }
        E entity = newEntity();
        setField(entity, "id", id);
        setField(entity, fieldName, value);
        if (mapper.updateById(entity) == 0) {
            throw new CustomException(ResultCodeEnum.SYSTEM_ERROR);
        }
    }

    @Override
    public void batchDelete(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            throw new CustomException(ResultCodeEnum.SYSTEM_ERROR);
        }
        if (mapper.deleteBatchIds(ids) == 0) {
            throw new CustomException(ResultCodeEnum.SYSTEM_ERROR);
        }
    }

    // ==================== AI 操作 ====================

    @Override
    public V createFromMap(Map<String, Object> fields) {
        E entity = newEntity();
        Set<String> allowedColumns = getAllFieldNames(entityClass);
        fields.forEach((key, value) -> {
            if (allowedColumns.contains(key)) {
                setField(entity, key, value);
            }
        });
        if (mapper.insert(entity) == 0) {
            throw new CustomException(ResultCodeEnum.SYSTEM_ERROR);
        }
        return toVo(entity);
    }

    @Override
    public List<V> queryByFields(Map<String, Object> fields) {
        // 白名单：只允许实体类及其父类声明的字段作为列名，防止 SQL 注入
        Set<String> allowedColumns = getAllFieldNames(entityClass);
        QueryWrapper<E> wrapper = new QueryWrapper<>();
        fields.forEach((key, value) -> {
            if (value != null && allowedColumns.contains(key)) {
                wrapper.eq(key, value);
            }
        });
        String orderColumn = getOrderColumn();
        if (orderColumn != null) {
            wrapper.orderByDesc(orderColumn);
        }
        List<E> entities = mapper.selectList(wrapper);
        return entities.stream().map(this::toVo).collect(Collectors.toList());
    }

    // ==================== 模板方法（子类覆写） ====================

    /** DTO -> Entity，默认 BeanUtils 浅拷贝 */
    protected E toEntity(D dto) {
        E entity = newEntity();
        BeanUtils.copyProperties(dto, entity);
        return entity;
    }

    /** Entity -> VO，默认 BeanUtils 浅拷贝 */
    protected V toVo(E entity) {
        V vo = newVo();
        BeanUtils.copyProperties(entity, vo);
        return vo;
    }

    /** 新增前钩子：做唯一性校验、密码加密等 */
    protected void beforeAdd(D dto) {}

    /** 新增后钩子 */
    protected void afterAdd(E entity) {}

    /** 修改前钩子 */
    protected void beforeUpdate(D dto) {}

    /** 构建关键词搜索条件，子类覆写以指定搜索字段 */
    protected void buildKeywordCondition(QueryWrapper<E> wrapper, String keyword) {
        // 默认不搜索，子类应覆写
    }

    /** 默认排序列，子类可覆写返回 null 取消排序 */
    protected String getOrderColumn() {
        return "create_time";
    }

    /**
     * 从请求上下文读取数据范围并应用过滤
     */
    private void applyDataScope(QueryWrapper<E> wrapper) {
        try {
            ServletRequestAttributes attrs =
                    (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attrs == null) return;

            HttpServletRequest request = attrs.getRequest();
            Object scopeObj = request.getAttribute("userDataScope");
            if (scopeObj == null) return;

            int dataScope = (int) scopeObj;
            if (dataScope == 1) return; // 全部数据，不过滤

            Long currentUserId = getLongAttr(request, "currentUserId");
            Long currentDeptId = getLongAttr(request, "currentDeptId");

            buildDataScopeCondition(wrapper, dataScope, currentUserId, currentDeptId);
        } catch (Exception ignored) {
            // 数据权限异常不影响查询
        }
    }

    private Long getLongAttr(HttpServletRequest request, String key) {
        Object val = request.getAttribute(key);
        if (val instanceof Long) return (Long) val;
        if (val instanceof Number) return ((Number) val).longValue();
        return null;
    }

    /**
     * 应用高级筛选条件（从前端 _filter 参数解析）
     */
    private void applyAdvancedFilter(QueryWrapper<E> wrapper) {
        try {
            ServletRequestAttributes attrs =
                    (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attrs == null) return;

            HttpServletRequest request = attrs.getRequest();
            String filterJson = request.getParameter("_filter");
            String logic = request.getParameter("_logic");

            if (filterJson == null || filterJson.isEmpty()) return;

            JSONArray conditions = JSON.parseArray(filterJson);
            if (conditions == null || conditions.isEmpty()) return;

            Set<String> allowedColumns = getAllFieldNames(entityClass);
            boolean isOr = "OR".equalsIgnoreCase(logic);

            for (int i = 0; i < conditions.size(); i++) {
                JSONObject cond = conditions.getJSONObject(i);
                String field = cond.getString("field");
                String operator = cond.getString("operator");
                String value = cond.getString("value");

                if (field == null || operator == null) continue;
                if (!allowedColumns.contains(field)) continue; // 白名单校验，防注入

                String column = camelToUnderscore(field);

                switch (operator) {
                    case "eq" -> {
                        if (isOr) wrapper.or().eq(column, value);
                        else wrapper.eq(column, value);
                    }
                    case "ne" -> {
                        if (isOr) wrapper.or().ne(column, value);
                        else wrapper.ne(column, value);
                    }
                    case "like" -> {
                        if (isOr) wrapper.or().like(column, value);
                        else wrapper.like(column, value);
                    }
                    case "gt" -> {
                        if (isOr) wrapper.or().gt(column, value);
                        else wrapper.gt(column, value);
                    }
                    case "lt" -> {
                        if (isOr) wrapper.or().lt(column, value);
                        else wrapper.lt(column, value);
                    }
                    case "isNull" -> {
                        if (isOr) wrapper.or().isNull(column);
                        else wrapper.isNull(column);
                    }
                    case "isNotNull" -> {
                        if (isOr) wrapper.or().isNotNull(column);
                        else wrapper.isNotNull(column);
                    }
                }
            }
        } catch (Exception ignored) {
            // 高级筛选异常不影响查询
        }
    }

    private String camelToUnderscore(String camel) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < camel.length(); i++) {
            char c = camel.charAt(i);
            if (Character.isUpperCase(c)) {
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

    /**
     * 数据权限条件钩子
     * 子类覆写以添加行级数据过滤条件
     * 默认不过滤（全部数据可见）
     *
     * @param wrapper 查询条件
     * @param dataScope 数据范围：1=全部 2=本部门 3=仅本人
     * @param currentUserId 当前用户ID
     * @param currentDeptId 当前用户部门ID
     */
    protected void buildDataScopeCondition(QueryWrapper<E> wrapper, int dataScope,
                                            Long currentUserId, Long currentDeptId) {
        // 默认不过滤，子类按需覆写
    }

    // ==================== 工具方法 ====================

    protected E newEntity() {
        try {
            return entityClass.getDeclaredConstructor().newInstance();
        } catch (Exception e) {
            throw new CustomException(ResultCodeEnum.SYSTEM_ERROR);
        }
    }

    protected V newVo() {
        try {
            return voClass.getDeclaredConstructor().newInstance();
        } catch (Exception e) {
            throw new CustomException(ResultCodeEnum.SYSTEM_ERROR);
        }
    }

    protected void setField(Object obj, String fieldName, Object value) {
        try {
            Field field = findField(obj.getClass(), fieldName);
            if (field == null) {
                throw new CustomException(ResultCodeEnum.PARAM_ERROR.getCode(),
                        "字段不存在: " + fieldName);
            }
            field.setAccessible(true);
            field.set(obj, value);
        } catch (CustomException e) {
            throw e;
        } catch (Exception e) {
            throw new CustomException(ResultCodeEnum.SYSTEM_ERROR.getCode(),
                    "字段赋值失败: " + fieldName + " - " + e.getMessage());
        }
    }

    private Field findField(Class<?> clazz, String fieldName) {
        Class<?> current = clazz;
        while (current != null) {
            try {
                return current.getDeclaredField(fieldName);
            } catch (NoSuchFieldException e) {
                current = current.getSuperclass();
            }
        }
        return null;
    }

    /** 获取实体类及所有父类的字段名集合（用于白名单校验） */
    private Set<String> getAllFieldNames(Class<?> clazz) {
        Set<String> names = new java.util.HashSet<>();
        Class<?> current = clazz;
        while (current != null && current != Object.class) {
            for (Field f : current.getDeclaredFields()) {
                names.add(f.getName());
            }
            current = current.getSuperclass();
        }
        return names;
    }

    // ==================== 回收站功能 ====================

    @Override
    public PageResult<V> trash(String keyword, Integer pageNum, Integer pageSize) {
        String tableName = getTableName();
        String orderColumn = getOrderColumn();

        StringBuilder whereClause = new StringBuilder("WHERE deleted = 1");
        List<Object> params = new java.util.ArrayList<>();

        if (keyword != null && !keyword.isEmpty()) {
            buildTrashKeywordCondition(whereClause, params, keyword);
        }

        // 查询总数（参数化）
        String countSql = "SELECT COUNT(*) FROM " + tableName + " " + whereClause;
        Long total = jdbcTemplate.queryForObject(countSql, Long.class, params.toArray());

        // 查询分页数据（参数化）
        Long offset = (long) (pageNum - 1) * pageSize;
        String dataSql = "SELECT * FROM " + tableName + " " + whereClause;
        if (orderColumn != null) {
            dataSql += " ORDER BY " + orderColumn + " DESC";
        }
        dataSql += " LIMIT ? OFFSET ?";
        params.add(pageSize);
        params.add(offset);

        List<E> entities = jdbcTemplate.queryForList(dataSql, entityClass, params.toArray());
        List<V> voList = entities.stream().map(this::toVo).collect(Collectors.toList());

        return new PageResult<>(voList, total, pageNum, pageSize);
    }

    /** 回收站关键词搜索条件，子类覆写以指定搜索字段（默认不搜索） */
    protected void buildTrashKeywordCondition(StringBuilder whereClause, List<Object> params, String keyword) {
        // 默认不搜索，子类应覆写
    }

    @Override
    public void restore(Long id) {
        // 恢复已删除记录：将 deleted 设置为 0
        String tableName = getTableName();
        String sql = "UPDATE " + tableName + " SET deleted = 0 WHERE id = ? AND deleted = 1";
        int rows = jdbcTemplate.update(sql, id);
        if (rows == 0) {
            throw new CustomException(ResultCodeEnum.NOT_FOUND);
        }
    }

    @Override
    public void permanentDelete(Long id) {
        // 彻底删除记录：物理删除
        String tableName = getTableName();
        String sql = "DELETE FROM " + tableName + " WHERE id = ? AND deleted = 1";
        int rows = jdbcTemplate.update(sql, id);
        if (rows == 0) {
            throw new CustomException(ResultCodeEnum.NOT_FOUND);
        }
    }

    @Override
    public void batchRestore(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            throw new CustomException(ResultCodeEnum.SYSTEM_ERROR);
        }
        String tableName = getTableName();
        String placeholders = ids.stream().map(id -> "?").collect(Collectors.joining(","));
        String sql = "UPDATE " + tableName + " SET deleted = 0 WHERE id IN (" + placeholders + ") AND deleted = 1";
        int rows = jdbcTemplate.update(sql, ids.toArray());
        if (rows == 0) {
            throw new CustomException(ResultCodeEnum.SYSTEM_ERROR);
        }
    }

    @Override
    public void batchPermanentDelete(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            throw new CustomException(ResultCodeEnum.SYSTEM_ERROR);
        }
        String tableName = getTableName();
        String placeholders = ids.stream().map(id -> "?").collect(Collectors.joining(","));
        String sql = "DELETE FROM " + tableName + " WHERE id IN (" + placeholders + ") AND deleted = 1";
        int rows = jdbcTemplate.update(sql, ids.toArray());
        if (rows == 0) {
            throw new CustomException(ResultCodeEnum.SYSTEM_ERROR);
        }
    }

    /** 获取表名（从实体类的 @TableName 注解或类名推断） */
    private String getTableName() {
        // 尝试从 @TableName 注解获取
        try {
            com.baomidou.mybatisplus.annotation.TableName tableNameAnnotation =
                    entityClass.getAnnotation(com.baomidou.mybatisplus.annotation.TableName.class);
            if (tableNameAnnotation != null && !tableNameAnnotation.value().isEmpty()) {
                return tableNameAnnotation.value();
            }
        } catch (Exception ignored) {}

        // 默认使用类名转下划线
        String className = entityClass.getSimpleName();
        return className.replaceAll("([a-z])([A-Z])", "$1_$2").toLowerCase();
    }
}
