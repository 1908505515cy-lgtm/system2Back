package com.example.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.example.entity.*;
import com.example.exception.CustomException;
import com.example.common.enums.ResultCodeEnum;
import com.example.mapper.*;
import com.example.service.WorkflowEngine;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service
public class WorkflowEngineImpl implements WorkflowEngine {

    private static final Logger log = LoggerFactory.getLogger(WorkflowEngineImpl.class);
    private final ObjectMapper objectMapper = new ObjectMapper();

    private final WfDefinitionMapper definitionMapper;
    private final WfInstanceMapper instanceMapper;
    private final WfInstanceNodeMapper instanceNodeMapper;
    private final WfTaskMapper taskMapper;
    private final NotificationMapper notificationMapper;

    public WorkflowEngineImpl(WfDefinitionMapper definitionMapper,
                               WfInstanceMapper instanceMapper,
                               WfInstanceNodeMapper instanceNodeMapper,
                               WfTaskMapper taskMapper,
                               NotificationMapper notificationMapper) {
        this.definitionMapper = definitionMapper;
        this.instanceMapper = instanceMapper;
        this.instanceNodeMapper = instanceNodeMapper;
        this.taskMapper = taskMapper;
        this.notificationMapper = notificationMapper;
    }

    @Override
    @Transactional
    public WfInstance startInstance(Long definitionId, String initiator, String title,
                                    String businessType, Long businessId) {
        WfDefinition definition = definitionMapper.selectById(definitionId);
        if (definition == null || definition.getDeleted() == 1) {
            throw new CustomException(ResultCodeEnum.NOT_FOUND);
        }
        if (definition.getStatus() != 1) {
            throw new CustomException("500", "流程定义未启用");
        }

        // 解析节点和连线
        List<Map<String, Object>> nodes = parseJson(definition.getNodesJson());
        List<Map<String, Object>> edges = parseJson(definition.getEdgesJson());

        // 创建实例
        WfInstance instance = new WfInstance();
        instance.setDefinitionId(definitionId);
        instance.setTitle(title);
        instance.setInitiator(initiator);
        instance.setStatus("running");
        instance.setBusinessType(businessType);
        instance.setBusinessId(businessId);
        instanceMapper.insert(instance);

        // 为每个节点创建实例节点记录
        for (Map<String, Object> node : nodes) {
            WfInstanceNode instanceNode = new WfInstanceNode();
            instanceNode.setInstanceId(instance.getId());
            instanceNode.setNodeId((String) node.get("id"));
            instanceNode.setNodeName((String) node.get("label"));
            instanceNode.setNodeType((String) node.get("type"));
            instanceNode.setStatus("pending");
            instanceNodeMapper.insert(instanceNode);
        }

        // 找到 start 节点，推进到第一个审批节点
        String startNodeId = nodes.stream()
                .filter(n -> "start".equals(n.get("type")))
                .map(n -> (String) n.get("id"))
                .findFirst()
                .orElse(null);

        if (startNodeId != null) {
            // 标记 start 节点完成
            updateNodeStatus(instance.getId(), startNodeId, "completed", null);
            // 推进到下一个节点
            advanceToNextNode(instance, startNodeId, nodes, edges, initiator);
        }

        log.info("流程实例已启动: instanceId={}, definition={}", instance.getId(), definition.getName());
        return instance;
    }

    @Override
    @Transactional
    public void completeTask(Long taskId, String action, String comment, String operator) {
        WfTask task = taskMapper.selectById(taskId);
        if (task == null || task.getDeleted() == 1) {
            throw new CustomException(ResultCodeEnum.NOT_FOUND);
        }
        if (!"pending".equals(task.getAction())) {
            throw new CustomException("500", "该任务已处理");
        }
        if (!task.getAssignee().equals(operator)) {
            throw new CustomException("500", "您不是该任务的审批人");
        }

        // 更新任务状态
        task.setAction(action);
        task.setComment(comment);
        taskMapper.updateById(task);

        WfInstance instance = instanceMapper.selectById(task.getInstanceId());
        if (instance == null) return;

        if ("rejected".equals(action)) {
            // 驳回：流程结束
            instance.setStatus("rejected");
            instanceMapper.updateById(instance);
            updateNodeStatus(instance.getId(), task.getNodeId(), "completed", operator);
            // 通知发起人
            createNotification(instance.getInitiator(), instance, "流程已驳回",
                    "您发起的流程 [" + instance.getTitle() + "] 已被 " + operator + " 驳回");
            log.info("流程实例已驳回: instanceId={}", instance.getId());
            return;
        }

        // 通过：标记当前节点完成，推进到下一个
        updateNodeStatus(instance.getId(), task.getNodeId(), "completed", operator);

        // 重新加载定义
        WfDefinition definition = definitionMapper.selectById(instance.getDefinitionId());
        List<Map<String, Object>> nodes = parseJson(definition.getNodesJson());
        List<Map<String, Object>> edges = parseJson(definition.getEdgesJson());

        advanceToNextNode(instance, task.getNodeId(), nodes, edges, instance.getInitiator());
    }

    @Override
    @Transactional
    public void transferTask(Long taskId, String assignee, String transferTo, String comment) {
        WfTask task = taskMapper.selectById(taskId);
        if (task == null || task.getDeleted() == 1) {
            throw new CustomException(ResultCodeEnum.NOT_FOUND);
        }
        if (!"pending".equals(task.getAction())) {
            throw new CustomException("500", "该任务已处理");
        }
        if (!task.getAssignee().equals(assignee)) {
            throw new CustomException("500", "您不是该任务的审批人");
        }

        // 原任务标记为转办
        task.setAction("transferred");
        task.setTransferTo(transferTo);
        task.setComment(comment);
        taskMapper.updateById(task);

        // 创建新任务给转办目标
        WfTask newTask = new WfTask();
        newTask.setInstanceId(task.getInstanceId());
        newTask.setNodeId(task.getNodeId());
        newTask.setNodeName(task.getNodeName());
        newTask.setAssignee(transferTo);
        newTask.setAction("pending");
        taskMapper.insert(newTask);

        // 通知转办目标
        WfInstance instance = instanceMapper.selectById(task.getInstanceId());
        if (instance != null) {
            createNotification(transferTo, instance, "转办任务",
                    assignee + " 将流程 [" + instance.getTitle() + "] 的审批任务转交给您");
        }
        log.info("任务已转办: taskId={}, from={}, to={}", taskId, assignee, transferTo);
    }

    @Override
    @Transactional
    public void cancelInstance(Long instanceId, String initiator) {
        WfInstance instance = instanceMapper.selectById(instanceId);
        if (instance == null || instance.getDeleted() == 1) {
            throw new CustomException(ResultCodeEnum.NOT_FOUND);
        }
        if (!instance.getInitiator().equals(initiator)) {
            throw new CustomException("500", "只有发起人可以撤回流程");
        }
        if (!"running".equals(instance.getStatus())) {
            throw new CustomException("500", "只能撤回进行中的流程");
        }

        instance.setStatus("cancelled");
        instanceMapper.updateById(instance);

        // 撤销所有待办任务
        List<WfTask> pendingTasks = taskMapper.selectList(
                new QueryWrapper<WfTask>()
                        .eq("instance_id", instanceId)
                        .eq("action", "pending"));
        for (WfTask task : pendingTasks) {
            task.setAction("cancelled");
            taskMapper.updateById(task);
        }

        log.info("流程实例已撤回: instanceId={}", instanceId);
    }

    /**
     * 推进到下一个节点
     */
    private void advanceToNextNode(WfInstance instance, String currentNodeId,
                                    List<Map<String, Object>> nodes,
                                    List<Map<String, Object>> edges,
                                    String initiator) {
        // 找到当前节点的出边
        String nextNodeId = edges.stream()
                .filter(e -> currentNodeId.equals(e.get("source")))
                .map(e -> (String) e.get("target"))
                .findFirst()
                .orElse(null);

        if (nextNodeId == null) {
            // 没有下一个节点，流程结束
            instance.setStatus("completed");
            instance.setCurrentNodeId(null);
            instanceMapper.updateById(instance);
            createNotification(instance.getInitiator(), instance, "流程已完成",
                    "您发起的流程 [" + instance.getTitle() + "] 已审批通过");
            return;
        }

        // 找到下一个节点定义
        Map<String, Object> nextNode = nodes.stream()
                .filter(n -> nextNodeId.equals(n.get("id")))
                .findFirst()
                .orElse(null);

        if (nextNode == null) return;

        String nodeType = (String) nextNode.get("type");
        instance.setCurrentNodeId(nextNodeId);
        instanceMapper.updateById(instance);
        updateNodeStatus(instance.getId(), nextNodeId, "active", null);

        switch (nodeType) {
            case "end":
                // 流程结束
                instance.setStatus("completed");
                instance.setCurrentNodeId(nextNodeId);
                instanceMapper.updateById(instance);
                updateNodeStatus(instance.getId(), nextNodeId, "completed", null);
                createNotification(instance.getInitiator(), instance, "流程已完成",
                        "您发起的流程 [" + instance.getTitle() + "] 已审批通过");
                break;

            case "approval":
                // 创建审批任务
                String assignee = resolveAssignee(nextNode, initiator);
                WfTask task = new WfTask();
                task.setInstanceId(instance.getId());
                task.setNodeId(nextNodeId);
                task.setNodeName((String) nextNode.get("label"));
                task.setAssignee(assignee);
                task.setAction("pending");
                taskMapper.insert(task);
                // 发通知
                createNotification(assignee, instance, "待审批",
                        "您有一个待审批的流程 [" + instance.getTitle() + "]");
                break;

            case "notify":
                // 通知节点：自动发送通知，继续推进
                String notifyTarget = resolveNotifyTarget(nextNode, initiator);
                createNotification(notifyTarget, instance, "流程通知",
                        "流程 [" + instance.getTitle() + "] " + nextNode.get("label"));
                updateNodeStatus(instance.getId(), nextNodeId, "completed", null);
                // 继续推进
                advanceToNextNode(instance, nextNodeId, nodes, edges, initiator);
                break;

            case "gateway":
                // 网关节点：简化处理，直接走第一个出边（无条件判断）
                updateNodeStatus(instance.getId(), nextNodeId, "completed", null);
                advanceToNextNode(instance, nextNodeId, nodes, edges, initiator);
                break;

            default:
                log.warn("未知节点类型: {}", nodeType);
                break;
        }
    }

    /**
     * 解析审批人
     */
    private String resolveAssignee(Map<String, Object> node, String initiator) {
        String assigneeType = (String) node.getOrDefault("assigneeType", "user");
        String assignee = (String) node.getOrDefault("assignee", "");

        if ("user".equals(assigneeType)) {
            return assignee;
        } else if ("role".equals(assigneeType)) {
            // 根据角色查找用户（简化：返回 assignee 字段值作为角色名）
            // 实际可查询 sys_admin 表根据 roleIds 查找
            return assignee;
        } else if ("initiator".equals(assigneeType)) {
            return initiator;
        }
        return assignee;
    }

    /**
     * 解析通知目标
     */
    private String resolveNotifyTarget(Map<String, Object> node, String initiator) {
        String notifyTarget = (String) node.getOrDefault("notifyTarget", "");
        if ("initiator".equals(notifyTarget)) {
            return initiator;
        }
        return notifyTarget;
    }

    /**
     * 更新节点状态
     */
    private void updateNodeStatus(Long instanceId, String nodeId, String status, String assignee) {
        WfInstanceNode node = instanceNodeMapper.selectByInstanceAndNode(instanceId, nodeId);
        if (node != null) {
            node.setStatus(status);
            if (assignee != null) {
                node.setAssignee(assignee);
            }
            instanceNodeMapper.updateById(node);
        }
    }

    /**
     * 创建通知
     */
    private void createNotification(String targetUser, WfInstance instance,
                                     String title, String content) {
        Notification notification = new Notification();
        notification.setTitle(title);
        notification.setContent(content);
        notification.setType("workflow");
        notification.setTargetUser(targetUser);
        notification.setIsRead(0);
        notificationMapper.insert(notification);
    }

    /**
     * 解析 JSON
     */
    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> parseJson(String json) {
        try {
            return objectMapper.readValue(json, new TypeReference<List<Map<String, Object>>>() {});
        } catch (JsonProcessingException e) {
            log.error("JSON 解析失败: {}", e.getMessage());
            return Collections.emptyList();
        }
    }
}
