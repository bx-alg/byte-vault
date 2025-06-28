package com.bytevault.app.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import com.bytevault.app.auth.service.AuthService;
import com.bytevault.app.model.AvatarUploadResponse;
import com.bytevault.app.model.User;
import com.bytevault.app.service.UserService;

import io.minio.*;
import io.minio.http.Method;

import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import org.springframework.beans.factory.annotation.Value;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;
    private final AuthService authService;
    private final MinioClient minioClient;
    
    @Value("${minio.bucketName}")
    private String avatarBucket;

    @GetMapping
    public ResponseEntity<List<User>> getAllUsers() {
        return ResponseEntity.ok(userService.getAllUsers());
    }

    @GetMapping("/{id}")
    public ResponseEntity<User> getUserById(@PathVariable Long id) {
        User user = userService.getUserById(id);
        if (user == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(user);
    }
    
    @GetMapping("/username/{username}")
    public ResponseEntity<User> getUserByUsername(@PathVariable String username) {
        User user = userService.getUserByUsername(username);
        if (user == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(user);
    }

    @PostMapping
    public ResponseEntity<User> addUser(@RequestBody User user) {
        // 检查用户名是否已存在
        if (userService.isUsernameExists(user.getUsername())) {
            return ResponseEntity.status(HttpStatus.CONFLICT).build();
        }
        
        User savedUser = userService.addUser(user);
        return ResponseEntity.status(HttpStatus.CREATED).body(savedUser);
    }
    
    @PutMapping("/{id}")
    public ResponseEntity<User> updateUser(@PathVariable Long id, @RequestBody User user) {
        User existingUser = userService.getUserById(id);
        if (existingUser == null) {
            return ResponseEntity.notFound().build();
        }
        
        // 设置ID确保更新的是正确的记录
        user.setId(id);
        User updatedUser = userService.updateUser(user);
        
        return ResponseEntity.ok(updatedUser);
    }
    
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteUser(@PathVariable Long id) {
        if (userService.getUserById(id) == null) {
            return ResponseEntity.notFound().build();
        }
        
        boolean deleted = userService.deleteUser(id);
        if (deleted) {
            return ResponseEntity.noContent().build();
        } else {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    @GetMapping("/exists/{username}")
    public ResponseEntity<Boolean> checkUsernameExists(@PathVariable String username) {
        return ResponseEntity.ok(userService.isUsernameExists(username));
    }

    /**
     * 上传用户头像
     *
     * @param file 头像文件
     * @return 上传结果
     */
    @PostMapping(value = "/avatar", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> uploadAvatar(@RequestParam("file") MultipartFile file) {
        try {
            if (file.isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("message", "请选择一个文件上传"));
            }

            // 获取当前登录用户
            User currentUser = authService.getCurrentUser();
            if (currentUser == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("message", "用户未登录"));
            }

            // 验证文件类型
            String contentType = file.getContentType();
            if (contentType == null || !contentType.startsWith("image/")) {
                return ResponseEntity.badRequest().body(Map.of("message", "只支持上传图片文件"));
            }

            // 生成唯一文件名
            String originalFilename = file.getOriginalFilename();
            String fileExtension = "";
            if (originalFilename != null && originalFilename.contains(".")) {
                fileExtension = originalFilename.substring(originalFilename.lastIndexOf("."));
            }
            String objectName = "avatar_" + currentUser.getId() + "_" + UUID.randomUUID() + fileExtension;

            // 上传文件到MinIO的头像桶
            InputStream inputStream = file.getInputStream();
            minioClient.putObject(
                    PutObjectArgs.builder()
                            .bucket(avatarBucket)
                            .object(objectName)
                            .stream(inputStream, file.getSize(), -1)
                            .contentType(contentType)
                            .build());

            // 删除旧头像（如果存在）
            if (currentUser.getAvatarUrl() != null && !currentUser.getAvatarUrl().isEmpty()) {
                try {
                    String oldObjectName = extractObjectNameFromUrl(currentUser.getAvatarUrl());
                    if (oldObjectName != null) {
                        minioClient.removeObject(
                                RemoveObjectArgs.builder()
                                        .bucket(avatarBucket)
                                        .object(oldObjectName)
                                        .build());
                    }
                } catch (Exception e) {
                    log.warn("删除旧头像失败: {}", e.getMessage());
                }
            }

            // 生成访问URL
            String avatarUrl = "/api/users/avatar/" + objectName;
            
            // 更新用户头像URL
            currentUser.setAvatarUrl(avatarUrl);
            userService.updateUser(currentUser);

            log.info("用户 {} 的头像已更新: {}", currentUser.getUsername(), avatarUrl);

            return ResponseEntity.ok(AvatarUploadResponse.builder()
                    .avatarUrl(avatarUrl)
                    .message("头像上传成功")
                    .build());
        } catch (Exception e) {
            log.error("头像上传失败: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "头像上传失败: " + e.getMessage()));
        }
    }

    /**
     * 删除用户头像
     *
     * @return 删除结果
     */
    @DeleteMapping("/avatar")
    public ResponseEntity<?> deleteAvatar() {
        try {
            // 获取当前登录用户
            User currentUser = authService.getCurrentUser();
            if (currentUser == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("message", "用户未登录"));
            }

            String avatarUrl = currentUser.getAvatarUrl();
            if (avatarUrl == null || avatarUrl.isEmpty()) {
                return ResponseEntity.ok(Map.of("message", "用户没有设置头像"));
            }

            // 从URL中提取对象名
            String objectName = extractObjectNameFromUrl(avatarUrl);
            if (objectName != null) {
                // 删除MinIO中的头像文件
                minioClient.removeObject(
                        RemoveObjectArgs.builder()
                                .bucket(avatarBucket)
                                .object(objectName)
                                .build());
            }

            // 更新用户信息，清空头像URL
            currentUser.setAvatarUrl(null);
            userService.updateUser(currentUser);

            log.info("用户 {} 的头像已删除", currentUser.getUsername());

            return ResponseEntity.ok(Map.of("message", "头像已删除"));
        } catch (Exception e) {
            log.error("头像删除失败: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "头像删除失败: " + e.getMessage()));
        }
    }
    
    /**
     * 获取头像图片
     * 
     * @param objectName 头像对象名
     * @return 头像图片
     */
    @GetMapping("/avatar/{objectName}")
    public ResponseEntity<?> getAvatar(@PathVariable String objectName) {
        try {
            // 生成预签名URL
            String presignedUrl = minioClient.getPresignedObjectUrl(
                    GetPresignedObjectUrlArgs.builder()
                            .method(Method.GET)
                            .bucket(avatarBucket)
                            .object(objectName)
                            .expiry(1, TimeUnit.HOURS)
                            .build());
            
            // 重定向到预签名URL
            return ResponseEntity.status(HttpStatus.FOUND)
                    .header("Location", presignedUrl)
                    .build();
        } catch (Exception e) {
            log.error("获取头像失败: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "获取头像失败: " + e.getMessage()));
        }
    }
    
    /**
     * 从URL中提取对象名
     * 
     * @param url 头像URL
     * @return 对象名
     */
    private String extractObjectNameFromUrl(String url) {
        if (url == null || url.isEmpty()) {
            return null;
        }
        
        // 头像URL格式: /api/users/avatar/objectName
        if (url.startsWith("/api/users/avatar/")) {
            return url.substring("/api/users/avatar/".length());
        }
        
        return null;
    }
} 