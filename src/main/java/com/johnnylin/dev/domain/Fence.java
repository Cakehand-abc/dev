package com.johnnylin.dev.domain;

import com.baomidou.mybatisplus.annotation.*;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;
import java.time.*;

@Data
@TableName("geofence")
public class Fence {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String name;
    private Double centerLon;
    private Double centerLat;
    private Double radiusM;
    private Boolean enabled;
    private Integer version;
}

