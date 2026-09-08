package com.johnnylin.dev.domain;

import com.baomidou.mybatisplus.annotation.*;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;
import java.time.*;

@Data
@TableName("health_rule")
public class HealthRule {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String metric;
    private Double lowerBound;
    private Double upperBound;
    private String unit;
    private Integer version;
    private Boolean enabled;
}

