package com.johnnylin.dev.service;

import com.johnnylin.dev.auth.AccountPrincipal;
import com.johnnylin.dev.auth.LoginUser;
import com.johnnylin.dev.auth.TokenResponse;

/**
 * 分布式双 Token（Access Token + Refresh Token）管理服务接口。
 *
 * <p>负责基于 Redis 的令牌生命周期管理：
 * <ul>
 *   <li>登录时生成 AccessToken（短效 30 分钟）与 RefreshToken（长效 7 天）</li>
 *   <li>验证请求头中的 AccessToken 并反查用户身份</li>
 *   <li>当 AccessToken 过期时，凭借 RefreshToken 申请置换新 AccessToken（无感刷新）</li>
 *   <li>注销登出时主动清除 Redis 中的有效凭据</li>
 * </ul>
 */
public interface TokenService {

    /**
     * 为成功认证的用户主体签发 AccessToken 与 RefreshToken。
     *
     * @param principal 认证通过的用户主体
     * @return 包含双 Token 与有效期的 TokenResponse
     */
    TokenResponse createTokens(AccountPrincipal principal);

    /**
     * 根据 AccessToken 获取 Redis 中缓存的轻量用户身份。
     *
     * @param accessToken 访问令牌字符串
     * @return 缓存的 LoginUser 实例，若不存在或已失效则返回 null
     */
    LoginUser getLoginUserByAccessToken(String accessToken);

    /**
     * 使用有效的 RefreshToken 无感置换新的 AccessToken。
     *
     * @param refreshToken 刷新令牌字符串
     * @return 包含新 AccessToken 与更新后有效期的响应对象
     */
    TokenResponse refreshAccessToken(String refreshToken);

    /**
     * 注销并从 Redis 中销毁指定的令牌。
     *
     * @param accessToken 访问令牌（允许为 null）
     * @param refreshToken 刷新令牌（允许为 null）
     */
    void removeTokens(String accessToken, String refreshToken);
}
