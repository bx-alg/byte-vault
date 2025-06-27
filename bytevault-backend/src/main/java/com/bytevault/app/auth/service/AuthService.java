package com.bytevault.app.auth.service;

import com.bytevault.app.auth.model.LoginResponse;
import com.bytevault.app.auth.model.RegisterRequest;
import com.bytevault.app.model.User;

public interface AuthService {
    
    /**
     * 用户登录
     * 
     * @param username 用户名
     * @param password 密码
     * @return 登录响应，包含token和用户信息
     */
    LoginResponse login(String username, String password);
    
    /**
     * 获取当前登录用户
     * 
     * @return 当前用户信息
     */
    User getCurrentUser();
    
    /**
     * 用户登出
     */
    void logout();
    
    /**
     * 用户注册
     * 
     * @param registerRequest 注册请求
     * @return 注册成功的用户
     */
    User register(RegisterRequest registerRequest);
}
