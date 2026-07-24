package com.example.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.entity.Dashboard;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface DashboardMapper extends BaseMapper<Dashboard> {

    @Select("SELECT * FROM sys_dashboard WHERE deleted = 0 AND is_default = 1 AND status = 1 LIMIT 1")
    Dashboard selectDefault();
}
