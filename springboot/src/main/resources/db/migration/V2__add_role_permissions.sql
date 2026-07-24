-- V2: 角色表增加按钮权限和数据范围字段

ALTER TABLE sys_role ADD COLUMN button_perms TEXT COMMENT '按钮权限，逗号分隔';
ALTER TABLE sys_role ADD COLUMN data_scope INT DEFAULT 1 COMMENT '数据范围：1=全部 2=本部门 3=仅本人';
