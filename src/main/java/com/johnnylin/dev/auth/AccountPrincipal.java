package com.johnnylin.dev.auth;

import com.johnnylin.dev.domain.User;
import org.springframework.security.core.*;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import java.util.*;

/**
 * Spring Security 认证用户安全上下文主体（Principal）。
 *
 * <p>实现 {@link UserDetails} 接口，封装已成功通过身份认证的系统用户信息，
 * 包括用户 ID、用户名、加密口令、系统角色、姓名、启用状态以及安全凭证版本号（{@code authVersion}）。
 *
 * @param id 用户主键 ID
 * @param username 登录账号名
 * @param password BCrypt 加密存储的密码散列值
 * @param role 用户角色标识（如 ADMIN、OPERATOR、ANALYST）
 * @param displayName 用户真实姓名/展示昵称
 * @param enabled 账号启用状态（true 为启用，false 为禁用）
 * @param authVersion 认证安全版本号，用于密码重置或角色变更时即时强制注销已有会话
 */
public record AccountPrincipal(
        Long id,
        String username,
        String password,
        String role,
        String displayName,
        boolean enabled,
        int authVersion
) implements UserDetails {

    /**
     * 根据系统用户持久化实体构建认证安全主体。
     *
     * @param u 用户领域实体对象
     * @return 封装后的 AccountPrincipal 实例
     */
    public static AccountPrincipal of(User u) {
        return new AccountPrincipal(
                u.getId(),
                u.getUsername(),
                u.getPasswordHash(),
                u.getRole(),
                u.getDisplayName(),
                u.getEnabled(),
                u.getAuthVersion()
        );
    }

    /**
     * 获取用户权限与角色集合。
     *
     * @return 包含对应角色（格式为 "ROLE_{role}"）的 GrantedAuthority 集合
     */
    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_" + role));
    }

    /**
     * 获取加密存储的密码散列值。
     *
     * @return BCrypt 密码密文
     */
    @Override
    public String getPassword() {
        return password;
    }

    /**
     * 获取登录用户名。
     *
     * @return 用户账号名称
     */
    @Override
    public String getUsername() {
        return username;
    }

    /**
     * 判断账号是否处于可用状态。
     *
     * @return true 表示可用，false 表示已禁用
     */
    @Override
    public boolean isEnabled() {
        return enabled;
    }

    /**
     * 返回脱敏后的主体摘要字符串，隐去密码散列值。
     *
     * @return 包含 ID、用户名与角色的字符串表示
     */
    @Override
    public String toString() {
        return "AccountPrincipal[id=" + id + ", username=" + username + ", role=" + role + "]";
    }

    /**
     * 转换为对前端公开展示的安全视图 DTO（排除密码哈希与内部版本号）。
     *
     * @return 包含 id、username、displayName、role 的公开属性 Map
     */
    public Map<String, Object> publicView() {
        return Map.of("id", id, "username", username, "displayName", displayName, "role", role);
    }
}
