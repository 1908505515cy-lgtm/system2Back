package com.example.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.entity.AuditLog;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Mapper
public interface AuditLogMapper extends BaseMapper<AuditLog> {

    @Select("SELECT action, COUNT(*) as count FROM sys_audit_log GROUP BY action")
    List<Map<String, Object>> selectActionDistribution();

    @Select("SELECT DATE(operate_time) as date, COUNT(*) as count FROM sys_audit_log " +
            "WHERE operate_time >= #{since} GROUP BY DATE(operate_time) ORDER BY date")
    List<Map<String, Object>> selectDailyTrend(LocalDateTime since);

    @Delete("DELETE FROM sys_audit_log WHERE operate_time < #{beforeTime}")
    int deleteAuditLogsBefore(LocalDateTime beforeTime);
}
