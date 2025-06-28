package com.bytevault.app.file.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.bytevault.app.mapper.FileMapper;
import com.bytevault.app.mapper.UserMapper;
import com.bytevault.app.model.FileInfo;
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
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
public class FileServiceImpl implements FileService {

    private final MinioClient minioClient;
    private final FileMapper fileMapper;
    private final UserMapper userMapper;

    @Value("${minio.userFilesBucket}")
    private String userFilesBucket;

    @Value("${minio.endpoint}")
    private String endpoint;
    
    @Value("${minio.bucketName}")
    private String bucketName;

    public FileServiceImpl(MinioClient minioClient, FileMapper fileMapper, UserMapper userMapper) {
        this.minioClient = minioClient;
        this.fileMapper = fileMapper;
        this.userMapper = userMapper;
    }

    @Override
    @Transactional
    public FileInfo uploadFile(MultipartFile file, Long userId, Long parentId, boolean isPublic) {
        try {
            // 获取原始文件名
            String originalFilename = file.getOriginalFilename();
            if (originalFilename == null) {
                originalFilename = "unknown_file";
            }
            
            // 构建对象名: 用户ID/uuid/文件名
            String uuid = UUID.randomUUID().toString();
            String objectName = userId + "/" + uuid + "/" + originalFilename;
            
            // 上传文件到MinIO
            InputStream inputStream = file.getInputStream();
            minioClient.putObject(
                    PutObjectArgs.builder()
                            .bucket(userFilesBucket)
                            .object(objectName)
                            .stream(inputStream, file.getSize(), -1)
                            .contentType(file.getContentType())
                            .build());
            
            // 保存文件信息到数据库
            FileInfo fileInfo = new FileInfo();
            fileInfo.setUserId(userId);
            fileInfo.setFilename(originalFilename);
            fileInfo.setParentId(parentId);
            fileInfo.setObjectName(objectName);
            fileInfo.setFileSize(file.getSize());
            fileInfo.setFileType(file.getContentType());
            fileInfo.setIsDir(false);
            fileInfo.setVisibility(isPublic ? "public" : "private");
            fileInfo.setDeleted(false);
            fileInfo.setCreateTime(LocalDateTime.now());
            fileInfo.setUpdateTime(LocalDateTime.now());
            
            fileMapper.insert(fileInfo);
            
            log.info("文件上传成功: {}, 用户ID: {}", objectName, userId);
            return fileInfo;
        } catch (Exception e) {
            log.error("文件上传失败: {}", e.getMessage(), e);
            throw new RuntimeException("文件上传失败", e);
        }
    }

    @Override
    @Transactional
    public boolean deleteFile(Long fileId, Long userId) {
        try {
            // 查询文件信息
            FileInfo fileInfo = fileMapper.selectById(fileId);
            if (fileInfo == null) {
                log.warn("文件不存在: {}", fileId);
                return false;
            }
            
            // 检查权限
            if (!fileInfo.getUserId().equals(userId)) {
                log.warn("无权限删除文件: {}, 用户ID: {}", fileId, userId);
                return false;
            }
            
            // 如果是文件，从MinIO中删除
            if (!fileInfo.getIsDir()) {
                minioClient.removeObject(
                        RemoveObjectArgs.builder()
                                .bucket(userFilesBucket)
                                .object(fileInfo.getObjectName())
                                .build());
            }
            
            // 从数据库中删除文件记录（逻辑删除）
            fileInfo.setDeleted(true);
            fileInfo.setUpdateTime(LocalDateTime.now());
            fileMapper.updateById(fileInfo);
            
            log.info("文件删除成功: {}, 用户ID: {}", fileId, userId);
            return true;
        } catch (Exception e) {
            log.error("文件删除失败: {}", e.getMessage(), e);
            return false;
        }
    }

    @Override
    public String getFileDownloadUrl(Long fileId, Long userId) {
        try {
            // 查询文件信息
            FileInfo fileInfo = fileMapper.selectById(fileId);
            if (fileInfo == null) {
                log.warn("文件不存在: {}", fileId);
                return null;
            }
            
            // 检查权限
            if (!fileInfo.getUserId().equals(userId) && !"public".equals(fileInfo.getVisibility())) {
                log.warn("无权限下载文件: {}, 用户ID: {}", fileId, userId);
                return null;
            }
            
            // 如果是目录，不能下载
            if (fileInfo.getIsDir()) {
                log.warn("不能下载目录: {}", fileId);
                return null;
            }
            
            // 生成预签名URL
            String url = minioClient.getPresignedObjectUrl(
                    GetPresignedObjectUrlArgs.builder()
                            .method(Method.GET)
                            .bucket(userFilesBucket)
                            .object(fileInfo.getObjectName())
                            .expiry(1, TimeUnit.HOURS) // URL有效期1小时
                            .build());
            
            log.info("生成文件下载URL: {}, 文件ID: {}", url, fileId);
            return url;
        } catch (Exception e) {
            log.error("生成文件下载URL失败: {}", e.getMessage(), e);
            return null;
        }
    }

    @Override
    public IPage<FileInfo> getUserFiles(Long userId, Long parentId, int page, int size) {
        try {
            Page<FileInfo> pageParam = new Page<>(page, size);
            IPage<FileInfo> result = fileMapper.selectUserFiles(pageParam, userId, parentId);
            
            // 为每个文件生成下载URL
            for (FileInfo fileInfo : result.getRecords()) {
                if (!fileInfo.getIsDir()) {
                    try {
                        String url = minioClient.getPresignedObjectUrl(
                                GetPresignedObjectUrlArgs.builder()
                                        .method(Method.GET)
                                        .bucket(userFilesBucket)
                                        .object(fileInfo.getObjectName())
                                        .expiry(1, TimeUnit.HOURS)
                                        .build());
                        fileInfo.setDownloadUrl(url);
                    } catch (Exception e) {
                        log.error("生成文件下载URL失败: {}", e.getMessage(), e);
                    }
                }
            }
            
            return result;
        } catch (Exception e) {
            log.error("获取用户文件列表失败: {}", e.getMessage(), e);
            throw new RuntimeException("获取用户文件列表失败", e);
        }
    }

    @Override
    public IPage<FileInfo> getPublicFiles(int page, int size) {
        try {
            Page<FileInfo> pageParam = new Page<>(page, size);
            IPage<FileInfo> result = fileMapper.selectPublicFiles(pageParam);
            
            // 为每个文件生成下载URL
            for (FileInfo fileInfo : result.getRecords()) {
                if (!fileInfo.getIsDir()) {
                    try {
                        String url = minioClient.getPresignedObjectUrl(
                                GetPresignedObjectUrlArgs.builder()
                                        .method(Method.GET)
                                        .bucket(userFilesBucket)
                                        .object(fileInfo.getObjectName())
                                        .expiry(1, TimeUnit.HOURS)
                                        .build());
                        fileInfo.setDownloadUrl(url);
                    } catch (Exception e) {
                        log.error("生成文件下载URL失败: {}", e.getMessage(), e);
                    }
                }
            }
            
            return result;
        } catch (Exception e) {
            log.error("获取公开文件列表失败: {}", e.getMessage(), e);
            throw new RuntimeException("获取公开文件列表失败", e);
        }
    }

    @Override
    public IPage<FileInfo> searchFiles(Long userId, String keyword, int page, int size) {
        try {
            Page<FileInfo> pageParam = new Page<>(page, size);
            IPage<FileInfo> result = fileMapper.searchFiles(pageParam, userId, keyword);
            
            // 为每个文件生成下载URL
            for (FileInfo fileInfo : result.getRecords()) {
                if (!fileInfo.getIsDir()) {
                    try {
                        // 只为用户有权限访问的文件生成下载URL
                        if (fileInfo.getUserId().equals(userId) || "public".equals(fileInfo.getVisibility())) {
                            String url = minioClient.getPresignedObjectUrl(
                                    GetPresignedObjectUrlArgs.builder()
                                            .method(Method.GET)
                                            .bucket(userFilesBucket)
                                            .object(fileInfo.getObjectName())
                                            .expiry(1, TimeUnit.HOURS)
                                            .build());
                            fileInfo.setDownloadUrl(url);
                        }
                    } catch (Exception e) {
                        log.error("生成文件下载URL失败: {}", e.getMessage(), e);
                    }
                }
            }
            
            return result;
        } catch (Exception e) {
            log.error("搜索文件失败: {}", e.getMessage(), e);
            throw new RuntimeException("搜索文件失败", e);
        }
    }

    @Override
    @Transactional
    public boolean updateFilePublicStatus(Long fileId, Long userId, boolean isPublic) {
        try {
            // 查询文件信息
            FileInfo fileInfo = fileMapper.selectById(fileId);
            if (fileInfo == null) {
                log.warn("文件不存在: {}", fileId);
                return false;
            }
            
            // 检查权限
            if (!fileInfo.getUserId().equals(userId)) {
                log.warn("无权限更新文件: {}, 用户ID: {}", fileId, userId);
                return false;
            }
            
            // 更新文件公开状态
            fileInfo.setVisibility(isPublic ? "public" : "private");
            fileInfo.setUpdateTime(LocalDateTime.now());
            
            fileMapper.updateById(fileInfo);
            
            log.info("文件公开状态更新成功: {}, 用户ID: {}, 公开状态: {}", fileId, userId, isPublic);
            return true;
        } catch (Exception e) {
            log.error("更新文件公开状态失败: {}", e.getMessage(), e);
            return false;
        }
    }

    @Override
    public FileInfo getFileInfo(Long fileId) {
        return fileMapper.selectById(fileId);
    }
    
    @Override
    @Transactional
    public FileInfo createFolder(Long userId, Long parentId, String folderName) {
        try {
            // 检查父目录是否存在
            if (parentId != null && parentId > 0) {
                FileInfo parentFolder = fileMapper.selectById(parentId);
                if (parentFolder == null || !parentFolder.getIsDir()) {
                    log.warn("父目录不存在或不是目录: {}", parentId);
                    throw new RuntimeException("父目录不存在或不是目录");
                }
                
                // 检查权限
                if (!parentFolder.getUserId().equals(userId)) {
                    log.warn("无权限在此目录下创建文件夹: {}, 用户ID: {}", parentId, userId);
                    throw new RuntimeException("无权限在此目录下创建文件夹");
                }
            }
            
            // 创建文件夹记录
            FileInfo folder = new FileInfo();
            folder.setUserId(userId);
            folder.setFilename(folderName);
            folder.setParentId(parentId);
            folder.setObjectName(null); // 目录没有对应的对象
            folder.setFileSize(0L);
            folder.setFileType("directory");
            folder.setIsDir(true);
            folder.setVisibility("private"); // 默认私有
            folder.setDeleted(false);
            folder.setCreateTime(LocalDateTime.now());
            folder.setUpdateTime(LocalDateTime.now());
            
            fileMapper.insert(folder);
            
            log.info("文件夹创建成功: {}, 用户ID: {}", folderName, userId);
            return folder;
        } catch (Exception e) {
            log.error("创建文件夹失败: {}", e.getMessage(), e);
            throw new RuntimeException("创建文件夹失败", e);
        }
    }
}