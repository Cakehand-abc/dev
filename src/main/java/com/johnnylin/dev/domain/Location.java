package com.johnnylin.dev.domain;

import com.baomidou.mybatisplus.annotation.*;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;
import java.time.*;

@Data
@TableName("location_point")
public class Location {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long deviceId;
    private Long bindingId;
    private Long elderId;
    private String eventId;
    private Double longitude;
    private Double latitude;
    private LocalDateTime recordedAt;
    private LocalDateTime receivedAt;
}

