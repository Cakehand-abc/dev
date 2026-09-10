package com.johnnylin.dev.domain;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.*;

/**
 * 医生健康随访计划实体。
 *
 * <p>映射数据库表 {@code followup_plan}，记录医生对长者预定的线下或线上健康巡访任务，
 * 跟踪待执行、已完成与取消状态，支撑随访履约率统计。
 */
@Data
@TableName("followup_plan")
public class FollowupPlan {

    /** 随访计划主键 ID，数据库自增 */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 关联的长者档案 ID (elder.id) */
    private Long elderId;

    /** 指定执行随访的医生 ID (doctor.id) */
    private Long doctorId;

    /** 预计应完成随访的目标截止时间 */
    private LocalDateTime dueAt;

    /** 计划当前状态（PENDING: 待随访, COMPLETED: 已完成, CANCELED: 已取消） */
    private String status;

    /** 计划被取消的时间（若未取消为 null） */
    private LocalDateTime canceledAt;

    /** 取消原因说明（例如 "老人归档"、"医生调班" 等） */
    private String cancelReason;

    /** 乐观锁版本号 */
    private Integer version;
}

