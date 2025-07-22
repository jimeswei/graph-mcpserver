/*
 Navicat Premium Data Transfer

 Source Server         : 192.168.3.78
 Source Server Type    : MySQL
 Source Server Version : 80404
 Source Host           : 192.168.3.78:3307
 Source Schema         : graph-agent

 Target Server Type    : MySQL
 Target Server Version : 80404
 File Encoding         : 65001

 Date: 21/07/2025 20:30:58
*/

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ----------------------------
-- Table structure for graph-cache
-- ----------------------------
DROP TABLE IF EXISTS `graph-cache`;
CREATE TABLE `graph-cache` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `session_id` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '会话ID',
  `thread_id` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '线程ID',
  `content` text COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '内容',
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_session_id` (`session_id`),
  KEY `idx_thread_id` (`thread_id`),
  KEY `idx_session_thread` (`session_id`,`thread_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='图数据缓存表';

SET FOREIGN_KEY_CHECKS = 1;
