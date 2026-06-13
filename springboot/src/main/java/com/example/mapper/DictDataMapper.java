package com.example.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.entity.DictData;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface DictDataMapper extends BaseMapper<DictData> {

    /** 根据字典类型编码查询字典数据列表 */
    List<DictData> selectByTypeCode(String dictTypeCode);
}
