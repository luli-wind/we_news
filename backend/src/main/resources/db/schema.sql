SET @db = DATABASE();

-- source_name
SET @sql = (
  SELECT IF(COUNT(*) = 0,
    'ALTER TABLE news ADD COLUMN source_name VARCHAR(128) AFTER cover_url',
    'SELECT "source_name exists"')
  FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = @db AND TABLE_NAME = 'news' AND COLUMN_NAME = 'source_name'
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- source_url
SET @sql = (
  SELECT IF(COUNT(*) = 0,
    'ALTER TABLE news ADD COLUMN source_url VARCHAR(500) AFTER source_name',
    'SELECT "source_url exists"')
  FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = @db AND TABLE_NAME = 'news' AND COLUMN_NAME = 'source_url'
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- origin_hash
SET @sql = (
  SELECT IF(COUNT(*) = 0,
    'ALTER TABLE news ADD COLUMN origin_hash CHAR(64) AFTER source_url',
    'SELECT "origin_hash exists"')
  FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = @db AND TABLE_NAME = 'news' AND COLUMN_NAME = 'origin_hash'
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- unique index
SET @sql = (
  SELECT IF(COUNT(*) = 0,
    'CREATE UNIQUE INDEX uk_news_origin_hash ON news(origin_hash)',
    'SELECT "uk_news_origin_hash exists"')
  FROM information_schema.STATISTICS
  WHERE TABLE_SCHEMA = @db AND TABLE_NAME = 'news' AND INDEX_NAME = 'uk_news_origin_hash'
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
