package com.bytevault.app.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.bytevault.app.model.FileInfo;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface FileMapper extends BaseMapper<FileInfo> {
    
    /**
     * 查询用户的文件列表
     * @param page 分页对象
     * @param userId 用户ID
     * @param parentId 父目录ID
     * @return 分页文件列表
     */
    @Select("SELECT f.*, u.username as ownerName FROM user_files f " +
            "LEFT JOIN user u ON f.user_id = u.id " +
            "WHERE f.user_id = #{userId} AND f.parent_id = #{parentId} AND f.is_deleted = false " +
            "ORDER BY f.is_dir DESC, f.update_time DESC")
    IPage<FileInfo> selectUserFiles(Page<FileInfo> page, @Param("userId") Long userId, @Param("parentId") Long parentId);
    
    /**
     * 查询公开文件列表
     * @param page 分页对象
     * @return 分页公开文件列表
     */
    @Select("SELECT f.*, u.username as ownerName FROM user_files f " +
            "LEFT JOIN user u ON f.user_id = u.id " +
            "WHERE f.visibility = 'public' AND f.is_deleted = false " +
            "ORDER BY f.update_time DESC")
    IPage<FileInfo> selectPublicFiles(Page<FileInfo> page);
    
    /**
     * 搜索文件
     * @param page 分页对象
     * @param userId 当前用户ID
     * @param keyword 关键词
     * @return 分页搜索结果
     */
    @Select("SELECT f.*, u.username as ownerName FROM user_files f " +
            "LEFT JOIN user u ON f.user_id = u.id " +
            "WHERE (f.user_id = #{userId} OR f.visibility = 'public') AND f.is_deleted = false " +
            "AND (f.file_name LIKE CONCAT('%', #{keyword}, '%') OR u.username LIKE CONCAT('%', #{keyword}, '%')) " +
            "ORDER BY f.update_time DESC")
    IPage<FileInfo> searchFiles(Page<FileInfo> page, @Param("userId") Long userId, @Param("keyword") String keyword);
} 