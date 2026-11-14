# Issue #435 — T149：其餘 8 組 class 守門改 Maven 相容＋逐案獨立執行選項

**結論：工具改完、離線測試已寫好，等 CI 驗證；本機沒有執行任何程式或測試。**

- 8 組裡有 7 組（existing 13／16、affected 04／05／07／12／16）的 GameStateStore 守門已接上 Maven 相容模式。
- existing 15 也接上了，但它比對的 `Main.class`／`SourceLogin.class` 跟 Maven 版**真的有程式碼差異**。依規則不能放寬，所以它在 Maven 模式**仍會失敗**，差異會寫進收據。
- targeted、composed、player、existing-20、affected-18 五個入口都加了 `--continue-per-case`。預設行為完全不變。
- 「案」的單位是入口能控制的最小單位：一支 test driver 或一支 probe。Java probe 內部的逐案迴圈本張沒有改（見「推測／限制」）。

## 做了什麼

### 1. class 守門（範圍：`tools/combat-continuation/`，沒改 `src/main`，也沒改 `--resources` 底下沒進 git 的原始驅動）

**靜態分析（已驗證，純 Python 解析，沒執行 JVM）**：用 Maven JAR `84ba0cde…`（與 #433 相同）逐一比對這 8 組守門的 class。

| 組別 | 守門內容 | 比對歷史 class（normalized）的結果 | 處理方式 |
|---|---|---|---|
| existing 13 `probe-player-action-authority.py` | parent JAR 的 GameStateStore 必須等於受測 JAR 的 | `normalized-differ`：Maven 多了 `rankingPeriodKeyAt(J)I`、`validRankingPeriodKey(I)Z` | 改成「每個 runtime 用自己的 overlay」（見下方 A） |
| affected 04 `…cast-material-preflight.py` | 同上；observer 另外會蓋掉 `RuntimeInventory` | GameStateStore 同上；RuntimeInventory 有 4 個方法不同 | A，兩個 class 都檢查 |
| affected 05／07／12／16 | 同上 | 同上（只有 GameStateStore） | A |
| existing 16 `replay-player-action-transaction.py` | 凍結的歷史 overlay，parent 的 GameStateStore 必須等於受測 JAR 的 | 同上 | 重新產生凍結 overlay（見下方 B） |
| existing 15 `replay-player-action-main-owned.py` | 凍結的 Main／SourceLogin overlay；parent 與受測 JAR 這兩個 class 必須相同 | `normalized-differ`：`Main.accept(…)`、`Main.main`、`SourceLogin.lambda$start$0` 有指令差異 | 改走 normalized 比對（見下方 C）。**仍然失敗** |

GameStateStore 多出的兩個方法是 `SettlementSql`／`SharedDatabase` 實際會呼叫的方法（#418 已確認），不是編譯器表示方式的差別。所以「拿 normalized 結果跟歷史 class 比」這條路**一定過不了，也不應該過**。原本的守門是為了防止「用歷史 class 產生的 overlay 偷換掉受測 JAR 自己的 class」。Maven 模式改成**直接消除偷換**，不是放寬守門：

- **A. 每個 runtime 用自己的 overlay**（`assert_runtime_overlays`）：
  - 同一支 observer 分別對受測 JAR 產生 `memory-observer`，對 parent／before JAR 產生 `memory-observer-parent`。每個分支的 classpath 用自己 runtime 的 overlay（`runtime_overlay`）。
  - 必須全部成立才放行：
    1. 每個 overlay 的檔案剛好就是該 observer 會寫的那幾個 class。
    2. 每個 class 去掉探針 hook 之後，與該 runtime 自己的 class 正規化相同（`compare_overlay`，只排除 `max_stack`）。
    3. 兩個 overlay 去掉的 hook 集合完全相同。
  - 歷史 class 與 Maven class 的比對只寫進收據，不作為放行依據。
  - before／parent 分支跑的 overlay，跟候選版 exact 模式跑的是同一支 observer、同一個來源 class 產生的。
- **B. 重新產生凍結 overlay**（`regenerate_frozen_overlay`，用於 existing 16，以及 composed 的 Fault／Ground Clock）：
  1. 先驗凍結 overlay 確實是「parent class 加上 hook」。
  2. 驗 observer 原始碼的 SHA 必須是 `f50b3f6e…`，也就是當年產生凍結 overlay 的那一份。
  3. 把凍結 overlay 改名保留成 `<overlay>-historical`。
  4. 用同一支 observer 從受測 JAR 重寫 overlay，再驗它是「受測 class 加上**同一組** hook」。
- **C. normalized 比對**（`assert_normalized_class`，用於 existing 15）：凍結的 Main overlay 會整個取代受測 JAR 的 Main。只有在 Main／SourceLogin 證明 normalized 相同時，這個取代才等於「同一個程式」。目前比對結果是不同，所以會失敗並列出差異方法。
- **receiver hook（`class_identity.py`）**：`ObserveNativeMaterialPreflight` 插入的是 `aload_0; invokestatic afterCopy`。新增 `receiver_hooks` 參數，**只有**緊接在該 hook 前面的那一個 `aload_0` 會跟著 hook 一起去掉。少了這個 load，或 hook 旁邊還有別的改動，都會判不同。
- **exact 模式**（候選版）：`adapt_class_guards` 直接回傳原文，驅動副本跟以前一模一樣，維持位元組比對。composed 的 exact 路徑也保留原本的 `assert a.read(...)==b.read(...)`。
- Maven 模式下，這 8 支驅動任一支找不到預期的守門寫法、observer 不在已知清單、或守門出現不只一處，都會直接 AssertionError，不會默默跳過。

**用真實歷史產物做的靜態驗證（已驗證）**：

- 6 組 observer 的歷史 overlay，都是當年從 parent JAR 產生的：
  - 對 parent 的檢查全部判 `runtime-class-plus-probe-hooks-only`。affected 04 的 RuntimeInventory 去掉的是 `aload_0` 加上 `afterCopy`。
  - 同一批 overlay 拿去比 Maven class，全部判 `overlay-differs-beyond-probe-hooks`。這表示新守門仍然抓得到偷換。
- composed 會用到的 `build/boundaries/memory-observer`（共 36 個 stage 有這個目錄），凍結 overlay 對各自的來源 JAR 全部是「只差 hook」，對 Maven 則全部判不同。
- Maven 的 GameStateStore 有唯一的 `saveEconomyState`，裡面恰好一次 `save()`；RuntimeInventory 的 `copy()` 恰好一個 `areturn`。這符合 observer 本身的前置條件。
- 8 支真實驅動經過 `adapt_class_guards` 和 `adapt_driver` 改寫後都能通過 `ast.parse`；exact 模式下的輸出與原文逐字相同。
- 腳本與輸出放在 `scratch/issue-435/`（被 git 忽略）：`static_compare.py`、`static_historical_overlays.py`、`static_composed_overlays.py`、`static_observer_preconditions.py`、`adapt_dry_run.py`、`diff_methods.py`。

**額外處理（不在原本 8 組內，但同一個入口、同一種守門）**：`replay_composed_materials.py` 的 Fault／Ground Clock 也是「凍結 overlay 加位元組守門」。#433 的 group-differences 有 16 組 composed Fault／Clock（15 組 Fault 加 Ground Clock）在 Maven 側卡在這裡。它們已改走 B。

### 2. `--continue-per-case`（逐案獨立執行）

| 入口 | 一案＝ | 預設（不加選項） |
|---|---|---|
| `cancellation-battle-runtime/replay_targeted.py` | 一個 stage/folder 的 test driver（27 案） | 遇到失敗就停，與原本相同 |
| `cancellation-battle-runtime/replay_player_cancellation.py` | 一個 test driver（3 案） | 同上 |
| `replay_composed_materials.py` | 一個 probe（例如 `WeaknessWorldNpc`） | 同上 |
| `full_regression.py`（existing 20） | 一支驅動 | 同上 |
| `affected_regressions.py`（affected 18） | 一支驅動 | 同上 |

- 每一案的程式碼原封不動搬進一個函式，斷言、逾時、REUSE 規則都沒有改。`CaseRunner` 在預設模式下就是直接呼叫那個函式。
- 每一案記錄 `status`（`passed`／`failed`／`blocked-prerequisite`）、`first_failure`、起訖時間。
  - `first_failure` 的取用順序：該案這次寫出的 `result.json` 裡的 `error` → 該案 log 最後一行像錯誤訊息的內容 → 例外本身。
  - 逐案模式下，targeted／player 的子程序輸出另外寫到 `<stage>/<folder>.case.log`。
- `candidate_regression.py` 拒絕這個選項：它的 reconcile 流程要解讀「第一個失敗的驅動」，逐案模式請直接執行 full／affected 兩個入口。
- `affected_regressions.py` 在預設模式仍然要求 existing-20 已通過。逐案模式下改成把這個條件記進 `entry_prerequisites`，案例照樣全部執行；只要這個條件不成立，收據就不可能變成 passed。

### 3. 收據如何區分「案例失敗」與「入口中止」

逐案模式的收據（targeted／player 是 `receipt.json`，composed 是 `result.json`，full／affected 是 manifest）會多出以下欄位：

| 欄位 | 內容 |
|---|---|
| `case_mode` | `continue-per-case` |
| `cases`、`case_summary` | 每案結果；摘要含 `planned`、`passed`、`failed`、`blocked-prerequisite`、`not_run` |
| `entry_outcome` | `completed`（所有案例都跑過）或 `aborted`（入口本身中止） |
| `failure_kind` | `null`（全部通過）、`case-failures`、`entry-prerequisite-failed`、`entry-aborted`（另附 `entry_error`） |
| 結束碼 | 全部通過＝0；有案例失敗＝**2**；入口中止（守門、建置、正式檔保護、Ctrl-C）＝1 並印出 traceback |

`status` 仍只有 `passed`／`failed`（full 為 `passed-…`），所以 `seal_validation.py` 的判斷完全不受影響。只要有一案沒過，就不會出現 passed。

### 4. existing 19／20 的前置依賴

- existing 19 `replay-player-action-main-interrupt.py` 與 20 `replay-player-action-main.py` 都會讀 `regression/replay-current.json`。這個檔由 **existing 11 `replay-native-de-item-state.py`** 產生；20 另外要求它的 manifest 狀態是 `passed-…`，而且 runtime 相同。
- `full_regression.py` 的執行順序本來就是 11 在 19／20 之前。新增的 `PREREQUISITES` 表會在啟動時 assert 這個先後順序，所以順序不可能被改錯。
- 逐案模式下：如果 11 這次沒有通過，或檔案不存在，19／20 會記成 `blocked-prerequisite`，附上原因與提示（「先用同一組 `--jar`／`--out` 跑 existing 11」），不會拿前一次留下的舊檔去跑。被擋下的案例不算通過，結束碼為 2。
- 預設模式行為不變。

## 怎麼驗證

- **已驗證（本機，只用靜態工具）**：上面列的靜態比對與真實驅動的改寫 dry-run；所有改動過的 `.py` 都能通過 `ast.parse`。
- **交給 CI**（`combat-regression-tools` job 原本就會跑這兩個檔，沒有改 workflow）：
  - `test_class_identity.py` 新增：
    - `ReceiverHookTests`：正例 1、反例 3。
    - `GuardAdaptationTests`：exact 模式逐字不變；三種守門改寫後可編譯；反例包括已知驅動缺守門、未知 observer、守門重複、少了分支 classpath。
    - `RuntimeOverlayTests`：正例 1；反例包括把歷史 overlay 塞進 Maven、parent overlay 來自錯的 class、缺 parent overlay、overlay 多一個 class、少 hook、hook 不同；另測分支選擇。
    - `NormalizedListGuardTests`：正例、反例各 1。
    - `FrozenOverlayTests`：正例 1；反例包括凍結 overlay 不是 parent 加 hook、重產後換成歷史 class、重產後沒有 hook、observer 原始碼 SHA 不符。
  - `test_receipt_entrypoints.py` 新增：
    - targeted 與 player：一案失敗時其餘案例照跑，結束碼 2，`case-failures`。
    - 全部通過時狀態為 passed。
    - 正式檔守門拒絕時記為 `entry-aborted`，`not_run` 為 27。
    - 預設模式的收據沒有 case 欄位。
    - existing-20 用替身驅動驗證三件事：11 失敗時 19／20 被擋；11 通過但沒寫出 `replay-current.json` 時也被擋；有前置時 20 案全部執行，而且 11 先於 20。
    - affected-18：預設模式維持原本的 assert；逐案模式下 `entry-prerequisite-failed`；單一案例失敗的情形。
    - `CaseRunnerTests` 6 項。

## 未驗證／待驗證

1. **CI 結果**：本機依規定不執行測試。要看 PR 上 `combat-regression-tools` 的結果；若有失敗，依 log 修正。
2. **實際 Maven 執行**（交 Codex／本機執行器，需要 P24 實驗 DB）：
   - 對 Maven JAR `84ba0cde…` 以 `--class-identity maven-normalized --continue-per-case` 跑 full、affected、targeted、player、composed。
   - 預期 existing 13／16、affected 04／05／07／12／16、composed Fault／Clock 會通過 class 守門。之後是否通過，就由原本的行為斷言決定；候選版在這些組也有 SQL `Unknown column 'id'` 等前置失敗，兩邊必須用同一批前置條件比對。
   - existing 15 預期仍失敗，收據會列出 Main／SourceLogin 的差異方法。
3. 實跑時 observer 能否在 Maven 的 class 上順利完成插樁與反向檢查。靜態前置條件已經符合，但沒有實際跑過 JVM。

## 推測／限制

- **逐案粒度**：Java probe（共 479 支）在內部迴圈裡跑多個案例，一案斷言失敗就會讓整個 JVM 結束，而且沒有案例選擇參數。要做到「Java 內部每一案各自執行」，必須逐一修改 probe（加上案例過濾或 try／record），這等於動到凍結的 fixture，不在本張範圍。
  - 本張的逐案模式能確保：一支 probe／driver 失敗，不會讓後面的 probe／driver 沒跑到。#433 剩下的 5,070 案裡，大部分屬於「整組沒跑到」（剩餘數等於計畫數），這部分預期可以補齊。
  - 仍然拿不到逐案結果的，只剩「失敗那一支 probe 內、失敗點之後」的案例，例如 EarthBindWorldNpc 第 1 案失敗後的 31 案。
- **existing 15 的 Main 差異**：差異包含 local 變數編號、例外處理範圍的終點、`main` 多一個暫存 local、以及 finally 區塊的順序與內容。其中例外處理範圍與 finally 的內容並不保證語意相同，無法只靠正規化證明「只是表示方式不同」。看起來像 ASM 打補丁的候選版與 javac 重新編譯 `src/main` 之間的差異，但這是推測。
  - 若要讓這組可以比對，正確做法是比照 B：用當年產生 `main-overlay` 的工具（de-item-transaction application replay 裡的 Main 插樁步驟）從 Maven 的 Main 重新產生 overlay，並驗證只差 hook。這需要先找出並固定那支產生工具，建議另開任務。

## 產出路徑

- 程式：`lito伺服器/LiTo381-Server/tools/combat-continuation/` 底下的 `class_identity.py`、`regression_support.py`、`full_regression.py`、`affected_regressions.py`、`replay_composed_materials.py`、`candidate_regression.py`、`cancellation-battle-runtime/replay_targeted.py`、`cancellation-battle-runtime/replay_player_cancellation.py`、`test_class_identity.py`、`test_receipt_entrypoints.py`。
- 本報告：`docs/reports/issue-435.md`。
- 靜態證據（不進 git）：`scratch/issue-435/`。
- 分支：`task/435`。

## 給下一棒的提示

1. **Claude（審查）**：重點看兩件事。一是 A／B 是否等於「消除偷換」而不是「放寬」：hook 集合必須相同、檔案清單必須完全一致、歷史比對只記錄不放行。二是 exact 模式確實逐字不變。
2. **Codex／本機執行器（實測）**：
   - 在同一個 `--out` 下依序執行，每個入口都帶 `--jar <maven.jar> --class-identity maven-normalized --continue-per-case`：
     1. `full_regression.py`
     2. `affected_regressions.py`
     3. `cancellation-battle-runtime/replay_targeted.py`
     4. `cancellation-battle-runtime/replay_player_cancellation.py`
     5. `replay_composed_materials.py cancellation-battle-runtime`
   - 候選版用 exact 加 `--continue-per-case` 再跑一輪對照。
   - 依各收據的 `cases`／`failure_kind` 更新 #433 的剩餘清單。結束碼 2 代表案例失敗，不是工具壞了。
3. **後續任務建議**：(a) existing 15 的 Main overlay 從 Maven 重新產生（見上）。(b) 若需要 probe 內部的逐案結果，另開任務評估替 Java probe 加上案例過濾參數；這會動到凍結的 fixture，需要主控裁定。
