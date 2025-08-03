package com.bytevault.app.search.controller;

import com.bytevault.app.search.service.DataSyncService;
import com.bytevault.app.auth.model.UserDetailsImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/search")
@RequiredArgsConstructor
public class SearchController {
    
    private final DataSyncService dataSyncService;
    
    /**
     * 用户同步自己的文件数据到ES
     * 任何认证用户都可以同步自己的文件
     */
    @PostMapping("/sync/my-files")
    public ResponseEntity<String> syncMyFiles(@AuthenticationPrincipal UserDetailsImpl userDetails) {
        try {
            Long userId = userDetails.getUser().getId();
            dataSyncService.syncUserFilesToES(userId);
            return ResponseEntity.ok("您的文件数据同步成功");
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("同步失败: " + e.getMessage());
        }
    }
} 