-- Active: 1751014198967@@127.0.0.1@3306@bytevault
drop table if exists `user_files`;

-- 创建用户文件表
CREATE TABLE user_files (
    id           BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id      BIGINT NOT NULL,
    parent_id    BIGINT DEFAULT NULL,
    file_name    VARCHAR(255) NOT NULL,
    file_size    BIGINT,
    file_type    VARCHAR(255),
    is_dir       BOOLEAN DEFAULT FALSE,
    visibility   ENUM('private', 'public') DEFAULT 'private',
    is_deleted   BOOLEAN DEFAULT FALSE,
    create_time  DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time  DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);


create table user_background_images
(
    id          bigint primary key auto_increment,
    user_id     bigint not null,
    image_url   varchar(255) not null,
    upload_time timestamp default current_timestamp,
    is_deleted  tinyint default 0 comment '逻辑删除标志，0=正常，1=已删除'

);
-- 背景图片索引
CREATE INDEX idx_user_id ON user_background_images(user_id);

-- 查询某用户某文件夹下的文件（核心索引）
CREATE INDEX idx_user_folder ON user_files(user_id, parent_id, is_deleted);

-- 最近修改排序用
CREATE INDEX idx_user_update_time ON user_files(user_id, is_deleted, update_time);

-- 文件名前缀查询
CREATE INDEX idx_file_name_prefix ON user_files(file_name);