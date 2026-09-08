package com.johnnylin.dev.domain;

import com.baomidou.mybatisplus.annotation.*;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;
import java.time.*;

@Data
@TableName("audit_log")
public class Audit {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    private String action;
    private String targetType;
    private Long targetId;
    private LocalDateTime occurredAt;
    private String result;
}

