# LiTo 資料庫規格（多來源、權限清單、欄位對照）

T104／Issue #302。更新：2026-11-11。對應差距矩陣 `docs/reports/server-capability-matrix.md`「區塊三：資料庫」與 customer-gaps G01。
本文是正式 0.6.71（共用來源、`legacy_mysql`、來源唯讀）部署的資料庫權威說明；舊文件說法與本文不同時以本文為準。

**路徑縮寫**
- `S/` = `lito伺服器/LiTo381-Server/src/main/java/local/lito381/server/`
- `D/` = `lito伺服器/LiTo381-Server/`
- `1-伺服器/…` = `D:\lineage開發\1-伺服器` 下的相對路徑（唯讀參考，未複製原始碼）
- `T/` = `tools/db-grants/`

**證據標記**：［已驗證］讀過程式／檔案並核對行號；［推測］由程式推論、未執行；［待驗證］需要連 DB 或實機。

---

## 1. 三類資料庫

| 類別 | 設定來源 | 預設名 | LiTo 權限 | 連線方式 |
|---|---|---|---|---|
| 靜態庫 | `server.properties` 的 `database.static.database` | `380_db` | 只讀 | 各載入器以 `DriverManager` 開唯讀連線（例：`S/NativeProcHealBonus.java:54-59`、`S/NativeArmorProc.java:31-32`）；角色庫連線池內以 `resourceDatabase.table` 跨庫讀（`S/Accounts.java:12932-12937`） |
| LiTo 控制庫 | `database.lito.database` | `380_lito` | 只讀；`lito_battle_settlement` 另可 INSERT／UPDATE | `S/UnifiedConfiguration.java:49-51`、`S/ResourceDatabase.java:21-28`；收據表以 `` `<控制庫>`.`lito_battle_settlement` `` 全名存取（`S/SettlementSql.java:6`） |
| 來源角色庫 | `380_lito.lito_source_server.character_database`（每個 `enabled=1` 列一個） | `380_ac`、`380_ac_1`… | 只讀；`character_lito` 另可 INSERT／UPDATE | 每個來源一個 HikariCP 池，`pool.setReadOnly(readOnly)`（`S/Accounts.java:4234`），共用模式固定 `legacy_mysql`＋唯讀（`S/SharedDatabase.java:39-52`）；結算另用 `Route.connection()`（`S/SharedDatabase.java:162-168`） |

- 三類庫使用**同一組帳密**（`database.user`／`database.password`），只換庫名（`S/UnifiedConfiguration.java:40-43`、`S/SharedDatabase.java:164-166`）。所以 GRANT 給同一個帳號。［已驗證］
- 6 張玩法控制表 `lito_battle_rule`、`lito_battle_object`、`lito_battle_spawn`、`lito_battle_player_spawn`、`lito_tournament_shop`、`commands` 在正式 JAR 由 shared-database patch 導向控制庫（`S/SharedDatabase.java:21-22,64-68`）。`src/main` 的 `Accounts.resourceTable` 還沒接這個掛鉤（矩陣 X14），本文以正式 JAR 行為為準。［已驗證：程式；導向行為依 patch 原始碼推論］
- 天堂角色資料唯讀，LiTo 只寫兩張表：來源庫 `character_lito`、控制庫 `lito_battle_settlement`（`S/SharedDatabase.java:9-12,101-102`、`S/SettlementSql.java:14,38-39,42`）。［已驗證］

## 2. 權限清單怎麼產生

清單**由程式掃描產生**，不再手列。流程（全部是靜態分析，不連 DB、不跑 Java）：

```
py -3 tools/db-grants/scan_db_access.py          # 掃 S/ 全部 947 個 .java → T/generated/db-access.{json,tsv}
py -3 tools/db-grants/gen_grants.py --update-doc docs/design/database.md
                                                   # 套用 T/review.json → grants.sql、table-inventory.tsv、excluded.tsv，並更新本文第 3、4 節
py -3 tools/db-grants/jar_tables.py <正式 JAR>      # 交叉檢查：正式 JAR 常數池引用的表
```

1. **SQL 字串**：解析 Java 字串字面值（含 `\uXXXX`、text block），同一敘述的字面值依序串接，抓 `FROM／JOIN／INTO／UPDATE` 後的表名、SELECT／INSERT／UPDATE 欄位；`%s` 依 `.formatted()` 參數代入。
2. **表名白名單**：`D/db/*.sql` 傾印裡的表名（`lito_static.sql`＝380_db 快照 183 表、`380_ac_1.sql`＝380_ac 快照 86 表、`lito_*.sql`＝380_lito 19 表）。字面值剛好等於白名單表名、且同敘述有 `checkTable／resourceTable／*TABLE*` 等表名參數提示時也算一次讀取（例如 `checkTable(..., "accounts")`）。
3. **人工核對**（`T/review.json`，每項附行號）：排除 `Modern*`（另一個入口 `ModernStandaloneMain`，`D/pom.xml:141-151`）、`lito_mysql` 限定區段（`S/Accounts.java:4361-4495,4498-`）、`lito_mysql` 擴充表 16 張、只是字串相同的誤判（例如 `"armor".equals(kind)`、`exp` 欄位名）；寫入只放行 `character_lito`、`lito_battle_settlement`；補上 src/main 看不到、正式 JAR 才有的讀取。
4. **正式 JAR 交叉檢查**：讀 `lito登入器/LiTo381-Launcher/app/server/LiTo381-SharedResources-candidate.jar`（SHA-256 `E1498871…D498`，與 `lito登入器/LiTo381-Launcher/docs/maintenance/release-0.6.71.json` 一致）的 class 常數池。結果：JAR 引用 118 個白名單表名，扣掉已列入的 98 張，其餘 20 個全部能解釋——16 張 `lito_mysql` 擴充表、`character_buff_snapshot`（條件分支，見 §5）、`server_info`（只有寫入）、`exp`／`petitem`（欄位名或道具類型字串，不是表）。這一步抓到兩張 src/main 掃不到的表：`skills_魔屏無效技能`（修正掃描後 src/main `S/Accounts.java:10350` 也抓得到）、`道具_物品詳細說明`（正式 JAR 的 `ClientSession` 才呼叫 `LiToItemTemplates`）。［已驗證］

### 與 #83 實驗授權清單的差異（G01 根因）

#83 逐表授權 80 張（`docs/reports/issue-83.md:30-77`），本清單 98 張，多 18 張：

| 庫 | 多出的表 | 讀取點 | 啟動即讀 |
|---|---|---|---|
| 380_db | `角色_智力成長` | `S/NativeProcHealBonus.java:31`（`ResourceDatabase.load` 第 4 步，`S/ResourceDatabase.java:17`） | 是 → **#284 就是死在這裡** |
| 380_db | `裝備_armor_魔法` | `S/NativeArmorProc.java:40`（`S/ResourceDatabase.java:18`） | 是 |
| 380_db | `spr_action` | `S/NativeDeSpriteCatalog.java:12`（`NativeDeSprites.load`） | 是 |
| 380_db | `假人_人物清單`、`假人_能力設置`、`假人_設置狀態` | `S/NativeDeControlCatalog.java:22-24`（`NativeDeFactoryControls.load`） | 是 |
| 380_db | `mobskill` | `S/NativeDeNpcTemplateMobLoader.java:19-20`（`NativeDeNpcTemplateResources.load`） | 是 |
| 380_db | `mapids_shop` | `S/NativeDeShopTable.java:41`（`NativeDeShopResources.load`） | 是 |
| 380_db | `npcchat` | `S/NativeDeChatTable.java:29`（`NativeDeChatResources.load`） | 是 |
| 380_db | `polymorphs`、`道具_變形控制魔杖` | `S/NativePolymorphSource.java:14,20`；`S/GmPolyMe.java:107` | 否（首次使用） |
| 380_db | `skills_相消怪物轉換` | `S/NativeCancellationNpcRules.java:23` | 否 |
| 380_db | `skills_魔屏無效技能` | `S/Accounts.java:10350` | 否 |
| 380_db | `裝備_weapon_魔法` | `S/NativeDeItemWeaponProcTable.java:31` | 否 |
| 380_db | `道具_物品詳細說明` | 正式 JAR `LiToItemTemplates`（`S/LiToItemTemplates.java:21`），`-Dlito.extendedItemDetails=true`（`lito登入器/LiTo381-Launcher/src/FormalStandardSources.cs:37`） | 否；權限被拒時物品詳細說明整體停用（`S/LiToItemTemplates.java:23,26`） |
| 380_ac | `clan_emblem` | `S/Accounts.java:11394` | 否 |
| 380_ac | `server_盟屋_紀錄` | `S/Accounts.java:8687,8703,8724` | 否 |
| 380_ac | `server_紀錄_獎勵`（選用） | `S/Accounts.java:12039,12494,12837` | 否 |

`ResourceDatabase.load`（`S/ResourceDatabase.java:13-81`）依序載入 13 個目錄，任何一個 SELECT 被拒就以 exit 1 結束；#284 只走到第 4 個，所以上表**其餘 8 張「啟動即讀」的表在 #284 的授權下也一定會失敗**，只是還沒輪到。［已驗證：程式順序；推測：補齊後能通過，待驗證］

## 3. 表與欄位清單（掃描產生）

「啟動即讀」：伺服器啟動（健康檢查、`ResourceDatabase.load` 鏈、`UnifiedConfiguration`、`NativeRoomRules.install`、結算 `check`）就會讀，缺權限即啟動失敗；「否」＝首次用到時才讀，缺權限是執行期錯誤。欄位是掃描 SQL 字串與 `rs.getXxx("欄")` 得到、且確實存在於 repo 傾印的欄位；`SELECT *` 會標出來。完整逐筆紀錄（含每個檔:行）在 `T/generated/db-access.tsv`，排除項與理由在 `T/generated/excluded.tsv`。

<!-- BEGIN GENERATED:inventory -->
| 庫 | 表 | 權限 | 啟動即讀 | 讀取／寫入欄位（掃描） | 證據 |
|---|---|---|---|---|---|
| 380_db | `armor` | SELECT | 是 | 全部欄位（SELECT *）；程式取用：ac、add_adena_rate、add_boss_dmg、add_boss_redux、add_bow_dmgup、add_bow_hitup、add_bow_redux、add_cha、add_con、add_dex、add_dmgup、add_dodge、add_drop_rate、add_er、add_exp_rate、add_hitup、add_hp、add_hpr、add_ignore_reduction、add_int、add_magic_hit、add_melee_redux、add_mp、add_mpr、add_poison_resist、add_potion_recovery、add_pvp_dmg、add_pvp_redux、add_sp、add_str、add_stun_success、add_wis、bless、bow_dmg_modifier、bow_hit_modifier、cant_delete、classname、damage_reduction、defense_earth、defense_fire、defense_water、defense_wind、dmg_modifier、grdgfx、greater_tier、haste_item、hit_modifier、invgfx、is_activity、item_id、itemdesc_id、m_def、magicdmgmodifier、material、max_lvl、max_use_time、min_lvl、name、name_id、note、regist_blind、regist_freeze、regist_sleep、regist_stone、regist_stun、regist_sustain、safenchant、set_id、trade、type、use_darkelf、use_dragonknight、use_elf、use_illusionist、use_knight、use_mage、use_royal、weight、weight_reduction | LegacyEquipmentMagicLoader.java:38、LegacyEquipmentMagicLoader.java:122、LegacyInventoryWeightLoader.java:30… |
| 380_db | `etcitem` | SELECT | 是 | 全部欄位（SELECT *）；程式取用：bless、cant_delete、classname、delay_effect、delay_id、delay_time、food_volume、grdgfx、invgfx、item_id、item_type、itemdesc_id、limit_class、material、max_charge_count、max_lvl、min_lvl、name、name_id、note、save_at_once、stackable、trade、use_type、weight | LegacyEquipmentMagicLoader.java:38、LegacyEquipmentMagicLoader.java:130、LegacyInventoryWeightLoader.java:44… |
| 380_db | `etcitem_arrow` | SELECT | 是 | arrow_spr、consume_on_attack、dmg_large、dmg_small、item_id | LegacyEquipmentMagicLoader.java:57、LegacyEquipmentMagicLoader.java:158、LegacyEquipmentMagicLoader.java:161… |
| 380_db | `etcitem_buff藥水能力` | SELECT | 是 | 全部欄位（SELECT *）；程式取用：item_id | SourceEyeControls.java:40 |
| 380_db | `etcitem_buff藥水設置` | SELECT | 是 | 全部欄位（SELECT *）；程式取用：buff_id、buff_time_sec、can_overwrite、cast_gfx、consume_count、consume_item_id、end_msg_str、group_check、item_id、status_icon、use_custom_packet | SourceEyeControls.java:20 |
| 380_db | `etcitem_doll_power_new` | SELECT | 否 | attr_type、classname、gfx_id、gfx_target、id、interval_time、item_count、item_id、mode、note、passive_skill_id、probability、status_id、trigger_skill_id、value_main、value_sub | Accounts.java:13185 |
| 380_db | `etcitem_doll_type` | SELECT | 是 | count、doll_type、gfxid、icon、itemid、nameid、need、powers、time | 人工：同上，sourceInventorySql() 在 Accounts.java:13140-13143 JOIN 此表 |
| 380_db | `mapids` | SELECT | 是 | 全部欄位（SELECT *）；程式取用：CopyMapId、drop_rate、endX、endY、escapable、is_arrows、is_guaji、locationname、mapid、markable、monster_amount、painwand、penalty、recall_pets、resurrection、startX、startY、take_pets、teleportable、underwater、usable_item、usable_shop、usable_skill | NativeDeTerrainTable.java:20、SourceBlessingControls.java:32 |
| 380_db | `mapids_shop` | SELECT | 是 | currency、mapid、x_max、x_min、y_max、y_min | NativeDeShopTable.java:41 |
| 380_db | `mobskill` | SELECT | 是 | 全部欄位（SELECT *）；程式取用：AreaHeight、AreaWidth、ChangeTarget、Gfxid、Leverage、PolyId、Range、SkillId、SummonId、SummonMax、SummonMin、TriCompanionHp、TriCount、TriHp、TriRange、TriRnd、actNo、mobid、mobname、reuseDelay | NativeDeNpcTemplateMobLoader.java:19、NativeDeNpcTemplateMobLoader.java:20 |
| 380_db | `npc` | SELECT | 是 | 全部欄位（SELECT *）；程式取用：IsErase、IsTU、ac、agro、agrocoi、agrofamily、agrogfxid1、agrogfxid2、agrososc、amount_fixed、atk_magic_speed、atkspeed、boss、bowActId、bravespeed、cant_resurrect、change_head、classname、con、damage_reduction、dex、digestitem、doppel、exp、family、gfxid、hard、hp、hpr、hprinterval、impl、intel、karma、lawful、light_size、lvl、mp、mpr、mprinterval、mr、name、nameid、npcid、paralysis_atk、passispeed、picupitem、poison_atk、randomac、randomexp、randomhp、randomlawful、randomlevel、randommp、ranged、size、str、sub_magic_speed、tamable、teleport、transform_gfxid、transform_id、undead、weakAttr、wis | Accounts.java:4300、LegacyEquipmentMagicLoader.java:38、NativeDeNpcTemplateNpcLoader.java:27… |
| 380_db | `npcchat` | SELECT | 是 | 全部欄位（SELECT *）；程式取用：chat_id1、chat_id2、chat_id3、chat_id4、chat_id5、chat_interval、chat_timing、game_time、is_repeat、is_shout、is_world_chat、npc_id、repeat_interval、start_delay_time | NativeDeChatTable.java:29 |
| 380_db | `pettypes` | SELECT | 是 | BaseNpcId、ChaCost、CollarId、EvolutionItemId、HpUpMax、HpUpMin、ItemIdForTaming、MinEvoLevel、MpUpMax、MpUpMin、NpcIdForEvolving、TameRate | Accounts.java:4269、Accounts.java:4344 |
| 380_db | `polymorphs` | SELECT | 否 | 全部欄位（SELECT *）；程式取用：armorequip、cause、isSkillUse、minlevel、name、polyid、weaponequip | GmPolyMe.java:107、NativePolymorphSource.java:20 |
| 380_db | `server_event` | SELECT | 是 | eventstart | SourceSkillControls.java:79 |
| 380_db | `skills` | SELECT | 是 | buffDuration、castgfx、damage_dice、damage_dice_count、damage_value、hpConsume、itemConsumeCount、itemConsumeId、mpConsume、name、probability_dice、probability_value、reuseDelay、skill_id、skill_level、type | Accounts.java:4267、CombatDatabase.java:59、NativeDeNpcTemplateResources.java:107… |
| 380_db | `skills_buff_ability` | SELECT | 是 | ability_type、ability_value、skill_id | SourceSkillControls.java:104 |
| 380_db | `skills_相消免疫技能` | SELECT | 是 | skill_id | SourceSkillControls.java:82 |
| 380_db | `skills_相消怪物轉換` | SELECT | 否 | duration_seconds、new_name_id、new_passispeed、npc_id、target_id、transform_type | NativeCancellationNpcRules.java:23 |
| 380_db | `skills_負面boss免疫` | SELECT | 是 | modifier、skill_id | SourceSkillControls.java:97 |
| 380_db | `skills_負面技能控制` | SELECT | 是 | bonus_dex、bonus_int、bonus_str、check_magic_hit、check_magic_level、check_resist_type、con_multiplier、mr_multiplier、prob_equal、prob_high、prob_low、skill_id、wis_multiplier | SourceSkillControls.java:85 |
| 380_db | `skills_魔屏無效技能` | SELECT | 否 | skill_id | Accounts.java:10350 |
| 380_db | `spawnlist_npc` | SELECT | 是 | 全部欄位（SELECT *）；程式取用：count、heading、id、locx、locy、mapid、movement_distance、npc_templateid、randomx、randomy | SourceBlessingControls.java:18 |
| 380_db | `spr_action` | SELECT | 是 | 全部欄位（SELECT *）；程式取用：act_id、framecount、framerate、spr_id | NativeDeSpriteCatalog.java:12 |
| 380_db | `weapon` | SELECT | 是 | 全部欄位（SELECT *）；程式取用：add_adena_rate、add_boss_dmg、add_boss_redux、add_bow_dmgup、add_bow_hitup、add_bow_redux、add_cha、add_con、add_dex、add_dmgup、add_dodge、add_drop_rate、add_er、add_exp_rate、add_hitup、add_hp、add_hpr、add_ignore_reduction、add_int、add_magic_hit、add_melee_redux、add_mp、add_mpr、add_pierce、add_poison_resist、add_potion_recovery、add_pvp_dmg、add_pvp_redux、add_sp、add_str、add_stun_success、add_wis、arrow_spr、bless、canbedmg、cant_delete、classname、dmg_cri、dmg_cri_chance、dmg_cri_effect、dmg_large、dmg_max_chance、dmg_max_effect、dmg_small、dmgmodifier、grdgfx、greater_tier、haste_item、hitmodifier、invgfx、item_id、itemdesc_id、m_def、magic_bow、magicdmgmodifier、material、max_lvl、max_use_time、min_lvl、name、name_id、range、safenchant、set_id、trade、type、use_darkelf、use_dragonknight、use_elf、use_illusionist、use_knight、use_mage、use_royal、weight | LegacyEquipmentMagicLoader.java:38、LegacyEquipmentMagicLoader.java:128、LegacyInventoryWeightLoader.java:30… |
| 380_db | `假人_人物清單` | SELECT | 是 | 全部欄位（SELECT *）；程式取用：clanid、class_id、denamebig5、deobjid、deshopbig5、detitlebig5、sex、weapon_type | NativeDeControlCatalog.java:22、NativeDeControlCatalog.java:62 |
| 380_db | `假人_能力設置` | SELECT | 是 | 全部欄位（SELECT *）；程式取用：class_id | NativeDeControlCatalog.java:23 |
| 380_db | `假人_設置狀態` | SELECT | 是 | 全部欄位（SELECT *）；程式取用：De_pet、akk_skill、class_id、doll_list、id、poly_list、use_item_Gfx、use_skill、weapon_type | NativeDeControlCatalog.java:24、NativeDeControlCatalog.java:177 |
| 380_db | `系統_劇情任務` | SELECT | 是 | action_name、event_type、id | SourceBlessingControls.java:14 |
| 380_db | `裝備_armor_bless_指定` | SELECT | 是 | 全部欄位（SELECT *）；程式取用：add_ac、bless_id、item_id | LegacyEquipmentMagicLoader.java:33、LegacyEquipmentMagicLoader.java:41… |
| 380_db | `裝備_armor_bless_類型` | SELECT | 是 | 全部欄位（SELECT *）；程式取用：add_ac、armor_type、bless_id | LegacyEquipmentMagicLoader.java:32、LegacyEquipmentMagicLoader.java:40… |
| 380_db | `裝備_armor_enchant_指定` | SELECT | 是 | 全部欄位（SELECT *）；程式取用：add_ac、enchant_level、item_id | LegacyEquipmentMagicLoader.java:34、LegacyEquipmentMagicLoader.java:42… |
| 380_db | `裝備_armor_enchant_類型` | SELECT | 是 | 全部欄位（SELECT *）；程式取用：add_ac、armor_type、over_enchant | LegacyEquipmentMagicLoader.java:35、LegacyEquipmentMagicLoader.java:43… |
| 380_db | `裝備_armor_greater_類型` | SELECT | 是 | 全部欄位（SELECT *）；程式取用：add_ac、armor_type、tier_id | LegacyEquipmentMagicLoader.java:31、LegacyEquipmentMagicLoader.java:39… |
| 380_db | `裝備_armor_魔法` | SELECT | 是 | 全部欄位（SELECT *）；程式取用：enchant_level、flat_damage、gfx_id、gfx_mode、heal_hp、heal_hp_rand、heal_mode、heal_mp、heal_mp_rand、id、item_id、on_attack_chance、on_hurt_chance、proc_group、rand_damage、skill_target、skill_time_sec、trigger_skill | NativeArmorProc.java:40 |
| 380_db | `裝備_weapon_bless_指定` | SELECT | 是 | 全部欄位（SELECT *）；程式取用：bless_id、item_id | LegacyEquipmentMagicLoader.java:33、LegacyEquipmentMagicLoader.java:41… |
| 380_db | `裝備_weapon_bless_類型` | SELECT | 是 | 全部欄位（SELECT *）；程式取用：add_ac、bless_id、weapon_type | LegacyEquipmentMagicLoader.java:32、LegacyEquipmentMagicLoader.java:40… |
| 380_db | `裝備_weapon_enchant_指定` | SELECT | 是 | 全部欄位（SELECT *）；程式取用：add_ac、enchant_level、item_id | LegacyEquipmentMagicLoader.java:34、LegacyEquipmentMagicLoader.java:42… |
| 380_db | `裝備_weapon_enchant_類型` | SELECT | 是 | 全部欄位（SELECT *）；程式取用：add_ac、over_enchant、weapon_type | LegacyEquipmentMagicLoader.java:35、LegacyEquipmentMagicLoader.java:43… |
| 380_db | `裝備_weapon_greater_類型` | SELECT | 是 | 全部欄位（SELECT *）；程式取用：tier_id、weapon_type | LegacyEquipmentMagicLoader.java:31、LegacyEquipmentMagicLoader.java:39… |
| 380_db | `裝備_weapon_屬性` | SELECT | 是 | 全部欄位（SELECT *）；程式取用：chance、id、name、probability、stage、type_add_dmg_max、type_add_dmg_min、type_dmg_passive、type_dmgup、type_drain_hp_max、type_drain_hp_min、type_drain_mp_max、type_drain_mp_min、type_gfx_id、type_range、type_range_dmg | LegacyEquipmentMagicLoader.java:38、LegacyEquipmentMagicLoader.java:132… |
| 380_db | `裝備_weapon_魔法` | SELECT | 否 | 全部欄位（SELECT *）；程式取用：area_size、description、dmg_attr、dmg_fix、dmg_rnd、dmg_scaling、dmg_type、enchant_step、gfx_id、gfx_target、hp_drain_fix、hp_drain_rnd、id、min_enchant、mp_drain_fix、mp_drain_rnd、prob_base、prob_enchant、skill_id、skill_target、skill_time_sec、weapon_id | NativeDeItemDefinitionsSql.java:21、NativeDeItemWeaponProcTable.java:31 |
| 380_db | `裝備_特殊_卡片_組合` | SELECT | 是 | add_ac、add_bow_dmg、add_bow_hit、add_bow_redux、add_cha、add_con、add_dex、add_dmg、add_earth、add_er、add_fire、add_hit、add_hp、add_hpr、add_ignore_reduction、add_int、add_magic_dmg、add_magic_hit、add_melee_redux、add_mp、add_mpr、add_mr、add_pierce、add_potion_recovery、add_pvp_redux、add_reduction、add_str、add_stun_success、add_water、add_weight_reduction、add_wind、add_wis、regist_blind、regist_freeze、regist_sleep、regist_stone、regist_stun、regist_sustain、require_cards | LegacyEquipmentMagicLoader.java:44、LegacyEquipmentMagicLoader.java:151 |
| 380_db | `裝備_特殊_卡片_能力` | SELECT | 是 | 全部欄位（SELECT *）；程式取用：item_id | LegacyEquipmentMagicLoader.java:36、LegacyEquipmentMagicLoader.java:44… |
| 380_db | `裝備_特殊_套裝_能力` | SELECT | 是 | ac、add_bow_dmgup、add_bow_hitup、add_cha、add_con、add_dex、add_dmgup、add_er、add_hitup、add_hp、add_hpr、add_ignore_reduction、add_int、add_magic_hit、add_mp、add_mpr、add_pierce、add_potion_recovery、add_pvp_redux、add_str、add_stun_success、add_wis、damage_reduction、defense_earth、defense_fire、defense_water、defense_wind、m_def、magic_dmg_modifier、regist_blind、regist_freeze、regist_sleep、regist_stone、regist_stun、regist_sustain、weight_reduction | LegacyEquipmentMagicLoader.java:45、LegacyEquipmentMagicLoader.java:146 |
| 380_db | `裝備_特殊_魔石_魔法` | SELECT | 是 | 全部欄位（SELECT *）；程式取用：add_con、add_dex、add_hp、add_int、add_mp、add_mr、add_sp、add_str、add_wis、area_size、dmg_attr、dmg_fix、dmg_rnd、dmg_type、duration、gfx_id、hp_drain、item_id、mp_drain、name_suffix、prob_fail_mode、prob_success、proc_chance、skill_id、tier | LegacyEquipmentMagicLoader.java:45、LegacyEquipmentMagicLoader.java:141… |
| 380_db | `角色_力量成長` | SELECT | 是 | — | Accounts.java:4354 |
| 380_db | `角色_敏捷成長` | SELECT | 是 | — | Accounts.java:4356 |
| 380_db | `角色_智力成長` | SELECT | 是 | 全部欄位（SELECT *）；程式取用：id、治癒骰加成、魔法加成 | NativeProcHealBonus.java:31 |
| 380_db | `角色_精神成長` | SELECT | 是 | id、魔防 | Accounts.java:4350 |
| 380_db | `角色_體質成長` | SELECT | 是 | id、回血 | Accounts.java:4352 |
| 380_db | `道具_物品詳細說明` | SELECT（人工補充） | 否 | — | 人工補充：正式 JAR 的 ClientSession 引用 LiToItemTemplates（jar_tables.py／常數池），src/main 看不到呼叫點；讀取 LiToItemTemplates.java:21，僅在 -Dlito.extendedItemDetails=true（lito登入器/LiTo381-Launcher/src/FormalStandardSources.cs:37）時預載；缺表（42S02）可容忍，權限拒絕會讓物品詳細說明整體停用（LiToItemTemplates.java:23,26） |
| 380_db | `道具_自訂穿脫能力` | SELECT | 是 | 力量、回血、回魔、地屬性防禦、寒冰耐性、抗魔、支撐耐性、敏捷、昏迷耐性、智力、暗黑耐性、水屬性防禦、火屬性防禦、睡眠耐性、石化耐性、精神、近戰傷害、近戰命中、遠攻傷害、遠攻命中、風屬性防禦、體質、魔攻 | LegacyEquipmentMagicLoader.java:53、LegacyEquipmentMagicLoader.java:152、LegacyEquipmentMagicLoader.java:156… |
| 380_db | `道具_變形控制魔杖` | SELECT | 否 | 全部欄位（SELECT *）；程式取用：chance、name、poly_id | NativePolymorphSource.java:14 |
| 380_lito | `commands` | SELECT | 是 | access_level、class_name、name、note、system | Accounts.java:4271 |
| 380_lito | `lito_battle_object` | SELECT | 是 | gfx、map_id、maximum_hp、mode_id、role、x、y | Accounts.java:4277 |
| 380_lito | `lito_battle_player_spawn` | SELECT | 是 | map_id、mode_id、spawn_index、x、y | Accounts.java:4281 |
| 380_lito | `lito_battle_rule` | SELECT | 是 | enabled、goal、map_id、minimum_players、mode_id、time_limit_seconds | Accounts.java:4275 |
| 380_lito | `lito_battle_settlement` | SELECT/INSERT/UPDATE | 是 | applied、applied_at、battle_id、char_name、char_obj_id、disconnected、result_id、source_database、source_name、won | SettlementSql.java:7… |
| 380_lito | `lito_battle_spawn` | SELECT | 是 | map_id、mode_id、npc_id、quantity、sort_order、wave、x、y | Accounts.java:4279 |
| 380_lito | `lito_resource_buff` | SELECT | 是 | packet_opcode、packet_selector、skill_id | ResourceDatabase.java:48 |
| 380_lito | `lito_resource_map` | SELECT | 是 | category、map_id、name、raw_slot | ResourceDatabase.java:51 |
| 380_lito | `lito_resource_map_page` | SELECT | 是 | wire_page | ResourceDatabase.java:58 |
| 380_lito | `lito_resource_npc_identity` | SELECT | 是 | description、resource_index | ResourceDatabase.java:57 |
| 380_lito | `lito_resource_pending` | SELECT | 是 | archive_ref、category、evidence_note、evidence_status、record_key、source_table | ResourceDatabase.java:40 |
| 380_lito | `lito_resource_portal` | SELECT | 是 | raw_tag、source_map_id、source_x、source_y、target_map_id、target_x、target_y | ResourceDatabase.java:59 |
| 380_lito | `lito_resource_projectile` | SELECT | 是 | flight_type、packet_opcode、projectile_gfx | ResourceDatabase.java:68 |
| 380_lito | `lito_resource_release` | SELECT | 是 | qualified_version | ResourceDatabase.java:29 |
| 380_lito | `lito_resource_skill_value` | SELECT | 是 | field_name、record_type、skill_id、value | ResourceDatabase.java:41 |
| 380_lito | `lito_resource_zone` | SELECT | 是 | blue、color_index、green、map_id、max_x、max_y、min_x、min_y、red、zone_key | ResourceDatabase.java:54 |
| 380_lito | `lito_room_policy` | SELECT | 是 | policy_id、test_bot_enabled、test_room_enabled | NativeRoomRules.java:20 |
| 380_lito | `lito_source_server` | SELECT | 是 | character_database、display_name、enabled、source_id | UnifiedConfiguration.java:51 |
| 380_lito | `lito_tournament_shop` | SELECT | 是 | buy_price、item_id、sell_price、sort_order | Accounts.java:4273 |
| 380_ac | `accounts` | SELECT | 是 | access_level、banned、login、password、warehouse | Accounts.java:4250… |
| 380_ac | `character_buddys` | SELECT | 是 | buddy_id、buddy_name | Accounts.java:4259 |
| 380_ac | `character_buff` | SELECT | 是 | 全部欄位（SELECT *）；程式取用：char_obj_id、poly_id、remaining_time、skill_id、snapshot_version | Accounts.java:4258、Accounts.java:4334… |
| 380_ac | `character_elf_warehouse` | SELECT | 是 | id | Accounts.java:4262 |
| 380_ac | `character_items` | SELECT | 是 | 全部欄位（SELECT *）；程式取用：attr_enchant_kind、attr_enchant_level、bless、char_id、charge_count、count、durability、enchantlvl、gamno、greater、id、is_equipped、is_id、is_sealed、item_id、item_name、magic_stone_item_id、magic_stone_level、magic_stone_time | Accounts.java:4255、Accounts.java:4309、LegacyEquipmentMagicLoader.java:116… |
| 380_ac | `character_items_card` | SELECT | 是 | card_1、card_2、card_3、card_4、card_5、item_objid | Accounts.java:4264 |
| 380_ac | `character_items_dark` | SELECT | 是 | add_ac、add_bow_dmg、add_bow_hit、add_bow_redux、add_cha、add_con、add_def_earth、add_def_fire、add_def_water、add_def_wind、add_dex、add_dmg、add_dmg_redux、add_er、add_hit、add_hp、add_hp_regen、add_ignore_reduction、add_int、add_magic_dmg_modifier、add_magic_hit、add_melee_redux、add_mp、add_mp_regen、add_mr、add_potion_recovery、add_regist_blind、add_regist_freeze、add_regist_sleep、add_regist_stone、add_regist_stun、add_regist_sustain、add_sp、add_str、add_stun_success、add_weight_reduction、add_wis、item_objid | Accounts.java:4265、LegacyEquipmentMagicLoader.java:114 |
| 380_ac | `character_items_time` | SELECT | 是 | itemr_obj_id | 人工：Accounts.java:4313-4314 啟動時跑 sourceInventorySql() + LIMIT 0，該 SQL 在 Accounts.java:13070-13071 JOIN 此表 |
| 380_ac | `character_letter` | SELECT | 是 | code、content、date、isCheck、item_object_id、receiver、sender、subject、template_id | Accounts.java:4260… |
| 380_ac | `character_lito` | SELECT/INSERT/UPDATE | 是 | char_name、char_obj_id、last_result_id、losses、updated_at、version、wins | SettlementSql.java:7、SharedDatabase.java:74… |
| 380_ac | `character_other` | SELECT | 是 | char_obj_id、deathCount、killCount | Accounts.java:4252、Accounts.java:4295 |
| 380_ac | `character_pets` | SELECT | 是 | exp、hp、item_obj_id、lawful、lvl、mp、name、npcid、objid | Accounts.java:4256、Accounts.java:4308… |
| 380_ac | `character_quests` | SELECT | 是 | quest_id、quest_step | Accounts.java:4254 |
| 380_ac | `character_skills` | SELECT | 是 | char_obj_id、skill_id | Accounts.java:4253 |
| 380_ac | `character_teleport` | SELECT | 是 | char_id、id、locx、locy、mapid、name | Accounts.java:4257、Accounts.java:4325 |
| 380_ac | `character_warehouse` | SELECT | 是 | id | Accounts.java:4261 |
| 380_ac | `characters` | SELECT | 是 | 全部欄位（SELECT *）；程式取用：Ac、AccessLevel、Banned、Cha、ClanID、ClanRank、Clanname、Class、Con、Dex、ElfAttr、Exp、Food、HellTime、Intel、LastPk、LastPkForElf、Lawful、LocX、LocY、MapID、MaxHp、MaxMp、OriginalCon、OriginalDex、OriginalInt、OriginalStr、OriginalWis、PKcount、PartnerID、PkCountForElf、Sex、Str、Title、Type、Wis、account_name、char_name、clanNote、level、objid | Accounts.java:4251、Accounts.java:4290… |
| 380_ac | `clan_data` | SELECT | 是 | Announcement、ClanBirthDay、clan_id、clan_name、hascastle、hashouse、leader_id、leader_name、max_online_user、showEmblem | Accounts.java:4357… |
| 380_ac | `clan_emblem` | SELECT | 否 | clan_id、emblem_id、emblemicon | Accounts.java:11394 |
| 380_ac | `clan_warehouse` | SELECT | 是 | id | Accounts.java:4263 |
| 380_ac | `server_城堡` | SELECT | 是 | 全部欄位（SELECT *）；程式取用：castle_id、count、item_id、name、note、public_money、tax_rate、war_duration、war_interval、war_time | Accounts.java:4358… |
| 380_ac | `server_盟屋_佈告` | SELECT | 是 | content、date、id、name、title | Accounts.java:4360… |
| 380_ac | `server_盟屋_拍賣` | SELECT | 是 | — | Accounts.java:4359 |
| 380_ac | `server_盟屋_紀錄` | SELECT | 否 | house_area、house_id、house_name、is_on_sale、is_purchase_basement、location、tax_deadline | Accounts.java:8687、Accounts.java:8703、Accounts.java:8724… |
| 380_ac | `server_紀錄_獎勵` | SELECT（選用） | 否 | 強化值、流水號、玩家帳號、發送物品數量、發送物品編號、領取狀態 | Accounts.java:12039、Accounts.java:12494、Accounts.java:12837 |
<!-- END GENERATED:inventory -->

## 4. 部署用 GRANT 範本

只含讀取與 `character_lito`、`lito_battle_settlement` 的 INSERT／UPDATE；沒有 DELETE、DDL、`LOCK TABLES`、`FILE`、`PROCESS`、`GRANT OPTION`，也不用 `db.*` 整庫授權。

- `information_schema` 查詢（`S/SharedDatabase.java:75`、`S/SettlementSql.java:7`、`S/ResourceDatabase.java:36`、`S/LegacyEquipmentMagicLoader.java:720,743`、`S/Accounts.java:4677,4691`）不需另外授權：MySQL 只回傳帳號有權限的物件，正好就是上面授權的表。
- `GET_LOCK`／`RELEASE_LOCK`（`S/SettlementSql.java:28,45`）不需要權限。
- **每個 `enabled=1` 的來源**都要複製一份「來源角色庫」段並換庫名；`character_lito` 必須先有 `last_result_id` 欄（`D/db/lito_battle_settlement.sql:19-27`）。
- 表級 GRANT 遇到不存在的表會失敗，所以 `server_紀錄_獎勵`（1-伺服器 現行 `db/380_ac.sql` 沒有）預設註解。

<!-- BEGIN GENERATED:grants -->
```sql
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
```
<!-- END GENERATED:grants -->

部署後的唯讀驗收（營運或 Codex 在**實驗庫**執行，不要對正式 3306 做寫入測試）：以執行帳號對上面每張表跑 `SELECT * FROM <表> LIMIT 0`，全部要成功；再確認對 `characters` 的 `UPDATE … WHERE 1=0` 被拒（#83 的做法，`docs/reports/issue-83.md:86-97`）。［待驗證］

## 5. 刻意不授權的部分

| 項目 | 為什麼不給 | 證據 |
|---|---|---|
| 角色庫 20 張表的 INSERT／UPDATE／DELETE（`characters`、`character_items`、`clan_data`、`server_城堡`…，完整見 `T/generated/excluded.tsv`） | 寫入程式都在 `readOnly || !lito_mysql` 守門之後，連線池也是唯讀 | `S/Accounts.java:4842,4897,5030,5195,5433,5493,5601,6076`；`S/Accounts.java:4234` |
| 16 張 `lito_*` 擴充表（`lito_character_origin`、`lito_item_origin`、`lito_tournament_profile`…）與 `lito_tournament_character_record` | 只在 `lito_mysql` 模式檢查與使用；正式 380_ac 也不一定有這些表 | `S/Accounts.java:4361-4388,9356-9358` |
| `character_buff_snapshot` | `SourceBuffRead` 先看 `character_buff` 有沒有 `snapshot_version` 欄，沒有就走舊單表分支；380_ac 系列（含 1-伺服器 現行）都沒有此欄 | `S/SourceBuffRead.java:11-26`；`docs/reports/issue-83.md:65` |
| `LOCK TABLES` | 只出現在 `lito_mysql` 寫入路徑；結算用 `GET_LOCK` | 例：`S/Accounts.java:5202-5215` |

若哪天部署庫的 `character_buff` 有 `snapshot_version` 欄，要補 `character_buff_snapshot` 的 SELECT。［推測］

## 6. `lito_source_server` 權威說明

**表結構**（`D/db/lito_source_server.sql:3-11`）［已驗證］

| 欄位 | 型別 | 規則 |
|---|---|---|
| `source_id` | TINYINT UNSIGNED PK | 1～8（CHECK）；固定編號，停用某服也**不能重編**其他服 |
| `display_name` | VARCHAR(45) | 選服畫面名稱；啟用列必須非空、不含逗號與控制字元、能用 MS950 編碼且 ≤ 63 bytes、名稱不分大小寫不重複 |
| `character_database` | VARCHAR(64) UNIQUE，可 NULL | 該來源的角色庫；啟用列必須是 `[A-Za-z0-9_]+` 且不重複；停用列可 NULL |
| `enabled` | TINYINT UNSIGNED，0／1 | 不能是 NULL 或其他值；只載入 `enabled=1` 的列 |

**程式怎麼用**［已驗證］
- 啟動時以唯讀連線讀全部列（查詢逾時 10 秒），檢查上述規則，至少要有 1 個啟用來源，否則拒絕啟動（`S/UnifiedConfiguration.java:47-64`）。
- 啟用列依 `source_id` 排序後轉成 `login.classic.server.names／ids`、`database.dynamic.databases`；所有來源共用 `weight.rate`（`S/UnifiedConfiguration.java:82-85`）。
- 每個來源開一個登入埠：`port`（`server.properties`）＋`source_id`；連到哪個埠就固定用哪個來源，帳號改寫成 `source:<id>:<帳號>`（`S/SourceLogin.java:36-46,88-95`）。
- 修改後**重啟**伺服器／登入器才生效；執行期不重讀。執行帳號只有 SELECT，改表是 SQL 管理者的事。

**種子值（`D/db/lito_source_server.sql:13-21`，權威為此 SQL）**

| source_id | display_name | character_database | enabled |
|---|---|---|---|
| 1 | 太陽神 | 380_ac | 1 |
| 2 | 月亮神 | 380_ac_1 | 1 |
| 3 | 伺服器3 | 380_ac_2 | 1 |
| 4～8 | 伺服器4～伺服器8 | NULL | 0 |

種子是 `INSERT IGNORE`，**已有列不覆寫**；實際部署值由營運在 `380_lito.lito_source_server` 設定，以現場表為準（例如曾出現的「公牛、天使」是舊現場值／舊測試腳本，不是種子）。目前現場啟用哪些來源：［待驗證］（需唯讀查詢現場表）。

**啟用一個新來源的步驟**（例：開第 4 服）
1. 準備角色庫（與 1-伺服器 `db/380_ac.sql` 同結構），確認有 `character_lito` 表。
2. 對該庫執行 `D/db/lito_battle_settlement.sql` 第 19～27 行的 `last_result_id` 遷移（把 `@lito_source` 改成新庫名）。
3. 依 §4 範本為新庫複製一份「來源角色庫」GRANT。
4. 用 SQL 管理帳號：`UPDATE 380_lito.lito_source_server SET display_name='…',character_database='380_ac_3',enabled=1 WHERE source_id=4;`（`D/db/lito_source_server.sql:23-24`）。
5. 確認 `port+4` 沒被占用，重啟伺服器與登入器。

跨來源角色 objid 撞號尚未處理（矩陣 X10，T105）；啟用多個來源前要先看 T105 的結論。

## 7. `character_lito` 引擎：MyISAM 下結算是否正確

**現況**［已驗證］
- 1-伺服器 的 `character_lito` 是 **MyISAM**（`1-伺服器/db/380_ac.sql:983-992`），已有 `last_result_id CHAR(64)`（`:990`）；該庫 70 張表中 55 張是 MyISAM。
- LiTo 接受 InnoDB 或 MyISAM（`S/SharedDatabase.java:75-77`），但收據表 `lito_battle_settlement` **必須** InnoDB（`S/SettlementSql.java:7`）。

**寫入流程**（`S/SettlementSql.java`、`S/DurableBattleSettlement.java`）
1. 戰鬥結束先把結果寫本機 WAL 並 `fsync`（`S/SettlementJournal.java:18-25`）。
2. `apply`：在一個 InnoDB 交易裡 `INSERT … ON DUPLICATE KEY` 收據並讀回比對，`commit`（`S/SettlementSql.java:11-17`）。收據 ID＝SHA-256(場次|來源庫|角色)，重放不會產生第二筆（`S/SettlementJournal.java:7`）。
3. `drain`：autocommit 模式，先 `GET_LOCK(<64 字元雜湊>,5)`（`:26-28`），依 `sequence_id` 逐筆處理未套用收據：
   - 讀 `character_lito` 的 wins／losses／version／last_result_id（`:35`）；
   - 若 `last_result_id != 本收據`：**一條** `UPDATE … SET wins,losses,version+1,last_result_id=本收據 WHERE char_obj_id=? AND version=? AND last_result_id=?`，影響列數不是 1 就丟例外（`:38`）；沒有列時一條 `INSERT`（`:39`）；
   - 再把收據標 `applied=1`（InnoDB，`:42`）。
4. 失敗由背景每 2 秒重試（`S/DurableBattleSettlement.java:22,111-122`）；啟動時先重放 WAL 再 `drainAll`（`:21`）。

**逐項判斷**

| 情境 | MyISAM 下的結果 | 依據 |
|---|---|---|
| 計數與標記是否可能只寫一半 | 不會（正常運作下）。兩者在**同一列、同一條 UPDATE**；MyISAM 單一敘述對單列是原子的，不需要交易 | `:38,41` 註解與程式一致［已驗證：程式；MyISAM 行為為 MySQL 文件語意］ |
| UPDATE 已生效但回覆遺失（`socketTimeout=10000`） | 重試時讀到 `last_result_id == 本收據` → 跳過加總，只補標記；不會重複加 | `:36,42`［已驗證］ |
| 計數成功、標記收據失敗 | 同上，下輪補標記 | `:41` 註解［已驗證］ |
| 同一角色多筆待套用 | 逐筆、依序；B 一定在 A 的標記成功後才處理，所以「只記最後一筆」的標記足夠 | `:30-44`［已驗證］ |
| 兩個執行緒／兩個 LiTo 程序同時 drain 同一角色 | `GET_LOCK` 讓它們排隊（鎖名含控制庫＋來源庫＋角色，同一 MySQL 上全域有效）；就算鎖失效（例如連線被換掉），CAS 條件 `version`＋`last_result_id` 也會讓後到者影響 0 列而失敗重試，不會雙加 | `:26-28,38`［推測：依程式推論，未做並發實測］ |
| 有人不經 LiTo 改 `character_lito` | 只要有改 `version` 或 `last_result_id`，CAS 會拒絕並重試；若外部只改 wins 而不動 version，LiTo 會在它之上加，不會覆蓋 | `:38`［推測］ |
| MyISAM 表鎖等待 | UPDATE 要拿表級寫鎖，若 1-伺服器 有長查詢會等；等太久用戶端逾時，但伺服器之後仍會執行 → 屬於「回覆遺失」情境，重試正確 | ［推測］ |
| 作業系統當機／斷電 | **唯一的缺口**：MyISAM 沒有 redo log，資料寫到 OS 快取就算完成；InnoDB 的收據標記卻已持久化。可能結果是「收據已標 applied，但那一場勝敗沒留下」——**少算，不會多算**；也可能需要 `REPAIR TABLE` | `D/SQL-PERSISTENCE-GATES.zh-TW.md:44` 已聲明不保證［推測，未實測］ |
| 收據表誤用 MyISAM | 啟動直接拒絕 | `:7`［已驗證］ |

**結論**：`GET_LOCK`＋`last_result_id` 的寫回在 MyISAM 下**邏輯正確**——並發、重試、回覆遺失、標記失敗都不會重複加或少加；唯一不保證的是主機當機時 MyISAM 本身的持久性（最多少算最近套用的場次）。

**建議**
1. 規定維持「InnoDB 或 MyISAM 都接受」，不讓 LiTo 自動改引擎（共用庫歸 1-伺服器 管，`D/SQL-PERSISTENCE-GATES.zh-TW.md:38`）。
2. 新建或營運可控的部署，`character_lito` **建議用 InnoDB**：它是 LiTo 專用表，1-伺服器 只在合併清單登記（`1-伺服器/db/380_ac.sql:377`），改引擎不影響 1-伺服器 遊戲邏輯；改了就沒有斷電少算的問題。是否改由營運決定（需要 `ALTER TABLE … ENGINE=InnoDB`，屬不可逆風險較低但仍是現場 DDL），列為 `needs:user` 決策。
3. 不要在 JDBC 選項加 `autoReconnect=true`：重連會悄悄丟掉 `GET_LOCK`（CAS 仍會擋住雙加，但會多出衝突重試）。
4. 附帶發現（與引擎無關）：`drain` 會比對 `characters.char_name`（`:33`），角色在結算前被改名會讓該收據一直重試失敗。［推測］

## 8. weapon／armor 中文欄位對照

**問題**：LiTo 讀的是**英文欄位**（`D/db/lito_static.sql:13028`、`:84557` 的 380_db 快照）。1-伺服器 現行 `db/380_db.sql` 的 weapon／armor 已全部改成**中文欄位**（`1-伺服器/db/380_db.sql:14007`、`:86124`），`ItemTable.java` 也照中文讀。`gen_grants.py --ref-schema static=…/1-伺服器/db/380_db.sql` 比對結果：LiTo 讀的 armor 欄位 78 個、weapon 73 個在 1-伺服器 現行庫**全部不存在**（只剩 `item_id` 同名）。也就是：**把 LiTo 指到 1-伺服器 現行的 380_db 會在啟動健康檢查就失敗**（`S/LegacyInventoryWeightLoader.java:16-19` 讀 `weight`，`S/LegacyEquipmentMagicLoader.java:103-171`）。［已驗證：兩份傾印與程式；實際啟動結果待驗證］

LiTo 的 `S/NativeDeItemTable.java` 與 1-伺服器 `ItemTable.java` 是同一份讀取邏輯（呼叫同名 setter），差別只在欄位名，所以下表是**用 setter 名稱逐行對齊**產生，不是人工猜。LiTo 用途代號：A＝背包／商店投影（`S/Accounts.java:13080-13143`）、L＝裝備戰鬥數值（`S/LegacyEquipmentMagicLoader.java`）、W＝負重（`S/LegacyInventoryWeightLoader.java`）、N＝`NativeDeItemTable`（GM／.de 道具表）。另外 `LiToItemTemplates`（物品詳細說明）以 `SELECT *` 讀全部欄位並用小寫欄名當 key（`S/LiToItemTemplates.java:18-19`），中文欄位會讓它找不到值。

### weapon（ItemTable.java:405-506）

| LiTo 欄位 | 用途 | 1-伺服器 欄位 | ItemTable.java | NativeDeItemTable.java |
|---|---|---|---|---|
| `item_id` | ALNW | item_id（同名） | 410 | 167 |
| `name` | AN | 名稱 | 413 | 170 |
| `classname` | AN | 執行類別 | 414 | 171 |
| `name_id` | AN | 名稱字串編號 | 416 | 173 |
| `type` | AN | 裝備類型 | 417、418 | 174、175 |
| `material` | ALN | 材質 | 421 | 178 |
| `weight` | ANW | 重量 | 422 | 179 |
| `invgfx` | ALN | 背包圖示編號 | 423 | 180 |
| `grdgfx` | AN | 地面圖示編號 | 424 | 181 |
| `itemdesc_id` | AN | 道具說明編號 | 425 | 182 |
| `dmg_small` | ALN | 小型目標傷害 | 426 | 183 |
| `dmg_large` | ALN | 大型目標傷害 | 427 | 184 |
| `range` | LN | 攻擊距離 | 428 | 185 |
| `safenchant` | AN | 安定強化值 | 429 | 186 |
| `use_royal` | AN | 王族可用 | 430 | 187 |
| `use_knight` | AN | 騎士可用 | 431 | 188 |
| `use_elf` | AN | 妖精可用 | 432 | 189 |
| `use_mage` | AN | 法師可用 | 433 | 190 |
| `use_darkelf` | AN | 黑暗妖精可用 | 434 | 191 |
| `use_dragonknight` | AN | 龍騎士可用 | 435 | 192 |
| `use_illusionist` | AN | 幻術師可用 | 436 | 193 |
| `hitmodifier` | LN | 近戰命中修正 | 437 | 194 |
| `dmgmodifier` | LN | 近戰傷害修正 | 438 | 195 |
| `add_str` | ALN | 力量加成 | 439 | 196 |
| `add_dex` | ALN | 敏捷加成 | 440 | 197 |
| `add_con` | ALN | 體質加成 | 441 | 198 |
| `add_int` | AN | 智力加成 | 442 | 199 |
| `add_wis` | AN | 精神加成 | 443 | 200 |
| `add_cha` | ALN | 魅力加成 | 444 | 201 |
| `add_hp` | ALN | 體力加成 | 445 | 202 |
| `add_mp` | ALN | 魔力加成 | 446 | 203 |
| `add_hpr` | LN | 體力回復加成 | 447 | 204 |
| `add_mpr` | LN | 魔力回復加成 | 448 | 205 |
| `add_sp` | AN | 魔攻加成 | 449 | 206 |
| `m_def` | AN | 魔法防禦 | 450 | 207 |
| `dmg_cri_chance` | LN | 暴擊機率 | 452 | 208 |
| `dmg_cri` | L | 暴擊倍率 | 453 | 209 |
| `dmg_max_chance` | LN | 最大傷害機率 | 454 | 210 |
| `dmg_cri_effect` | N | 暴擊標記特效 | 455 | 211 |
| `dmg_max_effect` | N | 最大傷害標記特效 | 456 | 212 |
| `magicdmgmodifier` | N | 魔法傷害修正 | 457 | 213 |
| `canbedmg` | N | 可損壞 | 458 | 214 |
| `min_lvl` | N | 最低使用等級 | 459 | 215 |
| `max_lvl` | N | 最高使用等級 | 460 | 216 |
| `bless` | ALN | 祝福狀態 | 461 | 217 |
| `trade` | AN | 禁止交易 | 462 | 218 |
| `cant_delete` | AN | 禁止刪除 | 463 | 219 |
| `haste_item` | AN | 加速效果 | 464 | 220 |
| `max_use_time` | N | 最大使用時間 | 465 | 221 |
| `add_pvp_dmg` | N | 對玩家傷害加成 | 466 | 222 |
| `add_ignore_reduction` | LN | 無視傷害減免 | 467 | 223 |
| `add_pvp_redux` | N | 玩家傷害減免加成 | 468 | 224 |
| `add_boss_dmg` | N | 對首領傷害加成 | 469 | 225 |
| `add_boss_redux` | N | 首領傷害減免加成 | 470 | 226 |
| `add_dodge` | N | 閃避加成 | 471 | 227 |
| `add_er` | N | 遠距迴避加成 | 472 | 228 |
| `add_hitup` | LN | 近戰命中追加 | 473 | 229 |
| `add_dmgup` | LN | 近戰傷害追加 | 474 | 230 |
| `add_bow_hitup` | LN | 遠距命中追加 | 475 | 231 |
| `add_bow_dmgup` | LN | 遠距傷害追加 | 476 | 232 |
| `add_adena_rate` | N | 金幣倍率加成 | 477 | 233 |
| `add_drop_rate` | N | 掉寶倍率加成 | 478 | 234 |
| `add_melee_redux` | LN | 近戰傷害減免加成 | 479 | 235 |
| `add_bow_redux` | LN | 遠距傷害減免加成 | 480 | 236 |
| `add_stun_success` | LN | 昏迷成功率加成 | 481 | 237 |
| `add_potion_recovery` | LN | 藥水回復加成 | 482 | 238 |
| `add_poison_resist` | N | 毒性抵抗 | 483 | 239 |
| `add_exp_rate` | N | 經驗倍率加成 | 484 | 240 |
| `add_pierce` | LN | 貫穿效果 | 485 | 241 |
| `magic_bow` | N | 魔法弓 | 486 | 242 |
| `arrow_spr` | N | 箭矢動畫編號 | 487 | 243 |
| `add_magic_hit` | LN | 魔法命中加成 | 488 | 244 |
| `set_id` | N | 套裝編號 | 493 | 247 |
| `greater_tier` | N | 初始高階等級 | 502 | 254 |

### armor（ItemTable.java:537-640；任務指定的 548-589 是前段，後段一併列出）

| LiTo 欄位 | 用途 | 1-伺服器 欄位 | ItemTable.java | NativeDeItemTable.java |
|---|---|---|---|---|
| `item_id` | ANW | item_id（同名） | 545 | 291 |
| `note` | A | 備註（ItemTable 不讀） | — | — |
| `name` | AN | 名稱 | 548 | 294 |
| `classname` | AN | 執行類別 | 549 | 295 |
| `name_id` | AN | 名稱字串編號 | 551 | 297 |
| `type` | AN | 裝備類型 | 552、554 | 298、300 |
| `material` | AN | 材質 | 555 | 301 |
| `weight` | ANW | 重量 | 556 | 302 |
| `invgfx` | ALN | 背包圖示編號 | 557 | 303 |
| `grdgfx` | AN | 地面圖示編號 | 558 | 304 |
| `itemdesc_id` | AN | 道具說明編號 | 559 | 305 |
| `ac` | ALN | 防禦值 | 560 | 306 |
| `safenchant` | AN | 安定強化值 | 561 | 307 |
| `use_royal` | AN | 王族可用 | 562 | 308 |
| `use_knight` | AN | 騎士可用 | 563 | 309 |
| `use_elf` | AN | 妖精可用 | 564 | 310 |
| `use_mage` | AN | 法師可用 | 565 | 311 |
| `use_darkelf` | AN | 黑暗妖精可用 | 566 | 312 |
| `use_dragonknight` | AN | 龍騎士可用 | 567 | 313 |
| `use_illusionist` | AN | 幻術師可用 | 568 | 314 |
| `add_str` | ALN | 力量加成 | 569 | 315 |
| `add_con` | ALN | 體質加成 | 570 | 316 |
| `add_dex` | ALN | 敏捷加成 | 571 | 317 |
| `add_int` | AN | 智力加成 | 572 | 318 |
| `add_wis` | AN | 精神加成 | 573 | 319 |
| `add_cha` | ALN | 魅力加成 | 574 | 320 |
| `add_hp` | ALN | 體力加成 | 575 | 321 |
| `add_mp` | ALN | 魔力加成 | 576 | 322 |
| `add_hpr` | LN | 體力回復加成 | 577 | 323 |
| `add_mpr` | LN | 魔力回復加成 | 578 | 324 |
| `add_sp` | AN | 魔攻加成 | 579 | 325 |
| `min_lvl` | N | 最低使用等級 | 580 | 326 |
| `max_lvl` | N | 最高使用等級 | 581 | 327 |
| `m_def` | AN | 魔法防禦 | 582 | 328 |
| `damage_reduction` | AN | 傷害減免 | 583 | 329 |
| `weight_reduction` | LN | 負重減輕 | 584 | 330 |
| `hit_modifier` | LN | 近戰命中修正 | 585 | 331 |
| `dmg_modifier` | LN | 近戰傷害修正 | 586 | 332 |
| `bow_hit_modifier` | LN | 遠距命中修正 | 587 | 333 |
| `bow_dmg_modifier` | LN | 遠距傷害修正 | 588 | 334 |
| `haste_item` | AN | 加速效果 | 589 | 335 |
| `bless` | AN | 祝福狀態 | 590 | 336 |
| `trade` | AN | 禁止交易 | 591 | 337 |
| `cant_delete` | AN | 禁止刪除 | 592 | 338 |
| `defense_earth` | ALN | 地屬性防禦 | 593 | 339 |
| `defense_water` | ALN | 水屬性防禦 | 594 | 340 |
| `defense_wind` | ALN | 風屬性防禦 | 595 | 341 |
| `defense_fire` | ALN | 火屬性防禦 | 596 | 342 |
| `regist_stun` | ALN | 昏迷耐性 | 597 | 343 |
| `regist_stone` | ALN | 石化耐性 | 598 | 344 |
| `regist_sleep` | ALN | 睡眠耐性 | 599 | 345 |
| `regist_freeze` | ALN | 冰凍耐性 | 600 | 346 |
| `regist_sustain` | ALN | 支撐耐性 | 601 | 347 |
| `regist_blind` | ALN | 黑暗耐性 | 602 | 348 |
| `max_use_time` | N | 最大使用時間 | 603 | 349 |
| `is_activity` | N | 活動飾品 | 604 | 350 |
| `add_pvp_redux` | N | 玩家傷害減免加成 | 605 | 351 |
| `add_ignore_reduction` | LN | 無視傷害減免 | 606 | 352 |
| `add_pvp_dmg` | N | 對玩家傷害加成 | 607 | 353 |
| `add_boss_dmg` | N | 對首領傷害加成 | 608 | 354 |
| `add_boss_redux` | N | 首領傷害減免加成 | 609 | 355 |
| `add_dodge` | N | 閃避加成 | 610 | 356 |
| `add_er` | N | 遠距迴避加成 | 611 | 357 |
| `add_hitup` | LN | 近戰命中追加 | 612 | 358 |
| `add_dmgup` | LN | 近戰傷害追加 | 613 | 359 |
| `add_bow_hitup` | LN | 遠距命中追加 | 614 | 360 |
| `add_bow_dmgup` | LN | 遠距傷害追加 | 615 | 361 |
| `add_adena_rate` | N | 金幣倍率加成 | 616 | 362 |
| `add_drop_rate` | N | 掉寶倍率加成 | 617 | 363 |
| `add_melee_redux` | LN | 近戰傷害減免加成 | 618 | 364 |
| `add_bow_redux` | LN | 遠距傷害減免加成 | 619 | 365 |
| `add_stun_success` | LN | 昏迷成功率加成 | 620 | 366 |
| `magicdmgmodifier` | N | 魔法傷害修正 | 621 | 367 |
| `add_potion_recovery` | LN | 藥水回復加成 | 622 | 368 |
| `add_poison_resist` | N | 毒性抵抗 | 623 | 369 |
| `add_exp_rate` | N | 經驗倍率加成 | 624 | 370 |
| `add_magic_hit` | LN | 魔法命中加成 | 625 | 371 |
| `set_id` | N | 套裝編號 | 630 | 374 |
| `greater_tier` | N | 初始高階等級 | 638 | 381 |

**同樣改名的其他 17 張表**（同一次比對，1-伺服器 現行庫缺 LiTo 讀的英文欄位；除 `裝備_weapon_魔法` 外都是啟動即讀）：`裝備_armor_魔法`（18 欄）、`裝備_weapon_魔法`（22）、`裝備_weapon_屬性`（16）、`裝備_特殊_卡片_組合`（39）、`裝備_特殊_套裝_能力`（36）、`裝備_特殊_魔石_魔法`（25）、`裝備_特殊_卡片_能力`（1）、10 張 `裝備_{weapon,armor}_{bless_指定,bless_類型,enchant_指定,enchant_類型,greater_類型}`（各 2～3 欄）。#284 的 `裝備_armor_魔法.id` 錯誤（P24Lab tw-bin 結構不同）是同一類問題。［已驗證：傾印比對］

**規定**：LiTo 0.6.71 只支援**英文欄位**版的 380_db（`D/db/lito_static.sql` 的結構，即 1-伺服器 改中文欄位前的版本）。部署前用 `gen_grants.py --ref-schema static=<部署庫匯出的 .sql>` 比對，輸出不能有「缺欄位」。要支援中文欄位版需要改 Java（依上表做欄名對應層），另開任務。

## 9. 已驗證／推測／待驗證

**已驗證**
- 98 張表的讀寫清單由掃描產生，與正式 JAR 常數池交叉比對無未解釋差異；比 #83 多 18 張，其中 9 張在 `ResourceDatabase.load` 啟動鏈上（含 G01 的 `角色_智力成長`）。
- 寫入只有 `character_lito`、`lito_battle_settlement`；其他 20 張表的寫入程式都被唯讀／`lito_mysql` 守門。
- `lito_source_server` 結構與檢查規則、種子值；README 已改成與 SQL 一致。
- 1-伺服器 現行 380_db 的 weapon／armor 與 `裝備_*` 改成中文欄位，LiTo 讀的英文欄位在該庫不存在；`server_紀錄_獎勵` 在 1-伺服器 現行 380_ac 不存在。
- MyISAM 下結算寫回的邏輯（逐行讀程式）。

**推測**
- 補齊本清單權限後啟動能通過（#284 只證明到第 4 個載入器）。
- 並發 drain、外部改表、表鎖等待情境下的行為（依程式推論，未做並發實測）。
- MyISAM 斷電時「少算不多算」。
- `lito_mysql` 擴充表的私有輔助方法（例如 `S/Accounts.java:6163,6415,7079`）只會被守門後的呼叫端用到（逐一追呼叫端未做完，靠 healthCheck 分區與 #83 運作證據）。

**待驗證**
- 在實驗庫以 §4 範本建帳號，`SELECT … LIMIT 0` 全表通過，並用正式 JAR 啟動到 `[LiTo] ready`（交給 Codex／本機測試任務）。
- 現場 `lito_source_server` 實際啟用哪些來源。
- 現場部署的 380_db 是英文欄位版還是中文欄位版。

## 10. 跨來源角色 ID 撞號（T105／Issue #306）

**問題**：每個來源角色庫各自編 `characters.objid`，兩個來源的不同角色可能有相同 objid（LC X10）。戰鬥裡所有「玩家是誰」都靠 `ClientSession.playerObjectId()`（全 src/main 約 1,545 處：房間名單、目標查找、`S_ATTACK_MANY` 等封包、`BattleRules.Resolution.winners/losers`、斷線結算的 `departed`），撞號時兩位玩家在伺服器眼中是同一個物件：互打的目標解析錯、勝負集合同時含兩人 → 兩人都記勝（或都記敗）。

### 以 objid 當 key 的地方（盤點）

| 類別 | 位置 | 用哪個 ID | T105 後 |
|---|---|---|---|
| 房間／戰鬥名單、目標、封包 objectId | `S/ClientSession.java`（1,348 處）、`S/GameHub.java`（69 處）、`Combat*`／`Native*` 輔助類 | `playerObjectId()` | 改回傳**執行期 ID**（見下） |
| 勝負判定 | `S/BattleRules.java` `Resolution(winners, losers)`；`S/DurableBattleSettlement.java:37,45-52` | `playerObjectId()` | 執行期 ID，不撞號 |
| 結算收據與回寫 | `S/DurableBattleSettlement.java:71-74`（`entry`）、`S/SettlementSql.java`、`S/SharedDatabase.java:98-158` | `(route.database, ch.objectId())` 原 objid | 不變：一直是「來源庫＋原 objid」 |
| 戰績／排名 | `S/ClientSession.java:7778-7812`（`sqlTournamentProfiles` 每個 session 各自一份、以原 objid 為 key）；`GameStateStore.sourceCharacter(來源名, 角色名)`（`S/ClientSession.java:56793-56795`） | 原 objid／來源＋角色名 | 不變：本來就有來源區隔 |
| 角色庫讀取（背包、buff、信件、寵物…） | `S/ClientSession.java:7932-8100` 等 `var5.xxx(var1.objectId())` | `CharacterSummary.objectId()` 原 objid | 不變 |
| 盟屋競標寫入 | `S/ClientSession.java:6919` `persistHouseBid` | 原本誤用 `playerObjectId()` | 改用 `sourceCharacterObjectId()`（原 objid） |

### 設計

- 新類別 `S/SourceObjectIds.java`：每個 `GameHub` 一張登記表（`SourceObjectIds.of(hub)`，`S/SourceObjectIds.java:63`），key 是 `(來源名小寫, 原 objid)`，owner 是 session（弱參照）。
  - 第一個佔用某 objid 的來源**保留原值**；同來源同 objid（同角色重連）共用同一個值；其他來源撞到時（或原 objid 剛好落在別名區）配發別名（`S/SourceObjectIds.java:77-94`）。
  - 別名區 `0x7800_0000..0x78FF_FFFF`（`S/SourceObjectIds.java:43-44`）：盤點所有執行期配號區段（`0x4000_0000` 假人、`0x5000_0000..0x5FFF_FFFF` 地面物品、`0x6000_0000..0x76FF_FFFF` NPC／效果／門／塔等、`0x7A10_0000` 魔法娃娃、`0x8000_0000` 料理火、`0x9000_0000` 寵物）後挑的空段；值 ≤ `0x7FFF_FFFF`，送給客戶端仍是合法的有號 32 位元 objectId。
  - 別名在 owner 存活期間不重配（不依 socket 狀態回收），斷線結算時拿到的 ID 與戰鬥中一致；owner 被 GC 或改選別的角色時才釋放。
- `ClientSession.playerObjectId()`（`S/ClientSession.java:57100-57106`）改為查登記表；只有一個來源（`login.classic.server.names` 只有一個）、或原 objid 不在 1..0x7FFF_FFFF 時直接回原值，所以**單來源部署行為完全不變**。新增 `sourceCharacterObjectId()`（`S/ClientSession.java:57109`）給資料庫用。
- 結算：`DurableBattleSettlement.freeze` 先做 `requireDistinct`（`S/DurableBattleSettlement.java:47,64-69`）——同一個執行期 ID 對到兩個不同的（來源庫, 原 objid）就拒絕凍結（寧可不結算也不寫錯）；勝負用執行期 ID 判，收據與 `character_lito` 回寫仍用 `(route.database, 原 objid)`（`entry`，`S/DurableBattleSettlement.java:71-74`）。收據 key `SettlementJournal.Entry.key(battle, database, objid)` 與 drain 鎖名 `controls_database + objid` 本來就含來源庫，不需改。

### 不處理（範圍外）

- 原 objid 落在 LiTo 內部配號區（例如 `0x4000_0000` 以上）與 NPC／假人撞號：這是單來源也會有的既有問題，不屬於跨來源；只有落在別名區時會被改配。
- 不同 LiTo 程式之間的互連（LC X15）。

### 已驗證／推測／待驗證

- **已驗證**：本機 `mvn -o -DskipTests test-compile` 編譯通過；盤點表的行號逐一讀過。單元測試 `SourceObjectIdsTest`（兩來源同 objid 同房 DEATHMATCH、勝負各自、結算身分各自）由 CI 執行，結果見 `docs/reports/issue-306.md`。
- **推測**：所有送給客戶端的玩家 objectId 都經過 `playerObjectId()`（grep `Protocol.*(…).objectId()` 未找到直接用角色原 objid 的封包）。
- **待驗證**：真機兩個來源同 objid 角色同房對戰的畫面與結算；正式 JAR（0.6.71）尚未包含本修改。
