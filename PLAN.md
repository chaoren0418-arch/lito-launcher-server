# PLAN — lito-launcher-server

更新：2026-11-04。本檔由 Claude 維護；任務細目在 `TASKS.md`。

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
| 建置可重現性 | **差**：`Build.ps1` 硬編 `C:\Users\CHAOREN\...\python.exe`；`Build-Server.ps1` 只做檢查、不產 JAR；正式 JAR 由 `tools/*/build.py` 逐層 patch 疊出來，pom 版本 0.7.864 與 0.6.x 無對應 |
| CI | GitHub Actions：Maven compile（補 `SharedDatabase.java`）+ 登入器 dotnet build |
| 安全 | `config/server.properties` 曾含明文 DB 密碼進 git（已移除，需換密碼） |

## 真正的缺口
1. **伺服器無法從原始碼一鍵建出正式 JAR**。現在的 JAR 是「base-0.6.70.jar + 一疊 patch 工具」，沒人能在乾淨機器重現。對廠商交付這是致命的。
2. 戰鬥候選與正式版脫鉤：候選是獨立 JAR，正式 0.6.71 的資源／圖示／DLL／GM7200 要「隔離合成」才能合併。
3. 版本號三處不一致（0.6.69 / 0.6.70 / 0.6.71），文件與 csproj 互相矛盾。
4. 測試有 1,331 個檔，但有 DB 依賴的 `Legacy*IntegrationTest` 沒有 assume 守門，`mvn test` 在無 DB 環境會紅。

## 里程碑
### M0 可重現建置（本週）
- CI 綠燈（T001）。
- 伺服器：讓 `mvn package` 直接產出等價於正式 0.6.71 的 JAR（把 patch 工具的改動併回 `src/main`，或把 patch 流程寫進 Maven 階段）；`pom.xml` 版本改 0.6.71（T002）。
- 登入器：`Build.ps1` 去硬編路徑（T003）。
- 統一版本號與文件（T004）。
- 真實設定檔不進 git，`server.properties.example` 為範本（已做）。

### M1 正式版實機回歸（使用者操作）
- 用 M0 的建置跑：啟動伺服器 `--check`／`--smoke` → 登入器登入 → 大廳 → 進房 → 一場戰鬥 → 結算寫入 `character_lito` → 退出。
- 結果寫 `docs/acceptance/0.6.71.md`。這是把「published-and-verified」變成有紀錄的基線。

### M2 戰鬥技能同源化收斂
- 先決定合併策略：把候選 `50635f7e…` 的來源差異併進 `src/main`（而非再疊一層 patch）。
- 依 TASKS M2 逐項收斂 12 組缺口；每組一個 PR，CI + 既有案例通過才合。
- 完成後做同 SHA 全回歸，再進 M1 流程實測一次。

### M3 交付
- 安裝包：伺服器 JAR + `db/*.sql` + `server.properties.example` + 登入器 release；一份安裝手冊。
- tag `launcher-v0.6.x` / `server-v0.6.x`。

## 分工
Claude：PLAN／TASKS、審 PR、CI、合併策略。Cursor：實作。ChatGPT：第二意見與測試案例。使用者：實機測試、DB、合併 PR、與廠商確認範圍。

## 停用的舊流程
總控任務板、監製索引、對話 ID、`工作佇列.json`／`監製進度.json`、每輪時間戳 JSON。
