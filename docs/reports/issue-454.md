# Issue #454：T156，只重測 08

**結論：部分通過（✅ 9／❌ 1／⚠️ 0）。** 08 在 `check_source` 拒絕原始專案的 `app/world_resource_preflight.py`，未建立遊戲程序，沒有大廳 PNG。九個既有通過項目沿用 #414；本次建置、備份、啟動、保護檢查與清理是重測 08 的必要前後置。

## 已驗證

2026-11-14，既有分支 `task/454`；已讀規範與 `docs/REPORT.md`。`git fetch origin` 後 HEAD 與 main 同為 `b972e23df7a6ad787c7cf9e3b12e641806df3df1`，包含 #448、#453，無需合併或切分支。lease 持有人為本張，到期 `2026-11-14T07:53:55.310093Z`，與派工一致。執行與清理均在有效期內。

下表 `E = docs/reports/issue-454/`。沿用項目詳見 [#414 報告](https://github.com/chaoren0418-arch/lito-launcher-server/blob/task/414/docs/reports/issue-414.md)，沒有重新執行其他驗收案例。

| 編號 | 結果 | 實際觀察（現象） | 重現步驟 | 證據路徑 |
|---|---|---|---|---|
| 01 | ✅ 沿用；必要重建成功 | 新 JAR 與登入器封裝成功，兩支 helper SHA 與驗收文件一致。 | `mvn -Dmaven.test.skip=true package`；`dotnet build -c Release`。 | `E/server-build.txt`、`E/launcher-build.txt`、`E/artifact-hashes.json`、`E/runtime-artifacts.json`、`E/launcher-artifacts.json` |
| 02 | ✅ 沿用 | #83 實驗憑證只讀取，另產生 worktree 設定；43324、42083、`*_lito_exp`，設定契約 PASS。 | 執行既有 `Prepare.java`，使用實驗設定與 `.example`。 | `E/config-public.properties`、`E/preparation.log` |
| 03 | ✅ 沿用；本次另備份 | P24 冷備份 1,722 檔，逐檔 SHA 相同後才啟動。 | 複製 P24Lab `db-data` 至本次私有備份並核對。 | `E/p24-backup.json`、`E/lease-observation.json` |
| 04 | ✅ 沿用 | 沿用 #414／#284 的 17 表、731 中文欄位值差異 0；本次未做遷移。 | 不重跑。 | #414 的 `inherited/retest-equipment-migration-plan.json`、`inherited/retest-equipment-data-after.tsv` |
| 05 | ✅ 沿用 | #414 profile 與本次唯讀 SELECT 相同：id=1、P24LiToExp、p24_accounts_lito_exp。 | 複用 #414 profile；43324 核對啟用來源。 | `E/external-profile.json`、`E/source-server.tsv`、`E/identity-before.tsv` |
| 06 | ✅ 沿用；本次 ready | 本張啟動 P24Lab 與新 JAR，Server 明確輸出 `[LiTo] ready`。 | 隱藏視窗啟動自有 MySQL PID 2520／16304；Java PID 19916。 | `E/server-console.log`、`E/server-stderr.log`、`E/server-owned.json`、`E/p24-owned-verified.json` |
| 07 | ✅ 沿用／主控裁定 | 四個 loopback 端點為已接受設計，本次啟動觀察一致。 | 沿用 #414 的四端點 PASS，不重跑此案例。 | #414 的 `listen-check.txt`；`E/server-console.log` |
| 08 | ❌ 本次重測 | 新登入器已開啟外部模式並觸發啟動；`world_resource_preflight.py` 指紋不符，未進選服／登入／選角／大廳。 | 依驗收文件啟動，200% 縮放；以 Enter 觸發預設「啟動 LiTo.bin」按鈕。 | `E/08-preflight-failed.png`、`E/launcher-request.json`、`E/launcher-terminal.json`、`E/preflight-fingerprint.json` |
| 09 | ✅ 沿用；本次保護檢查 | 客戶端前後及 #413 基準完全相同；P24 原三庫 270 表 checksum 相同，實驗戰績／結算筆數仍為 2／0。 | 同 #413 範圍逐檔 SHA、大小、mtime 比對；43324 唯讀核對。 | `E/client-before.json`、`E/client-after.json`、`E/baseline-compare-before.json`、`E/baseline-compare-after.json`、`E/data-protection.json` |
| 10 | ✅ 沿用；本次清理完成 | 登入器關閉、Java 依自有 PID 結束、P24 正常 SHUTDOWN；42061／42083／42084／43324 均無 listener。 | Alt+F4 關登入器；核對 PID、路徑及建立時間後停 Java；核對 P24 UUID 後 SHUTDOWN。 | `E/app-cleanup.json`、`E/p24-shutdown.json`、`E/final-cleanup.json` |

### 08 的直接原因與畫面

登入器命令為本 worktree 建出的 `lito登入器/LiTo381-Launcher/src/bin/Release/net10.0-windows/LiTo381Launcher.exe --external-server-profile <run>/external-profile.json`，沒有 `--legacy-formal-mode`。設定與收據使用本 worktree 的 `LITO_LAUNCHER_SETTINGS_DIRECTORY`／`LITO_LAUNCHER_RUN_ROOT`。

`<run> = D:\lineage開發\3-lito開發\scratch\wt\task-454\scratch\t014\run-20261114T065600Z`。

- JAR SHA-256：`84BA0CDE7ABACE28456232B86922B22DD7FBFF98E8B44E8B7BEC5F995A79EDCC`。
- 登入器 EXE SHA-256：`8F82C64CE1DAA5643426CF1CF0CB0DCF3DFC74929870AFCBB729CAB3D1EF1439`；實際 .NET DLL SHA 另記於 `E/runtime-artifacts.json`。
- 被拒絕的是**原始專案** `D:\lineage開發\3-lito開發\lito登入器\LiTo381-Launcher\app\world_resource_preflight.py`：實際 **3,705 bytes**，SHA `353C362EC12D3443C790D707FBC26B50944081BC2D1DA9967A75D471CB289F34`。
- 本次 main／helper 預期 **11,044 bytes**，SHA `D8AB4990D1AA40AC366B47DC9CE90D96351B2ED518B56BBE18BE3BE0C4B83EEA`。兩檔均無 CRLF，並非換行差異。helper 仍以絕對路徑讀原始專案 `app`，沒有採用 worktree 的新版檔案。
- GUI 收據：`failed(stage=check_source, writes=0)`，結束碼 1、`accepted=false`、`product_complete=false`、`cleanup_verified=true`。未產生 `server-ready.json`／`native-created.json`，不把 Server 自己的 ready 當成客戶端成功。
- 截圖工具 `sky.get_window_state(include_screenshot=true)` 回報 **`FrameArrived timed out: timed out waiting on channel`**；元素點擊亦遇到 `coordinate input geometry is unavailable`。已改用鍵盤操作及 PowerShell `System.Drawing.CopyFromScreen`，最大化登入器後成功保存完整錯誤畫面；原始 PNG 未裁切或改造，擷取資訊見 `E/screenshot.json`。一次性擷取腳本在 `scratch/issue-454/capture.ps1`。

### 資料保護與產出

客戶端範圍完全依 #413：`0-客戶端` 頂層全部檔案及 `LiTo/` 子樹，其餘子目錄不遞迴。前後皆 **669 檔／9,607,667,880 bytes**，路徑、大小、mtime、SHA 差異 0；`lito381-compat.log` 仍 0 bytes、`LiToError.txt` 仍 16,095 bytes。原始專案 `app/runs`／`app/gui-runs` 目錄清單不變。未覆寫原始專案、客戶端、原 BIN、封存或機密設定，未接觸正式 2000／3306，未改產品程式。

本次未手動修改 SQL 或建立測試角色，無新增資料修正要還原。完整冷備份、manifest、既有兩筆 `character_lito` 的保險還原 SQL 與 270 表前後 checksum 留在 `<run>/private-cold-backup/`、`private-backup-manifest.json`、`private-character-lito-restore.sql`、`original-checksum-{before,after}.tsv`，不入 git、不執行還原。公開前後筆數見 `E/counts-before.tsv`／`E/counts-after.tsv`。

產出為本報告與 `E`；未建立 `docs/acceptance/m4-84ba0cde/08-lobby.png`，因為沒有到大廳。未修改 `docs/REPORT.md`，未移除 worktree、junction 或遞迴刪除任何目錄。

## 推測

原始專案的交付檔尚未同步 #453，造成新 helper 搭到舊檔；已直接驗證檔案內容不一致，但本張不調整部署或修改釘選。修正此檔後是否能到大廳仍不能推定。

## 待驗證／給下一棒的提示

交主控安排修正交付來源的一致性：讓外部模式實際讀到與新版釘選相符的 `world_resource_preflight.py`，或明確設計可驗證的隔離交付入口。本張依「原始專案只讀、失敗不自行修程式」規則沒有覆寫。完成後只重測 **08** 的選服、#83 帳號登入、選角、大廳與 PNG；01–07、09、10 仍沿用。截圖可沿用本次 PowerShell 方法，無需再卡在原截圖工具。
