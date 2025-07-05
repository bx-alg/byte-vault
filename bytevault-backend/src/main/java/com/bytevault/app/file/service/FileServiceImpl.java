package com.bytevault.app.file.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.bytevault.app.mapper.FileMapper;
import com.bytevault.app.mapper.UserMapper;
import com.bytevault.app.model.FileInfo;
import io.minio.*;
import io.minio.http.Method;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.SequenceInputStream;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Slf4j
@Service
public class FileServiceImpl implements FileService {

    private final MinioClient minioClient;
    private final FileMapper fileMapper;
    private final UserMapper userMapper;
    private final RedisTemplate<String, Object> redisTemplate;

    @Value("${minio.userFilesBucketName}")
    private String userFilesBucket;

    @Value("${minio.endpoint}")
    private String endpoint;
    
    // Redis键前缀
    private static final String UPLOAD_ID_PREFIX = "chunk_upload:";
    private static final String UPLOAD_CHUNKS_PREFIX = "chunk_upload_chunks:";
    private static final String UPLOAD_INFO_PREFIX = "chunk_upload_info:";
    
    // 上传过期时间（24小时）
    private static final long UPLOAD_EXPIRATION = 24 * 60 * 60;
    
    public FileServiceImpl(MinioClient minioClient, FileMapper fileMapper, UserMapper userMapper, RedisTemplate<String, Object> redisTemplate) {
        this.minioClient = minioClient;
        this.fileMapper = fileMapper;
        this.userMapper = userMapper;
        this.redisTemplate = redisTemplate;
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
            
            // 构建对象名: 用户ID/文件名
            String minioObjectName = userId + "/" + originalFilename;
            
            // 上传文件到MinIO
            InputStream inputStream = file.getInputStream();
            minioClient.putObject(
                    PutObjectArgs.builder()
                            .bucket(userFilesBucket)
                            .object(minioObjectName)
                            .stream(inputStream, file.getSize(), -1)
                            .contentType(file.getContentType())
                            .build());
            
            // 保存文件信息到数据库
            FileInfo fileInfo = new FileInfo();
            fileInfo.setUserId(userId);
            fileInfo.setFilename(originalFilename);
            fileInfo.setParentId(parentId);
            fileInfo.setFileSize(file.getSize());
            fileInfo.setFileType(file.getContentType());
            fileInfo.setIsDir(false);
            fileInfo.setVisibility(isPublic ? "public" : "private");
            fileInfo.setDeleted(false);
            fileInfo.setCreateTime(LocalDateTime.now());
            fileInfo.setUpdateTime(LocalDateTime.now());
            
            fileMapper.insert(fileInfo);
            
            log.info("文件上传成功: {}, 用户ID: {}", minioObjectName, userId);
            return fileInfo;
        } catch (Exception e) {
            log.error("文件上传失败: {}", e.getMessage(), e);
            throw new RuntimeException("文件上传失败", e);
        }
    }
    
    @Override
    @Transactional
    public List<FileInfo> uploadFolder(List<MultipartFile> files, List<String> relativePaths, Long userId, Long parentId, boolean isPublic) {
        try {
            List<FileInfo> uploadedFiles = new ArrayList<>();
            Map<String, Long> pathToFolderId = new HashMap<>();
            
            // 记录根目录ID
            pathToFolderId.put("", parentId);
            
            // 处理每个文件
            for (int i = 0; i < files.size(); i++) {
                MultipartFile file = files.get(i);
                String relativePath = relativePaths.get(i);
                
                // 跳过空文件（可能是文件夹占位符）
                if (file.isEmpty() && !relativePath.endsWith("/")) {
                    continue;
                }
                
                // 解析路径
                String[] pathParts = relativePath.split("/");
                StringBuilder currentPath = new StringBuilder();
                Long currentParentId = parentId;
                
                // 创建或获取文件夹路径
                for (int j = 0; j < pathParts.length - 1; j++) {
                    String folderName = pathParts[j];
                    if (folderName.isEmpty()) {
                        continue;
                    }
                    
                    // 构建当前路径
                    if (currentPath.length() > 0) {
                        currentPath.append("/");
                    }
                    currentPath.append(folderName);
                    String pathKey = currentPath.toString();
                    
                    // 检查路径是否已处理
                    if (pathToFolderId.containsKey(pathKey)) {
                        currentParentId = pathToFolderId.get(pathKey);
                        continue;
                    }
                    
                    // 检查文件夹是否已存在
                    LambdaQueryWrapper<FileInfo> queryWrapper = new LambdaQueryWrapper<>();
                    queryWrapper.eq(FileInfo::getUserId, userId)
                               .eq(FileInfo::getParentId, currentParentId)
                               .eq(FileInfo::getFilename, folderName)
                               .eq(FileInfo::getIsDir, true)
                               .eq(FileInfo::getDeleted, false);
                    
                    FileInfo existingFolder = fileMapper.selectOne(queryWrapper);
                    
                    if (existingFolder != null) {
                        // 使用已存在的文件夹
                        currentParentId = existingFolder.getId();
                        pathToFolderId.put(pathKey, currentParentId);
                    } else {
                        // 创建新文件夹
                        FileInfo newFolder = createFolder(userId, currentParentId, folderName);
                        currentParentId = newFolder.getId();
                        pathToFolderId.put(pathKey, currentParentId);
                        uploadedFiles.add(newFolder);
                    }
                }
                
                // 如果是文件夹标记（通常是空文件夹），跳过文件上传
                if (relativePath.endsWith("/")) {
                    continue;
                }
                
                // 上传文件
                String filename = pathParts[pathParts.length - 1];
                
                // 构建对象名: 用户ID/相对路径
                String minioObjectName = userId + "/" + relativePath;
                
                // 上传文件到MinIO
                InputStream inputStream = file.getInputStream();
                minioClient.putObject(
                        PutObjectArgs.builder()
                                .bucket(userFilesBucket)
                                .object(minioObjectName)
                                .stream(inputStream, file.getSize(), -1)
                                .contentType(file.getContentType())
                                .build());
                
                // 保存文件信息到数据库
                FileInfo fileInfo = new FileInfo();
                fileInfo.setUserId(userId);
                fileInfo.setFilename(filename);
                fileInfo.setParentId(currentParentId);
                // 不再设置objectName字段
                fileInfo.setFileSize(file.getSize());
                fileInfo.setFileType(file.getContentType());
                fileInfo.setIsDir(false);
                fileInfo.setVisibility(isPublic ? "public" : "private");
                fileInfo.setDeleted(false);
                fileInfo.setCreateTime(LocalDateTime.now());
                fileInfo.setUpdateTime(LocalDateTime.now());
                
                fileMapper.insert(fileInfo);
                uploadedFiles.add(fileInfo);
                
                log.info("文件上传成功: {}, 用户ID: {}", minioObjectName, userId);
            }
            
            return uploadedFiles;
        } catch (Exception e) {
            log.error("文件夹上传失败: {}", e.getMessage(), e);
            throw new RuntimeException("文件夹上传失败", e);
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
            
            // 如果是目录，递归删除子文件和子文件夹
            if (fileInfo.getIsDir()) {
                // 查询所有子文件和子文件夹
                LambdaQueryWrapper<FileInfo> queryWrapper = new LambdaQueryWrapper<>();
                queryWrapper.eq(FileInfo::getParentId, fileId)
                           .eq(FileInfo::getUserId, userId)
                           .eq(FileInfo::getDeleted, false);
                List<FileInfo> childFiles = fileMapper.selectList(queryWrapper);
                
                // 递归删除所有子文件和子文件夹
                for (FileInfo childFile : childFiles) {
                    deleteFile(childFile.getId(), userId);
                }
            } else {
                // 如果是文件，从MinIO中删除
                String minioObjectName = userId + "/" + fileInfo.getFilename();
                minioClient.removeObject(
                        RemoveObjectArgs.builder()
                                .bucket(userFilesBucket)
                                .object(minioObjectName)
                                .build());
            }
            
            // 使用MyBatis-Plus的deleteById直接进行逻辑删除
            int deleteResult = fileMapper.deleteById(fileId);
            
            log.info("文件删除操作: {}, 用户ID: {}, 删除结果: {}", fileId, userId, deleteResult);
            return deleteResult > 0;
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
            
            // 构建MinIO对象名称
            String minioObjectName = userId + "/" + fileInfo.getFilename();
            
            // 生成预签名URL
            String url = minioClient.getPresignedObjectUrl(
                    GetPresignedObjectUrlArgs.builder()
                            .method(Method.GET)
                            .bucket(userFilesBucket)
                            .object(minioObjectName)
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
                        // 构建MinIO对象名称
                        String minioObjectName = fileInfo.getUserId() + "/" + fileInfo.getFilename();
                        
                        String url = minioClient.getPresignedObjectUrl(
                                GetPresignedObjectUrlArgs.builder()
                                        .method(Method.GET)
                                        .bucket(userFilesBucket)
                                        .object(minioObjectName)
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
                        // 构建MinIO对象名称
                        String minioObjectName = fileInfo.getUserId() + "/" + fileInfo.getFilename();
                        
                        String url = minioClient.getPresignedObjectUrl(
                                GetPresignedObjectUrlArgs.builder()
                                        .method(Method.GET)
                                        .bucket(userFilesBucket)
                                        .object(minioObjectName)
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
                            // 构建MinIO对象名称
                            String minioObjectName = fileInfo.getUserId() + "/" + fileInfo.getFilename();
                            
                            String url = minioClient.getPresignedObjectUrl(
                                    GetPresignedObjectUrlArgs.builder()
                                            .method(Method.GET)
                                            .bucket(userFilesBucket)
                                            .object(minioObjectName)
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

    @Override
    @Transactional
    public boolean updateFolderPublicStatus(Long folderId, Long userId, boolean isPublic) {
        try {
            // 查询文件夹信息
            FileInfo folder = fileMapper.selectById(folderId);
            if (folder == null) {
                log.warn("文件夹不存在: {}", folderId);
                return false;
            }
            
            // 检查权限
            if (!folder.getUserId().equals(userId)) {
                log.warn("无权限更新文件夹: {}, 用户ID: {}", folderId, userId);
                return false;
            }
            
            // 检查是否是文件夹
            if (!folder.getIsDir()) {
                log.warn("不是文件夹: {}", folderId);
                return false;
            }
            
            // 更新当前文件夹的公开状态
            boolean currentFolderUpdated = updateFilePublicStatus(folderId, userId, isPublic);
            if (!currentFolderUpdated) {
                return false;
            }
            
            // 查询所有子文件和子文件夹
            LambdaQueryWrapper<FileInfo> queryWrapper = new LambdaQueryWrapper<>();
            queryWrapper.eq(FileInfo::getParentId, folderId)
                       .eq(FileInfo::getUserId, userId)
                       .eq(FileInfo::getDeleted, false);
            List<FileInfo> childFiles = fileMapper.selectList(queryWrapper);
            
            // 递归更新所有子文件和子文件夹
            for (FileInfo childFile : childFiles) {
                if (childFile.getIsDir()) {
                    // 递归处理子文件夹
                    updateFolderPublicStatus(childFile.getId(), userId, isPublic);
                } else {
                    // 更新子文件的公开状态
                    updateFilePublicStatus(childFile.getId(), userId, isPublic);
                }
            }
            
            log.info("文件夹及其子文件公开状态更新成功: {}, 用户ID: {}, 公开状态: {}", folderId, userId, isPublic);
            return true;
        } catch (Exception e) {
            log.error("更新文件夹公开状态失败: {}", e.getMessage(), e);
            return false;
        }
    }

    @Override
    public String initChunkUpload(String filename, Long fileSize, String fileType, Long userId, Long parentId, boolean isPublic) {
        try {
            // 生成唯一的上传ID
            String uploadId = UUID.randomUUID().toString();
            
            // 存储上传信息到Redis
            Map<String, Object> uploadInfo = new HashMap<>();
            uploadInfo.put("filename", filename);
            uploadInfo.put("fileSize", fileSize);
            uploadInfo.put("fileType", fileType);
            uploadInfo.put("userId", userId);
            uploadInfo.put("parentId", parentId);
            uploadInfo.put("isPublic", isPublic);
            uploadInfo.put("createTime", System.currentTimeMillis());
            
            // 保存上传信息，设置24小时过期
            redisTemplate.opsForValue().set(UPLOAD_INFO_PREFIX + uploadId, uploadInfo, UPLOAD_EXPIRATION, TimeUnit.SECONDS);
            
            // 初始化已上传分块集合
            redisTemplate.opsForValue().set(UPLOAD_CHUNKS_PREFIX + uploadId, new ArrayList<Integer>(), UPLOAD_EXPIRATION, TimeUnit.SECONDS);
            
            log.info("初始化分块上传: {}, 用户ID: {}", uploadId, userId);
            return uploadId;
        } catch (Exception e) {
            log.error("初始化分块上传失败: {}", e.getMessage(), e);
            throw new RuntimeException("初始化分块上传失败", e);
        }
    }

    @Override
    public boolean uploadChunk(String uploadId, int chunkIndex, MultipartFile chunk, Long userId) {
        try {
            // 获取上传信息
            Map<String, Object> uploadInfo = (Map<String, Object>) redisTemplate.opsForValue().get(UPLOAD_INFO_PREFIX + uploadId);
            if (uploadInfo == null) {
                log.warn("上传ID不存在或已过期: {}", uploadId);
                return false;
            }
            
            // 验证用户权限
            Long fileUserId = Long.valueOf(uploadInfo.get("userId").toString());
            if (!fileUserId.equals(userId)) {
                log.warn("无权限上传分块: {}, 用户ID: {}", uploadId, userId);
                return false;
            }
            
            // 构建分块对象名: 用户ID/uploadId/chunkIndex
            String chunkObjectName = userId + "/chunks/" + uploadId + "/" + chunkIndex;
            
            // 上传分块到MinIO
            InputStream inputStream = chunk.getInputStream();
            minioClient.putObject(
                    PutObjectArgs.builder()
                            .bucket(userFilesBucket)
                            .object(chunkObjectName)
                            .stream(inputStream, chunk.getSize(), -1)
                            .contentType("application/octet-stream")
                            .build());
            
            // 更新已上传分块列表
            List<Integer> uploadedChunks = getUploadedChunks(uploadId, userId);
            if (!uploadedChunks.contains(chunkIndex)) {
                uploadedChunks.add(chunkIndex);
                redisTemplate.opsForValue().set(UPLOAD_CHUNKS_PREFIX + uploadId, uploadedChunks, UPLOAD_EXPIRATION, TimeUnit.SECONDS);
            }
            
            log.info("分块上传成功: {}, 分块索引: {}, 用户ID: {}", uploadId, chunkIndex, userId);
            return true;
        } catch (Exception e) {
            log.error("分块上传失败: {}", e.getMessage(), e);
            throw new RuntimeException("分块上传失败", e);
        }
    }

    @Override
    public List<Integer> getUploadedChunks(String uploadId, Long userId) {
        try {
            // 获取上传信息
            Map<String, Object> uploadInfo = (Map<String, Object>) redisTemplate.opsForValue().get(UPLOAD_INFO_PREFIX + uploadId);
            if (uploadInfo == null) {
                log.warn("上传ID不存在或已过期: {}", uploadId);
                return new ArrayList<>();
            }
            
            // 验证用户权限
            Long fileUserId = Long.valueOf(uploadInfo.get("userId").toString());
            if (!fileUserId.equals(userId)) {
                log.warn("无权限获取已上传分块列表: {}, 用户ID: {}", uploadId, userId);
                return new ArrayList<>();
            }
            
            // 获取已上传分块列表
            List<Integer> uploadedChunks = (List<Integer>) redisTemplate.opsForValue().get(UPLOAD_CHUNKS_PREFIX + uploadId);
            if (uploadedChunks == null) {
                uploadedChunks = new ArrayList<>();
            }
            
            return uploadedChunks;
        } catch (Exception e) {
            log.error("获取已上传分块列表失败: {}", e.getMessage(), e);
            throw new RuntimeException("获取已上传分块列表失败", e);
        }
    }

    @Override
    @Transactional
    public FileInfo completeChunkUpload(String uploadId, int totalChunks, Long userId) {
        try {
            // 获取上传信息
            Map<String, Object> uploadInfo = (Map<String, Object>) redisTemplate.opsForValue().get(UPLOAD_INFO_PREFIX + uploadId);
            if (uploadInfo == null) {
                log.warn("上传ID不存在或已过期: {}", uploadId);
                throw new RuntimeException("上传ID不存在或已过期");
            }
            
            // 验证用户权限
            Long fileUserId = Long.valueOf(uploadInfo.get("userId").toString());
            if (!fileUserId.equals(userId)) {
                log.warn("无权限完成上传: {}, 用户ID: {}", uploadId, userId);
                throw new RuntimeException("无权限完成上传");
            }
            
            // 获取已上传分块列表
            List<Integer> uploadedChunks = getUploadedChunks(uploadId, userId);
            if (uploadedChunks.size() != totalChunks) {
                log.warn("分块数量不匹配: {}, 已上传: {}, 总数: {}", uploadId, uploadedChunks.size(), totalChunks);
                throw new RuntimeException("分块数量不匹配，请确保所有分块已上传");
            }
            
            // 排序分块
            Collections.sort(uploadedChunks);
            
            // 获取文件信息
            String filename = (String) uploadInfo.get("filename");
            Long fileSize = Long.valueOf(uploadInfo.get("fileSize").toString());
            String fileType = (String) uploadInfo.get("fileType");
            Long parentId = Long.valueOf(uploadInfo.get("parentId").toString());
            boolean isPublic = Boolean.parseBoolean(uploadInfo.get("isPublic").toString());
            
            // 合并文件的最终对象名
            String finalObjectName = userId + "/" + filename;
            
            // 使用PutObject方法手动合并文件，而不是使用ComposeObject
            // 这样可以避开MinIO对ComposeObject的5MB最小分块大小限制
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            for (int i = 0; i < totalChunks; i++) {
                String chunkObjectName = userId + "/chunks/" + uploadId + "/" + i;
                
                // 获取分块内容
                GetObjectResponse response = minioClient.getObject(
                        GetObjectArgs.builder()
                                .bucket(userFilesBucket)
                                .object(chunkObjectName)
                                .build()
                );
                
                // 将分块内容写入输出流
                byte[] buffer = new byte[8192];
                int bytesRead;
                while ((bytesRead = response.read(buffer, 0, buffer.length)) != -1) {
                    outputStream.write(buffer, 0, bytesRead);
                }
                response.close();
            }
            
            // 将合并后的内容上传到MinIO
            ByteArrayInputStream inputStream = new ByteArrayInputStream(outputStream.toByteArray());
            minioClient.putObject(
                    PutObjectArgs.builder()
                            .bucket(userFilesBucket)
                            .object(finalObjectName)
                            .stream(inputStream, outputStream.size(), -1)
                            .contentType(fileType)
                            .build()
            );
            
            // 保存文件信息到数据库
            FileInfo fileInfo = new FileInfo();
            fileInfo.setUserId(userId);
            fileInfo.setFilename(filename);
            fileInfo.setParentId(parentId);
            fileInfo.setFileSize(fileSize);
            fileInfo.setFileType(fileType);
            fileInfo.setIsDir(false);
            fileInfo.setVisibility(isPublic ? "public" : "private");
            fileInfo.setDeleted(false);
            fileInfo.setCreateTime(LocalDateTime.now());
            fileInfo.setUpdateTime(LocalDateTime.now());
            
            fileMapper.insert(fileInfo);
            
            // 清理分块和上传信息
            for (int i = 0; i < totalChunks; i++) {
                String chunkObjectName = userId + "/chunks/" + uploadId + "/" + i;
                try {
                    minioClient.removeObject(
                            RemoveObjectArgs.builder()
                                    .bucket(userFilesBucket)
                                    .object(chunkObjectName)
                                    .build()
                    );
                } catch (Exception e) {
                    log.warn("清理分块失败: {}", chunkObjectName, e);
                }
            }
            
            // 删除Redis中的上传信息
            redisTemplate.delete(UPLOAD_INFO_PREFIX + uploadId);
            redisTemplate.delete(UPLOAD_CHUNKS_PREFIX + uploadId);
            
            log.info("文件上传完成: {}, 文件ID: {}, 用户ID: {}", uploadId, fileInfo.getId(), userId);
            return fileInfo;
        } catch (Exception e) {
            log.error("完成文件上传失败: {}", e.getMessage(), e);
            throw new RuntimeException("完成文件上传失败", e);
        }
    }
}