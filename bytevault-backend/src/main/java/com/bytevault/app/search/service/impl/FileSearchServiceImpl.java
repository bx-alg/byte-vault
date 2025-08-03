package com.bytevault.app.search.service.impl;

import com.bytevault.app.search.document.FileDocument;
import com.bytevault.app.search.repository.FileSearchRepository;
import com.bytevault.app.search.service.FileSearchService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class FileSearchServiceImpl implements FileSearchService {
    
    private final FileSearchRepository fileSearchRepository;
    
    @Override
    public void indexFile(FileDocument fileDocument) {
        try {
            fileSearchRepository.save(fileDocument);
            log.info("文件已同步到ES索引: {}", fileDocument.getFileName());
        } catch (Exception e) {
            log.error("同步文件到ES索引失败: {}", fileDocument.getFileName(), e);
        }
    }
    
    @Override
    public void deleteFile(Long fileId) {
        try {
            fileSearchRepository.deleteById(fileId);
            log.info("文件已从ES索引删除: {}", fileId);
        } catch (Exception e) {
            log.error("从ES索引删除文件失败: {}", fileId, e);
        }
    }
    
    @Override
    public Page<FileDocument> searchPrivateFiles(Long userId, String keyword, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "updateTime"));
        return fileSearchRepository.findByFileNameContainingAndUserIdAndVisibilityAndIsDeleted(
                keyword, userId, "private", false, pageable);
    }
    
    @Override
    public Page<FileDocument> searchPublicFiles(String keyword, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "updateTime"));
        return fileSearchRepository.findByFileNameContainingAndVisibilityAndIsDeleted(
                keyword, "public", false, pageable);
    }
    
    @Override
    public Page<FileDocument> searchPublicFilesByUsername(String username, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "updateTime"));
        return fileSearchRepository.findByUsernameContainingAndVisibilityAndIsDeleted(
                username, "public", false, pageable);
    }
    
    @Override
    public Page<FileDocument> getUserPrivateFiles(Long userId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "updateTime"));
        return fileSearchRepository.findByUserIdAndVisibilityAndIsDeleted(
                userId, "private", false, pageable);
    }
    
    @Override
    public Page<FileDocument> getAllPublicFiles(int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "updateTime"));
        return fileSearchRepository.findByVisibilityAndIsDeleted("public", false, pageable);
    }
    
    @Override
    public void clearAllFiles() {
        try {
            fileSearchRepository.deleteAll();
            log.info("已清理ES索引中的所有文件数据");
        } catch (Exception e) {
            log.error("清理ES索引中的所有文件数据失败", e);
            throw new RuntimeException("清理ES索引失败", e);
        }
    }
    
    @Override
    public void clearUserFiles(Long userId) {
        try {
            fileSearchRepository.deleteByUserId(userId);
            log.info("已清理ES索引中用户 {} 的所有文件数据", userId);
        } catch (Exception e) {
            log.error("清理ES索引中用户 {} 的文件数据失败", userId, e);
            throw new RuntimeException("清理用户ES索引失败", e);
        }
    }
} 