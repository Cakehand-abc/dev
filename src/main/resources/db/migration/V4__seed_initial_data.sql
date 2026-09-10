-- ===================================================================
-- 智慧医养平台 Flyway 版本迁移脚本 V4: 初始化系统基础片区与健康阈值字典数据
-- （源自 02-demo-data.sql 基础初始化数据，兼容标准 SQL）
-- ===================================================================

-- 1. 初始化四个核心行政管理服务片区
INSERT INTO region (id, code, name, parent_id) VALUES
    (1, 'DEMO-1', '滨江片区', NULL),
    (2, 'DEMO-2', '文苑片区', NULL),
    (3, 'DEMO-3', '湖畔片区', NULL),
    (4, 'DEMO-4', '城北片区', NULL);

-- 2. 初始化国家标准生理健康体征基线判定规则字典
-- 指标项：systolic(收缩压 mmHg), diastolic(舒张压 mmHg), heartRate(心率 次/分), oxygen(血氧 %), temperature(体温 ℃)
INSERT INTO health_rule (id, metric, lower_bound, upper_bound, unit, version, enabled) VALUES
    (1, 'systolic', 90.0, 139.0, 'mmHg', 1, TRUE),
    (2, 'diastolic', 60.0, 89.0, 'mmHg', 1, TRUE),
    (3, 'heartRate', 60.0, 100.0, '次/分', 1, TRUE),
    (4, 'oxygen', 95.0, 100.0, '%', 1, TRUE),
    (5, 'temperature', 36.0, 37.3, '℃', 1, TRUE);

