-- M4 實驗庫準備（T128／Issue #379）：補 character_lito 欄位、確認收據表、來源路由指向實驗角色庫、授權實驗帳號。
-- **只作用在名稱以 _lito_exp 結尾的實驗 schema**；埠不是 43324 或 schema 名稱不符就中止，之後每一句也各自檢查 @m4_guard。
-- 執行者：Codex，持有 p24lab lease、完成冷備份後，以 P24Lab 管理身分執行（本檔在 #379 沒有對 P24Lab 執行過，只在 CI 的拋棄式 MySQL 8.0 跑過）：
--   cmd /c "mysql --defaults-extra-file=<私密 cnf> --protocol=TCP --host=127.0.0.1 --port=43324 --default-character-set=utf8mb4 --table < tools\acceptance\m4-db-prepare.sql > <run>\evidence\db-prepare.txt"
-- 不要加 --force（守門失敗的錯誤訊息是 Unknown database 'm4_guard__<原因>'，退出碼非 0）。可重複執行（每一步先查 information_schema，已完成就略過）。
-- 前置：p24_380db_lito_exp 已由 380_db.sql 快照匯入（命令見 tools/acceptance/README.md）；本檔不匯入快照、不建立 schema、不建立帳號。
-- 不做：不刪資料、不改 P24Lab 原有的庫（p24_world／p24_accounts／p24_lito）、不碰 3306／2000。
-- GRANT 清單（GENERATED 區塊）由 tools/acceptance/m4-schema-precheck.py --update-sql 依 tools/db-grants 產生。
SET NAMES utf8mb4;

-- ===== 參數（要換庫名時改這裡，或用 --init-command 預先 SET；已設定的值不會被覆蓋）=====
SET @m4_static        = IFNULL(@m4_static, 'p24_380db_lito_exp');      -- #284 02:43 裁定：由 380_db.sql 快照匯入
SET @m4_lito          = IFNULL(@m4_lito,   'p24_lito_lito_exp');       -- 改用 380_lito.sql 快照時是 'p24_380lito_lito_exp'
SET @m4_ac            = IFNULL(@m4_ac,     'p24_accounts_lito_exp');   -- #83 的實驗角色庫
SET @m4_grantee       = IFNULL(@m4_grantee, '''lito83_exp''@''127.0.0.1''');  -- #83 建立的實驗帳號（帳號名稱是公開資訊，密碼不在 repo）
SET @m4_grant_mode    = IFNULL(@m4_grant_mode, 'table');               -- 'table'＝靜態庫逐表 SELECT（同正式部署清單）；'schema'＝靜態庫整庫 SELECT（#284 02:43 裁定第 2 點）
SET @m4_expected_uuid = IFNULL(@m4_expected_uuid, 'e3443e40-d090-11f1-8040-4cedfb679789');  -- 設成 '' 表示不檢查

-- ===== 守門 =====
SET @m4_guard = 'FAIL';
SELECT 'P0' AS step, @@port AS port, @@server_uuid AS server_uuid, @@bind_address AS bind_address,
       @@datadir AS datadir, VERSION() AS version, CURRENT_USER() AS connected_as;
SET @m4_sql = IF(@@port = 43324, 'DO 0', 'SELECT * FROM `m4_guard__port_is_not_43324_not_p24lab`.`guard`');
PREPARE m4_stmt FROM @m4_sql; EXECUTE m4_stmt; DEALLOCATE PREPARE m4_stmt;
SET @m4_sql = IF(@m4_static REGEXP '^[A-Za-z0-9_]+_lito_exp$' AND @m4_lito REGEXP '^[A-Za-z0-9_]+_lito_exp$'
                 AND @m4_ac REGEXP '^[A-Za-z0-9_]+_lito_exp$' AND @m4_static <> @m4_lito AND @m4_static <> @m4_ac AND @m4_lito <> @m4_ac,
                 'DO 0', 'SELECT * FROM `m4_guard__schemas_must_be_distinct_and_end_with_lito_exp`.`guard`');
PREPARE m4_stmt FROM @m4_sql; EXECUTE m4_stmt; DEALLOCATE PREPARE m4_stmt;
SET @m4_sql = IF(@m4_expected_uuid = '' OR @@server_uuid = @m4_expected_uuid,
                 'DO 0', 'SELECT * FROM `m4_guard__server_uuid_is_not_the_recorded_p24lab`.`guard`');
PREPARE m4_stmt FROM @m4_sql; EXECUTE m4_stmt; DEALLOCATE PREPARE m4_stmt;
SET @m4_sql = IF(@m4_grant_mode IN ('table', 'schema') AND @m4_grantee REGEXP '^''[A-Za-z0-9_]+''@''(127\\.0\\.0\\.1|localhost)''$',
                 'DO 0', 'SELECT * FROM `m4_guard__grantee_must_be_loopback_and_mode_table_or_schema`.`guard`');
PREPARE m4_stmt FROM @m4_sql; EXECUTE m4_stmt; DEALLOCATE PREPARE m4_stmt;
SET @m4_schemas = (SELECT COUNT(*) FROM information_schema.SCHEMATA
                   WHERE SCHEMA_NAME = CONVERT(@m4_static USING utf8)
                      OR SCHEMA_NAME = CONVERT(@m4_lito USING utf8)
                      OR SCHEMA_NAME = CONVERT(@m4_ac USING utf8));
SET @m4_sql = IF(@m4_schemas = 3, 'DO 0', 'SELECT * FROM `m4_guard__an_experiment_schema_is_missing_import_snapshot`.`guard`');
PREPARE m4_stmt FROM @m4_sql; EXECUTE m4_stmt; DEALLOCATE PREPARE m4_stmt;
SET @m4_guard = 'PASS';
SET @m4_skipped = '';

-- ===== P1：來源角色庫的 character_lito =====
-- P1a：表不存在才建立。定義同 380_ac.sql 快照（MyISAM、utf8），再由 P1b／P1c 補欄位。#83 已建立此表時本步略過。
SET @m4_sql = IF(@m4_guard = 'PASS', CONCAT('CREATE TABLE IF NOT EXISTS `', @m4_ac, '`.`character_lito` (',
  '`char_obj_id` int(10) UNSIGNED NOT NULL, ',
  '`char_name` varchar(45) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL DEFAULT '''', ',
  '`wins` bigint(20) UNSIGNED NOT NULL DEFAULT 0, ',
  '`losses` bigint(20) UNSIGNED NOT NULL DEFAULT 0, ',
  '`version` bigint(20) UNSIGNED NOT NULL DEFAULT 0, ',
  '`updated_at` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP, ',
  'PRIMARY KEY (`char_obj_id`) USING BTREE) ENGINE = MyISAM CHARACTER SET = utf8 COLLATE = utf8_general_ci'), 'DO 0');
PREPARE m4_stmt FROM @m4_sql; EXECUTE m4_stmt; DEALLOCATE PREPARE m4_stmt;

-- P1b：last_result_id（寫法同 db/lito_battle_settlement.sql:20-27）
SET @lito_source = @m4_ac;
SET @lito_alter = IF(@m4_guard <> 'PASS' OR EXISTS(SELECT 1 FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = CONVERT(@lito_source USING utf8) AND TABLE_NAME='character_lito' AND COLUMN_NAME='last_result_id'),
  'DO 0', CONCAT('ALTER TABLE `', REPLACE(@lito_source,'`','``'),
  '`.`character_lito` ADD COLUMN last_result_id CHAR(64) CHARACTER SET ascii COLLATE ascii_bin NOT NULL DEFAULT ', CHAR(39),CHAR(39)));
PREPARE lito_migration FROM @lito_alter; EXECUTE lito_migration; DEALLOCATE PREPARE lito_migration;

-- P1c：T103b 積分、斷線數、月計（寫法同 db/lito_character_ranking.sql:22-58；每欄先查 information_schema，已存在就略過）
SET @lito_alter = IF(@m4_guard <> 'PASS' OR EXISTS(SELECT 1 FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = CONVERT(@lito_source USING utf8) AND TABLE_NAME='character_lito' AND COLUMN_NAME='ranking_points'),
  'DO 0', CONCAT('ALTER TABLE `', REPLACE(@lito_source,'`','``'),
  '`.`character_lito` ADD COLUMN ranking_points BIGINT(20) UNSIGNED NOT NULL DEFAULT 0'));
PREPARE lito_migration FROM @lito_alter; EXECUTE lito_migration; DEALLOCATE PREPARE lito_migration;

SET @lito_alter = IF(@m4_guard <> 'PASS' OR EXISTS(SELECT 1 FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = CONVERT(@lito_source USING utf8) AND TABLE_NAME='character_lito' AND COLUMN_NAME='disconnects'),
  'DO 0', CONCAT('ALTER TABLE `', REPLACE(@lito_source,'`','``'),
  '`.`character_lito` ADD COLUMN disconnects BIGINT(20) UNSIGNED NOT NULL DEFAULT 0'));
PREPARE lito_migration FROM @lito_alter; EXECUTE lito_migration; DEALLOCATE PREPARE lito_migration;

SET @lito_alter = IF(@m4_guard <> 'PASS' OR EXISTS(SELECT 1 FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = CONVERT(@lito_source USING utf8) AND TABLE_NAME='character_lito' AND COLUMN_NAME='monthly_period'),
  'DO 0', CONCAT('ALTER TABLE `', REPLACE(@lito_source,'`','``'),
  '`.`character_lito` ADD COLUMN monthly_period INT(10) UNSIGNED NOT NULL DEFAULT 0'));
PREPARE lito_migration FROM @lito_alter; EXECUTE lito_migration; DEALLOCATE PREPARE lito_migration;

SET @lito_alter = IF(@m4_guard <> 'PASS' OR EXISTS(SELECT 1 FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = CONVERT(@lito_source USING utf8) AND TABLE_NAME='character_lito' AND COLUMN_NAME='monthly_ranking_points'),
  'DO 0', CONCAT('ALTER TABLE `', REPLACE(@lito_source,'`','``'),
  '`.`character_lito` ADD COLUMN monthly_ranking_points BIGINT(20) UNSIGNED NOT NULL DEFAULT 0'));
PREPARE lito_migration FROM @lito_alter; EXECUTE lito_migration; DEALLOCATE PREPARE lito_migration;

SET @lito_alter = IF(@m4_guard <> 'PASS' OR EXISTS(SELECT 1 FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = CONVERT(@lito_source USING utf8) AND TABLE_NAME='character_lito' AND COLUMN_NAME='monthly_wins'),
  'DO 0', CONCAT('ALTER TABLE `', REPLACE(@lito_source,'`','``'),
  '`.`character_lito` ADD COLUMN monthly_wins BIGINT(20) UNSIGNED NOT NULL DEFAULT 0'));
PREPARE lito_migration FROM @lito_alter; EXECUTE lito_migration; DEALLOCATE PREPARE lito_migration;

SET @lito_alter = IF(@m4_guard <> 'PASS' OR EXISTS(SELECT 1 FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = CONVERT(@lito_source USING utf8) AND TABLE_NAME='character_lito' AND COLUMN_NAME='monthly_losses'),
  'DO 0', CONCAT('ALTER TABLE `', REPLACE(@lito_source,'`','``'),
  '`.`character_lito` ADD COLUMN monthly_losses BIGINT(20) UNSIGNED NOT NULL DEFAULT 0'));
PREPARE lito_migration FROM @lito_alter; EXECUTE lito_migration; DEALLOCATE PREPARE lito_migration;

SELECT 'P1' AS step, COLUMN_NAME, COLUMN_TYPE, COLUMN_DEFAULT FROM information_schema.COLUMNS
WHERE TABLE_SCHEMA = CONVERT(@m4_ac USING utf8) AND TABLE_NAME = 'character_lito' ORDER BY ORDINAL_POSITION;

-- ===== P2：控制庫的結算收據表（定義同 db/lito_battle_settlement.sql:3-17；已存在就不動，不清既有收據）=====
SET @m4_sql = IF(@m4_guard = 'PASS', CONCAT('CREATE TABLE IF NOT EXISTS `', @m4_lito, '`.`lito_battle_settlement` (',
  'result_id CHAR(64) CHARACTER SET ascii COLLATE ascii_bin NOT NULL PRIMARY KEY, ',
  'sequence_id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT UNIQUE, ',
  'battle_id CHAR(36) CHARACTER SET ascii NOT NULL, ',
  'source_database VARCHAR(64) NOT NULL, ',
  'source_name VARCHAR(100) NOT NULL, ',
  'char_obj_id BIGINT UNSIGNED NOT NULL, ',
  'char_name VARCHAR(100) NOT NULL, ',
  'won BOOLEAN NOT NULL, ',
  'disconnected BOOLEAN NOT NULL, ',
  'applied BOOLEAN NOT NULL DEFAULT 0, ',
  'created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP, ',
  'applied_at TIMESTAMP NULL, ',
  'INDEX pending_character(source_database,char_obj_id,applied,sequence_id)',
  ') ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_bin'), 'DO 0');
PREPARE m4_stmt FROM @m4_sql; EXECUTE m4_stmt; DEALLOCATE PREPARE m4_stmt;

-- ===== P3：來源路由只留一條啟用，指向實驗角色庫（#284 02:43 裁定第 4 點；p24_lito_lito_exp 已是這個狀態時三句都改 0 列）=====
SET @m4_has = (SELECT COUNT(*) FROM information_schema.TABLES
               WHERE TABLE_SCHEMA = CONVERT(@m4_lito USING utf8) AND TABLE_NAME = 'lito_source_server');
SET @m4_route_ok = (@m4_guard = 'PASS' AND @m4_has = 1);
-- P3a：其他來源一律停用（不刪列，保留 source_id 與顯示名稱）
SET @m4_sql = IF(@m4_route_ok, CONCAT('UPDATE `', @m4_lito, '`.`lito_source_server` SET enabled = 0 ',
  'WHERE enabled <> 0 AND (character_database IS NULL OR character_database <> CONVERT(@m4_ac USING utf8mb4) COLLATE utf8mb4_general_ci)'), 'DO 0');
PREPARE m4_stmt FROM @m4_sql; EXECUTE m4_stmt; DEALLOCATE PREPARE m4_stmt;
-- P3b：沒有任何一列指向實驗角色庫時，把 source_id 最小的一列改成指向它（沿用該列的顯示名稱）
SET @m4_route_n = 0; SET @m4_route_min = NULL;
SET @m4_sql = IF(@m4_route_ok, CONCAT('SELECT IFNULL(SUM(character_database = CONVERT(@m4_ac USING utf8mb4) COLLATE utf8mb4_general_ci), 0), MIN(source_id) INTO @m4_route_n, @m4_route_min FROM `',
  @m4_lito, '`.`lito_source_server`'), 'DO 0');
PREPARE m4_stmt FROM @m4_sql; EXECUTE m4_stmt; DEALLOCATE PREPARE m4_stmt;
SET @m4_sql = IF(@m4_route_ok AND @m4_route_n = 0 AND @m4_route_min IS NOT NULL, CONCAT('UPDATE `', @m4_lito,
  '`.`lito_source_server` SET character_database = @m4_ac WHERE source_id = @m4_route_min'), 'DO 0');
PREPARE m4_stmt FROM @m4_sql; EXECUTE m4_stmt; DEALLOCATE PREPARE m4_stmt;
-- P3c：表是空的才新增一列
SET @m4_sql = IF(@m4_route_ok AND @m4_route_min IS NULL, CONCAT('INSERT INTO `', @m4_lito,
  '`.`lito_source_server` (source_id, display_name, character_database, enabled) VALUES (1, ''M4-EXP'', @m4_ac, 1)'), 'DO 0');
PREPARE m4_stmt FROM @m4_sql; EXECUTE m4_stmt; DEALLOCATE PREPARE m4_stmt;
-- P3d：指向實驗角色庫的那一列設為啟用
SET @m4_sql = IF(@m4_route_ok, CONCAT('UPDATE `', @m4_lito, '`.`lito_source_server` SET enabled = 1 ',
  'WHERE character_database = CONVERT(@m4_ac USING utf8mb4) COLLATE utf8mb4_general_ci AND enabled <> 1'), 'DO 0');
PREPARE m4_stmt FROM @m4_sql; EXECUTE m4_stmt; DEALLOCATE PREPARE m4_stmt;
SET @m4_sql = IF(@m4_route_ok, CONCAT('SELECT ''P3'' AS step, source_id, display_name, character_database, enabled FROM `',
  @m4_lito, '`.`lito_source_server` ORDER BY source_id'), 'SELECT ''P3'' AS step, ''lito_source_server 不存在，未處理'' AS note');
PREPARE m4_stmt FROM @m4_sql; EXECUTE m4_stmt; DEALLOCATE PREPARE m4_stmt;

-- ===== P4：授權實驗帳號（只授權、不建立帳號、不撤權）=====
-- 表不存在時表級 GRANT 會失敗，所以每張表先查 information_schema；略過的表記在 @m4_skipped，最後一併列出。
-- P4a：@m4_grant_mode='schema' 時，靜態庫改授整庫 SELECT（控制庫、角色庫仍逐表）
SET @m4_sql = IF(@m4_guard = 'PASS' AND @m4_grant_mode = 'schema', CONCAT('GRANT SELECT ON `', @m4_static, '`.* TO ', @m4_grantee), 'DO 0');
PREPARE m4_stmt FROM @m4_sql; EXECUTE m4_stmt; DEALLOCATE PREPARE m4_stmt;
-- P4b：逐表
-- BEGIN GENERATED:grants
-- 由 tools/acceptance/m4-schema-precheck.py --update-sql 產生；請勿手改。來源：tools/db-grants（review.json 套用後）。
SET @m4_t = 'armor'; SET @m4_p = 'SELECT'; SET @m4_s = @m4_static;
SET @m4_exists = (SELECT COUNT(*) FROM information_schema.TABLES WHERE TABLE_SCHEMA = CONVERT(@m4_s USING utf8) AND TABLE_NAME = CONVERT(@m4_t USING utf8));
SET @m4_do = (@m4_guard='PASS' AND @m4_exists=1 AND (@m4_p<>'SELECT' OR @m4_grant_mode='table' OR @m4_s<>@m4_static));
SET @m4_sql = IF(@m4_do, CONCAT('GRANT ', @m4_p, ' ON `', @m4_s, '`.`', REPLACE(@m4_t,'`','``'), '` TO ', @m4_grantee), 'DO 0');
PREPARE m4_stmt FROM @m4_sql; EXECUTE m4_stmt; DEALLOCATE PREPARE m4_stmt;
SET @m4_skipped = CONCAT(@m4_skipped, IF(@m4_exists=1, '', CONCAT(@m4_s, '.', @m4_t, '', '; ')));
SET @m4_t = 'etcitem'; SET @m4_p = 'SELECT'; SET @m4_s = @m4_static;
SET @m4_exists = (SELECT COUNT(*) FROM information_schema.TABLES WHERE TABLE_SCHEMA = CONVERT(@m4_s USING utf8) AND TABLE_NAME = CONVERT(@m4_t USING utf8));
SET @m4_do = (@m4_guard='PASS' AND @m4_exists=1 AND (@m4_p<>'SELECT' OR @m4_grant_mode='table' OR @m4_s<>@m4_static));
SET @m4_sql = IF(@m4_do, CONCAT('GRANT ', @m4_p, ' ON `', @m4_s, '`.`', REPLACE(@m4_t,'`','``'), '` TO ', @m4_grantee), 'DO 0');
PREPARE m4_stmt FROM @m4_sql; EXECUTE m4_stmt; DEALLOCATE PREPARE m4_stmt;
SET @m4_skipped = CONCAT(@m4_skipped, IF(@m4_exists=1, '', CONCAT(@m4_s, '.', @m4_t, '', '; ')));
SET @m4_t = 'etcitem_arrow'; SET @m4_p = 'SELECT'; SET @m4_s = @m4_static;
SET @m4_exists = (SELECT COUNT(*) FROM information_schema.TABLES WHERE TABLE_SCHEMA = CONVERT(@m4_s USING utf8) AND TABLE_NAME = CONVERT(@m4_t USING utf8));
SET @m4_do = (@m4_guard='PASS' AND @m4_exists=1 AND (@m4_p<>'SELECT' OR @m4_grant_mode='table' OR @m4_s<>@m4_static));
SET @m4_sql = IF(@m4_do, CONCAT('GRANT ', @m4_p, ' ON `', @m4_s, '`.`', REPLACE(@m4_t,'`','``'), '` TO ', @m4_grantee), 'DO 0');
PREPARE m4_stmt FROM @m4_sql; EXECUTE m4_stmt; DEALLOCATE PREPARE m4_stmt;
SET @m4_skipped = CONCAT(@m4_skipped, IF(@m4_exists=1, '', CONCAT(@m4_s, '.', @m4_t, '', '; ')));
SET @m4_t = 'etcitem_buff藥水能力'; SET @m4_p = 'SELECT'; SET @m4_s = @m4_static;
SET @m4_exists = (SELECT COUNT(*) FROM information_schema.TABLES WHERE TABLE_SCHEMA = CONVERT(@m4_s USING utf8) AND TABLE_NAME = CONVERT(@m4_t USING utf8));
SET @m4_do = (@m4_guard='PASS' AND @m4_exists=1 AND (@m4_p<>'SELECT' OR @m4_grant_mode='table' OR @m4_s<>@m4_static));
SET @m4_sql = IF(@m4_do, CONCAT('GRANT ', @m4_p, ' ON `', @m4_s, '`.`', REPLACE(@m4_t,'`','``'), '` TO ', @m4_grantee), 'DO 0');
PREPARE m4_stmt FROM @m4_sql; EXECUTE m4_stmt; DEALLOCATE PREPARE m4_stmt;
SET @m4_skipped = CONCAT(@m4_skipped, IF(@m4_exists=1, '', CONCAT(@m4_s, '.', @m4_t, '', '; ')));
SET @m4_t = 'etcitem_buff藥水設置'; SET @m4_p = 'SELECT'; SET @m4_s = @m4_static;
SET @m4_exists = (SELECT COUNT(*) FROM information_schema.TABLES WHERE TABLE_SCHEMA = CONVERT(@m4_s USING utf8) AND TABLE_NAME = CONVERT(@m4_t USING utf8));
SET @m4_do = (@m4_guard='PASS' AND @m4_exists=1 AND (@m4_p<>'SELECT' OR @m4_grant_mode='table' OR @m4_s<>@m4_static));
SET @m4_sql = IF(@m4_do, CONCAT('GRANT ', @m4_p, ' ON `', @m4_s, '`.`', REPLACE(@m4_t,'`','``'), '` TO ', @m4_grantee), 'DO 0');
PREPARE m4_stmt FROM @m4_sql; EXECUTE m4_stmt; DEALLOCATE PREPARE m4_stmt;
SET @m4_skipped = CONCAT(@m4_skipped, IF(@m4_exists=1, '', CONCAT(@m4_s, '.', @m4_t, '', '; ')));
SET @m4_t = 'etcitem_doll_power_new'; SET @m4_p = 'SELECT'; SET @m4_s = @m4_static;
SET @m4_exists = (SELECT COUNT(*) FROM information_schema.TABLES WHERE TABLE_SCHEMA = CONVERT(@m4_s USING utf8) AND TABLE_NAME = CONVERT(@m4_t USING utf8));
SET @m4_do = (@m4_guard='PASS' AND @m4_exists=1 AND (@m4_p<>'SELECT' OR @m4_grant_mode='table' OR @m4_s<>@m4_static));
SET @m4_sql = IF(@m4_do, CONCAT('GRANT ', @m4_p, ' ON `', @m4_s, '`.`', REPLACE(@m4_t,'`','``'), '` TO ', @m4_grantee), 'DO 0');
PREPARE m4_stmt FROM @m4_sql; EXECUTE m4_stmt; DEALLOCATE PREPARE m4_stmt;
SET @m4_skipped = CONCAT(@m4_skipped, IF(@m4_exists=1, '', CONCAT(@m4_s, '.', @m4_t, '', '; ')));
SET @m4_t = 'etcitem_doll_type'; SET @m4_p = 'SELECT'; SET @m4_s = @m4_static;
SET @m4_exists = (SELECT COUNT(*) FROM information_schema.TABLES WHERE TABLE_SCHEMA = CONVERT(@m4_s USING utf8) AND TABLE_NAME = CONVERT(@m4_t USING utf8));
SET @m4_do = (@m4_guard='PASS' AND @m4_exists=1 AND (@m4_p<>'SELECT' OR @m4_grant_mode='table' OR @m4_s<>@m4_static));
SET @m4_sql = IF(@m4_do, CONCAT('GRANT ', @m4_p, ' ON `', @m4_s, '`.`', REPLACE(@m4_t,'`','``'), '` TO ', @m4_grantee), 'DO 0');
PREPARE m4_stmt FROM @m4_sql; EXECUTE m4_stmt; DEALLOCATE PREPARE m4_stmt;
SET @m4_skipped = CONCAT(@m4_skipped, IF(@m4_exists=1, '', CONCAT(@m4_s, '.', @m4_t, '', '; ')));
SET @m4_t = 'mapids'; SET @m4_p = 'SELECT'; SET @m4_s = @m4_static;
SET @m4_exists = (SELECT COUNT(*) FROM information_schema.TABLES WHERE TABLE_SCHEMA = CONVERT(@m4_s USING utf8) AND TABLE_NAME = CONVERT(@m4_t USING utf8));
SET @m4_do = (@m4_guard='PASS' AND @m4_exists=1 AND (@m4_p<>'SELECT' OR @m4_grant_mode='table' OR @m4_s<>@m4_static));
SET @m4_sql = IF(@m4_do, CONCAT('GRANT ', @m4_p, ' ON `', @m4_s, '`.`', REPLACE(@m4_t,'`','``'), '` TO ', @m4_grantee), 'DO 0');
PREPARE m4_stmt FROM @m4_sql; EXECUTE m4_stmt; DEALLOCATE PREPARE m4_stmt;
SET @m4_skipped = CONCAT(@m4_skipped, IF(@m4_exists=1, '', CONCAT(@m4_s, '.', @m4_t, '', '; ')));
SET @m4_t = 'mapids_shop'; SET @m4_p = 'SELECT'; SET @m4_s = @m4_static;
SET @m4_exists = (SELECT COUNT(*) FROM information_schema.TABLES WHERE TABLE_SCHEMA = CONVERT(@m4_s USING utf8) AND TABLE_NAME = CONVERT(@m4_t USING utf8));
SET @m4_do = (@m4_guard='PASS' AND @m4_exists=1 AND (@m4_p<>'SELECT' OR @m4_grant_mode='table' OR @m4_s<>@m4_static));
SET @m4_sql = IF(@m4_do, CONCAT('GRANT ', @m4_p, ' ON `', @m4_s, '`.`', REPLACE(@m4_t,'`','``'), '` TO ', @m4_grantee), 'DO 0');
PREPARE m4_stmt FROM @m4_sql; EXECUTE m4_stmt; DEALLOCATE PREPARE m4_stmt;
SET @m4_skipped = CONCAT(@m4_skipped, IF(@m4_exists=1, '', CONCAT(@m4_s, '.', @m4_t, '', '; ')));
SET @m4_t = 'mobskill'; SET @m4_p = 'SELECT'; SET @m4_s = @m4_static;
SET @m4_exists = (SELECT COUNT(*) FROM information_schema.TABLES WHERE TABLE_SCHEMA = CONVERT(@m4_s USING utf8) AND TABLE_NAME = CONVERT(@m4_t USING utf8));
SET @m4_do = (@m4_guard='PASS' AND @m4_exists=1 AND (@m4_p<>'SELECT' OR @m4_grant_mode='table' OR @m4_s<>@m4_static));
SET @m4_sql = IF(@m4_do, CONCAT('GRANT ', @m4_p, ' ON `', @m4_s, '`.`', REPLACE(@m4_t,'`','``'), '` TO ', @m4_grantee), 'DO 0');
PREPARE m4_stmt FROM @m4_sql; EXECUTE m4_stmt; DEALLOCATE PREPARE m4_stmt;
SET @m4_skipped = CONCAT(@m4_skipped, IF(@m4_exists=1, '', CONCAT(@m4_s, '.', @m4_t, '', '; ')));
SET @m4_t = 'npc'; SET @m4_p = 'SELECT'; SET @m4_s = @m4_static;
SET @m4_exists = (SELECT COUNT(*) FROM information_schema.TABLES WHERE TABLE_SCHEMA = CONVERT(@m4_s USING utf8) AND TABLE_NAME = CONVERT(@m4_t USING utf8));
SET @m4_do = (@m4_guard='PASS' AND @m4_exists=1 AND (@m4_p<>'SELECT' OR @m4_grant_mode='table' OR @m4_s<>@m4_static));
SET @m4_sql = IF(@m4_do, CONCAT('GRANT ', @m4_p, ' ON `', @m4_s, '`.`', REPLACE(@m4_t,'`','``'), '` TO ', @m4_grantee), 'DO 0');
PREPARE m4_stmt FROM @m4_sql; EXECUTE m4_stmt; DEALLOCATE PREPARE m4_stmt;
SET @m4_skipped = CONCAT(@m4_skipped, IF(@m4_exists=1, '', CONCAT(@m4_s, '.', @m4_t, '', '; ')));
SET @m4_t = 'npcchat'; SET @m4_p = 'SELECT'; SET @m4_s = @m4_static;
SET @m4_exists = (SELECT COUNT(*) FROM information_schema.TABLES WHERE TABLE_SCHEMA = CONVERT(@m4_s USING utf8) AND TABLE_NAME = CONVERT(@m4_t USING utf8));
SET @m4_do = (@m4_guard='PASS' AND @m4_exists=1 AND (@m4_p<>'SELECT' OR @m4_grant_mode='table' OR @m4_s<>@m4_static));
SET @m4_sql = IF(@m4_do, CONCAT('GRANT ', @m4_p, ' ON `', @m4_s, '`.`', REPLACE(@m4_t,'`','``'), '` TO ', @m4_grantee), 'DO 0');
PREPARE m4_stmt FROM @m4_sql; EXECUTE m4_stmt; DEALLOCATE PREPARE m4_stmt;
SET @m4_skipped = CONCAT(@m4_skipped, IF(@m4_exists=1, '', CONCAT(@m4_s, '.', @m4_t, '', '; ')));
SET @m4_t = 'pettypes'; SET @m4_p = 'SELECT'; SET @m4_s = @m4_static;
SET @m4_exists = (SELECT COUNT(*) FROM information_schema.TABLES WHERE TABLE_SCHEMA = CONVERT(@m4_s USING utf8) AND TABLE_NAME = CONVERT(@m4_t USING utf8));
SET @m4_do = (@m4_guard='PASS' AND @m4_exists=1 AND (@m4_p<>'SELECT' OR @m4_grant_mode='table' OR @m4_s<>@m4_static));
SET @m4_sql = IF(@m4_do, CONCAT('GRANT ', @m4_p, ' ON `', @m4_s, '`.`', REPLACE(@m4_t,'`','``'), '` TO ', @m4_grantee), 'DO 0');
PREPARE m4_stmt FROM @m4_sql; EXECUTE m4_stmt; DEALLOCATE PREPARE m4_stmt;
SET @m4_skipped = CONCAT(@m4_skipped, IF(@m4_exists=1, '', CONCAT(@m4_s, '.', @m4_t, '', '; ')));
SET @m4_t = 'polymorphs'; SET @m4_p = 'SELECT'; SET @m4_s = @m4_static;
SET @m4_exists = (SELECT COUNT(*) FROM information_schema.TABLES WHERE TABLE_SCHEMA = CONVERT(@m4_s USING utf8) AND TABLE_NAME = CONVERT(@m4_t USING utf8));
SET @m4_do = (@m4_guard='PASS' AND @m4_exists=1 AND (@m4_p<>'SELECT' OR @m4_grant_mode='table' OR @m4_s<>@m4_static));
SET @m4_sql = IF(@m4_do, CONCAT('GRANT ', @m4_p, ' ON `', @m4_s, '`.`', REPLACE(@m4_t,'`','``'), '` TO ', @m4_grantee), 'DO 0');
PREPARE m4_stmt FROM @m4_sql; EXECUTE m4_stmt; DEALLOCATE PREPARE m4_stmt;
SET @m4_skipped = CONCAT(@m4_skipped, IF(@m4_exists=1, '', CONCAT(@m4_s, '.', @m4_t, '', '; ')));
SET @m4_t = 'server_event'; SET @m4_p = 'SELECT'; SET @m4_s = @m4_static;
SET @m4_exists = (SELECT COUNT(*) FROM information_schema.TABLES WHERE TABLE_SCHEMA = CONVERT(@m4_s USING utf8) AND TABLE_NAME = CONVERT(@m4_t USING utf8));
SET @m4_do = (@m4_guard='PASS' AND @m4_exists=1 AND (@m4_p<>'SELECT' OR @m4_grant_mode='table' OR @m4_s<>@m4_static));
SET @m4_sql = IF(@m4_do, CONCAT('GRANT ', @m4_p, ' ON `', @m4_s, '`.`', REPLACE(@m4_t,'`','``'), '` TO ', @m4_grantee), 'DO 0');
PREPARE m4_stmt FROM @m4_sql; EXECUTE m4_stmt; DEALLOCATE PREPARE m4_stmt;
SET @m4_skipped = CONCAT(@m4_skipped, IF(@m4_exists=1, '', CONCAT(@m4_s, '.', @m4_t, '', '; ')));
SET @m4_t = 'skills'; SET @m4_p = 'SELECT'; SET @m4_s = @m4_static;
SET @m4_exists = (SELECT COUNT(*) FROM information_schema.TABLES WHERE TABLE_SCHEMA = CONVERT(@m4_s USING utf8) AND TABLE_NAME = CONVERT(@m4_t USING utf8));
SET @m4_do = (@m4_guard='PASS' AND @m4_exists=1 AND (@m4_p<>'SELECT' OR @m4_grant_mode='table' OR @m4_s<>@m4_static));
SET @m4_sql = IF(@m4_do, CONCAT('GRANT ', @m4_p, ' ON `', @m4_s, '`.`', REPLACE(@m4_t,'`','``'), '` TO ', @m4_grantee), 'DO 0');
PREPARE m4_stmt FROM @m4_sql; EXECUTE m4_stmt; DEALLOCATE PREPARE m4_stmt;
SET @m4_skipped = CONCAT(@m4_skipped, IF(@m4_exists=1, '', CONCAT(@m4_s, '.', @m4_t, '', '; ')));
SET @m4_t = 'skills_buff_ability'; SET @m4_p = 'SELECT'; SET @m4_s = @m4_static;
SET @m4_exists = (SELECT COUNT(*) FROM information_schema.TABLES WHERE TABLE_SCHEMA = CONVERT(@m4_s USING utf8) AND TABLE_NAME = CONVERT(@m4_t USING utf8));
SET @m4_do = (@m4_guard='PASS' AND @m4_exists=1 AND (@m4_p<>'SELECT' OR @m4_grant_mode='table' OR @m4_s<>@m4_static));
SET @m4_sql = IF(@m4_do, CONCAT('GRANT ', @m4_p, ' ON `', @m4_s, '`.`', REPLACE(@m4_t,'`','``'), '` TO ', @m4_grantee), 'DO 0');
PREPARE m4_stmt FROM @m4_sql; EXECUTE m4_stmt; DEALLOCATE PREPARE m4_stmt;
SET @m4_skipped = CONCAT(@m4_skipped, IF(@m4_exists=1, '', CONCAT(@m4_s, '.', @m4_t, '', '; ')));
SET @m4_t = 'skills_相消免疫技能'; SET @m4_p = 'SELECT'; SET @m4_s = @m4_static;
SET @m4_exists = (SELECT COUNT(*) FROM information_schema.TABLES WHERE TABLE_SCHEMA = CONVERT(@m4_s USING utf8) AND TABLE_NAME = CONVERT(@m4_t USING utf8));
SET @m4_do = (@m4_guard='PASS' AND @m4_exists=1 AND (@m4_p<>'SELECT' OR @m4_grant_mode='table' OR @m4_s<>@m4_static));
SET @m4_sql = IF(@m4_do, CONCAT('GRANT ', @m4_p, ' ON `', @m4_s, '`.`', REPLACE(@m4_t,'`','``'), '` TO ', @m4_grantee), 'DO 0');
PREPARE m4_stmt FROM @m4_sql; EXECUTE m4_stmt; DEALLOCATE PREPARE m4_stmt;
SET @m4_skipped = CONCAT(@m4_skipped, IF(@m4_exists=1, '', CONCAT(@m4_s, '.', @m4_t, '', '; ')));
SET @m4_t = 'skills_相消怪物轉換'; SET @m4_p = 'SELECT'; SET @m4_s = @m4_static;
SET @m4_exists = (SELECT COUNT(*) FROM information_schema.TABLES WHERE TABLE_SCHEMA = CONVERT(@m4_s USING utf8) AND TABLE_NAME = CONVERT(@m4_t USING utf8));
SET @m4_do = (@m4_guard='PASS' AND @m4_exists=1 AND (@m4_p<>'SELECT' OR @m4_grant_mode='table' OR @m4_s<>@m4_static));
SET @m4_sql = IF(@m4_do, CONCAT('GRANT ', @m4_p, ' ON `', @m4_s, '`.`', REPLACE(@m4_t,'`','``'), '` TO ', @m4_grantee), 'DO 0');
PREPARE m4_stmt FROM @m4_sql; EXECUTE m4_stmt; DEALLOCATE PREPARE m4_stmt;
SET @m4_skipped = CONCAT(@m4_skipped, IF(@m4_exists=1, '', CONCAT(@m4_s, '.', @m4_t, '', '; ')));
SET @m4_t = 'skills_負面boss免疫'; SET @m4_p = 'SELECT'; SET @m4_s = @m4_static;
SET @m4_exists = (SELECT COUNT(*) FROM information_schema.TABLES WHERE TABLE_SCHEMA = CONVERT(@m4_s USING utf8) AND TABLE_NAME = CONVERT(@m4_t USING utf8));
SET @m4_do = (@m4_guard='PASS' AND @m4_exists=1 AND (@m4_p<>'SELECT' OR @m4_grant_mode='table' OR @m4_s<>@m4_static));
SET @m4_sql = IF(@m4_do, CONCAT('GRANT ', @m4_p, ' ON `', @m4_s, '`.`', REPLACE(@m4_t,'`','``'), '` TO ', @m4_grantee), 'DO 0');
PREPARE m4_stmt FROM @m4_sql; EXECUTE m4_stmt; DEALLOCATE PREPARE m4_stmt;
SET @m4_skipped = CONCAT(@m4_skipped, IF(@m4_exists=1, '', CONCAT(@m4_s, '.', @m4_t, '', '; ')));
SET @m4_t = 'skills_負面技能控制'; SET @m4_p = 'SELECT'; SET @m4_s = @m4_static;
SET @m4_exists = (SELECT COUNT(*) FROM information_schema.TABLES WHERE TABLE_SCHEMA = CONVERT(@m4_s USING utf8) AND TABLE_NAME = CONVERT(@m4_t USING utf8));
SET @m4_do = (@m4_guard='PASS' AND @m4_exists=1 AND (@m4_p<>'SELECT' OR @m4_grant_mode='table' OR @m4_s<>@m4_static));
SET @m4_sql = IF(@m4_do, CONCAT('GRANT ', @m4_p, ' ON `', @m4_s, '`.`', REPLACE(@m4_t,'`','``'), '` TO ', @m4_grantee), 'DO 0');
PREPARE m4_stmt FROM @m4_sql; EXECUTE m4_stmt; DEALLOCATE PREPARE m4_stmt;
SET @m4_skipped = CONCAT(@m4_skipped, IF(@m4_exists=1, '', CONCAT(@m4_s, '.', @m4_t, '', '; ')));
SET @m4_t = 'skills_魔屏無效技能'; SET @m4_p = 'SELECT'; SET @m4_s = @m4_static;
SET @m4_exists = (SELECT COUNT(*) FROM information_schema.TABLES WHERE TABLE_SCHEMA = CONVERT(@m4_s USING utf8) AND TABLE_NAME = CONVERT(@m4_t USING utf8));
SET @m4_do = (@m4_guard='PASS' AND @m4_exists=1 AND (@m4_p<>'SELECT' OR @m4_grant_mode='table' OR @m4_s<>@m4_static));
SET @m4_sql = IF(@m4_do, CONCAT('GRANT ', @m4_p, ' ON `', @m4_s, '`.`', REPLACE(@m4_t,'`','``'), '` TO ', @m4_grantee), 'DO 0');
PREPARE m4_stmt FROM @m4_sql; EXECUTE m4_stmt; DEALLOCATE PREPARE m4_stmt;
SET @m4_skipped = CONCAT(@m4_skipped, IF(@m4_exists=1, '', CONCAT(@m4_s, '.', @m4_t, '', '; ')));
SET @m4_t = 'spawnlist_npc'; SET @m4_p = 'SELECT'; SET @m4_s = @m4_static;
SET @m4_exists = (SELECT COUNT(*) FROM information_schema.TABLES WHERE TABLE_SCHEMA = CONVERT(@m4_s USING utf8) AND TABLE_NAME = CONVERT(@m4_t USING utf8));
SET @m4_do = (@m4_guard='PASS' AND @m4_exists=1 AND (@m4_p<>'SELECT' OR @m4_grant_mode='table' OR @m4_s<>@m4_static));
SET @m4_sql = IF(@m4_do, CONCAT('GRANT ', @m4_p, ' ON `', @m4_s, '`.`', REPLACE(@m4_t,'`','``'), '` TO ', @m4_grantee), 'DO 0');
PREPARE m4_stmt FROM @m4_sql; EXECUTE m4_stmt; DEALLOCATE PREPARE m4_stmt;
SET @m4_skipped = CONCAT(@m4_skipped, IF(@m4_exists=1, '', CONCAT(@m4_s, '.', @m4_t, '', '; ')));
SET @m4_t = 'spawnlist_time'; SET @m4_p = 'SELECT'; SET @m4_s = @m4_static;
SET @m4_exists = (SELECT COUNT(*) FROM information_schema.TABLES WHERE TABLE_SCHEMA = CONVERT(@m4_s USING utf8) AND TABLE_NAME = CONVERT(@m4_t USING utf8));
SET @m4_do = (@m4_guard='PASS' AND @m4_exists=1 AND (@m4_p<>'SELECT' OR @m4_grant_mode='table' OR @m4_s<>@m4_static));
SET @m4_sql = IF(@m4_do, CONCAT('GRANT ', @m4_p, ' ON `', @m4_s, '`.`', REPLACE(@m4_t,'`','``'), '` TO ', @m4_grantee), 'DO 0');
PREPARE m4_stmt FROM @m4_sql; EXECUTE m4_stmt; DEALLOCATE PREPARE m4_stmt;
SET @m4_skipped = CONCAT(@m4_skipped, IF(@m4_exists=1, '', CONCAT(@m4_s, '.', @m4_t, '', '; ')));
SET @m4_t = 'spr_action'; SET @m4_p = 'SELECT'; SET @m4_s = @m4_static;
SET @m4_exists = (SELECT COUNT(*) FROM information_schema.TABLES WHERE TABLE_SCHEMA = CONVERT(@m4_s USING utf8) AND TABLE_NAME = CONVERT(@m4_t USING utf8));
SET @m4_do = (@m4_guard='PASS' AND @m4_exists=1 AND (@m4_p<>'SELECT' OR @m4_grant_mode='table' OR @m4_s<>@m4_static));
SET @m4_sql = IF(@m4_do, CONCAT('GRANT ', @m4_p, ' ON `', @m4_s, '`.`', REPLACE(@m4_t,'`','``'), '` TO ', @m4_grantee), 'DO 0');
PREPARE m4_stmt FROM @m4_sql; EXECUTE m4_stmt; DEALLOCATE PREPARE m4_stmt;
SET @m4_skipped = CONCAT(@m4_skipped, IF(@m4_exists=1, '', CONCAT(@m4_s, '.', @m4_t, '', '; ')));
SET @m4_t = 'weapon'; SET @m4_p = 'SELECT'; SET @m4_s = @m4_static;
SET @m4_exists = (SELECT COUNT(*) FROM information_schema.TABLES WHERE TABLE_SCHEMA = CONVERT(@m4_s USING utf8) AND TABLE_NAME = CONVERT(@m4_t USING utf8));
SET @m4_do = (@m4_guard='PASS' AND @m4_exists=1 AND (@m4_p<>'SELECT' OR @m4_grant_mode='table' OR @m4_s<>@m4_static));
SET @m4_sql = IF(@m4_do, CONCAT('GRANT ', @m4_p, ' ON `', @m4_s, '`.`', REPLACE(@m4_t,'`','``'), '` TO ', @m4_grantee), 'DO 0');
PREPARE m4_stmt FROM @m4_sql; EXECUTE m4_stmt; DEALLOCATE PREPARE m4_stmt;
SET @m4_skipped = CONCAT(@m4_skipped, IF(@m4_exists=1, '', CONCAT(@m4_s, '.', @m4_t, '', '; ')));
SET @m4_t = '假人_人物清單'; SET @m4_p = 'SELECT'; SET @m4_s = @m4_static;
SET @m4_exists = (SELECT COUNT(*) FROM information_schema.TABLES WHERE TABLE_SCHEMA = CONVERT(@m4_s USING utf8) AND TABLE_NAME = CONVERT(@m4_t USING utf8));
SET @m4_do = (@m4_guard='PASS' AND @m4_exists=1 AND (@m4_p<>'SELECT' OR @m4_grant_mode='table' OR @m4_s<>@m4_static));
SET @m4_sql = IF(@m4_do, CONCAT('GRANT ', @m4_p, ' ON `', @m4_s, '`.`', REPLACE(@m4_t,'`','``'), '` TO ', @m4_grantee), 'DO 0');
PREPARE m4_stmt FROM @m4_sql; EXECUTE m4_stmt; DEALLOCATE PREPARE m4_stmt;
SET @m4_skipped = CONCAT(@m4_skipped, IF(@m4_exists=1, '', CONCAT(@m4_s, '.', @m4_t, '', '; ')));
SET @m4_t = '假人_能力設置'; SET @m4_p = 'SELECT'; SET @m4_s = @m4_static;
SET @m4_exists = (SELECT COUNT(*) FROM information_schema.TABLES WHERE TABLE_SCHEMA = CONVERT(@m4_s USING utf8) AND TABLE_NAME = CONVERT(@m4_t USING utf8));
SET @m4_do = (@m4_guard='PASS' AND @m4_exists=1 AND (@m4_p<>'SELECT' OR @m4_grant_mode='table' OR @m4_s<>@m4_static));
SET @m4_sql = IF(@m4_do, CONCAT('GRANT ', @m4_p, ' ON `', @m4_s, '`.`', REPLACE(@m4_t,'`','``'), '` TO ', @m4_grantee), 'DO 0');
PREPARE m4_stmt FROM @m4_sql; EXECUTE m4_stmt; DEALLOCATE PREPARE m4_stmt;
SET @m4_skipped = CONCAT(@m4_skipped, IF(@m4_exists=1, '', CONCAT(@m4_s, '.', @m4_t, '', '; ')));
SET @m4_t = '假人_設置狀態'; SET @m4_p = 'SELECT'; SET @m4_s = @m4_static;
SET @m4_exists = (SELECT COUNT(*) FROM information_schema.TABLES WHERE TABLE_SCHEMA = CONVERT(@m4_s USING utf8) AND TABLE_NAME = CONVERT(@m4_t USING utf8));
SET @m4_do = (@m4_guard='PASS' AND @m4_exists=1 AND (@m4_p<>'SELECT' OR @m4_grant_mode='table' OR @m4_s<>@m4_static));
SET @m4_sql = IF(@m4_do, CONCAT('GRANT ', @m4_p, ' ON `', @m4_s, '`.`', REPLACE(@m4_t,'`','``'), '` TO ', @m4_grantee), 'DO 0');
PREPARE m4_stmt FROM @m4_sql; EXECUTE m4_stmt; DEALLOCATE PREPARE m4_stmt;
SET @m4_skipped = CONCAT(@m4_skipped, IF(@m4_exists=1, '', CONCAT(@m4_s, '.', @m4_t, '', '; ')));
SET @m4_t = '系統_劇情任務'; SET @m4_p = 'SELECT'; SET @m4_s = @m4_static;
SET @m4_exists = (SELECT COUNT(*) FROM information_schema.TABLES WHERE TABLE_SCHEMA = CONVERT(@m4_s USING utf8) AND TABLE_NAME = CONVERT(@m4_t USING utf8));
SET @m4_do = (@m4_guard='PASS' AND @m4_exists=1 AND (@m4_p<>'SELECT' OR @m4_grant_mode='table' OR @m4_s<>@m4_static));
SET @m4_sql = IF(@m4_do, CONCAT('GRANT ', @m4_p, ' ON `', @m4_s, '`.`', REPLACE(@m4_t,'`','``'), '` TO ', @m4_grantee), 'DO 0');
PREPARE m4_stmt FROM @m4_sql; EXECUTE m4_stmt; DEALLOCATE PREPARE m4_stmt;
SET @m4_skipped = CONCAT(@m4_skipped, IF(@m4_exists=1, '', CONCAT(@m4_s, '.', @m4_t, '', '; ')));
SET @m4_t = '系統_受擊無動作gfx'; SET @m4_p = 'SELECT'; SET @m4_s = @m4_static;
SET @m4_exists = (SELECT COUNT(*) FROM information_schema.TABLES WHERE TABLE_SCHEMA = CONVERT(@m4_s USING utf8) AND TABLE_NAME = CONVERT(@m4_t USING utf8));
SET @m4_do = (@m4_guard='PASS' AND @m4_exists=1 AND (@m4_p<>'SELECT' OR @m4_grant_mode='table' OR @m4_s<>@m4_static));
SET @m4_sql = IF(@m4_do, CONCAT('GRANT ', @m4_p, ' ON `', @m4_s, '`.`', REPLACE(@m4_t,'`','``'), '` TO ', @m4_grantee), 'DO 0');
PREPARE m4_stmt FROM @m4_sql; EXECUTE m4_stmt; DEALLOCATE PREPARE m4_stmt;
SET @m4_skipped = CONCAT(@m4_skipped, IF(@m4_exists=1, '', CONCAT(@m4_s, '.', @m4_t, '', '; ')));
SET @m4_t = '裝備_armor_bless_指定'; SET @m4_p = 'SELECT'; SET @m4_s = @m4_static;
SET @m4_exists = (SELECT COUNT(*) FROM information_schema.TABLES WHERE TABLE_SCHEMA = CONVERT(@m4_s USING utf8) AND TABLE_NAME = CONVERT(@m4_t USING utf8));
SET @m4_do = (@m4_guard='PASS' AND @m4_exists=1 AND (@m4_p<>'SELECT' OR @m4_grant_mode='table' OR @m4_s<>@m4_static));
SET @m4_sql = IF(@m4_do, CONCAT('GRANT ', @m4_p, ' ON `', @m4_s, '`.`', REPLACE(@m4_t,'`','``'), '` TO ', @m4_grantee), 'DO 0');
PREPARE m4_stmt FROM @m4_sql; EXECUTE m4_stmt; DEALLOCATE PREPARE m4_stmt;
SET @m4_skipped = CONCAT(@m4_skipped, IF(@m4_exists=1, '', CONCAT(@m4_s, '.', @m4_t, '', '; ')));
SET @m4_t = '裝備_armor_bless_類型'; SET @m4_p = 'SELECT'; SET @m4_s = @m4_static;
SET @m4_exists = (SELECT COUNT(*) FROM information_schema.TABLES WHERE TABLE_SCHEMA = CONVERT(@m4_s USING utf8) AND TABLE_NAME = CONVERT(@m4_t USING utf8));
SET @m4_do = (@m4_guard='PASS' AND @m4_exists=1 AND (@m4_p<>'SELECT' OR @m4_grant_mode='table' OR @m4_s<>@m4_static));
SET @m4_sql = IF(@m4_do, CONCAT('GRANT ', @m4_p, ' ON `', @m4_s, '`.`', REPLACE(@m4_t,'`','``'), '` TO ', @m4_grantee), 'DO 0');
PREPARE m4_stmt FROM @m4_sql; EXECUTE m4_stmt; DEALLOCATE PREPARE m4_stmt;
SET @m4_skipped = CONCAT(@m4_skipped, IF(@m4_exists=1, '', CONCAT(@m4_s, '.', @m4_t, '', '; ')));
SET @m4_t = '裝備_armor_enchant_指定'; SET @m4_p = 'SELECT'; SET @m4_s = @m4_static;
SET @m4_exists = (SELECT COUNT(*) FROM information_schema.TABLES WHERE TABLE_SCHEMA = CONVERT(@m4_s USING utf8) AND TABLE_NAME = CONVERT(@m4_t USING utf8));
SET @m4_do = (@m4_guard='PASS' AND @m4_exists=1 AND (@m4_p<>'SELECT' OR @m4_grant_mode='table' OR @m4_s<>@m4_static));
SET @m4_sql = IF(@m4_do, CONCAT('GRANT ', @m4_p, ' ON `', @m4_s, '`.`', REPLACE(@m4_t,'`','``'), '` TO ', @m4_grantee), 'DO 0');
PREPARE m4_stmt FROM @m4_sql; EXECUTE m4_stmt; DEALLOCATE PREPARE m4_stmt;
SET @m4_skipped = CONCAT(@m4_skipped, IF(@m4_exists=1, '', CONCAT(@m4_s, '.', @m4_t, '', '; ')));
SET @m4_t = '裝備_armor_enchant_類型'; SET @m4_p = 'SELECT'; SET @m4_s = @m4_static;
SET @m4_exists = (SELECT COUNT(*) FROM information_schema.TABLES WHERE TABLE_SCHEMA = CONVERT(@m4_s USING utf8) AND TABLE_NAME = CONVERT(@m4_t USING utf8));
SET @m4_do = (@m4_guard='PASS' AND @m4_exists=1 AND (@m4_p<>'SELECT' OR @m4_grant_mode='table' OR @m4_s<>@m4_static));
SET @m4_sql = IF(@m4_do, CONCAT('GRANT ', @m4_p, ' ON `', @m4_s, '`.`', REPLACE(@m4_t,'`','``'), '` TO ', @m4_grantee), 'DO 0');
PREPARE m4_stmt FROM @m4_sql; EXECUTE m4_stmt; DEALLOCATE PREPARE m4_stmt;
SET @m4_skipped = CONCAT(@m4_skipped, IF(@m4_exists=1, '', CONCAT(@m4_s, '.', @m4_t, '', '; ')));
SET @m4_t = '裝備_armor_greater_類型'; SET @m4_p = 'SELECT'; SET @m4_s = @m4_static;
SET @m4_exists = (SELECT COUNT(*) FROM information_schema.TABLES WHERE TABLE_SCHEMA = CONVERT(@m4_s USING utf8) AND TABLE_NAME = CONVERT(@m4_t USING utf8));
SET @m4_do = (@m4_guard='PASS' AND @m4_exists=1 AND (@m4_p<>'SELECT' OR @m4_grant_mode='table' OR @m4_s<>@m4_static));
SET @m4_sql = IF(@m4_do, CONCAT('GRANT ', @m4_p, ' ON `', @m4_s, '`.`', REPLACE(@m4_t,'`','``'), '` TO ', @m4_grantee), 'DO 0');
PREPARE m4_stmt FROM @m4_sql; EXECUTE m4_stmt; DEALLOCATE PREPARE m4_stmt;
SET @m4_skipped = CONCAT(@m4_skipped, IF(@m4_exists=1, '', CONCAT(@m4_s, '.', @m4_t, '', '; ')));
SET @m4_t = '裝備_armor_魔法'; SET @m4_p = 'SELECT'; SET @m4_s = @m4_static;
SET @m4_exists = (SELECT COUNT(*) FROM information_schema.TABLES WHERE TABLE_SCHEMA = CONVERT(@m4_s USING utf8) AND TABLE_NAME = CONVERT(@m4_t USING utf8));
SET @m4_do = (@m4_guard='PASS' AND @m4_exists=1 AND (@m4_p<>'SELECT' OR @m4_grant_mode='table' OR @m4_s<>@m4_static));
SET @m4_sql = IF(@m4_do, CONCAT('GRANT ', @m4_p, ' ON `', @m4_s, '`.`', REPLACE(@m4_t,'`','``'), '` TO ', @m4_grantee), 'DO 0');
PREPARE m4_stmt FROM @m4_sql; EXECUTE m4_stmt; DEALLOCATE PREPARE m4_stmt;
SET @m4_skipped = CONCAT(@m4_skipped, IF(@m4_exists=1, '', CONCAT(@m4_s, '.', @m4_t, '', '; ')));
SET @m4_t = '裝備_weapon_bless_指定'; SET @m4_p = 'SELECT'; SET @m4_s = @m4_static;
SET @m4_exists = (SELECT COUNT(*) FROM information_schema.TABLES WHERE TABLE_SCHEMA = CONVERT(@m4_s USING utf8) AND TABLE_NAME = CONVERT(@m4_t USING utf8));
SET @m4_do = (@m4_guard='PASS' AND @m4_exists=1 AND (@m4_p<>'SELECT' OR @m4_grant_mode='table' OR @m4_s<>@m4_static));
SET @m4_sql = IF(@m4_do, CONCAT('GRANT ', @m4_p, ' ON `', @m4_s, '`.`', REPLACE(@m4_t,'`','``'), '` TO ', @m4_grantee), 'DO 0');
PREPARE m4_stmt FROM @m4_sql; EXECUTE m4_stmt; DEALLOCATE PREPARE m4_stmt;
SET @m4_skipped = CONCAT(@m4_skipped, IF(@m4_exists=1, '', CONCAT(@m4_s, '.', @m4_t, '', '; ')));
SET @m4_t = '裝備_weapon_bless_類型'; SET @m4_p = 'SELECT'; SET @m4_s = @m4_static;
SET @m4_exists = (SELECT COUNT(*) FROM information_schema.TABLES WHERE TABLE_SCHEMA = CONVERT(@m4_s USING utf8) AND TABLE_NAME = CONVERT(@m4_t USING utf8));
SET @m4_do = (@m4_guard='PASS' AND @m4_exists=1 AND (@m4_p<>'SELECT' OR @m4_grant_mode='table' OR @m4_s<>@m4_static));
SET @m4_sql = IF(@m4_do, CONCAT('GRANT ', @m4_p, ' ON `', @m4_s, '`.`', REPLACE(@m4_t,'`','``'), '` TO ', @m4_grantee), 'DO 0');
PREPARE m4_stmt FROM @m4_sql; EXECUTE m4_stmt; DEALLOCATE PREPARE m4_stmt;
SET @m4_skipped = CONCAT(@m4_skipped, IF(@m4_exists=1, '', CONCAT(@m4_s, '.', @m4_t, '', '; ')));
SET @m4_t = '裝備_weapon_enchant_指定'; SET @m4_p = 'SELECT'; SET @m4_s = @m4_static;
SET @m4_exists = (SELECT COUNT(*) FROM information_schema.TABLES WHERE TABLE_SCHEMA = CONVERT(@m4_s USING utf8) AND TABLE_NAME = CONVERT(@m4_t USING utf8));
SET @m4_do = (@m4_guard='PASS' AND @m4_exists=1 AND (@m4_p<>'SELECT' OR @m4_grant_mode='table' OR @m4_s<>@m4_static));
SET @m4_sql = IF(@m4_do, CONCAT('GRANT ', @m4_p, ' ON `', @m4_s, '`.`', REPLACE(@m4_t,'`','``'), '` TO ', @m4_grantee), 'DO 0');
PREPARE m4_stmt FROM @m4_sql; EXECUTE m4_stmt; DEALLOCATE PREPARE m4_stmt;
SET @m4_skipped = CONCAT(@m4_skipped, IF(@m4_exists=1, '', CONCAT(@m4_s, '.', @m4_t, '', '; ')));
SET @m4_t = '裝備_weapon_enchant_類型'; SET @m4_p = 'SELECT'; SET @m4_s = @m4_static;
SET @m4_exists = (SELECT COUNT(*) FROM information_schema.TABLES WHERE TABLE_SCHEMA = CONVERT(@m4_s USING utf8) AND TABLE_NAME = CONVERT(@m4_t USING utf8));
SET @m4_do = (@m4_guard='PASS' AND @m4_exists=1 AND (@m4_p<>'SELECT' OR @m4_grant_mode='table' OR @m4_s<>@m4_static));
SET @m4_sql = IF(@m4_do, CONCAT('GRANT ', @m4_p, ' ON `', @m4_s, '`.`', REPLACE(@m4_t,'`','``'), '` TO ', @m4_grantee), 'DO 0');
PREPARE m4_stmt FROM @m4_sql; EXECUTE m4_stmt; DEALLOCATE PREPARE m4_stmt;
SET @m4_skipped = CONCAT(@m4_skipped, IF(@m4_exists=1, '', CONCAT(@m4_s, '.', @m4_t, '', '; ')));
SET @m4_t = '裝備_weapon_greater_類型'; SET @m4_p = 'SELECT'; SET @m4_s = @m4_static;
SET @m4_exists = (SELECT COUNT(*) FROM information_schema.TABLES WHERE TABLE_SCHEMA = CONVERT(@m4_s USING utf8) AND TABLE_NAME = CONVERT(@m4_t USING utf8));
SET @m4_do = (@m4_guard='PASS' AND @m4_exists=1 AND (@m4_p<>'SELECT' OR @m4_grant_mode='table' OR @m4_s<>@m4_static));
SET @m4_sql = IF(@m4_do, CONCAT('GRANT ', @m4_p, ' ON `', @m4_s, '`.`', REPLACE(@m4_t,'`','``'), '` TO ', @m4_grantee), 'DO 0');
PREPARE m4_stmt FROM @m4_sql; EXECUTE m4_stmt; DEALLOCATE PREPARE m4_stmt;
SET @m4_skipped = CONCAT(@m4_skipped, IF(@m4_exists=1, '', CONCAT(@m4_s, '.', @m4_t, '', '; ')));
SET @m4_t = '裝備_weapon_屬性'; SET @m4_p = 'SELECT'; SET @m4_s = @m4_static;
SET @m4_exists = (SELECT COUNT(*) FROM information_schema.TABLES WHERE TABLE_SCHEMA = CONVERT(@m4_s USING utf8) AND TABLE_NAME = CONVERT(@m4_t USING utf8));
SET @m4_do = (@m4_guard='PASS' AND @m4_exists=1 AND (@m4_p<>'SELECT' OR @m4_grant_mode='table' OR @m4_s<>@m4_static));
SET @m4_sql = IF(@m4_do, CONCAT('GRANT ', @m4_p, ' ON `', @m4_s, '`.`', REPLACE(@m4_t,'`','``'), '` TO ', @m4_grantee), 'DO 0');
PREPARE m4_stmt FROM @m4_sql; EXECUTE m4_stmt; DEALLOCATE PREPARE m4_stmt;
SET @m4_skipped = CONCAT(@m4_skipped, IF(@m4_exists=1, '', CONCAT(@m4_s, '.', @m4_t, '', '; ')));
SET @m4_t = '裝備_weapon_魔法'; SET @m4_p = 'SELECT'; SET @m4_s = @m4_static;
SET @m4_exists = (SELECT COUNT(*) FROM information_schema.TABLES WHERE TABLE_SCHEMA = CONVERT(@m4_s USING utf8) AND TABLE_NAME = CONVERT(@m4_t USING utf8));
SET @m4_do = (@m4_guard='PASS' AND @m4_exists=1 AND (@m4_p<>'SELECT' OR @m4_grant_mode='table' OR @m4_s<>@m4_static));
SET @m4_sql = IF(@m4_do, CONCAT('GRANT ', @m4_p, ' ON `', @m4_s, '`.`', REPLACE(@m4_t,'`','``'), '` TO ', @m4_grantee), 'DO 0');
PREPARE m4_stmt FROM @m4_sql; EXECUTE m4_stmt; DEALLOCATE PREPARE m4_stmt;
SET @m4_skipped = CONCAT(@m4_skipped, IF(@m4_exists=1, '', CONCAT(@m4_s, '.', @m4_t, '', '; ')));
SET @m4_t = '裝備_特殊_卡片_組合'; SET @m4_p = 'SELECT'; SET @m4_s = @m4_static;
SET @m4_exists = (SELECT COUNT(*) FROM information_schema.TABLES WHERE TABLE_SCHEMA = CONVERT(@m4_s USING utf8) AND TABLE_NAME = CONVERT(@m4_t USING utf8));
SET @m4_do = (@m4_guard='PASS' AND @m4_exists=1 AND (@m4_p<>'SELECT' OR @m4_grant_mode='table' OR @m4_s<>@m4_static));
SET @m4_sql = IF(@m4_do, CONCAT('GRANT ', @m4_p, ' ON `', @m4_s, '`.`', REPLACE(@m4_t,'`','``'), '` TO ', @m4_grantee), 'DO 0');
PREPARE m4_stmt FROM @m4_sql; EXECUTE m4_stmt; DEALLOCATE PREPARE m4_stmt;
SET @m4_skipped = CONCAT(@m4_skipped, IF(@m4_exists=1, '', CONCAT(@m4_s, '.', @m4_t, '', '; ')));
SET @m4_t = '裝備_特殊_卡片_能力'; SET @m4_p = 'SELECT'; SET @m4_s = @m4_static;
SET @m4_exists = (SELECT COUNT(*) FROM information_schema.TABLES WHERE TABLE_SCHEMA = CONVERT(@m4_s USING utf8) AND TABLE_NAME = CONVERT(@m4_t USING utf8));
SET @m4_do = (@m4_guard='PASS' AND @m4_exists=1 AND (@m4_p<>'SELECT' OR @m4_grant_mode='table' OR @m4_s<>@m4_static));
SET @m4_sql = IF(@m4_do, CONCAT('GRANT ', @m4_p, ' ON `', @m4_s, '`.`', REPLACE(@m4_t,'`','``'), '` TO ', @m4_grantee), 'DO 0');
PREPARE m4_stmt FROM @m4_sql; EXECUTE m4_stmt; DEALLOCATE PREPARE m4_stmt;
SET @m4_skipped = CONCAT(@m4_skipped, IF(@m4_exists=1, '', CONCAT(@m4_s, '.', @m4_t, '', '; ')));
SET @m4_t = '裝備_特殊_套裝_能力'; SET @m4_p = 'SELECT'; SET @m4_s = @m4_static;
SET @m4_exists = (SELECT COUNT(*) FROM information_schema.TABLES WHERE TABLE_SCHEMA = CONVERT(@m4_s USING utf8) AND TABLE_NAME = CONVERT(@m4_t USING utf8));
SET @m4_do = (@m4_guard='PASS' AND @m4_exists=1 AND (@m4_p<>'SELECT' OR @m4_grant_mode='table' OR @m4_s<>@m4_static));
SET @m4_sql = IF(@m4_do, CONCAT('GRANT ', @m4_p, ' ON `', @m4_s, '`.`', REPLACE(@m4_t,'`','``'), '` TO ', @m4_grantee), 'DO 0');
PREPARE m4_stmt FROM @m4_sql; EXECUTE m4_stmt; DEALLOCATE PREPARE m4_stmt;
SET @m4_skipped = CONCAT(@m4_skipped, IF(@m4_exists=1, '', CONCAT(@m4_s, '.', @m4_t, '', '; ')));
SET @m4_t = '裝備_特殊_魔石_魔法'; SET @m4_p = 'SELECT'; SET @m4_s = @m4_static;
SET @m4_exists = (SELECT COUNT(*) FROM information_schema.TABLES WHERE TABLE_SCHEMA = CONVERT(@m4_s USING utf8) AND TABLE_NAME = CONVERT(@m4_t USING utf8));
SET @m4_do = (@m4_guard='PASS' AND @m4_exists=1 AND (@m4_p<>'SELECT' OR @m4_grant_mode='table' OR @m4_s<>@m4_static));
SET @m4_sql = IF(@m4_do, CONCAT('GRANT ', @m4_p, ' ON `', @m4_s, '`.`', REPLACE(@m4_t,'`','``'), '` TO ', @m4_grantee), 'DO 0');
PREPARE m4_stmt FROM @m4_sql; EXECUTE m4_stmt; DEALLOCATE PREPARE m4_stmt;
SET @m4_skipped = CONCAT(@m4_skipped, IF(@m4_exists=1, '', CONCAT(@m4_s, '.', @m4_t, '', '; ')));
SET @m4_t = '角色_力量成長'; SET @m4_p = 'SELECT'; SET @m4_s = @m4_static;
SET @m4_exists = (SELECT COUNT(*) FROM information_schema.TABLES WHERE TABLE_SCHEMA = CONVERT(@m4_s USING utf8) AND TABLE_NAME = CONVERT(@m4_t USING utf8));
SET @m4_do = (@m4_guard='PASS' AND @m4_exists=1 AND (@m4_p<>'SELECT' OR @m4_grant_mode='table' OR @m4_s<>@m4_static));
SET @m4_sql = IF(@m4_do, CONCAT('GRANT ', @m4_p, ' ON `', @m4_s, '`.`', REPLACE(@m4_t,'`','``'), '` TO ', @m4_grantee), 'DO 0');
PREPARE m4_stmt FROM @m4_sql; EXECUTE m4_stmt; DEALLOCATE PREPARE m4_stmt;
SET @m4_skipped = CONCAT(@m4_skipped, IF(@m4_exists=1, '', CONCAT(@m4_s, '.', @m4_t, '', '; ')));
SET @m4_t = '角色_敏捷成長'; SET @m4_p = 'SELECT'; SET @m4_s = @m4_static;
SET @m4_exists = (SELECT COUNT(*) FROM information_schema.TABLES WHERE TABLE_SCHEMA = CONVERT(@m4_s USING utf8) AND TABLE_NAME = CONVERT(@m4_t USING utf8));
SET @m4_do = (@m4_guard='PASS' AND @m4_exists=1 AND (@m4_p<>'SELECT' OR @m4_grant_mode='table' OR @m4_s<>@m4_static));
SET @m4_sql = IF(@m4_do, CONCAT('GRANT ', @m4_p, ' ON `', @m4_s, '`.`', REPLACE(@m4_t,'`','``'), '` TO ', @m4_grantee), 'DO 0');
PREPARE m4_stmt FROM @m4_sql; EXECUTE m4_stmt; DEALLOCATE PREPARE m4_stmt;
SET @m4_skipped = CONCAT(@m4_skipped, IF(@m4_exists=1, '', CONCAT(@m4_s, '.', @m4_t, '', '; ')));
SET @m4_t = '角色_智力成長'; SET @m4_p = 'SELECT'; SET @m4_s = @m4_static;
SET @m4_exists = (SELECT COUNT(*) FROM information_schema.TABLES WHERE TABLE_SCHEMA = CONVERT(@m4_s USING utf8) AND TABLE_NAME = CONVERT(@m4_t USING utf8));
SET @m4_do = (@m4_guard='PASS' AND @m4_exists=1 AND (@m4_p<>'SELECT' OR @m4_grant_mode='table' OR @m4_s<>@m4_static));
SET @m4_sql = IF(@m4_do, CONCAT('GRANT ', @m4_p, ' ON `', @m4_s, '`.`', REPLACE(@m4_t,'`','``'), '` TO ', @m4_grantee), 'DO 0');
PREPARE m4_stmt FROM @m4_sql; EXECUTE m4_stmt; DEALLOCATE PREPARE m4_stmt;
SET @m4_skipped = CONCAT(@m4_skipped, IF(@m4_exists=1, '', CONCAT(@m4_s, '.', @m4_t, '', '; ')));
SET @m4_t = '角色_精神成長'; SET @m4_p = 'SELECT'; SET @m4_s = @m4_static;
SET @m4_exists = (SELECT COUNT(*) FROM information_schema.TABLES WHERE TABLE_SCHEMA = CONVERT(@m4_s USING utf8) AND TABLE_NAME = CONVERT(@m4_t USING utf8));
SET @m4_do = (@m4_guard='PASS' AND @m4_exists=1 AND (@m4_p<>'SELECT' OR @m4_grant_mode='table' OR @m4_s<>@m4_static));
SET @m4_sql = IF(@m4_do, CONCAT('GRANT ', @m4_p, ' ON `', @m4_s, '`.`', REPLACE(@m4_t,'`','``'), '` TO ', @m4_grantee), 'DO 0');
PREPARE m4_stmt FROM @m4_sql; EXECUTE m4_stmt; DEALLOCATE PREPARE m4_stmt;
SET @m4_skipped = CONCAT(@m4_skipped, IF(@m4_exists=1, '', CONCAT(@m4_s, '.', @m4_t, '', '; ')));
SET @m4_t = '角色_體質成長'; SET @m4_p = 'SELECT'; SET @m4_s = @m4_static;
SET @m4_exists = (SELECT COUNT(*) FROM information_schema.TABLES WHERE TABLE_SCHEMA = CONVERT(@m4_s USING utf8) AND TABLE_NAME = CONVERT(@m4_t USING utf8));
SET @m4_do = (@m4_guard='PASS' AND @m4_exists=1 AND (@m4_p<>'SELECT' OR @m4_grant_mode='table' OR @m4_s<>@m4_static));
SET @m4_sql = IF(@m4_do, CONCAT('GRANT ', @m4_p, ' ON `', @m4_s, '`.`', REPLACE(@m4_t,'`','``'), '` TO ', @m4_grantee), 'DO 0');
PREPARE m4_stmt FROM @m4_sql; EXECUTE m4_stmt; DEALLOCATE PREPARE m4_stmt;
SET @m4_skipped = CONCAT(@m4_skipped, IF(@m4_exists=1, '', CONCAT(@m4_s, '.', @m4_t, '', '; ')));
SET @m4_t = '道具_物品詳細說明'; SET @m4_p = 'SELECT'; SET @m4_s = @m4_static;
SET @m4_exists = (SELECT COUNT(*) FROM information_schema.TABLES WHERE TABLE_SCHEMA = CONVERT(@m4_s USING utf8) AND TABLE_NAME = CONVERT(@m4_t USING utf8));
SET @m4_do = (@m4_guard='PASS' AND @m4_exists=1 AND (@m4_p<>'SELECT' OR @m4_grant_mode='table' OR @m4_s<>@m4_static));
SET @m4_sql = IF(@m4_do, CONCAT('GRANT ', @m4_p, ' ON `', @m4_s, '`.`', REPLACE(@m4_t,'`','``'), '` TO ', @m4_grantee), 'DO 0');
PREPARE m4_stmt FROM @m4_sql; EXECUTE m4_stmt; DEALLOCATE PREPARE m4_stmt;
SET @m4_skipped = CONCAT(@m4_skipped, IF(@m4_exists=1, '', CONCAT(@m4_s, '.', @m4_t, '(選用)', '; ')));
SET @m4_t = '道具_自訂穿脫能力'; SET @m4_p = 'SELECT'; SET @m4_s = @m4_static;
SET @m4_exists = (SELECT COUNT(*) FROM information_schema.TABLES WHERE TABLE_SCHEMA = CONVERT(@m4_s USING utf8) AND TABLE_NAME = CONVERT(@m4_t USING utf8));
SET @m4_do = (@m4_guard='PASS' AND @m4_exists=1 AND (@m4_p<>'SELECT' OR @m4_grant_mode='table' OR @m4_s<>@m4_static));
SET @m4_sql = IF(@m4_do, CONCAT('GRANT ', @m4_p, ' ON `', @m4_s, '`.`', REPLACE(@m4_t,'`','``'), '` TO ', @m4_grantee), 'DO 0');
PREPARE m4_stmt FROM @m4_sql; EXECUTE m4_stmt; DEALLOCATE PREPARE m4_stmt;
SET @m4_skipped = CONCAT(@m4_skipped, IF(@m4_exists=1, '', CONCAT(@m4_s, '.', @m4_t, '', '; ')));
SET @m4_t = '道具_變形控制魔杖'; SET @m4_p = 'SELECT'; SET @m4_s = @m4_static;
SET @m4_exists = (SELECT COUNT(*) FROM information_schema.TABLES WHERE TABLE_SCHEMA = CONVERT(@m4_s USING utf8) AND TABLE_NAME = CONVERT(@m4_t USING utf8));
SET @m4_do = (@m4_guard='PASS' AND @m4_exists=1 AND (@m4_p<>'SELECT' OR @m4_grant_mode='table' OR @m4_s<>@m4_static));
SET @m4_sql = IF(@m4_do, CONCAT('GRANT ', @m4_p, ' ON `', @m4_s, '`.`', REPLACE(@m4_t,'`','``'), '` TO ', @m4_grantee), 'DO 0');
PREPARE m4_stmt FROM @m4_sql; EXECUTE m4_stmt; DEALLOCATE PREPARE m4_stmt;
SET @m4_skipped = CONCAT(@m4_skipped, IF(@m4_exists=1, '', CONCAT(@m4_s, '.', @m4_t, '', '; ')));
SET @m4_t = 'commands'; SET @m4_p = 'SELECT'; SET @m4_s = @m4_lito;
SET @m4_exists = (SELECT COUNT(*) FROM information_schema.TABLES WHERE TABLE_SCHEMA = CONVERT(@m4_s USING utf8) AND TABLE_NAME = CONVERT(@m4_t USING utf8));
SET @m4_do = (@m4_guard='PASS' AND @m4_exists=1 AND (@m4_p<>'SELECT' OR @m4_grant_mode='table' OR @m4_s<>@m4_static));
SET @m4_sql = IF(@m4_do, CONCAT('GRANT ', @m4_p, ' ON `', @m4_s, '`.`', REPLACE(@m4_t,'`','``'), '` TO ', @m4_grantee), 'DO 0');
PREPARE m4_stmt FROM @m4_sql; EXECUTE m4_stmt; DEALLOCATE PREPARE m4_stmt;
SET @m4_skipped = CONCAT(@m4_skipped, IF(@m4_exists=1, '', CONCAT(@m4_s, '.', @m4_t, '', '; ')));
SET @m4_t = 'lito_battle_object'; SET @m4_p = 'SELECT'; SET @m4_s = @m4_lito;
SET @m4_exists = (SELECT COUNT(*) FROM information_schema.TABLES WHERE TABLE_SCHEMA = CONVERT(@m4_s USING utf8) AND TABLE_NAME = CONVERT(@m4_t USING utf8));
SET @m4_do = (@m4_guard='PASS' AND @m4_exists=1 AND (@m4_p<>'SELECT' OR @m4_grant_mode='table' OR @m4_s<>@m4_static));
SET @m4_sql = IF(@m4_do, CONCAT('GRANT ', @m4_p, ' ON `', @m4_s, '`.`', REPLACE(@m4_t,'`','``'), '` TO ', @m4_grantee), 'DO 0');
PREPARE m4_stmt FROM @m4_sql; EXECUTE m4_stmt; DEALLOCATE PREPARE m4_stmt;
SET @m4_skipped = CONCAT(@m4_skipped, IF(@m4_exists=1, '', CONCAT(@m4_s, '.', @m4_t, '', '; ')));
SET @m4_t = 'lito_battle_player_spawn'; SET @m4_p = 'SELECT'; SET @m4_s = @m4_lito;
SET @m4_exists = (SELECT COUNT(*) FROM information_schema.TABLES WHERE TABLE_SCHEMA = CONVERT(@m4_s USING utf8) AND TABLE_NAME = CONVERT(@m4_t USING utf8));
SET @m4_do = (@m4_guard='PASS' AND @m4_exists=1 AND (@m4_p<>'SELECT' OR @m4_grant_mode='table' OR @m4_s<>@m4_static));
SET @m4_sql = IF(@m4_do, CONCAT('GRANT ', @m4_p, ' ON `', @m4_s, '`.`', REPLACE(@m4_t,'`','``'), '` TO ', @m4_grantee), 'DO 0');
PREPARE m4_stmt FROM @m4_sql; EXECUTE m4_stmt; DEALLOCATE PREPARE m4_stmt;
SET @m4_skipped = CONCAT(@m4_skipped, IF(@m4_exists=1, '', CONCAT(@m4_s, '.', @m4_t, '', '; ')));
SET @m4_t = 'lito_battle_rule'; SET @m4_p = 'SELECT'; SET @m4_s = @m4_lito;
SET @m4_exists = (SELECT COUNT(*) FROM information_schema.TABLES WHERE TABLE_SCHEMA = CONVERT(@m4_s USING utf8) AND TABLE_NAME = CONVERT(@m4_t USING utf8));
SET @m4_do = (@m4_guard='PASS' AND @m4_exists=1 AND (@m4_p<>'SELECT' OR @m4_grant_mode='table' OR @m4_s<>@m4_static));
SET @m4_sql = IF(@m4_do, CONCAT('GRANT ', @m4_p, ' ON `', @m4_s, '`.`', REPLACE(@m4_t,'`','``'), '` TO ', @m4_grantee), 'DO 0');
PREPARE m4_stmt FROM @m4_sql; EXECUTE m4_stmt; DEALLOCATE PREPARE m4_stmt;
SET @m4_skipped = CONCAT(@m4_skipped, IF(@m4_exists=1, '', CONCAT(@m4_s, '.', @m4_t, '', '; ')));
SET @m4_t = 'lito_battle_settlement'; SET @m4_p = 'SELECT, INSERT, UPDATE'; SET @m4_s = @m4_lito;
SET @m4_exists = (SELECT COUNT(*) FROM information_schema.TABLES WHERE TABLE_SCHEMA = CONVERT(@m4_s USING utf8) AND TABLE_NAME = CONVERT(@m4_t USING utf8));
SET @m4_do = (@m4_guard='PASS' AND @m4_exists=1 AND (@m4_p<>'SELECT' OR @m4_grant_mode='table' OR @m4_s<>@m4_static));
SET @m4_sql = IF(@m4_do, CONCAT('GRANT ', @m4_p, ' ON `', @m4_s, '`.`', REPLACE(@m4_t,'`','``'), '` TO ', @m4_grantee), 'DO 0');
PREPARE m4_stmt FROM @m4_sql; EXECUTE m4_stmt; DEALLOCATE PREPARE m4_stmt;
SET @m4_skipped = CONCAT(@m4_skipped, IF(@m4_exists=1, '', CONCAT(@m4_s, '.', @m4_t, '', '; ')));
SET @m4_t = 'lito_battle_spawn'; SET @m4_p = 'SELECT'; SET @m4_s = @m4_lito;
SET @m4_exists = (SELECT COUNT(*) FROM information_schema.TABLES WHERE TABLE_SCHEMA = CONVERT(@m4_s USING utf8) AND TABLE_NAME = CONVERT(@m4_t USING utf8));
SET @m4_do = (@m4_guard='PASS' AND @m4_exists=1 AND (@m4_p<>'SELECT' OR @m4_grant_mode='table' OR @m4_s<>@m4_static));
SET @m4_sql = IF(@m4_do, CONCAT('GRANT ', @m4_p, ' ON `', @m4_s, '`.`', REPLACE(@m4_t,'`','``'), '` TO ', @m4_grantee), 'DO 0');
PREPARE m4_stmt FROM @m4_sql; EXECUTE m4_stmt; DEALLOCATE PREPARE m4_stmt;
SET @m4_skipped = CONCAT(@m4_skipped, IF(@m4_exists=1, '', CONCAT(@m4_s, '.', @m4_t, '', '; ')));
SET @m4_t = 'lito_resource_buff'; SET @m4_p = 'SELECT'; SET @m4_s = @m4_lito;
SET @m4_exists = (SELECT COUNT(*) FROM information_schema.TABLES WHERE TABLE_SCHEMA = CONVERT(@m4_s USING utf8) AND TABLE_NAME = CONVERT(@m4_t USING utf8));
SET @m4_do = (@m4_guard='PASS' AND @m4_exists=1 AND (@m4_p<>'SELECT' OR @m4_grant_mode='table' OR @m4_s<>@m4_static));
SET @m4_sql = IF(@m4_do, CONCAT('GRANT ', @m4_p, ' ON `', @m4_s, '`.`', REPLACE(@m4_t,'`','``'), '` TO ', @m4_grantee), 'DO 0');
PREPARE m4_stmt FROM @m4_sql; EXECUTE m4_stmt; DEALLOCATE PREPARE m4_stmt;
SET @m4_skipped = CONCAT(@m4_skipped, IF(@m4_exists=1, '', CONCAT(@m4_s, '.', @m4_t, '', '; ')));
SET @m4_t = 'lito_resource_map'; SET @m4_p = 'SELECT'; SET @m4_s = @m4_lito;
SET @m4_exists = (SELECT COUNT(*) FROM information_schema.TABLES WHERE TABLE_SCHEMA = CONVERT(@m4_s USING utf8) AND TABLE_NAME = CONVERT(@m4_t USING utf8));
SET @m4_do = (@m4_guard='PASS' AND @m4_exists=1 AND (@m4_p<>'SELECT' OR @m4_grant_mode='table' OR @m4_s<>@m4_static));
SET @m4_sql = IF(@m4_do, CONCAT('GRANT ', @m4_p, ' ON `', @m4_s, '`.`', REPLACE(@m4_t,'`','``'), '` TO ', @m4_grantee), 'DO 0');
PREPARE m4_stmt FROM @m4_sql; EXECUTE m4_stmt; DEALLOCATE PREPARE m4_stmt;
SET @m4_skipped = CONCAT(@m4_skipped, IF(@m4_exists=1, '', CONCAT(@m4_s, '.', @m4_t, '', '; ')));
SET @m4_t = 'lito_resource_map_page'; SET @m4_p = 'SELECT'; SET @m4_s = @m4_lito;
SET @m4_exists = (SELECT COUNT(*) FROM information_schema.TABLES WHERE TABLE_SCHEMA = CONVERT(@m4_s USING utf8) AND TABLE_NAME = CONVERT(@m4_t USING utf8));
SET @m4_do = (@m4_guard='PASS' AND @m4_exists=1 AND (@m4_p<>'SELECT' OR @m4_grant_mode='table' OR @m4_s<>@m4_static));
SET @m4_sql = IF(@m4_do, CONCAT('GRANT ', @m4_p, ' ON `', @m4_s, '`.`', REPLACE(@m4_t,'`','``'), '` TO ', @m4_grantee), 'DO 0');
PREPARE m4_stmt FROM @m4_sql; EXECUTE m4_stmt; DEALLOCATE PREPARE m4_stmt;
SET @m4_skipped = CONCAT(@m4_skipped, IF(@m4_exists=1, '', CONCAT(@m4_s, '.', @m4_t, '', '; ')));
SET @m4_t = 'lito_resource_npc_identity'; SET @m4_p = 'SELECT'; SET @m4_s = @m4_lito;
SET @m4_exists = (SELECT COUNT(*) FROM information_schema.TABLES WHERE TABLE_SCHEMA = CONVERT(@m4_s USING utf8) AND TABLE_NAME = CONVERT(@m4_t USING utf8));
SET @m4_do = (@m4_guard='PASS' AND @m4_exists=1 AND (@m4_p<>'SELECT' OR @m4_grant_mode='table' OR @m4_s<>@m4_static));
SET @m4_sql = IF(@m4_do, CONCAT('GRANT ', @m4_p, ' ON `', @m4_s, '`.`', REPLACE(@m4_t,'`','``'), '` TO ', @m4_grantee), 'DO 0');
PREPARE m4_stmt FROM @m4_sql; EXECUTE m4_stmt; DEALLOCATE PREPARE m4_stmt;
SET @m4_skipped = CONCAT(@m4_skipped, IF(@m4_exists=1, '', CONCAT(@m4_s, '.', @m4_t, '', '; ')));
SET @m4_t = 'lito_resource_pending'; SET @m4_p = 'SELECT'; SET @m4_s = @m4_lito;
SET @m4_exists = (SELECT COUNT(*) FROM information_schema.TABLES WHERE TABLE_SCHEMA = CONVERT(@m4_s USING utf8) AND TABLE_NAME = CONVERT(@m4_t USING utf8));
SET @m4_do = (@m4_guard='PASS' AND @m4_exists=1 AND (@m4_p<>'SELECT' OR @m4_grant_mode='table' OR @m4_s<>@m4_static));
SET @m4_sql = IF(@m4_do, CONCAT('GRANT ', @m4_p, ' ON `', @m4_s, '`.`', REPLACE(@m4_t,'`','``'), '` TO ', @m4_grantee), 'DO 0');
PREPARE m4_stmt FROM @m4_sql; EXECUTE m4_stmt; DEALLOCATE PREPARE m4_stmt;
SET @m4_skipped = CONCAT(@m4_skipped, IF(@m4_exists=1, '', CONCAT(@m4_s, '.', @m4_t, '', '; ')));
SET @m4_t = 'lito_resource_portal'; SET @m4_p = 'SELECT'; SET @m4_s = @m4_lito;
SET @m4_exists = (SELECT COUNT(*) FROM information_schema.TABLES WHERE TABLE_SCHEMA = CONVERT(@m4_s USING utf8) AND TABLE_NAME = CONVERT(@m4_t USING utf8));
SET @m4_do = (@m4_guard='PASS' AND @m4_exists=1 AND (@m4_p<>'SELECT' OR @m4_grant_mode='table' OR @m4_s<>@m4_static));
SET @m4_sql = IF(@m4_do, CONCAT('GRANT ', @m4_p, ' ON `', @m4_s, '`.`', REPLACE(@m4_t,'`','``'), '` TO ', @m4_grantee), 'DO 0');
PREPARE m4_stmt FROM @m4_sql; EXECUTE m4_stmt; DEALLOCATE PREPARE m4_stmt;
SET @m4_skipped = CONCAT(@m4_skipped, IF(@m4_exists=1, '', CONCAT(@m4_s, '.', @m4_t, '', '; ')));
SET @m4_t = 'lito_resource_projectile'; SET @m4_p = 'SELECT'; SET @m4_s = @m4_lito;
SET @m4_exists = (SELECT COUNT(*) FROM information_schema.TABLES WHERE TABLE_SCHEMA = CONVERT(@m4_s USING utf8) AND TABLE_NAME = CONVERT(@m4_t USING utf8));
SET @m4_do = (@m4_guard='PASS' AND @m4_exists=1 AND (@m4_p<>'SELECT' OR @m4_grant_mode='table' OR @m4_s<>@m4_static));
SET @m4_sql = IF(@m4_do, CONCAT('GRANT ', @m4_p, ' ON `', @m4_s, '`.`', REPLACE(@m4_t,'`','``'), '` TO ', @m4_grantee), 'DO 0');
PREPARE m4_stmt FROM @m4_sql; EXECUTE m4_stmt; DEALLOCATE PREPARE m4_stmt;
SET @m4_skipped = CONCAT(@m4_skipped, IF(@m4_exists=1, '', CONCAT(@m4_s, '.', @m4_t, '', '; ')));
SET @m4_t = 'lito_resource_release'; SET @m4_p = 'SELECT'; SET @m4_s = @m4_lito;
SET @m4_exists = (SELECT COUNT(*) FROM information_schema.TABLES WHERE TABLE_SCHEMA = CONVERT(@m4_s USING utf8) AND TABLE_NAME = CONVERT(@m4_t USING utf8));
SET @m4_do = (@m4_guard='PASS' AND @m4_exists=1 AND (@m4_p<>'SELECT' OR @m4_grant_mode='table' OR @m4_s<>@m4_static));
SET @m4_sql = IF(@m4_do, CONCAT('GRANT ', @m4_p, ' ON `', @m4_s, '`.`', REPLACE(@m4_t,'`','``'), '` TO ', @m4_grantee), 'DO 0');
PREPARE m4_stmt FROM @m4_sql; EXECUTE m4_stmt; DEALLOCATE PREPARE m4_stmt;
SET @m4_skipped = CONCAT(@m4_skipped, IF(@m4_exists=1, '', CONCAT(@m4_s, '.', @m4_t, '', '; ')));
SET @m4_t = 'lito_resource_skill_value'; SET @m4_p = 'SELECT'; SET @m4_s = @m4_lito;
SET @m4_exists = (SELECT COUNT(*) FROM information_schema.TABLES WHERE TABLE_SCHEMA = CONVERT(@m4_s USING utf8) AND TABLE_NAME = CONVERT(@m4_t USING utf8));
SET @m4_do = (@m4_guard='PASS' AND @m4_exists=1 AND (@m4_p<>'SELECT' OR @m4_grant_mode='table' OR @m4_s<>@m4_static));
SET @m4_sql = IF(@m4_do, CONCAT('GRANT ', @m4_p, ' ON `', @m4_s, '`.`', REPLACE(@m4_t,'`','``'), '` TO ', @m4_grantee), 'DO 0');
PREPARE m4_stmt FROM @m4_sql; EXECUTE m4_stmt; DEALLOCATE PREPARE m4_stmt;
SET @m4_skipped = CONCAT(@m4_skipped, IF(@m4_exists=1, '', CONCAT(@m4_s, '.', @m4_t, '', '; ')));
SET @m4_t = 'lito_resource_zone'; SET @m4_p = 'SELECT'; SET @m4_s = @m4_lito;
SET @m4_exists = (SELECT COUNT(*) FROM information_schema.TABLES WHERE TABLE_SCHEMA = CONVERT(@m4_s USING utf8) AND TABLE_NAME = CONVERT(@m4_t USING utf8));
SET @m4_do = (@m4_guard='PASS' AND @m4_exists=1 AND (@m4_p<>'SELECT' OR @m4_grant_mode='table' OR @m4_s<>@m4_static));
SET @m4_sql = IF(@m4_do, CONCAT('GRANT ', @m4_p, ' ON `', @m4_s, '`.`', REPLACE(@m4_t,'`','``'), '` TO ', @m4_grantee), 'DO 0');
PREPARE m4_stmt FROM @m4_sql; EXECUTE m4_stmt; DEALLOCATE PREPARE m4_stmt;
SET @m4_skipped = CONCAT(@m4_skipped, IF(@m4_exists=1, '', CONCAT(@m4_s, '.', @m4_t, '', '; ')));
SET @m4_t = 'lito_room_policy'; SET @m4_p = 'SELECT'; SET @m4_s = @m4_lito;
SET @m4_exists = (SELECT COUNT(*) FROM information_schema.TABLES WHERE TABLE_SCHEMA = CONVERT(@m4_s USING utf8) AND TABLE_NAME = CONVERT(@m4_t USING utf8));
SET @m4_do = (@m4_guard='PASS' AND @m4_exists=1 AND (@m4_p<>'SELECT' OR @m4_grant_mode='table' OR @m4_s<>@m4_static));
SET @m4_sql = IF(@m4_do, CONCAT('GRANT ', @m4_p, ' ON `', @m4_s, '`.`', REPLACE(@m4_t,'`','``'), '` TO ', @m4_grantee), 'DO 0');
PREPARE m4_stmt FROM @m4_sql; EXECUTE m4_stmt; DEALLOCATE PREPARE m4_stmt;
SET @m4_skipped = CONCAT(@m4_skipped, IF(@m4_exists=1, '', CONCAT(@m4_s, '.', @m4_t, '', '; ')));
SET @m4_t = 'lito_source_server'; SET @m4_p = 'SELECT'; SET @m4_s = @m4_lito;
SET @m4_exists = (SELECT COUNT(*) FROM information_schema.TABLES WHERE TABLE_SCHEMA = CONVERT(@m4_s USING utf8) AND TABLE_NAME = CONVERT(@m4_t USING utf8));
SET @m4_do = (@m4_guard='PASS' AND @m4_exists=1 AND (@m4_p<>'SELECT' OR @m4_grant_mode='table' OR @m4_s<>@m4_static));
SET @m4_sql = IF(@m4_do, CONCAT('GRANT ', @m4_p, ' ON `', @m4_s, '`.`', REPLACE(@m4_t,'`','``'), '` TO ', @m4_grantee), 'DO 0');
PREPARE m4_stmt FROM @m4_sql; EXECUTE m4_stmt; DEALLOCATE PREPARE m4_stmt;
SET @m4_skipped = CONCAT(@m4_skipped, IF(@m4_exists=1, '', CONCAT(@m4_s, '.', @m4_t, '', '; ')));
SET @m4_t = 'lito_tournament_shop'; SET @m4_p = 'SELECT'; SET @m4_s = @m4_lito;
SET @m4_exists = (SELECT COUNT(*) FROM information_schema.TABLES WHERE TABLE_SCHEMA = CONVERT(@m4_s USING utf8) AND TABLE_NAME = CONVERT(@m4_t USING utf8));
SET @m4_do = (@m4_guard='PASS' AND @m4_exists=1 AND (@m4_p<>'SELECT' OR @m4_grant_mode='table' OR @m4_s<>@m4_static));
SET @m4_sql = IF(@m4_do, CONCAT('GRANT ', @m4_p, ' ON `', @m4_s, '`.`', REPLACE(@m4_t,'`','``'), '` TO ', @m4_grantee), 'DO 0');
PREPARE m4_stmt FROM @m4_sql; EXECUTE m4_stmt; DEALLOCATE PREPARE m4_stmt;
SET @m4_skipped = CONCAT(@m4_skipped, IF(@m4_exists=1, '', CONCAT(@m4_s, '.', @m4_t, '', '; ')));
SET @m4_t = 'accounts'; SET @m4_p = 'SELECT'; SET @m4_s = @m4_ac;
SET @m4_exists = (SELECT COUNT(*) FROM information_schema.TABLES WHERE TABLE_SCHEMA = CONVERT(@m4_s USING utf8) AND TABLE_NAME = CONVERT(@m4_t USING utf8));
SET @m4_do = (@m4_guard='PASS' AND @m4_exists=1 AND (@m4_p<>'SELECT' OR @m4_grant_mode='table' OR @m4_s<>@m4_static));
SET @m4_sql = IF(@m4_do, CONCAT('GRANT ', @m4_p, ' ON `', @m4_s, '`.`', REPLACE(@m4_t,'`','``'), '` TO ', @m4_grantee), 'DO 0');
PREPARE m4_stmt FROM @m4_sql; EXECUTE m4_stmt; DEALLOCATE PREPARE m4_stmt;
SET @m4_skipped = CONCAT(@m4_skipped, IF(@m4_exists=1, '', CONCAT(@m4_s, '.', @m4_t, '', '; ')));
SET @m4_t = 'character_buddys'; SET @m4_p = 'SELECT'; SET @m4_s = @m4_ac;
SET @m4_exists = (SELECT COUNT(*) FROM information_schema.TABLES WHERE TABLE_SCHEMA = CONVERT(@m4_s USING utf8) AND TABLE_NAME = CONVERT(@m4_t USING utf8));
SET @m4_do = (@m4_guard='PASS' AND @m4_exists=1 AND (@m4_p<>'SELECT' OR @m4_grant_mode='table' OR @m4_s<>@m4_static));
SET @m4_sql = IF(@m4_do, CONCAT('GRANT ', @m4_p, ' ON `', @m4_s, '`.`', REPLACE(@m4_t,'`','``'), '` TO ', @m4_grantee), 'DO 0');
PREPARE m4_stmt FROM @m4_sql; EXECUTE m4_stmt; DEALLOCATE PREPARE m4_stmt;
SET @m4_skipped = CONCAT(@m4_skipped, IF(@m4_exists=1, '', CONCAT(@m4_s, '.', @m4_t, '', '; ')));
SET @m4_t = 'character_buff'; SET @m4_p = 'SELECT'; SET @m4_s = @m4_ac;
SET @m4_exists = (SELECT COUNT(*) FROM information_schema.TABLES WHERE TABLE_SCHEMA = CONVERT(@m4_s USING utf8) AND TABLE_NAME = CONVERT(@m4_t USING utf8));
SET @m4_do = (@m4_guard='PASS' AND @m4_exists=1 AND (@m4_p<>'SELECT' OR @m4_grant_mode='table' OR @m4_s<>@m4_static));
SET @m4_sql = IF(@m4_do, CONCAT('GRANT ', @m4_p, ' ON `', @m4_s, '`.`', REPLACE(@m4_t,'`','``'), '` TO ', @m4_grantee), 'DO 0');
PREPARE m4_stmt FROM @m4_sql; EXECUTE m4_stmt; DEALLOCATE PREPARE m4_stmt;
SET @m4_skipped = CONCAT(@m4_skipped, IF(@m4_exists=1, '', CONCAT(@m4_s, '.', @m4_t, '', '; ')));
SET @m4_t = 'character_elf_warehouse'; SET @m4_p = 'SELECT'; SET @m4_s = @m4_ac;
SET @m4_exists = (SELECT COUNT(*) FROM information_schema.TABLES WHERE TABLE_SCHEMA = CONVERT(@m4_s USING utf8) AND TABLE_NAME = CONVERT(@m4_t USING utf8));
SET @m4_do = (@m4_guard='PASS' AND @m4_exists=1 AND (@m4_p<>'SELECT' OR @m4_grant_mode='table' OR @m4_s<>@m4_static));
SET @m4_sql = IF(@m4_do, CONCAT('GRANT ', @m4_p, ' ON `', @m4_s, '`.`', REPLACE(@m4_t,'`','``'), '` TO ', @m4_grantee), 'DO 0');
PREPARE m4_stmt FROM @m4_sql; EXECUTE m4_stmt; DEALLOCATE PREPARE m4_stmt;
SET @m4_skipped = CONCAT(@m4_skipped, IF(@m4_exists=1, '', CONCAT(@m4_s, '.', @m4_t, '', '; ')));
SET @m4_t = 'character_items'; SET @m4_p = 'SELECT'; SET @m4_s = @m4_ac;
SET @m4_exists = (SELECT COUNT(*) FROM information_schema.TABLES WHERE TABLE_SCHEMA = CONVERT(@m4_s USING utf8) AND TABLE_NAME = CONVERT(@m4_t USING utf8));
SET @m4_do = (@m4_guard='PASS' AND @m4_exists=1 AND (@m4_p<>'SELECT' OR @m4_grant_mode='table' OR @m4_s<>@m4_static));
SET @m4_sql = IF(@m4_do, CONCAT('GRANT ', @m4_p, ' ON `', @m4_s, '`.`', REPLACE(@m4_t,'`','``'), '` TO ', @m4_grantee), 'DO 0');
PREPARE m4_stmt FROM @m4_sql; EXECUTE m4_stmt; DEALLOCATE PREPARE m4_stmt;
SET @m4_skipped = CONCAT(@m4_skipped, IF(@m4_exists=1, '', CONCAT(@m4_s, '.', @m4_t, '', '; ')));
SET @m4_t = 'character_items_card'; SET @m4_p = 'SELECT'; SET @m4_s = @m4_ac;
SET @m4_exists = (SELECT COUNT(*) FROM information_schema.TABLES WHERE TABLE_SCHEMA = CONVERT(@m4_s USING utf8) AND TABLE_NAME = CONVERT(@m4_t USING utf8));
SET @m4_do = (@m4_guard='PASS' AND @m4_exists=1 AND (@m4_p<>'SELECT' OR @m4_grant_mode='table' OR @m4_s<>@m4_static));
SET @m4_sql = IF(@m4_do, CONCAT('GRANT ', @m4_p, ' ON `', @m4_s, '`.`', REPLACE(@m4_t,'`','``'), '` TO ', @m4_grantee), 'DO 0');
PREPARE m4_stmt FROM @m4_sql; EXECUTE m4_stmt; DEALLOCATE PREPARE m4_stmt;
SET @m4_skipped = CONCAT(@m4_skipped, IF(@m4_exists=1, '', CONCAT(@m4_s, '.', @m4_t, '', '; ')));
SET @m4_t = 'character_items_dark'; SET @m4_p = 'SELECT'; SET @m4_s = @m4_ac;
SET @m4_exists = (SELECT COUNT(*) FROM information_schema.TABLES WHERE TABLE_SCHEMA = CONVERT(@m4_s USING utf8) AND TABLE_NAME = CONVERT(@m4_t USING utf8));
SET @m4_do = (@m4_guard='PASS' AND @m4_exists=1 AND (@m4_p<>'SELECT' OR @m4_grant_mode='table' OR @m4_s<>@m4_static));
SET @m4_sql = IF(@m4_do, CONCAT('GRANT ', @m4_p, ' ON `', @m4_s, '`.`', REPLACE(@m4_t,'`','``'), '` TO ', @m4_grantee), 'DO 0');
PREPARE m4_stmt FROM @m4_sql; EXECUTE m4_stmt; DEALLOCATE PREPARE m4_stmt;
SET @m4_skipped = CONCAT(@m4_skipped, IF(@m4_exists=1, '', CONCAT(@m4_s, '.', @m4_t, '', '; ')));
SET @m4_t = 'character_items_time'; SET @m4_p = 'SELECT'; SET @m4_s = @m4_ac;
SET @m4_exists = (SELECT COUNT(*) FROM information_schema.TABLES WHERE TABLE_SCHEMA = CONVERT(@m4_s USING utf8) AND TABLE_NAME = CONVERT(@m4_t USING utf8));
SET @m4_do = (@m4_guard='PASS' AND @m4_exists=1 AND (@m4_p<>'SELECT' OR @m4_grant_mode='table' OR @m4_s<>@m4_static));
SET @m4_sql = IF(@m4_do, CONCAT('GRANT ', @m4_p, ' ON `', @m4_s, '`.`', REPLACE(@m4_t,'`','``'), '` TO ', @m4_grantee), 'DO 0');
PREPARE m4_stmt FROM @m4_sql; EXECUTE m4_stmt; DEALLOCATE PREPARE m4_stmt;
SET @m4_skipped = CONCAT(@m4_skipped, IF(@m4_exists=1, '', CONCAT(@m4_s, '.', @m4_t, '', '; ')));
SET @m4_t = 'character_letter'; SET @m4_p = 'SELECT'; SET @m4_s = @m4_ac;
SET @m4_exists = (SELECT COUNT(*) FROM information_schema.TABLES WHERE TABLE_SCHEMA = CONVERT(@m4_s USING utf8) AND TABLE_NAME = CONVERT(@m4_t USING utf8));
SET @m4_do = (@m4_guard='PASS' AND @m4_exists=1 AND (@m4_p<>'SELECT' OR @m4_grant_mode='table' OR @m4_s<>@m4_static));
SET @m4_sql = IF(@m4_do, CONCAT('GRANT ', @m4_p, ' ON `', @m4_s, '`.`', REPLACE(@m4_t,'`','``'), '` TO ', @m4_grantee), 'DO 0');
PREPARE m4_stmt FROM @m4_sql; EXECUTE m4_stmt; DEALLOCATE PREPARE m4_stmt;
SET @m4_skipped = CONCAT(@m4_skipped, IF(@m4_exists=1, '', CONCAT(@m4_s, '.', @m4_t, '', '; ')));
SET @m4_t = 'character_lito'; SET @m4_p = 'SELECT, INSERT, UPDATE'; SET @m4_s = @m4_ac;
SET @m4_exists = (SELECT COUNT(*) FROM information_schema.TABLES WHERE TABLE_SCHEMA = CONVERT(@m4_s USING utf8) AND TABLE_NAME = CONVERT(@m4_t USING utf8));
SET @m4_do = (@m4_guard='PASS' AND @m4_exists=1 AND (@m4_p<>'SELECT' OR @m4_grant_mode='table' OR @m4_s<>@m4_static));
SET @m4_sql = IF(@m4_do, CONCAT('GRANT ', @m4_p, ' ON `', @m4_s, '`.`', REPLACE(@m4_t,'`','``'), '` TO ', @m4_grantee), 'DO 0');
PREPARE m4_stmt FROM @m4_sql; EXECUTE m4_stmt; DEALLOCATE PREPARE m4_stmt;
SET @m4_skipped = CONCAT(@m4_skipped, IF(@m4_exists=1, '', CONCAT(@m4_s, '.', @m4_t, '', '; ')));
SET @m4_t = 'character_other'; SET @m4_p = 'SELECT'; SET @m4_s = @m4_ac;
SET @m4_exists = (SELECT COUNT(*) FROM information_schema.TABLES WHERE TABLE_SCHEMA = CONVERT(@m4_s USING utf8) AND TABLE_NAME = CONVERT(@m4_t USING utf8));
SET @m4_do = (@m4_guard='PASS' AND @m4_exists=1 AND (@m4_p<>'SELECT' OR @m4_grant_mode='table' OR @m4_s<>@m4_static));
SET @m4_sql = IF(@m4_do, CONCAT('GRANT ', @m4_p, ' ON `', @m4_s, '`.`', REPLACE(@m4_t,'`','``'), '` TO ', @m4_grantee), 'DO 0');
PREPARE m4_stmt FROM @m4_sql; EXECUTE m4_stmt; DEALLOCATE PREPARE m4_stmt;
SET @m4_skipped = CONCAT(@m4_skipped, IF(@m4_exists=1, '', CONCAT(@m4_s, '.', @m4_t, '', '; ')));
SET @m4_t = 'character_pets'; SET @m4_p = 'SELECT'; SET @m4_s = @m4_ac;
SET @m4_exists = (SELECT COUNT(*) FROM information_schema.TABLES WHERE TABLE_SCHEMA = CONVERT(@m4_s USING utf8) AND TABLE_NAME = CONVERT(@m4_t USING utf8));
SET @m4_do = (@m4_guard='PASS' AND @m4_exists=1 AND (@m4_p<>'SELECT' OR @m4_grant_mode='table' OR @m4_s<>@m4_static));
SET @m4_sql = IF(@m4_do, CONCAT('GRANT ', @m4_p, ' ON `', @m4_s, '`.`', REPLACE(@m4_t,'`','``'), '` TO ', @m4_grantee), 'DO 0');
PREPARE m4_stmt FROM @m4_sql; EXECUTE m4_stmt; DEALLOCATE PREPARE m4_stmt;
SET @m4_skipped = CONCAT(@m4_skipped, IF(@m4_exists=1, '', CONCAT(@m4_s, '.', @m4_t, '', '; ')));
SET @m4_t = 'character_quests'; SET @m4_p = 'SELECT'; SET @m4_s = @m4_ac;
SET @m4_exists = (SELECT COUNT(*) FROM information_schema.TABLES WHERE TABLE_SCHEMA = CONVERT(@m4_s USING utf8) AND TABLE_NAME = CONVERT(@m4_t USING utf8));
SET @m4_do = (@m4_guard='PASS' AND @m4_exists=1 AND (@m4_p<>'SELECT' OR @m4_grant_mode='table' OR @m4_s<>@m4_static));
SET @m4_sql = IF(@m4_do, CONCAT('GRANT ', @m4_p, ' ON `', @m4_s, '`.`', REPLACE(@m4_t,'`','``'), '` TO ', @m4_grantee), 'DO 0');
PREPARE m4_stmt FROM @m4_sql; EXECUTE m4_stmt; DEALLOCATE PREPARE m4_stmt;
SET @m4_skipped = CONCAT(@m4_skipped, IF(@m4_exists=1, '', CONCAT(@m4_s, '.', @m4_t, '', '; ')));
SET @m4_t = 'character_skills'; SET @m4_p = 'SELECT'; SET @m4_s = @m4_ac;
SET @m4_exists = (SELECT COUNT(*) FROM information_schema.TABLES WHERE TABLE_SCHEMA = CONVERT(@m4_s USING utf8) AND TABLE_NAME = CONVERT(@m4_t USING utf8));
SET @m4_do = (@m4_guard='PASS' AND @m4_exists=1 AND (@m4_p<>'SELECT' OR @m4_grant_mode='table' OR @m4_s<>@m4_static));
SET @m4_sql = IF(@m4_do, CONCAT('GRANT ', @m4_p, ' ON `', @m4_s, '`.`', REPLACE(@m4_t,'`','``'), '` TO ', @m4_grantee), 'DO 0');
PREPARE m4_stmt FROM @m4_sql; EXECUTE m4_stmt; DEALLOCATE PREPARE m4_stmt;
SET @m4_skipped = CONCAT(@m4_skipped, IF(@m4_exists=1, '', CONCAT(@m4_s, '.', @m4_t, '', '; ')));
SET @m4_t = 'character_teleport'; SET @m4_p = 'SELECT'; SET @m4_s = @m4_ac;
SET @m4_exists = (SELECT COUNT(*) FROM information_schema.TABLES WHERE TABLE_SCHEMA = CONVERT(@m4_s USING utf8) AND TABLE_NAME = CONVERT(@m4_t USING utf8));
SET @m4_do = (@m4_guard='PASS' AND @m4_exists=1 AND (@m4_p<>'SELECT' OR @m4_grant_mode='table' OR @m4_s<>@m4_static));
SET @m4_sql = IF(@m4_do, CONCAT('GRANT ', @m4_p, ' ON `', @m4_s, '`.`', REPLACE(@m4_t,'`','``'), '` TO ', @m4_grantee), 'DO 0');
PREPARE m4_stmt FROM @m4_sql; EXECUTE m4_stmt; DEALLOCATE PREPARE m4_stmt;
SET @m4_skipped = CONCAT(@m4_skipped, IF(@m4_exists=1, '', CONCAT(@m4_s, '.', @m4_t, '', '; ')));
SET @m4_t = 'character_warehouse'; SET @m4_p = 'SELECT'; SET @m4_s = @m4_ac;
SET @m4_exists = (SELECT COUNT(*) FROM information_schema.TABLES WHERE TABLE_SCHEMA = CONVERT(@m4_s USING utf8) AND TABLE_NAME = CONVERT(@m4_t USING utf8));
SET @m4_do = (@m4_guard='PASS' AND @m4_exists=1 AND (@m4_p<>'SELECT' OR @m4_grant_mode='table' OR @m4_s<>@m4_static));
SET @m4_sql = IF(@m4_do, CONCAT('GRANT ', @m4_p, ' ON `', @m4_s, '`.`', REPLACE(@m4_t,'`','``'), '` TO ', @m4_grantee), 'DO 0');
PREPARE m4_stmt FROM @m4_sql; EXECUTE m4_stmt; DEALLOCATE PREPARE m4_stmt;
SET @m4_skipped = CONCAT(@m4_skipped, IF(@m4_exists=1, '', CONCAT(@m4_s, '.', @m4_t, '', '; ')));
SET @m4_t = 'characters'; SET @m4_p = 'SELECT'; SET @m4_s = @m4_ac;
SET @m4_exists = (SELECT COUNT(*) FROM information_schema.TABLES WHERE TABLE_SCHEMA = CONVERT(@m4_s USING utf8) AND TABLE_NAME = CONVERT(@m4_t USING utf8));
SET @m4_do = (@m4_guard='PASS' AND @m4_exists=1 AND (@m4_p<>'SELECT' OR @m4_grant_mode='table' OR @m4_s<>@m4_static));
SET @m4_sql = IF(@m4_do, CONCAT('GRANT ', @m4_p, ' ON `', @m4_s, '`.`', REPLACE(@m4_t,'`','``'), '` TO ', @m4_grantee), 'DO 0');
PREPARE m4_stmt FROM @m4_sql; EXECUTE m4_stmt; DEALLOCATE PREPARE m4_stmt;
SET @m4_skipped = CONCAT(@m4_skipped, IF(@m4_exists=1, '', CONCAT(@m4_s, '.', @m4_t, '', '; ')));
SET @m4_t = 'clan_data'; SET @m4_p = 'SELECT'; SET @m4_s = @m4_ac;
SET @m4_exists = (SELECT COUNT(*) FROM information_schema.TABLES WHERE TABLE_SCHEMA = CONVERT(@m4_s USING utf8) AND TABLE_NAME = CONVERT(@m4_t USING utf8));
SET @m4_do = (@m4_guard='PASS' AND @m4_exists=1 AND (@m4_p<>'SELECT' OR @m4_grant_mode='table' OR @m4_s<>@m4_static));
SET @m4_sql = IF(@m4_do, CONCAT('GRANT ', @m4_p, ' ON `', @m4_s, '`.`', REPLACE(@m4_t,'`','``'), '` TO ', @m4_grantee), 'DO 0');
PREPARE m4_stmt FROM @m4_sql; EXECUTE m4_stmt; DEALLOCATE PREPARE m4_stmt;
SET @m4_skipped = CONCAT(@m4_skipped, IF(@m4_exists=1, '', CONCAT(@m4_s, '.', @m4_t, '', '; ')));
SET @m4_t = 'clan_emblem'; SET @m4_p = 'SELECT'; SET @m4_s = @m4_ac;
SET @m4_exists = (SELECT COUNT(*) FROM information_schema.TABLES WHERE TABLE_SCHEMA = CONVERT(@m4_s USING utf8) AND TABLE_NAME = CONVERT(@m4_t USING utf8));
SET @m4_do = (@m4_guard='PASS' AND @m4_exists=1 AND (@m4_p<>'SELECT' OR @m4_grant_mode='table' OR @m4_s<>@m4_static));
SET @m4_sql = IF(@m4_do, CONCAT('GRANT ', @m4_p, ' ON `', @m4_s, '`.`', REPLACE(@m4_t,'`','``'), '` TO ', @m4_grantee), 'DO 0');
PREPARE m4_stmt FROM @m4_sql; EXECUTE m4_stmt; DEALLOCATE PREPARE m4_stmt;
SET @m4_skipped = CONCAT(@m4_skipped, IF(@m4_exists=1, '', CONCAT(@m4_s, '.', @m4_t, '', '; ')));
SET @m4_t = 'clan_warehouse'; SET @m4_p = 'SELECT'; SET @m4_s = @m4_ac;
SET @m4_exists = (SELECT COUNT(*) FROM information_schema.TABLES WHERE TABLE_SCHEMA = CONVERT(@m4_s USING utf8) AND TABLE_NAME = CONVERT(@m4_t USING utf8));
SET @m4_do = (@m4_guard='PASS' AND @m4_exists=1 AND (@m4_p<>'SELECT' OR @m4_grant_mode='table' OR @m4_s<>@m4_static));
SET @m4_sql = IF(@m4_do, CONCAT('GRANT ', @m4_p, ' ON `', @m4_s, '`.`', REPLACE(@m4_t,'`','``'), '` TO ', @m4_grantee), 'DO 0');
PREPARE m4_stmt FROM @m4_sql; EXECUTE m4_stmt; DEALLOCATE PREPARE m4_stmt;
SET @m4_skipped = CONCAT(@m4_skipped, IF(@m4_exists=1, '', CONCAT(@m4_s, '.', @m4_t, '', '; ')));
SET @m4_t = 'server_城堡'; SET @m4_p = 'SELECT'; SET @m4_s = @m4_ac;
SET @m4_exists = (SELECT COUNT(*) FROM information_schema.TABLES WHERE TABLE_SCHEMA = CONVERT(@m4_s USING utf8) AND TABLE_NAME = CONVERT(@m4_t USING utf8));
SET @m4_do = (@m4_guard='PASS' AND @m4_exists=1 AND (@m4_p<>'SELECT' OR @m4_grant_mode='table' OR @m4_s<>@m4_static));
SET @m4_sql = IF(@m4_do, CONCAT('GRANT ', @m4_p, ' ON `', @m4_s, '`.`', REPLACE(@m4_t,'`','``'), '` TO ', @m4_grantee), 'DO 0');
PREPARE m4_stmt FROM @m4_sql; EXECUTE m4_stmt; DEALLOCATE PREPARE m4_stmt;
SET @m4_skipped = CONCAT(@m4_skipped, IF(@m4_exists=1, '', CONCAT(@m4_s, '.', @m4_t, '', '; ')));
SET @m4_t = 'server_盟屋_佈告'; SET @m4_p = 'SELECT'; SET @m4_s = @m4_ac;
SET @m4_exists = (SELECT COUNT(*) FROM information_schema.TABLES WHERE TABLE_SCHEMA = CONVERT(@m4_s USING utf8) AND TABLE_NAME = CONVERT(@m4_t USING utf8));
SET @m4_do = (@m4_guard='PASS' AND @m4_exists=1 AND (@m4_p<>'SELECT' OR @m4_grant_mode='table' OR @m4_s<>@m4_static));
SET @m4_sql = IF(@m4_do, CONCAT('GRANT ', @m4_p, ' ON `', @m4_s, '`.`', REPLACE(@m4_t,'`','``'), '` TO ', @m4_grantee), 'DO 0');
PREPARE m4_stmt FROM @m4_sql; EXECUTE m4_stmt; DEALLOCATE PREPARE m4_stmt;
SET @m4_skipped = CONCAT(@m4_skipped, IF(@m4_exists=1, '', CONCAT(@m4_s, '.', @m4_t, '', '; ')));
SET @m4_t = 'server_盟屋_拍賣'; SET @m4_p = 'SELECT'; SET @m4_s = @m4_ac;
SET @m4_exists = (SELECT COUNT(*) FROM information_schema.TABLES WHERE TABLE_SCHEMA = CONVERT(@m4_s USING utf8) AND TABLE_NAME = CONVERT(@m4_t USING utf8));
SET @m4_do = (@m4_guard='PASS' AND @m4_exists=1 AND (@m4_p<>'SELECT' OR @m4_grant_mode='table' OR @m4_s<>@m4_static));
SET @m4_sql = IF(@m4_do, CONCAT('GRANT ', @m4_p, ' ON `', @m4_s, '`.`', REPLACE(@m4_t,'`','``'), '` TO ', @m4_grantee), 'DO 0');
PREPARE m4_stmt FROM @m4_sql; EXECUTE m4_stmt; DEALLOCATE PREPARE m4_stmt;
SET @m4_skipped = CONCAT(@m4_skipped, IF(@m4_exists=1, '', CONCAT(@m4_s, '.', @m4_t, '', '; ')));
SET @m4_t = 'server_盟屋_紀錄'; SET @m4_p = 'SELECT'; SET @m4_s = @m4_ac;
SET @m4_exists = (SELECT COUNT(*) FROM information_schema.TABLES WHERE TABLE_SCHEMA = CONVERT(@m4_s USING utf8) AND TABLE_NAME = CONVERT(@m4_t USING utf8));
SET @m4_do = (@m4_guard='PASS' AND @m4_exists=1 AND (@m4_p<>'SELECT' OR @m4_grant_mode='table' OR @m4_s<>@m4_static));
SET @m4_sql = IF(@m4_do, CONCAT('GRANT ', @m4_p, ' ON `', @m4_s, '`.`', REPLACE(@m4_t,'`','``'), '` TO ', @m4_grantee), 'DO 0');
PREPARE m4_stmt FROM @m4_sql; EXECUTE m4_stmt; DEALLOCATE PREPARE m4_stmt;
SET @m4_skipped = CONCAT(@m4_skipped, IF(@m4_exists=1, '', CONCAT(@m4_s, '.', @m4_t, '', '; ')));
SET @m4_t = 'server_紀錄_獎勵'; SET @m4_p = 'SELECT'; SET @m4_s = @m4_ac;
SET @m4_exists = (SELECT COUNT(*) FROM information_schema.TABLES WHERE TABLE_SCHEMA = CONVERT(@m4_s USING utf8) AND TABLE_NAME = CONVERT(@m4_t USING utf8));
SET @m4_do = (@m4_guard='PASS' AND @m4_exists=1 AND (@m4_p<>'SELECT' OR @m4_grant_mode='table' OR @m4_s<>@m4_static));
SET @m4_sql = IF(@m4_do, CONCAT('GRANT ', @m4_p, ' ON `', @m4_s, '`.`', REPLACE(@m4_t,'`','``'), '` TO ', @m4_grantee), 'DO 0');
PREPARE m4_stmt FROM @m4_sql; EXECUTE m4_stmt; DEALLOCATE PREPARE m4_stmt;
SET @m4_skipped = CONCAT(@m4_skipped, IF(@m4_exists=1, '', CONCAT(@m4_s, '.', @m4_t, '(選用)', '; ')));
-- END GENERATED:grants

-- ===== 結果 =====
SELECT 'P4' AS step, @m4_grant_mode AS grant_mode, @m4_grantee AS grantee,
       IF(@m4_skipped = '', '（無）', @m4_skipped) AS tables_skipped_because_missing;
SELECT 'M4-PREPARE: DONE' AS result, @m4_static AS static_schema, @m4_lito AS lito_schema, @m4_ac AS character_schema,
       '接著執行 m4-db-verify.sql，以它的 verdict 為準' AS next_step;
