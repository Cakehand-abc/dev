package com.johnnylin.dev.auth;

import java.io.Serializable;
import java.util.Map;

/**
 * Redis 缓存与分布式会话中保存的用户身份实体。
 *
 * <p>封装经认证后的用户关键属性，包含用户 ID、账号名、角色标识、展示昵称、
 * 启用状态以及内部安全凭证版本号（{@code authVersion}）。
 */
public class LoginUser implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;
    private String username;
    private String role;
    private String displayName;
    private boolean enabled;
    private int authVersion;

    public LoginUser() {
    }

    public LoginUser(Long id, String username, String role, String displayName, boolean enabled, int authVersion) {
        this.id = id;
        this.username = username;
        this.role = role;
        this.displayName = displayName;
        this.enabled = enabled;
        this.authVersion = authVersion;
    }

    /**
     * 从安全主体 AccountPrincipal 构建 LoginUser 缓存对象。
     *
     * @param p 认证主体实例
     * @return LoginUser 缓存实体
     */
    public static LoginUser fromPrincipal(AccountPrincipal p) {
        return new LoginUser(p.id(), p.username(), p.role(), p.displayName(), p.isEnabled(), p.authVersion());
    }

    /**
     * 转换为 AccountPrincipal 安全上下文主体。
     *
     * @return 对应的 AccountPrincipal 实例
     */
    public AccountPrincipal toPrincipal() {
        return new AccountPrincipal(id, username, "", role, displayName, enabled, authVersion);
    }

    /**
     * 转换为前端可公开读取的视图属性 Map。
     *
     * @return 包含公开属性的键值对集合
     */
    public Map<String, Object> publicView() {
        return Map.of(
                "id", id,
                "username", username,
                "displayName", displayName,
                "role", role
        );
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public int getAuthVersion() {
        return authVersion;
    }

    public void setAuthVersion(int authVersion) {
        this.authVersion = authVersion;
    }
}
