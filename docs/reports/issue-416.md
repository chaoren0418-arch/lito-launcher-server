# Issue #416 — 分流 #341（T083）實機驗收失敗項目 → 修正任務＋重測

## 結論

#341 第 2 輪（✅ 7／❌ 5／⚠️ 2）的 7 個未通過項目已全部對應到任務或已知限制，共開出 4 張任務（`docs/plans/issue-416.json`）：
- **T142**（code，claude-local）：處理 D1、D2，把歷史 class 位元組守門改成和 Maven 產物相容。
- **T143**（resource，codex-local，持 p24lab）：替 F1–F5 收集環境與 fixture 的證據。
- **T144**（code，claude-local，依賴 T143）：定位並修正 F1–F5 的根因。
- **T145**（test，milestone_smoke，依賴 T142／T143／T144）：只重測 05–10、12，另外附帶回歸 01／02／11／13／14。

本 repo 裡沒有其他排隊中、`depends_on` 含 #341 的任務，所以不需要關閉或改寫任何 Issue。

本任務只改文件與規劃檔，沒有修改程式，也沒有執行任何程式。

## 輸入

- `origin/task/341`（`dcf99703`）的 `docs/reports/issue-341.md`
- #341 最後一則 RESULT（2026-11-13 21:39Z）
- 本機 `scratch/wt/task-341/scratch/issue-341/retest/results/*.log`（唯讀）
- git 內的 probe 原始碼：`WaterLifeMaterialProbe.java:35`、`CancellationProbabilityProbe.java:56`、`cancellation-battle-runtime/test_boundaries.py:29`

## 已通過（✅）項目——不重測

| 編號 | 內容 | 是否在 T145 附帶回歸 | 原因 |
|---|---|---|---|
| 01 | Maven package | **是** | T144 若改到 `src/main`，JAR SHA 會變，必須重建 |
| 02 | 輸入 SHA | **是** | 同上，需要記錄新的 Maven SHA |
| 03 | 設定與 DB 身分 | 否（沿用） | 只補 #341 沒取到的測後筆數，作為資料保護檢查 |
| 04 | lease | 否（沿用） | 執行前本來就要核實 lease |
| 11 | #407 失敗路徑收據 | **是** | T142 會改 `regression_support`／`candidate_regression` 的包裝層與收據 |
| 13 | 正式檔保護 | **是** | 工具改動後需要再確認 `formal_protection` |
| 14 | 程序清理 | **是** | T145 會自己啟動 P24Lab 與 JVM |

## 失敗項目對應

| 編號 | 結果 | 失敗點 | 分類 | 對應 |
|---|---|---|---|---|
| 05 | ❌ | D1：Maven 的 `GameStateStore.class` 和歷史 base 的位元組不同（`test_boundaries.py:29`），導致 12 個 fault 案未執行 | (a) 工具缺陷 | **T142** |
| 05 | ❌ | F1：候選 CallNature `MP actual=1000 expectedCost=50` | 先查環境（b），再修根因 | **T143 → T144** |
| 06 | ❌ | F2：雙邊的 CancellationProbability 都找不到機率紀錄行 | (b)→(a) | **T143 → T144** |
| 07 | ❌ | F3：雙邊 WaterLife 的 `Source resources`（log 顯示 `mp-cost=1 mp=999/1000`） | (b)→(a) | **T143 → T144** |
| 08 | ❌ | D2：未入 git 的 `replay-native-de-item-state-components.py` 用 allowlist 擋下 `NativeDeCorpseClock$1.class` | (a) 工具缺陷（和 D1 同一根因，合成一張） | **T142** |
| 08 | ❌ | F5：候選第 7 組 `NativeDeActorStateRegression` 的事件順序 | (b)→(a) | **T143 → T144** |
| 09 | ❌ | F4：雙邊 CurseClock `actual first socket write delayed` | (b)→(a) | **T143 → T144** |
| 10 | ⚠️ | seal 拒絕，原因是 targeted failed | 05–09 的連帶結果 | 不另開任務，由 **T145** 重測 |
| 12 | ⚠️ | 7,588 案中只有 276 案有雙邊結果 | 05–09 的連帶結果 | 不另開任務，由 **T145** 重測 |
| 03（附註） | ✅ | 測後筆數沒取到（PowerShell 引數問題） | 測試程序細節 | 寫進 **T143**（步驟 1c）與 **T145** 的附帶項目 |

F1–F5 分成「先取證、再修」兩張，理由如下：
- 候選 `50635f7e` 是歷史資格已通過的 JAR，這次卻和 Maven 在同一批首案一起失敗，所以根因比較可能在現行測試環境，而不是兩顆 JAR 的差異。可能的環境來源有：P24 實驗 profile `P24LiToExp/SignedHp1`、實驗 schema 的技能列、原始專案未入 git 的 `build/` fixture classes、2026-10-16 的 vectors。
- 這些證據要連 P24 才查得到，claude-local 做不到，所以先交給 codex-local（T143）取證，再交給 claude-local（T144）定位。
- T144 允許四種結論：環境、fixture 過期、預期值寫死、`src/main` B09 缺陷，每一種都有對應的修法。T144 明文禁止刪除或放寬斷言；如果需要改實驗資料，只能標 `needs:local`，不能自己改。

## 已知限制（不開任務）

1. **逐案完整行為等價**：工具只輸出斷言結果與彙總欄位，沒有每一案的完整狀態／封包 trace。因此 12 的「一致」只能指既有的 observations 欄位一致，這是工具設計的範圍，不屬於本輪缺陷。
2. **約 113 個例外／迴圈方法的逐 handler 覆蓋**：現有工具沒有覆蓋率量測。#341 的背景提到要優先覆蓋例外路徑，但這需要另外規劃一套覆蓋工具。如果 M2 的完成定義必須包含它，應該由主控另開 plan 任務，本次分流不處理。
3. **啟動包裝誤報 timeout**：腳本只比對監督 PID，而 listener 屬於子程序。#341 已經確認 ready 並繼續測試，結果不受影響。在 T145 內文的作法沿用 #341 報告，不另開任務。

## 其他排隊任務

- 查了 `gh issue list --state open` 的全部內文：除了本張，只有 #84 的內文含有「341」字樣，但它的 `depends_on` 是 `[83]`，不依賴 #341，所以依規定沒有改動。
- 提醒：#84 是用同一套入口對另一顆候選 `8d26c9d7` 跑回歸。如果 F1–F5 真的是環境問題，#84 很可能也會在同樣的首案失敗。建議主控在 T144 完成前先暫緩 #84（這不在本張的權限範圍內）。

## 已驗證／推測／待驗證

- **已驗證**：
  - 各失敗點的斷言字串、檔案行號與 classpath，都從 #341 的原始 log 與 git 內原始碼直接讀到。
  - D2 的腳本確實不在 git 裡，只存在於原始專案的 `tools/resource-database/build/`。
  - `docs/plans/issue-416.json` 用 `json.load` 解析成功。
- **推測**：
  - F1–F5 是環境或 fixture 差異，不是 B09 缺陷。依據是候選也一起失敗、log 裡的 MP 數值異常；尚未證實。
- **待驗證**：
  - T142–T145 全部。

## 產出路徑

- `docs/plans/issue-416.json`（T142–T145）
- `docs/reports/issue-416.md`
- 分支：`task/416`

## 給下一棒的提示

- **T142**：D2 的守門在未入 git 的腳本裡，只能從 git 內的包裝層處理，不要動 `--resources` 下的檔案。候選的位元組完全相同檢查必須保留。
- **T143**：重點在比對歷史資格當時的環境（source profile、設定、resourceProfile）和現在 `SignedHp1`（MP 1000/1000）的差異，以及 fixture `.class` 的版本。只能 SELECT。
- **T144**：先讀 T143 的 tsv 再定位。F3 的 log 是 `mp-cost=1`，而 probe 用 `cost(pc,170,row)` 計算，可以先從這裡查。
- **T145**：使用全新的輸出目錄；收據裡必須看得到 `class_identity_mode`。
