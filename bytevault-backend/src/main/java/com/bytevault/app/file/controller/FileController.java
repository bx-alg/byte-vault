package com.bytevault.app.file.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.bytevault.app.auth.model.UserDetailsImpl;
import com.bytevault.app.file.service.FileService;
import com.bytevault.app.model.FileInfo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/files")
@RequiredArgsConstructor
@Slf4j
public class FileController {

    private final FileService fileService;

    /**
     * 上传文件 (使用断点续传实现)
     */
    @PostMapping("/upload")
    public ResponseEntity<Map<String, Object>> uploadFile(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "parentId", required = false, defaultValue = "0") Long parentId,
            @RequestParam(value = "isPublic", defaultValue = "false") boolean isPublic,
            @AuthenticationPrincipal UserDetailsImpl userDetails) {

        try {
            // 使用断点续传逻辑处理单个文件上传
            String filename = file.getOriginalFilename();
            Long fileSize = file.getSize();
            String fileType = file.getContentType();
            
            // 1. 初始化上传
            String uploadId = fileService.initChunkUpload(filename, fileSize, fileType, userDetails.getId(), parentId, isPublic);
            
            // 2. 上传单个分块
            fileService.uploadChunk(uploadId, 0, file, userDetails.getId());
            
            // 3. 完成上传
            FileInfo fileInfo = fileService.completeChunkUpload(uploadId, 1, userDetails.getId());

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
     * 上传文件夹
     */
    @PostMapping("/upload-folder")
    public ResponseEntity<Map<String, Object>> uploadFolder(
            @RequestParam("files") List<MultipartFile> files,
            @RequestParam("relativePaths") List<String> relativePaths,
            @RequestParam(value = "parentId", required = false, defaultValue = "0") Long parentId,
            @RequestParam(value = "isPublic", defaultValue = "false") boolean isPublic,
            @AuthenticationPrincipal UserDetailsImpl userDetails) {

        try {
            List<FileInfo> uploadedFiles = fileService.uploadFolder(files, relativePaths, userDetails.getId(), parentId,
                    isPublic);

            Map<String, Object> response = new HashMap<>();
            response.put("message", "文件夹上传成功");
            response.put("fileCount", uploadedFiles.size());

            List<Map<String, Object>> fileInfoList = new ArrayList<>();
            for (FileInfo fileInfo : uploadedFiles) {
                Map<String, Object> fileInfoMap = new HashMap<>();
                fileInfoMap.put("id", fileInfo.getId());
                fileInfoMap.put("name", fileInfo.getFilename());
                fileInfoMap.put("isDir", fileInfo.getIsDir());
                fileInfoMap.put("parentId", fileInfo.getParentId());
                fileInfoList.add(fileInfoMap);
            }
            response.put("files", fileInfoList);

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("message", "文件夹上传失败: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    /**
     * 删除文件
     * 如果是文件夹，会递归删除所有子文件和子文件夹
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

    /**
     * 更新文件夹公开状态（包括所有子文件）
     */
    @PutMapping("/{folderId}/folder-public")
    public ResponseEntity<Map<String, Object>> updateFolderPublicStatus(
            @PathVariable Long folderId,
            @RequestParam boolean isPublic,
            @AuthenticationPrincipal UserDetailsImpl userDetails) {

        boolean success = fileService.updateFolderPublicStatus(folderId, userDetails.getId(), isPublic);

        Map<String, Object> response = new HashMap<>();
        if (success) {
            response.put("message", "文件夹及其子文件公开状态更新成功");
            return ResponseEntity.ok(response);
        } else {
            response.put("message", "文件夹公开状态更新失败");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        }
    }

    /**
     * 初始化分块上传
     */
    @PostMapping("/chunk/init")
    public ResponseEntity<Map<String, Object>> initChunkUpload(
            @RequestBody Map<String, Object> request,
            @AuthenticationPrincipal UserDetailsImpl userDetails) {

        try {
            String filename = (String) request.get("filename");
            Long fileSize = Long.valueOf(request.get("fileSize").toString());
            String fileType = (String) request.get("fileType");
            Long parentId = Long.valueOf(request.get("parentId").toString());
            boolean isPublic = Boolean.parseBoolean(request.get("isPublic").toString());

            String uploadId = fileService.initChunkUpload(filename, fileSize, fileType, userDetails.getId(), parentId, isPublic);

            Map<String, Object> response = new HashMap<>();
            response.put("message", "初始化分块上传成功");
            response.put("uploadId", uploadId);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("message", "初始化分块上传失败: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    /**
     * 上传分块
     */
    @PostMapping("/chunk/upload")
    public ResponseEntity<Map<String, Object>> uploadChunk(
            @RequestParam("uploadId") String uploadId,
            @RequestParam("chunkIndex") int chunkIndex,
            @RequestParam("chunk") MultipartFile chunk,
            @AuthenticationPrincipal UserDetailsImpl userDetails) {
        
        Map<String, Object> response = new HashMap<>();
        
        try {
            log.info("上传分块: uploadId={}, chunkIndex={}, chunkSize={} bytes", uploadId, chunkIndex, chunk.getSize());
            
            boolean result = fileService.uploadChunk(uploadId, chunkIndex, chunk, userDetails.getId());
            
            if (result) {
                response.put("success", true);
                response.put("message", "分块上传成功");
                return ResponseEntity.ok(response);
            } else {
                response.put("success", false);
                response.put("message", "分块上传失败");
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
            }
        } catch (Exception e) {
            log.error("分块上传异常: {}", e.getMessage(), e);
            response.put("success", false);
            response.put("message", "分块上传异常: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    /**
     * 获取已上传的分块列表
     */
    @GetMapping("/chunk/uploaded/{uploadId}")
    public ResponseEntity<Map<String, Object>> getUploadedChunks(
            @PathVariable String uploadId,
            @AuthenticationPrincipal UserDetailsImpl userDetails) {

        try {
            List<Integer> uploadedChunks = fileService.getUploadedChunks(uploadId, userDetails.getId());

            Map<String, Object> response = new HashMap<>();
            response.put("message", "获取已上传分块列表成功");
            response.put("uploadedChunks", uploadedChunks);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("message", "获取已上传分块列表失败: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    /**
     * 完成分块上传
     */
    @PostMapping("/chunk/complete/{uploadId}")
    public ResponseEntity<Map<String, Object>> completeChunkUpload(
            @PathVariable String uploadId,
            @RequestBody Map<String, Object> request,
            @AuthenticationPrincipal UserDetailsImpl userDetails) {

        try {
            int totalChunks = Integer.parseInt(request.get("totalChunks").toString());
            FileInfo fileInfo = fileService.completeChunkUpload(uploadId, totalChunks, userDetails.getId());

            Map<String, Object> response = new HashMap<>();
            response.put("message", "文件上传完成");
            response.put("fileId", fileInfo.getId());
            response.put("fileName", fileInfo.getFilename());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("message", "完成文件上传失败: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }
}