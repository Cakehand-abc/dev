package com.johnnylin.dev.config;

import com.johnnylin.dev.auth.AccountPrincipal;
import com.johnnylin.dev.auth.LoginUser;
import com.johnnylin.dev.service.TokenService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * 分布式 Token 认证解析过滤器。
 *
 * <p>拦截并提取请求头 {@code Authorization: Bearer <token>} 中的 AccessToken，
 * 委托 {@link TokenService} 从 Redis 检索登录用户信息，并转换为 Spring Security 的
 * {@link UsernamePasswordAuthenticationToken} 注入安全上下文。
 *
 * <p>若请求未携带 Bearer 令牌，则跳过解析，交由后续安全过滤器（如 Session 机制或匿名访问策略）处理。
 */
@Component
public class TokenAuthenticationFilter extends OncePerRequestFilter {

    private final TokenService tokenService;

    public TokenAuthenticationFilter(TokenService tokenService) {
        this.tokenService = tokenService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String token = resolveToken(request);
        if (token != null && !token.isBlank()) {
            LoginUser loginUser = tokenService.getLoginUserByAccessToken(token);
            if (loginUser != null) {
                AccountPrincipal principal = loginUser.toPrincipal();
                UsernamePasswordAuthenticationToken authentication =
                        new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());
                SecurityContext context = SecurityContextHolder.createEmptyContext();
                context.setAuthentication(authentication);
                SecurityContextHolder.setContext(context);
            }
        }
        filterChain.doFilter(request, response);
    }

    /**
     * 从请求头或请求参数中提取访问令牌。
     *
     * @param request HTTP 请求对象
     * @return 提取出的令牌字符串，若未携带则返回 null
     */
    private String resolveToken(HttpServletRequest request) {
        String bearer = request.getHeader("Authorization");
        if (bearer != null && bearer.regionMatches(true, 0, "Bearer ", 0, 7)) {
            return bearer.substring(7).trim();
        }
        String tokenParam = request.getParameter("access_token");
        if (tokenParam != null && !tokenParam.isBlank()) {
            return tokenParam.trim();
        }
        return null;
    }
}
