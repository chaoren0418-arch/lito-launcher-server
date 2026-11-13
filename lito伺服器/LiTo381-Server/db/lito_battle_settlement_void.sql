-- T136（#387）：結算收據「作廢」狀態的稽核欄位。由 SQL 管理者執行（需要 ALTER 權限）。
-- 前提：已執行 db/lito_battle_settlement.sql（`380_lito`.`lito_battle_settlement` 已存在）。
-- 權限：LiTo 執行帳號不需要新權限；既有表級
--   GRANT SELECT, INSERT, UPDATE ON `380_lito`.`lito_battle_settlement`
-- 已涵蓋新欄位。**不新增任何對來源庫（character_lito、characters）的寫入權限。**
--
-- applied 欄位的值（BOOLEAN＝TINYINT(1)，可存 0～127）：
--   0 待寫入；1 已寫進 character_lito；2 已作廢（營運確認角色改名／刪除後作廢，永不計入）。
-- 作廢只在伺服器啟動時、依 runtime/battle-settlements/void-requests.txt 執行（docs/INSTALL.md §6、docs/design/gameplay.md §4.2）。
--
-- 引擎：本表必須是 InnoDB（SettlementSql.check）；ADD COLUMN … NULL DEFAULT NULL 不改既有欄位、不刪資料。
-- 冪等：每個欄位先查 information_schema，已存在就跳過，可重複執行。
-- 只在需要作廢時才必須先跑本檔：伺服器只在 void-requests.txt 存在時檢查這兩個欄位，缺欄位就拒絕啟動。

SET @lito_controls = '380_lito';

SET @lito_alter = IF(EXISTS(SELECT 1 FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA=@lito_controls AND TABLE_NAME='lito_battle_settlement' AND COLUMN_NAME='voided_at'),
  'SELECT 1', CONCAT('ALTER TABLE `', REPLACE(@lito_controls,'`','``'),
  '`.`lito_battle_settlement` ADD COLUMN voided_at TIMESTAMP NULL DEFAULT NULL'));
PREPARE lito_migration FROM @lito_alter; EXECUTE lito_migration; DEALLOCATE PREPARE lito_migration;

SET @lito_alter = IF(EXISTS(SELECT 1 FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA=@lito_controls AND TABLE_NAME='lito_battle_settlement' AND COLUMN_NAME='void_reason'),
  'SELECT 1', CONCAT('ALTER TABLE `', REPLACE(@lito_controls,'`','``'),
  '`.`lito_battle_settlement` ADD COLUMN void_reason VARCHAR(255) NULL DEFAULT NULL'));
PREPARE lito_migration FROM @lito_alter; EXECUTE lito_migration; DEALLOCATE PREPARE lito_migration;

-- 執行後核對：
--   SELECT COLUMN_NAME, COLUMN_TYPE, IS_NULLABLE FROM information_schema.COLUMNS
--   WHERE TABLE_SCHEMA=@lito_controls AND TABLE_NAME='lito_battle_settlement' ORDER BY ORDINAL_POSITION;
-- 查已作廢的結果：
--   SELECT result_id, source_database, char_obj_id, char_name, won, disconnected, voided_at, void_reason
--   FROM `380_lito`.`lito_battle_settlement` WHERE applied=2;
-- 不得刪除收據列；作廢列保留供稽核與舊事件去重。
