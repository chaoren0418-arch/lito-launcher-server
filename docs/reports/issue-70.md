# Issue #70　B09 在 #254／#259 後重建與移植補正

## 結論

**未達 B09 驗收，尚不可 B-land。** 本輪在指定 worktree 與 `task/70`，以 `3920ab5` 合併 `origin/main@f795017`，其中含 #254／PR #260 與 #259／PR #265。依最新主控裁定保留 T029 inventory 的求值順序，使用新工具重驗，同時修正盤點確認的 Java 漏接。主控裁定見下方連結。

[2026-11-06 主控裁定](https://github.com/chaoren0418-arch/lito-launcher-server/issues/70#issuecomment-6670996283)


完整 **362 entries** 的類別結果為 360 SAME、1 SAME_CFG、1 DIFF。類別 missing／extra／ONLY_LEFT／ONLY_RIGHT 全為 0。方法診斷仍有 **255 DIFF、9 ONLY_LEFT、63 ONLY_RIGHT**。CLI exit 0 表示報表產生成功。

## 做了什麼

- 補入 `SourceLogin.account` 的八處漏呼叫，涵蓋 login 的五處、requestLitoCharacterInfo、enterChannel、authorizesAuthenticatedEndpointTransfer。handoff 先讀取 hub、帳號與其餘參數，再呼叫 account 包裝，依候選完整 javap 保留求值順序。另修正既有六個 handoff 方法及 auto-select lambda 的逆序暫存。
- setReady 的兩個提早 return 補 feedback。joinRoom 的九個提早 return 補 joined，正常出口的 joined 移到日誌與 LobbyRoomDirectory.refresh 之後。startBattle 依候選保留舊條件求值後取得 activeBot，forceWinner 使用 botSingle。
- 依完整指令差異補入 inventory、warehouse、rentalTownInventory 的 NativeDeItemApplication.replace，以及 scene／poison 寫入接線。修正 resetBattleVitals、respawnAtBattleSpawn 的 playerMaxHp 讀取位置，先取值再 advance。各方法寫入收據保存在 scratch。
- GameHub 補 ground／cube 的 advance、schedule、caster、dispatch 與完整 pulse 傳遞，保持 dispatch 位於每個 IOException catch 範圍之前。修正加入房間的 channel 成員登記、SIEGE_WAR 條件、disconnect 重複移除與 freezeWork 的呼叫位置。botTicks 改為具型別區域變數，消除多餘轉型。
- 四個 wave 方法以 raw List 參數對齊候選的 Signature metadata，迭代處保留 Snapshot 型別。say 沿用既有 rethrowUnchecked，縮小 checked throws 至候選的 IOException。
- movementUnlock 的 sendEncrypted 仍需 checked throws，依本輪明示核准加入精確 Exceptions 條目。核准僅忽略該方法的 Exceptions 屬性。
- 四個 enclosing-method 條目、前輪 Protocol EOF 修正、五個 AsRecord 與 AsmFinal 已在起點保留。本輪手改兩份 staged Java、exceptions-exempt 與本報告。

## 已驗證

Microsoft OpenJDK 17.0.17、ASM 9.7。從 Java 原始碼重新編譯三個外層類別、semantic-diff、AsRecord 與 annotation，使用全新的應用程式、工具及 annotation 輸出目錄。javac 成功，AsRecord `scanned=362 converted=6`，產物 class 集合與 B09 verify_entries 精確相等。建置流程直接以 staged Java 編出的 classes 套用核准 AsRecord 轉換。

驗收啟用完整 T029 `--allow-methods`／正式 `--allow-right`、兩筆既有 effects、一筆既有 extra helper、四筆 enclosing-method 與一筆 movementUnlock Exceptions。其餘核准模式未啟用。


| 階段 | 類別結果 | 方法 DIFF | 方法 ONLY_LEFT／ONLY_RIGHT |
|---|---|---|---|
| 前輪 #216 | 357 SAME、2 SAME_PATHS、3 DIFF | 346 | 9／63 |
| 本輪 #259 基線，Java 尚未修改 | 360 SAME、2 DIFF | 333 | 9／63 |
| 本輪交付 | 360 SAME、1 SAME_CFG、1 DIFF | 255 | 9／63 |


比對前後完整方法鍵，原 DIFF 有 78 個通過，新增 DIFF 為 0。方法診斷與後備證明的分布如下，此表僅計 methodResults 所列紀錄。

| 狀態 | 數量 |
|---|---|
| DIFF | 255 |
| ONLY_LEFT | 9 |
| ONLY_RIGHT | 63 |
| ORPHAN_IGNORED | 8 |
| SAME_CFG | 42 |
| SAME_PATHS | 39 |


### T029 八方法

| 方法與完整描述子 | 對正式版結果 |
|---|---|
| `local/lito381/server/ClientSession#exitRoom(Z)V` | SAME |
| `local/lito381/server/ClientSession#exitRoomBeforePolyMe(Z)V` | SAME_EFFECTS |
| `local/lito381/server/ClientSession#run()V` | SAME |
| `local/lito381/server/ClientSession#sendRoomParticipant(JLlocal/lito381/server/ClientSession;Llocal/lito381/server/Protocol$LitoForce;Ljava/lang/String;)V` | SAME |
| `local/lito381/server/ClientSession#sourcePoisonBody$useInventoryItemBeforeTravel([BLlocal/lito381/server/CombatPoisonLifecycle$Scope;)V` | SAME_EFFECTS |
| `local/lito381/server/Protocol#litoRoomParticipant(JLjava/lang/String;Llocal/lito381/server/Protocol$LitoForce;IILlocal/lito381/server/GameStateStore$TournamentStatistics;Ljava/lang/String;IILlocal/lito381/server/Protocol$LitoParticipantState;ILlocal/lito381/server/GameStateStore$TournamentAwards;)[B` | SAME |
| `local/lito381/server/Protocol#magicDollCustomIcon(IIZ)[B` | SAME |
| `local/lito381/server/Protocol#magicDollIcon(I)[B` | SAME |


### movementUnlock 的精確 Exceptions 核准

javap 的方法宣告如下，左側 Java 需保留 sendEncrypted 的 checked throws，核准條目只處理該屬性。


```text
candidate
private void movementUnlock(byte[]);
    descriptor: ([B)V

staged
private void movementUnlock(byte[]) throws java.io.IOException;
    descriptor: ([B)V
```


### light、disconnect、switch-map 與 healing records

light 的比較重新核對完整逐路徑事件、引數及 multiplicity。以下為實際收據。


```text
LIGHT_PATHS left=90 right=90 equal=true
LIGHT_CASTS LEFT [CHECKCAST local/lito381/server/ClientSession$Phase, CHECKCAST java/lang/Long]
LIGHT_CASTS RIGHT [CHECKCAST local/lito381/server/ClientSession$Phase, CHECKCAST java/lang/Long]
```


| 項目 | 結果 |
|---|---|
| acceptsLitoRosterBroadcast()Z | SAME |
| movementUnlock([B)V | SAME |
| nativeLightCommit(Llocal/lito381/server/BattleSpellCatalog$LightSpell;Ljava/lang/Object;J)Ljava/lang/Integer; | SAME_PATHS |
| sourceApplicationRun()V | DIFF |
| <clinit>()V | SAME |
| battleDisconnectWork(JJ)Llocal/lito381/server/GameHub$BattleDisconnectWork; | SAME |
| ClientSession$1CompanionHealing | SAME |
| ClientSession$1PlayerHealing | SAME |
| ClientSession$2CompanionHealing | SAME |
| ClientSession$2PlayerHealing | SAME |

## 呼叫點完整對照

以下掃描候選 JAR 中 B09 三個外層類別及其內部類別的全部 INVOKESTATIC 呼叫點。位置為 javap 對應的有效指令序號，排除 label／frame／line。每列列出同一完整方法及目標的所有位置。Protocol 沒有這兩組呼叫。

候選 exitRoom 的 restoreBot 由 T029 正式版的 exitRoomBeforePolyMe 承接，兩者分別按核准目標驗證。sourceApplicationRun 的 restoreBot 仍為 3 對 4 份線性複本，整個方法保留 DIFF，等待 handler／finally 路徑核對。其餘呼叫點數相同仍需完整方法通過。


### SourceLogin.account

| 完整呼叫端 | 目標 | 候選位置 | staged 位置 | 完整方法結果 |
|---|---|---|---|---|
| `local/lito381/server/ClientSession#activatePendingRentalBattleHandoff(Ljava/lang/String;)Z` | `local/lito381/server/SourceLogin.account(Llocal/lito381/server/ClientSession;Ljava/lang/String;)Ljava/lang/String;` | 25 | 25 | SAME |
| `local/lito381/server/ClientSession#administratorBan([B)V` | `local/lito381/server/SourceLogin.account(Llocal/lito381/server/ClientSession;Ljava/lang/String;)Ljava/lang/String;` | 35 | 37 | SAME |
| `local/lito381/server/ClientSession#authorizesAuthenticatedEndpointTransfer(Ljava/lang/String;)Z` | `local/lito381/server/SourceLogin.account(Llocal/lito381/server/ClientSession;Ljava/lang/String;)Ljava/lang/String;` | 29 | 31 | DIFF |
| `local/lito381/server/ClientSession#enterChannel([B)V` | `local/lito381/server/SourceLogin.account(Llocal/lito381/server/ClientSession;Ljava/lang/String;)Ljava/lang/String;` | 330 | 328 | SAME |
| `local/lito381/server/ClientSession#handleRentalTownTeleportAction(Llocal/lito381/server/NativeLitoRentalTownNpcCatalog$Npc;Ljava/lang/String;)V` | `local/lito381/server/SourceLogin.account(Llocal/lito381/server/ClientSession;Ljava/lang/String;)Ljava/lang/String;` | 34 | 34 | SAME |
| `local/lito381/server/ClientSession#lambda$autoSelectPendingRentalBattleHandoff$30(Llocal/lito381/server/CharacterSummary;)Z` | `local/lito381/server/SourceLogin.account(Llocal/lito381/server/ClientSession;Ljava/lang/String;)Ljava/lang/String;` | 14 | 14 | SAME |
| `local/lito381/server/ClientSession#login([B)V` | `local/lito381/server/SourceLogin.account(Llocal/lito381/server/ClientSession;Ljava/lang/String;)Ljava/lang/String;` | 161,196,208,250,396 | 160,191,199,241,387 | SAME |
| `local/lito381/server/ClientSession#redirectToChannelPort(I)V` | `local/lito381/server/SourceLogin.account(Llocal/lito381/server/ClientSession;Ljava/lang/String;)Ljava/lang/String;` | 34,62 | 34,62 | SAME |
| `local/lito381/server/ClientSession#rentalItemSelectionContext()Z` | `local/lito381/server/SourceLogin.account(Llocal/lito381/server/ClientSession;Ljava/lang/String;)Ljava/lang/String;` | 27 | 35 | SAME_PATHS |
| `local/lito381/server/ClientSession#requestLitoCharacterInfo([B)V` | `local/lito381/server/SourceLogin.account(Llocal/lito381/server/ClientSession;Ljava/lang/String;)Ljava/lang/String;` | 102 | 102 | SAME |
| `local/lito381/server/ClientSession#requestQuit()V` | `local/lito381/server/SourceLogin.account(Llocal/lito381/server/ClientSession;Ljava/lang/String;)Ljava/lang/String;` | 64,84 | 64,84 | SAME |
| `local/lito381/server/ClientSession#selectClassicServer([B)V` | `local/lito381/server/SourceLogin.account(Llocal/lito381/server/ClientSession;Ljava/lang/String;)Ljava/lang/String;` | 67 | 67 | SAME |
| `local/lito381/server/ClientSession#sourcePoisonBody$useInventoryItemBeforeTravel([BLlocal/lito381/server/CombatPoisonLifecycle$Scope;)V` | `local/lito381/server/SourceLogin.account(Llocal/lito381/server/ClientSession;Ljava/lang/String;)Ljava/lang/String;` | 95 | 95 | SAME_EFFECTS |
| `local/lito381/server/GameHub#acceptAuthenticatedAccount(Llocal/lito381/server/ClientSession;Llocal/lito381/server/ClientSession$Endpoint;Ljava/lang/String;)Z` | `local/lito381/server/SourceLogin.account(Llocal/lito381/server/ClientSession;Ljava/lang/String;)Ljava/lang/String;` | 3 | 2 | SAME |
| `local/lito381/server/GameHub#releaseAuthenticatedAccountForTransfer(Llocal/lito381/server/ClientSession;Ljava/lang/String;)Z` | `local/lito381/server/SourceLogin.account(Llocal/lito381/server/ClientSession;Ljava/lang/String;)Ljava/lang/String;` | 3 | 2 | SAME |

### NativeRoomRules.*

| 完整呼叫端 | 目標 | 候選位置 | staged 位置 | 完整方法結果 |
|---|---|---|---|---|
| `local/lito381/server/ClientSession#changeRoomMode(Llocal/lito381/server/Protocol$LitoModeStepDirection;)V` | `local/lito381/server/NativeRoomRules.bot(Llocal/lito381/server/BattleRules$Mode;Llocal/lito381/server/ClientSession;)Z` | 127,131 | 127,131 | SAME |
| `local/lito381/server/ClientSession#completeRestartReturn(Z)V` | `local/lito381/server/NativeRoomRules.restoreBot(Llocal/lito381/server/ClientSession;)V` | 110 | 111 | DIFF |
| `local/lito381/server/ClientSession#exitRoom(Z)V` | `local/lito381/server/NativeRoomRules.restoreBot(Llocal/lito381/server/ClientSession;)V` | 241 | 無 | SAME |
| `local/lito381/server/ClientSession#findUser([B)V` | `local/lito381/server/NativeRoomRules.bot(Llocal/lito381/server/BattleRules$Mode;Llocal/lito381/server/GameHub;J)Z` | 65 | 65 | SAME |
| `local/lito381/server/ClientSession#handleGmChatCommandBeforeTravel(Ljava/lang/String;)Z` | `local/lito381/server/NativeRoomRules.bot(Llocal/lito381/server/BattleRules$Mode;Llocal/lito381/server/ClientSession;)Z` | 212,330 | 204,322 | DIFF |
| `local/lito381/server/ClientSession#joinRoom([BLjava/lang/String;Z)V` | `local/lito381/server/NativeRoomRules.bot(Llocal/lito381/server/BattleRules$Mode;Llocal/lito381/server/GameHub;J)Z` | 468,569 | 468,569 | SAME |
| `local/lito381/server/ClientSession#joinRoom([BLjava/lang/String;Z)V` | `local/lito381/server/NativeRoomRules.joined(Llocal/lito381/server/ClientSession;)V` | 42,59,114,147,179,200,223,243,310,618 | 42,59,114,147,179,200,223,243,310,618 | SAME |
| `local/lito381/server/ClientSession#nativePlacementPortalBody(Llocal/lito381/server/Protocol$PortalDestination;Llocal/lito381/server/ClientSession$PendingClassicWorldPortal;)V` | `local/lito381/server/NativeRoomRules.restoreBot(Llocal/lito381/server/ClientSession;)V` | 107 | 110 | DIFF |
| `local/lito381/server/ClientSession#sendPortalWorldRebuild()V` | `local/lito381/server/NativeRoomRules.bot(Llocal/lito381/server/BattleRules$Mode;Llocal/lito381/server/ClientSession;)Z` | 127,346 | 127,346 | SAME |
| `local/lito381/server/ClientSession#sendRoomUser([B)V` | `local/lito381/server/NativeRoomRules.bot(Llocal/lito381/server/BattleRules$Mode;Llocal/lito381/server/GameHub;J)Z` | 110 | 110 | SAME |
| `local/lito381/server/ClientSession#sendRoomWho([B)V` | `local/lito381/server/NativeRoomRules.bot(Llocal/lito381/server/BattleRules$Mode;Llocal/lito381/server/GameHub;J)Z` | 105 | 105 | SAME |
| `local/lito381/server/ClientSession#setReady([B)V` | `local/lito381/server/NativeRoomRules.bot(Llocal/lito381/server/BattleRules$Mode;Llocal/lito381/server/ClientSession;)Z` | 153 | 153 | SAME |
| `local/lito381/server/ClientSession#setReady([B)V` | `local/lito381/server/NativeRoomRules.feedback(Llocal/lito381/server/ClientSession;)V` | 5,45,207 | 5,45,207 | SAME |
| `local/lito381/server/ClientSession#sourceApplicationRun()V` | `local/lito381/server/NativeRoomRules.restoreBot(Llocal/lito381/server/ClientSession;)V` | 350,686,1022,1332 | 394,716,1026 | DIFF |
| `local/lito381/server/ClientSession#sourcePoisonBody$serverEndBattle(Llocal/lito381/server/BattleRules$Resolution;Llocal/lito381/server/CombatPoisonLifecycle$Scope;)V` | `local/lito381/server/NativeRoomRules.bot(Llocal/lito381/server/BattleRules$Mode;Llocal/lito381/server/ClientSession;)Z` | 261 | 264 | SAME |
| `local/lito381/server/ClientSession#startBattle()V` | `local/lito381/server/NativeRoomRules.activeBot(Llocal/lito381/server/ClientSession;)Z` | 246 | 244 | DIFF |
| `local/lito381/server/GameHub#<init>(Llocal/lito381/server/ServerConfig;Llocal/lito381/server/GameStateStore;Llocal/lito381/server/AccountRepository;)V` | `local/lito381/server/NativeRoomRules.install(Llocal/lito381/server/GameHub;Llocal/lito381/server/ServerConfig;Llocal/lito381/server/AccountRepository;)V` | 454 | 456 | SAME |
| `local/lito381/server/GameHub#battleDisconnectWork(JJ)Llocal/lito381/server/GameHub$BattleDisconnectWork;` | `local/lito381/server/NativeRoomRules.botHp(Ljava/lang/Object;Llocal/lito381/server/GameHub;)I` | 311 | 312 | SAME |
| `local/lito381/server/GameHub#countdownWork(J)Llocal/lito381/server/GameHub$CountdownWork;` | `local/lito381/server/NativeRoomRules.botHp(Ljava/lang/Object;Llocal/lito381/server/GameHub;)I` | 137 | 137 | SAME |
| `local/lito381/server/GameHub#endBattle(J)V` | `local/lito381/server/NativeRoomRules.botHp(Ljava/lang/Object;Llocal/lito381/server/GameHub;)I` | 78 | 78 | SAME |
| `local/lito381/server/GameHub#ensureDefaultRoom(Llocal/lito381/server/GameHub$Channel;)Llocal/lito381/server/GameHub$Room;` | `local/lito381/server/NativeRoomRules.enabled(Llocal/lito381/server/GameHub;)Z` | 1 | 1 | SAME |
| `local/lito381/server/GameHub#forceWinner(JI)Llocal/lito381/server/BattleRules$Resolution;` | `local/lito381/server/NativeRoomRules.botSingle(Llocal/lito381/server/GameHub;J)I` | 109 | 109 | SAME |
| `local/lito381/server/GameHub#nativeRosterReady(Llocal/lito381/server/GameHub$Room;Ljava/util/List;)Z` | `local/lito381/server/NativeRoomRules.rosterReady(Llocal/lito381/server/GameHub;Ljava/lang/Object;Ljava/util/List;)Z` | 3 | 3 | SAME |
| `local/lito381/server/GameHub#resetOrRemove(Llocal/lito381/server/GameHub$Room;)V` | `local/lito381/server/NativeRoomRules.botHp(Ljava/lang/Object;Llocal/lito381/server/GameHub;)I` | 52 | 52 | SAME |
| `local/lito381/server/GameHub#winningForce(JLlocal/lito381/server/BattleRules$Resolution;)I` | `local/lito381/server/NativeRoomRules.botSingle(Llocal/lito381/server/GameHub;J)I` | 84 | 84 | SAME_CFG |

## 剩餘差異與待驗證

剩餘方法包含尚未完成的 Java 移植。缺呼叫涉及 NativeSkillConsumption、NativeCastMaterial、NativeSelectedPlayerIntent、CombatWorldPhysical、NativeCompanionCallbacks 等。這些需要本機繼續按候選補入，尚未逐一修完。sourceApplicationRun、跨區塊 stack／POP、boolean 匯合及 lambda 配對則仍需主控檢查完整差異。

下表的四種標記依完整正規化線性指令分類。缺／多呼叫按目標與次數計算，finally 複製也可能改變線性次數。順序標記包含區塊排列。純形狀待裁定表示呼叫序列相同，仍須檢查引數、欄位、常數、metadata 與控制流程。本輪未逐一完成所有剩餘方法的語意根因裁定，所有列均保留官方 DIFF。


| 標記 | 方法數 |
|---|---|
| 多呼叫 | 1 |
| 純形狀待裁定 | 31 |
| 缺呼叫 | 200 |
| 順序不同 | 23 |

| 完整方法 | 呼叫序列標記 | 首項診斷 |
|---|---|---|
| `local/lito381/server/ClientSession#answerAsk([B)V` | 缺呼叫 | method answerAsk([B)V: length left=0 right=109; paths rejected: normal control-flow cycle; labelled CFGs differ |
| `local/lito381/server/ClientSession#applyBattleCompanionFeedbackDamage(Llocal/lito381/server/BattleCompanion;ILjava/util/List;Ljava/lang/String;)Llocal/lito381/server/BattleCompanion$Damage;` | 純形狀待裁定 | method applyBattleCompanionFeedbackDamage(Llocal/lito381/server/BattleCompanion;ILjava/util/List;Ljava/lang/String;)Llocal/lito381/server/BattleCompanion$Damage; insn[6]: left=ALOAD 0 right=ALOAD 1; paths rejected: normal control-flow cycle; labelled CFGs differ |
| `local/lito381/server/ClientSession#applyClassicWorldMonsterCompanionDamageInternal(Llocal/lito381/server/BattleCompanion;Llocal/lito381/server/BattleCompanion$Snapshot;IJLjava/lang/String;Z)Llocal/lito381/server/BattleCompanion$Damage;` | 純形狀待裁定 | method applyClassicWorldMonsterCompanionDamageInternal(Llocal/lito381/server/BattleCompanion;Llocal/lito381/server/BattleCompanion$Snapshot;IJLjava/lang/String;Z)Llocal/lito381/server/BattleCompanion$Damage; insn[40]: left=ALOAD 0 right=ALOAD 1; paths rejected: normal control-flow cycle; labelled CFGs differ |
| `local/lito381/server/ClientSession#applyJoyOfPainClassicWorldMonsterFeedback(Llocal/lito381/server/ClassicWorldMonsterPopulation$Snapshot;Llocal/lito381/server/BattleJoyOfPainStatus$Lease;ILjava/lang/String;)V` | 缺呼叫 | method applyJoyOfPainClassicWorldMonsterFeedback(Llocal/lito381/server/ClassicWorldMonsterPopulation$Snapshot;Llocal/lito381/server/BattleJoyOfPainStatus$Lease;ILjava/lang/String;)V insn[0]: left=ALOAD 1 right=ALOAD 0; paths rejected: normal control-flow cycle; labelled CFGs differ |
| `local/lito381/server/ClientSession#attackClassicWorld(Llocal/lito381/server/Protocol$AttackRequest;Llocal/lito381/server/ClientSession$AttackEnvelope;)V` | 缺呼叫 | method attackClassicWorld(Llocal/lito381/server/Protocol$AttackRequest;Llocal/lito381/server/ClientSession$AttackEnvelope;)V: left=1471:[1475:java/lang/Exception  ] right=1591:[1595:java/lang/Exception  ]; paths rejected: normal control-flow cycle; labelled CFGs differ |
| `local/lito381/server/ClientSession#attackClassicWorldPlayer(Llocal/lito381/server/Protocol$AttackRequest;Llocal/lito381/server/ClientSession$AttackEnvelope;Llocal/lito381/server/ClientSession;)V` | 缺呼叫 | method attackClassicWorldPlayer(Llocal/lito381/server/Protocol$AttackRequest;Llocal/lito381/server/ClientSession$AttackEnvelope;Llocal/lito381/server/ClientSession;)V insn[1]: left=GETFIELD local/lito381/server/ClientSession.classicWorldMapId:I right=INVOKESTATIC local/lito381/server/CombatDragon.normalAttack(Llocal/lito381/server/ClientSession;)V; paths rejected: normal control-flow cycle; labelled CFGs differ |
| `local/lito381/server/ClientSession#authorizesAuthenticatedEndpointTransfer(Ljava/lang/String;)Z` | 純形狀待裁定 | method authorizesAuthenticatedEndpointTransfer(Ljava/lang/String;)Z insn[11]: left=CONST Integer:0 right=ALOAD 0; path event multisets differ (left=157, right=205); labelled CFGs differ |
| `local/lito381/server/ClientSession#autoSelectPendingRentalBattleHandoff()Z` | 純形狀待裁定 | method autoSelectPendingRentalBattleHandoff()Z insn[21]: left=INVOKEDYNAMIC test(Llocal/lito381/server/ClientSession;)Ljava/util/function/Predicate; bsm=H6 java/lang/invoke/LambdaMetafactory.metafactory(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;Ljava/lang/in… right=INVOKEDYNAMIC test(Llocal/lito381/server/ClientSession;)Ljava/util/function/Predicate; bsm=H6 java/lang/invoke/LambdaMetafactory.metafactory(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;Ljava/lang/in…; path event multisets differ (left=29, right=29); labelled CFGs differ |
| `local/lito381/server/ClientSession#callClanDestinationOccupied(Llocal/lito381/server/ClientSession$PendingCallClan;IIJ)Z` | 缺呼叫 | method callClanDestinationOccupied(Llocal/lito381/server/ClientSession$PendingCallClan;IIJ)Z insn[3]: left=IF_ACMPNE L81 right=IF_ACMPNE L82; paths rejected: normal control-flow cycle; labelled CFGs differ |
| `local/lito381/server/ClientSession#classicWorldCompanionSnapshots()Ljava/util/List;` | 純形狀待裁定 | method classicWorldCompanionSnapshots()Ljava/util/List;: left=21:[32:java/lang/Throwable  ] right=19:[32:java/lang/Throwable  ]; path event multisets differ (left=17, right=17); labelled CFGs differ |
| `local/lito381/server/ClientSession#classicWorldMonsterPhysicalSkillHit(Llocal/lito381/server/ClassicWorldMonsterPopulation$Snapshot;Llocal/lito381/server/ClassicWorldMonsterPhysicalSkillCatalog$Definition;Llocal/lito381/server/ClientSession$ClassicWorldMonsterPhysicalTarget;J)Llocal/lito381/server/ClientSession$ClassicWorldMonsterPhysicalHit;` | 缺呼叫 | method classicWorldMonsterPhysicalSkillHit(Llocal/lito381/server/ClassicWorldMonsterPopulation$Snapshot;Llocal/lito381/server/ClassicWorldMonsterPhysicalSkillCatalog$Definition;Llocal/lito381/server/ClientSession$ClassicWorldMonsterPhysicalTarget;J)Llocal/lito381/server/ClientSession$ClassicWorldMonsterPhysicalHit;: left=276:[283:java/lang/Exception  ] right=281:[288:java/lang/Exception  ]; paths rejected: path limit 4096 exceeded; labelled CFGs differ |
| `local/lito381/server/ClientSession#cleanupClassicWorldTransientCompanionsOnDisconnect()V` | 純形狀待裁定 | method cleanupClassicWorldTransientCompanionsOnDisconnect()V insn[8]: left=INVOKEDYNAMIC test()Ljava/util/function/Predicate; bsm=H6 java/lang/invoke/LambdaMetafactory.metafactory(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodType;Ljava/lang/invoke/Me… right=INVOKEDYNAMIC test()Ljava/util/function/Predicate; bsm=H6 java/lang/invoke/LambdaMetafactory.metafactory(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodType;Ljava/lang/invoke/Me…; paths rejected: normal control-flow cycle; labelled CFGs differ |
| `local/lito381/server/ClientSession#clearNativeMagicDollAutoSkills(JLjava/lang/String;Z)V` | 缺呼叫 | method clearNativeMagicDollAutoSkills(JLjava/lang/String;Z)V insn[6]: left=IFEQ L121 right=IFEQ L123; paths rejected: normal control-flow cycle; labelled CFGs differ |
| `local/lito381/server/ClientSession#commitClassicWorldMonsterCounterBarrier(Llocal/lito381/server/ClassicWorldMonsterPopulation$Snapshot;Llocal/lito381/server/BattleCounterBarrier$Decision;)V` | 缺呼叫 | method commitClassicWorldMonsterCounterBarrier(Llocal/lito381/server/ClassicWorldMonsterPopulation$Snapshot;Llocal/lito381/server/BattleCounterBarrier$Decision;)V insn[141]: left=INVOKESTATIC local/lito381/server/Protocol.hitRatio(JII)[B right=INVOKESTATIC local/lito381/server/NativeBattleTamedLifecycle.visualHitRatio(JII)[B; paths rejected: normal control-flow cycle; labelled CFGs differ |
| `local/lito381/server/ClientSession#commitNativeMagicDollClassicWorldProcDamage(JILjava/lang/String;)V` | 缺呼叫 | method commitNativeMagicDollClassicWorldProcDamage(JILjava/lang/String;)V insn[0]: left=ILOAD 3 right=ALOAD 0; paths rejected: normal control-flow cycle; labelled CFGs differ |
| `local/lito381/server/ClientSession#completeClassicWorldPortal(Llocal/lito381/server/Protocol$PortalDestination;Llocal/lito381/server/ClientSession$PendingClassicWorldPortal;)V` | 純形狀待裁定 | method completeClassicWorldPortal(Llocal/lito381/server/Protocol$PortalDestination;Llocal/lito381/server/ClientSession$PendingClassicWorldPortal;)V insn[6]: left=INVOKEDYNAMIC run(Llocal/lito381/server/ClientSession;Llocal/lito381/server/Protocol$PortalDestination;Llocal/lito381/server/ClientSession$PendingClassicWorldPortal;)Llocal/lito381/server/NativeSelfCastAction$Task; bsm=H6 java/lang/invoke/L… right=INVOKEDYNAMIC run(Llocal/lito381/server/ClientSession;Llocal/lito381/server/Protocol$PortalDestination;Llocal/lito381/server/ClientSession$PendingClassicWorldPortal;)Llocal/lito381/server/NativeSelfCastAction$Task; bsm=H6 java/lang/invoke/L…; path event multisets differ (left=3, right=3); labelled CFGs differ |
| `local/lito381/server/ClientSession#completeClassicWorldRescueFollowerIfReady()Z` | 缺呼叫 | method completeClassicWorldRescueFollowerIfReady()Z: left=153:[169:java/io/IOException  , 169:java/lang/RuntimeException  ] right=155:[171:java/io/IOException  , 171:java/lang/RuntimeException  ]; paths rejected: expression size limit 32768 exceeded; labelled CFGs differ |
| `local/lito381/server/ClientSession#completeClassicWorldRoiFollowerIfReady()Z` | 缺呼叫 | method completeClassicWorldRoiFollowerIfReady()Z: left=37:[100:java/io/IOException  , 100:java/lang/RuntimeException  ] right=39:[102:java/io/IOException  , 102:java/lang/RuntimeException  ]; path event multisets differ (left=374, right=374); labelled CFGs differ |
| `local/lito381/server/ClientSession#completePortal([B)V` | 缺呼叫 | method completePortal([B)V insn[322]: left=ALOAD 0 right=NEW local/lito381/server/Protocol$Writer; paths rejected: normal control-flow cycle; labelled CFGs differ |
| `local/lito381/server/ClientSession#completeRestartReturn(Z)V` | 缺呼叫 | method completeRestartReturn(Z)V insn[109]: left=ISTORE 5 right=ALOAD 0; path event multisets differ (left=2693, right=2693); labelled CFGs differ |
| `local/lito381/server/ClientSession#confirmDeath([B)V` | 缺呼叫 | method confirmDeath([B)V insn[23]: left=INVOKEVIRTUAL local/lito381/server/GameHub.playerDefeated(JLlocal/lito381/server/ClientSession;Llocal/lito381/server/ClientSession;)Llocal/lito381/server/BattleRules$Resolution; right=INVOKESTATIC local/lito381/server/CombatPoisonLifecycle.defeated(Llocal/lito381/server/GameHub;JLlocal/lito381/server/ClientSession;Llocal/lito381/server/ClientSession;)Llocal/lito381/server/BattleRules$Resolution;; paths rejected: normal control-flow cycle; labelled CFGs differ |
| `local/lito381/server/ClientSession#consumeClassicWorldDeathSafe(ILjava/lang/String;)Z` | 純形狀待裁定 | method consumeClassicWorldDeathSafe(ILjava/lang/String;)Z insn[9]: left=IFEQ L34 right=IFEQ L32; paths rejected: normal control-flow cycle; labelled CFGs differ |
| `local/lito381/server/ClientSession#distributeClassicWorldMonsterDrops(Llocal/lito381/server/ClassicWorldMonsterPopulation$Snapshot;Ljava/util/List;Ljava/util/Map;)V` | 缺呼叫 | method distributeClassicWorldMonsterDrops(Llocal/lito381/server/ClassicWorldMonsterPopulation$Snapshot;Ljava/util/List;Ljava/util/Map;)V insn[20]: left=INVOKESPECIAL local/lito381/server/ClientSession.activeMagicDollPowers()Llocal/lito381/server/NativeMagicDollPowers; right=ALOAD 1; paths rejected: normal control-flow cycle; labelled CFGs differ |
| `local/lito381/server/ClientSession#handleClassicCastleEquipmentEnchant(Llocal/lito381/server/Protocol$HypertextActionRequest;Llocal/lito381/server/ClassicWorldNpcCatalog$StaticNpc;)V` | 缺呼叫 | method handleClassicCastleEquipmentEnchant(Llocal/lito381/server/Protocol$HypertextActionRequest;Llocal/lito381/server/ClassicWorldNpcCatalog$StaticNpc;)V insn[12]: left=IF_ACMPNE L69 right=IF_ACMPNE L71; paths rejected: normal control-flow cycle; labelled CFGs differ |
| `local/lito381/server/ClientSession#handleClassicWorldCkewCompletionAction(Llocal/lito381/server/Protocol$HypertextActionRequest;Llocal/lito381/server/ClassicWorldNpcCatalog$StaticNpc;)Z` | 缺呼叫 | method handleClassicWorldCkewCompletionAction(Llocal/lito381/server/Protocol$HypertextActionRequest;Llocal/lito381/server/ClassicWorldNpcCatalog$StaticNpc;)Z insn[38]: left=IF_ACMPNE L67 right=IF_ACMPNE L69; paths rejected: normal control-flow cycle; labelled CFGs differ |
| `local/lito381/server/ClientSession#handleClassicWorldCompanionAction(Llocal/lito381/server/Protocol$HypertextActionRequest;)V` | 缺呼叫 | method handleClassicWorldCompanionAction(Llocal/lito381/server/Protocol$HypertextActionRequest;)V insn[1]: left=GETFIELD local/lito381/server/ClientSession.observer:Z right=INVOKESTATIC local/lito381/server/NativeCompanionCommit.captureMaster(Llocal/lito381/server/ClientSession;)Llocal/lito381/server/NativePlayerCommit$Binding;; paths rejected: path limit 4096 exceeded; labelled CFGs differ |
| `local/lito381/server/ClientSession#handleClassicWorldDesireCaveAction(Llocal/lito381/server/Protocol$HypertextActionRequest;Llocal/lito381/server/ClassicWorldNpcCatalog$StaticNpc;)Z` | 缺呼叫 | method handleClassicWorldDesireCaveAction(Llocal/lito381/server/Protocol$HypertextActionRequest;Llocal/lito381/server/ClassicWorldNpcCatalog$StaticNpc;)Z insn[8]: left=IF_ACMPNE L34 right=IF_ACMPNE L36; path event multisets differ (left=600, right=600); labelled CFGs differ |
| `local/lito381/server/ClientSession#handleClassicWorldHotelAction(Llocal/lito381/server/Protocol$HypertextActionRequest;Llocal/lito381/server/ClassicWorldNpcCatalog$StaticNpc;)V` | 缺呼叫 | method handleClassicWorldHotelAction(Llocal/lito381/server/Protocol$HypertextActionRequest;Llocal/lito381/server/ClassicWorldNpcCatalog$StaticNpc;)V insn[3]: left=IF_ACMPNE L29 right=IF_ACMPNE L31; path event multisets differ (left=1066, right=1066); labelled CFGs differ |
| `local/lito381/server/ClientSession#handleClassicWorldKamylaAction(Llocal/lito381/server/Protocol$HypertextActionRequest;Llocal/lito381/server/ClassicWorldNpcCatalog$StaticNpc;)Z` | 缺呼叫 | method handleClassicWorldKamylaAction(Llocal/lito381/server/Protocol$HypertextActionRequest;Llocal/lito381/server/ClassicWorldNpcCatalog$StaticNpc;)Z insn[25]: left=IF_ACMPNE L51 right=IF_ACMPNE L53; paths rejected: path limit 4096 exceeded; labelled CFGs differ |
| `local/lito381/server/ClientSession#handleClassicWorldNpcAction(Llocal/lito381/server/Protocol$HypertextActionRequest;)V` | 缺呼叫 | method handleClassicWorldNpcAction(Llocal/lito381/server/Protocol$HypertextActionRequest;)V insn[118]: left=IFNULL L129 right=IFNULL L131; paths rejected: normal control-flow cycle; labelled CFGs differ |
| `local/lito381/server/ClientSession#handleClassicWorldRescueStaticAction(Llocal/lito381/server/Protocol$HypertextActionRequest;Llocal/lito381/server/ClassicWorldNpcCatalog$StaticNpc;Llocal/lito381/server/ClassicWorldRescueFollowers$Definition;)V` | 缺呼叫 | method handleClassicWorldRescueStaticAction(Llocal/lito381/server/Protocol$HypertextActionRequest;Llocal/lito381/server/ClassicWorldNpcCatalog$StaticNpc;Llocal/lito381/server/ClassicWorldRescueFollowers$Definition;)V: left=131:[171:java/io/IOException  , 171:java/lang/RuntimeException  ] right=137:[178:java/io/IOException  , 178:java/lang/RuntimeException  ]; path event multisets differ (left=2868, right=2868); labelled CFGs differ |
| `local/lito381/server/ClientSession#handleClassicWorldShipGraveAction(Llocal/lito381/server/Protocol$HypertextActionRequest;Llocal/lito381/server/ClassicWorldNpcCatalog$StaticNpc;)Z` | 缺呼叫 | method handleClassicWorldShipGraveAction(Llocal/lito381/server/Protocol$HypertextActionRequest;Llocal/lito381/server/ClassicWorldNpcCatalog$StaticNpc;)Z insn[6]: left=IFEQ L229 right=IFEQ L231; paths rejected: normal control-flow cycle; labelled CFGs differ |
| `local/lito381/server/ClientSession#handleClassicWorldStoryAction(Llocal/lito381/server/Protocol$HypertextActionRequest;Llocal/lito381/server/ClassicWorldNpcCatalog$StaticNpc;)Z` | 缺呼叫 | method handleClassicWorldStoryAction(Llocal/lito381/server/Protocol$HypertextActionRequest;Llocal/lito381/server/ClassicWorldNpcCatalog$StaticNpc;)Z insn[50]: left=INVOKEVIRTUAL local/lito381/server/GameHub.classicWorldNpcGfx(Llocal/lito381/server/ClassicWorldNpcCatalog$StaticNpc;)I right=ALOAD 0; paths rejected: normal control-flow cycle; labelled CFGs differ |
| `local/lito381/server/ClientSession#handleClassicWorldWaterDragonGateAction(Llocal/lito381/server/Protocol$HypertextActionRequest;Llocal/lito381/server/ClassicWorldNpcCatalog$StaticNpc;)V` | 缺呼叫 | method handleClassicWorldWaterDragonGateAction(Llocal/lito381/server/Protocol$HypertextActionRequest;Llocal/lito381/server/ClassicWorldNpcCatalog$StaticNpc;)V insn[3]: left=IF_ACMPNE L29 right=IF_ACMPNE L31; path event multisets differ (left=2032, right=2032); labelled CFGs differ |
| `local/lito381/server/ClientSession#handleGmChatCommandBeforeTravel(Ljava/lang/String;)Z` | 缺呼叫 | method handleGmChatCommandBeforeTravel(Ljava/lang/String;)Z: left=41:[45:java/lang/NumberFormatException  ] right=47:[51:java/lang/NumberFormatException  ]; paths rejected: normal control-flow cycle; labelled CFGs differ |
| `local/lito381/server/ClientSession#handleIvoryTowerElfAttributeAction(Llocal/lito381/server/Protocol$HypertextActionRequest;Llocal/lito381/server/ClassicWorldNpcCatalog$StaticNpc;)Z` | 缺呼叫 | method handleIvoryTowerElfAttributeAction(Llocal/lito381/server/Protocol$HypertextActionRequest;Llocal/lito381/server/ClassicWorldNpcCatalog$StaticNpc;)Z insn[72]: left=IF_ACMPNE L101 right=IF_ACMPNE L103; paths rejected: path limit 4096 exceeded; labelled CFGs differ |
| `local/lito381/server/ClientSession#handleIvoryTowerPriceAction(Llocal/lito381/server/Protocol$HypertextActionRequest;Llocal/lito381/server/ClassicWorldNpcCatalog$StaticNpc;)Z` | 缺呼叫 | method handleIvoryTowerPriceAction(Llocal/lito381/server/Protocol$HypertextActionRequest;Llocal/lito381/server/ClassicWorldNpcCatalog$StaticNpc;)Z insn[49]: left=IF_ICMPLT L79 right=IF_ICMPLT L81; path event multisets differ (left=2863, right=2863); labelled CFGs differ |
| `local/lito381/server/ClientSession#handlePendingRewardAction(Llocal/lito381/server/Protocol$HypertextActionRequest;)V` | 缺呼叫 | method handlePendingRewardAction(Llocal/lito381/server/Protocol$HypertextActionRequest;)V insn[98]: left=ALOAD 5 right=ALOAD 0; paths rejected: path limit 4096 exceeded; labelled CFGs differ |
| `local/lito381/server/ClientSession#handleSilentCavernFullHeal(Llocal/lito381/server/Protocol$HypertextActionRequest;Llocal/lito381/server/ClassicWorldNpcCatalog$StaticNpc;)Z` | 缺呼叫 | method handleSilentCavernFullHeal(Llocal/lito381/server/Protocol$HypertextActionRequest;Llocal/lito381/server/ClassicWorldNpcCatalog$StaticNpc;)Z insn[15]: left=IF_ACMPNE L41 right=IF_ACMPNE L43; path event multisets differ (left=291, right=291); labelled CFGs differ |
| `local/lito381/server/ClientSession#hypertextAction([B)V` | 缺呼叫 | method hypertextAction([B)V insn[422]: left=ALOAD 2 right=INVOKESTATIC local/lito381/server/NativePlayerCommit$Binding.capture(Llocal/lito381/server/ClientSession;)Llocal/lito381/server/NativePlayerCommit$Binding;; paths rejected: normal control-flow cycle; labelled CFGs differ |
| `local/lito381/server/ClientSession#hypertextInputResult([B)V` | 缺呼叫 | method hypertextInputResult([B)V: left=500:[539:java/lang/Exception  ] right=504:[543:java/lang/Exception  ]; paths rejected: path limit 4096 exceeded; labelled CFGs differ |
| `local/lito381/server/ClientSession#loadPersistentState()V` | 順序不同 | method loadPersistentState()V: left=557:[576:java/lang/Exception  ] right=556:[576:java/lang/Exception  ]; paths rejected: path limit 4096 exceeded; labelled CFGs differ |
| `local/lito381/server/ClientSession#loadSqlTournamentProfiles()V` | 缺呼叫 | method loadSqlTournamentProfiles()V: left=1:[5:java/lang/Exception  ] right=3:[7:java/lang/Exception  ]; paths rejected: normal control-flow cycle; labelled CFGs differ |
| `local/lito381/server/ClientSession#maintainNativeMagicDollAutoSkills(JLjava/lang/String;)V` | 缺呼叫 | method maintainNativeMagicDollAutoSkills(JLjava/lang/String;)V insn[21]: left=IFEQ L367 right=IFEQ L371; paths rejected: normal control-flow cycle; labelled CFGs differ |
| `local/lito381/server/ClientSession#move([B)V` | 缺呼叫 | method move([B)V insn[45]: left=INVOKESPECIAL local/lito381/server/ClientSession.rejectWhileParalyzed(Ljava/lang/String;)Z right=INVOKESPECIAL local/lito381/server/ClientSession.nativeRejectMovementLocked(Ljava/lang/String;)Z; paths rejected: normal control-flow cycle; labelled CFGs differ |
| `local/lito381/server/ClientSession#nativeAreaBattleSleepBody([BLlocal/lito381/server/BattleSpellCatalog$SleepSpell;)V` | 缺呼叫 | method nativeAreaBattleSleepBody([BLlocal/lito381/server/BattleSpellCatalog$SleepSpell;)V: length left=0 right=9; paths rejected: normal control-flow cycle; labelled CFGs differ |
| `local/lito381/server/ClientSession#nativeAreaBattleSlowBody([BLlocal/lito381/server/BattleSpellCatalog$MassSlowSpell;)V` | 缺呼叫 | method nativeAreaBattleSlowBody([BLlocal/lito381/server/BattleSpellCatalog$MassSlowSpell;)V: length left=0 right=24; paths rejected: normal control-flow cycle; labelled CFGs differ |
| `local/lito381/server/ClientSession#nativeAreaFreezeAcceptedBattle(Llocal/lito381/server/BattleSpellCatalog$FreezeAttackSpell;Llocal/lito381/server/ClientSession;Ljava/util/List;)V` | 純形狀待裁定 | method nativeAreaFreezeAcceptedBattle(Llocal/lito381/server/BattleSpellCatalog$FreezeAttackSpell;Llocal/lito381/server/ClientSession;Ljava/util/List;)V insn[8]: left=DUP right=IFEQ L11; path event multisets differ (left=18, right=32); labelled CFGs differ |
| `local/lito381/server/ClientSession#nativeAreaFreezeAcceptedWorld(Llocal/lito381/server/BattleSpellCatalog$FreezeAttackSpell;Llocal/lito381/server/ClientSession;Ljava/util/List;)V` | 純形狀待裁定 | method nativeAreaFreezeAcceptedWorld(Llocal/lito381/server/BattleSpellCatalog$FreezeAttackSpell;Llocal/lito381/server/ClientSession;Ljava/util/List;)V insn[8]: left=DUP right=IFEQ L11; path event multisets differ (left=18, right=32); labelled CFGs differ |
| `local/lito381/server/ClientSession#nativeAreaFreezeOriginalWorld([BLlocal/lito381/server/BattleSpellCatalog$FreezeAttackSpell;)V` | 缺呼叫 | method nativeAreaFreezeOriginalWorld([BLlocal/lito381/server/BattleSpellCatalog$FreezeAttackSpell;)V: left=78:[302:java/lang/Exception  ] right=80:[308:java/lang/Exception  ]; paths rejected: normal control-flow cycle; labelled CFGs differ |
| `local/lito381/server/ClientSession#nativeAreaWorldSleepBody([BLlocal/lito381/server/BattleSpellCatalog$SleepSpell;)V` | 缺呼叫 | method nativeAreaWorldSleepBody([BLlocal/lito381/server/BattleSpellCatalog$SleepSpell;)V: length left=0 right=6; paths rejected: normal control-flow cycle; labelled CFGs differ |
| `local/lito381/server/ClientSession#nativeAreaWorldSlowBody([BLlocal/lito381/server/BattleSpellCatalog$MassSlowSpell;)V` | 缺呼叫 | method nativeAreaWorldSlowBody([BLlocal/lito381/server/BattleSpellCatalog$MassSlowSpell;)V: length left=0 right=58; paths rejected: normal control-flow cycle; labelled CFGs differ |
| `local/lito381/server/ClientSession#nativeBattleControlledCancellationBody(Llocal/lito381/server/Protocol$ObjectSpellRequest;Llocal/lito381/server/ClientSession;Llocal/lito381/server/BattleCompanion;Llocal/lito381/server/BattleCompanion$Snapshot;Llocal/lito381/server/ClassicWorldMonsterPopulation$Snapshot;Llocal/lito381/server/BattleSpellCatalog$CancellationSpell;Llocal/lito381/server/CombatPoisonLifecycle$Scope;)V` | 缺呼叫 | method nativeBattleControlledCancellationBody(Llocal/lito381/server/Protocol$ObjectSpellRequest;Llocal/lito381/server/ClientSession;Llocal/lito381/server/BattleCompanion;Llocal/lito381/server/BattleCompanion$Snapshot;Llocal/lito381/server/ClassicWorldMonsterPopulation$Snapshot;Llocal/lito381/server/BattleSpellCatalog$CancellationSpell;Llocal/lito381/server/CombatPoisonLifecycle$Scope;)V: length left=82 right=0; paths rejected: normal control-flow cycle; labelled CFGs differ |
| `local/lito381/server/ClientSession#nativeCompanionTargetuseClassicWorldAreaAttackSpell(Llocal/lito381/server/Protocol$AttackSpellRequest;Llocal/lito381/server/BattleSpellCatalog$AreaAttackSpell;)V` | 缺呼叫 | method nativeCompanionTargetuseClassicWorldAreaAttackSpell(Llocal/lito381/server/Protocol$AttackSpellRequest;Llocal/lito381/server/BattleSpellCatalog$AreaAttackSpell;)V: left=518:[687:java/lang/Exception  ] right=542:[715:java/lang/Exception  ]; paths rejected: normal control-flow cycle; labelled CFGs differ |
| `local/lito381/server/ClientSession#nativeCompanionTargetuseClassicWorldAttackSpell(Llocal/lito381/server/Protocol$AttackSpellRequest;Llocal/lito381/server/BattleSpellCatalog$AttackSpell;)V` | 順序不同 | method nativeCompanionTargetuseClassicWorldAttackSpell(Llocal/lito381/server/Protocol$AttackSpellRequest;Llocal/lito381/server/BattleSpellCatalog$AttackSpell;)V: left=626:[631:java/lang/Exception  ] right=630:[635:java/lang/Exception  ]; paths rejected: normal control-flow cycle; labelled CFGs differ |
| `local/lito381/server/ClientSession#nativeCompanionTargetuseClassicWorldLineAttackSpell([BLlocal/lito381/server/BattleSpellCatalog$LineAttackSpell;)V` | 缺呼叫 | method nativeCompanionTargetuseClassicWorldLineAttackSpell([BLlocal/lito381/server/BattleSpellCatalog$LineAttackSpell;)V: left=424:[680:java/lang/Exception  ] right=437:[697:java/lang/Exception  ]; paths rejected: normal control-flow cycle; labelled CFGs differ |
| `local/lito381/server/ClientSession#nativeCubeHarmonyBody([BLlocal/lito381/server/BattleSpellCatalog$CubeHarmonySpell;)V` | 缺呼叫 | method nativeCubeHarmonyBody([BLlocal/lito381/server/BattleSpellCatalog$CubeHarmonySpell;)V insn[0]: left=ALOAD 0 right=ALOAD 1; paths rejected: normal control-flow cycle; labelled CFGs differ |
| `local/lito381/server/ClientSession#nativeCubeIgnitionBody([BLlocal/lito381/server/BattleSpellCatalog$CubeIgnitionSpell;)V` | 缺呼叫 | method nativeCubeIgnitionBody([BLlocal/lito381/server/BattleSpellCatalog$CubeIgnitionSpell;)V insn[0]: left=ALOAD 0 right=ALOAD 1; paths rejected: normal control-flow cycle; labelled CFGs differ |
| `local/lito381/server/ClientSession#nativeCubeQuakeBody([BLlocal/lito381/server/BattleSpellCatalog$CubeQuakeSpell;)V` | 缺呼叫 | method nativeCubeQuakeBody([BLlocal/lito381/server/BattleSpellCatalog$CubeQuakeSpell;)V insn[0]: left=ALOAD 0 right=ALOAD 1; paths rejected: normal control-flow cycle; labelled CFGs differ |
| `local/lito381/server/ClientSession#nativeCubeShockBody([BLlocal/lito381/server/BattleSpellCatalog$CubeShockSpell;)V` | 缺呼叫 | method nativeCubeShockBody([BLlocal/lito381/server/BattleSpellCatalog$CubeShockSpell;)V insn[0]: left=ALOAD 0 right=ALOAD 1; paths rejected: normal control-flow cycle; labelled CFGs differ |
| `local/lito381/server/ClientSession#nativeFreezeAcceptedOriginalWorld([BLlocal/lito381/server/BattleSpellCatalog$FreezeAttackSpell;)V` | 順序不同 | method nativeFreezeAcceptedOriginalWorld([BLlocal/lito381/server/BattleSpellCatalog$FreezeAttackSpell;)V: left=329:[334:java/lang/Exception  ] right=342:[347:java/lang/Exception  ]; paths rejected: normal control-flow cycle; labelled CFGs differ |
| `local/lito381/server/ClientSession#nativeMassTeleportAnswerBody(Llocal/lito381/server/Protocol$AskAnswer;)V` | 缺呼叫 | method nativeMassTeleportAnswerBody(Llocal/lito381/server/Protocol$AskAnswer;)V insn[1]: left=ALOAD 1 right=GETFIELD local/lito381/server/ClientSession.pendingMassTeleport:Llocal/lito381/server/ClientSession$PendingMassTeleport;; paths rejected: normal control-flow cycle; labelled CFGs differ |
| `local/lito381/server/ClientSession#nativeMaterialAbsoluteBarrierBody([BLlocal/lito381/server/BattleSpellCatalog$AbsoluteBarrierSpell;)V` | 缺呼叫 | method nativeMaterialAbsoluteBarrierBody([BLlocal/lito381/server/BattleSpellCatalog$AbsoluteBarrierSpell;)V insn[0]: left=ALOAD 0 right=ALOAD 1; paths rejected: normal control-flow cycle; labelled CFGs differ |
| `local/lito381/server/ClientSession#nativeMaterialAdditionalFireBody([BLlocal/lito381/server/BattleSpellCatalog$AdditionalFireSpell;)V` | 缺呼叫 | method nativeMaterialAdditionalFireBody([BLlocal/lito381/server/BattleSpellCatalog$AdditionalFireSpell;)V insn[0]: left=ALOAD 0 right=ALOAD 1; paths rejected: normal control-flow cycle; labelled CFGs differ |
| `local/lito381/server/ClientSession#nativeMaterialAreaSilenceBody([BLlocal/lito381/server/BattleSpellCatalog$AreaSilenceSpell;I)V` | 缺呼叫 | method nativeMaterialAreaSilenceBody([BLlocal/lito381/server/BattleSpellCatalog$AreaSilenceSpell;I)V insn[0]: left=ALOAD 0 right=ILOAD 3; paths rejected: normal control-flow cycle; labelled CFGs differ |
| `local/lito381/server/ClientSession#nativeMaterialAreaWindBody([BLlocal/lito381/server/BattleSpellCatalog$WindShackleSpell;I)V` | 缺呼叫 | method nativeMaterialAreaWindBody([BLlocal/lito381/server/BattleSpellCatalog$WindShackleSpell;I)V insn[0]: left=ALOAD 0 right=ILOAD 3; paths rejected: normal control-flow cycle; labelled CFGs differ |
| `local/lito381/server/ClientSession#nativeMaterialArmorBreakBody([BLlocal/lito381/server/BattleSpellCatalog$ArmorBreakSpell;)V` | 缺呼叫 | method nativeMaterialArmorBreakBody([BLlocal/lito381/server/BattleSpellCatalog$ArmorBreakSpell;)V: length left=0 right=2; paths rejected: normal control-flow cycle; labelled CFGs differ |
| `local/lito381/server/ClientSession#nativeMaterialAwakenBody([BLlocal/lito381/server/BattleSpellCatalog$AwakenSpell;)V` | 缺呼叫 | method nativeMaterialAwakenBody([BLlocal/lito381/server/BattleSpellCatalog$AwakenSpell;)V insn[0]: left=ALOAD 0 right=ALOAD 1; paths rejected: normal control-flow cycle; labelled CFGs differ |
| `local/lito381/server/ClientSession#nativeMaterialBlindBody([BLlocal/lito381/server/BattleSpellCatalog$BlindSpell;)V` | 缺呼叫 | method nativeMaterialBlindBody([BLlocal/lito381/server/BattleSpellCatalog$BlindSpell;)V: length left=0 right=2; paths rejected: normal control-flow cycle; labelled CFGs differ |
| `local/lito381/server/ClientSession#nativeMaterialBloodlustBody([BLlocal/lito381/server/BattleSpellCatalog$BloodlustSpell;)V` | 缺呼叫 | method nativeMaterialBloodlustBody([BLlocal/lito381/server/BattleSpellCatalog$BloodlustSpell;)V insn[0]: left=ALOAD 0 right=ALOAD 1; paths rejected: normal control-flow cycle; labelled CFGs differ |
| `local/lito381/server/ClientSession#nativeMaterialCallNatureBody([BLlocal/lito381/server/BattleSpellCatalog$ResurrectionSpell;)V` | 缺呼叫 | method nativeMaterialCallNatureBody([BLlocal/lito381/server/BattleSpellCatalog$ResurrectionSpell;)V insn[0]: left=ALOAD 0 right=ALOAD 1; paths rejected: normal control-flow cycle; labelled CFGs differ |
| `local/lito381/server/ClientSession#nativeMaterialCancellationBody([BLlocal/lito381/server/BattleSpellCatalog$CancellationSpell;Llocal/lito381/server/CombatPoisonLifecycle$Scope;)V` | 缺呼叫 | method nativeMaterialCancellationBody([BLlocal/lito381/server/BattleSpellCatalog$CancellationSpell;Llocal/lito381/server/CombatPoisonLifecycle$Scope;)V insn[0]: left=ALOAD 0 right=CONST null; paths rejected: normal control-flow cycle; labelled CFGs differ |
| `local/lito381/server/ClientSession#nativeMaterialClearMindBody([BLlocal/lito381/server/BattleSpellCatalog$ClearMindSpell;)V` | 缺呼叫 | method nativeMaterialClearMindBody([BLlocal/lito381/server/BattleSpellCatalog$ClearMindSpell;)V insn[0]: left=ALOAD 0 right=ALOAD 1; paths rejected: normal control-flow cycle; labelled CFGs differ |
| `local/lito381/server/ClientSession#nativeMaterialCounterMagicBody([BLlocal/lito381/server/BattleSpellCatalog$CounterMagicSpell;)V` | 缺呼叫 | method nativeMaterialCounterMagicBody([BLlocal/lito381/server/BattleSpellCatalog$CounterMagicSpell;)V insn[0]: left=ALOAD 0 right=ALOAD 1; paths rejected: normal control-flow cycle; labelled CFGs differ |
| `local/lito381/server/ClientSession#nativeMaterialCounterMirrorBody([BLlocal/lito381/server/BattleSpellCatalog$CounterMirrorSpell;)V` | 缺呼叫 | method nativeMaterialCounterMirrorBody([BLlocal/lito381/server/BattleSpellCatalog$CounterMirrorSpell;)V insn[0]: left=ALOAD 0 right=ALOAD 1; paths rejected: normal control-flow cycle; labelled CFGs differ |
| `local/lito381/server/ClientSession#nativeMaterialDecayPotionBody([BLlocal/lito381/server/BattleSpellCatalog$DecayPotionSpell;)V` | 缺呼叫 | method nativeMaterialDecayPotionBody([BLlocal/lito381/server/BattleSpellCatalog$DecayPotionSpell;)V: length left=0 right=2; paths rejected: normal control-flow cycle; labelled CFGs differ |
| `local/lito381/server/ClientSession#nativeMaterialEarthBindBody([BLlocal/lito381/server/BattleSpellCatalog$BindSpell;)V` | 缺呼叫 | method nativeMaterialEarthBindBody([BLlocal/lito381/server/BattleSpellCatalog$BindSpell;)V insn[0]: left=ALOAD 0 right=ALOAD 1; paths rejected: normal control-flow cycle; labelled CFGs differ |
| `local/lito381/server/ClientSession#nativeMaterialElementalProtectionBody([BLlocal/lito381/server/BattleSpellCatalog$ElementalProtectionSpell;)V` | 缺呼叫 | method nativeMaterialElementalProtectionBody([BLlocal/lito381/server/BattleSpellCatalog$ElementalProtectionSpell;)V insn[0]: left=ALOAD 0 right=ALOAD 1; paths rejected: normal control-flow cycle; labelled CFGs differ |
| `local/lito381/server/ClientSession#nativeMaterialElementalSummonBody([BLlocal/lito381/server/BattleSpellCatalog$ElementalSummonSpell;)V` | 缺呼叫 | method nativeMaterialElementalSummonBody([BLlocal/lito381/server/BattleSpellCatalog$ElementalSummonSpell;)V: length left=0 right=8; paths rejected: normal control-flow cycle; labelled CFGs differ |
| `local/lito381/server/ClientSession#nativeMaterialEnchantVenomBody([BLlocal/lito381/server/BattleSpellCatalog$EnchantVenomSpell;)V` | 缺呼叫 | method nativeMaterialEnchantVenomBody([BLlocal/lito381/server/BattleSpellCatalog$EnchantVenomSpell;)V insn[0]: left=ALOAD 0 right=ALOAD 1; paths rejected: normal control-flow cycle; labelled CFGs differ |
| `local/lito381/server/ClientSession#nativeMaterialEntangleBody([BLlocal/lito381/server/BattleSpellCatalog$EntangleSpell;I)V` | 缺呼叫 | method nativeMaterialEntangleBody([BLlocal/lito381/server/BattleSpellCatalog$EntangleSpell;I)V: length left=0 right=2; paths rejected: normal control-flow cycle; labelled CFGs differ |
| `local/lito381/server/ClientSession#nativeMaterialEraseBody([BLlocal/lito381/server/BattleSpellCatalog$EraseMagicSpell;I)V` | 缺呼叫 | method nativeMaterialEraseBody([BLlocal/lito381/server/BattleSpellCatalog$EraseMagicSpell;I)V: length left=0 right=2; paths rejected: normal control-flow cycle; labelled CFGs differ |
| `local/lito381/server/ClientSession#nativeMaterialExoticVitalizeBody([BLlocal/lito381/server/BattleSpellCatalog$ExoticVitalizeSpell;)V` | 缺呼叫 | method nativeMaterialExoticVitalizeBody([BLlocal/lito381/server/BattleSpellCatalog$ExoticVitalizeSpell;)V insn[0]: left=ALOAD 0 right=ALOAD 1; paths rejected: normal control-flow cycle; labelled CFGs differ |
| `local/lito381/server/ClientSession#nativeMaterialGroundBody([BLlocal/lito381/server/BattleSpellCatalog$GroundSpell;)V` | 缺呼叫 | method nativeMaterialGroundBody([BLlocal/lito381/server/BattleSpellCatalog$GroundSpell;)V insn[0]: left=ALOAD 0 right=ALOAD 1; paths rejected: normal control-flow cycle; labelled CFGs differ |
| `local/lito381/server/ClientSession#nativeMaterialInvisibilityBody([BLlocal/lito381/server/BattleSpellCatalog$InvisibilitySpell;)V` | 缺呼叫 | method nativeMaterialInvisibilityBody([BLlocal/lito381/server/BattleSpellCatalog$InvisibilitySpell;)V insn[0]: left=ALOAD 0 right=CONST null; paths rejected: normal control-flow cycle; labelled CFGs differ |
| `local/lito381/server/ClientSession#nativeMaterialItemWeaponEnchantBody([BLlocal/lito381/server/BattleSpellCatalog$ItemWeaponEnchantSpell;)V` | 缺呼叫 | method nativeMaterialItemWeaponEnchantBody([BLlocal/lito381/server/BattleSpellCatalog$ItemWeaponEnchantSpell;)V insn[0]: left=ALOAD 0 right=ALOAD 1; paths rejected: normal control-flow cycle; labelled CFGs differ |
| `local/lito381/server/ClientSession#nativeMaterialMagicResistanceBody([BLlocal/lito381/server/BattleSpellCatalog$MagicResistanceSpell;)V` | 缺呼叫 | method nativeMaterialMagicResistanceBody([BLlocal/lito381/server/BattleSpellCatalog$MagicResistanceSpell;)V insn[0]: left=ALOAD 0 right=ALOAD 1; paths rejected: normal control-flow cycle; labelled CFGs differ |
| `local/lito381/server/ClientSession#nativeMaterialMagicalWeaponBody([BLlocal/lito381/server/BattleSpellCatalog$MagicalWeaponSpell;)V` | 缺呼叫 | method nativeMaterialMagicalWeaponBody([BLlocal/lito381/server/BattleSpellCatalog$MagicalWeaponSpell;)V insn[0]: left=ALOAD 0 right=ALOAD 1; paths rejected: normal control-flow cycle; labelled CFGs differ |
| `local/lito381/server/ClientSession#nativeMaterialMassTeleportBody([BLlocal/lito381/server/BattleSpellCatalog$MassTeleportSpell;)V` | 缺呼叫 | method nativeMaterialMassTeleportBody([BLlocal/lito381/server/BattleSpellCatalog$MassTeleportSpell;)V: length left=81 right=0; paths rejected: normal control-flow cycle; labelled CFGs differ |
| `local/lito381/server/ClientSession#nativeMaterialParalysisScopeBody([BLlocal/lito381/server/BattleSpellCatalog$ParalysisSpell;Llocal/lito381/server/CombatPoisonLifecycle$Scope;)V` | 缺呼叫 | method nativeMaterialParalysisScopeBody([BLlocal/lito381/server/BattleSpellCatalog$ParalysisSpell;Llocal/lito381/server/CombatPoisonLifecycle$Scope;)V: length left=0 right=2; paths rejected: normal control-flow cycle; labelled CFGs differ |
| `local/lito381/server/ClientSession#nativeMaterialPhysicalProcBuffBody([BLlocal/lito381/server/BattleSpellCatalog$PhysicalProcBuffSpell;)V` | 缺呼叫 | method nativeMaterialPhysicalProcBuffBody([BLlocal/lito381/server/BattleSpellCatalog$PhysicalProcBuffSpell;)V insn[0]: left=ALOAD 0 right=ALOAD 1; paths rejected: normal control-flow cycle; labelled CFGs differ |
| `local/lito381/server/ClientSession#nativeMaterialPoisonScopeBody([BLlocal/lito381/server/BattleSpellCatalog$PoisonSpell;Llocal/lito381/server/CombatPoisonLifecycle$Scope;)V` | 缺呼叫 | method nativeMaterialPoisonScopeBody([BLlocal/lito381/server/BattleSpellCatalog$PoisonSpell;Llocal/lito381/server/CombatPoisonLifecycle$Scope;)V: length left=0 right=2; paths rejected: normal control-flow cycle; labelled CFGs differ |
| `local/lito381/server/ClientSession#nativeMaterialPolymorphBody([BLlocal/lito381/server/BattleSpellCatalog$PolymorphSpell;)V` | 缺呼叫 | method nativeMaterialPolymorphBody([BLlocal/lito381/server/BattleSpellCatalog$PolymorphSpell;)V insn[0]: left=ALOAD 0 right=ALOAD 1; paths rejected: normal control-flow cycle; labelled CFGs differ |
| `local/lito381/server/ClientSession#nativeMaterialProtectionBody([BLlocal/lito381/server/BattleSpellCatalog$DamageProtectionSpell;)V` | 缺呼叫 | method nativeMaterialProtectionBody([BLlocal/lito381/server/BattleSpellCatalog$DamageProtectionSpell;)V insn[0]: left=ALOAD 0 right=ALOAD 1; paths rejected: normal control-flow cycle; labelled CFGs differ |
| `local/lito381/server/ClientSession#nativeMaterialResistElementalBody([BLlocal/lito381/server/BattleSpellCatalog$ResistElementalSpell;)V` | 缺呼叫 | method nativeMaterialResistElementalBody([BLlocal/lito381/server/BattleSpellCatalog$ResistElementalSpell;)V insn[0]: left=ALOAD 0 right=ALOAD 1; paths rejected: normal control-flow cycle; labelled CFGs differ |
| `local/lito381/server/ClientSession#nativeMaterialResurrectionBody([BLlocal/lito381/server/BattleSpellCatalog$ResurrectionSpell;)V` | 缺呼叫 | method nativeMaterialResurrectionBody([BLlocal/lito381/server/BattleSpellCatalog$ResurrectionSpell;)V insn[0]: left=ALOAD 0 right=ALOAD 1; paths rejected: normal control-flow cycle; labelled CFGs differ |
| `local/lito381/server/ClientSession#nativeMaterialReturnNatureBody([BLlocal/lito381/server/BattleSpellCatalog$ReturnToNatureSpell;I)V` | 缺呼叫 | method nativeMaterialReturnNatureBody([BLlocal/lito381/server/BattleSpellCatalog$ReturnToNatureSpell;I)V: length left=0 right=3; paths rejected: normal control-flow cycle; labelled CFGs differ |
| `local/lito381/server/ClientSession#nativeMaterialSilenceBody([BLlocal/lito381/server/BattleSpellCatalog$SilenceSpell;)V` | 缺呼叫 | method nativeMaterialSilenceBody([BLlocal/lito381/server/BattleSpellCatalog$SilenceSpell;)V: length left=0 right=2; paths rejected: normal control-flow cycle; labelled CFGs differ |
| `local/lito381/server/ClientSession#nativeMaterialSummonBody([BLlocal/lito381/server/BattleSpellCatalog$SummonSpell;)V` | 缺呼叫 | method nativeMaterialSummonBody([BLlocal/lito381/server/BattleSpellCatalog$SummonSpell;)V: length left=0 right=10; paths rejected: normal control-flow cycle; labelled CFGs differ |
| `local/lito381/server/ClientSession#nativeMaterialTamingBody([BLlocal/lito381/server/BattleSpellCatalog$TamingSpell;)V` | 缺呼叫 | method nativeMaterialTamingBody([BLlocal/lito381/server/BattleSpellCatalog$TamingSpell;)V: length left=0 right=2; paths rejected: normal control-flow cycle; labelled CFGs differ |
| `local/lito381/server/ClientSession#nativeMaterialTargetNegativeBody([BLlocal/lito381/server/BattleSpellCatalog$TargetNegativeStatusSpell;ILlocal/lito381/server/ClientSession$TargetNegativeStatusKind;)V` | 缺呼叫 | method nativeMaterialTargetNegativeBody([BLlocal/lito381/server/BattleSpellCatalog$TargetNegativeStatusSpell;ILlocal/lito381/server/ClientSession$TargetNegativeStatusKind;)V: left=491:[496:java/lang/Exception  ] right=523:[528:java/lang/Exception  ]; paths rejected: normal control-flow cycle; labelled CFGs differ |
| `local/lito381/server/ClientSession#nativeMaterialWaterLifeBody([BLlocal/lito381/server/BattleSpellCatalog$WaterLifeSpell;)V` | 缺呼叫 | method nativeMaterialWaterLifeBody([BLlocal/lito381/server/BattleSpellCatalog$WaterLifeSpell;)V insn[0]: left=ALOAD 0 right=ALOAD 1; paths rejected: normal control-flow cycle; labelled CFGs differ |
| `local/lito381/server/ClientSession#nativeMaterialWeaknessBody([BLlocal/lito381/server/BattleSpellCatalog$WeaknessSpell;)V` | 缺呼叫 | method nativeMaterialWeaknessBody([BLlocal/lito381/server/BattleSpellCatalog$WeaknessSpell;)V: length left=0 right=2; paths rejected: normal control-flow cycle; labelled CFGs differ |
| `local/lito381/server/ClientSession#nativeMaterialWeaponBreakBody([BLlocal/lito381/server/BattleSpellCatalog$WeaponBreakSpell;)V` | 缺呼叫 | method nativeMaterialWeaponBreakBody([BLlocal/lito381/server/BattleSpellCatalog$WeaponBreakSpell;)V: length left=0 right=2; paths rejected: normal control-flow cycle; labelled CFGs differ |
| `local/lito381/server/ClientSession#nativeMaterialWorldBlindBody([BLlocal/lito381/server/BattleSpellCatalog$BlindSpell;)V` | 缺呼叫 | method nativeMaterialWorldBlindBody([BLlocal/lito381/server/BattleSpellCatalog$BlindSpell;)V: length left=0 right=2; paths rejected: normal control-flow cycle; labelled CFGs differ |
| `local/lito381/server/ClientSession#nativeMaterialWorldCancellationBody(Llocal/lito381/server/Protocol$ObjectSpellRequest;Llocal/lito381/server/ClientSession;Llocal/lito381/server/BattleSpellCatalog$CancellationSpell;Llocal/lito381/server/CombatPoisonLifecycle$Scope;)V` | 缺呼叫 | method nativeMaterialWorldCancellationBody(Llocal/lito381/server/Protocol$ObjectSpellRequest;Llocal/lito381/server/ClientSession;Llocal/lito381/server/BattleSpellCatalog$CancellationSpell;Llocal/lito381/server/CombatPoisonLifecycle$Scope;)V insn[2]: left=ALOAD 2 right=INVOKESTATIC local/lito381/server/NativeSelectedPlayerIntent.object(Llocal/lito381/server/ClientSession;Llocal/lito381/server/Protocol$ObjectSpellRequest;)Z; paths rejected: normal control-flow cycle; labelled CFGs differ |
| `local/lito381/server/ClientSession#nativeMaterialWorldDecayPotionBody([BLlocal/lito381/server/BattleSpellCatalog$DecayPotionSpell;)V` | 缺呼叫 | method nativeMaterialWorldDecayPotionBody([BLlocal/lito381/server/BattleSpellCatalog$DecayPotionSpell;)V: length left=0 right=2; paths rejected: normal control-flow cycle; labelled CFGs differ |
| `local/lito381/server/ClientSession#nativeMaterialWorldEarthBindBody([BLlocal/lito381/server/BattleSpellCatalog$BindSpell;)V` | 缺呼叫 | method nativeMaterialWorldEarthBindBody([BLlocal/lito381/server/BattleSpellCatalog$BindSpell;)V insn[0]: left=ALOAD 0 right=ALOAD 1; paths rejected: normal control-flow cycle; labelled CFGs differ |
| `local/lito381/server/ClientSession#nativeMaterialWorldGroundBody([BLlocal/lito381/server/BattleSpellCatalog$GroundSpell;)V` | 缺呼叫 | method nativeMaterialWorldGroundBody([BLlocal/lito381/server/BattleSpellCatalog$GroundSpell;)V insn[0]: left=ALOAD 0 right=ALOAD 1; paths rejected: normal control-flow cycle; labelled CFGs differ |
| `local/lito381/server/ClientSession#nativeMaterialWorldNpcCancellationBody(Llocal/lito381/server/Protocol$ObjectSpellRequest;Llocal/lito381/server/ClientSession;Llocal/lito381/server/BattleCompanion;Llocal/lito381/server/BattleCompanion$Snapshot;Llocal/lito381/server/ClassicWorldMonsterPopulation$Snapshot;Llocal/lito381/server/BattleSpellCatalog$CancellationSpell;Llocal/lito381/server/CombatPoisonLifecycle$Scope;)V` | 缺呼叫 | method nativeMaterialWorldNpcCancellationBody(Llocal/lito381/server/Protocol$ObjectSpellRequest;Llocal/lito381/server/ClientSession;Llocal/lito381/server/BattleCompanion;Llocal/lito381/server/BattleCompanion$Snapshot;Llocal/lito381/server/ClassicWorldMonsterPopulation$Snapshot;Llocal/lito381/server/BattleSpellCatalog$CancellationSpell;Llocal/lito381/server/CombatPoisonLifecycle$Scope;)V insn[2]: left=ALOAD 2 right=INVOKESTATIC local/lito381/server/NativeSelectedPlayerIntent.object(Llocal/lito381/server/ClientSession;Llocal/lito381/server/Protocol$ObjectSpellRequest;)Z; paths rejected: normal control-flow cycle; labelled CFGs differ |
| `local/lito381/server/ClientSession#nativeMaterialWorldParalysisScopeBody([BLlocal/lito381/server/BattleSpellCatalog$ParalysisSpell;Llocal/lito381/server/CombatPoisonLifecycle$Scope;)V` | 缺呼叫 | method nativeMaterialWorldParalysisScopeBody([BLlocal/lito381/server/BattleSpellCatalog$ParalysisSpell;Llocal/lito381/server/CombatPoisonLifecycle$Scope;)V: length left=0 right=2; paths rejected: normal control-flow cycle; labelled CFGs differ |
| `local/lito381/server/ClientSession#nativeMaterialWorldPoisonScopeBody([BLlocal/lito381/server/BattleSpellCatalog$PoisonSpell;Llocal/lito381/server/CombatPoisonLifecycle$Scope;)V` | 缺呼叫 | method nativeMaterialWorldPoisonScopeBody([BLlocal/lito381/server/BattleSpellCatalog$PoisonSpell;Llocal/lito381/server/CombatPoisonLifecycle$Scope;)V: length left=0 right=2; paths rejected: normal control-flow cycle; labelled CFGs differ |
| `local/lito381/server/ClientSession#nativeMaterialWorldResurrectionBody([BLlocal/lito381/server/BattleSpellCatalog$ResurrectionSpell;)V` | 缺呼叫 | method nativeMaterialWorldResurrectionBody([BLlocal/lito381/server/BattleSpellCatalog$ResurrectionSpell;)V insn[0]: left=ALOAD 0 right=ALOAD 1; paths rejected: normal control-flow cycle; labelled CFGs differ |
| `local/lito381/server/ClientSession#nativeMaterialWorldSilenceBody([BLlocal/lito381/server/BattleSpellCatalog$SilenceSpell;)V` | 缺呼叫 | method nativeMaterialWorldSilenceBody([BLlocal/lito381/server/BattleSpellCatalog$SilenceSpell;)V: length left=0 right=2; paths rejected: normal control-flow cycle; labelled CFGs differ |
| `local/lito381/server/ClientSession#nativeMaterialWorldWeaknessBody([BLlocal/lito381/server/BattleSpellCatalog$WeaknessSpell;)V` | 缺呼叫 | method nativeMaterialWorldWeaknessBody([BLlocal/lito381/server/BattleSpellCatalog$WeaknessSpell;)V: length left=0 right=2; paths rejected: normal control-flow cycle; labelled CFGs differ |
| `local/lito381/server/ClientSession#nativeMaterialWorldWeaponBreakBody([BLlocal/lito381/server/BattleSpellCatalog$WeaponBreakSpell;)V` | 缺呼叫 | method nativeMaterialWorldWeaponBreakBody([BLlocal/lito381/server/BattleSpellCatalog$WeaponBreakSpell;)V: length left=0 right=2; paths rejected: normal control-flow cycle; labelled CFGs differ |
| `local/lito381/server/ClientSession#nativeMaterialZombieBody([BLlocal/lito381/server/BattleSpellCatalog$CreateZombieSpell;)V` | 缺呼叫 | method nativeMaterialZombieBody([BLlocal/lito381/server/BattleSpellCatalog$CreateZombieSpell;)V: length left=0 right=7; paths rejected: normal control-flow cycle; labelled CFGs differ |
| `local/lito381/server/ClientSession#nativePlacementPortalBody(Llocal/lito381/server/Protocol$PortalDestination;Llocal/lito381/server/ClientSession$PendingClassicWorldPortal;)V` | 純形狀待裁定 | method nativePlacementPortalBody(Llocal/lito381/server/Protocol$PortalDestination;Llocal/lito381/server/ClientSession$PendingClassicWorldPortal;)V insn[77]: left=DUP right=ISTORE 6; paths rejected: path limit 4096 exceeded; labelled CFGs differ |
| `local/lito381/server/ClientSession#nativeRawOriginal_useLightningWand(Llocal/lito381/server/RuntimeInventory$Item;Llocal/lito381/server/Protocol$TargetLocationInventoryUse;)V` | 順序不同 | method nativeRawOriginal_useLightningWand(Llocal/lito381/server/RuntimeInventory$Item;Llocal/lito381/server/Protocol$TargetLocationInventoryUse;)V insn[1]: left=IFNULL L640 right=IFNULL L17; paths rejected: normal control-flow cycle; labelled CFGs differ |
| `local/lito381/server/ClientSession#nativeResurrectionAnswerBody(Llocal/lito381/server/Protocol$AskAnswer;)V` | 缺呼叫 | method nativeResurrectionAnswerBody(Llocal/lito381/server/Protocol$AskAnswer;)V insn[1]: left=ALOAD 1 right=GETFIELD local/lito381/server/ClientSession.pendingResurrection:Llocal/lito381/server/ClientSession$PendingResurrection;; paths rejected: normal control-flow cycle; labelled CFGs differ |
| `local/lito381/server/ClientSession#nativeSelfShieldBody([BLlocal/lito381/server/BattleSpellCatalog$ShieldSpell;)V` | 缺呼叫 | method nativeSelfShieldBody([BLlocal/lito381/server/BattleSpellCatalog$ShieldSpell;)V insn[0]: left=ALOAD 0 right=ALOAD 2; paths rejected: normal control-flow cycle; labelled CFGs differ |
| `local/lito381/server/ClientSession#nativeSelfWeightBody([BLlocal/lito381/server/BattleSpellCatalog$WeightSpell;)V` | 缺呼叫 | method nativeSelfWeightBody([BLlocal/lito381/server/BattleSpellCatalog$WeightSpell;)V insn[0]: left=ALOAD 0 right=ALOAD 1; paths rejected: normal control-flow cycle; labelled CFGs differ |
| `local/lito381/server/ClientSession#nativeShockPlayerBattleBody([BLlocal/lito381/server/BattleSpellCatalog$ShockStunSpell;)V` | 順序不同 | method nativeShockPlayerBattleBody([BLlocal/lito381/server/BattleSpellCatalog$ShockStunSpell;)V: left=289:[294:java/lang/Exception  ] right=314:[319:java/lang/Exception  ]; paths rejected: normal control-flow cycle; labelled CFGs differ |
| `local/lito381/server/ClientSession#nativeShockPlayerOriginalBattle([BLlocal/lito381/server/BattleSpellCatalog$ShockStunSpell;)V` | 順序不同 | method nativeShockPlayerOriginalBattle([BLlocal/lito381/server/BattleSpellCatalog$ShockStunSpell;)V: left=289:[294:java/lang/Exception  ] right=314:[319:java/lang/Exception  ]; paths rejected: normal control-flow cycle; labelled CFGs differ |
| `local/lito381/server/ClientSession#nativeShockPlayerOriginalWorld([BLlocal/lito381/server/BattleSpellCatalog$ShockStunSpell;)V` | 順序不同 | method nativeShockPlayerOriginalWorld([BLlocal/lito381/server/BattleSpellCatalog$ShockStunSpell;)V: left=307:[312:java/lang/Exception  ] right=320:[325:java/lang/Exception  ]; paths rejected: normal control-flow cycle; labelled CFGs differ |
| `local/lito381/server/ClientSession#nativeShockPlayerWorldBody([BLlocal/lito381/server/BattleSpellCatalog$ShockStunSpell;)V` | 順序不同 | method nativeShockPlayerWorldBody([BLlocal/lito381/server/BattleSpellCatalog$ShockStunSpell;)V: left=307:[312:java/lang/Exception  ] right=319:[324:java/lang/Exception  ]; paths rejected: normal control-flow cycle; labelled CFGs differ |
| `local/lito381/server/ClientSession#nativeSourceCompanionClockTick(JJLjava/lang/Object;)V` | 純形狀待裁定 | method nativeSourceCompanionClockTick(JJLjava/lang/Object;)V: left=50:[58:java/lang/Throwable  ] right=48:[56:java/lang/Throwable  ]; paths rejected: normal control-flow cycle; labelled CFGs differ |
| `local/lito381/server/ClientSession#nativeSourceControlledOriginal_attack(Llocal/lito381/server/Protocol$AttackRequest;Llocal/lito381/server/ClientSession$AttackEnvelope;)V` | 順序不同 | method nativeSourceControlledOriginal_attack(Llocal/lito381/server/Protocol$AttackRequest;Llocal/lito381/server/ClientSession$AttackEnvelope;)V: left=514:[519:java/lang/Exception  ] right=546:[551:java/lang/Exception  ]; paths rejected: normal control-flow cycle; labelled CFGs differ |
| `local/lito381/server/ClientSession#nativeSourceControlledOriginal_nativeAreaFreezeOriginalBattle([BLlocal/lito381/server/BattleSpellCatalog$FreezeAttackSpell;)V` | 缺呼叫 | method nativeSourceControlledOriginal_nativeAreaFreezeOriginalBattle([BLlocal/lito381/server/BattleSpellCatalog$FreezeAttackSpell;)V: left=260:[266:java/lang/Exception  ] right=262:[268:java/lang/Exception  ]; paths rejected: normal control-flow cycle; labelled CFGs differ |
| `local/lito381/server/ClientSession#nativeSourceControlledOriginal_nativeFreezeAcceptedOriginalBattle([BLlocal/lito381/server/BattleSpellCatalog$FreezeAttackSpell;)V` | 順序不同 | method nativeSourceControlledOriginal_nativeFreezeAcceptedOriginalBattle([BLlocal/lito381/server/BattleSpellCatalog$FreezeAttackSpell;)V: left=489:[494:java/lang/Exception  ] right=508:[513:java/lang/Exception  ]; paths rejected: normal control-flow cycle; labelled CFGs differ |
| `local/lito381/server/ClientSession#nativeSourceControlledOriginal_nativeSourceLightningOriginalBody(Llocal/lito381/server/Protocol$AttackSpellRequest;Llocal/lito381/server/BattleSpellCatalog$DirectionalLineSpell;)V` | 缺呼叫 | method nativeSourceControlledOriginal_nativeSourceLightningOriginalBody(Llocal/lito381/server/Protocol$AttackSpellRequest;Llocal/lito381/server/BattleSpellCatalog$DirectionalLineSpell;)V: left=645:[769:java/lang/Throwable  ] right=663:[789:java/lang/Throwable  ]; paths rejected: normal control-flow cycle; labelled CFGs differ |
| `local/lito381/server/ClientSession#nativeSourceControlledOriginal_useAreaAttackSpell(Llocal/lito381/server/Protocol$AttackSpellRequest;Llocal/lito381/server/BattleSpellCatalog$AreaAttackSpell;)V` | 缺呼叫 | method nativeSourceControlledOriginal_useAreaAttackSpell(Llocal/lito381/server/Protocol$AttackSpellRequest;Llocal/lito381/server/BattleSpellCatalog$AreaAttackSpell;)V: left=728:[734:java/lang/Exception  ] right=757:[763:java/lang/Exception  ]; paths rejected: normal control-flow cycle; labelled CFGs differ |
| `local/lito381/server/ClientSession#nativeSourceControlledOriginal_useAttackSpell(Llocal/lito381/server/Protocol$AttackSpellRequest;Llocal/lito381/server/BattleSpellCatalog$AttackSpell;IIII)V` | 順序不同 | method nativeSourceControlledOriginal_useAttackSpell(Llocal/lito381/server/Protocol$AttackSpellRequest;Llocal/lito381/server/BattleSpellCatalog$AttackSpell;IIII)V: left=463:[468:java/lang/Exception  ] right=484:[489:java/lang/Exception  ]; paths rejected: normal control-flow cycle; labelled CFGs differ |
| `local/lito381/server/ClientSession#nativeSourceControlledOriginal_useCenteredAreaAttackSpell([BLlocal/lito381/server/BattleSpellCatalog$AreaAttackSpell;)V` | 缺呼叫 | method nativeSourceControlledOriginal_useCenteredAreaAttackSpell([BLlocal/lito381/server/BattleSpellCatalog$AreaAttackSpell;)V: left=162:[168:java/lang/Exception  ] right=166:[172:java/lang/Exception  ]; paths rejected: normal control-flow cycle; labelled CFGs differ |
| `local/lito381/server/ClientSession#nativeSourceControlledOriginal_useHealingSpell([BLlocal/lito381/server/BattleSpellCatalog$HealingSpell;)V` | 順序不同 | method nativeSourceControlledOriginal_useHealingSpell([BLlocal/lito381/server/BattleSpellCatalog$HealingSpell;)V: left=391:[396:java/lang/Exception  ] right=408:[413:java/lang/Exception  ]; paths rejected: normal control-flow cycle; labelled CFGs differ |
| `local/lito381/server/ClientSession#nativeSourceFireWeaponOriginalBody([BLlocal/lito381/server/BattleSpellCatalog$FireWeaponSpell;)V` | 順序不同 | method nativeSourceFireWeaponOriginalBody([BLlocal/lito381/server/BattleSpellCatalog$FireWeaponSpell;)V: left=213:[388:java/lang/Throwable  ] right=216:[393:java/lang/Throwable  ]; paths rejected: normal control-flow cycle; labelled CFGs differ |
| `local/lito381/server/ClientSession#nativeSourceHealingOriginalGroup([BLlocal/lito381/server/BattleSpellCatalog$GroupHealingSpell;)V` | 缺呼叫 | method nativeSourceHealingOriginalGroup([BLlocal/lito381/server/BattleSpellCatalog$GroupHealingSpell;)V: left=217:[353:java/lang/Throwable  ] right=220:[358:java/lang/Throwable  ]; paths rejected: normal control-flow cycle; labelled CFGs differ |
| `local/lito381/server/ClientSession#nativeSourceHealingOriginalNature([BLlocal/lito381/server/BattleSpellCatalog$NaturesBlessingSpell;I)V` | 缺呼叫 | method nativeSourceHealingOriginalNature([BLlocal/lito381/server/BattleSpellCatalog$NaturesBlessingSpell;I)V: left=274:[403:java/lang/Throwable  ] right=283:[414:java/lang/Throwable  ]; paths rejected: normal control-flow cycle; labelled CFGs differ |
| `local/lito381/server/ClientSession#nativeSourceRemainingOriginal133([BLlocal/lito381/server/BattleSpellCatalog$ElementalFallDownSpell;I)V` | 順序不同 | method nativeSourceRemainingOriginal133([BLlocal/lito381/server/BattleSpellCatalog$ElementalFallDownSpell;I)V: left=436:[441:java/lang/Exception  ] right=455:[460:java/lang/Exception  ]; paths rejected: normal control-flow cycle; labelled CFGs differ |
| `local/lito381/server/ClientSession#nativeSourceRemainingOriginal18([BLlocal/lito381/server/BattleSpellCatalog$TurnUndeadSpell;)V` | 順序不同 | method nativeSourceRemainingOriginal18([BLlocal/lito381/server/BattleSpellCatalog$TurnUndeadSpell;)V: left=231:[236:java/lang/Exception  ] right=239:[244:java/lang/Exception  ]; paths rejected: normal control-flow cycle; labelled CFGs differ |
| `local/lito381/server/ClientSession#nativeSourceRemainingOriginal39([BLlocal/lito381/server/BattleSpellCatalog$ManaDrainSpell;)V` | 順序不同 | method nativeSourceRemainingOriginal39([BLlocal/lito381/server/BattleSpellCatalog$ManaDrainSpell;)V: left=288:[293:java/lang/Exception  ] right=291:[296:java/lang/Exception  ]; paths rejected: normal control-flow cycle; labelled CFGs differ |
| `local/lito381/server/ClientSession#nativeSourceSingleNpcLifeDrain(Llocal/lito381/server/ClientSession;Llocal/lito381/server/BattleSpellCatalog$AttackSpell;ILjava/lang/String;Llocal/lito381/server/NativeSourcePhysicalNpc$Commit;)I` | 純形狀待裁定 | method nativeSourceSingleNpcLifeDrain(Llocal/lito381/server/ClientSession;Llocal/lito381/server/BattleSpellCatalog$AttackSpell;ILjava/lang/String;Llocal/lito381/server/NativeSourcePhysicalNpc$Commit;)I: left=14:[41:java/lang/Throwable  ] right=12:[40:java/lang/Throwable  ]; path event multisets differ (left=22, right=22); labelled CFGs differ |
| `local/lito381/server/ClientSession#nativeSourceWindShotOriginalBody([BLlocal/lito381/server/BattleSpellCatalog$WindShotSpell;)V` | 順序不同 | method nativeSourceWindShotOriginalBody([BLlocal/lito381/server/BattleSpellCatalog$WindShotSpell;)V: left=162:[321:java/lang/Throwable  ] right=165:[326:java/lang/Throwable  ]; paths rejected: normal control-flow cycle; labelled CFGs differ |
| `local/lito381/server/ClientSession#openClassicWorldHousekeeper(Llocal/lito381/server/ClassicWorldNpcCatalog$StaticNpc;)V` | 缺呼叫 | method openClassicWorldHousekeeper(Llocal/lito381/server/ClassicWorldNpcCatalog$StaticNpc;)V insn[4]: left=INVOKEVIRTUAL local/lito381/server/ClassicWorldNpcCatalog$StaticNpc.x()I right=ALOAD 0; path event multisets differ (left=74, right=74); labelled CFGs differ |
| `local/lito381/server/ClientSession#publishNpcCounterBarrierTrigger(JLlocal/lito381/server/BattleCounterBarrier$Decision;Ljava/util/List;Ljava/lang/String;)V` | 順序不同 | method publishNpcCounterBarrierTrigger(JLlocal/lito381/server/BattleCounterBarrier$Decision;Ljava/util/List;Ljava/lang/String;)V insn[18]: left=IFEQ L51 right=IFEQ L54; paths rejected: normal control-flow cycle; labelled CFGs differ |
| `local/lito381/server/ClientSession#publishNpcCounterMirrorTrigger(JIILlocal/lito381/server/BattleCounterMirrorStatus$Decision;Ljava/util/List;Ljava/lang/String;)V` | 順序不同 | method publishNpcCounterMirrorTrigger(JIILlocal/lito381/server/BattleCounterMirrorStatus$Decision;Ljava/util/List;Ljava/lang/String;)V insn[22]: left=IFEQ L74 right=IFEQ L77; paths rejected: normal control-flow cycle; labelled CFGs differ |
| `local/lito381/server/ClientSession#publishPlayerMagicCancellation(Llocal/lito381/server/ClientSession;Llocal/lito381/server/ClientSession$PlayerMagicCancellation;Ljava/util/List;J)V` | 缺呼叫 | method publishPlayerMagicCancellation(Llocal/lito381/server/ClientSession;Llocal/lito381/server/ClientSession$PlayerMagicCancellation;Ljava/util/List;J)V insn[259]: left=INVOKESPECIAL local/lito381/server/ClientSession.sendEncrypted([BLjava/lang/String;)V right=INVOKESTATIC local/lito381/server/CombatPoisonParalysis.release(Llocal/lito381/server/ClientSession;[BLjava/lang/String;)V; paths rejected: normal control-flow cycle; labelled CFGs differ |
| `local/lito381/server/ClientSession#publishTimeAttackPoisonUpdate(Llocal/lito381/server/GameHub$TimeAttackPoisonUpdate;J)V` | 缺呼叫 | method publishTimeAttackPoisonUpdate(Llocal/lito381/server/GameHub$TimeAttackPoisonUpdate;J)V insn[2]: left=ASTORE 4 right=INVOKESTATIC local/lito381/server/NativeSourceArenaPoison.hpbar(Ljava/lang/Object;)V; paths rejected: normal control-flow cycle; labelled CFGs differ |
| `local/lito381/server/ClientSession#quitFight()V` | 多呼叫 | method quitFight()V: left=79:[95:java/io/IOException  , 115:java/lang/Throwable  ] right=86:[102:java/io/IOException  , 122:java/lang/Throwable  ]; path event multisets differ (left=1514, right=1503); labelled CFGs differ |
| `local/lito381/server/ClientSession#releaseCompanionToNature(Llocal/lito381/server/BattleCompanion;Llocal/lito381/server/BattleCompanion$Snapshot;I)Z` | 缺呼叫 | method releaseCompanionToNature(Llocal/lito381/server/BattleCompanion;Llocal/lito381/server/BattleCompanion$Snapshot;I)Z: left=46:[54:java/lang/Throwable  ] right=48:[56:java/lang/Throwable  ]; paths rejected: normal control-flow cycle; labelled CFGs differ |
| `local/lito381/server/ClientSession#resetAndSendRentalTownStatus()V` | 缺呼叫 | method resetAndSendRentalTownStatus()V insn[42]: left=PUTFIELD local/lito381/server/ClientSession.playerHp:I right=ALOAD 0; path event multisets differ (left=92, right=93); labelled CFGs differ |
| `local/lito381/server/ClientSession#resolveClassicWorldScarecrowHit(Llocal/lito381/server/ClassicWorldNpcCatalog$StaticNpc;Llocal/lito381/server/ClassicWorldMonsterCatalog$MonsterTemplate;I)V` | 純形狀待裁定 | method resolveClassicWorldScarecrowHit(Llocal/lito381/server/ClassicWorldNpcCatalog$StaticNpc;Llocal/lito381/server/ClassicWorldMonsterCatalog$MonsterTemplate;I)V insn[29]: left=GETFIELD local/lito381/server/ClientSession.hub:Llocal/lito381/server/GameHub; right=INVOKESTATIC local/lito381/server/NativeStaticNpcScene.heading(Llocal/lito381/server/GameHub;Llocal/lito381/server/ClassicWorldNpcCatalog$StaticNpc;Llocal/lito381/server/ClientSession;)I; paths rejected: normal control-flow cycle; labelled CFGs differ |
| `local/lito381/server/ClientSession#resolveDragonKnightNegativeTarget(Llocal/lito381/server/ClientSession$Phase;JIIIIIZ)Llocal/lito381/server/ClientSession$ArmorBreakTarget;` | 純形狀待裁定 | method resolveDragonKnightNegativeTarget(Llocal/lito381/server/ClientSession$Phase;JIIIIIZ)Llocal/lito381/server/ClientSession$ArmorBreakTarget; insn[551]: left=INVOKEDYNAMIC apply()Ljava/util/function/Function; bsm=H6 java/lang/invoke/LambdaMetafactory.metafactory(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodType;Ljava/lang/invoke/Me… right=INVOKEDYNAMIC apply()Ljava/util/function/Function; bsm=H6 java/lang/invoke/LambdaMetafactory.metafactory(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodType;Ljava/lang/invoke/Me…; paths rejected: normal control-flow cycle; labelled CFGs differ |
| `local/lito381/server/ClientSession#resolveSelectorOnlyIllusionAvatarTarget(Llocal/lito381/server/BattleSpellCatalog$IllusionAvatarSpell;Llocal/lito381/server/ClientSession$Phase;JIIIIJ)Llocal/lito381/server/ClientSession;` | 缺呼叫 | method resolveSelectorOnlyIllusionAvatarTarget(Llocal/lito381/server/BattleSpellCatalog$IllusionAvatarSpell;Llocal/lito381/server/ClientSession$Phase;JIIIIJ)Llocal/lito381/server/ClientSession; insn[39]: left=IFEQ L156 right=IFEQ L159; paths rejected: normal control-flow cycle; labelled CFGs differ |
| `local/lito381/server/ClientSession#serverAwakeningMpDrain(J)V` | 純形狀待裁定 | method serverAwakeningMpDrain(J)V: left=7:[53:java/lang/Throwable  ] right=5:[52:java/lang/Throwable  ]; path event multisets differ (left=28, right=28); labelled CFGs differ |
| `local/lito381/server/ClientSession#serverBotTick(JIII)V` | 缺呼叫 | method serverBotTick(JIII)V insn[80]: left=INVOKESPECIAL local/lito381/server/ClientSession.counterBarrierDecision(IIZIJ)Llocal/lito381/server/BattleCounterBarrier$Decision; right=ALOAD 0; paths rejected: normal control-flow cycle; labelled CFGs differ |
| `local/lito381/server/ClientSession#serverClassicWorldCompanionTick(IJ)V` | 缺呼叫 | method serverClassicWorldCompanionTick(IJ)V insn[1]: left=GETFIELD local/lito381/server/ClientSession.phase:Llocal/lito381/server/ClientSession$Phase; right=LLOAD 2/3; paths rejected: normal control-flow cycle; labelled CFGs differ |
| `local/lito381/server/ClientSession#serverClassicWorldCompanionTick(Llocal/lito381/server/BattleCompanion;IJ)V` | 缺呼叫 | method serverClassicWorldCompanionTick(Llocal/lito381/server/BattleCompanion;IJ)V: left=625:[631:java/lang/Exception  ] right=7:[1102:java/lang/Throwable  ]; paths rejected: normal control-flow cycle; labelled CFGs differ |
| `local/lito381/server/ClientSession#serverClassicWorldMonsterMagicAttack(Llocal/lito381/server/ClassicWorldMonsterPopulation$MagicAttack;)V` | 缺呼叫 | method serverClassicWorldMonsterMagicAttack(Llocal/lito381/server/ClassicWorldMonsterPopulation$MagicAttack;)V: left=111:[117:java/lang/Exception  ] right=12:[703:java/lang/Throwable  ]; paths rejected: normal control-flow cycle; labelled CFGs differ |
| `local/lito381/server/ClientSession#serverCompanionTick(JJ)V` | 缺呼叫 | method serverCompanionTick(JJ)V: left=31:[39:java/lang/Throwable  ] right=39:[47:java/lang/Throwable  ]; paths rejected: normal control-flow cycle; labelled CFGs differ |
| `local/lito381/server/ClientSession#serverCompanionTick(Llocal/lito381/server/BattleCompanion;JJ)V` | 缺呼叫 | method serverCompanionTick(Llocal/lito381/server/BattleCompanion;JJ)V: left=365:[370:java/lang/Exception  ] right=7:[822:java/lang/Throwable  ]; paths rejected: normal control-flow cycle; labelled CFGs differ |
| `local/lito381/server/ClientSession#serverCubeHarmonyNonPlayerDamage(JIILlocal/lito381/server/BattleCubeHarmonyEffects$Event;)V` | 缺呼叫 | method serverCubeHarmonyNonPlayerDamage(JIILlocal/lito381/server/BattleCubeHarmonyEffects$Event;)V insn[0]: left=ALOAD 5 right=ALOAD 0; paths rejected: normal control-flow cycle; labelled CFGs differ |
| `local/lito381/server/ClientSession#serverCubeIgnitionMonsterDamage(JIILlocal/lito381/server/BattleCubeIgnitionEffects$Event;)V` | 缺呼叫 | method serverCubeIgnitionMonsterDamage(JIILlocal/lito381/server/BattleCubeIgnitionEffects$Event;)V insn[0]: left=ALOAD 5 right=ALOAD 0; path event multisets differ (left=30, right=32); labelled CFGs differ |
| `local/lito381/server/ClientSession#serverHonorRespawnDue(JJII)V` | 缺呼叫 | method serverHonorRespawnDue(JJII)V: left=7:[40:java/lang/Throwable  ] right=10:[49:java/lang/Throwable  ]; paths rejected: normal control-flow cycle; labelled CFGs differ |
| `local/lito381/server/ClientSession#serverMonsterAiTick(Llocal/lito381/server/GameHub$MonsterAiTick;)V` | 缺呼叫 | method serverMonsterAiTick(Llocal/lito381/server/GameHub$MonsterAiTick;)V: left=444:[449:java/lang/Exception  ] right=12:[1091:java/lang/Throwable  ]; paths rejected: normal control-flow cycle; labelled CFGs differ |
| `local/lito381/server/ClientSession#serverQuestMonsterAreaPhysicalHit(Llocal/lito381/server/GameHub$MonsterAiTick;Llocal/lito381/server/BattleQuestEncounter$Snapshot;Ljava/util/List;Llocal/lito381/server/BattleTimeAttackMagicSkillCatalog$PhysicalDefinition;Llocal/lito381/server/SourceNpcPhysicalStats;ZI)V` | 缺呼叫 | method serverQuestMonsterAreaPhysicalHit(Llocal/lito381/server/GameHub$MonsterAiTick;Llocal/lito381/server/BattleQuestEncounter$Snapshot;Ljava/util/List;Llocal/lito381/server/BattleTimeAttackMagicSkillCatalog$PhysicalDefinition;Llocal/lito381/server/SourceNpcPhysicalStats;ZI)V: left=269:[338:java/lang/IllegalArgumentException  ] right=10:[550:java/lang/Throwable  ]; paths rejected: normal control-flow cycle; labelled CFGs differ |
| `local/lito381/server/ClientSession#serverQuestMonsterMagicAttack(Llocal/lito381/server/GameHub$MonsterAiTick;Llocal/lito381/server/BattleQuestEncounter$Snapshot;Ljava/util/List;)V` | 缺呼叫 | method serverQuestMonsterMagicAttack(Llocal/lito381/server/GameHub$MonsterAiTick;Llocal/lito381/server/BattleQuestEncounter$Snapshot;Ljava/util/List;)V: left=87:[93:java/lang/Exception  ] right=10:[557:java/lang/Throwable  ]; paths rejected: normal control-flow cycle; labelled CFGs differ |
| `local/lito381/server/ClientSession#serverTimeAttackMonsterAreaPhysicalHit(Llocal/lito381/server/GameHub$MonsterAiTick;Llocal/lito381/server/BattleTimeAttackEncounter$Snapshot;Ljava/util/List;Llocal/lito381/server/BattleTimeAttackMagicSkillCatalog$PhysicalDefinition;Llocal/lito381/server/SourceNpcPhysicalStats;ZI)V` | 缺呼叫 | method serverTimeAttackMonsterAreaPhysicalHit(Llocal/lito381/server/GameHub$MonsterAiTick;Llocal/lito381/server/BattleTimeAttackEncounter$Snapshot;Ljava/util/List;Llocal/lito381/server/BattleTimeAttackMagicSkillCatalog$PhysicalDefinition;Llocal/lito381/server/SourceNpcPhysicalStats;ZI)V: left=272:[341:java/lang/IllegalArgumentException  ] right=10:[553:java/lang/Throwable  ]; paths rejected: normal control-flow cycle; labelled CFGs differ |
| `local/lito381/server/ClientSession#serverTimeAttackMonsterMagicAttack(Llocal/lito381/server/GameHub$MonsterAiTick;Llocal/lito381/server/BattleTimeAttackEncounter$Snapshot;Ljava/util/List;)V` | 缺呼叫 | method serverTimeAttackMonsterMagicAttack(Llocal/lito381/server/GameHub$MonsterAiTick;Llocal/lito381/server/BattleTimeAttackEncounter$Snapshot;Ljava/util/List;)V: left=122:[128:java/lang/Exception  ] right=10:[592:java/lang/Throwable  ]; paths rejected: normal control-flow cycle; labelled CFGs differ |
| `local/lito381/server/ClientSession#slaveControl([B)V` | 缺呼叫 | method slaveControl([B)V: length left=0 right=134; paths rejected: path limit 4096 exceeded; labelled CFGs differ |
| `local/lito381/server/ClientSession#sourceApplicationRun()V` | 缺呼叫 | method sourceApplicationRun()V: left=1:[8:java/lang/Exception  ] right=24:[740:java/lang/Exception  , 1062:java/lang/Throwable  ]; paths rejected: normal control-flow cycle; labelled CFGs differ |
| `local/lito381/server/ClientSession#sourcePlayerBattleCommit(ILlocal/lito381/server/ClientSession;ZZJLlocal/lito381/server/BattleJoyOfPainStatus$Lease;I)V` | 純形狀待裁定 | method sourcePlayerBattleCommit(ILlocal/lito381/server/ClientSession;ZZJLlocal/lito381/server/BattleJoyOfPainStatus$Lease;I)V insn[4]: left=INVOKESTATIC local/lito381/server/NativeAcceptedState.requireCurrent()V right=ALOAD 0; paths rejected: normal control-flow cycle; labelled CFGs differ |
| `local/lito381/server/ClientSession#sourcePlayerLegacyWorldDirectDamage(DJLlocal/lito381/server/CombatPoisonLifecycle$Scope;)Llocal/lito381/server/ClientSession$ClassicWorldFatalContext;` | 純形狀待裁定 | method sourcePlayerLegacyWorldDirectDamage(DJLlocal/lito381/server/CombatPoisonLifecycle$Scope;)Llocal/lito381/server/ClientSession$ClassicWorldFatalContext; insn[38]: left=DUP right=ISTORE 9; paths rejected: normal control-flow cycle; labelled CFGs differ |
| `local/lito381/server/ClientSession#sourcePlayerWorldCommit(DJLlocal/lito381/server/CombatPoisonLifecycle$Scope;Llocal/lito381/server/ClassicWorldMonsterPopulation$Snapshot;Llocal/lito381/server/BattleAbilityStatus$IllusionAvatar;I)Llocal/lito381/server/ClientSession$ClassicWorldFatalContext;` | 順序不同 | method sourcePlayerWorldCommit(DJLlocal/lito381/server/CombatPoisonLifecycle$Scope;Llocal/lito381/server/ClassicWorldMonsterPopulation$Snapshot;Llocal/lito381/server/BattleAbilityStatus$IllusionAvatar;I)Llocal/lito381/server/ClientSession$ClassicWorldFatalContext; insn[0]: left=INVOKESTATIC local/lito381/server/NativeAcceptedState.requireCurrent()V right=ALOAD 0; paths rejected: normal control-flow cycle; labelled CFGs differ |
| `local/lito381/server/ClientSession#sourcePoisonBody$applyFatalityCancellation(Llocal/lito381/server/BattleDeathmatchFatalityCatalog$Effect;Llocal/lito381/server/CombatPoisonLifecycle$Scope;)V` | 純形狀待裁定 | method sourcePoisonBody$applyFatalityCancellation(Llocal/lito381/server/BattleDeathmatchFatalityCatalog$Effect;Llocal/lito381/server/CombatPoisonLifecycle$Scope;)V: left=11:[219:java/lang/Throwable  ] right=9:[216:java/lang/Throwable  ]; path event multisets differ (left=60, right=58); labelled CFGs differ |
| `local/lito381/server/ClientSession#sourcePoisonBody$attemptEnchantVenomPoison(Llocal/lito381/server/ClientSession;Llocal/lito381/server/BattleQuestEncounter$Snapshot;Llocal/lito381/server/BattleTimeAttackEncounter$Snapshot;Llocal/lito381/server/ClassicWorldMonsterPopulation$Snapshot;ZZJLlocal/lito381/server/CombatPoisonLifecycle$Scope;)V` | 純形狀待裁定 | method sourcePoisonBody$attemptEnchantVenomPoison(Llocal/lito381/server/ClientSession;Llocal/lito381/server/BattleQuestEncounter$Snapshot;Llocal/lito381/server/BattleTimeAttackEncounter$Snapshot;Llocal/lito381/server/ClassicWorldMonsterPopulation$Snapshot;ZZJLlocal/lito381/server/CombatPoisonLifecycle$Scope;)V: left=69:[159:java/lang/Throwable  ] right=65:[153:java/lang/Throwable  ]; paths rejected: normal control-flow cycle; labelled CFGs differ |
| `local/lito381/server/ClientSession#sourcePoisonBody$publishClassicWorldMonsterPoisonUpdate(Llocal/lito381/server/ClassicWorldMonsterPopulation$PoisonUpdate;JLlocal/lito381/server/CombatPoisonLifecycle$Scope;)V` | 純形狀待裁定 | method sourcePoisonBody$publishClassicWorldMonsterPoisonUpdate(Llocal/lito381/server/ClassicWorldMonsterPopulation$PoisonUpdate;JLlocal/lito381/server/CombatPoisonLifecycle$Scope;)V insn[27]: left=IFEQ L178 right=IFEQ L158; paths rejected: normal control-flow cycle; labelled CFGs differ |
| `local/lito381/server/ClientSession#sourcePoisonBody$publishNpcMagicCancellation(JLjava/util/List;ZZZZZZZZZZZZZZZZZZZZZLlocal/lito381/server/CombatPoisonLifecycle$Scope;)V` | 順序不同 | method sourcePoisonBody$publishNpcMagicCancellation(JLjava/util/List;ZZZZZZZZZZZZZZZZZZZZZLlocal/lito381/server/CombatPoisonLifecycle$Scope;)V: left= right=(JLjava/util/List<Llocal/lito381/server/ClientSession;>;ZZZZZZZZZZZZZZZZZZZZZ)V |
| `local/lito381/server/ClientSession#sourcePoisonBody$serverClassicWorldMonsterStatusSkillAttack(Llocal/lito381/server/ClassicWorldMonsterPopulation$StatusSkillAttack;ILlocal/lito381/server/CombatPoisonLifecycle$Scope;)V` | 純形狀待裁定 | method sourcePoisonBody$serverClassicWorldMonsterStatusSkillAttack(Llocal/lito381/server/ClassicWorldMonsterPopulation$StatusSkillAttack;ILlocal/lito381/server/CombatPoisonLifecycle$Scope;)V: left=126:[133:java/lang/Exception  ] right=124:[131:java/lang/Exception  ]; paths rejected: normal control-flow cycle; labelled CFGs differ |
| `local/lito381/server/ClientSession#sourcePoisonBody$serverExpireTimedStatuses(JLlocal/lito381/server/CombatPoisonLifecycle$Scope;)V` | 純形狀待裁定 | method sourcePoisonBody$serverExpireTimedStatuses(JLlocal/lito381/server/CombatPoisonLifecycle$Scope;)V insn[74]: left=IFEQ L481 right=IFEQ L442; paths rejected: normal control-flow cycle; labelled CFGs differ |
| `local/lito381/server/ClientSession#sourcePoisonBody$serverPrepareBattle(Llocal/lito381/server/CombatPoisonLifecycle$Scope;)V` | 純形狀待裁定 | method sourcePoisonBody$serverPrepareBattle(Llocal/lito381/server/CombatPoisonLifecycle$Scope;)V: left=140:[164:java/io/IOException  ] right=138:[162:java/io/IOException  ]; path event multisets differ (left=175, right=173); labelled CFGs differ |
| `local/lito381/server/ClientSession#sourcePoisonBody$sourceDeathCancellation(Llocal/lito381/server/BattleDeathmatchFatalityCatalog$Effect;Llocal/lito381/server/CombatPoisonLifecycle$Scope;)V` | 純形狀待裁定 | method sourcePoisonBody$sourceDeathCancellation(Llocal/lito381/server/BattleDeathmatchFatalityCatalog$Effect;Llocal/lito381/server/CombatPoisonLifecycle$Scope;)V: left=13:[401:java/lang/Throwable  ] right=12:[388:java/lang/Throwable  ]; path event multisets differ (left=150, right=148); labelled CFGs differ |
| `local/lito381/server/ClientSession#sourcePoisonBody$takeClassicWorldDirectDamage(DJLlocal/lito381/server/CombatPoisonLifecycle$Scope;)Llocal/lito381/server/ClientSession$ClassicWorldFatalContext;` | 純形狀待裁定 | method sourcePoisonBody$takeClassicWorldDirectDamage(DJLlocal/lito381/server/CombatPoisonLifecycle$Scope;)Llocal/lito381/server/ClientSession$ClassicWorldFatalContext;: left=5:[79:java/lang/Throwable  ] right=5:[77:java/lang/Throwable  ]; path event multisets differ (left=58, right=58); labelled CFGs differ |
| `local/lito381/server/ClientSession#sourcePoisonBody$useClassicWorldCurseRemovalSpell([BLlocal/lito381/server/BattleSpellCatalog$CurseRemovalSpell;Llocal/lito381/server/CombatPoisonLifecycle$Scope;)V` | 順序不同 | method sourcePoisonBody$useClassicWorldCurseRemovalSpell([BLlocal/lito381/server/BattleSpellCatalog$CurseRemovalSpell;Llocal/lito381/server/CombatPoisonLifecycle$Scope;)V: left=307:[392:java/lang/Throwable  ] right=306:[391:java/lang/Throwable  ]; paths rejected: normal control-flow cycle; labelled CFGs differ |
| `local/lito381/server/ClientSession#sourcePoisonBody$useCurseRemovalSpell([BLlocal/lito381/server/BattleSpellCatalog$CurseRemovalSpell;Llocal/lito381/server/CombatPoisonLifecycle$Scope;)V` | 順序不同 | method sourcePoisonBody$useCurseRemovalSpell([BLlocal/lito381/server/BattleSpellCatalog$CurseRemovalSpell;Llocal/lito381/server/CombatPoisonLifecycle$Scope;)V: left=452:[531:java/lang/Throwable  ] right=444:[523:java/lang/Throwable  ]; paths rejected: normal control-flow cycle; labelled CFGs differ |
| `local/lito381/server/ClientSession#startBattle()V` | 純形狀待裁定 | method startBattle()V insn[242]: left=ISTORE 7 right=POP; paths rejected: normal control-flow cycle; labelled CFGs differ |
| `local/lito381/server/ClientSession#takePlayerDamage(ILlocal/lito381/server/ClientSession;ZZJ)V` | 缺呼叫 | method takePlayerDamage(ILlocal/lito381/server/ClientSession;ZZJ)V: length left=0 right=29; paths rejected: normal control-flow cycle; labelled CFGs differ |
| `local/lito381/server/ClientSession#tryMortalBodyClassicWorldMonsterReflection(Llocal/lito381/server/ClassicWorldMonsterPopulation$Snapshot;Ljava/lang/String;)V` | 缺呼叫 | method tryMortalBodyClassicWorldMonsterReflection(Llocal/lito381/server/ClassicWorldMonsterPopulation$Snapshot;Ljava/lang/String;)V insn[0]: left=ALOAD 1 right=ALOAD 0; paths rejected: normal control-flow cycle; labelled CFGs differ |
| `local/lito381/server/ClientSession#useAbilityBuffSpell([BLlocal/lito381/server/BattleSpellCatalog$AbilityBuffSpell;)V` | 缺呼叫 | method useAbilityBuffSpell([BLlocal/lito381/server/BattleSpellCatalog$AbilityBuffSpell;)V: left=257:[443:java/lang/Throwable  ] right=271:[461:java/lang/Throwable  ]; paths rejected: normal control-flow cycle; labelled CFGs differ |
| `local/lito381/server/ClientSession#useAdvanceSpiritSpell([BLlocal/lito381/server/BattleSpellCatalog$AdvanceSpiritSpell;)V` | 缺呼叫 | method useAdvanceSpiritSpell([BLlocal/lito381/server/BattleSpellCatalog$AdvanceSpiritSpell;)V: left=188:[358:java/lang/Throwable  ] right=197:[371:java/lang/Throwable  ]; paths rejected: normal control-flow cycle; labelled CFGs differ |
| `local/lito381/server/ClientSession#useAquaProtectSpell([BLlocal/lito381/server/BattleSpellCatalog$AquaProtectSpell;)V` | 缺呼叫 | method useAquaProtectSpell([BLlocal/lito381/server/BattleSpellCatalog$AquaProtectSpell;)V: left=282:[460:java/lang/Throwable  ] right=298:[480:java/lang/Throwable  ]; paths rejected: normal control-flow cycle; labelled CFGs differ |
| `local/lito381/server/ClientSession#useBodyToMindSpell([BLlocal/lito381/server/BattleSpellCatalog$BodyToMindSpell;)V` | 缺呼叫 | method useBodyToMindSpell([BLlocal/lito381/server/BattleSpellCatalog$BodyToMindSpell;)V: left=34:[141:java/lang/Throwable  ] right=38:[149:java/lang/Throwable  ]; paths rejected: normal control-flow cycle; labelled CFGs differ |
| `local/lito381/server/ClientSession#useBounceAttackSpell([BLlocal/lito381/server/BattleSpellCatalog$BounceAttackSpell;)V` | 缺呼叫 | method useBounceAttackSpell([BLlocal/lito381/server/BattleSpellCatalog$BounceAttackSpell;)V: left=119:[269:java/lang/Throwable  ] right=119:[273:java/lang/Throwable  ]; paths rejected: normal control-flow cycle; labelled CFGs differ |
| `local/lito381/server/ClientSession#useBringStoneSpell([BLlocal/lito381/server/BattleSpellCatalog$BringStoneSpell;)V` | 缺呼叫 | method useBringStoneSpell([BLlocal/lito381/server/BattleSpellCatalog$BringStoneSpell;)V: left=172:[380:java/lang/Throwable  ] right=188:[400:java/lang/Throwable  ]; paths rejected: normal control-flow cycle; labelled CFGs differ |
| `local/lito381/server/ClientSession#useBurningSlashSpell([BLlocal/lito381/server/BattleSpellCatalog$BurningSlashSpell;)V` | 缺呼叫 | method useBurningSlashSpell([BLlocal/lito381/server/BattleSpellCatalog$BurningSlashSpell;)V: left=90:[218:java/lang/Throwable  ] right=96:[228:java/lang/Throwable  ]; paths rejected: normal control-flow cycle; labelled CFGs differ |
| `local/lito381/server/ClientSession#useCallClanSpell([BLlocal/lito381/server/BattleSpellCatalog$CallClanSpell;)V` | 缺呼叫 | method useCallClanSpell([BLlocal/lito381/server/BattleSpellCatalog$CallClanSpell;)V: left=148:[276:java/lang/Throwable  ] right=155:[287:java/lang/Throwable  ]; paths rejected: normal control-flow cycle; labelled CFGs differ |
| `local/lito381/server/ClientSession#useClassicWorldAreaAttackSpell(Llocal/lito381/server/Protocol$AttackSpellRequest;Llocal/lito381/server/BattleSpellCatalog$AreaAttackSpell;)V` | 純形狀待裁定 | method useClassicWorldAreaAttackSpell(Llocal/lito381/server/Protocol$AttackSpellRequest;Llocal/lito381/server/BattleSpellCatalog$AreaAttackSpell;)V insn[10]: left=INVOKEDYNAMIC run(Llocal/lito381/server/ClientSession;Llocal/lito381/server/Protocol$AttackSpellRequest;Llocal/lito381/server/BattleSpellCatalog$AreaAttackSpell;)Llocal/lito381/server/NativeSelfCastAction$Task; bsm=H6 java/lang/invoke/Lambd… right=INVOKEDYNAMIC run(Llocal/lito381/server/ClientSession;Llocal/lito381/server/Protocol$AttackSpellRequest;Llocal/lito381/server/BattleSpellCatalog$AreaAttackSpell;)Llocal/lito381/server/NativeSelfCastAction$Task; bsm=H6 java/lang/invoke/Lambd…; path event multisets differ (left=5, right=5); labelled CFGs differ |
| `local/lito381/server/ClientSession#useClassicWorldAttackSpell(Llocal/lito381/server/Protocol$AttackSpellRequest;Llocal/lito381/server/BattleSpellCatalog$AttackSpell;)V` | 純形狀待裁定 | method useClassicWorldAttackSpell(Llocal/lito381/server/Protocol$AttackSpellRequest;Llocal/lito381/server/BattleSpellCatalog$AttackSpell;)V insn[10]: left=INVOKEDYNAMIC run(Llocal/lito381/server/ClientSession;Llocal/lito381/server/Protocol$AttackSpellRequest;Llocal/lito381/server/BattleSpellCatalog$AttackSpell;)Llocal/lito381/server/NativeSelfCastAction$Task; bsm=H6 java/lang/invoke/LambdaMet… right=INVOKEDYNAMIC run(Llocal/lito381/server/ClientSession;Llocal/lito381/server/Protocol$AttackSpellRequest;Llocal/lito381/server/BattleSpellCatalog$AttackSpell;)Llocal/lito381/server/NativeSelfCastAction$Task; bsm=H6 java/lang/invoke/LambdaMet…; path event multisets differ (left=5, right=5); labelled CFGs differ |
| `local/lito381/server/ClientSession#useClassicWorldCenteredAreaAttackSpell([BLlocal/lito381/server/BattleSpellCatalog$AreaAttackSpell;)V` | 缺呼叫 | method useClassicWorldCenteredAreaAttackSpell([BLlocal/lito381/server/BattleSpellCatalog$AreaAttackSpell;)V: left=101:[528:java/lang/Exception  ] right=105:[532:java/lang/Exception  ]; paths rejected: normal control-flow cycle; labelled CFGs differ |
| `local/lito381/server/ClientSession#useClassicWorldCounterDetectionSpell([BLlocal/lito381/server/BattleSpellCatalog$CounterDetectionSpell;)V` | 缺呼叫 | method useClassicWorldCounterDetectionSpell([BLlocal/lito381/server/BattleSpellCatalog$CounterDetectionSpell;)V: left=108:[204:java/lang/Throwable  ] right=110:[210:java/lang/Throwable  ]; paths rejected: normal control-flow cycle; labelled CFGs differ |
| `local/lito381/server/ClientSession#useClassicWorldHealingSpell([BLlocal/lito381/server/BattleSpellCatalog$HealingSpell;I)V` | 缺呼叫 | method useClassicWorldHealingSpell([BLlocal/lito381/server/BattleSpellCatalog$HealingSpell;I)V: left=327:[332:java/lang/Exception  ] right=336:[341:java/lang/Exception  ]; paths rejected: normal control-flow cycle; labelled CFGs differ |
| `local/lito381/server/ClientSession#useClassicWorldLineAttackSpell([BLlocal/lito381/server/BattleSpellCatalog$LineAttackSpell;)V` | 純形狀待裁定 | method useClassicWorldLineAttackSpell([BLlocal/lito381/server/BattleSpellCatalog$LineAttackSpell;)V insn[5]: left=INVOKEDYNAMIC run(Llocal/lito381/server/ClientSession;[BLlocal/lito381/server/BattleSpellCatalog$LineAttackSpell;)Llocal/lito381/server/NativeSelfCastAction$Task; bsm=H6 java/lang/invoke/LambdaMetafactory.metafactory(Ljava/lang/invoke/Metho… right=INVOKEDYNAMIC run(Llocal/lito381/server/ClientSession;[BLlocal/lito381/server/BattleSpellCatalog$LineAttackSpell;)Llocal/lito381/server/NativeSelfCastAction$Task; bsm=H6 java/lang/invoke/LambdaMetafactory.metafactory(Ljava/lang/invoke/Metho…; path event multisets differ (left=3, right=3); labelled CFGs differ |
| `local/lito381/server/ClientSession#useClassicWorldLineFreezeAttackSpell(Llocal/lito381/server/Protocol$AttackSpellRequest;Llocal/lito381/server/BattleSpellCatalog$FreezeAttackSpell;)V` | 缺呼叫 | method useClassicWorldLineFreezeAttackSpell(Llocal/lito381/server/Protocol$AttackSpellRequest;Llocal/lito381/server/BattleSpellCatalog$FreezeAttackSpell;)V: left=183:[371:java/lang/Exception  ] right=192:[380:java/lang/Exception  ]; paths rejected: normal control-flow cycle; labelled CFGs differ |
| `local/lito381/server/ClientSession#useClassicWorldManaDrainSpell([BLlocal/lito381/server/BattleSpellCatalog$ManaDrainSpell;)V` | 缺呼叫 | method useClassicWorldManaDrainSpell([BLlocal/lito381/server/BattleSpellCatalog$ManaDrainSpell;)V: left=269:[274:java/lang/Exception  ] right=278:[283:java/lang/Exception  ]; paths rejected: normal control-flow cycle; labelled CFGs differ |
| `local/lito381/server/ClientSession#useClassicWorldTurnUndeadSpell([BLlocal/lito381/server/BattleSpellCatalog$TurnUndeadSpell;)V` | 缺呼叫 | method useClassicWorldTurnUndeadSpell([BLlocal/lito381/server/BattleSpellCatalog$TurnUndeadSpell;)V: left=226:[231:java/lang/Exception  ] right=235:[240:java/lang/Exception  ]; paths rejected: normal control-flow cycle; labelled CFGs differ |
| `local/lito381/server/ClientSession#useCombatBuffSpell([BLlocal/lito381/server/BattleSpellCatalog$CombatBuffSpell;)V` | 缺呼叫 | method useCombatBuffSpell([BLlocal/lito381/server/BattleSpellCatalog$CombatBuffSpell;)V: left=208:[397:java/lang/Throwable  ] right=217:[410:java/lang/Throwable  ]; paths rejected: normal control-flow cycle; labelled CFGs differ |
| `local/lito381/server/ClientSession#useCounterBarrierSpell([BLlocal/lito381/server/BattleSpellCatalog$CounterBarrierSpell;)V` | 缺呼叫 | method useCounterBarrierSpell([BLlocal/lito381/server/BattleSpellCatalog$CounterBarrierSpell;)V: left=118:[269:java/lang/Throwable  ] right=118:[273:java/lang/Throwable  ]; paths rejected: normal control-flow cycle; labelled CFGs differ |
| `local/lito381/server/ClientSession#useCounterDetectionSpell([BLlocal/lito381/server/BattleSpellCatalog$CounterDetectionSpell;)V` | 缺呼叫 | method useCounterDetectionSpell([BLlocal/lito381/server/BattleSpellCatalog$CounterDetectionSpell;)V: left=212:[312:java/lang/Throwable  ] right=214:[318:java/lang/Throwable  ]; paths rejected: normal control-flow cycle; labelled CFGs differ |
| `local/lito381/server/ClientSession#useDetectionSpell([BLlocal/lito381/server/BattleSpellCatalog$DetectionSpell;)V` | 缺呼叫 | method useDetectionSpell([BLlocal/lito381/server/BattleSpellCatalog$DetectionSpell;)V: left=143:[252:java/lang/Throwable  ] right=143:[256:java/lang/Throwable  ]; paths rejected: normal control-flow cycle; labelled CFGs differ |
| `local/lito381/server/ClientSession#useDiamondGolemSpell([BLlocal/lito381/server/BattleSpellCatalog$DiamondGolemSpell;)V` | 缺呼叫 | method useDiamondGolemSpell([BLlocal/lito381/server/BattleSpellCatalog$DiamondGolemSpell;)V: left=136:[341:java/lang/Throwable  ] right=143:[352:java/lang/Throwable  ]; paths rejected: normal control-flow cycle; labelled CFGs differ |
| `local/lito381/server/ClientSession#useDodgeBuffSpell([BLlocal/lito381/server/BattleSpellCatalog$DodgeBuffSpell;)V` | 缺呼叫 | method useDodgeBuffSpell([BLlocal/lito381/server/BattleSpellCatalog$DodgeBuffSpell;)V: left=158:[303:java/lang/Throwable  ] right=158:[307:java/lang/Throwable  ]; paths rejected: normal control-flow cycle; labelled CFGs differ |
| `local/lito381/server/ClientSession#useDragonSkinSpell([BLlocal/lito381/server/BattleSpellCatalog$DragonSkinSpell;)V` | 缺呼叫 | method useDragonSkinSpell([BLlocal/lito381/server/BattleSpellCatalog$DragonSkinSpell;)V: left=90:[220:java/lang/Throwable  ] right=96:[230:java/lang/Throwable  ]; paths rejected: normal control-flow cycle; labelled CFGs differ |
| `local/lito381/server/ClientSession#useEarthBlessSpell([BLlocal/lito381/server/BattleSpellCatalog$EarthBlessSpell;)V` | 缺呼叫 | method useEarthBlessSpell([BLlocal/lito381/server/BattleSpellCatalog$EarthBlessSpell;)V: left=258:[350:java/lang/Throwable  ] right=265:[361:java/lang/Throwable  ]; paths rejected: normal control-flow cycle; labelled CFGs differ |
| `local/lito381/server/ClientSession#useFireBlessSpell([BLlocal/lito381/server/BattleSpellCatalog$FireBlessSpell;)V` | 缺呼叫 | method useFireBlessSpell([BLlocal/lito381/server/BattleSpellCatalog$FireBlessSpell;)V: left=258:[366:java/lang/Throwable  ] right=265:[377:java/lang/Throwable  ]; paths rejected: normal control-flow cycle; labelled CFGs differ |
| `local/lito381/server/ClientSession#useFoeSlayerSpell([BLlocal/lito381/server/BattleSpellCatalog$FoeSlayerSpell;)V` | 缺呼叫 | method useFoeSlayerSpell([BLlocal/lito381/server/BattleSpellCatalog$FoeSlayerSpell;)V: left=104:[243:java/lang/Throwable  ] right=116:[263:java/lang/Throwable  ]; paths rejected: normal control-flow cycle; labelled CFGs differ |
| `local/lito381/server/ClientSession#useGuardBrakeSpell([BLlocal/lito381/server/BattleSpellCatalog$GuardBrakeSpell;)V` | 缺呼叫 | method useGuardBrakeSpell([BLlocal/lito381/server/BattleSpellCatalog$GuardBrakeSpell;)V: left=349:[491:java/lang/Throwable  ] right=373:[519:java/lang/Throwable  ]; paths rejected: normal control-flow cycle; labelled CFGs differ |
| `local/lito381/server/ClientSession#useHasteSpell([BLlocal/lito381/server/BattleSpellCatalog$HasteSpell;)V` | 缺呼叫 | method useHasteSpell([BLlocal/lito381/server/BattleSpellCatalog$HasteSpell;)V: left=476:[602:java/lang/Throwable  ] right=485:[615:java/lang/Throwable  ]; paths rejected: normal control-flow cycle; labelled CFGs differ |
| `local/lito381/server/ClientSession#useHeroPumpkinBag(Llocal/lito381/server/RuntimeInventory$Item;I)V` | 缺呼叫 | method useHeroPumpkinBag(Llocal/lito381/server/RuntimeInventory$Item;I)V insn[199]: left=PUTFIELD local/lito381/server/ClientSession.inventory:Llocal/lito381/server/RuntimeInventory; right=CONST Integer:0; paths rejected: path limit 4096 exceeded; labelled CFGs differ |
| `local/lito381/server/ClientSession#useIllusionAvatarSpell([BLlocal/lito381/server/BattleSpellCatalog$IllusionAvatarSpell;)V` | 缺呼叫 | method useIllusionAvatarSpell([BLlocal/lito381/server/BattleSpellCatalog$IllusionAvatarSpell;)V: left=136:[381:java/lang/Throwable  ] right=143:[395:java/lang/Throwable  ]; paths rejected: normal control-flow cycle; labelled CFGs differ |
| `local/lito381/server/ClientSession#useIllusionLichSpell([BLlocal/lito381/server/BattleSpellCatalog$IllusionLichSpell;)V` | 缺呼叫 | method useIllusionLichSpell([BLlocal/lito381/server/BattleSpellCatalog$IllusionLichSpell;)V: left=136:[341:java/lang/Throwable  ] right=143:[352:java/lang/Throwable  ]; paths rejected: normal control-flow cycle; labelled CFGs differ |
| `local/lito381/server/ClientSession#useIllusionOgreSpell([BLlocal/lito381/server/BattleSpellCatalog$IllusionOgreSpell;)V` | 缺呼叫 | method useIllusionOgreSpell([BLlocal/lito381/server/BattleSpellCatalog$IllusionOgreSpell;)V: left=136:[343:java/lang/Throwable  ] right=143:[354:java/lang/Throwable  ]; paths rejected: normal control-flow cycle; labelled CFGs differ |
| `local/lito381/server/ClientSession#useInsightSpell([BLlocal/lito381/server/BattleSpellCatalog$InsightSpell;)V` | 缺呼叫 | method useInsightSpell([BLlocal/lito381/server/BattleSpellCatalog$InsightSpell;)V: left=136:[341:java/lang/Throwable  ] right=143:[352:java/lang/Throwable  ]; paths rejected: normal control-flow cycle; labelled CFGs differ |
| `local/lito381/server/ClientSession#useIronSkinSpell([BLlocal/lito381/server/BattleSpellCatalog$IronSkinSpell;)V` | 缺呼叫 | method useIronSkinSpell([BLlocal/lito381/server/BattleSpellCatalog$IronSkinSpell;)V: left=164:[265:java/lang/Throwable  ] right=180:[285:java/lang/Throwable  ]; paths rejected: normal control-flow cycle; labelled CFGs differ |
| `local/lito381/server/ClientSession#useItemArmorEnchantSpell([BLlocal/lito381/server/BattleSpellCatalog$ItemArmorEnchantSpell;)V` | 缺呼叫 | method useItemArmorEnchantSpell([BLlocal/lito381/server/BattleSpellCatalog$ItemArmorEnchantSpell;)V: left=114:[269:java/lang/Throwable  ] right=123:[284:java/lang/Throwable  ]; paths rejected: normal control-flow cycle; labelled CFGs differ |
| `local/lito381/server/ClientSession#useJoyOfPainSpell([BLlocal/lito381/server/BattleSpellCatalog$JoyOfPainSpell;)V` | 缺呼叫 | method useJoyOfPainSpell([BLlocal/lito381/server/BattleSpellCatalog$JoyOfPainSpell;)V: left=129:[311:java/lang/Throwable  ] right=136:[322:java/lang/Throwable  ]; paths rejected: normal control-flow cycle; labelled CFGs differ |
| `local/lito381/server/ClientSession#useMortalBodySpell([BLlocal/lito381/server/BattleSpellCatalog$MortalBodySpell;)V` | 缺呼叫 | method useMortalBodySpell([BLlocal/lito381/server/BattleSpellCatalog$MortalBodySpell;)V: left=90:[227:java/lang/Throwable  ] right=96:[237:java/lang/Throwable  ]; paths rejected: normal control-flow cycle; labelled CFGs differ |
| `local/lito381/server/ClientSession#useMovementBuffSpell([BLlocal/lito381/server/BattleSpellCatalog$MovementBuffSpell;)V` | 缺呼叫 | method useMovementBuffSpell([BLlocal/lito381/server/BattleSpellCatalog$MovementBuffSpell;)V: left=62:[167:java/lang/Throwable  ] right=62:[171:java/lang/Throwable  ]; paths rejected: normal control-flow cycle; labelled CFGs differ |
| `local/lito381/server/ClientSession#useNaturesTouchSpell([BLlocal/lito381/server/BattleSpellCatalog$NaturesTouchSpell;)V` | 缺呼叫 | method useNaturesTouchSpell([BLlocal/lito381/server/BattleSpellCatalog$NaturesTouchSpell;)V: left=284:[471:java/lang/Throwable  ] right=300:[491:java/lang/Throwable  ]; paths rejected: normal control-flow cycle; labelled CFGs differ |
| `local/lito381/server/ClientSession#useOtherNoEffectSpell([BLlocal/lito381/server/BattleSpellCatalog$OtherNoEffectSpell;)V` | 缺呼叫 | method useOtherNoEffectSpell([BLlocal/lito381/server/BattleSpellCatalog$OtherNoEffectSpell;)V: left=170:[242:java/lang/Throwable  ] right=170:[246:java/lang/Throwable  ]; paths rejected: path limit 4096 exceeded; labelled CFGs differ |
| `local/lito381/server/ClientSession#usePanicSpell([BLlocal/lito381/server/BattleSpellCatalog$PanicSpell;I)V` | 缺呼叫 | method usePanicSpell([BLlocal/lito381/server/BattleSpellCatalog$PanicSpell;I)V: left=411:[602:java/lang/Throwable  ] right=424:[619:java/lang/Throwable  ]; paths rejected: normal control-flow cycle; labelled CFGs differ |
| `local/lito381/server/ClientSession#usePatienceSpell([BLlocal/lito381/server/BattleSpellCatalog$PatienceSpell;)V` | 缺呼叫 | method usePatienceSpell([BLlocal/lito381/server/BattleSpellCatalog$PatienceSpell;)V: left=115:[317:java/lang/Throwable  ] right=122:[328:java/lang/Throwable  ]; paths rejected: normal control-flow cycle; labelled CFGs differ |
| `local/lito381/server/ClientSession#usePetCollar(Llocal/lito381/server/RuntimeInventory$Item;)V` | 缺呼叫 | method usePetCollar(Llocal/lito381/server/RuntimeInventory$Item;)V: left=202:[209:java/lang/Throwable  ] right=198:[205:java/lang/Throwable  ]; paths rejected: normal control-flow cycle; labelled CFGs differ |
| `local/lito381/server/ClientSession#usePhantasmSpell([BLlocal/lito381/server/BattleSpellCatalog$PhantasmSpell;I)V` | 缺呼叫 | method usePhantasmSpell([BLlocal/lito381/server/BattleSpellCatalog$PhantasmSpell;I)V: left=317:[506:java/lang/Throwable  ] right=324:[517:java/lang/Throwable  ]; paths rejected: normal control-flow cycle; labelled CFGs differ |
| `local/lito381/server/ClientSession#usePoisonResistanceSpell([BLlocal/lito381/server/BattleSpellCatalog$PoisonResistanceSpell;)V` | 缺呼叫 | method usePoisonResistanceSpell([BLlocal/lito381/server/BattleSpellCatalog$PoisonResistanceSpell;)V: left=158:[285:java/lang/Throwable  ] right=158:[289:java/lang/Throwable  ]; paths rejected: normal control-flow cycle; labelled CFGs differ |
| `local/lito381/server/ClientSession#useRangedEvasionBuffSpell([BLlocal/lito381/server/BattleSpellCatalog$RangedEvasionBuffSpell;)V` | 缺呼叫 | method useRangedEvasionBuffSpell([BLlocal/lito381/server/BattleSpellCatalog$RangedEvasionBuffSpell;)V: left=158:[284:java/lang/Throwable  ] right=158:[288:java/lang/Throwable  ]; paths rejected: normal control-flow cycle; labelled CFGs differ |
| `local/lito381/server/ClientSession#useReductionArmorSpell([BLlocal/lito381/server/BattleSpellCatalog$ReductionArmorSpell;)V` | 缺呼叫 | method useReductionArmorSpell([BLlocal/lito381/server/BattleSpellCatalog$ReductionArmorSpell;)V: left=119:[263:java/lang/Throwable  ] right=119:[267:java/lang/Throwable  ]; paths rejected: normal control-flow cycle; labelled CFGs differ |
| `local/lito381/server/ClientSession#useRegenSpell([BLlocal/lito381/server/BattleSpellCatalog$RegenSpell;)V` | 缺呼叫 | method useRegenSpell([BLlocal/lito381/server/BattleSpellCatalog$RegenSpell;)V: left=145:[372:java/lang/Throwable  ] right=145:[376:java/lang/Throwable  ]; paths rejected: normal control-flow cycle; labelled CFGs differ |
| `local/lito381/server/ClientSession#useResistFearSpell([BLlocal/lito381/server/BattleSpellCatalog$ResistFearSpell;)V` | 缺呼叫 | method useResistFearSpell([BLlocal/lito381/server/BattleSpellCatalog$ResistFearSpell;)V: left=138:[143:java/lang/Exception  ] right=136:[141:java/lang/Exception  ]; paths rejected: normal control-flow cycle; labelled CFGs differ |
| `local/lito381/server/ClientSession#useRevealWeaknessSpell([BLlocal/lito381/server/BattleSpellCatalog$RevealWeaknessSpell;)V` | 缺呼叫 | method useRevealWeaknessSpell([BLlocal/lito381/server/BattleSpellCatalog$RevealWeaknessSpell;)V: left=246:[251:java/lang/Exception  ] right=255:[260:java/lang/Exception  ]; paths rejected: normal control-flow cycle; labelled CFGs differ |
| `local/lito381/server/ClientSession#useRoiPouch(Llocal/lito381/server/RuntimeInventory$Item;I)V` | 缺呼叫 | method useRoiPouch(Llocal/lito381/server/RuntimeInventory$Item;I)V insn[106]: left=PUTFIELD local/lito381/server/ClientSession.inventory:Llocal/lito381/server/RuntimeInventory; right=CONST Integer:0; path event multisets differ (left=421, right=421); labelled CFGs differ |
| `local/lito381/server/ClientSession#useRoyalAuraSpell([BLlocal/lito381/server/BattleSpellCatalog$RoyalAuraSpell;)V` | 缺呼叫 | method useRoyalAuraSpell([BLlocal/lito381/server/BattleSpellCatalog$RoyalAuraSpell;)V: left=224:[333:java/lang/Throwable  ] right=231:[344:java/lang/Throwable  ]; paths rejected: normal control-flow cycle; labelled CFGs differ |
| `local/lito381/server/ClientSession#useRunClanSpell([BLlocal/lito381/server/BattleSpellCatalog$RunClanSpell;)V` | 缺呼叫 | method useRunClanSpell([BLlocal/lito381/server/BattleSpellCatalog$RunClanSpell;)V: left=229:[337:java/lang/Throwable  ] right=236:[348:java/lang/Throwable  ]; paths rejected: normal control-flow cycle; labelled CFGs differ |
| `local/lito381/server/ClientSession#useSelfHasteSpell([BLlocal/lito381/server/BattleSpellCatalog$SelfHasteSpell;)V` | 缺呼叫 | method useSelfHasteSpell([BLlocal/lito381/server/BattleSpellCatalog$SelfHasteSpell;)V: left=85:[179:java/lang/Throwable  ] right=85:[183:java/lang/Throwable  ]; paths rejected: normal control-flow cycle; labelled CFGs differ |
| `local/lito381/server/ClientSession#useSolidCarriageSpell([BLlocal/lito381/server/BattleSpellCatalog$SolidCarriageSpell;)V` | 缺呼叫 | method useSolidCarriageSpell([BLlocal/lito381/server/BattleSpellCatalog$SolidCarriageSpell;)V: left=131:[282:java/lang/Throwable  ] right=131:[286:java/lang/Throwable  ]; paths rejected: normal control-flow cycle; labelled CFGs differ |
| `local/lito381/server/ClientSession#useSourceWeapon(Llocal/lito381/server/RuntimeInventory$Item;)V` | 缺呼叫 | method useSourceWeapon(Llocal/lito381/server/RuntimeInventory$Item;)V: left=41:[50:java/lang/Exception  ] right=50:[59:java/lang/Exception  ]; path event multisets differ (left=329, right=330); labelled CFGs differ |
| `local/lito381/server/ClientSession#useSpellBeforeDragonTargets([B)V` | 缺呼叫 | method useSpellBeforeDragonTargets([B)V: left=26:[46:java/lang/Throwable  ] right=29:[51:java/lang/Throwable  ]; paths rejected: normal control-flow cycle; labelled CFGs differ |
| `local/lito381/server/ClientSession#useStormEyeSpell([BLlocal/lito381/server/BattleSpellCatalog$StormEyeSpell;)V` | 缺呼叫 | method useStormEyeSpell([BLlocal/lito381/server/BattleSpellCatalog$StormEyeSpell;)V: left=258:[366:java/lang/Throwable  ] right=265:[377:java/lang/Throwable  ]; paths rejected: normal control-flow cycle; labelled CFGs differ |
| `local/lito381/server/ClientSession#useStormShotSpell([BLlocal/lito381/server/BattleSpellCatalog$StormShotSpell;)V` | 缺呼叫 | method useStormShotSpell([BLlocal/lito381/server/BattleSpellCatalog$StormShotSpell;)V: left=173:[320:java/lang/Throwable  ] right=189:[340:java/lang/Throwable  ]; paths rejected: normal control-flow cycle; labelled CFGs differ |
| `local/lito381/server/ClientSession#useTargetNegativeStatusSpell([BLlocal/lito381/server/BattleSpellCatalog$TargetNegativeStatusSpell;ILlocal/lito381/server/ClientSession$TargetNegativeStatusKind;)V` | 純形狀待裁定 | method useTargetNegativeStatusSpell([BLlocal/lito381/server/BattleSpellCatalog$TargetNegativeStatusSpell;ILlocal/lito381/server/ClientSession$TargetNegativeStatusKind;)V insn[7]: left=INVOKEDYNAMIC run(Llocal/lito381/server/ClientSession;[BLlocal/lito381/server/BattleSpellCatalog$TargetNegativeStatusSpell;ILlocal/lito381/server/ClientSession$TargetNegativeStatusKind;)Llocal/lito381/server/NativeSelfCastAction$Task; bsm=H… right=INVOKEDYNAMIC run(Llocal/lito381/server/ClientSession;[BLlocal/lito381/server/BattleSpellCatalog$TargetNegativeStatusSpell;ILlocal/lito381/server/ClientSession$TargetNegativeStatusKind;)Llocal/lito381/server/NativeSelfCastAction$Task; bsm=H…; path event multisets differ (left=3, right=3); labelled CFGs differ |
| `local/lito381/server/ClientSession#useTeleportSpell([B)V` | 缺呼叫 | method useTeleportSpell([B)V: left=272:[450:java/lang/Throwable  ] right=290:[488:java/lang/Throwable  ]; paths rejected: normal control-flow cycle; labelled CFGs differ |
| `local/lito381/server/ClientSession#useTripleArrowSpell([BLlocal/lito381/server/BattleSpellCatalog$TripleArrowSpell;)V` | 缺呼叫 | method useTripleArrowSpell([BLlocal/lito381/server/BattleSpellCatalog$TripleArrowSpell;)V: left=108:[220:java/lang/Throwable  ] right=117:[233:java/lang/Throwable  ]; paths rejected: normal control-flow cycle; labelled CFGs differ |
| `local/lito381/server/ClientSession#useTrueTargetSpell([BLlocal/lito381/server/BattleSpellCatalog$TrueTargetSpell;)V` | 缺呼叫 | method useTrueTargetSpell([BLlocal/lito381/server/BattleSpellCatalog$TrueTargetSpell;)V: left=191:[302:java/lang/Throwable  ] right=201:[316:java/lang/Throwable  ]; paths rejected: normal control-flow cycle; labelled CFGs differ |
| `local/lito381/server/ClientSession#useWeaponBuffSpell([BLlocal/lito381/server/BattleSpellCatalog$WeaponBuffSpell;)V` | 缺呼叫 | method useWeaponBuffSpell([BLlocal/lito381/server/BattleSpellCatalog$WeaponBuffSpell;)V: left=455:[649:java/lang/Throwable  ] right=464:[663:java/lang/Throwable  ]; paths rejected: normal control-flow cycle; labelled CFGs differ |
| `local/lito381/server/ClientSession#useWorldRoiCancellation([BLlocal/lito381/server/BattleSpellCatalog$CancellationSpell;)V` | 缺呼叫 | method useWorldRoiCancellation([BLlocal/lito381/server/BattleSpellCatalog$CancellationSpell;)V: left=323:[529:java/lang/Throwable  ] right=339:[554:java/lang/Throwable  ]; paths rejected: normal control-flow cycle; labelled CFGs differ |
| `local/lito381/server/ClientSession#useWorldTreeCallSpell([BLlocal/lito381/server/BattleSpellCatalog$WorldTreeCallSpell;)V` | 缺呼叫 | method useWorldTreeCallSpell([BLlocal/lito381/server/BattleSpellCatalog$WorldTreeCallSpell;)V: left=148:[286:java/lang/Throwable  ] right=148:[290:java/lang/Throwable  ]; paths rejected: normal control-flow cycle; labelled CFGs differ |

### lambda 配對逐項清單

下表逐項列出仍未配對的方法及母方法呼叫點。箭頭右側為同一母方法相同 indy 序位的 staged／候選對照，僅作定位。具有對應呼叫點仍需比較捕獲型別與本體。未能唯一定位的項目保持待查；本輪未宣告 lambda 本體缺口已清零。


| 方法 | 狀態 | 母方法與對側呼叫點 |
|---|---|---|
| `local/lito381/server/ClientSession#lambda$autoSelectPendingRentalBattleHandoff$30(Llocal/lito381/server/CharacterSummary;)Z` | ONLY_LEFT | local/lito381/server/ClientSession#autoSelectPendingRentalBattleHandoff()Z @ 21 → local/lito381/server/ClientSession#lambda$autoSelectPendingRentalBattleHandoff$30(Llocal/lito381/server/CharacterSummary;)Z |
| `local/lito381/server/ClientSession#lambda$cleanupClassicWorldTransientCompanionsOnDisconnect$456(Llocal/lito381/server/BattleCompanion;)Z` | ONLY_RIGHT | local/lito381/server/ClientSession#cleanupClassicWorldTransientCompanionsOnDisconnect()V @ 8 → local/lito381/server/ClientSession#lambda$cleanupClassicWorldTransientCompanionsOnDisconnect$286(Llocal/lito381/server/BattleCompanion;)Z |
| `local/lito381/server/ClientSession#lambda$useAreaSilenceSpell$263(Llocal/lito381/server/ClientSession;)Z` | ONLY_RIGHT | local/lito381/server/ClientSession#nativeMaterialAreaSilenceBody([BLlocal/lito381/server/BattleSpellCatalog$AreaSilenceSpell;I)V @ 148 → 同一母方法下無此序位呼叫點 |
| `local/lito381/server/ClientSession#lambda$useAreaSilenceSpell$264(Llocal/lito381/server/ClientSession;)Z` | ONLY_RIGHT | local/lito381/server/ClientSession#nativeMaterialAreaSilenceBody([BLlocal/lito381/server/BattleSpellCatalog$AreaSilenceSpell;I)V @ 151 → 同一母方法下無此序位呼叫點 |
| `local/lito381/server/ClientSession#lambda$useAreaSilenceSpell$265(Llocal/lito381/server/ClientSession;)Z` | ONLY_RIGHT | local/lito381/server/ClientSession#nativeMaterialAreaSilenceBody([BLlocal/lito381/server/BattleSpellCatalog$AreaSilenceSpell;I)V @ 154 → 同一母方法下無此序位呼叫點 |
| `local/lito381/server/ClientSession#lambda$useAreaSilenceSpell$266(Llocal/lito381/server/ClientSession$Phase;JIIZLlocal/lito381/server/ClassicWorldMapCatalog$ClassicMap;Llocal/lito381/server/ClientSession;)Z` | ONLY_RIGHT | local/lito381/server/ClientSession#nativeMaterialAreaSilenceBody([BLlocal/lito381/server/BattleSpellCatalog$AreaSilenceSpell;I)V @ 162 → 同一母方法下無此序位呼叫點 |
| `local/lito381/server/ClientSession#lambda$useAreaSilenceSpell$268(Llocal/lito381/server/ClassicWorldMapCatalog$ClassicMap;Llocal/lito381/server/ClassicWorldMonsterPopulation$Snapshot;)Z` | ONLY_RIGHT | local/lito381/server/ClientSession#nativeMaterialAreaSilenceBody([BLlocal/lito381/server/BattleSpellCatalog$AreaSilenceSpell;I)V @ 189 → 同一母方法下無此序位呼叫點 |
| `local/lito381/server/ClientSession#lambda$useAreaSilenceSpell$269(Llocal/lito381/server/BattleQuestEncounter$Snapshot;)Z` | ONLY_RIGHT | local/lito381/server/ClientSession#nativeMaterialAreaSilenceBody([BLlocal/lito381/server/BattleSpellCatalog$AreaSilenceSpell;I)V @ 207 → 同一母方法下無此序位呼叫點 |
| `local/lito381/server/ClientSession#lambda$useAreaSilenceSpell$270(Llocal/lito381/server/BattleTimeAttackEncounter$Snapshot;)Z` | ONLY_RIGHT | local/lito381/server/ClientSession#nativeMaterialAreaSilenceBody([BLlocal/lito381/server/BattleSpellCatalog$AreaSilenceSpell;I)V @ 225 → 同一母方法下無此序位呼叫點 |
| `local/lito381/server/ClientSession#lambda$useCallOfNatureSpell$381(Llocal/lito381/server/ClientSession;Llocal/lito381/server/ClientSession$Phase;IILlocal/lito381/server/ClientSession;)Z` | ONLY_RIGHT | local/lito381/server/ClientSession#nativeMaterialCallNatureBody([BLlocal/lito381/server/BattleSpellCatalog$ResurrectionSpell;)V @ 456 → 同一母方法下無此序位呼叫點 |
| `local/lito381/server/ClientSession#lambda$useClassicWorldBlindSpell$244(IIIILlocal/lito381/server/ClientSession;)Z` | ONLY_RIGHT | local/lito381/server/ClientSession#nativeMaterialWorldBlindBody([BLlocal/lito381/server/BattleSpellCatalog$BlindSpell;)V @ 667 → 同一母方法下無此序位呼叫點 |
| `local/lito381/server/ClientSession#lambda$useClassicWorldDecayPotionSpell$281(II)I` | ONLY_RIGHT | local/lito381/server/ClientSession#nativeMaterialWorldDecayPotionBody([BLlocal/lito381/server/BattleSpellCatalog$DecayPotionSpell;)V @ 356 → 同一母方法下無此序位呼叫點 |
| `local/lito381/server/ClientSession#lambda$useClassicWorldMassSlowSpell$311(Llocal/lito381/server/Protocol$AttackSpellRequest;Llocal/lito381/server/ClientSession;)Z` | ONLY_RIGHT | local/lito381/server/ClientSession#nativeAreaWorldSlowBody([BLlocal/lito381/server/BattleSpellCatalog$MassSlowSpell;)V @ 48 → 同一母方法下無此序位呼叫點 |
| `local/lito381/server/ClientSession#lambda$useClassicWorldMassSlowSpell$313(Llocal/lito381/server/ClassicWorldMapCatalog$ClassicMap;IIILlocal/lito381/server/ClientSession;)Z` | ONLY_RIGHT | local/lito381/server/ClientSession#nativeAreaWorldSlowBody([BLlocal/lito381/server/BattleSpellCatalog$MassSlowSpell;)V @ 320 → 同一母方法下無此序位呼叫點 |
| `local/lito381/server/ClientSession#lambda$useClassicWorldMassSlowSpell$315(Llocal/lito381/server/Protocol$AttackSpellRequest;Llocal/lito381/server/ClientSession$MassSlowCompanionTarget;)Z` | ONLY_RIGHT | local/lito381/server/ClientSession#nativeAreaWorldSlowBody([BLlocal/lito381/server/BattleSpellCatalog$MassSlowSpell;)V @ 524 → 同一母方法下無此序位呼叫點 |
| `local/lito381/server/ClientSession#lambda$useClassicWorldMassSlowSpell$316(Llocal/lito381/server/Protocol$AttackSpellRequest;Llocal/lito381/server/ClassicWorldMonsterPopulation$Snapshot;)Z` | ONLY_RIGHT | local/lito381/server/ClientSession#nativeAreaWorldSlowBody([BLlocal/lito381/server/BattleSpellCatalog$MassSlowSpell;)V @ 530 → 同一母方法下無此序位呼叫點 |
| `local/lito381/server/ClientSession#lambda$useClassicWorldResurrectionSpell$379(Llocal/lito381/server/ClientSession;IILlocal/lito381/server/ClientSession;)Z` | ONLY_RIGHT | local/lito381/server/ClientSession#nativeMaterialWorldResurrectionBody([BLlocal/lito381/server/BattleSpellCatalog$ResurrectionSpell;)V @ 280 → 同一母方法下無此序位呼叫點 |
| `local/lito381/server/ClientSession#lambda$useClassicWorldSilenceSpell$247(IIIIIILlocal/lito381/server/ClientSession;)Z` | ONLY_RIGHT | local/lito381/server/ClientSession#nativeMaterialWorldSilenceBody([BLlocal/lito381/server/BattleSpellCatalog$SilenceSpell;)V @ 759 → 同一母方法下無此序位呼叫點 |
| `local/lito381/server/ClientSession#lambda$useClassicWorldSleepSpell$327(Llocal/lito381/server/ClientSession$PatienceTarget;Llocal/lito381/server/ClientSession$ClassicWorldSleepTarget;)Z` | ONLY_RIGHT | local/lito381/server/ClientSession#nativeAreaWorldSleepBody([BLlocal/lito381/server/BattleSpellCatalog$SleepSpell;)V @ 411 → 同一母方法下無此序位呼叫點 |
| `local/lito381/server/ClientSession#lambda$useMassSlowSpell$321(IILlocal/lito381/server/BattleSpellCatalog$MassSlowSpell;Llocal/lito381/server/ClientSession;)Z` | ONLY_RIGHT | local/lito381/server/ClientSession#nativeAreaBattleSlowBody([BLlocal/lito381/server/BattleSpellCatalog$MassSlowSpell;)V @ 200 → 同一母方法下無此序位呼叫點 |
| `local/lito381/server/ClientSession#lambda$useResurrectionSpell$383(Llocal/lito381/server/ClientSession;IILlocal/lito381/server/ClientSession;)Z` | ONLY_RIGHT | local/lito381/server/ClientSession#nativeMaterialResurrectionBody([BLlocal/lito381/server/BattleSpellCatalog$ResurrectionSpell;)V @ 214 → 同一母方法下無此序位呼叫點 |
| `local/lito381/server/ClientSession#lambda$useTamingSpell$384(IILlocal/lito381/server/ClientSession;)Z` | ONLY_RIGHT | local/lito381/server/ClientSession#nativeMaterialTamingBody([BLlocal/lito381/server/BattleSpellCatalog$TamingSpell;)V @ 562 → 同一母方法下無此序位呼叫點 |
| `local/lito381/server/ClientSession#lambda$useWorldNpcCancellation$217(IILlocal/lito381/server/ClientSession;)Z` | ONLY_RIGHT | local/lito381/server/ClientSession#nativeMaterialWorldNpcCancellationBody(Llocal/lito381/server/Protocol$ObjectSpellRequest;Llocal/lito381/server/ClientSession;Llocal/lito381/server/BattleCompanion;Llocal/lito381/server/BattleCompanion$Snapshot;Llocal/lito381/server/ClassicWorldMonsterPopulation$Snapshot;Llocal/lito381/server/BattleSpellCatalog$CancellationSpell;Llocal/lito381/server/CombatPoisonLifecycle$Scope;)V @ 465 → 同一母方法下無此序位呼叫點 |
| `local/lito381/server/ClientSession#lambda$useWorldPlayerCancellation$219(Llocal/lito381/server/ClientSession;Llocal/lito381/server/ClientSession;)Z` | ONLY_RIGHT | local/lito381/server/ClientSession#nativeMaterialWorldCancellationBody(Llocal/lito381/server/Protocol$ObjectSpellRequest;Llocal/lito381/server/ClientSession;Llocal/lito381/server/BattleSpellCatalog$CancellationSpell;Llocal/lito381/server/CombatPoisonLifecycle$Scope;)V @ 592 → 同一母方法下無此序位呼叫點 |
| `local/lito381/server/ClientSession#nativeSourceFrozenSlow_lambda$useMassSlowSpell$322(IILlocal/lito381/server/BattleSpellCatalog$MassSlowSpell;Ljava/lang/Object;Llocal/lito381/server/BattleQuestEncounter$Snapshot;)Z` | ONLY_RIGHT | local/lito381/server/ClientSession#nativeAreaBattleSlowBody([BLlocal/lito381/server/BattleSpellCatalog$MassSlowSpell;)V @ 222 → 同一母方法下無此序位呼叫點 |
| `local/lito381/server/ClientSession#nativeSourceFrozenSlow_lambda$useMassSlowSpell$323(IILlocal/lito381/server/BattleSpellCatalog$MassSlowSpell;Ljava/lang/Object;Llocal/lito381/server/BattleTimeAttackEncounter$Snapshot;)Z` | ONLY_RIGHT | local/lito381/server/ClientSession#nativeAreaBattleSlowBody([BLlocal/lito381/server/BattleSpellCatalog$MassSlowSpell;)V @ 238 → 同一母方法下無此序位呼叫點 |
| `local/lito381/server/ClientSession#repair014$lambda$useHealingSpell$406(Llocal/lito381/server/Protocol$ObjectSpellRequest;Llocal/lito381/server/ClientSession;)Z` | ONLY_RIGHT | local/lito381/server/ClientSession#nativeSourceControlledOriginal_useHealingSpell([BLlocal/lito381/server/BattleSpellCatalog$HealingSpell;)V @ 35 → local/lito381/server/ClientSession#lambda$nativeSourceControlledOriginal_useHealingSpell$385(Llocal/lito381/server/Protocol$ObjectSpellRequest;Llocal/lito381/server/ClientSession;)Z |
| `local/lito381/server/ClientSession#repair014$lambda$useHealingSpell$407(Llocal/lito381/server/BattleQuestEncounter$Snapshot;Llocal/lito381/server/BattleQuestEncounter$Snapshot;)Z` | ONLY_RIGHT | local/lito381/server/ClientSession#nativeSourceControlledOriginal_useHealingSpell([BLlocal/lito381/server/BattleSpellCatalog$HealingSpell;)V @ 213 → local/lito381/server/ClientSession#lambda$nativeSourceControlledOriginal_useHealingSpell$386(Llocal/lito381/server/BattleQuestEncounter$Snapshot;Llocal/lito381/server/BattleQuestEncounter$Snapshot;)Z |
| `local/lito381/server/ClientSession#repair014$lambda$useHealingSpell$408(Llocal/lito381/server/BattleGuardianTower$Snapshot;Llocal/lito381/server/BattleGuardianTower$Snapshot;)Z` | ONLY_RIGHT | local/lito381/server/ClientSession#nativeSourceControlledOriginal_useHealingSpell([BLlocal/lito381/server/BattleSpellCatalog$HealingSpell;)V @ 229 → local/lito381/server/ClientSession#lambda$nativeSourceControlledOriginal_useHealingSpell$387(Llocal/lito381/server/BattleGuardianTower$Snapshot;Llocal/lito381/server/BattleGuardianTower$Snapshot;)Z |
| `local/lito381/server/ClientSession#repair014$lambda$useHealingSpell$409(Llocal/lito381/server/BattleTimeAttackEncounter$Snapshot;Llocal/lito381/server/BattleTimeAttackEncounter$Snapshot;)Z` | ONLY_RIGHT | local/lito381/server/ClientSession#nativeSourceControlledOriginal_useHealingSpell([BLlocal/lito381/server/BattleSpellCatalog$HealingSpell;)V @ 245 → local/lito381/server/ClientSession#lambda$nativeSourceControlledOriginal_useHealingSpell$388(Llocal/lito381/server/BattleTimeAttackEncounter$Snapshot;Llocal/lito381/server/BattleTimeAttackEncounter$Snapshot;)Z |
| `local/lito381/server/ClientSession#repair014$lambda$useHealingSpell$410(Llocal/lito381/server/BattleGuardianTower$Snapshot;Llocal/lito381/server/BattleGuardianTower$Snapshot;)Z` | ONLY_RIGHT | local/lito381/server/ClientSession#nativeSourceControlledOriginal_useHealingSpell([BLlocal/lito381/server/BattleSpellCatalog$HealingSpell;)V @ 676 → local/lito381/server/ClientSession#lambda$nativeSourceControlledOriginal_useHealingSpell$389(Llocal/lito381/server/BattleGuardianTower$Snapshot;Llocal/lito381/server/BattleGuardianTower$Snapshot;)Z |
| `local/lito381/server/ClientSession#repair020$lambda$resolveDragonKnightNegativeTarget$191(Llocal/lito381/server/ClientSession$ArmorBreakTarget;)Llocal/lito381/server/BattleGuardBrake$Candidate;` | ONLY_RIGHT | local/lito381/server/ClientSession#resolveDragonKnightNegativeTarget(Llocal/lito381/server/ClientSession$Phase;JIIIIIZ)Llocal/lito381/server/ClientSession$ArmorBreakTarget; @ 551 → local/lito381/server/ClientSession#lambda$resolveDragonKnightNegativeTarget$176(Llocal/lito381/server/ClientSession$ArmorBreakTarget;)Llocal/lito381/server/BattleGuardBrake$Candidate; |
| `local/lito381/server/ClientSession#repair020$lambda$resolveDragonKnightNegativeTarget$192(Llocal/lito381/server/ClientSession$ArmorBreakTarget;)Llocal/lito381/server/BattleResistFear$Candidate;` | ONLY_RIGHT | local/lito381/server/ClientSession#resolveDragonKnightNegativeTarget(Llocal/lito381/server/ClientSession$Phase;JIIIIIZ)Llocal/lito381/server/ClientSession$ArmorBreakTarget; @ 558 → local/lito381/server/ClientSession#lambda$resolveDragonKnightNegativeTarget$177(Llocal/lito381/server/ClientSession$ArmorBreakTarget;)Llocal/lito381/server/BattleResistFear$Candidate; |
| `local/lito381/server/ClientSession#repair020$lambda$resolveDragonKnightNegativeTarget$193(IIILlocal/lito381/server/ClientSession$ArmorBreakTarget;)Z` | ONLY_RIGHT | local/lito381/server/ClientSession#resolveDragonKnightNegativeTarget(Llocal/lito381/server/ClientSession$Phase;JIIIIIZ)Llocal/lito381/server/ClientSession$ArmorBreakTarget; @ 590 → local/lito381/server/ClientSession#lambda$resolveDragonKnightNegativeTarget$178(IIILlocal/lito381/server/ClientSession$ArmorBreakTarget;)Z |
| `local/lito381/server/ClientSession#repair020$lambda$useAttackSpell$350(Llocal/lito381/server/Protocol$AttackSpellRequest;Llocal/lito381/server/ClientSession;)Z` | ONLY_RIGHT | local/lito381/server/ClientSession#nativeSourceControlledOriginal_useAttackSpell(Llocal/lito381/server/Protocol$AttackSpellRequest;Llocal/lito381/server/BattleSpellCatalog$AttackSpell;IIII)V @ 117 → local/lito381/server/ClientSession#lambda$nativeSourceControlledOriginal_useAttackSpell$379(Llocal/lito381/server/Protocol$AttackSpellRequest;Llocal/lito381/server/ClientSession;)Z |
| `local/lito381/server/ClientSession#repair020$lambda$useAttackSpell$351(I)I` | ONLY_RIGHT | local/lito381/server/ClientSession#nativeSourceControlledOriginal_useAttackSpell(Llocal/lito381/server/Protocol$AttackSpellRequest;Llocal/lito381/server/BattleSpellCatalog$AttackSpell;IIII)V @ 1072 → local/lito381/server/ClientSession#lambda$nativeSourceControlledOriginal_useAttackSpell$380(I)I |
| `local/lito381/server/ClientSession#repair020$lambda$useEntangleSpell$250(Llocal/lito381/server/Protocol$AttackSpellRequest;Llocal/lito381/server/ClientSession;)Z` | ONLY_RIGHT | local/lito381/server/ClientSession#nativeMaterialEntangleBody([BLlocal/lito381/server/BattleSpellCatalog$EntangleSpell;I)V @ 129 → 同一母方法下無此序位呼叫點 |
| `local/lito381/server/ClientSession#repair020$lambda$useEntangleSpell$251(Llocal/lito381/server/Protocol$AttackSpellRequest;Llocal/lito381/server/BattleCompanion$Snapshot;)Z` | ONLY_RIGHT | local/lito381/server/ClientSession#nativeMaterialEntangleBody([BLlocal/lito381/server/BattleSpellCatalog$EntangleSpell;I)V @ 186 → 同一母方法下無此序位呼叫點 |
| `local/lito381/server/ClientSession#repair020$lambda$useEraseMagicSpell$252(Llocal/lito381/server/Protocol$AttackSpellRequest;Llocal/lito381/server/ClientSession;)Z` | ONLY_RIGHT | local/lito381/server/ClientSession#nativeMaterialEraseBody([BLlocal/lito381/server/BattleSpellCatalog$EraseMagicSpell;I)V @ 129 → 同一母方法下無此序位呼叫點 |
| `local/lito381/server/ClientSession#repair020$lambda$useEraseMagicSpell$253(Llocal/lito381/server/Protocol$AttackSpellRequest;Llocal/lito381/server/BattleCompanion$Snapshot;)Z` | ONLY_RIGHT | local/lito381/server/ClientSession#nativeMaterialEraseBody([BLlocal/lito381/server/BattleSpellCatalog$EraseMagicSpell;I)V @ 186 → 同一母方法下無此序位呼叫點 |
| `local/lito381/server/ClientSession#repair020$lambda$useFreezeAttackSpell$343(Llocal/lito381/server/Protocol$AttackSpellRequest;Llocal/lito381/server/ClientSession;)Z` | ONLY_RIGHT | local/lito381/server/ClientSession#nativeSourceControlledOriginal_nativeFreezeAcceptedOriginalBattle([BLlocal/lito381/server/BattleSpellCatalog$FreezeAttackSpell;)V @ 34 → local/lito381/server/ClientSession#lambda$nativeSourceControlledOriginal_nativeFreezeAcceptedOriginalBattle$373(Llocal/lito381/server/Protocol$AttackSpellRequest;Llocal/lito381/server/ClientSession;)Z |
| `local/lito381/server/ClientSession#repair020$lambda$useFreezeAttackSpell$344(Llocal/lito381/server/Protocol$AttackSpellRequest;Llocal/lito381/server/BattleCompanion$Snapshot;)Z` | ONLY_RIGHT | local/lito381/server/ClientSession#nativeSourceControlledOriginal_nativeFreezeAcceptedOriginalBattle([BLlocal/lito381/server/BattleSpellCatalog$FreezeAttackSpell;)V @ 61 → local/lito381/server/ClientSession#lambda$nativeSourceControlledOriginal_nativeFreezeAcceptedOriginalBattle$374(Llocal/lito381/server/Protocol$AttackSpellRequest;Llocal/lito381/server/BattleCompanion$Snapshot;)Z |
| `local/lito381/server/ClientSession#repair020$lambda$useSleepSpell$328(Llocal/lito381/server/ClientSession$PatienceTarget;Llocal/lito381/server/ClientSession$BattleSleepTarget;)Z` | ONLY_RIGHT | local/lito381/server/ClientSession#nativeAreaBattleSleepBody([BLlocal/lito381/server/BattleSpellCatalog$SleepSpell;)V @ 490 → 同一母方法下無此序位呼叫點 |
| `local/lito381/server/ClientSession#repair021$lambda$attack$413(Llocal/lito381/server/RuntimeInventory$Item;Ljava/util/Map$Entry;)Z` | ONLY_RIGHT | local/lito381/server/ClientSession#nativeSourceControlledOriginal_attack(Llocal/lito381/server/Protocol$AttackRequest;Llocal/lito381/server/ClientSession$AttackEnvelope;)V @ 797 → local/lito381/server/ClientSession#lambda$nativeSourceControlledOriginal_attack$367(Llocal/lito381/server/RuntimeInventory$Item;Ljava/util/Map$Entry;)Z |
| `local/lito381/server/ClientSession#repair021$lambda$attack$414()I` | ONLY_RIGHT | local/lito381/server/ClientSession#nativeSourceControlledOriginal_attack(Llocal/lito381/server/Protocol$AttackRequest;Llocal/lito381/server/ClientSession$AttackEnvelope;)V @ 2302 → local/lito381/server/ClientSession#lambda$nativeSourceControlledOriginal_attack$368()I |
| `local/lito381/server/ClientSession#repair021$lambda$useLightningWand$119(Llocal/lito381/server/Protocol$TargetLocationInventoryUse;Llocal/lito381/server/ClientSession;)Z` | ONLY_RIGHT | local/lito381/server/ClientSession#nativeRawOriginal_useLightningWand(Llocal/lito381/server/RuntimeInventory$Item;Llocal/lito381/server/Protocol$TargetLocationInventoryUse;)V @ 51 → local/lito381/server/ClientSession#lambda$nativeRawOriginal_useLightningWand$354(Llocal/lito381/server/Protocol$TargetLocationInventoryUse;Llocal/lito381/server/ClientSession;)Z |
| `local/lito381/server/ClientSession#repair021$lambda$useLightningWand$120(Llocal/lito381/server/Protocol$TargetLocationInventoryUse;Llocal/lito381/server/BattleQuestEncounter$Snapshot;)Z` | ONLY_RIGHT | local/lito381/server/ClientSession#nativeRawOriginal_useLightningWand(Llocal/lito381/server/RuntimeInventory$Item;Llocal/lito381/server/Protocol$TargetLocationInventoryUse;)V @ 66 → local/lito381/server/ClientSession#lambda$nativeRawOriginal_useLightningWand$355(Llocal/lito381/server/Protocol$TargetLocationInventoryUse;Llocal/lito381/server/BattleQuestEncounter$Snapshot;)Z |
| `local/lito381/server/ClientSession#repair021$lambda$useLightningWand$121(Llocal/lito381/server/Protocol$TargetLocationInventoryUse;Llocal/lito381/server/BattleTimeAttackEncounter$Snapshot;)Z` | ONLY_RIGHT | local/lito381/server/ClientSession#nativeRawOriginal_useLightningWand(Llocal/lito381/server/RuntimeInventory$Item;Llocal/lito381/server/Protocol$TargetLocationInventoryUse;)V @ 81 → local/lito381/server/ClientSession#lambda$nativeRawOriginal_useLightningWand$356(Llocal/lito381/server/Protocol$TargetLocationInventoryUse;Llocal/lito381/server/BattleTimeAttackEncounter$Snapshot;)Z |
| `local/lito381/server/ClientSession#repair023$lambda$useHasteSpell$202(Llocal/lito381/server/Protocol$ObjectSpellRequest;Llocal/lito381/server/ClientSession;)Z` | ONLY_RIGHT | local/lito381/server/ClientSession#useHasteSpell([BLlocal/lito381/server/BattleSpellCatalog$HasteSpell;)V @ 51 → local/lito381/server/ClientSession#lambda$useHasteSpell$186(Llocal/lito381/server/Protocol$ObjectSpellRequest;Llocal/lito381/server/ClientSession;)Z |
| `local/lito381/server/ClientSession#repair023$lambda$useHasteSpell$203(ZLlocal/lito381/server/ClientSession;)Z` | ONLY_RIGHT | local/lito381/server/ClientSession#useHasteSpell([BLlocal/lito381/server/BattleSpellCatalog$HasteSpell;)V @ 55 → local/lito381/server/ClientSession#lambda$useHasteSpell$187(ZLlocal/lito381/server/ClientSession;)Z |
| `local/lito381/server/ClientSession#repair023$lambda$useHasteSpell$204(Llocal/lito381/server/Protocol$ObjectSpellRequest;Llocal/lito381/server/BattleCompanion$Snapshot;)Z` | ONLY_RIGHT | local/lito381/server/ClientSession#useHasteSpell([BLlocal/lito381/server/BattleSpellCatalog$HasteSpell;)V @ 89 → local/lito381/server/ClientSession#lambda$useHasteSpell$188(Llocal/lito381/server/Protocol$ObjectSpellRequest;Llocal/lito381/server/BattleCompanion$Snapshot;)Z |
| `local/lito381/server/ClientSession#repair023$lambda$useHasteSpell$205(Llocal/lito381/server/Protocol$ObjectSpellRequest;Llocal/lito381/server/BattleQuestEncounter$Snapshot;)Z` | ONLY_RIGHT | local/lito381/server/ClientSession#useHasteSpell([BLlocal/lito381/server/BattleSpellCatalog$HasteSpell;)V @ 138 → local/lito381/server/ClientSession#lambda$useHasteSpell$189(Llocal/lito381/server/Protocol$ObjectSpellRequest;Llocal/lito381/server/BattleQuestEncounter$Snapshot;)Z |
| `local/lito381/server/ClientSession#repair023$lambda$useHasteSpell$206(Llocal/lito381/server/Protocol$ObjectSpellRequest;Llocal/lito381/server/BattleTimeAttackEncounter$Snapshot;)Z` | ONLY_RIGHT | local/lito381/server/ClientSession#useHasteSpell([BLlocal/lito381/server/BattleSpellCatalog$HasteSpell;)V @ 157 → local/lito381/server/ClientSession#lambda$useHasteSpell$190(Llocal/lito381/server/Protocol$ObjectSpellRequest;Llocal/lito381/server/BattleTimeAttackEncounter$Snapshot;)Z |
| `local/lito381/server/ClientSession#repair032$lambda$useAreaAttackSpell$332(Llocal/lito381/server/Protocol$AttackSpellRequest;Llocal/lito381/server/ClientSession;)Z` | ONLY_RIGHT | local/lito381/server/ClientSession#nativeSourceControlledOriginal_useAreaAttackSpell(Llocal/lito381/server/Protocol$AttackSpellRequest;Llocal/lito381/server/BattleSpellCatalog$AreaAttackSpell;)V @ 42 → local/lito381/server/ClientSession#lambda$nativeSourceControlledOriginal_useAreaAttackSpell$378(Llocal/lito381/server/Protocol$AttackSpellRequest;Llocal/lito381/server/ClientSession;)Z |
| `local/lito381/server/ClientSession#repair032$lambda$useBlindSpell$245(Llocal/lito381/server/Protocol$AttackSpellRequest;Llocal/lito381/server/ClientSession;)Z` | ONLY_RIGHT | local/lito381/server/ClientSession#nativeMaterialBlindBody([BLlocal/lito381/server/BattleSpellCatalog$BlindSpell;)V @ 65 → 同一母方法下無此序位呼叫點 |
| `local/lito381/server/ClientSession#repair032$lambda$useCenteredAreaAttackSpell$333(JJLlocal/lito381/server/BattleSpellCatalog$AreaAttackSpell;IIILlocal/lito381/server/ClientSession;)Z` | ONLY_RIGHT | local/lito381/server/ClientSession#nativeSourceControlledOriginal_useCenteredAreaAttackSpell([BLlocal/lito381/server/BattleSpellCatalog$AreaAttackSpell;)V @ 67 → local/lito381/server/ClientSession#lambda$nativeSourceControlledOriginal_useCenteredAreaAttackSpell$381(JJLlocal/lito381/server/BattleSpellCatalog$AreaAttackSpell;IIILlocal/lito381/server/ClientSession;)Z |
| `local/lito381/server/ClientSession#repair032$lambda$useCenteredAreaAttackSpell$334(JIIILlocal/lito381/server/BattleGuardianTower$Snapshot;)Z` | ONLY_RIGHT | local/lito381/server/ClientSession#nativeSourceControlledOriginal_useCenteredAreaAttackSpell([BLlocal/lito381/server/BattleSpellCatalog$AreaAttackSpell;)V @ 213 → local/lito381/server/ClientSession#lambda$nativeSourceControlledOriginal_useCenteredAreaAttackSpell$382(JIIILlocal/lito381/server/BattleGuardianTower$Snapshot;)Z |
| `local/lito381/server/ClientSession#repair032$lambda$useCenteredAreaAttackSpell$335(JIIILlocal/lito381/server/BattleQuestEncounter$Snapshot;)Z` | ONLY_RIGHT | local/lito381/server/ClientSession#nativeSourceControlledOriginal_useCenteredAreaAttackSpell([BLlocal/lito381/server/BattleSpellCatalog$AreaAttackSpell;)V @ 228 → local/lito381/server/ClientSession#lambda$nativeSourceControlledOriginal_useCenteredAreaAttackSpell$383(JIIILlocal/lito381/server/BattleQuestEncounter$Snapshot;)Z |
| `local/lito381/server/ClientSession#repair032$lambda$useCenteredAreaAttackSpell$336(JIIILlocal/lito381/server/BattleTimeAttackEncounter$Snapshot;)Z` | ONLY_RIGHT | local/lito381/server/ClientSession#nativeSourceControlledOriginal_useCenteredAreaAttackSpell([BLlocal/lito381/server/BattleSpellCatalog$AreaAttackSpell;)V @ 243 → local/lito381/server/ClientSession#lambda$nativeSourceControlledOriginal_useCenteredAreaAttackSpell$384(JIIILlocal/lito381/server/BattleTimeAttackEncounter$Snapshot;)Z |
| `local/lito381/server/ClientSession#repair032$lambda$useManaDrainSpell$240(Llocal/lito381/server/Protocol$AttackSpellRequest;Llocal/lito381/server/ClientSession;)Z` | ONLY_RIGHT | local/lito381/server/ClientSession#nativeSourceRemainingOriginal39([BLlocal/lito381/server/BattleSpellCatalog$ManaDrainSpell;)V @ 79 → local/lito381/server/ClientSession#lambda$nativeSourceRemainingOriginal39$412(Llocal/lito381/server/Protocol$AttackSpellRequest;Llocal/lito381/server/ClientSession;)Z |
| `local/lito381/server/ClientSession#repair032$lambda$useManaDrainSpell$241(II)I` | ONLY_RIGHT | local/lito381/server/ClientSession#nativeSourceRemainingOriginal39([BLlocal/lito381/server/BattleSpellCatalog$ManaDrainSpell;)V @ 436 → local/lito381/server/ClientSession#lambda$nativeSourceRemainingOriginal39$413(II)I |
| `local/lito381/server/ClientSession#repair032$lambda$useManaDrainSpell$242(II)I` | ONLY_RIGHT | local/lito381/server/ClientSession#nativeSourceRemainingOriginal39([BLlocal/lito381/server/BattleSpellCatalog$ManaDrainSpell;)V @ 713 → local/lito381/server/ClientSession#lambda$nativeSourceRemainingOriginal39$414(II)I |
| `local/lito381/server/ClientSession#repair032$lambda$useWeaknessSpell$306(Llocal/lito381/server/Protocol$AttackSpellRequest;Llocal/lito381/server/ClientSession;)Z` | ONLY_RIGHT | local/lito381/server/ClientSession#nativeMaterialWeaknessBody([BLlocal/lito381/server/BattleSpellCatalog$WeaknessSpell;)V @ 97 → 同一母方法下無此序位呼叫點 |
| `local/lito381/server/ClientSession#repair032$lambda$useWeaknessSpell$307(I)I` | ONLY_RIGHT | local/lito381/server/ClientSession#nativeMaterialWeaknessBody([BLlocal/lito381/server/BattleSpellCatalog$WeaknessSpell;)V @ 460 → 同一母方法下無此序位呼叫點 |
| `local/lito381/server/ClientSession#lambda$cleanupClassicWorldTransientCompanionsOnDisconnect$286(Llocal/lito381/server/BattleCompanion;)Z` | ONLY_LEFT | local/lito381/server/ClientSession#cleanupClassicWorldTransientCompanionsOnDisconnect()V @ 8 → local/lito381/server/ClientSession#lambda$cleanupClassicWorldTransientCompanionsOnDisconnect$456(Llocal/lito381/server/BattleCompanion;)Z |
| `local/lito381/server/ClientSession#lambda$completeClassicWorldPortal$162(Llocal/lito381/server/Protocol$PortalDestination;Llocal/lito381/server/ClientSession$PendingClassicWorldPortal;)V` | ONLY_LEFT | local/lito381/server/ClientSession#completeClassicWorldPortal(Llocal/lito381/server/Protocol$PortalDestination;Llocal/lito381/server/ClientSession$PendingClassicWorldPortal;)V @ 6 → 同一母方法下無此序位呼叫點 |
| `local/lito381/server/ClientSession#lambda$nativeSourceHealingOriginalGroup$402(Llocal/lito381/server/ClientSession$Phase;Llocal/lito381/server/ClassicWorldMapCatalog$ClassicMap;IILlocal/lito381/server/ArenaMapCatalog$Arena;Llocal/lito381/server/BattleSpellCatalog$GroupHealingSpell;Llocal/lito381/server/ClientSession;)Z` | ONLY_LEFT | local/lito381/server/ClientSession#nativeSourceHealingOriginalGroup([BLlocal/lito381/server/BattleSpellCatalog$GroupHealingSpell;)V @ 171 → local/lito381/server/ClientSession#lambda$useGroupHealingSpell$398(Llocal/lito381/server/ClientSession;)Z |
| `local/lito381/server/ClientSession#lambda$nativeSourceHealingOriginalGroup$406(Llocal/lito381/server/ClientSession$Phase;Llocal/lito381/server/ClassicWorldMapCatalog$ClassicMap;IILlocal/lito381/server/ArenaMapCatalog$Arena;Llocal/lito381/server/BattleSpellCatalog$GroupHealingSpell;Llocal/lito381/server/BattleCompanion$Snapshot;)Z` | ONLY_LEFT | local/lito381/server/ClientSession#nativeSourceHealingOriginalGroup([BLlocal/lito381/server/BattleSpellCatalog$GroupHealingSpell;)V @ 209 → local/lito381/server/ClientSession#lambda$useGroupHealingSpell$403(I)I |
| `local/lito381/server/ClientSession#lambda$useClassicWorldAreaAttackSpell$240(Llocal/lito381/server/Protocol$AttackSpellRequest;Llocal/lito381/server/BattleSpellCatalog$AreaAttackSpell;)V` | ONLY_LEFT | local/lito381/server/ClientSession#useClassicWorldAreaAttackSpell(Llocal/lito381/server/Protocol$AttackSpellRequest;Llocal/lito381/server/BattleSpellCatalog$AreaAttackSpell;)V @ 10 → 同一母方法下無此序位呼叫點 |
| `local/lito381/server/ClientSession#lambda$useClassicWorldAttackSpell$241(Llocal/lito381/server/Protocol$AttackSpellRequest;Llocal/lito381/server/BattleSpellCatalog$AttackSpell;)V` | ONLY_LEFT | local/lito381/server/ClientSession#useClassicWorldAttackSpell(Llocal/lito381/server/Protocol$AttackSpellRequest;Llocal/lito381/server/BattleSpellCatalog$AttackSpell;)V @ 10 → 同一母方法下無此序位呼叫點 |
| `local/lito381/server/ClientSession#lambda$useClassicWorldLineAttackSpell$238([BLlocal/lito381/server/BattleSpellCatalog$LineAttackSpell;)V` | ONLY_LEFT | local/lito381/server/ClientSession#useClassicWorldLineAttackSpell([BLlocal/lito381/server/BattleSpellCatalog$LineAttackSpell;)V @ 5 → 同一母方法下無此序位呼叫點 |
| `local/lito381/server/ClientSession#lambda$useTargetNegativeStatusSpell$222([BLlocal/lito381/server/BattleSpellCatalog$TargetNegativeStatusSpell;ILlocal/lito381/server/ClientSession$TargetNegativeStatusKind;)V` | ONLY_LEFT | local/lito381/server/ClientSession#useTargetNegativeStatusSpell([BLlocal/lito381/server/BattleSpellCatalog$TargetNegativeStatusSpell;ILlocal/lito381/server/ClientSession$TargetNegativeStatusKind;)V @ 7 → 同一母方法下無此序位呼叫點 |

72 個方法均已定位母方法。37 列在對側同一母方法的 lambda indy 序列缺少該序位，其餘 35 列列出對照目標。這項序位盤點仍需逐一檢查本體與捕獲參數。

### 本輪通過的 78 個原 DIFF 方法

| 方法 |
|---|
| `local/lito381/server/ClientSession#activatePendingRentalBattleHandoff(Ljava/lang/String;)Z` |
| `local/lito381/server/ClientSession#applyClassicWorldDeathItem(Llocal/lito381/server/RuntimeInventory$Item;ZLjava/lang/String;)V` |
| `local/lito381/server/ClientSession#applyWeaponBreakWeaponDamage(I)V` |
| `local/lito381/server/ClientSession#awardClassicWorldCkewQuestDrop(Llocal/lito381/server/ClassicWorldMonsterPopulation$Snapshot;)V` |
| `local/lito381/server/ClientSession#awardClassicWorldRoyalTrialQuestDrop(Llocal/lito381/server/ClassicWorldMonsterPopulation$Snapshot;)V` |
| `local/lito381/server/ClientSession#buySell([B)V` |
| `local/lito381/server/ClientSession#buySpells([B)V` |
| `local/lito381/server/ClientSession#claimOnePendingReward(Llocal/lito381/server/RepositoryPendingReward;)Llocal/lito381/server/RepositoryPendingRewardClaim;` |
| `local/lito381/server/ClientSession#classicBuySell([B)V` |
| `local/lito381/server/ClientSession#completeClassicWorldDspyArrival(Llocal/lito381/server/ClassicWorldDspyEscorts$Snapshot;)V` |
| `local/lito381/server/ClientSession#createClassicClan([B)V` |
| `local/lito381/server/ClientSession#damageAttackingWeapon(Llocal/lito381/server/SourceWeaponPhysicalStats;Llocal/lito381/server/SourceWeaponPhysicalStats;ZJ)V` |
| `local/lito381/server/ClientSession#debitClassicWorldProjectile(Llocal/lito381/server/SourceWeaponPhysicalStats;)Llocal/lito381/server/ClientSession$ClassicWorldProjectileDebit;` |
| `local/lito381/server/ClientSession#destroyInventoryItem([B)V` |
| `local/lito381/server/ClientSession#destroySqlPhysicalLetter(Llocal/lito381/server/RuntimeInventory$Item;)V` |
| `local/lito381/server/ClientSession#dropInventoryItem([B)V` |
| `local/lito381/server/ClientSession#enterChannel([B)V` |
| `local/lito381/server/ClientSession#exchangeSpells([B)V` |
| `local/lito381/server/ClientSession#getClassicWorldGroundItem(Llocal/lito381/server/Protocol$GroundPickup;)V` |
| `local/lito381/server/ClientSession#getGroundItem([B)V` |
| `local/lito381/server/ClientSession#giveInventoryItem([B)V` |
| `local/lito381/server/ClientSession#gotoClassicWorldShip(Llocal/lito381/server/Protocol$GotoMapRequest;Llocal/lito381/server/ClientSession$PendingShipTicket;)V` |
| `local/lito381/server/ClientSession#handleClassicCastleTrebuchetAction(Llocal/lito381/server/Protocol$HypertextActionRequest;Llocal/lito381/server/ClassicWorldCastleWarField$WarObject;)V` |
| `local/lito381/server/ClientSession#handleRentalTownTeleportAction(Llocal/lito381/server/NativeLitoRentalTownNpcCatalog$Npc;Ljava/lang/String;)V` |
| `local/lito381/server/ClientSession#harvestClassicWorldGuardian(Llocal/lito381/server/ClassicWorldMonsterPopulation$Snapshot;I)Z` |
| `local/lito381/server/ClientSession#joinRoom([BLjava/lang/String;Z)V` |
| `local/lito381/server/ClientSession#liberateClassicWorldPet(Llocal/lito381/server/BattleCompanion;Llocal/lito381/server/BattleCompanion$Snapshot;)Z` |
| `local/lito381/server/ClientSession#login([B)V` |
| `local/lito381/server/ClientSession#movementUnlock([B)V` |
| `local/lito381/server/ClientSession#nativeSourceCubeQuestWaveAdvance(Llocal/lito381/server/ClientSession;IILjava/util/List;ZLjava/lang/String;Llocal/lito381/server/NativeSourceCubeNpc$Commit;)V` |
| `local/lito381/server/ClientSession#nativeSourcePhysicalQuestWaveAdvance(Llocal/lito381/server/ClientSession;IILjava/util/List;ZLjava/lang/String;Llocal/lito381/server/NativeSourcePhysicalNpc$Commit;)V` |
| `local/lito381/server/ClientSession#nativeSourcePoisonQuestWaveAdvance(Llocal/lito381/server/ClientSession;IILjava/util/List;ZLjava/lang/String;Llocal/lito381/server/NativeSourcePoisonNpc$Commit;)V` |
| `local/lito381/server/ClientSession#payClassicWorldHouseTax(Llocal/lito381/server/ClassicWorldNpcCatalog$StaticNpc;)V` |
| `local/lito381/server/ClientSession#performClassicWorldCraft(Llocal/lito381/server/ClassicWorldCraftCatalog$Recipe;I)V` |
| `local/lito381/server/ClientSession#performClassicWorldHotelRental(Llocal/lito381/server/ClientSession$PendingHotelRental;ILlocal/lito381/server/ClassicWorldNpcCatalog$StaticNpc;)V` |
| `local/lito381/server/ClientSession#performRentalTownHotelRental(Llocal/lito381/server/NativeLitoRentalTownNpcCatalog$Npc;I)V` |
| `local/lito381/server/ClientSession#redirectToChannelPort(I)V` |
| `local/lito381/server/ClientSession#rentClassicWorldWaterDragonPass(Llocal/lito381/server/ClassicWorldNpcCatalog$StaticNpc;)V` |
| `local/lito381/server/ClientSession#rentalItemSelectionContext()Z` |
| `local/lito381/server/ClientSession#rentalTownBuySell([B)V` |
| `local/lito381/server/ClientSession#repairInventoryItem([B)V` |
| `local/lito381/server/ClientSession#requestLitoCharacterInfo([B)V` |
| `local/lito381/server/ClientSession#requestQuit()V` |
| `local/lito381/server/ClientSession#resetBattleVitals()V` |
| `local/lito381/server/ClientSession#respawnAtBattleSpawn()V` |
| `local/lito381/server/ClientSession#say([BI)V` |
| `local/lito381/server/ClientSession#selectClassicServer([B)V` |
| `local/lito381/server/ClientSession#serverClassicWorldCkewAltarCompleted(ILlocal/lito381/server/ClassicWorldMonsterPopulation$Snapshot;)V` |
| `local/lito381/server/ClientSession#serverClassicWorldCkewStageTeleport(JILlocal/lito381/server/ClassicWorldCkewCatalog$Location;)V` |
| `local/lito381/server/ClientSession#serverExpireTimedInventory(J)V` |
| `local/lito381/server/ClientSession#serverRentalHotelLeaseInvalidated(Llocal/lito381/server/RentalTownHotelRegistry$Invalidation;Ljava/lang/String;)V` |
| `local/lito381/server/ClientSession#setReady([B)V` |
| `local/lito381/server/ClientSession#sourceProcQuestWaveAdvance(IILjava/util/List;ZLjava/lang/String;Llocal/lito381/server/NativeProcEncounterPublication$Context;)V` |
| `local/lito381/server/ClientSession#startRentalTownScene()V` |
| `local/lito381/server/ClientSession#transferClassicWorldCompanionItems(Llocal/lito381/server/BattleCompanion;Z)V` |
| `local/lito381/server/ClientSession#transferCompanionItems(Llocal/lito381/server/BattleCompanion;)V` |
| `local/lito381/server/ClientSession#useClassicWorldCkewDestructionMedicine(Llocal/lito381/server/RuntimeInventory$Item;)V` |
| `local/lito381/server/ClientSession#useCustomAreaTeleportItem(Llocal/lito381/server/RuntimeInventory$Item;)V` |
| `local/lito381/server/ClientSession#useExperienceGem(Llocal/lito381/server/RuntimeInventory$Item;)V` |
| `local/lito381/server/ClientSession#useHomeTeleportScroll(Llocal/lito381/server/RuntimeInventory$Item;Llocal/lito381/server/Protocol$HomeTeleportInventoryUse;)V` |
| `local/lito381/server/ClientSession#useLetterPaper(Llocal/lito381/server/RuntimeInventory$Item;Llocal/lito381/server/Protocol$LetterComposeInventoryUse;)V` |
| `local/lito381/server/ClientSession#useNativeMagicDoll(Llocal/lito381/server/RuntimeInventory$Item;)V` |
| `local/lito381/server/ClientSession#useRentalTownItem([BLlocal/lito381/server/RuntimeInventory$Item;)V` |
| `local/lito381/server/ClientSession#useSourceArmor(Llocal/lito381/server/RuntimeInventory$Item;)V` |
| `local/lito381/server/ClientSession#useSqlLetterPaper(Llocal/lito381/server/RuntimeInventory$Item;Llocal/lito381/server/Protocol$LetterComposeInventoryUse;Llocal/lito381/server/ClientSession;)V` |
| `local/lito381/server/ClientSession#writeBoardTopic([B)V` |
| `local/lito381/server/GameHub#botTicks(J)Ljava/util/List;` |
| `local/lito381/server/GameHub#createRoom(ILlocal/lito381/server/GameHub$RoomSettings;)J` |
| `local/lito381/server/GameHub#cubeHarmonyTicks(J)Ljava/util/List;` |
| `local/lito381/server/GameHub#cubeQuakeTicks(J)Ljava/util/List;` |
| `local/lito381/server/GameHub#cubeShockTicks(J)Ljava/util/List;` |
| `local/lito381/server/GameHub#cubeTicks(J)Ljava/util/List;` |
| `local/lito381/server/GameHub#forceWinner(JI)Llocal/lito381/server/BattleRules$Resolution;` |
| `local/lito381/server/GameHub#groundTickSafely()V` |
| `local/lito381/server/GameHub#groundTicks(J)Ljava/util/List;` |
| `local/lito381/server/GameHub#nativeSummonOriginalDisconnect(Llocal/lito381/server/ClientSession;)Ljava/util/List;` |
| `local/lito381/server/GameHub#resolveBattleDisconnectSafely(JJ)V` |
| `local/lito381/server/GameHub#tryJoinRoom(JLlocal/lito381/server/ClientSession;IILjava/lang/String;)Llocal/lito381/server/GameHub$RoomJoinOutcome;` |

## 推測

部分線性次數差、finally、POP 與 boolean 匯合可能屬工具未證明的形狀。上表亦存在明確新增的業務呼叫，後續須分開處理。呼叫點對應只能作診斷，無法取代完整方法與類別驗收。

## 給下一棒的提示

1. 本機執行器繼續補齊上表的 Java 移植缺口，優先處理技能消耗、material、companion 與 combat 路徑。保留本輪已通過的方法與 T029 八方法，從新目錄重建全部 362 entries。
2. Claude 主控審查 sourceApplicationRun 的完整 handler／finally 差異與純形狀代表，依既有 U2-1 規則裁定後續工具工作。保留 needs:claude，整批仍不符合再送審／B-land 條件。
3. lambda 表已定位 72 個未配對方法的母方法呼叫點，仍須核對對側同序位呼叫的捕獲型別與本體，並補上 staged 缺少的呼叫點與本體。本輪尚未完成所有「缺呼叫／順序不同一律修 Java」與配對缺口清零的要求。
4. 依任務安排，單元、回歸、實機驗收由 B-land 處理，本輪只做編譯與靜態語意驗證。

## 完整 PR 差異與產出路徑

交付路徑為三份本批 staged Java 的累積 PR 差異、核准清單條目與本報告。本輪實際編輯 ClientSession.java、GameHub.java、exceptions-exempt.txt、issue-70.md。完整 PR 額外包含前輪 Protocol.java 與 extra-methods-approved.txt。完整 PR 格式檢查涵蓋 `origin/main` 到整個工作樹及提交後的 `origin/main...HEAD`，收據在 `scratch/issue-70/full-pr-diff-check-259.txt`。

本輪主動寫入均在指定 worktree。Git fetch／merge／commit／push 使用 worktree 共用的 Git 中繼資料。交付前以 `e7274ca` 再合併 `origin/main@e8871cd`，帶入 B08 及 docs/REPORT.md 的上游索引。完整 PR 檔案清單共六個核准檔案。原專案資源、正式程序、DB、機密設定、封存與 release/releases 皆未操作。

最後同步與四處行尾空白修正後，再於全新 `target/postmerge259`、`postmerge259-tool`、`postmerge259-annotations` 重建。應用 JAR、工具與 annotation 的全部 class bytes 均與上述驗證產物相同，批次裁切後的五份核准／entry 清單雜湊亦相同。收據為 `postmerge259-build.log`、`postmerge259-input-check.json`，完整語意與 audit 結果因此仍適用於交付來源。


最終產物 `scratch/issue-70/target/delivery259.jar` SHA-256 為 `3230ecbe3339e0f6992566d3515bdadc2e1005c2a2ff6efbd35502f39523274a`。

| 輸入 | SHA-256 |
|---|---|
| lito伺服器\LiTo381-Server\reference\base-0.6.71.jar | e1498871c7be6b287821c57ef5d95933db1740d9b4d4a80f6c3465f21d99d498 |
| lito伺服器\LiTo381-Server\reference\candidate-50635f7e.jar | 50635f7edca8ec6ce77787cc32371c7590b497e48ea202f8a679caf3f517b729 |

本機證據集中在 `scratch/issue-70/`，包含 `verify-259.py`、`evidence259.py`、`Audit259.java`、`Calls259.java`、`Dump259.java`、`report259.py`、`field-ports259.json`、`more-field-ports259.json`、`poison-ports259.json`、`latest-command.json`、`delivery259-{diff.json,diff.md,audit.txt,dump.txt,calls.tsv,classified.json}`、完整三類 candidate javap 與 formal ClientSession javap。建置輸出位於 target，均保留為本機證據。

從 repo 根目錄可依下列方式重建。第一個命令必須傳入全新輸出名稱。第二個命令使用 latest-command.json 所指向的實際產物，產生完整呼叫點、指令與重點方法 audit。



```powershell
& 'C:/Users/CHAOREN/.cache/codex-runtimes/codex-primary-runtime/dependencies/python/python.exe' -B -X utf8 scratch/issue-70/verify-259.py reproduce259
& 'C:/Users/CHAOREN/.cache/codex-runtimes/codex-primary-runtime/dependencies/python/python.exe' -B -X utf8 scratch/issue-70/evidence259.py
```


## 完整代表差異

每類依完整差異長度選三個新代表，多呼叫類僅一個方法，列出該方法。保留全部 hunks、TRY、META 與 bootstrap 內容。sourceApplicationRun 另附完整差異。

### 多呼叫　`local/lito381/server/ClientSession#quitFight()V`

<details>
<summary>完整差異</summary>



```diff
--- staged/local/lito381/server/ClientSession#quitFight()V
+++ candidate/local/lito381/server/ClientSession#quitFight()V
@@ -1,11 +1,12 @@
 META 2 null [java/io/IOException]
-TRY [79:[95:java/io/IOException  , 115:java/lang/Throwable  ], 81:[95:java/io/IOException  , 115:java/lang/Throwable  ], 83:[95:java/io/IOException  , 115:java/lang/Throwable  ], 85:[95:java/io/IOException  , 115:java/lang/Throwable  ], 86:[95:java/io/IOException  , 115:java/lang/Throwable  ], 88:[95:java/io/IOException  , 115:java/lang/Throwable  ], 99:[115:java/lang/Throwable  ], 100:[115:java/lang/Throwable  ], 102:[115:java/lang/Throwable  ], 103:[115:java/lang/Throwable  ], 105:[115:java/lang/Throwable  ], 106:[115:java/lang/Throwable  ], 107:[115:java/lang/Throwable  ], 108:[115:java/lang/Throwable  ], 206:[220:java/io/IOException  , 234:java/lang/Throwable  ], 208:[220:java/io/IOException  , 234:java/lang/Throwable  ], 210:[220:java/io/IOException  , 234:java/lang/Throwable  ], 212:[220:java/io/IOException  , 234:java/lang/Throwable  ], 213:[220:java/io/IOException  , 234:java/lang/Throwable  ], 215:[220:java/io/IOException  , 234:java/lang/Throwable  ], 221:[234:java/lang/Throwable  ], 223:[234:java/lang/Throwable  ], 224:[234:java/lang/Throwable  ], 226:[234:java/lang/Throwable  ], 227:[234:java/lang/Throwable  ], 228:[234:java/lang/Throwable  ], 229:[234:java/lang/Throwable  ]]
-ALOAD 0
-GETFIELD local/lito381/server/ClientSession.rentalTownTransferPending:Z
-IFEQ L163
-ALOAD 0
-GETFIELD local/lito381/server/ClientSession.rentalTownRoomState:Z
-IFEQ L163
+TRY [86:[102:java/io/IOException  , 122:java/lang/Throwable  ], 88:[102:java/io/IOException  , 122:java/lang/Throwable  ], 90:[102:java/io/IOException  , 122:java/lang/Throwable  ], 92:[102:java/io/IOException  , 122:java/lang/Throwable  ], 93:[102:java/io/IOException  , 122:java/lang/Throwable  ], 95:[102:java/io/IOException  , 122:java/lang/Throwable  ], 106:[122:java/lang/Throwable  ], 107:[122:java/lang/Throwable  ], 109:[122:java/lang/Throwable  ], 110:[122:java/lang/Throwable  ], 112:[122:java/lang/Throwable  ], 113:[122:java/lang/Throwable  ], 114:[122:java/lang/Throwable  ], 115:[122:java/lang/Throwable  ], 195:[209:java/io/IOException  , 223:java/lang/Throwable  ], 197:[209:java/io/IOException  , 223:java/lang/Throwable  ], 199:[209:java/io/IOException  , 223:java/lang/Throwable  ], 201:[209:java/io/IOException  , 223:java/lang/Throwable  ], 202:[209:java/io/IOException  , 223:java/lang/Throwable  ], 204:[209:java/io/IOException  , 223:java/lang/Throwable  ], 210:[223:java/lang/Throwable  ], 212:[223:java/lang/Throwable  ], 213:[223:java/lang/Throwable  ], 215:[223:java/lang/Throwable  ], 216:[223:java/lang/Throwable  ], 217:[223:java/lang/Throwable  ], 218:[223:java/lang/Throwable  ]]
+ALOAD 0
+GETFIELD local/lito381/server/ClientSession.phase:Llocal/lito381/server/ClientSession$Phase;
+GETSTATIC local/lito381/server/ClientSession$Phase.BATTLE:Llocal/lito381/server/ClientSession$Phase;
+IF_ACMPNE L7
+ALOAD 0
+INVOKESPECIAL local/lito381/server/ClientSession.settleRequestedBattleExit()V
+RETURN
 ALOAD 0
 CONST null
 PUTFIELD local/lito381/server/ClientSession.pendingWarehousePassword:Llocal/lito381/server/ClientSession$PendingWarehousePassword;
@@ -17,11 +18,17 @@
 PUTFIELD local/lito381/server/ClientSession.pendingPetRename:Llocal/lito381/server/ClientSession$PendingPetRename;
 ALOAD 0
 INVOKESPECIAL local/lito381/server/ClientSession.clearControlledCompanionTargets()V
+ALOAD 0
+GETFIELD local/lito381/server/ClientSession.rentalTownTransferPending:Z
+IFEQ L170
+ALOAD 0
+GETFIELD local/lito381/server/ClientSession.rentalTownRoomState:Z
+IFEQ L170
 ALOAD 0
 GETFIELD local/lito381/server/ClientSession.pendingRentalTeleportEndpoint:Llocal/lito381/server/NativeLitoRentalTeleportEndpointCatalog$Endpoint;
 ASTORE 1
 ALOAD 1
-IFNULL L124
+IFNULL L131
 ALOAD 1
 INVOKEVIRTUAL local/lito381/server/NativeLitoRentalTeleportEndpointCatalog$Endpoint.ipv4()J
 ALOAD 0
@@ -29,9 +36,9 @@
 INVOKEVIRTUAL local/lito381/server/ServerConfig.advertisedHost()Ljava/lang/String;
 INVOKESTATIC local/lito381/server/ClientSession.ipv4LittleEndian(Ljava/lang/String;)J
 LCMP
-IFNE L32
+IFNE L39
 CONST Integer:1
-GOTO L33
+GOTO L40
 CONST Integer:0
 ISTORE 2
 ALOAD 0
@@ -59,7 +66,7 @@
 CONST str:"phase"
 INVOKESTATIC local/lito381/server/NativePlayerCommit.scene(Llocal/lito381/server/ClientSession;Ljava/lang/Object;Ljava/lang/String;)V
 ILOAD 2
-IFNE L78
+IFNE L85
 ALOAD 0
 GETFIELD local/lito381/server/ClientSession.hub:Llocal/lito381/server/GameHub;
 ALOAD 0
@@ -90,14 +97,14 @@
 CONST str:"S_LITO_QUIT_FIGHT rental-town configured endpoint"
 INVOKESPECIAL local/lito381/server/ClientSession.sendEncrypted([BLjava/lang/String;)V
 ILOAD 2
-IFNE L123
+IFNE L130
 ALOAD 0
 CONST str:"rental-town-cross-endpoint-handoff"
 INVOKEVIRTUAL local/lito381/server/ClientSession.forceDisconnect(Ljava/lang/String;)V
-GOTO L123
+GOTO L130
 ASTORE 4
 ILOAD 2
-IFEQ L100
+IFEQ L107
 ALOAD 4
 ATHROW
 GETSTATIC java/lang/System.out:Ljava/io/PrintStream;
@@ -110,14 +117,14 @@
 INVOKEDYNAMIC makeConcatWithConstants(Ljava/lang/String;Ljava/lang/String;)Ljava/lang/String; bsm=H6 java/lang/invoke/StringConcatFactory.makeConcatWithConstants(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;Ljava/lang/String;[Ljava/lang/Object;)Ljava/lang/invoke/CallSite; args=[str:"[LiTo] rental-town old endpoint already reset account=\u0001 reply-skipped=\u0001"]
 INVOKEVIRTUAL java/io/PrintStream.println(Ljava/lang/String;)V
 ILOAD 2
-IFNE L123
+IFNE L130
 ALOAD 0
 CONST str:"rental-town-cross-endpoint-handoff"
 INVOKEVIRTUAL local/lito381/server/ClientSession.forceDisconnect(Ljava/lang/String;)V
-GOTO L123
+GOTO L130
 ASTORE 5
 ILOAD 2
-IFNE L121
+IFNE L128
 ALOAD 0
 CONST str:"rental-town-cross-endpoint-handoff"
 INVOKEVIRTUAL local/lito381/server/ClientSession.forceDisconnect(Ljava/lang/String;)V
@@ -164,30 +171,12 @@
 INVOKEVIRTUAL java/io/PrintStream.println(Ljava/lang/String;)V
 RETURN
 ALOAD 0
-GETFIELD local/lito381/server/ClientSession.phase:Llocal/lito381/server/ClientSession$Phase;
-GETSTATIC local/lito381/server/ClientSession$Phase.BATTLE:Llocal/lito381/server/ClientSession$Phase;
-IF_ACMPNE L170
-ALOAD 0
-INVOKESPECIAL local/lito381/server/ClientSession.settleRequestedBattleExit()V
-RETURN
-ALOAD 0
-CONST null
-PUTFIELD local/lito381/server/ClientSession.pendingWarehousePassword:Llocal/lito381/server/ClientSession$PendingWarehousePassword;
-ALOAD 0
-CONST null
-PUTFIELD local/lito381/server/ClientSession.pendingSlaveTarget:Llocal/lito381/server/ClientSession$PendingSlaveTarget;
-ALOAD 0
-CONST null
-PUTFIELD local/lito381/server/ClientSession.pendingPetRename:Llocal/lito381/server/ClientSession$PendingPetRename;
-ALOAD 0
-INVOKESPECIAL local/lito381/server/ClientSession.clearControlledCompanionTargets()V
-ALOAD 0
 GETFIELD local/lito381/server/ClientSession.config:Llocal/lito381/server/ServerConfig;
 INVOKEVIRTUAL local/lito381/server/ServerConfig.nativeTransferStoneProbeEnabled()Z
-IFEQ L241
+IFEQ L230
 ALOAD 0
 GETFIELD local/lito381/server/ClientSession.transferStoneProbeHandoffPending:Z
-IFEQ L241
+IFEQ L230
 ALOAD 0
 CONST Integer:0
 PUTFIELD local/lito381/server/ClientSession.transferStoneProbeHandoffPending:Z
@@ -219,7 +208,7 @@
 ALOAD 0
 CONST str:"transfer-stone-probe-endpoint-handoff"
 INVOKEVIRTUAL local/lito381/server/ClientSession.forceDisconnect(Ljava/lang/String;)V
-GOTO L240
+GOTO L229
 ASTORE 7
 GETSTATIC java/lang/System.out:Ljava/io/PrintStream;
 ALOAD 0
@@ -233,7 +222,7 @@
 ALOAD 0
 CONST str:"transfer-stone-probe-endpoint-handoff"
 INVOKEVIRTUAL local/lito381/server/ClientSession.forceDisconnect(Ljava/lang/String;)V
-GOTO L240
+GOTO L229
 ASTORE 8
 ALOAD 0
 CONST str:"transfer-stone-probe-endpoint-handoff"
@@ -244,7 +233,7 @@
 ALOAD 0
 GETFIELD local/lito381/server/ClientSession.pendingTransition:Llocal/lito381/server/ClientSession$PendingTransition;
 GETSTATIC local/lito381/server/ClientSession$PendingTransition.RESTART_AWAIT_QUIT_FIGHT:Llocal/lito381/server/ClientSession$PendingTransition;
-IF_ACMPNE L260
+IF_ACMPNE L249
 ALOAD 0
 NEW local/lito381/server/Protocol$Writer
 DUP
@@ -263,21 +252,21 @@
 ALOAD 0
 GETFIELD local/lito381/server/ClientSession.phase:Llocal/lito381/server/ClientSession$Phase;
 GETSTATIC local/lito381/server/ClientSession$Phase.LOBBY:Llocal/lito381/server/ClientSession$Phase;
-IF_ACMPNE L297
+IF_ACMPNE L286
 ALOAD 0
 GETFIELD local/lito381/server/ClientSession.config:Llocal/lito381/server/ServerConfig;
 INVOKEVIRTUAL local/lito381/server/ServerConfig.rentalTownDirectoryEnabled()Z
-IFEQ L297
+IFEQ L286
 ALOAD 0
 GETFIELD local/lito381/server/ClientSession.rentalTownDirectoryAdvertised:Z
-IFEQ L297
+IFEQ L286
 ALOAD 0
 GETFIELD local/lito381/server/ClientSession.selectedCharacter:Ljava/lang/String;
 INVOKEVIRTUAL java/lang/String.isEmpty()Z
-IFNE L297
+IFNE L286
 ALOAD 0
 GETFIELD local/lito381/server/ClientSession.rentalItemSetSelection:I
-IFLT L297
+IFLT L286
 ALOAD 0
 CONST Integer:1
 PUTFIELD local/lito381/server/ClientSession.rentalTownJoinPending:Z
@@ -296,18 +285,18 @@
 INVOKESTATIC local/lito381/server/Protocol.allowPostTransition()[B
 CONST str:"S_LITO_VS_SERVER_ALLOW_JOIN rental-town cached-room join"
 INVOKESPECIAL local/lito381/server/ClientSession.sendEncrypted([BLjava/lang/String;)V
-GOTO L345
+GOTO L334
 ALOAD 0
 GETFIELD local/lito381/server/ClientSession.phase:Llocal/lito381/server/ClientSession$Phase;
 GETSTATIC local/lito381/server/ClientSession$Phase.BATTLE:Llocal/lito381/server/ClientSession$Phase;
-IF_ACMPNE L304
+IF_ACMPNE L293
 ALOAD 0
 INVOKESPECIAL local/lito381/server/ClientSession.settleRequestedBattleExit()V
-GOTO L345
+RETURN
 ALOAD 0
 GETFIELD local/lito381/server/ClientSession.phase:Llocal/lito381/server/ClientSession$Phase;
 GETSTATIC local/lito381/server/ClientSession$Phase.RESULT:Llocal/lito381/server/ClientSession$Phase;
-IF_ACMPNE L320
+IF_ACMPNE L309
 ALOAD 0
 NEW local/lito381/server/Protocol$Writer
 DUP
@@ -319,11 +308,11 @@
 INVOKEVIRTUAL local/lito381/server/Protocol$Writer.bytes()[B
 CONST str:"S_LITO_QUIT_FIGHT accepted; waiting-summary-confirm"
 INVOKESPECIAL local/lito381/server/ClientSession.sendEncrypted([BLjava/lang/String;)V
-GOTO L345
+GOTO L334
 ALOAD 0
 GETFIELD local/lito381/server/ClientSession.phase:Llocal/lito381/server/ClientSession$Phase;
 GETSTATIC local/lito381/server/ClientSession$Phase.ROOM:Llocal/lito381/server/ClientSession$Phase;
-IF_ACMPNE L342
+IF_ACMPNE L331
 ALOAD 0
 NEW local/lito381/server/Protocol$Writer
 DUP
@@ -337,11 +326,11 @@
 INVOKESPECIAL local/lito381/server/ClientSession.sendEncrypted([BLjava/lang/String;)V
 ALOAD 0
 GETFIELD local/lito381/server/ClientSession.rentalTownRoomState:Z
-IFNE L345
+IFNE L330
 ALOAD 0
 CONST Integer:0
 INVOKESPECIAL local/lito381/server/ClientSession.exitRoom(Z)V
-GOTO L345
+GOTO L334
 GETSTATIC java/lang/System.out:Ljava/io/PrintStream;
 CONST str:"[LiTo] C_LITO_QUIT_FIGHT acknowledged outside battle"
 INVOKEVIRTUAL java/io/PrintStream.println(Ljava/lang/String;)V
```


</details>

### 純形狀待裁定　`local/lito381/server/ClientSession#nativeAreaFreezeAcceptedWorld(Llocal/lito381/server/BattleSpellCatalog$FreezeAttackSpell;Llocal/lito381/server/ClientSession;Ljava/util/List;)V`

<details>
<summary>完整差異</summary>



```diff
--- staged/local/lito381/server/ClientSession#nativeAreaFreezeAcceptedWorld(Llocal/lito381/server/BattleSpellCatalog$FreezeAttackSpell;Llocal/lito381/server/ClientSession;Ljava/util/List;)V
+++ candidate/local/lito381/server/ClientSession#nativeAreaFreezeAcceptedWorld(Llocal/lito381/server/BattleSpellCatalog$FreezeAttackSpell;Llocal/lito381/server/ClientSession;Ljava/util/List;)V
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


</details>

### 純形狀待裁定　`local/lito381/server/ClientSession#nativeAreaFreezeAcceptedBattle(Llocal/lito381/server/BattleSpellCatalog$FreezeAttackSpell;Llocal/lito381/server/ClientSession;Ljava/util/List;)V`

<details>
<summary>完整差異</summary>



```diff
--- staged/local/lito381/server/ClientSession#nativeAreaFreezeAcceptedBattle(Llocal/lito381/server/BattleSpellCatalog$FreezeAttackSpell;Llocal/lito381/server/ClientSession;Ljava/util/List;)V
+++ candidate/local/lito381/server/ClientSession#nativeAreaFreezeAcceptedBattle(Llocal/lito381/server/BattleSpellCatalog$FreezeAttackSpell;Llocal/lito381/server/ClientSession;Ljava/util/List;)V
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


</details>

### 純形狀待裁定　`local/lito381/server/ClientSession#resolveClassicWorldScarecrowHit(Llocal/lito381/server/ClassicWorldNpcCatalog$StaticNpc;Llocal/lito381/server/ClassicWorldMonsterCatalog$MonsterTemplate;I)V`

<details>
<summary>完整差異</summary>



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

### 缺呼叫　`local/lito381/server/ClientSession#resetAndSendRentalTownStatus()V`

<details>
<summary>完整差異</summary>



```diff
--- staged/local/lito381/server/ClientSession#resetAndSendRentalTownStatus()V
+++ candidate/local/lito381/server/ClientSession#resetAndSendRentalTownStatus()V
@@ -42,6 +42,8 @@
 ALOAD 0
 ALOAD 0
 GETFIELD local/lito381/server/ClientSession.playerMaxHp:I
+ALOAD 0
+INVOKESTATIC local/lito381/server/NativePlayerCommit.advance(Llocal/lito381/server/ClientSession;)V
 PUTFIELD local/lito381/server/ClientSession.playerHp:I
 ALOAD 0
 ALOAD 0
```


</details>

### 缺呼叫　`local/lito381/server/ClientSession#confirmDeath([B)V`

<details>
<summary>完整差異</summary>



```diff
--- staged/local/lito381/server/ClientSession#confirmDeath([B)V
+++ candidate/local/lito381/server/ClientSession#confirmDeath([B)V
@@ -23,7 +23,7 @@
 ALOAD 0
 GETFIELD local/lito381/server/ClientSession.lastAttacker:Llocal/lito381/server/ClientSession;
 ALOAD 0
-INVOKEVIRTUAL local/lito381/server/GameHub.playerDefeated(JLlocal/lito381/server/ClientSession;Llocal/lito381/server/ClientSession;)Llocal/lito381/server/BattleRules$Resolution;
+INVOKESTATIC local/lito381/server/CombatPoisonLifecycle.defeated(Llocal/lito381/server/GameHub;JLlocal/lito381/server/ClientSession;Llocal/lito381/server/ClientSession;)Llocal/lito381/server/BattleRules$Resolution;
 ASTORE 2
 GETSTATIC java/lang/System.out:Ljava/io/PrintStream;
 ALOAD 0
```


</details>

### 缺呼叫　`local/lito381/server/ClientSession#commitClassicWorldMonsterCounterBarrier(Llocal/lito381/server/ClassicWorldMonsterPopulation$Snapshot;Llocal/lito381/server/BattleCounterBarrier$Decision;)V`

<details>
<summary>完整差異</summary>



```diff
--- staged/local/lito381/server/ClientSession#commitClassicWorldMonsterCounterBarrier(Llocal/lito381/server/ClassicWorldMonsterPopulation$Snapshot;Llocal/lito381/server/BattleCounterBarrier$Decision;)V
+++ candidate/local/lito381/server/ClientSession#commitClassicWorldMonsterCounterBarrier(Llocal/lito381/server/ClassicWorldMonsterPopulation$Snapshot;Llocal/lito381/server/BattleCounterBarrier$Decision;)V
@@ -141,7 +141,7 @@
 INVOKEVIRTUAL local/lito381/server/ClassicWorldMonsterPopulation$Snapshot.currentHp()I
 ALOAD 13
 INVOKEVIRTUAL local/lito381/server/ClassicWorldMonsterPopulation$Snapshot.maximumHp()I
-INVOKESTATIC local/lito381/server/Protocol.hitRatio(JII)[B
+INVOKESTATIC local/lito381/server/NativeBattleTamedLifecycle.visualHitRatio(JII)[B
 ASTORE 14
 ALOAD 0
 ALOAD 0
```


</details>

### 順序不同　`local/lito381/server/ClientSession#sourcePoisonBody$publishNpcMagicCancellation(JLjava/util/List;ZZZZZZZZZZZZZZZZZZZZZLlocal/lito381/server/CombatPoisonLifecycle$Scope;)V`

<details>
<summary>完整差異</summary>



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

### 順序不同　`local/lito381/server/ClientSession#publishNpcCounterMirrorTrigger(JIILlocal/lito381/server/BattleCounterMirrorStatus$Decision;Ljava/util/List;Ljava/lang/String;)V`

<details>
<summary>完整差異</summary>



```diff
--- staged/local/lito381/server/ClientSession#publishNpcCounterMirrorTrigger(JIILlocal/lito381/server/BattleCounterMirrorStatus$Decision;Ljava/util/List;Ljava/lang/String;)V
+++ candidate/local/lito381/server/ClientSession#publishNpcCounterMirrorTrigger(JIILlocal/lito381/server/BattleCounterMirrorStatus$Decision;Ljava/util/List;Ljava/lang/String;)V
@@ -22,7 +22,7 @@
 ASTORE 11
 ALOAD 11
 INVOKEINTERFACE java/util/Iterator.hasNext()Z itf
-IFEQ L74
+IFEQ L77
 ALOAD 11
 INVOKEINTERFACE java/util/Iterator.next()Ljava/lang/Object; itf
 CHECKCAST local/lito381/server/ClientSession
@@ -60,11 +60,6 @@
 ALOAD 7
 INVOKEDYNAMIC makeConcatWithConstants(JJILjava/lang/String;)Ljava/lang/String; bsm=H6 java/lang/invoke/StringConcatFactory.makeConcatWithConstants(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;Ljava/lang/String;[Ljava/lang/Object;)Ljava/lang/invoke/CallSite; args=[str:"S_EFFECT Counter-Mirror-trigger defender=\u0001 npc-attacker=\u0001 gfx=\u0001 source=\u0001"]
 INVOKESPECIAL local/lito381/server/ClientSession.sendEncrypted([BLjava/lang/String;)V
-ALOAD 0
-LLOAD 1/2
-INVOKESTATIC local/lito381/server/NativeSourceNpcReaction.allow(Llocal/lito381/server/ClientSession;J)Z
-IFNE L65
-GOTO L20
 ALOAD 12
 ALOAD 10
 LLOAD 1/2
@@ -72,6 +67,14 @@
 INVOKEVIRTUAL local/lito381/server/ClientSession.playerObjectId()J
 ALOAD 7
 INVOKEDYNAMIC makeConcatWithConstants(JJLjava/lang/String;)Ljava/lang/String; bsm=H6 java/lang/invoke/StringConcatFactory.makeConcatWithConstants(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;Ljava/lang/String;[Ljava/lang/Object;)Ljava/lang/invoke/CallSite; args=[str:"S_ACTION Counter-Mirror-reflected npc-attacker=\u0001 defender=\u0001 action=2 source=\u0001"]
+ALOAD 0
+LLOAD 1/2
+INVOKESTATIC local/lito381/server/NativeSourceNpcReaction.allow(Llocal/lito381/server/ClientSession;J)Z
+IFNE L75
+POP
+POP
+POP
+GOTO L76
 INVOKESPECIAL local/lito381/server/ClientSession.sendEncrypted([BLjava/lang/String;)V
 GOTO L20
 GETSTATIC java/lang/System.out:Ljava/io/PrintStream;
```


</details>

### 順序不同　`local/lito381/server/ClientSession#publishNpcCounterBarrierTrigger(JLlocal/lito381/server/BattleCounterBarrier$Decision;Ljava/util/List;Ljava/lang/String;)V`

<details>
<summary>完整差異</summary>



```diff
--- staged/local/lito381/server/ClientSession#publishNpcCounterBarrierTrigger(JLlocal/lito381/server/BattleCounterBarrier$Decision;Ljava/util/List;Ljava/lang/String;)V
+++ candidate/local/lito381/server/ClientSession#publishNpcCounterBarrierTrigger(JLlocal/lito381/server/BattleCounterBarrier$Decision;Ljava/util/List;Ljava/lang/String;)V
@@ -18,7 +18,7 @@
 ASTORE 9
 ALOAD 9
 INVOKEINTERFACE java/util/Iterator.hasNext()Z itf
-IFEQ L51
+IFEQ L54
 ALOAD 9
 INVOKEINTERFACE java/util/Iterator.next()Ljava/lang/Object; itf
 CHECKCAST local/lito381/server/ClientSession
@@ -37,11 +37,6 @@
 ALOAD 5
 INVOKEDYNAMIC makeConcatWithConstants(JJIIILjava/lang/String;)Ljava/lang/String; bsm=H6 java/lang/invoke/StringConcatFactory.makeConcatWithConstants(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;Ljava/lang/String;[Ljava/lang/Object;)Ljava/lang/invoke/CallSite; args=[str:"S_EFFECT counter-barrier-trigger defender=\u0001 npc-attacker=\u0001 gfx=\u0001 probability=\u0001 roll=\u0001 source=\u0001"]
 INVOKESPECIAL local/lito381/server/ClientSession.sendEncrypted([BLjava/lang/String;)V
-ALOAD 0
-LLOAD 1/2
-INVOKESTATIC local/lito381/server/NativeSourceNpcReaction.allow(Llocal/lito381/server/ClientSession;J)Z
-IFNE L42
-GOTO L16
 ALOAD 10
 ALOAD 8
 LLOAD 1/2
@@ -49,6 +44,14 @@
 INVOKEVIRTUAL local/lito381/server/ClientSession.playerObjectId()J
 ALOAD 5
 INVOKEDYNAMIC makeConcatWithConstants(JJLjava/lang/String;)Ljava/lang/String; bsm=H6 java/lang/invoke/StringConcatFactory.makeConcatWithConstants(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;Ljava/lang/String;[Ljava/lang/Object;)Ljava/lang/invoke/CallSite; args=[str:"S_ACTION counter-barrier-reflected npc-attacker=\u0001 defender=\u0001 action=2 source=\u0001"]
+ALOAD 0
+LLOAD 1/2
+INVOKESTATIC local/lito381/server/NativeSourceNpcReaction.allow(Llocal/lito381/server/ClientSession;J)Z
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

### 缺呼叫　`local/lito381/server/ClientSession#sourceApplicationRun()V`

<details>
<summary>完整差異</summary>



```diff
--- staged/local/lito381/server/ClientSession#sourceApplicationRun()V
+++ candidate/local/lito381/server/ClientSession#sourceApplicationRun()V
@@ -1,5 +1,5 @@
 META 2 null []
-TRY [1:[8:java/lang/Exception  ], 3:[8:java/lang/Exception  ], 4:[8:java/lang/Exception  ], 28:[433:java/lang/Exception  , 755:java/lang/Throwable  ], 31:[106:java/lang/Throwable  , 433:java/lang/Exception  , 755:java/lang/Throwable  ], 33:[106:java/lang/Throwable  , 433:java/lang/Exception  , 755:java/lang/Throwable  ], 35:[106:java/lang/Throwable  , 433:java/lang/Exception  , 755:java/lang/Throwable  ], 37:[106:java/lang/Throwable  , 433:java/lang/Exception  , 755:java/lang/Throwable  ], 38:[106:java/lang/Throwable  , 433:java/lang/Exception  , 755:java/lang/Throwable  ], 40:[106:java/lang/Throwable  , 433:java/lang/Exception  , 755:java/lang/Throwable  ], 41:[106:java/lang/Throwable  , 433:java/lang/Exception  , 755:java/lang/Throwable  ], 43:[106:java/lang/Throwable  , 433:java/lang/Exception  , 755:java/lang/Throwable  ], 45:[106:java/lang/Throwable  , 433:java/lang/Exception  , 755:java/lang/Throwable  ], 47:[106:java/lang/Throwable  , 433:java/lang/Exception  , 755:java/lang/Throwable  ], 49:[106:java/lang/Throwable  , 433:java/lang/Exception  , 755:java/lang/Throwable  ], 50:[106:java/lang/Throwable  , 433:java/lang/Exception  , 755:java/lang/Throwable  ], 54:[106:java/lang/Throwable  , 433:java/lang/Exception  , 755:java/lang/Throwable  ], 56:[106:java/lang/Throwable  , 433:java/lang/Exception  , 755:java/lang/Throwable  ], 58:[106:java/lang/Throwable  , 433:java/lang/Exception  , 755:java/lang/Throwable  ], 61:[106:java/lang/Throwable  , 433:java/lang/Exception  , 755:java/lang/Throwable  ], 62:[106:java/lang/Throwable  , 433:java/lang/Exception  , 755:java/lang/Throwable  ], 64:[106:java/lang/Throwable  , 433:java/lang/Exception  , 755:java/lang/Throwable  ], 67:[106:java/lang/Throwable  , 433:java/lang/Exception  , 755:java/lang/Throwable  ], 68:[106:java/lang/Throwable  , 433:java/lang/Exception  , 755:java/lang/Throwable  ], 70:[106:java/lang/Throwable  , 433:java/lang/Exception  , 755:java/lang/Throwable  ], 71:[106:java/lang/Throwable  , 433:java/lang/Exception  , 755:java/lang/Throwable  ], 73:[106:java/lang/Throwable  , 433:java/lang/Exception  , 755:java/lang/Throwable  ], 74:[106:java/lang/Throwable  , 433:java/lang/Exception  , 755:java/lang/Throwable  ], 76:[106:java/lang/Throwable  , 433:java/lang/Exception  , 755:java/lang/Throwable  ], 82:[106:java/lang/Throwable  , 433:java/lang/Exception  , 755:java/lang/Throwable  ], 84:[106:java/lang/Throwable  , 433:java/lang/Exception  , 755:java/lang/Throwable  ], 87:[106:java/lang/Throwable  , 433:java/lang/Exception  , 755:java/lang/Throwable  ], 91:[106:java/lang/Throwable  , 433:java/lang/Exception  , 755:java/lang/Throwable  ], 95:[106:java/lang/Throwable  , 433:java/lang/Exception  , 755:java/lang/Throwable  ], 96:[106:java/lang/Throwable  , 433:java/lang/Exception  , 755:java/lang/Throwable  ], 99:[106:java/lang/Throwable  , 433:java/lang/Exception  , 755:java/lang/Throwable  ], 101:[106:java/lang/Throwable  , 433:java/lang/Exception  , 755:java/lang/Throwable  ], 111:[113:java/lang/Throwable  , 433:java/lang/Exception  , 755:java/lang/Throwable  ], 116:[433:java/lang/Exception  , 755:java/lang/Throwable  ], 118:[433:java/lang/Exception  , 755:java/lang/Throwable  ], 123:[433:java/lang/Exception  , 755:java/lang/Throwable  ], 150:[152:java/io/IOException  ], 164:[166:java/io/IOException  ], 177:[179:java/io/IOException  , 179:java/lang/RuntimeException  ], 186:[188:java/io/IOException  , 188:java/lang/RuntimeException  ], 195:[197:java/io/IOException  , 197:java/lang/RuntimeException  ], 204:[206:java/io/IOException  , 206:java/lang/RuntimeException  ], 213:[215:java/io/IOException  , 215:java/lang/RuntimeException  ], 222:[224:java/io/IOException  , 224:java/lang/RuntimeException  ], 231:[233:java/io/IOException  , 233:java/lang/RuntimeException  ], 240:[242:java/io/IOException  , 242:java/lang/RuntimeException  ], 249:[251:java/io/IOException  , 251:java/lang/RuntimeException  ], 258:[260:java/io/IOException  , 260:java/lang/RuntimeException  ], 267:[269:java/io/IOException  , 269:java/lang/RuntimeException  ], 276:[278:java/io/IOException  , 278:java/lang/RuntimeException  ], 285:[287:java/io/IOException  , 287:java/lang/RuntimeException  ], 299:[307:java/io/IOException  , 307:java/lang/RuntimeException  ], 302:[307:java/io/IOException  , 307:java/lang/RuntimeException  ], 305:[307:java/io/IOException  , 307:java/lang/RuntimeException  ], 314:[331:java/io/IOException  , 331:java/lang/RuntimeException  ], 317:[331:java/io/IOException  , 331:java/lang/RuntimeException  ], 321:[325:java/lang/Throwable  , 331:java/io/IOException  , 331:java/lang/RuntimeException  ], 323:[325:java/lang/Throwable  , 331:java/io/IOException  , 331:java/lang/RuntimeException  ], 327:[325:java/lang/Throwable  , 331:java/io/IOException  , 331:java/lang/RuntimeException  ], 329:[331:java/io/IOException  , 331:java/lang/RuntimeException  ], 338:[340:java/lang/Exception  ], 350:[352:java/io/IOException  ], 400:[402:java/io/IOException  ], 422:[424:java/io/IOException  ], 434:[755:java/lang/Throwable  ], 436:[755:java/lang/Throwable  ], 437:[755:java/lang/Throwable  ], 440:[755:java/lang/Throwable  ], 441:[755:java/lang/Throwable  ], 443:[755:java/lang/Throwable  ], 444:[755:java/lang/Throwable  ], 445:[755:java/lang/Throwable  ], 472:[474:java/io/IOException  ], 486:[488:java/io/IOException  ], 499:[501:java/io/IOException  , 501:java/lang/RuntimeException  ], 508:[510:java/io/IOException  , 510:java/lang/RuntimeException  ], 517:[519:java/io/IOException  , 519:java/lang/RuntimeException  ], 526:[528:java/io/IOException  , 528:java/lang/RuntimeException  ], 535:[537:java/io/IOException  , 537:java/lang/RuntimeException  ], 544:[546:java/io/IOException  , 546:java/lang/RuntimeException  ], 553:[555:java/io/IOException  , 555:java/lang/RuntimeException  ], 562:[564:java/io/IOException  , 564:java/lang/RuntimeException  ], 571:[573:java/io/IOException  , 573:java/lang/RuntimeException  ], 580:[582:java/io/IOException  , 582:java/lang/RuntimeException  ], 589:[591:java/io/IOException  , 591:java/lang/RuntimeException  ], 598:[600:java/io/IOException  , 600:java/lang/RuntimeException  ], 607:[609:java/io/IOException  , 609:java/lang/RuntimeException  ], 621:[629:java/io/IOException  , 629:java/lang/RuntimeException  ], 624:[629:java/io/IOException  , 629:java/lang/RuntimeException  ], 627:[629:java/io/IOException  , 629:java/lang/RuntimeException  ], 636:[653:java/io/IOException  , 653:java/lang/RuntimeException  ], 639:[653:java/io/IOException  , 653:java/lang/RuntimeException  ], 643:[647:java/lang/Throwable  , 653:java/io/IOException  , 653:java/lang/RuntimeException  ], 645:[647:java/lang/Throwable  , 653:java/io/IOException  , 653:java/lang/RuntimeException  ], 649:[647:java/lang/Throwable  , 653:java/io/IOException  , 653:java/lang/RuntimeException  ], 651:[653:java/io/IOException  , 653:java/lang/RuntimeException  ], 660:[662:java/lang/Exception  ], 672:[674:java/io/IOException  ], 722:[724:java/io/IOException  ], 744:[746:java/io/IOException  ], 782:[784:java/io/IOException  ], 796:[798:java/io/IOException  ], 809:[811:java/io/IOException  , 811:java/lang/RuntimeException  ], 818:[820:java/io/IOException  , 820:java/lang/RuntimeException  ], 827:[829:java/io/IOException  , 829:java/lang/RuntimeException  ], 836:[838:java/io/IOException  , 838:java/lang/RuntimeException  ], 845:[847:java/io/IOException  , 847:java/lang/RuntimeException  ], 854:[856:java/io/IOException  , 856:java/lang/RuntimeException  ], 863:[865:java/io/IOException  , 865:java/lang/RuntimeException  ], 872:[874:java/io/IOException  , 874:java/lang/RuntimeException  ], 881:[883:java/io/IOException  , 883:java/lang/RuntimeException  ], 890:[892:java/io/IOException  , 892:java/lang/RuntimeException  ], 899:[901:java/io/IOException  , 901:java/lang/RuntimeException  ], 908:[910:java/io/IOException  , 910:java/lang/RuntimeException  ], 917:[919:java/io/IOException  , 919:java/lang/RuntimeException  ], 931:[939:java/io/IOException  , 939:java/lang/RuntimeException  ], 934:[939:java/io/IOException  , 939:java/lang/RuntimeException  ], 937:[939:java/io/IOException  , 939:java/lang/RuntimeException  ], 946:[963:java/io/IOException  , 963:java/lang/RuntimeException  ], 949:[963:java/io/IOException  , 963:java/lang/RuntimeException  ], 953:[957:java/lang/Throwable  , 963:java/io/IOException  , 963:java/lang/RuntimeException  ], 955:[957:java/lang/Throwable  , 963:java/io/IOException  , 963:java/lang/RuntimeException  ], 959:[957:java/lang/Throwable  , 963:java/io/IOException  , 963:java/lang/RuntimeException  ], 961:[963:java/io/IOException  , 963:java/lang/RuntimeException  ], 970:[972:java/lang/Exception  ], 982:[984:java/io/IOException  ], 1032:[1034:java/io/IOException  ], 1054:[1056:java/io/IOException  ]]
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
-GOTO L105
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
@@ -392,57 +685,71 @@
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
-GOTO L1066
-ASTORE 40
+GOTO L705
+RETURN
+GOTO L65
+ASTORE 67
+ALOAD 2
+IFNULL L738
+ALOAD 2
+ALOAD 0
+INVOKESTATIC local/lito381/server/NativeServerLifetime.closeSessionSocket(Ljava/net/Socket;Llocal/lito381/server/ClientSession;)V
+GOTO L738
+ASTORE 68
+ALOAD 67
+ALOAD 68
+INVOKEVIRTUAL java/lang/Throwable.addSuppressed(Ljava/lang/Throwable;)V
+ALOAD 67
+ATHROW
+ASTORE 69
 GETSTATIC java/lang/System.out:Ljava/io/PrintStream;
 ALOAD 0
 GETFIELD local/lito381/server/ClientSession.endpoint:Llocal/lito381/server/ClientSession$Endpoint;
 INVOKESTATIC java/lang/String.valueOf(Ljava/lang/Object;)Ljava/lang/String;
-ALOAD 2
-ALOAD 40
+ALOAD 1
+ALOAD 69
 INVOKEVIRTUAL java/lang/Object.getClass()Ljava/lang/Class;
 INVOKEVIRTUAL java/lang/Class.getSimpleName()Ljava/lang/String;
-ALOAD 40
+ALOAD 69
 INVOKEVIRTUAL java/lang/Exception.getMessage()Ljava/lang/String;
 INVOKEDYNAMIC makeConcatWithConstants(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;)Ljava/lang/String; bsm=H6 java/lang/invoke/StringConcatFactory.makeConcatWithConstants(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;Ljava/lang/String;[Ljava/lang/Object;)Ljava/lang/invoke/CallSite; args=[str:"[LiTo] session ended endpoint=\u0001 peer=\u0001: \u0001: \u0001"]
 INVOKEVIRTUAL java/io/PrintStream.println(Ljava/lang/String;)V
@@ -450,175 +757,175 @@
 INVOKESTATIC local/lito381/server/NativeServerLifetime.awaitCleanup(Llocal/lito381/server/ClientSession;)V
 ALOAD 0
 GETFIELD local/lito381/server/ClientSession.currentRoomId:J
-LSTORE 41/42
+LSTORE 70/71
 ALOAD 0
 GETFIELD local/lito381/server/ClientSession.phase:Llocal/lito381/server/ClientSession$Phase;
 GETSTATIC local/lito381/server/ClientSession$Phase.ROOM:Llocal/lito381/server/ClientSession$Phase;
-IF_ACMPNE L467
-LLOAD 41/42
+IF_ACMPNE L774
+LLOAD 70/71
 CONST Long:0
 LCMP
-IFEQ L467
+IFEQ L774
 ALOAD 0
 GETFIELD local/lito381/server/ClientSession.hub:Llocal/lito381/server/GameHub;
-LLOAD 41/42
+LLOAD 70/71
 ALOAD 0
 INVOKEVIRTUAL local/lito381/server/GameHub.isOwner(JLlocal/lito381/server/ClientSession;)Z
-IFEQ L467
+IFEQ L774
 CONST Integer:1
-GOTO L468
-CONST Integer:0
-ISTORE 43
+GOTO L775
+CONST Integer:0
+ISTORE 72
 ALOAD 0
 CONST str:"disconnect"
 CONST Integer:0
 INVOKESPECIAL local/lito381/server/ClientSession.cancelActivePlayerTrade(Ljava/lang/String;Z)V
-GOTO L480
-ASTORE 44
-GETSTATIC java/lang/System.out:Ljava/io/PrintStream;
-ALOAD 44
+GOTO L787
+ASTORE 73
+GETSTATIC java/lang/System.out:Ljava/io/PrintStream;
+ALOAD 73
 INVOKEVIRTUAL java/io/IOException.getMessage()Ljava/lang/String;
 INVOKEDYNAMIC makeConcatWithConstants(Ljava/lang/String;)Ljava/lang/String; bsm=H6 java/lang/invoke/StringConcatFactory.makeConcatWithConstants(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;Ljava/lang/String;[Ljava/lang/Object;)Ljava/lang/invoke/CallSite; args=[str:"[LiTo] trade disconnect cleanup failed: \u0001"]
 INVOKEVIRTUAL java/io/PrintStream.println(Ljava/lang/String;)V
 ALOAD 0
 GETFIELD local/lito381/server/ClientSession.classicPartyParticipant:Z
-IFEQ L494
+IFEQ L801
 ALOAD 0
 CONST str:"disconnect"
 CONST Integer:0
 INVOKESPECIAL local/lito381/server/ClientSession.leaveClassicParty(Ljava/lang/String;Z)V
-GOTO L494
-ASTORE 45
-GETSTATIC java/lang/System.out:Ljava/io/PrintStream;
-ALOAD 45
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
-IF_ACMPNE L516
+IF_ACMPNE L823
 ALOAD 0
 INVOKESPECIAL local/lito381/server/ClientSession.collectClassicWorldPetsOnDisconnect()V
-GOTO L507
-ASTORE 46
-GETSTATIC java/lang/System.out:Ljava/io/PrintStream;
-ALOAD 46
+GOTO L814
+ASTORE 75
+GETSTATIC java/lang/System.out:Ljava/io/PrintStream;
+ALOAD 75
 INVOKEVIRTUAL java/lang/Exception.getMessage()Ljava/lang/String;
 INVOKEDYNAMIC makeConcatWithConstants(Ljava/lang/String;)Ljava/lang/String; bsm=H6 java/lang/invoke/StringConcatFactory.makeConcatWithConstants(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;Ljava/lang/String;[Ljava/lang/Object;)Ljava/lang/invoke/CallSite; args=[str:"[LiTo] persistent pet disconnect cleanup failed: \u0001"]
 INVOKEVIRTUAL java/io/PrintStream.println(Ljava/lang/String;)V
 ALOAD 0
 INVOKESPECIAL local/lito381/server/ClientSession.cleanupClassicWorldTransientCompanionsOnDisconnect()V
-GOTO L516
-ASTORE 47
-GETSTATIC java/lang/System.out:Ljava/io/PrintStream;
-ALOAD 47
+GOTO L823
+ASTORE 76
+GETSTATIC java/lang/System.out:Ljava/io/PrintStream;
+ALOAD 76
 INVOKEVIRTUAL java/lang/Exception.getMessage()Ljava/lang/String;
 INVOKEDYNAMIC makeConcatWithConstants(Ljava/lang/String;)Ljava/lang/String; bsm=H6 java/lang/invoke/StringConcatFactory.makeConcatWithConstants(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;Ljava/lang/String;[Ljava/lang/Object;)Ljava/lang/invoke/CallSite; args=[str:"[LiTo] transient companion disconnect cleanup failed: \u0001"]
 INVOKEVIRTUAL java/io/PrintStream.println(Ljava/lang/String;)V
 ALOAD 0
 INVOKESPECIAL local/lito381/server/ClientSession.persistExperienceGemOnDisconnect()V
-GOTO L525
-ASTORE 48
-GETSTATIC java/lang/System.out:Ljava/io/PrintStream;
-ALOAD 48
+GOTO L832
+ASTORE 77
+GETSTATIC java/lang/System.out:Ljava/io/PrintStream;
+ALOAD 77
 INVOKEVIRTUAL java/lang/Exception.getMessage()Ljava/lang/String;
 INVOKEDYNAMIC makeConcatWithConstants(Ljava/lang/String;)Ljava/lang/String; bsm=H6 java/lang/invoke/StringConcatFactory.makeConcatWithConstants(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;Ljava/lang/String;[Ljava/lang/Object;)Ljava/lang/invoke/CallSite; args=[str:"[LiTo] Dragon Gem disconnect persistence failed: \u0001"]
 INVOKEVIRTUAL java/io/PrintStream.println(Ljava/lang/String;)V
 ALOAD 0
 INVOKESPECIAL local/lito381/server/ClientSession.persistFireWeaponOnDisconnect()V
-GOTO L534
-ASTORE 49
-GETSTATIC java/lang/System.out:Ljava/io/PrintStream;
-ALOAD 49
+GOTO L841
+ASTORE 78
+GETSTATIC java/lang/System.out:Ljava/io/PrintStream;
+ALOAD 78
 INVOKEVIRTUAL java/lang/Exception.getMessage()Ljava/lang/String;
 INVOKEDYNAMIC makeConcatWithConstants(Ljava/lang/String;)Ljava/lang/String; bsm=H6 java/lang/invoke/StringConcatFactory.makeConcatWithConstants(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;Ljava/lang/String;[Ljava/lang/Object;)Ljava/lang/invoke/CallSite; args=[str:"[LiTo] Fire Weapon disconnect persistence failed: \u0001"]
 INVOKEVIRTUAL java/io/PrintStream.println(Ljava/lang/String;)V
 ALOAD 0
 INVOKESPECIAL local/lito381/server/ClientSession.persistBurningWeaponOnDisconnect()V
-GOTO L543
-ASTORE 50
-GETSTATIC java/lang/System.out:Ljava/io/PrintStream;
-ALOAD 50
+GOTO L850
+ASTORE 79
+GETSTATIC java/lang/System.out:Ljava/io/PrintStream;
+ALOAD 79
 INVOKEVIRTUAL java/lang/Exception.getMessage()Ljava/lang/String;
 INVOKEDYNAMIC makeConcatWithConstants(Ljava/lang/String;)Ljava/lang/String; bsm=H6 java/lang/invoke/StringConcatFactory.makeConcatWithConstants(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;Ljava/lang/String;[Ljava/lang/Object;)Ljava/lang/invoke/CallSite; args=[str:"[LiTo] Burning Weapon disconnect persistence failed: \u0001"]
 INVOKEVIRTUAL java/io/PrintStream.println(Ljava/lang/String;)V
 ALOAD 0
 INVOKESPECIAL local/lito381/server/ClientSession.persistWindShotOnDisconnect()V
-GOTO L552
-ASTORE 51
-GETSTATIC java/lang/System.out:Ljava/io/PrintStream;
-ALOAD 51
+GOTO L859
+ASTORE 80
+GETSTATIC java/lang/System.out:Ljava/io/PrintStream;
+ALOAD 80
 INVOKEVIRTUAL java/lang/Exception.getMessage()Ljava/lang/String;
 INVOKEDYNAMIC makeConcatWithConstants(Ljava/lang/String;)Ljava/lang/String; bsm=H6 java/lang/invoke/StringConcatFactory.makeConcatWithConstants(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;Ljava/lang/String;[Ljava/lang/Object;)Ljava/lang/invoke/CallSite; args=[str:"[LiTo] Wind Shot disconnect persistence failed: \u0001"]
 INVOKEVIRTUAL java/io/PrintStream.println(Ljava/lang/String;)V
 ALOAD 0
 INVOKESPECIAL local/lito381/server/ClientSession.persistFireBlessOnDisconnect()V
-GOTO L561
-ASTORE 52
-GETSTATIC java/lang/System.out:Ljava/io/PrintStream;
-ALOAD 52
+GOTO L868
+ASTORE 81
+GETSTATIC java/lang/System.out:Ljava/io/PrintStream;
+ALOAD 81
 INVOKEVIRTUAL java/lang/Exception.getMessage()Ljava/lang/String;
 INVOKEDYNAMIC makeConcatWithConstants(Ljava/lang/String;)Ljava/lang/String; bsm=H6 java/lang/invoke/StringConcatFactory.makeConcatWithConstants(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;Ljava/lang/String;[Ljava/lang/Object;)Ljava/lang/invoke/CallSite; args=[str:"[LiTo] Fire Bless disconnect persistence failed: \u0001"]
 INVOKEVIRTUAL java/io/PrintStream.println(Ljava/lang/String;)V
 ALOAD 0
 INVOKESPECIAL local/lito381/server/ClientSession.persistStormEyeOnDisconnect()V
-GOTO L570
-ASTORE 53
-GETSTATIC java/lang/System.out:Ljava/io/PrintStream;
-ALOAD 53
+GOTO L877
+ASTORE 82
+GETSTATIC java/lang/System.out:Ljava/io/PrintStream;
+ALOAD 82
 INVOKEVIRTUAL java/lang/Exception.getMessage()Ljava/lang/String;
 INVOKEDYNAMIC makeConcatWithConstants(Ljava/lang/String;)Ljava/lang/String; bsm=H6 java/lang/invoke/StringConcatFactory.makeConcatWithConstants(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;Ljava/lang/String;[Ljava/lang/Object;)Ljava/lang/invoke/CallSite; args=[str:"[LiTo] Storm Eye disconnect persistence failed: \u0001"]
 INVOKEVIRTUAL java/io/PrintStream.println(Ljava/lang/String;)V
 ALOAD 0
 INVOKESPECIAL local/lito381/server/ClientSession.persistStormShotOnDisconnect()V
-GOTO L579
-ASTORE 54
-GETSTATIC java/lang/System.out:Ljava/io/PrintStream;
-ALOAD 54
+GOTO L886
+ASTORE 83
+GETSTATIC java/lang/System.out:Ljava/io/PrintStream;
+ALOAD 83
 INVOKEVIRTUAL java/lang/Exception.getMessage()Ljava/lang/String;
 INVOKEDYNAMIC makeConcatWithConstants(Ljava/lang/String;)Ljava/lang/String; bsm=H6 java/lang/invoke/StringConcatFactory.makeConcatWithConstants(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;Ljava/lang/String;[Ljava/lang/Object;)Ljava/lang/invoke/CallSite; args=[str:"[LiTo] Storm Shot disconnect persistence failed: \u0001"]
 INVOKEVIRTUAL java/io/PrintStream.println(Ljava/lang/String;)V
 ALOAD 0
 INVOKESPECIAL local/lito381/server/ClientSession.persistEarthBlessOnDisconnect()V
-GOTO L588
-ASTORE 55
-GETSTATIC java/lang/System.out:Ljava/io/PrintStream;
-ALOAD 55
+GOTO L895
+ASTORE 84
+GETSTATIC java/lang/System.out:Ljava/io/PrintStream;
+ALOAD 84
 INVOKEVIRTUAL java/lang/Exception.getMessage()Ljava/lang/String;
 INVOKEDYNAMIC makeConcatWithConstants(Ljava/lang/String;)Ljava/lang/String; bsm=H6 java/lang/invoke/StringConcatFactory.makeConcatWithConstants(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;Ljava/lang/String;[Ljava/lang/Object;)Ljava/lang/invoke/CallSite; args=[str:"[LiTo] Earth Bless disconnect persistence failed: \u0001"]
 INVOKEVIRTUAL java/io/PrintStream.println(Ljava/lang/String;)V
 ALOAD 0
 INVOKESPECIAL local/lito381/server/ClientSession.persistIronSkinOnDisconnect()V
-GOTO L597
-ASTORE 56
-GETSTATIC java/lang/System.out:Ljava/io/PrintStream;
-ALOAD 56
+GOTO L904
+ASTORE 85
+GETSTATIC java/lang/System.out:Ljava/io/PrintStream;
+ALOAD 85
 INVOKEVIRTUAL java/lang/Exception.getMessage()Ljava/lang/String;
 INVOKEDYNAMIC makeConcatWithConstants(Ljava/lang/String;)Ljava/lang/String; bsm=H6 java/lang/invoke/StringConcatFactory.makeConcatWithConstants(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;Ljava/lang/String;[Ljava/lang/Object;)Ljava/lang/invoke/CallSite; args=[str:"[LiTo] Iron Skin disconnect persistence failed: \u0001"]
 INVOKEVIRTUAL java/io/PrintStream.println(Ljava/lang/String;)V
 ALOAD 0
 INVOKESPECIAL local/lito381/server/ClientSession.persistMovementBuffOnDisconnect()V
-GOTO L606
-ASTORE 57
-GETSTATIC java/lang/System.out:Ljava/io/PrintStream;
-ALOAD 57
+GOTO L913
+ASTORE 86
+GETSTATIC java/lang/System.out:Ljava/io/PrintStream;
+ALOAD 86
 INVOKEVIRTUAL java/lang/Exception.getMessage()Ljava/lang/String;
 INVOKEDYNAMIC makeConcatWithConstants(Ljava/lang/String;)Ljava/lang/String; bsm=H6 java/lang/invoke/StringConcatFactory.makeConcatWithConstants(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;Ljava/lang/String;[Ljava/lang/Object;)Ljava/lang/invoke/CallSite; args=[str:"[LiTo] movement buff disconnect persistence failed: \u0001"]
 INVOKEVIRTUAL java/io/PrintStream.println(Ljava/lang/String;)V
 ALOAD 0
 INVOKESPECIAL local/lito381/server/ClientSession.persistArmorProtectionOnDisconnect()V
-GOTO L615
-ASTORE 58
-GETSTATIC java/lang/System.out:Ljava/io/PrintStream;
-ALOAD 58
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
-IF_ACMPNE L635
+IF_ACMPNE L942
 ALOAD 0
 CONST str:"owner-disconnect"
 INVOKESPECIAL local/lito381/server/ClientSession.abandonClassicWorldRescueFollower(Ljava/lang/String;)V
@@ -628,55 +935,55 @@
 ALOAD 0
 CONST str:"owner-disconnect"
 INVOKESPECIAL local/lito381/server/ClientSession.abandonClassicWorldRoiFollower(Ljava/lang/String;)V
-GOTO L635
-ASTORE 59
-GETSTATIC java/lang/System.out:Ljava/io/PrintStream;
-ALOAD 59
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
-ASTORE 60
+ASTORE 89
 MONITORENTER
 ALOAD 0
 CONST str:"owner-disconnect"
 CONST Integer:0
 INVOKESPECIAL local/lito381/server/ClientSession.recallNativeMagicDollLocked(Ljava/lang/String;Z)V
-ALOAD 60
+ALOAD 89
 MONITOREXIT
-GOTO L652
-ASTORE 61
-ALOAD 60
+GOTO L959
+ASTORE 90
+ALOAD 89
 MONITOREXIT
-ALOAD 61
+ALOAD 90
 ATHROW
-GOTO L659
-ASTORE 62
-GETSTATIC java/lang/System.out:Ljava/io/PrintStream;
-ALOAD 62
+GOTO L966
+ASTORE 91
+GETSTATIC java/lang/System.out:Ljava/io/PrintStream;
+ALOAD 91
 INVOKEVIRTUAL java/lang/Exception.getMessage()Ljava/lang/String;
 INVOKEDYNAMIC makeConcatWithConstants(Ljava/lang/String;)Ljava/lang/String; bsm=H6 java/lang/invoke/StringConcatFactory.makeConcatWithConstants(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;Ljava/lang/String;[Ljava/lang/Object;)Ljava/lang/invoke/CallSite; args=[str:"[LiTo] magic-doll disconnect cleanup failed: \u0001"]
 INVOKEVIRTUAL java/io/PrintStream.println(Ljava/lang/String;)V
 ALOAD 0
 INVOKESPECIAL local/lito381/server/ClientSession.persistLitoCharacterOnDisconnect()V
-GOTO L671
-ASTORE 63
-GETSTATIC java/lang/System.out:Ljava/io/PrintStream;
-ALOAD 63
+GOTO L978
+ASTORE 92
+GETSTATIC java/lang/System.out:Ljava/io/PrintStream;
+ALOAD 92
 INVOKEVIRTUAL java/lang/Object.getClass()Ljava/lang/Class;
 INVOKEVIRTUAL java/lang/Class.getSimpleName()Ljava/lang/String;
-ALOAD 63
+ALOAD 92
 INVOKEVIRTUAL java/lang/Exception.getMessage()Ljava/lang/String;
 INVOKEDYNAMIC makeConcatWithConstants(Ljava/lang/String;Ljava/lang/String;)Ljava/lang/String; bsm=H6 java/lang/invoke/StringConcatFactory.makeConcatWithConstants(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;Ljava/lang/String;[Ljava/lang/Object;)Ljava/lang/invoke/CallSite; args=[str:"[LiTo] MySQL character disconnect persistence failed: \u0001: \u0001"]
 INVOKEVIRTUAL java/io/PrintStream.println(Ljava/lang/String;)V
 ALOAD 0
 INVOKESPECIAL local/lito381/server/ClientSession.notifyPeersOfDisconnect()V
-GOTO L680
-ASTORE 64
-GETSTATIC java/lang/System.out:Ljava/io/PrintStream;
-ALOAD 64
+GOTO L987
+ASTORE 93
+GETSTATIC java/lang/System.out:Ljava/io/PrintStream;
+ALOAD 93
 INVOKEVIRTUAL java/io/IOException.getMessage()Ljava/lang/String;
 INVOKEDYNAMIC makeConcatWithConstants(Ljava/lang/String;)Ljava/lang/String; bsm=H6 java/lang/invoke/StringConcatFactory.makeConcatWithConstants(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;Ljava/lang/String;[Ljava/lang/Object;)Ljava/lang/invoke/CallSite; args=[str:"[LiTo] disconnect broadcast failed: \u0001"]
 INVOKEVIRTUAL java/io/PrintStream.println(Ljava/lang/String;)V
@@ -695,7 +1002,7 @@
 ALOAD 0
 GETFIELD local/lito381/server/ClientSession.selectedCharacter:Ljava/lang/String;
 INVOKEVIRTUAL java/lang/String.isBlank()Z
-IFNE L710
+IFNE L1017
 ALOAD 0
 GETFIELD local/lito381/server/ClientSession.state:Llocal/lito381/server/GameStateStore;
 ALOAD 0
@@ -714,221 +1021,221 @@
 GETFIELD local/lito381/server/ClientSession.hub:Llocal/lito381/server/GameHub;
 ALOAD 0
 INVOKEVIRTUAL local/lito381/server/GameHub.disconnect(Llocal/lito381/server/ClientSession;)Ljava/util/List;
-ASTORE 65
 ALOAD 0
 INVOKESTATIC local/lito381/server/NativeRoomRules.restoreBot(Llocal/lito381/server/ClientSession;)V
-ILOAD 43
-IFEQ L731
-ALOAD 0
-LLOAD 41/42
+ASTORE 94
+ILOAD 72
+IFEQ L1038
+ALOAD 0
+LLOAD 70/71
 CONST str:"disconnect"
 INVOKESPECIAL local/lito381/server/ClientSession.broadcastRoomOwner(JLjava/lang/String;)V
-GOTO L731
-ASTORE 66
-GETSTATIC java/lang/System.out:Ljava/io/PrintStream;
-LLOAD 41/42
-ALOAD 66
+GOTO L1038
+ASTORE 95
+GETSTATIC java/lang/System.out:Ljava/io/PrintStream;
+LLOAD 70/71
+ALOAD 95
 INVOKEVIRTUAL java/io/IOException.getMessage()Ljava/lang/String;
 INVOKEDYNAMIC makeConcatWithConstants(JLjava/lang/String;)Ljava/lang/String; bsm=H6 java/lang/invoke/StringConcatFactory.makeConcatWithConstants(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;Ljava/lang/String;[Ljava/lang/Object;)Ljava/lang/invoke/CallSite; args=[str:"[LiTo] room-owner broadcast failed room=\u0001: \u0001"]
 INVOKEVIRTUAL java/io/PrintStream.println(Ljava/lang/String;)V
-ALOAD 65
+ALOAD 94
 INVOKEINTERFACE java/util/List.iterator()Ljava/util/Iterator; itf
-ASTORE 67
-ALOAD 67
+ASTORE 96
+ALOAD 96
 INVOKEINTERFACE java/util/Iterator.hasNext()Z itf
-IFEQ L754
-ALOAD 67
+IFEQ L1061
+ALOAD 96
 INVOKEINTERFACE java/util/Iterator.next()Ljava/lang/Object; itf
 CHECKCAST java/lang/Long
 INVOKEVIRTUAL java/lang/Long.longValue()J
-LSTORE 68/69
-ALOAD 0
-LLOAD 68/69
+LSTORE 97/98
+ALOAD 0
+LLOAD 97/98
 INVOKESPECIAL local/lito381/server/ClientSession.broadcastRoomDeleted(J)V
-GOTO L753
-ASTORE 70
-GETSTATIC java/lang/System.out:Ljava/io/PrintStream;
-LLOAD 68/69
-ALOAD 70
+GOTO L1060
+ASTORE 99
+GETSTATIC java/lang/System.out:Ljava/io/PrintStream;
+LLOAD 97/98
+ALOAD 99
 INVOKEVIRTUAL java/io/IOException.getMessage()Ljava/lang/String;
 INVOKEDYNAMIC makeConcatWithConstants(JLjava/lang/String;)Ljava/lang/String; bsm=H6 java/lang/invoke/StringConcatFactory.makeConcatWithConstants(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;Ljava/lang/String;[Ljava/lang/Object;)Ljava/lang/invoke/CallSite; args=[str:"[LiTo] room-delete broadcast failed room=\u0001: \u0001"]
 INVOKEVIRTUAL java/io/PrintStream.println(Ljava/lang/String;)V
-GOTO L734
-GOTO L1066
-ASTORE 71
+GOTO L1041
+GOTO L1373
+ASTORE 100
 ALOAD 0
 INVOKESTATIC local/lito381/server/NativeServerLifetime.awaitCleanup(Llocal/lito381/server/ClientSession;)V
 ALOAD 0
 GETFIELD local/lito381/server/ClientSession.currentRoomId:J
-LSTORE 72/73
+LSTORE 101/102
 ALOAD 0
 GETFIELD local/lito381/server/ClientSession.phase:Llocal/lito381/server/ClientSession$Phase;
 GETSTATIC local/lito381/server/ClientSession$Phase.ROOM:Llocal/lito381/server/ClientSession$Phase;
-IF_ACMPNE L777
-LLOAD 72/73
+IF_ACMPNE L1084
+LLOAD 101/102
 CONST Long:0
 LCMP
-IFEQ L777
+IFEQ L1084
 ALOAD 0
 GETFIELD local/lito381/server/ClientSession.hub:Llocal/lito381/server/GameHub;
-LLOAD 72/73
+LLOAD 101/102
 ALOAD 0
 INVOKEVIRTUAL local/lito381/server/GameHub.isOwner(JLlocal/lito381/server/ClientSession;)Z
-IFEQ L777
+IFEQ L1084
 CONST Integer:1
-GOTO L778
-CONST Integer:0
-ISTORE 74
+GOTO L1085
+CONST Integer:0
+ISTORE 103
 ALOAD 0
 CONST str:"disconnect"
 CONST Integer:0
 INVOKESPECIAL local/lito381/server/ClientSession.cancelActivePlayerTrade(Ljava/lang/String;Z)V
-GOTO L790
-ASTORE 75
-GETSTATIC java/lang/System.out:Ljava/io/PrintStream;
-ALOAD 75
+GOTO L1097
+ASTORE 104
+GETSTATIC java/lang/System.out:Ljava/io/PrintStream;
+ALOAD 104
 INVOKEVIRTUAL java/io/IOException.getMessage()Ljava/lang/String;
 INVOKEDYNAMIC makeConcatWithConstants(Ljava/lang/String;)Ljava/lang/String; bsm=H6 java/lang/invoke/StringConcatFactory.makeConcatWithConstants(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;Ljava/lang/String;[Ljava/lang/Object;)Ljava/lang/invoke/CallSite; args=[str:"[LiTo] trade disconnect cleanup failed: \u0001"]
 INVOKEVIRTUAL java/io/PrintStream.println(Ljava/lang/String;)V
 ALOAD 0
 GETFIELD local/lito381/server/ClientSession.classicPartyParticipant:Z
-IFEQ L804
+IFEQ L1111
 ALOAD 0
 CONST str:"disconnect"
 CONST Integer:0
 INVOKESPECIAL local/lito381/server/ClientSession.leaveClassicParty(Ljava/lang/String;Z)V
-GOTO L804
-ASTORE 76
-GETSTATIC java/lang/System.out:Ljava/io/PrintStream;
-ALOAD 76
+GOTO L1111
+ASTORE 105
+GETSTATIC java/lang/System.out:Ljava/io/PrintStream;
+ALOAD 105
 INVOKEVIRTUAL java/io/IOException.getMessage()Ljava/lang/String;
 INVOKEDYNAMIC makeConcatWithConstants(Ljava/lang/String;)Ljava/lang/String; bsm=H6 java/lang/invoke/StringConcatFactory.makeConcatWithConstants(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;Ljava/lang/String;[Ljava/lang/Object;)Ljava/lang/invoke/CallSite; args=[str:"[LiTo] party disconnect cleanup failed: \u0001"]
 INVOKEVIRTUAL java/io/PrintStream.println(Ljava/lang/String;)V
 ALOAD 0
 GETFIELD local/lito381/server/ClientSession.phase:Llocal/lito381/server/ClientSession$Phase;
 GETSTATIC local/lito381/server/ClientSession$Phase.WORLD:Llocal/lito381/server/ClientSession$Phase;
-IF_ACMPNE L826
+IF_ACMPNE L1133
 ALOAD 0
 INVOKESPECIAL local/lito381/server/ClientSession.collectClassicWorldPetsOnDisconnect()V
-GOTO L817
-ASTORE 77
-GETSTATIC java/lang/System.out:Ljava/io/PrintStream;
-ALOAD 77
+GOTO L1124
+ASTORE 106
+GETSTATIC java/lang/System.out:Ljava/io/PrintStream;
+ALOAD 106
 INVOKEVIRTUAL java/lang/Exception.getMessage()Ljava/lang/String;
 INVOKEDYNAMIC makeConcatWithConstants(Ljava/lang/String;)Ljava/lang/String; bsm=H6 java/lang/invoke/StringConcatFactory.makeConcatWithConstants(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;Ljava/lang/String;[Ljava/lang/Object;)Ljava/lang/invoke/CallSite; args=[str:"[LiTo] persistent pet disconnect cleanup failed: \u0001"]
 INVOKEVIRTUAL java/io/PrintStream.println(Ljava/lang/String;)V
 ALOAD 0
 INVOKESPECIAL local/lito381/server/ClientSession.cleanupClassicWorldTransientCompanionsOnDisconnect()V
-GOTO L826
-ASTORE 78
-GETSTATIC java/lang/System.out:Ljava/io/PrintStream;
-ALOAD 78
+GOTO L1133
+ASTORE 107
+GETSTATIC java/lang/System.out:Ljava/io/PrintStream;
+ALOAD 107
 INVOKEVIRTUAL java/lang/Exception.getMessage()Ljava/lang/String;
 INVOKEDYNAMIC makeConcatWithConstants(Ljava/lang/String;)Ljava/lang/String; bsm=H6 java/lang/invoke/StringConcatFactory.makeConcatWithConstants(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;Ljava/lang/String;[Ljava/lang/Object;)Ljava/lang/invoke/CallSite; args=[str:"[LiTo] transient companion disconnect cleanup failed: \u0001"]
 INVOKEVIRTUAL java/io/PrintStream.println(Ljava/lang/String;)V
 ALOAD 0
 INVOKESPECIAL local/lito381/server/ClientSession.persistExperienceGemOnDisconnect()V
-GOTO L835
-ASTORE 79
-GETSTATIC java/lang/System.out:Ljava/io/PrintStream;
-ALOAD 79
+GOTO L1142
+ASTORE 108
+GETSTATIC java/lang/System.out:Ljava/io/PrintStream;
+ALOAD 108
 INVOKEVIRTUAL java/lang/Exception.getMessage()Ljava/lang/String;
 INVOKEDYNAMIC makeConcatWithConstants(Ljava/lang/String;)Ljava/lang/String; bsm=H6 java/lang/invoke/StringConcatFactory.makeConcatWithConstants(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;Ljava/lang/String;[Ljava/lang/Object;)Ljava/lang/invoke/CallSite; args=[str:"[LiTo] Dragon Gem disconnect persistence failed: \u0001"]
 INVOKEVIRTUAL java/io/PrintStream.println(Ljava/lang/String;)V
 ALOAD 0
 INVOKESPECIAL local/lito381/server/ClientSession.persistFireWeaponOnDisconnect()V
-GOTO L844
-ASTORE 80
-GETSTATIC java/lang/System.out:Ljava/io/PrintStream;
-ALOAD 80
+GOTO L1151
+ASTORE 109
+GETSTATIC java/lang/System.out:Ljava/io/PrintStream;
+ALOAD 109
 INVOKEVIRTUAL java/lang/Exception.getMessage()Ljava/lang/String;
 INVOKEDYNAMIC makeConcatWithConstants(Ljava/lang/String;)Ljava/lang/String; bsm=H6 java/lang/invoke/StringConcatFactory.makeConcatWithConstants(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;Ljava/lang/String;[Ljava/lang/Object;)Ljava/lang/invoke/CallSite; args=[str:"[LiTo] Fire Weapon disconnect persistence failed: \u0001"]
 INVOKEVIRTUAL java/io/PrintStream.println(Ljava/lang/String;)V
 ALOAD 0
 INVOKESPECIAL local/lito381/server/ClientSession.persistBurningWeaponOnDisconnect()V
-GOTO L853
-ASTORE 81
-GETSTATIC java/lang/System.out:Ljava/io/PrintStream;
-ALOAD 81
+GOTO L1160
+ASTORE 110
+GETSTATIC java/lang/System.out:Ljava/io/PrintStream;
+ALOAD 110
 INVOKEVIRTUAL java/lang/Exception.getMessage()Ljava/lang/String;
 INVOKEDYNAMIC makeConcatWithConstants(Ljava/lang/String;)Ljava/lang/String; bsm=H6 java/lang/invoke/StringConcatFactory.makeConcatWithConstants(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;Ljava/lang/String;[Ljava/lang/Object;)Ljava/lang/invoke/CallSite; args=[str:"[LiTo] Burning Weapon disconnect persistence failed: \u0001"]
 INVOKEVIRTUAL java/io/PrintStream.println(Ljava/lang/String;)V
 ALOAD 0
 INVOKESPECIAL local/lito381/server/ClientSession.persistWindShotOnDisconnect()V
-GOTO L862
-ASTORE 82
-GETSTATIC java/lang/System.out:Ljava/io/PrintStream;
-ALOAD 82
+GOTO L1169
+ASTORE 111
+GETSTATIC java/lang/System.out:Ljava/io/PrintStream;
+ALOAD 111
 INVOKEVIRTUAL java/lang/Exception.getMessage()Ljava/lang/String;
 INVOKEDYNAMIC makeConcatWithConstants(Ljava/lang/String;)Ljava/lang/String; bsm=H6 java/lang/invoke/StringConcatFactory.makeConcatWithConstants(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;Ljava/lang/String;[Ljava/lang/Object;)Ljava/lang/invoke/CallSite; args=[str:"[LiTo] Wind Shot disconnect persistence failed: \u0001"]
 INVOKEVIRTUAL java/io/PrintStream.println(Ljava/lang/String;)V
 ALOAD 0
 INVOKESPECIAL local/lito381/server/ClientSession.persistFireBlessOnDisconnect()V
-GOTO L871
-ASTORE 83
-GETSTATIC java/lang/System.out:Ljava/io/PrintStream;
-ALOAD 83
+GOTO L1178
+ASTORE 112
+GETSTATIC java/lang/System.out:Ljava/io/PrintStream;
+ALOAD 112
 INVOKEVIRTUAL java/lang/Exception.getMessage()Ljava/lang/String;
 INVOKEDYNAMIC makeConcatWithConstants(Ljava/lang/String;)Ljava/lang/String; bsm=H6 java/lang/invoke/StringConcatFactory.makeConcatWithConstants(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;Ljava/lang/String;[Ljava/lang/Object;)Ljava/lang/invoke/CallSite; args=[str:"[LiTo] Fire Bless disconnect persistence failed: \u0001"]
 INVOKEVIRTUAL java/io/PrintStream.println(Ljava/lang/String;)V
 ALOAD 0
 INVOKESPECIAL local/lito381/server/ClientSession.persistStormEyeOnDisconnect()V
-GOTO L880
-ASTORE 84
-GETSTATIC java/lang/System.out:Ljava/io/PrintStream;
-ALOAD 84
+GOTO L1187
+ASTORE 113
+GETSTATIC java/lang/System.out:Ljava/io/PrintStream;
+ALOAD 113
 INVOKEVIRTUAL java/lang/Exception.getMessage()Ljava/lang/String;
 INVOKEDYNAMIC makeConcatWithConstants(Ljava/lang/String;)Ljava/lang/String; bsm=H6 java/lang/invoke/StringConcatFactory.makeConcatWithConstants(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;Ljava/lang/String;[Ljava/lang/Object;)Ljava/lang/invoke/CallSite; args=[str:"[LiTo] Storm Eye disconnect persistence failed: \u0001"]
 INVOKEVIRTUAL java/io/PrintStream.println(Ljava/lang/String;)V
 ALOAD 0
 INVOKESPECIAL local/lito381/server/ClientSession.persistStormShotOnDisconnect()V
-GOTO L889
-ASTORE 85
-GETSTATIC java/lang/System.out:Ljava/io/PrintStream;
-ALOAD 85
+GOTO L1196
+ASTORE 114
+GETSTATIC java/lang/System.out:Ljava/io/PrintStream;
+ALOAD 114
 INVOKEVIRTUAL java/lang/Exception.getMessage()Ljava/lang/String;
 INVOKEDYNAMIC makeConcatWithConstants(Ljava/lang/String;)Ljava/lang/String; bsm=H6 java/lang/invoke/StringConcatFactory.makeConcatWithConstants(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;Ljava/lang/String;[Ljava/lang/Object;)Ljava/lang/invoke/CallSite; args=[str:"[LiTo] Storm Shot disconnect persistence failed: \u0001"]
 INVOKEVIRTUAL java/io/PrintStream.println(Ljava/lang/String;)V
 ALOAD 0
 INVOKESPECIAL local/lito381/server/ClientSession.persistEarthBlessOnDisconnect()V
-GOTO L898
-ASTORE 86
-GETSTATIC java/lang/System.out:Ljava/io/PrintStream;
-ALOAD 86
+GOTO L1205
+ASTORE 115
+GETSTATIC java/lang/System.out:Ljava/io/PrintStream;
+ALOAD 115
 INVOKEVIRTUAL java/lang/Exception.getMessage()Ljava/lang/String;
 INVOKEDYNAMIC makeConcatWithConstants(Ljava/lang/String;)Ljava/lang/String; bsm=H6 java/lang/invoke/StringConcatFactory.makeConcatWithConstants(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;Ljava/lang/String;[Ljava/lang/Object;)Ljava/lang/invoke/CallSite; args=[str:"[LiTo] Earth Bless disconnect persistence failed: \u0001"]
 INVOKEVIRTUAL java/io/PrintStream.println(Ljava/lang/String;)V
 ALOAD 0
 INVOKESPECIAL local/lito381/server/ClientSession.persistIronSkinOnDisconnect()V
-GOTO L907
-ASTORE 87
-GETSTATIC java/lang/System.out:Ljava/io/PrintStream;
-ALOAD 87
+GOTO L1214
+ASTORE 116
+GETSTATIC java/lang/System.out:Ljava/io/PrintStream;
+ALOAD 116
 INVOKEVIRTUAL java/lang/Exception.getMessage()Ljava/lang/String;
 INVOKEDYNAMIC makeConcatWithConstants(Ljava/lang/String;)Ljava/lang/String; bsm=H6 java/lang/invoke/StringConcatFactory.makeConcatWithConstants(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;Ljava/lang/String;[Ljava/lang/Object;)Ljava/lang/invoke/CallSite; args=[str:"[LiTo] Iron Skin disconnect persistence failed: \u0001"]
 INVOKEVIRTUAL java/io/PrintStream.println(Ljava/lang/String;)V
 ALOAD 0
 INVOKESPECIAL local/lito381/server/ClientSession.persistMovementBuffOnDisconnect()V
-GOTO L916
-ASTORE 88
-GETSTATIC java/lang/System.out:Ljava/io/PrintStream;
-ALOAD 88
+GOTO L1223
+ASTORE 117
+GETSTATIC java/lang/System.out:Ljava/io/PrintStream;
+ALOAD 117
 INVOKEVIRTUAL java/lang/Exception.getMessage()Ljava/lang/String;
 INVOKEDYNAMIC makeConcatWithConstants(Ljava/lang/String;)Ljava/lang/String; bsm=H6 java/lang/invoke/StringConcatFactory.makeConcatWithConstants(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;Ljava/lang/String;[Ljava/lang/Object;)Ljava/lang/invoke/CallSite; args=[str:"[LiTo] movement buff disconnect persistence failed: \u0001"]
 INVOKEVIRTUAL java/io/PrintStream.println(Ljava/lang/String;)V
 ALOAD 0
 INVOKESPECIAL local/lito381/server/ClientSession.persistArmorProtectionOnDisconnect()V
-GOTO L925
-ASTORE 89
-GETSTATIC java/lang/System.out:Ljava/io/PrintStream;
-ALOAD 89
+GOTO L1232
+ASTORE 118
+GETSTATIC java/lang/System.out:Ljava/io/PrintStream;
+ALOAD 118
 INVOKEVIRTUAL java/lang/Exception.getMessage()Ljava/lang/String;
 INVOKEDYNAMIC makeConcatWithConstants(Ljava/lang/String;)Ljava/lang/String; bsm=H6 java/lang/invoke/StringConcatFactory.makeConcatWithConstants(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;Ljava/lang/String;[Ljava/lang/Object;)Ljava/lang/invoke/CallSite; args=[str:"[LiTo] armor protection disconnect persistence failed: \u0001"]
 INVOKEVIRTUAL java/io/PrintStream.println(Ljava/lang/String;)V
 ALOAD 0
 GETFIELD local/lito381/server/ClientSession.phase:Llocal/lito381/server/ClientSession$Phase;
 GETSTATIC local/lito381/server/ClientSession$Phase.WORLD:Llocal/lito381/server/ClientSession$Phase;
-IF_ACMPNE L945
+IF_ACMPNE L1252
 ALOAD 0
 CONST str:"owner-disconnect"
 INVOKESPECIAL local/lito381/server/ClientSession.abandonClassicWorldRescueFollower(Ljava/lang/String;)V
@@ -938,55 +1245,55 @@
 ALOAD 0
 CONST str:"owner-disconnect"
 INVOKESPECIAL local/lito381/server/ClientSession.abandonClassicWorldRoiFollower(Ljava/lang/String;)V
-GOTO L945
-ASTORE 90
-GETSTATIC java/lang/System.out:Ljava/io/PrintStream;
-ALOAD 90
+GOTO L1252
+ASTORE 119
+GETSTATIC java/lang/System.out:Ljava/io/PrintStream;
+ALOAD 119
 INVOKEVIRTUAL java/lang/Exception.getMessage()Ljava/lang/String;
 INVOKEDYNAMIC makeConcatWithConstants(Ljava/lang/String;)Ljava/lang/String; bsm=H6 java/lang/invoke/StringConcatFactory.makeConcatWithConstants(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;Ljava/lang/String;[Ljava/lang/Object;)Ljava/lang/invoke/CallSite; args=[str:"[LiTo] quest follower disconnect cleanup failed: \u0001"]
 INVOKEVIRTUAL java/io/PrintStream.println(Ljava/lang/String;)V
 ALOAD 0
 GETFIELD local/lito381/server/ClientSession.activeMagicDollObjectIds:Ljava/util/Set;
 DUP
-ASTORE 91
+ASTORE 120
 MONITORENTER
 ALOAD 0
 CONST str:"owner-disconnect"
 CONST Integer:0
 INVOKESPECIAL local/lito381/server/ClientSession.recallNativeMagicDollLocked(Ljava/lang/String;Z)V
-ALOAD 91
+ALOAD 120
 MONITOREXIT
-GOTO L962
-ASTORE 92
-ALOAD 91
+GOTO L1269
+ASTORE 121
+ALOAD 120
 MONITOREXIT
-ALOAD 92
+ALOAD 121
 ATHROW
-GOTO L969
-ASTORE 93
-GETSTATIC java/lang/System.out:Ljava/io/PrintStream;
-ALOAD 93
+GOTO L1276
+ASTORE 122
+GETSTATIC java/lang/System.out:Ljava/io/PrintStream;
+ALOAD 122
 INVOKEVIRTUAL java/lang/Exception.getMessage()Ljava/lang/String;
 INVOKEDYNAMIC makeConcatWithConstants(Ljava/lang/String;)Ljava/lang/String; bsm=H6 java/lang/invoke/StringConcatFactory.makeConcatWithConstants(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;Ljava/lang/String;[Ljava/lang/Object;)Ljava/lang/invoke/CallSite; args=[str:"[LiTo] magic-doll disconnect cleanup failed: \u0001"]
 INVOKEVIRTUAL java/io/PrintStream.println(Ljava/lang/String;)V
 ALOAD 0
 INVOKESPECIAL local/lito381/server/ClientSession.persistLitoCharacterOnDisconnect()V
-GOTO L981
-ASTORE 94
-GETSTATIC java/lang/System.out:Ljava/io/PrintStream;
-ALOAD 94
+GOTO L1288
+ASTORE 123
+GETSTATIC java/lang/System.out:Ljava/io/PrintStream;
+ALOAD 123
 INVOKEVIRTUAL java/lang/Object.getClass()Ljava/lang/Class;
 INVOKEVIRTUAL java/lang/Class.getSimpleName()Ljava/lang/String;
-ALOAD 94
+ALOAD 123
 INVOKEVIRTUAL java/lang/Exception.getMessage()Ljava/lang/String;
 INVOKEDYNAMIC makeConcatWithConstants(Ljava/lang/String;Ljava/lang/String;)Ljava/lang/String; bsm=H6 java/lang/invoke/StringConcatFactory.makeConcatWithConstants(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;Ljava/lang/String;[Ljava/lang/Object;)Ljava/lang/invoke/CallSite; args=[str:"[LiTo] MySQL character disconnect persistence failed: \u0001: \u0001"]
 INVOKEVIRTUAL java/io/PrintStream.println(Ljava/lang/String;)V
 ALOAD 0
 INVOKESPECIAL local/lito381/server/ClientSession.notifyPeersOfDisconnect()V
-GOTO L990
-ASTORE 95
-GETSTATIC java/lang/System.out:Ljava/io/PrintStream;
-ALOAD 95
+GOTO L1297
+ASTORE 124
+GETSTATIC java/lang/System.out:Ljava/io/PrintStream;
+ALOAD 124
 INVOKEVIRTUAL java/io/IOException.getMessage()Ljava/lang/String;
 INVOKEDYNAMIC makeConcatWithConstants(Ljava/lang/String;)Ljava/lang/String; bsm=H6 java/lang/invoke/StringConcatFactory.makeConcatWithConstants(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;Ljava/lang/String;[Ljava/lang/Object;)Ljava/lang/invoke/CallSite; args=[str:"[LiTo] disconnect broadcast failed: \u0001"]
 INVOKEVIRTUAL java/io/PrintStream.println(Ljava/lang/String;)V
@@ -1005,7 +1312,7 @@
 ALOAD 0
 GETFIELD local/lito381/server/ClientSession.selectedCharacter:Ljava/lang/String;
 INVOKEVIRTUAL java/lang/String.isBlank()Z
-IFNE L1020
+IFNE L1327
 ALOAD 0
 GETFIELD local/lito381/server/ClientSession.state:Llocal/lito381/server/GameStateStore;
 ALOAD 0
@@ -1024,46 +1331,46 @@
 GETFIELD local/lito381/server/ClientSession.hub:Llocal/lito381/server/GameHub;
 ALOAD 0
 INVOKEVIRTUAL local/lito381/server/GameHub.disconnect(Llocal/lito381/server/ClientSession;)Ljava/util/List;
-ASTORE 96
 ALOAD 0
 INVOKESTATIC local/lito381/server/NativeRoomRules.restoreBot(Llocal/lito381/server/ClientSession;)V
-ILOAD 74
-IFEQ L1041
-ALOAD 0
-LLOAD 72/73
+ASTORE 125
+ILOAD 103
+IFEQ L1348
+ALOAD 0
+LLOAD 101/102
 CONST str:"disconnect"
 INVOKESPECIAL local/lito381/server/ClientSession.broadcastRoomOwner(JLjava/lang/String;)V
-GOTO L1041
-ASTORE 97
-GETSTATIC java/lang/System.out:Ljava/io/PrintStream;
-LLOAD 72/73
-ALOAD 97
+GOTO L1348
+ASTORE 126
+GETSTATIC java/lang/System.out:Ljava/io/PrintStream;
+LLOAD 101/102
+ALOAD 126
 INVOKEVIRTUAL java/io/IOException.getMessage()Ljava/lang/String;
 INVOKEDYNAMIC makeConcatWithConstants(JLjava/lang/String;)Ljava/lang/String; bsm=H6 java/lang/invoke/StringConcatFactory.makeConcatWithConstants(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;Ljava/lang/String;[Ljava/lang/Object;)Ljava/lang/invoke/CallSite; args=[str:"[LiTo] room-owner broadcast failed room=\u0001: \u0001"]
 INVOKEVIRTUAL java/io/PrintStream.println(Ljava/lang/String;)V
-ALOAD 96
+ALOAD 125
 INVOKEINTERFACE java/util/List.iterator()Ljava/util/Iterator; itf
-ASTORE 98
-ALOAD 98
+ASTORE 127
+ALOAD 127
 INVOKEINTERFACE java/util/Iterator.hasNext()Z itf
-IFEQ L1064
-ALOAD 98
+IFEQ L1371
+ALOAD 127
 INVOKEINTERFACE java/util/Iterator.next()Ljava/lang/Object; itf
 CHECKCAST java/lang/Long
 INVOKEVIRTUAL java/lang/Long.longValue()J
-LSTORE 99/100
-ALOAD 0
-LLOAD 99/100
+LSTORE 128/129
+ALOAD 0
+LLOAD 128/129
 INVOKESPECIAL local/lito381/server/ClientSession.broadcastRoomDeleted(J)V
-GOTO L1063
-ASTORE 101
-GETSTATIC java/lang/System.out:Ljava/io/PrintStream;
-LLOAD 99/100
-ALOAD 101
+GOTO L1370
+ASTORE 130
+GETSTATIC java/lang/System.out:Ljava/io/PrintStream;
+LLOAD 128/129
+ALOAD 130
 INVOKEVIRTUAL java/io/IOException.getMessage()Ljava/lang/String;
 INVOKEDYNAMIC makeConcatWithConstants(JLjava/lang/String;)Ljava/lang/String; bsm=H6 java/lang/invoke/StringConcatFactory.makeConcatWithConstants(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;Ljava/lang/String;[Ljava/lang/Object;)Ljava/lang/invoke/CallSite; args=[str:"[LiTo] room-delete broadcast failed room=\u0001: \u0001"]
 INVOKEVIRTUAL java/io/PrintStream.println(Ljava/lang/String;)V
-GOTO L1044
-ALOAD 71
+GOTO L1351
+ALOAD 100
 ATHROW
 RETURN
```


</details>

### lambda 名稱與本體代表

以下保留兩個方向各三個原名方法的全部差異。同名方法不存在時顯示該側為空；對側其他名稱的呼叫點見前表，仍須建立完整配對證明。

#### ONLY_LEFT　`local/lito381/server/ClientSession#lambda$autoSelectPendingRentalBattleHandoff$30(Llocal/lito381/server/CharacterSummary;)Z`


```diff
--- staged/local/lito381/server/ClientSession#lambda$autoSelectPendingRentalBattleHandoff$30(Llocal/lito381/server/CharacterSummary;)Z
+++ candidate/local/lito381/server/ClientSession#lambda$autoSelectPendingRentalBattleHandoff$30(Llocal/lito381/server/CharacterSummary;)Z
@@ -2,19 +2,19 @@
 TRY []
 ALOAD 0
 GETFIELD local/lito381/server/ClientSession.hub:Llocal/lito381/server/GameHub;
-ASTORE 2
 ALOAD 0
 INVOKEVIRTUAL local/lito381/server/ClientSession.accountName()Ljava/lang/String;
-ASTORE 3
 ALOAD 0
 ALOAD 1
 INVOKEVIRTUAL local/lito381/server/CharacterSummary.name()Ljava/lang/String;
 INVOKESPECIAL local/lito381/server/ClientSession.stateCharacter(Ljava/lang/String;)Ljava/lang/String;
+ASTORE 2
+ASTORE 3
 ASTORE 4
-ALOAD 2
+ALOAD 4
 ALOAD 0
 ALOAD 3
 INVOKESTATIC local/lito381/server/SourceLogin.account(Llocal/lito381/server/ClientSession;Ljava/lang/String;)Ljava/lang/String;
-ALOAD 4
+ALOAD 2
 INVOKEVIRTUAL local/lito381/server/GameHub.hasRentalBattleHandoff(Ljava/lang/String;Ljava/lang/String;)Z
 IRETURN
```


#### ONLY_LEFT　`local/lito381/server/ClientSession#lambda$cleanupClassicWorldTransientCompanionsOnDisconnect$286(Llocal/lito381/server/BattleCompanion;)Z`


```diff
--- staged/local/lito381/server/ClientSession#lambda$cleanupClassicWorldTransientCompanionsOnDisconnect$286(Llocal/lito381/server/BattleCompanion;)Z
+++ candidate/local/lito381/server/ClientSession#lambda$cleanupClassicWorldTransientCompanionsOnDisconnect$286(Llocal/lito381/server/BattleCompanion;)Z
@@ -1,11 +0,0 @@
-META 4106 null []
-TRY []
-ALOAD 0
-INVOKEVIRTUAL local/lito381/server/BattleCompanion.snapshot()Llocal/lito381/server/BattleCompanion$Snapshot;
-INVOKEVIRTUAL local/lito381/server/BattleCompanion$Snapshot.kind()Llocal/lito381/server/BattleCompanion$Kind;
-GETSTATIC local/lito381/server/BattleCompanion$Kind.PET:Llocal/lito381/server/BattleCompanion$Kind;
-IF_ACMPEQ L7
-CONST Integer:1
-GOTO L8
-CONST Integer:0
-IRETURN
```


#### ONLY_LEFT　`local/lito381/server/ClientSession#lambda$completeClassicWorldPortal$162(Llocal/lito381/server/Protocol$PortalDestination;Llocal/lito381/server/ClientSession$PendingClassicWorldPortal;)V`


```diff
--- staged/local/lito381/server/ClientSession#lambda$completeClassicWorldPortal$162(Llocal/lito381/server/Protocol$PortalDestination;Llocal/lito381/server/ClientSession$PendingClassicWorldPortal;)V
+++ candidate/local/lito381/server/ClientSession#lambda$completeClassicWorldPortal$162(Llocal/lito381/server/Protocol$PortalDestination;Llocal/lito381/server/ClientSession$PendingClassicWorldPortal;)V
@@ -1,7 +0,0 @@
-META 4098 null [java/io/IOException]
-TRY []
-ALOAD 0
-ALOAD 1
-ALOAD 2
-INVOKESPECIAL local/lito381/server/ClientSession.nativePlacementPortalBody(Llocal/lito381/server/Protocol$PortalDestination;Llocal/lito381/server/ClientSession$PendingClassicWorldPortal;)V
-RETURN
```


#### ONLY_RIGHT　`local/lito381/server/ClientSession#lambda$cleanupClassicWorldTransientCompanionsOnDisconnect$456(Llocal/lito381/server/BattleCompanion;)Z`


```diff
--- staged/local/lito381/server/ClientSession#lambda$cleanupClassicWorldTransientCompanionsOnDisconnect$456(Llocal/lito381/server/BattleCompanion;)Z
+++ candidate/local/lito381/server/ClientSession#lambda$cleanupClassicWorldTransientCompanionsOnDisconnect$456(Llocal/lito381/server/BattleCompanion;)Z
@@ -0,0 +1,18 @@
+META 4106 null []
+TRY []
+ALOAD 0
+INVOKEVIRTUAL local/lito381/server/BattleCompanion.snapshot()Llocal/lito381/server/BattleCompanion$Snapshot;
+INVOKEVIRTUAL local/lito381/server/BattleCompanion$Snapshot.kind()Llocal/lito381/server/BattleCompanion$Kind;
+GETSTATIC local/lito381/server/BattleCompanion$Kind.SUMMON:Llocal/lito381/server/BattleCompanion$Kind;
+IF_ACMPNE L7
+CONST Integer:0
+IRETURN
+ALOAD 0
+INVOKEVIRTUAL local/lito381/server/BattleCompanion.snapshot()Llocal/lito381/server/BattleCompanion$Snapshot;
+INVOKEVIRTUAL local/lito381/server/BattleCompanion$Snapshot.kind()Llocal/lito381/server/BattleCompanion$Kind;
+GETSTATIC local/lito381/server/BattleCompanion$Kind.PET:Llocal/lito381/server/BattleCompanion$Kind;
+IF_ACMPEQ L14
+CONST Integer:1
+GOTO L15
+CONST Integer:0
+IRETURN
```


#### ONLY_RIGHT　`local/lito381/server/ClientSession#lambda$useAreaSilenceSpell$263(Llocal/lito381/server/ClientSession;)Z`


```diff
--- staged/local/lito381/server/ClientSession#lambda$useAreaSilenceSpell$263(Llocal/lito381/server/ClientSession;)Z
+++ candidate/local/lito381/server/ClientSession#lambda$useAreaSilenceSpell$263(Llocal/lito381/server/ClientSession;)Z
@@ -0,0 +1,12 @@
+META 4106 null []
+TRY []
+ALOAD 0
+GETFIELD local/lito381/server/ClientSession.playerHp:I
+IFLE L8
+ALOAD 0
+GETFIELD local/lito381/server/ClientSession.observer:Z
+IFNE L8
+CONST Integer:1
+GOTO L9
+CONST Integer:0
+IRETURN
```


#### ONLY_RIGHT　`local/lito381/server/ClientSession#lambda$useAreaSilenceSpell$264(Llocal/lito381/server/ClientSession;)Z`


```diff
--- staged/local/lito381/server/ClientSession#lambda$useAreaSilenceSpell$264(Llocal/lito381/server/ClientSession;)Z
+++ candidate/local/lito381/server/ClientSession#lambda$useAreaSilenceSpell$264(Llocal/lito381/server/ClientSession;)Z
@@ -0,0 +1,12 @@
+META 4098 null []
+TRY []
+ALOAD 1
+GETFIELD local/lito381/server/ClientSession.invisible:Z
+IFEQ L6
+ALOAD 1
+ALOAD 0
+IF_ACMPNE L8
+CONST Integer:1
+GOTO L9
+CONST Integer:0
+IRETURN
```
