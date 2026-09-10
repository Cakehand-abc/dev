package com.johnnylin.dev.controller;

import com.johnnylin.dev.common.*;
import com.johnnylin.dev.service.GeoService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.*;

import java.util.*;

/**
 * 演示环境专用模拟数据上报控制器。
 *
 * <p>仅在激活 {@code demo} Profile 时生效（{@code @Profile("demo")}）。
 * 提供无需设备 API 密钥签名的便捷 HTTP 端点，专供前端演示系统、压力测试工具
 * 或联调脚本模拟生成硬件单点定位、批量轨迹和心跳保活事件。
 */
@RestController
@Profile("demo")
@RequestMapping("/api/v1/demo")
@RequiredArgsConstructor
public class DemoController {

    private final GeoService geo;

    /**
     * 演示环境模拟单条定位点上报。
     *
     * @param b 请求载荷 Map，包含 deviceId, lng, lat, altitude, speed, heading, accuracy, reportTime 等
     * @return 围栏检测与定位入库结果摘要
     */
    @PostMapping("/locations")
    public Api<?> location(@RequestBody Map<String, Object> b) {
        return Api.ok(geo.ingest(b));
    }

    /**
     * 演示环境模拟批量定位轨迹上报。
     *
     * @param b 请求载荷 Map，包含 deviceId 与 points 列表
     * @return 批量轨迹点入库处理结果，包含成功写入条数与触发的越界告警信息
     */
    @PostMapping("/locations/batch")
    public Api<?> locations(@RequestBody Map<String, Object> b) {
        return Api.ok(geo.ingestBatch(b));
    }

    /**
     * 演示环境模拟设备心跳事件上报。
     *
     * @param b 请求载荷 Map，包含 deviceId, batteryLevel, signalStrength, networkType 等
     * @return 更新后的设备在线状态与心跳确认数据
     */
    @PostMapping("/heartbeats")
    public Api<?> heartbeat(@RequestBody Map<String, Object> b) {
        return Api.ok(geo.heartbeat(b));
    }
}
