package com.johnnylin.dev.service;

import com.johnnylin.dev.domain.*;
import java.util.*;

/**
 * 空间地理信息、穿戴设备生命周期管理与电子围栏告警服务接口。
 *
 * <p>涵盖 IoT 穿戴设备台账、设备长者一对一动态绑定、高频 GPS 轨迹与心跳保活上报、
 * Haversine 球面距离判定以及电子围栏出界告警与自动回栏消除状态机。
 */
public interface GeoService {

    /**
     * 计算两个经纬度坐标点之间的地表球面大圆距离（Haversine 算法）。
     *
     * <p>地球平均半径取 6,371,008.8 米。
     *
     * @param lon1 点1经度
     * @param lat1 点1纬度
     * @param lon2 点2经度
     * @param lat2 点2纬度
     * @return 两点间的直线地表球面大圆距离（单位：米）
     */
    static double distance(double lon1, double lat1, double lon2, double lat2) {
        double a = Math.pow(Math.sin(Math.toRadians(lat2 - lat1) / 2), 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) * Math.pow(Math.sin(Math.toRadians(lon2 - lon1) / 2), 2);
        return 6371008.8 * 2 * Math.asin(Math.sqrt(Math.min(1, Math.max(0, a))));
    }

    /**
     * 记录穿戴设备维度的业务审计日志。
     *
     * @param action 操作动作
     * @param id 设备 ID
     */
    void auditDeviceAction(String action, Long id);

    /**
     * 释放指定长者所绑定的所有硬件设备与围栏成员关系（长者归档时调用）。
     *
     * @param elderId 长者档案 ID
     */
    void releaseElder(Long elderId);

    /**
     * 统计指定长者关联的历史绑定记录总数。
     *
     * @param elderId 长者档案 ID
     * @return 绑定记录条数
     */
    long bindingCountForElder(Long elderId);

    /**
     * 统计指定长者关联的历史定位轨迹点总数。
     *
     * @param elderId 长者档案 ID
     * @return 历史定位点条数
     */
    long locationCountForElder(Long elderId);

    /**
     * 统计指定长者已被纳入的电子围栏规则总数。
     *
     * @param elderId 长者档案 ID
     * @return 所在电子围栏成员数
     */
    long fenceMembershipCountForElder(Long elderId);

    /**
     * 分页查询智能硬件设备档案列表。
     *
     * @param page 页码
     * @param size 每页大小
     * @return 分页设备列表及绑定长者信息
     */
    Map<String, Object> devices(int page, int size);

    /**
     * 新增录入或修改更新智能硬件设备档案。
     *
     * @param id 设备主键 ID（新增时为 null）
     * @param b 请求参数 Map
     * @return 保存后的设备实体
     */
    Device saveDevice(Long id, Map<String, Object> b);

    /**
     * 为穿戴设备与长者建立佩戴绑定关系。
     *
     * @param deviceId 设备主键 ID
     * @param elderId 长者主键 ID
     * @return 建立的新绑定记录实体
     */
    Binding bind(Long deviceId, Long elderId);

    /**
     * 解除智能硬件设备当前的佩戴绑定关系。
     *
     * @param deviceId 设备主键 ID
     */
    void unbind(Long deviceId);

    /**
     * 统计穿戴设备的地理分布点位与实时联网在线状态。
     *
     * @param regionId 片区 ID（可选）
     * @param status 在线状态过滤（可选）
     * @return 设备地理分布列表
     */
    List<Map<String, Object>> distribution(Long regionId, String status);

    /**
     * 查询系统配置的全部电子安全围栏规则列表。
     *
     * @return 围栏规则列表及关联成员统计
     */
    List<Map<String, Object>> fences();

    /**
     * 新增或编辑更新电子安全围栏规则几何边界。
     *
     * @param id 围栏主键 ID（新增时为 null）
     * @param b 请求参数 Map
     * @return 保存后的围栏实体
     */
    Fence saveFence(Long id, Map<String, Object> b);

    /**
     * 覆盖配置指定电子安全围栏的关联监管长者成员名单。
     *
     * @param fenceId 围栏主键 ID
     * @param elderIds 长者主键 ID 列表
     */
    void setMembers(Long fenceId, List<Long> elderIds);

    /**
     * 分页多条件筛选电子围栏出入越界告警流水记录。
     *
     * @param elderId 长者 ID
     * @param handled 是否已处置
     * @param start 起始时间
     * @param end 截止时间
     * @param page 页码
     * @param size 每页大小
     * @return 分页围栏告警列表
     */
    Map<String, Object> alerts(Long elderId, Boolean handled, String start, String end, int page, int size);

    /**
     * 登记处置越界告警事件并完成闭环。
     *
     * @param id 告警流水主键 ID
     * @param b 包含 handleNotes 的请求参数 Map
     */
    void handle(Long id, Map<String, Object> b);

    /**
     * 查询指定长者在历史时段内的连续定位轨迹序列。
     *
     * @param elderId 长者档案 ID
     * @param start 起始时间
     * @param end 截止时间
     * @return 按采集时间正序排列的定位点列表
     */
    List<Location> trajectory(Long elderId, String start, String end);

    /**
     * 单个 GPS 定位点实时接入、排重与电子围栏进出界状态机驱动。
     *
     * @param b 单个定位点请求参数 Map
     * @return 写入或幂等确认后的定位点实体
     */
    Location ingest(Map<String, Object> b);

    /**
     * 批量定位轨迹点加密接入处理。
     *
     * @param b 包含 deviceId 与 points 列表的请求参数 Map
     * @return 处理结果摘要（写入条数、重复忽略数、触发的告警数）
     */
    Map<String, Object> ingestBatch(Map<String, Object> b);

    /**
     * 硬件设备网络心跳保活与状态事件接入上报。
     *
     * @param b 请求参数 Map
     * @return 更新后的设备实体
     */
    Device heartbeat(Map<String, Object> b);
}
