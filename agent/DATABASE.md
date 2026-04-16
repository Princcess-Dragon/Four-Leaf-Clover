# AI办公助理 - 数据库设计文档

## 📊 数据库概述

本数据库为AI办公助理智能体提供数据持久化支持，采用MySQL 5.7+数据库，包含8个核心数据表、2个视图和2个存储过程。

## 🗄️ 数据库信息

- **数据库名称**: agent
- **字符集**: utf8mb4
- **排序规则**: utf8mb4_unicode_ci
- **存储引擎**: InnoDB
- **支持版本**: MySQL 5.7+

## 📋 数据表清单

### 1. user - 用户表
存储系统用户信息

| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGINT | 主键ID |
| user_id | VARCHAR(64) | 用户唯一标识 |
| username | VARCHAR(64) | 用户名 |
| email | VARCHAR(128) | 邮箱 |
| phone | VARCHAR(20) | 手机号 |
| avatar | VARCHAR(255) | 头像URL |
| status | TINYINT | 状态: 0-禁用, 1-启用 |
| created_at | DATETIME | 创建时间 |
| updated_at | DATETIME | 更新时间 |

**索引**:
- PRIMARY KEY (id)
- UNIQUE KEY uk_user_id (user_id)
- KEY idx_status (status)

### 2. task - 任务表
存储用户任务信息

| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGINT | 任务ID |
| user_id | VARCHAR(64) | 用户ID |
| title | VARCHAR(255) | 任务标题 |
| description | TEXT | 任务描述 |
| status | TINYINT | 状态: 0-待完成, 1-已完成, 2-已取消 |
| priority | TINYINT | 优先级: 0-普通, 1-重要, 2-紧急 |
| due_date | DATETIME | 截止时间 |
| completed_at | DATETIME | 完成时间 |
| created_at | DATETIME | 创建时间 |
| updated_at | DATETIME | 更新时间 |

**索引**:
- PRIMARY KEY (id)
- KEY idx_user_id (user_id)
- KEY idx_status (status)
- KEY idx_priority (priority)
- KEY idx_due_date (due_date)

### 3. file_record - 文件记录表
记录文件操作历史

| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGINT | 记录ID |
| user_id | VARCHAR(64) | 用户ID |
| file_name | VARCHAR(255) | 文件名 |
| file_path | VARCHAR(512) | 文件路径 |
| file_size | BIGINT | 文件大小(字节) |
| file_type | VARCHAR(64) | 文件类型 |
| content | TEXT | 文件内容 |
| operation | VARCHAR(32) | 操作类型: create, read, update, delete |
| created_at | DATETIME | 创建时间 |
| updated_at | DATETIME | 更新时间 |

**索引**:
- PRIMARY KEY (id)
- KEY idx_user_id (user_id)
- KEY idx_file_name (file_name)
- KEY idx_created_at (created_at)

### 4. conversation - 对话历史表
存储用户对话记录

| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGINT | 对话ID |
| user_id | VARCHAR(64) | 用户ID |
| session_id | VARCHAR(64) | 会话ID |
| role | VARCHAR(16) | 角色: user, assistant, system |
| content | TEXT | 消息内容 |
| message_type | VARCHAR(32) | 消息类型: text, image, file |
| tool_calls | JSON | 工具调用信息 |
| created_at | DATETIME | 创建时间 |

**索引**:
- PRIMARY KEY (id)
- KEY idx_user_id (user_id)
- KEY idx_session_id (session_id)
- KEY idx_created_at (created_at)

### 5. weather_query - 天气查询记录表
记录天气查询历史

| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGINT | 查询ID |
| user_id | VARCHAR(64) | 用户ID |
| city | VARCHAR(64) | 城市名称 |
| weather_data | JSON | 天气数据JSON |
| temperature | DECIMAL(5,2) | 温度 |
| humidity | INT | 湿度(%) |
| wind_speed | DECIMAL(5,2) | 风速(m/s) |
| condition | VARCHAR(32) | 天气状况 |
| advice | VARCHAR(512) | 出行建议 |
| created_at | DATETIME | 查询时间 |

**索引**:
- PRIMARY KEY (id)
- KEY idx_user_id (user_id)
- KEY idx_city (city)
- KEY idx_created_at (created_at)

### 6. calculation_record - 计算记录表
记录数学计算历史

| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGINT | 记录ID |
| user_id | VARCHAR(64) | 用户ID |
| expression | VARCHAR(512) | 数学表达式 |
| result | DECIMAL(20,4) | 计算结果 |
| created_at | DATETIME | 计算时间 |

**索引**:
- PRIMARY KEY (id)
- KEY idx_user_id (user_id)
- KEY idx_created_at (created_at)

### 7. system_config - 系统配置表
存储系统配置参数

| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGINT | 配置ID |
| config_key | VARCHAR(128) | 配置键 |
| config_value | TEXT | 配置值 |
| config_type | VARCHAR(32) | 配置类型: string, number, boolean, json |
| description | VARCHAR(255) | 配置描述 |
| updated_at | DATETIME | 更新时间 |

**索引**:
- PRIMARY KEY (id)
- UNIQUE KEY uk_config_key (config_key)

### 8. operation_log - 操作日志表
记录系统操作日志

| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGINT | 日志ID |
| user_id | VARCHAR(64) | 用户ID |
| operation | VARCHAR(64) | 操作类型 |
| module | VARCHAR(64) | 模块名称 |
| description | VARCHAR(512) | 操作描述 |
| request_data | JSON | 请求数据 |
| response_data | JSON | 响应数据 |
| ip_address | VARCHAR(64) | IP地址 |
| status | TINYINT | 状态: 0-失败, 1-成功 |
| error_message | TEXT | 错误信息 |
| execution_time | INT | 执行时间(ms) |
| created_at | DATETIME | 创建时间 |

**索引**:
- PRIMARY KEY (id)
- KEY idx_user_id (user_id)
- KEY idx_operation (operation)
- KEY idx_module (module)
- KEY idx_created_at (created_at)

## 📊 视图

### 1. v_user_task_stats - 用户任务统计视图
统计每个用户的任务数量（总数、待完成、已完成、已取消）

### 2. v_user_activity_stats - 用户活动统计视图
统计每个用户的活动数据（对话数、文件数、天气查询数、计算数）

## 🔧 存储过程

### 1. sp_cleanup_old_conversations
清理过期对话历史
```sql
CALL sp_cleanup_old_conversations(30); -- 清理30天前的对话
```

### 2. sp_cleanup_old_logs
清理过期操作日志
```sql
CALL sp_cleanup_old_logs(30); -- 清理30天前的日志
```

## 📝 初始化数据

### 系统配置
- agent.api.enabled: 是否启用AI API
- agent.api.model: AI模型名称
- agent.max.history.length: 最大对话历史长度
- agent.file.base.dir: 文件存储基础目录
- agent.task.auto.cleanup.days: 任务自动清理天数
- weather.api.enabled: 是否启用天气API
- weather.default.city: 默认查询城市

### 测试用户
- user_001: 测试用户1
- user_002: 测试用户2
- admin: 管理员

### 示例任务
包含3个示例任务，用于演示功能

## 🚀 使用方法

### 1. 初始化数据库
```bash
mysql -u root -p < server/src/main/resources/db/init.sql
```

或在MySQL客户端中执行：
```sql
source D:/code/java/agent/server/src/main/resources/db/init.sql
```

### 2. 验证安装
```sql
USE agent;
SHOW TABLES;
SELECT * FROM system_config;
SELECT * FROM v_user_task_stats;
```

### 3. 配置数据源
编辑 `application-dev.yml`:
```yaml
spring:
  datasource:
    druid:
      host: localhost
      port: 3306
      username: root
      password: your-password
```

## 🔍 常用查询示例

### 查询用户任务
```sql
SELECT * FROM task WHERE user_id = 'user_001' ORDER BY created_at DESC;
```

### 查询用户对话历史
```sql
SELECT * FROM conversation WHERE user_id = 'user_001' AND session_id = 'session_123' ORDER BY created_at ASC;
```

### 统计用户活动
```sql
SELECT * FROM v_user_activity_stats WHERE user_id = 'user_001';
```

### 查询最近天气记录
```sql
SELECT * FROM weather_query WHERE user_id = 'user_001' ORDER BY created_at DESC LIMIT 10;
```

### 清理过期数据
```sql
-- 清理30天前的对话
CALL sp_cleanup_old_conversations(30);

-- 清理30天前的日志
CALL sp_cleanup_old_logs(30);

-- 清理60天前的任务
DELETE FROM task WHERE created_at < DATE_SUB(NOW(), INTERVAL 60 DAY);
```

## 📈 性能优化建议

### 1. 索引优化
根据实际查询模式，可以考虑添加复合索引：
```sql
ALTER TABLE conversation ADD INDEX idx_user_session (user_id, session_id);
ALTER TABLE task ADD INDEX idx_user_status (user_id, status);
ALTER TABLE operation_log ADD INDEX idx_user_operation (user_id, operation);
```

### 2. 分区表
对于大数据量的表（如conversation、operation_log），可以考虑按时间分区：
```sql
ALTER TABLE conversation PARTITION BY RANGE (YEAR_MONTH(created_at)) (
    PARTITION p202401 VALUES LESS THAN (202402),
    PARTITION p202402 VALUES LESS THAN (202403),
    ...
);
```

### 3. 定期清理
建议设置定时任务清理过期数据：
```sql
-- 每天凌晨2点清理90天前的数据
CREATE EVENT cleanup_old_data
ON SCHEDULE EVERY 1 DAY
STARTS '2024-01-01 02:00:00'
DO
BEGIN
    CALL sp_cleanup_old_conversations(90);
    CALL sp_cleanup_old_logs(90);
END;
```

## 🔒 安全建议

1. **访问控制**: 限制数据库访问IP
2. **密码策略**: 使用强密码
3. **备份策略**: 定期备份数据库
4. **日志审计**: 启用操作日志审计
5. **数据加密**: 敏感数据加密存储

## 📊 ER关系图

```
user (1) ──────< (N) task
user (1) ──────< (N) file_record
user (1) ──────< (N) conversation
user (1) ──────< (N) weather_query
user (1) ──────< (N) calculation_record
user (1) ──────< (N) operation_log
```

## 📝 数据库版本

- **版本**: 1.0
- **创建日期**: 2024-01-15
- **最后更新**: 2024-01-15
- **维护者**: AI办公助理团队

---

**注意**: 生产环境使用前请根据实际情况调整配置和优化策略。
