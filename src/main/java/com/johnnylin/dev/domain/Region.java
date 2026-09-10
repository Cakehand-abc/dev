package com.johnnylin.dev.domain;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

/**
 * 社区行政与网格服务片区实体。
 *
 * <p>映射数据库表 {@code region}，用于将社区养老服务划分为不同网格/片区，
 * 支持按片区分流长者管理、医生分派、设备分布与统计汇总。
 */
@Data
@TableName("region")
public class Region {

    /** 片区主键 ID，数据库自增 */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 片区业务编码（例如 DEMO-1、ZONE-01） */
    private String code;

    /** 片区名称（例如 滨江片区、文苑片区） */
    private String name;

    /** 父级片区 ID（支持树形层级扩展，顶层为 null） */
    private Long parentId;
}

