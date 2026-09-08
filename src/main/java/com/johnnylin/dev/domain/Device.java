package com.johnnylin.dev.domain;

import com.baomidou.mybatisplus.annotation.*;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;
import java.time.*;

@Data
@TableName("watch_device")
public class Device {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String serialNo;
    private String model;
    private Boolean enabled;
    private LocalDateTime lastSeenAt;
    private Integer version;
}

