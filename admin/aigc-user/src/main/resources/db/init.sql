CREATE DATABASE IF NOT EXISTS aigc_user DEFAULT CHARACTER SET utf8mb4;
USE aigc_user;

CREATE TABLE IF NOT EXISTS t_user (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '用户ID',
    username    VARCHAR(50) NOT NULL UNIQUE COMMENT '用户名',
    password    VARCHAR(255) NOT NULL COMMENT '加密密码',
    nickname    VARCHAR(50) COMMENT '昵称',
    email       VARCHAR(100) COMMENT '邮箱',
    avatar      VARCHAR(500) COMMENT '头像URL',
    status      TINYINT NOT NULL DEFAULT 1 COMMENT '0-禁用 1-正常',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted     TINYINT NOT NULL DEFAULT 0,
    INDEX idx_username (username)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户表';

-- 初始管理员账号 admin/admin123
INSERT IGNORE INTO t_user (username, password, nickname, status)
VALUES ('admin', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iAt.Ku.G', 'Admin', 1);
