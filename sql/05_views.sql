-- 皮肤病变检测系统数据库结构（仅结构，不含数据）
-- 分类：数据看板视图

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

DROP VIEW IF EXISTS `v_tumor_type_stats`;
CREATE VIEW `v_tumor_type_stats` AS
SELECT
    'defect_type' AS stat_type,
    CASE
        WHEN JSON_EXTRACT(detection_data, '$.defect_type') IS NOT NULL
            AND JSON_UNQUOTE(JSON_EXTRACT(detection_data, '$.defect_type')) != ''
            AND JSON_UNQUOTE(JSON_EXTRACT(detection_data, '$.defect_type')) != 'null'
            THEN JSON_UNQUOTE(JSON_EXTRACT(detection_data, '$.defect_type'))
        WHEN JSON_EXTRACT(detection_data, '$.boxes[0].defect_type') IS NOT NULL
            AND JSON_UNQUOTE(JSON_EXTRACT(detection_data, '$.boxes[0].defect_type')) != ''
            THEN JSON_UNQUOTE(JSON_EXTRACT(detection_data, '$.boxes[0].defect_type'))
        ELSE 'normal'
    END AS stat_key,
    COUNT(*) AS stat_count
FROM detect
WHERE detect_status = 2
GROUP BY stat_key;

DROP VIEW IF EXISTS `v_user_prediction_stats`;
CREATE VIEW `v_user_prediction_stats` AS
SELECT
    'user_prediction' AS stat_type,
    user_name AS stat_key,
    COUNT(*) AS stat_count
FROM detect
WHERE detect_status = 2
GROUP BY user_id, user_name;

DROP VIEW IF EXISTS `v_user_confidence_stats`;
CREATE VIEW `v_user_confidence_stats` AS
SELECT
    'user_confidence' AS stat_type,
    user_name AS stat_key,
    AVG(CAST(JSON_UNQUOTE(JSON_EXTRACT(detection_data, '$.boxes[0].confidence')) AS DECIMAL(5,4))) AS stat_value,
    COUNT(*) AS stat_count
FROM detect
WHERE detect_status = 2 AND detection_data IS NOT NULL
GROUP BY user_id, user_name;

DROP VIEW IF EXISTS `v_daily_stats`;
CREATE VIEW `v_daily_stats` AS
SELECT
    'daily' AS stat_type,
    DATE(create_time) AS stat_date,
    COUNT(*) AS stat_count
FROM detect
WHERE detect_status = 2
GROUP BY DATE(create_time);

SET FOREIGN_KEY_CHECKS = 1;
