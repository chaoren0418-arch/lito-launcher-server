# Issue #59 / T020e — A 階段（src/main→正式 0.6.71）修補批次（設計）

## 做了什麼
- `docs/design/T020-combat-merge.md` 新增 §7：A 階段範圍、相依分析、暫存區做法、6 個批次、逐批驗收、A-land。
- `lito伺服器/LiTo381-Server/tools/semantic-diff/plan_a_batches.py`：從 gap 報告產生固定批次（重用 `plan_batches.py` 的常數池解析與 Tarjan）。
- `docs/combat-merge/a-batches.json`：6 批、224 個外層類別、692 個驗收 entry。
- `docs/plans/issue-59.json`：A01–A06（可並行）與 A-land。

## 怎麼驗證
- 已驗證：指令（repo 根目錄，main @ `e79919e`）
  ```bash
  python3 lito伺服器/LiTo381-Server/tools/semantic-diff/plan_a_batches.py \
    --gap docs/server-semantic-gap.md --owned docs/combat-merge/batches.json \
    --formal lito伺服器/LiTo381-Server/reference/base-0.6.71.jar \
    --src lito伺服器/LiTo381-Server/src/main/java/local/lito381/server \
    --max 50 --out docs/combat-merge/a-batches.json
  ```
  輸入：gap 報告（CI run 41852194064）、`batches.json`（#43，303 個 owned_by_B）、正式 JAR `e1498871…`。輸出：DIFF 485＋ONLY_RIGHT 5；屬 B 的 203＋2；A 目標 285 entry／224 外層類別（171 DECOMPILED）；驗收 692 entry；127 SCC、最大 89；批次 49／49／47／25／27／27。
- 已驗證：批次涵蓋 224 個外層類別，無重複、無遺漏（腳本內 assert）。

## 未驗證什麼
- 各批反編譯／修正後能否達到 SAME（各批任務驗證）。
- A-land 後既有單元測試的失敗量。

## 產出路徑
`docs/design/T020-combat-merge.md` §7、`docs/combat-merge/a-batches.json`、`docs/plans/issue-59.json`、`lito伺服器/LiTo381-Server/tools/semantic-diff/plan_a_batches.py`、`docs/reports/issue-59.md`

## 給下一棒的提示
A01–A06 不依賴 #45（只用已在 main 的正式 JAR 與 T020d 工具），可以立即並行派工。
