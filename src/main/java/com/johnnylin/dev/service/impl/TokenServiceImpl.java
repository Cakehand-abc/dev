package com.johnnylin.dev.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.johnnylin.dev.auth.AccountPrincipal;
import com.johnnylin.dev.auth.LoginUser;
import com.johnnylin.dev.auth.TokenResponse;
import com.johnnylin.dev.common.Api;
import com.johnnylin.dev.domain.User;
import com.johnnylin.dev.mapper.UserMapper;
import com.johnnylin.dev.service.TokenService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * 分布式双 Token 管理服务实现类。
 *
 * <p>核心机制：
 * <ul>
 *   <li>短效 AccessToken 存入 {@code login:access:{token}}，默认 TTL 30 分钟</li>
 *   <li>长效 RefreshToken 存入 {@code login:refresh:{token}}，默认 TTL 7 天</li>
 *   <li>后端应用重启不会清除独立 Redis 进程内的数据，已登录会话在 RefreshToken 有效期内无需重新输入口令</li>
 *   <li>通过比对 DB 端的 {@code authVersion} 与 {@code enabled} 保证账号被禁用或修改密码时能即时作废</li>
 * </ul>
 */
@Slf4j
@Service
public class TokenServiceImpl implements TokenService {

    public static final String ACCESS_TOKEN_PREFIX = "login:access:";
    public static final String REFRESH_TOKEN_PREFIX = "login:refresh:";
    public static final long ACCESS_EXPIRE_SECONDS = 30 * 60L; // 30 分钟
    public static final long REFRESH_EXPIRE_SECONDS = 7 * 24 * 3600L; // 7 天

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;
    private final UserMapper userMapper;

    public TokenServiceImpl(StringRedisTemplate redisTemplate, ObjectMapper objectMapper, UserMapper userMapper) {
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
        this.userMapper = userMapper;
    }

    @Override
    public TokenResponse createTokens(AccountPrincipal principal) {
        String accessToken = generateToken();
        String refreshToken = generateToken();
        LoginUser loginUser = LoginUser.fromPrincipal(principal);

        try {
            String json = objectMapper.writeValueAsString(loginUser);
            redisTemplate.opsForValue().set(ACCESS_TOKEN_PREFIX + accessToken, json, ACCESS_EXPIRE_SECONDS, TimeUnit.SECONDS);
            redisTemplate.opsForValue().set(REFRESH_TOKEN_PREFIX + refreshToken, json, REFRESH_EXPIRE_SECONDS, TimeUnit.SECONDS);
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize LoginUser for user: {}", principal.getUsername(), e);
            throw new Api.Failure(500, "令牌生成失败");
        }

        return new TokenResponse(accessToken, refreshToken, ACCESS_EXPIRE_SECONDS, loginUser);
    }

    @Override
    public LoginUser getLoginUserByAccessToken(String accessToken) {
        if (accessToken == null || accessToken.isBlank()) {
            return null;
        }
        String json = redisTemplate.opsForValue().get(ACCESS_TOKEN_PREFIX + accessToken);
        if (json == null || json.isBlank()) {
            return null;
        }
        try {
            return objectMapper.readValue(json, LoginUser.class);
        } catch (JsonProcessingException e) {
            log.warn("Failed to parse cached LoginUser for token: {}", accessToken, e);
            return null;
        }
    }

    @Override
    public TokenResponse refreshAccessToken(String refreshToken) {
        if (refreshToken == null || refreshToken.isBlank()) {
            throw new Api.Failure(401, "刷新令牌不能为空");
        }

        String refreshKey = REFRESH_TOKEN_PREFIX + refreshToken;
        String json = redisTemplate.opsForValue().get(refreshKey);
        if (json == null || json.isBlank()) {
            throw new Api.Failure(401, "登录会话已过期，请重新登录");
        }

        LoginUser cachedUser;
        try {
            cachedUser = objectMapper.readValue(json, LoginUser.class);
        } catch (JsonProcessingException e) {
            redisTemplate.delete(refreshKey);
            throw new Api.Failure(401, "无效的刷新令牌");
        }

        // 验证数据库中账号的实时状态（是否启用、安全版本号是否匹配）
        User current = userMapper.selectById(cachedUser.getId());
        if (current == null || !Boolean.TRUE.equals(current.getEnabled()) || current.getAuthVersion() != cachedUser.getAuthVersion()) {
            redisTemplate.delete(refreshKey);
            throw new Api.Failure(401, "账号状态已变更，请重新登录");
        }

        // 签发新的短效 AccessToken
        String newAccessToken = generateToken();
        LoginUser updatedUser = LoginUser.fromPrincipal(AccountPrincipal.of(current));
        try {
            String updatedJson = objectMapper.writeValueAsString(updatedUser);
            redisTemplate.opsForValue().set(ACCESS_TOKEN_PREFIX + newAccessToken, updatedJson, ACCESS_EXPIRE_SECONDS, TimeUnit.SECONDS);
            // 滑动续签 RefreshToken 的有效期为 7 天
            redisTemplate.opsForValue().set(refreshKey, updatedJson, REFRESH_EXPIRE_SECONDS, TimeUnit.SECONDS);
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize updated LoginUser", e);
            throw new Api.Failure(500, "令牌续期失败");
        }

        return new TokenResponse(newAccessToken, refreshToken, ACCESS_EXPIRE_SECONDS, updatedUser);
    }

    @Override
    public void removeTokens(String accessToken, String refreshToken) {
        if (accessToken != null && !accessToken.isBlank()) {
            redisTemplate.delete(ACCESS_TOKEN_PREFIX + accessToken);
        }
        if (refreshToken != null && !refreshToken.isBlank()) {
            redisTemplate.delete(REFRESH_TOKEN_PREFIX + refreshToken);
        }
    }

    /**
     * 生成不带破折号的高强度 32 位随机 Token 字符串。
     *
     * @return 32 位十六进制随机字符串
     */
    private String generateToken() {
        return UUID.randomUUID().toString().replace("-", "");
    }
}
