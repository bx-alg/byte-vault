package com.bytevault.app.model;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableField;
import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("user_files")
public class FileInfo {

    @TableId(type = IdType.AUTO)
    private Long id;
    
    @TableField("user_id")
    private Long userId;
    
    @TableField("file_name")
    private String filename;
    
    @TableField("parent_id")
    private Long parentId;
    
    @TableField("object_name")
    private String objectName;
    
    @TableField("file_size")
    private Long fileSize;
    
    @TableField("file_type")
    private String fileType;
    
    @TableField("is_dir")
    @Builder.Default
    private Boolean isDir = false;
    
    @TableField("visibility")
    @Builder.Default
    private String visibility = "private";
    
    @TableField("is_deleted")
    @TableLogic
    @Builder.Default
    private Boolean deleted = false;
    
    @TableField("create_time")
    private LocalDateTime createTime;
    
    @TableField("update_time")
    private LocalDateTime updateTime;
    
    // 非数据库字段，用于前端展示
    @TableField(exist = false)
    private String ownerName;
    
    @TableField(exist = false)
    private String downloadUrl;
} 