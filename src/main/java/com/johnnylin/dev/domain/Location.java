package com.johnnylin.dev.domain;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.*;

/**
 * GPS 空间地理定位轨迹点实体。
 *
 * <p>映射数据库表 {@code location_point}，记录智能腕表设备采集并上传的时空轨迹点，
 * 保存经纬度坐标、采集时间、所属设备、绑定长者以及对应绑定周期 ID，支撑历史轨迹回放与围栏越界判定。
 */
@Data
@TableName("location_point")
public class Location {

    /** 定位点主键 ID，数据库自增 */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 上报定位的智能设备 ID (watch_device.id) */
    private Long deviceId;

    /** 定位时刻所关联生效的设备长者绑定记录 ID (device_binding.id) */
    private Long bindingId;

    /** 定位时刻佩戴该设备的长者档案 ID (elder.id) */
    private Long elderId;

    /** 客户端/设备端生成的唯一定位事件流水号，用于幂等去重 */
    private String eventId;

    /** 经度坐标（WGS-84 坐标系，保留 7 位小数，范围 -180.0 ~ 180.0） */
    private Double longitude;

    /** 纬度坐标（WGS-84 坐标系，保留 7 位小数，范围 -90.0 ~ 90.0） */
    private Double latitude;

    /** 设备 GPS 硬件实际采集定位的时间戳 */
    private LocalDateTime recordedAt;

    /** 服务端接收并写入本条定位记录的时间戳 */
    private LocalDateTime receivedAt;
}

