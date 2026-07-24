package com.example.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.example.common.GenericServiceImpl;
import com.example.entity.WfTask;
import com.example.mapper.WfTaskMapper;
import com.example.service.WfTaskService;
import com.example.service.WorkflowEngine;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class WfTaskServiceImpl extends GenericServiceImpl<WfTask, WfTask, WfTask>
        implements WfTaskService {

    private final WfTaskMapper taskMapper;
    private final WorkflowEngine workflowEngine;

    public WfTaskServiceImpl(WfTaskMapper mapper, JdbcTemplate jdbcTemplate,
                              WorkflowEngine workflowEngine) {
        super(mapper, jdbcTemplate);
        this.taskMapper = mapper;
        this.workflowEngine = workflowEngine;
    }

    @Override
    protected void buildKeywordCondition(QueryWrapper<WfTask> wrapper, String keyword) {
        wrapper.and(w -> w
                .like("node_name", keyword)
                .or().like("assignee", keyword)
                .or().like("comment", keyword)
        );
    }

    @Override
    protected void buildTrashKeywordCondition(StringBuilder whereClause, java.util.List<Object> params, String keyword) {
        whereClause.append(" AND (node_name LIKE ? OR assignee LIKE ? OR comment LIKE ?)");
        String pattern = "%" + keyword + "%";
        params.add(pattern);
        params.add(pattern);
        params.add(pattern);
    }

    @Override
    public void approve(Long taskId, String assignee, String comment) {
        workflowEngine.completeTask(taskId, "approved", comment, assignee);
    }

    @Override
    public void reject(Long taskId, String assignee, String comment) {
        workflowEngine.completeTask(taskId, "rejected", comment, assignee);
    }

    @Override
    public void transfer(Long taskId, String assignee, String transferTo, String comment) {
        workflowEngine.transferTask(taskId, assignee, transferTo, comment);
    }

    @Override
    public List<WfTask> getMyPending(String username) {
        return taskMapper.selectPendingByAssignee(username);
    }

    @Override
    public List<WfTask> getMyDone(String username) {
        return taskMapper.selectDoneByAssignee(username);
    }

    @Override
    public int getPendingCount(String username) {
        return taskMapper.countPending(username);
    }
}
