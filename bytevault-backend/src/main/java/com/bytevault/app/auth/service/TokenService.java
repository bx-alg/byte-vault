package com.bytevault.app.auth.service;

import com.bytevault.app.model.User;

/**
 * 令牌服务接口
 */
public interface TokenService {

    /**
     * 创建令牌
     *
     * @param user 用户
     * @param device 设备信息
     * @param ip IP地址
     * @return 令牌
     */
    String createToken(User user, String device, String ip);

    /**
     * 验证令牌
     *
     * @param token 令牌
     * @return 是否有效
     */
    boolean validateToken(String token);

    /**
     * 刷新令牌
     *
     * @param token 原令牌
     * @return 新令牌
     */
    String refreshToken(String token);

    /**
     * 使令牌失效
     *
     * @param token 令牌
     */
    void invalidateToken(String token);

    /**
     * 使用户的所有令牌失效
     *
     * @param userId 用户ID
     */
    void invalidateUserTokens(Long userId);

    /**
     * 封禁用户
     *
     * @param userId 用户ID
     * @param reason 封禁原因
     * @param minutes 封禁时间（分钟）
     */
    void banUser(Long userId, String reason, int minutes);

    /**
     * 检查用户是否被封禁
     *
     * @param userId 用户ID
     * @return 封禁信息，如果未被封禁则返回null
     */
    String getUserBanInfo(Long userId);
} 