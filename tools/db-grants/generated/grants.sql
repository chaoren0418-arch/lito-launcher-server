-- 由 tools/db-grants/gen_grants.py 產生；請勿手改，改 review.json 後重產。
-- 佔位：380_db＝database.static.database、380_lito＝database.lito.database、
--       380_ac＝每個 enabled=1 來源的 character_database（每個來源各複製一份「來源角色庫」段並換庫名）。
-- 帳號：'lito_shared'@'127.0.0.1' 只是範例；Host 依實際連線來源設定，不要用 '%'。
-- 前提：表必須已存在（MySQL 對不存在的表做表級 GRANT 會失敗）；character_lito 需已有 last_result_id 欄（db/lito_battle_settlement.sql）。
CREATE USER IF NOT EXISTS 'lito_shared'@'127.0.0.1' IDENTIFIED BY '<由營運設定>';

-- ===== 靜態庫（database.static.database，預設 380_db）：54 表 =====
GRANT SELECT ON `380_db`.`armor` TO 'lito_shared'@'127.0.0.1';
GRANT SELECT ON `380_db`.`etcitem` TO 'lito_shared'@'127.0.0.1';
GRANT SELECT ON `380_db`.`etcitem_arrow` TO 'lito_shared'@'127.0.0.1';
GRANT SELECT ON `380_db`.`etcitem_buff藥水能力` TO 'lito_shared'@'127.0.0.1';
GRANT SELECT ON `380_db`.`etcitem_buff藥水設置` TO 'lito_shared'@'127.0.0.1';
GRANT SELECT ON `380_db`.`etcitem_doll_power_new` TO 'lito_shared'@'127.0.0.1';
GRANT SELECT ON `380_db`.`etcitem_doll_type` TO 'lito_shared'@'127.0.0.1';
GRANT SELECT ON `380_db`.`mapids` TO 'lito_shared'@'127.0.0.1';
GRANT SELECT ON `380_db`.`mapids_shop` TO 'lito_shared'@'127.0.0.1';
GRANT SELECT ON `380_db`.`mobskill` TO 'lito_shared'@'127.0.0.1';
GRANT SELECT ON `380_db`.`npc` TO 'lito_shared'@'127.0.0.1';
GRANT SELECT ON `380_db`.`npcchat` TO 'lito_shared'@'127.0.0.1';
GRANT SELECT ON `380_db`.`pettypes` TO 'lito_shared'@'127.0.0.1';
GRANT SELECT ON `380_db`.`polymorphs` TO 'lito_shared'@'127.0.0.1';
GRANT SELECT ON `380_db`.`server_event` TO 'lito_shared'@'127.0.0.1';
GRANT SELECT ON `380_db`.`skills` TO 'lito_shared'@'127.0.0.1';
GRANT SELECT ON `380_db`.`skills_buff_ability` TO 'lito_shared'@'127.0.0.1';
GRANT SELECT ON `380_db`.`skills_相消免疫技能` TO 'lito_shared'@'127.0.0.1';
GRANT SELECT ON `380_db`.`skills_相消怪物轉換` TO 'lito_shared'@'127.0.0.1';
GRANT SELECT ON `380_db`.`skills_負面boss免疫` TO 'lito_shared'@'127.0.0.1';
GRANT SELECT ON `380_db`.`skills_負面技能控制` TO 'lito_shared'@'127.0.0.1';
GRANT SELECT ON `380_db`.`skills_魔屏無效技能` TO 'lito_shared'@'127.0.0.1';
GRANT SELECT ON `380_db`.`spawnlist_npc` TO 'lito_shared'@'127.0.0.1';
GRANT SELECT ON `380_db`.`spr_action` TO 'lito_shared'@'127.0.0.1';
GRANT SELECT ON `380_db`.`weapon` TO 'lito_shared'@'127.0.0.1';
GRANT SELECT ON `380_db`.`假人_人物清單` TO 'lito_shared'@'127.0.0.1';
GRANT SELECT ON `380_db`.`假人_能力設置` TO 'lito_shared'@'127.0.0.1';
GRANT SELECT ON `380_db`.`假人_設置狀態` TO 'lito_shared'@'127.0.0.1';
GRANT SELECT ON `380_db`.`系統_劇情任務` TO 'lito_shared'@'127.0.0.1';
GRANT SELECT ON `380_db`.`裝備_armor_bless_指定` TO 'lito_shared'@'127.0.0.1';
GRANT SELECT ON `380_db`.`裝備_armor_bless_類型` TO 'lito_shared'@'127.0.0.1';
GRANT SELECT ON `380_db`.`裝備_armor_enchant_指定` TO 'lito_shared'@'127.0.0.1';
GRANT SELECT ON `380_db`.`裝備_armor_enchant_類型` TO 'lito_shared'@'127.0.0.1';
GRANT SELECT ON `380_db`.`裝備_armor_greater_類型` TO 'lito_shared'@'127.0.0.1';
GRANT SELECT ON `380_db`.`裝備_armor_魔法` TO 'lito_shared'@'127.0.0.1';
GRANT SELECT ON `380_db`.`裝備_weapon_bless_指定` TO 'lito_shared'@'127.0.0.1';
GRANT SELECT ON `380_db`.`裝備_weapon_bless_類型` TO 'lito_shared'@'127.0.0.1';
GRANT SELECT ON `380_db`.`裝備_weapon_enchant_指定` TO 'lito_shared'@'127.0.0.1';
GRANT SELECT ON `380_db`.`裝備_weapon_enchant_類型` TO 'lito_shared'@'127.0.0.1';
GRANT SELECT ON `380_db`.`裝備_weapon_greater_類型` TO 'lito_shared'@'127.0.0.1';
GRANT SELECT ON `380_db`.`裝備_weapon_屬性` TO 'lito_shared'@'127.0.0.1';
GRANT SELECT ON `380_db`.`裝備_weapon_魔法` TO 'lito_shared'@'127.0.0.1';
GRANT SELECT ON `380_db`.`裝備_特殊_卡片_組合` TO 'lito_shared'@'127.0.0.1';
GRANT SELECT ON `380_db`.`裝備_特殊_卡片_能力` TO 'lito_shared'@'127.0.0.1';
GRANT SELECT ON `380_db`.`裝備_特殊_套裝_能力` TO 'lito_shared'@'127.0.0.1';
GRANT SELECT ON `380_db`.`裝備_特殊_魔石_魔法` TO 'lito_shared'@'127.0.0.1';
GRANT SELECT ON `380_db`.`角色_力量成長` TO 'lito_shared'@'127.0.0.1';
GRANT SELECT ON `380_db`.`角色_敏捷成長` TO 'lito_shared'@'127.0.0.1';
GRANT SELECT ON `380_db`.`角色_智力成長` TO 'lito_shared'@'127.0.0.1';
GRANT SELECT ON `380_db`.`角色_精神成長` TO 'lito_shared'@'127.0.0.1';
GRANT SELECT ON `380_db`.`角色_體質成長` TO 'lito_shared'@'127.0.0.1';
GRANT SELECT ON `380_db`.`道具_物品詳細說明` TO 'lito_shared'@'127.0.0.1';
GRANT SELECT ON `380_db`.`道具_自訂穿脫能力` TO 'lito_shared'@'127.0.0.1';
GRANT SELECT ON `380_db`.`道具_變形控制魔杖` TO 'lito_shared'@'127.0.0.1';

-- ===== LiTo 控制庫（database.lito.database，預設 380_lito）：19 表 =====
GRANT SELECT ON `380_lito`.`commands` TO 'lito_shared'@'127.0.0.1';
GRANT SELECT ON `380_lito`.`lito_battle_object` TO 'lito_shared'@'127.0.0.1';
GRANT SELECT ON `380_lito`.`lito_battle_player_spawn` TO 'lito_shared'@'127.0.0.1';
GRANT SELECT ON `380_lito`.`lito_battle_rule` TO 'lito_shared'@'127.0.0.1';
GRANT SELECT, INSERT, UPDATE ON `380_lito`.`lito_battle_settlement` TO 'lito_shared'@'127.0.0.1';
GRANT SELECT ON `380_lito`.`lito_battle_spawn` TO 'lito_shared'@'127.0.0.1';
GRANT SELECT ON `380_lito`.`lito_resource_buff` TO 'lito_shared'@'127.0.0.1';
GRANT SELECT ON `380_lito`.`lito_resource_map` TO 'lito_shared'@'127.0.0.1';
GRANT SELECT ON `380_lito`.`lito_resource_map_page` TO 'lito_shared'@'127.0.0.1';
GRANT SELECT ON `380_lito`.`lito_resource_npc_identity` TO 'lito_shared'@'127.0.0.1';
GRANT SELECT ON `380_lito`.`lito_resource_pending` TO 'lito_shared'@'127.0.0.1';
GRANT SELECT ON `380_lito`.`lito_resource_portal` TO 'lito_shared'@'127.0.0.1';
GRANT SELECT ON `380_lito`.`lito_resource_projectile` TO 'lito_shared'@'127.0.0.1';
GRANT SELECT ON `380_lito`.`lito_resource_release` TO 'lito_shared'@'127.0.0.1';
GRANT SELECT ON `380_lito`.`lito_resource_skill_value` TO 'lito_shared'@'127.0.0.1';
GRANT SELECT ON `380_lito`.`lito_resource_zone` TO 'lito_shared'@'127.0.0.1';
GRANT SELECT ON `380_lito`.`lito_room_policy` TO 'lito_shared'@'127.0.0.1';
GRANT SELECT ON `380_lito`.`lito_source_server` TO 'lito_shared'@'127.0.0.1';
GRANT SELECT ON `380_lito`.`lito_tournament_shop` TO 'lito_shared'@'127.0.0.1';

-- ===== 來源角色庫（lito_source_server.character_database，每個啟用來源一個）：25 表 =====
GRANT SELECT ON `380_ac`.`accounts` TO 'lito_shared'@'127.0.0.1';
GRANT SELECT ON `380_ac`.`character_buddys` TO 'lito_shared'@'127.0.0.1';
GRANT SELECT ON `380_ac`.`character_buff` TO 'lito_shared'@'127.0.0.1';
GRANT SELECT ON `380_ac`.`character_elf_warehouse` TO 'lito_shared'@'127.0.0.1';
GRANT SELECT ON `380_ac`.`character_items` TO 'lito_shared'@'127.0.0.1';
GRANT SELECT ON `380_ac`.`character_items_card` TO 'lito_shared'@'127.0.0.1';
GRANT SELECT ON `380_ac`.`character_items_dark` TO 'lito_shared'@'127.0.0.1';
GRANT SELECT ON `380_ac`.`character_items_time` TO 'lito_shared'@'127.0.0.1';
GRANT SELECT ON `380_ac`.`character_letter` TO 'lito_shared'@'127.0.0.1';
GRANT SELECT, INSERT, UPDATE ON `380_ac`.`character_lito` TO 'lito_shared'@'127.0.0.1';
GRANT SELECT ON `380_ac`.`character_other` TO 'lito_shared'@'127.0.0.1';
GRANT SELECT ON `380_ac`.`character_pets` TO 'lito_shared'@'127.0.0.1';
GRANT SELECT ON `380_ac`.`character_quests` TO 'lito_shared'@'127.0.0.1';
GRANT SELECT ON `380_ac`.`character_skills` TO 'lito_shared'@'127.0.0.1';
GRANT SELECT ON `380_ac`.`character_teleport` TO 'lito_shared'@'127.0.0.1';
GRANT SELECT ON `380_ac`.`character_warehouse` TO 'lito_shared'@'127.0.0.1';
GRANT SELECT ON `380_ac`.`characters` TO 'lito_shared'@'127.0.0.1';
GRANT SELECT ON `380_ac`.`clan_data` TO 'lito_shared'@'127.0.0.1';
GRANT SELECT ON `380_ac`.`clan_emblem` TO 'lito_shared'@'127.0.0.1';
GRANT SELECT ON `380_ac`.`clan_warehouse` TO 'lito_shared'@'127.0.0.1';
GRANT SELECT ON `380_ac`.`server_城堡` TO 'lito_shared'@'127.0.0.1';
GRANT SELECT ON `380_ac`.`server_盟屋_佈告` TO 'lito_shared'@'127.0.0.1';
GRANT SELECT ON `380_ac`.`server_盟屋_拍賣` TO 'lito_shared'@'127.0.0.1';
GRANT SELECT ON `380_ac`.`server_盟屋_紀錄` TO 'lito_shared'@'127.0.0.1';
-- 選用（表存在才執行）：GRANT SELECT ON `380_ac`.`server_紀錄_獎勵` TO 'lito_shared'@'127.0.0.1';
