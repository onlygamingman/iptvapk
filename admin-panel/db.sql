-- KhelaGhor Live Sports System - MySQL Database Schema
-- File: db.sql

SET SQL_MODE = "NO_AUTO_VALUE_ON_ZERO";
START TRANSACTION;
SET time_zone = "+00:00";

-- 1. Matches Table
CREATE TABLE IF NOT EXISTS `matches` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `category` varchar(50) NOT NULL,
  `team1_name` varchar(100) NOT NULL,
  `team1_logo_url` text NOT NULL,
  `team2_name` varchar(100) NOT NULL,
  `team2_logo_url` text NOT NULL,
  `stream_url` text NOT NULL,
  `tournament` varchar(150) NOT NULL,
  `status` enum('LIVE','UPCOMING') NOT NULL DEFAULT 'LIVE',
  `start_timestamp` bigint(20) DEFAULT 0,
  `added_at` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_status` (`status`),
  KEY `idx_category` (`category`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 2. TV / Sports Channels Table
CREATE TABLE IF NOT EXISTS `channels` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `category_name` varchar(100) NOT NULL,
  `channel_name` varchar(150) NOT NULL,
  `channel_logo_url` text DEFAULT NULL,
  `stream_url` text NOT NULL,
  `added_at` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_category_name` (`category_name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 3. System Config and Ads settings 
CREATE TABLE IF NOT EXISTS `app_config` (
  `id` int(11) NOT NULL DEFAULT '1',
  `ads_enabled` tinyint(1) NOT NULL DEFAULT '1',
  `banner_ad_url` text DEFAULT NULL,
  `pop_under_url` text DEFAULT NULL,
  `banner_ad_code` text DEFAULT NULL,
  `pop_under_code` text DEFAULT NULL,
  `show_notice` tinyint(1) NOT NULL DEFAULT '1',
  `notice_title` varchar(255) DEFAULT 'খেলাঘর নোটিশ বোর্ড',
  `notice_message` text DEFAULT NULL,
  `notice_button_text` varchar(100) DEFAULT 'টেলিগ্রামে জয়েন করুন',
  `notice_link` text DEFAULT NULL,
  `admin_password_hash` varchar(255) NOT NULL,
  `created_at` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Seed Default Config with Admin Hash (Bcrypt for admin123)
INSERT INTO `app_config` (`id`, `ads_enabled`, `banner_ad_url`, `pop_under_url`, `admin_password_hash`) 
VALUES (1, 1, 'https://gotechbd.com', 'https://gotechbd.com', '$2y$10$A68HInqO8eYfU1L7C26sD.pE3K/uI2m/scM0H.y9D8AmsP7gZ638G')
ON DUPLICATE KEY UPDATE `id`=`id`;

COMMIT;
