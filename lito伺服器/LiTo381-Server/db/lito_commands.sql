-- LiTo 專用 GM 指令：由管理者明確匯入，遊戲執行期只讀取。
-- 自訂庫名時請替換 380_lito（專用設定庫）及 380_db（天堂共用靜態庫）。
-- 不修改天堂 commands，也不修改任何帳號或角色資料。
CREATE TABLE IF NOT EXISTS `380_lito`.`commands` (
  `name` varchar(64) NOT NULL COMMENT '指令名稱，不含前導句點',
  `access_level` int NOT NULL DEFAULT 200 COMMENT '最低來源 accounts.access_level，1 至 200',
  `class_name` varchar(255) NOT NULL COMMENT '已實作處理器識別，不能由資料表新增任意程式',
  `note` varchar(255) DEFAULT NULL COMMENT '指令用途及參數說明',
  `system` tinyint unsigned NOT NULL DEFAULT 0 COMMENT '目前三個指令必須為 0',
  PRIMARY KEY (`name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='LiTo 專用 GM 指令設定';

-- 保留來源已有的同名指令門檻；重複執行不覆寫 LiTo 管理者的設定。
INSERT INTO `380_lito`.`commands` (`name`,`access_level`,`class_name`,`note`,`system`)
SELECT s.`name`,s.`access_level`,s.`class_name`,s.`note`,s.`system`
FROM `380_db`.`commands` s
WHERE s.`name` IN ('item','spawn','de')
  AND NOT EXISTS (SELECT 1 FROM `380_lito`.`commands` t WHERE t.`name`=s.`name`);

INSERT INTO `380_lito`.`commands` (`name`,`access_level`,`class_name`,`note`,`system`)
SELECT 'item',200,'L1CreateItem','創造道具：.item 道具ID [數量] [強化值]；僅本場記憶體',0
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM `380_lito`.`commands` WHERE `name`='item');
INSERT INTO `380_lito`.`commands` (`name`,`access_level`,`class_name`,`note`,`system`)
SELECT 'spawn',200,'L1SpawnCmd','召喚 NPC：.spawn NPCID [數量] [範圍]；僅本場記憶體',0
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM `380_lito`.`commands` WHERE `name`='spawn');
INSERT INTO `380_lito`.`commands` (`name`,`access_level`,`class_name`,`note`,`system`)
SELECT 'de',200,'L1SpawnDe','召喚測試假人：.de 數量 [範圍]',0
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM `380_lito`.`commands` WHERE `name`='de');
