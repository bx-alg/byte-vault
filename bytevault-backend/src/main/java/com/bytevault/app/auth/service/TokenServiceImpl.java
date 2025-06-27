package com.bytevault.app.auth.service;

import com.bytevault.app.auth.config.JwtUtils;
import com.bytevault.app.model.User;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
public class TokenServiceImpl implements TokenService {

    private static final String BAN_USER_KEY_PREFIX = "ban:user:";
    private static final String LOGIN_TOKEN_KEY_PREFIX = "login:token:";
    private static final String LOGIN_USER_TOKENS_KEY_PREFIX = "login:user:tokens:";
    
    // JWT默认有效期为1天（毫秒）
    private static final long JWT_EXPIRATION_TIME = 24 * 60 * 60 * 1000;
    // 自动续期阈值为30分钟（毫秒）
    private static final long JWT_REFRESH_THRESHOLD = 30 * 60 * 1000;

    private final RedisTemplate<String, Object> redisTemplate;
    private final JwtUtils jwtUtils;
    private final ObjectMapper objectMapper;

    public TokenServiceImpl(RedisTemplate<String, Object> redisTemplate, JwtUtils jwtUtils, ObjectMapper objectMapper) {
        this.redisTemplate = redisTemplate;
        this.jwtUtils = jwtUtils;
        this.objectMapper = objectMapper;
    }

    @Override
    public String createToken(User user, String device, String ip) {
        // 检查用户是否被封禁
        String banInfo = getUserBanInfo(user.getId());
        if (banInfo != null) {
            throw new RuntimeException("用户已被封禁: " + banInfo);
        }

        // 创建JWT令牌
        String token = jwtUtils.generateToken(user);

        // 存储令牌信息到Redis
        saveTokenInfo(token, user.getId(), device, ip);

        return token;
    }

    @Override
    public boolean validateToken(String token) {
        // 先检查令牌格式是否正确
        if (!jwtUtils.validateToken(token)) {
            return false;
        }

        // 获取用户ID
        Long userId = jwtUtils.getUserIdFromToken(token);
        if (userId == null) {
            return false;
        }

        // 检查用户是否被封禁
        if (getUserBanInfo(userId) != null) {
            return false;
        }

        // 检查令牌是否存在于Redis中
        String tokenKey = LOGIN_TOKEN_KEY_PREFIX + token;
        return Boolean.TRUE.equals(redisTemplate.hasKey(tokenKey));
    }

    @Override
    public String refreshToken(String token) {
        // 验证令牌有效性
        if (!validateToken(token)) {
            throw new RuntimeException("无效的令牌");
        }

        // 检查是否需要刷新令牌
        if (!needRefresh(token)) {
            return token;
        }

        // 获取令牌信息
        String tokenKey = LOGIN_TOKEN_KEY_PREFIX + token;
        Object tokenInfoObj = redisTemplate.opsForValue().get(tokenKey);
        if (tokenInfoObj == null) {
            throw new RuntimeException("令牌信息不存在");
        }

        // 解析令牌信息
        Map<String, Object> tokenInfo;
        try {
            tokenInfo = objectMapper.readValue(tokenInfoObj.toString(), Map.class);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("解析令牌信息失败", e);
        }

        // 获取用户ID
        Long userId = Long.valueOf(tokenInfo.get("userId").toString());
        String device = (String) tokenInfo.get("device");
        String ip = (String) tokenInfo.get("ip");

        // 使旧令牌失效
        invalidateToken(token);

        // 创建新令牌
        User user = new User();
        user.setId(userId);
        String newToken = jwtUtils.generateToken(user);

        // 存储新令牌信息
        saveTokenInfo(newToken, userId, device, ip);

        return newToken;
    }

    @Override
    public void invalidateToken(String token) {
        // 获取用户ID
        Long userId = jwtUtils.getUserIdFromToken(token);
        if (userId == null) {
            return;
        }

        // 删除令牌信息
        String tokenKey = LOGIN_TOKEN_KEY_PREFIX + token;
        redisTemplate.delete(tokenKey);

        // 从用户令牌集合中移除
        String userTokensKey = LOGIN_USER_TOKENS_KEY_PREFIX + userId;
        redisTemplate.opsForSet().remove(userTokensKey, token);
    }

    @Override
    public void invalidateUserTokens(Long userId) {
        // 获取用户的所有令牌
        String userTokensKey = LOGIN_USER_TOKENS_KEY_PREFIX + userId;
        Set<Object> tokens = redisTemplate.opsForSet().members(userTokensKey);
        if (tokens == null || tokens.isEmpty()) {
            return;
        }

        // 删除每个令牌
        for (Object tokenObj : tokens) {
            String token = tokenObj.toString();
            String tokenKey = LOGIN_TOKEN_KEY_PREFIX + token;
            redisTemplate.delete(tokenKey);
        }

        // 清空用户令牌集合
        redisTemplate.delete(userTokensKey);
    }

    @Override
    public void banUser(Long userId, String reason, int minutes) {
        String banKey = BAN_USER_KEY_PREFIX + userId;
        redisTemplate.opsForValue().set(banKey, reason, minutes, TimeUnit.MINUTES);
        
        // 使该用户的所有令牌失效
        invalidateUserTokens(userId);
    }

    @Override
    public String getUserBanInfo(Long userId) {
        String banKey = BAN_USER_KEY_PREFIX + userId;
        Object banInfo = redisTemplate.opsForValue().get(banKey);
        return banInfo != null ? banInfo.toString() : null;
    }

    /**
     * 检查令牌是否需要刷新
     *
     * @param token 令牌
     * @return 是否需要刷新
     */
    private boolean needRefresh(String token) {
        long expirationTime = jwtUtils.getExpirationDateFromToken(token).getTime();
        long currentTime = System.currentTimeMillis();
        
        // 如果过期时间小于当前时间加上刷新阈值，则需要刷新
        return expirationTime < currentTime + JWT_REFRESH_THRESHOLD;
    }

    /**
     * 保存令牌信息到Redis
     *
     * @param token 令牌
     * @param userId 用户ID
     * @param device 设备信息
     * @param ip IP地址
     */
    private void saveTokenInfo(String token, Long userId, String device, String ip) {
        // 创建令牌信息
        Map<String, Object> tokenInfo = new HashMap<>();
        tokenInfo.put("userId", userId);
        tokenInfo.put("device", device);
        tokenInfo.put("ip", ip);

        try {
            // 将令牌信息存储到Redis
            String tokenKey = LOGIN_TOKEN_KEY_PREFIX + token;
            String tokenInfoJson = objectMapper.writeValueAsString(tokenInfo);
            redisTemplate.opsForValue().set(tokenKey, tokenInfoJson, JWT_EXPIRATION_TIME, TimeUnit.MILLISECONDS);

            // 将令牌添加到用户令牌集合
            String userTokensKey = LOGIN_USER_TOKENS_KEY_PREFIX + userId;
            redisTemplate.opsForSet().add(userTokensKey, token);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("序列化令牌信息失败", e);
        }
    }

    /**
     * 定时清理过期令牌
     * 每小时执行一次
     */
    @Scheduled(fixedRate = 60 * 60 * 1000)
    public void cleanExpiredTokens() {
        log.info("开始清理过期令牌");
        
        // 获取所有用户令牌集合的key
        Set<String> userTokensKeys = redisTemplate.keys(LOGIN_USER_TOKENS_KEY_PREFIX + "*");
        if (userTokensKeys == null || userTokensKeys.isEmpty()) {
            return;
        }

        // 遍历每个用户的令牌集合
        for (String userTokensKey : userTokensKeys) {
            Set<Object> tokens = redisTemplate.opsForSet().members(userTokensKey);
            if (tokens == null || tokens.isEmpty()) {
                continue;
            }

            // 检查每个令牌是否过期
            for (Object tokenObj : tokens) {
                String token = tokenObj.toString();
                String tokenKey = LOGIN_TOKEN_KEY_PREFIX + token;
                
                // 如果令牌信息不存在，则从用户令牌集合中移除
                if (!Boolean.TRUE.equals(redisTemplate.hasKey(tokenKey))) {
                    redisTemplate.opsForSet().remove(userTokensKey, token);
                    log.debug("移除过期令牌: {}", token);
                }
            }
        }
        
        log.info("过期令牌清理完成");
    }
} 