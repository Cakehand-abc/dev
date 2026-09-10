package com.johnnylin.dev.domain;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.*;

/**
 * 系统安全与业务操作审计日志实体。
 *
 * <p>映射数据库表 {@code audit_log}，记录管理端重要数据变更动作（如增删改长者、
 * 绑定设备、围栏调配、密码重置等），作为追溯合规与安全取证的关键凭据。
 */
@Data
@TableName("audit_log")
public class Audit {

    /** 审计日志主键 ID，数据库自增 */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 执行操作的操作员用户 ID (sys_user.id)，若为匿名/系统内部操作可为 null */
    private Long userId;

    /** 操作动作标识（例如 CREATE、UPDATE、DELETE、BIND、UNBIND、ROTATE_CREDENTIAL 等） */
    private String action;

    /** 目标业务对象类型（例如 elder、doctor、watch_device、geofence、sys_user 等） */
    private String targetType;

    /** 被操作对象的主键 ID */
    private Long targetId;

    /** 操作发生的时间戳 */
    private LocalDateTime occurredAt;

    /** 操作执行结果（例如 SUCCESS、FAILURE） */
    private String result;
}

