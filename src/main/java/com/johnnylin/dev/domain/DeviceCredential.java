package com.johnnylin.dev.domain;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("device_credential")
public class DeviceCredential {
    @TableId
    private Long deviceId;
    private String keyHash;
    private LocalDateTime createdAt;
    private LocalDateTime rotatedAt;
}
