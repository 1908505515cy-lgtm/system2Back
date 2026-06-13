package com.example.controller;

import com.example.common.GenericController;
import com.example.entity.DictType;
import com.example.service.DictTypeService;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/dictType")
public class DictTypeController extends GenericController<DictType, DictType, DictType> {

    private final DictTypeService dictTypeService;

    public DictTypeController(DictTypeService dictTypeService) {
        this.dictTypeService = dictTypeService;
    }

    @Override
    protected DictTypeService getService() {
        return dictTypeService;
    }
}
