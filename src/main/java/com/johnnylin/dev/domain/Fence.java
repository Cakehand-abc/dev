package com.johnnylin.dev.domain;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

/**
 * 圆形电子地理围栏定义实体。
 *
 * <p>映射数据库表 {@code geofence}，定义监护安全区域的中心点经纬度、
 * 半径及启用状态，与长者成员关联后提供实时的离开与返回监控。
 */
@Data
@TableName("geofence")
public class Fence {

    /** 围栏主键 ID，数据库自增 */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 围栏名称（例如 滨江服务中心活动范围） */
    private String name;

    /** 圆心经度（WGS-84 坐标系，范围 -180.0 ~ 180.0） */
    private Double centerLon;

    /** 圆心纬度（WGS-84 坐标系，范围 -90.0 ~ 90.0） */
    private Double centerLat;

    /** 围栏半径，单位：米（允许范围 50 ~ 5000 米） */
    private Double radiusM;

    /** 围栏启用状态（true 为生效监控中，false 为停用） */
    private Boolean enabled;

    /** 乐观锁版本号 */
    private Integer version;
}

