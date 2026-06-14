-- 为 conversation 表添加 session_title 字段
-- 执行日期: 2026-06-01

-- 添加会话标题字段
ALTER TABLE conversation 
ADD COLUMN session_title VARCHAR(255) DEFAULT '新对话' COMMENT '会话标题' AFTER session_id;

-- 验证字段是否添加成功
-- DESCRIBE conversation;
