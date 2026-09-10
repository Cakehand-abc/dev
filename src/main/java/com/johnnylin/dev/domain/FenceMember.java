package com.johnnylin.dev.domain;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

/**
 * 电子地理围栏纳管长者成员关联实体。
 *
 * <p>映射数据库表 {@code geofence_member}，建立围栏与受保护长者的多对多业务关联，
 * 并支持单独启用或临时禁用特定成员在某围栏中的监控。
 */
@Data
@TableName("geofence_member")
public class FenceMember {

    /** 成员关联记录主键 ID，数据库自增 */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 关联的电子围栏 ID (geofence.id) */
    private Long fenceId;

    /** 关联的长者档案 ID (elder.id) */
    private Long elderId;

    /** 成员在此围栏中的监控启用状态（true 为生效，false 为暂停） */
    private Boolean enabled;
}

