package com.bytevault.app.file.service;

import io.minio.*;
import io.minio.http.Method;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
public class FileServiceImpl implements FileService {

    private final MinioClient minioClient;

    @Value("${minio.bucketName}")
    private String bucketName;

    @Value("${minio.endpoint}")
    private String endpoint;

    public FileServiceImpl(MinioClient minioClient) {
        this.minioClient = minioClient;
    }

    @Override
    public String uploadFile(MultipartFile file, String directory) {
        try {
            // 生成唯一文件名
            String originalFilename = file.getOriginalFilename();
            String fileExtension = "";
            if (originalFilename != null && originalFilename.contains(".")) {
                fileExtension = originalFilename.substring(originalFilename.lastIndexOf("."));
            }
            String filename = UUID.randomUUID() + fileExtension;

            // 构建文件路径
            String objectName = directory + "/" + filename;

            // 上传文件
            InputStream inputStream = file.getInputStream();
            minioClient.putObject(
                    PutObjectArgs.builder()
                            .bucket(bucketName)
                            .object(objectName)
                            .stream(inputStream, file.getSize(), -1)
                            .contentType(file.getContentType())
                            .build());

            // 生成代理URL
            String proxyUrl = "/api/file/proxy/" + directory + "/" + filename;

            log.info("文件上传成功: {}", proxyUrl);
            return proxyUrl;
        } catch (Exception e) {
            log.error("文件上传失败: {}", e.getMessage(), e);
            throw new RuntimeException("文件上传失败", e);
        }
    }

    @Override
    public boolean deleteFile(String fileUrl) {
        try {
            // 从URL中提取对象名
            String objectName = extractObjectName(fileUrl);
            if (objectName == null) {
                return false;
            }

            // 删除文件
            minioClient.removeObject(
                    RemoveObjectArgs.builder()
                            .bucket(bucketName)
                            .object(objectName)
                            .build());

            log.info("文件删除成功: {}", objectName);
            return true;
        } catch (Exception e) {
            log.error("文件删除失败: {}", e.getMessage(), e);
            return false;
        }
    }

    /**
     * 从文件URL中提取对象名
     * 
     * @param fileUrl 文件URL
     * @return 对象名
     */
    private String extractObjectName(String fileUrl) {
        if (fileUrl == null || fileUrl.isEmpty()) {
            return null;
        }

        try {
            // 代理URL格式：/api/file/proxy/avatars/xxxx.jpg
            if (fileUrl.startsWith("/api/file/proxy/")) {
                return fileUrl.substring("/api/file/proxy/".length());
            }

            // 如果不是代理URL格式，尝试解析其他格式
            log.warn("不支持的URL格式: {}", fileUrl);
            return null;
        } catch (Exception e) {
            log.error("从URL提取对象名失败: {}", fileUrl, e);
            return null;
        }
    }
}