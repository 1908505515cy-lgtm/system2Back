package com.example.service;

import com.example.common.GenericService;
import com.example.entity.Dashboard;

public interface DashboardService extends GenericService<Dashboard, Dashboard, Dashboard> {

    /** 获取默认仪表盘 */
    Dashboard getDefault();

    /** 设为默认仪表盘 */
    void setDefault(Long id);
}
