package com.johnnylin.dev.auth;

import com.johnnylin.dev.domain.User;
import org.springframework.security.core.*;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import java.util.*;

public record AccountPrincipal(Long id,String username,String password,String role,String displayName,
                               boolean enabled,int authVersion) implements UserDetails {
    public static AccountPrincipal of(User u){return new AccountPrincipal(u.getId(),u.getUsername(),u.getPasswordHash(),u.getRole(),u.getDisplayName(),u.getEnabled(),u.getAuthVersion());}
    public Collection<? extends GrantedAuthority> getAuthorities(){return List.of(new SimpleGrantedAuthority("ROLE_"+role));}
    public String getPassword(){return password;}
    public String getUsername(){return username;}
    public boolean isEnabled(){return enabled;}
    @Override public String toString(){return "AccountPrincipal[id="+id+", username="+username+", role="+role+"]";}
    public Map<String,Object> publicView(){return Map.of("id",id,"username",username,"displayName",displayName,"role",role);}
}
