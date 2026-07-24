package com.example.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.example.common.GenericServiceImpl;
import com.example.entity.Dashboard;
import com.example.mapper.DashboardMapper;
import com.example.service.DashboardService;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

@Service
public class DashboardServiceImpl extends GenericServiceImpl<Dashboard, Dashboard, Dashboard>
        implements DashboardService {

    private final DashboardMapper dashboardMapper;

    public DashboardServiceImpl(DashboardMapper mapper, JdbcTemplate jdbcTemplate) {
        super(mapper, jdbcTemplate);
        this.dashboardMapper = mapper;
    }

    @Override
    protected void buildKeywordCondition(QueryWrapper<Dashboard> wrapper, String keyword) {
        wrapper.and(w -> w
                .like("name", keyword)
                .or().like("description", keyword)
        );
    }

    @Override
    protected void buildTrashKeywordCondition(StringBuilder whereClause, java.util.List<Object> params, String keyword) {
        whereClause.append(" AND (name LIKE ? OR description LIKE ?)");
        String pattern = "%" + keyword + "%";
        params.add(pattern);
        params.add(pattern);
    }

    @Override
    public Dashboard getDefault() {
        return dashboardMapper.selectDefault();
    }

    @Override
    public void setDefault(Long id) {
        // 清除所有默认
        dashboardMapper.update(null,
                new UpdateWrapper<Dashboard>().set("is_default", 0).eq("is_default", 1));
        // 设置新的默认
        Dashboard d = new Dashboard();
        d.setId(id);
        d.setIsDefault(1);
        dashboardMapper.updateById(d);
    }
}
