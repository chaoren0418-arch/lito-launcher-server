# Issue #30：T020a 本機類別差異盤點

**第 2 次補證結論：未完成，來源驗收仍有 2 筆阻塞。GmTravel 的最後寫入層已查明，但完整 Java 缺件；NativeArmorStatus 的最後寫入層與來源仍缺件。請任務擁有者補件，不視為驗收通過。**

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

## 第 2 次補證／實際核對結果

- 前次已驗證的 9,521 列、三個固定輸入、Maven 產物與建置 log SHA 再次核對通過；本輪未重新 package，保留前次產物。未修改比較數量或 T029 保留清單。
- 68 個第一批戰鬥鏈節點＋59 條 resource-database 父鏈邊均核對 SHA 與兩個 entry bytes；已修正前次因不同收據 schema 而停在 efd4 凍結副本的追溯缺口。
- `lito伺服器/LiTo381-Server/tools/resource-database/build/build-manifest.json` 記錄 GmTravel 的最後寫入層、完整 Java 歷史路徑及期望 SHA；凍結 classes 的 GmTravel.class 與候選 SHA 相同，但現版 Java SHA `ee071a58…` 不符收據，未當作有效來源。
- NativeArmorStatus 經 room-rules → durable-settlement → native-room-admission → native-room-state 的 4 條父鏈邊均保持原 bytes；每層 `evidence.json`、`build.py`、父子 JAR 的路徑及完整 SHA 已寫入 `class-diff.json` 的 `precombat_trace`。
- 前戰鬥 tools 的限定來源搜尋只找到 GmTravelPatch.java，它修改 ClientSession 的掛鉤；無法代替 GmTravel 完整來源。未搜尋封存或讀取機密設定。
- 可重現唯讀指令：在此 worktree 依序執行 `& 'C:\Users\CHAOREN\AppData\Local\Programs\Python\Python312\python.exe' -B -X utf8 scratch/issue-30/retry_verify.py` 與同 Python 的 `-B -X utf8 scratch/issue-30/validate.py`。前者回報 68 節點、59 條父鏈、GmTravel origin／source SHA mismatch、4 條早期父鏈 PASS，exit 0；JSON status 仍為 `blocked_missing_historical_sources`。後者 9,521 列 PASS，但仍有 2 筆 provenance exceptions，不能把腳本通過誤認為驗收完成。
- 完整缺件 SHA、收據與來源路徑、指令及實際結果見 [class-diff.md 的第 2 次補證](../combat-merge/class-diff.md#第-2-次補證已驗證的來源邊界與缺件)；本機驗證腳本／log SHA 見 JSON `scripts`／`retry_review`。

## 推測

- 正式 JAR 和第一批候選高度重合，但 Maven 與正式不等價；目前來源合併工作不能因第一組只有 8 筆差異就略過。
- Maven bytes 差異包含編譯資訊與來源形狀變化的可能性；本次未證明任何 CHANGED 類別語意相同。

## 未驗證／未完成

- **2 筆歷史來源例外仍未解除**：GmTravel 已透過同 SHA 父鏈與 resource-database/build/build-manifest.json 證明在 efd4bc36 層編譯新增，但缺少 SHA `319b3d460756991907cc03b3a662c15b4212a92ffbfc52109f840fd45a624ffe` 的完整 Java；現版同路徑 SHA 不符。NativeArmorStatus 已再追至 native-room-state/build/base.jar（`e5f2e01b…`），沿途 entry 不變，仍缺之前的最後寫入層與完整來源／ASM 修改證據。JSON 兩列 source 均保持 null，未冒稱 asm-only。
- 未跑單元測試、完整候選回歸、啟動伺服器／登入器／遊戲、正式 DB 或真機顯示。未修改 src/main、正式 JAR、機密設定、docs/REPORT.md、封存或 hardlink。

## 產出路徑

- [class-diff.json](../combat-merge/class-diff.json)：逐 class 與血緣證據。
- [class-diff.md](../combat-merge/class-diff.md)：分類計數、每組前 50 大差異、T029 清單與限制。
- 本報告：`docs/reports/issue-30.md`。
- 本機可重跑腳本與 log：`scratch/issue-30/`（依規則不入 git）；建置產物 `lito伺服器/LiTo381-Server/target/LiTo381-Server.jar`（不入 git）。

## 給下一棒的提示

- **Claude／T020b**：按第二批 623 個 entry 的最後層與 Java/ASM 分類拆批，每批 ≤30；先確認計數口徑是 class entry，內部類別應與外層同行。29 個 entry 對應 19 個 DECOMPILED 檔。
- **Issue #30 任務擁有者／雲端主控**：本次仍未達逐列來源驗收。請提供 SHA `319b3d46…` 的歷史 GmTravel.java，以及 e5f2e01b 固定輸入之前能銜接 NativeArmorStatus 的父鏈收據與最後層完整 Java／ASM 證據，放在允許讀取的 sources/inputs。由本機執行器核對後再送審；不要派使用者查找資源，也不要以現版來源或別的候選替代。
- **T029**：保留 4 個正式獨有 entry；GmTravel／InventoryCatalog$NativeArmorStatus 是正式改、候選未改，ClientSession／Protocol 需三方合併。職業圖示與 litoddraw.dll 的外部部分另依既有發布收據驗證。
- **T020c**：本次只有 package 與靜態盤點，仍須跑設計 §2-4 指定的全部同 SHA 套件；不可把 50635 的既有針對性案例當成全回歸。
