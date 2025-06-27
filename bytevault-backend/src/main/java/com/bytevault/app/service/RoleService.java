package com.bytevault.app.service;

import com.bytevault.app.model.Role;
import java.util.List;

public interface RoleService {
    
    /**
     * 获取所有角色
     */
    List<Role> getAllRoles();
    
    /**
     * 根据ID获取角色
     */
    Role getRoleById(Long id);
    
    /**
     * 根据名称获取角色
     */
    Role getRoleByName(String name);
    
    /**
     * 根据用户ID获取角色列表
     */
    List<Role> getRolesByUserId(Long userId);
    
    /**
     * 创建角色
     */
    Role createRole(Role role);
    
    /**
     * 更新角色
     */
    Role updateRole(Role role);
    
    /**
     * 删除角色
     */
    boolean deleteRole(Long id);
    
    /**
     * 为角色分配权限
     */
    boolean assignPermissionsToRole(Long roleId, List<Long> permissionIds);
    
    /**
     * 为用户分配角色
     */
    boolean assignRolesToUser(Long userId, List<Long> roleIds);
} 