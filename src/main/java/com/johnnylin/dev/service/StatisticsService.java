package com.johnnylin.dev.service;

import java.util.Map;

/**
 * 养老与健康监护数据统计分析及大屏驾驶舱指标服务接口。
 *
 * <p>提供人口结构画像、健康监测时序趋势、随访任务履约以及驾驶舱综合大屏全景看板。
 */
public interface StatisticsService {

    /**
     * 统计长者人口结构分布概况。
     *
     * @param region 片区 ID（可为 null，表示全辖区范围）
     * @return 包含 total (长者总数)、age (年龄段列表)、gender (性别分布)、region (片区分布)、asOf (统计基准日) 的聚合 Map
     */
    Map<String, Object> population(Long region);

    /**
     * 统计指定时间区间与片区内的健康体征测量概况与时间变化趋势。
     *
     * @param region 片区 ID（可为 null）
     * @param start 起始时间 ISO 文本（可为 null，默认近 30 天）
     * @param end 结束时间 ISO 文本（可为 null，默认至次日）
     * @return 包含 summary (综合受检与异常指标) 与 trend (按天时序趋势列表) 的 Map
     */
    Map<String, Object> health(Long region, String start, String end);

    /**
     * 统计指定时间区间内医生随访任务的整体与个人履约指标。
     *
     * @param region 片区 ID（可为 null）
     * @param doctor 指定医生 ID（可为 null，统计全部医生）
     * @param start 起始时间 ISO 文本（可为 null）
     * @param end 结束时间 ISO 文本（可为 null）
     * @return 包含 summary (总体履约完成率) 与 doctors (医生维度明细列表) 的 Map
     */
    Map<String, Object> followups(Long region, Long doctor, String start, String end);

    /**
     * 聚合生成综合运营管理大屏看板数据包。
     *
     * @param region 片区 ID（可为 null）
     * @param start 起始时间 ISO 文本（可为 null）
     * @param end 结束时间 ISO 文本（可为 null）
     * @return 包含 population、health、followups、devices、generatedAt 的全量驾驶舱看板数据
     */
    Map<String, Object> dashboard(Long region, String start, String end);
}
