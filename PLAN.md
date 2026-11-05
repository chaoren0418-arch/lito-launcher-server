# PLAN — lito-launcher-server

更新：2026-11-06（範圍凍結，Issue #231）。本檔由 Claude 維護；任務細目、凍結的完成定義與主線清單在 `TASKS.md`。

## 一句話目標
交付給廠商一套**可重現建置、可安裝、可驗收**的 LiTo 登入器 + 伺服器：玩家能登入、進大廳、進房、戰鬥、結算、正常退出；技能行為與原版一致；GM 功能可用。

## 現況（事實）
| 項目 | 狀態 |
|---|---|
| 登入器 | **0.6.71**（`docs/maintenance/release-0.6.71.json` published-and-verified，2026-10-23）；.NET 10 WinForms，win-x86 self-contained |
| 伺服器 | **0.6.71**（0.6.70 核心 + L1PolyMe）；正式 JAR 放在 `lito登入器/LiTo381-Launcher/app/server/LiTo381-SharedResources-candidate.jar` |
| 聯合基準 | `backups/baseline-0.6.66.zip`（僅本機） |
| 協定覆蓋 | C→S 157/157 可達已處理；S→C 167 已發送；LiTo 擴充 49/49 — **靜態盤點，不等於實測** |
| 戰鬥技能同源化 | 大量隔離候選（最新 `50635f7e…`，2,400 案／5,164 組合）**全部未部署**；開放缺口見 TASKS M2 |
| 建置可重現性 | 進行中：`Build.ps1` 已去除硬編路徑（T003），pom 已改為 0.6.71（T007）。`src/main` 對齊正式 0.6.71 分 A01–A06 暫存，A03–A06 已達 0 DIFF；A-land（#78）完成後，`mvn package` 即可直接產出等價 JAR |
| CI | GitHub Actions：Maven compile（補 `SharedDatabase.java`）+ 登入器 dotnet build |
| 安全 | `config/server.properties` 曾含明文 DB 密碼進 git（已移除，需換密碼） |

## 真正的缺口
1. **伺服器無法從原始碼一鍵建出正式 JAR**。現在的 JAR 是「base-0.6.70.jar + 一疊 patch 工具」，沒人能在乾淨機器重現。對廠商交付這是致命的。
2. 戰鬥候選與正式版脫鉤：候選是獨立 JAR，正式 0.6.71 的資源／圖示／DLL／GM7200 要「隔離合成」才能合併。
3. 版本號三處不一致（0.6.69 / 0.6.70 / 0.6.71），文件與 csproj 互相矛盾。
4. 測試有 1,331 個檔，但有 DB 依賴的 `Legacy*IntegrationTest` 沒有 assume 守門，`mvn test` 在無 DB 環境會紅。

## 里程碑（完成定義凍結於 `TASKS.md`）
進度只計 `## M*` 區段；新發現的工作一律進 Backlog（詳見 `TASKS.md` 開頭的規則）。

### M0 從原始碼建出正式版，並啟動到大廳（主線 9 項，封頂）
使用者打開 #225，會看到：`mvn test` 全綠、`src/main` 建出的 JAR 對正式版語意 DIFF=0，以及用這顆 JAR 在實驗環境（3307）啟動的伺服器、登入器、大廳三張截圖。
依賴鏈：A01／A02／A03／A06 ＋ 比較器 #203 → #216 → A-land #78 ─┐
　　　　實驗 MySQL #83 ────────────────────────────────┴→ T050 #225

### M1 正式版實機回歸
使用者打開 `docs/acceptance/0.6.71.md`，會看到：在實驗環境完成登入→大廳→進房→戰鬥→結算→退出，每一步都有截圖，並附 `character_lito` 結算前後的對照。

### M2 戰鬥技能同源化
B-land 後，Maven 產物對候選 `50635f7e…` 語意 DIFF=0；既有戰鬥回歸（2,400 案、5,164 組合、curse-clock 24 案）在 Maven 產物上全部一致；附實驗環境的指定技能戰鬥截圖。B01–B09 目前在 Backlog，M1 完成後封頂升格。

### M3 交付
一個 `release/package-0.6.x.zip`，依 `docs/INSTALL.md` 在乾淨環境安裝並啟動到大廳（截圖），並打 tag。

## 分工
Claude（雲端主控）：PLAN／TASKS、裁定、審查。本機執行器（codex-local）：實作與驗證。dot：本機資源與實驗環境、截圖驗收。使用者：驗收與觀測，並對 DB 授權、範圍變更做決定。

## 停用的舊流程
總控任務板、監製索引、對話 ID、`工作佇列.json`／`監製進度.json`、每輪時間戳 JSON。
