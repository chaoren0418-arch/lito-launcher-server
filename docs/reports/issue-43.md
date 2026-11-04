# Issue #43 / T020b — 戰鬥合併批次（設計）

**現行版本：第 4 版**（設計 §6，`docs/combat-merge/batches.json`、`t029-methods.txt`、`docs/plans/issue-43.json`、`tools/semantic-diff/plan_batches.py`）。第 1–3 版的敘述已被取代，只留在 git 歷史。

## 結論
- 第二階段（B）的 303 個外層類別（候選中共 1,384 個 class entry）分成 9 批：B01 asm-only 新類別 17、B02–B05 有原始碼的新類別 51／51／51／48、B06 Battle 29、B07 Combat／Material 23、B08 其他既有 30、B09 核心 3。
- 每批只在暫存區 `merge-inputs/50635f7e/staged/` 工作，以候選 JAR＋正式 JAR 當 classpath 獨立編譯，並對候選做語意比對（驗收範圍為完整 `verify_entries`）；最後由 B-land 一次併入 `src/main`，再由 T020-close 跑總比對與全套戰鬥回歸。
- T029：8 個方法整個採正式版，**沒有需要人工合成的方法**。

## 各版審查回應
| 審查 | 項目 | 處理（第 4 版狀態） |
|---|---|---|
| 14:02 | P1 相依閉包 | 相依分析顯示最大 SCC 282 類，依相依分批不可行；改為暫存區分批（§6-4）。 |
| 14:02 | P1 A／B 重疊 | `owned_by_B` 歸 B，A 不得修改；收尾依賴 #31 與 A 修補全部完成（§6-2、§6-6）。 |
| 14:02 | P1 逐批驗收 | 每批 text 寫明範圍、做法、編譯／語意驗收與不適用理由（plans）。 |
| 14:02 | P2 試算證據 | 降為推測（§6-1）。 |
| 14:02 | P2 範圍 | 任務擁有者調整 Issue 範圍。 |
| 14:30 | P1 T029 契約互斥 | 以正規化 javap 逐方法比對後確認：7 個方法正式與候選只差 T029 項（whole-formal），`exitRoomBeforePolyMe(Z)` 只在正式版且等於候選 `exitRoom(Z)`（formal-only-method）。8 個方法都對正式比對要求 SAME，與工具契約一致（§6-5）。 |
| 14:30 | P1 只驗歷史差異 entry | 每批改為 `verify_entries`（從候選展開外層＋全部 `$` 內部類別，合計 1,384）、`history_entries`（623，僅參考）、`remove_entries`（皆 0）；驗收要求無缺漏、無多出；B-land 與總比對使用同一展開（§6-4、§6-5、§6-6）。 |
| 14:30 | P2 報告與數字 | 本報告改為第 4 版最終狀態；成員差異數字改由 `plan_batches.py` 計算並寫入 `batches.json`；§6 開頭明訂取代 §4 的 ≤30。 |

## 已驗證
- 輸入：候選 `reference/candidate-50635f7e.jar`（取自 `task/45`，SHA256 `50635f7edca8ec6ce77787cc32371c7590b497e48ea202f8a679caf3f517b729`）、正式 `reference/base-0.6.71.jar`（`e1498871c7be6b287821c57ef5d95933db1740d9b4d4a80f6c3465f21d99d498`）、`class-diff.json`（`task/30` @ `31b5233`）。
- 指令（repo 根目錄，需 JDK 的 `javap` 在 PATH、`LC_ALL=C.UTF-8`）：
  ```bash
  python3 lito伺服器/LiTo381-Server/tools/semantic-diff/plan_batches.py \
    --candidate lito伺服器/LiTo381-Server/reference/candidate-50635f7e.jar \
    --owned docs/combat-merge/batches.json --diff <task/30 的 docs/combat-merge/class-diff.json> \
    --formal lito伺服器/LiTo381-Server/reference/base-0.6.71.jar \
    --t029-out docs/combat-merge/t029-methods.txt --max 60 --out docs/combat-merge/batches.json
  ```
- 輸出（寫在 `batches.json`）：303 nodes、16 SCC、最大 282、去核心最大 248；候選 entry 1,384；既有 entry 新增成員 107 個 entry／62 個外層類別／518 個成員，171 個外層類別使用它們；各批 `verify_entries` 22／108／135／98／120／323／56／160／362（合計 1,384），`remove_entries` 皆 0；T029 8 個方法與分類。
- 以 JDK 21 的 javap 執行。正規化規則見腳本 `javap_methods()`。

## 推測／未驗證
- Maven 產物與正式的語意差異量（待 #44 T020d）。
- 各批能否在暫存區編譯成功、反編譯／移植後能否達到 SAME（各批任務驗證）。
- B-land 後既有單元測試的失敗量（B-land 處理）。

## 產出路徑
`docs/design/T020-combat-merge.md` §6、`docs/combat-merge/batches.json`、`docs/combat-merge/t029-methods.txt`、`docs/plans/issue-43.json`、`lito伺服器/LiTo381-Server/tools/semantic-diff/plan_batches.py`、`docs/reports/issue-43.md`

## 給下一棒的提示
B01–B09 等 #44（工具，需支援完整 entry 清單、`--allow-methods` 中不在 `--right` 的方法只和 `--allow-right` 比）與 #45（JAR 入庫）進 main 後由主控建 Issue，可並行。
