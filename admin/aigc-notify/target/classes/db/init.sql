CREATE DATABASE IF NOT EXISTS aigc_notify DEFAULT CHARACTER SET utf8mb4;
USE aigc_notify;

CREATE TABLE IF NOT EXISTS t_message (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '消息ID',
    receiver_id BIGINT NOT NULL COMMENT '接收者用户ID',
    sender_id   BIGINT COMMENT '发送者用户ID，NULL=系统消息',
    type        VARCHAR(50) NOT NULL DEFAULT 'system' COMMENT 'system/business/alert',
    title       VARCHAR(200) NOT NULL COMMENT '消息标题',
    content     TEXT NOT NULL COMMENT '消息内容',
    extra       JSON COMMENT '扩展数据',
    is_read     TINYINT NOT NULL DEFAULT 0 COMMENT '0-未读 1-已读',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_receiver (receiver_id),
    INDEX idx_receiver_unread (receiver_id, is_read)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='站内消息表';
