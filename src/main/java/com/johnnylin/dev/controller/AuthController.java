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

    /**
     * 构造认证控制器并注入依赖组件。
     *
     * @param manager Spring Security 认证管理器
     * @param contexts 安全上下文存储持久化仓库
     * @param csrf CSRF 令牌生成与存储仓库
     */
    public AuthController(AuthenticationManager manager, SecurityContextRepository contexts, CsrfTokenRepository csrf) {
        this.manager = manager;
        this.contexts = contexts;
        this.csrf = csrf;
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
     * 执行用户登录认证。
     *
     * <p>执行流程：
     * <ol>
     *   <li>校验入参仅包含 username 与 password，防止多余参数注入</li>
     *   <li>委托 {@link AuthenticationManager} 进行凭据匹配</li>
     *   <li>应用 {@link ChangeSessionIdAuthenticationStrategy} 防御会话固定攻击 (Session Fixation)</li>
     *   <li>将认证结果保存到 HttpSession 与 {@link SecurityContextHolder}</li>
     *   <li>重置 CSRF 令牌，避免会话提升前后使用旧令牌</li>
     * </ol>
     *
     * @param body 包含 username 和 password 的 JSON 载荷
     * @param request HTTP 请求上下文
     * @param response HTTP 响应上下文
     * @return 登录成功用户的公开信息（ID、用户名、姓名、角色）
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
            return Api.ok(((AccountPrincipal) auth.getPrincipal()).publicView());
        } catch (AuthenticationException e) {
            throw new Api.Failure(401, "用户名或密码错误");
        }
    }

    /**
     * 获取当前登录会话的主体信息。
     *
     * @param auth Spring Security 自动注入的当前认证信息
     * @return 当前登录用户的公开信息
     */
    @GetMapping("/me")
    public Api<?> me(Authentication auth) {
        return Api.ok(((AccountPrincipal) auth.getPrincipal()).publicView());
    }

    /**
     * 注销当前用户登录会话。
     *
     * <p>清理当前会话的 CSRF 令牌、使现有 HttpSession 失效，并清空当前线程的 {@link SecurityContextHolder}。
     *
     * @param req HTTP 请求上下文
     * @param res HTTP 响应上下文
     * @return 成功响应（数据载荷为 null）
     */
    @PostMapping("/logout")
    public Api<?> logout(HttpServletRequest req, HttpServletResponse res) {
        csrf.saveToken(null, req, res);
        if (req.getSession(false) != null) {
            req.getSession(false).invalidate();
        }
        SecurityContextHolder.clearContext();
        return Api.ok(null);
    }
}
