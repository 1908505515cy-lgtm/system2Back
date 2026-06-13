package com.example.common;

import com.example.exception.CustomException;
import com.example.common.enums.ResultCodeEnum;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 泛型 Controller 基类
 * 提供标准 CRUD 端点，子类只需声明 @RequestMapping 并实现 getService()
 *
 * @param <E> 实体类型
 * @param <D> DTO 类型
 * @param <V> VO 类型
 */
public abstract class GenericController<E, D, V> {

    private static final int MAX_PAGE_SIZE = 100;
    private static final int MAX_BATCH_SIZE = 100;

    protected abstract GenericService<E, D, V> getService();

    @PostMapping("/add")
    public Result add(@RequestBody D dto) {
        getService().add(dto);
        return Result.success("新增成功");
    }

    @PutMapping("/update")
    public Result update(@RequestBody D dto) {
        getService().update(dto);
        return Result.success("更新成功");
    }

    @DeleteMapping("/delete/{id}")
    public Result delete(@PathVariable Long id) {
        getService().delete(id);
        return Result.success("删除成功");
    }

    @GetMapping("/get/{id}")
    public Result getById(@PathVariable Long id) {
        V vo = getService().getById(id);
        return Result.success(vo);
    }

    @GetMapping("/page")
    public Result page(@RequestParam(required = false) String keyword,
                       @RequestParam(required = false) Integer status,
                       @RequestParam(defaultValue = "1") Integer pageNum,
                       @RequestParam(defaultValue = "10") Integer pageSize) {
        if (pageNum < 1) pageNum = 1;
        if (pageSize < 1) pageSize = 10;
        if (pageSize > MAX_PAGE_SIZE) pageSize = MAX_PAGE_SIZE;
        PageResult<V> result = getService().page(keyword, status, pageNum, pageSize);
        return Result.success(result);
    }

    @PutMapping("/status")
    public Result updateStatus(@RequestParam Long id, @RequestParam Integer status) {
        getService().updateStatus(id, status);
        return Result.success("状态修改成功");
    }

    @PutMapping("/field")
    public Result updateField(@RequestParam Long id, @RequestParam String fieldName, @RequestBody Map<String, Object> body) {
        Object value = body.get("value");
        getService().updateField(id, fieldName, value);
        return Result.success("字段修改成功");
    }

    @PostMapping("/batch-delete")
    public Result batchDelete(@RequestBody List<Long> ids) {
        if (ids.size() > MAX_BATCH_SIZE) {
            throw new CustomException(ResultCodeEnum.PARAM_ERROR.getCode(),
                    "批量操作最多支持 " + MAX_BATCH_SIZE + " 条");
        }
        getService().batchDelete(ids);
        return Result.success("批量删除成功");
    }
}
