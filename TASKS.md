# TASKS — lito-launcher-server

格式：`ID | 狀態 | 負責 | 範圍 | 驗收條件`。狀態：todo / doing / review / done / blocked。
PR 標題用 `ID: 摘要`。已知範圍與不確定項的估計見 `PLAN.md`「盤點：已知範圍與不確定項」。

## 規則（2026-11-10，#290 重定方向；沿用 #231 的凍結規則）
- **主線是伺服器功能開發**：角色、道具、戰鬥、技能、跨界（多來源）。行為參考 `D:\lineage開發\1-伺服器`（唯讀），協定以 `D:\lineage開發\0-客戶端` 為準（唯讀）。任務必須寫明參考的類別或檔案。
- **開發基線是 `src/main`**（M2 完成後）。不再在 `tools/*/build.py` patch 鏈上做新功能。
- **進度只算 `## M*` 區段**；`## Backlog` 只顯示數量，不算進度。
- 每個里程碑的「**完成定義**」一經寫定即凍結。要修改，須另開 Issue 說明理由並標 `needs:user`。
- **主線封頂**：最近一個未完成的里程碑，其剩餘任務一次列完，標題註明「主線 N 項，封頂」。之後新發現的工作一律進 Backlog；要升格為主線，必須同時刪掉一項主線，或經使用者同意。**唯一的例外**是 `PLAN.md` 已列的不確定項兌現：可以直接增加主線，但 PR 必須引用當初的估計。
- 本機任務預設 `runner: claude-local`；本專案同時最多 1 個本機任務，TW BIN 優先。需要 P24Lab 或實機的任務等 Codex 恢復（2026-11-13 18:11 之後）。
- 不碰正式 2000／3306；不改 `0-客戶端`、正式 `LiTo.bin`、shared BIN、TW／LHZ 資源。

---

## M1 能力盤點（主線 4 項，封頂）

**完成定義（凍結）**：使用者打開 `docs/reports/server-capability-matrix.md`，會看到：
1. 角色、道具、戰鬥、技能、跨界（多來源）五個領域，每一列寫：`1-伺服器` 怎麼做（類別或檔案＋行號）／LiTo 現況（正式 0.6.71、候選或未實作）／客戶端需要的協定／差距／對應的 M3 任務；
2. 每列的證據都可以點開；沒查到的寫「未找到（查過哪裡）」，不推測；
3. M3 主線清單與排序，以及移到 Backlog 的項目與理由。

| # | ID | Issue | 內容 | 負責 | 狀態 |
|---|---|---|---|---|---|
| 1 | T090 | #291 | 盤點 `1-伺服器` 的角色／道具／戰鬥／技能／多來源實作（唯讀），寫 `docs/reference/server1-inventory.md` | claude-local | todo |
| 2 | T091 | #292 | LiTo 現況能力矩陣，寫 `docs/reports/lito-capability-current.md`（併入 #288 的 `customer-gaps.md`） | claude-local | todo |
| 3 | T092 | #293 | 客戶端協定與資源需求（`lito脫殼資源/`＋`0-客戶端`，唯讀），寫 `docs/reference/client-protocol-needs.md` | claude-local | todo |
| 4 | T093 | #294 | 合併成差距矩陣，拆 M3 任務並封頂 | Claude（主控） | todo（等 1–3） |

## M2 原始碼開發基線（M1 完成後封頂）

**完成定義（凍結）**：使用者會看到：
1. `src/main` 已併入候選 `50635f7e…` 的戰鬥內容（B-land），`mvn -f lito伺服器/LiTo381-Server/pom.xml -B test` 為 `Failures: 0, Errors: 0`；
2. `mvn package` 產物對候選的語意比對摘要：B 範圍除了 `docs/combat-merge/b09-shape-diffs.md` 逐條列出的形狀差異外，DIFF=0；
3. 離線戰鬥回歸（`tools/combat-continuation`）在 Maven 產物上的執行結果；跑不起來時寫明原因（PLAN U2-2）。

| ID | Issue | 內容 | 狀態 |
|---|---|---|---|
| T020-B09 | #70 | existing-core 3 類收尾（依使用者選 C：缺呼叫等改 Java，形狀差異逐條列表） | todo（等 #293） |
| T020-B-land | #138 | B 暫存併入 `src/main`，`mvn test` 綠燈 | todo（等 #70） |
| T051 | #281 | `LegacyMySqlRepository` 的 shared-database 接線；B-land 若已帶入候選版本則驗證後關閉 | todo（等 #138） |

## M3 功能開發（M1 的 T093 完成後封頂）

**完成定義（凍結）**：使用者打開 `docs/reports/server-capability-matrix.md`，M3 主線的每一列都標為「已實作＋單元測試＋離線回歸通過」，並附 PR 連結；`mvn test` 仍為綠燈。

任務由 T093 開出（PLAN U3-1 估 8–15 張）。已知會併入的：`customer-gaps.md` 的 G03–G08（技能目標／範圍與 Source 不一致）。

## M4 實機驗收與交付（Codex 恢復後；M3 完成後封頂）

**完成定義（凍結）**：使用者會看到：
1. 用 M3 的新 JAR 在實驗環境跑完 `docs/acceptance/CHECKLIST.md`（登入 → 大廳 → 進房 → 技能 → 戰鬥 → 結算 → 退出），每步截圖，寫在 `docs/acceptance/<版本>.md`；
2. `release/package-0.6.x.zip`，依 `docs/INSTALL.md` 在乾淨目錄安裝並啟動到大廳的截圖；
3. git tag `launcher-v0.6.x`／`server-v0.6.x`。

| ID | Issue | 內容 | 狀態 |
|---|---|---|---|
| T014 | #284 | 實驗環境啟動（改用新 JAR；P24 資料不相容時改用 `environments/` 的 `380_db.sql` 快照，PLAN U4-1） | blocked（等 Codex、M3） |
| T011 | #285 | 依 CHECKLIST 跑表並截圖 | blocked（等 #284） |
| T060 | — | 打包新 JAR，更新版本號 | todo |
| T061 | — | 依 INSTALL 在乾淨目錄安裝並啟動到大廳 | todo |
| T062 | — | 打 tag 並建 release 收據 | todo |

<details><summary>已完成的項目（計入歷史，不計入上表）</summary>

- T001–T009、T012：CI、建置腳本、版本號、DB 測試守門、類別清單比對等早期項目。
- T010／T013（#283）：實機核對表 `docs/acceptance/CHECKLIST.md`（實驗環境版）。
- T040（Issue #32，PR #33）：安裝手冊＋打包腳本。
- T070（#288）：客戶缺口第一版 `docs/reports/customer-gaps.md`。
- 原始碼重建 A 階段：A01–A06（#72–#77）、比較器 #203／#216／#235／#254／#259、A-land（#78，A 範圍 692 entries 語意 DIFF=0）。
- B01–B08（#62–#69）暫存完成。
- 實驗環境：#83 P24Lab 實驗 schema；事故善後 #247、#273。
</details>

---

## Backlog（不算進度）
| ID | Issue | 內容 | 狀態 |
|---|---|---|---|
| T050 | #225 | 用 `src/main` 的 JAR 在實驗環境啟動到大廳並截圖（由 M4 的 T014 取代） | blocked |
| T020c-3 | #84 | 8d26c9d7 基準完整回歸 | todo |
| T020c | #31 | 回歸工具參數化（review:pass，待關閉） | 收尾 |
| — | — | 可重現封裝（固定 `project.build.outputTimestamp`） | todo |
| — | — | 候選 `3ccbb…`／`046c58c5…` 相對 `50635f7e…` 的差異，依 T093 決定是否併入 M3 | todo |
| — | — | `customer-gaps.md` 的 G09–G28（依 T093 重新分類） | todo |
| — | — | 斷線結算的長時間壓力與災難復原 | todo |
