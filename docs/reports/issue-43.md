# Issue #43 / T020b — 戰鬥合併批次（設計）

## 做了什麼
- `docs/design/T020-combat-merge.md` 新增 §6：依 T020a 結果改寫前提（正式已含第一批；`src/main` 與正式不等價），定兩階段做法、共用前置（T020d 比對工具、T020f 候選入庫）與第二階段 9 個批次。
- `docs/combat-merge/batches.json`：303 個外層類別的批次歸屬。
- `docs/plans/issue-43.json`：13 個後續任務與相依關係。

## 怎麼驗證
- 已驗證：批次由 `class-diff.json`（`task/30`）的 `candidate_5063_vs_8d26` 非 SAME 列（623 筆）彙整為 303 個外層類別：新類別 218（完整 Java 156、混合 45、asm-only 17）、既有類別改動 85（Battle 29、Combat 12、MaterialAction 11、Native 等 30、核心 3），其中 19 個在 `src/main` 為 DECOMPILED。`batches.json` 總數 303。
- 已驗證：在雲端以 `mvn -B -DskipTests -Dmaven.test.skip=true package` 建出 Maven 產物，和 `reference/base-0.6.71.jar` 做 `javap -c -p -constants` 比對（去常數池編號與空白），2,538 個共同類別中約 593 筆不同；其中約 162 筆是批次解析失敗，真正不同的約 430 筆。這是上限估計，正式數字由 T020d 工具產出。
- 推測：約 430 筆裡有一部分是 lambda 編號、合成方法等編譯器雜訊，T020d 正規化後應會下降。

## 未驗證什麼
- 各批單獨能否編譯（設計已允許提前帶入相依類別）。
- 第一階段實際修補量（待 T020d）。

## 產出路徑
`docs/design/T020-combat-merge.md`、`docs/combat-merge/batches.json`、`docs/plans/issue-43.json`、`docs/reports/issue-43.md`

## 給下一棒的提示
T020d 與 T020f 可並行；B2 起的批次都等這兩個完成。T020e 由主控在 T020d 出報告後做。
