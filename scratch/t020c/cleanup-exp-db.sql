-- Issue #83 實驗資料庫清理腳本。僅建立檔案，本次未執行。
-- 執行前核對 issue-83.md 的實例識別與本次建立的物件。
-- 目前實例識別及建立作業尚未完成，請勿直接執行。
-- 由管理連線執行，帳密應透過本機安全憑證入口提供。
DROP USER 'lito_exp_ro'@'localhost';
DROP USER 'lito_exp_rw'@'localhost';
DROP DATABASE `380_lito_exp`;
