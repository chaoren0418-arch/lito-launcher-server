# Issue #459：T158，只重測 08

**結論：部分通過（✅ 9／❌ 0／⚠️ 1）。** 08 卡在 GUI 操作通道，未觸發遊戲啟動，沒有大廳 PNG；不能判定產品登入流程成功或失敗。01–07、09、10 沿用 [#414](https://github.com/chaoren0418-arch/lito-launcher-server/blob/task/414/docs/reports/issue-414.md)，本次建置、備份、開服、保護檢查與清理僅為 08 的必要前後置。

## 已驗證

2026-11-14，既有 `task/459`；已讀 AGENTS.md、docs/REPORT.md 與外部入口驗收文件。`git fetch origin` 後 worktree／main 均為 `c65036e30f58197371d6454f8d71f4e02f751177`。原始專案仍為 `main@b972e23d`，只落後 #458 報告及索引兩檔，產品程式與最新 main 相同；遵守原始專案只讀，未同步寫入。原始 helper 是 11,044 bytes、`D8AB4990…B83EEA`，已排除 #454 的舊版檔案問題。

下表 `E = docs/reports/issue-459/`；所有「沿用」均非重跑原案例。

| 編號 | 結果 | 實際觀察（現象） | 重現步驟 | 證據路徑 |
|---|---|---|---|---|
| 01 | ✅ 沿用；必要建置成功 | 新 JAR 封裝成功，登入器 0 警告／0 錯誤，helper SHA 符合文件。 | `mvn '-Dmaven.test.skip=true' package`；`dotnet build -c Release`。 | `E/server-build.txt`、`E/launcher-build.txt`、`E/runtime-artifacts.json`、`E/artifact-hashes.json` |
| 02 | ✅ 沿用 | #83 設定只讀取，另產生 worktree 實驗設定；43324、42083、`*_lito_exp`。 | 既有 Prepare.java 核對設定契約；未改正式設定。 | `E/config-public.properties`、`E/preparation.log` |
| 03 | ✅ 沿用；本次另備份 | 冷備份 1,722 檔逐檔 SHA 相同後才啟動 P24。 | 冷備份、核對 lease 與 P24 UUID。 | `E/p24-backup.json`、`E/lease-observation.json`、`E/p24-owned-verified.json` |
| 04 | ✅ 沿用 | #414／#284 的 17 表、731 中文欄位值差異 0；本次未遷移。 | 不重跑。 | #414 `inherited/retest-equipment-migration-plan.json`、`inherited/retest-equipment-data-after.tsv` |
| 05 | ✅ 沿用 | 唯讀核對來源與 #414 profile 相同：1／P24LiToExp／p24_accounts_lito_exp，直接複用該 profile。 | 43324 SELECT 後比較 #414 profile。 | `E/source-server.tsv`、`E/external-profile.json` |
| 06 | ✅ 沿用；本次 ready | 新 JAR 的 Java PID 19048 輸出 `[LiTo] ready`。 | 以本次私有實驗設定啟動一次。 | `E/server-console.log`、`E/server-owned.json` |
| 07 | ✅ 沿用／主控裁定 | 四端點是已接受設計；本次開服觀察一致。 | 不重跑案例。 | #414 `listen-check.txt`；`E/server-console.log` |
| 08 | ⚠️ 無法判定 | 外部模式 GUI 已開啟；操作工具無法啟用視窗，未觸發遊戲啟動。 | EXE 加 `--external-server-profile <run>/external-profile.json`，200%，不加 legacy；Enter／按鈕／Raise，重綁與通道重設仍失敗。 | `E/08-launcher-input-blocked.png`、`E/launcher-initial-ax.json`、`E/ui-automation.json`、`E/source-preflight.json` |
| 09 | ✅ 沿用；本次保護檢查 | 客戶端前後及 #413 基準差異 0；P24 原三庫 270 表 checksum 不變，實驗戰績／結算仍 2／0。 | 同 #413 範圍比對路徑、大小、mtime、SHA；43324 唯讀 checksum。 | `E/client-{before,after}.json`、`E/baseline-compare-{before,after}.json`、`E/data-protection.json` |
| 10 | ✅ 沿用；本次清理完成 | 登入器已關閉、Java 已停止、P24 正常 SHUTDOWN；42061／42083／42084／43324 無 listener，五個自有 PID 均不存在。 | 核對 PID、路徑、建立時間；登入器 CloseMainWindow、Java Stop-Process、P24 身分核對後 SHUTDOWN。 | `E/app-cleanup.json`、`E/p24-shutdown.json`、`E/final-cleanup.json` |

### 08 的停點與版本

- 新 JAR SHA-256：`84BA0CDE7ABACE28456232B86922B22DD7FBFF98E8B44E8B7BEC5F995A79EDCC`。
- 新登入器 EXE SHA-256：`42F668871BAD5FAC2F94662F6601EE1897DFFD8EE41DFC993421D59AF7FC8648`；實際 .NET DLL 與兩支 helper 詳見 `E/runtime-artifacts.json`。
- `development_startup.py`：`E30E88C3485F12A97F1D59C74205D6DD40D5014DA7779C1574B6B251A2BB0402`；`external_server_startup.py`：`BDCE29438DBABA7BAF498841870023D5E3A8493D491C23EC555ABD8D89B7C3D6`。
- 直接呼叫建置輸出的真實 helper `_formal_helper().check_source(...)` 及 `verify_client_compat(...,200)`：均 PASS、writes=0。只讀取真實檔案，無 stub／替代實作；**這不是 GUI 成功收據，更不是大廳驗收**。
- 操作工具回傳 `failed to activate captured window`；重新列舉／綁定、Raise、正常關閉後以最大化重開、CUA session 重設仍相同。按鈕操作回傳 `coordinate input geometry is unavailable`，擷取回傳 `FrameArrived timed out: timed out waiting on channel`。已改 PowerShell `System.Drawing.CopyFromScreen` 保存 PNG，另以 `PrintWindow` 取證，均未裁切或改造。兩張 PNG 都僅顯示登入器上半部；完整模式／設定／日誌另存 AX 文字，不能把這些圖視為大廳。最大化要求與 AX 的「還原」按鈕亦未反映在擷取尺寸，不推定視窗實際呈現狀態。
- 最初 Maven 指令因 PowerShell 未加引號而把 `-Dmaven.test.skip=true` 拆錯，在 lifecycle 解析階段失敗、尚未編譯；修正命令引號後完成唯一一次建置。啟 P24 的首次腳本載入因空的 `$PSScriptRoot` 在建立程序前失敗；改成本 worktree 明確路徑後啟動一次。均未修產品程式。

### 資料保護與清理

客戶端範圍完全依 #413：頂層全部檔案與 `LiTo/` 子樹，其餘子目錄不遞迴；前後 **669 檔／9,607,667,880 bytes**，與基準逐檔完全相同。`lito381-compat.log` 維持 0 bytes、`LiToError.txt` 維持 16,095 bytes；原始專案 `app/runs`、`app/gui-runs` 清單未變。沒有 GUI `request.json`、`server-ready.json`、`native-created.json` 或 `terminal.json`，也未觀察到 LiTo.bin。

本張啟動的登入器 PID 1488／8724、Java PID 19048、P24 MySQL 父／子 PID 8736／17984 已全數結束；只需 P24Lab 的 MySQL，未另啟動 42061 遊戲服務。最後核對時間 `2026-11-14T12:20:41Z`，所有實驗埠皆釋放，lease 仍為本張、到期 `2026-11-14T13:08:06.242450Z`。無手動 SQL 寫入或測試帳號變更，無新增資料修正需還原；私有冷備份與保險還原 SQL 保留。

## 推測

目前證據指向 GUI 操作通道故障；不能由此判定登入器、遊戲協定或帳號有缺陷，也不能推定新版資源預檢通過後必能到大廳。

## 待驗證／給下一棒的提示

選服、#83 測試帳號登入、選角、大廳與 `docs/acceptance/m4-84ba0cde/08-lobby.png` 尚未完成，未產生該大廳檔。交調度器／本機工具維護者先恢復視窗啟用與輸入通道，再派只重測 **08**；不要再修改 SHA 釘選或客戶端，也不要重測其他九項。PowerShell 截圖可用，但它沒有修復輸入通道。

產出為本報告與 `E`。本機 run 為 `scratch/t014/run-20261114T120955Z/`；冷備份、完整 manifest、原三庫 checksum 與保險還原 SQL 留在其中的 `private-cold-backup/`、`private-backup-manifest.json`、`original-checksum-{before,after}.tsv`、`private-character-lito-restore.sql`，不入 git、不執行還原。一次性腳本在 `scratch/issue-459/`。未改 docs/REPORT.md、產品程式、原始專案、客戶端、封存、原 BIN 或系統設定；未碰正式 2000／3306，未刪除 worktree／junction 或遞迴刪除目錄。
