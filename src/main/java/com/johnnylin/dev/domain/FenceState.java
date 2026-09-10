package com.johnnylin.dev.domain;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.*;

/**
 * 长者在电子围栏内的实时监控状态跟踪实体。
 *
 * <p>映射数据库表 {@code geofence_state}，保存长者相对于具体围栏的最新的在圈/出圈状态、
 * 最后计算判别的轨迹点、发生时间以及当前未消除的活跃越界告警 ID，作为状态机判定的缓存表。
 */
@Data
@TableName("geofence_state")
public class FenceState {

    /** 状态记录主键 ID，数据库自增 */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 关联的围栏成员关系 ID (geofence_member.id) */
    private Long memberId;

    /** 最后一次参与围栏判定计算的定位轨迹点 ID (location_point.id) */
    private Long lastPointId;

    /** 最后一个判定点的设备定位时间，用于保证判定严格按时间单调递增执行 */
    private LocalDateTime lastRecordedAt;

    /** 长者当前是否在围栏内部（true: 围栏内, false: 围栏外, null: 尚未收到有效定位或已重置） */
    private Boolean inside;

    /** 当前处于未关闭状态的活动出界告警 ID (geofence_alert.id)，长者回栏后重置为 null */
    private Long activeAlertId;
}

