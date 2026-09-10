-- ===================================================================
-- 智慧医养平台 Flyway 版本迁移脚本 V1: 初始化核心主数据与健康档案基础模型
-- ===================================================================

-- 1. 系统运营管理用户表
CREATE TABLE sys_user (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(64) NOT NULL UNIQUE,
    password_hash VARCHAR(100) NOT NULL,
    display_name VARCHAR(80) NOT NULL,
    role VARCHAR(20) NOT NULL,
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    auth_version INT NOT NULL DEFAULT 0,
    CONSTRAINT ck_user_role CHECK (role IN ('ADMIN', 'OPERATOR', 'ANALYST'))
);

-- 2. 行政服务片区与网格字典表
CREATE TABLE region (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    code VARCHAR(30) NOT NULL UNIQUE,
    name VARCHAR(80) NOT NULL,
    parent_id BIGINT,
    FOREIGN KEY (parent_id) REFERENCES region(id)
);

-- 3. 长者健康档案基础资料表
CREATE TABLE elder (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    code VARCHAR(40) NOT NULL UNIQUE,
    name VARCHAR(80) NOT NULL,
    gender VARCHAR(10) NOT NULL,
    birth_date DATE,
    region_id BIGINT NOT NULL,
    phone VARCHAR(30),
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    version INT NOT NULL DEFAULT 0,
    FOREIGN KEY (region_id) REFERENCES region(id),
    CHECK (gender IN ('MALE', 'FEMALE', 'UNKNOWN')),
    CHECK (status IN ('ACTIVE', 'ARCHIVED'))
);
CREATE INDEX ix_elder_region ON elder(region_id, status);

-- 4. 社区签约家庭与责任医生表
CREATE TABLE doctor (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    code VARCHAR(40) NOT NULL UNIQUE,
    name VARCHAR(80) NOT NULL,
    department VARCHAR(100) NOT NULL,
    phone VARCHAR(30),
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    version INT NOT NULL DEFAULT 0
);

-- 5. 生理体征健康基线阈值判定规则表
CREATE TABLE health_rule (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    metric VARCHAR(30) NOT NULL,
    lower_bound DOUBLE NOT NULL,
    upper_bound DOUBLE NOT NULL,
    unit VARCHAR(20) NOT NULL,
    version INT NOT NULL,
    enabled BOOLEAN NOT NULL,
    UNIQUE(metric, version),
    CHECK (lower_bound < upper_bound)
);

-- 6. 长者日常健康体征检测监测流水记录表
CREATE TABLE health_record (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    elder_id BIGINT NOT NULL,
    measured_at DATETIME(6) NOT NULL,
    systolic DOUBLE,
    diastolic DOUBLE,
    heart_rate DOUBLE,
    oxygen DOUBLE,
    temperature DOUBLE,
    abnormal BOOLEAN NOT NULL,
    rule_snapshot TEXT NOT NULL,
    source VARCHAR(30) NOT NULL,
    event_id VARCHAR(100) NOT NULL,
    FOREIGN KEY (elder_id) REFERENCES elder(id),
    UNIQUE(source, event_id)
);
CREATE INDEX ix_health_elder_time ON health_record(elder_id, measured_at);
CREATE INDEX ix_health_time_abnormal ON health_record(measured_at, abnormal);
