# Issue #30：T020a 本機類別差異盤點

**結論：沙箱故障已恢復；盤點與建置已重新核對，但 NativeArmorStatus 仍缺最後寫入層的歷史證據，完整驗收未通過。**

## 做了什麼／已驗證

- 直接在既有 `task/30` worktree 工作，未 checkout／建分支。已讀根目錄 AGENTS.md、CLAUDE.md、docs/REPORT.md、伺服器規範及維護規範。fetch `origin/task/24` 後讀取設計文件。
- 三個指定輸入 SHA 全部吻合；正式 Git blob 為 `d59ae2e4c56f599456e764a205a0e09a825975d3`。未替換輸入，未使用歷史 75da 作基準。
- 執行 `mvn -B -DskipTests package` 成功（2026-11-04 21:07:15 +08:00，JDK 17.0.17／Maven 3.9.16），重編 940 個主來源、1,413 個測試來源。依指令略過測試，快取與暫存在 worktree scratch，建置產物在 target。
- 新 Maven 產物的 3,929 個 class bytes 與前輪全部相同。三組共 9,521 列的分類、雙邊 SHA、大小、來源及 DECOMPILED 標記重新核對；保留 T029 的 8 筆清單與各組前 50 大差異。

| 輸入 | SHA-256 |
|---|---|
| 8d26 候選 | `8d26c9d791e094ab3d3cd6c8dbe4218d99f96fcbe73858be5af534106c81cfb8` |
| 50635 negative final frame 候選 | `50635f7edca8ec6ce77787cc32371c7590b497e48ea202f8a679caf3f517b729` |
| 正式 base-0.6.71.jar | `e1498871c7be6b287821c57ef5d95933db1740d9b4d4a80f6c3465f21d99d498` |
| 本輪 Maven LiTo381-Server.jar | `e63ab991176cdc7128f10d7e43f2ebba28594411d831d99f1929066575f9b706` |

## 怎麼驗證

| 比較（待比較端 → 參考端） | SAME | CHANGED | NEW_IN_CANDIDATE | ONLY_IN_FORMAL |
|---|---:|---:|---:|---:|
| 8d26 → 正式 | 2546 | 4 | 0 | 4 |
| 50635 → 8d26 | 2410 | 140 | 483 | 0 |
| Maven → 正式 | 1008 | 1541 | 1380 | 5 |

- 完成前輪未走完的限定來源搜尋：伺服器 tools／src／docs、根 tools、登入器 docs 共 516,622 個檔名，讀取 2,369 個文字檔。工具目錄 513,250 個檔名沒有額外 InventoryCatalog／NativeArmorStatus Java，唯一命中是既有 src/main。7 筆早期 SHA 命中沒有新增父鏈。搜尋範圍、排除項、命中路徑與 SHA 均嵌入 JSON `rerun_review.complete_source_search`，不依賴未提交 scratch 才能閱讀結論。
- 現有 InventoryCatalog.java SHA 為 `338a3a4299bd35499cae6be58a7c71628c1cccfa0d3ee3f3ab871fdc7f444bc9`。隔離 javac 產生 NativeArmorStatus SHA `a93dbd3d007e9c6504667e99db85218118f95d0138b928e65d6a905a8adbc121`，與候選 `c2da4b0cb66fcbb629090d82c2008e1ed79f16525b2bcf10cc091bd2128b7b1f` 不同。
- javap 方法指令按簽名配對、正規化常數池索引後，33 個方法清單相同。此核對沒有涵蓋所有 class attributes，不作完整等價或歷史來源證明；`source` 與 `last_layer` 維持 null，另提供 `current_source_probe` 線索。
- 前輪 GmTravel 復原證據保留並重新核對：Java SHA `319b3d460756991907cc03b3a662c15b4212a92ffbfc52109f840fd45a624ffe` 符合歷史收據，隔離 class SHA `8561ad6243444e94417b924e1928e90937b7b481e9eab5eabb77311a3a8dd985` 符合候選。59 條 resource 父鏈、12 條早期父鏈與來源檔案 SHA 重新驗證。
- 來源驗收仍為 `acceptance_passed=false`、`unresolved_entries=1`；驗證器通過只表示已記錄證據一致。

## 推測

- 現有 InventoryCatalog 的 NativeArmorStatus 方法可作後續移植參考；指令清單一致不足以認定它是歷史最後寫入層的完整來源。
- Maven 與正式的 CHANGED 可能含編譯資訊差異；本任務沒有判定全部差異語意等價。

## 待驗證／未完成

- NativeArmorStatus 最後寫入層及其來源／ASM 證據仍缺。可銜接血緣停於 `tools/source-administration/build/base.jar`（正式 0.6.42，SHA `004b1ba8cc258f2f95a04ed3e6f1cbd63c1fe7737dc3db5d6b4c0de009130951`）；0.6.35 固定輸入 `3f31ef32…` 含相同 class，但尚未證明血緣。不能將未知來源偽填 `asm-only`。
- 未執行單元測試、整條 patch 鏈、第二批 380 筆來源重編或完整候選回歸，未啟動伺服器／登入器／遊戲、未連 DB。
- 未改 src/main、正式 JAR、docs/REPORT.md；未讀取機密設定、封存或硬連結，未複製 pak/map/sprite、未部署。

## 產出路徑

- [class-diff.json](../combat-merge/class-diff.json)：9,521 列分類、來源與父鏈、T029 清單及本輪證據。
- [class-diff.md](../combat-merge/class-diff.md)：分類計數、各組前 50 大差異及來源限制。
- 本報告：`docs/reports/issue-30.md`。
- 本機一次性腳本、搜尋證據與建置 log：`scratch/issue-30/`（不入 git）；隔離 class 在伺服器 `target/issue30-armor-source-validation/`（不入 git）。

## 給下一棒的提示

- **調度器／雲端主控**：本輪已成功執行，剩下的是 `historical-provenance-missing`，不是沙箱偶發錯誤。請按來源缺件交任務擁有者處理；無新證據的自動重排不會解除驗收阻塞。
- **Issue #30 任務擁有者**：補齊 004b1ba8 之前可銜接的父鏈及 NativeArmorStatus 最後寫入層的 Java／ASM 證據，放允許讀取的 sources/inputs 後再派本機核對。不得擅入封存，也不要要求使用者查資源。
- **Claude／T020b**：第二批 623 個 entry 保留 380 筆 Java／243 筆 ASM 分類；可用來規劃批次，但仍須明列 1 筆來源驗收阻塞。
- **T029**：保留 4 個正式獨有 entry；GmTravel／NativeArmorStatus 為正式改、候選未改，ClientSession／Protocol 需三方合併。現有 InventoryCatalog 也含候選的舊 AC 限制，不能直接覆蓋正式修正。
- **T020c**：依設計 §2-4 執行全部同 SHA 回歸；靜態盤點與 package 不代替行為驗收。
