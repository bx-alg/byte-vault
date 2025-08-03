package com.bytevault.app.search.service;

import com.bytevault.app.search.document.FileDocument;
import org.springframework.data.domain.Page;

public interface FileSearchService {
    
    /**
     * 同步文件到ES索引
     * @param fileDocument 文件文档
     */
    void indexFile(FileDocument fileDocument);
    
    /**
     * 从ES索引中删除文件
     * @param fileId 文件ID
     */
    void deleteFile(Long fileId);
    
    /**
     * 搜索私有文件
     * @param userId 用户ID
     * @param keyword 关键词
     * @param page 页码
     * @param size 每页大小
     * @return 搜索结果
     */
    Page<FileDocument> searchPrivateFiles(Long userId, String keyword, int page, int size);
    
    /**
     * 搜索公开文件
     * @param keyword 关键词
     * @param page 页码
     * @param size 每页大小
     * @return 搜索结果
     */
    Page<FileDocument> searchPublicFiles(String keyword, int page, int size);
    
    /**
     * 根据用户名搜索公开文件
     * @param username 用户名
     * @param page 页码
     * @param size 每页大小
     * @return 搜索结果
     */
    Page<FileDocument> searchPublicFilesByUsername(String username, int page, int size);
    
    /**
     * 获取用户的所有私有文件
     * @param userId 用户ID
     * @param page 页码
     * @param size 每页大小
     * @return 搜索结果
     */
    Page<FileDocument> getUserPrivateFiles(Long userId, int page, int size);
    
    /**
     * 获取所有公开文件
     * @param page 页码
     * @param size 每页大小
     * @return 搜索结果
     */
    Page<FileDocument> getAllPublicFiles(int page, int size);
    
    /**
     * 清理ES索引中的所有文件数据
     */
    void clearAllFiles();
    
    /**
     * 清理ES索引中指定用户的所有文件数据
     * @param userId 用户ID
     */
    void clearUserFiles(Long userId);
} 