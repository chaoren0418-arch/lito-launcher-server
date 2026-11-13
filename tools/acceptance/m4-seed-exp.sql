-- M4 驗收資料前置（T128／Issue #379）：對應 docs/acceptance/M4-RUNBOOK.md「資料前置」D1–D14。
-- GM 帳號、八角色翻頁、龍騎士／幻術師、技能、同血盟、魔法娃娃／藍水／GM 魔杖、GM 指令列；可選的第 3、第 4 個帳號。
-- **只寫入名稱以 _lito_exp 結尾的實驗 schema**（角色庫 accounts／characters／character_skills／character_items，控制庫 commands）。
-- 只新增缺少的列：不 UPDATE、不 DELETE、不改既有帳號的密碼或權限；可重複執行。
-- 本檔不含任何帳號或密碼。帳密由 Codex 寫在 git 忽略的私密檔（範本 m4-seed-vars.example.sql），把私密檔接在本檔前面一起送進 mysql：
--   cmd /c "type scratch\m4\private\m4-vars.sql tools\acceptance\m4-seed-exp.sql | mysql --defaults-extra-file=<私密 cnf> --protocol=TCP --host=127.0.0.1 --port=43324 --default-character-set=utf8mb4 --table > <run>\evidence\db-seed.txt"
-- 佔位符（__M4_…__）沒有被代換時守門會中止（錯誤訊息是 Unknown database 'm4_guard__<原因>'）。不要加 --force。
-- 本檔在 #379 沒有對 P24Lab 執行過（只在 CI 的拋棄式 MySQL 8.0 跑過）。每一列的判斷依據（類別與行號）見 tools/acceptance/README.md「資料前置」。
SET NAMES utf8mb4;

-- ===== 參數 =====
SET @m4_static        = IFNULL(@m4_static, 'p24_380db_lito_exp');
SET @m4_lito          = IFNULL(@m4_lito,   'p24_lito_lito_exp');       -- 改用 380_lito.sql 快照時是 'p24_380lito_lito_exp'
SET @m4_ac            = IFNULL(@m4_ac,     'p24_accounts_lito_exp');
SET @m4_expected_uuid = IFNULL(@m4_expected_uuid, 'e3443e40-d090-11f1-8040-4cedfb679789');  -- 設成 '' 表示不檢查
-- 私密值：由 m4-vars.sql 預先 SET；這裡只放佔位符。
SET @m4_gm_login    = IFNULL(@m4_gm_login,    '__M4_GM_LOGIN__');     -- 新建的實驗 GM 帳號（access_level=200）
SET @m4_gm_password = IFNULL(@m4_gm_password, '__M4_GM_PASSWORD__');
SET @m4_a_login     = IFNULL(@m4_a_login,     '__M4_A_LOGIN__');      -- 帳號 A：#83 的普通測試帳號 A（已存在時只需要帳號名稱）
SET @m4_a_password  = IFNULL(@m4_a_password,  '__M4_A_PASSWORD__');   -- 只在帳號 A 不存在、需要新建時使用
SET @m4_b_login     = IFNULL(@m4_b_login,     '__M4_B_LOGIN__');      -- 帳號 B：#83 的普通測試帳號 B
SET @m4_b_password  = IFNULL(@m4_b_password,  '__M4_B_PASSWORD__');
SET @m4_c_login     = IFNULL(@m4_c_login,     '__M4_C_LOGIN__');      -- 可選（D13）：第 3 個帳號；維持佔位符＝不建立
SET @m4_c_password  = IFNULL(@m4_c_password,  '__M4_C_PASSWORD__');
SET @m4_d_login     = IFNULL(@m4_d_login,     '__M4_D_LOGIN__');      -- 可選（D13）：第 4 個帳號；維持佔位符＝不建立
SET @m4_d_password  = IFNULL(@m4_d_password,  '__M4_D_PASSWORD__');

-- ===== 守門 =====
SET @m4_guard = 'FAIL';
SELECT 'S0' AS step, @@port AS port, @@server_uuid AS server_uuid, @@bind_address AS bind_address, VERSION() AS version, CURRENT_USER() AS connected_as;
SET @m4_sql = IF(@@port = 43324, 'DO 0', 'SELECT * FROM `m4_guard__port_is_not_43324_not_p24lab`.`guard`');
PREPARE m4_stmt FROM @m4_sql; EXECUTE m4_stmt; DEALLOCATE PREPARE m4_stmt;
SET @m4_sql = IF(@m4_static REGEXP '^[A-Za-z0-9_]+_lito_exp$' AND @m4_lito REGEXP '^[A-Za-z0-9_]+_lito_exp$'
                 AND @m4_ac REGEXP '^[A-Za-z0-9_]+_lito_exp$' AND @m4_static <> @m4_lito AND @m4_static <> @m4_ac AND @m4_lito <> @m4_ac,
                 'DO 0', 'SELECT * FROM `m4_guard__schemas_must_be_distinct_and_end_with_lito_exp`.`guard`');
PREPARE m4_stmt FROM @m4_sql; EXECUTE m4_stmt; DEALLOCATE PREPARE m4_stmt;
SET @m4_sql = IF(@m4_expected_uuid = '' OR @@server_uuid = @m4_expected_uuid,
                 'DO 0', 'SELECT * FROM `m4_guard__server_uuid_is_not_the_recorded_p24lab`.`guard`');
PREPARE m4_stmt FROM @m4_sql; EXECUTE m4_stmt; DEALLOCATE PREPARE m4_stmt;
-- 帳號名稱：英數底線 1..13 字（characters.account_name 是 varchar(13)）、互不相同、不是佔位符。C、D 維持佔位符時視為「不使用」。
SET @m4_c_on = IF(@m4_c_login LIKE '%\_\_M4\_%', 0, 1);
SET @m4_d_on = IF(@m4_d_login LIKE '%\_\_M4\_%', 0, 1);
SET @m4_sql = IF(@m4_gm_login REGEXP '^[A-Za-z0-9_]{1,13}$' AND @m4_a_login REGEXP '^[A-Za-z0-9_]{1,13}$' AND @m4_b_login REGEXP '^[A-Za-z0-9_]{1,13}$'
                 AND @m4_gm_login NOT LIKE '%\_\_M4\_%' AND @m4_a_login NOT LIKE '%\_\_M4\_%' AND @m4_b_login NOT LIKE '%\_\_M4\_%'
                 AND LOWER(@m4_gm_login) <> LOWER(@m4_a_login) AND LOWER(@m4_gm_login) <> LOWER(@m4_b_login) AND LOWER(@m4_a_login) <> LOWER(@m4_b_login)
                 AND (@m4_c_on = 0 OR (@m4_c_login REGEXP '^[A-Za-z0-9_]{1,13}$'
                      AND LOWER(@m4_c_login) NOT IN (LOWER(@m4_gm_login), LOWER(@m4_a_login), LOWER(@m4_b_login))))
                 AND (@m4_d_on = 0 OR (@m4_d_login REGEXP '^[A-Za-z0-9_]{1,13}$'
                      AND LOWER(@m4_d_login) NOT IN (LOWER(@m4_gm_login), LOWER(@m4_a_login), LOWER(@m4_b_login), LOWER(@m4_c_login)))),
                 'DO 0', 'SELECT * FROM `m4_guard__logins_not_substituted_load_private_vars_first`.`guard`');
PREPARE m4_stmt FROM @m4_sql; EXECUTE m4_stmt; DEALLOCATE PREPARE m4_stmt;
-- 需要的表與欄位（本檔 INSERT 用到的全部欄位）都存在
SET @m4_cols = (SELECT COUNT(*) FROM information_schema.COLUMNS
  WHERE (TABLE_SCHEMA = CONVERT(@m4_ac USING utf8) AND (
           (TABLE_NAME = 'accounts' AND COLUMN_NAME IN ('login','password','access_level','ip','host','banned','character_slot','spw','warehouse','ezpay'))
        OR (TABLE_NAME = 'characters' AND COLUMN_NAME IN ('account_name','AccessLevel','char_name','objid','level','HighLevel','MaxHp','CurHp','MaxMp','CurMp',
              'Ac','Str','Con','Dex','Cha','Intel','Wis','Class','Sex','Type','LocX','LocY','MapID','Food','Lawful','Clanname',
              'OriginalStr','OriginalCon','OriginalDex','OriginalCha','OriginalInt','OriginalWis','Banned','ElfAttr'))
        OR (TABLE_NAME = 'character_skills' AND COLUMN_NAME IN ('char_obj_id','skill_id','skill_name','is_active','activetimeleft'))
        OR (TABLE_NAME = 'character_items' AND COLUMN_NAME IN ('id','item_id','char_id','item_name','count','is_equipped','enchantlvl','is_id','durability','charge_count','remaining_time','bless'))))
     OR (TABLE_SCHEMA = CONVERT(@m4_static USING utf8) AND (
           (TABLE_NAME = 'etcitem' AND COLUMN_NAME IN ('item_id','name','classname','bless','max_charge_count'))
        OR (TABLE_NAME = 'skills' AND COLUMN_NAME IN ('skill_id','name','skill_level'))))
     OR (TABLE_SCHEMA = CONVERT(@m4_lito USING utf8)
         AND TABLE_NAME = 'commands' AND COLUMN_NAME IN ('name','access_level','class_name','note','system')));
SELECT 'S0' AS step, @m4_cols AS seed_columns_found, 74 AS seed_columns_expected;
SET @m4_sql = IF(@m4_cols = 74, 'DO 0', 'SELECT * FROM `m4_guard__seed_table_or_column_missing_run_m4_db_verify`.`guard`');
PREPARE m4_stmt FROM @m4_sql; EXECUTE m4_stmt; DEALLOCATE PREPARE m4_stmt;
-- 密碼：要新建的帳號必須有已代換的密碼（1..50 字，accounts.password 是 varchar(50)）；已存在的帳號不需要、也不會被修改
SET @m4_gm_exists = 0; SET @m4_a_exists = 0; SET @m4_b_exists = 0; SET @m4_c_exists = 0; SET @m4_d_exists = 0;
SET @m4_sql = CONCAT('SELECT ',
  '(SELECT COUNT(*) FROM `', @m4_ac, '`.`accounts` WHERE login = CONVERT(@m4_gm_login USING utf8mb4) COLLATE utf8mb4_general_ci), ',
  '(SELECT COUNT(*) FROM `', @m4_ac, '`.`accounts` WHERE login = CONVERT(@m4_a_login USING utf8mb4) COLLATE utf8mb4_general_ci), ',
  '(SELECT COUNT(*) FROM `', @m4_ac, '`.`accounts` WHERE login = CONVERT(@m4_b_login USING utf8mb4) COLLATE utf8mb4_general_ci), ',
  '(SELECT COUNT(*) FROM `', @m4_ac, '`.`accounts` WHERE login = CONVERT(@m4_c_login USING utf8mb4) COLLATE utf8mb4_general_ci), ',
  '(SELECT COUNT(*) FROM `', @m4_ac, '`.`accounts` WHERE login = CONVERT(@m4_d_login USING utf8mb4) COLLATE utf8mb4_general_ci) ',
  'INTO @m4_gm_exists, @m4_a_exists, @m4_b_exists, @m4_c_exists, @m4_d_exists');
PREPARE m4_stmt FROM @m4_sql; EXECUTE m4_stmt; DEALLOCATE PREPARE m4_stmt;
SET @m4_sql = IF((@m4_gm_exists = 1 OR (@m4_gm_password NOT LIKE '%\_\_M4\_%' AND CHAR_LENGTH(@m4_gm_password) BETWEEN 1 AND 50))
             AND (@m4_a_exists = 1 OR (@m4_a_password NOT LIKE '%\_\_M4\_%' AND CHAR_LENGTH(@m4_a_password) BETWEEN 1 AND 50))
             AND (@m4_b_exists = 1 OR (@m4_b_password NOT LIKE '%\_\_M4\_%' AND CHAR_LENGTH(@m4_b_password) BETWEEN 1 AND 50))
             AND (@m4_c_on = 0 OR @m4_c_exists = 1 OR (@m4_c_password NOT LIKE '%\_\_M4\_%' AND CHAR_LENGTH(@m4_c_password) BETWEEN 1 AND 50))
             AND (@m4_d_on = 0 OR @m4_d_exists = 1 OR (@m4_d_password NOT LIKE '%\_\_M4\_%' AND CHAR_LENGTH(@m4_d_password) BETWEEN 1 AND 50)),
                 'DO 0', 'SELECT * FROM `m4_guard__password_placeholder_for_account_to_create`.`guard`');
PREPARE m4_stmt FROM @m4_sql; EXECUTE m4_stmt; DEALLOCATE PREPARE m4_stmt;
SET @m4_guard = 'PASS';
SELECT 'S0' AS step, IF(@m4_gm_exists = 1, '已存在（不修改）', '將新建') AS gm_account,
       IF(@m4_a_exists = 1, '已存在（不修改）', '將新建') AS account_a, IF(@m4_b_exists = 1, '已存在（不修改）', '將新建') AS account_b,
       IF(@m4_c_on = 0, '不使用', IF(@m4_c_exists = 1, '已存在（不修改）', '將新建')) AS account_c,
       IF(@m4_d_on = 0, '不使用', IF(@m4_d_exists = 1, '已存在（不修改）', '將新建')) AS account_d;

-- ===== S1：帳號（只新增不存在的；GM access_level=200，其餘 0）— D3、D13 =====
-- 依據：登入只讀 accounts.login／password／banned，密碼為明文比對（LegacyMySqlRepository.java:694-701）；
--       GM 資格只看 accounts.access_level（LegacyMySqlRepository.java:707-718、SourceAdministration.java:20-33），指令門檻 200。
SET @m4_sql = IF(@m4_guard = 'PASS', CONCAT('INSERT INTO `', @m4_ac, '`.`accounts` ',
  '(login, password, access_level, ip, host, banned, character_slot, spw, warehouse, ezpay) ',
  'SELECT v.login, v.password, v.access_level, ''127.0.0.1'', ''127.0.0.1'', 0, 0, '''', -256, 0 FROM (',
  'SELECT @m4_gm_login AS login, @m4_gm_password AS password, 200 AS access_level, 1 AS enabled ',
  'UNION ALL SELECT @m4_a_login, @m4_a_password, 0, 1 ',
  'UNION ALL SELECT @m4_b_login, @m4_b_password, 0, 1 ',
  'UNION ALL SELECT @m4_c_login, @m4_c_password, 0, @m4_c_on ',
  'UNION ALL SELECT @m4_d_login, @m4_d_password, 0, @m4_d_on) v ',
  'WHERE v.enabled = 1 AND NOT EXISTS (SELECT 1 FROM `', @m4_ac, '`.`accounts` a WHERE a.login = CONVERT(v.login USING utf8mb4) COLLATE utf8mb4_general_ci)'), 'DO 0');
PREPARE m4_stmt FROM @m4_sql; EXECUTE m4_stmt; DEALLOCATE PREPARE m4_stmt;

-- ===== S2：LiTo GM 指令列（控制庫 commands；只補缺少的名稱，內容同 db/lito_commands.sql、db/lito_gm_polyme.sql、db/lito_gm_travel.sql）— D4 =====
-- 依據：指令以 name 查 commands，executor 必須相符（GmPolyMe.java:23-30、GmTravel.java:36-40、LiToGmCommands.java:26-35）。
SET @m4_sql = IF(@m4_guard = 'PASS', CONCAT('INSERT INTO `', @m4_lito, '`.`commands` (`name`, `access_level`, `class_name`, `note`, `system`) ',
  'SELECT v.name, 200, v.class_name, v.note, 0 FROM (',
  'SELECT ''polyme'' AS name, ''L1PolyMe'' AS class_name, ''自身變身：.polyme 外型代號；7200秒；不寫回角色庫'' AS note ',
  'UNION ALL SELECT ''loc'', ''L1Loc'', ''查詢座標：.loc；顯示 X/Y/地圖/方向'' ',
  'UNION ALL SELECT ''move'', ''L1Move'', ''同地圖傳送：.move X Y [目前地圖ID]；不寫回來源角色庫'' ',
  'UNION ALL SELECT ''item'', ''L1CreateItem'', ''創造道具：.item 道具ID [數量] [強化值]；僅本場記憶體'' ',
  'UNION ALL SELECT ''spawn'', ''L1SpawnCmd'', ''召喚 NPC：.spawn NPCID [數量] [範圍]；僅本場記憶體'' ',
  'UNION ALL SELECT ''de'', ''L1SpawnDe'', ''召喚測試假人：.de 數量 [範圍]'') v ',
  'WHERE NOT EXISTS (SELECT 1 FROM `', @m4_lito, '`.`commands` c WHERE c.`name` = CONVERT(v.name USING utf8mb4) COLLATE utf8mb4_general_ci)'), 'DO 0');
PREPARE m4_stmt FROM @m4_sql; EXECUTE m4_stmt; DEALLOCATE PREPARE m4_stmt;

-- ===== S3：角色（objid 190379001–190379012；名稱或 objid 已存在就略過該列；帳號不存在的角色不寫）— D1、D2、D6、D9、D13 =====
-- 依據：角色清單固定 ORDER BY char_name LIMIT 8、不看 character_slot（LegacyMySqlRepository.java:969-983）；職業＝Type（0 王族 1 騎士 2 妖精 3 法師
--       4 黑妖 5 龍騎士 6 幻術師）＋Sex，Class 是外型（BattleClassGfx.java:5-6）；結算畫面職業圖示只送 Type。
-- 全部等級 52（D1：等級 15 以上才有頻道可進）。帳號 A 加 7 個角色：連同 #83 既有的 1 個剛好 8 個（兩頁）。
-- 同血盟（D9，G02 對友方）：M4A4（帳號 A）與 M4B2（帳號 B）的 Clanname 都是 M4CLAN。伺服器把角色的原始血盟名稱匯入記憶體
--       （ClientSession.java:94074 → GameHub.registerOriginalClan，GameHub.java:5172-5194），sameClan 比的是這個（GameHub.java:5274-5279）。只填 Clanname、ClanID 留 0，
--       沒有對應的 clan_data 列——是否足夠〔推測，待實機〕。其他 M4 角色不入盟，避免影響一般對戰。
-- ElfAttr（T129 #380，RUNBOOK C 部 E5）：M4A3 填 8（風）。精靈魔法（技能等級 17..22）在 skills.attr 不為 0 時要求 ElfAttr 含該位元
--       （SourceElfAttributeGate.java:20-24；1 地、2 火、4 水、8 風）；靜態庫裡 167 風之枷鎖的 attr 是 8。M4D1 維持 0，用來驗屬性不符（V28）。
-- AccessLevel 一律 0：characters.AccessLevel>=200 會略過技能材料（NativeCastMaterialPolicy.java:44、96），不是 GM 指令資格。
SET @m4_sql = IF(@m4_guard = 'PASS', CONCAT('INSERT INTO `', @m4_ac, '`.`characters` ',
  '(account_name, AccessLevel, char_name, objid, level, HighLevel, MaxHp, CurHp, MaxMp, CurMp, Ac, Str, Con, Dex, Cha, Intel, Wis, ',
  'Class, Sex, Type, LocX, LocY, MapID, Food, Lawful, Clanname, ElfAttr, OriginalStr, OriginalCon, OriginalDex, OriginalCha, OriginalInt, OriginalWis) ',
  'SELECT a.login, 0, v.char_name, v.objid, 52, 52, v.hp, v.hp, v.mp, v.mp, v.ac, v.s, v.c, v.d, v.ch, v.i, v.w, ',
  'v.gfx, v.sex, v.type, 33440, 32798, 4, 40, 0, v.clan, v.elf, v.s, v.c, v.d, v.ch, v.i, v.w FROM (',
  'SELECT @m4_gm_login AS account_name, ''M4GM'' AS char_name, 190379001 AS objid, 400 AS hp, 300 AS mp, -5 AS ac, 8 AS s, 12 AS c, 8 AS d, 8 AS ch, 18 AS i, 16 AS w, 734 AS gfx, 0 AS sex, 3 AS type, '''' AS clan, 0 AS elf ',
  'UNION ALL SELECT @m4_a_login, ''M4DK'', 190379002, 550, 150, -10, 18, 16, 12, 8, 11, 12, 6658, 0, 5, '''', 0 ',
  'UNION ALL SELECT @m4_a_login, ''M4A2'', 190379003, 600, 100, -10, 18, 16, 12, 8, 8, 12, 61, 0, 1, '''', 0 ',
  'UNION ALL SELECT @m4_a_login, ''M4A3'', 190379004, 450, 200, -8, 11, 12, 18, 9, 12, 12, 138, 0, 2, '''', 8 ',
  'UNION ALL SELECT @m4_a_login, ''M4A4'', 190379005, 400, 300, -5, 8, 12, 8, 8, 18, 16, 734, 0, 3, ''M4CLAN'', 0 ',
  'UNION ALL SELECT @m4_a_login, ''M4A5'', 190379006, 480, 180, -8, 12, 10, 18, 9, 11, 10, 2786, 0, 4, '''', 0 ',
  'UNION ALL SELECT @m4_a_login, ''M4A6'', 190379007, 500, 150, -8, 13, 13, 11, 18, 10, 11, 0, 0, 0, '''', 0 ',
  'UNION ALL SELECT @m4_a_login, ''M4A7'', 190379008, 600, 100, -10, 18, 16, 12, 8, 8, 12, 48, 1, 1, '''', 0 ',
  'UNION ALL SELECT @m4_b_login, ''M4IL'', 190379009, 420, 280, -6, 11, 12, 10, 8, 17, 14, 6671, 0, 6, '''', 0 ',
  'UNION ALL SELECT @m4_b_login, ''M4B2'', 190379010, 600, 100, -10, 18, 16, 12, 8, 8, 12, 61, 0, 1, ''M4CLAN'', 0 ',
  'UNION ALL SELECT @m4_c_login, ''M4C1'', 190379011, 600, 100, -10, 18, 16, 12, 8, 8, 12, 61, 0, 1, '''', 0 ',
  'UNION ALL SELECT @m4_d_login, ''M4D1'', 190379012, 450, 200, -8, 11, 12, 18, 9, 12, 12, 138, 0, 2, '''', 0) v ',
  'JOIN `', @m4_ac, '`.`accounts` a ON a.login = CONVERT(v.account_name USING utf8mb4) COLLATE utf8mb4_general_ci ',
  'WHERE NOT EXISTS (SELECT 1 FROM `', @m4_ac, '`.`characters` c WHERE c.objid = v.objid ',
  'OR c.char_name = CONVERT(v.char_name USING utf8mb4) COLLATE utf8mb4_general_ci)'), 'DO 0');
PREPARE m4_stmt FROM @m4_sql; EXECUTE m4_stmt; DEALLOCATE PREPARE m4_stmt;

-- ===== S4：技能（只寫給上面的 M4 角色；skill_name 取自靜態庫 skills.name；樣板 skill_level 不在 1..28 的不寫）— D7、D8、D10、D11 =====
-- 依據：施法前檢查 learnedSkills，來源是 character_skills JOIN skills（LegacyMySqlRepository.java:2688-2691、NativeSkillMastery.java:14-23）。
-- M4A4（法師）：4 光箭、26 通暢氣脈術（G02）、29 緩速術（G23，材料 40318）；M4A2（騎士）：87 衝擊之暈；M4DK：186 血之渴望（G25，材料 49157）。
-- T129（#380，RUNBOOK C 部）：M4A4 加 3、10、12、17、21、28、39、40、43、44、47、50、58、76、80（V09–V16；40／44／47／76 的材料也是 40318）；
--       M4GM 加 3、12、21、43（V12 當 44 相消的目標，自己先上 BUFF；這四個不需要材料）；M4A3（妖精）加 148、149、152、153、167（材料 40319）；
--       M4A5（黑妖）加 103（材料 40321）；M4DK 加 194；M4D1（可選帳號 D 的妖精，ElfAttr=0）加 167（V28 屬性不符）。
SET @m4_sql = IF(@m4_guard = 'PASS', CONCAT('INSERT INTO `', @m4_ac, '`.`character_skills` (char_obj_id, skill_id, skill_name, is_active, activetimeleft) ',
  'SELECT v.objid, s.skill_id, s.name, 0, 0 FROM (',
  'SELECT 190379005 AS objid, ''M4A4'' AS char_name, 4 AS skill_id UNION ALL SELECT 190379005, ''M4A4'', 26 UNION ALL SELECT 190379005, ''M4A4'', 29 ',
  'UNION ALL SELECT 190379001, ''M4GM'', 4 UNION ALL SELECT 190379001, ''M4GM'', 26 ',
  'UNION ALL SELECT 190379003, ''M4A2'', 87 UNION ALL SELECT 190379002, ''M4DK'', 186 ',
  'UNION ALL SELECT 190379005, ''M4A4'', 3 UNION ALL SELECT 190379005, ''M4A4'', 10 UNION ALL SELECT 190379005, ''M4A4'', 12 UNION ALL SELECT 190379005, ''M4A4'', 17 ',
  'UNION ALL SELECT 190379005, ''M4A4'', 21 UNION ALL SELECT 190379005, ''M4A4'', 28 UNION ALL SELECT 190379005, ''M4A4'', 39 UNION ALL SELECT 190379005, ''M4A4'', 40 ',
  'UNION ALL SELECT 190379005, ''M4A4'', 43 UNION ALL SELECT 190379005, ''M4A4'', 44 UNION ALL SELECT 190379005, ''M4A4'', 47 UNION ALL SELECT 190379005, ''M4A4'', 50 ',
  'UNION ALL SELECT 190379005, ''M4A4'', 58 UNION ALL SELECT 190379005, ''M4A4'', 76 UNION ALL SELECT 190379005, ''M4A4'', 80 ',
  'UNION ALL SELECT 190379001, ''M4GM'', 3 UNION ALL SELECT 190379001, ''M4GM'', 12 UNION ALL SELECT 190379001, ''M4GM'', 21 UNION ALL SELECT 190379001, ''M4GM'', 43 ',
  'UNION ALL SELECT 190379004, ''M4A3'', 148 UNION ALL SELECT 190379004, ''M4A3'', 149 UNION ALL SELECT 190379004, ''M4A3'', 152 UNION ALL SELECT 190379004, ''M4A3'', 153 ',
  'UNION ALL SELECT 190379004, ''M4A3'', 167 UNION ALL SELECT 190379006, ''M4A5'', 103 UNION ALL SELECT 190379002, ''M4DK'', 194 UNION ALL SELECT 190379012, ''M4D1'', 167) v ',
  'JOIN `', @m4_static, '`.`skills` s ON s.skill_id = v.skill_id AND s.skill_level BETWEEN 1 AND 28 ',
  'JOIN `', @m4_ac, '`.`characters` c ON c.objid = v.objid AND c.char_name = CONVERT(v.char_name USING utf8mb4) COLLATE utf8mb4_general_ci ',
  'WHERE NOT EXISTS (SELECT 1 FROM `', @m4_ac, '`.`character_skills` k WHERE k.char_obj_id = v.objid AND k.skill_id = v.skill_id)'), 'DO 0');
PREPARE m4_stmt FROM @m4_sql; EXECUTE m4_stmt; DEALLOCATE PREPARE m4_stmt;

-- ===== S5：背包道具（character_items.id 190379101–190379119；樣板不在靜態庫 etcitem 的道具不寫）— D5、D12、D14 的替代 =====
-- 依據：背包查詢 JOIN 靜態庫樣板；count 必須 1..2^31-1、item_id 必須對得到樣板，否則該角色整個背包載入失敗
--       （LegacyMySqlRepository.java:8989-8992、6181-6188；LegacyInventoryWeightLoader.java:46-48）。
-- GM 魔杖的 item_id 不寫死：以 classname='wand.Storm_Walk' 從靜態庫查（GmTravel.java:16-17；使用魔杖本身不檢查 GM 權限，GmTravel.java:59-74）。
-- 魔法娃娃 55000 召喚時要扣耗材 etcitem_doll_type.need／count＝40308 金幣 ×1000（ClientSession.java:19935-19976）；藍水＝40015（InventoryCatalog.java:703-707）。
-- 40088 變形卷軸：A12 沒有 GM 時的替代（RUNBOOK D14 是從商店買；這裡直接放進 M4A4 背包）。
-- T129（#380，RUNBOOK C 部 E7 與材料）：M4A4 加 40010 紅水、40013 綠水、40014 勇水（V07），40318 由 30 改成 100 顆（負重是否足夠待實機）；M4A3 加 40319 精靈玉（152／153／167）；
--       M4A5 加 40321 二級黑魔石（103）；M4D1 加 40319。
SET @m4_wand = NULL;
SET @m4_sql = IF(@m4_guard = 'PASS', CONCAT('SELECT MIN(item_id) INTO @m4_wand FROM `', @m4_static, '`.`etcitem` WHERE classname = ''wand.Storm_Walk'''), 'DO 0');
PREPARE m4_stmt FROM @m4_sql; EXECUTE m4_stmt; DEALLOCATE PREPARE m4_stmt;
SET @m4_sql = IF(@m4_guard = 'PASS', CONCAT('INSERT INTO `', @m4_ac, '`.`character_items` ',
  '(id, item_id, char_id, item_name, `count`, is_equipped, enchantlvl, is_id, durability, charge_count, remaining_time, bless) ',
  'SELECT v.id, e.item_id, v.objid, e.name, v.cnt, 0, 0, 1, 0, e.max_charge_count, 0, e.bless FROM (',
  'SELECT 190379101 AS id, 190379001 AS objid, ''M4GM'' AS char_name, @m4_wand AS item_id, 1 AS cnt ',
  'UNION ALL SELECT 190379102, 190379001, ''M4GM'', 40015, 10 ',
  'UNION ALL SELECT 190379103, 190379002, ''M4DK'', 49157, 30 ',
  'UNION ALL SELECT 190379104, 190379002, ''M4DK'', 40015, 10 ',
  'UNION ALL SELECT 190379105, 190379002, ''M4DK'', 55000, 1 ',
  'UNION ALL SELECT 190379106, 190379002, ''M4DK'', 40308, 100000 ',
  'UNION ALL SELECT 190379107, 190379009, ''M4IL'', 40015, 10 ',
  'UNION ALL SELECT 190379108, 190379009, ''M4IL'', 55000, 1 ',
  'UNION ALL SELECT 190379109, 190379009, ''M4IL'', 40308, 100000 ',
  'UNION ALL SELECT 190379110, 190379005, ''M4A4'', 40318, 100 ',
  'UNION ALL SELECT 190379111, 190379005, ''M4A4'', 40015, 10 ',
  'UNION ALL SELECT 190379112, 190379003, ''M4A2'', 40015, 10 ',
  'UNION ALL SELECT 190379113, 190379005, ''M4A4'', 40088, 5 ',
  'UNION ALL SELECT 190379114, 190379005, ''M4A4'', 40010, 10 ',
  'UNION ALL SELECT 190379115, 190379005, ''M4A4'', 40013, 5 ',
  'UNION ALL SELECT 190379116, 190379005, ''M4A4'', 40014, 5 ',
  'UNION ALL SELECT 190379117, 190379004, ''M4A3'', 40319, 30 ',
  'UNION ALL SELECT 190379118, 190379006, ''M4A5'', 40321, 30 ',
  'UNION ALL SELECT 190379119, 190379012, ''M4D1'', 40319, 5) v ',
  'JOIN `', @m4_static, '`.`etcitem` e ON e.item_id = v.item_id ',
  'JOIN `', @m4_ac, '`.`characters` c ON c.objid = v.objid AND c.char_name = CONVERT(v.char_name USING utf8mb4) COLLATE utf8mb4_general_ci ',
  'WHERE NOT EXISTS (SELECT 1 FROM `', @m4_ac, '`.`character_items` i WHERE i.id = v.id ',
  'OR (i.char_id = v.objid AND i.item_id = v.item_id))'), 'DO 0');
PREPARE m4_stmt FROM @m4_sql; EXECUTE m4_stmt; DEALLOCATE PREPARE m4_stmt;

-- ===== 結果（不列密碼）=====
SET @m4_sql = CONCAT('SELECT ''S1'' AS step, a.login, a.access_level, a.banned, ',
  '(SELECT COUNT(*) FROM `', @m4_ac, '`.`characters` c WHERE c.account_name = a.login AND c.Banned = 0) AS characters ',
  'FROM `', @m4_ac, '`.`accounts` a WHERE a.login IN (CONVERT(@m4_gm_login USING utf8mb4) COLLATE utf8mb4_general_ci, ',
  'CONVERT(@m4_a_login USING utf8mb4) COLLATE utf8mb4_general_ci, CONVERT(@m4_b_login USING utf8mb4) COLLATE utf8mb4_general_ci, ',
  'CONVERT(@m4_c_login USING utf8mb4) COLLATE utf8mb4_general_ci, CONVERT(@m4_d_login USING utf8mb4) COLLATE utf8mb4_general_ci) ORDER BY a.login');
PREPARE m4_stmt FROM @m4_sql; EXECUTE m4_stmt; DEALLOCATE PREPARE m4_stmt;
SET @m4_sql = CONCAT('SELECT ''S2'' AS step, `name`, access_level, class_name, `system` FROM `', @m4_lito, '`.`commands` ORDER BY `name`');
PREPARE m4_stmt FROM @m4_sql; EXECUTE m4_stmt; DEALLOCATE PREPARE m4_stmt;
SET @m4_sql = CONCAT('SELECT ''S3'' AS step, c.objid, c.char_name, c.account_name, c.Type, c.Sex, c.Class, c.level, c.Clanname, ',
  '(SELECT COUNT(*) FROM `', @m4_ac, '`.`character_skills` k WHERE k.char_obj_id = c.objid) AS skills, ',
  '(SELECT COUNT(*) FROM `', @m4_ac, '`.`character_items` i WHERE i.char_id = c.objid) AS item_rows ',
  'FROM `', @m4_ac, '`.`characters` c WHERE c.objid BETWEEN 190379001 AND 190379012 ORDER BY c.objid');
PREPARE m4_stmt FROM @m4_sql; EXECUTE m4_stmt; DEALLOCATE PREPARE m4_stmt;
SET @m4_sql = CONCAT('SELECT ''S5'' AS step, i.id, i.char_id, i.item_id, i.item_name, i.`count`, i.charge_count FROM `', @m4_ac,
  '`.`character_items` i WHERE i.id BETWEEN 190379101 AND 190379119 ORDER BY i.id');
PREPARE m4_stmt FROM @m4_sql; EXECUTE m4_stmt; DEALLOCATE PREPARE m4_stmt;
SELECT 'M4-SEED: DONE' AS result, @m4_ac AS character_schema, @m4_lito AS lito_schema, IFNULL(@m4_wand, '（靜態庫沒有 wand.Storm_Walk，未放 GM 魔杖）') AS gm_wand_item_id,
       '接著重跑 m4-db-verify.sql（V9 列出資料前置狀態）；帳號權限於登入後首次使用時讀取，改完要重新登入' AS next_step;
