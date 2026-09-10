package com.johnnylin.dev.config;

import com.johnnylin.dev.domain.Binding;
import com.johnnylin.dev.domain.Device;
import com.johnnylin.dev.domain.Doctor;
import com.johnnylin.dev.domain.Elder;
import com.johnnylin.dev.domain.Fence;
import com.johnnylin.dev.domain.Region;
import com.johnnylin.dev.mapper.BindingMapper;
import com.johnnylin.dev.mapper.DeviceMapper;
import com.johnnylin.dev.mapper.DoctorMapper;
import com.johnnylin.dev.mapper.ElderMapper;
import com.johnnylin.dev.mapper.RegionMapper;
import com.johnnylin.dev.service.CareService;
import com.johnnylin.dev.service.GeoService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import static com.johnnylin.dev.common.Input.*;

/**
 * 演示环境模拟测试数据播种器。
 *
 * <p>仅在激活 {@code demo} Profile 且配置 {@code app.seed-demo=true} 时生效（{@code @Order(1)}），
 * 负责在长者表为空时自动注入完整的端到端仿真业务数据，便于功能演示与前端联调：
 * <ul>
 *   <li>演示角色账号：业务操作员 (operator)、数据分析员 (analyst)</li>
 *   <li>6 名全科/老年医学科演示医生</li>
 *   <li>120 名长者健康档案（涵盖不同性别、年龄区间与网格片区）</li>
 *   <li>多维度健康体征检测历史记录（收缩压、舒张压、心率、血氧、体温）</li>
 *   <li>医生随访巡查计划与履约记录（含待执行、已完成及逾期场景）</li>
 *   <li>圆形电子地理围栏与纳管长者绑定</li>
 *   <li>18 台智能定位腕表、双向绑定历史、连续 GPS 轨迹点及心跳保活事件</li>
 * </ul>
 */
@Component
@Profile("demo")
@ConditionalOnProperty(name = "app.seed-demo", havingValue = "true")
@Order(1)
@RequiredArgsConstructor
public class DemoSeeder implements ApplicationRunner {

    private final ElderMapper elders;
    private final DoctorMapper doctors;
    private final RegionMapper regions;
    private final DeviceMapper devices;
    private final BindingMapper bindings;
    private final CareService care;
    private final GeoService geo;

    /** 初始密码，由配置项 app.bootstrap-password 注入 */
    @Value("${app.bootstrap-password:}")
    private String password;

    /**
     * 容器就绪后执行演示数据注入任务。
     *
     * @param args 命令行启动参数
     */
    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        // 若老人表已有记录，则跳过播种避免重复写入
        if (elders.selectCount(null) > 0) {
            return;
        }

        // 1. 初始化演示业务账号 (operator, analyst)
        CareService.password(Map.of("password", password));
        for (var role : List.of("OPERATOR", "ANALYST")) {
            care.saveUser(null, Map.of(
                    "username", role.toLowerCase(),
                    "displayName", role.equals("OPERATOR") ? "业务演示员" : "数据分析员",
                    "role", role,
                    "enabled", true,
                    "password", password
            ));
        }

        // 2. 初始化 6 名演示医生
        var regionList = regions.selectList(null);
        List<Elder> people = new ArrayList<>();
        List<Doctor> medics = new ArrayList<>();
        var random = new Random(20260907);
        for (int i = 0; i < 6; i++) {
            var d = new Doctor();
            d.setCode("DOC-" + (i + 1));
            d.setName("演示医生" + (i + 1));
            d.setDepartment(i % 2 == 0 ? "全科医学" : "老年健康");
            d.setEnabled(true);
            d.setVersion(0);
            doctors.insert(d);
            medics.add(d);
        }

        // 3. 初始化 120 名老人、健康体征测量数据与随访计划
        for (int i = 0; i < 120; i++) {
            var e = new Elder();
            e.setCode(String.format("EL-%04d", i + 1));
            e.setName(String.format("演示老人%03d", i + 1));
            e.setGender(i % 11 == 0 ? "UNKNOWN" : i % 2 == 0 ? "MALE" : "FEMALE");
            e.setBirthDate(i % 17 == 0 ? null : LocalDate.now(ZONE).minusYears(58 + random.nextInt(40)).minusDays(random.nextInt(300)));
            e.setRegionId(regionList.get(i % regionList.size()).getId());
            e.setStatus("ACTIVE");
            e.setVersion(0);
            elders.insert(e);
            people.add(e);

            // 为每个老人生成 9 条体征检测历史
            for (int j = 0; j < 9; j++) {
                var body = new HashMap<String, Object>();
                body.put("elderId", e.getId());
                body.put("measuredAt", now().minusDays(j * 3).minusMinutes(i + 10).atOffset(ZoneOffset.ofHours(8)).toString());
                body.put("systolic", i % 7 == 0 ? 152 : 110 + random.nextInt(25));
                body.put("diastolic", 65 + random.nextInt(20));
                body.put("heartRate", 62 + random.nextInt(30));
                body.put("oxygen", i % 13 == 0 ? 93 : 97);
                body.put("temperature", 36.0 + random.nextInt(10) / 10.0);
                body.put("eventId", "demo-health-" + i + "-" + j);
                care.addHealth(body);
            }

            // 生成随访计划，部分模拟完成，部分待办/逾期
            var plan = care.addPlan(Map.of(
                    "elderId", e.getId(),
                    "doctorId", medics.get(i % 6).getId(),
                    "dueAt", now().minusDays(i % 20).minusHours(2).atOffset(ZoneOffset.ofHours(8)).toString()
            ));
            if (i % 4 != 0) {
                care.complete(plan.getId(), Map.of(
                        "completedAt", now().minusDays(i % 20).minusHours(i % 3 == 0 ? 1 : 3).atOffset(ZoneOffset.ofHours(8)).toString(),
                        "content", "教学演示：记录日常健康情况和服务需求。",
                        "result", "已完成常规随访"
                ));
            }
        }

        // 4. 创建演示电子围栏并纳管前 18 名老人
        var fence = geo.saveFence(null, Map.of(
                "name", "滨江服务中心活动范围",
                "centerLon", 120.16,
                "centerLat", 30.25,
                "radiusM", 650,
                "enabled", true
        ));
        geo.setMembers(fence.getId(), people.subList(0, 18).stream().map(Elder::getId).toList());

        // 5. 初始化 18 台穿戴设备、绑定关系、轨迹点及心跳数据
        for (int i = 0; i < 18; i++) {
            var d = geo.saveDevice(null, Map.of(
                    "serialNo", String.format("WATCH-%04d", i + 1),
                    "model", "教学模拟腕表",
                    "enabled", true
            ));
            var binding = new Binding();
            binding.setDeviceId(d.getId());
            binding.setElderId(people.get(i).getId());
            binding.setBoundAt(now().minusDays(2));
            binding.setActiveDeviceId(d.getId());
            binding.setActiveElderId(people.get(i).getId());
            bindings.insert(binding);

            // 生成环状扩散的仿真轨迹点（部分越界触发告警）
            for (int j = 0; j < 36; j++) {
                double r = (i % 3 == 0 && j > 14 && j < 27) ? 0.009 : 0.0025;
                double angle = (j * 10 + i * 7) * Math.PI / 180;
                geo.ingest(Map.of(
                        "deviceId", d.getId(),
                        "eventId", "demo-position-" + i + "-" + j,
                        "longitude", 120.16 + r * Math.cos(angle),
                        "latitude", 30.25 + r * Math.sin(angle),
                        "recordedAt", now().minusMinutes((36 - j) * 5L).atOffset(ZoneOffset.ofHours(8)).toString()
                ));
            }

            // 模拟心跳上报
            geo.heartbeat(Map.of(
                    "deviceId", d.getId(),
                    "eventId", "demo-heartbeat-" + i,
                    "recordedAt", now().minusMinutes(i % 5 == 0 ? 30 : 1).atOffset(ZoneOffset.ofHours(8)).toString()
            ));
        }
    }
}
