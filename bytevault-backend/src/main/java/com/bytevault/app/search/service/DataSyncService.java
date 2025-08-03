package com.bytevault.app.search.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.bytevault.app.mapper.FileMapper;
import com.bytevault.app.mapper.UserMapper;
import com.bytevault.app.model.FileInfo;
import com.bytevault.app.model.User;
import com.bytevault.app.search.util.FileDocumentConverter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class DataSyncService {
    
    private final FileMapper fileMapper;
    private final UserMapper userMapper;
    private final FileSearchService fileSearchService;

    
    /**
     * 同步所有文件数据到ES
     */
    public void syncAllFilesToES() {
        log.info("开始同步所有文件数据到ES索引");
        
        try {
            // 先清理ES中的所有数据
            log.info("清理ES索引中的原有数据...");
            fileSearchService.clearAllFiles();
            
            // 查询所有未删除的文件
            LambdaQueryWrapper<FileInfo> queryWrapper = new LambdaQueryWrapper<>();
            queryWrapper.eq(FileInfo::getDeleted, false);
            List<FileInfo> allFiles = fileMapper.selectList(queryWrapper);
            
            // 获取所有用户信息，建立用户ID到用户名的映射
            List<User> allUsers = userMapper.selectList(null);
            Map<Long, String> userIdToNameMap = allUsers.stream()
                    .collect(Collectors.toMap(User::getId, User::getUsername));
            
            int successCount = 0;
            int failCount = 0;
            
            for (FileInfo fileInfo : allFiles) {
                try {
                    String username = userIdToNameMap.getOrDefault(fileInfo.getUserId(), "unknown");
                    fileSearchService.indexFile(FileDocumentConverter.toFileDocument(fileInfo, username));
                    successCount++;
                    
                    if (successCount % 100 == 0) {
                        log.info("已同步 {} 个文件到ES索引", successCount);
                    }
                } catch (Exception e) {
                    failCount++;
                    log.error("同步文件到ES失败: 文件ID={}, 文件名={}", fileInfo.getId(), fileInfo.getFilename(), e);
                }
            }
            
            log.info("文件数据同步完成: 总数={}, 成功={}, 失败={}", allFiles.size(), successCount, failCount);
        } catch (Exception e) {
            log.error("同步文件数据到ES失败", e);
            throw new RuntimeException("同步文件数据到ES失败", e);
        }
    }
    
    /**
     * 同步指定用户的文件数据到ES
     */
    public void syncUserFilesToES(Long userId) {
        log.info("开始同步用户 {} 的文件数据到ES索引", userId);
        
        try {
            // 获取用户信息
            User user = userMapper.selectById(userId);
            if (user == null) {
                log.warn("用户不存在: {}", userId);
                return;
            }
            
            // 先清理ES中该用户的所有数据
            log.info("清理ES索引中用户 {} 的原有数据...", userId);
            fileSearchService.clearUserFiles(userId);
            
            // 查询用户的所有未删除文件
            LambdaQueryWrapper<FileInfo> queryWrapper = new LambdaQueryWrapper<>();
            queryWrapper.eq(FileInfo::getUserId, userId)
                       .eq(FileInfo::getDeleted, false);
            List<FileInfo> userFiles = fileMapper.selectList(queryWrapper);
            
            int successCount = 0;
            int failCount = 0;
            
            for (FileInfo fileInfo : userFiles) {
                try {
                    fileSearchService.indexFile(FileDocumentConverter.toFileDocument(fileInfo, user.getUsername()));
                    successCount++;
                } catch (Exception e) {
                    failCount++;
                    log.error("同步文件到ES失败: 文件ID={}, 文件名={}", fileInfo.getId(), fileInfo.getFilename(), e);
                }
            }
            
            log.info("用户文件数据同步完成: 用户ID={}, 总数={}, 成功={}, 失败={}", 
                    userId, userFiles.size(), successCount, failCount);
        } catch (Exception e) {
            log.error("同步用户文件数据到ES失败: 用户ID={}", userId, e);
            throw new RuntimeException("同步用户文件数据到ES失败", e);
        }
    }
} 