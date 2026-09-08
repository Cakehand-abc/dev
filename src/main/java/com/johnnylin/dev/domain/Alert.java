package com.johnnylin.dev.domain;

import com.baomidou.mybatisplus.annotation.*;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;
import java.time.*;

@Data
@TableName("geofence_alert")
public class Alert {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long memberId;
    private Long exitPointId;
    private LocalDateTime triggeredAt;
    private LocalDateTime returnedAt;
    private LocalDateTime closedAt;
    private String closeReason;
    private Long handledBy;
    private LocalDateTime handledAt;
    private String handlingNote;
    private String geometrySnapshot;
    private Integer version;
}

