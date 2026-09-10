package com.johnnylin.dev.domain;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.*;

/**
 * 长者生命体征健康检测记录实体。
 *
 * <p>映射数据库表 {@code health_record}，保存长者血压、心率、血氧、体温等多维
 * 生理体征数据，并结合系统健康阈值规则自动判定是否异常并留存判定快照。
 */
@Data
@TableName("health_record")
public class HealthRecord {

    /** 健康记录主键 ID，数据库自增 */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 关联的长者档案 ID (elder.id) */
    private Long elderId;

    /** 实际检测/测量时间戳（不得晚于当前服务器时间） */
    private LocalDateTime measuredAt;

    /** 收缩压（高压），单位：mmHg，正常参考范围通常为 90 ~ 139 mmHg */
    private Double systolic;

    /** 舒张压（低压），单位：mmHg，正常参考范围通常为 60 ~ 89 mmHg */
    private Double diastolic;

    /** 脉搏/心率，单位：次/分 (bpm)，正常参考范围通常为 60 ~ 100 bpm */
    private Double heartRate;

    /** 血液氧饱和度 (SpO2)，单位：%，正常参考范围通常为 95% ~ 100% */
    private Double oxygen;

    /** 腋下/体表温度，单位：℃，正常参考范围通常为 36.0 ~ 37.3 ℃ */
    private Double temperature;

    /** 综合判决是否超出正常参考范围（true 为存在异常指标，false 为完全正常） */
    private Boolean abnormal;

    /** 判定该条体征数据时所依据的阈值规则快照 JSON（包含各项指标的上下限与规则版本） */
    private String ruleSnapshot;

    /** 数据采集来源（例如 MANUAL: 人工登记, DEVICE: 智能设备自动回传） */
    private String source;

    /** 客户端/设备端生成的唯一检测事件流水号，用于幂等写入去重 */
    private String eventId;
}

