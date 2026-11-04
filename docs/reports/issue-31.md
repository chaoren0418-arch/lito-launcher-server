# Issue #31 — T020c 回歸入口參數化與受阻驗證

## 結論

**未達完整回歸驗收；請勿關閉 Issue 或宣稱 8d26c9d7 本輪全套通過。**

已修改八個指定入口及其直接共用工具，完成離線接線檢查與 30 個 probe 編譯。本輪沒有執行戰鬥行為案例，沒有產生新的通過收據。`seal_validation.py` 實際 exit 1，停在歷史正式 JAR 保護值不符。

工作分支 `task/31`；從既有 worktree 直接修改，未 checkout／另建分支。設計依據：`git fetch origin task/24` 後讀取 `origin/task/24:docs/design/T020-combat-merge.md` §2-4。

## 做了什麼

- `full_regression.py`、`candidate_regression.py`、`affected_regressions.py`、`replay_composed_materials.py`，以及 `cancellation-battle-runtime/{replay_targeted,replay_player_cancellation,test_curse_clock_boundary,seal_validation}.py` 均接受 `--jar`／`--out`。
- 共用工具 `regression_support.py` 依 `JAVA_HOME/bin`、PATH 順序解析 JDK；無效的 JAVA_HOME 明確失敗。直接子驅動在既有凍結／轉接階段改寫 JDK 呼叫，歷史 JSON 中的 Java 命令也轉接。沒有批量修改 1,008 支歷史腳本或任何 `build.py`。
- 未指定參數時保留各入口既有候選／收據位置。保留 candidate/composed 的 stage 參數；省略 stage 時採本任務的 `cancellation-battle-runtime`。
- `--out` 下使用 `targeted/receipt.json`、`player-cancellation/receipt.json`、`composed-replay/current.json`、`regression/{qualification,affected}-current.json`、`curse-clock-boundary/receipt.json`；seal 成功才寫 `qualification-current.json`。獨立輸出模式不更新全域 `continuation-current.json` 或 `control-gaps.json`。
- 指定 JAR 或 out 時，targeted 原先重用的 replacement／fault 兩組改為重新執行，總組數仍是 27；執行失敗會留下 failed 收據，不沿用舊 passed 指標。
- 額外 `--resources <Server 目錄>` 用於讀取 worktree 外的本機依賴，必須搭配 `--out`；`--config <隔離設定>` 讓下一棒不用引用正式設定。原始專案資源只讀，生成子驅動及 runtime 輸出指向指定 out。
- seal 對新套件核對傳入 JAR、收據 SHA、狀態與原有案例數。SQL／重建／靜態稽核／原生準備收據仍核對其歷史候選，獨立輸出時明確標為 `historical_evidence`、`rerun=false`，不將其冒充新 JAR 的執行結果。歷史正式檔保護斷言仍保留。

## 已驗證／怎麼驗證

1. 候選 JAR 實際 SHA256：`8d26c9d791e094ab3d3cd6c8dbe4218d99f96fcbe73858be5af534106c81cfb8`。
2. 八個入口 `--help` 全部成功，均列出 `--jar`／`--out`。
3. 68 個直接子驅動（27＋3＋20＋18）的轉接結果全部可解析，執行程式路徑不再硬編 JDK。逐一比較轉接前後的 `ast.Assert`，全部相同；沒有刪案或修改行為斷言。
4. 30 個 targeted／player 子驅動均以同一候選執行既有 `--compile-only` 分支成功。全部收據狀態為 `compiled-only-not-qualified`。提供給它們的設定路徑不存在且未被讀取；**編譯不算行為回歸**。
5. 七項離線測試通過：JAVA_HOME 優先、PATH 後援、無效 HOME、缺失工具、轉接保留斷言／timeout、凍結命令轉接、八入口 CLI。
6. 核對下表的舊頂層收據 SHA、子收據／manifest SHA 與案例／組數。
7. 以指定 JAR 及 `--out scratch/t020c/8d26c9d7/` 實際執行 seal，exit 1；完整錯誤存於 `seal-attempt.log`。沒有取得全 assert 通過。
8. `git diff --check` 通過。

### 套件對照（本輪沒有新行為通過收據）

| 套件 | 新收據 SHA256 | 本輪案例／組數 | 既有收據案例／組數 | 一致與否 |
|---|---|---|---|---|
| targeted | —（未產生） | 未執行 | 27 組；2104 案 | 待驗證 |
| player-cancellation | —（未產生） | 未執行 | 3 組；296 案 | 待驗證 |
| composed | —（未產生） | 未執行 | 71 組；5164 案 | 待驗證 |
| existing_20 | —（未產生） | 未執行 | 20 組 | 待驗證 |
| affected_18 | —（未產生） | 未執行 | 18 組 | 待驗證 |
| curse-clock | —（未產生） | 未執行 | 24 案 | 待驗證 |

既有 targeted 27 組＋player 3 組＝2,104＋296＝2,400 案；composed 71 組＝5,164 案。以上都是**舊收據基準**。

| 舊收據 | SHA256 |
|---|---|
| targeted | `f0c9902051c1d482d000d2fe013aa45917ae0538fd743734ecbbe1ddcd62286a` |
| player-cancellation | `6c88a681178718b19eeaa03130ca78e23a3b21b31ee8e29d8c9469aa40c4145e` |
| composed | `24222fb09585e66eb82f58143cf89d6133f72ba3157a54687bb9a3c6b5c1b8c8` |
| existing_20 | `3d4e0edaed9847b1c3e0d4c6f89ce28c194dc587fe2fb628de32cb6f8b80f252` |
| affected_18 | `329f00e77a08494b24103c6169b88b21441c56df298cecd2a249b6d48f8a35c2` |
| curse-clock | `b191689cbe55468fad6e7e2a2d3ba903c57b12b9ad257a1343ef4a3a90c9018b` |

### 受阻證據

**戰鬥 probe 也依賴 SQL，並非只有 SQL 啟動套件需要資料庫。**

`cancellation-probability/CancellationMaterialProbe.java` 的 main 呼叫 `ServerConfig.load(...)`、`ResourceDatabase.load(...)`。對指定候選執行 `javap -c -p`，`ResourceDatabase.load` 的 bytecode 有 `UnifiedConfiguration.read` 與 `java/sql/DriverManager.getConnection`。已檢查使用中的 fixture classpath，未找到覆蓋 ResourceDatabase 或 JDBC Driver 的隔離 fixture。本輪不讀正式 `config/server.properties`，也不嘗試正式 DB 連線。

**歷史正式檔保護值已落後於目前部署檔。**

seal 的 `old['protected']` 要求正式 JAR 為 `75da69b0d2f7f9d394ee6bf2fdb396b153c75d9b938069b0999ed8dac4f50371`，本機唯讀計算為 `e1498871c7be6b287821c57ef5d95933db1740d9b4d4a80f6c3465f21d99d498`。與設計文件所述正式基準一致，但不符合歷史 seal 斷言，因此保持失敗。既有 `reconcile_replay.py` 也仍固定歷史 `75da69b0…`，沒有自行改為接受目前正式檔。

## 推測

取得隔離 SQL 資源與非機密測試設定後，才有條件繼續行為回歸。30 組編譯成功不能證明其行為相容；後續仍可能暴露歷史資源、golden、外控設定或其他舊收據保護值問題。

## 待驗證／未驗證什麼

- 全套 2,400＋5,164＋20／18 組＋24 案的重新執行與新舊一致性，全部待驗證。
- 全部 seal assert 通過，待解決歷史保護基準衝突及補齊新行為收據。
- 任意不同 SHA 的 JAR 的行為通過狀態；目前只完成指定候選的編譯驗證。
- SQL 啟動與 owned cleanup 沒有重跑；因 seal 提前停於正式檔保護，本輪也沒有宣稱其後續歷史核對全部成功。
- 沒有啟動正式／測試伺服器、登入器、遊戲，沒有啟停 DB、部署候選、改正式 JAR、讀取機密設定、處理封存目錄或修改硬連結。沒有修改 src/main、各層 build.py、docs/REPORT.md。

## 產出路徑

- 可提交：上述八入口、`tools/combat-continuation/regression_support.py`、本報告。
- 本機非 git 證據：`scratch/t020c/8d26c9d7/{verification-summary.json,seal-attempt.log}`。
- 只編譯證據：`scratch/t020c/compile-only/summary.json` 與各組 result/log/classes。
- 離線檢查腳本：`scratch/t020c/{check_adapters,test_regression_support,compile_probes,verify_baseline}.py`；轉接盤點 `adapter-audit.json`；JDBC bytecode 證據 `ResourceDatabase-bytecode.txt`。

- `scratch/t020c/compile-only/summary.json` SHA256：`1f53df3b0ea0061094c66516234b7c9484586f0d2489085eb6c49c47369805eb`。
- `scratch/t020c/8d26c9d7/verification-summary.json` SHA256：`b0d67b508e8857d84d25968eab9f38f246ae1b12f2dba530cee28203af4c62a2`。
- `scratch/t020c/8d26c9d7/seal-attempt.log` SHA256：`ad075ace7d454efc8693e9a3915a364eecb7a885d192d42cb5075e795cc91cb0`。

## 給下一棒的提示

1. **Claude 雲端主控／調度器**：本任務標 blocked／needs:local，不要以編譯成功結案。安排隔離 SQL 資源與非機密測試設定；使用者不需代跑準備工作。
2. **Claude 審查**：裁定歷史正式 JAR 保護基準如何有證據地銜接到正式 `e1498871…`。不得只替換常數、刪除保護或將舊結果冒充新收據。
3. **本機執行器**：條件具備後，依下列順序跑完整套件，補上本表的新 SHA／案例數／一致性，再執行 seal。保持原斷言，失敗則保留證據。

以下命令是**下一棒待執行步驟**，不是本輪已通過結果。`$isolatedConfig` 須指向準備完成的隔離設定；不可指向正式 `config/server.properties`。

```powershell
$drivers = 'lito伺服器/LiTo381-Server/tools/combat-continuation'
$resources = 'D:/lineage開發/3-lito開發/lito伺服器/LiTo381-Server'
$candidateJar = "$resources/tools/combat-continuation/cancellation-battle-runtime/build/candidate-cancellation-battle-runtime.jar"
$isolatedConfig = 'scratch/t020c/isolated-test.properties'
$sharedArgs = @('--jar', $candidateJar, '--out', 'scratch/t020c/8d26c9d7', '--resources', $resources, '--config', $isolatedConfig)
py -3.12 -B -X utf8 "$drivers/cancellation-battle-runtime/replay_targeted.py" @sharedArgs
# 每一入口須 exit 0 才繼續下一套；candidate_regression 內依序執行 existing_20、affected_18。
py -3.12 -B -X utf8 "$drivers/cancellation-battle-runtime/replay_player_cancellation.py" @sharedArgs
py -3.12 -B -X utf8 "$drivers/replay_composed_materials.py" cancellation-battle-runtime @sharedArgs
py -3.12 -B -X utf8 "$drivers/candidate_regression.py" cancellation-battle-runtime @sharedArgs
py -3.12 -B -X utf8 "$drivers/cancellation-battle-runtime/test_curse_clock_boundary.py" @sharedArgs
py -3.12 -B -X utf8 "$drivers/cancellation-battle-runtime/seal_validation.py" @sharedArgs
```
