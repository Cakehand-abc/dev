package com.johnnylin.dev.domain;

import com.baomidou.mybatisplus.annotation.*;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;
import java.time.*;

@Data
@TableName("health_record")
public class HealthRecord {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long elderId;
    private LocalDateTime measuredAt;
    private Double systolic;
    private Double diastolic;
    private Double heartRate;
    private Double oxygen;
    private Double temperature;
    private Boolean abnormal;
    private String ruleSnapshot;
    private String source;
    private String eventId;
}

