package com.bytevault.app.search.document;

import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;
import org.springframework.data.elasticsearch.annotations.MultiField;
import org.springframework.data.elasticsearch.annotations.InnerField;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(indexName = "user_files")
public class FileDocument {
    
    @Id
    private Long id;
    
    @Field(type = FieldType.Long, name = "user_id")
    private Long userId;
    
    @MultiField(
        mainField = @Field(type = FieldType.Text, name = "username", analyzer = "ik_max_word", searchAnalyzer = "ik_max_word"),
        otherFields = {
            @InnerField(suffix = "keyword", type = FieldType.Keyword)
        }
    )
    private String username;
    
    @Field(type = FieldType.Long, name = "parent_id")
    private Long parentId;
    
    @MultiField(
        mainField = @Field(type = FieldType.Text, name = "file_name", analyzer = "ik_max_word", searchAnalyzer = "ik_max_word"),
        otherFields = {
            @InnerField(suffix = "keyword", type = FieldType.Keyword)
        }
    )
    private String fileName;
    
    @Field(type = FieldType.Long, name = "file_size")
    private Long fileSize;
    
    @Field(type = FieldType.Keyword, name = "file_type")
    private String fileType;
    
    @Field(type = FieldType.Boolean, name = "is_dir")
    private Boolean isDir;
    
    @Field(type = FieldType.Keyword, name = "visibility")
    private String visibility;
    
    @Field(type = FieldType.Boolean, name = "is_deleted")
    private Boolean isDeleted;
    
    @Field(type = FieldType.Date, name = "create_time", format = {}, pattern = "yyyy-MM-dd HH:mm:ss")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;
    
    @Field(type = FieldType.Date, name = "update_time", format = {}, pattern = "yyyy-MM-dd HH:mm:ss")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updateTime;
} 