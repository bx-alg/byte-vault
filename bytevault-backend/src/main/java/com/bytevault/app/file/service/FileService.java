package com.bytevault.app.file.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.bytevault.app.model.FileInfo;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * 文件服务接口
 */
public interface FileService {
    
    /**
     * 上传文件
     * @param file 文件对象
     * @param userId 用户ID
     * @param parentId 父目录ID
     * @param isPublic 是否公开
     * @return 文件信息
     */
    FileInfo uploadFile(MultipartFile file, Long userId, Long parentId, boolean isPublic);
    
    /**
     * 上传文件夹
     * @param files 文件列表
     * @param relativePaths 文件相对路径列表
     * @param userId 用户ID
     * @param parentId 父目录ID
     * @param isPublic 是否公开
     * @return 文件信息列表
     */
    List<FileInfo> uploadFolder(List<MultipartFile> files, List<String> relativePaths, Long userId, Long parentId, boolean isPublic);
    
    /**
     * 删除文件
     * @param fileId 文件ID
     * @param userId 用户ID
     * @return 是否成功
     */
    boolean deleteFile(Long fileId, Long userId);
    
    /**
     * 获取文件下载URL
     * @param fileId 文件ID
     * @param userId 用户ID
     * @return 下载URL
     */
    String getFileDownloadUrl(Long fileId, Long userId);
    
    /**
     * 获取用户文件列表
     * @param userId 用户ID
     * @param parentId 父目录ID
     * @param page 页码
     * @param size 每页大小
     * @return 文件列表
     */
    IPage<FileInfo> getUserFiles(Long userId, Long parentId, int page, int size);
    
    /**
     * 获取公开文件列表
     * @param page 页码
     * @param size 每页大小
     * @return 文件列表
     */
    IPage<FileInfo> getPublicFiles(int page, int size);
    
    /**
     * 搜索文件
     * @param userId 用户ID
     * @param keyword 关键词
     * @param page 页码
     * @param size 每页大小
     * @return 文件列表
     */
    IPage<FileInfo> searchFiles(Long userId, String keyword, int page, int size);
    
    /**
     * 更新文件公开状态
     * @param fileId 文件ID
     * @param userId 用户ID
     * @param isPublic 是否公开
     * @return 是否成功
     */
    boolean updateFilePublicStatus(Long fileId, Long userId, boolean isPublic);
    
    /**
     * 获取文件信息
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
    
    /**
     * 更新文件夹公开状态
     * @param folderId 文件夹ID
     * @param userId 用户ID
     * @param isPublic 是否公开
     * @return 是否成功
     */
    boolean updateFolderPublicStatus(Long folderId, Long userId, boolean isPublic);
    
    /**
     * 初始化分块上传
     * @param filename 文件名
     * @param fileSize 文件大小
     * @param fileType 文件类型
     * @param userId 用户ID
     * @param parentId 父目录ID
     * @param isPublic 是否公开
     * @return 上传ID
     */
    String initChunkUpload(String filename, Long fileSize, String fileType, Long userId, Long parentId, boolean isPublic);
    
    /**
     * 上传文件分块
     * @param uploadId 上传ID
     * @param chunkIndex 分块索引
     * @param chunk 分块数据
     * @param userId 用户ID
     * @return 是否成功
     */
    boolean uploadChunk(String uploadId, int chunkIndex, MultipartFile chunk, Long userId);
    
    /**
     * 获取已上传的分块列表
     * @param uploadId 上传ID
     * @param userId 用户ID
     * @return 已上传的分块索引列表
     */
    List<Integer> getUploadedChunks(String uploadId, Long userId);
    
    /**
     * 完成分块上传
     * @param uploadId 上传ID
     * @param totalChunks 总分块数
     * @param userId 用户ID
     * @return 文件信息
     */
    FileInfo completeChunkUpload(String uploadId, int totalChunks, Long userId);
} 