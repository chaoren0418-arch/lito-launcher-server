# B09 形狀差異清單（留給 M2 離線回歸驗收）

依 #70 主控裁定（2026-11-10，[留言](https://github.com/chaoren0418-arch/lito-launcher-server/issues/70#issuecomment-6771730899)）：
呼叫與欄位讀寫多重集合相同、只差在形狀的方法，不再追求靜態 DIFF=0，逐條列在這裡，由 M2 的離線回歸驗收；9 個 private 實例轉呼叫 wrapper 同樣列在這裡，不改比較器。

- 驗證對象：`task/70` staged 三個外層類別編譯產物（JAR SHA-256 `aa94ee447b870509035c1cb947d30085d91eb380321cbc0a0aef311ffed0b679`），對照 `reference/candidate-50635f7e.jar`。
- 比較器：`tools/semantic-diff`（ASM 9.7），傳入 T029、effects、extra-methods、exceptions、metadata 核准清單（指令見 `docs/reports/issue-70.md`）。
- 本清單 **174** 個 DIFF 方法 + **9** 個 ONLY_LEFT lambda。另有 1 個方法（`sourceApplicationRun`）不是形狀差異，列在最後一節待主控裁定。

## 收錄條件（每個方法都經機器檢查）

| 檢查 | 結果 |
|---|---|
| 直接呼叫（INVOKE*，不含 indy）多重集合 | 174 個全部與候選相同（無缺呼叫、無多呼叫） |
| 欄位讀寫（GET/PUT FIELD/STATIC）多重集合 | 174 個全部相同 |
| 逐事件後繼集合（見下） | 174 個全部相同（`LAYOUT_ONLY`） |

**逐事件後繼集合檢查**（scratch 工具 `scratch/issue-70/EventOrder.java`，診斷用、不進正式比較器）：把方法建成指令級 CFG（一般邊＋呼叫／indy／monitor／athrow 的例外邊），
「事件」= 方法呼叫、欄位讀寫、invokedynamic、MONITORENTER/EXIT；對每個事件求「下一步可能遇到的事件集合」（含 RETURN／ATHROW 終點），比較兩側 (事件, 後繼集合) 的多重集合。
相同代表兩側只差在區塊排列、堆疊／區域變數、分支方向與 handler 切法，**沒有**任何一條路徑上相鄰事件的先後被交換、也沒有事件跨出／跨入 synchronized 或 try 範圍。
這是局部（一步）檢查，不是完整的路徑等價證明，所以仍列為「待 M2 回歸驗收」。

前一輪（`retry5`）標為「順序不同（線性排列）」的 49 個方法，用這個檢查重新判讀後：真正有逐路徑順序差異的 25 個方法已在本輪改 Java（見 `docs/reports/issue-70.md`），其餘都只是線性排列不同，列入本清單。

## 差異類型統計

類型取自比較器第一個差異點（`detail`），一個方法可能同時有多種。

| 類型 | 方法數 |
|---|---|
| 例外表涵蓋、含迴圈（路徑比較器不支援） | 113 |
| 分支排列、含迴圈（路徑比較器不支援） | 33 |
| 堆疊／區域變數形狀、含迴圈（路徑比較器不支援） | 17 |
| 路徑事件多重集合不同（長度相同） | 5 |
| 堆疊／區域變數形狀、路徑事件多重集合不同（長度相同） | 2 |
| 含迴圈（路徑比較器不支援） | 1 |
| 堆疊／區域變數形狀 | 1 |
| 指令形狀 | 1 |
| 例外表涵蓋 | 1 |

說明：
- 「含迴圈」：正式比較器的路徑比較遇到一般控制流迴圈即放棄（`paths rejected: normal control-flow cycle`），只能退回 CFG 比較，因此這類方法即使只差 handler 切法也會留在 DIFF。
- 「例外表涵蓋」：多數是 VF 反編譯出的 try／synchronized 範圍邊界與候選 patch 後的範圍不同，事件後繼檢查已確認沒有呼叫跨出或跨入範圍。
- 「路徑事件多重集合不同」：比較器的路徑事件包含 indy handle 與常數；7 個方法中 5 個是 lambda 父方法（indy 指向 staged wrapper／候選直接指向 private 方法，見下一節），2 個（`nativeAreaFreezeAccepted*`）是候選把 `activateFreeze(...)` 的結果用分支具體化成 0／1，staged 用 `DUP` 同時存兩個區域變數，呼叫與欄位不變。

## 9 個 ONLY_LEFT lambda

候選的父方法 invokedynamic 直接指向同類別 private 方法；staged Java 只能寫成 lambda，因此多出一個「載入參數 → 呼叫同一個 private 方法 → return」的合成方法，比較器找不到配對而報 ONLY_LEFT。比較器的 `--extra-methods` 只接受 private static，這 8 個是 private 實例方法（依裁定不改比較器）。

| lambda | 父方法 | 候選 indy 目標 | 檢查 |
|---|---|---|---|
| `lambda$autoSelectPendingRentalBattleHandoff$30(Llocal/lito381/server/CharacterSummary;)Z` | `autoSelectPendingRentalBattleHandoff` | （候選同名 lambda） | 不是轉呼叫 wrapper；以同名直接比對方法本體結果為 **SAME**（呼叫順序 hub→accountName→name→stateCharacter→SourceLogin.account→hasRentalBattleHandoff 兩側一致），ONLY_LEFT 只因父方法 DIFF 使 lambda 無法依 call site 配對 |
| `lambda$completeClassicWorldPortal$162(Llocal/lito381/server/Protocol$PortalDestination;Llocal/lito381/server/ClientSession$PendingClassicWorldPortal;)V` | `completeClassicWorldPortal` | `nativePlacementPortalBody` | 轉呼叫 wrapper：參數依序載入、唯一一個同類別 private 呼叫（原始 opcode 182）、無 handler，目標與候選 indy 相同 |
| `lambda$nativeAreaBattleSlowBody$330(IILlocal/lito381/server/BattleSpellCatalog$MassSlowSpell;Ljava/lang/Object;Llocal/lito381/server/BattleQuestEncounter$Snapshot;)Z` | `nativeAreaBattleSlowBody` | `nativeSourceFrozenSlow_lambda$useMassSlowSpell$322` | 轉呼叫 wrapper：參數依序載入、唯一一個同類別 private 呼叫（原始 opcode 182）、無 handler，目標與候選 indy 相同 |
| `lambda$nativeAreaBattleSlowBody$331(IILlocal/lito381/server/BattleSpellCatalog$MassSlowSpell;Ljava/lang/Object;Llocal/lito381/server/BattleTimeAttackEncounter$Snapshot;)Z` | `nativeAreaBattleSlowBody` | `nativeSourceFrozenSlow_lambda$useMassSlowSpell$323` | 轉呼叫 wrapper：參數依序載入、唯一一個同類別 private 呼叫（原始 opcode 182）、無 handler，目標與候選 indy 相同 |
| `lambda$nativeMaterialGroundBody$389(Llocal/lito381/server/RuntimeInventory;ILlocal/lito381/server/BattleSpellCatalog$GroundSpell;JI)V` | `nativeMaterialGroundBody` | `nativeMaterialGroundBodyCommit` | 轉呼叫 wrapper：參數依序載入、唯一一個同類別 private 呼叫（原始 opcode 182）、無 handler，目標與候選 indy 相同 |
| `lambda$nativeMaterialWorldGroundBody$390(Llocal/lito381/server/RuntimeInventory;ILlocal/lito381/server/BattleSpellCatalog$GroundSpell;JI)V` | `nativeMaterialWorldGroundBody` | `nativeMaterialWorldGroundBodyCommit` | 轉呼叫 wrapper：參數依序載入、唯一一個同類別 private 呼叫（原始 opcode 182）、無 handler，目標與候選 indy 相同 |
| `lambda$useClassicWorldAreaAttackSpell$240(Llocal/lito381/server/Protocol$AttackSpellRequest;Llocal/lito381/server/BattleSpellCatalog$AreaAttackSpell;)V` | `useClassicWorldAreaAttackSpell` | `nativeCompanionTargetuseClassicWorldAreaAttackSpell` | 轉呼叫 wrapper：參數依序載入、唯一一個同類別 private 呼叫（原始 opcode 182）、無 handler，目標與候選 indy 相同 |
| `lambda$useClassicWorldAttackSpell$241(Llocal/lito381/server/Protocol$AttackSpellRequest;Llocal/lito381/server/BattleSpellCatalog$AttackSpell;)V` | `useClassicWorldAttackSpell` | `nativeCompanionTargetuseClassicWorldAttackSpell` | 轉呼叫 wrapper：參數依序載入、唯一一個同類別 private 呼叫（原始 opcode 182）、無 handler，目標與候選 indy 相同 |
| `lambda$useClassicWorldLineAttackSpell$238([BLlocal/lito381/server/BattleSpellCatalog$LineAttackSpell;)V` | `useClassicWorldLineAttackSpell` | `nativeCompanionTargetuseClassicWorldLineAttackSpell` | 轉呼叫 wrapper：參數依序載入、唯一一個同類別 private 呼叫（原始 opcode 182）、無 handler，目標與候選 indy 相同 |

## 逐方法清單

每項列出：比較器第一差異點、類型、代表 hunk（正規化指令，`-` 為 staged、`+` 為候選；只列第一個 hunk）。

### `applyBattleCompanionFeedbackDamage(Llocal/lito381/server/BattleCompanion;ILjava/util/List;Ljava/lang/String;)Llocal/lito381/server/BattleCompanion$Damage;`

- 類別：`local/lito381/server/ClientSession`
- 類型：堆疊／區域變數形狀、含迴圈（路徑比較器不支援）；事件後繼：`LAYOUT_ONLY`
- 比較器：`method applyBattleCompanionFeedbackDamage(Llocal/lito381/server/BattleCompanion;ILjava/util/List;Ljava/lang/String;)Llocal/lito381/server/BattleCompanion$Damage; insn[6]: left=ALOAD 0 right=ALOAD 1; paths rejected: normal control-flow cycle; labelled CFGs differ`

```diff
@@ -6,14 +6,16 @@
 IFGT L6
 CONST null
 ARETURN
-ALOAD 0
 ALOAD 1
 ILOAD 2
+ALOAD 0
+DUP_X2
+POP
 INVOKESTATIC local/lito381/server/NativeSummonTerminalLifecycle.combatDamage(Llocal/lito381/server/ClientSession;Llocal/lito381/server/BattleCompanion;I)Llocal/lito381/server/BattleCompanion$Damage;
 ASTORE 5
 ALOAD 5
 INVOKESTATIC local/lito381/server/NativeSummonTerminalLifecycle.legacyApplied(Llocal/lito381/server/BattleCompanion$Damage;)I
-IFGT L16
+IFGT L18
 ALOAD 5
 ARETURN
 ALOAD 5
```

### `applyClassicWorldMonsterCompanionDamageInternal(Llocal/lito381/server/BattleCompanion;Llocal/lito381/server/BattleCompanion$Snapshot;IJLjava/lang/String;Z)Llocal/lito381/server/BattleCompanion$Damage;`

- 類別：`local/lito381/server/ClientSession`
- 類型：堆疊／區域變數形狀、含迴圈（路徑比較器不支援）；事件後繼：`LAYOUT_ONLY`
- 比較器：`method applyClassicWorldMonsterCompanionDamageInternal(Llocal/lito381/server/BattleCompanion;Llocal/lito381/server/BattleCompanion$Snapshot;IJLjava/lang/String;Z)Llocal/lito381/server/BattleCompanion$Damage; insn[40]: left=ALOAD 0 right=ALOAD 1; paths rejected: normal control-flow cycle; labelled CFGs differ`

```diff
@@ -40,14 +40,16 @@
 ALOAD 1
 INVOKESTATIC local/lito381/server/NativeCompanionTargets.finished(Llocal/lito381/server/BattleCompanion$Damage;Llocal/lito381/server/ClientSession;Llocal/lito381/server/BattleCompanion;)Llocal/lito381/server/BattleCompanion$Damage;
 ARETURN
-ALOAD 0
 ALOAD 1
 ILOAD 3
+ALOAD 0
+DUP_X2
+POP
 INVOKESTATIC local/lito381/server/NativeCompanionTargets.damage(Llocal/lito381/server/ClientSession;Llocal/lito381/server/BattleCompanion;I)Llocal/lito381/server/BattleCompanion$Damage;
 ASTORE 8
 ALOAD 8
 INVOKESTATIC local/lito381/server/NativeSummonTerminalLifecycle.legacyApplied(Llocal/lito381/server/BattleCompanion$Damage;)I
-IFGT L53
+IFGT L55
 ALOAD 8
 ALOAD 0
 ALOAD 1
```

### `attackClassicWorld(Llocal/lito381/server/Protocol$AttackRequest;Llocal/lito381/server/ClientSession$AttackEnvelope;)V`

- 類別：`local/lito381/server/ClientSession`
- 類型：例外表涵蓋、含迴圈（路徑比較器不支援）；事件後繼：`LAYOUT_ONLY`
- 比較器：`method attackClassicWorld(Llocal/lito381/server/Protocol$AttackRequest;Llocal/lito381/server/ClientSession$AttackEnvelope;)V: left=1596:[1600:java/lang/Exception  ] right=1591:[1595:java/lang/Exception  ]; paths rejected: normal control-flow cycle; labelled CFGs differ`

```diff
@@ -1,5 +1,5 @@
 META 2 null [java/io/IOException]
-TRY [1596:[1600:java/lang/Exception  ], 1597:[1600:java/lang/Exception  ]]
+TRY [1591:[1595:java/lang/Exception  ], 1592:[1595:java/lang/Exception  ]]
 CONST null
 ASTORE 3
 CONST null
```

### `attackClassicWorldPlayer(Llocal/lito381/server/Protocol$AttackRequest;Llocal/lito381/server/ClientSession$AttackEnvelope;Llocal/lito381/server/ClientSession;)V`

- 類別：`local/lito381/server/ClientSession`
- 類型：堆疊／區域變數形狀、含迴圈（路徑比較器不支援）；事件後繼：`LAYOUT_ONLY`
- 比較器：`method attackClassicWorldPlayer(Llocal/lito381/server/Protocol$AttackRequest;Llocal/lito381/server/ClientSession$AttackEnvelope;Llocal/lito381/server/ClientSession;)V insn[102]: left=DUP right=ISTORE 15; paths rejected: normal control-flow cycle; labelled CFGs differ`

```diff
@@ -102,48 +102,44 @@
 CONST Integer:1
 GOTO L102
 CONST Integer:0
-DUP
 ISTORE 15
-ISTORE 16
 ILOAD 9
-IFEQ L122
+IFEQ L120
 ALOAD 14
 INVOKEVIRTUAL local/lito381/server/SourceWeaponPhysicalStats.present()Z
-IFEQ L122
+IFEQ L120
 ALOAD 0
 GETFIELD local/lito381/server/ClientSession.state:Llocal/lito381/server/GameStateStore;
 ALOAD 0
 INVOKESPECIAL local/lito381/server/ClientSession.stateCharacter()Ljava/lang/String;
 ALOAD 13
 INVOKEVIRTUAL local/lito381/server/GameStateStore.sourceWeaponDurabilityBalance(Ljava/lang/String;Llocal/lito381/server/SourceWeaponPhysicalStats;)Llocal/lito381/server/GameStateStore$SourceWeaponDurabilityBalance;
-ASTORE 17
-ALOAD 14
-ALOAD 17
+ASTORE 16
…（其餘 hunk 見 scratch/issue-70/r6-b-classified.json）
```

### `autoSelectPendingRentalBattleHandoff()Z`

- 類別：`local/lito381/server/ClientSession`
- 類型：路徑事件多重集合不同（長度相同）；事件後繼：`LAYOUT_ONLY`
- 比較器：`method autoSelectPendingRentalBattleHandoff()Z insn[21]: left=INVOKEDYNAMIC test(Llocal/lito381/server/ClientSession;)Ljava/util/function/Predicate; bsm=H6 java/lang/invoke/LambdaMetafactory.metafactory(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;Ljava/lang/in… right=INVOKEDYNAMIC test(Llocal/lito381/server/ClientSession;)Ljava/util/function/Predicate; bsm`

```diff
@@ -21,7 +21,7 @@
 GETFIELD local/lito381/server/ClientSession.characters:Ljava/util/List;
 INVOKEINTERFACE java/util/List.stream()Ljava/util/stream/Stream; itf
 ALOAD 0
-INVOKEDYNAMIC test(Llocal/lito381/server/ClientSession;)Ljava/util/function/Predicate; bsm=H6 java/lang/invoke/LambdaMetafactory.metafactory(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodHandle;Ljava/lang/invoke/MethodType;)Ljava/lang/invoke/CallSite; args=[type:(Ljava/lang/Object;)Z, handle:H7 local/lito381/server/ClientSession.lambda$content$a97e73fc7e034d36aebb82c2bdc5a3deb5c86cc3c09e0500b58cb94bee5c29f3(Llocal/lito381/server/CharacterSummary;)Z, type:(Llocal/lito381/server/CharacterSummary;)Z]
+INVOKEDYNAMIC test(Llocal/lito381/server/ClientSession;)Ljava/util/function/Predicate; bsm=H6 java/lang/invoke/LambdaMetafactory.metafactory(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodHandle;Ljava/lang/invoke/MethodType;)Ljava/lang/invoke/CallSite; args=[type:(Ljava/lang/Object;)Z, handle:H7 local/lito381/server/ClientSession.lambda$content$c8a6b2a1bc8c6e2129b4912face422b657e26aab6e07711f200c1d965375ca75(Llocal/lito381/server/CharacterSummary;)Z, type:(Llocal/lito381/server/CharacterSummary;)Z]
 INVOKEINTERFACE java/util/stream/Stream.filter(Ljava/util/function/Predicate;)Ljava/util/stream/Stream; itf
 INVOKEINTERFACE java/util/stream/Stream.findFirst()Ljava/util/Optional; itf
 CONST null
```

### `completeClassicWorldPortal(Llocal/lito381/server/Protocol$PortalDestination;Llocal/lito381/server/ClientSession$PendingClassicWorldPortal;)V`

- 類別：`local/lito381/server/ClientSession`
- 類型：路徑事件多重集合不同（長度相同）；事件後繼：`LAYOUT_ONLY`
- 比較器：`method completeClassicWorldPortal(Llocal/lito381/server/Protocol$PortalDestination;Llocal/lito381/server/ClientSession$PendingClassicWorldPortal;)V insn[6]: left=INVOKEDYNAMIC run(Llocal/lito381/server/ClientSession;Llocal/lito381/server/Protocol$PortalDestination;Llocal/lito381/server/ClientSession$PendingClassicWorldPortal;)Llocal/lito381/server/NativeSelfCastAction$Task; bsm=H6 java/lang/invoke`

```diff
@@ -6,6 +6,6 @@
 ALOAD 0
 ALOAD 1
 ALOAD 2
-INVOKEDYNAMIC run(Llocal/lito381/server/ClientSession;Llocal/lito381/server/Protocol$PortalDestination;Llocal/lito381/server/ClientSession$PendingClassicWorldPortal;)Llocal/lito381/server/NativeSelfCastAction$Task; bsm=H6 java/lang/invoke/LambdaMetafactory.metafactory(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodHandle;Ljava/lang/invoke/MethodType;)Ljava/lang/invoke/CallSite; args=[type:()V, handle:H7 local/lito381/server/ClientSession.lambda$content$59c2809294fdc4ecf9aee82b27e47107a33f73571660064cfdc99b4b5c301fd2(Llocal/lito381/server/Protocol$PortalDestination;Llocal/lito381/server/ClientSession$PendingClassicWorldPortal;)V, type:()V]
+INVOKEDYNAMIC run(Llocal/lito381/server/ClientSession;Llocal/lito381/server/Protocol$PortalDestination;Llocal/lito381/server/ClientSession$PendingClassicWorldPortal;)Llocal/lito381/server/NativeSelfCastAction$Task; bsm=H6 java/lang/invoke/LambdaMetafactory.metafactory(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodHandle;Ljava/lang/invoke/MethodType;)Ljava/lang/invoke/CallSite; args=[type:()V, handle:H7 local/lito381/server/ClientSession.nativePlacementPortalBody(Llocal/lito381/server/Protocol$PortalDestination;Llocal/lito381/server/ClientSession$PendingClassicWorldPortal;)V, type:()V]
 INVOKESTATIC local/lito381/server/NativeCompanionPlacement.portal(Llocal/lito381/server/ClientSession;Llocal/lito381/server/Protocol$PortalDestination;Ljava/lang/Object;Llocal/lito381/server/NativeSelfCastAction$Task;)V
 RETURN
```

### `nativeAreaBattleSleepBody([BLlocal/lito381/server/BattleSpellCatalog$SleepSpell;)V`

- 類別：`local/lito381/server/ClientSession`
- 類型：例外表涵蓋、含迴圈（路徑比較器不支援）；事件後繼：`LAYOUT_ONLY`
- 比較器：`method nativeAreaBattleSleepBody([BLlocal/lito381/server/BattleSpellCatalog$SleepSpell;)V: left=293:[299:java/lang/Exception  ] right=330:[336:java/lang/Exception  ]; paths rejected: normal control-flow cycle; labelled CFGs differ`

```diff
@@ -1,5 +1,5 @@
 META 0 null [java/io/IOException]
-TRY [293:[299:java/lang/Exception  ], 295:[299:java/lang/Exception  ], 296:[299:java/lang/Exception  ], 346:[352:java/lang/Exception  ], 348:[352:java/lang/Exception  ], 349:[352:java/lang/Exception  ], 432:[438:java/lang/Exception  ], 434:[438:java/lang/Exception  ], 435:[438:java/lang/Exception  ]]
+TRY [330:[336:java/lang/Exception  ], 332:[336:java/lang/Exception  ], 333:[336:java/lang/Exception  ], 388:[394:java/lang/Exception  ], 390:[394:java/lang/Exception  ], 391:[394:java/lang/Exception  ], 442:[448:java/lang/Exception  ], 444:[448:java/lang/Exception  ], 445:[448:java/lang/Exception  ]]
 ALOAD 0
 ALOAD 2
 INVOKEVIRTUAL local/lito381/server/BattleSpellCatalog$SleepSpell.skillId()I
```

### `nativeAreaBattleSlowBody([BLlocal/lito381/server/BattleSpellCatalog$MassSlowSpell;)V`

- 類別：`local/lito381/server/ClientSession`
- 類型：分支排列、含迴圈（路徑比較器不支援）；事件後繼：`LAYOUT_ONLY`
- 比較器：`method nativeAreaBattleSlowBody([BLlocal/lito381/server/BattleSpellCatalog$MassSlowSpell;)V insn[10]: left=IFEQ L1159 right=IFNE L12; paths rejected: normal control-flow cycle; labelled CFGs differ`

```diff
@@ -10,12 +10,14 @@
 ALOAD 0
 ALOAD 3
 INVOKESTATIC local/lito381/server/NativeSelectedPlayerIntent.attack(Llocal/lito381/server/ClientSession;Llocal/lito381/server/Protocol$AttackSpellRequest;)Z
-IFEQ L1159
+IFNE L12
+RETURN
 ALOAD 3
 INVOKEVIRTUAL local/lito381/server/Protocol$AttackSpellRequest.skillId()I
 ALOAD 2
 INVOKEVIRTUAL local/lito381/server/BattleSpellCatalog$MassSlowSpell.skillId()I
-IF_ICMPNE L1159
+IF_ICMPEQ L18
+RETURN
 ALOAD 0
 GETFIELD local/lito381/server/ClientSession.hub:Llocal/lito381/server/GameHub;
 ALOAD 0
```

### `nativeAreaFreezeAcceptedBattle(Llocal/lito381/server/BattleSpellCatalog$FreezeAttackSpell;Llocal/lito381/server/ClientSession;Ljava/util/List;)V`

- 類別：`local/lito381/server/ClientSession`
- 類型：堆疊／區域變數形狀、路徑事件多重集合不同（長度相同）；事件後繼：`LAYOUT_ONLY`
- 比較器：`method nativeAreaFreezeAcceptedBattle(Llocal/lito381/server/BattleSpellCatalog$FreezeAttackSpell;Llocal/lito381/server/ClientSession;Ljava/util/List;)V insn[8]: left=DUP right=IFEQ L11; path event multisets differ (left=18, right=32); labelled CFGs differ`

```diff
@@ -8,11 +8,13 @@
 ALOAD 1
 INVOKESTATIC local/lito381/server/NativeAcceptedFreezeMutation.duration(Llocal/lito381/server/BattleSpellCatalog$FreezeAttackSpell;)I
 INVOKEVIRTUAL local/lito381/server/BattleTimedStatus.activateFreeze(JI)Z
-DUP
+IFEQ L11
+CONST Integer:1
+GOTO L12
+CONST Integer:0
 ISTORE 6
-ISTORE 7
 ILOAD 6
-IFEQ L41
+IFEQ L43
 ALOAD 0
 ALOAD 2
 INVOKEVIRTUAL local/lito381/server/ClientSession.playerObjectId()J
```

### `nativeAreaFreezeAcceptedWorld(Llocal/lito381/server/BattleSpellCatalog$FreezeAttackSpell;Llocal/lito381/server/ClientSession;Ljava/util/List;)V`

- 類別：`local/lito381/server/ClientSession`
- 類型：堆疊／區域變數形狀、路徑事件多重集合不同（長度相同）；事件後繼：`LAYOUT_ONLY`
- 比較器：`method nativeAreaFreezeAcceptedWorld(Llocal/lito381/server/BattleSpellCatalog$FreezeAttackSpell;Llocal/lito381/server/ClientSession;Ljava/util/List;)V insn[8]: left=DUP right=IFEQ L11; path event multisets differ (left=18, right=32); labelled CFGs differ`

```diff
@@ -8,11 +8,13 @@
 ALOAD 1
 INVOKESTATIC local/lito381/server/NativeAcceptedFreezeMutation.duration(Llocal/lito381/server/BattleSpellCatalog$FreezeAttackSpell;)I
 INVOKEVIRTUAL local/lito381/server/BattleTimedStatus.activateFreeze(JI)Z
-DUP
+IFEQ L11
+CONST Integer:1
+GOTO L12
+CONST Integer:0
 ISTORE 6
-ISTORE 7
 ILOAD 6
-IFEQ L40
+IFEQ L42
 ALOAD 0
 ALOAD 2
 INVOKEVIRTUAL local/lito381/server/ClientSession.playerObjectId()J
```

### `nativeAreaFreezeOriginalWorld([BLlocal/lito381/server/BattleSpellCatalog$FreezeAttackSpell;)V`

- 類別：`local/lito381/server/ClientSession`
- 類型：例外表涵蓋、含迴圈（路徑比較器不支援）；事件後繼：`LAYOUT_ONLY`
- 比較器：`method nativeAreaFreezeOriginalWorld([BLlocal/lito381/server/BattleSpellCatalog$FreezeAttackSpell;)V: left=78:[302:java/lang/Exception  ] right=80:[308:java/lang/Exception  ]; paths rejected: normal control-flow cycle; labelled CFGs differ`

```diff
@@ -1,5 +1,5 @@
 META 0 null [java/io/IOException]
-TRY [78:[302:java/lang/Exception  ], 81:[302:java/lang/Exception  ], 84:[302:java/lang/Exception  ], 87:[302:java/lang/Exception  ], 88:[302:java/lang/Exception  ], 94:[302:java/lang/Exception  ], 95:[302:java/lang/Exception  ], 98:[302:java/lang/Exception  ], 102:[302:java/lang/Exception  ], 106:[302:java/lang/Exception  ], 107:[302:java/lang/Exception  ], 110:[302:java/lang/Exception  ], 113:[302:java/lang/Exception  ], 114:[302:java/lang/Exception  ], 117:[302:java/lang/Exception  ], 118:[302:java/lang/Exception  ], 121:[302:java/lang/Exception  ], 124:[302:java/lang/Exception  ], 128:[302:java/lang/Exception  ], 130:[302:java/lang/Exception  ], 131:[302:java/lang/Exception  ], 134:[302:java/lang/Exception  ], 137:[302:java/lang/Exception  ], 139:[302:java/lang/Exception  ], 142:[302:java/lang/Exception  ], 143:[302:java/lang/Exception  ], 149:[302:java/lang/Exception  ], 151:[302:java/lang/Exception  ], 152:[302:java/lang/Exception  ], 158:[302:java/lang/Exception  ], 160:[302:java/lang/Exception  ], 164:[302:java/lang/Exception  ], 168:[302:java/lang/Exception  ], 169:[302:java/lang/Exception  ], 175:[302:java/lang/Exception  ], 178:[302:java/lang/Exception  ], 180:[302:java/lang/Exception  ], 181:[302:java/lang/Exception  ], 185:[302:java/lang/Exception  ], 187:[302:java/lang/Exception  ], 189:[302:java/lang/Exception  ], 190:[302:java/lang/Exception  ], 191:[302:java/lang/Exception  ], 194:[302:java/lang/Exception  ], 200:[302:java/lang/Exception  ], 201:[302:java/lang/Exception  ], 206:[302:java/lang/Exception  ], 211:[302:java/lang/Exception  ], 212:[302:java/lang/Exception  ], 215:[302:java/lang/Exception  ], 218:[302:java/lang/Exception  ], 219:[302:java/lang/Exception  ], 222:[302:java/lang/Exception  ], 225:[302:java/lang/Exception  ], 228:[302:java/lang/Exception  ], 232:[302:java/lang/Exception  ], 236:[302:java/lang/Exception  ], 238:[302:java/lang/Exception  ], 241:[302:java/lang/Exception  ], 244:[302:java/lang/Exception  ], 246:[302:java/lang/Exception  ], 249:[302:java/lang/Exception  ], 250:[302:java/lang/Exception  ], 256:[302:java/lang/Exception  ], 258:[302:java/lang/Exception  ], 259:[302:java/lang/Exception  ], 265:[302:java/lang/Exception  ], 267:[302:java/lang/Exception  ], 271:[302:java/lang/Exception  ], 274:[302:java/lang/Exception  ], 276:[302:java/lang/Exception  ], 277:[302:java/lang/Exception  ], 281:[302:java/lang/Exception  ], 283:[302:java/lang/Exception  ], 285:[302:java/lang/Exception  ], 286:[302:java/lang/Exception  ], 287:[302:java/lang/Exception  ], 291:[302:java/lang/Exception  ], 292:[302:java/lang/Exception  ], 294:[302:java/lang/Exception  ], 298:[302:java/lang/Exception  ], 382:[488:java/lang/Throwable  ], 385:[488:java/lang/Throwable  ], 387:[488:java/lang/Throwable  ], 388:[488:java/lang/Throwable  ], 390:[488:java/lang/Throwable  ], 391:[488:java/lang/Throwable  ], 392:[488:java/lang/Throwable  ], 393:[488:java/lang/Throwable  ], 400:[488:java/lang/Throwable  ], 401:[488:java/lang/Throwable  ], 404:[488:java/lang/Throwable  ], 407:[488:java/lang/Throwable  ], 410:[488:java/lang/Throwable  ], 414:[488:java/lang/Throwable  ], 418:[488:java/lang/Throwable  ], 422:[488:java/lang/Throwable  ], 426:[488:java/lang/Throwable  ], 430:[488:java/lang/Throwable  ], 436:[488:java/lang/Throwable  ], 437:[488:java/lang/Throwable  ], 439:[488:java/lang/Throwable  ], 440:[488:java/lang/Throwable  ], 441:[488:java/lang/Throwable  ], 442:[488:java/lang/Throwable  ], 444:[488:java/lang/Throwable  ], 446:[488:java/lang/Throwable  ], 452:[488:java/lang/Throwable  ], 453:[488:java/lang/Throwable  ], 454:[488:java/lang/Throwable  ], 456:[488:java/lang/Throwable  ], 460:[488:java/lang/Throwable  ], 461:[488:java/lang/Throwable  ], 462:[488:java/lang/Throwable  ], 466:[488:java/lang/Throwable  ], 469:[488:java/lang/Throwable  ], 470:[488:java/lang/Throwable  ], 472:[488:java/lang/Throwable  ], 476:[488:java/lang/Throwable  ], 479:[488:java/lang/Throwable  ], 484:[488:java/lang/Throwable  ], 486:[488:java/lang/Throwable  ], 490:[488:java/lang/Throwable  ]]
+TRY [80:[308:java/lang/Exception  ], 83:[308:java/lang/Exception  ], 86:[308:java/lang/Exception  ], 89:[308:java/lang/Exception  ], 90:[308:java/lang/Exception  ], 96:[308:java/lang/Exception  ], 97:[308:java/lang/Exception  ], 100:[308:java/lang/Exception  ], 104:[308:java/lang/Exception  ], 109:[308:java/lang/Exception  ], 110:[308:java/lang/Exception  ], 113:[308:java/lang/Exception  ], 116:[308:java/lang/Exception  ], 117:[308:java/lang/Exception  ], 120:[308:java/lang/Exception  ], 121:[308:java/lang/Exception  ], 124:[308:java/lang/Exception  ], 127:[308:java/lang/Exception  ], 131:[308:java/lang/Exception  ], 133:[308:java/lang/Exception  ], 134:[308:java/lang/Exception  ], 137:[308:java/lang/Exception  ], 140:[308:java/lang/Exception  ], 142:[308:java/lang/Exception  ], 145:[308:java/lang/Exception  ], 146:[308:java/lang/Exception  ], 152:[308:java/lang/Exception  ], 154:[308:java/lang/Exception  ], 155:[308:java/lang/Exception  ], 161:[308:java/lang/Exception  ], 163:[308:java/lang/Exception  ], 167:[308:java/lang/Exception  ], 172:[308:java/lang/Exception  ], 173:[308:java/lang/Exception  ], 179:[308:java/lang/Exception  ], 183:[308:java/lang/Exception  ], 185:[308:java/lang/Exception  ], 186:[308:java/lang/Exception  ], 190:[308:java/lang/Exception  ], 192:[308:java/lang/Exception  ], 194:[308:java/lang/Exception  ], 195:[308:java/lang/Exception  ], 196:[308:java/lang/Exception  ], 199:[308:java/lang/Exception  ], 205:[308:java/lang/Exception  ], 206:[308:java/lang/Exception  ], 211:[308:java/lang/Exception  ], 216:[308:java/lang/Exception  ], 217:[308:java/lang/Exception  ], 220:[308:java/lang/Exception  ], 223:[308:java/lang/Exception  ], 224:[308:java/lang/Exception  ], 227:[308:java/lang/Exception  ], 230:[308:java/lang/Exception  ], 233:[308:java/lang/Exception  ], 237:[308:java/lang/Exception  ], 241:[308:java/lang/Exception  ], 243:[308:java/lang/Exception  ], 246:[308:java/lang/Exception  ], 249:[308:java/lang/Exception  ], 251:[308:java/lang/Exception  ], 254:[308:java/lang/Exception  ], 255:[308:java/lang/Exception  ], 261:[308:java/lang/Exception  ], 263:[308:java/lang/Exception  ], 264:[308:java/lang/Exception  ], 270:[308:java/lang/Exception  ], 272:[308:java/lang/Exception  ], 276:[308:java/lang/Exception  ], 280:[308:java/lang/Exception  ], 282:[308:java/lang/Exception  ], 283:[308:java/lang/Exception  ], 287:[308:java/lang/Exception  ], 289:[308:java/lang/Exception  ], 291:[308:java/lang/Exception  ], 292:[308:java/lang/Exception  ], 293:[308:java/lang/Exception  ], 297:[308:java/lang/Exception  ], 298:[308:java/lang/Exception  ], 300:[308:java/lang/Exception  ], 304:[308:java/lang/Exception  ], 388:[496:java/lang/Throwable  ], 391:[496:java/lang/Throwable  ], 393:[496:java/lang/Throwable  ], 394:[496:java/lang/Throwable  ], 396:[496:java/lang/Throwable  ], 397:[496:java/lang/Throwable  ], 398:[496:java/lang/Throwable  ], 399:[496:java/lang/Throwable  ], 406:[496:java/lang/Throwable  ], 407:[496:java/lang/Throwable  ], 410:[496:java/lang/Throwable  ], 413:[496:java/lang/Throwable  ], 416:[496:java/lang/Throwable  ], 420:[496:java/lang/Throwable  ], 424:[496:java/lang/Throwable  ], 428:[496:java/lang/Throwable  ], 432:[496:java/lang/Throwable  ], 436:[496:java/lang/Throwable  ], 442:[496:java/lang/Throwable  ], 443:[496:java/lang/Throwable  ], 445:[496:java/lang/Throwable  ], 446:[496:java/lang/Throwable  ], 447:[496:java/lang/Throwable  ], 448:[496:java/lang/Throwable  ], 450:[496:java/lang/Throwable  ], 452:[496:java/lang/Throwable  ], 458:[496:java/lang/Throwable  ], 459:[496:java/lang/Throwable  ], 460:[496:java/lang/Throwable  ], 462:[496:java/lang/Throwable  ], 466:[496:java/lang/Throwable  ], 467:[496:java/lang/Throwable  ], 468:[496:java/lang/Throwable  ], 471:[496:java/lang/Throwable  ], 475:[496:java/lang/Throwable  ], 478:[496:java/lang/Throwable  ], 480:[496:java/lang/Throwable  ], 484:[496:java/lang/Throwable  ], 487:[496:java/lang/Throwable  ], 492:[496:java/lang/Throwable  ], 494:[496:java/lang/Throwable  ], 498:[496:java/lang/Throwable  ]]
 INVOKESTATIC local/lito381/server/CombatWorldFreeze.create()Llocal/lito381/server/CombatWorldFreeze$Batch;
 ASTORE 3
 CONST null
```

### `nativeAreaWorldSleepBody([BLlocal/lito381/server/BattleSpellCatalog$SleepSpell;)V`

- 類別：`local/lito381/server/ClientSession`
- 類型：例外表涵蓋、含迴圈（路徑比較器不支援）；事件後繼：`LAYOUT_ONLY`
- 比較器：`method nativeAreaWorldSleepBody([BLlocal/lito381/server/BattleSpellCatalog$SleepSpell;)V: left=1154:[1160:java/lang/Exception  ] right=322:[328:java/lang/Exception  ]; paths rejected: normal control-flow cycle; labelled CFGs differ`

```diff
@@ -1,5 +1,5 @@
 META 0 null [java/io/IOException]
-TRY [1154:[1160:java/lang/Exception  ], 1156:[1160:java/lang/Exception  ], 1157:[1160:java/lang/Exception  ], 1225:[1231:java/lang/Exception  ], 1227:[1231:java/lang/Exception  ], 1228:[1231:java/lang/Exception  ]]
+TRY [322:[328:java/lang/Exception  ], 324:[328:java/lang/Exception  ], 325:[328:java/lang/Exception  ], 370:[376:java/lang/Exception  ], 372:[376:java/lang/Exception  ], 373:[376:java/lang/Exception  ]]
 ALOAD 1
 INVOKESTATIC local/lito381/server/Protocol.attackSpellRequest([B)Llocal/lito381/server/Protocol$AttackSpellRequest;
 ALOAD 0
```

### `nativeAreaWorldSlowBody([BLlocal/lito381/server/BattleSpellCatalog$MassSlowSpell;)V`

- 類別：`local/lito381/server/ClientSession`
- 類型：例外表涵蓋、含迴圈（路徑比較器不支援）；事件後繼：`LAYOUT_ONLY`
- 比較器：`method nativeAreaWorldSlowBody([BLlocal/lito381/server/BattleSpellCatalog$MassSlowSpell;)V: left=93:[99:java/lang/Exception  ] right=96:[102:java/lang/Exception  ]; paths rejected: normal control-flow cycle; labelled CFGs differ`

```diff
@@ -1,5 +1,5 @@
 META 0 null [java/io/IOException]
-TRY [93:[99:java/lang/Exception  ], 95:[99:java/lang/Exception  ], 96:[99:java/lang/Exception  ], 327:[419:java/lang/Exception  ], 328:[419:java/lang/Exception  ], 331:[419:java/lang/Exception  ], 334:[419:java/lang/Exception  ], 335:[419:java/lang/Exception  ], 339:[419:java/lang/Exception  ], 342:[419:java/lang/Exception  ], 343:[419:java/lang/Exception  ], 347:[419:java/lang/Exception  ], 350:[419:java/lang/Exception  ], 351:[419:java/lang/Exception  ], 354:[419:java/lang/Exception  ], 357:[419:java/lang/Exception  ], 358:[419:java/lang/Exception  ], 361:[419:java/lang/Exception  ], 362:[419:java/lang/Exception  ], 365:[419:java/lang/Exception  ], 368:[419:java/lang/Exception  ], 372:[419:java/lang/Exception  ], 374:[419:java/lang/Exception  ], 375:[419:java/lang/Exception  ], 378:[419:java/lang/Exception  ], 382:[419:java/lang/Exception  ], 385:[419:java/lang/Exception  ], 390:[419:java/lang/Exception  ], 391:[419:java/lang/Exception  ], 394:[419:java/lang/Exception  ], 396:[419:java/lang/Exception  ], 397:[419:java/lang/Exception  ], 404:[419:java/lang/Exception  ], 407:[419:java/lang/Exception  ], 413:[419:java/lang/Exception  ], 414:[419:java/lang/Exception  ], 437:[500:java/lang/Exception  ], 442:[500:java/lang/Exception  ], 443:[500:java/lang/Exception  ], 446:[500:java/lang/Exception  ], 449:[500:java/lang/Exception  ], 450:[500:java/lang/Exception  ], 453:[500:java/lang/Exception  ], 456:[500:java/lang/Exception  ], 460:[500:java/lang/Exception  ], 462:[500:java/lang/Exception  ], 463:[500:java/lang/Exception  ], 466:[500:java/lang/Exception  ], 470:[500:java/lang/Exception  ], 473:[500:java/lang/Exception  ], 477:[500:java/lang/Exception  ], 479:[500:java/lang/Exception  ], 480:[500:java/lang/Exception  ], 485:[500:java/lang/Exception  ], 489:[500:java/lang/Exception  ], 490:[500:java/lang/Exception  ], 492:[500:java/lang/Exception  ], 496:[500:java/lang/Exception  ]]
+TRY [96:[102:java/lang/Exception  ], 98:[102:java/lang/Exception  ], 99:[102:java/lang/Exception  ], 330:[424:java/lang/Exception  ], 331:[424:java/lang/Exception  ], 334:[424:java/lang/Exception  ], 337:[424:java/lang/Exception  ], 338:[424:java/lang/Exception  ], 342:[424:java/lang/Exception  ], 345:[424:java/lang/Exception  ], 346:[424:java/lang/Exception  ], 350:[424:java/lang/Exception  ], 354:[424:java/lang/Exception  ], 355:[424:java/lang/Exception  ], 358:[424:java/lang/Exception  ], 361:[424:java/lang/Exception  ], 362:[424:java/lang/Exception  ], 365:[424:java/lang/Exception  ], 366:[424:java/lang/Exception  ], 369:[424:java/lang/Exception  ], 372:[424:java/lang/Exception  ], 376:[424:java/lang/Exception  ], 378:[424:java/lang/Exception  ], 379:[424:java/lang/Exception  ], 382:[424:java/lang/Exception  ], 386:[424:java/lang/Exception  ], 389:[424:java/lang/Exception  ], 395:[424:java/lang/Exception  ], 396:[424:java/lang/Exception  ], 399:[424:java/lang/Exception  ], 401:[424:java/lang/Exception  ], 402:[424:java/lang/Exception  ], 409:[424:java/lang/Exception  ], 412:[424:java/lang/Exception  ], 418:[424:java/lang/Exception  ], 419:[424:java/lang/Exception  ], 442:[506:java/lang/Exception  ], 447:[506:java/lang/Exception  ], 448:[506:java/lang/Exception  ], 451:[506:java/lang/Exception  ], 454:[506:java/lang/Exception  ], 455:[506:java/lang/Exception  ], 458:[506:java/lang/Exception  ], 461:[506:java/lang/Exception  ], 465:[506:java/lang/Exception  ], 467:[506:java/lang/Exception  ], 468:[506:java/lang/Exception  ], 471:[506:java/lang/Exception  ], 475:[506:java/lang/Exception  ], 478:[506:java/lang/Exception  ], 483:[506:java/lang/Exception  ], 485:[506:java/lang/Exception  ], 486:[506:java/lang/Exception  ], 491:[506:java/lang/Exception  ], 495:[506:java/lang/Exception  ], 496:[506:java/lang/Exception  ], 498:[506:java/lang/Exception  ], 502:[506:java/lang/Exception  ]]
 ALOAD 1
 INVOKESTATIC local/lito381/server/Protocol.attackSpellRequest([B)Llocal/lito381/server/Protocol$AttackSpellRequest;
 ALOAD 0
```

### `nativeBattleControlledCancellationBody(Llocal/lito381/server/Protocol$ObjectSpellRequest;Llocal/lito381/server/ClientSession;Llocal/lito381/server/BattleCompanion;Llocal/lito381/server/BattleCompanion$Snapshot;Llocal/lito381/server/ClassicWorldMonsterPopulation$Snapshot;Llocal/lito381/server/BattleSpellCatalog$CancellationSpell;Llocal/lito381/server/CombatPoisonLifecycle$Scope;)V`

- 類別：`local/lito381/server/ClientSession`
- 類型：分支排列、含迴圈（路徑比較器不支援）；事件後繼：`LAYOUT_ONLY`
- 比較器：`method nativeBattleControlledCancellationBody(Llocal/lito381/server/Protocol$ObjectSpellRequest;Llocal/lito381/server/ClientSession;Llocal/lito381/server/BattleCompanion;Llocal/lito381/server/BattleCompanion$Snapshot;Llocal/lito381/server/ClassicWorldMonsterPopulation$Snapshot;Llocal/lito381/server/BattleSpellCatalog$CancellationSpell;Llocal/lito381/server/CombatPoisonLifecycle$Scope;)V insn[3]: l`

```diff
@@ -3,80 +3,81 @@
 ALOAD 0
 ALOAD 1
 INVOKESTATIC local/lito381/server/NativeSelectedPlayerIntent.object(Llocal/lito381/server/ClientSession;Llocal/lito381/server/Protocol$ObjectSpellRequest;)Z
-IFEQ L793
-ALOAD 4
-IFNONNULL L8
-CONST Integer:0
-GOTO L9
-CONST Integer:1
-ALOAD 5
-IFNONNULL L13
-CONST Integer:0
-GOTO L14
+IFNE L5
+RETURN
+ALOAD 4
+IFNONNULL L9
+CONST Integer:0
+GOTO L10
+CONST Integer:1
+ALOAD 5
+IFNONNULL L14
+CONST Integer:0
…（其餘 hunk 見 scratch/issue-70/r6-b-classified.json）
```

### `nativeCompanionTargetuseClassicWorldAreaAttackSpell(Llocal/lito381/server/Protocol$AttackSpellRequest;Llocal/lito381/server/BattleSpellCatalog$AreaAttackSpell;)V`

- 類別：`local/lito381/server/ClientSession`
- 類型：例外表涵蓋、含迴圈（路徑比較器不支援）；事件後繼：`LAYOUT_ONLY`
- 比較器：`method nativeCompanionTargetuseClassicWorldAreaAttackSpell(Llocal/lito381/server/Protocol$AttackSpellRequest;Llocal/lito381/server/BattleSpellCatalog$AreaAttackSpell;)V: left=536:[705:java/lang/Exception  ] right=542:[715:java/lang/Exception  ]; paths rejected: normal control-flow cycle; labelled CFGs differ`

```diff
@@ -1,9 +1,10 @@
 META 2 null [java/io/IOException]
-TRY [536:[705:java/lang/Exception  ], 538:[705:java/lang/Exception  ], 539:[705:java/lang/Exception  ], 540:[705:java/lang/Exception  ], 544:[705:java/lang/Exception  ], 546:[705:java/lang/Exception  ], 548:[705:java/lang/Exception  ], 550:[705:java/lang/Exception  ], 551:[705:java/lang/Exception  ], 552:[705:java/lang/Exception  ], 553:[705:java/lang/Exception  ], 556:[705:java/lang/Exception  ], 559:[705:java/lang/Exception  ], 561:[705:java/lang/Exception  ], 563:[705:java/lang/Exception  ], 565:[705:java/lang/Exception  ], 566:[705:java/lang/Exception  ], 570:[705:java/lang/Exception  ], 571:[705:java/lang/Exception  ], 574:[705:java/lang/Exception  ], 577:[705:java/lang/Exception  ], 578:[705:java/lang/Exception  ], 584:[705:java/lang/Exception  ], 585:[705:java/lang/Exception  ], 589:[705:java/lang/Exception  ], 594:[705:java/lang/Exception  ], 599:[705:java/lang/Exception  ], 600:[705:java/lang/Exception  ], 603:[705:java/lang/Exception  ], 606:[705:java/lang/Exception  ], 607:[705:java/lang/Exception  ], 612:[705:java/lang/Exception  ], 614:[705:java/lang/Exception  ], 618:[705:java/lang/Exception  ], 620:[705:java/lang/Exception  ], 623:[705:java/lang/Exception  ], 626:[705:java/lang/Exception  ], 627:[705:java/lang/Exception  ], 630:[705:java/lang/Exception  ], 633:[705:java/lang/Exception  ], 637:[705:java/lang/Exception  ], 640:[705:java/lang/Exception  ], 642:[705:java/lang/Exception  ], 644:[705:java/lang/Exception  ], 646:[705:java/lang/Exception  ], 647:[705:java/lang/Exception  ], 651:[705:java/lang/Exception  ], 653:[705:java/lang/Exception  ], 655:[705:java/lang/Exception  ], 657:[705:java/lang/Exception  ], 659:[705:java/lang/Exception  ], 661:[705:java/lang/Exception  ], 665:[705:java/lang/Exception  ], 669:[705:java/lang/Exception  ], 670:[705:java/lang/Exception  ], 675:[705:java/lang/Exception  ], 677:[705:java/lang/Exception  ], 678:[705:java/lang/Exception  ], 682:[705:java/lang/Exception  ], 684:[705:java/lang/Exception  ], 686:[705:java/lang/Exception  ], 688:[705:java/lang/Exception  ], 689:[705:java/lang/Exception  ], 690:[705:java/lang/Exception  ], 693:[705:java/lang/Exception  ], 699:[705:java/lang/Exception  ], 700:[705:java/lang/Exception  ], 1071:[1077:java/lang/Exception  ], 1073:[1077:java/lang/Exception  ], 1074:[1077:java/lang/Exception  ], 1121:[1127:java/lang/Exception  ], 1123:[1127:java/lang/Exception  ], 1124:[1127:java/lang/Exception  ], 1171:[1177:java/lang/Exception  ], 1173:[1177:java/lang/Exception  ], 1174:[1177:java/lang/Exception  ], 1221:[1226:java/lang/Exception  ], 1223:[1226:java/lang/Exception  ], 1320:[1750:java/lang/Throwable  ], 1321:[1739:java/lang/Throwable  , 1750:java/lang/Throwable  ], 1324:[1739:java/lang/Throwable  , 1750:java/lang/Throwable  ], 1326:[1739:java/lang/Throwable  , 1750:java/lang/Throwable  ], 1327:[1739:java/lang/Throwable  , 1750:java/lang/Throwable  ], 1329:[1739:java/lang/Throwable  , 1750:java/lang/Throwable  ], 1330:[1739:java/lang/Throwable  , 1750:java/lang/Throwable  ], 1331:[1739:java/lang/Throwable  , 1750:java/lang/Throwable  ], 1332:[1739:java/lang/Throwable  , 1750:java/lang/Throwable  ], 1337:[1739:java/lang/Throwable  , 1750:java/lang/Throwable  ], 1338:[1739:java/lang/Throwable  , 1750:java/lang/Throwable  ], 1341:[1739:java/lang/Throwable  , 1750:java/lang/Throwable  ], 1345:[1739:java/lang/Throwable  , 1750:java/lang/Throwable  ], 1349:[1739:java/lang/Throwable  , 1750:java/lang/Throwable  ], 1352:[1739:java/lang/Throwable  , 1750:java/lang/Throwable  ], 1355:[1739:java/lang/Throwable  , 1750:java/lang/Throwable  ], 1358:[1739:java/lang/Throwable  , 1750:java/lang/Throwable  ], 1362:[1739:java/lang/Throwable  , 1750:java/lang/Throwable  ], 1368:[1739:java/lang/Throwable  , 1750:java/lang/Throwable  ], 1373:[1739:java/lang/Throwable  , 1750:java/lang/Throwable  ], 1376:[1739:java/lang/Throwable  , 1750:java/lang/Throwable  ], 1378:[1739:java/lang/Throwable  , 1750:java/lang/Throwable  ], 1380:[1739:java/lang/Throwable  , 1750:java/lang/Throwable  ], 1381:[1739:java/lang/Throwable  , 1750:java/lang/Throwable  ], 1392:[1739:java/lang/Throwable  , 1750:java/lang/Throwable  ], 1393:[1739:java/lang/Throwable  , 1750:java/lang/Throwable  ], 1400:[1739:java/lang/Throwable  , 1750:java/lang/Throwable  ], 1402:[1739:java/lang/Throwable  , 1750:java/lang/Throwable  ], 1406:[1739:java/lang/Throwable  , 1750:java/lang/Throwable  ], 1407:[1739:java/lang/Throwable  , 1750:java/lang/Throwable  ], 1410:[1739:java/lang/Throwable  , 1750:java/lang/Throwable  ], 1413:[1739:java/lang/Throwable  , 1750:java/lang/Throwable  ], 1416:[1739:java/lang/Throwable  , 1750:java/lang/Throwable  ], 1420:[1739:java/lang/Throwable  , 1750:java/lang/Throwable  ], 1424:[1739:java/lang/Throwable  , 1750:java/lang/Throwable  ], 1425:[1739:java/lang/Throwable  , 1750:java/lang/Throwable  ], 1426:[1739:java/lang/Throwable  , 1750:java/lang/Throwable  ], 1429:[1739:java/lang/Throwable  , 1750:java/lang/Throwable  ], 1431:[1739:java/lang/Throwable  , 1750:java/lang/Throwable  ], 1435:[1739:java/lang/Throwable  , 1750:java/lang/Throwable  ], 1437:[1739:java/lang/Throwable  , 1750:java/lang/Throwable  ], 1449:[1739:java/lang/Throwable  , 1750:java/lang/Throwable  ], 1453:[1739:java/lang/Throwable  , 1750:java/lang/Throwable  ], 1454:[1739:java/lang/Throwable  , 1750:java/lang/Throwable  ], 1461:[1739:java/lang/Throwable  , 1750:java/lang/Throwable  ], 1464:[1739:java/lang/Throwable  , 1750:java/lang/Throwable  ], 1467:[1739:java/lang/Throwable  , 1750:java/lang/Throwable  ], 1471:[1739:java/lang/Throwable  , 1750:java/lang/Throwable  ], 1475:[1739:java/lang/Throwable  , 1750:java/lang/Throwable  ], 1487:[1739:java/lang/Throwable  , 1750:java/lang/Throwable  ], 1489:[1739:java/lang/Throwable  , 1750:java/lang/Throwable  ], 1492:[1739:java/lang/Throwable  , 1750:java/lang/Throwable  ], 1493:[1739:java/lang/Throwable  , 1750:java/lang/Throwable  ], 1500:[1739:java/lang/Throwable  , 1750:java/lang/Throwable  ], 1503:[1739:java/lang/Throwable  , 1750:java/lang/Throwable  ], 1506:[1739:java/lang/Throwable  , 1750:java/lang/Throwable  ], 1510:[1739:java/lang/Throwable  , 1750:java/lang/Throwable  ], 1514:[1739:java/lang/Throwable  , 1750:java/lang/Throwable  ], 1527:[1739:java/lang/Throwable  , 1750:java/lang/Throwable  ], 1534:[1739:java/lang/Throwable  , 1750:java/lang/Throwable  ], 1538:[1739:java/lang/Throwable  , 1750:java/lang/Throwable  ], 1553:[1739:java/lang/Throwable  , 1750:java/lang/Throwable  ], 1560:[1739:java/lang/Throwable  , 1750:java/lang/Throwable  ], 1561:[1739:java/lang/Throwable  , 1750:java/lang/Throwable  ], 1565:[1739:java/lang/Throwable  , 1750:java/lang/Throwable  ], 1566:[1739:java/lang/Throwable  , 1750:java/lang/Throwable  ], 1574:[1739:java/lang/Throwable  , 1750:java/lang/Throwable  ], 1575:[1739:java/lang/Throwable  , 1750:java/lang/Throwable  ], 1578:[1739:java/lang/Throwable  , 1750:java/lang/Throwable  ], 1581:[1739:java/lang/Throwable  , 1750:java/lang/Throwable  ], 1585:[1739:java/lang/Throwable  , 1750:java/lang/Throwable  ], 1589:[1739:java/lang/Throwable  , 1750:java/lang/Throwable  ], 1593:[1739:java/lang/Throwable  , 1750:java/lang/Throwable  ], 1600:[1739:java/lang/Throwable  , 1750:java/lang/Throwable  ], 1608:[1739:java/lang/Throwable  , 1750:java/lang/Throwable  ], 1609:[1739:java/lang/Throwable  , 1750:java/lang/Throwable  ], 1636:[1739:java/lang/Throwable  , 1750:java/lang/Throwable  ], 1638:[1739:java/lang/Throwable  , 1750:java/lang/Throwable  ], 1641:[1739:java/lang/Throwable  , 1750:java/lang/Throwable  ], 1650:[1739:java/lang/Throwable  , 1750:java/lang/Throwable  ], 1652:[1739:java/lang/Throwable  , 1750:java/lang/Throwable  ], 1654:[1739:java/lang/Throwable  , 1750:java/lang/Throwable  ], 1658:[1739:java/lang/Throwable  , 1750:java/lang/Throwable  ], 1661:[1739:java/lang/Throwable  , 1750:java/lang/Throwable  ], 1662:[1739:java/lang/Throwable  , 1750:java/lang/Throwable  ], 1664:[1739:java/lang/Throwable  , 1750:java/lang/Throwable  ], 1667:[1739:java/lang/Throwable  , 1750:java/lang/Throwable  ], 1670:[1739:java/lang/Throwable  , 1750:java/lang/Throwable  ], 1672:[1739:java/lang/Throwable  , 1750:java/lang/Throwable  ], 1673:[1739:java/lang/Throwable  , 1750:java/lang/Throwable  ], 1676:[1739:java/lang/Throwable  , 1750:java/lang/Throwable  ], 1681:[1739:java/lang/Throwable  , 1750:java/lang/Throwable  ], 1682:[1739:java/lang/Throwable  , 1750:java/lang/Throwable  ], 1685:[1739:java/lang/Throwable  , 1750:java/lang/Throwable  ], 1688:[1739:java/lang/Throwable  , 1750:java/lang/Throwable  ], 1690:[1739:java/lang/Throwable  , 1750:java/lang/Throwable  ], 1695:[1739:java/lang/Throwable  , 1750:java/lang/Throwable  ], 1696:[1739:java/lang/Throwable  , 1750:java/lang/Throwable  ], 1698:[1739:java/lang/Throwable  , 1750:java/lang/Throwable  ], 1699:[1739:java/lang/Throwable  , 1750:java/lang/Throwable  ], 1700:[1739:java/lang/Throwable  , 1750:java/lang/Throwable  ], 1703:[1739:java/lang/Throwable  , 1750:java/lang/Throwable  ], 1709:[1739:java/lang/Throwable  , 1750:java/lang/Throwable  ], 1710:[1739:java/lang/Throwable  , 1750:java/lang/Throwable  ], 1712:[1739:java/lang/Throwable  , 1750:java/lang/Throwable  ], 1713:[1739:java/lang/Throwable  , 1750:java/lang/Throwable  ], 1714:[1739:java/lang/Throwable  , 1750:java/lang/Throwable  ], 1715:[1739:java/lang/Throwable  , 1750:java/lang/Throwable  ], 1717:[1739:java/lang/Throwable  , 1750:java/lang/Throwable  ], 1719:[1739:java/lang/Throwable  , 1750:java/lang/Throwable  ], 1721:[1739:java/lang/Throwable  , 1750:java/lang/Throwable  ], 1723:[1739:java/lang/Throwable  , 1750:java/lang/Throwable  ], 1725:[1739:java/lang/Throwable  , 1750:java/lang/Throwable  ], 1733:[1739:java/lang/Throwable  , 1750:java/lang/Throwable  ], 1734:[1739:java/lang/Throwable  , 1750:java/lang/Throwable  ], 1735:[1739:java/lang/Throwable  , 1750:java/lang/Throwable  ], 1737:[1739:java/lang/Throwable  , 1750:java/lang/Throwable  ], 1741:[1739:java/lang/Throwable  , 1750:java/lang/Throwable  ], 1743:[1750:java/lang/Throwable  ], 1745:[1750:java/lang/Throwable  ], 1748:[1750:java/lang/Throwable  ], 1752:[1750:java/lang/Throwable  ]]
+TRY [542:[715:java/lang/Exception  ], 544:[715:java/lang/Exception  ], 545:[715:java/lang/Exception  ], 546:[715:java/lang/Exception  ], 550:[715:java/lang/Exception  ], 552:[715:java/lang/Exception  ], 554:[715:java/lang/Exception  ], 556:[715:java/lang/Exception  ], 557:[715:java/lang/Exception  ], 558:[715:java/lang/Exception  ], 559:[715:java/lang/Exception  ], 562:[715:java/lang/Exception  ], 565:[715:java/lang/Exception  ], 567:[715:java/lang/Exception  ], 569:[715:java/lang/Exception  ], 571:[715:java/lang/Exception  ], 572:[715:java/lang/Exception  ], 576:[715:java/lang/Exception  ], 577:[715:java/lang/Exception  ], 580:[715:java/lang/Exception  ], 583:[715:java/lang/Exception  ], 584:[715:java/lang/Exception  ], 590:[715:java/lang/Exception  ], 591:[715:java/lang/Exception  ], 595:[715:java/lang/Exception  ], 600:[715:java/lang/Exception  ], 606:[715:java/lang/Exception  ], 607:[715:java/lang/Exception  ], 610:[715:java/lang/Exception  ], 613:[715:java/lang/Exception  ], 614:[715:java/lang/Exception  ], 619:[715:java/lang/Exception  ], 621:[715:java/lang/Exception  ], 626:[715:java/lang/Exception  ], 628:[715:java/lang/Exception  ], 631:[715:java/lang/Exception  ], 634:[715:java/lang/Exception  ], 635:[715:java/lang/Exception  ], 638:[715:java/lang/Exception  ], 641:[715:java/lang/Exception  ], 645:[715:java/lang/Exception  ], 648:[715:java/lang/Exception  ], 650:[715:java/lang/Exception  ], 652:[715:java/lang/Exception  ], 654:[715:java/lang/Exception  ], 655:[715:java/lang/Exception  ], 659:[715:java/lang/Exception  ], 661:[715:java/lang/Exception  ], 663:[715:java/lang/Exception  ], 665:[715:java/lang/Exception  ], 667:[715:java/lang/Exception  ], 669:[715:java/lang/Exception  ], 673:[715:java/lang/Exception  ], 678:[715:java/lang/Exception  ], 679:[715:java/lang/Exception  ], 685:[715:java/lang/Exception  ], 687:[715:java/lang/Exception  ], 688:[715:java/lang/Exception  ], 692:[715:java/lang/Exception  ], 694:[715:java/lang/Exception  ], 696:[715:java/lang/Exception  ], 698:[715:java/lang/Exception  ], 699:[715:java/lang/Exception  ], 700:[715:java/lang/Exception  ], 703:[715:java/lang/Exception  ], 709:[715:java/lang/Exception  ], 710:[715:java/lang/Exception  ], 1089:[1095:java/lang/Exception  ], 1091:[1095:java/lang/Exception  ], 1092:[1095:java/lang/Exception  ], 1139:[1145:java/lang/Exception  ], 1141:[1145:java/lang/Exception  ], 1142:[1145:java/lang/Exception  ], 1189:[1195:java/lang/Exception  ], 1191:[1195:java/lang/Exception  ], 1192:[1195:java/lang/Exception  ], 1239:[1244:java/lang/Exception  ], 1241:[1244:java/lang/Exception  ], 1338:[1769:java/lang/Throwable  ], 1339:[1761:java/lang/Throwable  , 1769:java/lang/Throwable  ], 1342:[1761:java/lang/Throwable  , 1769:java/lang/Throwable  ], 1344:[1761:java/lang/Throwable  , 1769:java/lang/Throwable  ], 1345:[1761:java/lang/Throwable  , 1769:java/lang/Throwable  ], 1347:[1761:java/lang/Throwable  , 1769:java/lang/Throwable  ], 1348:[1761:java/lang/Throwable  , 1769:java/lang/Throwable  ], 1349:[1761:java/lang/Throwable  , 1769:java/lang/Throwable  ], 1350:[1761:java/lang/Throwable  , 1769:java/lang/Throwable  ], 1355:[1761:java/lang/Throwable  , 1769:java/lang/Throwable  ], 1356:[1761:java/lang/Throwable  , 1769:java/lang/Throwable  ], 1359:[1761:java/lang/Throwable  , 1769:java/lang/Throwable  ], 1363:[1761:java/lang/Throwable  , 1769:java/lang/Throwable  ], 1367:[1761:java/lang/Throwable  , 1769:java/lang/Throwable  ], 1370:[1761:java/lang/Throwable  , 1769:java/lang/Throwable  ], 1373:[1761:java/lang/Throwable  , 1769:java/lang/Throwable  ], 1376:[1761:java/lang/Throwable  , 1769:java/lang/Throwable  ], 1380:[1761:java/lang/Throwable  , 1769:java/lang/Throwable  ], 1386:[1761:java/lang/Throwable  , 1769:java/lang/Throwable  ], 1391:[1761:java/lang/Throwable  , 1769:java/lang/Throwable  ], 1394:[1761:java/lang/Throwable  , 1769:java/lang/Throwable  ], 1396:[1761:java/lang/Throwable  , 1769:java/lang/Throwable  ], 1398:[1761:java/lang/Throwable  , 1769:java/lang/Throwable  ], 1399:[1761:java/lang/Throwable  , 1769:java/lang/Throwable  ], 1410:[1761:java/lang/Throwable  , 1769:java/lang/Throwable  ], 1411:[1761:java/lang/Throwable  , 1769:java/lang/Throwable  ], 1418:[1761:java/lang/Throwable  , 1769:java/lang/Throwable  ], 1420:[1761:java/lang/Throwable  , 1769:java/lang/Throwable  ], 1424:[1761:java/lang/Throwable  , 1769:java/lang/Throwable  ], 1425:[1761:java/lang/Throwable  , 1769:java/lang/Throwable  ], 1428:[1761:java/lang/Throwable  , 1769:java/lang/Throwable  ], 1431:[1761:java/lang/Throwable  , 1769:java/lang/Throwable  ], 1434:[1761:java/lang/Throwable  , 1769:java/lang/Throwable  ], 1438:[1761:java/lang/Throwable  , 1769:java/lang/Throwable  ], 1442:[1761:java/lang/Throwable  , 1769:java/lang/Throwable  ], 1443:[1761:java/lang/Throwable  , 1769:java/lang/Throwable  ], 1444:[1761:java/lang/Throwable  , 1769:java/lang/Throwable  ], 1447:[1761:java/lang/Throwable  , 1769:java/lang/Throwable  ], 1449:[1761:java/lang/Throwable  , 1769:java/lang/Throwable  ], 1453:[1761:java/lang/Throwable  , 1769:java/lang/Throwable  ], 1455:[1761:java/lang/Throwable  , 1769:java/lang/Throwable  ], 1467:[1761:java/lang/Throwable  , 1769:java/lang/Throwable  ], 1471:[1761:java/lang/Throwable  , 1769:java/lang/Throwable  ], 1472:[1761:java/lang/Throwable  , 1769:java/lang/Throwable  ], 1479:[1761:java/lang/Throwable  , 1769:java/lang/Throwable  ], 1482:[1761:java/lang/Throwable  , 1769:java/lang/Throwable  ], 1485:[1761:java/lang/Throwable  , 1769:java/lang/Throwable  ], 1489:[1761:java/lang/Throwable  , 1769:java/lang/Throwable  ], 1493:[1761:java/lang/Throwable  , 1769:java/lang/Throwable  ], 1505:[1761:java/lang/Throwable  , 1769:java/lang/Throwable  ], 1507:[1761:java/lang/Throwable  , 1769:java/lang/Throwable  ], 1510:[1761:java/lang/Throwable  , 1769:java/lang/Throwable  ], 1511:[1761:java/lang/Throwable  , 1769:java/lang/Throwable  ], 1518:[1761:java/lang/Throwable  , 1769:java/lang/Throwable  ], 1521:[1761:java/lang/Throwable  , 1769:java/lang/Throwable  ], 1524:[1761:java/lang/Throwable  , 1769:java/lang/Throwable  ], 1528:[1761:java/lang/Throwable  , 1769:java/lang/Throwable  ], 1532:[1761:java/lang/Throwable  , 1769:java/lang/Throwable  ], 1545:[1761:java/lang/Throwable  , 1769:java/lang/Throwable  ], 1552:[1761:java/lang/Throwable  , 1769:java/lang/Throwable  ], 1556:[1761:java/lang/Throwable  , 1769:java/lang/Throwable  ], 1571:[1761:java/lang/Throwable  , 1769:java/lang/Throwable  ], 1578:[1761:java/lang/Throwable  , 1769:java/lang/Throwable  ], 1579:[1761:java/lang/Throwable  , 1769:java/lang/Throwable  ], 1583:[1761:java/lang/Throwable  , 1769:java/lang/Throwable  ], 1584:[1761:java/lang/Throwable  , 1769:java/lang/Throwable  ], 1592:[1761:java/lang/Throwable  , 1769:java/lang/Throwable  ], 1593:[1761:java/lang/Throwable  , 1769:java/lang/Throwable  ], 1596:[1761:java/lang/Throwable  , 1769:java/lang/Throwable  ], 1599:[1761:java/lang/Throwable  , 1769:java/lang/Throwable  ], 1603:[1761:java/lang/Throwable  , 1769:java/lang/Throwable  ], 1607:[1761:java/lang/Throwable  , 1769:java/lang/Throwable  ], 1611:[1761:java/lang/Throwable  , 1769:java/lang/Throwable  ], 1618:[1761:java/lang/Throwable  , 1769:java/lang/Throwable  ], 1626:[1761:java/lang/Throwable  , 1769:java/lang/Throwable  ], 1627:[1761:java/lang/Throwable  , 1769:java/lang/Throwable  ], 1654:[1761:java/lang/Throwable  , 1769:java/lang/Throwable  ], 1656:[1761:java/lang/Throwable  , 1769:java/lang/Throwable  ], 1659:[1761:java/lang/Throwable  , 1769:java/lang/Throwable  ], 1667:[1761:java/lang/Throwable  , 1769:java/lang/Throwable  ], 1669:[1761:java/lang/Throwable  , 1769:java/lang/Throwable  ], 1674:[1761:java/lang/Throwable  , 1769:java/lang/Throwable  ], 1675:[1761:java/lang/Throwable  , 1769:java/lang/Throwable  ], 1677:[1761:java/lang/Throwable  , 1769:java/lang/Throwable  ], 1678:[1761:java/lang/Throwable  , 1769:java/lang/Throwable  ], 1679:[1761:java/lang/Throwable  , 1769:java/lang/Throwable  ], 1682:[1761:java/lang/Throwable  , 1769:java/lang/Throwable  ], 1688:[1761:java/lang/Throwable  , 1769:java/lang/Throwable  ], 1689:[1761:java/lang/Throwable  , 1769:java/lang/Throwable  ], 1691:[1761:java/lang/Throwable  , 1769:java/lang/Throwable  ], 1692:[1761:java/lang/Throwable  , 1769:java/lang/Throwable  ], 1693:[1761:java/lang/Throwable  , 1769:java/lang/Throwable  ], 1694:[1761:java/lang/Throwable  , 1769:java/lang/Throwable  ], 1696:[1761:java/lang/Throwable  , 1769:java/lang/Throwable  ], 1698:[1761:java/lang/Throwable  , 1769:java/lang/Throwable  ], 1700:[1761:java/lang/Throwable  , 1769:java/lang/Throwable  ], 1702:[1761:java/lang/Throwable  , 1769:java/lang/Throwable  ], 1704:[1761:java/lang/Throwable  , 1769:java/lang/Throwable  ], 1712:[1761:java/lang/Throwable  , 1769:java/lang/Throwable  ], 1713:[1761:java/lang/Throwable  , 1769:java/lang/Throwable  ], 1714:[1761:java/lang/Throwable  , 1769:java/lang/Throwable  ], 1716:[1761:java/lang/Throwable  , 1769:java/lang/Throwable  ], 1718:[1769:java/lang/Throwable  ], 1722:[1761:java/lang/Throwable  , 1769:java/lang/Throwable  ], 1724:[1761:java/lang/Throwable  , 1769:java/lang/Throwable  ], 1726:[1761:java/lang/Throwable  , 1769:java/lang/Throwable  ], 1729:[1761:java/lang/Throwable  , 1769:java/lang/Throwable  ], 1733:[1761:java/lang/Throwable  , 1769:java/lang/Throwable  ], 1736:[1761:java/lang/Throwable  , 1769:java/lang/Throwable  ], 1738:[1761:java/lang/Throwable  , 1769:java/lang/Throwable  ], 1741:[1761:java/lang/Throwable  , 1769:java/lang/Throwable  ], 1744:[1761:java/lang/Throwable  , 1769:java/lang/Throwable  ], 1746:[1761:java/lang/Throwable  , 1769:java/lang/Throwable  ], 1747:[1761:java/lang/Throwable  , 1769:java/lang/Throwable  ], 1750:[1761:java/lang/Throwable  , 1769:java/lang/Throwable  ], 1755:[1761:java/lang/Throwable  , 1769:java/lang/Throwable  ], 1756:[1761:java/lang/Throwable  , 1769:java/lang/Throwable  ], 1759:[1761:java/lang/Throwable  , 1769:java/lang/Throwable  ], 1763:[1761:java/lang/Throwable  , 1769:java/lang/Throwable  ], 1765:[1769:java/lang/Throwable  ], 1767:[1769:java/lang/Throwable  ], 1771:[1769:java/lang/Throwable  ]]
 ALOAD 0
 ALOAD 1
 INVOKESTATIC local/lito381/server/NativeSelectedPlayerIntent.attack(Llocal/lito381/server/ClientSession;Llocal/lito381/server/Protocol$AttackSpellRequest;)Z
-IFEQ L2984
+IFNE L5
+RETURN
 INVOKESTATIC local/lito381/server/CombatWorldMultiMagic.create()Llocal/lito381/server/CombatWorldMultiMagic$Batch;
 ASTORE 3
 CONST null
```

### `nativeCompanionTargetuseClassicWorldAttackSpell(Llocal/lito381/server/Protocol$AttackSpellRequest;Llocal/lito381/server/BattleSpellCatalog$AttackSpell;)V`

- 類別：`local/lito381/server/ClientSession`
- 類型：例外表涵蓋、含迴圈（路徑比較器不支援）；事件後繼：`LAYOUT_ONLY`
- 比較器：`method nativeCompanionTargetuseClassicWorldAttackSpell(Llocal/lito381/server/Protocol$AttackSpellRequest;Llocal/lito381/server/BattleSpellCatalog$AttackSpell;)V: left=626:[631:java/lang/Exception  ] right=630:[635:java/lang/Exception  ]; paths rejected: normal control-flow cycle; labelled CFGs differ`

```diff
@@ -1,9 +1,10 @@
 META 2 null [java/io/IOException]
-TRY [626:[631:java/lang/Exception  ], 628:[631:java/lang/Exception  ], 802:[1033:java/lang/Throwable  ], 805:[1033:java/lang/Throwable  ], 807:[1033:java/lang/Throwable  ], 808:[1033:java/lang/Throwable  ], 810:[1033:java/lang/Throwable  ], 811:[1033:java/lang/Throwable  ], 812:[1033:java/lang/Throwable  ], 813:[1033:java/lang/Throwable  ], 820:[1033:java/lang/Throwable  ], 827:[1033:java/lang/Throwable  ], 829:[1033:java/lang/Throwable  ], 833:[1033:java/lang/Throwable  ], 834:[1033:java/lang/Throwable  ], 837:[1033:java/lang/Throwable  ], 840:[1033:java/lang/Throwable  ], 843:[1033:java/lang/Throwable  ], 844:[1033:java/lang/Throwable  ], 848:[1033:java/lang/Throwable  ], 859:[1033:java/lang/Throwable  ], 860:[1033:java/lang/Throwable  ], 863:[1033:java/lang/Throwable  ], 871:[1033:java/lang/Throwable  ], 876:[1033:java/lang/Throwable  ], 877:[1033:java/lang/Throwable  ], 880:[1033:java/lang/Throwable  ], 883:[1033:java/lang/Throwable  ], 887:[1033:java/lang/Throwable  ], 889:[1033:java/lang/Throwable  ], 891:[1033:java/lang/Throwable  ], 893:[1033:java/lang/Throwable  ], 895:[1033:java/lang/Throwable  ], 901:[1033:java/lang/Throwable  ], 902:[1033:java/lang/Throwable  ], 903:[1033:java/lang/Throwable  ], 905:[1033:java/lang/Throwable  ], 910:[1033:java/lang/Throwable  ], 916:[1033:java/lang/Throwable  ], 917:[1033:java/lang/Throwable  ], 919:[1033:java/lang/Throwable  ], 920:[1033:java/lang/Throwable  ], 921:[1033:java/lang/Throwable  ], 923:[1033:java/lang/Throwable  ], 926:[1033:java/lang/Throwable  ], 932:[1033:java/lang/Throwable  ], 933:[1033:java/lang/Throwable  ], 935:[1033:java/lang/Throwable  ], 936:[1033:java/lang/Throwable  ], 937:[1033:java/lang/Throwable  ], 938:[1033:java/lang/Throwable  ], 940:[1033:java/lang/Throwable  ], 942:[1033:java/lang/Throwable  ], 944:[1033:java/lang/Throwable  ], 946:[1033:java/lang/Throwable  ], 947:[1033:java/lang/Throwable  ], 949:[1033:java/lang/Throwable  ], 952:[1033:java/lang/Throwable  ], 955:[1033:java/lang/Throwable  ], 959:[1033:java/lang/Throwable  ], 965:[1033:java/lang/Throwable  ], 968:[1033:java/lang/Throwable  ], 971:[1033:java/lang/Throwable  ], 974:[1033:java/lang/Throwable  ], 980:[1033:java/lang/Throwable  ], 984:[1033:java/lang/Throwable  ], 987:[1033:java/lang/Throwable  ], 993:[1033:java/lang/Throwable  ], 997:[1033:java/lang/Throwable  ], 1000:[1033:java/lang/Throwable  ], 1004:[1033:java/lang/Throwable  ], 1007:[1033:java/lang/Throwable  ], 1008:[1033:java/lang/Throwable  ], 1010:[1033:java/lang/Throwable  ], 1013:[1033:java/lang/Throwable  ], 1016:[1033:java/lang/Throwable  ], 1018:[1033:java/lang/Throwable  ], 1019:[1033:java/lang/Throwable  ], 1022:[1033:java/lang/Throwable  ], 1027:[1033:java/lang/Throwable  ], 1028:[1033:java/lang/Throwable  ], 1031:[1033:java/lang/Throwable  ], 1035:[1033:java/lang/Throwable  ]]
+TRY [630:[635:java/lang/Exception  ], 632:[635:java/lang/Exception  ], 806:[1037:java/lang/Throwable  ], 809:[1037:java/lang/Throwable  ], 811:[1037:java/lang/Throwable  ], 812:[1037:java/lang/Throwable  ], 814:[1037:java/lang/Throwable  ], 815:[1037:java/lang/Throwable  ], 816:[1037:java/lang/Throwable  ], 817:[1037:java/lang/Throwable  ], 824:[1037:java/lang/Throwable  ], 831:[1037:java/lang/Throwable  ], 833:[1037:java/lang/Throwable  ], 837:[1037:java/lang/Throwable  ], 838:[1037:java/lang/Throwable  ], 841:[1037:java/lang/Throwable  ], 844:[1037:java/lang/Throwable  ], 847:[1037:java/lang/Throwable  ], 848:[1037:java/lang/Throwable  ], 852:[1037:java/lang/Throwable  ], 863:[1037:java/lang/Throwable  ], 864:[1037:java/lang/Throwable  ], 867:[1037:java/lang/Throwable  ], 875:[1037:java/lang/Throwable  ], 880:[1037:java/lang/Throwable  ], 881:[1037:java/lang/Throwable  ], 884:[1037:java/lang/Throwable  ], 887:[1037:java/lang/Throwable  ], 891:[1037:java/lang/Throwable  ], 893:[1037:java/lang/Throwable  ], 895:[1037:java/lang/Throwable  ], 897:[1037:java/lang/Throwable  ], 899:[1037:java/lang/Throwable  ], 905:[1037:java/lang/Throwable  ], 906:[1037:java/lang/Throwable  ], 907:[1037:java/lang/Throwable  ], 909:[1037:java/lang/Throwable  ], 914:[1037:java/lang/Throwable  ], 920:[1037:java/lang/Throwable  ], 921:[1037:java/lang/Throwable  ], 923:[1037:java/lang/Throwable  ], 924:[1037:java/lang/Throwable  ], 925:[1037:java/lang/Throwable  ], 927:[1037:java/lang/Throwable  ], 930:[1037:java/lang/Throwable  ], 936:[1037:java/lang/Throwable  ], 937:[1037:java/lang/Throwable  ], 939:[1037:java/lang/Throwable  ], 940:[1037:java/lang/Throwable  ], 941:[1037:java/lang/Throwable  ], 942:[1037:java/lang/Throwable  ], 944:[1037:java/lang/Throwable  ], 946:[1037:java/lang/Throwable  ], 948:[1037:java/lang/Throwable  ], 950:[1037:java/lang/Throwable  ], 951:[1037:java/lang/Throwable  ], 953:[1037:java/lang/Throwable  ], 956:[1037:java/lang/Throwable  ], 959:[1037:java/lang/Throwable  ], 963:[1037:java/lang/Throwable  ], 969:[1037:java/lang/Throwable  ], 972:[1037:java/lang/Throwable  ], 975:[1037:java/lang/Throwable  ], 978:[1037:java/lang/Throwable  ], 984:[1037:java/lang/Throwable  ], 988:[1037:java/lang/Throwable  ], 992:[1037:java/lang/Throwable  ], 995:[1037:java/lang/Throwable  ], 999:[1037:java/lang/Throwable  ], 1002:[1037:java/lang/Throwable  ], 1005:[1037:java/lang/Throwable  ], 1009:[1037:java/lang/Throwable  ], 1012:[1037:java/lang/Throwable  ], 1014:[1037:java/lang/Throwable  ], 1017:[1037:java/lang/Throwable  ], 1020:[1037:java/lang/Throwable  ], 1022:[1037:java/lang/Throwable  ], 1023:[1037:java/lang/Throwable  ], 1026:[1037:java/lang/Throwable  ], 1031:[1037:java/lang/Throwable  ], 1032:[1037:java/lang/Throwable  ], 1035:[1037:java/lang/Throwable  ], 1039:[1037:java/lang/Throwable  ]]
 ALOAD 0
 ALOAD 1
 INVOKESTATIC local/lito381/server/NativeSelectedPlayerIntent.attack(Llocal/lito381/server/ClientSession;Llocal/lito381/server/Protocol$AttackSpellRequest;)Z
-IFEQ L2609
+IFNE L5
+RETURN
 CONST null
 ASTORE 3
 CONST null
```

### `nativeCompanionTargetuseClassicWorldLineAttackSpell([BLlocal/lito381/server/BattleSpellCatalog$LineAttackSpell;)V`

- 類別：`local/lito381/server/ClientSession`
- 類型：例外表涵蓋、含迴圈（路徑比較器不支援）；事件後繼：`LAYOUT_ONLY`
- 比較器：`method nativeCompanionTargetuseClassicWorldLineAttackSpell([BLlocal/lito381/server/BattleSpellCatalog$LineAttackSpell;)V: left=432:[688:java/lang/Exception  ] right=437:[697:java/lang/Exception  ]; paths rejected: normal control-flow cycle; labelled CFGs differ`

```diff
@@ -1,5 +1,5 @@
 META 2 null [java/io/IOException]
-TRY [432:[688:java/lang/Exception  ], 434:[688:java/lang/Exception  ], 435:[688:java/lang/Exception  ], 436:[688:java/lang/Exception  ], 440:[688:java/lang/Exception  ], 442:[688:java/lang/Exception  ], 443:[688:java/lang/Exception  ], 444:[688:java/lang/Exception  ], 445:[688:java/lang/Exception  ], 448:[688:java/lang/Exception  ], 451:[688:java/lang/Exception  ], 453:[688:java/lang/Exception  ], 455:[688:java/lang/Exception  ], 457:[688:java/lang/Exception  ], 458:[688:java/lang/Exception  ], 462:[688:java/lang/Exception  ], 463:[688:java/lang/Exception  ], 466:[688:java/lang/Exception  ], 469:[688:java/lang/Exception  ], 470:[688:java/lang/Exception  ], 476:[688:java/lang/Exception  ], 477:[688:java/lang/Exception  ], 481:[688:java/lang/Exception  ], 486:[688:java/lang/Exception  ], 491:[688:java/lang/Exception  ], 492:[688:java/lang/Exception  ], 495:[688:java/lang/Exception  ], 498:[688:java/lang/Exception  ], 499:[688:java/lang/Exception  ], 502:[688:java/lang/Exception  ], 504:[688:java/lang/Exception  ], 508:[688:java/lang/Exception  ], 509:[688:java/lang/Exception  ], 512:[688:java/lang/Exception  ], 515:[688:java/lang/Exception  ], 520:[688:java/lang/Exception  ], 524:[688:java/lang/Exception  ], 526:[688:java/lang/Exception  ], 527:[688:java/lang/Exception  ], 531:[688:java/lang/Exception  ], 533:[688:java/lang/Exception  ], 534:[688:java/lang/Exception  ], 540:[688:java/lang/Exception  ], 542:[688:java/lang/Exception  ], 544:[688:java/lang/Exception  ], 545:[688:java/lang/Exception  ], 549:[688:java/lang/Exception  ], 550:[688:java/lang/Exception  ], 555:[688:java/lang/Exception  ], 557:[688:java/lang/Exception  ], 558:[688:java/lang/Exception  ], 562:[688:java/lang/Exception  ], 564:[688:java/lang/Exception  ], 565:[688:java/lang/Exception  ], 566:[688:java/lang/Exception  ], 569:[688:java/lang/Exception  ], 575:[688:java/lang/Exception  ], 576:[688:java/lang/Exception  ], 585:[688:java/lang/Exception  ], 587:[688:java/lang/Exception  ], 588:[688:java/lang/Exception  ], 592:[688:java/lang/Exception  ], 594:[688:java/lang/Exception  ], 595:[688:java/lang/Exception  ], 596:[688:java/lang/Exception  ], 600:[688:java/lang/Exception  ], 604:[688:java/lang/Exception  ], 605:[688:java/lang/Exception  ], 607:[688:java/lang/Exception  ], 610:[688:java/lang/Exception  ], 615:[688:java/lang/Exception  ], 616:[688:java/lang/Exception  ], 619:[688:java/lang/Exception  ], 622:[688:java/lang/Exception  ], 623:[688:java/lang/Exception  ], 626:[688:java/lang/Exception  ], 629:[688:java/lang/Exception  ], 632:[688:java/lang/Exception  ], 638:[688:java/lang/Exception  ], 640:[688:java/lang/Exception  ], 645:[688:java/lang/Exception  ], 647:[688:java/lang/Exception  ], 648:[688:java/lang/Exception  ], 654:[688:java/lang/Exception  ], 656:[688:java/lang/Exception  ], 658:[688:java/lang/Exception  ], 659:[688:java/lang/Exception  ], 662:[688:java/lang/Exception  ], 664:[688:java/lang/Exception  ], 665:[688:java/lang/Exception  ], 669:[688:java/lang/Exception  ], 671:[688:java/lang/Exception  ], 672:[688:java/lang/Exception  ], 673:[688:java/lang/Exception  ], 677:[688:java/lang/Exception  ], 681:[688:java/lang/Exception  ], 682:[688:java/lang/Exception  ], 684:[688:java/lang/Exception  ], 733:[846:java/lang/Throwable  ], 736:[846:java/lang/Throwable  ], 738:[846:java/lang/Throwable  ], 739:[846:java/lang/Throwable  ], 741:[846:java/lang/Throwable  ], 742:[846:java/lang/Throwable  ], 743:[846:java/lang/Throwable  ], 744:[846:java/lang/Throwable  ], 751:[846:java/lang/Throwable  ], 752:[846:java/lang/Throwable  ], 755:[846:java/lang/Throwable  ], 758:[846:java/lang/Throwable  ], 761:[846:java/lang/Throwable  ], 765:[846:java/lang/Throwable  ], 769:[846:java/lang/Throwable  ], 773:[846:java/lang/Throwable  ], 777:[846:java/lang/Throwable  ], 781:[846:java/lang/Throwable  ], 787:[846:java/lang/Throwable  ], 788:[846:java/lang/Throwable  ], 790:[846:java/lang/Throwable  ], 791:[846:java/lang/Throwable  ], 792:[846:java/lang/Throwable  ], 793:[846:java/lang/Throwable  ], 795:[846:java/lang/Throwable  ], 797:[846:java/lang/Throwable  ], 803:[846:java/lang/Throwable  ], 804:[846:java/lang/Throwable  ], 805:[846:java/lang/Throwable  ], 807:[846:java/lang/Throwable  ], 811:[846:java/lang/Throwable  ], 812:[846:java/lang/Throwable  ], 813:[846:java/lang/Throwable  ], 817:[846:java/lang/Throwable  ], 820:[846:java/lang/Throwable  ], 821:[846:java/lang/Throwable  ], 823:[846:java/lang/Throwable  ], 830:[846:java/lang/Throwable  ], 831:[846:java/lang/Throwable  ], 834:[846:java/lang/Throwable  ], 837:[846:java/lang/Throwable  ], 842:[846:java/lang/Throwable  ], 844:[846:java/lang/Throwable  ], 848:[846:java/lang/Throwable  ]]
+TRY [437:[697:java/lang/Exception  ], 439:[697:java/lang/Exception  ], 440:[697:java/lang/Exception  ], 441:[697:java/lang/Exception  ], 445:[697:java/lang/Exception  ], 447:[697:java/lang/Exception  ], 448:[697:java/lang/Exception  ], 449:[697:java/lang/Exception  ], 450:[697:java/lang/Exception  ], 453:[697:java/lang/Exception  ], 456:[697:java/lang/Exception  ], 458:[697:java/lang/Exception  ], 460:[697:java/lang/Exception  ], 462:[697:java/lang/Exception  ], 463:[697:java/lang/Exception  ], 467:[697:java/lang/Exception  ], 468:[697:java/lang/Exception  ], 471:[697:java/lang/Exception  ], 474:[697:java/lang/Exception  ], 475:[697:java/lang/Exception  ], 481:[697:java/lang/Exception  ], 482:[697:java/lang/Exception  ], 486:[697:java/lang/Exception  ], 491:[697:java/lang/Exception  ], 497:[697:java/lang/Exception  ], 498:[697:java/lang/Exception  ], 501:[697:java/lang/Exception  ], 504:[697:java/lang/Exception  ], 505:[697:java/lang/Exception  ], 508:[697:java/lang/Exception  ], 510:[697:java/lang/Exception  ], 514:[697:java/lang/Exception  ], 515:[697:java/lang/Exception  ], 518:[697:java/lang/Exception  ], 521:[697:java/lang/Exception  ], 526:[697:java/lang/Exception  ], 530:[697:java/lang/Exception  ], 532:[697:java/lang/Exception  ], 533:[697:java/lang/Exception  ], 537:[697:java/lang/Exception  ], 539:[697:java/lang/Exception  ], 540:[697:java/lang/Exception  ], 546:[697:java/lang/Exception  ], 548:[697:java/lang/Exception  ], 550:[697:java/lang/Exception  ], 551:[697:java/lang/Exception  ], 556:[697:java/lang/Exception  ], 557:[697:java/lang/Exception  ], 563:[697:java/lang/Exception  ], 565:[697:java/lang/Exception  ], 566:[697:java/lang/Exception  ], 570:[697:java/lang/Exception  ], 572:[697:java/lang/Exception  ], 573:[697:java/lang/Exception  ], 574:[697:java/lang/Exception  ], 577:[697:java/lang/Exception  ], 583:[697:java/lang/Exception  ], 584:[697:java/lang/Exception  ], 593:[697:java/lang/Exception  ], 595:[697:java/lang/Exception  ], 596:[697:java/lang/Exception  ], 600:[697:java/lang/Exception  ], 602:[697:java/lang/Exception  ], 603:[697:java/lang/Exception  ], 604:[697:java/lang/Exception  ], 608:[697:java/lang/Exception  ], 612:[697:java/lang/Exception  ], 613:[697:java/lang/Exception  ], 615:[697:java/lang/Exception  ], 618:[697:java/lang/Exception  ], 623:[697:java/lang/Exception  ], 624:[697:java/lang/Exception  ], 627:[697:java/lang/Exception  ], 630:[697:java/lang/Exception  ], 631:[697:java/lang/Exception  ], 634:[697:java/lang/Exception  ], 637:[697:java/lang/Exception  ], 640:[697:java/lang/Exception  ], 646:[697:java/lang/Exception  ], 648:[697:java/lang/Exception  ], 653:[697:java/lang/Exception  ], 655:[697:java/lang/Exception  ], 656:[697:java/lang/Exception  ], 662:[697:java/lang/Exception  ], 664:[697:java/lang/Exception  ], 666:[697:java/lang/Exception  ], 667:[697:java/lang/Exception  ], 671:[697:java/lang/Exception  ], 673:[697:java/lang/Exception  ], 674:[697:java/lang/Exception  ], 678:[697:java/lang/Exception  ], 680:[697:java/lang/Exception  ], 681:[697:java/lang/Exception  ], 682:[697:java/lang/Exception  ], 686:[697:java/lang/Exception  ], 690:[697:java/lang/Exception  ], 691:[697:java/lang/Exception  ], 693:[697:java/lang/Exception  ], 742:[857:java/lang/Throwable  ], 745:[857:java/lang/Throwable  ], 747:[857:java/lang/Throwable  ], 748:[857:java/lang/Throwable  ], 750:[857:java/lang/Throwable  ], 751:[857:java/lang/Throwable  ], 752:[857:java/lang/Throwable  ], 753:[857:java/lang/Throwable  ], 760:[857:java/lang/Throwable  ], 761:[857:java/lang/Throwable  ], 764:[857:java/lang/Throwable  ], 767:[857:java/lang/Throwable  ], 770:[857:java/lang/Throwable  ], 774:[857:java/lang/Throwable  ], 778:[857:java/lang/Throwable  ], 782:[857:java/lang/Throwable  ], 786:[857:java/lang/Throwable  ], 790:[857:java/lang/Throwable  ], 796:[857:java/lang/Throwable  ], 797:[857:java/lang/Throwable  ], 799:[857:java/lang/Throwable  ], 800:[857:java/lang/Throwable  ], 801:[857:java/lang/Throwable  ], 802:[857:java/lang/Throwable  ], 804:[857:java/lang/Throwable  ], 806:[857:java/lang/Throwable  ], 812:[857:java/lang/Throwable  ], 813:[857:java/lang/Throwable  ], 814:[857:java/lang/Throwable  ], 816:[857:java/lang/Throwable  ], 820:[857:java/lang/Throwable  ], 821:[857:java/lang/Throwable  ], 822:[857:java/lang/Throwable  ], 825:[857:java/lang/Throwable  ], 829:[857:java/lang/Throwable  ], 832:[857:java/lang/Throwable  ], 834:[857:java/lang/Throwable  ], 841:[857:java/lang/Throwable  ], 842:[857:java/lang/Throwable  ], 845:[857:java/lang/Throwable  ], 848:[857:java/lang/Throwable  ], 853:[857:java/lang/Throwable  ], 855:[857:java/lang/Throwable  ], 859:[857:java/lang/Throwable  ]]
 INVOKESTATIC local/lito381/server/CombatWorldMultiMagic.create()Llocal/lito381/server/CombatWorldMultiMagic$Batch;
 ASTORE 3
 CONST null
```

### `nativeCubeHarmonyBody([BLlocal/lito381/server/BattleSpellCatalog$CubeHarmonySpell;)V`

- 類別：`local/lito381/server/ClientSession`
- 類型：分支排列、含迴圈（路徑比較器不支援）；事件後繼：`LAYOUT_ONLY`
- 比較器：`method nativeCubeHarmonyBody([BLlocal/lito381/server/BattleSpellCatalog$CubeHarmonySpell;)V insn[7]: left=IF_ICMPNE L384 right=IF_ICMPNE L12; paths rejected: normal control-flow cycle; labelled CFGs differ`

```diff
@@ -7,34 +7,36 @@
 INVOKEVIRTUAL local/lito381/server/Protocol$SpellSelector.skillId()I
 ALOAD 2
 INVOKEVIRTUAL local/lito381/server/BattleSpellCatalog$CubeHarmonySpell.skillId()I
-IF_ICMPNE L384
+IF_ICMPNE L12
 ALOAD 2
 INVOKEVIRTUAL local/lito381/server/BattleSpellCatalog$CubeHarmonySpell.requestShape()Llocal/lito381/server/BattleSpellCatalog$RequestShape;
 GETSTATIC local/lito381/server/BattleSpellCatalog$RequestShape.SELECTOR_ONLY:Llocal/lito381/server/BattleSpellCatalog$RequestShape;
-IF_ACMPNE L384
+IF_ACMPEQ L13
+RETURN
 ALOAD 0
 GETFIELD local/lito381/server/ClientSession.phase:Llocal/lito381/server/ClientSession$Phase;
 ASTORE 4
 ALOAD 4
 GETSTATIC local/lito381/server/ClientSession$Phase.BATTLE:Llocal/lito381/server/ClientSession$Phase;
-IF_ACMPEQ L21
-ALOAD 4
-GETSTATIC local/lito381/server/ClientSession$Phase.WORLD:Llocal/lito381/server/ClientSession$Phase;
-IF_ACMPNE L384
+IF_ACMPEQ L23
+ALOAD 4
+GETSTATIC local/lito381/server/ClientSession$Phase.WORLD:Llocal/lito381/server/ClientSession$Phase;
…（其餘 hunk 見 scratch/issue-70/r6-b-classified.json）
```

### `nativeCubeIgnitionBody([BLlocal/lito381/server/BattleSpellCatalog$CubeIgnitionSpell;)V`

- 類別：`local/lito381/server/ClientSession`
- 類型：分支排列、含迴圈（路徑比較器不支援）；事件後繼：`LAYOUT_ONLY`
- 比較器：`method nativeCubeIgnitionBody([BLlocal/lito381/server/BattleSpellCatalog$CubeIgnitionSpell;)V insn[7]: left=IF_ICMPNE L382 right=IF_ICMPNE L12; paths rejected: normal control-flow cycle; labelled CFGs differ`

```diff
@@ -7,34 +7,36 @@
 INVOKEVIRTUAL local/lito381/server/Protocol$SpellSelector.skillId()I
 ALOAD 2
 INVOKEVIRTUAL local/lito381/server/BattleSpellCatalog$CubeIgnitionSpell.skillId()I
-IF_ICMPNE L382
+IF_ICMPNE L12
 ALOAD 2
 INVOKEVIRTUAL local/lito381/server/BattleSpellCatalog$CubeIgnitionSpell.requestShape()Llocal/lito381/server/BattleSpellCatalog$RequestShape;
 GETSTATIC local/lito381/server/BattleSpellCatalog$RequestShape.SELECTOR_ONLY:Llocal/lito381/server/BattleSpellCatalog$RequestShape;
-IF_ACMPNE L382
+IF_ACMPEQ L13
+RETURN
 ALOAD 0
 GETFIELD local/lito381/server/ClientSession.phase:Llocal/lito381/server/ClientSession$Phase;
 ASTORE 4
 ALOAD 4
 GETSTATIC local/lito381/server/ClientSession$Phase.BATTLE:Llocal/lito381/server/ClientSession$Phase;
-IF_ACMPEQ L21
-ALOAD 4
-GETSTATIC local/lito381/server/ClientSession$Phase.WORLD:Llocal/lito381/server/ClientSession$Phase;
-IF_ACMPNE L382
+IF_ACMPEQ L23
+ALOAD 4
+GETSTATIC local/lito381/server/ClientSession$Phase.WORLD:Llocal/lito381/server/ClientSession$Phase;
…（其餘 hunk 見 scratch/issue-70/r6-b-classified.json）
```

### `nativeCubeQuakeBody([BLlocal/lito381/server/BattleSpellCatalog$CubeQuakeSpell;)V`

- 類別：`local/lito381/server/ClientSession`
- 類型：分支排列、含迴圈（路徑比較器不支援）；事件後繼：`LAYOUT_ONLY`
- 比較器：`method nativeCubeQuakeBody([BLlocal/lito381/server/BattleSpellCatalog$CubeQuakeSpell;)V insn[7]: left=IF_ICMPNE L382 right=IF_ICMPNE L12; paths rejected: normal control-flow cycle; labelled CFGs differ`

```diff
@@ -7,34 +7,36 @@
 INVOKEVIRTUAL local/lito381/server/Protocol$SpellSelector.skillId()I
 ALOAD 2
 INVOKEVIRTUAL local/lito381/server/BattleSpellCatalog$CubeQuakeSpell.skillId()I
-IF_ICMPNE L382
+IF_ICMPNE L12
 ALOAD 2
 INVOKEVIRTUAL local/lito381/server/BattleSpellCatalog$CubeQuakeSpell.requestShape()Llocal/lito381/server/BattleSpellCatalog$RequestShape;
 GETSTATIC local/lito381/server/BattleSpellCatalog$RequestShape.SELECTOR_ONLY:Llocal/lito381/server/BattleSpellCatalog$RequestShape;
-IF_ACMPNE L382
+IF_ACMPEQ L13
+RETURN
 ALOAD 0
 GETFIELD local/lito381/server/ClientSession.phase:Llocal/lito381/server/ClientSession$Phase;
 ASTORE 4
 ALOAD 4
 GETSTATIC local/lito381/server/ClientSession$Phase.BATTLE:Llocal/lito381/server/ClientSession$Phase;
-IF_ACMPEQ L21
-ALOAD 4
-GETSTATIC local/lito381/server/ClientSession$Phase.WORLD:Llocal/lito381/server/ClientSession$Phase;
-IF_ACMPNE L382
+IF_ACMPEQ L23
+ALOAD 4
+GETSTATIC local/lito381/server/ClientSession$Phase.WORLD:Llocal/lito381/server/ClientSession$Phase;
…（其餘 hunk 見 scratch/issue-70/r6-b-classified.json）
```

### `nativeCubeShockBody([BLlocal/lito381/server/BattleSpellCatalog$CubeShockSpell;)V`

- 類別：`local/lito381/server/ClientSession`
- 類型：分支排列、含迴圈（路徑比較器不支援）；事件後繼：`LAYOUT_ONLY`
- 比較器：`method nativeCubeShockBody([BLlocal/lito381/server/BattleSpellCatalog$CubeShockSpell;)V insn[7]: left=IF_ICMPNE L382 right=IF_ICMPNE L12; paths rejected: normal control-flow cycle; labelled CFGs differ`

```diff
@@ -7,34 +7,36 @@
 INVOKEVIRTUAL local/lito381/server/Protocol$SpellSelector.skillId()I
 ALOAD 2
 INVOKEVIRTUAL local/lito381/server/BattleSpellCatalog$CubeShockSpell.skillId()I
-IF_ICMPNE L382
+IF_ICMPNE L12
 ALOAD 2
 INVOKEVIRTUAL local/lito381/server/BattleSpellCatalog$CubeShockSpell.requestShape()Llocal/lito381/server/BattleSpellCatalog$RequestShape;
 GETSTATIC local/lito381/server/BattleSpellCatalog$RequestShape.SELECTOR_ONLY:Llocal/lito381/server/BattleSpellCatalog$RequestShape;
-IF_ACMPNE L382
+IF_ACMPEQ L13
+RETURN
 ALOAD 0
 GETFIELD local/lito381/server/ClientSession.phase:Llocal/lito381/server/ClientSession$Phase;
 ASTORE 4
 ALOAD 4
 GETSTATIC local/lito381/server/ClientSession$Phase.BATTLE:Llocal/lito381/server/ClientSession$Phase;
-IF_ACMPEQ L21
-ALOAD 4
-GETSTATIC local/lito381/server/ClientSession$Phase.WORLD:Llocal/lito381/server/ClientSession$Phase;
-IF_ACMPNE L382
+IF_ACMPEQ L23
+ALOAD 4
+GETSTATIC local/lito381/server/ClientSession$Phase.WORLD:Llocal/lito381/server/ClientSession$Phase;
…（其餘 hunk 見 scratch/issue-70/r6-b-classified.json）
```

### `nativeFreezeAcceptedOriginalWorld([BLlocal/lito381/server/BattleSpellCatalog$FreezeAttackSpell;)V`

- 類別：`local/lito381/server/ClientSession`
- 類型：例外表涵蓋、含迴圈（路徑比較器不支援）；事件後繼：`LAYOUT_ONLY`
- 比較器：`method nativeFreezeAcceptedOriginalWorld([BLlocal/lito381/server/BattleSpellCatalog$FreezeAttackSpell;)V: left=337:[342:java/lang/Exception  ] right=342:[347:java/lang/Exception  ]; paths rejected: normal control-flow cycle; labelled CFGs differ`

```diff
@@ -1,5 +1,5 @@
 META 0 null [java/io/IOException]
-TRY [337:[342:java/lang/Exception  ], 339:[342:java/lang/Exception  ], 430:[639:java/lang/Throwable  ], 433:[639:java/lang/Throwable  ], 435:[639:java/lang/Throwable  ], 436:[639:java/lang/Throwable  ], 438:[639:java/lang/Throwable  ], 439:[639:java/lang/Throwable  ], 440:[639:java/lang/Throwable  ], 441:[639:java/lang/Throwable  ], 448:[639:java/lang/Throwable  ], 455:[639:java/lang/Throwable  ], 459:[639:java/lang/Throwable  ], 460:[639:java/lang/Throwable  ], 465:[639:java/lang/Throwable  ], 466:[639:java/lang/Throwable  ], 469:[639:java/lang/Throwable  ], 472:[639:java/lang/Throwable  ], 475:[639:java/lang/Throwable  ], 479:[639:java/lang/Throwable  ], 490:[639:java/lang/Throwable  ], 492:[639:java/lang/Throwable  ], 496:[639:java/lang/Throwable  ], 497:[639:java/lang/Throwable  ], 500:[639:java/lang/Throwable  ], 503:[639:java/lang/Throwable  ], 508:[639:java/lang/Throwable  ], 509:[639:java/lang/Throwable  ], 513:[639:java/lang/Throwable  ], 524:[639:java/lang/Throwable  ], 527:[639:java/lang/Throwable  ], 530:[639:java/lang/Throwable  ], 538:[639:java/lang/Throwable  ], 539:[639:java/lang/Throwable  ], 542:[639:java/lang/Throwable  ], 545:[639:java/lang/Throwable  ], 548:[639:java/lang/Throwable  ], 552:[639:java/lang/Throwable  ], 556:[639:java/lang/Throwable  ], 560:[639:java/lang/Throwable  ], 568:[639:java/lang/Throwable  ], 578:[639:java/lang/Throwable  ], 584:[639:java/lang/Throwable  ], 585:[639:java/lang/Throwable  ], 587:[639:java/lang/Throwable  ], 588:[639:java/lang/Throwable  ], 589:[639:java/lang/Throwable  ], 590:[639:java/lang/Throwable  ], 592:[639:java/lang/Throwable  ], 594:[639:java/lang/Throwable  ], 600:[639:java/lang/Throwable  ], 601:[639:java/lang/Throwable  ], 602:[639:java/lang/Throwable  ], 604:[639:java/lang/Throwable  ], 608:[639:java/lang/Throwable  ], 613:[639:java/lang/Throwable  ], 616:[639:java/lang/Throwable  ], 617:[639:java/lang/Throwable  ], 619:[639:java/lang/Throwable  ], 622:[639:java/lang/Throwable  ], 624:[639:java/lang/Throwable  ], 625:[639:java/lang/Throwable  ], 628:[639:java/lang/Throwable  ], 633:[639:java/lang/Throwable  ], 634:[639:java/lang/Throwable  ], 637:[639:java/lang/Throwable  ], 641:[639:java/lang/Throwable  ]]
+TRY [342:[347:java/lang/Exception  ], 344:[347:java/lang/Exception  ], 435:[646:java/lang/Throwable  ], 438:[646:java/lang/Throwable  ], 440:[646:java/lang/Throwable  ], 441:[646:java/lang/Throwable  ], 443:[646:java/lang/Throwable  ], 444:[646:java/lang/Throwable  ], 445:[646:java/lang/Throwable  ], 446:[646:java/lang/Throwable  ], 453:[646:java/lang/Throwable  ], 460:[646:java/lang/Throwable  ], 464:[646:java/lang/Throwable  ], 465:[646:java/lang/Throwable  ], 470:[646:java/lang/Throwable  ], 471:[646:java/lang/Throwable  ], 474:[646:java/lang/Throwable  ], 477:[646:java/lang/Throwable  ], 480:[646:java/lang/Throwable  ], 484:[646:java/lang/Throwable  ], 495:[646:java/lang/Throwable  ], 497:[646:java/lang/Throwable  ], 501:[646:java/lang/Throwable  ], 502:[646:java/lang/Throwable  ], 505:[646:java/lang/Throwable  ], 508:[646:java/lang/Throwable  ], 513:[646:java/lang/Throwable  ], 514:[646:java/lang/Throwable  ], 518:[646:java/lang/Throwable  ], 529:[646:java/lang/Throwable  ], 532:[646:java/lang/Throwable  ], 535:[646:java/lang/Throwable  ], 543:[646:java/lang/Throwable  ], 544:[646:java/lang/Throwable  ], 547:[646:java/lang/Throwable  ], 550:[646:java/lang/Throwable  ], 553:[646:java/lang/Throwable  ], 557:[646:java/lang/Throwable  ], 561:[646:java/lang/Throwable  ], 565:[646:java/lang/Throwable  ], 573:[646:java/lang/Throwable  ], 583:[646:java/lang/Throwable  ], 589:[646:java/lang/Throwable  ], 590:[646:java/lang/Throwable  ], 592:[646:java/lang/Throwable  ], 593:[646:java/lang/Throwable  ], 594:[646:java/lang/Throwable  ], 595:[646:java/lang/Throwable  ], 597:[646:java/lang/Throwable  ], 599:[646:java/lang/Throwable  ], 605:[646:java/lang/Throwable  ], 606:[646:java/lang/Throwable  ], 607:[646:java/lang/Throwable  ], 609:[646:java/lang/Throwable  ], 613:[646:java/lang/Throwable  ], 617:[646:java/lang/Throwable  ], 621:[646:java/lang/Throwable  ], 624:[646:java/lang/Throwable  ], 626:[646:java/lang/Throwable  ], 629:[646:java/lang/Throwable  ], 631:[646:java/lang/Throwable  ], 632:[646:java/lang/Throwable  ], 635:[646:java/lang/Throwable  ], 640:[646:java/lang/Throwable  ], 641:[646:java/lang/Throwable  ], 644:[646:java/lang/Throwable  ], 648:[646:java/lang/Throwable  ]]
 CONST null
 ASTORE 3
 CONST null
```

### `nativeMassTeleportAnswerBody(Llocal/lito381/server/Protocol$AskAnswer;)V`

- 類別：`local/lito381/server/ClientSession`
- 類型：含迴圈（路徑比較器不支援）；事件後繼：`LAYOUT_ONLY`
- 比較器：`method nativeMassTeleportAnswerBody(Llocal/lito381/server/Protocol$AskAnswer;)V insn[5]: left=INVOKESTATIC local/lito381/server/NativeMassTeleportAnswerLife.capture(Llocal/lito381/server/ClientSession;Ljava/lang/Object;)V right=DUP2; paths rejected: normal control-flow cycle; labelled CFGs differ`

```diff
@@ -5,20 +5,19 @@
 ASTORE 2
 ALOAD 0
 CONST null
+DUP2
 INVOKESTATIC local/lito381/server/NativeMassTeleportAnswerLife.capture(Llocal/lito381/server/ClientSession;Ljava/lang/Object;)V
-ALOAD 0
-CONST null
 PUTFIELD local/lito381/server/ClientSession.pendingMassTeleport:Llocal/lito381/server/ClientSession$PendingMassTeleport;
 ALOAD 2
-IFNULL L19
+IFNULL L18
 ALOAD 1
 INVOKEVIRTUAL local/lito381/server/Protocol$AskAnswer.questionId()I
 CONST Integer:748
-IF_ICMPNE L19
+IF_ICMPNE L18
 ALOAD 1
 INVOKEVIRTUAL local/lito381/server/Protocol$AskAnswer.text()Ljava/lang/String;
 INVOKEVIRTUAL java/lang/String.isEmpty()Z
-IFNE L27
+IFNE L26
 GETSTATIC java/lang/System.out:Ljava/io/PrintStream;
 ALOAD 0
…（其餘 hunk 見 scratch/issue-70/r6-b-classified.json）
```

### `nativeMaterialAbsoluteBarrierBody([BLlocal/lito381/server/BattleSpellCatalog$AbsoluteBarrierSpell;)V`

- 類別：`local/lito381/server/ClientSession`
- 類型：分支排列、含迴圈（路徑比較器不支援）；事件後繼：`LAYOUT_ONLY`
- 比較器：`method nativeMaterialAbsoluteBarrierBody([BLlocal/lito381/server/BattleSpellCatalog$AbsoluteBarrierSpell;)V insn[71]: left=IFLT L99 right=IFLT L98; paths rejected: normal control-flow cycle; labelled CFGs differ`

```diff
@@ -71,11 +71,11 @@
 LLOAD 9/10
 LLOAD 11/12
 LCMP
-IFLT L99
-ALOAD 0
-GETFIELD local/lito381/server/ClientSession.playerMp:I
-ILOAD 8
-IF_ICMPLT L99
+IFLT L98
+ALOAD 0
+GETFIELD local/lito381/server/ClientSession.playerMp:I
+ILOAD 8
+IF_ICMPLT L98
 ALOAD 0
 GETFIELD local/lito381/server/ClientSession.phase:Llocal/lito381/server/ClientSession$Phase;
 GETSTATIC local/lito381/server/ClientSession$Phase.BATTLE:Llocal/lito381/server/ClientSession$Phase;
```

### `nativeMaterialAdditionalFireBody([BLlocal/lito381/server/BattleSpellCatalog$AdditionalFireSpell;)V`

- 類別：`local/lito381/server/ClientSession`
- 類型：堆疊／區域變數形狀、含迴圈（路徑比較器不支援）；事件後繼：`LAYOUT_ONLY`
- 比較器：`method nativeMaterialAdditionalFireBody([BLlocal/lito381/server/BattleSpellCatalog$AdditionalFireSpell;)V insn[124]: left=ALOAD 0 right=ALOAD 2; paths rejected: normal control-flow cycle; labelled CFGs differ`

```diff
@@ -124,14 +124,14 @@
 INVOKESTATIC java/lang/Math.max(II)I
 INVOKESTATIC local/lito381/server/BattleSpellCatalog.originalMagicConsumeReduction(II)I
 ISTORE 9
+ALOAD 2
+ILOAD 9
 ALOAD 0
 INVOKEVIRTUAL local/lito381/server/ClientSession.nativeSkillConsumptionIntelligence()I
 ISTORE 10
 ALOAD 0
 INVOKEVIRTUAL local/lito381/server/ClientSession.nativeSkillConsumptionOriginalReduction()I
 ISTORE 11
-ALOAD 2
-ILOAD 9
 ILOAD 10
 INVOKESTATIC local/lito381/server/NativeSkillConsumptionBridges.AdditionalFireSpell_effectiveMpCost_II(Llocal/lito381/server/BattleSpellCatalog$AdditionalFireSpell;II)I
 ISTORE 12
```

### `nativeMaterialAreaSilenceBody([BLlocal/lito381/server/BattleSpellCatalog$AreaSilenceSpell;I)V`

- 類別：`local/lito381/server/ClientSession`
- 類型：分支排列、含迴圈（路徑比較器不支援）；事件後繼：`LAYOUT_ONLY`
- 比較器：`method nativeMaterialAreaSilenceBody([BLlocal/lito381/server/BattleSpellCatalog$AreaSilenceSpell;I)V insn[17]: left=IF_ICMPNE L1010 right=IF_ICMPEQ L19; paths rejected: normal control-flow cycle; labelled CFGs differ`

```diff
@@ -17,7 +17,8 @@
 INVOKEVIRTUAL local/lito381/server/Protocol$SpellSelector.skillId()I
 ALOAD 2
 INVOKEVIRTUAL local/lito381/server/BattleSpellCatalog$AreaSilenceSpell.skillId()I
-IF_ICMPNE L1010
+IF_ICMPEQ L19
+RETURN
 INVOKESTATIC java/lang/System.nanoTime()J
 LSTORE 5/6
 ALOAD 0
```

### `nativeMaterialAreaWindBody([BLlocal/lito381/server/BattleSpellCatalog$WindShackleSpell;I)V`

- 類別：`local/lito381/server/ClientSession`
- 類型：分支排列、含迴圈（路徑比較器不支援）；事件後繼：`LAYOUT_ONLY`
- 比較器：`method nativeMaterialAreaWindBody([BLlocal/lito381/server/BattleSpellCatalog$WindShackleSpell;I)V insn[20]: left=IFEQ L1333 right=IFNE L22; paths rejected: normal control-flow cycle; labelled CFGs differ`

```diff
@@ -20,12 +20,14 @@
 ALOAD 0
 ALOAD 4
 INVOKESTATIC local/lito381/server/NativeSelectedPlayerIntent.attack(Llocal/lito381/server/ClientSession;Llocal/lito381/server/Protocol$AttackSpellRequest;)Z
-IFEQ L1333
+IFNE L22
+RETURN
 ALOAD 4
 INVOKEVIRTUAL local/lito381/server/Protocol$AttackSpellRequest.skillId()I
 ALOAD 2
 INVOKEVIRTUAL local/lito381/server/BattleSpellCatalog$WindShackleSpell.skillId()I
-IF_ICMPNE L1333
+IF_ICMPEQ L28
+RETURN
 INVOKESTATIC java/lang/System.nanoTime()J
 LSTORE 5/6
 ALOAD 0
```

### `nativeMaterialArmorBreakBody([BLlocal/lito381/server/BattleSpellCatalog$ArmorBreakSpell;)V`

- 類別：`local/lito381/server/ClientSession`
- 類型：例外表涵蓋、含迴圈（路徑比較器不支援）；事件後繼：`LAYOUT_ONLY`
- 比較器：`method nativeMaterialArmorBreakBody([BLlocal/lito381/server/BattleSpellCatalog$ArmorBreakSpell;)V: left=147:[152:java/lang/Exception  ] right=149:[154:java/lang/Exception  ]; paths rejected: normal control-flow cycle; labelled CFGs differ`

```diff
@@ -1,5 +1,5 @@
 META 0 null [java/io/IOException]
-TRY [147:[152:java/lang/Exception  ], 149:[152:java/lang/Exception  ]]
+TRY [149:[154:java/lang/Exception  ], 151:[154:java/lang/Exception  ]]
 ALOAD 1
 INVOKESTATIC local/lito381/server/Protocol.selectorOnlySpellRequest([B)Llocal/lito381/server/Protocol$SpellSelector;
 ASTORE 3
```

### `nativeMaterialAwakenBody([BLlocal/lito381/server/BattleSpellCatalog$AwakenSpell;)V`

- 類別：`local/lito381/server/ClientSession`
- 類型：分支排列、含迴圈（路徑比較器不支援）；事件後繼：`LAYOUT_ONLY`
- 比較器：`method nativeMaterialAwakenBody([BLlocal/lito381/server/BattleSpellCatalog$AwakenSpell;)V insn[14]: left=IF_ICMPNE L569 right=IF_ICMPNE L21; paths rejected: normal control-flow cycle; labelled CFGs differ`

```diff
@@ -14,16 +14,17 @@
 INVOKEVIRTUAL local/lito381/server/Protocol$SpellSelector.skillId()I
 ALOAD 2
 INVOKEVIRTUAL local/lito381/server/BattleSpellCatalog$AwakenSpell.skillId()I
-IF_ICMPNE L569
+IF_ICMPNE L21
 ALOAD 5
 GETSTATIC local/lito381/server/ClientSession$Phase.BATTLE:Llocal/lito381/server/ClientSession$Phase;
-IF_ACMPEQ L21
-ALOAD 5
-GETSTATIC local/lito381/server/ClientSession$Phase.WORLD:Llocal/lito381/server/ClientSession$Phase;
-IF_ACMPNE L569
-ALOAD 5
-GETSTATIC local/lito381/server/ClientSession$Phase.WORLD:Llocal/lito381/server/ClientSession$Phase;
-IF_ACMPNE L32
+IF_ACMPEQ L22
+ALOAD 5
+GETSTATIC local/lito381/server/ClientSession$Phase.WORLD:Llocal/lito381/server/ClientSession$Phase;
+IF_ACMPEQ L22
+RETURN
+ALOAD 5
+GETSTATIC local/lito381/server/ClientSession$Phase.WORLD:Llocal/lito381/server/ClientSession$Phase;
+IF_ACMPNE L34
 ALOAD 0
…（其餘 hunk 見 scratch/issue-70/r6-b-classified.json）
```

### `nativeMaterialBlindBody([BLlocal/lito381/server/BattleSpellCatalog$BlindSpell;)V`

- 類別：`local/lito381/server/ClientSession`
- 類型：例外表涵蓋、含迴圈（路徑比較器不支援）；事件後繼：`LAYOUT_ONLY`
- 比較器：`method nativeMaterialBlindBody([BLlocal/lito381/server/BattleSpellCatalog$BlindSpell;)V: left=280:[285:java/lang/Exception  ] right=283:[288:java/lang/Exception  ]; paths rejected: normal control-flow cycle; labelled CFGs differ`

```diff
@@ -1,5 +1,5 @@
 META 0 null [java/io/IOException]
-TRY [280:[285:java/lang/Exception  ], 282:[285:java/lang/Exception  ]]
+TRY [283:[288:java/lang/Exception  ], 285:[288:java/lang/Exception  ]]
 ALOAD 1
 INVOKESTATIC local/lito381/server/Protocol.attackSpellRequest([B)Llocal/lito381/server/Protocol$AttackSpellRequest;
 ALOAD 0
```

### `nativeMaterialBloodlustBody([BLlocal/lito381/server/BattleSpellCatalog$BloodlustSpell;)V`

- 類別：`local/lito381/server/ClientSession`
- 類型：分支排列、含迴圈（路徑比較器不支援）；事件後繼：`LAYOUT_ONLY`
- 比較器：`method nativeMaterialBloodlustBody([BLlocal/lito381/server/BattleSpellCatalog$BloodlustSpell;)V insn[10]: left=IF_ICMPNE L493 right=IF_ICMPNE L17; paths rejected: normal control-flow cycle; labelled CFGs differ`

```diff
@@ -10,16 +10,17 @@
 INVOKEVIRTUAL local/lito381/server/Protocol$SpellSelector.skillId()I
 ALOAD 2
 INVOKEVIRTUAL local/lito381/server/BattleSpellCatalog$BloodlustSpell.skillId()I
-IF_ICMPNE L493
+IF_ICMPNE L17
 ALOAD 4
 GETSTATIC local/lito381/server/ClientSession$Phase.BATTLE:Llocal/lito381/server/ClientSession$Phase;
-IF_ACMPEQ L17
-ALOAD 4
-GETSTATIC local/lito381/server/ClientSession$Phase.WORLD:Llocal/lito381/server/ClientSession$Phase;
-IF_ACMPNE L493
-ALOAD 4
-GETSTATIC local/lito381/server/ClientSession$Phase.WORLD:Llocal/lito381/server/ClientSession$Phase;
-IF_ACMPNE L28
+IF_ACMPEQ L18
+ALOAD 4
+GETSTATIC local/lito381/server/ClientSession$Phase.WORLD:Llocal/lito381/server/ClientSession$Phase;
+IF_ACMPEQ L18
+RETURN
+ALOAD 4
+GETSTATIC local/lito381/server/ClientSession$Phase.WORLD:Llocal/lito381/server/ClientSession$Phase;
+IF_ACMPNE L30
 ALOAD 0
…（其餘 hunk 見 scratch/issue-70/r6-b-classified.json）
```

### `nativeMaterialCallNatureBody([BLlocal/lito381/server/BattleSpellCatalog$ResurrectionSpell;)V`

- 類別：`local/lito381/server/ClientSession`
- 類型：分支排列、含迴圈（路徑比較器不支援）；事件後繼：`LAYOUT_ONLY`
- 比較器：`method nativeMaterialCallNatureBody([BLlocal/lito381/server/BattleSpellCatalog$ResurrectionSpell;)V insn[10]: left=IFEQ L1006 right=IFNE L12; paths rejected: normal control-flow cycle; labelled CFGs differ`

```diff
@@ -10,34 +10,37 @@
 ALOAD 0
 ALOAD 3
 INVOKESTATIC local/lito381/server/NativeSelectedPlayerIntent.object(Llocal/lito381/server/ClientSession;Llocal/lito381/server/Protocol$ObjectSpellRequest;)Z
-IFEQ L1006
+IFNE L12
+RETURN
 ALOAD 3
 INVOKEVIRTUAL local/lito381/server/Protocol$ObjectSpellRequest.skillId()I
 CONST Integer:165
-IF_ICMPNE L1006
+IF_ICMPNE L21
 ALOAD 3
 INVOKEVIRTUAL local/lito381/server/Protocol$ObjectSpellRequest.skillId()I
 ALOAD 2
 INVOKEVIRTUAL local/lito381/server/BattleSpellCatalog$ResurrectionSpell.skillId()I
-IF_ICMPNE L1006
+IF_ICMPEQ L22
+RETURN
 ALOAD 0
 GETFIELD local/lito381/server/ClientSession.phase:Llocal/lito381/server/ClientSession$Phase;
 ASTORE 4
 ALOAD 4
 GETSTATIC local/lito381/server/ClientSession$Phase.BATTLE:Llocal/lito381/server/ClientSession$Phase;
…（其餘 hunk 見 scratch/issue-70/r6-b-classified.json）
```

### `nativeMaterialCancellationBody([BLlocal/lito381/server/BattleSpellCatalog$CancellationSpell;Llocal/lito381/server/CombatPoisonLifecycle$Scope;)V`

- 類別：`local/lito381/server/ClientSession`
- 類型：分支排列、含迴圈（路徑比較器不支援）；事件後繼：`LAYOUT_ONLY`
- 比較器：`method nativeMaterialCancellationBody([BLlocal/lito381/server/BattleSpellCatalog$CancellationSpell;Llocal/lito381/server/CombatPoisonLifecycle$Scope;)V insn[12]: left=IFEQ L1153 right=IFNE L14; paths rejected: normal control-flow cycle; labelled CFGs differ`

```diff
@@ -12,7 +12,8 @@
 ALOAD 0
 ALOAD 5
 INVOKESTATIC local/lito381/server/NativeSelectedPlayerIntent.object(Llocal/lito381/server/ClientSession;Llocal/lito381/server/Protocol$ObjectSpellRequest;)Z
-IFEQ L1153
+IFNE L14
+RETURN
 ALOAD 0
 GETFIELD local/lito381/server/ClientSession.hub:Llocal/lito381/server/GameHub;
 ALOAD 0
```

### `nativeMaterialClearMindBody([BLlocal/lito381/server/BattleSpellCatalog$ClearMindSpell;)V`

- 類別：`local/lito381/server/ClientSession`
- 類型：堆疊／區域變數形狀、含迴圈（路徑比較器不支援）；事件後繼：`LAYOUT_ONLY`
- 比較器：`method nativeMaterialClearMindBody([BLlocal/lito381/server/BattleSpellCatalog$ClearMindSpell;)V insn[123]: left=ALOAD 0 right=ALOAD 2; paths rejected: normal control-flow cycle; labelled CFGs differ`

```diff
@@ -123,14 +123,14 @@
 INVOKESTATIC java/lang/Math.max(II)I
 INVOKESTATIC local/lito381/server/BattleSpellCatalog.originalMagicConsumeReduction(II)I
 ISTORE 8
+ALOAD 2
+ILOAD 8
 ALOAD 0
 INVOKEVIRTUAL local/lito381/server/ClientSession.nativeSkillConsumptionIntelligence()I
 ISTORE 9
 ALOAD 0
 INVOKEVIRTUAL local/lito381/server/ClientSession.nativeSkillConsumptionOriginalReduction()I
 ISTORE 10
-ALOAD 2
-ILOAD 8
 ILOAD 9
 INVOKESTATIC local/lito381/server/NativeSkillConsumptionBridges.ClearMindSpell_effectiveMpCost_II(Llocal/lito381/server/BattleSpellCatalog$ClearMindSpell;II)I
 ISTORE 11
```

### `nativeMaterialCounterMagicBody([BLlocal/lito381/server/BattleSpellCatalog$CounterMagicSpell;)V`

- 類別：`local/lito381/server/ClientSession`
- 類型：堆疊／區域變數形狀、含迴圈（路徑比較器不支援）；事件後繼：`LAYOUT_ONLY`
- 比較器：`method nativeMaterialCounterMagicBody([BLlocal/lito381/server/BattleSpellCatalog$CounterMagicSpell;)V insn[120]: left=ALOAD 0 right=ALOAD 2; paths rejected: normal control-flow cycle; labelled CFGs differ`

```diff
@@ -120,14 +120,14 @@
 INVOKESTATIC java/lang/Math.max(II)I
 INVOKESTATIC local/lito381/server/BattleSpellCatalog.originalMagicConsumeReduction(II)I
 ISTORE 10
+ALOAD 2
+ILOAD 10
 ALOAD 0
 INVOKEVIRTUAL local/lito381/server/ClientSession.nativeSkillConsumptionIntelligence()I
 ISTORE 11
 ALOAD 0
 INVOKEVIRTUAL local/lito381/server/ClientSession.nativeSkillConsumptionOriginalReduction()I
 ISTORE 12
-ALOAD 2
-ILOAD 10
 ILOAD 11
 INVOKESTATIC local/lito381/server/NativeSkillConsumptionBridges.CounterMagicSpell_effectiveMpCost_II(Llocal/lito381/server/BattleSpellCatalog$CounterMagicSpell;II)I
 ISTORE 13
```

### `nativeMaterialCounterMirrorBody([BLlocal/lito381/server/BattleSpellCatalog$CounterMirrorSpell;)V`

- 類別：`local/lito381/server/ClientSession`
- 類型：堆疊／區域變數形狀、含迴圈（路徑比較器不支援）；事件後繼：`LAYOUT_ONLY`
- 比較器：`method nativeMaterialCounterMirrorBody([BLlocal/lito381/server/BattleSpellCatalog$CounterMirrorSpell;)V insn[123]: left=ALOAD 0 right=ALOAD 2; paths rejected: normal control-flow cycle; labelled CFGs differ`

```diff
@@ -123,14 +123,14 @@
 INVOKESTATIC java/lang/Math.max(II)I
 INVOKESTATIC local/lito381/server/BattleSpellCatalog.originalMagicConsumeReduction(II)I
 ISTORE 8
+ALOAD 2
+ILOAD 8
 ALOAD 0
 INVOKEVIRTUAL local/lito381/server/ClientSession.nativeSkillConsumptionIntelligence()I
 ISTORE 9
 ALOAD 0
 INVOKEVIRTUAL local/lito381/server/ClientSession.nativeSkillConsumptionOriginalReduction()I
 ISTORE 10
-ALOAD 2
-ILOAD 8
 ILOAD 9
 INVOKESTATIC local/lito381/server/NativeSkillConsumptionBridges.CounterMirrorSpell_effectiveMpCost_II(Llocal/lito381/server/BattleSpellCatalog$CounterMirrorSpell;II)I
 ISTORE 11
```

### `nativeMaterialDecayPotionBody([BLlocal/lito381/server/BattleSpellCatalog$DecayPotionSpell;)V`

- 類別：`local/lito381/server/ClientSession`
- 類型：例外表涵蓋、含迴圈（路徑比較器不支援）；事件後繼：`LAYOUT_ONLY`
- 比較器：`method nativeMaterialDecayPotionBody([BLlocal/lito381/server/BattleSpellCatalog$DecayPotionSpell;)V: left=239:[244:java/lang/Exception  ] right=241:[246:java/lang/Exception  ]; paths rejected: normal control-flow cycle; labelled CFGs differ`

```diff
@@ -1,5 +1,5 @@
 META 0 null [java/io/IOException]
-TRY [239:[244:java/lang/Exception  ], 241:[244:java/lang/Exception  ]]
+TRY [241:[246:java/lang/Exception  ], 243:[246:java/lang/Exception  ]]
 ALOAD 1
 INVOKESTATIC local/lito381/server/Protocol.attackSpellRequest([B)Llocal/lito381/server/Protocol$AttackSpellRequest;
 ALOAD 0
```

### `nativeMaterialEarthBindBody([BLlocal/lito381/server/BattleSpellCatalog$BindSpell;)V`

- 類別：`local/lito381/server/ClientSession`
- 類型：分支排列、含迴圈（路徑比較器不支援）；事件後繼：`LAYOUT_ONLY`
- 比較器：`method nativeMaterialEarthBindBody([BLlocal/lito381/server/BattleSpellCatalog$BindSpell;)V insn[10]: left=IFEQ L763 right=IFNE L12; paths rejected: normal control-flow cycle; labelled CFGs differ`

```diff
@@ -10,12 +10,14 @@
 ALOAD 0
 ALOAD 3
 INVOKESTATIC local/lito381/server/NativeSelectedPlayerIntent.attack(Llocal/lito381/server/ClientSession;Llocal/lito381/server/Protocol$AttackSpellRequest;)Z
-IFEQ L763
+IFNE L12
+RETURN
 ALOAD 3
 INVOKEVIRTUAL local/lito381/server/Protocol$AttackSpellRequest.skillId()I
 ALOAD 2
 INVOKEVIRTUAL local/lito381/server/BattleSpellCatalog$BindSpell.skillId()I
-IF_ICMPNE L763
+IF_ICMPEQ L18
+RETURN
 ALOAD 0
 GETFIELD local/lito381/server/ClientSession.hub:Llocal/lito381/server/GameHub;
 ALOAD 0
```

### `nativeMaterialElementalProtectionBody([BLlocal/lito381/server/BattleSpellCatalog$ElementalProtectionSpell;)V`

- 類別：`local/lito381/server/ClientSession`
- 類型：堆疊／區域變數形狀、含迴圈（路徑比較器不支援）；事件後繼：`LAYOUT_ONLY`
- 比較器：`method nativeMaterialElementalProtectionBody([BLlocal/lito381/server/BattleSpellCatalog$ElementalProtectionSpell;)V insn[134]: left=ALOAD 0 right=ALOAD 2; paths rejected: normal control-flow cycle; labelled CFGs differ`

```diff
@@ -134,14 +134,14 @@
 INVOKESTATIC java/lang/Math.max(II)I
 INVOKESTATIC local/lito381/server/BattleSpellCatalog.originalMagicConsumeReduction(II)I
 ISTORE 8
+ALOAD 2
+ILOAD 8
 ALOAD 0
 INVOKEVIRTUAL local/lito381/server/ClientSession.nativeSkillConsumptionIntelligence()I
 ISTORE 9
 ALOAD 0
 INVOKEVIRTUAL local/lito381/server/ClientSession.nativeSkillConsumptionOriginalReduction()I
 ISTORE 10
-ALOAD 2
-ILOAD 8
 ILOAD 9
 INVOKESTATIC local/lito381/server/NativeSkillConsumptionBridges.ElementalProtectionSpell_effectiveMpCost_II(Llocal/lito381/server/BattleSpellCatalog$ElementalProtectionSpell;II)I
 ISTORE 11
```

### `nativeMaterialElementalSummonBody([BLlocal/lito381/server/BattleSpellCatalog$ElementalSummonSpell;)V`

- 類別：`local/lito381/server/ClientSession`
- 類型：例外表涵蓋、含迴圈（路徑比較器不支援）；事件後繼：`LAYOUT_ONLY`
- 比較器：`method nativeMaterialElementalSummonBody([BLlocal/lito381/server/BattleSpellCatalog$ElementalSummonSpell;)V: left=315:[325:java/lang/Exception  ] right=322:[332:java/lang/Exception  ]; paths rejected: normal control-flow cycle; labelled CFGs differ`

```diff
@@ -1,5 +1,5 @@
 META 0 null [java/io/IOException]
-TRY [315:[325:java/lang/Exception  ], 317:[325:java/lang/Exception  ], 320:[325:java/lang/Exception  ], 322:[325:java/lang/Exception  ], 420:[427:java/lang/Throwable  ], 422:[427:java/lang/Throwable  ], 425:[427:java/lang/Throwable  ], 429:[427:java/lang/Throwable  ]]
+TRY [322:[332:java/lang/Exception  ], 324:[332:java/lang/Exception  ], 327:[332:java/lang/Exception  ], 329:[332:java/lang/Exception  ], 427:[434:java/lang/Throwable  ], 429:[434:java/lang/Throwable  ], 432:[434:java/lang/Throwable  ], 436:[434:java/lang/Throwable  ]]
 ALOAD 1
 INVOKESTATIC local/lito381/server/Protocol.selectorOnlySpellRequest([B)Llocal/lito381/server/Protocol$SpellSelector;
 ASTORE 3
```

### `nativeMaterialEnchantVenomBody([BLlocal/lito381/server/BattleSpellCatalog$EnchantVenomSpell;)V`

- 類別：`local/lito381/server/ClientSession`
- 類型：堆疊／區域變數形狀、含迴圈（路徑比較器不支援）；事件後繼：`LAYOUT_ONLY`
- 比較器：`method nativeMaterialEnchantVenomBody([BLlocal/lito381/server/BattleSpellCatalog$EnchantVenomSpell;)V insn[49]: left=ALOAD 0 right=ALOAD 2; paths rejected: normal control-flow cycle; labelled CFGs differ`

```diff
@@ -49,14 +49,14 @@
 INVOKESTATIC java/lang/Math.max(II)I
 INVOKESTATIC local/lito381/server/BattleSpellCatalog.originalMagicConsumeReduction(II)I
 ISTORE 5
+ALOAD 2
+ILOAD 5
 ALOAD 0
 INVOKEVIRTUAL local/lito381/server/ClientSession.nativeSkillConsumptionIntelligence()I
 ISTORE 6
 ALOAD 0
 INVOKEVIRTUAL local/lito381/server/ClientSession.nativeSkillConsumptionOriginalReduction()I
 ISTORE 7
-ALOAD 2
-ILOAD 5
 ILOAD 6
 INVOKESTATIC local/lito381/server/NativeSkillConsumptionBridges.EnchantVenomSpell_effectiveMpCost_II(Llocal/lito381/server/BattleSpellCatalog$EnchantVenomSpell;II)I
 ISTORE 8
```

### `nativeMaterialEntangleBody([BLlocal/lito381/server/BattleSpellCatalog$EntangleSpell;I)V`

- 類別：`local/lito381/server/ClientSession`
- 類型：例外表涵蓋、含迴圈（路徑比較器不支援）；事件後繼：`LAYOUT_ONLY`
- 比較器：`method nativeMaterialEntangleBody([BLlocal/lito381/server/BattleSpellCatalog$EntangleSpell;I)V: left=604:[609:java/lang/Exception  ] right=607:[612:java/lang/Exception  ]; paths rejected: normal control-flow cycle; labelled CFGs differ`

```diff
@@ -1,5 +1,5 @@
 META 0 null [java/io/IOException]
-TRY [604:[609:java/lang/Exception  ], 606:[609:java/lang/Exception  ]]
+TRY [607:[612:java/lang/Exception  ], 609:[612:java/lang/Exception  ]]
 ILOAD 3
 IFLT L5
 ILOAD 3
```

### `nativeMaterialEraseBody([BLlocal/lito381/server/BattleSpellCatalog$EraseMagicSpell;I)V`

- 類別：`local/lito381/server/ClientSession`
- 類型：例外表涵蓋、含迴圈（路徑比較器不支援）；事件後繼：`LAYOUT_ONLY`
- 比較器：`method nativeMaterialEraseBody([BLlocal/lito381/server/BattleSpellCatalog$EraseMagicSpell;I)V: left=608:[613:java/lang/Exception  ] right=611:[616:java/lang/Exception  ]; paths rejected: normal control-flow cycle; labelled CFGs differ`

```diff
@@ -1,5 +1,5 @@
 META 0 null [java/io/IOException]
-TRY [608:[613:java/lang/Exception  ], 610:[613:java/lang/Exception  ]]
+TRY [611:[616:java/lang/Exception  ], 613:[616:java/lang/Exception  ]]
 ILOAD 3
 IFLT L5
 ILOAD 3
```

### `nativeMaterialExoticVitalizeBody([BLlocal/lito381/server/BattleSpellCatalog$ExoticVitalizeSpell;)V`

- 類別：`local/lito381/server/ClientSession`
- 類型：堆疊／區域變數形狀、含迴圈（路徑比較器不支援）；事件後繼：`LAYOUT_ONLY`
- 比較器：`method nativeMaterialExoticVitalizeBody([BLlocal/lito381/server/BattleSpellCatalog$ExoticVitalizeSpell;)V insn[124]: left=ALOAD 0 right=ALOAD 2; paths rejected: normal control-flow cycle; labelled CFGs differ`

```diff
@@ -124,14 +124,14 @@
 INVOKESTATIC java/lang/Math.max(II)I
 INVOKESTATIC local/lito381/server/BattleSpellCatalog.originalMagicConsumeReduction(II)I
 ISTORE 9
+ALOAD 2
+ILOAD 9
 ALOAD 0
 INVOKEVIRTUAL local/lito381/server/ClientSession.nativeSkillConsumptionIntelligence()I
 ISTORE 10
 ALOAD 0
 INVOKEVIRTUAL local/lito381/server/ClientSession.nativeSkillConsumptionOriginalReduction()I
 ISTORE 11
-ALOAD 2
-ILOAD 9
 ILOAD 10
 INVOKESTATIC local/lito381/server/NativeSkillConsumptionBridges.ExoticVitalizeSpell_effectiveMpCost_II(Llocal/lito381/server/BattleSpellCatalog$ExoticVitalizeSpell;II)I
 ISTORE 12
```

### `nativeMaterialGroundBody([BLlocal/lito381/server/BattleSpellCatalog$GroundSpell;)V`

- 類別：`local/lito381/server/ClientSession`
- 類型：分支排列、含迴圈（路徑比較器不支援）；事件後繼：`LAYOUT_ONLY`
- 比較器：`method nativeMaterialGroundBody([BLlocal/lito381/server/BattleSpellCatalog$GroundSpell;)V insn[7]: left=IF_ICMPNE L387 right=IF_ICMPEQ L9; paths rejected: normal control-flow cycle; labelled CFGs differ`

```diff
@@ -7,7 +7,8 @@
 INVOKEVIRTUAL local/lito381/server/Protocol$GroundSpellRequest.skillId()I
 ALOAD 2
 INVOKEVIRTUAL local/lito381/server/BattleSpellCatalog$GroundSpell.skillId()I
-IF_ICMPNE L387
+IF_ICMPEQ L9
+RETURN
 INVOKESTATIC local/lito381/server/BattleAnimationCatalog.get()Llocal/lito381/server/BattleAnimationCatalog;
 ASTORE 4
 ALOAD 4
```

### `nativeMaterialInvisibilityBody([BLlocal/lito381/server/BattleSpellCatalog$InvisibilitySpell;)V`

- 類別：`local/lito381/server/ClientSession`
- 類型：堆疊／區域變數形狀、含迴圈（路徑比較器不支援）；事件後繼：`LAYOUT_ONLY`
- 比較器：`method nativeMaterialInvisibilityBody([BLlocal/lito381/server/BattleSpellCatalog$InvisibilitySpell;)V insn[147]: left=ALOAD 0 right=DUP; paths rejected: normal control-flow cycle; labelled CFGs differ`

```diff
@@ -147,12 +147,14 @@
 CONST Integer:1
 ISTORE 13
 ALOAD 0
-ALOAD 0
-ALOAD 0
+DUP
 GETFIELD local/lito381/server/ClientSession.playerMp:I
 ILOAD 11
 ISUB
+DUP2
 INVOKESTATIC local/lito381/server/NativeSkillConsumption.boundMp(Llocal/lito381/server/ClientSession;I)I
+SWAP
+POP
 PUTFIELD local/lito381/server/ClientSession.playerMp:I
 ALOAD 0
 GETFIELD local/lito381/server/ClientSession.playerMp:I
```

### `nativeMaterialItemWeaponEnchantBody([BLlocal/lito381/server/BattleSpellCatalog$ItemWeaponEnchantSpell;)V`

- 類別：`local/lito381/server/ClientSession`
- 類型：堆疊／區域變數形狀、含迴圈（路徑比較器不支援）；事件後繼：`LAYOUT_ONLY`
- 比較器：`method nativeMaterialItemWeaponEnchantBody([BLlocal/lito381/server/BattleSpellCatalog$ItemWeaponEnchantSpell;)V insn[125]: left=ALOAD 0 right=INVOKESTATIC java/lang/System.nanoTime()J; paths rejected: normal control-flow cycle; labelled CFGs differ`

```diff
@@ -125,10 +125,8 @@
 ISTORE 17
 CONST Integer:0
 ISTORE 18
-ALOAD 0
-ASTORE 19
 INVOKESTATIC java/lang/System.nanoTime()J
-LSTORE 20/21
+LSTORE 19/20
 ALOAD 0
 GETFIELD local/lito381/server/ClientSession.spellReadyNanos:Ljava/util/Map;
 ALOAD 2
```

### `nativeMaterialMagicResistanceBody([BLlocal/lito381/server/BattleSpellCatalog$MagicResistanceSpell;)V`

- 類別：`local/lito381/server/ClientSession`
- 類型：分支排列、含迴圈（路徑比較器不支援）；事件後繼：`LAYOUT_ONLY`
- 比較器：`method nativeMaterialMagicResistanceBody([BLlocal/lito381/server/BattleSpellCatalog$MagicResistanceSpell;)V insn[71]: left=IFLT L91 right=IFLT L90; paths rejected: normal control-flow cycle; labelled CFGs differ`

```diff
@@ -71,11 +71,11 @@
 LLOAD 9/10
 LLOAD 11/12
 LCMP
-IFLT L91
-ALOAD 0
-GETFIELD local/lito381/server/ClientSession.playerMp:I
-ILOAD 8
-IF_ICMPLT L91
+IFLT L90
+ALOAD 0
+GETFIELD local/lito381/server/ClientSession.playerMp:I
+ILOAD 8
+IF_ICMPLT L90
 ALOAD 0
 GETFIELD local/lito381/server/ClientSession.phase:Llocal/lito381/server/ClientSession$Phase;
 GETSTATIC local/lito381/server/ClientSession$Phase.BATTLE:Llocal/lito381/server/ClientSession$Phase;
```

### `nativeMaterialMagicalWeaponBody([BLlocal/lito381/server/BattleSpellCatalog$MagicalWeaponSpell;)V`

- 類別：`local/lito381/server/ClientSession`
- 類型：分支排列、含迴圈（路徑比較器不支援）；事件後繼：`LAYOUT_ONLY`
- 比較器：`method nativeMaterialMagicalWeaponBody([BLlocal/lito381/server/BattleSpellCatalog$MagicalWeaponSpell;)V insn[10]: left=IFEQ L418 right=IFNE L12; paths rejected: normal control-flow cycle; labelled CFGs differ`

```diff
@@ -10,7 +10,8 @@
 ALOAD 0
 ALOAD 3
 INVOKESTATIC local/lito381/server/NativeSelectedPlayerIntent.object(Llocal/lito381/server/ClientSession;Llocal/lito381/server/Protocol$ObjectSpellRequest;)Z
-IFEQ L418
+IFNE L12
+RETURN
 ALOAD 0
 GETFIELD local/lito381/server/ClientSession.phase:Llocal/lito381/server/ClientSession$Phase;
 ASTORE 4
```

### `nativeMaterialMassTeleportBody([BLlocal/lito381/server/BattleSpellCatalog$MassTeleportSpell;)V`

- 類別：`local/lito381/server/ClientSession`
- 類型：分支排列、含迴圈（路徑比較器不支援）；事件後繼：`LAYOUT_ONLY`
- 比較器：`method nativeMaterialMassTeleportBody([BLlocal/lito381/server/BattleSpellCatalog$MassTeleportSpell;)V insn[14]: left=IFEQ L23 right=IFEQ L24; paths rejected: normal control-flow cycle; labelled CFGs differ`

```diff
@@ -14,7 +14,7 @@
 CONST Integer:0
 ISTORE 5
 ILOAD 5
-IFEQ L23
+IFEQ L24
 ALOAD 0
 ALOAD 2
 INVOKEVIRTUAL local/lito381/server/BattleSpellCatalog$MassTeleportSpell.skillId()I
```

### `nativeMaterialParalysisScopeBody([BLlocal/lito381/server/BattleSpellCatalog$ParalysisSpell;Llocal/lito381/server/CombatPoisonLifecycle$Scope;)V`

- 類別：`local/lito381/server/ClientSession`
- 類型：例外表涵蓋、含迴圈（路徑比較器不支援）；事件後繼：`LAYOUT_ONLY`
- 比較器：`method nativeMaterialParalysisScopeBody([BLlocal/lito381/server/BattleSpellCatalog$ParalysisSpell;Llocal/lito381/server/CombatPoisonLifecycle$Scope;)V: left=229:[234:java/lang/Exception  ] right=231:[236:java/lang/Exception  ]; paths rejected: normal control-flow cycle; labelled CFGs differ`

```diff
@@ -1,5 +1,5 @@
 META 0 null [java/io/IOException]
-TRY [229:[234:java/lang/Exception  ], 231:[234:java/lang/Exception  ]]
+TRY [231:[236:java/lang/Exception  ], 233:[236:java/lang/Exception  ]]
 ALOAD 1
 INVOKESTATIC local/lito381/server/Protocol.attackSpellRequest([B)Llocal/lito381/server/Protocol$AttackSpellRequest;
 ALOAD 0
```

### `nativeMaterialPhysicalProcBuffBody([BLlocal/lito381/server/BattleSpellCatalog$PhysicalProcBuffSpell;)V`

- 類別：`local/lito381/server/ClientSession`
- 類型：堆疊／區域變數形狀、含迴圈（路徑比較器不支援）；事件後繼：`LAYOUT_ONLY`
- 比較器：`method nativeMaterialPhysicalProcBuffBody([BLlocal/lito381/server/BattleSpellCatalog$PhysicalProcBuffSpell;)V insn[333]: left=ALOAD 0 right=DUP; paths rejected: normal control-flow cycle; labelled CFGs differ`

```diff
@@ -333,12 +333,14 @@
 INVOKESPECIAL java/lang/IllegalStateException.<init>(Ljava/lang/String;)V
 ATHROW
 ALOAD 0
-ALOAD 0
-ALOAD 0
+DUP
 GETFIELD local/lito381/server/ClientSession.playerMp:I
 ILOAD 12
 ISUB
+DUP2
 INVOKESTATIC local/lito381/server/NativeSkillConsumption.boundMp(Llocal/lito381/server/ClientSession;I)I
+SWAP
+POP
 PUTFIELD local/lito381/server/ClientSession.playerMp:I
 ALOAD 0
 GETFIELD local/lito381/server/ClientSession.playerMp:I
```

### `nativeMaterialPoisonScopeBody([BLlocal/lito381/server/BattleSpellCatalog$PoisonSpell;Llocal/lito381/server/CombatPoisonLifecycle$Scope;)V`

- 類別：`local/lito381/server/ClientSession`
- 類型：例外表涵蓋、含迴圈（路徑比較器不支援）；事件後繼：`LAYOUT_ONLY`
- 比較器：`method nativeMaterialPoisonScopeBody([BLlocal/lito381/server/BattleSpellCatalog$PoisonSpell;Llocal/lito381/server/CombatPoisonLifecycle$Scope;)V: left=211:[216:java/lang/Exception  ] right=213:[218:java/lang/Exception  ]; paths rejected: normal control-flow cycle; labelled CFGs differ`

```diff
@@ -1,5 +1,5 @@
 META 0 null [java/io/IOException]
-TRY [211:[216:java/lang/Exception  ], 213:[216:java/lang/Exception  ]]
+TRY [213:[218:java/lang/Exception  ], 215:[218:java/lang/Exception  ]]
 ALOAD 1
 INVOKESTATIC local/lito381/server/Protocol.attackSpellRequest([B)Llocal/lito381/server/Protocol$AttackSpellRequest;
 ALOAD 0
```

### `nativeMaterialPolymorphBody([BLlocal/lito381/server/BattleSpellCatalog$PolymorphSpell;)V`

- 類別：`local/lito381/server/ClientSession`
- 類型：分支排列、含迴圈（路徑比較器不支援）；事件後繼：`LAYOUT_ONLY`
- 比較器：`method nativeMaterialPolymorphBody([BLlocal/lito381/server/BattleSpellCatalog$PolymorphSpell;)V insn[10]: left=IFEQ L698 right=IFNE L12; paths rejected: normal control-flow cycle; labelled CFGs differ`

```diff
@@ -10,7 +10,8 @@
 ALOAD 0
 ALOAD 3
 INVOKESTATIC local/lito381/server/NativeSelectedPlayerIntent.object(Llocal/lito381/server/ClientSession;Llocal/lito381/server/Protocol$ObjectSpellRequest;)Z
-IFEQ L698
+IFNE L12
+RETURN
 ALOAD 0
 GETFIELD local/lito381/server/ClientSession.phase:Llocal/lito381/server/ClientSession$Phase;
 ASTORE 4
```

### `nativeMaterialProtectionBody([BLlocal/lito381/server/BattleSpellCatalog$DamageProtectionSpell;)V`

- 類別：`local/lito381/server/ClientSession`
- 類型：分支排列、含迴圈（路徑比較器不支援）；事件後繼：`LAYOUT_ONLY`
- 比較器：`method nativeMaterialProtectionBody([BLlocal/lito381/server/BattleSpellCatalog$DamageProtectionSpell;)V insn[10]: left=IFEQ L598 right=IFNE L12; paths rejected: normal control-flow cycle; labelled CFGs differ`

```diff
@@ -10,7 +10,8 @@
 ALOAD 0
 ALOAD 3
 INVOKESTATIC local/lito381/server/NativeSelectedPlayerIntent.object(Llocal/lito381/server/ClientSession;Llocal/lito381/server/Protocol$ObjectSpellRequest;)Z
-IFEQ L598
+IFNE L12
+RETURN
 ALOAD 0
 GETFIELD local/lito381/server/ClientSession.phase:Llocal/lito381/server/ClientSession$Phase;
 ASTORE 4
```

### `nativeMaterialResistElementalBody([BLlocal/lito381/server/BattleSpellCatalog$ResistElementalSpell;)V`

- 類別：`local/lito381/server/ClientSession`
- 類型：堆疊／區域變數形狀、含迴圈（路徑比較器不支援）；事件後繼：`LAYOUT_ONLY`
- 比較器：`method nativeMaterialResistElementalBody([BLlocal/lito381/server/BattleSpellCatalog$ResistElementalSpell;)V insn[123]: left=ALOAD 0 right=ALOAD 2; paths rejected: normal control-flow cycle; labelled CFGs differ`

```diff
@@ -123,14 +123,14 @@
 INVOKESTATIC java/lang/Math.max(II)I
 INVOKESTATIC local/lito381/server/BattleSpellCatalog.originalMagicConsumeReduction(II)I
 ISTORE 8
+ALOAD 2
+ILOAD 8
 ALOAD 0
 INVOKEVIRTUAL local/lito381/server/ClientSession.nativeSkillConsumptionIntelligence()I
 ISTORE 9
 ALOAD 0
 INVOKEVIRTUAL local/lito381/server/ClientSession.nativeSkillConsumptionOriginalReduction()I
 ISTORE 10
-ALOAD 2
-ILOAD 8
 ILOAD 9
 INVOKESTATIC local/lito381/server/NativeSkillConsumptionBridges.ResistElementalSpell_effectiveMpCost_II(Llocal/lito381/server/BattleSpellCatalog$ResistElementalSpell;II)I
 ISTORE 11
```

### `nativeMaterialResurrectionBody([BLlocal/lito381/server/BattleSpellCatalog$ResurrectionSpell;)V`

- 類別：`local/lito381/server/ClientSession`
- 類型：分支排列、含迴圈（路徑比較器不支援）；事件後繼：`LAYOUT_ONLY`
- 比較器：`method nativeMaterialResurrectionBody([BLlocal/lito381/server/BattleSpellCatalog$ResurrectionSpell;)V insn[10]: left=IFEQ L611 right=IFNE L12; paths rejected: normal control-flow cycle; labelled CFGs differ`

```diff
@@ -10,12 +10,14 @@
 ALOAD 0
 ALOAD 3
 INVOKESTATIC local/lito381/server/NativeSelectedPlayerIntent.object(Llocal/lito381/server/ClientSession;Llocal/lito381/server/Protocol$ObjectSpellRequest;)Z
-IFEQ L611
+IFNE L12
+RETURN
 ALOAD 3
 INVOKEVIRTUAL local/lito381/server/Protocol$ObjectSpellRequest.skillId()I
 ALOAD 2
 INVOKEVIRTUAL local/lito381/server/BattleSpellCatalog$ResurrectionSpell.skillId()I
-IF_ICMPNE L611
+IF_ICMPEQ L18
+RETURN
 ALOAD 0
 GETFIELD local/lito381/server/ClientSession.hub:Llocal/lito381/server/GameHub;
 ALOAD 0
```

### `nativeMaterialReturnNatureBody([BLlocal/lito381/server/BattleSpellCatalog$ReturnToNatureSpell;I)V`

- 類別：`local/lito381/server/ClientSession`
- 類型：例外表涵蓋、含迴圈（路徑比較器不支援）；事件後繼：`LAYOUT_ONLY`
- 比較器：`method nativeMaterialReturnNatureBody([BLlocal/lito381/server/BattleSpellCatalog$ReturnToNatureSpell;I)V: left=346:[352:java/lang/Exception  ] right=348:[354:java/lang/Exception  ]; paths rejected: normal control-flow cycle; labelled CFGs differ`

```diff
@@ -1,5 +1,5 @@
 META 0 null [java/io/IOException]
-TRY [346:[352:java/lang/Exception  ], 348:[352:java/lang/Exception  ], 349:[352:java/lang/Exception  ]]
+TRY [348:[354:java/lang/Exception  ], 350:[354:java/lang/Exception  ], 351:[354:java/lang/Exception  ]]
 CONST null
 ASTORE 4
 ILOAD 3
```

### `nativeMaterialSilenceBody([BLlocal/lito381/server/BattleSpellCatalog$SilenceSpell;)V`

- 類別：`local/lito381/server/ClientSession`
- 類型：例外表涵蓋、含迴圈（路徑比較器不支援）；事件後繼：`LAYOUT_ONLY`
- 比較器：`method nativeMaterialSilenceBody([BLlocal/lito381/server/BattleSpellCatalog$SilenceSpell;)V: left=239:[244:java/lang/Exception  ] right=241:[246:java/lang/Exception  ]; paths rejected: normal control-flow cycle; labelled CFGs differ`

```diff
@@ -1,5 +1,5 @@
 META 0 null [java/io/IOException]
-TRY [239:[244:java/lang/Exception  ], 241:[244:java/lang/Exception  ]]
+TRY [241:[246:java/lang/Exception  ], 243:[246:java/lang/Exception  ]]
 ALOAD 1
 INVOKESTATIC local/lito381/server/Protocol.attackSpellRequest([B)Llocal/lito381/server/Protocol$AttackSpellRequest;
 ALOAD 0
```

### `nativeMaterialSummonBody([BLlocal/lito381/server/BattleSpellCatalog$SummonSpell;)V`

- 類別：`local/lito381/server/ClientSession`
- 類型：例外表涵蓋、含迴圈（路徑比較器不支援）；事件後繼：`LAYOUT_ONLY`
- 比較器：`method nativeMaterialSummonBody([BLlocal/lito381/server/BattleSpellCatalog$SummonSpell;)V: left=114:[126:java/lang/Exception  ] right=116:[128:java/lang/Exception  ]; paths rejected: normal control-flow cycle; labelled CFGs differ`

```diff
@@ -1,5 +1,5 @@
 META 0 null [java/io/IOException]
-TRY [114:[126:java/lang/Exception  ], 116:[126:java/lang/Exception  ], 117:[126:java/lang/Exception  ], 120:[126:java/lang/Exception  ], 122:[126:java/lang/Exception  ], 123:[126:java/lang/Exception  ], 594:[601:java/lang/Throwable  ], 596:[601:java/lang/Throwable  ], 599:[601:java/lang/Throwable  ], 603:[601:java/lang/Throwable  ]]
+TRY [116:[128:java/lang/Exception  ], 118:[128:java/lang/Exception  ], 119:[128:java/lang/Exception  ], 122:[128:java/lang/Exception  ], 124:[128:java/lang/Exception  ], 125:[128:java/lang/Exception  ], 621:[628:java/lang/Throwable  ], 623:[628:java/lang/Throwable  ], 626:[628:java/lang/Throwable  ], 630:[628:java/lang/Throwable  ]]
 ALOAD 1
 INVOKESTATIC local/lito381/server/Protocol.summonSpellRequest([B)Llocal/lito381/server/Protocol$SummonSpellRequest;
 ASTORE 3
```

### `nativeMaterialTamingBody([BLlocal/lito381/server/BattleSpellCatalog$TamingSpell;)V`

- 類別：`local/lito381/server/ClientSession`
- 類型：例外表涵蓋、含迴圈（路徑比較器不支援）；事件後繼：`LAYOUT_ONLY`
- 比較器：`method nativeMaterialTamingBody([BLlocal/lito381/server/BattleSpellCatalog$TamingSpell;)V: left=205:[210:java/lang/Exception  ] right=207:[212:java/lang/Exception  ]; paths rejected: normal control-flow cycle; labelled CFGs differ`

```diff
@@ -1,5 +1,5 @@
 META 0 null [java/io/IOException]
-TRY [205:[210:java/lang/Exception  ], 207:[210:java/lang/Exception  ]]
+TRY [207:[212:java/lang/Exception  ], 209:[212:java/lang/Exception  ]]
 ALOAD 1
 INVOKESTATIC local/lito381/server/Protocol.attackSpellRequest([B)Llocal/lito381/server/Protocol$AttackSpellRequest;
 ALOAD 0
```

### `nativeMaterialTargetNegativeBody([BLlocal/lito381/server/BattleSpellCatalog$TargetNegativeStatusSpell;ILlocal/lito381/server/ClientSession$TargetNegativeStatusKind;)V`

- 類別：`local/lito381/server/ClientSession`
- 類型：例外表涵蓋、含迴圈（路徑比較器不支援）；事件後繼：`LAYOUT_ONLY`
- 比較器：`method nativeMaterialTargetNegativeBody([BLlocal/lito381/server/BattleSpellCatalog$TargetNegativeStatusSpell;ILlocal/lito381/server/ClientSession$TargetNegativeStatusKind;)V: left=519:[524:java/lang/Exception  ] right=523:[528:java/lang/Exception  ]; paths rejected: normal control-flow cycle; labelled CFGs differ`

```diff
@@ -1,16 +1,17 @@
 META 0 null [java/io/IOException]
-TRY [519:[524:java/lang/Exception  ], 521:[524:java/lang/Exception  ]]
+TRY [523:[528:java/lang/Exception  ], 525:[528:java/lang/Exception  ]]
 ALOAD 0
 ALOAD 1
 ALOAD 2
 ILOAD 3
 INVOKESTATIC local/lito381/server/CombatNpcDebuffs.negative(Llocal/lito381/server/ClientSession;[BLlocal/lito381/server/BattleSpellCatalog$TargetNegativeStatusSpell;I)Z
-IFNE L1401
+IFEQ L7
+RETURN
 ILOAD 3
-IFLT L11
+IFLT L12
 ILOAD 3
 CONST Integer:100
-IF_ICMPLE L18
+IF_ICMPLE L19
 NEW java/lang/IllegalArgumentException
 DUP
 ALOAD 4
```

### `nativeMaterialWaterLifeBody([BLlocal/lito381/server/BattleSpellCatalog$WaterLifeSpell;)V`

- 類別：`local/lito381/server/ClientSession`
- 類型：分支排列、含迴圈（路徑比較器不支援）；事件後繼：`LAYOUT_ONLY`
- 比較器：`method nativeMaterialWaterLifeBody([BLlocal/lito381/server/BattleSpellCatalog$WaterLifeSpell;)V insn[10]: left=IFEQ L598 right=IFNE L12; paths rejected: normal control-flow cycle; labelled CFGs differ`

```diff
@@ -10,12 +10,14 @@
 ALOAD 0
 ALOAD 3
 INVOKESTATIC local/lito381/server/NativeSelectedPlayerIntent.object(Llocal/lito381/server/ClientSession;Llocal/lito381/server/Protocol$ObjectSpellRequest;)Z
-IFEQ L598
+IFNE L12
+RETURN
 ALOAD 3
 INVOKEVIRTUAL local/lito381/server/Protocol$ObjectSpellRequest.skillId()I
 ALOAD 2
 INVOKEVIRTUAL local/lito381/server/BattleSpellCatalog$WaterLifeSpell.skillId()I
-IF_ICMPNE L598
+IF_ICMPEQ L18
+RETURN
 INVOKESTATIC java/lang/System.nanoTime()J
 LSTORE 4/5
 ALOAD 0
```

### `nativeMaterialWeaknessBody([BLlocal/lito381/server/BattleSpellCatalog$WeaknessSpell;)V`

- 類別：`local/lito381/server/ClientSession`
- 類型：例外表涵蓋、含迴圈（路徑比較器不支援）；事件後繼：`LAYOUT_ONLY`
- 比較器：`method nativeMaterialWeaknessBody([BLlocal/lito381/server/BattleSpellCatalog$WeaknessSpell;)V: left=318:[323:java/lang/Exception  ] right=320:[325:java/lang/Exception  ]; paths rejected: normal control-flow cycle; labelled CFGs differ`

```diff
@@ -1,5 +1,5 @@
 META 0 null [java/io/IOException]
-TRY [318:[323:java/lang/Exception  ], 320:[323:java/lang/Exception  ]]
+TRY [320:[325:java/lang/Exception  ], 322:[325:java/lang/Exception  ]]
 ALOAD 1
 INVOKESTATIC local/lito381/server/Protocol.attackSpellRequest([B)Llocal/lito381/server/Protocol$AttackSpellRequest;
 ALOAD 0
```

### `nativeMaterialWeaponBreakBody([BLlocal/lito381/server/BattleSpellCatalog$WeaponBreakSpell;)V`

- 類別：`local/lito381/server/ClientSession`
- 類型：例外表涵蓋、含迴圈（路徑比較器不支援）；事件後繼：`LAYOUT_ONLY`
- 比較器：`method nativeMaterialWeaponBreakBody([BLlocal/lito381/server/BattleSpellCatalog$WeaponBreakSpell;)V: left=203:[208:java/lang/Exception  ] right=205:[210:java/lang/Exception  ]; paths rejected: normal control-flow cycle; labelled CFGs differ`

```diff
@@ -1,5 +1,5 @@
 META 0 null [java/io/IOException]
-TRY [203:[208:java/lang/Exception  ], 205:[208:java/lang/Exception  ]]
+TRY [205:[210:java/lang/Exception  ], 207:[210:java/lang/Exception  ]]
 ALOAD 0
 GETFIELD local/lito381/server/ClientSession.phase:Llocal/lito381/server/ClientSession$Phase;
 GETSTATIC local/lito381/server/ClientSession$Phase.WORLD:Llocal/lito381/server/ClientSession$Phase;
```

### `nativeMaterialWorldBlindBody([BLlocal/lito381/server/BattleSpellCatalog$BlindSpell;)V`

- 類別：`local/lito381/server/ClientSession`
- 類型：例外表涵蓋、含迴圈（路徑比較器不支援）；事件後繼：`LAYOUT_ONLY`
- 比較器：`method nativeMaterialWorldBlindBody([BLlocal/lito381/server/BattleSpellCatalog$BlindSpell;)V: left=199:[204:java/lang/Exception  ] right=202:[207:java/lang/Exception  ]; paths rejected: normal control-flow cycle; labelled CFGs differ`

```diff
@@ -1,5 +1,5 @@
 META 0 null [java/io/IOException]
-TRY [199:[204:java/lang/Exception  ], 201:[204:java/lang/Exception  ]]
+TRY [202:[207:java/lang/Exception  ], 204:[207:java/lang/Exception  ]]
 ALOAD 1
 INVOKESTATIC local/lito381/server/Protocol.attackSpellRequest([B)Llocal/lito381/server/Protocol$AttackSpellRequest;
 ALOAD 0
```

### `nativeMaterialWorldCancellationBody(Llocal/lito381/server/Protocol$ObjectSpellRequest;Llocal/lito381/server/ClientSession;Llocal/lito381/server/BattleSpellCatalog$CancellationSpell;Llocal/lito381/server/CombatPoisonLifecycle$Scope;)V`

- 類別：`local/lito381/server/ClientSession`
- 類型：分支排列、含迴圈（路徑比較器不支援）；事件後繼：`LAYOUT_ONLY`
- 比較器：`method nativeMaterialWorldCancellationBody(Llocal/lito381/server/Protocol$ObjectSpellRequest;Llocal/lito381/server/ClientSession;Llocal/lito381/server/BattleSpellCatalog$CancellationSpell;Llocal/lito381/server/CombatPoisonLifecycle$Scope;)V insn[3]: left=IFEQ L674 right=IFNE L5; paths rejected: normal control-flow cycle; labelled CFGs differ`

```diff
@@ -3,7 +3,8 @@
 ALOAD 0
 ALOAD 1
 INVOKESTATIC local/lito381/server/NativeSelectedPlayerIntent.object(Llocal/lito381/server/ClientSession;Llocal/lito381/server/Protocol$ObjectSpellRequest;)Z
-IFEQ L674
+IFNE L5
+RETURN
 CONST null
 ASTORE 5
 INVOKESTATIC java/lang/System.nanoTime()J
```

### `nativeMaterialWorldDecayPotionBody([BLlocal/lito381/server/BattleSpellCatalog$DecayPotionSpell;)V`

- 類別：`local/lito381/server/ClientSession`
- 類型：例外表涵蓋、含迴圈（路徑比較器不支援）；事件後繼：`LAYOUT_ONLY`
- 比較器：`method nativeMaterialWorldDecayPotionBody([BLlocal/lito381/server/BattleSpellCatalog$DecayPotionSpell;)V: left=197:[202:java/lang/Exception  ] right=200:[205:java/lang/Exception  ]; paths rejected: normal control-flow cycle; labelled CFGs differ`

```diff
@@ -1,5 +1,5 @@
 META 0 null [java/io/IOException]
-TRY [197:[202:java/lang/Exception  ], 199:[202:java/lang/Exception  ]]
+TRY [200:[205:java/lang/Exception  ], 202:[205:java/lang/Exception  ]]
 ALOAD 1
 INVOKESTATIC local/lito381/server/Protocol.attackSpellRequest([B)Llocal/lito381/server/Protocol$AttackSpellRequest;
 ALOAD 0
```

### `nativeMaterialWorldEarthBindBody([BLlocal/lito381/server/BattleSpellCatalog$BindSpell;)V`

- 類別：`local/lito381/server/ClientSession`
- 類型：分支排列、含迴圈（路徑比較器不支援）；事件後繼：`LAYOUT_ONLY`
- 比較器：`method nativeMaterialWorldEarthBindBody([BLlocal/lito381/server/BattleSpellCatalog$BindSpell;)V insn[10]: left=IFEQ L990 right=IFNE L12; paths rejected: normal control-flow cycle; labelled CFGs differ`

```diff
@@ -10,16 +10,17 @@
 ALOAD 0
 ALOAD 3
 INVOKESTATIC local/lito381/server/NativeSelectedPlayerIntent.attack(Llocal/lito381/server/ClientSession;Llocal/lito381/server/Protocol$AttackSpellRequest;)Z
-IFEQ L990
+IFNE L12
+RETURN
 ALOAD 3
 INVOKEVIRTUAL local/lito381/server/Protocol$AttackSpellRequest.skillId()I
 ALOAD 2
 INVOKEVIRTUAL local/lito381/server/BattleSpellCatalog$BindSpell.skillId()I
-IF_ICMPNE L990
+IF_ICMPNE L29
 ALOAD 0
 GETFIELD local/lito381/server/ClientSession.phase:Llocal/lito381/server/ClientSession$Phase;
 GETSTATIC local/lito381/server/ClientSession$Phase.WORLD:Llocal/lito381/server/ClientSession$Phase;
-IF_ACMPNE L990
+IF_ACMPNE L29
 ALOAD 0
 ALOAD 2
 INVOKEVIRTUAL local/lito381/server/BattleSpellCatalog$BindSpell.skillId()I
```

### `nativeMaterialWorldGroundBody([BLlocal/lito381/server/BattleSpellCatalog$GroundSpell;)V`

- 類別：`local/lito381/server/ClientSession`
- 類型：分支排列、含迴圈（路徑比較器不支援）；事件後繼：`LAYOUT_ONLY`
- 比較器：`method nativeMaterialWorldGroundBody([BLlocal/lito381/server/BattleSpellCatalog$GroundSpell;)V insn[7]: left=IF_ICMPNE L459 right=IF_ICMPEQ L9; paths rejected: normal control-flow cycle; labelled CFGs differ`

```diff
@@ -7,7 +7,8 @@
 INVOKEVIRTUAL local/lito381/server/Protocol$GroundSpellRequest.skillId()I
 ALOAD 2
 INVOKEVIRTUAL local/lito381/server/BattleSpellCatalog$GroundSpell.skillId()I
-IF_ICMPNE L459
+IF_ICMPEQ L9
+RETURN
 ALOAD 0
 ALOAD 2
 INVOKEVIRTUAL local/lito381/server/BattleSpellCatalog$GroundSpell.skillId()I
```

### `nativeMaterialWorldNpcCancellationBody(Llocal/lito381/server/Protocol$ObjectSpellRequest;Llocal/lito381/server/ClientSession;Llocal/lito381/server/BattleCompanion;Llocal/lito381/server/BattleCompanion$Snapshot;Llocal/lito381/server/ClassicWorldMonsterPopulation$Snapshot;Llocal/lito381/server/BattleSpellCatalog$CancellationSpell;Llocal/lito381/server/CombatPoisonLifecycle$Scope;)V`

- 類別：`local/lito381/server/ClientSession`
- 類型：分支排列、含迴圈（路徑比較器不支援）；事件後繼：`LAYOUT_ONLY`
- 比較器：`method nativeMaterialWorldNpcCancellationBody(Llocal/lito381/server/Protocol$ObjectSpellRequest;Llocal/lito381/server/ClientSession;Llocal/lito381/server/BattleCompanion;Llocal/lito381/server/BattleCompanion$Snapshot;Llocal/lito381/server/ClassicWorldMonsterPopulation$Snapshot;Llocal/lito381/server/BattleSpellCatalog$CancellationSpell;Llocal/lito381/server/CombatPoisonLifecycle$Scope;)V insn[3]: l`

```diff
@@ -3,80 +3,81 @@
 ALOAD 0
 ALOAD 1
 INVOKESTATIC local/lito381/server/NativeSelectedPlayerIntent.object(Llocal/lito381/server/ClientSession;Llocal/lito381/server/Protocol$ObjectSpellRequest;)Z
-IFEQ L793
-ALOAD 4
-IFNONNULL L8
-CONST Integer:0
-GOTO L9
-CONST Integer:1
-ALOAD 5
-IFNONNULL L13
-CONST Integer:0
-GOTO L14
+IFNE L5
+RETURN
+ALOAD 4
+IFNONNULL L9
+CONST Integer:0
+GOTO L10
+CONST Integer:1
+ALOAD 5
+IFNONNULL L14
+CONST Integer:0
…（其餘 hunk 見 scratch/issue-70/r6-b-classified.json）
```

### `nativeMaterialWorldParalysisScopeBody([BLlocal/lito381/server/BattleSpellCatalog$ParalysisSpell;Llocal/lito381/server/CombatPoisonLifecycle$Scope;)V`

- 類別：`local/lito381/server/ClientSession`
- 類型：例外表涵蓋、含迴圈（路徑比較器不支援）；事件後繼：`LAYOUT_ONLY`
- 比較器：`method nativeMaterialWorldParalysisScopeBody([BLlocal/lito381/server/BattleSpellCatalog$ParalysisSpell;Llocal/lito381/server/CombatPoisonLifecycle$Scope;)V: left=278:[283:java/lang/Exception  ] right=280:[285:java/lang/Exception  ]; paths rejected: normal control-flow cycle; labelled CFGs differ`

```diff
@@ -1,5 +1,5 @@
 META 0 null [java/io/IOException]
-TRY [278:[283:java/lang/Exception  ], 280:[283:java/lang/Exception  ]]
+TRY [280:[285:java/lang/Exception  ], 282:[285:java/lang/Exception  ]]
 ALOAD 1
 INVOKESTATIC local/lito381/server/Protocol.attackSpellRequest([B)Llocal/lito381/server/Protocol$AttackSpellRequest;
 ALOAD 0
```

### `nativeMaterialWorldPoisonScopeBody([BLlocal/lito381/server/BattleSpellCatalog$PoisonSpell;Llocal/lito381/server/CombatPoisonLifecycle$Scope;)V`

- 類別：`local/lito381/server/ClientSession`
- 類型：例外表涵蓋、含迴圈（路徑比較器不支援）；事件後繼：`LAYOUT_ONLY`
- 比較器：`method nativeMaterialWorldPoisonScopeBody([BLlocal/lito381/server/BattleSpellCatalog$PoisonSpell;Llocal/lito381/server/CombatPoisonLifecycle$Scope;)V: left=273:[278:java/lang/Exception  ] right=275:[280:java/lang/Exception  ]; paths rejected: normal control-flow cycle; labelled CFGs differ`

```diff
@@ -1,5 +1,5 @@
 META 0 null [java/io/IOException]
-TRY [273:[278:java/lang/Exception  ], 275:[278:java/lang/Exception  ]]
+TRY [275:[280:java/lang/Exception  ], 277:[280:java/lang/Exception  ]]
 ALOAD 1
 INVOKESTATIC local/lito381/server/Protocol.attackSpellRequest([B)Llocal/lito381/server/Protocol$AttackSpellRequest;
 ALOAD 0
```

### `nativeMaterialWorldResurrectionBody([BLlocal/lito381/server/BattleSpellCatalog$ResurrectionSpell;)V`

- 類別：`local/lito381/server/ClientSession`
- 類型：分支排列、含迴圈（路徑比較器不支援）；事件後繼：`LAYOUT_ONLY`
- 比較器：`method nativeMaterialWorldResurrectionBody([BLlocal/lito381/server/BattleSpellCatalog$ResurrectionSpell;)V insn[10]: left=IFEQ L708 right=IFNE L12; paths rejected: normal control-flow cycle; labelled CFGs differ`

```diff
@@ -10,16 +10,18 @@
 ALOAD 0
 ALOAD 3
 INVOKESTATIC local/lito381/server/NativeSelectedPlayerIntent.object(Llocal/lito381/server/ClientSession;Llocal/lito381/server/Protocol$ObjectSpellRequest;)Z
-IFEQ L708
+IFNE L12
+RETURN
 ALOAD 3
 INVOKEVIRTUAL local/lito381/server/Protocol$ObjectSpellRequest.skillId()I
 ALOAD 2
 INVOKEVIRTUAL local/lito381/server/BattleSpellCatalog$ResurrectionSpell.skillId()I
-IF_ICMPNE L708
+IF_ICMPNE L21
 ALOAD 0
 GETFIELD local/lito381/server/ClientSession.phase:Llocal/lito381/server/ClientSession$Phase;
 GETSTATIC local/lito381/server/ClientSession$Phase.WORLD:Llocal/lito381/server/ClientSession$Phase;
-IF_ACMPNE L708
+IF_ACMPEQ L22
+RETURN
 ALOAD 0
 GETFIELD local/lito381/server/ClientSession.classicWorldMapId:I
 ISTORE 4
```

### `nativeMaterialWorldSilenceBody([BLlocal/lito381/server/BattleSpellCatalog$SilenceSpell;)V`

- 類別：`local/lito381/server/ClientSession`
- 類型：例外表涵蓋、含迴圈（路徑比較器不支援）；事件後繼：`LAYOUT_ONLY`
- 比較器：`method nativeMaterialWorldSilenceBody([BLlocal/lito381/server/BattleSpellCatalog$SilenceSpell;)V: left=199:[204:java/lang/Exception  ] right=202:[207:java/lang/Exception  ]; paths rejected: normal control-flow cycle; labelled CFGs differ`

```diff
@@ -1,5 +1,5 @@
 META 0 null [java/io/IOException]
-TRY [199:[204:java/lang/Exception  ], 201:[204:java/lang/Exception  ]]
+TRY [202:[207:java/lang/Exception  ], 204:[207:java/lang/Exception  ]]
 ALOAD 1
 INVOKESTATIC local/lito381/server/Protocol.attackSpellRequest([B)Llocal/lito381/server/Protocol$AttackSpellRequest;
 ALOAD 0
```

### `nativeMaterialWorldWeaknessBody([BLlocal/lito381/server/BattleSpellCatalog$WeaknessSpell;)V`

- 類別：`local/lito381/server/ClientSession`
- 類型：例外表涵蓋、含迴圈（路徑比較器不支援）；事件後繼：`LAYOUT_ONLY`
- 比較器：`method nativeMaterialWorldWeaknessBody([BLlocal/lito381/server/BattleSpellCatalog$WeaknessSpell;)V: left=221:[226:java/lang/Exception  ] right=224:[229:java/lang/Exception  ]; paths rejected: normal control-flow cycle; labelled CFGs differ`

```diff
@@ -1,5 +1,5 @@
 META 0 null [java/io/IOException]
-TRY [221:[226:java/lang/Exception  ], 223:[226:java/lang/Exception  ]]
+TRY [224:[229:java/lang/Exception  ], 226:[229:java/lang/Exception  ]]
 ALOAD 1
 INVOKESTATIC local/lito381/server/Protocol.attackSpellRequest([B)Llocal/lito381/server/Protocol$AttackSpellRequest;
 ALOAD 0
```

### `nativeMaterialWorldWeaponBreakBody([BLlocal/lito381/server/BattleSpellCatalog$WeaponBreakSpell;)V`

- 類別：`local/lito381/server/ClientSession`
- 類型：例外表涵蓋、含迴圈（路徑比較器不支援）；事件後繼：`LAYOUT_ONLY`
- 比較器：`method nativeMaterialWorldWeaponBreakBody([BLlocal/lito381/server/BattleSpellCatalog$WeaponBreakSpell;)V: left=368:[373:java/lang/Exception  ] right=370:[375:java/lang/Exception  ]; paths rejected: normal control-flow cycle; labelled CFGs differ`

```diff
@@ -1,5 +1,5 @@
 META 0 null [java/io/IOException]
-TRY [368:[373:java/lang/Exception  ], 370:[373:java/lang/Exception  ]]
+TRY [370:[375:java/lang/Exception  ], 372:[375:java/lang/Exception  ]]
 ALOAD 1
 INVOKESTATIC local/lito381/server/Protocol.attackSpellRequest([B)Llocal/lito381/server/Protocol$AttackSpellRequest;
 ALOAD 0
```

### `nativeMaterialZombieBody([BLlocal/lito381/server/BattleSpellCatalog$CreateZombieSpell;)V`

- 類別：`local/lito381/server/ClientSession`
- 類型：例外表涵蓋、含迴圈（路徑比較器不支援）；事件後繼：`LAYOUT_ONLY`
- 比較器：`method nativeMaterialZombieBody([BLlocal/lito381/server/BattleSpellCatalog$CreateZombieSpell;)V: left=228:[234:java/lang/Exception  ] right=231:[237:java/lang/Exception  ]; paths rejected: normal control-flow cycle; labelled CFGs differ`

```diff
@@ -1,5 +1,5 @@
 META 0 null [java/io/IOException]
-TRY [228:[234:java/lang/Exception  ], 230:[234:java/lang/Exception  ], 231:[234:java/lang/Exception  ], 760:[767:java/lang/Throwable  ], 762:[767:java/lang/Throwable  ], 765:[767:java/lang/Throwable  ], 769:[767:java/lang/Throwable  ]]
+TRY [231:[237:java/lang/Exception  ], 233:[237:java/lang/Exception  ], 234:[237:java/lang/Exception  ], 765:[772:java/lang/Throwable  ], 767:[772:java/lang/Throwable  ], 770:[772:java/lang/Throwable  ], 774:[772:java/lang/Throwable  ]]
 ALOAD 1
 INVOKESTATIC local/lito381/server/Protocol.objectSpellRequest([B)Llocal/lito381/server/Protocol$ObjectSpellRequest;
 ALOAD 0
```

### `nativePlacementPortalBody(Llocal/lito381/server/Protocol$PortalDestination;Llocal/lito381/server/ClientSession$PendingClassicWorldPortal;)V`

- 類別：`local/lito381/server/ClientSession`
- 類型：堆疊／區域變數形狀；事件後繼：`LAYOUT_ONLY`
- 比較器：`method nativePlacementPortalBody(Llocal/lito381/server/Protocol$PortalDestination;Llocal/lito381/server/ClientSession$PendingClassicWorldPortal;)V insn[77]: left=DUP right=ISTORE 6; paths rejected: path limit 4096 exceeded; labelled CFGs differ`

```diff
@@ -77,13 +77,11 @@
 CONST Integer:1
 GOTO L77
 CONST Integer:0
-DUP
 ISTORE 6
-ISTORE 7
-ILOAD 5
-IFNE L96
+ILOAD 5
+IFNE L94
 ILOAD 6
-IFNE L96
+IFNE L94
 GETSTATIC java/lang/System.out:Ljava/io/PrintStream;
 ALOAD 3
 ALOAD 0
```

### `nativeRawOriginal_useLightningWand(Llocal/lito381/server/RuntimeInventory$Item;Llocal/lito381/server/Protocol$TargetLocationInventoryUse;)V`

- 類別：`local/lito381/server/ClientSession`
- 類型：分支排列、含迴圈（路徑比較器不支援）；事件後繼：`LAYOUT_ONLY`
- 比較器：`method nativeRawOriginal_useLightningWand(Llocal/lito381/server/RuntimeInventory$Item;Llocal/lito381/server/Protocol$TargetLocationInventoryUse;)V insn[27]: left=GOTO L650 right=RETURN; paths rejected: normal control-flow cycle; labelled CFGs differ`

```diff
@@ -27,7 +27,7 @@
 GETFIELD local/lito381/server/ClientSession.playerHp:I
 INVOKEDYNAMIC makeConcatWithConstants(Ljava/lang/String;ZI)Ljava/lang/String; bsm=H6 java/lang/invoke/StringConcatFactory.makeConcatWithConstants(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;Ljava/lang/String;[Ljava/lang/Object;)Ljava/lang/invoke/CallSite; args=[str:"[LiTo] C_USE_ITEM lightning-wand rejected phase=\u0001 observer=\u0001 hp=\u0001"]
 INVOKEVIRTUAL java/io/PrintStream.println(Ljava/lang/String;)V
-GOTO L650
+RETURN
 ALOAD 0
 CONST str:"item-lightning-wand"
 INVOKESPECIAL local/lito381/server/ClientSession.cancelAbsoluteBarrier(Ljava/lang/String;)V
```

### `nativeSelfShieldBody([BLlocal/lito381/server/BattleSpellCatalog$ShieldSpell;)V`

- 類別：`local/lito381/server/ClientSession`
- 類型：分支排列、含迴圈（路徑比較器不支援）；事件後繼：`LAYOUT_ONLY`
- 比較器：`method nativeSelfShieldBody([BLlocal/lito381/server/BattleSpellCatalog$ShieldSpell;)V insn[61]: left=GOTO L556 right=RETURN; paths rejected: normal control-flow cycle; labelled CFGs differ`

```diff
@@ -61,27 +61,28 @@
 ILOAD 4
 INVOKEDYNAMIC makeConcatWithConstants(Ljava/lang/String;I)Ljava/lang/String; bsm=H6 java/lang/invoke/StringConcatFactory.makeConcatWithConstants(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;Ljava/lang/String;[Ljava/lang/Object;)Ljava/lang/invoke/CallSite; args=[str:"[LiTo] C_USE_SPELL shield rejected selector/action player=\u0001 skill=\u0001"]
 INVOKEVIRTUAL java/io/PrintStream.println(Ljava/lang/String;)V
-GOTO L556
+RETURN
 INVOKESTATIC java/lang/System.nanoTime()J
 LSTORE 10/11
 ALOAD 0
 GETFIELD local/lito381/server/ClientSession.phase:Llocal/lito381/server/ClientSession$Phase;
 GETSTATIC local/lito381/server/ClientSession$Phase.BATTLE:Llocal/lito381/server/ClientSession$Phase;
-IF_ACMPEQ L72
+IF_ACMPEQ L73
 ALOAD 0
 GETFIELD local/lito381/server/ClientSession.phase:Llocal/lito381/server/ClientSession$Phase;
 GETSTATIC local/lito381/server/ClientSession$Phase.WORLD:Llocal/lito381/server/ClientSession$Phase;
-IF_ACMPNE L556
+IF_ACMPEQ L73
+RETURN
 ALOAD 0
 GETFIELD local/lito381/server/ClientSession.polymorphStatus:Llocal/lito381/server/BattlePolymorphStatus;
 LLOAD 10/11
 INVOKEVIRTUAL local/lito381/server/BattlePolymorphStatus.active(J)Llocal/lito381/server/BattlePolymorphCatalog$Form;
 ASTORE 12
…（其餘 hunk 見 scratch/issue-70/r6-b-classified.json）
```

### `nativeSelfWeightBody([BLlocal/lito381/server/BattleSpellCatalog$WeightSpell;)V`

- 類別：`local/lito381/server/ClientSession`
- 類型：分支排列、含迴圈（路徑比較器不支援）；事件後繼：`LAYOUT_ONLY`
- 比較器：`method nativeSelfWeightBody([BLlocal/lito381/server/BattleSpellCatalog$WeightSpell;)V insn[7]: left=IF_ICMPNE L501 right=IF_ICMPEQ L9; paths rejected: normal control-flow cycle; labelled CFGs differ`

```diff
@@ -7,7 +7,8 @@
 INVOKEVIRTUAL local/lito381/server/Protocol$SpellSelector.skillId()I
 ALOAD 2
 INVOKEVIRTUAL local/lito381/server/BattleSpellCatalog$WeightSpell.skillId()I
-IF_ICMPNE L501
+IF_ICMPEQ L9
+RETURN
 INVOKESTATIC java/lang/System.nanoTime()J
 LSTORE 4/5
 ALOAD 0
```

### `nativeShockPlayerBattleBody([BLlocal/lito381/server/BattleSpellCatalog$ShockStunSpell;)V`

- 類別：`local/lito381/server/ClientSession`
- 類型：例外表涵蓋、含迴圈（路徑比較器不支援）；事件後繼：`LAYOUT_ONLY`
- 比較器：`method nativeShockPlayerBattleBody([BLlocal/lito381/server/BattleSpellCatalog$ShockStunSpell;)V: left=309:[314:java/lang/Exception  ] right=314:[319:java/lang/Exception  ]; paths rejected: normal control-flow cycle; labelled CFGs differ`

```diff
@@ -1,14 +1,16 @@
 META 0 null [java/io/IOException]
-TRY [309:[314:java/lang/Exception  ], 311:[314:java/lang/Exception  ]]
+TRY [314:[319:java/lang/Exception  ], 316:[319:java/lang/Exception  ]]
 ALOAD 0
 ALOAD 1
 INVOKESTATIC local/lito381/server/NativeSourceTargetNpc.beforePacket(Llocal/lito381/server/ClientSession;[B)Z
-IFEQ L1192
+IFNE L5
+RETURN
 ALOAD 0
 ALOAD 1
 ALOAD 2
 INVOKESTATIC local/lito381/server/CombatCompanionSpells.shock(Llocal/lito381/server/ClientSession;[BLlocal/lito381/server/BattleSpellCatalog$ShockStunSpell;)Z
-IFNE L1192
+IFEQ L11
+RETURN
 CONST null
 ASTORE 3
 ALOAD 1
```

### `nativeShockPlayerOriginalBattle([BLlocal/lito381/server/BattleSpellCatalog$ShockStunSpell;)V`

- 類別：`local/lito381/server/ClientSession`
- 類型：例外表涵蓋、含迴圈（路徑比較器不支援）；事件後繼：`LAYOUT_ONLY`
- 比較器：`method nativeShockPlayerOriginalBattle([BLlocal/lito381/server/BattleSpellCatalog$ShockStunSpell;)V: left=309:[314:java/lang/Exception  ] right=314:[319:java/lang/Exception  ]; paths rejected: normal control-flow cycle; labelled CFGs differ`

```diff
@@ -1,14 +1,16 @@
 META 0 null [java/io/IOException]
-TRY [309:[314:java/lang/Exception  ], 311:[314:java/lang/Exception  ], 383:[480:java/lang/Throwable  ], 386:[480:java/lang/Throwable  ], 388:[480:java/lang/Throwable  ], 389:[480:java/lang/Throwable  ], 391:[480:java/lang/Throwable  ], 392:[480:java/lang/Throwable  ], 393:[480:java/lang/Throwable  ], 394:[480:java/lang/Throwable  ], 401:[480:java/lang/Throwable  ], 405:[480:java/lang/Throwable  ], 406:[480:java/lang/Throwable  ], 409:[480:java/lang/Throwable  ], 411:[480:java/lang/Throwable  ], 413:[480:java/lang/Throwable  ], 415:[480:java/lang/Throwable  ], 417:[480:java/lang/Throwable  ], 423:[480:java/lang/Throwable  ], 424:[480:java/lang/Throwable  ], 425:[480:java/lang/Throwable  ], 427:[480:java/lang/Throwable  ], 430:[480:java/lang/Throwable  ], 436:[480:java/lang/Throwable  ], 439:[480:java/lang/Throwable  ], 442:[480:java/lang/Throwable  ], 445:[480:java/lang/Throwable  ], 452:[480:java/lang/Throwable  ], 453:[480:java/lang/Throwable  ], 454:[480:java/lang/Throwable  ], 458:[480:java/lang/Throwable  ], 461:[480:java/lang/Throwable  ], 462:[480:java/lang/Throwable  ], 464:[480:java/lang/Throwable  ], 468:[480:java/lang/Throwable  ], 471:[480:java/lang/Throwable  ], 476:[480:java/lang/Throwable  ], 478:[480:java/lang/Throwable  ], 482:[480:java/lang/Throwable  ]]
+TRY [314:[319:java/lang/Exception  ], 316:[319:java/lang/Exception  ], 388:[487:java/lang/Throwable  ], 391:[487:java/lang/Throwable  ], 393:[487:java/lang/Throwable  ], 394:[487:java/lang/Throwable  ], 396:[487:java/lang/Throwable  ], 397:[487:java/lang/Throwable  ], 398:[487:java/lang/Throwable  ], 399:[487:java/lang/Throwable  ], 406:[487:java/lang/Throwable  ], 410:[487:java/lang/Throwable  ], 411:[487:java/lang/Throwable  ], 414:[487:java/lang/Throwable  ], 416:[487:java/lang/Throwable  ], 418:[487:java/lang/Throwable  ], 420:[487:java/lang/Throwable  ], 422:[487:java/lang/Throwable  ], 428:[487:java/lang/Throwable  ], 429:[487:java/lang/Throwable  ], 430:[487:java/lang/Throwable  ], 432:[487:java/lang/Throwable  ], 435:[487:java/lang/Throwable  ], 441:[487:java/lang/Throwable  ], 444:[487:java/lang/Throwable  ], 447:[487:java/lang/Throwable  ], 450:[487:java/lang/Throwable  ], 457:[487:java/lang/Throwable  ], 458:[487:java/lang/Throwable  ], 459:[487:java/lang/Throwable  ], 462:[487:java/lang/Throwable  ], 466:[487:java/lang/Throwable  ], 469:[487:java/lang/Throwable  ], 471:[487:java/lang/Throwable  ], 475:[487:java/lang/Throwable  ], 478:[487:java/lang/Throwable  ], 483:[487:java/lang/Throwable  ], 485:[487:java/lang/Throwable  ], 489:[487:java/lang/Throwable  ]]
 ALOAD 0
 ALOAD 1
 INVOKESTATIC local/lito381/server/NativeSourceTargetNpc.beforePacket(Llocal/lito381/server/ClientSession;[B)Z
-IFEQ L1192
+IFNE L5
+RETURN
 ALOAD 0
 ALOAD 1
 ALOAD 2
 INVOKESTATIC local/lito381/server/CombatCompanionSpells.shock(Llocal/lito381/server/ClientSession;[BLlocal/lito381/server/BattleSpellCatalog$ShockStunSpell;)Z
-IFNE L1192
+IFEQ L11
+RETURN
 CONST null
 ASTORE 3
 ALOAD 1
```

### `nativeShockPlayerOriginalWorld([BLlocal/lito381/server/BattleSpellCatalog$ShockStunSpell;)V`

- 類別：`local/lito381/server/ClientSession`
- 類型：例外表涵蓋、含迴圈（路徑比較器不支援）；事件後繼：`LAYOUT_ONLY`
- 比較器：`method nativeShockPlayerOriginalWorld([BLlocal/lito381/server/BattleSpellCatalog$ShockStunSpell;)V: left=315:[320:java/lang/Exception  ] right=320:[325:java/lang/Exception  ]; paths rejected: normal control-flow cycle; labelled CFGs differ`

```diff
@@ -1,5 +1,5 @@
 META 0 null [java/io/IOException]
-TRY [315:[320:java/lang/Exception  ], 317:[320:java/lang/Exception  ], 386:[606:java/lang/Throwable  ], 389:[606:java/lang/Throwable  ], 391:[606:java/lang/Throwable  ], 392:[606:java/lang/Throwable  ], 394:[606:java/lang/Throwable  ], 395:[606:java/lang/Throwable  ], 396:[606:java/lang/Throwable  ], 397:[606:java/lang/Throwable  ], 404:[606:java/lang/Throwable  ], 411:[606:java/lang/Throwable  ], 415:[606:java/lang/Throwable  ], 416:[606:java/lang/Throwable  ], 423:[606:java/lang/Throwable  ], 426:[606:java/lang/Throwable  ], 429:[606:java/lang/Throwable  ], 431:[606:java/lang/Throwable  ], 435:[606:java/lang/Throwable  ], 436:[606:java/lang/Throwable  ], 441:[606:java/lang/Throwable  ], 442:[606:java/lang/Throwable  ], 446:[606:java/lang/Throwable  ], 457:[606:java/lang/Throwable  ], 460:[606:java/lang/Throwable  ], 469:[606:java/lang/Throwable  ], 470:[606:java/lang/Throwable  ], 473:[606:java/lang/Throwable  ], 476:[606:java/lang/Throwable  ], 479:[606:java/lang/Throwable  ], 483:[606:java/lang/Throwable  ], 490:[606:java/lang/Throwable  ], 491:[606:java/lang/Throwable  ], 494:[606:java/lang/Throwable  ], 498:[606:java/lang/Throwable  ], 502:[606:java/lang/Throwable  ], 506:[606:java/lang/Throwable  ], 510:[606:java/lang/Throwable  ], 517:[606:java/lang/Throwable  ], 527:[606:java/lang/Throwable  ], 533:[606:java/lang/Throwable  ], 534:[606:java/lang/Throwable  ], 536:[606:java/lang/Throwable  ], 537:[606:java/lang/Throwable  ], 538:[606:java/lang/Throwable  ], 539:[606:java/lang/Throwable  ], 541:[606:java/lang/Throwable  ], 543:[606:java/lang/Throwable  ], 549:[606:java/lang/Throwable  ], 550:[606:java/lang/Throwable  ], 551:[606:java/lang/Throwable  ], 553:[606:java/lang/Throwable  ], 556:[606:java/lang/Throwable  ], 562:[606:java/lang/Throwable  ], 565:[606:java/lang/Throwable  ], 568:[606:java/lang/Throwable  ], 571:[606:java/lang/Throwable  ], 578:[606:java/lang/Throwable  ], 579:[606:java/lang/Throwable  ], 580:[606:java/lang/Throwable  ], 584:[606:java/lang/Throwable  ], 587:[606:java/lang/Throwable  ], 588:[606:java/lang/Throwable  ], 590:[606:java/lang/Throwable  ], 594:[606:java/lang/Throwable  ], 597:[606:java/lang/Throwable  ], 602:[606:java/lang/Throwable  ], 604:[606:java/lang/Throwable  ], 608:[606:java/lang/Throwable  ]]
+TRY [320:[325:java/lang/Exception  ], 322:[325:java/lang/Exception  ], 391:[613:java/lang/Throwable  ], 394:[613:java/lang/Throwable  ], 396:[613:java/lang/Throwable  ], 397:[613:java/lang/Throwable  ], 399:[613:java/lang/Throwable  ], 400:[613:java/lang/Throwable  ], 401:[613:java/lang/Throwable  ], 402:[613:java/lang/Throwable  ], 409:[613:java/lang/Throwable  ], 416:[613:java/lang/Throwable  ], 420:[613:java/lang/Throwable  ], 421:[613:java/lang/Throwable  ], 428:[613:java/lang/Throwable  ], 431:[613:java/lang/Throwable  ], 434:[613:java/lang/Throwable  ], 436:[613:java/lang/Throwable  ], 440:[613:java/lang/Throwable  ], 441:[613:java/lang/Throwable  ], 446:[613:java/lang/Throwable  ], 447:[613:java/lang/Throwable  ], 451:[613:java/lang/Throwable  ], 462:[613:java/lang/Throwable  ], 465:[613:java/lang/Throwable  ], 474:[613:java/lang/Throwable  ], 475:[613:java/lang/Throwable  ], 478:[613:java/lang/Throwable  ], 481:[613:java/lang/Throwable  ], 484:[613:java/lang/Throwable  ], 488:[613:java/lang/Throwable  ], 495:[613:java/lang/Throwable  ], 496:[613:java/lang/Throwable  ], 499:[613:java/lang/Throwable  ], 503:[613:java/lang/Throwable  ], 507:[613:java/lang/Throwable  ], 511:[613:java/lang/Throwable  ], 515:[613:java/lang/Throwable  ], 522:[613:java/lang/Throwable  ], 532:[613:java/lang/Throwable  ], 538:[613:java/lang/Throwable  ], 539:[613:java/lang/Throwable  ], 541:[613:java/lang/Throwable  ], 542:[613:java/lang/Throwable  ], 543:[613:java/lang/Throwable  ], 544:[613:java/lang/Throwable  ], 546:[613:java/lang/Throwable  ], 548:[613:java/lang/Throwable  ], 554:[613:java/lang/Throwable  ], 555:[613:java/lang/Throwable  ], 556:[613:java/lang/Throwable  ], 558:[613:java/lang/Throwable  ], 561:[613:java/lang/Throwable  ], 567:[613:java/lang/Throwable  ], 570:[613:java/lang/Throwable  ], 573:[613:java/lang/Throwable  ], 576:[613:java/lang/Throwable  ], 583:[613:java/lang/Throwable  ], 584:[613:java/lang/Throwable  ], 585:[613:java/lang/Throwable  ], 588:[613:java/lang/Throwable  ], 592:[613:java/lang/Throwable  ], 595:[613:java/lang/Throwable  ], 597:[613:java/lang/Throwable  ], 601:[613:java/lang/Throwable  ], 604:[613:java/lang/Throwable  ], 609:[613:java/lang/Throwable  ], 611:[613:java/lang/Throwable  ], 615:[613:java/lang/Throwable  ]]
 CONST null
 ASTORE 3
 ALOAD 1
```

### `nativeShockPlayerWorldBody([BLlocal/lito381/server/BattleSpellCatalog$ShockStunSpell;)V`

- 類別：`local/lito381/server/ClientSession`
- 類型：例外表涵蓋、含迴圈（路徑比較器不支援）；事件後繼：`LAYOUT_ONLY`
- 比較器：`method nativeShockPlayerWorldBody([BLlocal/lito381/server/BattleSpellCatalog$ShockStunSpell;)V: left=315:[320:java/lang/Exception  ] right=319:[324:java/lang/Exception  ]; paths rejected: normal control-flow cycle; labelled CFGs differ`

```diff
@@ -1,5 +1,5 @@
 META 0 null [java/io/IOException]
-TRY [315:[320:java/lang/Exception  ], 317:[320:java/lang/Exception  ]]
+TRY [319:[324:java/lang/Exception  ], 321:[324:java/lang/Exception  ]]
 CONST null
 ASTORE 3
 ALOAD 1
```

### `nativeSourceControlledOriginal_attack(Llocal/lito381/server/Protocol$AttackRequest;Llocal/lito381/server/ClientSession$AttackEnvelope;)V`

- 類別：`local/lito381/server/ClientSession`
- 類型：例外表涵蓋、含迴圈（路徑比較器不支援）；事件後繼：`LAYOUT_ONLY`
- 比較器：`method nativeSourceControlledOriginal_attack(Llocal/lito381/server/Protocol$AttackRequest;Llocal/lito381/server/ClientSession$AttackEnvelope;)V: left=541:[546:java/lang/Exception  ] right=546:[551:java/lang/Exception  ]; paths rejected: normal control-flow cycle; labelled CFGs differ`

```diff
@@ -1,8 +1,9 @@
 META 0 null [java/io/IOException]
-TRY [541:[546:java/lang/Exception  ], 543:[546:java/lang/Exception  ], 2315:[2471:java/io/IOException  ], 2317:[2471:java/io/IOException  ], 2318:[2471:java/io/IOException  ], 2319:[2471:java/io/IOException  ], 2320:[2471:java/io/IOException  ], 2325:[2471:java/io/IOException  ], 2327:[2471:java/io/IOException  ], 2328:[2471:java/io/IOException  ], 2335:[2471:java/io/IOException  ], 2338:[2471:java/io/IOException  ], 2339:[2471:java/io/IOException  ], 2342:[2471:java/io/IOException  ], 2347:[2471:java/io/IOException  ], 2348:[2471:java/io/IOException  ], 2351:[2471:java/io/IOException  ], 2356:[2471:java/io/IOException  ], 2358:[2471:java/io/IOException  ], 2361:[2471:java/io/IOException  ], 2362:[2471:java/io/IOException  ], 2364:[2471:java/io/IOException  ], 2366:[2471:java/io/IOException  ], 2368:[2471:java/io/IOException  ], 2370:[2471:java/io/IOException  ], 2372:[2471:java/io/IOException  ], 2373:[2471:java/io/IOException  ], 2377:[2471:java/io/IOException  ], 2379:[2471:java/io/IOException  ], 2381:[2471:java/io/IOException  ], 2388:[2471:java/io/IOException  ], 2391:[2471:java/io/IOException  ], 2394:[2471:java/io/IOException  ], 2397:[2471:java/io/IOException  ], 2399:[2471:java/io/IOException  ], 2401:[2471:java/io/IOException  ], 2403:[2471:java/io/IOException  ], 2404:[2471:java/io/IOException  ], 2410:[2471:java/io/IOException  ], 2412:[2471:java/io/IOException  ], 2414:[2471:java/io/IOException  ], 2418:[2471:java/io/IOException  ], 2421:[2471:java/io/IOException  ], 2423:[2471:java/io/IOException  ], 2426:[2471:java/io/IOException  ], 2427:[2471:java/io/IOException  ], 2430:[2471:java/io/IOException  ], 2436:[2471:java/io/IOException  ], 2438:[2471:java/io/IOException  ], 2441:[2471:java/io/IOException  ], 2442:[2471:java/io/IOException  ], 2444:[2471:java/io/IOException  ], 2446:[2471:java/io/IOException  ], 2450:[2471:java/io/IOException  ], 2452:[2471:java/io/IOException  ], 2453:[2471:java/io/IOException  ], 2456:[2471:java/io/IOException  ], 2459:[2471:java/io/IOException  ], 2464:[2471:java/io/IOException  ], 2466:[2471:java/io/IOException  ], 2468:[2471:java/io/IOException  ]]
+TRY [546:[551:java/lang/Exception  ], 548:[551:java/lang/Exception  ], 2310:[2466:java/io/IOException  ], 2312:[2466:java/io/IOException  ], 2313:[2466:java/io/IOException  ], 2314:[2466:java/io/IOException  ], 2315:[2466:java/io/IOException  ], 2320:[2466:java/io/IOException  ], 2322:[2466:java/io/IOException  ], 2323:[2466:java/io/IOException  ], 2330:[2466:java/io/IOException  ], 2333:[2466:java/io/IOException  ], 2334:[2466:java/io/IOException  ], 2337:[2466:java/io/IOException  ], 2342:[2466:java/io/IOException  ], 2343:[2466:java/io/IOException  ], 2346:[2466:java/io/IOException  ], 2351:[2466:java/io/IOException  ], 2353:[2466:java/io/IOException  ], 2356:[2466:java/io/IOException  ], 2357:[2466:java/io/IOException  ], 2359:[2466:java/io/IOException  ], 2361:[2466:java/io/IOException  ], 2363:[2466:java/io/IOException  ], 2365:[2466:java/io/IOException  ], 2367:[2466:java/io/IOException  ], 2368:[2466:java/io/IOException  ], 2372:[2466:java/io/IOException  ], 2374:[2466:java/io/IOException  ], 2376:[2466:java/io/IOException  ], 2383:[2466:java/io/IOException  ], 2386:[2466:java/io/IOException  ], 2389:[2466:java/io/IOException  ], 2392:[2466:java/io/IOException  ], 2394:[2466:java/io/IOException  ], 2396:[2466:java/io/IOException  ], 2398:[2466:java/io/IOException  ], 2399:[2466:java/io/IOException  ], 2405:[2466:java/io/IOException  ], 2407:[2466:java/io/IOException  ], 2409:[2466:java/io/IOException  ], 2413:[2466:java/io/IOException  ], 2416:[2466:java/io/IOException  ], 2418:[2466:java/io/IOException  ], 2421:[2466:java/io/IOException  ], 2422:[2466:java/io/IOException  ], 2425:[2466:java/io/IOException  ], 2431:[2466:java/io/IOException  ], 2433:[2466:java/io/IOException  ], 2436:[2466:java/io/IOException  ], 2437:[2466:java/io/IOException  ], 2439:[2466:java/io/IOException  ], 2441:[2466:java/io/IOException  ], 2445:[2466:java/io/IOException  ], 2447:[2466:java/io/IOException  ], 2448:[2466:java/io/IOException  ], 2451:[2466:java/io/IOException  ], 2454:[2466:java/io/IOException  ], 2459:[2466:java/io/IOException  ], 2461:[2466:java/io/IOException  ], 2463:[2466:java/io/IOException  ]]
 ALOAD 0
 INVOKESTATIC local/lito381/server/NativeSourceDollAction.before(Llocal/lito381/server/ClientSession;)Z
-IFEQ L3152
+IFNE L4
+RETURN
 ALOAD 0
 ALOAD 1
 INVOKESTATIC local/lito381/server/NativeSourceNpcDamageDisplay.capture(Llocal/lito381/server/ClientSession;Llocal/lito381/server/Protocol$AttackRequest;)Llocal/lito381/server/NativeSourceMonsterHp$Aim;
```

### `nativeSourceControlledOriginal_nativeAreaFreezeOriginalBattle([BLlocal/lito381/server/BattleSpellCatalog$FreezeAttackSpell;)V`

- 類別：`local/lito381/server/ClientSession`
- 類型：例外表涵蓋、含迴圈（路徑比較器不支援）；事件後繼：`LAYOUT_ONLY`
- 比較器：`method nativeSourceControlledOriginal_nativeAreaFreezeOriginalBattle([BLlocal/lito381/server/BattleSpellCatalog$FreezeAttackSpell;)V: left=260:[266:java/lang/Exception  ] right=262:[268:java/lang/Exception  ]; paths rejected: normal control-flow cycle; labelled CFGs differ`

```diff
@@ -1,9 +1,10 @@
 META 0 null [java/io/IOException]
-TRY [260:[266:java/lang/Exception  ], 262:[266:java/lang/Exception  ], 263:[266:java/lang/Exception  ], 342:[348:java/lang/Exception  ], 344:[348:java/lang/Exception  ], 345:[348:java/lang/Exception  ], 422:[496:java/lang/Throwable  ], 425:[496:java/lang/Throwable  ], 427:[496:java/lang/Throwable  ], 428:[496:java/lang/Throwable  ], 430:[496:java/lang/Throwable  ], 431:[496:java/lang/Throwable  ], 432:[496:java/lang/Throwable  ], 433:[496:java/lang/Throwable  ], 440:[496:java/lang/Throwable  ], 444:[496:java/lang/Throwable  ], 445:[496:java/lang/Throwable  ], 448:[496:java/lang/Throwable  ], 450:[496:java/lang/Throwable  ], 452:[496:java/lang/Throwable  ], 454:[496:java/lang/Throwable  ], 460:[496:java/lang/Throwable  ], 461:[496:java/lang/Throwable  ], 462:[496:java/lang/Throwable  ], 464:[496:java/lang/Throwable  ], 468:[496:java/lang/Throwable  ], 469:[496:java/lang/Throwable  ], 470:[496:java/lang/Throwable  ], 474:[496:java/lang/Throwable  ], 477:[496:java/lang/Throwable  ], 478:[496:java/lang/Throwable  ], 480:[496:java/lang/Throwable  ], 484:[496:java/lang/Throwable  ], 487:[496:java/lang/Throwable  ], 492:[496:java/lang/Throwable  ], 494:[496:java/lang/Throwable  ], 498:[496:java/lang/Throwable  ]]
+TRY [262:[268:java/lang/Exception  ], 264:[268:java/lang/Exception  ], 265:[268:java/lang/Exception  ], 344:[350:java/lang/Exception  ], 346:[350:java/lang/Exception  ], 347:[350:java/lang/Exception  ], 424:[500:java/lang/Throwable  ], 427:[500:java/lang/Throwable  ], 429:[500:java/lang/Throwable  ], 430:[500:java/lang/Throwable  ], 432:[500:java/lang/Throwable  ], 433:[500:java/lang/Throwable  ], 434:[500:java/lang/Throwable  ], 435:[500:java/lang/Throwable  ], 442:[500:java/lang/Throwable  ], 446:[500:java/lang/Throwable  ], 447:[500:java/lang/Throwable  ], 450:[500:java/lang/Throwable  ], 452:[500:java/lang/Throwable  ], 454:[500:java/lang/Throwable  ], 456:[500:java/lang/Throwable  ], 462:[500:java/lang/Throwable  ], 463:[500:java/lang/Throwable  ], 464:[500:java/lang/Throwable  ], 466:[500:java/lang/Throwable  ], 470:[500:java/lang/Throwable  ], 471:[500:java/lang/Throwable  ], 472:[500:java/lang/Throwable  ], 475:[500:java/lang/Throwable  ], 479:[500:java/lang/Throwable  ], 482:[500:java/lang/Throwable  ], 484:[500:java/lang/Throwable  ], 488:[500:java/lang/Throwable  ], 491:[500:java/lang/Throwable  ], 496:[500:java/lang/Throwable  ], 498:[500:java/lang/Throwable  ], 502:[500:java/lang/Throwable  ]]
 ALOAD 0
 ALOAD 1
 INVOKESTATIC local/lito381/server/NativeSourceTargetNpc.beforePacket(Llocal/lito381/server/ClientSession;[B)Z
-IFEQ L1723
+IFNE L5
+RETURN
 CONST null
 ASTORE 3
 CONST null
```

### `nativeSourceControlledOriginal_nativeFreezeAcceptedOriginalBattle([BLlocal/lito381/server/BattleSpellCatalog$FreezeAttackSpell;)V`

- 類別：`local/lito381/server/ClientSession`
- 類型：例外表涵蓋、含迴圈（路徑比較器不支援）；事件後繼：`LAYOUT_ONLY`
- 比較器：`method nativeSourceControlledOriginal_nativeFreezeAcceptedOriginalBattle([BLlocal/lito381/server/BattleSpellCatalog$FreezeAttackSpell;)V: left=503:[508:java/lang/Exception  ] right=508:[513:java/lang/Exception  ]; paths rejected: normal control-flow cycle; labelled CFGs differ`

```diff
@@ -1,9 +1,10 @@
 META 0 null [java/io/IOException]
-TRY [503:[508:java/lang/Exception  ], 505:[508:java/lang/Exception  ], 627:[771:java/lang/Throwable  ], 630:[771:java/lang/Throwable  ], 632:[771:java/lang/Throwable  ], 633:[771:java/lang/Throwable  ], 635:[771:java/lang/Throwable  ], 636:[771:java/lang/Throwable  ], 637:[771:java/lang/Throwable  ], 638:[771:java/lang/Throwable  ], 645:[771:java/lang/Throwable  ], 652:[771:java/lang/Throwable  ], 654:[771:java/lang/Throwable  ], 658:[771:java/lang/Throwable  ], 661:[771:java/lang/Throwable  ], 666:[771:java/lang/Throwable  ], 667:[771:java/lang/Throwable  ], 670:[771:java/lang/Throwable  ], 672:[771:java/lang/Throwable  ], 684:[771:java/lang/Throwable  ], 688:[771:java/lang/Throwable  ], 689:[771:java/lang/Throwable  ], 692:[771:java/lang/Throwable  ], 699:[771:java/lang/Throwable  ], 700:[771:java/lang/Throwable  ], 703:[771:java/lang/Throwable  ], 705:[771:java/lang/Throwable  ], 707:[771:java/lang/Throwable  ], 709:[771:java/lang/Throwable  ], 715:[771:java/lang/Throwable  ], 716:[771:java/lang/Throwable  ], 717:[771:java/lang/Throwable  ], 719:[771:java/lang/Throwable  ], 722:[771:java/lang/Throwable  ], 728:[771:java/lang/Throwable  ], 731:[771:java/lang/Throwable  ], 734:[771:java/lang/Throwable  ], 737:[771:java/lang/Throwable  ], 745:[771:java/lang/Throwable  ], 748:[771:java/lang/Throwable  ], 749:[771:java/lang/Throwable  ], 751:[771:java/lang/Throwable  ], 754:[771:java/lang/Throwable  ], 756:[771:java/lang/Throwable  ], 757:[771:java/lang/Throwable  ], 760:[771:java/lang/Throwable  ], 765:[771:java/lang/Throwable  ], 766:[771:java/lang/Throwable  ], 769:[771:java/lang/Throwable  ], 773:[771:java/lang/Throwable  ]]
+TRY [508:[513:java/lang/Exception  ], 510:[513:java/lang/Exception  ], 632:[778:java/lang/Throwable  ], 635:[778:java/lang/Throwable  ], 637:[778:java/lang/Throwable  ], 638:[778:java/lang/Throwable  ], 640:[778:java/lang/Throwable  ], 641:[778:java/lang/Throwable  ], 642:[778:java/lang/Throwable  ], 643:[778:java/lang/Throwable  ], 650:[778:java/lang/Throwable  ], 657:[778:java/lang/Throwable  ], 659:[778:java/lang/Throwable  ], 663:[778:java/lang/Throwable  ], 666:[778:java/lang/Throwable  ], 671:[778:java/lang/Throwable  ], 672:[778:java/lang/Throwable  ], 675:[778:java/lang/Throwable  ], 677:[778:java/lang/Throwable  ], 689:[778:java/lang/Throwable  ], 693:[778:java/lang/Throwable  ], 694:[778:java/lang/Throwable  ], 697:[778:java/lang/Throwable  ], 704:[778:java/lang/Throwable  ], 705:[778:java/lang/Throwable  ], 708:[778:java/lang/Throwable  ], 710:[778:java/lang/Throwable  ], 712:[778:java/lang/Throwable  ], 714:[778:java/lang/Throwable  ], 720:[778:java/lang/Throwable  ], 721:[778:java/lang/Throwable  ], 722:[778:java/lang/Throwable  ], 724:[778:java/lang/Throwable  ], 727:[778:java/lang/Throwable  ], 733:[778:java/lang/Throwable  ], 736:[778:java/lang/Throwable  ], 739:[778:java/lang/Throwable  ], 742:[778:java/lang/Throwable  ], 749:[778:java/lang/Throwable  ], 753:[778:java/lang/Throwable  ], 756:[778:java/lang/Throwable  ], 758:[778:java/lang/Throwable  ], 761:[778:java/lang/Throwable  ], 763:[778:java/lang/Throwable  ], 764:[778:java/lang/Throwable  ], 767:[778:java/lang/Throwable  ], 772:[778:java/lang/Throwable  ], 773:[778:java/lang/Throwable  ], 776:[778:java/lang/Throwable  ], 780:[778:java/lang/Throwable  ]]
 ALOAD 0
 ALOAD 1
 INVOKESTATIC local/lito381/server/NativeSourceTargetNpc.beforePacket(Llocal/lito381/server/ClientSession;[B)Z
-IFEQ L1716
+IFNE L5
+RETURN
 CONST null
 ASTORE 3
 ALOAD 1
```

### `nativeSourceControlledOriginal_nativeSourceLightningOriginalBody(Llocal/lito381/server/Protocol$AttackSpellRequest;Llocal/lito381/server/BattleSpellCatalog$DirectionalLineSpell;)V`

- 類別：`local/lito381/server/ClientSession`
- 類型：例外表涵蓋、含迴圈（路徑比較器不支援）；事件後繼：`LAYOUT_ONLY`
- 比較器：`method nativeSourceControlledOriginal_nativeSourceLightningOriginalBody(Llocal/lito381/server/Protocol$AttackSpellRequest;Llocal/lito381/server/BattleSpellCatalog$DirectionalLineSpell;)V: left=655:[779:java/lang/Throwable  ] right=663:[789:java/lang/Throwable  ]; paths rejected: normal control-flow cycle; labelled CFGs differ`

```diff
@@ -1,13 +1,15 @@
 META 0 null [java/io/IOException]
-TRY [655:[779:java/lang/Throwable  ], 658:[779:java/lang/Throwable  ], 660:[779:java/lang/Throwable  ], 661:[779:java/lang/Throwable  ], 663:[779:java/lang/Throwable  ], 664:[779:java/lang/Throwable  ], 665:[779:java/lang/Throwable  ], 666:[779:java/lang/Throwable  ], 673:[779:java/lang/Throwable  ], 677:[779:java/lang/Throwable  ], 678:[779:java/lang/Throwable  ], 681:[779:java/lang/Throwable  ], 683:[779:java/lang/Throwable  ], 687:[779:java/lang/Throwable  ], 690:[779:java/lang/Throwable  ], 692:[779:java/lang/Throwable  ], 697:[779:java/lang/Throwable  ], 698:[779:java/lang/Throwable  ], 700:[779:java/lang/Throwable  ], 701:[779:java/lang/Throwable  ], 702:[779:java/lang/Throwable  ], 705:[779:java/lang/Throwable  ], 711:[779:java/lang/Throwable  ], 712:[779:java/lang/Throwable  ], 714:[779:java/lang/Throwable  ], 715:[779:java/lang/Throwable  ], 716:[779:java/lang/Throwable  ], 717:[779:java/lang/Throwable  ], 719:[779:java/lang/Throwable  ], 721:[779:java/lang/Throwable  ], 723:[779:java/lang/Throwable  ], 725:[779:java/lang/Throwable  ], 727:[779:java/lang/Throwable  ], 733:[779:java/lang/Throwable  ], 734:[779:java/lang/Throwable  ], 735:[779:java/lang/Throwable  ], 737:[779:java/lang/Throwable  ], 741:[779:java/lang/Throwable  ], 742:[779:java/lang/Throwable  ], 743:[779:java/lang/Throwable  ], 746:[779:java/lang/Throwable  ], 748:[779:java/lang/Throwable  ], 750:[779:java/lang/Throwable  ], 754:[779:java/lang/Throwable  ], 757:[779:java/lang/Throwable  ], 758:[779:java/lang/Throwable  ], 760:[779:java/lang/Throwable  ], 763:[779:java/lang/Throwable  ], 767:[779:java/lang/Throwable  ], 770:[779:java/lang/Throwable  ], 775:[779:java/lang/Throwable  ], 777:[779:java/lang/Throwable  ], 781:[779:java/lang/Throwable  ], 921:[927:java/lang/Exception  ], 923:[927:java/lang/Exception  ], 924:[927:java/lang/Exception  ], 1118:[1124:java/lang/Exception  ], 1120:[1124:java/lang/Exception  ], 1121:[1124:java/lang/Exception  ]]
+TRY [663:[789:java/lang/Throwable  ], 666:[789:java/lang/Throwable  ], 668:[789:java/lang/Throwable  ], 669:[789:java/lang/Throwable  ], 671:[789:java/lang/Throwable  ], 672:[789:java/lang/Throwable  ], 673:[789:java/lang/Throwable  ], 674:[789:java/lang/Throwable  ], 681:[789:java/lang/Throwable  ], 685:[789:java/lang/Throwable  ], 686:[789:java/lang/Throwable  ], 689:[789:java/lang/Throwable  ], 691:[789:java/lang/Throwable  ], 694:[789:java/lang/Throwable  ], 696:[789:java/lang/Throwable  ], 701:[789:java/lang/Throwable  ], 702:[789:java/lang/Throwable  ], 704:[789:java/lang/Throwable  ], 705:[789:java/lang/Throwable  ], 706:[789:java/lang/Throwable  ], 709:[789:java/lang/Throwable  ], 715:[789:java/lang/Throwable  ], 716:[789:java/lang/Throwable  ], 718:[789:java/lang/Throwable  ], 719:[789:java/lang/Throwable  ], 720:[789:java/lang/Throwable  ], 721:[789:java/lang/Throwable  ], 723:[789:java/lang/Throwable  ], 725:[789:java/lang/Throwable  ], 727:[789:java/lang/Throwable  ], 729:[789:java/lang/Throwable  ], 731:[789:java/lang/Throwable  ], 737:[789:java/lang/Throwable  ], 738:[789:java/lang/Throwable  ], 739:[789:java/lang/Throwable  ], 741:[789:java/lang/Throwable  ], 745:[789:java/lang/Throwable  ], 749:[789:java/lang/Throwable  ], 750:[789:java/lang/Throwable  ], 751:[789:java/lang/Throwable  ], 754:[789:java/lang/Throwable  ], 756:[789:java/lang/Throwable  ], 758:[789:java/lang/Throwable  ], 761:[789:java/lang/Throwable  ], 765:[789:java/lang/Throwable  ], 768:[789:java/lang/Throwable  ], 770:[789:java/lang/Throwable  ], 773:[789:java/lang/Throwable  ], 777:[789:java/lang/Throwable  ], 780:[789:java/lang/Throwable  ], 785:[789:java/lang/Throwable  ], 787:[789:java/lang/Throwable  ], 791:[789:java/lang/Throwable  ], 932:[938:java/lang/Exception  ], 934:[938:java/lang/Exception  ], 935:[938:java/lang/Exception  ], 1130:[1136:java/lang/Exception  ], 1132:[1136:java/lang/Exception  ], 1133:[1136:java/lang/Exception  ]]
 ALOAD 0
 ALOAD 1
 INVOKESTATIC local/lito381/server/NativeSourceTargetNpc.before(Llocal/lito381/server/ClientSession;Llocal/lito381/server/Protocol$AttackSpellRequest;)Z
-IFEQ L2290
+IFNE L5
+RETURN
 ALOAD 0
 ALOAD 1
 INVOKESTATIC local/lito381/server/NativeSelectedPlayerIntent.attack(Llocal/lito381/server/ClientSession;Llocal/lito381/server/Protocol$AttackSpellRequest;)Z
-IFEQ L2290
+IFNE L10
+RETURN
 CONST null
 ASTORE 3
 CONST null
```

### `nativeSourceControlledOriginal_useAreaAttackSpell(Llocal/lito381/server/Protocol$AttackSpellRequest;Llocal/lito381/server/BattleSpellCatalog$AreaAttackSpell;)V`

- 類別：`local/lito381/server/ClientSession`
- 類型：例外表涵蓋、含迴圈（路徑比較器不支援）；事件後繼：`LAYOUT_ONLY`
- 比較器：`method nativeSourceControlledOriginal_useAreaAttackSpell(Llocal/lito381/server/Protocol$AttackSpellRequest;Llocal/lito381/server/BattleSpellCatalog$AreaAttackSpell;)V: left=749:[755:java/lang/Exception  ] right=757:[763:java/lang/Exception  ]; paths rejected: normal control-flow cycle; labelled CFGs differ`

```diff
@@ -1,5 +1,5 @@
 META 0 null [java/io/IOException]
-TRY [749:[755:java/lang/Exception  ], 751:[755:java/lang/Exception  ], 752:[755:java/lang/Exception  ], 789:[795:java/lang/Exception  ], 791:[795:java/lang/Exception  ], 792:[795:java/lang/Exception  ], 848:[1240:java/lang/Throwable  ], 849:[1229:java/lang/Throwable  , 1240:java/lang/Throwable  ], 852:[1229:java/lang/Throwable  , 1240:java/lang/Throwable  ], 854:[1229:java/lang/Throwable  , 1240:java/lang/Throwable  ], 855:[1229:java/lang/Throwable  , 1240:java/lang/Throwable  ], 857:[1229:java/lang/Throwable  , 1240:java/lang/Throwable  ], 858:[1229:java/lang/Throwable  , 1240:java/lang/Throwable  ], 859:[1229:java/lang/Throwable  , 1240:java/lang/Throwable  ], 860:[1229:java/lang/Throwable  , 1240:java/lang/Throwable  ], 867:[1229:java/lang/Throwable  , 1240:java/lang/Throwable  ], 870:[1229:java/lang/Throwable  , 1240:java/lang/Throwable  ], 871:[1229:java/lang/Throwable  , 1240:java/lang/Throwable  ], 878:[1229:java/lang/Throwable  , 1240:java/lang/Throwable  ], 881:[1229:java/lang/Throwable  , 1240:java/lang/Throwable  ], 882:[1229:java/lang/Throwable  , 1240:java/lang/Throwable  ], 889:[1229:java/lang/Throwable  , 1240:java/lang/Throwable  ], 892:[1229:java/lang/Throwable  , 1240:java/lang/Throwable  ], 893:[1229:java/lang/Throwable  , 1240:java/lang/Throwable  ], 898:[1229:java/lang/Throwable  , 1240:java/lang/Throwable  ], 900:[1229:java/lang/Throwable  , 1240:java/lang/Throwable  ], 909:[1229:java/lang/Throwable  , 1240:java/lang/Throwable  ], 910:[1229:java/lang/Throwable  , 1240:java/lang/Throwable  ], 915:[1229:java/lang/Throwable  , 1240:java/lang/Throwable  ], 916:[1229:java/lang/Throwable  , 1240:java/lang/Throwable  ], 919:[1229:java/lang/Throwable  , 1240:java/lang/Throwable  ], 922:[1229:java/lang/Throwable  , 1240:java/lang/Throwable  ], 925:[1229:java/lang/Throwable  , 1240:java/lang/Throwable  ], 928:[1229:java/lang/Throwable  , 1240:java/lang/Throwable  ], 933:[1229:java/lang/Throwable  , 1240:java/lang/Throwable  ], 936:[1229:java/lang/Throwable  , 1240:java/lang/Throwable  ], 940:[1229:java/lang/Throwable  , 1240:java/lang/Throwable  ], 944:[1229:java/lang/Throwable  , 1240:java/lang/Throwable  ], 948:[1229:java/lang/Throwable  , 1240:java/lang/Throwable  ], 952:[1229:java/lang/Throwable  , 1240:java/lang/Throwable  ], 955:[1229:java/lang/Throwable  , 1240:java/lang/Throwable  ], 957:[1229:java/lang/Throwable  , 1240:java/lang/Throwable  ], 959:[1229:java/lang/Throwable  , 1240:java/lang/Throwable  ], 960:[1229:java/lang/Throwable  , 1240:java/lang/Throwable  ], 971:[1229:java/lang/Throwable  , 1240:java/lang/Throwable  ], 975:[1229:java/lang/Throwable  , 1240:java/lang/Throwable  ], 979:[1229:java/lang/Throwable  , 1240:java/lang/Throwable  ], 983:[1229:java/lang/Throwable  , 1240:java/lang/Throwable  ], 984:[1229:java/lang/Throwable  , 1240:java/lang/Throwable  ], 995:[1229:java/lang/Throwable  , 1240:java/lang/Throwable  ], 998:[1229:java/lang/Throwable  , 1240:java/lang/Throwable  ], 1002:[1229:java/lang/Throwable  , 1240:java/lang/Throwable  ], 1006:[1229:java/lang/Throwable  , 1240:java/lang/Throwable  ], 1010:[1229:java/lang/Throwable  , 1240:java/lang/Throwable  ], 1011:[1229:java/lang/Throwable  , 1240:java/lang/Throwable  ], 1022:[1229:java/lang/Throwable  , 1240:java/lang/Throwable  ], 1025:[1229:java/lang/Throwable  , 1240:java/lang/Throwable  ], 1029:[1229:java/lang/Throwable  , 1240:java/lang/Throwable  ], 1033:[1229:java/lang/Throwable  , 1240:java/lang/Throwable  ], 1037:[1229:java/lang/Throwable  , 1240:java/lang/Throwable  ], 1038:[1229:java/lang/Throwable  , 1240:java/lang/Throwable  ], 1048:[1229:java/lang/Throwable  , 1240:java/lang/Throwable  ], 1053:[1229:java/lang/Throwable  , 1240:java/lang/Throwable  ], 1057:[1229:java/lang/Throwable  , 1240:java/lang/Throwable  ], 1065:[1229:java/lang/Throwable  , 1240:java/lang/Throwable  ], 1066:[1229:java/lang/Throwable  , 1240:java/lang/Throwable  ], 1069:[1229:java/lang/Throwable  , 1240:java/lang/Throwable  ], 1074:[1229:java/lang/Throwable  , 1240:java/lang/Throwable  ], 1077:[1229:java/lang/Throwable  , 1240:java/lang/Throwable  ], 1081:[1229:java/lang/Throwable  , 1240:java/lang/Throwable  ], 1085:[1229:java/lang/Throwable  , 1240:java/lang/Throwable  ], 1092:[1229:java/lang/Throwable  , 1240:java/lang/Throwable  ], 1095:[1229:java/lang/Throwable  , 1240:java/lang/Throwable  ], 1102:[1229:java/lang/Throwable  , 1240:java/lang/Throwable  ], 1103:[1229:java/lang/Throwable  , 1240:java/lang/Throwable  ], 1108:[1229:java/lang/Throwable  , 1240:java/lang/Throwable  ], 1112:[1229:java/lang/Throwable  , 1240:java/lang/Throwable  ], 1113:[1229:java/lang/Throwable  , 1240:java/lang/Throwable  ], 1115:[1229:java/lang/Throwable  , 1240:java/lang/Throwable  ], 1126:[1229:java/lang/Throwable  , 1240:java/lang/Throwable  ], 1130:[1229:java/lang/Throwable  , 1240:java/lang/Throwable  ], 1132:[1229:java/lang/Throwable  , 1240:java/lang/Throwable  ], 1137:[1229:java/lang/Throwable  , 1240:java/lang/Throwable  ], 1146:[1229:java/lang/Throwable  , 1240:java/lang/Throwable  ], 1149:[1229:java/lang/Throwable  , 1240:java/lang/Throwable  ], 1150:[1229:java/lang/Throwable  , 1240:java/lang/Throwable  ], 1153:[1229:java/lang/Throwable  , 1240:java/lang/Throwable  ], 1155:[1229:java/lang/Throwable  , 1240:java/lang/Throwable  ], 1157:[1229:java/lang/Throwable  , 1240:java/lang/Throwable  ], 1159:[1229:java/lang/Throwable  , 1240:java/lang/Throwable  ], 1162:[1229:java/lang/Throwable  , 1240:java/lang/Throwable  ], 1165:[1229:java/lang/Throwable  , 1240:java/lang/Throwable  ], 1167:[1229:java/lang/Throwable  , 1240:java/lang/Throwable  ], 1168:[1229:java/lang/Throwable  , 1240:java/lang/Throwable  ], 1171:[1229:java/lang/Throwable  , 1240:java/lang/Throwable  ], 1176:[1229:java/lang/Throwable  , 1240:java/lang/Throwable  ], 1177:[1229:java/lang/Throwable  , 1240:java/lang/Throwable  ], 1180:[1229:java/lang/Throwable  , 1240:java/lang/Throwable  ], 1183:[1229:java/lang/Throwable  , 1240:java/lang/Throwable  ], 1185:[1229:java/lang/Throwable  , 1240:java/lang/Throwable  ], 1190:[1229:java/lang/Throwable  , 1240:java/lang/Throwable  ], 1191:[1229:java/lang/Throwable  , 1240:java/lang/Throwable  ], 1193:[1229:java/lang/Throwable  , 1240:java/lang/Throwable  ], 1194:[1229:java/lang/Throwable  , 1240:java/lang/Throwable  ], 1195:[1229:java/lang/Throwable  , 1240:java/lang/Throwable  ], 1198:[1229:java/lang/Throwable  , 1240:java/lang/Throwable  ], 1204:[1229:java/lang/Throwable  , 1240:java/lang/Throwable  ], 1205:[1229:java/lang/Throwable  , 1240:java/lang/Throwable  ], 1207:[1229:java/lang/Throwable  , 1240:java/lang/Throwable  ], 1208:[1229:java/lang/Throwable  , 1240:java/lang/Throwable  ], 1209:[1229:java/lang/Throwable  , 1240:java/lang/Throwable  ], 1210:[1229:java/lang/Throwable  , 1240:java/lang/Throwable  ], 1212:[1229:java/lang/Throwable  , 1240:java/lang/Throwable  ], 1214:[1229:java/lang/Throwable  , 1240:java/lang/Throwable  ], 1216:[1229:java/lang/Throwable  , 1240:java/lang/Throwable  ], 1223:[1229:java/lang/Throwable  , 1240:java/lang/Throwable  ], 1224:[1229:java/lang/Throwable  , 1240:java/lang/Throwable  ], 1225:[1229:java/lang/Throwable  , 1240:java/lang/Throwable  ], 1227:[1229:java/lang/Throwable  , 1240:java/lang/Throwable  ], 1231:[1229:java/lang/Throwable  , 1240:java/lang/Throwable  ], 1233:[1240:java/lang/Throwable  ], 1235:[1240:java/lang/Throwable  ], 1238:[1240:java/lang/Throwable  ], 1242:[1240:java/lang/Throwable  ]]
+TRY [757:[763:java/lang/Exception  ], 759:[763:java/lang/Exception  ], 760:[763:java/lang/Exception  ], 797:[803:java/lang/Exception  ], 799:[803:java/lang/Exception  ], 800:[803:java/lang/Exception  ], 856:[1249:java/lang/Throwable  ], 857:[1241:java/lang/Throwable  , 1249:java/lang/Throwable  ], 860:[1241:java/lang/Throwable  , 1249:java/lang/Throwable  ], 862:[1241:java/lang/Throwable  , 1249:java/lang/Throwable  ], 863:[1241:java/lang/Throwable  , 1249:java/lang/Throwable  ], 865:[1241:java/lang/Throwable  , 1249:java/lang/Throwable  ], 866:[1241:java/lang/Throwable  , 1249:java/lang/Throwable  ], 867:[1241:java/lang/Throwable  , 1249:java/lang/Throwable  ], 868:[1241:java/lang/Throwable  , 1249:java/lang/Throwable  ], 875:[1241:java/lang/Throwable  , 1249:java/lang/Throwable  ], 878:[1241:java/lang/Throwable  , 1249:java/lang/Throwable  ], 879:[1241:java/lang/Throwable  , 1249:java/lang/Throwable  ], 886:[1241:java/lang/Throwable  , 1249:java/lang/Throwable  ], 889:[1241:java/lang/Throwable  , 1249:java/lang/Throwable  ], 890:[1241:java/lang/Throwable  , 1249:java/lang/Throwable  ], 897:[1241:java/lang/Throwable  , 1249:java/lang/Throwable  ], 900:[1241:java/lang/Throwable  , 1249:java/lang/Throwable  ], 901:[1241:java/lang/Throwable  , 1249:java/lang/Throwable  ], 906:[1241:java/lang/Throwable  , 1249:java/lang/Throwable  ], 908:[1241:java/lang/Throwable  , 1249:java/lang/Throwable  ], 917:[1241:java/lang/Throwable  , 1249:java/lang/Throwable  ], 918:[1241:java/lang/Throwable  , 1249:java/lang/Throwable  ], 923:[1241:java/lang/Throwable  , 1249:java/lang/Throwable  ], 924:[1241:java/lang/Throwable  , 1249:java/lang/Throwable  ], 927:[1241:java/lang/Throwable  , 1249:java/lang/Throwable  ], 930:[1241:java/lang/Throwable  , 1249:java/lang/Throwable  ], 933:[1241:java/lang/Throwable  , 1249:java/lang/Throwable  ], 936:[1241:java/lang/Throwable  , 1249:java/lang/Throwable  ], 941:[1241:java/lang/Throwable  , 1249:java/lang/Throwable  ], 944:[1241:java/lang/Throwable  , 1249:java/lang/Throwable  ], 948:[1241:java/lang/Throwable  , 1249:java/lang/Throwable  ], 952:[1241:java/lang/Throwable  , 1249:java/lang/Throwable  ], 956:[1241:java/lang/Throwable  , 1249:java/lang/Throwable  ], 960:[1241:java/lang/Throwable  , 1249:java/lang/Throwable  ], 963:[1241:java/lang/Throwable  , 1249:java/lang/Throwable  ], 965:[1241:java/lang/Throwable  , 1249:java/lang/Throwable  ], 967:[1241:java/lang/Throwable  , 1249:java/lang/Throwable  ], 968:[1241:java/lang/Throwable  , 1249:java/lang/Throwable  ], 979:[1241:java/lang/Throwable  , 1249:java/lang/Throwable  ], 983:[1241:java/lang/Throwable  , 1249:java/lang/Throwable  ], 987:[1241:java/lang/Throwable  , 1249:java/lang/Throwable  ], 991:[1241:java/lang/Throwable  , 1249:java/lang/Throwable  ], 992:[1241:java/lang/Throwable  , 1249:java/lang/Throwable  ], 1003:[1241:java/lang/Throwable  , 1249:java/lang/Throwable  ], 1006:[1241:java/lang/Throwable  , 1249:java/lang/Throwable  ], 1010:[1241:java/lang/Throwable  , 1249:java/lang/Throwable  ], 1014:[1241:java/lang/Throwable  , 1249:java/lang/Throwable  ], 1018:[1241:java/lang/Throwable  , 1249:java/lang/Throwable  ], 1019:[1241:java/lang/Throwable  , 1249:java/lang/Throwable  ], 1030:[1241:java/lang/Throwable  , 1249:java/lang/Throwable  ], 1033:[1241:java/lang/Throwable  , 1249:java/lang/Throwable  ], 1037:[1241:java/lang/Throwable  , 1249:java/lang/Throwable  ], 1041:[1241:java/lang/Throwable  , 1249:java/lang/Throwable  ], 1045:[1241:java/lang/Throwable  , 1249:java/lang/Throwable  ], 1046:[1241:java/lang/Throwable  , 1249:java/lang/Throwable  ], 1056:[1241:java/lang/Throwable  , 1249:java/lang/Throwable  ], 1061:[1241:java/lang/Throwable  , 1249:java/lang/Throwable  ], 1065:[1241:java/lang/Throwable  , 1249:java/lang/Throwable  ], 1073:[1241:java/lang/Throwable  , 1249:java/lang/Throwable  ], 1074:[1241:java/lang/Throwable  , 1249:java/lang/Throwable  ], 1077:[1241:java/lang/Throwable  , 1249:java/lang/Throwable  ], 1082:[1241:java/lang/Throwable  , 1249:java/lang/Throwable  ], 1085:[1241:java/lang/Throwable  , 1249:java/lang/Throwable  ], 1089:[1241:java/lang/Throwable  , 1249:java/lang/Throwable  ], 1093:[1241:java/lang/Throwable  , 1249:java/lang/Throwable  ], 1100:[1241:java/lang/Throwable  , 1249:java/lang/Throwable  ], 1103:[1241:java/lang/Throwable  , 1249:java/lang/Throwable  ], 1110:[1241:java/lang/Throwable  , 1249:java/lang/Throwable  ], 1111:[1241:java/lang/Throwable  , 1249:java/lang/Throwable  ], 1116:[1241:java/lang/Throwable  , 1249:java/lang/Throwable  ], 1120:[1241:java/lang/Throwable  , 1249:java/lang/Throwable  ], 1121:[1241:java/lang/Throwable  , 1249:java/lang/Throwable  ], 1123:[1241:java/lang/Throwable  , 1249:java/lang/Throwable  ], 1134:[1241:java/lang/Throwable  , 1249:java/lang/Throwable  ], 1138:[1241:java/lang/Throwable  , 1249:java/lang/Throwable  ], 1140:[1241:java/lang/Throwable  , 1249:java/lang/Throwable  ], 1145:[1241:java/lang/Throwable  , 1249:java/lang/Throwable  ], 1152:[1241:java/lang/Throwable  , 1249:java/lang/Throwable  ], 1154:[1241:java/lang/Throwable  , 1249:java/lang/Throwable  ], 1159:[1241:java/lang/Throwable  , 1249:java/lang/Throwable  ], 1160:[1241:java/lang/Throwable  , 1249:java/lang/Throwable  ], 1162:[1241:java/lang/Throwable  , 1249:java/lang/Throwable  ], 1163:[1241:java/lang/Throwable  , 1249:java/lang/Throwable  ], 1164:[1241:java/lang/Throwable  , 1249:java/lang/Throwable  ], 1167:[1241:java/lang/Throwable  , 1249:java/lang/Throwable  ], 1173:[1241:java/lang/Throwable  , 1249:java/lang/Throwable  ], 1174:[1241:java/lang/Throwable  , 1249:java/lang/Throwable  ], 1176:[1241:java/lang/Throwable  , 1249:java/lang/Throwable  ], 1177:[1241:java/lang/Throwable  , 1249:java/lang/Throwable  ], 1178:[1241:java/lang/Throwable  , 1249:java/lang/Throwable  ], 1179:[1241:java/lang/Throwable  , 1249:java/lang/Throwable  ], 1181:[1241:java/lang/Throwable  , 1249:java/lang/Throwable  ], 1183:[1241:java/lang/Throwable  , 1249:java/lang/Throwable  ], 1185:[1241:java/lang/Throwable  , 1249:java/lang/Throwable  ], 1192:[1241:java/lang/Throwable  , 1249:java/lang/Throwable  ], 1193:[1241:java/lang/Throwable  , 1249:java/lang/Throwable  ], 1194:[1241:java/lang/Throwable  , 1249:java/lang/Throwable  ], 1196:[1241:java/lang/Throwable  , 1249:java/lang/Throwable  ], 1198:[1249:java/lang/Throwable  ], 1202:[1241:java/lang/Throwable  , 1249:java/lang/Throwable  ], 1206:[1241:java/lang/Throwable  , 1249:java/lang/Throwable  ], 1209:[1241:java/lang/Throwable  , 1249:java/lang/Throwable  ], 1212:[1241:java/lang/Throwable  , 1249:java/lang/Throwable  ], 1214:[1241:java/lang/Throwable  , 1249:java/lang/Throwable  ], 1216:[1241:java/lang/Throwable  , 1249:java/lang/Throwable  ], 1218:[1241:java/lang/Throwable  , 1249:java/lang/Throwable  ], 1221:[1241:java/lang/Throwable  , 1249:java/lang/Throwable  ], 1224:[1241:java/lang/Throwable  , 1249:java/lang/Throwable  ], 1226:[1241:java/lang/Throwable  , 1249:java/lang/Throwable  ], 1227:[1241:java/lang/Throwable  , 1249:java/lang/Throwable  ], 1230:[1241:java/lang/Throwable  , 1249:java/lang/Throwable  ], 1235:[1241:java/lang/Throwable  , 1249:java/lang/Throwable  ], 1236:[1241:java/lang/Throwable  , 1249:java/lang/Throwable  ], 1239:[1241:java/lang/Throwable  , 1249:java/lang/Throwable  ], 1243:[1241:java/lang/Throwable  , 1249:java/lang/Throwable  ], 1245:[1249:java/lang/Throwable  ], 1247:[1249:java/lang/Throwable  ], 1251:[1249:java/lang/Throwable  ]]
 ALOAD 1
 ALOAD 0
 ALOAD 2
```

### `nativeSourceControlledOriginal_useAttackSpell(Llocal/lito381/server/Protocol$AttackSpellRequest;Llocal/lito381/server/BattleSpellCatalog$AttackSpell;IIII)V`

- 類別：`local/lito381/server/ClientSession`
- 類型：例外表涵蓋、含迴圈（路徑比較器不支援）；事件後繼：`LAYOUT_ONLY`
- 比較器：`method nativeSourceControlledOriginal_useAttackSpell(Llocal/lito381/server/Protocol$AttackSpellRequest;Llocal/lito381/server/BattleSpellCatalog$AttackSpell;IIII)V: left=478:[483:java/lang/Exception  ] right=484:[489:java/lang/Exception  ]; paths rejected: normal control-flow cycle; labelled CFGs differ`

```diff
@@ -1,23 +1,25 @@
 META 0 null [java/io/IOException]
-TRY [478:[483:java/lang/Exception  ], 480:[483:java/lang/Exception  ], 676:[851:java/lang/Throwable  ], 679:[851:java/lang/Throwable  ], 681:[851:java/lang/Throwable  ], 682:[851:java/lang/Throwable  ], 684:[851:java/lang/Throwable  ], 685:[851:java/lang/Throwable  ], 686:[851:java/lang/Throwable  ], 687:[851:java/lang/Throwable  ], 693:[851:java/lang/Throwable  ], 696:[851:java/lang/Throwable  ], 703:[851:java/lang/Throwable  ], 704:[851:java/lang/Throwable  ], 707:[851:java/lang/Throwable  ], 712:[851:java/lang/Throwable  ], 713:[851:java/lang/Throwable  ], 716:[851:java/lang/Throwable  ], 718:[851:java/lang/Throwable  ], 720:[851:java/lang/Throwable  ], 722:[851:java/lang/Throwable  ], 724:[851:java/lang/Throwable  ], 729:[851:java/lang/Throwable  ], 730:[851:java/lang/Throwable  ], 731:[851:java/lang/Throwable  ], 733:[851:java/lang/Throwable  ], 738:[851:java/lang/Throwable  ], 744:[851:java/lang/Throwable  ], 745:[851:java/lang/Throwable  ], 747:[851:java/lang/Throwable  ], 748:[851:java/lang/Throwable  ], 749:[851:java/lang/Throwable  ], 751:[851:java/lang/Throwable  ], 754:[851:java/lang/Throwable  ], 760:[851:java/lang/Throwable  ], 761:[851:java/lang/Throwable  ], 763:[851:java/lang/Throwable  ], 764:[851:java/lang/Throwable  ], 765:[851:java/lang/Throwable  ], 767:[851:java/lang/Throwable  ], 770:[851:java/lang/Throwable  ], 773:[851:java/lang/Throwable  ], 777:[851:java/lang/Throwable  ], 783:[851:java/lang/Throwable  ], 786:[851:java/lang/Throwable  ], 789:[851:java/lang/Throwable  ], 792:[851:java/lang/Throwable  ], 798:[851:java/lang/Throwable  ], 802:[851:java/lang/Throwable  ], 805:[851:java/lang/Throwable  ], 811:[851:java/lang/Throwable  ], 815:[851:java/lang/Throwable  ], 818:[851:java/lang/Throwable  ], 822:[851:java/lang/Throwable  ], 825:[851:java/lang/Throwable  ], 826:[851:java/lang/Throwable  ], 828:[851:java/lang/Throwable  ], 831:[851:java/lang/Throwable  ], 834:[851:java/lang/Throwable  ], 836:[851:java/lang/Throwable  ], 837:[851:java/lang/Throwable  ], 840:[851:java/lang/Throwable  ], 845:[851:java/lang/Throwable  ], 846:[851:java/lang/Throwable  ], 849:[851:java/lang/Throwable  ], 853:[851:java/lang/Throwable  ]]
+TRY [484:[489:java/lang/Exception  ], 486:[489:java/lang/Exception  ], 682:[857:java/lang/Throwable  ], 685:[857:java/lang/Throwable  ], 687:[857:java/lang/Throwable  ], 688:[857:java/lang/Throwable  ], 690:[857:java/lang/Throwable  ], 691:[857:java/lang/Throwable  ], 692:[857:java/lang/Throwable  ], 693:[857:java/lang/Throwable  ], 699:[857:java/lang/Throwable  ], 702:[857:java/lang/Throwable  ], 709:[857:java/lang/Throwable  ], 710:[857:java/lang/Throwable  ], 713:[857:java/lang/Throwable  ], 718:[857:java/lang/Throwable  ], 719:[857:java/lang/Throwable  ], 722:[857:java/lang/Throwable  ], 724:[857:java/lang/Throwable  ], 726:[857:java/lang/Throwable  ], 728:[857:java/lang/Throwable  ], 730:[857:java/lang/Throwable  ], 735:[857:java/lang/Throwable  ], 736:[857:java/lang/Throwable  ], 737:[857:java/lang/Throwable  ], 739:[857:java/lang/Throwable  ], 744:[857:java/lang/Throwable  ], 750:[857:java/lang/Throwable  ], 751:[857:java/lang/Throwable  ], 753:[857:java/lang/Throwable  ], 754:[857:java/lang/Throwable  ], 755:[857:java/lang/Throwable  ], 757:[857:java/lang/Throwable  ], 760:[857:java/lang/Throwable  ], 766:[857:java/lang/Throwable  ], 767:[857:java/lang/Throwable  ], 769:[857:java/lang/Throwable  ], 770:[857:java/lang/Throwable  ], 771:[857:java/lang/Throwable  ], 773:[857:java/lang/Throwable  ], 776:[857:java/lang/Throwable  ], 779:[857:java/lang/Throwable  ], 783:[857:java/lang/Throwable  ], 789:[857:java/lang/Throwable  ], 792:[857:java/lang/Throwable  ], 795:[857:java/lang/Throwable  ], 798:[857:java/lang/Throwable  ], 804:[857:java/lang/Throwable  ], 808:[857:java/lang/Throwable  ], 812:[857:java/lang/Throwable  ], 815:[857:java/lang/Throwable  ], 819:[857:java/lang/Throwable  ], 822:[857:java/lang/Throwable  ], 825:[857:java/lang/Throwable  ], 829:[857:java/lang/Throwable  ], 832:[857:java/lang/Throwable  ], 834:[857:java/lang/Throwable  ], 837:[857:java/lang/Throwable  ], 840:[857:java/lang/Throwable  ], 842:[857:java/lang/Throwable  ], 843:[857:java/lang/Throwable  ], 846:[857:java/lang/Throwable  ], 851:[857:java/lang/Throwable  ], 852:[857:java/lang/Throwable  ], 855:[857:java/lang/Throwable  ], 859:[857:java/lang/Throwable  ]]
 ALOAD 0
 INVOKESTATIC local/lito381/server/NativeSourceSpecialNpc.clear(Llocal/lito381/server/ClientSession;)V
 ALOAD 0
 ALOAD 1
 INVOKESTATIC local/lito381/server/NativeSourceSingleNpc.before(Llocal/lito381/server/ClientSession;Llocal/lito381/server/Protocol$AttackSpellRequest;)Z
-IFEQ L2516
+IFNE L7
+RETURN
 ALOAD 0
 ALOAD 1
 INVOKESTATIC local/lito381/server/NativeSelectedPlayerIntent.attack(Llocal/lito381/server/ClientSession;Llocal/lito381/server/Protocol$AttackSpellRequest;)Z
-IFEQ L2516
+IFNE L12
+RETURN
 CONST null
 ASTORE 7
 ILOAD 3
 CONST Integer:-1
-IF_ICMPLT L18
+IF_ICMPLT L20
…（其餘 hunk 見 scratch/issue-70/r6-b-classified.json）
```

### `nativeSourceControlledOriginal_useCenteredAreaAttackSpell([BLlocal/lito381/server/BattleSpellCatalog$AreaAttackSpell;)V`

- 類別：`local/lito381/server/ClientSession`
- 類型：例外表涵蓋、含迴圈（路徑比較器不支援）；事件後繼：`LAYOUT_ONLY`
- 比較器：`method nativeSourceControlledOriginal_useCenteredAreaAttackSpell([BLlocal/lito381/server/BattleSpellCatalog$AreaAttackSpell;)V: left=162:[168:java/lang/Exception  ] right=166:[172:java/lang/Exception  ]; paths rejected: normal control-flow cycle; labelled CFGs differ`

```diff
@@ -1,5 +1,5 @@
 META 0 null [java/io/IOException]
-TRY [162:[168:java/lang/Exception  ], 164:[168:java/lang/Exception  ], 165:[168:java/lang/Exception  ], 358:[364:java/lang/Exception  ], 360:[364:java/lang/Exception  ], 361:[364:java/lang/Exception  ], 398:[404:java/lang/Exception  ], 400:[404:java/lang/Exception  ], 401:[404:java/lang/Exception  ], 431:[573:java/lang/Throwable  ], 434:[573:java/lang/Throwable  ], 436:[573:java/lang/Throwable  ], 437:[573:java/lang/Throwable  ], 439:[573:java/lang/Throwable  ], 440:[573:java/lang/Throwable  ], 441:[573:java/lang/Throwable  ], 442:[573:java/lang/Throwable  ], 449:[573:java/lang/Throwable  ], 453:[573:java/lang/Throwable  ], 455:[573:java/lang/Throwable  ], 458:[573:java/lang/Throwable  ], 459:[573:java/lang/Throwable  ], 462:[573:java/lang/Throwable  ], 465:[573:java/lang/Throwable  ], 470:[573:java/lang/Throwable  ], 474:[573:java/lang/Throwable  ], 478:[573:java/lang/Throwable  ], 480:[573:java/lang/Throwable  ], 482:[573:java/lang/Throwable  ], 484:[573:java/lang/Throwable  ], 487:[573:java/lang/Throwable  ], 489:[573:java/lang/Throwable  ], 494:[573:java/lang/Throwable  ], 495:[573:java/lang/Throwable  ], 497:[573:java/lang/Throwable  ], 498:[573:java/lang/Throwable  ], 499:[573:java/lang/Throwable  ], 502:[573:java/lang/Throwable  ], 508:[573:java/lang/Throwable  ], 509:[573:java/lang/Throwable  ], 511:[573:java/lang/Throwable  ], 512:[573:java/lang/Throwable  ], 513:[573:java/lang/Throwable  ], 514:[573:java/lang/Throwable  ], 516:[573:java/lang/Throwable  ], 518:[573:java/lang/Throwable  ], 521:[573:java/lang/Throwable  ], 523:[573:java/lang/Throwable  ], 528:[573:java/lang/Throwable  ], 529:[573:java/lang/Throwable  ], 530:[573:java/lang/Throwable  ], 532:[573:java/lang/Throwable  ], 537:[573:java/lang/Throwable  ], 540:[573:java/lang/Throwable  ], 541:[573:java/lang/Throwable  ], 544:[573:java/lang/Throwable  ], 546:[573:java/lang/Throwable  ], 548:[573:java/lang/Throwable  ], 550:[573:java/lang/Throwable  ], 553:[573:java/lang/Throwable  ], 556:[573:java/lang/Throwable  ], 558:[573:java/lang/Throwable  ], 559:[573:java/lang/Throwable  ], 562:[573:java/lang/Throwable  ], 567:[573:java/lang/Throwable  ], 568:[573:java/lang/Throwable  ], 571:[573:java/lang/Throwable  ], 575:[573:java/lang/Throwable  ]]
+TRY [166:[172:java/lang/Exception  ], 168:[172:java/lang/Exception  ], 169:[172:java/lang/Exception  ], 362:[368:java/lang/Exception  ], 364:[368:java/lang/Exception  ], 365:[368:java/lang/Exception  ], 402:[408:java/lang/Exception  ], 404:[408:java/lang/Exception  ], 405:[408:java/lang/Exception  ], 435:[579:java/lang/Throwable  ], 438:[579:java/lang/Throwable  ], 440:[579:java/lang/Throwable  ], 441:[579:java/lang/Throwable  ], 443:[579:java/lang/Throwable  ], 444:[579:java/lang/Throwable  ], 445:[579:java/lang/Throwable  ], 446:[579:java/lang/Throwable  ], 453:[579:java/lang/Throwable  ], 457:[579:java/lang/Throwable  ], 459:[579:java/lang/Throwable  ], 462:[579:java/lang/Throwable  ], 463:[579:java/lang/Throwable  ], 466:[579:java/lang/Throwable  ], 469:[579:java/lang/Throwable  ], 474:[579:java/lang/Throwable  ], 478:[579:java/lang/Throwable  ], 482:[579:java/lang/Throwable  ], 484:[579:java/lang/Throwable  ], 486:[579:java/lang/Throwable  ], 488:[579:java/lang/Throwable  ], 491:[579:java/lang/Throwable  ], 493:[579:java/lang/Throwable  ], 498:[579:java/lang/Throwable  ], 499:[579:java/lang/Throwable  ], 501:[579:java/lang/Throwable  ], 502:[579:java/lang/Throwable  ], 503:[579:java/lang/Throwable  ], 506:[579:java/lang/Throwable  ], 512:[579:java/lang/Throwable  ], 513:[579:java/lang/Throwable  ], 515:[579:java/lang/Throwable  ], 516:[579:java/lang/Throwable  ], 517:[579:java/lang/Throwable  ], 518:[579:java/lang/Throwable  ], 520:[579:java/lang/Throwable  ], 522:[579:java/lang/Throwable  ], 525:[579:java/lang/Throwable  ], 527:[579:java/lang/Throwable  ], 532:[579:java/lang/Throwable  ], 533:[579:java/lang/Throwable  ], 534:[579:java/lang/Throwable  ], 536:[579:java/lang/Throwable  ], 540:[579:java/lang/Throwable  ], 544:[579:java/lang/Throwable  ], 547:[579:java/lang/Throwable  ], 550:[579:java/lang/Throwable  ], 552:[579:java/lang/Throwable  ], 554:[579:java/lang/Throwable  ], 556:[579:java/lang/Throwable  ], 559:[579:java/lang/Throwable  ], 562:[579:java/lang/Throwable  ], 564:[579:java/lang/Throwable  ], 565:[579:java/lang/Throwable  ], 568:[579:java/lang/Throwable  ], 573:[579:java/lang/Throwable  ], 574:[579:java/lang/Throwable  ], 577:[579:java/lang/Throwable  ], 581:[579:java/lang/Throwable  ]]
 CONST null
 ASTORE 3
 ALOAD 1
```

### `nativeSourceControlledOriginal_useHealingSpell([BLlocal/lito381/server/BattleSpellCatalog$HealingSpell;)V`

- 類別：`local/lito381/server/ClientSession`
- 類型：例外表涵蓋、含迴圈（路徑比較器不支援）；事件後繼：`LAYOUT_ONLY`
- 比較器：`method nativeSourceControlledOriginal_useHealingSpell([BLlocal/lito381/server/BattleSpellCatalog$HealingSpell;)V: left=405:[410:java/lang/Exception  ] right=408:[413:java/lang/Exception  ]; paths rejected: normal control-flow cycle; labelled CFGs differ`

```diff
@@ -1,5 +1,5 @@
 META 0 null [java/io/IOException]
-TRY [405:[410:java/lang/Exception  ], 407:[410:java/lang/Exception  ], 538:[967:java/lang/Throwable  ], 539:[956:java/lang/Throwable  , 967:java/lang/Throwable  ], 542:[956:java/lang/Throwable  , 967:java/lang/Throwable  ], 544:[956:java/lang/Throwable  , 967:java/lang/Throwable  ], 545:[956:java/lang/Throwable  , 967:java/lang/Throwable  ], 547:[956:java/lang/Throwable  , 967:java/lang/Throwable  ], 548:[956:java/lang/Throwable  , 967:java/lang/Throwable  ], 549:[956:java/lang/Throwable  , 967:java/lang/Throwable  ], 550:[956:java/lang/Throwable  , 967:java/lang/Throwable  ], 557:[956:java/lang/Throwable  , 967:java/lang/Throwable  ], 560:[956:java/lang/Throwable  , 967:java/lang/Throwable  ], 561:[956:java/lang/Throwable  , 967:java/lang/Throwable  ], 568:[956:java/lang/Throwable  , 967:java/lang/Throwable  ], 571:[956:java/lang/Throwable  , 967:java/lang/Throwable  ], 572:[956:java/lang/Throwable  , 967:java/lang/Throwable  ], 579:[956:java/lang/Throwable  , 967:java/lang/Throwable  ], 582:[956:java/lang/Throwable  , 967:java/lang/Throwable  ], 583:[956:java/lang/Throwable  , 967:java/lang/Throwable  ], 588:[956:java/lang/Throwable  , 967:java/lang/Throwable  ], 590:[956:java/lang/Throwable  , 967:java/lang/Throwable  ], 597:[956:java/lang/Throwable  , 967:java/lang/Throwable  ], 598:[956:java/lang/Throwable  , 967:java/lang/Throwable  ], 601:[956:java/lang/Throwable  , 967:java/lang/Throwable  ], 604:[956:java/lang/Throwable  , 967:java/lang/Throwable  ], 607:[956:java/lang/Throwable  , 967:java/lang/Throwable  ], 610:[956:java/lang/Throwable  , 967:java/lang/Throwable  ], 615:[956:java/lang/Throwable  , 967:java/lang/Throwable  ], 618:[956:java/lang/Throwable  , 967:java/lang/Throwable  ], 622:[956:java/lang/Throwable  , 967:java/lang/Throwable  ], 626:[956:java/lang/Throwable  , 967:java/lang/Throwable  ], 630:[956:java/lang/Throwable  , 967:java/lang/Throwable  ], 632:[956:java/lang/Throwable  , 967:java/lang/Throwable  ], 634:[956:java/lang/Throwable  , 967:java/lang/Throwable  ], 635:[956:java/lang/Throwable  , 967:java/lang/Throwable  ], 646:[956:java/lang/Throwable  , 967:java/lang/Throwable  ], 649:[956:java/lang/Throwable  , 967:java/lang/Throwable  ], 653:[956:java/lang/Throwable  , 967:java/lang/Throwable  ], 657:[956:java/lang/Throwable  , 967:java/lang/Throwable  ], 661:[956:java/lang/Throwable  , 967:java/lang/Throwable  ], 662:[956:java/lang/Throwable  , 967:java/lang/Throwable  ], 665:[956:java/lang/Throwable  , 967:java/lang/Throwable  ], 668:[956:java/lang/Throwable  , 967:java/lang/Throwable  ], 669:[956:java/lang/Throwable  , 967:java/lang/Throwable  ], 671:[956:java/lang/Throwable  , 967:java/lang/Throwable  ], 672:[956:java/lang/Throwable  , 967:java/lang/Throwable  ], 683:[956:java/lang/Throwable  , 967:java/lang/Throwable  ], 686:[956:java/lang/Throwable  , 967:java/lang/Throwable  ], 690:[956:java/lang/Throwable  , 967:java/lang/Throwable  ], 694:[956:java/lang/Throwable  , 967:java/lang/Throwable  ], 697:[956:java/lang/Throwable  , 967:java/lang/Throwable  ], 698:[956:java/lang/Throwable  , 967:java/lang/Throwable  ], 700:[956:java/lang/Throwable  , 967:java/lang/Throwable  ], 701:[956:java/lang/Throwable  , 967:java/lang/Throwable  ], 712:[956:java/lang/Throwable  , 967:java/lang/Throwable  ], 715:[956:java/lang/Throwable  , 967:java/lang/Throwable  ], 719:[956:java/lang/Throwable  , 967:java/lang/Throwable  ], 723:[956:java/lang/Throwable  , 967:java/lang/Throwable  ], 726:[956:java/lang/Throwable  , 967:java/lang/Throwable  ], 727:[956:java/lang/Throwable  , 967:java/lang/Throwable  ], 729:[956:java/lang/Throwable  , 967:java/lang/Throwable  ], 730:[956:java/lang/Throwable  , 967:java/lang/Throwable  ], 740:[956:java/lang/Throwable  , 967:java/lang/Throwable  ], 745:[956:java/lang/Throwable  , 967:java/lang/Throwable  ], 747:[956:java/lang/Throwable  , 967:java/lang/Throwable  ], 750:[956:java/lang/Throwable  , 967:java/lang/Throwable  ], 754:[956:java/lang/Throwable  , 967:java/lang/Throwable  ], 762:[956:java/lang/Throwable  , 967:java/lang/Throwable  ], 763:[956:java/lang/Throwable  , 967:java/lang/Throwable  ], 766:[956:java/lang/Throwable  , 967:java/lang/Throwable  ], 769:[956:java/lang/Throwable  , 967:java/lang/Throwable  ], 772:[956:java/lang/Throwable  , 967:java/lang/Throwable  ], 777:[956:java/lang/Throwable  , 967:java/lang/Throwable  ], 780:[956:java/lang/Throwable  , 967:java/lang/Throwable  ], 784:[956:java/lang/Throwable  , 967:java/lang/Throwable  ], 788:[956:java/lang/Throwable  , 967:java/lang/Throwable  ], 795:[956:java/lang/Throwable  , 967:java/lang/Throwable  ], 798:[956:java/lang/Throwable  , 967:java/lang/Throwable  ], 805:[956:java/lang/Throwable  , 967:java/lang/Throwable  ], 806:[956:java/lang/Throwable  , 967:java/lang/Throwable  ], 817:[956:java/lang/Throwable  , 967:java/lang/Throwable  ], 827:[956:java/lang/Throwable  , 967:java/lang/Throwable  ], 830:[956:java/lang/Throwable  , 967:java/lang/Throwable  ], 831:[956:java/lang/Throwable  , 967:java/lang/Throwable  ], 833:[956:java/lang/Throwable  , 967:java/lang/Throwable  ], 836:[956:java/lang/Throwable  , 967:java/lang/Throwable  ], 838:[956:java/lang/Throwable  , 967:java/lang/Throwable  ], 839:[956:java/lang/Throwable  , 967:java/lang/Throwable  ], 842:[956:java/lang/Throwable  , 967:java/lang/Throwable  ], 847:[956:java/lang/Throwable  , 967:java/lang/Throwable  ], 848:[956:java/lang/Throwable  , 967:java/lang/Throwable  ], 853:[956:java/lang/Throwable  , 967:java/lang/Throwable  ], 855:[956:java/lang/Throwable  , 967:java/lang/Throwable  ], 858:[956:java/lang/Throwable  , 967:java/lang/Throwable  ], 860:[956:java/lang/Throwable  , 967:java/lang/Throwable  ], 863:[956:java/lang/Throwable  , 967:java/lang/Throwable  ], 865:[956:java/lang/Throwable  , 967:java/lang/Throwable  ], 868:[956:java/lang/Throwable  , 967:java/lang/Throwable  ], 870:[956:java/lang/Throwable  , 967:java/lang/Throwable  ], 877:[956:java/lang/Throwable  , 967:java/lang/Throwable  ], 880:[956:java/lang/Throwable  , 967:java/lang/Throwable  ], 881:[956:java/lang/Throwable  , 967:java/lang/Throwable  ], 887:[956:java/lang/Throwable  , 967:java/lang/Throwable  ], 891:[956:java/lang/Throwable  , 967:java/lang/Throwable  ], 893:[956:java/lang/Throwable  , 967:java/lang/Throwable  ], 895:[956:java/lang/Throwable  , 967:java/lang/Throwable  ], 896:[956:java/lang/Throwable  , 967:java/lang/Throwable  ], 897:[956:java/lang/Throwable  , 967:java/lang/Throwable  ], 899:[956:java/lang/Throwable  , 967:java/lang/Throwable  ], 905:[956:java/lang/Throwable  , 967:java/lang/Throwable  ], 908:[956:java/lang/Throwable  , 967:java/lang/Throwable  ], 910:[956:java/lang/Throwable  , 967:java/lang/Throwable  ], 915:[956:java/lang/Throwable  , 967:java/lang/Throwable  ], 918:[956:java/lang/Throwable  , 967:java/lang/Throwable  ], 920:[956:java/lang/Throwable  , 967:java/lang/Throwable  ], 931:[956:java/lang/Throwable  , 967:java/lang/Throwable  ], 934:[956:java/lang/Throwable  , 967:java/lang/Throwable  ], 936:[956:java/lang/Throwable  , 967:java/lang/Throwable  ], 938:[956:java/lang/Throwable  , 967:java/lang/Throwable  ], 940:[956:java/lang/Throwable  , 967:java/lang/Throwable  ], 942:[956:java/lang/Throwable  , 967:java/lang/Throwable  ], 950:[956:java/lang/Throwable  , 967:java/lang/Throwable  ], 951:[956:java/lang/Throwable  , 967:java/lang/Throwable  ], 952:[956:java/lang/Throwable  , 967:java/lang/Throwable  ], 954:[956:java/lang/Throwable  , 967:java/lang/Throwable  ], 958:[956:java/lang/Throwable  , 967:java/lang/Throwable  ], 960:[967:java/lang/Throwable  ], 962:[967:java/lang/Throwable  ], 965:[967:java/lang/Throwable  ], 969:[967:java/lang/Throwable  ]]
+TRY [408:[413:java/lang/Exception  ], 410:[413:java/lang/Exception  ], 543:[973:java/lang/Throwable  ], 544:[965:java/lang/Throwable  , 973:java/lang/Throwable  ], 547:[965:java/lang/Throwable  , 973:java/lang/Throwable  ], 549:[965:java/lang/Throwable  , 973:java/lang/Throwable  ], 550:[965:java/lang/Throwable  , 973:java/lang/Throwable  ], 552:[965:java/lang/Throwable  , 973:java/lang/Throwable  ], 553:[965:java/lang/Throwable  , 973:java/lang/Throwable  ], 554:[965:java/lang/Throwable  , 973:java/lang/Throwable  ], 555:[965:java/lang/Throwable  , 973:java/lang/Throwable  ], 562:[965:java/lang/Throwable  , 973:java/lang/Throwable  ], 565:[965:java/lang/Throwable  , 973:java/lang/Throwable  ], 566:[965:java/lang/Throwable  , 973:java/lang/Throwable  ], 573:[965:java/lang/Throwable  , 973:java/lang/Throwable  ], 576:[965:java/lang/Throwable  , 973:java/lang/Throwable  ], 577:[965:java/lang/Throwable  , 973:java/lang/Throwable  ], 584:[965:java/lang/Throwable  , 973:java/lang/Throwable  ], 587:[965:java/lang/Throwable  , 973:java/lang/Throwable  ], 588:[965:java/lang/Throwable  , 973:java/lang/Throwable  ], 593:[965:java/lang/Throwable  , 973:java/lang/Throwable  ], 595:[965:java/lang/Throwable  , 973:java/lang/Throwable  ], 602:[965:java/lang/Throwable  , 973:java/lang/Throwable  ], 603:[965:java/lang/Throwable  , 973:java/lang/Throwable  ], 606:[965:java/lang/Throwable  , 973:java/lang/Throwable  ], 609:[965:java/lang/Throwable  , 973:java/lang/Throwable  ], 612:[965:java/lang/Throwable  , 973:java/lang/Throwable  ], 615:[965:java/lang/Throwable  , 973:java/lang/Throwable  ], 620:[965:java/lang/Throwable  , 973:java/lang/Throwable  ], 623:[965:java/lang/Throwable  , 973:java/lang/Throwable  ], 627:[965:java/lang/Throwable  , 973:java/lang/Throwable  ], 631:[965:java/lang/Throwable  , 973:java/lang/Throwable  ], 635:[965:java/lang/Throwable  , 973:java/lang/Throwable  ], 637:[965:java/lang/Throwable  , 973:java/lang/Throwable  ], 639:[965:java/lang/Throwable  , 973:java/lang/Throwable  ], 640:[965:java/lang/Throwable  , 973:java/lang/Throwable  ], 651:[965:java/lang/Throwable  , 973:java/lang/Throwable  ], 654:[965:java/lang/Throwable  , 973:java/lang/Throwable  ], 658:[965:java/lang/Throwable  , 973:java/lang/Throwable  ], 662:[965:java/lang/Throwable  , 973:java/lang/Throwable  ], 666:[965:java/lang/Throwable  , 973:java/lang/Throwable  ], 667:[965:java/lang/Throwable  , 973:java/lang/Throwable  ], 670:[965:java/lang/Throwable  , 973:java/lang/Throwable  ], 673:[965:java/lang/Throwable  , 973:java/lang/Throwable  ], 674:[965:java/lang/Throwable  , 973:java/lang/Throwable  ], 676:[965:java/lang/Throwable  , 973:java/lang/Throwable  ], 677:[965:java/lang/Throwable  , 973:java/lang/Throwable  ], 688:[965:java/lang/Throwable  , 973:java/lang/Throwable  ], 691:[965:java/lang/Throwable  , 973:java/lang/Throwable  ], 695:[965:java/lang/Throwable  , 973:java/lang/Throwable  ], 699:[965:java/lang/Throwable  , 973:java/lang/Throwable  ], 702:[965:java/lang/Throwable  , 973:java/lang/Throwable  ], 703:[965:java/lang/Throwable  , 973:java/lang/Throwable  ], 705:[965:java/lang/Throwable  , 973:java/lang/Throwable  ], 706:[965:java/lang/Throwable  , 973:java/lang/Throwable  ], 717:[965:java/lang/Throwable  , 973:java/lang/Throwable  ], 720:[965:java/lang/Throwable  , 973:java/lang/Throwable  ], 724:[965:java/lang/Throwable  , 973:java/lang/Throwable  ], 728:[965:java/lang/Throwable  , 973:java/lang/Throwable  ], 731:[965:java/lang/Throwable  , 973:java/lang/Throwable  ], 732:[965:java/lang/Throwable  , 973:java/lang/Throwable  ], 734:[965:java/lang/Throwable  , 973:java/lang/Throwable  ], 735:[965:java/lang/Throwable  , 973:java/lang/Throwable  ], 745:[965:java/lang/Throwable  , 973:java/lang/Throwable  ], 750:[965:java/lang/Throwable  , 973:java/lang/Throwable  ], 752:[965:java/lang/Throwable  , 973:java/lang/Throwable  ], 755:[965:java/lang/Throwable  , 973:java/lang/Throwable  ], 759:[965:java/lang/Throwable  , 973:java/lang/Throwable  ], 767:[965:java/lang/Throwable  , 973:java/lang/Throwable  ], 768:[965:java/lang/Throwable  , 973:java/lang/Throwable  ], 771:[965:java/lang/Throwable  , 973:java/lang/Throwable  ], 774:[965:java/lang/Throwable  , 973:java/lang/Throwable  ], 777:[965:java/lang/Throwable  , 973:java/lang/Throwable  ], 782:[965:java/lang/Throwable  , 973:java/lang/Throwable  ], 785:[965:java/lang/Throwable  , 973:java/lang/Throwable  ], 789:[965:java/lang/Throwable  , 973:java/lang/Throwable  ], 793:[965:java/lang/Throwable  , 973:java/lang/Throwable  ], 800:[965:java/lang/Throwable  , 973:java/lang/Throwable  ], 803:[965:java/lang/Throwable  , 973:java/lang/Throwable  ], 810:[965:java/lang/Throwable  , 973:java/lang/Throwable  ], 811:[965:java/lang/Throwable  , 973:java/lang/Throwable  ], 822:[965:java/lang/Throwable  , 973:java/lang/Throwable  ], 829:[965:java/lang/Throwable  , 973:java/lang/Throwable  ], 831:[965:java/lang/Throwable  , 973:java/lang/Throwable  ], 833:[965:java/lang/Throwable  , 973:java/lang/Throwable  ], 835:[965:java/lang/Throwable  , 973:java/lang/Throwable  ], 843:[965:java/lang/Throwable  , 973:java/lang/Throwable  ], 844:[965:java/lang/Throwable  , 973:java/lang/Throwable  ], 845:[965:java/lang/Throwable  , 973:java/lang/Throwable  ], 847:[965:java/lang/Throwable  , 973:java/lang/Throwable  ], 849:[973:java/lang/Throwable  ], 853:[965:java/lang/Throwable  , 973:java/lang/Throwable  ], 857:[965:java/lang/Throwable  , 973:java/lang/Throwable  ], 860:[965:java/lang/Throwable  , 973:java/lang/Throwable  ], 862:[965:java/lang/Throwable  , 973:java/lang/Throwable  ], 865:[965:java/lang/Throwable  , 973:java/lang/Throwable  ], 867:[965:java/lang/Throwable  , 973:java/lang/Throwable  ], 868:[965:java/lang/Throwable  , 973:java/lang/Throwable  ], 871:[965:java/lang/Throwable  , 973:java/lang/Throwable  ], 876:[965:java/lang/Throwable  , 973:java/lang/Throwable  ], 877:[965:java/lang/Throwable  , 973:java/lang/Throwable  ], 882:[965:java/lang/Throwable  , 973:java/lang/Throwable  ], 884:[965:java/lang/Throwable  , 973:java/lang/Throwable  ], 887:[965:java/lang/Throwable  , 973:java/lang/Throwable  ], 889:[965:java/lang/Throwable  , 973:java/lang/Throwable  ], 892:[965:java/lang/Throwable  , 973:java/lang/Throwable  ], 894:[965:java/lang/Throwable  , 973:java/lang/Throwable  ], 897:[965:java/lang/Throwable  , 973:java/lang/Throwable  ], 899:[965:java/lang/Throwable  , 973:java/lang/Throwable  ], 906:[965:java/lang/Throwable  , 973:java/lang/Throwable  ], 909:[965:java/lang/Throwable  , 973:java/lang/Throwable  ], 910:[965:java/lang/Throwable  , 973:java/lang/Throwable  ], 916:[965:java/lang/Throwable  , 973:java/lang/Throwable  ], 920:[965:java/lang/Throwable  , 973:java/lang/Throwable  ], 922:[965:java/lang/Throwable  , 973:java/lang/Throwable  ], 924:[965:java/lang/Throwable  , 973:java/lang/Throwable  ], 925:[965:java/lang/Throwable  , 973:java/lang/Throwable  ], 926:[965:java/lang/Throwable  , 973:java/lang/Throwable  ], 928:[965:java/lang/Throwable  , 973:java/lang/Throwable  ], 934:[965:java/lang/Throwable  , 973:java/lang/Throwable  ], 937:[965:java/lang/Throwable  , 973:java/lang/Throwable  ], 939:[965:java/lang/Throwable  , 973:java/lang/Throwable  ], 944:[965:java/lang/Throwable  , 973:java/lang/Throwable  ], 947:[965:java/lang/Throwable  , 973:java/lang/Throwable  ], 949:[965:java/lang/Throwable  , 973:java/lang/Throwable  ], 960:[965:java/lang/Throwable  , 973:java/lang/Throwable  ], 963:[965:java/lang/Throwable  , 973:java/lang/Throwable  ], 967:[965:java/lang/Throwable  , 973:java/lang/Throwable  ], 969:[973:java/lang/Throwable  ], 971:[973:java/lang/Throwable  ], 975:[973:java/lang/Throwable  ]]
 ALOAD 1
 INVOKESTATIC local/lito381/server/Protocol.objectSpellRequest([B)Llocal/lito381/server/Protocol$ObjectSpellRequest;
 ALOAD 0
```

### `nativeSourceFireWeaponOriginalBody([BLlocal/lito381/server/BattleSpellCatalog$FireWeaponSpell;)V`

- 類別：`local/lito381/server/ClientSession`
- 類型：例外表涵蓋、含迴圈（路徑比較器不支援）；事件後繼：`LAYOUT_ONLY`
- 比較器：`method nativeSourceFireWeaponOriginalBody([BLlocal/lito381/server/BattleSpellCatalog$FireWeaponSpell;)V: left=213:[388:java/lang/Throwable  ] right=216:[393:java/lang/Throwable  ]; paths rejected: normal control-flow cycle; labelled CFGs differ`

```diff
@@ -1,5 +1,5 @@
 META 0 null [java/io/IOException]
-TRY [213:[388:java/lang/Throwable  ], 216:[388:java/lang/Throwable  ], 218:[388:java/lang/Throwable  ], 219:[388:java/lang/Throwable  ], 221:[388:java/lang/Throwable  ], 222:[388:java/lang/Throwable  ], 223:[388:java/lang/Throwable  ], 224:[388:java/lang/Throwable  ], 231:[388:java/lang/Throwable  ], 235:[388:java/lang/Throwable  ], 238:[388:java/lang/Throwable  ], 241:[388:java/lang/Throwable  ], 243:[388:java/lang/Throwable  ], 244:[388:java/lang/Throwable  ], 248:[388:java/lang/Throwable  ], 249:[388:java/lang/Throwable  ], 252:[388:java/lang/Throwable  ], 253:[388:java/lang/Throwable  ], 257:[388:java/lang/Throwable  ], 259:[388:java/lang/Throwable  ], 261:[388:java/lang/Throwable  ], 262:[388:java/lang/Throwable  ], 265:[388:java/lang/Throwable  ], 271:[388:java/lang/Throwable  ], 272:[388:java/lang/Throwable  ], 274:[388:java/lang/Throwable  ], 275:[388:java/lang/Throwable  ], 278:[388:java/lang/Throwable  ], 280:[388:java/lang/Throwable  ], 281:[388:java/lang/Throwable  ], 286:[388:java/lang/Throwable  ], 288:[388:java/lang/Throwable  ], 289:[388:java/lang/Throwable  ], 292:[388:java/lang/Throwable  ], 293:[388:java/lang/Throwable  ], 294:[388:java/lang/Throwable  ], 295:[388:java/lang/Throwable  ], 298:[388:java/lang/Throwable  ], 300:[388:java/lang/Throwable  ], 303:[388:java/lang/Throwable  ], 308:[388:java/lang/Throwable  ], 310:[388:java/lang/Throwable  ], 311:[388:java/lang/Throwable  ], 312:[388:java/lang/Throwable  ], 313:[388:java/lang/Throwable  ], 315:[388:java/lang/Throwable  ], 319:[388:java/lang/Throwable  ], 320:[388:java/lang/Throwable  ], 321:[388:java/lang/Throwable  ], 325:[388:java/lang/Throwable  ], 328:[388:java/lang/Throwable  ], 330:[388:java/lang/Throwable  ], 332:[388:java/lang/Throwable  ], 333:[388:java/lang/Throwable  ], 336:[388:java/lang/Throwable  ], 337:[388:java/lang/Throwable  ], 340:[388:java/lang/Throwable  ], 342:[388:java/lang/Throwable  ], 343:[388:java/lang/Throwable  ], 348:[388:java/lang/Throwable  ], 351:[388:java/lang/Throwable  ], 352:[388:java/lang/Throwable  ], 354:[388:java/lang/Throwable  ], 358:[388:java/lang/Throwable  ], 361:[388:java/lang/Throwable  ], 366:[388:java/lang/Throwable  ], 368:[388:java/lang/Throwable  ], 371:[388:java/lang/Throwable  ], 374:[388:java/lang/Throwable  ], 377:[388:java/lang/Throwable  ], 380:[388:java/lang/Throwable  ], 383:[388:java/lang/Throwable  ], 386:[388:java/lang/Throwable  ], 390:[388:java/lang/Throwable  ]]
+TRY [216:[393:java/lang/Throwable  ], 219:[393:java/lang/Throwable  ], 221:[393:java/lang/Throwable  ], 222:[393:java/lang/Throwable  ], 224:[393:java/lang/Throwable  ], 225:[393:java/lang/Throwable  ], 226:[393:java/lang/Throwable  ], 227:[393:java/lang/Throwable  ], 234:[393:java/lang/Throwable  ], 238:[393:java/lang/Throwable  ], 241:[393:java/lang/Throwable  ], 244:[393:java/lang/Throwable  ], 246:[393:java/lang/Throwable  ], 247:[393:java/lang/Throwable  ], 251:[393:java/lang/Throwable  ], 252:[393:java/lang/Throwable  ], 255:[393:java/lang/Throwable  ], 256:[393:java/lang/Throwable  ], 259:[393:java/lang/Throwable  ], 265:[393:java/lang/Throwable  ], 266:[393:java/lang/Throwable  ], 268:[393:java/lang/Throwable  ], 269:[393:java/lang/Throwable  ], 272:[393:java/lang/Throwable  ], 274:[393:java/lang/Throwable  ], 275:[393:java/lang/Throwable  ], 280:[393:java/lang/Throwable  ], 282:[393:java/lang/Throwable  ], 283:[393:java/lang/Throwable  ], 286:[393:java/lang/Throwable  ], 287:[393:java/lang/Throwable  ], 288:[393:java/lang/Throwable  ], 289:[393:java/lang/Throwable  ], 292:[393:java/lang/Throwable  ], 294:[393:java/lang/Throwable  ], 297:[393:java/lang/Throwable  ], 302:[393:java/lang/Throwable  ], 304:[393:java/lang/Throwable  ], 305:[393:java/lang/Throwable  ], 306:[393:java/lang/Throwable  ], 307:[393:java/lang/Throwable  ], 309:[393:java/lang/Throwable  ], 313:[393:java/lang/Throwable  ], 315:[393:java/lang/Throwable  ], 317:[393:java/lang/Throwable  ], 318:[393:java/lang/Throwable  ], 322:[393:java/lang/Throwable  ], 323:[393:java/lang/Throwable  ], 324:[393:java/lang/Throwable  ], 328:[393:java/lang/Throwable  ], 331:[393:java/lang/Throwable  ], 333:[393:java/lang/Throwable  ], 335:[393:java/lang/Throwable  ], 336:[393:java/lang/Throwable  ], 339:[393:java/lang/Throwable  ], 340:[393:java/lang/Throwable  ], 343:[393:java/lang/Throwable  ], 345:[393:java/lang/Throwable  ], 346:[393:java/lang/Throwable  ], 350:[393:java/lang/Throwable  ], 354:[393:java/lang/Throwable  ], 357:[393:java/lang/Throwable  ], 359:[393:java/lang/Throwable  ], 363:[393:java/lang/Throwable  ], 366:[393:java/lang/Throwable  ], 371:[393:java/lang/Throwable  ], 373:[393:java/lang/Throwable  ], 376:[393:java/lang/Throwable  ], 379:[393:java/lang/Throwable  ], 382:[393:java/lang/Throwable  ], 385:[393:java/lang/Throwable  ], 388:[393:java/lang/Throwable  ], 391:[393:java/lang/Throwable  ], 395:[393:java/lang/Throwable  ]]
 ALOAD 1
 INVOKESTATIC local/lito381/server/Protocol.objectSpellRequest([B)Llocal/lito381/server/Protocol$ObjectSpellRequest;
 ALOAD 0
```

### `nativeSourceHealingOriginalGroup([BLlocal/lito381/server/BattleSpellCatalog$GroupHealingSpell;)V`

- 類別：`local/lito381/server/ClientSession`
- 類型：例外表涵蓋、含迴圈（路徑比較器不支援）；事件後繼：`LAYOUT_ONLY`
- 比較器：`method nativeSourceHealingOriginalGroup([BLlocal/lito381/server/BattleSpellCatalog$GroupHealingSpell;)V: left=219:[355:java/lang/Throwable  ] right=220:[358:java/lang/Throwable  ]; paths rejected: normal control-flow cycle; labelled CFGs differ`

```diff
@@ -1,5 +1,5 @@
 META 2 null [java/io/IOException]
-TRY [219:[355:java/lang/Throwable  ], 222:[355:java/lang/Throwable  ], 224:[355:java/lang/Throwable  ], 225:[355:java/lang/Throwable  ], 227:[355:java/lang/Throwable  ], 228:[355:java/lang/Throwable  ], 229:[355:java/lang/Throwable  ], 230:[355:java/lang/Throwable  ], 233:[355:java/lang/Throwable  ], 237:[355:java/lang/Throwable  ], 240:[355:java/lang/Throwable  ], 243:[355:java/lang/Throwable  ], 247:[355:java/lang/Throwable  ], 251:[355:java/lang/Throwable  ], 254:[355:java/lang/Throwable  ], 258:[355:java/lang/Throwable  ], 263:[355:java/lang/Throwable  ], 268:[355:java/lang/Throwable  ], 271:[355:java/lang/Throwable  ], 283:[355:java/lang/Throwable  ], 287:[355:java/lang/Throwable  ], 289:[355:java/lang/Throwable  ], 293:[355:java/lang/Throwable  ], 295:[355:java/lang/Throwable  ], 297:[355:java/lang/Throwable  ], 299:[355:java/lang/Throwable  ], 301:[355:java/lang/Throwable  ], 307:[355:java/lang/Throwable  ], 309:[355:java/lang/Throwable  ], 310:[355:java/lang/Throwable  ], 311:[355:java/lang/Throwable  ], 312:[355:java/lang/Throwable  ], 314:[355:java/lang/Throwable  ], 318:[355:java/lang/Throwable  ], 320:[355:java/lang/Throwable  ], 322:[355:java/lang/Throwable  ], 326:[355:java/lang/Throwable  ], 329:[355:java/lang/Throwable  ], 330:[355:java/lang/Throwable  ], 332:[355:java/lang/Throwable  ], 335:[355:java/lang/Throwable  ], 338:[355:java/lang/Throwable  ], 340:[355:java/lang/Throwable  ], 341:[355:java/lang/Throwable  ], 344:[355:java/lang/Throwable  ], 349:[355:java/lang/Throwable  ], 350:[355:java/lang/Throwable  ], 353:[355:java/lang/Throwable  ], 357:[355:java/lang/Throwable  ], 413:[553:java/lang/Throwable  ], 417:[553:java/lang/Throwable  ], 420:[553:java/lang/Throwable  ], 424:[553:java/lang/Throwable  ], 429:[553:java/lang/Throwable  ], 434:[553:java/lang/Throwable  ], 437:[553:java/lang/Throwable  ], 445:[553:java/lang/Throwable  ], 451:[553:java/lang/Throwable  ], 453:[553:java/lang/Throwable  ], 454:[553:java/lang/Throwable  ], 460:[553:java/lang/Throwable  ], 462:[553:java/lang/Throwable  ], 465:[553:java/lang/Throwable  ], 466:[553:java/lang/Throwable  ], 471:[553:java/lang/Throwable  ], 474:[553:java/lang/Throwable  ], 477:[553:java/lang/Throwable  ], 482:[553:java/lang/Throwable  ], 484:[553:java/lang/Throwable  ], 485:[553:java/lang/Throwable  ], 490:[553:java/lang/Throwable  ], 492:[553:java/lang/Throwable  ], 494:[553:java/lang/Throwable  ], 495:[553:java/lang/Throwable  ], 498:[553:java/lang/Throwable  ], 501:[553:java/lang/Throwable  ], 503:[553:java/lang/Throwable  ], 508:[553:java/lang/Throwable  ], 510:[553:java/lang/Throwable  ], 512:[553:java/lang/Throwable  ], 514:[553:java/lang/Throwable  ], 516:[553:java/lang/Throwable  ], 518:[553:java/lang/Throwable  ], 520:[553:java/lang/Throwable  ], 521:[553:java/lang/Throwable  ], 527:[553:java/lang/Throwable  ], 531:[553:java/lang/Throwable  ], 533:[553:java/lang/Throwable  ], 536:[553:java/lang/Throwable  ], 537:[553:java/lang/Throwable  ], 539:[553:java/lang/Throwable  ], 544:[553:java/lang/Throwable  ], 547:[553:java/lang/Throwable  ], 548:[553:java/lang/Throwable  ], 551:[553:java/lang/Throwable  ], 555:[553:java/lang/Throwable  ]]
+TRY [220:[358:java/lang/Throwable  ], 223:[358:java/lang/Throwable  ], 225:[358:java/lang/Throwable  ], 226:[358:java/lang/Throwable  ], 228:[358:java/lang/Throwable  ], 229:[358:java/lang/Throwable  ], 230:[358:java/lang/Throwable  ], 231:[358:java/lang/Throwable  ], 234:[358:java/lang/Throwable  ], 238:[358:java/lang/Throwable  ], 241:[358:java/lang/Throwable  ], 244:[358:java/lang/Throwable  ], 248:[358:java/lang/Throwable  ], 252:[358:java/lang/Throwable  ], 255:[358:java/lang/Throwable  ], 259:[358:java/lang/Throwable  ], 264:[358:java/lang/Throwable  ], 269:[358:java/lang/Throwable  ], 272:[358:java/lang/Throwable  ], 284:[358:java/lang/Throwable  ], 288:[358:java/lang/Throwable  ], 290:[358:java/lang/Throwable  ], 294:[358:java/lang/Throwable  ], 296:[358:java/lang/Throwable  ], 298:[358:java/lang/Throwable  ], 300:[358:java/lang/Throwable  ], 302:[358:java/lang/Throwable  ], 308:[358:java/lang/Throwable  ], 310:[358:java/lang/Throwable  ], 311:[358:java/lang/Throwable  ], 312:[358:java/lang/Throwable  ], 313:[358:java/lang/Throwable  ], 315:[358:java/lang/Throwable  ], 319:[358:java/lang/Throwable  ], 321:[358:java/lang/Throwable  ], 323:[358:java/lang/Throwable  ], 326:[358:java/lang/Throwable  ], 330:[358:java/lang/Throwable  ], 333:[358:java/lang/Throwable  ], 335:[358:java/lang/Throwable  ], 338:[358:java/lang/Throwable  ], 341:[358:java/lang/Throwable  ], 343:[358:java/lang/Throwable  ], 344:[358:java/lang/Throwable  ], 347:[358:java/lang/Throwable  ], 352:[358:java/lang/Throwable  ], 353:[358:java/lang/Throwable  ], 356:[358:java/lang/Throwable  ], 360:[358:java/lang/Throwable  ], 416:[556:java/lang/Throwable  ], 420:[556:java/lang/Throwable  ], 423:[556:java/lang/Throwable  ], 427:[556:java/lang/Throwable  ], 432:[556:java/lang/Throwable  ], 437:[556:java/lang/Throwable  ], 440:[556:java/lang/Throwable  ], 448:[556:java/lang/Throwable  ], 454:[556:java/lang/Throwable  ], 456:[556:java/lang/Throwable  ], 457:[556:java/lang/Throwable  ], 463:[556:java/lang/Throwable  ], 465:[556:java/lang/Throwable  ], 468:[556:java/lang/Throwable  ], 469:[556:java/lang/Throwable  ], 474:[556:java/lang/Throwable  ], 477:[556:java/lang/Throwable  ], 480:[556:java/lang/Throwable  ], 485:[556:java/lang/Throwable  ], 487:[556:java/lang/Throwable  ], 488:[556:java/lang/Throwable  ], 493:[556:java/lang/Throwable  ], 495:[556:java/lang/Throwable  ], 497:[556:java/lang/Throwable  ], 498:[556:java/lang/Throwable  ], 501:[556:java/lang/Throwable  ], 504:[556:java/lang/Throwable  ], 506:[556:java/lang/Throwable  ], 511:[556:java/lang/Throwable  ], 513:[556:java/lang/Throwable  ], 515:[556:java/lang/Throwable  ], 517:[556:java/lang/Throwable  ], 519:[556:java/lang/Throwable  ], 521:[556:java/lang/Throwable  ], 523:[556:java/lang/Throwable  ], 524:[556:java/lang/Throwable  ], 530:[556:java/lang/Throwable  ], 534:[556:java/lang/Throwable  ], 536:[556:java/lang/Throwable  ], 539:[556:java/lang/Throwable  ], 540:[556:java/lang/Throwable  ], 542:[556:java/lang/Throwable  ], 547:[556:java/lang/Throwable  ], 550:[556:java/lang/Throwable  ], 551:[556:java/lang/Throwable  ], 554:[556:java/lang/Throwable  ], 558:[556:java/lang/Throwable  ]]
 ALOAD 1
 INVOKESTATIC local/lito381/server/Protocol.selectorOnlySpellRequest([B)Llocal/lito381/server/Protocol$SpellSelector;
 ASTORE 3
```

### `nativeSourceHealingOriginalNature([BLlocal/lito381/server/BattleSpellCatalog$NaturesBlessingSpell;I)V`

- 類別：`local/lito381/server/ClientSession`
- 類型：例外表涵蓋、含迴圈（路徑比較器不支援）；事件後繼：`LAYOUT_ONLY`
- 比較器：`method nativeSourceHealingOriginalNature([BLlocal/lito381/server/BattleSpellCatalog$NaturesBlessingSpell;I)V: left=279:[408:java/lang/Throwable  ] right=283:[414:java/lang/Throwable  ]; paths rejected: normal control-flow cycle; labelled CFGs differ`

```diff
@@ -1,5 +1,5 @@
 META 2 null [java/io/IOException]
-TRY [279:[408:java/lang/Throwable  ], 282:[408:java/lang/Throwable  ], 284:[408:java/lang/Throwable  ], 285:[408:java/lang/Throwable  ], 287:[408:java/lang/Throwable  ], 288:[408:java/lang/Throwable  ], 289:[408:java/lang/Throwable  ], 290:[408:java/lang/Throwable  ], 293:[408:java/lang/Throwable  ], 297:[408:java/lang/Throwable  ], 300:[408:java/lang/Throwable  ], 303:[408:java/lang/Throwable  ], 307:[408:java/lang/Throwable  ], 311:[408:java/lang/Throwable  ], 314:[408:java/lang/Throwable  ], 318:[408:java/lang/Throwable  ], 323:[408:java/lang/Throwable  ], 328:[408:java/lang/Throwable  ], 331:[408:java/lang/Throwable  ], 343:[408:java/lang/Throwable  ], 349:[408:java/lang/Throwable  ], 355:[408:java/lang/Throwable  ], 356:[408:java/lang/Throwable  ], 358:[408:java/lang/Throwable  ], 359:[408:java/lang/Throwable  ], 361:[408:java/lang/Throwable  ], 363:[408:java/lang/Throwable  ], 369:[408:java/lang/Throwable  ], 371:[408:java/lang/Throwable  ], 372:[408:java/lang/Throwable  ], 373:[408:java/lang/Throwable  ], 374:[408:java/lang/Throwable  ], 376:[408:java/lang/Throwable  ], 380:[408:java/lang/Throwable  ], 381:[408:java/lang/Throwable  ], 382:[408:java/lang/Throwable  ], 386:[408:java/lang/Throwable  ], 389:[408:java/lang/Throwable  ], 390:[408:java/lang/Throwable  ], 392:[408:java/lang/Throwable  ], 396:[408:java/lang/Throwable  ], 399:[408:java/lang/Throwable  ], 404:[408:java/lang/Throwable  ], 406:[408:java/lang/Throwable  ], 410:[408:java/lang/Throwable  ], 446:[600:java/lang/Throwable  ], 450:[600:java/lang/Throwable  ], 453:[600:java/lang/Throwable  ], 459:[600:java/lang/Throwable  ], 464:[600:java/lang/Throwable  ], 466:[600:java/lang/Throwable  ], 467:[600:java/lang/Throwable  ], 474:[600:java/lang/Throwable  ], 480:[600:java/lang/Throwable  ], 482:[600:java/lang/Throwable  ], 484:[600:java/lang/Throwable  ], 485:[600:java/lang/Throwable  ], 488:[600:java/lang/Throwable  ], 491:[600:java/lang/Throwable  ], 495:[600:java/lang/Throwable  ], 500:[600:java/lang/Throwable  ], 505:[600:java/lang/Throwable  ], 508:[600:java/lang/Throwable  ], 518:[600:java/lang/Throwable  ], 523:[600:java/lang/Throwable  ], 524:[600:java/lang/Throwable  ], 533:[600:java/lang/Throwable  ], 534:[600:java/lang/Throwable  ], 537:[600:java/lang/Throwable  ], 539:[600:java/lang/Throwable  ], 545:[600:java/lang/Throwable  ], 551:[600:java/lang/Throwable  ], 553:[600:java/lang/Throwable  ], 555:[600:java/lang/Throwable  ], 557:[600:java/lang/Throwable  ], 559:[600:java/lang/Throwable  ], 561:[600:java/lang/Throwable  ], 562:[600:java/lang/Throwable  ], 563:[600:java/lang/Throwable  ], 566:[600:java/lang/Throwable  ], 567:[600:java/lang/Throwable  ], 573:[600:java/lang/Throwable  ], 577:[600:java/lang/Throwable  ], 579:[600:java/lang/Throwable  ], 581:[600:java/lang/Throwable  ], 582:[600:java/lang/Throwable  ], 583:[600:java/lang/Throwable  ], 585:[600:java/lang/Throwable  ], 589:[600:java/lang/Throwable  ], 594:[600:java/lang/Throwable  ], 595:[600:java/lang/Throwable  ], 598:[600:java/lang/Throwable  ], 602:[600:java/lang/Throwable  ]]
+TRY [283:[414:java/lang/Throwable  ], 286:[414:java/lang/Throwable  ], 288:[414:java/lang/Throwable  ], 289:[414:java/lang/Throwable  ], 291:[414:java/lang/Throwable  ], 292:[414:java/lang/Throwable  ], 293:[414:java/lang/Throwable  ], 294:[414:java/lang/Throwable  ], 297:[414:java/lang/Throwable  ], 301:[414:java/lang/Throwable  ], 304:[414:java/lang/Throwable  ], 307:[414:java/lang/Throwable  ], 311:[414:java/lang/Throwable  ], 315:[414:java/lang/Throwable  ], 318:[414:java/lang/Throwable  ], 322:[414:java/lang/Throwable  ], 327:[414:java/lang/Throwable  ], 332:[414:java/lang/Throwable  ], 335:[414:java/lang/Throwable  ], 347:[414:java/lang/Throwable  ], 353:[414:java/lang/Throwable  ], 359:[414:java/lang/Throwable  ], 360:[414:java/lang/Throwable  ], 362:[414:java/lang/Throwable  ], 363:[414:java/lang/Throwable  ], 365:[414:java/lang/Throwable  ], 367:[414:java/lang/Throwable  ], 373:[414:java/lang/Throwable  ], 375:[414:java/lang/Throwable  ], 376:[414:java/lang/Throwable  ], 377:[414:java/lang/Throwable  ], 378:[414:java/lang/Throwable  ], 380:[414:java/lang/Throwable  ], 384:[414:java/lang/Throwable  ], 385:[414:java/lang/Throwable  ], 386:[414:java/lang/Throwable  ], 389:[414:java/lang/Throwable  ], 393:[414:java/lang/Throwable  ], 396:[414:java/lang/Throwable  ], 398:[414:java/lang/Throwable  ], 402:[414:java/lang/Throwable  ], 405:[414:java/lang/Throwable  ], 410:[414:java/lang/Throwable  ], 412:[414:java/lang/Throwable  ], 416:[414:java/lang/Throwable  ], 452:[606:java/lang/Throwable  ], 456:[606:java/lang/Throwable  ], 459:[606:java/lang/Throwable  ], 465:[606:java/lang/Throwable  ], 470:[606:java/lang/Throwable  ], 472:[606:java/lang/Throwable  ], 473:[606:java/lang/Throwable  ], 480:[606:java/lang/Throwable  ], 486:[606:java/lang/Throwable  ], 488:[606:java/lang/Throwable  ], 490:[606:java/lang/Throwable  ], 491:[606:java/lang/Throwable  ], 494:[606:java/lang/Throwable  ], 497:[606:java/lang/Throwable  ], 501:[606:java/lang/Throwable  ], 506:[606:java/lang/Throwable  ], 511:[606:java/lang/Throwable  ], 514:[606:java/lang/Throwable  ], 524:[606:java/lang/Throwable  ], 529:[606:java/lang/Throwable  ], 530:[606:java/lang/Throwable  ], 539:[606:java/lang/Throwable  ], 540:[606:java/lang/Throwable  ], 543:[606:java/lang/Throwable  ], 545:[606:java/lang/Throwable  ], 551:[606:java/lang/Throwable  ], 557:[606:java/lang/Throwable  ], 559:[606:java/lang/Throwable  ], 561:[606:java/lang/Throwable  ], 563:[606:java/lang/Throwable  ], 565:[606:java/lang/Throwable  ], 567:[606:java/lang/Throwable  ], 568:[606:java/lang/Throwable  ], 569:[606:java/lang/Throwable  ], 572:[606:java/lang/Throwable  ], 573:[606:java/lang/Throwable  ], 579:[606:java/lang/Throwable  ], 583:[606:java/lang/Throwable  ], 585:[606:java/lang/Throwable  ], 587:[606:java/lang/Throwable  ], 588:[606:java/lang/Throwable  ], 589:[606:java/lang/Throwable  ], 591:[606:java/lang/Throwable  ], 595:[606:java/lang/Throwable  ], 600:[606:java/lang/Throwable  ], 601:[606:java/lang/Throwable  ], 604:[606:java/lang/Throwable  ], 608:[606:java/lang/Throwable  ]]
 ILOAD 3
 IFLT L6
 ILOAD 3
```

### `nativeSourceRemainingOriginal133([BLlocal/lito381/server/BattleSpellCatalog$ElementalFallDownSpell;I)V`

- 類別：`local/lito381/server/ClientSession`
- 類型：例外表涵蓋、含迴圈（路徑比較器不支援）；事件後繼：`LAYOUT_ONLY`
- 比較器：`method nativeSourceRemainingOriginal133([BLlocal/lito381/server/BattleSpellCatalog$ElementalFallDownSpell;I)V: left=453:[458:java/lang/Exception  ] right=455:[460:java/lang/Exception  ]; paths rejected: normal control-flow cycle; labelled CFGs differ`

```diff
@@ -1,5 +1,5 @@
 META 0 null [java/io/IOException]
-TRY [453:[458:java/lang/Exception  ], 455:[458:java/lang/Exception  ], 671:[835:java/lang/Throwable  ], 674:[835:java/lang/Throwable  ], 676:[835:java/lang/Throwable  ], 677:[835:java/lang/Throwable  ], 679:[835:java/lang/Throwable  ], 680:[835:java/lang/Throwable  ], 681:[835:java/lang/Throwable  ], 682:[835:java/lang/Throwable  ], 687:[835:java/lang/Throwable  ], 688:[835:java/lang/Throwable  ], 693:[835:java/lang/Throwable  ], 694:[835:java/lang/Throwable  ], 698:[835:java/lang/Throwable  ], 702:[835:java/lang/Throwable  ], 705:[835:java/lang/Throwable  ], 708:[835:java/lang/Throwable  ], 710:[835:java/lang/Throwable  ], 717:[835:java/lang/Throwable  ], 718:[835:java/lang/Throwable  ], 721:[835:java/lang/Throwable  ], 728:[835:java/lang/Throwable  ], 729:[835:java/lang/Throwable  ], 738:[835:java/lang/Throwable  ], 747:[835:java/lang/Throwable  ], 752:[835:java/lang/Throwable  ], 755:[835:java/lang/Throwable  ], 762:[835:java/lang/Throwable  ], 766:[835:java/lang/Throwable  ], 769:[835:java/lang/Throwable  ], 777:[835:java/lang/Throwable  ], 779:[835:java/lang/Throwable  ], 781:[835:java/lang/Throwable  ], 782:[835:java/lang/Throwable  ], 784:[835:java/lang/Throwable  ], 786:[835:java/lang/Throwable  ], 788:[835:java/lang/Throwable  ], 794:[835:java/lang/Throwable  ], 795:[835:java/lang/Throwable  ], 796:[835:java/lang/Throwable  ], 798:[835:java/lang/Throwable  ], 807:[835:java/lang/Throwable  ], 808:[835:java/lang/Throwable  ], 809:[835:java/lang/Throwable  ], 813:[835:java/lang/Throwable  ], 816:[835:java/lang/Throwable  ], 817:[835:java/lang/Throwable  ], 819:[835:java/lang/Throwable  ], 823:[835:java/lang/Throwable  ], 826:[835:java/lang/Throwable  ], 831:[835:java/lang/Throwable  ], 833:[835:java/lang/Throwable  ], 837:[835:java/lang/Throwable  ]]
+TRY [455:[460:java/lang/Exception  ], 457:[460:java/lang/Exception  ], 673:[839:java/lang/Throwable  ], 676:[839:java/lang/Throwable  ], 678:[839:java/lang/Throwable  ], 679:[839:java/lang/Throwable  ], 681:[839:java/lang/Throwable  ], 682:[839:java/lang/Throwable  ], 683:[839:java/lang/Throwable  ], 684:[839:java/lang/Throwable  ], 689:[839:java/lang/Throwable  ], 690:[839:java/lang/Throwable  ], 695:[839:java/lang/Throwable  ], 696:[839:java/lang/Throwable  ], 700:[839:java/lang/Throwable  ], 704:[839:java/lang/Throwable  ], 707:[839:java/lang/Throwable  ], 710:[839:java/lang/Throwable  ], 712:[839:java/lang/Throwable  ], 719:[839:java/lang/Throwable  ], 720:[839:java/lang/Throwable  ], 723:[839:java/lang/Throwable  ], 730:[839:java/lang/Throwable  ], 731:[839:java/lang/Throwable  ], 740:[839:java/lang/Throwable  ], 749:[839:java/lang/Throwable  ], 754:[839:java/lang/Throwable  ], 757:[839:java/lang/Throwable  ], 764:[839:java/lang/Throwable  ], 768:[839:java/lang/Throwable  ], 771:[839:java/lang/Throwable  ], 777:[839:java/lang/Throwable  ], 779:[839:java/lang/Throwable  ], 781:[839:java/lang/Throwable  ], 787:[839:java/lang/Throwable  ], 788:[839:java/lang/Throwable  ], 789:[839:java/lang/Throwable  ], 791:[839:java/lang/Throwable  ], 795:[839:java/lang/Throwable  ], 797:[839:java/lang/Throwable  ], 799:[839:java/lang/Throwable  ], 800:[839:java/lang/Throwable  ], 809:[839:java/lang/Throwable  ], 810:[839:java/lang/Throwable  ], 811:[839:java/lang/Throwable  ], 814:[839:java/lang/Throwable  ], 818:[839:java/lang/Throwable  ], 821:[839:java/lang/Throwable  ], 823:[839:java/lang/Throwable  ], 827:[839:java/lang/Throwable  ], 830:[839:java/lang/Throwable  ], 835:[839:java/lang/Throwable  ], 837:[839:java/lang/Throwable  ], 841:[839:java/lang/Throwable  ]]
 ILOAD 3
 IFLT L5
 ILOAD 3
```

### `nativeSourceRemainingOriginal18([BLlocal/lito381/server/BattleSpellCatalog$TurnUndeadSpell;)V`

- 類別：`local/lito381/server/ClientSession`
- 類型：例外表涵蓋、含迴圈（路徑比較器不支援）；事件後繼：`LAYOUT_ONLY`
- 比較器：`method nativeSourceRemainingOriginal18([BLlocal/lito381/server/BattleSpellCatalog$TurnUndeadSpell;)V: left=237:[242:java/lang/Exception  ] right=239:[244:java/lang/Exception  ]; paths rejected: normal control-flow cycle; labelled CFGs differ`

```diff
@@ -1,5 +1,5 @@
 META 0 null [java/io/IOException]
-TRY [237:[242:java/lang/Exception  ], 239:[242:java/lang/Exception  ], 274:[362:java/lang/Throwable  ], 277:[362:java/lang/Throwable  ], 279:[362:java/lang/Throwable  ], 280:[362:java/lang/Throwable  ], 282:[362:java/lang/Throwable  ], 283:[362:java/lang/Throwable  ], 284:[362:java/lang/Throwable  ], 285:[362:java/lang/Throwable  ], 292:[362:java/lang/Throwable  ], 296:[362:java/lang/Throwable  ], 299:[362:java/lang/Throwable  ], 300:[362:java/lang/Throwable  ], 303:[362:java/lang/Throwable  ], 307:[362:java/lang/Throwable  ], 309:[362:java/lang/Throwable  ], 311:[362:java/lang/Throwable  ], 312:[362:java/lang/Throwable  ], 314:[362:java/lang/Throwable  ], 316:[362:java/lang/Throwable  ], 318:[362:java/lang/Throwable  ], 323:[362:java/lang/Throwable  ], 324:[362:java/lang/Throwable  ], 325:[362:java/lang/Throwable  ], 327:[362:java/lang/Throwable  ], 331:[362:java/lang/Throwable  ], 332:[362:java/lang/Throwable  ], 333:[362:java/lang/Throwable  ], 337:[362:java/lang/Throwable  ], 341:[362:java/lang/Throwable  ], 343:[362:java/lang/Throwable  ], 344:[362:java/lang/Throwable  ], 346:[362:java/lang/Throwable  ], 350:[362:java/lang/Throwable  ], 353:[362:java/lang/Throwable  ], 358:[362:java/lang/Throwable  ], 360:[362:java/lang/Throwable  ], 364:[362:java/lang/Throwable  ]]
+TRY [239:[244:java/lang/Exception  ], 241:[244:java/lang/Exception  ], 276:[366:java/lang/Throwable  ], 279:[366:java/lang/Throwable  ], 281:[366:java/lang/Throwable  ], 282:[366:java/lang/Throwable  ], 284:[366:java/lang/Throwable  ], 285:[366:java/lang/Throwable  ], 286:[366:java/lang/Throwable  ], 287:[366:java/lang/Throwable  ], 294:[366:java/lang/Throwable  ], 298:[366:java/lang/Throwable  ], 301:[366:java/lang/Throwable  ], 302:[366:java/lang/Throwable  ], 305:[366:java/lang/Throwable  ], 307:[366:java/lang/Throwable  ], 309:[366:java/lang/Throwable  ], 311:[366:java/lang/Throwable  ], 316:[366:java/lang/Throwable  ], 317:[366:java/lang/Throwable  ], 318:[366:java/lang/Throwable  ], 320:[366:java/lang/Throwable  ], 324:[366:java/lang/Throwable  ], 326:[366:java/lang/Throwable  ], 328:[366:java/lang/Throwable  ], 329:[366:java/lang/Throwable  ], 333:[366:java/lang/Throwable  ], 334:[366:java/lang/Throwable  ], 335:[366:java/lang/Throwable  ], 338:[366:java/lang/Throwable  ], 342:[366:java/lang/Throwable  ], 345:[366:java/lang/Throwable  ], 348:[366:java/lang/Throwable  ], 350:[366:java/lang/Throwable  ], 354:[366:java/lang/Throwable  ], 357:[366:java/lang/Throwable  ], 362:[366:java/lang/Throwable  ], 364:[366:java/lang/Throwable  ], 368:[366:java/lang/Throwable  ]]
 ALOAD 1
 INVOKESTATIC local/lito381/server/Protocol.attackSpellRequest([B)Llocal/lito381/server/Protocol$AttackSpellRequest;
 ALOAD 0
```

### `nativeSourceRemainingOriginal39([BLlocal/lito381/server/BattleSpellCatalog$ManaDrainSpell;)V`

- 類別：`local/lito381/server/ClientSession`
- 類型：例外表涵蓋、含迴圈（路徑比較器不支援）；事件後繼：`LAYOUT_ONLY`
- 比較器：`method nativeSourceRemainingOriginal39([BLlocal/lito381/server/BattleSpellCatalog$ManaDrainSpell;)V: left=288:[293:java/lang/Exception  ] right=291:[296:java/lang/Exception  ]; paths rejected: normal control-flow cycle; labelled CFGs differ`

```diff
@@ -1,5 +1,5 @@
 META 0 null [java/io/IOException]
-TRY [288:[293:java/lang/Exception  ], 290:[293:java/lang/Exception  ], 453:[566:java/lang/Throwable  ], 458:[566:java/lang/Throwable  ], 460:[566:java/lang/Throwable  ], 461:[566:java/lang/Throwable  ], 464:[566:java/lang/Throwable  ], 467:[566:java/lang/Throwable  ], 469:[566:java/lang/Throwable  ], 470:[566:java/lang/Throwable  ], 472:[566:java/lang/Throwable  ], 473:[566:java/lang/Throwable  ], 474:[566:java/lang/Throwable  ], 475:[566:java/lang/Throwable  ], 482:[566:java/lang/Throwable  ], 486:[566:java/lang/Throwable  ], 488:[566:java/lang/Throwable  ], 491:[566:java/lang/Throwable  ], 492:[566:java/lang/Throwable  ], 495:[566:java/lang/Throwable  ], 499:[566:java/lang/Throwable  ], 501:[566:java/lang/Throwable  ], 503:[566:java/lang/Throwable  ], 504:[566:java/lang/Throwable  ], 506:[566:java/lang/Throwable  ], 508:[566:java/lang/Throwable  ], 510:[566:java/lang/Throwable  ], 512:[566:java/lang/Throwable  ], 514:[566:java/lang/Throwable  ], 520:[566:java/lang/Throwable  ], 521:[566:java/lang/Throwable  ], 522:[566:java/lang/Throwable  ], 524:[566:java/lang/Throwable  ], 528:[566:java/lang/Throwable  ], 529:[566:java/lang/Throwable  ], 530:[566:java/lang/Throwable  ], 534:[566:java/lang/Throwable  ], 537:[566:java/lang/Throwable  ], 538:[566:java/lang/Throwable  ], 541:[566:java/lang/Throwable  ], 543:[566:java/lang/Throwable  ], 545:[566:java/lang/Throwable  ], 547:[566:java/lang/Throwable  ], 550:[566:java/lang/Throwable  ], 554:[566:java/lang/Throwable  ], 557:[566:java/lang/Throwable  ], 562:[566:java/lang/Throwable  ], 564:[566:java/lang/Throwable  ], 568:[566:java/lang/Throwable  ], 717:[740:java/lang/Throwable  ], 721:[740:java/lang/Throwable  ], 726:[740:java/lang/Throwable  ], 729:[740:java/lang/Throwable  ], 730:[740:java/lang/Throwable  ], 732:[740:java/lang/Throwable  ], 735:[740:java/lang/Throwable  ], 738:[740:java/lang/Throwable  ], 742:[740:java/lang/Throwable  ], 846:[860:java/lang/Throwable  ], 848:[860:java/lang/Throwable  ], 851:[860:java/lang/Throwable  ], 852:[860:java/lang/Throwable  ], 853:[860:java/lang/Throwable  ], 855:[860:java/lang/Throwable  ], 858:[860:java/lang/Throwable  ], 862:[860:java/lang/Throwable  ]]
+TRY [291:[296:java/lang/Exception  ], 293:[296:java/lang/Exception  ], 456:[571:java/lang/Throwable  ], 461:[571:java/lang/Throwable  ], 463:[571:java/lang/Throwable  ], 464:[571:java/lang/Throwable  ], 467:[571:java/lang/Throwable  ], 470:[571:java/lang/Throwable  ], 472:[571:java/lang/Throwable  ], 473:[571:java/lang/Throwable  ], 475:[571:java/lang/Throwable  ], 476:[571:java/lang/Throwable  ], 477:[571:java/lang/Throwable  ], 478:[571:java/lang/Throwable  ], 485:[571:java/lang/Throwable  ], 489:[571:java/lang/Throwable  ], 491:[571:java/lang/Throwable  ], 494:[571:java/lang/Throwable  ], 495:[571:java/lang/Throwable  ], 498:[571:java/lang/Throwable  ], 500:[571:java/lang/Throwable  ], 502:[571:java/lang/Throwable  ], 504:[571:java/lang/Throwable  ], 506:[571:java/lang/Throwable  ], 508:[571:java/lang/Throwable  ], 514:[571:java/lang/Throwable  ], 515:[571:java/lang/Throwable  ], 516:[571:java/lang/Throwable  ], 518:[571:java/lang/Throwable  ], 522:[571:java/lang/Throwable  ], 524:[571:java/lang/Throwable  ], 526:[571:java/lang/Throwable  ], 527:[571:java/lang/Throwable  ], 531:[571:java/lang/Throwable  ], 532:[571:java/lang/Throwable  ], 533:[571:java/lang/Throwable  ], 536:[571:java/lang/Throwable  ], 540:[571:java/lang/Throwable  ], 543:[571:java/lang/Throwable  ], 546:[571:java/lang/Throwable  ], 548:[571:java/lang/Throwable  ], 550:[571:java/lang/Throwable  ], 552:[571:java/lang/Throwable  ], 555:[571:java/lang/Throwable  ], 559:[571:java/lang/Throwable  ], 562:[571:java/lang/Throwable  ], 567:[571:java/lang/Throwable  ], 569:[571:java/lang/Throwable  ], 573:[571:java/lang/Throwable  ], 723:[748:java/lang/Throwable  ], 727:[748:java/lang/Throwable  ], 731:[748:java/lang/Throwable  ], 735:[748:java/lang/Throwable  ], 738:[748:java/lang/Throwable  ], 740:[748:java/lang/Throwable  ], 743:[748:java/lang/Throwable  ], 746:[748:java/lang/Throwable  ], 750:[748:java/lang/Throwable  ], 853:[870:java/lang/Throwable  ], 855:[870:java/lang/Throwable  ], 858:[870:java/lang/Throwable  ], 860:[870:java/lang/Throwable  ], 863:[870:java/lang/Throwable  ], 865:[870:java/lang/Throwable  ], 868:[870:java/lang/Throwable  ], 872:[870:java/lang/Throwable  ]]
 ALOAD 1
 INVOKESTATIC local/lito381/server/Protocol.attackSpellRequest([B)Llocal/lito381/server/Protocol$AttackSpellRequest;
 ALOAD 0
```

### `nativeSourceWindShotOriginalBody([BLlocal/lito381/server/BattleSpellCatalog$WindShotSpell;)V`

- 類別：`local/lito381/server/ClientSession`
- 類型：例外表涵蓋、含迴圈（路徑比較器不支援）；事件後繼：`LAYOUT_ONLY`
- 比較器：`method nativeSourceWindShotOriginalBody([BLlocal/lito381/server/BattleSpellCatalog$WindShotSpell;)V: left=162:[321:java/lang/Throwable  ] right=165:[326:java/lang/Throwable  ]; paths rejected: normal control-flow cycle; labelled CFGs differ`

```diff
@@ -1,5 +1,5 @@
 META 0 null [java/io/IOException]
-TRY [162:[321:java/lang/Throwable  ], 165:[321:java/lang/Throwable  ], 167:[321:java/lang/Throwable  ], 168:[321:java/lang/Throwable  ], 170:[321:java/lang/Throwable  ], 171:[321:java/lang/Throwable  ], 172:[321:java/lang/Throwable  ], 173:[321:java/lang/Throwable  ], 180:[321:java/lang/Throwable  ], 184:[321:java/lang/Throwable  ], 187:[321:java/lang/Throwable  ], 190:[321:java/lang/Throwable  ], 192:[321:java/lang/Throwable  ], 193:[321:java/lang/Throwable  ], 197:[321:java/lang/Throwable  ], 198:[321:java/lang/Throwable  ], 201:[321:java/lang/Throwable  ], 202:[321:java/lang/Throwable  ], 206:[321:java/lang/Throwable  ], 208:[321:java/lang/Throwable  ], 210:[321:java/lang/Throwable  ], 211:[321:java/lang/Throwable  ], 214:[321:java/lang/Throwable  ], 220:[321:java/lang/Throwable  ], 221:[321:java/lang/Throwable  ], 223:[321:java/lang/Throwable  ], 226:[321:java/lang/Throwable  ], 228:[321:java/lang/Throwable  ], 229:[321:java/lang/Throwable  ], 234:[321:java/lang/Throwable  ], 236:[321:java/lang/Throwable  ], 237:[321:java/lang/Throwable  ], 239:[321:java/lang/Throwable  ], 240:[321:java/lang/Throwable  ], 241:[321:java/lang/Throwable  ], 242:[321:java/lang/Throwable  ], 244:[321:java/lang/Throwable  ], 246:[321:java/lang/Throwable  ], 249:[321:java/lang/Throwable  ], 254:[321:java/lang/Throwable  ], 256:[321:java/lang/Throwable  ], 257:[321:java/lang/Throwable  ], 258:[321:java/lang/Throwable  ], 259:[321:java/lang/Throwable  ], 261:[321:java/lang/Throwable  ], 265:[321:java/lang/Throwable  ], 266:[321:java/lang/Throwable  ], 267:[321:java/lang/Throwable  ], 269:[321:java/lang/Throwable  ], 270:[321:java/lang/Throwable  ], 273:[321:java/lang/Throwable  ], 275:[321:java/lang/Throwable  ], 276:[321:java/lang/Throwable  ], 281:[321:java/lang/Throwable  ], 284:[321:java/lang/Throwable  ], 285:[321:java/lang/Throwable  ], 287:[321:java/lang/Throwable  ], 291:[321:java/lang/Throwable  ], 294:[321:java/lang/Throwable  ], 299:[321:java/lang/Throwable  ], 301:[321:java/lang/Throwable  ], 304:[321:java/lang/Throwable  ], 307:[321:java/lang/Throwable  ], 310:[321:java/lang/Throwable  ], 313:[321:java/lang/Throwable  ], 316:[321:java/lang/Throwable  ], 319:[321:java/lang/Throwable  ], 323:[321:java/lang/Throwable  ]]
+TRY [165:[326:java/lang/Throwable  ], 168:[326:java/lang/Throwable  ], 170:[326:java/lang/Throwable  ], 171:[326:java/lang/Throwable  ], 173:[326:java/lang/Throwable  ], 174:[326:java/lang/Throwable  ], 175:[326:java/lang/Throwable  ], 176:[326:java/lang/Throwable  ], 183:[326:java/lang/Throwable  ], 187:[326:java/lang/Throwable  ], 190:[326:java/lang/Throwable  ], 193:[326:java/lang/Throwable  ], 195:[326:java/lang/Throwable  ], 196:[326:java/lang/Throwable  ], 200:[326:java/lang/Throwable  ], 201:[326:java/lang/Throwable  ], 204:[326:java/lang/Throwable  ], 205:[326:java/lang/Throwable  ], 208:[326:java/lang/Throwable  ], 214:[326:java/lang/Throwable  ], 215:[326:java/lang/Throwable  ], 217:[326:java/lang/Throwable  ], 220:[326:java/lang/Throwable  ], 222:[326:java/lang/Throwable  ], 223:[326:java/lang/Throwable  ], 228:[326:java/lang/Throwable  ], 230:[326:java/lang/Throwable  ], 231:[326:java/lang/Throwable  ], 233:[326:java/lang/Throwable  ], 234:[326:java/lang/Throwable  ], 235:[326:java/lang/Throwable  ], 236:[326:java/lang/Throwable  ], 238:[326:java/lang/Throwable  ], 240:[326:java/lang/Throwable  ], 243:[326:java/lang/Throwable  ], 248:[326:java/lang/Throwable  ], 250:[326:java/lang/Throwable  ], 251:[326:java/lang/Throwable  ], 252:[326:java/lang/Throwable  ], 253:[326:java/lang/Throwable  ], 255:[326:java/lang/Throwable  ], 259:[326:java/lang/Throwable  ], 261:[326:java/lang/Throwable  ], 263:[326:java/lang/Throwable  ], 264:[326:java/lang/Throwable  ], 268:[326:java/lang/Throwable  ], 269:[326:java/lang/Throwable  ], 270:[326:java/lang/Throwable  ], 272:[326:java/lang/Throwable  ], 273:[326:java/lang/Throwable  ], 276:[326:java/lang/Throwable  ], 278:[326:java/lang/Throwable  ], 279:[326:java/lang/Throwable  ], 283:[326:java/lang/Throwable  ], 287:[326:java/lang/Throwable  ], 290:[326:java/lang/Throwable  ], 292:[326:java/lang/Throwable  ], 296:[326:java/lang/Throwable  ], 299:[326:java/lang/Throwable  ], 304:[326:java/lang/Throwable  ], 306:[326:java/lang/Throwable  ], 309:[326:java/lang/Throwable  ], 312:[326:java/lang/Throwable  ], 315:[326:java/lang/Throwable  ], 318:[326:java/lang/Throwable  ], 321:[326:java/lang/Throwable  ], 324:[326:java/lang/Throwable  ], 328:[326:java/lang/Throwable  ]]
 ALOAD 1
 INVOKESTATIC local/lito381/server/Protocol.objectSpellRequest([B)Llocal/lito381/server/Protocol$ObjectSpellRequest;
 ALOAD 0
```

### `publishNpcCounterBarrierTrigger(JLlocal/lito381/server/BattleCounterBarrier$Decision;Ljava/util/List;Ljava/lang/String;)V`

- 類別：`local/lito381/server/ClientSession`
- 類型：分支排列、含迴圈（路徑比較器不支援）；事件後繼：`LAYOUT_ONLY`
- 比較器：`method publishNpcCounterBarrierTrigger(JLlocal/lito381/server/BattleCounterBarrier$Decision;Ljava/util/List;Ljava/lang/String;)V insn[18]: left=IFEQ L53 right=IFEQ L54; paths rejected: normal control-flow cycle; labelled CFGs differ`

```diff
@@ -18,7 +18,7 @@
 ASTORE 9
 ALOAD 9
 INVOKEINTERFACE java/util/Iterator.hasNext()Z itf
-IFEQ L53
+IFEQ L54
 ALOAD 9
 INVOKEINTERFACE java/util/Iterator.next()Ljava/lang/Object; itf
 CHECKCAST local/lito381/server/ClientSession
```

### `publishNpcCounterMirrorTrigger(JIILlocal/lito381/server/BattleCounterMirrorStatus$Decision;Ljava/util/List;Ljava/lang/String;)V`

- 類別：`local/lito381/server/ClientSession`
- 類型：分支排列、含迴圈（路徑比較器不支援）；事件後繼：`LAYOUT_ONLY`
- 比較器：`method publishNpcCounterMirrorTrigger(JIILlocal/lito381/server/BattleCounterMirrorStatus$Decision;Ljava/util/List;Ljava/lang/String;)V insn[22]: left=IFEQ L76 right=IFEQ L77; paths rejected: normal control-flow cycle; labelled CFGs differ`

```diff
@@ -22,7 +22,7 @@
 ASTORE 11
 ALOAD 11
 INVOKEINTERFACE java/util/Iterator.hasNext()Z itf
-IFEQ L76
+IFEQ L77
 ALOAD 11
 INVOKEINTERFACE java/util/Iterator.next()Ljava/lang/Object; itf
 CHECKCAST local/lito381/server/ClientSession
```

### `resolveDragonKnightNegativeTarget(Llocal/lito381/server/ClientSession$Phase;JIIIIIZ)Llocal/lito381/server/ClientSession$ArmorBreakTarget;`

- 類別：`local/lito381/server/ClientSession`
- 類型：分支排列、含迴圈（路徑比較器不支援）；事件後繼：`LAYOUT_ONLY`
- 比較器：`method resolveDragonKnightNegativeTarget(Llocal/lito381/server/ClientSession$Phase;JIIIIIZ)Llocal/lito381/server/ClientSession$ArmorBreakTarget; insn[6]: left=IF_ACMPNE L268 right=IF_ACMPNE L265; paths rejected: normal control-flow cycle; labelled CFGs differ`

```diff
@@ -6,7 +6,7 @@
 ASTORE 10
 ALOAD 1
 GETSTATIC local/lito381/server/ClientSession$Phase.WORLD:Llocal/lito381/server/ClientSession$Phase;
-IF_ACMPNE L268
+IF_ACMPNE L265
 ILOAD 4
 INVOKESTATIC local/lito381/server/ClassicWorldMapCatalog.forMap(I)Llocal/lito381/server/ClassicWorldMapCatalog$ClassicMap;
 ASTORE 11
```

### `serverCompanionTick(Llocal/lito381/server/BattleCompanion;JJ)V`

- 類別：`local/lito381/server/ClientSession`
- 類型：例外表涵蓋、含迴圈（路徑比較器不支援）；事件後繼：`LAYOUT_ONLY`
- 比較器：`method serverCompanionTick(Llocal/lito381/server/BattleCompanion;JJ)V: left=323:[822:java/lang/Throwable  ] right=325:[822:java/lang/Throwable  ]; paths rejected: normal control-flow cycle; labelled CFGs differ`

```diff
@@ -1,5 +1,5 @@
 META 2 null [java/io/IOException]
-TRY [7:[822:java/lang/Throwable  ], 10:[822:java/lang/Throwable  ], 11:[822:java/lang/Throwable  ], 14:[822:java/lang/Throwable  ], 17:[822:java/lang/Throwable  ], 22:[822:java/lang/Throwable  ], 25:[822:java/lang/Throwable  ], 27:[822:java/lang/Throwable  ], 30:[822:java/lang/Throwable  ], 35:[822:java/lang/Throwable  ], 37:[822:java/lang/Throwable  ], 40:[822:java/lang/Throwable  ], 43:[822:java/lang/Throwable  ], 49:[822:java/lang/Throwable  ], 52:[822:java/lang/Throwable  ], 55:[822:java/lang/Throwable  ], 57:[822:java/lang/Throwable  ], 60:[822:java/lang/Throwable  ], 65:[822:java/lang/Throwable  ], 67:[822:java/lang/Throwable  ], 71:[822:java/lang/Throwable  ], 73:[822:java/lang/Throwable  ], 80:[822:java/lang/Throwable  ], 82:[822:java/lang/Throwable  ], 83:[822:java/lang/Throwable  ], 84:[822:java/lang/Throwable  ], 86:[822:java/lang/Throwable  ], 87:[822:java/lang/Throwable  ], 95:[822:java/lang/Throwable  ], 96:[822:java/lang/Throwable  ], 99:[822:java/lang/Throwable  ], 102:[822:java/lang/Throwable  ], 105:[822:java/lang/Throwable  ], 109:[822:java/lang/Throwable  ], 116:[822:java/lang/Throwable  ], 119:[822:java/lang/Throwable  ], 120:[822:java/lang/Throwable  ], 123:[822:java/lang/Throwable  ], 126:[822:java/lang/Throwable  ], 127:[822:java/lang/Throwable  ], 132:[822:java/lang/Throwable  ], 136:[822:java/lang/Throwable  ], 137:[822:java/lang/Throwable  ], 146:[822:java/lang/Throwable  ], 150:[822:java/lang/Throwable  ], 151:[822:java/lang/Throwable  ], 166:[822:java/lang/Throwable  ], 167:[822:java/lang/Throwable  ], 169:[822:java/lang/Throwable  ], 171:[822:java/lang/Throwable  ], 172:[822:java/lang/Throwable  ], 173:[822:java/lang/Throwable  ], 175:[822:java/lang/Throwable  ], 180:[822:java/lang/Throwable  ], 182:[822:java/lang/Throwable  ], 191:[822:java/lang/Throwable  ], 193:[822:java/lang/Throwable  ], 198:[822:java/lang/Throwable  ], 203:[822:java/lang/Throwable  ], 208:[822:java/lang/Throwable  ], 211:[822:java/lang/Throwable  ], 216:[822:java/lang/Throwable  ], 221:[822:java/lang/Throwable  ], 226:[822:java/lang/Throwable  ], 229:[822:java/lang/Throwable  ], 232:[822:java/lang/Throwable  ], 235:[822:java/lang/Throwable  ], 237:[822:java/lang/Throwable  ], 240:[822:java/lang/Throwable  ], 241:[822:java/lang/Throwable  ], 247:[822:java/lang/Throwable  ], 250:[822:java/lang/Throwable  ], 251:[822:java/lang/Throwable  ], 260:[822:java/lang/Throwable  ], 261:[822:java/lang/Throwable  ], 268:[822:java/lang/Throwable  ], 270:[822:java/lang/Throwable  ], 275:[822:java/lang/Throwable  ], 278:[822:java/lang/Throwable  ], 281:[822:java/lang/Throwable  ], 284:[822:java/lang/Throwable  ], 289:[822:java/lang/Throwable  ], 291:[822:java/lang/Throwable  ], 293:[822:java/lang/Throwable  ], 295:[822:java/lang/Throwable  ], 297:[822:java/lang/Throwable  ], 298:[822:java/lang/Throwable  ], 300:[822:java/lang/Throwable  ], 301:[822:java/lang/Throwable  ], 303:[822:java/lang/Throwable  ], 304:[822:java/lang/Throwable  ], 306:[822:java/lang/Throwable  ], 307:[822:java/lang/Throwable  ], 308:[822:java/lang/Throwable  ], 313:[822:java/lang/Throwable  ], 316:[822:java/lang/Throwable  ], 317:[822:java/lang/Throwable  ], 320:[822:java/lang/Throwable  ], 322:[822:java/lang/Throwable  ], 323:[822:java/lang/Throwable  ], 326:[822:java/lang/Throwable  ], 329:[822:java/lang/Throwable  ], 330:[822:java/lang/Throwable  ], 335:[822:java/lang/Throwable  ], 337:[822:java/lang/Throwable  ], 339:[822:java/lang/Throwable  ], 341:[822:java/lang/Throwable  ], 343:[822:java/lang/Throwable  ], 345:[822:java/lang/Throwable  ], 346:[822:java/lang/Throwable  ], 347:[822:java/lang/Throwable  ], 350:[822:java/lang/Throwable  ], 358:[822:java/lang/Throwable  ], 360:[822:java/lang/Throwable  ], 366:[822:java/lang/Throwable  ], 371:[822:java/lang/Throwable  ], 382:[822:java/lang/Throwable  ], 385:[822:java/lang/Throwable  ], 388:[393:java/lang/Exception  , 822:java/lang/Throwable  ], 390:[393:java/lang/Exception  , 822:java/lang/Throwable  ], 395:[822:java/lang/Throwable  ], 396:[822:java/lang/Throwable  ], 398:[822:java/lang/Throwable  ], 401:[822:java/lang/Throwable  ], 402:[822:java/lang/Throwable  ], 404:[822:java/lang/Throwable  ], 405:[822:java/lang/Throwable  ], 406:[822:java/lang/Throwable  ], 408:[822:java/lang/Throwable  ], 417:[822:java/lang/Throwable  ], 420:[822:java/lang/Throwable  ], 424:[822:java/lang/Throwable  ], 430:[822:java/lang/Throwable  ], 435:[822:java/lang/Throwable  ], 437:[822:java/lang/Throwable  ], 440:[822:java/lang/Throwable  ], 442:[822:java/lang/Throwable  ], 444:[822:java/lang/Throwable  ], 447:[822:java/lang/Throwable  ], 458:[822:java/lang/Throwable  ], 460:[822:java/lang/Throwable  ], 461:[822:java/lang/Throwable  ], 463:[822:java/lang/Throwable  ], 465:[822:java/lang/Throwable  ], 470:[822:java/lang/Throwable  ], 472:[822:java/lang/Throwable  ], 476:[822:java/lang/Throwable  ], 479:[822:java/lang/Throwable  ], 480:[822:java/lang/Throwable  ], 481:[822:java/lang/Throwable  ], 486:[822:java/lang/Throwable  ], 487:[822:java/lang/Throwable  ], 488:[822:java/lang/Throwable  ], 489:[822:java/lang/Throwable  ], 492:[822:java/lang/Throwable  ], 498:[822:java/lang/Throwable  ], 500:[822:java/lang/Throwable  ], 507:[822:java/lang/Throwable  ], 508:[822:java/lang/Throwable  ], 516:[822:java/lang/Throwable  ], 520:[822:java/lang/Throwable  ], 526:[822:java/lang/Throwable  ], 528:[822:java/lang/Throwable  ], 531:[822:java/lang/Throwable  ], 533:[822:java/lang/Throwable  ], 545:[822:java/lang/Throwable  ], 548:[822:java/lang/Throwable  ], 553:[822:java/lang/Throwable  ], 554:[822:java/lang/Throwable  ], 555:[822:java/lang/Throwable  ], 556:[822:java/lang/Throwable  ], 566:[822:java/lang/Throwable  ], 571:[822:java/lang/Throwable  ], 581:[822:java/lang/Throwable  ], 583:[822:java/lang/Throwable  ], 586:[822:java/lang/Throwable  ], 587:[822:java/lang/Throwable  ], 592:[822:java/lang/Throwable  ], 595:[822:java/lang/Throwable  ], 596:[822:java/lang/Throwable  ], 599:[822:java/lang/Throwable  ], 601:[822:java/lang/Throwable  ], 604:[822:java/lang/Throwable  ], 607:[822:java/lang/Throwable  ], 610:[822:java/lang/Throwable  ], 611:[822:java/lang/Throwable  ], 616:[822:java/lang/Throwable  ], 618:[822:java/lang/Throwable  ], 620:[822:java/lang/Throwable  ], 624:[822:java/lang/Throwable  ], 625:[822:java/lang/Throwable  ], 626:[822:java/lang/Throwable  ], 633:[822:java/lang/Throwable  ], 636:[822:java/lang/Throwable  ], 639:[822:java/lang/Throwable  ], 644:[822:java/lang/Throwable  ], 649:[822:java/lang/Throwable  ], 650:[822:java/lang/Throwable  ], 657:[822:java/lang/Throwable  ], 663:[822:java/lang/Throwable  ], 669:[822:java/lang/Throwable  ], 677:[822:java/lang/Throwable  ], 679:[822:java/lang/Throwable  ], 682:[822:java/lang/Throwable  ], 684:[822:java/lang/Throwable  ], 690:[822:java/lang/Throwable  ], 693:[822:java/lang/Throwable  ], 694:[822:java/lang/Throwable  ], 695:[822:java/lang/Throwable  ], 698:[822:java/lang/Throwable  ], 702:[822:java/lang/Throwable  ], 703:[822:java/lang/Throwable  ], 706:[822:java/lang/Throwable  ], 708:[822:java/lang/Throwable  ], 714:[822:java/lang/Throwable  ], 717:[822:java/lang/Throwable  ], 718:[822:java/lang/Throwable  ], 719:[822:java/lang/Throwable  ], 722:[822:java/lang/Throwable  ], 726:[822:java/lang/Throwable  ], 727:[822:java/lang/Throwable  ], 730:[822:java/lang/Throwable  ], 732:[822:java/lang/Throwable  ], 735:[822:java/lang/Throwable  ], 738:[822:java/lang/Throwable  ], 749:[822:java/lang/Throwable  ], 753:[822:java/lang/Throwable  ], 756:[822:java/lang/Throwable  ], 757:[822:java/lang/Throwable  ], 759:[822:java/lang/Throwable  ], 761:[822:java/lang/Throwable  ], 763:[822:java/lang/Throwable  ], 765:[822:java/lang/Throwable  ], 766:[822:java/lang/Throwable  ], 769:[822:java/lang/Throwable  ], 771:[822:java/lang/Throwable  ], 772:[822:java/lang/Throwable  ], 775:[822:java/lang/Throwable  ], 778:[822:java/lang/Throwable  ], 779:[822:java/lang/Throwable  ], 784:[822:java/lang/Throwable  ], 786:[822:java/lang/Throwable  ], 791:[822:java/lang/Throwable  ], 792:[822:java/lang/Throwable  ], 793:[822:java/lang/Throwable  ], 794:[822:java/lang/Throwable  ], 796:[822:java/lang/Throwable  ], 799:[822:java/lang/Throwable  ], 801:[822:java/lang/Throwable  ], 802:[822:java/lang/Throwable  ], 805:[822:java/lang/Throwable  ], 808:[822:java/lang/Throwable  ], 809:[822:java/lang/Throwable  ], 813:[822:java/lang/Throwable  ], 816:[822:java/lang/Throwable  ], 818:[822:java/lang/Throwable  ], 820:[822:java/lang/Throwable  ]]
+TRY [7:[822:java/lang/Throwable  ], 10:[822:java/lang/Throwable  ], 11:[822:java/lang/Throwable  ], 14:[822:java/lang/Throwable  ], 17:[822:java/lang/Throwable  ], 22:[822:java/lang/Throwable  ], 25:[822:java/lang/Throwable  ], 27:[822:java/lang/Throwable  ], 30:[822:java/lang/Throwable  ], 35:[822:java/lang/Throwable  ], 37:[822:java/lang/Throwable  ], 40:[822:java/lang/Throwable  ], 43:[822:java/lang/Throwable  ], 49:[822:java/lang/Throwable  ], 52:[822:java/lang/Throwable  ], 55:[822:java/lang/Throwable  ], 57:[822:java/lang/Throwable  ], 60:[822:java/lang/Throwable  ], 65:[822:java/lang/Throwable  ], 67:[822:java/lang/Throwable  ], 71:[822:java/lang/Throwable  ], 73:[822:java/lang/Throwable  ], 80:[822:java/lang/Throwable  ], 82:[822:java/lang/Throwable  ], 83:[822:java/lang/Throwable  ], 84:[822:java/lang/Throwable  ], 86:[822:java/lang/Throwable  ], 87:[822:java/lang/Throwable  ], 95:[822:java/lang/Throwable  ], 96:[822:java/lang/Throwable  ], 99:[822:java/lang/Throwable  ], 102:[822:java/lang/Throwable  ], 105:[822:java/lang/Throwable  ], 109:[822:java/lang/Throwable  ], 116:[822:java/lang/Throwable  ], 119:[822:java/lang/Throwable  ], 120:[822:java/lang/Throwable  ], 123:[822:java/lang/Throwable  ], 126:[822:java/lang/Throwable  ], 127:[822:java/lang/Throwable  ], 132:[822:java/lang/Throwable  ], 136:[822:java/lang/Throwable  ], 137:[822:java/lang/Throwable  ], 146:[822:java/lang/Throwable  ], 150:[822:java/lang/Throwable  ], 151:[822:java/lang/Throwable  ], 166:[822:java/lang/Throwable  ], 167:[822:java/lang/Throwable  ], 169:[822:java/lang/Throwable  ], 171:[822:java/lang/Throwable  ], 172:[822:java/lang/Throwable  ], 173:[822:java/lang/Throwable  ], 175:[822:java/lang/Throwable  ], 180:[822:java/lang/Throwable  ], 182:[822:java/lang/Throwable  ], 191:[822:java/lang/Throwable  ], 193:[822:java/lang/Throwable  ], 198:[822:java/lang/Throwable  ], 203:[822:java/lang/Throwable  ], 208:[822:java/lang/Throwable  ], 211:[822:java/lang/Throwable  ], 216:[822:java/lang/Throwable  ], 221:[822:java/lang/Throwable  ], 226:[822:java/lang/Throwable  ], 229:[822:java/lang/Throwable  ], 232:[822:java/lang/Throwable  ], 235:[822:java/lang/Throwable  ], 237:[822:java/lang/Throwable  ], 240:[822:java/lang/Throwable  ], 241:[822:java/lang/Throwable  ], 247:[822:java/lang/Throwable  ], 250:[822:java/lang/Throwable  ], 251:[822:java/lang/Throwable  ], 260:[822:java/lang/Throwable  ], 261:[822:java/lang/Throwable  ], 268:[822:java/lang/Throwable  ], 270:[822:java/lang/Throwable  ], 275:[822:java/lang/Throwable  ], 278:[822:java/lang/Throwable  ], 281:[822:java/lang/Throwable  ], 284:[822:java/lang/Throwable  ], 289:[822:java/lang/Throwable  ], 291:[822:java/lang/Throwable  ], 293:[822:java/lang/Throwable  ], 295:[822:java/lang/Throwable  ], 297:[822:java/lang/Throwable  ], 298:[822:java/lang/Throwable  ], 300:[822:java/lang/Throwable  ], 301:[822:java/lang/Throwable  ], 303:[822:java/lang/Throwable  ], 304:[822:java/lang/Throwable  ], 306:[822:java/lang/Throwable  ], 307:[822:java/lang/Throwable  ], 308:[822:java/lang/Throwable  ], 313:[822:java/lang/Throwable  ], 316:[822:java/lang/Throwable  ], 317:[822:java/lang/Throwable  ], 320:[822:java/lang/Throwable  ], 322:[822:java/lang/Throwable  ], 325:[822:java/lang/Throwable  ], 328:[822:java/lang/Throwable  ], 331:[822:java/lang/Throwable  ], 332:[822:java/lang/Throwable  ], 337:[822:java/lang/Throwable  ], 339:[822:java/lang/Throwable  ], 341:[822:java/lang/Throwable  ], 343:[822:java/lang/Throwable  ], 345:[822:java/lang/Throwable  ], 347:[822:java/lang/Throwable  ], 348:[822:java/lang/Throwable  ], 349:[822:java/lang/Throwable  ], 352:[822:java/lang/Throwable  ], 360:[822:java/lang/Throwable  ], 362:[822:java/lang/Throwable  ], 368:[822:java/lang/Throwable  ], 373:[822:java/lang/Throwable  ], 384:[822:java/lang/Throwable  ], 387:[822:java/lang/Throwable  ], 390:[395:java/lang/Exception  , 822:java/lang/Throwable  ], 392:[395:java/lang/Exception  , 822:java/lang/Throwable  ], 397:[822:java/lang/Throwable  ], 398:[822:java/lang/Throwable  ], 400:[822:java/lang/Throwable  ], 403:[822:java/lang/Throwable  ], 404:[822:java/lang/Throwable  ], 406:[822:java/lang/Throwable  ], 407:[822:java/lang/Throwable  ], 408:[822:java/lang/Throwable  ], 410:[822:java/lang/Throwable  ], 419:[822:java/lang/Throwable  ], 422:[822:java/lang/Throwable  ], 426:[822:java/lang/Throwable  ], 432:[822:java/lang/Throwable  ], 437:[822:java/lang/Throwable  ], 439:[822:java/lang/Throwable  ], 442:[822:java/lang/Throwable  ], 444:[822:java/lang/Throwable  ], 446:[822:java/lang/Throwable  ], 449:[822:java/lang/Throwable  ], 460:[822:java/lang/Throwable  ], 462:[822:java/lang/Throwable  ], 463:[822:java/lang/Throwable  ], 465:[822:java/lang/Throwable  ], 467:[822:java/lang/Throwable  ], 472:[822:java/lang/Throwable  ], 474:[822:java/lang/Throwable  ], 478:[822:java/lang/Throwable  ], 481:[822:java/lang/Throwable  ], 482:[822:java/lang/Throwable  ], 483:[822:java/lang/Throwable  ], 488:[822:java/lang/Throwable  ], 489:[822:java/lang/Throwable  ], 490:[822:java/lang/Throwable  ], 491:[822:java/lang/Throwable  ], 494:[822:java/lang/Throwable  ], 500:[822:java/lang/Throwable  ], 502:[822:java/lang/Throwable  ], 509:[822:java/lang/Throwable  ], 510:[822:java/lang/Throwable  ], 518:[822:java/lang/Throwable  ], 522:[822:java/lang/Throwable  ], 528:[822:java/lang/Throwable  ], 530:[822:java/lang/Throwable  ], 533:[822:java/lang/Throwable  ], 535:[822:java/lang/Throwable  ], 547:[822:java/lang/Throwable  ], 550:[822:java/lang/Throwable  ], 555:[822:java/lang/Throwable  ], 556:[822:java/lang/Throwable  ], 557:[822:java/lang/Throwable  ], 558:[822:java/lang/Throwable  ], 568:[822:java/lang/Throwable  ], 573:[822:java/lang/Throwable  ], 583:[822:java/lang/Throwable  ], 585:[822:java/lang/Throwable  ], 588:[822:java/lang/Throwable  ], 589:[822:java/lang/Throwable  ], 594:[822:java/lang/Throwable  ], 597:[822:java/lang/Throwable  ], 598:[822:java/lang/Throwable  ], 601:[822:java/lang/Throwable  ], 603:[822:java/lang/Throwable  ], 606:[822:java/lang/Throwable  ], 609:[822:java/lang/Throwable  ], 612:[822:java/lang/Throwable  ], 613:[822:java/lang/Throwable  ], 618:[822:java/lang/Throwable  ], 620:[822:java/lang/Throwable  ], 622:[822:java/lang/Throwable  ], 626:[822:java/lang/Throwable  ], 627:[822:java/lang/Throwable  ], 628:[822:java/lang/Throwable  ], 635:[822:java/lang/Throwable  ], 637:[822:java/lang/Throwable  ], 639:[822:java/lang/Throwable  ], 644:[822:java/lang/Throwable  ], 649:[822:java/lang/Throwable  ], 650:[822:java/lang/Throwable  ], 657:[822:java/lang/Throwable  ], 663:[822:java/lang/Throwable  ], 669:[822:java/lang/Throwable  ], 677:[822:java/lang/Throwable  ], 679:[822:java/lang/Throwable  ], 682:[822:java/lang/Throwable  ], 684:[822:java/lang/Throwable  ], 690:[822:java/lang/Throwable  ], 693:[822:java/lang/Throwable  ], 694:[822:java/lang/Throwable  ], 695:[822:java/lang/Throwable  ], 698:[822:java/lang/Throwable  ], 702:[822:java/lang/Throwable  ], 703:[822:java/lang/Throwable  ], 706:[822:java/lang/Throwable  ], 708:[822:java/lang/Throwable  ], 714:[822:java/lang/Throwable  ], 717:[822:java/lang/Throwable  ], 718:[822:java/lang/Throwable  ], 719:[822:java/lang/Throwable  ], 722:[822:java/lang/Throwable  ], 726:[822:java/lang/Throwable  ], 727:[822:java/lang/Throwable  ], 730:[822:java/lang/Throwable  ], 732:[822:java/lang/Throwable  ], 735:[822:java/lang/Throwable  ], 738:[822:java/lang/Throwable  ], 749:[822:java/lang/Throwable  ], 753:[822:java/lang/Throwable  ], 756:[822:java/lang/Throwable  ], 757:[822:java/lang/Throwable  ], 759:[822:java/lang/Throwable  ], 761:[822:java/lang/Throwable  ], 763:[822:java/lang/Throwable  ], 765:[822:java/lang/Throwable  ], 766:[822:java/lang/Throwable  ], 769:[822:java/lang/Throwable  ], 771:[822:java/lang/Throwable  ], 772:[822:java/lang/Throwable  ], 775:[822:java/lang/Throwable  ], 778:[822:java/lang/Throwable  ], 779:[822:java/lang/Throwable  ], 784:[822:java/lang/Throwable  ], 786:[822:java/lang/Throwable  ], 791:[822:java/lang/Throwable  ], 792:[822:java/lang/Throwable  ], 793:[822:java/lang/Throwable  ], 794:[822:java/lang/Throwable  ], 796:[822:java/lang/Throwable  ], 799:[822:java/lang/Throwable  ], 801:[822:java/lang/Throwable  ], 802:[822:java/lang/Throwable  ], 805:[822:java/lang/Throwable  ], 808:[822:java/lang/Throwable  ], 809:[822:java/lang/Throwable  ], 813:[822:java/lang/Throwable  ], 816:[822:java/lang/Throwable  ], 818:[822:java/lang/Throwable  ], 820:[822:java/lang/Throwable  ]]
 ALOAD 0
 ALOAD 1
 INVOKESTATIC local/lito381/server/NativeCombatAttacker.enterCompanion(Llocal/lito381/server/ClientSession;Llocal/lito381/server/BattleCompanion;)Llocal/lito381/server/NativeCombatAttacker$Ingress;
```

### `serverHonorRespawnDue(JJII)V`

- 類別：`local/lito381/server/ClientSession`
- 類型：例外表涵蓋、含迴圈（路徑比較器不支援）；事件後繼：`LAYOUT_ONLY`
- 比較器：`method serverHonorRespawnDue(JJII)V: left=6:[39:java/lang/Throwable  ] right=10:[49:java/lang/Throwable  ]; paths rejected: normal control-flow cycle; labelled CFGs differ`

```diff
@@ -1,24 +1,30 @@
 META 0 null [java/io/IOException]
-TRY [6:[39:java/lang/Throwable  ], 11:[39:java/lang/Throwable  ], 13:[39:java/lang/Throwable  ], 16:[39:java/lang/Throwable  ], 19:[39:java/lang/Throwable  ], 22:[39:java/lang/Throwable  ], 25:[39:java/lang/Throwable  ], 28:[39:java/lang/Throwable  ], 30:[39:java/lang/Throwable  ], 33:[39:java/lang/Throwable  ], 36:[39:java/lang/Throwable  ]]
+TRY [10:[49:java/lang/Throwable  ], 19:[49:java/lang/Throwable  ], 22:[49:java/lang/Throwable  ], 24:[49:java/lang/Throwable  ], 25:[49:java/lang/Throwable  ], 28:[49:java/lang/Throwable  ], 31:[49:java/lang/Throwable  ], 34:[49:java/lang/Throwable  ], 36:[49:java/lang/Throwable  ], 39:[49:java/lang/Throwable  ], 42:[49:java/lang/Throwable  ]]
 ALOAD 0
 INVOKESTATIC local/lito381/server/NativeSourceManualScope.enterRespawn(Llocal/lito381/server/ClientSession;)Llocal/lito381/server/NativePlayerActionScope$Ticket;
 ASTORE 7
 ALOAD 0
+DUP
+ASTORE 8
+POP
+ALOAD 0
 LLOAD 1/2
 LLOAD 3/4
 INVOKEVIRTUAL local/lito381/server/ClientSession.honorRespawnPending(JJ)Z
-IFNE L9
-GOTO L110
+IFNE L17
+ALOAD 0
+ALOAD 7
+CONST null
+INVOKESTATIC local/lito381/server/NativeSourceManualScope.exitRespawn(Llocal/lito381/server/ClientSession;Llocal/lito381/server/NativePlayerActionScope$Ticket;Ljava/lang/Throwable;)V
+RETURN
 ALOAD 0
…（其餘 hunk 見 scratch/issue-70/r6-b-classified.json）
```

### `sourcePlayerBattleCommit(ILlocal/lito381/server/ClientSession;ZZJLlocal/lito381/server/BattleJoyOfPainStatus$Lease;I)V`

- 類別：`local/lito381/server/ClientSession`
- 類型：堆疊／區域變數形狀、含迴圈（路徑比較器不支援）；事件後繼：`LAYOUT_ONLY`
- 比較器：`method sourcePlayerBattleCommit(ILlocal/lito381/server/ClientSession;ZZJLlocal/lito381/server/BattleJoyOfPainStatus$Lease;I)V insn[4]: left=INVOKESTATIC local/lito381/server/NativeAcceptedState.requireCurrent()V right=ALOAD 0; paths rejected: normal control-flow cycle; labelled CFGs differ`

```diff
@@ -4,400 +4,303 @@
 INVOKESTATIC local/lito381/server/NativeSourceHealingHp.recovery(Llocal/lito381/server/ClientSession;)Z
 IFEQ L4
 RETURN
-INVOKESTATIC local/lito381/server/NativeAcceptedState.requireCurrent()V
-ALOAD 0
-ALOAD 2
+ALOAD 0
+ALOAD 2
+ALOAD 0
+ALOAD 2
+INVOKESTATIC local/lito381/server/NativeAcceptedState.requireCurrent()V
 INVOKESTATIC local/lito381/server/NativePlayerCommit.markCombat(Llocal/lito381/server/ClientSession;Llocal/lito381/server/ClientSession;)V
 INVOKESTATIC local/lito381/server/NativeAcceptedState.requireCurrent()V
-ALOAD 0
-ALOAD 2
 INVOKESTATIC local/lito381/server/CombatPoisonLifecycle.lastDamage(Llocal/lito381/server/ClientSession;Llocal/lito381/server/ClientSession;)V
-INVOKESTATIC local/lito381/server/NativeAcceptedState.requireCurrent()V
-ALOAD 0
-INVOKESTATIC local/lito381/server/NativePlayerCommit.deathHp(Llocal/lito381/server/ClientSession;)I
-IFNE L40
+ALOAD 0
+INVOKESTATIC local/lito381/server/NativeAcceptedState.requireCurrent()V
+INVOKESTATIC local/lito381/server/NativePlayerCommit.deathHp(Llocal/lito381/server/ClientSession;)I
…（其餘 hunk 見 scratch/issue-70/r6-b-classified.json）
```

### `sourcePlayerWorldCommit(DJLlocal/lito381/server/CombatPoisonLifecycle$Scope;Llocal/lito381/server/ClassicWorldMonsterPopulation$Snapshot;Llocal/lito381/server/BattleAbilityStatus$IllusionAvatar;I)Llocal/lito381/server/ClientSession$ClassicWorldFatalContext;`

- 類別：`local/lito381/server/ClientSession`
- 類型：堆疊／區域變數形狀、含迴圈（路徑比較器不支援）；事件後繼：`LAYOUT_ONLY`
- 比較器：`method sourcePlayerWorldCommit(DJLlocal/lito381/server/CombatPoisonLifecycle$Scope;Llocal/lito381/server/ClassicWorldMonsterPopulation$Snapshot;Llocal/lito381/server/BattleAbilityStatus$IllusionAvatar;I)Llocal/lito381/server/ClientSession$ClassicWorldFatalContext; insn[0]: left=INVOKESTATIC local/lito381/server/NativeAcceptedState.requireCurrent()V right=ALOAD 0; paths rejected: normal control-flo`

```diff
@@ -1,600 +1,448 @@
 META 2 null [java/io/IOException]
 TRY []
-INVOKESTATIC local/lito381/server/NativeAcceptedState.requireCurrent()V
-ALOAD 0
+ALOAD 0
+ALOAD 0
+INVOKESTATIC local/lito381/server/NativeAcceptedState.requireCurrent()V
 INVOKESTATIC local/lito381/server/NativePlayerCommit.deathHp(Llocal/lito381/server/ClientSession;)I
-IFNE L6
+IFNE L7
 CONST str:"classic-world-fatal-damage"
-GOTO L7
+GOTO L8
 CONST str:"classic-world-damage"
-ASTORE 9
-INVOKESTATIC local/lito381/server/NativeAcceptedState.requireCurrent()V
-ALOAD 0
-ALOAD 9
+INVOKESTATIC local/lito381/server/NativeAcceptedState.requireCurrent()V
 INVOKESTATIC local/lito381/server/NativeAcceptedState.sleep(Llocal/lito381/server/ClientSession;Ljava/lang/String;)V
+ALOAD 0
 NEW local/lito381/server/Protocol$Writer
 DUP
…（其餘 hunk 見 scratch/issue-70/r6-b-classified.json）
```

### `sourcePoisonBody$publishNpcMagicCancellation(JLjava/util/List;ZZZZZZZZZZZZZZZZZZZZZLlocal/lito381/server/CombatPoisonLifecycle$Scope;)V`

- 類別：`local/lito381/server/ClientSession`
- 類型：指令形狀；事件後繼：`LAYOUT_ONLY`
- 比較器：`method sourcePoisonBody$publishNpcMagicCancellation(JLjava/util/List;ZZZZZZZZZZZZZZZZZZZZZLlocal/lito381/server/CombatPoisonLifecycle$Scope;)V: left= right=(JLjava/util/List<Llocal/lito381/server/ClientSession;>;ZZZZZZZZZZZZZZZZZZZZZ)V`

```diff
@@ -1,12 +1,15 @@
-META 2 null [java/io/IOException]
+META 2 (JLjava/util/List<Llocal/lito381/server/ClientSession;>;ZZZZZZZZZZZZZZZZZZZZZ)V [java/io/IOException]
 TRY []
 ALOAD 3
 INVOKEINTERFACE java/util/List.iterator()Ljava/util/Iterator; itf
 ASTORE 26
 ALOAD 26
 INVOKEINTERFACE java/util/Iterator.hasNext()Z itf
-IFNE L7
-GOTO L47
+IFNE L10
+LLOAD 1/2
+ALOAD 25
+INVOKESTATIC local/lito381/server/CombatMonsterCurseLifecycle.paletteFinishedForId(JLlocal/lito381/server/CombatPoisonLifecycle$Scope;)V
+GOTO L49
 ALOAD 26
 INVOKEINTERFACE java/util/Iterator.next()Ljava/lang/Object; itf
 CHECKCAST local/lito381/server/ClientSession
```

### `sourcePoisonBody$serverClassicWorldMonsterStatusSkillAttack(Llocal/lito381/server/ClassicWorldMonsterPopulation$StatusSkillAttack;ILlocal/lito381/server/CombatPoisonLifecycle$Scope;)V`

- 類別：`local/lito381/server/ClientSession`
- 類型：例外表涵蓋、含迴圈（路徑比較器不支援）；事件後繼：`LAYOUT_ONLY`
- 比較器：`method sourcePoisonBody$serverClassicWorldMonsterStatusSkillAttack(Llocal/lito381/server/ClassicWorldMonsterPopulation$StatusSkillAttack;ILlocal/lito381/server/CombatPoisonLifecycle$Scope;)V: left=126:[133:java/lang/Exception  ] right=124:[131:java/lang/Exception  ]; paths rejected: normal control-flow cycle; labelled CFGs differ`

```diff
@@ -1,5 +1,5 @@
 META 2 null [java/io/IOException]
-TRY [126:[133:java/lang/Exception  ], 128:[133:java/lang/Exception  ], 129:[133:java/lang/Exception  ], 130:[133:java/lang/Exception  ]]
+TRY [124:[131:java/lang/Exception  ], 126:[131:java/lang/Exception  ], 127:[131:java/lang/Exception  ], 128:[131:java/lang/Exception  ]]
 ILOAD 2
 IFLT L5
 ILOAD 2
```

### `sourcePoisonBody$useClassicWorldCurseRemovalSpell([BLlocal/lito381/server/BattleSpellCatalog$CurseRemovalSpell;Llocal/lito381/server/CombatPoisonLifecycle$Scope;)V`

- 類別：`local/lito381/server/ClientSession`
- 類型：例外表涵蓋、含迴圈（路徑比較器不支援）；事件後繼：`LAYOUT_ONLY`
- 比較器：`method sourcePoisonBody$useClassicWorldCurseRemovalSpell([BLlocal/lito381/server/BattleSpellCatalog$CurseRemovalSpell;Llocal/lito381/server/CombatPoisonLifecycle$Scope;)V: left=304:[387:java/lang/Throwable  ] right=306:[391:java/lang/Throwable  ]; paths rejected: normal control-flow cycle; labelled CFGs differ`

```diff
@@ -1,5 +1,5 @@
 META 2 null [java/io/IOException]
-TRY [304:[387:java/lang/Throwable  ], 307:[387:java/lang/Throwable  ], 309:[387:java/lang/Throwable  ], 310:[387:java/lang/Throwable  ], 312:[387:java/lang/Throwable  ], 313:[387:java/lang/Throwable  ], 314:[387:java/lang/Throwable  ], 315:[387:java/lang/Throwable  ], 322:[387:java/lang/Throwable  ], 326:[387:java/lang/Throwable  ], 329:[387:java/lang/Throwable  ], 332:[387:java/lang/Throwable  ], 333:[387:java/lang/Throwable  ], 336:[387:java/lang/Throwable  ], 340:[387:java/lang/Throwable  ], 343:[387:java/lang/Throwable  ], 345:[387:java/lang/Throwable  ], 351:[387:java/lang/Throwable  ], 352:[387:java/lang/Throwable  ], 353:[387:java/lang/Throwable  ], 355:[387:java/lang/Throwable  ], 359:[387:java/lang/Throwable  ], 360:[387:java/lang/Throwable  ], 361:[387:java/lang/Throwable  ], 365:[387:java/lang/Throwable  ], 368:[387:java/lang/Throwable  ], 369:[387:java/lang/Throwable  ], 371:[387:java/lang/Throwable  ], 375:[387:java/lang/Throwable  ], 378:[387:java/lang/Throwable  ], 383:[387:java/lang/Throwable  ], 385:[387:java/lang/Throwable  ], 389:[387:java/lang/Throwable  ]]
+TRY [306:[391:java/lang/Throwable  ], 309:[391:java/lang/Throwable  ], 311:[391:java/lang/Throwable  ], 312:[391:java/lang/Throwable  ], 314:[391:java/lang/Throwable  ], 315:[391:java/lang/Throwable  ], 316:[391:java/lang/Throwable  ], 317:[391:java/lang/Throwable  ], 324:[391:java/lang/Throwable  ], 328:[391:java/lang/Throwable  ], 331:[391:java/lang/Throwable  ], 334:[391:java/lang/Throwable  ], 335:[391:java/lang/Throwable  ], 338:[391:java/lang/Throwable  ], 342:[391:java/lang/Throwable  ], 345:[391:java/lang/Throwable  ], 347:[391:java/lang/Throwable  ], 353:[391:java/lang/Throwable  ], 354:[391:java/lang/Throwable  ], 355:[391:java/lang/Throwable  ], 357:[391:java/lang/Throwable  ], 361:[391:java/lang/Throwable  ], 362:[391:java/lang/Throwable  ], 363:[391:java/lang/Throwable  ], 366:[391:java/lang/Throwable  ], 370:[391:java/lang/Throwable  ], 373:[391:java/lang/Throwable  ], 375:[391:java/lang/Throwable  ], 379:[391:java/lang/Throwable  ], 382:[391:java/lang/Throwable  ], 387:[391:java/lang/Throwable  ], 389:[391:java/lang/Throwable  ], 393:[391:java/lang/Throwable  ]]
 CONST null
 ASTORE 4
 ALOAD 1
```

### `sourcePoisonBody$useCurseRemovalSpell([BLlocal/lito381/server/BattleSpellCatalog$CurseRemovalSpell;Llocal/lito381/server/CombatPoisonLifecycle$Scope;)V`

- 類別：`local/lito381/server/ClientSession`
- 類型：例外表涵蓋、含迴圈（路徑比較器不支援）；事件後繼：`LAYOUT_ONLY`
- 比較器：`method sourcePoisonBody$useCurseRemovalSpell([BLlocal/lito381/server/BattleSpellCatalog$CurseRemovalSpell;Llocal/lito381/server/CombatPoisonLifecycle$Scope;)V: left=442:[519:java/lang/Throwable  ] right=444:[523:java/lang/Throwable  ]; paths rejected: normal control-flow cycle; labelled CFGs differ`

```diff
@@ -1,5 +1,5 @@
 META 2 null [java/io/IOException]
-TRY [442:[519:java/lang/Throwable  ], 445:[519:java/lang/Throwable  ], 447:[519:java/lang/Throwable  ], 448:[519:java/lang/Throwable  ], 450:[519:java/lang/Throwable  ], 451:[519:java/lang/Throwable  ], 452:[519:java/lang/Throwable  ], 453:[519:java/lang/Throwable  ], 460:[519:java/lang/Throwable  ], 464:[519:java/lang/Throwable  ], 465:[519:java/lang/Throwable  ], 468:[519:java/lang/Throwable  ], 471:[519:java/lang/Throwable  ], 473:[519:java/lang/Throwable  ], 475:[519:java/lang/Throwable  ], 477:[519:java/lang/Throwable  ], 483:[519:java/lang/Throwable  ], 484:[519:java/lang/Throwable  ], 485:[519:java/lang/Throwable  ], 487:[519:java/lang/Throwable  ], 491:[519:java/lang/Throwable  ], 492:[519:java/lang/Throwable  ], 493:[519:java/lang/Throwable  ], 497:[519:java/lang/Throwable  ], 500:[519:java/lang/Throwable  ], 501:[519:java/lang/Throwable  ], 503:[519:java/lang/Throwable  ], 507:[519:java/lang/Throwable  ], 510:[519:java/lang/Throwable  ], 515:[519:java/lang/Throwable  ], 517:[519:java/lang/Throwable  ], 521:[519:java/lang/Throwable  ]]
+TRY [444:[523:java/lang/Throwable  ], 447:[523:java/lang/Throwable  ], 449:[523:java/lang/Throwable  ], 450:[523:java/lang/Throwable  ], 452:[523:java/lang/Throwable  ], 453:[523:java/lang/Throwable  ], 454:[523:java/lang/Throwable  ], 455:[523:java/lang/Throwable  ], 462:[523:java/lang/Throwable  ], 466:[523:java/lang/Throwable  ], 467:[523:java/lang/Throwable  ], 470:[523:java/lang/Throwable  ], 473:[523:java/lang/Throwable  ], 475:[523:java/lang/Throwable  ], 477:[523:java/lang/Throwable  ], 479:[523:java/lang/Throwable  ], 485:[523:java/lang/Throwable  ], 486:[523:java/lang/Throwable  ], 487:[523:java/lang/Throwable  ], 489:[523:java/lang/Throwable  ], 493:[523:java/lang/Throwable  ], 494:[523:java/lang/Throwable  ], 495:[523:java/lang/Throwable  ], 498:[523:java/lang/Throwable  ], 502:[523:java/lang/Throwable  ], 505:[523:java/lang/Throwable  ], 507:[523:java/lang/Throwable  ], 511:[523:java/lang/Throwable  ], 514:[523:java/lang/Throwable  ], 519:[523:java/lang/Throwable  ], 521:[523:java/lang/Throwable  ], 525:[523:java/lang/Throwable  ]]
 CONST null
 ASTORE 4
 ALOAD 1
```

### `startBattle()V`

- 類別：`local/lito381/server/ClientSession`
- 類型：堆疊／區域變數形狀、含迴圈（路徑比較器不支援）；事件後繼：`LAYOUT_ONLY`
- 比較器：`method startBattle()V insn[242]: left=ISTORE 7 right=POP; paths rejected: normal control-flow cycle; labelled CFGs differ`

```diff
@@ -242,10 +242,10 @@
 CONST Integer:1
 GOTO L242
 CONST Integer:0
+POP
+ALOAD 0
+INVOKESTATIC local/lito381/server/NativeRoomRules.activeBot(Llocal/lito381/server/ClientSession;)Z
 ISTORE 7
-ALOAD 0
-INVOKESTATIC local/lito381/server/NativeRoomRules.activeBot(Llocal/lito381/server/ClientSession;)Z
-ISTORE 8
 ALOAD 1
 INVOKEVIRTUAL local/lito381/server/BattleRules$RuleSet.mode()Llocal/lito381/server/BattleRules$Mode;
 GETSTATIC local/lito381/server/BattleRules$Mode.PET_RACE:Llocal/lito381/server/BattleRules$Mode;
```

### `useAbilityBuffSpell([BLlocal/lito381/server/BattleSpellCatalog$AbilityBuffSpell;)V`

- 類別：`local/lito381/server/ClientSession`
- 類型：例外表涵蓋、含迴圈（路徑比較器不支援）；事件後繼：`LAYOUT_ONLY`
- 比較器：`method useAbilityBuffSpell([BLlocal/lito381/server/BattleSpellCatalog$AbilityBuffSpell;)V: left=269:[458:java/lang/Throwable  ] right=271:[461:java/lang/Throwable  ]; paths rejected: normal control-flow cycle; labelled CFGs differ`

```diff
@@ -1,5 +1,5 @@
 META 2 null [java/io/IOException]
-TRY [269:[458:java/lang/Throwable  ], 271:[447:java/lang/Throwable  , 458:java/lang/Throwable  ], 273:[447:java/lang/Throwable  , 458:java/lang/Throwable  ], 274:[447:java/lang/Throwable  , 458:java/lang/Throwable  ], 276:[447:java/lang/Throwable  , 458:java/lang/Throwable  ], 277:[447:java/lang/Throwable  , 458:java/lang/Throwable  ], 278:[447:java/lang/Throwable  , 458:java/lang/Throwable  ], 279:[447:java/lang/Throwable  , 458:java/lang/Throwable  ], 282:[447:java/lang/Throwable  , 458:java/lang/Throwable  ], 285:[447:java/lang/Throwable  , 458:java/lang/Throwable  ], 286:[447:java/lang/Throwable  , 458:java/lang/Throwable  ], 289:[447:java/lang/Throwable  , 458:java/lang/Throwable  ], 294:[447:java/lang/Throwable  , 458:java/lang/Throwable  ], 295:[447:java/lang/Throwable  , 458:java/lang/Throwable  ], 298:[447:java/lang/Throwable  , 458:java/lang/Throwable  ], 307:[447:java/lang/Throwable  , 458:java/lang/Throwable  ], 310:[447:java/lang/Throwable  , 458:java/lang/Throwable  ], 311:[447:java/lang/Throwable  , 458:java/lang/Throwable  ], 314:[447:java/lang/Throwable  , 458:java/lang/Throwable  ], 318:[447:java/lang/Throwable  , 458:java/lang/Throwable  ], 322:[447:java/lang/Throwable  , 458:java/lang/Throwable  ], 323:[447:java/lang/Throwable  , 458:java/lang/Throwable  ], 326:[447:java/lang/Throwable  , 458:java/lang/Throwable  ], 330:[447:java/lang/Throwable  , 458:java/lang/Throwable  ], 344:[447:java/lang/Throwable  , 458:java/lang/Throwable  ], 347:[447:java/lang/Throwable  , 458:java/lang/Throwable  ], 350:[447:java/lang/Throwable  , 458:java/lang/Throwable  ], 353:[447:java/lang/Throwable  , 458:java/lang/Throwable  ], 355:[447:java/lang/Throwable  , 458:java/lang/Throwable  ], 357:[447:java/lang/Throwable  , 458:java/lang/Throwable  ], 359:[447:java/lang/Throwable  , 458:java/lang/Throwable  ], 364:[447:java/lang/Throwable  , 458:java/lang/Throwable  ], 366:[447:java/lang/Throwable  , 458:java/lang/Throwable  ], 367:[447:java/lang/Throwable  , 458:java/lang/Throwable  ], 368:[447:java/lang/Throwable  , 458:java/lang/Throwable  ], 370:[447:java/lang/Throwable  , 458:java/lang/Throwable  ], 372:[458:java/lang/Throwable  ], 375:[447:java/lang/Throwable  , 458:java/lang/Throwable  ], 380:[447:java/lang/Throwable  , 458:java/lang/Throwable  ], 381:[447:java/lang/Throwable  , 458:java/lang/Throwable  ], 382:[447:java/lang/Throwable  , 458:java/lang/Throwable  ], 384:[447:java/lang/Throwable  , 458:java/lang/Throwable  ], 385:[447:java/lang/Throwable  , 458:java/lang/Throwable  ], 388:[447:java/lang/Throwable  , 458:java/lang/Throwable  ], 390:[447:java/lang/Throwable  , 458:java/lang/Throwable  ], 393:[447:java/lang/Throwable  , 458:java/lang/Throwable  ], 395:[447:java/lang/Throwable  , 458:java/lang/Throwable  ], 396:[447:java/lang/Throwable  , 458:java/lang/Throwable  ], 400:[447:java/lang/Throwable  , 458:java/lang/Throwable  ], 402:[447:java/lang/Throwable  , 458:java/lang/Throwable  ], 405:[447:java/lang/Throwable  , 458:java/lang/Throwable  ], 407:[447:java/lang/Throwable  , 458:java/lang/Throwable  ], 408:[447:java/lang/Throwable  , 458:java/lang/Throwable  ], 413:[447:java/lang/Throwable  , 458:java/lang/Throwable  ], 416:[447:java/lang/Throwable  , 458:java/lang/Throwable  ], 417:[447:java/lang/Throwable  , 458:java/lang/Throwable  ], 419:[447:java/lang/Throwable  , 458:java/lang/Throwable  ], 423:[447:java/lang/Throwable  , 458:java/lang/Throwable  ], 426:[447:java/lang/Throwable  , 458:java/lang/Throwable  ], 431:[447:java/lang/Throwable  , 458:java/lang/Throwable  ], 433:[447:java/lang/Throwable  , 458:java/lang/Throwable  ], 438:[447:java/lang/Throwable  , 458:java/lang/Throwable  ], 439:[447:java/lang/Throwable  , 458:java/lang/Throwable  ], 441:[447:java/lang/Throwable  , 458:java/lang/Throwable  ], 442:[447:java/lang/Throwable  , 458:java/lang/Throwable  ], 443:[447:java/lang/Throwable  , 458:java/lang/Throwable  ], 445:[447:java/lang/Throwable  , 458:java/lang/Throwable  ], 449:[447:java/lang/Throwable  , 458:java/lang/Throwable  ], 451:[458:java/lang/Throwable  ], 453:[458:java/lang/Throwable  ], 456:[458:java/lang/Throwable  ], 460:[458:java/lang/Throwable  ]]
+TRY [271:[461:java/lang/Throwable  ], 273:[453:java/lang/Throwable  , 461:java/lang/Throwable  ], 275:[453:java/lang/Throwable  , 461:java/lang/Throwable  ], 276:[453:java/lang/Throwable  , 461:java/lang/Throwable  ], 278:[453:java/lang/Throwable  , 461:java/lang/Throwable  ], 279:[453:java/lang/Throwable  , 461:java/lang/Throwable  ], 280:[453:java/lang/Throwable  , 461:java/lang/Throwable  ], 281:[453:java/lang/Throwable  , 461:java/lang/Throwable  ], 284:[453:java/lang/Throwable  , 461:java/lang/Throwable  ], 287:[453:java/lang/Throwable  , 461:java/lang/Throwable  ], 288:[453:java/lang/Throwable  , 461:java/lang/Throwable  ], 291:[453:java/lang/Throwable  , 461:java/lang/Throwable  ], 296:[453:java/lang/Throwable  , 461:java/lang/Throwable  ], 297:[453:java/lang/Throwable  , 461:java/lang/Throwable  ], 300:[453:java/lang/Throwable  , 461:java/lang/Throwable  ], 309:[453:java/lang/Throwable  , 461:java/lang/Throwable  ], 312:[453:java/lang/Throwable  , 461:java/lang/Throwable  ], 313:[453:java/lang/Throwable  , 461:java/lang/Throwable  ], 316:[453:java/lang/Throwable  , 461:java/lang/Throwable  ], 320:[453:java/lang/Throwable  , 461:java/lang/Throwable  ], 324:[453:java/lang/Throwable  , 461:java/lang/Throwable  ], 325:[453:java/lang/Throwable  , 461:java/lang/Throwable  ], 328:[453:java/lang/Throwable  , 461:java/lang/Throwable  ], 332:[453:java/lang/Throwable  , 461:java/lang/Throwable  ], 346:[453:java/lang/Throwable  , 461:java/lang/Throwable  ], 349:[453:java/lang/Throwable  , 461:java/lang/Throwable  ], 352:[453:java/lang/Throwable  , 461:java/lang/Throwable  ], 355:[453:java/lang/Throwable  , 461:java/lang/Throwable  ], 357:[453:java/lang/Throwable  , 461:java/lang/Throwable  ], 359:[453:java/lang/Throwable  , 461:java/lang/Throwable  ], 361:[453:java/lang/Throwable  , 461:java/lang/Throwable  ], 366:[453:java/lang/Throwable  , 461:java/lang/Throwable  ], 368:[453:java/lang/Throwable  , 461:java/lang/Throwable  ], 369:[453:java/lang/Throwable  , 461:java/lang/Throwable  ], 370:[453:java/lang/Throwable  , 461:java/lang/Throwable  ], 372:[453:java/lang/Throwable  , 461:java/lang/Throwable  ], 374:[461:java/lang/Throwable  ], 377:[453:java/lang/Throwable  , 461:java/lang/Throwable  ], 383:[453:java/lang/Throwable  , 461:java/lang/Throwable  ], 384:[453:java/lang/Throwable  , 461:java/lang/Throwable  ], 386:[453:java/lang/Throwable  , 461:java/lang/Throwable  ], 387:[453:java/lang/Throwable  , 461:java/lang/Throwable  ], 388:[453:java/lang/Throwable  , 461:java/lang/Throwable  ], 390:[453:java/lang/Throwable  , 461:java/lang/Throwable  ], 392:[461:java/lang/Throwable  ], 396:[453:java/lang/Throwable  , 461:java/lang/Throwable  ], 397:[453:java/lang/Throwable  , 461:java/lang/Throwable  ], 398:[453:java/lang/Throwable  , 461:java/lang/Throwable  ], 400:[453:java/lang/Throwable  , 461:java/lang/Throwable  ], 401:[453:java/lang/Throwable  , 461:java/lang/Throwable  ], 404:[453:java/lang/Throwable  , 461:java/lang/Throwable  ], 406:[453:java/lang/Throwable  , 461:java/lang/Throwable  ], 409:[453:java/lang/Throwable  , 461:java/lang/Throwable  ], 411:[453:java/lang/Throwable  , 461:java/lang/Throwable  ], 412:[453:java/lang/Throwable  , 461:java/lang/Throwable  ], 416:[453:java/lang/Throwable  , 461:java/lang/Throwable  ], 418:[453:java/lang/Throwable  , 461:java/lang/Throwable  ], 421:[453:java/lang/Throwable  , 461:java/lang/Throwable  ], 423:[453:java/lang/Throwable  , 461:java/lang/Throwable  ], 424:[453:java/lang/Throwable  , 461:java/lang/Throwable  ], 428:[453:java/lang/Throwable  , 461:java/lang/Throwable  ], 432:[453:java/lang/Throwable  , 461:java/lang/Throwable  ], 435:[453:java/lang/Throwable  , 461:java/lang/Throwable  ], 437:[453:java/lang/Throwable  , 461:java/lang/Throwable  ], 441:[453:java/lang/Throwable  , 461:java/lang/Throwable  ], 444:[453:java/lang/Throwable  , 461:java/lang/Throwable  ], 449:[453:java/lang/Throwable  , 461:java/lang/Throwable  ], 451:[453:java/lang/Throwable  , 461:java/lang/Throwable  ], 455:[453:java/lang/Throwable  , 461:java/lang/Throwable  ], 457:[461:java/lang/Throwable  ], 459:[461:java/lang/Throwable  ], 463:[461:java/lang/Throwable  ]]
 ALOAD 0
 GETFIELD local/lito381/server/ClientSession.phase:Llocal/lito381/server/ClientSession$Phase;
 ASTORE 3
```

### `useAdvanceSpiritSpell([BLlocal/lito381/server/BattleSpellCatalog$AdvanceSpiritSpell;)V`

- 類別：`local/lito381/server/ClientSession`
- 類型：例外表涵蓋、含迴圈（路徑比較器不支援）；事件後繼：`LAYOUT_ONLY`
- 比較器：`method useAdvanceSpiritSpell([BLlocal/lito381/server/BattleSpellCatalog$AdvanceSpiritSpell;)V: left=203:[379:java/lang/Throwable  ] right=197:[371:java/lang/Throwable  ]; paths rejected: normal control-flow cycle; labelled CFGs differ`

```diff
@@ -1,5 +1,5 @@
 META 2 null [java/io/IOException]
-TRY [203:[379:java/lang/Throwable  ], 205:[371:java/lang/Throwable  , 379:java/lang/Throwable  ], 207:[371:java/lang/Throwable  , 379:java/lang/Throwable  ], 208:[371:java/lang/Throwable  , 379:java/lang/Throwable  ], 210:[371:java/lang/Throwable  , 379:java/lang/Throwable  ], 211:[371:java/lang/Throwable  , 379:java/lang/Throwable  ], 212:[371:java/lang/Throwable  , 379:java/lang/Throwable  ], 213:[371:java/lang/Throwable  , 379:java/lang/Throwable  ], 216:[371:java/lang/Throwable  , 379:java/lang/Throwable  ], 217:[371:java/lang/Throwable  , 379:java/lang/Throwable  ], 220:[371:java/lang/Throwable  , 379:java/lang/Throwable  ], 225:[371:java/lang/Throwable  , 379:java/lang/Throwable  ], 226:[371:java/lang/Throwable  , 379:java/lang/Throwable  ], 229:[371:java/lang/Throwable  , 379:java/lang/Throwable  ], 231:[371:java/lang/Throwable  , 379:java/lang/Throwable  ], 239:[371:java/lang/Throwable  , 379:java/lang/Throwable  ], 240:[371:java/lang/Throwable  , 379:java/lang/Throwable  ], 243:[371:java/lang/Throwable  , 379:java/lang/Throwable  ], 244:[371:java/lang/Throwable  , 379:java/lang/Throwable  ], 248:[371:java/lang/Throwable  , 379:java/lang/Throwable  ], 261:[371:java/lang/Throwable  , 379:java/lang/Throwable  ], 265:[371:java/lang/Throwable  , 379:java/lang/Throwable  ], 266:[371:java/lang/Throwable  , 379:java/lang/Throwable  ], 269:[371:java/lang/Throwable  , 379:java/lang/Throwable  ], 270:[371:java/lang/Throwable  , 379:java/lang/Throwable  ], 277:[371:java/lang/Throwable  , 379:java/lang/Throwable  ], 279:[371:java/lang/Throwable  , 379:java/lang/Throwable  ], 282:[371:java/lang/Throwable  , 379:java/lang/Throwable  ], 285:[371:java/lang/Throwable  , 379:java/lang/Throwable  ], 288:[371:java/lang/Throwable  , 379:java/lang/Throwable  ], 291:[371:java/lang/Throwable  , 379:java/lang/Throwable  ], 294:[371:java/lang/Throwable  , 379:java/lang/Throwable  ], 299:[371:java/lang/Throwable  , 379:java/lang/Throwable  ], 301:[371:java/lang/Throwable  , 379:java/lang/Throwable  ], 303:[371:java/lang/Throwable  , 379:java/lang/Throwable  ], 309:[371:java/lang/Throwable  , 379:java/lang/Throwable  ], 310:[371:java/lang/Throwable  , 379:java/lang/Throwable  ], 311:[371:java/lang/Throwable  , 379:java/lang/Throwable  ], 313:[371:java/lang/Throwable  , 379:java/lang/Throwable  ], 315:[379:java/lang/Throwable  ], 319:[371:java/lang/Throwable  , 379:java/lang/Throwable  ], 320:[371:java/lang/Throwable  , 379:java/lang/Throwable  ], 321:[371:java/lang/Throwable  , 379:java/lang/Throwable  ], 327:[371:java/lang/Throwable  , 379:java/lang/Throwable  ], 330:[371:java/lang/Throwable  , 379:java/lang/Throwable  ], 331:[371:java/lang/Throwable  , 379:java/lang/Throwable  ], 333:[371:java/lang/Throwable  , 379:java/lang/Throwable  ], 336:[371:java/lang/Throwable  , 379:java/lang/Throwable  ], 339:[371:java/lang/Throwable  , 379:java/lang/Throwable  ], 340:[371:java/lang/Throwable  , 379:java/lang/Throwable  ], 344:[371:java/lang/Throwable  , 379:java/lang/Throwable  ], 346:[371:java/lang/Throwable  , 379:java/lang/Throwable  ], 349:[371:java/lang/Throwable  , 379:java/lang/Throwable  ], 352:[371:java/lang/Throwable  , 379:java/lang/Throwable  ], 355:[371:java/lang/Throwable  , 379:java/lang/Throwable  ], 359:[371:java/lang/Throwable  , 379:java/lang/Throwable  ], 362:[371:java/lang/Throwable  , 379:java/lang/Throwable  ], 367:[371:java/lang/Throwable  , 379:java/lang/Throwable  ], 369:[371:java/lang/Throwable  , 379:java/lang/Throwable  ], 373:[371:java/lang/Throwable  , 379:java/lang/Throwable  ], 375:[379:java/lang/Throwable  ], 377:[379:java/lang/Throwable  ], 381:[379:java/lang/Throwable  ]]
+TRY [197:[371:java/lang/Throwable  ], 199:[363:java/lang/Throwable  , 371:java/lang/Throwable  ], 201:[363:java/lang/Throwable  , 371:java/lang/Throwable  ], 202:[363:java/lang/Throwable  , 371:java/lang/Throwable  ], 204:[363:java/lang/Throwable  , 371:java/lang/Throwable  ], 205:[363:java/lang/Throwable  , 371:java/lang/Throwable  ], 206:[363:java/lang/Throwable  , 371:java/lang/Throwable  ], 207:[363:java/lang/Throwable  , 371:java/lang/Throwable  ], 210:[363:java/lang/Throwable  , 371:java/lang/Throwable  ], 211:[363:java/lang/Throwable  , 371:java/lang/Throwable  ], 214:[363:java/lang/Throwable  , 371:java/lang/Throwable  ], 219:[363:java/lang/Throwable  , 371:java/lang/Throwable  ], 220:[363:java/lang/Throwable  , 371:java/lang/Throwable  ], 223:[363:java/lang/Throwable  , 371:java/lang/Throwable  ], 225:[363:java/lang/Throwable  , 371:java/lang/Throwable  ], 233:[363:java/lang/Throwable  , 371:java/lang/Throwable  ], 234:[363:java/lang/Throwable  , 371:java/lang/Throwable  ], 237:[363:java/lang/Throwable  , 371:java/lang/Throwable  ], 238:[363:java/lang/Throwable  , 371:java/lang/Throwable  ], 242:[363:java/lang/Throwable  , 371:java/lang/Throwable  ], 253:[363:java/lang/Throwable  , 371:java/lang/Throwable  ], 257:[363:java/lang/Throwable  , 371:java/lang/Throwable  ], 258:[363:java/lang/Throwable  , 371:java/lang/Throwable  ], 261:[363:java/lang/Throwable  , 371:java/lang/Throwable  ], 262:[363:java/lang/Throwable  , 371:java/lang/Throwable  ], 269:[363:java/lang/Throwable  , 371:java/lang/Throwable  ], 271:[363:java/lang/Throwable  , 371:java/lang/Throwable  ], 274:[363:java/lang/Throwable  , 371:java/lang/Throwable  ], 277:[363:java/lang/Throwable  , 371:java/lang/Throwable  ], 280:[363:java/lang/Throwable  , 371:java/lang/Throwable  ], 283:[363:java/lang/Throwable  , 371:java/lang/Throwable  ], 286:[363:java/lang/Throwable  , 371:java/lang/Throwable  ], 291:[363:java/lang/Throwable  , 371:java/lang/Throwable  ], 293:[363:java/lang/Throwable  , 371:java/lang/Throwable  ], 295:[363:java/lang/Throwable  , 371:java/lang/Throwable  ], 301:[363:java/lang/Throwable  , 371:java/lang/Throwable  ], 302:[363:java/lang/Throwable  , 371:java/lang/Throwable  ], 303:[363:java/lang/Throwable  , 371:java/lang/Throwable  ], 305:[363:java/lang/Throwable  , 371:java/lang/Throwable  ], 307:[371:java/lang/Throwable  ], 311:[363:java/lang/Throwable  , 371:java/lang/Throwable  ], 312:[363:java/lang/Throwable  , 371:java/lang/Throwable  ], 313:[363:java/lang/Throwable  , 371:java/lang/Throwable  ], 316:[363:java/lang/Throwable  , 371:java/lang/Throwable  ], 320:[363:java/lang/Throwable  , 371:java/lang/Throwable  ], 323:[363:java/lang/Throwable  , 371:java/lang/Throwable  ], 325:[363:java/lang/Throwable  , 371:java/lang/Throwable  ], 328:[363:java/lang/Throwable  , 371:java/lang/Throwable  ], 331:[363:java/lang/Throwable  , 371:java/lang/Throwable  ], 332:[363:java/lang/Throwable  , 371:java/lang/Throwable  ], 336:[363:java/lang/Throwable  , 371:java/lang/Throwable  ], 338:[363:java/lang/Throwable  , 371:java/lang/Throwable  ], 341:[363:java/lang/Throwable  , 371:java/lang/Throwable  ], 344:[363:java/lang/Throwable  , 371:java/lang/Throwable  ], 347:[363:java/lang/Throwable  , 371:java/lang/Throwable  ], 351:[363:java/lang/Throwable  , 371:java/lang/Throwable  ], 354:[363:java/lang/Throwable  , 371:java/lang/Throwable  ], 359:[363:java/lang/Throwable  , 371:java/lang/Throwable  ], 361:[363:java/lang/Throwable  , 371:java/lang/Throwable  ], 365:[363:java/lang/Throwable  , 371:java/lang/Throwable  ], 367:[371:java/lang/Throwable  ], 369:[371:java/lang/Throwable  ], 373:[371:java/lang/Throwable  ]]
 ALOAD 1
 INVOKESTATIC local/lito381/server/Protocol.objectSpellRequest([B)Llocal/lito381/server/Protocol$ObjectSpellRequest;
 ALOAD 0
```

### `useAquaProtectSpell([BLlocal/lito381/server/BattleSpellCatalog$AquaProtectSpell;)V`

- 類別：`local/lito381/server/ClientSession`
- 類型：例外表涵蓋、含迴圈（路徑比較器不支援）；事件後繼：`LAYOUT_ONLY`
- 比較器：`method useAquaProtectSpell([BLlocal/lito381/server/BattleSpellCatalog$AquaProtectSpell;)V: left=295:[476:java/lang/Throwable  ] right=298:[480:java/lang/Throwable  ]; paths rejected: normal control-flow cycle; labelled CFGs differ`

```diff
@@ -1,5 +1,5 @@
 META 2 null [java/io/IOException]
-TRY [295:[476:java/lang/Throwable  ], 296:[465:java/lang/Throwable  , 476:java/lang/Throwable  ], 299:[465:java/lang/Throwable  , 476:java/lang/Throwable  ], 301:[465:java/lang/Throwable  , 476:java/lang/Throwable  ], 302:[465:java/lang/Throwable  , 476:java/lang/Throwable  ], 304:[465:java/lang/Throwable  , 476:java/lang/Throwable  ], 305:[465:java/lang/Throwable  , 476:java/lang/Throwable  ], 306:[465:java/lang/Throwable  , 476:java/lang/Throwable  ], 307:[465:java/lang/Throwable  , 476:java/lang/Throwable  ], 310:[465:java/lang/Throwable  , 476:java/lang/Throwable  ], 314:[465:java/lang/Throwable  , 476:java/lang/Throwable  ], 317:[465:java/lang/Throwable  , 476:java/lang/Throwable  ], 320:[465:java/lang/Throwable  , 476:java/lang/Throwable  ], 324:[465:java/lang/Throwable  , 476:java/lang/Throwable  ], 327:[465:java/lang/Throwable  , 476:java/lang/Throwable  ], 330:[465:java/lang/Throwable  , 476:java/lang/Throwable  ], 336:[465:java/lang/Throwable  , 476:java/lang/Throwable  ], 338:[465:java/lang/Throwable  , 476:java/lang/Throwable  ], 340:[465:java/lang/Throwable  , 476:java/lang/Throwable  ], 342:[465:java/lang/Throwable  , 476:java/lang/Throwable  ], 343:[465:java/lang/Throwable  , 476:java/lang/Throwable  ], 346:[465:java/lang/Throwable  , 476:java/lang/Throwable  ], 348:[465:java/lang/Throwable  , 476:java/lang/Throwable  ], 350:[465:java/lang/Throwable  , 476:java/lang/Throwable  ], 351:[465:java/lang/Throwable  , 476:java/lang/Throwable  ], 354:[465:java/lang/Throwable  , 476:java/lang/Throwable  ], 357:[465:java/lang/Throwable  , 476:java/lang/Throwable  ], 361:[465:java/lang/Throwable  , 476:java/lang/Throwable  ], 365:[465:java/lang/Throwable  , 476:java/lang/Throwable  ], 366:[465:java/lang/Throwable  , 476:java/lang/Throwable  ], 369:[465:java/lang/Throwable  , 476:java/lang/Throwable  ], 373:[465:java/lang/Throwable  , 476:java/lang/Throwable  ], 378:[465:java/lang/Throwable  , 476:java/lang/Throwable  ], 383:[465:java/lang/Throwable  , 476:java/lang/Throwable  ], 384:[465:java/lang/Throwable  , 476:java/lang/Throwable  ], 387:[465:java/lang/Throwable  , 476:java/lang/Throwable  ], 392:[465:java/lang/Throwable  , 476:java/lang/Throwable  ], 395:[465:java/lang/Throwable  , 476:java/lang/Throwable  ], 410:[465:java/lang/Throwable  , 476:java/lang/Throwable  ], 411:[465:java/lang/Throwable  , 476:java/lang/Throwable  ], 412:[465:java/lang/Throwable  , 476:java/lang/Throwable  ], 414:[465:java/lang/Throwable  , 476:java/lang/Throwable  ], 417:[465:java/lang/Throwable  , 476:java/lang/Throwable  ], 419:[465:java/lang/Throwable  , 476:java/lang/Throwable  ], 420:[465:java/lang/Throwable  , 476:java/lang/Throwable  ], 425:[465:java/lang/Throwable  , 476:java/lang/Throwable  ], 428:[465:java/lang/Throwable  , 476:java/lang/Throwable  ], 429:[465:java/lang/Throwable  , 476:java/lang/Throwable  ], 431:[465:java/lang/Throwable  , 476:java/lang/Throwable  ], 435:[465:java/lang/Throwable  , 476:java/lang/Throwable  ], 438:[465:java/lang/Throwable  , 476:java/lang/Throwable  ], 443:[465:java/lang/Throwable  , 476:java/lang/Throwable  ], 445:[465:java/lang/Throwable  , 476:java/lang/Throwable  ], 448:[465:java/lang/Throwable  , 476:java/lang/Throwable  ], 454:[465:java/lang/Throwable  , 476:java/lang/Throwable  ], 455:[465:java/lang/Throwable  , 476:java/lang/Throwable  ], 457:[465:java/lang/Throwable  , 476:java/lang/Throwable  ], 459:[465:java/lang/Throwable  , 476:java/lang/Throwable  ], 461:[465:java/lang/Throwable  , 476:java/lang/Throwable  ], 463:[465:java/lang/Throwable  , 476:java/lang/Throwable  ], 467:[465:java/lang/Throwable  , 476:java/lang/Throwable  ], 469:[476:java/lang/Throwable  ], 471:[476:java/lang/Throwable  ], 474:[476:java/lang/Throwable  ], 478:[476:java/lang/Throwable  ]]
+TRY [298:[480:java/lang/Throwable  ], 299:[472:java/lang/Throwable  , 480:java/lang/Throwable  ], 302:[472:java/lang/Throwable  , 480:java/lang/Throwable  ], 304:[472:java/lang/Throwable  , 480:java/lang/Throwable  ], 305:[472:java/lang/Throwable  , 480:java/lang/Throwable  ], 307:[472:java/lang/Throwable  , 480:java/lang/Throwable  ], 308:[472:java/lang/Throwable  , 480:java/lang/Throwable  ], 309:[472:java/lang/Throwable  , 480:java/lang/Throwable  ], 310:[472:java/lang/Throwable  , 480:java/lang/Throwable  ], 313:[472:java/lang/Throwable  , 480:java/lang/Throwable  ], 317:[472:java/lang/Throwable  , 480:java/lang/Throwable  ], 320:[472:java/lang/Throwable  , 480:java/lang/Throwable  ], 323:[472:java/lang/Throwable  , 480:java/lang/Throwable  ], 327:[472:java/lang/Throwable  , 480:java/lang/Throwable  ], 330:[472:java/lang/Throwable  , 480:java/lang/Throwable  ], 333:[472:java/lang/Throwable  , 480:java/lang/Throwable  ], 339:[472:java/lang/Throwable  , 480:java/lang/Throwable  ], 341:[472:java/lang/Throwable  , 480:java/lang/Throwable  ], 343:[472:java/lang/Throwable  , 480:java/lang/Throwable  ], 345:[472:java/lang/Throwable  , 480:java/lang/Throwable  ], 346:[472:java/lang/Throwable  , 480:java/lang/Throwable  ], 349:[472:java/lang/Throwable  , 480:java/lang/Throwable  ], 351:[472:java/lang/Throwable  , 480:java/lang/Throwable  ], 353:[472:java/lang/Throwable  , 480:java/lang/Throwable  ], 354:[472:java/lang/Throwable  , 480:java/lang/Throwable  ], 357:[472:java/lang/Throwable  , 480:java/lang/Throwable  ], 360:[472:java/lang/Throwable  , 480:java/lang/Throwable  ], 364:[472:java/lang/Throwable  , 480:java/lang/Throwable  ], 368:[472:java/lang/Throwable  , 480:java/lang/Throwable  ], 369:[472:java/lang/Throwable  , 480:java/lang/Throwable  ], 372:[472:java/lang/Throwable  , 480:java/lang/Throwable  ], 376:[472:java/lang/Throwable  , 480:java/lang/Throwable  ], 381:[472:java/lang/Throwable  , 480:java/lang/Throwable  ], 386:[472:java/lang/Throwable  , 480:java/lang/Throwable  ], 387:[472:java/lang/Throwable  , 480:java/lang/Throwable  ], 390:[472:java/lang/Throwable  , 480:java/lang/Throwable  ], 395:[472:java/lang/Throwable  , 480:java/lang/Throwable  ], 398:[472:java/lang/Throwable  , 480:java/lang/Throwable  ], 412:[472:java/lang/Throwable  , 480:java/lang/Throwable  ], 418:[472:java/lang/Throwable  , 480:java/lang/Throwable  ], 419:[472:java/lang/Throwable  , 480:java/lang/Throwable  ], 421:[472:java/lang/Throwable  , 480:java/lang/Throwable  ], 423:[472:java/lang/Throwable  , 480:java/lang/Throwable  ], 425:[472:java/lang/Throwable  , 480:java/lang/Throwable  ], 427:[472:java/lang/Throwable  , 480:java/lang/Throwable  ], 429:[480:java/lang/Throwable  ], 433:[472:java/lang/Throwable  , 480:java/lang/Throwable  ], 434:[472:java/lang/Throwable  , 480:java/lang/Throwable  ], 435:[472:java/lang/Throwable  , 480:java/lang/Throwable  ], 437:[472:java/lang/Throwable  , 480:java/lang/Throwable  ], 440:[472:java/lang/Throwable  , 480:java/lang/Throwable  ], 442:[472:java/lang/Throwable  , 480:java/lang/Throwable  ], 443:[472:java/lang/Throwable  , 480:java/lang/Throwable  ], 447:[472:java/lang/Throwable  , 480:java/lang/Throwable  ], 451:[472:java/lang/Throwable  , 480:java/lang/Throwable  ], 454:[472:java/lang/Throwable  , 480:java/lang/Throwable  ], 456:[472:java/lang/Throwable  , 480:java/lang/Throwable  ], 460:[472:java/lang/Throwable  , 480:java/lang/Throwable  ], 463:[472:java/lang/Throwable  , 480:java/lang/Throwable  ], 468:[472:java/lang/Throwable  , 480:java/lang/Throwable  ], 470:[472:java/lang/Throwable  , 480:java/lang/Throwable  ], 474:[472:java/lang/Throwable  , 480:java/lang/Throwable  ], 476:[480:java/lang/Throwable  ], 478:[480:java/lang/Throwable  ], 482:[480:java/lang/Throwable  ]]
 ALOAD 1
 INVOKESTATIC local/lito381/server/Protocol.objectSpellRequest([B)Llocal/lito381/server/Protocol$ObjectSpellRequest;
 ALOAD 0
```

### `useBodyToMindSpell([BLlocal/lito381/server/BattleSpellCatalog$BodyToMindSpell;)V`

- 類別：`local/lito381/server/ClientSession`
- 類型：例外表涵蓋、含迴圈（路徑比較器不支援）；事件後繼：`LAYOUT_ONLY`
- 比較器：`method useBodyToMindSpell([BLlocal/lito381/server/BattleSpellCatalog$BodyToMindSpell;)V: left=40:[151:java/lang/Throwable  ] right=38:[149:java/lang/Throwable  ]; paths rejected: normal control-flow cycle; labelled CFGs differ`

```diff
@@ -1,5 +1,5 @@
 META 2 null [java/io/IOException]
-TRY [40:[151:java/lang/Throwable  ], 42:[151:java/lang/Throwable  ], 43:[151:java/lang/Throwable  ], 45:[151:java/lang/Throwable  ], 46:[151:java/lang/Throwable  ], 47:[151:java/lang/Throwable  ], 48:[151:java/lang/Throwable  ], 55:[151:java/lang/Throwable  ], 56:[151:java/lang/Throwable  ], 59:[151:java/lang/Throwable  ], 60:[151:java/lang/Throwable  ], 63:[151:java/lang/Throwable  ], 66:[151:java/lang/Throwable  ], 68:[151:java/lang/Throwable  ], 71:[151:java/lang/Throwable  ], 73:[151:java/lang/Throwable  ], 78:[151:java/lang/Throwable  ], 79:[151:java/lang/Throwable  ], 80:[151:java/lang/Throwable  ], 82:[151:java/lang/Throwable  ], 86:[151:java/lang/Throwable  ], 87:[151:java/lang/Throwable  ], 92:[151:java/lang/Throwable  ], 93:[151:java/lang/Throwable  ], 95:[151:java/lang/Throwable  ], 96:[151:java/lang/Throwable  ], 97:[151:java/lang/Throwable  ], 100:[151:java/lang/Throwable  ], 102:[151:java/lang/Throwable  ], 103:[151:java/lang/Throwable  ], 104:[151:java/lang/Throwable  ], 106:[151:java/lang/Throwable  ], 110:[151:java/lang/Throwable  ], 111:[151:java/lang/Throwable  ], 112:[151:java/lang/Throwable  ], 116:[151:java/lang/Throwable  ], 117:[151:java/lang/Throwable  ], 118:[151:java/lang/Throwable  ], 122:[151:java/lang/Throwable  ], 124:[151:java/lang/Throwable  ], 125:[151:java/lang/Throwable  ], 126:[151:java/lang/Throwable  ], 129:[151:java/lang/Throwable  ], 132:[151:java/lang/Throwable  ], 135:[151:java/lang/Throwable  ], 139:[151:java/lang/Throwable  ], 142:[151:java/lang/Throwable  ], 147:[151:java/lang/Throwable  ], 149:[151:java/lang/Throwable  ], 153:[151:java/lang/Throwable  ]]
+TRY [38:[149:java/lang/Throwable  ], 40:[149:java/lang/Throwable  ], 41:[149:java/lang/Throwable  ], 43:[149:java/lang/Throwable  ], 44:[149:java/lang/Throwable  ], 45:[149:java/lang/Throwable  ], 46:[149:java/lang/Throwable  ], 53:[149:java/lang/Throwable  ], 54:[149:java/lang/Throwable  ], 57:[149:java/lang/Throwable  ], 58:[149:java/lang/Throwable  ], 61:[149:java/lang/Throwable  ], 64:[149:java/lang/Throwable  ], 66:[149:java/lang/Throwable  ], 69:[149:java/lang/Throwable  ], 71:[149:java/lang/Throwable  ], 76:[149:java/lang/Throwable  ], 77:[149:java/lang/Throwable  ], 78:[149:java/lang/Throwable  ], 80:[149:java/lang/Throwable  ], 84:[149:java/lang/Throwable  ], 85:[149:java/lang/Throwable  ], 90:[149:java/lang/Throwable  ], 91:[149:java/lang/Throwable  ], 93:[149:java/lang/Throwable  ], 94:[149:java/lang/Throwable  ], 95:[149:java/lang/Throwable  ], 98:[149:java/lang/Throwable  ], 100:[149:java/lang/Throwable  ], 101:[149:java/lang/Throwable  ], 102:[149:java/lang/Throwable  ], 104:[149:java/lang/Throwable  ], 108:[149:java/lang/Throwable  ], 109:[149:java/lang/Throwable  ], 110:[149:java/lang/Throwable  ], 114:[149:java/lang/Throwable  ], 115:[149:java/lang/Throwable  ], 116:[149:java/lang/Throwable  ], 120:[149:java/lang/Throwable  ], 122:[149:java/lang/Throwable  ], 123:[149:java/lang/Throwable  ], 124:[149:java/lang/Throwable  ], 127:[149:java/lang/Throwable  ], 130:[149:java/lang/Throwable  ], 133:[149:java/lang/Throwable  ], 137:[149:java/lang/Throwable  ], 140:[149:java/lang/Throwable  ], 145:[149:java/lang/Throwable  ], 147:[149:java/lang/Throwable  ], 151:[149:java/lang/Throwable  ]]
 ALOAD 1
 INVOKESTATIC local/lito381/server/Protocol.selectorOnlySpellRequest([B)Llocal/lito381/server/Protocol$SpellSelector;
 ASTORE 3
```

### `useBounceAttackSpell([BLlocal/lito381/server/BattleSpellCatalog$BounceAttackSpell;)V`

- 類別：`local/lito381/server/ClientSession`
- 類型：例外表涵蓋、含迴圈（路徑比較器不支援）；事件後繼：`LAYOUT_ONLY`
- 比較器：`method useBounceAttackSpell([BLlocal/lito381/server/BattleSpellCatalog$BounceAttackSpell;)V: left=118:[270:java/lang/Throwable  ] right=119:[273:java/lang/Throwable  ]; paths rejected: normal control-flow cycle; labelled CFGs differ`

```diff
@@ -1,5 +1,5 @@
 META 2 null [java/io/IOException]
-TRY [118:[270:java/lang/Throwable  ], 120:[270:java/lang/Throwable  ], 121:[270:java/lang/Throwable  ], 123:[270:java/lang/Throwable  ], 124:[270:java/lang/Throwable  ], 125:[270:java/lang/Throwable  ], 126:[270:java/lang/Throwable  ], 129:[270:java/lang/Throwable  ], 133:[270:java/lang/Throwable  ], 136:[270:java/lang/Throwable  ], 139:[270:java/lang/Throwable  ], 143:[270:java/lang/Throwable  ], 147:[270:java/lang/Throwable  ], 150:[270:java/lang/Throwable  ], 154:[270:java/lang/Throwable  ], 159:[270:java/lang/Throwable  ], 164:[270:java/lang/Throwable  ], 167:[270:java/lang/Throwable  ], 180:[270:java/lang/Throwable  ], 182:[270:java/lang/Throwable  ], 184:[270:java/lang/Throwable  ], 186:[270:java/lang/Throwable  ], 191:[270:java/lang/Throwable  ], 193:[270:java/lang/Throwable  ], 194:[270:java/lang/Throwable  ], 196:[270:java/lang/Throwable  ], 197:[270:java/lang/Throwable  ], 198:[270:java/lang/Throwable  ], 200:[270:java/lang/Throwable  ], 203:[270:java/lang/Throwable  ], 205:[270:java/lang/Throwable  ], 211:[270:java/lang/Throwable  ], 220:[270:java/lang/Throwable  ], 221:[270:java/lang/Throwable  ], 222:[270:java/lang/Throwable  ], 224:[270:java/lang/Throwable  ], 227:[270:java/lang/Throwable  ], 229:[270:java/lang/Throwable  ], 231:[270:java/lang/Throwable  ], 233:[270:java/lang/Throwable  ], 234:[270:java/lang/Throwable  ], 237:[270:java/lang/Throwable  ], 239:[270:java/lang/Throwable  ], 241:[270:java/lang/Throwable  ], 245:[270:java/lang/Throwable  ], 248:[270:java/lang/Throwable  ], 249:[270:java/lang/Throwable  ], 252:[270:java/lang/Throwable  ], 255:[270:java/lang/Throwable  ], 260:[270:java/lang/Throwable  ], 262:[270:java/lang/Throwable  ], 265:[270:java/lang/Throwable  ], 268:[270:java/lang/Throwable  ], 272:[270:java/lang/Throwable  ]]
+TRY [119:[273:java/lang/Throwable  ], 121:[273:java/lang/Throwable  ], 122:[273:java/lang/Throwable  ], 124:[273:java/lang/Throwable  ], 125:[273:java/lang/Throwable  ], 126:[273:java/lang/Throwable  ], 127:[273:java/lang/Throwable  ], 130:[273:java/lang/Throwable  ], 134:[273:java/lang/Throwable  ], 137:[273:java/lang/Throwable  ], 140:[273:java/lang/Throwable  ], 144:[273:java/lang/Throwable  ], 148:[273:java/lang/Throwable  ], 151:[273:java/lang/Throwable  ], 155:[273:java/lang/Throwable  ], 160:[273:java/lang/Throwable  ], 165:[273:java/lang/Throwable  ], 168:[273:java/lang/Throwable  ], 181:[273:java/lang/Throwable  ], 183:[273:java/lang/Throwable  ], 185:[273:java/lang/Throwable  ], 187:[273:java/lang/Throwable  ], 192:[273:java/lang/Throwable  ], 194:[273:java/lang/Throwable  ], 195:[273:java/lang/Throwable  ], 197:[273:java/lang/Throwable  ], 198:[273:java/lang/Throwable  ], 199:[273:java/lang/Throwable  ], 201:[273:java/lang/Throwable  ], 204:[273:java/lang/Throwable  ], 206:[273:java/lang/Throwable  ], 212:[273:java/lang/Throwable  ], 221:[273:java/lang/Throwable  ], 222:[273:java/lang/Throwable  ], 223:[273:java/lang/Throwable  ], 225:[273:java/lang/Throwable  ], 228:[273:java/lang/Throwable  ], 230:[273:java/lang/Throwable  ], 232:[273:java/lang/Throwable  ], 234:[273:java/lang/Throwable  ], 235:[273:java/lang/Throwable  ], 238:[273:java/lang/Throwable  ], 240:[273:java/lang/Throwable  ], 242:[273:java/lang/Throwable  ], 245:[273:java/lang/Throwable  ], 249:[273:java/lang/Throwable  ], 252:[273:java/lang/Throwable  ], 255:[273:java/lang/Throwable  ], 258:[273:java/lang/Throwable  ], 263:[273:java/lang/Throwable  ], 265:[273:java/lang/Throwable  ], 268:[273:java/lang/Throwable  ], 271:[273:java/lang/Throwable  ], 275:[273:java/lang/Throwable  ]]
 ALOAD 1
 INVOKESTATIC local/lito381/server/Protocol.selectorOnlySpellRequest([B)Llocal/lito381/server/Protocol$SpellSelector;
 ASTORE 3
```

### `useBringStoneSpell([BLlocal/lito381/server/BattleSpellCatalog$BringStoneSpell;)V`

- 類別：`local/lito381/server/ClientSession`
- 類型：例外表涵蓋、含迴圈（路徑比較器不支援）；事件後繼：`LAYOUT_ONLY`
- 比較器：`method useBringStoneSpell([BLlocal/lito381/server/BattleSpellCatalog$BringStoneSpell;)V: left=190:[404:java/lang/Throwable  ] right=188:[400:java/lang/Throwable  ]; paths rejected: normal control-flow cycle; labelled CFGs differ`

```diff
@@ -1,5 +1,5 @@
 META 2 null [java/io/IOException]
-TRY [190:[404:java/lang/Throwable  ], 192:[404:java/lang/Throwable  ], 193:[404:java/lang/Throwable  ], 195:[404:java/lang/Throwable  ], 196:[404:java/lang/Throwable  ], 197:[404:java/lang/Throwable  ], 198:[404:java/lang/Throwable  ], 205:[404:java/lang/Throwable  ], 206:[404:java/lang/Throwable  ], 209:[404:java/lang/Throwable  ], 210:[404:java/lang/Throwable  ], 213:[404:java/lang/Throwable  ], 214:[404:java/lang/Throwable  ], 217:[404:java/lang/Throwable  ], 218:[404:java/lang/Throwable  ], 221:[404:java/lang/Throwable  ], 224:[404:java/lang/Throwable  ], 226:[404:java/lang/Throwable  ], 228:[404:java/lang/Throwable  ], 230:[404:java/lang/Throwable  ], 235:[404:java/lang/Throwable  ], 236:[404:java/lang/Throwable  ], 237:[404:java/lang/Throwable  ], 239:[404:java/lang/Throwable  ], 242:[404:java/lang/Throwable  ], 248:[404:java/lang/Throwable  ], 249:[404:java/lang/Throwable  ], 251:[404:java/lang/Throwable  ], 253:[404:java/lang/Throwable  ], 256:[404:java/lang/Throwable  ], 258:[404:java/lang/Throwable  ], 259:[404:java/lang/Throwable  ], 266:[404:java/lang/Throwable  ], 267:[404:java/lang/Throwable  ], 268:[404:java/lang/Throwable  ], 275:[404:java/lang/Throwable  ], 278:[404:java/lang/Throwable  ], 282:[404:java/lang/Throwable  ], 285:[404:java/lang/Throwable  ], 288:[404:java/lang/Throwable  ], 289:[404:java/lang/Throwable  ], 293:[404:java/lang/Throwable  ], 296:[404:java/lang/Throwable  ], 301:[404:java/lang/Throwable  ], 302:[404:java/lang/Throwable  ], 306:[404:java/lang/Throwable  ], 308:[404:java/lang/Throwable  ], 310:[404:java/lang/Throwable  ], 313:[404:java/lang/Throwable  ], 314:[404:java/lang/Throwable  ], 318:[404:java/lang/Throwable  ], 322:[404:java/lang/Throwable  ], 323:[404:java/lang/Throwable  ], 325:[404:java/lang/Throwable  ], 328:[404:java/lang/Throwable  ], 333:[404:java/lang/Throwable  ], 335:[404:java/lang/Throwable  ], 338:[404:java/lang/Throwable  ], 345:[404:java/lang/Throwable  ], 350:[404:java/lang/Throwable  ], 352:[404:java/lang/Throwable  ], 356:[404:java/lang/Throwable  ], 357:[404:java/lang/Throwable  ], 358:[404:java/lang/Throwable  ], 364:[404:java/lang/Throwable  ], 367:[404:java/lang/Throwable  ], 368:[404:java/lang/Throwable  ], 370:[404:java/lang/Throwable  ], 374:[404:java/lang/Throwable  ], 377:[404:java/lang/Throwable  ], 382:[404:java/lang/Throwable  ], 384:[404:java/lang/Throwable  ], 387:[404:java/lang/Throwable  ], 390:[404:java/lang/Throwable  ], 393:[404:java/lang/Throwable  ], 396:[404:java/lang/Throwable  ], 399:[404:java/lang/Throwable  ], 402:[404:java/lang/Throwable  ], 406:[404:java/lang/Throwable  ]]
+TRY [188:[400:java/lang/Throwable  ], 190:[400:java/lang/Throwable  ], 191:[400:java/lang/Throwable  ], 193:[400:java/lang/Throwable  ], 194:[400:java/lang/Throwable  ], 195:[400:java/lang/Throwable  ], 196:[400:java/lang/Throwable  ], 203:[400:java/lang/Throwable  ], 204:[400:java/lang/Throwable  ], 207:[400:java/lang/Throwable  ], 208:[400:java/lang/Throwable  ], 211:[400:java/lang/Throwable  ], 212:[400:java/lang/Throwable  ], 215:[400:java/lang/Throwable  ], 216:[400:java/lang/Throwable  ], 219:[400:java/lang/Throwable  ], 222:[400:java/lang/Throwable  ], 224:[400:java/lang/Throwable  ], 226:[400:java/lang/Throwable  ], 228:[400:java/lang/Throwable  ], 233:[400:java/lang/Throwable  ], 234:[400:java/lang/Throwable  ], 235:[400:java/lang/Throwable  ], 237:[400:java/lang/Throwable  ], 240:[400:java/lang/Throwable  ], 246:[400:java/lang/Throwable  ], 247:[400:java/lang/Throwable  ], 249:[400:java/lang/Throwable  ], 251:[400:java/lang/Throwable  ], 254:[400:java/lang/Throwable  ], 256:[400:java/lang/Throwable  ], 257:[400:java/lang/Throwable  ], 264:[400:java/lang/Throwable  ], 265:[400:java/lang/Throwable  ], 266:[400:java/lang/Throwable  ], 271:[400:java/lang/Throwable  ], 274:[400:java/lang/Throwable  ], 278:[400:java/lang/Throwable  ], 281:[400:java/lang/Throwable  ], 284:[400:java/lang/Throwable  ], 285:[400:java/lang/Throwable  ], 289:[400:java/lang/Throwable  ], 292:[400:java/lang/Throwable  ], 297:[400:java/lang/Throwable  ], 298:[400:java/lang/Throwable  ], 302:[400:java/lang/Throwable  ], 304:[400:java/lang/Throwable  ], 306:[400:java/lang/Throwable  ], 309:[400:java/lang/Throwable  ], 310:[400:java/lang/Throwable  ], 314:[400:java/lang/Throwable  ], 318:[400:java/lang/Throwable  ], 319:[400:java/lang/Throwable  ], 321:[400:java/lang/Throwable  ], 324:[400:java/lang/Throwable  ], 329:[400:java/lang/Throwable  ], 331:[400:java/lang/Throwable  ], 334:[400:java/lang/Throwable  ], 341:[400:java/lang/Throwable  ], 346:[400:java/lang/Throwable  ], 348:[400:java/lang/Throwable  ], 352:[400:java/lang/Throwable  ], 353:[400:java/lang/Throwable  ], 354:[400:java/lang/Throwable  ], 357:[400:java/lang/Throwable  ], 361:[400:java/lang/Throwable  ], 364:[400:java/lang/Throwable  ], 366:[400:java/lang/Throwable  ], 370:[400:java/lang/Throwable  ], 373:[400:java/lang/Throwable  ], 378:[400:java/lang/Throwable  ], 380:[400:java/lang/Throwable  ], 383:[400:java/lang/Throwable  ], 386:[400:java/lang/Throwable  ], 389:[400:java/lang/Throwable  ], 392:[400:java/lang/Throwable  ], 395:[400:java/lang/Throwable  ], 398:[400:java/lang/Throwable  ], 402:[400:java/lang/Throwable  ]]
 ALOAD 1
 INVOKESTATIC local/lito381/server/Protocol.objectSpellRequest([B)Llocal/lito381/server/Protocol$ObjectSpellRequest;
 ALOAD 0
```

### `useBurningSlashSpell([BLlocal/lito381/server/BattleSpellCatalog$BurningSlashSpell;)V`

- 類別：`local/lito381/server/ClientSession`
- 類型：例外表涵蓋、含迴圈（路徑比較器不支援）；事件後繼：`LAYOUT_ONLY`
- 比較器：`method useBurningSlashSpell([BLlocal/lito381/server/BattleSpellCatalog$BurningSlashSpell;)V: left=94:[226:java/lang/Throwable  ] right=96:[228:java/lang/Throwable  ]; paths rejected: normal control-flow cycle; labelled CFGs differ`

```diff
@@ -1,5 +1,5 @@
 META 2 null [java/io/IOException]
-TRY [94:[226:java/lang/Throwable  ], 96:[226:java/lang/Throwable  ], 97:[226:java/lang/Throwable  ], 99:[226:java/lang/Throwable  ], 100:[226:java/lang/Throwable  ], 101:[226:java/lang/Throwable  ], 102:[226:java/lang/Throwable  ], 105:[226:java/lang/Throwable  ], 109:[226:java/lang/Throwable  ], 112:[226:java/lang/Throwable  ], 115:[226:java/lang/Throwable  ], 119:[226:java/lang/Throwable  ], 123:[226:java/lang/Throwable  ], 126:[226:java/lang/Throwable  ], 130:[226:java/lang/Throwable  ], 135:[226:java/lang/Throwable  ], 140:[226:java/lang/Throwable  ], 143:[226:java/lang/Throwable  ], 156:[226:java/lang/Throwable  ], 158:[226:java/lang/Throwable  ], 160:[226:java/lang/Throwable  ], 165:[226:java/lang/Throwable  ], 167:[226:java/lang/Throwable  ], 168:[226:java/lang/Throwable  ], 170:[226:java/lang/Throwable  ], 171:[226:java/lang/Throwable  ], 172:[226:java/lang/Throwable  ], 174:[226:java/lang/Throwable  ], 177:[226:java/lang/Throwable  ], 179:[226:java/lang/Throwable  ], 188:[226:java/lang/Throwable  ], 189:[226:java/lang/Throwable  ], 190:[226:java/lang/Throwable  ], 192:[226:java/lang/Throwable  ], 195:[226:java/lang/Throwable  ], 196:[226:java/lang/Throwable  ], 200:[226:java/lang/Throwable  ], 202:[226:java/lang/Throwable  ], 204:[226:java/lang/Throwable  ], 207:[226:java/lang/Throwable  ], 211:[226:java/lang/Throwable  ], 214:[226:java/lang/Throwable  ], 219:[226:java/lang/Throwable  ], 221:[226:java/lang/Throwable  ], 224:[226:java/lang/Throwable  ], 228:[226:java/lang/Throwable  ]]
+TRY [96:[228:java/lang/Throwable  ], 98:[228:java/lang/Throwable  ], 99:[228:java/lang/Throwable  ], 101:[228:java/lang/Throwable  ], 102:[228:java/lang/Throwable  ], 103:[228:java/lang/Throwable  ], 104:[228:java/lang/Throwable  ], 107:[228:java/lang/Throwable  ], 111:[228:java/lang/Throwable  ], 114:[228:java/lang/Throwable  ], 117:[228:java/lang/Throwable  ], 121:[228:java/lang/Throwable  ], 125:[228:java/lang/Throwable  ], 128:[228:java/lang/Throwable  ], 132:[228:java/lang/Throwable  ], 137:[228:java/lang/Throwable  ], 142:[228:java/lang/Throwable  ], 145:[228:java/lang/Throwable  ], 158:[228:java/lang/Throwable  ], 160:[228:java/lang/Throwable  ], 162:[228:java/lang/Throwable  ], 167:[228:java/lang/Throwable  ], 169:[228:java/lang/Throwable  ], 170:[228:java/lang/Throwable  ], 172:[228:java/lang/Throwable  ], 173:[228:java/lang/Throwable  ], 174:[228:java/lang/Throwable  ], 176:[228:java/lang/Throwable  ], 179:[228:java/lang/Throwable  ], 181:[228:java/lang/Throwable  ], 190:[228:java/lang/Throwable  ], 191:[228:java/lang/Throwable  ], 192:[228:java/lang/Throwable  ], 194:[228:java/lang/Throwable  ], 197:[228:java/lang/Throwable  ], 198:[228:java/lang/Throwable  ], 202:[228:java/lang/Throwable  ], 204:[228:java/lang/Throwable  ], 206:[228:java/lang/Throwable  ], 209:[228:java/lang/Throwable  ], 213:[228:java/lang/Throwable  ], 216:[228:java/lang/Throwable  ], 221:[228:java/lang/Throwable  ], 223:[228:java/lang/Throwable  ], 226:[228:java/lang/Throwable  ], 230:[228:java/lang/Throwable  ]]
 ALOAD 1
 INVOKESTATIC local/lito381/server/Protocol.selectorOnlySpellRequest([B)Llocal/lito381/server/Protocol$SpellSelector;
 ASTORE 3
```

### `useCallClanSpell([BLlocal/lito381/server/BattleSpellCatalog$CallClanSpell;)V`

- 類別：`local/lito381/server/ClientSession`
- 類型：例外表涵蓋、含迴圈（路徑比較器不支援）；事件後繼：`LAYOUT_ONLY`
- 比較器：`method useCallClanSpell([BLlocal/lito381/server/BattleSpellCatalog$CallClanSpell;)V: left=154:[284:java/lang/Throwable  ] right=155:[287:java/lang/Throwable  ]; paths rejected: normal control-flow cycle; labelled CFGs differ`

```diff
@@ -1,5 +1,5 @@
 META 2 null [java/io/IOException]
-TRY [154:[284:java/lang/Throwable  ], 156:[284:java/lang/Throwable  ], 157:[284:java/lang/Throwable  ], 159:[284:java/lang/Throwable  ], 160:[284:java/lang/Throwable  ], 161:[284:java/lang/Throwable  ], 162:[284:java/lang/Throwable  ], 167:[284:java/lang/Throwable  ], 168:[284:java/lang/Throwable  ], 171:[284:java/lang/Throwable  ], 175:[284:java/lang/Throwable  ], 183:[284:java/lang/Throwable  ], 184:[284:java/lang/Throwable  ], 187:[284:java/lang/Throwable  ], 192:[284:java/lang/Throwable  ], 205:[284:java/lang/Throwable  ], 211:[284:java/lang/Throwable  ], 214:[284:java/lang/Throwable  ], 217:[284:java/lang/Throwable  ], 220:[284:java/lang/Throwable  ], 222:[284:java/lang/Throwable  ], 228:[284:java/lang/Throwable  ], 230:[284:java/lang/Throwable  ], 232:[284:java/lang/Throwable  ], 236:[284:java/lang/Throwable  ], 237:[284:java/lang/Throwable  ], 238:[284:java/lang/Throwable  ], 242:[284:java/lang/Throwable  ], 245:[284:java/lang/Throwable  ], 246:[284:java/lang/Throwable  ], 248:[284:java/lang/Throwable  ], 252:[284:java/lang/Throwable  ], 255:[284:java/lang/Throwable  ], 260:[284:java/lang/Throwable  ], 261:[284:java/lang/Throwable  ], 269:[284:java/lang/Throwable  ], 271:[284:java/lang/Throwable  ], 273:[284:java/lang/Throwable  ], 275:[284:java/lang/Throwable  ], 279:[284:java/lang/Throwable  ], 282:[284:java/lang/Throwable  ], 286:[284:java/lang/Throwable  ]]
+TRY [155:[287:java/lang/Throwable  ], 157:[287:java/lang/Throwable  ], 158:[287:java/lang/Throwable  ], 160:[287:java/lang/Throwable  ], 161:[287:java/lang/Throwable  ], 162:[287:java/lang/Throwable  ], 163:[287:java/lang/Throwable  ], 168:[287:java/lang/Throwable  ], 169:[287:java/lang/Throwable  ], 172:[287:java/lang/Throwable  ], 176:[287:java/lang/Throwable  ], 184:[287:java/lang/Throwable  ], 185:[287:java/lang/Throwable  ], 188:[287:java/lang/Throwable  ], 193:[287:java/lang/Throwable  ], 206:[287:java/lang/Throwable  ], 212:[287:java/lang/Throwable  ], 215:[287:java/lang/Throwable  ], 218:[287:java/lang/Throwable  ], 221:[287:java/lang/Throwable  ], 223:[287:java/lang/Throwable  ], 229:[287:java/lang/Throwable  ], 231:[287:java/lang/Throwable  ], 233:[287:java/lang/Throwable  ], 237:[287:java/lang/Throwable  ], 238:[287:java/lang/Throwable  ], 239:[287:java/lang/Throwable  ], 242:[287:java/lang/Throwable  ], 246:[287:java/lang/Throwable  ], 249:[287:java/lang/Throwable  ], 251:[287:java/lang/Throwable  ], 255:[287:java/lang/Throwable  ], 258:[287:java/lang/Throwable  ], 263:[287:java/lang/Throwable  ], 264:[287:java/lang/Throwable  ], 272:[287:java/lang/Throwable  ], 274:[287:java/lang/Throwable  ], 276:[287:java/lang/Throwable  ], 278:[287:java/lang/Throwable  ], 282:[287:java/lang/Throwable  ], 285:[287:java/lang/Throwable  ], 289:[287:java/lang/Throwable  ]]
 ALOAD 0
 GETFIELD local/lito381/server/ClientSession.phase:Llocal/lito381/server/ClientSession$Phase;
 ASTORE 3
```

### `useClassicWorldAreaAttackSpell(Llocal/lito381/server/Protocol$AttackSpellRequest;Llocal/lito381/server/BattleSpellCatalog$AreaAttackSpell;)V`

- 類別：`local/lito381/server/ClientSession`
- 類型：路徑事件多重集合不同（長度相同）；事件後繼：`LAYOUT_ONLY`
- 比較器：`method useClassicWorldAreaAttackSpell(Llocal/lito381/server/Protocol$AttackSpellRequest;Llocal/lito381/server/BattleSpellCatalog$AreaAttackSpell;)V insn[10]: left=INVOKEDYNAMIC run(Llocal/lito381/server/ClientSession;Llocal/lito381/server/Protocol$AttackSpellRequest;Llocal/lito381/server/BattleSpellCatalog$AreaAttackSpell;)Llocal/lito381/server/NativeSelfCastAction$Task; bsm=H6 java/lang/invoke/La`

```diff
@@ -10,6 +10,6 @@
 ALOAD 0
 ALOAD 1
 ALOAD 2
-INVOKEDYNAMIC run(Llocal/lito381/server/ClientSession;Llocal/lito381/server/Protocol$AttackSpellRequest;Llocal/lito381/server/BattleSpellCatalog$AreaAttackSpell;)Llocal/lito381/server/NativeSelfCastAction$Task; bsm=H6 java/lang/invoke/LambdaMetafactory.metafactory(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodHandle;Ljava/lang/invoke/MethodType;)Ljava/lang/invoke/CallSite; args=[type:()V, handle:H7 local/lito381/server/ClientSession.lambda$content$0f34469fb86a43461a7b178f3b03c9a24e9f45f14b4d5dd57c7a2f1c43006be3(Llocal/lito381/server/Protocol$AttackSpellRequest;Llocal/lito381/server/BattleSpellCatalog$AreaAttackSpell;)V, type:()V]
+INVOKEDYNAMIC run(Llocal/lito381/server/ClientSession;Llocal/lito381/server/Protocol$AttackSpellRequest;Llocal/lito381/server/BattleSpellCatalog$AreaAttackSpell;)Llocal/lito381/server/NativeSelfCastAction$Task; bsm=H6 java/lang/invoke/LambdaMetafactory.metafactory(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodHandle;Ljava/lang/invoke/MethodType;)Ljava/lang/invoke/CallSite; args=[type:()V, handle:H7 local/lito381/server/ClientSession.nativeCompanionTargetuseClassicWorldAreaAttackSpell(Llocal/lito381/server/Protocol$AttackSpellRequest;Llocal/lito381/server/BattleSpellCatalog$AreaAttackSpell;)V, type:()V]
 INVOKESTATIC local/lito381/server/NativeCompanionTargets.cast(Llocal/lito381/server/ClientSession;ILlocal/lito381/server/NativeSelfCastAction$Task;)V
 RETURN
```

### `useClassicWorldAttackSpell(Llocal/lito381/server/Protocol$AttackSpellRequest;Llocal/lito381/server/BattleSpellCatalog$AttackSpell;)V`

- 類別：`local/lito381/server/ClientSession`
- 類型：路徑事件多重集合不同（長度相同）；事件後繼：`LAYOUT_ONLY`
- 比較器：`method useClassicWorldAttackSpell(Llocal/lito381/server/Protocol$AttackSpellRequest;Llocal/lito381/server/BattleSpellCatalog$AttackSpell;)V insn[10]: left=INVOKEDYNAMIC run(Llocal/lito381/server/ClientSession;Llocal/lito381/server/Protocol$AttackSpellRequest;Llocal/lito381/server/BattleSpellCatalog$AttackSpell;)Llocal/lito381/server/NativeSelfCastAction$Task; bsm=H6 java/lang/invoke/LambdaMet… rig`

```diff
@@ -10,6 +10,6 @@
 ALOAD 0
 ALOAD 1
 ALOAD 2
-INVOKEDYNAMIC run(Llocal/lito381/server/ClientSession;Llocal/lito381/server/Protocol$AttackSpellRequest;Llocal/lito381/server/BattleSpellCatalog$AttackSpell;)Llocal/lito381/server/NativeSelfCastAction$Task; bsm=H6 java/lang/invoke/LambdaMetafactory.metafactory(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodHandle;Ljava/lang/invoke/MethodType;)Ljava/lang/invoke/CallSite; args=[type:()V, handle:H7 local/lito381/server/ClientSession.lambda$content$1b57ad960992782b2c403d8904005ccc64db5388f417cdfec5450bfde96cb176(Llocal/lito381/server/Protocol$AttackSpellRequest;Llocal/lito381/server/BattleSpellCatalog$AttackSpell;)V, type:()V]
+INVOKEDYNAMIC run(Llocal/lito381/server/ClientSession;Llocal/lito381/server/Protocol$AttackSpellRequest;Llocal/lito381/server/BattleSpellCatalog$AttackSpell;)Llocal/lito381/server/NativeSelfCastAction$Task; bsm=H6 java/lang/invoke/LambdaMetafactory.metafactory(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodHandle;Ljava/lang/invoke/MethodType;)Ljava/lang/invoke/CallSite; args=[type:()V, handle:H7 local/lito381/server/ClientSession.nativeCompanionTargetuseClassicWorldAttackSpell(Llocal/lito381/server/Protocol$AttackSpellRequest;Llocal/lito381/server/BattleSpellCatalog$AttackSpell;)V, type:()V]
 INVOKESTATIC local/lito381/server/NativeCompanionTargets.cast(Llocal/lito381/server/ClientSession;ILlocal/lito381/server/NativeSelfCastAction$Task;)V
 RETURN
```

### `useClassicWorldCenteredAreaAttackSpell([BLlocal/lito381/server/BattleSpellCatalog$AreaAttackSpell;)V`

- 類別：`local/lito381/server/ClientSession`
- 類型：例外表涵蓋、含迴圈（路徑比較器不支援）；事件後繼：`LAYOUT_ONLY`
- 比較器：`method useClassicWorldCenteredAreaAttackSpell([BLlocal/lito381/server/BattleSpellCatalog$AreaAttackSpell;)V: left=103:[523:java/lang/Exception  ] right=105:[532:java/lang/Exception  ]; paths rejected: normal control-flow cycle; labelled CFGs differ`

```diff
@@ -1,5 +1,5 @@
 META 2 null [java/io/IOException]
-TRY [103:[523:java/lang/Exception  ], 106:[523:java/lang/Exception  ], 109:[523:java/lang/Exception  ], 112:[523:java/lang/Exception  ], 113:[523:java/lang/Exception  ], 119:[523:java/lang/Exception  ], 120:[523:java/lang/Exception  ], 123:[523:java/lang/Exception  ], 127:[523:java/lang/Exception  ], 131:[523:java/lang/Exception  ], 132:[523:java/lang/Exception  ], 135:[523:java/lang/Exception  ], 138:[523:java/lang/Exception  ], 139:[523:java/lang/Exception  ], 142:[523:java/lang/Exception  ], 143:[523:java/lang/Exception  ], 146:[523:java/lang/Exception  ], 149:[523:java/lang/Exception  ], 153:[523:java/lang/Exception  ], 155:[523:java/lang/Exception  ], 156:[523:java/lang/Exception  ], 161:[523:java/lang/Exception  ], 163:[523:java/lang/Exception  ], 164:[523:java/lang/Exception  ], 170:[523:java/lang/Exception  ], 172:[523:java/lang/Exception  ], 173:[523:java/lang/Exception  ], 179:[523:java/lang/Exception  ], 181:[523:java/lang/Exception  ], 185:[523:java/lang/Exception  ], 189:[523:java/lang/Exception  ], 190:[523:java/lang/Exception  ], 195:[523:java/lang/Exception  ], 197:[523:java/lang/Exception  ], 198:[523:java/lang/Exception  ], 202:[523:java/lang/Exception  ], 204:[523:java/lang/Exception  ], 206:[523:java/lang/Exception  ], 207:[523:java/lang/Exception  ], 208:[523:java/lang/Exception  ], 211:[523:java/lang/Exception  ], 217:[523:java/lang/Exception  ], 218:[523:java/lang/Exception  ], 223:[523:java/lang/Exception  ], 228:[523:java/lang/Exception  ], 229:[523:java/lang/Exception  ], 232:[523:java/lang/Exception  ], 235:[523:java/lang/Exception  ], 236:[523:java/lang/Exception  ], 239:[523:java/lang/Exception  ], 242:[523:java/lang/Exception  ], 245:[523:java/lang/Exception  ], 251:[523:java/lang/Exception  ], 253:[523:java/lang/Exception  ], 254:[523:java/lang/Exception  ], 260:[523:java/lang/Exception  ], 262:[523:java/lang/Exception  ], 263:[523:java/lang/Exception  ], 269:[523:java/lang/Exception  ], 271:[523:java/lang/Exception  ], 275:[523:java/lang/Exception  ], 278:[523:java/lang/Exception  ], 280:[523:java/lang/Exception  ], 281:[523:java/lang/Exception  ], 285:[523:java/lang/Exception  ], 287:[523:java/lang/Exception  ], 289:[523:java/lang/Exception  ], 290:[523:java/lang/Exception  ], 291:[523:java/lang/Exception  ], 295:[523:java/lang/Exception  ], 299:[523:java/lang/Exception  ], 300:[523:java/lang/Exception  ], 302:[523:java/lang/Exception  ], 307:[523:java/lang/Exception  ], 311:[523:java/lang/Exception  ], 312:[523:java/lang/Exception  ], 315:[523:java/lang/Exception  ], 318:[523:java/lang/Exception  ], 319:[523:java/lang/Exception  ], 324:[523:java/lang/Exception  ], 326:[523:java/lang/Exception  ], 327:[523:java/lang/Exception  ], 333:[523:java/lang/Exception  ], 335:[523:java/lang/Exception  ], 336:[523:java/lang/Exception  ], 342:[523:java/lang/Exception  ], 344:[523:java/lang/Exception  ], 348:[523:java/lang/Exception  ], 351:[523:java/lang/Exception  ], 353:[523:java/lang/Exception  ], 354:[523:java/lang/Exception  ], 358:[523:java/lang/Exception  ], 360:[523:java/lang/Exception  ], 362:[523:java/lang/Exception  ], 363:[523:java/lang/Exception  ], 364:[523:java/lang/Exception  ], 368:[523:java/lang/Exception  ], 372:[523:java/lang/Exception  ], 373:[523:java/lang/Exception  ], 375:[523:java/lang/Exception  ], 380:[523:java/lang/Exception  ], 384:[523:java/lang/Exception  ], 385:[523:java/lang/Exception  ], 388:[523:java/lang/Exception  ], 391:[523:java/lang/Exception  ], 392:[523:java/lang/Exception  ], 397:[523:java/lang/Exception  ], 399:[523:java/lang/Exception  ], 400:[523:java/lang/Exception  ], 406:[523:java/lang/Exception  ], 408:[523:java/lang/Exception  ], 409:[523:java/lang/Exception  ], 415:[523:java/lang/Exception  ], 417:[523:java/lang/Exception  ], 421:[523:java/lang/Exception  ], 424:[523:java/lang/Exception  ], 426:[523:java/lang/Exception  ], 427:[523:java/lang/Exception  ], 431:[523:java/lang/Exception  ], 433:[523:java/lang/Exception  ], 435:[523:java/lang/Exception  ], 436:[523:java/lang/Exception  ], 437:[523:java/lang/Exception  ], 441:[523:java/lang/Exception  ], 445:[523:java/lang/Exception  ], 446:[523:java/lang/Exception  ], 448:[523:java/lang/Exception  ], 455:[523:java/lang/Exception  ], 456:[523:java/lang/Exception  ], 459:[523:java/lang/Exception  ], 462:[523:java/lang/Exception  ], 463:[523:java/lang/Exception  ], 466:[523:java/lang/Exception  ], 471:[523:java/lang/Exception  ], 473:[523:java/lang/Exception  ], 474:[523:java/lang/Exception  ], 480:[523:java/lang/Exception  ], 482:[523:java/lang/Exception  ], 483:[523:java/lang/Exception  ], 489:[523:java/lang/Exception  ], 491:[523:java/lang/Exception  ], 495:[523:java/lang/Exception  ], 498:[523:java/lang/Exception  ], 500:[523:java/lang/Exception  ], 504:[523:java/lang/Exception  ], 506:[523:java/lang/Exception  ], 507:[523:java/lang/Exception  ], 508:[523:java/lang/Exception  ], 512:[523:java/lang/Exception  ], 516:[523:java/lang/Exception  ], 517:[523:java/lang/Exception  ], 519:[523:java/lang/Exception  ], 572:[717:java/lang/Throwable  ], 575:[717:java/lang/Throwable  ], 577:[717:java/lang/Throwable  ], 578:[717:java/lang/Throwable  ], 580:[717:java/lang/Throwable  ], 581:[717:java/lang/Throwable  ], 582:[717:java/lang/Throwable  ], 583:[717:java/lang/Throwable  ], 590:[717:java/lang/Throwable  ], 591:[717:java/lang/Throwable  ], 594:[717:java/lang/Throwable  ], 597:[717:java/lang/Throwable  ], 601:[717:java/lang/Throwable  ], 605:[717:java/lang/Throwable  ], 609:[717:java/lang/Throwable  ], 613:[717:java/lang/Throwable  ], 615:[717:java/lang/Throwable  ], 618:[717:java/lang/Throwable  ], 622:[717:java/lang/Throwable  ], 624:[717:java/lang/Throwable  ], 626:[717:java/lang/Throwable  ], 628:[717:java/lang/Throwable  ], 631:[717:java/lang/Throwable  ], 633:[717:java/lang/Throwable  ], 638:[717:java/lang/Throwable  ], 639:[717:java/lang/Throwable  ], 641:[717:java/lang/Throwable  ], 642:[717:java/lang/Throwable  ], 643:[717:java/lang/Throwable  ], 646:[717:java/lang/Throwable  ], 652:[717:java/lang/Throwable  ], 653:[717:java/lang/Throwable  ], 655:[717:java/lang/Throwable  ], 656:[717:java/lang/Throwable  ], 657:[717:java/lang/Throwable  ], 658:[717:java/lang/Throwable  ], 660:[717:java/lang/Throwable  ], 662:[717:java/lang/Throwable  ], 664:[717:java/lang/Throwable  ], 666:[717:java/lang/Throwable  ], 672:[717:java/lang/Throwable  ], 673:[717:java/lang/Throwable  ], 674:[717:java/lang/Throwable  ], 676:[717:java/lang/Throwable  ], 680:[717:java/lang/Throwable  ], 682:[717:java/lang/Throwable  ], 684:[717:java/lang/Throwable  ], 688:[717:java/lang/Throwable  ], 691:[717:java/lang/Throwable  ], 692:[717:java/lang/Throwable  ], 694:[717:java/lang/Throwable  ], 697:[717:java/lang/Throwable  ], 700:[717:java/lang/Throwable  ], 702:[717:java/lang/Throwable  ], 703:[717:java/lang/Throwable  ], 706:[717:java/lang/Throwable  ], 711:[717:java/lang/Throwable  ], 712:[717:java/lang/Throwable  ], 715:[717:java/lang/Throwable  ], 719:[717:java/lang/Throwable  ]]
+TRY [105:[532:java/lang/Exception  ], 108:[532:java/lang/Exception  ], 111:[532:java/lang/Exception  ], 114:[532:java/lang/Exception  ], 115:[532:java/lang/Exception  ], 121:[532:java/lang/Exception  ], 122:[532:java/lang/Exception  ], 125:[532:java/lang/Exception  ], 129:[532:java/lang/Exception  ], 134:[532:java/lang/Exception  ], 135:[532:java/lang/Exception  ], 138:[532:java/lang/Exception  ], 141:[532:java/lang/Exception  ], 142:[532:java/lang/Exception  ], 145:[532:java/lang/Exception  ], 146:[532:java/lang/Exception  ], 149:[532:java/lang/Exception  ], 152:[532:java/lang/Exception  ], 156:[532:java/lang/Exception  ], 158:[532:java/lang/Exception  ], 159:[532:java/lang/Exception  ], 164:[532:java/lang/Exception  ], 166:[532:java/lang/Exception  ], 167:[532:java/lang/Exception  ], 173:[532:java/lang/Exception  ], 175:[532:java/lang/Exception  ], 176:[532:java/lang/Exception  ], 182:[532:java/lang/Exception  ], 184:[532:java/lang/Exception  ], 188:[532:java/lang/Exception  ], 193:[532:java/lang/Exception  ], 194:[532:java/lang/Exception  ], 200:[532:java/lang/Exception  ], 202:[532:java/lang/Exception  ], 203:[532:java/lang/Exception  ], 207:[532:java/lang/Exception  ], 209:[532:java/lang/Exception  ], 211:[532:java/lang/Exception  ], 212:[532:java/lang/Exception  ], 213:[532:java/lang/Exception  ], 216:[532:java/lang/Exception  ], 222:[532:java/lang/Exception  ], 223:[532:java/lang/Exception  ], 228:[532:java/lang/Exception  ], 233:[532:java/lang/Exception  ], 234:[532:java/lang/Exception  ], 237:[532:java/lang/Exception  ], 240:[532:java/lang/Exception  ], 241:[532:java/lang/Exception  ], 244:[532:java/lang/Exception  ], 247:[532:java/lang/Exception  ], 250:[532:java/lang/Exception  ], 256:[532:java/lang/Exception  ], 258:[532:java/lang/Exception  ], 259:[532:java/lang/Exception  ], 265:[532:java/lang/Exception  ], 267:[532:java/lang/Exception  ], 268:[532:java/lang/Exception  ], 274:[532:java/lang/Exception  ], 276:[532:java/lang/Exception  ], 280:[532:java/lang/Exception  ], 284:[532:java/lang/Exception  ], 286:[532:java/lang/Exception  ], 287:[532:java/lang/Exception  ], 291:[532:java/lang/Exception  ], 293:[532:java/lang/Exception  ], 295:[532:java/lang/Exception  ], 296:[532:java/lang/Exception  ], 297:[532:java/lang/Exception  ], 301:[532:java/lang/Exception  ], 305:[532:java/lang/Exception  ], 306:[532:java/lang/Exception  ], 308:[532:java/lang/Exception  ], 313:[532:java/lang/Exception  ], 317:[532:java/lang/Exception  ], 318:[532:java/lang/Exception  ], 321:[532:java/lang/Exception  ], 324:[532:java/lang/Exception  ], 325:[532:java/lang/Exception  ], 330:[532:java/lang/Exception  ], 332:[532:java/lang/Exception  ], 333:[532:java/lang/Exception  ], 339:[532:java/lang/Exception  ], 341:[532:java/lang/Exception  ], 342:[532:java/lang/Exception  ], 348:[532:java/lang/Exception  ], 350:[532:java/lang/Exception  ], 354:[532:java/lang/Exception  ], 358:[532:java/lang/Exception  ], 360:[532:java/lang/Exception  ], 361:[532:java/lang/Exception  ], 365:[532:java/lang/Exception  ], 367:[532:java/lang/Exception  ], 369:[532:java/lang/Exception  ], 370:[532:java/lang/Exception  ], 371:[532:java/lang/Exception  ], 375:[532:java/lang/Exception  ], 379:[532:java/lang/Exception  ], 380:[532:java/lang/Exception  ], 382:[532:java/lang/Exception  ], 387:[532:java/lang/Exception  ], 391:[532:java/lang/Exception  ], 392:[532:java/lang/Exception  ], 395:[532:java/lang/Exception  ], 398:[532:java/lang/Exception  ], 399:[532:java/lang/Exception  ], 404:[532:java/lang/Exception  ], 406:[532:java/lang/Exception  ], 407:[532:java/lang/Exception  ], 413:[532:java/lang/Exception  ], 415:[532:java/lang/Exception  ], 416:[532:java/lang/Exception  ], 422:[532:java/lang/Exception  ], 424:[532:java/lang/Exception  ], 428:[532:java/lang/Exception  ], 432:[532:java/lang/Exception  ], 434:[532:java/lang/Exception  ], 435:[532:java/lang/Exception  ], 439:[532:java/lang/Exception  ], 441:[532:java/lang/Exception  ], 443:[532:java/lang/Exception  ], 444:[532:java/lang/Exception  ], 445:[532:java/lang/Exception  ], 449:[532:java/lang/Exception  ], 453:[532:java/lang/Exception  ], 454:[532:java/lang/Exception  ], 456:[532:java/lang/Exception  ], 463:[532:java/lang/Exception  ], 464:[532:java/lang/Exception  ], 467:[532:java/lang/Exception  ], 470:[532:java/lang/Exception  ], 471:[532:java/lang/Exception  ], 474:[532:java/lang/Exception  ], 479:[532:java/lang/Exception  ], 481:[532:java/lang/Exception  ], 482:[532:java/lang/Exception  ], 488:[532:java/lang/Exception  ], 490:[532:java/lang/Exception  ], 491:[532:java/lang/Exception  ], 497:[532:java/lang/Exception  ], 499:[532:java/lang/Exception  ], 503:[532:java/lang/Exception  ], 507:[532:java/lang/Exception  ], 509:[532:java/lang/Exception  ], 513:[532:java/lang/Exception  ], 515:[532:java/lang/Exception  ], 516:[532:java/lang/Exception  ], 517:[532:java/lang/Exception  ], 521:[532:java/lang/Exception  ], 525:[532:java/lang/Exception  ], 526:[532:java/lang/Exception  ], 528:[532:java/lang/Exception  ], 581:[728:java/lang/Throwable  ], 584:[728:java/lang/Throwable  ], 586:[728:java/lang/Throwable  ], 587:[728:java/lang/Throwable  ], 589:[728:java/lang/Throwable  ], 590:[728:java/lang/Throwable  ], 591:[728:java/lang/Throwable  ], 592:[728:java/lang/Throwable  ], 599:[728:java/lang/Throwable  ], 600:[728:java/lang/Throwable  ], 603:[728:java/lang/Throwable  ], 606:[728:java/lang/Throwable  ], 610:[728:java/lang/Throwable  ], 614:[728:java/lang/Throwable  ], 618:[728:java/lang/Throwable  ], 622:[728:java/lang/Throwable  ], 624:[728:java/lang/Throwable  ], 627:[728:java/lang/Throwable  ], 631:[728:java/lang/Throwable  ], 633:[728:java/lang/Throwable  ], 635:[728:java/lang/Throwable  ], 637:[728:java/lang/Throwable  ], 640:[728:java/lang/Throwable  ], 642:[728:java/lang/Throwable  ], 647:[728:java/lang/Throwable  ], 648:[728:java/lang/Throwable  ], 650:[728:java/lang/Throwable  ], 651:[728:java/lang/Throwable  ], 652:[728:java/lang/Throwable  ], 655:[728:java/lang/Throwable  ], 661:[728:java/lang/Throwable  ], 662:[728:java/lang/Throwable  ], 664:[728:java/lang/Throwable  ], 665:[728:java/lang/Throwable  ], 666:[728:java/lang/Throwable  ], 667:[728:java/lang/Throwable  ], 669:[728:java/lang/Throwable  ], 671:[728:java/lang/Throwable  ], 673:[728:java/lang/Throwable  ], 675:[728:java/lang/Throwable  ], 681:[728:java/lang/Throwable  ], 682:[728:java/lang/Throwable  ], 683:[728:java/lang/Throwable  ], 685:[728:java/lang/Throwable  ], 689:[728:java/lang/Throwable  ], 691:[728:java/lang/Throwable  ], 693:[728:java/lang/Throwable  ], 696:[728:java/lang/Throwable  ], 700:[728:java/lang/Throwable  ], 703:[728:java/lang/Throwable  ], 705:[728:java/lang/Throwable  ], 708:[728:java/lang/Throwable  ], 711:[728:java/lang/Throwable  ], 713:[728:java/lang/Throwable  ], 714:[728:java/lang/Throwable  ], 717:[728:java/lang/Throwable  ], 722:[728:java/lang/Throwable  ], 723:[728:java/lang/Throwable  ], 726:[728:java/lang/Throwable  ], 730:[728:java/lang/Throwable  ]]
 INVOKESTATIC local/lito381/server/CombatWorldMultiMagic.create()Llocal/lito381/server/CombatWorldMultiMagic$Batch;
 ASTORE 3
 CONST null
```

### `useClassicWorldCounterDetectionSpell([BLlocal/lito381/server/BattleSpellCatalog$CounterDetectionSpell;)V`

- 類別：`local/lito381/server/ClientSession`
- 類型：例外表涵蓋、含迴圈（路徑比較器不支援）；事件後繼：`LAYOUT_ONLY`
- 比較器：`method useClassicWorldCounterDetectionSpell([BLlocal/lito381/server/BattleSpellCatalog$CounterDetectionSpell;)V: left=108:[206:java/lang/Throwable  ] right=110:[210:java/lang/Throwable  ]; paths rejected: normal control-flow cycle; labelled CFGs differ`

```diff
@@ -1,5 +1,5 @@
 META 2 null [java/io/IOException]
-TRY [108:[206:java/lang/Throwable  ], 111:[206:java/lang/Throwable  ], 113:[206:java/lang/Throwable  ], 114:[206:java/lang/Throwable  ], 116:[206:java/lang/Throwable  ], 117:[206:java/lang/Throwable  ], 118:[206:java/lang/Throwable  ], 119:[206:java/lang/Throwable  ], 126:[206:java/lang/Throwable  ], 130:[206:java/lang/Throwable  ], 133:[206:java/lang/Throwable  ], 136:[206:java/lang/Throwable  ], 137:[206:java/lang/Throwable  ], 140:[206:java/lang/Throwable  ], 144:[206:java/lang/Throwable  ], 148:[206:java/lang/Throwable  ], 152:[206:java/lang/Throwable  ], 156:[206:java/lang/Throwable  ], 159:[206:java/lang/Throwable  ], 161:[206:java/lang/Throwable  ], 163:[206:java/lang/Throwable  ], 166:[206:java/lang/Throwable  ], 171:[206:java/lang/Throwable  ], 172:[206:java/lang/Throwable  ], 173:[206:java/lang/Throwable  ], 175:[206:java/lang/Throwable  ], 180:[206:java/lang/Throwable  ], 183:[206:java/lang/Throwable  ], 184:[206:java/lang/Throwable  ], 186:[206:java/lang/Throwable  ], 189:[206:java/lang/Throwable  ], 191:[206:java/lang/Throwable  ], 192:[206:java/lang/Throwable  ], 195:[206:java/lang/Throwable  ], 200:[206:java/lang/Throwable  ], 201:[206:java/lang/Throwable  ], 204:[206:java/lang/Throwable  ], 208:[206:java/lang/Throwable  ], 272:[412:java/lang/Throwable  ], 273:[412:java/lang/Throwable  ], 276:[412:java/lang/Throwable  ], 280:[412:java/lang/Throwable  ], 284:[412:java/lang/Throwable  ], 287:[412:java/lang/Throwable  ], 289:[412:java/lang/Throwable  ], 292:[412:java/lang/Throwable  ], 294:[412:java/lang/Throwable  ], 299:[412:java/lang/Throwable  ], 301:[412:java/lang/Throwable  ], 302:[412:java/lang/Throwable  ], 311:[412:java/lang/Throwable  ], 314:[412:java/lang/Throwable  ], 318:[412:java/lang/Throwable  ], 321:[412:java/lang/Throwable  ], 326:[412:java/lang/Throwable  ], 330:[412:java/lang/Throwable  ], 332:[412:java/lang/Throwable  ], 333:[412:java/lang/Throwable  ], 336:[412:java/lang/Throwable  ], 338:[412:java/lang/Throwable  ], 340:[412:java/lang/Throwable  ], 341:[412:java/lang/Throwable  ], 344:[412:java/lang/Throwable  ], 347:[412:java/lang/Throwable  ], 348:[412:java/lang/Throwable  ], 352:[412:java/lang/Throwable  ], 354:[412:java/lang/Throwable  ], 358:[412:java/lang/Throwable  ], 359:[412:java/lang/Throwable  ], 362:[412:java/lang/Throwable  ], 364:[412:java/lang/Throwable  ], 366:[412:java/lang/Throwable  ], 367:[412:java/lang/Throwable  ], 370:[412:java/lang/Throwable  ], 373:[412:java/lang/Throwable  ], 378:[412:java/lang/Throwable  ], 379:[412:java/lang/Throwable  ], 380:[412:java/lang/Throwable  ], 385:[412:java/lang/Throwable  ], 386:[412:java/lang/Throwable  ], 387:[412:java/lang/Throwable  ], 392:[412:java/lang/Throwable  ], 397:[412:java/lang/Throwable  ], 398:[412:java/lang/Throwable  ], 401:[412:java/lang/Throwable  ], 404:[412:java/lang/Throwable  ], 406:[412:java/lang/Throwable  ], 407:[412:java/lang/Throwable  ], 410:[412:java/lang/Throwable  ], 414:[412:java/lang/Throwable  ]]
+TRY [110:[210:java/lang/Throwable  ], 113:[210:java/lang/Throwable  ], 115:[210:java/lang/Throwable  ], 116:[210:java/lang/Throwable  ], 118:[210:java/lang/Throwable  ], 119:[210:java/lang/Throwable  ], 120:[210:java/lang/Throwable  ], 121:[210:java/lang/Throwable  ], 128:[210:java/lang/Throwable  ], 132:[210:java/lang/Throwable  ], 135:[210:java/lang/Throwable  ], 138:[210:java/lang/Throwable  ], 139:[210:java/lang/Throwable  ], 142:[210:java/lang/Throwable  ], 146:[210:java/lang/Throwable  ], 150:[210:java/lang/Throwable  ], 154:[210:java/lang/Throwable  ], 158:[210:java/lang/Throwable  ], 161:[210:java/lang/Throwable  ], 163:[210:java/lang/Throwable  ], 165:[210:java/lang/Throwable  ], 168:[210:java/lang/Throwable  ], 173:[210:java/lang/Throwable  ], 174:[210:java/lang/Throwable  ], 175:[210:java/lang/Throwable  ], 177:[210:java/lang/Throwable  ], 181:[210:java/lang/Throwable  ], 185:[210:java/lang/Throwable  ], 188:[210:java/lang/Throwable  ], 190:[210:java/lang/Throwable  ], 193:[210:java/lang/Throwable  ], 195:[210:java/lang/Throwable  ], 196:[210:java/lang/Throwable  ], 199:[210:java/lang/Throwable  ], 204:[210:java/lang/Throwable  ], 205:[210:java/lang/Throwable  ], 208:[210:java/lang/Throwable  ], 212:[210:java/lang/Throwable  ], 276:[416:java/lang/Throwable  ], 277:[416:java/lang/Throwable  ], 280:[416:java/lang/Throwable  ], 284:[416:java/lang/Throwable  ], 288:[416:java/lang/Throwable  ], 291:[416:java/lang/Throwable  ], 293:[416:java/lang/Throwable  ], 296:[416:java/lang/Throwable  ], 298:[416:java/lang/Throwable  ], 303:[416:java/lang/Throwable  ], 305:[416:java/lang/Throwable  ], 306:[416:java/lang/Throwable  ], 315:[416:java/lang/Throwable  ], 318:[416:java/lang/Throwable  ], 322:[416:java/lang/Throwable  ], 325:[416:java/lang/Throwable  ], 330:[416:java/lang/Throwable  ], 334:[416:java/lang/Throwable  ], 336:[416:java/lang/Throwable  ], 337:[416:java/lang/Throwable  ], 340:[416:java/lang/Throwable  ], 342:[416:java/lang/Throwable  ], 344:[416:java/lang/Throwable  ], 345:[416:java/lang/Throwable  ], 348:[416:java/lang/Throwable  ], 351:[416:java/lang/Throwable  ], 352:[416:java/lang/Throwable  ], 356:[416:java/lang/Throwable  ], 358:[416:java/lang/Throwable  ], 362:[416:java/lang/Throwable  ], 363:[416:java/lang/Throwable  ], 366:[416:java/lang/Throwable  ], 368:[416:java/lang/Throwable  ], 370:[416:java/lang/Throwable  ], 371:[416:java/lang/Throwable  ], 374:[416:java/lang/Throwable  ], 377:[416:java/lang/Throwable  ], 382:[416:java/lang/Throwable  ], 383:[416:java/lang/Throwable  ], 384:[416:java/lang/Throwable  ], 389:[416:java/lang/Throwable  ], 390:[416:java/lang/Throwable  ], 391:[416:java/lang/Throwable  ], 396:[416:java/lang/Throwable  ], 401:[416:java/lang/Throwable  ], 402:[416:java/lang/Throwable  ], 405:[416:java/lang/Throwable  ], 408:[416:java/lang/Throwable  ], 410:[416:java/lang/Throwable  ], 411:[416:java/lang/Throwable  ], 414:[416:java/lang/Throwable  ], 418:[416:java/lang/Throwable  ]]
 CONST null
 ASTORE 3
 ALOAD 1
```

### `useClassicWorldHealingSpell([BLlocal/lito381/server/BattleSpellCatalog$HealingSpell;I)V`

- 類別：`local/lito381/server/ClientSession`
- 類型：例外表涵蓋、含迴圈（路徑比較器不支援）；事件後繼：`LAYOUT_ONLY`
- 比較器：`method useClassicWorldHealingSpell([BLlocal/lito381/server/BattleSpellCatalog$HealingSpell;I)V: left=334:[339:java/lang/Exception  ] right=336:[341:java/lang/Exception  ]; paths rejected: normal control-flow cycle; labelled CFGs differ`

```diff
@@ -1,5 +1,5 @@
 META 2 null [java/io/IOException]
-TRY [334:[339:java/lang/Exception  ], 336:[339:java/lang/Exception  ], 475:[809:java/lang/Throwable  ], 476:[798:java/lang/Throwable  , 809:java/lang/Throwable  ], 479:[798:java/lang/Throwable  , 809:java/lang/Throwable  ], 481:[798:java/lang/Throwable  , 809:java/lang/Throwable  ], 482:[798:java/lang/Throwable  , 809:java/lang/Throwable  ], 484:[798:java/lang/Throwable  , 809:java/lang/Throwable  ], 485:[798:java/lang/Throwable  , 809:java/lang/Throwable  ], 486:[798:java/lang/Throwable  , 809:java/lang/Throwable  ], 487:[798:java/lang/Throwable  , 809:java/lang/Throwable  ], 497:[798:java/lang/Throwable  , 809:java/lang/Throwable  ], 498:[798:java/lang/Throwable  , 809:java/lang/Throwable  ], 505:[798:java/lang/Throwable  , 809:java/lang/Throwable  ], 512:[798:java/lang/Throwable  , 809:java/lang/Throwable  ], 516:[798:java/lang/Throwable  , 809:java/lang/Throwable  ], 517:[798:java/lang/Throwable  , 809:java/lang/Throwable  ], 526:[798:java/lang/Throwable  , 809:java/lang/Throwable  ], 529:[798:java/lang/Throwable  , 809:java/lang/Throwable  ], 532:[798:java/lang/Throwable  , 809:java/lang/Throwable  ], 536:[798:java/lang/Throwable  , 809:java/lang/Throwable  ], 546:[798:java/lang/Throwable  , 809:java/lang/Throwable  ], 549:[798:java/lang/Throwable  , 809:java/lang/Throwable  ], 552:[798:java/lang/Throwable  , 809:java/lang/Throwable  ], 556:[798:java/lang/Throwable  , 809:java/lang/Throwable  ], 560:[798:java/lang/Throwable  , 809:java/lang/Throwable  ], 568:[798:java/lang/Throwable  , 809:java/lang/Throwable  ], 569:[798:java/lang/Throwable  , 809:java/lang/Throwable  ], 572:[798:java/lang/Throwable  , 809:java/lang/Throwable  ], 575:[798:java/lang/Throwable  , 809:java/lang/Throwable  ], 581:[798:java/lang/Throwable  , 809:java/lang/Throwable  ], 584:[798:java/lang/Throwable  , 809:java/lang/Throwable  ], 588:[798:java/lang/Throwable  , 809:java/lang/Throwable  ], 592:[798:java/lang/Throwable  , 809:java/lang/Throwable  ], 596:[798:java/lang/Throwable  , 809:java/lang/Throwable  ], 600:[798:java/lang/Throwable  , 809:java/lang/Throwable  ], 602:[798:java/lang/Throwable  , 809:java/lang/Throwable  ], 604:[798:java/lang/Throwable  , 809:java/lang/Throwable  ], 605:[798:java/lang/Throwable  , 809:java/lang/Throwable  ], 612:[798:java/lang/Throwable  , 809:java/lang/Throwable  ], 613:[798:java/lang/Throwable  , 809:java/lang/Throwable  ], 616:[798:java/lang/Throwable  , 809:java/lang/Throwable  ], 619:[798:java/lang/Throwable  , 809:java/lang/Throwable  ], 622:[798:java/lang/Throwable  , 809:java/lang/Throwable  ], 626:[798:java/lang/Throwable  , 809:java/lang/Throwable  ], 630:[798:java/lang/Throwable  , 809:java/lang/Throwable  ], 634:[798:java/lang/Throwable  , 809:java/lang/Throwable  ], 641:[798:java/lang/Throwable  , 809:java/lang/Throwable  ], 648:[798:java/lang/Throwable  , 809:java/lang/Throwable  ], 663:[798:java/lang/Throwable  , 809:java/lang/Throwable  ], 677:[798:java/lang/Throwable  , 809:java/lang/Throwable  ], 681:[798:java/lang/Throwable  , 809:java/lang/Throwable  ], 683:[798:java/lang/Throwable  , 809:java/lang/Throwable  ], 690:[798:java/lang/Throwable  , 809:java/lang/Throwable  ], 696:[798:java/lang/Throwable  , 809:java/lang/Throwable  ], 698:[798:java/lang/Throwable  , 809:java/lang/Throwable  ], 701:[798:java/lang/Throwable  , 809:java/lang/Throwable  ], 703:[798:java/lang/Throwable  , 809:java/lang/Throwable  ], 709:[798:java/lang/Throwable  , 809:java/lang/Throwable  ], 711:[798:java/lang/Throwable  , 809:java/lang/Throwable  ], 713:[798:java/lang/Throwable  , 809:java/lang/Throwable  ], 715:[798:java/lang/Throwable  , 809:java/lang/Throwable  ], 716:[798:java/lang/Throwable  , 809:java/lang/Throwable  ], 721:[798:java/lang/Throwable  , 809:java/lang/Throwable  ], 724:[798:java/lang/Throwable  , 809:java/lang/Throwable  ], 725:[798:java/lang/Throwable  , 809:java/lang/Throwable  ], 727:[798:java/lang/Throwable  , 809:java/lang/Throwable  ], 730:[798:java/lang/Throwable  , 809:java/lang/Throwable  ], 732:[798:java/lang/Throwable  , 809:java/lang/Throwable  ], 733:[798:java/lang/Throwable  , 809:java/lang/Throwable  ], 736:[798:java/lang/Throwable  , 809:java/lang/Throwable  ], 741:[798:java/lang/Throwable  , 809:java/lang/Throwable  ], 742:[798:java/lang/Throwable  , 809:java/lang/Throwable  ], 747:[798:java/lang/Throwable  , 809:java/lang/Throwable  ], 748:[798:java/lang/Throwable  , 809:java/lang/Throwable  ], 754:[798:java/lang/Throwable  , 809:java/lang/Throwable  ], 756:[798:java/lang/Throwable  , 809:java/lang/Throwable  ], 758:[798:java/lang/Throwable  , 809:java/lang/Throwable  ], 759:[798:java/lang/Throwable  , 809:java/lang/Throwable  ], 760:[798:java/lang/Throwable  , 809:java/lang/Throwable  ], 762:[798:java/lang/Throwable  , 809:java/lang/Throwable  ], 765:[798:java/lang/Throwable  , 809:java/lang/Throwable  ], 768:[798:java/lang/Throwable  , 809:java/lang/Throwable  ], 774:[798:java/lang/Throwable  , 809:java/lang/Throwable  ], 775:[798:java/lang/Throwable  , 809:java/lang/Throwable  ], 777:[798:java/lang/Throwable  , 809:java/lang/Throwable  ], 778:[798:java/lang/Throwable  , 809:java/lang/Throwable  ], 780:[798:java/lang/Throwable  , 809:java/lang/Throwable  ], 782:[798:java/lang/Throwable  , 809:java/lang/Throwable  ], 784:[798:java/lang/Throwable  , 809:java/lang/Throwable  ], 792:[798:java/lang/Throwable  , 809:java/lang/Throwable  ], 793:[798:java/lang/Throwable  , 809:java/lang/Throwable  ], 794:[798:java/lang/Throwable  , 809:java/lang/Throwable  ], 796:[798:java/lang/Throwable  , 809:java/lang/Throwable  ], 800:[798:java/lang/Throwable  , 809:java/lang/Throwable  ], 802:[809:java/lang/Throwable  ], 804:[809:java/lang/Throwable  ], 807:[809:java/lang/Throwable  ], 811:[809:java/lang/Throwable  ]]
+TRY [336:[341:java/lang/Exception  ], 338:[341:java/lang/Exception  ], 477:[812:java/lang/Throwable  ], 478:[804:java/lang/Throwable  , 812:java/lang/Throwable  ], 481:[804:java/lang/Throwable  , 812:java/lang/Throwable  ], 483:[804:java/lang/Throwable  , 812:java/lang/Throwable  ], 484:[804:java/lang/Throwable  , 812:java/lang/Throwable  ], 486:[804:java/lang/Throwable  , 812:java/lang/Throwable  ], 487:[804:java/lang/Throwable  , 812:java/lang/Throwable  ], 488:[804:java/lang/Throwable  , 812:java/lang/Throwable  ], 489:[804:java/lang/Throwable  , 812:java/lang/Throwable  ], 499:[804:java/lang/Throwable  , 812:java/lang/Throwable  ], 500:[804:java/lang/Throwable  , 812:java/lang/Throwable  ], 507:[804:java/lang/Throwable  , 812:java/lang/Throwable  ], 514:[804:java/lang/Throwable  , 812:java/lang/Throwable  ], 518:[804:java/lang/Throwable  , 812:java/lang/Throwable  ], 519:[804:java/lang/Throwable  , 812:java/lang/Throwable  ], 524:[804:java/lang/Throwable  , 812:java/lang/Throwable  ], 525:[804:java/lang/Throwable  , 812:java/lang/Throwable  ], 528:[804:java/lang/Throwable  , 812:java/lang/Throwable  ], 531:[804:java/lang/Throwable  , 812:java/lang/Throwable  ], 537:[804:java/lang/Throwable  , 812:java/lang/Throwable  ], 540:[804:java/lang/Throwable  , 812:java/lang/Throwable  ], 544:[804:java/lang/Throwable  , 812:java/lang/Throwable  ], 548:[804:java/lang/Throwable  , 812:java/lang/Throwable  ], 552:[804:java/lang/Throwable  , 812:java/lang/Throwable  ], 556:[804:java/lang/Throwable  , 812:java/lang/Throwable  ], 558:[804:java/lang/Throwable  , 812:java/lang/Throwable  ], 560:[804:java/lang/Throwable  , 812:java/lang/Throwable  ], 561:[804:java/lang/Throwable  , 812:java/lang/Throwable  ], 572:[804:java/lang/Throwable  , 812:java/lang/Throwable  ], 575:[804:java/lang/Throwable  , 812:java/lang/Throwable  ], 578:[804:java/lang/Throwable  , 812:java/lang/Throwable  ], 582:[804:java/lang/Throwable  , 812:java/lang/Throwable  ], 592:[804:java/lang/Throwable  , 812:java/lang/Throwable  ], 595:[804:java/lang/Throwable  , 812:java/lang/Throwable  ], 598:[804:java/lang/Throwable  , 812:java/lang/Throwable  ], 602:[804:java/lang/Throwable  , 812:java/lang/Throwable  ], 606:[804:java/lang/Throwable  , 812:java/lang/Throwable  ], 614:[804:java/lang/Throwable  , 812:java/lang/Throwable  ], 615:[804:java/lang/Throwable  , 812:java/lang/Throwable  ], 618:[804:java/lang/Throwable  , 812:java/lang/Throwable  ], 621:[804:java/lang/Throwable  , 812:java/lang/Throwable  ], 624:[804:java/lang/Throwable  , 812:java/lang/Throwable  ], 628:[804:java/lang/Throwable  , 812:java/lang/Throwable  ], 632:[804:java/lang/Throwable  , 812:java/lang/Throwable  ], 636:[804:java/lang/Throwable  , 812:java/lang/Throwable  ], 643:[804:java/lang/Throwable  , 812:java/lang/Throwable  ], 650:[804:java/lang/Throwable  , 812:java/lang/Throwable  ], 665:[804:java/lang/Throwable  , 812:java/lang/Throwable  ], 669:[804:java/lang/Throwable  , 812:java/lang/Throwable  ], 675:[804:java/lang/Throwable  , 812:java/lang/Throwable  ], 676:[804:java/lang/Throwable  , 812:java/lang/Throwable  ], 678:[804:java/lang/Throwable  , 812:java/lang/Throwable  ], 679:[804:java/lang/Throwable  , 812:java/lang/Throwable  ], 681:[804:java/lang/Throwable  , 812:java/lang/Throwable  ], 683:[804:java/lang/Throwable  , 812:java/lang/Throwable  ], 685:[804:java/lang/Throwable  , 812:java/lang/Throwable  ], 693:[804:java/lang/Throwable  , 812:java/lang/Throwable  ], 694:[804:java/lang/Throwable  , 812:java/lang/Throwable  ], 695:[804:java/lang/Throwable  , 812:java/lang/Throwable  ], 697:[804:java/lang/Throwable  , 812:java/lang/Throwable  ], 699:[812:java/lang/Throwable  ], 712:[804:java/lang/Throwable  , 812:java/lang/Throwable  ], 716:[804:java/lang/Throwable  , 812:java/lang/Throwable  ], 718:[804:java/lang/Throwable  , 812:java/lang/Throwable  ], 725:[804:java/lang/Throwable  , 812:java/lang/Throwable  ], 731:[804:java/lang/Throwable  , 812:java/lang/Throwable  ], 733:[804:java/lang/Throwable  , 812:java/lang/Throwable  ], 736:[804:java/lang/Throwable  , 812:java/lang/Throwable  ], 738:[804:java/lang/Throwable  , 812:java/lang/Throwable  ], 744:[804:java/lang/Throwable  , 812:java/lang/Throwable  ], 746:[804:java/lang/Throwable  , 812:java/lang/Throwable  ], 748:[804:java/lang/Throwable  , 812:java/lang/Throwable  ], 750:[804:java/lang/Throwable  , 812:java/lang/Throwable  ], 751:[804:java/lang/Throwable  , 812:java/lang/Throwable  ], 755:[804:java/lang/Throwable  , 812:java/lang/Throwable  ], 759:[804:java/lang/Throwable  , 812:java/lang/Throwable  ], 762:[804:java/lang/Throwable  , 812:java/lang/Throwable  ], 764:[804:java/lang/Throwable  , 812:java/lang/Throwable  ], 767:[804:java/lang/Throwable  , 812:java/lang/Throwable  ], 769:[804:java/lang/Throwable  , 812:java/lang/Throwable  ], 770:[804:java/lang/Throwable  , 812:java/lang/Throwable  ], 773:[804:java/lang/Throwable  , 812:java/lang/Throwable  ], 778:[804:java/lang/Throwable  , 812:java/lang/Throwable  ], 779:[804:java/lang/Throwable  , 812:java/lang/Throwable  ], 784:[804:java/lang/Throwable  , 812:java/lang/Throwable  ], 785:[804:java/lang/Throwable  , 812:java/lang/Throwable  ], 791:[804:java/lang/Throwable  , 812:java/lang/Throwable  ], 793:[804:java/lang/Throwable  , 812:java/lang/Throwable  ], 795:[804:java/lang/Throwable  , 812:java/lang/Throwable  ], 796:[804:java/lang/Throwable  , 812:java/lang/Throwable  ], 797:[804:java/lang/Throwable  , 812:java/lang/Throwable  ], 799:[804:java/lang/Throwable  , 812:java/lang/Throwable  ], 802:[804:java/lang/Throwable  , 812:java/lang/Throwable  ], 806:[804:java/lang/Throwable  , 812:java/lang/Throwable  ], 808:[812:java/lang/Throwable  ], 810:[812:java/lang/Throwable  ], 814:[812:java/lang/Throwable  ]]
 ILOAD 3
 IFLT L6
 ILOAD 3
```

### `useClassicWorldLineAttackSpell([BLlocal/lito381/server/BattleSpellCatalog$LineAttackSpell;)V`

- 類別：`local/lito381/server/ClientSession`
- 類型：路徑事件多重集合不同（長度相同）；事件後繼：`LAYOUT_ONLY`
- 比較器：`method useClassicWorldLineAttackSpell([BLlocal/lito381/server/BattleSpellCatalog$LineAttackSpell;)V insn[5]: left=INVOKEDYNAMIC run(Llocal/lito381/server/ClientSession;[BLlocal/lito381/server/BattleSpellCatalog$LineAttackSpell;)Llocal/lito381/server/NativeSelfCastAction$Task; bsm=H6 java/lang/invoke/LambdaMetafactory.metafactory(Ljava/lang/invoke/Metho… right=INVOKEDYNAMIC run(Llocal/lito381/serve`

```diff
@@ -5,6 +5,6 @@
 ALOAD 0
 ALOAD 1
 ALOAD 2
-INVOKEDYNAMIC run(Llocal/lito381/server/ClientSession;[BLlocal/lito381/server/BattleSpellCatalog$LineAttackSpell;)Llocal/lito381/server/NativeSelfCastAction$Task; bsm=H6 java/lang/invoke/LambdaMetafactory.metafactory(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodHandle;Ljava/lang/invoke/MethodType;)Ljava/lang/invoke/CallSite; args=[type:()V, handle:H7 local/lito381/server/ClientSession.lambda$content$dfb3791738c5b4d0ec1d5ae51a7b2311d8001284ea4625b6d859dc694635b012([BLlocal/lito381/server/BattleSpellCatalog$LineAttackSpell;)V, type:()V]
+INVOKEDYNAMIC run(Llocal/lito381/server/ClientSession;[BLlocal/lito381/server/BattleSpellCatalog$LineAttackSpell;)Llocal/lito381/server/NativeSelfCastAction$Task; bsm=H6 java/lang/invoke/LambdaMetafactory.metafactory(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodHandle;Ljava/lang/invoke/MethodType;)Ljava/lang/invoke/CallSite; args=[type:()V, handle:H7 local/lito381/server/ClientSession.nativeCompanionTargetuseClassicWorldLineAttackSpell([BLlocal/lito381/server/BattleSpellCatalog$LineAttackSpell;)V, type:()V]
 INVOKESTATIC local/lito381/server/NativeCompanionTargets.cast(Llocal/lito381/server/ClientSession;ILlocal/lito381/server/NativeSelfCastAction$Task;)V
 RETURN
```

### `useClassicWorldLineFreezeAttackSpell(Llocal/lito381/server/Protocol$AttackSpellRequest;Llocal/lito381/server/BattleSpellCatalog$FreezeAttackSpell;)V`

- 類別：`local/lito381/server/ClientSession`
- 類型：例外表涵蓋、含迴圈（路徑比較器不支援）；事件後繼：`LAYOUT_ONLY`
- 比較器：`method useClassicWorldLineFreezeAttackSpell(Llocal/lito381/server/Protocol$AttackSpellRequest;Llocal/lito381/server/BattleSpellCatalog$FreezeAttackSpell;)V: left=188:[372:java/lang/Exception  ] right=192:[380:java/lang/Exception  ]; paths rejected: normal control-flow cycle; labelled CFGs differ`

```diff
@@ -1,9 +1,10 @@
 META 2 null [java/io/IOException]
-TRY [188:[372:java/lang/Exception  ], 191:[372:java/lang/Exception  ], 194:[372:java/lang/Exception  ], 197:[372:java/lang/Exception  ], 198:[372:java/lang/Exception  ], 204:[372:java/lang/Exception  ], 205:[372:java/lang/Exception  ], 208:[372:java/lang/Exception  ], 212:[372:java/lang/Exception  ], 216:[372:java/lang/Exception  ], 217:[372:java/lang/Exception  ], 220:[372:java/lang/Exception  ], 223:[372:java/lang/Exception  ], 224:[372:java/lang/Exception  ], 227:[372:java/lang/Exception  ], 228:[372:java/lang/Exception  ], 231:[372:java/lang/Exception  ], 234:[372:java/lang/Exception  ], 238:[372:java/lang/Exception  ], 240:[372:java/lang/Exception  ], 241:[372:java/lang/Exception  ], 245:[372:java/lang/Exception  ], 247:[372:java/lang/Exception  ], 248:[372:java/lang/Exception  ], 252:[372:java/lang/Exception  ], 253:[372:java/lang/Exception  ], 259:[372:java/lang/Exception  ], 262:[372:java/lang/Exception  ], 264:[372:java/lang/Exception  ], 265:[372:java/lang/Exception  ], 269:[372:java/lang/Exception  ], 271:[372:java/lang/Exception  ], 272:[372:java/lang/Exception  ], 273:[372:java/lang/Exception  ], 276:[372:java/lang/Exception  ], 282:[372:java/lang/Exception  ], 283:[372:java/lang/Exception  ], 286:[372:java/lang/Exception  ], 288:[372:java/lang/Exception  ], 296:[372:java/lang/Exception  ], 301:[372:java/lang/Exception  ], 302:[372:java/lang/Exception  ], 305:[372:java/lang/Exception  ], 308:[372:java/lang/Exception  ], 309:[372:java/lang/Exception  ], 312:[372:java/lang/Exception  ], 315:[372:java/lang/Exception  ], 318:[372:java/lang/Exception  ], 322:[372:java/lang/Exception  ], 326:[372:java/lang/Exception  ], 328:[372:java/lang/Exception  ], 332:[372:java/lang/Exception  ], 334:[372:java/lang/Exception  ], 335:[372:java/lang/Exception  ], 338:[372:java/lang/Exception  ], 340:[372:java/lang/Exception  ], 341:[372:java/lang/Exception  ], 345:[372:java/lang/Exception  ], 347:[372:java/lang/Exception  ], 348:[372:java/lang/Exception  ], 349:[372:java/lang/Exception  ], 353:[372:java/lang/Exception  ], 357:[372:java/lang/Exception  ], 358:[372:java/lang/Exception  ], 360:[372:java/lang/Exception  ], 363:[372:java/lang/Exception  ], 365:[372:java/lang/Exception  ], 462:[581:java/lang/Throwable  ], 465:[581:java/lang/Throwable  ], 467:[581:java/lang/Throwable  ], 468:[581:java/lang/Throwable  ], 470:[581:java/lang/Throwable  ], 471:[581:java/lang/Throwable  ], 472:[581:java/lang/Throwable  ], 473:[581:java/lang/Throwable  ], 480:[581:java/lang/Throwable  ], 481:[581:java/lang/Throwable  ], 484:[581:java/lang/Throwable  ], 487:[581:java/lang/Throwable  ], 491:[581:java/lang/Throwable  ], 495:[581:java/lang/Throwable  ], 499:[581:java/lang/Throwable  ], 503:[581:java/lang/Throwable  ], 505:[581:java/lang/Throwable  ], 508:[581:java/lang/Throwable  ], 512:[581:java/lang/Throwable  ], 514:[581:java/lang/Throwable  ], 519:[581:java/lang/Throwable  ], 520:[581:java/lang/Throwable  ], 522:[581:java/lang/Throwable  ], 523:[581:java/lang/Throwable  ], 524:[581:java/lang/Throwable  ], 527:[581:java/lang/Throwable  ], 533:[581:java/lang/Throwable  ], 534:[581:java/lang/Throwable  ], 536:[581:java/lang/Throwable  ], 537:[581:java/lang/Throwable  ], 538:[581:java/lang/Throwable  ], 540:[581:java/lang/Throwable  ], 544:[581:java/lang/Throwable  ], 546:[581:java/lang/Throwable  ], 548:[581:java/lang/Throwable  ], 552:[581:java/lang/Throwable  ], 555:[581:java/lang/Throwable  ], 556:[581:java/lang/Throwable  ], 558:[581:java/lang/Throwable  ], 561:[581:java/lang/Throwable  ], 564:[581:java/lang/Throwable  ], 566:[581:java/lang/Throwable  ], 567:[581:java/lang/Throwable  ], 570:[581:java/lang/Throwable  ], 575:[581:java/lang/Throwable  ], 576:[581:java/lang/Throwable  ], 579:[581:java/lang/Throwable  ], 583:[581:java/lang/Throwable  ]]
+TRY [192:[380:java/lang/Exception  ], 195:[380:java/lang/Exception  ], 198:[380:java/lang/Exception  ], 201:[380:java/lang/Exception  ], 202:[380:java/lang/Exception  ], 208:[380:java/lang/Exception  ], 209:[380:java/lang/Exception  ], 212:[380:java/lang/Exception  ], 216:[380:java/lang/Exception  ], 221:[380:java/lang/Exception  ], 222:[380:java/lang/Exception  ], 225:[380:java/lang/Exception  ], 228:[380:java/lang/Exception  ], 229:[380:java/lang/Exception  ], 232:[380:java/lang/Exception  ], 233:[380:java/lang/Exception  ], 236:[380:java/lang/Exception  ], 239:[380:java/lang/Exception  ], 243:[380:java/lang/Exception  ], 245:[380:java/lang/Exception  ], 246:[380:java/lang/Exception  ], 250:[380:java/lang/Exception  ], 252:[380:java/lang/Exception  ], 253:[380:java/lang/Exception  ], 258:[380:java/lang/Exception  ], 259:[380:java/lang/Exception  ], 265:[380:java/lang/Exception  ], 269:[380:java/lang/Exception  ], 271:[380:java/lang/Exception  ], 272:[380:java/lang/Exception  ], 276:[380:java/lang/Exception  ], 278:[380:java/lang/Exception  ], 279:[380:java/lang/Exception  ], 280:[380:java/lang/Exception  ], 283:[380:java/lang/Exception  ], 289:[380:java/lang/Exception  ], 290:[380:java/lang/Exception  ], 293:[380:java/lang/Exception  ], 295:[380:java/lang/Exception  ], 303:[380:java/lang/Exception  ], 308:[380:java/lang/Exception  ], 309:[380:java/lang/Exception  ], 312:[380:java/lang/Exception  ], 315:[380:java/lang/Exception  ], 316:[380:java/lang/Exception  ], 319:[380:java/lang/Exception  ], 322:[380:java/lang/Exception  ], 325:[380:java/lang/Exception  ], 329:[380:java/lang/Exception  ], 333:[380:java/lang/Exception  ], 335:[380:java/lang/Exception  ], 339:[380:java/lang/Exception  ], 341:[380:java/lang/Exception  ], 342:[380:java/lang/Exception  ], 346:[380:java/lang/Exception  ], 348:[380:java/lang/Exception  ], 349:[380:java/lang/Exception  ], 353:[380:java/lang/Exception  ], 355:[380:java/lang/Exception  ], 356:[380:java/lang/Exception  ], 357:[380:java/lang/Exception  ], 361:[380:java/lang/Exception  ], 365:[380:java/lang/Exception  ], 366:[380:java/lang/Exception  ], 368:[380:java/lang/Exception  ], 371:[380:java/lang/Exception  ], 373:[380:java/lang/Exception  ], 470:[591:java/lang/Throwable  ], 473:[591:java/lang/Throwable  ], 475:[591:java/lang/Throwable  ], 476:[591:java/lang/Throwable  ], 478:[591:java/lang/Throwable  ], 479:[591:java/lang/Throwable  ], 480:[591:java/lang/Throwable  ], 481:[591:java/lang/Throwable  ], 488:[591:java/lang/Throwable  ], 489:[591:java/lang/Throwable  ], 492:[591:java/lang/Throwable  ], 495:[591:java/lang/Throwable  ], 499:[591:java/lang/Throwable  ], 503:[591:java/lang/Throwable  ], 507:[591:java/lang/Throwable  ], 511:[591:java/lang/Throwable  ], 513:[591:java/lang/Throwable  ], 516:[591:java/lang/Throwable  ], 520:[591:java/lang/Throwable  ], 522:[591:java/lang/Throwable  ], 527:[591:java/lang/Throwable  ], 528:[591:java/lang/Throwable  ], 530:[591:java/lang/Throwable  ], 531:[591:java/lang/Throwable  ], 532:[591:java/lang/Throwable  ], 535:[591:java/lang/Throwable  ], 541:[591:java/lang/Throwable  ], 542:[591:java/lang/Throwable  ], 544:[591:java/lang/Throwable  ], 545:[591:java/lang/Throwable  ], 546:[591:java/lang/Throwable  ], 548:[591:java/lang/Throwable  ], 552:[591:java/lang/Throwable  ], 554:[591:java/lang/Throwable  ], 556:[591:java/lang/Throwable  ], 559:[591:java/lang/Throwable  ], 563:[591:java/lang/Throwable  ], 566:[591:java/lang/Throwable  ], 568:[591:java/lang/Throwable  ], 571:[591:java/lang/Throwable  ], 574:[591:java/lang/Throwable  ], 576:[591:java/lang/Throwable  ], 577:[591:java/lang/Throwable  ], 580:[591:java/lang/Throwable  ], 585:[591:java/lang/Throwable  ], 586:[591:java/lang/Throwable  ], 589:[591:java/lang/Throwable  ], 593:[591:java/lang/Throwable  ]]
 ALOAD 0
 ALOAD 1
 INVOKESTATIC local/lito381/server/NativeSelectedPlayerIntent.attack(Llocal/lito381/server/ClientSession;Llocal/lito381/server/Protocol$AttackSpellRequest;)Z
-IFEQ L1714
+IFNE L5
+RETURN
 INVOKESTATIC local/lito381/server/CombatWorldFreeze.create()Llocal/lito381/server/CombatWorldFreeze$Batch;
 ASTORE 3
 CONST null
```

### `useClassicWorldManaDrainSpell([BLlocal/lito381/server/BattleSpellCatalog$ManaDrainSpell;)V`

- 類別：`local/lito381/server/ClientSession`
- 類型：例外表涵蓋、含迴圈（路徑比較器不支援）；事件後繼：`LAYOUT_ONLY`
- 比較器：`method useClassicWorldManaDrainSpell([BLlocal/lito381/server/BattleSpellCatalog$ManaDrainSpell;)V: left=276:[281:java/lang/Exception  ] right=278:[283:java/lang/Exception  ]; paths rejected: normal control-flow cycle; labelled CFGs differ`

```diff
@@ -1,5 +1,5 @@
 META 2 null [java/io/IOException]
-TRY [276:[281:java/lang/Exception  ], 278:[281:java/lang/Exception  ], 460:[614:java/lang/Throwable  ], 463:[614:java/lang/Throwable  ], 465:[614:java/lang/Throwable  ], 466:[614:java/lang/Throwable  ], 468:[614:java/lang/Throwable  ], 469:[614:java/lang/Throwable  ], 470:[614:java/lang/Throwable  ], 471:[614:java/lang/Throwable  ], 478:[614:java/lang/Throwable  ], 482:[614:java/lang/Throwable  ], 483:[614:java/lang/Throwable  ], 490:[614:java/lang/Throwable  ], 493:[614:java/lang/Throwable  ], 500:[614:java/lang/Throwable  ], 501:[614:java/lang/Throwable  ], 504:[614:java/lang/Throwable  ], 510:[614:java/lang/Throwable  ], 514:[614:java/lang/Throwable  ], 525:[614:java/lang/Throwable  ], 529:[614:java/lang/Throwable  ], 531:[614:java/lang/Throwable  ], 534:[614:java/lang/Throwable  ], 537:[614:java/lang/Throwable  ], 538:[614:java/lang/Throwable  ], 541:[614:java/lang/Throwable  ], 545:[614:java/lang/Throwable  ], 550:[614:java/lang/Throwable  ], 552:[614:java/lang/Throwable  ], 554:[614:java/lang/Throwable  ], 556:[614:java/lang/Throwable  ], 558:[614:java/lang/Throwable  ], 564:[614:java/lang/Throwable  ], 565:[614:java/lang/Throwable  ], 566:[614:java/lang/Throwable  ], 568:[614:java/lang/Throwable  ], 576:[614:java/lang/Throwable  ], 577:[614:java/lang/Throwable  ], 578:[614:java/lang/Throwable  ], 582:[614:java/lang/Throwable  ], 585:[614:java/lang/Throwable  ], 586:[614:java/lang/Throwable  ], 589:[614:java/lang/Throwable  ], 591:[614:java/lang/Throwable  ], 593:[614:java/lang/Throwable  ], 595:[614:java/lang/Throwable  ], 598:[614:java/lang/Throwable  ], 602:[614:java/lang/Throwable  ], 605:[614:java/lang/Throwable  ], 610:[614:java/lang/Throwable  ], 612:[614:java/lang/Throwable  ], 616:[614:java/lang/Throwable  ], 788:[811:java/lang/Throwable  ], 792:[811:java/lang/Throwable  ], 797:[811:java/lang/Throwable  ], 800:[811:java/lang/Throwable  ], 801:[811:java/lang/Throwable  ], 803:[811:java/lang/Throwable  ], 806:[811:java/lang/Throwable  ], 809:[811:java/lang/Throwable  ], 813:[811:java/lang/Throwable  ], 869:[883:java/lang/Throwable  ], 871:[883:java/lang/Throwable  ], 874:[883:java/lang/Throwable  ], 875:[883:java/lang/Throwable  ], 876:[883:java/lang/Throwable  ], 878:[883:java/lang/Throwable  ], 881:[883:java/lang/Throwable  ], 885:[883:java/lang/Throwable  ]]
+TRY [278:[283:java/lang/Exception  ], 280:[283:java/lang/Exception  ], 462:[618:java/lang/Throwable  ], 465:[618:java/lang/Throwable  ], 467:[618:java/lang/Throwable  ], 468:[618:java/lang/Throwable  ], 470:[618:java/lang/Throwable  ], 471:[618:java/lang/Throwable  ], 472:[618:java/lang/Throwable  ], 473:[618:java/lang/Throwable  ], 480:[618:java/lang/Throwable  ], 484:[618:java/lang/Throwable  ], 485:[618:java/lang/Throwable  ], 490:[618:java/lang/Throwable  ], 491:[618:java/lang/Throwable  ], 494:[618:java/lang/Throwable  ], 500:[618:java/lang/Throwable  ], 504:[618:java/lang/Throwable  ], 513:[618:java/lang/Throwable  ], 516:[618:java/lang/Throwable  ], 527:[618:java/lang/Throwable  ], 531:[618:java/lang/Throwable  ], 533:[618:java/lang/Throwable  ], 536:[618:java/lang/Throwable  ], 539:[618:java/lang/Throwable  ], 540:[618:java/lang/Throwable  ], 543:[618:java/lang/Throwable  ], 547:[618:java/lang/Throwable  ], 552:[618:java/lang/Throwable  ], 554:[618:java/lang/Throwable  ], 556:[618:java/lang/Throwable  ], 558:[618:java/lang/Throwable  ], 560:[618:java/lang/Throwable  ], 566:[618:java/lang/Throwable  ], 567:[618:java/lang/Throwable  ], 568:[618:java/lang/Throwable  ], 570:[618:java/lang/Throwable  ], 578:[618:java/lang/Throwable  ], 579:[618:java/lang/Throwable  ], 580:[618:java/lang/Throwable  ], 583:[618:java/lang/Throwable  ], 587:[618:java/lang/Throwable  ], 590:[618:java/lang/Throwable  ], 593:[618:java/lang/Throwable  ], 595:[618:java/lang/Throwable  ], 597:[618:java/lang/Throwable  ], 599:[618:java/lang/Throwable  ], 602:[618:java/lang/Throwable  ], 606:[618:java/lang/Throwable  ], 609:[618:java/lang/Throwable  ], 614:[618:java/lang/Throwable  ], 616:[618:java/lang/Throwable  ], 620:[618:java/lang/Throwable  ], 793:[818:java/lang/Throwable  ], 797:[818:java/lang/Throwable  ], 801:[818:java/lang/Throwable  ], 805:[818:java/lang/Throwable  ], 808:[818:java/lang/Throwable  ], 810:[818:java/lang/Throwable  ], 813:[818:java/lang/Throwable  ], 816:[818:java/lang/Throwable  ], 820:[818:java/lang/Throwable  ], 875:[892:java/lang/Throwable  ], 877:[892:java/lang/Throwable  ], 880:[892:java/lang/Throwable  ], 882:[892:java/lang/Throwable  ], 885:[892:java/lang/Throwable  ], 887:[892:java/lang/Throwable  ], 890:[892:java/lang/Throwable  ], 894:[892:java/lang/Throwable  ]]
 ALOAD 1
 INVOKESTATIC local/lito381/server/Protocol.attackSpellRequest([B)Llocal/lito381/server/Protocol$AttackSpellRequest;
 ALOAD 0
```

### `useClassicWorldTurnUndeadSpell([BLlocal/lito381/server/BattleSpellCatalog$TurnUndeadSpell;)V`

- 類別：`local/lito381/server/ClientSession`
- 類型：例外表涵蓋、含迴圈（路徑比較器不支援）；事件後繼：`LAYOUT_ONLY`
- 比較器：`method useClassicWorldTurnUndeadSpell([BLlocal/lito381/server/BattleSpellCatalog$TurnUndeadSpell;)V: left=233:[238:java/lang/Exception  ] right=235:[240:java/lang/Exception  ]; paths rejected: normal control-flow cycle; labelled CFGs differ`

```diff
@@ -1,5 +1,5 @@
 META 2 null [java/io/IOException]
-TRY [233:[238:java/lang/Exception  ], 235:[238:java/lang/Exception  ], 267:[427:java/lang/Throwable  ], 270:[427:java/lang/Throwable  ], 272:[427:java/lang/Throwable  ], 273:[427:java/lang/Throwable  ], 275:[427:java/lang/Throwable  ], 276:[427:java/lang/Throwable  ], 277:[427:java/lang/Throwable  ], 278:[427:java/lang/Throwable  ], 285:[427:java/lang/Throwable  ], 289:[427:java/lang/Throwable  ], 290:[427:java/lang/Throwable  ], 295:[427:java/lang/Throwable  ], 296:[427:java/lang/Throwable  ], 299:[427:java/lang/Throwable  ], 302:[427:java/lang/Throwable  ], 306:[427:java/lang/Throwable  ], 315:[427:java/lang/Throwable  ], 318:[427:java/lang/Throwable  ], 329:[427:java/lang/Throwable  ], 333:[427:java/lang/Throwable  ], 336:[427:java/lang/Throwable  ], 337:[427:java/lang/Throwable  ], 340:[427:java/lang/Throwable  ], 343:[427:java/lang/Throwable  ], 346:[427:java/lang/Throwable  ], 350:[427:java/lang/Throwable  ], 356:[427:java/lang/Throwable  ], 360:[427:java/lang/Throwable  ], 365:[427:java/lang/Throwable  ], 366:[427:java/lang/Throwable  ], 368:[427:java/lang/Throwable  ], 369:[427:java/lang/Throwable  ], 371:[427:java/lang/Throwable  ], 373:[427:java/lang/Throwable  ], 378:[427:java/lang/Throwable  ], 379:[427:java/lang/Throwable  ], 380:[427:java/lang/Throwable  ], 382:[427:java/lang/Throwable  ], 389:[427:java/lang/Throwable  ], 392:[427:java/lang/Throwable  ], 396:[427:java/lang/Throwable  ], 397:[427:java/lang/Throwable  ], 398:[427:java/lang/Throwable  ], 402:[427:java/lang/Throwable  ], 406:[427:java/lang/Throwable  ], 408:[427:java/lang/Throwable  ], 409:[427:java/lang/Throwable  ], 411:[427:java/lang/Throwable  ], 415:[427:java/lang/Throwable  ], 418:[427:java/lang/Throwable  ], 423:[427:java/lang/Throwable  ], 425:[427:java/lang/Throwable  ], 429:[427:java/lang/Throwable  ]]
+TRY [235:[240:java/lang/Exception  ], 237:[240:java/lang/Exception  ], 269:[431:java/lang/Throwable  ], 272:[431:java/lang/Throwable  ], 274:[431:java/lang/Throwable  ], 275:[431:java/lang/Throwable  ], 277:[431:java/lang/Throwable  ], 278:[431:java/lang/Throwable  ], 279:[431:java/lang/Throwable  ], 280:[431:java/lang/Throwable  ], 287:[431:java/lang/Throwable  ], 291:[431:java/lang/Throwable  ], 292:[431:java/lang/Throwable  ], 297:[431:java/lang/Throwable  ], 298:[431:java/lang/Throwable  ], 301:[431:java/lang/Throwable  ], 304:[431:java/lang/Throwable  ], 308:[431:java/lang/Throwable  ], 317:[431:java/lang/Throwable  ], 320:[431:java/lang/Throwable  ], 331:[431:java/lang/Throwable  ], 335:[431:java/lang/Throwable  ], 338:[431:java/lang/Throwable  ], 339:[431:java/lang/Throwable  ], 342:[431:java/lang/Throwable  ], 345:[431:java/lang/Throwable  ], 348:[431:java/lang/Throwable  ], 352:[431:java/lang/Throwable  ], 358:[431:java/lang/Throwable  ], 362:[431:java/lang/Throwable  ], 367:[431:java/lang/Throwable  ], 368:[431:java/lang/Throwable  ], 370:[431:java/lang/Throwable  ], 371:[431:java/lang/Throwable  ], 373:[431:java/lang/Throwable  ], 375:[431:java/lang/Throwable  ], 380:[431:java/lang/Throwable  ], 381:[431:java/lang/Throwable  ], 382:[431:java/lang/Throwable  ], 384:[431:java/lang/Throwable  ], 391:[431:java/lang/Throwable  ], 394:[431:java/lang/Throwable  ], 398:[431:java/lang/Throwable  ], 399:[431:java/lang/Throwable  ], 400:[431:java/lang/Throwable  ], 403:[431:java/lang/Throwable  ], 407:[431:java/lang/Throwable  ], 410:[431:java/lang/Throwable  ], 413:[431:java/lang/Throwable  ], 415:[431:java/lang/Throwable  ], 419:[431:java/lang/Throwable  ], 422:[431:java/lang/Throwable  ], 427:[431:java/lang/Throwable  ], 429:[431:java/lang/Throwable  ], 433:[431:java/lang/Throwable  ]]
 ALOAD 1
 INVOKESTATIC local/lito381/server/Protocol.attackSpellRequest([B)Llocal/lito381/server/Protocol$AttackSpellRequest;
 ALOAD 0
```

### `useCombatBuffSpell([BLlocal/lito381/server/BattleSpellCatalog$CombatBuffSpell;)V`

- 類別：`local/lito381/server/ClientSession`
- 類型：例外表涵蓋、含迴圈（路徑比較器不支援）；事件後繼：`LAYOUT_ONLY`
- 比較器：`method useCombatBuffSpell([BLlocal/lito381/server/BattleSpellCatalog$CombatBuffSpell;)V: left=216:[408:java/lang/Throwable  ] right=217:[410:java/lang/Throwable  ]; paths rejected: normal control-flow cycle; labelled CFGs differ`

```diff
@@ -1,5 +1,5 @@
 META 2 null [java/io/IOException]
-TRY [216:[408:java/lang/Throwable  ], 218:[397:java/lang/Throwable  , 408:java/lang/Throwable  ], 220:[397:java/lang/Throwable  , 408:java/lang/Throwable  ], 221:[397:java/lang/Throwable  , 408:java/lang/Throwable  ], 223:[397:java/lang/Throwable  , 408:java/lang/Throwable  ], 224:[397:java/lang/Throwable  , 408:java/lang/Throwable  ], 225:[397:java/lang/Throwable  , 408:java/lang/Throwable  ], 226:[397:java/lang/Throwable  , 408:java/lang/Throwable  ], 229:[397:java/lang/Throwable  , 408:java/lang/Throwable  ], 232:[397:java/lang/Throwable  , 408:java/lang/Throwable  ], 233:[397:java/lang/Throwable  , 408:java/lang/Throwable  ], 236:[397:java/lang/Throwable  , 408:java/lang/Throwable  ], 241:[397:java/lang/Throwable  , 408:java/lang/Throwable  ], 242:[397:java/lang/Throwable  , 408:java/lang/Throwable  ], 245:[397:java/lang/Throwable  , 408:java/lang/Throwable  ], 254:[397:java/lang/Throwable  , 408:java/lang/Throwable  ], 257:[397:java/lang/Throwable  , 408:java/lang/Throwable  ], 258:[397:java/lang/Throwable  , 408:java/lang/Throwable  ], 261:[397:java/lang/Throwable  , 408:java/lang/Throwable  ], 265:[397:java/lang/Throwable  , 408:java/lang/Throwable  ], 269:[397:java/lang/Throwable  , 408:java/lang/Throwable  ], 270:[397:java/lang/Throwable  , 408:java/lang/Throwable  ], 273:[397:java/lang/Throwable  , 408:java/lang/Throwable  ], 277:[397:java/lang/Throwable  , 408:java/lang/Throwable  ], 289:[397:java/lang/Throwable  , 408:java/lang/Throwable  ], 295:[397:java/lang/Throwable  , 408:java/lang/Throwable  ], 298:[397:java/lang/Throwable  , 408:java/lang/Throwable  ], 301:[397:java/lang/Throwable  , 408:java/lang/Throwable  ], 304:[397:java/lang/Throwable  , 408:java/lang/Throwable  ], 307:[397:java/lang/Throwable  , 408:java/lang/Throwable  ], 311:[397:java/lang/Throwable  , 408:java/lang/Throwable  ], 315:[397:java/lang/Throwable  , 408:java/lang/Throwable  ], 319:[397:java/lang/Throwable  , 408:java/lang/Throwable  ], 323:[397:java/lang/Throwable  , 408:java/lang/Throwable  ], 329:[397:java/lang/Throwable  , 408:java/lang/Throwable  ], 331:[397:java/lang/Throwable  , 408:java/lang/Throwable  ], 333:[397:java/lang/Throwable  , 408:java/lang/Throwable  ], 335:[397:java/lang/Throwable  , 408:java/lang/Throwable  ], 336:[397:java/lang/Throwable  , 408:java/lang/Throwable  ], 341:[397:java/lang/Throwable  , 408:java/lang/Throwable  ], 344:[397:java/lang/Throwable  , 408:java/lang/Throwable  ], 345:[397:java/lang/Throwable  , 408:java/lang/Throwable  ], 347:[397:java/lang/Throwable  , 408:java/lang/Throwable  ], 350:[397:java/lang/Throwable  , 408:java/lang/Throwable  ], 352:[397:java/lang/Throwable  , 408:java/lang/Throwable  ], 353:[397:java/lang/Throwable  , 408:java/lang/Throwable  ], 356:[397:java/lang/Throwable  , 408:java/lang/Throwable  ], 361:[397:java/lang/Throwable  , 408:java/lang/Throwable  ], 362:[397:java/lang/Throwable  , 408:java/lang/Throwable  ], 365:[397:java/lang/Throwable  , 408:java/lang/Throwable  ], 368:[397:java/lang/Throwable  , 408:java/lang/Throwable  ], 370:[397:java/lang/Throwable  , 408:java/lang/Throwable  ], 372:[397:java/lang/Throwable  , 408:java/lang/Throwable  ], 374:[397:java/lang/Throwable  , 408:java/lang/Throwable  ], 375:[397:java/lang/Throwable  , 408:java/lang/Throwable  ], 377:[397:java/lang/Throwable  , 408:java/lang/Throwable  ], 379:[397:java/lang/Throwable  , 408:java/lang/Throwable  ], 381:[397:java/lang/Throwable  , 408:java/lang/Throwable  ], 383:[397:java/lang/Throwable  , 408:java/lang/Throwable  ], 389:[397:java/lang/Throwable  , 408:java/lang/Throwable  ], 391:[397:java/lang/Throwable  , 408:java/lang/Throwable  ], 392:[397:java/lang/Throwable  , 408:java/lang/Throwable  ], 393:[397:java/lang/Throwable  , 408:java/lang/Throwable  ], 395:[397:java/lang/Throwable  , 408:java/lang/Throwable  ], 399:[397:java/lang/Throwable  , 408:java/lang/Throwable  ], 401:[408:java/lang/Throwable  ], 403:[408:java/lang/Throwable  ], 406:[408:java/lang/Throwable  ], 410:[408:java/lang/Throwable  ]]
+TRY [217:[410:java/lang/Throwable  ], 219:[402:java/lang/Throwable  , 410:java/lang/Throwable  ], 221:[402:java/lang/Throwable  , 410:java/lang/Throwable  ], 222:[402:java/lang/Throwable  , 410:java/lang/Throwable  ], 224:[402:java/lang/Throwable  , 410:java/lang/Throwable  ], 225:[402:java/lang/Throwable  , 410:java/lang/Throwable  ], 226:[402:java/lang/Throwable  , 410:java/lang/Throwable  ], 227:[402:java/lang/Throwable  , 410:java/lang/Throwable  ], 230:[402:java/lang/Throwable  , 410:java/lang/Throwable  ], 233:[402:java/lang/Throwable  , 410:java/lang/Throwable  ], 234:[402:java/lang/Throwable  , 410:java/lang/Throwable  ], 237:[402:java/lang/Throwable  , 410:java/lang/Throwable  ], 242:[402:java/lang/Throwable  , 410:java/lang/Throwable  ], 243:[402:java/lang/Throwable  , 410:java/lang/Throwable  ], 246:[402:java/lang/Throwable  , 410:java/lang/Throwable  ], 255:[402:java/lang/Throwable  , 410:java/lang/Throwable  ], 258:[402:java/lang/Throwable  , 410:java/lang/Throwable  ], 259:[402:java/lang/Throwable  , 410:java/lang/Throwable  ], 262:[402:java/lang/Throwable  , 410:java/lang/Throwable  ], 266:[402:java/lang/Throwable  , 410:java/lang/Throwable  ], 270:[402:java/lang/Throwable  , 410:java/lang/Throwable  ], 271:[402:java/lang/Throwable  , 410:java/lang/Throwable  ], 274:[402:java/lang/Throwable  , 410:java/lang/Throwable  ], 278:[402:java/lang/Throwable  , 410:java/lang/Throwable  ], 290:[402:java/lang/Throwable  , 410:java/lang/Throwable  ], 296:[402:java/lang/Throwable  , 410:java/lang/Throwable  ], 299:[402:java/lang/Throwable  , 410:java/lang/Throwable  ], 302:[402:java/lang/Throwable  , 410:java/lang/Throwable  ], 305:[402:java/lang/Throwable  , 410:java/lang/Throwable  ], 308:[402:java/lang/Throwable  , 410:java/lang/Throwable  ], 312:[402:java/lang/Throwable  , 410:java/lang/Throwable  ], 316:[402:java/lang/Throwable  , 410:java/lang/Throwable  ], 320:[402:java/lang/Throwable  , 410:java/lang/Throwable  ], 324:[402:java/lang/Throwable  , 410:java/lang/Throwable  ], 330:[402:java/lang/Throwable  , 410:java/lang/Throwable  ], 332:[402:java/lang/Throwable  , 410:java/lang/Throwable  ], 334:[402:java/lang/Throwable  , 410:java/lang/Throwable  ], 336:[402:java/lang/Throwable  , 410:java/lang/Throwable  ], 337:[402:java/lang/Throwable  , 410:java/lang/Throwable  ], 339:[402:java/lang/Throwable  , 410:java/lang/Throwable  ], 341:[402:java/lang/Throwable  , 410:java/lang/Throwable  ], 343:[402:java/lang/Throwable  , 410:java/lang/Throwable  ], 349:[402:java/lang/Throwable  , 410:java/lang/Throwable  ], 351:[402:java/lang/Throwable  , 410:java/lang/Throwable  ], 352:[402:java/lang/Throwable  , 410:java/lang/Throwable  ], 353:[402:java/lang/Throwable  , 410:java/lang/Throwable  ], 355:[402:java/lang/Throwable  , 410:java/lang/Throwable  ], 357:[410:java/lang/Throwable  ], 361:[402:java/lang/Throwable  , 410:java/lang/Throwable  ], 365:[402:java/lang/Throwable  , 410:java/lang/Throwable  ], 368:[402:java/lang/Throwable  , 410:java/lang/Throwable  ], 370:[402:java/lang/Throwable  , 410:java/lang/Throwable  ], 373:[402:java/lang/Throwable  , 410:java/lang/Throwable  ], 375:[402:java/lang/Throwable  , 410:java/lang/Throwable  ], 376:[402:java/lang/Throwable  , 410:java/lang/Throwable  ], 379:[402:java/lang/Throwable  , 410:java/lang/Throwable  ], 384:[402:java/lang/Throwable  , 410:java/lang/Throwable  ], 385:[402:java/lang/Throwable  , 410:java/lang/Throwable  ], 388:[402:java/lang/Throwable  , 410:java/lang/Throwable  ], 391:[402:java/lang/Throwable  , 410:java/lang/Throwable  ], 393:[402:java/lang/Throwable  , 410:java/lang/Throwable  ], 395:[402:java/lang/Throwable  , 410:java/lang/Throwable  ], 397:[402:java/lang/Throwable  , 410:java/lang/Throwable  ], 398:[402:java/lang/Throwable  , 410:java/lang/Throwable  ], 400:[402:java/lang/Throwable  , 410:java/lang/Throwable  ], 404:[402:java/lang/Throwable  , 410:java/lang/Throwable  ], 406:[410:java/lang/Throwable  ], 408:[410:java/lang/Throwable  ], 412:[410:java/lang/Throwable  ]]
 ALOAD 1
 INVOKESTATIC local/lito381/server/Protocol.objectSpellRequest([B)Llocal/lito381/server/Protocol$ObjectSpellRequest;
 ALOAD 0
```

### `useCounterBarrierSpell([BLlocal/lito381/server/BattleSpellCatalog$CounterBarrierSpell;)V`

- 類別：`local/lito381/server/ClientSession`
- 類型：例外表涵蓋、含迴圈（路徑比較器不支援）；事件後繼：`LAYOUT_ONLY`
- 比較器：`method useCounterBarrierSpell([BLlocal/lito381/server/BattleSpellCatalog$CounterBarrierSpell;)V: left=117:[270:java/lang/Throwable  ] right=118:[273:java/lang/Throwable  ]; paths rejected: normal control-flow cycle; labelled CFGs differ`

```diff
@@ -1,5 +1,5 @@
 META 2 null [java/io/IOException]
-TRY [117:[270:java/lang/Throwable  ], 119:[270:java/lang/Throwable  ], 120:[270:java/lang/Throwable  ], 122:[270:java/lang/Throwable  ], 123:[270:java/lang/Throwable  ], 124:[270:java/lang/Throwable  ], 125:[270:java/lang/Throwable  ], 128:[270:java/lang/Throwable  ], 132:[270:java/lang/Throwable  ], 135:[270:java/lang/Throwable  ], 138:[270:java/lang/Throwable  ], 142:[270:java/lang/Throwable  ], 146:[270:java/lang/Throwable  ], 149:[270:java/lang/Throwable  ], 153:[270:java/lang/Throwable  ], 158:[270:java/lang/Throwable  ], 163:[270:java/lang/Throwable  ], 166:[270:java/lang/Throwable  ], 181:[270:java/lang/Throwable  ], 183:[270:java/lang/Throwable  ], 185:[270:java/lang/Throwable  ], 187:[270:java/lang/Throwable  ], 189:[270:java/lang/Throwable  ], 192:[270:java/lang/Throwable  ], 197:[270:java/lang/Throwable  ], 199:[270:java/lang/Throwable  ], 200:[270:java/lang/Throwable  ], 202:[270:java/lang/Throwable  ], 203:[270:java/lang/Throwable  ], 204:[270:java/lang/Throwable  ], 206:[270:java/lang/Throwable  ], 209:[270:java/lang/Throwable  ], 211:[270:java/lang/Throwable  ], 217:[270:java/lang/Throwable  ], 226:[270:java/lang/Throwable  ], 227:[270:java/lang/Throwable  ], 228:[270:java/lang/Throwable  ], 230:[270:java/lang/Throwable  ], 233:[270:java/lang/Throwable  ], 234:[270:java/lang/Throwable  ], 237:[270:java/lang/Throwable  ], 239:[270:java/lang/Throwable  ], 241:[270:java/lang/Throwable  ], 245:[270:java/lang/Throwable  ], 248:[270:java/lang/Throwable  ], 249:[270:java/lang/Throwable  ], 252:[270:java/lang/Throwable  ], 255:[270:java/lang/Throwable  ], 260:[270:java/lang/Throwable  ], 262:[270:java/lang/Throwable  ], 265:[270:java/lang/Throwable  ], 268:[270:java/lang/Throwable  ], 272:[270:java/lang/Throwable  ]]
+TRY [118:[273:java/lang/Throwable  ], 120:[273:java/lang/Throwable  ], 121:[273:java/lang/Throwable  ], 123:[273:java/lang/Throwable  ], 124:[273:java/lang/Throwable  ], 125:[273:java/lang/Throwable  ], 126:[273:java/lang/Throwable  ], 129:[273:java/lang/Throwable  ], 133:[273:java/lang/Throwable  ], 136:[273:java/lang/Throwable  ], 139:[273:java/lang/Throwable  ], 143:[273:java/lang/Throwable  ], 147:[273:java/lang/Throwable  ], 150:[273:java/lang/Throwable  ], 154:[273:java/lang/Throwable  ], 159:[273:java/lang/Throwable  ], 164:[273:java/lang/Throwable  ], 167:[273:java/lang/Throwable  ], 182:[273:java/lang/Throwable  ], 184:[273:java/lang/Throwable  ], 186:[273:java/lang/Throwable  ], 188:[273:java/lang/Throwable  ], 190:[273:java/lang/Throwable  ], 193:[273:java/lang/Throwable  ], 198:[273:java/lang/Throwable  ], 200:[273:java/lang/Throwable  ], 201:[273:java/lang/Throwable  ], 203:[273:java/lang/Throwable  ], 204:[273:java/lang/Throwable  ], 205:[273:java/lang/Throwable  ], 207:[273:java/lang/Throwable  ], 210:[273:java/lang/Throwable  ], 212:[273:java/lang/Throwable  ], 218:[273:java/lang/Throwable  ], 227:[273:java/lang/Throwable  ], 228:[273:java/lang/Throwable  ], 229:[273:java/lang/Throwable  ], 231:[273:java/lang/Throwable  ], 234:[273:java/lang/Throwable  ], 235:[273:java/lang/Throwable  ], 238:[273:java/lang/Throwable  ], 240:[273:java/lang/Throwable  ], 242:[273:java/lang/Throwable  ], 245:[273:java/lang/Throwable  ], 249:[273:java/lang/Throwable  ], 252:[273:java/lang/Throwable  ], 255:[273:java/lang/Throwable  ], 258:[273:java/lang/Throwable  ], 263:[273:java/lang/Throwable  ], 265:[273:java/lang/Throwable  ], 268:[273:java/lang/Throwable  ], 271:[273:java/lang/Throwable  ], 275:[273:java/lang/Throwable  ]]
 ALOAD 1
 INVOKESTATIC local/lito381/server/Protocol.selectorOnlySpellRequest([B)Llocal/lito381/server/Protocol$SpellSelector;
 ASTORE 3
```

### `useCounterDetectionSpell([BLlocal/lito381/server/BattleSpellCatalog$CounterDetectionSpell;)V`

- 類別：`local/lito381/server/ClientSession`
- 類型：例外表涵蓋、含迴圈（路徑比較器不支援）；事件後繼：`LAYOUT_ONLY`
- 比較器：`method useCounterDetectionSpell([BLlocal/lito381/server/BattleSpellCatalog$CounterDetectionSpell;)V: left=211:[313:java/lang/Throwable  ] right=214:[318:java/lang/Throwable  ]; paths rejected: normal control-flow cycle; labelled CFGs differ`

```diff
@@ -1,5 +1,5 @@
 META 2 null [java/io/IOException]
-TRY [211:[313:java/lang/Throwable  ], 214:[313:java/lang/Throwable  ], 216:[313:java/lang/Throwable  ], 217:[313:java/lang/Throwable  ], 219:[313:java/lang/Throwable  ], 220:[313:java/lang/Throwable  ], 221:[313:java/lang/Throwable  ], 222:[313:java/lang/Throwable  ], 229:[313:java/lang/Throwable  ], 233:[313:java/lang/Throwable  ], 236:[313:java/lang/Throwable  ], 237:[313:java/lang/Throwable  ], 240:[313:java/lang/Throwable  ], 243:[313:java/lang/Throwable  ], 248:[313:java/lang/Throwable  ], 251:[313:java/lang/Throwable  ], 255:[313:java/lang/Throwable  ], 259:[313:java/lang/Throwable  ], 263:[313:java/lang/Throwable  ], 266:[313:java/lang/Throwable  ], 268:[313:java/lang/Throwable  ], 270:[313:java/lang/Throwable  ], 273:[313:java/lang/Throwable  ], 278:[313:java/lang/Throwable  ], 279:[313:java/lang/Throwable  ], 280:[313:java/lang/Throwable  ], 282:[313:java/lang/Throwable  ], 287:[313:java/lang/Throwable  ], 290:[313:java/lang/Throwable  ], 291:[313:java/lang/Throwable  ], 293:[313:java/lang/Throwable  ], 296:[313:java/lang/Throwable  ], 298:[313:java/lang/Throwable  ], 299:[313:java/lang/Throwable  ], 302:[313:java/lang/Throwable  ], 307:[313:java/lang/Throwable  ], 308:[313:java/lang/Throwable  ], 311:[313:java/lang/Throwable  ], 315:[313:java/lang/Throwable  ], 369:[532:java/lang/Throwable  ], 370:[532:java/lang/Throwable  ], 373:[532:java/lang/Throwable  ], 378:[532:java/lang/Throwable  ], 381:[532:java/lang/Throwable  ], 384:[532:java/lang/Throwable  ], 388:[532:java/lang/Throwable  ], 390:[532:java/lang/Throwable  ], 393:[532:java/lang/Throwable  ], 395:[532:java/lang/Throwable  ], 400:[532:java/lang/Throwable  ], 402:[532:java/lang/Throwable  ], 403:[532:java/lang/Throwable  ], 412:[532:java/lang/Throwable  ], 415:[532:java/lang/Throwable  ], 421:[532:java/lang/Throwable  ], 429:[532:java/lang/Throwable  ], 431:[532:java/lang/Throwable  ], 434:[532:java/lang/Throwable  ], 435:[532:java/lang/Throwable  ], 442:[532:java/lang/Throwable  ], 449:[532:java/lang/Throwable  ], 453:[532:java/lang/Throwable  ], 456:[532:java/lang/Throwable  ], 458:[532:java/lang/Throwable  ], 460:[532:java/lang/Throwable  ], 461:[532:java/lang/Throwable  ], 464:[532:java/lang/Throwable  ], 467:[532:java/lang/Throwable  ], 468:[532:java/lang/Throwable  ], 472:[532:java/lang/Throwable  ], 474:[532:java/lang/Throwable  ], 478:[532:java/lang/Throwable  ], 479:[532:java/lang/Throwable  ], 482:[532:java/lang/Throwable  ], 484:[532:java/lang/Throwable  ], 486:[532:java/lang/Throwable  ], 487:[532:java/lang/Throwable  ], 490:[532:java/lang/Throwable  ], 493:[532:java/lang/Throwable  ], 498:[532:java/lang/Throwable  ], 499:[532:java/lang/Throwable  ], 500:[532:java/lang/Throwable  ], 505:[532:java/lang/Throwable  ], 506:[532:java/lang/Throwable  ], 507:[532:java/lang/Throwable  ], 512:[532:java/lang/Throwable  ], 517:[532:java/lang/Throwable  ], 518:[532:java/lang/Throwable  ], 521:[532:java/lang/Throwable  ], 524:[532:java/lang/Throwable  ], 526:[532:java/lang/Throwable  ], 527:[532:java/lang/Throwable  ], 530:[532:java/lang/Throwable  ], 534:[532:java/lang/Throwable  ]]
+TRY [214:[318:java/lang/Throwable  ], 217:[318:java/lang/Throwable  ], 219:[318:java/lang/Throwable  ], 220:[318:java/lang/Throwable  ], 222:[318:java/lang/Throwable  ], 223:[318:java/lang/Throwable  ], 224:[318:java/lang/Throwable  ], 225:[318:java/lang/Throwable  ], 232:[318:java/lang/Throwable  ], 236:[318:java/lang/Throwable  ], 239:[318:java/lang/Throwable  ], 240:[318:java/lang/Throwable  ], 243:[318:java/lang/Throwable  ], 246:[318:java/lang/Throwable  ], 251:[318:java/lang/Throwable  ], 254:[318:java/lang/Throwable  ], 258:[318:java/lang/Throwable  ], 262:[318:java/lang/Throwable  ], 266:[318:java/lang/Throwable  ], 269:[318:java/lang/Throwable  ], 271:[318:java/lang/Throwable  ], 273:[318:java/lang/Throwable  ], 276:[318:java/lang/Throwable  ], 281:[318:java/lang/Throwable  ], 282:[318:java/lang/Throwable  ], 283:[318:java/lang/Throwable  ], 285:[318:java/lang/Throwable  ], 289:[318:java/lang/Throwable  ], 293:[318:java/lang/Throwable  ], 296:[318:java/lang/Throwable  ], 298:[318:java/lang/Throwable  ], 301:[318:java/lang/Throwable  ], 303:[318:java/lang/Throwable  ], 304:[318:java/lang/Throwable  ], 307:[318:java/lang/Throwable  ], 312:[318:java/lang/Throwable  ], 313:[318:java/lang/Throwable  ], 316:[318:java/lang/Throwable  ], 320:[318:java/lang/Throwable  ], 374:[537:java/lang/Throwable  ], 375:[537:java/lang/Throwable  ], 378:[537:java/lang/Throwable  ], 383:[537:java/lang/Throwable  ], 386:[537:java/lang/Throwable  ], 389:[537:java/lang/Throwable  ], 393:[537:java/lang/Throwable  ], 395:[537:java/lang/Throwable  ], 398:[537:java/lang/Throwable  ], 400:[537:java/lang/Throwable  ], 405:[537:java/lang/Throwable  ], 407:[537:java/lang/Throwable  ], 408:[537:java/lang/Throwable  ], 417:[537:java/lang/Throwable  ], 420:[537:java/lang/Throwable  ], 426:[537:java/lang/Throwable  ], 434:[537:java/lang/Throwable  ], 436:[537:java/lang/Throwable  ], 439:[537:java/lang/Throwable  ], 440:[537:java/lang/Throwable  ], 447:[537:java/lang/Throwable  ], 454:[537:java/lang/Throwable  ], 458:[537:java/lang/Throwable  ], 461:[537:java/lang/Throwable  ], 463:[537:java/lang/Throwable  ], 465:[537:java/lang/Throwable  ], 466:[537:java/lang/Throwable  ], 469:[537:java/lang/Throwable  ], 472:[537:java/lang/Throwable  ], 473:[537:java/lang/Throwable  ], 477:[537:java/lang/Throwable  ], 479:[537:java/lang/Throwable  ], 483:[537:java/lang/Throwable  ], 484:[537:java/lang/Throwable  ], 487:[537:java/lang/Throwable  ], 489:[537:java/lang/Throwable  ], 491:[537:java/lang/Throwable  ], 492:[537:java/lang/Throwable  ], 495:[537:java/lang/Throwable  ], 498:[537:java/lang/Throwable  ], 503:[537:java/lang/Throwable  ], 504:[537:java/lang/Throwable  ], 505:[537:java/lang/Throwable  ], 510:[537:java/lang/Throwable  ], 511:[537:java/lang/Throwable  ], 512:[537:java/lang/Throwable  ], 517:[537:java/lang/Throwable  ], 522:[537:java/lang/Throwable  ], 523:[537:java/lang/Throwable  ], 526:[537:java/lang/Throwable  ], 529:[537:java/lang/Throwable  ], 531:[537:java/lang/Throwable  ], 532:[537:java/lang/Throwable  ], 535:[537:java/lang/Throwable  ], 539:[537:java/lang/Throwable  ]]
 CONST null
 ASTORE 3
 ALOAD 1
```

### `useDetectionSpell([BLlocal/lito381/server/BattleSpellCatalog$DetectionSpell;)V`

- 類別：`local/lito381/server/ClientSession`
- 類型：例外表涵蓋、含迴圈（路徑比較器不支援）；事件後繼：`LAYOUT_ONLY`
- 比較器：`method useDetectionSpell([BLlocal/lito381/server/BattleSpellCatalog$DetectionSpell;)V: left=145:[260:java/lang/Throwable  ] right=143:[256:java/lang/Throwable  ]; paths rejected: normal control-flow cycle; labelled CFGs differ`

```diff
@@ -1,5 +1,5 @@
 META 2 null [java/io/IOException]
-TRY [145:[260:java/lang/Throwable  ], 148:[260:java/lang/Throwable  ], 150:[260:java/lang/Throwable  ], 151:[260:java/lang/Throwable  ], 153:[260:java/lang/Throwable  ], 154:[260:java/lang/Throwable  ], 155:[260:java/lang/Throwable  ], 156:[260:java/lang/Throwable  ], 159:[260:java/lang/Throwable  ], 163:[260:java/lang/Throwable  ], 166:[260:java/lang/Throwable  ], 169:[260:java/lang/Throwable  ], 172:[260:java/lang/Throwable  ], 176:[260:java/lang/Throwable  ], 180:[260:java/lang/Throwable  ], 183:[260:java/lang/Throwable  ], 198:[260:java/lang/Throwable  ], 203:[260:java/lang/Throwable  ], 205:[260:java/lang/Throwable  ], 206:[260:java/lang/Throwable  ], 208:[260:java/lang/Throwable  ], 211:[260:java/lang/Throwable  ], 216:[260:java/lang/Throwable  ], 217:[260:java/lang/Throwable  ], 218:[260:java/lang/Throwable  ], 220:[260:java/lang/Throwable  ], 224:[260:java/lang/Throwable  ], 230:[260:java/lang/Throwable  ], 231:[260:java/lang/Throwable  ], 232:[260:java/lang/Throwable  ], 238:[260:java/lang/Throwable  ], 241:[260:java/lang/Throwable  ], 242:[260:java/lang/Throwable  ], 244:[260:java/lang/Throwable  ], 248:[260:java/lang/Throwable  ], 251:[260:java/lang/Throwable  ], 256:[260:java/lang/Throwable  ], 258:[260:java/lang/Throwable  ], 262:[260:java/lang/Throwable  ]]
+TRY [143:[256:java/lang/Throwable  ], 146:[256:java/lang/Throwable  ], 148:[256:java/lang/Throwable  ], 149:[256:java/lang/Throwable  ], 151:[256:java/lang/Throwable  ], 152:[256:java/lang/Throwable  ], 153:[256:java/lang/Throwable  ], 154:[256:java/lang/Throwable  ], 157:[256:java/lang/Throwable  ], 161:[256:java/lang/Throwable  ], 164:[256:java/lang/Throwable  ], 167:[256:java/lang/Throwable  ], 170:[256:java/lang/Throwable  ], 174:[256:java/lang/Throwable  ], 178:[256:java/lang/Throwable  ], 181:[256:java/lang/Throwable  ], 194:[256:java/lang/Throwable  ], 199:[256:java/lang/Throwable  ], 201:[256:java/lang/Throwable  ], 202:[256:java/lang/Throwable  ], 204:[256:java/lang/Throwable  ], 207:[256:java/lang/Throwable  ], 212:[256:java/lang/Throwable  ], 213:[256:java/lang/Throwable  ], 214:[256:java/lang/Throwable  ], 216:[256:java/lang/Throwable  ], 220:[256:java/lang/Throwable  ], 226:[256:java/lang/Throwable  ], 227:[256:java/lang/Throwable  ], 228:[256:java/lang/Throwable  ], 231:[256:java/lang/Throwable  ], 235:[256:java/lang/Throwable  ], 238:[256:java/lang/Throwable  ], 240:[256:java/lang/Throwable  ], 244:[256:java/lang/Throwable  ], 247:[256:java/lang/Throwable  ], 252:[256:java/lang/Throwable  ], 254:[256:java/lang/Throwable  ], 258:[256:java/lang/Throwable  ]]
 ALOAD 1
 INVOKESTATIC local/lito381/server/Protocol.selectorOnlySpellRequest([B)Llocal/lito381/server/Protocol$SpellSelector;
 ASTORE 3
```

### `useDiamondGolemSpell([BLlocal/lito381/server/BattleSpellCatalog$DiamondGolemSpell;)V`

- 類別：`local/lito381/server/ClientSession`
- 類型：例外表涵蓋、含迴圈（路徑比較器不支援）；事件後繼：`LAYOUT_ONLY`
- 比較器：`method useDiamondGolemSpell([BLlocal/lito381/server/BattleSpellCatalog$DiamondGolemSpell;)V: left=147:[358:java/lang/Throwable  ] right=143:[352:java/lang/Throwable  ]; paths rejected: normal control-flow cycle; labelled CFGs differ`

```diff
@@ -1,5 +1,5 @@
 META 2 null [java/io/IOException]
-TRY [147:[358:java/lang/Throwable  ], 149:[350:java/lang/Throwable  , 358:java/lang/Throwable  ], 151:[350:java/lang/Throwable  , 358:java/lang/Throwable  ], 152:[350:java/lang/Throwable  , 358:java/lang/Throwable  ], 154:[350:java/lang/Throwable  , 358:java/lang/Throwable  ], 155:[350:java/lang/Throwable  , 358:java/lang/Throwable  ], 156:[350:java/lang/Throwable  , 358:java/lang/Throwable  ], 157:[350:java/lang/Throwable  , 358:java/lang/Throwable  ], 160:[350:java/lang/Throwable  , 358:java/lang/Throwable  ], 163:[350:java/lang/Throwable  , 358:java/lang/Throwable  ], 164:[350:java/lang/Throwable  , 358:java/lang/Throwable  ], 167:[350:java/lang/Throwable  , 358:java/lang/Throwable  ], 172:[350:java/lang/Throwable  , 358:java/lang/Throwable  ], 173:[350:java/lang/Throwable  , 358:java/lang/Throwable  ], 176:[350:java/lang/Throwable  , 358:java/lang/Throwable  ], 185:[350:java/lang/Throwable  , 358:java/lang/Throwable  ], 186:[350:java/lang/Throwable  , 358:java/lang/Throwable  ], 189:[350:java/lang/Throwable  , 358:java/lang/Throwable  ], 193:[350:java/lang/Throwable  , 358:java/lang/Throwable  ], 197:[350:java/lang/Throwable  , 358:java/lang/Throwable  ], 198:[350:java/lang/Throwable  , 358:java/lang/Throwable  ], 201:[350:java/lang/Throwable  , 358:java/lang/Throwable  ], 205:[350:java/lang/Throwable  , 358:java/lang/Throwable  ], 216:[350:java/lang/Throwable  , 358:java/lang/Throwable  ], 219:[350:java/lang/Throwable  , 358:java/lang/Throwable  ], 222:[350:java/lang/Throwable  , 358:java/lang/Throwable  ], 225:[350:java/lang/Throwable  , 358:java/lang/Throwable  ], 227:[350:java/lang/Throwable  , 358:java/lang/Throwable  ], 232:[350:java/lang/Throwable  , 358:java/lang/Throwable  ], 234:[350:java/lang/Throwable  , 358:java/lang/Throwable  ], 235:[350:java/lang/Throwable  , 358:java/lang/Throwable  ], 237:[350:java/lang/Throwable  , 358:java/lang/Throwable  ], 240:[350:java/lang/Throwable  , 358:java/lang/Throwable  ], 243:[350:java/lang/Throwable  , 358:java/lang/Throwable  ], 247:[350:java/lang/Throwable  , 358:java/lang/Throwable  ], 249:[350:java/lang/Throwable  , 358:java/lang/Throwable  ], 251:[350:java/lang/Throwable  , 358:java/lang/Throwable  ], 252:[350:java/lang/Throwable  , 358:java/lang/Throwable  ], 267:[350:java/lang/Throwable  , 358:java/lang/Throwable  ], 270:[350:java/lang/Throwable  , 358:java/lang/Throwable  ], 274:[350:java/lang/Throwable  , 358:java/lang/Throwable  ], 276:[350:java/lang/Throwable  , 358:java/lang/Throwable  ], 278:[350:java/lang/Throwable  , 358:java/lang/Throwable  ], 283:[350:java/lang/Throwable  , 358:java/lang/Throwable  ], 285:[350:java/lang/Throwable  , 358:java/lang/Throwable  ], 286:[350:java/lang/Throwable  , 358:java/lang/Throwable  ], 287:[350:java/lang/Throwable  , 358:java/lang/Throwable  ], 289:[350:java/lang/Throwable  , 358:java/lang/Throwable  ], 291:[358:java/lang/Throwable  ], 294:[350:java/lang/Throwable  , 358:java/lang/Throwable  ], 300:[350:java/lang/Throwable  , 358:java/lang/Throwable  ], 301:[350:java/lang/Throwable  , 358:java/lang/Throwable  ], 303:[350:java/lang/Throwable  , 358:java/lang/Throwable  ], 305:[350:java/lang/Throwable  , 358:java/lang/Throwable  ], 307:[358:java/lang/Throwable  ], 311:[350:java/lang/Throwable  , 358:java/lang/Throwable  ], 312:[350:java/lang/Throwable  , 358:java/lang/Throwable  ], 313:[350:java/lang/Throwable  , 358:java/lang/Throwable  ], 315:[350:java/lang/Throwable  , 358:java/lang/Throwable  ], 318:[350:java/lang/Throwable  , 358:java/lang/Throwable  ], 320:[350:java/lang/Throwable  , 358:java/lang/Throwable  ], 321:[350:java/lang/Throwable  , 358:java/lang/Throwable  ], 328:[350:java/lang/Throwable  , 358:java/lang/Throwable  ], 331:[350:java/lang/Throwable  , 358:java/lang/Throwable  ], 332:[350:java/lang/Throwable  , 358:java/lang/Throwable  ], 334:[350:java/lang/Throwable  , 358:java/lang/Throwable  ], 338:[350:java/lang/Throwable  , 358:java/lang/Throwable  ], 341:[350:java/lang/Throwable  , 358:java/lang/Throwable  ], 346:[350:java/lang/Throwable  , 358:java/lang/Throwable  ], 348:[350:java/lang/Throwable  , 358:java/lang/Throwable  ], 352:[350:java/lang/Throwable  , 358:java/lang/Throwable  ], 354:[358:java/lang/Throwable  ], 356:[358:java/lang/Throwable  ], 360:[358:java/lang/Throwable  ]]
+TRY [143:[352:java/lang/Throwable  ], 145:[344:java/lang/Throwable  , 352:java/lang/Throwable  ], 147:[344:java/lang/Throwable  , 352:java/lang/Throwable  ], 148:[344:java/lang/Throwable  , 352:java/lang/Throwable  ], 150:[344:java/lang/Throwable  , 352:java/lang/Throwable  ], 151:[344:java/lang/Throwable  , 352:java/lang/Throwable  ], 152:[344:java/lang/Throwable  , 352:java/lang/Throwable  ], 153:[344:java/lang/Throwable  , 352:java/lang/Throwable  ], 156:[344:java/lang/Throwable  , 352:java/lang/Throwable  ], 159:[344:java/lang/Throwable  , 352:java/lang/Throwable  ], 160:[344:java/lang/Throwable  , 352:java/lang/Throwable  ], 163:[344:java/lang/Throwable  , 352:java/lang/Throwable  ], 168:[344:java/lang/Throwable  , 352:java/lang/Throwable  ], 169:[344:java/lang/Throwable  , 352:java/lang/Throwable  ], 172:[344:java/lang/Throwable  , 352:java/lang/Throwable  ], 181:[344:java/lang/Throwable  , 352:java/lang/Throwable  ], 182:[344:java/lang/Throwable  , 352:java/lang/Throwable  ], 185:[344:java/lang/Throwable  , 352:java/lang/Throwable  ], 189:[344:java/lang/Throwable  , 352:java/lang/Throwable  ], 193:[344:java/lang/Throwable  , 352:java/lang/Throwable  ], 194:[344:java/lang/Throwable  , 352:java/lang/Throwable  ], 197:[344:java/lang/Throwable  , 352:java/lang/Throwable  ], 201:[344:java/lang/Throwable  , 352:java/lang/Throwable  ], 212:[344:java/lang/Throwable  , 352:java/lang/Throwable  ], 215:[344:java/lang/Throwable  , 352:java/lang/Throwable  ], 218:[344:java/lang/Throwable  , 352:java/lang/Throwable  ], 221:[344:java/lang/Throwable  , 352:java/lang/Throwable  ], 223:[344:java/lang/Throwable  , 352:java/lang/Throwable  ], 228:[344:java/lang/Throwable  , 352:java/lang/Throwable  ], 230:[344:java/lang/Throwable  , 352:java/lang/Throwable  ], 231:[344:java/lang/Throwable  , 352:java/lang/Throwable  ], 233:[344:java/lang/Throwable  , 352:java/lang/Throwable  ], 236:[344:java/lang/Throwable  , 352:java/lang/Throwable  ], 239:[344:java/lang/Throwable  , 352:java/lang/Throwable  ], 243:[344:java/lang/Throwable  , 352:java/lang/Throwable  ], 245:[344:java/lang/Throwable  , 352:java/lang/Throwable  ], 247:[344:java/lang/Throwable  , 352:java/lang/Throwable  ], 248:[344:java/lang/Throwable  , 352:java/lang/Throwable  ], 261:[344:java/lang/Throwable  , 352:java/lang/Throwable  ], 264:[344:java/lang/Throwable  , 352:java/lang/Throwable  ], 268:[344:java/lang/Throwable  , 352:java/lang/Throwable  ], 270:[344:java/lang/Throwable  , 352:java/lang/Throwable  ], 272:[344:java/lang/Throwable  , 352:java/lang/Throwable  ], 277:[344:java/lang/Throwable  , 352:java/lang/Throwable  ], 279:[344:java/lang/Throwable  , 352:java/lang/Throwable  ], 280:[344:java/lang/Throwable  , 352:java/lang/Throwable  ], 281:[344:java/lang/Throwable  , 352:java/lang/Throwable  ], 283:[344:java/lang/Throwable  , 352:java/lang/Throwable  ], 285:[352:java/lang/Throwable  ], 288:[344:java/lang/Throwable  , 352:java/lang/Throwable  ], 294:[344:java/lang/Throwable  , 352:java/lang/Throwable  ], 295:[344:java/lang/Throwable  , 352:java/lang/Throwable  ], 297:[344:java/lang/Throwable  , 352:java/lang/Throwable  ], 299:[344:java/lang/Throwable  , 352:java/lang/Throwable  ], 301:[352:java/lang/Throwable  ], 305:[344:java/lang/Throwable  , 352:java/lang/Throwable  ], 306:[344:java/lang/Throwable  , 352:java/lang/Throwable  ], 307:[344:java/lang/Throwable  , 352:java/lang/Throwable  ], 309:[344:java/lang/Throwable  , 352:java/lang/Throwable  ], 312:[344:java/lang/Throwable  , 352:java/lang/Throwable  ], 314:[344:java/lang/Throwable  , 352:java/lang/Throwable  ], 315:[344:java/lang/Throwable  , 352:java/lang/Throwable  ], 319:[344:java/lang/Throwable  , 352:java/lang/Throwable  ], 323:[344:java/lang/Throwable  , 352:java/lang/Throwable  ], 326:[344:java/lang/Throwable  , 352:java/lang/Throwable  ], 328:[344:java/lang/Throwable  , 352:java/lang/Throwable  ], 332:[344:java/lang/Throwable  , 352:java/lang/Throwable  ], 335:[344:java/lang/Throwable  , 352:java/lang/Throwable  ], 340:[344:java/lang/Throwable  , 352:java/lang/Throwable  ], 342:[344:java/lang/Throwable  , 352:java/lang/Throwable  ], 346:[344:java/lang/Throwable  , 352:java/lang/Throwable  ], 348:[352:java/lang/Throwable  ], 350:[352:java/lang/Throwable  ], 354:[352:java/lang/Throwable  ]]
 ALOAD 1
 INVOKESTATIC local/lito381/server/Protocol.selectorOnlySpellRequest([B)Llocal/lito381/server/Protocol$SpellSelector;
 ASTORE 3
```

### `useDodgeBuffSpell([BLlocal/lito381/server/BattleSpellCatalog$DodgeBuffSpell;)V`

- 類別：`local/lito381/server/ClientSession`
- 類型：例外表涵蓋、含迴圈（路徑比較器不支援）；事件後繼：`LAYOUT_ONLY`
- 比較器：`method useDodgeBuffSpell([BLlocal/lito381/server/BattleSpellCatalog$DodgeBuffSpell;)V: left=157:[304:java/lang/Throwable  ] right=158:[307:java/lang/Throwable  ]; paths rejected: normal control-flow cycle; labelled CFGs differ`

```diff
@@ -1,5 +1,5 @@
 META 2 null [java/io/IOException]
-TRY [157:[304:java/lang/Throwable  ], 160:[304:java/lang/Throwable  ], 162:[304:java/lang/Throwable  ], 163:[304:java/lang/Throwable  ], 165:[304:java/lang/Throwable  ], 166:[304:java/lang/Throwable  ], 167:[304:java/lang/Throwable  ], 168:[304:java/lang/Throwable  ], 171:[304:java/lang/Throwable  ], 175:[304:java/lang/Throwable  ], 178:[304:java/lang/Throwable  ], 184:[304:java/lang/Throwable  ], 188:[304:java/lang/Throwable  ], 202:[304:java/lang/Throwable  ], 205:[304:java/lang/Throwable  ], 207:[304:java/lang/Throwable  ], 209:[304:java/lang/Throwable  ], 211:[304:java/lang/Throwable  ], 217:[304:java/lang/Throwable  ], 218:[304:java/lang/Throwable  ], 219:[304:java/lang/Throwable  ], 221:[304:java/lang/Throwable  ], 224:[304:java/lang/Throwable  ], 230:[304:java/lang/Throwable  ], 231:[304:java/lang/Throwable  ], 233:[304:java/lang/Throwable  ], 234:[304:java/lang/Throwable  ], 235:[304:java/lang/Throwable  ], 237:[304:java/lang/Throwable  ], 241:[304:java/lang/Throwable  ], 242:[304:java/lang/Throwable  ], 243:[304:java/lang/Throwable  ], 245:[304:java/lang/Throwable  ], 249:[304:java/lang/Throwable  ], 252:[304:java/lang/Throwable  ], 254:[304:java/lang/Throwable  ], 255:[304:java/lang/Throwable  ], 258:[304:java/lang/Throwable  ], 260:[304:java/lang/Throwable  ], 261:[304:java/lang/Throwable  ], 265:[304:java/lang/Throwable  ], 268:[304:java/lang/Throwable  ], 270:[304:java/lang/Throwable  ], 271:[304:java/lang/Throwable  ], 274:[304:java/lang/Throwable  ], 276:[304:java/lang/Throwable  ], 277:[304:java/lang/Throwable  ], 282:[304:java/lang/Throwable  ], 285:[304:java/lang/Throwable  ], 286:[304:java/lang/Throwable  ], 288:[304:java/lang/Throwable  ], 292:[304:java/lang/Throwable  ], 295:[304:java/lang/Throwable  ], 300:[304:java/lang/Throwable  ], 302:[304:java/lang/Throwable  ], 306:[304:java/lang/Throwable  ]]
+TRY [158:[307:java/lang/Throwable  ], 161:[307:java/lang/Throwable  ], 163:[307:java/lang/Throwable  ], 164:[307:java/lang/Throwable  ], 166:[307:java/lang/Throwable  ], 167:[307:java/lang/Throwable  ], 168:[307:java/lang/Throwable  ], 169:[307:java/lang/Throwable  ], 172:[307:java/lang/Throwable  ], 176:[307:java/lang/Throwable  ], 179:[307:java/lang/Throwable  ], 185:[307:java/lang/Throwable  ], 189:[307:java/lang/Throwable  ], 203:[307:java/lang/Throwable  ], 206:[307:java/lang/Throwable  ], 208:[307:java/lang/Throwable  ], 210:[307:java/lang/Throwable  ], 212:[307:java/lang/Throwable  ], 218:[307:java/lang/Throwable  ], 219:[307:java/lang/Throwable  ], 220:[307:java/lang/Throwable  ], 222:[307:java/lang/Throwable  ], 225:[307:java/lang/Throwable  ], 231:[307:java/lang/Throwable  ], 232:[307:java/lang/Throwable  ], 234:[307:java/lang/Throwable  ], 235:[307:java/lang/Throwable  ], 236:[307:java/lang/Throwable  ], 238:[307:java/lang/Throwable  ], 242:[307:java/lang/Throwable  ], 243:[307:java/lang/Throwable  ], 244:[307:java/lang/Throwable  ], 246:[307:java/lang/Throwable  ], 250:[307:java/lang/Throwable  ], 253:[307:java/lang/Throwable  ], 255:[307:java/lang/Throwable  ], 256:[307:java/lang/Throwable  ], 259:[307:java/lang/Throwable  ], 261:[307:java/lang/Throwable  ], 262:[307:java/lang/Throwable  ], 266:[307:java/lang/Throwable  ], 269:[307:java/lang/Throwable  ], 271:[307:java/lang/Throwable  ], 272:[307:java/lang/Throwable  ], 275:[307:java/lang/Throwable  ], 277:[307:java/lang/Throwable  ], 278:[307:java/lang/Throwable  ], 282:[307:java/lang/Throwable  ], 286:[307:java/lang/Throwable  ], 289:[307:java/lang/Throwable  ], 291:[307:java/lang/Throwable  ], 295:[307:java/lang/Throwable  ], 298:[307:java/lang/Throwable  ], 303:[307:java/lang/Throwable  ], 305:[307:java/lang/Throwable  ], 309:[307:java/lang/Throwable  ]]
 ALOAD 1
 INVOKESTATIC local/lito381/server/Protocol.selectorOnlySpellRequest([B)Llocal/lito381/server/Protocol$SpellSelector;
 ASTORE 3
```

### `useDragonSkinSpell([BLlocal/lito381/server/BattleSpellCatalog$DragonSkinSpell;)V`

- 類別：`local/lito381/server/ClientSession`
- 類型：例外表涵蓋、含迴圈（路徑比較器不支援）；事件後繼：`LAYOUT_ONLY`
- 比較器：`method useDragonSkinSpell([BLlocal/lito381/server/BattleSpellCatalog$DragonSkinSpell;)V: left=94:[228:java/lang/Throwable  ] right=96:[230:java/lang/Throwable  ]; paths rejected: normal control-flow cycle; labelled CFGs differ`

```diff
@@ -1,5 +1,5 @@
 META 2 null [java/io/IOException]
-TRY [94:[228:java/lang/Throwable  ], 96:[228:java/lang/Throwable  ], 97:[228:java/lang/Throwable  ], 99:[228:java/lang/Throwable  ], 100:[228:java/lang/Throwable  ], 101:[228:java/lang/Throwable  ], 102:[228:java/lang/Throwable  ], 105:[228:java/lang/Throwable  ], 109:[228:java/lang/Throwable  ], 112:[228:java/lang/Throwable  ], 115:[228:java/lang/Throwable  ], 119:[228:java/lang/Throwable  ], 123:[228:java/lang/Throwable  ], 126:[228:java/lang/Throwable  ], 130:[228:java/lang/Throwable  ], 135:[228:java/lang/Throwable  ], 140:[228:java/lang/Throwable  ], 143:[228:java/lang/Throwable  ], 156:[228:java/lang/Throwable  ], 158:[228:java/lang/Throwable  ], 160:[228:java/lang/Throwable  ], 165:[228:java/lang/Throwable  ], 167:[228:java/lang/Throwable  ], 168:[228:java/lang/Throwable  ], 170:[228:java/lang/Throwable  ], 171:[228:java/lang/Throwable  ], 172:[228:java/lang/Throwable  ], 174:[228:java/lang/Throwable  ], 177:[228:java/lang/Throwable  ], 179:[228:java/lang/Throwable  ], 188:[228:java/lang/Throwable  ], 189:[228:java/lang/Throwable  ], 190:[228:java/lang/Throwable  ], 192:[228:java/lang/Throwable  ], 195:[228:java/lang/Throwable  ], 197:[228:java/lang/Throwable  ], 198:[228:java/lang/Throwable  ], 202:[228:java/lang/Throwable  ], 204:[228:java/lang/Throwable  ], 206:[228:java/lang/Throwable  ], 209:[228:java/lang/Throwable  ], 213:[228:java/lang/Throwable  ], 216:[228:java/lang/Throwable  ], 221:[228:java/lang/Throwable  ], 223:[228:java/lang/Throwable  ], 226:[228:java/lang/Throwable  ], 230:[228:java/lang/Throwable  ]]
+TRY [96:[230:java/lang/Throwable  ], 98:[230:java/lang/Throwable  ], 99:[230:java/lang/Throwable  ], 101:[230:java/lang/Throwable  ], 102:[230:java/lang/Throwable  ], 103:[230:java/lang/Throwable  ], 104:[230:java/lang/Throwable  ], 107:[230:java/lang/Throwable  ], 111:[230:java/lang/Throwable  ], 114:[230:java/lang/Throwable  ], 117:[230:java/lang/Throwable  ], 121:[230:java/lang/Throwable  ], 125:[230:java/lang/Throwable  ], 128:[230:java/lang/Throwable  ], 132:[230:java/lang/Throwable  ], 137:[230:java/lang/Throwable  ], 142:[230:java/lang/Throwable  ], 145:[230:java/lang/Throwable  ], 158:[230:java/lang/Throwable  ], 160:[230:java/lang/Throwable  ], 162:[230:java/lang/Throwable  ], 167:[230:java/lang/Throwable  ], 169:[230:java/lang/Throwable  ], 170:[230:java/lang/Throwable  ], 172:[230:java/lang/Throwable  ], 173:[230:java/lang/Throwable  ], 174:[230:java/lang/Throwable  ], 176:[230:java/lang/Throwable  ], 179:[230:java/lang/Throwable  ], 181:[230:java/lang/Throwable  ], 190:[230:java/lang/Throwable  ], 191:[230:java/lang/Throwable  ], 192:[230:java/lang/Throwable  ], 194:[230:java/lang/Throwable  ], 197:[230:java/lang/Throwable  ], 199:[230:java/lang/Throwable  ], 200:[230:java/lang/Throwable  ], 204:[230:java/lang/Throwable  ], 206:[230:java/lang/Throwable  ], 208:[230:java/lang/Throwable  ], 211:[230:java/lang/Throwable  ], 215:[230:java/lang/Throwable  ], 218:[230:java/lang/Throwable  ], 223:[230:java/lang/Throwable  ], 225:[230:java/lang/Throwable  ], 228:[230:java/lang/Throwable  ], 232:[230:java/lang/Throwable  ]]
 ALOAD 1
 INVOKESTATIC local/lito381/server/Protocol.selectorOnlySpellRequest([B)Llocal/lito381/server/Protocol$SpellSelector;
 ASTORE 3
```

### `useEarthBlessSpell([BLlocal/lito381/server/BattleSpellCatalog$EarthBlessSpell;)V`

- 類別：`local/lito381/server/ClientSession`
- 類型：例外表涵蓋、含迴圈（路徑比較器不支援）；事件後繼：`LAYOUT_ONLY`
- 比較器：`method useEarthBlessSpell([BLlocal/lito381/server/BattleSpellCatalog$EarthBlessSpell;)V: left=259:[353:java/lang/Throwable  ] right=265:[361:java/lang/Throwable  ]; paths rejected: normal control-flow cycle; labelled CFGs differ`

```diff
@@ -1,5 +1,5 @@
 META 2 null [java/io/IOException]
-TRY [259:[353:java/lang/Throwable  ], 262:[353:java/lang/Throwable  ], 264:[353:java/lang/Throwable  ], 265:[353:java/lang/Throwable  ], 267:[353:java/lang/Throwable  ], 268:[353:java/lang/Throwable  ], 269:[353:java/lang/Throwable  ], 270:[353:java/lang/Throwable  ], 277:[353:java/lang/Throwable  ], 281:[353:java/lang/Throwable  ], 284:[353:java/lang/Throwable  ], 287:[353:java/lang/Throwable  ], 291:[353:java/lang/Throwable  ], 294:[353:java/lang/Throwable  ], 299:[353:java/lang/Throwable  ], 302:[353:java/lang/Throwable  ], 306:[353:java/lang/Throwable  ], 310:[353:java/lang/Throwable  ], 316:[353:java/lang/Throwable  ], 317:[353:java/lang/Throwable  ], 319:[353:java/lang/Throwable  ], 321:[353:java/lang/Throwable  ], 325:[353:java/lang/Throwable  ], 326:[353:java/lang/Throwable  ], 327:[353:java/lang/Throwable  ], 331:[353:java/lang/Throwable  ], 334:[353:java/lang/Throwable  ], 335:[353:java/lang/Throwable  ], 337:[353:java/lang/Throwable  ], 341:[353:java/lang/Throwable  ], 344:[353:java/lang/Throwable  ], 349:[353:java/lang/Throwable  ], 351:[353:java/lang/Throwable  ], 355:[353:java/lang/Throwable  ], 385:[467:java/lang/Throwable  ], 388:[467:java/lang/Throwable  ], 392:[467:java/lang/Throwable  ], 395:[467:java/lang/Throwable  ], 397:[467:java/lang/Throwable  ], 399:[467:java/lang/Throwable  ], 400:[467:java/lang/Throwable  ], 405:[467:java/lang/Throwable  ], 407:[467:java/lang/Throwable  ], 408:[467:java/lang/Throwable  ], 411:[467:java/lang/Throwable  ], 414:[467:java/lang/Throwable  ], 415:[467:java/lang/Throwable  ], 418:[467:java/lang/Throwable  ], 422:[467:java/lang/Throwable  ], 427:[467:java/lang/Throwable  ], 428:[467:java/lang/Throwable  ], 431:[467:java/lang/Throwable  ], 436:[467:java/lang/Throwable  ], 439:[467:java/lang/Throwable  ], 449:[467:java/lang/Throwable  ], 454:[467:java/lang/Throwable  ], 457:[467:java/lang/Throwable  ], 459:[467:java/lang/Throwable  ], 460:[467:java/lang/Throwable  ], 461:[467:java/lang/Throwable  ], 462:[467:java/lang/Throwable  ], 465:[467:java/lang/Throwable  ], 469:[467:java/lang/Throwable  ]]
+TRY [265:[361:java/lang/Throwable  ], 268:[361:java/lang/Throwable  ], 270:[361:java/lang/Throwable  ], 271:[361:java/lang/Throwable  ], 273:[361:java/lang/Throwable  ], 274:[361:java/lang/Throwable  ], 275:[361:java/lang/Throwable  ], 276:[361:java/lang/Throwable  ], 283:[361:java/lang/Throwable  ], 287:[361:java/lang/Throwable  ], 290:[361:java/lang/Throwable  ], 293:[361:java/lang/Throwable  ], 297:[361:java/lang/Throwable  ], 300:[361:java/lang/Throwable  ], 305:[361:java/lang/Throwable  ], 308:[361:java/lang/Throwable  ], 312:[361:java/lang/Throwable  ], 316:[361:java/lang/Throwable  ], 322:[361:java/lang/Throwable  ], 323:[361:java/lang/Throwable  ], 325:[361:java/lang/Throwable  ], 327:[361:java/lang/Throwable  ], 331:[361:java/lang/Throwable  ], 332:[361:java/lang/Throwable  ], 333:[361:java/lang/Throwable  ], 336:[361:java/lang/Throwable  ], 340:[361:java/lang/Throwable  ], 343:[361:java/lang/Throwable  ], 345:[361:java/lang/Throwable  ], 349:[361:java/lang/Throwable  ], 352:[361:java/lang/Throwable  ], 357:[361:java/lang/Throwable  ], 359:[361:java/lang/Throwable  ], 363:[361:java/lang/Throwable  ], 393:[475:java/lang/Throwable  ], 396:[475:java/lang/Throwable  ], 400:[475:java/lang/Throwable  ], 403:[475:java/lang/Throwable  ], 405:[475:java/lang/Throwable  ], 407:[475:java/lang/Throwable  ], 408:[475:java/lang/Throwable  ], 413:[475:java/lang/Throwable  ], 415:[475:java/lang/Throwable  ], 416:[475:java/lang/Throwable  ], 419:[475:java/lang/Throwable  ], 422:[475:java/lang/Throwable  ], 423:[475:java/lang/Throwable  ], 426:[475:java/lang/Throwable  ], 430:[475:java/lang/Throwable  ], 435:[475:java/lang/Throwable  ], 436:[475:java/lang/Throwable  ], 439:[475:java/lang/Throwable  ], 444:[475:java/lang/Throwable  ], 447:[475:java/lang/Throwable  ], 457:[475:java/lang/Throwable  ], 462:[475:java/lang/Throwable  ], 465:[475:java/lang/Throwable  ], 467:[475:java/lang/Throwable  ], 468:[475:java/lang/Throwable  ], 469:[475:java/lang/Throwable  ], 470:[475:java/lang/Throwable  ], 473:[475:java/lang/Throwable  ], 477:[475:java/lang/Throwable  ]]
 ALOAD 1
 INVOKESTATIC local/lito381/server/Protocol.selectorOnlySpellRequest([B)Llocal/lito381/server/Protocol$SpellSelector;
 ASTORE 3
```

### `useFireBlessSpell([BLlocal/lito381/server/BattleSpellCatalog$FireBlessSpell;)V`

- 類別：`local/lito381/server/ClientSession`
- 類型：例外表涵蓋、含迴圈（路徑比較器不支援）；事件後繼：`LAYOUT_ONLY`
- 比較器：`method useFireBlessSpell([BLlocal/lito381/server/BattleSpellCatalog$FireBlessSpell;)V: left=259:[369:java/lang/Throwable  ] right=265:[377:java/lang/Throwable  ]; paths rejected: normal control-flow cycle; labelled CFGs differ`

```diff
@@ -1,5 +1,5 @@
 META 2 null [java/io/IOException]
-TRY [259:[369:java/lang/Throwable  ], 262:[369:java/lang/Throwable  ], 264:[369:java/lang/Throwable  ], 265:[369:java/lang/Throwable  ], 267:[369:java/lang/Throwable  ], 268:[369:java/lang/Throwable  ], 269:[369:java/lang/Throwable  ], 270:[369:java/lang/Throwable  ], 277:[369:java/lang/Throwable  ], 281:[369:java/lang/Throwable  ], 284:[369:java/lang/Throwable  ], 287:[369:java/lang/Throwable  ], 291:[369:java/lang/Throwable  ], 294:[369:java/lang/Throwable  ], 299:[369:java/lang/Throwable  ], 302:[369:java/lang/Throwable  ], 306:[369:java/lang/Throwable  ], 310:[369:java/lang/Throwable  ], 316:[369:java/lang/Throwable  ], 317:[369:java/lang/Throwable  ], 319:[369:java/lang/Throwable  ], 320:[369:java/lang/Throwable  ], 322:[369:java/lang/Throwable  ], 324:[369:java/lang/Throwable  ], 327:[369:java/lang/Throwable  ], 328:[369:java/lang/Throwable  ], 333:[369:java/lang/Throwable  ], 334:[369:java/lang/Throwable  ], 335:[369:java/lang/Throwable  ], 337:[369:java/lang/Throwable  ], 341:[369:java/lang/Throwable  ], 342:[369:java/lang/Throwable  ], 343:[369:java/lang/Throwable  ], 347:[369:java/lang/Throwable  ], 350:[369:java/lang/Throwable  ], 351:[369:java/lang/Throwable  ], 353:[369:java/lang/Throwable  ], 357:[369:java/lang/Throwable  ], 360:[369:java/lang/Throwable  ], 365:[369:java/lang/Throwable  ], 367:[369:java/lang/Throwable  ], 371:[369:java/lang/Throwable  ], 401:[483:java/lang/Throwable  ], 404:[483:java/lang/Throwable  ], 408:[483:java/lang/Throwable  ], 411:[483:java/lang/Throwable  ], 413:[483:java/lang/Throwable  ], 415:[483:java/lang/Throwable  ], 416:[483:java/lang/Throwable  ], 421:[483:java/lang/Throwable  ], 423:[483:java/lang/Throwable  ], 424:[483:java/lang/Throwable  ], 427:[483:java/lang/Throwable  ], 430:[483:java/lang/Throwable  ], 431:[483:java/lang/Throwable  ], 434:[483:java/lang/Throwable  ], 438:[483:java/lang/Throwable  ], 443:[483:java/lang/Throwable  ], 444:[483:java/lang/Throwable  ], 447:[483:java/lang/Throwable  ], 452:[483:java/lang/Throwable  ], 455:[483:java/lang/Throwable  ], 465:[483:java/lang/Throwable  ], 470:[483:java/lang/Throwable  ], 473:[483:java/lang/Throwable  ], 475:[483:java/lang/Throwable  ], 476:[483:java/lang/Throwable  ], 477:[483:java/lang/Throwable  ], 478:[483:java/lang/Throwable  ], 481:[483:java/lang/Throwable  ], 485:[483:java/lang/Throwable  ]]
+TRY [265:[377:java/lang/Throwable  ], 268:[377:java/lang/Throwable  ], 270:[377:java/lang/Throwable  ], 271:[377:java/lang/Throwable  ], 273:[377:java/lang/Throwable  ], 274:[377:java/lang/Throwable  ], 275:[377:java/lang/Throwable  ], 276:[377:java/lang/Throwable  ], 283:[377:java/lang/Throwable  ], 287:[377:java/lang/Throwable  ], 290:[377:java/lang/Throwable  ], 293:[377:java/lang/Throwable  ], 297:[377:java/lang/Throwable  ], 300:[377:java/lang/Throwable  ], 305:[377:java/lang/Throwable  ], 308:[377:java/lang/Throwable  ], 312:[377:java/lang/Throwable  ], 316:[377:java/lang/Throwable  ], 322:[377:java/lang/Throwable  ], 323:[377:java/lang/Throwable  ], 325:[377:java/lang/Throwable  ], 326:[377:java/lang/Throwable  ], 328:[377:java/lang/Throwable  ], 330:[377:java/lang/Throwable  ], 333:[377:java/lang/Throwable  ], 334:[377:java/lang/Throwable  ], 339:[377:java/lang/Throwable  ], 340:[377:java/lang/Throwable  ], 341:[377:java/lang/Throwable  ], 343:[377:java/lang/Throwable  ], 347:[377:java/lang/Throwable  ], 348:[377:java/lang/Throwable  ], 349:[377:java/lang/Throwable  ], 352:[377:java/lang/Throwable  ], 356:[377:java/lang/Throwable  ], 359:[377:java/lang/Throwable  ], 361:[377:java/lang/Throwable  ], 365:[377:java/lang/Throwable  ], 368:[377:java/lang/Throwable  ], 373:[377:java/lang/Throwable  ], 375:[377:java/lang/Throwable  ], 379:[377:java/lang/Throwable  ], 409:[491:java/lang/Throwable  ], 412:[491:java/lang/Throwable  ], 416:[491:java/lang/Throwable  ], 419:[491:java/lang/Throwable  ], 421:[491:java/lang/Throwable  ], 423:[491:java/lang/Throwable  ], 424:[491:java/lang/Throwable  ], 429:[491:java/lang/Throwable  ], 431:[491:java/lang/Throwable  ], 432:[491:java/lang/Throwable  ], 435:[491:java/lang/Throwable  ], 438:[491:java/lang/Throwable  ], 439:[491:java/lang/Throwable  ], 442:[491:java/lang/Throwable  ], 446:[491:java/lang/Throwable  ], 451:[491:java/lang/Throwable  ], 452:[491:java/lang/Throwable  ], 455:[491:java/lang/Throwable  ], 460:[491:java/lang/Throwable  ], 463:[491:java/lang/Throwable  ], 473:[491:java/lang/Throwable  ], 478:[491:java/lang/Throwable  ], 481:[491:java/lang/Throwable  ], 483:[491:java/lang/Throwable  ], 484:[491:java/lang/Throwable  ], 485:[491:java/lang/Throwable  ], 486:[491:java/lang/Throwable  ], 489:[491:java/lang/Throwable  ], 493:[491:java/lang/Throwable  ]]
 ALOAD 1
 INVOKESTATIC local/lito381/server/Protocol.selectorOnlySpellRequest([B)Llocal/lito381/server/Protocol$SpellSelector;
 ASTORE 3
```

### `useFoeSlayerSpell([BLlocal/lito381/server/BattleSpellCatalog$FoeSlayerSpell;)V`

- 類別：`local/lito381/server/ClientSession`
- 類型：例外表涵蓋、含迴圈（路徑比較器不支援）；事件後繼：`LAYOUT_ONLY`
- 比較器：`method useFoeSlayerSpell([BLlocal/lito381/server/BattleSpellCatalog$FoeSlayerSpell;)V: left=113:[258:java/lang/Throwable  ] right=116:[263:java/lang/Throwable  ]; paths rejected: normal control-flow cycle; labelled CFGs differ`

```diff
@@ -1,5 +1,5 @@
 META 2 null [java/io/IOException]
-TRY [113:[258:java/lang/Throwable  ], 116:[258:java/lang/Throwable  ], 118:[258:java/lang/Throwable  ], 119:[258:java/lang/Throwable  ], 121:[258:java/lang/Throwable  ], 122:[258:java/lang/Throwable  ], 123:[258:java/lang/Throwable  ], 124:[258:java/lang/Throwable  ], 127:[258:java/lang/Throwable  ], 131:[258:java/lang/Throwable  ], 134:[258:java/lang/Throwable  ], 137:[258:java/lang/Throwable  ], 141:[258:java/lang/Throwable  ], 147:[258:java/lang/Throwable  ], 151:[258:java/lang/Throwable  ], 156:[258:java/lang/Throwable  ], 161:[258:java/lang/Throwable  ], 164:[258:java/lang/Throwable  ], 177:[258:java/lang/Throwable  ], 179:[258:java/lang/Throwable  ], 181:[258:java/lang/Throwable  ], 183:[258:java/lang/Throwable  ], 185:[258:java/lang/Throwable  ], 190:[258:java/lang/Throwable  ], 191:[258:java/lang/Throwable  ], 192:[258:java/lang/Throwable  ], 194:[258:java/lang/Throwable  ], 197:[258:java/lang/Throwable  ], 199:[258:java/lang/Throwable  ], 205:[258:java/lang/Throwable  ], 209:[258:java/lang/Throwable  ], 217:[258:java/lang/Throwable  ], 218:[258:java/lang/Throwable  ], 219:[258:java/lang/Throwable  ], 222:[258:java/lang/Throwable  ], 224:[258:java/lang/Throwable  ], 226:[258:java/lang/Throwable  ], 230:[258:java/lang/Throwable  ], 234:[258:java/lang/Throwable  ], 236:[258:java/lang/Throwable  ], 237:[258:java/lang/Throwable  ], 240:[258:java/lang/Throwable  ], 243:[258:java/lang/Throwable  ], 248:[258:java/lang/Throwable  ], 250:[258:java/lang/Throwable  ], 253:[258:java/lang/Throwable  ], 256:[258:java/lang/Throwable  ], 260:[258:java/lang/Throwable  ], 444:[456:java/lang/Throwable  ], 448:[456:java/lang/Throwable  ], 449:[456:java/lang/Throwable  ]]
+TRY [116:[263:java/lang/Throwable  ], 119:[263:java/lang/Throwable  ], 121:[263:java/lang/Throwable  ], 122:[263:java/lang/Throwable  ], 124:[263:java/lang/Throwable  ], 125:[263:java/lang/Throwable  ], 126:[263:java/lang/Throwable  ], 127:[263:java/lang/Throwable  ], 130:[263:java/lang/Throwable  ], 134:[263:java/lang/Throwable  ], 137:[263:java/lang/Throwable  ], 140:[263:java/lang/Throwable  ], 144:[263:java/lang/Throwable  ], 150:[263:java/lang/Throwable  ], 154:[263:java/lang/Throwable  ], 159:[263:java/lang/Throwable  ], 164:[263:java/lang/Throwable  ], 167:[263:java/lang/Throwable  ], 180:[263:java/lang/Throwable  ], 182:[263:java/lang/Throwable  ], 184:[263:java/lang/Throwable  ], 186:[263:java/lang/Throwable  ], 188:[263:java/lang/Throwable  ], 193:[263:java/lang/Throwable  ], 194:[263:java/lang/Throwable  ], 195:[263:java/lang/Throwable  ], 197:[263:java/lang/Throwable  ], 200:[263:java/lang/Throwable  ], 202:[263:java/lang/Throwable  ], 208:[263:java/lang/Throwable  ], 212:[263:java/lang/Throwable  ], 220:[263:java/lang/Throwable  ], 221:[263:java/lang/Throwable  ], 222:[263:java/lang/Throwable  ], 225:[263:java/lang/Throwable  ], 227:[263:java/lang/Throwable  ], 229:[263:java/lang/Throwable  ], 232:[263:java/lang/Throwable  ], 236:[263:java/lang/Throwable  ], 239:[263:java/lang/Throwable  ], 242:[263:java/lang/Throwable  ], 245:[263:java/lang/Throwable  ], 248:[263:java/lang/Throwable  ], 253:[263:java/lang/Throwable  ], 255:[263:java/lang/Throwable  ], 258:[263:java/lang/Throwable  ], 261:[263:java/lang/Throwable  ], 265:[263:java/lang/Throwable  ], 454:[466:java/lang/Throwable  ], 458:[466:java/lang/Throwable  ], 459:[466:java/lang/Throwable  ]]
 CONST null
 ASTORE 3
 ALOAD 1
```

### `useGuardBrakeSpell([BLlocal/lito381/server/BattleSpellCatalog$GuardBrakeSpell;)V`

- 類別：`local/lito381/server/ClientSession`
- 類型：例外表涵蓋、含迴圈（路徑比較器不支援）；事件後繼：`LAYOUT_ONLY`
- 比較器：`method useGuardBrakeSpell([BLlocal/lito381/server/BattleSpellCatalog$GuardBrakeSpell;)V: left=162:[167:java/lang/Exception  ] right=164:[169:java/lang/Exception  ]; paths rejected: normal control-flow cycle; labelled CFGs differ`

```diff
@@ -1,5 +1,5 @@
 META 2 null [java/io/IOException]
-TRY [162:[167:java/lang/Exception  ], 164:[167:java/lang/Exception  ], 371:[515:java/lang/Throwable  ], 374:[515:java/lang/Throwable  ], 376:[515:java/lang/Throwable  ], 377:[515:java/lang/Throwable  ], 379:[515:java/lang/Throwable  ], 380:[515:java/lang/Throwable  ], 381:[515:java/lang/Throwable  ], 382:[515:java/lang/Throwable  ], 385:[515:java/lang/Throwable  ], 389:[515:java/lang/Throwable  ], 392:[515:java/lang/Throwable  ], 395:[515:java/lang/Throwable  ], 399:[515:java/lang/Throwable  ], 405:[515:java/lang/Throwable  ], 409:[515:java/lang/Throwable  ], 414:[515:java/lang/Throwable  ], 419:[515:java/lang/Throwable  ], 422:[515:java/lang/Throwable  ], 435:[515:java/lang/Throwable  ], 438:[515:java/lang/Throwable  ], 440:[515:java/lang/Throwable  ], 442:[515:java/lang/Throwable  ], 448:[515:java/lang/Throwable  ], 450:[515:java/lang/Throwable  ], 451:[515:java/lang/Throwable  ], 453:[515:java/lang/Throwable  ], 454:[515:java/lang/Throwable  ], 455:[515:java/lang/Throwable  ], 457:[515:java/lang/Throwable  ], 460:[515:java/lang/Throwable  ], 462:[515:java/lang/Throwable  ], 468:[515:java/lang/Throwable  ], 477:[515:java/lang/Throwable  ], 478:[515:java/lang/Throwable  ], 479:[515:java/lang/Throwable  ], 482:[515:java/lang/Throwable  ], 484:[515:java/lang/Throwable  ], 486:[515:java/lang/Throwable  ], 490:[515:java/lang/Throwable  ], 493:[515:java/lang/Throwable  ], 494:[515:java/lang/Throwable  ], 497:[515:java/lang/Throwable  ], 500:[515:java/lang/Throwable  ], 505:[515:java/lang/Throwable  ], 507:[515:java/lang/Throwable  ], 510:[515:java/lang/Throwable  ], 513:[515:java/lang/Throwable  ], 517:[515:java/lang/Throwable  ]]
+TRY [164:[169:java/lang/Exception  ], 166:[169:java/lang/Exception  ], 373:[519:java/lang/Throwable  ], 376:[519:java/lang/Throwable  ], 378:[519:java/lang/Throwable  ], 379:[519:java/lang/Throwable  ], 381:[519:java/lang/Throwable  ], 382:[519:java/lang/Throwable  ], 383:[519:java/lang/Throwable  ], 384:[519:java/lang/Throwable  ], 387:[519:java/lang/Throwable  ], 391:[519:java/lang/Throwable  ], 394:[519:java/lang/Throwable  ], 397:[519:java/lang/Throwable  ], 401:[519:java/lang/Throwable  ], 407:[519:java/lang/Throwable  ], 411:[519:java/lang/Throwable  ], 416:[519:java/lang/Throwable  ], 421:[519:java/lang/Throwable  ], 424:[519:java/lang/Throwable  ], 437:[519:java/lang/Throwable  ], 440:[519:java/lang/Throwable  ], 442:[519:java/lang/Throwable  ], 444:[519:java/lang/Throwable  ], 450:[519:java/lang/Throwable  ], 452:[519:java/lang/Throwable  ], 453:[519:java/lang/Throwable  ], 455:[519:java/lang/Throwable  ], 456:[519:java/lang/Throwable  ], 457:[519:java/lang/Throwable  ], 459:[519:java/lang/Throwable  ], 462:[519:java/lang/Throwable  ], 464:[519:java/lang/Throwable  ], 470:[519:java/lang/Throwable  ], 479:[519:java/lang/Throwable  ], 480:[519:java/lang/Throwable  ], 481:[519:java/lang/Throwable  ], 484:[519:java/lang/Throwable  ], 486:[519:java/lang/Throwable  ], 488:[519:java/lang/Throwable  ], 491:[519:java/lang/Throwable  ], 495:[519:java/lang/Throwable  ], 498:[519:java/lang/Throwable  ], 501:[519:java/lang/Throwable  ], 504:[519:java/lang/Throwable  ], 509:[519:java/lang/Throwable  ], 511:[519:java/lang/Throwable  ], 514:[519:java/lang/Throwable  ], 517:[519:java/lang/Throwable  ], 521:[519:java/lang/Throwable  ]]
 ALOAD 1
 INVOKESTATIC local/lito381/server/Protocol.selectorOnlySpellRequest([B)Llocal/lito381/server/Protocol$SpellSelector;
 ASTORE 3
```

### `useHasteSpell([BLlocal/lito381/server/BattleSpellCatalog$HasteSpell;)V`

- 類別：`local/lito381/server/ClientSession`
- 類型：例外表涵蓋、含迴圈（路徑比較器不支援）；事件後繼：`LAYOUT_ONLY`
- 比較器：`method useHasteSpell([BLlocal/lito381/server/BattleSpellCatalog$HasteSpell;)V: left=482:[612:java/lang/Throwable  ] right=485:[615:java/lang/Throwable  ]; paths rejected: normal control-flow cycle; labelled CFGs differ`

```diff
@@ -1,5 +1,5 @@
 META 2 null [java/io/IOException]
-TRY [482:[612:java/lang/Throwable  ], 484:[601:java/lang/Throwable  , 612:java/lang/Throwable  ], 486:[601:java/lang/Throwable  , 612:java/lang/Throwable  ], 487:[601:java/lang/Throwable  , 612:java/lang/Throwable  ], 489:[601:java/lang/Throwable  , 612:java/lang/Throwable  ], 490:[601:java/lang/Throwable  , 612:java/lang/Throwable  ], 491:[601:java/lang/Throwable  , 612:java/lang/Throwable  ], 492:[601:java/lang/Throwable  , 612:java/lang/Throwable  ], 499:[601:java/lang/Throwable  , 612:java/lang/Throwable  ], 503:[601:java/lang/Throwable  , 612:java/lang/Throwable  ], 507:[601:java/lang/Throwable  , 612:java/lang/Throwable  ], 510:[601:java/lang/Throwable  , 612:java/lang/Throwable  ], 513:[601:java/lang/Throwable  , 612:java/lang/Throwable  ], 517:[601:java/lang/Throwable  , 612:java/lang/Throwable  ], 520:[601:java/lang/Throwable  , 612:java/lang/Throwable  ], 529:[601:java/lang/Throwable  , 612:java/lang/Throwable  ], 532:[601:java/lang/Throwable  , 612:java/lang/Throwable  ], 534:[601:java/lang/Throwable  , 612:java/lang/Throwable  ], 536:[601:java/lang/Throwable  , 612:java/lang/Throwable  ], 538:[601:java/lang/Throwable  , 612:java/lang/Throwable  ], 539:[601:java/lang/Throwable  , 612:java/lang/Throwable  ], 541:[601:java/lang/Throwable  , 612:java/lang/Throwable  ], 543:[601:java/lang/Throwable  , 612:java/lang/Throwable  ], 545:[601:java/lang/Throwable  , 612:java/lang/Throwable  ], 551:[601:java/lang/Throwable  , 612:java/lang/Throwable  ], 552:[601:java/lang/Throwable  , 612:java/lang/Throwable  ], 553:[601:java/lang/Throwable  , 612:java/lang/Throwable  ], 557:[601:java/lang/Throwable  , 612:java/lang/Throwable  ], 558:[601:java/lang/Throwable  , 612:java/lang/Throwable  ], 559:[601:java/lang/Throwable  , 612:java/lang/Throwable  ], 561:[601:java/lang/Throwable  , 612:java/lang/Throwable  ], 562:[601:java/lang/Throwable  , 612:java/lang/Throwable  ], 565:[601:java/lang/Throwable  , 612:java/lang/Throwable  ], 568:[601:java/lang/Throwable  , 612:java/lang/Throwable  ], 570:[601:java/lang/Throwable  , 612:java/lang/Throwable  ], 571:[601:java/lang/Throwable  , 612:java/lang/Throwable  ], 576:[601:java/lang/Throwable  , 612:java/lang/Throwable  ], 579:[601:java/lang/Throwable  , 612:java/lang/Throwable  ], 580:[601:java/lang/Throwable  , 612:java/lang/Throwable  ], 582:[601:java/lang/Throwable  , 612:java/lang/Throwable  ], 586:[601:java/lang/Throwable  , 612:java/lang/Throwable  ], 589:[601:java/lang/Throwable  , 612:java/lang/Throwable  ], 594:[601:java/lang/Throwable  , 612:java/lang/Throwable  ], 596:[601:java/lang/Throwable  , 612:java/lang/Throwable  ], 599:[601:java/lang/Throwable  , 612:java/lang/Throwable  ], 603:[601:java/lang/Throwable  , 612:java/lang/Throwable  ], 605:[612:java/lang/Throwable  ], 607:[612:java/lang/Throwable  ], 610:[612:java/lang/Throwable  ], 614:[612:java/lang/Throwable  ], 625:[739:java/lang/Throwable  ], 627:[739:java/lang/Throwable  ], 628:[739:java/lang/Throwable  ], 630:[739:java/lang/Throwable  ], 631:[739:java/lang/Throwable  ], 632:[739:java/lang/Throwable  ], 633:[739:java/lang/Throwable  ], 640:[739:java/lang/Throwable  ], 644:[739:java/lang/Throwable  ], 648:[739:java/lang/Throwable  ], 651:[739:java/lang/Throwable  ], 654:[739:java/lang/Throwable  ], 661:[739:java/lang/Throwable  ], 663:[739:java/lang/Throwable  ], 665:[739:java/lang/Throwable  ], 667:[739:java/lang/Throwable  ], 673:[739:java/lang/Throwable  ], 674:[739:java/lang/Throwable  ], 675:[739:java/lang/Throwable  ], 677:[739:java/lang/Throwable  ], 680:[739:java/lang/Throwable  ], 683:[739:java/lang/Throwable  ], 686:[739:java/lang/Throwable  ], 689:[739:java/lang/Throwable  ], 691:[739:java/lang/Throwable  ], 693:[739:java/lang/Throwable  ], 695:[739:java/lang/Throwable  ], 696:[739:java/lang/Throwable  ], 701:[739:java/lang/Throwable  ], 703:[739:java/lang/Throwable  ], 704:[739:java/lang/Throwable  ], 707:[739:java/lang/Throwable  ], 711:[739:java/lang/Throwable  ], 712:[739:java/lang/Throwable  ], 713:[739:java/lang/Throwable  ], 717:[739:java/lang/Throwable  ], 720:[739:java/lang/Throwable  ], 721:[739:java/lang/Throwable  ], 723:[739:java/lang/Throwable  ], 727:[739:java/lang/Throwable  ], 730:[739:java/lang/Throwable  ], 735:[739:java/lang/Throwable  ], 737:[739:java/lang/Throwable  ], 741:[739:java/lang/Throwable  ], 754:[860:java/lang/Throwable  ], 756:[860:java/lang/Throwable  ], 757:[860:java/lang/Throwable  ], 759:[860:java/lang/Throwable  ], 760:[860:java/lang/Throwable  ], 761:[860:java/lang/Throwable  ], 762:[860:java/lang/Throwable  ], 765:[860:java/lang/Throwable  ], 769:[860:java/lang/Throwable  ], 770:[860:java/lang/Throwable  ], 777:[860:java/lang/Throwable  ], 781:[860:java/lang/Throwable  ], 782:[860:java/lang/Throwable  ], 785:[860:java/lang/Throwable  ], 788:[860:java/lang/Throwable  ], 791:[860:java/lang/Throwable  ], 795:[860:java/lang/Throwable  ], 801:[860:java/lang/Throwable  ], 804:[860:java/lang/Throwable  ], 807:[860:java/lang/Throwable  ], 809:[860:java/lang/Throwable  ], 811:[860:java/lang/Throwable  ], 813:[860:java/lang/Throwable  ], 814:[860:java/lang/Throwable  ], 817:[860:java/lang/Throwable  ], 821:[860:java/lang/Throwable  ], 824:[860:java/lang/Throwable  ], 825:[860:java/lang/Throwable  ], 828:[860:java/lang/Throwable  ], 832:[860:java/lang/Throwable  ], 833:[860:java/lang/Throwable  ], 834:[860:java/lang/Throwable  ], 838:[860:java/lang/Throwable  ], 841:[860:java/lang/Throwable  ], 842:[860:java/lang/Throwable  ], 844:[860:java/lang/Throwable  ], 848:[860:java/lang/Throwable  ], 851:[860:java/lang/Throwable  ], 856:[860:java/lang/Throwable  ], 858:[860:java/lang/Throwable  ], 862:[860:java/lang/Throwable  ], 876:[957:java/lang/Throwable  ], 878:[957:java/lang/Throwable  ], 879:[957:java/lang/Throwable  ], 881:[957:java/lang/Throwable  ], 882:[957:java/lang/Throwable  ], 883:[957:java/lang/Throwable  ], 884:[957:java/lang/Throwable  ], 888:[957:java/lang/Throwable  ], 892:[957:java/lang/Throwable  ], 893:[957:java/lang/Throwable  ], 896:[957:java/lang/Throwable  ], 899:[957:java/lang/Throwable  ], 902:[957:java/lang/Throwable  ], 904:[957:java/lang/Throwable  ], 906:[957:java/lang/Throwable  ], 908:[957:java/lang/Throwable  ], 911:[957:java/lang/Throwable  ], 912:[957:java/lang/Throwable  ], 916:[957:java/lang/Throwable  ], 918:[957:java/lang/Throwable  ], 919:[957:java/lang/Throwable  ], 921:[957:java/lang/Throwable  ], 923:[957:java/lang/Throwable  ], 927:[957:java/lang/Throwable  ], 928:[957:java/lang/Throwable  ], 929:[957:java/lang/Throwable  ], 933:[957:java/lang/Throwable  ], 936:[957:java/lang/Throwable  ], 937:[957:java/lang/Throwable  ], 939:[957:java/lang/Throwable  ], 943:[957:java/lang/Throwable  ], 946:[957:java/lang/Throwable  ], 951:[957:java/lang/Throwable  ], 955:[957:java/lang/Throwable  ], 959:[957:java/lang/Throwable  ], 968:[1042:java/lang/Throwable  ], 970:[1042:java/lang/Throwable  ], 971:[1042:java/lang/Throwable  ], 973:[1042:java/lang/Throwable  ], 974:[1042:java/lang/Throwable  ], 975:[1042:java/lang/Throwable  ], 976:[1042:java/lang/Throwable  ], 983:[1042:java/lang/Throwable  ], 987:[1042:java/lang/Throwable  ], 988:[1042:java/lang/Throwable  ], 991:[1042:java/lang/Throwable  ], 994:[1042:java/lang/Throwable  ], 996:[1042:java/lang/Throwable  ], 998:[1042:java/lang/Throwable  ], 1000:[1042:java/lang/Throwable  ], 1006:[1042:java/lang/Throwable  ], 1007:[1042:java/lang/Throwable  ], 1008:[1042:java/lang/Throwable  ], 1010:[1042:java/lang/Throwable  ], 1014:[1042:java/lang/Throwable  ], 1015:[1042:java/lang/Throwable  ], 1016:[1042:java/lang/Throwable  ], 1020:[1042:java/lang/Throwable  ], 1023:[1042:java/lang/Throwable  ], 1024:[1042:java/lang/Throwable  ], 1026:[1042:java/lang/Throwable  ], 1030:[1042:java/lang/Throwable  ], 1033:[1042:java/lang/Throwable  ], 1038:[1042:java/lang/Throwable  ], 1040:[1042:java/lang/Throwable  ], 1044:[1042:java/lang/Throwable  ]]
+TRY [485:[615:java/lang/Throwable  ], 487:[607:java/lang/Throwable  , 615:java/lang/Throwable  ], 489:[607:java/lang/Throwable  , 615:java/lang/Throwable  ], 490:[607:java/lang/Throwable  , 615:java/lang/Throwable  ], 492:[607:java/lang/Throwable  , 615:java/lang/Throwable  ], 493:[607:java/lang/Throwable  , 615:java/lang/Throwable  ], 494:[607:java/lang/Throwable  , 615:java/lang/Throwable  ], 495:[607:java/lang/Throwable  , 615:java/lang/Throwable  ], 502:[607:java/lang/Throwable  , 615:java/lang/Throwable  ], 506:[607:java/lang/Throwable  , 615:java/lang/Throwable  ], 510:[607:java/lang/Throwable  , 615:java/lang/Throwable  ], 513:[607:java/lang/Throwable  , 615:java/lang/Throwable  ], 516:[607:java/lang/Throwable  , 615:java/lang/Throwable  ], 520:[607:java/lang/Throwable  , 615:java/lang/Throwable  ], 523:[607:java/lang/Throwable  , 615:java/lang/Throwable  ], 532:[607:java/lang/Throwable  , 615:java/lang/Throwable  ], 535:[607:java/lang/Throwable  , 615:java/lang/Throwable  ], 537:[607:java/lang/Throwable  , 615:java/lang/Throwable  ], 539:[607:java/lang/Throwable  , 615:java/lang/Throwable  ], 541:[607:java/lang/Throwable  , 615:java/lang/Throwable  ], 542:[607:java/lang/Throwable  , 615:java/lang/Throwable  ], 544:[607:java/lang/Throwable  , 615:java/lang/Throwable  ], 546:[607:java/lang/Throwable  , 615:java/lang/Throwable  ], 548:[607:java/lang/Throwable  , 615:java/lang/Throwable  ], 554:[607:java/lang/Throwable  , 615:java/lang/Throwable  ], 555:[607:java/lang/Throwable  , 615:java/lang/Throwable  ], 556:[607:java/lang/Throwable  , 615:java/lang/Throwable  ], 558:[607:java/lang/Throwable  , 615:java/lang/Throwable  ], 560:[615:java/lang/Throwable  ], 564:[607:java/lang/Throwable  , 615:java/lang/Throwable  ], 565:[607:java/lang/Throwable  , 615:java/lang/Throwable  ], 566:[607:java/lang/Throwable  , 615:java/lang/Throwable  ], 568:[607:java/lang/Throwable  , 615:java/lang/Throwable  ], 569:[607:java/lang/Throwable  , 615:java/lang/Throwable  ], 572:[607:java/lang/Throwable  , 615:java/lang/Throwable  ], 575:[607:java/lang/Throwable  , 615:java/lang/Throwable  ], 577:[607:java/lang/Throwable  , 615:java/lang/Throwable  ], 578:[607:java/lang/Throwable  , 615:java/lang/Throwable  ], 582:[607:java/lang/Throwable  , 615:java/lang/Throwable  ], 586:[607:java/lang/Throwable  , 615:java/lang/Throwable  ], 589:[607:java/lang/Throwable  , 615:java/lang/Throwable  ], 591:[607:java/lang/Throwable  , 615:java/lang/Throwable  ], 595:[607:java/lang/Throwable  , 615:java/lang/Throwable  ], 598:[607:java/lang/Throwable  , 615:java/lang/Throwable  ], 603:[607:java/lang/Throwable  , 615:java/lang/Throwable  ], 605:[607:java/lang/Throwable  , 615:java/lang/Throwable  ], 609:[607:java/lang/Throwable  , 615:java/lang/Throwable  ], 611:[615:java/lang/Throwable  ], 613:[615:java/lang/Throwable  ], 617:[615:java/lang/Throwable  ], 628:[744:java/lang/Throwable  ], 630:[744:java/lang/Throwable  ], 631:[744:java/lang/Throwable  ], 633:[744:java/lang/Throwable  ], 634:[744:java/lang/Throwable  ], 635:[744:java/lang/Throwable  ], 636:[744:java/lang/Throwable  ], 643:[744:java/lang/Throwable  ], 647:[744:java/lang/Throwable  ], 651:[744:java/lang/Throwable  ], 654:[744:java/lang/Throwable  ], 657:[744:java/lang/Throwable  ], 664:[744:java/lang/Throwable  ], 666:[744:java/lang/Throwable  ], 668:[744:java/lang/Throwable  ], 670:[744:java/lang/Throwable  ], 676:[744:java/lang/Throwable  ], 677:[744:java/lang/Throwable  ], 678:[744:java/lang/Throwable  ], 680:[744:java/lang/Throwable  ], 683:[744:java/lang/Throwable  ], 686:[744:java/lang/Throwable  ], 689:[744:java/lang/Throwable  ], 692:[744:java/lang/Throwable  ], 694:[744:java/lang/Throwable  ], 696:[744:java/lang/Throwable  ], 698:[744:java/lang/Throwable  ], 699:[744:java/lang/Throwable  ], 704:[744:java/lang/Throwable  ], 706:[744:java/lang/Throwable  ], 707:[744:java/lang/Throwable  ], 710:[744:java/lang/Throwable  ], 714:[744:java/lang/Throwable  ], 715:[744:java/lang/Throwable  ], 716:[744:java/lang/Throwable  ], 719:[744:java/lang/Throwable  ], 723:[744:java/lang/Throwable  ], 726:[744:java/lang/Throwable  ], 728:[744:java/lang/Throwable  ], 732:[744:java/lang/Throwable  ], 735:[744:java/lang/Throwable  ], 740:[744:java/lang/Throwable  ], 742:[744:java/lang/Throwable  ], 746:[744:java/lang/Throwable  ], 759:[867:java/lang/Throwable  ], 761:[867:java/lang/Throwable  ], 762:[867:java/lang/Throwable  ], 764:[867:java/lang/Throwable  ], 765:[867:java/lang/Throwable  ], 766:[867:java/lang/Throwable  ], 767:[867:java/lang/Throwable  ], 770:[867:java/lang/Throwable  ], 774:[867:java/lang/Throwable  ], 775:[867:java/lang/Throwable  ], 782:[867:java/lang/Throwable  ], 786:[867:java/lang/Throwable  ], 787:[867:java/lang/Throwable  ], 790:[867:java/lang/Throwable  ], 793:[867:java/lang/Throwable  ], 796:[867:java/lang/Throwable  ], 800:[867:java/lang/Throwable  ], 806:[867:java/lang/Throwable  ], 809:[867:java/lang/Throwable  ], 812:[867:java/lang/Throwable  ], 814:[867:java/lang/Throwable  ], 816:[867:java/lang/Throwable  ], 818:[867:java/lang/Throwable  ], 819:[867:java/lang/Throwable  ], 822:[867:java/lang/Throwable  ], 826:[867:java/lang/Throwable  ], 829:[867:java/lang/Throwable  ], 830:[867:java/lang/Throwable  ], 833:[867:java/lang/Throwable  ], 837:[867:java/lang/Throwable  ], 838:[867:java/lang/Throwable  ], 839:[867:java/lang/Throwable  ], 842:[867:java/lang/Throwable  ], 846:[867:java/lang/Throwable  ], 849:[867:java/lang/Throwable  ], 851:[867:java/lang/Throwable  ], 855:[867:java/lang/Throwable  ], 858:[867:java/lang/Throwable  ], 863:[867:java/lang/Throwable  ], 865:[867:java/lang/Throwable  ], 869:[867:java/lang/Throwable  ], 883:[966:java/lang/Throwable  ], 885:[966:java/lang/Throwable  ], 886:[966:java/lang/Throwable  ], 888:[966:java/lang/Throwable  ], 889:[966:java/lang/Throwable  ], 890:[966:java/lang/Throwable  ], 891:[966:java/lang/Throwable  ], 895:[966:java/lang/Throwable  ], 899:[966:java/lang/Throwable  ], 900:[966:java/lang/Throwable  ], 903:[966:java/lang/Throwable  ], 906:[966:java/lang/Throwable  ], 909:[966:java/lang/Throwable  ], 911:[966:java/lang/Throwable  ], 913:[966:java/lang/Throwable  ], 915:[966:java/lang/Throwable  ], 918:[966:java/lang/Throwable  ], 919:[966:java/lang/Throwable  ], 923:[966:java/lang/Throwable  ], 925:[966:java/lang/Throwable  ], 926:[966:java/lang/Throwable  ], 928:[966:java/lang/Throwable  ], 930:[966:java/lang/Throwable  ], 934:[966:java/lang/Throwable  ], 935:[966:java/lang/Throwable  ], 936:[966:java/lang/Throwable  ], 939:[966:java/lang/Throwable  ], 943:[966:java/lang/Throwable  ], 946:[966:java/lang/Throwable  ], 948:[966:java/lang/Throwable  ], 952:[966:java/lang/Throwable  ], 955:[966:java/lang/Throwable  ], 960:[966:java/lang/Throwable  ], 964:[966:java/lang/Throwable  ], 968:[966:java/lang/Throwable  ], 977:[1053:java/lang/Throwable  ], 979:[1053:java/lang/Throwable  ], 980:[1053:java/lang/Throwable  ], 982:[1053:java/lang/Throwable  ], 983:[1053:java/lang/Throwable  ], 984:[1053:java/lang/Throwable  ], 985:[1053:java/lang/Throwable  ], 992:[1053:java/lang/Throwable  ], 996:[1053:java/lang/Throwable  ], 997:[1053:java/lang/Throwable  ], 1000:[1053:java/lang/Throwable  ], 1003:[1053:java/lang/Throwable  ], 1005:[1053:java/lang/Throwable  ], 1007:[1053:java/lang/Throwable  ], 1009:[1053:java/lang/Throwable  ], 1015:[1053:java/lang/Throwable  ], 1016:[1053:java/lang/Throwable  ], 1017:[1053:java/lang/Throwable  ], 1019:[1053:java/lang/Throwable  ], 1023:[1053:java/lang/Throwable  ], 1024:[1053:java/lang/Throwable  ], 1025:[1053:java/lang/Throwable  ], 1028:[1053:java/lang/Throwable  ], 1032:[1053:java/lang/Throwable  ], 1035:[1053:java/lang/Throwable  ], 1037:[1053:java/lang/Throwable  ], 1041:[1053:java/lang/Throwable  ], 1044:[1053:java/lang/Throwable  ], 1049:[1053:java/lang/Throwable  ], 1051:[1053:java/lang/Throwable  ], 1055:[1053:java/lang/Throwable  ]]
 ALOAD 1
 INVOKESTATIC local/lito381/server/Protocol.objectSpellRequest([B)Llocal/lito381/server/Protocol$ObjectSpellRequest;
 ALOAD 0
```

### `useIllusionAvatarSpell([BLlocal/lito381/server/BattleSpellCatalog$IllusionAvatarSpell;)V`

- 類別：`local/lito381/server/ClientSession`
- 類型：例外表涵蓋、含迴圈（路徑比較器不支援）；事件後繼：`LAYOUT_ONLY`
- 比較器：`method useIllusionAvatarSpell([BLlocal/lito381/server/BattleSpellCatalog$IllusionAvatarSpell;)V: left=140:[391:java/lang/Throwable  ] right=143:[395:java/lang/Throwable  ]; paths rejected: normal control-flow cycle; labelled CFGs differ`

```diff
@@ -1,5 +1,5 @@
 META 2 null [java/io/IOException]
-TRY [140:[391:java/lang/Throwable  ], 142:[380:java/lang/Throwable  , 391:java/lang/Throwable  ], 144:[380:java/lang/Throwable  , 391:java/lang/Throwable  ], 145:[380:java/lang/Throwable  , 391:java/lang/Throwable  ], 147:[380:java/lang/Throwable  , 391:java/lang/Throwable  ], 148:[380:java/lang/Throwable  , 391:java/lang/Throwable  ], 149:[380:java/lang/Throwable  , 391:java/lang/Throwable  ], 150:[380:java/lang/Throwable  , 391:java/lang/Throwable  ], 153:[380:java/lang/Throwable  , 391:java/lang/Throwable  ], 156:[380:java/lang/Throwable  , 391:java/lang/Throwable  ], 157:[380:java/lang/Throwable  , 391:java/lang/Throwable  ], 160:[380:java/lang/Throwable  , 391:java/lang/Throwable  ], 165:[380:java/lang/Throwable  , 391:java/lang/Throwable  ], 168:[380:java/lang/Throwable  , 391:java/lang/Throwable  ], 172:[380:java/lang/Throwable  , 391:java/lang/Throwable  ], 173:[380:java/lang/Throwable  , 391:java/lang/Throwable  ], 176:[380:java/lang/Throwable  , 391:java/lang/Throwable  ], 181:[380:java/lang/Throwable  , 391:java/lang/Throwable  ], 184:[380:java/lang/Throwable  , 391:java/lang/Throwable  ], 192:[380:java/lang/Throwable  , 391:java/lang/Throwable  ], 193:[380:java/lang/Throwable  , 391:java/lang/Throwable  ], 196:[380:java/lang/Throwable  , 391:java/lang/Throwable  ], 200:[380:java/lang/Throwable  , 391:java/lang/Throwable  ], 204:[380:java/lang/Throwable  , 391:java/lang/Throwable  ], 205:[380:java/lang/Throwable  , 391:java/lang/Throwable  ], 208:[380:java/lang/Throwable  , 391:java/lang/Throwable  ], 212:[380:java/lang/Throwable  , 391:java/lang/Throwable  ], 223:[380:java/lang/Throwable  , 391:java/lang/Throwable  ], 226:[380:java/lang/Throwable  , 391:java/lang/Throwable  ], 230:[380:java/lang/Throwable  , 391:java/lang/Throwable  ], 232:[380:java/lang/Throwable  , 391:java/lang/Throwable  ], 234:[380:java/lang/Throwable  , 391:java/lang/Throwable  ], 235:[380:java/lang/Throwable  , 391:java/lang/Throwable  ], 239:[380:java/lang/Throwable  , 391:java/lang/Throwable  ], 243:[380:java/lang/Throwable  , 391:java/lang/Throwable  ], 245:[380:java/lang/Throwable  , 391:java/lang/Throwable  ], 248:[380:java/lang/Throwable  , 391:java/lang/Throwable  ], 249:[380:java/lang/Throwable  , 391:java/lang/Throwable  ], 259:[380:java/lang/Throwable  , 391:java/lang/Throwable  ], 262:[380:java/lang/Throwable  , 391:java/lang/Throwable  ], 265:[380:java/lang/Throwable  , 391:java/lang/Throwable  ], 268:[380:java/lang/Throwable  , 391:java/lang/Throwable  ], 270:[380:java/lang/Throwable  , 391:java/lang/Throwable  ], 275:[380:java/lang/Throwable  , 391:java/lang/Throwable  ], 277:[380:java/lang/Throwable  , 391:java/lang/Throwable  ], 278:[380:java/lang/Throwable  , 391:java/lang/Throwable  ], 280:[380:java/lang/Throwable  , 391:java/lang/Throwable  ], 295:[380:java/lang/Throwable  , 391:java/lang/Throwable  ], 298:[380:java/lang/Throwable  , 391:java/lang/Throwable  ], 302:[380:java/lang/Throwable  , 391:java/lang/Throwable  ], 304:[380:java/lang/Throwable  , 391:java/lang/Throwable  ], 306:[380:java/lang/Throwable  , 391:java/lang/Throwable  ], 311:[380:java/lang/Throwable  , 391:java/lang/Throwable  ], 313:[380:java/lang/Throwable  , 391:java/lang/Throwable  ], 314:[380:java/lang/Throwable  , 391:java/lang/Throwable  ], 315:[380:java/lang/Throwable  , 391:java/lang/Throwable  ], 317:[380:java/lang/Throwable  , 391:java/lang/Throwable  ], 319:[391:java/lang/Throwable  ], 322:[380:java/lang/Throwable  , 391:java/lang/Throwable  ], 327:[380:java/lang/Throwable  , 391:java/lang/Throwable  ], 328:[380:java/lang/Throwable  , 391:java/lang/Throwable  ], 329:[380:java/lang/Throwable  , 391:java/lang/Throwable  ], 331:[380:java/lang/Throwable  , 391:java/lang/Throwable  ], 334:[380:java/lang/Throwable  , 391:java/lang/Throwable  ], 336:[380:java/lang/Throwable  , 391:java/lang/Throwable  ], 338:[380:java/lang/Throwable  , 391:java/lang/Throwable  ], 340:[380:java/lang/Throwable  , 391:java/lang/Throwable  ], 342:[380:java/lang/Throwable  , 391:java/lang/Throwable  ], 343:[380:java/lang/Throwable  , 391:java/lang/Throwable  ], 348:[380:java/lang/Throwable  , 391:java/lang/Throwable  ], 351:[380:java/lang/Throwable  , 391:java/lang/Throwable  ], 352:[380:java/lang/Throwable  , 391:java/lang/Throwable  ], 354:[380:java/lang/Throwable  , 391:java/lang/Throwable  ], 358:[380:java/lang/Throwable  , 391:java/lang/Throwable  ], 361:[380:java/lang/Throwable  , 391:java/lang/Throwable  ], 366:[380:java/lang/Throwable  , 391:java/lang/Throwable  ], 368:[380:java/lang/Throwable  , 391:java/lang/Throwable  ], 373:[380:java/lang/Throwable  , 391:java/lang/Throwable  ], 374:[380:java/lang/Throwable  , 391:java/lang/Throwable  ], 376:[380:java/lang/Throwable  , 391:java/lang/Throwable  ], 378:[380:java/lang/Throwable  , 391:java/lang/Throwable  ], 382:[380:java/lang/Throwable  , 391:java/lang/Throwable  ], 384:[391:java/lang/Throwable  ], 386:[391:java/lang/Throwable  ], 389:[391:java/lang/Throwable  ], 393:[391:java/lang/Throwable  ]]
+TRY [143:[395:java/lang/Throwable  ], 145:[387:java/lang/Throwable  , 395:java/lang/Throwable  ], 147:[387:java/lang/Throwable  , 395:java/lang/Throwable  ], 148:[387:java/lang/Throwable  , 395:java/lang/Throwable  ], 150:[387:java/lang/Throwable  , 395:java/lang/Throwable  ], 151:[387:java/lang/Throwable  , 395:java/lang/Throwable  ], 152:[387:java/lang/Throwable  , 395:java/lang/Throwable  ], 153:[387:java/lang/Throwable  , 395:java/lang/Throwable  ], 156:[387:java/lang/Throwable  , 395:java/lang/Throwable  ], 159:[387:java/lang/Throwable  , 395:java/lang/Throwable  ], 160:[387:java/lang/Throwable  , 395:java/lang/Throwable  ], 163:[387:java/lang/Throwable  , 395:java/lang/Throwable  ], 168:[387:java/lang/Throwable  , 395:java/lang/Throwable  ], 171:[387:java/lang/Throwable  , 395:java/lang/Throwable  ], 175:[387:java/lang/Throwable  , 395:java/lang/Throwable  ], 176:[387:java/lang/Throwable  , 395:java/lang/Throwable  ], 179:[387:java/lang/Throwable  , 395:java/lang/Throwable  ], 184:[387:java/lang/Throwable  , 395:java/lang/Throwable  ], 187:[387:java/lang/Throwable  , 395:java/lang/Throwable  ], 195:[387:java/lang/Throwable  , 395:java/lang/Throwable  ], 196:[387:java/lang/Throwable  , 395:java/lang/Throwable  ], 199:[387:java/lang/Throwable  , 395:java/lang/Throwable  ], 203:[387:java/lang/Throwable  , 395:java/lang/Throwable  ], 207:[387:java/lang/Throwable  , 395:java/lang/Throwable  ], 208:[387:java/lang/Throwable  , 395:java/lang/Throwable  ], 211:[387:java/lang/Throwable  , 395:java/lang/Throwable  ], 215:[387:java/lang/Throwable  , 395:java/lang/Throwable  ], 226:[387:java/lang/Throwable  , 395:java/lang/Throwable  ], 229:[387:java/lang/Throwable  , 395:java/lang/Throwable  ], 233:[387:java/lang/Throwable  , 395:java/lang/Throwable  ], 235:[387:java/lang/Throwable  , 395:java/lang/Throwable  ], 237:[387:java/lang/Throwable  , 395:java/lang/Throwable  ], 238:[387:java/lang/Throwable  , 395:java/lang/Throwable  ], 242:[387:java/lang/Throwable  , 395:java/lang/Throwable  ], 246:[387:java/lang/Throwable  , 395:java/lang/Throwable  ], 248:[387:java/lang/Throwable  , 395:java/lang/Throwable  ], 251:[387:java/lang/Throwable  , 395:java/lang/Throwable  ], 252:[387:java/lang/Throwable  , 395:java/lang/Throwable  ], 262:[387:java/lang/Throwable  , 395:java/lang/Throwable  ], 265:[387:java/lang/Throwable  , 395:java/lang/Throwable  ], 268:[387:java/lang/Throwable  , 395:java/lang/Throwable  ], 271:[387:java/lang/Throwable  , 395:java/lang/Throwable  ], 273:[387:java/lang/Throwable  , 395:java/lang/Throwable  ], 278:[387:java/lang/Throwable  , 395:java/lang/Throwable  ], 280:[387:java/lang/Throwable  , 395:java/lang/Throwable  ], 281:[387:java/lang/Throwable  , 395:java/lang/Throwable  ], 283:[387:java/lang/Throwable  , 395:java/lang/Throwable  ], 298:[387:java/lang/Throwable  , 395:java/lang/Throwable  ], 301:[387:java/lang/Throwable  , 395:java/lang/Throwable  ], 305:[387:java/lang/Throwable  , 395:java/lang/Throwable  ], 307:[387:java/lang/Throwable  , 395:java/lang/Throwable  ], 309:[387:java/lang/Throwable  , 395:java/lang/Throwable  ], 314:[387:java/lang/Throwable  , 395:java/lang/Throwable  ], 316:[387:java/lang/Throwable  , 395:java/lang/Throwable  ], 317:[387:java/lang/Throwable  , 395:java/lang/Throwable  ], 318:[387:java/lang/Throwable  , 395:java/lang/Throwable  ], 320:[387:java/lang/Throwable  , 395:java/lang/Throwable  ], 322:[395:java/lang/Throwable  ], 325:[387:java/lang/Throwable  , 395:java/lang/Throwable  ], 331:[387:java/lang/Throwable  , 395:java/lang/Throwable  ], 332:[387:java/lang/Throwable  , 395:java/lang/Throwable  ], 334:[387:java/lang/Throwable  , 395:java/lang/Throwable  ], 336:[387:java/lang/Throwable  , 395:java/lang/Throwable  ], 338:[395:java/lang/Throwable  ], 342:[387:java/lang/Throwable  , 395:java/lang/Throwable  ], 343:[387:java/lang/Throwable  , 395:java/lang/Throwable  ], 344:[387:java/lang/Throwable  , 395:java/lang/Throwable  ], 346:[387:java/lang/Throwable  , 395:java/lang/Throwable  ], 349:[387:java/lang/Throwable  , 395:java/lang/Throwable  ], 351:[387:java/lang/Throwable  , 395:java/lang/Throwable  ], 353:[387:java/lang/Throwable  , 395:java/lang/Throwable  ], 355:[387:java/lang/Throwable  , 395:java/lang/Throwable  ], 357:[387:java/lang/Throwable  , 395:java/lang/Throwable  ], 358:[387:java/lang/Throwable  , 395:java/lang/Throwable  ], 362:[387:java/lang/Throwable  , 395:java/lang/Throwable  ], 366:[387:java/lang/Throwable  , 395:java/lang/Throwable  ], 369:[387:java/lang/Throwable  , 395:java/lang/Throwable  ], 371:[387:java/lang/Throwable  , 395:java/lang/Throwable  ], 375:[387:java/lang/Throwable  , 395:java/lang/Throwable  ], 378:[387:java/lang/Throwable  , 395:java/lang/Throwable  ], 383:[387:java/lang/Throwable  , 395:java/lang/Throwable  ], 385:[387:java/lang/Throwable  , 395:java/lang/Throwable  ], 389:[387:java/lang/Throwable  , 395:java/lang/Throwable  ], 391:[395:java/lang/Throwable  ], 393:[395:java/lang/Throwable  ], 397:[395:java/lang/Throwable  ]]
 ALOAD 1
 INVOKESTATIC local/lito381/server/Protocol.selectorOnlySpellRequest([B)Llocal/lito381/server/Protocol$SpellSelector;
 ASTORE 3
```

### `useIllusionLichSpell([BLlocal/lito381/server/BattleSpellCatalog$IllusionLichSpell;)V`

- 類別：`local/lito381/server/ClientSession`
- 類型：例外表涵蓋、含迴圈（路徑比較器不支援）；事件後繼：`LAYOUT_ONLY`
- 比較器：`method useIllusionLichSpell([BLlocal/lito381/server/BattleSpellCatalog$IllusionLichSpell;)V: left=147:[358:java/lang/Throwable  ] right=143:[352:java/lang/Throwable  ]; paths rejected: normal control-flow cycle; labelled CFGs differ`

```diff
@@ -1,5 +1,5 @@
 META 2 null [java/io/IOException]
-TRY [147:[358:java/lang/Throwable  ], 149:[350:java/lang/Throwable  , 358:java/lang/Throwable  ], 151:[350:java/lang/Throwable  , 358:java/lang/Throwable  ], 152:[350:java/lang/Throwable  , 358:java/lang/Throwable  ], 154:[350:java/lang/Throwable  , 358:java/lang/Throwable  ], 155:[350:java/lang/Throwable  , 358:java/lang/Throwable  ], 156:[350:java/lang/Throwable  , 358:java/lang/Throwable  ], 157:[350:java/lang/Throwable  , 358:java/lang/Throwable  ], 160:[350:java/lang/Throwable  , 358:java/lang/Throwable  ], 163:[350:java/lang/Throwable  , 358:java/lang/Throwable  ], 164:[350:java/lang/Throwable  , 358:java/lang/Throwable  ], 167:[350:java/lang/Throwable  , 358:java/lang/Throwable  ], 172:[350:java/lang/Throwable  , 358:java/lang/Throwable  ], 173:[350:java/lang/Throwable  , 358:java/lang/Throwable  ], 176:[350:java/lang/Throwable  , 358:java/lang/Throwable  ], 185:[350:java/lang/Throwable  , 358:java/lang/Throwable  ], 186:[350:java/lang/Throwable  , 358:java/lang/Throwable  ], 189:[350:java/lang/Throwable  , 358:java/lang/Throwable  ], 193:[350:java/lang/Throwable  , 358:java/lang/Throwable  ], 197:[350:java/lang/Throwable  , 358:java/lang/Throwable  ], 198:[350:java/lang/Throwable  , 358:java/lang/Throwable  ], 201:[350:java/lang/Throwable  , 358:java/lang/Throwable  ], 205:[350:java/lang/Throwable  , 358:java/lang/Throwable  ], 216:[350:java/lang/Throwable  , 358:java/lang/Throwable  ], 219:[350:java/lang/Throwable  , 358:java/lang/Throwable  ], 222:[350:java/lang/Throwable  , 358:java/lang/Throwable  ], 225:[350:java/lang/Throwable  , 358:java/lang/Throwable  ], 227:[350:java/lang/Throwable  , 358:java/lang/Throwable  ], 232:[350:java/lang/Throwable  , 358:java/lang/Throwable  ], 234:[350:java/lang/Throwable  , 358:java/lang/Throwable  ], 235:[350:java/lang/Throwable  , 358:java/lang/Throwable  ], 237:[350:java/lang/Throwable  , 358:java/lang/Throwable  ], 240:[350:java/lang/Throwable  , 358:java/lang/Throwable  ], 243:[350:java/lang/Throwable  , 358:java/lang/Throwable  ], 247:[350:java/lang/Throwable  , 358:java/lang/Throwable  ], 249:[350:java/lang/Throwable  , 358:java/lang/Throwable  ], 251:[350:java/lang/Throwable  , 358:java/lang/Throwable  ], 252:[350:java/lang/Throwable  , 358:java/lang/Throwable  ], 267:[350:java/lang/Throwable  , 358:java/lang/Throwable  ], 270:[350:java/lang/Throwable  , 358:java/lang/Throwable  ], 274:[350:java/lang/Throwable  , 358:java/lang/Throwable  ], 276:[350:java/lang/Throwable  , 358:java/lang/Throwable  ], 278:[350:java/lang/Throwable  , 358:java/lang/Throwable  ], 283:[350:java/lang/Throwable  , 358:java/lang/Throwable  ], 285:[350:java/lang/Throwable  , 358:java/lang/Throwable  ], 286:[350:java/lang/Throwable  , 358:java/lang/Throwable  ], 287:[350:java/lang/Throwable  , 358:java/lang/Throwable  ], 289:[350:java/lang/Throwable  , 358:java/lang/Throwable  ], 291:[358:java/lang/Throwable  ], 294:[350:java/lang/Throwable  , 358:java/lang/Throwable  ], 300:[350:java/lang/Throwable  , 358:java/lang/Throwable  ], 301:[350:java/lang/Throwable  , 358:java/lang/Throwable  ], 303:[350:java/lang/Throwable  , 358:java/lang/Throwable  ], 305:[350:java/lang/Throwable  , 358:java/lang/Throwable  ], 307:[358:java/lang/Throwable  ], 311:[350:java/lang/Throwable  , 358:java/lang/Throwable  ], 312:[350:java/lang/Throwable  , 358:java/lang/Throwable  ], 313:[350:java/lang/Throwable  , 358:java/lang/Throwable  ], 315:[350:java/lang/Throwable  , 358:java/lang/Throwable  ], 318:[350:java/lang/Throwable  , 358:java/lang/Throwable  ], 320:[350:java/lang/Throwable  , 358:java/lang/Throwable  ], 321:[350:java/lang/Throwable  , 358:java/lang/Throwable  ], 328:[350:java/lang/Throwable  , 358:java/lang/Throwable  ], 331:[350:java/lang/Throwable  , 358:java/lang/Throwable  ], 332:[350:java/lang/Throwable  , 358:java/lang/Throwable  ], 334:[350:java/lang/Throwable  , 358:java/lang/Throwable  ], 338:[350:java/lang/Throwable  , 358:java/lang/Throwable  ], 341:[350:java/lang/Throwable  , 358:java/lang/Throwable  ], 346:[350:java/lang/Throwable  , 358:java/lang/Throwable  ], 348:[350:java/lang/Throwable  , 358:java/lang/Throwable  ], 352:[350:java/lang/Throwable  , 358:java/lang/Throwable  ], 354:[358:java/lang/Throwable  ], 356:[358:java/lang/Throwable  ], 360:[358:java/lang/Throwable  ]]
+TRY [143:[352:java/lang/Throwable  ], 145:[344:java/lang/Throwable  , 352:java/lang/Throwable  ], 147:[344:java/lang/Throwable  , 352:java/lang/Throwable  ], 148:[344:java/lang/Throwable  , 352:java/lang/Throwable  ], 150:[344:java/lang/Throwable  , 352:java/lang/Throwable  ], 151:[344:java/lang/Throwable  , 352:java/lang/Throwable  ], 152:[344:java/lang/Throwable  , 352:java/lang/Throwable  ], 153:[344:java/lang/Throwable  , 352:java/lang/Throwable  ], 156:[344:java/lang/Throwable  , 352:java/lang/Throwable  ], 159:[344:java/lang/Throwable  , 352:java/lang/Throwable  ], 160:[344:java/lang/Throwable  , 352:java/lang/Throwable  ], 163:[344:java/lang/Throwable  , 352:java/lang/Throwable  ], 168:[344:java/lang/Throwable  , 352:java/lang/Throwable  ], 169:[344:java/lang/Throwable  , 352:java/lang/Throwable  ], 172:[344:java/lang/Throwable  , 352:java/lang/Throwable  ], 181:[344:java/lang/Throwable  , 352:java/lang/Throwable  ], 182:[344:java/lang/Throwable  , 352:java/lang/Throwable  ], 185:[344:java/lang/Throwable  , 352:java/lang/Throwable  ], 189:[344:java/lang/Throwable  , 352:java/lang/Throwable  ], 193:[344:java/lang/Throwable  , 352:java/lang/Throwable  ], 194:[344:java/lang/Throwable  , 352:java/lang/Throwable  ], 197:[344:java/lang/Throwable  , 352:java/lang/Throwable  ], 201:[344:java/lang/Throwable  , 352:java/lang/Throwable  ], 212:[344:java/lang/Throwable  , 352:java/lang/Throwable  ], 215:[344:java/lang/Throwable  , 352:java/lang/Throwable  ], 218:[344:java/lang/Throwable  , 352:java/lang/Throwable  ], 221:[344:java/lang/Throwable  , 352:java/lang/Throwable  ], 223:[344:java/lang/Throwable  , 352:java/lang/Throwable  ], 228:[344:java/lang/Throwable  , 352:java/lang/Throwable  ], 230:[344:java/lang/Throwable  , 352:java/lang/Throwable  ], 231:[344:java/lang/Throwable  , 352:java/lang/Throwable  ], 233:[344:java/lang/Throwable  , 352:java/lang/Throwable  ], 236:[344:java/lang/Throwable  , 352:java/lang/Throwable  ], 239:[344:java/lang/Throwable  , 352:java/lang/Throwable  ], 243:[344:java/lang/Throwable  , 352:java/lang/Throwable  ], 245:[344:java/lang/Throwable  , 352:java/lang/Throwable  ], 247:[344:java/lang/Throwable  , 352:java/lang/Throwable  ], 248:[344:java/lang/Throwable  , 352:java/lang/Throwable  ], 261:[344:java/lang/Throwable  , 352:java/lang/Throwable  ], 264:[344:java/lang/Throwable  , 352:java/lang/Throwable  ], 268:[344:java/lang/Throwable  , 352:java/lang/Throwable  ], 270:[344:java/lang/Throwable  , 352:java/lang/Throwable  ], 272:[344:java/lang/Throwable  , 352:java/lang/Throwable  ], 277:[344:java/lang/Throwable  , 352:java/lang/Throwable  ], 279:[344:java/lang/Throwable  , 352:java/lang/Throwable  ], 280:[344:java/lang/Throwable  , 352:java/lang/Throwable  ], 281:[344:java/lang/Throwable  , 352:java/lang/Throwable  ], 283:[344:java/lang/Throwable  , 352:java/lang/Throwable  ], 285:[352:java/lang/Throwable  ], 288:[344:java/lang/Throwable  , 352:java/lang/Throwable  ], 294:[344:java/lang/Throwable  , 352:java/lang/Throwable  ], 295:[344:java/lang/Throwable  , 352:java/lang/Throwable  ], 297:[344:java/lang/Throwable  , 352:java/lang/Throwable  ], 299:[344:java/lang/Throwable  , 352:java/lang/Throwable  ], 301:[352:java/lang/Throwable  ], 305:[344:java/lang/Throwable  , 352:java/lang/Throwable  ], 306:[344:java/lang/Throwable  , 352:java/lang/Throwable  ], 307:[344:java/lang/Throwable  , 352:java/lang/Throwable  ], 309:[344:java/lang/Throwable  , 352:java/lang/Throwable  ], 312:[344:java/lang/Throwable  , 352:java/lang/Throwable  ], 314:[344:java/lang/Throwable  , 352:java/lang/Throwable  ], 315:[344:java/lang/Throwable  , 352:java/lang/Throwable  ], 319:[344:java/lang/Throwable  , 352:java/lang/Throwable  ], 323:[344:java/lang/Throwable  , 352:java/lang/Throwable  ], 326:[344:java/lang/Throwable  , 352:java/lang/Throwable  ], 328:[344:java/lang/Throwable  , 352:java/lang/Throwable  ], 332:[344:java/lang/Throwable  , 352:java/lang/Throwable  ], 335:[344:java/lang/Throwable  , 352:java/lang/Throwable  ], 340:[344:java/lang/Throwable  , 352:java/lang/Throwable  ], 342:[344:java/lang/Throwable  , 352:java/lang/Throwable  ], 346:[344:java/lang/Throwable  , 352:java/lang/Throwable  ], 348:[352:java/lang/Throwable  ], 350:[352:java/lang/Throwable  ], 354:[352:java/lang/Throwable  ]]
 ALOAD 1
 INVOKESTATIC local/lito381/server/Protocol.selectorOnlySpellRequest([B)Llocal/lito381/server/Protocol$SpellSelector;
 ASTORE 3
```

### `useIllusionOgreSpell([BLlocal/lito381/server/BattleSpellCatalog$IllusionOgreSpell;)V`

- 類別：`local/lito381/server/ClientSession`
- 類型：例外表涵蓋、含迴圈（路徑比較器不支援）；事件後繼：`LAYOUT_ONLY`
- 比較器：`method useIllusionOgreSpell([BLlocal/lito381/server/BattleSpellCatalog$IllusionOgreSpell;)V: left=147:[360:java/lang/Throwable  ] right=143:[354:java/lang/Throwable  ]; paths rejected: normal control-flow cycle; labelled CFGs differ`

```diff
@@ -1,5 +1,5 @@
 META 2 null [java/io/IOException]
-TRY [147:[360:java/lang/Throwable  ], 149:[352:java/lang/Throwable  , 360:java/lang/Throwable  ], 151:[352:java/lang/Throwable  , 360:java/lang/Throwable  ], 152:[352:java/lang/Throwable  , 360:java/lang/Throwable  ], 154:[352:java/lang/Throwable  , 360:java/lang/Throwable  ], 155:[352:java/lang/Throwable  , 360:java/lang/Throwable  ], 156:[352:java/lang/Throwable  , 360:java/lang/Throwable  ], 157:[352:java/lang/Throwable  , 360:java/lang/Throwable  ], 160:[352:java/lang/Throwable  , 360:java/lang/Throwable  ], 163:[352:java/lang/Throwable  , 360:java/lang/Throwable  ], 164:[352:java/lang/Throwable  , 360:java/lang/Throwable  ], 167:[352:java/lang/Throwable  , 360:java/lang/Throwable  ], 172:[352:java/lang/Throwable  , 360:java/lang/Throwable  ], 173:[352:java/lang/Throwable  , 360:java/lang/Throwable  ], 176:[352:java/lang/Throwable  , 360:java/lang/Throwable  ], 185:[352:java/lang/Throwable  , 360:java/lang/Throwable  ], 186:[352:java/lang/Throwable  , 360:java/lang/Throwable  ], 189:[352:java/lang/Throwable  , 360:java/lang/Throwable  ], 193:[352:java/lang/Throwable  , 360:java/lang/Throwable  ], 197:[352:java/lang/Throwable  , 360:java/lang/Throwable  ], 198:[352:java/lang/Throwable  , 360:java/lang/Throwable  ], 201:[352:java/lang/Throwable  , 360:java/lang/Throwable  ], 205:[352:java/lang/Throwable  , 360:java/lang/Throwable  ], 216:[352:java/lang/Throwable  , 360:java/lang/Throwable  ], 219:[352:java/lang/Throwable  , 360:java/lang/Throwable  ], 222:[352:java/lang/Throwable  , 360:java/lang/Throwable  ], 225:[352:java/lang/Throwable  , 360:java/lang/Throwable  ], 227:[352:java/lang/Throwable  , 360:java/lang/Throwable  ], 232:[352:java/lang/Throwable  , 360:java/lang/Throwable  ], 234:[352:java/lang/Throwable  , 360:java/lang/Throwable  ], 235:[352:java/lang/Throwable  , 360:java/lang/Throwable  ], 237:[352:java/lang/Throwable  , 360:java/lang/Throwable  ], 240:[352:java/lang/Throwable  , 360:java/lang/Throwable  ], 243:[352:java/lang/Throwable  , 360:java/lang/Throwable  ], 247:[352:java/lang/Throwable  , 360:java/lang/Throwable  ], 249:[352:java/lang/Throwable  , 360:java/lang/Throwable  ], 251:[352:java/lang/Throwable  , 360:java/lang/Throwable  ], 252:[352:java/lang/Throwable  , 360:java/lang/Throwable  ], 267:[352:java/lang/Throwable  , 360:java/lang/Throwable  ], 270:[352:java/lang/Throwable  , 360:java/lang/Throwable  ], 274:[352:java/lang/Throwable  , 360:java/lang/Throwable  ], 276:[352:java/lang/Throwable  , 360:java/lang/Throwable  ], 278:[352:java/lang/Throwable  , 360:java/lang/Throwable  ], 283:[352:java/lang/Throwable  , 360:java/lang/Throwable  ], 285:[352:java/lang/Throwable  , 360:java/lang/Throwable  ], 286:[352:java/lang/Throwable  , 360:java/lang/Throwable  ], 287:[352:java/lang/Throwable  , 360:java/lang/Throwable  ], 289:[352:java/lang/Throwable  , 360:java/lang/Throwable  ], 291:[360:java/lang/Throwable  ], 294:[352:java/lang/Throwable  , 360:java/lang/Throwable  ], 300:[352:java/lang/Throwable  , 360:java/lang/Throwable  ], 301:[352:java/lang/Throwable  , 360:java/lang/Throwable  ], 303:[352:java/lang/Throwable  , 360:java/lang/Throwable  ], 305:[352:java/lang/Throwable  , 360:java/lang/Throwable  ], 307:[360:java/lang/Throwable  ], 311:[352:java/lang/Throwable  , 360:java/lang/Throwable  ], 312:[352:java/lang/Throwable  , 360:java/lang/Throwable  ], 313:[352:java/lang/Throwable  , 360:java/lang/Throwable  ], 315:[352:java/lang/Throwable  , 360:java/lang/Throwable  ], 318:[352:java/lang/Throwable  , 360:java/lang/Throwable  ], 320:[352:java/lang/Throwable  , 360:java/lang/Throwable  ], 322:[352:java/lang/Throwable  , 360:java/lang/Throwable  ], 323:[352:java/lang/Throwable  , 360:java/lang/Throwable  ], 330:[352:java/lang/Throwable  , 360:java/lang/Throwable  ], 333:[352:java/lang/Throwable  , 360:java/lang/Throwable  ], 334:[352:java/lang/Throwable  , 360:java/lang/Throwable  ], 336:[352:java/lang/Throwable  , 360:java/lang/Throwable  ], 340:[352:java/lang/Throwable  , 360:java/lang/Throwable  ], 343:[352:java/lang/Throwable  , 360:java/lang/Throwable  ], 348:[352:java/lang/Throwable  , 360:java/lang/Throwable  ], 350:[352:java/lang/Throwable  , 360:java/lang/Throwable  ], 354:[352:java/lang/Throwable  , 360:java/lang/Throwable  ], 356:[360:java/lang/Throwable  ], 358:[360:java/lang/Throwable  ], 362:[360:java/lang/Throwable  ]]
+TRY [143:[354:java/lang/Throwable  ], 145:[346:java/lang/Throwable  , 354:java/lang/Throwable  ], 147:[346:java/lang/Throwable  , 354:java/lang/Throwable  ], 148:[346:java/lang/Throwable  , 354:java/lang/Throwable  ], 150:[346:java/lang/Throwable  , 354:java/lang/Throwable  ], 151:[346:java/lang/Throwable  , 354:java/lang/Throwable  ], 152:[346:java/lang/Throwable  , 354:java/lang/Throwable  ], 153:[346:java/lang/Throwable  , 354:java/lang/Throwable  ], 156:[346:java/lang/Throwable  , 354:java/lang/Throwable  ], 159:[346:java/lang/Throwable  , 354:java/lang/Throwable  ], 160:[346:java/lang/Throwable  , 354:java/lang/Throwable  ], 163:[346:java/lang/Throwable  , 354:java/lang/Throwable  ], 168:[346:java/lang/Throwable  , 354:java/lang/Throwable  ], 169:[346:java/lang/Throwable  , 354:java/lang/Throwable  ], 172:[346:java/lang/Throwable  , 354:java/lang/Throwable  ], 181:[346:java/lang/Throwable  , 354:java/lang/Throwable  ], 182:[346:java/lang/Throwable  , 354:java/lang/Throwable  ], 185:[346:java/lang/Throwable  , 354:java/lang/Throwable  ], 189:[346:java/lang/Throwable  , 354:java/lang/Throwable  ], 193:[346:java/lang/Throwable  , 354:java/lang/Throwable  ], 194:[346:java/lang/Throwable  , 354:java/lang/Throwable  ], 197:[346:java/lang/Throwable  , 354:java/lang/Throwable  ], 201:[346:java/lang/Throwable  , 354:java/lang/Throwable  ], 212:[346:java/lang/Throwable  , 354:java/lang/Throwable  ], 215:[346:java/lang/Throwable  , 354:java/lang/Throwable  ], 218:[346:java/lang/Throwable  , 354:java/lang/Throwable  ], 221:[346:java/lang/Throwable  , 354:java/lang/Throwable  ], 223:[346:java/lang/Throwable  , 354:java/lang/Throwable  ], 228:[346:java/lang/Throwable  , 354:java/lang/Throwable  ], 230:[346:java/lang/Throwable  , 354:java/lang/Throwable  ], 231:[346:java/lang/Throwable  , 354:java/lang/Throwable  ], 233:[346:java/lang/Throwable  , 354:java/lang/Throwable  ], 236:[346:java/lang/Throwable  , 354:java/lang/Throwable  ], 239:[346:java/lang/Throwable  , 354:java/lang/Throwable  ], 243:[346:java/lang/Throwable  , 354:java/lang/Throwable  ], 245:[346:java/lang/Throwable  , 354:java/lang/Throwable  ], 247:[346:java/lang/Throwable  , 354:java/lang/Throwable  ], 248:[346:java/lang/Throwable  , 354:java/lang/Throwable  ], 261:[346:java/lang/Throwable  , 354:java/lang/Throwable  ], 264:[346:java/lang/Throwable  , 354:java/lang/Throwable  ], 268:[346:java/lang/Throwable  , 354:java/lang/Throwable  ], 270:[346:java/lang/Throwable  , 354:java/lang/Throwable  ], 272:[346:java/lang/Throwable  , 354:java/lang/Throwable  ], 277:[346:java/lang/Throwable  , 354:java/lang/Throwable  ], 279:[346:java/lang/Throwable  , 354:java/lang/Throwable  ], 280:[346:java/lang/Throwable  , 354:java/lang/Throwable  ], 281:[346:java/lang/Throwable  , 354:java/lang/Throwable  ], 283:[346:java/lang/Throwable  , 354:java/lang/Throwable  ], 285:[354:java/lang/Throwable  ], 288:[346:java/lang/Throwable  , 354:java/lang/Throwable  ], 294:[346:java/lang/Throwable  , 354:java/lang/Throwable  ], 295:[346:java/lang/Throwable  , 354:java/lang/Throwable  ], 297:[346:java/lang/Throwable  , 354:java/lang/Throwable  ], 299:[346:java/lang/Throwable  , 354:java/lang/Throwable  ], 301:[354:java/lang/Throwable  ], 305:[346:java/lang/Throwable  , 354:java/lang/Throwable  ], 306:[346:java/lang/Throwable  , 354:java/lang/Throwable  ], 307:[346:java/lang/Throwable  , 354:java/lang/Throwable  ], 309:[346:java/lang/Throwable  , 354:java/lang/Throwable  ], 312:[346:java/lang/Throwable  , 354:java/lang/Throwable  ], 314:[346:java/lang/Throwable  , 354:java/lang/Throwable  ], 316:[346:java/lang/Throwable  , 354:java/lang/Throwable  ], 317:[346:java/lang/Throwable  , 354:java/lang/Throwable  ], 321:[346:java/lang/Throwable  , 354:java/lang/Throwable  ], 325:[346:java/lang/Throwable  , 354:java/lang/Throwable  ], 328:[346:java/lang/Throwable  , 354:java/lang/Throwable  ], 330:[346:java/lang/Throwable  , 354:java/lang/Throwable  ], 334:[346:java/lang/Throwable  , 354:java/lang/Throwable  ], 337:[346:java/lang/Throwable  , 354:java/lang/Throwable  ], 342:[346:java/lang/Throwable  , 354:java/lang/Throwable  ], 344:[346:java/lang/Throwable  , 354:java/lang/Throwable  ], 348:[346:java/lang/Throwable  , 354:java/lang/Throwable  ], 350:[354:java/lang/Throwable  ], 352:[354:java/lang/Throwable  ], 356:[354:java/lang/Throwable  ]]
 ALOAD 1
 INVOKESTATIC local/lito381/server/Protocol.selectorOnlySpellRequest([B)Llocal/lito381/server/Protocol$SpellSelector;
 ASTORE 3
```

### `useInsightSpell([BLlocal/lito381/server/BattleSpellCatalog$InsightSpell;)V`

- 類別：`local/lito381/server/ClientSession`
- 類型：例外表涵蓋、含迴圈（路徑比較器不支援）；事件後繼：`LAYOUT_ONLY`
- 比較器：`method useInsightSpell([BLlocal/lito381/server/BattleSpellCatalog$InsightSpell;)V: left=147:[358:java/lang/Throwable  ] right=143:[352:java/lang/Throwable  ]; paths rejected: normal control-flow cycle; labelled CFGs differ`

```diff
@@ -1,5 +1,5 @@
 META 2 null [java/io/IOException]
-TRY [147:[358:java/lang/Throwable  ], 149:[350:java/lang/Throwable  , 358:java/lang/Throwable  ], 151:[350:java/lang/Throwable  , 358:java/lang/Throwable  ], 152:[350:java/lang/Throwable  , 358:java/lang/Throwable  ], 154:[350:java/lang/Throwable  , 358:java/lang/Throwable  ], 155:[350:java/lang/Throwable  , 358:java/lang/Throwable  ], 156:[350:java/lang/Throwable  , 358:java/lang/Throwable  ], 157:[350:java/lang/Throwable  , 358:java/lang/Throwable  ], 160:[350:java/lang/Throwable  , 358:java/lang/Throwable  ], 163:[350:java/lang/Throwable  , 358:java/lang/Throwable  ], 164:[350:java/lang/Throwable  , 358:java/lang/Throwable  ], 167:[350:java/lang/Throwable  , 358:java/lang/Throwable  ], 172:[350:java/lang/Throwable  , 358:java/lang/Throwable  ], 173:[350:java/lang/Throwable  , 358:java/lang/Throwable  ], 176:[350:java/lang/Throwable  , 358:java/lang/Throwable  ], 185:[350:java/lang/Throwable  , 358:java/lang/Throwable  ], 186:[350:java/lang/Throwable  , 358:java/lang/Throwable  ], 189:[350:java/lang/Throwable  , 358:java/lang/Throwable  ], 193:[350:java/lang/Throwable  , 358:java/lang/Throwable  ], 197:[350:java/lang/Throwable  , 358:java/lang/Throwable  ], 198:[350:java/lang/Throwable  , 358:java/lang/Throwable  ], 201:[350:java/lang/Throwable  , 358:java/lang/Throwable  ], 205:[350:java/lang/Throwable  , 358:java/lang/Throwable  ], 216:[350:java/lang/Throwable  , 358:java/lang/Throwable  ], 219:[350:java/lang/Throwable  , 358:java/lang/Throwable  ], 222:[350:java/lang/Throwable  , 358:java/lang/Throwable  ], 225:[350:java/lang/Throwable  , 358:java/lang/Throwable  ], 227:[350:java/lang/Throwable  , 358:java/lang/Throwable  ], 232:[350:java/lang/Throwable  , 358:java/lang/Throwable  ], 234:[350:java/lang/Throwable  , 358:java/lang/Throwable  ], 235:[350:java/lang/Throwable  , 358:java/lang/Throwable  ], 237:[350:java/lang/Throwable  , 358:java/lang/Throwable  ], 240:[350:java/lang/Throwable  , 358:java/lang/Throwable  ], 243:[350:java/lang/Throwable  , 358:java/lang/Throwable  ], 247:[350:java/lang/Throwable  , 358:java/lang/Throwable  ], 249:[350:java/lang/Throwable  , 358:java/lang/Throwable  ], 251:[350:java/lang/Throwable  , 358:java/lang/Throwable  ], 252:[350:java/lang/Throwable  , 358:java/lang/Throwable  ], 267:[350:java/lang/Throwable  , 358:java/lang/Throwable  ], 270:[350:java/lang/Throwable  , 358:java/lang/Throwable  ], 274:[350:java/lang/Throwable  , 358:java/lang/Throwable  ], 276:[350:java/lang/Throwable  , 358:java/lang/Throwable  ], 278:[350:java/lang/Throwable  , 358:java/lang/Throwable  ], 283:[350:java/lang/Throwable  , 358:java/lang/Throwable  ], 285:[350:java/lang/Throwable  , 358:java/lang/Throwable  ], 286:[350:java/lang/Throwable  , 358:java/lang/Throwable  ], 287:[350:java/lang/Throwable  , 358:java/lang/Throwable  ], 289:[350:java/lang/Throwable  , 358:java/lang/Throwable  ], 291:[358:java/lang/Throwable  ], 294:[350:java/lang/Throwable  , 358:java/lang/Throwable  ], 300:[350:java/lang/Throwable  , 358:java/lang/Throwable  ], 301:[350:java/lang/Throwable  , 358:java/lang/Throwable  ], 303:[350:java/lang/Throwable  , 358:java/lang/Throwable  ], 305:[350:java/lang/Throwable  , 358:java/lang/Throwable  ], 307:[358:java/lang/Throwable  ], 311:[350:java/lang/Throwable  , 358:java/lang/Throwable  ], 312:[350:java/lang/Throwable  , 358:java/lang/Throwable  ], 313:[350:java/lang/Throwable  , 358:java/lang/Throwable  ], 315:[350:java/lang/Throwable  , 358:java/lang/Throwable  ], 318:[350:java/lang/Throwable  , 358:java/lang/Throwable  ], 320:[350:java/lang/Throwable  , 358:java/lang/Throwable  ], 321:[350:java/lang/Throwable  , 358:java/lang/Throwable  ], 328:[350:java/lang/Throwable  , 358:java/lang/Throwable  ], 331:[350:java/lang/Throwable  , 358:java/lang/Throwable  ], 332:[350:java/lang/Throwable  , 358:java/lang/Throwable  ], 334:[350:java/lang/Throwable  , 358:java/lang/Throwable  ], 338:[350:java/lang/Throwable  , 358:java/lang/Throwable  ], 341:[350:java/lang/Throwable  , 358:java/lang/Throwable  ], 346:[350:java/lang/Throwable  , 358:java/lang/Throwable  ], 348:[350:java/lang/Throwable  , 358:java/lang/Throwable  ], 352:[350:java/lang/Throwable  , 358:java/lang/Throwable  ], 354:[358:java/lang/Throwable  ], 356:[358:java/lang/Throwable  ], 360:[358:java/lang/Throwable  ]]
+TRY [143:[352:java/lang/Throwable  ], 145:[344:java/lang/Throwable  , 352:java/lang/Throwable  ], 147:[344:java/lang/Throwable  , 352:java/lang/Throwable  ], 148:[344:java/lang/Throwable  , 352:java/lang/Throwable  ], 150:[344:java/lang/Throwable  , 352:java/lang/Throwable  ], 151:[344:java/lang/Throwable  , 352:java/lang/Throwable  ], 152:[344:java/lang/Throwable  , 352:java/lang/Throwable  ], 153:[344:java/lang/Throwable  , 352:java/lang/Throwable  ], 156:[344:java/lang/Throwable  , 352:java/lang/Throwable  ], 159:[344:java/lang/Throwable  , 352:java/lang/Throwable  ], 160:[344:java/lang/Throwable  , 352:java/lang/Throwable  ], 163:[344:java/lang/Throwable  , 352:java/lang/Throwable  ], 168:[344:java/lang/Throwable  , 352:java/lang/Throwable  ], 169:[344:java/lang/Throwable  , 352:java/lang/Throwable  ], 172:[344:java/lang/Throwable  , 352:java/lang/Throwable  ], 181:[344:java/lang/Throwable  , 352:java/lang/Throwable  ], 182:[344:java/lang/Throwable  , 352:java/lang/Throwable  ], 185:[344:java/lang/Throwable  , 352:java/lang/Throwable  ], 189:[344:java/lang/Throwable  , 352:java/lang/Throwable  ], 193:[344:java/lang/Throwable  , 352:java/lang/Throwable  ], 194:[344:java/lang/Throwable  , 352:java/lang/Throwable  ], 197:[344:java/lang/Throwable  , 352:java/lang/Throwable  ], 201:[344:java/lang/Throwable  , 352:java/lang/Throwable  ], 212:[344:java/lang/Throwable  , 352:java/lang/Throwable  ], 215:[344:java/lang/Throwable  , 352:java/lang/Throwable  ], 218:[344:java/lang/Throwable  , 352:java/lang/Throwable  ], 221:[344:java/lang/Throwable  , 352:java/lang/Throwable  ], 223:[344:java/lang/Throwable  , 352:java/lang/Throwable  ], 228:[344:java/lang/Throwable  , 352:java/lang/Throwable  ], 230:[344:java/lang/Throwable  , 352:java/lang/Throwable  ], 231:[344:java/lang/Throwable  , 352:java/lang/Throwable  ], 233:[344:java/lang/Throwable  , 352:java/lang/Throwable  ], 236:[344:java/lang/Throwable  , 352:java/lang/Throwable  ], 239:[344:java/lang/Throwable  , 352:java/lang/Throwable  ], 243:[344:java/lang/Throwable  , 352:java/lang/Throwable  ], 245:[344:java/lang/Throwable  , 352:java/lang/Throwable  ], 247:[344:java/lang/Throwable  , 352:java/lang/Throwable  ], 248:[344:java/lang/Throwable  , 352:java/lang/Throwable  ], 261:[344:java/lang/Throwable  , 352:java/lang/Throwable  ], 264:[344:java/lang/Throwable  , 352:java/lang/Throwable  ], 268:[344:java/lang/Throwable  , 352:java/lang/Throwable  ], 270:[344:java/lang/Throwable  , 352:java/lang/Throwable  ], 272:[344:java/lang/Throwable  , 352:java/lang/Throwable  ], 277:[344:java/lang/Throwable  , 352:java/lang/Throwable  ], 279:[344:java/lang/Throwable  , 352:java/lang/Throwable  ], 280:[344:java/lang/Throwable  , 352:java/lang/Throwable  ], 281:[344:java/lang/Throwable  , 352:java/lang/Throwable  ], 283:[344:java/lang/Throwable  , 352:java/lang/Throwable  ], 285:[352:java/lang/Throwable  ], 288:[344:java/lang/Throwable  , 352:java/lang/Throwable  ], 294:[344:java/lang/Throwable  , 352:java/lang/Throwable  ], 295:[344:java/lang/Throwable  , 352:java/lang/Throwable  ], 297:[344:java/lang/Throwable  , 352:java/lang/Throwable  ], 299:[344:java/lang/Throwable  , 352:java/lang/Throwable  ], 301:[352:java/lang/Throwable  ], 305:[344:java/lang/Throwable  , 352:java/lang/Throwable  ], 306:[344:java/lang/Throwable  , 352:java/lang/Throwable  ], 307:[344:java/lang/Throwable  , 352:java/lang/Throwable  ], 309:[344:java/lang/Throwable  , 352:java/lang/Throwable  ], 312:[344:java/lang/Throwable  , 352:java/lang/Throwable  ], 314:[344:java/lang/Throwable  , 352:java/lang/Throwable  ], 315:[344:java/lang/Throwable  , 352:java/lang/Throwable  ], 319:[344:java/lang/Throwable  , 352:java/lang/Throwable  ], 323:[344:java/lang/Throwable  , 352:java/lang/Throwable  ], 326:[344:java/lang/Throwable  , 352:java/lang/Throwable  ], 328:[344:java/lang/Throwable  , 352:java/lang/Throwable  ], 332:[344:java/lang/Throwable  , 352:java/lang/Throwable  ], 335:[344:java/lang/Throwable  , 352:java/lang/Throwable  ], 340:[344:java/lang/Throwable  , 352:java/lang/Throwable  ], 342:[344:java/lang/Throwable  , 352:java/lang/Throwable  ], 346:[344:java/lang/Throwable  , 352:java/lang/Throwable  ], 348:[352:java/lang/Throwable  ], 350:[352:java/lang/Throwable  ], 354:[352:java/lang/Throwable  ]]
 ALOAD 1
 INVOKESTATIC local/lito381/server/Protocol.selectorOnlySpellRequest([B)Llocal/lito381/server/Protocol$SpellSelector;
 ASTORE 3
```

### `useIronSkinSpell([BLlocal/lito381/server/BattleSpellCatalog$IronSkinSpell;)V`

- 類別：`local/lito381/server/ClientSession`
- 類型：例外表涵蓋、含迴圈（路徑比較器不支援）；事件後繼：`LAYOUT_ONLY`
- 比較器：`method useIronSkinSpell([BLlocal/lito381/server/BattleSpellCatalog$IronSkinSpell;)V: left=182:[287:java/lang/Throwable  ] right=180:[285:java/lang/Throwable  ]; paths rejected: normal control-flow cycle; labelled CFGs differ`

```diff
@@ -1,5 +1,5 @@
 META 2 null [java/io/IOException]
-TRY [182:[287:java/lang/Throwable  ], 185:[287:java/lang/Throwable  ], 187:[287:java/lang/Throwable  ], 188:[287:java/lang/Throwable  ], 190:[287:java/lang/Throwable  ], 191:[287:java/lang/Throwable  ], 192:[287:java/lang/Throwable  ], 193:[287:java/lang/Throwable  ], 200:[287:java/lang/Throwable  ], 204:[287:java/lang/Throwable  ], 207:[287:java/lang/Throwable  ], 210:[287:java/lang/Throwable  ], 214:[287:java/lang/Throwable  ], 217:[287:java/lang/Throwable  ], 222:[287:java/lang/Throwable  ], 225:[287:java/lang/Throwable  ], 229:[287:java/lang/Throwable  ], 233:[287:java/lang/Throwable  ], 239:[287:java/lang/Throwable  ], 240:[287:java/lang/Throwable  ], 242:[287:java/lang/Throwable  ], 244:[287:java/lang/Throwable  ], 248:[287:java/lang/Throwable  ], 249:[287:java/lang/Throwable  ], 250:[287:java/lang/Throwable  ], 252:[287:java/lang/Throwable  ], 255:[287:java/lang/Throwable  ], 257:[287:java/lang/Throwable  ], 258:[287:java/lang/Throwable  ], 265:[287:java/lang/Throwable  ], 268:[287:java/lang/Throwable  ], 269:[287:java/lang/Throwable  ], 271:[287:java/lang/Throwable  ], 275:[287:java/lang/Throwable  ], 278:[287:java/lang/Throwable  ], 283:[287:java/lang/Throwable  ], 285:[287:java/lang/Throwable  ], 289:[287:java/lang/Throwable  ]]
+TRY [180:[285:java/lang/Throwable  ], 183:[285:java/lang/Throwable  ], 185:[285:java/lang/Throwable  ], 186:[285:java/lang/Throwable  ], 188:[285:java/lang/Throwable  ], 189:[285:java/lang/Throwable  ], 190:[285:java/lang/Throwable  ], 191:[285:java/lang/Throwable  ], 198:[285:java/lang/Throwable  ], 202:[285:java/lang/Throwable  ], 205:[285:java/lang/Throwable  ], 208:[285:java/lang/Throwable  ], 212:[285:java/lang/Throwable  ], 215:[285:java/lang/Throwable  ], 220:[285:java/lang/Throwable  ], 223:[285:java/lang/Throwable  ], 227:[285:java/lang/Throwable  ], 231:[285:java/lang/Throwable  ], 237:[285:java/lang/Throwable  ], 238:[285:java/lang/Throwable  ], 240:[285:java/lang/Throwable  ], 242:[285:java/lang/Throwable  ], 246:[285:java/lang/Throwable  ], 247:[285:java/lang/Throwable  ], 248:[285:java/lang/Throwable  ], 250:[285:java/lang/Throwable  ], 253:[285:java/lang/Throwable  ], 255:[285:java/lang/Throwable  ], 256:[285:java/lang/Throwable  ], 260:[285:java/lang/Throwable  ], 264:[285:java/lang/Throwable  ], 267:[285:java/lang/Throwable  ], 269:[285:java/lang/Throwable  ], 273:[285:java/lang/Throwable  ], 276:[285:java/lang/Throwable  ], 281:[285:java/lang/Throwable  ], 283:[285:java/lang/Throwable  ], 287:[285:java/lang/Throwable  ]]
 ALOAD 1
 INVOKESTATIC local/lito381/server/Protocol.objectSpellRequest([B)Llocal/lito381/server/Protocol$ObjectSpellRequest;
 ALOAD 0
```

### `useItemArmorEnchantSpell([BLlocal/lito381/server/BattleSpellCatalog$ItemArmorEnchantSpell;)V`

- 類別：`local/lito381/server/ClientSession`
- 類型：例外表涵蓋、含迴圈（路徑比較器不支援）；事件後繼：`LAYOUT_ONLY`
- 比較器：`method useItemArmorEnchantSpell([BLlocal/lito381/server/BattleSpellCatalog$ItemArmorEnchantSpell;)V: left=125:[286:java/lang/Throwable  ] right=123:[284:java/lang/Throwable  ]; paths rejected: normal control-flow cycle; labelled CFGs differ`

```diff
@@ -1,5 +1,5 @@
 META 2 null [java/io/IOException]
-TRY [125:[286:java/lang/Throwable  ], 128:[286:java/lang/Throwable  ], 130:[286:java/lang/Throwable  ], 131:[286:java/lang/Throwable  ], 133:[286:java/lang/Throwable  ], 134:[286:java/lang/Throwable  ], 135:[286:java/lang/Throwable  ], 136:[286:java/lang/Throwable  ], 143:[286:java/lang/Throwable  ], 147:[286:java/lang/Throwable  ], 150:[286:java/lang/Throwable  ], 154:[286:java/lang/Throwable  ], 157:[286:java/lang/Throwable  ], 160:[286:java/lang/Throwable  ], 165:[286:java/lang/Throwable  ], 168:[286:java/lang/Throwable  ], 172:[286:java/lang/Throwable  ], 175:[286:java/lang/Throwable  ], 177:[286:java/lang/Throwable  ], 179:[286:java/lang/Throwable  ], 181:[286:java/lang/Throwable  ], 182:[286:java/lang/Throwable  ], 184:[286:java/lang/Throwable  ], 190:[286:java/lang/Throwable  ], 191:[286:java/lang/Throwable  ], 192:[286:java/lang/Throwable  ], 194:[286:java/lang/Throwable  ], 197:[286:java/lang/Throwable  ], 199:[286:java/lang/Throwable  ], 201:[286:java/lang/Throwable  ], 208:[286:java/lang/Throwable  ], 209:[286:java/lang/Throwable  ], 214:[286:java/lang/Throwable  ], 215:[286:java/lang/Throwable  ], 220:[286:java/lang/Throwable  ], 230:[286:java/lang/Throwable  ], 238:[286:java/lang/Throwable  ], 239:[286:java/lang/Throwable  ], 240:[286:java/lang/Throwable  ], 244:[286:java/lang/Throwable  ], 247:[286:java/lang/Throwable  ], 249:[286:java/lang/Throwable  ], 251:[286:java/lang/Throwable  ], 253:[286:java/lang/Throwable  ], 255:[286:java/lang/Throwable  ], 257:[286:java/lang/Throwable  ], 264:[286:java/lang/Throwable  ], 267:[286:java/lang/Throwable  ], 268:[286:java/lang/Throwable  ], 270:[286:java/lang/Throwable  ], 274:[286:java/lang/Throwable  ], 277:[286:java/lang/Throwable  ], 282:[286:java/lang/Throwable  ], 284:[286:java/lang/Throwable  ], 288:[286:java/lang/Throwable  ]]
+TRY [123:[284:java/lang/Throwable  ], 126:[284:java/lang/Throwable  ], 128:[284:java/lang/Throwable  ], 129:[284:java/lang/Throwable  ], 131:[284:java/lang/Throwable  ], 132:[284:java/lang/Throwable  ], 133:[284:java/lang/Throwable  ], 134:[284:java/lang/Throwable  ], 141:[284:java/lang/Throwable  ], 145:[284:java/lang/Throwable  ], 148:[284:java/lang/Throwable  ], 152:[284:java/lang/Throwable  ], 155:[284:java/lang/Throwable  ], 158:[284:java/lang/Throwable  ], 163:[284:java/lang/Throwable  ], 166:[284:java/lang/Throwable  ], 170:[284:java/lang/Throwable  ], 173:[284:java/lang/Throwable  ], 175:[284:java/lang/Throwable  ], 177:[284:java/lang/Throwable  ], 179:[284:java/lang/Throwable  ], 180:[284:java/lang/Throwable  ], 182:[284:java/lang/Throwable  ], 188:[284:java/lang/Throwable  ], 189:[284:java/lang/Throwable  ], 190:[284:java/lang/Throwable  ], 192:[284:java/lang/Throwable  ], 195:[284:java/lang/Throwable  ], 197:[284:java/lang/Throwable  ], 199:[284:java/lang/Throwable  ], 206:[284:java/lang/Throwable  ], 207:[284:java/lang/Throwable  ], 212:[284:java/lang/Throwable  ], 213:[284:java/lang/Throwable  ], 218:[284:java/lang/Throwable  ], 228:[284:java/lang/Throwable  ], 236:[284:java/lang/Throwable  ], 237:[284:java/lang/Throwable  ], 238:[284:java/lang/Throwable  ], 242:[284:java/lang/Throwable  ], 245:[284:java/lang/Throwable  ], 247:[284:java/lang/Throwable  ], 249:[284:java/lang/Throwable  ], 251:[284:java/lang/Throwable  ], 253:[284:java/lang/Throwable  ], 255:[284:java/lang/Throwable  ], 259:[284:java/lang/Throwable  ], 263:[284:java/lang/Throwable  ], 266:[284:java/lang/Throwable  ], 268:[284:java/lang/Throwable  ], 272:[284:java/lang/Throwable  ], 275:[284:java/lang/Throwable  ], 280:[284:java/lang/Throwable  ], 282:[284:java/lang/Throwable  ], 286:[284:java/lang/Throwable  ]]
 ALOAD 1
 INVOKESTATIC local/lito381/server/Protocol.objectSpellRequest([B)Llocal/lito381/server/Protocol$ObjectSpellRequest;
 ALOAD 0
```

### `useJoyOfPainSpell([BLlocal/lito381/server/BattleSpellCatalog$JoyOfPainSpell;)V`

- 類別：`local/lito381/server/ClientSession`
- 類型：例外表涵蓋、含迴圈（路徑比較器不支援）；事件後繼：`LAYOUT_ONLY`
- 比較器：`method useJoyOfPainSpell([BLlocal/lito381/server/BattleSpellCatalog$JoyOfPainSpell;)V: left=133:[317:java/lang/Throwable  ] right=136:[322:java/lang/Throwable  ]; paths rejected: normal control-flow cycle; labelled CFGs differ`

```diff
@@ -1,5 +1,5 @@
 META 2 null [java/io/IOException]
-TRY [133:[317:java/lang/Throwable  ], 136:[317:java/lang/Throwable  ], 138:[317:java/lang/Throwable  ], 139:[317:java/lang/Throwable  ], 141:[317:java/lang/Throwable  ], 142:[317:java/lang/Throwable  ], 143:[317:java/lang/Throwable  ], 144:[317:java/lang/Throwable  ], 147:[317:java/lang/Throwable  ], 151:[317:java/lang/Throwable  ], 154:[317:java/lang/Throwable  ], 156:[317:java/lang/Throwable  ], 159:[317:java/lang/Throwable  ], 163:[317:java/lang/Throwable  ], 167:[317:java/lang/Throwable  ], 170:[317:java/lang/Throwable  ], 174:[317:java/lang/Throwable  ], 179:[317:java/lang/Throwable  ], 184:[317:java/lang/Throwable  ], 187:[317:java/lang/Throwable  ], 195:[317:java/lang/Throwable  ], 213:[317:java/lang/Throwable  ], 215:[317:java/lang/Throwable  ], 223:[317:java/lang/Throwable  ], 224:[317:java/lang/Throwable  ], 225:[317:java/lang/Throwable  ], 226:[317:java/lang/Throwable  ], 231:[317:java/lang/Throwable  ], 233:[317:java/lang/Throwable  ], 242:[317:java/lang/Throwable  ], 250:[317:java/lang/Throwable  ], 256:[317:java/lang/Throwable  ], 257:[317:java/lang/Throwable  ], 259:[317:java/lang/Throwable  ], 260:[317:java/lang/Throwable  ], 262:[317:java/lang/Throwable  ], 264:[317:java/lang/Throwable  ], 266:[317:java/lang/Throwable  ], 268:[317:java/lang/Throwable  ], 274:[317:java/lang/Throwable  ], 276:[317:java/lang/Throwable  ], 277:[317:java/lang/Throwable  ], 278:[317:java/lang/Throwable  ], 280:[317:java/lang/Throwable  ], 285:[317:java/lang/Throwable  ], 289:[317:java/lang/Throwable  ], 290:[317:java/lang/Throwable  ], 291:[317:java/lang/Throwable  ], 295:[317:java/lang/Throwable  ], 298:[317:java/lang/Throwable  ], 299:[317:java/lang/Throwable  ], 301:[317:java/lang/Throwable  ], 305:[317:java/lang/Throwable  ], 308:[317:java/lang/Throwable  ], 313:[317:java/lang/Throwable  ], 315:[317:java/lang/Throwable  ], 319:[317:java/lang/Throwable  ]]
+TRY [136:[322:java/lang/Throwable  ], 139:[322:java/lang/Throwable  ], 141:[322:java/lang/Throwable  ], 142:[322:java/lang/Throwable  ], 144:[322:java/lang/Throwable  ], 145:[322:java/lang/Throwable  ], 146:[322:java/lang/Throwable  ], 147:[322:java/lang/Throwable  ], 150:[322:java/lang/Throwable  ], 154:[322:java/lang/Throwable  ], 157:[322:java/lang/Throwable  ], 159:[322:java/lang/Throwable  ], 162:[322:java/lang/Throwable  ], 166:[322:java/lang/Throwable  ], 170:[322:java/lang/Throwable  ], 173:[322:java/lang/Throwable  ], 177:[322:java/lang/Throwable  ], 182:[322:java/lang/Throwable  ], 187:[322:java/lang/Throwable  ], 190:[322:java/lang/Throwable  ], 198:[322:java/lang/Throwable  ], 216:[322:java/lang/Throwable  ], 218:[322:java/lang/Throwable  ], 226:[322:java/lang/Throwable  ], 227:[322:java/lang/Throwable  ], 228:[322:java/lang/Throwable  ], 229:[322:java/lang/Throwable  ], 234:[322:java/lang/Throwable  ], 236:[322:java/lang/Throwable  ], 245:[322:java/lang/Throwable  ], 253:[322:java/lang/Throwable  ], 259:[322:java/lang/Throwable  ], 260:[322:java/lang/Throwable  ], 262:[322:java/lang/Throwable  ], 263:[322:java/lang/Throwable  ], 265:[322:java/lang/Throwable  ], 267:[322:java/lang/Throwable  ], 269:[322:java/lang/Throwable  ], 271:[322:java/lang/Throwable  ], 277:[322:java/lang/Throwable  ], 279:[322:java/lang/Throwable  ], 280:[322:java/lang/Throwable  ], 281:[322:java/lang/Throwable  ], 283:[322:java/lang/Throwable  ], 288:[322:java/lang/Throwable  ], 292:[322:java/lang/Throwable  ], 293:[322:java/lang/Throwable  ], 294:[322:java/lang/Throwable  ], 297:[322:java/lang/Throwable  ], 301:[322:java/lang/Throwable  ], 304:[322:java/lang/Throwable  ], 306:[322:java/lang/Throwable  ], 310:[322:java/lang/Throwable  ], 313:[322:java/lang/Throwable  ], 318:[322:java/lang/Throwable  ], 320:[322:java/lang/Throwable  ], 324:[322:java/lang/Throwable  ]]
 ALOAD 1
 INVOKESTATIC local/lito381/server/Protocol.selectorOnlySpellRequest([B)Llocal/lito381/server/Protocol$SpellSelector;
 ASTORE 3
```

### `useMortalBodySpell([BLlocal/lito381/server/BattleSpellCatalog$MortalBodySpell;)V`

- 類別：`local/lito381/server/ClientSession`
- 類型：例外表涵蓋、含迴圈（路徑比較器不支援）；事件後繼：`LAYOUT_ONLY`
- 比較器：`method useMortalBodySpell([BLlocal/lito381/server/BattleSpellCatalog$MortalBodySpell;)V: left=94:[235:java/lang/Throwable  ] right=96:[237:java/lang/Throwable  ]; paths rejected: normal control-flow cycle; labelled CFGs differ`

```diff
@@ -1,5 +1,5 @@
 META 2 null [java/io/IOException]
-TRY [94:[235:java/lang/Throwable  ], 96:[235:java/lang/Throwable  ], 97:[235:java/lang/Throwable  ], 99:[235:java/lang/Throwable  ], 100:[235:java/lang/Throwable  ], 101:[235:java/lang/Throwable  ], 102:[235:java/lang/Throwable  ], 105:[235:java/lang/Throwable  ], 109:[235:java/lang/Throwable  ], 112:[235:java/lang/Throwable  ], 115:[235:java/lang/Throwable  ], 119:[235:java/lang/Throwable  ], 123:[235:java/lang/Throwable  ], 126:[235:java/lang/Throwable  ], 130:[235:java/lang/Throwable  ], 135:[235:java/lang/Throwable  ], 140:[235:java/lang/Throwable  ], 143:[235:java/lang/Throwable  ], 156:[235:java/lang/Throwable  ], 158:[235:java/lang/Throwable  ], 160:[235:java/lang/Throwable  ], 165:[235:java/lang/Throwable  ], 167:[235:java/lang/Throwable  ], 168:[235:java/lang/Throwable  ], 170:[235:java/lang/Throwable  ], 171:[235:java/lang/Throwable  ], 172:[235:java/lang/Throwable  ], 174:[235:java/lang/Throwable  ], 177:[235:java/lang/Throwable  ], 179:[235:java/lang/Throwable  ], 188:[235:java/lang/Throwable  ], 189:[235:java/lang/Throwable  ], 190:[235:java/lang/Throwable  ], 192:[235:java/lang/Throwable  ], 194:[235:java/lang/Throwable  ], 195:[235:java/lang/Throwable  ], 202:[235:java/lang/Throwable  ], 205:[235:java/lang/Throwable  ], 206:[235:java/lang/Throwable  ], 209:[235:java/lang/Throwable  ], 211:[235:java/lang/Throwable  ], 213:[235:java/lang/Throwable  ], 216:[235:java/lang/Throwable  ], 220:[235:java/lang/Throwable  ], 223:[235:java/lang/Throwable  ], 228:[235:java/lang/Throwable  ], 230:[235:java/lang/Throwable  ], 233:[235:java/lang/Throwable  ], 237:[235:java/lang/Throwable  ]]
+TRY [96:[237:java/lang/Throwable  ], 98:[237:java/lang/Throwable  ], 99:[237:java/lang/Throwable  ], 101:[237:java/lang/Throwable  ], 102:[237:java/lang/Throwable  ], 103:[237:java/lang/Throwable  ], 104:[237:java/lang/Throwable  ], 107:[237:java/lang/Throwable  ], 111:[237:java/lang/Throwable  ], 114:[237:java/lang/Throwable  ], 117:[237:java/lang/Throwable  ], 121:[237:java/lang/Throwable  ], 125:[237:java/lang/Throwable  ], 128:[237:java/lang/Throwable  ], 132:[237:java/lang/Throwable  ], 137:[237:java/lang/Throwable  ], 142:[237:java/lang/Throwable  ], 145:[237:java/lang/Throwable  ], 158:[237:java/lang/Throwable  ], 160:[237:java/lang/Throwable  ], 162:[237:java/lang/Throwable  ], 167:[237:java/lang/Throwable  ], 169:[237:java/lang/Throwable  ], 170:[237:java/lang/Throwable  ], 172:[237:java/lang/Throwable  ], 173:[237:java/lang/Throwable  ], 174:[237:java/lang/Throwable  ], 176:[237:java/lang/Throwable  ], 179:[237:java/lang/Throwable  ], 181:[237:java/lang/Throwable  ], 190:[237:java/lang/Throwable  ], 191:[237:java/lang/Throwable  ], 192:[237:java/lang/Throwable  ], 194:[237:java/lang/Throwable  ], 196:[237:java/lang/Throwable  ], 197:[237:java/lang/Throwable  ], 204:[237:java/lang/Throwable  ], 207:[237:java/lang/Throwable  ], 208:[237:java/lang/Throwable  ], 211:[237:java/lang/Throwable  ], 213:[237:java/lang/Throwable  ], 215:[237:java/lang/Throwable  ], 218:[237:java/lang/Throwable  ], 222:[237:java/lang/Throwable  ], 225:[237:java/lang/Throwable  ], 230:[237:java/lang/Throwable  ], 232:[237:java/lang/Throwable  ], 235:[237:java/lang/Throwable  ], 239:[237:java/lang/Throwable  ]]
 ALOAD 1
 INVOKESTATIC local/lito381/server/Protocol.selectorOnlySpellRequest([B)Llocal/lito381/server/Protocol$SpellSelector;
 ASTORE 3
```

### `useMovementBuffSpell([BLlocal/lito381/server/BattleSpellCatalog$MovementBuffSpell;)V`

- 類別：`local/lito381/server/ClientSession`
- 類型：例外表涵蓋、含迴圈（路徑比較器不支援）；事件後繼：`LAYOUT_ONLY`
- 比較器：`method useMovementBuffSpell([BLlocal/lito381/server/BattleSpellCatalog$MovementBuffSpell;)V: left=64:[173:java/lang/Throwable  ] right=62:[171:java/lang/Throwable  ]; paths rejected: normal control-flow cycle; labelled CFGs differ`

```diff
@@ -1,5 +1,5 @@
 META 2 null [java/io/IOException]
-TRY [64:[173:java/lang/Throwable  ], 66:[173:java/lang/Throwable  ], 67:[173:java/lang/Throwable  ], 69:[173:java/lang/Throwable  ], 70:[173:java/lang/Throwable  ], 71:[173:java/lang/Throwable  ], 72:[173:java/lang/Throwable  ], 79:[173:java/lang/Throwable  ], 83:[173:java/lang/Throwable  ], 84:[173:java/lang/Throwable  ], 87:[173:java/lang/Throwable  ], 88:[173:java/lang/Throwable  ], 91:[173:java/lang/Throwable  ], 94:[173:java/lang/Throwable  ], 96:[173:java/lang/Throwable  ], 98:[173:java/lang/Throwable  ], 100:[173:java/lang/Throwable  ], 106:[173:java/lang/Throwable  ], 107:[173:java/lang/Throwable  ], 108:[173:java/lang/Throwable  ], 110:[173:java/lang/Throwable  ], 117:[173:java/lang/Throwable  ], 120:[173:java/lang/Throwable  ], 121:[173:java/lang/Throwable  ], 123:[173:java/lang/Throwable  ], 126:[173:java/lang/Throwable  ], 128:[173:java/lang/Throwable  ], 129:[173:java/lang/Throwable  ], 132:[173:java/lang/Throwable  ], 137:[173:java/lang/Throwable  ], 138:[173:java/lang/Throwable  ], 141:[173:java/lang/Throwable  ], 144:[173:java/lang/Throwable  ], 146:[173:java/lang/Throwable  ], 147:[173:java/lang/Throwable  ], 150:[173:java/lang/Throwable  ], 151:[173:java/lang/Throwable  ], 153:[173:java/lang/Throwable  ], 156:[173:java/lang/Throwable  ], 159:[173:java/lang/Throwable  ], 162:[173:java/lang/Throwable  ], 165:[173:java/lang/Throwable  ], 168:[173:java/lang/Throwable  ], 171:[173:java/lang/Throwable  ], 175:[173:java/lang/Throwable  ]]
+TRY [62:[171:java/lang/Throwable  ], 64:[171:java/lang/Throwable  ], 65:[171:java/lang/Throwable  ], 67:[171:java/lang/Throwable  ], 68:[171:java/lang/Throwable  ], 69:[171:java/lang/Throwable  ], 70:[171:java/lang/Throwable  ], 77:[171:java/lang/Throwable  ], 81:[171:java/lang/Throwable  ], 82:[171:java/lang/Throwable  ], 85:[171:java/lang/Throwable  ], 86:[171:java/lang/Throwable  ], 89:[171:java/lang/Throwable  ], 92:[171:java/lang/Throwable  ], 94:[171:java/lang/Throwable  ], 96:[171:java/lang/Throwable  ], 98:[171:java/lang/Throwable  ], 104:[171:java/lang/Throwable  ], 105:[171:java/lang/Throwable  ], 106:[171:java/lang/Throwable  ], 108:[171:java/lang/Throwable  ], 112:[171:java/lang/Throwable  ], 116:[171:java/lang/Throwable  ], 119:[171:java/lang/Throwable  ], 121:[171:java/lang/Throwable  ], 124:[171:java/lang/Throwable  ], 126:[171:java/lang/Throwable  ], 127:[171:java/lang/Throwable  ], 130:[171:java/lang/Throwable  ], 135:[171:java/lang/Throwable  ], 136:[171:java/lang/Throwable  ], 139:[171:java/lang/Throwable  ], 142:[171:java/lang/Throwable  ], 144:[171:java/lang/Throwable  ], 145:[171:java/lang/Throwable  ], 148:[171:java/lang/Throwable  ], 149:[171:java/lang/Throwable  ], 151:[171:java/lang/Throwable  ], 154:[171:java/lang/Throwable  ], 157:[171:java/lang/Throwable  ], 160:[171:java/lang/Throwable  ], 163:[171:java/lang/Throwable  ], 166:[171:java/lang/Throwable  ], 169:[171:java/lang/Throwable  ], 173:[171:java/lang/Throwable  ]]
 ALOAD 1
 INVOKESTATIC local/lito381/server/Protocol.selectorOnlySpellRequest([B)Llocal/lito381/server/Protocol$SpellSelector;
 ASTORE 3
```

### `useNaturesTouchSpell([BLlocal/lito381/server/BattleSpellCatalog$NaturesTouchSpell;)V`

- 類別：`local/lito381/server/ClientSession`
- 類型：例外表涵蓋、含迴圈（路徑比較器不支援）；事件後繼：`LAYOUT_ONLY`
- 比較器：`method useNaturesTouchSpell([BLlocal/lito381/server/BattleSpellCatalog$NaturesTouchSpell;)V: left=297:[487:java/lang/Throwable  ] right=300:[491:java/lang/Throwable  ]; paths rejected: normal control-flow cycle; labelled CFGs differ`

```diff
@@ -1,5 +1,5 @@
 META 2 null [java/io/IOException]
-TRY [297:[487:java/lang/Throwable  ], 298:[476:java/lang/Throwable  , 487:java/lang/Throwable  ], 301:[476:java/lang/Throwable  , 487:java/lang/Throwable  ], 303:[476:java/lang/Throwable  , 487:java/lang/Throwable  ], 304:[476:java/lang/Throwable  , 487:java/lang/Throwable  ], 306:[476:java/lang/Throwable  , 487:java/lang/Throwable  ], 307:[476:java/lang/Throwable  , 487:java/lang/Throwable  ], 308:[476:java/lang/Throwable  , 487:java/lang/Throwable  ], 309:[476:java/lang/Throwable  , 487:java/lang/Throwable  ], 312:[476:java/lang/Throwable  , 487:java/lang/Throwable  ], 316:[476:java/lang/Throwable  , 487:java/lang/Throwable  ], 319:[476:java/lang/Throwable  , 487:java/lang/Throwable  ], 322:[476:java/lang/Throwable  , 487:java/lang/Throwable  ], 326:[476:java/lang/Throwable  , 487:java/lang/Throwable  ], 329:[476:java/lang/Throwable  , 487:java/lang/Throwable  ], 332:[476:java/lang/Throwable  , 487:java/lang/Throwable  ], 338:[476:java/lang/Throwable  , 487:java/lang/Throwable  ], 340:[476:java/lang/Throwable  , 487:java/lang/Throwable  ], 342:[476:java/lang/Throwable  , 487:java/lang/Throwable  ], 344:[476:java/lang/Throwable  , 487:java/lang/Throwable  ], 345:[476:java/lang/Throwable  , 487:java/lang/Throwable  ], 348:[476:java/lang/Throwable  , 487:java/lang/Throwable  ], 350:[476:java/lang/Throwable  , 487:java/lang/Throwable  ], 352:[476:java/lang/Throwable  , 487:java/lang/Throwable  ], 353:[476:java/lang/Throwable  , 487:java/lang/Throwable  ], 356:[476:java/lang/Throwable  , 487:java/lang/Throwable  ], 359:[476:java/lang/Throwable  , 487:java/lang/Throwable  ], 363:[476:java/lang/Throwable  , 487:java/lang/Throwable  ], 367:[476:java/lang/Throwable  , 487:java/lang/Throwable  ], 368:[476:java/lang/Throwable  , 487:java/lang/Throwable  ], 371:[476:java/lang/Throwable  , 487:java/lang/Throwable  ], 375:[476:java/lang/Throwable  , 487:java/lang/Throwable  ], 380:[476:java/lang/Throwable  , 487:java/lang/Throwable  ], 385:[476:java/lang/Throwable  , 487:java/lang/Throwable  ], 386:[476:java/lang/Throwable  , 487:java/lang/Throwable  ], 389:[476:java/lang/Throwable  , 487:java/lang/Throwable  ], 394:[476:java/lang/Throwable  , 487:java/lang/Throwable  ], 397:[476:java/lang/Throwable  , 487:java/lang/Throwable  ], 412:[476:java/lang/Throwable  , 487:java/lang/Throwable  ], 413:[476:java/lang/Throwable  , 487:java/lang/Throwable  ], 414:[476:java/lang/Throwable  , 487:java/lang/Throwable  ], 416:[476:java/lang/Throwable  , 487:java/lang/Throwable  ], 419:[476:java/lang/Throwable  , 487:java/lang/Throwable  ], 420:[476:java/lang/Throwable  , 487:java/lang/Throwable  ], 423:[476:java/lang/Throwable  , 487:java/lang/Throwable  ], 424:[476:java/lang/Throwable  , 487:java/lang/Throwable  ], 429:[476:java/lang/Throwable  , 487:java/lang/Throwable  ], 432:[476:java/lang/Throwable  , 487:java/lang/Throwable  ], 433:[476:java/lang/Throwable  , 487:java/lang/Throwable  ], 435:[476:java/lang/Throwable  , 487:java/lang/Throwable  ], 439:[476:java/lang/Throwable  , 487:java/lang/Throwable  ], 442:[476:java/lang/Throwable  , 487:java/lang/Throwable  ], 447:[476:java/lang/Throwable  , 487:java/lang/Throwable  ], 449:[476:java/lang/Throwable  , 487:java/lang/Throwable  ], 452:[476:java/lang/Throwable  , 487:java/lang/Throwable  ], 458:[476:java/lang/Throwable  , 487:java/lang/Throwable  ], 459:[476:java/lang/Throwable  , 487:java/lang/Throwable  ], 461:[476:java/lang/Throwable  , 487:java/lang/Throwable  ], 465:[476:java/lang/Throwable  , 487:java/lang/Throwable  ], 467:[476:java/lang/Throwable  , 487:java/lang/Throwable  ], 468:[476:java/lang/Throwable  , 487:java/lang/Throwable  ], 470:[476:java/lang/Throwable  , 487:java/lang/Throwable  ], 471:[476:java/lang/Throwable  , 487:java/lang/Throwable  ], 472:[476:java/lang/Throwable  , 487:java/lang/Throwable  ], 474:[476:java/lang/Throwable  , 487:java/lang/Throwable  ], 478:[476:java/lang/Throwable  , 487:java/lang/Throwable  ], 480:[487:java/lang/Throwable  ], 482:[487:java/lang/Throwable  ], 485:[487:java/lang/Throwable  ], 489:[487:java/lang/Throwable  ]]
+TRY [300:[491:java/lang/Throwable  ], 301:[483:java/lang/Throwable  , 491:java/lang/Throwable  ], 304:[483:java/lang/Throwable  , 491:java/lang/Throwable  ], 306:[483:java/lang/Throwable  , 491:java/lang/Throwable  ], 307:[483:java/lang/Throwable  , 491:java/lang/Throwable  ], 309:[483:java/lang/Throwable  , 491:java/lang/Throwable  ], 310:[483:java/lang/Throwable  , 491:java/lang/Throwable  ], 311:[483:java/lang/Throwable  , 491:java/lang/Throwable  ], 312:[483:java/lang/Throwable  , 491:java/lang/Throwable  ], 315:[483:java/lang/Throwable  , 491:java/lang/Throwable  ], 319:[483:java/lang/Throwable  , 491:java/lang/Throwable  ], 322:[483:java/lang/Throwable  , 491:java/lang/Throwable  ], 325:[483:java/lang/Throwable  , 491:java/lang/Throwable  ], 329:[483:java/lang/Throwable  , 491:java/lang/Throwable  ], 332:[483:java/lang/Throwable  , 491:java/lang/Throwable  ], 335:[483:java/lang/Throwable  , 491:java/lang/Throwable  ], 341:[483:java/lang/Throwable  , 491:java/lang/Throwable  ], 343:[483:java/lang/Throwable  , 491:java/lang/Throwable  ], 345:[483:java/lang/Throwable  , 491:java/lang/Throwable  ], 347:[483:java/lang/Throwable  , 491:java/lang/Throwable  ], 348:[483:java/lang/Throwable  , 491:java/lang/Throwable  ], 351:[483:java/lang/Throwable  , 491:java/lang/Throwable  ], 353:[483:java/lang/Throwable  , 491:java/lang/Throwable  ], 355:[483:java/lang/Throwable  , 491:java/lang/Throwable  ], 356:[483:java/lang/Throwable  , 491:java/lang/Throwable  ], 359:[483:java/lang/Throwable  , 491:java/lang/Throwable  ], 362:[483:java/lang/Throwable  , 491:java/lang/Throwable  ], 366:[483:java/lang/Throwable  , 491:java/lang/Throwable  ], 370:[483:java/lang/Throwable  , 491:java/lang/Throwable  ], 371:[483:java/lang/Throwable  , 491:java/lang/Throwable  ], 374:[483:java/lang/Throwable  , 491:java/lang/Throwable  ], 378:[483:java/lang/Throwable  , 491:java/lang/Throwable  ], 383:[483:java/lang/Throwable  , 491:java/lang/Throwable  ], 388:[483:java/lang/Throwable  , 491:java/lang/Throwable  ], 389:[483:java/lang/Throwable  , 491:java/lang/Throwable  ], 392:[483:java/lang/Throwable  , 491:java/lang/Throwable  ], 397:[483:java/lang/Throwable  , 491:java/lang/Throwable  ], 400:[483:java/lang/Throwable  , 491:java/lang/Throwable  ], 414:[483:java/lang/Throwable  , 491:java/lang/Throwable  ], 420:[483:java/lang/Throwable  , 491:java/lang/Throwable  ], 421:[483:java/lang/Throwable  , 491:java/lang/Throwable  ], 423:[483:java/lang/Throwable  , 491:java/lang/Throwable  ], 427:[483:java/lang/Throwable  , 491:java/lang/Throwable  ], 429:[483:java/lang/Throwable  , 491:java/lang/Throwable  ], 430:[483:java/lang/Throwable  , 491:java/lang/Throwable  ], 432:[483:java/lang/Throwable  , 491:java/lang/Throwable  ], 433:[483:java/lang/Throwable  , 491:java/lang/Throwable  ], 434:[483:java/lang/Throwable  , 491:java/lang/Throwable  ], 436:[483:java/lang/Throwable  , 491:java/lang/Throwable  ], 438:[491:java/lang/Throwable  ], 442:[483:java/lang/Throwable  , 491:java/lang/Throwable  ], 443:[483:java/lang/Throwable  , 491:java/lang/Throwable  ], 444:[483:java/lang/Throwable  , 491:java/lang/Throwable  ], 446:[483:java/lang/Throwable  , 491:java/lang/Throwable  ], 449:[483:java/lang/Throwable  , 491:java/lang/Throwable  ], 450:[483:java/lang/Throwable  , 491:java/lang/Throwable  ], 453:[483:java/lang/Throwable  , 491:java/lang/Throwable  ], 454:[483:java/lang/Throwable  , 491:java/lang/Throwable  ], 458:[483:java/lang/Throwable  , 491:java/lang/Throwable  ], 462:[483:java/lang/Throwable  , 491:java/lang/Throwable  ], 465:[483:java/lang/Throwable  , 491:java/lang/Throwable  ], 467:[483:java/lang/Throwable  , 491:java/lang/Throwable  ], 471:[483:java/lang/Throwable  , 491:java/lang/Throwable  ], 474:[483:java/lang/Throwable  , 491:java/lang/Throwable  ], 479:[483:java/lang/Throwable  , 491:java/lang/Throwable  ], 481:[483:java/lang/Throwable  , 491:java/lang/Throwable  ], 485:[483:java/lang/Throwable  , 491:java/lang/Throwable  ], 487:[491:java/lang/Throwable  ], 489:[491:java/lang/Throwable  ], 493:[491:java/lang/Throwable  ]]
 ALOAD 1
 INVOKESTATIC local/lito381/server/Protocol.objectSpellRequest([B)Llocal/lito381/server/Protocol$ObjectSpellRequest;
 ALOAD 0
```

### `useOtherNoEffectSpell([BLlocal/lito381/server/BattleSpellCatalog$OtherNoEffectSpell;)V`

- 類別：`local/lito381/server/ClientSession`
- 類型：例外表涵蓋；事件後繼：`LAYOUT_ONLY`
- 比較器：`method useOtherNoEffectSpell([BLlocal/lito381/server/BattleSpellCatalog$OtherNoEffectSpell;)V: left=172:[248:java/lang/Throwable  ] right=170:[246:java/lang/Throwable  ]; paths rejected: path limit 4096 exceeded; labelled CFGs differ`

```diff
@@ -1,5 +1,5 @@
 META 2 null [java/io/IOException]
-TRY [172:[248:java/lang/Throwable  ], 176:[248:java/lang/Throwable  ], 180:[248:java/lang/Throwable  ], 183:[248:java/lang/Throwable  ], 186:[248:java/lang/Throwable  ], 189:[248:java/lang/Throwable  ], 195:[248:java/lang/Throwable  ], 199:[248:java/lang/Throwable  ], 203:[248:java/lang/Throwable  ], 209:[248:java/lang/Throwable  ], 210:[248:java/lang/Throwable  ], 212:[248:java/lang/Throwable  ], 213:[248:java/lang/Throwable  ], 215:[248:java/lang/Throwable  ], 217:[248:java/lang/Throwable  ], 220:[248:java/lang/Throwable  ], 221:[248:java/lang/Throwable  ], 222:[248:java/lang/Throwable  ], 223:[248:java/lang/Throwable  ], 225:[248:java/lang/Throwable  ], 229:[248:java/lang/Throwable  ], 230:[248:java/lang/Throwable  ], 231:[248:java/lang/Throwable  ], 237:[248:java/lang/Throwable  ], 240:[248:java/lang/Throwable  ], 241:[248:java/lang/Throwable  ], 243:[248:java/lang/Throwable  ], 246:[248:java/lang/Throwable  ], 250:[248:java/lang/Throwable  ]]
+TRY [170:[246:java/lang/Throwable  ], 174:[246:java/lang/Throwable  ], 178:[246:java/lang/Throwable  ], 181:[246:java/lang/Throwable  ], 184:[246:java/lang/Throwable  ], 187:[246:java/lang/Throwable  ], 193:[246:java/lang/Throwable  ], 197:[246:java/lang/Throwable  ], 201:[246:java/lang/Throwable  ], 207:[246:java/lang/Throwable  ], 208:[246:java/lang/Throwable  ], 210:[246:java/lang/Throwable  ], 211:[246:java/lang/Throwable  ], 213:[246:java/lang/Throwable  ], 215:[246:java/lang/Throwable  ], 218:[246:java/lang/Throwable  ], 219:[246:java/lang/Throwable  ], 220:[246:java/lang/Throwable  ], 221:[246:java/lang/Throwable  ], 223:[246:java/lang/Throwable  ], 227:[246:java/lang/Throwable  ], 228:[246:java/lang/Throwable  ], 229:[246:java/lang/Throwable  ], 232:[246:java/lang/Throwable  ], 236:[246:java/lang/Throwable  ], 239:[246:java/lang/Throwable  ], 241:[246:java/lang/Throwable  ], 244:[246:java/lang/Throwable  ], 248:[246:java/lang/Throwable  ]]
 ALOAD 1
 INVOKESTATIC local/lito381/server/Protocol.selectorOnlySpellRequest([B)Llocal/lito381/server/Protocol$SpellSelector;
 ASTORE 3
```

### `usePanicSpell([BLlocal/lito381/server/BattleSpellCatalog$PanicSpell;I)V`

- 類別：`local/lito381/server/ClientSession`
- 類型：例外表涵蓋、含迴圈（路徑比較器不支援）；事件後繼：`LAYOUT_ONLY`
- 比較器：`method usePanicSpell([BLlocal/lito381/server/BattleSpellCatalog$PanicSpell;I)V: left=212:[217:java/lang/Exception  ] right=217:[222:java/lang/Exception  ]; paths rejected: normal control-flow cycle; labelled CFGs differ`

```diff
@@ -1,5 +1,5 @@
 META 2 null [java/io/IOException]
-TRY [212:[217:java/lang/Exception  ], 214:[217:java/lang/Exception  ], 419:[612:java/lang/Throwable  ], 422:[612:java/lang/Throwable  ], 424:[612:java/lang/Throwable  ], 425:[612:java/lang/Throwable  ], 427:[612:java/lang/Throwable  ], 428:[612:java/lang/Throwable  ], 429:[612:java/lang/Throwable  ], 430:[612:java/lang/Throwable  ], 433:[612:java/lang/Throwable  ], 437:[612:java/lang/Throwable  ], 440:[612:java/lang/Throwable  ], 442:[612:java/lang/Throwable  ], 445:[612:java/lang/Throwable  ], 449:[612:java/lang/Throwable  ], 453:[612:java/lang/Throwable  ], 456:[612:java/lang/Throwable  ], 460:[612:java/lang/Throwable  ], 465:[612:java/lang/Throwable  ], 470:[612:java/lang/Throwable  ], 473:[612:java/lang/Throwable  ], 481:[612:java/lang/Throwable  ], 499:[612:java/lang/Throwable  ], 501:[612:java/lang/Throwable  ], 509:[612:java/lang/Throwable  ], 510:[612:java/lang/Throwable  ], 511:[612:java/lang/Throwable  ], 512:[612:java/lang/Throwable  ], 517:[612:java/lang/Throwable  ], 519:[612:java/lang/Throwable  ], 528:[612:java/lang/Throwable  ], 536:[612:java/lang/Throwable  ], 542:[612:java/lang/Throwable  ], 543:[612:java/lang/Throwable  ], 545:[612:java/lang/Throwable  ], 546:[612:java/lang/Throwable  ], 548:[612:java/lang/Throwable  ], 550:[612:java/lang/Throwable  ], 552:[612:java/lang/Throwable  ], 554:[612:java/lang/Throwable  ], 556:[612:java/lang/Throwable  ], 562:[612:java/lang/Throwable  ], 564:[612:java/lang/Throwable  ], 565:[612:java/lang/Throwable  ], 566:[612:java/lang/Throwable  ], 568:[612:java/lang/Throwable  ], 574:[612:java/lang/Throwable  ], 575:[612:java/lang/Throwable  ], 576:[612:java/lang/Throwable  ], 579:[612:java/lang/Throwable  ], 581:[612:java/lang/Throwable  ], 583:[612:java/lang/Throwable  ], 587:[612:java/lang/Throwable  ], 590:[612:java/lang/Throwable  ], 591:[612:java/lang/Throwable  ], 593:[612:java/lang/Throwable  ], 596:[612:java/lang/Throwable  ], 600:[612:java/lang/Throwable  ], 603:[612:java/lang/Throwable  ], 608:[612:java/lang/Throwable  ], 610:[612:java/lang/Throwable  ], 614:[612:java/lang/Throwable  ]]
+TRY [217:[222:java/lang/Exception  ], 219:[222:java/lang/Exception  ], 424:[619:java/lang/Throwable  ], 427:[619:java/lang/Throwable  ], 429:[619:java/lang/Throwable  ], 430:[619:java/lang/Throwable  ], 432:[619:java/lang/Throwable  ], 433:[619:java/lang/Throwable  ], 434:[619:java/lang/Throwable  ], 435:[619:java/lang/Throwable  ], 438:[619:java/lang/Throwable  ], 442:[619:java/lang/Throwable  ], 445:[619:java/lang/Throwable  ], 447:[619:java/lang/Throwable  ], 450:[619:java/lang/Throwable  ], 454:[619:java/lang/Throwable  ], 458:[619:java/lang/Throwable  ], 461:[619:java/lang/Throwable  ], 465:[619:java/lang/Throwable  ], 470:[619:java/lang/Throwable  ], 475:[619:java/lang/Throwable  ], 478:[619:java/lang/Throwable  ], 486:[619:java/lang/Throwable  ], 504:[619:java/lang/Throwable  ], 506:[619:java/lang/Throwable  ], 514:[619:java/lang/Throwable  ], 515:[619:java/lang/Throwable  ], 516:[619:java/lang/Throwable  ], 517:[619:java/lang/Throwable  ], 522:[619:java/lang/Throwable  ], 524:[619:java/lang/Throwable  ], 533:[619:java/lang/Throwable  ], 541:[619:java/lang/Throwable  ], 547:[619:java/lang/Throwable  ], 548:[619:java/lang/Throwable  ], 550:[619:java/lang/Throwable  ], 551:[619:java/lang/Throwable  ], 553:[619:java/lang/Throwable  ], 555:[619:java/lang/Throwable  ], 557:[619:java/lang/Throwable  ], 559:[619:java/lang/Throwable  ], 561:[619:java/lang/Throwable  ], 567:[619:java/lang/Throwable  ], 569:[619:java/lang/Throwable  ], 570:[619:java/lang/Throwable  ], 571:[619:java/lang/Throwable  ], 573:[619:java/lang/Throwable  ], 579:[619:java/lang/Throwable  ], 580:[619:java/lang/Throwable  ], 581:[619:java/lang/Throwable  ], 584:[619:java/lang/Throwable  ], 586:[619:java/lang/Throwable  ], 588:[619:java/lang/Throwable  ], 591:[619:java/lang/Throwable  ], 595:[619:java/lang/Throwable  ], 598:[619:java/lang/Throwable  ], 600:[619:java/lang/Throwable  ], 603:[619:java/lang/Throwable  ], 607:[619:java/lang/Throwable  ], 610:[619:java/lang/Throwable  ], 615:[619:java/lang/Throwable  ], 617:[619:java/lang/Throwable  ], 621:[619:java/lang/Throwable  ]]
 ILOAD 3
 IFLT L5
 ILOAD 3
```

### `usePatienceSpell([BLlocal/lito381/server/BattleSpellCatalog$PatienceSpell;)V`

- 類別：`local/lito381/server/ClientSession`
- 類型：例外表涵蓋、含迴圈（路徑比較器不支援）；事件後繼：`LAYOUT_ONLY`
- 比較器：`method usePatienceSpell([BLlocal/lito381/server/BattleSpellCatalog$PatienceSpell;)V: left=119:[323:java/lang/Throwable  ] right=122:[328:java/lang/Throwable  ]; paths rejected: normal control-flow cycle; labelled CFGs differ`

```diff
@@ -1,5 +1,5 @@
 META 2 null [java/io/IOException]
-TRY [119:[323:java/lang/Throwable  ], 122:[323:java/lang/Throwable  ], 124:[323:java/lang/Throwable  ], 125:[323:java/lang/Throwable  ], 127:[323:java/lang/Throwable  ], 128:[323:java/lang/Throwable  ], 129:[323:java/lang/Throwable  ], 130:[323:java/lang/Throwable  ], 133:[323:java/lang/Throwable  ], 137:[323:java/lang/Throwable  ], 140:[323:java/lang/Throwable  ], 143:[323:java/lang/Throwable  ], 147:[323:java/lang/Throwable  ], 151:[323:java/lang/Throwable  ], 154:[323:java/lang/Throwable  ], 158:[323:java/lang/Throwable  ], 163:[323:java/lang/Throwable  ], 168:[323:java/lang/Throwable  ], 171:[323:java/lang/Throwable  ], 179:[323:java/lang/Throwable  ], 197:[323:java/lang/Throwable  ], 199:[323:java/lang/Throwable  ], 207:[323:java/lang/Throwable  ], 208:[323:java/lang/Throwable  ], 209:[323:java/lang/Throwable  ], 210:[323:java/lang/Throwable  ], 215:[323:java/lang/Throwable  ], 217:[323:java/lang/Throwable  ], 226:[323:java/lang/Throwable  ], 234:[323:java/lang/Throwable  ], 240:[323:java/lang/Throwable  ], 241:[323:java/lang/Throwable  ], 243:[323:java/lang/Throwable  ], 244:[323:java/lang/Throwable  ], 246:[323:java/lang/Throwable  ], 248:[323:java/lang/Throwable  ], 253:[323:java/lang/Throwable  ], 255:[323:java/lang/Throwable  ], 256:[323:java/lang/Throwable  ], 257:[323:java/lang/Throwable  ], 259:[323:java/lang/Throwable  ], 264:[323:java/lang/Throwable  ], 267:[323:java/lang/Throwable  ], 271:[323:java/lang/Throwable  ], 274:[323:java/lang/Throwable  ], 275:[323:java/lang/Throwable  ], 278:[323:java/lang/Throwable  ], 281:[323:java/lang/Throwable  ], 284:[323:java/lang/Throwable  ], 285:[323:java/lang/Throwable  ], 288:[323:java/lang/Throwable  ], 291:[323:java/lang/Throwable  ], 295:[323:java/lang/Throwable  ], 296:[323:java/lang/Throwable  ], 297:[323:java/lang/Throwable  ], 301:[323:java/lang/Throwable  ], 304:[323:java/lang/Throwable  ], 305:[323:java/lang/Throwable  ], 307:[323:java/lang/Throwable  ], 311:[323:java/lang/Throwable  ], 314:[323:java/lang/Throwable  ], 319:[323:java/lang/Throwable  ], 321:[323:java/lang/Throwable  ], 325:[323:java/lang/Throwable  ]]
+TRY [122:[328:java/lang/Throwable  ], 125:[328:java/lang/Throwable  ], 127:[328:java/lang/Throwable  ], 128:[328:java/lang/Throwable  ], 130:[328:java/lang/Throwable  ], 131:[328:java/lang/Throwable  ], 132:[328:java/lang/Throwable  ], 133:[328:java/lang/Throwable  ], 136:[328:java/lang/Throwable  ], 140:[328:java/lang/Throwable  ], 143:[328:java/lang/Throwable  ], 146:[328:java/lang/Throwable  ], 150:[328:java/lang/Throwable  ], 154:[328:java/lang/Throwable  ], 157:[328:java/lang/Throwable  ], 161:[328:java/lang/Throwable  ], 166:[328:java/lang/Throwable  ], 171:[328:java/lang/Throwable  ], 174:[328:java/lang/Throwable  ], 182:[328:java/lang/Throwable  ], 200:[328:java/lang/Throwable  ], 202:[328:java/lang/Throwable  ], 210:[328:java/lang/Throwable  ], 211:[328:java/lang/Throwable  ], 212:[328:java/lang/Throwable  ], 213:[328:java/lang/Throwable  ], 218:[328:java/lang/Throwable  ], 220:[328:java/lang/Throwable  ], 229:[328:java/lang/Throwable  ], 237:[328:java/lang/Throwable  ], 243:[328:java/lang/Throwable  ], 244:[328:java/lang/Throwable  ], 246:[328:java/lang/Throwable  ], 247:[328:java/lang/Throwable  ], 249:[328:java/lang/Throwable  ], 251:[328:java/lang/Throwable  ], 256:[328:java/lang/Throwable  ], 258:[328:java/lang/Throwable  ], 259:[328:java/lang/Throwable  ], 260:[328:java/lang/Throwable  ], 262:[328:java/lang/Throwable  ], 267:[328:java/lang/Throwable  ], 270:[328:java/lang/Throwable  ], 274:[328:java/lang/Throwable  ], 277:[328:java/lang/Throwable  ], 278:[328:java/lang/Throwable  ], 281:[328:java/lang/Throwable  ], 284:[328:java/lang/Throwable  ], 287:[328:java/lang/Throwable  ], 288:[328:java/lang/Throwable  ], 291:[328:java/lang/Throwable  ], 294:[328:java/lang/Throwable  ], 298:[328:java/lang/Throwable  ], 299:[328:java/lang/Throwable  ], 300:[328:java/lang/Throwable  ], 303:[328:java/lang/Throwable  ], 307:[328:java/lang/Throwable  ], 310:[328:java/lang/Throwable  ], 312:[328:java/lang/Throwable  ], 316:[328:java/lang/Throwable  ], 319:[328:java/lang/Throwable  ], 324:[328:java/lang/Throwable  ], 326:[328:java/lang/Throwable  ], 330:[328:java/lang/Throwable  ]]
 ALOAD 1
 INVOKESTATIC local/lito381/server/Protocol.selectorOnlySpellRequest([B)Llocal/lito381/server/Protocol$SpellSelector;
 ASTORE 3
```

### `usePhantasmSpell([BLlocal/lito381/server/BattleSpellCatalog$PhantasmSpell;I)V`

- 類別：`local/lito381/server/ClientSession`
- 類型：例外表涵蓋、含迴圈（路徑比較器不支援）；事件後繼：`LAYOUT_ONLY`
- 比較器：`method usePhantasmSpell([BLlocal/lito381/server/BattleSpellCatalog$PhantasmSpell;I)V: left=319:[510:java/lang/Throwable  ] right=324:[517:java/lang/Throwable  ]; paths rejected: normal control-flow cycle; labelled CFGs differ`

```diff
@@ -1,5 +1,5 @@
 META 2 null [java/io/IOException]
-TRY [319:[510:java/lang/Throwable  ], 322:[510:java/lang/Throwable  ], 324:[510:java/lang/Throwable  ], 325:[510:java/lang/Throwable  ], 327:[510:java/lang/Throwable  ], 328:[510:java/lang/Throwable  ], 329:[510:java/lang/Throwable  ], 330:[510:java/lang/Throwable  ], 333:[510:java/lang/Throwable  ], 337:[510:java/lang/Throwable  ], 340:[510:java/lang/Throwable  ], 342:[510:java/lang/Throwable  ], 345:[510:java/lang/Throwable  ], 349:[510:java/lang/Throwable  ], 353:[510:java/lang/Throwable  ], 356:[510:java/lang/Throwable  ], 360:[510:java/lang/Throwable  ], 365:[510:java/lang/Throwable  ], 370:[510:java/lang/Throwable  ], 373:[510:java/lang/Throwable  ], 381:[510:java/lang/Throwable  ], 399:[510:java/lang/Throwable  ], 401:[510:java/lang/Throwable  ], 409:[510:java/lang/Throwable  ], 410:[510:java/lang/Throwable  ], 411:[510:java/lang/Throwable  ], 412:[510:java/lang/Throwable  ], 417:[510:java/lang/Throwable  ], 419:[510:java/lang/Throwable  ], 428:[510:java/lang/Throwable  ], 436:[510:java/lang/Throwable  ], 442:[510:java/lang/Throwable  ], 443:[510:java/lang/Throwable  ], 445:[510:java/lang/Throwable  ], 446:[510:java/lang/Throwable  ], 448:[510:java/lang/Throwable  ], 450:[510:java/lang/Throwable  ], 452:[510:java/lang/Throwable  ], 454:[510:java/lang/Throwable  ], 456:[510:java/lang/Throwable  ], 462:[510:java/lang/Throwable  ], 464:[510:java/lang/Throwable  ], 465:[510:java/lang/Throwable  ], 466:[510:java/lang/Throwable  ], 468:[510:java/lang/Throwable  ], 472:[510:java/lang/Throwable  ], 473:[510:java/lang/Throwable  ], 474:[510:java/lang/Throwable  ], 477:[510:java/lang/Throwable  ], 479:[510:java/lang/Throwable  ], 481:[510:java/lang/Throwable  ], 485:[510:java/lang/Throwable  ], 488:[510:java/lang/Throwable  ], 489:[510:java/lang/Throwable  ], 491:[510:java/lang/Throwable  ], 494:[510:java/lang/Throwable  ], 498:[510:java/lang/Throwable  ], 501:[510:java/lang/Throwable  ], 506:[510:java/lang/Throwable  ], 508:[510:java/lang/Throwable  ], 512:[510:java/lang/Throwable  ]]
+TRY [324:[517:java/lang/Throwable  ], 327:[517:java/lang/Throwable  ], 329:[517:java/lang/Throwable  ], 330:[517:java/lang/Throwable  ], 332:[517:java/lang/Throwable  ], 333:[517:java/lang/Throwable  ], 334:[517:java/lang/Throwable  ], 335:[517:java/lang/Throwable  ], 338:[517:java/lang/Throwable  ], 342:[517:java/lang/Throwable  ], 345:[517:java/lang/Throwable  ], 347:[517:java/lang/Throwable  ], 350:[517:java/lang/Throwable  ], 354:[517:java/lang/Throwable  ], 358:[517:java/lang/Throwable  ], 361:[517:java/lang/Throwable  ], 365:[517:java/lang/Throwable  ], 370:[517:java/lang/Throwable  ], 375:[517:java/lang/Throwable  ], 378:[517:java/lang/Throwable  ], 386:[517:java/lang/Throwable  ], 404:[517:java/lang/Throwable  ], 406:[517:java/lang/Throwable  ], 414:[517:java/lang/Throwable  ], 415:[517:java/lang/Throwable  ], 416:[517:java/lang/Throwable  ], 417:[517:java/lang/Throwable  ], 422:[517:java/lang/Throwable  ], 424:[517:java/lang/Throwable  ], 433:[517:java/lang/Throwable  ], 441:[517:java/lang/Throwable  ], 447:[517:java/lang/Throwable  ], 448:[517:java/lang/Throwable  ], 450:[517:java/lang/Throwable  ], 451:[517:java/lang/Throwable  ], 453:[517:java/lang/Throwable  ], 455:[517:java/lang/Throwable  ], 457:[517:java/lang/Throwable  ], 459:[517:java/lang/Throwable  ], 461:[517:java/lang/Throwable  ], 467:[517:java/lang/Throwable  ], 469:[517:java/lang/Throwable  ], 470:[517:java/lang/Throwable  ], 471:[517:java/lang/Throwable  ], 473:[517:java/lang/Throwable  ], 477:[517:java/lang/Throwable  ], 478:[517:java/lang/Throwable  ], 479:[517:java/lang/Throwable  ], 482:[517:java/lang/Throwable  ], 484:[517:java/lang/Throwable  ], 486:[517:java/lang/Throwable  ], 489:[517:java/lang/Throwable  ], 493:[517:java/lang/Throwable  ], 496:[517:java/lang/Throwable  ], 498:[517:java/lang/Throwable  ], 501:[517:java/lang/Throwable  ], 505:[517:java/lang/Throwable  ], 508:[517:java/lang/Throwable  ], 513:[517:java/lang/Throwable  ], 515:[517:java/lang/Throwable  ], 519:[517:java/lang/Throwable  ]]
 ILOAD 3
 IFLT L5
 ILOAD 3
```

### `usePoisonResistanceSpell([BLlocal/lito381/server/BattleSpellCatalog$PoisonResistanceSpell;)V`

- 類別：`local/lito381/server/ClientSession`
- 類型：例外表涵蓋、含迴圈（路徑比較器不支援）；事件後繼：`LAYOUT_ONLY`
- 比較器：`method usePoisonResistanceSpell([BLlocal/lito381/server/BattleSpellCatalog$PoisonResistanceSpell;)V: left=160:[291:java/lang/Throwable  ] right=158:[289:java/lang/Throwable  ]; paths rejected: normal control-flow cycle; labelled CFGs differ`

```diff
@@ -1,5 +1,5 @@
 META 2 null [java/io/IOException]
-TRY [160:[291:java/lang/Throwable  ], 162:[291:java/lang/Throwable  ], 163:[291:java/lang/Throwable  ], 165:[291:java/lang/Throwable  ], 166:[291:java/lang/Throwable  ], 167:[291:java/lang/Throwable  ], 168:[291:java/lang/Throwable  ], 175:[291:java/lang/Throwable  ], 179:[291:java/lang/Throwable  ], 183:[291:java/lang/Throwable  ], 186:[291:java/lang/Throwable  ], 189:[291:java/lang/Throwable  ], 192:[291:java/lang/Throwable  ], 198:[291:java/lang/Throwable  ], 202:[291:java/lang/Throwable  ], 206:[291:java/lang/Throwable  ], 212:[291:java/lang/Throwable  ], 213:[291:java/lang/Throwable  ], 215:[291:java/lang/Throwable  ], 216:[291:java/lang/Throwable  ], 218:[291:java/lang/Throwable  ], 220:[291:java/lang/Throwable  ], 226:[291:java/lang/Throwable  ], 228:[291:java/lang/Throwable  ], 229:[291:java/lang/Throwable  ], 231:[291:java/lang/Throwable  ], 232:[291:java/lang/Throwable  ], 233:[291:java/lang/Throwable  ], 235:[291:java/lang/Throwable  ], 239:[291:java/lang/Throwable  ], 240:[291:java/lang/Throwable  ], 241:[291:java/lang/Throwable  ], 243:[291:java/lang/Throwable  ], 245:[291:java/lang/Throwable  ], 248:[291:java/lang/Throwable  ], 251:[291:java/lang/Throwable  ], 252:[291:java/lang/Throwable  ], 258:[291:java/lang/Throwable  ], 261:[291:java/lang/Throwable  ], 262:[291:java/lang/Throwable  ], 264:[291:java/lang/Throwable  ], 267:[291:java/lang/Throwable  ], 269:[291:java/lang/Throwable  ], 270:[291:java/lang/Throwable  ], 272:[291:java/lang/Throwable  ], 273:[291:java/lang/Throwable  ], 274:[291:java/lang/Throwable  ], 275:[291:java/lang/Throwable  ], 276:[291:java/lang/Throwable  ], 280:[291:java/lang/Throwable  ], 287:[291:java/lang/Throwable  ], 289:[291:java/lang/Throwable  ], 293:[291:java/lang/Throwable  ]]
+TRY [158:[289:java/lang/Throwable  ], 160:[289:java/lang/Throwable  ], 161:[289:java/lang/Throwable  ], 163:[289:java/lang/Throwable  ], 164:[289:java/lang/Throwable  ], 165:[289:java/lang/Throwable  ], 166:[289:java/lang/Throwable  ], 173:[289:java/lang/Throwable  ], 177:[289:java/lang/Throwable  ], 181:[289:java/lang/Throwable  ], 184:[289:java/lang/Throwable  ], 187:[289:java/lang/Throwable  ], 190:[289:java/lang/Throwable  ], 196:[289:java/lang/Throwable  ], 200:[289:java/lang/Throwable  ], 204:[289:java/lang/Throwable  ], 210:[289:java/lang/Throwable  ], 211:[289:java/lang/Throwable  ], 213:[289:java/lang/Throwable  ], 214:[289:java/lang/Throwable  ], 216:[289:java/lang/Throwable  ], 218:[289:java/lang/Throwable  ], 224:[289:java/lang/Throwable  ], 226:[289:java/lang/Throwable  ], 227:[289:java/lang/Throwable  ], 229:[289:java/lang/Throwable  ], 230:[289:java/lang/Throwable  ], 231:[289:java/lang/Throwable  ], 233:[289:java/lang/Throwable  ], 237:[289:java/lang/Throwable  ], 238:[289:java/lang/Throwable  ], 239:[289:java/lang/Throwable  ], 241:[289:java/lang/Throwable  ], 243:[289:java/lang/Throwable  ], 246:[289:java/lang/Throwable  ], 249:[289:java/lang/Throwable  ], 250:[289:java/lang/Throwable  ], 253:[289:java/lang/Throwable  ], 257:[289:java/lang/Throwable  ], 260:[289:java/lang/Throwable  ], 262:[289:java/lang/Throwable  ], 265:[289:java/lang/Throwable  ], 267:[289:java/lang/Throwable  ], 268:[289:java/lang/Throwable  ], 270:[289:java/lang/Throwable  ], 271:[289:java/lang/Throwable  ], 272:[289:java/lang/Throwable  ], 273:[289:java/lang/Throwable  ], 274:[289:java/lang/Throwable  ], 278:[289:java/lang/Throwable  ], 285:[289:java/lang/Throwable  ], 287:[289:java/lang/Throwable  ], 291:[289:java/lang/Throwable  ]]
 ALOAD 1
 INVOKESTATIC local/lito381/server/Protocol.selectorOnlySpellRequest([B)Llocal/lito381/server/Protocol$SpellSelector;
 ASTORE 3
```

### `useRangedEvasionBuffSpell([BLlocal/lito381/server/BattleSpellCatalog$RangedEvasionBuffSpell;)V`

- 類別：`local/lito381/server/ClientSession`
- 類型：例外表涵蓋、含迴圈（路徑比較器不支援）；事件後繼：`LAYOUT_ONLY`
- 比較器：`method useRangedEvasionBuffSpell([BLlocal/lito381/server/BattleSpellCatalog$RangedEvasionBuffSpell;)V: left=157:[285:java/lang/Throwable  ] right=158:[288:java/lang/Throwable  ]; paths rejected: normal control-flow cycle; labelled CFGs differ`

```diff
@@ -1,5 +1,5 @@
 META 2 null [java/io/IOException]
-TRY [157:[285:java/lang/Throwable  ], 160:[285:java/lang/Throwable  ], 162:[285:java/lang/Throwable  ], 163:[285:java/lang/Throwable  ], 165:[285:java/lang/Throwable  ], 166:[285:java/lang/Throwable  ], 167:[285:java/lang/Throwable  ], 168:[285:java/lang/Throwable  ], 171:[285:java/lang/Throwable  ], 175:[285:java/lang/Throwable  ], 178:[285:java/lang/Throwable  ], 184:[285:java/lang/Throwable  ], 188:[285:java/lang/Throwable  ], 202:[285:java/lang/Throwable  ], 205:[285:java/lang/Throwable  ], 207:[285:java/lang/Throwable  ], 209:[285:java/lang/Throwable  ], 211:[285:java/lang/Throwable  ], 217:[285:java/lang/Throwable  ], 218:[285:java/lang/Throwable  ], 219:[285:java/lang/Throwable  ], 221:[285:java/lang/Throwable  ], 224:[285:java/lang/Throwable  ], 230:[285:java/lang/Throwable  ], 231:[285:java/lang/Throwable  ], 233:[285:java/lang/Throwable  ], 235:[285:java/lang/Throwable  ], 238:[285:java/lang/Throwable  ], 240:[285:java/lang/Throwable  ], 241:[285:java/lang/Throwable  ], 249:[285:java/lang/Throwable  ], 250:[285:java/lang/Throwable  ], 251:[285:java/lang/Throwable  ], 253:[285:java/lang/Throwable  ], 256:[285:java/lang/Throwable  ], 258:[285:java/lang/Throwable  ], 259:[285:java/lang/Throwable  ], 263:[285:java/lang/Throwable  ], 266:[285:java/lang/Throwable  ], 267:[285:java/lang/Throwable  ], 269:[285:java/lang/Throwable  ], 273:[285:java/lang/Throwable  ], 276:[285:java/lang/Throwable  ], 281:[285:java/lang/Throwable  ], 283:[285:java/lang/Throwable  ], 287:[285:java/lang/Throwable  ]]
+TRY [158:[288:java/lang/Throwable  ], 161:[288:java/lang/Throwable  ], 163:[288:java/lang/Throwable  ], 164:[288:java/lang/Throwable  ], 166:[288:java/lang/Throwable  ], 167:[288:java/lang/Throwable  ], 168:[288:java/lang/Throwable  ], 169:[288:java/lang/Throwable  ], 172:[288:java/lang/Throwable  ], 176:[288:java/lang/Throwable  ], 179:[288:java/lang/Throwable  ], 185:[288:java/lang/Throwable  ], 189:[288:java/lang/Throwable  ], 203:[288:java/lang/Throwable  ], 206:[288:java/lang/Throwable  ], 208:[288:java/lang/Throwable  ], 210:[288:java/lang/Throwable  ], 212:[288:java/lang/Throwable  ], 218:[288:java/lang/Throwable  ], 219:[288:java/lang/Throwable  ], 220:[288:java/lang/Throwable  ], 222:[288:java/lang/Throwable  ], 225:[288:java/lang/Throwable  ], 231:[288:java/lang/Throwable  ], 232:[288:java/lang/Throwable  ], 234:[288:java/lang/Throwable  ], 236:[288:java/lang/Throwable  ], 239:[288:java/lang/Throwable  ], 241:[288:java/lang/Throwable  ], 242:[288:java/lang/Throwable  ], 250:[288:java/lang/Throwable  ], 251:[288:java/lang/Throwable  ], 252:[288:java/lang/Throwable  ], 254:[288:java/lang/Throwable  ], 257:[288:java/lang/Throwable  ], 259:[288:java/lang/Throwable  ], 260:[288:java/lang/Throwable  ], 263:[288:java/lang/Throwable  ], 267:[288:java/lang/Throwable  ], 270:[288:java/lang/Throwable  ], 272:[288:java/lang/Throwable  ], 276:[288:java/lang/Throwable  ], 279:[288:java/lang/Throwable  ], 284:[288:java/lang/Throwable  ], 286:[288:java/lang/Throwable  ], 290:[288:java/lang/Throwable  ]]
 ALOAD 1
 INVOKESTATIC local/lito381/server/Protocol.selectorOnlySpellRequest([B)Llocal/lito381/server/Protocol$SpellSelector;
 ASTORE 3
```

### `useReductionArmorSpell([BLlocal/lito381/server/BattleSpellCatalog$ReductionArmorSpell;)V`

- 類別：`local/lito381/server/ClientSession`
- 類型：例外表涵蓋、含迴圈（路徑比較器不支援）；事件後繼：`LAYOUT_ONLY`
- 比較器：`method useReductionArmorSpell([BLlocal/lito381/server/BattleSpellCatalog$ReductionArmorSpell;)V: left=118:[264:java/lang/Throwable  ] right=119:[267:java/lang/Throwable  ]; paths rejected: normal control-flow cycle; labelled CFGs differ`

```diff
@@ -1,5 +1,5 @@
 META 2 null [java/io/IOException]
-TRY [118:[264:java/lang/Throwable  ], 120:[264:java/lang/Throwable  ], 121:[264:java/lang/Throwable  ], 123:[264:java/lang/Throwable  ], 124:[264:java/lang/Throwable  ], 125:[264:java/lang/Throwable  ], 126:[264:java/lang/Throwable  ], 129:[264:java/lang/Throwable  ], 133:[264:java/lang/Throwable  ], 136:[264:java/lang/Throwable  ], 139:[264:java/lang/Throwable  ], 143:[264:java/lang/Throwable  ], 147:[264:java/lang/Throwable  ], 150:[264:java/lang/Throwable  ], 154:[264:java/lang/Throwable  ], 159:[264:java/lang/Throwable  ], 164:[264:java/lang/Throwable  ], 167:[264:java/lang/Throwable  ], 180:[264:java/lang/Throwable  ], 182:[264:java/lang/Throwable  ], 184:[264:java/lang/Throwable  ], 186:[264:java/lang/Throwable  ], 191:[264:java/lang/Throwable  ], 193:[264:java/lang/Throwable  ], 194:[264:java/lang/Throwable  ], 196:[264:java/lang/Throwable  ], 197:[264:java/lang/Throwable  ], 198:[264:java/lang/Throwable  ], 200:[264:java/lang/Throwable  ], 203:[264:java/lang/Throwable  ], 205:[264:java/lang/Throwable  ], 211:[264:java/lang/Throwable  ], 220:[264:java/lang/Throwable  ], 221:[264:java/lang/Throwable  ], 222:[264:java/lang/Throwable  ], 224:[264:java/lang/Throwable  ], 227:[264:java/lang/Throwable  ], 228:[264:java/lang/Throwable  ], 231:[264:java/lang/Throwable  ], 233:[264:java/lang/Throwable  ], 235:[264:java/lang/Throwable  ], 239:[264:java/lang/Throwable  ], 242:[264:java/lang/Throwable  ], 243:[264:java/lang/Throwable  ], 246:[264:java/lang/Throwable  ], 249:[264:java/lang/Throwable  ], 254:[264:java/lang/Throwable  ], 256:[264:java/lang/Throwable  ], 259:[264:java/lang/Throwable  ], 262:[264:java/lang/Throwable  ], 266:[264:java/lang/Throwable  ]]
+TRY [119:[267:java/lang/Throwable  ], 121:[267:java/lang/Throwable  ], 122:[267:java/lang/Throwable  ], 124:[267:java/lang/Throwable  ], 125:[267:java/lang/Throwable  ], 126:[267:java/lang/Throwable  ], 127:[267:java/lang/Throwable  ], 130:[267:java/lang/Throwable  ], 134:[267:java/lang/Throwable  ], 137:[267:java/lang/Throwable  ], 140:[267:java/lang/Throwable  ], 144:[267:java/lang/Throwable  ], 148:[267:java/lang/Throwable  ], 151:[267:java/lang/Throwable  ], 155:[267:java/lang/Throwable  ], 160:[267:java/lang/Throwable  ], 165:[267:java/lang/Throwable  ], 168:[267:java/lang/Throwable  ], 181:[267:java/lang/Throwable  ], 183:[267:java/lang/Throwable  ], 185:[267:java/lang/Throwable  ], 187:[267:java/lang/Throwable  ], 192:[267:java/lang/Throwable  ], 194:[267:java/lang/Throwable  ], 195:[267:java/lang/Throwable  ], 197:[267:java/lang/Throwable  ], 198:[267:java/lang/Throwable  ], 199:[267:java/lang/Throwable  ], 201:[267:java/lang/Throwable  ], 204:[267:java/lang/Throwable  ], 206:[267:java/lang/Throwable  ], 212:[267:java/lang/Throwable  ], 221:[267:java/lang/Throwable  ], 222:[267:java/lang/Throwable  ], 223:[267:java/lang/Throwable  ], 225:[267:java/lang/Throwable  ], 228:[267:java/lang/Throwable  ], 229:[267:java/lang/Throwable  ], 232:[267:java/lang/Throwable  ], 234:[267:java/lang/Throwable  ], 236:[267:java/lang/Throwable  ], 239:[267:java/lang/Throwable  ], 243:[267:java/lang/Throwable  ], 246:[267:java/lang/Throwable  ], 249:[267:java/lang/Throwable  ], 252:[267:java/lang/Throwable  ], 257:[267:java/lang/Throwable  ], 259:[267:java/lang/Throwable  ], 262:[267:java/lang/Throwable  ], 265:[267:java/lang/Throwable  ], 269:[267:java/lang/Throwable  ]]
 ALOAD 1
 INVOKESTATIC local/lito381/server/Protocol.selectorOnlySpellRequest([B)Llocal/lito381/server/Protocol$SpellSelector;
 ASTORE 3
```

### `useRegenSpell([BLlocal/lito381/server/BattleSpellCatalog$RegenSpell;)V`

- 類別：`local/lito381/server/ClientSession`
- 類型：例外表涵蓋、含迴圈（路徑比較器不支援）；事件後繼：`LAYOUT_ONLY`
- 比較器：`method useRegenSpell([BLlocal/lito381/server/BattleSpellCatalog$RegenSpell;)V: left=149:[382:java/lang/Throwable  ] right=145:[376:java/lang/Throwable  ]; paths rejected: normal control-flow cycle; labelled CFGs differ`

```diff
@@ -1,5 +1,5 @@
 META 2 null [java/io/IOException]
-TRY [149:[382:java/lang/Throwable  ], 151:[374:java/lang/Throwable  , 382:java/lang/Throwable  ], 153:[374:java/lang/Throwable  , 382:java/lang/Throwable  ], 154:[374:java/lang/Throwable  , 382:java/lang/Throwable  ], 156:[374:java/lang/Throwable  , 382:java/lang/Throwable  ], 157:[374:java/lang/Throwable  , 382:java/lang/Throwable  ], 158:[374:java/lang/Throwable  , 382:java/lang/Throwable  ], 159:[374:java/lang/Throwable  , 382:java/lang/Throwable  ], 162:[374:java/lang/Throwable  , 382:java/lang/Throwable  ], 165:[374:java/lang/Throwable  , 382:java/lang/Throwable  ], 166:[374:java/lang/Throwable  , 382:java/lang/Throwable  ], 169:[374:java/lang/Throwable  , 382:java/lang/Throwable  ], 174:[374:java/lang/Throwable  , 382:java/lang/Throwable  ], 175:[374:java/lang/Throwable  , 382:java/lang/Throwable  ], 178:[374:java/lang/Throwable  , 382:java/lang/Throwable  ], 187:[374:java/lang/Throwable  , 382:java/lang/Throwable  ], 188:[374:java/lang/Throwable  , 382:java/lang/Throwable  ], 191:[374:java/lang/Throwable  , 382:java/lang/Throwable  ], 195:[374:java/lang/Throwable  , 382:java/lang/Throwable  ], 199:[374:java/lang/Throwable  , 382:java/lang/Throwable  ], 200:[374:java/lang/Throwable  , 382:java/lang/Throwable  ], 203:[374:java/lang/Throwable  , 382:java/lang/Throwable  ], 207:[374:java/lang/Throwable  , 382:java/lang/Throwable  ], 218:[374:java/lang/Throwable  , 382:java/lang/Throwable  ], 221:[374:java/lang/Throwable  , 382:java/lang/Throwable  ], 224:[374:java/lang/Throwable  , 382:java/lang/Throwable  ], 227:[374:java/lang/Throwable  , 382:java/lang/Throwable  ], 229:[374:java/lang/Throwable  , 382:java/lang/Throwable  ], 234:[374:java/lang/Throwable  , 382:java/lang/Throwable  ], 236:[374:java/lang/Throwable  , 382:java/lang/Throwable  ], 237:[374:java/lang/Throwable  , 382:java/lang/Throwable  ], 239:[374:java/lang/Throwable  , 382:java/lang/Throwable  ], 242:[374:java/lang/Throwable  , 382:java/lang/Throwable  ], 245:[374:java/lang/Throwable  , 382:java/lang/Throwable  ], 249:[374:java/lang/Throwable  , 382:java/lang/Throwable  ], 251:[374:java/lang/Throwable  , 382:java/lang/Throwable  ], 253:[374:java/lang/Throwable  , 382:java/lang/Throwable  ], 254:[374:java/lang/Throwable  , 382:java/lang/Throwable  ], 269:[374:java/lang/Throwable  , 382:java/lang/Throwable  ], 272:[374:java/lang/Throwable  , 382:java/lang/Throwable  ], 276:[374:java/lang/Throwable  , 382:java/lang/Throwable  ], 278:[374:java/lang/Throwable  , 382:java/lang/Throwable  ], 280:[374:java/lang/Throwable  , 382:java/lang/Throwable  ], 282:[374:java/lang/Throwable  , 382:java/lang/Throwable  ], 287:[374:java/lang/Throwable  , 382:java/lang/Throwable  ], 289:[374:java/lang/Throwable  , 382:java/lang/Throwable  ], 290:[374:java/lang/Throwable  , 382:java/lang/Throwable  ], 291:[374:java/lang/Throwable  , 382:java/lang/Throwable  ], 293:[374:java/lang/Throwable  , 382:java/lang/Throwable  ], 295:[382:java/lang/Throwable  ], 298:[374:java/lang/Throwable  , 382:java/lang/Throwable  ], 300:[374:java/lang/Throwable  , 382:java/lang/Throwable  ], 308:[374:java/lang/Throwable  , 382:java/lang/Throwable  ], 317:[374:java/lang/Throwable  , 382:java/lang/Throwable  ], 318:[374:java/lang/Throwable  , 382:java/lang/Throwable  ], 319:[374:java/lang/Throwable  , 382:java/lang/Throwable  ], 321:[374:java/lang/Throwable  , 382:java/lang/Throwable  ], 322:[374:java/lang/Throwable  , 382:java/lang/Throwable  ], 325:[374:java/lang/Throwable  , 382:java/lang/Throwable  ], 328:[374:java/lang/Throwable  , 382:java/lang/Throwable  ], 329:[374:java/lang/Throwable  , 382:java/lang/Throwable  ], 332:[374:java/lang/Throwable  , 382:java/lang/Throwable  ], 335:[374:java/lang/Throwable  , 382:java/lang/Throwable  ], 336:[374:java/lang/Throwable  , 382:java/lang/Throwable  ], 339:[374:java/lang/Throwable  , 382:java/lang/Throwable  ], 341:[374:java/lang/Throwable  , 382:java/lang/Throwable  ], 343:[374:java/lang/Throwable  , 382:java/lang/Throwable  ], 349:[374:java/lang/Throwable  , 382:java/lang/Throwable  ], 352:[374:java/lang/Throwable  , 382:java/lang/Throwable  ], 353:[374:java/lang/Throwable  , 382:java/lang/Throwable  ], 356:[374:java/lang/Throwable  , 382:java/lang/Throwable  ], 359:[374:java/lang/Throwable  , 382:java/lang/Throwable  ], 364:[374:java/lang/Throwable  , 382:java/lang/Throwable  ], 366:[374:java/lang/Throwable  , 382:java/lang/Throwable  ], 369:[374:java/lang/Throwable  , 382:java/lang/Throwable  ], 372:[374:java/lang/Throwable  , 382:java/lang/Throwable  ], 376:[374:java/lang/Throwable  , 382:java/lang/Throwable  ], 378:[382:java/lang/Throwable  ], 380:[382:java/lang/Throwable  ], 384:[382:java/lang/Throwable  ]]
+TRY [145:[376:java/lang/Throwable  ], 147:[368:java/lang/Throwable  , 376:java/lang/Throwable  ], 149:[368:java/lang/Throwable  , 376:java/lang/Throwable  ], 150:[368:java/lang/Throwable  , 376:java/lang/Throwable  ], 152:[368:java/lang/Throwable  , 376:java/lang/Throwable  ], 153:[368:java/lang/Throwable  , 376:java/lang/Throwable  ], 154:[368:java/lang/Throwable  , 376:java/lang/Throwable  ], 155:[368:java/lang/Throwable  , 376:java/lang/Throwable  ], 158:[368:java/lang/Throwable  , 376:java/lang/Throwable  ], 161:[368:java/lang/Throwable  , 376:java/lang/Throwable  ], 162:[368:java/lang/Throwable  , 376:java/lang/Throwable  ], 165:[368:java/lang/Throwable  , 376:java/lang/Throwable  ], 170:[368:java/lang/Throwable  , 376:java/lang/Throwable  ], 171:[368:java/lang/Throwable  , 376:java/lang/Throwable  ], 174:[368:java/lang/Throwable  , 376:java/lang/Throwable  ], 183:[368:java/lang/Throwable  , 376:java/lang/Throwable  ], 184:[368:java/lang/Throwable  , 376:java/lang/Throwable  ], 187:[368:java/lang/Throwable  , 376:java/lang/Throwable  ], 191:[368:java/lang/Throwable  , 376:java/lang/Throwable  ], 195:[368:java/lang/Throwable  , 376:java/lang/Throwable  ], 196:[368:java/lang/Throwable  , 376:java/lang/Throwable  ], 199:[368:java/lang/Throwable  , 376:java/lang/Throwable  ], 203:[368:java/lang/Throwable  , 376:java/lang/Throwable  ], 214:[368:java/lang/Throwable  , 376:java/lang/Throwable  ], 217:[368:java/lang/Throwable  , 376:java/lang/Throwable  ], 220:[368:java/lang/Throwable  , 376:java/lang/Throwable  ], 223:[368:java/lang/Throwable  , 376:java/lang/Throwable  ], 225:[368:java/lang/Throwable  , 376:java/lang/Throwable  ], 230:[368:java/lang/Throwable  , 376:java/lang/Throwable  ], 232:[368:java/lang/Throwable  , 376:java/lang/Throwable  ], 233:[368:java/lang/Throwable  , 376:java/lang/Throwable  ], 235:[368:java/lang/Throwable  , 376:java/lang/Throwable  ], 238:[368:java/lang/Throwable  , 376:java/lang/Throwable  ], 241:[368:java/lang/Throwable  , 376:java/lang/Throwable  ], 245:[368:java/lang/Throwable  , 376:java/lang/Throwable  ], 247:[368:java/lang/Throwable  , 376:java/lang/Throwable  ], 249:[368:java/lang/Throwable  , 376:java/lang/Throwable  ], 250:[368:java/lang/Throwable  , 376:java/lang/Throwable  ], 263:[368:java/lang/Throwable  , 376:java/lang/Throwable  ], 266:[368:java/lang/Throwable  , 376:java/lang/Throwable  ], 270:[368:java/lang/Throwable  , 376:java/lang/Throwable  ], 272:[368:java/lang/Throwable  , 376:java/lang/Throwable  ], 274:[368:java/lang/Throwable  , 376:java/lang/Throwable  ], 276:[368:java/lang/Throwable  , 376:java/lang/Throwable  ], 281:[368:java/lang/Throwable  , 376:java/lang/Throwable  ], 283:[368:java/lang/Throwable  , 376:java/lang/Throwable  ], 284:[368:java/lang/Throwable  , 376:java/lang/Throwable  ], 285:[368:java/lang/Throwable  , 376:java/lang/Throwable  ], 287:[368:java/lang/Throwable  , 376:java/lang/Throwable  ], 289:[376:java/lang/Throwable  ], 292:[368:java/lang/Throwable  , 376:java/lang/Throwable  ], 294:[368:java/lang/Throwable  , 376:java/lang/Throwable  ], 302:[368:java/lang/Throwable  , 376:java/lang/Throwable  ], 311:[368:java/lang/Throwable  , 376:java/lang/Throwable  ], 312:[368:java/lang/Throwable  , 376:java/lang/Throwable  ], 313:[368:java/lang/Throwable  , 376:java/lang/Throwable  ], 315:[368:java/lang/Throwable  , 376:java/lang/Throwable  ], 316:[368:java/lang/Throwable  , 376:java/lang/Throwable  ], 319:[368:java/lang/Throwable  , 376:java/lang/Throwable  ], 322:[368:java/lang/Throwable  , 376:java/lang/Throwable  ], 323:[368:java/lang/Throwable  , 376:java/lang/Throwable  ], 326:[368:java/lang/Throwable  , 376:java/lang/Throwable  ], 329:[368:java/lang/Throwable  , 376:java/lang/Throwable  ], 330:[368:java/lang/Throwable  , 376:java/lang/Throwable  ], 333:[368:java/lang/Throwable  , 376:java/lang/Throwable  ], 335:[368:java/lang/Throwable  , 376:java/lang/Throwable  ], 337:[368:java/lang/Throwable  , 376:java/lang/Throwable  ], 340:[368:java/lang/Throwable  , 376:java/lang/Throwable  ], 344:[368:java/lang/Throwable  , 376:java/lang/Throwable  ], 347:[368:java/lang/Throwable  , 376:java/lang/Throwable  ], 350:[368:java/lang/Throwable  , 376:java/lang/Throwable  ], 353:[368:java/lang/Throwable  , 376:java/lang/Throwable  ], 358:[368:java/lang/Throwable  , 376:java/lang/Throwable  ], 360:[368:java/lang/Throwable  , 376:java/lang/Throwable  ], 363:[368:java/lang/Throwable  , 376:java/lang/Throwable  ], 366:[368:java/lang/Throwable  , 376:java/lang/Throwable  ], 370:[368:java/lang/Throwable  , 376:java/lang/Throwable  ], 372:[376:java/lang/Throwable  ], 374:[376:java/lang/Throwable  ], 378:[376:java/lang/Throwable  ]]
 ALOAD 1
 INVOKESTATIC local/lito381/server/Protocol.selectorOnlySpellRequest([B)Llocal/lito381/server/Protocol$SpellSelector;
 ASTORE 3
```

### `useResistFearSpell([BLlocal/lito381/server/BattleSpellCatalog$ResistFearSpell;)V`

- 類別：`local/lito381/server/ClientSession`
- 類型：例外表涵蓋、含迴圈（路徑比較器不支援）；事件後繼：`LAYOUT_ONLY`
- 比較器：`method useResistFearSpell([BLlocal/lito381/server/BattleSpellCatalog$ResistFearSpell;)V: left=134:[139:java/lang/Exception  ] right=136:[141:java/lang/Exception  ]; paths rejected: normal control-flow cycle; labelled CFGs differ`

```diff
@@ -1,5 +1,5 @@
 META 2 null [java/io/IOException]
-TRY [134:[139:java/lang/Exception  ], 136:[139:java/lang/Exception  ], 326:[469:java/lang/Throwable  ], 329:[469:java/lang/Throwable  ], 331:[469:java/lang/Throwable  ], 332:[469:java/lang/Throwable  ], 334:[469:java/lang/Throwable  ], 335:[469:java/lang/Throwable  ], 336:[469:java/lang/Throwable  ], 337:[469:java/lang/Throwable  ], 340:[469:java/lang/Throwable  ], 344:[469:java/lang/Throwable  ], 347:[469:java/lang/Throwable  ], 350:[469:java/lang/Throwable  ], 354:[469:java/lang/Throwable  ], 360:[469:java/lang/Throwable  ], 364:[469:java/lang/Throwable  ], 369:[469:java/lang/Throwable  ], 374:[469:java/lang/Throwable  ], 377:[469:java/lang/Throwable  ], 390:[469:java/lang/Throwable  ], 392:[469:java/lang/Throwable  ], 394:[469:java/lang/Throwable  ], 396:[469:java/lang/Throwable  ], 402:[469:java/lang/Throwable  ], 404:[469:java/lang/Throwable  ], 405:[469:java/lang/Throwable  ], 407:[469:java/lang/Throwable  ], 408:[469:java/lang/Throwable  ], 409:[469:java/lang/Throwable  ], 411:[469:java/lang/Throwable  ], 414:[469:java/lang/Throwable  ], 416:[469:java/lang/Throwable  ], 422:[469:java/lang/Throwable  ], 431:[469:java/lang/Throwable  ], 432:[469:java/lang/Throwable  ], 433:[469:java/lang/Throwable  ], 436:[469:java/lang/Throwable  ], 438:[469:java/lang/Throwable  ], 440:[469:java/lang/Throwable  ], 444:[469:java/lang/Throwable  ], 447:[469:java/lang/Throwable  ], 448:[469:java/lang/Throwable  ], 451:[469:java/lang/Throwable  ], 454:[469:java/lang/Throwable  ], 459:[469:java/lang/Throwable  ], 461:[469:java/lang/Throwable  ], 464:[469:java/lang/Throwable  ], 467:[469:java/lang/Throwable  ], 471:[469:java/lang/Throwable  ]]
+TRY [136:[141:java/lang/Exception  ], 138:[141:java/lang/Exception  ], 328:[473:java/lang/Throwable  ], 331:[473:java/lang/Throwable  ], 333:[473:java/lang/Throwable  ], 334:[473:java/lang/Throwable  ], 336:[473:java/lang/Throwable  ], 337:[473:java/lang/Throwable  ], 338:[473:java/lang/Throwable  ], 339:[473:java/lang/Throwable  ], 342:[473:java/lang/Throwable  ], 346:[473:java/lang/Throwable  ], 349:[473:java/lang/Throwable  ], 352:[473:java/lang/Throwable  ], 356:[473:java/lang/Throwable  ], 362:[473:java/lang/Throwable  ], 366:[473:java/lang/Throwable  ], 371:[473:java/lang/Throwable  ], 376:[473:java/lang/Throwable  ], 379:[473:java/lang/Throwable  ], 392:[473:java/lang/Throwable  ], 394:[473:java/lang/Throwable  ], 396:[473:java/lang/Throwable  ], 398:[473:java/lang/Throwable  ], 404:[473:java/lang/Throwable  ], 406:[473:java/lang/Throwable  ], 407:[473:java/lang/Throwable  ], 409:[473:java/lang/Throwable  ], 410:[473:java/lang/Throwable  ], 411:[473:java/lang/Throwable  ], 413:[473:java/lang/Throwable  ], 416:[473:java/lang/Throwable  ], 418:[473:java/lang/Throwable  ], 424:[473:java/lang/Throwable  ], 433:[473:java/lang/Throwable  ], 434:[473:java/lang/Throwable  ], 435:[473:java/lang/Throwable  ], 438:[473:java/lang/Throwable  ], 440:[473:java/lang/Throwable  ], 442:[473:java/lang/Throwable  ], 445:[473:java/lang/Throwable  ], 449:[473:java/lang/Throwable  ], 452:[473:java/lang/Throwable  ], 455:[473:java/lang/Throwable  ], 458:[473:java/lang/Throwable  ], 463:[473:java/lang/Throwable  ], 465:[473:java/lang/Throwable  ], 468:[473:java/lang/Throwable  ], 471:[473:java/lang/Throwable  ], 475:[473:java/lang/Throwable  ]]
 ALOAD 1
 INVOKESTATIC local/lito381/server/Protocol.selectorOnlySpellRequest([B)Llocal/lito381/server/Protocol$SpellSelector;
 ASTORE 3
```

### `useRevealWeaknessSpell([BLlocal/lito381/server/BattleSpellCatalog$RevealWeaknessSpell;)V`

- 類別：`local/lito381/server/ClientSession`
- 類型：例外表涵蓋、含迴圈（路徑比較器不支援）；事件後繼：`LAYOUT_ONLY`
- 比較器：`method useRevealWeaknessSpell([BLlocal/lito381/server/BattleSpellCatalog$RevealWeaknessSpell;)V: left=252:[257:java/lang/Exception  ] right=255:[260:java/lang/Exception  ]; paths rejected: normal control-flow cycle; labelled CFGs differ`

```diff
@@ -1,5 +1,5 @@
 META 2 null [java/io/IOException]
-TRY [252:[257:java/lang/Exception  ], 254:[257:java/lang/Exception  ], 313:[424:java/lang/Throwable  ], 316:[424:java/lang/Throwable  ], 318:[424:java/lang/Throwable  ], 319:[424:java/lang/Throwable  ], 321:[424:java/lang/Throwable  ], 322:[424:java/lang/Throwable  ], 323:[424:java/lang/Throwable  ], 324:[424:java/lang/Throwable  ], 327:[424:java/lang/Throwable  ], 331:[424:java/lang/Throwable  ], 334:[424:java/lang/Throwable  ], 337:[424:java/lang/Throwable  ], 340:[424:java/lang/Throwable  ], 344:[424:java/lang/Throwable  ], 349:[424:java/lang/Throwable  ], 364:[424:java/lang/Throwable  ], 368:[424:java/lang/Throwable  ], 374:[424:java/lang/Throwable  ], 375:[424:java/lang/Throwable  ], 377:[424:java/lang/Throwable  ], 378:[424:java/lang/Throwable  ], 380:[424:java/lang/Throwable  ], 382:[424:java/lang/Throwable  ], 388:[424:java/lang/Throwable  ], 389:[424:java/lang/Throwable  ], 390:[424:java/lang/Throwable  ], 392:[424:java/lang/Throwable  ], 396:[424:java/lang/Throwable  ], 397:[424:java/lang/Throwable  ], 398:[424:java/lang/Throwable  ], 402:[424:java/lang/Throwable  ], 405:[424:java/lang/Throwable  ], 406:[424:java/lang/Throwable  ], 408:[424:java/lang/Throwable  ], 412:[424:java/lang/Throwable  ], 415:[424:java/lang/Throwable  ], 420:[424:java/lang/Throwable  ], 422:[424:java/lang/Throwable  ], 426:[424:java/lang/Throwable  ]]
+TRY [255:[260:java/lang/Exception  ], 257:[260:java/lang/Exception  ], 316:[429:java/lang/Throwable  ], 319:[429:java/lang/Throwable  ], 321:[429:java/lang/Throwable  ], 322:[429:java/lang/Throwable  ], 324:[429:java/lang/Throwable  ], 325:[429:java/lang/Throwable  ], 326:[429:java/lang/Throwable  ], 327:[429:java/lang/Throwable  ], 330:[429:java/lang/Throwable  ], 334:[429:java/lang/Throwable  ], 337:[429:java/lang/Throwable  ], 340:[429:java/lang/Throwable  ], 343:[429:java/lang/Throwable  ], 347:[429:java/lang/Throwable  ], 352:[429:java/lang/Throwable  ], 367:[429:java/lang/Throwable  ], 371:[429:java/lang/Throwable  ], 377:[429:java/lang/Throwable  ], 378:[429:java/lang/Throwable  ], 380:[429:java/lang/Throwable  ], 381:[429:java/lang/Throwable  ], 383:[429:java/lang/Throwable  ], 385:[429:java/lang/Throwable  ], 391:[429:java/lang/Throwable  ], 392:[429:java/lang/Throwable  ], 393:[429:java/lang/Throwable  ], 395:[429:java/lang/Throwable  ], 399:[429:java/lang/Throwable  ], 400:[429:java/lang/Throwable  ], 401:[429:java/lang/Throwable  ], 404:[429:java/lang/Throwable  ], 408:[429:java/lang/Throwable  ], 411:[429:java/lang/Throwable  ], 413:[429:java/lang/Throwable  ], 417:[429:java/lang/Throwable  ], 420:[429:java/lang/Throwable  ], 425:[429:java/lang/Throwable  ], 427:[429:java/lang/Throwable  ], 431:[429:java/lang/Throwable  ]]
 ALOAD 1
 INVOKESTATIC local/lito381/server/Protocol.objectSpellRequest([B)Llocal/lito381/server/Protocol$ObjectSpellRequest;
 ALOAD 0
```

### `useRoyalAuraSpell([BLlocal/lito381/server/BattleSpellCatalog$RoyalAuraSpell;)V`

- 類別：`local/lito381/server/ClientSession`
- 類型：例外表涵蓋、含迴圈（路徑比較器不支援）；事件後繼：`LAYOUT_ONLY`
- 比較器：`method useRoyalAuraSpell([BLlocal/lito381/server/BattleSpellCatalog$RoyalAuraSpell;)V: left=227:[338:java/lang/Throwable  ] right=231:[344:java/lang/Throwable  ]; paths rejected: normal control-flow cycle; labelled CFGs differ`

```diff
@@ -1,5 +1,5 @@
 META 2 null [java/io/IOException]
-TRY [227:[338:java/lang/Throwable  ], 229:[338:java/lang/Throwable  ], 230:[338:java/lang/Throwable  ], 232:[338:java/lang/Throwable  ], 233:[338:java/lang/Throwable  ], 234:[338:java/lang/Throwable  ], 235:[338:java/lang/Throwable  ], 238:[338:java/lang/Throwable  ], 242:[338:java/lang/Throwable  ], 245:[338:java/lang/Throwable  ], 248:[338:java/lang/Throwable  ], 252:[338:java/lang/Throwable  ], 258:[338:java/lang/Throwable  ], 262:[338:java/lang/Throwable  ], 267:[338:java/lang/Throwable  ], 272:[338:java/lang/Throwable  ], 275:[338:java/lang/Throwable  ], 287:[338:java/lang/Throwable  ], 292:[338:java/lang/Throwable  ], 294:[338:java/lang/Throwable  ], 296:[338:java/lang/Throwable  ], 302:[338:java/lang/Throwable  ], 303:[338:java/lang/Throwable  ], 304:[338:java/lang/Throwable  ], 306:[338:java/lang/Throwable  ], 310:[338:java/lang/Throwable  ], 311:[338:java/lang/Throwable  ], 312:[338:java/lang/Throwable  ], 316:[338:java/lang/Throwable  ], 319:[338:java/lang/Throwable  ], 320:[338:java/lang/Throwable  ], 322:[338:java/lang/Throwable  ], 326:[338:java/lang/Throwable  ], 329:[338:java/lang/Throwable  ], 334:[338:java/lang/Throwable  ], 336:[338:java/lang/Throwable  ], 340:[338:java/lang/Throwable  ], 395:[433:java/lang/Throwable  ], 398:[433:java/lang/Throwable  ], 400:[433:java/lang/Throwable  ], 401:[433:java/lang/Throwable  ], 405:[433:java/lang/Throwable  ], 406:[433:java/lang/Throwable  ], 412:[433:java/lang/Throwable  ], 414:[433:java/lang/Throwable  ], 417:[433:java/lang/Throwable  ], 419:[433:java/lang/Throwable  ], 421:[433:java/lang/Throwable  ], 423:[433:java/lang/Throwable  ], 425:[433:java/lang/Throwable  ], 426:[433:java/lang/Throwable  ], 427:[433:java/lang/Throwable  ], 428:[433:java/lang/Throwable  ], 431:[433:java/lang/Throwable  ], 435:[433:java/lang/Throwable  ]]
+TRY [231:[344:java/lang/Throwable  ], 233:[344:java/lang/Throwable  ], 234:[344:java/lang/Throwable  ], 236:[344:java/lang/Throwable  ], 237:[344:java/lang/Throwable  ], 238:[344:java/lang/Throwable  ], 239:[344:java/lang/Throwable  ], 242:[344:java/lang/Throwable  ], 246:[344:java/lang/Throwable  ], 249:[344:java/lang/Throwable  ], 252:[344:java/lang/Throwable  ], 256:[344:java/lang/Throwable  ], 262:[344:java/lang/Throwable  ], 266:[344:java/lang/Throwable  ], 271:[344:java/lang/Throwable  ], 276:[344:java/lang/Throwable  ], 279:[344:java/lang/Throwable  ], 291:[344:java/lang/Throwable  ], 296:[344:java/lang/Throwable  ], 298:[344:java/lang/Throwable  ], 300:[344:java/lang/Throwable  ], 306:[344:java/lang/Throwable  ], 307:[344:java/lang/Throwable  ], 308:[344:java/lang/Throwable  ], 310:[344:java/lang/Throwable  ], 314:[344:java/lang/Throwable  ], 315:[344:java/lang/Throwable  ], 316:[344:java/lang/Throwable  ], 319:[344:java/lang/Throwable  ], 323:[344:java/lang/Throwable  ], 326:[344:java/lang/Throwable  ], 328:[344:java/lang/Throwable  ], 332:[344:java/lang/Throwable  ], 335:[344:java/lang/Throwable  ], 340:[344:java/lang/Throwable  ], 342:[344:java/lang/Throwable  ], 346:[344:java/lang/Throwable  ], 401:[439:java/lang/Throwable  ], 404:[439:java/lang/Throwable  ], 406:[439:java/lang/Throwable  ], 407:[439:java/lang/Throwable  ], 411:[439:java/lang/Throwable  ], 412:[439:java/lang/Throwable  ], 418:[439:java/lang/Throwable  ], 420:[439:java/lang/Throwable  ], 423:[439:java/lang/Throwable  ], 425:[439:java/lang/Throwable  ], 427:[439:java/lang/Throwable  ], 429:[439:java/lang/Throwable  ], 431:[439:java/lang/Throwable  ], 432:[439:java/lang/Throwable  ], 433:[439:java/lang/Throwable  ], 434:[439:java/lang/Throwable  ], 437:[439:java/lang/Throwable  ], 441:[439:java/lang/Throwable  ]]
 ALOAD 1
 INVOKESTATIC local/lito381/server/Protocol.selectorOnlySpellRequest([B)Llocal/lito381/server/Protocol$SpellSelector;
 ASTORE 3
```

### `useRunClanSpell([BLlocal/lito381/server/BattleSpellCatalog$RunClanSpell;)V`

- 類別：`local/lito381/server/ClientSession`
- 類型：例外表涵蓋、含迴圈（路徑比較器不支援）；事件後繼：`LAYOUT_ONLY`
- 比較器：`method useRunClanSpell([BLlocal/lito381/server/BattleSpellCatalog$RunClanSpell;)V: left=235:[345:java/lang/Throwable  ] right=236:[348:java/lang/Throwable  ]; paths rejected: normal control-flow cycle; labelled CFGs differ`

```diff
@@ -1,5 +1,5 @@
 META 2 null [java/io/IOException]
-TRY [235:[345:java/lang/Throwable  ], 237:[345:java/lang/Throwable  ], 238:[345:java/lang/Throwable  ], 240:[345:java/lang/Throwable  ], 241:[345:java/lang/Throwable  ], 242:[345:java/lang/Throwable  ], 243:[345:java/lang/Throwable  ], 248:[345:java/lang/Throwable  ], 249:[345:java/lang/Throwable  ], 252:[345:java/lang/Throwable  ], 256:[345:java/lang/Throwable  ], 264:[345:java/lang/Throwable  ], 265:[345:java/lang/Throwable  ], 268:[345:java/lang/Throwable  ], 273:[345:java/lang/Throwable  ], 286:[345:java/lang/Throwable  ], 292:[345:java/lang/Throwable  ], 295:[345:java/lang/Throwable  ], 298:[345:java/lang/Throwable  ], 301:[345:java/lang/Throwable  ], 303:[345:java/lang/Throwable  ], 309:[345:java/lang/Throwable  ], 311:[345:java/lang/Throwable  ], 313:[345:java/lang/Throwable  ], 317:[345:java/lang/Throwable  ], 318:[345:java/lang/Throwable  ], 319:[345:java/lang/Throwable  ], 323:[345:java/lang/Throwable  ], 326:[345:java/lang/Throwable  ], 327:[345:java/lang/Throwable  ], 329:[345:java/lang/Throwable  ], 333:[345:java/lang/Throwable  ], 336:[345:java/lang/Throwable  ], 341:[345:java/lang/Throwable  ], 343:[345:java/lang/Throwable  ], 347:[345:java/lang/Throwable  ]]
+TRY [236:[348:java/lang/Throwable  ], 238:[348:java/lang/Throwable  ], 239:[348:java/lang/Throwable  ], 241:[348:java/lang/Throwable  ], 242:[348:java/lang/Throwable  ], 243:[348:java/lang/Throwable  ], 244:[348:java/lang/Throwable  ], 249:[348:java/lang/Throwable  ], 250:[348:java/lang/Throwable  ], 253:[348:java/lang/Throwable  ], 257:[348:java/lang/Throwable  ], 265:[348:java/lang/Throwable  ], 266:[348:java/lang/Throwable  ], 269:[348:java/lang/Throwable  ], 274:[348:java/lang/Throwable  ], 287:[348:java/lang/Throwable  ], 293:[348:java/lang/Throwable  ], 296:[348:java/lang/Throwable  ], 299:[348:java/lang/Throwable  ], 302:[348:java/lang/Throwable  ], 304:[348:java/lang/Throwable  ], 310:[348:java/lang/Throwable  ], 312:[348:java/lang/Throwable  ], 314:[348:java/lang/Throwable  ], 318:[348:java/lang/Throwable  ], 319:[348:java/lang/Throwable  ], 320:[348:java/lang/Throwable  ], 323:[348:java/lang/Throwable  ], 327:[348:java/lang/Throwable  ], 330:[348:java/lang/Throwable  ], 332:[348:java/lang/Throwable  ], 336:[348:java/lang/Throwable  ], 339:[348:java/lang/Throwable  ], 344:[348:java/lang/Throwable  ], 346:[348:java/lang/Throwable  ], 350:[348:java/lang/Throwable  ]]
 ALOAD 0
 GETFIELD local/lito381/server/ClientSession.phase:Llocal/lito381/server/ClientSession$Phase;
 ASTORE 3
```

### `useSelfHasteSpell([BLlocal/lito381/server/BattleSpellCatalog$SelfHasteSpell;)V`

- 類別：`local/lito381/server/ClientSession`
- 類型：例外表涵蓋、含迴圈（路徑比較器不支援）；事件後繼：`LAYOUT_ONLY`
- 比較器：`method useSelfHasteSpell([BLlocal/lito381/server/BattleSpellCatalog$SelfHasteSpell;)V: left=89:[187:java/lang/Throwable  ] right=85:[183:java/lang/Throwable  ]; paths rejected: normal control-flow cycle; labelled CFGs differ`

```diff
@@ -1,5 +1,5 @@
 META 2 null [java/io/IOException]
-TRY [89:[187:java/lang/Throwable  ], 91:[187:java/lang/Throwable  ], 92:[187:java/lang/Throwable  ], 94:[187:java/lang/Throwable  ], 95:[187:java/lang/Throwable  ], 96:[187:java/lang/Throwable  ], 97:[187:java/lang/Throwable  ], 104:[187:java/lang/Throwable  ], 108:[187:java/lang/Throwable  ], 112:[187:java/lang/Throwable  ], 115:[187:java/lang/Throwable  ], 118:[187:java/lang/Throwable  ], 121:[187:java/lang/Throwable  ], 123:[187:java/lang/Throwable  ], 125:[187:java/lang/Throwable  ], 127:[187:java/lang/Throwable  ], 133:[187:java/lang/Throwable  ], 134:[187:java/lang/Throwable  ], 135:[187:java/lang/Throwable  ], 137:[187:java/lang/Throwable  ], 144:[187:java/lang/Throwable  ], 147:[187:java/lang/Throwable  ], 148:[187:java/lang/Throwable  ], 150:[187:java/lang/Throwable  ], 153:[187:java/lang/Throwable  ], 155:[187:java/lang/Throwable  ], 156:[187:java/lang/Throwable  ], 159:[187:java/lang/Throwable  ], 164:[187:java/lang/Throwable  ], 165:[187:java/lang/Throwable  ], 168:[187:java/lang/Throwable  ], 171:[187:java/lang/Throwable  ], 173:[187:java/lang/Throwable  ], 174:[187:java/lang/Throwable  ], 177:[187:java/lang/Throwable  ], 181:[187:java/lang/Throwable  ], 182:[187:java/lang/Throwable  ], 185:[187:java/lang/Throwable  ], 189:[187:java/lang/Throwable  ]]
+TRY [85:[183:java/lang/Throwable  ], 87:[183:java/lang/Throwable  ], 88:[183:java/lang/Throwable  ], 90:[183:java/lang/Throwable  ], 91:[183:java/lang/Throwable  ], 92:[183:java/lang/Throwable  ], 93:[183:java/lang/Throwable  ], 100:[183:java/lang/Throwable  ], 104:[183:java/lang/Throwable  ], 108:[183:java/lang/Throwable  ], 111:[183:java/lang/Throwable  ], 114:[183:java/lang/Throwable  ], 117:[183:java/lang/Throwable  ], 119:[183:java/lang/Throwable  ], 121:[183:java/lang/Throwable  ], 123:[183:java/lang/Throwable  ], 129:[183:java/lang/Throwable  ], 130:[183:java/lang/Throwable  ], 131:[183:java/lang/Throwable  ], 133:[183:java/lang/Throwable  ], 137:[183:java/lang/Throwable  ], 141:[183:java/lang/Throwable  ], 144:[183:java/lang/Throwable  ], 146:[183:java/lang/Throwable  ], 149:[183:java/lang/Throwable  ], 151:[183:java/lang/Throwable  ], 152:[183:java/lang/Throwable  ], 155:[183:java/lang/Throwable  ], 160:[183:java/lang/Throwable  ], 161:[183:java/lang/Throwable  ], 164:[183:java/lang/Throwable  ], 167:[183:java/lang/Throwable  ], 169:[183:java/lang/Throwable  ], 170:[183:java/lang/Throwable  ], 173:[183:java/lang/Throwable  ], 177:[183:java/lang/Throwable  ], 178:[183:java/lang/Throwable  ], 181:[183:java/lang/Throwable  ], 185:[183:java/lang/Throwable  ]]
 ALOAD 1
 INVOKESTATIC local/lito381/server/Protocol.selectorOnlySpellRequest([B)Llocal/lito381/server/Protocol$SpellSelector;
 ASTORE 3
```

### `useSolidCarriageSpell([BLlocal/lito381/server/BattleSpellCatalog$SolidCarriageSpell;)V`

- 類別：`local/lito381/server/ClientSession`
- 類型：例外表涵蓋、含迴圈（路徑比較器不支援）；事件後繼：`LAYOUT_ONLY`
- 比較器：`method useSolidCarriageSpell([BLlocal/lito381/server/BattleSpellCatalog$SolidCarriageSpell;)V: left=130:[283:java/lang/Throwable  ] right=131:[286:java/lang/Throwable  ]; paths rejected: normal control-flow cycle; labelled CFGs differ`

```diff
@@ -1,5 +1,5 @@
 META 2 null [java/io/IOException]
-TRY [130:[283:java/lang/Throwable  ], 132:[283:java/lang/Throwable  ], 133:[283:java/lang/Throwable  ], 135:[283:java/lang/Throwable  ], 136:[283:java/lang/Throwable  ], 137:[283:java/lang/Throwable  ], 138:[283:java/lang/Throwable  ], 141:[283:java/lang/Throwable  ], 145:[283:java/lang/Throwable  ], 148:[283:java/lang/Throwable  ], 151:[283:java/lang/Throwable  ], 155:[283:java/lang/Throwable  ], 159:[283:java/lang/Throwable  ], 162:[283:java/lang/Throwable  ], 166:[283:java/lang/Throwable  ], 171:[283:java/lang/Throwable  ], 176:[283:java/lang/Throwable  ], 179:[283:java/lang/Throwable  ], 193:[283:java/lang/Throwable  ], 195:[283:java/lang/Throwable  ], 197:[283:java/lang/Throwable  ], 199:[283:java/lang/Throwable  ], 201:[283:java/lang/Throwable  ], 203:[283:java/lang/Throwable  ], 208:[283:java/lang/Throwable  ], 210:[283:java/lang/Throwable  ], 211:[283:java/lang/Throwable  ], 213:[283:java/lang/Throwable  ], 214:[283:java/lang/Throwable  ], 215:[283:java/lang/Throwable  ], 217:[283:java/lang/Throwable  ], 220:[283:java/lang/Throwable  ], 222:[283:java/lang/Throwable  ], 228:[283:java/lang/Throwable  ], 237:[283:java/lang/Throwable  ], 238:[283:java/lang/Throwable  ], 239:[283:java/lang/Throwable  ], 241:[283:java/lang/Throwable  ], 244:[283:java/lang/Throwable  ], 246:[283:java/lang/Throwable  ], 247:[283:java/lang/Throwable  ], 250:[283:java/lang/Throwable  ], 252:[283:java/lang/Throwable  ], 254:[283:java/lang/Throwable  ], 258:[283:java/lang/Throwable  ], 261:[283:java/lang/Throwable  ], 262:[283:java/lang/Throwable  ], 265:[283:java/lang/Throwable  ], 268:[283:java/lang/Throwable  ], 273:[283:java/lang/Throwable  ], 275:[283:java/lang/Throwable  ], 278:[283:java/lang/Throwable  ], 281:[283:java/lang/Throwable  ], 285:[283:java/lang/Throwable  ]]
+TRY [131:[286:java/lang/Throwable  ], 133:[286:java/lang/Throwable  ], 134:[286:java/lang/Throwable  ], 136:[286:java/lang/Throwable  ], 137:[286:java/lang/Throwable  ], 138:[286:java/lang/Throwable  ], 139:[286:java/lang/Throwable  ], 142:[286:java/lang/Throwable  ], 146:[286:java/lang/Throwable  ], 149:[286:java/lang/Throwable  ], 152:[286:java/lang/Throwable  ], 156:[286:java/lang/Throwable  ], 160:[286:java/lang/Throwable  ], 163:[286:java/lang/Throwable  ], 167:[286:java/lang/Throwable  ], 172:[286:java/lang/Throwable  ], 177:[286:java/lang/Throwable  ], 180:[286:java/lang/Throwable  ], 194:[286:java/lang/Throwable  ], 196:[286:java/lang/Throwable  ], 198:[286:java/lang/Throwable  ], 200:[286:java/lang/Throwable  ], 202:[286:java/lang/Throwable  ], 204:[286:java/lang/Throwable  ], 209:[286:java/lang/Throwable  ], 211:[286:java/lang/Throwable  ], 212:[286:java/lang/Throwable  ], 214:[286:java/lang/Throwable  ], 215:[286:java/lang/Throwable  ], 216:[286:java/lang/Throwable  ], 218:[286:java/lang/Throwable  ], 221:[286:java/lang/Throwable  ], 223:[286:java/lang/Throwable  ], 229:[286:java/lang/Throwable  ], 238:[286:java/lang/Throwable  ], 239:[286:java/lang/Throwable  ], 240:[286:java/lang/Throwable  ], 242:[286:java/lang/Throwable  ], 245:[286:java/lang/Throwable  ], 247:[286:java/lang/Throwable  ], 248:[286:java/lang/Throwable  ], 251:[286:java/lang/Throwable  ], 253:[286:java/lang/Throwable  ], 255:[286:java/lang/Throwable  ], 258:[286:java/lang/Throwable  ], 262:[286:java/lang/Throwable  ], 265:[286:java/lang/Throwable  ], 268:[286:java/lang/Throwable  ], 271:[286:java/lang/Throwable  ], 276:[286:java/lang/Throwable  ], 278:[286:java/lang/Throwable  ], 281:[286:java/lang/Throwable  ], 284:[286:java/lang/Throwable  ], 288:[286:java/lang/Throwable  ]]
 ALOAD 1
 INVOKESTATIC local/lito381/server/Protocol.selectorOnlySpellRequest([B)Llocal/lito381/server/Protocol$SpellSelector;
 ASTORE 3
```

### `useSpellBeforeDragonTargets([B)V`

- 類別：`local/lito381/server/ClientSession`
- 類型：例外表涵蓋、含迴圈（路徑比較器不支援）；事件後繼：`LAYOUT_ONLY`
- 比較器：`method useSpellBeforeDragonTargets([B)V: left=25:[43:java/lang/Throwable  ] right=29:[51:java/lang/Throwable  ]; paths rejected: normal control-flow cycle; labelled CFGs differ`

```diff
@@ -1,5 +1,5 @@
 META 2 null [java/io/IOException]
-TRY [25:[43:java/lang/Throwable  ], 26:[43:java/lang/Throwable  ], 27:[43:java/lang/Throwable  ], 30:[43:java/lang/Throwable  ], 33:[43:java/lang/Throwable  ], 34:[43:java/lang/Throwable  ], 35:[43:java/lang/Throwable  ], 39:[43:java/lang/Throwable  ]]
+TRY [29:[51:java/lang/Throwable  ], 30:[51:java/lang/Throwable  ], 31:[51:java/lang/Throwable  ], 34:[51:java/lang/Throwable  ], 37:[51:java/lang/Throwable  ], 38:[51:java/lang/Throwable  ], 39:[51:java/lang/Throwable  ], 43:[51:java/lang/Throwable  ]]
 ALOAD 1
 INVOKESTATIC local/lito381/server/Protocol.spellSelector([B)Llocal/lito381/server/Protocol$SpellSelector;
 ASTORE 2
```

### `useStormEyeSpell([BLlocal/lito381/server/BattleSpellCatalog$StormEyeSpell;)V`

- 類別：`local/lito381/server/ClientSession`
- 類型：例外表涵蓋、含迴圈（路徑比較器不支援）；事件後繼：`LAYOUT_ONLY`
- 比較器：`method useStormEyeSpell([BLlocal/lito381/server/BattleSpellCatalog$StormEyeSpell;)V: left=259:[369:java/lang/Throwable  ] right=265:[377:java/lang/Throwable  ]; paths rejected: normal control-flow cycle; labelled CFGs differ`

```diff
@@ -1,5 +1,5 @@
 META 2 null [java/io/IOException]
-TRY [259:[369:java/lang/Throwable  ], 262:[369:java/lang/Throwable  ], 264:[369:java/lang/Throwable  ], 265:[369:java/lang/Throwable  ], 267:[369:java/lang/Throwable  ], 268:[369:java/lang/Throwable  ], 269:[369:java/lang/Throwable  ], 270:[369:java/lang/Throwable  ], 277:[369:java/lang/Throwable  ], 281:[369:java/lang/Throwable  ], 284:[369:java/lang/Throwable  ], 287:[369:java/lang/Throwable  ], 291:[369:java/lang/Throwable  ], 294:[369:java/lang/Throwable  ], 299:[369:java/lang/Throwable  ], 302:[369:java/lang/Throwable  ], 306:[369:java/lang/Throwable  ], 310:[369:java/lang/Throwable  ], 316:[369:java/lang/Throwable  ], 317:[369:java/lang/Throwable  ], 319:[369:java/lang/Throwable  ], 320:[369:java/lang/Throwable  ], 322:[369:java/lang/Throwable  ], 324:[369:java/lang/Throwable  ], 327:[369:java/lang/Throwable  ], 328:[369:java/lang/Throwable  ], 333:[369:java/lang/Throwable  ], 334:[369:java/lang/Throwable  ], 335:[369:java/lang/Throwable  ], 337:[369:java/lang/Throwable  ], 341:[369:java/lang/Throwable  ], 342:[369:java/lang/Throwable  ], 343:[369:java/lang/Throwable  ], 347:[369:java/lang/Throwable  ], 350:[369:java/lang/Throwable  ], 351:[369:java/lang/Throwable  ], 353:[369:java/lang/Throwable  ], 357:[369:java/lang/Throwable  ], 360:[369:java/lang/Throwable  ], 365:[369:java/lang/Throwable  ], 367:[369:java/lang/Throwable  ], 371:[369:java/lang/Throwable  ], 401:[485:java/lang/Throwable  ], 404:[485:java/lang/Throwable  ], 408:[485:java/lang/Throwable  ], 411:[485:java/lang/Throwable  ], 413:[485:java/lang/Throwable  ], 415:[485:java/lang/Throwable  ], 416:[485:java/lang/Throwable  ], 421:[485:java/lang/Throwable  ], 423:[485:java/lang/Throwable  ], 424:[485:java/lang/Throwable  ], 427:[485:java/lang/Throwable  ], 430:[485:java/lang/Throwable  ], 431:[485:java/lang/Throwable  ], 434:[485:java/lang/Throwable  ], 438:[485:java/lang/Throwable  ], 443:[485:java/lang/Throwable  ], 444:[485:java/lang/Throwable  ], 447:[485:java/lang/Throwable  ], 452:[485:java/lang/Throwable  ], 455:[485:java/lang/Throwable  ], 465:[485:java/lang/Throwable  ], 470:[485:java/lang/Throwable  ], 473:[485:java/lang/Throwable  ], 475:[485:java/lang/Throwable  ], 477:[485:java/lang/Throwable  ], 478:[485:java/lang/Throwable  ], 479:[485:java/lang/Throwable  ], 480:[485:java/lang/Throwable  ], 483:[485:java/lang/Throwable  ], 487:[485:java/lang/Throwable  ]]
+TRY [265:[377:java/lang/Throwable  ], 268:[377:java/lang/Throwable  ], 270:[377:java/lang/Throwable  ], 271:[377:java/lang/Throwable  ], 273:[377:java/lang/Throwable  ], 274:[377:java/lang/Throwable  ], 275:[377:java/lang/Throwable  ], 276:[377:java/lang/Throwable  ], 283:[377:java/lang/Throwable  ], 287:[377:java/lang/Throwable  ], 290:[377:java/lang/Throwable  ], 293:[377:java/lang/Throwable  ], 297:[377:java/lang/Throwable  ], 300:[377:java/lang/Throwable  ], 305:[377:java/lang/Throwable  ], 308:[377:java/lang/Throwable  ], 312:[377:java/lang/Throwable  ], 316:[377:java/lang/Throwable  ], 322:[377:java/lang/Throwable  ], 323:[377:java/lang/Throwable  ], 325:[377:java/lang/Throwable  ], 326:[377:java/lang/Throwable  ], 328:[377:java/lang/Throwable  ], 330:[377:java/lang/Throwable  ], 333:[377:java/lang/Throwable  ], 334:[377:java/lang/Throwable  ], 339:[377:java/lang/Throwable  ], 340:[377:java/lang/Throwable  ], 341:[377:java/lang/Throwable  ], 343:[377:java/lang/Throwable  ], 347:[377:java/lang/Throwable  ], 348:[377:java/lang/Throwable  ], 349:[377:java/lang/Throwable  ], 352:[377:java/lang/Throwable  ], 356:[377:java/lang/Throwable  ], 359:[377:java/lang/Throwable  ], 361:[377:java/lang/Throwable  ], 365:[377:java/lang/Throwable  ], 368:[377:java/lang/Throwable  ], 373:[377:java/lang/Throwable  ], 375:[377:java/lang/Throwable  ], 379:[377:java/lang/Throwable  ], 409:[493:java/lang/Throwable  ], 412:[493:java/lang/Throwable  ], 416:[493:java/lang/Throwable  ], 419:[493:java/lang/Throwable  ], 421:[493:java/lang/Throwable  ], 423:[493:java/lang/Throwable  ], 424:[493:java/lang/Throwable  ], 429:[493:java/lang/Throwable  ], 431:[493:java/lang/Throwable  ], 432:[493:java/lang/Throwable  ], 435:[493:java/lang/Throwable  ], 438:[493:java/lang/Throwable  ], 439:[493:java/lang/Throwable  ], 442:[493:java/lang/Throwable  ], 446:[493:java/lang/Throwable  ], 451:[493:java/lang/Throwable  ], 452:[493:java/lang/Throwable  ], 455:[493:java/lang/Throwable  ], 460:[493:java/lang/Throwable  ], 463:[493:java/lang/Throwable  ], 473:[493:java/lang/Throwable  ], 478:[493:java/lang/Throwable  ], 481:[493:java/lang/Throwable  ], 483:[493:java/lang/Throwable  ], 485:[493:java/lang/Throwable  ], 486:[493:java/lang/Throwable  ], 487:[493:java/lang/Throwable  ], 488:[493:java/lang/Throwable  ], 491:[493:java/lang/Throwable  ], 495:[493:java/lang/Throwable  ]]
 ALOAD 1
 INVOKESTATIC local/lito381/server/Protocol.selectorOnlySpellRequest([B)Llocal/lito381/server/Protocol$SpellSelector;
 ASTORE 3
```

### `useStormShotSpell([BLlocal/lito381/server/BattleSpellCatalog$StormShotSpell;)V`

- 類別：`local/lito381/server/ClientSession`
- 類型：例外表涵蓋、含迴圈（路徑比較器不支援）；事件後繼：`LAYOUT_ONLY`
- 比較器：`method useStormShotSpell([BLlocal/lito381/server/BattleSpellCatalog$StormShotSpell;)V: left=191:[342:java/lang/Throwable  ] right=189:[340:java/lang/Throwable  ]; paths rejected: normal control-flow cycle; labelled CFGs differ`

```diff
@@ -1,5 +1,5 @@
 META 2 null [java/io/IOException]
-TRY [191:[342:java/lang/Throwable  ], 194:[342:java/lang/Throwable  ], 196:[342:java/lang/Throwable  ], 197:[342:java/lang/Throwable  ], 199:[342:java/lang/Throwable  ], 200:[342:java/lang/Throwable  ], 201:[342:java/lang/Throwable  ], 202:[342:java/lang/Throwable  ], 209:[342:java/lang/Throwable  ], 213:[342:java/lang/Throwable  ], 216:[342:java/lang/Throwable  ], 219:[342:java/lang/Throwable  ], 221:[342:java/lang/Throwable  ], 225:[342:java/lang/Throwable  ], 226:[342:java/lang/Throwable  ], 229:[342:java/lang/Throwable  ], 230:[342:java/lang/Throwable  ], 233:[342:java/lang/Throwable  ], 239:[342:java/lang/Throwable  ], 240:[342:java/lang/Throwable  ], 242:[342:java/lang/Throwable  ], 245:[342:java/lang/Throwable  ], 247:[342:java/lang/Throwable  ], 252:[342:java/lang/Throwable  ], 254:[342:java/lang/Throwable  ], 255:[342:java/lang/Throwable  ], 257:[342:java/lang/Throwable  ], 258:[342:java/lang/Throwable  ], 259:[342:java/lang/Throwable  ], 260:[342:java/lang/Throwable  ], 262:[342:java/lang/Throwable  ], 264:[342:java/lang/Throwable  ], 267:[342:java/lang/Throwable  ], 272:[342:java/lang/Throwable  ], 274:[342:java/lang/Throwable  ], 275:[342:java/lang/Throwable  ], 276:[342:java/lang/Throwable  ], 277:[342:java/lang/Throwable  ], 279:[342:java/lang/Throwable  ], 283:[342:java/lang/Throwable  ], 284:[342:java/lang/Throwable  ], 285:[342:java/lang/Throwable  ], 287:[342:java/lang/Throwable  ], 290:[342:java/lang/Throwable  ], 292:[342:java/lang/Throwable  ], 294:[342:java/lang/Throwable  ], 295:[342:java/lang/Throwable  ], 302:[342:java/lang/Throwable  ], 305:[342:java/lang/Throwable  ], 306:[342:java/lang/Throwable  ], 308:[342:java/lang/Throwable  ], 312:[342:java/lang/Throwable  ], 315:[342:java/lang/Throwable  ], 320:[342:java/lang/Throwable  ], 322:[342:java/lang/Throwable  ], 325:[342:java/lang/Throwable  ], 328:[342:java/lang/Throwable  ], 331:[342:java/lang/Throwable  ], 334:[342:java/lang/Throwable  ], 337:[342:java/lang/Throwable  ], 340:[342:java/lang/Throwable  ], 344:[342:java/lang/Throwable  ]]
+TRY [189:[340:java/lang/Throwable  ], 192:[340:java/lang/Throwable  ], 194:[340:java/lang/Throwable  ], 195:[340:java/lang/Throwable  ], 197:[340:java/lang/Throwable  ], 198:[340:java/lang/Throwable  ], 199:[340:java/lang/Throwable  ], 200:[340:java/lang/Throwable  ], 207:[340:java/lang/Throwable  ], 211:[340:java/lang/Throwable  ], 214:[340:java/lang/Throwable  ], 217:[340:java/lang/Throwable  ], 219:[340:java/lang/Throwable  ], 223:[340:java/lang/Throwable  ], 224:[340:java/lang/Throwable  ], 227:[340:java/lang/Throwable  ], 228:[340:java/lang/Throwable  ], 231:[340:java/lang/Throwable  ], 237:[340:java/lang/Throwable  ], 238:[340:java/lang/Throwable  ], 240:[340:java/lang/Throwable  ], 243:[340:java/lang/Throwable  ], 245:[340:java/lang/Throwable  ], 250:[340:java/lang/Throwable  ], 252:[340:java/lang/Throwable  ], 253:[340:java/lang/Throwable  ], 255:[340:java/lang/Throwable  ], 256:[340:java/lang/Throwable  ], 257:[340:java/lang/Throwable  ], 258:[340:java/lang/Throwable  ], 260:[340:java/lang/Throwable  ], 262:[340:java/lang/Throwable  ], 265:[340:java/lang/Throwable  ], 270:[340:java/lang/Throwable  ], 272:[340:java/lang/Throwable  ], 273:[340:java/lang/Throwable  ], 274:[340:java/lang/Throwable  ], 275:[340:java/lang/Throwable  ], 277:[340:java/lang/Throwable  ], 281:[340:java/lang/Throwable  ], 282:[340:java/lang/Throwable  ], 283:[340:java/lang/Throwable  ], 285:[340:java/lang/Throwable  ], 288:[340:java/lang/Throwable  ], 290:[340:java/lang/Throwable  ], 292:[340:java/lang/Throwable  ], 293:[340:java/lang/Throwable  ], 297:[340:java/lang/Throwable  ], 301:[340:java/lang/Throwable  ], 304:[340:java/lang/Throwable  ], 306:[340:java/lang/Throwable  ], 310:[340:java/lang/Throwable  ], 313:[340:java/lang/Throwable  ], 318:[340:java/lang/Throwable  ], 320:[340:java/lang/Throwable  ], 323:[340:java/lang/Throwable  ], 326:[340:java/lang/Throwable  ], 329:[340:java/lang/Throwable  ], 332:[340:java/lang/Throwable  ], 335:[340:java/lang/Throwable  ], 338:[340:java/lang/Throwable  ], 342:[340:java/lang/Throwable  ]]
 ALOAD 1
 INVOKESTATIC local/lito381/server/Protocol.objectSpellRequest([B)Llocal/lito381/server/Protocol$ObjectSpellRequest;
 ALOAD 0
```

### `useTripleArrowSpell([BLlocal/lito381/server/BattleSpellCatalog$TripleArrowSpell;)V`

- 類別：`local/lito381/server/ClientSession`
- 類型：例外表涵蓋、含迴圈（路徑比較器不支援）；事件後繼：`LAYOUT_ONLY`
- 比較器：`method useTripleArrowSpell([BLlocal/lito381/server/BattleSpellCatalog$TripleArrowSpell;)V: left=116:[230:java/lang/Throwable  ] right=117:[233:java/lang/Throwable  ]; paths rejected: normal control-flow cycle; labelled CFGs differ`

```diff
@@ -1,5 +1,5 @@
 META 2 null [java/io/IOException]
-TRY [116:[230:java/lang/Throwable  ], 118:[230:java/lang/Throwable  ], 119:[230:java/lang/Throwable  ], 121:[230:java/lang/Throwable  ], 122:[230:java/lang/Throwable  ], 123:[230:java/lang/Throwable  ], 124:[230:java/lang/Throwable  ], 127:[230:java/lang/Throwable  ], 128:[230:java/lang/Throwable  ], 131:[230:java/lang/Throwable  ], 136:[230:java/lang/Throwable  ], 138:[230:java/lang/Throwable  ], 139:[230:java/lang/Throwable  ], 146:[230:java/lang/Throwable  ], 147:[230:java/lang/Throwable  ], 150:[230:java/lang/Throwable  ], 151:[230:java/lang/Throwable  ], 164:[230:java/lang/Throwable  ], 167:[230:java/lang/Throwable  ], 169:[230:java/lang/Throwable  ], 171:[230:java/lang/Throwable  ], 173:[230:java/lang/Throwable  ], 178:[230:java/lang/Throwable  ], 180:[230:java/lang/Throwable  ], 181:[230:java/lang/Throwable  ], 183:[230:java/lang/Throwable  ], 184:[230:java/lang/Throwable  ], 185:[230:java/lang/Throwable  ], 188:[230:java/lang/Throwable  ], 193:[230:java/lang/Throwable  ], 195:[230:java/lang/Throwable  ], 197:[230:java/lang/Throwable  ], 198:[230:java/lang/Throwable  ], 202:[230:java/lang/Throwable  ], 203:[230:java/lang/Throwable  ], 204:[230:java/lang/Throwable  ], 208:[230:java/lang/Throwable  ], 211:[230:java/lang/Throwable  ], 212:[230:java/lang/Throwable  ], 214:[230:java/lang/Throwable  ], 218:[230:java/lang/Throwable  ], 221:[230:java/lang/Throwable  ], 226:[230:java/lang/Throwable  ], 228:[230:java/lang/Throwable  ], 232:[230:java/lang/Throwable  ]]
+TRY [117:[233:java/lang/Throwable  ], 119:[233:java/lang/Throwable  ], 120:[233:java/lang/Throwable  ], 122:[233:java/lang/Throwable  ], 123:[233:java/lang/Throwable  ], 124:[233:java/lang/Throwable  ], 125:[233:java/lang/Throwable  ], 128:[233:java/lang/Throwable  ], 129:[233:java/lang/Throwable  ], 132:[233:java/lang/Throwable  ], 137:[233:java/lang/Throwable  ], 139:[233:java/lang/Throwable  ], 140:[233:java/lang/Throwable  ], 147:[233:java/lang/Throwable  ], 148:[233:java/lang/Throwable  ], 151:[233:java/lang/Throwable  ], 152:[233:java/lang/Throwable  ], 165:[233:java/lang/Throwable  ], 168:[233:java/lang/Throwable  ], 170:[233:java/lang/Throwable  ], 172:[233:java/lang/Throwable  ], 174:[233:java/lang/Throwable  ], 179:[233:java/lang/Throwable  ], 181:[233:java/lang/Throwable  ], 182:[233:java/lang/Throwable  ], 184:[233:java/lang/Throwable  ], 185:[233:java/lang/Throwable  ], 186:[233:java/lang/Throwable  ], 189:[233:java/lang/Throwable  ], 194:[233:java/lang/Throwable  ], 196:[233:java/lang/Throwable  ], 198:[233:java/lang/Throwable  ], 199:[233:java/lang/Throwable  ], 203:[233:java/lang/Throwable  ], 204:[233:java/lang/Throwable  ], 205:[233:java/lang/Throwable  ], 208:[233:java/lang/Throwable  ], 212:[233:java/lang/Throwable  ], 215:[233:java/lang/Throwable  ], 217:[233:java/lang/Throwable  ], 221:[233:java/lang/Throwable  ], 224:[233:java/lang/Throwable  ], 229:[233:java/lang/Throwable  ], 231:[233:java/lang/Throwable  ], 235:[233:java/lang/Throwable  ]]
 ALOAD 1
 INVOKESTATIC local/lito381/server/Protocol.attackSpellRequest([B)Llocal/lito381/server/Protocol$AttackSpellRequest;
 ALOAD 0
```

### `useTrueTargetSpell([BLlocal/lito381/server/BattleSpellCatalog$TrueTargetSpell;)V`

- 類別：`local/lito381/server/ClientSession`
- 類型：例外表涵蓋、含迴圈（路徑比較器不支援）；事件後繼：`LAYOUT_ONLY`
- 比較器：`method useTrueTargetSpell([BLlocal/lito381/server/BattleSpellCatalog$TrueTargetSpell;)V: left=200:[313:java/lang/Throwable  ] right=201:[316:java/lang/Throwable  ]; paths rejected: normal control-flow cycle; labelled CFGs differ`

```diff
@@ -1,5 +1,5 @@
 META 2 null [java/io/IOException]
-TRY [200:[313:java/lang/Throwable  ], 202:[313:java/lang/Throwable  ], 203:[313:java/lang/Throwable  ], 205:[313:java/lang/Throwable  ], 206:[313:java/lang/Throwable  ], 207:[313:java/lang/Throwable  ], 208:[313:java/lang/Throwable  ], 211:[313:java/lang/Throwable  ], 215:[313:java/lang/Throwable  ], 218:[313:java/lang/Throwable  ], 221:[313:java/lang/Throwable  ], 225:[313:java/lang/Throwable  ], 231:[313:java/lang/Throwable  ], 235:[313:java/lang/Throwable  ], 240:[313:java/lang/Throwable  ], 245:[313:java/lang/Throwable  ], 248:[313:java/lang/Throwable  ], 261:[313:java/lang/Throwable  ], 263:[313:java/lang/Throwable  ], 265:[313:java/lang/Throwable  ], 270:[313:java/lang/Throwable  ], 271:[313:java/lang/Throwable  ], 272:[313:java/lang/Throwable  ], 274:[313:java/lang/Throwable  ], 277:[313:java/lang/Throwable  ], 285:[313:java/lang/Throwable  ], 286:[313:java/lang/Throwable  ], 287:[313:java/lang/Throwable  ], 291:[313:java/lang/Throwable  ], 294:[313:java/lang/Throwable  ], 295:[313:java/lang/Throwable  ], 297:[313:java/lang/Throwable  ], 301:[313:java/lang/Throwable  ], 304:[313:java/lang/Throwable  ], 309:[313:java/lang/Throwable  ], 311:[313:java/lang/Throwable  ], 315:[313:java/lang/Throwable  ]]
+TRY [201:[316:java/lang/Throwable  ], 203:[316:java/lang/Throwable  ], 204:[316:java/lang/Throwable  ], 206:[316:java/lang/Throwable  ], 207:[316:java/lang/Throwable  ], 208:[316:java/lang/Throwable  ], 209:[316:java/lang/Throwable  ], 212:[316:java/lang/Throwable  ], 216:[316:java/lang/Throwable  ], 219:[316:java/lang/Throwable  ], 222:[316:java/lang/Throwable  ], 226:[316:java/lang/Throwable  ], 232:[316:java/lang/Throwable  ], 236:[316:java/lang/Throwable  ], 241:[316:java/lang/Throwable  ], 246:[316:java/lang/Throwable  ], 249:[316:java/lang/Throwable  ], 262:[316:java/lang/Throwable  ], 264:[316:java/lang/Throwable  ], 266:[316:java/lang/Throwable  ], 271:[316:java/lang/Throwable  ], 272:[316:java/lang/Throwable  ], 273:[316:java/lang/Throwable  ], 275:[316:java/lang/Throwable  ], 278:[316:java/lang/Throwable  ], 286:[316:java/lang/Throwable  ], 287:[316:java/lang/Throwable  ], 288:[316:java/lang/Throwable  ], 291:[316:java/lang/Throwable  ], 295:[316:java/lang/Throwable  ], 298:[316:java/lang/Throwable  ], 300:[316:java/lang/Throwable  ], 304:[316:java/lang/Throwable  ], 307:[316:java/lang/Throwable  ], 312:[316:java/lang/Throwable  ], 314:[316:java/lang/Throwable  ], 318:[316:java/lang/Throwable  ]]
 ALOAD 1
 INVOKESTATIC local/lito381/server/Protocol.trueTargetRequest([B)Llocal/lito381/server/Protocol$TrueTargetRequest;
 ASTORE 3
```

### `useWeaponBuffSpell([BLlocal/lito381/server/BattleSpellCatalog$WeaponBuffSpell;)V`

- 類別：`local/lito381/server/ClientSession`
- 類型：例外表涵蓋、含迴圈（路徑比較器不支援）；事件後繼：`LAYOUT_ONLY`
- 比較器：`method useWeaponBuffSpell([BLlocal/lito381/server/BattleSpellCatalog$WeaponBuffSpell;)V: left=460:[659:java/lang/Throwable  ] right=464:[663:java/lang/Throwable  ]; paths rejected: normal control-flow cycle; labelled CFGs differ`

```diff
@@ -1,5 +1,5 @@
 META 2 null [java/io/IOException]
-TRY [460:[659:java/lang/Throwable  ], 462:[648:java/lang/Throwable  , 659:java/lang/Throwable  ], 464:[648:java/lang/Throwable  , 659:java/lang/Throwable  ], 465:[648:java/lang/Throwable  , 659:java/lang/Throwable  ], 467:[648:java/lang/Throwable  , 659:java/lang/Throwable  ], 468:[648:java/lang/Throwable  , 659:java/lang/Throwable  ], 469:[648:java/lang/Throwable  , 659:java/lang/Throwable  ], 470:[648:java/lang/Throwable  , 659:java/lang/Throwable  ], 477:[648:java/lang/Throwable  , 659:java/lang/Throwable  ], 481:[648:java/lang/Throwable  , 659:java/lang/Throwable  ], 485:[648:java/lang/Throwable  , 659:java/lang/Throwable  ], 488:[648:java/lang/Throwable  , 659:java/lang/Throwable  ], 491:[648:java/lang/Throwable  , 659:java/lang/Throwable  ], 495:[648:java/lang/Throwable  , 659:java/lang/Throwable  ], 499:[648:java/lang/Throwable  , 659:java/lang/Throwable  ], 503:[648:java/lang/Throwable  , 659:java/lang/Throwable  ], 506:[648:java/lang/Throwable  , 659:java/lang/Throwable  ], 508:[648:java/lang/Throwable  , 659:java/lang/Throwable  ], 510:[648:java/lang/Throwable  , 659:java/lang/Throwable  ], 512:[648:java/lang/Throwable  , 659:java/lang/Throwable  ], 518:[648:java/lang/Throwable  , 659:java/lang/Throwable  ], 519:[648:java/lang/Throwable  , 659:java/lang/Throwable  ], 520:[648:java/lang/Throwable  , 659:java/lang/Throwable  ], 522:[648:java/lang/Throwable  , 659:java/lang/Throwable  ], 524:[659:java/lang/Throwable  ], 529:[648:java/lang/Throwable  , 659:java/lang/Throwable  ], 533:[648:java/lang/Throwable  , 659:java/lang/Throwable  ], 537:[648:java/lang/Throwable  , 659:java/lang/Throwable  ], 541:[648:java/lang/Throwable  , 659:java/lang/Throwable  ], 545:[648:java/lang/Throwable  , 659:java/lang/Throwable  ], 549:[648:java/lang/Throwable  , 659:java/lang/Throwable  ], 553:[648:java/lang/Throwable  , 659:java/lang/Throwable  ], 559:[648:java/lang/Throwable  , 659:java/lang/Throwable  ], 561:[648:java/lang/Throwable  , 659:java/lang/Throwable  ], 563:[648:java/lang/Throwable  , 659:java/lang/Throwable  ], 565:[648:java/lang/Throwable  , 659:java/lang/Throwable  ], 566:[648:java/lang/Throwable  , 659:java/lang/Throwable  ], 568:[648:java/lang/Throwable  , 659:java/lang/Throwable  ], 570:[648:java/lang/Throwable  , 659:java/lang/Throwable  ], 571:[648:java/lang/Throwable  , 659:java/lang/Throwable  ], 572:[648:java/lang/Throwable  , 659:java/lang/Throwable  ], 575:[648:java/lang/Throwable  , 659:java/lang/Throwable  ], 578:[648:java/lang/Throwable  , 659:java/lang/Throwable  ], 584:[648:java/lang/Throwable  , 659:java/lang/Throwable  ], 587:[648:java/lang/Throwable  , 659:java/lang/Throwable  ], 588:[648:java/lang/Throwable  , 659:java/lang/Throwable  ], 593:[648:java/lang/Throwable  , 659:java/lang/Throwable  ], 595:[648:java/lang/Throwable  , 659:java/lang/Throwable  ], 596:[648:java/lang/Throwable  , 659:java/lang/Throwable  ], 599:[648:java/lang/Throwable  , 659:java/lang/Throwable  ], 602:[648:java/lang/Throwable  , 659:java/lang/Throwable  ], 604:[648:java/lang/Throwable  , 659:java/lang/Throwable  ], 606:[648:java/lang/Throwable  , 659:java/lang/Throwable  ], 608:[648:java/lang/Throwable  , 659:java/lang/Throwable  ], 610:[648:java/lang/Throwable  , 659:java/lang/Throwable  ], 612:[648:java/lang/Throwable  , 659:java/lang/Throwable  ], 614:[648:java/lang/Throwable  , 659:java/lang/Throwable  ], 619:[648:java/lang/Throwable  , 659:java/lang/Throwable  ], 622:[648:java/lang/Throwable  , 659:java/lang/Throwable  ], 623:[648:java/lang/Throwable  , 659:java/lang/Throwable  ], 625:[648:java/lang/Throwable  , 659:java/lang/Throwable  ], 628:[648:java/lang/Throwable  , 659:java/lang/Throwable  ], 630:[648:java/lang/Throwable  , 659:java/lang/Throwable  ], 631:[648:java/lang/Throwable  , 659:java/lang/Throwable  ], 634:[648:java/lang/Throwable  , 659:java/lang/Throwable  ], 639:[648:java/lang/Throwable  , 659:java/lang/Throwable  ], 640:[648:java/lang/Throwable  , 659:java/lang/Throwable  ], 643:[648:java/lang/Throwable  , 659:java/lang/Throwable  ], 646:[648:java/lang/Throwable  , 659:java/lang/Throwable  ], 650:[648:java/lang/Throwable  , 659:java/lang/Throwable  ], 652:[659:java/lang/Throwable  ], 654:[659:java/lang/Throwable  ], 657:[659:java/lang/Throwable  ], 661:[659:java/lang/Throwable  ], 672:[792:java/lang/Throwable  ], 674:[792:java/lang/Throwable  ], 675:[792:java/lang/Throwable  ], 677:[792:java/lang/Throwable  ], 678:[792:java/lang/Throwable  ], 679:[792:java/lang/Throwable  ], 680:[792:java/lang/Throwable  ], 683:[792:java/lang/Throwable  ], 690:[792:java/lang/Throwable  ], 694:[792:java/lang/Throwable  ], 695:[792:java/lang/Throwable  ], 698:[792:java/lang/Throwable  ], 701:[792:java/lang/Throwable  ], 704:[792:java/lang/Throwable  ], 708:[792:java/lang/Throwable  ], 712:[792:java/lang/Throwable  ], 716:[792:java/lang/Throwable  ], 720:[792:java/lang/Throwable  ], 721:[792:java/lang/Throwable  ], 724:[792:java/lang/Throwable  ], 728:[792:java/lang/Throwable  ], 732:[792:java/lang/Throwable  ], 735:[792:java/lang/Throwable  ], 738:[792:java/lang/Throwable  ], 742:[792:java/lang/Throwable  ], 746:[792:java/lang/Throwable  ], 748:[792:java/lang/Throwable  ], 750:[792:java/lang/Throwable  ], 752:[792:java/lang/Throwable  ], 753:[792:java/lang/Throwable  ], 755:[792:java/lang/Throwable  ], 757:[792:java/lang/Throwable  ], 758:[792:java/lang/Throwable  ], 759:[792:java/lang/Throwable  ], 761:[792:java/lang/Throwable  ], 766:[792:java/lang/Throwable  ], 769:[792:java/lang/Throwable  ], 770:[792:java/lang/Throwable  ], 772:[792:java/lang/Throwable  ], 775:[792:java/lang/Throwable  ], 777:[792:java/lang/Throwable  ], 778:[792:java/lang/Throwable  ], 781:[792:java/lang/Throwable  ], 786:[792:java/lang/Throwable  ], 787:[792:java/lang/Throwable  ], 790:[792:java/lang/Throwable  ], 794:[792:java/lang/Throwable  ], 805:[920:java/lang/Throwable  ], 807:[920:java/lang/Throwable  ], 808:[920:java/lang/Throwable  ], 810:[920:java/lang/Throwable  ], 811:[920:java/lang/Throwable  ], 812:[920:java/lang/Throwable  ], 813:[920:java/lang/Throwable  ], 816:[920:java/lang/Throwable  ], 820:[920:java/lang/Throwable  ], 821:[920:java/lang/Throwable  ], 828:[920:java/lang/Throwable  ], 832:[920:java/lang/Throwable  ], 833:[920:java/lang/Throwable  ], 836:[920:java/lang/Throwable  ], 839:[920:java/lang/Throwable  ], 842:[920:java/lang/Throwable  ], 846:[920:java/lang/Throwable  ], 850:[920:java/lang/Throwable  ], 854:[920:java/lang/Throwable  ], 860:[920:java/lang/Throwable  ], 863:[920:java/lang/Throwable  ], 866:[920:java/lang/Throwable  ], 870:[920:java/lang/Throwable  ], 874:[920:java/lang/Throwable  ], 876:[920:java/lang/Throwable  ], 878:[920:java/lang/Throwable  ], 880:[920:java/lang/Throwable  ], 881:[920:java/lang/Throwable  ], 883:[920:java/lang/Throwable  ], 885:[920:java/lang/Throwable  ], 886:[920:java/lang/Throwable  ], 887:[920:java/lang/Throwable  ], 889:[920:java/lang/Throwable  ], 894:[920:java/lang/Throwable  ], 897:[920:java/lang/Throwable  ], 898:[920:java/lang/Throwable  ], 900:[920:java/lang/Throwable  ], 903:[920:java/lang/Throwable  ], 905:[920:java/lang/Throwable  ], 906:[920:java/lang/Throwable  ], 909:[920:java/lang/Throwable  ], 914:[920:java/lang/Throwable  ], 915:[920:java/lang/Throwable  ], 918:[920:java/lang/Throwable  ], 922:[920:java/lang/Throwable  ], 931:[1004:java/lang/Throwable  ], 933:[1004:java/lang/Throwable  ], 934:[1004:java/lang/Throwable  ], 936:[1004:java/lang/Throwable  ], 937:[1004:java/lang/Throwable  ], 938:[1004:java/lang/Throwable  ], 939:[1004:java/lang/Throwable  ], 946:[1004:java/lang/Throwable  ], 950:[1004:java/lang/Throwable  ], 951:[1004:java/lang/Throwable  ], 954:[1004:java/lang/Throwable  ], 957:[1004:java/lang/Throwable  ], 959:[1004:java/lang/Throwable  ], 961:[1004:java/lang/Throwable  ], 963:[1004:java/lang/Throwable  ], 969:[1004:java/lang/Throwable  ], 970:[1004:java/lang/Throwable  ], 971:[1004:java/lang/Throwable  ], 973:[1004:java/lang/Throwable  ], 978:[1004:java/lang/Throwable  ], 981:[1004:java/lang/Throwable  ], 982:[1004:java/lang/Throwable  ], 984:[1004:java/lang/Throwable  ], 987:[1004:java/lang/Throwable  ], 989:[1004:java/lang/Throwable  ], 990:[1004:java/lang/Throwable  ], 993:[1004:java/lang/Throwable  ], 998:[1004:java/lang/Throwable  ], 999:[1004:java/lang/Throwable  ], 1002:[1004:java/lang/Throwable  ], 1006:[1004:java/lang/Throwable  ]]
+TRY [464:[663:java/lang/Throwable  ], 466:[655:java/lang/Throwable  , 663:java/lang/Throwable  ], 468:[655:java/lang/Throwable  , 663:java/lang/Throwable  ], 469:[655:java/lang/Throwable  , 663:java/lang/Throwable  ], 471:[655:java/lang/Throwable  , 663:java/lang/Throwable  ], 472:[655:java/lang/Throwable  , 663:java/lang/Throwable  ], 473:[655:java/lang/Throwable  , 663:java/lang/Throwable  ], 474:[655:java/lang/Throwable  , 663:java/lang/Throwable  ], 481:[655:java/lang/Throwable  , 663:java/lang/Throwable  ], 485:[655:java/lang/Throwable  , 663:java/lang/Throwable  ], 489:[655:java/lang/Throwable  , 663:java/lang/Throwable  ], 492:[655:java/lang/Throwable  , 663:java/lang/Throwable  ], 495:[655:java/lang/Throwable  , 663:java/lang/Throwable  ], 499:[655:java/lang/Throwable  , 663:java/lang/Throwable  ], 503:[655:java/lang/Throwable  , 663:java/lang/Throwable  ], 507:[655:java/lang/Throwable  , 663:java/lang/Throwable  ], 510:[655:java/lang/Throwable  , 663:java/lang/Throwable  ], 512:[655:java/lang/Throwable  , 663:java/lang/Throwable  ], 514:[655:java/lang/Throwable  , 663:java/lang/Throwable  ], 516:[655:java/lang/Throwable  , 663:java/lang/Throwable  ], 522:[655:java/lang/Throwable  , 663:java/lang/Throwable  ], 523:[655:java/lang/Throwable  , 663:java/lang/Throwable  ], 524:[655:java/lang/Throwable  , 663:java/lang/Throwable  ], 526:[655:java/lang/Throwable  , 663:java/lang/Throwable  ], 528:[663:java/lang/Throwable  ], 533:[655:java/lang/Throwable  , 663:java/lang/Throwable  ], 537:[655:java/lang/Throwable  , 663:java/lang/Throwable  ], 541:[655:java/lang/Throwable  , 663:java/lang/Throwable  ], 545:[655:java/lang/Throwable  , 663:java/lang/Throwable  ], 549:[655:java/lang/Throwable  , 663:java/lang/Throwable  ], 553:[655:java/lang/Throwable  , 663:java/lang/Throwable  ], 557:[655:java/lang/Throwable  , 663:java/lang/Throwable  ], 563:[655:java/lang/Throwable  , 663:java/lang/Throwable  ], 565:[655:java/lang/Throwable  , 663:java/lang/Throwable  ], 567:[655:java/lang/Throwable  , 663:java/lang/Throwable  ], 569:[655:java/lang/Throwable  , 663:java/lang/Throwable  ], 570:[655:java/lang/Throwable  , 663:java/lang/Throwable  ], 572:[655:java/lang/Throwable  , 663:java/lang/Throwable  ], 574:[655:java/lang/Throwable  , 663:java/lang/Throwable  ], 575:[655:java/lang/Throwable  , 663:java/lang/Throwable  ], 576:[655:java/lang/Throwable  , 663:java/lang/Throwable  ], 578:[655:java/lang/Throwable  , 663:java/lang/Throwable  ], 580:[663:java/lang/Throwable  ], 583:[655:java/lang/Throwable  , 663:java/lang/Throwable  ], 586:[655:java/lang/Throwable  , 663:java/lang/Throwable  ], 592:[655:java/lang/Throwable  , 663:java/lang/Throwable  ], 595:[655:java/lang/Throwable  , 663:java/lang/Throwable  ], 596:[655:java/lang/Throwable  , 663:java/lang/Throwable  ], 601:[655:java/lang/Throwable  , 663:java/lang/Throwable  ], 603:[655:java/lang/Throwable  , 663:java/lang/Throwable  ], 604:[655:java/lang/Throwable  , 663:java/lang/Throwable  ], 607:[655:java/lang/Throwable  , 663:java/lang/Throwable  ], 610:[655:java/lang/Throwable  , 663:java/lang/Throwable  ], 612:[655:java/lang/Throwable  , 663:java/lang/Throwable  ], 614:[655:java/lang/Throwable  , 663:java/lang/Throwable  ], 616:[655:java/lang/Throwable  , 663:java/lang/Throwable  ], 618:[655:java/lang/Throwable  , 663:java/lang/Throwable  ], 620:[655:java/lang/Throwable  , 663:java/lang/Throwable  ], 622:[655:java/lang/Throwable  , 663:java/lang/Throwable  ], 626:[655:java/lang/Throwable  , 663:java/lang/Throwable  ], 630:[655:java/lang/Throwable  , 663:java/lang/Throwable  ], 633:[655:java/lang/Throwable  , 663:java/lang/Throwable  ], 635:[655:java/lang/Throwable  , 663:java/lang/Throwable  ], 638:[655:java/lang/Throwable  , 663:java/lang/Throwable  ], 640:[655:java/lang/Throwable  , 663:java/lang/Throwable  ], 641:[655:java/lang/Throwable  , 663:java/lang/Throwable  ], 644:[655:java/lang/Throwable  , 663:java/lang/Throwable  ], 649:[655:java/lang/Throwable  , 663:java/lang/Throwable  ], 650:[655:java/lang/Throwable  , 663:java/lang/Throwable  ], 653:[655:java/lang/Throwable  , 663:java/lang/Throwable  ], 657:[655:java/lang/Throwable  , 663:java/lang/Throwable  ], 659:[663:java/lang/Throwable  ], 661:[663:java/lang/Throwable  ], 665:[663:java/lang/Throwable  ], 676:[798:java/lang/Throwable  ], 678:[798:java/lang/Throwable  ], 679:[798:java/lang/Throwable  ], 681:[798:java/lang/Throwable  ], 682:[798:java/lang/Throwable  ], 683:[798:java/lang/Throwable  ], 684:[798:java/lang/Throwable  ], 687:[798:java/lang/Throwable  ], 694:[798:java/lang/Throwable  ], 698:[798:java/lang/Throwable  ], 699:[798:java/lang/Throwable  ], 702:[798:java/lang/Throwable  ], 705:[798:java/lang/Throwable  ], 708:[798:java/lang/Throwable  ], 712:[798:java/lang/Throwable  ], 716:[798:java/lang/Throwable  ], 720:[798:java/lang/Throwable  ], 724:[798:java/lang/Throwable  ], 725:[798:java/lang/Throwable  ], 728:[798:java/lang/Throwable  ], 732:[798:java/lang/Throwable  ], 736:[798:java/lang/Throwable  ], 739:[798:java/lang/Throwable  ], 742:[798:java/lang/Throwable  ], 746:[798:java/lang/Throwable  ], 750:[798:java/lang/Throwable  ], 752:[798:java/lang/Throwable  ], 754:[798:java/lang/Throwable  ], 756:[798:java/lang/Throwable  ], 757:[798:java/lang/Throwable  ], 759:[798:java/lang/Throwable  ], 761:[798:java/lang/Throwable  ], 762:[798:java/lang/Throwable  ], 763:[798:java/lang/Throwable  ], 765:[798:java/lang/Throwable  ], 769:[798:java/lang/Throwable  ], 773:[798:java/lang/Throwable  ], 776:[798:java/lang/Throwable  ], 778:[798:java/lang/Throwable  ], 781:[798:java/lang/Throwable  ], 783:[798:java/lang/Throwable  ], 784:[798:java/lang/Throwable  ], 787:[798:java/lang/Throwable  ], 792:[798:java/lang/Throwable  ], 793:[798:java/lang/Throwable  ], 796:[798:java/lang/Throwable  ], 800:[798:java/lang/Throwable  ], 811:[928:java/lang/Throwable  ], 813:[928:java/lang/Throwable  ], 814:[928:java/lang/Throwable  ], 816:[928:java/lang/Throwable  ], 817:[928:java/lang/Throwable  ], 818:[928:java/lang/Throwable  ], 819:[928:java/lang/Throwable  ], 822:[928:java/lang/Throwable  ], 826:[928:java/lang/Throwable  ], 827:[928:java/lang/Throwable  ], 834:[928:java/lang/Throwable  ], 838:[928:java/lang/Throwable  ], 839:[928:java/lang/Throwable  ], 842:[928:java/lang/Throwable  ], 845:[928:java/lang/Throwable  ], 848:[928:java/lang/Throwable  ], 852:[928:java/lang/Throwable  ], 856:[928:java/lang/Throwable  ], 860:[928:java/lang/Throwable  ], 866:[928:java/lang/Throwable  ], 869:[928:java/lang/Throwable  ], 872:[928:java/lang/Throwable  ], 876:[928:java/lang/Throwable  ], 880:[928:java/lang/Throwable  ], 882:[928:java/lang/Throwable  ], 884:[928:java/lang/Throwable  ], 886:[928:java/lang/Throwable  ], 887:[928:java/lang/Throwable  ], 889:[928:java/lang/Throwable  ], 891:[928:java/lang/Throwable  ], 892:[928:java/lang/Throwable  ], 893:[928:java/lang/Throwable  ], 895:[928:java/lang/Throwable  ], 899:[928:java/lang/Throwable  ], 903:[928:java/lang/Throwable  ], 906:[928:java/lang/Throwable  ], 908:[928:java/lang/Throwable  ], 911:[928:java/lang/Throwable  ], 913:[928:java/lang/Throwable  ], 914:[928:java/lang/Throwable  ], 917:[928:java/lang/Throwable  ], 922:[928:java/lang/Throwable  ], 923:[928:java/lang/Throwable  ], 926:[928:java/lang/Throwable  ], 930:[928:java/lang/Throwable  ], 939:[1014:java/lang/Throwable  ], 941:[1014:java/lang/Throwable  ], 942:[1014:java/lang/Throwable  ], 944:[1014:java/lang/Throwable  ], 945:[1014:java/lang/Throwable  ], 946:[1014:java/lang/Throwable  ], 947:[1014:java/lang/Throwable  ], 954:[1014:java/lang/Throwable  ], 958:[1014:java/lang/Throwable  ], 959:[1014:java/lang/Throwable  ], 962:[1014:java/lang/Throwable  ], 965:[1014:java/lang/Throwable  ], 967:[1014:java/lang/Throwable  ], 969:[1014:java/lang/Throwable  ], 971:[1014:java/lang/Throwable  ], 977:[1014:java/lang/Throwable  ], 978:[1014:java/lang/Throwable  ], 979:[1014:java/lang/Throwable  ], 981:[1014:java/lang/Throwable  ], 985:[1014:java/lang/Throwable  ], 989:[1014:java/lang/Throwable  ], 992:[1014:java/lang/Throwable  ], 994:[1014:java/lang/Throwable  ], 997:[1014:java/lang/Throwable  ], 999:[1014:java/lang/Throwable  ], 1000:[1014:java/lang/Throwable  ], 1003:[1014:java/lang/Throwable  ], 1008:[1014:java/lang/Throwable  ], 1009:[1014:java/lang/Throwable  ], 1012:[1014:java/lang/Throwable  ], 1016:[1014:java/lang/Throwable  ]]
 ALOAD 1
 INVOKESTATIC local/lito381/server/Protocol.objectSpellRequest([B)Llocal/lito381/server/Protocol$ObjectSpellRequest;
 ALOAD 0
```

### `useWorldRoiCancellation([BLlocal/lito381/server/BattleSpellCatalog$CancellationSpell;)V`

- 類別：`local/lito381/server/ClientSession`
- 類型：例外表涵蓋、含迴圈（路徑比較器不支援）；事件後繼：`LAYOUT_ONLY`
- 比較器：`method useWorldRoiCancellation([BLlocal/lito381/server/BattleSpellCatalog$CancellationSpell;)V: left=316:[529:java/lang/Throwable  ] right=339:[554:java/lang/Throwable  ]; paths rejected: normal control-flow cycle; labelled CFGs differ`

```diff
@@ -1,10 +1,11 @@
 META 2 null [java/io/IOException]
-TRY [316:[529:java/lang/Throwable  ], 318:[529:java/lang/Throwable  ], 319:[529:java/lang/Throwable  ], 321:[529:java/lang/Throwable  ], 322:[529:java/lang/Throwable  ], 323:[529:java/lang/Throwable  ], 324:[529:java/lang/Throwable  ], 331:[529:java/lang/Throwable  ], 335:[529:java/lang/Throwable  ], 336:[529:java/lang/Throwable  ], 339:[529:java/lang/Throwable  ], 342:[529:java/lang/Throwable  ], 345:[529:java/lang/Throwable  ], 348:[529:java/lang/Throwable  ], 350:[529:java/lang/Throwable  ], 352:[529:java/lang/Throwable  ], 354:[529:java/lang/Throwable  ], 356:[529:java/lang/Throwable  ], 362:[529:java/lang/Throwable  ], 363:[529:java/lang/Throwable  ], 364:[529:java/lang/Throwable  ], 366:[529:java/lang/Throwable  ], 369:[529:java/lang/Throwable  ], 372:[529:java/lang/Throwable  ], 376:[529:java/lang/Throwable  ], 380:[529:java/lang/Throwable  ], 389:[529:java/lang/Throwable  ], 393:[529:java/lang/Throwable  ], 411:[529:java/lang/Throwable  ], 414:[529:java/lang/Throwable  ], 415:[529:java/lang/Throwable  ], 419:[529:java/lang/Throwable  ], 421:[529:java/lang/Throwable  ], 423:[529:java/lang/Throwable  ], 428:[529:java/lang/Throwable  ], 429:[529:java/lang/Throwable  ], 431:[529:java/lang/Throwable  ], 432:[529:java/lang/Throwable  ], 434:[529:java/lang/Throwable  ], 436:[529:java/lang/Throwable  ], 437:[529:java/lang/Throwable  ], 438:[529:java/lang/Throwable  ], 440:[529:java/lang/Throwable  ], 444:[529:java/lang/Throwable  ], 447:[529:java/lang/Throwable  ], 452:[529:java/lang/Throwable  ], 458:[529:java/lang/Throwable  ], 461:[529:java/lang/Throwable  ], 462:[529:java/lang/Throwable  ], 465:[529:java/lang/Throwable  ], 473:[529:java/lang/Throwable  ], 478:[529:java/lang/Throwable  ], 483:[529:java/lang/Throwable  ], 486:[529:java/lang/Throwable  ], 490:[529:java/lang/Throwable  ], 494:[529:java/lang/Throwable  ], 495:[529:java/lang/Throwable  ], 496:[529:java/lang/Throwable  ], 498:[529:java/lang/Throwable  ], 503:[529:java/lang/Throwable  ], 507:[529:java/lang/Throwable  ], 510:[529:java/lang/Throwable  ], 511:[529:java/lang/Throwable  ], 513:[529:java/lang/Throwable  ], 517:[529:java/lang/Throwable  ], 520:[529:java/lang/Throwable  ], 525:[529:java/lang/Throwable  ], 527:[529:java/lang/Throwable  ], 531:[529:java/lang/Throwable  ]]
+TRY [339:[554:java/lang/Throwable  ], 341:[554:java/lang/Throwable  ], 342:[554:java/lang/Throwable  ], 344:[554:java/lang/Throwable  ], 345:[554:java/lang/Throwable  ], 346:[554:java/lang/Throwable  ], 347:[554:java/lang/Throwable  ], 354:[554:java/lang/Throwable  ], 358:[554:java/lang/Throwable  ], 359:[554:java/lang/Throwable  ], 362:[554:java/lang/Throwable  ], 365:[554:java/lang/Throwable  ], 368:[554:java/lang/Throwable  ], 371:[554:java/lang/Throwable  ], 373:[554:java/lang/Throwable  ], 375:[554:java/lang/Throwable  ], 377:[554:java/lang/Throwable  ], 379:[554:java/lang/Throwable  ], 385:[554:java/lang/Throwable  ], 386:[554:java/lang/Throwable  ], 387:[554:java/lang/Throwable  ], 389:[554:java/lang/Throwable  ], 392:[554:java/lang/Throwable  ], 395:[554:java/lang/Throwable  ], 399:[554:java/lang/Throwable  ], 403:[554:java/lang/Throwable  ], 412:[554:java/lang/Throwable  ], 416:[554:java/lang/Throwable  ], 434:[554:java/lang/Throwable  ], 437:[554:java/lang/Throwable  ], 438:[554:java/lang/Throwable  ], 442:[554:java/lang/Throwable  ], 444:[554:java/lang/Throwable  ], 446:[554:java/lang/Throwable  ], 451:[554:java/lang/Throwable  ], 452:[554:java/lang/Throwable  ], 454:[554:java/lang/Throwable  ], 455:[554:java/lang/Throwable  ], 457:[554:java/lang/Throwable  ], 459:[554:java/lang/Throwable  ], 460:[554:java/lang/Throwable  ], 461:[554:java/lang/Throwable  ], 463:[554:java/lang/Throwable  ], 467:[554:java/lang/Throwable  ], 470:[554:java/lang/Throwable  ], 475:[554:java/lang/Throwable  ], 481:[554:java/lang/Throwable  ], 484:[554:java/lang/Throwable  ], 485:[554:java/lang/Throwable  ], 488:[554:java/lang/Throwable  ], 496:[554:java/lang/Throwable  ], 501:[554:java/lang/Throwable  ], 506:[554:java/lang/Throwable  ], 509:[554:java/lang/Throwable  ], 513:[554:java/lang/Throwable  ], 517:[554:java/lang/Throwable  ], 518:[554:java/lang/Throwable  ], 519:[554:java/lang/Throwable  ], 521:[554:java/lang/Throwable  ], 526:[554:java/lang/Throwable  ], 529:[554:java/lang/Throwable  ], 533:[554:java/lang/Throwable  ], 536:[554:java/lang/Throwable  ], 538:[554:java/lang/Throwable  ], 542:[554:java/lang/Throwable  ], 545:[554:java/lang/Throwable  ], 550:[554:java/lang/Throwable  ], 552:[554:java/lang/Throwable  ], 556:[554:java/lang/Throwable  ]]
 ALOAD 0
 ALOAD 1
 ALOAD 2
 INVOKESTATIC local/lito381/server/NativeCancellationNpcStatic.route(Llocal/lito381/server/ClientSession;[BLlocal/lito381/server/BattleSpellCatalog$CancellationSpell;)Z
-IFNE L705
+IFEQ L6
+RETURN
 ALOAD 1
 INVOKESTATIC local/lito381/server/Protocol.objectSpellRequest([B)Llocal/lito381/server/Protocol$ObjectSpellRequest;
 ALOAD 0
```

### `useWorldTreeCallSpell([BLlocal/lito381/server/BattleSpellCatalog$WorldTreeCallSpell;)V`

- 類別：`local/lito381/server/ClientSession`
- 類型：例外表涵蓋、含迴圈（路徑比較器不支援）；事件後繼：`LAYOUT_ONLY`
- 比較器：`method useWorldTreeCallSpell([BLlocal/lito381/server/BattleSpellCatalog$WorldTreeCallSpell;)V: left=148:[288:java/lang/Throwable  ] right=148:[290:java/lang/Throwable  ]; paths rejected: normal control-flow cycle; labelled CFGs differ`

```diff
@@ -1,5 +1,5 @@
 META 2 null [java/io/IOException]
-TRY [148:[288:java/lang/Throwable  ], 150:[288:java/lang/Throwable  ], 151:[288:java/lang/Throwable  ], 153:[288:java/lang/Throwable  ], 154:[288:java/lang/Throwable  ], 155:[288:java/lang/Throwable  ], 156:[288:java/lang/Throwable  ], 159:[288:java/lang/Throwable  ], 161:[288:java/lang/Throwable  ], 164:[288:java/lang/Throwable  ], 165:[288:java/lang/Throwable  ], 168:[288:java/lang/Throwable  ], 173:[288:java/lang/Throwable  ], 175:[288:java/lang/Throwable  ], 176:[288:java/lang/Throwable  ], 183:[288:java/lang/Throwable  ], 184:[288:java/lang/Throwable  ], 187:[288:java/lang/Throwable  ], 188:[288:java/lang/Throwable  ], 193:[288:java/lang/Throwable  ], 195:[288:java/lang/Throwable  ], 210:[288:java/lang/Throwable  ], 213:[288:java/lang/Throwable  ], 216:[288:java/lang/Throwable  ], 219:[288:java/lang/Throwable  ], 221:[288:java/lang/Throwable  ], 223:[288:java/lang/Throwable  ], 225:[288:java/lang/Throwable  ], 227:[288:java/lang/Throwable  ], 233:[288:java/lang/Throwable  ], 235:[288:java/lang/Throwable  ], 237:[288:java/lang/Throwable  ], 242:[288:java/lang/Throwable  ], 243:[288:java/lang/Throwable  ], 246:[288:java/lang/Throwable  ], 251:[288:java/lang/Throwable  ], 253:[288:java/lang/Throwable  ], 255:[288:java/lang/Throwable  ], 256:[288:java/lang/Throwable  ], 260:[288:java/lang/Throwable  ], 261:[288:java/lang/Throwable  ], 262:[288:java/lang/Throwable  ], 266:[288:java/lang/Throwable  ], 269:[288:java/lang/Throwable  ], 270:[288:java/lang/Throwable  ], 272:[288:java/lang/Throwable  ], 276:[288:java/lang/Throwable  ], 279:[288:java/lang/Throwable  ], 284:[288:java/lang/Throwable  ], 286:[288:java/lang/Throwable  ], 290:[288:java/lang/Throwable  ]]
+TRY [148:[290:java/lang/Throwable  ], 150:[290:java/lang/Throwable  ], 151:[290:java/lang/Throwable  ], 153:[290:java/lang/Throwable  ], 154:[290:java/lang/Throwable  ], 155:[290:java/lang/Throwable  ], 156:[290:java/lang/Throwable  ], 159:[290:java/lang/Throwable  ], 161:[290:java/lang/Throwable  ], 164:[290:java/lang/Throwable  ], 165:[290:java/lang/Throwable  ], 168:[290:java/lang/Throwable  ], 173:[290:java/lang/Throwable  ], 175:[290:java/lang/Throwable  ], 176:[290:java/lang/Throwable  ], 183:[290:java/lang/Throwable  ], 184:[290:java/lang/Throwable  ], 187:[290:java/lang/Throwable  ], 188:[290:java/lang/Throwable  ], 193:[290:java/lang/Throwable  ], 195:[290:java/lang/Throwable  ], 210:[290:java/lang/Throwable  ], 213:[290:java/lang/Throwable  ], 216:[290:java/lang/Throwable  ], 219:[290:java/lang/Throwable  ], 221:[290:java/lang/Throwable  ], 223:[290:java/lang/Throwable  ], 225:[290:java/lang/Throwable  ], 227:[290:java/lang/Throwable  ], 233:[290:java/lang/Throwable  ], 235:[290:java/lang/Throwable  ], 237:[290:java/lang/Throwable  ], 242:[290:java/lang/Throwable  ], 243:[290:java/lang/Throwable  ], 246:[290:java/lang/Throwable  ], 251:[290:java/lang/Throwable  ], 253:[290:java/lang/Throwable  ], 255:[290:java/lang/Throwable  ], 256:[290:java/lang/Throwable  ], 260:[290:java/lang/Throwable  ], 261:[290:java/lang/Throwable  ], 262:[290:java/lang/Throwable  ], 265:[290:java/lang/Throwable  ], 269:[290:java/lang/Throwable  ], 272:[290:java/lang/Throwable  ], 274:[290:java/lang/Throwable  ], 278:[290:java/lang/Throwable  ], 281:[290:java/lang/Throwable  ], 286:[290:java/lang/Throwable  ], 288:[290:java/lang/Throwable  ], 292:[290:java/lang/Throwable  ]]
 ALOAD 1
 INVOKESTATIC local/lito381/server/Protocol.selectorOnlySpellRequest([B)Llocal/lito381/server/Protocol$SpellSelector;
 ASTORE 3
```

## 不屬於形狀差異：`sourceApplicationRun()V`（待主控裁定）

候選第一個陳述式是直接呼叫 `SourceLogin.allowed(Socket, ServerConfig)`（宣告 `throws Exception`），外面沒有 try；而 `sourceApplicationRun` 與呼叫它的 `run()`（T029，正式版 SAME）都沒有 throws。
Java 原始碼要在不宣告 throws 的方法中呼叫 `throws Exception` 的方法，只有三條路：(1) 加 catch 後轉拋（需要額外呼叫，即目前已核准的 `rethrowUnchecked`）；(2) 方法宣告 `throws Exception`，但 `run()` 是 `Runnable.run` 覆寫，必須再加 catch，會破壞 T029 的 `run()` SAME；(3) catch 後 `throw (RuntimeException) e`，checked 例外會變成 ClassCastException，行為不同。
因此本輪保留 (1)，事件差異只有「`allowed` 失敗時多一次 `rethrowUnchecked` 呼叫」，該 helper 只做 `throw (E) t`，原例外原樣拋出。其餘 handler／CFG 差異（`readFrames` 迴圈、finally 清理）呼叫與欄位多重集合除上述外相同。
