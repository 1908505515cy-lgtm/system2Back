package com.example.service;

import com.example.common.GenericService;
import com.example.entity.AuditLog;

public interface AuditLogService extends GenericService<AuditLog, AuditLog, AuditLog> {

    /** 记录操作日志 */
    void log(String operator, String action, String module, Long recordId, String detail, String ip);
}
