# Issue #259　T020i-12 stack／暫存資料流與 switch-map 內容配對

## 做了什麼

沿用 worktree `task-259` 與分支 `task/259`，起點為 `8df7c89c3a9b4dbdb8eb01b40a5b53462dbac0f8`，其中已包含 #254。此次依 #70 `989417ab` 的 B09 附錄，兌現 PLAN.md M2 不確定項 U2-1 的第 2 張，達到原估計 2 張。後續比較器缺口交主控依估計調整流程裁定。

1. `TemporaryValues` 在 LocalWebs 切開槽位重用後，檢查唯一寫入、每次讀取的唯一 reaching definition、型別與寬度、wide 重疊、指令註解及 handler 可達區域。符合條件的跨區塊暫存以保存值綁定表示，按生產者編號放在正常區塊出口。
2. `BlockExpressions` 以原值表示區塊內 local／stack 搬移，純常數保留型別及完整值。ASM Frame 執行 DUP 系列與 SWAP，保留寬度、operand 順序及跨邊 stack 關聯。單次寫入而完全無讀取的保存值可消去，生產者與其他 stack 用途仍保留。呼叫、欄位／陣列操作、monitor、throw、return、分支與例外邊比較完整有序事件及引數。
3. `LocalTransport` 擴充既有唯一前驅 null-return 證明，接受中間 CHECKCAST。cast 的位置與例外涵蓋仍受完整比較。
4. 移除舊 `tempStores` 的 DUP 配額摘要消除。連續 ASTORE 改由完整值來源證明處理。effects 的區塊證明失敗時，原核准摘要保留全部 ASTORE，之後可由既有 paths／CFG 證明。paths 沿用逐路徑 Frame。
5. `SwitchMaps` 驗證完整 enum-map 初始化與 NoSuchFieldError 區間，從 classpath 確認實際 enum 類別。欄位依 enum＋完整常數→case 值內容識別，引用與 field handle 共用映射。`<clinit>` 以完整 enum 段排序，段內指令／catch 順序照比。未知形狀、額外呼叫、重複內容或缺失 enum metadata 保留原名。
6. 新增 `TwelfthSelfTest` 並接入 `selftest.sh`。舊無 initializer 的欄位改名 fixture 改為預期 DIFF，#186 的正例改用完整可辨識 enum 初始化。舊 effects 的單讀取／雙 POP fixture 改由 SAME_PATHS 證明。

## 已驗證／怎麼驗證

Microsoft OpenJDK 17.0.17，ASM／asm-tree／asm-analysis 9.7。本機 11 套 Java 自測均以 `-Xverify:all` 執行，共 **2106 checks**，包含本輪 **261 checks**。

同份 framed JAR 供雙向 CLI、直接 paths／CFG 及 platform-parent 隔離 ClassLoader 使用。反例須實際產生不同結果，VerifyError 不計入行為差異。

| 反例 | JVM 左／右 | 工具結果 |
|---|---|---|
| 前兩次求值順序對調 | 1244／2144 | 雙向 DIFF，paths／CFG 拒絕 |
| 兩次讀取之間覆寫暫存 | 2／3 | 雙向 DIFF，paths／CFG 拒絕 |
| DUP 與第二個讀取來自不同運算式，input=1 | 2／3 | 雙向 DIFF，paths／CFG 拒絕 |
| 預壓常數不同 | 0／1 | 雙向 DIFF，paths／CFG 拒絕 |
| handler 讀取不同保存值 | 1／2 | 雙向 DIFF，暫存資格拒絕 |
| wide shuffle 的值不同 | 2／0 | 雙向 DIFF，paths／CFG 拒絕 |
| 死暫存的生產者具有副作用，input=0 | 0／1 | 雙向 DIFF，paths／CFG 拒絕 |
| switch-map case 值不同 | 7／17 | 雙向 DIFF |
| switch-map 常數集合不同 | 7／2 | 雙向 DIFF |

正例涵蓋四連 ASTORE、跨區塊讀取、重複讀取、區域槽位型別重用、預壓 receiver／常數、null／cast return、六種 stack shuffle、category-2 值及兩個 enum 的欄位／初始化順序互換。停用 `value-flow` 的 CFG 消融確認跨區塊四連 store 與預壓常數恢復 DIFF。

### 指定方法探查

`sourcePoisonBody$useInventoryItemBeforeTravel` 對正式 reference 的完整區塊／CFG 證明通過。`movementUnlock` 的本體同樣通過，但完整方法 metadata 為 staged `java/io/IOException`、候選空集合，驗收持續判 DIFF。上述探查只讀取 bytecode。

### 九批重跑

已保存修改前工具 classes，對相同 staged bytes 重跑。每批先核對報告中的 JAR SHA-256 與 batches.json 的完整 class 集合。#69 使用最新 `4955c7b8580f` 的 `final-254/b08.jar` 及該輪已核准的 Exceptions／access／extra-method 裁切清單。#70 固定最新 `989417ab30b7` 的 362-entry `verified216.jar`。

最終 delivery 工具的完整九批重跑進行中，結果將在交付前補入本報告。

### CI

本機自測已通過，PR CI 收據待推送後核對。

## 推測

資料流及 CFG 結果適用於本次固定 bytecode 與工具模型。遊戲、並行執行及外部系統行為仍需各批原定驗收。

## 待驗證／未驗證什麼

- 最終九批 delivery 結果及 PR CI 待本輪收齊。
- 實機遊戲、登入、伺服器與 DB 操作均未執行。
- `movementUnlock` 的 Exceptions 差異需要 B09 負責人依既有授權處理。

## 產出路徑與給下一棒的提示

- 程式與自測位於 `lito伺服器/LiTo381-Server/tools/semantic-diff/**`，本報告位於 `docs/reports/issue-259.md`。
- 暫存腳本、CLI argv、前後 JSON／Markdown 與日誌位於 `scratch/issue-259/`，工具及 fixture classes／JAR 位於工具 `target/issue-259/`。
- 重跑入口為 `scratch/issue-259/verify.py` 與 `batches.py`。參數 `before` 使用修改前已編譯工具，`delivery` 使用交付版工具。自測可執行 `verify.py tests`，CI 入口為工具 `selftest.sh`。
- Claude 審查應聚焦唯一寫入／handler 排除、區塊出口保存值、跨邊 stack 關聯，以及完整 switch initializer 的拒絕條件。主控依 U2-1 已兌現 2 張的上限處理後續缺口。
