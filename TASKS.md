# TASKS — lito-launcher-server

格式：`ID | 狀態 | 負責 | 範圍 | 驗收條件`。狀態：todo / doing / review / done / blocked。
PR 標題用 `ID: 摘要`。已知範圍與不確定項的估計見 `PLAN.md`「盤點：已知範圍」。

## 規則（2026-11-06，#231 範圍凍結；#282 重做計畫）
- **交付基準是正式 0.6.71**（`release/current/LiTo381Launcher.exe`＋`app/server/LiTo381-SharedResources-candidate.jar`）。要改伺服器時只做該項的**最小**修改（既有 `tools/*/build.py` patch 鏈，或只對齊要改的類別），不做整顆重建。
- **進度只算 `## M*` 區段**；`## Backlog` 只顯示數量，不算進度。
- 每個里程碑的「**完成定義**」一經寫定即凍結。要修改，須另開 Issue 說明理由並標 `needs:user`。
- **主線封頂**：最近一個未完成的里程碑，其剩餘任務一次列完，標題註明「主線 N 項，封頂」。之後新發現的工作一律進 Backlog；要升格為主線，必須同時刪掉一項主線，或經使用者同意。**唯一的例外**是 `PLAN.md` 已列的不確定項兌現：可以直接增加主線，但 PR 必須引用當初的估計。
- 實驗環境 = P24Lab 的 #83 實驗 schema（`p24_*_lito_exp`）。不碰正式 2000／3306，不改 P24Lab 原有的庫與 tw-bin 測試資料；啟動 P24Lab 必須持有調度器的 `p24lab` lease。

---

## M1 正式版實機回歸（主線 3 項，封頂）

**完成定義（凍結）**：使用者打開 `docs/acceptance/0.6.71.md`，會看到：用**正式 0.6.71**（上述登入器 exe＋正式 JAR，SHA-256 記錄在文件開頭）在**實驗環境**完成：
1. 登入 → 大廳 → 進房 → 施放技能（近戰、魔法、變身各一次）→ 一場戰鬥 → 結算 → 正常退出，每一步都有截圖；
2. 結算後實驗庫 `p24_accounts_lito_exp.character_lito` 的勝敗有變化，附 `SELECT` 前後對照；
3. 每個失敗或異常步驟都寫成一列「現象／步驟／證據」，作為 M2 盤點的來源（失敗不擋 M1 完成，但必須如實記錄）。

| # | ID | 內容 | 負責 | 狀態 |
|---|---|---|---|---|
| 1 | T013 | 把 `docs/acceptance/CHECKLIST.md` 改成實驗環境版：正式 JAR＋#83 實驗 schema，不用 `config/server.properties` 與正式 DB；拿掉不存在的 `--check`／`--smoke`；補「技能」步驟、GM 魔杖滑鼠點選、`ddraw.dll`／`litoddraw.dll` 入口名稱 | codex-local | todo |
| 2 | T014 | 正式 JAR 的實驗設定與啟動預檢：在 `scratch/t014/` 寫實驗設定（`bind=127.0.0.1`、`game.bind=127.0.0.2`、`port=42083`、#83 的實驗 schema 與受限帳號、全新的 runtime 目錄），以正式 JAR 啟動並保存完整主控台輸出到啟動完成；登入器連上實驗伺服器 | dot | todo（等 T013） |
| 3 | T011 | 依 CHECKLIST 在實驗環境跑一次，寫 `docs/acceptance/0.6.71.md`（dot 執行並截圖，使用者觀看驗收） | dot | todo（等 T014） |

## M2 客戶缺口（M1 完成後封頂）

**完成定義（凍結）**：使用者打開 `docs/reports/customer-gaps.md`，會看到：
1. 盤點出的每個缺口一列：現象、影響誰（玩家／GM／營運）、證據（M1 截圖、合約條文、或候選的同 SHA 收據），以及處理結果（已修／延後到 Backlog 並說明理由）；
2. 每個「已修」的缺口：修改方式（patch 鏈的哪一層或哪個類別）、修前／修後在實驗環境的重現截圖；
3. 修改後的伺服器 JAR 的 SHA-256，且 M1 的 CHECKLIST 在新 JAR 上重跑一次、沒有新增失敗。

| ID | 內容 | 狀態 |
|---|---|---|
| T070 | 盤點客戶缺口。來源只有三種：(a) M1 回歸的失敗或異常；(b) 廠商合約要求（登入、大廳、技能、進房、戰鬥結算、正常退出）但正式版做不到的；(c) 隔離候選中**有證據**已修復、正式版沒有的。沒有證據的差異一律進 Backlog。產出缺口清單與每項的任務草案，M2 主線依此封頂 | todo（等 M1） |

## M3 交付（M2 完成後封頂）

**完成定義（凍結）**：使用者會看到：
1. 一個 `release/package-0.6.x.zip` 檔案。
2. 依 `docs/INSTALL.md` 在乾淨的 Windows 目錄安裝後，於實驗環境啟動到大廳的截圖。
3. git tag `launcher-v0.6.x`／`server-v0.6.x`。

| ID | 內容 | 狀態 |
|---|---|---|
| T040 | 安裝手冊＋打包腳本（Issue #32，PR #33） | done |
| T060 | 打包使用正式 JAR（含 M2 修補），更新版本號 | todo |
| T061 | 依 INSTALL 在乾淨目錄安裝並啟動到大廳（dot，實驗環境） | todo |
| T062 | 打 tag 並建 release 收據 | todo |

<details><summary>已完成的早期項目（計入歷史，不計入上表）</summary>

- T001 done：CI（Maven compile＋登入器 dotnet build）。
- T002 done（第一階段）：`src/main` 補齊缺漏類別、pom 改 0.6.71、加上 Main-Class。
- T003 done（Issue #4，PR #8）：`Build.ps1` 去掉硬編路徑。
- T004 done（Issue #5）：版本號與文件統一為 0.6.71。
- T005 done（Issue #3；經 #12/#15、#20/#22 達成）：DB 依賴測試加守門，Server tests 改為阻斷式。
- T006 done：真實 `server.properties` 不進 git。
- T007 done（Issue #2，PR #7）：CI 比對類別清單（`docs/server-source-gap.md`）。
- T008 done（否定結果，Issue #6）：evidence.zip 內找不到 266 個 DECOMPILED 類別的原始碼。
- T009 done（Issue #12，PR #15）：本機資源依賴測試加守門。
- T012 done（Issue #37，PR #39）：修正不穩定的測試。
- T010 done：實機核對表 `docs/acceptance/CHECKLIST.md`（需依 T013 改寫）。
- T020c-2 done（#83）：P24Lab 實驗 schema 與受限帳號。
- #83 事故善後 done（#247 稽核、#273 停止 3307）。
</details>

---

## Backlog（不算進度）

### 原始碼重建（原 M0，#282 移入；需要時再升級）
「從 `pom.xml` 建出與正式版等價的 JAR」。已完成並保留在 `src/main`：A01–A06（#72–#77）、比較器 #203／#216／#235、A-land（#78：A 範圍 692 entries 語意 DIFF=0、`mvn test` 綠燈）。
| ID | Issue | 內容 | 狀態 |
|---|---|---|---|
| T051 | #281 | `src/main` `LegacyMySqlRepository` 對齊正式版的 shared-database 接線（原 U0-5） | blocked |
| T050 | #225 | 用 `src/main` 建出的 JAR 在實驗環境啟動到大廳並截圖 | blocked（等 #281） |
| — | — | 可重現封裝（固定 `project.build.outputTimestamp`） | todo |

### 戰鬥技能整顆同源化（原 M2，#282 移入）
B01–B08（#62–#69）暫存已完成；B09（#70）暫停，使用者已選 C（先以回歸驗收 `ClientSession` 形狀差異，再補比較器，見 #70 與 PR #274）。若 M2 盤點的某個缺口需要候選內容，只取該項的最小修改，不走整顆併入。
| ID | Issue | 內容 | 狀態 |
|---|---|---|---|
| T020-B09 | #70 | existing-core 3 類（含 T029 八方法） | blocked |
| T020-B-land | #138 | B 暫存併入 src/main | 等 B09 |
| T020c-3 | #84 | 8d26c9d7 基準完整回歸 | todo |
| T020c | #31 | 回歸工具參數化（review:pass，待關閉） | 收尾 |

設計：`docs/design/T020-combat-merge.md`。

### Source 全技能對齊（原 M4，未立）
`docs/maintenance/combat-closeout-current.zh-TW.md` 的 12 項收尾矩陣與下列開放缺口。若其中某項在 M2 盤點中有「客戶會碰到」的證據，才升格。
- T021 技能目標／Ctrl 政策（807 列技能 × 正常／Ctrl）
- T022 26／42 自施：Source26 不按 Ctrl 無法自施
- T023 受控 NPC（PET／SUMMON）Source 語意
- T024 HP 生產端：魔杖／反射／毒傷／裝備／魔石／武器 proc、魔法娃娃 proc、fatality、cube、ground pulse、AI
- T025 58 列材料技能逐列（剩 27、36、41、44）
- T026 外控完整鏈：讀取器→消費端、動態鍵／別名、SQL 控制
- T027 生命週期：receipt 保留／排空、成對鎖、退役／死亡復活／同 ID 替換
- T028 顯示與 NPC 語意：HPBAR／傷害數字／奇古獸、NPC 工廠／AI／hate、免疫時序
- T029 正式功能隔離合成
- T030 同 SHA 後端全回歸
- T031 實機：游標／前景／多視窗／大廳集中驗收（使用者）
- T032 中文 MySQL 真裝備角色唯讀重載（使用者延後）
- 候選 `3ccbb…`（76／167 主目標修補）與 `046c58c5…` 相對 `50635f7e…` 的差異

### 其他
- 斷線結算的長時間壓力與災難復原。
