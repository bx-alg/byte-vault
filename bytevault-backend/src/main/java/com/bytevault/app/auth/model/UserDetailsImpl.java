package com.bytevault.app.auth.model;

import com.bytevault.app.model.Permission;
import com.bytevault.app.model.Role;
import com.bytevault.app.model.User;
import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Getter
public class UserDetailsImpl implements UserDetails {

    private static final long serialVersionUID = 1L;

    private final User user;
    private final Collection<? extends GrantedAuthority> authorities;

    public UserDetailsImpl(User user, List<Role> roles, List<Permission> permissions) {
        this.user = user;
        
        // 收集所有权限（包括角色和具体权限）
        Set<GrantedAuthority> auths = new HashSet<>();
        
        // 添加角色权限，格式为：ROLE_角色名
        if (roles != null) {
            auths.addAll(roles.stream()
                    .map(role -> new SimpleGrantedAuthority("ROLE_" + role.getName()))
                    .collect(Collectors.toList()));
        }
        
        // 添加具体权限（去重）
        if (permissions != null) {
            auths.addAll(permissions.stream()
                    .map(permission -> new SimpleGrantedAuthority(permission.getName()))
                    .collect(Collectors.toList()));
        }
        
        this.authorities = new ArrayList<>(auths);
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return authorities;
    }

    @Override
    public String getPassword() {
        return user.getPassword();
    }

    @Override
    public String getUsername() {
        return user.getUsername();
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return user.getStatus() == 1;
    }

    public Long getId() {
        return user.getId();
    }
} 