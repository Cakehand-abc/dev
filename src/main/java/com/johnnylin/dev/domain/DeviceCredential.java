package com.johnnylin.dev.domain;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 智能设备端安全接入凭据实体。
 *
 * <p>映射数据库表 {@code device_credential}。设备端通过 HTTP Header {@code X-Device-Key}
 * 携带明文密钥（形如 {@code scd_...}），服务端仅持久化其 SHA-256 哈希值，用于设备端无状态身份鉴权。
 */
@Data
@TableName("device_credential")
public class DeviceCredential {

    /** 关联的设备 ID (watch_device.id)，作为本表主键 */
    @TableId
    private Long deviceId;

    /** 设备接入密钥明文的 SHA-256 十六进制哈希摘要 */
    private String keyHash;

    /** 凭据首次生成创建时间 */
    private LocalDateTime createdAt;

    /** 凭据最近一次轮转重置时间 */
    private LocalDateTime rotatedAt;
}
