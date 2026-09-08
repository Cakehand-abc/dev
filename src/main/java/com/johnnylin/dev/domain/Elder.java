package com.johnnylin.dev.domain;

import com.baomidou.mybatisplus.annotation.*;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;
import java.time.*;

@Data
@TableName("elder")
public class Elder {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String code;
    private String name;
    private String gender;
    private LocalDate birthDate;
    private Long regionId;
    private String phone;
    private String status;
    private Integer version;
}

