package com.johnnylin.dev.domain;

import com.baomidou.mybatisplus.annotation.*;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;

/**
 * 养老系统管理与业务操作员实体。
 *
 * <p>映射数据库表 {@code sys_user}，维护可登录后台系统的用户账户，包括系统管理员 (ADMIN)、
 * 业务操作员 (OPERATOR)、数据分析员 (ANALYST)。支持通过 {@code authVersion} 控制安全凭证即时失效。
 */
@Data
@TableName("sys_user")
public class User {

    /** 系统用户主键 ID，数据库自增 */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 登录账号名（唯一标识） */
    private String username;

    /**
     * BCrypt 加密存储的密码散列值。
     *
     * <p>配置 {@code @JsonIgnore}，在任何 JSON 序列化返回给前端时自动隐藏，防止凭证泄露。
     */
    @JsonIgnore
    private String passwordHash;

    /** 用户真实姓名或展示昵称 */
    private String displayName;

    /** 用户系统角色（ADMIN: 超级管理员, OPERATOR: 业务操作员, ANALYST: 只读数据分析员） */
    private String role;

    /** 账户启用状态（true 为正常启用，false 为已停用禁止登录） */
    private Boolean enabled;

    /** 认证安全版本号，密码重置或角色变更时自动递增，用于强制已登录会话失效重新认证 */
    private Integer authVersion;
}

