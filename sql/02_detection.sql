-- 皮肤病变检测系统数据库结构（仅结构，不含数据）
-- 分类：检测业务

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

DROP TABLE IF EXISTS `detect`;
CREATE TABLE `detect` (
  `id` int NOT NULL AUTO_INCREMENT COMMENT '检测记录ID',
  `user_id` int NOT NULL COMMENT '操作用户ID',
  `user_name` varchar(50) DEFAULT NULL COMMENT '操作用户名',
  `original_image_name` varchar(255) DEFAULT NULL COMMENT '原始图像文件名',
  `original_image_url` varchar(500) NOT NULL COMMENT '原始图像存储路径/URL',
  `original_image_size` bigint DEFAULT NULL COMMENT '原始图像大小(字节)',
  `original_image_format` varchar(10) DEFAULT NULL COMMENT '原始图像格式(jpg/png等)',
  `result_image_url` varchar(500) DEFAULT NULL COMMENT '检测结果图像路径/URL',
  `detection_data` json DEFAULT NULL COMMENT '检测结果数据: 包含检测框坐标、置信度、病变类型等',
  `ai_model` varchar(50) DEFAULT NULL COMMENT '使用的大模型(deepseek/glm/kimi)',
  `ai_analysis_result` text COMMENT 'AI大模型分析结果文本',
  `ai_analysis_time` datetime DEFAULT NULL COMMENT 'AI分析完成时间',
  `detect_status` tinyint DEFAULT '0' COMMENT '检测状态: 0-待检测 1-检测中 2-检测完成 3-检测失败',
  `ai_status` tinyint DEFAULT '0' COMMENT 'AI分析状态: 0-未分析 1-分析中 2-分析完成 3-分析失败',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '记录创建时间',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '记录更新时间',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注信息',
  PRIMARY KEY (`id`),
  KEY `idx_detect_user_id` (`user_id`),
  KEY `idx_detect_status` (`detect_status`),
  KEY `idx_detect_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='皮肤病变检测记录表';

DROP TABLE IF EXISTS `video_detect`;
CREATE TABLE `video_detect` (
  `id` int NOT NULL AUTO_INCREMENT COMMENT '视频检测记录ID',
  `user_id` int NOT NULL COMMENT '操作用户ID',
  `user_name` varchar(50) DEFAULT NULL COMMENT '操作用户名',
  `original_video_name` varchar(255) DEFAULT NULL COMMENT '原始视频文件名',
  `original_video_url` varchar(500) NOT NULL COMMENT '原始视频存储路径/URL',
  `original_video_size` bigint DEFAULT NULL COMMENT '原始视频大小(字节)',
  `original_video_format` varchar(10) DEFAULT NULL COMMENT '原始视频格式(mp4/avi/mov等)',
  `result_video_url` varchar(500) DEFAULT NULL COMMENT '检测结果视频路径/URL',
  `detection_data` json DEFAULT NULL COMMENT '视频检测结果数据',
  `total_frames` int DEFAULT NULL COMMENT '视频总帧数',
  `fps` double DEFAULT NULL COMMENT '视频帧率',
  `duration` double DEFAULT NULL COMMENT '视频时长(秒)',
  `ai_model` varchar(50) DEFAULT NULL COMMENT '使用的大模型',
  `ai_analysis_result` text COMMENT 'AI大模型分析结果文本',
  `ai_analysis_time` datetime DEFAULT NULL COMMENT 'AI分析完成时间',
  `detect_status` tinyint DEFAULT '0' COMMENT '检测状态: 0-待检测 1-检测中 2-检测完成 3-检测失败',
  `ai_status` tinyint DEFAULT '0' COMMENT 'AI分析状态: 0-未分析 1-分析中 2-分析完成 3-分析失败',
  `source_type` varchar(20) DEFAULT 'upload' COMMENT '视频来源: upload-直接上传, batch_image-批量图片生成',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '记录创建时间',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '记录更新时间',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注信息',
  PRIMARY KEY (`id`),
  KEY `idx_video_detect_user_id` (`user_id`),
  KEY `idx_video_detect_status` (`detect_status`),
  KEY `idx_video_detect_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='皮肤病变视频检测记录表';

DROP TABLE IF EXISTS `detection_logs`;
CREATE TABLE `detection_logs` (
  `id` int NOT NULL AUTO_INCREMENT COMMENT '日志ID',
  `detect_id` int DEFAULT NULL COMMENT '关联detect表ID',
  `user_id` int DEFAULT NULL COMMENT '用户ID',
  `user_name` varchar(50) DEFAULT NULL COMMENT '用户名',
  `model_name` varchar(255) DEFAULT NULL COMMENT '权重名称',
  `conf_threshold` double DEFAULT NULL COMMENT '置信度阈值',
  `ai_model` varchar(50) DEFAULT NULL COMMENT 'AI模型',
  `tumor_type` varchar(100) DEFAULT NULL COMMENT '检测类型(良性/恶性/正常)',
  `confidence` double DEFAULT NULL COMMENT '检测置信度',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  KEY `idx_detection_logs_detect_id` (`detect_id`),
  KEY `idx_detection_logs_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='检测日志表';

SET FOREIGN_KEY_CHECKS = 1;
