-- LiTo 0.6.42：來源目錄。權限：執行帳號只需 SELECT 此表。
-- 已有列不覆寫；修改後重啟 LiTo 與登入器。
CREATE TABLE IF NOT EXISTS `380_lito`.`lito_source_server` (
  `source_id` TINYINT UNSIGNED NOT NULL PRIMARY KEY COMMENT '固定來源編號 1~8，勿重編既有來源',
  `display_name` VARCHAR(45) NOT NULL COMMENT '原生選服顯示名稱（MS950 最多63 bytes）',
  `character_database` VARCHAR(64) NULL COMMENT '角色來源庫，停用列可留 NULL',
  `enabled` TINYINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '0停用 1啟用，下次啟動生效',
  UNIQUE KEY `uq_character_database` (`character_database`),
  CONSTRAINT `chk_lito_source_id` CHECK (`source_id` BETWEEN 1 AND 8),
  CONSTRAINT `chk_lito_source_enabled` CHECK (`enabled` IN (0,1))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='LiTo 來源伺服器清單';

INSERT IGNORE INTO `380_lito`.`lito_source_server` (`source_id`,`display_name`,`character_database`,`enabled`) VALUES
(1,'太陽神','380_ac',1),
(2,'月亮神','380_ac_1',1),
(3,'伺服器3','380_ac_2',1),
(4,'伺服器4',NULL,0),
(5,'伺服器5',NULL,0),
(6,'伺服器6',NULL,0),
(7,'伺服器7',NULL,0),
(8,'伺服器8',NULL,0);

-- 範例：資料庫與 character_lito、讀取來源及寫入戰績權限已備妥後，開放第4服：
-- UPDATE 380_lito.lito_source_server SET display_name='新伺服器',character_database='380_ac_3',enabled=1 WHERE source_id=4;
