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
3. ~~版本號三處不一致~~：已由 T004 統一為 0.6.71。
4. ~~DB 依賴測試沒有守門~~：已由 T005／T009 加上守門，Server tests 改為阻斷式。
5. 實機驗證需要不碰正式服務的實驗環境：P24Lab 或 `environments/` 快照（#83）。

## 盤點：已知範圍（2026-11-06，Issue #231 步驟 0）

**讀過的資料**（本 repo；tw-bin／重建版是其他 repo，不在本專案範圍）：
- 伺服器 `docs/HISTORY.md`（0.6.26～0.6.71 的脈絡、回復點、候選表）
- `docs/maintenance/`：86 份，其中最主要的兩份是 `combat-continuation-current`（戰鬥主進度表）與 `combat-closeout-current`（12 項收尾矩陣，候選 `046c58c5…`）
- `tools/` 下 42 個項目，其中 17 層 `build.py` patch 鏈；`tools/combat-continuation/` 下 154 個項目（隔離候選與回歸）
- `docs/acceptance/CHECKLIST.md`
- `docs/design/T020-combat-merge.md`、`docs/combat-merge/{a-batches,batches}.json`
- 全部 190 張 Issue 的狀態（2026-11-05 20:46 UTC 以 GitHub API 查詢）

### M0 從原始碼建出正式版，並啟動到大廳
**已知事項**（共 12 項，對應 TASKS 主線）：
- A01–A06 暫存對齊正式（692 entries）
- 比較器工具 #203、#216、#235
- A-land #78：搬進 `src/main` 並補 7 項單元測試
- 實驗 DB #83
- 啟動到大廳截圖 #225
- 17 層 `build.py` 的改動已由 A 階段吸收：A 範圍等於正式 JAR 中由 `src/main` 負責的類別；`tools/*/build.py` 在 A-land 後只剩驗證用途

**不確定項**：
- **U0-1**：A01、A02、A-land 驗收時又遇到比較器表達不了的形狀，估 **2 張**（已兌現 1 張：#235）。
- **U0-2**：A-land 後既有單元測試有失敗（src/main 從舊版換成正式版行為），估 **1–2 張**修正。
- **U0-3**：#83 在 P24Lab 與 `environments/` 都找不到可用資料。這時要交使用者決定，估 **1 張**；逾時預設為「M0 第 3 點改以 `--check`／`--smoke` 輸出取代截圖」，但這屬於修改完成定義，須另開 Issue 並標 `needs:user`。
- **U0-4**：登入器要連實驗伺服器（不同埠或來源設定），可能需要調整實驗設定，估 **1 張**。

### M1 正式版實機回歸（實驗環境）
**已知事項**：
- CHECKLIST 改成實驗環境版（T013）
- 依表跑一次（T011）

**不確定項**：
- **U1-1**：跑表時發現 bug，估 **3 張**。參考：結算職業圖示、Ctrl 雙視窗、GM 魔杖、斷線結算都從未有人工回報。

### M2 戰鬥技能同源化
**已知事項**：
- B01–B09（1,384 entries；B02、B05 已完成）
- B-land #138
- T020-close #139
- 8d26 基準回歸 #84
- #31 收尾

**不確定項**：
- **U2-1**：B 階段比較器缺口，估 **2 張**。
- **U2-2**：B-land 後的測試失敗修正，估 **2 張**。
- **U2-3**：候選 `046c58c5…`（收尾開發，比 50635f7e 新）要不要再併一輪。估 **0 或 5 張**，在 M2 封頂時與使用者確認。

### M3 交付
**已知事項**：
- T040（done）
- T060 打包改用 Maven 產物
- T061 乾淨目錄安裝到大廳
- T062 tag

**不確定項**：
- **U3-1**：乾淨安裝發現缺檔或缺路徑，估 **1 張**。

### 不在 M0–M3 的範圍（M4，未立）
T021–T032 與收尾矩陣 12 項，屬「Source 全技能對齊」的長期開發。M3 交付後，與使用者／廠商確認後再決定。

## 里程碑（完成定義凍結於 `TASKS.md`）
進度只計算 `## M*` 區段；新工作進 Backlog，除非是上面不確定項的兌現，而且要在 PR 中引用估計。

### M0 從原始碼建出正式版，並啟動到大廳（主線 12 項，封頂）
使用者打開 #225 會看到：`mvn test` 全綠；`src/main` 建出的 JAR 對正式版語意比對 DIFF=0；以及用這顆 JAR 在實驗環境（P24Lab 或隔離 DB，不碰正式 3306）拍的伺服器、登入器、大廳三張截圖。

依賴鏈：
- A01／A02 ＋ 比較器 #216 → #235 → A-land #78 ─┐
- 實驗 DB #83 ─────────────────────────────────┴→ T050 #225

### M1 正式版實機回歸
使用者打開 `docs/acceptance/0.6.71.md` 會看到：在實驗環境完成登入→大廳→進房→戰鬥→結算→退出，每一步都有截圖，並附 `character_lito` 的前後對照。

### M2 戰鬥技能同源化
B-land 之後，Maven 產物對候選 `50635f7e…` 語意比對 DIFF=0；既有戰鬥回歸在 Maven 產物上全部一致；附 5 個指定技能在實驗環境的戰鬥截圖。

### M3 交付
一個 `release/package-0.6.x.zip`；依 `docs/INSTALL.md` 在乾淨目錄安裝並啟動到大廳（附截圖）；打 tag。

## 分工
Claude（雲端主控）：PLAN／TASKS、裁定、審查。本機執行器（codex-local）：實作與驗證。dot：本機資源與實驗環境（不碰正式 3306）、截圖。使用者：驗收與觀測，並對 DB 授權、範圍變更做決定。

## 停用的舊流程
總控任務板、監製索引、對話 ID、`工作佇列.json`／`監製進度.json`、每輪時間戳 JSON。
