package com.johnnylin.dev.domain;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.*;

/**
 * 医生健康随访执行记录实体。
 *
 * <p>映射数据库表 {@code followup_record}，保存医生实际完成随访时的记录详情，
 * 包括实际完成时间、查访内容阐述与健康指导结论。
 */
@Data
@TableName("followup_record")
public class FollowupRecord {

    /** 随访记录主键 ID，数据库自增 */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 关联的随访计划 ID (followup_plan.id)，一对一绑定 */
    private Long planId;

    /** 医生实际完成随访的时间戳 */
    private LocalDateTime completedAt;

    /** 随访查体与沟通详细过程记录 */
    private String content;

    /** 随访处理结果、医嘱建议或转诊指导结论 */
    private String result;
}

