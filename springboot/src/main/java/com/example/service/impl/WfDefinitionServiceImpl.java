package com.example.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.example.common.GenericServiceImpl;
import com.example.entity.WfDefinition;
import com.example.mapper.WfDefinitionMapper;
import com.example.service.WfDefinitionService;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

@Service
public class WfDefinitionServiceImpl extends GenericServiceImpl<WfDefinition, WfDefinition, WfDefinition>
        implements WfDefinitionService {

    public WfDefinitionServiceImpl(WfDefinitionMapper mapper, JdbcTemplate jdbcTemplate) {
        super(mapper, jdbcTemplate);
    }

    @Override
    protected void buildKeywordCondition(QueryWrapper<WfDefinition> wrapper, String keyword) {
        wrapper.and(w -> w
                .like("name", keyword)
                .or().like("code", keyword)
                .or().like("description", keyword)
        );
    }

    @Override
    protected void buildTrashKeywordCondition(StringBuilder whereClause, java.util.List<Object> params, String keyword) {
        whereClause.append(" AND (name LIKE ? OR code LIKE ? OR description LIKE ?)");
        String pattern = "%" + keyword + "%";
        params.add(pattern);
        params.add(pattern);
        params.add(pattern);
    }
}
