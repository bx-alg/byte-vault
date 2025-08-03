package com.bytevault.app.search.controller;

import com.bytevault.app.search.service.DataSyncService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/search")
@RequiredArgsConstructor
public class SearchAdminController {
    
    private final DataSyncService dataSyncService;
    
    /**
     * 同步所有文件数据到ES（先清理原有数据，再重新同步）
     * 只有管理员可以执行全量同步
     */
    @PostMapping("/sync/all")
    @PreAuthorize("hasRole('admin')")
    public ResponseEntity<String> syncAllFiles() {
        try {
            dataSyncService.syncAllFilesToES();
            return ResponseEntity.ok("所有文件数据同步成功");
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("同步失败: " + e.getMessage());
        }
    }
    
    /**
     * 管理员同步指定用户的文件数据到ES
     * 只有管理员可以同步其他用户的文件
     */
    @PostMapping("/sync/user/{userId}")
    @PreAuthorize("hasRole('admin')")
    public ResponseEntity<String> syncUserFilesByAdmin(@PathVariable Long userId) {
        try {
            dataSyncService.syncUserFilesToES(userId);
            return ResponseEntity.ok("用户文件数据同步成功");
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("同步失败: " + e.getMessage());
        }
    }
} 