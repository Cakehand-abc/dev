-- Upgrade an existing smart_care database for authenticated device ingestion.
-- Apply once after backing up the database. The script intentionally has no DROP statements.
CREATE TABLE device_credential (
 device_id BIGINT PRIMARY KEY, key_hash CHAR(64) NOT NULL UNIQUE,
 created_at DATETIME(6) NOT NULL, rotated_at DATETIME(6),
 FOREIGN KEY (device_id) REFERENCES watch_device(id)
);

CREATE TABLE device_heartbeat_event (
 id BIGINT AUTO_INCREMENT PRIMARY KEY, device_id BIGINT NOT NULL, event_id VARCHAR(100) NOT NULL,
 recorded_at DATETIME(6) NOT NULL, received_at DATETIME(6) NOT NULL,
 FOREIGN KEY (device_id) REFERENCES watch_device(id), UNIQUE(device_id,event_id)
);

CREATE INDEX ix_heartbeat_device_time ON device_heartbeat_event(device_id,recorded_at,id);
