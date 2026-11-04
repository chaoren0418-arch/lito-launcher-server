# Issue #30：T020a 本機類別差異盤點

## 做了什麼／已驗證

- 在既有 `task/30` worktree 執行；未 checkout 或建立分支。先讀 AGENTS.md、CLAUDE.md、docs/REPORT.md 與伺服器子目錄規則。設計取自 origin/task/24 @ `e22872064e41d49dfa3f87dfd95ebf0eb4df39e2`。
- 三個指定輸入全部通過 SHA 閘門；沒有以歷史 `75da69b0…` 或其他候選替代。
- `mvn -B -DskipTests package` 成功，主來源 940 檔、測試來源 1,413 檔完成編譯；依參數未執行測試。使用 JDK 17.0.17 與 Maven 3.9.16。快取副本與暫存均在 worktree scratch；產物在 target。
- 逐 entry 實際 bytes 比較三組共 9,521 列；每列含分類、大小、雙邊 SHA、最後內容層／來源資訊、src/main 路徑及 DECOMPILED。JSON 另含父鏈收據 SHA、完整 Java 源碼 SHA 與 T029 保留清單。

| 輸入 | SHA-256 |
|---|---|
| 8d26 候選 | `8d26c9d791e094ab3d3cd6c8dbe4218d99f96fcbe73858be5af534106c81cfb8` |
| 50635 negative final frame 候選 | `50635f7edca8ec6ce77787cc32371c7590b497e48ea202f8a679caf3f517b729` |
| 正式 base-0.6.71.jar | `e1498871c7be6b287821c57ef5d95933db1740d9b4d4a80f6c3465f21d99d498` |
| Maven LiTo381-Server.jar | `b076330de235c45ae50185116d17cab4d78bd72b9d1b3a98d9ec0b188e7921a2` |

## 怎麼驗證

- 8d26 → 正式：SAME 2,546／CHANGED 4／NEW 0／ONLY_IN_FORMAL 4。
- 50635 → 8d26：SAME 2,410／CHANGED 140／NEW 483／ONLY_IN_FORMAL 0。
- Maven → 正式：SAME 1,008／CHANGED 1,541／NEW 1,380／ONLY_IN_FORMAL 5。
- 第一批／第二批實際戰鬥鏈 67／289 個層，父 JAR 的 SHA 逐層通過；沒有以檔案時間或腳本名排序代替實際血緣。
- 第二批 380 個完整 Java entry 的來源 SHA 符合收據，凍結 classes 輸出與最終候選逐 entry SHA 一致；243 個最後層為 ASM。
- 用 javap 靜態確認正式 GM 入口／離房清理、七職業與娃娃圖示封包、選角恢復、裝備 AC 顯示差異；未執行候選程式。
- Windows 沙箱初始化失敗，改用經工具核准的提升權限命令執行上述工作；原始專案資源保持唯讀。

## 推測

- 正式 JAR 和第一批候選高度重合，但 Maven 與正式不等價；目前來源合併工作不能因第一組只有 8 筆差異就略過。
- Maven bytes 差異包含編譯資訊與來源形狀變化的可能性；本次未證明任何 CHANGED 類別語意相同。

## 未驗證／未完成

- **2 筆歷史來源例外**：GmTravel、InventoryCatalog$NativeArmorStatus 在 §1 整條戰鬥鏈都未變動；已追到更早 resource-database 的 baseline-PC-ingress-efd4-v2，仍無可銜接同 SHA 父收據，未能確定最初最後寫入層／完整候選來源。JSON 用 source=null 與 inherited-before-combat-root 明示，沒有偽填 asm-only。嚴格「每個 CHANGED 都有完整歷史層與來源」驗收尚餘這 2 筆。
- 未跑單元測試、完整候選回歸、啟動伺服器／登入器／遊戲、正式 DB 或真機顯示。未修改 src/main、正式 JAR、機密設定、docs/REPORT.md、封存或 hardlink。

## 產出路徑

- [class-diff.json](../combat-merge/class-diff.json)：逐 class 與血緣證據。
- [class-diff.md](../combat-merge/class-diff.md)：分類計數、每組前 50 大差異、T029 清單與限制。
- 本報告：`docs/reports/issue-30.md`。
- 本機可重跑腳本與 log：`scratch/issue-30/`（依規則不入 git）；建置產物 `lito伺服器/LiTo381-Server/target/LiTo381-Server.jar`（不入 git）。

## 給下一棒的提示

- **Claude／T020b**：按第二批 623 個 entry 的最後層與 Java/ASM 分類拆批，每批 ≤30；先確認計數口徑是 class entry，內部類別應與外層同行。29 個 entry 對應 19 個 DECOMPILED 檔。
- **本機資源執行器**：補齊上述 2 筆戰鬥鏈前歷史來源。避免直接取 src/main 現版冒充候選，亦不可改用非指定 SHA。
- **T029**：保留 4 個正式獨有 entry；GmTravel／InventoryCatalog$NativeArmorStatus 是正式改、候選未改，ClientSession／Protocol 需三方合併。職業圖示與 litoddraw.dll 的外部部分另依既有發布收據驗證。
- **T020c**：本次只有 package 與靜態盤點，仍須跑設計 §2-4 指定的全部同 SHA 套件；不可把 50635 的既有針對性案例當成全回歸。
