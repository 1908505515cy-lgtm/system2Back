package com.example.common;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.exception.CustomException;
import com.example.common.enums.ResultCodeEnum;
import org.springframework.beans.BeanUtils;

import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

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

    @SuppressWarnings("unchecked")
    public GenericServiceImpl(BaseMapper<E> mapper) {
        this.mapper = mapper;
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

    @Override
    public void updateField(Long id, String fieldName, Object value) {
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
}
