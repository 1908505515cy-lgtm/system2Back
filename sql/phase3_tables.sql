-- Phase 1: 部门表
CREATE TABLE IF NOT EXISTS sys_department (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    dept_code VARCHAR(50) NOT NULL,
    leader VARCHAR(50),
    phone VARCHAR(20),
    status INT DEFAULT 1,
    remark VARCHAR(500),
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted INT DEFAULT 0
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Phase 3: 数据字典
CREATE TABLE IF NOT EXISTS sys_dict_type (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    code VARCHAR(100) NOT NULL UNIQUE,
    status INT DEFAULT 1,
    remark VARCHAR(500),
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted INT DEFAULT 0
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS sys_dict_data (
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

-- Phase 3: 审计日志
CREATE TABLE IF NOT EXISTS sys_audit_log (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    operator VARCHAR(50),
    action VARCHAR(20),
    module VARCHAR(50),
    record_id BIGINT,
    detail TEXT,
    operate_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    ip VARCHAR(50)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Phase 3: 角色
CREATE TABLE IF NOT EXISTS sys_role (
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

-- 初始化数据
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
