package com.example.service;

import com.example.common.GenericService;
import com.example.entity.WfInstance;

import java.util.List;

public interface WfInstanceService extends GenericService<WfInstance, WfInstance, WfInstance> {

    /** 发起流程实例 */
    WfInstance startInstance(Long definitionId, String initiator, String title,
                             String businessType, Long businessId);

    /** 撤回流程实例 */
    void cancelInstance(Long instanceId, String initiator);

    /** 获取我发起的流程 */
    List<WfInstance> getMyInstances(String username);
}
