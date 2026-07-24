-- 工作流模块表

-- 1. 流程定义
CREATE TABLE wf_definition (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  name VARCHAR(100) NOT NULL COMMENT '流程名称',
  code VARCHAR(50) NOT NULL COMMENT '流程编码',
  description TEXT COMMENT '流程说明',
  category VARCHAR(50) DEFAULT 'default' COMMENT '分类',
  nodes_json TEXT NOT NULL COMMENT '节点定义JSON',
  edges_json TEXT NOT NULL COMMENT '连线定义JSON',
  status INT DEFAULT 1 COMMENT '1=启用 0=禁用',
  version INT DEFAULT 1 COMMENT '版本号',
  create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
  update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  deleted INT DEFAULT 0,
  UNIQUE KEY uk_code (code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 2. 流程实例
CREATE TABLE wf_instance (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  definition_id BIGINT NOT NULL COMMENT '流程定义ID',
  title VARCHAR(200) NOT NULL COMMENT '实例标题',
  initiator VARCHAR(50) NOT NULL COMMENT '发起人',
  status VARCHAR(20) DEFAULT 'running' COMMENT 'running/completed/rejected/cancelled',
  business_type VARCHAR(50) COMMENT '关联业务类型',
  business_id BIGINT COMMENT '关联业务ID',
  current_node_id VARCHAR(50) COMMENT '当前节点ID',
  create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
  update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  deleted INT DEFAULT 0
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 3. 审批任务
CREATE TABLE wf_task (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  instance_id BIGINT NOT NULL COMMENT '流程实例ID',
  node_id VARCHAR(50) NOT NULL COMMENT '节点ID',
  node_name VARCHAR(100) COMMENT '节点名称',
  assignee VARCHAR(50) NOT NULL COMMENT '审批人',
  action VARCHAR(20) DEFAULT 'pending' COMMENT 'pending/approved/rejected/transferred',
  comment TEXT COMMENT '审批意见',
  transfer_to VARCHAR(50) COMMENT '转办目标人',
  create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
  update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  deleted INT DEFAULT 0
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 4. 流程实例节点状态
CREATE TABLE wf_instance_node (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  instance_id BIGINT NOT NULL COMMENT '流程实例ID',
  node_id VARCHAR(50) NOT NULL COMMENT '节点ID',
  node_name VARCHAR(100) COMMENT '节点名称',
  node_type VARCHAR(20) COMMENT '节点类型: start/end/approval/notify/gateway',
  status VARCHAR(20) DEFAULT 'pending' COMMENT 'pending/active/completed/skipped',
  assignee VARCHAR(50) COMMENT '处理人',
  create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
  update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 5. 字典数据扩充
INSERT INTO sys_dict_type (name, code, status) VALUES ('流程分类', 'wf_category', 1);
INSERT INTO sys_dict_data (dict_type_code, label, value, sort, status) VALUES
('wf_category', '默认', 'default', 1, 1),
('wf_category', '人事', 'hr', 2, 1),
('wf_category', '财务', 'finance', 3, 1),
('wf_category', '行政', 'admin', 4, 1);
