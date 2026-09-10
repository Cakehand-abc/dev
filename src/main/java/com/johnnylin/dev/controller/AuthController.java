package com.johnnylin.dev.controller;

import com.johnnylin.dev.auth.AccountPrincipal;
import com.johnnylin.dev.common.*;
import jakarta.servlet.http.*;
import org.springframework.security.authentication.*;
import org.springframework.security.core.*;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.security.web.csrf.*;
import org.springframework.security.web.authentication.session.ChangeSessionIdAuthenticationStrategy;
import org.springframework.web.bind.annotation.*;

import java.util.*;

/**
 * 用户身份认证与会话管理控制器。
 *
 * <p>提供基于标准 Cookie-Session + CSRF 令牌的安全认证接口，包括：
 * <ul>
 *   <li>获取 CSRF 令牌信息</li>
 *   <li>用户账号密码登录认证（集成防会话固定攻击防护）</li>
 *   <li>获取当前已登录用户的公开信息</li>
 *   <li>安全退出登录（作废 Session 并清理安全上下文）</li>
 * </ul>
 */
@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final AuthenticationManager manager;
    private final SecurityContextRepository contexts;
    private final CsrfTokenRepository csrf;
    private final com.johnnylin.dev.service.TokenService tokenService;

    /**
     * 构造认证控制器并注入依赖组件。
     *
     * @param manager Spring Security 认证管理器
     * @param contexts 安全上下文存储持久化仓库
     * @param csrf CSRF 令牌生成与存储仓库
     * @param tokenService 分布式双 Token 管理服务
     */
    public AuthController(AuthenticationManager manager, SecurityContextRepository contexts,
                          CsrfTokenRepository csrf, com.johnnylin.dev.service.TokenService tokenService) {
        this.manager = manager;
        this.contexts = contexts;
        this.csrf = csrf;
        this.tokenService = tokenService;
    }

    /**
     * 获取当前会话绑定的 CSRF 防护令牌及对应 Header 标头名称。
     *
     * @param token 由 Spring Security 过滤器自动注入的 CsrfToken 实例
     * @return 包含 token 字符串与 headerName 的响应体
     */
    @GetMapping("/csrf")
    public Api<?> csrf(CsrfToken token) {
        return Api.ok(Map.of("token", token.getToken(), "headerName", token.getHeaderName()));
    }

    /**
     * 执行用户登录认证并签发双 Token。
     *
     * <p>执行流程：
     * <ol>
     *   <li>校验入参仅包含 username 与 password，防止多余参数注入</li>
     *   <li>委托 {@link AuthenticationManager} 进行凭据匹配</li>
     *   <li>应用 {@link ChangeSessionIdAuthenticationStrategy} 防御会话固定攻击</li>
     *   <li>将认证结果保存到 HttpSession 与 {@link SecurityContextHolder}</li>
     *   <li>委托 {@link com.johnnylin.dev.service.TokenService} 签发 AccessToken 与 RefreshToken 并写入 Redis</li>
     *   <li>返回包含双 Token 及用户核心属性的组合响应实体</li>
     * </ol>
     *
     * @param body 包含 username 和 password 的 JSON 载荷
     * @param request HTTP 请求上下文
     * @param response HTTP 响应上下文
     * @return 包含双 Token 及登录用户公开信息的响应字典
     * @throws Api.Failure 当用户名或密码不匹配时抛出 401 异常
     */
    @PostMapping("/login")
    public Api<?> login(@RequestBody Map<String, Object> body, HttpServletRequest request, HttpServletResponse response) {
        Input.keys(body, "username", "password");
        String username = Input.text(body, "username", 64);
        String password = Input.text(body, "password", 72);
        try {
            // 验证用户名与口令
            Authentication auth = manager.authenticate(UsernamePasswordAuthenticationToken.unauthenticated(username, password));
            // 登录成功时轮转 Session ID，防御会话固定攻击
            new ChangeSessionIdAuthenticationStrategy().onAuthentication(auth, request, response);
            // 构建并持久化安全上下文
            var context = SecurityContextHolder.createEmptyContext();
            context.setAuthentication(auth);
            SecurityContextHolder.setContext(context);
            contexts.saveContext(context, request, response);
            // 重新刷新 CSRF 令牌
            csrf.saveToken(null, request, response);

            AccountPrincipal principal = (AccountPrincipal) auth.getPrincipal();
            var tokens = tokenService.createTokens(principal);
            return Api.ok(tokens.toResultMap());
        } catch (AuthenticationException e) {
            throw new Api.Failure(401, "用户名或密码错误");
        }
    }

    /**
     * 使用有效的 RefreshToken 无感置换新的 AccessToken。
     *
     * @param body 包含 refreshToken 键值的请求体载荷
     * @param request HTTP 请求对象
     * @return 包含新 AccessToken 与更新后有效期的响应实体
     */
    @PostMapping("/refresh")
    public Api<?> refresh(@RequestBody(required = false) Map<String, Object> body, HttpServletRequest request) {
        String refreshToken = null;
        if (body != null && body.containsKey("refreshToken")) {
            refreshToken = String.valueOf(body.get("refreshToken"));
        }
        if (refreshToken == null || refreshToken.isBlank()) {
            refreshToken = request.getHeader("X-Refresh-Token");
        }
        if (refreshToken == null || refreshToken.isBlank()) {
            throw new Api.Failure(401, "刷新令牌不能为空");
        }
        var refreshed = tokenService.refreshAccessToken(refreshToken.trim());
        return Api.ok(refreshed.toResultMap());
    }

    /**
     * 获取当前登录会话的主体信息。
     *
     * @param auth Spring Security 自动注入的当前认证信息
     * @return 当前登录用户的公开信息
     */
    @GetMapping("/me")
    public Api<?> me(Authentication auth) {
        if (auth == null || !(auth.getPrincipal() instanceof AccountPrincipal principal)) {
            throw new Api.Failure(401, "请先登录");
        }
        return Api.ok(principal.publicView());
    }

    /**
     * 注销当前用户登录会话并清理 Redis 中的双 Token。
     *
     * @param body 可选携带 refreshToken 的载荷
     * @param req HTTP 请求上下文
     * @param res HTTP 响应上下文
     * @return 成功响应（数据载荷为 null）
     */
    @PostMapping("/logout")
    public Api<?> logout(@RequestBody(required = false) Map<String, Object> body,
                         HttpServletRequest req, HttpServletResponse res) {
        String bearer = req.getHeader("Authorization");
        String accessToken = null;
        if (bearer != null && bearer.regionMatches(true, 0, "Bearer ", 0, 7)) {
            accessToken = bearer.substring(7).trim();
        }
        String refreshToken = null;
        if (body != null && body.containsKey("refreshToken")) {
            refreshToken = String.valueOf(body.get("refreshToken"));
        }
        tokenService.removeTokens(accessToken, refreshToken);

        csrf.saveToken(null, req, res);
        if (req.getSession(false) != null) {
            req.getSession(false).invalidate();
        }
        SecurityContextHolder.clearContext();
        return Api.ok(null);
    }
}
