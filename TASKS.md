# TASKS — lito-launcher-server

格式：`ID | 狀態 | 負責 | 範圍 | 驗收條件`。狀態：todo / doing / review / done / blocked。
PR 標題用 `ID: 摘要`。

## 規則（2026-11-06 範圍凍結，Issue #231）
- **進度只算 `## M*` 區段**；`## Backlog` 另計數量，不算進度。
- 每個里程碑的「**完成定義**」寫好就凍結。要修改必須另開 Issue 說明理由，並標 `needs:user`。
- **主線封頂**：最近一個未完成里程碑的剩餘任務一次列完，標題寫「主線 N 項，封頂」。之後新發現的工作一律放進 Backlog；要升格為主線，必須同時刪掉一項主線，或取得使用者同意。
- 比較器（semantic-diff）的工具缺口，併進同一張進行中的工具任務，不另開主線項目。

---

## M0 從原始碼建出正式版，並啟動到大廳（主線 9 項，封頂）

**完成定義（凍結）**：使用者打開 Issue #225，會看到：
1. 一行指令 `mvn -f lito伺服器/LiTo381-Server/pom.xml -B test` 的輸出，結果為 `Failures: 0, Errors: 0`。
2. `src/main` 用 `mvn package` 建出的 `LiTo381-Server.jar` 對正式 `reference/base-0.6.71.jar` 的語意比對摘要：A 範圍 692 個 entry，**DIFF=0**（核准的偏離另行列出）。
3. 三張截圖，都是用上述 JAR 在實驗環境（實驗 MySQL 127.0.0.1:3307，不碰正式服務）拍的：伺服器主控台啟動完成、登入器連線、遊戲**大廳**。
4. 一份使用者可自己重跑的指令清單。

| # | ID | Issue | 內容 | 狀態 |
|---|---|---|---|---|
| 1 | T020-A01 | #72 | src/main 對齊正式（nativede，49 類）暫存 | doing（等 #203） |
| 2 | T020-A02 | #73 | 同上（nativede，49 類） | doing |
| 3 | T020-A03 | #74 | 同上（nativede，47 類）：0 DIFF，review:pass | review（待合併） |
| 4 | T020-A06 | #77 | 同上（other，27 類）：0 DIFF，review:pass | review（待合併） |
| 5 | T020i-8 | #203 | 比較器：依呼叫點配對 lambda、不看分支方向的 CFG 比對、可見性 bridge | doing |
| 6 | T020i-9 | #216 | 比較器：單一屬性 metadata 豁免、純轉拋 handler、public 虛擬呼叫 owner | todo（等 #203） |
| 7 | T020-A-land | #78 | A 暫存併入 src/main、補單元測試、`mvn test` 綠、A 範圍語意 DIFF=0 | todo（等 1–6） |
| 8 | T020c-2 | #83 | 實驗 MySQL（從正式庫唯讀匯出 → 獨立 3307 實例）與實驗設定 | doing（dot） |
| 9 | T050 | #225 | 實驗環境啟動到大廳＋截圖（dot） | todo（等 7、8） |

已完成、計入 M0：A04（#75）、A05（#76）；工具 #102、#108、#132、#134、#135、#153、#166、#174、#176、#186；以及下列早期項目。

<details><summary>M0 早期已完成項目（T001–T012、T040）</summary>

- T001 done：CI（Maven compile＋登入器 dotnet build）。
- T002 done（第一階段）：`src/main` 補齊缺漏類別、pom 0.6.71、Main-Class。**等價 JAR** 由本里程碑的 A-land（#78）完成。
- T003 done（Issue #4，PR #8）：`Build.ps1` 去除硬編路徑。
- T004 done（Issue #5）：版本號與文件統一為 0.6.71。
- T005 done（Issue #3；由 #12/#15、#20/#22 達成）：DB 依賴測試加守門，Server tests 改為阻斷式。
- T006 done：真實 `server.properties` 不進 git。
- T007 done（Issue #2，PR #7）：CI 比對類別清單，`docs/server-source-gap.md`。
- T008 done（否定結果，Issue #6）：evidence.zip 中沒有 266 個 DECOMPILED 類別的原始碼。
- T009 done（Issue #12，PR #15）：本機資源依賴測試加守門。
- T012 done（Issue #37，PR #39）：修正不穩定測試。
- T040 done（Issue #32，PR #33）：安裝手冊＋打包腳本（歸 M3 交付，先行完成）。
</details>

## M1 正式版實機回歸（M0 完成後封頂）

**完成定義（凍結）**：使用者打開 `docs/acceptance/0.6.71.md`，會看到：用 M0 建出的伺服器，在實驗環境完成「登入 → 大廳 → 進房 → 一場戰鬥 → 結算 → 正常退出」，每一步都有截圖；結算後實驗庫 `character_lito` 的勝敗筆數有變化（附 `SELECT` 前後對照）。

| ID | 內容 | 狀態 |
|---|---|---|
| T010 | 實機核對表 `docs/acceptance/CHECKLIST.md` | done |
| T011 | 依 T010 在實驗環境跑一次，寫 `docs/acceptance/0.6.71.md`；失敗項各開 bug（進 Backlog，影響驗收者升格為主線） | blocked（等 M0） |

## M2 戰鬥技能同源化（M1 完成後封頂）

**完成定義（凍結）**：使用者會看到：
1. B-land 之後，`mvn package` 產物對候選 `50635f7e…` 的語意比對摘要，B 範圍 1,384 個 entry **DIFF=0**（核准的偏離另行列出）。
2. `tools/combat-continuation` 既有 2,400 案、5,164 組合、curse-clock 24 案，在 Maven 產物上重跑的對照表，結果全部一致。
3. 在實驗環境用新建置打一場戰鬥，施放指定技能（清單在 M2 封頂時訂定），附截圖。

目前 B01–B09、B-land、T020-close 等工作列在 Backlog，待 M1 完成後封頂時再升格。

## M3 交付（M2 完成後封頂）

**完成定義（凍結）**：使用者會看到：
1. `release/package-0.6.x.zip` 一個檔案。
2. 依 `docs/INSTALL.md` 在乾淨的 Windows 機器（或乾淨目錄）安裝後，啟動到大廳的截圖。
3. git tag `launcher-v0.6.x`／`server-v0.6.x`。

| ID | 內容 | 狀態 |
|---|---|---|
| T040 | 安裝手冊＋打包腳本（Issue #32，PR #33） | done |

---

## Backlog（不算進度）

### 戰鬥合併 B 階段（M2 封頂時升格）
| ID | Issue | 內容 | 狀態 |
|---|---|---|---|
| T020-B01 | #62 | new-asm 17 類 | 等 #203（20 SAME＋1 DIFF） |
| T020-B02 | #63 | new-java 51 類 | done（107 SAME＋1 SAME_PATHS） |
| T020-B03 | #64 | new-java 51 類 | 等 #69 |
| T020-B04 | #65 | new-java 51 類 | 等 #203 |
| T020-B05 | #66 | new-java 48 類 | review:pass（0 DIFF） |
| T020-B06 | #67 | existing-battle 29 類 | 等 #203 |
| T020-B07 | #68 | existing-combat-material 23 類 | 等 #203、#216 |
| T020-B08 | #69 | existing-native 30 類 | 等 #203、#216 |
| T020-B09 | #70 | existing-core 3 類（含 T029 八方法） | 等 #203、#216 |
| T020-B-land | #138 | B 暫存併入 src/main | 等 B01–B09 |
| T020-close | #139 | 總語意比對＋Maven 產物全套戰鬥回歸 | 等 #78、#138、#84 |
| T020c-3 | #84 | 8d26c9d7 基準完整回歸 | 等 #83 |

設計：`docs/design/T020-combat-merge.md`（§6 B 階段、§7 A 階段）。

### 戰鬥技能開放缺口（原 M2 清單）
- T021 技能目標／Ctrl 政策（807 列技能 × 正常／Ctrl，既有 3,814 案未驗此政策）
- T022 26／42 自施：Source26 不按 Ctrl 無法自施（需真實游標與送包證據）
- T023 受控 NPC（PET／SUMMON）Source 語意：1,472 組目標失敗＋1,472 正常順序
- T024 HP 生產端：魔杖／反射／毒傷／裝備／魔石／武器 proc、魔法娃娃 proc、fatality、cube、ground pulse、AI
- T025 58 列材料技能逐列（剩 27、36、41、44；靜態掃描剩 3 個舊呼叫）
- T026 外控完整鏈：讀取器→消費端、動態鍵／別名、SQL 控制
- T027 生命週期：receipt 保留／排空、成對鎖、退役／死復活／同 ID 替換、DRAINING／STOPPED／Main
- T028 顯示與 NPC 語意：HPBAR／傷害數字／奇古獸、NPC 工廠／AI／hate、免疫時序
- T029 正式功能隔離合成：0.6.71 資源／圖示／litoddraw.dll／GM7200（已由 B09 的 T029 八方法部分涵蓋）
- T030 同 SHA 後端全回歸（等 T021–T029）
- T031 實機：游標／前景／多視窗／大廳集中驗收（使用者）
- T032 中文 MySQL 真裝備角色唯讀重載（使用者延後）

### 其他
- T042（#184）：調查 CombatSkillDerived 技能表欄位遺失的潛伏 bug。
- 登入器已知未驗：結算畫面職業圖示、0.6.67 Ctrl 強制攻擊雙視窗、GM 魔杖滑鼠點選、斷線結算壓力／災難復原、舊正式入口仍使用通用 `ddraw.dll` 名稱（候選為 `litoddraw.dll`）。
