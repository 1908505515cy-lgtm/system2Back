package com.example.controller;

import com.example.common.GenericController;
import com.example.entity.AuditLog;
import com.example.service.AuditLogService;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/auditLog")
public class AuditLogController extends GenericController<AuditLog, AuditLog, AuditLog> {

    private final AuditLogService auditLogService;

    public AuditLogController(AuditLogService auditLogService) {
        this.auditLogService = auditLogService;
    }

    @Override
    protected AuditLogService getService() {
        return auditLogService;
    }
}
