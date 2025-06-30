package com.bytevault.app.service;

import com.bytevault.app.mapper.BackgroundImageMapper;
import com.bytevault.app.mapper.UserMapper;
import com.bytevault.app.model.BackgroundImage;
import com.bytevault.app.model.User;
import io.minio.*;
import io.minio.http.Method;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
public class BackgroundImageServiceImpl implements BackgroundImageService {
    
    private final BackgroundImageMapper backgroundImageMapper;
    private final UserMapper userMapper;
    private final MinioClient minioClient;
    
    @Value("${minio.backgroundBucketName}")
    private String backgroundBucket;
    
    @Value("${minio.endpoint}")
    private String minioEndpoint;
    
    public BackgroundImageServiceImpl(BackgroundImageMapper backgroundImageMapper, 
                                     UserMapper userMapper, 
                                     MinioClient minioClient) {
        this.backgroundImageMapper = backgroundImageMapper;
        this.userMapper = userMapper;
        this.minioClient = minioClient;
    }
    
    @Override
    @Transactional
    public BackgroundImage uploadBackgroundImage(MultipartFile file, Long userId) {
        try {
            // 验证文件类型
            String contentType = file.getContentType();
            if (contentType == null || !contentType.startsWith("image/")) {
                throw new IllegalArgumentException("只支持上传图片文件");
            }
            
            // 生成唯一文件名
            String originalFilename = file.getOriginalFilename();
            
            // 构建对象名: 用户ID/文件名
            String objectName = userId + "/" + originalFilename;
            
            // 检查桶是否存在，不存在则创建
            boolean bucketExists = minioClient.bucketExists(BucketExistsArgs.builder().bucket(backgroundBucket).build());
            if (!bucketExists) {
                minioClient.makeBucket(MakeBucketArgs.builder().bucket(backgroundBucket).build());
            }
            
            // 上传文件到MinIO
            InputStream inputStream = file.getInputStream();
            minioClient.putObject(
                    PutObjectArgs.builder()
                            .bucket(backgroundBucket)
                            .object(objectName)
                            .stream(inputStream, file.getSize(), -1)
                            .contentType(contentType)
                            .build());
            
            // 构建访问URL - 使用代理URL
            String imageUrl = "/api/users/background/" + objectName;
            
            // 保存记录到数据库
            BackgroundImage backgroundImage = new BackgroundImage();
            backgroundImage.setUserId(userId);
            backgroundImage.setImageUrl(imageUrl);
            backgroundImage.setUploadTime(LocalDateTime.now());
            backgroundImage.setDeleted(0);
            
            backgroundImageMapper.insert(backgroundImage);
            
            log.info("背景图片上传成功: {}, 用户ID: {}", objectName, userId);
            return backgroundImage;
        } catch (Exception e) {
            log.error("背景图片上传失败: {}", e.getMessage(), e);
            throw new RuntimeException("背景图片上传失败: " + e.getMessage(), e);
        }
    }
    
    @Override
    public List<BackgroundImage> getUserBackgroundImages(Long userId) {
        return backgroundImageMapper.getUserBackgroundImages(userId);
    }
    
    @Override
    @Transactional
    public boolean deleteBackgroundImage(Long imageId, Long userId) {
        try {
            // 查询图片信息
            BackgroundImage image = backgroundImageMapper.getBackgroundImageById(imageId);
            if (image == null) {
                log.warn("背景图片不存在: {}", imageId);
                return false;
            }
            
            // 检查权限
            if (!image.getUserId().equals(userId)) {
                log.warn("无权限删除背景图片: {}, 用户ID: {}", imageId, userId);
                return false;
            }
            
            // 从MinIO中删除
            String objectName = extractObjectNameFromUrl(image.getImageUrl());
            if (objectName != null) {
                minioClient.removeObject(
                        RemoveObjectArgs.builder()
                                .bucket(backgroundBucket)
                                .object(objectName)
                                .build());
            }
            
            // 如果是当前背景，重置用户的背景图片ID
            User user = userMapper.selectById(userId);
            if (user != null && user.getCurrentBackgroundImageId() != null && 
                    user.getCurrentBackgroundImageId().equals(imageId)) {
                user.setCurrentBackgroundImageId(null);
                userMapper.updateById(user);
            }
            
            // 从数据库中删除
            int result = backgroundImageMapper.deleteById(imageId);
            
            log.info("背景图片删除成功: {}, 用户ID: {}", imageId, userId);
            return result > 0;
        } catch (Exception e) {
            log.error("背景图片删除失败: {}", e.getMessage(), e);
            return false;
        }
    }
    
    @Override
    @Transactional
    public boolean setCurrentBackgroundImage(Long imageId, Long userId) {
        try {
            // 查询图片信息
            BackgroundImage image = backgroundImageMapper.getBackgroundImageById(imageId);
            if (image == null) {
                log.warn("背景图片不存在: {}", imageId);
                return false;
            }
            
            // 检查权限
            if (!image.getUserId().equals(userId)) {
                log.warn("无权限设置背景图片: {}, 用户ID: {}", imageId, userId);
                return false;
            }
            
            // 更新用户的当前背景图片ID
            User user = userMapper.selectById(userId);
            if (user == null) {
                log.warn("用户不存在: {}", userId);
                return false;
            }
            
            user.setCurrentBackgroundImageId(imageId);
            int result = userMapper.updateById(user);
            
            log.info("设置当前背景图片成功: {}, 用户ID: {}", imageId, userId);
            return result > 0;
        } catch (Exception e) {
            log.error("设置当前背景图片失败: {}", e.getMessage(), e);
            return false;
        }
    }
    
    @Override
    public String getCurrentBackgroundImageUrl(Long userId) {
        try {
            // 查询用户信息
            User user = userMapper.selectById(userId);
            if (user == null || user.getCurrentBackgroundImageId() == null) {
                return null;
            }
            
            // 查询背景图片信息
            BackgroundImage image = backgroundImageMapper.getBackgroundImageById(user.getCurrentBackgroundImageId());
            if (image == null) {
                return null;
            }
            
            return image.getImageUrl();
        } catch (Exception e) {
            log.error("获取当前背景图片URL失败: {}", e.getMessage(), e);
            return null;
        }
    }
    
    @Override
    public BackgroundImage getBackgroundImageById(Long imageId) {
        return backgroundImageMapper.getBackgroundImageById(imageId);
    }
    
    /**
     * 从URL中提取对象名
     * @param url 图片URL
     * @return 对象名
     */
    private String extractObjectNameFromUrl(String url) {
        if (url == null || url.isEmpty()) {
            return null;
        }
        
        // 背景图片URL格式: /api/users/background/objectName
        if (url.startsWith("/api/users/background/")) {
            return url.substring("/api/users/background/".length());
        }
        
        return null;
    }
    
    /**
     * 获取背景图片的预签名URL
     * @param objectName 对象名
     * @return 预签名URL
     */
    public String getPresignedUrl(String objectName) {
        try {
            return minioClient.getPresignedObjectUrl(
                    GetPresignedObjectUrlArgs.builder()
                            .method(Method.GET)
                            .bucket(backgroundBucket)
                            .object(objectName)
                            .expiry(1, TimeUnit.HOURS)
                            .build());
        } catch (Exception e) {
            log.error("获取预签名URL失败: {}", e.getMessage(), e);
            return null;
        }
    }
} 