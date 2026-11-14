# Issue #449：T154，只重測 08（#448 後）

**結論：部分通過（✅ 9／❌ 1／⚠️ 0）。** 九項沿用 #414；08 實際 helper 拒絕 `shared Tile index changed; re-export and qualify resources`，沒有大廳 PNG。另有桌面操作／截圖工具故障，不能把登入器文字畫面當成大廳驗收。

## 做了什麼／怎麼驗證

2026-11-14 13:37–13:47（Asia/Taipei），在既有 `task/449` worktree 執行。已讀 `AGENTS.md`、`docs/REPORT.md`、外部入口驗收文件及指定 #414／#413 證據；未重測其他驗收項目、未修產品、未改 `docs/REPORT.md`。fetch 後 HEAD 與 `origin/main` 同為 `a9bf83696be91b4792ba5aae55ad57b59a90b954`，已含 #448，無須合併或切換分支。

以 Maven package（跳過測試）及 dotnet Release 從本 worktree 重建；#83 隔離設定產生於本次 scratch，使用 `127.0.0.1:43324`、`*_lito_exp`，啟動本次 JAR 到 ready。以 #414 的原 profile 開啟 `LiTo381Launcher.exe --external-server-profile <profile>`，未加 `--legacy-formal-mode`，200% 縮放、一般無界擂台。

以下 `E = docs/reports/issue-449/`；沿用證據見 [task/414 報告與附件](https://github.com/chaoren0418-arch/lito-launcher-server/blob/task/414/docs/reports/issue-414.md)。

| 編號 | 結果 | 實際觀察（現象） | 重現步驟 | 證據路徑 |
|---|---|---|---|---|
| 01 | ✅ 沿用 | 本次必要的 JAR／登入器重建也成功，helper SHA 符合 #448。 | Maven package、dotnet build Release。 | `E/build-summary.json`、`E/server-build.txt`、`E/launcher-build.txt` |
| 02 | ✅ 沿用 | #83 隔離設定沿用；本次僅產生實驗設定副本。 | 使用本次 `config-public.properties` 對照隔離端點。 | `E/config-public.properties`、`E/combat.properties` |
| 03 | ✅ 沿用 | 本次啟動前另完成 P24 全資料目錄冷備份，1,722 檔 SHA 全相同。 | 停機時複製至本 worktree scratch 並逐檔比對。 | `E/p24-backup.json` |
| 04 | ✅ 沿用 | 17 表／731 中文值差異 0 的既有結果沿用；本次無 DDL。 | 不重測資料移轉。 | #414 `inherited/retest-equipment-migration-plan.json` |
| 05 | ✅ 沿用 | 沿用 id=1、P24LiToExp、p24_accounts_lito_exp 的實查 profile。 | 直接複用 #414 `attempt-2/external-profile.json`。 | `E/external-profile.json` |
| 06 | ✅ 沿用 | 本次新建 JAR 亦顯示 ready，Java PID 16628。 | 於隔離 run 目錄執行 JAR 及 experiment.properties。 | `E/server-console.log`、`E/server-owned.json` |
| 07 | ✅ 沿用／主控裁定 | 四個 loopback 端點為正確設計，不重跑驗收。 | 沿用 #414 主控裁定；本次僅觀察啟動所需端點。 | #414 `listen-check.txt`；本次 `E/server-console.log` |
| 08 | ❌ 重測 | GUI 正確進入外部模式，但操作／截圖工具失效；真實 helper 進一步確認 Tile.idx 指紋拒絕，未啟遊戲、未登入、無大廳 PNG。 | 依外部入口文件開 GUI；再執行 `E/helper-execution.json` 的原版 helper 命令重現預檢拒絕。 | `E/launcher-before.txt`、`E/gui-errors.json`、`E/helper-stdout.jsonl`、`E/tile-mismatch.json` |
| 09 | ✅ 沿用 | 原三庫保護結果沿用；本次額外保存 270 張實驗表前後 checksum，全部相同。 | 僅連 43324，對實驗庫做前後唯讀快照；沒有執行還原。 | `E/database-comparison.json`、`E/experiment-checksum-before.tsv`、`E/experiment-checksum-after.tsv` |
| 10 | ✅ 沿用 | 本次登入器、Java、P24 MySQL 父子程序亦全部退出，實驗埠皆釋放。 | 核對自有 PID／路徑／起始時間後停止；P24 核對身份後 SQL SHUTDOWN。 | `E/app-cleanup.json`、`E/p24-shutdown.json` |

## 已驗證

- JAR SHA-256：`84BA0CDE7ABACE28456232B86922B22DD7FBFF98E8B44E8B7BEC5F995A79EDCC`；登入器 EXE：`C3EAD30D13A662D7F85436B507BAA8577001A76243023470451D4334D471E5C2`。DLL 與兩支 helper 的完整 SHA 見 `E/build-summary.json`。新建 JAR 與前次 SHA 相同，不是拿舊 JAR 冒充重建。
- #448 的 `TW13081901.txt` 不再造成拒絕。`check_source` 已通過 PINS、UNPINNED 與 BIN 檢查，接著在正式交付 `app/world_resource_preflight.py:25` 拒絕。`effectlist2.xml.extraction.json` 的 `archiveIndexSha256` 預期 `D35A7E3B5B9D4BB432E7B8DB3FF5D9B0A79EB638672C5E025B900F9DE53B0BE6`，實際 `0-客戶端/Tile.idx` 為 `9291FD800AC8CAE6DC355C1D8BBB965880561DE169FBC7EA22D174FFF9AD24DB`（152,492 bytes）。
- 原版 helper CLI 結束碼 1，事件為 `failed(stage=check_source,writes=0)`、`finished(cleanup_verified=true,accepted=false,product_complete=false)`。stdin 預送 stop 僅避免診斷意外通過後啟遊戲；本次在讀取 stdin 的階段之前就拒絕。沒有修改 helper、替代依賴或注入假資料。
- GUI 工具 `sky` 與 `cua` 均回報 `coordinate input geometry is unavailable`；截圖分別 `FrameArrived timed out`／`window capture timed out`。可取得外部模式的無障礙文字，但沒有成功按下啟動、沒有 GUI session 收據或可交付 PNG；詳細觀察見 `E/gui-errors.json`。
- 依 #413 範圍：客戶端頂層全部檔案（含隱藏檔）及 `LiTo/` 全子樹，其他子目錄不遞迴。前後各 **669 檔／9,607,667,880 bytes**；與 #413 比較路徑、大小、mtime、SHA 差異 **0**；本次前後以奈秒 mtime 比較亦差異 **0**。`lito381-compat.log` 0→0 bytes，`LiToError.txt` 16,095→16,095 bytes。見 `E/client-comparison.json` 與完整前後快照。#413 時間欄位按其 `datetime.fromtimestamp(stat.st_mtime)` 格式比對，避免將格式精度差當成檔案異動。
- 保存的 API lease 證據有效：holder `chaoren0418-arch/lito-launcher-server#449`，exp `2026-11-14T06:37:42.259692Z`，符合派工的秒級到期時間。最初 PowerShell 顯示未取得 lease，後續實存 JSON 已確認，無須變更調度器。
- 冷備份為 279,504,637 bytes；本次沒有 DDL／DML，270 張實驗表 checksum 與 `character_lito` 還原 SQL 前後相同。還原 SQL 附於 `E/character-lito-restore.sql`，未執行；完整冷備份及逐檔清冊留於本次 scratch。P24 未附帶啟動 42061 遊戲服，該埠始終空閒。
- 本次自有程序：初次隱藏登入器 PID 4752 已停止，改以可見 GUI PID 6572；Java PID 16628；P24 PID 2944／17868。全部退出，沒有 LiTo.bin 程序；42061／42083／42084／43324 的剩餘 listener 為空。未連正式 3306、未啟停正式 2000、未改原 BIN／客戶端／正式交付／封存／硬連結／系統設定。

## 推測

Tile.idx 與 #413 基準相同，而正式 effectlist2 抽取收據期待另一個 SHA，推測是正式交付的資源收據與既有客戶端版本未同步；本次只證實不一致，不裁定要改哪一邊。修正這一層後是否還有 icon 等後續資源檢查阻擋，未知。

## 待驗證／給下一棒的提示

交主控分派資源／程式任務處理 Tile.idx 與正式 world 資源收據的版本一致性，並由本機工具維護方處理視窗操作與截圖逾時；本張 test 不自行放寬釘選或修程式。完成後**只重測 08**：選服 → #83 帳號登入 → 選角 → 大廳。01–07、09、10 繼續沿用。

預期截圖路徑 `docs/acceptance/m4-84ba0cde/08-lobby.png` **未產生**；不可用文字樹或預檢通過代替。其餘後續資源檢查、遊戲畫面、登入協定與帳號登入均未驗證。產出是本報告與 `E`；一次性腳本、私有隔離設定及備份位於 `scratch/issue-449/`，未入 git。未刪除 worktree 或任何目錄。
