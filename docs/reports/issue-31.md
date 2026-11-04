# Issue #31 — T020c 回歸入口參數化與本機驗證

## 結論

**2026-11-05，第 3 次嘗試：blocked／未達驗收。隔離 SQL 設定仍未就緒，完整回歸與成功 seal 均未執行；不得標為 done。**

本次從既有 `task/31` 的 `82b3328` 接續，未 checkout／建立分支。前輪已完成正式檔保護銜接及 25 項離線測試，本次保留全部程式與斷言，只更新本報告。新增行為通過收據為零。

## 本次已驗證與未完成事項

- 已讀根目錄 AGENTS.md、docs/REPORT.md、TASKS.md 的 T020、既有本報告及 Issue 最新留言。工作樹起始乾淨。Issue 引用的 `docs/design/T020-combat-merge.md` 在此分支不存在，本次依 Issue 明列的驗收條件查核。
- `Test-Path -LiteralPath ... -PathType Leaf` 確認下列兩個位置均不存在：
  - `D:/lineage開發/3-lito開發/scratch/wt/task-31/scratch/t020c/isolated-test.properties`
  - `D:/lineage開發/3-lito開發/scratch/t020c/isolated-test.properties`
- 已確認指定隔離設定路徑受 git ignore 保護。未建立空白、假密碼或未驗權限的設定以冒充就緒。
- Issue 留言直到本次派工（[6636450341](https://github.com/chaoren0418-arch/lito-launcher-server/issues/31#issuecomment-6636450341)）仍沒有「隔離設定已就緒」及帳號名稱的交接紀錄；[審查退回](https://github.com/chaoren0418-arch/lito-launcher-server/issues/31#issuecomment-6636219980)指出的前置缺口仍存在。本次讀取最新留言，未變更工作流標籤；排入執行不代表前置完成。
- 唯讀重算候選 JAR SHA256 為 `8d26c9d791e094ab3d3cd6c8dbe4218d99f96fcbe73858be5af534106c81cfb8`；worktree reference 與原專案部署檔均為 `e1498871c7be6b287821c57ef5d95933db1740d9b4d4a80f6c3465f21d99d498`。這只是前置檔案雜湊查核，**不是行為回歸前後的 formal_protection 證據**。
- 靜態核對 regression_support.py、seal_validation.py 與 reconcile_replay.py，前輪正式檔保護銜接仍在。本次未修改程式，未重跑已由前輪及審查確認的離線測試；下節 25 項測試及 scratch 稽核檔均歸屬前輪。
- 未執行 targeted、player、composed、20＋18 組、curse-clock 或 seal，沒有新通過收據與案例數。未讀取機密設定、未連 DB、未建立帳號或授權、未啟停任何 DB／伺服器／遊戲、未修改正式 JAR、未部署候選。

### 第 3 次嘗試的增量查核

- 本機 shell 與替代讀檔工具一開始均遇到 `setup refresh had errors`；改用工具核准的 `require_escalated` 完成唯讀查核，未遭自動審查拒絕。沙箱錯誤已繞過，實際阻礙仍是隔離設定與帳號交接缺漏。
- 本次重新確認兩處指定設定皆不存在、隔離設定受 git ignore 保護；重新計算三份 JAR 的雜湊，均符合上列候選／現行正式基準。靜態核對共用保護器與 14 項正式保護測試仍在，未變更程式或重跑既有測試。
- 最新 Issue 留言截至派工 `6636450341`，沒有新增隔離設定就緒紀錄。不能以重新派工、前輪離線測試或歷史收據取代前置與實跑驗收；本次行為案例數仍為 0，沒有新成功收據。
- 本次僅更新本報告；`docs/REPORT.md`、正式設定、歷史收據及程式均未修改。完整回歸指令繼續保留於下節，須待 Claude 本機對話完成既定 `needs:local` 交接後執行。

### 本機前置交接依據

公開程式 `src/main/java/local/lito381/server/UnifiedConfiguration.java` 的 `KEYS`／`validate` 列出完整必要設定鍵，包含 `database.jdbc.base.url`、`database.jdbc.options`、`database.user`、`database.password`、`database.static.database`、`database.lito.database`；不能只填帳密就認定設定有效，也不能以連線的 `setReadOnly(true)` 代替資料庫帳號權限限制。

靜態來源顯示 `UnifiedConfiguration.sources` 會查詢 `lito_source_server`；既有 `scratch/t020c/ResourceDatabase-bytecode.txt` 顯示資源讀取含 `lito_resource_release`、`lito_resource_pending`、`lito_resource_skill_value` 等表，並會繼續呼叫 `CombatDatabase.load`／`NativeDeSprites.load`。這只是協助本機對話定位的讀取範圍線索，**不是完整授權表清單或候選 JAR 已通過設定驗證的證明**。本機前置仍須依指定候選與實際 probe 完整核對，提供僅 SELECT 的專用帳號與權限證據；本次沒有可用的隔離設定或專用帳號交接，無法安全自行補齊。

## 前輪已完成的程式修改

- regression_support.py 新增共用正式檔保護：僅 --out 模式核對現行 reference／部署檔皆為 `e1498871c7be6b287821c57ef5d95933db1740d9b4d4a80f6c3465f21d99d498`，實跑前後量測部署檔 SHA，相異即失敗。收據加入 formal_protection，含 before／after／reference／時間／狀態，以及 `historical_formal_at_candidate_qualification = 75da69b0d2f7f9d394ee6bf2fdb396b153c75d9b938069b0999ed8dac4f50371`。
- targeted、player、composed、existing_20、affected_18、curse-clock 的行為執行區塊均套用保護；失敗仍記錄可取得的雜湊證據，部署改變不會保留 passed 狀態。修正 curse-clock 在保護先行失敗、尚未建立 log 時的例外處理。
- seal 只對正式 JAR 使用主控核准的新基準；其他 old['protected'] 仍逐項原樣核對，舊模式原斷言保留。新模式另外核對每套件的前後保護收據，不能僅以 seal 當下的兩次量測代替回歸期間量測。只更新新輸出的 protected 參照，不修改歷史收據。
- reconcile_replay.py 新增 --out／--jar／--resources／--config／--manifest 接線，保留 48 組、1,728 案與所有結果雜湊的原斷言。新模式核對實際失敗回歸 manifest 的 formal_protection，另外記錄 reconciliation 期間前後雜湊；無 --out 時仍要求舊的 75da69b0…。
- candidate 新模式直接呼叫 reconciliation 並傳遞相同參數；每次開始先失效化上次 reconciliation，僅本次已完成的行為結果可進入既有 reconciliation 重試流程。
- 新增可提交的 `tools/combat-continuation/test_formal_protection.py`，不執行 Java、SQL 或戰鬥。未改各層 build.py、src/main 或 docs/REPORT.md。

--resources 指向原專案的未入 git 資源與部署位置；reference 使用目前 worktree 的 `lito伺服器/LiTo381-Server/reference/base-0.6.71.jar`。原專案相同 reference 路徑不存在；worktree 追蹤版本已驗證符合指定 SHA，沒有複製或寫入原專案。

## 前輪已驗證／怎麼驗證

1. 既有 11 項離線測試全部通過：test_regression_support.py 7 項、test_seal_failure.py 2 項、test_replay_policy.py 2 項。後者只將 AST 斷言比對改成保留重數、不依走訪順序的比對；新增 with 區塊改變廣度走訪深度，原斷言內容與數量均未刪減。
2. 新增 14 項正式檔保護測試全部通過，包含：舊 seal／reconcile 仍接受歷史 SHA 且拒絕現行 SHA；新模式 reference／部署 SHA 不符即失敗；執行期間部署／reference 改變即失敗；行為例外仍記錄結束雜湊；缺少、失敗或不一致的套件保護收據被拒絕；其他 protected 檔案改變仍失敗。這些是雜湊模擬與實際斷言區塊的離線測試，不是戰鬥通過證據。
3. 八入口 CLI、68 支直接子驅動轉接語法檢查通過；reconcile CLI 也可顯示新增參數。針對十支既有變更程式，AST 比對 9955180，全部原 assert（含重複次數）保留；變更檔案均為單一連結。
4. 移除子程序 JAVA_HOME，將已安裝 JDK 的 bin 放入該子程序 PATH，以 java_tool 實跑 java／javac -version 均通過；未改系統環境。
5. **唯讀實檔稽核**：reference 與部署檔符合 e1498871…；稽核區塊前後部署 SHA 相同。候選符合 `8d26c9d791e094ab3d3cd6c8dbe4218d99f96fcbe73858be5af534106c81cfb8`；歷史 protected 中其他檔案（ddraw.dll）符合原收據。
6. git diff --check 通過。未執行完整行為套件或新的成功 seal；上述實檔量測只涵蓋離線稽核區塊，不能當成全套回歸期間的保護證明。

### 套件對照表

| 套件 | 新通過收據 SHA256 | 本次案例數／組數 | 既有收據案例數／組數 | 一致與否 |
|---|---|---|---|---|
| targeted | 無，未產生 | 未執行 | 27 組／2,104 案 | 待驗證 |
| player-cancellation | 無，未產生 | 未執行 | 3 組／296 案 | 待驗證 |
| composed | 無，未產生 | 未執行 | 71 組／5,164 案 | 待驗證 |
| existing_20 | 無，未產生 | 未執行 | 20 組 | 待驗證 |
| affected_18 | 無，未產生 | 未執行 | 18 組 | 待驗證 |
| curse-clock | 無，未產生 | 未執行 | 24 案 | 待驗證 |

targeted＋player 的歷史基準為 2,400 案。前輪已核對的歷史收據 SHA 保留如下；它們不是本次新產物：

| 既有收據 | SHA256 |
|---|---|
| targeted | `f0c9902051c1d482d000d2fe013aa45917ae0538fd743734ecbbe1ddcd62286a` |
| player-cancellation | `6c88a681178718b19eeaa03130ca78e23a3b21b31ee8e29d8c9469aa40c4145e` |
| composed | `24222fb09585e66eb82f58143cf89d6133f72ba3157a54687bb9a3c6b5c1b8c8` |
| existing_20 | `3d4e0edaed9847b1c3e0d4c6f89ce28c194dc587fe2fb628de32cb6f8b80f252` |
| affected_18 | `329f00e77a08494b24103c6169b88b21441c56df298cecd2a249b6d48f8a35c2` |
| curse-clock | `b191689cbe55468fad6e7e2a2d3ba903c57b12b9ad257a1343ef4a3a90c9018b` |

## 阻礙、推測與待驗證

**已確認的前置缺口**：worktree 與原專案的 `scratch/t020c/isolated-test.properties` 都不存在。Issue 留言有主控要求由本機對話準備隔離設定，以及後續調度器直接移除 needs:local 並排隊的紀錄，但沒有「隔離設定已就緒」或專用 SELECT 帳號就緒的證據。可直接派工不等於 SQL 前置已完成。

行為 probe 需要讀資源表；前輪沒有隔離設定與 SELECT 專用帳號，因此未啟動這些 probe，也沒有自行建立帳號／授權或讀取正式設定。這是本機資源前置，不交給使用者準備。主控的正式基準裁定已完成程式銜接，不需再重派相同裁定。

**推測**：隔離資源就緒後可依下列順序嘗試全套；仍可能遇到歷史 fixture、golden、時序或資料差異，不能推定通過。

**待驗證**：全部行為案例數、新收據 SHA、reconciliation 的實際 48 組重試鏈、seal 全部 assert、各套件實跑前後正式 SHA 相同。歷史 SQL 啟動／清理收據維持原核對邏輯；前輪獨立核對仍只代表歷史證據，前輪未重跑 SQL 啟動／清理。

未讀取或複製 config/server.properties、未連任何 DB、未啟停 DB／正式或測試伺服器／遊戲、未改正式 JAR、未部署候選、未讀寫封存目錄、未修改硬連結。預設 sandbox 初始化失敗後，使用工具核准的 require_escalated 執行完成前輪檢查，沒有自動審查拒絕。

## 產出路徑

- 報告：`docs/reports/issue-31.md`。
- 前輪程式：`lito伺服器/LiTo381-Server/tools/combat-continuation/` 的任務指定十支程式及新增 test_formal_protection.py。
- 前輪不進 git 的離線稽核：`scratch/t020c/8d26c9d7/formal-protection-audit.json`，SHA256 `ca125d107d86aec3e1249fdc58b3e52790d128130fdcd2a00cd7a8a8707c5a4c`。內含實檔 before／after、測試程式與 log SHA、原斷言保留檢查、候選 SHA、兩處隔離設定不存在的結果；behavioral_cases=0。
- 前輪不進 git 的重現腳本與 log：`scratch/t020c/verify_formal_revision.py`、`test_{regression_support,seal_failure,replay_policy,formal_protection}.log`、`check_adapters.log`、`jdk-path-check.log`。既有 scratch 證據仍保留，均不標為本次結果。

## 給下一棒的提示

1. **調度器／Claude 本機對話**：本次結果須保持 blocked／未完成，恢復 needs:local 前置交接；不要因報告已提交或分支已推送而標為 done。安排原先 needs:local 前置，準備 worktree 內的 scratch/t020c/isolated-test.properties，使用已運行 MySQL 上僅有 SELECT 權限的資源專用帳號；不得碰正式設定、啟停 DB 或寫表。完成後按 Issue 指示只回報路徑與帳號名稱，不寫密碼，再排本機測試。設定未就緒前不要再反覆派完整回歸。
2. **本機執行器**：先確認隔離帳號權限證據與候選 SHA，再依下列順序逐套執行；每一步 exit 0 才繼續。新收據必須來自實跑，補齊上表並使 seal 全部通過。保留全部案例與既有 timeout／行為斷言。
3. **Claude 審查**：可沿用前輪基準保護與離線測試審查，但 Issue 保持未完成，不能以 25 項離線測試取代全套行為驗收。

以下是尚未執行的完整回歸指令；必須在此 worktree 執行：

```powershell
$drivers = 'lito伺服器/LiTo381-Server/tools/combat-continuation'
$resources = 'D:/lineage開發/3-lito開發/lito伺服器/LiTo381-Server'
$candidateJar = "$resources/tools/combat-continuation/cancellation-battle-runtime/build/candidate-cancellation-battle-runtime.jar"
$isolatedConfig = 'scratch/t020c/isolated-test.properties'
if (-not (Test-Path -LiteralPath $isolatedConfig)) { throw '隔離設定尚未就緒' }
$sharedArgs = @('--jar', $candidateJar, '--out', 'scratch/t020c/8d26c9d7', '--resources', $resources, '--config', $isolatedConfig)
py -3.12 -B -X utf8 "$drivers/cancellation-battle-runtime/replay_targeted.py" @sharedArgs
if ($LASTEXITCODE -ne 0) { throw 'targeted 失敗，停止' }
py -3.12 -B -X utf8 "$drivers/cancellation-battle-runtime/replay_player_cancellation.py" @sharedArgs
if ($LASTEXITCODE -ne 0) { throw 'player 失敗，停止' }
py -3.12 -B -X utf8 "$drivers/replay_composed_materials.py" cancellation-battle-runtime @sharedArgs
if ($LASTEXITCODE -ne 0) { throw 'composed 失敗，停止' }
py -3.12 -B -X utf8 "$drivers/candidate_regression.py" cancellation-battle-runtime @sharedArgs
if ($LASTEXITCODE -ne 0) { throw '20＋18 組失敗，停止' }
py -3.12 -B -X utf8 "$drivers/cancellation-battle-runtime/test_curse_clock_boundary.py" @sharedArgs
if ($LASTEXITCODE -ne 0) { throw 'curse-clock 失敗，停止' }
py -3.12 -B -X utf8 "$drivers/cancellation-battle-runtime/seal_validation.py" @sharedArgs
if ($LASTEXITCODE -ne 0) { throw 'seal 失敗，保持未完成' }
```
