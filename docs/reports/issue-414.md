# Issue #414：T141 第 2 次執行，只重測 08

**結論：部分通過（✅ 9／❌ 1／⚠️ 0）。** 2026-11-14。08 仍被 `TW13081901.txt` 指紋核對拒絕，沒有大廳 PNG；#443 的失敗清理收據修正已由新版 GUI 實測確認。其餘結果沿用，07 依主控裁定改為通過。

## 做了什麼／怎麼驗證

已讀 `AGENTS.md`、`docs/REPORT.md`、外部入口驗收文件與 #443 報告。在既有 `task/414` 合併最新 `origin/main`（`fe7b78246e07e1908141c690203baf0af1de2999`），沒有 checkout／另建分支。依 [主控重新開啟裁定](https://github.com/chaoren0418-arch/lito-launcher-server/issues/414#issuecomment-6870813965)，**本次只重測 08**；07 的四端點是正確設計，其餘沿用前次。前次完整報告可由 commit `092ddc0c422259f1f4909958e7a996628f5c1c7f` 讀取，原公開證據仍保留。

以下 `E = docs/reports/issue-414/`、`A = E/attempt-2/`；本次 `R = scratch/t014/run-20261114T035218Z/`。

| 編號 | 結果 | 實際觀察（現象） | 重現步驟 | 證據路徑 |
|---|---|---|---|---|
| 01 | ✅ 沿用 | 前次建置通過；本次僅為 08 另建登入器，0 警告／0 錯誤。 | 沿用前次；本次 `dotnet build lito登入器/LiTo381-Launcher/src/LiTo381Launcher.csproj -c Release`。 | `E/artifact-hashes.json`、`A/launcher-build.txt`、`A/launcher-artifacts.json` |
| 02 | ✅ 沿用 | 實驗設定及 combat 設定沿用，本次未啟動伺服器。 | 沿用前次設定檢查。 | `E/config-public.properties`、`E/combat.properties` |
| 03 | ✅ 沿用 | 前次冷備份 1,722 檔逐檔相同；本次無 DB 啟動或寫入，未另備份。 | 沿用前次冷備份結果。 | `E/p24-backup.json` |
| 04 | ✅ 沿用 | 17 表／731 中文欄位值差異 0；本次無 DDL。 | 沿用 #284 第 5 輪結果。 | `E/inherited/retest-equipment-migration-plan.json`、`E/inherited/retest-equipment-data-after.tsv` |
| 05 | ✅ 沿用 | 沿用前次實查 profile：id=1、P24LiToExp、p24_accounts_lito_exp。 | 複用已填 profile；本次未再查 DB。 | `E/source-server.tsv`、`A/external-profile.json` |
| 06 | ✅ 沿用 | 前次 `84BA0CDE…EDCC` JAR 已 ready；本次只驗證資源拒絕，未啟 Java。 | 沿用前次實際啟動結果。 | `E/server-console.log`、`E/server-owned.json` |
| 07 | ✅ 沿用／主控裁定 | 四個 loopback 端點為正確設計；前次 m4 PASS，本次未重測監聽。 | 依主控留言接受前次實測。 | 上述裁定連結；`E/listen-check.txt`、`E/listen-task-comparison.json` |
| 08 | ❌ 重測 | 新版 GUI 在 check_source 拒絕 TW13081901.txt；清理已確認，但未選服／登入／大廳。 | 以 external profile、200%、worktree run-root 開啟新 EXE，按 Enter 一次。 | `A/request.json`、`A/terminal.json`、`A/launcher-result.txt`、`A/integrity.json` |
| 09 | ✅ 沿用 | 前次原三庫 270 表 checksum 相同；本次無 DB 連線，沒有重做 checksum。 | 沿用前次資料保護結果。 | `E/data-protection.json`、`E/original-checksum-before.tsv`、`E/original-checksum-after.tsv` |
| 10 | ✅ 沿用；本次另附清理 | 前次自有程序已清理；本次兩次登入器程序均依身分核對關閉，無自有 Java／P24／遊戲。 | PID＋執行檔＋建立時間一致後 Stop-Process，確認 PID 消失。 | `E/app-cleanup.json`、`A/hidden-launcher-cleaned.json`、`A/process-cleanup.json`、`A/ports-after.json` |

## 已驗證

- 本次 lease 的 holder 為 `chaoren0418-arch/lito-launcher-server#414`、到期 `2026-11-14T04:51:07Z`（API 原值見 `A/lease-observation.json`），與派工一致。未修改 lease 或調度器。
- 新版 helper SHA-256 為 `7E9EE3D832C594E4F31D8FF4CAFD4CF1600B6A4978CF392F2569BED09D547C2F`，正式 helper 仍為 `ADFF1EDC784B6F54803A050D32E3D6D3EE06EB9242DA0AF1B4B227DCB3EAC9CD`。驗收文件尚列 T139 舊 helper SHA；本次依 #443 已合併程式與報告核對，沒有手改釘選。
- 使用 `LITO_LAUNCHER_SETTINGS_DIRECTORY=R/launcher-settings`、`LITO_LAUNCHER_RUN_ROOT=R/launcher-runs`，參數 `--external-server-profile R/external-profile.json`，未使用 `--legacy-formal-mode`。GUI 可讀文字確認外部模式、200%、一般無界擂台及 R 收據位置。
- `TW13081901.txt` 實際 SHA-256 仍為 `28C4FC600833FB8C609AB195A67FF926E8FFA571100D8D4B389A23BE8C2C8DB8`；正式釘選仍為 `7EBE2B464C8B1EA9C58AC985DD8FF4C823784D8231894262EDDCD7691CA94008`。#443 沒有解決此版本選擇，只修復拒絕事件的收尾。
- GUI 實際顯示 `Startup file changed: D:\lineage開發\0-客戶端\TW13081901.txt`，失敗 stage 為 `check_source`、`writes=0`。新 terminal 為 `cleanup_qualified=true`、`ownership_retained=false`、`helper_result.cleanup_verified=true`、`accepted=false`；沒有再顯示「清理尚未確認」。這確認拒絕流程修正，**不代表 08 通過**。
- 收據位於 `R/launcher-runs/gui-runs/external-031b358819c34224ab0cb7a19d54e798/`，包含 request 與 terminal；沒有 server-ready、native-created 或大廳 PNG。資源核對在連線檢查之前即拒絕，依停止條件不再試啟、不覆寫客戶端、不變更釘選。
- 本次只啟動登入器：首次 hidden PID 16304 沒有可操作視窗，核對身分後關閉；改以可見 GUI 啟動 PID 18420，操作一次後亦關閉。P24、Java、LiTo.bin 均未由本次啟動，未連 DB、未取得或寫入私密設定，無需產生新的還原 SQL。06／07／09 不因必要前置而重新執行。
- 客戶端快照沿用 T140 範圍：頂層檔案＋`LiTo/` 全部子檔；其他子目錄不遞迴。前後 **669 檔／9,607,667,880 bytes**，路徑、大小、mtime、SHA 差異 **0**。兩個例外日誌亦未變：`lito381-compat.log` **0→0 bytes**；`LiToError.txt` **16,095→16,095 bytes**。原始專案 `app/gui-runs`、`app/runs` 目錄清單完全相同。
- 截圖第一次回 `FrameArrived timed out: timed out waiting on channel`；重新取得視窗後重試回 `window capture timed out: timed out waiting on channel`。已保存文字樹及原始錯誤；沒有用文字圖冒充 PNG。

## 未驗證／給下一棒的提示

08 的選服、測試帳號登入、角色選擇、大廳及成功收據仍未驗證。主控應先處理 [#443 報告](issue-443.md) 的客戶端版本裁定與對應修正，再派只重測 08；本次未取得任何覆寫客戶端或修改正式釘選的額外授權。執行環境維護者另處理截圖逾時，否則後續仍無法提供 PNG。

只修改本報告及公開測試證據，不修產品程式、不改 `docs/REPORT.md`。未接觸正式 2000／3306、原 BIN、原始專案寫入、封存、硬連結或系統設定；未刪除 worktree 或任何目錄。產出為本報告與 `A`；本機原始收據在 R，一次性腳本在 `scratch/issue-414/`。
