-- 新闻小程序及后台管理系统
-- MySQL 8.x 初始化脚本
-- 执行前请先选择数据库，例如：
-- CREATE DATABASE news_platform DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
-- USE news_platform;

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

DROP VIEW IF EXISTS `v_submission_audit_detail`;
DROP VIEW IF EXISTS `v_dynamic_news`;
DROP VIEW IF EXISTS `v_headline_news`;

DROP TABLE IF EXISTS `operation_log`;
DROP TABLE IF EXISTS `audit_log`;
DROP TABLE IF EXISTS `favorite`;
DROP TABLE IF EXISTS `comment`;
DROP TABLE IF EXISTS `news_media`;
DROP TABLE IF EXISTS `video`;
DROP TABLE IF EXISTS `post_submission`;
DROP TABLE IF EXISTS `news`;
DROP TABLE IF EXISTS `user_role`;
DROP TABLE IF EXISTS `role`;
DROP TABLE IF EXISTS `user`;

CREATE TABLE `user` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
  `username` VARCHAR(64) DEFAULT NULL COMMENT '后台登录用户名，可为空',
  `password` VARCHAR(255) DEFAULT NULL COMMENT '后台登录密码哈希，可为空',
  `nickname` VARCHAR(64) NOT NULL COMMENT '昵称',
  `avatar` VARCHAR(500) DEFAULT NULL COMMENT '头像地址',
  `open_id` VARCHAR(128) DEFAULT NULL COMMENT '微信 openId',
  `enabled` TINYINT NOT NULL DEFAULT 1 COMMENT '是否启用：1启用，0禁用',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_username` (`username`),
  UNIQUE KEY `uk_user_open_id` (`open_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户表';

CREATE TABLE `role` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
  `name` VARCHAR(64) NOT NULL COMMENT '角色名称',
  `code` VARCHAR(64) NOT NULL COMMENT '角色编码',
  `description` VARCHAR(255) DEFAULT NULL COMMENT '角色描述',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_role_code` (`code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='角色表';

CREATE TABLE `user_role` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
  `user_id` BIGINT NOT NULL COMMENT '用户ID',
  `role_id` BIGINT NOT NULL COMMENT '角色ID',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_role_user_role` (`user_id`, `role_id`),
  KEY `idx_user_role_role_id` (`role_id`),
  CONSTRAINT `fk_user_role_user` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`),
  CONSTRAINT `fk_user_role_role` FOREIGN KEY (`role_id`) REFERENCES `role` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户角色关联表';

CREATE TABLE `news` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
  `title` VARCHAR(255) NOT NULL COMMENT '标题',
  `summary` VARCHAR(500) DEFAULT NULL COMMENT '摘要',
  `content` TEXT COMMENT '正文',
  `category` VARCHAR(64) NOT NULL COMMENT '分类，用户投稿统一使用“用户投稿”',
  `cover_url` VARCHAR(500) DEFAULT NULL COMMENT '封面图',
  `source_name` VARCHAR(128) DEFAULT NULL COMMENT '来源名称，外部新闻使用',
  `source_url` VARCHAR(500) DEFAULT NULL COMMENT '来源链接，外部新闻使用',
  `origin_hash` CHAR(64) DEFAULT NULL COMMENT '外部新闻去重哈希',
  `status` VARCHAR(32) NOT NULL DEFAULT 'DRAFT' COMMENT '状态：DRAFT/PUBLISHED/REJECTED',
  `author_id` BIGINT DEFAULT NULL COMMENT '作者ID，用户动态或后台创建内容可填写',
  `published_at` DATETIME DEFAULT NULL COMMENT '发布时间',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_news_origin_hash` (`origin_hash`),
  KEY `idx_news_category_status` (`category`, `status`, `published_at`),
  KEY `idx_news_status_published_at` (`status`, `published_at`),
  KEY `idx_news_author_id` (`author_id`),
  CONSTRAINT `fk_news_author` FOREIGN KEY (`author_id`) REFERENCES `user` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='新闻与动态主表';

CREATE TABLE `news_media` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
  `news_id` BIGINT NOT NULL COMMENT '新闻ID',
  `media_type` VARCHAR(32) NOT NULL COMMENT '媒体类型：IMAGE/VIDEO',
  `url` VARCHAR(500) NOT NULL COMMENT '媒体地址',
  `sort_order` INT NOT NULL DEFAULT 0 COMMENT '排序值',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  KEY `idx_news_media_news_id` (`news_id`),
  CONSTRAINT `fk_news_media_news` FOREIGN KEY (`news_id`) REFERENCES `news` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='新闻媒体表';

CREATE TABLE `video` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
  `title` VARCHAR(255) NOT NULL COMMENT '标题',
  `description` VARCHAR(1000) DEFAULT NULL COMMENT '描述',
  `url` VARCHAR(500) NOT NULL COMMENT '视频地址',
  `cover_url` VARCHAR(500) DEFAULT NULL COMMENT '封面图',
  `category` VARCHAR(64) DEFAULT NULL COMMENT '分类',
  `status` VARCHAR(32) NOT NULL DEFAULT 'DRAFT' COMMENT '状态：DRAFT/PUBLISHED/REJECTED',
  `author_id` BIGINT DEFAULT NULL COMMENT '作者ID',
  `published_at` DATETIME DEFAULT NULL COMMENT '发布时间',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_video_category_status` (`category`, `status`, `published_at`),
  KEY `idx_video_author_id` (`author_id`),
  CONSTRAINT `fk_video_author` FOREIGN KEY (`author_id`) REFERENCES `user` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='视频表';

CREATE TABLE `post_submission` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
  `user_id` BIGINT NOT NULL COMMENT '投稿用户ID',
  `title` VARCHAR(255) NOT NULL COMMENT '投稿标题',
  `content` TEXT COMMENT '投稿正文',
  `media_type` VARCHAR(32) DEFAULT NULL COMMENT '媒体类型',
  `media_url` VARCHAR(500) DEFAULT NULL COMMENT '媒体地址',
  `status` VARCHAR(32) NOT NULL DEFAULT 'PENDING' COMMENT '状态：PENDING/APPROVED/REJECTED',
  `reviewer_id` BIGINT DEFAULT NULL COMMENT '审核人ID',
  `review_remark` VARCHAR(500) DEFAULT NULL COMMENT '审核备注或驳回原因',
  `published_news_id` BIGINT DEFAULT NULL COMMENT '审核通过后生成的新闻ID',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_submission_user_created` (`user_id`, `created_at`),
  KEY `idx_submission_status_created` (`status`, `created_at`),
  KEY `idx_submission_reviewer_id` (`reviewer_id`),
  KEY `idx_submission_published_news_id` (`published_news_id`),
  CONSTRAINT `fk_submission_user` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`),
  CONSTRAINT `fk_submission_reviewer` FOREIGN KEY (`reviewer_id`) REFERENCES `user` (`id`),
  CONSTRAINT `fk_submission_news` FOREIGN KEY (`published_news_id`) REFERENCES `news` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='投稿审核表';

CREATE TABLE `comment` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
  `biz_type` VARCHAR(32) NOT NULL COMMENT '业务类型：NEWS/VIDEO',
  `biz_id` BIGINT NOT NULL COMMENT '业务ID',
  `user_id` BIGINT NOT NULL COMMENT '评论用户ID',
  `parent_id` BIGINT DEFAULT NULL COMMENT '父评论ID',
  `root_id` BIGINT DEFAULT NULL COMMENT '根评论ID',
  `content` TEXT NOT NULL COMMENT '评论内容',
  `is_deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除：0未删，1已删',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_comment_biz` (`biz_type`, `biz_id`, `created_at`),
  KEY `idx_comment_user_id` (`user_id`),
  KEY `idx_comment_parent_id` (`parent_id`),
  KEY `idx_comment_root_id` (`root_id`),
  CONSTRAINT `fk_comment_user` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='评论表';

CREATE TABLE `favorite` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
  `user_id` BIGINT NOT NULL COMMENT '用户ID',
  `news_id` BIGINT NOT NULL COMMENT '新闻ID',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_favorite_user_news` (`user_id`, `news_id`),
  KEY `idx_favorite_news_id` (`news_id`),
  CONSTRAINT `fk_favorite_user` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`),
  CONSTRAINT `fk_favorite_news` FOREIGN KEY (`news_id`) REFERENCES `news` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='收藏表';

CREATE TABLE `audit_log` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
  `biz_type` VARCHAR(32) NOT NULL COMMENT '业务类型，如 submission',
  `biz_id` BIGINT NOT NULL COMMENT '业务ID',
  `action` VARCHAR(32) NOT NULL COMMENT '审核动作，如 APPROVED/REJECTED',
  `operator_id` BIGINT NOT NULL COMMENT '操作人ID',
  `remark` VARCHAR(500) DEFAULT NULL COMMENT '审核备注',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  KEY `idx_audit_log_biz` (`biz_type`, `biz_id`, `created_at`),
  KEY `idx_audit_log_operator_id` (`operator_id`),
  CONSTRAINT `fk_audit_log_operator` FOREIGN KEY (`operator_id`) REFERENCES `user` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='审核日志表';

CREATE TABLE `operation_log` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
  `module_name` VARCHAR(64) NOT NULL COMMENT '模块名',
  `action_name` VARCHAR(64) NOT NULL COMMENT '操作名',
  `operator_id` BIGINT DEFAULT NULL COMMENT '操作人ID',
  `detail` VARCHAR(1000) DEFAULT NULL COMMENT '操作详情',
  `ip` VARCHAR(64) DEFAULT NULL COMMENT '操作IP',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  KEY `idx_operation_log_module_action` (`module_name`, `action_name`, `created_at`),
  KEY `idx_operation_log_operator_id` (`operator_id`),
  CONSTRAINT `fk_operation_log_operator` FOREIGN KEY (`operator_id`) REFERENCES `user` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='操作日志表';

CREATE VIEW `v_headline_news` AS
SELECT
  n.id,
  n.title,
  n.summary,
  n.content,
  n.category,
  n.cover_url,
  n.source_name,
  n.source_url,
  n.status,
  n.author_id,
  n.published_at,
  n.created_at,
  n.updated_at
FROM `news` n
WHERE n.status = 'PUBLISHED'
  AND n.category <> '用户投稿';

CREATE VIEW `v_dynamic_news` AS
SELECT
  n.id,
  n.title,
  n.summary,
  n.content,
  n.category,
  n.cover_url,
  n.status,
  n.author_id,
  n.published_at,
  n.created_at,
  n.updated_at
FROM `news` n
WHERE n.status = 'PUBLISHED'
  AND n.category = '用户投稿';

CREATE VIEW `v_submission_audit_detail` AS
SELECT
  ps.id AS submission_id,
  ps.title,
  ps.content,
  ps.media_type,
  ps.media_url,
  ps.status,
  ps.review_remark,
  ps.created_at AS submission_created_at,
  ps.updated_at AS submission_updated_at,
  u.nickname AS submitter_nickname,
  u.id AS submitter_id,
  r.nickname AS reviewer_nickname,
  r.id AS reviewer_id,
  n.id AS published_news_id,
  n.title AS published_news_title,
  n.published_at
FROM `post_submission` ps
LEFT JOIN `user` u ON ps.user_id = u.id
LEFT JOIN `user` r ON ps.reviewer_id = r.id
LEFT JOIN `news` n ON ps.published_news_id = n.id;

SET FOREIGN_KEY_CHECKS = 1;

-- 可选初始化角色数据
INSERT INTO `role` (`name`, `code`, `description`) VALUES
('管理员', 'ADMIN', '系统管理员'),
('编辑', 'EDITOR', '内容编辑'),
('普通用户', 'USER', '小程序普通用户');
