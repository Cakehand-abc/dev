package com.johnnylin.dev.controller;

import com.johnnylin.dev.common.*;
import com.johnnylin.dev.service.*;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.*;

/**
 * 智慧医养平台综合业务控制器。
 *
 * <p>提供智慧养老业务全流程 RESTful 接口，涵盖：
 * <ul>
 *   <li>行政网格区域字典</li>
 *   <li>长者档案管理（分页、详情、建档、更新、归档、删除）</li>
 *   <li>责任医生管理（分页列表、新增签约医生、更新信息）</li>
 *   <li>健康体征监测记录（分页查询、手工录入、体征指标历史趋势）</li>
 *   <li>随访计划执行（计划列表、发起计划、完成随访录入、取消计划）</li>
 *   <li>大数据统计大屏与指标驾驶舱（人口画像、体征分布、随访履约、综合驾驶舱）</li>
 *   <li>智能硬件设备管理（分页列表、注册入库、绑定/解绑长者、密钥轮转、地图分布）</li>
 *   <li>电子围栏与出入告警（围栏规则 CRUD、绑定成员维护、告警流水与闭环处置）</li>
 *   <li>长者历史活动轨迹查询</li>
 *   <li>系统运营管理用户账号管理与密码重置</li>
 * </ul>
 */
@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class CareController {

    private final CareService care;
    private final GeoService geo;
    private final StatisticsService stats;
    private final DeviceAccessService deviceAccess;

    /**
     * 获取系统全量行政区域网格列表。
     *
     * @return 包含所有区域列表的统一响应体
     */
    @GetMapping("/regions")
    public Api<?> regions() {
        return Api.ok(care.regions());
    }

    /**
     * 分页多条件筛选长者档案列表。
     *
     * @param regionId 所属区域ID（可选）
     * @param keyword 姓名或身份证号模糊检索关键字（可选）
     * @param status 长者状态（ACTIVE 在册 / ARCHIVED 归档，可选）
     * @param page 分页页码，从 1 开始，默认 1
     * @param size 每页记录条数，默认 20
     * @return 分页结果对象，包含总条数、页码与长者列表
     */
    @GetMapping("/elders")
    public Api<?> elders(
            @RequestParam(required = false) Long regionId,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        return Api.ok(care.elders(regionId, keyword, status, page, size));
    }

    /**
     * 根据长者唯一主键查询档案详情。
     *
     * @param id 长者主键ID
     * @return 包含长者完整个人资料、责任医生与区域名称的统一响应体
     */
    @GetMapping("/elders/{id}")
    public Api<?> elder(@PathVariable Long id) {
        return Api.ok(care.elder(id));
    }

    /**
     * 新建登记长者档案资料。
     *
     * @param b 请求载荷 Map，包含 name、idCard、gender、phone、address、regionId、doctorId 等字段
     * @return 新建成功的长者档案详情
     */
    @PostMapping("/elders")
    public Api<?> elder(@RequestBody Map<String, Object> b) {
        return Api.ok(care.saveElder(null, b));
    }

    /**
     * 修改更新已有长者档案信息。
     *
     * @param id 待修改的长者主键ID
     * @param b 请求载荷 Map，包含更新后的长者字段信息
     * @return 更新后的长者档案详情
     */
    @PutMapping("/elders/{id}")
    public Api<?> elder(@PathVariable Long id, @RequestBody Map<String, Object> b) {
        return Api.ok(care.saveElder(id, b));
    }

    /**
     * 将指定长者档案变更为归档状态（软离册）。
     *
     * @param id 待归档的长者主键ID
     * @return 空操作结果响应体
     */
    @PostMapping("/elders/{id}/archive")
    public Api<?> archive(@PathVariable Long id) {
        care.archive(id);
        return Api.ok(null);
    }

    /**
     * 物理删除长者档案及其关联数据。
     *
     * @param id 待删除的长者主键ID
     * @return 空操作结果响应体
     */
    @DeleteMapping("/elders/{id}")
    public Api<?> deleteElder(@PathVariable Long id) {
        care.deleteElder(id);
        return Api.ok(null);
    }

    /**
     * 分页查询签约责任医生列表。
     *
     * @param page 分页页码，默认 1
     * @param size 每页记录条数，默认 20
     * @return 分页医生列表数据
     */
    @GetMapping("/doctors")
    public Api<?> doctors(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        return Api.ok(care.doctors(page, size));
    }

    /**
     * 新建签约责任医生信息。
     *
     * @param b 请求载荷 Map，包含 name, phone, title, hospital 等字段
     * @return 新建成功的医生信息
     */
    @PostMapping("/doctors")
    public Api<?> doctor(@RequestBody Map<String, Object> b) {
        return Api.ok(care.saveDoctor(null, b));
    }

    /**
     * 修改更新已有签约责任医生信息。
     *
     * @param id 医生主键ID
     * @param b 请求载荷 Map
     * @return 更新后的医生信息
     */
    @PutMapping("/doctors/{id}")
    public Api<?> doctor(@PathVariable Long id, @RequestBody Map<String, Object> b) {
        return Api.ok(care.saveDoctor(id, b));
    }

    /**
     * 分页多维度检索健康体征监测记录。
     *
     * @param regionId 行政网格区域ID过滤（可选）
     * @param elderId 长者ID过滤（可选）
     * @param start 采集起始时间（yyyy-MM-dd 或 ISO-8601，可选）
     * @param end 采集截止时间（yyyy-MM-dd 或 ISO-8601，可选）
     * @param abnormal 是否仅查询异常指标数据（可选）
     * @param page 分页页码，默认 1
     * @param size 每页记录条数，默认 20
     * @return 分页体征记录列表
     */
    @GetMapping("/health-records")
    public Api<?> health(
            @RequestParam(required = false) Long regionId,
            @RequestParam(required = false) Long elderId,
            @RequestParam(required = false) String start,
            @RequestParam(required = false) String end,
            @RequestParam(required = false) Boolean abnormal,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        return Api.ok(care.health(regionId, elderId, start, end, abnormal, page, size));
    }

    /**
     * 手工补录或设备上报长者单次健康体征数据。
     *
     * @param b 请求载荷 Map，包含 elderId、sbp、dbp、heartRate、temperature、bloodSugar 等
     * @return 评估并持久化后的健康体征记录（含 isAbnormal 与 abnormalReason）
     */
    @PostMapping("/health-records")
    public Api<?> health(@RequestBody Map<String, Object> b) {
        return Api.ok(care.addHealth(b));
    }

    /**
     * 查询指定长者在特定时间区间内的单项生理体征历史趋势序列。
     *
     * @param id 长者主键ID
     * @param metric 待查询指标项（如 sbp, dbp, heartRate, temperature, bloodSugar）
     * @param start 起始时间范围
     * @param end 截止时间范围
     * @return 包含时间点列表与数值列表的时间序列趋势数据结构
     */
    @GetMapping("/elders/{id}/health-trend")
    public Api<?> trend(
            @PathVariable Long id,
            @RequestParam String metric,
            @RequestParam String start,
            @RequestParam String end) {
        return Api.ok(care.trend(id, metric, start, end));
    }

    /**
     * 分页多条件筛选医生随访服务计划。
     *
     * @param regionId 区域ID（可选）
     * @param elderId 长者ID（可选）
     * @param doctorId 责任医生ID（可选）
     * @param start 计划起始日期（可选）
     * @param end 计划截止日期（可选）
     * @param status 随访状态（PENDING/COMPLETED/CANCELED，可选）
     * @param page 分页页码，默认 1
     * @param size 每页记录条数，默认 20
     * @return 分页随访计划列表
     */
    @GetMapping("/followup-plans")
    public Api<?> plans(
            @RequestParam(required = false) Long regionId,
            @RequestParam(required = false) Long elderId,
            @RequestParam(required = false) Long doctorId,
            @RequestParam(required = false) String start,
            @RequestParam(required = false) String end,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        return Api.ok(care.plans(regionId, elderId, doctorId, start, end, status, page, size));
    }

    /**
     * 新建制定一条长者随访服务计划。
     *
     * @param b 请求载荷 Map，包含 elderId, doctorId, planDate, notes 等
     * @return 新建的随访计划记录
     */
    @PostMapping("/followup-plans")
    public Api<?> plan(@RequestBody Map<String, Object> b) {
        return Api.ok(care.addPlan(b));
    }

    /**
     * 标记完成随访计划并录入执行记录与医生评估意见。
     *
     * @param id 待完成的随访计划ID
     * @param b 请求载荷 Map，包含 followupDate, findings, doctorAdvice, sbp, dbp, heartRate 等
     * @return 伴生生成的随访履约明细记录
     */
    @PostMapping("/followup-plans/{id}/complete")
    public Api<?> complete(@PathVariable Long id, @RequestBody Map<String, Object> b) {
        return Api.ok(care.complete(id, b));
    }

    /**
     * 取消已有随访计划并登记取消原因。
     *
     * @param id 待取消的随访计划ID
     * @param b 请求载荷 Map，包含 cancelReason
     * @return 空操作结果响应体
     */
    @PostMapping("/followup-plans/{id}/cancel")
    public Api<?> cancel(@PathVariable Long id, @RequestBody Map<String, Object> b) {
        care.cancel(id, b);
        return Api.ok(null);
    }

    /**
     * 统计老人人口学画像数据。
     *
     * @param regionId 区域ID过滤（可选）
     * @return 包含在册总数、高龄占比、慢性病病种分布等画像聚合指标
     */
    @GetMapping("/statistics/population")
    public Api<?> population(@RequestParam(required = false) Long regionId) {
        return Api.ok(stats.population(regionId));
    }

    /**
     * 统计指定时间与区域内的体征异常与健康监测指标。
     *
     * @param regionId 区域ID过滤（可选）
     * @param start 起始统计时间（可选）
     * @param end 截止统计时间（可选）
     * @return 健康监测多维统计指标
     */
    @GetMapping("/statistics/health")
    public Api<?> healthStats(
            @RequestParam(required = false) Long regionId,
            @RequestParam(required = false) String start,
            @RequestParam(required = false) String end) {
        return Api.ok(stats.health(regionId, start, end));
    }

    /**
     * 统计随访履约履约率、及时率与责任医生工作量。
     *
     * @param regionId 区域ID过滤（可选）
     * @param doctorId 医生ID过滤（可选）
     * @param start 起始统计时间（可选）
     * @param end 截止统计时间（可选）
     * @return 随访履约统计报表数据
     */
    @GetMapping("/statistics/followups")
    public Api<?> followStats(
            @RequestParam(required = false) Long regionId,
            @RequestParam(required = false) Long doctorId,
            @RequestParam(required = false) String start,
            @RequestParam(required = false) String end) {
        return Api.ok(stats.followups(regionId, doctorId, start, end));
    }

    /**
     * 获取管理大屏驾驶舱（Dashboard）综合看板全景数据。
     *
     * @param regionId 区域ID过滤（可选）
     * @param start 起始统计时间（可选）
     * @param end 截止统计时间（可选）
     * @return 包含人口、健康、告警、随访等多模块的聚合综合看板
     */
    @GetMapping("/dashboard")
    public Api<?> dashboard(
            @RequestParam(required = false) Long regionId,
            @RequestParam(required = false) String start,
            @RequestParam(required = false) String end) {
        return Api.ok(stats.dashboard(regionId, start, end));
    }

    /**
     * 分页查询智能硬件设备档案列表。
     *
     * @param page 分页页码，默认 1
     * @param size 每页记录条数，默认 20
     * @return 分页设备列表，含设备编号、型号、通信协议与绑定状态
     */
    @GetMapping("/devices")
    public Api<?> devices(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        return Api.ok(geo.devices(page, size));
    }

    /**
     * 注册录入全新智能硬件设备档案。
     *
     * @param b 请求载荷 Map，包含 deviceSn, deviceType, model, vendor, phone 等
     * @return 录入成功的新设备详情
     */
    @PostMapping("/devices")
    public Api<?> device(@RequestBody Map<String, Object> b) {
        return Api.ok(geo.saveDevice(null, b));
    }

    /**
     * 修改更新已有智能硬件设备档案信息。
     *
     * @param id 待修改的设备主键ID
     * @param b 请求载荷 Map
     * @return 更新后的设备详情
     */
    @PutMapping("/devices/{id}")
    public Api<?> device(@PathVariable Long id, @RequestBody Map<String, Object> b) {
        return Api.ok(geo.saveDevice(id, b));
    }

    /**
     * 将指定硬件设备绑定到特定长者身上。
     *
     * @param id 设备主键ID
     * @param b 请求载荷 Map，必须包含 elderId 字段
     * @return 建立的新绑定记录实体
     */
    @PostMapping("/devices/{id}/bind")
    public Api<?> bind(@PathVariable Long id, @RequestBody Map<String, Object> b) {
        Input.keys(b, "elderId");
        return Api.ok(geo.bind(id, Input.id(b, "elderId")));
    }

    /**
     * 解除指定硬件设备当前的佩戴绑定关系。
     *
     * @param id 设备主键ID
     * @return 空操作结果响应体
     */
    @PostMapping("/devices/{id}/unbind")
    public Api<?> unbind(@PathVariable Long id) {
        geo.unbind(id);
        return Api.ok(null);
    }

    /**
     * 为指定设备轮转生成新凭证密钥，旧密钥立即失效。
     *
     * @param id 设备主键ID
     * @return 包含明文原始 API Key 与脱敏摘要的凭证对象
     */
    @PostMapping("/devices/{id}/credential")
    public Api<?> credential(@PathVariable Long id) {
        return Api.ok(deviceAccess.rotate(id));
    }

    /**
     * 查询智能设备的地理分布与实时在线/离线聚合态势。
     *
     * @param regionId 区域ID过滤（可选）
     * @param status 设备状态过滤（ONLINE 在线 / OFFLINE 离线，可选）
     * @return 包含各设备最新定位点、佩戴长者与运行状态的分布列表
     */
    @GetMapping("/devices/distribution")
    public Api<?> distribution(
            @RequestParam(required = false) Long regionId,
            @RequestParam(required = false) String status) {
        return Api.ok(geo.distribution(regionId, status));
    }

    /**
     * 查询系统已配置的全部地理电子围栏规则列表。
     *
     * @return 电子围栏规则列表，包含中心点、半径、告警触发类型及成员数量
     */
    @GetMapping("/geofences")
    public Api<?> fences() {
        return Api.ok(geo.fences());
    }

    /**
     * 新建定义一条地理电子围栏规则。
     *
     * @param b 请求载荷 Map，包含 name, centerLng, centerLat, radiusMeters, alertOnExit, alertOnEnter 等
     * @return 创建成功的电子围栏详情
     */
    @PostMapping("/geofences")
    public Api<?> fence(@RequestBody Map<String, Object> b) {
        return Api.ok(geo.saveFence(null, b));
    }

    /**
     * 修改更新已有电子围栏的边界几何参数或触发策略。
     *
     * @param id 待修改的围栏主键ID
     * @param b 请求载荷 Map
     * @return 更新后的电子围栏详情
     */
    @PutMapping("/geofences/{id}")
    public Api<?> fence(@PathVariable Long id, @RequestBody Map<String, Object> b) {
        return Api.ok(geo.saveFence(id, b));
    }

    /**
     * 电子围栏生效成员批量变更请求体。
     *
     * @param elderIds 纳入此围栏管控的长者主键ID列表
     */
    public record MemberRequest(List<Long> elderIds) {}

    /**
     * 覆盖配置指定电子围栏关联的长者成员名单。
     *
     * @param id 电子围栏主键ID
     * @param b 包含长者ID列表的请求体
     * @return 空操作结果响应体
     */
    @PutMapping("/geofences/{id}/members")
    public Api<?> members(@PathVariable Long id, @RequestBody MemberRequest b) {
        geo.setMembers(id, b.elderIds());
        return Api.ok(null);
    }

    /**
     * 分页查询地理围栏越界与非法侵入告警流水记录。
     *
     * @param elderId 长者ID过滤（可选）
     * @param handled 闭环处置状态过滤（true 已处置 / false 待处置，可选）
     * @param start 触发起始时间（可选）
     * @param end 触发截止时间（可选）
     * @param page 分页页码，默认 1
     * @param size 每页记录条数，默认 20
     * @return 分页围栏告警列表
     */
    @GetMapping("/geofence-alerts")
    public Api<?> alerts(
            @RequestParam(required = false) Long elderId,
            @RequestParam(required = false) Boolean handled,
            @RequestParam(required = false) String start,
            @RequestParam(required = false) String end,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        return Api.ok(geo.alerts(elderId, handled, start, end, page, size));
    }

    /**
     * 登记处置围栏告警事件并使其闭环。
     *
     * @param id 告警记录主键ID
     * @param b 请求载荷 Map，包含 handleNotes（处置说明）
     * @return 空操作结果响应体
     */
    @PostMapping("/geofence-alerts/{id}/handle")
    public Api<?> handle(@PathVariable Long id, @RequestBody Map<String, Object> b) {
        geo.handle(id, b);
        return Api.ok(null);
    }

    /**
     * 查询指定长者在历史时段内的连续定位轨迹点集合。
     *
     * @param id 长者主键ID
     * @param start 起始时间（ISO-8601 或 yyyy-MM-dd HH:mm:ss）
     * @param end 截止时间（ISO-8601 或 yyyy-MM-dd HH:mm:ss）
     * @return 按采集时间正序排列的经纬度与速度、精度轨迹列表
     */
    @GetMapping("/elders/{id}/trajectory")
    public Api<?> trajectory(
            @PathVariable Long id,
            @RequestParam String start,
            @RequestParam String end) {
        return Api.ok(geo.trajectory(id, start, end));
    }

    /**
     * 分页查询系统运营管理人员用户账号列表。
     *
     * @param page 分页页码，默认 1
     * @param size 每页记录条数，默认 20
     * @return 分页用户列表（已脱敏密码）
     */
    @GetMapping("/users")
    public Api<?> users(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        return Api.ok(care.users(page, size));
    }

    /**
     * 新建注册运营管理用户账号。
     *
     * @param b 请求载荷 Map，包含 username, password, role, realName 等
     * @return 创建成功的用户详情（脱敏哈希）
     */
    @PostMapping("/users")
    public Api<?> user(@RequestBody Map<String, Object> b) {
        return Api.ok(care.saveUser(null, b));
    }

    /**
     * 修改更新已有运营管理用户资料（如角色、状态、姓名）。
     *
     * @param id 待修改的用户主键ID
     * @param b 请求载荷 Map
     * @return 更新后的用户详情
     */
    @PutMapping("/users/{id}")
    public Api<?> user(@PathVariable Long id, @RequestBody Map<String, Object> b) {
        return Api.ok(care.saveUser(id, b));
    }

    /**
     * 运营管理员强制重置指定用户的登录密码。
     *
     * @param id 目标用户主键ID
     * @param b 请求载荷 Map，包含 newPassword 字段
     * @return 空操作结果响应体
     */
    @PostMapping("/users/{id}/reset-password")
    public Api<?> reset(@PathVariable Long id, @RequestBody Map<String, Object> b) {
        care.resetPassword(id, b);
        return Api.ok(null);
    }
}
