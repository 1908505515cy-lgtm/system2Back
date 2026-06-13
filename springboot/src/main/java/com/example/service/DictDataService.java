package com.example.service;

import com.example.common.GenericService;
import com.example.entity.DictData;
import com.example.common.OptionItem;

import java.util.List;

public interface DictDataService extends GenericService<DictData, DictData, DictData> {

    /** 根据字典类型编码获取选项列表 */
    List<OptionItem> getOptionsByTypeCode(String dictTypeCode);
}
