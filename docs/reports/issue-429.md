# Issue #429 — T147：05／08 雙邊重測與 F8 各三次

**結論：部分通過（✅ 7／❌ 1／⚠️ 3）。#428 的 D2、F8 已通過實測；05 已到達案例結果一致，08 的 F5 不一致，尚未取得完整回歸資格。**

## 已驗證

### 基線與執行範圍

- 在既有 `task/429` worktree 執行，沒有 checkout、另建分支、刪除 worktree 或遞迴刪除目錄。僅提交本報告及兩張對照表，未修改產品或正式回歸工具。
- 建置基線 `0b4d693262fb7e1002daf6197f9456873c9a580b`；`mvn -B -DskipTests package` 成功，耗時 1 分 41 秒。JDK 17.0.17.10／Maven 3.9.16，未執行 Maven 單元測試。
- 執行期間同步至 main `a753de2b`；該提交只有調度器新增的 `docs/REPORT.md` 索引，程式碼／fixture 完全未變。本張未自行編輯該索引。同步後重新 package 成功（1 分 33 秒），JAR 雜湊與受測產物完全相同，因此沿用本輪測試，不重跑已通過案例。
- 私密設定沿用 #83 隔離設定的本機交接副本，僅改輸出位置，保存於 ignored scratch；受限帳號 `lito83_exp@127.0.0.1`，MySQL `127.0.0.1:43324`，UUID `e3443e40-d090-11f1-8040-4cedfb679789`。未連正式 2000／3306，未啟動遊戲或正式伺服器。
- `p24lab` lease 持有人為 `chaoren0418-arch/lito-launcher-server#429`，到期 `2026-11-14T01:02:03Z`；啟動與收尾均確認有效。
- 下文 `E` = 本 worktree 的 `scratch/issue-429/`，`R` = `E/results/`。原專案僅提供唯讀歷史資源；`E/resources/` 含 260 個 junction，**不可沿其遞迴刪除**。

| JAR | SHA-256 |
|---|---|
| Maven `target/LiTo381-Server.jar` | `84ba0cde7abace28456232b86922b22dd7fbff98e8b44e8b7bec5f995a79edcc` |
| 候選 `reference/candidate-50635f7e.jar` | `50635f7edca8ec6ce77787cc32371c7590b497e48ea202f8a679caf3f517b729` |
| 正式 JAR，測試前後一致 | `e1498871c7be6b287821c57ef5d95933db1740d9b4d4a80f6c3465f21d99d498` |

### 逐項結果

✅ 以本張「雙邊結果一致」標準判定；兩邊同一斷言失敗不等於功能通過。表中 11 列是本報告結論的計數單位。

| 編號 | 結果 | 實際觀察 | 重現步驟 | 證據路徑 |
|---|---|---|---|---|
| 00 | ✅ | Maven 建置成功，產物 SHA 如上 | 在 Server 執行 package | `E/maven-package.log`、`maven-after-sync.log`、`evidence/jars.json` |
| 05／F6 | ✅ | 904 案完整通過；races 前 12 案通過、第 13 案同樣失敗，917 案結果一致 | A，各 JAR 一次 | `R/{candidate,maven}-targeted.log`、[逐案表](issue-429/targeted-cases.tsv) |
| 08／D2 | ✅ | components 五組均完成；Maven character／composition 的指定 class 兩處守門通過 | B，第 4 組 | `R/maven/regression/qualification-runs/*/replay-native-de-item-state-components.py.class-identity.json` |
| 08／F5 | ❌ | 候選 ActorState normal 失敗，Maven 同組通過，結果不同 | B，第 7 組 | `R/{candidate,maven}/regression/qualification-runs/*/run-native-de-item-state-regressions.py.log` |
| 08／新增 posture | ⚠️ | Maven 第 11 組 aggressive posture 斷言失敗；候選未到達，無法判定雙邊差異 | B，第 11 組 | `R/maven/regression/qualification-runs/*/replay-native-de-item-state.py.log` |
| F8-1 | ✅ | 雙邊 exit 0，1 case／40 checks／16 frames／鎖內寫入 0 | C，第 1 次 | `R/f8/{candidate,maven}-1/{probe.log,observations.json}` |
| F8-2 | ✅ | 雙邊同上，observations 全欄位一致 | C，第 2 次 | `R/f8/{candidate,maven}-2/{probe.log,observations.json}` |
| F8-3 | ✅ | 雙邊同上，observations 全欄位一致 | C，第 3 次 | `R/f8/{candidate,maven}-3/{probe.log,observations.json}` |
| 10 | ⚠️ | 未執行 seal：08 不一致，未滿足本張續跑條件 | 須 05／08 一致後才執行 | `R/comparison.json`；本張無 seal 收據 |
| 12 | ⚠️ | 未續跑完整 7,380 案；已配對 904 案完整 observations，尚缺 6,476 案 | 只盤點本輪覆蓋，不重測 F3／F7 或已通過項目 | `R/coverage.json`、`comparison.json` |
| 14 | ✅ | 自有 P24 正常 SHUTDOWN；無遺留 JVM／Python／MySQL，實驗埠皆釋放 | D | `E/evidence/shutdown.json`、`remaining-processes.json`、`db-{before,after}.txt` |

### 對照細節

- **05**：兩邊依序完成 cancellation battle tests 276、boundaries 12、CallNature 七組 488、MassTeleport tests 128，共 **904 案**；10 份 observations 全欄位相同。276 案均為 7,617 checks／3,476 frames／locked_write_calls=0。
- **F6**：兩邊皆在 `MassTeleportAnswerRace, true-false-false-69-12` 失敗：`only live or already admitted shutdown reply commits mode=12`。該 probe 依序執行 mode 0–13；兩邊已建立 mode 0–12 的案例目錄，堆疊在 mode 12 的 exercise 斷言，故前 12 案已完成。逐案表共 **916 通過＋1 失敗**，917 列全部相同；這 13 案沒有整組完成 observations，不併入 904 案完整收據數。
- **D2**：`NativeDeCachedFactoryServices` 在 character／composition 兩筆皆為 `normalized-identical-inner-class-flags`、`identical=true`、`changed=false`；歷史 flags 24、受測 flags 8、`runtime_declared_flags=8`。其餘 components 守門及行為測試亦完成。
- **F5**：候選失敗於 `NativeDeActorStateRegression.main:124`，斷言 **`Follower event queued before current-max change`**；Maven 第 7 組 exit 0。不是「兩邊都失敗」；候選這次也不是先前記錄的 child encounter 斷言。
- **08 後續**：候選僅第 1–6 組通過、第 7 組失敗；Maven 第 1–10 組通過，第 11 組 `NativeCompanionCommandRegression.main:8` 失敗：**`actual C_HACTION preserves native posture world=false kind=ARENA action=aggressive`**。候選未到該案，不能稱為「只有 Maven 失敗」。上層 reconciliation 又在資格條件拋 AssertionError，原始姿態斷言保存在內層 log。
- **08 配對 observations**：25 份全欄位相同；另有 27 份僅 Maven 產生，不能當成雙邊通過。雙邊均未進入 affected 18 組。
- **F8**：另外各執行三次獨立 JVM，篩選 `CancellationBattleReplacement, true-false-false-44-0-18`；六份 observations 全欄位相同，未見陣列越界。scratch 副本僅將 probe 類名改成 `F8RepeatProbe` 並在原迴圈加入單案篩選，保留 #428 的 clock barrier、原 exercise 與全部斷言；沒有修改產品 JAR 或正式 fixture。

完整 [65 組對照表](issue-429/group-results.tsv) 包含 05 的 27 組、08 的 20 existing＋18 affected，未到達者逐列標「無法判定」；[917 案對照表](issue-429/targeted-cases.tsv) 每列列出雙邊結果及證據。TSV 證據路徑均相對 `E`。通過／失敗一致與 observations 彙總相等，均不擴張為所有內部狀態或逐 byte wire 等價。

### 重現方式與收尾

| 代號 | 命令／方式 | 候選／Maven 耗時 |
|---|---|---|
| A | `python -B -X utf8 tools/combat-continuation/cancellation-battle-runtime/replay_targeted.py` 加下列共同參數 | 279.51／277.38 秒，皆 exit 1（F6） |
| B | `python -B -X utf8 tools/combat-continuation/candidate_regression.py cancellation-battle-runtime` 加共同參數 | 263.19／390.69 秒，皆 exit 1（不同失敗點） |
| C | `python -B -X utf8 scratch/issue-429/run_f8.py`；完整 javac／java argv 保存於 `R/f8/results.json` | 各三次均 exit 0 |
| D | `ReadOnlyProbe` 執行 SELECT／SHOW GRANTS 後，`E/stop-p24.ps1` 核對 PID、開始時間、執行檔、UUID 與其他連線，再正常 SHUTDOWN | `2026-11-14T00:27:35Z` 完成 |

共同參數：`--jar <本表指定 JAR> --out <E/results/candidate 或 maven> --resources <E/resources/lito伺服器/LiTo381-Server> --config <E/inputs/isolated-test.properties>`；Maven 另加 `--class-identity maven-normalized`，候選保持預設 exact。`E/run_suites.py`、`run_f8.py`、`compare.py`、`coverage.py` 保存本輪命令與盤點方式。重新執行須使用新的輸出目錄與當時有效 lease。

P24 parent PID 17468、MySQL listener PID 19612 均屬本張，已結束；42061／43324／42083／42084 無 listener。所有測試各自少於 10 分鐘，無逾時強殺。DB 前後的身分、授權及兩張結算表列數查詢檔雜湊相同；本張未做 DDL／DML 或修改 schema，因此沒有資料變更與還原 SQL。這是列數及權限核對，不宣稱全庫逐列 checksum。四次 05／08 的 formal protection 均 passed，正式 JAR 未變。

## 推測

F5 同一候選這次首先失敗的斷言與先前不同，可能受排程／事件觀測時序影響；單次候選失敗、Maven 通過不足以證明產品語意差異。posture 失敗的根因與候選表現尚未驗證。本張沒有自行修程式或放寬斷言。

## 待驗證／給下一棒的提示

1. **Claude／fixture 維護者**：先處理 08-F5 的雙邊不一致，保留候選的 Follower 斷言與 Maven 通過證據；勿重開已通過的 D2、F8、05。針對第 11 組 posture 補候選單案對照，再判定 fixture 或產品問題。
2. **主控／本機重測**：08 尚缺後續 existing／affected 的雙邊覆蓋；一致後才做 10／12。本張未重測排除範圍 F3、F7。
3. **12 剩餘**：7,380 = targeted 2,104＋player probability-lifecycle 112＋composed 5,164；本輪完整配對 targeted 904，剩 **targeted 1,200＋player 112＋composed 5,164＝6,476**。F6 部分迴圈的 13 案有結果對照，但未有完整收據，故保留在剩餘數。前輪已通過的 player 184＋curse-clock 24 沿用，不重跑。
4. **調度器**：保全 `E/results/`、`E/evidence/` 與 scratch 腳本後再管理 worktree；私密 `E/inputs/` 不公開。本報告未授予部署或正式驗收資格。

## 產出路徑

- `docs/reports/issue-429.md`
- `docs/reports/issue-429/group-results.tsv`
- `docs/reports/issue-429/targeted-cases.tsv`
