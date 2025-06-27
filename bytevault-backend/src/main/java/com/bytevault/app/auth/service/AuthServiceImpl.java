package com.bytevault.app.auth.service;

import com.bytevault.app.auth.model.LoginResponse;
import com.bytevault.app.auth.model.RegisterRequest;
import com.bytevault.app.auth.model.UserDetailsImpl;
import com.bytevault.app.model.User;
import com.bytevault.app.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.servlet.http.HttpServletRequest;
import java.util.Objects;

@Slf4j
@Service
public class AuthServiceImpl implements AuthService {

    private final UserService userService;
    private final TokenService tokenService;
    private final AuthenticationManager authenticationManager;
    private final BCryptPasswordEncoder passwordEncoder;

    public AuthServiceImpl(
            UserService userService, 
            TokenService tokenService, 
            AuthenticationManager authenticationManager, 
            BCryptPasswordEncoder passwordEncoder) {
        this.userService = userService;
        this.tokenService = tokenService;
        this.authenticationManager = authenticationManager;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public LoginResponse login(String username, String password) {
        try {
            // 使用Spring Security的认证管理器进行认证
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(username, password));
            
            // 认证成功后，将认证信息设置到安全上下文中
            SecurityContextHolder.getContext().setAuthentication(authentication);
            
            // 从认证对象中获取用户详情
            UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();
            User user = userDetails.getUser();
            
            // 获取客户端信息
            String device = getClientDevice();
            String ip = getClientIp();
            
            // 使用TokenService创建令牌
            String token = tokenService.createToken(user, device, ip);
            
            // 返回登录响应
            return LoginResponse.builder()
                    .token(token)
                    .user(user)
                    .build();
        } catch (BadCredentialsException e) {
            throw new BadCredentialsException("用户名或密码错误");
        }
    }

    @Override
    public User getCurrentUser() {
        // 从安全上下文中获取当前认证信息
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new BadCredentialsException("用户未登录或登录已过期");
        }
        
        // 如果认证主体是UserDetailsImpl类型，直接获取用户信息
        if (authentication.getPrincipal() instanceof UserDetailsImpl) {
            return ((UserDetailsImpl) authentication.getPrincipal()).getUser();
        }
        
        // 如果是字符串类型（用户名），则通过用户名查询用户
        if (authentication.getPrincipal() instanceof String) {
            String username = (String) authentication.getPrincipal();
            User user = userService.getUserByUsername(username);
            if (user == null) {
                throw new BadCredentialsException("用户不存在");
            }
            return user;
        }
        
        throw new BadCredentialsException("无法获取当前用户信息");
    }

    @Override
    public void logout() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.isAuthenticated()) {
            // 获取当前的JWT令牌
            HttpServletRequest request = ((ServletRequestAttributes) RequestContextHolder.currentRequestAttributes()).getRequest();
            String authHeader = request.getHeader("Authorization");
            if (authHeader != null && authHeader.startsWith("Bearer ")) {
                String token = authHeader.substring(7);
                // 使令牌失效
                tokenService.invalidateToken(token);
            }
        }
        
        // 清除安全上下文
        SecurityContextHolder.clearContext();
    }
    
    @Override
    @Transactional
    public User register(RegisterRequest registerRequest) {
        log.info("开始注册用户: {}", registerRequest.getUsername());
        
        // 验证密码是否一致
        if (!Objects.equals(registerRequest.getPassword(), registerRequest.getConfirmPassword())) {
            throw new IllegalArgumentException("两次输入的密码不一致");
        }
        
        // 检查用户名是否已存在
        if (userService.isUsernameExists(registerRequest.getUsername())) {
            throw new IllegalArgumentException("用户名已存在");
        }
        
        // 创建用户对象
        User user = User.builder()
                .username(registerRequest.getUsername())
                .password(registerRequest.getPassword()) // 密码会在UserService中加密
                .status(1) // 默认状态为启用
                .build();
        
        // 保存用户
        User savedUser = userService.addUser(user);
        log.info("用户注册成功: {}", savedUser.getUsername());
        
        return savedUser;
    }
    
    /**
     * 获取客户端设备信息
     */
    private String getClientDevice() {
        HttpServletRequest request = ((ServletRequestAttributes) RequestContextHolder.currentRequestAttributes()).getRequest();
        String userAgent = request.getHeader("User-Agent");
        return userAgent != null ? userAgent : "未知设备";
    }
    
    /**
     * 获取客户端IP地址
     */
    private String getClientIp() {
        HttpServletRequest request = ((ServletRequestAttributes) RequestContextHolder.currentRequestAttributes()).getRequest();
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("Proxy-Client-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("WL-Proxy-Client-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("HTTP_X_FORWARDED_FOR");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("HTTP_X_FORWARDED");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("HTTP_X_CLUSTER_CLIENT_IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("HTTP_CLIENT_IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("HTTP_FORWARDED_FOR");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("HTTP_FORWARDED");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("HTTP_VIA");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("REMOTE_ADDR");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        return ip;
    }
}
