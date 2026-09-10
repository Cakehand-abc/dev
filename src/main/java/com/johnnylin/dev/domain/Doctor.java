package com.johnnylin.dev.domain;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

/**
 * 社区随访医生/医护人员信息实体。
 *
 * <p>映射数据库表 {@code doctor}，维护家庭医生及网格医护人员档案，
 * 支撑线下健康巡访、随访计划派单与履约情况跟踪。
 */
@Data
@TableName("doctor")
public class Doctor {

    /** 医生记录主键 ID，数据库自增 */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 医生工号或唯一业务编码（例如 DOC-0001） */
    private String code;

    /** 医生姓名 */
    private String name;

    /** 所属科室或诊疗部门（例如 全科医学、老年健康科） */
    private String department;

    /** 医生联系电话（展示给前端时会自动进行脱敏处理） */
    private String phone;

    /** 医生在职/启用状态（true 为启用，false 为停用；停用后不可分派新随访） */
    private Boolean enabled;

    /** 乐观锁版本号 */
    private Integer version;
}

