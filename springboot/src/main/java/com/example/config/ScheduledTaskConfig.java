package com.example.config;

import com.example.mapper.AuditLogMapper;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

import java.time.LocalDateTime;

@Configuration
@EnableScheduling
public class ScheduledTaskConfig {

    private final AuditLogMapper auditLogMapper;

    public ScheduledTaskConfig(AuditLogMapper auditLogMapper) {
        this.auditLogMapper = auditLogMapper;
    }

    /**
     * 每天凌晨 3 点清理 30 天前的审计日志
     */
    @Scheduled(cron = "0 0 3 * * ?")
    public void cleanAuditLogs() {
        LocalDateTime thirtyDaysAgo = LocalDateTime.now().minusDays(30);
        int deleted = auditLogMapper.deleteAuditLogsBefore(thirtyDaysAgo);
        if (deleted > 0) {
            System.out.println("[定时任务] 已清理 " + deleted + " 条审计日志");
        }
    }
}
