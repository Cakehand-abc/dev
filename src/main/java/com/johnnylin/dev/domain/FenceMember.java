package com.johnnylin.dev.domain;

import com.baomidou.mybatisplus.annotation.*;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;
import java.time.*;

@Data
@TableName("geofence_member")
public class FenceMember {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long fenceId;
    private Long elderId;
    private Boolean enabled;
}

