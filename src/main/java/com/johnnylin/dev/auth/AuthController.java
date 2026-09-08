package com.johnnylin.dev.auth;

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

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {
    private final AuthenticationManager manager;
    private final SecurityContextRepository contexts;
    private final CsrfTokenRepository csrf;
    public AuthController(AuthenticationManager manager,SecurityContextRepository contexts,CsrfTokenRepository csrf){this.manager=manager;this.contexts=contexts;this.csrf=csrf;}
    @GetMapping("/csrf") Api<?> csrf(CsrfToken token){return Api.ok(Map.of("token",token.getToken(),"headerName",token.getHeaderName()));}
    @PostMapping("/login") Api<?> login(@RequestBody Map<String,Object> body,HttpServletRequest request,HttpServletResponse response){
        Input.keys(body,"username","password");
        String username=Input.text(body,"username",64),password=Input.text(body,"password",72);
        try{
            Authentication auth=manager.authenticate(UsernamePasswordAuthenticationToken.unauthenticated(username,password));
            new ChangeSessionIdAuthenticationStrategy().onAuthentication(auth,request,response);
            var context=SecurityContextHolder.createEmptyContext();context.setAuthentication(auth);SecurityContextHolder.setContext(context);
            contexts.saveContext(context,request,response);csrf.saveToken(null,request,response);
            return Api.ok(((AccountPrincipal)auth.getPrincipal()).publicView());
        }catch(AuthenticationException e){throw new Api.Failure(401,"用户名或密码错误");}
    }
    @GetMapping("/me") Api<?> me(Authentication auth){return Api.ok(((AccountPrincipal)auth.getPrincipal()).publicView());}
    @PostMapping("/logout") Api<?> logout(HttpServletRequest req,HttpServletResponse res){
        csrf.saveToken(null,req,res);if(req.getSession(false)!=null)req.getSession(false).invalidate();SecurityContextHolder.clearContext();return Api.ok(null);
    }
}
