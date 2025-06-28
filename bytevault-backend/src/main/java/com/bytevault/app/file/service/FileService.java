package com.bytevault.app.file.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.bytevault.app.model.FileInfo;
import org.springframework.web.multipart.MultipartFile;

/**
 * 文件服务接口
 */
public interface FileService {
    
    /**
     * 上传文件
     * @param file 文件
     * @param userId 用户ID
     * @param parentId 父目录ID
     * @param isPublic 是否公开
     * @return 文件信息
     */
    FileInfo uploadFile(MultipartFile file, Long userId, Long parentId, boolean isPublic);
    
    /**
     * 删除文件
     * @param fileId 文件ID
     * @param userId 用户ID
     * @return 是否删除成功
     */
    boolean deleteFile(Long fileId, Long userId);
    
    /**
     * 获取文件下载URL
     * @param fileId 文件ID
     * @param userId 当前用户ID
     * @return 下载URL
     */
    String getFileDownloadUrl(Long fileId, Long userId);
    
    /**
     * 获取用户文件列表
     * @param userId 用户ID
     * @param parentId 父目录ID
     * @param page 页码
     * @param size 每页大小
     * @return 分页文件列表
     */
    IPage<FileInfo> getUserFiles(Long userId, Long parentId, int page, int size);
    
    /**
     * 获取公开文件列表
     * @param page 页码
     * @param size 每页大小
     * @return 分页公开文件列表
     */
    IPage<FileInfo> getPublicFiles(int page, int size);
    
    /**
     * 搜索文件
     * @param userId 当前用户ID
     * @param keyword 关键词
     * @param page 页码
     * @param size 每页大小
     * @return 分页搜索结果
     */
    IPage<FileInfo> searchFiles(Long userId, String keyword, int page, int size);
    
    /**
     * 更新文件公开状态
     * @param fileId 文件ID
     * @param userId 用户ID
     * @param isPublic 是否公开
     * @return 是否更新成功
     */
    boolean updateFilePublicStatus(Long fileId, Long userId, boolean isPublic);
    
    /**
     * 获取文件详情
     * @param fileId 文件ID
     * @return 文件信息
     */
    FileInfo getFileInfo(Long fileId);
    
    /**
     * 创建文件夹
     * @param userId 用户ID
     * @param parentId 父目录ID
     * @param folderName 文件夹名称
     * @return 文件夹信息
     */
    FileInfo createFolder(Long userId, Long parentId, String folderName);
} 