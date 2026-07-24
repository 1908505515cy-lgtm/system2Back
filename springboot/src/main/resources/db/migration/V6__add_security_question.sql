-- 安全问题字段（用于忘记密码验证）
-- MySQL 8.0 不直接支持 ADD COLUMN IF NOT EXISTS，通过设置 baseline-version 跳过已执行的迁移

ALTER TABLE sys_admin
  ADD COLUMN security_question VARCHAR(200) DEFAULT NULL COMMENT '安全问题',
  ADD COLUMN security_answer VARCHAR(200) DEFAULT NULL COMMENT '安全答案（BCrypt加密）';
