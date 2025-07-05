package com.bytevault.app.file.controller;

import com.bytevault.app.auth.model.UserDetailsImpl;
import com.bytevault.app.file.service.FileService;
import com.bytevault.app.model.FileInfo;
import io.minio.GetObjectArgs;
import io.minio.MinioClient;
import io.minio.errors.MinioException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@Slf4j
@RestController
@RequestMapping("/api/files/proxy")
@RequiredArgsConstructor
public class FileProxyController {

    private final FileService fileService;
    private final MinioClient minioClient;
    
    @Value("${minio.userFilesBucketName}")
    private String userFilesBucket;

    /**
     * 代理下载文件
     * 通过文件ID获取文件并提供下载
     */ 
    @GetMapping("/{fileId}")
    public ResponseEntity<InputStreamResource> proxyDownload(
            @PathVariable Long fileId,
            @AuthenticationPrincipal UserDetailsImpl userDetails) {
        try {
            // 获取文件信息
            FileInfo fileInfo = fileService.getFileInfo(fileId);
            if (fileInfo == null) {
                log.warn("文件不存在: {}", fileId);
                return ResponseEntity.notFound().build();
            }
            
            // 检查权限
            if (userDetails == null) {
                // 未登录用户只能访问公开文件
                if (!"public".equals(fileInfo.getVisibility())) {
                    log.warn("未登录用户尝试访问非公开文件: {}", fileId);
                    return ResponseEntity.status(403).build();
                }
            } else if (!fileInfo.getUserId().equals(userDetails.getId()) && !"public".equals(fileInfo.getVisibility())) {
                // 已登录用户只能访问自己的文件和公开文件
                log.warn("无权限下载文件: {}, 用户ID: {}", fileId, userDetails.getId());
                return ResponseEntity.status(403).build();
            }
            
            // 检查是否是目录
            if (fileInfo.getIsDir()) {
                log.warn("不能下载目录: {}", fileId);
                return ResponseEntity.badRequest().build();
            }
            
            // 构建MinIO对象名称
            String minioObjectName = fileInfo.getUserId() + "/" + fileInfo.getFilename();
            
            // 从MinIO获取文件
            InputStream inputStream = minioClient.getObject(
                    GetObjectArgs.builder()
                            .bucket(userFilesBucket)
                            .object(minioObjectName)
                            .build());
            
            // 设置响应头
            String encodedFilename = URLEncoder.encode(fileInfo.getFilename(), StandardCharsets.UTF_8.toString())
                    .replace("+", "%20"); // 替换空格
            
            HttpHeaders headers = new HttpHeaders();
            // 强制浏览器下载文件而不是打开
            headers.add(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename*=UTF-8''" + encodedFilename);
            headers.add(HttpHeaders.CACHE_CONTROL, "no-cache, no-store, must-revalidate");
            headers.add(HttpHeaders.PRAGMA, "no-cache");
            headers.add(HttpHeaders.EXPIRES, "0");
            
            // 设置内容类型
            MediaType contentType = MediaType.APPLICATION_OCTET_STREAM;
            if (fileInfo.getFileType() != null && !fileInfo.getFileType().isEmpty()) {
                try {
                    contentType = MediaType.parseMediaType(fileInfo.getFileType());
                } catch (Exception e) {
                    log.warn("无法解析内容类型: {}", fileInfo.getFileType());
                }
            }
            
            log.info("代理下载文件: {}, 用户: {}", fileInfo.getFilename(), 
                    userDetails != null ? userDetails.getUsername() : "匿名用户");
            return ResponseEntity.ok()
                    .headers(headers)
                    .contentType(contentType)
                    .body(new InputStreamResource(inputStream));
            
        } catch (Exception e) {
            log.error("代理下载文件失败: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * 代理访问MinIO中的文件
     * 
     * @param directory 目录
     * @param filename 文件名
     * @return 文件内容
     */
    @GetMapping("/proxy/{directory}/{filename:.+}")
    public ResponseEntity<byte[]> proxyFile(
            @PathVariable String directory,
            @PathVariable String filename) {
        
        try {
            String objectName = directory + "/" + filename;
            log.debug("代理访问文件: {}/{}", userFilesBucket, objectName);
            
            // 从MinIO获取对象
            InputStream stream = minioClient.getObject(
                    GetObjectArgs.builder()
                            .bucket(userFilesBucket)
                            .object(objectName)
                            .build()
            );
            
            // 获取对象的内容类型
            String contentType = determineContentType(filename);
            
            // 读取对象内容
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            byte[] buffer = new byte[16384];
            int bytesRead;
            while ((bytesRead = stream.read(buffer, 0, buffer.length)) != -1) {
                outputStream.write(buffer, 0, bytesRead);
            }
            byte[] bytes = outputStream.toByteArray();
            
            // 设置响应头
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.parseMediaType(contentType));
            headers.setContentLength(bytes.length);
            
            return new ResponseEntity<>(bytes, headers, HttpStatus.OK);
        } catch (MinioException e) {
            log.error("MinIO错误: {}", e.getMessage(), e);
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            log.error("代理文件失败: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    /**
     * 根据文件名确定内容类型
     * 
     * @param filename 文件名
     * @return 内容类型
     */
    private String determineContentType(String filename) {
        String extension = filename.substring(filename.lastIndexOf('.') + 1).toLowerCase();
        
        switch (extension) {
            case "jpg":
            case "jpeg":
                return "image/jpeg";
            case "png":
                return "image/png";
            case "gif":
                return "image/gif";
            case "webp":
                return "image/webp";
            case "svg":
                return "image/svg+xml";
            case "bmp":
                return "image/bmp";
            case "ico":
                return "image/x-icon";
            default:
                return "application/octet-stream";
        }
    }
} 