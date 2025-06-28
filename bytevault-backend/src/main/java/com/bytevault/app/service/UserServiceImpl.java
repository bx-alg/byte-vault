package com.bytevault.app.service;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.bytevault.app.mapper.UserMapper;
import com.bytevault.app.model.Role;
import com.bytevault.app.model.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements UserService {

    private final BCryptPasswordEncoder passwordEncoder;
    private RoleService roleService;

    public UserServiceImpl(BCryptPasswordEncoder passwordEncoder) {
        this.passwordEncoder = passwordEncoder;
    }

    @Autowired
    public void setRoleService(@Lazy RoleService roleService) {
        this.roleService = roleService;
        
    }

    @Override
    public List<User> getAllUsers() {
        // 获取所有用户
        List<User> users = this.list();

        // 为每个用户加载角色信息
        for (User user : users) {
            List<Role> roles = roleService.getRolesByUserId(user.getId());
            user.setRoles(roles);
        }

        return users;
    }

    @Override
    public User getUserByUsername(String username) {
        return this.lambdaQuery()
                .eq(User::getUsername, username)
                .one();
    }

    @Override
    public User getUserById(Long id) {
        return this.getById(id);
    }

    @Override
    @Transactional
    public User addUser(User user) {
        // 设置创建和更新时间
        LocalDateTime now = LocalDateTime.now();
        user.setCreateTime(now);
        user.setUpdateTime(now);

        // 加密密码
        user.setPassword(passwordEncoder.encode(user.getPassword()));

        // 保存用户
        this.save(user);
        return user;
    }

    @Override
    @Transactional
    public User updateUser(User user) {
        // 设置更新时间
        user.setUpdateTime(LocalDateTime.now());

        // 如果密码被修改，需要重新加密
        User existingUser = this.getById(user.getId());
        if (existingUser != null && !existingUser.getPassword().equals(user.getPassword())) {
            user.setPassword(passwordEncoder.encode(user.getPassword()));
        }

        // 更新用户
        this.updateById(user);
        return user;
    }

    @Override
    @Transactional
    public boolean deleteUser(Long id) {
        return this.removeById(id);
    }

    @Override
    public boolean isUsernameExists(String username) {
        return this.lambdaQuery()
                .eq(User::getUsername, username)
                .count() > 0;
    }

    @Override
    public User getUserWithRoles(Long id) {
        User user = this.getById(id);
        if (user != null) {
            List<Role> roles = roleService.getRolesByUserId(id);
            user.setRoles(roles);
        }
        return user;
    }

    @Override
    public boolean existsById(Long id) {
        return this.getById(id) != null;
    }
}