# Issue #414：T141 外部入口重測與必要回歸

**結論：部分通過（✅ 8／❌ 2／⚠️ 0）。** 2026-11-14，分支 `task/414`。06、10 重測通過；07 的實際四端點不符合本張「只有兩端點」文字；08 被客戶端 `TW13081901.txt` 指紋核對拒絕，未啟動遊戲，沒有大廳 PNG。未修產品程式、未改釘選、未使用 `--legacy-formal-mode`，不放行大廳驗收。

## 做了什麼／怎麼驗證

工作目錄為指定 worktree，建置基準 `origin/main = 5334f93529ce8c1c5eec9741cec12b00b5c8530d`，未切換或另建分支。已讀 `AGENTS.md`、`docs/REPORT.md`、T139 文件及 T140 報告。依 [#414 主控補充](https://github.com/chaoren0418-arch/lito-launcher-server/issues/414#issuecomment-6870602933) 補做 #413 尚缺的 SELECT／profile；GUI 放行與舊基準快照沿用。`docs/REPORT.md` 未編輯。

以下 `E = docs/reports/issue-414/`；`R = scratch/t014/run-20261114T030157Z/`。01–05、09 沿用 #284 第 5 輪，來源報告位於 `D:\lineage開發\3-lito開發\scratch\wt\task-284\docs\reports\issue-284.md`；其 SHA、Git commit 與沿用範圍見 [inherited/source.json](issue-414/inherited/source.json)。必要建置、冷備份及資料保護檢查不改成那些列的重測判定。

| 編號 | 結果 | 實際觀察（現象） | 重現步驟 | 證據路徑（相對 E） |
|---|---|---|---|---|
| 01 | ✅ 通過（沿用） | 前輪建置通過；本輪為 08 前置另建置最新 main，Maven 成功、登入器 0 警告／0 錯誤。 | `mvn -f lito伺服器/LiTo381-Server/pom.xml -B -DskipTests package`；`dotnet build lito登入器/LiTo381-Launcher/src/LiTo381Launcher.csproj -c Release`。 | `inherited/retest-artifact-hashes.json`；本輪 `maven-build.txt`、`launcher-build.txt`、`artifact-hashes.json` |
| 02 | ✅ 通過（沿用） | 前輪 22 鍵與 combat 設定已驗證；本輪私密實驗設定由既有 Prepare.java 產生。 | 沿用 Prepare.java 與實驗路徑；本輪靜態庫採 `p24_380db_lito_exp`。 | `inherited/retest-config-public.properties`、`inherited/retest-combat.properties`；本輪 `config-public.properties`、`combat.properties` |
| 03 | ✅ 通過（沿用） | 前輪冷備份通過；本輪啟動前另備份 1,722 檔／279,502,555 bytes，逐檔 SHA 相同。 | P24 離線且實驗埠空閒時普通複製 datadir，再比對 SHA。 | `inherited/retest-p24-backup.json`；本輪 `p24-backup.json`、`lease-observation.json` |
| 04 | ✅ 通過（沿用） | 前輪 17 表／731 中文欄位值差異 0；本輪沒有遷移／DDL。 | 沿用前輪欄位與值比對。 | `inherited/retest-equipment-migration-plan.json`、`inherited/retest-equipment-data-after.tsv` |
| 05 | ✅ 通過（沿用） | 前輪來源路由通過；本輪補查名稱為 `P24LiToExp`、id=1、角色庫 `p24_accounts_lito_exp`。 | 唯讀 SELECT `source_id,display_name,character_database`，將結果填入外部 profile。 | `inherited/retest-counts-before.txt`；本輪 `source-server.tsv`、`external-profile.json` |
| 06 | ✅ 通過（重測） | 最新 Maven JAR 約 4 秒 ready，無 replay；本輪確實啟動了 `84BA0CDE…EDCC`。 | 全新空 runtime，以 Java 17 `-jar LiTo381-Server.jar config/experiment.properties` 啟動並讀取 stdout。 | `server-owned.json`、`server-console.log`、`server-stderr.log`、`artifact-hashes.json` |
| 07 | ❌ 失敗（重測；驗收文字差異） | 實際開四個 loopback；現行 m4 腳本 PASS，但多出本張未允許的 `127.0.0.2:42083`、`127.0.0.1:42084`。 | `m4-listen-check.ps1 -ProcessId 14640 -SourceId 1`，將實測結果另與本張兩端點要求比對。 | `listen-check.txt`、`listen-task-comparison.json` |
| 08 | ❌ 失敗（重測） | 外部模式／200%／run-root 正確；按啟動後 `check_source` 拒絕 `TW13081901.txt`，未到選服、帳密、角色或大廳。 | 設定下方環境變數，以 `--external-server-profile R/external-profile.json` 啟動新 EXE，鍵盤 Enter 啟動一次。 | `launcher-before.txt`、`launcher-progress.txt`、`request.json`、`terminal.json`、`startup-pin-mismatch.json`、`receipt-inventory.json` |
| 09 | ✅ 通過（沿用） | 前輪資料保護通過；本輪附帶檢查 `character_lito` 2→2、結算表 0→0，原三庫 270 表 checksum 全相同。 | 本輪啟動前與 Java 關閉後，COUNT(*) 與原三庫 CHECKSUM TABLE EXTENDED。 | `inherited/retest-original-checksum-*.tsv`；本輪 `counts-*.tsv`、`original-checksum-*.tsv`、`data-protection.json` |
| 10 | ✅ 通過（重測） | 自有登入器 7536、Java 14640、P24 parent 1060／listener 18856 全退出，03:07:16Z 實驗埠全釋放；無遊戲程序。 | 核對 PID＋路徑＋建立時間後停止登入器／Java；核對 DB 身分與無其他連線後 SHUTDOWN。 | `app-cleanup.json`、`p24-shutdown.json`、`process-cleanup.json`、`later-port-reuse.json` |

## 已驗證：08 拒絕原因與寫入邊界

- 本輪 JAR SHA-256：`84BA0CDE7ABACE28456232B86922B22DD7FBFF98E8B44E8B7BEC5F995A79EDCC`；與 #284 第 5 輪新 JAR 相同，本輪已取得它的 ready 證據。新建登入器及兩支 helper 指紋見 [launcher-artifacts.json](issue-414/launcher-artifacts.json)，外部 helper 為 T139 的 `3CAE67C0…28B9`。
- 啟動環境：`LITO_LAUNCHER_SETTINGS_DIRECTORY=R/launcher-settings`、`LITO_LAUNCHER_RUN_ROOT=R/launcher-runs`；R 在本 worktree，實際完整路徑已記於 request。GUI 文字控制項確認 200%、一般無界擂台、外部 42083／42084；沒有加 legacy 旗標。
- `TW13081901.txt`：釘選 **`7EBE2B464C8B1EA9C58AC985DD8FF4C823784D8231894262EDDCD7691CA94008`**，實際 **`28C4FC600833FB8C609AB195A67FF926E8FFA571100D8D4B389A23BE8C2C8DB8`**。實際值與 T140 基準相同，差異早於本輪存在。拒絕發生於 `development_startup.py:69`，先於 T139 DLL／ini 核對、外部端點檢查與 native 建立。未覆寫、未修改釘選、未再次嘗試。
- `request.json`、失敗 `terminal.json` 位於 `R/launcher-runs/gui-runs/external-6e70fe9662a740c0a6ddddbd828c8c02/`；沒有 `server-ready.json`、`native-created.json`、helper runs 子目錄或 `08-lobby.png`。terminal 的 `cleanup_qualified=false` 是 helper 在 ready 前 EOF 的實際失敗收據，不能解讀為成功清理證明；本輪另外以 PID 核對清理。
- [完整性證據](issue-414/integrity.json)：本輪前後各 **669 檔／9,607,667,880 bytes**，路徑、大小、mtime、SHA 差異 **0**；與 #413 基準逐檔大小／SHA 差異也為 **0**。範圍依 T140：客戶端頂層檔案及 `LiTo/` 全部子檔，其他子目錄不遞迴。`litoddraw.dll` 與 ini 符合預期，沒有對客戶端寫入。
- 兩個例外日誌也未變：`lito381-compat.log` **0→0 bytes**；`LiToError.txt` **16,095→16,095 bytes**。原始專案 `app/gui-runs`、`app/runs` 的新增／移除目錄皆 **0**。
- Computer Use 的兩次畫面擷取分別回 `FrameArrived timed out: timed out waiting on channel`、`window capture timed out: timed out waiting on channel`；滑鼠按鈕操作回 `coordinate input geometry is unavailable`。文字樹與鍵盤 Enter 可用，確實觸發上述一次 helper 啟動。未取得可保存的 PNG，沒有用空白圖或文字圖替代大廳。

## 已驗證：資料保護、程序與限制

僅使用 P24Lab `127.0.0.1:43324`，UUID／datadir 見 `identity-before.tsv`、`identity-after.tsv`；未連正式 3306／2000。lease 觀測仍指向 #436，依本張「程序疑慮只記錄」規則記錄；備份及啟動時實驗埠沒有 listener、無既有 P24 程序。本輪未修改 lease 或調度器。沒有讀正式 `config/server.properties`，只在記憶體讀 #83 實驗交接憑證並將私密設定寫至忽略的 R。

完整冷備份在 `R/private-cold-backup/db-data/`，雜湊清單為 `R/private-backup-manifest.json`；可還原本次允許寫入表的 SQL 為 `R/private-character-lito-restore.sql`，先指定實驗庫，再以 transaction 還原 `character_lito` 的既有兩列。SQL 未執行、不公開私密備份。現場沒有 `mysqldump.exe`，因此用唯讀 SELECT 產生帶完整欄名及 QUOTE／NULL 處理的還原 SQL；此前冷備份已完整核實。本輪沒有登入、沒有人工 DML／DDL，不宣稱兩個實驗表已做逐欄相等檢查；原三庫則有 270 表 checksum 前後證據。

登入器與 Java 經身分核對後使用 `Stop-Process`，**不是 Java Ctrl+C 正常退出驗收**；本張只驗證自有 PID／埠釋放。P24 正常 SHUTDOWN exit 0。03:09 復核時另一組 P24 parent 20388／listener 4452 已在 03:08:44Z 重占 43324，晚於本輪釋放；未停止該程序，也未再連 DB。`process-cleanup.json` 因此保留當時的非自有 listener，不能誤報全機一直無監聽。

本輪沒有產品修改，不額外執行單元測試；驗證採建置、實際 ready／監聽／GUI helper 收據、逐檔快照及 DB 保護檢查。公開附件統一為 UTF-8／LF（P24 stderr 原為 CP950、PowerShell 建置輸出原為 UTF-16）；只正規化編碼／換行／行尾空白，原始 run 保留。JSON、報告連結、10 列結果、證據一致性、實際私密密碼掃描與 `git diff --cached --check` 均通過，見 `artifact-validation.json`。原 BIN、正式程序、原始專案、封存、硬連結、系統設定均未修改，未移除任何 worktree 或遞迴刪除目錄。

## 推測／未驗證／給下一棒的提示

1. **主控／Claude**：先裁定 07 的兩端點任務文字與現行四端點設計／腳本差異；目前登入器也會檢查 `127.0.0.1:42084`，不能直接為符合舊文字而關閉它。本輪只記錄，沒有改程式或驗收準則。
2. **資源／登入器維護者**：查明既有 `TW13081901.txt` 與正式釘選差異的來源，走獨立核准的修正流程；本報告不授權覆寫客戶端或放寬釘選。亦應檢視 check_source 在事件保護區外拋錯，導致 GUI 報「清理尚未確認」的行為。
3. **執行環境維護者**：另查視窗擷取逾時；文字控制項成功不等於能取得 PNG。這是附帶環境限制，08 的首個產品流程阻擋已由失敗收據確認。
4. **下一次本機重測**：前置修好後只處理 07／08，附帶其必要的建置、備份、資料保護與自有 PID 清理；已過項目不原樣全套重跑。選服、測試帳密、角色選擇、大廳、遊戲啟動後日誌變化與成功收據仍待驗證，不能由本輪推定。

產出：本報告、`docs/reports/issue-414/` 公開證據；一次性腳本在 `scratch/issue-414/`、完整本機 run 在 R，私密設定／冷備份／還原 SQL 不提交。調度器清理 worktree 前須保全本機證據。
