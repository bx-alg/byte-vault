package com.bytevault.app.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.bytevault.app.mapper.PermissionMapper;
import com.bytevault.app.mapper.RoleMapper;
import com.bytevault.app.model.Permission;
import com.bytevault.app.model.Role;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
public class RoleServiceImpl extends ServiceImpl<RoleMapper, Role> implements RoleService {

    private final RoleMapper roleMapper;
    private final PermissionMapper permissionMapper;
    private final JdbcTemplate jdbcTemplate;

    public RoleServiceImpl(RoleMapper roleMapper, PermissionMapper permissionMapper, JdbcTemplate jdbcTemplate) {
        this.roleMapper = roleMapper;
        this.permissionMapper = permissionMapper;
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public List<Role> getAllRoles() {
        List<Role> roles = this.list();
        // 加载每个角色的权限
        roles.forEach(this::loadRolePermissions);
        return roles;
    }

    @Override
    public Role getRoleById(Long id) {
        Role role = this.getById(id);
        if (role != null) {
            loadRolePermissions(role);
        }
        return role;
    }

    @Override
    public Role getRoleByName(String name) {
        LambdaQueryWrapper<Role> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Role::getName, name);
        Role role = this.getOne(wrapper);
        if (role != null) {
            loadRolePermissions(role);
        }
        return role;
    }

    @Override
    public List<Role> getRolesByUserId(Long userId) {
        List<Role> roles = roleMapper.selectRolesByUserId(userId);
        // 加载每个角色的权限
        roles.forEach(this::loadRolePermissions);
        return roles;
    }

    /**
     * 加载角色的权限
     */
    private void loadRolePermissions(Role role) {
        List<Permission> permissions = permissionMapper.selectPermissionsByRoleId(role.getId());
        role.setPermissions(permissions);
    }

    @Override
    @Transactional
    public Role createRole(Role role) {
        this.save(role);
        return role;
    }

    @Override
    @Transactional
    public Role updateRole(Role role) {
        this.updateById(role);
        return role;
    }

    @Override
    @Transactional
    public boolean deleteRole(Long id) {
        // 先删除角色-权限关联
        jdbcTemplate.update("DELETE FROM role_permission WHERE role_id = ?", id);
        // 再删除用户-角色关联
        jdbcTemplate.update("DELETE FROM user_role WHERE role_id = ?", id);
        // 最后删除角色
        return this.removeById(id);
    }

    @Override
    @Transactional
    public boolean assignPermissionsToRole(Long roleId, List<Long> permissionIds) {
        try {
            // 先删除已有的角色-权限关联
            jdbcTemplate.update("DELETE FROM role_permission WHERE role_id = ?", roleId);
            
            // 批量插入新的角色-权限关联
            if (permissionIds != null && !permissionIds.isEmpty()) {
                List<Object[]> batchArgs = new ArrayList<>();
                for (Long permissionId : permissionIds) {
                    batchArgs.add(new Object[]{roleId, permissionId});
                }
                jdbcTemplate.batchUpdate("INSERT INTO role_permission (role_id, permission_id) VALUES (?, ?)", batchArgs);
            }
            return true;
        } catch (Exception e) {
            log.error("为角色分配权限失败", e);
            return false;
        }
    }

    @Override
    @Transactional
    public boolean assignRolesToUser(Long userId, List<Long> roleIds) {
        try {
            // 先删除已有的用户-角色关联
            jdbcTemplate.update("DELETE FROM user_role WHERE user_id = ?", userId);
            
            // 批量插入新的用户-角色关联
            if (roleIds != null && !roleIds.isEmpty()) {
                List<Object[]> batchArgs = new ArrayList<>();
                for (Long roleId : roleIds) {
                    batchArgs.add(new Object[]{userId, roleId});
                }
                jdbcTemplate.batchUpdate("INSERT INTO user_role (user_id, role_id) VALUES (?, ?)", batchArgs);
            }
            return true;
        } catch (Exception e) {
            log.error("为用户分配角色失败", e);
            return false;
        }
    }
} 