package com.johnnylin.dev.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.github.yulichang.wrapper.MPJLambdaWrapper;
import com.johnnylin.dev.domain.*;
import com.johnnylin.dev.mapper.*;
import com.johnnylin.dev.service.GeoService;
import com.johnnylin.dev.service.StatisticsService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.Period;
import java.util.*;

import static com.johnnylin.dev.common.Input.*;

/**
 * 养老与健康监护数据统计分析及大屏驾驶舱指标服务实现类。
 *
 * <p>全面采用 {@code mybatis-plus-join} 插件的 {@link MPJLambdaWrapper} 进行类型安全的多表连表聚合查询，
 * 彻底废除 XML 映射依赖。
 */
@Service
@RequiredArgsConstructor
public class StatisticsServiceImpl implements StatisticsService {

    private final ElderMapper elderMapper;
    private final HealthRecordMapper healthRecordMapper;
    private final FollowupPlanMapper followupPlanMapper;
    private final GeoService geo;

    @Override
    public Map<String, Object> population(Long region) {
        var age = calculatePopulationAge(region);
        long total = age.stream().mapToLong(r -> ((Number) r.get("value")).longValue()).sum();
        return Map.of(
                "total", total,
                "age", age,
                "gender", calculatePopulationGender(region),
                "region", calculatePopulationRegion(region),
                "asOf", LocalDate.now(ZONE)
        );
    }

    /**
     * 计算长者各年龄段梯级分布。
     */
    private List<Map<String, Object>> calculatePopulationAge(Long regionId) {
        var query = new LambdaQueryWrapper<Elder>()
                .select(Elder::getId, Elder::getBirthDate)
                .eq(Elder::getStatus, "ACTIVE");
        if (regionId != null) {
            query.eq(Elder::getRegionId, regionId);
        }
        List<Elder> elders = elderMapper.selectList(query);

        Map<String, Long> counts = new LinkedHashMap<>();
        counts.put("60岁以下", 0L);
        counts.put("60至69岁", 0L);
        counts.put("70至79岁", 0L);
        counts.put("80至89岁", 0L);
        counts.put("90岁及以上", 0L);
        counts.put("年龄未知", 0L);

        LocalDate today = LocalDate.now(ZONE);
        for (Elder e : elders) {
            if (e == null || e.getBirthDate() == null) {
                counts.merge("年龄未知", 1L, Long::sum);
            } else {
                int years = Period.between(e.getBirthDate(), today).getYears();
                if (years < 60) {
                    counts.merge("60岁以下", 1L, Long::sum);
                } else if (years < 70) {
                    counts.merge("60至69岁", 1L, Long::sum);
                } else if (years < 80) {
                    counts.merge("70至79岁", 1L, Long::sum);
                } else if (years < 90) {
                    counts.merge("80至89岁", 1L, Long::sum);
                } else {
                    counts.merge("90岁及以上", 1L, Long::sum);
                }
            }
        }

        List<Map<String, Object>> list = new ArrayList<>();
        counts.forEach((k, v) -> list.add(new LinkedHashMap<>(Map.of("name", k, "value", v))));
        return list;
    }

    /**
     * 计算长者生理性别比例分布。
     */
    private List<Map<String, Object>> calculatePopulationGender(Long regionId) {
        var query = new LambdaQueryWrapper<Elder>()
                .select(Elder::getId, Elder::getGender)
                .eq(Elder::getStatus, "ACTIVE");
        if (regionId != null) {
            query.eq(Elder::getRegionId, regionId);
        }
        List<Elder> elders = elderMapper.selectList(query);

        Map<String, Long> counts = new LinkedHashMap<>();
        counts.put("男", 0L);
        counts.put("女", 0L);
        counts.put("未知", 0L);

        for (Elder e : elders) {
            if (e == null || e.getGender() == null) {
                counts.merge("未知", 1L, Long::sum);
                continue;
            }
            String g = e.getGender();
            if ("MALE".equalsIgnoreCase(g)) {
                counts.merge("男", 1L, Long::sum);
            } else if ("FEMALE".equalsIgnoreCase(g)) {
                counts.merge("女", 1L, Long::sum);
            } else {
                counts.merge("未知", 1L, Long::sum);
            }
        }

        List<Map<String, Object>> list = new ArrayList<>();
        counts.forEach((k, v) -> list.add(new LinkedHashMap<>(Map.of("name", k, "value", v))));
        return list;
    }

    /**
     * 基于 MyBatis-Plus-Join 关联查询片区网格分布。
     */
    private List<Map<String, Object>> calculatePopulationRegion(Long regionId) {
        MPJLambdaWrapper<Elder> wrapper = new MPJLambdaWrapper<Elder>()
                .selectAs(Region::getName, "name")
                .selectCount(Elder::getId, "`value`")
                .innerJoin(Region.class, Region::getId, Elder::getRegionId)
                .eq(Elder::getStatus, "ACTIVE")
                .groupBy(Region::getId, Region::getName)
                .orderByAsc(Region::getId);
        if (regionId != null) {
            wrapper.eq(Elder::getRegionId, regionId);
        }
        return elderMapper.selectJoinMaps(wrapper);
    }

    @Override
    public Map<String, Object> health(Long region, String start, String end) {
        var p = filters(region, start, end, null, null);

        // 1. 综合指标汇总：使用 MPJ 连表
        MPJLambdaWrapper<HealthRecord> summaryWrapper = new MPJLambdaWrapper<HealthRecord>()
                .select("COUNT(*) AS measurements")
                .select("COUNT(DISTINCT t.elder_id) AS people")
                .select("COUNT(DISTINCT CASE WHEN t.abnormal = TRUE THEN t.elder_id END) AS abnormalPeople")
                .select("COALESCE(SUM(CASE WHEN t.abnormal = TRUE THEN 1 ELSE 0 END), 0) AS abnormalRecords")
                .innerJoin(Elder.class, Elder::getId, HealthRecord::getElderId)
                .ge(HealthRecord::getMeasuredAt, p.get("start"))
                .lt(HealthRecord::getMeasuredAt, p.get("end"));
        if (p.get("regionId") != null) {
            summaryWrapper.eq(Elder::getRegionId, p.get("regionId"));
        }
        Map<String, Object> summary = healthRecordMapper.selectJoinMap(summaryWrapper);
        if (summary == null) {
            summary = new LinkedHashMap<>(Map.of("measurements", 0L, "people", 0L, "abnormalPeople", 0L, "abnormalRecords", 0L));
        }

        // 2. 按天趋势折线：使用 MPJ 连表，使用 MySQL 与 H2 均完全兼容的标准 CAST AS DATE
        MPJLambdaWrapper<HealthRecord> trendWrapper = new MPJLambdaWrapper<HealthRecord>()
                .select("CAST(t.measured_at AS DATE) AS name")
                .select("COUNT(*) AS `value`")
                .select("COALESCE(SUM(CASE WHEN t.abnormal = TRUE THEN 1 ELSE 0 END), 0) AS abnormal")
                .innerJoin(Elder.class, Elder::getId, HealthRecord::getElderId)
                .ge(HealthRecord::getMeasuredAt, p.get("start"))
                .lt(HealthRecord::getMeasuredAt, p.get("end"));
        if (p.get("regionId") != null) {
            trendWrapper.eq(Elder::getRegionId, p.get("regionId"));
        }
        trendWrapper.groupBy("CAST(t.measured_at AS DATE)")
                .orderByAsc("CAST(t.measured_at AS DATE)");
        List<Map<String, Object>> trendList = healthRecordMapper.selectJoinMaps(trendWrapper);

        return Map.of(
                "summary", summary,
                "trend", trendList
        );
    }

    private Map<String, Object> rate(Map<String, Object> m) {
        if (m == null) {
            return new HashMap<>();
        }
        Object plannedObj = m.get("planned");
        long count = plannedObj instanceof Number ? ((Number) plannedObj).longValue() : 0L;
        Object completedObj = m.get("completed");
        double completed = completedObj instanceof Number ? ((Number) completedObj).doubleValue() : 0.0;
        m.put("completionRate", count == 0 ? null : Math.round(completed / count * 1000) / 10.0);
        return m;
    }

    @Override
    public Map<String, Object> followups(Long region, Long doctor, String start, String end) {
        var p = filters(region, start, end, null, doctor);
        String cutoff = String.valueOf(p.get("cutoff"));

        // 1. 总体履约概况：使用 MPJ 连表
        MPJLambdaWrapper<FollowupPlan> summaryWrapper = new MPJLambdaWrapper<FollowupPlan>()
                .select("COUNT(*) AS planned")
                .select("COALESCE(SUM(CASE WHEN t1.completed_at < '" + cutoff + "' THEN 1 ELSE 0 END), 0) AS completed")
                .select("COALESCE(SUM(CASE WHEN t.due_at < '" + cutoff + "' AND (t1.completed_at IS NULL OR t1.completed_at >= '" + cutoff + "') THEN 1 ELSE 0 END), 0) AS overdue")
                .select("COALESCE(SUM(CASE WHEN t1.completed_at < '" + cutoff + "' AND t1.completed_at > t.due_at THEN 1 ELSE 0 END), 0) AS late")
                .leftJoin(FollowupRecord.class, FollowupRecord::getPlanId, FollowupPlan::getId)
                .innerJoin(Elder.class, Elder::getId, FollowupPlan::getElderId)
                .ge(FollowupPlan::getDueAt, p.get("start"))
                .lt(FollowupPlan::getDueAt, p.get("end"))
                .apply("(t.canceled_at IS NULL OR t.canceled_at >= {0})", cutoff);
        if (p.get("regionId") != null) {
            summaryWrapper.eq(Elder::getRegionId, p.get("regionId"));
        }
        if (p.get("doctorId") != null) {
            summaryWrapper.eq(FollowupPlan::getDoctorId, p.get("doctorId"));
        }
        Map<String, Object> summary = followupPlanMapper.selectJoinMap(summaryWrapper);
        if (summary == null) {
            summary = new LinkedHashMap<>(Map.of("planned", 0L, "completed", 0L, "overdue", 0L, "late", 0L));
        }

        // 2. 医生维度履约排行：使用 MPJ 连表
        MPJLambdaWrapper<FollowupPlan> doctorsWrapper = new MPJLambdaWrapper<FollowupPlan>()
                .selectAs(Doctor::getName, "name")
                .select("COUNT(*) AS planned")
                .select("COALESCE(SUM(CASE WHEN t1.completed_at < '" + cutoff + "' THEN 1 ELSE 0 END), 0) AS completed")
                .select("COALESCE(SUM(CASE WHEN t.due_at < '" + cutoff + "' AND (t1.completed_at IS NULL OR t1.completed_at >= '" + cutoff + "') THEN 1 ELSE 0 END), 0) AS overdue")
                .select("COALESCE(SUM(CASE WHEN t1.completed_at < '" + cutoff + "' AND t1.completed_at > t.due_at THEN 1 ELSE 0 END), 0) AS late")
                .leftJoin(FollowupRecord.class, FollowupRecord::getPlanId, FollowupPlan::getId)
                .innerJoin(Doctor.class, Doctor::getId, FollowupPlan::getDoctorId)
                .innerJoin(Elder.class, Elder::getId, FollowupPlan::getElderId)
                .ge(FollowupPlan::getDueAt, p.get("start"))
                .lt(FollowupPlan::getDueAt, p.get("end"))
                .apply("(t.canceled_at IS NULL OR t.canceled_at >= {0})", cutoff);
        if (p.get("regionId") != null) {
            doctorsWrapper.eq(Elder::getRegionId, p.get("regionId"));
        }
        if (p.get("doctorId") != null) {
            doctorsWrapper.eq(FollowupPlan::getDoctorId, p.get("doctorId"));
        }
        doctorsWrapper.groupBy(Doctor::getId, Doctor::getName)
                .orderByAsc(Doctor::getId);
        List<Map<String, Object>> doctorsList = followupPlanMapper.selectJoinMaps(doctorsWrapper);

        return Map.of(
                "summary", rate(new LinkedHashMap<>(summary)),
                "doctors", doctorsList.stream().map(d -> rate(new LinkedHashMap<>(d))).toList()
        );
    }

    @Override
    public Map<String, Object> dashboard(Long region, String start, String end) {
        var devices = geo.distribution(region, null);
        var counts = new HashMap<String, Long>();
        for (var row : devices) {
            counts.merge((String) row.get("status"), 1L, Long::sum);
        }
        return Map.of(
                "population", population(region),
                "health", health(region, start, end),
                "followups", followups(region, null, start, end),
                "devices", counts,
                "generatedAt", now()
        );
    }
}
