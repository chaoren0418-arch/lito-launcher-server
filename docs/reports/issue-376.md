# Issue #376 — M3d：從 Backlog 再拉一批，並先替 M4 實機驗收做好準備

分支 `task/376`。執行者：`claude-local`（主控對話不在線，由本機做拆分）。本張只改文件與規劃檔，沒有改程式。

## 結論

- Backlog 每一項都有結論（下表）。M3c 的 6 張與 #366–#371 的 5 張都已合併，`TASKS.md` 原本還標 todo，已更正。
- 規劃檔 `docs/plans/issue-376.json` 開 **12 張**：
  - **B. M4 驗收準備 4 張**（`claude-local`，T127–T130，優先做）；
  - **A. 本機開發 6 張**（`claude-local`，T131–T136）；
  - **實機 2 張**（`codex-local`、`stage: test`，T137、T138，依賴 B 類）。
- `TASKS.md` 新增 M3d（主線 10 項，封頂）並把 T137、T138 列進 M4；`PLAN.md` 新增 M3d 與不確定項 U3d-1～3、U4-2。
- 有 3 件事需要主控或調度器處理，見「給下一棒的提示」第 1–3 點。

## Backlog 逐項結論

| 項目 | 結論 | 依據 |
|---|---|---|
| T050（#225）用 `src/main` 的 JAR 啟動到大廳 | 排除：由 M4 的 T014（#284）取代 | #284 的主控 2026-11-11 14:08 留言：M4 用新 JAR |
| T020c（#31）回歸工具參數化 | 已做，只差關閉 Issue | #31 標籤 `review:pass`、`gate:pass`，狀態仍 OPEN |
| T020c-3（#84）8d26c9d7 基準完整回歸 | 排除出 M3d，留 Backlog | 要 Codex 與 #83 的隔離 SQL；M2 的驗收已改由 #341 負責 |
| 可重現封裝 | 已做 | #357（T126） |
| 候選 `3ccbb…` 差異 | 已做 | #354：`3ccbb6f5…` 相對 `50635f7e…` 的全部內容（新增 3、改動 6 個 entry）就是 G14，已移植 |
| 候選 `046c58c5…` 差異（G15） | 開任務 T135（5 個類別）；其餘 24 個留 Backlog | #354 盤點：29 個 `NativeCloseout*` 都不在 `src/main`（本張再次用 Glob 確認為 0 個）；候選沒有測試收據 |
| G09–G12、G16 | 已做 | #356（T125） |
| G13、G18 | 已做（本來就在 `src/main`） | #354 |
| G14 | 已做（已移植） | #354 |
| G17 WORLD 專屬 | 排除 | 產品只有競技場；沒有找到競技場另外會碰到的部分（見下方「G17 的核對」） |
| G19–G27 人工驗收 | 開任務：準備 T127、T129、T130；實機 T137 | 都要實機；G27 的資產查核可以先在本機用 PakViewer 做 |
| G28 技能 131 停用 | 排除 | 現行規則，不是缺陷；要開放需使用者決定 |
| T111 藥水改讀 SQL | 已做；剩餘「共用冷卻」開 T134 | #352；剩餘項見 `issue-352.md:29-31` |
| 受控 NPC 完整語意 | 已做；G15 剩餘開 T135 | #354 |
| 588 列非玩家技能 | 競技場用到的 33 列已做；剩餘 3 項開 T132；572 列排除 | #353：其餘 572 列 Quest／Time Attack 用不到 |
| 跨程式互連（LC X15，#294） | 排除 | `server-capability-matrix.md:96`：只需要同一個 LiTo 程式接多個來源庫；`lito-capability-current.md:186`：未實作、沒有協定；要做需使用者先定需求 |
| 斷線結算的壓力與災難復原 | 已做（自動化）；真實拔線與強制結束交 T138 | #355 |
| 租屋城鎮 C_D0／C_D9（#138） | 已做 | #368 涵蓋，見下方核對 |

另外從 M3b、M3c 與 #366–#371 報告的「未處理／給下一棒」收進這一批或 Backlog 的項目：

| 來源 | 項目 | 去向 |
|---|---|---|
| #370、#369 | `DurableBattleSettlement.context()` 鎖順序死結；CI MySQL job 沒有逾時 | T131（P1） |
| #353 | 反制魔法擋 NPC 魔法、安息喊話、ChangeTarget=3 | T132 |
| #353 | 30004 火牢是否對齊、C10、距離 2 擲幣 | Backlog（第 1 項要使用者決定） |
| #338 | Z7 凍結中的怪物仍受傷 | T133 |
| #352 | 藥水共用冷卻 | T134 |
| #352 | 勇敢藥水職業限制、其他消耗品 | Backlog（要先知道正式 `380_db` 的值） |
| #355 | 作廢工具、`.tmp` 清理 | T136（P3） |
| #335 | `tools/db-grants/generated/` 與 main 脫節 | 併入 T128 第 1 步 |
| #336、#339、#340 | 44 對加速道具、祝福欄慣例、怪物物理技能打寵物 | Backlog（要使用者決定或先實機） |
| #371 | 租裝 TSV 兩欄可能對調 | Backlog（租屋城鎮未開放） |

### 租屋城鎮（#138 與 #368）的核對
- Backlog 原文：C_D0（離開）／C_D9（租屋傳送師）在 BATTLE 時被當成一般退戰，舊 `src/main` 的修正在 B-land 後消失。
- #368 的修正在目前 `src/main`：`ClientSession.java:13448` `stagedRentalTownQuitFight()`（`rentalTownTransferPending` 或 `RESTART_AWAIT_QUIT_FIGHT`）、`:13455` `stagedRentalTownReturn()`，分別在 `quitFight()`（`:13505`）與 `exitRoomBeforePolyMe()`（`:13160`）擋掉結算分支。
- #368 把 `NativeLitoRentalTeleportEndpointCatalogTest.exactTeleporterActionSendsStatusThenDirectEndpoint` 從「#138 記下的 0.6.71 行為」改成新行為，也就是 Backlog 這一列描述的兩個情況都已涵蓋。
- 沒有逐行比對「舊 `src/main` 的修正」原始內容（那個版本在 B-land 時被覆蓋），所以「與舊修正完全等價」屬推測；行為面由 #368 的測試證明。

### G17 的核對
- `combat-continuation-current.zh-TW.md:28-33` 六列都標 WORLD（角色生命、共用 Ref、特化 Proc、隨從 MP 提交與轉移、反射 HP），收據場景是 WORLD 傳送、主人場景與跨場景。
- 競技場裡同類的受控 NPC 問題（目標退役、成對生命週期）是 G13、G14、G18，已處理。
- 沒有逐類別追查這六個候選改到的類別在 BATTLE 路徑有沒有被呼叫，所以「競技場完全碰不到」是依文件判斷，屬推測。

## 規劃的 12 張任務

| ID | runner | stage | 優先 | 內容 | 依賴 |
|---|---|---|---|---|---|
| T127 | claude-local | code | P0 | CHECKLIST 改新 JAR 版＋`M4-RUNBOOK.md` A 部（01–21）、B 部（G19–G27、G02 封包） | — |
| T128 | claude-local | code | P0 | 實驗環境預檢與腳本（存取清單重產、schema 靜態比對、資料前置 SQL、`tools/acceptance/`） | — |
| T129 | claude-local | code | P0 | M3 系列實機待驗證彙整成 RUNBOOK C 部 | T127 |
| T130 | claude-local | resource | P1 | G27 map 12000 資產查核（PakViewer） | — |
| T131 | claude-local | code | P1 | 開戰與斷線同時發生的死結＋CI MySQL job 逾時 | — |
| T132 | claude-local | code | P2 | PvE 怪物魔法收尾 3 項 | — |
| T133 | claude-local | code | P2 | 凍結中的怪物仍受傷（Z7） | — |
| T134 | claude-local | code | P2 | 藥水共用冷卻 | — |
| T135 | claude-local | code | P2 | 受控 NPC 恢復與技能（G15 前兩組） | — |
| T136 | claude-local | code | P3 | 結算作廢工具與 `.tmp` 清理 | — |
| T137 | codex-local | test | P1 | 依 RUNBOOK B 部驗收 G19–G27、G02 | T127、T128、T130 |
| T138 | codex-local | test | P2 | 依 RUNBOOK C 部驗收 M3 系列改動 | T128、T129 |

設計上的取捨：
- **不重開 #284、#285、#341**。這三張已存在並等 Codex；新的實機任務只做它們沒涵蓋的部分（G19–G27 的專項、G02 封包、M3 系列改動），並在內文寫明前置是 #284 成功、#285 跑完。
- **命名契約寫在 T127**（版本目錄 `docs/acceptance/m4-<JAR SHA 前 8 碼>/`、截圖 `NN-`／`G<編號>-`／`V<序>-`、SQL 快照檔名），T128 的腳本與 T137／T138 都照它，這樣 T127 和 T128 誰先做都可以。
- **T128 先做靜態比對**：#284 三次失敗都是實驗庫的權限或結構問題。新 JAR 比正式 0.6.71 多了讀寫需求，用 python 把「程式會讀寫的表與欄位」對 `D:\BinEngine\environments\p24\sql\` 的傾印比一次，可以在取得 `p24lab` lease 之前就知道缺什麼。
- **T131 排在 A 類最前**：這是正式程式碼的問題，不只是 CI 不穩。
- **G17 沒有開任務**，理由如上。

## 怎麼驗證

### 已驗證
- M3c 6 張與 #366、#368–#371 的 Issue 狀態：`gh issue list` 顯示全部 CLOSED、`task:done`、`review:pass`；`docs/REPORT.md` 有對應的合併索引。
- #368 的兩個判斷方法在 `src/main`（Grep，行號如上）。
- `NativeCloseout*` 在 `src/main` 為 0 個檔案（Glob）；候選原始碼在 `tools/combat-continuation/integration-lifecycle/closeout-src/`（29 個 `NativeCloseout*`＋2 個其他檔）。
- 調度器的規劃檔格式：讀了 `D:\dev-orchestrator\orchestrator\plans.py`（唯讀）。它支援 `runner` 欄位；`depends_on` **只解析同一份規劃檔裡、排在前面的 id**；標題會自動加上 `id: ` 前綴。規劃檔已照這三點寫（依賴的項目都排在前面、標題不重複 id）。
- `docs/plans/issue-376.json` 用 python `json.load` 解析成功：12 項、id 不重複、每個 `depends_on` 都指向前面的項目。
- 任務內文引用的 `1-伺服器` 檔案存在：`model/L1ItemDelay.java`、`clientpackets/C_ItemUSe.java`、`model/L1MobSkillUse.java`、`model/L1AttackMode.java`、`model/Instance/L1PetInstance.java`、`L1SummonInstance.java`（Glob，唯讀）。
- 實驗庫快照存在：`D:\BinEngine\environments\p24\sql\380_db.sql`（9,552,749 bytes）、`380_lito.sql`、`380_ac.sql`（只列目錄，沒有讀內容）。
- `.github/workflows/build.yml` 沒有任何 `timeout-minutes`（Grep 無結果）。

### 推測
- G17 在競技場碰不到（依文件，未逐類別追查）。
- #368 與「舊 `src/main` 的修正」等價（未比對舊版原始碼）。
- 任務內文裡沿用其他報告的行號（例如 `ClientSession.java:54440-54600`、`GameHub.java:12500`、`issue-338` 的 `:45263-45296`）在 #366–#371 合併後可能已位移；內文已註明以方法名為準。
- T128 的靜態比對能預先抓到實驗庫的結構問題：`p24_accounts_lito_exp` 是 #83 從 P24 建的，沒有傾印可比，這一塊仍要開機前用 `information_schema` 查。
- 12 張的工作量：T127、T128、T135 偏大，可能各自要拆（T135 已寫明量太大時先做第 1 組）。

### 待驗證
- 規劃檔合併後調度器實際建出 12 張 Issue、依賴編號正確（本張合併後才會發生）。
- `stage: resource`（T130）在這個 repo 的調度器路由存在：過去 #291–#293 用過，沒有讀調度器設定確認。
- #341 的內文寫「不連任何 DB」，但 #84、#31 記載回歸 probe 需要讀資源表（隔離 SQL 設定）。#341 實際跑的時候可能卡在這裡，本張沒有查證。
- 所有實機項目（T137、T138 的內容）。

## 未做
- 沒有寫 RUNBOOK、腳本或任何程式（那是 T127–T136 的範圍）。
- 沒有關閉或改寫任何既有 Issue（#31、#84、#225、#284、#285）。
- 沒有改 `docs/REPORT.md`。

## 產出路徑
- `docs/plans/issue-376.json`
- `TASKS.md`（M3c 狀態更正、新增 M3d、M4 加 T137／T138、Backlog 改寫成「已結案／仍在 Backlog」兩表）
- `PLAN.md`（新增 M3d、U3d-1～3、U4-2、里程碑與依賴鏈）
- `docs/reports/customer-gaps.md`（G13、G14、G15、G18 四列的現況）
- `docs/reports/issue-376.md`（本檔）

## 給下一棒的提示
1. **調度器／主控：#284、#285 的派工時機**。這兩張已是 queued／blocked，Codex 18:11 恢復後會直接派出，可能早於 T127、T128 合併。#284 不受影響（它本來就照 02:43 裁定處理實驗庫）；但 **#285 的內文還寫「正式 0.6.71」與 runner `dot`**，和 M4「用新 JAR」的定義不一致。建議 T127 合併後再放行 #285，並把內文改成「依新版 CHECKLIST 與 M4-RUNBOOK A 部」。
2. **主控：可以關閉的 Issue**。#31（已 review:pass／gate:pass）、#225（被 #284 取代）。#84 等 #341 有結果後決定。
3. **主控：規劃檔建出 Issue 後回填編號**到 `TASKS.md` 的 M3d 與 M4 表（目前寫「待建」）。
4. **本機執行順序建議**（同時只能 1 張）：T128 → T127 → T130 → T129 → T131 → 其餘。T128 的靜態比對結果最能幫 #284 省時間；T127 與 T128 互不依賴。
5. **T137、T138 的前置寫在內文，不在 `depends_on`**：調度器只能解析同一份規劃檔的 id，所以「#284 成功、#285 跑完」是文字條件，執行者遇到不成立會回報阻塞。調度器若支援手動加依賴，建議建出後補上 `depends_on: [284]`。
6. G02、G20–G26 需要的角色與權限（GM、八角色、龍騎士／幻術師、魔法娃娃、GM 魔杖）#83 都沒有準備，T128 第 4 步會產出只寫 `*_lito_exp` 的資料前置 SQL；這是 M4 最可能卡住的地方。
