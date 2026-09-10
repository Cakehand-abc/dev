package com.johnnylin.dev.domain;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

/**
 * 生理体征健康告警判决阈值规则实体。
 *
 * <p>映射数据库表 {@code health_rule}，定义各项生理体征（收缩压、舒张压、心率、血氧、体温）
 * 的正常值上下界区间与单位，系统根据最新启用的规则版本判断体征是否异常。
 */
@Data
@TableName("health_rule")
public class HealthRule {

    /** 规则主键 ID，数据库自增 */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 体征指标键名（例如 systolic: 收缩压, diastolic: 舒张压, heartRate: 心率, oxygen: 血氧, temperature: 体温） */
    private String metric;

    /** 正常参考区间下限（包含） */
    private Double lowerBound;

    /** 正常参考区间上限（包含） */
    private Double upperBound;

    /** 物理度量单位（例如 mmHg、次/分、%、℃） */
    private String unit;

    /** 规则版本号，规则升级时递增，并快照存入检测记录 */
    private Integer version;

    /** 规则启用状态（true 为生效中，false 为停用） */
    private Boolean enabled;
}

