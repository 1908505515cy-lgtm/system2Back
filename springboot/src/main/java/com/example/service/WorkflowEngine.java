package com.example.service;

import com.example.entity.WfInstance;

/**
 * 工作流引擎接口 - 轻量级状态机
 */
public interface WorkflowEngine {

    /** 发起流程实例 */
    WfInstance startInstance(Long definitionId, String initiator, String title,
                             String businessType, Long businessId);

    /** 完成审批任务 */
    void completeTask(Long taskId, String action, String comment, String operator);

    /** 转办任务 */
    void transferTask(Long taskId, String assignee, String transferTo, String comment);

    /** 撤回流程 */
    void cancelInstance(Long instanceId, String initiator);
}
