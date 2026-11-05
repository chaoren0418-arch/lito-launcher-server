# Issue #67 — T020-B06 戰鬥併入暫存

## 結論

2026-11-06 合併最新 main（`9678e97`，包含 #186）後，從 29 個 staged Java 重新編譯並完成核准轉換，重驗全部 323 個 entry：**311 SAME、0 SAME_EFFECTS、10 SAME_PATHS、2 DIFF**，missing／extra／ONLY_LEFT／ONLY_RIGHT 均為零。現行工具尚不支援 SAME_CFG，不能填成 CFG 驗收通過。**本批受 #203 尚未合併阻擋，驗收未完成；不得送入 B-land 或以一般 CI 綠燈宣告完成。**

本輪以 GitHub API（`gh pr view 215 --json state,mergedAt,mergeCommit,headRefOid`）確認 [PR #215](https://github.com/chaoren0418-arch/lito-launcher-server/pull/215) 仍為 `OPEN`、`mergedAt=null`、`mergeCommit=null`，head 為 `1f5900d7c7cb292e3e4cd724aeebb1379f74cbfe`。已合併的 main 沒有 SAME_CFG 實作，`PathComparison.java` 仍維持 4096 上限。依 [最新主控裁定](https://github.com/chaoren0418-arch/lito-launcher-server/issues/67#issuecomment-6663017511)，兩個方法均保持 Java 不變，等待 #203 的 CFG 比較；本輪結果僅為依賴合併前的重建證據，不是審查要求的最終驗收。

主控特別指定的 `BattleQuestEncounter#pollPoisons`、`BattleTimeAttackEncounter#pollPoisons` 與 `BattleCubeHarmonyEffects#advance` 均已是 **SAME**，無須改寫。其餘兩項是 `BattleGroundEffects#cast` 的迴圈拒絕與 `BattleTimedStatus#cancelMagicEffects` 超過逐路徑比較 4096 上限。

## 做了什麼

- 在既有 `task/67` 工作，未 checkout 或建立分支。本輪起點 `b900eae`；先 fetch 並以一般 merge 合併最新 `origin/main`（`9678e97`），合併提交 `4c9d9ab`。#174／#176／#186 已合併，#203 尚未合併。前輪合併 `c724539` 的紀錄保留於 git。
- 保留前輪三方移植的全部 29 個 Java、14 個 `@AsRecord` 與 7 個 `@AsmFinal`，本輪沒有修改 Java。新的兩項仍屬 DUP／暫存位置差異，依本輪第 1 項「Java 不改」不自行改寫，也不提高工具限制或新增豁免。第 2 項列出的三個迴圈方法已通過，不觸發其改寫條件。
- 從 staged 原始碼重新 javac 編譯，唯一後處理是 repo 的 AsRecord CLI（包含 AsmFinal 支援）。未複製候選 class bytes、未在驗收前替換或改名產物。
- 核對全部現行核准清單的精確 owner；B06 命中 **0 條**。因此 CLI 不傳空清單，也未新增授權。核對包含 access-widen、deviations、effects、exceptions-exempt、extra-methods、layout、metadata-exempt、T029；完整裁切收據為 `scratch/issue-67/replay/approved-scope.json`。
- 本輪只更新本報告。`docs/REPORT.md` 的變化僅為合併 main 帶入，沒有自行編輯；相對 main 的任務範圍仍是指定 29 個 staged Java 與本報告。

## 已驗證

### 固定輸入與編譯輸出

| 項目 | SHA-256／結果 |
|---|---|
| 正式 reference/base-0.6.71.jar | `e1498871c7be6b287821c57ef5d95933db1740d9b4d4a80f6c3465f21d99d498` |
| 候選 reference/candidate-50635f7e.jar | `50635f7edca8ec6ce77787cc32371c7590b497e48ea202f8a679caf3f517b729` |
| 本輪重建 staged.jar | `8820abf5684b6fd6c9c55826372a21bad10f5f965880d4a954e57fe22d1640c6` |
| JDK | Microsoft JDK 17.0.17，`--release 17 -encoding UTF-8` |
| 來源與 entry | 29 個來源；323／323 entry；missing=[]、extra=[] |
| record | 14／14 SAME |
| AsRecord／AsmFinal | 第一次轉換 21 類；第二次轉換 0 類；逐 class SHA-256 不變 |
| 產物來源 | 本輪編譯與核准轉換；JAR hash 與前輪相同，驗證結果改善來自比較器更新 |

annotation classes 與本批輸出分離。`lib/*` 在本 worktree 為空，依賴取自兩份 reference 與 annotation 目錄。`--left-classpath` 只提供候選依賴 metadata，不覆蓋左側主 JAR。

實際輸出如下；semantic-diff 遇到 DIFF 仍 exit 0，重現程式的驗收檢查以 exit 2 表示尚未通過。

```text
build-as-record exit 0
build-semantic-diff exit 0
annotations exit 0
javac exit 0
Note: Some input files use unchecked or unsafe operations.
Note: Recompile with -Xlint:unchecked for details.
as-record exit 0
as-record: scanned=323 converted=21
entries=323 missing=[] extra=[]
as-record-idempotent exit 0
as-record: scanned=323 converted=0
semantic exit 0
SUMMARY SAME=311 SAME_EFFECTS=0 SAME_PATHS=10 ORPHAN_IGNORED=0 SAME_LAYOUT=0 DEVIATION=0 DIFF=2 ONLY_LEFT=0 ONLY_RIGHT=0 BYTE_SAME=6 ALLOW_METHODS=0
```

### 全部 323 個 entry 的分布

各列涵蓋外層與全部 `$` entry，清單直接取 B06.verify_entries；逐 entry 結果可用下方重現程式產生。

| 外層類別 | SAME | SAME_PATHS | DIFF |
|---|---:|---:|---:|
| `BattleAbilityStatus` | 21 | 2 | 0 |
| `BattleBurningWeaponStatus` | 2 | 0 | 0 |
| `BattleCompanion` | 18 | 1 | 0 |
| `BattleCounterMirrorStatus` | 1 | 1 | 0 |
| `BattleCubeHarmonyEffects` | 11 | 0 | 0 |
| `BattleCubeIgnitionEffects` | 13 | 0 | 0 |
| `BattleCubeQuakeEffects` | 15 | 0 | 0 |
| `BattleCubeShockEffects` | 14 | 0 | 0 |
| `BattleDragonSkinStatus` | 2 | 0 | 0 |
| `BattleElementalFallDownStatus` | 3 | 1 | 0 |
| `BattleEraseMagicStatus` | 2 | 0 | 0 |
| `BattleFireBlessStatus` | 2 | 0 | 0 |
| `BattleFireWeaponStatus` | 2 | 0 | 0 |
| `BattleGroundEffects` | 4 | 0 | 1 |
| `BattleGuardBrakeStatus` | 2 | 0 | 0 |
| `BattleHealingRecovery` | 1 | 0 | 0 |
| `BattleHitReactionSuppressCatalog` | 1 | 0 | 0 |
| `BattleNaturesTouchStatus` | 2 | 0 | 0 |
| `BattleQuestEncounter` | 19 | 0 | 0 |
| `BattleResistFearStatus` | 1 | 1 | 0 |
| `BattleSpellCatalog` | 132 | 2 | 0 |
| `BattleStormEyeStatus` | 2 | 0 | 0 |
| `BattleStormShotStatus` | 2 | 0 | 0 |
| `BattleTimeAttackEncounter` | 19 | 0 | 0 |
| `BattleTimedStatus` | 14 | 0 | 1 |
| `BattleTournamentNpcPhysicalDamage` | 2 | 0 | 0 |
| `BattleWeightStatus` | 1 | 1 | 0 |
| `BattleWindShackleStatus` | 1 | 1 | 0 |
| `BattleWindShotStatus` | 2 | 0 | 0 |

### 指定的三個迴圈方法

以下同時通過正式 CLI 的整類驗收與針對精確描述子的 ClassComparer.compareMethod 診斷。其整類均為 SAME，沒有使用 SAME_PATHS 或核准清單。

```text
BattleQuestEncounter#pollPoisons(JII)Ljava/util/List; SAME
BattleTimeAttackEncounter#pollPoisons(JII)Ljava/util/List; SAME
BattleCubeHarmonyEffects#advance(JLjava/util/List;Llocal/lito381/server/BattleCubeHarmonyEffects$Alliance;)Ljava/util/List; SAME
```

### 等待 #203 處理的完整差異

下列為兩個 DIFF 方法的**全部正規化指令差異區塊**，不是只取第一處差異。使用同版 JarIndex／ClassComparer.instructions 與 tryCatch，左右附相同 metadata，unified diff 的未列部分相同；沒有以此診斷取代正式 CLI。完整左右方法保留於 `scratch/issue-67/replay/methods/`，診斷程式附在後方可重建。

`BattleGroundEffects#cast` 第一處為 insn[39]，left=DUP、right=ASTORE 11。CLI 理由為 `paths rejected: normal control-flow cycle`。來源的兩個 List 別名與候選暫存位置不同，方法包含建立 Lease 的 for 迴圈。主控已指定交由 #203 CFG 比較，保持 Java 不變。

`BattleTimedStatus#cancelMagicEffects` 第一處為 insn[289]，left=DUP、right=CONST Long:0。CLI 理由為 `paths rejected: path limit 4096 exceeded`。這是未完成證明的工具界限，不能據此宣稱等價，也沒有調高上限重跑充當驗收。

精確方法：`BattleGroundEffects#cast(Llocal/lito381/server/BattleSpellCatalog$GroundSpell;JIIIILlocal/lito381/server/ArenaMapCatalog$Arena;J)Llocal/lito381/server/BattleGroundEffects$CastResult;`。

```diff
--- staged/BattleGroundEffects#cast
+++ candidate/BattleGroundEffects#cast
@@ -39,12 +39,10 @@
 ILOAD 7
 INVOKESPECIAL local/lito381/server/ArenaMapCatalog$Point.<init>(II)V
 INVOKESTATIC java/util/List.of(Ljava/lang/Object;)Ljava/util/List; itf
-DUP
 ASTORE 11
-ASTORE 12
 ALOAD 11
 INVOKEINTERFACE java/util/List.isEmpty()Z itf
-IFEQ L47
+IFEQ L45
 INVOKESTATIC local/lito381/server/BattleGroundEffects$CastResult.rejected()Llocal/lito381/server/BattleGroundEffects$CastResult;
 ARETURN
 LLOAD 9/10
@@ -54,12 +52,12 @@
 CONST Long:1000000000
 INVOKESTATIC java/lang/Math.multiplyExact(JJ)J
 INVOKESTATIC java/lang/Math.addExact(JJ)J
-LSTORE 13/14
+LSTORE 12/13
 ALOAD 1
 INVOKEVIRTUAL local/lito381/server/BattleSpellCatalog$GroundSpell.pulseIntervalMillis()I
-IFGT L60
+IFGT L58
 CONST Long:9223372036854775807
-GOTO L67
+GOTO L65
 LLOAD 9/10
 ALOAD 1
 INVOKEVIRTUAL local/lito381/server/BattleSpellCatalog$GroundSpell.pulseIntervalMillis()I
@@ -67,17 +65,17 @@
 CONST Long:1000000
 INVOKESTATIC java/lang/Math.multiplyExact(JJ)J
 INVOKESTATIC java/lang/Math.addExact(JJ)J
-LSTORE 15/16
+LSTORE 14/15
 ALOAD 11
 INVOKEINTERFACE java/util/List.iterator()Ljava/util/Iterator; itf
-ASTORE 17
-ALOAD 17
+ASTORE 16
+ALOAD 16
 INVOKEINTERFACE java/util/Iterator.hasNext()Z itf
-IFEQ L100
-ALOAD 17
+IFEQ L98
+ALOAD 16
 INVOKEINTERFACE java/util/Iterator.next()Ljava/lang/Object; itf
 CHECKCAST local/lito381/server/ArenaMapCatalog$Point
-ASTORE 18
+ASTORE 17
 ALOAD 0
 GETFIELD local/lito381/server/BattleGroundEffects.leases:Ljava/util/List;
 NEW local/lito381/server/BattleGroundEffects$Lease
@@ -85,21 +83,21 @@
 ALOAD 1
 INVOKEVIRTUAL local/lito381/server/BattleSpellCatalog$GroundSpell.skillId()I
 LLOAD 2/3
-ALOAD 18
+ALOAD 17
 INVOKEVIRTUAL local/lito381/server/ArenaMapCatalog$Point.x()I
-ALOAD 18
+ALOAD 17
 INVOKEVIRTUAL local/lito381/server/ArenaMapCatalog$Point.y()I
 ALOAD 1
 INVOKEVIRTUAL local/lito381/server/BattleSpellCatalog$GroundSpell.effectRadius()I
-LLOAD 13/14
-LLOAD 15/16
+LLOAD 12/13
+LLOAD 14/15
 ALOAD 1
 INVOKEVIRTUAL local/lito381/server/BattleSpellCatalog$GroundSpell.pulseIntervalMillis()I
 ALOAD 0
 INVOKESPECIAL local/lito381/server/BattleGroundEffects$Lease.<init>(IJIIIJJILlocal/lito381/server/BattleGroundEffects;)V
 INVOKEINTERFACE java/util/List.add(Ljava/lang/Object;)Z itf
 POP
-GOTO L71
+GOTO L69
 NEW local/lito381/server/BattleGroundEffects$CastResult
 DUP
 CONST Integer:1
```

精確方法：`BattleTimedStatus#cancelMagicEffects()Llocal/lito381/server/BattleTimedStatus$MagicCancellation;`。

```diff
--- staged/BattleTimedStatus#cancelMagicEffects
+++ candidate/BattleTimedStatus#cancelMagicEffects
@@ -289,52 +289,54 @@
 CONST Integer:0
 PUTFIELD local/lito381/server/BattleTimedStatus.slowSkillId:I
 ALOAD 0
-DUP
-ASTORE 31
-CONST Long:0
+CONST Long:0
+LSTORE 31/32
+DUP
+ASTORE 33
+LLOAD 31/32
 PUTFIELD local/lito381/server/BattleTimedStatus.braveExpiresAtNanos:J
-ALOAD 31
-INVOKESTATIC local/lito381/server/NativeSourceIcons1Epoch.bump(Ljava/lang/Object;)V
-CONST Integer:0
-ISTORE 32
-ALOAD 0
-ASTORE 33
-ALOAD 0
-ILOAD 32
+ALOAD 33
+INVOKESTATIC local/lito381/server/NativeSourceIcons1Epoch.bump(Ljava/lang/Object;)V
+ALOAD 0
+CONST Integer:0
+ISTORE 34
+DUP
+ASTORE 35
+ILOAD 34
 PUTFIELD local/lito381/server/BattleTimedStatus.nativeSourceIcons1BraveSkill:I
-ALOAD 33
-INVOKESTATIC local/lito381/server/NativeSourceIcons1Epoch.bump(Ljava/lang/Object;)V
-CONST Integer:0
-ISTORE 34
-ALOAD 0
-ASTORE 35
-ALOAD 0
-ILOAD 34
+ALOAD 35
+INVOKESTATIC local/lito381/server/NativeSourceIcons1Epoch.bump(Ljava/lang/Object;)V
+ALOAD 0
+CONST Integer:0
+ISTORE 36
+DUP
+ASTORE 37
+ILOAD 36
 PUTFIELD local/lito381/server/BattleTimedStatus.braveMode:I
-ALOAD 35
+ALOAD 37
 INVOKESTATIC local/lito381/server/NativeSourceIcons1Epoch.bump(Ljava/lang/Object;)V
 ALOAD 0
 ALOAD 0
 GETFIELD local/lito381/server/BattleTimedStatus.blindExpiresAtNanos:J
 INVOKESTATIC local/lito381/server/NativeBlindSourceState.cleared(J)J
 PUTFIELD local/lito381/server/BattleTimedStatus.blindExpiresAtNanos:J
-CONST Long:0
-LSTORE 36/37
-ALOAD 0
-ASTORE 38
-ALOAD 0
-LLOAD 36/37
+ALOAD 0
+CONST Long:0
+LSTORE 38/39
+DUP
+ASTORE 40
+LLOAD 38/39
 PUTFIELD local/lito381/server/BattleTimedStatus.weaknessExpiresAtNanos:J
-ALOAD 38
-INVOKESTATIC local/lito381/server/NativeSourceIcons1Epoch.bump(Ljava/lang/Object;)V
-CONST Long:0
-LSTORE 39/40
-ALOAD 0
-ASTORE 41
-ALOAD 0
-LLOAD 39/40
+ALOAD 40
+INVOKESTATIC local/lito381/server/NativeSourceIcons1Epoch.bump(Ljava/lang/Object;)V
+ALOAD 0
+CONST Long:0
+LSTORE 41/42
+DUP
+ASTORE 43
+LLOAD 41/42
 PUTFIELD local/lito381/server/BattleTimedStatus.diseaseExpiresAtNanos:J
-ALOAD 41
+ALOAD 43
 INVOKESTATIC local/lito381/server/NativeSourceIcons1Epoch.bump(Ljava/lang/Object;)V
 ALOAD 0
 CONST Long:0
@@ -342,32 +344,32 @@
 ALOAD 0
 CONST Long:0
 PUTFIELD local/lito381/server/BattleTimedStatus.areaSilenceExpiresAtNanos:J
-CONST Long:0
-LSTORE 42/43
-ALOAD 0
-ASTORE 44
-ALOAD 0
-LLOAD 42/43
+ALOAD 0
+CONST Long:0
+LSTORE 44/45
+DUP
+ASTORE 46
+LLOAD 44/45
 PUTFIELD local/lito381/server/BattleTimedStatus.decayPotionExpiresAtNanos:J
-ALOAD 44
-INVOKESTATIC local/lito381/server/NativeSourceIcons1Epoch.bump(Ljava/lang/Object;)V
-CONST Long:0
-LSTORE 45/46
-ALOAD 0
-ASTORE 47
-ALOAD 0
-LLOAD 45/46
+ALOAD 46
+INVOKESTATIC local/lito381/server/NativeSourceIcons1Epoch.bump(Ljava/lang/Object;)V
+ALOAD 0
+CONST Long:0
+LSTORE 47/48
+DUP
+ASTORE 49
+LLOAD 47/48
 PUTFIELD local/lito381/server/BattleTimedStatus.polluteWaterExpiresAtNanos:J
-ALOAD 47
-INVOKESTATIC local/lito381/server/NativeSourceIcons1Epoch.bump(Ljava/lang/Object;)V
-CONST Long:0
-LSTORE 48/49
-ALOAD 0
-ASTORE 50
-ALOAD 0
-LLOAD 48/49
+ALOAD 49
+INVOKESTATIC local/lito381/server/NativeSourceIcons1Epoch.bump(Ljava/lang/Object;)V
+ALOAD 0
+CONST Long:0
+LSTORE 50/51
+DUP
+ASTORE 52
+LLOAD 50/51
 PUTFIELD local/lito381/server/BattleTimedStatus.strikerGaleExpiresAtNanos:J
-ALOAD 50
+ALOAD 52
 INVOKESTATIC local/lito381/server/NativeSourceIcons1Epoch.bump(Ljava/lang/Object;)V
 ALOAD 0
 CONST Long:0
@@ -378,32 +380,32 @@
 ALOAD 0
 CONST Long:0
 PUTFIELD local/lito381/server/BattleTimedStatus.wisdomPotionExpiresAtNanos:J
-CONST Long:0
-LSTORE 51/52
-ALOAD 0
-ASTORE 53
-ALOAD 0
-LLOAD 51/52
+ALOAD 0
+CONST Long:0
+LSTORE 53/54
+DUP
+ASTORE 55
+LLOAD 53/54
 PUTFIELD local/lito381/server/BattleTimedStatus.meditationExpiresAtNanos:J
-ALOAD 53
-INVOKESTATIC local/lito381/server/NativeSourceIcons1Epoch.bump(Ljava/lang/Object;)V
-CONST Long:0
-LSTORE 54/55
-ALOAD 0
-ASTORE 56
-ALOAD 0
-LLOAD 54/55
+ALOAD 55
+INVOKESTATIC local/lito381/server/NativeSourceIcons1Epoch.bump(Ljava/lang/Object;)V
+ALOAD 0
+CONST Long:0
+LSTORE 56/57
+DUP
+ASTORE 58
+LLOAD 56/57
 PUTFIELD local/lito381/server/BattleTimedStatus.concentrationExpiresAtNanos:J
-ALOAD 56
-INVOKESTATIC local/lito381/server/NativeSourceIcons1Epoch.bump(Ljava/lang/Object;)V
-CONST Long:0
-LSTORE 57/58
-ALOAD 0
-ASTORE 59
-ALOAD 0
-LLOAD 57/58
+ALOAD 58
+INVOKESTATIC local/lito381/server/NativeSourceIcons1Epoch.bump(Ljava/lang/Object;)V
+ALOAD 0
+CONST Long:0
+LSTORE 59/60
+DUP
+ASTORE 61
+LLOAD 59/60
 PUTFIELD local/lito381/server/BattleTimedStatus.counterMagicExpiresAtNanos:J
-ALOAD 59
+ALOAD 61
 INVOKESTATIC local/lito381/server/NativeSourceIcons1Epoch.bump(Ljava/lang/Object;)V
 ALOAD 0
 CONST Long:0
@@ -418,13 +420,9 @@
 CONST Integer:0
 PUTFIELD local/lito381/server/BattleTimedStatus.poisonSilence:Z
 ALOAD 0
-ASTORE 60
-CONST Long:0
-LSTORE 61/62
+CONST Long:0
 ALOAD 0
 INVOKESTATIC local/lito381/server/NativeAcceptedState.sleepChanged(Llocal/lito381/server/BattleTimedStatus;)V
-ALOAD 60
-LLOAD 61/62
 PUTFIELD local/lito381/server/BattleTimedStatus.sleepExpiresAtNanos:J
 ALOAD 0
 CONST Long:0
@@ -435,44 +433,44 @@
 ALOAD 0
 CONST Long:0
 PUTFIELD local/lito381/server/BattleTimedStatus.burningSpiritExpiresAtNanos:J
-CONST Long:0
-LSTORE 63/64
-ALOAD 0
-ASTORE 65
-ALOAD 0
-LLOAD 63/64
+ALOAD 0
+CONST Long:0
+LSTORE 62/63
+DUP
+ASTORE 64
+LLOAD 62/63
 PUTFIELD local/lito381/server/BattleTimedStatus.elementalFireExpiresAtNanos:J
-ALOAD 65
-INVOKESTATIC local/lito381/server/NativeSourceIcons1Epoch.bump(Ljava/lang/Object;)V
-CONST Long:0
-LSTORE 66/67
-ALOAD 0
-ASTORE 68
-ALOAD 0
-LLOAD 66/67
+ALOAD 64
+INVOKESTATIC local/lito381/server/NativeSourceIcons1Epoch.bump(Ljava/lang/Object;)V
+ALOAD 0
+CONST Long:0
+LSTORE 65/66
+DUP
+ASTORE 67
+LLOAD 65/66
 PUTFIELD local/lito381/server/BattleTimedStatus.soulOfFlameExpiresAtNanos:J
-ALOAD 68
-INVOKESTATIC local/lito381/server/NativeSourceIcons1Epoch.bump(Ljava/lang/Object;)V
-CONST Long:0
-LSTORE 69/70
-ALOAD 0
-ASTORE 71
-ALOAD 0
-LLOAD 69/70
+ALOAD 67
+INVOKESTATIC local/lito381/server/NativeSourceIcons1Epoch.bump(Ljava/lang/Object;)V
+ALOAD 0
+CONST Long:0
+LSTORE 68/69
+DUP
+ASTORE 70
+LLOAD 68/69
 PUTFIELD local/lito381/server/BattleTimedStatus.venomResistExpiresAtNanos:J
-ALOAD 71
+ALOAD 70
 INVOKESTATIC local/lito381/server/NativeSourceIcons1Epoch.bump(Ljava/lang/Object;)V
 ALOAD 0
 CONST Long:0
 PUTFIELD local/lito381/server/BattleTimedStatus.doubleBrakeExpiresAtNanos:J
-CONST Long:0
-LSTORE 72/73
-ALOAD 0
-ASTORE 74
-ALOAD 0
-LLOAD 72/73
+ALOAD 0
+CONST Long:0
+LSTORE 71/72
+DUP
+ASTORE 73
+LLOAD 71/72
 PUTFIELD local/lito381/server/BattleTimedStatus.horrorOfDeathExpiresAtNanos:J
-ALOAD 74
+ALOAD 73
 INVOKESTATIC local/lito381/server/NativeSourceIcons1Epoch.bump(Ljava/lang/Object;)V
 ALOAD 0
 CONST Long:0
```

## 可直接重跑的指令

將下列 Python 區塊存成 `scratch/issue-67/reproduce.py`，於本 worktree 根目錄執行。程式從 batches.json 生成完整 323 條清單，建置同版工具與註解、編譯 staged、轉換及檢查冪等性、打包並比對。ASM 9.7 以固定 SHA-256 驗證，優先讀取本機 Maven cache，缺少時下載到本 worktree target。

```powershell
Set-Location 'D:\lineage開發\3-lito開發\scratch\wt\task-67'
& 'C:/Users/CHAOREN/AppData/Local/Programs/Python/Python312/python.exe' -B -X utf8 scratch/issue-67/reproduce.py
# 目前預期 exit 2：#203 尚未合併，2 個 DIFF 尚未清零，不能當成通過。
```

```python
import hashlib
import json
import os
from pathlib import Path
import shutil
import subprocess
import urllib.request
import zipfile

root = Path.cwd()
server = root / 'lito伺服器/LiTo381-Server'
scratch = root / 'scratch/issue-67/replay'
target = server / 'target/issue-67/replay'
scratch.mkdir(parents=True, exist_ok=True)
target.mkdir(parents=True, exist_ok=True)
batch = next(b for b in json.loads((root / 'docs/combat-merge/batches.json').read_text('utf-8'))['batches'] if b['id'] == 'B06')
candidate = server / 'reference/candidate-50635f7e.jar'
formal = server / 'reference/base-0.6.71.jar'
assert hashlib.sha256(candidate.read_bytes()).hexdigest() == '50635f7edca8ec6ce77787cc32371c7590b497e48ea202f8a679caf3f517b729'
assert hashlib.sha256(formal.read_bytes()).hexdigest() == 'e1498871c7be6b287821c57ef5d95933db1740d9b4d4a80f6c3465f21d99d498'
jdk = Path(shutil.which('javac')).parent
suffix = '.exe' if os.name == 'nt' else ''
java = jdk / ('java' + suffix)
javac = jdk / ('javac' + suffix)

def run(args, name):
    result = subprocess.run([str(a) for a in args], stdout=subprocess.PIPE, stderr=subprocess.STDOUT)
    (scratch / (name + '.log')).write_bytes(result.stdout)
    print(name, 'exit', result.returncode)
    print(result.stdout.decode('utf-8', errors='replace'))
    assert result.returncode == 0, name

hashes = {
    'asm': 'adf46d5e34940bdf148ecdd26a9ee8eea94496a72034ff7141066b3eea5c4e9d',
    'asm-tree': '62f4b3bc436045c1acb5c3ba2d8ec556ec3369093d7f5d06c747eb04b56d52b1',
    'asm-analysis': '7bc6bcbc21379948a0c8c467fb0f864206e5b818f6bc0b546872f5c9f941556f',
}
jars = []
for name, expected in hashes.items():
    relative = f'org/ow2/asm/{name}/9.7/{name}-9.7.jar'
    jar = Path('D:/dev-cache/maven/repository') / relative
    if not jar.is_file():
        jar = target / (name + '-9.7.jar')
        if not jar.is_file():
            jar.write_bytes(urllib.request.urlopen('https://repo.maven.apache.org/maven2/' + relative).read())
    assert hashlib.sha256(jar.read_bytes()).hexdigest() == expected
    jars.append(str(jar))
asm_cp = os.pathsep.join(jars)
for name in ['as-record', 'semantic-diff']:
    output = target / name
    output.mkdir(exist_ok=True)
    sources = sorted((server / 'tools' / name / 'src/main/java').rglob('*.java'))
    run([javac, '-J-Dfile.encoding=UTF-8', '--release', '17', '-encoding', 'UTF-8', '-cp', asm_cp, '-d', output, *sources], 'build-' + name)
annotations = target / 'annotations'
annotations.mkdir(exist_ok=True)
source = server / 'src/main/java/local/lito381/server'
run([javac, '-J-Dfile.encoding=UTF-8', '--release', '17', '-encoding', 'UTF-8', '-d', annotations, source / 'AsRecord.java', source / 'AsmFinal.java'], 'annotations')
classes = target / 'classes'
classes.mkdir(exist_ok=True)
for path in classes.rglob('*.class'):
    path.unlink()
staged = server / 'merge-inputs/50635f7e/staged/local/lito381/server'
sources = [staged / (name + '.java') for name in batch['classes']]
cp = os.pathsep.join(map(str, [candidate, formal, server / 'lib/*', annotations]))
run([javac, '-J-Dfile.encoding=UTF-8', '--release', '17', '-encoding', 'UTF-8', '-cp', cp, '-d', classes, *sources], 'javac')
converter_cp = str(target / 'as-record') + os.pathsep + asm_cp
run([java, '-Dfile.encoding=UTF-8', '-cp', converter_cp, 'local.lito381.asrecord.AsRecord', classes], 'as-record')
entries = sorted(p.relative_to(classes).as_posix() for p in classes.rglob('*.class'))
missing = sorted(set(batch['verify_entries']) - set(entries))
extra = sorted(set(entries) - set(batch['verify_entries']))
assert not missing and not extra
print('entries=323 missing=[] extra=[]')
before = {entry: hashlib.sha256((classes / entry).read_bytes()).hexdigest() for entry in entries}
run([java, '-Dfile.encoding=UTF-8', '-cp', converter_cp, 'local.lito381.asrecord.AsRecord', classes], 'as-record-idempotent')
assert before == {entry: hashlib.sha256((classes / entry).read_bytes()).hexdigest() for entry in entries}
jar = target / 'staged.jar'
with zipfile.ZipFile(jar, 'w') as archive:
    for entry in entries:
        archive.writestr(zipfile.ZipInfo(entry, (1980, 1, 1, 0, 0, 0)), (classes / entry).read_bytes())
selection = scratch / 'verify_entries.txt'
selection.write_text('\n'.join(batch['verify_entries']) + '\n', encoding='utf-8')
selected = {e.removesuffix('.class') for e in batch['verify_entries']}
approval_scope = {}
for name in ['access-widen-approved', 'deviations', 'effects-approved', 'exceptions-exempt', 'extra-methods-approved', 'layout-approved', 'metadata-exempt', 't029-methods']:
    applicable = []
    for raw in (root / 'docs/combat-merge' / (name + '.txt')).read_text('utf-8-sig').splitlines():
        raw = raw.strip()
        if not raw or raw.startswith('#'):
            continue
        parts = raw.split()
        key = parts[1] if parts[0] in ('method-diff', 'ctor-prologue-order') else parts[0]
        if key.split('#')[0].removesuffix('.class') in selected:
            applicable.append(raw)
    approval_scope[name] = applicable
(scratch / 'approved-scope.json').write_text(json.dumps(approval_scope, indent=2), encoding='utf-8')
assert all(not entries for entries in approval_scope.values()), 'Approval lists changed; review and clip before running'
print('B06 applicable approval entries=0')
semantic_cp = str(target / 'semantic-diff') + os.pathsep + asm_cp
run([java, '-Dfile.encoding=UTF-8', '-Xmx2g', '-cp', semantic_cp, 'local.lito381.semanticdiff.SemanticDiff', '--left', jar, '--right', candidate, '--left-classpath', candidate, '--classes', selection, '--json', scratch / 'semantic.json', '--md', scratch / 'semantic.md'], 'semantic')
data = json.loads((scratch / 'semantic.json').read_text('utf-8'))
record_entries = [r['entry'][:-6] for r in json.loads((root / 'docs/combat-merge/record-asm-fields.json').read_text('utf-8'))['records'] if r['class'].split('/')[-1].split('$')[0] in batch['classes']]
assert len(record_entries) == 14
assert all(c['status'] == 'SAME' for c in data['classes'] if c['name'] in record_entries)
assert data['classSelection']['missingLeft'] == [] and data['classSelection']['extraLeft'] == []
receipt = {'counts': data['counts'], 'recordSame': 14, 'entries': len(entries), 'jarSha256': hashlib.sha256(jar.read_bytes()).hexdigest(), 'sources': {p.name: hashlib.sha256(p.read_bytes()).hexdigest() for p in sources}}
(scratch / 'receipt.json').write_text(json.dumps(receipt, indent=2), encoding='utf-8')
print(json.dumps(receipt, ensure_ascii=False))
if len(data['classes']) != 323 or any(c['status'] not in {'SAME', 'SAME_EFFECTS', 'SAME_PATHS', 'SAME_CFG'} for c in data['classes']):
    raise SystemExit(2)
```

### 完整差異診斷重現

將以下 Java 存成 `scratch/issue-67/CurrentMethods.java`。編譯依賴沿用上段 `asm_cp`，工具 classes 為 `target/issue-67/replay/semantic-diff`，診斷 classes 另放 `target/issue-67/replay/diagnostic`；此診斷只讀驗收 JAR，不寫入 B06 classes。

```java
package local.lito381.semanticdiff;
import java.nio.file.*;
import java.util.*;
import org.objectweb.asm.tree.*;
public class CurrentMethods {
    public static void main(String[] args) throws Exception {
        JarIndex left = JarIndex.load(Path.of(args[0]));
        JarIndex right = JarIndex.load(Path.of(args[1]));
        left.addClasspath(Path.of(args[1]));
        Path output = Path.of(args[2]);
        Files.createDirectories(output);
        Map<String,String> selected = Map.of(
            "BattleGroundEffects", "cast", "BattleTimedStatus", "cancelMagicEffects",
            "BattleQuestEncounter", "pollPoisons", "BattleTimeAttackEncounter", "pollPoisons",
            "BattleCubeHarmonyEffects", "advance");
        for (var entry : new TreeMap<>(selected).entrySet()) {
            String name = "local/lito381/server/" + entry.getKey();
            ClassNode a = left.readTree(name), b = right.readTree(name);
            for (MethodNode m : a.methods) {
                if (!m.name.equals(entry.getValue())) continue;
                MethodNode other = b.methods.stream().filter(n -> n.name.equals(m.name) && n.desc.equals(m.desc)).findFirst().orElseThrow();
                var diff = ClassComparer.compareMethod(m.name + m.desc, m, other, left, right, false);
                String key = entry.getKey() + "#" + m.name;
                System.out.println(key + m.desc + " " + (diff == null ? "SAME" : diff.detail));
                for (int side = 0; side < 2; side++) {
                    MethodNode method = side == 0 ? m : other;
                    JarIndex jar = side == 0 ? left : right;
                    List<String> lines = new ArrayList<>();
                    lines.add("METHOD " + method.name + method.desc);
                    lines.add("ACCESS " + method.access + " SIGNATURE " + method.signature + " EXCEPTIONS " + method.exceptions);
                    lines.addAll(ClassComparer.instructions(method, jar));
                    lines.add("TRY " + ClassComparer.tryCatch(method, jar));
                    Files.write(output.resolve(key + (side == 0 ? "-left.txt" : "-right.txt")), lines);
                }
            }
        }
    }
}
```

以下 Python 接在重現程式的變數初始化與工具建置後執行（或在完整程式最後驗收 exit 之前插入），會輸出五個精確方法的判定與完整左右指令：

```python
diagnostic = target / 'diagnostic'
diagnostic.mkdir(exist_ok=True)
run([javac, '--release', '17', '-encoding', 'UTF-8', '-cp', semantic_cp,
     '-d', diagnostic, root / 'scratch/issue-67/CurrentMethods.java'], 'diagnostic-build')
run([java, '-Dfile.encoding=UTF-8', '-Xmx2g', '-cp', str(diagnostic) + os.pathsep + semantic_cp,
     'local.lito381.semanticdiff.CurrentMethods', jar, candidate, scratch / 'methods'], 'diagnostic')
```

## 產出路徑

- 交付報告：`docs/reports/issue-67.md`。
- 保留的 29 個來源：`lito伺服器/LiTo381-Server/merge-inputs/50635f7e/staged/local/lito381/server/`，精確範圍見 B06.classes。
- 本輪本機證據：`scratch/issue-67/replay/{semantic.json,semantic.md,verify_entries.txt,receipt.json,approved-scope.json}`、各步驟 `.log` 與 `methods/` 完整左右差異。
- 本輪產物：`lito伺服器/LiTo381-Server/target/issue-67/replay/{classes,staged.jar,as-record,semantic-diff,annotations}`。生成檔不提交。

## 推測

主控預期 #203 的 CFG 比較可消除剩餘兩項 DIFF；此預期尚未以合併後工具驗證。不能把「cycle／path limit」解讀為行為相同，也不宣稱 javac 不可表達。

## 待驗證

- 2 個 DIFF 尚未清零，B06 尚未完成；主控已裁定等待 #203，不再請求是否改 Java 的裁定。須待 PR #215 合併後，用合併後工具重驗全部 323 entries，分列 SAME／SAME_EFFECTS／SAME_PATHS／SAME_CFG，且 DIFF／ONLY_LEFT／ONLY_RIGHT／missing／extra 均為零。
- 單元測試與回歸依 §6-5 不適用於本次 staged 任務，留待 B-land 統一執行。未執行伺服器、登入器、遊戲、正式 DB 或部署。
- javac 的 unchecked 警告仍存在，編譯成功不代表完成行為回歸。一般 Maven CI 也不能替代本批驗收。

## 給下一棒的提示

請調度器／Claude 主控先確認 #203（PR #215）已合併，再派本機執行器重驗；目前是依賴阻塞，不能將本輪提交判為 done 或重新送審驗收完成。#186 已在本輪 main，#203 仍未合併。Java、14 個 `@AsRecord`、7 個 `@AsmFinal` 與 4096 上限維持不變；不新增豁免、不覆蓋 class bytes、不使用未合併工具代替驗收。

依賴合併後先 merge 最新 main，以本報告重現程式重建全部 323 entries，更新逐 entry／逐方法結果及末段交接。只有新 CFG 工具仍判 DIFF 時，才依最新裁定附完整方法差異並加 `needs:claude`；目前不把已裁定的兩個方法重新交付相同問題。三個原指定迴圈方法已通過，無須改寫。單元測試與回歸由 B-land 統一處理；本輪不合併 PR、不關閉 Issue。
