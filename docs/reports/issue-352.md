# Issue #352 報告 — T111：藥水與消耗品數值改讀 SQL

## 做了什麼
1. **對照表** `docs/design/items-consumables.md`：LiTo 支援的 11 項帶參數藥水（UserAddHp／StatusHaste／StatusBrave／StatusElfBrave／StatusWisdomPotion／UserMprBlue）＋ 3 項固定值藥水（解毒、失明、伊娃的祝福）＋ 開發用 1002，逐項對照 `1-伺服器/db/380_db.sql` 的 classname 與 executor 原始碼。結論：**數值全部一致**，沒有要修正的數值；1002 標「刻意不同」。
2. **改讀表**：新增 `ConsumableItemParameters`（`lito伺服器/LiTo381-Server/src/main/java/local/lito381/server/ConsumableItemParameters.java`）。
   - 啟動時在 `ResourceDatabase.load` 裡（`CombatDatabase.load` 之後）對 380_db 跑一次唯讀 `SELECT item_id,classname FROM etcitem WHERE item_id IN (…11 個編號…)`。
   - 解析照 1-伺服器 `set_set` 的預設與修正規則（min≤0→1、max<min→固定 min、時間≤0→300／600 秒、數字解析失敗沿用預設）。
   - 單列有問題（列不存在、寫的是別的 executor、設成「不扣原道具」的 `false` 模式）→ 那一列用內建值並寫 log；整個 DB 讀不到 → 全部用內建值並寫 log。`load` 會吞掉所有例外，不會讓伺服器啟動失敗。
   - `InventoryCatalog.healingRange／healingEffectGfx／bluePotionDurationSeconds` 改從這裡取值；新增 `bluePotionEffectGfx`、`selfHaste*`、`brave*`、`wisdomPotion*`、`elvenWafer*` 存取方法；`ClientSession` 藥水分派改用這些方法（只換識別字，行數沒變）。原本的常數保留，當作內建值。
3. **測試**：
   - `ConsumableItemParametersTest`（不需要 DB）：1-伺服器 11 列 classname 原文解析後與內建值完全相同；SQL 值改變時 `InventoryCatalog` 跟著變；解析邊界值；三種壞列退回內建值且有 log；沒有 DB 設定時 `load` 不丟例外、維持內建值。
   - `ConsumableItemParametersMySqlIntegrationTest`（CI MySQL，#334）：只在 `LITO_CI_DB_USER` 等於 `config/server.properties` 的帳號時執行；對 CI 的 380_db（`db/lito_static.sql`）實際載入，預期來源 `380_db.etcitem`、0 筆退回、值等於內建值。
4. `docs/reports/server-capability-matrix.md`「藥水與消耗品」列改成「一致（T111）」。

## 怎麼驗證
- **已驗證**
  - `mvn -B -q -DskipTests test-compile` 本機編譯成功（exit 0）。
  - 1-伺服器與 repo 的 `db/lito_static.sql` 中這 11 列的 classname 字串逐字相同（用 Select-String 直接比對兩份傾印）。
  - PR #360 CI（build run 43137309109、package run 43137309208）：`Server (Java 17 / Maven)`、**`Server tests`（`mvn -B test`）綠燈**、Launcher、Semantic gap、Build-Package 都通過。
  - CI MySQL job（非阻擋）：`ConsumableItemParametersMySqlIntegrationTest` **有執行且通過**（Tests run: 1, Failures: 0）。這個 job 整體是紅的，但失敗的是既有的 16 個類別（`ClientSessionNativeTournamentEconomyTest`、`ClientSessionNativeDropIntegrationTest` 等，錯誤多為 `this.accounts is null`／`selectedSourceAccounts`），main 的 base commit c57546b7（run 43095066705）同一個 job 也是紅的、有 17 個失敗類別；兩邊差異只有 3 個類別互換（PR 多 `ClientSessionClassicWorldMonsterGiveIntegrationTest`，main 多 `ClientSessionClassicWorldGroundIntegrationTest`、`ClientSessionHorrorOfDeathIntegrationTest`），錯誤同型，看起來跟測試執行順序有關，跟藥水無關（推測，未逐一追）。
  - 1-伺服器回復量公式 `min + random*(max-min+1)`（`UserAddHp.java:96-100`）與 LiTo `nextInt(min,max+1)`（`ClientSession.java:21852-21853`）分布一樣。
- **推測**
  - 正式 380_db 的這幾列和 1-伺服器傾印一樣（沒連正式 DB）；如果不一樣，現在 LiTo 會照正式表走，log 會印出「differing-from-built-in=N」與每一項的差異。
- **待驗證**
  - 本機沒跑測試（本機執行限制），結果全部來自 CI。
  - 實機：用正式設定啟動伺服器，確認 log 有 `[LiTo] consumable parameters loaded from 380_db.etcitem: items=11 differing-from-built-in=0 fallbacks=0`；遊戲內喝紅水、綠水、勇水、藍水，回復量／時間不變。交給 Codex／使用者。

## 未處理（列給主控）
1. **勇敢藥水職業限制**：`1-伺服器/db/380_db.sql:26737` 的 `limit_class=2`（只有騎士），repo `db/lito_static.sql:25758` 是 `127`；LiTo 勇水沒檢查職業。要先查正式 380_db 是哪個值，再決定 LiTo 要不要加限制（玩家可見）。
2. **藥水共用冷卻**：`delay_id=2`、`delay_time=500/1000` 毫秒，LiTo 藥水分派沒有讀，沒比對行為。
3. **其他消耗品（LC I8）**：變身、魔杖、磨刀石、料理、傳送等各自有 catalog，不是 classname 數值，沒在這次範圍。
4. `docs/design/database.md` 的存取清單是 `tools/db-grants/scan_db_access.py` 產生的，這次沒重產；新的 etcitem 讀取已在既有 GRANT（`tools/db-grants/generated/grants.sql` 的 `380_db.etcitem` SELECT）內，不需要新權限。

## 產出路徑
- `docs/design/items-consumables.md`
- `lito伺服器/LiTo381-Server/src/main/java/local/lito381/server/ConsumableItemParameters.java`（新）
- `lito伺服器/LiTo381-Server/src/main/java/local/lito381/server/InventoryCatalog.java`、`ClientSession.java`、`ResourceDatabase.java`
- `lito伺服器/LiTo381-Server/src/test/java/local/lito381/server/ConsumableItemParametersTest.java`、`ConsumableItemParametersMySqlIntegrationTest.java`
- `docs/reports/server-capability-matrix.md`
- 分支 `task/352`

## 給下一棒的提示
- 要新增讀表的藥水：在 `ConsumableItemParameters.BUILT_IN` 加一列（executor + 內建值），`InventoryCatalog.find` 那邊要有 `Effect.HEAL`／`BLUE_MPR` 等定義；測試 `SERVER1_CLASSNAMES` 也要補同一列，否則 `builtInValuesEqualServer1Classnames` 會失敗（刻意的）。
- 不支援 `false`（改扣其他物品）模式；要支援得改 `ClientSession` 的扣道具流程，不只是解析。
- CI 若 MySQL job 顯示 `ConsumableItemParametersMySqlIntegrationTest` 被跳過，看 `LITO_CI_DB_USER` 是否有傳到 `mvn test` 那一步。
