-- 皮肤病变检测系统数据库结构（仅结构，不含数据）
-- 分类：系统配置

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

DROP TABLE IF EXISTS `model_config`;
CREATE TABLE `model_config` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '配置ID',
  `page_type` varchar(20) NOT NULL COMMENT '页面类型：detect/video',
  `folder_path` varchar(500) NOT NULL COMMENT '模型文件夹路径',
  `display_name` varchar(200) DEFAULT NULL COMMENT '前端显示名称',
  `model_name` varchar(200) NOT NULL COMMENT '模型文件名',
  `model_path` varchar(500) NOT NULL COMMENT '模型完整路径',
  `model_type` varchar(10) NOT NULL COMMENT '模型类型：pt/pth/onnx',
  `is_active` tinyint DEFAULT '1' COMMENT '是否启用：0-禁用 1-启用',
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_page_model_path` (`page_type`,`model_path`),
  KEY `idx_page_type` (`page_type`),
  KEY `idx_folder_path` (`folder_path`),
  KEY `idx_is_active` (`is_active`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='模型配置表';

DROP TABLE IF EXISTS `system_config`;
CREATE TABLE `system_config` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '配置ID',
  `config_key` varchar(50) NOT NULL COMMENT '配置键',
  `config_value` varchar(500) NOT NULL COMMENT '配置值',
  `description` varchar(200) DEFAULT NULL COMMENT '配置说明',
  `updated_at` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `config_key` (`config_key`),
  KEY `idx_config_key` (`config_key`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='系统配置表';

DROP TABLE IF EXISTS `user_config`;
CREATE TABLE `user_config` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `user_id` int NOT NULL,
  `config_key` varchar(100) NOT NULL,
  `config_value` text,
  `description` varchar(255) DEFAULT NULL,
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_config_user_key` (`user_id`,`config_key`),
  KEY `idx_user_config_user_id` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户配置表';

INSERT INTO `system_config` (`config_key`, `config_value`, `description`) VALUES
('threshold.detect', '0.55', '图像检测页面默认置信度阈值'),
('threshold.video', '0.60', '视频检测页面默认置信度阈值'),
('global.backend_url', 'http://localhost:1907', '后端API地址'),
('global.python_detect_url', 'http://localhost:2026', 'Python检测服务地址'),
('ai.deepseek.api_key', '', 'DeepSeek API Key'),
('ai.glm.api_key', '', 'GLM(智谱) API Key'),
('ai.kimi.api_key', '', 'Kimi API Key')
ON DUPLICATE KEY UPDATE
  `description` = VALUES(`description`),
  `updated_at` = NOW();

SET FOREIGN_KEY_CHECKS = 1;
