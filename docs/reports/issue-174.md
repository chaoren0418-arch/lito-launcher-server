# Issue #174 — T020i-5 semantic-diff 第五批正規化

## 結論

四項功能已實作，878 項本機自測通過。固定 #62 核准模式由 **6 DIFF → 2 DIFF**，#63 由 **2 DIFF → 1 DIFF**；#66、#72、#76 的所有 entry 狀態均維持原結果，沒有把原 DIFF 判成 SAME。沒有以跨區塊來源 union 放行 #63 的回傳暫存。

交付分支 `task/174`，[PR #178](https://github.com/chaoren0418-arch/lito-launcher-server/pull/178)。只修改 semantic-diff 工具、指定 Exceptions 清單與本報告；不改 src/main、staged Java、docs/REPORT.md 或原專案工作檔，未啟動服務／遊戲、未連 DB、未讀寫機密設定、封存或正式產物。

## 做了什麼

- 第 1 項：以有效指令位置識別 handler 入口，逐條比較可能拋例外指令的有序涵蓋清單。整數除法／餘數、欄位、呼叫、陣列、monitor 等不會被裁掉；分段 try 與純運送指令的覆蓋差異不再決定結果。LDC class／method handle／method type／condy 的解析失敗仍保守保留。
- 第 2 項：新增 `NonNullChecks`。正常路徑成功取用 receiver 才產生非 null 證據；匯合取交集，例外邊不使用本次成功證據，迴圈重新執行 producer 會清除舊證據。只移除精確 Object overload、回傳值被丟棄的三指令形狀。多來源、額外入口、註解或分析失敗不放行。
- 第 3 項：新增 `BlockExpressions`，用區塊內有序運算節點和 operands 表示式比較 LOAD 位置、dead-at-exit 暫存與 DUP／DUP2。保留正常與例外出口、入口／出口 stack 位置、全方法 local 雙射、逐值複本數及所有非運送操作的順序。STORE／IINC 更新值版本；一次 STORE 的全部讀取必須同區塊，不在 handler 或其他區塊。既有 #166 多來源 singleton 拒絕邏輯未變。
- 第 4 項：新增 `--exceptions-exempt`，只忽略精確方法的 Exceptions 屬性，驗證兩側方法存在、在比較範圍內及附核准 URL；JSON／Markdown 列出使用條目。初始 `captureOutput` 條目引用 [#62 的最新核准](https://github.com/chaoren0418-arch/lito-launcher-server/issues/62#issuecomment-6654588607)，不是前一則要求修改 throws 的舊裁定。
- `FifthSelfTest` 接進既有 `selftest.sh`，其餘三套自測與 CI 接線沿用。

完整守門與保守限制見 `tools/semantic-diff/README.md`。方法原樹及既有 #166 局部改寫結果各有獨立比較路徑；第 3 項不賦予未核准方法 CFG 重排權限。

## 已驗證

### 自我測試

Microsoft JDK 17.0.17、ASM 9.7，全部 Java 從來源重新編譯，以 `-Xverify:all` 執行。

| 測試 | 檢查數 | 結果 |
|---|---:|---|
| NormalizationSelfTest | 517 | ALL PASS |
| AdvancedSelfTest | 73 | ALL PASS |
| FourthSelfTest | 119 | ALL PASS |
| FifthSelfTest | 169 | ALL PASS |
| 合計 | 878 | ALL PASS |

新增 fixture 同一份 framed class bytes 同時交 CLI 和隔離 ClassLoader，沒有載入或執行伺服器類別。正例與反例均做左右互換；CLI exit 0 之外另檢查實際 SAME／DIFF。重要實測反例如下：

| 規則／反例 | 左側 JVM | 右側 JVM | 比較器 |
|---|---|---|---|
| 第 1 項：IDIV 僅一側涵蓋，輸入 0 | ArithmeticException | -1 | DIFF |
| 第 1 項：handler 不同或優先序不同 | 2 | -1 | DIFF |
| 第 1 項：parseInt 僅一側涵蓋，輸入 bad | NumberFormatException | -1 | DIFF |
| 第 2 項：一條路徑未取用 null receiver | NullPointerException | 7 | DIFF |
| 第 2 項：local 覆寫／receiver 例外已捕捉／迴圈新值 | NullPointerException | 7 | DIFF |
| 第 3(a) 項：LOAD 跨 IINC，輸入 7 | 7 | 8 | DIFF |
| 第 3(a) 項：LOAD 跨 STORE，輸入 7 | 7 | 9 | DIFF |
| 第 3(b) 項：暫存讀取次數不同，輸入 7 | 23 | 16 | DIFF |
| 第 3(b) 項：欄位副作用呼叫順序不同 | 15 | 16 | DIFF |
| 第 3(b) 項：區塊外讀取受到覆寫 | 9 | 7 | DIFF |
| 第 3 項：分支選值互換（false，-7） | 16 | 15 | DIFF |

`block-merged-entry` 正例在停用 blocks 時 DIFF，啟用後 SAME；true／false 的 JVM 結果都符合各自原分支。沒有將多來源集合當作相同值。Exceptions 測試另驗證預設仍 DIFF、精確核准可通過、其他方法 throws／本體不同仍 DIFF，以及 typo／範圍外／缺核准 URL 拒絕。

### 固定輸入與重跑方式

修改前比較器取自本 worktree 起點 `450178d`（已包含 #166），與本次比較器均由來源編譯。#62／#63 報告從題目指定 git commit 讀取，不依賴可能被其他任務更新的工作檔。五份 staged JAR 雜湊均符合各指定提交報告收據，固定複本放本 worktree 的工具 `target/issue-174/inputs/`，所有比較使用相同 bytes。

#66、#72、#76 另以 `git ls-remote` 查核遠端分支，仍是下表提交。所有參考 JAR 只作 metadata／欄位 owner 依賴，沒有混進 staged 比較集合。每輪恰為 **595 entries**，ONLY_LEFT／ONLY_RIGHT=0，missingLeft／extraLeft 皆空。

| 批次／提交 | entries | staged SHA-256 |
|---|---:|---|
| #62 `63da8a2` | 22 | `4ba5c47dadda30b4a7f2f1be66c0ca501ed0d453ad7ac19c618eda7e4456b0ea` |
| #63 `8313f57` | 108 | `4e6ea515e960e1679b22c937aa61673d2e754b39b9aea5a733c4d5ac22d9b75f` |
| #66 `be0b284` | 120 | `79e9462b75590bb9cdb8767ebf333e69a0c8ff95bed0b1239edb11d97d753374` |
| #72 `c460ce9` | 185 | `ba50147a61ce3f50a08aae1ea0f35aba09fd19efdb1a44b3db37d4c4d7fcea49` |
| #76 `e62d213` | 160 | `3af155bc5c65e2413862fc1ceb1db598f9aa704fe91297c788288e6e0570e091` |

候選 reference SHA-256：`50635f7edca8ec6ce77787cc32371c7590b497e48ea202f8a679caf3f517b729`；正式 reference：`e1498871c7be6b287821c57ef5d95933db1740d9b4d4a80f6c3465f21d99d498`。

共九種模式／五批＝45 次：修改前預設、修改前既有核准、第 1 項、加第 2 項、新版全預設、新版核准，以及逐項停用的診斷對照。既有核准僅取該批交集；新版唯一新增豁免是 #62 的指定 Exceptions 方法。

| 批次 | 修改前預設 SAME／DIFF | 第 1 項 DIFF | 第 1＋2 項 DIFF | 新版預設 SAME／DIFF | 修改前核准 DIFF | 新版核准 SAME／EFFECTS／LAYOUT／DIFF |
|---|---:|---:|---:|---:|---:|---:|
| #62 | 15／7 | 6 | 4 | 19／3 | 6 | 19／1／0／2 |
| #63 | 106／2 | 2 | 1 | 107／1 | 2 | 107／0／0／1 |
| #66 | 117／3 | 3 | 3 | 117／3 | 2 | 115／3／0／2 |
| #72 | 180／5 | 5 | 5 | 180／5 | 5 | 180／0／0／5 |
| #76 | 159／1 | 1 | 1 | 159／1 | 0 | 158／1／1／0 |

以下逐 entry 使用「修改前既有核准 → 新版核准」，同為 SAME 的 entry 沒有新差異可消除。第 1～3 項預設開啟，Forest 在新版預設亦已 SAME；核准模式仍顯示 SAME_EFFECTS 是既有清單的結果標籤。

### #62 逐 entry 對照

全部位於 `local/lito381/server/`，省略 `.class`。

| Entry | 前 → 後 | 消除項目／剩餘 |
|---|---|---|
| `NativeBlindSceneAction` | DIFF → SAME | 第 2 項：companion 的 context 已成功取用，移除冗餘 null 檢查。 |
| `NativeCompanionForestArea` | SAME_EFFECTS → SAME_EFFECTS | 第 2 項：新版預設已 SAME；既有核准模式仍標 SAME_EFFECTS，沒有新增 effects 授權。 |
| `NativeCompanionRefScene` | SAME → SAME | 維持相同。 |
| `NativeCompanionWindAction` | DIFF → DIFF | 第 2 項消除 null 差異；保留 DIFF：staged 提前配置參數陣列／常數的操作順序仍不同，依 #62 最新裁定回到一般 varargs Java。 |
| `NativeControlledNegativeMaterial` | DIFF → SAME | 第 1 項：cast 每條可能拋例外指令的 handler 清單一致，6 段／2 段 try 差異消除。 |
| `NativePairedCastSafety` | SAME → SAME | 維持相同。 |
| `NativeSourceBatchTerminal` | SAME → SAME | 維持相同。 |
| `NativeSourceCubePlacement` | SAME → SAME | 維持相同。 |
| `NativeSourceCubePlacement$Draft` | SAME → SAME | 維持相同。 |
| `NativeSourceDollAccepted` | DIFF → DIFF | 保留 DIFF：staged 每個 plan.caster／target 只讀一次，候選各讀兩次；第 3 項不合併 GETFIELD，交 #62 修改 Java。 |
| `NativeSourceDollAccepted$Pending` | SAME → SAME | 維持相同。 |
| `NativeSourceDollNpc` | SAME → SAME | 維持相同。 |
| `NativeSourceDollNpc$Scope` | SAME → SAME | 維持相同。 |
| `NativeSourceDollSkills` | SAME → SAME | 維持相同。 |
| `NativeSourceDollSkills$View` | SAME → SAME | 維持相同。 |
| `NativeSourceDollSlow` | SAME → SAME | 維持相同。 |
| `NativeSourceHealingCast` | DIFF → SAME | 第 3(a) 項配合既有 #166：owned 載入延後到使用處仍為相同值；保留所有呼叫次序。 |
| `NativeSourceHealingCast$Cast` | SAME → SAME | 維持相同。 |
| `NativeSourcePendingAdmission` | SAME → SAME | 維持相同。 |
| `NativeSummonTerminalLifecycle` | SAME → SAME | 維持相同。 |
| `NativeWeaponBreakBattleCompanion` | SAME → SAME | 維持相同。 |
| `NativeWeaponBreakTargetLifecycle` | DIFF → SAME | 第 2＋4 項：已取用 context 的 null 檢查消除；captureOutput 的 IOException 只由精確核准豁免。未傳清單仍 DIFF。 |
### #63 逐 entry 對照

全部位於 `local/lito381/server/`，省略 `.class`。

| Entry | 前 → 後 | 消除項目／剩餘 |
|---|---|---|
| `NativeAcceptedAreaFreezeMutation` | DIFF → DIFF | 第 3(b) 項已對齊 stream/context 區塊內暫存；整類仍 DIFF：回傳 local 2 在兩個分支寫入、匯合後讀取，超出單一區塊前提。 |
| `NativeAcceptedAreaFreezeMutation$Group` | SAME → SAME | 維持相同。 |
| `NativeAcceptedAreaFreezeMutation$Npc` | SAME → SAME | 維持相同。 |
| `NativeAcceptedAreaFreezeMutation$Output` | SAME → SAME | 維持相同。 |
| `NativeAcceptedAreaFreezeMutation$Player` | SAME → SAME | 維持相同。 |
| `NativeAcceptedFreezeMutation` | SAME → SAME | 維持相同。 |
| `NativeAcceptedFreezeMutation$Batch` | SAME → SAME | 維持相同。 |
| `NativeAcceptedFreezeMutation$Linked` | SAME → SAME | 維持相同。 |
| `NativeAcceptedFreezeMutation$Output` | SAME → SAME | 維持相同。 |
| `NativeAcceptedShockMutation` | SAME → SAME | 維持相同。 |
| `NativeAcceptedShockMutation$Output` | SAME → SAME | 維持相同。 |
| `NativeAcceptedShockMutation$Plan` | SAME → SAME | 維持相同。 |
| `NativeBattleControlledCancellation` | DIFF → SAME | 第 2 項移除已取用 context 的 null 檢查後，既有 #166 即可證明 stream 暫存；不需豁免。 |
| `NativeBattleControlledCancellation$Output` | SAME → SAME | 維持相同。 |
| `NativeBattleControlledCancellation$Scope` | SAME → SAME | 維持相同。 |
| `NativeBattleTamedLifecycle` | SAME → SAME | 維持相同。 |
| `NativeBattleTamedLifecycle$CaptureRollback` | SAME → SAME | 維持相同。 |
| `NativeBlindSourceState` | SAME → SAME | 維持相同。 |
| `NativeCompanionArea` | SAME → SAME | 維持相同。 |
| `NativeCompanionCombatScene` | SAME → SAME | 維持相同。 |
| `NativeCompanionEarth` | SAME → SAME | 維持相同。 |
| `NativeCompanionExpiry` | SAME → SAME | 維持相同。 |
| `NativeCompanionExpiry$Due` | SAME → SAME | 維持相同。 |
| `NativeCompanionForestCancellation` | SAME → SAME | 維持相同。 |
| `NativeCompanionLanding` | SAME → SAME | 維持相同。 |
| `NativeCompanionLanding$Terrain` | SAME → SAME | 維持相同。 |
| `NativeCompanionManaTransfer` | SAME → SAME | 維持相同。 |
| `NativeCompanionNature` | SAME → SAME | 維持相同。 |
| `NativeCompanionNegative` | SAME → SAME | 維持相同。 |
| `NativeCompanionPlacement` | SAME → SAME | 維持相同。 |
| `NativeCompanionPlacement$Removal` | SAME → SAME | 維持相同。 |
| `NativeCompanionPlacement$Scene` | SAME → SAME | 維持相同。 |
| `NativeCompanionPlacement$State` | SAME → SAME | 維持相同。 |
| `NativeCompanionPlacement$Transition` | SAME → SAME | 維持相同。 |
| `NativeCompanionPlacement$Viewer` | SAME → SAME | 維持相同。 |
| `NativeCompanionProcScene` | SAME → SAME | 維持相同。 |
| `NativeCompanionReflectionScene` | SAME → SAME | 維持相同。 |
| `NativeCompanionResurrection` | SAME → SAME | 維持相同。 |
| `NativeCompanionResurrection$Corpse` | SAME → SAME | 維持相同。 |
| `NativeCompanionResurrection$NpcCorpse` | SAME → SAME | 維持相同。 |
| `NativeCompanionRoleIdentity` | SAME → SAME | 維持相同。 |
| `NativeCompanionRoleIdentity$Epoch` | SAME → SAME | 維持相同。 |
| `NativeCompanionRoleIdentity$Role` | SAME → SAME | 維持相同。 |
| `NativeCompanionTargets` | SAME → SAME | 維持相同。 |
| `NativeCompanionTargets$Death` | SAME → SAME | 維持相同。 |
| `NativeCompanionTargets$Scope` | SAME → SAME | 維持相同。 |
| `NativeCompanionWindState` | SAME → SAME | 維持相同。 |
| `NativeCreateZombieLifecycle` | SAME → SAME | 維持相同。 |
| `NativeCreateZombieLifecycle$Stage` | SAME → SAME | 維持相同。 |
| `NativeHpCompletionAdmission` | SAME → SAME | 維持相同。 |
| `NativeNpcWeaponBreakActions` | SAME → SAME | 維持相同。 |
| `NativeNpcWeaponBreakAreaActions` | SAME → SAME | 維持相同。 |
| `NativeNpcWeaponBreakDamage` | SAME → SAME | 維持相同。 |
| `NativeResurrectionQueueLife` | SAME → SAME | 維持相同。 |
| `NativeRetiredReuseTimer` | SAME → SAME | 維持相同。 |
| `NativeRetiredReuseTimer$Retired` | SAME → SAME | 維持相同。 |
| `NativeSelectedPlayerIntent` | SAME → SAME | 維持相同。 |
| `NativeSelectorOnlyCtrl` | SAME → SAME | 維持相同。 |
| `NativeShockCompanionAction` | SAME → SAME | 維持相同。 |
| `NativeShockCompanionAction$Death` | SAME → SAME | 維持相同。 |
| `NativeShockPlayerAction` | SAME → SAME | 維持相同。 |
| `NativeShockPlayerAction$Linked` | SAME → SAME | 維持相同。 |
| `NativeShockPlayerAction$Scope` | SAME → SAME | 維持相同。 |
| `NativeSkillTargetMetadata` | SAME → SAME | 維持相同。 |
| `NativeSkillTargetMetadata$Row` | SAME → SAME | 維持相同。 |
| `NativeSourceAcceptedPlayers` | SAME → SAME | 維持相同。 |
| `NativeSourceAcceptedPlayers$Participant` | SAME → SAME | 維持相同。 |
| `NativeSourceAcceptedPlayers$Proof` | SAME → SAME | 維持相同。 |
| `NativeSourceAreaCasterTargets` | SAME → SAME | 維持相同。 |
| `NativeSourceAreaCasterTargets$Cursor` | SAME → SAME | 維持相同。 |
| `NativeSourceAreaCasterTargets$Selection` | SAME → SAME | 維持相同。 |
| `NativeSourceAreaClassSelection` | SAME → SAME | 維持相同。 |
| `NativeSourceAreaClassSelection$Cursor` | SAME → SAME | 維持相同。 |
| `NativeSourceAreaPlayerProof` | SAME → SAME | 維持相同。 |
| `NativeSourceArenaPoison` | SAME → SAME | 維持相同。 |
| `NativeSourceArenaPoison$Lease` | SAME → SAME | 維持相同。 |
| `NativeSourceArenaPoison$Output` | SAME → SAME | 維持相同。 |
| `NativeSourceArenaPoison$Proof` | SAME → SAME | 維持相同。 |
| `NativeSourceArenaPoison$Viewer` | SAME → SAME | 維持相同。 |
| `NativeSourceBarrierTargets` | SAME → SAME | 維持相同。 |
| `NativeSourceCompanionClock` | SAME → SAME | 維持相同。 |
| `NativeSourceCompanionElemental` | SAME → SAME | 維持相同。 |
| `NativeSourceCompanionElemental$Lease` | SAME → SAME | 維持相同。 |
| `NativeSourceCompanionFirstActor` | SAME → SAME | 維持相同。 |
| `NativeSourceCompanionFirstActor$View` | SAME → SAME | 維持相同。 |
| `NativeSourceControlledAudience` | SAME → SAME | 維持相同。 |
| `NativeSourceControlledAudience$Proof` | SAME → SAME | 維持相同。 |
| `NativeSourceControlledDisplay` | SAME → SAME | 維持相同。 |
| `NativeSourceControlledDisplay$Action` | SAME → SAME | 維持相同。 |
| `NativeSourceControlledDisplay$Output` | SAME → SAME | 維持相同。 |
| `NativeSourceControlledDisplay$Proof` | SAME → SAME | 維持相同。 |
| `NativeSourceControlledDisplay$Target` | SAME → SAME | 維持相同。 |
| `NativeSourceControlledHealing` | SAME → SAME | 維持相同。 |
| `NativeSourceControlledHealing$Frozen` | SAME → SAME | 維持相同。 |
| `NativeSourceControlledHealing$Output` | SAME → SAME | 維持相同。 |
| `NativeSourceControlledHealing$Scope` | SAME → SAME | 維持相同。 |
| `NativeSourceControlledNegativeFinal` | SAME → SAME | 維持相同。 |
| `NativeSourceControlledNegativeFinal$Scope` | SAME → SAME | 維持相同。 |
| `NativeSourceControlledNegativeFinal$Value` | SAME → SAME | 維持相同。 |
| `NativeSourceControlledRoleFrame` | SAME → SAME | 維持相同。 |
| `NativeSourceCubeClock` | SAME → SAME | 維持相同。 |
| `NativeSourceCubeClock$Phase` | SAME → SAME | 維持相同。 |
| `NativeSourceCubeNpc` | SAME → SAME | 維持相同。 |
| `NativeSourceCubeNpc$Commit` | SAME → SAME | 維持相同。 |
| `NativeSourceCubeNpc$Effect` | SAME → SAME | 維持相同。 |
| `NativeSourceCubeNpc$Output` | SAME → SAME | 維持相同。 |
| `NativeSourceCubeNpc$Spawn` | SAME → SAME | 維持相同。 |
| `NativeSourceCubeNpcLease` | SAME → SAME | 維持相同。 |

### #63 players 的明確剩餘邊界

直接檢查 BlockExpressions：stream/context 所在區塊的前七個運算節點及 operands、context 的一個複本兩側完全對應；staged 只有末尾多 `ASTORE 2`。null 檢查仍保留，因 `Group.c` 是被讀出的值，先前 GETFIELD 證明的是 Group receiver 非 null，不能推論欄位值非 null。

另外兩個區塊仍為：staged 分支寫 local 2／candidate 留在出口 stack；匯合後 staged `ALOAD 2; ARETURN`／candidate 直接 `ARETURN`。local 2 有跨區塊讀取，因此不符合第 3(b) 項，整類保留 DIFF。這不是 stream/context 區塊內展開失敗，也不以手工證明覆蓋整類結果。診斷全文：`scratch/issue-174/NativeAcceptedAreaFreezeMutation-players.log`。

### #66／#72／#76 防誤放行對照

全部 465 entries 在相同核准模式的狀態逐項相等；預設模式亦逐項相等。原 DIFF 如下，沒有新增核准：

| 批次／entry | 修改前 → 新版 | 保留原因 |
|---|---|---|
| #66 NativeSourceRawDamage$Member | DIFF → DIFF | 建構子額外初始化。 |
| #66 NativeStaticNpcScene | DIFF → DIFF | heading 的暫存、呼叫與 monitor 離開順序不同。 |
| #66 NativeSourceSignedRecovery | 預設 DIFF → DIFF；核准 SAME_EFFECTS → SAME_EFFECTS | 既有精確 effects 授權，沒有擴大。 |
| #72 NativeDeCharacterFields | DIFF → DIFF | SKD1 等左側獨有欄位。 |
| #72 NativeDeCharacterFields$CombatSkillDerived | DIFF → DIFF | 正式 JAR 的 SKU1 等欄位未解析，不視為 staged 已解析宣告。 |
| #72 NativeDeClientViewer | DIFF → DIFF | sendPackets 的指令／分支不同。 |
| #72 NativeDeDeletionState | DIFF → DIFF | sourceDeleteNpc 的例外涵蓋與指令不同。 |
| #72 NativeDeInstanceState | DIFF → DIFF | 其他 Name 建構子的呼叫／初始化次序不同。 |
| #76 Main | 預設 DIFF → DIFF；核准 SAME_LAYOUT → SAME_LAYOUT | 仍須原 layout 核准，沒有自動放寬。 |

其餘原 SAME、SAME_EFFECTS、SAME_LAYOUT 皆維持原狀，完整 595 列及所有模式見 `scratch/issue-174/entry-audit.json`。

### CI

- 程式提交 `65be0a1d5dc12b63b6c7cc51d171eff3332cb858` 的 [Semantic gap job](https://github.com/chaoren0418-arch/lito-launcher-server/actions/runs/42007901903/job/127179206980) 實際 SUCCESS。原始 log 確認四套自測分別 `517／73／119／169 ALL PASS`，並有 `SELFTEST ALL PASS`、`FALLBACK ALL PASS`；不是只看 non-blocking 工作流總狀態。
- 同提交的 Server 建置、Launcher 建置與 [打包 job](https://github.com/chaoren0418-arch/lito-launcher-server/actions/runs/42007901918/job/127179206800) 均 SUCCESS。完整 CI 新建 JAR 的預設結果為 2160 SAME／389 DIFF；這是額外整包比較，不能混入五批固定 staged 的改善數。
- [Server tests](https://github.com/chaoren0418-arch/lito-launcher-server/actions/runs/42007901903/job/127179207067) 與後續報告提交的最終狀態由 [PR #178 checks](https://github.com/chaoren0418-arch/lito-launcher-server/pull/178/checks) 查核。報告補充提交沒有改程式；交付前仍核對最後 HEAD 的全部五項 job，並保存 `scratch/issue-174/ci-status.json`、`ci-semantic.log`、`ci-server-tests.log`。

## 推測

#63 players 跨分支回傳暫存可能可用不改語意的 Java 表達對齊，或另立保留 edge/value 關係的證明；這需要下一任務裁定，本次不擴大為跨基本區塊正規化。

## 待驗證／未完成事項

- 固定 #62 仍有 Wind／DollAccepted 兩類 DIFF，按 #62 最新裁定由該批修改 staged Java；本任務沒有寫入其 worktree。
- 固定 #63 仍有 players 跨區塊暫存 DIFF；不宣稱 B02 已全部 SAME。
- 不做正式服務、DB、遊戲或真機驗收。SAME 是比較器證明結果，不替代 B-land 執行期回歸。
- 本機無 Bash；完整 shell／冷啟動 fallback 已由上述 Linux CI 實跑通過。

## 產出路徑與重跑

- 交付：`lito伺服器/LiTo381-Server/tools/semantic-diff/**`、`docs/combat-merge/exceptions-exempt.txt`、本報告。
- 可重跑腳本（本機、不入 git）：`scratch/issue-174/verify.py`、`batches.py`、`MethodProbe.java`。
- 證據（本機、不入 git）：`scratch/issue-174/inputs.json`、`entry-audit.json`、`{62,63,66,72,76}-{before,before-default,coverage-only,coverage-null,after,approved,no-coverage,no-nonnull,no-blocks}.{json,md,log}`、四套自測 log／fixture 結果、`approval.json`、`batches-final.log`。
- 建置產物：工具 `target/issue-174/{classes,baseline,inputs,NormalizationSelfTest,AdvancedSelfTest,FourthSelfTest,FifthSelfTest}`。未提交 class／JAR。

```powershell
& D:/dev-orchestrator/tools/python/python.exe -B -X utf8 scratch/issue-174/verify.py
& D:/dev-orchestrator/tools/python/python.exe -B -X utf8 scratch/issue-174/batches.py
```

跨平台正式入口：`bash lito伺服器/LiTo381-Server/tools/semantic-diff/selftest.sh` 與 `selftest-fallback.sh`。各批實際 JAR／classpath／entry 清單與核准旗標在 inputs.json；batches.py 固定修改前提交 `450178d`，不使用日後移動的 HEAD 作基準。

## 給下一棒的提示

由 Claude 審查前三項的 fail-closed 前提及有效 JVM 反例，特別是所有路徑非 null 交集、迴圈 producer 失效、區塊出口值及逐值複本數。合併後交 #62 執行器按最新留言修 Wind／DollAccepted 並重跑；#63 執行器處理 players 的跨區塊回傳形狀，保留本次同區塊限制。使用者只負責後續真機驗收，無需代做編譯或資料收集。
