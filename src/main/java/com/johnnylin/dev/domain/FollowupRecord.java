package com.johnnylin.dev.domain;

import com.baomidou.mybatisplus.annotation.*;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;
import java.time.*;

@Data
@TableName("followup_record")
public class FollowupRecord {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long planId;
    private LocalDateTime completedAt;
    private String content;
    private String result;
}

