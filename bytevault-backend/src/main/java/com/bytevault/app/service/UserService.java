package com.bytevault.app.service;

import com.bytevault.app.model.User;
import java.util.List;

/**
 * 用户服务接口
 */
public interface UserService {
    
    /**
     * 获取所有用户
     * 
     * @return 用户列表
     */
    List<User> getAllUsers();
    
    /**
     * 根据用户名获取用户
     * 
     * @param username 用户名
     * @return 用户对象，如果不存在则返回null
     */
    User getUserByUsername(String username);
    
    /**
     * 根据ID获取用户
     * 
     * @param id 用户ID
     * @return 用户对象，如果不存在则返回null
     */
    User getUserById(Long id);
    
    /**
     * 添加新用户
     * 
     * @param user 用户对象
     * @return 添加后的用户（包含ID）
     */
    User addUser(User user);
    
    /**
     * 更新用户信息
     * 
     * @param user 需要更新的用户对象
     * @return 更新后的用户
     */
    User updateUser(User user);
    
    /**
     * 删除用户
     * 
     * @param id 用户ID
     * @return 是否删除成功
     */
    boolean deleteUser(Long id);
    
    /**
     * 检查用户名是否已存在
     * 
     * @param username 用户名
     * @return 是否存在
     */
    boolean isUsernameExists(String username);
    
    /**
     * 获取包含角色信息的用户
     * 
     * @param id 用户ID
     * @return 包含角色信息的用户对象
     */
    User getUserWithRoles(Long id);
    
    /**
     * 检查用户ID是否存在
     * 
     * @param id 用户ID
     * @return 是否存在
     */
    boolean existsById(Long id);
} 