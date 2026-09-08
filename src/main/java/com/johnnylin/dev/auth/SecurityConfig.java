package com.johnnylin.dev.auth;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.johnnylin.dev.mapper.UserMapper;
import com.johnnylin.dev.domain.User;
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

@Configuration
public class SecurityConfig {
    @Bean PasswordEncoder passwords(){return new BCryptPasswordEncoder();}
    @Bean UserDetailsService users(UserMapper mapper){return name->{
        User u=mapper.selectOne(new QueryWrapper<User>().eq("username",name));
        if(u==null)throw new UsernameNotFoundException("凭据错误");return AccountPrincipal.of(u);
    };}
    @Bean AuthenticationManager authentication(UserDetailsService users,PasswordEncoder passwords){
        var provider=new DaoAuthenticationProvider(users);provider.setPasswordEncoder(passwords);return new ProviderManager(provider);
    }
    @Bean SecurityContextRepository contexts(){return new HttpSessionSecurityContextRepository();}
    @Bean CsrfTokenRepository csrfRepository(){return new HttpSessionCsrfTokenRepository();}
    public static void error(HttpServletResponse response,int status,String message)throws IOException{
        response.setStatus(status);response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write("{\"code\":\"HTTP_"+status+"\",\"message\":\""+message+"\",\"data\":null}");
    }
    @Bean SecurityFilterChain chain(HttpSecurity http,UserMapper users,SecurityContextRepository contexts,CsrfTokenRepository csrf)throws Exception{
        http.securityContext(c->c.securityContextRepository(contexts))
            .csrf(c->c.csrfTokenRepository(csrf).ignoringRequestMatchers("/api/device/**"))
            .authorizeHttpRequests(a->a
                .requestMatchers("/","/index.html","/assets/**","/favicon.svg","/api/v1/auth/csrf","/api/v1/auth/login","/error").permitAll()
                .requestMatchers("/api/device/**").permitAll()
                .requestMatchers(HttpMethod.DELETE,"/api/v1/elders/**").hasRole("ADMIN")
                .requestMatchers("/api/v1/users/**","/api/v1/demo/**","/api/v1/devices/*/credential").hasRole("ADMIN")
                .requestMatchers(HttpMethod.GET,"/api/**").authenticated()
                .requestMatchers("/api/v1/auth/logout").authenticated()
                .requestMatchers("/api/**").hasAnyRole("ADMIN","OPERATOR")
                .anyRequest().denyAll())
            .exceptionHandling(e->e.authenticationEntryPoint((r,s,x)->error(s,401,"请先登录"))
                .accessDeniedHandler((r,s,x)->error(s,SecurityContextHolder.getContext().getAuthentication()==null?401:403,"无权限或安全令牌已失效")))
            .logout(l->l.disable())
            .requestCache(c->c.disable());
        http.addFilterBefore(new OncePerRequestFilter(){
            @Override protected void doFilterInternal(HttpServletRequest req,HttpServletResponse res,FilterChain next)throws ServletException,IOException{
                var auth=SecurityContextHolder.getContext().getAuthentication();
                if(auth!=null&&auth.getPrincipal() instanceof AccountPrincipal p){
                    User current=users.selectById(p.id());
                    if(current==null||!current.getEnabled()||current.getAuthVersion()!=p.authVersion()){
                        SecurityContextHolder.clearContext();if(req.getSession(false)!=null)req.getSession(false).invalidate();
                        error(res,401,"账号状态已变化，请重新登录");return;
                    }
                }
                next.doFilter(req,res);
            }
        },AuthorizationFilter.class);
        return http.build();
    }
}
