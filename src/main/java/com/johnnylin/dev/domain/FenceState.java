package com.johnnylin.dev.domain;

import com.baomidou.mybatisplus.annotation.*;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;
import java.time.*;

@Data
@TableName("geofence_state")
public class FenceState {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long memberId;
    private Long lastPointId;
    private LocalDateTime lastRecordedAt;
    private Boolean inside;
    private Long activeAlertId;
}

