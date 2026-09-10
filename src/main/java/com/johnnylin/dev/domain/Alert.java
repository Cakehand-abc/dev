package com.johnnylin.dev.domain;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.*;

/**
 * 电子地理围栏出界告警事件实体。
 *
 * <p>映射数据库表 {@code geofence_alert}，记录长者佩戴的智能设备离开所设定的安全围栏
 * 时触发的告警全生命周期数据，包括出界定位点快照、触发时间、自动回栏时间、人工处置状态等。
 */
@Data
@TableName("geofence_alert")
public class Alert {

    /** 告警记录主键 ID，数据库自增 */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 关联的围栏成员关系 ID (geofence_member.id) */
    private Long memberId;

    /** 触发越界告警时的首个越界定位点 ID (location_point.id) */
    private Long exitPointId;

    /** 越界告警触发时间（由定位点的 recordedAt 决定） */
    private LocalDateTime triggeredAt;

    /** 长者重新回到围栏范围内的时间（自动消除时写入） */
    private LocalDateTime returnedAt;

    /** 告警关闭/归档时间 */
    private LocalDateTime closedAt;

    /** 告警关闭原因（例如 RETURNED: 自行返回, RECONFIGURED: 围栏重配, DISABLED: 围栏停用, REMOVED: 成员移除） */
    private String closeReason;

    /** 人工处理该告警的管理员用户 ID (sys_user.id) */
    private Long handledBy;

    /** 人工处理时间 */
    private LocalDateTime handledAt;

    /** 人工处理意见与跟踪备注 */
    private String handlingNote;

    /** 告警发生时围栏几何参数的 JSON 静态快照（中心经纬度、半径及配置版本号） */
    private String geometrySnapshot;

    /** 乐观锁版本号 */
    private Integer version;
}

