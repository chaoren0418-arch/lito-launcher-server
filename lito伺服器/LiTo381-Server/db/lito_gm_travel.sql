-- LiTo 同地圖 GM 傳送；只新增缺少的指令，保留管理者既有設定。
-- 自訂庫名請替換 380_lito；不修改來源角色、帳號或天堂 commands。
INSERT INTO `380_lito`.`commands` (`name`,`access_level`,`class_name`,`note`,`system`)
SELECT 'loc',200,'L1Loc','查詢座標：.loc；顯示 X/Y/地圖/方向',0 FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM `380_lito`.`commands` WHERE `name`='loc');
INSERT INTO `380_lito`.`commands` (`name`,`access_level`,`class_name`,`note`,`system`)
SELECT 'move',200,'L1Move','同地圖傳送：.move X Y [目前地圖ID]；不寫回來源角色庫',0 FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM `380_lito`.`commands` WHERE `name`='move');
-- Storm_Walk 的 item_id 從共用靜態庫 etcitem.classname='wand.Storm_Walk' 讀取。
-- 不另建道具、不覆寫原靜態資料；變更 classname 後需重啟 LiTo。
