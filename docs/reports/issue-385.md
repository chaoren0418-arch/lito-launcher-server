# Issue #385 報告 — T134：藥水共用冷卻（`etcitem.delay_id`／`delay_time`）

## 做了什麼

1. **對照 1-伺服器**（細節與行號在 `docs/design/items-consumables.md`「共用冷卻」節）：
   - 檢查：`C_ItemUSe.java:352-373`。HP > 0 時，若道具的 `delay_id` 群組還在冷卻 → `sendTeleportUnlockIfIdle()` 後 return，不扣道具、沒有訊息。
   - 拒絕封包：`L1PcInstance.java:2212-2228` → `S_Paralysis(TYPE_TELEPORT_UNLOCK)`＝opcode 202＋`0x07`（`S_Paralysis.java:39、57-61`、`OpcodesServer.java:153`），傳送中不送。
   - 建立：`C_ItemUSe.java:933-936` → `L1ItemDelay.onItemUse`（`L1ItemDelay.java:339-377`）；群組或時間為 0 不建（`:87-90`）；同群組取較晚期限（`:261-271`）；`期限 − 現在 ≤ 0` 即到期（`:242-251`）。
   - 14 項藥水的值：只有治癒類 40010（2／500）、40011、40012、40506（2／1000）有冷卻，其他 10 項是 0／0。`1-伺服器/db/380_db.sql`、repo `db/lito_static.sql`、M4 快照 `D:\BinEngine\environments\p24\sql\380_db.sql` 三份一致（`scratch/t134_delay_extract.py` 逐列解析）。
2. **LiTo 現況**（改動前）：`ClientSession.java:21910-22293` 藥水分派、`GmTravel.java:62-71` 入口都沒有使用間隔限制，每個封包都能喝一瓶。
3. **實作**：
   - `ConsumableItemParameters`：T111 的 SELECT 加上 `delay_id,delay_time`，`IN` 清單擴成 14 項；新增 `Delay`、`Row`、`BUILT_IN_DELAYS`，`Snapshot` 多 `delays`／`delayFallbacks`。選不到這兩欄 → 寫 log、改跑原本只有 classname 的查詢（T111 參數照常載入），冷卻全用內建值；個別列缺、NULL、`delay_id` 為負 → 該項用內建值並寫 log；連不上 DB → 全部內建值，`load` 不丟例外。
   - 新檔 `ItemUseDelays`：每個連線一份「群組 → 期限」，規則照 `L1ItemDelay`（共用群組、取較晚期限、剛好滿 `delay_time` 放行、到期順便移除）。
   - `ClientSession`：藥水分派前 `rejectItemUseDelay`（冷卻中 → 不扣、不產生效果，沒有傳送進行中就送 `S_PARALYSE` 模式 7）；9 個藥水分支在扣道具成功後 `startItemUseDelay`。
   - 客戶端對應：LiTo `S_PARALYSE`（0x25）模式 7＝`ParalysisMode.TELEPORT_UNLOCK`，`Protocol.java:3026-3050` 註解記錄 3.81C 接收函式 `sub_4A6A80` 接受 0..25；正式 0.6.71 已在傳送拒絕、共用技能冷卻拒絕送同一個封包。
4. **測試**：
   - `ConsumableItemParametersTest` 新增：內建冷卻＝1-伺服器 14 列；治癒類共用群組 2、其他無冷卻；**群組共用**（40010 後 40011／40506 被擋、加速藥水與其他群組不受影響）；**時間邊界**（500 ms：499.999… 擋、剛好 500 放行；1000 ms：999 擋、1000 放行）；同群組取較晚期限、0／負時間不建立；SQL 值覆蓋內建、壞列退回並寫 log；**讀表失敗**（JDBC 代理：delay 欄不存在 → 重試 classname 查詢、冷卻全內建、T111 參數不受影響）；無 DB 時 `load` 不丟例外且冷卻是內建值。
   - `ConsumableItemParametersMySqlIntegrationTest`：CI MySQL 的 380_db 讀出的冷卻＝內建值、無退回。
   - `ClientSessionNativeTournamentEconomyTest.encryptedHealingPotionsPublishNativeGfxAndConsumeAtFullHp`：原本連喝 3 瓶治癒藥水，現在第 2、3 瓶先立刻再喝一次，斷言只收到 `S_PARALYSE` 模式 7、藥水還在、HP 不變，再清掉冷卻喝成功。
5. **文件**：`docs/design/items-consumables.md` 新增「共用冷卻」節；`docs/acceptance/m4-schema-precheck.md` 由 `tools/acceptance/m4-schema-precheck.py --update-sql` 重產（`m4-precheck-rules.json` 的 etcitem 檢查擴成 14 列並顯示兩欄；P24 快照 14 列齊全、值相同）；`TASKS.md` T134 → review；`docs/reports/server-capability-matrix.md` 藥水列。

## 怎麼驗證

- **已驗證**
  - `mvn -B -q -o -DskipTests test-compile` 本機編譯成功（exit 0）。依本機規定**沒有在本機跑測試**，`mvn -B test` 交給 PR 的 CI。
  - 三份 SQL 傾印 14 列的 `delay_id`／`delay_time` 由 python 逐列解析（結果見上表）。
  - 授權：新讀的兩欄在 `tools/db-grants/generated/grants.sql:10` 既有的 `GRANT SELECT ON 380_db.etcitem`（表級）之內，不需新權限；M4 預檢重產後 static 缺表／缺欄位仍為 0。
- **推測**
  - 3.81C 客戶端收到模式 7 只解除請求鎖、沒有其他副作用：依 repo 既有逆向註記與正式版既有用法，本任務沒有重新反組譯 `sub_4A6A80`。
- **待驗證**
  - CI：`mvn -B test` Failures 0／Errors 0、MySQL job 的 `ConsumableItemParametersMySqlIntegrationTest`（看 PR 的 CI 結果）。
  - **實機連喝間隔**（交 Codex／使用者）：戰鬥中狂按治癒藥水 → 兩瓶之間至少 0.5 秒（治癒藥水）／1 秒（強力、終極、安特的水果）；混喝不同治癒藥水也受同一冷卻；加速、勇敢、藍水等不受影響；被擋時畫面不卡住、藥水數量不變、沒有訊息。伺服器 log 應有 `[LiTo] C_USE_ITEM rejected item-delay … delay-id=2` 與啟動時 `cooldowns=14 cooldown-differing=0 cooldown-fallbacks=0`。

## 未做（依任務「不做」）

- 勇敢藥水職業限制（`limit_class`）：留 Backlog。補充證據：M4 實驗快照 `D:\BinEngine\environments\p24\sql\380_db.sql:26337` 的 40014 也是 `2`（與 `1-伺服器` 傾印同，與 repo `lito_static.sql` 的 127 不同），仍需正式 380_db 的值才能決定。
- 其他消耗品的冷卻、502「道具禁止使用」群組（LiTo 沒有來源會建立它）。

## 和 1-伺服器的刻意差異

計時起點用處理當下的 `nanoTime`（1-伺服器用封包進站時間）；冷卻只在藥水真的扣掉後建立（1-伺服器在 executor 被 `stopPotion` 擋下時也建立，那次本來沒效果，不影響連喝速度）。見設計文件同節。

## 產出路徑

- `lito伺服器/LiTo381-Server/src/main/java/local/lito381/server/ConsumableItemParameters.java`、`ItemUseDelays.java`（新）、`ClientSession.java`
- `lito伺服器/LiTo381-Server/src/test/java/local/lito381/server/ConsumableItemParametersTest.java`、`ConsumableItemParametersMySqlIntegrationTest.java`、`ClientSessionNativeTournamentEconomyTest.java`
- `docs/design/items-consumables.md`、`docs/acceptance/m4-schema-precheck.md`、`tools/acceptance/m4-precheck-rules.json`、`TASKS.md`、`docs/reports/server-capability-matrix.md`
- 一次性腳本：`scratch/t134_delay_extract.py`（不進 git）
- 分支 `task/385`

## 給下一棒的提示

- 新藥水要冷卻：在 `ConsumableItemParameters.BUILT_IN_DELAYS` 加內建值，並在該藥水分支扣道具成功後呼叫 `startItemUseDelay(definition)`；檢查是統一在分派前做的，不用每個分支加。
- 測試裡要連續使用同群組道具時，用 `setField(session, "itemUseDelays", null)` 清冷卻（欄位是延遲建立的）。
- 若正式 380_db 的冷卻值和內建不同，啟動 log 會出現 `cooldown from 380_db.etcitem Delay[...]`，以資料庫為準，不用改程式。
