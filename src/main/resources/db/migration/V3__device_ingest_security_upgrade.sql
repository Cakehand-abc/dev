-- ===================================================================
-- 智慧医养平台 Flyway 版本迁移脚本 V3: 追加物联网设备访问鉴权与心跳保活流水
-- （源自 03-device-ingest-upgrade.sql 增量安全升级）
-- ===================================================================

-- 1. 物联网终端直连访问 API 密钥凭证表（保存 SHA-256 签名加盐散列值）
CREATE TABLE device_credential (
    device_id BIGINT PRIMARY KEY,
    key_hash CHAR(64) NOT NULL UNIQUE,
    created_at DATETIME(6) NOT NULL,
    rotated_at DATETIME(6),
    FOREIGN KEY (device_id) REFERENCES watch_device(id)
);

-- 2. 硬件终端网络心跳保活与状态监测事件流水表（具备 event_id 幂等约束）
CREATE TABLE device_heartbeat_event (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    device_id BIGINT NOT NULL,
    event_id VARCHAR(100) NOT NULL,
    recorded_at DATETIME(6) NOT NULL,
    received_at DATETIME(6) NOT NULL,
    FOREIGN KEY (device_id) REFERENCES watch_device(id),
    UNIQUE(device_id, event_id)
);

CREATE INDEX ix_heartbeat_device_time ON device_heartbeat_event(device_id, recorded_at, id);
