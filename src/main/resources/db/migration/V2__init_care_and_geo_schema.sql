-- ===================================================================
-- 智慧医养平台 Flyway 版本迁移脚本 V2: 追加医生随访与地理电子围栏监护体系
-- ===================================================================

-- 1. 医生随访服务计划表
CREATE TABLE followup_plan (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    elder_id BIGINT NOT NULL,
    doctor_id BIGINT NOT NULL,
    due_at DATETIME(6) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    canceled_at DATETIME(6),
    cancel_reason VARCHAR(500),
    version INT NOT NULL DEFAULT 0,
    FOREIGN KEY (elder_id) REFERENCES elder(id),
    FOREIGN KEY (doctor_id) REFERENCES doctor(id),
    CHECK (status IN ('PENDING', 'COMPLETED', 'CANCELED'))
);
CREATE INDEX ix_plan_doctor_due ON followup_plan(doctor_id, due_at);
CREATE INDEX ix_plan_elder_due ON followup_plan(elder_id, due_at);

-- 2. 随访执行完成履约记录表
CREATE TABLE followup_record (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    plan_id BIGINT NOT NULL UNIQUE,
    completed_at DATETIME(6) NOT NULL,
    content VARCHAR(2000) NOT NULL,
    result VARCHAR(500) NOT NULL,
    FOREIGN KEY (plan_id) REFERENCES followup_plan(id)
);
CREATE INDEX ix_followup_completed ON followup_record(completed_at);

-- 3. 智能手表硬件设备主档案表
CREATE TABLE watch_device (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    serial_no VARCHAR(80) NOT NULL UNIQUE,
    model VARCHAR(80) NOT NULL,
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    last_seen_at DATETIME(6),
    version INT NOT NULL DEFAULT 0
);

-- 4. 智能硬件设备与长者佩戴绑定历史关联表
CREATE TABLE device_binding (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    device_id BIGINT NOT NULL,
    elder_id BIGINT NOT NULL,
    bound_at DATETIME(6) NOT NULL,
    unbound_at DATETIME(6),
    active_device_id BIGINT UNIQUE,
    active_elder_id BIGINT UNIQUE,
    FOREIGN KEY (device_id) REFERENCES watch_device(id),
    FOREIGN KEY (elder_id) REFERENCES elder(id),
    CHECK ((unbound_at IS NULL AND active_device_id IS NOT NULL AND active_elder_id IS NOT NULL
            AND active_device_id = device_id AND active_elder_id = elder_id)
        OR (unbound_at IS NOT NULL AND active_device_id IS NULL AND active_elder_id IS NULL)),
    CHECK (unbound_at IS NULL OR unbound_at >= bound_at)
);
CREATE INDEX ix_binding_history ON device_binding(device_id, bound_at, unbound_at);

-- 5. 硬件设备上报经纬度位置轨迹明细表
CREATE TABLE location_point (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    device_id BIGINT NOT NULL,
    binding_id BIGINT NOT NULL,
    elder_id BIGINT NOT NULL,
    event_id VARCHAR(100) NOT NULL,
    longitude DECIMAL(10, 7) NOT NULL,
    latitude DECIMAL(10, 7) NOT NULL,
    recorded_at DATETIME(6) NOT NULL,
    received_at DATETIME(6) NOT NULL,
    FOREIGN KEY (device_id) REFERENCES watch_device(id),
    FOREIGN KEY (binding_id) REFERENCES device_binding(id),
    FOREIGN KEY (elder_id) REFERENCES elder(id),
    UNIQUE(device_id, event_id),
    CHECK (longitude BETWEEN -180 AND 180),
    CHECK (latitude BETWEEN -90 AND 90)
);
CREATE INDEX ix_location_elder_time ON location_point(elder_id, recorded_at, id);
CREATE INDEX ix_location_device_time ON location_point(device_id, recorded_at, id);

-- 6. 地理圆形电子安全围栏规则表
CREATE TABLE geofence (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(80) NOT NULL,
    center_lon DECIMAL(10, 7) NOT NULL,
    center_lat DECIMAL(10, 7) NOT NULL,
    radius_m DOUBLE NOT NULL,
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    version INT NOT NULL DEFAULT 0,
    CHECK (radius_m BETWEEN 50 AND 5000),
    CHECK (center_lon BETWEEN -180 AND 180),
    CHECK (center_lat BETWEEN -90 AND 90)
);

-- 7. 电子安全围栏关联管控长者成员表
CREATE TABLE geofence_member (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    fence_id BIGINT NOT NULL,
    elder_id BIGINT NOT NULL,
    enabled BOOLEAN NOT NULL,
    FOREIGN KEY (fence_id) REFERENCES geofence(id),
    FOREIGN KEY (elder_id) REFERENCES elder(id),
    UNIQUE(fence_id, elder_id)
);

-- 8. 电子围栏长者实时进出界状态监控表
CREATE TABLE geofence_state (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    member_id BIGINT NOT NULL UNIQUE,
    last_point_id BIGINT,
    last_recorded_at DATETIME(6),
    inside BOOLEAN,
    active_alert_id BIGINT,
    FOREIGN KEY (member_id) REFERENCES geofence_member(id),
    FOREIGN KEY (last_point_id) REFERENCES location_point(id)
);

-- 9. 电子围栏越界与非法侵入报警流水表
CREATE TABLE geofence_alert (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    member_id BIGINT NOT NULL,
    exit_point_id BIGINT NOT NULL,
    triggered_at DATETIME(6) NOT NULL,
    returned_at DATETIME(6),
    closed_at DATETIME(6),
    close_reason VARCHAR(30),
    handled_by BIGINT,
    handled_at DATETIME(6),
    handling_note VARCHAR(1000),
    geometry_snapshot TEXT NOT NULL,
    version INT NOT NULL DEFAULT 0,
    FOREIGN KEY (member_id) REFERENCES geofence_member(id),
    FOREIGN KEY (exit_point_id) REFERENCES location_point(id),
    FOREIGN KEY (handled_by) REFERENCES sys_user(id)
);
ALTER TABLE geofence_state ADD CONSTRAINT fk_state_alert FOREIGN KEY (active_alert_id) REFERENCES geofence_alert(id);
CREATE INDEX ix_alert_member_time ON geofence_alert(member_id, triggered_at);
CREATE INDEX ix_alert_handled ON geofence_alert(handled_at);

-- 10. 系统全局操作安全审计日志表
CREATE TABLE audit_log (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT,
    action VARCHAR(60) NOT NULL,
    target_type VARCHAR(60) NOT NULL,
    target_id BIGINT,
    occurred_at DATETIME(6) NOT NULL,
    result VARCHAR(40) NOT NULL,
    FOREIGN KEY (user_id) REFERENCES sys_user(id)
);
CREATE INDEX ix_audit_user_time ON audit_log(user_id, occurred_at);
