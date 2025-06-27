-- 创建数据库
CREATE DATABASE IF NOT EXISTS bytevault DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;

USE bytevault;


-- 创建用户表
CREATE TABLE `user` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
    `username` VARCHAR(64) NOT NULL UNIQUE,
    `password` VARCHAR(255) NOT NULL,
    `avatar_url` VARCHAR(255) NULL COMMENT '头像',
    `status` TINYINT DEFAULT 1 COMMENT '1正常，0禁用',
    `deleted` TINYINT DEFAULT 0 COMMENT '是否删除',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

-- 创建角色表
CREATE TABLE `role` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
    `name` VARCHAR(64) NOT NULL UNIQUE,
    `description` VARCHAR(255),
    `deleted` TINYINT DEFAULT 0 COMMENT '是否删除'
);

-- 创建用户角色关联表
CREATE TABLE `user_role` (
    `user_id` BIGINT NOT NULL,
    `role_id` BIGINT NOT NULL,
    `deleted` TINYINT DEFAULT 0 COMMENT '是否删除',
    PRIMARY KEY (`user_id`, `role_id`)
);

-- 创建权限表
CREATE TABLE `permission` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
    `name` VARCHAR(64) NOT NULL UNIQUE,
    `description` VARCHAR(255),
    `deleted` TINYINT DEFAULT 0 COMMENT '是否删除'
);

-- 创建角色权限关联表
CREATE TABLE `role_permission` (
    `role_id` BIGINT NOT NULL,
    `permission_id` BIGINT NOT NULL,
    `deleted` TINYINT DEFAULT 0,
    PRIMARY KEY (`role_id`, `permission_id`)
);


-- 添加管理员用户（密码：admin）
INSERT INTO `user` (username, password) VALUES
('admin', '$2a$10$CehLbipOZVv0VqQtxx3L2ehR5OXZtEvQWPL1DaU4TXsGfq66yINjW');

-- 添加 admin 角色
INSERT INTO `role` (name, description) VALUES
('admin', '管理员');

-- 绑定 admin 用户的角色
INSERT INTO `user_role` (user_id, role_id) VALUES
(1, 1);

-- 添加权限（可选）
INSERT INTO `permission` (name, description) VALUES
('file:upload', '上传文件'),
('file:delete', '删除文件'),
('file:share', '分享文件');

-- admin 拥有全部权限
INSERT INTO `role_permission` (role_id, permission_id) VALUES
(1, 1),
(1, 2),
(1, 3);