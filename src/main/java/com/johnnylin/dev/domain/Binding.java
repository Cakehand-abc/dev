package com.johnnylin.dev.domain;

import com.baomidou.mybatisplus.annotation.*;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;
import java.time.*;

@Data
@TableName("device_binding")
public class Binding {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long deviceId;
    private Long elderId;
    private LocalDateTime boundAt;
    private LocalDateTime unboundAt;
    private Long activeDeviceId;
    private Long activeElderId;
}

