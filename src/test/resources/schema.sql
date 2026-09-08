-- Smart Care schema: MySQL 8.0.16+. Run only in a new dedicated database.
-- No DROP statements. Re-running fails visibly rather than deleting existing data.
CREATE TABLE sys_user (
 id BIGINT AUTO_INCREMENT PRIMARY KEY, username VARCHAR(64) NOT NULL UNIQUE,
 password_hash VARCHAR(100) NOT NULL, display_name VARCHAR(80) NOT NULL,
 role VARCHAR(20) NOT NULL, enabled BOOLEAN NOT NULL DEFAULT TRUE, auth_version INT NOT NULL DEFAULT 0,
 CONSTRAINT ck_user_role CHECK (role IN ('ADMIN','OPERATOR','ANALYST'))
);
CREATE TABLE region (id BIGINT AUTO_INCREMENT PRIMARY KEY, code VARCHAR(30) NOT NULL UNIQUE, name VARCHAR(80) NOT NULL, parent_id BIGINT, FOREIGN KEY (parent_id) REFERENCES region(id));
CREATE TABLE elder (
 id BIGINT AUTO_INCREMENT PRIMARY KEY, code VARCHAR(40) NOT NULL UNIQUE, name VARCHAR(80) NOT NULL,
 gender VARCHAR(10) NOT NULL, birth_date DATE, region_id BIGINT NOT NULL, phone VARCHAR(30),
 status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE', version INT NOT NULL DEFAULT 0,
 FOREIGN KEY (region_id) REFERENCES region(id), CHECK (gender IN ('MALE','FEMALE','UNKNOWN')),
 CHECK (status IN ('ACTIVE','ARCHIVED'))
);
CREATE INDEX ix_elder_region ON elder(region_id,status);
CREATE TABLE doctor (
 id BIGINT AUTO_INCREMENT PRIMARY KEY, code VARCHAR(40) NOT NULL UNIQUE, name VARCHAR(80) NOT NULL,
 department VARCHAR(100) NOT NULL, phone VARCHAR(30), enabled BOOLEAN NOT NULL DEFAULT TRUE, version INT NOT NULL DEFAULT 0
);
CREATE TABLE health_rule (
 id BIGINT AUTO_INCREMENT PRIMARY KEY, metric VARCHAR(30) NOT NULL, lower_bound DOUBLE NOT NULL,
 upper_bound DOUBLE NOT NULL, unit VARCHAR(20) NOT NULL, version INT NOT NULL, enabled BOOLEAN NOT NULL,
 UNIQUE(metric,version), CHECK (lower_bound < upper_bound)
);
CREATE TABLE health_record (
 id BIGINT AUTO_INCREMENT PRIMARY KEY, elder_id BIGINT NOT NULL, measured_at DATETIME(6) NOT NULL,
 systolic DOUBLE, diastolic DOUBLE, heart_rate DOUBLE, oxygen DOUBLE, temperature DOUBLE,
 abnormal BOOLEAN NOT NULL, rule_snapshot TEXT NOT NULL, source VARCHAR(30) NOT NULL,
 event_id VARCHAR(100) NOT NULL, FOREIGN KEY (elder_id) REFERENCES elder(id), UNIQUE(source,event_id)
);
CREATE INDEX ix_health_elder_time ON health_record(elder_id,measured_at);
CREATE INDEX ix_health_time_abnormal ON health_record(measured_at,abnormal);
CREATE TABLE followup_plan (
 id BIGINT AUTO_INCREMENT PRIMARY KEY, elder_id BIGINT NOT NULL, doctor_id BIGINT NOT NULL, due_at DATETIME(6) NOT NULL,
 status VARCHAR(20) NOT NULL DEFAULT 'PENDING', canceled_at DATETIME(6), cancel_reason VARCHAR(500), version INT NOT NULL DEFAULT 0,
 FOREIGN KEY (elder_id) REFERENCES elder(id), FOREIGN KEY (doctor_id) REFERENCES doctor(id),
 CHECK (status IN ('PENDING','COMPLETED','CANCELED'))
);
CREATE INDEX ix_plan_doctor_due ON followup_plan(doctor_id,due_at);
CREATE INDEX ix_plan_elder_due ON followup_plan(elder_id,due_at);
CREATE TABLE followup_record (
 id BIGINT AUTO_INCREMENT PRIMARY KEY, plan_id BIGINT NOT NULL UNIQUE, completed_at DATETIME(6) NOT NULL,
 content VARCHAR(2000) NOT NULL, result VARCHAR(500) NOT NULL, FOREIGN KEY (plan_id) REFERENCES followup_plan(id)
);
CREATE INDEX ix_followup_completed ON followup_record(completed_at);
CREATE TABLE watch_device (
 id BIGINT AUTO_INCREMENT PRIMARY KEY, serial_no VARCHAR(80) NOT NULL UNIQUE, model VARCHAR(80) NOT NULL,
 enabled BOOLEAN NOT NULL DEFAULT TRUE, last_seen_at DATETIME(6), version INT NOT NULL DEFAULT 0
);
CREATE TABLE device_binding (
 id BIGINT AUTO_INCREMENT PRIMARY KEY, device_id BIGINT NOT NULL, elder_id BIGINT NOT NULL,
 bound_at DATETIME(6) NOT NULL, unbound_at DATETIME(6), active_device_id BIGINT UNIQUE, active_elder_id BIGINT UNIQUE,
 FOREIGN KEY (device_id) REFERENCES watch_device(id), FOREIGN KEY (elder_id) REFERENCES elder(id),
 CHECK ((unbound_at IS NULL AND active_device_id IS NOT NULL AND active_elder_id IS NOT NULL
         AND active_device_id = device_id AND active_elder_id = elder_id)
     OR (unbound_at IS NOT NULL AND active_device_id IS NULL AND active_elder_id IS NULL)),
 CHECK (unbound_at IS NULL OR unbound_at >= bound_at)
);
CREATE INDEX ix_binding_history ON device_binding(device_id,bound_at,unbound_at);
CREATE TABLE location_point (
 id BIGINT AUTO_INCREMENT PRIMARY KEY, device_id BIGINT NOT NULL, binding_id BIGINT NOT NULL, elder_id BIGINT NOT NULL,
 event_id VARCHAR(100) NOT NULL, longitude DECIMAL(10,7) NOT NULL, latitude DECIMAL(10,7) NOT NULL,
 recorded_at DATETIME(6) NOT NULL, received_at DATETIME(6) NOT NULL,
 FOREIGN KEY (device_id) REFERENCES watch_device(id), FOREIGN KEY (binding_id) REFERENCES device_binding(id),
 FOREIGN KEY (elder_id) REFERENCES elder(id), UNIQUE(device_id,event_id),
 CHECK (longitude BETWEEN -180 AND 180), CHECK (latitude BETWEEN -90 AND 90)
);
CREATE INDEX ix_location_elder_time ON location_point(elder_id,recorded_at,id);
CREATE INDEX ix_location_device_time ON location_point(device_id,recorded_at,id);
CREATE TABLE geofence (
 id BIGINT AUTO_INCREMENT PRIMARY KEY, name VARCHAR(80) NOT NULL, center_lon DECIMAL(10,7) NOT NULL,
 center_lat DECIMAL(10,7) NOT NULL, radius_m DOUBLE NOT NULL, enabled BOOLEAN NOT NULL DEFAULT TRUE,
 version INT NOT NULL DEFAULT 0, CHECK (radius_m BETWEEN 50 AND 5000),
 CHECK (center_lon BETWEEN -180 AND 180), CHECK (center_lat BETWEEN -90 AND 90)
);
CREATE TABLE geofence_member (
 id BIGINT AUTO_INCREMENT PRIMARY KEY, fence_id BIGINT NOT NULL, elder_id BIGINT NOT NULL, enabled BOOLEAN NOT NULL,
 FOREIGN KEY (fence_id) REFERENCES geofence(id), FOREIGN KEY (elder_id) REFERENCES elder(id), UNIQUE(fence_id,elder_id)
);
CREATE TABLE geofence_state (
 id BIGINT AUTO_INCREMENT PRIMARY KEY, member_id BIGINT NOT NULL UNIQUE, last_point_id BIGINT,
 last_recorded_at DATETIME(6), inside BOOLEAN, active_alert_id BIGINT,
 FOREIGN KEY (member_id) REFERENCES geofence_member(id), FOREIGN KEY (last_point_id) REFERENCES location_point(id)
);
CREATE TABLE geofence_alert (
 id BIGINT AUTO_INCREMENT PRIMARY KEY, member_id BIGINT NOT NULL, exit_point_id BIGINT NOT NULL,
 triggered_at DATETIME(6) NOT NULL, returned_at DATETIME(6), closed_at DATETIME(6), close_reason VARCHAR(30),
 handled_by BIGINT, handled_at DATETIME(6), handling_note VARCHAR(1000), geometry_snapshot TEXT NOT NULL,
 version INT NOT NULL DEFAULT 0, FOREIGN KEY (member_id) REFERENCES geofence_member(id),
 FOREIGN KEY (exit_point_id) REFERENCES location_point(id), FOREIGN KEY (handled_by) REFERENCES sys_user(id)
);
ALTER TABLE geofence_state ADD CONSTRAINT fk_state_alert FOREIGN KEY (active_alert_id) REFERENCES geofence_alert(id);
CREATE INDEX ix_alert_member_time ON geofence_alert(member_id,triggered_at);
CREATE INDEX ix_alert_handled ON geofence_alert(handled_at);
CREATE TABLE audit_log (
 id BIGINT AUTO_INCREMENT PRIMARY KEY, user_id BIGINT, action VARCHAR(60) NOT NULL, target_type VARCHAR(60) NOT NULL,
 target_id BIGINT, occurred_at DATETIME(6) NOT NULL, result VARCHAR(40) NOT NULL, FOREIGN KEY (user_id) REFERENCES sys_user(id)
);
CREATE INDEX ix_audit_user_time ON audit_log(user_id,occurred_at);

