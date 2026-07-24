package com.example.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.example.common.GenericServiceImpl;
import com.example.entity.WfInstanceNode;
import com.example.mapper.WfInstanceNodeMapper;
import com.example.service.WfInstanceNodeService;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class WfInstanceNodeServiceImpl extends GenericServiceImpl<WfInstanceNode, WfInstanceNode, WfInstanceNode>
        implements WfInstanceNodeService {

    private final WfInstanceNodeMapper instanceNodeMapper;

    public WfInstanceNodeServiceImpl(WfInstanceNodeMapper mapper, JdbcTemplate jdbcTemplate) {
        super(mapper, jdbcTemplate);
        this.instanceNodeMapper = mapper;
    }

    @Override
    protected void buildKeywordCondition(QueryWrapper<WfInstanceNode> wrapper, String keyword) {
        wrapper.and(w -> w
                .like("node_name", keyword)
                .or().like("assignee", keyword)
        );
    }

    @Override
    protected void buildTrashKeywordCondition(StringBuilder whereClause, java.util.List<Object> params, String keyword) {
        whereClause.append(" AND (node_name LIKE ? OR assignee LIKE ?)");
        String pattern = "%" + keyword + "%";
        params.add(pattern);
        params.add(pattern);
    }

    @Override
    public List<WfInstanceNode> getByInstanceId(Long instanceId) {
        return instanceNodeMapper.selectByInstanceId(instanceId);
    }
}
