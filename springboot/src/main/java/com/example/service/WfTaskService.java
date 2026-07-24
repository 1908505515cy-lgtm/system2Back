package com.example.service;

import com.example.common.GenericService;
import com.example.entity.WfTask;

import java.util.List;

public interface WfTaskService extends GenericService<WfTask, WfTask, WfTask> {

    /** 通过审批 */
    void approve(Long taskId, String assignee, String comment);

    /** 驳回审批 */
    void reject(Long taskId, String assignee, String comment);

    /** 转办 */
    void transfer(Long taskId, String assignee, String transferTo, String comment);

    /** 我的待办 */
    List<WfTask> getMyPending(String username);

    /** 我的已办 */
    List<WfTask> getMyDone(String username);

    /** 待办数量 */
    int getPendingCount(String username);
}
