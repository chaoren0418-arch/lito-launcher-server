# 藥水與消耗品數值對照（T111 / Issue #352）

LiTo 伺服器支援的藥水與消耗品，逐項對照 `1-伺服器` 的 `etcitem.classname` 參數與 ItemExecutor 實作。

- 1-伺服器資料：`1-伺服器/db/380_db.sql`（etcitem 表，行號如下）；repo 內 CI 用的同一張表在 `lito伺服器/LiTo381-Server/db/lito_static.sql:25712` 起，以下各列字串完全相同（例外見「職業限制」）。
- LiTo 讀表：`ConsumableItemParameters`（`lito伺服器/LiTo381-Server/src/main/java/local/lito381/server/ConsumableItemParameters.java`）。啟動時在 `ResourceDatabase.load` 裡、`CombatDatabase.load` 之後，對 `database.static.database`（380_db）跑一次唯讀的 `SELECT item_id,classname FROM etcitem WHERE item_id IN (…)`。
- 退回規則：資料庫連不上、列不存在、classname 寫的是別的 executor、或設成「不扣原道具、改扣其他物品」（`false`）模式時，那一項保留內建值並寫 log `[LiTo] consumable parameters: …`；**不會**讓伺服器啟動失敗（`load` 會吞掉所有例外）。
- 解析規則照 1-伺服器 `set_set`：`hp/UserAddHp.java:141-204`（min≤0→1；max<min→固定 min；數字解析失敗沿用欄位預設）、`brave/StatusHaste.java:151-202`、`brave/StatusBrave.java:115-161`、`brave/StatusElfBrave.java:94-127`、`brave/StatusWisdomPotion.java:72-86`、`mp/UserMprBlue.java:88-102`（時間≤0→300／600 秒）。
- 回復量公式：1-伺服器 `UserAddHp.java:96-100` 是 `min + random*(max-min+1)`，即 [min,max] 均勻分布；LiTo `ClientSession.java:21852-21853` 是 `nextInt(min, max+1)`，同一個分布。之後的 PotionRecovery、污濁之水、水龍副本修正已在 `BattlePotionRecovery` 對齊過（LC I7），本任務沒改。

## 讀表項目（classname 帶參數）

| 道具 | item_id | 1-伺服器 classname（380_db.sql:行） | LiTo 內建值（改動前寫死的位置） | LiTo 現在 | 結論 |
|---|---|---|---|---|---|
| 治癒藥水 | 40010 | `hp.UserAddHp 10 20 189 true 0 0`（:26733） | 10–20、gfx 189（`InventoryCatalog.healingRange`／`healingEffectGfx`） | 讀表，內建同值 | 一致 |
| 強力治癒藥水 | 40011 | `hp.UserAddHp 30 50 194 true 0 0`（:26734） | 30–50、gfx 194 | 讀表 | 一致 |
| 終極治癒藥水 | 40012 | `hp.UserAddHp 60 80 197 true`（:26735） | 60–80、gfx 197 | 讀表 | 一致 |
| 安特的水果 | 40506 | `hp.UserAddHp 30 50 197 true`（:27229） | 30–50、gfx 197 | 讀表 | 一致 |
| 自我加速藥水 | 40013 | `brave.StatusHaste 300 191 true 40308 10`（:26736） | 300 秒、gfx 191（`SELF_HASTE_*`） | 讀表 | 一致（`true` 時 1-伺服器不看後兩個金幣參數，`StatusHaste.java:71-76`） |
| 勇敢藥水 | 40014 | `brave.StatusBrave 300 751 true`（:26737） | 300 秒、gfx 751（`BRAVE_*`） | 讀表 | 數值一致；職業限制見下 |
| 慎重藥水 | 40016 | `brave.StatusWisdomPotion 300 750`（:26739） | 300 秒、gfx 750、SP+2（`WISDOM_POTION_*`） | 讀表（SP+2 是 executor 寫死，`StatusWisdomPotion.java:56`，LiTo 也維持寫死） | 一致 |
| 精靈餅乾 | 40068 | `brave.StatusElfBrave 480 751 true`（:26791） | 480 秒、gfx 751（`ELVEN_WAFER_*`） | 讀表 | 一致 |
| 加速魔力回復藥水 | 40015 | `mp.UserMprBlue 600 190`（:26738） | 600 秒、gfx 190（`bluePotionDurationSeconds`、`BLUE_POTION_EFFECT_GFX`） | 讀表 | 一致 |
| 受祝福的加速魔力回復藥水 | 140015 | `mp.UserMprBlue 700 190`（:28493） | 700 秒、gfx 190 | 讀表 | 一致 |
| 智慧貨幣 | 40736 | `mp.UserMprBlue 600 190`（:27459） | 600 秒、gfx 190 | 讀表 | 一致 |
| 測試用治療藥水 | 1002 | （1-伺服器沒有這個編號） | 固定 25、無 gfx | 不讀表 | 刻意不同：LiTo 開發用舊相容道具，只在相容模式商店出現，沒有對應的 1-伺服器列 |

## 固定值項目（classname 不帶參數，數值寫在 1-伺服器 executor 裡）

| 道具 | item_id | 1-伺服器 | LiTo | 結論 |
|---|---|---|---|---|
| 解毒藥水 | 40017 | `Disintoxicat_Potion`（:26740）；gfx 192 寫死（`item_etcitem/Disintoxicat_Potion.java:40`） | `CURE_POISON_EFFECT_GFX = 192` | 一致（沒有參數可讀，維持寫死） |
| 失明藥水 | 40025 | `Blindness_Potion`（:26748）；16 秒寫死（`Blindness_Potion.java:45,58`） | `BLINDNESS_DURATION_SECONDS = 16` | 一致（同上） |
| 伊娃的祝福 | 40032 | `Water_Essence`（:26755）；+1800 秒、上限 3600、gfx 190（`Water_Essence.java:52-70`） | `BLESS_OF_EVA_DURATION_SECONDS = 1800`、`UNDERWATER_BREATH_MAX_SECONDS = 3600`、gfx 190 | 一致（同上） |

## 共用冷卻（`delay_id`／`delay_time`，T134 / Issue #385）

### 1-伺服器怎麼做

- 檢查（`1-伺服器/src/com/lineage/server/clientpackets/C_ItemUSe.java`）：
  - `:352` 只在角色 HP > 0 時檢查；`:354-363` 一般道具（type2=0）先取 `etcitem.delay_id`，若 502「道具禁止使用」租約存在就直接 return（沒送任何封包）。
  - `:365-373` `delay_id ≠ 0` 且該群組租約還在 → 記 `ITEM_REJECTED reason=item-delay`、呼叫 `pc.sendTeleportUnlockIfIdle()` 後 return。**沒有訊息、沒有效果、道具不扣**。
  - `:933-936` 道具用完（executor 跑完）後 `L1ItemDelay.onItemUse(client, useItem)` 建立租約。
- 拒絕時的封包：`L1PcInstance.java:2212-2228` `sendTeleportUnlockIfIdle()`，角色不在傳送中才送 `S_Paralysis(TYPE_TELEPORT_UNLOCK, false)`；`S_Paralysis.java:39、57-61` 寫成 `S_OPCODE_PARALYSIS`（`OpcodesServer.java:153`，202）＋ 模式位元組 `0x07`。
- 租約（`1-伺服器/src/com/lineage/server/model/L1ItemDelay.java`）：
  - `:339-377` `onItemUse(pc, item)`：type2=0 取 `get_delayid()`／`get_delaytime()`；`:87-90` 群組或時間為 0 不建立；`:99` 負的時間夾成 0。
  - `:100-103` 期限＝封包到達時間＋`delay_time` 毫秒，`L1Character.installOrMergeItemDelay`（`L1Character.java:1899-1906`）同群組已有租約時 `merge`（`L1ItemDelay.java:261-271`）取**較晚**的期限。
  - `:242-251` `isExpiredForCurrentAction`：`期限 − 現在 ≤ 0` 就算到期，也就是**剛好滿 `delay_time` 時可以再用**；`L1Character.hasItemDelay`（`:1938-1955`）順便把到期的租約移除。
- 群組是「共用」的：檢查與建立都只看 `delay_id`，所以同群組的不同藥水互相擋。

### 14 項藥水的值（`1-伺服器/db/380_db.sql` etcitem；欄位順序 `:26691-26716`）

| 道具 | item_id | 行 | delay_id | delay_time（毫秒） |
|---|---|---|---|---|
| 治癒藥水 | 40010 | :26733 | 2 | 500 |
| 強力治癒藥水 | 40011 | :26734 | 2 | 1000 |
| 終極治癒藥水 | 40012 | :26735 | 2 | 1000 |
| 安特的水果 | 40506 | :27229 | 2 | 1000 |
| 自我加速藥水 | 40013 | :26736 | 0 | 0 |
| 勇敢藥水 | 40014 | :26737 | 0 | 0 |
| 加速魔力回復藥水 | 40015 | :26738 | 0 | 0 |
| 慎重藥水 | 40016 | :26739 | 0 | 0 |
| 解毒藥水 | 40017 | :26740 | 0 | 0 |
| 失明藥水 | 40025 | :26748 | 0 | 0 |
| 伊娃的祝福 | 40032 | :26755 | 0 | 0 |
| 精靈餅乾 | 40068 | :26791 | 0 | 0 |
| 智慧貨幣 | 40736 | :27459 | 0 | 0 |
| 受祝福的加速魔力回復藥水 | 140015 | :28493 | 0 | 0 |

repo 的 `db/lito_static.sql`（CI 用，:25754-27514）與 M4 實驗快照 `D:\BinEngine\environments\p24\sql\380_db.sql`（:26333 起）這兩欄 14 列都一樣。三份的 `delay_effect` 都是 0（那是另一個持久化的「道具使用延遲」，本任務不涉及）。測試用治療藥水 1002 沒有 1-伺服器列，維持沒有冷卻。

### LiTo 改動前

`ClientSession.java`（改動前 main）`sourcePoisonBody$useInventoryItemBeforeTravel`（:21445 起）的藥水分派 `Effect.HEAL`（:21910）到 `Effect.BREATH`（:22264）各分支只檢查階段／觀戰／HP／職業，**沒有任何使用間隔限制**；`GmTravel.useItem`（`GmTravel.java:62-71`）也只分流風暴魔杖。玩家可以每個封包喝一瓶。

### LiTo 改動後

- 讀表：`ConsumableItemParameters.read`（`ConsumableItemParameters.java:203-230`）把 T111 那條 SELECT 改成 `SELECT item_id,classname,delay_id,delay_time FROM etcitem WHERE item_id IN (14 項)`；若這兩欄選不到（舊 schema），寫 log 後改跑原本只有 classname 的查詢，T111 參數照樣載入。內建值 `BUILT_IN_DELAYS`（:137-152）＝上表。`resolve` 逐項比對：列不存在、欄位讀不到（NULL）、`delay_id` 為負 → 該項用內建值並寫 `[LiTo] consumable parameters: item N cooldown …`；結尾 log 多了 `cooldowns=14 cooldown-differing=N cooldown-fallbacks=N`。資料庫整個連不上時照 T111 規則全部用內建值，**不會**讓啟動失敗。
- 冷卻狀態：`ItemUseDelays`（新檔）每個連線一份，`群組 → 期限（nanoTime）`。`blocked` 用 `期限 − 現在 > 0` 判斷、到期順便移除；`start` 同群組取較晚的期限；群組或時間 ≤ 0 不建立。
- 檢查：`ClientSession.java:21910` 在藥水分派之前呼叫 `rejectItemUseDelay`（:26080-26092）：道具有作用中的群組、角色 HP > 0、群組還在冷卻 → 不扣道具、不產生效果，記 `[LiTo] C_USE_ITEM rejected item-delay …`，在沒有傳送進行中（`pendingPortal`／`pendingPortalReady`／`pendingClassicWorldPortal` 都沒有）時送 `S_PARALYSE` 模式 7（`Protocol.paralysis(TELEPORT_UNLOCK)`，即既有的 `sendTeleportCursorUnlock`）。
- 建立：9 個藥水分支在 `inventory.remove` 成功、確定會產生效果後呼叫 `startItemUseDelay`（:21921、21989、22028、22063、22099、22131、22209、22262、22283）。
- 客戶端：LiTo 的 `S_PARALYSE`（`Protocol.java:474`，0x25）模式 7 是 `ParalysisMode.TELEPORT_UNLOCK`（`Protocol.java:3026-3050`），註解記錄 3.81C 原生接收函式 `sub_4A6A80` 接受 0..25；`docs/maintenance/movement-lock-0.6.66.md` 記錄模式 7 不清動作鎖 bit 8。正式 0.6.71 已在傳送拒絕、共用技能冷卻拒絕（`ClientSession.java` `shared-spell-cooldown`）送同一個封包。

### 和 1-伺服器的刻意差異

1. **計時起點**：1-伺服器用封包進站時間（`PacketExecutionContext`），LiTo 用處理 C_USE_ITEM 當下的 `System.nanoTime()`。LiTo 每個連線依序處理封包，差距只有排隊時間。
2. **租約建立時機**：1-伺服器在 executor 跑完後一律建立（即使 executor 內 `stopPotion` 擋下沒喝成），LiTo 只在藥水真的扣掉後建立。被擋下的那次本來就沒有效果，不會讓玩家比原服更快連喝。
3. **502「道具禁止使用」**：LiTo 沒有任何來源會建立群組 502，所以沒有移植這一層。
4. 1-伺服器的冷卻在角色斷線時清空；LiTo 冷卻跟著連線物件，斷線重連同樣是新的一份。

## 這次沒有比對的部分

- **職業限制（`limit_class` 欄，不在 classname 裡）**：勇敢藥水 40014 在 `1-伺服器/db/380_db.sql:26737` 是 `2`（只有騎士能用），但 repo 的 `db/lito_static.sql:25758` 是 `127`（全職業）；M4 實驗快照 `D:\BinEngine\environments\p24\sql\380_db.sql:26337` 也是 `2`（T134 查到，僅供參考，仍不是正式 380_db）。LiTo 現在勇敢藥水沒有職業檢查。要先查正式 380_db 才能決定，列 Backlog。慎重藥水（8）、精靈餅乾（4）兩份一致，LiTo 已檢查（`ClientSession.java` `classCanUseEquipment(0x08)`／`(0x04)`）。
- **其他消耗品**（I8：變身卷軸、閃電魔杖、磨刀石、鑑定、料理、信紙、傳送、魔眼等）：效果種類不同、各自有專屬 catalog，不在這張表；魔眼已在 #338 讀 `etcitem_buff藥水設置`。
- 1-伺服器另有 LiTo 沒支援的同 executor 道具（例：40019、40022–40024、40029、40043、41141、140010–140012、240010），LiTo 背包裡沒有它們的定義，本任務不新增。
