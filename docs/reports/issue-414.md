# Issue #414：T141 第 3 次執行，08 預檢仍拒絕

**結論：部分通過（✅ 9／❌ 1／⚠️ 0）。** 2026-11-14。08 仍因客戶端指紋不符而停止，沒有大廳 PNG；其他九項沿用已驗證結果，並非本次重新通過。

## 做了什麼／怎麼驗證

已讀 `AGENTS.md`、`docs/REPORT.md`、外部入口驗收文件及 #443 報告。在既有 `task/414` 合併 `origin/main`（`81b3a2fa`）；相較前次 main 僅多報告索引，產品程式沒有修正。未 checkout、另建分支或手改 `docs/REPORT.md`。

依 [主控裁定](https://github.com/chaoren0418-arch/lito-launcher-server/issues/414#issuecomment-6870813965) 只重測 08，其餘沿用。[最新調度留言](https://github.com/chaoren0418-arch/lito-launcher-server/issues/414#issuecomment-6871541113) 記載採方案 B（不改客戶端、另開程式任務重產目錄與釘選），但 main 尚未包含修正。本張為 test，未自行修改產品或釘選。

本次重建登入器，直接執行輸出目錄的原版 `external_server_startup.py`，使用既有實查 profile、200% 縮放及 worktree run-root。這是實際檔案預檢，未注入假資料或替代 helper。拒絕後依停止條件結束，未再開 GUI，也未重測前次通過項目。

以下 `E = docs/reports/issue-414/`、`A = E/attempt-2/`、`P = E/current-preflight/`；本次完整命令、UTC、run-root 與事件見 `P/preflight.json`。前兩次報告可由 `092ddc0c`、`cd21cdbf` 讀取。

| 編號 | 結果 | 實際觀察（現象） | 重現步驟 | 證據路徑 |
|---|---|---|---|---|
| 01 | ✅ 沿用 | 原建置通過；本次必要登入器建置亦成功。 | 沿用；本次 `dotnet build -c Release --no-restore`。 | `E/artifact-hashes.json`、`P/launcher-build.txt`、`P/artifacts.json` |
| 02 | ✅ 沿用 | 實驗設定沿用，本次未啟伺服器。 | 沿用既有設定檢查。 | `E/config-public.properties`、`E/combat.properties` |
| 03 | ✅ 沿用 | 前次冷備份 1,722 檔相同；本次無 DB 啟動或寫入。 | 沿用冷備份，本次無需另備份。 | `E/p24-backup.json` |
| 04 | ✅ 沿用 | 17 表／731 中文欄位值差異 0，本次無 DDL。 | 沿用 #284 第 5 輪。 | `E/inherited/retest-equipment-migration-plan.json`、`E/inherited/retest-equipment-data-after.tsv` |
| 05 | ✅ 沿用 | profile：id=1、P24LiToExp、p24_accounts_lito_exp。 | 複用既有實查 profile，本次不連 DB。 | `E/source-server.tsv`、`A/external-profile.json` |
| 06 | ✅ 沿用 | 首次執行已證實 `84BA0CDE…EDCC` JAR ready。 | 沿用既有啟動結果，本次未啟 Java。 | `E/server-console.log`、`E/server-owned.json` |
| 07 | ✅ 沿用／主控裁定 | 四個 loopback 端點為正確設計，既有 m4 PASS。 | 依上述主控裁定沿用。 | `E/listen-check.txt`、`E/listen-task-comparison.json` |
| 08 | ❌ 重測預檢 | 真實 helper 在 check_source 拒絕 TW13081901.txt，未進大廳。 | 執行 `P/preflight.json` 的 command，拒絕後停止。 | `P/preflight.json`、`P/helper-stdout.jsonl` |
| 09 | ✅ 沿用 | 前次原三庫 270 表 checksum 相同；本次未連 DB。 | 沿用資料保護檢查，本次無新增還原 SQL。 | `E/data-protection.json`、`E/original-checksum-before.tsv`、`E/original-checksum-after.tsv` |
| 10 | ✅ 沿用 | 前次自有程序已清理；本次 helper 已退出，未啟 Java／P24／GUI／遊戲。 | 確認 subprocess 結束，觀察實驗埠。 | `E/app-cleanup.json`、`P/preflight.json`、`P/experiment-listeners.json` |

## 已驗證

- `TW13081901.txt` 實際 SHA-256 仍為 `28C4FC600833FB8C609AB195A67FF926E8FFA571100D8D4B389A23BE8C2C8DB8`；預期仍為 `7EBE2B464C8B1EA9C58AC985DD8FF4C823784D8231894262EDDCD7691CA94008`。
- 真實 helper 回傳 `failed(stage=check_source, writes=0)` 及 `finished(cleanup_verified=true, accepted=false)`；沒有進入端點檢查或建立遊戲程序。未使用 `--legacy-formal-mode`、覆寫客戶端或變更釘選。
- 依 T140 範圍對客戶端頂層檔案及 `LiTo/` 全部檔案做前後快照，其他子目錄不遞迴。前後均為 **669 檔／9,607,667,880 bytes，路徑、大小、mtime、SHA 差異 0**。`lito381-compat.log` **0→0 bytes**；`LiToError.txt` **16,095→16,095 bytes**。詳細結果見 `P/preflight.json`，完整快照保留在該檔所列本機 run-root。
- 原始專案 `app/gui-runs`、`app/runs` 前後目錄清單完全相同，沒有新增目錄；清單保留在同一 run-root。沒有寫入原始專案。
- lease 見 `P/lease-observation.json`：holder 為本張，API exp 換算為 `2026-11-14T05:16:42.945826Z`，與派工時間相符。僅讀取，未操作調度器。
- 本次未啟實驗 DB 或寫 DB，無須新增冷備份、筆數／checksum 查詢或還原 SQL。未修改正式環境、原 BIN、封存、硬連結或系統設定。

## 未驗證／給下一棒的提示

08 的選服、帳號登入、角色選擇、大廳與 PNG 仍未驗證。本次未開 GUI，因此沒有新增 GUI 的 `request.json`／`terminal.json`，也沒有 `server-ready.json`／`native-created.json`。本次 helper 事件不是成功收據；前次 GUI 拒絕收據仍保留在 A。

交主控先落實方案 B 的程式修正與資源交付，再派只重測 08；不要因 #443 已合併就認定資源問題已解決。#443 僅修正拒絕時清理事件。#445 目前已關閉（not_planned），後續任務須明確承接重產 `common_catalog.bin`／manifest、同步釘選與 helper SHA，以及外部入口驗收的交付位置。

產出為本報告及 P；一次性腳本在 `scratch/issue-414/attempt3.py`。未修產品程式、未刪除 worktree 或其中任何目錄。前次 GUI 截圖逾時也未在本次驗證修復。
