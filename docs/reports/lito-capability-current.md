# LiTo 現況能力矩陣（T091，Issue #292）

更新：2026-11-11。範圍：唯讀 `lito伺服器/LiTo381-Server/` 的 `src/main`、`docs/`、`merge-inputs/`、`db/`，以及本機 `tools/combat-continuation/` 收據。沒有改程式、沒有啟動、沒有連 DB。本表是 T093 差距矩陣的「LiTo 這一邊」，另一邊是 #291（`1-伺服器`）。

## 讀法

**路徑縮寫**
- `S/` = `lito伺服器/LiTo381-Server/src/main/java/local/lito381/server/`
- `C/` = `lito伺服器/LiTo381-Server/merge-inputs/50635f7e/staged/local/lito381/server/`
- `D/` = `lito伺服器/LiTo381-Server/`
- `M/` = `lito伺服器/LiTo381-Server/docs/maintenance/`
- `R/` = 本機唯讀收據 `D:\lineage開發\3-lito開發\lito伺服器\LiTo381-Server\tools\combat-continuation\integration-lifecycle\build\`（不在 git 內）

**版本欄的意思**
- **正式**：`S/` 原始碼裡有。依 #78，A 範圍與正式 0.6.71 JAR 語意等價，所以算「正式 0.6.71 已有」。
- **正式（patch）**：正式 JAR 有，但功能是 `tools/*/build.py` 的 bytecode patch 插進去的。`S/` 原始碼裡只有被呼叫的類別，**看不到呼叫點**；M2 B-land 時要注意不能漏掉。
- **50635**：只在候選 `50635f7e…`（`C/` 共 300 檔，其中 219 個是新類別、81 個是改過的類別）。
- **3ccbb**、**046c**：只在更新的候選 `3ccbb6f5…`、`046c58c5…`（`M/combat-closeout-current.zh-TW.md:7`、`:11`）。
- **未實作**：程式和文件都找不到。

**證據等級**：已驗證＝讀過程式或文件，行號確認過；推測＝從程式推論，沒有執行；待驗證＝需要實機、DB 或同 SHA 測試。

---

## 1. 角色

| # | 項目 | 現況 | 證據 | 版本 | 等級 |
|---|---|---|---|---|---|
| A1 | 執行模式 | 共用來源走 `legacy_mysql` 模式，來源庫唯讀，狀態不落地（`database.read.only=true`、`state.persistence.enabled=false`） | `S/UnifiedConfiguration.java:73`；`S/SharedDatabase.java:39-40,50-52`；`S/Main.java:39-41` | 正式 | 已驗證 |
| A2 | 角色列表與欄位 | 從來源庫 `characters` 讀名字、血盟、Type、Sex、level、MaxHp/MaxMp、Ac、六屬性、Original 屬性、Exp、Lawful、PK、座標、Food、ElfAttr；只取 Banned=0，最多 8 筆 | `S/Accounts.java:4977-5021` | 正式 | 已驗證 |
| A3 | 目前 HP/MP | **沒有讀**存檔的 CurHp/CurMp，進場直接補滿 | `S/ClientSession.java:11400-11401`；讀取點未找到 | 正式 | 已驗證（沒讀）；是否刻意如此待決定 |
| A4 | 成長表（中文表，屬 380_db 靜態庫） | `角色_體質成長.回血`、`角色_精神成長.魔防`、`角色_力量成長`／`角色_敏捷成長` 的命中與傷害；表名走白名單 | `S/Accounts.java:4986-4992,10314-10337,10404-10410,13234-13240`；健康檢查 `:4350-4356`；靜態庫名 `S/ServerConfig.java:391-399` | 正式 | 已驗證 |
| A5 | `角色_智力成長` | `NativeProcHealBonus` 讀 `魔法加成`、`治癒骰加成`，給治癒技能用；`BattleSpellCatalog` 另外寫死一份同表數值 | `S/NativeProcHealBonus.java:31-41,53-56`；`S/ResourceDatabase.java:17`；`S/NativeProcHealingPlayer.java:68`；`S/BattleSpellCatalog.java:3444-3458` | 正式；候選改成 `ResourceDatabase.value(...)`（`C/NativeProcHealingPlayer.java:60`） | 已驗證；部署時權限漏表見 customer-gaps G01 |
| A6 | 物理命中與傷害加成 | 力量（近戰）／敏捷（遠程）依 SQL 成長表換算，再對目標 AC 擲 d20 | `S/ClientSession.java:8343-8345,75096-75132`；`S/BattlePhysicalAttack.java:83-121,136-159` | 正式 | 已驗證（程式）；數值對 Source 待驗證 |
| A7 | AC | 以存檔 Ac 為底，只套用 LiTo 穿脫造成的差值，再疊娃娃／技能／料理 buff；有「unresolved-ac-delta」紀錄，代表存檔 AC 裡有些來源層還沒拆開 | `S/ClientSession.java:81987-81995,81780-81800,8404-8417` | 正式（含 NativeArmorStatus 的 AC −888 修正，`docs/reports/issue-30.md:56`） | 已驗證；真角色數值待驗證 |
| A8 | MR | 職業底值＋等級＋原始精神＋精神換算 MR＋裝備＋娃娃＋buff | `S/Accounts.java:10309-10337`；`S/ClientSession.java:87358-87372`；`S/BattleMagicResistance.java:14-49` | 正式 | 已驗證 |
| A9 | 屬性抗性 | 火、水、風、地抗＝裝備＋娃娃＋背包＋buff | `S/ClientSession.java:87254-87302` | 正式 | 已驗證 |
| A10 | SP 與魔法傷害 | 智力（含裝備 SP、料理）、裝備魔傷、原始智力都會影響魔法傷害 | `S/ClientSession.java:81940-81964,87004-87014`；`S/CombatRules.java:233-246` | 正式 | 已驗證 |
| A11 | 負重上限 | 依力量、體質、原始屬性、裝備減重、伺服器倍率計算；會擋撿取、購買、自然回復 | `S/BattleInventoryWeight.java:47-61,99-134` | 正式 | 已驗證 |
| A12 | 經驗與等級 | 只在記憶體裡算（`GameStateStore`），不寫回 `characters` | `S/GameStateStore.java:2975,3029-3033` | 正式 | 已驗證 |
| A13 | 寫回（正式路徑） | 只寫 `380_lito.lito_battle_settlement` 收據，以及來源庫的 `character_lito` 勝敗 | `S/SettlementSql.java:6,14,35-42`；`S/DurableBattleSettlement.java:14-21,78` | 正式 | 已驗證 |
| A14 | 寫回（被閘住的路徑） | `persistLitoCharacter` 會更新 `characters`、技能、任務，但只在 `lito_mysql` 模式且非唯讀才執行；建角、刪角同樣被閘住 | `S/Accounts.java:5431-5486,5028-5031`；呼叫端 `S/ClientSession.java:1361-1369` | 正式（共用模式下停用） | 已驗證 |
| A15 | 性別 Sex | 有載入，戰鬥或裝備判定裡沒有用到 | 消費端未找到 | 正式 | 推測（grep 沒找到） |
| A16 | 擴充屬性遙測 | 只輸出到 stdout，不影響戰鬥 | `S/ExtendedCharacterStatsBridge.java:11-14,45-73` | 正式 | 已驗證 |

## 2. 道具

| # | 項目 | 現況 | 證據 | 版本 | 等級 |
|---|---|---|---|---|---|
| I1 | 背包載入 | 來源庫 `character_items` JOIN 380_db 的 `weapon`／`armor`／`etcitem`／`etcitem_doll_type`；讀強化、耐久、祝福、鑑定、封印、到期、娃娃 | `S/Accounts.java:6749-6758,13098-13142,10242-10272` | 正式 | 已驗證 |
| I2 | 裝備數值 | 380_db 範本＋`character_items_card`／`_dark` → 命中、傷害、AC、屬性、抗性；只計算存檔裡已穿戴的列 | `S/LegacyEquipmentMagicLoader.java:478-527,606-691`；`S/ClientSession.java:8336-8342` | 正式 | 已驗證；真角色裝備重載待驗證（`M/combat-closeout-current.zh-TW.md:55`） |
| I3 | 強化值影響數值 | 防具 AC、武器命中與傷害、`裝備_*_enchant_指定/類型` 加成表（含超過安全值的部分） | `S/LegacyEquipmentMagicLoader.java:286-300,322-333`；`S/SourceEquipmentPhysicalStats.java:244-274` | 正式 | 已驗證 |
| I4 | 套裝 | 依 `set_id` 算件數，查 `裝備_特殊_套裝_能力` | `S/LegacyEquipmentMagicLoader.java:302-303,1019-1027` | 正式 | 已驗證 |
| I5 | 穿脫 | 武器與防具可以切換；先通過來源評估寫進記憶體，再送封包；不改來源庫的穿戴旗標 | `S/ClientSession.java:22798,23479,25204-25205`；`M/equipment-toggle-0.6.65.md:15-17` | 正式 | 已驗證（程式）；實機待驗證 |
| I6 | 伺服器端強化 | 只有技能 73「創造魔法武器」能把 +0 變 +1；附魔武器／鎧甲是限時 buff。**沒有捲軸強化／爆裝流程** | `S/RuntimeInventory.java:1073-1083`；`S/ClientSession.java:81797`；捲軸強化未找到 | 正式；捲軸強化未實作 | 已驗證 |
| I7 | 藥水 | 只能在 BATTLE 階段用；回復範圍寫死在 `InventoryCatalog`，再加 PotionRecovery、水汙染等修正 | `S/ClientSession.java:23547-23577`；`S/InventoryCatalog.java:1474-1475`；`S/BattlePotionRecovery.java:7-26` | 正式 | 已驗證 |
| I8 | 其他消耗品 | 加速、勇敢、慎重、變身、閃電魔杖、磨刀石、鑑定、料理、信紙、傳送等效果種類 | `S/InventoryCatalog.java:202-212`；分派 `S/ClientSession.java:23207-23340` | 正式 | 程式存在已驗證；逐項完整度待驗證 |
| I9 | 魔法娃娃 | 可召喚；被動 AC／SP／MR／屬性抗和攻防 proc 都生效 | `S/ClientSession.java:21597-21601,75455,81786,87009,87256,87370-87398` | 正式；候選另有 `NativeSourceDoll*` 系列（只在 `C/`） | 已驗證 |
| I10 | 背包重量 | 380_db weight × 數量；購買、撿取時檢查，超過 240 段就不准加 | `S/LegacyInventoryWeightLoader.java:23-75`；`S/BattleInventoryWeight.java:84-124` | 正式；`C/LegacyInventoryWeightLoader.java` 是改寫版，語意差異未驗證 | 已驗證 |
| I11 | 商店 `TournamentShopCatalog` | 讀 SQL `lito_tournament_shop`；預設 16 項、每場 20000 金幣，取自同系列 2003 資料，**不是 3.81C 抓包**；賣價等於買價；只能在 LOBBY／CHANNEL／ROOM 開；不收來源綁定道具 | `S/TournamentShopCatalog.java:12-19,38-54,83-97,107-112`；`S/ClientSession.java:20306-20373`；`D/docs/DATA-SOURCES.zh-TW.md:69` | 正式 | 已驗證；從哪個庫讀待驗證（見 X9） |
| I12 | 購買交易 | 金幣、背包空間、負重都通過後，先 `saveEconomyState` 再送背包差異 | `S/ClientSession.java:20195-20284` | 正式 | 已驗證 |
| I13 | 持久化 | `GameStateStore.save()` 在 `persistent=false` 時直接 return，所以購買、消耗、金幣只存在記憶體；寫回 `character_items` 的程式存在，但共用模式下被閘住 | `S/GameStateStore.java:7618-7619`；`S/Accounts.java:5525-5601`；`D/README.zh-TW.md:31` | 正式（刻意不寫回） | 已驗證 |
| I14 | 租賃城鎮道具 | 有範本，但統一設定把租賃城鎮關掉 | `S/RentalItemDefinitionCatalog.java:16-17`；`S/UnifiedConfiguration.java:75` | 正式（停用） | 已驗證 |
| I15 | 槽位與職業規則 `EquipmentRules` | 只有 Modern* 引用它；正式路徑走 `RuntimeInventory.toggle*` | `S/EquipmentRules.java:11-31`；`S/ModernEquipmentAuthority.java:316-317` | 正式（Modern 路徑不算正式流程） | 已驗證 |

## 3. 戰鬥

戰鬥是 LiTo 自己依 Source（L1AttackPc／L1MagicPc）逐段重寫的實作，不是直接跑 Source 的類別（`M/combat-parity-0.6.68.md:20`）。

| # | 項目 | 現況 | 證據 | 版本 | 等級 |
|---|---|---|---|---|---|
| B1 | 物理命中 | 等級＋職業修正＋力量或敏捷＋裝備＋料理，擲 d20 對目標 AC；結果只有 15% 或 100% 兩檔 | `S/BattlePhysicalAttack.java:83-121,161-172`；呼叫 `S/ClientSession.java:75125,75136` | 正式 | 已驗證 |
| B2 | 閃避與遠程迴避 | 閃避值併入命中骰；弓命中後再擲一次 ER | `S/ClientSession.java:75109-75113,75137-75149`；`S/BattleRangedEvasion.java:53` | 正式 | 已驗證 |
| B3 | 傷害骰 | 武器／箭依大小型骰；空手 (4..8)>>2；鋼爪類例外 | `S/BattlePhysicalDamage.java:13-81`；`S/ClientSession.java:75165-75215` | 正式 | 已驗證 |
| B4 | 材質、祝福、屬性加傷 | 銀／祕銀／奧里哈魯根對不死系、祝福武器、屬性係數 | `S/BattlePhysicalOffense.java:27-114` | 正式 | 已驗證 |
| B5 | 最大傷害與爆擊 | `dmg_max_chance`；`dmg_cri` 和其他倍率取最大值，不相乘 | `S/BattleWeaponCritical.java:31-115` | 正式 | 已驗證 |
| B6 | AC 減免（打玩家） | AC 隨機減傷＋裝備減免＋近遠減免，再扣額外 \|AC\|/除數 | `S/BattlePhysicalDefense.java:42-51,92-116`（額外層在 `:112`）；`S/CombatRules.java:220-228` | 正式 | 已驗證。0.6.68 報告說缺這層（`M/combat-parity-0.6.68.md:22`），現已補上，該文件過時 |
| B7 | AC 減免（打 NPC） | (10−AC)/7 隨機減傷＋NPC 減傷；硬皮怪吃遠程減半 | `S/BattlePhysicalDefense.java:16-39` | 正式 | 已驗證 |
| B8 | 職業傷害倍率 | 讀 `Other_To_is<職業>_pc/_npc` 外控表 | `S/CombatClassDamage.java:25-29`；接線用 bytecode patch `D/tools/resource-database/ClassDamagePatch.java:6` | 正式（patch） | 已驗證；`ClientSession` 原始碼內沒有呼叫點 |
| B9 | 魔法傷害 | 屬性抗×INT → 魔法爆擊／原始 INT／裝備魔傷 → 防具減免 → MR；職業倍率 `Other_ADD_is*_MagicPC` | `S/ClientSession.java:81934-81964`；`S/BattleMagicResistance.java:128-144`；`S/CombatMagicDamage.java:39-44` | 正式；`CombatMagicDamage` 屬 patch 接線 | 已驗證 |
| B10 | 技能機率與 BOSS 免疫 | SQL 外控的機率與 BOSS 修正，用等級、MR、魔法命中計算 | `S/CombatProbability.java:11-38,74-94` | 正式（patch） | 已驗證 |
| B11 | 外控設定 | `combat.properties` 唯讀載入；候選多 4 個鍵（Triple_Arrow_Dmg、SLAY_BREAK_NOT_TELEPORT、INVIS_CAST_BREAK、SOLID_CARRIAGE_MODE） | `S/CombatRules.java:23-55`；`C/CombatRules.java:24` | 正式；4 鍵只在 50635 | 已驗證 |
| B12 | 生命的祝福 +100 | 程式依 Source 實際載入行為設為 0（Source 宣告了欄位但從沒賦值）；0.6.68 報告則說「缺 +100」，兩邊說法矛盾 | `S/CombatRules.java:82-86`；`M/combat-parity-0.6.68.md:21`；`M/combat-coverage-current.md:250` | 正式 | 程式已驗證；哪一邊才對待 T093 對照 `1-伺服器` |
| B13 | 死亡判定 | 客戶端確認死亡後交給房間判定；重複死亡封包不重複計分 | `S/ClientSession.java:26655-26701`；`S/GameHub.java:9917-9921`；`S/BattleRules.java:345-376` | 正式 | 已驗證 |
| B14 | 勝敗規則 | 死鬥全滅、競技／攻城／Quest 可復活、Time Attack 隊長或全隊陣亡就輸 | `S/BattleRules.java:358-401,473-480,561-587` | 正式 | 已驗證 |
| B15 | 房間結算 | 先凍結結果、寫持久日誌，再送結果；測試房與觀戰者不寫回 | `S/ClientSession.java:11809-11810`；`S/DurableBattleSettlement.java:33-58` | 正式 | 已驗證（程式）；實機待驗證（G19） |
| B16 | `character_lito` 寫回 | `GET_LOCK`＋`last_result_id` 同列更新 wins／losses／version，重放不會重複加 | `S/SettlementSql.java:25-47` | 正式 | 已驗證 |
| B17 | 排名 | 勝負、月計、斷線數、ranking_points；一般敗場仍加分，斷線才扣分；「榮譽點數」欄位未找到 | `S/GameStateStore.java:5575-5614`；`S/Accounts.java:4596` | 正式 | 已驗證 |
| B18 | 斷線結算 | 300ms 內的斷線合併處理；全員斷線判無效；還有人在就繼續 | `S/GameHub.java:22,12969-13024`；`S/TeamDisconnectSettlement.java:4-15` | 正式 | 程式已驗證；雙人實機待驗證（`D/VALIDATION.md:9`，G24） |
| B19 | 斷線冪等與重試 | SHA-256(battle\|db\|char) 當結果 ID；`ON DUPLICATE KEY`；背景每 2 秒重試 | `S/SettlementJournal.java:5-26`；`S/SettlementSql.java:14-16`；`S/DurableBattleSettlement.java:22,111-122`；`S/DisconnectedBattleResult.java:15-37` | 正式 | 已驗證（有測試）；長時間壓力待驗證 |
| B20 | Ctrl 強制目標 | 每個請求各自有效，處理完清除 | `S/ForcedCombat.java:7`；`M/forced-combat-0.6.67.md:5-14` | 正式；7 技能新 Ctrl 政策只在候選 `323d708e…`（G16） | 已驗證；人工待驗證（G23） |
| B21 | 戰鬥模式 | 寵物競賽、死鬥、Quest、攻城、Time Attack、Honor Match；產品只用競技場 | `S/BattleRules.java:35-71`；`M/combat-continuation-current.zh-TW.md:14` | 正式；Quest／TimeAttack 遭遇戰在 50635 有改版 | 已驗證 |
| B22 | 死亡懲罰 | 沒有（只存戰績）；普通世界的死亡懲罰入口已停用 | `M/combat-parity-0.6.68.md:26`；`M/combat-coverage-current.md:253` | 正式（刻意的規則） | 已驗證 |
| B23 | 受控 NPC（寵物、召喚）戰鬥語意 | 逐目標失敗、扣料、MP、效果順序都還沒做完 | `M/combat-continuation-current.zh-TW.md:78,83`；`M/combat-closeout-current.zh-TW.md:25-31` | 部分在 50635／3ccbb／046c，全部未完成 | 已驗證（文件） |
| B24 | Modern* 對戰核心 | `ModernPhysicalCombat`、`ModernMagicCombat`（只有光箭）等；屬於另一個 jar（`classifier=modern`），不在正式流程 | `S/ModernPhysicalCombat.java:13-21`；`S/ModernMagicCombat.java:8-16`；`M/combat-parity-0.6.68.md:9` | 不算正式 | 已驗證 |

**與 Source 已知不一致**（文件有記錄）
1. 技能數值大多寫死在 Java catalog，改 SQL 不會同步（`M/combat-parity-0.6.68.md:19`）。
2. 負面技能控制、buff 能力、BOSS 免疫三張表：0.6.68 時沒有讀（`M/combat-parity-0.6.68.md:24`）；現在 BUFF 79 欄已有數值消費端，但完整共存與相消仍未完成（`M/combat-coverage-current.md:253`）。
3. 生命的祝福 +100（B12）：哪邊才對仍有矛盾。
4. skill208 暈眩 2 秒對 SQL 3 秒：Source 執行器本來就固定 2 秒，不算缺陷（`M/combat-parity-0.6.68.md:13`）。
5. 受控 NPC 的 Source 語意（B23）。
6. 技能 131 在競技場停用，是刻意的規則（`M/combat-continuation-current.zh-TW.md:22,84`）。
7. 單體、範圍目標與 Source 不一致的 G03–G12（見 4.3）。

## 4. 技能

### 4.1 807 技能統計

資料來源：`M/combat-coverage-current.json`（候選 `efd4bc36…` 掃描，2026-10-16，JSON:3）和本機收尾矩陣 `R/development-closeout-progress/20261028T222200402677Z/skills.tsv`（候選 `046c58c5…`，807 列）。數字是我用 `scratch/t091/skill_stats.py`、`tsv_stats.py` 重算的，兩份一致。

| 狀態 | 數量 | 怎麼判定 | 證據 |
|---|---|---|---|
| 有 handler（專用 `use*Spell` 方法） | 175 | catalog 有型別、找得到專用方法；技能 5 指定傳送用獨立處理器 | `M/combat-coverage-current.md:11-230`；`S/ClientSession.java:27916` |
| 有入口，但 coverage 標「待查」 | 6（192、202、203、207、208、213） | catalog 有型別，掃描器沒認出來；實際走 `useSelectorOnlyTargetedAttackSpell` | `M/combat-coverage-current.md:202,212,213,217,218,223`；`S/ClientSession.java:33198-33210,60420-60444` |
| **玩家技能有入口，小計** | **181** | 收尾矩陣 `player-selector-skill`＋`symbolic-dispatch-path-located` 共 181 列 | `skills.tsv`；`M/combat-closeout-current.zh-TW.md:43` |
| **已驗證**（行為對齊 Source） | **0** | 每列 `behavior_verified=false`；`full_behavior_parity_verified=false`；收尾矩陣 `same_SHA_semantics_qualified=False` 共 807 列 | `M/combat-coverage-current.json:1820`；`skills.tsv` |
| **待驗證**（有入口，行為未驗收） | **181** | 181 列都是 `inherited-implementation-needs-per-skill-review`；其中 76、167 另有守門修補，但測試延後 | `skills.tsv` development_status；`M/combat-closeout-current.zh-TW.md:52` |
| SQL 保留槽（name=none） | 38 | 24、81–86、92–96、119–121、123–128、135–136、139–144、177–180、196–200 | `M/combat-coverage-current.md:34,91-96,102-106,129-131,133-138,145-146,149-154,187-190,206-210` |
| catalog 專用保留槽（沒有 SQL 列） | 1（122） | 不算在 807 列裡 | `M/combat-coverage-current.md:132` |
| **未實作**（非玩家資料列，ID>220） | **588** | 有 SQL、沒有 catalog，例如 4000 束縛、4011–4013、10000 系列。NPC 施法另有怪物技能表路徑，逐列適用性還沒審 | coverage JSON `skill_matrix`；`M/combat-closeout-current.zh-TW.md:43,52` |

補充：
- 0.6.68 版比對過 212 個技能的 576 個 MP／HP／冷卻／持續欄位，575 個相符（`M/combat-parity-0.6.68.md:11`）。這只證明數值欄位一樣，不代表行為通過。
- 「已驗證」的口徑是 Source 行為對齊。各候選的同 SHA 收據只證明各自的 SHA（`M/combat-closeout-current.zh-TW.md:11`），不能算正式版通過，所以不計入。

### 4.2 依職業分組

分組依 SQL skill_level，不代表該職業可學（`M/combat-coverage-current.json:53070`）。玩家技能入口在 `S/BattleSpellCatalog.java`（122 個 `record *Spell` 型別，`ReservedSkillSlot` 在 `:4200`）＋`S/ClientSession.java:32176`（`useSpellBeforeDragonTargets`）。

| 職業 | 列數 | 有入口 | 已驗證 | 待驗證 | 保留槽 | 未實作 | coverage.md 行 | 版本 |
|---|---|---|---|---|---|---|---|---|
| 共通魔法／法師 | 82 | 79（含 5） | 0 | 79 | 0 | 3（10131、11015、11030，NPC 資料列） | 11-90 | 正式 |
| 騎士 | 5 | 5 | 0 | 5 | 0 | 0 | 97-101 | 正式 |
| 黑暗妖精 | 16 | 16 | 0 | 16 | 0 | 0 | 107-122 | 正式 |
| 王族 | 8 | 6 | 0 | 6 | 2（119、120） | 0 | 123-130 | 正式 |
| 妖精 | 40 | 40 | 0 | 40 | 0 | 0 | 139-186 | 正式 |
| 龍騎士 | 12 | 12 | 0 | 12 | 0 | 0 | 191-202 | 正式；0.6.46 有 7 技能原生選取器回歸（`M/dragon-skills-0.6.46.json`） |
| 幻術師 | 23 | 23 | 0 | 23 | 0 | 0 | 203-230 | 正式；0.6.50 有呈現回歸（`M/illusion-presentation-0.6.50.json`） |
| 其他／NPC／道具 | 622 | 0 | 0 | 0 | 37（36 列 SQL＋122） | 585 | 其餘 | 玩家入口未實作；NPC 路徑待查 |
| **合計** | **808** | **181** | **0** | **181** | **39** | **588** | | |

### 4.3 customer-gaps G03–G18 對應到技能

候選 SHA 與數字都取自 `docs/reports/customer-gaps.md` 引用的那一行。版本欄說明：G03–G12、G16–G18 的候選都早於 `50635f7e…`；依 `docs/reports/issue-30.md:23` 的上層關係，推測已含在 50635 內，但收據只證明各自的 SHA。

| G | 技能 | 職業 | 現象摘要 | 修正候選（同 SHA 收據） | 證據 | 版本 |
|---|---|---|---|---|---|---|
| G03 | 167 風之枷鎖 | 妖精 | 單體技能誤中旁人 | `4627ff77…` 626 案 | `M/combat-continuation-current.zh-TW.md:36`；`R/wind-source-target-progress-current.json` | 50635（推測）；正式版有沒有這個缺陷待重現 |
| G04 | 194 暴龍之眼 | 幻術師 | 單體、射程 10 卻誤中非指定玩家 | `5945812d…` 1,434 案 | `:46` | 50635（推測） |
| G05 | 76 集體緩速術 | 法師 | 以自己為主目標時扣費、顯示、狀態、範圍出錯 | `b2ebe000…` 3,814 案 | `:50` | 50635（推測）；046c 另加守門（`skills.tsv`） |
| G06 | 29 緩速術、152 地面障礙、153 魔法消除 | 法師／妖精 | 施放准入與狀態結果有差異 | 同 SHA 3,636 案 | `:20` | 50635 |
| G07 | 17 極光雷電、148 火焰武器、149 風之神射 | 法師／妖精 | 範圍與目標語意不同 | 640 案（收據頁沒寫 SHA） | `:17` | 50635（推測） |
| G08 | 40 黑闇之影、103 暗黑盲咒 | 法師／黑妖 | 遠距離誤生效、漏找目標 | `17d49e19…` 1,128 案 | `:38` | 50635（推測） |
| G09 | 29、44 魔法相消術、76 | 法師 | 視野射程（−1）邊界 | `26552f69…` 1,842 案 | `:47` | 50635（推測） |
| G10 | 87 衝擊之暈 | 騎士 | HP 還沒扣就先出現暈眩 | `4828aad9…` 2,489 案 | `:42` | 50635（推測） |
| G11 | 50 冰矛圍籬 | 法師 | 提早消魔或冰凍、期限錯讀 | `f4d1e83a…` 1,074 案 | `:44` | 50635（推測） |
| G12 | 80 冰雪颶風 | 法師 | 過期扣 MP、動畫前先扣 HP | `619ed21e…` 1,202 案 | `:45` | 50635（推測） |
| G13 | 29、152、153（受控 NPC 施放） | — | 目標或觀察者中途退役 | `50635f7e…` 300 等待 | `M/source-negative-final-frame.zh-TW.md:3-9` | 50635 |
| G14 | 76、167（受控 NPC 範圍主次目標） | — | 退役後舊次目標仍被擲骰 | `3ccbb6f5…` | `M/combat-closeout-current.zh-TW.md:11` | 3ccbb |
| G15 | 全 807 列的效果身分（49014→202、49017→33 等 7 個別名） | — | Source 效果身分對照，消費端還沒做 | `046c58c5…`，**沒有測試收據** | `M/combat-closeout-current.zh-TW.md:15,76`；別名表 `M/combat-coverage-current.md:235-243` | 046c |
| G16 | 1、4、26、42、50、76、167 | 法師／妖精 | 7 技能的新 Ctrl 政策 | `323d708e…` 1,096 案 | `:52`；7 個技能 ID 取自 `R/source-wind-and-intent-progress/20261023T152955269357Z/selected-player-intent-progress.json` | 50635（推測） |
| G17 | 多技能（WORLD 專屬） | — | WORLD 生命、Ref、Proc、MP | `104716cd…`～`3386173a…` | `:28-33` | 50635（推測）；產品只有競技場，不列門檻 |
| G18 | 173 污濁之水、174 精準射擊、87、44、161 封印禁地 | 妖精／騎士／法師 | 受控 NPC 材料帳本與成對生命週期 | `3e853214…`、`7a982b61…`、`246ef565…`、`c6611ff7…`／`f213c87f…` | `:34-35,39-40,48` | 50635（推測） |

G02（技能 26 要按 Ctrl 才能自施）屬客戶端游標／送包問題，伺服器端 26 有入口（`M/combat-continuation-current.zh-TW.md:21`）。G28（131 停用）是刻意的規則。

## 5. 跨界（多來源）

| # | 項目 | 現況 | 證據 | 版本 | 等級 |
|---|---|---|---|---|---|
| X1 | `lito_source_server` 表 | 4 欄（source_id 1～8 主鍵、display_name、character_database 唯一、enabled），CHECK 限定 1～8 | `D/db/lito_source_server.sql:3-11` | 正式 | 已驗證 |
| X2 | 支援幾個來源 | 最多 8 個。SQL 種子啟用 3 個（太陽神→380_ac、月亮神→380_ac_1、伺服器3→380_ac_2）；README 卻寫「公牛、天使啟用」，**兩份文件不一致** | `D/db/lito_source_server.sql:13-21`；`D/README.zh-TW.md:12` | 正式 | 已驗證（不一致）；實際啟用以 live 表為準，待驗證 |
| X3 | 讀取來源表（`UnifiedConfiguration`） | 唯讀連 380_lito，檢查 ID 不重複、名稱 MS950 不超過 63 bytes、至少 1 個啟用，只回傳啟用的列；`apply()` 轉成 login 與 dynamic DB 設定 | `S/UnifiedConfiguration.java:47-64,82-85` | 正式 | 已驗證 |
| X4 | 怎麼決定資料來源（`SourceLogin`） | 每個來源開一個埠（port＋source_id），連到哪個埠就固定用哪個來源，在收到帳密之前就決定了 | `S/SourceLogin.java:9,41-46,78-87`；`S/Main.java:116` | 正式 | 已驗證 |
| X5 | 連線（`SharedDatabase`） | 每個來源一個 `LegacyMySqlRepository`、一個 HikariCP 連線池（最多 8 條），帳密共用、只換資料庫名；超過 1 個來源時用 `SourceRoutedAccountRepository` 分流 | `S/SharedDatabase.java:47-55,162-168`；`S/Accounts.java:118-185,4224-4246` | 正式 | 已驗證 |
| X6 | 帳號隔離 | 帳號改寫成 `source:<id>:<帳號>`；**src/main 原始碼裡沒有呼叫點**，正式 JAR 是用 `PatchSourceLogin` 把呼叫插進 `ClientSession`（isAccountBanned／banAccount／accountName）和 `GameHub` | `S/SourceLogin.java:88-95`；`D/tools/shared-database/PatchSourceLogin.java:29-69` | 正式（patch） | 已驗證；B-land 時要補成原始碼呼叫 |
| X7 | 帳號驗證與選角 | 每個來源各驗一次，選角時只能選驗證通過的來源 | `S/Accounts.java:90-110,146-173` | 正式 | 已驗證 |
| X8 | 管理權限 | 權限綁定（repository, 帳號），等級讀所選來源的 `accounts.access_level` | `S/SourceAdministration.java:20-46`；`M/source-administration-validation.json:2-3,18-22` | 正式 | 已驗證 |
| X9 | 角色隔離 | `character_lito` 不帶來源 ID，放在各來源自己的庫裡，靠所在資料庫隔離；中央戰績表 `lito_tournament_character_record` 才有 `(source_server_id, char_obj_id)`，但共用模式不走那條 | `S/SharedDatabase.java:74,139-140`；`S/SettlementSql.java:35-39`；`S/Accounts.java:4588-4603,9216-9233` | 正式 | 已驗證 |
| X10 | 角色 ID 跨來源撞號 | 戰鬥內的玩家 ID 直接用來源 objid，沒有加來源前綴；兩個來源出現相同 objid 時怎麼處理：**未找到** | `S/ClientSession.java:88170-88174` | **未實作** | 推測有風險；待驗證 |
| X11 | 跨來源同房 | 0.6.54 有「同頻道跨來源建房」測試紀錄；原生雙玩家畫面寫「待實測」 | `M/room-directory-0.6.54.json:19-22`；`M/bot-room-count-0.6.64.md:14` | 正式 | 後端已驗證；實機待驗證 |
| X12 | 跨來源對戰結算 | 每位玩家用 `route(player)` 找自己的來源，寫回各自的 `character_lito`；收據帶 `source_database` | `S/DurableBattleSettlement.java:32,45-48`；`S/SettlementSql.java:23,30-42`；`D/db/lito_battle_settlement.sql:3-17` | 正式 | 程式已驗證；實機待驗證 |
| X13 | 跨來源同場戰鬥是否可行 | 有競技場路徑驗證紀錄；沒有明文規格寫「可行」或「不可行」 | `M/combat-integration-lifecycle.zh-TW.md:295` | 正式（部分） | 推測可行；X10 撞號未處理 |
| X14 | #281 shared-database 接線 | 候選的 `LegacyMySqlRepository` 有 5 個 `SharedDatabase` 掛鉤（check、registered、profiles、persist、resourceTable），src/main 版**一個都沒有**；推測會讓控制表去 380_db 找、戰績 CAS 失效，這就是「src/main JAR 不能啟動」的原因 | `C/LegacyMySqlRepository.java:239,489,5202,5246,8886`；`S/Accounts.java:4153,12932-12937`；`PLAN.md:25`（repo 根） | 掛鉤只在 50635 | 差異已驗證；影響屬推測 |
| X15 | 跨伺服器（不同程式）對戰 | 只有「同一個 LiTo 程式接多個來源庫」，沒有不同伺服器程式之間的互連協定 | 未找到 | **未實作** | 推測（grep 沒找到） |

---

## 已驗證／推測／待驗證

**已驗證**（讀過程式或文件，行號確認過）
- 角色 A1–A14、A16；道具 I1–I7、I9–I15；戰鬥 B1–B11、B13–B24；跨界 X1、X3–X9、X12 的程式位置。
- 技能統計：181 個有入口（其中 6 個 coverage 標「待查」，實際走 selector-only，`S/ClientSession.java:60420`）、0 個已驗證、181 個待驗證、38＋1 個保留槽、588 個未實作；兩份底稿重算結果一致。
- G16 的 7 個技能 ID（本機收據）。
- 0.6.68 文件說缺的 AC 額外減傷，現在已在 `S/BattlePhysicalDefense.java:112`。

**推測**
- G03–G12、G16–G18 的修正都已含在 `50635f7e…`；正式版也有同一缺陷（customer-gaps 的推論，本表沿用）。
- X10 跨來源 objid 撞號的風險；X13 跨來源同場可行；X14 #281 的實際影響；A15、X15「沒有」是 grep 沒找到的結果。
- B8、B10、X6 的 patch 接線在正式 JAR 生效：依據是 patch 原始碼，沒有反編譯正式 JAR 逐點核對。

**待驗證**
- 真角色唯讀載入（裝備、INT、成長、加成重載）需要連 DB（`M/combat-closeout-current.zh-TW.md:55`）。
- 181 個玩家技能逐一的行為（正常／Ctrl、自友敵）、全部實機項目（G19–G26）。
- B12 生命的祝福 +100 的矛盾，要對照 `1-伺服器`。
- X2 目前 live 的啟用來源清單。
- 588 個非玩家技能列在 NPC 路徑上的適用性。

## 給 T093 的重點

1. **技能**：主要差距是「181 個有入口但 0 個驗證」，不是「缺 handler」。588 列 NPC／道具技能要先決定在不在產品範圍內。
2. **道具**：沒有捲軸強化；購買與消耗只在記憶體（刻意如此）；商店價目不是 3.81C 原服資料。
3. **角色**：唯讀、不寫回角色本體是設計；CurHp/MP 不讀；Sex 沒用到。
4. **跨界**：多來源（最多 8 個）＋分埠選服＋各自結算都已具備；缺 objid 撞號處理和跨程式互連。B-land 時，X6、B8、B10 這類 patch 接線要轉成原始碼呼叫，不能漏。
