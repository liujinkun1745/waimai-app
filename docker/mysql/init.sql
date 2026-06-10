-- 外卖平台初始化 SQL
-- 主数据库（非分片表）
CREATE DATABASE IF NOT EXISTS waimai CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

-- 分片数据库（订单表）
CREATE DATABASE IF NOT EXISTS waimai_ds0 CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE DATABASE IF NOT EXISTS waimai_ds1 CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE DATABASE IF NOT EXISTS waimai_ds2 CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE DATABASE IF NOT EXISTS waimai_ds3 CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

USE waimai;

-- Seata AT 模式 UndoLog 表（主库）
CREATE TABLE IF NOT EXISTS undo_log (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    branch_id BIGINT NOT NULL,
    xid VARCHAR(128) NOT NULL,
    context VARCHAR(128) NOT NULL,
    rollback_info LONGBLOB NOT NULL,
    log_status INT NOT NULL,
    log_created DATETIME NOT NULL,
    log_modified DATETIME NOT NULL,
    UNIQUE KEY ux_undo_log (xid, branch_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 本地消息表（最终一致性备选）
CREATE TABLE IF NOT EXISTS local_message (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    business_key VARCHAR(64) NOT NULL,
    topic VARCHAR(64) NOT NULL,
    tag VARCHAR(64),
    message_body TEXT,
    status VARCHAR(10) NOT NULL DEFAULT '待发送',
    retry_count INT NOT NULL DEFAULT 0,
    max_retry_count INT NOT NULL DEFAULT 10,
    next_retry_at DATETIME,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_status (status),
    INDEX idx_business_key (business_key)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 在每个分片库也建 undo_log
USE waimai_ds0;
CREATE TABLE IF NOT EXISTS undo_log LIKE waimai.undo_log;

USE waimai_ds1;
CREATE TABLE IF NOT EXISTS undo_log LIKE waimai.undo_log;

USE waimai_ds2;
CREATE TABLE IF NOT EXISTS undo_log LIKE waimai.undo_log;

USE waimai_ds3;
CREATE TABLE IF NOT EXISTS undo_log LIKE waimai.undo_log;

-- 表结构由 JPA ddl-auto: update 自动创建
