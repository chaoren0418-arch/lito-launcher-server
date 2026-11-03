-- 原生 LiTo 0.6.60：由 SQL 管理者執行。執行帳號僅授予收據表 SELECT/INSERT/UPDATE。
-- 380_lito 收據使用 InnoDB；來源 character_lito 保留原本 MyISAM/InnoDB。
CREATE TABLE IF NOT EXISTS `380_lito`.`lito_battle_settlement` (
  result_id CHAR(64) CHARACTER SET ascii COLLATE ascii_bin NOT NULL PRIMARY KEY,
  sequence_id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT UNIQUE,
  battle_id CHAR(36) CHARACTER SET ascii NOT NULL,
  source_database VARCHAR(64) NOT NULL,
  source_name VARCHAR(100) NOT NULL,
  char_obj_id BIGINT UNSIGNED NOT NULL,
  char_name VARCHAR(100) NOT NULL,
  won BOOLEAN NOT NULL,
  disconnected BOOLEAN NOT NULL,
  applied BOOLEAN NOT NULL DEFAULT 0,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  applied_at TIMESTAMP NULL,
  INDEX pending_character(source_database,char_obj_id,applied,sequence_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_bin;

-- 每個啟用來源各執行一次本段；修改資料庫名。已存在欄位時不重建。
SET @lito_source = '380_ac_1';
SET @lito_alter = IF(EXISTS(SELECT 1 FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA=@lito_source AND TABLE_NAME='character_lito' AND COLUMN_NAME='last_result_id'),
  'SELECT 1', CONCAT('ALTER TABLE `', REPLACE(@lito_source,'`','``'),
  '`.`character_lito` ADD COLUMN last_result_id CHAR(64) CHARACTER SET ascii COLLATE ascii_bin NOT NULL DEFAULT ', CHAR(39),CHAR(39)));
PREPARE lito_migration FROM @lito_alter;
EXECUTE lito_migration;
DEALLOCATE PREPARE lito_migration;

-- 現場已透過 SettlementSchema 對 380_ac_1、380_ac_2 完成上述遷移。
-- 未啟用的未來來源也需先增加 last_result_id，再啟用來源列。
-- 不得清除未完成收據；已完成收據也保留供舊事件去重。
