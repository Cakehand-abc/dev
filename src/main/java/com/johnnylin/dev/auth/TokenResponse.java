package com.johnnylin.dev.auth;

import java.util.HashMap;
import java.util.Map;

/**
 * 双 Token 签发与刷新结果数据载荷。
 *
 * @param accessToken 访问令牌（用于常规业务鉴权，TTL 30 分钟）
 * @param refreshToken 刷新令牌（用于无感刷新访问令牌，TTL 7 天）
 * @param expiresIn 访问令牌剩余有效期（秒）
 * @param user 认证成功用户的轻量身份视图
 */
public record TokenResponse(
        String accessToken,
        String refreshToken,
        long expiresIn,
        LoginUser user
) {

    /**
     * 将双 Token 与用户信息转换为兼顾单测与前端消费的响应数据字典。
     *
     * <p>扁平化展开用户角色、ID 等关键属性，以便与现有接口断言和前端直接绑定保持完全一致。
     *
     * @return 包含 accessToken、refreshToken、expiresIn 及用户基础字段的复合 Map
     */
    public Map<String, Object> toResultMap() {
        Map<String, Object> result = new HashMap<>();
        result.put("accessToken", accessToken);
        result.put("refreshToken", refreshToken);
        result.put("expiresIn", expiresIn);
        if (user != null) {
            result.put("id", user.getId());
            result.put("username", user.getUsername());
            result.put("displayName", user.getDisplayName());
            result.put("role", user.getRole());
            result.put("user", user.publicView());
        }
        return result;
    }
}
