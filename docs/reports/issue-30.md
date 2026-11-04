# Issue #30：T020a 本機類別差異盤點（第 3 次嘗試）

**結論：新增四條歷史父鏈，已接至 0.6.35 固定輸入；NativeArmorStatus 的真正最後寫入層及歷史來源仍缺件，驗收未通過。**

## 做了什麼／已驗證

- 直接在既有 `task/30` worktree 工作，已讀根目錄、兩個子專案規範、維護規範及 docs/REPORT.md；fetch `origin/task/24` 讀取設計文件。未 checkout／建分支。
- 三個指定 JAR SHA 再次吻合，正式 Git blob 為 `d59ae2e4c56f599456e764a205a0e09a825975d3`，未替換比較輸入。
- `mvn -B -DskipTests package` 於 2026-11-04 21:30:00 +08:00 成功：JDK 17.0.17／Maven 3.9.16，重編 940 個主來源、1,413 個測試來源，測試略過。快取及暫存位於 worktree scratch，產物位於 target；3,929 個 class bytes 與前輪全部相同。

| 輸入 | SHA-256 |
|---|---|
| 8d26 候選 | `8d26c9d791e094ab3d3cd6c8dbe4218d99f96fcbe73858be5af534106c81cfb8` |
| 50635 negative final frame 候選 | `50635f7edca8ec6ce77787cc32371c7590b497e48ea202f8a679caf3f517b729` |
| 正式 base-0.6.71.jar | `e1498871c7be6b287821c57ef5d95933db1740d9b4d4a80f6c3465f21d99d498` |
| 本輪 Maven LiTo381-Server.jar | `0aefa242a7889e3dc1cad348c4d9650579325503e51c3efaa70e49e631182866` |

## 怎麼驗證

| 比較（待比較端 → 參考端） | SAME | CHANGED | NEW_IN_CANDIDATE | ONLY_IN_FORMAL |
|---|---:|---:|---:|---:|
| 8d26 → 正式 | 2546 | 4 | 0 | 4 |
| 50635 → 8d26 | 2410 | 140 | 483 | 0 |
| Maven → 正式 | 1008 | 1541 | 1380 | 5 |

- 重新核對 9,521 列分類、雙邊 class SHA、大小、來源與 DECOMPILED 標記；T029 的 8 筆清單及各組前 50 排名保留。
- 原有 59 條 resource 父鏈與 12 條早期父鏈重驗通過；GmTravel 復原 Java 與隔離 class SHA 仍符合歷史收據與候選。
- **新增四條邊**：`004b1ba8 → eeb0ba0b → 8b9f0319 → 4a3bb9be → 3f31ef32`。前三條來自登入器 `server-patches/repair-042/041/040` 的固定 JAR、build.py／Patch.java 父 SHA；第四條由 `layout-cleanup-20261012.json` 的 removed entry 證明 shared-database 歷史輸出等於保留的 repair-040/base.jar，再以 builder／PatchShared 父 SHA 銜接 0.6.35。
- 四條邊不同 class 數依序為 7／4／3／10；NativeArmorStatus 全部不在其中，SHA 均為 `c2da4b0cb66fcbb629090d82c2008e1ed79f16525b2bcf10cc091bd2128b7b1f`。早期父鏈現為 16 條。完整路徑、SHA、清理收據 JSON pointer、pin 行與差異清單已內嵌 `attempt3_review.additional_parent_edges`。
- 限定搜尋登入器 tools／server-patches／docs、伺服器早期 tools／docs：562 個文字檔、19 個命中；未取得 3f31ef32 之前的新父鏈或完整歷史來源。搜尋範圍、排除項及命中 SHA 已內嵌 `attempt3_review.source_search`。未進入封存。
- 重現：`py -3 -B -X utf8 scratch/issue-30/attempt3-trace.py`、`attempt3-launcher-search.py`、`verify-attempt3.py`（後兩支同目錄）。父鏈核對實際結果為 `PASS 4 earlier edges, armor unchanged; origin still missing`；驗證通過僅代表資料一致，不代表來源驗收完成。

## 推測

- 前輪現有 Java 重編的 NativeArmorStatus SHA `a93dbd3d…` 不等於候選 `c2da4b0c…`；33 個正規化方法指令清單相同僅可提供移植線索，不證明歷史來源。
- Maven 的 bytes 差異可能包含編譯資訊差異；未判定全部語意等價。

## 待驗證／未完成

- **唯一驗收阻塞仍為 historical-provenance-missing**。新的連續邊界是 `lito伺服器/LiTo381-Server/tools/shared-database/build/base-0.6.35.jar`，SHA `3f31ef32c8ca1ca8f9507897f03b54ca37593326ad986b5c10fcff68f695ed5c`。缺其之前真正最後寫入層及完整歷史 Java／可驗證 ASM 修改證據；`last_layer`、`source` 仍為 null，`acceptance_passed=false`、`unresolved_entries=1`。
- 未重播 patch 鏈、未重編第二批 380 筆凍結來源、未執行單元測試或完整候選回歸。未啟動伺服器／登入器／遊戲，未連 DB。
- 未改 src/main、正式 JAR、docs/REPORT.md；未讀機密設定、封存及硬連結，未複製 pak/map/sprite、未部署。

## 產出路徑

- [class-diff.json](../combat-merge/class-diff.json)：9,521 列分類與來源、父鏈、T029 清單及本輪補證。
- [class-diff.md](../combat-merge/class-diff.md)：分類計數、前 50 差異與來源限制。
- 本報告：`docs/reports/issue-30.md`。
- 重現腳本與建置 log：`scratch/issue-30/`（不入 git）。

## 給下一棒的提示

- **Issue #30 任務擁有者／雲端主控**：本輪已把父鏈從 004b1ba8 接至 3f31ef32。下一步補 0.6.35 之前可銜接的父鏈、真正最後寫入來源及候選 class SHA 證據，放允許讀取的 sources/inputs；不要要求使用者查資源或擅入封存。
- **調度器**：保持未完成；這是來源缺件，不是沙箱偶發失敗。取得上述新證據前不要無條件自動重排。
- **Claude／T020b**：可依既有 380 Java／243 ASM entry 規劃第二批，但不可把 1 筆來源阻塞隱藏或視為已通過。
- **T029**：保留 4 個正式獨有 entry；GmTravel／NativeArmorStatus 為正式改、候選未改，ClientSession／Protocol 需三方合併。不得以候選舊 AC 限制覆蓋正式修正。
- **T020c**：依設計 §2-4 執行全部同 SHA 回歸；靜態盤點及 package 不代替行為驗收。
