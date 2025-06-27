package com.bytevault.app.auth.service;

import com.bytevault.app.auth.model.UserDetailsImpl;
import com.bytevault.app.model.Permission;
import com.bytevault.app.model.Role;
import com.bytevault.app.model.User;
import com.bytevault.app.service.RoleService;
import com.bytevault.app.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class UserDetailsServiceImpl implements UserDetailsService {

    private final UserService userService;
    private final RoleService roleService;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        // 加载用户基本信息
        User user = userService.getUserByUsername(username);
        if (user == null) {
            throw new UsernameNotFoundException("用户名不存在: " + username);
        }
        
        // 加载用户角色（角色中已包含权限）
        List<Role> roles = roleService.getRolesByUserId(user.getId());
        user.setRoles(roles);
        
        // 从角色中提取所有权限
        List<Permission> permissions = new ArrayList<>();
        roles.forEach(role -> {
            if (role.getPermissions() != null) {
                permissions.addAll(role.getPermissions());
            }
        });
        
        return new UserDetailsImpl(user, roles, permissions);
    }
} 