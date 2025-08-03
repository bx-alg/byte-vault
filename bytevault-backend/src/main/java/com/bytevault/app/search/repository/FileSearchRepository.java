package com.bytevault.app.search.repository;

import com.bytevault.app.search.document.FileDocument;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface FileSearchRepository extends ElasticsearchRepository<FileDocument, Long> {
    
    /**
     * 根据文件名搜索私有文件
     * @param fileName 文件名
     * @param userId 用户ID
     * @param pageable 分页参数
     * @return 搜索结果
     */
    Page<FileDocument> findByFileNameContainingAndUserIdAndVisibilityAndIsDeleted(
            String fileName, Long userId, String visibility, Boolean isDeleted, Pageable pageable);
    
    /**
     * 根据文件名搜索公开文件
     * @param fileName 文件名
     * @param pageable 分页参数
     * @return 搜索结果
     */
    Page<FileDocument> findByFileNameContainingAndVisibilityAndIsDeleted(
            String fileName, String visibility, Boolean isDeleted, Pageable pageable);
    
    /**
     * 根据用户名搜索公开文件
     * @param username 用户名
     * @param pageable 分页参数
     * @return 搜索结果
     */
    Page<FileDocument> findByUsernameContainingAndVisibilityAndIsDeleted(
            String username, String visibility, Boolean isDeleted, Pageable pageable);
    
    /**
     * 根据用户ID查找所有私有文件
     * @param userId 用户ID
     * @param pageable 分页参数
     * @return 搜索结果
     */
    Page<FileDocument> findByUserIdAndVisibilityAndIsDeleted(
            Long userId, String visibility, Boolean isDeleted, Pageable pageable);
    
    /**
     * 查找所有公开文件
     * @param pageable 分页参数
     * @return 搜索结果
     */
    Page<FileDocument> findByVisibilityAndIsDeleted(String visibility, Boolean isDeleted, Pageable pageable);
    
    /**
     * 删除指定用户的所有文件
     * @param userId 用户ID
     */
    void deleteByUserId(Long userId);
} 