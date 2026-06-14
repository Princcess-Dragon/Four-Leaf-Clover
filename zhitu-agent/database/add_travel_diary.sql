-- ============================================
-- 旅行日记表
-- ============================================

USE `agent`;

DROP TABLE IF EXISTS `travel_diary`;
CREATE TABLE `travel_diary` (
  `id` BIGINT(20) NOT NULL AUTO_INCREMENT COMMENT '日记ID',
  `user_id` VARCHAR(64) NOT NULL COMMENT '用户ID',
  `title` VARCHAR(255) NOT NULL COMMENT '日记标题',
  `content` TEXT NOT NULL COMMENT '日记内容',
  `location` VARCHAR(255) DEFAULT NULL COMMENT '旅行地点',
  `travel_date` DATE DEFAULT NULL COMMENT '旅行日期',
  `weather` VARCHAR(64) DEFAULT NULL COMMENT '天气状况',
  `mood` VARCHAR(64) DEFAULT NULL COMMENT '心情',
  `images` JSON DEFAULT NULL COMMENT '图片URL列表(JSON数组)',
  `tags` VARCHAR(512) DEFAULT NULL COMMENT '标签(逗号分隔)',
  `cover_image` VARCHAR(512) DEFAULT NULL COMMENT '封面图片URL',
  `is_public` TINYINT(1) NOT NULL DEFAULT 1 COMMENT '是否公开: 0-私密, 1-公开',
  `view_count` INT(11) NOT NULL DEFAULT 0 COMMENT '浏览次数',
  `like_count` INT(11) NOT NULL DEFAULT 0 COMMENT '点赞次数',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_travel_date` (`travel_date`),
  KEY `idx_created_at` (`created_at`),
  KEY `idx_is_public` (`is_public`),
  KEY `idx_location` (`location`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='旅行日记表';
