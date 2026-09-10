package com.johnnylin.dev.domain;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.*;

/**
 * 社区长者健康与监护档案实体。
 *
 * <p>映射数据库表 {@code elder}，维护被监护长者的基础身份画像，包括姓名、
 * 编码、性别、出生年月、居住网格片区、联系电话及当前档案状态（在册 ACTIVE / 归档 ARCHIVED）。
 */
@Data
@TableName("elder")
public class Elder {

    /** 长者档案主键 ID，数据库自增 */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 长者唯一档案编号（例如 EL-0001） */
    private String code;

    /** 长者姓名 */
    private String name;

    /** 性别（MALE: 男, FEMALE: 女, UNKNOWN: 未知） */
    private String gender;

    /** 出生日期（用于计算周岁年龄及年龄段统计） */
    private LocalDate birthDate;

    /** 所属行政或网格片区 ID (region.id) */
    private Long regionId;

    /** 家属或长者联系电话（展示给前端时自动掩码脱敏） */
    private String phone;

    /** 档案状态（ACTIVE: 在册监护中, ARCHIVED: 已归档迁出；归档后禁止修改与新增业务） */
    private String status;

    /** 乐观锁版本号 */
    private Integer version;
}

