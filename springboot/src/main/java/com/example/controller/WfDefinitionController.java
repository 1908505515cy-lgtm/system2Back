package com.example.controller;

import com.example.common.GenericController;
import com.example.common.Result;
import com.example.entity.WfDefinition;
import com.example.service.WfDefinitionService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/wfDefinition")
public class WfDefinitionController extends GenericController<WfDefinition, WfDefinition, WfDefinition> {

    private final WfDefinitionService wfDefinitionService;

    public WfDefinitionController(WfDefinitionService wfDefinitionService) {
        this.wfDefinitionService = wfDefinitionService;
    }

    @Override
    protected WfDefinitionService getService() {
        return wfDefinitionService;
    }
}
