-- V3: 知识库文档表

CREATE TABLE sys_document (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    title VARCHAR(200) NOT NULL,
    content TEXT NOT NULL,
    doc_type VARCHAR(50) DEFAULT 'other' COMMENT '文档类型: faq/manual/policy/other',
    status INT DEFAULT 1 COMMENT '状态: 1=启用 0=禁用',
    vector_status INT DEFAULT 0 COMMENT '向量化状态: 0=未处理 1=已向量化 2=失败',
    remark VARCHAR(500),
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted INT DEFAULT 0
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE INDEX idx_doc_type ON sys_document(doc_type);
CREATE INDEX idx_doc_status ON sys_document(status);
CREATE INDEX idx_doc_deleted ON sys_document(deleted);
