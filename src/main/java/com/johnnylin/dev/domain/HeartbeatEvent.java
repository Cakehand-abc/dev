package com.johnnylin.dev.domain;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("device_heartbeat_event")
public class HeartbeatEvent {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long deviceId;
    private String eventId;
    private LocalDateTime recordedAt;
    private LocalDateTime receivedAt;
}
