package com.example.service;

import com.example.common.GenericService;
import com.example.entity.WfInstanceNode;

import java.util.List;

public interface WfInstanceNodeService extends GenericService<WfInstanceNode, WfInstanceNode, WfInstanceNode> {

    /** 获取实例的所有节点状态 */
    List<WfInstanceNode> getByInstanceId(Long instanceId);
}
