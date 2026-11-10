# 1-伺服器 能力盤點（角色／道具／戰鬥／技能／多來源）

- 來源：`D:\lineage開發\1-伺服器`（唯讀；未修改、未建置、未啟動、未連 DB）。盤點日期 2026-11-10，Issue #291（T090）。
- 路徑寫法：相對於 `D:\lineage開發\1-伺服器`；`~/` 代表 `src/com/lineage/`。行號為盤點當日原始碼。
- 標記：未加標記者為**已驗證**（以 Grep -n／Read 實際看到該行）；**推測**＝依命名或片段推論、未讀完整流程；**待驗證**＝需執行期或真機才能確認。
- 抽驗：另以獨立指令逐行回讀 27 個 Java 行號與 6 個 SQL `CREATE TABLE` 行號，全部吻合。
- 依規定沒有開啟 `config/server.properties`、`config/sql.properties`、`config/其他控制端/*` 的內容；下面只列出原始碼中 `getProperty` 用到的**鍵名**。

---

## 0. 專案概況

| 項目 | 內容 | 位置 |
|---|---|---|
| 語言／規模 | Java 17，1899 個 `.java`，單一套件根 `com.lineage` | `pom.xml`（`maven.compiler.source=17`） |
| 建置 | Maven，`sourceDirectory=src`，`finalName=Server_Game`，相依套件複製到 `jar/`；另有 `伺服器建置.bat` | `pom.xml` |
| 主要相依 | netty-handler 4.1.104、mysql-connector-j 8.0.33、HikariCP 5.1.0、log4j2 2.22.1、commons-lang3 | `pom.xml` |
| 進入點 | `com.lineage.Server.main` | `~/Server.java:43`；Manifest `mainClass=com.lineage.Server` |
| 啟動順序 | 載設定（Server.java:60-77）→ 建連線池 `initializeDatabasePools()`（:100、:138-151）→ `GameServer.initialize()`（:114） | `~/Server.java` |
| GameServer 初始化 | `~/server/GameServer.java:209`：schema 檢查（:235）→ IdFactory（:244）→ 跨服移民匯入 `ImportService.performImport()`（:252）→ ExpTable（:266）、NpcTable（:310）、ItemTable（:349）、SkillsTable（:371）、PolyTable（:387）→ CharBuffTable／CharSkillTable（:539/:542）→ L1SkillMode（:594）→ CharItemsTable（:603）→ 計時器（:739-755）→ `NettyServer.bindForStartup()`（:781） | |
| 網路層 | `~/netty/`（NettyServer、L1CipherDecoder/Encoder、L1HandshakeHandler、ProxyProtocolV2Handler） | `~/netty/NettyServer.java:118,125` |
| 離線工具入口 | `~/CrossServer.java:14`（多服合併，`--check`、`--launcher-info`，不啟動 GameServer） | |

### 0.1 主要套件結構（檔案數）
`server/clientpackets`(109)、`server/serverpackets`(247)、`server/datatables`(166＋user 38＋server 22)、`server/model`(149)、`server/model/Instance`(51＋deai 14＋npcai 2＋pcai 3)、`server/model/combat`(20)、`server/model/skill`(17＋skillmode 67)、`server/model/classes`(8)、`server/templates`(103)、`server/timecontroller/*`、`data/item_etcitem/*`（道具執行類別，約 300 檔）、`data/npc/*`、`commons/system`(20，含移民／合併)、`config`(21)。

### 0.2 設定檔（只列檔名與鍵名，未讀值）
| 載入類別 | 檔案 | 位置 |
|---|---|---|
| Config | `config/server.properties`、`config/pack.properties` | `~/config/Config.java:131,138`；鍵如 `ServerNo`(:179)、`GameserverPort`(:185)、`AutosaveInterval`(:224)、`AutosaveIntervalOfInventory`(:231) |
| ConfigSQL | `config/sql.properties`（鍵：Driver、URL1/2/3、Login、Password、URL*_LOGIN、Login_LOGIN、Password_LOGIN） | `~/config/ConfigSQL.java:55,66-78` |
| ConfigAlt／CharSetting／Other／Rate／Record／IpCheck | altsettings、charsettings、other、rates、record、ipcheck `.properties` | 各 Config*.java |
| ConfigMigration | `config/其他控制端/7-移民控制設定表.properties`（URL*_TRANSIT、Server_1~8） | `~/config/ConfigMigration.java:23,74-84` |
| ConfigPcDamage／ConfigSkill／ConfigEnchant／ConfigGeneration | `其他控制端/5-職業傷害設定表`、`4-職業_技能設定表`、`9-武防強化設置表`、`6-道具特殊設置表` | ConfigPcDamage.java:26、ConfigSkill.java:19、ConfigEnchant.java:22、ConfigGeneration.java:74 |
| ConfigCrossSQL | `其他控制端/8-多服合併設置表`（只給 CrossServer） | `~/config/ConfigCrossSQL.java:30,45-57` |

### 0.3 資料庫（dump 在 `db/`，檔頭標 MySQL 5.5.22）
| DB | 連線類別 | 內容 | 位置 |
|---|---|---|---|
| 380_db（靜態庫，184 表） | `~/DatabaseFactory.java`（pool 5/30，:91-92） | weapon(:86124)、armor(:14007)、etcitem(:26691)、skills(:47987)、npc(:35264)、mobskill(:34258)、polymorphs(:42925)、exp(:32377)、droplist、spawnlist、shop，及大量中文表（`假人_*`、`系統_*`、`道具_*`、`裝備_weapon_*`） | `db/380_db.sql` |
| 380_ac（玩家／動態庫，70 表） | `~/DatabaseFactoryLogin.java:15`（pool 10/50） | accounts(:454)、characters(:1322)、character_items(:762)、character_skills(:1134)、character_buff(:691)、**character_lito(:983)**，以及 clan_*、log_*、server_*、紀錄_* | `db/380_ac.sql` |
| 380_transit（34 表） | `~/DatabaseFactoryTransit.java:21`（pool 10/100） | 移民中轉：transit_index、transit_import_job、transit_characters…、transit_character_lito(:488) | `db/380_transit.sql` |
| 380_cross（70 表） | CrossServerBoot（DriverManager 直連，`~/commons/system/CrossServerBoot.java:50,136`） | 與 380_ac 同表同欄（逐表比對無差），供離線合併 | `db/380_cross.sql` |
| 380_lito（19 表） | **1-伺服器 src 不使用** | commands＋18 張 `lito_*`（battle_rule/object/spawn/player_spawn/settlement、source_server、room_policy、tournament_shop、resource_*） | `db/380_lito.sql` |

**欄位命名（重要）**：
- 英文欄位：`accounts`、`characters`、`character_items`、`character_skills`、`character_buff`、`character_lito`、`etcitem`、`skills`、`exp`、`polymorphs`、`npc`；`380_lito` 全部英文。
- **中文欄位**：`weapon`、`armor`（除 `item_id` 外全中文，例：`名稱`、`執行類別`、`裝備類型`、`重量`、`小型目標傷害`、`大型目標傷害`、`安定強化值`、`王族可用`…`幻術師可用`、`暴擊機率`、`防禦值`、`魔法防禦`、`傷害減免`）；中文表名如 `系統_強化機率`、`skill_傷害天賦設置`；380_ac／cross 的 `a_*`、`紀錄_*`、`log_強化紀錄`、部分 `server_*` 含中文欄位。
- `characters` 關鍵欄位：`objid`(PK, :1327)、`account_name`、`char_name`、`level`、`HighLevel`、`Exp`、`MaxHp/CurHp/MaxMp/CurMp`、`Ac`、`Str/Con/Dex/Cha/Intel/Wis`、`Class`(:1343)、`Sex`、`Type`(:1345)、`LocX/LocY/MapID`、`Lawful`、`Banned`(:1367)、`OriginalStr…`(:1372)、`ClanID`、`BonusStatus`、`ElixirStatus`。引擎 MyISAM。
- `character_lito`（380_ac.sql:983-992）：`char_obj_id`(PK)、`char_name`、`wins`、`losses`、`version`、`updated_at`、`last_result_id char(64)`；**ENGINE=MyISAM**。

---

## 1. 角色

| 項目 | 類別/檔案:行號 | 主要方法 | 說明 |
|---|---|---|---|
| 登入封包 | `~/server/clientpackets/C_LoginToServer.java:192` | `start()` | :172 `loadAuthorizedCharacter()`→`L1PcInstance.load`；:436 `World.storeObject`；:469 `items(pc)`；:495 `skills(pc)`；:1201 下發技能；:1286 還原 buff |
| 讀檔入口 | `~/server/model/Instance/L1PcInstance.java:6826` | `static load(String)` | 轉 `CharacterTable.get().loadCharacter` |
| DB→物件 | `~/server/datatables/user/CharacterTable.java:497` | `loadCharacter()`（:486 `restoreCharacter` 為別名） | :566-571 寫入六圍；:574 `setTempCharGfx(classId)`；:657-661 地圖不合法改送 SKT |
| 背包／倉庫讀取 | `CharacterTable.java:793` | `restoreInventory()` | 背包、矮人倉庫、精靈倉庫、專屬倉庫 |
| 選角清單 | `CharacterTable.java:381` | `loadCharacterSelections()` | |
| SQL | `~/server/model/CharacterPersistenceService.java:27,60,71,140,788-845,287` | SELECT／INSERT／UPDATE、`loadCharacter`、`readCharacterRecord`、`storeCharacter(snapshot)` | `SELECT * FROM characters WHERE char_name=?` |
| 等級 | `~/server/model/L1Character.java:2434/2438` | `getLevel/setLevel` | |
| 職業判斷 | `L1PcInstance.java:4800-4855` | `isCrown()`…`isIllusionist()` | 依 `getClassId()`（外型 gfx） |
| 職業特性 | `~/server/model/classes/L1ClassFeature.java:7,45-66` | `newClassFeature(classId)`；抽象 AC 防禦、魔法／攻擊／命中等級 | 7 子類（Royal/Elf/Knight/Wizard/DarkElf/DragonKnight/Illusionist）；**不含成長表** |
| 六圍 | `L1Character.java:2564/2588/2612/2636/2660/2684`；`L1PcInstance.java:5218,6530` | `getStr/Con/Dex/Cha/Int/Wis`、`getBaseStr` | 型別 `short` |
| HP/MP | `L1Character.java:2449/2486`；`L1PcInstance.java:6795` | `getMaxHp/getMaxMp` | |
| AC／MR | `L1Character.java:2519/3108`；`L1PcInstance.java:6633/6645,8101/8129` | `getAc/getMr`；呼叫 `CalcStat.calcAc/calcStatMr` | |
| 升降級 | `L1PcInstance.java:1163,7068,7103,7194,8138` | `onChangeExp`、`addExp`、`levelUp`、`levelDown`、`resetLevel` | :7108-7109 用 `CalcStat.calcStatHp/Mp` |
| 成長公式 | `~/server/utils/CalcStat.java:30/55/75/163` | `calcAc`、`calcStatMr`、`calcStatHp`、`calcStatMp` | HP 成長＝職業亂數＋`BaseStatTable.getConHpAdd`，乘 `ConfigCharSetting.HP_UP_RATE`（:80-86） |
| 初始 HP/MP | `~/server/utils/CalcInitHpMp.java:22/55` | `calcInitHp/calcInitMp` | |
| 經驗表 | `~/server/datatables/ExpTable.java:40-43,104,119,129,177` | `load`、`getExpByLevel`、`getNeedExpNextLevel`、`getLevelByExp`、死亡懲罰率 | 表 `exp`：level, exp, expPenalty, death_penalty_rate, restore_rate, restore_cost, talent_point |
| 變身表 | `~/server/datatables/PolyTable.java:42-47,61-69,83/87` | `load`、`getTemplate(name/polyId)` | 表 `polymorphs`：id, name, polyid, minlevel, weaponequip, armorequip, isSkillUse, cause, arrow_spr |
| 變身動作 | `~/server/model/L1PolyMorph.java:483/530/662/712/732` | `canDoPoly`、`doPoly`、`undoPoly`、`isEquipableWeapon/Armor` | 裝備檢查在 C_ItemUSe.java:1133/1260 |
| 存檔 | `L1PcInstance.java:6842/6856/6874` | `save`（非同步）、`saveSync`、`saveInventory` | |
| 快照存檔 | `CharacterTable.java:142/163/285/313`；`~/server/model/CharacterSaveService.java:44/98/110` | `storeCharacter(Sync)`、`captureSaveSnapshot`、`beginCapture/saveAsync/saveSync` | 持玩家鎖抓快照再寫 DB；快照類別在 `~/server/storage/` |
| 自動存檔 | `~/server/timecontroller/pc/PcAutoSaveTimer.java:99/190/210`；`PcAutoSaveInventoryTimer.java:98/182/199`；`StartTimer_Pc.java:84/90` | `run/save` | 間隔來自 `AutosaveInterval`(Config.java:224)／`AutosaveIntervalOfInventory`(:231)；單位為秒屬**推測** |
| 登出存檔 | `~/data/npc/event/QuitGame.java:473/480` | `saveSync`、`saveInventoryWithResult` | |

未找到：`C_EnterWorld`、`CharacterStorage`、`MySqlCharacterStorage`（以 `*Storage*`、`*EnterWorld*` 檔名 glob 查 `src/com/lineage` 全目錄；`server/storage/` 只有兩個 Snapshot 類別）。`BaseStatTable` 內容未開啟（**待驗證**其欄位）。

**LiTo 可以直接參考**：`characters` 欄位意義與 `Class`=外型 gfx／`Type`=職業的區分；六圍、AC、MR、Max HP/MP 由 `L1PcInstance` 覆寫取得的計算方式（`CalcStat.calcAc/calcStatMr`）；`L1ClassFeature` 的職業 AC 防禦上限與攻擊／命中等級；`PolyTable`＋`L1PolyMorph.isEquipableWeapon/Armor` 的變身可裝備判定；`ExpTable` 結構（若 LiTo 要顯示等級）。
**LiTo 不需要**：升降級與經驗累積（LiTo 讀角色只讀，不改等級／經驗）、`CalcInitHpMp`（不建新角色）、自動存檔與快照存檔整條鏈（LiTo 只回存 `character_lito`）、地圖合法性檢查與 SKT 回城、倉庫讀取。

---

## 2. 道具

| 項目 | 類別/檔案:行號 | 主要方法 | 說明 |
|---|---|---|---|
| 道具表 | `~/server/datatables/ItemTable.java:261,273/280/287,721/735,747-761` | `load`、`reloadArmor/Weapon/Etcitem`、`getTemplate`、`createItem` | |
| etcitem | `ItemTable.java:297-305,310-338,361` | `allEtcItem`、`applyArrowConfig` | `etcitem LEFT JOIN etcitem_arrow`；英文欄位 item_id, name, classname, name_id, item_type, use_type, material, weight, invgfx, grdgfx, min_lvl, max_lvl, bless, trade, stackable, delay_id, delay_time, food_volume, limit_class；箭矢／飛刀缺 `etcitem_arrow` 時整筆不載入 |
| weapon | `ItemTable.java:402-405,413-502` | `allWeapon` | **中文欄位**（見 0.3） |
| armor | `ItemTable.java:537-540,548-589` | `allArmor` | **中文欄位**：防禦值、安定強化值、魔法防禦、傷害減免、負重減輕… |
| 模板類別 | `~/server/templates/L1Item.java:7,21,184,494,598`；`L1Weapon.java:5`、`L1Armor.java:3(:15 get_ac)`、`L1EtcItem.java:3` | `getType2`(0道具/1武器/2防具)、`getWeight`、`getUseType`、`get_safeenchant` | |
| 道具實例 | `~/server/model/Instance/L1ItemInstance.java:37,1524` | `getWeight()` | 數量×單位重÷1000，最少 1 |
| 執行類別分派 | `~/data/ItemClass.java:43-75,104,136` | `addList`、`getExecutor`、`item` | 依 `classname` 反射載入 `data.item_etcitem/item_weapon/item_armor` |
| 背包基底 | `~/server/model/L1Inventory.java:134,167/181,264/355,582,667-687,1196` | `getWeight`、`checkAddItem`、`storeItem`、`consumeItem`、`removeItem`、`findItemId` | |
| 玩家背包 | `~/server/model/L1PcInventory.java:1479,1543,1621,1798/1895,2141,2862,3159/3175,3232,3324/3328` | `loadItems`、`insertItem`、`storeItem`、`updateItem`、`consumeItem`、`removeItem`、`saveAllItems(WithResult)`、`deleteItem`、`setEquipped` | |
| 道具存取 | `~/server/datatables/user/CharItemsTable.java:121,340,614,712,744,969,984`；`~/server/model/item/CharacterItemPersistenceService.java:27/122/127` | 全表預載、`loadItems(objid)`、`storeItem`、`updateItemCore`、`deleteItem`、`updateItemEnchantLevel`、`updateItemEquipped` | `character_items` 英文欄位：id, item_id, char_id, count, is_equipped, enchantlvl, is_id, durability, charge_count, remaining_time, bless, attr_enchant_kind/level, greater… |
| 裝備／卸下 | `~/server/clientpackets/C_ItemUSe.java:123,212,548,560,1038,1248` | `start`、`useItemRequest`、`useArmor`、`useWeapon` | 其他道具交 `ItemClass.item()` |
| 裝備欄 | `~/server/model/L1EquipmentSlot.java:110,150,257,339,397` | `setWeapon`、`setArmor`、`set`、`remove`、`setMagic` | `L1PcInventory.java:3367/3386` 呼叫 |
| 強化 | `~/data/item_etcitem/reel/ScrollEnchantWeapon.java:22`、`ScrollEnchantArmor.java:24`；`~/server/model/EnchantManager.java:52,120,135,227,254,275,278-281` | `execute`→`doEnchant(type)`、`randomELevel`、`doFailure`、`doSuccess` | 低於安定值必成功；否則查表＋亂數；上限 `ConfigEnchant.*_MAX_ENCHANT` |
| 強化機率表 | `~/server/datatables/EnchantChanceTable.java:52,75,89,103` | `load`、`getWeapon/Armor/AccessoryChance` | 中文表名 `系統_強化機率` |
| 藥水 | `~/data/item_etcitem/hp/UserAddHp.java:50/123/141`、`hp/UserHpr.java:51`、`mp/UserAddMp.java:45`、`mp/UserMpr.java:50` | `execute`、`set_set` | 參數寫在 etcitem.classname，例 `hp.UserAddHp 30 50 197 true`（註解 :24-25） |
| 加速／勇水 | `~/data/item_etcitem/brave/StatusBrave.java:48`、`StatusHaste.java:56`、`StatusElfBrave.java:40` | `execute` | |
| 負重 | `L1PcInventory.java:989/999`；`L1PcInstance.java:6883` | `getWeight240`、`calcWeight240`、`getMaxWeight` | 上限 150×floor(0.6·STR+0.4·CON+1)，再套減重與 `ConfigRate.RATE_WEIGHT_LIMIT` |

**LiTo 可以直接參考**：weapon／armor 中文欄位 → `L1Weapon/L1Armor` 屬性的對照（`ItemTable.java:413-502,548-589`，LiTo 讀同一個 380_db 時必用）；`L1EquipmentSlot.set/remove` 的裝備加成套用；`L1PolyMorph` 變身可裝備判定；藥水／勇水／加速的 classname 參數格式與效果；負重公式（LiTo 已有 `weight.rate` 設定與 `ModernInventoryWeight`）；`EnchantManager` 中「強化值→加成」的部分。
**LiTo 不需要**：強化成功率與爆裝（LiTo 只讀角色既有裝備，不在競技場強化）、倉庫、商店、交易、掉落入背包、道具 DB 回寫（`CharItemsTable.storeItem/update*`）、家具／任務／傳送道具（`data/item_etcitem/furniture`、`quest`、`teleport`）。LiTo 自有的租借道具（`RentalItem*`）與 1-伺服器 無對應。

---

## 3. 戰鬥

| 項目 | 類別/檔案:行號 | 主要方法 | 說明／公式 |
|---|---|---|---|
| 近戰封包 | `~/server/clientpackets/C_Attack.java:33,94-96,143,152,181,206,219,250` | `start`、`performAttack` | 檢查死亡、圍欄、攻速時間軸、加速器，再 `onAction(pc)` |
| 遠攻封包 | `~/server/clientpackets/C_AttackBow.java:53,136-138,198,208,304,333,343` | `start`、`performAttack`、`getArrow`、`getSting` | 無獨立 C_FarAttack |
| PC 被打 | `L1PcInstance.java:2920,2931,2937,2945` | `onAction` | 任一方在安全區或禁 PK 只播動作；否則 `PcPhysicalAttackFlow.execute` |
| 怪物被打 | `~/server/model/Instance/L1MonsterInstance.java:933,942,944` | `onAction` | |
| PC 物理流程 | `~/server/model/combat/PcPhysicalAttackFlow.java:71,80,88,101,108-109,117,162` | `execute`、`executeAutoHunt`、`executeMotionOnly` | 建 `L1AttackPc`→calcHit→反擊屏障→calcDamage→action/commit |
| NPC 物理流程 | `~/server/model/combat/NpcPhysicalAttackFlow.java:20`（由 `L1NpcInstance.java:1226` 呼叫） | `execute` | |
| 傷害提交 | `~/server/model/combat/DamageCommit.java:51/136`、`DamageCommitQueue.java:118/484` | `enqueue`、`commitInternal`、`drain` | 每目標一條非同步佇列 |
| 命中後副作用 | `~/server/model/combat/PhysicalHitCommitPlan.java:47,74,105`；`PcIncomingHitCommitPlan.java:32,50,51,53`；`CombatSideEffects.java:70` | `afterAccepted`、`aggravateGuardsForPinkAttacker` | 吸魔、武器耐久、PVP 時間、粉名、警衛 |
| 抽象基底 | `~/server/model/L1AttackMode.java:141,224,234,250,275,299,344-371` | `attackerDice`、`calcErEvasion`、`calcEvasion`、`calcPcDefense`、`calcNpcDamageReduction`、`calcCounterBarrierDamage` | PC 防禦：ac=10−AC，查職業 AcDefenseMax，取 rnd(max)+max/8（:253-259）；NPC：rnd(ac/7)+ac/7+damagereduction（:282-285） |
| PC 命中 | `~/server/model/L1AttackPc.java:213,229,237,245-255` | `calcHit` | 近戰距離 ≤ weaponRange+1；弓無箭 miss；地形視線 |
| PC→PC 命中 | `L1AttackPc.java:294,313-345,354,361-371,381-399` | `calcPcHit` | hit=Lv+職業補正＋(近戰 StrHit…/遠攻 DexHit…)−負重；attackerDice=rnd(20)+2+hit−10；defenderDice＝AC≥0 時 10−AC，否則 10+rnd(−AC×1.5)+1；結果 100% 或 15%；弓再判 ER |
| PC→NPC 命中 | `L1AttackPc.java:409,487,517-529` | `calcNpcHit` | defenderDice=10−NPC AC；特定 NPC 需指定技能／變身 |
| 傷害分派與武器骰 | `L1AttackPc.java:575,607,646-653,665,682,707,745-823,840,856,902` | `calcDamage`、`weaponDamage1/2`、`resolveProjectileDamage`、`pcDmgMode` | 弓 Dex 傷害 PvP×2.5、PvE×1.5；箭矢依大小目標；多倍率取最高不連乘 |
| PC→PC 傷害 | `L1AttackPc.java:909,944-962,976-1041` | `calcPcDamage` | 骰＋武器＋強化＋dmgup＋StrDmg/DexDmg，扣 AC 減傷、裝備減免、遠近減免、PvP 減免，職業倍率（`ConfigPcDamage`） |
| PC→NPC 傷害 | `L1AttackPc.java:1088,1107-1120,1158,1170-1210` | `calcNpcDamage` | 大小目標、材質祝福、寵物倍率、BOSS |
| 素質補正 | `~/server/utils/CalcStat.java:218/227/236/245` | `calcStrHit/DexHit/StrDmg/DexDmg` | 查 BaseStatTable |
| 箭／飛刀消耗 | `L1AttackPc.java:1539,1623,1630,1664,1731` | `action`、`consumeProjectile`、commit | 送 `S_UseArrowSkill` |
| NPC 攻擊 | `~/server/model/L1AttackNpc.java:114,126-155,182-184,194,217,274-290,308,322-379,389,411` | `calcPcHit`、`calcNpcHit`、`npcDmgMode`、`calcPcDamage`、`calcNpcDamage` | NPC→PC：hit=Lv+5+hitup；dmg=rnd(Lv)+STR×0.8+STRD；15% 暴擊×1.8 |
| PC 死亡 | `L1PcInstance.java:3287,3466-3483,3535,3545,3563,3624,3745,3869,3884-3888,3933,3994-4010` | `receiveDamageInternal`、`death`、`beginDeathStateLocked`、`finishDeathStart`、`Death.run`、`continueDeathProcess` | 決鬥、安全區、戰鬥區、地圖未開懲罰時跳過懲罰 |
| NPC 死亡 | `L1MonsterInstance.java:1348,1559,1581,1593-1617`；`~/server/model/Instance/NpcDeathHandoff.java:182`；`~/server/timecontroller/npc/NpcDeadTimer.java:103` | `scheduleMonsterDeath`、`Death.run`、`submit`、`run` | 經驗／掉落／業力分配、死亡召喚 |
| 復活 | `~/server/clientpackets/C_Attr.java:471/496,482,506,518-520`；`L1PcInstance.java:4378/4404/4417`；`~/server/clientpackets/C_Restart.java:34,45-50` | `resurrect`、`restartLikeC_Restart`、`C_Restart.start` | |
| 經驗分配 | `~/server/utils/CalcExp.java:54/67/75,165,204,237-243,420,430-444,489`（由 `L1MonsterInstance.java:1727` 呼叫） | `calcExp`、`calcExpWithNpc`、`addExp` | 依仇恨比例與隊伍分配 |
| 死亡懲罰 | `L1PcInstance.java:4013,4022-4046,4188,4216,4287,4563,4627,4644,4651` | `computeDeathSafeNonConsumeMask`、`lostRate`、`lostSkillRate`、`caoPenaltyResult`、`resExp`、`deathPenalty` | 損失＝下一級所需經驗×懲罰率（ExpTable.java:177，預設 0.05 於 :182） |
| PK／粉名 | `~/server/model/L1PinkName.java:133,147-194`；`L1PcInstance.java:1716,3209,4067-4141,7046` | `onAction`、`set_PKcount`、`addLawful` | 粉名 180 秒；PK +1 條件；正義值懲罰 Lv<50：−Lv²×4（:4114），≥50：−Lv³×0.08（:4117）；PK 5 次警告、100 次地獄 |
| NPC AI | `~/server/model/Instance/L1NpcInstance.java:577,594,723-736,749,848,868-906,1156`；`~/server/model/Instance/NpcAI.java:228,286,306-368` | `startAI`、`setHate`、`setLink`、`onTarget`、`attack`、`attackTarget`、`runAILoop`、`AIProcess` | |
| 仇恨表 | `~/server/model/L1HateList.java:45,49,110,129` | `add`（累加）、`getMaxHateCharacter`、`removeInvalidCharacter` | 第一擊另加 MaxHp/10（L1NpcInstance.java:727） |
| 主動索敵 | `L1MonsterInstance.java:614/626,637-653,692-770,861-865,877,1164,1180-1190` | `searchTarget`、`processAcceptedDamageCommit` | agro/agrososc/agrocoi/agrogfxid 旗標；正義值 < −1000 會被非主動怪攻擊 |
| 假人 AI | `~/server/model/Instance/deai/DeCombatBase.java:313/782/835/1246`；`deai/DeManager.java:92` | `searchTarget`、`runHateListCombatTick`、`handleCombatLogic`、`onHit` | De0Idle～De7Deathmatch |
| NPC 計時器 | `~/server/timecontroller/npc/NpcSecondTickCoordinator.java:80,89-107` | `run` | hpr/mpr/bow/rest 四階段 |

未找到：`C_FarAttack`（查 `server/clientpackets/C_Attack*`）；獨立的 `calcPcPc/calcPcNpc/calcNpcPc` 方法（以 `_calcType` 分派到 calcPcHit/calcNpcHit/calcPcDamage/calcNpcDamage）；`L1NpcInstance` 自身的 `death()`（只有 template.death()，`L1NpcInstance.java:2144`；寵物 `L1PetInstance.java:934`）。
推測：`L1PcInstance.java:3466-3483` 的死亡觸發是否在 DamageCommit 提交後的回呼路徑上，未讀完整段落；`L1AttackNpc.java:175` 附近 BOSS attackerDice +30 在 `_hitRate` 已定後才加，可能無效果（**待驗證**，需執行期）。

**LiTo 可以直接參考**：`L1AttackPc.calcPcHit/calcPcDamage` 的 PvP 命中與傷害公式、`L1AttackMode.calcPcDefense`（AC 減傷）、`calcErEvasion`、弓／飛刀／箭矢大小目標處理、`CalcStat.calcStr/DexHit/Dmg`、`pcDmgMode` 的倍率取最高規則、反擊屏障；`NpcAI`＋`L1HateList` 的仇恨與索敵（LiTo 有 PvE 模式 `lito_battle_spawn`、`battle.ai.enabled`）；`L1AttackNpc` 的 NPC→PC 公式；`DamageCommit` 的「同目標傷害序列化」概念。LiTo 現有 `BattlePhysicalAttack.java:6`、`Accounts.java:3171`、`BattleKiringkuDamage.java:51` 的註解已寫「Retained L1AttackPc …」，表示 LiTo 已在比照這套公式（是否就是 1-伺服器 這份＝**待驗證**）。
**LiTo 不需要**：經驗分配與死亡經驗／道具／技能懲罰、復活卷軸與回村（LiTo 對戰結束即結算）、正義值／PK 次數／粉名／警衛／地獄（競技場無 WORLD 法律系統）、怪物掉落與業力、自動練功（`executeAutoHunt`）、假人 AI `deai`（LiTo 另有 test_bot 政策，`lito_room_policy`）。

---

## 4. 技能

| 項目 | 類別/檔案:行號 | 主要方法 | 說明 |
|---|---|---|---|
| 定義表 | `db/380_db.sql:47987-48019`；target_to 註解 :48000 | `skills` | 英文欄位：skill_id, effect_skill_id, name, skill_level, skill_number, mpConsume, hpConsume, itemConsumeId, itemConsumeCount, reuseDelay, buffDuration, target, target_to, damage_value/dice/dice_count, probability_value/dice, attr, type, lawful, ranged, area, through, nameid, action_id, castgfx, castgfx2, sysmsgID_*；target_to 位元 0自己/1人物/2NPC/4血盟/8組隊/16寵物/32地點 |
| 載入 | `~/server/datatables/SkillsTable.java:38,42,52,95,103` | `load`、`fillSkillsTable`、`getTemplate` | 熱重載鍵 "skills"（`~/server/hotreload/HotReloadDefaultBindings.java:44`） |
| 模板 | `~/server/templates/L1Skills.java:17-31,34-52,56-366` | TYPE_*、TARGET_TO_* 常數與 getter | |
| 技能 ID | `~/server/model/skill/L1SkillId.java:3` | 常數 | |
| 學習（魔法書） | `~/data/item_etcitem/skill/Skill_SpellbookLv1.java:34,104`；`Skill_SpellbookCrown.java:52`、`Skill_DragonKnigh.java:61`、`Skill_SpiritCrystal.java:61` | `execute` | 20 檔 |
| 學習判定 | `~/data/cmd/Skill_Check.java:23,25`；`~/data/cmd/Skill_Studying.java:48,50,55,137,174,274,383,438,475,546,707,726` | `check`、`magic`、`mapPosition`、`spellBook` | 依職業分支與等級；寫入 `CharSkillTable.spellMastery` |
| NPC 購買 | `~/server/clientpackets/C_SkillBuy.java:28`、`C_SkillBuyOK.java:31`、`C_SkillBuyItemOK.java:32`；`CharSkillTable.java:203,248` | `purchaseSkill(Authorized)` | |
| 施法封包 | `~/server/clientpackets/C_UseSkill.java:52,106-111,177,623,653,676` | `start`、`performSkill` | 檢查 skill delay、`isSkillMastery`（`L1PcInstance.java:1473`），交 `L1SkillUse.handleCommands` |
| 施放檢查 | `~/server/model/skill/L1SkillUse.java:386,397,437-445,528,549,616,633,1850,1869,1997,2020,2570,2615` | `checkUseSkill`、`isNormalSkillUsable`、`isAttrAgrees`、`isHPMPConsume`、`isItemConsume`、`useConsume`、`commitMaterialAndAcquireCastLocks` | MP/HP 與材料（itemConsumeId/Count） |
| 冷卻／准入 | `~/server/model/skill/L1SkillDelay.java:90,100,114`；`SkillCastAdmission.java:26` | `onSkillUse`、`tryOnSkillUse`、`tryReserveSkillUse`、`admit` | 冷卻依 reuseDelay 屬**推測** |
| 施法流程 | `L1SkillUse.java:126-131,649,935,966-1011,1196,1206,1842,2180-2265,2726,2945,2954-2968` | `handleCommands`（兩多載）、`runSkill`、`applySideEffectsAfterNormalCast`、`failSkill`、`sendCastMotion/sendGrfx` | TYPE_NORMAL/LOGIN/SPELLSC/GMBUFF/NPCBUFF/PROCS |
| 目標與範圍 | `L1SkillUse.java:1239,1347-1418,1468,1571-1616,1631,1672,1687,1695,1704-1706,1758,1785,1790,4049-4243` | `isTarget`、`makeTargetList`、`isParty`、`isTargetCalc`、`isUseCounterMagic` | ranged 判距離；area 0=單體、−1=全畫面、其餘 `getVisibleObjects(target, area)`；target_to 位元過濾 |
| BUFF 掛上／互斥 | `L1SkillUse.java:2060,2139-2146,2167,2512-2556` | `addMagicList`、`sendIcon`、`deleteRepeatedSkills`、`stopSkillList` | |
| 效果表與期限 | `~/server/model/L1Character.java:113,1450,1473,1518,1546,1601,1636,1664,1690,1716` | `setSkillEffect`、`removeSkillEffect`、`hasSkillEffect`、`getSkillEffectTimeSec` | `ConcurrentHashMap<Integer, L1SkillTimer>` |
| 計時器 | `~/server/model/skill/L1SkillTimer.java:14-29`、`L1SkillTimerCreator.java:7`、`L1SkillTimerTimerImpl.java:89,94,117,121,130,157` | `begin/end/kill/getRemainingTime` | 到期呼叫 `L1SkillStop.stopSkill` |
| 解除 | `~/server/model/skill/L1SkillStop.java:53,80,1180` | `stopSkill` | :80 呼叫 `mode.stop(cha)` |
| 特殊計時器 | `~/server/timecontroller/skill/`（Skill_Awake_Timer.java:41,88；EffectFirewallTimer.java:54,101；EffectCube*Timer.java:42,89；EffectCubeExecutor.java:34） | | |
| BUFF 存檔 | `db/380_ac.sql:691`（char_obj_id, skill_id, remaining_time, poly_id）；`~/server/datatables/user/CharBuffTable.java:250-680`；`~/server/model/skill/CharacterBuffPersistenceService.java:59-201` | `saveBuff`、`restoreLoginBuffs`、`replaceBuffs` | |
| 效果分派 | `~/server/model/skill/L1SkillMode.java:128,144,157,164-208,242`；`L1SkillUse.java:2255-2260` | `load`（54 筆 put）、`getSkill`、`isNotCancelable` | `Map<Integer, SkillMode>`；先查 skillId、再查 effect_skill_id；多 ID 可共用一類（CURSE_BLIND＝DARKNESS＝DARK_BLIND） |
| 效果基底 | `~/server/model/skill/skillmode/SkillMode.java:8,19,31,34,36` | 抽象 `start(pc…)`/`start(npc…)`/`stop` | 同目錄 66 個效果類別 |
| 已學技能 | `db/380_ac.sql:1134`；`~/server/datatables/user/CharSkillTable.java:51,63-355`；`~/server/model/skill/CharacterSkillPersistenceService.java:31-109` | `skills`、`spellMastery`、`spellLost`、`spellCheck` | 表 character_skills：char_obj_id, skill_id, skill_name, is_active, activetimeleft |
| 怪物技能 | `~/server/model/L1MobSkillUse.java:36`；`~/server/datatables/MobSkillTable.java:24` | | 方法未細查（**待驗證**） |

未找到：`CharSkillReading`、`CharBuffReading`（Grep 全 src 無結果）。死碼推測：`~/server/model/skillUse/`（`L1SkillUseMode.java:55`、`L1SkillMode.java:12`）全專案無 import／extends。

**LiTo 可以直接參考**：`skills` 表欄位語意與 target_to 位元；`L1SkillUse.checkUseSkill/isTarget/makeTargetList` 的目標與範圍判定；`isHPMPConsume/isItemConsume` 的消耗；`L1SkillMode` 的「skillId→效果類別」註冊表與 effect_skill_id 後備查詢（LiTo 目前 `BattleSpellCatalog`／`BattleTimeAttackMagicSkillCatalog` 為大型 switch，可比照拆分）；`L1Character._skillEffect`＋`L1SkillTimer` 的期限管理；`deleteRepeatedSkills` 互斥規則；`skillmode/*` 各效果實作；角色已學技能 `character_skills` 讀取（只讀）。
**LiTo 不需要**：魔法書學習、NPC 購買技能、`spellLost`（LiTo 不改角色技能）；BUFF 下線存檔／登入還原（`CharBuffTable.saveBuff/restoreLoginBuffs`，LiTo 只寫 character_lito）；傳送類技能（TELEPORT、MASS_TELEPORT）與世界地圖相關效果；自動練功 `handleCommandsForAutoHunt`；GMBUFF／NPCBUFF 類型（**推測**LiTo 無此入口）。

---

## 5. 多伺服器／多資料庫

| 項目 | 類別/檔案:行號 | 說明 |
|---|---|---|
| 三個常駐連線池 | `~/DatabaseFactory.java:15,56,91-92`（380_db 靜態）；`~/DatabaseFactoryLogin.java:15,73,108-109`（380_ac 玩家）；`~/DatabaseFactoryTransit.java:21,84,110-111`（380_transit） | HikariCP；`DatabaseFactoryLogin.get()` 被 62 檔使用（例 `IdFactory.java:57`），靜態表用 `DatabaseFactory.get()`（例 `ItemTable.java:299`、`SkillsTable.java:41`） |
| 寫入佇列 | `~/server/persistence/LoginPersistencePool.java:25,28-29`、`LatestPersistenceQueue.java:23`、`VersionedPersistenceBarrier.java:28` | login 庫專用有界執行緒池、同 key 合併、版本屏障 |
| 伺服器編號 | `~/config/Config.java:179`（`ServerNo`）；`accounts.server_no`（`~/server/templates/L1Account.java:29`） | 帳號記錄登入服編號 |
| 帳號隔離 | `accounts` 在 380_ac（與角色同庫）；380_db 無帳號 | 一個遊戲服＝一個 ac 庫 |
| 跨服移民（線上） | `~/commons/system/MigrationService.java:51,83,89,121,236,263,267,901`；`ImportService.java:35,42,67,164,396,1465`；`MigrationAssetScope.java:29,177-182` | 角色匯出到 transit_*（含 `transit_character_lito`），目標服開服時匯入並改 `accounts.server_no`；`copyLito` 會帶走 character_lito |
| 多服合併（離線） | `~/CrossServer.java:14,28`；`~/commons/system/CrossMergePlan.java:32,43,45-55`；`CrossMergeSchema.java:108-110`；`CrossAccountMerge.java:145,306,391` | 1～64 個來源庫依 `a_合併設定`（合併資料庫名、新增後綴）合併成 `380cross_ac`（ConfigCrossSQL.java:49），54 張玩家表 |
| character_lito 引用 | `~/server/IdFactory.java:40`、`~/commons/system/DBWasher.java:54` 及上列 | 1-伺服器 **知道** character_lito（合併、移民、洗白、ID 掃描），但**不讀寫勝敗** |
| 380_lito | 未找到任何引用（Grep `380_lito`、`lito_` 於 src 全目錄） | 380_lito.sql 只是放在 1-伺服器/db 的 dump；`lito_source_server.character_database`、`lito_battle_settlement.source_database/source_name` 是 LiTo 自己的多來源設計 |

**LiTo 可以直接參考**：「靜態庫（380_db）＋玩家庫（380_ac）」兩池分離；`accounts.server_no` 與 `ServerNo`；移民／合併時 character_lito 的處理方式（LiTo 寫入時要保持 `char_obj_id` 與 `characters.objid` 一致，合併後 objid 可能重編——`CrossAccountMerge.java:145/306/391` 需**待驗證**是否重編 character_lito.char_obj_id）。
**LiTo 不需要**：transit 中轉庫與移民匯入、離線合併工具、login 庫寫入佇列（LiTo 只寫一張表）。

---

## 6. 1-伺服器 與 LiTo 的命名／資料結構差異

| 面向 | 1-伺服器 | LiTo（本 repo `lito伺服器/LiTo381-Server`） |
|---|---|---|
| 套件 | `com.lineage.*` 多層套件（`server/model/Instance` 首字大寫） | 單一扁平套件 `local.lito381.server`（947 檔），前綴分族：Native(343)、Modern(268)、Battle(115)、Classic(96)、Combat(46) |
| 主類 | `com.lineage.Server` | `local.lito381.server.SharedResourceMain` |
| 角色物件 | `L1PcInstance` / `L1Character`（六圍 `short`） | 無 L1 物件階層；`NativeDeCharacterFields`、`GameStateStore`、`ModernVipCharacterState` 等（**推測**對應關係，未逐一比對） |
| 角色庫名 | 380_ac（`characters`、`character_*`、`character_lito`） | 專案 CLAUDE.md 寫「資料庫 `380_db`、`380_lito`」；實際角色庫由 `lito_source_server.character_database` 指定（`UnifiedConfiguration.java:51`），內容需是 380_ac 結構（**待驗證**實際部署值） |
| 設定 | 多個 `.properties`＋中文檔名控制表 | 單一 `config/server.properties`（`database.jdbc.base.url`、`database.static.database`、`database.lito.database`、`weight.rate`、`battle.*`…） |
| 道具欄位 | weapon/armor **中文欄位** | LiTo 讀同表時須用中文欄名（`NativeDeItemDefinitionsSql` 等，**待驗證**是否全部對齊） |
| 戰績寫入 | 不寫勝敗 | `SettlementSql.java:35-39`、`SharedDatabase.java:88-140` 以 `version`/`last_result_id` 樂觀鎖寫 `character_lito`；**該表在 380_ac.sql:992 是 MyISAM（無交易）**，LiTo 的 `SharedDatabase.java:75-77` 允許 InnoDB 或 MyISAM |
| `Banned` 欄 | `characters.Banned`（380_ac.sql:1367） | `SharedDatabase.java:88` 以 `c.Banned=0` 過濾，名稱一致 |
| 技能分派 | `L1SkillMode` 註冊表＋`skillmode/*` 類別 | `BattleSpellCatalog`、`BattleTimeAttackMagicSkillCatalog`、`NativeSkillConsumptionBridges`、`ModernSkillsJdbc` |
| 職業欄位 | `Class`＝外型 gfx、`Type`＝職業 | 需確認 LiTo 使用哪個欄位判職業（**待驗證**） |
| 欄位大小寫 | INSERT 用 `LocX`、讀取用 `locX`；`Intel`↔`addBaseInt`；`BonusStatus`↔`setBonusStats` | MySQL 欄位名不分大小寫，影響僅在程式可讀性 |
| 1-伺服器 類名拼寫 | `C_ItemUSe`、`C_CharcterConfig`、`Skill_DragonKnigh`、`Chang_typeclass` | 引用時照原拼寫搜尋 |
