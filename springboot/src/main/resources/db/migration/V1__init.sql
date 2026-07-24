-- V1: 初始化系统表结构

-- 管理员表
CREATE TABLE sys_admin (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    account_code VARCHAR(50),
    username VARCHAR(50) NOT NULL UNIQUE,
    password VARCHAR(200) NOT NULL,
    real_name VARCHAR(50),
    avatar TEXT,
    gender INT DEFAULT 0,
    mobile VARCHAR(20),
    email VARCHAR(100),
    status INT DEFAULT 1,
    last_login_time DATETIME,
    last_login_ip VARCHAR(50),
    login_count INT DEFAULT 0,
    role VARCHAR(50),
    admin_code VARCHAR(50) UNIQUE,
    dept_id BIGINT,
    role_ids VARCHAR(200),
    remark VARCHAR(500),
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted INT DEFAULT 0
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 部门表
CREATE TABLE sys_department (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    parent_id BIGINT DEFAULT NULL COMMENT '上级部门ID',
    name VARCHAR(100) NOT NULL,
    dept_code VARCHAR(50) NOT NULL,
    leader VARCHAR(50),
    phone VARCHAR(20),
    status INT DEFAULT 1,
    remark VARCHAR(500),
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted INT DEFAULT 0,
    INDEX idx_parent_id (parent_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 数据字典类型
CREATE TABLE sys_dict_type (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    code VARCHAR(100) NOT NULL UNIQUE,
    status INT DEFAULT 1,
    remark VARCHAR(500),
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted INT DEFAULT 0
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 数据字典数据
CREATE TABLE sys_dict_data (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    dict_type_code VARCHAR(100) NOT NULL,
    label VARCHAR(100) NOT NULL,
    value VARCHAR(100) NOT NULL,
    sort INT DEFAULT 0,
    status INT DEFAULT 1,
    remark VARCHAR(500),
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted INT DEFAULT 0
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 审计日志
CREATE TABLE sys_audit_log (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    operator VARCHAR(50),
    action VARCHAR(20),
    module VARCHAR(50),
    record_id BIGINT,
    detail TEXT,
    operate_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    ip VARCHAR(50)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 角色表
CREATE TABLE sys_role (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    code VARCHAR(50) NOT NULL UNIQUE,
    module_perms TEXT,
    status INT DEFAULT 1,
    remark VARCHAR(500),
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted INT DEFAULT 0
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- AI对话会话
CREATE TABLE chat_session (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    title VARCHAR(200) NOT NULL DEFAULT '新对话',
    user_id BIGINT,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted INT DEFAULT 0
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- AI对话消息
CREATE TABLE chat_message (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    session_id BIGINT NOT NULL,
    role VARCHAR(20) NOT NULL COMMENT 'user 或 ai',
    content TEXT NOT NULL,
    confirm_data TEXT COMMENT 'JSON格式的确认操作数据',
    confirm_status VARCHAR(20) DEFAULT NULL COMMENT 'pending/confirmed/cancelled',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    deleted INT DEFAULT 0
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 系统通知
CREATE TABLE sys_notification (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    title VARCHAR(200) NOT NULL,
    content TEXT,
    type VARCHAR(20) DEFAULT 'info' COMMENT 'info/warning/success',
    target_user VARCHAR(50) COMMENT '目标用户名，null表示全员',
    is_read INT DEFAULT 0,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    deleted INT DEFAULT 0
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 性能索引
CREATE INDEX idx_admin_username ON sys_admin(username);
CREATE INDEX idx_admin_status ON sys_admin(status);
CREATE INDEX idx_admin_deleted ON sys_admin(deleted);
CREATE INDEX idx_auditlog_operator ON sys_audit_log(operator);
CREATE INDEX idx_auditlog_action ON sys_audit_log(action);
CREATE INDEX idx_auditlog_module ON sys_audit_log(module);
CREATE INDEX idx_auditlog_time ON sys_audit_log(operate_time);
CREATE INDEX idx_dictdata_type_code ON sys_dict_data(dict_type_code);
CREATE INDEX idx_role_deleted ON sys_role(deleted);
CREATE INDEX idx_dept_deleted ON sys_department(deleted);
CREATE INDEX idx_chat_session_user ON chat_session(user_id);
CREATE INDEX idx_chat_msg_session ON chat_message(session_id);

-- 初始数据
INSERT IGNORE INTO sys_admin (account_code, username, password, real_name, status, role, admin_code, dept_id, role_ids) VALUES
('A001', 'admin', '$2b$12$vk.kL90UNrSqL7UU21yYeOLSRF84Z61Bp3cWbPdIJypHLGFfXCOjS', '超级管理员', 1, 'super_admin', 'ADMIN001', 1, '1');

INSERT IGNORE INTO sys_department (name, dept_code, leader, status) VALUES
('技术部', 'TECH', '张三', 1),
('产品部', 'PRODUCT', '李四', 1),
('运营部', 'OPS', '王五', 1);

INSERT IGNORE INTO sys_role (name, code, module_perms, status) VALUES
('超级管理员', 'super_admin', 'admin,department,dictType,dictData,role,auditLog', 1),
('普通管理员', 'admin', 'admin,department', 1);

INSERT IGNORE INTO sys_dict_type (name, code) VALUES
('性别', 'sys_gender'),
('状态', 'sys_status');

INSERT IGNORE INTO sys_dict_data (dict_type_code, label, value, sort) VALUES
('sys_gender', '未知', '0', 1),
('sys_gender', '男', '1', 2),
('sys_gender', '女', '2', 3),
('sys_status', '正常', '1', 1),
('sys_status', '禁用', '0', 2);
