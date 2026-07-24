package com.example.common;

import java.util.List;
import java.util.Map;

/**
 * 泛型 Service 接口
 * 所有业务模块的标准 CRUD 操作
 *
 * @param <E> 实体类型
 * @param <D> DTO 类型
 * @param <V> VO 类型
 */
public interface GenericService<E, D, V> {

    void add(D dto);

    void update(D dto);

    void delete(Long id);

    V getById(Long id);

    PageResult<V> page(String keyword, Integer status, Integer pageNum, Integer pageSize);

    void updateStatus(Long id, Integer status);

    void updateField(Long id, String fieldName, Object value);

    void batchDelete(List<Long> ids);

    /** 从字段 Map 创建实体（供 AI 调用） */
    V createFromMap(Map<String, Object> fields);

    /** 按字段精确查询（供 AI 调用） */
    List<V> queryByFields(Map<String, Object> fields);

    // ==================== 回收站功能 ====================

    /** 查询回收站（已删除记录） */
    PageResult<V> trash(String keyword, Integer pageNum, Integer pageSize);

    /** 恢复已删除记录 */
    void restore(Long id);

    /** 彻底删除记录 */
    void permanentDelete(Long id);

    /** 批量恢复 */
    void batchRestore(List<Long> ids);

    /** 批量彻底删除 */
    void batchPermanentDelete(List<Long> ids);
}
