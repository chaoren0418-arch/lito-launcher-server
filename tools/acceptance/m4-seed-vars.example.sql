-- m4-seed-exp.sql 的私密參數範本（T128／Issue #379）。本檔只有佔位符，可以進 git。
-- 用法：複製成 scratch/m4/private/m4-vars.sql（scratch/ 已被 .gitignore 忽略），把 __M4_…__ 換成實際值，
--       再依 tools/acceptance/README.md 的命令，用 type 把私密檔接在各個 SQL 檔前面送進 mysql。
-- 不要把代換後的檔案放進 tools/、docs/ 或任何會被 git 追蹤的位置；不要把內容貼進報告、Issue 或截圖。
--
-- 帳號 A、B：#83 建立的兩組普通測試帳號，帳號名稱取自 #83 私密交接檔
--   D:\lineage開發\3-lito開發\scratch\wt\task-83\scratch\t020c\test-accounts-private.properties
--   帳號已存在時 m4-seed-exp.sql 不會讀也不會改它的密碼，密碼欄可以維持佔位符。
-- GM 帳號：新建的實驗 GM（access_level=200）。帳號名稱與密碼由執行者自訂（英數底線，帳號 1..13 字、密碼 1..50 字），
--   寫回 #83 的私密交接檔所在目錄供之後登入使用。
-- 要改用 380_lito.sql 快照建的控制庫時，另外加一行：SET @m4_lito = 'p24_380lito_lito_exp';
-- 同一個檔也接在 m4-db-verify.sql、m4-db-prepare.sql 前面，所以 @m4_lito、@m4_grant_mode 這類覆寫寫在這裡一次就好。
-- 帳號 C、D（可選，RUNBOOK D13：2 對 2 才有真正的隊友）：不需要就讓下面四行維持佔位符，seed 會略過。
SET @m4_gm_login    = '__M4_GM_LOGIN__';
SET @m4_gm_password = '__M4_GM_PASSWORD__';
SET @m4_a_login     = '__M4_A_LOGIN__';
SET @m4_a_password  = '__M4_A_PASSWORD__';
SET @m4_b_login     = '__M4_B_LOGIN__';
SET @m4_b_password  = '__M4_B_PASSWORD__';
SET @m4_c_login     = '__M4_C_LOGIN__';
SET @m4_c_password  = '__M4_C_PASSWORD__';
SET @m4_d_login     = '__M4_D_LOGIN__';
SET @m4_d_password  = '__M4_D_PASSWORD__';
