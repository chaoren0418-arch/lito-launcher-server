# Issue #421 — T145：Maven／50635f7e 指定項目重測

**結論：部分通過（✅ 8／❌ 4／⚠️ 2）。T083／M2／B09 尚未驗收。**

2026-11-14（Asia/Taipei），在既有 `task/421` worktree 執行。兩顆 JAR 各呼叫六套入口；失敗後繼續下一入口，沒有重跑、改工具、改斷言或補 DB 資料。P24Lab 於 **07:24:46 正常關閉**，無自有程序殘留。唯一提交產出為本報告；未修改 `docs/REPORT.md`。

## 已驗證：版本與環境

- 已讀根目錄／伺服器 `AGENTS.md`、維護規範、`docs/REPORT.md`、T083、#341 規格與指定報告、T142／T144 交接。03／04 判定沿用 #341 第二輪，僅補本輪必需查核。
- `HEAD` 與 `origin/main`：`44a103e00cec2990bbf7d9f59f467d9bc2d8cff6`；開始及測試中再次 fetch 均無落後，沒有 checkout／另建分支。
- `mvn -o -B -DskipTests package`：exit 0、BUILD SUCCESS、1 分 59 秒；`-o` 使用既有本機依賴，未執行 Maven 單元測試。JDK 17.0.17.10、Maven 3.9.16。

| 輸入 | SHA-256 |
|---|---|
| 新建 Maven `target/LiTo381-Server.jar` | `84ba0cde7abace28456232b86922b22dd7fbff98e8b44e8b7bec5f995a79edcc` |
| 候選 `reference/candidate-50635f7e.jar` | `50635f7edca8ec6ce77787cc32371c7590b497e48ea202f8a679caf3f517b729` |
| 正式 JAR，前後一致 | `e1498871c7be6b287821c57ef5d95933db1740d9b4d4a80f6c3465f21d99d498` |

T144 未改 `src/main`，本輪新建 Maven SHA 與 #341 第二輪相同，並非沿用其 JAR。下文 `E`＝本 worktree 的 `scratch/issue-421/`，`R`＝`E/results/`，`{candidate,maven}` 表示兩份新輸出。

原始專案的 15 個相關 tracked 檔案較舊。依本張「原始目錄只讀」要求，沒有照 T144 建議在原始 checkout 執行 pull；改在 `E/resources/` 建立資源檢視：目前 worktree 的 tracked 工具／probe 普通複製且逐檔驗 SHA，未入 git 的 fixture 目錄用唯讀用途的 junction 引用。沒有硬連結、沒有重建凍結 fixture、沒有變更工具內容。正式保護路徑經 junction resolve 後仍是原始部署 JAR。清單：`E/evidence/resource-overlay.json`；重現準備：`E/prepare.py`。

隔離設定從 #341 已驗證的實驗副本複製，僅將 capture／log 路徑改到本輪；`bind=127.0.0.1`、`game.bind=127.0.0.2`，帶 sibling `combat.properties`。私密設定留在 ignored scratch，不提交。實際 JDBC 身分 `lito83_exp@127.0.0.1`、43324、UUID `e3443e40-d090-11f1-8040-4cedfb679789`，只讀實驗 schema。未連正式 2000／3306，未修改正式設定、BIN、封存或玩家表。

有效 lease holder=`chaoren0418-arch/lito-launcher-server#421`，`exp=1794616479.4953482`（2026-11-14 00:34:39.495348Z），與派工相符；初次 API 暫無 lease，啟動前已取得相符有效收據，未依空值判成失效。證據：`E/evidence/lease.json`、`lease-final.json`。

## 01–14 結果

每個入口外層上限 590 秒，實際皆未 timeout。A–F 命令見後節。

| 編號 | 類型 | 結果 | 實際觀察 | 重現步驟 | 新證據路徑／沿用依據 |
|---|---|---|---|---|---|
| 01 | 回歸 | ✅ | 最新 main package 成功 | Server 執行 Maven | `E/maven-package.log` |
| 02 | 回歸 | ✅ | 新 Maven 與候選 SHA 如上 | SHA-256 查核 | `E/summary.json`、`R/*-execution.json` |
| 03 | 沿用＋補測後筆數 | ✅ | 沿用設定判定；本輪兩表測前／測後均 2／0 | Java 唯讀查詢，完整陣列引數 | #341 第二輪；`E/evidence/db-before.log`、`db-after.log` |
| 04 | 沿用 | ✅ | 沿用鎖判定；本輪啟動前 lease 相符有效 | 讀 `/api/state` leases | #341 第二輪；`E/evidence/lease.json`、`lease-final.json` |
| 05 | 重測 | ❌ | 候選越過 F1 後遇新 F6；Maven 在 276 案內遇新 F8，未到 targeted D1 | A | `R/{candidate,maven}-targeted.log`、各 `targeted/receipt.json` |
| 06 | 重測 | ❌ | 雙邊 176 案機率測試及 8 案 fault 通過，後遇同一新 F7 | B | `R/{candidate,maven}-player.log`、各 `player-cancellation/receipt.json` |
| 07 | 重測 | ❌ | 雙邊仍是 F3，WaterLife mode 1 資源斷言 | C | `R/{candidate,maven}-composed.log`、各 `composed-replay/current.json` 指向的新 `result.json` |
| 08 | 重測 | ❌ | Maven 第 4 組 character class guard 失敗；候選第 7 組仍 F5；affected 18 均未進入 | D | `R/{candidate,maven}-existing-affected.log`、各 `regression/qualification-runs/*/manifest.json` |
| 09 | 重測 | ✅ | 雙邊 24 案、987 checks、locked_writes=0；原 F4 已越過 | E | `R/{candidate,maven}-curse-clock.log`、各 `curse-clock-boundary/probe.log`、`receipt.json` |
| 10 | 重測 | ⚠️ | seal 正確拒絕 targeted failed，未取得資格 | F | `R/{candidate,maven}-seal.log`、各 `qualification-current.json` |
| 11 | 回歸 | ✅ | 失敗收據皆 failed，無 TypeError；reconciliation 指明真正失敗 driver／log | 核對 A／B／C／D／F 失敗收尾 | `E/summary.json`、各失敗 receipt／manifest、`*-existing-affected.log` |
| 12 | 重測 | ⚠️ | 7,588 案僅 208 案有雙邊完整 observations，逐案全覆蓋未達成 | 比對本輪 JSON、機率 log／goldens、clock 序列 | `R/comparison.json`；生成腳本 `E/compare_results.py`、`enrich_comparison.py` |
| 13 | 回歸 | ✅ | 12 份 formal_protection 全 passed，前後 SHA 均 e1498871…，changed=false | 查核所有入口收據 | `E/summary.json`、`R/*-execution.json` |
| 14 | 回歸 | ✅ | 自有 P24／JVM 已退出，四個實驗埠皆無 listener | 身分／PID 查核後正常 SHUTDOWN，再查程序及埠 | `E/evidence/p24-processes.json`、`shutdown.json`、`remaining-processes.json` |

## 失敗點與 B09 對應

F1／F2／F4 指 #341 原首案；F6–F8 是本輪新增識別。對應方法是供後續定位的入口，沒有聲稱已證明例外 handler／迴圈的逐方法覆蓋，也沒有套用 #138 偏離來放行。

| 識別／案例 | 本輪觀察 | B09／#138 關係與證據 |
|---|---|---|
| D1：GameStateStore overlay | Maven 在 **player boundaries** 實測通過；targeted 尚未走到這一步 | `R/maven/player-cancellation/cancellation-probability/boundaries/class-identity.json`：`class_identity_mode=maven-normalized`、`runtime-class-plus-probe-hooks-only`，兩個 hook、唯一替換 GameStateStore；來源是受測 Maven JAR，非候選 overlay |
| D2：components character | Maven corpse／resurrection／vital 已通過；接著 `NativeDeCachedFactoryServices.class` 的 `InnerClasses` 仍不同，被原 allowlist 拒絕 | `R/maven/regression/qualification-runs/20261113T232124816523Z/replay-native-de-item-state-components.py.class-identity.json`；`NativeDeCorpseClock$1` 已 normalized-identical。與 T142 預告相符；不是 B09 ClientSession 方法，未核准為 #138 偏離 |
| F1：CallNature 165 | 候選七組 tests／boundaries／lifecycle／world-npc／companions／battle-npc／battle-lifecycle 全通過，共 488 案；Maven 被 F8 擋在前面 | `nativeMaterialCallNatureBody`；`R/candidate/targeted/call-nature-material/*/result.json` |
| F2：Cancellation 44 原機率首案 | 雙邊皆越過，176 案 probability tests 完成 | `nativeMaterialCancellationBody`／`nativeMaterialWorldCancellationBody`；各 `player-cancellation/cancellation-probability/tests/after/` |
| F3：WaterLife，false-false-false-170-1 | 雙邊仍 `Source resources and no HP change`；mode 0 已過 | `nativeMaterialWaterLifeBody`；各 `composed-replay/*/WaterLifeMaterial/probe.log`；符合 T144「凍結 probe 尚未修正」交接 |
| F4：Curse 33 | 雙邊 24 案全通過 | `nativeMaterialParalysisScopeBody`；各 `curse-clock-boundary/receipt.json` |
| F5：ActorState normal | 候選仍 `child encounter value event is queued before HP`；Maven 未到第 7 組 | `R/candidate/regression/qualification-runs/20261113T231445351701Z/run-native-de-item-state-regressions.py.log`；fixture 堆疊未定位單一 B09 方法，不套用 #138 偏離 |
| F6：MassTeleportAnswerRace，true-false-false-69-12 | 候選：`only live or already admitted shutdown reply commits mode=12`；Maven 未到。mode 12 是 shutdown 後原生 C_ANSWER 回覆 | `nativeMassTeleportAnswerBody`；`R/candidate/targeted/mass-teleport-lifetime/races/test.log` |
| F7：CancellationLifecycle，false-false-false-44-3 | 雙邊：`caster still receives committed MP after target retirement`，與原 F2 不同 | `nativeMaterialCancellationBody`；各 `player-cancellation/cancellation-probability/boundaries/test.log` |
| F8：CancellationBattleReplacement，true-false-false-44-0-18 | Maven：`NativeLightCastProbe.collect` 拋 `ArrayIndexOutOfBoundsException: Index 139 out of bounds for length 139`；候選完整 276 案通過 | `R/maven/targeted/cancellation-battle-runtime/tests/test.log`、`result.json`；mode 18 涉實際定時到期。失敗堆疊在 fixture 收集封包，沒有 production frame，**尚不能對應單一 B09 方法或判為 Maven 語意退化**，也不能套用 #138 偏離 |

## 新對照表與限制

`R/comparison.json` 保存 38 份 observations 配對：17 份所有彙總欄位相同、2 份不同、19 份單邊。其中既有 20 組回歸的 **16 份／39,812 案** observations 雙邊相同（前 3 組及 components 的 corpse／resurrection／vital），不併入下列 7,588 案。

| 集合 | 要求 | 候選完整 observations | Maven 完整 observations | 雙邊可配對 | 缺雙邊完整 observations |
|---|---:|---:|---:|---:|---:|
| targeted＋player | 2,400 | 1,088 | 184 | 184 | 2,216 |
| composed | 5,164 | 0 | 0 | 0 | 5,164 |
| curse-clock | 24 | 24 | 24 | 24 | 0 |
| 合計 | 7,588 | 1,112 | 208 | 208 | **7,380** |

「可配對」不等於所有欄位相同，更不等於逐案完整狀態等價。中斷前的局部進度不灌入完成數，也沒有借用 #341 的 276 案收據。

- **player 176 案**：依原 probe 固定迴圈次序，將 112 個實際 probability 訊息與 32 行 golden 配回案例 ID；數量、案例數與原斷言互核。`player_probability_cases.cases` 列出每案兩邊值與不同欄位，88 案記錄欄位相同、88 案不同；其中 33 案 success 不同、32 行 golden 有 18 行不同。每一列均標對應 `nativeMaterialCancellationBody` 或 `nativeMaterialWorldCancellationBody`。彙總 candidate／Maven：checks=2960／2958、frames=1090／1086、probability_failures=53／54。兩邊斷言均通過；原始碼使用 `ThreadLocalRandom`，此次未固定同一亂數序列，不能把實際擲骰不同直接判為形狀差異回歸，也沒有消除這些差異。
- **curse-clock 24 案**：`curse_clock_cases` 每案列值。23 案 `remaining_millis` 相同；`false-true-false-33-1` 為候選 0／Maven -1 ms，對應 `nativeMaterialParalysisScopeBody`。兩邊原始計時邊界斷言通過，保留 1 ms 實測差異。
- **player fault 8 案**：彙總 observations 全欄位相同；工具沒有輸出逐案完整 trace，未擴張成 8 案所有內部狀態等價。
- F3／F5／F6／F7／F8／D2 也逐項列在 `unresolved_cases`；F5／F8 缺可證實的單一 B09 方法歸因，明列未完成，不以 #138 偏離掩蓋。

## 實際命令與證據

完整 argv、UTC、PID、exit、耗時與每份 log SHA 在 `R/{candidate,maven}-execution.json`。兩顆依序執行，相同隔離輸入，只有 Maven 加 `--class-identity maven-normalized`；候選維持 exact。等價命令：

```powershell
# 在 Server 目錄；JAVA_HOME 指向 Microsoft JDK 17，暫存指向 E/tmp
mvn -o -B -DskipTests package
# 根目錄；準備腳本只需執行一次，資源檢視不可覆寫重建
python -B -X utf8 scratch/issue-421/prepare.py
powershell -NoProfile -ExecutionPolicy Bypass -File scratch/issue-421/start-p24.ps1
python -B -X utf8 scratch/issue-421/run_suites.py candidate
python -B -X utf8 scratch/issue-421/run_suites.py maven
# 測後唯讀筆數查詢完成後：
powershell -NoProfile -ExecutionPolicy Bypass -File scratch/issue-421/stop-p24.ps1
```

| 命令 | `tools/combat-continuation/` 相對入口 | 候選 exit／秒 | Maven exit／秒 |
|---|---|---:|---:|
| A | `cancellation-battle-runtime/replay_targeted.py` | 1／275.33 | 1／48.80 |
| B | `cancellation-battle-runtime/replay_player_cancellation.py` | 1／56.12 | 1／54.89 |
| C | `replay_composed_materials.py cancellation-battle-runtime` | 1／4.84 | 1／4.45 |
| D | `candidate_regression.py cancellation-battle-runtime` | 1／253.55 | 1／135.44 |
| E | `cancellation-battle-runtime/test_curse_clock_boundary.py` | 0／26.81 | 0／26.66 |
| F | `cancellation-battle-runtime/seal_validation.py` | 1／0.91 | 1／0.41 |

每個入口共用 `--jar <該 JAR> --out E/results/<label> --resources E/resources/lito伺服器/LiTo381-Server --config E/inputs/isolated-test.properties`。重測須另開全新輸出目錄。Maven D1／D2 收據含 `class_identity_mode`，候選 manifest 為 exact；未用候選 class overlay 冒充 Maven。

測前／測後 Java 都以 `$javaArgs=@('-Dfile.encoding=UTF-8','-cp',"$run/target;$run/resources/lito伺服器/LiTo381-Server/lib/*",'ReadOnlyProbe',"$run/inputs/isolated-test.properties")` 陣列呼叫。`E/ReadOnlyProbe.java` 僅核對端點／帳號後設 readonly、查身分、grants 與 COUNT。`p24_accounts_lito_exp.character_lito` **2→2**、`p24_lito_lito_exp.lito_battle_settlement` **0→0**；不宣稱所有欄位逐值相同。帳號既有 INSERT／UPDATE 權限未使用；無 DML／DDL／GRANT、無資料修改與還原 SQL。

P24 自有監督 PID **20180**、listener 子 PID **17888**，父子關係、啟動時間與 executable 見 `p24-processes.json`。Start-Process 初始 Path 為空，保留原收據並用 CIM 核對同 PID／啟動時間後補 `p24-owned-verified.json`，未靠名稱殺程序。停止前核實 43324 UUID／datadir 與無其他 client，正常 SHUTDOWN exit 0；兩 PID 均退出。`shutdown.json` 的 listeners 為空，`remaining-processes.json=[]`；沒有啟停非本輪程序。

## 推測／待驗證與給下一棒的提示

1. **Claude／code 維護者**：F3 凍結 probe、F5 依 T144 後續處理；D2 character 的 `NativeDeInitializerKernel.Name` flags 依 T142 裁定修正，不放寬守門。F6、F7 是新定位點，尚未證明根因。
2. **fixture／戰鬥維護者**：優先定位 F8 的 `collect` 與 mode 18 定時到期輸出是否競態；這只是待驗證假說，本輪沒有重跑或改 collector。候選通過／Maven 單次失敗不足以判定是穩定語意差異。
3. **本機下一次重測**：只針對未過的 05–08、10、12；09 已雙邊通過。依新修正是否影響 JAR／包裝層另做必要回歸。仍缺 7,380 案雙邊完整 observations、affected 18 與 B09 113 個例外／迴圈方法的覆蓋證明。
4. **調度器**：保全 `E/results/`、`E/evidence/`、腳本與本輪 log 後再管理 worktree；私密 `inputs` 不上傳公開 Issue。`E/resources` 含 260 個 junction，勿沿連結遞迴處理。本輪未刪任何 worktree／junction／封存，未修改產品、fixture 或回歸工具。
