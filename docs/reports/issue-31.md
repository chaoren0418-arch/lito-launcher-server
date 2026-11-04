# Issue #31 — T020c 回歸入口參數化與本機驗證

## 結論

**第 3 次嘗試仍未達完整回歸驗收；不得關閉 Issue，也不得宣稱 8d26c9d7 已於本輪全套通過。**

本輪從 `task/31` 的 `d62625d` 接續，未 checkout／建立分支。既有參數化與快取修正保留；本輪只更新本報告與不進 git 的離線證據，沒有修改回歸程式、刪案或放寬斷言。新增行為通過收據為零。

已讀根目錄 AGENTS.md、docs/REPORT.md、TASKS.md、伺服器 AGENTS.md 及其指定維護文件；以 `git fetch origin task/24`、`git show origin/task/24:docs/design/T020-combat-merge.md` 讀取設計。未修改 docs/REPORT.md。

## 做了什麼

前輪提交 `94decc9`／`8b0338e`／`d62625d` 已完成八入口的 `--jar`／`--out`、JAVA_HOME／PATH 選擇、共用轉接器、seal 失敗收據及指定 out 時一般快取停用。直接呼叫的歷史子驅動在 out 內產生路徑轉接版本，不改原專案資源；各層 build.py 與 src/main 未變更。

本輪重新核對套件工具、候選 SHA、歷史案例數與 seal 失敗，並**另外獨立核對歷史 SQL 啟動／清理收據**，避免 seal 提前失敗使這項證據一直未被檢查。SQL 只核對既有收據，不執行 SQL 或重跑啟動。

## 已驗證／怎麼驗證

- `check_adapters.py`：8 入口 CLI 均有 --jar／--out；68 子驅動轉接後可解析，未殘留指定硬編 JDK 路徑。這是工具檢查，不是 68 組戰鬥通過。
- `test_regression_support.py` 7 項、`test_seal_failure.py` 2 項、`test_replay_policy.py` 2 項：共 11 項通過，涵蓋 JDK 選擇、參數轉接、既有斷言保留、失敗 seal 不沿用成功標記、一般快取政策。
- 子程序移除 JAVA_HOME，將已安裝 JDK 的 bin 加入該子程序 PATH，透過 `java_tool` 實跑 java／javac `-version`，均為 17.0.17。未改系統環境。
- 同一候選實際 SHA256：`8d26c9d791e094ab3d3cd6c8dbe4218d99f96fcbe73858be5af534106c81cfb8`。
- `verify_baseline.py` 核對 targeted／player 子收據與 composed／20／18 manifest 的 SHA，重新取得下表案例數；以同一 --jar、`--out scratch/t020c/8d26c9d7/`、原專案 --resources 實跑 seal，**exit 1**。
- 獨立核對歷史 SQL 收據 SHA、accepted=true、cleanup_verified=true、instances 指向歷史候選，全部通過；reproduction JAR、material census（剩 3 個 legacy calls）及 inheritance audit 亦通過原條件。僅代表歷史證據，rerun=false。
- 前輪 30 子驅動的 compile-only 結果仍只屬前輪編譯證據，本輪未重跑，也不將其計入行為案例。

### 套件對照表

| 套件 | 新通過收據 SHA256 | 本輪案例數／組數 | 既有收據案例數／組數 | 一致與否 |
|---|---|---|---|---|
| targeted | 無，未產生 | 未執行 | 27 組／2,104 案 | 待驗證 |
| player-cancellation | 無，未產生 | 未執行 | 3 組／296 案 | 待驗證 |
| composed | 無，未產生 | 未執行 | 71 組／5,164 案 | 待驗證 |
| existing_20 | 無，未產生 | 未執行 | 20 組 | 待驗證 |
| affected_18 | 無，未產生 | 未執行 | 18 組 | 待驗證 |
| curse-clock | 無，未產生 | 未執行 | 24 案 | 待驗證 |

歷史 targeted＋player 為 2,104＋296＝2,400 案。上述數字均為既有收據基準，不是本輪通過結果。

| 既有收據 | SHA256 |
|---|---|
| targeted | `f0c9902051c1d482d000d2fe013aa45917ae0538fd743734ecbbe1ddcd62286a` |
| player-cancellation | `6c88a681178718b19eeaa03130ca78e23a3b21b31ee8e29d8c9469aa40c4145e` |
| composed | `24222fb09585e66eb82f58143cf89d6133f72ba3157a54687bb9a3c6b5c1b8c8` |
| existing_20 | `3d4e0edaed9847b1c3e0d4c6f89ce28c194dc587fe2fb628de32cb6f8b80f252` |
| affected_18 | `329f00e77a08494b24103c6169b88b21441c56df298cecd2a249b6d48f8a35c2` |
| curse-clock | `b191689cbe55468fad6e7e2a2d3ba903c57b12b9ad257a1343ef4a3a90c9018b` |

### 阻礙與證據

1. **行為 probe 本身依賴 SQL。** `cancellation-probability/CancellationMaterialProbe.java` 的 main 呼叫 `ServerConfig.load`、`ResourceDatabase.load`；既有候選的 `javap -c -p` 顯示後者讀 UnifiedConfiguration 並呼叫 DriverManager.getConnection，查詢 lito_resource 表。現有 fixture 未提供可直接取代這項依賴且保持真實資料等價的離線來源。這不是只有 SQL 啟動／清理套件的依賴。
2. **未提供可使用的隔離設定。** 只檢查本 worktree 與原專案的 `scratch/t020c/isolated-test.properties` 是否存在，兩者均不存在；沒有搜尋或讀取正式機密設定。依本任務禁止連正式 DB 的要求，未啟動依賴這項設定的 Java 行為 probe。
3. **seal 在第 25 行歷史保護斷言失敗。** `old['protected']` 中正式 JAR 預期 `75da69b0d2f7f9d394ee6bf2fdb396b153c75d9b938069b0999ed8dac4f50371`，目前正式 JAR 唯讀雜湊為 `e1498871c7be6b287821c57ef5d95933db1740d9b4d4a80f6c3465f21d99d498`，與設計 §1 現行正式基準一致。沒有改常數、跳過此斷言或修改正式檔。`reconcile_replay.py` 也仍要求歷史 75da69b0，需一併處理銜接。

本輪 shell 的預設 sandbox 初始失敗（setup refresh had errors）；經工具核准以 require_escalated 執行後恢復，並非本次驗收阻礙，也未發生自動審查拒絕。

## 推測

具備獲准的隔離 SQL 測試資源與設定後，現有入口可能可以重跑；但仍可能遇到歷史資源、golden、時序或正式檔保護差異。目前不能推定完整套件會通過，也不能以複製舊收據、編譯成功或 stub 代替驗證。

## 待驗證／未驗證什麼

- targeted 27 組＋player 3 組的 2,400 案、composed 71 組的 5,164 案、20＋18 組、curse-clock 24 案，全部仍需實跑並比對新收據。
- 全部 seal assert 通過；本輪僅取得 failed 收據。獨立歷史 SQL 核對通過不等於 seal 通過。
- 任意其他 SHA JAR 的行為結果、實機 UI／登入／戰鬥驗收均未驗證。
- 未讀 config/server.properties、未連 DB、未啟停 DB／正式或測試伺服器／遊戲、未改正式 JAR、未部署、未複製客戶端資源、未讀寫封存或修改硬連結。

## 產出路徑

- 追蹤報告：`docs/reports/issue-31.md`。
- 工具原始碼：`lito伺服器/LiTo381-Server/tools/combat-continuation/` 的既有八入口與 regression_support.py，本輪未修改。
- 本機離線證據（不進 git）：下列 scratch 檔案；不是戰鬥通過收據。

| 證據路徑 | SHA256 |
|---|---|
| `scratch/t020c/8d26c9d7/verification-summary.json` | `36e2381a375bc9c2fa5d26e97fac4095eacc422371eec7a34b998a1f32c0c2cf` |
| `scratch/t020c/8d26c9d7/offline-review.json` | `0b9efd5d7c9359fec192365cd249b39abebe90255f60f566f8e55580ad4ae60a` |
| `scratch/t020c/8d26c9d7/seal-attempt.log` | `5da82ac119e8cfb845886d1a06c8d8e0257a1f5b43cba914efa89338a72d5516` |
| `scratch/t020c/8d26c9d7/qualification-current.json` | `26732ee8d4c519a7521cdfa0377381d7d752438d4495f003a6a2cb208c0c9b34` |

其他工具檢查與 bytecode 證據：`scratch/t020c/{check_adapters,test_regression_support,test_seal_failure,test_replay_policy,verify_baseline}.py`、`adapter-audit.json`、`jdk-path-check.log`、`ResourceDatabase-bytecode.txt`。qualification-current.json 是 failed，不可當作資格通過。

## 給下一棒的提示

1. **Claude 雲端主控／調度器**：維持未完成。先處理行為 probe 的 SQL 依賴與本任務禁止 DB 操作的邊界，提供可使用的隔離資源及設定，或另派建立經核對真實資料的離線 fixture；不可用空資料／stub 宣稱等價。這是主控與本機資源工作，不交給使用者準備。
2. **Claude 審查／主控**：訂出有雜湊證據的歷史正式檔保護銜接方式，涵蓋 seal 與 reconcile_replay。設計已確認 e1498871 為現行正式版；需要明確保留舊證據與新驗證的界線，不直接刪除歷史斷言。
3. **本機執行器**：前兩項就緒才重派完整回歸，使用下列相同 JAR／out 依序執行，逐套 exit 0 才繼續，將各新收據 SHA／案例數填回上表；最後 seal 必須全部通過。不要再把同樣的離線工具複驗回報成進展已解除。

下列是接手後的執行步驟，**不是本輪已成功執行清單**。isolatedConfig 必須是主控提供的隔離設定，不是正式 config/server.properties。

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
