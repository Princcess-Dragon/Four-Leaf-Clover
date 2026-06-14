-- 优化对话历史表查询性能
-- 执行日期: 2026-06-01
-- 说明: 为 conversation 表添加组合索引以优化历史查询

-- 添加用户ID和会话ID的组合索引（用于快速查询特定会话的历史）
ALTER TABLE conversation 
ADD INDEX idx_user_session (user_id, session_id);

-- 添加用户ID和创建时间的组合索引（用于快速查询用户最近的对话）
ALTER TABLE conversation 
ADD INDEX idx_user_created (user_id, created_at DESC);

-- 验证索引是否创建成功
SHOW INDEX FROM conversation;

-- 测试查询性能
EXPLAIN SELECT * FROM conversation 
WHERE user_id = 'test_user' AND session_id = 'test_session' 
ORDER BY created_at ASC LIMIT 100;
