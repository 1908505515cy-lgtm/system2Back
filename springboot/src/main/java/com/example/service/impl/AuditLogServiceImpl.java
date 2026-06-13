package com.example.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.example.common.GenericServiceImpl;
import com.example.common.enums.ResultCodeEnum;
import com.example.entity.AuditLog;
import com.example.exception.CustomException;
import com.example.mapper.AuditLogMapper;
import com.example.service.AuditLogService;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class AuditLogServiceImpl extends GenericServiceImpl<AuditLog, AuditLog, AuditLog>
        implements AuditLogService {

    private final AuditLogMapper auditLogMapper;

    public AuditLogServiceImpl(AuditLogMapper mapper) {
        super(mapper);
        this.auditLogMapper = mapper;
    }

    @Override
    protected void buildKeywordCondition(QueryWrapper<AuditLog> wrapper, String keyword) {
        wrapper.and(w -> w
                .like("operator", keyword)
                .or().like("module", keyword)
                .or().like("action", keyword)
        );
    }

    @Override
    protected String getOrderColumn() {
        return "operate_time";
    }

    /** 审计日志不可删除 */
    @Override
    public void delete(Long id) {
        throw new CustomException(ResultCodeEnum.PARAM_ERROR.getCode(), "审计日志不可删除");
    }

    /** 审计日志不可批量删除 */
    @Override
    public void batchDelete(List<Long> ids) {
        throw new CustomException(ResultCodeEnum.PARAM_ERROR.getCode(), "审计日志不可删除");
    }

    /** 审计日志无 status 字段 */
    @Override
    public void updateStatus(Long id, Integer status) {
        throw new CustomException(ResultCodeEnum.PARAM_ERROR.getCode(), "审计日志不支持状态修改");
    }

    @Override
    public void log(String operator, String action, String module, Long recordId, String detail, String ip) {
        AuditLog log = new AuditLog();
        log.setOperator(operator);
        log.setAction(action);
        log.setModule(module);
        log.setRecordId(recordId);
        log.setDetail(detail);
        log.setIp(ip);
        auditLogMapper.insert(log);
    }
}
