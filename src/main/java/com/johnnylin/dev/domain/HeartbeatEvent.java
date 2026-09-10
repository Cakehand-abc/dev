package com.johnnylin.dev.domain;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 智能穿戴设备保活心跳事件实体。
 *
 * <p>映射数据库表 {@code device_heartbeat_event}，记录设备端定期主动上报的保活心跳，
 * 用于监测设备在线/离线健康状态，并支持根据唯一 {@code eventId} 进行幂等去重。
 */
@Data
@TableName("device_heartbeat_event")
public class HeartbeatEvent {

    /** 心跳事件记录主键 ID，数据库自增 */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 上报心跳的智能设备 ID (watch_device.id) */
    private Long deviceId;

    /** 客户端/设备端生成的唯一心跳事件流水号，用于幂等排重 */
    private String eventId;

    /** 设备本地生成心跳时的时间戳 */
    private LocalDateTime recordedAt;

    /** 服务端接收并写入本条心跳记录的时间戳 */
    private LocalDateTime receivedAt;
}
