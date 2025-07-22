-- 添加 query_type 列到 graph-cache 表
ALTER TABLE `graph-cache` ADD COLUMN `query_type` VARCHAR(50) NOT NULL DEFAULT 'unknown' AFTER `content`;

-- 更新现有数据的 query_type（如果有的话）
UPDATE `graph-cache` SET `query_type` = 'unknown' WHERE `query_type` = '' OR `query_type` IS NULL;

-- 创建索引以优化查询性能
CREATE INDEX idx_graph_cache_query_type ON `graph-cache`(`query_type`);
CREATE INDEX idx_graph_cache_session_query_type ON `graph-cache`(`session_id`, `query_type`);
CREATE INDEX idx_graph_cache_created_at ON `graph-cache`(`created_at`);