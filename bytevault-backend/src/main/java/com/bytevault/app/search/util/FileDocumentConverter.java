package com.bytevault.app.search.util;

import com.bytevault.app.model.FileInfo;
import com.bytevault.app.search.document.FileDocument;

public class FileDocumentConverter {
    
    /**
     * 将FileInfo转换为FileDocument
     */
    public static FileDocument toFileDocument(FileInfo fileInfo, String username) {
        return FileDocument.builder()
                .id(fileInfo.getId())
                .userId(fileInfo.getUserId())
                .username(username)
                .parentId(fileInfo.getParentId())
                .fileName(fileInfo.getFilename())
                .fileSize(fileInfo.getFileSize())
                .fileType(fileInfo.getFileType())
                .isDir(fileInfo.getIsDir())
                .visibility(fileInfo.getVisibility())
                .isDeleted(fileInfo.getDeleted())
                .createTime(fileInfo.getCreateTime())
                .updateTime(fileInfo.getUpdateTime())
                .build();
    }
    
    /**
     * 将FileDocument转换为FileInfo
     */
    public static FileInfo toFileInfo(FileDocument fileDocument) {
        return FileInfo.builder()
                .id(fileDocument.getId())
                .userId(fileDocument.getUserId())
                .parentId(fileDocument.getParentId())
                .filename(fileDocument.getFileName())
                .fileSize(fileDocument.getFileSize())
                .fileType(fileDocument.getFileType())
                .isDir(fileDocument.getIsDir())
                .visibility(fileDocument.getVisibility())
                .deleted(fileDocument.getIsDeleted())
                .createTime(fileDocument.getCreateTime())
                .updateTime(fileDocument.getUpdateTime())
                .ownerName(fileDocument.getUsername())
                .build();
    }
} 