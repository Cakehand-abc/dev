package com.johnnylin.dev.domain;

import com.baomidou.mybatisplus.annotation.*;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;
import java.time.*;

@Data
@TableName("doctor")
public class Doctor {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String code;
    private String name;
    private String department;
    private String phone;
    private Boolean enabled;
    private Integer version;
}

