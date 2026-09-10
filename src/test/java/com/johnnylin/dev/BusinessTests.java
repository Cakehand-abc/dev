package com.johnnylin.dev;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.johnnylin.dev.auth.AccountPrincipal;
import com.johnnylin.dev.common.Api;
import com.johnnylin.dev.common.Input;
import com.johnnylin.dev.domain.Alert;
import com.johnnylin.dev.domain.Binding;
import com.johnnylin.dev.domain.Device;
import com.johnnylin.dev.domain.Doctor;
import com.johnnylin.dev.domain.Elder;
import com.johnnylin.dev.domain.Fence;
import com.johnnylin.dev.domain.HeartbeatEvent;
import com.johnnylin.dev.domain.Location;
import com.johnnylin.dev.domain.Region;
import com.johnnylin.dev.domain.User;
import com.johnnylin.dev.mapper.AlertMapper;
import com.johnnylin.dev.mapper.BindingMapper;
import com.johnnylin.dev.mapper.DeviceMapper;
import com.johnnylin.dev.mapper.ElderMapper;
import com.johnnylin.dev.mapper.HeartbeatEventMapper;
import com.johnnylin.dev.mapper.LocationMapper;
import com.johnnylin.dev.mapper.UserMapper;
import com.johnnylin.dev.service.CareService;
import com.johnnylin.dev.service.DeviceAccessService;
import com.johnnylin.dev.service.GeoService;
import com.johnnylin.dev.service.StatisticsService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.*;
import java.util.*;

import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static com.johnnylin.dev.common.Input.*;

/**
 * 智慧医养平台业务核心全链路集成测试套件。
 *
 * <p>涵盖以下核心业务链路与安全边界校验：
 * <ul>
 *   <li>身份认证、会话保持、CSRF 凭证校验与多端登出</li>
 *   <li>基于角色的访问控制（RBAC）、只读角色拦截与动态提权防御</li>
 *   <li>长者档案并发控制（乐观锁版本号）与安全删除策略</li>
 *   <li>健康体征采集、范围校验与去重多维统计</li>
 *   <li>随访履约状态机、幂等防重入与履约率算法</li>
 *   <li>智能硬件生命周期（绑定/解绑、历史轨迹溯源归属、时间戳单调递增心跳保活）</li>
 *   <li>电子围栏进出界状态机、告警防抖去重与消警闭环</li>
 *   <li>设备端 SHA-256 访问密钥鉴权与动态轮转网关</li>
 *   <li>Haversine 球面大圆距离测距算法精度基准</li>
 * </ul>
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class BusinessTests {

    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;
    @Autowired CareService care;
    @Autowired GeoService geo;
    @Autowired StatisticsService stats;
    @Autowired DeviceAccessService deviceAccess;

    @Autowired ElderMapper elders;
    @Autowired UserMapper users;
    @Autowired BindingMapper bindings;
    @Autowired AlertMapper alerts;
    @Autowired LocationMapper locations;
    @Autowired DeviceMapper devices;
    @Autowired HeartbeatEventMapper heartbeatEvents;

    Elder person;
    Doctor doctor;
    Device device;
    AccountPrincipal admin;
    String t;

    /**
     * 初始化每个测试用例所需的上下文基础测试固件。
     *
     * <p>准备超级管理员身份、测试老人、测试医生、测试定位设备以及 3 天前的绑定关系，
     * 设定基准时间戳。
     */
    @BeforeEach
    void fixture() {
        admin = AccountPrincipal.of(users.selectOne(new QueryWrapper<User>().eq("username", "admin")));
        person = care.saveElder(null, new HashMap<>(Map.of("code", "TEST-E", "name", "测试老人", "gender", "UNKNOWN", "regionId", care.regions().get(0).getId())));
        doctor = care.saveDoctor(null, Map.of("code", "TEST-D", "name", "测试医生", "department", "全科", "enabled", true));
        device = geo.saveDevice(null, Map.of("serialNo", "TEST-W", "model", "测试设备", "enabled", true));
        var binding = geo.bind(device.getId(), person.getId());
        bindings.update(null, new UpdateWrapper<Binding>().eq("id", binding.getId()).set("bound_at", now().minusDays(3)));
        t = now().minusMinutes(30).atOffset(ZoneOffset.ofHours(8)).toString();
    }

    /**
     * 构建单个定位点上报测试参数载荷。
     *
     * @param event 客户端事件唯一流水号
     * @param lon 经度数值
     * @param at 点位采集时间
     * @return 构造完成的定位点 Map 参数
     */
    Map<String, Object> point(String event, double lon, LocalDateTime at) {
        return Map.of(
                "deviceId", device.getId(),
                "eventId", event,
                "longitude", lon,
                "latitude", 30.25,
                "recordedAt", at.atOffset(ZoneOffset.ofHours(8)).toString()
        );
    }

    /**
     * 创建一个默认的圆形电子围栏并绑定当前测试长者。
     *
     * @return 持久化后的电子围栏实例
     */
    Fence fence() {
        var f = geo.saveFence(null, Map.of("name", "测试围栏", "centerLon", 120.16, "centerLat", 30.25, "radiusM", 500, "enabled", true));
        geo.setMembers(f.getId(), List.of(person.getId()));
        return f;
    }

    /**
     * 测试匿名访问受保护业务接口被安全拦截。
     *
     * @throws Exception MockMvc 执行异常
     */
    @Test
    void anonymousCannotReadData() throws Exception {
        mvc.perform(get("/api/v1/elders")).andExpect(status().isUnauthorized());
    }

    /**
     * 测试真实用户名密码通过 CSRF 校验登录成功并在退出后使 Session 彻底失效。
     *
     * @throws Exception MockMvc 执行异常
     */
    @Test
    void realLoginAndLogoutInvalidateSession() throws Exception {
        var tokenResult = mvc.perform(get("/api/v1/auth/csrf")).andExpect(status().isOk()).andReturn();
        var token = json.readTree(tokenResult.getResponse().getContentAsString()).get("data");
        var session = (MockHttpSession) tokenResult.getRequest().getSession();
        mvc.perform(post("/api/v1/auth/login")
                .session(session)
                .header(token.get("headerName").asText(), token.get("token").asText())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"username\":\"admin\",\"password\":\"TestAdmin!2026\"}"))
           .andExpect(status().isOk())
           .andExpect(jsonPath("$.data.role").value("ADMIN"))
           .andExpect(jsonPath("$.data.password").doesNotExist());
        mvc.perform(get("/api/v1/auth/me").session(session)).andExpect(status().isOk());
        mvc.perform(post("/api/v1/auth/logout").session(session).with(csrf())).andExpect(status().isOk());
        assertThat(session.isInvalid()).isTrue();
    }

    /**
     * 测试使用错误密码登录被拒绝并返回 401 状态码。
     *
     * @throws Exception MockMvc 执行异常
     */
    @Test
    void invalidPasswordIsRejected() throws Exception {
        mvc.perform(post("/api/v1/auth/login")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"username\":\"admin\",\"password\":\"incorrect\"}"))
           .andExpect(status().isUnauthorized());
    }

    /**
     * 测试系统对 CSRF 令牌及只读分析员角色的写操作拦截。
     *
     * @throws Exception MockMvc 执行异常
     */
    @Test
    void csrfAndReadOnlyRoleAreEnforced() throws Exception {
        mvc.perform(post("/api/v1/elders").with(user(admin)).contentType(MediaType.APPLICATION_JSON).content("{}")).andExpect(status().isForbidden());
        var analyst = care.saveUser(null, Map.of("username", "readonly", "displayName", "分析", "role", "ANALYST", "enabled", true, "password", "ReadOnly!2026"));
        mvc.perform(post("/api/v1/elders").with(user(AccountPrincipal.of(analyst))).with(csrf()).contentType(MediaType.APPLICATION_JSON).content("{}")).andExpect(status().isForbidden());
        mvc.perform(get("/api/v1/elders").with(user(AccountPrincipal.of(analyst)))).andExpect(status().isOk());
    }

    /**
     * 测试用户权限角色在后端变更后，旧会话凭据在后续请求中失效。
     *
     * @throws Exception MockMvc 执行异常
     */
    @Test
    void roleChangeInvalidatesExistingAuthentication() throws Exception {
        var u = care.saveUser(null, Map.of("username", "op", "displayName", "业务", "role", "OPERATOR", "enabled", true, "password", "Operator!2026"));
        var old = AccountPrincipal.of(u);
        care.saveUser(u.getId(), Map.of("displayName", "业务", "role", "ANALYST", "enabled", true));
        mvc.perform(get("/api/v1/elders").with(user(old))).andExpect(status().isUnauthorized());
    }

    /**
     * 测试系统中唯一最后一名管理员账号禁止被停用或降权。
     */
    @Test
    void lastAdministratorCannotBeDisabled() {
        assertThatThrownBy(() -> care.saveUser(admin.id(), Map.of("displayName", "管理员", "role", "ADMIN", "enabled", false)))
                .isInstanceOf(Api.Failure.class)
                .hasMessageContaining("最后");
    }

    /**
     * 测试非法超长分页参数及未经授权的字段注入（Mass Assignment）被严格拦截。
     *
     * @throws Exception MockMvc 执行异常
     */
    @Test
    void paginationAndMassAssignmentAreRejected() throws Exception {
        mvc.perform(get("/api/v1/elders?size=101").with(user(admin))).andExpect(status().isBadRequest());
        mvc.perform(post("/api/v1/elders").with(user(admin)).with(csrf()).contentType(MediaType.APPLICATION_JSON).content("{\"id\":99}")).andExpect(status().isBadRequest());
    }

    /**
     * 测试人口画像统计包含年龄未知分类，且归档长者被自动排除在在册统计之外。
     */
    @Test
    void populationIncludesUnknownAndExcludesArchived() {
        var population = stats.population(null);
        assertThat(((Number) population.get("total")).longValue()).isEqualTo(1);
        assertThat(population.get("age").toString()).contains("年龄未知");
        care.archive(person.getId());
        assertThat(((Number) stats.population(null).get("total")).longValue()).isZero();
    }

    /**
     * 测试长者档案更新时版本号不匹配引发乐观锁并发冲突拦截。
     */
    @Test
    void staleElderUpdateIsRejected() {
        assertThatThrownBy(() -> care.saveElder(person.getId(), Map.of("version", 4)))
                .isInstanceOf(Api.Failure.class)
                .hasMessageContaining("刷新");
    }

    /**
     * 测试健康体征统计能够准确区分测量总次数、去重受检人数与异常人数。
     */
    @Test
    void healthCountsPeopleAndRecordsSeparately() {
        care.addHealth(Map.of("elderId", person.getId(), "measuredAt", t, "systolic", 160));
        care.addHealth(Map.of("elderId", person.getId(), "measuredAt", t, "heartRate", 70));
        @SuppressWarnings("unchecked")
        var s = (Map<String, Object>) stats.health(null, null, null).get("summary");
        assertThat(((Number) s.get("measurements")).longValue()).isEqualTo(2);
        assertThat(((Number) s.get("people")).longValue()).isEqualTo(1);
        assertThat(((Number) s.get("abnormalPeople")).longValue()).isEqualTo(1);
    }

    /**
     * 测试体征录入缺失有效指标或采集时间为未来时间被拒绝。
     */
    @Test
    void healthMissingMetricsAndFutureDataAreRejected() {
        assertThatThrownBy(() -> care.addHealth(Map.of("elderId", person.getId(), "measuredAt", t)))
                .isInstanceOf(Api.Failure.class);
        assertThatThrownBy(() -> care.addHealth(Map.of("elderId", person.getId(), "measuredAt", now().plusDays(1).atOffset(ZoneOffset.ofHours(8)).toString(), "oxygen", 99)))
                .isInstanceOf(Api.Failure.class);
    }

    /**
     * 测试随访计划完成操作的原子性与重复提交防护，并校验履约率统计。
     */
    @Test
    void completionIsAtomicAndCannotBeRepeated() {
        var p = care.addPlan(Map.of("elderId", person.getId(), "doctorId", doctor.getId(), "dueAt", t));
        var b = Map.<String, Object>of("completedAt", t, "content", "测试随访", "result", "已完成");
        care.complete(p.getId(), b);
        assertThatThrownBy(() -> care.complete(p.getId(), b)).isInstanceOf(Api.Failure.class);
        @SuppressWarnings("unchecked")
        var s = (Map<String, Object>) stats.followups(null, null, null, null).get("summary");
        assertThat(s.get("completionRate")).isEqualTo(100.0);
    }

    /**
     * 测试无随访计划记录时履约率返回 null 而非虚假的计算数值。
     */
    @Test
    void noPlansHaveNoArtificialCompletionRate() {
        @SuppressWarnings("unchecked")
        var s = (Map<String, Object>) stats.followups(null, null, null, null).get("summary");
        assertThat(s.get("completionRate")).isNull();
    }

    /**
     * 测试重复将同一设备绑定到已有长者时被系统拒绝。
     */
    @Test
    void duplicateBindingIsRejected() {
        assertThatThrownBy(() -> geo.bind(device.getId(), person.getId())).isInstanceOf(Api.Failure.class);
    }

    /**
     * 测试长者持续在围栏外部活动只产生一条告警，返还入界闭环后再次出界生成新告警。
     */
    @Test
    void persistentOutsideOnlyCreatesOneAlertAndReturnAllowsAnother() {
        fence();
        var base = now().minusHours(1);
        geo.ingest(point("a", 120.17, base));
        geo.ingest(point("b", 120.171, base.plusMinutes(1)));
        assertThat(alerts.selectCount(null)).isEqualTo(1);
        geo.ingest(point("c", 120.16, base.plusMinutes(2)));
        assertThat(alerts.selectList(null).get(0).getReturnedAt()).isNotNull();
        geo.ingest(point("d", 120.17, base.plusMinutes(3)));
        assertThat(alerts.selectCount(null)).isEqualTo(2);
    }

    /**
     * 测试重复事件与乱序定位点不会破坏实时越界监控与告警状态机。
     */
    @Test
    void duplicateAndOutOfOrderPointsDoNotCorruptMonitoring() {
        fence();
        var base = now().minusHours(1);
        var b = point("new", 120.17, base.plusMinutes(2));
        geo.ingest(b);
        geo.ingest(b);
        geo.ingest(point("old", 120.16, base));
        assertThat(locations.selectCount(null)).isEqualTo(2);
        assertThat(alerts.selectList(null).get(0).getReturnedAt()).isNull();
        assertThatThrownBy(() -> geo.ingest(point("new", 120.16, base.plusMinutes(2)))).isInstanceOf(Api.Failure.class);
    }

    /**
     * 测试历史轨迹查询结果严格按时间正序排序且查询跨度受到最大区间约束。
     */
    @Test
    void trajectoryIsSortedAndRestricted() {
        var base = now().minusHours(1);
        geo.ingest(point("late", 120.16, base.plusMinutes(2)));
        geo.ingest(point("early", 120.16, base));
        var list = geo.trajectory(person.getId(), base.minusSeconds(1).atOffset(ZoneOffset.ofHours(8)).toString(), base.plusHours(1).atOffset(ZoneOffset.ofHours(8)).toString());
        assertThat(list).extracting(Location::getEventId).containsExactly("early", "late");
        assertThatThrownBy(() -> geo.trajectory(person.getId(), now().minusDays(2).atOffset(ZoneOffset.ofHours(8)).toString(), now().atOffset(ZoneOffset.ofHours(8)).toString()))
                .isInstanceOf(Api.Failure.class);
    }

    /**
     * 测试设备重绑长者后，历史轨迹点的长者归属依然保留历史所有者不变。
     */
    @Test
    void rebindingPreservesHistoricalOwner() {
        var old = now().minusHours(1);
        geo.unbind(device.getId());
        var second = care.saveElder(null, Map.of("code", "SECOND", "name", "另一老人", "gender", "FEMALE", "regionId", person.getRegionId()));
        geo.bind(device.getId(), second.getId());
        assertThat(geo.ingest(point("historical", 120.16, old)).getElderId()).isEqualTo(person.getId());
        assertThat(geo.distribution(null, null).get(0).get("location")).isNull();
    }

    /**
     * 测试硬件心跳保活时间戳单调递增，延迟到达的陈旧心跳不得倒退在线时间。
     */
    @Test
    void heartbeatDoesNotMoveBackwards() {
        geo.heartbeat(Map.of("deviceId", device.getId(), "eventId", "h1", "recordedAt", t));
        geo.heartbeat(Map.of("deviceId", device.getId(), "eventId", "h0", "recordedAt", now().minusHours(2).atOffset(ZoneOffset.ofHours(8)).toString()));
        assertThat(devices.selectById(device.getId()).getLastSeenAt()).isEqualTo(time(t));
    }

    /**
     * 测试心跳事件具有持久化幂等性，重复事件号不会重复插入记录。
     */
    @Test
    void heartbeatEventsAreDurablyIdempotent() {
        var body = Map.<String, Object>of("deviceId", device.getId(), "eventId", "same-heartbeat", "recordedAt", t);
        geo.heartbeat(body);
        geo.heartbeat(body);
        assertThat(heartbeatEvents.selectCount(null)).isEqualTo(1);
        assertThatThrownBy(() -> geo.heartbeat(Map.of("deviceId", device.getId(), "eventId", "same-heartbeat", "recordedAt", now().minusHours(2).atOffset(ZoneOffset.ofHours(8)).toString())))
                .isInstanceOf(Api.Failure.class);
    }

    /**
     * 测试批量定位上报吞吐处理，包含有效点写入、重复点去重以及非法坐标拒绝。
     */
    @Test
    void batchLocationIngestionAcceptsMultiplePoints() {
        var base = now().minusHours(1);
        var body = Map.<String, Object>of("deviceId", device.getId(), "points", List.of(
                Map.of("eventId", "batch-1", "longitude", 120.16, "latitude", 30.25, "recordedAt", base.atOffset(ZoneOffset.ofHours(8)).toString()),
                Map.of("eventId", "batch-2", "longitude", 120.161, "latitude", 30.251, "recordedAt", base.plusMinutes(1).atOffset(ZoneOffset.ofHours(8)).toString())
        ));
        var result = geo.ingestBatch(body);
        assertThat(result.get("accepted")).isEqualTo(2);
        assertThat(locations.selectCount(null)).isEqualTo(2);

        var repeated = geo.ingestBatch(body);
        assertThat(repeated.get("duplicates")).isEqualTo(2);

        var invalid = Map.<String, Object>of("deviceId", device.getId(), "points", List.of(
                Map.of("eventId", "batch-3", "longitude", 120.162, "latitude", 30.252, "recordedAt", base.plusMinutes(2).atOffset(ZoneOffset.ofHours(8)).toString()),
                Map.of("eventId", "batch-invalid", "longitude", 999, "latitude", 30.25, "recordedAt", base.plusMinutes(3).atOffset(ZoneOffset.ofHours(8)).toString())
        ));
        assertThatThrownBy(() -> geo.ingestBatch(invalid)).isInstanceOf(Api.Failure.class);
        assertThat(locations.selectCount(null)).isEqualTo(2);
    }

    /**
     * 测试受保护删除规则，仅允许删除无历史关联业务数据的空白档案。
     */
    @Test
    void restrictedDeleteOnlyRemovesUnusedRecords() {
        var unused = care.saveElder(null, Map.of("code", "UNUSED", "name", "误录老人", "gender", "UNKNOWN", "regionId", person.getRegionId()));
        care.deleteElder(unused.getId());
        assertThat(elders.selectById(unused.getId())).isNull();
        assertThatThrownBy(() -> care.deleteElder(person.getId()))
                .isInstanceOf(Api.Failure.class)
                .hasMessageContaining("只能归档");
    }

    /**
     * 测试只有具备 ADMIN 角色的超级管理员有权删除长者档案，业务操作员无权删除。
     *
     * @throws Exception MockMvc 执行异常
     */
    @Test
    void onlyAdministratorCanDeleteElder() throws Exception {
        var unused = care.saveElder(null, Map.of("code", "DELETE-AUTH", "name", "权限测试", "gender", "UNKNOWN", "regionId", person.getRegionId()));
        var operator = care.saveUser(null, Map.of("username", "delete-op", "displayName", "业务", "role", "OPERATOR", "enabled", true, "password", "DeleteTest!2026"));
        mvc.perform(delete("/api/v1/elders/" + unused.getId()).with(user(AccountPrincipal.of(operator))).with(csrf())).andExpect(status().isForbidden());
        mvc.perform(delete("/api/v1/elders/" + unused.getId()).with(user(admin)).with(csrf())).andExpect(status().isOk());
    }

    /**
     * 测试设备鉴权接入端点校验 API 密钥，拒绝缺省密钥并在轮转后使旧密钥即刻失效。
     *
     * @throws Exception MockMvc 执行异常
     */
    @Test
    void authenticatedDeviceEndpointAcceptsKeyAndRejectsMissingKey() throws Exception {
        var credential = deviceAccess.rotate(device.getId());
        var base = now().minusHours(1);
        String body = json.writeValueAsString(Map.of("deviceId", device.getId(), "points", List.of(Map.of("eventId", "real-1", "longitude", 120.16, "latitude", 30.25, "recordedAt", base.atOffset(ZoneOffset.ofHours(8)).toString()))));
        mvc.perform(post("/api/device/v1/locations").contentType(MediaType.APPLICATION_JSON).content(body)).andExpect(status().isUnauthorized());
        mvc.perform(post("/api/device/v1/locations").header("X-Device-Key", credential.get("apiKey")).contentType(MediaType.APPLICATION_JSON).content(body)).andExpect(status().isOk()).andExpect(jsonPath("$.data.accepted").value(1));
        var rotated = deviceAccess.rotate(device.getId());
        String next = json.writeValueAsString(Map.of("deviceId", device.getId(), "points", List.of(Map.of("eventId", "real-2", "longitude", 120.161, "latitude", 30.251, "recordedAt", base.plusMinutes(1).atOffset(ZoneOffset.ofHours(8)).toString()))));
        mvc.perform(post("/api/device/v1/locations").header("X-Device-Key", credential.get("apiKey")).contentType(MediaType.APPLICATION_JSON).content(next)).andExpect(status().isUnauthorized());
        mvc.perform(post("/api/device/v1/locations").header("X-Device-Key", rotated.get("apiKey")).contentType(MediaType.APPLICATION_JSON).content(next)).andExpect(status().isOk());
    }

    /**
     * 测试生产环境禁用 Demo 模拟接入端点，访问时返回 404 Not Found。
     *
     * @throws Exception MockMvc 执行异常
     */
    @Test
    void productionDoesNotExposeDemoIngestion() throws Exception {
        mvc.perform(post("/api/v1/demo/locations").with(user(admin)).with(csrf()).contentType(MediaType.APPLICATION_JSON).content("{}")).andExpect(status().isNotFound());
    }

    /**
     * 测试驾驶舱及各项统计接口契约在合法鉴权下正常响应业务数据。
     *
     * @throws Exception MockMvc 执行异常
     */
    @Test
    void dashboardAndStatisticsContractsWork() throws Exception {
        for (String path : List.of("/dashboard", "/statistics/population", "/statistics/health", "/statistics/followups")) {
            mvc.perform(get("/api/v1" + path).with(user(admin))).andExpect(status().isOk()).andExpect(jsonPath("$.code").value("OK"));
        }
    }

    /**
     * 测试 Haversine 大圆距离公式计算标尺在原点及 1 纬度距离的几何准确性。
     */
    @Test
    void distanceHasCorrectScale() {
        assertThat(GeoService.distance(120.16, 30.25, 120.16, 30.25)).isZero();
        assertThat(GeoService.distance(0, 0, 0, 1)).isBetween(111000.0, 111300.0);
    }
}

