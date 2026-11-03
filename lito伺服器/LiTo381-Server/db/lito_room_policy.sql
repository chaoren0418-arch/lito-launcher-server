-- LiTo 0.6.61：測試房與玩家房規則。只新增控制表，不修改玩家資料。
-- 改動後重啟 LiTo 伺服器生效；重複執行保留已修改設定。
CREATE TABLE IF NOT EXISTS `380_lito`.`lito_room_policy` (
  `policy_id` TINYINT UNSIGNED NOT NULL COMMENT '固定為 1，僅允許一列',
  `test_room_enabled` TINYINT UNSIGNED NOT NULL DEFAULT 1 COMMENT '1=各頻道保留 Local Lobby 測試房；0=不自動建立',
  `test_bot_enabled` TINYINT UNSIGNED NOT NULL DEFAULT 1 COMMENT '1=測試房死亡競賽單人時加入 ArenaBot；玩家自建房永不自動加入',
  PRIMARY KEY (`policy_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COMMENT='LiTo 測試房控制；測試房固定不回存勝敗戰績';
INSERT INTO `380_lito`.`lito_room_policy` (`policy_id`,`test_room_enabled`,`test_bot_enabled`)
SELECT 1,1,1 FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `380_lito`.`lito_room_policy` WHERE `policy_id`=1);
-- 玩家房人數由 lito_battle_rule.minimum_players 控制（不計觀戰者／假人）。
-- 隊伍模式至少兩人且兩隊皆有人；選擇「人數一致」須等人數；攻城每隊 15～50 人。
-- 所有真人必須準備完成；倒數途中人數／隊伍／準備改變時重新檢查。
