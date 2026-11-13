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

## 這次沒有比對的部分

- **職業限制（`limit_class` 欄，不在 classname 裡）**：勇敢藥水 40014 在 `1-伺服器/db/380_db.sql:26737` 是 `2`（只有騎士能用），但 repo 的 `db/lito_static.sql:25758` 是 `127`（全職業）；LiTo 現在勇敢藥水沒有職業檢查。兩份傾印不一致，要先查正式 380_db 才能決定，列入報告的待驗證。慎重藥水（8）、精靈餅乾（4）兩份一致，LiTo 已檢查（`ClientSession.java` `classCanUseEquipment(0x08)`／`(0x04)`）。
- **共用冷卻（`delay_id`／`delay_time`）**：治癒類 1-伺服器是群組 2、500／1000 毫秒；LiTo 藥水分派沒有讀這兩欄。不屬 classname，列為後續。
- **其他消耗品**（I8：變身卷軸、閃電魔杖、磨刀石、鑑定、料理、信紙、傳送、魔眼等）：效果種類不同、各自有專屬 catalog，不在這張表；魔眼已在 #338 讀 `etcitem_buff藥水設置`。
- 1-伺服器另有 LiTo 沒支援的同 executor 道具（例：40019、40022–40024、40029、40043、41141、140010–140012、240010），LiTo 背包裡沒有它們的定義，本任務不新增。
