package com.johnnylin.dev.controller;

import com.johnnylin.dev.common.*;
import com.johnnylin.dev.service.*;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.*;

/**
 * 物联网硬件终端直连数据接入网关控制器。
 *
 * <p>本接口供定位手环、智能胸牌等硬件终端或第三方 IoT 网关直连调用。
 * 每次调用均需在 HTTP 头部附带 {@code X-Device-Key} 预共享密钥，
 * 网关先交由 {@link DeviceAccessService#authenticate(Long, String)} 进行
 * SHA-256 签名哈希校验；鉴权通过后方可执行批量轨迹写入与心跳保活。
 */
@RestController
@RequestMapping("/api/device/v1")
@RequiredArgsConstructor
public class DeviceIngestController {

    private final DeviceAccessService access;
    private final GeoService geo;

    /**
     * 物联网设备批量位置轨迹数据加密上报接入端点。
     *
     * <p>首先提取请求体中的 {@code deviceId} 并比对请求头 {@code X-Device-Key}，
     * 校验合法后交由地理服务进行批量入库与电子围栏出入判定。
     *
     * @param key 存放在 HTTP 头部 {@code X-Device-Key} 中的明文设备访问密钥
     * @param body 请求载荷 Map，包含 deviceId 与 points 轨迹点数组
     * @return 轨迹点写入条数与触发告警明细的响应包装
     */
    @PostMapping("/locations")
    public Api<?> locations(
            @RequestHeader(value = "X-Device-Key", required = false) String key,
            @RequestBody Map<String, Object> body) {
        Long deviceId = Input.id(body, "deviceId");
        access.authenticate(deviceId, key);
        return Api.ok(geo.ingestBatch(body));
    }

    /**
     * 物联网设备运行状态与心跳保活数据上报接入端点。
     *
     * <p>用于设备上报当前电量、蜂窝信号质量及网络类型，并刷新设备最后在线心跳时间戳。
     *
     * @param key 存放在 HTTP 头部 {@code X-Device-Key} 中的明文设备访问密钥
     * @param body 请求载荷 Map，包含 deviceId, batteryLevel, signalStrength, networkType 等
     * @return 包含设备最新在线状态与响应时间戳的确认响应体
     */
    @PostMapping("/heartbeats")
    public Api<?> heartbeat(
            @RequestHeader(value = "X-Device-Key", required = false) String key,
            @RequestBody Map<String, Object> body) {
        Long deviceId = Input.id(body, "deviceId");
        access.authenticate(deviceId, key);
        return Api.ok(geo.heartbeat(body));
    }
}
