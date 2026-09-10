package com.johnnylin.dev.domain;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.*;

/**
 * 智能定位手环/腕表硬件设备实体。
 *
 * <p>映射数据库表 {@code watch_device}，维护 IoT 终端设备的硬件序列号 (SN)、
 * 型号、可用启用状态、最后一次心跳保活上报时间及乐观锁版本号。
 */
@Data
@TableName("watch_device")
public class Device {

    /** 设备记录主键 ID，数据库自增 */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 设备硬件唯一出厂序列号 (Serial Number) */
    private String serialNo;

    /** 设备硬件型号（例如 教学模拟腕表、HW-W01 等） */
    private String model;

    /** 设备启用状态（true 为启用，false 为禁用；禁用后拒绝定位和心跳上报） */
    private Boolean enabled;

    /** 设备最后一次与服务端完成心跳或数据通讯的时间 */
    private LocalDateTime lastSeenAt;

    /** 乐观锁版本号，防止并发修改覆盖 */
    private Integer version;
}

