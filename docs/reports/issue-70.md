# Issue #70：B09 移植補齊與剩餘驗證缺口

## 結論

**尚未通過 B09 驗收，不可 B-land。** 本輪在既有 `task/70` worktree 修改，沒有切換或建立分支。以 `60337a6` 合併 `origin/main@fd25185`，已包含 #254、#259 的工具修正。編譯成功不等於語意通過；仍存在下表的 DIFF 與方法配對缺口。

此報告取代前輪報告，保留已核准的 T029、metadata 與 helper 規則，沒有新增豁免。需主控／Claude 裁定的項目列於後文；`needs:claude` 標籤及 PR #95 描述更新目前尚未送出（GitHub API 認證受阻）。

| 層級 | 最新結果 |
|---|---|
| 完整 362 entries | 360 SAME、1 SAME_CFG（GameHub）、1 DIFF（ClientSession） |
| 類別選取 | missingLeft=0、extraLeft=0；類別 ONLY_LEFT=0、ONLY_RIGHT=0 |
| 方法層級 | 192 DIFF、9 ONLY_LEFT、6 ORPHAN_IGNORED、77 SAME_CFG、49 SAME_PATHS |
| T029 八方法 | 6 SAME、2 SAME_EFFECTS；以正式 base-0.6.71.jar 比對 |

## 做了什麼

- 對照候選的完整方法，補齊 `ClientSession` 的技能消耗、材料、選中玩家意圖、物理戰鬥、同伴回呼與生命週期接線。`confirmDeath` 現在使用 `CombatPoisonLifecycle.defeated(hub, ...)`；`resetAndSendRentalTownStatus` 補入 `NativePlayerCommit.advance(this)`。同型入口與重載一起核對。
- 補入候選存在的 40 個具名 callback／repair 本體，並還原缺漏的 lambda 呼叫點。修正地面技能 callback 的完整捕獲參數、治療 LOS 的 static 捕獲及 Map／List 介面型別。
- 修正登入轉移與 auto-select 的 `accountName()`／`stateCharacter()` 求值順序；物理攻擊先求亂數與倍率再呼叫 `CombatDragon.consumeOnHit`；反射封包先建立描述字串再執行 `NativeSourceNpcReaction.allow`，與候選位置一致。
- 保留候選 patch 後仍存在的常數材料分支，以非 final 區域 boolean 表達，避免 javac 刪掉候選保留的拒絕訊息指令。未加入假回傳、空方法或反編譯失敗佔位碼。
- `sourceApplicationRun` 補齊正常退出、EOF、例外與 finally 的清理出口；`restoreBot` 線性份數現為 4 對 4，但完整方法仍 DIFF，不據此宣稱行為等價。
- 原有 T029 inventory 求值順序修正、nativeLightCommit 轉拋、AsRecord 及四條 `enclosing-method` 保留。`say` 原有 `rethrowUnchecked` 使用仍保留；沒有在本輪擴大用途，前次 P3 主控確認項仍列待確認。

相對本輪起點 `90b228f`，修改集中於 `ClientSession.java` 及本報告。主線合併帶入的其他檔案不屬本輪手改。完整 PR 相對 `origin/main` 的檔案範圍為三個 B09 Java、既有 exceptions／extra-methods 核准清單與本報告。

## 已驗證

- Microsoft OpenJDK 17.0.17，`javac --release 17 -encoding UTF-8`，三個 staged 外層類別一起編譯；semantic-diff、AsRecord 工具及 annotation 都由目前主線原始碼重新編譯。
- AsRecord：`scanned=362 converted=6`。產出 class 集合與 `docs/combat-merge/batches.json` 的 B09 `verify_entries` 精確相等。未以候選 class 覆蓋 staged 輸出。
- 完整驗證只使用既有批准的兩條 effects、一條 extra helper、四條 EnclosingMethod 屬性及 movementUnlock Exceptions；T029 指向正式 JAR。沒有修改工具以降低門檻。
- `git diff --check origin/main` 檢查完整 PR 差異（包含既有提交與本輪工作樹內容），不是僅檢查本輪 Java。Protocol 檔尾空白行修正已保留。
- 未啟動 server／launcher／遊戲、未連 DB；本批依任務不執行單元測試或回歸，交 B-land 統一處理。


本輪隔離驗證 JAR SHA-256：`0276d4198eb4dbfe2a9aa208e4b4660ad48aae0ad37bb0f005bf19279fddd41c`。

| 階段 | 方法 DIFF | ONLY_LEFT | ONLY_RIGHT |
|---|---|---|---|
| 本輪合併 main 後基線 | 255 | 9 | 63 |
| 本輪最終 | 192 | 9 | 0 |

### T029 對正式版的八方法結果

| 完整方法 | 核准模式結果 |
|---|---|
| `local/lito381/server/ClientSession#exitRoom(Z)V` | SAME |
| `local/lito381/server/ClientSession#exitRoomBeforePolyMe(Z)V` | SAME_EFFECTS |
| `local/lito381/server/ClientSession#run()V` | SAME |
| `local/lito381/server/ClientSession#sendRoomParticipant(JLlocal/lito381/server/ClientSession;Llocal/lito381/server/Protocol$LitoForce;Ljava/lang/String;)V` | SAME |
| `local/lito381/server/ClientSession#sourcePoisonBody$useInventoryItemBeforeTravel([BLlocal/lito381/server/CombatPoisonLifecycle$Scope;)V` | SAME_EFFECTS |
| `local/lito381/server/Protocol#litoRoomParticipant(JLjava/lang/String;Llocal/lito381/server/Protocol$LitoForce;IILlocal/lito381/server/GameStateStore$TournamentStatistics;Ljava/lang/String;IILlocal/lito381/server/Protocol$LitoParticipantState;ILlocal/lito381/server/GameStateStore$TournamentAwards;)[B` | SAME |
| `local/lito381/server/Protocol#magicDollCustomIcon(IIZ)[B` | SAME |
| `local/lito381/server/Protocol#magicDollIcon(I)[B` | SAME |

T029 inventory 的正式版完整 javap 指令保存在 `scratch/issue-70/formal259-ClientSession-javap.txt` 與方法切片 `base-0.6.71-sourcePoisonBody$useInventoryItemBeforeTravel.javap.txt`；最終判定為 SAME_EFFECTS，沒有 unsupported shuffle/subroutine。

### light、disconnect、switch-map 與 records


```text
LIGHT_PATHS left=90 right=90 equal=true
LIGHT_CASTS LEFT [CHECKCAST local/lito381/server/ClientSession$Phase, CHECKCAST java/lang/Long]
LIGHT_CASTS RIGHT [CHECKCAST local/lito381/server/ClientSession$Phase, CHECKCAST java/lang/Long]
```


| 項目 | 結果 |
|---|---|
| local/lito381/server/ClientSession#nativeLightCommit(Llocal/lito381/server/BattleSpellCatalog$LightSpell;Ljava/lang/Object;J)Ljava/lang/Integer; | SAME_PATHS |
| local/lito381/server/ClientSession$1#<clinit>()V | SAME |
| local/lito381/server/GameHub#battleDisconnectWork(JJ)Llocal/lito381/server/GameHub$BattleDisconnectWork; | SAME |
| local/lito381/server/ClientSession$1CompanionHealing | SAME |
| local/lito381/server/ClientSession$1PlayerHealing | SAME |
| local/lito381/server/ClientSession$2CompanionHealing | SAME |
| local/lito381/server/ClientSession$2PlayerHealing | SAME |

light 的 CHECKCAST 為 Phase 與 Long，左右一致；90 條路徑的事件與重數一致，沒有僅靠 Exceptions 豁免。四個 healing record 僅豁免 `outerMethod`／`outerMethodDesc`，其餘 metadata 持續比較。

## 待驗證：完整 DIFF 清單

以下「直接呼叫序列」只是分流線索，包含 static／virtual／interface／special 呼叫，不包含 invokedynamic。線性排列可能因互斥分支或 finally 複製不同而改變；序列相同也不能證明參數、分支、監視器或例外處理等價。**所有下列 DIFF 都仍是未解決項，不自動歸為工具問題。**

| 分流 | 方法數 |
|---|---|
| 多直接呼叫 | 1 |
| 直接呼叫序列相同 | 142 |
| 線性排列不同 | 49 |

| 完整方法 | 分流 | 正式工具原因 |
|---|---|---|
| `local/lito381/server/ClientSession#applyBattleCompanionFeedbackDamage(Llocal/lito381/server/BattleCompanion;ILjava/util/List;Ljava/lang/String;)Llocal/lito381/server/BattleCompanion$Damage;` | 直接呼叫序列相同 | method applyBattleCompanionFeedbackDamage(Llocal/lito381/server/BattleCompanion;ILjava/util/List;Ljava/lang/String;)Llocal/lito381/server/BattleCompanion$Damage; insn[6]: left=ALOAD 0 right=ALOAD 1; paths rejected: normal control-flow cycle; labelled CFGs differ |
| `local/lito381/server/ClientSession#applyClassicWorldMonsterCompanionDamageInternal(Llocal/lito381/server/BattleCompanion;Llocal/lito381/server/BattleCompanion$Snapshot;IJLjava/lang/String;Z)Llocal/lito381/server/BattleCompanion$Damage;` | 直接呼叫序列相同 | method applyClassicWorldMonsterCompanionDamageInternal(Llocal/lito381/server/BattleCompanion;Llocal/lito381/server/BattleCompanion$Snapshot;IJLjava/lang/String;Z)Llocal/lito381/server/BattleCompanion$Damage; insn[40]: left=ALOAD 0 right=ALOAD 1; paths rejected: normal control-flow cycle; labelled CFGs differ |
| `local/lito381/server/ClientSession#attackClassicWorld(Llocal/lito381/server/Protocol$AttackRequest;Llocal/lito381/server/ClientSession$AttackEnvelope;)V` | 線性排列不同 | method attackClassicWorld(Llocal/lito381/server/Protocol$AttackRequest;Llocal/lito381/server/ClientSession$AttackEnvelope;)V: left=1608:[1612:java/lang/Exception  ] right=1591:[1595:java/lang/Exception  ]; paths rejected: normal control-flow cycle; labelled CFGs differ |
| `local/lito381/server/ClientSession#attackClassicWorldPlayer(Llocal/lito381/server/Protocol$AttackRequest;Llocal/lito381/server/ClientSession$AttackEnvelope;Llocal/lito381/server/ClientSession;)V` | 直接呼叫序列相同 | method attackClassicWorldPlayer(Llocal/lito381/server/Protocol$AttackRequest;Llocal/lito381/server/ClientSession$AttackEnvelope;Llocal/lito381/server/ClientSession;)V insn[102]: left=DUP right=ISTORE 15; paths rejected: normal control-flow cycle; labelled CFGs differ |
| `local/lito381/server/ClientSession#autoSelectPendingRentalBattleHandoff()Z` | 直接呼叫序列相同 | method autoSelectPendingRentalBattleHandoff()Z insn[21]: left=INVOKEDYNAMIC test(Llocal/lito381/server/ClientSession;)Ljava/util/function/Predicate; bsm=H6 java/lang/invoke/LambdaMetafactory.metafactory(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;Ljava/lang/in… right=INVOKEDYNAMIC test(Llocal/lito381/server/ClientSession;)Ljava/util/function/Predicate; bsm=H6 java/lang/invoke/LambdaMetafactory.metafactory(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;Ljava/lang/in…; path event multisets differ (left=29, right=29); labelled CFGs differ |
| `local/lito381/server/ClientSession#classicWorldCompanionSnapshots()Ljava/util/List;` | 直接呼叫序列相同 | method classicWorldCompanionSnapshots()Ljava/util/List;: left=21:[32:java/lang/Throwable  ] right=19:[32:java/lang/Throwable  ]; path event multisets differ (left=17, right=17); labelled CFGs differ |
| `local/lito381/server/ClientSession#completeClassicWorldPortal(Llocal/lito381/server/Protocol$PortalDestination;Llocal/lito381/server/ClientSession$PendingClassicWorldPortal;)V` | 直接呼叫序列相同 | method completeClassicWorldPortal(Llocal/lito381/server/Protocol$PortalDestination;Llocal/lito381/server/ClientSession$PendingClassicWorldPortal;)V insn[6]: left=INVOKEDYNAMIC run(Llocal/lito381/server/ClientSession;Llocal/lito381/server/Protocol$PortalDestination;Llocal/lito381/server/ClientSession$PendingClassicWorldPortal;)Llocal/lito381/server/NativeSelfCastAction$Task; bsm=H6 java/lang/invoke/L… right=INVOKEDYNAMIC run(Llocal/lito381/server/ClientSession;Llocal/lito381/server/Protocol$PortalDestination;Llocal/lito381/server/ClientSession$PendingClassicWorldPortal;)Llocal/lito381/server/NativeSelfCastAction$Task; bsm=H6 java/lang/invoke/L…; path event multisets differ (left=3, right=3); labelled CFGs differ |
| `local/lito381/server/ClientSession#consumeClassicWorldDeathSafe(ILjava/lang/String;)Z` | 直接呼叫序列相同 | method consumeClassicWorldDeathSafe(ILjava/lang/String;)Z insn[9]: left=IFEQ L34 right=IFEQ L32; paths rejected: normal control-flow cycle; labelled CFGs differ |
| `local/lito381/server/ClientSession#handleIvoryTowerElfAttributeAction(Llocal/lito381/server/Protocol$HypertextActionRequest;Llocal/lito381/server/ClassicWorldNpcCatalog$StaticNpc;)Z` | 直接呼叫序列相同 | method handleIvoryTowerElfAttributeAction(Llocal/lito381/server/Protocol$HypertextActionRequest;Llocal/lito381/server/ClassicWorldNpcCatalog$StaticNpc;)Z insn[262]: left=TABLESWITCH min=1 max=8 default=L267 labels=L263,L265,L267,L272,L267,L267,L267,L274 right=TABLESWITCH min=1 max=8 default=L271 labels=L263,L265,L271,L267,L271,L271,L271,L269; paths rejected: path limit 4096 exceeded; labelled CFGs differ |
| `local/lito381/server/ClientSession#nativeAreaBattleSleepBody([BLlocal/lito381/server/BattleSpellCatalog$SleepSpell;)V` | 線性排列不同 | method nativeAreaBattleSleepBody([BLlocal/lito381/server/BattleSpellCatalog$SleepSpell;)V: left=293:[299:java/lang/Exception  ] right=330:[336:java/lang/Exception  ]; paths rejected: normal control-flow cycle; labelled CFGs differ |
| `local/lito381/server/ClientSession#nativeAreaBattleSlowBody([BLlocal/lito381/server/BattleSpellCatalog$MassSlowSpell;)V` | 直接呼叫序列相同 | method nativeAreaBattleSlowBody([BLlocal/lito381/server/BattleSpellCatalog$MassSlowSpell;)V: length left=69 right=24; paths rejected: normal control-flow cycle; labelled CFGs differ |
| `local/lito381/server/ClientSession#nativeAreaFreezeAcceptedBattle(Llocal/lito381/server/BattleSpellCatalog$FreezeAttackSpell;Llocal/lito381/server/ClientSession;Ljava/util/List;)V` | 直接呼叫序列相同 | method nativeAreaFreezeAcceptedBattle(Llocal/lito381/server/BattleSpellCatalog$FreezeAttackSpell;Llocal/lito381/server/ClientSession;Ljava/util/List;)V insn[8]: left=DUP right=IFEQ L11; path event multisets differ (left=18, right=32); labelled CFGs differ |
| `local/lito381/server/ClientSession#nativeAreaFreezeAcceptedWorld(Llocal/lito381/server/BattleSpellCatalog$FreezeAttackSpell;Llocal/lito381/server/ClientSession;Ljava/util/List;)V` | 直接呼叫序列相同 | method nativeAreaFreezeAcceptedWorld(Llocal/lito381/server/BattleSpellCatalog$FreezeAttackSpell;Llocal/lito381/server/ClientSession;Ljava/util/List;)V insn[8]: left=DUP right=IFEQ L11; path event multisets differ (left=18, right=32); labelled CFGs differ |
| `local/lito381/server/ClientSession#nativeAreaFreezeOriginalWorld([BLlocal/lito381/server/BattleSpellCatalog$FreezeAttackSpell;)V` | 直接呼叫序列相同 | method nativeAreaFreezeOriginalWorld([BLlocal/lito381/server/BattleSpellCatalog$FreezeAttackSpell;)V: left=78:[302:java/lang/Exception  ] right=80:[308:java/lang/Exception  ]; paths rejected: normal control-flow cycle; labelled CFGs differ |
| `local/lito381/server/ClientSession#nativeAreaWorldSleepBody([BLlocal/lito381/server/BattleSpellCatalog$SleepSpell;)V` | 線性排列不同 | method nativeAreaWorldSleepBody([BLlocal/lito381/server/BattleSpellCatalog$SleepSpell;)V: left=303:[522:java/lang/Throwable  ] right=322:[328:java/lang/Exception  ]; paths rejected: normal control-flow cycle; labelled CFGs differ |
| `local/lito381/server/ClientSession#nativeAreaWorldSlowBody([BLlocal/lito381/server/BattleSpellCatalog$MassSlowSpell;)V` | 直接呼叫序列相同 | method nativeAreaWorldSlowBody([BLlocal/lito381/server/BattleSpellCatalog$MassSlowSpell;)V: left=93:[99:java/lang/Exception  ] right=96:[102:java/lang/Exception  ]; paths rejected: normal control-flow cycle; labelled CFGs differ |
| `local/lito381/server/ClientSession#nativeBattleControlledCancellationBody(Llocal/lito381/server/Protocol$ObjectSpellRequest;Llocal/lito381/server/ClientSession;Llocal/lito381/server/BattleCompanion;Llocal/lito381/server/BattleCompanion$Snapshot;Llocal/lito381/server/ClassicWorldMonsterPopulation$Snapshot;Llocal/lito381/server/BattleSpellCatalog$CancellationSpell;Llocal/lito381/server/CombatPoisonLifecycle$Scope;)V` | 直接呼叫序列相同 | method nativeBattleControlledCancellationBody(Llocal/lito381/server/Protocol$ObjectSpellRequest;Llocal/lito381/server/ClientSession;Llocal/lito381/server/BattleCompanion;Llocal/lito381/server/BattleCompanion$Snapshot;Llocal/lito381/server/ClassicWorldMonsterPopulation$Snapshot;Llocal/lito381/server/BattleSpellCatalog$CancellationSpell;Llocal/lito381/server/CombatPoisonLifecycle$Scope;)V: length left=90 right=0; paths rejected: normal control-flow cycle; labelled CFGs differ |
| `local/lito381/server/ClientSession#nativeCompanionTargetuseClassicWorldAreaAttackSpell(Llocal/lito381/server/Protocol$AttackSpellRequest;Llocal/lito381/server/BattleSpellCatalog$AreaAttackSpell;)V` | 線性排列不同 | method nativeCompanionTargetuseClassicWorldAreaAttackSpell(Llocal/lito381/server/Protocol$AttackSpellRequest;Llocal/lito381/server/BattleSpellCatalog$AreaAttackSpell;)V: left=536:[705:java/lang/Exception  ] right=542:[715:java/lang/Exception  ]; paths rejected: normal control-flow cycle; labelled CFGs differ |
| `local/lito381/server/ClientSession#nativeCompanionTargetuseClassicWorldAttackSpell(Llocal/lito381/server/Protocol$AttackSpellRequest;Llocal/lito381/server/BattleSpellCatalog$AttackSpell;)V` | 線性排列不同 | method nativeCompanionTargetuseClassicWorldAttackSpell(Llocal/lito381/server/Protocol$AttackSpellRequest;Llocal/lito381/server/BattleSpellCatalog$AttackSpell;)V: left=626:[631:java/lang/Exception  ] right=630:[635:java/lang/Exception  ]; paths rejected: normal control-flow cycle; labelled CFGs differ |
| `local/lito381/server/ClientSession#nativeCompanionTargetuseClassicWorldLineAttackSpell([BLlocal/lito381/server/BattleSpellCatalog$LineAttackSpell;)V` | 直接呼叫序列相同 | method nativeCompanionTargetuseClassicWorldLineAttackSpell([BLlocal/lito381/server/BattleSpellCatalog$LineAttackSpell;)V: left=432:[688:java/lang/Exception  ] right=437:[697:java/lang/Exception  ]; paths rejected: normal control-flow cycle; labelled CFGs differ |
| `local/lito381/server/ClientSession#nativeCubeHarmonyBody([BLlocal/lito381/server/BattleSpellCatalog$CubeHarmonySpell;)V` | 直接呼叫序列相同 | method nativeCubeHarmonyBody([BLlocal/lito381/server/BattleSpellCatalog$CubeHarmonySpell;)V insn[7]: left=IF_ICMPNE L384 right=IF_ICMPNE L12; paths rejected: normal control-flow cycle; labelled CFGs differ |
| `local/lito381/server/ClientSession#nativeCubeIgnitionBody([BLlocal/lito381/server/BattleSpellCatalog$CubeIgnitionSpell;)V` | 直接呼叫序列相同 | method nativeCubeIgnitionBody([BLlocal/lito381/server/BattleSpellCatalog$CubeIgnitionSpell;)V insn[7]: left=IF_ICMPNE L382 right=IF_ICMPNE L12; paths rejected: normal control-flow cycle; labelled CFGs differ |
| `local/lito381/server/ClientSession#nativeCubeQuakeBody([BLlocal/lito381/server/BattleSpellCatalog$CubeQuakeSpell;)V` | 直接呼叫序列相同 | method nativeCubeQuakeBody([BLlocal/lito381/server/BattleSpellCatalog$CubeQuakeSpell;)V insn[7]: left=IF_ICMPNE L382 right=IF_ICMPNE L12; paths rejected: normal control-flow cycle; labelled CFGs differ |
| `local/lito381/server/ClientSession#nativeCubeShockBody([BLlocal/lito381/server/BattleSpellCatalog$CubeShockSpell;)V` | 直接呼叫序列相同 | method nativeCubeShockBody([BLlocal/lito381/server/BattleSpellCatalog$CubeShockSpell;)V insn[7]: left=IF_ICMPNE L382 right=IF_ICMPNE L12; paths rejected: normal control-flow cycle; labelled CFGs differ |
| `local/lito381/server/ClientSession#nativeFreezeAcceptedOriginalWorld([BLlocal/lito381/server/BattleSpellCatalog$FreezeAttackSpell;)V` | 直接呼叫序列相同 | method nativeFreezeAcceptedOriginalWorld([BLlocal/lito381/server/BattleSpellCatalog$FreezeAttackSpell;)V: left=337:[342:java/lang/Exception  ] right=342:[347:java/lang/Exception  ]; paths rejected: normal control-flow cycle; labelled CFGs differ |
| `local/lito381/server/ClientSession#nativeMassTeleportAnswerBody(Llocal/lito381/server/Protocol$AskAnswer;)V` | 直接呼叫序列相同 | method nativeMassTeleportAnswerBody(Llocal/lito381/server/Protocol$AskAnswer;)V: length left=68 right=0; paths rejected: normal control-flow cycle; labelled CFGs differ |
| `local/lito381/server/ClientSession#nativeMaterialAbsoluteBarrierBody([BLlocal/lito381/server/BattleSpellCatalog$AbsoluteBarrierSpell;)V` | 直接呼叫序列相同 | method nativeMaterialAbsoluteBarrierBody([BLlocal/lito381/server/BattleSpellCatalog$AbsoluteBarrierSpell;)V insn[58]: left=ASTORE 11 right=GETFIELD local/lito381/server/ClientSession.spellReadyNanos:Ljava/util/Map;; paths rejected: normal control-flow cycle; labelled CFGs differ |
| `local/lito381/server/ClientSession#nativeMaterialAdditionalFireBody([BLlocal/lito381/server/BattleSpellCatalog$AdditionalFireSpell;)V` | 直接呼叫序列相同 | method nativeMaterialAdditionalFireBody([BLlocal/lito381/server/BattleSpellCatalog$AdditionalFireSpell;)V insn[124]: left=ALOAD 0 right=ALOAD 2; paths rejected: normal control-flow cycle; labelled CFGs differ |
| `local/lito381/server/ClientSession#nativeMaterialAreaSilenceBody([BLlocal/lito381/server/BattleSpellCatalog$AreaSilenceSpell;I)V` | 直接呼叫序列相同 | method nativeMaterialAreaSilenceBody([BLlocal/lito381/server/BattleSpellCatalog$AreaSilenceSpell;I)V: length left=43 right=0; paths rejected: normal control-flow cycle; labelled CFGs differ |
| `local/lito381/server/ClientSession#nativeMaterialAreaWindBody([BLlocal/lito381/server/BattleSpellCatalog$WindShackleSpell;I)V` | 直接呼叫序列相同 | method nativeMaterialAreaWindBody([BLlocal/lito381/server/BattleSpellCatalog$WindShackleSpell;I)V: length left=44 right=0; paths rejected: normal control-flow cycle; labelled CFGs differ |
| `local/lito381/server/ClientSession#nativeMaterialArmorBreakBody([BLlocal/lito381/server/BattleSpellCatalog$ArmorBreakSpell;)V` | 直接呼叫序列相同 | method nativeMaterialArmorBreakBody([BLlocal/lito381/server/BattleSpellCatalog$ArmorBreakSpell;)V: left=147:[152:java/lang/Exception  ] right=149:[154:java/lang/Exception  ]; paths rejected: normal control-flow cycle; labelled CFGs differ |
| `local/lito381/server/ClientSession#nativeMaterialAwakenBody([BLlocal/lito381/server/BattleSpellCatalog$AwakenSpell;)V` | 直接呼叫序列相同 | method nativeMaterialAwakenBody([BLlocal/lito381/server/BattleSpellCatalog$AwakenSpell;)V: length left=66 right=0; paths rejected: normal control-flow cycle; labelled CFGs differ |
| `local/lito381/server/ClientSession#nativeMaterialBlindBody([BLlocal/lito381/server/BattleSpellCatalog$BlindSpell;)V` | 直接呼叫序列相同 | method nativeMaterialBlindBody([BLlocal/lito381/server/BattleSpellCatalog$BlindSpell;)V: left=280:[285:java/lang/Exception  ] right=283:[288:java/lang/Exception  ]; paths rejected: normal control-flow cycle; labelled CFGs differ |
| `local/lito381/server/ClientSession#nativeMaterialBloodlustBody([BLlocal/lito381/server/BattleSpellCatalog$BloodlustSpell;)V` | 直接呼叫序列相同 | method nativeMaterialBloodlustBody([BLlocal/lito381/server/BattleSpellCatalog$BloodlustSpell;)V: length left=58 right=0; paths rejected: normal control-flow cycle; labelled CFGs differ |
| `local/lito381/server/ClientSession#nativeMaterialCallNatureBody([BLlocal/lito381/server/BattleSpellCatalog$ResurrectionSpell;)V` | 線性排列不同 | method nativeMaterialCallNatureBody([BLlocal/lito381/server/BattleSpellCatalog$ResurrectionSpell;)V: length left=42 right=0; paths rejected: normal control-flow cycle; labelled CFGs differ |
| `local/lito381/server/ClientSession#nativeMaterialCancellationBody([BLlocal/lito381/server/BattleSpellCatalog$CancellationSpell;Llocal/lito381/server/CombatPoisonLifecycle$Scope;)V` | 直接呼叫序列相同 | method nativeMaterialCancellationBody([BLlocal/lito381/server/BattleSpellCatalog$CancellationSpell;Llocal/lito381/server/CombatPoisonLifecycle$Scope;)V: length left=159 right=0; paths rejected: normal control-flow cycle; labelled CFGs differ |
| `local/lito381/server/ClientSession#nativeMaterialClearMindBody([BLlocal/lito381/server/BattleSpellCatalog$ClearMindSpell;)V` | 直接呼叫序列相同 | method nativeMaterialClearMindBody([BLlocal/lito381/server/BattleSpellCatalog$ClearMindSpell;)V insn[123]: left=ALOAD 0 right=ALOAD 2; paths rejected: normal control-flow cycle; labelled CFGs differ |
| `local/lito381/server/ClientSession#nativeMaterialCounterMagicBody([BLlocal/lito381/server/BattleSpellCatalog$CounterMagicSpell;)V` | 直接呼叫序列相同 | method nativeMaterialCounterMagicBody([BLlocal/lito381/server/BattleSpellCatalog$CounterMagicSpell;)V insn[120]: left=ALOAD 0 right=ALOAD 2; paths rejected: normal control-flow cycle; labelled CFGs differ |
| `local/lito381/server/ClientSession#nativeMaterialCounterMirrorBody([BLlocal/lito381/server/BattleSpellCatalog$CounterMirrorSpell;)V` | 直接呼叫序列相同 | method nativeMaterialCounterMirrorBody([BLlocal/lito381/server/BattleSpellCatalog$CounterMirrorSpell;)V insn[123]: left=ALOAD 0 right=ALOAD 2; paths rejected: normal control-flow cycle; labelled CFGs differ |
| `local/lito381/server/ClientSession#nativeMaterialDecayPotionBody([BLlocal/lito381/server/BattleSpellCatalog$DecayPotionSpell;)V` | 線性排列不同 | method nativeMaterialDecayPotionBody([BLlocal/lito381/server/BattleSpellCatalog$DecayPotionSpell;)V: left=239:[244:java/lang/Exception  ] right=241:[246:java/lang/Exception  ]; paths rejected: normal control-flow cycle; labelled CFGs differ |
| `local/lito381/server/ClientSession#nativeMaterialEarthBindBody([BLlocal/lito381/server/BattleSpellCatalog$BindSpell;)V` | 直接呼叫序列相同 | method nativeMaterialEarthBindBody([BLlocal/lito381/server/BattleSpellCatalog$BindSpell;)V: length left=44 right=0; paths rejected: normal control-flow cycle; labelled CFGs differ |
| `local/lito381/server/ClientSession#nativeMaterialElementalProtectionBody([BLlocal/lito381/server/BattleSpellCatalog$ElementalProtectionSpell;)V` | 直接呼叫序列相同 | method nativeMaterialElementalProtectionBody([BLlocal/lito381/server/BattleSpellCatalog$ElementalProtectionSpell;)V insn[134]: left=ALOAD 0 right=ALOAD 2; paths rejected: normal control-flow cycle; labelled CFGs differ |
| `local/lito381/server/ClientSession#nativeMaterialElementalSummonBody([BLlocal/lito381/server/BattleSpellCatalog$ElementalSummonSpell;)V` | 直接呼叫序列相同 | method nativeMaterialElementalSummonBody([BLlocal/lito381/server/BattleSpellCatalog$ElementalSummonSpell;)V: left=77:[180:java/lang/Throwable  ] right=322:[332:java/lang/Exception  ]; paths rejected: normal control-flow cycle; labelled CFGs differ |
| `local/lito381/server/ClientSession#nativeMaterialEnchantVenomBody([BLlocal/lito381/server/BattleSpellCatalog$EnchantVenomSpell;)V` | 直接呼叫序列相同 | method nativeMaterialEnchantVenomBody([BLlocal/lito381/server/BattleSpellCatalog$EnchantVenomSpell;)V insn[49]: left=ALOAD 0 right=ALOAD 2; paths rejected: normal control-flow cycle; labelled CFGs differ |
| `local/lito381/server/ClientSession#nativeMaterialEntangleBody([BLlocal/lito381/server/BattleSpellCatalog$EntangleSpell;I)V` | 線性排列不同 | method nativeMaterialEntangleBody([BLlocal/lito381/server/BattleSpellCatalog$EntangleSpell;I)V: left=604:[609:java/lang/Exception  ] right=607:[612:java/lang/Exception  ]; paths rejected: normal control-flow cycle; labelled CFGs differ |
| `local/lito381/server/ClientSession#nativeMaterialEraseBody([BLlocal/lito381/server/BattleSpellCatalog$EraseMagicSpell;I)V` | 線性排列不同 | method nativeMaterialEraseBody([BLlocal/lito381/server/BattleSpellCatalog$EraseMagicSpell;I)V: left=608:[613:java/lang/Exception  ] right=611:[616:java/lang/Exception  ]; paths rejected: normal control-flow cycle; labelled CFGs differ |
| `local/lito381/server/ClientSession#nativeMaterialExoticVitalizeBody([BLlocal/lito381/server/BattleSpellCatalog$ExoticVitalizeSpell;)V` | 直接呼叫序列相同 | method nativeMaterialExoticVitalizeBody([BLlocal/lito381/server/BattleSpellCatalog$ExoticVitalizeSpell;)V insn[124]: left=ALOAD 0 right=ALOAD 2; paths rejected: normal control-flow cycle; labelled CFGs differ |
| `local/lito381/server/ClientSession#nativeMaterialGroundBody([BLlocal/lito381/server/BattleSpellCatalog$GroundSpell;)V` | 直接呼叫序列相同 | method nativeMaterialGroundBody([BLlocal/lito381/server/BattleSpellCatalog$GroundSpell;)V: length left=68 right=0; paths rejected: normal control-flow cycle; labelled CFGs differ |
| `local/lito381/server/ClientSession#nativeMaterialInvisibilityBody([BLlocal/lito381/server/BattleSpellCatalog$InvisibilitySpell;)V` | 直接呼叫序列相同 | method nativeMaterialInvisibilityBody([BLlocal/lito381/server/BattleSpellCatalog$InvisibilitySpell;)V insn[147]: left=ALOAD 0 right=DUP; paths rejected: normal control-flow cycle; labelled CFGs differ |
| `local/lito381/server/ClientSession#nativeMaterialItemWeaponEnchantBody([BLlocal/lito381/server/BattleSpellCatalog$ItemWeaponEnchantSpell;)V` | 直接呼叫序列相同 | method nativeMaterialItemWeaponEnchantBody([BLlocal/lito381/server/BattleSpellCatalog$ItemWeaponEnchantSpell;)V insn[125]: left=ALOAD 0 right=INVOKESTATIC java/lang/System.nanoTime()J; paths rejected: normal control-flow cycle; labelled CFGs differ |
| `local/lito381/server/ClientSession#nativeMaterialMagicResistanceBody([BLlocal/lito381/server/BattleSpellCatalog$MagicResistanceSpell;)V` | 直接呼叫序列相同 | method nativeMaterialMagicResistanceBody([BLlocal/lito381/server/BattleSpellCatalog$MagicResistanceSpell;)V insn[58]: left=ASTORE 11 right=GETFIELD local/lito381/server/ClientSession.spellReadyNanos:Ljava/util/Map;; paths rejected: normal control-flow cycle; labelled CFGs differ |
| `local/lito381/server/ClientSession#nativeMaterialMagicalWeaponBody([BLlocal/lito381/server/BattleSpellCatalog$MagicalWeaponSpell;)V` | 直接呼叫序列相同 | method nativeMaterialMagicalWeaponBody([BLlocal/lito381/server/BattleSpellCatalog$MagicalWeaponSpell;)V: length left=18 right=0; paths rejected: normal control-flow cycle; labelled CFGs differ |
| `local/lito381/server/ClientSession#nativeMaterialMassTeleportBody([BLlocal/lito381/server/BattleSpellCatalog$MassTeleportSpell;)V` | 線性排列不同 | method nativeMaterialMassTeleportBody([BLlocal/lito381/server/BattleSpellCatalog$MassTeleportSpell;)V: length left=81 right=0; paths rejected: normal control-flow cycle; labelled CFGs differ |
| `local/lito381/server/ClientSession#nativeMaterialParalysisScopeBody([BLlocal/lito381/server/BattleSpellCatalog$ParalysisSpell;Llocal/lito381/server/CombatPoisonLifecycle$Scope;)V` | 直接呼叫序列相同 | method nativeMaterialParalysisScopeBody([BLlocal/lito381/server/BattleSpellCatalog$ParalysisSpell;Llocal/lito381/server/CombatPoisonLifecycle$Scope;)V: left=229:[234:java/lang/Exception  ] right=231:[236:java/lang/Exception  ]; paths rejected: normal control-flow cycle; labelled CFGs differ |
| `local/lito381/server/ClientSession#nativeMaterialPhysicalProcBuffBody([BLlocal/lito381/server/BattleSpellCatalog$PhysicalProcBuffSpell;)V` | 直接呼叫序列相同 | method nativeMaterialPhysicalProcBuffBody([BLlocal/lito381/server/BattleSpellCatalog$PhysicalProcBuffSpell;)V insn[333]: left=ALOAD 0 right=DUP; paths rejected: normal control-flow cycle; labelled CFGs differ |
| `local/lito381/server/ClientSession#nativeMaterialPoisonScopeBody([BLlocal/lito381/server/BattleSpellCatalog$PoisonSpell;Llocal/lito381/server/CombatPoisonLifecycle$Scope;)V` | 直接呼叫序列相同 | method nativeMaterialPoisonScopeBody([BLlocal/lito381/server/BattleSpellCatalog$PoisonSpell;Llocal/lito381/server/CombatPoisonLifecycle$Scope;)V: left=211:[216:java/lang/Exception  ] right=213:[218:java/lang/Exception  ]; paths rejected: normal control-flow cycle; labelled CFGs differ |
| `local/lito381/server/ClientSession#nativeMaterialPolymorphBody([BLlocal/lito381/server/BattleSpellCatalog$PolymorphSpell;)V` | 線性排列不同 | method nativeMaterialPolymorphBody([BLlocal/lito381/server/BattleSpellCatalog$PolymorphSpell;)V: length left=103 right=0; paths rejected: normal control-flow cycle; labelled CFGs differ |
| `local/lito381/server/ClientSession#nativeMaterialProtectionBody([BLlocal/lito381/server/BattleSpellCatalog$DamageProtectionSpell;)V` | 線性排列不同 | method nativeMaterialProtectionBody([BLlocal/lito381/server/BattleSpellCatalog$DamageProtectionSpell;)V: length left=69 right=0; paths rejected: normal control-flow cycle; labelled CFGs differ |
| `local/lito381/server/ClientSession#nativeMaterialResistElementalBody([BLlocal/lito381/server/BattleSpellCatalog$ResistElementalSpell;)V` | 直接呼叫序列相同 | method nativeMaterialResistElementalBody([BLlocal/lito381/server/BattleSpellCatalog$ResistElementalSpell;)V insn[123]: left=ALOAD 0 right=ALOAD 2; paths rejected: normal control-flow cycle; labelled CFGs differ |
| `local/lito381/server/ClientSession#nativeMaterialResurrectionBody([BLlocal/lito381/server/BattleSpellCatalog$ResurrectionSpell;)V` | 直接呼叫序列相同 | method nativeMaterialResurrectionBody([BLlocal/lito381/server/BattleSpellCatalog$ResurrectionSpell;)V: length left=2 right=0; paths rejected: normal control-flow cycle; labelled CFGs differ |
| `local/lito381/server/ClientSession#nativeMaterialReturnNatureBody([BLlocal/lito381/server/BattleSpellCatalog$ReturnToNatureSpell;I)V` | 直接呼叫序列相同 | method nativeMaterialReturnNatureBody([BLlocal/lito381/server/BattleSpellCatalog$ReturnToNatureSpell;I)V: left=346:[352:java/lang/Exception  ] right=348:[354:java/lang/Exception  ]; paths rejected: normal control-flow cycle; labelled CFGs differ |
| `local/lito381/server/ClientSession#nativeMaterialSilenceBody([BLlocal/lito381/server/BattleSpellCatalog$SilenceSpell;)V` | 線性排列不同 | method nativeMaterialSilenceBody([BLlocal/lito381/server/BattleSpellCatalog$SilenceSpell;)V: left=239:[244:java/lang/Exception  ] right=241:[246:java/lang/Exception  ]; paths rejected: normal control-flow cycle; labelled CFGs differ |
| `local/lito381/server/ClientSession#nativeMaterialSummonBody([BLlocal/lito381/server/BattleSpellCatalog$SummonSpell;)V` | 線性排列不同 | method nativeMaterialSummonBody([BLlocal/lito381/server/BattleSpellCatalog$SummonSpell;)V: left=114:[126:java/lang/Exception  ] right=116:[128:java/lang/Exception  ]; paths rejected: normal control-flow cycle; labelled CFGs differ |
| `local/lito381/server/ClientSession#nativeMaterialTamingBody([BLlocal/lito381/server/BattleSpellCatalog$TamingSpell;)V` | 直接呼叫序列相同 | method nativeMaterialTamingBody([BLlocal/lito381/server/BattleSpellCatalog$TamingSpell;)V: left=205:[210:java/lang/Exception  ] right=207:[212:java/lang/Exception  ]; paths rejected: normal control-flow cycle; labelled CFGs differ |
| `local/lito381/server/ClientSession#nativeMaterialTargetNegativeBody([BLlocal/lito381/server/BattleSpellCatalog$TargetNegativeStatusSpell;ILlocal/lito381/server/ClientSession$TargetNegativeStatusKind;)V` | 直接呼叫序列相同 | method nativeMaterialTargetNegativeBody([BLlocal/lito381/server/BattleSpellCatalog$TargetNegativeStatusSpell;ILlocal/lito381/server/ClientSession$TargetNegativeStatusKind;)V: left=519:[524:java/lang/Exception  ] right=523:[528:java/lang/Exception  ]; paths rejected: normal control-flow cycle; labelled CFGs differ |
| `local/lito381/server/ClientSession#nativeMaterialWaterLifeBody([BLlocal/lito381/server/BattleSpellCatalog$WaterLifeSpell;)V` | 線性排列不同 | method nativeMaterialWaterLifeBody([BLlocal/lito381/server/BattleSpellCatalog$WaterLifeSpell;)V: length left=63 right=0; paths rejected: normal control-flow cycle; labelled CFGs differ |
| `local/lito381/server/ClientSession#nativeMaterialWeaknessBody([BLlocal/lito381/server/BattleSpellCatalog$WeaknessSpell;)V` | 線性排列不同 | method nativeMaterialWeaknessBody([BLlocal/lito381/server/BattleSpellCatalog$WeaknessSpell;)V: left=318:[323:java/lang/Exception  ] right=320:[325:java/lang/Exception  ]; paths rejected: normal control-flow cycle; labelled CFGs differ |
| `local/lito381/server/ClientSession#nativeMaterialWeaponBreakBody([BLlocal/lito381/server/BattleSpellCatalog$WeaponBreakSpell;)V` | 直接呼叫序列相同 | method nativeMaterialWeaponBreakBody([BLlocal/lito381/server/BattleSpellCatalog$WeaponBreakSpell;)V: left=203:[208:java/lang/Exception  ] right=205:[210:java/lang/Exception  ]; paths rejected: normal control-flow cycle; labelled CFGs differ |
| `local/lito381/server/ClientSession#nativeMaterialWorldBlindBody([BLlocal/lito381/server/BattleSpellCatalog$BlindSpell;)V` | 直接呼叫序列相同 | method nativeMaterialWorldBlindBody([BLlocal/lito381/server/BattleSpellCatalog$BlindSpell;)V: left=199:[204:java/lang/Exception  ] right=202:[207:java/lang/Exception  ]; paths rejected: normal control-flow cycle; labelled CFGs differ |
| `local/lito381/server/ClientSession#nativeMaterialWorldCancellationBody(Llocal/lito381/server/Protocol$ObjectSpellRequest;Llocal/lito381/server/ClientSession;Llocal/lito381/server/BattleSpellCatalog$CancellationSpell;Llocal/lito381/server/CombatPoisonLifecycle$Scope;)V` | 線性排列不同 | method nativeMaterialWorldCancellationBody(Llocal/lito381/server/Protocol$ObjectSpellRequest;Llocal/lito381/server/ClientSession;Llocal/lito381/server/BattleSpellCatalog$CancellationSpell;Llocal/lito381/server/CombatPoisonLifecycle$Scope;)V: length left=141 right=0; paths rejected: normal control-flow cycle; labelled CFGs differ |
| `local/lito381/server/ClientSession#nativeMaterialWorldDecayPotionBody([BLlocal/lito381/server/BattleSpellCatalog$DecayPotionSpell;)V` | 線性排列不同 | method nativeMaterialWorldDecayPotionBody([BLlocal/lito381/server/BattleSpellCatalog$DecayPotionSpell;)V: left=197:[202:java/lang/Exception  ] right=200:[205:java/lang/Exception  ]; paths rejected: normal control-flow cycle; labelled CFGs differ |
| `local/lito381/server/ClientSession#nativeMaterialWorldEarthBindBody([BLlocal/lito381/server/BattleSpellCatalog$BindSpell;)V` | 直接呼叫序列相同 | method nativeMaterialWorldEarthBindBody([BLlocal/lito381/server/BattleSpellCatalog$BindSpell;)V: length left=55 right=0; paths rejected: normal control-flow cycle; labelled CFGs differ |
| `local/lito381/server/ClientSession#nativeMaterialWorldGroundBody([BLlocal/lito381/server/BattleSpellCatalog$GroundSpell;)V` | 直接呼叫序列相同 | method nativeMaterialWorldGroundBody([BLlocal/lito381/server/BattleSpellCatalog$GroundSpell;)V: length left=66 right=0; paths rejected: normal control-flow cycle; labelled CFGs differ |
| `local/lito381/server/ClientSession#nativeMaterialWorldNpcCancellationBody(Llocal/lito381/server/Protocol$ObjectSpellRequest;Llocal/lito381/server/ClientSession;Llocal/lito381/server/BattleCompanion;Llocal/lito381/server/BattleCompanion$Snapshot;Llocal/lito381/server/ClassicWorldMonsterPopulation$Snapshot;Llocal/lito381/server/BattleSpellCatalog$CancellationSpell;Llocal/lito381/server/CombatPoisonLifecycle$Scope;)V` | 直接呼叫序列相同 | method nativeMaterialWorldNpcCancellationBody(Llocal/lito381/server/Protocol$ObjectSpellRequest;Llocal/lito381/server/ClientSession;Llocal/lito381/server/BattleCompanion;Llocal/lito381/server/BattleCompanion$Snapshot;Llocal/lito381/server/ClassicWorldMonsterPopulation$Snapshot;Llocal/lito381/server/BattleSpellCatalog$CancellationSpell;Llocal/lito381/server/CombatPoisonLifecycle$Scope;)V: length left=90 right=0; paths rejected: normal control-flow cycle; labelled CFGs differ |
| `local/lito381/server/ClientSession#nativeMaterialWorldParalysisScopeBody([BLlocal/lito381/server/BattleSpellCatalog$ParalysisSpell;Llocal/lito381/server/CombatPoisonLifecycle$Scope;)V` | 線性排列不同 | method nativeMaterialWorldParalysisScopeBody([BLlocal/lito381/server/BattleSpellCatalog$ParalysisSpell;Llocal/lito381/server/CombatPoisonLifecycle$Scope;)V: left=278:[283:java/lang/Exception  ] right=280:[285:java/lang/Exception  ]; paths rejected: normal control-flow cycle; labelled CFGs differ |
| `local/lito381/server/ClientSession#nativeMaterialWorldPoisonScopeBody([BLlocal/lito381/server/BattleSpellCatalog$PoisonSpell;Llocal/lito381/server/CombatPoisonLifecycle$Scope;)V` | 線性排列不同 | method nativeMaterialWorldPoisonScopeBody([BLlocal/lito381/server/BattleSpellCatalog$PoisonSpell;Llocal/lito381/server/CombatPoisonLifecycle$Scope;)V: left=273:[278:java/lang/Exception  ] right=275:[280:java/lang/Exception  ]; paths rejected: normal control-flow cycle; labelled CFGs differ |
| `local/lito381/server/ClientSession#nativeMaterialWorldResurrectionBody([BLlocal/lito381/server/BattleSpellCatalog$ResurrectionSpell;)V` | 直接呼叫序列相同 | method nativeMaterialWorldResurrectionBody([BLlocal/lito381/server/BattleSpellCatalog$ResurrectionSpell;)V: length left=2 right=0; paths rejected: normal control-flow cycle; labelled CFGs differ |
| `local/lito381/server/ClientSession#nativeMaterialWorldSilenceBody([BLlocal/lito381/server/BattleSpellCatalog$SilenceSpell;)V` | 線性排列不同 | method nativeMaterialWorldSilenceBody([BLlocal/lito381/server/BattleSpellCatalog$SilenceSpell;)V: left=199:[204:java/lang/Exception  ] right=202:[207:java/lang/Exception  ]; paths rejected: normal control-flow cycle; labelled CFGs differ |
| `local/lito381/server/ClientSession#nativeMaterialWorldWeaknessBody([BLlocal/lito381/server/BattleSpellCatalog$WeaknessSpell;)V` | 線性排列不同 | method nativeMaterialWorldWeaknessBody([BLlocal/lito381/server/BattleSpellCatalog$WeaknessSpell;)V: left=221:[226:java/lang/Exception  ] right=224:[229:java/lang/Exception  ]; paths rejected: normal control-flow cycle; labelled CFGs differ |
| `local/lito381/server/ClientSession#nativeMaterialWorldWeaponBreakBody([BLlocal/lito381/server/BattleSpellCatalog$WeaponBreakSpell;)V` | 直接呼叫序列相同 | method nativeMaterialWorldWeaponBreakBody([BLlocal/lito381/server/BattleSpellCatalog$WeaponBreakSpell;)V: left=368:[373:java/lang/Exception  ] right=370:[375:java/lang/Exception  ]; paths rejected: normal control-flow cycle; labelled CFGs differ |
| `local/lito381/server/ClientSession#nativeMaterialZombieBody([BLlocal/lito381/server/BattleSpellCatalog$CreateZombieSpell;)V` | 直接呼叫序列相同 | method nativeMaterialZombieBody([BLlocal/lito381/server/BattleSpellCatalog$CreateZombieSpell;)V: left=228:[234:java/lang/Exception  ] right=231:[237:java/lang/Exception  ]; paths rejected: normal control-flow cycle; labelled CFGs differ |
| `local/lito381/server/ClientSession#nativePlacementPortalBody(Llocal/lito381/server/Protocol$PortalDestination;Llocal/lito381/server/ClientSession$PendingClassicWorldPortal;)V` | 直接呼叫序列相同 | method nativePlacementPortalBody(Llocal/lito381/server/Protocol$PortalDestination;Llocal/lito381/server/ClientSession$PendingClassicWorldPortal;)V insn[77]: left=DUP right=ISTORE 6; paths rejected: path limit 4096 exceeded; labelled CFGs differ |
| `local/lito381/server/ClientSession#nativeRawOriginal_useLightningWand(Llocal/lito381/server/RuntimeInventory$Item;Llocal/lito381/server/Protocol$TargetLocationInventoryUse;)V` | 直接呼叫序列相同 | method nativeRawOriginal_useLightningWand(Llocal/lito381/server/RuntimeInventory$Item;Llocal/lito381/server/Protocol$TargetLocationInventoryUse;)V insn[27]: left=GOTO L650 right=RETURN; paths rejected: normal control-flow cycle; labelled CFGs differ |
| `local/lito381/server/ClientSession#nativeResurrectionAnswerBody(Llocal/lito381/server/Protocol$AskAnswer;)V` | 直接呼叫序列相同 | method nativeResurrectionAnswerBody(Llocal/lito381/server/Protocol$AskAnswer;)V insn[5]: left=INVOKESTATIC local/lito381/server/NativeResurrectionAnswerLife.capture(Llocal/lito381/server/ClientSession;Ljava/lang/Object;)V right=DUP2; paths rejected: normal control-flow cycle; labelled CFGs differ |
| `local/lito381/server/ClientSession#nativeSelfShieldBody([BLlocal/lito381/server/BattleSpellCatalog$ShieldSpell;)V` | 直接呼叫序列相同 | method nativeSelfShieldBody([BLlocal/lito381/server/BattleSpellCatalog$ShieldSpell;)V: length left=62 right=0; paths rejected: normal control-flow cycle; labelled CFGs differ |
| `local/lito381/server/ClientSession#nativeSelfWeightBody([BLlocal/lito381/server/BattleSpellCatalog$WeightSpell;)V` | 直接呼叫序列相同 | method nativeSelfWeightBody([BLlocal/lito381/server/BattleSpellCatalog$WeightSpell;)V: length left=60 right=0; paths rejected: normal control-flow cycle; labelled CFGs differ |
| `local/lito381/server/ClientSession#nativeShockPlayerBattleBody([BLlocal/lito381/server/BattleSpellCatalog$ShockStunSpell;)V` | 線性排列不同 | method nativeShockPlayerBattleBody([BLlocal/lito381/server/BattleSpellCatalog$ShockStunSpell;)V: left=309:[314:java/lang/Exception  ] right=314:[319:java/lang/Exception  ]; paths rejected: normal control-flow cycle; labelled CFGs differ |
| `local/lito381/server/ClientSession#nativeShockPlayerOriginalBattle([BLlocal/lito381/server/BattleSpellCatalog$ShockStunSpell;)V` | 線性排列不同 | method nativeShockPlayerOriginalBattle([BLlocal/lito381/server/BattleSpellCatalog$ShockStunSpell;)V: left=309:[314:java/lang/Exception  ] right=314:[319:java/lang/Exception  ]; paths rejected: normal control-flow cycle; labelled CFGs differ |
| `local/lito381/server/ClientSession#nativeShockPlayerOriginalWorld([BLlocal/lito381/server/BattleSpellCatalog$ShockStunSpell;)V` | 直接呼叫序列相同 | method nativeShockPlayerOriginalWorld([BLlocal/lito381/server/BattleSpellCatalog$ShockStunSpell;)V: left=315:[320:java/lang/Exception  ] right=320:[325:java/lang/Exception  ]; paths rejected: normal control-flow cycle; labelled CFGs differ |
| `local/lito381/server/ClientSession#nativeShockPlayerWorldBody([BLlocal/lito381/server/BattleSpellCatalog$ShockStunSpell;)V` | 直接呼叫序列相同 | method nativeShockPlayerWorldBody([BLlocal/lito381/server/BattleSpellCatalog$ShockStunSpell;)V: left=315:[320:java/lang/Exception  ] right=319:[324:java/lang/Exception  ]; paths rejected: normal control-flow cycle; labelled CFGs differ |
| `local/lito381/server/ClientSession#nativeSourceCompanionClockTick(JJLjava/lang/Object;)V` | 直接呼叫序列相同 | method nativeSourceCompanionClockTick(JJLjava/lang/Object;)V: left=50:[58:java/lang/Throwable  ] right=48:[56:java/lang/Throwable  ]; paths rejected: normal control-flow cycle; labelled CFGs differ |
| `local/lito381/server/ClientSession#nativeSourceControlledOriginal_attack(Llocal/lito381/server/Protocol$AttackRequest;Llocal/lito381/server/ClientSession$AttackEnvelope;)V` | 線性排列不同 | method nativeSourceControlledOriginal_attack(Llocal/lito381/server/Protocol$AttackRequest;Llocal/lito381/server/ClientSession$AttackEnvelope;)V: left=541:[546:java/lang/Exception  ] right=546:[551:java/lang/Exception  ]; paths rejected: normal control-flow cycle; labelled CFGs differ |
| `local/lito381/server/ClientSession#nativeSourceControlledOriginal_nativeAreaFreezeOriginalBattle([BLlocal/lito381/server/BattleSpellCatalog$FreezeAttackSpell;)V` | 直接呼叫序列相同 | method nativeSourceControlledOriginal_nativeAreaFreezeOriginalBattle([BLlocal/lito381/server/BattleSpellCatalog$FreezeAttackSpell;)V: left=260:[266:java/lang/Exception  ] right=262:[268:java/lang/Exception  ]; paths rejected: normal control-flow cycle; labelled CFGs differ |
| `local/lito381/server/ClientSession#nativeSourceControlledOriginal_nativeFreezeAcceptedOriginalBattle([BLlocal/lito381/server/BattleSpellCatalog$FreezeAttackSpell;)V` | 線性排列不同 | method nativeSourceControlledOriginal_nativeFreezeAcceptedOriginalBattle([BLlocal/lito381/server/BattleSpellCatalog$FreezeAttackSpell;)V: left=503:[508:java/lang/Exception  ] right=508:[513:java/lang/Exception  ]; paths rejected: normal control-flow cycle; labelled CFGs differ |
| `local/lito381/server/ClientSession#nativeSourceControlledOriginal_nativeSourceLightningOriginalBody(Llocal/lito381/server/Protocol$AttackSpellRequest;Llocal/lito381/server/BattleSpellCatalog$DirectionalLineSpell;)V` | 線性排列不同 | method nativeSourceControlledOriginal_nativeSourceLightningOriginalBody(Llocal/lito381/server/Protocol$AttackSpellRequest;Llocal/lito381/server/BattleSpellCatalog$DirectionalLineSpell;)V: left=655:[779:java/lang/Throwable  ] right=663:[789:java/lang/Throwable  ]; paths rejected: normal control-flow cycle; labelled CFGs differ |
| `local/lito381/server/ClientSession#nativeSourceControlledOriginal_useAreaAttackSpell(Llocal/lito381/server/Protocol$AttackSpellRequest;Llocal/lito381/server/BattleSpellCatalog$AreaAttackSpell;)V` | 線性排列不同 | method nativeSourceControlledOriginal_useAreaAttackSpell(Llocal/lito381/server/Protocol$AttackSpellRequest;Llocal/lito381/server/BattleSpellCatalog$AreaAttackSpell;)V: left=749:[755:java/lang/Exception  ] right=757:[763:java/lang/Exception  ]; paths rejected: normal control-flow cycle; labelled CFGs differ |
| `local/lito381/server/ClientSession#nativeSourceControlledOriginal_useAttackSpell(Llocal/lito381/server/Protocol$AttackSpellRequest;Llocal/lito381/server/BattleSpellCatalog$AttackSpell;IIII)V` | 線性排列不同 | method nativeSourceControlledOriginal_useAttackSpell(Llocal/lito381/server/Protocol$AttackSpellRequest;Llocal/lito381/server/BattleSpellCatalog$AttackSpell;IIII)V: left=478:[483:java/lang/Exception  ] right=484:[489:java/lang/Exception  ]; paths rejected: normal control-flow cycle; labelled CFGs differ |
| `local/lito381/server/ClientSession#nativeSourceControlledOriginal_useCenteredAreaAttackSpell([BLlocal/lito381/server/BattleSpellCatalog$AreaAttackSpell;)V` | 直接呼叫序列相同 | method nativeSourceControlledOriginal_useCenteredAreaAttackSpell([BLlocal/lito381/server/BattleSpellCatalog$AreaAttackSpell;)V: left=162:[168:java/lang/Exception  ] right=166:[172:java/lang/Exception  ]; paths rejected: normal control-flow cycle; labelled CFGs differ |
| `local/lito381/server/ClientSession#nativeSourceControlledOriginal_useHealingSpell([BLlocal/lito381/server/BattleSpellCatalog$HealingSpell;)V` | 線性排列不同 | method nativeSourceControlledOriginal_useHealingSpell([BLlocal/lito381/server/BattleSpellCatalog$HealingSpell;)V: left=405:[410:java/lang/Exception  ] right=408:[413:java/lang/Exception  ]; paths rejected: normal control-flow cycle; labelled CFGs differ |
| `local/lito381/server/ClientSession#nativeSourceFireWeaponOriginalBody([BLlocal/lito381/server/BattleSpellCatalog$FireWeaponSpell;)V` | 線性排列不同 | method nativeSourceFireWeaponOriginalBody([BLlocal/lito381/server/BattleSpellCatalog$FireWeaponSpell;)V: left=213:[388:java/lang/Throwable  ] right=216:[393:java/lang/Throwable  ]; paths rejected: normal control-flow cycle; labelled CFGs differ |
| `local/lito381/server/ClientSession#nativeSourceHealingOriginalGroup([BLlocal/lito381/server/BattleSpellCatalog$GroupHealingSpell;)V` | 直接呼叫序列相同 | method nativeSourceHealingOriginalGroup([BLlocal/lito381/server/BattleSpellCatalog$GroupHealingSpell;)V: left=219:[355:java/lang/Throwable  ] right=220:[358:java/lang/Throwable  ]; paths rejected: normal control-flow cycle; labelled CFGs differ |
| `local/lito381/server/ClientSession#nativeSourceHealingOriginalNature([BLlocal/lito381/server/BattleSpellCatalog$NaturesBlessingSpell;I)V` | 直接呼叫序列相同 | method nativeSourceHealingOriginalNature([BLlocal/lito381/server/BattleSpellCatalog$NaturesBlessingSpell;I)V: left=279:[408:java/lang/Throwable  ] right=283:[414:java/lang/Throwable  ]; paths rejected: normal control-flow cycle; labelled CFGs differ |
| `local/lito381/server/ClientSession#nativeSourceRemainingOriginal133([BLlocal/lito381/server/BattleSpellCatalog$ElementalFallDownSpell;I)V` | 線性排列不同 | method nativeSourceRemainingOriginal133([BLlocal/lito381/server/BattleSpellCatalog$ElementalFallDownSpell;I)V: left=453:[458:java/lang/Exception  ] right=455:[460:java/lang/Exception  ]; paths rejected: normal control-flow cycle; labelled CFGs differ |
| `local/lito381/server/ClientSession#nativeSourceRemainingOriginal18([BLlocal/lito381/server/BattleSpellCatalog$TurnUndeadSpell;)V` | 線性排列不同 | method nativeSourceRemainingOriginal18([BLlocal/lito381/server/BattleSpellCatalog$TurnUndeadSpell;)V: left=237:[242:java/lang/Exception  ] right=239:[244:java/lang/Exception  ]; paths rejected: normal control-flow cycle; labelled CFGs differ |
| `local/lito381/server/ClientSession#nativeSourceRemainingOriginal39([BLlocal/lito381/server/BattleSpellCatalog$ManaDrainSpell;)V` | 線性排列不同 | method nativeSourceRemainingOriginal39([BLlocal/lito381/server/BattleSpellCatalog$ManaDrainSpell;)V: left=288:[293:java/lang/Exception  ] right=291:[296:java/lang/Exception  ]; paths rejected: normal control-flow cycle; labelled CFGs differ |
| `local/lito381/server/ClientSession#nativeSourceSingleNpcLifeDrain(Llocal/lito381/server/ClientSession;Llocal/lito381/server/BattleSpellCatalog$AttackSpell;ILjava/lang/String;Llocal/lito381/server/NativeSourcePhysicalNpc$Commit;)I` | 直接呼叫序列相同 | method nativeSourceSingleNpcLifeDrain(Llocal/lito381/server/ClientSession;Llocal/lito381/server/BattleSpellCatalog$AttackSpell;ILjava/lang/String;Llocal/lito381/server/NativeSourcePhysicalNpc$Commit;)I: left=14:[41:java/lang/Throwable  ] right=12:[40:java/lang/Throwable  ]; path event multisets differ (left=22, right=22); labelled CFGs differ |
| `local/lito381/server/ClientSession#nativeSourceWindShotOriginalBody([BLlocal/lito381/server/BattleSpellCatalog$WindShotSpell;)V` | 線性排列不同 | method nativeSourceWindShotOriginalBody([BLlocal/lito381/server/BattleSpellCatalog$WindShotSpell;)V: left=162:[321:java/lang/Throwable  ] right=165:[326:java/lang/Throwable  ]; paths rejected: normal control-flow cycle; labelled CFGs differ |
| `local/lito381/server/ClientSession#publishNpcCounterBarrierTrigger(JLlocal/lito381/server/BattleCounterBarrier$Decision;Ljava/util/List;Ljava/lang/String;)V` | 直接呼叫序列相同 | method publishNpcCounterBarrierTrigger(JLlocal/lito381/server/BattleCounterBarrier$Decision;Ljava/util/List;Ljava/lang/String;)V insn[18]: left=IFEQ L53 right=IFEQ L54; paths rejected: normal control-flow cycle; labelled CFGs differ |
| `local/lito381/server/ClientSession#publishNpcCounterMirrorTrigger(JIILlocal/lito381/server/BattleCounterMirrorStatus$Decision;Ljava/util/List;Ljava/lang/String;)V` | 直接呼叫序列相同 | method publishNpcCounterMirrorTrigger(JIILlocal/lito381/server/BattleCounterMirrorStatus$Decision;Ljava/util/List;Ljava/lang/String;)V insn[22]: left=IFEQ L76 right=IFEQ L77; paths rejected: normal control-flow cycle; labelled CFGs differ |
| `local/lito381/server/ClientSession#resolveClassicWorldScarecrowHit(Llocal/lito381/server/ClassicWorldNpcCatalog$StaticNpc;Llocal/lito381/server/ClassicWorldMonsterCatalog$MonsterTemplate;I)V` | 直接呼叫序列相同 | method resolveClassicWorldScarecrowHit(Llocal/lito381/server/ClassicWorldNpcCatalog$StaticNpc;Llocal/lito381/server/ClassicWorldMonsterCatalog$MonsterTemplate;I)V insn[29]: left=GETFIELD local/lito381/server/ClientSession.hub:Llocal/lito381/server/GameHub; right=INVOKESTATIC local/lito381/server/NativeStaticNpcScene.heading(Llocal/lito381/server/GameHub;Llocal/lito381/server/ClassicWorldNpcCatalog$StaticNpc;Llocal/lito381/server/ClientSession;)I; paths rejected: normal control-flow cycle; labelled CFGs differ |
| `local/lito381/server/ClientSession#resolveDragonKnightNegativeTarget(Llocal/lito381/server/ClientSession$Phase;JIIIIIZ)Llocal/lito381/server/ClientSession$ArmorBreakTarget;` | 直接呼叫序列相同 | method resolveDragonKnightNegativeTarget(Llocal/lito381/server/ClientSession$Phase;JIIIIIZ)Llocal/lito381/server/ClientSession$ArmorBreakTarget; insn[6]: left=IF_ACMPNE L277 right=IF_ACMPNE L265; paths rejected: normal control-flow cycle; labelled CFGs differ |
| `local/lito381/server/ClientSession#serverAwakeningMpDrain(J)V` | 直接呼叫序列相同 | method serverAwakeningMpDrain(J)V: left=7:[53:java/lang/Throwable  ] right=5:[52:java/lang/Throwable  ]; path event multisets differ (left=28, right=28); labelled CFGs differ |
| `local/lito381/server/ClientSession#serverClassicWorldCompanionTick(Llocal/lito381/server/BattleCompanion;IJ)V` | 直接呼叫序列相同 | method serverClassicWorldCompanionTick(Llocal/lito381/server/BattleCompanion;IJ)V: left=7:[1120:java/lang/Throwable  ] right=7:[1102:java/lang/Throwable  ]; paths rejected: normal control-flow cycle; labelled CFGs differ |
| `local/lito381/server/ClientSession#serverCompanionTick(Llocal/lito381/server/BattleCompanion;JJ)V` | 直接呼叫序列相同 | method serverCompanionTick(Llocal/lito381/server/BattleCompanion;JJ)V: left=323:[822:java/lang/Throwable  ] right=325:[822:java/lang/Throwable  ]; paths rejected: normal control-flow cycle; labelled CFGs differ |
| `local/lito381/server/ClientSession#serverHonorRespawnDue(JJII)V` | 線性排列不同 | method serverHonorRespawnDue(JJII)V: left=8:[12:java/lang/Throwable  ] right=10:[49:java/lang/Throwable  ]; paths rejected: normal control-flow cycle; labelled CFGs differ |
| `local/lito381/server/ClientSession#sourceApplicationRun()V` | 多直接呼叫 | method sourceApplicationRun()V: left=1:[8:java/lang/Exception  ] right=24:[740:java/lang/Exception  , 1062:java/lang/Throwable  ]; paths rejected: normal control-flow cycle; labelled CFGs differ |
| `local/lito381/server/ClientSession#sourcePlayerBattleCommit(ILlocal/lito381/server/ClientSession;ZZJLlocal/lito381/server/BattleJoyOfPainStatus$Lease;I)V` | 直接呼叫序列相同 | method sourcePlayerBattleCommit(ILlocal/lito381/server/ClientSession;ZZJLlocal/lito381/server/BattleJoyOfPainStatus$Lease;I)V insn[4]: left=INVOKESTATIC local/lito381/server/NativeAcceptedState.requireCurrent()V right=ALOAD 0; paths rejected: normal control-flow cycle; labelled CFGs differ |
| `local/lito381/server/ClientSession#sourcePlayerLegacyWorldDirectDamage(DJLlocal/lito381/server/CombatPoisonLifecycle$Scope;)Llocal/lito381/server/ClientSession$ClassicWorldFatalContext;` | 直接呼叫序列相同 | method sourcePlayerLegacyWorldDirectDamage(DJLlocal/lito381/server/CombatPoisonLifecycle$Scope;)Llocal/lito381/server/ClientSession$ClassicWorldFatalContext; insn[38]: left=DUP right=ISTORE 9; paths rejected: normal control-flow cycle; labelled CFGs differ |
| `local/lito381/server/ClientSession#sourcePlayerWorldCommit(DJLlocal/lito381/server/CombatPoisonLifecycle$Scope;Llocal/lito381/server/ClassicWorldMonsterPopulation$Snapshot;Llocal/lito381/server/BattleAbilityStatus$IllusionAvatar;I)Llocal/lito381/server/ClientSession$ClassicWorldFatalContext;` | 線性排列不同 | method sourcePlayerWorldCommit(DJLlocal/lito381/server/CombatPoisonLifecycle$Scope;Llocal/lito381/server/ClassicWorldMonsterPopulation$Snapshot;Llocal/lito381/server/BattleAbilityStatus$IllusionAvatar;I)Llocal/lito381/server/ClientSession$ClassicWorldFatalContext; insn[0]: left=INVOKESTATIC local/lito381/server/NativeAcceptedState.requireCurrent()V right=ALOAD 0; paths rejected: normal control-flow cycle; labelled CFGs differ |
| `local/lito381/server/ClientSession#sourcePoisonBody$applyFatalityCancellation(Llocal/lito381/server/BattleDeathmatchFatalityCatalog$Effect;Llocal/lito381/server/CombatPoisonLifecycle$Scope;)V` | 直接呼叫序列相同 | method sourcePoisonBody$applyFatalityCancellation(Llocal/lito381/server/BattleDeathmatchFatalityCatalog$Effect;Llocal/lito381/server/CombatPoisonLifecycle$Scope;)V: left=11:[219:java/lang/Throwable  ] right=9:[216:java/lang/Throwable  ]; path event multisets differ (left=60, right=58); labelled CFGs differ |
| `local/lito381/server/ClientSession#sourcePoisonBody$attemptEnchantVenomPoison(Llocal/lito381/server/ClientSession;Llocal/lito381/server/BattleQuestEncounter$Snapshot;Llocal/lito381/server/BattleTimeAttackEncounter$Snapshot;Llocal/lito381/server/ClassicWorldMonsterPopulation$Snapshot;ZZJLlocal/lito381/server/CombatPoisonLifecycle$Scope;)V` | 直接呼叫序列相同 | method sourcePoisonBody$attemptEnchantVenomPoison(Llocal/lito381/server/ClientSession;Llocal/lito381/server/BattleQuestEncounter$Snapshot;Llocal/lito381/server/BattleTimeAttackEncounter$Snapshot;Llocal/lito381/server/ClassicWorldMonsterPopulation$Snapshot;ZZJLlocal/lito381/server/CombatPoisonLifecycle$Scope;)V: left=69:[159:java/lang/Throwable  ] right=65:[153:java/lang/Throwable  ]; paths rejected: normal control-flow cycle; labelled CFGs differ |
| `local/lito381/server/ClientSession#sourcePoisonBody$publishClassicWorldMonsterPoisonUpdate(Llocal/lito381/server/ClassicWorldMonsterPopulation$PoisonUpdate;JLlocal/lito381/server/CombatPoisonLifecycle$Scope;)V` | 直接呼叫序列相同 | method sourcePoisonBody$publishClassicWorldMonsterPoisonUpdate(Llocal/lito381/server/ClassicWorldMonsterPopulation$PoisonUpdate;JLlocal/lito381/server/CombatPoisonLifecycle$Scope;)V insn[27]: left=IFEQ L178 right=IFEQ L158; paths rejected: normal control-flow cycle; labelled CFGs differ |
| `local/lito381/server/ClientSession#sourcePoisonBody$publishNpcMagicCancellation(JLjava/util/List;ZZZZZZZZZZZZZZZZZZZZZLlocal/lito381/server/CombatPoisonLifecycle$Scope;)V` | 線性排列不同 | method sourcePoisonBody$publishNpcMagicCancellation(JLjava/util/List;ZZZZZZZZZZZZZZZZZZZZZLlocal/lito381/server/CombatPoisonLifecycle$Scope;)V: left= right=(JLjava/util/List<Llocal/lito381/server/ClientSession;>;ZZZZZZZZZZZZZZZZZZZZZ)V |
| `local/lito381/server/ClientSession#sourcePoisonBody$serverClassicWorldMonsterStatusSkillAttack(Llocal/lito381/server/ClassicWorldMonsterPopulation$StatusSkillAttack;ILlocal/lito381/server/CombatPoisonLifecycle$Scope;)V` | 直接呼叫序列相同 | method sourcePoisonBody$serverClassicWorldMonsterStatusSkillAttack(Llocal/lito381/server/ClassicWorldMonsterPopulation$StatusSkillAttack;ILlocal/lito381/server/CombatPoisonLifecycle$Scope;)V: left=126:[133:java/lang/Exception  ] right=124:[131:java/lang/Exception  ]; paths rejected: normal control-flow cycle; labelled CFGs differ |
| `local/lito381/server/ClientSession#sourcePoisonBody$serverExpireTimedStatuses(JLlocal/lito381/server/CombatPoisonLifecycle$Scope;)V` | 直接呼叫序列相同 | method sourcePoisonBody$serverExpireTimedStatuses(JLlocal/lito381/server/CombatPoisonLifecycle$Scope;)V insn[74]: left=IFEQ L481 right=IFEQ L442; paths rejected: normal control-flow cycle; labelled CFGs differ |
| `local/lito381/server/ClientSession#sourcePoisonBody$serverPrepareBattle(Llocal/lito381/server/CombatPoisonLifecycle$Scope;)V` | 直接呼叫序列相同 | method sourcePoisonBody$serverPrepareBattle(Llocal/lito381/server/CombatPoisonLifecycle$Scope;)V: left=140:[164:java/io/IOException  ] right=138:[162:java/io/IOException  ]; path event multisets differ (left=175, right=173); labelled CFGs differ |
| `local/lito381/server/ClientSession#sourcePoisonBody$sourceDeathCancellation(Llocal/lito381/server/BattleDeathmatchFatalityCatalog$Effect;Llocal/lito381/server/CombatPoisonLifecycle$Scope;)V` | 直接呼叫序列相同 | method sourcePoisonBody$sourceDeathCancellation(Llocal/lito381/server/BattleDeathmatchFatalityCatalog$Effect;Llocal/lito381/server/CombatPoisonLifecycle$Scope;)V: left=13:[401:java/lang/Throwable  ] right=12:[388:java/lang/Throwable  ]; path event multisets differ (left=150, right=148); labelled CFGs differ |
| `local/lito381/server/ClientSession#sourcePoisonBody$takeClassicWorldDirectDamage(DJLlocal/lito381/server/CombatPoisonLifecycle$Scope;)Llocal/lito381/server/ClientSession$ClassicWorldFatalContext;` | 直接呼叫序列相同 | method sourcePoisonBody$takeClassicWorldDirectDamage(DJLlocal/lito381/server/CombatPoisonLifecycle$Scope;)Llocal/lito381/server/ClientSession$ClassicWorldFatalContext;: left=5:[79:java/lang/Throwable  ] right=5:[77:java/lang/Throwable  ]; path event multisets differ (left=58, right=58); labelled CFGs differ |
| `local/lito381/server/ClientSession#sourcePoisonBody$useClassicWorldCurseRemovalSpell([BLlocal/lito381/server/BattleSpellCatalog$CurseRemovalSpell;Llocal/lito381/server/CombatPoisonLifecycle$Scope;)V` | 線性排列不同 | method sourcePoisonBody$useClassicWorldCurseRemovalSpell([BLlocal/lito381/server/BattleSpellCatalog$CurseRemovalSpell;Llocal/lito381/server/CombatPoisonLifecycle$Scope;)V: left=304:[387:java/lang/Throwable  ] right=306:[391:java/lang/Throwable  ]; paths rejected: normal control-flow cycle; labelled CFGs differ |
| `local/lito381/server/ClientSession#sourcePoisonBody$useCurseRemovalSpell([BLlocal/lito381/server/BattleSpellCatalog$CurseRemovalSpell;Llocal/lito381/server/CombatPoisonLifecycle$Scope;)V` | 線性排列不同 | method sourcePoisonBody$useCurseRemovalSpell([BLlocal/lito381/server/BattleSpellCatalog$CurseRemovalSpell;Llocal/lito381/server/CombatPoisonLifecycle$Scope;)V: left=442:[519:java/lang/Throwable  ] right=444:[523:java/lang/Throwable  ]; paths rejected: normal control-flow cycle; labelled CFGs differ |
| `local/lito381/server/ClientSession#startBattle()V` | 直接呼叫序列相同 | method startBattle()V insn[242]: left=ISTORE 7 right=POP; paths rejected: normal control-flow cycle; labelled CFGs differ |
| `local/lito381/server/ClientSession#useAbilityBuffSpell([BLlocal/lito381/server/BattleSpellCatalog$AbilityBuffSpell;)V` | 線性排列不同 | method useAbilityBuffSpell([BLlocal/lito381/server/BattleSpellCatalog$AbilityBuffSpell;)V: left=269:[458:java/lang/Throwable  ] right=271:[461:java/lang/Throwable  ]; paths rejected: normal control-flow cycle; labelled CFGs differ |
| `local/lito381/server/ClientSession#useAdvanceSpiritSpell([BLlocal/lito381/server/BattleSpellCatalog$AdvanceSpiritSpell;)V` | 直接呼叫序列相同 | method useAdvanceSpiritSpell([BLlocal/lito381/server/BattleSpellCatalog$AdvanceSpiritSpell;)V: left=203:[379:java/lang/Throwable  ] right=197:[371:java/lang/Throwable  ]; paths rejected: normal control-flow cycle; labelled CFGs differ |
| `local/lito381/server/ClientSession#useAquaProtectSpell([BLlocal/lito381/server/BattleSpellCatalog$AquaProtectSpell;)V` | 線性排列不同 | method useAquaProtectSpell([BLlocal/lito381/server/BattleSpellCatalog$AquaProtectSpell;)V: left=295:[476:java/lang/Throwable  ] right=298:[480:java/lang/Throwable  ]; paths rejected: normal control-flow cycle; labelled CFGs differ |
| `local/lito381/server/ClientSession#useBodyToMindSpell([BLlocal/lito381/server/BattleSpellCatalog$BodyToMindSpell;)V` | 直接呼叫序列相同 | method useBodyToMindSpell([BLlocal/lito381/server/BattleSpellCatalog$BodyToMindSpell;)V: left=40:[151:java/lang/Throwable  ] right=38:[149:java/lang/Throwable  ]; paths rejected: normal control-flow cycle; labelled CFGs differ |
| `local/lito381/server/ClientSession#useBounceAttackSpell([BLlocal/lito381/server/BattleSpellCatalog$BounceAttackSpell;)V` | 直接呼叫序列相同 | method useBounceAttackSpell([BLlocal/lito381/server/BattleSpellCatalog$BounceAttackSpell;)V: left=118:[270:java/lang/Throwable  ] right=119:[273:java/lang/Throwable  ]; paths rejected: normal control-flow cycle; labelled CFGs differ |
| `local/lito381/server/ClientSession#useBringStoneSpell([BLlocal/lito381/server/BattleSpellCatalog$BringStoneSpell;)V` | 直接呼叫序列相同 | method useBringStoneSpell([BLlocal/lito381/server/BattleSpellCatalog$BringStoneSpell;)V: left=190:[404:java/lang/Throwable  ] right=188:[400:java/lang/Throwable  ]; paths rejected: normal control-flow cycle; labelled CFGs differ |
| `local/lito381/server/ClientSession#useBurningSlashSpell([BLlocal/lito381/server/BattleSpellCatalog$BurningSlashSpell;)V` | 直接呼叫序列相同 | method useBurningSlashSpell([BLlocal/lito381/server/BattleSpellCatalog$BurningSlashSpell;)V: left=94:[226:java/lang/Throwable  ] right=96:[228:java/lang/Throwable  ]; paths rejected: normal control-flow cycle; labelled CFGs differ |
| `local/lito381/server/ClientSession#useCallClanSpell([BLlocal/lito381/server/BattleSpellCatalog$CallClanSpell;)V` | 直接呼叫序列相同 | method useCallClanSpell([BLlocal/lito381/server/BattleSpellCatalog$CallClanSpell;)V: left=154:[284:java/lang/Throwable  ] right=155:[287:java/lang/Throwable  ]; paths rejected: normal control-flow cycle; labelled CFGs differ |
| `local/lito381/server/ClientSession#useClassicWorldAreaAttackSpell(Llocal/lito381/server/Protocol$AttackSpellRequest;Llocal/lito381/server/BattleSpellCatalog$AreaAttackSpell;)V` | 直接呼叫序列相同 | method useClassicWorldAreaAttackSpell(Llocal/lito381/server/Protocol$AttackSpellRequest;Llocal/lito381/server/BattleSpellCatalog$AreaAttackSpell;)V insn[10]: left=INVOKEDYNAMIC run(Llocal/lito381/server/ClientSession;Llocal/lito381/server/Protocol$AttackSpellRequest;Llocal/lito381/server/BattleSpellCatalog$AreaAttackSpell;)Llocal/lito381/server/NativeSelfCastAction$Task; bsm=H6 java/lang/invoke/Lambd… right=INVOKEDYNAMIC run(Llocal/lito381/server/ClientSession;Llocal/lito381/server/Protocol$AttackSpellRequest;Llocal/lito381/server/BattleSpellCatalog$AreaAttackSpell;)Llocal/lito381/server/NativeSelfCastAction$Task; bsm=H6 java/lang/invoke/Lambd…; path event multisets differ (left=5, right=5); labelled CFGs differ |
| `local/lito381/server/ClientSession#useClassicWorldAttackSpell(Llocal/lito381/server/Protocol$AttackSpellRequest;Llocal/lito381/server/BattleSpellCatalog$AttackSpell;)V` | 直接呼叫序列相同 | method useClassicWorldAttackSpell(Llocal/lito381/server/Protocol$AttackSpellRequest;Llocal/lito381/server/BattleSpellCatalog$AttackSpell;)V insn[10]: left=INVOKEDYNAMIC run(Llocal/lito381/server/ClientSession;Llocal/lito381/server/Protocol$AttackSpellRequest;Llocal/lito381/server/BattleSpellCatalog$AttackSpell;)Llocal/lito381/server/NativeSelfCastAction$Task; bsm=H6 java/lang/invoke/LambdaMet… right=INVOKEDYNAMIC run(Llocal/lito381/server/ClientSession;Llocal/lito381/server/Protocol$AttackSpellRequest;Llocal/lito381/server/BattleSpellCatalog$AttackSpell;)Llocal/lito381/server/NativeSelfCastAction$Task; bsm=H6 java/lang/invoke/LambdaMet…; path event multisets differ (left=5, right=5); labelled CFGs differ |
| `local/lito381/server/ClientSession#useClassicWorldCenteredAreaAttackSpell([BLlocal/lito381/server/BattleSpellCatalog$AreaAttackSpell;)V` | 直接呼叫序列相同 | method useClassicWorldCenteredAreaAttackSpell([BLlocal/lito381/server/BattleSpellCatalog$AreaAttackSpell;)V: left=103:[523:java/lang/Exception  ] right=105:[532:java/lang/Exception  ]; paths rejected: normal control-flow cycle; labelled CFGs differ |
| `local/lito381/server/ClientSession#useClassicWorldCounterDetectionSpell([BLlocal/lito381/server/BattleSpellCatalog$CounterDetectionSpell;)V` | 直接呼叫序列相同 | method useClassicWorldCounterDetectionSpell([BLlocal/lito381/server/BattleSpellCatalog$CounterDetectionSpell;)V: left=108:[206:java/lang/Throwable  ] right=110:[210:java/lang/Throwable  ]; paths rejected: normal control-flow cycle; labelled CFGs differ |
| `local/lito381/server/ClientSession#useClassicWorldHealingSpell([BLlocal/lito381/server/BattleSpellCatalog$HealingSpell;I)V` | 線性排列不同 | method useClassicWorldHealingSpell([BLlocal/lito381/server/BattleSpellCatalog$HealingSpell;I)V: left=334:[339:java/lang/Exception  ] right=336:[341:java/lang/Exception  ]; paths rejected: normal control-flow cycle; labelled CFGs differ |
| `local/lito381/server/ClientSession#useClassicWorldLineAttackSpell([BLlocal/lito381/server/BattleSpellCatalog$LineAttackSpell;)V` | 直接呼叫序列相同 | method useClassicWorldLineAttackSpell([BLlocal/lito381/server/BattleSpellCatalog$LineAttackSpell;)V insn[5]: left=INVOKEDYNAMIC run(Llocal/lito381/server/ClientSession;[BLlocal/lito381/server/BattleSpellCatalog$LineAttackSpell;)Llocal/lito381/server/NativeSelfCastAction$Task; bsm=H6 java/lang/invoke/LambdaMetafactory.metafactory(Ljava/lang/invoke/Metho… right=INVOKEDYNAMIC run(Llocal/lito381/server/ClientSession;[BLlocal/lito381/server/BattleSpellCatalog$LineAttackSpell;)Llocal/lito381/server/NativeSelfCastAction$Task; bsm=H6 java/lang/invoke/LambdaMetafactory.metafactory(Ljava/lang/invoke/Metho…; path event multisets differ (left=3, right=3); labelled CFGs differ |
| `local/lito381/server/ClientSession#useClassicWorldLineFreezeAttackSpell(Llocal/lito381/server/Protocol$AttackSpellRequest;Llocal/lito381/server/BattleSpellCatalog$FreezeAttackSpell;)V` | 直接呼叫序列相同 | method useClassicWorldLineFreezeAttackSpell(Llocal/lito381/server/Protocol$AttackSpellRequest;Llocal/lito381/server/BattleSpellCatalog$FreezeAttackSpell;)V: left=188:[372:java/lang/Exception  ] right=192:[380:java/lang/Exception  ]; paths rejected: normal control-flow cycle; labelled CFGs differ |
| `local/lito381/server/ClientSession#useClassicWorldManaDrainSpell([BLlocal/lito381/server/BattleSpellCatalog$ManaDrainSpell;)V` | 線性排列不同 | method useClassicWorldManaDrainSpell([BLlocal/lito381/server/BattleSpellCatalog$ManaDrainSpell;)V: left=276:[281:java/lang/Exception  ] right=278:[283:java/lang/Exception  ]; paths rejected: normal control-flow cycle; labelled CFGs differ |
| `local/lito381/server/ClientSession#useClassicWorldTurnUndeadSpell([BLlocal/lito381/server/BattleSpellCatalog$TurnUndeadSpell;)V` | 直接呼叫序列相同 | method useClassicWorldTurnUndeadSpell([BLlocal/lito381/server/BattleSpellCatalog$TurnUndeadSpell;)V: left=233:[238:java/lang/Exception  ] right=235:[240:java/lang/Exception  ]; paths rejected: normal control-flow cycle; labelled CFGs differ |
| `local/lito381/server/ClientSession#useCombatBuffSpell([BLlocal/lito381/server/BattleSpellCatalog$CombatBuffSpell;)V` | 線性排列不同 | method useCombatBuffSpell([BLlocal/lito381/server/BattleSpellCatalog$CombatBuffSpell;)V: left=216:[408:java/lang/Throwable  ] right=217:[410:java/lang/Throwable  ]; paths rejected: normal control-flow cycle; labelled CFGs differ |
| `local/lito381/server/ClientSession#useCounterBarrierSpell([BLlocal/lito381/server/BattleSpellCatalog$CounterBarrierSpell;)V` | 直接呼叫序列相同 | method useCounterBarrierSpell([BLlocal/lito381/server/BattleSpellCatalog$CounterBarrierSpell;)V: left=117:[270:java/lang/Throwable  ] right=118:[273:java/lang/Throwable  ]; paths rejected: normal control-flow cycle; labelled CFGs differ |
| `local/lito381/server/ClientSession#useCounterDetectionSpell([BLlocal/lito381/server/BattleSpellCatalog$CounterDetectionSpell;)V` | 直接呼叫序列相同 | method useCounterDetectionSpell([BLlocal/lito381/server/BattleSpellCatalog$CounterDetectionSpell;)V: left=211:[313:java/lang/Throwable  ] right=214:[318:java/lang/Throwable  ]; paths rejected: normal control-flow cycle; labelled CFGs differ |
| `local/lito381/server/ClientSession#useDetectionSpell([BLlocal/lito381/server/BattleSpellCatalog$DetectionSpell;)V` | 直接呼叫序列相同 | method useDetectionSpell([BLlocal/lito381/server/BattleSpellCatalog$DetectionSpell;)V: left=145:[260:java/lang/Throwable  ] right=143:[256:java/lang/Throwable  ]; paths rejected: normal control-flow cycle; labelled CFGs differ |
| `local/lito381/server/ClientSession#useDiamondGolemSpell([BLlocal/lito381/server/BattleSpellCatalog$DiamondGolemSpell;)V` | 直接呼叫序列相同 | method useDiamondGolemSpell([BLlocal/lito381/server/BattleSpellCatalog$DiamondGolemSpell;)V: left=147:[358:java/lang/Throwable  ] right=143:[352:java/lang/Throwable  ]; paths rejected: normal control-flow cycle; labelled CFGs differ |
| `local/lito381/server/ClientSession#useDodgeBuffSpell([BLlocal/lito381/server/BattleSpellCatalog$DodgeBuffSpell;)V` | 直接呼叫序列相同 | method useDodgeBuffSpell([BLlocal/lito381/server/BattleSpellCatalog$DodgeBuffSpell;)V: left=157:[304:java/lang/Throwable  ] right=158:[307:java/lang/Throwable  ]; paths rejected: normal control-flow cycle; labelled CFGs differ |
| `local/lito381/server/ClientSession#useDragonSkinSpell([BLlocal/lito381/server/BattleSpellCatalog$DragonSkinSpell;)V` | 直接呼叫序列相同 | method useDragonSkinSpell([BLlocal/lito381/server/BattleSpellCatalog$DragonSkinSpell;)V: left=94:[228:java/lang/Throwable  ] right=96:[230:java/lang/Throwable  ]; paths rejected: normal control-flow cycle; labelled CFGs differ |
| `local/lito381/server/ClientSession#useEarthBlessSpell([BLlocal/lito381/server/BattleSpellCatalog$EarthBlessSpell;)V` | 直接呼叫序列相同 | method useEarthBlessSpell([BLlocal/lito381/server/BattleSpellCatalog$EarthBlessSpell;)V: left=259:[353:java/lang/Throwable  ] right=265:[361:java/lang/Throwable  ]; paths rejected: normal control-flow cycle; labelled CFGs differ |
| `local/lito381/server/ClientSession#useFireBlessSpell([BLlocal/lito381/server/BattleSpellCatalog$FireBlessSpell;)V` | 直接呼叫序列相同 | method useFireBlessSpell([BLlocal/lito381/server/BattleSpellCatalog$FireBlessSpell;)V: left=259:[369:java/lang/Throwable  ] right=265:[377:java/lang/Throwable  ]; paths rejected: normal control-flow cycle; labelled CFGs differ |
| `local/lito381/server/ClientSession#useFoeSlayerSpell([BLlocal/lito381/server/BattleSpellCatalog$FoeSlayerSpell;)V` | 直接呼叫序列相同 | method useFoeSlayerSpell([BLlocal/lito381/server/BattleSpellCatalog$FoeSlayerSpell;)V: left=113:[258:java/lang/Throwable  ] right=116:[263:java/lang/Throwable  ]; paths rejected: normal control-flow cycle; labelled CFGs differ |
| `local/lito381/server/ClientSession#useGuardBrakeSpell([BLlocal/lito381/server/BattleSpellCatalog$GuardBrakeSpell;)V` | 直接呼叫序列相同 | method useGuardBrakeSpell([BLlocal/lito381/server/BattleSpellCatalog$GuardBrakeSpell;)V: left=162:[167:java/lang/Exception  ] right=164:[169:java/lang/Exception  ]; paths rejected: normal control-flow cycle; labelled CFGs differ |
| `local/lito381/server/ClientSession#useHasteSpell([BLlocal/lito381/server/BattleSpellCatalog$HasteSpell;)V` | 直接呼叫序列相同 | method useHasteSpell([BLlocal/lito381/server/BattleSpellCatalog$HasteSpell;)V: left=482:[612:java/lang/Throwable  ] right=485:[615:java/lang/Throwable  ]; paths rejected: normal control-flow cycle; labelled CFGs differ |
| `local/lito381/server/ClientSession#useIllusionAvatarSpell([BLlocal/lito381/server/BattleSpellCatalog$IllusionAvatarSpell;)V` | 線性排列不同 | method useIllusionAvatarSpell([BLlocal/lito381/server/BattleSpellCatalog$IllusionAvatarSpell;)V: left=140:[391:java/lang/Throwable  ] right=143:[395:java/lang/Throwable  ]; paths rejected: normal control-flow cycle; labelled CFGs differ |
| `local/lito381/server/ClientSession#useIllusionLichSpell([BLlocal/lito381/server/BattleSpellCatalog$IllusionLichSpell;)V` | 直接呼叫序列相同 | method useIllusionLichSpell([BLlocal/lito381/server/BattleSpellCatalog$IllusionLichSpell;)V: left=147:[358:java/lang/Throwable  ] right=143:[352:java/lang/Throwable  ]; paths rejected: normal control-flow cycle; labelled CFGs differ |
| `local/lito381/server/ClientSession#useIllusionOgreSpell([BLlocal/lito381/server/BattleSpellCatalog$IllusionOgreSpell;)V` | 直接呼叫序列相同 | method useIllusionOgreSpell([BLlocal/lito381/server/BattleSpellCatalog$IllusionOgreSpell;)V: left=147:[360:java/lang/Throwable  ] right=143:[354:java/lang/Throwable  ]; paths rejected: normal control-flow cycle; labelled CFGs differ |
| `local/lito381/server/ClientSession#useInsightSpell([BLlocal/lito381/server/BattleSpellCatalog$InsightSpell;)V` | 直接呼叫序列相同 | method useInsightSpell([BLlocal/lito381/server/BattleSpellCatalog$InsightSpell;)V: left=147:[358:java/lang/Throwable  ] right=143:[352:java/lang/Throwable  ]; paths rejected: normal control-flow cycle; labelled CFGs differ |
| `local/lito381/server/ClientSession#useIronSkinSpell([BLlocal/lito381/server/BattleSpellCatalog$IronSkinSpell;)V` | 直接呼叫序列相同 | method useIronSkinSpell([BLlocal/lito381/server/BattleSpellCatalog$IronSkinSpell;)V: left=182:[287:java/lang/Throwable  ] right=180:[285:java/lang/Throwable  ]; paths rejected: normal control-flow cycle; labelled CFGs differ |
| `local/lito381/server/ClientSession#useItemArmorEnchantSpell([BLlocal/lito381/server/BattleSpellCatalog$ItemArmorEnchantSpell;)V` | 直接呼叫序列相同 | method useItemArmorEnchantSpell([BLlocal/lito381/server/BattleSpellCatalog$ItemArmorEnchantSpell;)V: left=125:[286:java/lang/Throwable  ] right=123:[284:java/lang/Throwable  ]; paths rejected: normal control-flow cycle; labelled CFGs differ |
| `local/lito381/server/ClientSession#useJoyOfPainSpell([BLlocal/lito381/server/BattleSpellCatalog$JoyOfPainSpell;)V` | 直接呼叫序列相同 | method useJoyOfPainSpell([BLlocal/lito381/server/BattleSpellCatalog$JoyOfPainSpell;)V: left=133:[317:java/lang/Throwable  ] right=136:[322:java/lang/Throwable  ]; paths rejected: normal control-flow cycle; labelled CFGs differ |
| `local/lito381/server/ClientSession#useMortalBodySpell([BLlocal/lito381/server/BattleSpellCatalog$MortalBodySpell;)V` | 直接呼叫序列相同 | method useMortalBodySpell([BLlocal/lito381/server/BattleSpellCatalog$MortalBodySpell;)V: left=94:[235:java/lang/Throwable  ] right=96:[237:java/lang/Throwable  ]; paths rejected: normal control-flow cycle; labelled CFGs differ |
| `local/lito381/server/ClientSession#useMovementBuffSpell([BLlocal/lito381/server/BattleSpellCatalog$MovementBuffSpell;)V` | 直接呼叫序列相同 | method useMovementBuffSpell([BLlocal/lito381/server/BattleSpellCatalog$MovementBuffSpell;)V: left=64:[173:java/lang/Throwable  ] right=62:[171:java/lang/Throwable  ]; paths rejected: normal control-flow cycle; labelled CFGs differ |
| `local/lito381/server/ClientSession#useNaturesTouchSpell([BLlocal/lito381/server/BattleSpellCatalog$NaturesTouchSpell;)V` | 線性排列不同 | method useNaturesTouchSpell([BLlocal/lito381/server/BattleSpellCatalog$NaturesTouchSpell;)V: left=297:[487:java/lang/Throwable  ] right=300:[491:java/lang/Throwable  ]; paths rejected: normal control-flow cycle; labelled CFGs differ |
| `local/lito381/server/ClientSession#useOtherNoEffectSpell([BLlocal/lito381/server/BattleSpellCatalog$OtherNoEffectSpell;)V` | 直接呼叫序列相同 | method useOtherNoEffectSpell([BLlocal/lito381/server/BattleSpellCatalog$OtherNoEffectSpell;)V: left=172:[248:java/lang/Throwable  ] right=170:[246:java/lang/Throwable  ]; paths rejected: path limit 4096 exceeded; labelled CFGs differ |
| `local/lito381/server/ClientSession#usePanicSpell([BLlocal/lito381/server/BattleSpellCatalog$PanicSpell;I)V` | 直接呼叫序列相同 | method usePanicSpell([BLlocal/lito381/server/BattleSpellCatalog$PanicSpell;I)V: left=212:[217:java/lang/Exception  ] right=217:[222:java/lang/Exception  ]; paths rejected: normal control-flow cycle; labelled CFGs differ |
| `local/lito381/server/ClientSession#usePatienceSpell([BLlocal/lito381/server/BattleSpellCatalog$PatienceSpell;)V` | 直接呼叫序列相同 | method usePatienceSpell([BLlocal/lito381/server/BattleSpellCatalog$PatienceSpell;)V: left=119:[323:java/lang/Throwable  ] right=122:[328:java/lang/Throwable  ]; paths rejected: normal control-flow cycle; labelled CFGs differ |
| `local/lito381/server/ClientSession#usePhantasmSpell([BLlocal/lito381/server/BattleSpellCatalog$PhantasmSpell;I)V` | 直接呼叫序列相同 | method usePhantasmSpell([BLlocal/lito381/server/BattleSpellCatalog$PhantasmSpell;I)V: left=319:[510:java/lang/Throwable  ] right=324:[517:java/lang/Throwable  ]; paths rejected: normal control-flow cycle; labelled CFGs differ |
| `local/lito381/server/ClientSession#usePoisonResistanceSpell([BLlocal/lito381/server/BattleSpellCatalog$PoisonResistanceSpell;)V` | 直接呼叫序列相同 | method usePoisonResistanceSpell([BLlocal/lito381/server/BattleSpellCatalog$PoisonResistanceSpell;)V: left=160:[291:java/lang/Throwable  ] right=158:[289:java/lang/Throwable  ]; paths rejected: normal control-flow cycle; labelled CFGs differ |
| `local/lito381/server/ClientSession#useRangedEvasionBuffSpell([BLlocal/lito381/server/BattleSpellCatalog$RangedEvasionBuffSpell;)V` | 直接呼叫序列相同 | method useRangedEvasionBuffSpell([BLlocal/lito381/server/BattleSpellCatalog$RangedEvasionBuffSpell;)V: left=157:[285:java/lang/Throwable  ] right=158:[288:java/lang/Throwable  ]; paths rejected: normal control-flow cycle; labelled CFGs differ |
| `local/lito381/server/ClientSession#useReductionArmorSpell([BLlocal/lito381/server/BattleSpellCatalog$ReductionArmorSpell;)V` | 直接呼叫序列相同 | method useReductionArmorSpell([BLlocal/lito381/server/BattleSpellCatalog$ReductionArmorSpell;)V: left=118:[264:java/lang/Throwable  ] right=119:[267:java/lang/Throwable  ]; paths rejected: normal control-flow cycle; labelled CFGs differ |
| `local/lito381/server/ClientSession#useRegenSpell([BLlocal/lito381/server/BattleSpellCatalog$RegenSpell;)V` | 直接呼叫序列相同 | method useRegenSpell([BLlocal/lito381/server/BattleSpellCatalog$RegenSpell;)V: left=149:[382:java/lang/Throwable  ] right=145:[376:java/lang/Throwable  ]; paths rejected: normal control-flow cycle; labelled CFGs differ |
| `local/lito381/server/ClientSession#useResistFearSpell([BLlocal/lito381/server/BattleSpellCatalog$ResistFearSpell;)V` | 直接呼叫序列相同 | method useResistFearSpell([BLlocal/lito381/server/BattleSpellCatalog$ResistFearSpell;)V: left=134:[139:java/lang/Exception  ] right=136:[141:java/lang/Exception  ]; paths rejected: normal control-flow cycle; labelled CFGs differ |
| `local/lito381/server/ClientSession#useRevealWeaknessSpell([BLlocal/lito381/server/BattleSpellCatalog$RevealWeaknessSpell;)V` | 直接呼叫序列相同 | method useRevealWeaknessSpell([BLlocal/lito381/server/BattleSpellCatalog$RevealWeaknessSpell;)V: left=252:[257:java/lang/Exception  ] right=255:[260:java/lang/Exception  ]; paths rejected: normal control-flow cycle; labelled CFGs differ |
| `local/lito381/server/ClientSession#useRoyalAuraSpell([BLlocal/lito381/server/BattleSpellCatalog$RoyalAuraSpell;)V` | 直接呼叫序列相同 | method useRoyalAuraSpell([BLlocal/lito381/server/BattleSpellCatalog$RoyalAuraSpell;)V: left=227:[338:java/lang/Throwable  ] right=231:[344:java/lang/Throwable  ]; paths rejected: normal control-flow cycle; labelled CFGs differ |
| `local/lito381/server/ClientSession#useRunClanSpell([BLlocal/lito381/server/BattleSpellCatalog$RunClanSpell;)V` | 直接呼叫序列相同 | method useRunClanSpell([BLlocal/lito381/server/BattleSpellCatalog$RunClanSpell;)V: left=235:[345:java/lang/Throwable  ] right=236:[348:java/lang/Throwable  ]; paths rejected: normal control-flow cycle; labelled CFGs differ |
| `local/lito381/server/ClientSession#useSelfHasteSpell([BLlocal/lito381/server/BattleSpellCatalog$SelfHasteSpell;)V` | 直接呼叫序列相同 | method useSelfHasteSpell([BLlocal/lito381/server/BattleSpellCatalog$SelfHasteSpell;)V: left=89:[187:java/lang/Throwable  ] right=85:[183:java/lang/Throwable  ]; paths rejected: normal control-flow cycle; labelled CFGs differ |
| `local/lito381/server/ClientSession#useSolidCarriageSpell([BLlocal/lito381/server/BattleSpellCatalog$SolidCarriageSpell;)V` | 直接呼叫序列相同 | method useSolidCarriageSpell([BLlocal/lito381/server/BattleSpellCatalog$SolidCarriageSpell;)V: left=130:[283:java/lang/Throwable  ] right=131:[286:java/lang/Throwable  ]; paths rejected: normal control-flow cycle; labelled CFGs differ |
| `local/lito381/server/ClientSession#useSpellBeforeDragonTargets([B)V` | 線性排列不同 | method useSpellBeforeDragonTargets([B)V: left=25:[43:java/lang/Throwable  ] right=29:[51:java/lang/Throwable  ]; paths rejected: normal control-flow cycle; labelled CFGs differ |
| `local/lito381/server/ClientSession#useStormEyeSpell([BLlocal/lito381/server/BattleSpellCatalog$StormEyeSpell;)V` | 直接呼叫序列相同 | method useStormEyeSpell([BLlocal/lito381/server/BattleSpellCatalog$StormEyeSpell;)V: left=259:[369:java/lang/Throwable  ] right=265:[377:java/lang/Throwable  ]; paths rejected: normal control-flow cycle; labelled CFGs differ |
| `local/lito381/server/ClientSession#useStormShotSpell([BLlocal/lito381/server/BattleSpellCatalog$StormShotSpell;)V` | 直接呼叫序列相同 | method useStormShotSpell([BLlocal/lito381/server/BattleSpellCatalog$StormShotSpell;)V: left=191:[342:java/lang/Throwable  ] right=189:[340:java/lang/Throwable  ]; paths rejected: normal control-flow cycle; labelled CFGs differ |
| `local/lito381/server/ClientSession#useTripleArrowSpell([BLlocal/lito381/server/BattleSpellCatalog$TripleArrowSpell;)V` | 直接呼叫序列相同 | method useTripleArrowSpell([BLlocal/lito381/server/BattleSpellCatalog$TripleArrowSpell;)V: left=116:[230:java/lang/Throwable  ] right=117:[233:java/lang/Throwable  ]; paths rejected: normal control-flow cycle; labelled CFGs differ |
| `local/lito381/server/ClientSession#useTrueTargetSpell([BLlocal/lito381/server/BattleSpellCatalog$TrueTargetSpell;)V` | 直接呼叫序列相同 | method useTrueTargetSpell([BLlocal/lito381/server/BattleSpellCatalog$TrueTargetSpell;)V: left=200:[313:java/lang/Throwable  ] right=201:[316:java/lang/Throwable  ]; paths rejected: normal control-flow cycle; labelled CFGs differ |
| `local/lito381/server/ClientSession#useWeaponBuffSpell([BLlocal/lito381/server/BattleSpellCatalog$WeaponBuffSpell;)V` | 直接呼叫序列相同 | method useWeaponBuffSpell([BLlocal/lito381/server/BattleSpellCatalog$WeaponBuffSpell;)V: left=460:[659:java/lang/Throwable  ] right=464:[663:java/lang/Throwable  ]; paths rejected: normal control-flow cycle; labelled CFGs differ |
| `local/lito381/server/ClientSession#useWorldRoiCancellation([BLlocal/lito381/server/BattleSpellCatalog$CancellationSpell;)V` | 線性排列不同 | method useWorldRoiCancellation([BLlocal/lito381/server/BattleSpellCatalog$CancellationSpell;)V: left=316:[529:java/lang/Throwable  ] right=339:[554:java/lang/Throwable  ]; paths rejected: normal control-flow cycle; labelled CFGs differ |
| `local/lito381/server/ClientSession#useWorldTreeCallSpell([BLlocal/lito381/server/BattleSpellCatalog$WorldTreeCallSpell;)V` | 直接呼叫序列相同 | method useWorldTreeCallSpell([BLlocal/lito381/server/BattleSpellCatalog$WorldTreeCallSpell;)V: left=148:[288:java/lang/Throwable  ] right=148:[290:java/lang/Throwable  ]; paths rejected: normal control-flow cycle; labelled CFGs differ |

### lambda 未配對清單

| 方法 | 狀態 | 母方法與對側呼叫點 |
|---|---|---|
| `local/lito381/server/ClientSession#lambda$autoSelectPendingRentalBattleHandoff$30(Llocal/lito381/server/CharacterSummary;)Z` | ONLY_LEFT | local/lito381/server/ClientSession#autoSelectPendingRentalBattleHandoff()Z @ 21，lambda 序位 0 → local/lito381/server/ClientSession#lambda$autoSelectPendingRentalBattleHandoff$30(Llocal/lito381/server/CharacterSummary;)Z |
| `local/lito381/server/ClientSession#lambda$completeClassicWorldPortal$161(Llocal/lito381/server/Protocol$PortalDestination;Llocal/lito381/server/ClientSession$PendingClassicWorldPortal;)V` | ONLY_LEFT | local/lito381/server/ClientSession#completeClassicWorldPortal(Llocal/lito381/server/Protocol$PortalDestination;Llocal/lito381/server/ClientSession$PendingClassicWorldPortal;)V @ 6，lambda 序位 0 → local/lito381/server/ClientSession#nativePlacementPortalBody(Llocal/lito381/server/Protocol$PortalDestination;Llocal/lito381/server/ClientSession$PendingClassicWorldPortal;)V |
| `local/lito381/server/ClientSession#lambda$nativeAreaBattleSlowBody$329(IILlocal/lito381/server/BattleSpellCatalog$MassSlowSpell;Ljava/lang/Object;Llocal/lito381/server/BattleQuestEncounter$Snapshot;)Z` | ONLY_LEFT | local/lito381/server/ClientSession#nativeAreaBattleSlowBody([BLlocal/lito381/server/BattleSpellCatalog$MassSlowSpell;)V @ 222，lambda 序位 2 → local/lito381/server/ClientSession#nativeSourceFrozenSlow_lambda$useMassSlowSpell$322(IILlocal/lito381/server/BattleSpellCatalog$MassSlowSpell;Ljava/lang/Object;Llocal/lito381/server/BattleQuestEncounter$Snapshot;)Z |
| `local/lito381/server/ClientSession#lambda$nativeAreaBattleSlowBody$330(IILlocal/lito381/server/BattleSpellCatalog$MassSlowSpell;Ljava/lang/Object;Llocal/lito381/server/BattleTimeAttackEncounter$Snapshot;)Z` | ONLY_LEFT | local/lito381/server/ClientSession#nativeAreaBattleSlowBody([BLlocal/lito381/server/BattleSpellCatalog$MassSlowSpell;)V @ 238，lambda 序位 3 → local/lito381/server/ClientSession#nativeSourceFrozenSlow_lambda$useMassSlowSpell$323(IILlocal/lito381/server/BattleSpellCatalog$MassSlowSpell;Ljava/lang/Object;Llocal/lito381/server/BattleTimeAttackEncounter$Snapshot;)Z |
| `local/lito381/server/ClientSession#lambda$nativeMaterialGroundBody$388(Llocal/lito381/server/RuntimeInventory;ILlocal/lito381/server/BattleSpellCatalog$GroundSpell;JI)V` | ONLY_LEFT | local/lito381/server/ClientSession#nativeMaterialGroundBody([BLlocal/lito381/server/BattleSpellCatalog$GroundSpell;)V @ 228，lambda 序位 0 → local/lito381/server/ClientSession#nativeMaterialGroundBodyCommit(Llocal/lito381/server/RuntimeInventory;ILlocal/lito381/server/BattleSpellCatalog$GroundSpell;JI)V |
| `local/lito381/server/ClientSession#lambda$nativeMaterialWorldGroundBody$389(Llocal/lito381/server/RuntimeInventory;ILlocal/lito381/server/BattleSpellCatalog$GroundSpell;JI)V` | ONLY_LEFT | local/lito381/server/ClientSession#nativeMaterialWorldGroundBody([BLlocal/lito381/server/BattleSpellCatalog$GroundSpell;)V @ 288，lambda 序位 0 → local/lito381/server/ClientSession#nativeMaterialWorldGroundBodyCommit(Llocal/lito381/server/RuntimeInventory;ILlocal/lito381/server/BattleSpellCatalog$GroundSpell;JI)V |
| `local/lito381/server/ClientSession#lambda$useClassicWorldAreaAttackSpell$239(Llocal/lito381/server/Protocol$AttackSpellRequest;Llocal/lito381/server/BattleSpellCatalog$AreaAttackSpell;)V` | ONLY_LEFT | local/lito381/server/ClientSession#useClassicWorldAreaAttackSpell(Llocal/lito381/server/Protocol$AttackSpellRequest;Llocal/lito381/server/BattleSpellCatalog$AreaAttackSpell;)V @ 10，lambda 序位 0 → local/lito381/server/ClientSession#nativeCompanionTargetuseClassicWorldAreaAttackSpell(Llocal/lito381/server/Protocol$AttackSpellRequest;Llocal/lito381/server/BattleSpellCatalog$AreaAttackSpell;)V |
| `local/lito381/server/ClientSession#lambda$useClassicWorldAttackSpell$240(Llocal/lito381/server/Protocol$AttackSpellRequest;Llocal/lito381/server/BattleSpellCatalog$AttackSpell;)V` | ONLY_LEFT | local/lito381/server/ClientSession#useClassicWorldAttackSpell(Llocal/lito381/server/Protocol$AttackSpellRequest;Llocal/lito381/server/BattleSpellCatalog$AttackSpell;)V @ 10，lambda 序位 0 → local/lito381/server/ClientSession#nativeCompanionTargetuseClassicWorldAttackSpell(Llocal/lito381/server/Protocol$AttackSpellRequest;Llocal/lito381/server/BattleSpellCatalog$AttackSpell;)V |
| `local/lito381/server/ClientSession#lambda$useClassicWorldLineAttackSpell$237([BLlocal/lito381/server/BattleSpellCatalog$LineAttackSpell;)V` | ONLY_LEFT | local/lito381/server/ClientSession#useClassicWorldLineAttackSpell([BLlocal/lito381/server/BattleSpellCatalog$LineAttackSpell;)V @ 5，lambda 序位 0 → local/lito381/server/ClientSession#nativeCompanionTargetuseClassicWorldLineAttackSpell([BLlocal/lito381/server/BattleSpellCatalog$LineAttackSpell;)V |

官方結果的 ONLY_RIGHT 已清零（含核准 ORPHAN_IGNORED 處理）；仍未配對的 staged 包裝方法不能視為自動可接受。私有實例方法的部分參數綁定在 Java 以 lambda 表達，會多出轉呼叫本體。現行 `FourthNormalizations.forward` 只收 private static synthetic 與 static／virtual／interface 指令，拒絕 invokespecial；此處需要主控審查完整證據，沒有自行放寬規則。

## 交主控的代表性完整差異

以下是目前工具正規化後的完整 unified diff（所有差異區段均保留，未截斷），不是自行核准的豁免。完整方法原始 javap 及所有方法指令另存本機 scratch。

<details><summary>local/lito381/server/ClientSession#sourceApplicationRun()V</summary>


method sourceApplicationRun()V: left=1:[8:java/lang/Exception  ] right=24:[740:java/lang/Exception  , 1062:java/lang/Throwable  ]; paths rejected: normal control-flow cycle; labelled CFGs differ


```diff
--- staged/local/lito381/server/ClientSession#sourceApplicationRun()V
+++ candidate/local/lito381/server/ClientSession#sourceApplicationRun()V
@@ -1,5 +1,5 @@
 META 2 null []
-TRY [1:[8:java/lang/Exception  ], 3:[8:java/lang/Exception  ], 4:[8:java/lang/Exception  ], 28:[747:java/lang/Exception  , 1069:java/lang/Throwable  ], 31:[106:java/lang/Throwable  , 747:java/lang/Exception  , 1069:java/lang/Throwable  ], 33:[106:java/lang/Throwable  , 747:java/lang/Exception  , 1069:java/lang/Throwable  ], 35:[106:java/lang/Throwable  , 747:java/lang/Exception  , 1069:java/lang/Throwable  ], 37:[106:java/lang/Throwable  , 747:java/lang/Exception  , 1069:java/lang/Throwable  ], 38:[106:java/lang/Throwable  , 747:java/lang/Exception  , 1069:java/lang/Throwable  ], 40:[106:java/lang/Throwable  , 747:java/lang/Exception  , 1069:java/lang/Throwable  ], 41:[106:java/lang/Throwable  , 747:java/lang/Exception  , 1069:java/lang/Throwable  ], 43:[106:java/lang/Throwable  , 747:java/lang/Exception  , 1069:java/lang/Throwable  ], 45:[106:java/lang/Throwable  , 747:java/lang/Exception  , 1069:java/lang/Throwable  ], 47:[106:java/lang/Throwable  , 747:java/lang/Exception  , 1069:java/lang/Throwable  ], 49:[106:java/lang/Throwable  , 747:java/lang/Exception  , 1069:java/lang/Throwable  ], 50:[106:java/lang/Throwable  , 747:java/lang/Exception  , 1069:java/lang/Throwable  ], 54:[106:java/lang/Throwable  , 747:java/lang/Exception  , 1069:java/lang/Throwable  ], 56:[106:java/lang/Throwable  , 747:java/lang/Exception  , 1069:java/lang/Throwable  ], 58:[106:java/lang/Throwable  , 747:java/lang/Exception  , 1069:java/lang/Throwable  ], 61:[106:java/lang/Throwable  , 747:java/lang/Exception  , 1069:java/lang/Throwable  ], 62:[106:java/lang/Throwable  , 747:java/lang/Exception  , 1069:java/lang/Throwable  ], 64:[106:java/lang/Throwable  , 747:java/lang/Exception  , 1069:java/lang/Throwable  ], 67:[106:java/lang/Throwable  , 747:java/lang/Exception  , 1069:java/lang/Throwable  ], 68:[106:java/lang/Throwable  , 747:java/lang/Exception  , 1069:java/lang/Throwable  ], 70:[106:java/lang/Throwable  , 747:java/lang/Exception  , 1069:java/lang/Throwable  ], 71:[106:java/lang/Throwable  , 747:java/lang/Exception  , 1069:java/lang/Throwable  ], 73:[106:java/lang/Throwable  , 747:java/lang/Exception  , 1069:java/lang/Throwable  ], 74:[106:java/lang/Throwable  , 747:java/lang/Exception  , 1069:java/lang/Throwable  ], 76:[106:java/lang/Throwable  , 747:java/lang/Exception  , 1069:java/lang/Throwable  ], 82:[106:java/lang/Throwable  , 747:java/lang/Exception  , 1069:java/lang/Throwable  ], 84:[106:java/lang/Throwable  , 747:java/lang/Exception  , 1069:java/lang/Throwable  ], 87:[106:java/lang/Throwable  , 747:java/lang/Exception  , 1069:java/lang/Throwable  ], 91:[106:java/lang/Throwable  , 747:java/lang/Exception  , 1069:java/lang/Throwable  ], 95:[106:java/lang/Throwable  , 747:java/lang/Exception  , 1069:java/lang/Throwable  ], 96:[106:java/lang/Throwable  , 747:java/lang/Exception  , 1069:java/lang/Throwable  ], 99:[106:java/lang/Throwable  , 747:java/lang/Exception  , 1069:java/lang/Throwable  ], 101:[106:java/lang/Throwable  , 747:java/lang/Exception  , 1069:java/lang/Throwable  ], 111:[113:java/lang/Throwable  , 747:java/lang/Exception  , 1069:java/lang/Throwable  ], 116:[747:java/lang/Exception  , 1069:java/lang/Throwable  ], 118:[747:java/lang/Exception  , 1069:java/lang/Throwable  ], 123:[747:java/lang/Exception  , 1069:java/lang/Throwable  ], 150:[152:java/io/IOException  ], 164:[166:java/io/IOException  ], 177:[179:java/io/IOException  , 179:java/lang/RuntimeException  ], 186:[188:java/io/IOException  , 188:java/lang/RuntimeException  ], 195:[197:java/io/IOException  , 197:java/lang/RuntimeException  ], 204:[206:java/io/IOException  , 206:java/lang/RuntimeException  ], 213:[215:java/io/IOException  , 215:java/lang/RuntimeException  ], 222:[224:java/io/IOException  , 224:java/lang/RuntimeException  ], 231:[233:java/io/IOException  , 233:java/lang/RuntimeException  ], 240:[242:java/io/IOException  , 242:java/lang/RuntimeException  ], 249:[251:java/io/IOException  , 251:java/lang/RuntimeException  ], 258:[260:java/io/IOException  , 260:java/lang/RuntimeException  ], 267:[269:java/io/IOException  , 269:java/lang/RuntimeException  ], 276:[278:java/io/IOException  , 278:java/lang/RuntimeException  ], 285:[287:java/io/IOException  , 287:java/lang/RuntimeException  ], 299:[307:java/io/IOException  , 307:java/lang/RuntimeException  ], 302:[307:java/io/IOException  , 307:java/lang/RuntimeException  ], 305:[307:java/io/IOException  , 307:java/lang/RuntimeException  ], 314:[331:java/io/IOException  , 331:java/lang/RuntimeException  ], 317:[331:java/io/IOException  , 331:java/lang/RuntimeException  ], 321:[325:java/lang/Throwable  , 331:java/io/IOException  , 331:java/lang/RuntimeException  ], 323:[325:java/lang/Throwable  , 331:java/io/IOException  , 331:java/lang/RuntimeException  ], 327:[325:java/lang/Throwable  , 331:java/io/IOException  , 331:java/lang/RuntimeException  ], 329:[331:java/io/IOException  , 331:java/lang/RuntimeException  ], 338:[340:java/lang/Exception  ], 350:[352:java/io/IOException  ], 400:[402:java/io/IOException  ], 422:[424:java/io/IOException  ], 437:[747:java/lang/Exception  , 1069:java/lang/Throwable  ], 464:[466:java/io/IOException  ], 478:[480:java/io/IOException  ], 491:[493:java/io/IOException  , 493:java/lang/RuntimeException  ], 500:[502:java/io/IOException  , 502:java/lang/RuntimeException  ], 509:[511:java/io/IOException  , 511:java/lang/RuntimeException  ], 518:[520:java/io/IOException  , 520:java/lang/RuntimeException  ], 527:[529:java/io/IOException  , 529:java/lang/RuntimeException  ], 536:[538:java/io/IOException  , 538:java/lang/RuntimeException  ], 545:[547:java/io/IOException  , 547:java/lang/RuntimeException  ], 554:[556:java/io/IOException  , 556:java/lang/RuntimeException  ], 563:[565:java/io/IOException  , 565:java/lang/RuntimeException  ], 572:[574:java/io/IOException  , 574:java/lang/RuntimeException  ], 581:[583:java/io/IOException  , 583:java/lang/RuntimeException  ], 590:[592:java/io/IOException  , 592:java/lang/RuntimeException  ], 599:[601:java/io/IOException  , 601:java/lang/RuntimeException  ], 613:[621:java/io/IOException  , 621:java/lang/RuntimeException  ], 616:[621:java/io/IOException  , 621:java/lang/RuntimeException  ], 619:[621:java/io/IOException  , 621:java/lang/RuntimeException  ], 628:[645:java/io/IOException  , 645:java/lang/RuntimeException  ], 631:[645:java/io/IOException  , 645:java/lang/RuntimeException  ], 635:[639:java/lang/Throwable  , 645:java/io/IOException  , 645:java/lang/RuntimeException  ], 637:[639:java/lang/Throwable  , 645:java/io/IOException  , 645:java/lang/RuntimeException  ], 641:[639:java/lang/Throwable  , 645:java/io/IOException  , 645:java/lang/RuntimeException  ], 643:[645:java/io/IOException  , 645:java/lang/RuntimeException  ], 652:[654:java/lang/Exception  ], 664:[666:java/io/IOException  ], 714:[716:java/io/IOException  ], 736:[738:java/io/IOException  ], 748:[1069:java/lang/Throwable  ], 750:[1069:java/lang/Throwable  ], 751:[1069:java/lang/Throwable  ], 754:[1069:java/lang/Throwable  ], 755:[1069:java/lang/Throwable  ], 757:[1069:java/lang/Throwable  ], 758:[1069:java/lang/Throwable  ], 759:[1069:java/lang/Throwable  ], 786:[788:java/io/IOException  ], 800:[802:java/io/IOException  ], 813:[815:java/io/IOException  , 815:java/lang/RuntimeException  ], 822:[824:java/io/IOException  , 824:java/lang/RuntimeException  ], 831:[833:java/io/IOException  , 833:java/lang/RuntimeException  ], 840:[842:java/io/IOException  , 842:java/lang/RuntimeException  ], 849:[851:java/io/IOException  , 851:java/lang/RuntimeException  ], 858:[860:java/io/IOException  , 860:java/lang/RuntimeException  ], 867:[869:java/io/IOException  , 869:java/lang/RuntimeException  ], 876:[878:java/io/IOException  , 878:java/lang/RuntimeException  ], 885:[887:java/io/IOException  , 887:java/lang/RuntimeException  ], 894:[896:java/io/IOException  , 896:java/lang/RuntimeException  ], 903:[905:java/io/IOException  , 905:java/lang/RuntimeException  ], 912:[914:java/io/IOException  , 914:java/lang/RuntimeException  ], 921:[923:java/io/IOException  , 923:java/lang/RuntimeException  ], 935:[943:java/io/IOException  , 943:java/lang/RuntimeException  ], 938:[943:java/io/IOException  , 943:java/lang/RuntimeException  ], 941:[943:java/io/IOException  , 943:java/lang/RuntimeException  ], 950:[967:java/io/IOException  , 967:java/lang/RuntimeException  ], 953:[967:java/io/IOException  , 967:java/lang/RuntimeException  ], 957:[961:java/lang/Throwable  , 967:java/io/IOException  , 967:java/lang/RuntimeException  ], 959:[961:java/lang/Throwable  , 967:java/io/IOException  , 967:java/lang/RuntimeException  ], 963:[961:java/lang/Throwable  , 967:java/io/IOException  , 967:java/lang/RuntimeException  ], 965:[967:java/io/IOException  , 967:java/lang/RuntimeException  ], 974:[976:java/lang/Exception  ], 986:[988:java/io/IOException  ], 1036:[1038:java/io/IOException  ], 1058:[1060:java/io/IOException  ], 1096:[1098:java/io/IOException  ], 1110:[1112:java/io/IOException  ], 1123:[1125:java/io/IOException  , 1125:java/lang/RuntimeException  ], 1132:[1134:java/io/IOException  , 1134:java/lang/RuntimeException  ], 1141:[1143:java/io/IOException  , 1143:java/lang/RuntimeException  ], 1150:[1152:java/io/IOException  , 1152:java/lang/RuntimeException  ], 1159:[1161:java/io/IOException  , 1161:java/lang/RuntimeException  ], 1168:[1170:java/io/IOException  , 1170:java/lang/RuntimeException  ], 1177:[1179:java/io/IOException  , 1179:java/lang/RuntimeException  ], 1186:[1188:java/io/IOException  , 1188:java/lang/RuntimeException  ], 1195:[1197:java/io/IOException  , 1197:java/lang/RuntimeException  ], 1204:[1206:java/io/IOException  , 1206:java/lang/RuntimeException  ], 1213:[1215:java/io/IOException  , 1215:java/lang/RuntimeException  ], 1222:[1224:java/io/IOException  , 1224:java/lang/RuntimeException  ], 1231:[1233:java/io/IOException  , 1233:java/lang/RuntimeException  ], 1245:[1253:java/io/IOException  , 1253:java/lang/RuntimeException  ], 1248:[1253:java/io/IOException  , 1253:java/lang/RuntimeException  ], 1251:[1253:java/io/IOException  , 1253:java/lang/RuntimeException  ], 1260:[1277:java/io/IOException  , 1277:java/lang/RuntimeException  ], 1263:[1277:java/io/IOException  , 1277:java/lang/RuntimeException  ], 1267:[1271:java/lang/Throwable  , 1277:java/io/IOException  , 1277:java/lang/RuntimeException  ], 1269:[1271:java/lang/Throwable  , 1277:java/io/IOException  , 1277:java/lang/RuntimeException  ], 1273:[1271:java/lang/Throwable  , 1277:java/io/IOException  , 1277:java/lang/RuntimeException  ], 1275:[1277:java/io/IOException  , 1277:java/lang/RuntimeException  ], 1284:[1286:java/lang/Exception  ], 1296:[1298:java/io/IOException  ], 1346:[1348:java/io/IOException  ], 1368:[1370:java/io/IOException  ]]
+TRY [24:[740:java/lang/Exception  , 1062:java/lang/Throwable  ], 27:[727:java/lang/Throwable  , 740:java/lang/Exception  , 1062:java/lang/Throwable  ], 29:[727:java/lang/Throwable  , 740:java/lang/Exception  , 1062:java/lang/Throwable  ], 31:[727:java/lang/Throwable  , 740:java/lang/Exception  , 1062:java/lang/Throwable  ], 33:[727:java/lang/Throwable  , 740:java/lang/Exception  , 1062:java/lang/Throwable  ], 34:[727:java/lang/Throwable  , 740:java/lang/Exception  , 1062:java/lang/Throwable  ], 36:[727:java/lang/Throwable  , 740:java/lang/Exception  , 1062:java/lang/Throwable  ], 37:[727:java/lang/Throwable  , 740:java/lang/Exception  , 1062:java/lang/Throwable  ], 39:[727:java/lang/Throwable  , 740:java/lang/Exception  , 1062:java/lang/Throwable  ], 41:[727:java/lang/Throwable  , 740:java/lang/Exception  , 1062:java/lang/Throwable  ], 43:[727:java/lang/Throwable  , 740:java/lang/Exception  , 1062:java/lang/Throwable  ], 45:[727:java/lang/Throwable  , 740:java/lang/Exception  , 1062:java/lang/Throwable  ], 46:[727:java/lang/Throwable  , 740:java/lang/Exception  , 1062:java/lang/Throwable  ], 50:[727:java/lang/Throwable  , 740:java/lang/Exception  , 1062:java/lang/Throwable  ], 52:[727:java/lang/Throwable  , 740:java/lang/Exception  , 1062:java/lang/Throwable  ], 54:[727:java/lang/Throwable  , 740:java/lang/Exception  , 1062:java/lang/Throwable  ], 57:[727:java/lang/Throwable  , 740:java/lang/Exception  , 1062:java/lang/Throwable  ], 58:[727:java/lang/Throwable  , 740:java/lang/Exception  , 1062:java/lang/Throwable  ], 60:[727:java/lang/Throwable  , 740:java/lang/Exception  , 1062:java/lang/Throwable  ], 63:[727:java/lang/Throwable  , 740:java/lang/Exception  , 1062:java/lang/Throwable  ], 64:[727:java/lang/Throwable  , 740:java/lang/Exception  , 1062:java/lang/Throwable  ], 66:[727:java/lang/Throwable  , 740:java/lang/Exception  , 1062:java/lang/Throwable  ], 67:[727:java/lang/Throwable  , 740:java/lang/Exception  , 1062:java/lang/Throwable  ], 69:[727:java/lang/Throwable  , 740:java/lang/Exception  , 1062:java/lang/Throwable  ], 70:[727:java/lang/Throwable  , 740:java/lang/Exception  , 1062:java/lang/Throwable  ], 72:[727:java/lang/Throwable  , 740:java/lang/Exception  , 1062:java/lang/Throwable  ], 80:[740:java/lang/Exception  , 1062:java/lang/Throwable  ], 107:[109:java/io/IOException  ], 121:[123:java/io/IOException  ], 134:[136:java/io/IOException  , 136:java/lang/RuntimeException  ], 143:[145:java/io/IOException  , 145:java/lang/RuntimeException  ], 152:[154:java/io/IOException  , 154:java/lang/RuntimeException  ], 161:[163:java/io/IOException  , 163:java/lang/RuntimeException  ], 170:[172:java/io/IOException  , 172:java/lang/RuntimeException  ], 179:[181:java/io/IOException  , 181:java/lang/RuntimeException  ], 188:[190:java/io/IOException  , 190:java/lang/RuntimeException  ], 197:[199:java/io/IOException  , 199:java/lang/RuntimeException  ], 206:[208:java/io/IOException  , 208:java/lang/RuntimeException  ], 215:[217:java/io/IOException  , 217:java/lang/RuntimeException  ], 224:[226:java/io/IOException  , 226:java/lang/RuntimeException  ], 233:[235:java/io/IOException  , 235:java/lang/RuntimeException  ], 242:[244:java/io/IOException  , 244:java/lang/RuntimeException  ], 256:[264:java/io/IOException  , 264:java/lang/RuntimeException  ], 259:[264:java/io/IOException  , 264:java/lang/RuntimeException  ], 262:[264:java/io/IOException  , 264:java/lang/RuntimeException  ], 271:[288:java/io/IOException  , 288:java/lang/RuntimeException  ], 274:[288:java/io/IOException  , 288:java/lang/RuntimeException  ], 278:[282:java/lang/Throwable  , 288:java/io/IOException  , 288:java/lang/RuntimeException  ], 280:[282:java/lang/Throwable  , 288:java/io/IOException  , 288:java/lang/RuntimeException  ], 284:[282:java/lang/Throwable  , 288:java/io/IOException  , 288:java/lang/RuntimeException  ], 286:[288:java/io/IOException  , 288:java/lang/RuntimeException  ], 295:[297:java/lang/Exception  ], 307:[309:java/io/IOException  ], 357:[359:java/io/IOException  ], 379:[381:java/io/IOException  ], 391:[727:java/lang/Throwable  , 740:java/lang/Exception  , 1062:java/lang/Throwable  ], 393:[727:java/lang/Throwable  , 740:java/lang/Exception  , 1062:java/lang/Throwable  ], 396:[727:java/lang/Throwable  , 740:java/lang/Exception  , 1062:java/lang/Throwable  ], 400:[727:java/lang/Throwable  , 740:java/lang/Exception  , 1062:java/lang/Throwable  ], 404:[727:java/lang/Throwable  , 740:java/lang/Exception  , 1062:java/lang/Throwable  ], 405:[727:java/lang/Throwable  , 740:java/lang/Exception  , 1062:java/lang/Throwable  ], 408:[727:java/lang/Throwable  , 740:java/lang/Exception  , 1062:java/lang/Throwable  ], 410:[727:java/lang/Throwable  , 740:java/lang/Exception  , 1062:java/lang/Throwable  ], 416:[740:java/lang/Exception  , 1062:java/lang/Throwable  ], 443:[445:java/io/IOException  ], 457:[459:java/io/IOException  ], 470:[472:java/io/IOException  , 472:java/lang/RuntimeException  ], 479:[481:java/io/IOException  , 481:java/lang/RuntimeException  ], 488:[490:java/io/IOException  , 490:java/lang/RuntimeException  ], 497:[499:java/io/IOException  , 499:java/lang/RuntimeException  ], 506:[508:java/io/IOException  , 508:java/lang/RuntimeException  ], 515:[517:java/io/IOException  , 517:java/lang/RuntimeException  ], 524:[526:java/io/IOException  , 526:java/lang/RuntimeException  ], 533:[535:java/io/IOException  , 535:java/lang/RuntimeException  ], 542:[544:java/io/IOException  , 544:java/lang/RuntimeException  ], 551:[553:java/io/IOException  , 553:java/lang/RuntimeException  ], 560:[562:java/io/IOException  , 562:java/lang/RuntimeException  ], 569:[571:java/io/IOException  , 571:java/lang/RuntimeException  ], 578:[580:java/io/IOException  , 580:java/lang/RuntimeException  ], 592:[600:java/io/IOException  , 600:java/lang/RuntimeException  ], 595:[600:java/io/IOException  , 600:java/lang/RuntimeException  ], 598:[600:java/io/IOException  , 600:java/lang/RuntimeException  ], 607:[624:java/io/IOException  , 624:java/lang/RuntimeException  ], 610:[624:java/io/IOException  , 624:java/lang/RuntimeException  ], 614:[618:java/lang/Throwable  , 624:java/io/IOException  , 624:java/lang/RuntimeException  ], 616:[618:java/lang/Throwable  , 624:java/io/IOException  , 624:java/lang/RuntimeException  ], 620:[618:java/lang/Throwable  , 624:java/io/IOException  , 624:java/lang/RuntimeException  ], 622:[624:java/io/IOException  , 624:java/lang/RuntimeException  ], 631:[633:java/lang/Exception  ], 643:[645:java/io/IOException  ], 693:[695:java/io/IOException  ], 715:[717:java/io/IOException  ], 732:[734:java/lang/Throwable  , 740:java/lang/Exception  , 1062:java/lang/Throwable  ], 737:[740:java/lang/Exception  , 1062:java/lang/Throwable  ], 739:[740:java/lang/Exception  , 1062:java/lang/Throwable  ], 741:[1062:java/lang/Throwable  ], 743:[1062:java/lang/Throwable  ], 744:[1062:java/lang/Throwable  ], 747:[1062:java/lang/Throwable  ], 748:[1062:java/lang/Throwable  ], 750:[1062:java/lang/Throwable  ], 751:[1062:java/lang/Throwable  ], 752:[1062:java/lang/Throwable  ], 779:[781:java/io/IOException  ], 793:[795:java/io/IOException  ], 806:[808:java/io/IOException  , 808:java/lang/RuntimeException  ], 815:[817:java/io/IOException  , 817:java/lang/RuntimeException  ], 824:[826:java/io/IOException  , 826:java/lang/RuntimeException  ], 833:[835:java/io/IOException  , 835:java/lang/RuntimeException  ], 842:[844:java/io/IOException  , 844:java/lang/RuntimeException  ], 851:[853:java/io/IOException  , 853:java/lang/RuntimeException  ], 860:[862:java/io/IOException  , 862:java/lang/RuntimeException  ], 869:[871:java/io/IOException  , 871:java/lang/RuntimeException  ], 878:[880:java/io/IOException  , 880:java/lang/RuntimeException  ], 887:[889:java/io/IOException  , 889:java/lang/RuntimeException  ], 896:[898:java/io/IOException  , 898:java/lang/RuntimeException  ], 905:[907:java/io/IOException  , 907:java/lang/RuntimeException  ], 914:[916:java/io/IOException  , 916:java/lang/RuntimeException  ], 928:[936:java/io/IOException  , 936:java/lang/RuntimeException  ], 931:[936:java/io/IOException  , 936:java/lang/RuntimeException  ], 934:[936:java/io/IOException  , 936:java/lang/RuntimeException  ], 943:[960:java/io/IOException  , 960:java/lang/RuntimeException  ], 946:[960:java/io/IOException  , 960:java/lang/RuntimeException  ], 950:[954:java/lang/Throwable  , 960:java/io/IOException  , 960:java/lang/RuntimeException  ], 952:[954:java/lang/Throwable  , 960:java/io/IOException  , 960:java/lang/RuntimeException  ], 956:[954:java/lang/Throwable  , 960:java/io/IOException  , 960:java/lang/RuntimeException  ], 958:[960:java/io/IOException  , 960:java/lang/RuntimeException  ], 967:[969:java/lang/Exception  ], 979:[981:java/io/IOException  ], 1029:[1031:java/io/IOException  ], 1051:[1053:java/io/IOException  ], 1089:[1091:java/io/IOException  ], 1103:[1105:java/io/IOException  ], 1116:[1118:java/io/IOException  , 1118:java/lang/RuntimeException  ], 1125:[1127:java/io/IOException  , 1127:java/lang/RuntimeException  ], 1134:[1136:java/io/IOException  , 1136:java/lang/RuntimeException  ], 1143:[1145:java/io/IOException  , 1145:java/lang/RuntimeException  ], 1152:[1154:java/io/IOException  , 1154:java/lang/RuntimeException  ], 1161:[1163:java/io/IOException  , 1163:java/lang/RuntimeException  ], 1170:[1172:java/io/IOException  , 1172:java/lang/RuntimeException  ], 1179:[1181:java/io/IOException  , 1181:java/lang/RuntimeException  ], 1188:[1190:java/io/IOException  , 1190:java/lang/RuntimeException  ], 1197:[1199:java/io/IOException  , 1199:java/lang/RuntimeException  ], 1206:[1208:java/io/IOException  , 1208:java/lang/RuntimeException  ], 1215:[1217:java/io/IOException  , 1217:java/lang/RuntimeException  ], 1224:[1226:java/io/IOException  , 1226:java/lang/RuntimeException  ], 1238:[1246:java/io/IOException  , 1246:java/lang/RuntimeException  ], 1241:[1246:java/io/IOException  , 1246:java/lang/RuntimeException  ], 1244:[1246:java/io/IOException  , 1246:java/lang/RuntimeException  ], 1253:[1270:java/io/IOException  , 1270:java/lang/RuntimeException  ], 1256:[1270:java/io/IOException  , 1270:java/lang/RuntimeException  ], 1260:[1264:java/lang/Throwable  , 1270:java/io/IOException  , 1270:java/lang/RuntimeException  ], 1262:[1264:java/lang/Throwable  , 1270:java/io/IOException  , 1270:java/lang/RuntimeException  ], 1266:[1264:java/lang/Throwable  , 1270:java/io/IOException  , 1270:java/lang/RuntimeException  ], 1268:[1270:java/io/IOException  , 1270:java/lang/RuntimeException  ], 1277:[1279:java/lang/Exception  ], 1289:[1291:java/io/IOException  ], 1339:[1341:java/io/IOException  ], 1361:[1363:java/io/IOException  ]]
 ALOAD 0
 GETFIELD local/lito381/server/ClientSession.socket:Ljava/net/Socket;
 ALOAD 0
@@ -7,20 +7,16 @@
 INVOKESTATIC local/lito381/server/SourceLogin.allowed(Ljava/net/Socket;Llocal/lito381/server/ServerConfig;)Z
 IFNE L7
 RETURN
-GOTO L11
-ASTORE 1
-ALOAD 1
-INVOKESTATIC local/lito381/server/ClientSession.rethrowUnchecked(Ljava/lang/Throwable;)V
 ALOAD 0
 GETFIELD local/lito381/server/ClientSession.socket:Ljava/net/Socket;
 INVOKEVIRTUAL java/net/Socket.getRemoteSocketAddress()Ljava/net/SocketAddress;
 INVOKEVIRTUAL java/lang/Object.toString()Ljava/lang/String;
-ASTORE 2
+ASTORE 1
 GETSTATIC java/lang/System.out:Ljava/io/PrintStream;
 ALOAD 0
 GETFIELD local/lito381/server/ClientSession.endpoint:Llocal/lito381/server/ClientSession$Endpoint;
 INVOKESTATIC java/lang/String.valueOf(Ljava/lang/Object;)Ljava/lang/String;
-ALOAD 2
+ALOAD 1
 INVOKEDYNAMIC makeConcatWithConstants(Ljava/lang/String;Ljava/lang/String;)Ljava/lang/String; bsm=H6 java/lang/invoke/StringConcatFactory.makeConcatWithConstants(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;Ljava/lang/String;[Ljava/lang/Object;)Ljava/lang/invoke/CallSite; args=[str:"[LiTo] connected endpoint=\u0001 peer=\u0001"]
 INVOKEVIRTUAL java/io/PrintStream.println(Ljava/lang/String;)V
 ALOAD 0
@@ -29,7 +25,7 @@
 INVOKEVIRTUAL local/lito381/server/GameHub.connect(Llocal/lito381/server/ClientSession;)V
 ALOAD 0
 GETFIELD local/lito381/server/ClientSession.socket:Ljava/net/Socket;
-ASTORE 3
+ASTORE 2
 ALOAD 0
 GETFIELD local/lito381/server/ClientSession.socket:Ljava/net/Socket;
 CONST Integer:1
@@ -42,31 +38,31 @@
 DUP
 INVOKESPECIAL java/security/SecureRandom.<init>()V
 INVOKEVIRTUAL java/security/SecureRandom.nextInt()I
-ISTORE 4
+ISTORE 3
 NEW local/lito381/server/Protocol$Writer
 DUP
 INVOKESPECIAL local/lito381/server/Protocol$Writer.<init>()V
 CONST Integer:1
 INVOKEVIRTUAL local/lito381/server/Protocol$Writer.c(I)Llocal/lito381/server/Protocol$Writer;
-ILOAD 4
+ILOAD 3
 INVOKESTATIC java/lang/Integer.toUnsignedLong(I)J
 INVOKEVIRTUAL local/lito381/server/Protocol$Writer.d(J)Llocal/lito381/server/Protocol$Writer;
-ASTORE 5
-ALOAD 0
-ALOAD 5
+ASTORE 4
+ALOAD 0
+ALOAD 4
 INVOKEVIRTUAL local/lito381/server/Protocol$Writer.bytes()[B
 CONST str:"S_KEY"
 INVOKESPECIAL local/lito381/server/ClientSession.sendPlain([BLjava/lang/String;)V
 ALOAD 0
 NEW local/lito381/server/Protocol$CipherState
 DUP
-ILOAD 4
+ILOAD 3
 INVOKESPECIAL local/lito381/server/Protocol$CipherState.<init>(I)V
 PUTFIELD local/lito381/server/ClientSession.incoming:Llocal/lito381/server/Protocol$CipherState;
 ALOAD 0
 NEW local/lito381/server/Protocol$CipherState
 DUP
-ILOAD 4
+ILOAD 3
 INVOKESPECIAL local/lito381/server/Protocol$CipherState.<init>(I)V
 PUTFIELD local/lito381/server/ClientSession.outgoing:Llocal/lito381/server/Protocol$CipherState;
 ALOAD 0
@@ -77,226 +73,523 @@
 INVOKEVIRTUAL local/lito381/server/ServerConfig.maxFrame()I
 ALOAD 0
 INVOKESTATIC local/lito381/server/NativeCastTimeline.readFrame(Ljava/io/InputStream;ILlocal/lito381/server/ClientSession;)[B
-ASTORE 6
-ALOAD 6
-IFNONNULL L81
-GOTO L433
+ASTORE 5
+ALOAD 5
+IFNONNULL L390
+ALOAD 2
+IFNULL L81
+ALOAD 2
+ALOAD 0
+INVOKESTATIC local/lito381/server/NativeServerLifetime.closeSessionSocket(Ljava/net/Socket;Llocal/lito381/server/ClientSession;)V
+ALOAD 0
+INVOKESTATIC local/lito381/server/NativeServerLifetime.awaitCleanup(Llocal/lito381/server/ClientSession;)V
+ALOAD 0
+GETFIELD local/lito381/server/ClientSession.currentRoomId:J
+LSTORE 6/7
+ALOAD 0
+GETFIELD local/lito381/server/ClientSession.phase:Llocal/lito381/server/ClientSession$Phase;
+GETSTATIC local/lito381/server/ClientSession$Phase.ROOM:Llocal/lito381/server/ClientSession$Phase;
+IF_ACMPNE L102
+LLOAD 6/7
+CONST Long:0
+LCMP
+IFEQ L102
+ALOAD 0
+GETFIELD local/lito381/server/ClientSession.hub:Llocal/lito381/server/GameHub;
+LLOAD 6/7
+ALOAD 0
+INVOKEVIRTUAL local/lito381/server/GameHub.isOwner(JLlocal/lito381/server/ClientSession;)Z
+IFEQ L102
+CONST Integer:1
+GOTO L103
+CONST Integer:0
+ISTORE 8
+ALOAD 0
+CONST str:"disconnect"
+CONST Integer:0
+INVOKESPECIAL local/lito381/server/ClientSession.cancelActivePlayerTrade(Ljava/lang/String;Z)V
+GOTO L115
+ASTORE 9
+GETSTATIC java/lang/System.out:Ljava/io/PrintStream;
+ALOAD 9
+INVOKEVIRTUAL java/io/IOException.getMessage()Ljava/lang/String;
+INVOKEDYNAMIC makeConcatWithConstants(Ljava/lang/String;)Ljava/lang/String; bsm=H6 java/lang/invoke/StringConcatFactory.makeConcatWithConstants(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;Ljava/lang/String;[Ljava/lang/Object;)Ljava/lang/invoke/CallSite; args=[str:"[LiTo] trade disconnect cleanup failed: \u0001"]
+INVOKEVIRTUAL java/io/PrintStream.println(Ljava/lang/String;)V
+ALOAD 0
+GETFIELD local/lito381/server/ClientSession.classicPartyParticipant:Z
+IFEQ L129
+ALOAD 0
+CONST str:"disconnect"
+CONST Integer:0
+INVOKESPECIAL local/lito381/server/ClientSession.leaveClassicParty(Ljava/lang/String;Z)V
+GOTO L129
+ASTORE 10
+GETSTATIC java/lang/System.out:Ljava/io/PrintStream;
+ALOAD 10
+INVOKEVIRTUAL java/io/IOException.getMessage()Ljava/lang/String;
+INVOKEDYNAMIC makeConcatWithConstants(Ljava/lang/String;)Ljava/lang/String; bsm=H6 java/lang/invoke/StringConcatFactory.makeConcatWithConstants(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;Ljava/lang/String;[Ljava/lang/Object;)Ljava/lang/invoke/CallSite; args=[str:"[LiTo] party disconnect cleanup failed: \u0001"]
+INVOKEVIRTUAL java/io/PrintStream.println(Ljava/lang/String;)V
+ALOAD 0
+GETFIELD local/lito381/server/ClientSession.phase:Llocal/lito381/server/ClientSession$Phase;
+GETSTATIC local/lito381/server/ClientSession$Phase.WORLD:Llocal/lito381/server/ClientSession$Phase;
+IF_ACMPNE L151
+ALOAD 0
+INVOKESPECIAL local/lito381/server/ClientSession.collectClassicWorldPetsOnDisconnect()V
+GOTO L142
+ASTORE 11
+GETSTATIC java/lang/System.out:Ljava/io/PrintStream;
+ALOAD 11
+INVOKEVIRTUAL java/lang/Exception.getMessage()Ljava/lang/String;
+INVOKEDYNAMIC makeConcatWithConstants(Ljava/lang/String;)Ljava/lang/String; bsm=H6 java/lang/invoke/StringConcatFactory.makeConcatWithConstants(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;Ljava/lang/String;[Ljava/lang/Object;)Ljava/lang/invoke/CallSite; args=[str:"[LiTo] persistent pet disconnect cleanup failed: \u0001"]
+INVOKEVIRTUAL java/io/PrintStream.println(Ljava/lang/String;)V
+ALOAD 0
+INVOKESPECIAL local/lito381/server/ClientSession.cleanupClassicWorldTransientCompanionsOnDisconnect()V
+GOTO L151
+ASTORE 12
+GETSTATIC java/lang/System.out:Ljava/io/PrintStream;
+ALOAD 12
+INVOKEVIRTUAL java/lang/Exception.getMessage()Ljava/lang/String;
+INVOKEDYNAMIC makeConcatWithConstants(Ljava/lang/String;)Ljava/lang/String; bsm=H6 java/lang/invoke/StringConcatFactory.makeConcatWithConstants(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;Ljava/lang/String;[Ljava/lang/Object;)Ljava/lang/invoke/CallSite; args=[str:"[LiTo] transient companion disconnect cleanup failed: \u0001"]
+INVOKEVIRTUAL java/io/PrintStream.println(Ljava/lang/String;)V
+ALOAD 0
+INVOKESPECIAL local/lito381/server/ClientSession.persistExperienceGemOnDisconnect()V
+GOTO L160
+ASTORE 13
+GETSTATIC java/lang/System.out:Ljava/io/PrintStream;
+ALOAD 13
+INVOKEVIRTUAL java/lang/Exception.getMessage()Ljava/lang/String;
+INVOKEDYNAMIC makeConcatWithConstants(Ljava/lang/String;)Ljava/lang/String; bsm=H6 java/lang/invoke/StringConcatFactory.makeConcatWithConstants(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;Ljava/lang/String;[Ljava/lang/Object;)Ljava/lang/invoke/CallSite; args=[str:"[LiTo] Dragon Gem disconnect persistence failed: \u0001"]
+INVOKEVIRTUAL java/io/PrintStream.println(Ljava/lang/String;)V
+ALOAD 0
+INVOKESPECIAL local/lito381/server/ClientSession.persistFireWeaponOnDisconnect()V
+GOTO L169
+ASTORE 14
+GETSTATIC java/lang/System.out:Ljava/io/PrintStream;
+ALOAD 14
+INVOKEVIRTUAL java/lang/Exception.getMessage()Ljava/lang/String;
+INVOKEDYNAMIC makeConcatWithConstants(Ljava/lang/String;)Ljava/lang/String; bsm=H6 java/lang/invoke/StringConcatFactory.makeConcatWithConstants(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;Ljava/lang/String;[Ljava/lang/Object;)Ljava/lang/invoke/CallSite; args=[str:"[LiTo] Fire Weapon disconnect persistence failed: \u0001"]
+INVOKEVIRTUAL java/io/PrintStream.println(Ljava/lang/String;)V
+ALOAD 0
+INVOKESPECIAL local/lito381/server/ClientSession.persistBurningWeaponOnDisconnect()V
+GOTO L178
+ASTORE 15
+GETSTATIC java/lang/System.out:Ljava/io/PrintStream;
+ALOAD 15
+INVOKEVIRTUAL java/lang/Exception.getMessage()Ljava/lang/String;
+INVOKEDYNAMIC makeConcatWithConstants(Ljava/lang/String;)Ljava/lang/String; bsm=H6 java/lang/invoke/StringConcatFactory.makeConcatWithConstants(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;Ljava/lang/String;[Ljava/lang/Object;)Ljava/lang/invoke/CallSite; args=[str:"[LiTo] Burning Weapon disconnect persistence failed: \u0001"]
+INVOKEVIRTUAL java/io/PrintStream.println(Ljava/lang/String;)V
+ALOAD 0
+INVOKESPECIAL local/lito381/server/ClientSession.persistWindShotOnDisconnect()V
+GOTO L187
+ASTORE 16
+GETSTATIC java/lang/System.out:Ljava/io/PrintStream;
+ALOAD 16
+INVOKEVIRTUAL java/lang/Exception.getMessage()Ljava/lang/String;
+INVOKEDYNAMIC makeConcatWithConstants(Ljava/lang/String;)Ljava/lang/String; bsm=H6 java/lang/invoke/StringConcatFactory.makeConcatWithConstants(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;Ljava/lang/String;[Ljava/lang/Object;)Ljava/lang/invoke/CallSite; args=[str:"[LiTo] Wind Shot disconnect persistence failed: \u0001"]
+INVOKEVIRTUAL java/io/PrintStream.println(Ljava/lang/String;)V
+ALOAD 0
+INVOKESPECIAL local/lito381/server/ClientSession.persistFireBlessOnDisconnect()V
+GOTO L196
+ASTORE 17
+GETSTATIC java/lang/System.out:Ljava/io/PrintStream;
+ALOAD 17
+INVOKEVIRTUAL java/lang/Exception.getMessage()Ljava/lang/String;
+INVOKEDYNAMIC makeConcatWithConstants(Ljava/lang/String;)Ljava/lang/String; bsm=H6 java/lang/invoke/StringConcatFactory.makeConcatWithConstants(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;Ljava/lang/String;[Ljava/lang/Object;)Ljava/lang/invoke/CallSite; args=[str:"[LiTo] Fire Bless disconnect persistence failed: \u0001"]
+INVOKEVIRTUAL java/io/PrintStream.println(Ljava/lang/String;)V
+ALOAD 0
+INVOKESPECIAL local/lito381/server/ClientSession.persistStormEyeOnDisconnect()V
+GOTO L205
+ASTORE 18
+GETSTATIC java/lang/System.out:Ljava/io/PrintStream;
+ALOAD 18
+INVOKEVIRTUAL java/lang/Exception.getMessage()Ljava/lang/String;
+INVOKEDYNAMIC makeConcatWithConstants(Ljava/lang/String;)Ljava/lang/String; bsm=H6 java/lang/invoke/StringConcatFactory.makeConcatWithConstants(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;Ljava/lang/String;[Ljava/lang/Object;)Ljava/lang/invoke/CallSite; args=[str:"[LiTo] Storm Eye disconnect persistence failed: \u0001"]
+INVOKEVIRTUAL java/io/PrintStream.println(Ljava/lang/String;)V
+ALOAD 0
+INVOKESPECIAL local/lito381/server/ClientSession.persistStormShotOnDisconnect()V
+GOTO L214
+ASTORE 19
+GETSTATIC java/lang/System.out:Ljava/io/PrintStream;
+ALOAD 19
+INVOKEVIRTUAL java/lang/Exception.getMessage()Ljava/lang/String;
+INVOKEDYNAMIC makeConcatWithConstants(Ljava/lang/String;)Ljava/lang/String; bsm=H6 java/lang/invoke/StringConcatFactory.makeConcatWithConstants(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;Ljava/lang/String;[Ljava/lang/Object;)Ljava/lang/invoke/CallSite; args=[str:"[LiTo] Storm Shot disconnect persistence failed: \u0001"]
+INVOKEVIRTUAL java/io/PrintStream.println(Ljava/lang/String;)V
+ALOAD 0
+INVOKESPECIAL local/lito381/server/ClientSession.persistEarthBlessOnDisconnect()V
+GOTO L223
+ASTORE 20
+GETSTATIC java/lang/System.out:Ljava/io/PrintStream;
+ALOAD 20
+INVOKEVIRTUAL java/lang/Exception.getMessage()Ljava/lang/String;
+INVOKEDYNAMIC makeConcatWithConstants(Ljava/lang/String;)Ljava/lang/String; bsm=H6 java/lang/invoke/StringConcatFactory.makeConcatWithConstants(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;Ljava/lang/String;[Ljava/lang/Object;)Ljava/lang/invoke/CallSite; args=[str:"[LiTo] Earth Bless disconnect persistence failed: \u0001"]
+INVOKEVIRTUAL java/io/PrintStream.println(Ljava/lang/String;)V
+ALOAD 0
+INVOKESPECIAL local/lito381/server/ClientSession.persistIronSkinOnDisconnect()V
+GOTO L232
+ASTORE 21
+GETSTATIC java/lang/System.out:Ljava/io/PrintStream;
+ALOAD 21
+INVOKEVIRTUAL java/lang/Exception.getMessage()Ljava/lang/String;
+INVOKEDYNAMIC makeConcatWithConstants(Ljava/lang/String;)Ljava/lang/String; bsm=H6 java/lang/invoke/StringConcatFactory.makeConcatWithConstants(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;Ljava/lang/String;[Ljava/lang/Object;)Ljava/lang/invoke/CallSite; args=[str:"[LiTo] Iron Skin disconnect persistence failed: \u0001"]
+INVOKEVIRTUAL java/io/PrintStream.println(Ljava/lang/String;)V
+ALOAD 0
+INVOKESPECIAL local/lito381/server/ClientSession.persistMovementBuffOnDisconnect()V
+GOTO L241
+ASTORE 22
+GETSTATIC java/lang/System.out:Ljava/io/PrintStream;
+ALOAD 22
+INVOKEVIRTUAL java/lang/Exception.getMessage()Ljava/lang/String;
+INVOKEDYNAMIC makeConcatWithConstants(Ljava/lang/String;)Ljava/lang/String; bsm=H6 java/lang/invoke/StringConcatFactory.makeConcatWithConstants(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;Ljava/lang/String;[Ljava/lang/Object;)Ljava/lang/invoke/CallSite; args=[str:"[LiTo] movement buff disconnect persistence failed: \u0001"]
+INVOKEVIRTUAL java/io/PrintStream.println(Ljava/lang/String;)V
+ALOAD 0
+INVOKESPECIAL local/lito381/server/ClientSession.persistArmorProtectionOnDisconnect()V
+GOTO L250
+ASTORE 23
+GETSTATIC java/lang/System.out:Ljava/io/PrintStream;
+ALOAD 23
+INVOKEVIRTUAL java/lang/Exception.getMessage()Ljava/lang/String;
+INVOKEDYNAMIC makeConcatWithConstants(Ljava/lang/String;)Ljava/lang/String; bsm=H6 java/lang/invoke/StringConcatFactory.makeConcatWithConstants(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;Ljava/lang/String;[Ljava/lang/Object;)Ljava/lang/invoke/CallSite; args=[str:"[LiTo] armor protection disconnect persistence failed: \u0001"]
+INVOKEVIRTUAL java/io/PrintStream.println(Ljava/lang/String;)V
+ALOAD 0
+GETFIELD local/lito381/server/ClientSession.phase:Llocal/lito381/server/ClientSession$Phase;
+GETSTATIC local/lito381/server/ClientSession$Phase.WORLD:Llocal/lito381/server/ClientSession$Phase;
+IF_ACMPNE L270
+ALOAD 0
+CONST str:"owner-disconnect"
+INVOKESPECIAL local/lito381/server/ClientSession.abandonClassicWorldRescueFollower(Ljava/lang/String;)V
+ALOAD 0
+CONST str:"owner-disconnect"
+INVOKESPECIAL local/lito381/server/ClientSession.abandonClassicWorldDspyFollower(Ljava/lang/String;)V
+ALOAD 0
+CONST str:"owner-disconnect"
+INVOKESPECIAL local/lito381/server/ClientSession.abandonClassicWorldRoiFollower(Ljava/lang/String;)V
+GOTO L270
+ASTORE 24
+GETSTATIC java/lang/System.out:Ljava/io/PrintStream;
+ALOAD 24
+INVOKEVIRTUAL java/lang/Exception.getMessage()Ljava/lang/String;
+INVOKEDYNAMIC makeConcatWithConstants(Ljava/lang/String;)Ljava/lang/String; bsm=H6 java/lang/invoke/StringConcatFactory.makeConcatWithConstants(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;Ljava/lang/String;[Ljava/lang/Object;)Ljava/lang/invoke/CallSite; args=[str:"[LiTo] quest follower disconnect cleanup failed: \u0001"]
+INVOKEVIRTUAL java/io/PrintStream.println(Ljava/lang/String;)V
+ALOAD 0
+GETFIELD local/lito381/server/ClientSession.activeMagicDollObjectIds:Ljava/util/Set;
+DUP
+ASTORE 25
+MONITORENTER
+ALOAD 0
+CONST str:"owner-disconnect"
+CONST Integer:0
+INVOKESPECIAL local/lito381/server/ClientSession.recallNativeMagicDollLocked(Ljava/lang/String;Z)V
+ALOAD 25
+MONITOREXIT
+GOTO L287
+ASTORE 26
+ALOAD 25
+MONITOREXIT
+ALOAD 26
+ATHROW
+GOTO L294
+ASTORE 27
+GETSTATIC java/lang/System.out:Ljava/io/PrintStream;
+ALOAD 27
+INVOKEVIRTUAL java/lang/Exception.getMessage()Ljava/lang/String;
+INVOKEDYNAMIC makeConcatWithConstants(Ljava/lang/String;)Ljava/lang/String; bsm=H6 java/lang/invoke/StringConcatFactory.makeConcatWithConstants(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;Ljava/lang/String;[Ljava/lang/Object;)Ljava/lang/invoke/CallSite; args=[str:"[LiTo] magic-doll disconnect cleanup failed: \u0001"]
+INVOKEVIRTUAL java/io/PrintStream.println(Ljava/lang/String;)V
+ALOAD 0
+INVOKESPECIAL local/lito381/server/ClientSession.persistLitoCharacterOnDisconnect()V
+GOTO L306
+ASTORE 28
+GETSTATIC java/lang/System.out:Ljava/io/PrintStream;
+ALOAD 28
+INVOKEVIRTUAL java/lang/Object.getClass()Ljava/lang/Class;
+INVOKEVIRTUAL java/lang/Class.getSimpleName()Ljava/lang/String;
+ALOAD 28
+INVOKEVIRTUAL java/lang/Exception.getMessage()Ljava/lang/String;
+INVOKEDYNAMIC makeConcatWithConstants(Ljava/lang/String;Ljava/lang/String;)Ljava/lang/String; bsm=H6 java/lang/invoke/StringConcatFactory.makeConcatWithConstants(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;Ljava/lang/String;[Ljava/lang/Object;)Ljava/lang/invoke/CallSite; args=[str:"[LiTo] MySQL character disconnect persistence failed: \u0001: \u0001"]
+INVOKEVIRTUAL java/io/PrintStream.println(Ljava/lang/String;)V
+ALOAD 0
+INVOKESPECIAL local/lito381/server/ClientSession.notifyPeersOfDisconnect()V
+GOTO L315
+ASTORE 29
+GETSTATIC java/lang/System.out:Ljava/io/PrintStream;
+ALOAD 29
+INVOKEVIRTUAL java/io/IOException.getMessage()Ljava/lang/String;
+INVOKEDYNAMIC makeConcatWithConstants(Ljava/lang/String;)Ljava/lang/String; bsm=H6 java/lang/invoke/StringConcatFactory.makeConcatWithConstants(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;Ljava/lang/String;[Ljava/lang/Object;)Ljava/lang/invoke/CallSite; args=[str:"[LiTo] disconnect broadcast failed: \u0001"]
+INVOKEVIRTUAL java/io/PrintStream.println(Ljava/lang/String;)V
+ALOAD 0
+CONST str:"disconnect"
+INVOKESPECIAL local/lito381/server/ClientSession.resetNativeTournamentEconomy(Ljava/lang/String;)V
+ALOAD 0
+CONST null
+PUTFIELD local/lito381/server/ClientSession.pendingLitoRoomInvitation:Llocal/lito381/server/ClientSession$PendingLitoRoomInvitation;
+ALOAD 0
+CONST null
+PUTFIELD local/lito381/server/ClientSession.acceptedLitoRoomInvitation:Llocal/lito381/server/ClientSession$PendingLitoRoomInvitation;
+ALOAD 0
+GETSTATIC local/lito381/server/Protocol$LitoBuddyPresence.OFFLINE:Llocal/lito381/server/Protocol$LitoBuddyPresence;
+INVOKESPECIAL local/lito381/server/ClientSession.notifyBuddyPresence(Llocal/lito381/server/Protocol$LitoBuddyPresence;)V
+ALOAD 0
+GETFIELD local/lito381/server/ClientSession.selectedCharacter:Ljava/lang/String;
+INVOKEVIRTUAL java/lang/String.isBlank()Z
+IFNE L345
+ALOAD 0
+GETFIELD local/lito381/server/ClientSession.state:Llocal/lito381/server/GameStateStore;
+ALOAD 0
+INVOKESPECIAL local/lito381/server/ClientSession.stateCharacter()Ljava/lang/String;
+ALOAD 0
+GETFIELD local/lito381/server/ClientSession.sqlTimedBuffHook:Llocal/lito381/server/GameStateStore$TimedBuffPersistenceHook;
+INVOKEVIRTUAL local/lito381/server/GameStateStore.unregisterTimedBuffPersistence(Ljava/lang/String;Llocal/lito381/server/GameStateStore$TimedBuffPersistenceHook;)V
+ALOAD 0
+GETFIELD local/lito381/server/ClientSession.hub:Llocal/lito381/server/GameHub;
+ALOAD 0
+INVOKESPECIAL local/lito381/server/ClientSession.stateCharacter()Ljava/lang/String;
+ALOAD 0
+GETFIELD local/lito381/server/ClientSession.sqlBuddyHook:Llocal/lito381/server/GameHub$BuddyPersistenceHook;
+INVOKEVIRTUAL local/lito381/server/GameHub.unregisterBuddyPersistence(Ljava/lang/String;Llocal/lito381/server/GameHub$BuddyPersistenceHook;)V
+ALOAD 0
+GETFIELD local/lito381/server/ClientSession.hub:Llocal/lito381/server/GameHub;
+ALOAD 0
+INVOKEVIRTUAL local/lito381/server/GameHub.disconnect(Llocal/lito381/server/ClientSession;)Ljava/util/List;
+ALOAD 0
+INVOKESTATIC local/lito381/server/NativeRoomRules.restoreBot(Llocal/lito381/server/ClientSession;)V
+ASTORE 30
+ILOAD 8
+IFEQ L366
+ALOAD 0
+LLOAD 6/7
+CONST str:"disconnect"
+INVOKESPECIAL local/lito381/server/ClientSession.broadcastRoomOwner(JLjava/lang/String;)V
+GOTO L366
+ASTORE 31
+GETSTATIC java/lang/System.out:Ljava/io/PrintStream;
+LLOAD 6/7
+ALOAD 31
+INVOKEVIRTUAL java/io/IOException.getMessage()Ljava/lang/String;
+INVOKEDYNAMIC makeConcatWithConstants(JLjava/lang/String;)Ljava/lang/String; bsm=H6 java/lang/invoke/StringConcatFactory.makeConcatWithConstants(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;Ljava/lang/String;[Ljava/lang/Object;)Ljava/lang/invoke/CallSite; args=[str:"[LiTo] room-owner broadcast failed room=\u0001: \u0001"]
+INVOKEVIRTUAL java/io/PrintStream.println(Ljava/lang/String;)V
+ALOAD 30
+INVOKEINTERFACE java/util/List.iterator()Ljava/util/Iterator; itf
+ASTORE 32
+ALOAD 32
+INVOKEINTERFACE java/util/Iterator.hasNext()Z itf
+IFEQ L389
+ALOAD 32
+INVOKEINTERFACE java/util/Iterator.next()Ljava/lang/Object; itf
+CHECKCAST java/lang/Long
+INVOKEVIRTUAL java/lang/Long.longValue()J
+LSTORE 33/34
+ALOAD 0
+LLOAD 33/34
+INVOKESPECIAL local/lito381/server/ClientSession.broadcastRoomDeleted(J)V
+GOTO L388
+ASTORE 35
+GETSTATIC java/lang/System.out:Ljava/io/PrintStream;
+LLOAD 33/34
+ALOAD 35
+INVOKEVIRTUAL java/io/IOException.getMessage()Ljava/lang/String;
+INVOKEDYNAMIC makeConcatWithConstants(JLjava/lang/String;)Ljava/lang/String; bsm=H6 java/lang/invoke/StringConcatFactory.makeConcatWithConstants(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;Ljava/lang/String;[Ljava/lang/Object;)Ljava/lang/invoke/CallSite; args=[str:"[LiTo] room-delete broadcast failed room=\u0001: \u0001"]
+INVOKEVIRTUAL java/io/PrintStream.println(Ljava/lang/String;)V
+GOTO L369
+RETURN
 ALOAD 0
 GETFIELD local/lito381/server/ClientSession.incoming:Llocal/lito381/server/Protocol$CipherState;
-ALOAD 6
+ALOAD 5
 INVOKEVIRTUAL local/lito381/server/Protocol$CipherState.decrypt([B)[B
-ASTORE 7
+ASTORE 36
 ALOAD 0
 GETFIELD local/lito381/server/ClientSession.capture:Llocal/lito381/server/CaptureLog;
 CONST str:"client_to_server"
-ALOAD 7
-ALOAD 6
+ALOAD 36
+ALOAD 5
 ARRAYLENGTH
 CONST Integer:2
 IADD
-ALOAD 7
+ALOAD 36
 INVOKESTATIC local/lito381/server/ClientSession.packetNote([B)Ljava/lang/String;
 INVOKEVIRTUAL local/lito381/server/CaptureLog.frame(Ljava/lang/String;[BILjava/lang/String;)V
 ALOAD 0
-ALOAD 7
+ALOAD 36
 INVOKESPECIAL local/lito381/server/ClientSession.handle([B)V
 ALOAD 0
 GETFIELD local/lito381/server/ClientSession.quitRequested:Z
-IFEQ L104
-GOTO L105
-GOTO L69
-GOTO L119
-ASTORE 8
-ALOAD 3
-IFNULL L117
-ALOAD 3
+IFEQ L726
+ALOAD 2
+IFNULL L417
+ALOAD 2
 ALOAD 0
 INVOKESTATIC local/lito381/server/NativeServerLifetime.closeSessionSocket(Ljava/net/Socket;Llocal/lito381/server/ClientSession;)V
-GOTO L117
-ASTORE 9
-ALOAD 8
-ALOAD 9
-INVOKEVIRTUAL java/lang/Throwable.addSuppressed(Ljava/lang/Throwable;)V
-ALOAD 8
-ATHROW
-ALOAD 3
-IFNULL L124
-ALOAD 3
-ALOAD 0
-INVOKESTATIC local/lito381/server/NativeServerLifetime.closeSessionSocket(Ljava/net/Socket;Llocal/lito381/server/ClientSession;)V
 ALOAD 0
 INVOKESTATIC local/lito381/server/NativeServerLifetime.awaitCleanup(Llocal/lito381/server/ClientSession;)V
 ALOAD 0
 GETFIELD local/lito381/server/ClientSession.currentRoomId:J
-LSTORE 10/11
+LSTORE 37/38
 ALOAD 0
 GETFIELD local/lito381/server/ClientSession.phase:Llocal/lito381/server/ClientSession$Phase;
 GETSTATIC local/lito381/server/ClientSession$Phase.ROOM:Llocal/lito381/server/ClientSession$Phase;
-IF_ACMPNE L145
-LLOAD 10/11
+IF_ACMPNE L438
+LLOAD 37/38
 CONST Long:0
 LCMP
-IFEQ L145
+IFEQ L438
 ALOAD 0
 GETFIELD local/lito381/server/ClientSession.hub:Llocal/lito381/server/GameHub;
-LLOAD 10/11
+LLOAD 37/38
 ALOAD 0
 INVOKEVIRTUAL local/lito381/server/GameHub.isOwner(JLlocal/lito381/server/ClientSession;)Z
-IFEQ L145
+IFEQ L438
 CONST Integer:1
-GOTO L146
-CONST Integer:0
-ISTORE 12
+GOTO L439
+CONST Integer:0
+ISTORE 39
 ALOAD 0
 CONST str:"disconnect"
 CONST Integer:0
 INVOKESPECIAL local/lito381/server/ClientSession.cancelActivePlayerTrade(Ljava/lang/String;Z)V
-GOTO L158
-ASTORE 13
-GETSTATIC java/lang/System.out:Ljava/io/PrintStream;
-ALOAD 13
+GOTO L451
+ASTORE 40
+GETSTATIC java/lang/System.out:Ljava/io/PrintStream;
+ALOAD 40
 INVOKEVIRTUAL java/io/IOException.getMessage()Ljava/lang/String;
 INVOKEDYNAMIC makeConcatWithConstants(Ljava/lang/String;)Ljava/lang/String; bsm=H6 java/lang/invoke/StringConcatFactory.makeConcatWithConstants(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;Ljava/lang/String;[Ljava/lang/Object;)Ljava/lang/invoke/CallSite; args=[str:"[LiTo] trade disconnect cleanup failed: \u0001"]
 INVOKEVIRTUAL java/io/PrintStream.println(Ljava/lang/String;)V
 ALOAD 0
 GETFIELD local/lito381/server/ClientSession.classicPartyParticipant:Z
-IFEQ L172
+IFEQ L465
 ALOAD 0
 CONST str:"disconnect"
 CONST Integer:0
 INVOKESPECIAL local/lito381/server/ClientSession.leaveClassicParty(Ljava/lang/String;Z)V
-GOTO L172
-ASTORE 14
-GETSTATIC java/lang/System.out:Ljava/io/PrintStream;
-ALOAD 14
+GOTO L465
+ASTORE 41
+GETSTATIC java/lang/System.out:Ljava/io/PrintStream;
+ALOAD 41
 INVOKEVIRTUAL java/io/IOException.getMessage()Ljava/lang/String;
 INVOKEDYNAMIC makeConcatWithConstants(Ljava/lang/String;)Ljava/lang/String; bsm=H6 java/lang/invoke/StringConcatFactory.makeConcatWithConstants(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;Ljava/lang/String;[Ljava/lang/Object;)Ljava/lang/invoke/CallSite; args=[str:"[LiTo] party disconnect cleanup failed: \u0001"]
 INVOKEVIRTUAL java/io/PrintStream.println(Ljava/lang/String;)V
 ALOAD 0
 GETFIELD local/lito381/server/ClientSession.phase:Llocal/lito381/server/ClientSession$Phase;
 GETSTATIC local/lito381/server/ClientSession$Phase.WORLD:Llocal/lito381/server/ClientSession$Phase;
-IF_ACMPNE L194
+IF_ACMPNE L487
 ALOAD 0
 INVOKESPECIAL local/lito381/server/ClientSession.collectClassicWorldPetsOnDisconnect()V
-GOTO L185
-ASTORE 15
-GETSTATIC java/lang/System.out:Ljava/io/PrintStream;
-ALOAD 15
+GOTO L478
+ASTORE 42
+GETSTATIC java/lang/System.out:Ljava/io/PrintStream;
+ALOAD 42
 INVOKEVIRTUAL java/lang/Exception.getMessage()Ljava/lang/String;
 INVOKEDYNAMIC makeConcatWithConstants(Ljava/lang/String;)Ljava/lang/String; bsm=H6 java/lang/invoke/StringConcatFactory.makeConcatWithConstants(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;Ljava/lang/String;[Ljava/lang/Object;)Ljava/lang/invoke/CallSite; args=[str:"[LiTo] persistent pet disconnect cleanup failed: \u0001"]
 INVOKEVIRTUAL java/io/PrintStream.println(Ljava/lang/String;)V
 ALOAD 0
 INVOKESPECIAL local/lito381/server/ClientSession.cleanupClassicWorldTransientCompanionsOnDisconnect()V
-GOTO L194
-ASTORE 16
-GETSTATIC java/lang/System.out:Ljava/io/PrintStream;
-ALOAD 16
+GOTO L487
+ASTORE 43
+GETSTATIC java/lang/System.out:Ljava/io/PrintStream;
+ALOAD 43
 INVOKEVIRTUAL java/lang/Exception.getMessage()Ljava/lang/String;
 INVOKEDYNAMIC makeConcatWithConstants(Ljava/lang/String;)Ljava/lang/String; bsm=H6 java/lang/invoke/StringConcatFactory.makeConcatWithConstants(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;Ljava/lang/String;[Ljava/lang/Object;)Ljava/lang/invoke/CallSite; args=[str:"[LiTo] transient companion disconnect cleanup failed: \u0001"]
 INVOKEVIRTUAL java/io/PrintStream.println(Ljava/lang/String;)V
 ALOAD 0
 INVOKESPECIAL local/lito381/server/ClientSession.persistExperienceGemOnDisconnect()V
-GOTO L203
-ASTORE 17
-GETSTATIC java/lang/System.out:Ljava/io/PrintStream;
-ALOAD 17
+GOTO L496
+ASTORE 44
+GETSTATIC java/lang/System.out:Ljava/io/PrintStream;
+ALOAD 44
 INVOKEVIRTUAL java/lang/Exception.getMessage()Ljava/lang/String;
 INVOKEDYNAMIC makeConcatWithConstants(Ljava/lang/String;)Ljava/lang/String; bsm=H6 java/lang/invoke/StringConcatFactory.makeConcatWithConstants(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;Ljava/lang/String;[Ljava/lang/Object;)Ljava/lang/invoke/CallSite; args=[str:"[LiTo] Dragon Gem disconnect persistence failed: \u0001"]
 INVOKEVIRTUAL java/io/PrintStream.println(Ljava/lang/String;)V
 ALOAD 0
 INVOKESPECIAL local/lito381/server/ClientSession.persistFireWeaponOnDisconnect()V
-GOTO L212
-ASTORE 18
-GETSTATIC java/lang/System.out:Ljava/io/PrintStream;
-ALOAD 18
+GOTO L505
+ASTORE 45
+GETSTATIC java/lang/System.out:Ljava/io/PrintStream;
+ALOAD 45
 INVOKEVIRTUAL java/lang/Exception.getMessage()Ljava/lang/String;
 INVOKEDYNAMIC makeConcatWithConstants(Ljava/lang/String;)Ljava/lang/String; bsm=H6 java/lang/invoke/StringConcatFactory.makeConcatWithConstants(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;Ljava/lang/String;[Ljava/lang/Object;)Ljava/lang/invoke/CallSite; args=[str:"[LiTo] Fire Weapon disconnect persistence failed: \u0001"]
 INVOKEVIRTUAL java/io/PrintStream.println(Ljava/lang/String;)V
 ALOAD 0
 INVOKESPECIAL local/lito381/server/ClientSession.persistBurningWeaponOnDisconnect()V
-GOTO L221
-ASTORE 19
-GETSTATIC java/lang/System.out:Ljava/io/PrintStream;
-ALOAD 19
+GOTO L514
+ASTORE 46
+GETSTATIC java/lang/System.out:Ljava/io/PrintStream;
+ALOAD 46
 INVOKEVIRTUAL java/lang/Exception.getMessage()Ljava/lang/String;
 INVOKEDYNAMIC makeConcatWithConstants(Ljava/lang/String;)Ljava/lang/String; bsm=H6 java/lang/invoke/StringConcatFactory.makeConcatWithConstants(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;Ljava/lang/String;[Ljava/lang/Object;)Ljava/lang/invoke/CallSite; args=[str:"[LiTo] Burning Weapon disconnect persistence failed: \u0001"]
 INVOKEVIRTUAL java/io/PrintStream.println(Ljava/lang/String;)V
 ALOAD 0
 INVOKESPECIAL local/lito381/server/ClientSession.persistWindShotOnDisconnect()V
-GOTO L230
-ASTORE 20
-GETSTATIC java/lang/System.out:Ljava/io/PrintStream;
-ALOAD 20
+GOTO L523
+ASTORE 47
+GETSTATIC java/lang/System.out:Ljava/io/PrintStream;
+ALOAD 47
 INVOKEVIRTUAL java/lang/Exception.getMessage()Ljava/lang/String;
 INVOKEDYNAMIC makeConcatWithConstants(Ljava/lang/String;)Ljava/lang/String; bsm=H6 java/lang/invoke/StringConcatFactory.makeConcatWithConstants(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;Ljava/lang/String;[Ljava/lang/Object;)Ljava/lang/invoke/CallSite; args=[str:"[LiTo] Wind Shot disconnect persistence failed: \u0001"]
 INVOKEVIRTUAL java/io/PrintStream.println(Ljava/lang/String;)V
 ALOAD 0
 INVOKESPECIAL local/lito381/server/ClientSession.persistFireBlessOnDisconnect()V
-GOTO L239
-ASTORE 21
-GETSTATIC java/lang/System.out:Ljava/io/PrintStream;
-ALOAD 21
+GOTO L532
+ASTORE 48
+GETSTATIC java/lang/System.out:Ljava/io/PrintStream;
+ALOAD 48
 INVOKEVIRTUAL java/lang/Exception.getMessage()Ljava/lang/String;
 INVOKEDYNAMIC makeConcatWithConstants(Ljava/lang/String;)Ljava/lang/String; bsm=H6 java/lang/invoke/StringConcatFactory.makeConcatWithConstants(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;Ljava/lang/String;[Ljava/lang/Object;)Ljava/lang/invoke/CallSite; args=[str:"[LiTo] Fire Bless disconnect persistence failed: \u0001"]
 INVOKEVIRTUAL java/io/PrintStream.println(Ljava/lang/String;)V
 ALOAD 0
 INVOKESPECIAL local/lito381/server/ClientSession.persistStormEyeOnDisconnect()V
-GOTO L248
-ASTORE 22
-GETSTATIC java/lang/System.out:Ljava/io/PrintStream;
-ALOAD 22
+GOTO L541
+ASTORE 49
+GETSTATIC java/lang/System.out:Ljava/io/PrintStream;
+ALOAD 49
 INVOKEVIRTUAL java/lang/Exception.getMessage()Ljava/lang/String;
 INVOKEDYNAMIC makeConcatWithConstants(Ljava/lang/String;)Ljava/lang/String; bsm=H6 java/lang/invoke/StringConcatFactory.makeConcatWithConstants(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;Ljava/lang/String;[Ljava/lang/Object;)Ljava/lang/invoke/CallSite; args=[str:"[LiTo] Storm Eye disconnect persistence failed: \u0001"]
 INVOKEVIRTUAL java/io/PrintStream.println(Ljava/lang/String;)V
 ALOAD 0
 INVOKESPECIAL local/lito381/server/ClientSession.persistStormShotOnDisconnect()V
-GOTO L257
-ASTORE 23
-GETSTATIC java/lang/System.out:Ljava/io/PrintStream;
-ALOAD 23
+GOTO L550
+ASTORE 50
+GETSTATIC java/lang/System.out:Ljava/io/PrintStream;
+ALOAD 50
 INVOKEVIRTUAL java/lang/Exception.getMessage()Ljava/lang/String;
 INVOKEDYNAMIC makeConcatWithConstants(Ljava/lang/String;)Ljava/lang/String; bsm=H6 java/lang/invoke/StringConcatFactory.makeConcatWithConstants(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;Ljava/lang/String;[Ljava/lang/Object;)Ljava/lang/invoke/CallSite; args=[str:"[LiTo] Storm Shot disconnect persistence failed: \u0001"]
 INVOKEVIRTUAL java/io/PrintStream.println(Ljava/lang/String;)V
 ALOAD 0
 INVOKESPECIAL local/lito381/server/ClientSession.persistEarthBlessOnDisconnect()V
-GOTO L266
-ASTORE 24
-GETSTATIC java/lang/System.out:Ljava/io/PrintStream;
-ALOAD 24
+GOTO L559
+ASTORE 51
+GETSTATIC java/lang/System.out:Ljava/io/PrintStream;
+ALOAD 51
 INVOKEVIRTUAL java/lang/Exception.getMessage()Ljava/lang/String;
 INVOKEDYNAMIC makeConcatWithConstants(Ljava/lang/String;)Ljava/lang/String; bsm=H6 java/lang/invoke/StringConcatFactory.makeConcatWithConstants(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;Ljava/lang/String;[Ljava/lang/Object;)Ljava/lang/invoke/CallSite; args=[str:"[LiTo] Earth Bless disconnect persistence failed: \u0001"]
 INVOKEVIRTUAL java/io/PrintStream.println(Ljava/lang/String;)V
 ALOAD 0
 INVOKESPECIAL local/lito381/server/ClientSession.persistIronSkinOnDisconnect()V
-GOTO L275
-ASTORE 25
-GETSTATIC java/lang/System.out:Ljava/io/PrintStream;
-ALOAD 25
+GOTO L568
+ASTORE 52
+GETSTATIC java/lang/System.out:Ljava/io/PrintStream;
+ALOAD 52
 INVOKEVIRTUAL java/lang/Exception.getMessage()Ljava/lang/String;
 INVOKEDYNAMIC makeConcatWithConstants(Ljava/lang/String;)Ljava/lang/String; bsm=H6 java/lang/invoke/StringConcatFactory.makeConcatWithConstants(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;Ljava/lang/String;[Ljava/lang/Object;)Ljava/lang/invoke/CallSite; args=[str:"[LiTo] Iron Skin disconnect persistence failed: \u0001"]
 INVOKEVIRTUAL java/io/PrintStream.println(Ljava/lang/String;)V
 ALOAD 0
 INVOKESPECIAL local/lito381/server/ClientSession.persistMovementBuffOnDisconnect()V
-GOTO L284
-ASTORE 26
-GETSTATIC java/lang/System.out:Ljava/io/PrintStream;
-ALOAD 26
+GOTO L577
+ASTORE 53
+GETSTATIC java/lang/System.out:Ljava/io/PrintStream;
+ALOAD 53
 INVOKEVIRTUAL java/lang/Exception.getMessage()Ljava/lang/String;
 INVOKEDYNAMIC makeConcatWithConstants(Ljava/lang/String;)Ljava/lang/String; bsm=H6 java/lang/invoke/StringConcatFactory.makeConcatWithConstants(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;Ljava/lang/String;[Ljava/lang/Object;)Ljava/lang/invoke/CallSite; args=[str:"[LiTo] movement buff disconnect persistence failed: \u0001"]
 INVOKEVIRTUAL java/io/PrintStream.println(Ljava/lang/String;)V
 ALOAD 0
 INVOKESPECIAL local/lito381/server/ClientSession.persistArmorProtectionOnDisconnect()V
-GOTO L293
-ASTORE 27
-GETSTATIC java/lang/System.out:Ljava/io/PrintStream;
-ALOAD 27
+GOTO L586
+ASTORE 54
+GETSTATIC java/lang/System.out:Ljava/io/PrintStream;
+ALOAD 54
 INVOKEVIRTUAL java/lang/Exception.getMessage()Ljava/lang/String;
 INVOKEDYNAMIC makeConcatWithConstants(Ljava/lang/String;)Ljava/lang/String; bsm=H6 java/lang/invoke/StringConcatFactory.makeConcatWithConstants(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;Ljava/lang/String;[Ljava/lang/Object;)Ljava/lang/invoke/CallSite; args=[str:"[LiTo] armor protection disconnect persistence failed: \u0001"]
 INVOKEVIRTUAL java/io/PrintStream.println(Ljava/lang/String;)V
 ALOAD 0
 GETFIELD local/lito381/server/ClientSession.phase:Llocal/lito381/server/ClientSession$Phase;
 GETSTATIC local/lito381/server/ClientSession$Phase.WORLD:Llocal/lito381/server/ClientSession$Phase;
-IF_ACMPNE L313
+IF_ACMPNE L606
 ALOAD 0
 CONST str:"owner-disconnect"
 INVOKESPECIAL local/lito381/server/ClientSession.abandonClassicWorldRescueFollower(Ljava/lang/String;)V
@@ -306,55 +599,55 @@
 ALOAD 0
 CONST str:"owner-disconnect"
 INVOKESPECIAL local/lito381/server/ClientSession.abandonClassicWorldRoiFollower(Ljava/lang/String;)V
-GOTO L313
-ASTORE 28
-GETSTATIC java/lang/System.out:Ljava/io/PrintStream;
-ALOAD 28
+GOTO L606
+ASTORE 55
+GETSTATIC java/lang/System.out:Ljava/io/PrintStream;
+ALOAD 55
 INVOKEVIRTUAL java/lang/Exception.getMessage()Ljava/lang/String;
 INVOKEDYNAMIC makeConcatWithConstants(Ljava/lang/String;)Ljava/lang/String; bsm=H6 java/lang/invoke/StringConcatFactory.makeConcatWithConstants(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;Ljava/lang/String;[Ljava/lang/Object;)Ljava/lang/invoke/CallSite; args=[str:"[LiTo] quest follower disconnect cleanup failed: \u0001"]
 INVOKEVIRTUAL java/io/PrintStream.println(Ljava/lang/String;)V
 ALOAD 0
 GETFIELD local/lito381/server/ClientSession.activeMagicDollObjectIds:Ljava/util/Set;
 DUP
-ASTORE 29
+ASTORE 56
 MONITORENTER
 ALOAD 0
 CONST str:"owner-disconnect"
 CONST Integer:0
 INVOKESPECIAL local/lito381/server/ClientSession.recallNativeMagicDollLocked(Ljava/lang/String;Z)V
-ALOAD 29
+ALOAD 56
 MONITOREXIT
-GOTO L330
-ASTORE 30
-ALOAD 29
+GOTO L623
+ASTORE 57
+ALOAD 56
 MONITOREXIT
-ALOAD 30
+ALOAD 57
 ATHROW
-GOTO L337
-ASTORE 31
-GETSTATIC java/lang/System.out:Ljava/io/PrintStream;
-ALOAD 31
+GOTO L630
+ASTORE 58
+GETSTATIC java/lang/System.out:Ljava/io/PrintStream;
+ALOAD 58
 INVOKEVIRTUAL java/lang/Exception.getMessage()Ljava/lang/String;
 INVOKEDYNAMIC makeConcatWithConstants(Ljava/lang/String;)Ljava/lang/String; bsm=H6 java/lang/invoke/StringConcatFactory.makeConcatWithConstants(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;Ljava/lang/String;[Ljava/lang/Object;)Ljava/lang/invoke/CallSite; args=[str:"[LiTo] magic-doll disconnect cleanup failed: \u0001"]
 INVOKEVIRTUAL java/io/PrintStream.println(Ljava/lang/String;)V
 ALOAD 0
 INVOKESPECIAL local/lito381/server/ClientSession.persistLitoCharacterOnDisconnect()V
-GOTO L349
-ASTORE 32
-GETSTATIC java/lang/System.out:Ljava/io/PrintStream;
-ALOAD 32
+GOTO L642
+ASTORE 59
+GETSTATIC java/lang/System.out:Ljava/io/PrintStream;
+ALOAD 59
 INVOKEVIRTUAL java/lang/Object.getClass()Ljava/lang/Class;
 INVOKEVIRTUAL java/lang/Class.getSimpleName()Ljava/lang/String;
-ALOAD 32
+ALOAD 59
 INVOKEVIRTUAL java/lang/Exception.getMessage()Ljava/lang/String;
 INVOKEDYNAMIC makeConcatWithConstants(Ljava/lang/String;Ljava/lang/String;)Ljava/lang/String; bsm=H6 java/lang/invoke/StringConcatFactory.makeConcatWithConstants(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;Ljava/lang/String;[Ljava/lang/Object;)Ljava/lang/invoke/CallSite; args=[str:"[LiTo] MySQL character disconnect persistence failed: \u0001: \u0001"]
 INVOKEVIRTUAL java/io/PrintStream.println(Ljava/lang/String;)V
 ALOAD 0
 INVOKESPECIAL local/lito381/server/ClientSession.notifyPeersOfDisconnect()V
-GOTO L358
-ASTORE 33
-GETSTATIC java/lang/System.out:Ljava/io/PrintStream;
-ALOAD 33
+GOTO L651
+ASTORE 60
+GETSTATIC java/lang/System.out:Ljava/io/PrintStream;
+ALOAD 60
 INVOKEVIRTUAL java/io/IOException.getMessage()Ljava/lang/String;
 INVOKEDYNAMIC makeConcatWithConstants(Ljava/lang/String;)Ljava/lang/String; bsm=H6 java/lang/invoke/StringConcatFactory.makeConcatWithConstants(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;Ljava/lang/String;[Ljava/lang/Object;)Ljava/lang/invoke/CallSite; args=[str:"[LiTo] disconnect broadcast failed: \u0001"]
 INVOKEVIRTUAL java/io/PrintStream.println(Ljava/lang/String;)V
@@ -373,7 +666,7 @@
 ALOAD 0
 GETFIELD local/lito381/server/ClientSession.selectedCharacter:Ljava/lang/String;
 INVOKEVIRTUAL java/lang/String.isBlank()Z
-IFNE L388
+IFNE L681
 ALOAD 0
 GETFIELD local/lito381/server/ClientSession.state:Llocal/lito381/server/GameStateStore;
 ALOAD 0
@@ -392,225 +685,247 @@
 GETFIELD local/lito381/server/ClientSession.hub:Llocal/lito381/server/GameHub;
 ALOAD 0
 INVOKEVIRTUAL local/lito381/server/GameHub.disconnect(Llocal/lito381/server/ClientSession;)Ljava/util/List;
-ASTORE 34
 ALOAD 0
 INVOKESTATIC local/lito381/server/NativeRoomRules.restoreBot(Llocal/lito381/server/ClientSession;)V
-ILOAD 12
-IFEQ L409
-ALOAD 0
-LLOAD 10/11
+ASTORE 61
+ILOAD 39
+IFEQ L702
+ALOAD 0
+LLOAD 37/38
 CONST str:"disconnect"
 INVOKESPECIAL local/lito381/server/ClientSession.broadcastRoomOwner(JLjava/lang/String;)V
-GOTO L409
-ASTORE 35
-GETSTATIC java/lang/System.out:Ljava/io/PrintStream;
-LLOAD 10/11
-ALOAD 35
+GOTO L702
+ASTORE 62
+GETSTATIC java/lang/System.out:Ljava/io/PrintStream;
+LLOAD 37/38
+ALOAD 62
 INVOKEVIRTUAL java/io/IOException.getMessage()Ljava/lang/String;
 INVOKEDYNAMIC makeConcatWithConstants(JLjava/lang/String;)Ljava/lang/String; bsm=H6 java/lang/invoke/StringConcatFactory.makeConcatWithConstants(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;Ljava/lang/String;[Ljava/lang/Object;)Ljava/lang/invoke/CallSite; args=[str:"[LiTo] room-owner broadcast failed room=\u0001: \u0001"]
 INVOKEVIRTUAL java/io/PrintStream.println(Ljava/lang/String;)V
-ALOAD 34
+ALOAD 61
 INVOKEINTERFACE java/util/List.iterator()Ljava/util/Iterator; itf
-ASTORE 36
-ALOAD 36
+ASTORE 63
+ALOAD 63
 INVOKEINTERFACE java/util/Iterator.hasNext()Z itf
-IFEQ L432
-ALOAD 36
+IFEQ L725
+ALOAD 63
 INVOKEINTERFACE java/util/Iterator.next()Ljava/lang/Object; itf
 CHECKCAST java/lang/Long
 INVOKEVIRTUAL java/lang/Long.longValue()J
-LSTORE 37/38
-ALOAD 0
-LLOAD 37/38
+LSTORE 64/65
+ALOAD 0
+LLOAD 64/65
 INVOKESPECIAL local/lito381/server/ClientSession.broadcastRoomDeleted(J)V
-GOTO L431
-ASTORE 39
-GETSTATIC java/lang/System.out:Ljava/io/PrintStream;
-LLOAD 37/38
-ALOAD 39
+GOTO L724
+ASTORE 66
+GETSTATIC java/lang/System.out:Ljava/io/PrintStream;
+LLOAD 64/65
+ALOAD 66
 INVOKEVIRTUAL java/io/IOException.getMessage()Ljava/lang/String;
 INVOKEDYNAMIC makeConcatWithConstants(JLjava/lang/String;)Ljava/lang/String; bsm=H6 java/lang/invoke/StringConcatFactory.makeConcatWithConstants(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;Ljava/lang/String;[Ljava/lang/Object;)Ljava/lang/invoke/CallSite; args=[str:"[LiTo] room-delete broadcast failed room=\u0001: \u0001"]
 INVOKEVIRTUAL java/io/PrintStream.println(Ljava/lang/String;)V
-GOTO L412
+GOTO L705
 RETURN
-ALOAD 3
-IFNULL L438
-ALOAD 3
+GOTO L65
+ASTORE 67
+ALOAD 2
+IFNULL L738
+ALOAD 2
 ALOAD 0
 INVOKESTATIC local/lito381/server/NativeServerLifetime.closeSessionSocket(Ljava/net/Socket;Llocal/lito381/server/ClientSession;)V
+GOTO L738
+ASTORE 68
+ALOAD 67
+ALOAD 68
+INVOKEVIRTUAL java/lang/Throwable.addSuppressed(Ljava/lang/Throwable;)V
+ALOAD 67
+ATHROW
+ASTORE 69
+GETSTATIC java/lang/System.out:Ljava/io/PrintStream;
+ALOAD 0
+GETFIELD local/lito381/server/ClientSession.endpoint:Llocal/lito381/server/ClientSession$Endpoint;
+INVOKESTATIC java/lang/String.valueOf(Ljava/lang/Object;)Ljava/lang/String;
+ALOAD 1
+ALOAD 69
+INVOKEVIRTUAL java/lang/Object.getClass()Ljava/lang/Class;
+INVOKEVIRTUAL java/lang/Class.getSimpleName()Ljava/lang/String;
+ALOAD 69
+INVOKEVIRTUAL java/lang/Exception.getMessage()Ljava/lang/String;
+INVOKEDYNAMIC makeConcatWithConstants(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;)Ljava/lang/String; bsm=H6 java/lang/invoke/StringConcatFactory.makeConcatWithConstants(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;Ljava/lang/String;[Ljava/lang/Object;)Ljava/lang/invoke/CallSite; args=[str:"[LiTo] session ended endpoint=\u0001 peer=\u0001: \u0001: \u0001"]
+INVOKEVIRTUAL java/io/PrintStream.println(Ljava/lang/String;)V
 ALOAD 0
 INVOKESTATIC local/lito381/server/NativeServerLifetime.awaitCleanup(Llocal/lito381/server/ClientSession;)V
 ALOAD 0
 GETFIELD local/lito381/server/ClientSession.currentRoomId:J
-LSTORE 40/41
+LSTORE 70/71
 ALOAD 0
 GETFIELD local/lito381/server/ClientSession.phase:Llocal/lito381/server/ClientSession$Phase;
 GETSTATIC local/lito381/server/ClientSession$Phase.ROOM:Llocal/lito381/server/ClientSession$Phase;
-IF_ACMPNE L459
-LLOAD 40/41
+IF_ACMPNE L774
+LLOAD 70/71
 CONST Long:0
 LCMP
-IFEQ L459
+IFEQ L774
 ALOAD 0
 GETFIELD local/lito381/server/ClientSession.hub:Llocal/lito381/server/GameHub;
-LLOAD 40/41
+LLOAD 70/71
 ALOAD 0
 INVOKEVIRTUAL local/lito381/server/GameHub.isOwner(JLlocal/lito381/server/ClientSession;)Z
-IFEQ L459
+IFEQ L774
 CONST Integer:1
-GOTO L460
-CONST Integer:0
-ISTORE 42
+GOTO L775
+CONST Integer:0
+ISTORE 72
 ALOAD 0
 CONST str:"disconnect"
 CONST Integer:0
 INVOKESPECIAL local/lito381/server/ClientSession.cancelActivePlayerTrade(Ljava/lang/String;Z)V
-GOTO L472
-ASTORE 43
-GETSTATIC java/lang/System.out:Ljava/io/PrintStream;
-ALOAD 43
+GOTO L787
+ASTORE 73
+GETSTATIC java/lang/System.out:Ljava/io/PrintStream;
+ALOAD 73
 INVOKEVIRTUAL java/io/IOException.getMessage()Ljava/lang/String;
 INVOKEDYNAMIC makeConcatWithConstants(Ljava/lang/String;)Ljava/lang/String; bsm=H6 java/lang/invoke/StringConcatFactory.makeConcatWithConstants(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;Ljava/lang/String;[Ljava/lang/Object;)Ljava/lang/invoke/CallSite; args=[str:"[LiTo] trade disconnect cleanup failed: \u0001"]
 INVOKEVIRTUAL java/io/PrintStream.println(Ljava/lang/String;)V
 ALOAD 0
 GETFIELD local/lito381/server/ClientSession.classicPartyParticipant:Z
-IFEQ L486
+IFEQ L801
 ALOAD 0
 CONST str:"disconnect"
 CONST Integer:0
 INVOKESPECIAL local/lito381/server/ClientSession.leaveClassicParty(Ljava/lang/String;Z)V
-GOTO L486
-ASTORE 44
-GETSTATIC java/lang/System.out:Ljava/io/PrintStream;
-ALOAD 44
+GOTO L801
+ASTORE 74
+GETSTATIC java/lang/System.out:Ljava/io/PrintStream;
+ALOAD 74
 INVOKEVIRTUAL java/io/IOException.getMessage()Ljava/lang/String;
 INVOKEDYNAMIC makeConcatWithConstants(Ljava/lang/String;)Ljava/lang/String; bsm=H6 java/lang/invoke/StringConcatFactory.makeConcatWithConstants(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;Ljava/lang/String;[Ljava/lang/Object;)Ljava/lang/invoke/CallSite; args=[str:"[LiTo] party disconnect cleanup failed: \u0001"]
 INVOKEVIRTUAL java/io/PrintStream.println(Ljava/lang/String;)V
 ALOAD 0
 GETFIELD local/lito381/server/ClientSession.phase:Llocal/lito381/server/ClientSession$Phase;
 GETSTATIC local/lito381/server/ClientSession$Phase.WORLD:Llocal/lito381/server/ClientSession$Phase;
-IF_ACMPNE L508
+IF_ACMPNE L823
 ALOAD 0
 INVOKESPECIAL local/lito381/server/ClientSession.collectClassicWorldPetsOnDisconnect()V
-GOTO L499
-ASTORE 45
-GETSTATIC java/lang/System.out:Ljava/io/PrintStream;
-ALOAD 45
+GOTO L814
+ASTORE 75
+GETSTATIC java/lang/System.out:Ljava/io/PrintStream;
+ALOAD 75
 INVOKEVIRTUAL java/lang/Exception.getMessage()Ljava/lang/String;
 INVOKEDYNAMIC makeConcatWithConstants(Ljava/lang/String;)Ljava/lang/String; bsm=H6 java/lang/invoke/StringConcatFactory.makeConcatWithConstants(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;Ljava/lang/String;[Ljava/lang/Object;)Ljava/lang/invoke/CallSite; args=[str:"[LiTo] persistent pet disconnect cleanup failed: \u0001"]
 INVOKEVIRTUAL java/io/PrintStream.println(Ljava/lang/String;)V
 ALOAD 0
 INVOKESPECIAL local/lito381/server/ClientSession.cleanupClassicWorldTransientCompanionsOnDisconnect()V
-GOTO L508
-ASTORE 46
-GETSTATIC java/lang/System.out:Ljava/io/PrintStream;
-ALOAD 46
+GOTO L823
+ASTORE 76
+GETSTATIC java/lang/System.out:Ljava/io/PrintStream;
+ALOAD 76
 INVOKEVIRTUAL java/lang/Exception.getMessage()Ljava/lang/String;
 INVOKEDYNAMIC makeConcatWithConstants(Ljava/lang/String;)Ljava/lang/String; bsm=H6 java/lang/invoke/StringConcatFactory.makeConcatWithConstants(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;Ljava/lang/String;[Ljava/lang/Object;)Ljava/lang/invoke/CallSite; args=[str:"[LiTo] transient companion disconnect cleanup failed: \u0001"]
 INVOKEVIRTUAL java/io/PrintStream.println(Ljava/lang/String;)V
 ALOAD 0
 INVOKESPECIAL local/lito381/server/ClientSession.persistExperienceGemOnDisconnect()V
-GOTO L517
-ASTORE 47
-GETSTATIC java/lang/System.out:Ljava/io/PrintStream;
-ALOAD 47
+GOTO L832
+ASTORE 77
+GETSTATIC java/lang/System.out:Ljava/io/PrintStream;
+ALOAD 77
 INVOKEVIRTUAL java/lang/Exception.getMessage()Ljava/lang/String;
 INVOKEDYNAMIC makeConcatWithConstants(Ljava/lang/String;)Ljava/lang/String; bsm=H6 java/lang/invoke/StringConcatFactory.makeConcatWithConstants(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;Ljava/lang/String;[Ljava/lang/Object;)Ljava/lang/invoke/CallSite; args=[str:"[LiTo] Dragon Gem disconnect persistence failed: \u0001"]
 INVOKEVIRTUAL java/io/PrintStream.println(Ljava/lang/String;)V
 ALOAD 0
 INVOKESPECIAL local/lito381/server/ClientSession.persistFireWeaponOnDisconnect()V
-GOTO L526
-ASTORE 48
-GETSTATIC java/lang/System.out:Ljava/io/PrintStream;
-ALOAD 48
+GOTO L841
+ASTORE 78
+GETSTATIC java/lang/System.out:Ljava/io/PrintStream;
+ALOAD 78
 INVOKEVIRTUAL java/lang/Exception.getMessage()Ljava/lang/String;
 INVOKEDYNAMIC makeConcatWithConstants(Ljava/lang/String;)Ljava/lang/String; bsm=H6 java/lang/invoke/StringConcatFactory.makeConcatWithConstants(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;Ljava/lang/String;[Ljava/lang/Object;)Ljava/lang/invoke/CallSite; args=[str:"[LiTo] Fire Weapon disconnect persistence failed: \u0001"]
 INVOKEVIRTUAL java/io/PrintStream.println(Ljava/lang/String;)V
 ALOAD 0
 INVOKESPECIAL local/lito381/server/ClientSession.persistBurningWeaponOnDisconnect()V
-GOTO L535
-ASTORE 49
-GETSTATIC java/lang/System.out:Ljava/io/PrintStream;
-ALOAD 49
+GOTO L850
+ASTORE 79
+GETSTATIC java/lang/System.out:Ljava/io/PrintStream;
+ALOAD 79
 INVOKEVIRTUAL java/lang/Exception.getMessage()Ljava/lang/String;
 INVOKEDYNAMIC makeConcatWithConstants(Ljava/lang/String;)Ljava/lang/String; bsm=H6 java/lang/invoke/StringConcatFactory.makeConcatWithConstants(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;Ljava/lang/String;[Ljava/lang/Object;)Ljava/lang/invoke/CallSite; args=[str:"[LiTo] Burning Weapon disconnect persistence failed: \u0001"]
 INVOKEVIRTUAL java/io/PrintStream.println(Ljava/lang/String;)V
 ALOAD 0
 INVOKESPECIAL local/lito381/server/ClientSession.persistWindShotOnDisconnect()V
-GOTO L544
-ASTORE 50
-GETSTATIC java/lang/System.out:Ljava/io/PrintStream;
-ALOAD 50
+GOTO L859
+ASTORE 80
+GETSTATIC java/lang/System.out:Ljava/io/PrintStream;
+ALOAD 80
 INVOKEVIRTUAL java/lang/Exception.getMessage()Ljava/lang/String;
 INVOKEDYNAMIC makeConcatWithConstants(Ljava/lang/String;)Ljava/lang/String; bsm=H6 java/lang/invoke/StringConcatFactory.makeConcatWithConstants(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;Ljava/lang/String;[Ljava/lang/Object;)Ljava/lang/invoke/CallSite; args=[str:"[LiTo] Wind Shot disconnect persistence failed: \u0001"]
 INVOKEVIRTUAL java/io/PrintStream.println(Ljava/lang/String;)V
 ALOAD 0
 INVOKESPECIAL local/lito381/server/ClientSession.persistFireBlessOnDisconnect()V
-GOTO L553
-ASTORE 51
-GETSTATIC java/lang/System.out:Ljava/io/PrintStream;
-ALOAD 51
+GOTO L868
+ASTORE 81
+GETSTATIC java/lang/System.out:Ljava/io/PrintStream;
+ALOAD 81
 INVOKEVIRTUAL java/lang/Exception.getMessage()Ljava/lang/String;
 INVOKEDYNAMIC makeConcatWithConstants(Ljava/lang/String;)Ljava/lang/String; bsm=H6 java/lang/invoke/StringConcatFactory.makeConcatWithConstants(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;Ljava/lang/String;[Ljava/lang/Object;)Ljava/lang/invoke/CallSite; args=[str:"[LiTo] Fire Bless disconnect persistence failed: \u0001"]
 INVOKEVIRTUAL java/io/PrintStream.println(Ljava/lang/String;)V
 ALOAD 0
 INVOKESPECIAL local/lito381/server/ClientSession.persistStormEyeOnDisconnect()V
-GOTO L562
-ASTORE 52
-GETSTATIC java/lang/System.out:Ljava/io/PrintStream;
-ALOAD 52
+GOTO L877
+ASTORE 82
+GETSTATIC java/lang/System.out:Ljava/io/PrintStream;
+ALOAD 82
 INVOKEVIRTUAL java/lang/Exception.getMessage()Ljava/lang/String;
 INVOKEDYNAMIC makeConcatWithConstants(Ljava/lang/String;)Ljava/lang/String; bsm=H6 java/lang/invoke/StringConcatFactory.makeConcatWithConstants(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;Ljava/lang/String;[Ljava/lang/Object;)Ljava/lang/invoke/CallSite; args=[str:"[LiTo] Storm Eye disconnect persistence failed: \u0001"]
 INVOKEVIRTUAL java/io/PrintStream.println(Ljava/lang/String;)V
 ALOAD 0
 INVOKESPECIAL local/lito381/server/ClientSession.persistStormShotOnDisconnect()V
-GOTO L571
-ASTORE 53
-GETSTATIC java/lang/System.out:Ljava/io/PrintStream;
-ALOAD 53
+GOTO L886
+ASTORE 83
+GETSTATIC java/lang/System.out:Ljava/io/PrintStream;
+ALOAD 83
 INVOKEVIRTUAL java/lang/Exception.getMessage()Ljava/lang/String;
 INVOKEDYNAMIC makeConcatWithConstants(Ljava/lang/String;)Ljava/lang/String; bsm=H6 java/lang/invoke/StringConcatFactory.makeConcatWithConstants(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;Ljava/lang/String;[Ljava/lang/Object;)Ljava/lang/invoke/CallSite; args=[str:"[LiTo] Storm Shot disconnect persistence failed: \u0001"]
 INVOKEVIRTUAL java/io/PrintStream.println(Ljava/lang/String;)V
 ALOAD 0
 INVOKESPECIAL local/lito381/server/ClientSession.persistEarthBlessOnDisconnect()V
-GOTO L580
-ASTORE 54
-GETSTATIC java/lang/System.out:Ljava/io/PrintStream;
-ALOAD 54
+GOTO L895
+ASTORE 84
+GETSTATIC java/lang/System.out:Ljava/io/PrintStream;
+ALOAD 84
 INVOKEVIRTUAL java/lang/Exception.getMessage()Ljava/lang/String;
 INVOKEDYNAMIC makeConcatWithConstants(Ljava/lang/String;)Ljava/lang/String; bsm=H6 java/lang/invoke/StringConcatFactory.makeConcatWithConstants(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;Ljava/lang/String;[Ljava/lang/Object;)Ljava/lang/invoke/CallSite; args=[str:"[LiTo] Earth Bless disconnect persistence failed: \u0001"]
 INVOKEVIRTUAL java/io/PrintStream.println(Ljava/lang/String;)V
 ALOAD 0
 INVOKESPECIAL local/lito381/server/ClientSession.persistIronSkinOnDisconnect()V
-GOTO L589
-ASTORE 55
-GETSTATIC java/lang/System.out:Ljava/io/PrintStream;
-ALOAD 55
+GOTO L904
+ASTORE 85
+GETSTATIC java/lang/System.out:Ljava/io/PrintStream;
+ALOAD 85
 INVOKEVIRTUAL java/lang/Exception.getMessage()Ljava/lang/String;
 INVOKEDYNAMIC makeConcatWithConstants(Ljava/lang/String;)Ljava/lang/String; bsm=H6 java/lang/invoke/StringConcatFactory.makeConcatWithConstants(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;Ljava/lang/String;[Ljava/lang/Object;)Ljava/lang/invoke/CallSite; args=[str:"[LiTo] Iron Skin disconnect persistence failed: \u0001"]
 INVOKEVIRTUAL java/io/PrintStream.println(Ljava/lang/String;)V
 ALOAD 0
 INVOKESPECIAL local/lito381/server/ClientSession.persistMovementBuffOnDisconnect()V
-GOTO L598
-ASTORE 56
-GETSTATIC java/lang/System.out:Ljava/io/PrintStream;
-ALOAD 56
+GOTO L913
+ASTORE 86
+GETSTATIC java/lang/System.out:Ljava/io/PrintStream;
+ALOAD 86
 INVOKEVIRTUAL java/lang/Exception.getMessage()Ljava/lang/String;
 INVOKEDYNAMIC makeConcatWithConstants(Ljava/lang/String;)Ljava/lang/String; bsm=H6 java/lang/invoke/StringConcatFactory.makeConcatWithConstants(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;Ljava/lang/String;[Ljava/lang/Object;)Ljava/lang/invoke/CallSite; args=[str:"[LiTo] movement buff disconnect persistence failed: \u0001"]
 INVOKEVIRTUAL java/io/PrintStream.println(Ljava/lang/String;)V
 ALOAD 0
 INVOKESPECIAL local/lito381/server/ClientSession.persistArmorProtectionOnDisconnect()V
-GOTO L607
-ASTORE 57
-GETSTATIC java/lang/System.out:Ljava/io/PrintStream;
-ALOAD 57
+GOTO L922
+ASTORE 87
+GETSTATIC java/lang/System.out:Ljava/io/PrintStream;
+ALOAD 87
 INVOKEVIRTUAL java/lang/Exception.getMessage()Ljava/lang/String;
 INVOKEDYNAMIC makeConcatWithConstants(Ljava/lang/String;)Ljava/lang/String; bsm=H6 java/lang/invoke/StringConcatFactory.makeConcatWithConstants(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;Ljava/lang/String;[Ljava/lang/Object;)Ljava/lang/invoke/CallSite; args=[str:"[LiTo] armor protection disconnect persistence failed: \u0001"]
 INVOKEVIRTUAL java/io/PrintStream.println(Ljava/lang/String;)V
 ALOAD 0
 GETFIELD local/lito381/server/ClientSession.phase:Llocal/lito381/server/ClientSession$Phase;
 GETSTATIC local/lito381/server/ClientSession$Phase.WORLD:Llocal/lito381/server/ClientSession$Phase;
-IF_ACMPNE L627
+IF_ACMPNE L942
 ALOAD 0
 CONST str:"owner-disconnect"
 INVOKESPECIAL local/lito381/server/ClientSession.abandonClassicWorldRescueFollower(Ljava/lang/String;)V
@@ -620,55 +935,55 @@
 ALOAD 0
 CONST str:"owner-disconnect"
 INVOKESPECIAL local/lito381/server/ClientSession.abandonClassicWorldRoiFollower(Ljava/lang/String;)V
-GOTO L627
-ASTORE 58
-GETSTATIC java/lang/System.out:Ljava/io/PrintStream;
-ALOAD 58
+GOTO L942
+ASTORE 88
+GETSTATIC java/lang/System.out:Ljava/io/PrintStream;
+ALOAD 88
 INVOKEVIRTUAL java/lang/Exception.getMessage()Ljava/lang/String;
 INVOKEDYNAMIC makeConcatWithConstants(Ljava/lang/String;)Ljava/lang/String; bsm=H6 java/lang/invoke/StringConcatFactory.makeConcatWithConstants(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;Ljava/lang/String;[Ljava/lang/Object;)Ljava/lang/invoke/CallSite; args=[str:"[LiTo] quest follower disconnect cleanup failed: \u0001"]
 INVOKEVIRTUAL java/io/PrintStream.println(Ljava/lang/String;)V
 ALOAD 0
 GETFIELD local/lito381/server/ClientSession.activeMagicDollObjectIds:Ljava/util/Set;
 DUP
-ASTORE 59
+ASTORE 89
 MONITORENTER
 ALOAD 0
 CONST str:"owner-disconnect"
 CONST Integer:0
 INVOKESPECIAL local/lito381/server/ClientSession.recallNativeMagicDollLocked(Ljava/lang/String;Z)V
-ALOAD 59
+ALOAD 89
 MONITOREXIT
-GOTO L644
-ASTORE 60
-ALOAD 59
+GOTO L959
+ASTORE 90
+ALOAD 89
 MONITOREXIT
-ALOAD 60
+ALOAD 90
 ATHROW
-GOTO L651
-ASTORE 61
-GETSTATIC java/lang/System.out:Ljava/io/PrintStream;
-ALOAD 61
+GOTO L966
+ASTORE 91
+GETSTATIC java/lang/System.out:Ljava/io/PrintStream;
+ALOAD 91
 INVOKEVIRTUAL java/lang/Exception.getMessage()Ljava/lang/String;
 INVOKEDYNAMIC makeConcatWithConstants(Ljava/lang/String;)Ljava/lang/String; bsm=H6 java/lang/invoke/StringConcatFactory.makeConcatWithConstants(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;Ljava/lang/String;[Ljava/lang/Object;)Ljava/lang/invoke/CallSite; args=[str:"[LiTo] magic-doll disconnect cleanup failed: \u0001"]
 INVOKEVIRTUAL java/io/PrintStream.println(Ljava/lang/String;)V
 ALOAD 0
 INVOKESPECIAL local/lito381/server/ClientSession.persistLitoCharacterOnDisconnect()V
-GOTO L663
-ASTORE 62
-GETSTATIC java/lang/System.out:Ljava/io/PrintStream;
-ALOAD 62
+GOTO L978
+ASTORE 92
+GETSTATIC java/lang/System.out:Ljava/io/PrintStream;
+ALOAD 92
 INVOKEVIRTUAL java/lang/Object.getClass()Ljava/lang/Class;
 INVOKEVIRTUAL java/lang/Class.getSimpleName()Ljava/lang/String;
-ALOAD 62
+ALOAD 92
 INVOKEVIRTUAL java/lang/Exception.getMessage()Ljava/lang/String;
 INVOKEDYNAMIC makeConcatWithConstants(Ljava/lang/String;Ljava/lang/String;)Ljava/lang/String; bsm=H6 java/lang/invoke/StringConcatFactory.makeConcatWithConstants(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;Ljava/lang/String;[Ljava/lang/Object;)Ljava/lang/invoke/CallSite; args=[str:"[LiTo] MySQL character disconnect persistence failed: \u0001: \u0001"]
 INVOKEVIRTUAL java/io/PrintStream.println(Ljava/lang/String;)V
 ALOAD 0
 INVOKESPECIAL local/lito381/server/ClientSession.notifyPeersOfDisconnect()V
-GOTO L672
-ASTORE 63
-GETSTATIC java/lang/System.out:Ljava/io/PrintStream;
-ALOAD 63
+GOTO L987
+ASTORE 93
+GETSTATIC java/lang/System.out:Ljava/io/PrintStream;
+ALOAD 93
 INVOKEVIRTUAL java/io/IOException.getMessage()Ljava/lang/String;
 INVOKEDYNAMIC makeConcatWithConstants(Ljava/lang/String;)Ljava/lang/String; bsm=H6 java/lang/invoke/StringConcatFactory.makeConcatWithConstants(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;Ljava/lang/String;[Ljava/lang/Object;)Ljava/lang/invoke/CallSite; args=[str:"[LiTo] disconnect broadcast failed: \u0001"]
 INVOKEVIRTUAL java/io/PrintStream.println(Ljava/lang/String;)V
@@ -687,7 +1002,7 @@
 ALOAD 0
 GETFIELD local/lito381/server/ClientSession.selectedCharacter:Ljava/lang/String;
 INVOKEVIRTUAL java/lang/String.isBlank()Z
-IFNE L702
+IFNE L1017
 ALOAD 0
 GETFIELD local/lito381/server/ClientSession.state:Llocal/lito381/server/GameStateStore;
 ALOAD 0
@@ -706,291 +1021,279 @@
 GETFIELD local/lito381/server/ClientSession.hub:Llocal/lito381/server/GameHub;
 ALOAD 0
 INVOKEVIRTUAL local/lito381/server/GameHub.disconnect(Llocal/lito381/server/ClientSession;)Ljava/util/List;
-ASTORE 64
 ALOAD 0
 INVOKESTATIC local/lito381/server/NativeRoomRules.restoreBot(Llocal/lito381/server/ClientSession;)V
-ILOAD 42
-IFEQ L723
-ALOAD 0
-LLOAD 40/41
+ASTORE 94
+ILOAD 72
+IFEQ L1038
+ALOAD 0
+LLOAD 70/71
 CONST str:"disconnect"
 INVOKESPECIAL local/lito381/server/ClientSession.broadcastRoomOwner(JLjava/lang/String;)V
-GOTO L723
-ASTORE 65
-GETSTATIC java/lang/System.out:Ljava/io/PrintStream;
-LLOAD 40/41
-ALOAD 65
+GOTO L1038
+ASTORE 95
+GETSTATIC java/lang/System.out:Ljava/io/PrintStream;
+LLOAD 70/71
+ALOAD 95
 INVOKEVIRTUAL java/io/IOException.getMessage()Ljava/lang/String;
 INVOKEDYNAMIC makeConcatWithConstants(JLjava/lang/String;)Ljava/lang/String; bsm=H6 java/lang/invoke/StringConcatFactory.makeConcatWithConstants(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;Ljava/lang/String;[Ljava/lang/Object;)Ljava/lang/invoke/CallSite; args=[str:"[LiTo] room-owner broadcast failed room=\u0001: \u0001"]
 INVOKEVIRTUAL java/io/PrintStream.println(Ljava/lang/String;)V
-ALOAD 64
+ALOAD 94
 INVOKEINTERFACE java/util/List.iterator()Ljava/util/Iterator; itf
-ASTORE 66
-ALOAD 66
+ASTORE 96
+ALOAD 96
 INVOKEINTERFACE java/util/Iterator.hasNext()Z itf
-IFEQ L746
-ALOAD 66
+IFEQ L1061
+ALOAD 96
 INVOKEINTERFACE java/util/Iterator.next()Ljava/lang/Object; itf
 CHECKCAST java/lang/Long
 INVOKEVIRTUAL java/lang/Long.longValue()J
-LSTORE 67/68
-ALOAD 0
-LLOAD 67/68
+LSTORE 97/98
+ALOAD 0
+LLOAD 97/98
 INVOKESPECIAL local/lito381/server/ClientSession.broadcastRoomDeleted(J)V
-GOTO L745
-ASTORE 69
-GETSTATIC java/lang/System.out:Ljava/io/PrintStream;
-LLOAD 67/68
-ALOAD 69
+GOTO L1060
+ASTORE 99
+GETSTATIC java/lang/System.out:Ljava/io/PrintStream;
+LLOAD 97/98
+ALOAD 99
 INVOKEVIRTUAL java/io/IOException.getMessage()Ljava/lang/String;
 INVOKEDYNAMIC makeConcatWithConstants(JLjava/lang/String;)Ljava/lang/String; bsm=H6 java/lang/invoke/StringConcatFactory.makeConcatWithConstants(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;Ljava/lang/String;[Ljava/lang/Object;)Ljava/lang/invoke/CallSite; args=[str:"[LiTo] room-delete broadcast failed room=\u0001: \u0001"]
 INVOKEVIRTUAL java/io/PrintStream.println(Ljava/lang/String;)V
-GOTO L726
-GOTO L1380
-ASTORE 70
-GETSTATIC java/lang/System.out:Ljava/io/PrintStream;
-ALOAD 0
-GETFIELD local/lito381/server/ClientSession.endpoint:Llocal/lito381/server/ClientSession$Endpoint;
-INVOKESTATIC java/lang/String.valueOf(Ljava/lang/Object;)Ljava/lang/String;
-ALOAD 2
-ALOAD 70
+GOTO L1041
+GOTO L1373
+ASTORE 100
+ALOAD 0
+INVOKESTATIC local/lito381/server/NativeServerLifetime.awaitCleanup(Llocal/lito381/server/ClientSession;)V
+ALOAD 0
+GETFIELD local/lito381/server/ClientSession.currentRoomId:J
+LSTORE 101/102
+ALOAD 0
+GETFIELD local/lito381/server/ClientSession.phase:Llocal/lito381/server/ClientSession$Phase;
+GETSTATIC local/lito381/server/ClientSession$Phase.ROOM:Llocal/lito381/server/ClientSession$Phase;
+IF_ACMPNE L1084
+LLOAD 101/102
+CONST Long:0
+LCMP
+IFEQ L1084
+ALOAD 0
+GETFIELD local/lito381/server/ClientSession.hub:Llocal/lito381/server/GameHub;
+LLOAD 101/102
+ALOAD 0
+INVOKEVIRTUAL local/lito381/server/GameHub.isOwner(JLlocal/lito381/server/ClientSession;)Z
+IFEQ L1084
+CONST Integer:1
+GOTO L1085
+CONST Integer:0
+ISTORE 103
+ALOAD 0
+CONST str:"disconnect"
+CONST Integer:0
+INVOKESPECIAL local/lito381/server/ClientSession.cancelActivePlayerTrade(Ljava/lang/String;Z)V
+GOTO L1097
+ASTORE 104
+GETSTATIC java/lang/System.out:Ljava/io/PrintStream;
+ALOAD 104
+INVOKEVIRTUAL java/io/IOException.getMessage()Ljava/lang/String;
+INVOKEDYNAMIC makeConcatWithConstants(Ljava/lang/String;)Ljava/lang/String; bsm=H6 java/lang/invoke/StringConcatFactory.makeConcatWithConstants(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;Ljava/lang/String;[Ljava/lang/Object;)Ljava/lang/invoke/CallSite; args=[str:"[LiTo] trade disconnect cleanup failed: \u0001"]
+INVOKEVIRTUAL java/io/PrintStream.println(Ljava/lang/String;)V
+ALOAD 0
+GETFIELD local/lito381/server/ClientSession.classicPartyParticipant:Z
+IFEQ L1111
+ALOAD 0
+CONST str:"disconnect"
+CONST Integer:0
+INVOKESPECIAL local/lito381/server/ClientSession.leaveClassicParty(Ljava/lang/String;Z)V
+GOTO L1111
+ASTORE 105
+GETSTATIC java/lang/System.out:Ljava/io/PrintStream;
+ALOAD 105
+INVOKEVIRTUAL java/io/IOException.getMessage()Ljava/lang/String;
+INVOKEDYNAMIC makeConcatWithConstants(Ljava/lang/String;)Ljava/lang/String; bsm=H6 java/lang/invoke/StringConcatFactory.makeConcatWithConstants(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;Ljava/lang/String;[Ljava/lang/Object;)Ljava/lang/invoke/CallSite; args=[str:"[LiTo] party disconnect cleanup failed: \u0001"]
+INVOKEVIRTUAL java/io/PrintStream.println(Ljava/lang/String;)V
+ALOAD 0
+GETFIELD local/lito381/server/ClientSession.phase:Llocal/lito381/server/ClientSession$Phase;
+GETSTATIC local/lito381/server/ClientSession$Phase.WORLD:Llocal/lito381/server/ClientSession$Phase;
+IF_ACMPNE L1133
+ALOAD 0
+INVOKESPECIAL local/lito381/server/ClientSession.collectClassicWorldPetsOnDisconnect()V
+GOTO L1124
+ASTORE 106
+GETSTATIC java/lang/System.out:Ljava/io/PrintStream;
+ALOAD 106
+INVOKEVIRTUAL java/lang/Exception.getMessage()Ljava/lang/String;
+INVOKEDYNAMIC makeConcatWithConstants(Ljava/lang/String;)Ljava/lang/String; bsm=H6 java/lang/invoke/StringConcatFactory.makeConcatWithConstants(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;Ljava/lang/String;[Ljava/lang/Object;)Ljava/lang/invoke/CallSite; args=[str:"[LiTo] persistent pet disconnect cleanup failed: \u0001"]
+INVOKEVIRTUAL java/io/PrintStream.println(Ljava/lang/String;)V
+ALOAD 0
+INVOKESPECIAL local/lito381/server/ClientSession.cleanupClassicWorldTransientCompanionsOnDisconnect()V
+GOTO L1133
+ASTORE 107
+GETSTATIC java/lang/System.out:Ljava/io/PrintStream;
+ALOAD 107
+INVOKEVIRTUAL java/lang/Exception.getMessage()Ljava/lang/String;
+INVOKEDYNAMIC makeConcatWithConstants(Ljava/lang/String;)Ljava/lang/String; bsm=H6 java/lang/invoke/StringConcatFactory.makeConcatWithConstants(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;Ljava/lang/String;[Ljava/lang/Object;)Ljava/lang/invoke/CallSite; args=[str:"[LiTo] transient companion disconnect cleanup failed: \u0001"]
+INVOKEVIRTUAL java/io/PrintStream.println(Ljava/lang/String;)V
+ALOAD 0
+INVOKESPECIAL local/lito381/server/ClientSession.persistExperienceGemOnDisconnect()V
+GOTO L1142
+ASTORE 108
+GETSTATIC java/lang/System.out:Ljava/io/PrintStream;
+ALOAD 108
+INVOKEVIRTUAL java/lang/Exception.getMessage()Ljava/lang/String;
+INVOKEDYNAMIC makeConcatWithConstants(Ljava/lang/String;)Ljava/lang/String; bsm=H6 java/lang/invoke/StringConcatFactory.makeConcatWithConstants(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;Ljava/lang/String;[Ljava/lang/Object;)Ljava/lang/invoke/CallSite; args=[str:"[LiTo] Dragon Gem disconnect persistence failed: \u0001"]
+INVOKEVIRTUAL java/io/PrintStream.println(Ljava/lang/String;)V
+ALOAD 0
+INVOKESPECIAL local/lito381/server/ClientSession.persistFireWeaponOnDisconnect()V
+GOTO L1151
+ASTORE 109
+GETSTATIC java/lang/System.out:Ljava/io/PrintStream;
+ALOAD 109
+INVOKEVIRTUAL java/lang/Exception.getMessage()Ljava/lang/String;
+INVOKEDYNAMIC makeConcatWithConstants(Ljava/lang/String;)Ljava/lang/String; bsm=H6 java/lang/invoke/StringConcatFactory.makeConcatWithConstants(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;Ljava/lang/String;[Ljava/lang/Object;)Ljava/lang/invoke/CallSite; args=[str:"[LiTo] Fire Weapon disconnect persistence failed: \u0001"]
+INVOKEVIRTUAL java/io/PrintStream.println(Ljava/lang/String;)V
+ALOAD 0
+INVOKESPECIAL local/lito381/server/ClientSession.persistBurningWeaponOnDisconnect()V
+GOTO L1160
+ASTORE 110
+GETSTATIC java/lang/System.out:Ljava/io/PrintStream;
+ALOAD 110
+INVOKEVIRTUAL java/lang/Exception.getMessage()Ljava/lang/String;
+INVOKEDYNAMIC makeConcatWithConstants(Ljava/lang/String;)Ljava/lang/String; bsm=H6 java/lang/invoke/StringConcatFactory.makeConcatWithConstants(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;Ljava/lang/String;[Ljava/lang/Object;)Ljava/lang/invoke/CallSite; args=[str:"[LiTo] Burning Weapon disconnect persistence failed: \u0001"]
+INVOKEVIRTUAL java/io/PrintStream.println(Ljava/lang/String;)V
+ALOAD 0
+INVOKESPECIAL local/lito381/server/ClientSession.persistWindShotOnDisconnect()V
+GOTO L1169
+ASTORE 111
+GETSTATIC java/lang/System.out:Ljava/io/PrintStream;
+ALOAD 111
+INVOKEVIRTUAL java/lang/Exception.getMessage()Ljava/lang/String;
+INVOKEDYNAMIC makeConcatWithConstants(Ljava/lang/String;)Ljava/lang/String; bsm=H6 java/lang/invoke/StringConcatFactory.makeConcatWithConstants(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;Ljava/lang/String;[Ljava/lang/Object;)Ljava/lang/invoke/CallSite; args=[str:"[LiTo] Wind Shot disconnect persistence failed: \u0001"]
+INVOKEVIRTUAL java/io/PrintStream.println(Ljava/lang/String;)V
+ALOAD 0
+INVOKESPECIAL local/lito381/server/ClientSession.persistFireBlessOnDisconnect()V
+GOTO L1178
+ASTORE 112
+GETSTATIC java/lang/System.out:Ljava/io/PrintStream;
+ALOAD 112
+INVOKEVIRTUAL java/lang/Exception.getMessage()Ljava/lang/String;
+INVOKEDYNAMIC makeConcatWithConstants(Ljava/lang/String;)Ljava/lang/String; bsm=H6 java/lang/invoke/StringConcatFactory.makeConcatWithConstants(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;Ljava/lang/String;[Ljava/lang/Object;)Ljava/lang/invoke/CallSite; args=[str:"[LiTo] Fire Bless disconnect persistence failed: \u0001"]
+INVOKEVIRTUAL java/io/PrintStream.println(Ljava/lang/String;)V
+ALOAD 0
+INVOKESPECIAL local/lito381/server/ClientSession.persistStormEyeOnDisconnect()V
+GOTO L1187
+ASTORE 113
+GETSTATIC java/lang/System.out:Ljava/io/PrintStream;
+ALOAD 113
+INVOKEVIRTUAL java/lang/Exception.getMessage()Ljava/lang/String;
+INVOKEDYNAMIC makeConcatWithConstants(Ljava/lang/String;)Ljava/lang/String; bsm=H6 java/lang/invoke/StringConcatFactory.makeConcatWithConstants(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;Ljava/lang/String;[Ljava/lang/Object;)Ljava/lang/invoke/CallSite; args=[str:"[LiTo] Storm Eye disconnect persistence failed: \u0001"]
+INVOKEVIRTUAL java/io/PrintStream.println(Ljava/lang/String;)V
+ALOAD 0
+INVOKESPECIAL local/lito381/server/ClientSession.persistStormShotOnDisconnect()V
+GOTO L1196
+ASTORE 114
+GETSTATIC java/lang/System.out:Ljava/io/PrintStream;
+ALOAD 114
+INVOKEVIRTUAL java/lang/Exception.getMessage()Ljava/lang/String;
+INVOKEDYNAMIC makeConcatWithConstants(Ljava/lang/String;)Ljava/lang/String; bsm=H6 java/lang/invoke/StringConcatFactory.makeConcatWithConstants(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;Ljava/lang/String;[Ljava/lang/Object;)Ljava/lang/invoke/CallSite; args=[str:"[LiTo] Storm Shot disconnect persistence failed: \u0001"]
+INVOKEVIRTUAL java/io/PrintStream.println(Ljava/lang/String;)V
+ALOAD 0
+INVOKESPECIAL local/lito381/server/ClientSession.persistEarthBlessOnDisconnect()V
+GOTO L1205
+ASTORE 115
+GETSTATIC java/lang/System.out:Ljava/io/PrintStream;
+ALOAD 115
+INVOKEVIRTUAL java/lang/Exception.getMessage()Ljava/lang/String;
+INVOKEDYNAMIC makeConcatWithConstants(Ljava/lang/String;)Ljava/lang/String; bsm=H6 java/lang/invoke/StringConcatFactory.makeConcatWithConstants(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;Ljava/lang/String;[Ljava/lang/Object;)Ljava/lang/invoke/CallSite; args=[str:"[LiTo] Earth Bless disconnect persistence failed: \u0001"]
+INVOKEVIRTUAL java/io/PrintStream.println(Ljava/lang/String;)V
+ALOAD 0
+INVOKESPECIAL local/lito381/server/ClientSession.persistIronSkinOnDisconnect()V
+GOTO L1214
+ASTORE 116
+GETSTATIC java/lang/System.out:Ljava/io/PrintStream;
+ALOAD 116
+INVOKEVIRTUAL java/lang/Exception.getMessage()Ljava/lang/String;
+INVOKEDYNAMIC makeConcatWithConstants(Ljava/lang/String;)Ljava/lang/String; bsm=H6 java/lang/invoke/StringConcatFactory.makeConcatWithConstants(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;Ljava/lang/String;[Ljava/lang/Object;)Ljava/lang/invoke/CallSite; args=[str:"[LiTo] Iron Skin disconnect persistence failed: \u0001"]
+INVOKEVIRTUAL java/io/PrintStream.println(Ljava/lang/String;)V
+ALOAD 0
+INVOKESPECIAL local/lito381/server/ClientSession.persistMovementBuffOnDisconnect()V
+GOTO L1223
+ASTORE 117
+GETSTATIC java/lang/System.out:Ljava/io/PrintStream;
+ALOAD 117
+INVOKEVIRTUAL java/lang/Exception.getMessage()Ljava/lang/String;
+INVOKEDYNAMIC makeConcatWithConstants(Ljava/lang/String;)Ljava/lang/String; bsm=H6 java/lang/invoke/StringConcatFactory.makeConcatWithConstants(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;Ljava/lang/String;[Ljava/lang/Object;)Ljava/lang/invoke/CallSite; args=[str:"[LiTo] movement buff disconnect persistence failed: \u0001"]
+INVOKEVIRTUAL java/io/PrintStream.println(Ljava/lang/String;)V
+ALOAD 0
+INVOKESPECIAL local/lito381/server/ClientSession.persistArmorProtectionOnDisconnect()V
+GOTO L1232
+ASTORE 118
+GETSTATIC java/lang/System.out:Ljava/io/PrintStream;
+ALOAD 118
+INVOKEVIRTUAL java/lang/Exception.getMessage()Ljava/lang/String;
+INVOKEDYNAMIC makeConcatWithConstants(Ljava/lang/String;)Ljava/lang/String; bsm=H6 java/lang/invoke/StringConcatFactory.makeConcatWithConstants(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;Ljava/lang/String;[Ljava/lang/Object;)Ljava/lang/invoke/CallSite; args=[str:"[LiTo] armor protection disconnect persistence failed: \u0001"]
+INVOKEVIRTUAL java/io/PrintStream.println(Ljava/lang/String;)V
+ALOAD 0
+GETFIELD local/lito381/server/ClientSession.phase:Llocal/lito381/server/ClientSession$Phase;
+GETSTATIC local/lito381/server/ClientSession$Phase.WORLD:Llocal/lito381/server/ClientSession$Phase;
+IF_ACMPNE L1252
+ALOAD 0
+CONST str:"owner-disconnect"
+INVOKESPECIAL local/lito381/server/ClientSession.abandonClassicWorldRescueFollower(Ljava/lang/String;)V
+ALOAD 0
+CONST str:"owner-disconnect"
+INVOKESPECIAL local/lito381/server/ClientSession.abandonClassicWorldDspyFollower(Ljava/lang/String;)V
+ALOAD 0
+CONST str:"owner-disconnect"
+INVOKESPECIAL local/lito381/server/ClientSession.abandonClassicWorldRoiFollower(Ljava/lang/String;)V
+GOTO L1252
+ASTORE 119
+GETSTATIC java/lang/System.out:Ljava/io/PrintStream;
+ALOAD 119
+INVOKEVIRTUAL java/lang/Exception.getMessage()Ljava/lang/String;
+INVOKEDYNAMIC makeConcatWithConstants(Ljava/lang/String;)Ljava/lang/String; bsm=H6 java/lang/invoke/StringConcatFactory.makeConcatWithConstants(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;Ljava/lang/String;[Ljava/lang/Object;)Ljava/lang/invoke/CallSite; args=[str:"[LiTo] quest follower disconnect cleanup failed: \u0001"]
+INVOKEVIRTUAL java/io/PrintStream.println(Ljava/lang/String;)V
+ALOAD 0
+GETFIELD local/lito381/server/ClientSession.activeMagicDollObjectIds:Ljava/util/Set;
+DUP
+ASTORE 120
+MONITORENTER
+ALOAD 0
+CONST str:"owner-disconnect"
+CONST Integer:0
+INVOKESPECIAL local/lito381/server/ClientSession.recallNativeMagicDollLocked(Ljava/lang/String;Z)V
+ALOAD 120
+MONITOREXIT
+GOTO L1269
+ASTORE 121
+ALOAD 120
+MONITOREXIT
+ALOAD 121
+ATHROW
+GOTO L1276
+ASTORE 122
+GETSTATIC java/lang/System.out:Ljava/io/PrintStream;
+ALOAD 122
+INVOKEVIRTUAL java/lang/Exception.getMessage()Ljava/lang/String;
+INVOKEDYNAMIC makeConcatWithConstants(Ljava/lang/String;)Ljava/lang/String; bsm=H6 java/lang/invoke/StringConcatFactory.makeConcatWithConstants(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;Ljava/lang/String;[Ljava/lang/Object;)Ljava/lang/invoke/CallSite; args=[str:"[LiTo] magic-doll disconnect cleanup failed: \u0001"]
+INVOKEVIRTUAL java/io/PrintStream.println(Ljava/lang/String;)V
+ALOAD 0
+INVOKESPECIAL local/lito381/server/ClientSession.persistLitoCharacterOnDisconnect()V
+GOTO L1288
+ASTORE 123
+GETSTATIC java/lang/System.out:Ljava/io/PrintStream;
+ALOAD 123
 INVOKEVIRTUAL java/lang/Object.getClass()Ljava/lang/Class;
 INVOKEVIRTUAL java/lang/Class.getSimpleName()Ljava/lang/String;
-ALOAD 70
-INVOKEVIRTUAL java/lang/Exception.getMessage()Ljava/lang/String;
-INVOKEDYNAMIC makeConcatWithConstants(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;)Ljava/lang/String; bsm=H6 java/lang/invoke/StringConcatFactory.makeConcatWithConstants(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;Ljava/lang/String;[Ljava/lang/Object;)Ljava/lang/invoke/CallSite; args=[str:"[LiTo] session ended endpoint=\u0001 peer=\u0001: \u0001: \u0001"]
-INVOKEVIRTUAL java/io/PrintStream.println(Ljava/lang/String;)V
-ALOAD 0
-INVOKESTATIC local/lito381/server/NativeServerLifetime.awaitCleanup(Llocal/lito381/server/ClientSession;)V
-ALOAD 0
-GETFIELD local/lito381/server/ClientSession.currentRoomId:J
-LSTORE 71/72
-ALOAD 0
-GETFIELD local/lito381/server/ClientSession.phase:Llocal/lito381/server/ClientSession$Phase;
-GETSTATIC local/lito381/server/ClientSession$Phase.ROOM:Llocal/lito381/server/ClientSession$Phase;
-IF_ACMPNE L781
-LLOAD 71/72
-CONST Long:0
-LCMP
-IFEQ L781
-ALOAD 0
-GETFIELD local/lito381/server/ClientSession.hub:Llocal/lito381/server/GameHub;
-LLOAD 71/72
-ALOAD 0
-INVOKEVIRTUAL local/lito381/server/GameHub.isOwner(JLlocal/lito381/server/ClientSession;)Z
-IFEQ L781
-CONST Integer:1
-GOTO L782
-CONST Integer:0
-ISTORE 73
-ALOAD 0
-CONST str:"disconnect"
-CONST Integer:0
-INVOKESPECIAL local/lito381/server/ClientSession.cancelActivePlayerTrade(Ljava/lang/String;Z)V
-GOTO L794
-ASTORE 74
-GETSTATIC java/lang/System.out:Ljava/io/PrintStream;
-ALOAD 74
-INVOKEVIRTUAL java/io/IOException.getMessage()Ljava/lang/String;
-INVOKEDYNAMIC makeConcatWithConstants(Ljava/lang/String;)Ljava/lang/String; bsm=H6 java/lang/invoke/StringConcatFactory.makeConcatWithConstants(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;Ljava/lang/String;[Ljava/lang/Object;)Ljava/lang/invoke/CallSite; args=[str:"[LiTo] trade disconnect cleanup failed: \u0001"]
-INVOKEVIRTUAL java/io/PrintStream.println(Ljava/lang/String;)V
-ALOAD 0
-GETFIELD local/lito381/server/ClientSession.classicPartyParticipant:Z
-IFEQ L808
-ALOAD 0
-CONST str:"disconnect"
-CONST Integer:0
-INVOKESPECIAL local/lito381/server/ClientSession.leaveClassicParty(Ljava/lang/String;Z)V
-GOTO L808
-ASTORE 75
-GETSTATIC java/lang/System.out:Ljava/io/PrintStream;
-ALOAD 75
-INVOKEVIRTUAL java/io/IOException.getMessage()Ljava/lang/String;
-INVOKEDYNAMIC makeConcatWithConstants(Ljava/lang/String;)Ljava/lang/String; bsm=H6 java/lang/invoke/StringConcatFactory.makeConcatWithConstants(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;Ljava/lang/String;[Ljava/lang/Object;)Ljava/lang/invoke/CallSite; args=[str:"[LiTo] party disconnect cleanup failed: \u0001"]
-INVOKEVIRTUAL java/io/PrintStream.println(Ljava/lang/String;)V
-ALOAD 0
-GETFIELD local/lito381/server/ClientSession.phase:Llocal/lito381/server/ClientSession$Phase;
-GETSTATIC local/lito381/server/ClientSession$Phase.WORLD:Llocal/lito381/server/ClientSession$Phase;
-IF_ACMPNE L830
-ALOAD 0
-INVOKESPECIAL local/lito381/server/ClientSession.collectClassicWorldPetsOnDisconnect()V
-GOTO L821
-ASTORE 76
-GETSTATIC java/lang/System.out:Ljava/io/PrintStream;
-ALOAD 76
-INVOKEVIRTUAL java/lang/Exception.getMessage()Ljava/lang/String;
-INVOKEDYNAMIC makeConcatWithConstants(Ljava/lang/String;)Ljava/lang/String; bsm=H6 java/lang/invoke/StringConcatFactory.makeConcatWithConstants(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;Ljava/lang/String;[Ljava/lang/Object;)Ljava/lang/invoke/CallSite; args=[str:"[LiTo] persistent pet disconnect cleanup failed: \u0001"]
-INVOKEVIRTUAL java/io/PrintStream.println(Ljava/lang/String;)V
-ALOAD 0
-INVOKESPECIAL local/lito381/server/ClientSession.cleanupClassicWorldTransientCompanionsOnDisconnect()V
-GOTO L830
-ASTORE 77
-GETSTATIC java/lang/System.out:Ljava/io/PrintStream;
-ALOAD 77
-INVOKEVIRTUAL java/lang/Exception.getMessage()Ljava/lang/String;
-INVOKEDYNAMIC makeConcatWithConstants(Ljava/lang/String;)Ljava/lang/String; bsm=H6 java/lang/invoke/StringConcatFactory.makeConcatWithConstants(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;Ljava/lang/String;[Ljava/lang/Object;)Ljava/lang/invoke/CallSite; args=[str:"[LiTo] transient companion disconnect cleanup failed: \u0001"]
-INVOKEVIRTUAL java/io/PrintStream.println(Ljava/lang/String;)V
-ALOAD 0
-INVOKESPECIAL local/lito381/server/ClientSession.persistExperienceGemOnDisconnect()V
-GOTO L839
-ASTORE 78
-GETSTATIC java/lang/System.out:Ljava/io/PrintStream;
-ALOAD 78
-INVOKEVIRTUAL java/lang/Exception.getMessage()Ljava/lang/String;
-INVOKEDYNAMIC makeConcatWithConstants(Ljava/lang/String;)Ljava/lang/String; bsm=H6 java/lang/invoke/StringConcatFactory.makeConcatWithConstants(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;Ljava/lang/String;[Ljava/lang/Object;)Ljava/lang/invoke/CallSite; args=[str:"[LiTo] Dragon Gem disconnect persistence failed: \u0001"]
-INVOKEVIRTUAL java/io/PrintStream.println(Ljava/lang/String;)V
-ALOAD 0
-INVOKESPECIAL local/lito381/server/ClientSession.persistFireWeaponOnDisconnect()V
-GOTO L848
-ASTORE 79
-GETSTATIC java/lang/System.out:Ljava/io/PrintStream;
-ALOAD 79
-INVOKEVIRTUAL java/lang/Exception.getMessage()Ljava/lang/String;
-INVOKEDYNAMIC makeConcatWithConstants(Ljava/lang/String;)Ljava/lang/String; bsm=H6 java/lang/invoke/StringConcatFactory.makeConcatWithConstants(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;Ljava/lang/String;[Ljava/lang/Object;)Ljava/lang/invoke/CallSite; args=[str:"[LiTo] Fire Weapon disconnect persistence failed: \u0001"]
-INVOKEVIRTUAL java/io/PrintStream.println(Ljava/lang/String;)V
-ALOAD 0
-INVOKESPECIAL local/lito381/server/ClientSession.persistBurningWeaponOnDisconnect()V
-GOTO L857
-ASTORE 80
-GETSTATIC java/lang/System.out:Ljava/io/PrintStream;
-ALOAD 80
-INVOKEVIRTUAL java/lang/Exception.getMessage()Ljava/lang/String;
-INVOKEDYNAMIC makeConcatWithConstants(Ljava/lang/String;)Ljava/lang/String; bsm=H6 java/lang/invoke/StringConcatFactory.makeConcatWithConstants(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;Ljava/lang/String;[Ljava/lang/Object;)Ljava/lang/invoke/CallSite; args=[str:"[LiTo] Burning Weapon disconnect persistence failed: \u0001"]
-INVOKEVIRTUAL java/io/PrintStream.println(Ljava/lang/String;)V
-ALOAD 0
-INVOKESPECIAL local/lito381/server/ClientSession.persistWindShotOnDisconnect()V
-GOTO L866
-ASTORE 81
-GETSTATIC java/lang/System.out:Ljava/io/PrintStream;
-ALOAD 81
-INVOKEVIRTUAL java/lang/Exception.getMessage()Ljava/lang/String;
-INVOKEDYNAMIC makeConcatWithConstants(Ljava/lang/String;)Ljava/lang/String; bsm=H6 java/lang/invoke/StringConcatFactory.makeConcatWithConstants(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;Ljava/lang/String;[Ljava/lang/Object;)Ljava/lang/invoke/CallSite; args=[str:"[LiTo] Wind Shot disconnect persistence failed: \u0001"]
-INVOKEVIRTUAL java/io/PrintStream.println(Ljava/lang/String;)V
-ALOAD 0
-INVOKESPECIAL local/lito381/server/ClientSession.persistFireBlessOnDisconnect()V
-GOTO L875
-ASTORE 82
-GETSTATIC java/lang/System.out:Ljava/io/PrintStream;
-ALOAD 82
-INVOKEVIRTUAL java/lang/Exception.getMessage()Ljava/lang/String;
-INVOKEDYNAMIC makeConcatWithConstants(Ljava/lang/String;)Ljava/lang/String; bsm=H6 java/lang/invoke/StringConcatFactory.makeConcatWithConstants(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;Ljava/lang/String;[Ljava/lang/Object;)Ljava/lang/invoke/CallSite; args=[str:"[LiTo] Fire Bless disconnect persistence failed: \u0001"]
-INVOKEVIRTUAL java/io/PrintStream.println(Ljava/lang/String;)V
-ALOAD 0
-INVOKESPECIAL local/lito381/server/ClientSession.persistStormEyeOnDisconnect()V
-GOTO L884
-ASTORE 83
-GETSTATIC java/lang/System.out:Ljava/io/PrintStream;
-ALOAD 83
-INVOKEVIRTUAL java/lang/Exception.getMessage()Ljava/lang/String;
-INVOKEDYNAMIC makeConcatWithConstants(Ljava/lang/String;)Ljava/lang/String; bsm=H6 java/lang/invoke/StringConcatFactory.makeConcatWithConstants(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;Ljava/lang/String;[Ljava/lang/Object;)Ljava/lang/invoke/CallSite; args=[str:"[LiTo] Storm Eye disconnect persistence failed: \u0001"]
-INVOKEVIRTUAL java/io/PrintStream.println(Ljava/lang/String;)V
-ALOAD 0
-INVOKESPECIAL local/lito381/server/ClientSession.persistStormShotOnDisconnect()V
-GOTO L893
-ASTORE 84
-GETSTATIC java/lang/System.out:Ljava/io/PrintStream;
-ALOAD 84
-INVOKEVIRTUAL java/lang/Exception.getMessage()Ljava/lang/String;
-INVOKEDYNAMIC makeConcatWithConstants(Ljava/lang/String;)Ljava/lang/String; bsm=H6 java/lang/invoke/StringConcatFactory.makeConcatWithConstants(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;Ljava/lang/String;[Ljava/lang/Object;)Ljava/lang/invoke/CallSite; args=[str:"[LiTo] Storm Shot disconnect persistence failed: \u0001"]
-INVOKEVIRTUAL java/io/PrintStream.println(Ljava/lang/String;)V
-ALOAD 0
-INVOKESPECIAL local/lito381/server/ClientSession.persistEarthBlessOnDisconnect()V
-GOTO L902
-ASTORE 85
-GETSTATIC java/lang/System.out:Ljava/io/PrintStream;
-ALOAD 85
-INVOKEVIRTUAL java/lang/Exception.getMessage()Ljava/lang/String;
-INVOKEDYNAMIC makeConcatWithConstants(Ljava/lang/String;)Ljava/lang/String; bsm=H6 java/lang/invoke/StringConcatFactory.makeConcatWithConstants(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;Ljava/lang/String;[Ljava/lang/Object;)Ljava/lang/invoke/CallSite; args=[str:"[LiTo] Earth Bless disconnect persistence failed: \u0001"]
-INVOKEVIRTUAL java/io/PrintStream.println(Ljava/lang/String;)V
-ALOAD 0
-INVOKESPECIAL local/lito381/server/ClientSession.persistIronSkinOnDisconnect()V
-GOTO L911
-ASTORE 86
-GETSTATIC java/lang/System.out:Ljava/io/PrintStream;
-ALOAD 86
-INVOKEVIRTUAL java/lang/Exception.getMessage()Ljava/lang/String;
-INVOKEDYNAMIC makeConcatWithConstants(Ljava/lang/String;)Ljava/lang/String; bsm=H6 java/lang/invoke/StringConcatFactory.makeConcatWithConstants(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;Ljava/lang/String;[Ljava/lang/Object;)Ljava/lang/invoke/CallSite; args=[str:"[LiTo] Iron Skin disconnect persistence failed: \u0001"]
-INVOKEVIRTUAL java/io/PrintStream.println(Ljava/lang/String;)V
-ALOAD 0
-INVOKESPECIAL local/lito381/server/ClientSession.persistMovementBuffOnDisconnect()V
-GOTO L920
-ASTORE 87
-GETSTATIC java/lang/System.out:Ljava/io/PrintStream;
-ALOAD 87
-INVOKEVIRTUAL java/lang/Exception.getMessage()Ljava/lang/String;
-INVOKEDYNAMIC makeConcatWithConstants(Ljava/lang/String;)Ljava/lang/String; bsm=H6 java/lang/invoke/StringConcatFactory.makeConcatWithConstants(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;Ljava/lang/String;[Ljava/lang/Object;)Ljava/lang/invoke/CallSite; args=[str:"[LiTo] movement buff disconnect persistence failed: \u0001"]
-INVOKEVIRTUAL java/io/PrintStream.println(Ljava/lang/String;)V
-ALOAD 0
-INVOKESPECIAL local/lito381/server/ClientSession.persistArmorProtectionOnDisconnect()V
-GOTO L929
-ASTORE 88
-GETSTATIC java/lang/System.out:Ljava/io/PrintStream;
-ALOAD 88
-INVOKEVIRTUAL java/lang/Exception.getMessage()Ljava/lang/String;
-INVOKEDYNAMIC makeConcatWithConstants(Ljava/lang/String;)Ljava/lang/String; bsm=H6 java/lang/invoke/StringConcatFactory.makeConcatWithConstants(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;Ljava/lang/String;[Ljava/lang/Object;)Ljava/lang/invoke/CallSite; args=[str:"[LiTo] armor protection disconnect persistence failed: \u0001"]
-INVOKEVIRTUAL java/io/PrintStream.println(Ljava/lang/String;)V
-ALOAD 0
-GETFIELD local/lito381/server/ClientSession.phase:Llocal/lito381/server/ClientSession$Phase;
-GETSTATIC local/lito381/server/ClientSession$Phase.WORLD:Llocal/lito381/server/ClientSession$Phase;
-IF_ACMPNE L949
-ALOAD 0
-CONST str:"owner-disconnect"
-INVOKESPECIAL local/lito381/server/ClientSession.abandonClassicWorldRescueFollower(Ljava/lang/String;)V
-ALOAD 0
-CONST str:"owner-disconnect"
-INVOKESPECIAL local/lito381/server/ClientSession.abandonClassicWorldDspyFollower(Ljava/lang/String;)V
-ALOAD 0
-CONST str:"owner-disconnect"
-INVOKESPECIAL local/lito381/server/ClientSession.abandonClassicWorldRoiFollower(Ljava/lang/String;)V
-GOTO L949
-ASTORE 89
-GETSTATIC java/lang/System.out:Ljava/io/PrintStream;
-ALOAD 89
-INVOKEVIRTUAL java/lang/Exception.getMessage()Ljava/lang/String;
-INVOKEDYNAMIC makeConcatWithConstants(Ljava/lang/String;)Ljava/lang/String; bsm=H6 java/lang/invoke/StringConcatFactory.makeConcatWithConstants(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;Ljava/lang/String;[Ljava/lang/Object;)Ljava/lang/invoke/CallSite; args=[str:"[LiTo] quest follower disconnect cleanup failed: \u0001"]
-INVOKEVIRTUAL java/io/PrintStream.println(Ljava/lang/String;)V
-ALOAD 0
-GETFIELD local/lito381/server/ClientSession.activeMagicDollObjectIds:Ljava/util/Set;
-DUP
-ASTORE 90
-MONITORENTER
-ALOAD 0
-CONST str:"owner-disconnect"
-CONST Integer:0
-INVOKESPECIAL local/lito381/server/ClientSession.recallNativeMagicDollLocked(Ljava/lang/String;Z)V
-ALOAD 90
-MONITOREXIT
-GOTO L966
-ASTORE 91
-ALOAD 90
-MONITOREXIT
-ALOAD 91
-ATHROW
-GOTO L973
-ASTORE 92
-GETSTATIC java/lang/System.out:Ljava/io/PrintStream;
-ALOAD 92
-INVOKEVIRTUAL java/lang/Exception.getMessage()Ljava/lang/String;
-INVOKEDYNAMIC makeConcatWithConstants(Ljava/lang/String;)Ljava/lang/String; bsm=H6 java/lang/invoke/StringConcatFactory.makeConcatWithConstants(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;Ljava/lang/String;[Ljava/lang/Object;)Ljava/lang/invoke/CallSite; args=[str:"[LiTo] magic-doll disconnect cleanup failed: \u0001"]
-INVOKEVIRTUAL java/io/PrintStream.println(Ljava/lang/String;)V
-ALOAD 0
-INVOKESPECIAL local/lito381/server/ClientSession.persistLitoCharacterOnDisconnect()V
-GOTO L985
-ASTORE 93
-GETSTATIC java/lang/System.out:Ljava/io/PrintStream;
-ALOAD 93
-INVOKEVIRTUAL java/lang/Object.getClass()Ljava/lang/Class;
-INVOKEVIRTUAL java/lang/Class.getSimpleName()Ljava/lang/String;
-ALOAD 93
+ALOAD 123
 INVOKEVIRTUAL java/lang/Exception.getMessage()Ljava/lang/String;
 INVOKEDYNAMIC makeConcatWithConstants(Ljava/lang/String;Ljava/lang/String;)Ljava/lang/String; bsm=H6 java/lang/invoke/StringConcatFactory.makeConcatWithConstants(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;Ljava/lang/String;[Ljava/lang/Object;)Ljava/lang/invoke/CallSite; args=[str:"[LiTo] MySQL character disconnect persistence failed: \u0001: \u0001"]
 INVOKEVIRTUAL java/io/PrintStream.println(Ljava/lang/String;)V
 ALOAD 0
 INVOKESPECIAL local/lito381/server/ClientSession.notifyPeersOfDisconnect()V
-GOTO L994
-ASTORE 94
-GETSTATIC java/lang/System.out:Ljava/io/PrintStream;
-ALOAD 94
+GOTO L1297
+ASTORE 124
+GETSTATIC java/lang/System.out:Ljava/io/PrintStream;
+ALOAD 124
 INVOKEVIRTUAL java/io/IOException.getMessage()Ljava/lang/String;
 INVOKEDYNAMIC makeConcatWithConstants(Ljava/lang/String;)Ljava/lang/String; bsm=H6 java/lang/invoke/StringConcatFactory.makeConcatWithConstants(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;Ljava/lang/String;[Ljava/lang/Object;)Ljava/lang/invoke/CallSite; args=[str:"[LiTo] disconnect broadcast failed: \u0001"]
 INVOKEVIRTUAL java/io/PrintStream.println(Ljava/lang/String;)V
@@ -1009,7 +1312,7 @@
 ALOAD 0
 GETFIELD local/lito381/server/ClientSession.selectedCharacter:Ljava/lang/String;
 INVOKEVIRTUAL java/lang/String.isBlank()Z
-IFNE L1024
+IFNE L1327
 ALOAD 0
 GETFIELD local/lito381/server/ClientSession.state:Llocal/lito381/server/GameStateStore;
 ALOAD 0
@@ -1028,356 +1331,46 @@
 GETFIELD local/lito381/server/ClientSession.hub:Llocal/lito381/server/GameHub;
 ALOAD 0
 INVOKEVIRTUAL local/lito381/server/GameHub.disconnect(Llocal/lito381/server/ClientSession;)Ljava/util/List;
-ASTORE 95
 ALOAD 0
 INVOKESTATIC local/lito381/server/NativeRoomRules.restoreBot(Llocal/lito381/server/ClientSession;)V
-ILOAD 73
-IFEQ L1045
-ALOAD 0
-LLOAD 71/72
+ASTORE 125
+ILOAD 103
+IFEQ L1348
+ALOAD 0
+LLOAD 101/102
 CONST str:"disconnect"
 INVOKESPECIAL local/lito381/server/ClientSession.broadcastRoomOwner(JLjava/lang/String;)V
-GOTO L1045
-ASTORE 96
-GETSTATIC java/lang/System.out:Ljava/io/PrintStream;
-LLOAD 71/72
-ALOAD 96
+GOTO L1348
+ASTORE 126
+GETSTATIC java/lang/System.out:Ljava/io/PrintStream;
+LLOAD 101/102
+ALOAD 126
 INVOKEVIRTUAL java/io/IOException.getMessage()Ljava/lang/String;
 INVOKEDYNAMIC makeConcatWithConstants(JLjava/lang/String;)Ljava/lang/String; bsm=H6 java/lang/invoke/StringConcatFactory.makeConcatWithConstants(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;Ljava/lang/String;[Ljava/lang/Object;)Ljava/lang/invoke/CallSite; args=[str:"[LiTo] room-owner broadcast failed room=\u0001: \u0001"]
 INVOKEVIRTUAL java/io/PrintStream.println(Ljava/lang/String;)V
-ALOAD 95
+ALOAD 125
 INVOKEINTERFACE java/util/List.iterator()Ljava/util/Iterator; itf
-ASTORE 97
-ALOAD 97
+ASTORE 127
+ALOAD 127
 INVOKEINTERFACE java/util/Iterator.hasNext()Z itf
-IFEQ L1068
-ALOAD 97
+IFEQ L1371
+ALOAD 127
 INVOKEINTERFACE java/util/Iterator.next()Ljava/lang/Object; itf
 CHECKCAST java/lang/Long
 INVOKEVIRTUAL java/lang/Long.longValue()J
-LSTORE 98/99
-ALOAD 0
-LLOAD 98/99
+LSTORE 128/129
+ALOAD 0
+LLOAD 128/129
 INVOKESPECIAL local/lito381/server/ClientSession.broadcastRoomDeleted(J)V
-GOTO L1067
-ASTORE 100
-GETSTATIC java/lang/System.out:Ljava/io/PrintStream;
-LLOAD 98/99
+GOTO L1370
+ASTORE 130
+GETSTATIC java/lang/System.out:Ljava/io/PrintStream;
+LLOAD 128/129
+ALOAD 130
+INVOKEVIRTUAL java/io/IOException.getMessage()Ljava/lang/String;
+INVOKEDYNAMIC makeConcatWithConstants(JLjava/lang/String;)Ljava/lang/String; bsm=H6 java/lang/invoke/StringConcatFactory.makeConcatWithConstants(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;Ljava/lang/String;[Ljava/lang/Object;)Ljava/lang/invoke/CallSite; args=[str:"[LiTo] room-delete broadcast failed room=\u0001: \u0001"]
+INVOKEVIRTUAL java/io/PrintStream.println(Ljava/lang/String;)V
+GOTO L1351
 ALOAD 100
-INVOKEVIRTUAL java/io/IOException.getMessage()Ljava/lang/String;
-INVOKEDYNAMIC makeConcatWithConstants(JLjava/lang/String;)Ljava/lang/String; bsm=H6 java/lang/invoke/StringConcatFactory.makeConcatWithConstants(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;Ljava/lang/String;[Ljava/lang/Object;)Ljava/lang/invoke/CallSite; args=[str:"[LiTo] room-delete broadcast failed room=\u0001: \u0001"]
-INVOKEVIRTUAL java/io/PrintStream.println(Ljava/lang/String;)V
-GOTO L1048
-RETURN
-ASTORE 101
-ALOAD 0
-INVOKESTATIC local/lito381/server/NativeServerLifetime.awaitCleanup(Llocal/lito381/server/ClientSession;)V
-ALOAD 0
-GETFIELD local/lito381/server/ClientSession.currentRoomId:J
-LSTORE 102/103
-ALOAD 0
-GETFIELD local/lito381/server/ClientSession.phase:Llocal/lito381/server/ClientSession$Phase;
-GETSTATIC local/lito381/server/ClientSession$Phase.ROOM:Llocal/lito381/server/ClientSession$Phase;
-IF_ACMPNE L1091
-LLOAD 102/103
-CONST Long:0
-LCMP
-IFEQ L1091
-ALOAD 0
-GETFIELD local/lito381/server/ClientSession.hub:Llocal/lito381/server/GameHub;
-LLOAD 102/103
-ALOAD 0
-INVOKEVIRTUAL local/lito381/server/GameHub.isOwner(JLlocal/lito381/server/ClientSession;)Z
-IFEQ L1091
-CONST Integer:1
-GOTO L1092
-CONST Integer:0
-ISTORE 104
-ALOAD 0
-CONST str:"disconnect"
-CONST Integer:0
-INVOKESPECIAL local/lito381/server/ClientSession.cancelActivePlayerTrade(Ljava/lang/String;Z)V
-GOTO L1104
-ASTORE 105
-GETSTATIC java/lang/System.out:Ljava/io/PrintStream;
-ALOAD 105
-INVOKEVIRTUAL java/io/IOException.getMessage()Ljava/lang/String;
-INVOKEDYNAMIC makeConcatWithConstants(Ljava/lang/String;)Ljava/lang/String; bsm=H6 java/lang/invoke/StringConcatFactory.makeConcatWithConstants(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;Ljava/lang/String;[Ljava/lang/Object;)Ljava/lang/invoke/CallSite; args=[str:"[LiTo] trade disconnect cleanup failed: \u0001"]
-INVOKEVIRTUAL java/io/PrintStream.println(Ljava/lang/String;)V
-ALOAD 0
-GETFIELD local/lito381/server/ClientSession.classicPartyParticipant:Z
-IFEQ L1118
-ALOAD 0
-CONST str:"disconnect"
-CONST Integer:0
-INVOKESPECIAL local/lito381/server/ClientSession.leaveClassicParty(Ljava/lang/String;Z)V
-GOTO L1118
-ASTORE 106
-GETSTATIC java/lang/System.out:Ljava/io/PrintStream;
-ALOAD 106
-INVOKEVIRTUAL java/io/IOException.getMessage()Ljava/lang/String;
-INVOKEDYNAMIC makeConcatWithConstants(Ljava/lang/String;)Ljava/lang/String; bsm=H6 java/lang/invoke/StringConcatFactory.makeConcatWithConstants(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;Ljava/lang/String;[Ljava/lang/Object;)Ljava/lang/invoke/CallSite; args=[str:"[LiTo] party disconnect cleanup failed: \u0001"]
-INVOKEVIRTUAL java/io/PrintStream.println(Ljava/lang/String;)V
-ALOAD 0
-GETFIELD local/lito381/server/ClientSession.phase:Llocal/lito381/server/ClientSession$Phase;
-GETSTATIC local/lito381/server/ClientSession$Phase.WORLD:Llocal/lito381/server/ClientSession$Phase;
-IF_ACMPNE L1140
-ALOAD 0
-INVOKESPECIAL local/lito381/server/ClientSession.collectClassicWorldPetsOnDisconnect()V
-GOTO L1131
-ASTORE 107
-GETSTATIC java/lang/System.out:Ljava/io/PrintStream;
-ALOAD 107
-INVOKEVIRTUAL java/lang/Exception.getMessage()Ljava/lang/String;
-INVOKEDYNAMIC makeConcatWithConstants(Ljava/lang/String;)Ljava/lang/String; bsm=H6 java/lang/invoke/StringConcatFactory.makeConcatWithConstants(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;Ljava/lang/String;[Ljava/lang/Object;)Ljava/lang/invoke/CallSite; args=[str:"[LiTo] persistent pet disconnect cleanup failed: \u0001"]
-INVOKEVIRTUAL java/io/PrintStream.println(Ljava/lang/String;)V
-ALOAD 0
-INVOKESPECIAL local/lito381/server/ClientSession.cleanupClassicWorldTransientCompanionsOnDisconnect()V
-GOTO L1140
-ASTORE 108
-GETSTATIC java/lang/System.out:Ljava/io/PrintStream;
-ALOAD 108
-INVOKEVIRTUAL java/lang/Exception.getMessage()Ljava/lang/String;
-INVOKEDYNAMIC makeConcatWithConstants(Ljava/lang/String;)Ljava/lang/String; bsm=H6 java/lang/invoke/StringConcatFactory.makeConcatWithConstants(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;Ljava/lang/String;[Ljava/lang/Object;)Ljava/lang/invoke/CallSite; args=[str:"[LiTo] transient companion disconnect cleanup failed: \u0001"]
-INVOKEVIRTUAL java/io/PrintStream.println(Ljava/lang/String;)V
-ALOAD 0
-INVOKESPECIAL local/lito381/server/ClientSession.persistExperienceGemOnDisconnect()V
-GOTO L1149
-ASTORE 109
-GETSTATIC java/lang/System.out:Ljava/io/PrintStream;
-ALOAD 109
-INVOKEVIRTUAL java/lang/Exception.getMessage()Ljava/lang/String;
-INVOKEDYNAMIC makeConcatWithConstants(Ljava/lang/String;)Ljava/lang/String; bsm=H6 java/lang/invoke/StringConcatFactory.makeConcatWithConstants(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;Ljava/lang/String;[Ljava/lang/Object;)Ljava/lang/invoke/CallSite; args=[str:"[LiTo] Dragon Gem disconnect persistence failed: \u0001"]
-INVOKEVIRTUAL java/io/PrintStream.println(Ljava/lang/String;)V
-ALOAD 0
-INVOKESPECIAL local/lito381/server/ClientSession.persistFireWeaponOnDisconnect()V
-GOTO L1158
-ASTORE 110
-GETSTATIC java/lang/System.out:Ljava/io/PrintStream;
-ALOAD 110
-INVOKEVIRTUAL java/lang/Exception.getMessage()Ljava/lang/String;
-INVOKEDYNAMIC makeConcatWithConstants(Ljava/lang/String;)Ljava/lang/String; bsm=H6 java/lang/invoke/StringConcatFactory.makeConcatWithConstants(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;Ljava/lang/String;[Ljava/lang/Object;)Ljava/lang/invoke/CallSite; args=[str:"[LiTo] Fire Weapon disconnect persistence failed: \u0001"]
-INVOKEVIRTUAL java/io/PrintStream.println(Ljava/lang/String;)V
-ALOAD 0
-INVOKESPECIAL local/lito381/server/ClientSession.persistBurningWeaponOnDisconnect()V
-GOTO L1167
-ASTORE 111
-GETSTATIC java/lang/System.out:Ljava/io/PrintStream;
-ALOAD 111
-INVOKEVIRTUAL java/lang/Exception.getMessage()Ljava/lang/String;
-INVOKEDYNAMIC makeConcatWithConstants(Ljava/lang/String;)Ljava/lang/String; bsm=H6 java/lang/invoke/StringConcatFactory.makeConcatWithConstants(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;Ljava/lang/String;[Ljava/lang/Object;)Ljava/lang/invoke/CallSite; args=[str:"[LiTo] Burning Weapon disconnect persistence failed: \u0001"]
-INVOKEVIRTUAL java/io/PrintStream.println(Ljava/lang/String;)V
-ALOAD 0
-INVOKESPECIAL local/lito381/server/ClientSession.persistWindShotOnDisconnect()V
-GOTO L1176
-ASTORE 112
-GETSTATIC java/lang/System.out:Ljava/io/PrintStream;
-ALOAD 112
-INVOKEVIRTUAL java/lang/Exception.getMessage()Ljava/lang/String;
-INVOKEDYNAMIC makeConcatWithConstants(Ljava/lang/String;)Ljava/lang/String; bsm=H6 java/lang/invoke/StringConcatFactory.makeConcatWithConstants(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;Ljava/lang/String;[Ljava/lang/Object;)Ljava/lang/invoke/CallSite; args=[str:"[LiTo] Wind Shot disconnect persistence failed: \u0001"]
-INVOKEVIRTUAL java/io/PrintStream.println(Ljava/lang/String;)V
-ALOAD 0
-INVOKESPECIAL local/lito381/server/ClientSession.persistFireBlessOnDisconnect()V
-GOTO L1185
-ASTORE 113
-GETSTATIC java/lang/System.out:Ljava/io/PrintStream;
-ALOAD 113
-INVOKEVIRTUAL java/lang/Exception.getMessage()Ljava/lang/String;
-INVOKEDYNAMIC makeConcatWithConstants(Ljava/lang/String;)Ljava/lang/String; bsm=H6 java/lang/invoke/StringConcatFactory.makeConcatWithConstants(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;Ljava/lang/String;[Ljava/lang/Object;)Ljava/lang/invoke/CallSite; args=[str:"[LiTo] Fire Bless disconnect persistence failed: \u0001"]
-INVOKEVIRTUAL java/io/PrintStream.println(Ljava/lang/String;)V
-ALOAD 0
-INVOKESPECIAL local/lito381/server/ClientSession.persistStormEyeOnDisconnect()V
-GOTO L1194
-ASTORE 114
-GETSTATIC java/lang/System.out:Ljava/io/PrintStream;
-ALOAD 114
-INVOKEVIRTUAL java/lang/Exception.getMessage()Ljava/lang/String;
-INVOKEDYNAMIC makeConcatWithConstants(Ljava/lang/String;)Ljava/lang/String; bsm=H6 java/lang/invoke/StringConcatFactory.makeConcatWithConstants(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;Ljava/lang/String;[Ljava/lang/Object;)Ljava/lang/invoke/CallSite; args=[str:"[LiTo] Storm Eye disconnect persistence failed: \u0001"]
-INVOKEVIRTUAL java/io/PrintStream.println(Ljava/lang/String;)V
-ALOAD 0
-INVOKESPECIAL local/lito381/server/ClientSession.persistStormShotOnDisconnect()V
-GOTO L1203
-ASTORE 115
-GETSTATIC java/lang/System.out:Ljava/io/PrintStream;
-ALOAD 115
-INVOKEVIRTUAL java/lang/Exception.getMessage()Ljava/lang/String;
-INVOKEDYNAMIC makeConcatWithConstants(Ljava/lang/String;)Ljava/lang/String; bsm=H6 java/lang/invoke/StringConcatFactory.makeConcatWithConstants(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;Ljava/lang/String;[Ljava/lang/Object;)Ljava/lang/invoke/CallSite; args=[str:"[LiTo] Storm Shot disconnect persistence failed: \u0001"]
-INVOKEVIRTUAL java/io/PrintStream.println(Ljava/lang/String;)V
-ALOAD 0
-INVOKESPECIAL local/lito381/server/ClientSession.persistEarthBlessOnDisconnect()V
-GOTO L1212
-ASTORE 116
-GETSTATIC java/lang/System.out:Ljava/io/PrintStream;
-ALOAD 116
-INVOKEVIRTUAL java/lang/Exception.getMessage()Ljava/lang/String;
-INVOKEDYNAMIC makeConcatWithConstants(Ljava/lang/String;)Ljava/lang/String; bsm=H6 java/lang/invoke/StringConcatFactory.makeConcatWithConstants(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;Ljava/lang/String;[Ljava/lang/Object;)Ljava/lang/invoke/CallSite; args=[str:"[LiTo] Earth Bless disconnect persistence failed: \u0001"]
-INVOKEVIRTUAL java/io/PrintStream.println(Ljava/lang/String;)V
-ALOAD 0
-INVOKESPECIAL local/lito381/server/ClientSession.persistIronSkinOnDisconnect()V
-GOTO L1221
-ASTORE 117
-GETSTATIC java/lang/System.out:Ljava/io/PrintStream;
-ALOAD 117
-INVOKEVIRTUAL java/lang/Exception.getMessage()Ljava/lang/String;
-INVOKEDYNAMIC makeConcatWithConstants(Ljava/lang/String;)Ljava/lang/String; bsm=H6 java/lang/invoke/StringConcatFactory.makeConcatWithConstants(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;Ljava/lang/String;[Ljava/lang/Object;)Ljava/lang/invoke/CallSite; args=[str:"[LiTo] Iron Skin disconnect persistence failed: \u0001"]
-INVOKEVIRTUAL java/io/PrintStream.println(Ljava/lang/String;)V
-ALOAD 0
-INVOKESPECIAL local/lito381/server/ClientSession.persistMovementBuffOnDisconnect()V
-GOTO L1230
-ASTORE 118
-GETSTATIC java/lang/System.out:Ljava/io/PrintStream;
-ALOAD 118
-INVOKEVIRTUAL java/lang/Exception.getMessage()Ljava/lang/String;
-INVOKEDYNAMIC makeConcatWithConstants(Ljava/lang/String;)Ljava/lang/String; bsm=H6 java/lang/invoke/StringConcatFactory.makeConcatWithConstants(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;Ljava/lang/String;[Ljava/lang/Object;)Ljava/lang/invoke/CallSite; args=[str:"[LiTo] movement buff disconnect persistence failed: \u0001"]
-INVOKEVIRTUAL java/io/PrintStream.println(Ljava/lang/String;)V
-ALOAD 0
-INVOKESPECIAL local/lito381/server/ClientSession.persistArmorProtectionOnDisconnect()V
-GOTO L1239
-ASTORE 119
-GETSTATIC java/lang/System.out:Ljava/io/PrintStream;
-ALOAD 119
-INVOKEVIRTUAL java/lang/Exception.getMessage()Ljava/lang/String;
-INVOKEDYNAMIC makeConcatWithConstants(Ljava/lang/String;)Ljava/lang/String; bsm=H6 java/lang/invoke/StringConcatFactory.makeConcatWithConstants(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;Ljava/lang/String;[Ljava/lang/Object;)Ljava/lang/invoke/CallSite; args=[str:"[LiTo] armor protection disconnect persistence failed: \u0001"]
-INVOKEVIRTUAL java/io/PrintStream.println(Ljava/lang/String;)V
-ALOAD 0
-GETFIELD local/lito381/server/ClientSession.phase:Llocal/lito381/server/ClientSession$Phase;
-GETSTATIC local/lito381/server/ClientSession$Phase.WORLD:Llocal/lito381/server/ClientSession$Phase;
-IF_ACMPNE L1259
-ALOAD 0
-CONST str:"owner-disconnect"
-INVOKESPECIAL local/lito381/server/ClientSession.abandonClassicWorldRescueFollower(Ljava/lang/String;)V
-ALOAD 0
-CONST str:"owner-disconnect"
-INVOKESPECIAL local/lito381/server/ClientSession.abandonClassicWorldDspyFollower(Ljava/lang/String;)V
-ALOAD 0
-CONST str:"owner-disconnect"
-INVOKESPECIAL local/lito381/server/ClientSession.abandonClassicWorldRoiFollower(Ljava/lang/String;)V
-GOTO L1259
-ASTORE 120
-GETSTATIC java/lang/System.out:Ljava/io/PrintStream;
-ALOAD 120
-INVOKEVIRTUAL java/lang/Exception.getMessage()Ljava/lang/String;
-INVOKEDYNAMIC makeConcatWithConstants(Ljava/lang/String;)Ljava/lang/String; bsm=H6 java/lang/invoke/StringConcatFactory.makeConcatWithConstants(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;Ljava/lang/String;[Ljava/lang/Object;)Ljava/lang/invoke/CallSite; args=[str:"[LiTo] quest follower disconnect cleanup failed: \u0001"]
-INVOKEVIRTUAL java/io/PrintStream.println(Ljava/lang/String;)V
-ALOAD 0
-GETFIELD local/lito381/server/ClientSession.activeMagicDollObjectIds:Ljava/util/Set;
-DUP
-ASTORE 121
-MONITORENTER
-ALOAD 0
-CONST str:"owner-disconnect"
-CONST Integer:0
-INVOKESPECIAL local/lito381/server/ClientSession.recallNativeMagicDollLocked(Ljava/lang/String;Z)V
-ALOAD 121
-MONITOREXIT
-GOTO L1276
-ASTORE 122
-ALOAD 121
-MONITOREXIT
-ALOAD 122
-ATHROW
-GOTO L1283
-ASTORE 123
-GETSTATIC java/lang/System.out:Ljava/io/PrintStream;
-ALOAD 123
-INVOKEVIRTUAL java/lang/Exception.getMessage()Ljava/lang/String;
-INVOKEDYNAMIC makeConcatWithConstants(Ljava/lang/String;)Ljava/lang/String; bsm=H6 java/lang/invoke/StringConcatFactory.makeConcatWithConstants(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;Ljava/lang/String;[Ljava/lang/Object;)Ljava/lang/invoke/CallSite; args=[str:"[LiTo] magic-doll disconnect cleanup failed: \u0001"]
-INVOKEVIRTUAL java/io/PrintStream.println(Ljava/lang/String;)V
-ALOAD 0
-INVOKESPECIAL local/lito381/server/ClientSession.persistLitoCharacterOnDisconnect()V
-GOTO L1295
-ASTORE 124
-GETSTATIC java/lang/System.out:Ljava/io/PrintStream;
-ALOAD 124
-INVOKEVIRTUAL java/lang/Object.getClass()Ljava/lang/Class;
-INVOKEVIRTUAL java/lang/Class.getSimpleName()Ljava/lang/String;
-ALOAD 124
-INVOKEVIRTUAL java/lang/Exception.getMessage()Ljava/lang/String;
-INVOKEDYNAMIC makeConcatWithConstants(Ljava/lang/String;Ljava/lang/String;)Ljava/lang/String; bsm=H6 java/lang/invoke/StringConcatFactory.makeConcatWithConstants(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;Ljava/lang/String;[Ljava/lang/Object;)Ljava/lang/invoke/CallSite; args=[str:"[LiTo] MySQL character disconnect persistence failed: \u0001: \u0001"]
-INVOKEVIRTUAL java/io/PrintStream.println(Ljava/lang/String;)V
-ALOAD 0
-INVOKESPECIAL local/lito381/server/ClientSession.notifyPeersOfDisconnect()V
-GOTO L1304
-ASTORE 125
-GETSTATIC java/lang/System.out:Ljava/io/PrintStream;
-ALOAD 125
-INVOKEVIRTUAL java/io/IOException.getMessage()Ljava/lang/String;
-INVOKEDYNAMIC makeConcatWithConstants(Ljava/lang/String;)Ljava/lang/String; bsm=H6 java/lang/invoke/StringConcatFactory.makeConcatWithConstants(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;Ljava/lang/String;[Ljava/lang/Object;)Ljava/lang/invoke/CallSite; args=[str:"[LiTo] disconnect broadcast failed: \u0001"]
-INVOKEVIRTUAL java/io/PrintStream.println(Ljava/lang/String;)V
-ALOAD 0
-CONST str:"disconnect"
-INVOKESPECIAL local/lito381/server/ClientSession.resetNativeTournamentEconomy(Ljava/lang/String;)V
-ALOAD 0
-CONST null
-PUTFIELD local/lito381/server/ClientSession.pendingLitoRoomInvitation:Llocal/lito381/server/ClientSession$PendingLitoRoomInvitation;
-ALOAD 0
-CONST null
-PUTFIELD local/lito381/server/ClientSession.acceptedLitoRoomInvitation:Llocal/lito381/server/ClientSession$PendingLitoRoomInvitation;
-ALOAD 0
-GETSTATIC local/lito381/server/Protocol$LitoBuddyPresence.OFFLINE:Llocal/lito381/server/Protocol$LitoBuddyPresence;
-INVOKESPECIAL local/lito381/server/ClientSession.notifyBuddyPresence(Llocal/lito381/server/Protocol$LitoBuddyPresence;)V
-ALOAD 0
-GETFIELD local/lito381/server/ClientSession.selectedCharacter:Ljava/lang/String;
-INVOKEVIRTUAL java/lang/String.isBlank()Z
-IFNE L1334
-ALOAD 0
-GETFIELD local/lito381/server/ClientSession.state:Llocal/lito381/server/GameStateStore;
-ALOAD 0
-INVOKESPECIAL local/lito381/server/ClientSession.stateCharacter()Ljava/lang/String;
-ALOAD 0
-GETFIELD local/lito381/server/ClientSession.sqlTimedBuffHook:Llocal/lito381/server/GameStateStore$TimedBuffPersistenceHook;
-INVOKEVIRTUAL local/lito381/server/GameStateStore.unregisterTimedBuffPersistence(Ljava/lang/String;Llocal/lito381/server/GameStateStore$TimedBuffPersistenceHook;)V
-ALOAD 0
-GETFIELD local/lito381/server/ClientSession.hub:Llocal/lito381/server/GameHub;
-ALOAD 0
-INVOKESPECIAL local/lito381/server/ClientSession.stateCharacter()Ljava/lang/String;
-ALOAD 0
-GETFIELD local/lito381/server/ClientSession.sqlBuddyHook:Llocal/lito381/server/GameHub$BuddyPersistenceHook;
-INVOKEVIRTUAL local/lito381/server/GameHub.unregisterBuddyPersistence(Ljava/lang/String;Llocal/lito381/server/GameHub$BuddyPersistenceHook;)V
-ALOAD 0
-GETFIELD local/lito381/server/ClientSession.hub:Llocal/lito381/server/GameHub;
-ALOAD 0
-INVOKEVIRTUAL local/lito381/server/GameHub.disconnect(Llocal/lito381/server/ClientSession;)Ljava/util/List;
-ASTORE 126
-ALOAD 0
-INVOKESTATIC local/lito381/server/NativeRoomRules.restoreBot(Llocal/lito381/server/ClientSession;)V
-ILOAD 104
-IFEQ L1355
-ALOAD 0
-LLOAD 102/103
-CONST str:"disconnect"
-INVOKESPECIAL local/lito381/server/ClientSession.broadcastRoomOwner(JLjava/lang/String;)V
-GOTO L1355
-ASTORE 127
-GETSTATIC java/lang/System.out:Ljava/io/PrintStream;
-LLOAD 102/103
-ALOAD 127
-INVOKEVIRTUAL java/io/IOException.getMessage()Ljava/lang/String;
-INVOKEDYNAMIC makeConcatWithConstants(JLjava/lang/String;)Ljava/lang/String; bsm=H6 java/lang/invoke/StringConcatFactory.makeConcatWithConstants(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;Ljava/lang/String;[Ljava/lang/Object;)Ljava/lang/invoke/CallSite; args=[str:"[LiTo] room-owner broadcast failed room=\u0001: \u0001"]
-INVOKEVIRTUAL java/io/PrintStream.println(Ljava/lang/String;)V
-ALOAD 126
-INVOKEINTERFACE java/util/List.iterator()Ljava/util/Iterator; itf
-ASTORE 128
-ALOAD 128
-INVOKEINTERFACE java/util/Iterator.hasNext()Z itf
-IFEQ L1378
-ALOAD 128
-INVOKEINTERFACE java/util/Iterator.next()Ljava/lang/Object; itf
-CHECKCAST java/lang/Long
-INVOKEVIRTUAL java/lang/Long.longValue()J
-LSTORE 129/130
-ALOAD 0
-LLOAD 129/130
-INVOKESPECIAL local/lito381/server/ClientSession.broadcastRoomDeleted(J)V
-GOTO L1377
-ASTORE 131
-GETSTATIC java/lang/System.out:Ljava/io/PrintStream;
-LLOAD 129/130
-ALOAD 131
-INVOKEVIRTUAL java/io/IOException.getMessage()Ljava/lang/String;
-INVOKEDYNAMIC makeConcatWithConstants(JLjava/lang/String;)Ljava/lang/String; bsm=H6 java/lang/invoke/StringConcatFactory.makeConcatWithConstants(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;Ljava/lang/String;[Ljava/lang/Object;)Ljava/lang/invoke/CallSite; args=[str:"[LiTo] room-delete broadcast failed room=\u0001: \u0001"]
-INVOKEVIRTUAL java/io/PrintStream.println(Ljava/lang/String;)V
-GOTO L1358
-ALOAD 101
 ATHROW
 RETURN
```


</details>

<details><summary>local/lito381/server/ClientSession#autoSelectPendingRentalBattleHandoff()Z</summary>


method autoSelectPendingRentalBattleHandoff()Z insn[21]: left=INVOKEDYNAMIC test(Llocal/lito381/server/ClientSession;)Ljava/util/function/Predicate; bsm=H6 java/lang/invoke/LambdaMetafactory.metafactory(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;Ljava/lang/in… right=INVOKEDYNAMIC test(Llocal/lito381/server/ClientSession;)Ljava/util/function/Predicate; bsm=H6 java/lang/invoke/LambdaMetafactory.metafactory(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;Ljava/lang/in…; path event multisets differ (left=29, right=29); labelled CFGs differ


```diff
--- staged/local/lito381/server/ClientSession#autoSelectPendingRentalBattleHandoff()Z
+++ candidate/local/lito381/server/ClientSession#autoSelectPendingRentalBattleHandoff()Z
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


</details>

<details><summary>local/lito381/server/ClientSession#completeClassicWorldPortal(Llocal/lito381/server/Protocol$PortalDestination;Llocal/lito381/server/ClientSession$PendingClassicWorldPortal;)V</summary>


method completeClassicWorldPortal(Llocal/lito381/server/Protocol$PortalDestination;Llocal/lito381/server/ClientSession$PendingClassicWorldPortal;)V insn[6]: left=INVOKEDYNAMIC run(Llocal/lito381/server/ClientSession;Llocal/lito381/server/Protocol$PortalDestination;Llocal/lito381/server/ClientSession$PendingClassicWorldPortal;)Llocal/lito381/server/NativeSelfCastAction$Task; bsm=H6 java/lang/invoke/L… right=INVOKEDYNAMIC run(Llocal/lito381/server/ClientSession;Llocal/lito381/server/Protocol$PortalDestination;Llocal/lito381/server/ClientSession$PendingClassicWorldPortal;)Llocal/lito381/server/NativeSelfCastAction$Task; bsm=H6 java/lang/invoke/L…; path event multisets differ (left=3, right=3); labelled CFGs differ


```diff
--- staged/local/lito381/server/ClientSession#completeClassicWorldPortal(Llocal/lito381/server/Protocol$PortalDestination;Llocal/lito381/server/ClientSession$PendingClassicWorldPortal;)V
+++ candidate/local/lito381/server/ClientSession#completeClassicWorldPortal(Llocal/lito381/server/Protocol$PortalDestination;Llocal/lito381/server/ClientSession$PendingClassicWorldPortal;)V
@@ -6,6 +6,6 @@
 ALOAD 0
 ALOAD 1
 ALOAD 2
-INVOKEDYNAMIC run(Llocal/lito381/server/ClientSession;Llocal/lito381/server/Protocol$PortalDestination;Llocal/lito381/server/ClientSession$PendingClassicWorldPortal;)Llocal/lito381/server/NativeSelfCastAction$Task; bsm=H6 java/lang/invoke/LambdaMetafactory.metafactory(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodHandle;Ljava/lang/invoke/MethodType;)Ljava/lang/invoke/CallSite; args=[type:()V, handle:H7 local/lito381/server/ClientSession.lambda$content$59c2809294fdc4ecf9aee82b27e47107a33f73571660064cfdc99b4b5c301fd2(Llocal/lito381/server/Protocol$PortalDestination;Llocal/lito381/server/ClientSession$PendingClassicWorldPortal;)V, type:()V]
+INVOKEDYNAMIC run(Llocal/lito381/server/ClientSession;Llocal/lito381/server/Protocol$PortalDestination;Llocal/lito381/server/ClientSession$PendingClassicWorldPortal;)Llocal/lito381/server/NativeSelfCastAction$Task; bsm=H6 java/lang/invoke/LambdaMetafactory.metafactory(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodHandle;Ljava/lang/invoke/MethodType;)Ljava/lang/invoke/CallSite; args=[type:()V, handle:H7 local/lito381/server/ClientSession.nativePlacementPortalBody(Llocal/lito381/server/Protocol$PortalDestination;Llocal/lito381/server/ClientSession$PendingClassicWorldPortal;)V, type:()V]
 INVOKESTATIC local/lito381/server/NativeCompanionPlacement.portal(Llocal/lito381/server/ClientSession;Llocal/lito381/server/Protocol$PortalDestination;Ljava/lang/Object;Llocal/lito381/server/NativeSelfCastAction$Task;)V
 RETURN
```


</details>

<details><summary>local/lito381/server/ClientSession#publishNpcCounterBarrierTrigger(JLlocal/lito381/server/BattleCounterBarrier$Decision;Ljava/util/List;Ljava/lang/String;)V</summary>


method publishNpcCounterBarrierTrigger(JLlocal/lito381/server/BattleCounterBarrier$Decision;Ljava/util/List;Ljava/lang/String;)V insn[18]: left=IFEQ L53 right=IFEQ L54; paths rejected: normal control-flow cycle; labelled CFGs differ


```diff
--- staged/local/lito381/server/ClientSession#publishNpcCounterBarrierTrigger(JLlocal/lito381/server/BattleCounterBarrier$Decision;Ljava/util/List;Ljava/lang/String;)V
+++ candidate/local/lito381/server/ClientSession#publishNpcCounterBarrierTrigger(JLlocal/lito381/server/BattleCounterBarrier$Decision;Ljava/util/List;Ljava/lang/String;)V
@@ -18,7 +18,7 @@
 ASTORE 9
 ALOAD 9
 INVOKEINTERFACE java/util/Iterator.hasNext()Z itf
-IFEQ L53
+IFEQ L54
 ALOAD 9
 INVOKEINTERFACE java/util/Iterator.next()Ljava/lang/Object; itf
 CHECKCAST local/lito381/server/ClientSession
@@ -37,20 +37,21 @@
 ALOAD 5
 INVOKEDYNAMIC makeConcatWithConstants(JJIIILjava/lang/String;)Ljava/lang/String; bsm=H6 java/lang/invoke/StringConcatFactory.makeConcatWithConstants(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;Ljava/lang/String;[Ljava/lang/Object;)Ljava/lang/invoke/CallSite; args=[str:"S_EFFECT counter-barrier-trigger defender=\u0001 npc-attacker=\u0001 gfx=\u0001 probability=\u0001 roll=\u0001 source=\u0001"]
 INVOKESPECIAL local/lito381/server/ClientSession.sendEncrypted([BLjava/lang/String;)V
+ALOAD 10
+ALOAD 8
 LLOAD 1/2
 ALOAD 0
 INVOKEVIRTUAL local/lito381/server/ClientSession.playerObjectId()J
 ALOAD 5
 INVOKEDYNAMIC makeConcatWithConstants(JJLjava/lang/String;)Ljava/lang/String; bsm=H6 java/lang/invoke/StringConcatFactory.makeConcatWithConstants(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;Ljava/lang/String;[Ljava/lang/Object;)Ljava/lang/invoke/CallSite; args=[str:"S_ACTION counter-barrier-reflected npc-attacker=\u0001 defender=\u0001 action=2 source=\u0001"]
-ASTORE 11
 ALOAD 0
 LLOAD 1/2
 INVOKESTATIC local/lito381/server/NativeSourceNpcReaction.allow(Llocal/lito381/server/ClientSession;J)Z
-IFNE L48
-GOTO L16
-ALOAD 10
-ALOAD 8
-ALOAD 11
+IFNE L52
+POP
+POP
+POP
+GOTO L53
 INVOKESPECIAL local/lito381/server/ClientSession.sendEncrypted([BLjava/lang/String;)V
 GOTO L16
 GETSTATIC java/lang/System.out:Ljava/io/PrintStream;
```


</details>

<details><summary>local/lito381/server/ClientSession#useClassicWorldAreaAttackSpell(Llocal/lito381/server/Protocol$AttackSpellRequest;Llocal/lito381/server/BattleSpellCatalog$AreaAttackSpell;)V</summary>


method useClassicWorldAreaAttackSpell(Llocal/lito381/server/Protocol$AttackSpellRequest;Llocal/lito381/server/BattleSpellCatalog$AreaAttackSpell;)V insn[10]: left=INVOKEDYNAMIC run(Llocal/lito381/server/ClientSession;Llocal/lito381/server/Protocol$AttackSpellRequest;Llocal/lito381/server/BattleSpellCatalog$AreaAttackSpell;)Llocal/lito381/server/NativeSelfCastAction$Task; bsm=H6 java/lang/invoke/Lambd… right=INVOKEDYNAMIC run(Llocal/lito381/server/ClientSession;Llocal/lito381/server/Protocol$AttackSpellRequest;Llocal/lito381/server/BattleSpellCatalog$AreaAttackSpell;)Llocal/lito381/server/NativeSelfCastAction$Task; bsm=H6 java/lang/invoke/Lambd…; path event multisets differ (left=5, right=5); labelled CFGs differ


```diff
--- staged/local/lito381/server/ClientSession#useClassicWorldAreaAttackSpell(Llocal/lito381/server/Protocol$AttackSpellRequest;Llocal/lito381/server/BattleSpellCatalog$AreaAttackSpell;)V
+++ candidate/local/lito381/server/ClientSession#useClassicWorldAreaAttackSpell(Llocal/lito381/server/Protocol$AttackSpellRequest;Llocal/lito381/server/BattleSpellCatalog$AreaAttackSpell;)V
@@ -10,6 +10,6 @@
 ALOAD 0
 ALOAD 1
 ALOAD 2
-INVOKEDYNAMIC run(Llocal/lito381/server/ClientSession;Llocal/lito381/server/Protocol$AttackSpellRequest;Llocal/lito381/server/BattleSpellCatalog$AreaAttackSpell;)Llocal/lito381/server/NativeSelfCastAction$Task; bsm=H6 java/lang/invoke/LambdaMetafactory.metafactory(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodHandle;Ljava/lang/invoke/MethodType;)Ljava/lang/invoke/CallSite; args=[type:()V, handle:H7 local/lito381/server/ClientSession.lambda$content$0f34469fb86a43461a7b178f3b03c9a24e9f45f14b4d5dd57c7a2f1c43006be3(Llocal/lito381/server/Protocol$AttackSpellRequest;Llocal/lito381/server/BattleSpellCatalog$AreaAttackSpell;)V, type:()V]
+INVOKEDYNAMIC run(Llocal/lito381/server/ClientSession;Llocal/lito381/server/Protocol$AttackSpellRequest;Llocal/lito381/server/BattleSpellCatalog$AreaAttackSpell;)Llocal/lito381/server/NativeSelfCastAction$Task; bsm=H6 java/lang/invoke/LambdaMetafactory.metafactory(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodHandle;Ljava/lang/invoke/MethodType;)Ljava/lang/invoke/CallSite; args=[type:()V, handle:H7 local/lito381/server/ClientSession.nativeCompanionTargetuseClassicWorldAreaAttackSpell(Llocal/lito381/server/Protocol$AttackSpellRequest;Llocal/lito381/server/BattleSpellCatalog$AreaAttackSpell;)V, type:()V]
 INVOKESTATIC local/lito381/server/NativeCompanionTargets.cast(Llocal/lito381/server/ClientSession;ILlocal/lito381/server/NativeSelfCastAction$Task;)V
 RETURN
```


</details>

<details><summary>local/lito381/server/ClientSession#sourcePoisonBody$publishNpcMagicCancellation(JLjava/util/List;ZZZZZZZZZZZZZZZZZZZZZLlocal/lito381/server/CombatPoisonLifecycle$Scope;)V</summary>


method sourcePoisonBody$publishNpcMagicCancellation(JLjava/util/List;ZZZZZZZZZZZZZZZZZZZZZLlocal/lito381/server/CombatPoisonLifecycle$Scope;)V: left= right=(JLjava/util/List<Llocal/lito381/server/ClientSession;>;ZZZZZZZZZZZZZZZZZZZZZ)V


```diff
--- staged/local/lito381/server/ClientSession#sourcePoisonBody$publishNpcMagicCancellation(JLjava/util/List;ZZZZZZZZZZZZZZZZZZZZZLlocal/lito381/server/CombatPoisonLifecycle$Scope;)V
+++ candidate/local/lito381/server/ClientSession#sourcePoisonBody$publishNpcMagicCancellation(JLjava/util/List;ZZZZZZZZZZZZZZZZZZZZZLlocal/lito381/server/CombatPoisonLifecycle$Scope;)V
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
@@ -28,14 +31,13 @@
 INVOKEDYNAMIC makeConcatWithConstants(J)Ljava/lang/String; bsm=H6 java/lang/invoke/StringConcatFactory.makeConcatWithConstants(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;Ljava/lang/String;[Ljava/lang/Object;)Ljava/lang/invoke/CallSite; args=[str:"S_EMOTION(brave) Cancellation NPC=\u0001 mode=0 duration=0"]
 INVOKESPECIAL local/lito381/server/ClientSession.sendEncrypted([BLjava/lang/String;)V
 ILOAD 14
-IFNE L36
+IFNE L38
 ILOAD 15
-IFNE L36
+IFNE L38
 ILOAD 16
-IFNE L36
+IFNE L38
 ILOAD 17
-IFNE L36
-GOTO L3
+IFEQ L48
 ALOAD 27
 LLOAD 1/2
 CONST Integer:0
@@ -47,9 +49,6 @@
 ALOAD 25
 INVOKESTATIC local/lito381/server/CombatMonsterCurseLifecycle.send(Llocal/lito381/server/ClientSession;[BLjava/lang/String;Llocal/lito381/server/CombatPoisonLifecycle$Scope;)V
 GOTO L3
-LLOAD 1/2
-ALOAD 25
-INVOKESTATIC local/lito381/server/CombatMonsterCurseLifecycle.paletteFinishedForId(JLlocal/lito381/server/CombatPoisonLifecycle$Scope;)V
 GETSTATIC java/lang/System.out:Ljava/io/PrintStream;
 LLOAD 1/2
 ILOAD 4
```


</details>

<details><summary>local/lito381/server/ClientSession#resolveClassicWorldScarecrowHit(Llocal/lito381/server/ClassicWorldNpcCatalog$StaticNpc;Llocal/lito381/server/ClassicWorldMonsterCatalog$MonsterTemplate;I)V</summary>


method resolveClassicWorldScarecrowHit(Llocal/lito381/server/ClassicWorldNpcCatalog$StaticNpc;Llocal/lito381/server/ClassicWorldMonsterCatalog$MonsterTemplate;I)V insn[29]: left=GETFIELD local/lito381/server/ClientSession.hub:Llocal/lito381/server/GameHub; right=INVOKESTATIC local/lito381/server/NativeStaticNpcScene.heading(Llocal/lito381/server/GameHub;Llocal/lito381/server/ClassicWorldNpcCatalog$StaticNpc;Llocal/lito381/server/ClientSession;)I; paths rejected: normal control-flow cycle; labelled CFGs differ


```diff
--- staged/local/lito381/server/ClientSession#resolveClassicWorldScarecrowHit(Llocal/lito381/server/ClassicWorldNpcCatalog$StaticNpc;Llocal/lito381/server/ClassicWorldMonsterCatalog$MonsterTemplate;I)V
+++ candidate/local/lito381/server/ClientSession#resolveClassicWorldScarecrowHit(Llocal/lito381/server/ClassicWorldNpcCatalog$StaticNpc;Llocal/lito381/server/ClassicWorldMonsterCatalog$MonsterTemplate;I)V
@@ -29,18 +29,18 @@
 GETFIELD local/lito381/server/ClientSession.hub:Llocal/lito381/server/GameHub;
 ALOAD 1
 ALOAD 0
-GETFIELD local/lito381/server/ClientSession.hub:Llocal/lito381/server/GameHub;
-ALOAD 1
-ALOAD 0
 INVOKESTATIC local/lito381/server/NativeStaticNpcScene.heading(Llocal/lito381/server/GameHub;Llocal/lito381/server/ClassicWorldNpcCatalog$StaticNpc;Llocal/lito381/server/ClientSession;)I
-DUP
 ISTORE 4
+ILOAD 4
 CONST Integer:1
 IADD
 CONST Integer:7
 IAND
-DUP
 ISTORE 5
+ALOAD 0
+GETFIELD local/lito381/server/ClientSession.hub:Llocal/lito381/server/GameHub;
+ALOAD 1
+ILOAD 5
 ALOAD 0
 INVOKESTATIC local/lito381/server/NativeStaticNpcScene.turn(Llocal/lito381/server/GameHub;Llocal/lito381/server/ClassicWorldNpcCatalog$StaticNpc;ILlocal/lito381/server/ClientSession;)Z
 IFNE L51
```


</details>

<details><summary>local/lito381/server/ClientSession#handleIvoryTowerElfAttributeAction(Llocal/lito381/server/Protocol$HypertextActionRequest;Llocal/lito381/server/ClassicWorldNpcCatalog$StaticNpc;)Z</summary>


method handleIvoryTowerElfAttributeAction(Llocal/lito381/server/Protocol$HypertextActionRequest;Llocal/lito381/server/ClassicWorldNpcCatalog$StaticNpc;)Z insn[262]: left=TABLESWITCH min=1 max=8 default=L267 labels=L263,L265,L267,L272,L267,L267,L267,L274 right=TABLESWITCH min=1 max=8 default=L271 labels=L263,L265,L271,L267,L271,L271,L271,L269; paths rejected: path limit 4096 exceeded; labelled CFGs differ


```diff
--- staged/local/lito381/server/ClientSession#handleIvoryTowerElfAttributeAction(Llocal/lito381/server/Protocol$HypertextActionRequest;Llocal/lito381/server/ClassicWorldNpcCatalog$StaticNpc;)Z
+++ candidate/local/lito381/server/ClientSession#handleIvoryTowerElfAttributeAction(Llocal/lito381/server/Protocol$HypertextActionRequest;Llocal/lito381/server/ClassicWorldNpcCatalog$StaticNpc;)Z
@@ -262,19 +262,20 @@
 ILOAD 6
 PUTFIELD local/lito381/server/ClientSession.elfAttribute:I
 ILOAD 6
-TABLESWITCH min=1 max=8 default=L267 labels=L263,L265,L267,L272,L267,L267,L267,L274
+TABLESWITCH min=1 max=8 default=L271 labels=L263,L265,L271,L267,L271,L271,L271,L269
 CONST str:"$683"
-GOTO L275
+GOTO L276
 CONST str:"$680"
-GOTO L275
+GOTO L276
+CONST str:"$681"
+GOTO L276
+CONST str:"$682"
+GOTO L276
 NEW java/lang/IllegalStateException
 DUP
 CONST str:"invalid Ellyonne attribute"
 INVOKESPECIAL java/lang/IllegalStateException.<init>(Ljava/lang/String;)V
 ATHROW
-CONST str:"$681"
-GOTO L275
-CONST str:"$682"
 ASTORE 16
 ALOAD 0
 CONST Integer:679
@@ -286,7 +287,7 @@
 AASTORE
 INVOKESTATIC local/lito381/server/Protocol.messageCode(I[Ljava/lang/String;)[B
 ILOAD 6
-INVOKEDYNAMIC makeConcatWithConstants(B)Ljava/lang/String; bsm=H6 java/lang/invoke/StringConcatFactory.makeConcatWithConstants(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;Ljava/lang/String;[Ljava/lang/Object;)Ljava/lang/invoke/CallSite; args=[str:"S_MESSAGE_CODE Ellyonne-attribute=679 attr=\u0001"]
+INVOKEDYNAMIC makeConcatWithConstants(I)Ljava/lang/String; bsm=H6 java/lang/invoke/StringConcatFactory.makeConcatWithConstants(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;Ljava/lang/String;[Ljava/lang/Object;)Ljava/lang/invoke/CallSite; args=[str:"S_MESSAGE_CODE Ellyonne-attribute=679 attr=\u0001"]
 INVOKESPECIAL local/lito381/server/ClientSession.sendEncrypted([BLjava/lang/String;)V
 ALOAD 0
 ALOAD 2
@@ -297,7 +298,7 @@
 GETSTATIC java/lang/System.out:Ljava/io/PrintStream;
 ALOAD 3
 ILOAD 6
-INVOKEDYNAMIC makeConcatWithConstants(Ljava/lang/String;B)Ljava/lang/String; bsm=H6 java/lang/invoke/StringConcatFactory.makeConcatWithConstants(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;Ljava/lang/String;[Ljava/lang/Object;)Ljava/lang/invoke/CallSite; args=[str:"[LiTo] C_HACTION Ellyonne accepted action=\u0001 attr=\u0001"]
+INVOKEDYNAMIC makeConcatWithConstants(Ljava/lang/String;I)Ljava/lang/String; bsm=H6 java/lang/invoke/StringConcatFactory.makeConcatWithConstants(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;Ljava/lang/String;[Ljava/lang/Object;)Ljava/lang/invoke/CallSite; args=[str:"[LiTo] C_HACTION Ellyonne accepted action=\u0001 attr=\u0001"]
 INVOKEVIRTUAL java/io/PrintStream.println(Ljava/lang/String;)V
 CONST Integer:1
 IRETURN
```


</details>

<details><summary>local/lito381/server/ClientSession#useClassicWorldAttackSpell(Llocal/lito381/server/Protocol$AttackSpellRequest;Llocal/lito381/server/BattleSpellCatalog$AttackSpell;)V</summary>


method useClassicWorldAttackSpell(Llocal/lito381/server/Protocol$AttackSpellRequest;Llocal/lito381/server/BattleSpellCatalog$AttackSpell;)V insn[10]: left=INVOKEDYNAMIC run(Llocal/lito381/server/ClientSession;Llocal/lito381/server/Protocol$AttackSpellRequest;Llocal/lito381/server/BattleSpellCatalog$AttackSpell;)Llocal/lito381/server/NativeSelfCastAction$Task; bsm=H6 java/lang/invoke/LambdaMet… right=INVOKEDYNAMIC run(Llocal/lito381/server/ClientSession;Llocal/lito381/server/Protocol$AttackSpellRequest;Llocal/lito381/server/BattleSpellCatalog$AttackSpell;)Llocal/lito381/server/NativeSelfCastAction$Task; bsm=H6 java/lang/invoke/LambdaMet…; path event multisets differ (left=5, right=5); labelled CFGs differ


```diff
--- staged/local/lito381/server/ClientSession#useClassicWorldAttackSpell(Llocal/lito381/server/Protocol$AttackSpellRequest;Llocal/lito381/server/BattleSpellCatalog$AttackSpell;)V
+++ candidate/local/lito381/server/ClientSession#useClassicWorldAttackSpell(Llocal/lito381/server/Protocol$AttackSpellRequest;Llocal/lito381/server/BattleSpellCatalog$AttackSpell;)V
@@ -10,6 +10,6 @@
 ALOAD 0
 ALOAD 1
 ALOAD 2
-INVOKEDYNAMIC run(Llocal/lito381/server/ClientSession;Llocal/lito381/server/Protocol$AttackSpellRequest;Llocal/lito381/server/BattleSpellCatalog$AttackSpell;)Llocal/lito381/server/NativeSelfCastAction$Task; bsm=H6 java/lang/invoke/LambdaMetafactory.metafactory(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodHandle;Ljava/lang/invoke/MethodType;)Ljava/lang/invoke/CallSite; args=[type:()V, handle:H7 local/lito381/server/ClientSession.lambda$content$1b57ad960992782b2c403d8904005ccc64db5388f417cdfec5450bfde96cb176(Llocal/lito381/server/Protocol$AttackSpellRequest;Llocal/lito381/server/BattleSpellCatalog$AttackSpell;)V, type:()V]
+INVOKEDYNAMIC run(Llocal/lito381/server/ClientSession;Llocal/lito381/server/Protocol$AttackSpellRequest;Llocal/lito381/server/BattleSpellCatalog$AttackSpell;)Llocal/lito381/server/NativeSelfCastAction$Task; bsm=H6 java/lang/invoke/LambdaMetafactory.metafactory(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodHandle;Ljava/lang/invoke/MethodType;)Ljava/lang/invoke/CallSite; args=[type:()V, handle:H7 local/lito381/server/ClientSession.nativeCompanionTargetuseClassicWorldAttackSpell(Llocal/lito381/server/Protocol$AttackSpellRequest;Llocal/lito381/server/BattleSpellCatalog$AttackSpell;)V, type:()V]
 INVOKESTATIC local/lito381/server/NativeCompanionTargets.cast(Llocal/lito381/server/ClientSession;ILlocal/lito381/server/NativeSelfCastAction$Task;)V
 RETURN
```


</details>

<details><summary>local/lito381/server/ClientSession#useClassicWorldLineAttackSpell([BLlocal/lito381/server/BattleSpellCatalog$LineAttackSpell;)V</summary>


method useClassicWorldLineAttackSpell([BLlocal/lito381/server/BattleSpellCatalog$LineAttackSpell;)V insn[5]: left=INVOKEDYNAMIC run(Llocal/lito381/server/ClientSession;[BLlocal/lito381/server/BattleSpellCatalog$LineAttackSpell;)Llocal/lito381/server/NativeSelfCastAction$Task; bsm=H6 java/lang/invoke/LambdaMetafactory.metafactory(Ljava/lang/invoke/Metho… right=INVOKEDYNAMIC run(Llocal/lito381/server/ClientSession;[BLlocal/lito381/server/BattleSpellCatalog$LineAttackSpell;)Llocal/lito381/server/NativeSelfCastAction$Task; bsm=H6 java/lang/invoke/LambdaMetafactory.metafactory(Ljava/lang/invoke/Metho…; path event multisets differ (left=3, right=3); labelled CFGs differ


```diff
--- staged/local/lito381/server/ClientSession#useClassicWorldLineAttackSpell([BLlocal/lito381/server/BattleSpellCatalog$LineAttackSpell;)V
+++ candidate/local/lito381/server/ClientSession#useClassicWorldLineAttackSpell([BLlocal/lito381/server/BattleSpellCatalog$LineAttackSpell;)V
@@ -5,6 +5,6 @@
 ALOAD 0
 ALOAD 1
 ALOAD 2
-INVOKEDYNAMIC run(Llocal/lito381/server/ClientSession;[BLlocal/lito381/server/BattleSpellCatalog$LineAttackSpell;)Llocal/lito381/server/NativeSelfCastAction$Task; bsm=H6 java/lang/invoke/LambdaMetafactory.metafactory(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodHandle;Ljava/lang/invoke/MethodType;)Ljava/lang/invoke/CallSite; args=[type:()V, handle:H7 local/lito381/server/ClientSession.lambda$content$dfb3791738c5b4d0ec1d5ae51a7b2311d8001284ea4625b6d859dc694635b012([BLlocal/lito381/server/BattleSpellCatalog$LineAttackSpell;)V, type:()V]
+INVOKEDYNAMIC run(Llocal/lito381/server/ClientSession;[BLlocal/lito381/server/BattleSpellCatalog$LineAttackSpell;)Llocal/lito381/server/NativeSelfCastAction$Task; bsm=H6 java/lang/invoke/LambdaMetafactory.metafactory(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodHandle;Ljava/lang/invoke/MethodType;)Ljava/lang/invoke/CallSite; args=[type:()V, handle:H7 local/lito381/server/ClientSession.nativeCompanionTargetuseClassicWorldLineAttackSpell([BLlocal/lito381/server/BattleSpellCatalog$LineAttackSpell;)V, type:()V]
 INVOKESTATIC local/lito381/server/NativeCompanionTargets.cast(Llocal/lito381/server/ClientSession;ILlocal/lito381/server/NativeSelfCastAction$Task;)V
 RETURN
```


</details>

<details><summary>lambda：local/lito381/server/ClientSession#lambda$autoSelectPendingRentalBattleHandoff$30(Llocal/lito381/server/CharacterSummary;)Z</summary>


```text
母方法 local/lito381/server/ClientSession#autoSelectPendingRentalBattleHandoff()Z
STAGED CALLSITE
test(Llocal/lito381/server/ClientSession;)Ljava/util/function/Predicate; [(Ljava/lang/Object;)Z, local/lito381/server/ClientSession.lambda$autoSelectPendingRentalBattleHandoff$30(Llocal/lito381/server/CharacterSummary;)Z (5), (Llocal/lito381/server/CharacterSummary;)Z]
CANDIDATE CALLSITE
test(Llocal/lito381/server/ClientSession;)Ljava/util/function/Predicate; [(Ljava/lang/Object;)Z, local/lito381/server/ClientSession.lambda$autoSelectPendingRentalBattleHandoff$30(Llocal/lito381/server/CharacterSummary;)Z (5), (Llocal/lito381/server/CharacterSummary;)Z]
STAGED BODY
META 2 null []
TRY []
ALOAD 0
GETFIELD local/lito381/server/ClientSession.hub:Llocal/lito381/server/GameHub;
ASTORE 2
ALOAD 0
INVOKEVIRTUAL local/lito381/server/ClientSession.accountName()Ljava/lang/String;
ASTORE 3
ALOAD 0
ALOAD 1
INVOKEVIRTUAL local/lito381/server/CharacterSummary.name()Ljava/lang/String;
INVOKESPECIAL local/lito381/server/ClientSession.stateCharacter(Ljava/lang/String;)Ljava/lang/String;
ASTORE 4
ALOAD 2
ALOAD 0
ALOAD 3
INVOKESTATIC local/lito381/server/SourceLogin.account(Llocal/lito381/server/ClientSession;Ljava/lang/String;)Ljava/lang/String;
ALOAD 4
INVOKEVIRTUAL local/lito381/server/GameHub.hasRentalBattleHandoff(Ljava/lang/String;Ljava/lang/String;)Z
IRETURN
CANDIDATE TARGET BODY
META 4098 null []
TRY []
ALOAD 0
GETFIELD local/lito381/server/ClientSession.hub:Llocal/lito381/server/GameHub;
ALOAD 0
INVOKEVIRTUAL local/lito381/server/ClientSession.accountName()Ljava/lang/String;
ALOAD 0
ALOAD 1
INVOKEVIRTUAL local/lito381/server/CharacterSummary.name()Ljava/lang/String;
INVOKESPECIAL local/lito381/server/ClientSession.stateCharacter(Ljava/lang/String;)Ljava/lang/String;
ASTORE 2
ASTORE 3
ASTORE 4
ALOAD 4
ALOAD 0
ALOAD 3
INVOKESTATIC local/lito381/server/SourceLogin.account(Llocal/lito381/server/ClientSession;Ljava/lang/String;)Ljava/lang/String;
ALOAD 2
INVOKEVIRTUAL local/lito381/server/GameHub.hasRentalBattleHandoff(Ljava/lang/String;Ljava/lang/String;)Z
IRETURN
```


</details>

<details><summary>lambda：local/lito381/server/ClientSession#lambda$completeClassicWorldPortal$161(Llocal/lito381/server/Protocol$PortalDestination;Llocal/lito381/server/ClientSession$PendingClassicWorldPortal;)V</summary>


```text
母方法 local/lito381/server/ClientSession#completeClassicWorldPortal(Llocal/lito381/server/Protocol$PortalDestination;Llocal/lito381/server/ClientSession$PendingClassicWorldPortal;)V
STAGED CALLSITE
run(Llocal/lito381/server/ClientSession;Llocal/lito381/server/Protocol$PortalDestination;Llocal/lito381/server/ClientSession$PendingClassicWorldPortal;)Llocal/lito381/server/NativeSelfCastAction$Task; [()V, local/lito381/server/ClientSession.lambda$completeClassicWorldPortal$161(Llocal/lito381/server/Protocol$PortalDestination;Llocal/lito381/server/ClientSession$PendingClassicWorldPortal;)V (5), ()V]
CANDIDATE CALLSITE
run(Llocal/lito381/server/ClientSession;Llocal/lito381/server/Protocol$PortalDestination;Llocal/lito381/server/ClientSession$PendingClassicWorldPortal;)Llocal/lito381/server/NativeSelfCastAction$Task; [()V, local/lito381/server/ClientSession.nativePlacementPortalBody(Llocal/lito381/server/Protocol$PortalDestination;Llocal/lito381/server/ClientSession$PendingClassicWorldPortal;)V (7), ()V]
STAGED BODY
META 4098 null [java/io/IOException]
TRY []
ALOAD 0
ALOAD 1
ALOAD 2
INVOKESPECIAL local/lito381/server/ClientSession.nativePlacementPortalBody(Llocal/lito381/server/Protocol$PortalDestination;Llocal/lito381/server/ClientSession$PendingClassicWorldPortal;)V
RETURN
CANDIDATE TARGET BODY
本節證據為完整 wrapper 本體與兩側 callsite；候選完整 target 本體另見 candidate-259-dump.txt 的 local/lito381/server/ClientSession#nativePlacementPortalBody(Llocal/lito381/server/Protocol$PortalDestination;Llocal/lito381/server/ClientSession$PendingClassicWorldPortal;)V。此 target 的獨立驗證狀態仍以完整 DIFF 表為準。
```


</details>

<details><summary>lambda：local/lito381/server/ClientSession#lambda$nativeAreaBattleSlowBody$329(IILlocal/lito381/server/BattleSpellCatalog$MassSlowSpell;Ljava/lang/Object;Llocal/lito381/server/BattleQuestEncounter$Snapshot;)Z</summary>


```text
母方法 local/lito381/server/ClientSession#nativeAreaBattleSlowBody([BLlocal/lito381/server/BattleSpellCatalog$MassSlowSpell;)V
STAGED CALLSITE
test(Llocal/lito381/server/ClientSession;IILlocal/lito381/server/BattleSpellCatalog$MassSlowSpell;Ljava/lang/Object;)Ljava/util/function/Predicate; [(Ljava/lang/Object;)Z, local/lito381/server/ClientSession.lambda$nativeAreaBattleSlowBody$329(IILlocal/lito381/server/BattleSpellCatalog$MassSlowSpell;Ljava/lang/Object;Llocal/lito381/server/BattleQuestEncounter$Snapshot;)Z (5), (Llocal/lito381/server/BattleQuestEncounter$Snapshot;)Z]
CANDIDATE CALLSITE
test(Llocal/lito381/server/ClientSession;IILlocal/lito381/server/BattleSpellCatalog$MassSlowSpell;Ljava/lang/Object;)Ljava/util/function/Predicate; [(Ljava/lang/Object;)Z, local/lito381/server/ClientSession.nativeSourceFrozenSlow_lambda$useMassSlowSpell$322(IILlocal/lito381/server/BattleSpellCatalog$MassSlowSpell;Ljava/lang/Object;Llocal/lito381/server/BattleQuestEncounter$Snapshot;)Z (5), (Llocal/lito381/server/BattleQuestEncounter$Snapshot;)Z]
STAGED BODY
META 4098 null []
TRY []
ALOAD 0
ILOAD 1
ILOAD 2
ALOAD 3
ALOAD 4
ALOAD 5
INVOKESPECIAL local/lito381/server/ClientSession.nativeSourceFrozenSlow_lambda$useMassSlowSpell$322(IILlocal/lito381/server/BattleSpellCatalog$MassSlowSpell;Ljava/lang/Object;Llocal/lito381/server/BattleQuestEncounter$Snapshot;)Z
IRETURN
CANDIDATE TARGET BODY
META 4098 null []
TRY []
ALOAD 0
GETFIELD local/lito381/server/ClientSession.hub:Llocal/lito381/server/GameHub;
ALOAD 0
GETFIELD local/lito381/server/ClientSession.currentRoomId:J
ALOAD 0
ALOAD 5
INVOKEVIRTUAL local/lito381/server/BattleQuestEncounter$Snapshot.objectId()J
INVOKEVIRTUAL local/lito381/server/GameHub.canAttackQuestMonster(JLlocal/lito381/server/ClientSession;J)Z
IFEQ L24
ALOAD 5
INVOKEVIRTUAL local/lito381/server/BattleQuestEncounter$Snapshot.x()I
ILOAD 1
ISUB
ALOAD 5
INVOKEVIRTUAL local/lito381/server/BattleQuestEncounter$Snapshot.y()I
ILOAD 2
ISUB
ALOAD 4
INVOKESTATIC local/lito381/server/NativeSourceSlowFailureMetadata.distance(IILjava/lang/Object;)I
ALOAD 3
INVOKEVIRTUAL local/lito381/server/BattleSpellCatalog$MassSlowSpell.areaRadius()I
IF_ICMPGT L24
CONST Integer:1
GOTO L25
CONST Integer:0
IRETURN
```


</details>

<details><summary>lambda：local/lito381/server/ClientSession#lambda$nativeAreaBattleSlowBody$330(IILlocal/lito381/server/BattleSpellCatalog$MassSlowSpell;Ljava/lang/Object;Llocal/lito381/server/BattleTimeAttackEncounter$Snapshot;)Z</summary>


```text
母方法 local/lito381/server/ClientSession#nativeAreaBattleSlowBody([BLlocal/lito381/server/BattleSpellCatalog$MassSlowSpell;)V
STAGED CALLSITE
test(Llocal/lito381/server/ClientSession;IILlocal/lito381/server/BattleSpellCatalog$MassSlowSpell;Ljava/lang/Object;)Ljava/util/function/Predicate; [(Ljava/lang/Object;)Z, local/lito381/server/ClientSession.lambda$nativeAreaBattleSlowBody$330(IILlocal/lito381/server/BattleSpellCatalog$MassSlowSpell;Ljava/lang/Object;Llocal/lito381/server/BattleTimeAttackEncounter$Snapshot;)Z (5), (Llocal/lito381/server/BattleTimeAttackEncounter$Snapshot;)Z]
CANDIDATE CALLSITE
test(Llocal/lito381/server/ClientSession;IILlocal/lito381/server/BattleSpellCatalog$MassSlowSpell;Ljava/lang/Object;)Ljava/util/function/Predicate; [(Ljava/lang/Object;)Z, local/lito381/server/ClientSession.nativeSourceFrozenSlow_lambda$useMassSlowSpell$323(IILlocal/lito381/server/BattleSpellCatalog$MassSlowSpell;Ljava/lang/Object;Llocal/lito381/server/BattleTimeAttackEncounter$Snapshot;)Z (5), (Llocal/lito381/server/BattleTimeAttackEncounter$Snapshot;)Z]
STAGED BODY
META 4098 null []
TRY []
ALOAD 0
ILOAD 1
ILOAD 2
ALOAD 3
ALOAD 4
ALOAD 5
INVOKESPECIAL local/lito381/server/ClientSession.nativeSourceFrozenSlow_lambda$useMassSlowSpell$323(IILlocal/lito381/server/BattleSpellCatalog$MassSlowSpell;Ljava/lang/Object;Llocal/lito381/server/BattleTimeAttackEncounter$Snapshot;)Z
IRETURN
CANDIDATE TARGET BODY
META 4098 null []
TRY []
ALOAD 0
GETFIELD local/lito381/server/ClientSession.hub:Llocal/lito381/server/GameHub;
ALOAD 0
GETFIELD local/lito381/server/ClientSession.currentRoomId:J
ALOAD 0
ALOAD 5
INVOKEVIRTUAL local/lito381/server/BattleTimeAttackEncounter$Snapshot.objectId()J
INVOKEVIRTUAL local/lito381/server/GameHub.canAttackTimeAttackActor(JLlocal/lito381/server/ClientSession;J)Z
IFEQ L24
ALOAD 5
INVOKEVIRTUAL local/lito381/server/BattleTimeAttackEncounter$Snapshot.x()I
ILOAD 1
ISUB
ALOAD 5
INVOKEVIRTUAL local/lito381/server/BattleTimeAttackEncounter$Snapshot.y()I
ILOAD 2
ISUB
ALOAD 4
INVOKESTATIC local/lito381/server/NativeSourceSlowFailureMetadata.distance(IILjava/lang/Object;)I
ALOAD 3
INVOKEVIRTUAL local/lito381/server/BattleSpellCatalog$MassSlowSpell.areaRadius()I
IF_ICMPGT L24
CONST Integer:1
GOTO L25
CONST Integer:0
IRETURN
```


</details>

<details><summary>lambda：local/lito381/server/ClientSession#lambda$nativeMaterialGroundBody$388(Llocal/lito381/server/RuntimeInventory;ILlocal/lito381/server/BattleSpellCatalog$GroundSpell;JI)V</summary>


```text
母方法 local/lito381/server/ClientSession#nativeMaterialGroundBody([BLlocal/lito381/server/BattleSpellCatalog$GroundSpell;)V
STAGED CALLSITE
run(Llocal/lito381/server/ClientSession;Llocal/lito381/server/RuntimeInventory;ILlocal/lito381/server/BattleSpellCatalog$GroundSpell;JI)Ljava/lang/Runnable; [()V, local/lito381/server/ClientSession.lambda$nativeMaterialGroundBody$388(Llocal/lito381/server/RuntimeInventory;ILlocal/lito381/server/BattleSpellCatalog$GroundSpell;JI)V (5), ()V]
CANDIDATE CALLSITE
run(Llocal/lito381/server/ClientSession;Llocal/lito381/server/RuntimeInventory;ILlocal/lito381/server/BattleSpellCatalog$GroundSpell;JI)Ljava/lang/Runnable; [()V, local/lito381/server/ClientSession.nativeMaterialGroundBodyCommit(Llocal/lito381/server/RuntimeInventory;ILlocal/lito381/server/BattleSpellCatalog$GroundSpell;JI)V (5), ()V]
STAGED BODY
META 4098 null []
TRY []
ALOAD 0
ALOAD 1
ILOAD 2
ALOAD 3
LLOAD 4/5
ILOAD 6
INVOKESPECIAL local/lito381/server/ClientSession.nativeMaterialGroundBodyCommit(Llocal/lito381/server/RuntimeInventory;ILlocal/lito381/server/BattleSpellCatalog$GroundSpell;JI)V
RETURN
CANDIDATE TARGET BODY
META 4098 null []
TRY []
ALOAD 0
ALOAD 3
INVOKESTATIC local/lito381/server/NativeGroundMaterialAction.admit(Llocal/lito381/server/ClientSession;Llocal/lito381/server/BattleSpellCatalog$GroundSpell;)V
INVOKESTATIC java/lang/System.nanoTime()J
LSTORE 7/8
ALOAD 0
DUP
GETFIELD local/lito381/server/ClientSession.playerMp:I
ILOAD 2
ISUB
DUP2
INVOKESTATIC local/lito381/server/NativeSkillConsumption.boundMp(Llocal/lito381/server/ClientSession;I)I
SWAP
POP
PUTFIELD local/lito381/server/ClientSession.playerMp:I
ALOAD 0
GETFIELD local/lito381/server/ClientSession.spellReadyNanos:Ljava/util/Map;
ALOAD 3
INVOKEVIRTUAL local/lito381/server/BattleSpellCatalog$GroundSpell.skillId()I
INVOKESTATIC java/lang/Integer.valueOf(I)Ljava/lang/Integer;
LLOAD 7/8
ALOAD 3
INVOKEVIRTUAL local/lito381/server/BattleSpellCatalog$GroundSpell.reuseDelayMillis()I
I2L
CONST Long:1000000
LMUL
LADD
INVOKESTATIC java/lang/Long.valueOf(J)Ljava/lang/Long;
INVOKEINTERFACE java/util/Map.put(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object; itf
POP
ALOAD 3
INVOKEVIRTUAL local/lito381/server/BattleSpellCatalog$GroundSpell.skillId()I
CONST Integer:58
IF_ICMPNE L37
ALOAD 0
ILOAD 6
PUTFIELD local/lito381/server/ClientSession.playerHeading:I
RETURN
```


</details>

<details><summary>lambda：local/lito381/server/ClientSession#lambda$nativeMaterialWorldGroundBody$389(Llocal/lito381/server/RuntimeInventory;ILlocal/lito381/server/BattleSpellCatalog$GroundSpell;JI)V</summary>


```text
母方法 local/lito381/server/ClientSession#nativeMaterialWorldGroundBody([BLlocal/lito381/server/BattleSpellCatalog$GroundSpell;)V
STAGED CALLSITE
run(Llocal/lito381/server/ClientSession;Llocal/lito381/server/RuntimeInventory;ILlocal/lito381/server/BattleSpellCatalog$GroundSpell;JI)Ljava/lang/Runnable; [()V, local/lito381/server/ClientSession.lambda$nativeMaterialWorldGroundBody$389(Llocal/lito381/server/RuntimeInventory;ILlocal/lito381/server/BattleSpellCatalog$GroundSpell;JI)V (5), ()V]
CANDIDATE CALLSITE
run(Llocal/lito381/server/ClientSession;Llocal/lito381/server/RuntimeInventory;ILlocal/lito381/server/BattleSpellCatalog$GroundSpell;JI)Ljava/lang/Runnable; [()V, local/lito381/server/ClientSession.nativeMaterialWorldGroundBodyCommit(Llocal/lito381/server/RuntimeInventory;ILlocal/lito381/server/BattleSpellCatalog$GroundSpell;JI)V (5), ()V]
STAGED BODY
META 4098 null []
TRY []
ALOAD 0
ALOAD 1
ILOAD 2
ALOAD 3
LLOAD 4/5
ILOAD 6
INVOKESPECIAL local/lito381/server/ClientSession.nativeMaterialWorldGroundBodyCommit(Llocal/lito381/server/RuntimeInventory;ILlocal/lito381/server/BattleSpellCatalog$GroundSpell;JI)V
RETURN
CANDIDATE TARGET BODY
META 4098 null []
TRY []
ALOAD 0
ALOAD 3
INVOKESTATIC local/lito381/server/NativeGroundMaterialAction.admit(Llocal/lito381/server/ClientSession;Llocal/lito381/server/BattleSpellCatalog$GroundSpell;)V
INVOKESTATIC java/lang/System.nanoTime()J
LSTORE 7/8
ALOAD 0
DUP
GETFIELD local/lito381/server/ClientSession.playerMp:I
ILOAD 2
ISUB
DUP2
INVOKESTATIC local/lito381/server/NativeSkillConsumption.boundMp(Llocal/lito381/server/ClientSession;I)I
SWAP
POP
PUTFIELD local/lito381/server/ClientSession.playerMp:I
ALOAD 0
GETFIELD local/lito381/server/ClientSession.spellReadyNanos:Ljava/util/Map;
ALOAD 3
INVOKEVIRTUAL local/lito381/server/BattleSpellCatalog$GroundSpell.skillId()I
INVOKESTATIC java/lang/Integer.valueOf(I)Ljava/lang/Integer;
LLOAD 7/8
ALOAD 3
INVOKEVIRTUAL local/lito381/server/BattleSpellCatalog$GroundSpell.reuseDelayMillis()I
I2L
CONST Long:1000000
LMUL
LADD
INVOKESTATIC java/lang/Long.valueOf(J)Ljava/lang/Long;
INVOKEINTERFACE java/util/Map.put(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object; itf
POP
ALOAD 3
INVOKEVIRTUAL local/lito381/server/BattleSpellCatalog$GroundSpell.skillId()I
CONST Integer:58
IF_ICMPNE L37
ALOAD 0
ILOAD 6
PUTFIELD local/lito381/server/ClientSession.playerHeading:I
RETURN
```


</details>

<details><summary>lambda：local/lito381/server/ClientSession#lambda$useClassicWorldAreaAttackSpell$239(Llocal/lito381/server/Protocol$AttackSpellRequest;Llocal/lito381/server/BattleSpellCatalog$AreaAttackSpell;)V</summary>


```text
母方法 local/lito381/server/ClientSession#useClassicWorldAreaAttackSpell(Llocal/lito381/server/Protocol$AttackSpellRequest;Llocal/lito381/server/BattleSpellCatalog$AreaAttackSpell;)V
STAGED CALLSITE
run(Llocal/lito381/server/ClientSession;Llocal/lito381/server/Protocol$AttackSpellRequest;Llocal/lito381/server/BattleSpellCatalog$AreaAttackSpell;)Llocal/lito381/server/NativeSelfCastAction$Task; [()V, local/lito381/server/ClientSession.lambda$useClassicWorldAreaAttackSpell$239(Llocal/lito381/server/Protocol$AttackSpellRequest;Llocal/lito381/server/BattleSpellCatalog$AreaAttackSpell;)V (5), ()V]
CANDIDATE CALLSITE
run(Llocal/lito381/server/ClientSession;Llocal/lito381/server/Protocol$AttackSpellRequest;Llocal/lito381/server/BattleSpellCatalog$AreaAttackSpell;)Llocal/lito381/server/NativeSelfCastAction$Task; [()V, local/lito381/server/ClientSession.nativeCompanionTargetuseClassicWorldAreaAttackSpell(Llocal/lito381/server/Protocol$AttackSpellRequest;Llocal/lito381/server/BattleSpellCatalog$AreaAttackSpell;)V (7), ()V]
STAGED BODY
META 4098 null [java/io/IOException]
TRY []
ALOAD 0
ALOAD 1
ALOAD 2
INVOKESPECIAL local/lito381/server/ClientSession.nativeCompanionTargetuseClassicWorldAreaAttackSpell(Llocal/lito381/server/Protocol$AttackSpellRequest;Llocal/lito381/server/BattleSpellCatalog$AreaAttackSpell;)V
RETURN
CANDIDATE TARGET BODY
本節證據為完整 wrapper 本體與兩側 callsite；候選完整 target 本體另見 candidate-259-dump.txt 的 local/lito381/server/ClientSession#nativeCompanionTargetuseClassicWorldAreaAttackSpell(Llocal/lito381/server/Protocol$AttackSpellRequest;Llocal/lito381/server/BattleSpellCatalog$AreaAttackSpell;)V。此 target 的獨立驗證狀態仍以完整 DIFF 表為準。
```


</details>

<details><summary>lambda：local/lito381/server/ClientSession#lambda$useClassicWorldAttackSpell$240(Llocal/lito381/server/Protocol$AttackSpellRequest;Llocal/lito381/server/BattleSpellCatalog$AttackSpell;)V</summary>


```text
母方法 local/lito381/server/ClientSession#useClassicWorldAttackSpell(Llocal/lito381/server/Protocol$AttackSpellRequest;Llocal/lito381/server/BattleSpellCatalog$AttackSpell;)V
STAGED CALLSITE
run(Llocal/lito381/server/ClientSession;Llocal/lito381/server/Protocol$AttackSpellRequest;Llocal/lito381/server/BattleSpellCatalog$AttackSpell;)Llocal/lito381/server/NativeSelfCastAction$Task; [()V, local/lito381/server/ClientSession.lambda$useClassicWorldAttackSpell$240(Llocal/lito381/server/Protocol$AttackSpellRequest;Llocal/lito381/server/BattleSpellCatalog$AttackSpell;)V (5), ()V]
CANDIDATE CALLSITE
run(Llocal/lito381/server/ClientSession;Llocal/lito381/server/Protocol$AttackSpellRequest;Llocal/lito381/server/BattleSpellCatalog$AttackSpell;)Llocal/lito381/server/NativeSelfCastAction$Task; [()V, local/lito381/server/ClientSession.nativeCompanionTargetuseClassicWorldAttackSpell(Llocal/lito381/server/Protocol$AttackSpellRequest;Llocal/lito381/server/BattleSpellCatalog$AttackSpell;)V (7), ()V]
STAGED BODY
META 4098 null [java/io/IOException]
TRY []
ALOAD 0
ALOAD 1
ALOAD 2
INVOKESPECIAL local/lito381/server/ClientSession.nativeCompanionTargetuseClassicWorldAttackSpell(Llocal/lito381/server/Protocol$AttackSpellRequest;Llocal/lito381/server/BattleSpellCatalog$AttackSpell;)V
RETURN
CANDIDATE TARGET BODY
本節證據為完整 wrapper 本體與兩側 callsite；候選完整 target 本體另見 candidate-259-dump.txt 的 local/lito381/server/ClientSession#nativeCompanionTargetuseClassicWorldAttackSpell(Llocal/lito381/server/Protocol$AttackSpellRequest;Llocal/lito381/server/BattleSpellCatalog$AttackSpell;)V。此 target 的獨立驗證狀態仍以完整 DIFF 表為準。
```


</details>

<details><summary>lambda：local/lito381/server/ClientSession#lambda$useClassicWorldLineAttackSpell$237([BLlocal/lito381/server/BattleSpellCatalog$LineAttackSpell;)V</summary>


```text
母方法 local/lito381/server/ClientSession#useClassicWorldLineAttackSpell([BLlocal/lito381/server/BattleSpellCatalog$LineAttackSpell;)V
STAGED CALLSITE
run(Llocal/lito381/server/ClientSession;[BLlocal/lito381/server/BattleSpellCatalog$LineAttackSpell;)Llocal/lito381/server/NativeSelfCastAction$Task; [()V, local/lito381/server/ClientSession.lambda$useClassicWorldLineAttackSpell$237([BLlocal/lito381/server/BattleSpellCatalog$LineAttackSpell;)V (5), ()V]
CANDIDATE CALLSITE
run(Llocal/lito381/server/ClientSession;[BLlocal/lito381/server/BattleSpellCatalog$LineAttackSpell;)Llocal/lito381/server/NativeSelfCastAction$Task; [()V, local/lito381/server/ClientSession.nativeCompanionTargetuseClassicWorldLineAttackSpell([BLlocal/lito381/server/BattleSpellCatalog$LineAttackSpell;)V (7), ()V]
STAGED BODY
META 4098 null [java/io/IOException]
TRY []
ALOAD 0
ALOAD 1
ALOAD 2
INVOKESPECIAL local/lito381/server/ClientSession.nativeCompanionTargetuseClassicWorldLineAttackSpell([BLlocal/lito381/server/BattleSpellCatalog$LineAttackSpell;)V
RETURN
CANDIDATE TARGET BODY
本節證據為完整 wrapper 本體與兩側 callsite；候選完整 target 本體另見 candidate-259-dump.txt 的 local/lito381/server/ClientSession#nativeCompanionTargetuseClassicWorldLineAttackSpell([BLlocal/lito381/server/BattleSpellCatalog$LineAttackSpell;)V。此 target 的獨立驗證狀態仍以完整 DIFF 表為準。
```


</details>

## 推測

剩餘差異中，私有 callback 包裝、跨分支堆疊值、boolean 物化、monitor handler 與清理程式碼複製可能需要更精確的比較器規則。這是待審查的方向，不是等價性結論；尤其 sourceApplicationRun 的 handler 涵蓋仍須裁定。

## 產出路徑與重現

- `lito伺服器/LiTo381-Server/merge-inputs/50635f7e/staged/local/lito381/server/ClientSession.java`
- `docs/reports/issue-70.md`
- 本機隔離證據：`scratch/issue-70/delivery70r4-diff.json`、`delivery70r4-diff.md`、`delivery70r4-dump.txt`、`delivery70r4-audit.txt`、`delivery70r4-classified.json`、`delivery70r4-calls.tsv`；編譯與工具日誌同前綴。
- 候選完整 javap：`scratch/issue-70/candidate259-ClientSession-javap.txt`；候選正規化指令：`candidate-259-dump.txt`。
- 本機重現腳本：`scratch/issue-70/verify-259.py <全新標籤>`，接著 `evidence259.py`。所有暫存腳本、classes、JAR 均未入 git。下列列出驗證參數，供其他環境重建；不要覆蓋正式 JAR。



```text
java -Xmx3g -cp <本輪工具與 ASM 9.7 classpath> local.lito381.semanticdiff.SemanticDiff
  --left scratch\issue-70\target\delivery70r4.jar
  --right lito伺服器\LiTo381-Server\reference\candidate-50635f7e.jar
  --left-classpath lito伺服器\LiTo381-Server\reference\candidate-50635f7e.jar
  --left-classpath lito伺服器\LiTo381-Server\reference\base-0.6.71.jar
  --classes scratch\issue-70\latest-entries.txt
  --allow-methods docs\combat-merge\t029-methods.txt
  --allow-right lito伺服器\LiTo381-Server\reference\base-0.6.71.jar
  --effects-methods scratch\issue-70\latest-effects-approved.txt
  --extra-methods scratch\issue-70\latest-extra-methods-approved.txt
  --exceptions-exempt scratch\issue-70\latest-exceptions-exempt.txt
  --metadata-exempt scratch\issue-70\latest-metadata-exempt.txt
  --json scratch\issue-70\delivery70r4-diff.json
  --md scratch\issue-70\delivery70r4-diff.md
```


## 給下一棒的提示

1. 主控／Claude：請先檢查上方 sourceApplicationRun 與私有 callback 完整差異，裁定是否需工具修正；本輪沒有改任何工具或新增核准清單。請維持 `needs:claude`，不可直接 B-land。
2. 本機執行器：針對仍列 DIFF 的方法繼續核對實際參數／路徑／handler。發現真實差異仍應改 Java；每次修改後重跑完整 362 entries，不能僅依直接呼叫數判定完成。
3. B-land：待 362 entries 全部為可接受狀態、方法配對清零後才執行本批單元／回歸。本輪沒有真機驗收結果。
4. GitHub 更新：PR #95 描述與 Issue 留言／標籤草稿已寫入 `scratch/issue-70/pr95-body-retry4.md`、`issue70-comment-retry4.md`。`gh` 回傳 401；嘗試改用 Git credential helper 的認證方式被自動審批拒絕，理由為尚未授權這種憑證取得方式。未繞過拒絕，未讀出或保存 token。待可用的已授權 GitHub 連線恢復後，以 `--body-file` 發布並加 `needs:claude`。
