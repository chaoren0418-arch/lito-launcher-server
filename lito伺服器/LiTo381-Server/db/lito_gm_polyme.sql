-- 僅新增 LiTo 專用指令；已有同名列時保留管理者設定。
-- 自訂 LiTo 庫名時替換 380_lito，不修改來源帳號／角色／靜態庫。
INSERT INTO `380_lito`.`commands` (`name`,`access_level`,`class_name`,`note`,`system`)
SELECT 'polyme',200,'L1PolyMe','自身變身：.polyme 外型代號；7200秒；不寫回角色庫',0
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM `380_lito`.`commands` WHERE `name`='polyme');
