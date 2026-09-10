package com.johnnylin.dev.domain;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.*;

/**
 * 智能穿戴设备与长者绑定关系实体。
 *
 * <p>映射数据库表 {@code device_binding}。系统支持设备历史绑定轨迹追溯，
 * 每一个绑定周期保存一条记录：
 * <ul>
 *   <li>有效绑定状态：{@code activeDeviceId} 与 {@code activeElderId} 保持非空，且受数据库唯一索引约束保证一对一；</li>
 *   <li>解绑状态：写入 {@code unboundAt} 并将 {@code activeDeviceId} 与 {@code activeElderId} 置为 null。</li>
 * </ul>
 */
@Data
@TableName("device_binding")
public class Binding {

    /** 绑定记录主键 ID，数据库自增 */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 关联的智能设备 ID (watch_device.id) */
    private Long deviceId;

    /** 关联的长者档案 ID (elder.id) */
    private Long elderId;

    /** 绑定生效时间 */
    private LocalDateTime boundAt;

    /** 解绑解除时间（未解绑时为 null） */
    private LocalDateTime unboundAt;

    /**
     * 当前有效绑定设备 ID。
     *
     * <p>用于数据库唯一索引保证一台设备同时仅能被绑定一次；解绑时置空。
     */
    private Long activeDeviceId;

    /**
     * 当前有效绑定长者 ID。
     *
     * <p>用于数据库唯一索引保证一位长者同时仅能佩戴一台设备；解绑时置空。
     */
    private Long activeElderId;
}

