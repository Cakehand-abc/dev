package com.johnnylin.dev.domain;

import com.baomidou.mybatisplus.annotation.*;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;
import java.time.*;

@Data
@TableName("followup_plan")
public class FollowupPlan {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long elderId;
    private Long doctorId;
    private LocalDateTime dueAt;
    private String status;
    private LocalDateTime canceledAt;
    private String cancelReason;
    private Integer version;
}

