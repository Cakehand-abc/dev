package com.johnnylin.dev.service;

import java.util.Map;

/**
 * 智能设备边缘接入凭据管理与无状态认证服务接口。
 *
 * <p>核心职责包括：
 * <ul>
 *   <li>为穿戴设备安全生成与轮转 256 位加密随机 API 密钥（带 {@code scd_} 前缀）</li>
 *   <li>在服务端仅存储密钥的 SHA-256 哈希值，防止数据库泄露导致密钥被利用</li>
 *   <li>对设备上报请求进行时间常数级（Constant-Time）哈希比对校验，防御侧信道时序攻击</li>
 * </ul>
 */
public interface DeviceAccessService {

    /**
     * 为指定穿戴设备生成或轮转重置接入 API 密钥。
     *
     * @param deviceId 设备主键 ID
     * @return 包含 deviceId、serialNo、apiKey（明文密钥）及 issuedAt 的响应 Map
     */
    Map<String, Object> rotate(Long deviceId);

    /**
     * 校验设备端上传数据时的 API 密钥与可用状态。
     *
     * @param deviceId 设备主键 ID
     * @param secret 设备在 Header {@code X-Device-Key} 中携带的明文密钥
     */
    void authenticate(Long deviceId, String secret);
}
