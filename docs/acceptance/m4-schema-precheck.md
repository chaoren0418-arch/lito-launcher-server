# M4 實驗環境開機前靜態預檢（schema／欄位／資料列）

由 `tools/acceptance/m4-schema-precheck.py` 產生，請勿手改；改程式、`tools/db-grants/review.json` 或 `tools/acceptance/m4-precheck-rules.json` 後重跑。

- 性質：**靜態比對**（python 讀原始碼掃描結果與 SQL 傾印檔），沒有連任何資料庫、沒有啟動伺服器。
- 「程式需要」＝`tools/db-grants` 掃描 `src/main` 後套用 `review.json` 的正式路徑存取清單（static 56 表、lito 19 表、character 25 表，共 104 筆表級權限）。`SELECT *` 的表只列程式實際取用的欄位。
- 「會擋啟動」＝該表在啟動路徑上被讀取（`review.json` 的 startup_classes／startup_ranges），缺表或缺欄位會讓伺服器在 `[LiTo] ready` 之前退出；其餘是執行到該功能時才會出錯。
- 傾印檔只讀取、沒有複製進 repo；實驗庫實際內容以 `tools/acceptance/m4-db-verify.sql` 連線查到的為準（待驗證）。

## 1. 輸入

| 庫別 | 傾印檔 | bytes | SHA-256 | 檔頭來源 | 表數 | 引擎 | 對應實驗 schema |
|---|---|---:|---|---|---:|---|---|
| static | `D:\BinEngine\environments\p24\sql\380_db.sql` | 9552749 | `0875F7CFF78D7D97A7380CC5D24F47B2C66B630D452377E5F8BFA6F3D29FD95E` | 380_db／MySQL 50522 (5.5.22) | 181 | InnoDB 114、MyISAM 67 | `p24_380db_lito_exp`（#284 主控 2026-11-07 02:43 裁定：由本傾印匯入） |
| lito | `D:\BinEngine\environments\p24\sql\380_lito.sql` | 271713 | `78B6940C39A1A1EA87F3AEAABCA36D3281CE60879518073621DC1B52E24C4919` | 380_lito／MySQL 50522 (5.5.22) | 19 | InnoDB 19 | `p24_lito_lito_exp`（#83 建立）；不相容時改由本傾印匯入 `p24_380lito_lito_exp` |
| character | `D:\BinEngine\environments\p24\sql\380_ac.sql` | 319710 | `F70078A7B72979340A449E626FF965E894A8B64BFF9487EAE31537848140EE6C` | 380_ac_1／MySQL 50522 (5.5.22) | 70 | InnoDB 15、MyISAM 55 | `p24_accounts_lito_exp`（#83 由 P24 的 `p24_accounts` 複製結構；**不是**由本傾印建立，本欄只當參考） |

## 2. 結論摘要

| 庫別 | 程式需要的表 | 傾印缺表 | 其中會擋啟動 | 缺欄位的表 | 缺欄位數 | 其中會擋啟動的表 |
|---|---:|---:|---:|---:|---:|---:|
| static | 56 | 0 | 0 | 0 | 0 | 0 |
| lito | 19 | 0 | 0 | 0 | 0 | 0 |
| character | 25 | 1 | 0 | 2 | 7 | 1 |

會擋啟動的缺口合計：**1 項**（缺表＋缺欄位的表）。各項判讀見下節；`character` 列比的是 `380_ac.sql` 參考傾印，`p24_accounts_lito_exp` 的實況要用 §6 的清單連線查。

## 3. 缺表與缺欄位（逐庫）

### static：靜態庫（`database.static.database`）

缺表 **0 項**、缺欄位 **0 項**。

### lito：LiTo 控制庫（`database.lito.database`）

缺表 **0 項**、缺欄位 **0 項**。

### character：來源角色庫（`lito_source_server.character_database`）

缺表 **1 項**：

| 表 | 權限 | 會擋啟動 | 證據 | 判讀 |
|---|---|---|---|---|
| `server_紀錄_獎勵` | SELECT（選用） | 否 | LegacyMySqlRepository.java:7992、LegacyMySqlRepository.java:8447 | 選用表：待領獎勵 UI 才讀（tools/db-grants/review.json optional_tables）；缺表不影響啟動與 CHECKLIST 01–21 |

缺欄位 **7 項**（2 張表）：

| 表 | 缺少的欄位 | 會擋啟動 | 證據 | 判讀 |
|---|---|---|---|---|
| `character_buff` | `snapshot_version` | 否（可容忍） | LegacyMySqlRepository.java:248、LegacyMySqlRepository.java:324 | 可容忍：`SourceBuffRead.versioned` 先用 `SELECT * … LIMIT 0` 看有沒有 `snapshot_version`，沒有就走舊單表分支（S/SourceBuffRead.java:10-23）；`healthCheck` 經 `SourceBuffRead.healthStatement` 改寫查詢（S/LegacyMySqlRepository.java:321-325）。若實驗庫**有**這一欄，就必須同時有 `character_buff_snapshot`（S/SourceBuffRead.java:24-28） |
| `character_lito` | `disconnects`、`monthly_losses`、`monthly_period`、`monthly_ranking_points`、`monthly_wins`、`ranking_points` | 是 | SettlementSql.java:45、SettlementSql.java:46 | T103b 新欄位（S/SettlementSql.java:6、45-46；S/SharedDatabase.java:76-80 缺欄位直接拒絕啟動）。`m4-db-prepare.sql` P1c 以 `db/lito_character_ranking.sql` 的同一寫法補上；`p24_accounts_lito_exp` 是 #83 在 T103b 之前建的，預期同樣缺這 6 欄（待 `m4-db-verify.sql` 確認） |

## 4. 引擎與匯入相容性

| 檢查 | 結果 | 依據 |
|---|---|---|
| lito `lito_battle_settlement` 引擎 | InnoDB（符合） | `SettlementSql.check` 要求 InnoDB |
| character `character_lito` 引擎 | MyISAM（符合） | `SharedDatabase.check` 要求 InnoDB 或 MyISAM |
| lito `lito_resource_*` 表集合 | 剛好 10 張（符合） | `ResourceDatabase.load` 要求剛好這 10 張，多或少都拒絕啟動（ResourceDatabase.java:33-38） |
| static 傾印內的 `CREATE DATABASE`／`USE`／`DROP DATABASE` | 0 處（匯入目標完全由命令列指定的 schema 決定） | #284 02:43 裁定第 1 點：不得建立或覆蓋其他庫 |
| static 傾印內的 `DEFINER`（view／trigger／routine） | 0 處 | 匯入帳號不需要 SUPER |
| static 零日期預設值（`DEFAULT '0000-00-00…'`） | 0 處 | MySQL 8.0 預設 `sql_mode`（`NO_ZERO_DATE`＋嚴格模式）會讓這種 `CREATE TABLE` 失敗；P24Lab 的 `mysql.ini` 設的是 `sql-mode=NO_ENGINE_SUBSTITUTION`（2026-11-13 唯讀確認設定檔），不受影響。在其他 8.0 實例匯入前要先調整 `sql_mode` |
| lito 傾印內的 `CREATE DATABASE`／`USE`／`DROP DATABASE` | 0 處（匯入目標完全由命令列指定的 schema 決定） | #284 02:43 裁定第 1 點：不得建立或覆蓋其他庫 |
| lito 傾印內的 `DEFINER`（view／trigger／routine） | 0 處 | 匯入帳號不需要 SUPER |
| lito 零日期預設值（`DEFAULT '0000-00-00…'`） | 0 處 | MySQL 8.0 預設 `sql_mode`（`NO_ZERO_DATE`＋嚴格模式）會讓這種 `CREATE TABLE` 失敗；P24Lab 的 `mysql.ini` 設的是 `sql-mode=NO_ENGINE_SUBSTITUTION`（2026-11-13 唯讀確認設定檔），不受影響。在其他 8.0 實例匯入前要先調整 `sql_mode` |
| character 傾印內的 `CREATE DATABASE`／`USE`／`DROP DATABASE` | 0 處（匯入目標完全由命令列指定的 schema 決定） | #284 02:43 裁定第 1 點：不得建立或覆蓋其他庫 |
| character 傾印內的 `DEFINER`（view／trigger／routine） | 0 處 | 匯入帳號不需要 SUPER |
| character 零日期預設值（`DEFAULT '0000-00-00…'`） | **2 處**：log_商城消費.time（第 2802 行）、log_gm指令.time（第 3005 行） | MySQL 8.0 預設 `sql_mode`（`NO_ZERO_DATE`＋嚴格模式）會讓這種 `CREATE TABLE` 失敗；P24Lab 的 `mysql.ini` 設的是 `sql-mode=NO_ENGINE_SUBSTITUTION`（2026-11-13 唯讀確認設定檔），不受影響。在其他 8.0 實例匯入前要先調整 `sql_mode` |

## 5. 資料列檢查（程式或驗收步驟依賴的現有資料）

| 庫別 | 表 | 檢查 | 結果 | 內容 | 用途／依據 |
|---|---|---|---|---|---|
| static | `etcitem`.`classname` | 值存在：wand.Storm_Walk | 齊全 | wand.Storm_Walk→42501/GM瞬間移動 | GM 魔杖：啟動時以 `SELECT item_id FROM etcitem WHERE classname='wand.Storm_Walk'` 取道具編號（S/GmTravel.java:16-17）；CHECKLIST 18／G22 |
| static | `etcitem`.`item_id` | 值存在：40010、40011、40012、40506、40013、40014、40068、40016、40015、140015、40736、40017、40025、40032 | 齊全 | 40010→治癒藥水/hp.UserAddHp 10 20 189 true 0 0/2/500；40011→強力治癒藥水/hp.UserAddHp 30 50 194 true 0 0/2/1000；40012→終極治癒藥水/hp.UserAddHp 60 80 197 true/2/1000；40013→自我加速藥水/brave.StatusHaste 300 191 true 40308 10/0/0；40014→勇敢藥水/brave.StatusBrave 300 751 true/0/0；40015→加速魔力回復藥水/mp.UserMprBlue 600 190/0/0；40016→慎重藥水/brave.StatusWisdomPotion 300 750/0/0；40017→解毒藥水/Disintoxicat_Potion/0/0；40025→失明藥水/Blindness_Potion/0/0；40032→伊娃的祝福/Water_Essence/0/0；40068→精靈餅乾/brave.StatusElfBrave 480 751 true/0/0；40506→安特的水果/hp.UserAddHp 30 50 197 true/2/1000；40736→智慧貨幣/mp.UserMprBlue 600 190/0/0；140015→加速魔力回復藥水/mp.UserMprBlue 700 190/0/0 | T111／T134 消耗品參數：啟動時同一條 SELECT 讀這 14 列的 `classname`、`delay_id`、`delay_time`（S/ConsumableItemParameters.java:137-152、203-230）；缺列或缺 delay 欄時退回內建值、不擋啟動。治癒類 40010／40011／40012／40506 應為冷卻群組 2、500／1000／1000／1000 毫秒，其餘 0。40015＝藍水（G25） |
| static | `etcitem`.`item_id` | 值存在：40088、40308、40318、49157、55000 | 齊全 | 40088→變形卷軸/Sosc_PolyReel；40308→金幣/0；40318→魔法寶石/0；49157→刻印的骨頭碎片/0；55000→魔法娃娃：肥肥/doll.Magic_Doll | `m4-seed-exp.sql` 會放進角色背包的道具：40088 變形卷軸（A12 的替代）、40308 金幣（魔法娃娃耗材）、40318 魔法寶石（緩速術 29 的材料）、49157 刻印的骨頭碎片（血之渴望 186 的材料）、55000 魔法娃娃：肥肥 |
| static | `etcitem_doll_type`.`itemid` | 值存在：55000 | 齊全 | 55000→102/40308/1000/1800/魔法娃娃：肥肥 | 魔法娃娃樣板：背包查詢 LEFT JOIN 此表（S/LegacyMySqlRepository.java:8992）；`need`／`count` 是召喚耗材（G25） |
| static | `etcitem_doll_power_new`.`id` | 值存在：102 | 齊全 | 102→重量減免+5%/PASSIVE_WEIGHT_REDUCTION | 娃娃 55000 的 `powers`=102 必須在此表存在，否則該角色背包載入失敗（S/LegacyMySqlRepository.java:9030-9033、6241-6244） |
| static | `skills`.`skill_id` | 值存在：4、26、29、87、186 | 齊全 | 4→光箭/1/0/0/0；26→通暢氣脈術/4/0/0/0；29→緩速術/4/40318/1/0；87→衝擊之暈/11/0/0/0；186→血之渴望/24/49157/1/30 | `m4-seed-exp.sql` 會寫進 `character_skills` 的技能：4 光箭與 29（G23）、26 通暢氣脈術（G02）、87 衝擊之暈、186 血之渴望（G25）；`skill_level` 必須 1..28（S/NativeSkillMastery.java:14-23） |
| lito | `lito_resource_release`.`release_id` | 值存在：1 | 齊全 | 1→0.6.60/0.6.61 | `ResourceDatabase.load` 要求 `release_id=1` 的 `qualified_version`＝`0.6.61`，否則啟動失敗（S/ResourceDatabase.java:29-30） |
| lito | `commands`.`name` | 值存在：polyme、move、loc、item、spawn、de | 缺 polyme、move、item、spawn | de→L1SpawnDe/200；loc→L1Loc/200 | GM 指令以 `name` 查 `commands`（S/LegacyMySqlRepository.java:720-723；S/GmPolyMe.java:23-30、S/GmTravel.java:36-40、S/LiToGmCommands.java:26-35）；缺的由 `m4-seed-exp.sql` S2 補（CHECKLIST 12、17；G21） |
| lito | `commands`.`name` | 列出現有列 | 5 列 | 1/L1CreateItem/200/0；2/L1SpawnCmd/200/0；3/L1Move/200/0；de/L1SpawnDe/200/0；loc/L1Loc/200/0 | 快照內現有的指令列（對照上一列缺少的名稱） |
| lito | `lito_tournament_shop`.`item_id` | 值存在：40015、40088 | 齊全 | 40015→700/1/藍色藥水；40088→1300/1/變形卷軸 | RUNBOOK D14：統一設定下擂台商店改讀資料表（S/TournamentShopCatalog.java:39-54）；藍色藥水 40015、變形卷軸 40088 要在商店內（G25-3、A12 的替代） |
| lito | `lito_source_server`.`source_id` | 列出現有列 | 8 列 | 1/公牛/380_ac_1/1；2/天使/380_ac_2/1；3/水蛇/380_ac_3/0；4/美神/380_ac_4/0；5/獨眼/380_ac_5/0；6/天后/380_ac_6/0；7/冥王/380_ac_7/0；8/蛇髮/380_ac_8/0 | 快照的來源路由指向正式來源庫名；改用 `p24_380lito_lito_exp` 時 `m4-db-prepare.sql` P3 會改成只啟用一條、指向 `p24_accounts_lito_exp`（#284 02:43 裁定第 4 點） |
| lito | `lito_room_policy`.`policy_id` | 列出現有列 | 1 列 | 1/1/1 | `NativeRoomRules.install` 啟動時讀（S/NativeRoomRules.java:20）；測試房不回存勝敗，CHECKLIST 08 要用玩家自建房 |
| lito | `lito_battle_settlement`.`result_id` | 列數 | 1 列 |  | 快照帶有正式環境的既有收據（`source_database` 是正式庫名）；`drainAll` 只處理 `source_database`＝本來源的列（S/SettlementSql.java:74），不影響實驗，但 #284 `Probe.java` 的『收據必須 0 筆』判斷在改用快照庫時不成立 |
| character | `accounts`.`login` | 列數 | 9 列 |  | 參考傾印內的帳號列數（實驗角色庫只複製結構，不使用這些資料） |

## 6. `p24_accounts_lito_exp`：開機前要用 `information_schema` 查的表與欄位

#83 的角色庫是從 P24 的 `p24_accounts` 複製結構，沒有傾印可以靜態比對。下表是程式對來源角色庫的全部需求；`tools/acceptance/m4-db-verify.sql` 的 GENERATED 區塊由同一份清單產生，會逐欄對 `information_schema.COLUMNS` 查並列出缺少的項目。

| 表 | 權限 | 會擋啟動 | 需要的欄位 | `380_ac.sql` 參考傾印 |
|---|---|---|---|---|
| `accounts` | SELECT | 是 | `access_level`、`banned`、`login`、`password`、`warehouse` | 齊全 |
| `character_buddys` | SELECT | 是 | `buddy_id`、`buddy_name` | 齊全 |
| `character_buff` | SELECT | 是 | `*`；`char_obj_id`、`poly_id`、`remaining_time`、`skill_id`、`snapshot_version` | 缺 snapshot_version |
| `character_elf_warehouse` | SELECT | 是 | `id` | 齊全 |
| `character_items` | SELECT | 是 | `*`；`attr_enchant_kind`、`attr_enchant_level`、`bless`、`char_id`、`charge_count`、`count`、`durability`、`enchantlvl`、`gamno`、`greater`、`id`、`is_equipped`、`is_id`、`is_sealed`、`item_id`、`item_name`、`magic_stone_item_id`、`magic_stone_level`、`magic_stone_time` | 齊全 |
| `character_items_card` | SELECT | 是 | `card_1`、`card_2`、`card_3`、`card_4`、`card_5`、`item_objid` | 齊全 |
| `character_items_dark` | SELECT | 是 | `add_ac`、`add_bow_dmg`、`add_bow_hit`、`add_bow_redux`、`add_cha`、`add_con`、`add_def_earth`、`add_def_fire`、`add_def_water`、`add_def_wind`、`add_dex`、`add_dmg`、`add_dmg_redux`、`add_er`、`add_hit`、`add_hp`、`add_hp_regen`、`add_ignore_reduction`、`add_int`、`add_magic_dmg_modifier`、`add_magic_hit`、`add_melee_redux`、`add_mp`、`add_mp_regen`、`add_mr`、`add_potion_recovery`、`add_regist_blind`、`add_regist_freeze`、`add_regist_sleep`、`add_regist_stone`、`add_regist_stun`、`add_regist_sustain`、`add_sp`、`add_str`、`add_stun_success`、`add_weight_reduction`、`add_wis`、`item_objid` | 齊全 |
| `character_items_time` | SELECT | 是 | `itemr_obj_id` | 齊全 |
| `character_letter` | SELECT | 是 | `code`、`content`、`date`、`isCheck`、`item_object_id`、`receiver`、`sender`、`subject`、`template_id` | 齊全 |
| `character_lito` | SELECT/INSERT/UPDATE | 是 | `char_name`、`char_obj_id`、`disconnects`、`last_result_id`、`losses`、`monthly_losses`、`monthly_period`、`monthly_ranking_points`、`monthly_wins`、`ranking_points`、`updated_at`、`version`、`wins` | 缺 disconnects、monthly_losses、monthly_period、monthly_ranking_points、monthly_wins、ranking_points |
| `character_other` | SELECT | 是 | `char_obj_id`、`deathCount`、`killCount` | 齊全 |
| `character_pets` | SELECT | 是 | `exp`、`hp`、`item_obj_id`、`lawful`、`lvl`、`mp`、`name`、`npcid`、`objid` | 齊全 |
| `character_quests` | SELECT | 是 | `quest_id`、`quest_step` | 齊全 |
| `character_skills` | SELECT | 是 | `char_obj_id`、`skill_id` | 齊全 |
| `character_teleport` | SELECT | 是 | `char_id`、`id`、`locx`、`locy`、`mapid`、`name` | 齊全 |
| `character_warehouse` | SELECT | 是 | `id` | 齊全 |
| `characters` | SELECT | 是 | `*`；`Ac`、`AccessLevel`、`Banned`、`Cha`、`ClanID`、`ClanRank`、`Clanname`、`Class`、`Con`、`Dex`、`ElfAttr`、`Exp`、`Food`、`HellTime`、`Intel`、`LastPk`、`LastPkForElf`、`Lawful`、`LocX`、`LocY`、`MapID`、`MaxHp`、`MaxMp`、`OriginalCon`、`OriginalDex`、`OriginalInt`、`OriginalStr`、`OriginalWis`、`PKcount`、`PartnerID`、`PkCountForElf`、`Sex`、`Str`、`Title`、`Type`、`Wis`、`account_name`、`char_name`、`clanNote`、`level`、`objid` | 齊全 |
| `clan_data` | SELECT | 是 | `Announcement`、`ClanBirthDay`、`clan_id`、`clan_name`、`hascastle`、`hashouse`、`leader_id`、`leader_name`、`max_online_user`、`showEmblem` | 齊全 |
| `clan_emblem` | SELECT | 否 | `clan_id`、`emblem_id`、`emblemicon` | 齊全 |
| `clan_warehouse` | SELECT | 是 | `id` | 齊全 |
| `server_城堡` | SELECT | 是 | `*`；`castle_id`、`count`、`item_id`、`name`、`note`、`public_money`、`tax_rate`、`war_duration`、`war_interval`、`war_time` | 齊全 |
| `server_盟屋_佈告` | SELECT | 是 | `content`、`date`、`id`、`name`、`title` | 齊全 |
| `server_盟屋_拍賣` | SELECT | 是 | （只檢查表存在） | 齊全 |
| `server_盟屋_紀錄` | SELECT | 否 | `house_area`、`house_id`、`house_name`、`is_on_sale`、`is_purchase_basement`、`location`、`tax_deadline` | 齊全 |
| `server_紀錄_獎勵`（選用） | SELECT | 否 | `強化值`、`流水號`、`玩家帳號`、`發送物品數量`、`發送物品編號`、`領取狀態` | 缺表 |

靜態庫與控制庫的同類清單在 `tools/acceptance/generated/m4-required-columns.tsv`（三個庫別全部列出），`m4-db-verify.sql` 一併檢查。

## 7. 判讀補充

- 靜態庫：`380_db.sql` 快照對新 JAR 需要的 56 張表與欄位**沒有缺口**，包含 #335 提到的 `spawnlist_time`、`系統_受擊無動作gfx`，以及 #284 第二輪卡住的 `裝備_armor_魔法.id`。也就是 #284 02:43 裁定的做法（由快照匯入 `p24_380db_lito_exp`）在結構上可行；資料內容是否讓各載入器的值域檢查通過，要實際啟動才知道（待驗證）。
- 控制庫：`380_lito.sql` 快照結構沒有缺口，但 `commands` 只有 5 列、名稱是 `1`／`2`／`3`／`de`／`loc`，沒有 `polyme`、`move`、`item`、`spawn`；`lito_source_server` 指向正式來源庫名。兩者都由準備腳本在實驗庫內處理。`p24_lito_lito_exp`（#83 由 P24 的 `p24_lito` 複製）沒有傾印可比，以 `m4-db-verify.sql` 的 V2、V4、V5、V6 為準。
- 角色庫：`380_ac.sql` 只是參考。`p24_accounts_lito_exp` 的實際結構（特別是 `character_lito` 的 7 個後加欄位，以及 §6 列出的各表欄位）要在取得 lease、啟動 P24Lab 後用 `m4-db-verify.sql` 查。
- 欄位名稱比對不分大小寫（同 MySQL）。`SELECT *` 的表只能檢查程式以 `getXxx("欄位")` 取用的欄位；以欄位序號取值的地方不在靜態比對範圍內。
- 本檢查不涵蓋：資料列的值域（例如各載入器對數值範圍的檢查）、索引、字元集／定序、資料量。
