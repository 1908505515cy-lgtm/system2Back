package com.example.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.example.common.GenericServiceImpl;
import com.example.entity.WfInstance;
import com.example.mapper.WfInstanceMapper;
import com.example.service.WfInstanceService;
import com.example.service.WorkflowEngine;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class WfInstanceServiceImpl extends GenericServiceImpl<WfInstance, WfInstance, WfInstance>
        implements WfInstanceService {

    private final WfInstanceMapper instanceMapper;
    private final WorkflowEngine workflowEngine;

    public WfInstanceServiceImpl(WfInstanceMapper mapper, JdbcTemplate jdbcTemplate,
                                  WorkflowEngine workflowEngine) {
        super(mapper, jdbcTemplate);
        this.instanceMapper = mapper;
        this.workflowEngine = workflowEngine;
    }

    @Override
    protected void buildKeywordCondition(QueryWrapper<WfInstance> wrapper, String keyword) {
        wrapper.and(w -> w
                .like("title", keyword)
                .or().like("initiator", keyword)
        );
    }

    @Override
    protected void buildTrashKeywordCondition(StringBuilder whereClause, java.util.List<Object> params, String keyword) {
        whereClause.append(" AND (title LIKE ? OR initiator LIKE ?)");
        String pattern = "%" + keyword + "%";
        params.add(pattern);
        params.add(pattern);
    }

    @Override
    public WfInstance startInstance(Long definitionId, String initiator, String title,
                                    String businessType, Long businessId) {
        return workflowEngine.startInstance(definitionId, initiator, title, businessType, businessId);
    }

    @Override
    public void cancelInstance(Long instanceId, String initiator) {
        workflowEngine.cancelInstance(instanceId, initiator);
    }

    @Override
    public List<WfInstance> getMyInstances(String username) {
        return instanceMapper.selectByInitiator(username);
    }
}
