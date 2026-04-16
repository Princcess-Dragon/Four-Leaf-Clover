-- ============================================
-- AI办公助理数据库初始化脚本
-- 数据库名称: agent
-- 支持: MySQL 5.7+
-- ============================================

-- 创建数据库
CREATE DATABASE IF NOT EXISTS `agent` DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

USE `agent`;

-- ============================================
-- 1. 用户表
-- ============================================
DROP TABLE IF EXISTS `user`;
CREATE TABLE `user` (
  `id` BIGINT(20) NOT NULL AUTO_INCREMENT COMMENT '用户ID',
  `user_id` VARCHAR(64) NOT NULL COMMENT '用户唯一标识',
  `username` VARCHAR(64) NOT NULL COMMENT '用户名',
  `password` VARCHAR(128) NOT NULL COMMENT '密码',
  `email` VARCHAR(128) DEFAULT NULL COMMENT '邮箱',
  `phone` VARCHAR(20) DEFAULT NULL COMMENT '手机号',
  `avatar` VARCHAR(255) DEFAULT NULL COMMENT '头像URL',
  `status` TINYINT(1) NOT NULL DEFAULT 1 COMMENT '状态: 0-禁用, 1-启用',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_id` (`user_id`),
  UNIQUE KEY `uk_username` (`username`),
  KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户表';

-- ============================================
-- 2. 任务表
-- ============================================
DROP TABLE IF EXISTS `task`;
CREATE TABLE `task` (
  `id` BIGINT(20) NOT NULL AUTO_INCREMENT COMMENT '任务ID',
  `user_id` VARCHAR(64) NOT NULL COMMENT '用户ID',
  `title` VARCHAR(255) NOT NULL COMMENT '任务标题',
  `description` TEXT COMMENT '任务描述',
  `status` TINYINT(1) NOT NULL DEFAULT 0 COMMENT '状态: 0-待完成, 1-已完成, 2-已取消',
  `priority` TINYINT(1) NOT NULL DEFAULT 0 COMMENT '优先级: 0-普通, 1-重要, 2-紧急',
  `due_date` DATETIME DEFAULT NULL COMMENT '截止时间',
  `completed_at` DATETIME DEFAULT NULL COMMENT '完成时间',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_status` (`status`),
  KEY `idx_priority` (`priority`),
  KEY `idx_due_date` (`due_date`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='任务表';

-- ============================================
-- 3. 文件记录表
-- ============================================
DROP TABLE IF EXISTS `file_record`;
CREATE TABLE `file_record` (
  `id` BIGINT(20) NOT NULL AUTO_INCREMENT COMMENT '记录ID',
  `user_id` VARCHAR(64) NOT NULL COMMENT '用户ID',
  `file_name` VARCHAR(255) NOT NULL COMMENT '文件名',
  `file_path` VARCHAR(512) NOT NULL COMMENT '文件路径',
  `file_size` BIGINT(20) DEFAULT 0 COMMENT '文件大小(字节)',
  `file_type` VARCHAR(64) DEFAULT NULL COMMENT '文件类型',
  `content` TEXT COMMENT '文件内容',
  `operation` VARCHAR(32) NOT NULL COMMENT '操作类型: create, read, update, delete',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_file_name` (`file_name`),
  KEY `idx_created_at` (`created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='文件记录表';

-- ============================================
-- 4. 对话历史表
-- ============================================
DROP TABLE IF EXISTS `conversation`;
CREATE TABLE `conversation` (
  `id` BIGINT(20) NOT NULL AUTO_INCREMENT COMMENT '对话ID',
  `user_id` VARCHAR(64) NOT NULL COMMENT '用户ID',
  `session_id` VARCHAR(64) NOT NULL COMMENT '会话ID',
  `role` VARCHAR(16) NOT NULL COMMENT '角色: user, assistant, system',
  `content` TEXT NOT NULL COMMENT '消息内容',
  `message_type` VARCHAR(32) DEFAULT 'text' COMMENT '消息类型: text, image, file',
  `tool_calls` JSON DEFAULT NULL COMMENT '工具调用信息',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_session_id` (`session_id`),
  KEY `idx_created_at` (`created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='对话历史表';

-- ============================================
-- 5. 天气查询记录表
-- ============================================
DROP TABLE IF EXISTS `weather_query`;
CREATE TABLE `weather_query` (
  `id` BIGINT(20) NOT NULL AUTO_INCREMENT COMMENT '查询ID',
  `user_id` VARCHAR(64) NOT NULL COMMENT '用户ID',
  `city` VARCHAR(64) NOT NULL COMMENT '城市名称',
  `weather_data` JSON DEFAULT NULL COMMENT '天气数据JSON',
  `temperature` DECIMAL(5,2) DEFAULT NULL COMMENT '温度',
  `humidity` INT(11) DEFAULT NULL COMMENT '湿度(%)',
  `wind_speed` DECIMAL(5,2) DEFAULT NULL COMMENT '风速(m/s)',
  `condition` VARCHAR(32) DEFAULT NULL COMMENT '天气状况',
  `advice` VARCHAR(512) DEFAULT NULL COMMENT '出行建议',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '查询时间',
  PRIMARY KEY (`id`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_city` (`city`),
  KEY `idx_created_at` (`created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='天气查询记录表';

-- ============================================
-- 6. 计算记录表
-- ============================================
DROP TABLE IF EXISTS `calculation_record`;
CREATE TABLE `calculation_record` (
  `id` BIGINT(20) NOT NULL AUTO_INCREMENT COMMENT '记录ID',
  `user_id` VARCHAR(64) NOT NULL COMMENT '用户ID',
  `expression` VARCHAR(512) NOT NULL COMMENT '数学表达式',
  `result` DECIMAL(20,4) NOT NULL COMMENT '计算结果',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '计算时间',
  PRIMARY KEY (`id`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_created_at` (`created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='计算记录表';

-- ============================================
-- 7. 系统配置表
-- ============================================
DROP TABLE IF EXISTS `system_config`;
CREATE TABLE `system_config` (
  `id` BIGINT(20) NOT NULL AUTO_INCREMENT COMMENT '配置ID',
  `config_key` VARCHAR(128) NOT NULL COMMENT '配置键',
  `config_value` TEXT COMMENT '配置值',
  `config_type` VARCHAR(32) DEFAULT 'string' COMMENT '配置类型: string, number, boolean, json',
  `description` VARCHAR(255) DEFAULT NULL COMMENT '配置描述',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_config_key` (`config_key`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='系统配置表';

-- ============================================
-- 8. 操作日志表
-- ============================================
DROP TABLE IF EXISTS `operation_log`;
CREATE TABLE `operation_log` (
  `id` BIGINT(20) NOT NULL AUTO_INCREMENT COMMENT '日志ID',
  `user_id` VARCHAR(64) NOT NULL COMMENT '用户ID',
  `operation` VARCHAR(64) NOT NULL COMMENT '操作类型',
  `module` VARCHAR(64) NOT NULL COMMENT '模块名称',
  `description` VARCHAR(512) DEFAULT NULL COMMENT '操作描述',
  `request_data` JSON DEFAULT NULL COMMENT '请求数据',
  `response_data` JSON DEFAULT NULL COMMENT '响应数据',
  `ip_address` VARCHAR(64) DEFAULT NULL COMMENT 'IP地址',
  `status` TINYINT(1) NOT NULL DEFAULT 1 COMMENT '状态: 0-失败, 1-成功',
  `error_message` TEXT DEFAULT NULL COMMENT '错误信息',
  `execution_time` INT(11) DEFAULT NULL COMMENT '执行时间(ms)',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_operation` (`operation`),
  KEY `idx_module` (`module`),
  KEY `idx_created_at` (`created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='操作日志表';

-- ============================================
-- 插入初始数据
-- ============================================

-- 插入默认系统配置
INSERT INTO `system_config` (`config_key`, `config_value`, `config_type`, `description`) VALUES
('agent.api.enabled', 'true', 'boolean', '是否启用AI API'),
('agent.api.model', 'gpt-3.5-turbo', 'string', 'AI模型名称'),
('agent.max.history.length', '20', 'number', '最大对话历史长度'),
('agent.file.base.dir', 'agent-files', 'string', '文件存储基础目录'),
('agent.task.auto.cleanup.days', '30', 'number', '任务自动清理天数'),
('weather.api.enabled', 'false', 'boolean', '是否启用天气API'),
('weather.default.city', '北京', 'string', '默认查询城市');

-- 插入测试用户（密码: 123456）
INSERT INTO `user` (`user_id`, `username`, `password`, `email`, `status`) VALUES
('user_001', '测试用户1', '123456', 'test1@example.com', 1),
('user_002', '测试用户2', '123456', 'test2@example.com', 1),
('admin', '管理员', '123456', 'admin@example.com', 1);

-- 插入示例任务
INSERT INTO `task` (`user_id`, `title`, `description`, `status`, `priority`) VALUES
('user_001', '完成项目报告', '撰写项目总结报告并提交', 0, 1),
('user_001', '学习新技术', '学习Spring Boot和AI集成', 0, 0),
('user_002', '准备会议材料', '准备下周项目评审会议材料', 0, 2);

-- ============================================
-- 创建视图
-- ============================================

-- 用户任务统计视图
CREATE OR REPLACE VIEW `v_user_task_stats` AS
SELECT 
    u.user_id,
    u.username,
    COUNT(t.id) as total_tasks,
    SUM(CASE WHEN t.status = 0 THEN 1 ELSE 0 END) as pending_tasks,
    SUM(CASE WHEN t.status = 1 THEN 1 ELSE 0 END) as completed_tasks,
    SUM(CASE WHEN t.status = 2 THEN 1 ELSE 0 END) as cancelled_tasks
FROM `user` u
LEFT JOIN `task` t ON u.user_id = t.user_id
GROUP BY u.user_id, u.username;

-- 用户活动统计视图
CREATE OR REPLACE VIEW `v_user_activity_stats` AS
SELECT 
    u.user_id,
    u.username,
    (SELECT COUNT(*) FROM `conversation` c WHERE c.user_id = u.user_id) as conversation_count,
    (SELECT COUNT(*) FROM `file_record` f WHERE f.user_id = u.user_id) as file_count,
    (SELECT COUNT(*) FROM `weather_query` w WHERE w.user_id = u.user_id) as weather_query_count,
    (SELECT COUNT(*) FROM `calculation_record` calc WHERE calc.user_id = u.user_id) as calculation_count
FROM `user` u;

-- ============================================
-- 创建存储过程
-- ============================================

-- 清理过期对话历史
DELIMITER //
CREATE PROCEDURE IF NOT EXISTS `sp_cleanup_old_conversations`(IN days_to_keep INT)
BEGIN
    DELETE FROM `conversation` 
    WHERE `created_at` < DATE_SUB(NOW(), INTERVAL days_to_keep DAY);
    
    SELECT ROW_COUNT() as deleted_rows;
END //
DELIMITER ;

-- 清理过期操作日志
DELIMITER //
CREATE PROCEDURE IF NOT EXISTS `sp_cleanup_old_logs`(IN days_to_keep INT)
BEGIN
    DELETE FROM `operation_log` 
    WHERE `created_at` < DATE_SUB(NOW(), INTERVAL days_to_keep DAY);
    
    SELECT ROW_COUNT() as deleted_rows;
END //
DELIMITER ;

-- ============================================
-- 创建索引优化建议
-- ============================================
-- 如果需要进一步优化，可以考虑添加以下索引：
-- ALTER TABLE `conversation` ADD INDEX `idx_user_session` (`user_id`, `session_id`);
-- ALTER TABLE `task` ADD INDEX `idx_user_status` (`user_id`, `status`);
-- ALTER TABLE `operation_log` ADD INDEX `idx_user_operation` (`user_id`, `operation`);

-- ============================================
-- 数据库信息
-- ============================================
SELECT 'Database initialization completed successfully!' as message;
SELECT DATABASE() as current_database;
SELECT COUNT(*) as total_tables FROM information_schema.tables WHERE table_schema = DATABASE();
