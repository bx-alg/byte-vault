package com.bytevault.app.search.controller;

import com.bytevault.app.search.document.FileDocument;
import com.bytevault.app.search.service.FileSearchService;
import com.bytevault.app.auth.config.JwtUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;

@RestController
@RequestMapping("/api/search")
@RequiredArgsConstructor
public class FileSearchController {
    
    private final FileSearchService fileSearchService;
    private final JwtUtils jwtUtils;
    
    /**
     * 搜索私有文件
     */
    @GetMapping("/private")
    public ResponseEntity<Page<FileDocument>> searchPrivateFiles(
            @RequestParam String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            HttpServletRequest request) {
        
        Long userId = getCurrentUserId(request);
        Page<FileDocument> result = fileSearchService.searchPrivateFiles(userId, keyword, page, size);
        return ResponseEntity.ok(result);
    }
    
    /**
     * 搜索公开文件
     */
    @GetMapping("/public")
    public ResponseEntity<Page<FileDocument>> searchPublicFiles(
            @RequestParam String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        
        Page<FileDocument> result = fileSearchService.searchPublicFiles(keyword, page, size);
        return ResponseEntity.ok(result);
    }
    
    /**
     * 根据用户名搜索公开文件
     */
    @GetMapping("/public/by-user")
    public ResponseEntity<Page<FileDocument>> searchPublicFilesByUsername(
            @RequestParam String username,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        
        Page<FileDocument> result = fileSearchService.searchPublicFilesByUsername(username, page, size);
        return ResponseEntity.ok(result);
    }
    
    /**
     * 获取用户的所有私有文件
     */
    @GetMapping("/private/all")
    public ResponseEntity<Page<FileDocument>> getUserPrivateFiles(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            HttpServletRequest request) {
        
        Long userId = getCurrentUserId(request);
        Page<FileDocument> result = fileSearchService.getUserPrivateFiles(userId, page, size);
        return ResponseEntity.ok(result);
    }
    
    /**
     * 获取所有公开文件
     */
    @GetMapping("/public/all")
    public ResponseEntity<Page<FileDocument>> getAllPublicFiles(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        
        Page<FileDocument> result = fileSearchService.getAllPublicFiles(page, size);
        return ResponseEntity.ok(result);
    }
    
    /**
     * 从请求中获取当前用户ID
     */
    private Long getCurrentUserId(HttpServletRequest request) {
        String token = request.getHeader("Authorization");
        if (token != null && token.startsWith("Bearer ")) {
            token = token.substring(7);
            return jwtUtils.getUserIdFromToken(token);
        }
        throw new RuntimeException("未找到有效的用户认证信息");
    }
} 