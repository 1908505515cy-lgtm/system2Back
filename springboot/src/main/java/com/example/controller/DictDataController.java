package com.example.controller;

import com.example.common.GenericController;
import com.example.common.OptionItem;
import com.example.common.Result;
import com.example.entity.DictData;
import com.example.service.DictDataService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/dictData")
public class DictDataController extends GenericController<DictData, DictData, DictData> {

    private final DictDataService dictDataService;

    public DictDataController(DictDataService dictDataService) {
        this.dictDataService = dictDataService;
    }

    @Override
    protected DictDataService getService() {
        return dictDataService;
    }

    /** 根据字典类型编码获取选项列表（供前端下拉使用） */
    @GetMapping("/options/{dictTypeCode}")
    public Result getOptions(@PathVariable String dictTypeCode) {
        List<OptionItem> options = dictDataService.getOptionsByTypeCode(dictTypeCode);
        return Result.success(options);
    }
}
