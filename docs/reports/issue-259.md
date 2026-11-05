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

Microsoft OpenJDK 17.0.17，ASM／asm-tree／asm-analysis 9.7。本機 11 套 Java 自測均以 `-Xverify:all` 執行，共 **2107 checks**，包含本輪 **262 checks**。

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

### 九批重跑

使用程式提交 `3088b20` 編譯 verified 工具，與起點 `8df7c89` 的 before 工具比較。每批使用相同 staged bytes、完整 class 集合與既有核准清單。#69 使用最新 `4955c7b8580f` 的 `final-254/b08.jar`，沿用該輪三項 Exceptions、access 及 extra-method 核准。#70 固定最新 `989417ab30b7` 的 362-entry `verified216.jar`，T029 方法以正式 reference 驗收。

| 批次 | staged 來源提交 | entries | 類別 DIFF 前→後 | 方法 DIFF 前→後 | 修改後類別結果 |
|---|---|---:|---:|---:|---|
| #62 | `58b2f00e40a3` | 22 | 0 → 0 | 0 → 0 | 21 SAME、1 LAMBDA_PAIRED_BY_CALLSITE |
| #63 | `23200d027c80` | 108 | 0 → 0 | 0 → 0 | 107 SAME、1 SAME_PATHS |
| #65 | `4d4cdd3d732a` | 98 | 0 → 0 | 0 → 0 | 95 SAME、1 SAME_PATHS、1 SAME_CFG、1 DEVIATION |
| #66 | `5380ca3a1ea7` | 120 | 0 → 0 | 0 → 0 | 115 SAME、3 SAME_EFFECTS、1 SAME_PATHS、1 ORPHAN_IGNORED |
| #67 | `1c887286fef4` | 323 | 0 → 0 | 0 → 0 | 318 SAME、5 SAME_PATHS |
| #68 | `6a255226e523` | 56 | 0 → 0 | 0 → 0 | 56 SAME |
| #69 | `4955c7b8580f` | 160 | 1 → 1 | 1 → 1 | 144 SAME、7 SAME_PATHS、7 SAME_CFG、1 LAMBDA_PAIRED_BY_CALLSITE、1 DIFF |
| #70 | `989417ab30b7` | 362 | 3 → 2 | 345 → 333 | 360 SAME、2 DIFF |
| #72 | `09af151ea7eb` | 185 | 0 → 0 | 0 → 0 | 183 SAME、2 DEVIATION |

九批合計 1434 entries，類別級 ONLY_LEFT／ONLY_RIGHT、missingLeft／extraLeft 均為 0。方法數合併 effectsMethods、approvedMethods 與 methodResults，以原方法描述子去重。原先通過的方法全部通過。

#62、#63、#65、#66、#67、#68、#69、#72 的 DIFF→SAME 清單為空。#67／#68 及 #69 的部分既有 SAME_PATHS／SAME_CFG 提前由完整區塊證明通過。#69 的剩餘 DIFF 為 `NativeLightAction$Batch#emit()V`，其診斷與修改前一致。

### B09 指定結果與全部 DIFF→SAME

| 指定方法 | 結果 | 驗收對象／說明 |
|---|---|---|
| `ClientSession#sourcePoisonBody$useInventoryItemBeforeTravel` | DIFF → SAME_EFFECTS | 正式 0.6.71，實際以完整區塊值來源證明通過 |
| `GameHub#battleDisconnectWork` | DIFF → SAME | 候選，完整區塊比較通過 |
| `ClientSession#movementUnlock` | DIFF → DIFF | 本體的 block／CFG 均通過，完整方法仍差 Exceptions metadata |
| `ClientSession#acceptsLitoRosterBroadcast` | DIFF → SAME | 候選，Phase switch-map 配對通過 |
| `ClientSession$1#<clinit>` | DIFF → SAME | 候選，完整 enum 分段排序後比較通過 |

`movementUnlock` 左側宣告 `java/io/IOException`，右側為空集合，這項差異交 B09 負責人處理。方法本體探查收據為 `scratch/issue-259/movementUnlock.log`。B09 全部新通過方法如下，SAME 表示 CLI 的線性／區塊完整比較已通過，SAME_EFFECTS 保留既有核准方法的分類。

B09 剩餘兩個 DIFF 類別為 `ClientSession` 與 `GameHub`，包含 333 個 DIFF 方法，另有方法級 9 ONLY_LEFT、63 ONLY_RIGHT。整批後續由 B09 原任務接手。

| 方法 | 前→後 | 說明 |
|---|---|---|
| `ClientSession#acceptsLitoRosterBroadcast()Z` | DIFF → SAME | Phase switch-map 內容配對 |
| `ClientSession#attack([BLlocal/lito381/server/ClientSession$AttackEnvelope;)V` | DIFF → SAME | AttackEnvelope switch-map 內容配對 |
| `ClientSession#serverClassicWorldDspyFollowerTick(IJ)V` | DIFF → SAME | TransitionKind switch-map 內容配對 |
| `ClientSession#sourceDamagePoisonClock(JZZLlocal/lito381/server/BattleSpellCatalog$PoisonSpell;Llocal/lito381/server/CombatPoisonLifecycle$Scope;)V` | DIFF → SAME | stack shuffle／暫存保存值正規化，完整有序事件及引數證明 |
| `ClientSession#sourceNpcPoisonState(JIIIJLjava/lang/String;)Llocal/lito381/server/CombatPoisonAtomic$NpcState;` | DIFF → SAME | PoisonAttack.Kind switch-map 內容配對 |
| `ClientSession#sourcePoisonBody$characterSelect([BZLlocal/lito381/server/CombatPoisonLifecycle$Scope;)V` | DIFF → SAME_CFG | stack shuffle／暫存保存值正規化，完整有序事件及引數證明 |
| `ClientSession#sourcePoisonBody$resetNativeTournamentEconomy(Ljava/lang/String;Llocal/lito381/server/CombatPoisonLifecycle$Scope;)V` | DIFF → SAME | stack shuffle／暫存保存值正規化，完整有序事件及引數證明 |
| `ClientSession#sourcePoisonBody$restartClassicWorldAfterDeath(Llocal/lito381/server/CombatPoisonLifecycle$Scope;)V` | DIFF → SAME_CFG | stack shuffle／暫存保存值正規化，完整有序事件及引數證明 |
| `ClientSession#sourcePoisonBody$serverEndBattle(Llocal/lito381/server/BattleRules$Resolution;Llocal/lito381/server/CombatPoisonLifecycle$Scope;)V` | DIFF → SAME | stack shuffle／暫存保存值正規化，完整有序事件及引數證明 |
| `ClientSession#sourcePoisonBody$useInventoryItemBeforeTravel([BLlocal/lito381/server/CombatPoisonLifecycle$Scope;)V` | DIFF → SAME_EFFECTS | 連續四個 ASTORE 與 stack 保存值同源，完整有序區塊證明 |
| `ClientSession$1#<clinit>()V` | DIFF → SAME | 完整 enum 初始化段配對與排序 |
| `GameHub#battleDisconnectWork(JJ)Llocal/lito381/server/GameHub$BattleDisconnectWork;` | DIFF → SAME | DUP／重複讀取與 null／cast return 的同值傳遞 |

初版移除舊 tempStores 後曾使 `exitRoomBeforePolyMe` 由 SAME_EFFECTS 回到 DIFF。追查為 `DUP; ASTORE` 的保存值完全無讀取，已補入同一資料流模型並以「死保存值」正例及「有副作用生產者」反例驗證。最終完整區塊／CFG 證明通過，既有方法持續為 SAME_EFFECTS。

### 固定輸入收據

候選 reference SHA-256 為 `50635f7edca8ec6ce77787cc32371c7590b497e48ea202f8a679caf3f517b729`。#72 及 #70 allow-right 的正式 reference 為 `e1498871c7be6b287821c57ef5d95933db1740d9b4d4a80f6c3465f21d99d498`。

| 批次 | staged JAR SHA-256 |
|---|---|
| #62 | `a79c74b4cab4b6714bf241953feeacb8201b2a125578ae2f31702ca8ff6d19c7` |
| #63 | `9cc4d4cbac153d7f13ffea5864fb7ac34994618bd7717c4a2900bd36080f38ef` |
| #65 | `ac5e21b26180b296dab457173b52cace3c0dfca63b250c8bc793859f77c80834` |
| #66 | `b8d54f8315093327943495503edb9e7dbb0a725d73ea10da012d37738f1d52e4` |
| #67 | `8820abf5684b6fd6c9c55826372a21bad10f5f965880d4a954e57fe22d1640c6` |
| #68 | `af7f16ed2373d96069dbedb059e2bb57ebb5a247eeca66b380ebed87a3f032f8` |
| #69 | `fc7697767d2da777f8e6bfd21bf8297701da1a2698f2795367f724c23859066f` |
| #70 | `eeb1b8d0bba82e6852bc6020eb17f119de44ae41d27147040cd4deae7e0d75f3` |
| #72 | `63e970a0ed1bd26f94586b79df3de840e6231b46977ce63215d5427e55363808` |

各來源絕對路徑、head 與 entry 數記錄在 `scratch/issue-259/batch-*/input.json`。每批實際 argv、核准裁切、before／verified JSON 與 Markdown 同目錄保存。所有 staged 來源皆只讀取，再以一般複製放入本 worktree 的工具 target 並核對雜湊。

### CI

程式提交 `3088b20` 的 Semantic gap 工作已完成。已下載實際日誌，確認 11 套自測共 **2107 checks**、`SELFTEST ALL PASS`、`FALLBACK ALL PASS`，以及四項必要反例的 JVM 左右差異收據。去除 ANSI 色碼的日誌保存於 `scratch/issue-259/ci-semantic.log`。

| 工作 | 程式提交 CI 收據 |
|---|---|
| Semantic gap | [127606923450](https://github.com/chaoren0418-arch/lito-launcher-server/actions/runs/42130896836/job/127606923450) |
| Server 建置 | [127606923353](https://github.com/chaoren0418-arch/lito-launcher-server/actions/runs/42130896836/job/127606923353) |
| Launcher 建置 | [127606922937](https://github.com/chaoren0418-arch/lito-launcher-server/actions/runs/42130896836/job/127606922937) |
| Server tests | [127606923540](https://github.com/chaoren0418-arch/lito-launcher-server/actions/runs/42130896836/job/127606923540) |
| Build-Package.ps1 zip | [127606923252](https://github.com/chaoren0418-arch/lito-launcher-server/actions/runs/42130896862/job/127606923252) |

CI 整包 Maven 產物對正式 reference 為 2176 SAME、17 SAME_PATHS、24 SAME_CFG、4 LAMBDA_PAIRED_BY_CALLSITE、328 DIFF、1382 ONLY_LEFT、5 ONLY_RIGHT。此輸入與上表 staged 批次分開計算。最終報告提交的 checks 在交付前另外核對。[PR #265 checks](https://github.com/chaoren0418-arch/lito-launcher-server/pull/265/checks)

## 推測

資料流及 CFG 結果適用於本次固定 bytecode 與工具模型。遊戲、並行執行及外部系統行為仍需各批原定驗收。

## 待驗證／未驗證什麼

- 九批 verified 重跑與語意 CI 完成，最終報告提交的完整 checks 於交付前另核對。
- 實機遊戲、登入、伺服器與 DB 操作均未執行。
- `movementUnlock` 的 Exceptions 差異需要 B09 負責人依既有授權處理。

## 產出路徑與給下一棒的提示

- 程式與自測位於 `lito伺服器/LiTo381-Server/tools/semantic-diff/**`，本報告位於 `docs/reports/issue-259.md`。
- 暫存腳本、CLI argv、前後 JSON／Markdown 與日誌位於 `scratch/issue-259/`，工具及 fixture classes／JAR 位於工具 `target/issue-259/`。
- 重跑入口為 `scratch/issue-259/verify.py` 與 `batches.py`。`verify.py verified` 編譯交付版工具，`batches.py verified 62 63 65 66 67 68 69 72 70` 執行本輪九批。`batches.py before` 使用修改前已保存 classes，重建該基線須取 `8df7c89` 的工具來源。自測可執行 `verify.py tests`，CI 入口為工具 `selftest.sh`。
- Claude 審查應聚焦唯一寫入／handler 排除、區塊出口保存值、跨邊 stack 關聯，以及完整 switch initializer 的拒絕條件。主控依 U2-1 已兌現 2 張的上限處理後續缺口。
