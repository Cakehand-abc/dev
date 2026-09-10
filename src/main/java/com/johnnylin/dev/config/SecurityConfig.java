package com.johnnylin.dev.config;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.johnnylin.dev.auth.AccountPrincipal;
import com.johnnylin.dev.domain.User;
import com.johnnylin.dev.mapper.UserMapper;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import org.springframework.context.annotation.*;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.*;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.*;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.*;
import org.springframework.security.web.access.intercept.AuthorizationFilter;
import org.springframework.security.web.context.*;
import org.springframework.security.web.csrf.*;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Spring Security 核心安全链配置与访问控制策略。
 *
 * <p>核心职责包括：
 * <ul>
 *   <li>配置密码散列器（BCrypt）、用户详情服务（UserDetailsService）与认证管理器（AuthenticationManager）</li>
 *   <li>基于 HttpSession 的安全上下文持久化（SecurityContextRepository）与 CSRF 令牌管理</li>
 *   <li>声明细粒度 RBAC 权限访问路由（管理员、操作员、分析员、设备端开放免密接口）</li>
 *   <li>统一未认证（401）与未授权（403）的 JSON 异常响应格式</li>
 *   <li>挂载账号状态校验过滤器，即时检测账号禁用或凭证版本（authVersion）变更并注销非法会话</li>
 * </ul>
 */
@Configuration
public class SecurityConfig {

    /**
     * 声明口令密码编码器。
     *
     * @return BCryptPasswordEncoder 实例，用于单向哈希加密与密码比对
     */
    @Bean
    PasswordEncoder passwords() {
        return new BCryptPasswordEncoder();
    }

    /**
     * 声明基于数据库查询的 Spring Security 用户认证详情服务。
     *
     * @param mapper 系统用户持久层 Mapper
     * @return UserDetailsService 实现，根据账号名加载对应的 {@link AccountPrincipal}
     */
    @Bean
    UserDetailsService users(UserMapper mapper) {
        return name -> {
            User u = mapper.selectOne(new QueryWrapper<User>().eq("username", name));
            if (u == null) {
                throw new UsernameNotFoundException("凭据错误");
            }
            return AccountPrincipal.of(u);
        };
    }

    /**
     * 构建认证管理器 AuthenticationManager。
     *
     * @param users 用户详情服务
     * @param passwords 密码哈希校验器
     * @return 包含 DaoAuthenticationProvider 的 ProviderManager 实例
     */
    @Bean
    AuthenticationManager authentication(UserDetailsService users, PasswordEncoder passwords) {
        var provider = new DaoAuthenticationProvider(users);
        provider.setPasswordEncoder(passwords);
        return new ProviderManager(provider);
    }

    /**
     * 声明基于 HttpSession 的安全上下文存储仓库。
     *
     * @return HttpSessionSecurityContextRepository 实例
     */
    @Bean
    SecurityContextRepository contexts() {
        return new HttpSessionSecurityContextRepository();
    }

    /**
     * 声明基于 HttpSession 的 CSRF 令牌存储仓库。
     *
     * @return HttpSessionCsrfTokenRepository 实例
     */
    @Bean
    CsrfTokenRepository csrfRepository() {
        return new HttpSessionCsrfTokenRepository();
    }

    /**
     * 向 HTTP 响应流写入统一格式的 JSON 错误体。
     *
     * @param response HTTP 响应对象
     * @param status HTTP 状态码（如 401、403）
     * @param message 错误提示信息
     * @throws IOException 当写入响应流失败时抛出
     */
    public static void error(HttpServletResponse response, int status, String message) throws IOException {
        response.setStatus(status);
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write("{\"code\":\"HTTP_" + status + "\",\"message\":\"" + message + "\",\"data\":null}");
    }

    /**
     * 构建 Spring Security 核心过滤链（SecurityFilterChain）。
     *
     * @param http HttpSecurity 配置构建器
     * @param users 系统用户持久层 Mapper
     * @param contexts 安全上下文仓库
     * @param csrf CSRF 令牌仓库
     * @return 构建就绪的 SecurityFilterChain
     * @throws Exception 当安全链配置异常时抛出
     */
    @Bean
    SecurityFilterChain chain(HttpSecurity http, UserMapper users, SecurityContextRepository contexts, CsrfTokenRepository csrf) throws Exception {
        http.securityContext(c -> c.securityContextRepository(contexts))
            // 针对 IoT 边缘设备端数据上报接口关闭 CSRF 防护，其余管理端接口强制启用
            .csrf(c -> c.csrfTokenRepository(csrf).ignoringRequestMatchers("/api/device/**"))
            .authorizeHttpRequests(a -> a
                // 静态资源与登录/CSRF免密端点放行
                .requestMatchers("/", "/index.html", "/assets/**", "/favicon.svg", "/api/v1/auth/csrf", "/api/v1/auth/login", "/error").permitAll()
                // IoT 边缘设备数据直传端点（基于设备 Key 自定义认证）
                .requestMatchers("/api/device/**").permitAll()
                // 敏感破坏性操作：删除老人档案仅限 ADMIN
                .requestMatchers(HttpMethod.DELETE, "/api/v1/elders/**").hasRole("ADMIN")
                // 用户管理、演示播种与设备凭据轮转仅限 ADMIN
                .requestMatchers("/api/v1/users/**", "/api/v1/demo/**", "/api/v1/devices/*/credential").hasRole("ADMIN")
                // 只读查询接口允许所有已认证用户（含 ANALYST）
                .requestMatchers(HttpMethod.GET, "/api/**").authenticated()
                // 退出登录端点需已认证
                .requestMatchers("/api/v1/auth/logout").authenticated()
                // 业务写操作限制为 ADMIN 与 OPERATOR 角色
                .requestMatchers("/api/**").hasAnyRole("ADMIN", "OPERATOR")
                // 其余任何未显式声明的请求一律拒绝
                .anyRequest().denyAll())
            .exceptionHandling(e -> e
                // 未认证或会话缺失时返回 401
                .authenticationEntryPoint((r, s, x) -> error(s, 401, "请先登录"))
                // 越权或安全令牌失效时返回 403 / 401
                .accessDeniedHandler((r, s, x) -> error(s, SecurityContextHolder.getContext().getAuthentication() == null ? 401 : 403, "无权限或安全令牌已失效")))
            .logout(l -> l.disable())
            .requestCache(c -> c.disable());

        // 挂载账号凭据版本与状态检查拦截过滤器，确保密码重置或角色变更后即时使老旧会话失效
        http.addFilterBefore(new OncePerRequestFilter() {
            @Override
            protected void doFilterInternal(HttpServletRequest req, HttpServletResponse res, FilterChain next) throws ServletException, IOException {
                var auth = SecurityContextHolder.getContext().getAuthentication();
                if (auth != null && auth.getPrincipal() instanceof AccountPrincipal p) {
                    User current = users.selectById(p.id());
                    // 若账号已不存在、被禁用，或内部安全版本号不一致，立即清理会话并拒绝请求
                    if (current == null || !current.getEnabled() || current.getAuthVersion() != p.authVersion()) {
                        SecurityContextHolder.clearContext();
                        if (req.getSession(false) != null) {
                            req.getSession(false).invalidate();
                        }
                        error(res, 401, "账号状态已变化，请重新登录");
                        return;
                    }
                }
                next.doFilter(req, res);
            }
        }, AuthorizationFilter.class);

        return http.build();
    }
}
