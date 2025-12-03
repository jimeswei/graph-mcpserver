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

 Date: 25/07/2025 18:21:46
*/

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ----------------------------
-- Table structure for analysis_sessions
-- ----------------------------
DROP TABLE IF EXISTS `analysis_sessions`;
CREATE TABLE `analysis_sessions` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `session_id` varchar(100) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '会话ID',
  `success` tinyint(1) NOT NULL COMMENT '是否成功',
  `message` text COLLATE utf8mb4_unicode_ci COMMENT '消息内容',
  `results_count` int DEFAULT '0' COMMENT '结果数量',
  `tool_calls_count` int DEFAULT '0' COMMENT '工具调用数量',
  `timestamp` bigint DEFAULT NULL COMMENT '时间戳',
  `created_time` timestamp NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_time` timestamp NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_session_id` (`session_id`),
  KEY `idx_timestamp` (`timestamp`)
) ENGINE=InnoDB AUTO_INCREMENT=29 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='分析会话表';

-- ----------------------------
-- Table structure for graph_cache
-- ----------------------------
DROP TABLE IF EXISTS `graph_cache`;
CREATE TABLE `graph_cache` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `thread_id` varchar(100) COLLATE utf8mb4_unicode_ci NOT NULL,
  `content` text COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '内容',
  `created_time` datetime(6) DEFAULT NULL,
  `updated_time` datetime(6) DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_thread_id` (`thread_id`),
  KEY `idx_session_thread` (`thread_id`)
) ENGINE=InnoDB AUTO_INCREMENT=221 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='图数据缓存表';

-- ----------------------------
-- Table structure for mcp_tool_results
-- ----------------------------
DROP TABLE IF EXISTS `mcp_tool_results`;
CREATE TABLE `mcp_tool_results` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `thread_id` varchar(100) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '线程ID',
  `session_id` varchar(100) COLLATE utf8mb4_unicode_ci NOT NULL,
  `agent` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '代理名称',
  `result_id` varchar(100) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '结果ID',
  `role` varchar(20) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '角色',
  `content` longtext COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '内容',
  `tool_call_id` varchar(100) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '工具调用ID',
  `created_time` timestamp NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_time` timestamp NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_thread_id` (`thread_id`),
  KEY `idx_agent` (`agent`),
  KEY `idx_tool_call_id` (`tool_call_id`),
  KEY `idx_result_id` (`result_id`)
) ENGINE=InnoDB AUTO_INCREMENT=371 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='MCP工具调用结果表';

-- ----------------------------
-- Table structure for tool_call_names
-- ----------------------------
DROP TABLE IF EXISTS `tool_call_names`;
CREATE TABLE `tool_call_names` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `name` varchar(100) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '工具名称',
  `call_id` varchar(100) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '调用ID',
  `type` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '调用类型',
  `args` text COLLATE utf8mb4_unicode_ci COMMENT '参数信息（JSON格式）',
  `call_index` int DEFAULT NULL COMMENT '调用索引',
  `created_time` timestamp NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_time` timestamp NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `session_id` varchar(100) COLLATE utf8mb4_unicode_ci NOT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_name` (`name`),
  KEY `idx_call_id` (`call_id`),
  KEY `idx_type` (`type`)
) ENGINE=InnoDB AUTO_INCREMENT=401 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='工具调用名称表';

-- ----------------------------
-- Table structure for users
-- ----------------------------
DROP TABLE IF EXISTS `users`;
CREATE TABLE `users` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `enabled` bit(1) NOT NULL,
  `password` varchar(255) COLLATE utf8mb4_general_ci NOT NULL,
  `role` varchar(255) COLLATE utf8mb4_general_ci DEFAULT NULL,
  `username` varchar(255) COLLATE utf8mb4_general_ci NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UKr43af9ap4edm43mmtq01oddj6` (`username`)
) ENGINE=InnoDB AUTO_INCREMENT=2 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

SET FOREIGN_KEY_CHECKS = 1;
