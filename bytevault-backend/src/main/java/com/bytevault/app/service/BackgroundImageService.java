package com.bytevault.app.service;

import com.bytevault.app.model.BackgroundImage;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface BackgroundImageService {
    
    /**
     * 上传背景图片
     * @param file 图片文件
     * @param userId 用户ID
     * @return 背景图片信息
     */
    BackgroundImage uploadBackgroundImage(MultipartFile file, Long userId);
    
    /**
     * 获取用户的所有背景图片
     * @param userId 用户ID
     * @return 背景图片列表
     */
    List<BackgroundImage> getUserBackgroundImages(Long userId);
    
    /**
     * 删除背景图片
     * @param imageId 图片ID
     * @param userId 用户ID
     * @return 是否删除成功
     */
    boolean deleteBackgroundImage(Long imageId, Long userId);
    
    /**
     * 设置当前背景图片
     * @param imageId 图片ID
     * @param userId 用户ID
     * @return 是否设置成功
     */
    boolean setCurrentBackgroundImage(Long imageId, Long userId);
    
    /**
     * 获取当前背景图片URL
     * @param userId 用户ID
     * @return 背景图片URL
     */
    String getCurrentBackgroundImageUrl(Long userId);
    
    /**
     * 获取背景图片详情
     * @param imageId 图片ID
     * @return 背景图片信息
     */
    BackgroundImage getBackgroundImageById(Long imageId);
} 