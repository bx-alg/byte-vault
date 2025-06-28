package com.bytevault.app.file.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.bytevault.app.auth.model.UserDetailsImpl;
import com.bytevault.app.file.service.FileService;
import com.bytevault.app.model.FileInfo;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/files")
@RequiredArgsConstructor
public class FileController {

    private final FileService fileService;

    /**
     * 上传文件
     */
    @PostMapping("/upload")
    public ResponseEntity<Map<String, Object>> uploadFile(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "parentId", required = false, defaultValue = "0") Long parentId,
            @RequestParam(value = "isPublic", defaultValue = "false") boolean isPublic,
            @AuthenticationPrincipal UserDetailsImpl userDetails) {
        
        try {
            FileInfo fileInfo = fileService.uploadFile(file, userDetails.getId(), parentId, isPublic);
            
            Map<String, Object> response = new HashMap<>();
            response.put("message", "文件上传成功");
            response.put("fileId", fileInfo.getId());
            response.put("fileName", fileInfo.getFilename());
            response.put("fileSize", fileInfo.getFileSize());
            response.put("fileType", fileInfo.getFileType());
            response.put("isPublic", "public".equals(fileInfo.getVisibility()));
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("message", "文件上传失败: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    /**
     * 删除文件
     */
    @DeleteMapping("/{fileId}")
    public ResponseEntity<Map<String, Object>> deleteFile(
            @PathVariable Long fileId,
            @AuthenticationPrincipal UserDetailsImpl userDetails) {
        
        boolean success = fileService.deleteFile(fileId, userDetails.getId());
        
        Map<String, Object> response = new HashMap<>();
        if (success) {
            response.put("message", "文件删除成功");
            return ResponseEntity.ok(response);
        } else {
            response.put("message", "文件删除失败");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        }
    }

    /**
     * 获取文件下载URL
     */
    @GetMapping("/{fileId}/download")
    public ResponseEntity<Map<String, Object>> getFileDownloadUrl(
            @PathVariable Long fileId,
            @AuthenticationPrincipal UserDetailsImpl userDetails) {
        
        String downloadUrl = fileService.getFileDownloadUrl(fileId, userDetails.getId());
        
        Map<String, Object> response = new HashMap<>();
        if (downloadUrl != null) {
            response.put("message", "获取下载链接成功");
            response.put("downloadUrl", downloadUrl);
            return ResponseEntity.ok(response);
        } else {
            response.put("message", "获取下载链接失败");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        }
    }

    /**
     * 更新文件公开状态
     */
    @PutMapping("/{fileId}/public")
    public ResponseEntity<Map<String, Object>> updateFilePublicStatus(
            @PathVariable Long fileId,
            @RequestParam boolean isPublic,
            @AuthenticationPrincipal UserDetailsImpl userDetails) {
        
        boolean success = fileService.updateFilePublicStatus(fileId, userDetails.getId(), isPublic);
        
        Map<String, Object> response = new HashMap<>();
        if (success) {
            response.put("message", "文件公开状态更新成功");
            return ResponseEntity.ok(response);
        } else {
            response.put("message", "文件公开状态更新失败");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        }
    }

    /**
     * 获取用户文件列表
     */
    @GetMapping("/my")
    public ResponseEntity<Map<String, Object>> getUserFiles(
            @RequestParam(value = "parentId", required = false, defaultValue = "0") Long parentId,
            @RequestParam(value = "page", defaultValue = "1") int page,
            @RequestParam(value = "size", defaultValue = "10") int size,
            @AuthenticationPrincipal UserDetailsImpl userDetails) {
        
        IPage<FileInfo> files = fileService.getUserFiles(userDetails.getId(), parentId, page, size);
        
        Map<String, Object> response = new HashMap<>();
        response.put("message", "获取文件列表成功");
        response.put("files", files.getRecords());
        response.put("total", files.getTotal());
        response.put("pages", files.getPages());
        response.put("current", files.getCurrent());
        response.put("size", files.getSize());
        
        return ResponseEntity.ok(response);
    }

    /**
     * 获取公开文件列表
     */
    @GetMapping("/public")
    public ResponseEntity<Map<String, Object>> getPublicFiles(
            @RequestParam(value = "page", defaultValue = "1") int page,
            @RequestParam(value = "size", defaultValue = "10") int size) {
        
        IPage<FileInfo> files = fileService.getPublicFiles(page, size);
        
        Map<String, Object> response = new HashMap<>();
        response.put("message", "获取公开文件列表成功");
        response.put("files", files.getRecords());
        response.put("total", files.getTotal());
        response.put("pages", files.getPages());
        response.put("current", files.getCurrent());
        response.put("size", files.getSize());
        
        return ResponseEntity.ok(response);
    }

    /**
     * 搜索文件
     */
    @GetMapping("/search")
    public ResponseEntity<Map<String, Object>> searchFiles(
            @RequestParam String keyword,
            @RequestParam(value = "page", defaultValue = "1") int page,
            @RequestParam(value = "size", defaultValue = "10") int size,
            @AuthenticationPrincipal UserDetailsImpl userDetails) {
        
        IPage<FileInfo> files = fileService.searchFiles(userDetails.getId(), keyword, page, size);
        
        Map<String, Object> response = new HashMap<>();
        response.put("message", "搜索文件成功");
        response.put("files", files.getRecords());
        response.put("total", files.getTotal());
        response.put("pages", files.getPages());
        response.put("current", files.getCurrent());
        response.put("size", files.getSize());
        
        return ResponseEntity.ok(response);
    }

    /**
     * 获取文件详情
     */
    @GetMapping("/{fileId}")
    public ResponseEntity<Map<String, Object>> getFileInfo(
            @PathVariable Long fileId,
            @AuthenticationPrincipal UserDetailsImpl userDetails) {
        
        FileInfo fileInfo = fileService.getFileInfo(fileId);
        
        Map<String, Object> response = new HashMap<>();
        if (fileInfo != null) {
            // 检查权限
            if (fileInfo.getUserId().equals(userDetails.getId()) || "public".equals(fileInfo.getVisibility())) {
                // 生成下载URL（如果是文件）
                if (!fileInfo.getIsDir()) {
                    String downloadUrl = fileService.getFileDownloadUrl(fileId, userDetails.getId());
                    fileInfo.setDownloadUrl(downloadUrl);
                }
                
                response.put("message", "获取文件详情成功");
                response.put("file", fileInfo);
                return ResponseEntity.ok(response);
            } else {
                response.put("message", "无权限访问此文件");
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(response);
            }
        } else {
            response.put("message", "文件不存在");
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
        }
    }
    
    /**
     * 创建文件夹
     */
    @PostMapping("/folder")
    public ResponseEntity<Map<String, Object>> createFolder(
            @RequestParam String folderName,
            @RequestParam(value = "parentId", required = false, defaultValue = "0") Long parentId,
            @AuthenticationPrincipal UserDetailsImpl userDetails) {
        
        try {
            FileInfo folder = fileService.createFolder(userDetails.getId(), parentId, folderName);
            
            Map<String, Object> response = new HashMap<>();
            response.put("message", "文件夹创建成功");
            response.put("folderId", folder.getId());
            response.put("folderName", folder.getFilename());
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("message", "文件夹创建失败: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        }
    }
} 