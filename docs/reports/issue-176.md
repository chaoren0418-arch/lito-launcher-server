# Issue #176：semantic-diff 第六批

## 結論／做了什麼

完成四項比較器擴充與自測接線。新版另列 SAME_PATHS、DEVIATION、ORPHAN_IGNORED；不將後備成功混入 SAME。#65 仍有一般控制流程迴圈、#67 仍有迴圈及超過 4096 路徑的阻塞，依嚴格前提保留 DIFF。未修改 staged Java、src/main 或 docs/REPORT.md。

第二次嘗試修正 PR #193（`aae760a`）審查指出的孤立方法誤放行：參照 owner 為子類別時，現在涵蓋其所有父類別與介面；階層資料不完整時，同名同描述子方法保守保留差異。新增實際執行反例後，審查案例改判 DIFF，方法為 ONLY_RIGHT。

1. 類別層級 access-widen 只放寬指定 InnerClasses 可見性，所有引用該巢狀類別的選取 class 都適用，其他 flags 不變。
2. 預設以逐路徑 ASM Frame 與表示式驗證有序可觀察事件；含例外邊、標準成對 monitor handler、記憶體讀值版本及分支方向。
3. method-diff 僅作用於精確兩側方法，另附完整左右差異。已以 javap 核對兩個 producer 後追加兩條核准。
4. 原始完整右側 JAR 掃描直接參照、handle、bootstrap／condy 及 LDC 字串，再判定 private synthetic 孤立方法。

## 已驗證

### 第二次嘗試：參照 owner 解析邊界

- `JarIndex.orphanMethodReference` 使用原始 class header，將直接呼叫及 method handle 的參照保守展開至 owner、所有 superclass 與 superinterface；indy／condy 的 bootstrap 和巢狀參數共用此入口。不把 private 當成停止搜尋的依據。
- 此處採參照候選集合，不宣稱完整實作 JVM 唯一方法選擇：即使宣告被遮蔽，也保留祖先候選；任何節點 metadata 缺失，就不忽略具有該名稱與描述子的右側方法。這可能多保留 DIFF，但不能用未知階層證明孤立。
- `SixthSelfTest` 的隔離 ClassLoader 改為從同一份比較 JAR 按需載入 Fixture／Child／Middle，parent 仍為 platform loader。所有測試以 `-Xverify:all` 執行。
- 先在未修正的程式執行新測試，確實得到左側 `NoSuchMethodError`、右側 5，工具卻為 DIFF=0／ORPHAN_IGNORED=1，並在 DIFF 斷言失敗；證據為 `review-before-selftest.log` 與 `review-before-inherited-call.json`。

| 新增反例（各含單層、兩層繼承） | JVM 左／右 | 修正後預設 CLI |
|---|---|---|
| `orphan-inherited-call` | NoSuchMethodError／5 | DIFF=1；hidden 為 ONLY_RIGHT |
| `orphan-inherited-handle` | NoSuchMethodError／5 | DIFF=1；hidden 為 ONLY_RIGHT |
| `orphan-inherited-indy-argument` | NoSuchMethodError／5 | DIFF=1；hidden 為 ONLY_RIGHT |
| `orphan-inherited-condy-argument` | NoSuchMethodError／5 | DIFF=1；hidden 為 ONLY_RIGHT |
| `orphan-inherited-indy-bootstrap` | NoSuchMethodError／5 | DIFF=1；hidden 為 ONLY_RIGHT |
| `orphan-inherited-condy-bootstrap` | NoSuchMethodError／5 | DIFF=1；hidden 為 ONLY_RIGHT |

共 12 組同 bytes 的 CLI＋JVM 反例，逐案斷言 ORPHAN_IGNORED=0；另有缺少 Child metadata 的保守拒絕測試（僅檢查 CLI，不宣稱缺依賴的 JAR 可執行）。既有真正孤立 private synthetic 正例、左側獨有、非合成、LDC 同名字串及原 owner 直接呼叫／handle 反例全部保留且通過。

另外直接讀取審查者 `scratch/review-176/Review176Probe.java`，複製到本 worktree 編譯執行，得到 `RUNTIME left=java.lang.NoSuchMethodError right=5` 與 DIFF=1／ORPHAN_IGNORED=0。沒有在原始工作區寫入。

七批 1016 entries、#64 的 131-entry diagnostic、以及 #62／#63／#72／#76 無核准旗標的預設模式均已重跑。逐一核對 SHA 與指定提交報告，並以完整 JSON `classes`（含逐方法 detail）及 `counts` 對照前次輸出，全部完全相同；因此以下前後表及逐方法表仍適用，沒有新增 DIFF → SAME 的轉換，#66 真正孤立方法仍為 ORPHAN_IGNORED。#76 原 worktree 的 reference 已不存在，改讀 #72 路徑中的相同正式 JAR，重新核對 SHA `e1498871…` 後才執行。

### 自我測試

Windows Microsoft JDK 17.0.17、ASM 9.7；五套 Java 自測以 `-Xverify:all` 執行，共 **1037 checks（517＋73＋119＋169＋159）全過**，比前次 974 checks 新增 63 項斷言。SixthSelfTest 已接進正式 selftest.sh。舊測試中只差 NOP／丟棄常數／丟棄參數／純 stack 搬移者，依新規格改期待 SAME_PATHS；豁免邊界反例改用實際呼叫差異。原 #153／#166 的真正副作用及資料來源反例保持預設 DIFF。舊規則 ablation 同時關閉 paths，避免另一項獨立證明遮住停用結果。

同一份 framed class bytes 交 CLI 與隔離 ClassLoader；不載入遊戲應用類別。以下為實際執行的反例：

| 反例 | 左／右執行結果 | 比較 |
|---|---|---|
| paths-branch-inversion | 2／1 | DIFF |
| paths-call-order | 12／21 | DIFF |
| paths-monitor-callback | true／false | DIFF |
| paths-field-version | 1／2 | DIFF |
| paths-volatile-count | 1／2 | DIFF |
| paths-iinc | 7／8 | DIFF |
| paths-exception-order | 2／-1 | DIFF |
| paths-isub-order | 7／-7 | DIFF |
| paths-array-version | 1／2 | DIFF |

另外驗證：InnerClasses 反向／final／class flags／外部引用條目；method-diff typo／清單外方法；孤立方法的 LDC 字串反射、method handle 與直接呼叫，左側方法缺少時實際產生不同的反射或連結結果；非合成方法不忽略。volatile 多出一次 POP 丟棄讀取亦保持 DIFF，即使單執行緒回傳相同。標準 monitor self-covering handler 有正常與丟例外正例，非標準形狀拒絕；4096 限制與一般迴圈有明確拒絕斷言。

### 固定輸入與完整性

比較器修改前固定 `2d8be85804c6e78cc4aff4ade7971415aae4b29b`（包含 #174）；新版使用本提交來源。每批兩輪都用同一份 SHA 固定 staged bytes、reference、classpath 與既有核准；新版只另加本任務核准。參考候選 SHA `50635f7edca8ec6ce77787cc32371c7590b497e48ea202f8a679caf3f517b729`，正式 SHA `e1498871c7be6b287821c57ef5d95933db1740d9b4d4a80f6c3465f21d99d498`。

#62／#63／#72／#76 先用 git ls-remote 查核最新分支；#65／#66／#67 使用題目固定提交。逐一核對本機 JAR SHA 出現在該提交報告中，再複製到本 worktree 的工具 target/issue-176/inputs；沒有寫入原始專案。

| 批次 | 提交 | entries | staged SHA-256 |
|---|---|---:|---|
| #62 | `63da8a2` | 22 | `4ba5c47dadda30b4a7f2f1be66c0ca501ed0d453ad7ac19c618eda7e4456b0ea` |
| #63 | `00c33c4` | 108 | `5c945982ef9f9bb9e778aea16e87ffebfed70c9ca691f55edec88936bb580e38` |
| #65 | `e5fa958` | 98 | `5ccec2e8573d427fd98a8de93776a6b4ef81966c45ae16b1d26d358f5d41b7e6` |
| #66 | `8ae6d3c` | 120 | `b8d54f8315093327943495503edb9e7dbb0a725d73ea10da012d37738f1d52e4` |
| #67 | `cd84b99` | 323 | `8820abf5684b6fd6c9c55826372a21bad10f5f965880d4a954e57fe22d1640c6` |
| #72 | `f4b20cb` | 185 | `6b0f198773388779c8ea56d39836b33bc55716eff5858febc878d1e030760894` |
| #76 | `0d3101a` | 160 | `e347988dcf41b60cb915195ffa98dc2eff74303ae3c9f0400c2800bf5bc2b553` |

七批合計 **1016 entries**，精確等於各批 verify_entries；missing／extra／ONLY_LEFT／ONLY_RIGHT 均為 0。額外讀取 #64 的 131-entry diagnostic.jar 只作部分重跑（維持 131 SAME）；其缺少 CubeVisibility 四個 entry 的既有跨批編譯阻塞仍在，未把這項診斷當成完整 B03 驗收。InnerClasses 授權用有效 fixture 驗證，#64 最終 135 entries 留給 #69／#64 接續。

### 前後摘要

表中欄序為 SAME／SAME_EFFECTS／SAME_LAYOUT／SAME_PATHS／DEVIATION／ORPHAN_IGNORED／DIFF；計數為 class，不是方法。

| 批次 | 修改前 | 修改後 |
|---|---|---|
| #62 | 19／1／0／0／0／0／2 | 19／1／0／1／0／0／1 |
| #63 | 107／0／0／0／0／0／1 | 107／0／0／1／0／0／0 |
| #65 | 95／0／0／0／0／0／3 | 95／0／0／1／1／0／1 |
| #66 | 115／3／0／0／0／0／2 | 115／3／0／1／0／1／0 |
| #67 | 311／0／0／0／0／0／12 | 311／0／0／10／0／0／2 |
| #72 | 182／0／0／0／0／0／3 | 182／0／0／0／0／0／3 |
| #76 | 158／1／1／0／0／0／0 | 158／1／1／0／0／0／0 |

#67 的修改前已是 311 SAME／12 DIFF；題目報告的 298／25 使用較早工具，差額屬 #174 既有成果，沒有算進本次改善。#62／#63 的原 DIFF 可被新證明判 SAME_PATHS，完全沒有原 DIFF 混入 SAME；#72／#76 的全部 entry 狀態維持不變。

### #65／#66／#67 逐方法新結果

方法名省略共同 owner 前綴 `local/lito381/server/`；描述子完整保留。其餘方法在線性／既有正規化已通過，不重複列為新成果。

| 批次／方法 | 狀態 | 原因 |
|---|---|---|
| #65 `NativeSourceNpcDamageDisplay#questDamage(Llocal/lito381/server/GameHub;JLlocal/lito381/server/ClientSession;JILlocal/lito381/server/NativeSourceNpcDamageDisplay$Receipt;)Llocal/lito381/server/BattleQuestEncounter$Hit;` | SAME_PATHS | path multisets equal (36) |
| #65 `NativeSourceNpcDamageDisplay#timeDamage(Llocal/lito381/server/GameHub;JLlocal/lito381/server/ClientSession;JILlocal/lito381/server/NativeSourceNpcDamageDisplay$Receipt;)Llocal/lito381/server/GameHub$TimeAttackActorHit;` | SAME_PATHS | path multisets equal (36) |
| #65 `NativeSourceNpcEscape#nativeSourceFirstViewHandled(Llocal/lito381/server/GameHub;Ljava/util/List;Ljava/lang/Object;Llocal/lito381/server/BattleRules$Mode;JIIILlocal/lito381/server/ClientSession;JIILjava/lang/String;Ljava/lang/Object;)Z` | DIFF | rejected: normal control-flow cycle |
| #65 `NativeSourceNpcEscape#nativeSourceNpcPlayerFirstHandled(Llocal/lito381/server/GameHub;Ljava/util/List;Ljava/lang/Object;Llocal/lito381/server/BattleRules$Mode;JIIILlocal/lito381/server/ClientSession;JIILjava/lang/String;Ljava/lang/Object;Ljava/lang/Object;)Z` | DIFF | rejected: normal control-flow cycle |
| #65 `NativeSourcePhysicalNpc#sourceCurrent(Llocal/lito381/server/NativeSourcePhysicalNpc$Origin;)Z` | DEVIATION | 精確核准；完整左右差異另列於本節後。 |
| #65 `NativeSourcePhysicalNpc#targetCurrent(Llocal/lito381/server/NativeSourcePhysicalNpc$Origin;)Z` | DEVIATION | 精確核准；完整左右差異另列於本節後。 |
| #66 `NativeSourcePoisonNpc#lambda$status$1(Llocal/lito381/server/NativeAreaMaterialAction$Context;Llocal/lito381/server/NativeSourcePoisonNpc$Status;Llocal/lito381/server/ClientSession;[BLjava/lang/String;)V` | ORPHAN_IGNORED | private synthetic; no JAR references or matching LDC string |
| #66 `NativeStaticNpcScene#heading(Llocal/lito381/server/GameHub;Llocal/lito381/server/ClassicWorldNpcCatalog$StaticNpc;Llocal/lito381/server/ClientSession;)I` | SAME_PATHS | path multisets equal (17) |
| #66 `NativeStaticNpcScene#restore(Llocal/lito381/server/GameHub;Llocal/lito381/server/ClassicWorldNpcCatalog$StaticNpc;ILlocal/lito381/server/ClientSession;)Z` | SAME_PATHS | path multisets equal (30) |
| #66 `NativeStaticNpcScene#transform(Llocal/lito381/server/GameHub;Llocal/lito381/server/ClassicWorldNpcCatalog$StaticNpc;IILlocal/lito381/server/ClientSession;)Z` | SAME_PATHS | path multisets equal (28) |
| #66 `NativeStaticNpcScene#turn(Llocal/lito381/server/GameHub;Llocal/lito381/server/ClassicWorldNpcCatalog$StaticNpc;ILlocal/lito381/server/ClientSession;)Z` | SAME_PATHS | path multisets equal (20) |
| #67 `BattleAbilityStatus#activateDexterity(IJII)I` | SAME_PATHS | path multisets equal (11) |
| #67 `BattleAbilityStatus#activateRoyalAura(IJIIIII)I` | SAME_PATHS | path multisets equal (49) |
| #67 `BattleAbilityStatus#activateStrength(IJII)I` | SAME_PATHS | path multisets equal (11) |
| #67 `BattleAbilityStatus#removeSkill(I)Z` | SAME_PATHS | path multisets equal (91) |
| #67 `BattleAbilityStatus$Protection#applyTo(I)I` | SAME_PATHS | path multisets equal (6) |
| #67 `BattleCompanion#applyDamage(I)Llocal/lito381/server/BattleCompanion$Damage;` | SAME_PATHS | path multisets equal (68) |
| #67 `BattleCompanion#receiveItemAndOnGetItemPersisted(Llocal/lito381/server/RuntimeInventory$Item;JJLlocal/lito381/server/DurableCommit;)Llocal/lito381/server/BattleCompanion$GivenItemReceipt;` | SAME_PATHS | path multisets equal (2316) |
| #67 `BattleCounterMirrorStatus#resolve(JIIIZ)Llocal/lito381/server/BattleCounterMirrorStatus$Decision;` | SAME_PATHS | path multisets equal (75) |
| #67 `BattleElementalFallDownStatus#activate(JII)Llocal/lito381/server/BattleElementalFallDownStatus$Activation;` | SAME_PATHS | path multisets equal (76) |
| #67 `BattleGroundEffects#cast(Llocal/lito381/server/BattleSpellCatalog$GroundSpell;JIIIILlocal/lito381/server/ArenaMapCatalog$Arena;J)Llocal/lito381/server/BattleGroundEffects$CastResult;` | DIFF | rejected: normal control-flow cycle |
| #67 `BattleResistFearStatus#activate(JII)Z` | SAME_PATHS | path multisets equal (39) |
| #67 `BattleSpellCatalog$HealingSpell#npcHpDelta(II)I` | SAME_PATHS | path multisets equal (9) |
| #67 `BattleSpellCatalog$WindShackleSpell#<init>(IIIIIIIIIIIIIIIIIIIIIIIIIDDZZIZJILjava/lang/String;)V` | SAME_PATHS | path multisets equal (145) |
| #67 `BattleTimedStatus#activateBrave(JII)V` | SAME_PATHS | path multisets equal (62) |
| #67 `BattleTimedStatus#cancelMagicEffects()Llocal/lito381/server/BattleTimedStatus$MagicCancellation;` | DIFF | rejected: path limit 4096 exceeded |
| #67 `BattleTimedStatus#clear()V` | SAME_PATHS | path multisets equal (68) |
| #67 `BattleTimedStatus#consumeCounterMagic(J)Z` | SAME_PATHS | path multisets equal (9) |
| #67 `BattleTimedStatus#expireBrave(J)Z` | SAME_PATHS | path multisets equal (9) |
| #67 `BattleWeightStatus#activate(IJI)V` | SAME_PATHS | path multisets equal (23) |
| #67 `BattleWeightStatus#clear()V` | SAME_PATHS | path multisets equal (6) |
| #67 `BattleWeightStatus#expire(J)Z` | SAME_PATHS | path multisets equal (27) |
| #67 `BattleWindShackleStatus#snapshot(J)Llocal/lito381/server/BattleWindShackleStatus$Snapshot;` | SAME_PATHS | path multisets equal (23) |

### #67 原報告列出但本次後備未啟用的方法

以下原報告的剩餘方法在含 #174 的修改前工具已通過，沒有重新標成 SAME_PATHS；同類仍可能有上述其他方法 DIFF。特別是 Harmony.advance 與兩個 pollPoisons 的迴圈，不是本次逐路徑證明放行。

- `BattleBurningWeaponStatus#activate(JIII)Z`：既有 SAME。
- `BattleCubeHarmonyEffects#advance(JLjava/util/List;Llocal/lito381/server/BattleCubeHarmonyEffects$Alliance;)Ljava/util/List;`：既有 SAME。
- `BattleDragonSkinStatus#activate(JII)Z`：既有 SAME。
- `BattleDragonSkinStatus#clear()V`：既有 SAME。
- `BattleFireBlessStatus#activate(JII)Z`：既有 SAME。
- `BattleFireWeaponStatus#activate(JII)Z`：既有 SAME。
- `BattleGuardBrakeStatus#activate(JII)Z`：既有 SAME。
- `BattleNaturesTouchStatus#activate(JI)Z`：既有 SAME。
- `BattleQuestEncounter#pollPoisons(JII)Ljava/util/List;`：既有 SAME。
- `BattleStormEyeStatus#activate(JIII)Z`：既有 SAME。
- `BattleStormShotStatus#activate(JIII)Z`：既有 SAME。
- `BattleTimeAttackEncounter#pollPoisons(JII)Ljava/util/List;`：既有 SAME。
- `BattleTimedStatus#activateElementalFire(JI)Z`：既有 SAME。
- `BattleTimedStatus#activateSoulOfFlame(JI)Z`：既有 SAME。
- `BattleTournamentNpcPhysicalDamage#againstPlayer(ILlocal/lito381/server/SourceNpcPhysicalStats;Llocal/lito381/server/SourcePhysicalGrowthStats;IIIIIIIJI)Llocal/lito381/server/BattleTournamentNpcPhysicalDamage$Outcome;`：既有 SAME。
- `BattleWindShackleStatus#cancel()Z`：既有 SAME。
- `BattleWindShotStatus#activate(JII)Z`：既有 SAME。

### #62／#63／#72／#76 原 DIFF 逐 entry 對照

| 批次／entry | 前 → 後 | 方法與原因 |
|---|---|---|
| #62 `NativeCompanionWindAction` | DIFF → SAME_PATHS | publish(Llocal/lito381/server/ClientSession;Llocal/lito381/server/NativeAreaMaterialAction$Context;JI)V path multisets equal (19) |
| #62 `NativeSourceDollAccepted` | DIFF → DIFF | left=90:[93:java/io/UncheckedIOException  ] right=88:[91:java/io/UncheckedIOException  ]; path event multisets differ (left=52, right=52) |
| #63 `NativeAcceptedAreaFreezeMutation` | DIFF → SAME_PATHS | players(Ljava/util/List;)Ljava/util/List; path multisets equal (9) |
| #72 `NativeDeCharacterFields` | DIFF → DIFF | only left |
| #72 `NativeDeCharacterFields$CombatSkillDerived` | DIFF → DIFF | left=GETSTATIC local/lito381/server/NativeDeCharacterFields.SKU1:Ljava/util/HashMap; right=GETSTATIC unresolved(local/lito381/server/NativeDeCharacterFields).SKU1:Ljava/util/HashMap;; paths rejected: normal control-flow cycle |
| #72 `NativeDeInstanceState` | DIFF → DIFF | only left |

#62 的 Wind 方法保留相同配置／呼叫／陣列寫入順序後，只消除 stack/local 的運送差異；DollAccepted 在呼叫邊界前後重新 GETFIELD，記憶體版本不同，維持 DIFF。#63 players 是原先已確認的跨分支回傳暫存，逐路徑不合併值來源後得到 SAME_PATHS。#76 核准模式原本就 0 DIFF，沒有新增放寬。

### 不傳核准清單的防誤判對照

另以同一固定輸入完全不傳核准旗標，重跑以下四批；仍沒有原 DIFF 變成 SAME。#76 Main 保留 DIFF（含一般控制流程迴圈），沒有因新版預設自動略過差異。

| 批次 | 修改前 SAME／DIFF | 修改後 SAME／SAME_PATHS／DIFF |
|---|---:|---:|
| #62 | 19／3 | 19／1／2 |
| #63 | 107／1 | 107／1／0 |
| #72 | 181／4 | 181／0／4 |
| #76 | 159／1 | 159／0／1 |

### 剩餘一般迴圈的 javap 證據

原始指令 offset，並非正規化標籤；兩側皆有正常回邊，不屬 monitor handler 自我涵蓋。

| 方法 | staged 回邊 | candidate 回邊 |
|---|---|---|
| `NativeSourceNpcEscape#nativeSourceFirstViewHandled` | 895 → 720 | 898 → 722 |
| `NativeSourceNpcEscape#nativeSourceNpcPlayerFirstHandled` | 900 → 725 | 903 → 727 |
| `BattleGroundEffects#cast` | 224 → 157 | 221 → 154 |

這些方法即使目前差異看起來只在局部排列，也不符合本 Issue 的「兩側一般控制流程均無迴圈」前提，故仍 DIFF。javap 完整輸出與回邊 JSON 保存在 scratch/issue-176。

### 兩條 DEVIATION 的證明與完整差異

producer javap 的所有 IRETURN 運算元：sourceState offset 25 → {-1}、224 → {0,1}；targetState offset 21 → {-1}、85 → {0,1}。兩個 consumer 都是 DUP、IFLT 9、offset 8 IRETURN，負值進 fallback。完整理由、核准 URL 與前驅 offset 見 [deviations.md](../combat-merge/deviations.md)。

#### `sourceCurrent(Llocal/lito381/server/NativeSourcePhysicalNpc$Origin;)Z`

```text
method sourceCurrent(Llocal/lito381/server/NativeSourcePhysicalNpc$Origin;)Z insn[2]: left=ISTORE 1 right=DUP
LEFT
(Llocal/lito381/server/NativeSourcePhysicalNpc$Origin;)Z
0x8


||||pvis=|pinv=|lvtvis=|lvtinv=

[]
[ALOAD 0, INVOKESTATIC local/lito381/server/NativeSourceRawNpc.sourceState(Llocal/lito381/server/NativeSourcePhysicalNpc$Origin;)I, ISTORE 1, ILOAD 1, IFLT L11, ILOAD 1, IFEQ L9, CONST Integer:1, GOTO L10, CONST Integer:0, IRETURN, ALOAD 0, INVOKEVIRTUAL local/lito381/server/NativeSourcePhysicalNpc$Origin.source()Llocal/lito381/server/NativeSourceDamageDisplay$Actor;, INVOKEVIRTUAL local/lito381/server/NativeSourceDamageDisplay$Actor.pc()Llocal/lito381/server/ClientSession;, ASTORE 2, ALOAD 0, INVOKEVIRTUAL local/lito381/server/NativeSourcePhysicalNpc$Origin.source()Llocal/lito381/server/NativeSourceDamageDisplay$Actor;, ALOAD 0, INVOKEVIRTUAL local/lito381/server/NativeSourcePhysicalNpc$Origin.source()Llocal/lito381/server/NativeSourceDamageDisplay$Actor;, INVOKEVIRTUAL local/lito381/server/NativeSourceDamageDisplay$Actor.life()Ljava/lang/Object;, INVOKEVIRTUAL local/lito381/server/NativeSourceDamageDisplay$Actor.valid(Ljava/lang/Object;)Z, IFEQ L58, ALOAD 2, CONST str:"playerHp", INVOKESTATIC local/lito381/server/NativeSourceDamageDisplay.n(Llocal/lito381/server/ClientSession;Ljava/lang/String;)I, IFLE L58, ALOAD 2, CONST str:"observer", INVOKESTATIC local/lito381/server/CombatAccess.field(Ljava/lang/Object;Ljava/lang/String;)Ljava/lang/Object;, CHECKCAST java/lang/Boolean, INVOKEVIRTUAL java/lang/Boolean.booleanValue()Z, IFNE L58, ALOAD 2, CONST str:"hub", INVOKESTATIC local/lito381/server/CombatAccess.field(Ljava/lang/Object;Ljava/lang/String;)Ljava/lang/Object;, ALOAD 0, INVOKEVIRTUAL local/lito381/server/NativeSourcePhysicalNpc$Origin.scene()Llocal/lito381/server/NativeSourceCubeOrigin$Scene;, INVOKEVIRTUAL local/lito381/server/NativeSourceCubeOrigin$Scene.hub()Llocal/lito381/server/GameHub;, IF_ACMPNE L58, ALOAD 2, CONST str:"phase", INVOKESTATIC local/lito381/server/CombatAccess.field(Ljava/lang/Object;Ljava/lang/String;)Ljava/lang/Object;, INVOKESTATIC java/lang/String.valueOf(Ljava/lang/Object;)Ljava/lang/String;, CONST str:"BATTLE", INVOKEVIRTUAL java/lang/String.equals(Ljava/lang/Object;)Z, IFEQ L58, ALOAD 2, CONST str:"currentRoomId", INVOKESTATIC local/lito381/server/CombatAccess.field(Ljava/lang/Object;Ljava/lang/String;)Ljava/lang/Object;, CHECKCAST java/lang/Long, INVOKEVIRTUAL java/lang/Long.longValue()J, ALOAD 0, INVOKEVIRTUAL local/lito381/server/NativeSourcePhysicalNpc$Origin.scene()Llocal/lito381/server/NativeSourceCubeOrigin$Scene;, INVOKEVIRTUAL local/lito381/server/NativeSourceCubeOrigin$Scene.id()J, LCMP, IFNE L58, CONST Integer:1, GOTO L59, CONST Integer:0, IRETURN]
RIGHT
(Llocal/lito381/server/NativeSourcePhysicalNpc$Origin;)Z
0x8


||||pvis=|pinv=|lvtvis=|lvtinv=

[]
[ALOAD 0, INVOKESTATIC local/lito381/server/NativeSourceRawNpc.sourceState(Llocal/lito381/server/NativeSourcePhysicalNpc$Origin;)I, DUP, IFLT L5, IRETURN, POP, ALOAD 0, INVOKEVIRTUAL local/lito381/server/NativeSourcePhysicalNpc$Origin.source()Llocal/lito381/server/NativeSourceDamageDisplay$Actor;, INVOKEVIRTUAL local/lito381/server/NativeSourceDamageDisplay$Actor.pc()Llocal/lito381/server/ClientSession;, ASTORE 1, ALOAD 0, INVOKEVIRTUAL local/lito381/server/NativeSourcePhysicalNpc$Origin.source()Llocal/lito381/server/NativeSourceDamageDisplay$Actor;, ALOAD 0, INVOKEVIRTUAL local/lito381/server/NativeSourcePhysicalNpc$Origin.source()Llocal/lito381/server/NativeSourceDamageDisplay$Actor;, INVOKEVIRTUAL local/lito381/server/NativeSourceDamageDisplay$Actor.life()Ljava/lang/Object;, INVOKEVIRTUAL local/lito381/server/NativeSourceDamageDisplay$Actor.valid(Ljava/lang/Object;)Z, IFEQ L53, ALOAD 1, CONST str:"playerHp", INVOKESTATIC local/lito381/server/NativeSourceDamageDisplay.n(Llocal/lito381/server/ClientSession;Ljava/lang/String;)I, IFLE L53, ALOAD 1, CONST str:"observer", INVOKESTATIC local/lito381/server/CombatAccess.field(Ljava/lang/Object;Ljava/lang/String;)Ljava/lang/Object;, CHECKCAST java/lang/Boolean, INVOKEVIRTUAL java/lang/Boolean.booleanValue()Z, IFNE L53, ALOAD 1, CONST str:"hub", INVOKESTATIC local/lito381/server/CombatAccess.field(Ljava/lang/Object;Ljava/lang/String;)Ljava/lang/Object;, ALOAD 0, INVOKEVIRTUAL local/lito381/server/NativeSourcePhysicalNpc$Origin.scene()Llocal/lito381/server/NativeSourceCubeOrigin$Scene;, INVOKEVIRTUAL local/lito381/server/NativeSourceCubeOrigin$Scene.hub()Llocal/lito381/server/GameHub;, IF_ACMPNE L53, ALOAD 1, CONST str:"phase", INVOKESTATIC local/lito381/server/CombatAccess.field(Ljava/lang/Object;Ljava/lang/String;)Ljava/lang/Object;, INVOKESTATIC java/lang/String.valueOf(Ljava/lang/Object;)Ljava/lang/String;, CONST str:"BATTLE", INVOKEVIRTUAL java/lang/String.equals(Ljava/lang/Object;)Z, IFEQ L53, ALOAD 1, CONST str:"currentRoomId", INVOKESTATIC local/lito381/server/CombatAccess.field(Ljava/lang/Object;Ljava/lang/String;)Ljava/lang/Object;, CHECKCAST java/lang/Long, INVOKEVIRTUAL java/lang/Long.longValue()J, ALOAD 0, INVOKEVIRTUAL local/lito381/server/NativeSourcePhysicalNpc$Origin.scene()Llocal/lito381/server/NativeSourceCubeOrigin$Scene;, INVOKEVIRTUAL local/lito381/server/NativeSourceCubeOrigin$Scene.id()J, LCMP, IFNE L53, CONST Integer:1, GOTO L54, CONST Integer:0, IRETURN]
```

#### `targetCurrent(Llocal/lito381/server/NativeSourcePhysicalNpc$Origin;)Z`

```text
method targetCurrent(Llocal/lito381/server/NativeSourcePhysicalNpc$Origin;)Z insn[2]: left=ISTORE 1 right=DUP
LEFT
(Llocal/lito381/server/NativeSourcePhysicalNpc$Origin;)Z
0x8


||||pvis=|pinv=|lvtvis=|lvtinv=

[]
[ALOAD 0, INVOKESTATIC local/lito381/server/NativeSourceGroundNpc.targetState(Llocal/lito381/server/NativeSourcePhysicalNpc$Origin;)I, ISTORE 1, ILOAD 1, IFLT L11, ILOAD 1, IFEQ L9, CONST Integer:1, GOTO L10, CONST Integer:0, IRETURN, ALOAD 0, INVOKEVIRTUAL local/lito381/server/NativeSourcePhysicalNpc$Origin.target()Llocal/lito381/server/NativeSourceCubeOrigin$Target;, ASTORE 2, ALOAD 2, INVOKEVIRTUAL local/lito381/server/NativeSourceCubeOrigin$Target.owner()Ljava/lang/Object;, CONST str:"sourceProcEncounterGeneration", INVOKESTATIC local/lito381/server/CombatAccess.field(Ljava/lang/Object;Ljava/lang/String;)Ljava/lang/Object;, ALOAD 2, INVOKEVIRTUAL local/lito381/server/NativeSourceCubeOrigin$Target.generation()Ljava/lang/Object;, IF_ACMPNE L67, ALOAD 2, INVOKEVIRTUAL local/lito381/server/NativeSourceCubeOrigin$Target.actor()Ljava/lang/Object;, INVOKESTATIC local/lito381/server/NativeSourceArenaPoison.rawLife(Ljava/lang/Object;)Ljava/lang/Object;, ALOAD 2, INVOKEVIRTUAL local/lito381/server/NativeSourceCubeOrigin$Target.life()Ljava/lang/Object;, IF_ACMPNE L67, ALOAD 2, INVOKEVIRTUAL local/lito381/server/NativeSourceCubeOrigin$Target.actor()Ljava/lang/Object;, CONST str:"objectId", INVOKESTATIC local/lito381/server/CombatAccess.field(Ljava/lang/Object;Ljava/lang/String;)Ljava/lang/Object;, CHECKCAST java/lang/Long, INVOKEVIRTUAL java/lang/Long.longValue()J, ALOAD 2, INVOKEVIRTUAL local/lito381/server/NativeSourceCubeOrigin$Target.id()J, LCMP, IFNE L67, ALOAD 2, INVOKEVIRTUAL local/lito381/server/NativeSourceCubeOrigin$Target.actor()Ljava/lang/Object;, CONST str:"hp", INVOKESTATIC local/lito381/server/CombatAccess.field(Ljava/lang/Object;Ljava/lang/String;)Ljava/lang/Object;, CHECKCAST java/lang/Integer, INVOKEVIRTUAL java/lang/Integer.intValue()I, IFLE L67, ALOAD 2, INVOKEVIRTUAL local/lito381/server/NativeSourceCubeOrigin$Target.actor()Ljava/lang/Object;, CONST str:"x", INVOKESTATIC local/lito381/server/CombatAccess.field(Ljava/lang/Object;Ljava/lang/String;)Ljava/lang/Object;, CHECKCAST java/lang/Integer, INVOKEVIRTUAL java/lang/Integer.intValue()I, ALOAD 0, INVOKEVIRTUAL local/lito381/server/NativeSourcePhysicalNpc$Origin.x()I, IF_ICMPNE L67, ALOAD 2, INVOKEVIRTUAL local/lito381/server/NativeSourceCubeOrigin$Target.actor()Ljava/lang/Object;, CONST str:"y", INVOKESTATIC local/lito381/server/CombatAccess.field(Ljava/lang/Object;Ljava/lang/String;)Ljava/lang/Object;, CHECKCAST java/lang/Integer, INVOKEVIRTUAL java/lang/Integer.intValue()I, ALOAD 0, INVOKEVIRTUAL local/lito381/server/NativeSourcePhysicalNpc$Origin.y()I, IF_ICMPNE L67, ALOAD 2, INVOKESTATIC local/lito381/server/NativeSourceCubeNpc.member(Llocal/lito381/server/NativeSourceCubeOrigin$Target;)Z, IFEQ L67, CONST Integer:1, GOTO L68, CONST Integer:0, IRETURN]
RIGHT
(Llocal/lito381/server/NativeSourcePhysicalNpc$Origin;)Z
0x8


||||pvis=|pinv=|lvtvis=|lvtinv=

[]
[ALOAD 0, INVOKESTATIC local/lito381/server/NativeSourceGroundNpc.targetState(Llocal/lito381/server/NativeSourcePhysicalNpc$Origin;)I, DUP, IFLT L5, IRETURN, POP, ALOAD 0, INVOKEVIRTUAL local/lito381/server/NativeSourcePhysicalNpc$Origin.target()Llocal/lito381/server/NativeSourceCubeOrigin$Target;, ASTORE 1, ALOAD 1, INVOKEVIRTUAL local/lito381/server/NativeSourceCubeOrigin$Target.owner()Ljava/lang/Object;, CONST str:"sourceProcEncounterGeneration", INVOKESTATIC local/lito381/server/CombatAccess.field(Ljava/lang/Object;Ljava/lang/String;)Ljava/lang/Object;, ALOAD 1, INVOKEVIRTUAL local/lito381/server/NativeSourceCubeOrigin$Target.generation()Ljava/lang/Object;, IF_ACMPNE L62, ALOAD 1, INVOKEVIRTUAL local/lito381/server/NativeSourceCubeOrigin$Target.actor()Ljava/lang/Object;, INVOKESTATIC local/lito381/server/NativeSourceArenaPoison.rawLife(Ljava/lang/Object;)Ljava/lang/Object;, ALOAD 1, INVOKEVIRTUAL local/lito381/server/NativeSourceCubeOrigin$Target.life()Ljava/lang/Object;, IF_ACMPNE L62, ALOAD 1, INVOKEVIRTUAL local/lito381/server/NativeSourceCubeOrigin$Target.actor()Ljava/lang/Object;, CONST str:"objectId", INVOKESTATIC local/lito381/server/CombatAccess.field(Ljava/lang/Object;Ljava/lang/String;)Ljava/lang/Object;, CHECKCAST java/lang/Long, INVOKEVIRTUAL java/lang/Long.longValue()J, ALOAD 1, INVOKEVIRTUAL local/lito381/server/NativeSourceCubeOrigin$Target.id()J, LCMP, IFNE L62, ALOAD 1, INVOKEVIRTUAL local/lito381/server/NativeSourceCubeOrigin$Target.actor()Ljava/lang/Object;, CONST str:"hp", INVOKESTATIC local/lito381/server/CombatAccess.field(Ljava/lang/Object;Ljava/lang/String;)Ljava/lang/Object;, CHECKCAST java/lang/Integer, INVOKEVIRTUAL java/lang/Integer.intValue()I, IFLE L62, ALOAD 1, INVOKEVIRTUAL local/lito381/server/NativeSourceCubeOrigin$Target.actor()Ljava/lang/Object;, CONST str:"x", INVOKESTATIC local/lito381/server/CombatAccess.field(Ljava/lang/Object;Ljava/lang/String;)Ljava/lang/Object;, CHECKCAST java/lang/Integer, INVOKEVIRTUAL java/lang/Integer.intValue()I, ALOAD 0, INVOKEVIRTUAL local/lito381/server/NativeSourcePhysicalNpc$Origin.x()I, IF_ICMPNE L62, ALOAD 1, INVOKEVIRTUAL local/lito381/server/NativeSourceCubeOrigin$Target.actor()Ljava/lang/Object;, CONST str:"y", INVOKESTATIC local/lito381/server/CombatAccess.field(Ljava/lang/Object;Ljava/lang/String;)Ljava/lang/Object;, CHECKCAST java/lang/Integer, INVOKEVIRTUAL java/lang/Integer.intValue()I, ALOAD 0, INVOKEVIRTUAL local/lito381/server/NativeSourcePhysicalNpc$Origin.y()I, IF_ICMPNE L62, ALOAD 1, INVOKESTATIC local/lito381/server/NativeSourceCubeNpc.member(Llocal/lito381/server/NativeSourceCubeOrigin$Target;)Z, IFEQ L62, CONST Integer:1, GOTO L63, CONST Integer:0, IRETURN]
```

## CI

本次修正不沿用舊提交的 CI 結論；本次 HEAD 的五項 job 收據見 [PR #193 checks](https://github.com/chaoren0418-arch/lito-launcher-server/pull/193/checks)，另在本機保存 `scratch/issue-176/review-ci-status.json` 與 `review-ci-semantic-proof.txt`。Semantic gap 為 non-blocking，須檢查實際 step／日誌中的 1037 checks、SELFTEST ALL PASS 與 FALLBACK ALL PASS，不只看 workflow 總狀態。

以下為前次提交的歷史收據：

- 程式提交 `04f1dccdeb940262492a2cdc8f368e4823d78415` 的 [Semantic gap job](https://github.com/chaoren0418-arch/lito-launcher-server/actions/runs/42034482198/job/127268940066) **SUCCESS**。已讀取實際日誌，五套自測分別為 517／73／119／169／96 ALL PASS，另有 `SELFTEST ALL PASS` 與 `FALLBACK ALL PASS`；不是只看 non-blocking workflow 的總狀態。
- 同提交的 Server 建置、Launcher 建置與 [打包 job](https://github.com/chaoren0418-arch/lito-launcher-server/actions/runs/42034482218/job/127268940023) 均 **SUCCESS**。
- CI 整包 Maven 產物對正式 reference 的額外結果為 2160 SAME／28 SAME_PATHS／361 DIFF；這是另一組輸入，不能混入七批固定 staged 的改善數。
- Server tests 與後續僅補報告提交的最後 HEAD，以 [PR #193 checks](https://github.com/chaoren0418-arch/lito-launcher-server/pull/193/checks) 為最終收據；交付時仍核對五項 job。已保存本機 `scratch/issue-176/ci-semantic-proof.txt`，最終 CI 狀態另存 `ci-status.json`。

## 推測

SAME_PATHS 是本工具的有界事件模型證明，不是實機戰鬥驗收。某些保留 DIFF 的大方法可能語意等價，但目前不具備無迴圈或資源上限前提；不以手工推測覆蓋結果。

## 待驗證／未完成事項

- #65 Escape 兩方法含一般迴圈，按本任務限制保留 DIFF，需主控決定後續 CFG 證明或來源端處理。
- #67 Ground.cast 有一般迴圈；Timed.cancelMagicEffects 超過 4096 路徑。兩者維持 DIFF，不擴大核准。
- #64 完整編譯依賴 #69 的 Output Java 放寬，本任務不改跨批來源。
- 未啟動伺服器、登入器、遊戲，未讀機密設定或封存，未連正式 DB；B-land 的 consumer 0／1 回歸與真機驗收另行安排。

## 產出路徑與重跑

交付：`lito伺服器/LiTo381-Server/tools/semantic-diff/**`、`docs/combat-merge/access-widen-approved.txt`、`deviations.txt`、`deviations.md`、本報告。

本機證據（不入 git）：`scratch/issue-176/{verify.py,batches.py,inputs.json,producer-audit.py,producer-audit.json,*-javap.txt,*-before.json,*-after.json,*.log}`；工具與 fixture class/JAR 位於工具 `target/issue-176/`。inputs.json 包含每個原始 staged 路徑、固定複本 SHA、reference、entries 與實際裁切核准旗標。

本次退回修正的重跑入口：`scratch/issue-176/recheck-review.py`；收據：`review-recheck.json`、`*-review-after*.json`、`review-probe.log`、`review-probe/inherited-reference.json`。審查者原始碼複本及 probe class/JAR 均留在本 worktree 的 scratch／target 內。

```powershell
& D:/dev-orchestrator/tools/python/python.exe -B -X utf8 -c "import sys,runpy; sys.path.insert(0,'scratch/issue-176'); runpy.run_path('scratch/issue-176/recheck-review.py')"
```

```powershell
& D:/dev-orchestrator/tools/python/python.exe -B -X utf8 scratch/issue-176/verify.py
& D:/dev-orchestrator/tools/python/python.exe -B -X utf8 -c "import sys; sys.path.insert(0,'scratch/issue-176'); import batches"
& D:/dev-orchestrator/tools/python/python.exe -B -X utf8 scratch/issue-176/producer-audit.py
```

跨平台自測：`bash lito伺服器/LiTo381-Server/tools/semantic-diff/selftest.sh` 與 `selftest-fallback.sh`。批次通用 CLI 如下；`entries.txt` 必須取指定提交的 batches.json／a-batches.json verify_entries，核准清單須裁切到該批精確方法或 InnerClasses 條目。

```text
semantic-diff --left <staged.jar> --right <reference.jar> --left-classpath <reference.jar>
  --classes <entries.txt> --json <result.json> --md <result.md>
  [--effects-methods <batch-effects.txt>] [--layout-methods <batch-layout.txt>]
  [--access-widen <batch-access.txt>] [--deviations <batch-deviations.txt>]
  [--exceptions-exempt <batch-exceptions.txt>]
```

## 給下一棒的提示

本次先由 Claude 重審 PR #193 的孤立方法參照修正：核對原始 header 階層候選、未知 metadata 的保守拒絕，以及 12 組 owner 為子類別的有效 bytes 回歸。無需新增逐方法核准；既有批次結果全部維持。

由 Claude 審查逐路徑模型的分支方向、例外優先序、讀值版本、monitor 配對與核准範圍。#66 可使用新版重驗並更新交付；#65／#67 剩餘方法交主控，不能在未滿足前提時逐一補豁免。#62／#63 重跑須分列 SAME_PATHS，不要求它們混成 SAME。#64 與 #69 接續跨批 Java 編譯，B-land 補 consumer 0／1 的實際回歸；使用者只負責後續真機驗收。
