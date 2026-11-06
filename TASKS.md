# TASKS — lito-launcher-server

格式：`ID | 狀態 | 負責 | 範圍 | 驗收條件`。狀態：todo / doing / review / done / blocked。
PR 標題用 `ID: 摘要`。已知範圍與不確定項的估計見 `PLAN.md`「盤點：已知範圍」。

## 規則（2026-11-06 範圍凍結，Issue #231）
- **進度只算 `## M*` 區段**；`## Backlog` 只顯示數量，不算進度。
- 每個里程碑的「**完成定義**」一經寫定即凍結。要修改，須另開 Issue 說明理由並標 `needs:user`。
- **主線封頂**：最近一個未完成的里程碑，其剩餘任務一次列完，標題註明「主線 N 項，封頂」。之後新發現的工作一律進 Backlog；要升格為主線，必須同時刪掉一項主線，或經使用者同意。**唯一的例外**是 `PLAN.md` 已列的不確定項兌現：可以直接增加主線，但 PR 必須引用當初的估計。
- 比較器（semantic-diff）的工具缺口：先併進**尚未開工**的工具任務；若唯一的工具任務已在執行中，才另開一張，並視為 M0 不確定項 U0-1 的兌現。

---

## M0 從原始碼建出正式版，並啟動到大廳（主線 12 項，封頂；已完成 7、剩餘 5）

**完成定義（凍結）**：使用者打開 Issue #225，會看到：
1. 一行指令 `mvn -f lito伺服器/LiTo381-Server/pom.xml -B test` 的輸出，結果為 `Failures: 0, Errors: 0`。
2. `src/main` 用 `mvn package` 建出 `LiTo381-Server.jar`，與正式 `reference/base-0.6.71.jar` 做語意比對，摘要分成：
   - **A 範圍** 692 個 entry：**DIFF=0**（核准的偏離另行列出）；
   - **整顆 JAR**：差異只能落在下面兩類，**其他差異必須為 0**：
     - (i) **B 範圍中與正式版不同的 entry**：約 205 個，即 DIFF 203＋ONLY_RIGHT 2（設計 §7-1；不同於 M2 第 1 點所說候選的全部 B entry 1,384 個）。這些類別在 M2 會改成候選 `50635f7e…` 的內容，**不會**變回正式版；
     - (ii) **`src/main` 專有的 ONLY_LEFT**：約 1,380 個（`Modern*` 等，T007 刻意排除）。
   - 因此 M0 的 JAR **不是**整顆等價於正式版。
3. 三張截圖，都是用上述 JAR 在**實驗環境**中拍的：伺服器主控台啟動完成、登入器連線、遊戲**大廳**。實驗環境指 P24Lab，或由 `environments/` 快照建立的隔離 DB，不碰正式 MySQL 3306 與正式設定。
4. 一份使用者可以自己重跑的指令清單。

| # | ID | Issue | 內容 | 狀態（2026-11-05 21:35 UTC 查詢） |
|---|---|---|---|---|
| 1 | T020-A01 | #72 | src/main 對齊正式（nativede，49 類）暫存 | **done**（closed） |
| 2 | T020-A02 | #73 | 同上（nativede，49 類） | queued；`cancelNpcChatTimer` 改名，等 #235 |
| 3 | T020-A03 | #74 | 同上（nativede，47 類） | **done**（closed） |
| 4 | T020-A04 | #75 | 同上（battle-combat，25 類） | **done**（closed） |
| 5 | T020-A05 | #76 | 同上（other，27 類） | **done**（closed） |
| 6 | T020-A06 | #77 | 同上（other，27 類） | **done**（closed） |
| 7 | T020i-8 | #203 | 比較器：依呼叫點配對 lambda、CFG 比對、可見性 bridge | **done**（closed） |
| 8 | T020i-9 | #216 | 比較器：單一屬性 metadata 豁免、純轉拋 handler、public 虛擬呼叫 owner | **done**（closed） |
| 9 | T020i-10 | #235 | 比較器：private 方法改名配對（不確定項 U0-1 兌現第 1 張） | doing |
| 10 | T020-A-land | #78 | A 暫存併入 src/main，補 7 項單元測試，`mvn test` 綠燈，A 範圍語意 DIFF=0 | todo（等 2、9） |
| 11 | T020c-2 | #83 | 實驗 DB：從 P24Lab 或 `environments/` 快照建立隔離的實驗 schema／實例與實驗設定（dot） | blocked（needs:user：處理誤匯出的正式副本，預設 C 凍結並改走 P24；事故稽核另由 #247 執行） |
| 12 | T050 | #225 | 用實驗環境啟動到大廳並截圖（dot） | todo（等 10、11） |

<details><summary>M0 之前已完成的早期項目（計入歷史，不計入上表）</summary>

- T001 done：CI（Maven compile＋登入器 dotnet build）。
- T002 done（第一階段）：`src/main` 補齊缺漏類別、pom 改 0.6.71、加上 Main-Class。A 範圍對齊正式版由上表第 10 項完成。B 範圍的類別在 M2 改成候選內容，`src/main` 專有類別（T007）照舊保留；最終 JAR 依設計 §6-2，是「B 類別＝候選，其他＝正式」，並非整顆等於正式版。
- T003 done（Issue #4，PR #8）：`Build.ps1` 去掉硬編路徑。
- T004 done（Issue #5）：版本號與文件統一為 0.6.71。
- T005 done（Issue #3；經 #12/#15、#20/#22 達成）：DB 依賴測試加守門，Server tests 改為阻斷式。
- T006 done：真實 `server.properties` 不進 git。
- T007 done（Issue #2，PR #7）：CI 比對類別清單（`docs/server-source-gap.md`）。
- T008 done（否定結果，Issue #6）：evidence.zip 內找不到 266 個 DECOMPILED 類別的原始碼。
- T009 done（Issue #12，PR #15）：本機資源依賴測試加守門。
- T012 done（Issue #37，PR #39）：修正不穩定的測試。
- 比較器與轉換器：#102、#108、#132、#134、#135、#153、#166、#174、#176、#186 皆 done。
</details>

## M1 正式版實機回歸（實驗環境；M0 完成後封頂）

**完成定義（凍結）**：使用者打開 `docs/acceptance/0.6.71.md`，會看到：用 M0 建出的伺服器，在**實驗環境**（同 M0 定義）完成「登入 → 大廳 → 進房 → 一場戰鬥 → 結算 → 正常退出」，每一步都有截圖；結算後實驗庫的 `character_lito` 勝敗筆數有變化，附 `SELECT` 前後對照。

| ID | 內容 | 狀態 |
|---|---|---|
| T010 | 實機核對表 `docs/acceptance/CHECKLIST.md` | done（需依 T013 改寫） |
| T013 | 把 CHECKLIST 改成實驗環境版：不用 `config/server.properties` 與正式 DB；補上登入器已知未驗的 GM 魔杖滑鼠點選、`ddraw.dll`／`litoddraw.dll` 入口名稱 | todo |
| T011 | 依 CHECKLIST 在實驗環境跑一次，寫 `docs/acceptance/0.6.71.md`（dot 執行，使用者觀看驗收） | blocked（等 M0、T013） |

## M2 戰鬥技能同源化（M1 完成後封頂）

**完成定義（凍結）**：使用者會看到：
1. B-land 之後，`mvn package` 產物對候選 `50635f7e…` 的語意比對摘要：B 範圍（候選的**全部** B entry）1,384 個、**DIFF=0**（核准的偏離另列）。
   - **修訂（2026-11-06，使用者於 #70 選 C）**：`ClientSession` 中「呼叫與欄位讀寫多重集合相同」的形狀差異方法（逐條列表），先改由第 2 點的戰鬥回歸全部一致來驗收；缺呼叫、多呼叫、欄位讀寫不同仍一律改 Java。之後再以比較器擴充（U2-1 追加 2 張）補靜態證明，補到的方法從列表移除。
2. `tools/combat-continuation` 既有的 2,400 案、5,164 組合、curse-clock 24 案，在 Maven 產物上重跑的對照表，全部一致。
3. 在實驗環境用新建置打一場戰鬥，技能 29、152、153 各施放一次並截圖。這三個技能有 `50635f7e…` 同 SHA 的專項收據（`docs/maintenance/source-negative-final-frame.zh-TW.md`）。

已知主線（M2 封頂時從 Backlog 升格）：B01–B09（#62–#70）、B-land（#138）、T020-close（#139）、T020c-3（#84）、#31 收尾。

## M3 交付（M2 完成後封頂）

**完成定義（凍結）**：使用者會看到：
1. 一個 `release/package-0.6.x.zip` 檔案。
2. 依 `docs/INSTALL.md` 在乾淨的 Windows 目錄安裝後，於實驗環境啟動到大廳的截圖。
3. git tag `launcher-v0.6.x`／`server-v0.6.x`。

| ID | 內容 | 狀態 |
|---|---|---|
| T040 | 安裝手冊＋打包腳本（Issue #32，PR #33） | done |
| T060 | 打包改用 src/main 的 Maven 產物，並更新版本號 | todo |
| T061 | 依 INSTALL 在乾淨目錄安裝並啟動到大廳（dot，實驗環境） | todo |
| T062 | 打 tag 並建 release 收據 | todo |

---

## Backlog（不算進度）

### M2 已知項目（M1 完成後升格）
| ID | Issue | 內容 | 狀態 |
|---|---|---|---|
| T020-B01 | #62 | new-asm 17 類 | review |
| T020-B02 | #63 | new-java 51 類 | **done** |
| T020-B03 | #64 | new-java 51 類 | queued（等 #69） |
| T020-B04 | #65 | new-java 51 類 | **done** |
| T020-B05 | #66 | new-java 48 類 | **done** |
| T020-B06 | #67 | existing-battle 29 類 | **done** |
| T020-B07 | #68 | existing-combat-material 23 類 | review |
| T020-B08 | #69 | existing-native 30 類 | running |
| T020-B09 | #70 | existing-core 3 類（含 T029 八方法） | queued |
| T020-B-land | #138 | B 暫存併入 src/main | 等 B01–B09 |
| T020-close | #139 | 總語意比對＋Maven 產物全套戰鬥回歸 | 等 #78、#138、#84 |
| T020c-3 | #84 | 8d26c9d7 基準完整回歸 | 等 #83 |
| T020c | #31 | 回歸工具參數化（review:pass，仍 open 待關閉） | 收尾 |

設計：`docs/design/T020-combat-merge.md`（§6 B 階段、§7 A 階段）。

### M4（未立）Source 全技能對齊——不在目前合約里程碑內
`docs/maintenance/combat-closeout-current.zh-TW.md` 的 12 項收尾矩陣與下列開放缺口，屬於「技能行為與原版完全一致」的長期開發，範圍超出 M2「把已驗證的候選併進原始碼」。M3 交付後，與使用者／廠商確認範圍時再決定是否立案。
- T021 技能目標／Ctrl 政策（807 列技能 × 正常／Ctrl，既有 3,814 案未驗證此政策）
- T022 26／42 自施：Source26 不按 Ctrl 無法自施（需真實游標與送包證據）
- T023 受控 NPC（PET／SUMMON）Source 語意：1,472 組目標失敗＋1,472 組正常順序
- T024 HP 生產端：魔杖／反射／毒傷／裝備／魔石／武器 proc、魔法娃娃 proc、fatality、cube、ground pulse、AI
- T025 58 列材料技能逐列（剩 27、36、41、44；靜態掃描剩 3 個舊呼叫）
- T026 外控完整鏈：讀取器→消費端、動態鍵／別名、SQL 控制（252 列盤點，141 列待審）
- T027 生命週期：receipt 保留／排空、成對鎖、退役／死亡復活／同 ID 替換、DRAINING／STOPPED／Main
- T028 顯示與 NPC 語意：HPBAR／傷害數字／奇古獸、NPC 工廠／AI／hate、免疫時序
- T029 正式功能隔離合成（大部分已由 B09 的 T029 八方法涵蓋）
- T030 同 SHA 後端全回歸
- T031 實機：游標／前景／多視窗／大廳集中驗收（使用者）
- T032 中文 MySQL 真裝備角色唯讀重載（使用者延後）
- 候選 `3ccbb…`（76／167 主目標修補）與 `046c58c5…`（2026-10-29 收尾開發，「已實作、待測」）相對 `50635f7e…` 的差異

### 其他
- 登入器已知未驗中，CHECKLIST 尚未涵蓋的只剩：斷線結算的長時間壓力與災難復原。結算職業圖示、Ctrl 強制攻擊雙視窗、斷線結算已在 CHECKLIST 步驟 10、14、12；GM 魔杖與 ddraw 名稱由 T013 補進。
