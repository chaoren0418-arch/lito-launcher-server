-- T103b（#335，#310 D3）：積分、斷線數、月計寫進各來源庫 character_lito，重啟不再歸零。
-- 由 SQL 管理者執行（需要 ALTER 權限）；LiTo 執行帳號不需要新權限：既有表級
-- GRANT SELECT, INSERT, UPDATE ON `<來源庫>`.`character_lito` 已涵蓋新欄位。
-- 前提：已執行 db/lito_battle_settlement.sql（character_lito 已有 last_result_id）。
-- 升級順序：先對「每個 enabled=1 來源」各執行一次本段，再換新版伺服器；
--           新版伺服器啟動時檢查欄位，缺欄位會直接拒絕啟動（SharedDatabase.check、SettlementSql.check）。
--
-- 引擎：MyISAM 與 InnoDB 都適用（docs/design/database.md §7）。
--   - 只用 ADD COLUMN … NOT NULL DEFAULT 0，不改引擎、不改既有欄位、不刪資料；既有列的新欄位為 0。
--   - MyISAM／MySQL 5.5 的 ALTER 會複製整張表並持有表鎖；character_lito 每角色一列，表很小，
--     但仍請在伺服器停機時執行。
--   - 冪等：每個欄位先查 information_schema，已存在就跳過，可重複執行。
--
-- 欄位語意（與 GameStateStore.TournamentProfileState 一一對應）：
--   ranking_points          累計積分（勝 +battle.reward.win、敗 +battle.reward.loss、斷線 −battle.reward.loss，最低 0）
--   disconnects             斷線敗場數（含在 losses 內）
--   monthly_period          月計所屬月份 YYYYMM（以台北時間每日 05:00 為日界），0＝尚無月計
--   monthly_ranking_points  該月積分（同上規則，最低 0）
--   monthly_wins／monthly_losses  該月勝／敗場數
--   跨月時下一場結算先把三個月計歸零、monthly_period 換成新月份，再加上本場。

SET @lito_source = '380_ac_1';

SET @lito_alter = IF(EXISTS(SELECT 1 FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA=@lito_source AND TABLE_NAME='character_lito' AND COLUMN_NAME='ranking_points'),
  'SELECT 1', CONCAT('ALTER TABLE `', REPLACE(@lito_source,'`','``'),
  '`.`character_lito` ADD COLUMN ranking_points BIGINT(20) UNSIGNED NOT NULL DEFAULT 0'));
PREPARE lito_migration FROM @lito_alter; EXECUTE lito_migration; DEALLOCATE PREPARE lito_migration;

SET @lito_alter = IF(EXISTS(SELECT 1 FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA=@lito_source AND TABLE_NAME='character_lito' AND COLUMN_NAME='disconnects'),
  'SELECT 1', CONCAT('ALTER TABLE `', REPLACE(@lito_source,'`','``'),
  '`.`character_lito` ADD COLUMN disconnects BIGINT(20) UNSIGNED NOT NULL DEFAULT 0'));
PREPARE lito_migration FROM @lito_alter; EXECUTE lito_migration; DEALLOCATE PREPARE lito_migration;

SET @lito_alter = IF(EXISTS(SELECT 1 FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA=@lito_source AND TABLE_NAME='character_lito' AND COLUMN_NAME='monthly_period'),
  'SELECT 1', CONCAT('ALTER TABLE `', REPLACE(@lito_source,'`','``'),
  '`.`character_lito` ADD COLUMN monthly_period INT(10) UNSIGNED NOT NULL DEFAULT 0'));
PREPARE lito_migration FROM @lito_alter; EXECUTE lito_migration; DEALLOCATE PREPARE lito_migration;

SET @lito_alter = IF(EXISTS(SELECT 1 FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA=@lito_source AND TABLE_NAME='character_lito' AND COLUMN_NAME='monthly_ranking_points'),
  'SELECT 1', CONCAT('ALTER TABLE `', REPLACE(@lito_source,'`','``'),
  '`.`character_lito` ADD COLUMN monthly_ranking_points BIGINT(20) UNSIGNED NOT NULL DEFAULT 0'));
PREPARE lito_migration FROM @lito_alter; EXECUTE lito_migration; DEALLOCATE PREPARE lito_migration;

SET @lito_alter = IF(EXISTS(SELECT 1 FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA=@lito_source AND TABLE_NAME='character_lito' AND COLUMN_NAME='monthly_wins'),
  'SELECT 1', CONCAT('ALTER TABLE `', REPLACE(@lito_source,'`','``'),
  '`.`character_lito` ADD COLUMN monthly_wins BIGINT(20) UNSIGNED NOT NULL DEFAULT 0'));
PREPARE lito_migration FROM @lito_alter; EXECUTE lito_migration; DEALLOCATE PREPARE lito_migration;

SET @lito_alter = IF(EXISTS(SELECT 1 FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA=@lito_source AND TABLE_NAME='character_lito' AND COLUMN_NAME='monthly_losses'),
  'SELECT 1', CONCAT('ALTER TABLE `', REPLACE(@lito_source,'`','``'),
  '`.`character_lito` ADD COLUMN monthly_losses BIGINT(20) UNSIGNED NOT NULL DEFAULT 0'));
PREPARE lito_migration FROM @lito_alter; EXECUTE lito_migration; DEALLOCATE PREPARE lito_migration;

-- 執行後核對（每個來源）：
--   SELECT COLUMN_NAME, COLUMN_TYPE, COLUMN_DEFAULT FROM information_schema.COLUMNS
--   WHERE TABLE_SCHEMA=@lito_source AND TABLE_NAME='character_lito' ORDER BY ORDINAL_POSITION;
-- 既有 wins／losses 不回填積分：升級前積分只存在記憶體、每次重啟歸零，沒有可回填的可靠來源。
