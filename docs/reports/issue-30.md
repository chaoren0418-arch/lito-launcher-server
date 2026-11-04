# Issue #30：T020a 本機類別差異盤點

**結論：本輪已解除 GmTravel 歷史來源缺件，NativeArmorStatus 仍有 1 筆來源阻塞；尚未通過完整驗收。**

## 做了什麼／已驗證

- 在既有 `task/30` worktree 工作，未 checkout／建分支。已讀 AGENTS.md、CLAUDE.md、docs/REPORT.md 與子專案規則；fetch 後設計分支仍是 `e22872064e41d49dfa3f87dfd95ebf0eb4df39e2`。
- 三個指定 JAR SHA 全部符合；正式基準 blob 符合 `d59ae2e4c56f599456e764a205a0e09a825975d3`，沒有改用歷史 75da 或其他候選。
- 重新執行 `mvn -B -DskipTests package` 成功，JDK 17.0.17、Maven 3.9.16；940 個主來源與 1,413 個測試來源重編，依參數略過測試。快取與暫存在 worktree scratch，產物只在 target。
- 新 Maven JAR 的 3,929 個 class bytes 與前次產物完全相同。三組共 9,521 列分類、大小、SHA 與 DECOMPILED 標記已重新核對。

| 輸入 | SHA-256 |
|---|---|
| 8d26 候選 | `8d26c9d791e094ab3d3cd6c8dbe4218d99f96fcbe73858be5af534106c81cfb8` |
| 50635 negative final frame 候選 | `50635f7edca8ec6ce77787cc32371c7590b497e48ea202f8a679caf3f517b729` |
| 正式 base-0.6.71.jar | `e1498871c7be6b287821c57ef5d95933db1740d9b4d4a80f6c3465f21d99d498` |
| 本輪 Maven LiTo381-Server.jar | `593aa88ca7468342f6d66f62d6fd9f4e79d0a83d542f21769bb6e7459442b030` |

## 怎麼驗證

| 比較（待比較端 → 參考端） | SAME | CHANGED | NEW_IN_CANDIDATE | ONLY_IN_FORMAL |
|---|---:|---:|---:|---:|
| 8d26 → 正式 | 2546 | 4 | 0 | 4 |
| 50635 → 8d26 | 2410 | 140 | 483 | 0 |
| Maven → 正式 | 1008 | 1541 | 1380 | 5 |

- **GmTravel 已解除缺件**：現版只刪除正式新增的 `GmPolyMe.command` 那一行及 CRLF，復原 Java SHA 為 `319b3d460756991907cc03b3a662c15b4212a92ffbfc52109f840fd45a624ffe`，精確符合歷史 `builder_inputs`。隔離 javac 產出的 class SHA 為 `8561ad6243444e94417b924e1928e90937b7b481e9eab5eabb77311a3a8dd985`，也精確符合指定候選。原 `src/main` 未改。
- **NativeArmorStatus 新增 8 條早期父鏈證據**，加上前次 4 條共 12 條；銜接至 `tools/source-administration/build/base.jar`，SHA `004b1ba8cc258f2f95a04ed3e6f1cbd63c1fe7737dc3db5d6b4c0de009130951`（正式 0.6.42）。沿途 entry 保持 `c2da4b0cb66fcbb629090d82c2008e1ed79f16525b2bcf10cc091bd2128b7b1f`。
- 更早固定 `tools/shared-database/build/base-0.6.35.jar`（`3f31ef32…`）也含相同 entry，但缺可銜接父鏈，只作獨立觀測，不冒充最後寫入層。
- Git 來源歷史、262 檔早期工具／維護文件、178 檔 docs 邊界搜尋已完成限定核對；90 秒廣域搜尋則不完整，沒有用它推論全專案不存在來源。
- 第二批原有 380 筆完整 Java／243 筆 ASM 分類、59 條 resource 父鏈及 8 筆 T029 保留清單均保留；完整 Java 收據／凍結輸出驗證結果保留。未重播整條 patch 鏈。
- 以 `scratch/issue-30/verify-current.py` 核對分類、雙邊 SHA、完整來源 SHA、DECOMPILED、GmTravel 復原／輸出、父鏈證據與報告範圍。驗證器通過不等於來源驗收通過，JSON `acceptance_passed=false`、`unresolved_entries=1`。
- 一般 Windows 沙箱初始化失敗，改用經自動核准審查的命令執行；原始專案僅讀取。

## 推測

- 正式 JAR 與第一批候選高度重合，但 Maven 與正式不等價；不能因第一組只有 8 筆差異就省略來源合併。
- Maven class 差異可能包含 debug 資訊與來源結構差異；本次不判定任何 CHANGED 類別語意等價。

## 未驗證／未完成

- **剩餘 NativeArmorStatus 1 筆**：尚未找到正式 0.6.42 之前可銜接的最後寫入層及完整 Java／ASM 證據。JSON 維持 `last_layer=null`、`source=null` 與明確原因，沒有偽填 asm-only。
- 未執行單元測試、完整候選回歸、伺服器／登入器／遊戲、正式 DB 或真機顯示。第二批完整來源沒有重新編譯。
- 未改 src/main、正式 JAR、docs/REPORT.md；未讀取機密設定、封存或硬連結，未複製 pak/map/sprite，未部署。

## 產出路徑

- [class-diff.json](../combat-merge/class-diff.json)：9,521 列差異、來源／復原程序、父鏈收據與 T029 清單。
- [class-diff.md](../combat-merge/class-diff.md)：分類計數、各組前 50 大差異、來源證據與限制。
- 本報告：`docs/reports/issue-30.md`。
- 本機腳本、搜尋證據、復原 Java、建置 log：`scratch/issue-30/`（不入 git）。隔離編譯輸出：`lito伺服器/LiTo381-Server/target/issue30-source-validation/`（不入 git）。

## 給下一棒的提示

- **Issue #30 任務擁有者／雲端主控**：只需再補 NativeArmorStatus。提供 004b1ba8 固定輸入之前的可銜接父鏈與最後層來源／ASM 證據，放在允許讀取的 sources/inputs，再交本機核對。0.6.35 的 3f31ef32 已含相同 bytes，可作查找線索，但未證明血緣；不要讓使用者查資源，也不要擅入封存。一般重排執行不會自動補齊歷史證據。
- **Claude／T020b**：第二批 623 個 entry 可依最後層與 Java/ASM 拆批（每批 ≤30），29 個 entry 對應 19 個 DECOMPILED 檔；仍須保留上述來源驗收阻塞。GmTravel 復原操作與完整 SHA 已嵌入 JSON，不依賴提交額外 Java。
- **T029**：保留 4 個正式獨有 entry；GmTravel／NativeArmorStatus 是正式改、候選未改，ClientSession／Protocol 需三方合併。GmTravel 的候選來源可精確復原，不代表合併時可刪除正式版 GmPolyMe 入口。
- **T020c**：另跑設計 §2-4 的全部同 SHA 回歸；本次 package／靜態比較不代替行為驗收。
