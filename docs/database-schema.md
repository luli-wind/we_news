# 新闻小程序及后台管理系统数据库设计

本文档基于当前项目代码和 `PLAN.md` 的最新方案整理，目标是给出一份可直接用于课程作业说明的数据库设计文档。

设计原则如下：

- 官方新闻与审核通过后的用户动态统一存入 `news` 表
- 用户投稿原稿、审核状态、审核意见存入 `post_submission` 表
- 首页双频道通过 `news.category` 区分：
  - `category = '用户投稿'` 表示“动态”
  - 其他分类表示“头条”
- 评论、收藏统一关联 `news.id`

## 1. 表清单

本项目实际使用到的核心表如下：

| 表名 | 说明 |
| --- | --- |
| `user` | 用户表，既包含小程序用户，也包含后台账号 |
| `role` | 角色表 |
| `user_role` | 用户与角色关联表 |
| `news` | 新闻内容主表，存官方新闻和审核通过后的用户动态 |
| `news_media` | 新闻媒体资源表 |
| `video` | 视频表 |
| `comment` | 评论表，支持父评论/子评论 |
| `favorite` | 收藏表 |
| `post_submission` | 用户投稿与审核表 |
| `audit_log` | 审核日志表 |
| `operation_log` | 操作日志表 |

## 2. 表结构定义

以下 SQL 以 MySQL 8.x 为基准，字符集建议统一使用 `utf8mb4`。

### 2.1 `user`

用途：保存小程序普通用户和后台可登录用户的基础信息。

```sql
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
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户表';
```

### 2.2 `role`

用途：保存系统角色，如 `ADMIN`、`EDITOR`、`USER`。

```sql
CREATE TABLE `role` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
  `name` VARCHAR(64) NOT NULL COMMENT '角色名称',
  `code` VARCHAR(64) NOT NULL COMMENT '角色编码',
  `description` VARCHAR(255) DEFAULT NULL COMMENT '角色描述',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_role_code` (`code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='角色表';
```

### 2.3 `user_role`

用途：建立用户与角色的多对多关系。

```sql
CREATE TABLE `user_role` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
  `user_id` BIGINT NOT NULL COMMENT '用户ID',
  `role_id` BIGINT NOT NULL COMMENT '角色ID',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_role_user_role` (`user_id`, `role_id`),
  KEY `idx_user_role_role_id` (`role_id`),
  CONSTRAINT `fk_user_role_user` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`),
  CONSTRAINT `fk_user_role_role` FOREIGN KEY (`role_id`) REFERENCES `role` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户角色关联表';
```

### 2.4 `news`

用途：内容主表，保存：

- 后台发布的官方新闻
- 外部接口同步的新闻
- 投稿审核通过后生成的用户动态

频道约定：

- `category = '用户投稿'`：动态
- `category <> '用户投稿'`：头条

```sql
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
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='新闻与动态主表';
```

### 2.5 `news_media`

用途：保存新闻正文关联的多媒体资源，支持一条新闻多图。

```sql
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
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='新闻媒体表';
```

### 2.6 `video`

用途：保存视频频道内容。

```sql
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
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='视频表';
```

### 2.7 `comment`

用途：评论表，支持新闻和视频评论，支持二级回复结构。

字段说明：

- `biz_type`：业务类型，当前取值为 `NEWS`、`VIDEO`
- `biz_id`：对应业务主键
- `parent_id`：父评论 ID
- `root_id`：根评论 ID
- `is_deleted`：逻辑删除标记

```sql
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
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='评论表';
```

### 2.8 `favorite`

用途：用户收藏新闻或动态。由于动态最终也进入 `news` 表，因此收藏只需要关联 `news_id`。

```sql
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
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='收藏表';
```

### 2.9 `post_submission`

用途：保存用户投稿原稿、审核状态、审核意见。该表是审核池，不直接作为首页公共内容来源。

说明：

- 投稿创建后状态为 `PENDING`
- 审核通过后状态为 `APPROVED`
- 审核驳回后状态为 `REJECTED`
- 推荐增加 `published_news_id`，用于关联审核通过后生成的 `news.id`

```sql
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
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='投稿审核表';
```

### 2.10 `audit_log`

用途：记录审核行为，例如投稿审核、内容审核。

```sql
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
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='审核日志表';
```

### 2.11 `operation_log`

用途：记录后台操作行为，如创建新闻、删除评论、同步新闻等。

```sql
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
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='操作日志表';
```

## 3. 视图名称与定义

说明：

- 当前项目代码没有直接依赖数据库视图
- 下面的视图是根据你当前方案补充的“数据库设计层推荐视图”
- 这些视图可以写进课程作业文档，用来表达业务分层更清晰

### 3.1 `v_headline_news`

用途：头条频道视图，只展示已发布且非“用户投稿”的新闻。

```sql
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
```

### 3.2 `v_dynamic_news`

用途：动态频道视图，只展示已通过审核并已发布到公共内容池的用户动态。

```sql
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
```

### 3.3 `v_submission_audit_detail`

用途：投稿审核明细视图，便于后台查看投稿人、审核人、审核结果和对应发布内容。

```sql
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
```

## 4. 状态字段约定

### 4.1 `news.status`

| 取值 | 含义 |
| --- | --- |
| `DRAFT` | 草稿 |
| `PUBLISHED` | 已发布 |
| `REJECTED` | 已驳回 |

### 4.2 `video.status`

| 取值 | 含义 |
| --- | --- |
| `DRAFT` | 草稿 |
| `PUBLISHED` | 已发布 |
| `REJECTED` | 已驳回 |

### 4.3 `post_submission.status`

| 取值 | 含义 |
| --- | --- |
| `PENDING` | 待审核 |
| `APPROVED` | 审核通过 |
| `REJECTED` | 审核驳回 |

### 4.4 `comment.biz_type`

| 取值 | 含义 |
| --- | --- |
| `NEWS` | 新闻/动态评论 |
| `VIDEO` | 视频评论 |

## 5. 关系说明

主要关系如下：

1. 一个用户可以拥有多个角色，`user` 与 `role` 通过 `user_role` 关联
2. 一个用户可以发布多条 `news` 或 `video`
3. 一个用户可以提交多条 `post_submission`
4. 一条 `post_submission` 审核通过后，可以生成一条 `news`
5. 一条 `news` 可以关联多条 `news_media`
6. 一条 `news` 可以被多个用户收藏，也可以拥有多条评论
7. 一条评论可以通过 `parent_id`、`root_id` 形成回复树

## 6. 结论

对于你当前这版作业，最合适的数据库落地方式是：

- 保持 `news` 作为统一公共内容表
- 保持 `post_submission` 作为投稿审核池
- 用 `category = '用户投稿'` 区分动态频道
- 用推荐视图表达“头条”和“动态”的业务边界

这样设计的优点是改动小、结构清楚、便于答辩，也和你当前代码实现方向一致。
