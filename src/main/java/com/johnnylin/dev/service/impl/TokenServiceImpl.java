package com.johnnylin.dev.service.impl;

import cn.hutool.json.JSONUtil;
import com.johnnylin.dev.auth.AccountPrincipal;
import com.johnnylin.dev.auth.LoginUser;
import com.johnnylin.dev.auth.TokenResponse;
import com.johnnylin.dev.common.Api;
import com.johnnylin.dev.domain.User;
import com.johnnylin.dev.mapper.UserMapper;
import com.johnnylin.dev.service.TokenService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * 分布式双 Token 管理服务实现类（单 AccessToken 严格约束与 Redis 分布式锁加固版本）。
 *
 * <p>核心机制：
 * <ul>
 *   <li>短效 AccessToken 存入 {@code login:access:{token}}，默认 TTL 30 分钟</li>
 *   <li>长效 RefreshToken 存入 {@code login:refresh:{token}}，默认 TTL 7 天</li>
 *   <li><b>单 AccessToken 保障</b>：通过 {@code login:user:access:{userId}} 记录当前活跃的唯一 AccessToken，换新时物理淘汰旧 Token，防止内存泄露与 Token 泛滥</li>
 *   <li><b>分布式互斥锁（SETNX + Lua 脚本原子释放）</b>：并发换票时基于用户 ID 加互斥锁，抢锁失败优雅重试复用，彻底规避误删锁与并发击穿</li>
 *   <li>比对 DB 端的 {@code authVersion} 与 {@code enabled} 保证账号被禁用或修改密码时能即时作废</li>
 * </ul>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TokenServiceImpl implements TokenService {

    public static final String ACCESS_TOKEN_PREFIX = "login:access:";
    public static final String REFRESH_TOKEN_PREFIX = "login:refresh:";
    public static final String USER_ACCESS_PREFIX = "login:user:access:";
    public static final String LOCK_REFRESH_PREFIX = "lock:token:refresh:";

    public static final long ACCESS_EXPIRE_SECONDS = 30 * 60L; // 30 分钟
    public static final long REFRESH_EXPIRE_SECONDS = 7 * 24 * 3600L; // 7 天
    public static final long LOCK_TIMEOUT_SECONDS = 5L; // 分布式锁超时 5 秒

    /** Lua 脚本：只有持有者（UUID 匹配）才允许执行 DEL，保证分布式锁的原子安全释放 */
    private static final DefaultRedisScript<Long> UNLOCK_LUA_SCRIPT = new DefaultRedisScript<>(
            "if redis.call('get', KEYS[1]) == ARGV[1] then " +
            "    return redis.call('del', KEYS[1]) " +
            "else " +
            "    return 0 " +
            "end",
            Long.class
    );

    private final StringRedisTemplate redisTemplate;
    private final UserMapper userMapper;

    @Override
    public TokenResponse createTokens(AccountPrincipal principal) {
        Long userId = principal.id();

        // 1. 单 AccessToken 约束：检查该用户是否已有旧 AccessToken，若有立即物理删除
        String oldAccessToken = redisTemplate.opsForValue().get(USER_ACCESS_PREFIX + userId);
        if (oldAccessToken != null && !oldAccessToken.isBlank()) {
            redisTemplate.delete(ACCESS_TOKEN_PREFIX + oldAccessToken);
        }

        // 2. 签发新的 AccessToken 与 RefreshToken
        String accessToken = generateToken();
        String refreshToken = generateToken();
        LoginUser loginUser = LoginUser.fromPrincipal(principal);

        // 使用 Hutool JSONUtil 将对象转为 JSON 字符串
        String json = JSONUtil.toJsonStr(loginUser);

        // 3. 写入 Redis
        redisTemplate.opsForValue().set(ACCESS_TOKEN_PREFIX + accessToken, json, ACCESS_EXPIRE_SECONDS, TimeUnit.SECONDS);
        redisTemplate.opsForValue().set(USER_ACCESS_PREFIX + userId, accessToken, ACCESS_EXPIRE_SECONDS, TimeUnit.SECONDS);
        redisTemplate.opsForValue().set(REFRESH_TOKEN_PREFIX + refreshToken, json, REFRESH_EXPIRE_SECONDS, TimeUnit.SECONDS);

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
        // 使用 Hutool JSONUtil 将 JSON 还原为 LoginUser 实例
        try {
            return JSONUtil.toBean(json, LoginUser.class);
        } catch (Exception e) {
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
            cachedUser = JSONUtil.toBean(json, LoginUser.class);
        } catch (Exception e) {
            redisTemplate.delete(refreshKey);
            throw new Api.Failure(401, "无效的刷新令牌");
        }

        Long userId = cachedUser.getId();
        String lockKey = LOCK_REFRESH_PREFIX + userId;

        // ★ 工业级分布式自旋重试循环（最长容忍等待 3 秒，避免单次 sleep(150) 因网络抖动误判）
        long deadline = System.currentTimeMillis() + 3000L;
        while (System.currentTimeMillis() < deadline) {
            String lockVal = UUID.randomUUID().toString(); // 持有者唯一标识

            // 尝试通过 SETNX 抢锁
            Boolean locked = redisTemplate.opsForValue().setIfAbsent(lockKey, lockVal, LOCK_TIMEOUT_SECONDS, TimeUnit.SECONDS);

            if (Boolean.TRUE.equals(locked)) {
                try {
                    // 1. 数据库实时安全性双重校验（检查是否被封号或修改密码）
                    User current = userMapper.selectById(userId);
                    if (current == null || !Boolean.TRUE.equals(current.getEnabled()) || current.getAuthVersion() != cachedUser.getAuthVersion()) {
                        redisTemplate.delete(refreshKey);
                        throw new Api.Failure(401, "账号状态已变更，请重新登录");
                    }

                    // 2. 单 AccessToken 约束：清理该用户旧的 AccessToken，防止内存膨胀
                    String oldAccessToken = redisTemplate.opsForValue().get(USER_ACCESS_PREFIX + userId);
                    if (oldAccessToken != null && !oldAccessToken.isBlank()) {
                        redisTemplate.delete(ACCESS_TOKEN_PREFIX + oldAccessToken);
                    }

                    // 3. 签发唯一的全新 AccessToken
                    String newAccessToken = generateToken();
                    LoginUser updatedUser = LoginUser.fromPrincipal(AccountPrincipal.of(current));
                    String updatedJson = JSONUtil.toJsonStr(updatedUser);

                    // 4. 写入新 Token 并更新用户活跃 Token 映射
                    redisTemplate.opsForValue().set(ACCESS_TOKEN_PREFIX + newAccessToken, updatedJson, ACCESS_EXPIRE_SECONDS, TimeUnit.SECONDS);
                    redisTemplate.opsForValue().set(USER_ACCESS_PREFIX + userId, newAccessToken, ACCESS_EXPIRE_SECONDS, TimeUnit.SECONDS);

                    // 5. 滑动续签原 RefreshToken
                    redisTemplate.opsForValue().set(refreshKey, updatedJson, REFRESH_EXPIRE_SECONDS, TimeUnit.SECONDS);

                    return new TokenResponse(newAccessToken, refreshToken, ACCESS_EXPIRE_SECONDS, updatedUser);
                } finally {
                    // 6. 使用 Lua 脚本原子校验并释放锁，彻底杜绝误删他人的锁！
                    redisTemplate.execute(UNLOCK_LUA_SCRIPT, Collections.singletonList(lockKey), lockVal);
                }
            }

            // 未抢到锁说明并发线程正在处理：短暂休眠 80ms
            try {
                Thread.sleep(80);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }

            // 自旋检查：如果抢到锁的并发线程已经处理完成并产出了新 Token，直接复用返回！
            String activeAccessToken = redisTemplate.opsForValue().get(USER_ACCESS_PREFIX + userId);
            if (activeAccessToken != null) {
                String latestJson = redisTemplate.opsForValue().get(ACCESS_TOKEN_PREFIX + activeAccessToken);
                if (latestJson != null) {
                    LoginUser latestUser = JSONUtil.toBean(latestJson, LoginUser.class);
                    return new TokenResponse(activeAccessToken, refreshToken, ACCESS_EXPIRE_SECONDS, latestUser);
                }
            }
            // 若仍未产出，说明抢锁线程还在执行中（或抢锁线程超时异常），继续下一次循环重试抢锁或复用！
        }

        throw new Api.Failure(401, "系统繁忙，请重试");
    }

    @Override
    public void removeTokens(String accessToken, String refreshToken) {
        if (accessToken != null && !accessToken.isBlank()) {
            LoginUser user = getLoginUserByAccessToken(accessToken);
            if (user != null) {
                redisTemplate.delete(USER_ACCESS_PREFIX + user.getId());
            }
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


