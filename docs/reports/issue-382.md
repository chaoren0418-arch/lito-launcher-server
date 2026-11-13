# Issue #382 報告：T131 開戰與斷線同時發生的鎖順序死結＋CI 測試 job 逾時

來源：#376（M3d）A 類；#369、#370 報告的移交事項。PR #392，分支 `task/382`。

## 結論

- 死結已在 `src/main` 修正：`DurableBattleSettlement.context()` 不再於持有 `CONTEXTS` 時取 `GameHub` 鎖。修正後全檔沒有任何路徑會在持有 `CONTEXTS` 時取其他鎖。
- 新的確定性測試在修正前紅燈、修正後綠燈（都在必過的 `Server tests` job）。
- `ModernSiegeWireOracleTest` **沒有卡住過**：三次「卡 6 小時」的 run 裡它都在 3 秒內 5/5 通過。真正卡住的是同一個死結，先前兩份報告是誤認。沒有對它做任何修改。
- CI 兩個測試 job 都有 30 分鐘上限；129 個使用真 `GameHub` 的測試類別全部有類別層級逾時。
- 正式 JAR（0.6.71）沒有改，仍有這個死結。

## 做了什麼

### 1. 修正（`S/` = `lito伺服器/LiTo381-Server/src/main/java/local/lito381/server/`）

`S/DurableBattleSettlement.java`（行數不變，其他文件引用的行號仍有效）：

- `context()`（`:26-29`）：測試房旗標 `NativeRoomRules.testRoom(hub, room)` 改在第一個 `synchronized(hub)` 區塊內、取 `CONTEXTS` 之前讀好。
- `Context` 建構子（`:9`）改收 `boolean test`，不再自己呼叫 `NativeRoomRules.testRoom`（那會取 `GameHub` 鎖）。

行為差異只有一個：測試房旗標原本只在「建立新 `Context`」時讀，現在每次 `context()` 都讀一次（多掃一次頻道清單，仍在 `GameHub` 鎖內）；既有 `Context` 被沿用時，新讀到的值不使用，結果與修正前相同。

### 2. 鎖順序檢查

逐一看過 `CONTEXTS` 的五個持有點（`context` `:28`、`cleanupAllowed` `:79`、`forget` `:81`、`reconcileRecovered` `:129`、`retry` `:148`），以及 `SettlementJournal`、`SettlementSql` 的鎖。任務描述列的 `begin`、`record` 本身不直接持有 `CONTEXTS`：`begin` 經 `context()` 取，`record` 持有的是 `Ticket`。

- 修正後 `CONTEXTS` 是葉鎖，鎖內只讀寫對照表與 `Context` 的 volatile 欄位。
- `SettlementJournal.save`／`pending` 鎖內只做檔案寫入；`replay` 不取 Java 鎖。
- `SettlementSql` 只有 `ROUTES`、`Route` 兩把葉鎖；資料庫的 `GET_LOCK` 等待上限 5 秒，不會永久互等。
- 沒有發現第二個鎖順序相反的路徑。

完整的表寫在 `docs/design/gameplay.md` §4.2「鎖順序（T131 #382）」。

### 3. 測試 `DurableBattleSettlementLockOrderTest`（不需 MySQL、不需 `config/server.properties`）

手法：把 `CONTEXTS` 裡這個 hub 的房間對照表換成一個 `HashMap` 子類別，`get` 會在「`context()` 正持有 `CONTEXTS`」的那一點執行掛鉤。之後所有等待都是等 latch 或等 `ThreadMXBean` 回報的執行緒狀態，沒有 sleep。類別層級 `@Timeout` 90 秒。

1. `contextNeverEntersTheHubMonitorWhileHoldingContexts`
   - 第一步（探測）：B 進 `context()` 持有 `CONTEXTS` 後停住；A 取得 `GameHub` 鎖後才放 B 繼續。B 若被 A 持有的鎖擋住，就是鎖順序相反，測試失敗。這一步 A **不去取** `CONTEXTS`，所以在未修正的程式上只會失敗、不會真的死結（Java monitor 死結解不開，會拖垮同一個 JVM 之後的所有測試）。
   - 第二步（真實交錯，第一步通過才會執行）：B 持有 `CONTEXTS` 停住，A 呼叫正式的 `GameHub.endBattle`（`synchronized` → `requireDurable` → `cleanupAllowed`），等到 `ThreadMXBean` 確認「A 持有 `GameHub` 鎖、卡在 B 持有的 `CONTEXTS`」之後才放 B 繼續；兩條執行緒都必須結束。這就是 thread dump 的交錯，修正前在這一點互等。
2. `contextStillRecordsTheTestRoomFlagAndBattleGeneration`：預設房的 `Context.testRoom` 為 true、自建房為 false、房號 0 為 false；同房同世代沿用同一個 `Context`，世代加一後換新的（新的 battle id）。確認搬出鎖外的兩個值沒有讀錯。

### 4. CI（`.github/workflows/build.yml`）

- `server-test`、`server-integration-test` 兩個 job 都加 `timeout-minutes: 30`（原本都沒有）。
- 兩個 job 的 `mvn -B test` 步驟另加 `timeout-minutes: 25`：步驟逾時算失敗（不是 cancelled），後面的摘要與證據上傳步驟仍會執行。

### 5. 真 `GameHub` 測試的類別層級逾時

`src/test` 裡有 `new GameHub(` 的測試類別共 129 個（不含本任務新增的 1 個）；其中 2 個原本就有 `@Timeout`（`BattleDisconnectStressMySqlIntegrationTest`、`ClientSessionLastRoomMemberExitTest`），其餘 **127 個**補上：

```java
@org.junit.jupiter.api.Timeout(value = 120, unit = java.util.concurrent.TimeUnit.SECONDS,
        threadMode = org.junit.jupiter.api.Timeout.ThreadMode.SEPARATE_THREAD) // T131 (#382): real GameHub
```

- 用 `SEPARATE_THREAD`：預設模式只會中斷執行緒，而卡在 monitor 的執行緒不理會中斷，等於沒有逾時。
- 120 秒的依據：這 127 個類別在 main（run 43365092150，兩個 job）最慢的單一測試方法是 6.6 秒。
- 這 127 個類別都沒有 `@BeforeEach`／`@AfterEach`，測試檔本身也沒有用 `ThreadLocal`，所以測試方法換到另一條執行緒執行不影響夾具。
- 用腳本插入（`scratch/t382/sweep.py`，不進 git），每個檔案只多 2 行，寫全名所以不動 import。

### 6. `ModernSiegeWireOracleTest` 的調查

重新下載四個被取消的 MySQL job log（`scratch/t382/mysql-*.log`，不進 git）：

| Run | 來源 | `ModernSiegeWireOracleTest` | 實際卡住的地方 |
|---|---|---|---|
| 43264755522 | task/369 | 17:09:17 開始、17:09:35 `Tests run: 5, Failures: 0, Errors: 0`（2.6 秒） | `BattleDisconnectStressMySqlIntegrationTest` 240 秒逾時，dump `deadlocked=[1140, 2046, 2047]`；之後最後一行是 `Running …ClientSessionLitoClanRosterIntegrationTest`（用真 `GameHub`、當時沒有逾時），卡到 6 小時 |
| 43264993090 | task/368 | 通過 | 同上，`deadlocked=[1132, 2052, 2051]`，同樣卡在 `ClientSessionLitoClanRosterIntegrationTest` |
| 43264734337 | task/370 | 通過 | `deadlocked=[947, 2047, 2045]`，卡在 `ClientSessionLastRoomMemberExitTest`（#370 報告已記錄） |
| 43144899123 | task/355 | 03:05:15 開始、03:05:25 5/5 通過（1.6 秒） | 最後一行是 `Running …BattleDisconnectStressMySqlIntegrationTest`（當時還沒有逾時與 thread dump），12 分鐘後被手動取消 |

run 43264755522 的 dump 與 #370 記錄的是同一個循環：`pool-381-thread-2` 在 `NativeRoomRules.testRoom`（`Context.<init>` ← `context` `:28` ← `begin`）等 `GameHub`，持有 `CONTEXTS`；`pool-381-thread-3` 在 `cleanupAllowed` `:79`（← `resetOrRemove` ← `GameHub.disconnect`）等 `CONTEXTS`，持有 `GameHub`。同一份 dump 還顯示**其他 hub** 的 `lito381-battle-clock` 與 `lito-settlement-recovery` 也卡在同一把 `CONTEXTS`，證實是全 JVM 的影響。

`ModernSiegeWireOracleTest` 本身不使用 `GameHub`，也不經過 `DurableBattleSettlement`。依任務指示不硬修、也沒有加逾時（它沒有卡住，加了沒有對象）。

## 怎麼驗證

### 已驗證

- **修正前紅燈**：commit `dcd85f8b`（只有新測試），run 43370888921 `Server tests`：`Tests run: 7452, Failures: 1, Errors: 0, Skipped: 117`。唯一失敗是 `DurableBattleSettlementLockOrderTest.contextNeverEntersTheHubMonitorWhileHoldingContexts:82->probe:141`，訊息 `DurableBattleSettlement.context() waited for the GameHub monitor while holding CONTEXTS…`；同類別另一個測試通過。MySQL job 同一個測試同樣失敗（`7719, Failures: 40, Errors: 34`，比 main 多的 1 個就是它）。
- **修正後綠燈**：commit `44728850`，run 43372164916：
  - `Server tests`（必過）：`Tests run: 7452, Failures: 0, Errors: 0, Skipped: 117`，BUILD SUCCESS，8 分 58 秒；`DurableBattleSettlementLockOrderTest` 2/2 通過（0.31 秒）。與 main（run 43365092150，`39c8f849`，7450／0／0／117）逐類別比對：只多出新類別的 2 個測試，其餘類別的測試數相同。127 個改成另一條執行緒執行的類別全部通過。
  - `Server tests (CI MySQL fixture)`：`Tests run: 7719, Failures: 39, Errors: 34, Skipped: 119`；新測試 2/2 通過。與 main 同一個 job（7717／39／34／119）用 surefire XML 逐類別、逐測試案例比對：失敗的 22 個類別相同、73 個失敗案例相同，**沒有新增失敗，也沒有消失的失敗**；多出的 2 個測試就是新類別。job 仍是紅燈，原因是 #334 既有的失敗。
  - `Server (Java 17 / Maven)`、可重現 JAR、Semantic gap、Launcher、Build-Package 全部通過。
- 兩個 job 的 `timeout-minutes` 已在工作流程檔內；本次各 run 都在上限內完成（MySQL job 最長 13 分 11 秒）。
- 本機 `mvn -B -q -o -DskipTests test-compile` 通過（依本機限制沒有在本機執行測試）。
- 四個歷史 run 的 log 重新下載後逐行確認（上表）；`docs/design/gameplay.md` 新增段落引用的行號已對過程式。

### 推測

- run 43144899123（task/355）也是同一個死結：卡住的類別就是會觸發它的 `BattleDisconnectStressMySqlIntegrationTest`，但當時沒有 thread dump，無法直接證明。
- 兩個 6 小時 run 最後卡在 `ClientSessionLitoClanRosterIntegrationTest` 的原因是它的 hub 清理走到 `cleanupAllowed` 等同一把 `CONTEXTS`：依程式路徑推論，那個時間點沒有 dump。
- 修正後的 MySQL job 這次跑 12 分 23 秒（main 是 8 分 46 秒）。變慢分散在沒有被本任務改動的類別（例如 `ClassicWorldMonsterItemUseTest` 12→20 秒），同一 commit 的 `Server tests` 則與 main 相同（8:58／8:54），判斷是 runner 速度差異，不是新加的逾時造成。
- 斷線判定時 WAL 的 `fsync` 是在 `GameHub` 鎖內做的（`battleDisconnectWork` → `freeze` → `journal.save`）；磁碟慢時全大廳會短暫等待。不是死結，沒有量測，已寫進 §4.2 鎖順序表。

### 待驗證

- 實機雙人「一人開戰、同時另一人斷線」後伺服器仍可離房、開新戰鬥：交 Codex／使用者（M4）。
- job 層級 `timeout-minutes` 真的觸發時，run 的最終結論是什麼（`continue-on-error` 的 MySQL job 被逾時取消後整個 run 算不算 cancelled）沒有實際觸發過；所以另外加了步驟層級的 25 分鐘上限，讓它先以「失敗」結束。
- `GameHub` 與 `ClientSession` 之間結算以外路徑的鎖順序沒有檢查（§4.2 表內已註明範圍）。

## 未驗證什麼

- 實機、真實 TCP 斷線。
- 正式 JAR（0.6.71／E149）沒有改；這個修正只在 `src/main`，部署要走 patch 鏈（T002）。
- 沒有做長時間、多 hub 的壓力測試；既有的 `BattleDisconnectStressMySqlIntegrationTest` 在本次兩個 run 都通過，但它本來就是偶發才卡。

## 產出路徑

- `lito伺服器/LiTo381-Server/src/main/java/local/lito381/server/DurableBattleSettlement.java`
- `lito伺服器/LiTo381-Server/src/test/java/local/lito381/server/DurableBattleSettlementLockOrderTest.java`
- `lito伺服器/LiTo381-Server/src/test/java/local/lito381/server/*.java`（127 個類別各加 2 行 `@Timeout`）
- `.github/workflows/build.yml`
- `docs/design/gameplay.md`（§4.2 鎖順序表、文末待驗證一條）
- `docs/reports/issue-382.md`
- 分支 `task/382`，PR #392

## 給下一棒的提示

- 主控：`TASKS.md` 的 T131 列（Issue 欄「待建」、狀態 todo）本任務沒有改，避免和同時進行的 T127–T136 互相衝突；合併後請回填 #382 與狀態。完成定義第 4 項「死結已修正並有測試；CI 的 MySQL job 有逾時上限」已達成。
- `docs/reports/issue-369.md:96` 與 `issue-355.md:91` 寫的「卡在 `ModernSiegeWireOracleTest`」是誤認，以本報告為準；不需要再為它開任務。
- 之後新增使用真 `GameHub` 的測試類別請加同樣的類別層級 `@Timeout`（`SEPARATE_THREAD`）。若類別有 `@BeforeEach` 設定 `ThreadLocal`，要注意測試方法會在另一條執行緒執行。
- 改 `DurableBattleSettlement` 時遵守 §4.2 的表：持有 `CONTEXTS` 時不取任何鎖；要從 hub 讀的東西先在 `GameHub` 鎖內讀好。
- 若要把修正帶進正式 JAR：只需 `DurableBattleSettlement` 與 `DurableBattleSettlement$Context` 兩個 class（建構子簽章多一個 `boolean`）。
- 實機核對：兩人在自建房開戰倒數結束的瞬間，第三人（或其中一人）強制關閉客戶端；之後任何人都還能離房、建房、開戰，伺服器 log 持續有輸出。
