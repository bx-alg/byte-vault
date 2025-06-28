drop table if exists `user_files`;

-- 创建用户文件表
CREATE TABLE user_files (
    id           BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id      BIGINT NOT NULL,
    parent_id    BIGINT DEFAULT NULL,
    object_name  VARCHAR(255) NOT NULL,
    file_name    VARCHAR(255) NOT NULL,
    file_size    BIGINT,
    file_type    VARCHAR(64),
    is_dir       BOOLEAN DEFAULT FALSE,
    visibility   ENUM('private', 'public') DEFAULT 'private',
    is_deleted   BOOLEAN DEFAULT FALSE,
    create_time  DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time  DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);


-- 查询某用户某文件夹下的文件（核心索引）
CREATE INDEX idx_user_folder ON user_files(user_id, parent_id, is_deleted);

-- 最近修改排序用
CREATE INDEX idx_user_update_time ON user_files(user_id, is_deleted, update_time);

-- 文件名前缀查询
CREATE INDEX idx_file_name_prefix ON user_files(file_name);