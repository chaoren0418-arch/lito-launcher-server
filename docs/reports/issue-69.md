# Issue #69 — T020-B08 戰鬥併入暫存

## 結論

**未通過驗收，待 `needs:claude` 裁定及後續修補，不可交付 B-land。**

已新增 B08 的 30 個 staged Java 檔，可用 JDK 17 編譯；轉換、打包後恰好為清單的 160 個 entry，沒有清單外產物。最終結果為 **SAME 136、SAME_EFFECTS 0、SAME_PATHS 8、DIFF 16、ONLY_LEFT 0、ONLY_RIGHT 0**。本 Issue 只允許 SAME／SAME_EFFECTS，故 8 個 SAME_PATHS 亦未計入驗收通過。

分支：`task/69`。起點：`c724539`。本輪沒有切換／建立分支。

## 已驗證

- 依根目錄 AGENTS.md、伺服器 AGENTS.md、docs/REPORT.md、TASKS.md、設計 §6-4／§6-5 與 B08 清單作業。
- 以 CFR 0.152 反編譯兩個 reference JAR 的全部 30 個外層類別，保存正式／候選差異；11 個原始碼類別以 src/main 複本移植，19 個 DECOMPILED 類別以候選反編譯版修正 Java 型別及控制流程。`LegacyMySqlRepository` 從 `Accounts.java` 抽出，原檔未改。
- 五個帶額外 ASM 欄位的 record 均使用 `@AsRecord`：`ClassicWorldMonsterPopulation$Snapshot`、`NativeCombatAttacker$Ref`、`NativeLightAction$Ingress`、`NativePlayerCommit$DefenseVisual`、`NativeProcSkillCatalog$Catalog`。另以相同轉換器表達一般 record `NativePlayerCommit$Event`，所以轉換輸出為 converted=6。
- `NativeLightAction.Output` 為 package-private；比對套用 #176 已核准的精確條目，該 entry 為 SAME。其他結構／方法沒有新增核准豁免。
- 全部 staged 檔沒有 CFR 的 `Decompilation failed` 佔位方法、`** GOTO` 或未完成 monitor 偽碼。
- 編譯與比對均為離線 class 處理；未啟動伺服器、登入器或遊戲，未連 DB。
- 原始專案只讀；未修改 src/main、pom.xml、docs/REPORT.md、正式 JAR、機密設定或封存資源。

### 指令及輸出

下列路徑以伺服器目錄為基準；`<repo>` 是本 worktree。Python 輔助腳本僅在 `<repo>/scratch/issue-69/`，未提交。JDK 使用 `C:/Program Files/Microsoft/jdk-17.0.17.10-hotspot/bin`，不是 PATH 上的另一套 java。

```text
javac -J-Dfile.encoding=UTF-8 --release 17 -encoding UTF-8
  -cp "<ASM 9.7 asm.jar;asm-tree.jar;asm-analysis.jar>"
  -d target/issue-69/tools <tools/semantic-diff/src/main/java/**/*.java> <tools/as-record/src/main/java/**/*.java>

javac -J-Dfile.encoding=UTF-8 --release 17 -encoding UTF-8
  -d target/issue-69/annotations
  src/main/java/local/lito381/server/AsRecord.java src/main/java/local/lito381/server/AsmFinal.java

javac -J-Dfile.encoding=UTF-8 --release 17 -encoding UTF-8
  -cp "reference/candidate-50635f7e.jar;reference/base-0.6.71.jar;lib/*;target/issue-69/annotations;D:/dev-cache/maven/repository/com/zaxxer/HikariCP/5.1.0/HikariCP-5.1.0.jar"
  -d target/issue-69/classes @<repo>/scratch/issue-69/javac.args

退出碼 0
Note: Some input files use unchecked or unsafe operations.
Note: Recompile with -Xlint:unchecked for details.

java -cp "target/issue-69/tools;<ASM classpath>" local.lito381.asrecord.AsRecord target/issue-69/classes
as-record: scanned=160 converted=6
退出碼 0

# 將 classes 內所有 .class 打包為 target/issue-69/b08.jar；不放入 annotations 或工具 classes。
java -Xmx2g -cp "target/issue-69/tools;<ASM classpath>" local.lito381.semanticdiff.SemanticDiff
  --left target/issue-69/b08.jar
  --left-classpath reference/candidate-50635f7e.jar
  --right reference/candidate-50635f7e.jar
  --classes <repo>/scratch/issue-69/entries.txt
  --access-widen <repo>/scratch/issue-69/access-widen-approved.txt
  --json <repo>/scratch/issue-69/semantic.json
  --md <repo>/scratch/issue-69/semantic.md

SUMMARY SAME=136 SAME_EFFECTS=0 SAME_PATHS=8 ORPHAN_IGNORED=0 SAME_LAYOUT=0 DEVIATION=0 DIFF=16 ONLY_LEFT=0 ONLY_RIGHT=0 BYTE_SAME=1 ALLOW_METHODS=0
classSelection.missingLeft=[]
classSelection.extraLeft=[]
退出碼 0（僅代表工具正常完成，不代表驗收通過）
```

`javac.args` 是 batches.json 的 B08.classes 對應 30 個 staged 絕對路徑，每行加雙引號、UTF-8 無 BOM；`entries.txt` 直接逐行寫入 B08.verify_entries。最後的工具診斷編譯也使用同一個 argfile 名稱，因此要重跑批次請由 check.py 重新產生，不可盲用最後留下的 argfile。

worktree 沒有 lib/，因此編譯額外引用既有 Maven 快取的 HikariCP；未建立 stub。AsRecord／AsmFinal 註解先編在獨立目錄，沒有混入 B08 輸出。ASM 9.7 的 asm／asm-tree 從既有 Maven 快取讀取，asm-analysis 放在本 worktree `target/issue-69/asm-analysis-9.7.jar`。

核准清單依工具規則裁切到本批；`access-widen-approved.txt` 只有原清單的 `local/lito381/server/NativeLightAction$Output # approved: https://github.com/chaoren0418-arch/lito-launcher-server/issues/64#issuecomment-6654963452`。直接傳整份原清單會因 NativeDeDeletionState 不在本批而失敗。effects／layout／deviations／extra-methods／metadata-exempt／exceptions-exempt 沒有 B08 適用條目，沒有自行新增豁免。左側 classpath 只提供依賴類別的欄位／繼承解析，不會把候選 class 複製進左側輸出。

### 輸入及產物 SHA-256

- `reference/base-0.6.71.jar`：`e1498871c7be6b287821c57ef5d95933db1740d9b4d4a80f6c3465f21d99d498`
- `reference/candidate-50635f7e.jar`：`50635f7edca8ec6ce77787cc32371c7590b497e48ea202f8a679caf3f517b729`
- `target/issue-69/b08.jar`：`43dcfd88621088d420c5288ef28ad16df5fc14166631d977e1e0d4a6df8fcc6f`

## 需要主控裁定

1. **checked exception 表達差異**：候選 ASM 可在沒有 throws 的方法中直接呼叫 checked-throws 方法，但 Java 編譯器不允許。`NativeLightAction$Batch.nativeSourceBatchOriginalEmit` 呼叫 `NativeSourceTargetIconBatch.rescue`（IOException），`emit` 呼叫 `finish`（Throwable），最後由 Runnable.run 進入。staged 為前者增加 IOException，後者用 `NativeLightAction.rethrowCandidateFailure` 泛型重拋原物件，未換成其他例外型別；新增 catch／helper 仍由工具列 DIFF，沒有隱藏。請裁定是否擴充來源表達轉換器，或為精確方法／helper 訂核准規則。`LegacyMySqlRepository.healthCheck`、`tournamentProfiles`、`persistTournamentProfile` 呼叫 SharedDatabase 的 Exception 方法，staged 需以 `throws Exception` 表達，候選卻只有 SQLException，亦待精確裁定。
2. **@AsRecord 呼叫端 owner**：`NativeCombatAttacker.calculated` 呼叫 `Ref.equals`。javac 編譯標註 class 時 equals 尚未由轉換器生成，所以 owner 是 `java/lang/Object`；候選 owner 是 `NativeCombatAttacker$Ref`。轉換器只生成 record 方法，未改寫呼叫端；現有比較器不將兩種 invokevirtual owner 視為相同。不可在來源手寫 equals，因 AsRecord 明確拒絕覆寫這些方法。需主控決定轉換器補上精確重綁或比對規則。
3. **SAME_PATHS 與本批驗收文字不一致**：#176 工具回報的逐路徑成功狀態獨立於 SAME。這輪有 8 個類別屬此狀態，未擅自算通過，請裁定是否允許。

其餘 DIFF 仍需續修。部分與 ASM 留在 operand stack 的值、CFR 暫存／cast、分支排列、lambda handle 及例外保護範圍有關；**不能只憑這些診斷宣稱全部語意等價或全部屬工具誤報**。下方附完整方法級診斷與正規化差異，沒有只附第一個差異。

## 推測

- 原樣重拋 helper 預期保留 Throwable 物件與型別，但新增方法／catch 影響指令結構，尚未符合本批契約。
- 對 AsRecord 生成的 equals 使用 Object owner，預期仍依 JVM 虛擬派送進入生成的 Ref.equals；此推論沒有取代語意驗收。
- 比對中遇到迴圈、4096 路徑上限、32768 表示式上限時，工具無法作出等價證明；這不是通過證據。

## 待驗證／未完成

- 16 個 DIFF entry 的完整對齊，以及 8 個 SAME_PATHS 的契約裁定。
- 本輪沒有單元測試／遊戲回歸，依設計應在 B-land 執行；不能將本報告當成遊戲行為驗證。
- 無法讀取 Issue #69 即時留言：GitHub connector 回覆 Unknown tool，gh 回覆 HTTP 401。五個 record 與 Output 的裁定以任務文字及已入庫文件為據。
- 已嘗試 `gh issue edit 69 --repo chaoren0418-arch/lito-launcher-server --add-label needs:claude`，HTTP 401；**標籤尚未加上**，需調度器／主控補標。

## 產出路徑及給下一棒的提示

- 交付原始碼：`lito伺服器/LiTo381-Server/merge-inputs/50635f7e/staged/local/lito381/server/`，只包含 B08 的 30 個 Java 檔。
- 交付報告：`docs/reports/issue-69.md`（本檔，含完整差異）。
- 本機重建腳本／反編譯／三方差異／編譯 log：`scratch/issue-69/`；Java class/JAR：`lito伺服器/LiTo381-Server/target/issue-69/`，均未入 git。
- 下一棒先由 Claude 主控處理以上三項裁定，再由本機執行器續修全部 DIFF，重跑完整 160 entry 比對。需要更改 converter／semantic-diff 時請另開工具任務，勿在 B08 範圍外修改。
- B03 可參考本批 package-private Output 來源，但 `NativeLightAction` 尚有 DIFF；不可將本輪視為已完成的 B08 編譯依賴交接。
- B-land 不得搬入本輪未驗收的 staged；使用者無需啟動遊戲驗收這一輪。

## 每個外層類別的結果

| 類別 | entry | SAME | SAME_PATHS | DIFF |
|---|---:|---:|---:|---:|
| ClassicWorldMonsterPopulation | 48 | 46 | 1 | 1 |
| LegacyInventoryWeightLoader | 1 | 1 | 0 | 0 |
| LegacyMySqlRepository | 15 | 14 | 0 | 1 |
| NativeAcceptedHpCompletion | 1 | 1 | 0 | 0 |
| NativeArmorProc | 3 | 3 | 0 | 0 |
| NativeArmorState | 1 | 0 | 1 | 0 |
| NativeBattleVisibility | 1 | 1 | 0 | 0 |
| NativeCancellationBattleRuntime | 5 | 5 | 0 | 0 |
| NativeCancellationNpcStatic | 4 | 3 | 0 | 1 |
| NativeCastMaterial | 4 | 4 | 0 | 0 |
| NativeCombatAttacker | 6 | 5 | 0 | 1 |
| NativeCombatOrigin | 6 | 5 | 1 | 0 |
| NativeDeItemDefinitionsSql | 5 | 3 | 0 | 2 |
| NativeEquipmentDefense | 3 | 2 | 0 | 1 |
| NativeLightAction | 5 | 3 | 0 | 2 |
| NativeMassTeleportAnswerLife | 2 | 1 | 1 | 0 |
| NativeNpcLifetime | 4 | 4 | 0 | 0 |
| NativePlayerActionScope | 4 | 3 | 1 | 0 |
| NativePlayerCommit | 8 | 5 | 2 | 1 |
| NativePlayerLifetime | 6 | 5 | 0 | 1 |
| NativePlayerVitalCompletions | 3 | 1 | 1 | 1 |
| NativeProcHealingCompanion | 7 | 6 | 0 | 1 |
| NativeProcHealingEncounter | 5 | 3 | 0 | 2 |
| NativeProcHealingPlayer | 2 | 1 | 0 | 1 |
| NativeProcSkillCatalog | 3 | 3 | 0 | 0 |
| NativeReturnNatureTransfer | 3 | 3 | 0 | 0 |
| NativeReuseActor | 1 | 1 | 0 | 0 |
| NativeReuseShutdown | 1 | 1 | 0 | 0 |
| NativeRolePublication | 2 | 2 | 0 | 0 |
| NativeSourcePlayerActionLock | 1 | 1 | 0 | 0 |

## 完整未通過項目的診斷與正規化差異

以下由本輪工具輸出產生。每個非 SAME entry 列全部 methodResults；其後的 unified diff 包含該類所有正規化仍不同的方法（含 metadata、完整 try 覆蓋表與指令），並非僅 firstDiff。lambda 以比較器的內容識別配對；左右獨有的方法完整列出。此診斷沒有修改或過濾驗收 JAR。

### ClassicWorldMonsterPopulation — DIFF

```json
{
  "where": "method",
  "member": "idleMove(IJLjava/util/Set;JII)Llocal/lito381/server/ClassicWorldMonsterPopulation$IdleMovement;",
  "insnIndex": 78,
  "detail": "left=DUP right=ASTORE 10; paths rejected: normal control-flow cycle"
}
```
<details>
<summary>全部方法診斷</summary>

```json
[
  {
    "method": "damage(IJLlocal/lito381/server/ClassicWorldMonsterPopulation$Contribution;I)Llocal/lito381/server/ClassicWorldMonsterPopulation$Hit;",
    "status": "SAME_PATHS",
    "detail": "path multisets equal (528)"
  },
  {
    "method": "idleMove(IJLjava/util/Set;JII)Llocal/lito381/server/ClassicWorldMonsterPopulation$IdleMovement;",
    "status": "DIFF",
    "detail": "method idleMove(IJLjava/util/Set;JII)Llocal/lito381/server/ClassicWorldMonsterPopulation$IdleMovement; insn[78]: left=DUP right=ASTORE 10; paths rejected: normal control-flow cycle"
  },
  {
    "method": "itemAction(IJLjava/util/List;Ljava/util/Set;JI)Llocal/lito381/server/ClassicWorldMonsterPopulation$ItemAction;",
    "status": "DIFF",
    "detail": "method itemAction(IJLjava/util/List;Ljava/util/Set;JI)Llocal/lito381/server/ClassicWorldMonsterPopulation$ItemAction; insn[200]: left=IF_ICMPGE L330 right=IF_ICMPGE L331; paths rejected: normal control-flow cycle"
  },
  {
    "method": "nativeCancellationSpawn(IJLlocal/lito381/server/ClassicWorldMonsterCatalog$MonsterTemplate;)Llocal/lito381/server/ClassicWorldMonsterPopulation$Snapshot;",
    "status": "DIFF",
    "detail": "method nativeCancellationSpawn(IJLlocal/lito381/server/ClassicWorldMonsterCatalog$MonsterTemplate;)Llocal/lito381/server/ClassicWorldMonsterPopulation$Snapshot; insn[43]: left=DUP right=GETFIELD local/lito381/server/ClassicWorldMonsterPopulation.nextQuestDynamicObjectId:J; path event multisets differ (left=37, right=37)"
  },
  {
    "method": "nativeStaticCancellationSpawn(IIIIILlocal/lito381/server/ClassicWorldMonsterCatalog$MonsterTemplate;)Llocal/lito381/server/ClassicWorldMonsterPopulation$Snapshot;",
    "status": "DIFF",
    "detail": "method nativeStaticCancellationSpawn(IIIIILlocal/lito381/server/ClassicWorldMonsterCatalog$MonsterTemplate;)Llocal/lito381/server/ClassicWorldMonsterPopulation$Snapshot; insn[23]: left=DUP right=GETFIELD local/lito381/server/ClassicWorldMonsterPopulation.nextQuestDynamicObjectId:J; path event multisets differ (left=27, right=27)"
  },
  {
    "method": "npcPoison(IJLlocal/lito381/server/ClassicWorldMonsterPopulation$ContributorKind;JJLlocal/lito381/server/ClassicWorldMonsterPoisonAttack$Kind;J)Llocal/lito381/server/ClassicWorldMonsterPopulation$NpcPoison;",
    "status": "DIFF",
    "detail": "method npcPoison(IJLlocal/lito381/server/ClassicWorldMonsterPopulation$ContributorKind;JJLlocal/lito381/server/ClassicWorldMonsterPoisonAttack$Kind;J)Llocal/lito381/server/ClassicWorldMonsterPopulation$NpcPoison; insn[61]: left=IF_ACMPEQ L99 right=IF_ACMPEQ L94; path event multisets differ (left=150, right=150)"
  },
  {
    "method": "pollParalyses(IJ)Ljava/util/List;",
    "status": "DIFF",
    "detail": "method pollParalyses(IJ)Ljava/util/List; insn[28]: left=IF_ICMPNE L80 right=IF_ICMPNE L17; paths rejected: normal control-flow cycle"
  },
  {
    "method": "pollPoisons(IJII)Ljava/util/List;",
    "status": "DIFF",
    "detail": "method pollPoisons(IJII)Ljava/util/List; insn[33]: left=IF_ICMPNE L138 right=IF_ICMPNE L22; paths rejected: normal control-flow cycle"
  },
  {
    "method": "returnGuardHome(IJLjava/util/Set;J)Llocal/lito381/server/ClassicWorldMonsterPopulation$GuardHomecoming;",
    "status": "DIFF",
    "detail": "method returnGuardHome(IJLjava/util/Set;J)Llocal/lito381/server/ClassicWorldMonsterPopulation$GuardHomecoming; insn[74]: left=IFLT L238 right=IFLT L237; paths rejected: normal control-flow cycle"
  }
]
```

</details>
<details>
<summary>完整正規化差異</summary>

```diff
--- staged/ClassicWorldMonsterPopulation/damage-ccc748ca.txt
+++ candidate/ClassicWorldMonsterPopulation/damage-ccc748ca.txt
@@ -17 +17 @@
-IFNULL L48
+IFNULL L41
@@ -22 +22 @@
-IF_ICMPNE L48
+IF_ICMPNE L41
@@ -25 +25 @@
-IFEQ L48
+IFEQ L41
@@ -28 +28 @@
-IFEQ L48
+IFEQ L41
@@ -32,6 +32,3 @@
-IFGT L25
-GOTO L48
-ALOAD 6
-ASTORE 7
-ALOAD 7
-ALOAD 7
+IFLE L41
+ALOAD 6
+DUP
@@ -41,6 +38,3 @@
-IF_ICMPNE L34
-GOTO L48
-ALOAD 6
-ASTORE 8
-ALOAD 8
-ALOAD 8
+IF_ICMPEQ L41
+ALOAD 6
+DUP
@@ -50 +44 @@
-IF_ICMPEQ L48
+IF_ICMPEQ L41
@@ -55,2 +49 @@
-IFNE L48
-GOTO L50
+IFEQ L43
@@ -61 +54 @@
-ASTORE 9
+ASTORE 7
@@ -67 +60 @@
-ISTORE 10
+ISTORE 8
@@ -71 +64 @@
-ILOAD 10
+ILOAD 8
@@ -82 +75 @@
-IFNULL L136
+IFNULL L129
@@ -85 +78 @@
-LSTORE 11/12
+LSTORE 9/10
@@ -88,2 +81,2 @@
-IFNE L93
-LLOAD 11/12
+IFNE L86
+LLOAD 9/10
@@ -98 +91 @@
-LSTORE 11/12
+LSTORE 9/10
@@ -111 +104 @@
-ASTORE 13
+ASTORE 11
@@ -114,2 +107,2 @@
-ALOAD 13
-LLOAD 11/12
+ALOAD 11
+LLOAD 9/10
@@ -125 +118 @@
-LLOAD 11/12
+LLOAD 9/10
@@ -140 +133 @@
-IF_ACMPNE L136
+IF_ACMPNE L129
@@ -147 +140 @@
-IFNE L141
+IFNE L134
@@ -149,3 +142,3 @@
-GOTO L142
-CONST Integer:0
-ISTORE 14
+GOTO L135
+CONST Integer:0
+ISTORE 12
@@ -153,3 +146,3 @@
-ASTORE 15
-ILOAD 14
-IFEQ L246
+ASTORE 13
+ILOAD 12
+IFEQ L239
@@ -163 +156 @@
-ASTORE 15
+ASTORE 13
@@ -257 +250 @@
-ALOAD 9
+ALOAD 7
@@ -260 +253 @@
-ILOAD 10
+ILOAD 8
@@ -262,2 +255,2 @@
-ILOAD 14
-ALOAD 15
+ILOAD 12
+ALOAD 13
--- staged/ClassicWorldMonsterPopulation/idleMove-eb9151bf.txt
+++ candidate/ClassicWorldMonsterPopulation/idleMove-eb9151bf.txt
@@ -87 +86,0 @@
-DUP
@@ -89,6 +88,5 @@
-ASTORE 11
-ALOAD 10
-IFNULL L135
-ALOAD 10
-ALOAD 9
-IF_ACMPEQ L135
+ALOAD 10
+IFNULL L133
+ALOAD 10
+ALOAD 9
+IF_ACMPEQ L133
@@ -97 +95 @@
-IFEQ L135
+IFEQ L133
@@ -100 +98 @@
-IFEQ L135
+IFEQ L133
@@ -105 +103 @@
-IF_ICMPNE L135
+IF_ICMPNE L133
@@ -118,0 +117,23 @@
+ISTORE 11
+ILOAD 11
+CONST Integer:2
+IF_ICMPLE L117
+ALOAD 9
+GETFIELD local/lito381/server/ClassicWorldMonsterPopulation$Actor.damagePoisonStatus:Llocal/lito381/server/BattleTimedStatus;
+LLOAD 5/6
+INVOKEVIRTUAL local/lito381/server/BattleTimedStatus.blindActive(J)Z
+IFEQ L122
+ALOAD 9
+LLOAD 5/6
+INVOKESTATIC local/lito381/server/ClassicWorldMonsterPopulation.deferIdle(Llocal/lito381/server/ClassicWorldMonsterPopulation$Actor;J)V
+CONST null
+ARETURN
+ALOAD 9
+GETFIELD local/lito381/server/ClassicWorldMonsterPopulation$Actor.x:I
+ALOAD 9
+GETFIELD local/lito381/server/ClassicWorldMonsterPopulation$Actor.y:I
+ALOAD 10
+GETFIELD local/lito381/server/ClassicWorldMonsterPopulation$Actor.x:I
+ALOAD 10
+GETFIELD local/lito381/server/ClassicWorldMonsterPopulation$Actor.y:I
+INVOKESTATIC local/lito381/server/ClassicWorldMonsterPopulation.heading(IIII)I
@@ -120,24 +141 @@
-ILOAD 12
-CONST Integer:2
-IF_ICMPLE L119
-ALOAD 9
-GETFIELD local/lito381/server/ClassicWorldMonsterPopulation$Actor.damagePoisonStatus:Llocal/lito381/server/BattleTimedStatus;
-LLOAD 5/6
-INVOKEVIRTUAL local/lito381/server/BattleTimedStatus.blindActive(J)Z
-IFEQ L124
-ALOAD 9
-LLOAD 5/6
-INVOKESTATIC local/lito381/server/ClassicWorldMonsterPopulation.deferIdle(Llocal/lito381/server/ClassicWorldMonsterPopulation$Actor;J)V
-CONST null
-ARETURN
-ALOAD 9
-GETFIELD local/lito381/server/ClassicWorldMonsterPopulation$Actor.x:I
-ALOAD 9
-GETFIELD local/lito381/server/ClassicWorldMonsterPopulation$Actor.y:I
-ALOAD 10
-GETFIELD local/lito381/server/ClassicWorldMonsterPopulation$Actor.x:I
-ALOAD 10
-GETFIELD local/lito381/server/ClassicWorldMonsterPopulation$Actor.y:I
-INVOKESTATIC local/lito381/server/ClassicWorldMonsterPopulation.heading(IIII)I
-ISTORE 13
-GOTO L228
+GOTO L226
@@ -146 +144 @@
-IFNE L161
+IFNE L159
@@ -149 +147 @@
-IFLE L143
+IFLE L141
@@ -151 +149 @@
-GOTO L149
+GOTO L147
@@ -161 +159 @@
-IFLT L155
+IFLT L153
@@ -163 +161 @@
-GOTO L159
+GOTO L157
@@ -169 +167 @@
-GOTO L167
+GOTO L165
@@ -179 +177 @@
-IF_ICMPLT L176
+IF_ICMPLT L174
@@ -189 +187 @@
-IF_ICMPNE L190
+IF_ICMPNE L188
@@ -194 +192 @@
-IF_ICMPNE L190
+IF_ICMPNE L188
@@ -197,2 +195,2 @@
-ISTORE 13
-GOTO L228
+ISTORE 12
+GOTO L226
@@ -212 +210 @@
-DSTORE 14/15
+DSTORE 13/14
@@ -217,2 +215,2 @@
-IFEQ L218
-DLOAD 14/15
+IFEQ L216
+DLOAD 13/14
@@ -221 +219 @@
-IFLT L218
+IFLT L216
@@ -236,3 +234,3 @@
-ISTORE 13
-ILOAD 13
-IFGE L235
+ISTORE 12
+ILOAD 12
+IFGE L233
@@ -248 +246 @@
-ILOAD 13
+ILOAD 12
@@ -252 +250 @@
-ILOAD 13
+ILOAD 12
@@ -260 +258 @@
-ILOAD 13
+ILOAD 12
@@ -268 +266 @@
-ILOAD 13
+ILOAD 12
@@ -276 +274 @@
-ILOAD 13
+ILOAD 12
@@ -284 +282 @@
-ILOAD 13
+ILOAD 12
@@ -292 +290 @@
-ILOAD 13
+ILOAD 12
@@ -300 +298 @@
-ILOAD 13
+ILOAD 12
@@ -305,0 +304,3 @@
+ASTORE 15
+ILOAD 1
+INVOKESTATIC local/lito381/server/ClassicWorldMapCatalog.forMap(I)Llocal/lito381/server/ClassicWorldMapCatalog$ClassicMap;
@@ -307,2 +308 @@
-ILOAD 1
-INVOKESTATIC local/lito381/server/ClassicWorldMapCatalog.forMap(I)Llocal/lito381/server/ClassicWorldMapCatalog$ClassicMap;
+ALOAD 15
@@ -310,3 +310 @@
-ALOAD 16
-ASTORE 18
-ALOAD 18
+ALOAD 17
@@ -313,0 +312,2 @@
+ISTORE 18
+CONST Integer:0
@@ -315 +315,6 @@
-CONST Integer:0
+ILOAD 19
+ILOAD 18
+IF_ICMPGE L428
+ALOAD 17
+ILOAD 19
+IALOAD
@@ -317,4 +322,3 @@
-ILOAD 20
-ILOAD 19
-IF_ICMPGE L429
-ALOAD 18
+ALOAD 9
+GETFIELD local/lito381/server/ClassicWorldMonsterPopulation$Actor.x:I
+GETSTATIC local/lito381/server/ClassicWorldMonsterPopulation.HEADING_X:[I
@@ -322,0 +327 @@
+IADD
@@ -325,2 +330,9 @@
-GETFIELD local/lito381/server/ClassicWorldMonsterPopulation$Actor.x:I
-GETSTATIC local/lito381/server/ClassicWorldMonsterPopulation.HEADING_X:[I
+GETFIELD local/lito381/server/ClassicWorldMonsterPopulation$Actor.y:I
+GETSTATIC local/lito381/server/ClassicWorldMonsterPopulation.HEADING_Y:[I
+ILOAD 20
+IALOAD
+IADD
+ISTORE 22
+NEW local/lito381/server/ClassicWorldMonsterPopulation$Cell
+DUP
+ILOAD 1
@@ -328,6 +340,8 @@
-IALOAD
-IADD
-ISTORE 22
-ALOAD 9
-GETFIELD local/lito381/server/ClassicWorldMonsterPopulation$Actor.y:I
-GETSTATIC local/lito381/server/ClassicWorldMonsterPopulation.HEADING_Y:[I
+ILOAD 22
+INVOKESPECIAL local/lito381/server/ClassicWorldMonsterPopulation$Cell.<init>(III)V
+ASTORE 23
+ALOAD 16
+ALOAD 9
+GETFIELD local/lito381/server/ClassicWorldMonsterPopulation$Actor.x:I
+ALOAD 9
+GETFIELD local/lito381/server/ClassicWorldMonsterPopulation$Actor.y:I
@@ -335,6 +348,0 @@
-IALOAD
-IADD
-ISTORE 23
-NEW local/lito381/server/ClassicWorldMonsterPopulation$Cell
-DUP
-ILOAD 1
@@ -342,10 +349,0 @@
-ILOAD 23
-INVOKESPECIAL local/lito381/server/ClassicWorldMonsterPopulation$Cell.<init>(III)V
-ASTORE 24
-ALOAD 17
-ALOAD 9
-GETFIELD local/lito381/server/ClassicWorldMonsterPopulation$Actor.x:I
-ALOAD 9
-GETFIELD local/lito381/server/ClassicWorldMonsterPopulation$Actor.y:I
-ILOAD 22
-ILOAD 23
@@ -353,3 +351,3 @@
-IFEQ L427
-ALOAD 0
-ALOAD 24
+IFEQ L426
+ALOAD 0
+ALOAD 23
@@ -357 +355 @@
-IFNE L427
+IFNE L426
@@ -360,0 +359 @@
+ILOAD 21
@@ -362 +360,0 @@
-ILOAD 23
@@ -365,2 +363,3 @@
-IFNE L427
-ILOAD 1
+IFNE L426
+ILOAD 1
+ILOAD 21
@@ -368 +366,0 @@
-ILOAD 23
@@ -371 +369,27 @@
-IFNE L427
+IFEQ L362
+GOTO L426
+ALOAD 9
+INVOKEVIRTUAL local/lito381/server/ClassicWorldMonsterPopulation$Actor.snapshot()Llocal/lito381/server/ClassicWorldMonsterPopulation$Snapshot;
+ASTORE 24
+ALOAD 0
+NEW local/lito381/server/ClassicWorldMonsterPopulation$Cell
+DUP
+ILOAD 1
+ALOAD 9
+GETFIELD local/lito381/server/ClassicWorldMonsterPopulation$Actor.x:I
+ALOAD 9
+GETFIELD local/lito381/server/ClassicWorldMonsterPopulation$Actor.y:I
+INVOKESPECIAL local/lito381/server/ClassicWorldMonsterPopulation$Cell.<init>(III)V
+INVOKEVIRTUAL local/lito381/server/ClassicWorldMonsterPopulation.vacate(Llocal/lito381/server/ClassicWorldMonsterPopulation$Cell;)V
+ALOAD 9
+ILOAD 21
+PUTFIELD local/lito381/server/ClassicWorldMonsterPopulation$Actor.x:I
+ALOAD 9
+ILOAD 22
+PUTFIELD local/lito381/server/ClassicWorldMonsterPopulation$Actor.y:I
+ALOAD 9
+ILOAD 20
+PUTFIELD local/lito381/server/ClassicWorldMonsterPopulation$Actor.heading:I
+ALOAD 0
+ALOAD 23
+INVOKEVIRTUAL local/lito381/server/ClassicWorldMonsterPopulation.occupy(Llocal/lito381/server/ClassicWorldMonsterPopulation$Cell;)V
@@ -375,25 +398,0 @@
-ALOAD 0
-NEW local/lito381/server/ClassicWorldMonsterPopulation$Cell
-DUP
-ILOAD 1
-ALOAD 9
-GETFIELD local/lito381/server/ClassicWorldMonsterPopulation$Actor.x:I
-ALOAD 9
-GETFIELD local/lito381/server/ClassicWorldMonsterPopulation$Actor.y:I
-INVOKESPECIAL local/lito381/server/ClassicWorldMonsterPopulation$Cell.<init>(III)V
-INVOKEVIRTUAL local/lito381/server/ClassicWorldMonsterPopulation.vacate(Llocal/lito381/server/ClassicWorldMonsterPopulation$Cell;)V
-ALOAD 9
-ILOAD 22
-PUTFIELD local/lito381/server/ClassicWorldMonsterPopulation$Actor.x:I
-ALOAD 9
-ILOAD 23
-PUTFIELD local/lito381/server/ClassicWorldMonsterPopulation$Actor.y:I
-ALOAD 9
-ILOAD 21
-PUTFIELD local/lito381/server/ClassicWorldMonsterPopulation$Actor.heading:I
-ALOAD 0
-ALOAD 24
-INVOKEVIRTUAL local/lito381/server/ClassicWorldMonsterPopulation.occupy(Llocal/lito381/server/ClassicWorldMonsterPopulation$Cell;)V
-ALOAD 9
-INVOKEVIRTUAL local/lito381/server/ClassicWorldMonsterPopulation$Actor.snapshot()Llocal/lito381/server/ClassicWorldMonsterPopulation$Snapshot;
-ASTORE 26
@@ -429,0 +429 @@
+ALOAD 24
@@ -431 +430,0 @@
-ALOAD 26
@@ -436,2 +435,2 @@
-IINC var=20 incr=1
-GOTO L308
+IINC var=19 incr=1
+GOTO L306
--- staged/ClassicWorldMonsterPopulation/itemAction-2b3a0435.txt
+++ candidate/ClassicWorldMonsterPopulation/itemAction-2b3a0435.txt
@@ -209 +209 @@
-IF_ICMPGE L330
+IF_ICMPGE L331
@@ -243 +243 @@
-IFEQ L328
+IFEQ L329
@@ -247 +247 @@
-IFNE L328
+IFNE L329
@@ -255 +255 @@
-IFNE L328
+IFNE L329
@@ -261 +261,2 @@
-IFNE L328
+IFEQ L254
+GOTO L329
--- staged/ClassicWorldMonsterPopulation/nativeCancellationSpawn-c7095212.txt
+++ candidate/ClassicWorldMonsterPopulation/nativeCancellationSpawn-c7095212.txt
@@ -52 +51,0 @@
-DUP
@@ -54,4 +52,0 @@
-DUP2_X1
-CONST Long:1
-LADD
-PUTFIELD local/lito381/server/ClassicWorldMonsterPopulation.nextQuestDynamicObjectId:J
@@ -142,0 +138,6 @@
+ALOAD 0
+DUP
+GETFIELD local/lito381/server/ClassicWorldMonsterPopulation.nextQuestDynamicObjectId:J
+CONST Long:1
+LADD
+PUTFIELD local/lito381/server/ClassicWorldMonsterPopulation.nextQuestDynamicObjectId:J
--- staged/ClassicWorldMonsterPopulation/nativeStaticCancellationSpawn-74b1477a.txt
+++ candidate/ClassicWorldMonsterPopulation/nativeStaticCancellationSpawn-74b1477a.txt
@@ -32 +31,0 @@
-DUP
@@ -34,4 +32,0 @@
-DUP2_X1
-CONST Long:1
-LADD
-PUTFIELD local/lito381/server/ClassicWorldMonsterPopulation.nextQuestDynamicObjectId:J
@@ -112,0 +108,6 @@
+ALOAD 0
+DUP
+GETFIELD local/lito381/server/ClassicWorldMonsterPopulation.nextQuestDynamicObjectId:J
+CONST Long:1
+LADD
+PUTFIELD local/lito381/server/ClassicWorldMonsterPopulation.nextQuestDynamicObjectId:J
--- staged/ClassicWorldMonsterPopulation/npcPoison-24e5d048.txt
+++ candidate/ClassicWorldMonsterPopulation/npcPoison-24e5d048.txt
@@ -70 +70 @@
-IF_ACMPEQ L99
+IF_ACMPEQ L94
@@ -72 +72 @@
-IFNE L99
+IFNE L94
@@ -77 +77 @@
-TABLESWITCH min=1 max=4 default=L69 labels=L73,L81,L87,L95
+TABLESWITCH min=1 max=4 default=L69 labels=L73,L80,L85,L92
@@ -88,2 +88 @@
-ISTORE 15
-GOTO L99
+GOTO L93
@@ -94,2 +93 @@
-ISTORE 16
-GOTO L99
+GOTO L93
@@ -102,2 +100 @@
-ISTORE 17
-GOTO L99
+GOTO L93
@@ -105 +101,0 @@
-DUP
@@ -107 +102,0 @@
-ISTORE 18
@@ -109 +104 @@
-IFEQ L107
+IFEQ L102
--- staged/ClassicWorldMonsterPopulation/pollParalyses-9bc5a531.txt
+++ candidate/ClassicWorldMonsterPopulation/pollParalyses-9bc5a531.txt
@@ -37 +37 @@
-IF_ICMPNE L80
+IF_ICMPNE L17
@@ -40 +40 @@
-IFEQ L80
+IFEQ L17
@@ -43 +43,2 @@
-IFEQ L80
+IFNE L36
+GOTO L17
@@ -47 +48 @@
-IF_ACMPNE L41
+IF_ACMPNE L42
@@ -49 +50 @@
-GOTO L42
+GOTO L43
@@ -59 +60 @@
-IFEQ L56
+IFEQ L57
@@ -61 +62 @@
-IFEQ L56
+IFEQ L57
@@ -70 +71 @@
-IFEQ L17
+IFEQ L80
@@ -72,2 +73 @@
-IFNE L65
-GOTO L17
+IFEQ L80
@@ -87 +87 @@
-INVOKEVIRTUAL java/util/ArrayList.add(Ljava/lang/Object;)Z
+INVOKEINTERFACE java/util/List.add(Ljava/lang/Object;)Z itf
--- staged/ClassicWorldMonsterPopulation/pollPoisons-37356651.txt
+++ candidate/ClassicWorldMonsterPopulation/pollPoisons-37356651.txt
@@ -42 +42 @@
-IF_ICMPNE L138
+IF_ICMPNE L22
@@ -45 +45 @@
-IFEQ L138
+IFEQ L22
@@ -48 +48,2 @@
-IFEQ L138
+IFNE L41
+GOTO L22
@@ -76 +77 @@
-IFNE L72
+IFNE L73
@@ -79 +80 @@
-IFNE L72
+IFNE L73
@@ -90 +91 @@
-IF_ICMPGE L103
+IF_ICMPGE L104
@@ -93 +94 @@
-IFEQ L103
+IFEQ L104
@@ -105 +106 @@
-IFNULL L101
+IFNULL L102
@@ -108 +109 @@
-INVOKEVIRTUAL java/util/ArrayList.add(Ljava/lang/Object;)Z
+INVOKEINTERFACE java/util/List.add(Ljava/lang/Object;)Z itf
@@ -111 +112 @@
-GOTO L78
+GOTO L79
@@ -113,2 +114,2 @@
-INVOKEVIRTUAL java/util/ArrayList.isEmpty()Z
-IFEQ L109
+INVOKEINTERFACE java/util/List.isEmpty()Z itf
+IFEQ L110
@@ -117 +118 @@
-GOTO L117
+GOTO L118
@@ -120 +121 @@
-INVOKEVIRTUAL java/util/ArrayList.size()I
+INVOKEINTERFACE java/util/List.size()I itf
@@ -123 +124 @@
-INVOKEVIRTUAL java/util/ArrayList.get(I)Ljava/lang/Object;
+INVOKEINTERFACE java/util/List.get(I)Ljava/lang/Object; itf
@@ -138 +139 @@
-INVOKEVIRTUAL java/util/ArrayList.add(Ljava/lang/Object;)Z
+INVOKEINTERFACE java/util/List.add(Ljava/lang/Object;)Z itf
@@ -142,2 +143 @@
-IFNE L135
-GOTO L22
+IFEQ L138
--- staged/ClassicWorldMonsterPopulation/returnGuardHome-55d0ea6f.txt
+++ candidate/ClassicWorldMonsterPopulation/returnGuardHome-55d0ea6f.txt
@@ -83 +83 @@
-IFLT L238
+IFLT L237
@@ -127 +126,0 @@
-CHECKCAST [I
@@ -136 +135 @@
-IF_ICMPGE L238
+IF_ICMPGE L237
@@ -163 +161,0 @@
-CHECKCAST local/lito381/server/ClassicWorldMapCatalog$ClassicMap
@@ -171 +169 @@
-IFEQ L236
+IFEQ L235
@@ -175 +173 @@
-IFNE L236
+IFNE L235
@@ -183 +181 @@
-IFNE L236
+IFNE L235
@@ -189 +187,2 @@
-IFNE L236
+IFEQ L180
+GOTO L235
@@ -246 +245 @@
-GOTO L125
+GOTO L124
@@ -253 +251,0 @@
-DUP
@@ -255 +253,2 @@
-IFNONNULL L254
+ALOAD 21
+IFNONNULL L253
@@ -278 +276,0 @@
-CHECKCAST local/lito381/server/ClassicWorldMonsterPopulation$GridPoint
@@ -283 +280,0 @@
-CHECKCAST local/lito381/server/ClassicWorldMonsterPopulation$GridPoint
@@ -324 +320,0 @@
-CHECKCAST local/lito381/server/ClassicWorldMonsterPopulation$Snapshot
```

</details>

### ClassicWorldMonsterPopulation$Snapshot — SAME_PATHS

<details>
<summary>全部方法診斷</summary>

```json
[
  {
    "method": "<init>(IIIJIIIIIIIIZZIILlocal/lito381/server/ClassicWorldMonsterCatalog$SpawnDefinition;ILjava/lang/String;Ljava/lang/String;III)V",
    "status": "SAME_PATHS",
    "detail": "path multisets equal (280)"
  }
]
```

</details>
<details>
<summary>完整正規化差異</summary>

```diff
--- staged/ClassicWorldMonsterPopulation$Snapshot/_init_-19dc55e1.txt
+++ candidate/ClassicWorldMonsterPopulation$Snapshot/_init_-19dc55e1.txt
@@ -12 +12 @@
-IFLT L76
+IFLT L79
@@ -14 +14 @@
-IFLE L76
+IFLE L79
@@ -16 +16 @@
-IFLE L76
+IFLE L79
@@ -20 +20 @@
-IFLE L76
+IFLE L79
@@ -22 +22 @@
-IFLE L76
+IFLE L79
@@ -24 +24 @@
-IFLE L76
+IFLE L79
@@ -26 +26 @@
-IFLE L76
+IFLE L79
@@ -28 +28 @@
-IFLT L76
+IFLT L79
@@ -31 +31 @@
-IF_ICMPGT L76
+IF_ICMPGT L79
@@ -33 +33 @@
-IFLT L76
+IFLT L79
@@ -35,2 +35,4 @@
-IFGT L28
-GOTO L76
+IFLE L79
+ILOAD 10
+ILOAD 11
+POP2
@@ -38 +40 @@
-IFLT L76
+IFLT L79
@@ -40,2 +42,4 @@
-IFGE L33
-GOTO L76
+IFLT L79
+ILOAD 12
+ILOAD 13
+POP2
@@ -44 +48 @@
-IFLE L38
+IFLE L42
@@ -46 +50 @@
-GOTO L39
+GOTO L43
@@ -48 +52 @@
-IF_ICMPNE L76
+IF_ICMPNE L79
@@ -50 +54 @@
-IFLT L76
+IFLT L79
@@ -53 +57 @@
-IF_ICMPGT L76
+IF_ICMPGT L79
@@ -55 +59 @@
-IFLT L76
+IFLT L79
@@ -58 +62 @@
-IF_ICMPGT L76
+IF_ICMPGT L79
@@ -60 +64 @@
-IFNE L54
+IFNE L58
@@ -62 +66 @@
-IFNE L76
+IFNE L79
@@ -64 +68 @@
-IFNULL L76
+IFNULL L79
@@ -66 +70 @@
-IFLT L76
+IFLT L79
@@ -69 +73 @@
-IF_ICMPGT L76
+IF_ICMPGT L79
@@ -71 +75 @@
-IFNULL L76
+IFNULL L79
@@ -73 +77 @@
-IFNULL L76
+IFNULL L79
@@ -76 +80 @@
-IF_ICMPLT L76
+IF_ICMPLT L79
@@ -79 +83 @@
-IF_ICMPGT L76
+IF_ICMPGT L79
@@ -81 +85 @@
-IFLT L76
+IFLT L79
@@ -83,2 +87 @@
-IFLT L76
-GOTO L81
+IFGE L84
```

</details>

### LegacyMySqlRepository — DIFF

```json
{
  "where": "method",
  "member": "healthCheck()V",
  "insnIndex": null,
  "detail": "left=java/lang/Exception right=java/sql/SQLException"
}
```
<details>
<summary>全部方法診斷</summary>

```json
[
  {
    "method": "<init>(Ljava/nio/file/Path;Ljava/lang/String;ILjava/lang/String;Ljava/lang/String;DZLjava/lang/String;)V",
    "status": "SAME_PATHS",
    "detail": "path multisets equal (766)"
  },
  {
    "method": "healthCheck()V",
    "status": "DIFF",
    "detail": "method healthCheck()V: left=java/lang/Exception right=java/sql/SQLException"
  },
  {
    "method": "persistTournamentProfile(JLjava/lang/String;Llocal/lito381/server/RepositoryTournamentProfile;Llocal/lito381/server/GameStateStore$TournamentProfileState;)Llocal/lito381/server/RepositoryTournamentMutation;",
    "status": "DIFF",
    "detail": "method persistTournamentProfile(JLjava/lang/String;Llocal/lito381/server/RepositoryTournamentProfile;Llocal/lito381/server/GameStateStore$TournamentProfileState;)Llocal/lito381/server/RepositoryTournamentMutation;: left=java/lang/Exception right=java/sql/SQLException"
  },
  {
    "method": "rewardArmorDefinition(Ljava/sql/Connection;I)Llocal/lito381/server/InventoryCatalog$Definition;",
    "status": "DIFF",
    "detail": "method rewardArmorDefinition(Ljava/sql/Connection;I)Llocal/lito381/server/InventoryCatalog$Definition;: left=7:[223:java/lang/IllegalArgumentException  ] right=7:[215:java/lang/IllegalArgumentException  ]; paths rejected: expression size limit 32768 exceeded"
  },
  {
    "method": "tournamentProfiles()Llocal/lito381/server/RepositoryTournamentRoster;",
    "status": "DIFF",
    "detail": "method tournamentProfiles()Llocal/lito381/server/RepositoryTournamentRoster;: left=java/lang/Exception right=java/sql/SQLException"
  }
]
```

</details>
<details>
<summary>完整正規化差異</summary>

```diff
--- staged/LegacyMySqlRepository/_init_-87101c19.txt
+++ candidate/LegacyMySqlRepository/_init_-87101c19.txt
@@ -8,3 +8,3 @@
-TRY 32:[40:java/lang/Throwable  ]
-TRY 34:[40:java/lang/Throwable  ]
-TRY 44:[46:java/lang/Throwable  ]
+TRY 33:[41:java/lang/Throwable  ]
+TRY 35:[41:java/lang/Throwable  ]
+TRY 45:[47:java/lang/Throwable  ]
@@ -42,0 +43 @@
+DUP2
@@ -44 +45 @@
-ALOAD 10
+POP
@@ -47 +48 @@
-IFNULL L52
+IFNULL L53
@@ -50 +51 @@
-GOTO L52
+GOTO L53
@@ -53 +54 @@
-IFNULL L50
+IFNULL L51
@@ -56 +57 @@
-GOTO L50
+GOTO L51
@@ -65 +66 @@
-IFNULL L58
+IFNULL L59
@@ -68 +69 @@
-IFEQ L63
+IFEQ L64
@@ -73 +74 @@
-GOTO L65
+GOTO L66
@@ -81 +82 @@
-IFNE L76
+IFNE L77
@@ -89 +90 @@
-IFNONNULL L81
+IFNONNULL L82
@@ -91 +92 @@
-GOTO L83
+GOTO L84
@@ -96 +97 @@
-IFLT L89
+IFLT L90
@@ -99 +100 @@
-IF_ICMPLE L94
+IF_ICMPLE L95
@@ -109 +110 @@
-IFNULL L103
+IFNULL L104
@@ -113 +114 @@
-IFNE L108
+IFNE L109
@@ -124 +125 @@
-IFEQ L118
+IFEQ L119
@@ -128 +129 @@
-IFGT L123
+IFGT L124
--- staged/LegacyMySqlRepository/healthCheck-9b75.txt
+++ candidate/LegacyMySqlRepository/healthCheck-9b75.txt
@@ -5 +5 @@
-java/lang/Exception
+java/sql/SQLException
--- staged/LegacyMySqlRepository/npcPhysicalStats-e05f78b0.txt
+++ candidate/LegacyMySqlRepository/npcPhysicalStats-e05f78b0.txt
@@ -14,4 +14,2 @@
-ASTORE 2
-ALOAD 2
-IFNULL L10
-ALOAD 2
+DUP
+IFNULL L8
@@ -18,0 +17 @@
+POP
--- staged/LegacyMySqlRepository/persistTournamentProfile-164dcc30.txt
+++ candidate/LegacyMySqlRepository/persistTournamentProfile-164dcc30.txt
@@ -5 +5 @@
-java/lang/Exception
+java/sql/SQLException
--- staged/LegacyMySqlRepository/rewardArmorDefinition-527f8935.txt
+++ candidate/LegacyMySqlRepository/rewardArmorDefinition-527f8935.txt
@@ -8,65 +8,65 @@
-TRY 7:[223:java/lang/IllegalArgumentException  ]
-TRY 12:[211:java/lang/Throwable  , 223:java/lang/IllegalArgumentException  ]
-TRY 14:[211:java/lang/Throwable  , 223:java/lang/IllegalArgumentException  ]
-TRY 17:[26:java/lang/Throwable  , 211:java/lang/Throwable  , 223:java/lang/IllegalArgumentException  ]
-TRY 30:[32:java/lang/Throwable  , 211:java/lang/Throwable  , 223:java/lang/IllegalArgumentException  ]
-TRY 35:[211:java/lang/Throwable  , 223:java/lang/IllegalArgumentException  ]
-TRY 37:[211:java/lang/Throwable  , 223:java/lang/IllegalArgumentException  ]
-TRY 39:[211:java/lang/Throwable  , 223:java/lang/IllegalArgumentException  ]
-TRY 45:[223:java/lang/IllegalArgumentException  ]
-TRY 48:[211:java/lang/Throwable  , 223:java/lang/IllegalArgumentException  ]
-TRY 52:[211:java/lang/Throwable  , 223:java/lang/IllegalArgumentException  ]
-TRY 55:[211:java/lang/Throwable  , 223:java/lang/IllegalArgumentException  ]
-TRY 56:[211:java/lang/Throwable  , 223:java/lang/IllegalArgumentException  ]
-TRY 59:[211:java/lang/Throwable  , 223:java/lang/IllegalArgumentException  ]
-TRY 62:[211:java/lang/Throwable  , 223:java/lang/IllegalArgumentException  ]
-TRY 65:[211:java/lang/Throwable  , 223:java/lang/IllegalArgumentException  ]
-TRY 68:[211:java/lang/Throwable  , 223:java/lang/IllegalArgumentException  ]
-TRY 71:[211:java/lang/Throwable  , 223:java/lang/IllegalArgumentException  ]
-TRY 74:[211:java/lang/Throwable  , 223:java/lang/IllegalArgumentException  ]
-TRY 77:[211:java/lang/Throwable  , 223:java/lang/IllegalArgumentException  ]
-TRY 80:[211:java/lang/Throwable  , 223:java/lang/IllegalArgumentException  ]
-TRY 83:[211:java/lang/Throwable  , 223:java/lang/IllegalArgumentException  ]
-TRY 86:[211:java/lang/Throwable  , 223:java/lang/IllegalArgumentException  ]
-TRY 89:[211:java/lang/Throwable  , 223:java/lang/IllegalArgumentException  ]
-TRY 92:[211:java/lang/Throwable  , 223:java/lang/IllegalArgumentException  ]
-TRY 95:[211:java/lang/Throwable  , 223:java/lang/IllegalArgumentException  ]
-TRY 98:[211:java/lang/Throwable  , 223:java/lang/IllegalArgumentException  ]
-TRY 101:[211:java/lang/Throwable  , 223:java/lang/IllegalArgumentException  ]
-TRY 104:[211:java/lang/Throwable  , 223:java/lang/IllegalArgumentException  ]
-TRY 107:[211:java/lang/Throwable  , 223:java/lang/IllegalArgumentException  ]
-TRY 110:[211:java/lang/Throwable  , 223:java/lang/IllegalArgumentException  ]
-TRY 113:[211:java/lang/Throwable  , 223:java/lang/IllegalArgumentException  ]
-TRY 116:[211:java/lang/Throwable  , 223:java/lang/IllegalArgumentException  ]
-TRY 119:[211:java/lang/Throwable  , 223:java/lang/IllegalArgumentException  ]
-TRY 122:[211:java/lang/Throwable  , 223:java/lang/IllegalArgumentException  ]
-TRY 125:[211:java/lang/Throwable  , 223:java/lang/IllegalArgumentException  ]
-TRY 128:[211:java/lang/Throwable  , 223:java/lang/IllegalArgumentException  ]
-TRY 131:[211:java/lang/Throwable  , 223:java/lang/IllegalArgumentException  ]
-TRY 132:[211:java/lang/Throwable  , 223:java/lang/IllegalArgumentException  ]
-TRY 133:[211:java/lang/Throwable  , 223:java/lang/IllegalArgumentException  ]
-TRY 135:[211:java/lang/Throwable  , 223:java/lang/IllegalArgumentException  ]
-TRY 141:[211:java/lang/Throwable  , 223:java/lang/IllegalArgumentException  ]
-TRY 144:[211:java/lang/Throwable  , 223:java/lang/IllegalArgumentException  ]
-TRY 147:[211:java/lang/Throwable  , 223:java/lang/IllegalArgumentException  ]
-TRY 150:[211:java/lang/Throwable  , 223:java/lang/IllegalArgumentException  ]
-TRY 153:[211:java/lang/Throwable  , 223:java/lang/IllegalArgumentException  ]
-TRY 157:[211:java/lang/Throwable  , 223:java/lang/IllegalArgumentException  ]
-TRY 164:[211:java/lang/Throwable  , 223:java/lang/IllegalArgumentException  ]
-TRY 167:[211:java/lang/Throwable  , 223:java/lang/IllegalArgumentException  ]
-TRY 173:[211:java/lang/Throwable  , 223:java/lang/IllegalArgumentException  ]
-TRY 176:[211:java/lang/Throwable  , 223:java/lang/IllegalArgumentException  ]
-TRY 179:[211:java/lang/Throwable  , 223:java/lang/IllegalArgumentException  ]
-TRY 182:[211:java/lang/Throwable  , 223:java/lang/IllegalArgumentException  ]
-TRY 183:[211:java/lang/Throwable  , 223:java/lang/IllegalArgumentException  ]
-TRY 186:[211:java/lang/Throwable  , 223:java/lang/IllegalArgumentException  ]
-TRY 188:[211:java/lang/Throwable  , 223:java/lang/IllegalArgumentException  ]
-TRY 191:[211:java/lang/Throwable  , 223:java/lang/IllegalArgumentException  ]
-TRY 192:[211:java/lang/Throwable  , 223:java/lang/IllegalArgumentException  ]
-TRY 193:[211:java/lang/Throwable  , 223:java/lang/IllegalArgumentException  ]
-TRY 196:[211:java/lang/Throwable  , 223:java/lang/IllegalArgumentException  ]
-TRY 202:[211:java/lang/Throwable  , 223:java/lang/IllegalArgumentException  ]
-TRY 208:[223:java/lang/IllegalArgumentException  ]
-TRY 215:[217:java/lang/Throwable  , 223:java/lang/IllegalArgumentException  ]
-TRY 220:[223:java/lang/IllegalArgumentException  ]
-TRY 222:[223:java/lang/IllegalArgumentException  ]
+TRY 7:[215:java/lang/IllegalArgumentException  ]
+TRY 12:[203:java/lang/Throwable  , 215:java/lang/IllegalArgumentException  ]
+TRY 14:[203:java/lang/Throwable  , 215:java/lang/IllegalArgumentException  ]
+TRY 17:[191:java/lang/Throwable  , 203:java/lang/Throwable  , 215:java/lang/IllegalArgumentException  ]
+TRY 24:[203:java/lang/Throwable  , 215:java/lang/IllegalArgumentException  ]
+TRY 28:[215:java/lang/IllegalArgumentException  ]
+TRY 31:[191:java/lang/Throwable  , 203:java/lang/Throwable  , 215:java/lang/IllegalArgumentException  ]
+TRY 35:[191:java/lang/Throwable  , 203:java/lang/Throwable  , 215:java/lang/IllegalArgumentException  ]
+TRY 38:[191:java/lang/Throwable  , 203:java/lang/Throwable  , 215:java/lang/IllegalArgumentException  ]
+TRY 39:[191:java/lang/Throwable  , 203:java/lang/Throwable  , 215:java/lang/IllegalArgumentException  ]
+TRY 42:[191:java/lang/Throwable  , 203:java/lang/Throwable  , 215:java/lang/IllegalArgumentException  ]
+TRY 45:[191:java/lang/Throwable  , 203:java/lang/Throwable  , 215:java/lang/IllegalArgumentException  ]
+TRY 48:[191:java/lang/Throwable  , 203:java/lang/Throwable  , 215:java/lang/IllegalArgumentException  ]
+TRY 51:[191:java/lang/Throwable  , 203:java/lang/Throwable  , 215:java/lang/IllegalArgumentException  ]
+TRY 54:[191:java/lang/Throwable  , 203:java/lang/Throwable  , 215:java/lang/IllegalArgumentException  ]
+TRY 57:[191:java/lang/Throwable  , 203:java/lang/Throwable  , 215:java/lang/IllegalArgumentException  ]
+TRY 60:[191:java/lang/Throwable  , 203:java/lang/Throwable  , 215:java/lang/IllegalArgumentException  ]
+TRY 63:[191:java/lang/Throwable  , 203:java/lang/Throwable  , 215:java/lang/IllegalArgumentException  ]
+TRY 66:[191:java/lang/Throwable  , 203:java/lang/Throwable  , 215:java/lang/IllegalArgumentException  ]
+TRY 69:[191:java/lang/Throwable  , 203:java/lang/Throwable  , 215:java/lang/IllegalArgumentException  ]
+TRY 72:[191:java/lang/Throwable  , 203:java/lang/Throwable  , 215:java/lang/IllegalArgumentException  ]
+TRY 75:[191:java/lang/Throwable  , 203:java/lang/Throwable  , 215:java/lang/IllegalArgumentException  ]
+TRY 78:[191:java/lang/Throwable  , 203:java/lang/Throwable  , 215:java/lang/IllegalArgumentException  ]
+TRY 81:[191:java/lang/Throwable  , 203:java/lang/Throwable  , 215:java/lang/IllegalArgumentException  ]
+TRY 84:[191:java/lang/Throwable  , 203:java/lang/Throwable  , 215:java/lang/IllegalArgumentException  ]
+TRY 87:[191:java/lang/Throwable  , 203:java/lang/Throwable  , 215:java/lang/IllegalArgumentException  ]
+TRY 90:[191:java/lang/Throwable  , 203:java/lang/Throwable  , 215:java/lang/IllegalArgumentException  ]
+TRY 93:[191:java/lang/Throwable  , 203:java/lang/Throwable  , 215:java/lang/IllegalArgumentException  ]
+TRY 96:[191:java/lang/Throwable  , 203:java/lang/Throwable  , 215:java/lang/IllegalArgumentException  ]
+TRY 99:[191:java/lang/Throwable  , 203:java/lang/Throwable  , 215:java/lang/IllegalArgumentException  ]
+TRY 102:[191:java/lang/Throwable  , 203:java/lang/Throwable  , 215:java/lang/IllegalArgumentException  ]
+TRY 105:[191:java/lang/Throwable  , 203:java/lang/Throwable  , 215:java/lang/IllegalArgumentException  ]
+TRY 108:[191:java/lang/Throwable  , 203:java/lang/Throwable  , 215:java/lang/IllegalArgumentException  ]
+TRY 111:[191:java/lang/Throwable  , 203:java/lang/Throwable  , 215:java/lang/IllegalArgumentException  ]
+TRY 114:[191:java/lang/Throwable  , 203:java/lang/Throwable  , 215:java/lang/IllegalArgumentException  ]
+TRY 115:[191:java/lang/Throwable  , 203:java/lang/Throwable  , 215:java/lang/IllegalArgumentException  ]
+TRY 116:[191:java/lang/Throwable  , 203:java/lang/Throwable  , 215:java/lang/IllegalArgumentException  ]
+TRY 118:[191:java/lang/Throwable  , 203:java/lang/Throwable  , 215:java/lang/IllegalArgumentException  ]
+TRY 124:[191:java/lang/Throwable  , 203:java/lang/Throwable  , 215:java/lang/IllegalArgumentException  ]
+TRY 127:[191:java/lang/Throwable  , 203:java/lang/Throwable  , 215:java/lang/IllegalArgumentException  ]
+TRY 130:[191:java/lang/Throwable  , 203:java/lang/Throwable  , 215:java/lang/IllegalArgumentException  ]
+TRY 133:[191:java/lang/Throwable  , 203:java/lang/Throwable  , 215:java/lang/IllegalArgumentException  ]
+TRY 136:[191:java/lang/Throwable  , 203:java/lang/Throwable  , 215:java/lang/IllegalArgumentException  ]
+TRY 140:[191:java/lang/Throwable  , 203:java/lang/Throwable  , 215:java/lang/IllegalArgumentException  ]
+TRY 147:[191:java/lang/Throwable  , 203:java/lang/Throwable  , 215:java/lang/IllegalArgumentException  ]
+TRY 150:[191:java/lang/Throwable  , 203:java/lang/Throwable  , 215:java/lang/IllegalArgumentException  ]
+TRY 156:[191:java/lang/Throwable  , 203:java/lang/Throwable  , 215:java/lang/IllegalArgumentException  ]
+TRY 159:[191:java/lang/Throwable  , 203:java/lang/Throwable  , 215:java/lang/IllegalArgumentException  ]
+TRY 162:[191:java/lang/Throwable  , 203:java/lang/Throwable  , 215:java/lang/IllegalArgumentException  ]
+TRY 165:[191:java/lang/Throwable  , 203:java/lang/Throwable  , 215:java/lang/IllegalArgumentException  ]
+TRY 166:[191:java/lang/Throwable  , 203:java/lang/Throwable  , 215:java/lang/IllegalArgumentException  ]
+TRY 169:[191:java/lang/Throwable  , 203:java/lang/Throwable  , 215:java/lang/IllegalArgumentException  ]
+TRY 171:[191:java/lang/Throwable  , 203:java/lang/Throwable  , 215:java/lang/IllegalArgumentException  ]
+TRY 174:[191:java/lang/Throwable  , 203:java/lang/Throwable  , 215:java/lang/IllegalArgumentException  ]
+TRY 175:[191:java/lang/Throwable  , 203:java/lang/Throwable  , 215:java/lang/IllegalArgumentException  ]
+TRY 176:[191:java/lang/Throwable  , 203:java/lang/Throwable  , 215:java/lang/IllegalArgumentException  ]
+TRY 179:[191:java/lang/Throwable  , 203:java/lang/Throwable  , 215:java/lang/IllegalArgumentException  ]
+TRY 184:[203:java/lang/Throwable  , 215:java/lang/IllegalArgumentException  ]
+TRY 188:[215:java/lang/IllegalArgumentException  ]
+TRY 195:[197:java/lang/Throwable  , 203:java/lang/Throwable  , 215:java/lang/IllegalArgumentException  ]
+TRY 200:[203:java/lang/Throwable  , 215:java/lang/IllegalArgumentException  ]
+TRY 202:[203:java/lang/Throwable  , 215:java/lang/IllegalArgumentException  ]
+TRY 207:[209:java/lang/Throwable  , 215:java/lang/IllegalArgumentException  ]
+TRY 212:[215:java/lang/IllegalArgumentException  ]
+TRY 214:[215:java/lang/IllegalArgumentException  ]
@@ -91,2 +91 @@
-IFEQ L20
-GOTO L48
+IFNE L31
@@ -96,3 +95,95 @@
-IFNONNULL L25
-GOTO L40
-GOTO L38
+IFNULL L25
+ALOAD 5
+INVOKEINTERFACE java/sql/ResultSet.close()V itf
+ALOAD 4
+IFNULL L29
+ALOAD 4
+INVOKEINTERFACE java/sql/PreparedStatement.close()V itf
+ALOAD 6
+ARETURN
+NEW local/lito381/server/InventoryCatalog$NativeArmorStatus
+DUP
+ALOAD 5
+CONST str:"armor_ac"
+INVOKEINTERFACE java/sql/ResultSet.getInt(Ljava/lang/String;)I itf
+ALOAD 5
+CONST str:"material"
+INVOKEINTERFACE java/sql/ResultSet.getString(Ljava/lang/String;)Ljava/lang/String; itf
+INVOKESTATIC local/lito381/server/ClassicWorldDropCatalog.materialCode(Ljava/lang/String;)I
+ALOAD 5
+CONST str:"weight"
+INVOKEINTERFACE java/sql/ResultSet.getInt(Ljava/lang/String;)I itf
+ALOAD 5
+CONST str:"class_mask"
+INVOKEINTERFACE java/sql/ResultSet.getInt(Ljava/lang/String;)I itf
+ALOAD 5
+CONST str:"safenchant"
+INVOKEINTERFACE java/sql/ResultSet.getInt(Ljava/lang/String;)I itf
+ALOAD 5
+CONST str:"add_str"
+INVOKEINTERFACE java/sql/ResultSet.getInt(Ljava/lang/String;)I itf
+ALOAD 5
+CONST str:"add_dex"
+INVOKEINTERFACE java/sql/ResultSet.getInt(Ljava/lang/String;)I itf
+ALOAD 5
+CONST str:"add_con"
+INVOKEINTERFACE java/sql/ResultSet.getInt(Ljava/lang/String;)I itf
+ALOAD 5
+CONST str:"add_wis"
+INVOKEINTERFACE java/sql/ResultSet.getInt(Ljava/lang/String;)I itf
+ALOAD 5
+CONST str:"add_int"
+INVOKEINTERFACE java/sql/ResultSet.getInt(Ljava/lang/String;)I itf
+ALOAD 5
+CONST str:"add_cha"
+INVOKEINTERFACE java/sql/ResultSet.getInt(Ljava/lang/String;)I itf
+ALOAD 5
+CONST str:"add_hp"
+INVOKEINTERFACE java/sql/ResultSet.getInt(Ljava/lang/String;)I itf
+ALOAD 5
+CONST str:"add_mp"
+INVOKEINTERFACE java/sql/ResultSet.getInt(Ljava/lang/String;)I itf
+ALOAD 5
+CONST str:"m_def"
+INVOKEINTERFACE java/sql/ResultSet.getInt(Ljava/lang/String;)I itf
+ALOAD 5
+CONST str:"add_sp"
+INVOKEINTERFACE java/sql/ResultSet.getInt(Ljava/lang/String;)I itf
+ALOAD 5
+CONST str:"haste_item"
+INVOKEINTERFACE java/sql/ResultSet.getBoolean(Ljava/lang/String;)Z itf
+ALOAD 5
+CONST str:"defense_fire"
+INVOKEINTERFACE java/sql/ResultSet.getInt(Ljava/lang/String;)I itf
+ALOAD 5
+CONST str:"defense_water"
+INVOKEINTERFACE java/sql/ResultSet.getInt(Ljava/lang/String;)I itf
+ALOAD 5
+CONST str:"defense_wind"
+INVOKEINTERFACE java/sql/ResultSet.getInt(Ljava/lang/String;)I itf
+ALOAD 5
+CONST str:"defense_earth"
+INVOKEINTERFACE java/sql/ResultSet.getInt(Ljava/lang/String;)I itf
+ALOAD 5
+CONST str:"regist_blind"
+INVOKEINTERFACE java/sql/ResultSet.getInt(Ljava/lang/String;)I itf
+ALOAD 5
+CONST str:"regist_sleep"
+INVOKEINTERFACE java/sql/ResultSet.getInt(Ljava/lang/String;)I itf
+ALOAD 5
+CONST str:"regist_freeze"
+INVOKEINTERFACE java/sql/ResultSet.getInt(Ljava/lang/String;)I itf
+ALOAD 5
+CONST str:"regist_stone"
+INVOKEINTERFACE java/sql/ResultSet.getInt(Ljava/lang/String;)I itf
+ALOAD 5
+CONST str:"regist_sustain"
+INVOKEINTERFACE java/sql/ResultSet.getInt(Ljava/lang/String;)I itf
+ALOAD 5
+CONST str:"regist_stun"
+INVOKEINTERFACE java/sql/ResultSet.getInt(Ljava/lang/String;)I itf
+ALOAD 5
+CONST str:"type"
+INVOKEINTERFACE java/sql/ResultSet.getString(Ljava/lang/String;)Ljava/lang/String; itf
+INVOKESTATIC local/lito381/server/ClassicWorldDropCatalog.armorTypeCode(Ljava/lang/String;)I
+INVOKESPECIAL local/lito381/server/InventoryCatalog$NativeArmorStatus.<init>(IIIIIIIIIIIIIIIZIIIIIIIIIII)V
@@ -100,2 +191,65 @@
-ALOAD 5
-IFNULL L36
+NEW local/lito381/server/ClassicWorldDropCatalog$ItemWire
+DUP
+ILOAD 2
+CONST str:"armor"
+ALOAD 5
+CONST str:"display_name"
+INVOKEINTERFACE java/sql/ResultSet.getString(Ljava/lang/String;)Ljava/lang/String; itf
+ALOAD 5
+CONST str:"type"
+INVOKEINTERFACE java/sql/ResultSet.getString(Ljava/lang/String;)Ljava/lang/String; itf
+ALOAD 5
+CONST str:"invgfx"
+INVOKEINTERFACE java/sql/ResultSet.getInt(Ljava/lang/String;)I itf
+ALOAD 5
+CONST str:"grdgfx"
+INVOKEINTERFACE java/sql/ResultSet.getInt(Ljava/lang/String;)I itf
+ALOAD 5
+CONST str:"bless"
+INVOKEINTERFACE java/sql/ResultSet.getInt(Ljava/lang/String;)I itf
+CONST Integer:0
+ALOAD 5
+CONST str:"trade"
+INVOKEINTERFACE java/sql/ResultSet.getInt(Ljava/lang/String;)I itf
+IFNE L144
+CONST Integer:1
+GOTO L145
+CONST Integer:0
+ALOAD 5
+CONST str:"weight"
+INVOKEINTERFACE java/sql/ResultSet.getInt(Ljava/lang/String;)I itf
+ALOAD 5
+CONST str:"itemdesc_id"
+INVOKEINTERFACE java/sql/ResultSet.getInt(Ljava/lang/String;)I itf
+CONST str:""
+CONST Integer:0
+CONST Integer:0
+ALOAD 5
+CONST str:"material"
+INVOKEINTERFACE java/sql/ResultSet.getString(Ljava/lang/String;)Ljava/lang/String; itf
+ALOAD 5
+CONST str:"safenchant"
+INVOKEINTERFACE java/sql/ResultSet.getInt(Ljava/lang/String;)I itf
+ALOAD 5
+CONST str:"class_mask"
+INVOKEINTERFACE java/sql/ResultSet.getInt(Ljava/lang/String;)I itf
+ALOAD 5
+CONST str:"cant_delete"
+INVOKEINTERFACE java/sql/ResultSet.getBoolean(Ljava/lang/String;)Z itf
+INVOKESPECIAL local/lito381/server/ClassicWorldDropCatalog$ItemWire.<init>(ILjava/lang/String;Ljava/lang/String;Ljava/lang/String;IIIZZIILjava/lang/String;IILjava/lang/String;IIZ)V
+ASTORE 8
+ALOAD 5
+INVOKEINTERFACE java/sql/ResultSet.next()Z itf
+IFEQ L177
+NEW java/sql/SQLException
+DUP
+ILOAD 2
+INVOKEDYNAMIC makeConcatWithConstants(I)Ljava/lang/String; bsm=H6 java/lang/invoke/StringConcatFactory.makeConcatWithConstants(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;Ljava/lang/String;[Ljava/lang/Object;)Ljava/lang/invoke/CallSite; args=[str:"duplicate armor item id: \u0001"]
+INVOKESPECIAL java/sql/SQLException.<init>(Ljava/lang/String;)V
+ATHROW
+ALOAD 8
+ALOAD 7
+INVOKEVIRTUAL local/lito381/server/ClassicWorldDropCatalog$ItemWire.inventoryDefinition(Llocal/lito381/server/InventoryCatalog$NativeArmorStatus;)Llocal/lito381/server/InventoryCatalog$Definition;
+ASTORE 9
+ALOAD 5
+IFNULL L185
@@ -104,13 +258,2 @@
-GOTO L36
-ASTORE 8
-ALOAD 7
-ALOAD 8
-INVOKEVIRTUAL java/lang/Throwable.addSuppressed(Ljava/lang/Throwable;)V
-ALOAD 7
-ATHROW
-ALOAD 5
-INVOKEINTERFACE java/sql/ResultSet.close()V itf
-ALOAD 6
-ASTORE 9
-ALOAD 4
-IFNULL L46
+ALOAD 4
+IFNULL L189
@@ -121,86 +263,0 @@
-NEW local/lito381/server/InventoryCatalog$NativeArmorStatus
-DUP
-ALOAD 5
-CONST str:"armor_ac"
-INVOKEINTERFACE java/sql/ResultSet.getInt(Ljava/lang/String;)I itf
-ALOAD 5
-CONST str:"material"
-INVOKEINTERFACE java/sql/ResultSet.getString(Ljava/lang/String;)Ljava/lang/String; itf
-INVOKESTATIC local/lito381/server/ClassicWorldDropCatalog.materialCode(Ljava/lang/String;)I
-ALOAD 5
-CONST str:"weight"
-INVOKEINTERFACE java/sql/ResultSet.getInt(Ljava/lang/String;)I itf
-ALOAD 5
-CONST str:"class_mask"
-INVOKEINTERFACE java/sql/ResultSet.getInt(Ljava/lang/String;)I itf
-ALOAD 5
-CONST str:"safenchant"
-INVOKEINTERFACE java/sql/ResultSet.getInt(Ljava/lang/String;)I itf
-ALOAD 5
-CONST str:"add_str"
-INVOKEINTERFACE java/sql/ResultSet.getInt(Ljava/lang/String;)I itf
-ALOAD 5
-CONST str:"add_dex"
-INVOKEINTERFACE java/sql/ResultSet.getInt(Ljava/lang/String;)I itf
-ALOAD 5
-CONST str:"add_con"
-INVOKEINTERFACE java/sql/ResultSet.getInt(Ljava/lang/String;)I itf
-ALOAD 5
-CONST str:"add_wis"
-INVOKEINTERFACE java/sql/ResultSet.getInt(Ljava/lang/String;)I itf
-ALOAD 5
-CONST str:"add_int"
-INVOKEINTERFACE java/sql/ResultSet.getInt(Ljava/lang/String;)I itf
-ALOAD 5
-CONST str:"add_cha"
-INVOKEINTERFACE java/sql/ResultSet.getInt(Ljava/lang/String;)I itf
-ALOAD 5
-CONST str:"add_hp"
-INVOKEINTERFACE java/sql/ResultSet.getInt(Ljava/lang/String;)I itf
-ALOAD 5
-CONST str:"add_mp"
-INVOKEINTERFACE java/sql/ResultSet.getInt(Ljava/lang/String;)I itf
-ALOAD 5
-CONST str:"m_def"
-INVOKEINTERFACE java/sql/ResultSet.getInt(Ljava/lang/String;)I itf
-ALOAD 5
-CONST str:"add_sp"
-INVOKEINTERFACE java/sql/ResultSet.getInt(Ljava/lang/String;)I itf
-ALOAD 5
-CONST str:"haste_item"
-INVOKEINTERFACE java/sql/ResultSet.getBoolean(Ljava/lang/String;)Z itf
-ALOAD 5
-CONST str:"defense_fire"
-INVOKEINTERFACE java/sql/ResultSet.getInt(Ljava/lang/String;)I itf
-ALOAD 5
-CONST str:"defense_water"
-INVOKEINTERFACE java/sql/ResultSet.getInt(Ljava/lang/String;)I itf
-ALOAD 5
-CONST str:"defense_wind"
-INVOKEINTERFACE java/sql/ResultSet.getInt(Ljava/lang/String;)I itf
-ALOAD 5
-CONST str:"defense_earth"
-INVOKEINTERFACE java/sql/ResultSet.getInt(Ljava/lang/String;)I itf
-ALOAD 5
-CONST str:"regist_blind"
-INVOKEINTERFACE java/sql/ResultSet.getInt(Ljava/lang/String;)I itf
-ALOAD 5
-CONST str:"regist_sleep"
-INVOKEINTERFACE java/sql/ResultSet.getInt(Ljava/lang/String;)I itf
-ALOAD 5
-CONST str:"regist_freeze"
-INVOKEINTERFACE java/sql/ResultSet.getInt(Ljava/lang/String;)I itf
-ALOAD 5
-CONST str:"regist_stone"
-INVOKEINTERFACE java/sql/ResultSet.getInt(Ljava/lang/String;)I itf
-ALOAD 5
-CONST str:"regist_sustain"
-INVOKEINTERFACE java/sql/ResultSet.getInt(Ljava/lang/String;)I itf
-ALOAD 5
-CONST str:"regist_stun"
-INVOKEINTERFACE java/sql/ResultSet.getInt(Ljava/lang/String;)I itf
-ALOAD 5
-CONST str:"type"
-INVOKEINTERFACE java/sql/ResultSet.getString(Ljava/lang/String;)Ljava/lang/String; itf
-INVOKESTATIC local/lito381/server/ClassicWorldDropCatalog.armorTypeCode(Ljava/lang/String;)I
-INVOKESPECIAL local/lito381/server/InventoryCatalog$NativeArmorStatus.<init>(IIIIIIIIIIIIIIIZIIIIIIIIIII)V
@@ -208,49 +265,5 @@
-NEW local/lito381/server/ClassicWorldDropCatalog$ItemWire
-DUP
-ILOAD 2
-CONST str:"armor"
-ALOAD 5
-CONST str:"display_name"
-INVOKEINTERFACE java/sql/ResultSet.getString(Ljava/lang/String;)Ljava/lang/String; itf
-ALOAD 5
-CONST str:"type"
-INVOKEINTERFACE java/sql/ResultSet.getString(Ljava/lang/String;)Ljava/lang/String; itf
-ALOAD 5
-CONST str:"invgfx"
-INVOKEINTERFACE java/sql/ResultSet.getInt(Ljava/lang/String;)I itf
-ALOAD 5
-CONST str:"grdgfx"
-INVOKEINTERFACE java/sql/ResultSet.getInt(Ljava/lang/String;)I itf
-ALOAD 5
-CONST str:"bless"
-INVOKEINTERFACE java/sql/ResultSet.getInt(Ljava/lang/String;)I itf
-CONST Integer:0
-ALOAD 5
-CONST str:"trade"
-INVOKEINTERFACE java/sql/ResultSet.getInt(Ljava/lang/String;)I itf
-IFNE L161
-CONST Integer:1
-GOTO L162
-CONST Integer:0
-ALOAD 5
-CONST str:"weight"
-INVOKEINTERFACE java/sql/ResultSet.getInt(Ljava/lang/String;)I itf
-ALOAD 5
-CONST str:"itemdesc_id"
-INVOKEINTERFACE java/sql/ResultSet.getInt(Ljava/lang/String;)I itf
-CONST str:""
-CONST Integer:0
-CONST Integer:0
-ALOAD 5
-CONST str:"material"
-INVOKEINTERFACE java/sql/ResultSet.getString(Ljava/lang/String;)Ljava/lang/String; itf
-ALOAD 5
-CONST str:"safenchant"
-INVOKEINTERFACE java/sql/ResultSet.getInt(Ljava/lang/String;)I itf
-ALOAD 5
-CONST str:"class_mask"
-INVOKEINTERFACE java/sql/ResultSet.getInt(Ljava/lang/String;)I itf
-ALOAD 5
-CONST str:"cant_delete"
-INVOKEINTERFACE java/sql/ResultSet.getBoolean(Ljava/lang/String;)Z itf
-INVOKESPECIAL local/lito381/server/ClassicWorldDropCatalog$ItemWire.<init>(ILjava/lang/String;Ljava/lang/String;Ljava/lang/String;IIIZZIILjava/lang/String;IILjava/lang/String;IIZ)V
+ALOAD 5
+IFNULL L201
+ALOAD 5
+INVOKEINTERFACE java/sql/ResultSet.close()V itf
+GOTO L201
@@ -258,3 +271,18 @@
-ALOAD 5
-INVOKEINTERFACE java/sql/ResultSet.next()Z itf
-IFEQ L194
+ALOAD 10
+ALOAD 11
+INVOKEVIRTUAL java/lang/Throwable.addSuppressed(Ljava/lang/Throwable;)V
+ALOAD 10
+ATHROW
+ASTORE 12
+ALOAD 4
+IFNULL L213
+ALOAD 4
+INVOKEINTERFACE java/sql/PreparedStatement.close()V itf
+GOTO L213
+ASTORE 13
+ALOAD 12
+ALOAD 13
+INVOKEVIRTUAL java/lang/Throwable.addSuppressed(Ljava/lang/Throwable;)V
+ALOAD 12
+ATHROW
+ASTORE 14
@@ -264,27 +292 @@
-INVOKEDYNAMIC makeConcatWithConstants(I)Ljava/lang/String; bsm=H6 java/lang/invoke/StringConcatFactory.makeConcatWithConstants(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;Ljava/lang/String;[Ljava/lang/Object;)Ljava/lang/invoke/CallSite; args=[str:"duplicate armor item id: \u0001"]
-INVOKESPECIAL java/sql/SQLException.<init>(Ljava/lang/String;)V
-ATHROW
-ALOAD 11
-ALOAD 10
-INVOKEVIRTUAL local/lito381/server/ClassicWorldDropCatalog$ItemWire.inventoryDefinition(Llocal/lito381/server/InventoryCatalog$NativeArmorStatus;)Llocal/lito381/server/InventoryCatalog$Definition;
-ASTORE 12
-ALOAD 5
-IFNONNULL L201
-GOTO L203
-ALOAD 5
-INVOKEINTERFACE java/sql/ResultSet.close()V itf
-ALOAD 12
-ASTORE 13
-ALOAD 4
-IFNULL L209
-ALOAD 4
-INVOKEINTERFACE java/sql/PreparedStatement.close()V itf
-ALOAD 13
-ARETURN
-ASTORE 14
-ALOAD 4
-IFNULL L221
-ALOAD 4
-INVOKEINTERFACE java/sql/PreparedStatement.close()V itf
-GOTO L221
-ASTORE 15
+INVOKEDYNAMIC makeConcatWithConstants(I)Ljava/lang/String; bsm=H6 java/lang/invoke/StringConcatFactory.makeConcatWithConstants(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;Ljava/lang/String;[Ljava/lang/Object;)Ljava/lang/invoke/CallSite; args=[str:"invalid reward armor template: \u0001"]
@@ -292,10 +293,0 @@
-ALOAD 15
-INVOKEVIRTUAL java/lang/Throwable.addSuppressed(Ljava/lang/Throwable;)V
-ALOAD 14
-ATHROW
-ASTORE 16
-NEW java/sql/SQLException
-DUP
-ILOAD 2
-INVOKEDYNAMIC makeConcatWithConstants(I)Ljava/lang/String; bsm=H6 java/lang/invoke/StringConcatFactory.makeConcatWithConstants(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;Ljava/lang/String;[Ljava/lang/Object;)Ljava/lang/invoke/CallSite; args=[str:"invalid reward armor template: \u0001"]
-ALOAD 16
--- staged/LegacyMySqlRepository/tournamentProfiles-81def19b.txt
+++ candidate/LegacyMySqlRepository/tournamentProfiles-81def19b.txt
@@ -5 +5 @@
-java/lang/Exception
+java/sql/SQLException
```

</details>

### NativeArmorState — SAME_PATHS

<details>
<summary>全部方法診斷</summary>

```json
[
  {
    "method": "activate(Llocal/lito381/server/ClientSession;JI)Z",
    "status": "SAME_PATHS",
    "detail": "path multisets equal (5)"
  },
  {
    "method": "active(Llocal/lito381/server/ClientSession;J)Z",
    "status": "SAME_PATHS",
    "detail": "path multisets equal (5)"
  },
  {
    "method": "cancel(Llocal/lito381/server/ClientSession;)Z",
    "status": "SAME_PATHS",
    "detail": "path multisets equal (5)"
  }
]
```

</details>
<details>
<summary>完整正規化差異</summary>

```diff
--- staged/NativeArmorState/activate-32f4bfa9.txt
+++ candidate/NativeArmorState/activate-32f4bfa9.txt
@@ -8 +8 @@
-TRY 6:[9:java/lang/Throwable  ]
+TRY 6:[14:java/lang/Throwable  ]
@@ -17 +17,6 @@
-GOTO L16
+ALOAD 0
+ALOAD 4
+CONST null
+INVOKESTATIC local/lito381/server/NativeSourceManualScope.exitState(Llocal/lito381/server/ClientSession;Llocal/lito381/server/NativePlayerActionScope$Ticket;Ljava/lang/Throwable;)V
+ILOAD 5
+IRETURN
@@ -25,6 +29,0 @@
-ALOAD 0
-ALOAD 4
-CONST null
-INVOKESTATIC local/lito381/server/NativeSourceManualScope.exitState(Llocal/lito381/server/ClientSession;Llocal/lito381/server/NativePlayerActionScope$Ticket;Ljava/lang/Throwable;)V
-ILOAD 5
-IRETURN
--- staged/NativeArmorState/active-e8de9722.txt
+++ candidate/NativeArmorState/active-e8de9722.txt
@@ -8 +8 @@
-TRY 5:[8:java/lang/Throwable  ]
+TRY 5:[13:java/lang/Throwable  ]
@@ -16 +16,6 @@
-GOTO L15
+ALOAD 0
+ALOAD 3
+CONST null
+INVOKESTATIC local/lito381/server/NativeSourceManualScope.exitState(Llocal/lito381/server/ClientSession;Llocal/lito381/server/NativePlayerActionScope$Ticket;Ljava/lang/Throwable;)V
+ILOAD 4
+IRETURN
@@ -24,6 +28,0 @@
-ALOAD 0
-ALOAD 3
-CONST null
-INVOKESTATIC local/lito381/server/NativeSourceManualScope.exitState(Llocal/lito381/server/ClientSession;Llocal/lito381/server/NativePlayerActionScope$Ticket;Ljava/lang/Throwable;)V
-ILOAD 4
-IRETURN
--- staged/NativeArmorState/cancel-390f6c6a.txt
+++ candidate/NativeArmorState/cancel-390f6c6a.txt
@@ -8 +8 @@
-TRY 4:[7:java/lang/Throwable  ]
+TRY 4:[12:java/lang/Throwable  ]
@@ -15 +15,6 @@
-GOTO L14
+ALOAD 0
+ALOAD 1
+CONST null
+INVOKESTATIC local/lito381/server/NativeSourceManualScope.exitState(Llocal/lito381/server/ClientSession;Llocal/lito381/server/NativePlayerActionScope$Ticket;Ljava/lang/Throwable;)V
+ILOAD 2
+IRETURN
@@ -23,6 +27,0 @@
-ALOAD 0
-ALOAD 1
-CONST null
-INVOKESTATIC local/lito381/server/NativeSourceManualScope.exitState(Llocal/lito381/server/ClientSession;Llocal/lito381/server/NativePlayerActionScope$Ticket;Ljava/lang/Throwable;)V
-ILOAD 2
-IRETURN
```

</details>

### NativeCancellationNpcStatic — DIFF

```json
{
  "where": "method",
  "member": "cast(Llocal/lito381/server/ClientSession;Llocal/lito381/server/GameHub;Llocal/lito381/server/ClassicWorldNpcCatalog$StaticNpc;Llocal/lito381/server/Protocol$ObjectSpellRequest;Llocal/lito381/server/BattleSpellCatalog$CancellationSpell;Llocal/lito381/server/NativeCancellationNpcRules$Rule;)V",
  "insnIndex": 337,
  "detail": "left=IFEQ L447 right=IFEQ L448; paths rejected: normal control-flow cycle"
}
```
<details>
<summary>全部方法診斷</summary>

```json
[
  {
    "method": "lambda$publish$3(Llocal/lito381/server/GameHub;Llocal/lito381/server/NativePlayerCommit$Binding;Llocal/lito381/server/ClientSession;IILjava/util/function/BooleanSupplier;[BLjava/lang/String;)V",
    "status": "ONLY_RIGHT",
    "detail": "only right"
  },
  {
    "method": "lambda$publish$4(Llocal/lito381/server/NativePlayerCommit$Binding;Llocal/lito381/server/ClientSession;[Ljava/lang/Runnable;Llocal/lito381/server/GameHub;IILjava/util/function/BooleanSupplier;[BLjava/lang/String;)V",
    "status": "ONLY_RIGHT",
    "detail": "only right"
  },
  {
    "method": "lambda$publish$5(Llocal/lito381/server/ClientSession;Llocal/lito381/server/NativePlayerCommit$Binding;Llocal/lito381/server/GameHub;IILjava/util/function/BooleanSupplier;[BLjava/lang/String;)V",
    "status": "ONLY_RIGHT",
    "detail": "only right"
  },
  {
    "method": "lambda$route$0(Llocal/lito381/server/GameHub;Llocal/lito381/server/NativePlayerCommit$Binding;Llocal/lito381/server/ClientSession;Ljava/lang/Object;IILlocal/lito381/server/ClassicWorldNpcCatalog$StaticNpc;Llocal/lito381/server/Protocol$ObjectSpellRequest;Llocal/lito381/server/BattleSpellCatalog$CancellationSpell;Llocal/lito381/server/NativeCancellationNpcRules$Rule;)V",
    "status": "ONLY_RIGHT",
    "detail": "only right"
  },
  {
    "method": "lambda$route$1(Llocal/lito381/server/ClientSession;Llocal/lito381/server/GameHub;Llocal/lito381/server/NativePlayerCommit$Binding;Ljava/lang/Object;IILlocal/lito381/server/ClassicWorldNpcCatalog$StaticNpc;Llocal/lito381/server/Protocol$ObjectSpellRequest;Llocal/lito381/server/BattleSpellCatalog$CancellationSpell;Llocal/lito381/server/NativeCancellationNpcRules$Rule;)V",
    "status": "ONLY_RIGHT",
    "detail": "only right"
  },
  {
    "method": "lambda$sendNpc$7(Llocal/lito381/server/ClassicWorldNpcCatalog$StaticNpc;ILlocal/lito381/server/GameHub;Ljava/lang/Object;Llocal/lito381/server/ClientSession;ILlocal/lito381/server/NativeCancellationNpcStatic$Appearance;)Z",
    "status": "ONLY_RIGHT",
    "detail": "only right"
  },
  {
    "method": "cast(Llocal/lito381/server/ClientSession;Llocal/lito381/server/GameHub;Llocal/lito381/server/ClassicWorldNpcCatalog$StaticNpc;Llocal/lito381/server/Protocol$ObjectSpellRequest;Llocal/lito381/server/BattleSpellCatalog$CancellationSpell;Llocal/lito381/server/NativeCancellationNpcRules$Rule;)V",
    "status": "DIFF",
    "detail": "method cast(Llocal/lito381/server/ClientSession;Llocal/lito381/server/GameHub;Llocal/lito381/server/ClassicWorldNpcCatalog$StaticNpc;Llocal/lito381/server/Protocol$ObjectSpellRequest;Llocal/lito381/server/BattleSpellCatalog$CancellationSpell;Llocal/lito381/server/NativeCancellationNpcRules$Rule;)V insn[337]: left=IFEQ L447 right=IFEQ L448; paths rejected: normal control-flow cycle"
  },
  {
    "method": "lambda$nativeStaticReplacementOriginalRoute$3(Llocal/lito381/server/GameHub;Llocal/lito381/server/NativePlayerCommit$Binding;Llocal/lito381/server/ClientSession;Ljava/lang/Object;IILlocal/lito381/server/ClassicWorldNpcCatalog$StaticNpc;Llocal/lito381/server/Protocol$ObjectSpellRequest;Llocal/lito381/server/BattleSpellCatalog$CancellationSpell;Llocal/lito381/server/NativeCancellationNpcRules$Rule;)V",
    "status": "ONLY_LEFT",
    "detail": "only left"
  },
  {
    "method": "lambda$nativeStaticReplacementOriginalRoute$4(Llocal/lito381/server/ClientSession;Llocal/lito381/server/GameHub;Llocal/lito381/server/NativePlayerCommit$Binding;Ljava/lang/Object;IILlocal/lito381/server/ClassicWorldNpcCatalog$StaticNpc;Llocal/lito381/server/Protocol$ObjectSpellRequest;Llocal/lito381/server/BattleSpellCatalog$CancellationSpell;Llocal/lito381/server/NativeCancellationNpcRules$Rule;)V",
    "status": "ONLY_LEFT",
    "detail": "only left"
  },
  {
    "method": "lambda$publish$5(Llocal/lito381/server/GameHub;Llocal/lito381/server/NativePlayerCommit$Binding;Llocal/lito381/server/ClientSession;IILjava/util/function/BooleanSupplier;[BLjava/lang/String;)V",
    "status": "ONLY_LEFT",
    "detail": "only left"
  },
  {
    "method": "lambda$publish$6(Llocal/lito381/server/NativePlayerCommit$Binding;Llocal/lito381/server/ClientSession;[Ljava/lang/Runnable;Llocal/lito381/server/GameHub;IILjava/util/function/BooleanSupplier;[BLjava/lang/String;)V",
    "status": "ONLY_LEFT",
    "detail": "only left"
  },
  {
    "method": "lambda$publish$7(Llocal/lito381/server/ClientSession;Llocal/lito381/server/NativePlayerCommit$Binding;Llocal/lito381/server/GameHub;IILjava/util/function/BooleanSupplier;[BLjava/lang/String;)V",
    "status": "ONLY_LEFT",
    "detail": "only left"
  },
  {
    "method": "lambda$sendNpc$2(Llocal/lito381/server/ClassicWorldNpcCatalog$StaticNpc;ILlocal/lito381/server/GameHub;Llocal/lito381/server/NativeStaticNpcScene$Actor;Llocal/lito381/server/ClientSession;ILlocal/lito381/server/NativeCancellationNpcStatic$Appearance;)Z",
    "status": "ONLY_LEFT",
    "detail": "only left"
  },
  {
    "method": "nativeStaticReplacementOriginalRoute(Llocal/lito381/server/ClientSession;[BLlocal/lito381/server/BattleSpellCatalog$CancellationSpell;)Z",
    "status": "DIFF",
    "detail": "method nativeStaticReplacementOriginalRoute(Llocal/lito381/server/ClientSession;[BLlocal/lito381/server/BattleSpellCatalog$CancellationSpell;)Z insn[66]: left=INVOKEDYNAMIC run(Llocal/lito381/server/ClientSession;Llocal/lito381/server/GameHub;Llocal/lito381/server/NativePlayerCommit$Binding;Ljava/lang/Object;IILlocal/lito381/server/ClassicWorldNpcCatalog$StaticNpc;Llocal/lito381/server/Protocol$O… right=INVOKEDYNAMIC run(Llocal/lito381/server/ClientSession;Llocal/lito381/server/GameHub;Llocal/lito381/server/NativePlayerCommit$Binding;Ljava/lang/Object;IILlocal/lito381/server/ClassicWorldNpcCatalog$StaticNpc;Llocal/lito381/server/Protocol$O…; path event multisets differ (left=32, right=32)"
  },
  {
    "method": "publish(Llocal/lito381/server/ClientSession;Llocal/lito381/server/GameHub;IILlocal/lito381/server/NativePlayerCommit$Binding;[BLjava/lang/String;Ljava/util/function/BooleanSupplier;)V",
    "status": "DIFF",
    "detail": "method publish(Llocal/lito381/server/ClientSession;Llocal/lito381/server/GameHub;IILlocal/lito381/server/NativePlayerCommit$Binding;[BLjava/lang/String;Ljava/util/function/BooleanSupplier;)V insn[12]: left=INVOKEDYNAMIC run(Llocal/lito381/server/ClientSession;Llocal/lito381/server/NativePlayerCommit$Binding;Llocal/lito381/server/GameHub;IILjava/util/function/BooleanSupplier;[BLjava/lang/String;)Llocal/lito381/server/NativeSelfCastAction$Task;… right=INVOKEDYNAMIC run(Llocal/lito381/server/ClientSession;Llocal/lito381/server/NativePlayerCommit$Binding;Llocal/lito381/server/GameHub;IILjava/util/function/BooleanSupplier;[BLjava/lang/String;)Llocal/lito381/server/NativeSelfCastAction$Task;…; path event multisets differ (left=5, right=5)"
  },
  {
    "method": "sendNpc(Llocal/lito381/server/ClientSession;Llocal/lito381/server/GameHub;Llocal/lito381/server/ClassicWorldNpcCatalog$StaticNpc;[BLjava/lang/String;)V",
    "status": "DIFF",
    "detail": "method sendNpc(Llocal/lito381/server/ClientSession;Llocal/lito381/server/GameHub;Llocal/lito381/server/ClassicWorldNpcCatalog$StaticNpc;[BLjava/lang/String;)V: left=25:[32:java/lang/Throwable  ] right=23:[30:java/lang/Throwable  ]; path event multisets differ (left=15, right=15)"
  }
]
```

</details>
<details>
<summary>完整正規化差異</summary>

```diff
--- staged/NativeCancellationNpcStatic/cast-d79ca2be.txt
+++ candidate/NativeCancellationNpcStatic/cast-d79ca2be.txt
@@ -346 +346 @@
-IFEQ L447
+IFEQ L448
@@ -363 +363 @@
-IFEQ L446
+IFEQ L335
@@ -381 +381,2 @@
-IFEQ L446
+IFNE L374
+GOTO L335
@@ -387,4 +388,4 @@
-IFEQ L382
-ALOAD 20
-ALOAD 0
-IF_ACMPNE L390
+IFEQ L383
+ALOAD 20
+ALOAD 0
+IF_ACMPNE L391
@@ -415 +416 @@
-IFEQ L446
+IFEQ L447
@@ -422 +423 @@
-IFEQ L428
+IFEQ L429
@@ -436 +437 @@
-GOTO L404
+GOTO L446
@@ -439 +440 @@
-IFEQ L445
+IFEQ L446
@@ -454 +455 @@
-GOTO L404
+GOTO L405
--- staged/NativeCancellationNpcStatic/lambda$content$08386371c8d8962f7d683c746b94c6a408a61a6a14555c8be2f743aa49e4747e-5da19760.txt
+++ candidate/NativeCancellationNpcStatic/lambda$content$08386371c8d8962f7d683c746b94c6a408a61a6a14555c8be2f743aa49e4747e-5da19760.txt
@@ -0,0 +1,24 @@
+lambda$content$08386371c8d8962f7d683c746b94c6a408a61a6a14555c8be2f743aa49e4747e(Llocal/lito381/server/ClientSession;Llocal/lito381/server/GameHub;Llocal/lito381/server/NativePlayerCommit$Binding;Ljava/lang/Object;IILlocal/lito381/server/ClassicWorldNpcCatalog$StaticNpc;Llocal/lito381/server/Protocol$ObjectSpellRequest;Llocal/lito381/server/BattleSpellCatalog$CancellationSpell;Llocal/lito381/server/NativeCancellationNpcRules$Rule;)V
+(Llocal/lito381/server/ClientSession;Llocal/lito381/server/GameHub;Llocal/lito381/server/NativePlayerCommit$Binding;Ljava/lang/Object;IILlocal/lito381/server/ClassicWorldNpcCatalog$StaticNpc;Llocal/lito381/server/Protocol$ObjectSpellRequest;Llocal/lito381/server/BattleSpellCatalog$CancellationSpell;Llocal/lito381/server/NativeCancellationNpcRules$Rule;)V
+0xa
+
+java/io/IOException
+||||pvis=|pinv=|lvtvis=|lvtinv=
+
+TRY
+ALOAD 0
+CONST Integer:1
+CONST Integer:44
+ALOAD 1
+ALOAD 2
+ALOAD 0
+ALOAD 3
+ILOAD 4
+ILOAD 5
+ALOAD 6
+ALOAD 7
+ALOAD 8
+ALOAD 9
+INVOKEDYNAMIC run(Llocal/lito381/server/GameHub;Llocal/lito381/server/NativePlayerCommit$Binding;Llocal/lito381/server/ClientSession;Ljava/lang/Object;IILlocal/lito381/server/ClassicWorldNpcCatalog$StaticNpc;Llocal/lito381/server/Protocol$ObjectSpellRequest;Llocal/lito381/server/BattleSpellCatalog$CancellationSpell;Llocal/lito381/server/NativeCancellationNpcRules$Rule;)Llocal/lito381/server/NativeSelfCastAction$Task; bsm=H6 java/lang/invoke/LambdaMetafactory.metafactory(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodHandle;Ljava/lang/invoke/MethodType;)Ljava/lang/invoke/CallSite; args=[type:()V, handle:H6 local/lito381/server/NativeCancellationNpcStatic.lambda$content$962f6bef9b50d0cdcb06a7c27f8a7540a164f9ef4c6112b9b8a90d2ebb12aedf(Llocal/lito381/server/GameHub;Llocal/lito381/server/NativePlayerCommit$Binding;Llocal/lito381/server/ClientSession;Ljava/lang/Object;IILlocal/lito381/server/ClassicWorldNpcCatalog$StaticNpc;Llocal/lito381/server/Protocol$ObjectSpellRequest;Llocal/lito381/server/BattleSpellCatalog$CancellationSpell;Llocal/lito381/server/NativeCancellationNpcRules$Rule;)V, type:()V]
+INVOKESTATIC local/lito381/server/NativeWorldMaterialPolicy.run(Llocal/lito381/server/ClientSession;ZILlocal/lito381/server/NativeSelfCastAction$Task;)V
+RETURN
--- staged/NativeCancellationNpcStatic/lambda$content$495daea9de780102b5d76f3c2f6d7d53062d5dacc2ff0bb0c25a9d638c71c892-ea0e19fa.txt
+++ candidate/NativeCancellationNpcStatic/lambda$content$495daea9de780102b5d76f3c2f6d7d53062d5dacc2ff0bb0c25a9d638c71c892-ea0e19fa.txt
@@ -1,32 +0,0 @@
-lambda$content$495daea9de780102b5d76f3c2f6d7d53062d5dacc2ff0bb0c25a9d638c71c892(Llocal/lito381/server/ClientSession;Llocal/lito381/server/NativePlayerCommit$Binding;Llocal/lito381/server/GameHub;IILjava/util/function/BooleanSupplier;[BLjava/lang/String;)V
-(Llocal/lito381/server/ClientSession;Llocal/lito381/server/NativePlayerCommit$Binding;Llocal/lito381/server/GameHub;IILjava/util/function/BooleanSupplier;[BLjava/lang/String;)V
-0xa
-
-java/io/IOException
-||||pvis=|pinv=|lvtvis=|lvtinv=
-
-TRY
-CONST Integer:1
-ANEWARRAY java/lang/Runnable
-ASTORE 8
-ALOAD 0
-ALOAD 1
-ALOAD 0
-ALOAD 8
-ALOAD 2
-ILOAD 3
-ILOAD 4
-ALOAD 5
-ALOAD 6
-ALOAD 7
-INVOKEDYNAMIC run(Llocal/lito381/server/NativePlayerCommit$Binding;Llocal/lito381/server/ClientSession;[Ljava/lang/Runnable;Llocal/lito381/server/GameHub;IILjava/util/function/BooleanSupplier;[BLjava/lang/String;)Ljava/lang/Runnable; bsm=H6 java/lang/invoke/LambdaMetafactory.metafactory(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodHandle;Ljava/lang/invoke/MethodType;)Ljava/lang/invoke/CallSite; args=[type:()V, handle:H6 local/lito381/server/NativeCancellationNpcStatic.lambda$content$92028d34f88473a345fcec275fba51f4e0eaccffa9958015869b33997969c3e9(Llocal/lito381/server/NativePlayerCommit$Binding;Llocal/lito381/server/ClientSession;[Ljava/lang/Runnable;Llocal/lito381/server/GameHub;IILjava/util/function/BooleanSupplier;[BLjava/lang/String;)V, type:()V]
-INVOKESTATIC local/lito381/server/NativeSourcePlayerActionLock.run(Llocal/lito381/server/ClientSession;Ljava/lang/Runnable;)V
-ALOAD 8
-CONST Integer:0
-AALOAD
-IFNULL L23
-ALOAD 8
-CONST Integer:0
-AALOAD
-INVOKEINTERFACE java/lang/Runnable.run()V itf
-RETURN
--- staged/NativeCancellationNpcStatic/lambda$content$5aa1d9d082153e5b1ba82daf29f13c4aa529401011a1457aab8cda63a6a7a2bc-a8d4a90f.txt
+++ candidate/NativeCancellationNpcStatic/lambda$content$5aa1d9d082153e5b1ba82daf29f13c4aa529401011a1457aab8cda63a6a7a2bc-a8d4a90f.txt
@@ -0,0 +1,27 @@
+lambda$content$5aa1d9d082153e5b1ba82daf29f13c4aa529401011a1457aab8cda63a6a7a2bc(Llocal/lito381/server/NativePlayerCommit$Binding;Llocal/lito381/server/ClientSession;[Ljava/lang/Runnable;Llocal/lito381/server/GameHub;IILjava/util/function/BooleanSupplier;[BLjava/lang/String;)V
+(Llocal/lito381/server/NativePlayerCommit$Binding;Llocal/lito381/server/ClientSession;[Ljava/lang/Runnable;Llocal/lito381/server/GameHub;IILjava/util/function/BooleanSupplier;[BLjava/lang/String;)V
+0xa
+
+
+||||pvis=|pinv=|lvtvis=|lvtinv=
+
+TRY
+ALOAD 0
+ALOAD 1
+INVOKEVIRTUAL local/lito381/server/NativePlayerCommit$Binding.matches(Llocal/lito381/server/ClientSession;)Z
+IFEQ L18
+ALOAD 2
+CONST Integer:0
+ALOAD 1
+ALOAD 3
+ALOAD 0
+ALOAD 1
+ILOAD 4
+ILOAD 5
+ALOAD 6
+ALOAD 7
+ALOAD 8
+INVOKEDYNAMIC run(Llocal/lito381/server/GameHub;Llocal/lito381/server/NativePlayerCommit$Binding;Llocal/lito381/server/ClientSession;IILjava/util/function/BooleanSupplier;[BLjava/lang/String;)Ljava/lang/Runnable; bsm=H6 java/lang/invoke/LambdaMetafactory.metafactory(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodHandle;Ljava/lang/invoke/MethodType;)Ljava/lang/invoke/CallSite; args=[type:()V, handle:H6 local/lito381/server/NativeCancellationNpcStatic.lambda$content$85cfc073469d79b3673d136c5d70ce0d382c0615f5e58cb5a1fe90960b7baad5(Llocal/lito381/server/GameHub;Llocal/lito381/server/NativePlayerCommit$Binding;Llocal/lito381/server/ClientSession;IILjava/util/function/BooleanSupplier;[BLjava/lang/String;)V, type:()V]
+INVOKESTATIC local/lito381/server/NativeRolePublication.completion(Llocal/lito381/server/ClientSession;Ljava/lang/Runnable;)Ljava/lang/Runnable;
+AASTORE
+RETURN
--- staged/NativeCancellationNpcStatic/lambda$content$85cfc073469d79b3673d136c5d70ce0d382c0615f5e58cb5a1fe90960b7baad5-315f336a.txt
+++ candidate/NativeCancellationNpcStatic/lambda$content$85cfc073469d79b3673d136c5d70ce0d382c0615f5e58cb5a1fe90960b7baad5-315f336a.txt
@@ -0,0 +1,56 @@
+lambda$content$85cfc073469d79b3673d136c5d70ce0d382c0615f5e58cb5a1fe90960b7baad5(Llocal/lito381/server/GameHub;Llocal/lito381/server/NativePlayerCommit$Binding;Llocal/lito381/server/ClientSession;IILjava/util/function/BooleanSupplier;[BLjava/lang/String;)V
+(Llocal/lito381/server/GameHub;Llocal/lito381/server/NativePlayerCommit$Binding;Llocal/lito381/server/ClientSession;IILjava/util/function/BooleanSupplier;[BLjava/lang/String;)V
+0xa
+
+
+||||pvis=|pinv=|lvtvis=|lvtinv=
+
+TRY 6:[24:java/lang/Throwable  ]
+TRY 12:[24:java/lang/Throwable  ]
+TRY 15:[24:java/lang/Throwable  ]
+TRY 22:[24:java/lang/Throwable  ]
+TRY 26:[24:java/lang/Throwable  ]
+TRY 34:[36:java/io/IOException  ]
+ALOAD 0
+DUP
+ASTORE 8
+MONITORENTER
+ALOAD 1
+ALOAD 2
+INVOKEVIRTUAL local/lito381/server/NativePlayerCommit$Binding.matches(Llocal/lito381/server/ClientSession;)Z
+IFEQ L19
+ALOAD 2
+ALOAD 0
+ILOAD 3
+ILOAD 4
+INVOKESTATIC local/lito381/server/NativeStaticNpcScene.inScene(Llocal/lito381/server/ClientSession;Llocal/lito381/server/GameHub;II)Z
+IFEQ L19
+ALOAD 5
+INVOKEINTERFACE java/util/function/BooleanSupplier.getAsBoolean()Z itf
+IFEQ L19
+CONST Integer:1
+GOTO L20
+CONST Integer:0
+ISTORE 9
+ALOAD 8
+MONITOREXIT
+GOTO L29
+ASTORE 10
+ALOAD 8
+MONITOREXIT
+ALOAD 10
+ATHROW
+ILOAD 9
+IFEQ L42
+ALOAD 2
+ALOAD 6
+ALOAD 7
+INVOKEVIRTUAL local/lito381/server/ClientSession.nativeSelfOriginalSend([BLjava/lang/String;)V
+GOTO L42
+ASTORE 11
+NEW java/io/UncheckedIOException
+DUP
+ALOAD 11
+INVOKESPECIAL java/io/UncheckedIOException.<init>(Ljava/io/IOException;)V
+ATHROW
+RETURN
--- staged/NativeCancellationNpcStatic/lambda$content$8666a0e5cabddffe22473a12e621c428c6442e965ef297a62620cc80b073ed2a-ac0b9e82.txt
+++ candidate/NativeCancellationNpcStatic/lambda$content$8666a0e5cabddffe22473a12e621c428c6442e965ef297a62620cc80b073ed2a-ac0b9e82.txt
@@ -1,41 +0,0 @@
-lambda$content$8666a0e5cabddffe22473a12e621c428c6442e965ef297a62620cc80b073ed2a(Llocal/lito381/server/ClassicWorldNpcCatalog$StaticNpc;ILlocal/lito381/server/GameHub;Llocal/lito381/server/NativeStaticNpcScene$Actor;Llocal/lito381/server/ClientSession;ILlocal/lito381/server/NativeCancellationNpcStatic$Appearance;)Z
-(Llocal/lito381/server/ClassicWorldNpcCatalog$StaticNpc;ILlocal/lito381/server/GameHub;Llocal/lito381/server/NativeStaticNpcScene$Actor;Llocal/lito381/server/ClientSession;ILlocal/lito381/server/NativeCancellationNpcStatic$Appearance;)Z
-0xa
-
-
-||||pvis=|pinv=|lvtvis=|lvtinv=
-
-TRY
-ALOAD 0
-INVOKEVIRTUAL local/lito381/server/ClassicWorldNpcCatalog$StaticNpc.mapId()I
-ALOAD 0
-INVOKEVIRTUAL local/lito381/server/ClassicWorldNpcCatalog$StaticNpc.objectId()J
-INVOKESTATIC local/lito381/server/ClassicWorldNpcCatalog.npc(IJ)Llocal/lito381/server/ClassicWorldNpcCatalog$StaticNpc;
-ALOAD 0
-IF_ACMPNE L31
-ILOAD 1
-IFLE L16
-ALOAD 2
-ALOAD 0
-ILOAD 1
-CONST Integer:0
-INVOKESTATIC local/lito381/server/NativeStaticNpcScene.actor(Llocal/lito381/server/GameHub;Llocal/lito381/server/ClassicWorldNpcCatalog$StaticNpc;IZ)Llocal/lito381/server/NativeStaticNpcScene$Actor;
-ALOAD 3
-IF_ACMPNE L31
-ALOAD 2
-ALOAD 0
-ALOAD 4
-INVOKESTATIC local/lito381/server/NativeStaticNpcScene.gfx(Llocal/lito381/server/GameHub;Llocal/lito381/server/ClassicWorldNpcCatalog$StaticNpc;Llocal/lito381/server/ClientSession;)I
-ILOAD 5
-IF_ICMPNE L31
-ALOAD 2
-ALOAD 0
-ILOAD 1
-INVOKESTATIC local/lito381/server/NativeStaticNpcScene.appearance(Llocal/lito381/server/GameHub;Llocal/lito381/server/ClassicWorldNpcCatalog$StaticNpc;I)Llocal/lito381/server/NativeCancellationNpcStatic$Appearance;
-ALOAD 6
-INVOKEVIRTUAL local/lito381/server/NativeCancellationNpcStatic$Appearance.equals(Ljava/lang/Object;)Z
-IFEQ L31
-CONST Integer:1
-GOTO L32
-CONST Integer:0
-IRETURN
--- staged/NativeCancellationNpcStatic/lambda$content$92028d34f88473a345fcec275fba51f4e0eaccffa9958015869b33997969c3e9-a8d4a90f.txt
+++ candidate/NativeCancellationNpcStatic/lambda$content$92028d34f88473a345fcec275fba51f4e0eaccffa9958015869b33997969c3e9-a8d4a90f.txt
@@ -1,27 +0,0 @@
-lambda$content$92028d34f88473a345fcec275fba51f4e0eaccffa9958015869b33997969c3e9(Llocal/lito381/server/NativePlayerCommit$Binding;Llocal/lito381/server/ClientSession;[Ljava/lang/Runnable;Llocal/lito381/server/GameHub;IILjava/util/function/BooleanSupplier;[BLjava/lang/String;)V
-(Llocal/lito381/server/NativePlayerCommit$Binding;Llocal/lito381/server/ClientSession;[Ljava/lang/Runnable;Llocal/lito381/server/GameHub;IILjava/util/function/BooleanSupplier;[BLjava/lang/String;)V
-0xa
-
-
-||||pvis=|pinv=|lvtvis=|lvtinv=
-
-TRY
-ALOAD 0
-ALOAD 1
-INVOKEVIRTUAL local/lito381/server/NativePlayerCommit$Binding.matches(Llocal/lito381/server/ClientSession;)Z
-IFEQ L18
-ALOAD 2
-CONST Integer:0
-ALOAD 1
-ALOAD 3
-ALOAD 0
-ALOAD 1
-ILOAD 4
-ILOAD 5
-ALOAD 6
-ALOAD 7
-ALOAD 8
-INVOKEDYNAMIC run(Llocal/lito381/server/GameHub;Llocal/lito381/server/NativePlayerCommit$Binding;Llocal/lito381/server/ClientSession;IILjava/util/function/BooleanSupplier;[BLjava/lang/String;)Ljava/lang/Runnable; bsm=H6 java/lang/invoke/LambdaMetafactory.metafactory(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodHandle;Ljava/lang/invoke/MethodType;)Ljava/lang/invoke/CallSite; args=[type:()V, handle:H6 local/lito381/server/NativeCancellationNpcStatic.lambda$content$c3700c08c7e23166049e95139affff9d3a2a92164934daf0461024e3868996d6(Llocal/lito381/server/GameHub;Llocal/lito381/server/NativePlayerCommit$Binding;Llocal/lito381/server/ClientSession;IILjava/util/function/BooleanSupplier;[BLjava/lang/String;)V, type:()V]
-INVOKESTATIC local/lito381/server/NativeRolePublication.completion(Llocal/lito381/server/ClientSession;Ljava/lang/Runnable;)Ljava/lang/Runnable;
-AASTORE
-RETURN
--- staged/NativeCancellationNpcStatic/lambda$content$962f6bef9b50d0cdcb06a7c27f8a7540a164f9ef4c6112b9b8a90d2ebb12aedf-cfb6a162.txt
+++ candidate/NativeCancellationNpcStatic/lambda$content$962f6bef9b50d0cdcb06a7c27f8a7540a164f9ef4c6112b9b8a90d2ebb12aedf-cfb6a162.txt
@@ -0,0 +1,58 @@
+lambda$content$962f6bef9b50d0cdcb06a7c27f8a7540a164f9ef4c6112b9b8a90d2ebb12aedf(Llocal/lito381/server/GameHub;Llocal/lito381/server/NativePlayerCommit$Binding;Llocal/lito381/server/ClientSession;Ljava/lang/Object;IILlocal/lito381/server/ClassicWorldNpcCatalog$StaticNpc;Llocal/lito381/server/Protocol$ObjectSpellRequest;Llocal/lito381/server/BattleSpellCatalog$CancellationSpell;Llocal/lito381/server/NativeCancellationNpcRules$Rule;)V
+(Llocal/lito381/server/GameHub;Llocal/lito381/server/NativePlayerCommit$Binding;Llocal/lito381/server/ClientSession;Ljava/lang/Object;IILlocal/lito381/server/ClassicWorldNpcCatalog$StaticNpc;Llocal/lito381/server/Protocol$ObjectSpellRequest;Llocal/lito381/server/BattleSpellCatalog$CancellationSpell;Llocal/lito381/server/NativeCancellationNpcRules$Rule;)V
+0xa
+
+java/io/IOException
+||||pvis=|pinv=|lvtvis=|lvtinv=
+
+TRY 6:[37:java/lang/Throwable  ]
+TRY 12:[37:java/lang/Throwable  ]
+TRY 19:[37:java/lang/Throwable  ]
+TRY 23:[37:java/lang/Throwable  ]
+TRY 24:[37:java/lang/Throwable  ]
+TRY 33:[37:java/lang/Throwable  ]
+TRY 35:[37:java/lang/Throwable  ]
+TRY 39:[37:java/lang/Throwable  ]
+ALOAD 0
+DUP
+ASTORE 10
+MONITORENTER
+ALOAD 1
+ALOAD 2
+INVOKEVIRTUAL local/lito381/server/NativePlayerCommit$Binding.matches(Llocal/lito381/server/ClientSession;)Z
+IFEQ L34
+ALOAD 3
+IFNULL L34
+ALOAD 0
+ALOAD 2
+INVOKESTATIC local/lito381/server/NativeStaticNpcScene.location(Llocal/lito381/server/GameHub;Llocal/lito381/server/ClientSession;)Ljava/lang/Object;
+ALOAD 3
+IF_ACMPNE L34
+ALOAD 2
+ALOAD 0
+ILOAD 4
+ILOAD 5
+INVOKESTATIC local/lito381/server/NativeStaticNpcScene.inScene(Llocal/lito381/server/ClientSession;Llocal/lito381/server/GameHub;II)Z
+IFEQ L34
+ILOAD 4
+ALOAD 6
+INVOKEVIRTUAL local/lito381/server/ClassicWorldNpcCatalog$StaticNpc.objectId()J
+INVOKESTATIC local/lito381/server/ClassicWorldNpcCatalog.npc(IJ)Llocal/lito381/server/ClassicWorldNpcCatalog$StaticNpc;
+ALOAD 6
+IF_ACMPNE L34
+ALOAD 2
+ALOAD 0
+ALOAD 6
+ALOAD 7
+ALOAD 8
+ALOAD 9
+INVOKESTATIC local/lito381/server/NativeCancellationNpcStatic.cast(Llocal/lito381/server/ClientSession;Llocal/lito381/server/GameHub;Llocal/lito381/server/ClassicWorldNpcCatalog$StaticNpc;Llocal/lito381/server/Protocol$ObjectSpellRequest;Llocal/lito381/server/BattleSpellCatalog$CancellationSpell;Llocal/lito381/server/NativeCancellationNpcRules$Rule;)V
+ALOAD 10
+MONITOREXIT
+GOTO L42
+ASTORE 11
+ALOAD 10
+MONITOREXIT
+ALOAD 11
+ATHROW
+RETURN
--- staged/NativeCancellationNpcStatic/lambda$content$b793b6a9b41159a7d410323b8d2bb0c810af74ee3827b50cec74d4d4e9bbb299-ea0e19fa.txt
+++ candidate/NativeCancellationNpcStatic/lambda$content$b793b6a9b41159a7d410323b8d2bb0c810af74ee3827b50cec74d4d4e9bbb299-ea0e19fa.txt
@@ -0,0 +1,32 @@
+lambda$content$b793b6a9b41159a7d410323b8d2bb0c810af74ee3827b50cec74d4d4e9bbb299(Llocal/lito381/server/ClientSession;Llocal/lito381/server/NativePlayerCommit$Binding;Llocal/lito381/server/GameHub;IILjava/util/function/BooleanSupplier;[BLjava/lang/String;)V
+(Llocal/lito381/server/ClientSession;Llocal/lito381/server/NativePlayerCommit$Binding;Llocal/lito381/server/GameHub;IILjava/util/function/BooleanSupplier;[BLjava/lang/String;)V
+0xa
+
+java/io/IOException
+||||pvis=|pinv=|lvtvis=|lvtinv=
+
+TRY
+CONST Integer:1
+ANEWARRAY java/lang/Runnable
+ASTORE 8
+ALOAD 0
+ALOAD 1
+ALOAD 0
+ALOAD 8
+ALOAD 2
+ILOAD 3
+ILOAD 4
+ALOAD 5
+ALOAD 6
+ALOAD 7
+INVOKEDYNAMIC run(Llocal/lito381/server/NativePlayerCommit$Binding;Llocal/lito381/server/ClientSession;[Ljava/lang/Runnable;Llocal/lito381/server/GameHub;IILjava/util/function/BooleanSupplier;[BLjava/lang/String;)Ljava/lang/Runnable; bsm=H6 java/lang/invoke/LambdaMetafactory.metafactory(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodHandle;Ljava/lang/invoke/MethodType;)Ljava/lang/invoke/CallSite; args=[type:()V, handle:H6 local/lito381/server/NativeCancellationNpcStatic.lambda$content$5aa1d9d082153e5b1ba82daf29f13c4aa529401011a1457aab8cda63a6a7a2bc(Llocal/lito381/server/NativePlayerCommit$Binding;Llocal/lito381/server/ClientSession;[Ljava/lang/Runnable;Llocal/lito381/server/GameHub;IILjava/util/function/BooleanSupplier;[BLjava/lang/String;)V, type:()V]
+INVOKESTATIC local/lito381/server/NativeSourcePlayerActionLock.run(Llocal/lito381/server/ClientSession;Ljava/lang/Runnable;)V
+ALOAD 8
+CONST Integer:0
+AALOAD
+IFNULL L23
+ALOAD 8
+CONST Integer:0
+AALOAD
+INVOKEINTERFACE java/lang/Runnable.run()V itf
+RETURN
--- staged/NativeCancellationNpcStatic/lambda$content$bc76c46b7616c78a8d315567060eaa549063cb69706ad1801c1f96960e3bae60-5da19760.txt
+++ candidate/NativeCancellationNpcStatic/lambda$content$bc76c46b7616c78a8d315567060eaa549063cb69706ad1801c1f96960e3bae60-5da19760.txt
@@ -1,24 +0,0 @@
-lambda$content$bc76c46b7616c78a8d315567060eaa549063cb69706ad1801c1f96960e3bae60(Llocal/lito381/server/ClientSession;Llocal/lito381/server/GameHub;Llocal/lito381/server/NativePlayerCommit$Binding;Ljava/lang/Object;IILlocal/lito381/server/ClassicWorldNpcCatalog$StaticNpc;Llocal/lito381/server/Protocol$ObjectSpellRequest;Llocal/lito381/server/BattleSpellCatalog$CancellationSpell;Llocal/lito381/server/NativeCancellationNpcRules$Rule;)V
-(Llocal/lito381/server/ClientSession;Llocal/lito381/server/GameHub;Llocal/lito381/server/NativePlayerCommit$Binding;Ljava/lang/Object;IILlocal/lito381/server/ClassicWorldNpcCatalog$StaticNpc;Llocal/lito381/server/Protocol$ObjectSpellRequest;Llocal/lito381/server/BattleSpellCatalog$CancellationSpell;Llocal/lito381/server/NativeCancellationNpcRules$Rule;)V
-0xa
-
-java/io/IOException
-||||pvis=|pinv=|lvtvis=|lvtinv=
-
-TRY
-ALOAD 0
-CONST Integer:1
-CONST Integer:44
-ALOAD 1
-ALOAD 2
-ALOAD 0
-ALOAD 3
-ILOAD 4
-ILOAD 5
-ALOAD 6
-ALOAD 7
-ALOAD 8
-ALOAD 9
-INVOKEDYNAMIC run(Llocal/lito381/server/GameHub;Llocal/lito381/server/NativePlayerCommit$Binding;Llocal/lito381/server/ClientSession;Ljava/lang/Object;IILlocal/lito381/server/ClassicWorldNpcCatalog$StaticNpc;Llocal/lito381/server/Protocol$ObjectSpellRequest;Llocal/lito381/server/BattleSpellCatalog$CancellationSpell;Llocal/lito381/server/NativeCancellationNpcRules$Rule;)Llocal/lito381/server/NativeSelfCastAction$Task; bsm=H6 java/lang/invoke/LambdaMetafactory.metafactory(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodHandle;Ljava/lang/invoke/MethodType;)Ljava/lang/invoke/CallSite; args=[type:()V, handle:H6 local/lito381/server/NativeCancellationNpcStatic.lambda$content$f9704272107d35746035ad1f0d1659732946bff61bb0199bb6f9c7de567ec838(Llocal/lito381/server/GameHub;Llocal/lito381/server/NativePlayerCommit$Binding;Llocal/lito381/server/ClientSession;Ljava/lang/Object;IILlocal/lito381/server/ClassicWorldNpcCatalog$StaticNpc;Llocal/lito381/server/Protocol$ObjectSpellRequest;Llocal/lito381/server/BattleSpellCatalog$CancellationSpell;Llocal/lito381/server/NativeCancellationNpcRules$Rule;)V, type:()V]
-INVOKESTATIC local/lito381/server/NativeWorldMaterialPolicy.run(Llocal/lito381/server/ClientSession;ZILlocal/lito381/server/NativeSelfCastAction$Task;)V
-RETURN
--- staged/NativeCancellationNpcStatic/lambda$content$c3700c08c7e23166049e95139affff9d3a2a92164934daf0461024e3868996d6-315f336a.txt
+++ candidate/NativeCancellationNpcStatic/lambda$content$c3700c08c7e23166049e95139affff9d3a2a92164934daf0461024e3868996d6-315f336a.txt
@@ -1,58 +0,0 @@
-lambda$content$c3700c08c7e23166049e95139affff9d3a2a92164934daf0461024e3868996d6(Llocal/lito381/server/GameHub;Llocal/lito381/server/NativePlayerCommit$Binding;Llocal/lito381/server/ClientSession;IILjava/util/function/BooleanSupplier;[BLjava/lang/String;)V
-(Llocal/lito381/server/GameHub;Llocal/lito381/server/NativePlayerCommit$Binding;Llocal/lito381/server/ClientSession;IILjava/util/function/BooleanSupplier;[BLjava/lang/String;)V
-0xa
-
-
-||||pvis=|pinv=|lvtvis=|lvtinv=
-
-TRY 8:[26:java/lang/Throwable  ]
-TRY 14:[26:java/lang/Throwable  ]
-TRY 17:[26:java/lang/Throwable  ]
-TRY 24:[26:java/lang/Throwable  ]
-TRY 28:[26:java/lang/Throwable  ]
-TRY 36:[38:java/io/IOException  ]
-ALOAD 0
-ASTORE 8
-ALOAD 8
-DUP
-ASTORE 9
-MONITORENTER
-ALOAD 1
-ALOAD 2
-INVOKEVIRTUAL local/lito381/server/NativePlayerCommit$Binding.matches(Llocal/lito381/server/ClientSession;)Z
-IFEQ L21
-ALOAD 2
-ALOAD 0
-ILOAD 3
-ILOAD 4
-INVOKESTATIC local/lito381/server/NativeStaticNpcScene.inScene(Llocal/lito381/server/ClientSession;Llocal/lito381/server/GameHub;II)Z
-IFEQ L21
-ALOAD 5
-INVOKEINTERFACE java/util/function/BooleanSupplier.getAsBoolean()Z itf
-IFEQ L21
-CONST Integer:1
-GOTO L22
-CONST Integer:0
-ISTORE 10
-ALOAD 9
-MONITOREXIT
-GOTO L31
-ASTORE 11
-ALOAD 9
-MONITOREXIT
-ALOAD 11
-ATHROW
-ILOAD 10
-IFEQ L44
-ALOAD 2
-ALOAD 6
-ALOAD 7
-INVOKEVIRTUAL local/lito381/server/ClientSession.nativeSelfOriginalSend([BLjava/lang/String;)V
-GOTO L44
-ASTORE 12
-NEW java/io/UncheckedIOException
-DUP
-ALOAD 12
-INVOKESPECIAL java/io/UncheckedIOException.<init>(Ljava/io/IOException;)V
-ATHROW
-RETURN
--- staged/NativeCancellationNpcStatic/lambda$content$f9704272107d35746035ad1f0d1659732946bff61bb0199bb6f9c7de567ec838-cfb6a162.txt
+++ candidate/NativeCancellationNpcStatic/lambda$content$f9704272107d35746035ad1f0d1659732946bff61bb0199bb6f9c7de567ec838-cfb6a162.txt
@@ -1,60 +0,0 @@
-lambda$content$f9704272107d35746035ad1f0d1659732946bff61bb0199bb6f9c7de567ec838(Llocal/lito381/server/GameHub;Llocal/lito381/server/NativePlayerCommit$Binding;Llocal/lito381/server/ClientSession;Ljava/lang/Object;IILlocal/lito381/server/ClassicWorldNpcCatalog$StaticNpc;Llocal/lito381/server/Protocol$ObjectSpellRequest;Llocal/lito381/server/BattleSpellCatalog$CancellationSpell;Llocal/lito381/server/NativeCancellationNpcRules$Rule;)V
-(Llocal/lito381/server/GameHub;Llocal/lito381/server/NativePlayerCommit$Binding;Llocal/lito381/server/ClientSession;Ljava/lang/Object;IILlocal/lito381/server/ClassicWorldNpcCatalog$StaticNpc;Llocal/lito381/server/Protocol$ObjectSpellRequest;Llocal/lito381/server/BattleSpellCatalog$CancellationSpell;Llocal/lito381/server/NativeCancellationNpcRules$Rule;)V
-0xa
-
-java/io/IOException
-||||pvis=|pinv=|lvtvis=|lvtinv=
-
-TRY 8:[39:java/lang/Throwable  ]
-TRY 14:[39:java/lang/Throwable  ]
-TRY 21:[39:java/lang/Throwable  ]
-TRY 25:[39:java/lang/Throwable  ]
-TRY 26:[39:java/lang/Throwable  ]
-TRY 35:[39:java/lang/Throwable  ]
-TRY 37:[39:java/lang/Throwable  ]
-TRY 41:[39:java/lang/Throwable  ]
-ALOAD 0
-ASTORE 10
-ALOAD 10
-DUP
-ASTORE 11
-MONITORENTER
-ALOAD 1
-ALOAD 2
-INVOKEVIRTUAL local/lito381/server/NativePlayerCommit$Binding.matches(Llocal/lito381/server/ClientSession;)Z
-IFEQ L36
-ALOAD 3
-IFNULL L36
-ALOAD 0
-ALOAD 2
-INVOKESTATIC local/lito381/server/NativeStaticNpcScene.location(Llocal/lito381/server/GameHub;Llocal/lito381/server/ClientSession;)Ljava/lang/Object;
-ALOAD 3
-IF_ACMPNE L36
-ALOAD 2
-ALOAD 0
-ILOAD 4
-ILOAD 5
-INVOKESTATIC local/lito381/server/NativeStaticNpcScene.inScene(Llocal/lito381/server/ClientSession;Llocal/lito381/server/GameHub;II)Z
-IFEQ L36
-ILOAD 4
-ALOAD 6
-INVOKEVIRTUAL local/lito381/server/ClassicWorldNpcCatalog$StaticNpc.objectId()J
-INVOKESTATIC local/lito381/server/ClassicWorldNpcCatalog.npc(IJ)Llocal/lito381/server/ClassicWorldNpcCatalog$StaticNpc;
-ALOAD 6
-IF_ACMPNE L36
-ALOAD 2
-ALOAD 0
-ALOAD 6
-ALOAD 7
-ALOAD 8
-ALOAD 9
-INVOKESTATIC local/lito381/server/NativeCancellationNpcStatic.cast(Llocal/lito381/server/ClientSession;Llocal/lito381/server/GameHub;Llocal/lito381/server/ClassicWorldNpcCatalog$StaticNpc;Llocal/lito381/server/Protocol$ObjectSpellRequest;Llocal/lito381/server/BattleSpellCatalog$CancellationSpell;Llocal/lito381/server/NativeCancellationNpcRules$Rule;)V
-ALOAD 11
-MONITOREXIT
-GOTO L44
-ASTORE 12
-ALOAD 11
-MONITOREXIT
-ALOAD 12
-ATHROW
-RETURN
--- staged/NativeCancellationNpcStatic/lambda$content$ff02bb4375c0273f66494130a5f77b06f2112cf1fa9c213faf9874f9e34d35cc-55f59473.txt
+++ candidate/NativeCancellationNpcStatic/lambda$content$ff02bb4375c0273f66494130a5f77b06f2112cf1fa9c213faf9874f9e34d35cc-55f59473.txt
@@ -0,0 +1,41 @@
+lambda$content$ff02bb4375c0273f66494130a5f77b06f2112cf1fa9c213faf9874f9e34d35cc(Llocal/lito381/server/ClassicWorldNpcCatalog$StaticNpc;ILlocal/lito381/server/GameHub;Ljava/lang/Object;Llocal/lito381/server/ClientSession;ILlocal/lito381/server/NativeCancellationNpcStatic$Appearance;)Z
+(Llocal/lito381/server/ClassicWorldNpcCatalog$StaticNpc;ILlocal/lito381/server/GameHub;Ljava/lang/Object;Llocal/lito381/server/ClientSession;ILlocal/lito381/server/NativeCancellationNpcStatic$Appearance;)Z
+0xa
+
+
+||||pvis=|pinv=|lvtvis=|lvtinv=
+
+TRY
+ALOAD 0
+INVOKEVIRTUAL local/lito381/server/ClassicWorldNpcCatalog$StaticNpc.mapId()I
+ALOAD 0
+INVOKEVIRTUAL local/lito381/server/ClassicWorldNpcCatalog$StaticNpc.objectId()J
+INVOKESTATIC local/lito381/server/ClassicWorldNpcCatalog.npc(IJ)Llocal/lito381/server/ClassicWorldNpcCatalog$StaticNpc;
+ALOAD 0
+IF_ACMPNE L31
+ILOAD 1
+IFLE L16
+ALOAD 2
+ALOAD 0
+ILOAD 1
+CONST Integer:0
+INVOKESTATIC local/lito381/server/NativeStaticNpcScene.actor(Llocal/lito381/server/GameHub;Llocal/lito381/server/ClassicWorldNpcCatalog$StaticNpc;IZ)Llocal/lito381/server/NativeStaticNpcScene$Actor;
+ALOAD 3
+IF_ACMPNE L31
+ALOAD 2
+ALOAD 0
+ALOAD 4
+INVOKESTATIC local/lito381/server/NativeStaticNpcScene.gfx(Llocal/lito381/server/GameHub;Llocal/lito381/server/ClassicWorldNpcCatalog$StaticNpc;Llocal/lito381/server/ClientSession;)I
+ILOAD 5
+IF_ICMPNE L31
+ALOAD 2
+ALOAD 0
+ILOAD 1
+INVOKESTATIC local/lito381/server/NativeStaticNpcScene.appearance(Llocal/lito381/server/GameHub;Llocal/lito381/server/ClassicWorldNpcCatalog$StaticNpc;I)Llocal/lito381/server/NativeCancellationNpcStatic$Appearance;
+ALOAD 6
+INVOKEVIRTUAL local/lito381/server/NativeCancellationNpcStatic$Appearance.equals(Ljava/lang/Object;)Z
+IFEQ L31
+CONST Integer:1
+GOTO L32
+CONST Integer:0
+IRETURN
--- staged/NativeCancellationNpcStatic/nativeStaticReplacementOriginalRoute-f84226e1.txt
+++ candidate/NativeCancellationNpcStatic/nativeStaticReplacementOriginalRoute-f84226e1.txt
@@ -75 +75 @@
-INVOKEDYNAMIC run(Llocal/lito381/server/ClientSession;Llocal/lito381/server/GameHub;Llocal/lito381/server/NativePlayerCommit$Binding;Ljava/lang/Object;IILlocal/lito381/server/ClassicWorldNpcCatalog$StaticNpc;Llocal/lito381/server/Protocol$ObjectSpellRequest;Llocal/lito381/server/BattleSpellCatalog$CancellationSpell;Llocal/lito381/server/NativeCancellationNpcRules$Rule;)Llocal/lito381/server/NativeSelfCastAction$Task; bsm=H6 java/lang/invoke/LambdaMetafactory.metafactory(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodHandle;Ljava/lang/invoke/MethodType;)Ljava/lang/invoke/CallSite; args=[type:()V, handle:H6 local/lito381/server/NativeCancellationNpcStatic.lambda$content$bc76c46b7616c78a8d315567060eaa549063cb69706ad1801c1f96960e3bae60(Llocal/lito381/server/ClientSession;Llocal/lito381/server/GameHub;Llocal/lito381/server/NativePlayerCommit$Binding;Ljava/lang/Object;IILlocal/lito381/server/ClassicWorldNpcCatalog$StaticNpc;Llocal/lito381/server/Protocol$ObjectSpellRequest;Llocal/lito381/server/BattleSpellCatalog$CancellationSpell;Llocal/lito381/server/NativeCancellationNpcRules$Rule;)V, type:()V]
+INVOKEDYNAMIC run(Llocal/lito381/server/ClientSession;Llocal/lito381/server/GameHub;Llocal/lito381/server/NativePlayerCommit$Binding;Ljava/lang/Object;IILlocal/lito381/server/ClassicWorldNpcCatalog$StaticNpc;Llocal/lito381/server/Protocol$ObjectSpellRequest;Llocal/lito381/server/BattleSpellCatalog$CancellationSpell;Llocal/lito381/server/NativeCancellationNpcRules$Rule;)Llocal/lito381/server/NativeSelfCastAction$Task; bsm=H6 java/lang/invoke/LambdaMetafactory.metafactory(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodHandle;Ljava/lang/invoke/MethodType;)Ljava/lang/invoke/CallSite; args=[type:()V, handle:H6 local/lito381/server/NativeCancellationNpcStatic.lambda$content$08386371c8d8962f7d683c746b94c6a408a61a6a14555c8be2f743aa49e4747e(Llocal/lito381/server/ClientSession;Llocal/lito381/server/GameHub;Llocal/lito381/server/NativePlayerCommit$Binding;Ljava/lang/Object;IILlocal/lito381/server/ClassicWorldNpcCatalog$StaticNpc;Llocal/lito381/server/Protocol$ObjectSpellRequest;Llocal/lito381/server/BattleSpellCatalog$CancellationSpell;Llocal/lito381/server/NativeCancellationNpcRules$Rule;)V, type:()V]
--- staged/NativeCancellationNpcStatic/publish-31e3128.txt
+++ candidate/NativeCancellationNpcStatic/publish-31e3128.txt
@@ -21 +21 @@
-INVOKEDYNAMIC run(Llocal/lito381/server/ClientSession;Llocal/lito381/server/NativePlayerCommit$Binding;Llocal/lito381/server/GameHub;IILjava/util/function/BooleanSupplier;[BLjava/lang/String;)Llocal/lito381/server/NativeSelfCastAction$Task; bsm=H6 java/lang/invoke/LambdaMetafactory.metafactory(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodHandle;Ljava/lang/invoke/MethodType;)Ljava/lang/invoke/CallSite; args=[type:()V, handle:H6 local/lito381/server/NativeCancellationNpcStatic.lambda$content$495daea9de780102b5d76f3c2f6d7d53062d5dacc2ff0bb0c25a9d638c71c892(Llocal/lito381/server/ClientSession;Llocal/lito381/server/NativePlayerCommit$Binding;Llocal/lito381/server/GameHub;IILjava/util/function/BooleanSupplier;[BLjava/lang/String;)V, type:()V]
+INVOKEDYNAMIC run(Llocal/lito381/server/ClientSession;Llocal/lito381/server/NativePlayerCommit$Binding;Llocal/lito381/server/GameHub;IILjava/util/function/BooleanSupplier;[BLjava/lang/String;)Llocal/lito381/server/NativeSelfCastAction$Task; bsm=H6 java/lang/invoke/LambdaMetafactory.metafactory(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodHandle;Ljava/lang/invoke/MethodType;)Ljava/lang/invoke/CallSite; args=[type:()V, handle:H6 local/lito381/server/NativeCancellationNpcStatic.lambda$content$b793b6a9b41159a7d410323b8d2bb0c810af74ee3827b50cec74d4d4e9bbb299(Llocal/lito381/server/ClientSession;Llocal/lito381/server/NativePlayerCommit$Binding;Llocal/lito381/server/GameHub;IILjava/util/function/BooleanSupplier;[BLjava/lang/String;)V, type:()V]
--- staged/NativeCancellationNpcStatic/restored-d8c739.txt
+++ candidate/NativeCancellationNpcStatic/restored-d8c739.txt
@@ -8,10 +8,10 @@
-TRY 10:[32:java/lang/Throwable  ]
-TRY 15:[32:java/lang/Throwable  ]
-TRY 17:[32:java/lang/Throwable  ]
-TRY 18:[32:java/lang/Throwable  ]
-TRY 19:[32:java/lang/Throwable  ]
-TRY 22:[32:java/lang/Throwable  ]
-TRY 23:[32:java/lang/Throwable  ]
-TRY 28:[32:java/lang/Throwable  ]
-TRY 30:[32:java/lang/Throwable  ]
-TRY 34:[32:java/lang/Throwable  ]
+TRY 8:[30:java/lang/Throwable  ]
+TRY 13:[30:java/lang/Throwable  ]
+TRY 15:[30:java/lang/Throwable  ]
+TRY 16:[30:java/lang/Throwable  ]
+TRY 17:[30:java/lang/Throwable  ]
+TRY 20:[30:java/lang/Throwable  ]
+TRY 21:[30:java/lang/Throwable  ]
+TRY 26:[30:java/lang/Throwable  ]
+TRY 28:[30:java/lang/Throwable  ]
+TRY 32:[30:java/lang/Throwable  ]
@@ -19 +19 @@
-IFEQ L37
+IFEQ L35
@@ -20,0 +21 @@
+DUP
@@ -22,3 +22,0 @@
-ALOAD 3
-DUP
-ASTORE 4
@@ -29,4 +27,4 @@
-ASTORE 5
-ALOAD 5
-IFNULL L29
-ALOAD 5
+ASTORE 4
+ALOAD 4
+IFNULL L27
+ALOAD 4
@@ -39 +37 @@
-ALOAD 5
+ALOAD 4
@@ -42 +40 @@
-IFEQ L29
+IFEQ L27
@@ -47 +45 @@
-ALOAD 4
+ALOAD 3
@@ -49,3 +47,3 @@
-GOTO L37
-ASTORE 6
-ALOAD 4
+GOTO L35
+ASTORE 5
+ALOAD 3
@@ -53 +51 @@
-ALOAD 6
+ALOAD 5
@@ -54,0 +53 @@
+ILOAD 0
@@ -57 +55,0 @@
-ILOAD 0
--- staged/NativeCancellationNpcStatic/sendNpc-9281a6ff.txt
+++ candidate/NativeCancellationNpcStatic/sendNpc-9281a6ff.txt
@@ -8,3 +8,3 @@
-TRY 25:[32:java/lang/Throwable  ]
-TRY 30:[32:java/lang/Throwable  ]
-TRY 34:[32:java/lang/Throwable  ]
+TRY 23:[30:java/lang/Throwable  ]
+TRY 28:[30:java/lang/Throwable  ]
+TRY 32:[30:java/lang/Throwable  ]
@@ -24,0 +25 @@
+DUP
@@ -26,3 +26,0 @@
-ALOAD 8
-DUP
-ASTORE 9
@@ -31 +29 @@
-IFLE L27
+IFLE L25
@@ -37 +35 @@
-GOTO L28
+GOTO L26
@@ -38,0 +37,4 @@
+ASTORE 9
+ALOAD 8
+MONITOREXIT
+GOTO L35
@@ -40 +42 @@
-ALOAD 9
+ALOAD 8
@@ -42,5 +44 @@
-GOTO L37
-ASTORE 11
-ALOAD 9
-MONITOREXIT
-ALOAD 11
+ALOAD 10
@@ -60 +58 @@
-ALOAD 10
+ALOAD 9
@@ -64 +62 @@
-INVOKEDYNAMIC getAsBoolean(Llocal/lito381/server/ClassicWorldNpcCatalog$StaticNpc;ILlocal/lito381/server/GameHub;Llocal/lito381/server/NativeStaticNpcScene$Actor;Llocal/lito381/server/ClientSession;ILlocal/lito381/server/NativeCancellationNpcStatic$Appearance;)Ljava/util/function/BooleanSupplier; bsm=H6 java/lang/invoke/LambdaMetafactory.metafactory(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodHandle;Ljava/lang/invoke/MethodType;)Ljava/lang/invoke/CallSite; args=[type:()Z, handle:H6 local/lito381/server/NativeCancellationNpcStatic.lambda$content$8666a0e5cabddffe22473a12e621c428c6442e965ef297a62620cc80b073ed2a(Llocal/lito381/server/ClassicWorldNpcCatalog$StaticNpc;ILlocal/lito381/server/GameHub;Llocal/lito381/server/NativeStaticNpcScene$Actor;Llocal/lito381/server/ClientSession;ILlocal/lito381/server/NativeCancellationNpcStatic$Appearance;)Z, type:()Z]
+INVOKEDYNAMIC getAsBoolean(Llocal/lito381/server/ClassicWorldNpcCatalog$StaticNpc;ILlocal/lito381/server/GameHub;Ljava/lang/Object;Llocal/lito381/server/ClientSession;ILlocal/lito381/server/NativeCancellationNpcStatic$Appearance;)Ljava/util/function/BooleanSupplier; bsm=H6 java/lang/invoke/LambdaMetafactory.metafactory(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodHandle;Ljava/lang/invoke/MethodType;)Ljava/lang/invoke/CallSite; args=[type:()Z, handle:H6 local/lito381/server/NativeCancellationNpcStatic.lambda$content$ff02bb4375c0273f66494130a5f77b06f2112cf1fa9c213faf9874f9e34d35cc(Llocal/lito381/server/ClassicWorldNpcCatalog$StaticNpc;ILlocal/lito381/server/GameHub;Ljava/lang/Object;Llocal/lito381/server/ClientSession;ILlocal/lito381/server/NativeCancellationNpcStatic$Appearance;)Z, type:()Z]
```

</details>

### NativeCombatAttacker — DIFF

```json
{
  "where": "method",
  "member": "calculated(Llocal/lito381/server/NativePlayerCommit$Binding;)V",
  "insnIndex": 17,
  "detail": "left=INVOKEVIRTUAL java/lang/Object.equals(Ljava/lang/Object;)Z right=INVOKEVIRTUAL local/lito381/server/NativeCombatAttacker$Ref.equals(Ljava/lang/Object;)Z; paths rejected: normal control-flow cycle"
}
```
<details>
<summary>全部方法診斷</summary>

```json
[
  {
    "method": "calculated(Llocal/lito381/server/NativePlayerCommit$Binding;)V",
    "status": "DIFF",
    "detail": "method calculated(Llocal/lito381/server/NativePlayerCommit$Binding;)V insn[17]: left=INVOKEVIRTUAL java/lang/Object.equals(Ljava/lang/Object;)Z right=INVOKEVIRTUAL local/lito381/server/NativeCombatAttacker$Ref.equals(Ljava/lang/Object;)Z; paths rejected: normal control-flow cycle"
  },
  {
    "method": "live(Llocal/lito381/server/ClientSession;Llocal/lito381/server/NativeCombatAttacker$Ref;)Llocal/lito381/server/NativeCombatAttacker$Live;",
    "status": "SAME_PATHS",
    "detail": "path multisets equal (7)"
  },
  {
    "method": "nativeRefOriginalLive(Llocal/lito381/server/ClientSession;Llocal/lito381/server/NativeCombatAttacker$Ref;)Llocal/lito381/server/NativeCombatAttacker$Live;",
    "status": "SAME_PATHS",
    "detail": "path multisets equal (157)"
  },
  {
    "method": "owner(Llocal/lito381/server/ClientSession;Llocal/lito381/server/CombatMonsterCurseLifecycle$Context;)Llocal/lito381/server/ClientSession;",
    "status": "SAME_PATHS",
    "detail": "path multisets equal (15)"
  }
]
```

</details>
<details>
<summary>完整正規化差異</summary>

```diff
--- staged/NativeCombatAttacker/advance-5d527811.txt
+++ candidate/NativeCombatAttacker/advance-5d527811.txt
@@ -8,7 +8,7 @@
-TRY 22:[36:java/lang/Throwable  ]
-TRY 25:[36:java/lang/Throwable  ]
-TRY 29:[36:java/lang/Throwable  ]
-TRY 31:[36:java/lang/Throwable  ]
-TRY 32:[36:java/lang/Throwable  ]
-TRY 34:[36:java/lang/Throwable  ]
-TRY 38:[36:java/lang/Throwable  ]
+TRY 20:[34:java/lang/Throwable  ]
+TRY 23:[34:java/lang/Throwable  ]
+TRY 27:[34:java/lang/Throwable  ]
+TRY 29:[34:java/lang/Throwable  ]
+TRY 30:[34:java/lang/Throwable  ]
+TRY 32:[34:java/lang/Throwable  ]
+TRY 36:[34:java/lang/Throwable  ]
@@ -28,0 +29,2 @@
+ASTORE 2
+ALOAD 2
@@ -30 +31,0 @@
-ASTORE 2
@@ -32,3 +32,0 @@
-ALOAD 3
-DUP
-ASTORE 4
@@ -41,2 +39,2 @@
-ASTORE 5
-ALOAD 5
+ASTORE 4
+ALOAD 4
@@ -48 +46 @@
-ALOAD 4
+ALOAD 3
@@ -50,3 +48,3 @@
-GOTO L41
-ASTORE 6
-ALOAD 4
+GOTO L39
+ASTORE 5
+ALOAD 3
@@ -54 +52 @@
-ALOAD 6
+ALOAD 5
--- staged/NativeCombatAttacker/calculated-76c9af.txt
+++ candidate/NativeCombatAttacker/calculated-76c9af.txt
@@ -26 +26 @@
-INVOKEVIRTUAL java/lang/Object.equals(Ljava/lang/Object;)Z
+INVOKEVIRTUAL local/lito381/server/NativeCombatAttacker$Ref.equals(Ljava/lang/Object;)Z
--- staged/NativeCombatAttacker/encounter-cc805439.txt
+++ candidate/NativeCombatAttacker/encounter-cc805439.txt
@@ -8,31 +8,31 @@
-TRY 37:[134:java/lang/Throwable  ]
-TRY 38:[134:java/lang/Throwable  ]
-TRY 40:[134:java/lang/Throwable  ]
-TRY 41:[134:java/lang/Throwable  ]
-TRY 42:[134:java/lang/Throwable  ]
-TRY 48:[134:java/lang/Throwable  ]
-TRY 57:[134:java/lang/Throwable  ]
-TRY 63:[134:java/lang/Throwable  ]
-TRY 70:[134:java/lang/Throwable  ]
-TRY 79:[129:java/lang/Throwable  , 134:java/lang/Throwable  ]
-TRY 81:[129:java/lang/Throwable  , 134:java/lang/Throwable  ]
-TRY 85:[129:java/lang/Throwable  , 134:java/lang/Throwable  ]
-TRY 86:[129:java/lang/Throwable  , 134:java/lang/Throwable  ]
-TRY 87:[129:java/lang/Throwable  , 134:java/lang/Throwable  ]
-TRY 88:[129:java/lang/Throwable  , 134:java/lang/Throwable  ]
-TRY 93:[129:java/lang/Throwable  , 134:java/lang/Throwable  ]
-TRY 97:[129:java/lang/Throwable  , 134:java/lang/Throwable  ]
-TRY 99:[134:java/lang/Throwable  ]
-TRY 103:[129:java/lang/Throwable  , 134:java/lang/Throwable  ]
-TRY 104:[129:java/lang/Throwable  , 134:java/lang/Throwable  ]
-TRY 106:[129:java/lang/Throwable  , 134:java/lang/Throwable  ]
-TRY 110:[129:java/lang/Throwable  , 134:java/lang/Throwable  ]
-TRY 113:[129:java/lang/Throwable  , 134:java/lang/Throwable  ]
-TRY 117:[129:java/lang/Throwable  , 134:java/lang/Throwable  ]
-TRY 122:[129:java/lang/Throwable  , 134:java/lang/Throwable  ]
-TRY 123:[129:java/lang/Throwable  , 134:java/lang/Throwable  ]
-TRY 125:[129:java/lang/Throwable  , 134:java/lang/Throwable  ]
-TRY 127:[134:java/lang/Throwable  ]
-TRY 131:[129:java/lang/Throwable  , 134:java/lang/Throwable  ]
-TRY 133:[134:java/lang/Throwable  ]
-TRY 136:[134:java/lang/Throwable  ]
+TRY 35:[130:java/lang/Throwable  ]
+TRY 36:[130:java/lang/Throwable  ]
+TRY 38:[130:java/lang/Throwable  ]
+TRY 39:[130:java/lang/Throwable  ]
+TRY 40:[130:java/lang/Throwable  ]
+TRY 46:[130:java/lang/Throwable  ]
+TRY 55:[130:java/lang/Throwable  ]
+TRY 61:[130:java/lang/Throwable  ]
+TRY 66:[130:java/lang/Throwable  ]
+TRY 75:[125:java/lang/Throwable  , 130:java/lang/Throwable  ]
+TRY 77:[125:java/lang/Throwable  , 130:java/lang/Throwable  ]
+TRY 81:[125:java/lang/Throwable  , 130:java/lang/Throwable  ]
+TRY 82:[125:java/lang/Throwable  , 130:java/lang/Throwable  ]
+TRY 83:[125:java/lang/Throwable  , 130:java/lang/Throwable  ]
+TRY 84:[125:java/lang/Throwable  , 130:java/lang/Throwable  ]
+TRY 89:[125:java/lang/Throwable  , 130:java/lang/Throwable  ]
+TRY 93:[125:java/lang/Throwable  , 130:java/lang/Throwable  ]
+TRY 95:[130:java/lang/Throwable  ]
+TRY 99:[125:java/lang/Throwable  , 130:java/lang/Throwable  ]
+TRY 100:[125:java/lang/Throwable  , 130:java/lang/Throwable  ]
+TRY 102:[125:java/lang/Throwable  , 130:java/lang/Throwable  ]
+TRY 106:[125:java/lang/Throwable  , 130:java/lang/Throwable  ]
+TRY 109:[125:java/lang/Throwable  , 130:java/lang/Throwable  ]
+TRY 113:[125:java/lang/Throwable  , 130:java/lang/Throwable  ]
+TRY 118:[125:java/lang/Throwable  , 130:java/lang/Throwable  ]
+TRY 119:[125:java/lang/Throwable  , 130:java/lang/Throwable  ]
+TRY 121:[125:java/lang/Throwable  , 130:java/lang/Throwable  ]
+TRY 123:[130:java/lang/Throwable  ]
+TRY 127:[125:java/lang/Throwable  , 130:java/lang/Throwable  ]
+TRY 129:[130:java/lang/Throwable  ]
+TRY 132:[130:java/lang/Throwable  ]
@@ -66,0 +67,2 @@
+ASTORE 3
+ALOAD 3
@@ -68 +69,0 @@
-ASTORE 3
@@ -70,3 +70,0 @@
-ALOAD 4
-DUP
-ASTORE 5
@@ -81,0 +80,15 @@
+ASTORE 5
+ALOAD 5
+IFNONNULL L48
+CONST null
+ALOAD 4
+MONITOREXIT
+ARETURN
+ALOAD 5
+ILOAD 2
+CONST Integer:3
+IF_ICMPNE L54
+CONST str:"questEncounter"
+GOTO L55
+CONST str:"timeAttackEncounter"
+INVOKESTATIC local/lito381/server/CombatAccess.field(Ljava/lang/Object;Ljava/lang/String;)Ljava/lang/Object;
@@ -84 +97 @@
-IFNONNULL L50
+IFNONNULL L63
@@ -86 +99 @@
-ALOAD 5
+ALOAD 4
@@ -90,7 +103 @@
-ILOAD 2
-CONST Integer:3
-IF_ICMPNE L56
-CONST str:"questEncounter"
-GOTO L57
-CONST str:"timeAttackEncounter"
-INVOKESTATIC local/lito381/server/CombatAccess.field(Ljava/lang/Object;Ljava/lang/String;)Ljava/lang/Object;
+DUP
@@ -98,11 +104,0 @@
-ALOAD 7
-IFNONNULL L65
-CONST null
-ALOAD 5
-MONITOREXIT
-ARETURN
-ALOAD 7
-ASTORE 8
-ALOAD 8
-DUP
-ASTORE 9
@@ -110 +106 @@
-ALOAD 7
+ALOAD 6
@@ -114 +110 @@
-IF_ICMPNE L78
+IF_ICMPNE L74
@@ -116 +112 @@
-GOTO L79
+GOTO L75
@@ -128,2 +124,2 @@
-ASTORE 10
-ALOAD 10
+ASTORE 8
+ALOAD 8
@@ -130,0 +127,4 @@
+ALOAD 6
+INVOKESTATIC local/lito381/server/NativeSourceNpcTickView.encounter(Ljava/lang/Object;Llocal/lito381/server/GameHub$MonsterAiTick;Ljava/lang/Object;)Z
+IFNE L97
+CONST null
@@ -132,4 +131,0 @@
-INVOKESTATIC local/lito381/server/NativeSourceNpcTickView.encounter(Ljava/lang/Object;Llocal/lito381/server/GameHub$MonsterAiTick;Ljava/lang/Object;)Z
-IFNE L101
-CONST null
-ALOAD 9
@@ -137 +133 @@
-ALOAD 5
+ALOAD 4
@@ -140 +136 @@
-ALOAD 10
+ALOAD 8
@@ -144 +140 @@
-ASTORE 11
+ASTORE 9
@@ -150 +146 @@
-ALOAD 11
+ALOAD 9
@@ -163 +159 @@
-ALOAD 9
+ALOAD 7
@@ -165 +161 @@
-ALOAD 5
+ALOAD 4
@@ -168,2 +164,2 @@
-ASTORE 12
-ALOAD 9
+ASTORE 10
+ALOAD 7
@@ -171 +167 @@
-ALOAD 12
+ALOAD 10
@@ -173,2 +169,2 @@
-ASTORE 13
-ALOAD 5
+ASTORE 11
+ALOAD 4
@@ -176 +172 @@
-ALOAD 13
+ALOAD 11
--- staged/NativeCombatAttacker/live-f6f58683.txt
+++ candidate/NativeCombatAttacker/live-f6f58683.txt
@@ -8,3 +8,2 @@
-TRY 12:[18:java/lang/Throwable  ]
-TRY 16:[18:java/lang/Throwable  ]
-TRY 20:[18:java/lang/Throwable  ]
+TRY 10:[16:java/lang/Throwable  ]
+TRY 13:[16:java/lang/Throwable  ]
@@ -13 +12 @@
-IFEQ L23
+IFEQ L21
@@ -15,0 +15 @@
+DUP
@@ -17,3 +16,0 @@
-ALOAD 2
-DUP
-ASTORE 3
@@ -23,0 +21,5 @@
+ASTORE 3
+ALOAD 2
+MONITOREXIT
+ALOAD 3
+ARETURN
@@ -24,0 +27,2 @@
+ALOAD 2
+MONITOREXIT
@@ -26,7 +29,0 @@
-ALOAD 3
-MONITOREXIT
-ARETURN
-ASTORE 5
-ALOAD 3
-MONITOREXIT
-ALOAD 5
--- staged/NativeCombatAttacker/nativeCombatSceneOriginalDebit-c461ab7.txt
+++ candidate/NativeCombatAttacker/nativeCombatSceneOriginalDebit-c461ab7.txt
@@ -8,53 +8,53 @@
-TRY 29:[72:java/lang/Throwable  ]
-TRY 30:[72:java/lang/Throwable  ]
-TRY 33:[72:java/lang/Throwable  ]
-TRY 36:[72:java/lang/Throwable  ]
-TRY 39:[72:java/lang/Throwable  ]
-TRY 44:[72:java/lang/Throwable  ]
-TRY 45:[72:java/lang/Throwable  ]
-TRY 49:[72:java/lang/Throwable  ]
-TRY 53:[72:java/lang/Throwable  ]
-TRY 54:[72:java/lang/Throwable  ]
-TRY 56:[72:java/lang/Throwable  ]
-TRY 63:[72:java/lang/Throwable  ]
-TRY 66:[72:java/lang/Throwable  ]
-TRY 67:[72:java/lang/Throwable  ]
-TRY 70:[72:java/lang/Throwable  ]
-TRY 74:[72:java/lang/Throwable  ]
-TRY 85:[190:java/lang/Throwable  ]
-TRY 91:[190:java/lang/Throwable  ]
-TRY 94:[190:java/lang/Throwable  ]
-TRY 99:[190:java/lang/Throwable  ]
-TRY 102:[185:java/lang/Throwable  , 190:java/lang/Throwable  ]
-TRY 104:[185:java/lang/Throwable  , 190:java/lang/Throwable  ]
-TRY 105:[185:java/lang/Throwable  , 190:java/lang/Throwable  ]
-TRY 109:[185:java/lang/Throwable  , 190:java/lang/Throwable  ]
-TRY 111:[190:java/lang/Throwable  ]
-TRY 114:[185:java/lang/Throwable  , 190:java/lang/Throwable  ]
-TRY 118:[185:java/lang/Throwable  , 190:java/lang/Throwable  ]
-TRY 119:[185:java/lang/Throwable  , 190:java/lang/Throwable  ]
-TRY 121:[185:java/lang/Throwable  , 190:java/lang/Throwable  ]
-TRY 123:[185:java/lang/Throwable  , 190:java/lang/Throwable  ]
-TRY 125:[185:java/lang/Throwable  , 190:java/lang/Throwable  ]
-TRY 132:[185:java/lang/Throwable  , 190:java/lang/Throwable  ]
-TRY 136:[185:java/lang/Throwable  , 190:java/lang/Throwable  ]
-TRY 138:[190:java/lang/Throwable  ]
-TRY 141:[185:java/lang/Throwable  , 190:java/lang/Throwable  ]
-TRY 143:[185:java/lang/Throwable  , 190:java/lang/Throwable  ]
-TRY 144:[185:java/lang/Throwable  , 190:java/lang/Throwable  ]
-TRY 145:[185:java/lang/Throwable  , 190:java/lang/Throwable  ]
-TRY 148:[185:java/lang/Throwable  , 190:java/lang/Throwable  ]
-TRY 153:[185:java/lang/Throwable  , 190:java/lang/Throwable  ]
-TRY 156:[185:java/lang/Throwable  , 190:java/lang/Throwable  ]
-TRY 159:[185:java/lang/Throwable  , 190:java/lang/Throwable  ]
-TRY 163:[185:java/lang/Throwable  , 190:java/lang/Throwable  ]
-TRY 166:[185:java/lang/Throwable  , 190:java/lang/Throwable  ]
-TRY 169:[185:java/lang/Throwable  , 190:java/lang/Throwable  ]
-TRY 177:[185:java/lang/Throwable  , 190:java/lang/Throwable  ]
-TRY 178:[185:java/lang/Throwable  , 190:java/lang/Throwable  ]
-TRY 179:[185:java/lang/Throwable  , 190:java/lang/Throwable  ]
-TRY 181:[185:java/lang/Throwable  , 190:java/lang/Throwable  ]
-TRY 183:[190:java/lang/Throwable  ]
-TRY 187:[185:java/lang/Throwable  , 190:java/lang/Throwable  ]
-TRY 189:[190:java/lang/Throwable  ]
-TRY 192:[190:java/lang/Throwable  ]
+TRY 27:[70:java/lang/Throwable  ]
+TRY 28:[70:java/lang/Throwable  ]
+TRY 31:[70:java/lang/Throwable  ]
+TRY 34:[70:java/lang/Throwable  ]
+TRY 37:[70:java/lang/Throwable  ]
+TRY 42:[70:java/lang/Throwable  ]
+TRY 43:[70:java/lang/Throwable  ]
+TRY 47:[70:java/lang/Throwable  ]
+TRY 51:[70:java/lang/Throwable  ]
+TRY 52:[70:java/lang/Throwable  ]
+TRY 54:[70:java/lang/Throwable  ]
+TRY 61:[70:java/lang/Throwable  ]
+TRY 64:[70:java/lang/Throwable  ]
+TRY 65:[70:java/lang/Throwable  ]
+TRY 68:[70:java/lang/Throwable  ]
+TRY 72:[70:java/lang/Throwable  ]
+TRY 81:[182:java/lang/Throwable  ]
+TRY 87:[182:java/lang/Throwable  ]
+TRY 90:[182:java/lang/Throwable  ]
+TRY 93:[182:java/lang/Throwable  ]
+TRY 96:[177:java/lang/Throwable  , 182:java/lang/Throwable  ]
+TRY 98:[177:java/lang/Throwable  , 182:java/lang/Throwable  ]
+TRY 99:[177:java/lang/Throwable  , 182:java/lang/Throwable  ]
+TRY 103:[177:java/lang/Throwable  , 182:java/lang/Throwable  ]
+TRY 105:[182:java/lang/Throwable  ]
+TRY 108:[177:java/lang/Throwable  , 182:java/lang/Throwable  ]
+TRY 112:[177:java/lang/Throwable  , 182:java/lang/Throwable  ]
+TRY 113:[177:java/lang/Throwable  , 182:java/lang/Throwable  ]
+TRY 115:[177:java/lang/Throwable  , 182:java/lang/Throwable  ]
+TRY 117:[177:java/lang/Throwable  , 182:java/lang/Throwable  ]
+TRY 119:[177:java/lang/Throwable  , 182:java/lang/Throwable  ]
+TRY 126:[177:java/lang/Throwable  , 182:java/lang/Throwable  ]
+TRY 128:[177:java/lang/Throwable  , 182:java/lang/Throwable  ]
+TRY 130:[182:java/lang/Throwable  ]
+TRY 133:[177:java/lang/Throwable  , 182:java/lang/Throwable  ]
+TRY 135:[177:java/lang/Throwable  , 182:java/lang/Throwable  ]
+TRY 136:[177:java/lang/Throwable  , 182:java/lang/Throwable  ]
+TRY 137:[177:java/lang/Throwable  , 182:java/lang/Throwable  ]
+TRY 140:[177:java/lang/Throwable  , 182:java/lang/Throwable  ]
+TRY 145:[177:java/lang/Throwable  , 182:java/lang/Throwable  ]
+TRY 148:[177:java/lang/Throwable  , 182:java/lang/Throwable  ]
+TRY 151:[177:java/lang/Throwable  , 182:java/lang/Throwable  ]
+TRY 155:[177:java/lang/Throwable  , 182:java/lang/Throwable  ]
+TRY 158:[177:java/lang/Throwable  , 182:java/lang/Throwable  ]
+TRY 161:[177:java/lang/Throwable  , 182:java/lang/Throwable  ]
+TRY 169:[177:java/lang/Throwable  , 182:java/lang/Throwable  ]
+TRY 170:[177:java/lang/Throwable  , 182:java/lang/Throwable  ]
+TRY 171:[177:java/lang/Throwable  , 182:java/lang/Throwable  ]
+TRY 173:[177:java/lang/Throwable  , 182:java/lang/Throwable  ]
+TRY 175:[182:java/lang/Throwable  ]
+TRY 179:[177:java/lang/Throwable  , 182:java/lang/Throwable  ]
+TRY 181:[182:java/lang/Throwable  ]
+TRY 184:[182:java/lang/Throwable  ]
@@ -81 +81 @@
-IF_ICMPNE L77
+IF_ICMPNE L75
@@ -83,0 +84 @@
+DUP
@@ -85,3 +85,0 @@
-ALOAD 5
-DUP
-ASTORE 6
@@ -91,0 +90,3 @@
+ASTORE 6
+ALOAD 6
+INVOKEVIRTUAL local/lito381/server/BattleCompanion.snapshot()Llocal/lito381/server/BattleCompanion$Snapshot;
@@ -94,3 +94,0 @@
-INVOKEVIRTUAL local/lito381/server/BattleCompanion.snapshot()Llocal/lito381/server/BattleCompanion$Snapshot;
-ASTORE 8
-ALOAD 8
@@ -98,2 +96,2 @@
-IFEQ L47
-ALOAD 8
+IFEQ L45
+ALOAD 7
@@ -101,3 +99,3 @@
-IFLE L47
-ALOAD 1
-ALOAD 7
+IFLE L45
+ALOAD 1
+ALOAD 6
@@ -107,7 +105,7 @@
-IFNE L51
-CONST Integer:0
-ALOAD 6
-MONITOREXIT
-IRETURN
-CONST Integer:0
-ALOAD 8
+IFNE L49
+CONST Integer:0
+ALOAD 5
+MONITOREXIT
+IRETURN
+CONST Integer:0
+ALOAD 7
@@ -118,4 +116,4 @@
-ISTORE 9
-ILOAD 9
-IFLE L68
-ALOAD 7
+ISTORE 8
+ILOAD 8
+IFLE L66
+ALOAD 6
@@ -123 +121 @@
-ALOAD 8
+ALOAD 7
@@ -125 +123 @@
-ILOAD 9
+ILOAD 8
@@ -129,8 +127,8 @@
-ILOAD 9
-ALOAD 6
-MONITOREXIT
-IRETURN
-ASTORE 10
-ALOAD 6
-MONITOREXIT
-ALOAD 10
+ILOAD 8
+ALOAD 5
+MONITOREXIT
+IRETURN
+ASTORE 9
+ALOAD 5
+MONITOREXIT
+ALOAD 9
@@ -138,0 +137,6 @@
+DUP
+ASTORE 10
+MONITORENTER
+ALOAD 0
+ALOAD 1
+INVOKESTATIC local/lito381/server/NativeCombatAttacker.live(Llocal/lito381/server/ClientSession;Llocal/lito381/server/NativeCombatAttacker$Ref;)Llocal/lito381/server/NativeCombatAttacker$Live;
@@ -140,0 +145,7 @@
+IFNONNULL L89
+CONST Integer:0
+ALOAD 10
+MONITOREXIT
+IRETURN
+ALOAD 11
+GETFIELD local/lito381/server/NativeCombatAttacker$Live.container:Ljava/lang/Object;
@@ -144,3 +155,26 @@
-ALOAD 0
-ALOAD 1
-INVOKESTATIC local/lito381/server/NativeCombatAttacker.live(Llocal/lito381/server/ClientSession;Llocal/lito381/server/NativeCombatAttacker$Ref;)Llocal/lito381/server/NativeCombatAttacker$Live;
+ALOAD 1
+ALOAD 11
+GETFIELD local/lito381/server/NativeCombatAttacker$Live.actor:Ljava/lang/Object;
+ALOAD 11
+GETFIELD local/lito381/server/NativeCombatAttacker$Live.timed:Llocal/lito381/server/BattleTimedStatus;
+INVOKESTATIC local/lito381/server/NativeCombatAttacker.same(Llocal/lito381/server/NativeCombatAttacker$Ref;Ljava/lang/Object;Llocal/lito381/server/BattleTimedStatus;)Z
+IFNE L107
+CONST Integer:0
+ALOAD 12
+MONITOREXIT
+ALOAD 10
+MONITOREXIT
+IRETURN
+ALOAD 1
+GETFIELD local/lito381/server/NativeCombatAttacker$Ref.kind:I
+CONST Integer:1
+IF_ICMPNE L132
+ALOAD 11
+GETFIELD local/lito381/server/NativeCombatAttacker$Live.container:Ljava/lang/Object;
+CHECKCAST local/lito381/server/ClassicWorldMonsterPopulation
+ALOAD 1
+GETFIELD local/lito381/server/NativeCombatAttacker$Ref.map:I
+ALOAD 1
+GETFIELD local/lito381/server/NativeCombatAttacker$Ref.id:J
+ILOAD 2
+INVOKEVIRTUAL local/lito381/server/ClassicWorldMonsterPopulation.drainMana(IJI)Llocal/lito381/server/ClassicWorldMonsterPopulation$ManaDrain;
@@ -149,5 +183,3 @@
-IFNONNULL L93
-CONST Integer:0
-ALOAD 12
-MONITOREXIT
-IRETURN
+IFNONNULL L125
+CONST Integer:0
+GOTO L127
@@ -155,38 +186,0 @@
-GETFIELD local/lito381/server/NativeCombatAttacker$Live.container:Ljava/lang/Object;
-ASTORE 14
-ALOAD 14
-DUP
-ASTORE 15
-MONITORENTER
-ALOAD 1
-ALOAD 13
-GETFIELD local/lito381/server/NativeCombatAttacker$Live.actor:Ljava/lang/Object;
-ALOAD 13
-GETFIELD local/lito381/server/NativeCombatAttacker$Live.timed:Llocal/lito381/server/BattleTimedStatus;
-INVOKESTATIC local/lito381/server/NativeCombatAttacker.same(Llocal/lito381/server/NativeCombatAttacker$Ref;Ljava/lang/Object;Llocal/lito381/server/BattleTimedStatus;)Z
-IFNE L113
-CONST Integer:0
-ALOAD 15
-MONITOREXIT
-ALOAD 12
-MONITOREXIT
-IRETURN
-ALOAD 1
-GETFIELD local/lito381/server/NativeCombatAttacker$Ref.kind:I
-CONST Integer:1
-IF_ICMPNE L140
-ALOAD 13
-GETFIELD local/lito381/server/NativeCombatAttacker$Live.container:Ljava/lang/Object;
-CHECKCAST local/lito381/server/ClassicWorldMonsterPopulation
-ALOAD 1
-GETFIELD local/lito381/server/NativeCombatAttacker$Ref.map:I
-ALOAD 1
-GETFIELD local/lito381/server/NativeCombatAttacker$Ref.id:J
-ILOAD 2
-INVOKEVIRTUAL local/lito381/server/ClassicWorldMonsterPopulation.drainMana(IJI)Llocal/lito381/server/ClassicWorldMonsterPopulation$ManaDrain;
-ASTORE 16
-ALOAD 16
-IFNONNULL L131
-CONST Integer:0
-GOTO L133
-ALOAD 16
@@ -194,8 +188,6 @@
-ISTORE 17
-ILOAD 17
-ALOAD 15
-MONITOREXIT
-ALOAD 12
-MONITOREXIT
-IRETURN
-ALOAD 13
+ALOAD 12
+MONITOREXIT
+ALOAD 10
+MONITOREXIT
+IRETURN
+ALOAD 11
@@ -207 +199 @@
-ISTORE 18
+ISTORE 14
@@ -211 +203 @@
-IF_ICMPNE L161
+IF_ICMPNE L153
@@ -218 +210 @@
-ILOAD 18
+ILOAD 14
@@ -221 +213 @@
-GOTO L170
+GOTO L162
@@ -228 +220 @@
-ILOAD 18
+ILOAD 14
@@ -231,6 +223,6 @@
-ASTORE 19
-ALOAD 19
-IFNONNULL L175
-CONST Integer:0
-GOTO L180
-ALOAD 19
+ASTORE 15
+ALOAD 15
+IFNONNULL L167
+CONST Integer:0
+GOTO L172
+ALOAD 15
@@ -241,9 +233,9 @@
-ALOAD 15
-MONITOREXIT
-ALOAD 12
-MONITOREXIT
-IRETURN
-ASTORE 20
-ALOAD 15
-MONITOREXIT
-ALOAD 20
+ALOAD 12
+MONITOREXIT
+ALOAD 10
+MONITOREXIT
+IRETURN
+ASTORE 16
+ALOAD 12
+MONITOREXIT
+ALOAD 16
@@ -251,4 +243,4 @@
-ASTORE 21
-ALOAD 12
-MONITOREXIT
-ALOAD 21
+ASTORE 17
+ALOAD 10
+MONITOREXIT
+ALOAD 17
--- staged/NativeCombatAttacker/nativeRefOriginalCompanion-f64fc43.txt
+++ candidate/NativeCombatAttacker/nativeRefOriginalCompanion-f64fc43.txt
@@ -8,15 +8,15 @@
-TRY 16:[53:java/lang/Throwable  ]
-TRY 19:[53:java/lang/Throwable  ]
-TRY 22:[53:java/lang/Throwable  ]
-TRY 26:[53:java/lang/Throwable  ]
-TRY 30:[53:java/lang/Throwable  ]
-TRY 31:[53:java/lang/Throwable  ]
-TRY 33:[53:java/lang/Throwable  ]
-TRY 37:[53:java/lang/Throwable  ]
-TRY 40:[53:java/lang/Throwable  ]
-TRY 44:[53:java/lang/Throwable  ]
-TRY 46:[53:java/lang/Throwable  ]
-TRY 48:[53:java/lang/Throwable  ]
-TRY 49:[53:java/lang/Throwable  ]
-TRY 51:[53:java/lang/Throwable  ]
-TRY 55:[53:java/lang/Throwable  ]
+TRY 14:[51:java/lang/Throwable  ]
+TRY 17:[51:java/lang/Throwable  ]
+TRY 20:[51:java/lang/Throwable  ]
+TRY 24:[51:java/lang/Throwable  ]
+TRY 28:[51:java/lang/Throwable  ]
+TRY 29:[51:java/lang/Throwable  ]
+TRY 31:[51:java/lang/Throwable  ]
+TRY 35:[51:java/lang/Throwable  ]
+TRY 38:[51:java/lang/Throwable  ]
+TRY 42:[51:java/lang/Throwable  ]
+TRY 44:[51:java/lang/Throwable  ]
+TRY 46:[51:java/lang/Throwable  ]
+TRY 47:[51:java/lang/Throwable  ]
+TRY 49:[51:java/lang/Throwable  ]
+TRY 53:[51:java/lang/Throwable  ]
@@ -32,0 +33 @@
+DUP
@@ -34,3 +34,0 @@
-ALOAD 3
-DUP
-ASTORE 4
@@ -40,2 +38,2 @@
-ASTORE 5
-ALOAD 5
+ASTORE 4
+ALOAD 4
@@ -43,2 +41,2 @@
-IFEQ L24
-ALOAD 5
+IFEQ L22
+ALOAD 4
@@ -46 +44 @@
-IFGT L28
+IFGT L26
@@ -48 +46 @@
-ALOAD 4
+ALOAD 3
@@ -55 +53 @@
-ASTORE 6
+ASTORE 5
@@ -58,0 +57,2 @@
+ALOAD 4
+INVOKEVIRTUAL local/lito381/server/BattleCompanion$Snapshot.objectId()J
@@ -60,2 +59,0 @@
-INVOKEVIRTUAL local/lito381/server/BattleCompanion$Snapshot.objectId()J
-ALOAD 6
@@ -73 +71 @@
-ALOAD 4
+ALOAD 3
@@ -76,2 +74,2 @@
-ASTORE 7
-ALOAD 4
+ASTORE 6
+ALOAD 3
@@ -79 +77 @@
-ALOAD 7
+ALOAD 6
--- staged/NativeCombatAttacker/nativeRefOriginalLive-f6f58683.txt
+++ candidate/NativeCombatAttacker/nativeRefOriginalLive-f6f58683.txt
@@ -8,90 +8,81 @@
-TRY 47:[95:java/lang/Throwable  ]
-TRY 51:[95:java/lang/Throwable  ]
-TRY 52:[95:java/lang/Throwable  ]
-TRY 55:[95:java/lang/Throwable  ]
-TRY 59:[95:java/lang/Throwable  ]
-TRY 62:[95:java/lang/Throwable  ]
-TRY 66:[95:java/lang/Throwable  ]
-TRY 72:[95:java/lang/Throwable  ]
-TRY 75:[95:java/lang/Throwable  ]
-TRY 79:[95:java/lang/Throwable  ]
-TRY 81:[95:java/lang/Throwable  ]
-TRY 89:[95:java/lang/Throwable  ]
-TRY 93:[95:java/lang/Throwable  ]
-TRY 97:[95:java/lang/Throwable  ]
-TRY 107:[329:java/lang/Throwable  ]
-TRY 112:[329:java/lang/Throwable  ]
-TRY 114:[329:java/lang/Throwable  ]
-TRY 116:[329:java/lang/Throwable  ]
-TRY 117:[329:java/lang/Throwable  ]
-TRY 123:[329:java/lang/Throwable  ]
-TRY 126:[329:java/lang/Throwable  ]
-TRY 131:[329:java/lang/Throwable  ]
-TRY 133:[206:java/lang/Throwable  , 329:java/lang/Throwable  ]
-TRY 135:[206:java/lang/Throwable  , 329:java/lang/Throwable  ]
-TRY 136:[206:java/lang/Throwable  , 329:java/lang/Throwable  ]
-TRY 137:[206:java/lang/Throwable  , 329:java/lang/Throwable  ]
-TRY 141:[206:java/lang/Throwable  , 329:java/lang/Throwable  ]
-TRY 143:[329:java/lang/Throwable  ]
-TRY 146:[206:java/lang/Throwable  , 329:java/lang/Throwable  ]
-TRY 148:[206:java/lang/Throwable  , 329:java/lang/Throwable  ]
-TRY 149:[206:java/lang/Throwable  , 329:java/lang/Throwable  ]
-TRY 150:[206:java/lang/Throwable  , 329:java/lang/Throwable  ]
-TRY 154:[206:java/lang/Throwable  , 329:java/lang/Throwable  ]
-TRY 156:[329:java/lang/Throwable  ]
-TRY 160:[206:java/lang/Throwable  , 329:java/lang/Throwable  ]
-TRY 162:[206:java/lang/Throwable  , 329:java/lang/Throwable  ]
-TRY 163:[206:java/lang/Throwable  , 329:java/lang/Throwable  ]
-TRY 167:[206:java/lang/Throwable  , 329:java/lang/Throwable  ]
-TRY 169:[329:java/lang/Throwable  ]
-TRY 172:[206:java/lang/Throwable  , 329:java/lang/Throwable  ]
-TRY 174:[206:java/lang/Throwable  , 329:java/lang/Throwable  ]
-TRY 176:[206:java/lang/Throwable  , 329:java/lang/Throwable  ]
-TRY 177:[206:java/lang/Throwable  , 329:java/lang/Throwable  ]
-TRY 183:[206:java/lang/Throwable  , 329:java/lang/Throwable  ]
-TRY 185:[329:java/lang/Throwable  ]
-TRY 187:[206:java/lang/Throwable  , 329:java/lang/Throwable  ]
-TRY 191:[206:java/lang/Throwable  , 329:java/lang/Throwable  ]
-TRY 193:[206:java/lang/Throwable  , 329:java/lang/Throwable  ]
-TRY 195:[206:java/lang/Throwable  , 329:java/lang/Throwable  ]
-TRY 198:[206:java/lang/Throwable  , 329:java/lang/Throwable  ]
-TRY 202:[206:java/lang/Throwable  , 329:java/lang/Throwable  ]
-TRY 204:[329:java/lang/Throwable  ]
-TRY 208:[206:java/lang/Throwable  , 329:java/lang/Throwable  ]
-TRY 210:[329:java/lang/Throwable  ]
-TRY 213:[329:java/lang/Throwable  ]
-TRY 214:[329:java/lang/Throwable  ]
-TRY 216:[329:java/lang/Throwable  ]
-TRY 217:[329:java/lang/Throwable  ]
-TRY 218:[329:java/lang/Throwable  ]
-TRY 224:[329:java/lang/Throwable  ]
-TRY 228:[329:java/lang/Throwable  ]
-TRY 234:[329:java/lang/Throwable  ]
-TRY 240:[329:java/lang/Throwable  ]
-TRY 247:[329:java/lang/Throwable  ]
-TRY 251:[324:java/lang/Throwable  , 329:java/lang/Throwable  ]
-TRY 257:[324:java/lang/Throwable  , 329:java/lang/Throwable  ]
-TRY 259:[324:java/lang/Throwable  , 329:java/lang/Throwable  ]
-TRY 263:[324:java/lang/Throwable  , 329:java/lang/Throwable  ]
-TRY 264:[324:java/lang/Throwable  , 329:java/lang/Throwable  ]
-TRY 265:[324:java/lang/Throwable  , 329:java/lang/Throwable  ]
-TRY 266:[324:java/lang/Throwable  , 329:java/lang/Throwable  ]
-TRY 272:[324:java/lang/Throwable  , 329:java/lang/Throwable  ]
-TRY 274:[329:java/lang/Throwable  ]
-TRY 278:[324:java/lang/Throwable  , 329:java/lang/Throwable  ]
-TRY 279:[324:java/lang/Throwable  , 329:java/lang/Throwable  ]
-TRY 284:[324:java/lang/Throwable  , 329:java/lang/Throwable  ]
-TRY 288:[324:java/lang/Throwable  , 329:java/lang/Throwable  ]
-TRY 292:[324:java/lang/Throwable  , 329:java/lang/Throwable  ]
-TRY 294:[329:java/lang/Throwable  ]
-TRY 299:[324:java/lang/Throwable  , 329:java/lang/Throwable  ]
-TRY 305:[324:java/lang/Throwable  , 329:java/lang/Throwable  ]
-TRY 307:[324:java/lang/Throwable  , 329:java/lang/Throwable  ]
-TRY 308:[324:java/lang/Throwable  , 329:java/lang/Throwable  ]
-TRY 310:[324:java/lang/Throwable  , 329:java/lang/Throwable  ]
-TRY 318:[324:java/lang/Throwable  , 329:java/lang/Throwable  ]
-TRY 320:[324:java/lang/Throwable  , 329:java/lang/Throwable  ]
-TRY 322:[329:java/lang/Throwable  ]
-TRY 326:[324:java/lang/Throwable  , 329:java/lang/Throwable  ]
-TRY 328:[329:java/lang/Throwable  ]
-TRY 331:[329:java/lang/Throwable  ]
+TRY 45:[81:java/lang/Throwable  ]
+TRY 49:[81:java/lang/Throwable  ]
+TRY 50:[81:java/lang/Throwable  ]
+TRY 53:[81:java/lang/Throwable  ]
+TRY 56:[81:java/lang/Throwable  ]
+TRY 62:[81:java/lang/Throwable  ]
+TRY 65:[81:java/lang/Throwable  ]
+TRY 67:[81:java/lang/Throwable  ]
+TRY 75:[81:java/lang/Throwable  ]
+TRY 79:[81:java/lang/Throwable  ]
+TRY 83:[81:java/lang/Throwable  ]
+TRY 91:[291:java/lang/Throwable  ]
+TRY 96:[291:java/lang/Throwable  ]
+TRY 98:[291:java/lang/Throwable  ]
+TRY 100:[291:java/lang/Throwable  ]
+TRY 101:[291:java/lang/Throwable  ]
+TRY 107:[291:java/lang/Throwable  ]
+TRY 110:[291:java/lang/Throwable  ]
+TRY 113:[291:java/lang/Throwable  ]
+TRY 115:[170:java/lang/Throwable  , 291:java/lang/Throwable  ]
+TRY 117:[170:java/lang/Throwable  , 291:java/lang/Throwable  ]
+TRY 118:[170:java/lang/Throwable  , 291:java/lang/Throwable  ]
+TRY 119:[170:java/lang/Throwable  , 291:java/lang/Throwable  ]
+TRY 122:[170:java/lang/Throwable  , 291:java/lang/Throwable  ]
+TRY 124:[170:java/lang/Throwable  , 291:java/lang/Throwable  ]
+TRY 125:[170:java/lang/Throwable  , 291:java/lang/Throwable  ]
+TRY 126:[170:java/lang/Throwable  , 291:java/lang/Throwable  ]
+TRY 130:[170:java/lang/Throwable  , 291:java/lang/Throwable  ]
+TRY 132:[170:java/lang/Throwable  , 291:java/lang/Throwable  ]
+TRY 133:[170:java/lang/Throwable  , 291:java/lang/Throwable  ]
+TRY 137:[170:java/lang/Throwable  , 291:java/lang/Throwable  ]
+TRY 139:[291:java/lang/Throwable  ]
+TRY 142:[170:java/lang/Throwable  , 291:java/lang/Throwable  ]
+TRY 144:[170:java/lang/Throwable  , 291:java/lang/Throwable  ]
+TRY 146:[170:java/lang/Throwable  , 291:java/lang/Throwable  ]
+TRY 147:[170:java/lang/Throwable  , 291:java/lang/Throwable  ]
+TRY 153:[170:java/lang/Throwable  , 291:java/lang/Throwable  ]
+TRY 157:[170:java/lang/Throwable  , 291:java/lang/Throwable  ]
+TRY 159:[170:java/lang/Throwable  , 291:java/lang/Throwable  ]
+TRY 161:[170:java/lang/Throwable  , 291:java/lang/Throwable  ]
+TRY 164:[170:java/lang/Throwable  , 291:java/lang/Throwable  ]
+TRY 166:[170:java/lang/Throwable  , 291:java/lang/Throwable  ]
+TRY 168:[291:java/lang/Throwable  ]
+TRY 172:[170:java/lang/Throwable  , 291:java/lang/Throwable  ]
+TRY 174:[291:java/lang/Throwable  ]
+TRY 177:[291:java/lang/Throwable  ]
+TRY 178:[291:java/lang/Throwable  ]
+TRY 180:[291:java/lang/Throwable  ]
+TRY 181:[291:java/lang/Throwable  ]
+TRY 182:[291:java/lang/Throwable  ]
+TRY 188:[291:java/lang/Throwable  ]
+TRY 192:[291:java/lang/Throwable  ]
+TRY 198:[291:java/lang/Throwable  ]
+TRY 204:[291:java/lang/Throwable  ]
+TRY 209:[291:java/lang/Throwable  ]
+TRY 213:[286:java/lang/Throwable  , 291:java/lang/Throwable  ]
+TRY 219:[286:java/lang/Throwable  , 291:java/lang/Throwable  ]
+TRY 221:[286:java/lang/Throwable  , 291:java/lang/Throwable  ]
+TRY 225:[286:java/lang/Throwable  , 291:java/lang/Throwable  ]
+TRY 226:[286:java/lang/Throwable  , 291:java/lang/Throwable  ]
+TRY 227:[286:java/lang/Throwable  , 291:java/lang/Throwable  ]
+TRY 228:[286:java/lang/Throwable  , 291:java/lang/Throwable  ]
+TRY 234:[286:java/lang/Throwable  , 291:java/lang/Throwable  ]
+TRY 236:[291:java/lang/Throwable  ]
+TRY 240:[286:java/lang/Throwable  , 291:java/lang/Throwable  ]
+TRY 241:[286:java/lang/Throwable  , 291:java/lang/Throwable  ]
+TRY 246:[286:java/lang/Throwable  , 291:java/lang/Throwable  ]
+TRY 250:[286:java/lang/Throwable  , 291:java/lang/Throwable  ]
+TRY 254:[286:java/lang/Throwable  , 291:java/lang/Throwable  ]
+TRY 256:[291:java/lang/Throwable  ]
+TRY 261:[286:java/lang/Throwable  , 291:java/lang/Throwable  ]
+TRY 267:[286:java/lang/Throwable  , 291:java/lang/Throwable  ]
+TRY 269:[286:java/lang/Throwable  , 291:java/lang/Throwable  ]
+TRY 270:[286:java/lang/Throwable  , 291:java/lang/Throwable  ]
+TRY 272:[286:java/lang/Throwable  , 291:java/lang/Throwable  ]
+TRY 280:[286:java/lang/Throwable  , 291:java/lang/Throwable  ]
+TRY 282:[286:java/lang/Throwable  , 291:java/lang/Throwable  ]
+TRY 284:[291:java/lang/Throwable  ]
+TRY 288:[286:java/lang/Throwable  , 291:java/lang/Throwable  ]
+TRY 290:[291:java/lang/Throwable  ]
+TRY 293:[291:java/lang/Throwable  ]
@@ -110 +101 @@
-IF_ICMPNE L100
+IF_ICMPNE L86
@@ -138,0 +130 @@
+DUP
@@ -140,2 +132,3 @@
-ALOAD 5
-DUP
+MONITORENTER
+ALOAD 4
+INVOKEVIRTUAL local/lito381/server/BattleCompanion.snapshot()Llocal/lito381/server/BattleCompanion$Snapshot;
@@ -143,3 +136,4 @@
-MONITORENTER
-ALOAD 4
-INVOKEVIRTUAL local/lito381/server/BattleCompanion.snapshot()Llocal/lito381/server/BattleCompanion$Snapshot;
+ALOAD 4
+CONST str:"timedStatus"
+INVOKESTATIC local/lito381/server/CombatAccess.field(Ljava/lang/Object;Ljava/lang/String;)Ljava/lang/Object;
+CHECKCAST local/lito381/server/BattleTimedStatus
@@ -147,5 +141,8 @@
-ALOAD 4
-CONST str:"timedStatus"
-INVOKESTATIC local/lito381/server/CombatAccess.field(Ljava/lang/Object;Ljava/lang/String;)Ljava/lang/Object;
-CHECKCAST local/lito381/server/BattleTimedStatus
-ASTORE 8
+ALOAD 6
+INVOKEVIRTUAL local/lito381/server/BattleCompanion$Snapshot.active()Z
+IFEQ L77
+ALOAD 6
+INVOKEVIRTUAL local/lito381/server/BattleCompanion$Snapshot.hp()I
+IFLE L77
+ALOAD 1
+ALOAD 4
@@ -153,16 +149,0 @@
-INVOKEVIRTUAL local/lito381/server/BattleCompanion$Snapshot.active()Z
-IFNE L61
-CONST null
-ALOAD 6
-MONITOREXIT
-ARETURN
-ALOAD 7
-INVOKEVIRTUAL local/lito381/server/BattleCompanion$Snapshot.hp()I
-IFGT L68
-CONST null
-ALOAD 6
-MONITOREXIT
-ARETURN
-ALOAD 1
-ALOAD 4
-ALOAD 8
@@ -174,2 +155,7 @@
-IFNE L81
-CONST null
+IFEQ L77
+NEW local/lito381/server/NativeCombatAttacker$Live
+DUP
+ALOAD 1
+ALOAD 4
+ALOAD 4
+ALOAD 7
@@ -177,9 +162,0 @@
-MONITOREXIT
-ARETURN
-NEW local/lito381/server/NativeCombatAttacker$Live
-DUP
-ALOAD 1
-ALOAD 4
-ALOAD 4
-ALOAD 8
-ALOAD 7
@@ -188,9 +165,9 @@
-ASTORE 9
-ALOAD 9
-ALOAD 6
-MONITOREXIT
-ARETURN
-ASTORE 10
-ALOAD 6
-MONITOREXIT
-ALOAD 10
+GOTO L78
+CONST null
+ALOAD 5
+MONITOREXIT
+ARETURN
+ASTORE 8
+ALOAD 5
+MONITOREXIT
+ALOAD 8
@@ -199,4 +176,2 @@
-ASTORE 11
-ALOAD 11
-DUP
-ASTORE 12
+DUP
+ASTORE 9
@@ -207 +182 @@
-IF_ICMPNE L211
+IF_ICMPNE L175
@@ -216,8 +191,8 @@
-ASTORE 13
-ALOAD 13
-IFNONNULL L125
-CONST null
-ALOAD 12
-MONITOREXIT
-ARETURN
-ALOAD 13
+ASTORE 10
+ALOAD 10
+IFNONNULL L109
+CONST null
+ALOAD 9
+MONITOREXIT
+ARETURN
+ALOAD 10
@@ -225,4 +200,2 @@
-ASTORE 14
-ALOAD 14
-DUP
-ASTORE 15
+DUP
+ASTORE 11
@@ -230 +203 @@
-ALOAD 13
+ALOAD 10
@@ -236,8 +209,2 @@
-IFNE L145
-CONST null
-ALOAD 15
-MONITOREXIT
-ALOAD 12
-MONITOREXIT
-ARETURN
-ALOAD 13
+IFEQ L135
+ALOAD 10
@@ -249,9 +216,3 @@
-IFNE L158
-CONST null
-ALOAD 15
-MONITOREXIT
-ALOAD 12
-MONITOREXIT
-ARETURN
-ALOAD 1
-ALOAD 13
+IFEQ L135
+ALOAD 1
+ALOAD 10
@@ -259 +220 @@
-ALOAD 13
+ALOAD 10
@@ -262,8 +223,8 @@
-IFNE L171
-CONST null
-ALOAD 15
-MONITOREXIT
-ALOAD 12
-MONITOREXIT
-ARETURN
-ALOAD 13
+IFNE L141
+CONST null
+ALOAD 11
+MONITOREXIT
+ALOAD 9
+MONITOREXIT
+ARETURN
+ALOAD 10
@@ -276,6 +237 @@
-ASTORE 16
-ALOAD 16
-IFNONNULL L187
-CONST null
-ALOAD 15
-MONITOREXIT
+ASTORE 12
@@ -283,2 +239,3 @@
-MONITOREXIT
-ARETURN
+IFNONNULL L153
+CONST null
+GOTO L165
@@ -287,0 +245,17 @@
+ALOAD 10
+INVOKEVIRTUAL local/lito381/server/CombatMonsterCurseLifecycle$Actor.population()Llocal/lito381/server/ClassicWorldMonsterPopulation;
+ALOAD 10
+INVOKEVIRTUAL local/lito381/server/CombatMonsterCurseLifecycle$Actor.actor()Ljava/lang/Object;
+ALOAD 10
+INVOKEVIRTUAL local/lito381/server/CombatMonsterCurseLifecycle$Actor.timed()Llocal/lito381/server/BattleTimedStatus;
+ALOAD 12
+CONST null
+INVOKESPECIAL local/lito381/server/NativeCombatAttacker$Live.<init>(Llocal/lito381/server/NativeCombatAttacker$Ref;Ljava/lang/Object;Ljava/lang/Object;Llocal/lito381/server/BattleTimedStatus;Ljava/lang/Object;Llocal/lito381/server/ClientSession;)V
+ALOAD 11
+MONITOREXIT
+ALOAD 9
+MONITOREXIT
+ARETURN
+ASTORE 13
+ALOAD 11
+MONITOREXIT
@@ -289,19 +262,0 @@
-INVOKEVIRTUAL local/lito381/server/CombatMonsterCurseLifecycle$Actor.population()Llocal/lito381/server/ClassicWorldMonsterPopulation;
-ALOAD 13
-INVOKEVIRTUAL local/lito381/server/CombatMonsterCurseLifecycle$Actor.actor()Ljava/lang/Object;
-ALOAD 13
-INVOKEVIRTUAL local/lito381/server/CombatMonsterCurseLifecycle$Actor.timed()Llocal/lito381/server/BattleTimedStatus;
-ALOAD 16
-CONST null
-INVOKESPECIAL local/lito381/server/NativeCombatAttacker$Live.<init>(Llocal/lito381/server/NativeCombatAttacker$Ref;Ljava/lang/Object;Ljava/lang/Object;Llocal/lito381/server/BattleTimedStatus;Ljava/lang/Object;Llocal/lito381/server/ClientSession;)V
-ASTORE 17
-ALOAD 17
-ALOAD 15
-MONITOREXIT
-ALOAD 12
-MONITOREXIT
-ARETURN
-ASTORE 18
-ALOAD 15
-MONITOREXIT
-ALOAD 18
@@ -317,8 +272,8 @@
-ASTORE 19
-ALOAD 19
-IFNONNULL L226
-CONST null
-ALOAD 12
-MONITOREXIT
-ARETURN
-ALOAD 19
+ASTORE 14
+ALOAD 14
+IFNONNULL L190
+CONST null
+ALOAD 9
+MONITOREXIT
+ARETURN
+ALOAD 14
@@ -328 +283 @@
-IF_ICMPNE L233
+IF_ICMPNE L197
@@ -330 +285 @@
-GOTO L234
+GOTO L198
@@ -333,12 +288,10 @@
-ASTORE 20
-ALOAD 20
-IFNONNULL L242
-CONST null
-ALOAD 12
-MONITOREXIT
-ARETURN
-ALOAD 20
-ASTORE 21
-ALOAD 21
-DUP
-ASTORE 22
+ASTORE 15
+ALOAD 15
+IFNONNULL L206
+CONST null
+ALOAD 9
+MONITOREXIT
+ARETURN
+ALOAD 15
+DUP
+ASTORE 16
@@ -346 +299 @@
-ALOAD 20
+ALOAD 15
@@ -351 +304 @@
-IF_ICMPNE L256
+IF_ICMPNE L218
@@ -353 +306 @@
-GOTO L257
+GOTO L219
@@ -365,10 +318,10 @@
-ASTORE 23
-ALOAD 23
-IFNONNULL L276
-CONST null
-ALOAD 22
-MONITOREXIT
-ALOAD 12
-MONITOREXIT
-ARETURN
-ALOAD 23
+ASTORE 17
+ALOAD 17
+IFNONNULL L238
+CONST null
+ALOAD 16
+MONITOREXIT
+ALOAD 9
+MONITOREXIT
+ARETURN
+ALOAD 17
@@ -378,4 +331,4 @@
-ASTORE 24
-ALOAD 1
-ALOAD 23
-ALOAD 24
+ASTORE 18
+ALOAD 1
+ALOAD 17
+ALOAD 18
@@ -384,2 +337,2 @@
-ALOAD 20
-ALOAD 23
+ALOAD 15
+ALOAD 17
@@ -387,8 +340,8 @@
-IFNE L296
-CONST null
-ALOAD 22
-MONITOREXIT
-ALOAD 12
-MONITOREXIT
-ARETURN
-ALOAD 23
+IFNE L258
+CONST null
+ALOAD 16
+MONITOREXIT
+ALOAD 9
+MONITOREXIT
+ARETURN
+ALOAD 17
@@ -399 +352 @@
-IF_ICMPNE L304
+IF_ICMPNE L266
@@ -401 +354 @@
-GOTO L305
+GOTO L267
@@ -407 +360 @@
-ASTORE 25
+ASTORE 19
@@ -410,0 +364,14 @@
+ALOAD 15
+ALOAD 17
+ALOAD 18
+ALOAD 19
+CONST null
+INVOKESPECIAL local/lito381/server/NativeCombatAttacker$Live.<init>(Llocal/lito381/server/NativeCombatAttacker$Ref;Ljava/lang/Object;Ljava/lang/Object;Llocal/lito381/server/BattleTimedStatus;Ljava/lang/Object;Llocal/lito381/server/ClientSession;)V
+ALOAD 16
+MONITOREXIT
+ALOAD 9
+MONITOREXIT
+ARETURN
+ASTORE 20
+ALOAD 16
+MONITOREXIT
@@ -412,14 +378,0 @@
-ALOAD 23
-ALOAD 24
-ALOAD 25
-CONST null
-INVOKESPECIAL local/lito381/server/NativeCombatAttacker$Live.<init>(Llocal/lito381/server/NativeCombatAttacker$Ref;Ljava/lang/Object;Ljava/lang/Object;Llocal/lito381/server/BattleTimedStatus;Ljava/lang/Object;Llocal/lito381/server/ClientSession;)V
-ALOAD 22
-MONITOREXIT
-ALOAD 12
-MONITOREXIT
-ARETURN
-ASTORE 26
-ALOAD 22
-MONITOREXIT
-ALOAD 26
@@ -427,4 +380,4 @@
-ASTORE 27
-ALOAD 12
-MONITOREXIT
-ALOAD 27
+ASTORE 21
+ALOAD 9
+MONITOREXIT
+ALOAD 21
--- staged/NativeCombatAttacker/nativeReflectionOriginalCompanionDamage-22bcde0b.txt
+++ candidate/NativeCombatAttacker/nativeReflectionOriginalCompanionDamage-22bcde0b.txt
@@ -8,14 +8,14 @@
-TRY 18:[53:java/lang/Throwable  ]
-TRY 19:[53:java/lang/Throwable  ]
-TRY 22:[53:java/lang/Throwable  ]
-TRY 26:[53:java/lang/Throwable  ]
-TRY 28:[53:java/lang/Throwable  ]
-TRY 29:[53:java/lang/Throwable  ]
-TRY 35:[53:java/lang/Throwable  ]
-TRY 39:[53:java/lang/Throwable  ]
-TRY 41:[53:java/lang/Throwable  ]
-TRY 44:[53:java/lang/Throwable  ]
-TRY 46:[53:java/lang/Throwable  ]
-TRY 49:[53:java/lang/Throwable  ]
-TRY 51:[53:java/lang/Throwable  ]
-TRY 55:[53:java/lang/Throwable  ]
+TRY 16:[51:java/lang/Throwable  ]
+TRY 17:[51:java/lang/Throwable  ]
+TRY 20:[51:java/lang/Throwable  ]
+TRY 24:[51:java/lang/Throwable  ]
+TRY 26:[51:java/lang/Throwable  ]
+TRY 27:[51:java/lang/Throwable  ]
+TRY 33:[51:java/lang/Throwable  ]
+TRY 37:[51:java/lang/Throwable  ]
+TRY 39:[51:java/lang/Throwable  ]
+TRY 42:[51:java/lang/Throwable  ]
+TRY 44:[51:java/lang/Throwable  ]
+TRY 47:[51:java/lang/Throwable  ]
+TRY 49:[51:java/lang/Throwable  ]
+TRY 53:[51:java/lang/Throwable  ]
@@ -32,0 +33 @@
+DUP
@@ -34,3 +34,0 @@
-ALOAD 3
-DUP
-ASTORE 4
@@ -42 +40 @@
-ASTORE 5
+ASTORE 4
@@ -46 +44 @@
-IF_ICMPNE L41
+IF_ICMPNE L39
@@ -53 +51 @@
-IFNE L41
+IFNE L39
@@ -56 +54 @@
-ALOAD 5
+ALOAD 4
@@ -58 +56 @@
-IFEQ L41
+IFEQ L39
@@ -62 +60 @@
-GOTO L50
+GOTO L48
@@ -72 +70 @@
-ALOAD 4
+ALOAD 3
@@ -75,2 +73,2 @@
-ASTORE 6
-ALOAD 4
+ASTORE 5
+ALOAD 3
@@ -78 +76 @@
-ALOAD 6
+ALOAD 5
--- staged/NativeCombatAttacker/nativeReflectionOriginalDamage-deb9b8e.txt
+++ candidate/NativeCombatAttacker/nativeReflectionOriginalDamage-deb9b8e.txt
@@ -8,63 +8,63 @@
-TRY 30:[140:java/lang/Throwable  ]
-TRY 32:[140:java/lang/Throwable  ]
-TRY 34:[140:java/lang/Throwable  ]
-TRY 35:[140:java/lang/Throwable  ]
-TRY 40:[140:java/lang/Throwable  ]
-TRY 43:[140:java/lang/Throwable  ]
-TRY 48:[140:java/lang/Throwable  ]
-TRY 50:[132:java/lang/Throwable  , 140:java/lang/Throwable  ]
-TRY 55:[132:java/lang/Throwable  , 140:java/lang/Throwable  ]
-TRY 58:[124:java/lang/Throwable  , 132:java/lang/Throwable  , 140:java/lang/Throwable  ]
-TRY 60:[124:java/lang/Throwable  , 132:java/lang/Throwable  , 140:java/lang/Throwable  ]
-TRY 61:[124:java/lang/Throwable  , 132:java/lang/Throwable  , 140:java/lang/Throwable  ]
-TRY 65:[124:java/lang/Throwable  , 132:java/lang/Throwable  , 140:java/lang/Throwable  ]
-TRY 66:[124:java/lang/Throwable  , 132:java/lang/Throwable  , 140:java/lang/Throwable  ]
-TRY 69:[124:java/lang/Throwable  , 132:java/lang/Throwable  , 140:java/lang/Throwable  ]
-TRY 72:[124:java/lang/Throwable  , 132:java/lang/Throwable  , 140:java/lang/Throwable  ]
-TRY 74:[132:java/lang/Throwable  , 140:java/lang/Throwable  ]
-TRY 76:[140:java/lang/Throwable  ]
-TRY 79:[124:java/lang/Throwable  , 132:java/lang/Throwable  , 140:java/lang/Throwable  ]
-TRY 81:[124:java/lang/Throwable  , 132:java/lang/Throwable  , 140:java/lang/Throwable  ]
-TRY 90:[124:java/lang/Throwable  , 132:java/lang/Throwable  , 140:java/lang/Throwable  ]
-TRY 92:[132:java/lang/Throwable  , 140:java/lang/Throwable  ]
-TRY 94:[140:java/lang/Throwable  ]
-TRY 97:[124:java/lang/Throwable  , 132:java/lang/Throwable  , 140:java/lang/Throwable  ]
-TRY 100:[124:java/lang/Throwable  , 132:java/lang/Throwable  , 140:java/lang/Throwable  ]
-TRY 103:[124:java/lang/Throwable  , 132:java/lang/Throwable  , 140:java/lang/Throwable  ]
-TRY 105:[124:java/lang/Throwable  , 132:java/lang/Throwable  , 140:java/lang/Throwable  ]
-TRY 107:[124:java/lang/Throwable  , 132:java/lang/Throwable  , 140:java/lang/Throwable  ]
-TRY 109:[124:java/lang/Throwable  , 132:java/lang/Throwable  , 140:java/lang/Throwable  ]
-TRY 110:[124:java/lang/Throwable  , 132:java/lang/Throwable  , 140:java/lang/Throwable  ]
-TRY 112:[124:java/lang/Throwable  , 132:java/lang/Throwable  , 140:java/lang/Throwable  ]
-TRY 114:[124:java/lang/Throwable  , 132:java/lang/Throwable  , 140:java/lang/Throwable  ]
-TRY 119:[124:java/lang/Throwable  , 132:java/lang/Throwable  , 140:java/lang/Throwable  ]
-TRY 120:[124:java/lang/Throwable  , 132:java/lang/Throwable  , 140:java/lang/Throwable  ]
-TRY 122:[124:java/lang/Throwable  , 132:java/lang/Throwable  , 140:java/lang/Throwable  ]
-TRY 126:[124:java/lang/Throwable  , 132:java/lang/Throwable  , 140:java/lang/Throwable  ]
-TRY 128:[132:java/lang/Throwable  , 140:java/lang/Throwable  ]
-TRY 130:[132:java/lang/Throwable  , 140:java/lang/Throwable  ]
-TRY 134:[132:java/lang/Throwable  , 140:java/lang/Throwable  ]
-TRY 136:[140:java/lang/Throwable  ]
-TRY 138:[140:java/lang/Throwable  ]
-TRY 142:[140:java/lang/Throwable  ]
-TRY 154:[229:java/lang/Throwable  ]
-TRY 161:[229:java/lang/Throwable  ]
-TRY 165:[229:java/lang/Throwable  ]
-TRY 166:[229:java/lang/Throwable  ]
-TRY 170:[229:java/lang/Throwable  ]
-TRY 171:[229:java/lang/Throwable  ]
-TRY 175:[229:java/lang/Throwable  ]
-TRY 176:[229:java/lang/Throwable  ]
-TRY 181:[229:java/lang/Throwable  ]
-TRY 182:[229:java/lang/Throwable  ]
-TRY 186:[229:java/lang/Throwable  ]
-TRY 187:[229:java/lang/Throwable  ]
-TRY 192:[229:java/lang/Throwable  ]
-TRY 200:[229:java/lang/Throwable  ]
-TRY 204:[229:java/lang/Throwable  ]
-TRY 205:[229:java/lang/Throwable  ]
-TRY 206:[229:java/lang/Throwable  ]
-TRY 210:[229:java/lang/Throwable  ]
-TRY 211:[229:java/lang/Throwable  ]
-TRY 215:[229:java/lang/Throwable  ]
-TRY 218:[229:java/lang/Throwable  ]
+TRY 28:[134:java/lang/Throwable  ]
+TRY 30:[134:java/lang/Throwable  ]
+TRY 32:[134:java/lang/Throwable  ]
+TRY 33:[134:java/lang/Throwable  ]
+TRY 38:[134:java/lang/Throwable  ]
+TRY 41:[134:java/lang/Throwable  ]
+TRY 44:[134:java/lang/Throwable  ]
+TRY 46:[126:java/lang/Throwable  , 134:java/lang/Throwable  ]
+TRY 49:[126:java/lang/Throwable  , 134:java/lang/Throwable  ]
+TRY 52:[118:java/lang/Throwable  , 126:java/lang/Throwable  , 134:java/lang/Throwable  ]
+TRY 54:[118:java/lang/Throwable  , 126:java/lang/Throwable  , 134:java/lang/Throwable  ]
+TRY 55:[118:java/lang/Throwable  , 126:java/lang/Throwable  , 134:java/lang/Throwable  ]
+TRY 59:[118:java/lang/Throwable  , 126:java/lang/Throwable  , 134:java/lang/Throwable  ]
+TRY 60:[118:java/lang/Throwable  , 126:java/lang/Throwable  , 134:java/lang/Throwable  ]
+TRY 63:[118:java/lang/Throwable  , 126:java/lang/Throwable  , 134:java/lang/Throwable  ]
+TRY 66:[118:java/lang/Throwable  , 126:java/lang/Throwable  , 134:java/lang/Throwable  ]
+TRY 68:[126:java/lang/Throwable  , 134:java/lang/Throwable  ]
+TRY 70:[134:java/lang/Throwable  ]
+TRY 73:[118:java/lang/Throwable  , 126:java/lang/Throwable  , 134:java/lang/Throwable  ]
+TRY 75:[118:java/lang/Throwable  , 126:java/lang/Throwable  , 134:java/lang/Throwable  ]
+TRY 84:[118:java/lang/Throwable  , 126:java/lang/Throwable  , 134:java/lang/Throwable  ]
+TRY 86:[126:java/lang/Throwable  , 134:java/lang/Throwable  ]
+TRY 88:[134:java/lang/Throwable  ]
+TRY 91:[118:java/lang/Throwable  , 126:java/lang/Throwable  , 134:java/lang/Throwable  ]
+TRY 94:[118:java/lang/Throwable  , 126:java/lang/Throwable  , 134:java/lang/Throwable  ]
+TRY 97:[118:java/lang/Throwable  , 126:java/lang/Throwable  , 134:java/lang/Throwable  ]
+TRY 99:[118:java/lang/Throwable  , 126:java/lang/Throwable  , 134:java/lang/Throwable  ]
+TRY 101:[118:java/lang/Throwable  , 126:java/lang/Throwable  , 134:java/lang/Throwable  ]
+TRY 103:[118:java/lang/Throwable  , 126:java/lang/Throwable  , 134:java/lang/Throwable  ]
+TRY 104:[118:java/lang/Throwable  , 126:java/lang/Throwable  , 134:java/lang/Throwable  ]
+TRY 106:[118:java/lang/Throwable  , 126:java/lang/Throwable  , 134:java/lang/Throwable  ]
+TRY 108:[118:java/lang/Throwable  , 126:java/lang/Throwable  , 134:java/lang/Throwable  ]
+TRY 113:[118:java/lang/Throwable  , 126:java/lang/Throwable  , 134:java/lang/Throwable  ]
+TRY 114:[118:java/lang/Throwable  , 126:java/lang/Throwable  , 134:java/lang/Throwable  ]
+TRY 116:[118:java/lang/Throwable  , 126:java/lang/Throwable  , 134:java/lang/Throwable  ]
+TRY 120:[118:java/lang/Throwable  , 126:java/lang/Throwable  , 134:java/lang/Throwable  ]
+TRY 122:[126:java/lang/Throwable  , 134:java/lang/Throwable  ]
+TRY 124:[126:java/lang/Throwable  , 134:java/lang/Throwable  ]
+TRY 128:[126:java/lang/Throwable  , 134:java/lang/Throwable  ]
+TRY 130:[134:java/lang/Throwable  ]
+TRY 132:[134:java/lang/Throwable  ]
+TRY 136:[134:java/lang/Throwable  ]
+TRY 148:[223:java/lang/Throwable  ]
+TRY 155:[223:java/lang/Throwable  ]
+TRY 159:[223:java/lang/Throwable  ]
+TRY 160:[223:java/lang/Throwable  ]
+TRY 164:[223:java/lang/Throwable  ]
+TRY 165:[223:java/lang/Throwable  ]
+TRY 169:[223:java/lang/Throwable  ]
+TRY 170:[223:java/lang/Throwable  ]
+TRY 175:[223:java/lang/Throwable  ]
+TRY 176:[223:java/lang/Throwable  ]
+TRY 180:[223:java/lang/Throwable  ]
+TRY 181:[223:java/lang/Throwable  ]
+TRY 186:[223:java/lang/Throwable  ]
+TRY 194:[223:java/lang/Throwable  ]
+TRY 198:[223:java/lang/Throwable  ]
+TRY 199:[223:java/lang/Throwable  ]
+TRY 200:[223:java/lang/Throwable  ]
+TRY 204:[223:java/lang/Throwable  ]
+TRY 205:[223:java/lang/Throwable  ]
+TRY 209:[223:java/lang/Throwable  ]
+TRY 212:[223:java/lang/Throwable  ]
@@ -87 +87 @@
-IF_ICMPNE L146
+IF_ICMPNE L140
@@ -92 +91,0 @@
-DUP
@@ -93,0 +93,2 @@
+ALOAD 5
+DUP
@@ -95,3 +95,0 @@
-ALOAD 6
-DUP
-ASTORE 7
@@ -106,0 +105,9 @@
+ASTORE 7
+ALOAD 7
+IFNONNULL L40
+ALOAD 6
+MONITOREXIT
+RETURN
+ALOAD 7
+INVOKEVIRTUAL local/lito381/server/CombatMonsterCurseLifecycle$Actor.population()Llocal/lito381/server/ClassicWorldMonsterPopulation;
+DUP
@@ -108,7 +115,4 @@
-ALOAD 8
-IFNONNULL L42
-ALOAD 7
-MONITOREXIT
-RETURN
-ALOAD 8
-INVOKEVIRTUAL local/lito381/server/CombatMonsterCurseLifecycle$Actor.population()Llocal/lito381/server/ClassicWorldMonsterPopulation;
+MONITORENTER
+ALOAD 7
+INVOKEVIRTUAL local/lito381/server/CombatMonsterCurseLifecycle$Actor.timed()Llocal/lito381/server/BattleTimedStatus;
+DUP
@@ -116,3 +119,0 @@
-ALOAD 9
-DUP
-ASTORE 10
@@ -120,9 +121,2 @@
-ALOAD 8
-INVOKEVIRTUAL local/lito381/server/CombatMonsterCurseLifecycle$Actor.timed()Llocal/lito381/server/BattleTimedStatus;
-ASTORE 11
-ALOAD 11
-DUP
-ASTORE 12
-MONITORENTER
-ALOAD 1
-ALOAD 8
+ALOAD 1
+ALOAD 7
@@ -130 +124 @@
-ALOAD 8
+ALOAD 7
@@ -133,2 +127,2 @@
-IFEQ L71
-ALOAD 8
+IFEQ L65
+ALOAD 7
@@ -138 +132 @@
-IFEQ L71
+IFEQ L65
@@ -141,8 +135,3 @@
-IFNE L78
-ALOAD 12
-MONITOREXIT
-ALOAD 10
-MONITOREXIT
-ALOAD 7
-MONITOREXIT
-RETURN
+IFNE L72
+ALOAD 9
+MONITOREXIT
@@ -149,0 +139,5 @@
+MONITOREXIT
+ALOAD 6
+MONITOREXIT
+RETURN
+ALOAD 7
@@ -153,2 +147,2 @@
-ISTORE 13
-ILOAD 13
+ISTORE 10
+ILOAD 10
@@ -156,2 +150,2 @@
-IF_ICMPEQ L89
-ILOAD 13
+IF_ICMPEQ L83
+ILOAD 10
@@ -159,6 +153,6 @@
-IF_ICMPNE L96
-ALOAD 12
-MONITOREXIT
-ALOAD 10
-MONITOREXIT
-ALOAD 7
+IF_ICMPNE L90
+ALOAD 9
+MONITOREXIT
+ALOAD 8
+MONITOREXIT
+ALOAD 6
@@ -169,2 +163,2 @@
-ASTORE 14
-ALOAD 8
+ASTORE 11
+ALOAD 7
@@ -173 +167 @@
-ALOAD 14
+ALOAD 11
@@ -175 +169 @@
-ALOAD 14
+ALOAD 11
@@ -177 +171 @@
-ALOAD 14
+ALOAD 11
@@ -179 +173 @@
-ALOAD 14
+ALOAD 11
@@ -188 +182 @@
-ALOAD 14
+ALOAD 11
@@ -191,0 +186,6 @@
+ALOAD 9
+MONITOREXIT
+GOTO L123
+ASTORE 12
+ALOAD 9
+MONITOREXIT
@@ -193,6 +192,0 @@
-MONITOREXIT
-GOTO L129
-ASTORE 15
-ALOAD 12
-MONITOREXIT
-ALOAD 15
@@ -200,7 +194,7 @@
-ALOAD 10
-MONITOREXIT
-GOTO L137
-ASTORE 16
-ALOAD 10
-MONITOREXIT
-ALOAD 16
+ALOAD 8
+MONITOREXIT
+GOTO L131
+ASTORE 13
+ALOAD 8
+MONITOREXIT
+ALOAD 13
@@ -208,7 +202,7 @@
-ALOAD 7
-MONITOREXIT
-GOTO L145
-ASTORE 17
-ALOAD 7
-MONITOREXIT
-ALOAD 17
+ALOAD 6
+MONITOREXIT
+GOTO L139
+ASTORE 14
+ALOAD 6
+MONITOREXIT
+ALOAD 14
@@ -220 +214 @@
-ASTORE 18
+ASTORE 15
@@ -227 +221 @@
-IF_ICMPNE L190
+IF_ICMPNE L184
@@ -260 +254 @@
-GOTO L220
+GOTO L214
@@ -265 +259 @@
-IF_ICMPNE L197
+IF_ICMPNE L191
@@ -267 +261 @@
-GOTO L198
+GOTO L192
@@ -291,2 +285,2 @@
-ALOAD 18
-IFNONNULL L225
+ALOAD 15
+IFNONNULL L219
@@ -295,3 +289,3 @@
-GOTO L240
-GETSTATIC local/lito381/server/NativeCombatAttacker.reflecting:Ljava/lang/ThreadLocal;
-ALOAD 18
+GOTO L234
+GETSTATIC local/lito381/server/NativeCombatAttacker.reflecting:Ljava/lang/ThreadLocal;
+ALOAD 15
@@ -299,4 +293,4 @@
-GOTO L240
-ASTORE 19
-ALOAD 18
-IFNONNULL L235
+GOTO L234
+ASTORE 16
+ALOAD 15
+IFNONNULL L229
@@ -305,3 +299,3 @@
-GOTO L238
-GETSTATIC local/lito381/server/NativeCombatAttacker.reflecting:Ljava/lang/ThreadLocal;
-ALOAD 18
+GOTO L232
+GETSTATIC local/lito381/server/NativeCombatAttacker.reflecting:Ljava/lang/ThreadLocal;
+ALOAD 15
@@ -309 +303 @@
-ALOAD 19
+ALOAD 16
--- staged/NativeCombatAttacker/nativeSourceNpcPlayerCurrent-810c345d.txt
+++ candidate/NativeCombatAttacker/nativeSourceNpcPlayerCurrent-810c345d.txt
@@ -12,4 +12,2 @@
-ASTORE 0
-ALOAD 0
-IFNULL L9
-ALOAD 0
+DUP
+IFNULL L7
@@ -17,0 +16 @@
+POP
--- staged/NativeCombatAttacker/owner-c203138e.txt
+++ candidate/NativeCombatAttacker/owner-c203138e.txt
@@ -24 +23,0 @@
-DUP
@@ -25,0 +25 @@
+ALOAD 2
--- staged/NativeCombatAttacker/questDamage-9b8065f2.txt
+++ candidate/NativeCombatAttacker/questDamage-9b8065f2.txt
@@ -8,17 +8,17 @@
-TRY 21:[78:java/lang/Throwable  ]
-TRY 24:[78:java/lang/Throwable  ]
-TRY 28:[78:java/lang/Throwable  ]
-TRY 33:[78:java/lang/Throwable  ]
-TRY 40:[78:java/lang/Throwable  ]
-TRY 44:[78:java/lang/Throwable  ]
-TRY 47:[78:java/lang/Throwable  ]
-TRY 52:[78:java/lang/Throwable  ]
-TRY 55:[73:java/lang/Throwable  , 78:java/lang/Throwable  ]
-TRY 57:[73:java/lang/Throwable  , 78:java/lang/Throwable  ]
-TRY 58:[73:java/lang/Throwable  , 78:java/lang/Throwable  ]
-TRY 65:[73:java/lang/Throwable  , 78:java/lang/Throwable  ]
-TRY 69:[73:java/lang/Throwable  , 78:java/lang/Throwable  ]
-TRY 71:[78:java/lang/Throwable  ]
-TRY 75:[73:java/lang/Throwable  , 78:java/lang/Throwable  ]
-TRY 77:[78:java/lang/Throwable  ]
-TRY 80:[78:java/lang/Throwable  ]
+TRY 19:[74:java/lang/Throwable  ]
+TRY 22:[74:java/lang/Throwable  ]
+TRY 26:[74:java/lang/Throwable  ]
+TRY 31:[74:java/lang/Throwable  ]
+TRY 38:[74:java/lang/Throwable  ]
+TRY 42:[74:java/lang/Throwable  ]
+TRY 45:[74:java/lang/Throwable  ]
+TRY 48:[74:java/lang/Throwable  ]
+TRY 51:[69:java/lang/Throwable  , 74:java/lang/Throwable  ]
+TRY 53:[69:java/lang/Throwable  , 74:java/lang/Throwable  ]
+TRY 54:[69:java/lang/Throwable  , 74:java/lang/Throwable  ]
+TRY 61:[69:java/lang/Throwable  , 74:java/lang/Throwable  ]
+TRY 65:[69:java/lang/Throwable  , 74:java/lang/Throwable  ]
+TRY 67:[74:java/lang/Throwable  ]
+TRY 71:[69:java/lang/Throwable  , 74:java/lang/Throwable  ]
+TRY 73:[74:java/lang/Throwable  ]
+TRY 76:[74:java/lang/Throwable  ]
@@ -38,0 +39 @@
+DUP
@@ -40,3 +40,0 @@
-ALOAD 8
-DUP
-ASTORE 9
@@ -47 +45 @@
-ASTORE 10
+ASTORE 9
@@ -51 +49 @@
-IF_ICMPNE L42
+IF_ICMPNE L40
@@ -56 +54 @@
-IFNE L42
+IFNE L40
@@ -61,3 +59,3 @@
-IFNE L42
-ALOAD 10
-IFNULL L42
+IFNE L40
+ALOAD 9
+IFNULL L40
@@ -66 +64 @@
-IFNE L46
+IFNE L44
@@ -68 +66 @@
-ALOAD 9
+ALOAD 8
@@ -71 +69 @@
-ALOAD 10
+ALOAD 9
@@ -73,2 +70,0 @@
-ASTORE 11
-ALOAD 11
@@ -76 +72 @@
-ASTORE 12
+ASTORE 10
@@ -79 +75 @@
-ALOAD 10
+ALOAD 9
@@ -81 +77 @@
-ALOAD 10
+ALOAD 9
@@ -84 +80 @@
-IFEQ L67
+IFEQ L63
@@ -91 +87 @@
-GOTO L68
+GOTO L64
@@ -93 +89 @@
-ALOAD 12
+ALOAD 10
@@ -95 +91 @@
-ALOAD 9
+ALOAD 8
@@ -98 +94,8 @@
-ASTORE 13
+ASTORE 11
+ALOAD 10
+MONITOREXIT
+ALOAD 11
+ATHROW
+ASTORE 12
+ALOAD 8
+MONITOREXIT
@@ -100,2 +102,0 @@
-MONITOREXIT
-ALOAD 13
@@ -103,5 +103,0 @@
-ASTORE 14
-ALOAD 9
-MONITOREXIT
-ALOAD 14
-ATHROW
--- staged/NativeCombatAttacker/resetWorld-9008b840.txt
+++ candidate/NativeCombatAttacker/resetWorld-9008b840.txt
@@ -8,7 +8,7 @@
-TRY 11:[28:java/lang/Throwable  ]
-TRY 17:[28:java/lang/Throwable  ]
-TRY 21:[28:java/lang/Throwable  ]
-TRY 23:[28:java/lang/Throwable  ]
-TRY 24:[28:java/lang/Throwable  ]
-TRY 26:[28:java/lang/Throwable  ]
-TRY 30:[28:java/lang/Throwable  ]
+TRY 9:[26:java/lang/Throwable  ]
+TRY 15:[26:java/lang/Throwable  ]
+TRY 19:[26:java/lang/Throwable  ]
+TRY 21:[26:java/lang/Throwable  ]
+TRY 22:[26:java/lang/Throwable  ]
+TRY 24:[26:java/lang/Throwable  ]
+TRY 28:[26:java/lang/Throwable  ]
@@ -18,0 +19 @@
+DUP
@@ -20,3 +20,0 @@
-ALOAD 1
-DUP
-ASTORE 2
@@ -27,4 +25,4 @@
-ASTORE 3
-ALOAD 3
-IFNULL L25
-ALOAD 3
+ASTORE 2
+ALOAD 2
+IFNULL L23
+ALOAD 2
@@ -33,2 +31,2 @@
-IFNULL L25
-ALOAD 3
+IFNULL L23
+ALOAD 2
@@ -40 +38 @@
-ALOAD 2
+ALOAD 1
@@ -42,3 +40,3 @@
-GOTO L33
-ASTORE 4
-ALOAD 2
+GOTO L31
+ASTORE 3
+ALOAD 1
@@ -46 +44 @@
-ALOAD 4
+ALOAD 3
--- staged/NativeCombatAttacker/timeDamage-c89b9096.txt
+++ candidate/NativeCombatAttacker/timeDamage-c89b9096.txt
@@ -8,17 +8,17 @@
-TRY 21:[78:java/lang/Throwable  ]
-TRY 24:[78:java/lang/Throwable  ]
-TRY 28:[78:java/lang/Throwable  ]
-TRY 33:[78:java/lang/Throwable  ]
-TRY 40:[78:java/lang/Throwable  ]
-TRY 44:[78:java/lang/Throwable  ]
-TRY 47:[78:java/lang/Throwable  ]
-TRY 52:[78:java/lang/Throwable  ]
-TRY 55:[73:java/lang/Throwable  , 78:java/lang/Throwable  ]
-TRY 57:[73:java/lang/Throwable  , 78:java/lang/Throwable  ]
-TRY 58:[73:java/lang/Throwable  , 78:java/lang/Throwable  ]
-TRY 65:[73:java/lang/Throwable  , 78:java/lang/Throwable  ]
-TRY 69:[73:java/lang/Throwable  , 78:java/lang/Throwable  ]
-TRY 71:[78:java/lang/Throwable  ]
-TRY 75:[73:java/lang/Throwable  , 78:java/lang/Throwable  ]
-TRY 77:[78:java/lang/Throwable  ]
-TRY 80:[78:java/lang/Throwable  ]
+TRY 19:[74:java/lang/Throwable  ]
+TRY 22:[74:java/lang/Throwable  ]
+TRY 26:[74:java/lang/Throwable  ]
+TRY 31:[74:java/lang/Throwable  ]
+TRY 38:[74:java/lang/Throwable  ]
+TRY 42:[74:java/lang/Throwable  ]
+TRY 45:[74:java/lang/Throwable  ]
+TRY 48:[74:java/lang/Throwable  ]
+TRY 51:[69:java/lang/Throwable  , 74:java/lang/Throwable  ]
+TRY 53:[69:java/lang/Throwable  , 74:java/lang/Throwable  ]
+TRY 54:[69:java/lang/Throwable  , 74:java/lang/Throwable  ]
+TRY 61:[69:java/lang/Throwable  , 74:java/lang/Throwable  ]
+TRY 65:[69:java/lang/Throwable  , 74:java/lang/Throwable  ]
+TRY 67:[74:java/lang/Throwable  ]
+TRY 71:[69:java/lang/Throwable  , 74:java/lang/Throwable  ]
+TRY 73:[74:java/lang/Throwable  ]
+TRY 76:[74:java/lang/Throwable  ]
@@ -38,0 +39 @@
+DUP
@@ -40,3 +40,0 @@
-ALOAD 8
-DUP
-ASTORE 9
@@ -47 +45 @@
-ASTORE 10
+ASTORE 9
@@ -51 +49 @@
-IF_ICMPNE L42
+IF_ICMPNE L40
@@ -56 +54 @@
-IFNE L42
+IFNE L40
@@ -61,3 +59,3 @@
-IFNE L42
-ALOAD 10
-IFNULL L42
+IFNE L40
+ALOAD 9
+IFNULL L40
@@ -66 +64 @@
-IFNE L46
+IFNE L44
@@ -68 +66 @@
-ALOAD 9
+ALOAD 8
@@ -71 +69 @@
-ALOAD 10
+ALOAD 9
@@ -73,2 +70,0 @@
-ASTORE 11
-ALOAD 11
@@ -76 +72 @@
-ASTORE 12
+ASTORE 10
@@ -79 +75 @@
-ALOAD 10
+ALOAD 9
@@ -81 +77 @@
-ALOAD 10
+ALOAD 9
@@ -84 +80 @@
-IFEQ L67
+IFEQ L63
@@ -91 +87 @@
-GOTO L68
+GOTO L64
@@ -93 +89 @@
-ALOAD 12
+ALOAD 10
@@ -95 +91 @@
-ALOAD 9
+ALOAD 8
@@ -98 +94,8 @@
-ASTORE 13
+ASTORE 11
+ALOAD 10
+MONITOREXIT
+ALOAD 11
+ATHROW
+ASTORE 12
+ALOAD 8
+MONITOREXIT
@@ -100,2 +102,0 @@
-MONITOREXIT
-ALOAD 13
@@ -103,5 +103,0 @@
-ASTORE 14
-ALOAD 9
-MONITOREXIT
-ALOAD 14
-ATHROW
--- staged/NativeCombatAttacker/token-d630f1e.txt
+++ candidate/NativeCombatAttacker/token-d630f1e.txt
@@ -8,8 +8,8 @@
-TRY 8:[36:java/lang/Throwable  ]
-TRY 14:[36:java/lang/Throwable  ]
-TRY 18:[36:java/lang/Throwable  ]
-TRY 24:[36:java/lang/Throwable  ]
-TRY 26:[36:java/lang/Throwable  ]
-TRY 31:[36:java/lang/Throwable  ]
-TRY 34:[36:java/lang/Throwable  ]
-TRY 38:[36:java/lang/Throwable  ]
+TRY 6:[34:java/lang/Throwable  ]
+TRY 12:[34:java/lang/Throwable  ]
+TRY 16:[34:java/lang/Throwable  ]
+TRY 22:[34:java/lang/Throwable  ]
+TRY 24:[34:java/lang/Throwable  ]
+TRY 29:[34:java/lang/Throwable  ]
+TRY 32:[34:java/lang/Throwable  ]
+TRY 36:[34:java/lang/Throwable  ]
@@ -16,0 +17 @@
+DUP
@@ -18,3 +18,0 @@
-ALOAD 2
-DUP
-ASTORE 3
@@ -24,0 +23,10 @@
+ASTORE 3
+ALOAD 3
+IFNONNULL L14
+CONST null
+ALOAD 2
+MONITOREXIT
+ARETURN
+ALOAD 3
+CONST str:"sourceNpcOwner"
+INVOKESTATIC local/lito381/server/CombatAccess.field(Ljava/lang/Object;Ljava/lang/String;)Ljava/lang/Object;
@@ -27,11 +35 @@
-IFNONNULL L16
-CONST null
-ALOAD 3
-MONITOREXIT
-ARETURN
-ALOAD 4
-CONST str:"sourceNpcOwner"
-INVOKESTATIC local/lito381/server/CombatAccess.field(Ljava/lang/Object;Ljava/lang/String;)Ljava/lang/Object;
-ASTORE 5
-ALOAD 5
-IFNONNULL L32
+IFNONNULL L30
@@ -39 +37 @@
-IFEQ L32
+IFEQ L30
@@ -43 +41,3 @@
-ASTORE 5
+ASTORE 4
+ALOAD 3
+CONST str:"sourceNpcOwner"
@@ -45,2 +44,0 @@
-CONST str:"sourceNpcOwner"
-ALOAD 5
@@ -48,2 +46,2 @@
-ALOAD 5
-ALOAD 3
+ALOAD 4
+ALOAD 2
@@ -52,2 +50,2 @@
-ASTORE 6
-ALOAD 3
+ASTORE 5
+ALOAD 2
@@ -55 +53 @@
-ALOAD 6
+ALOAD 5
--- staged/NativeCombatAttacker/world-117e8e93.txt
+++ candidate/NativeCombatAttacker/world-117e8e93.txt
@@ -8,25 +8,25 @@
-TRY 32:[94:java/lang/Throwable  ]
-TRY 38:[94:java/lang/Throwable  ]
-TRY 41:[94:java/lang/Throwable  ]
-TRY 46:[94:java/lang/Throwable  ]
-TRY 48:[89:java/lang/Throwable  , 94:java/lang/Throwable  ]
-TRY 50:[89:java/lang/Throwable  , 94:java/lang/Throwable  ]
-TRY 51:[89:java/lang/Throwable  , 94:java/lang/Throwable  ]
-TRY 52:[89:java/lang/Throwable  , 94:java/lang/Throwable  ]
-TRY 55:[89:java/lang/Throwable  , 94:java/lang/Throwable  ]
-TRY 57:[89:java/lang/Throwable  , 94:java/lang/Throwable  ]
-TRY 58:[89:java/lang/Throwable  , 94:java/lang/Throwable  ]
-TRY 59:[89:java/lang/Throwable  , 94:java/lang/Throwable  ]
-TRY 63:[89:java/lang/Throwable  , 94:java/lang/Throwable  ]
-TRY 65:[94:java/lang/Throwable  ]
-TRY 67:[89:java/lang/Throwable  , 94:java/lang/Throwable  ]
-TRY 72:[89:java/lang/Throwable  , 94:java/lang/Throwable  ]
-TRY 74:[89:java/lang/Throwable  , 94:java/lang/Throwable  ]
-TRY 76:[89:java/lang/Throwable  , 94:java/lang/Throwable  ]
-TRY 78:[89:java/lang/Throwable  , 94:java/lang/Throwable  ]
-TRY 83:[89:java/lang/Throwable  , 94:java/lang/Throwable  ]
-TRY 85:[89:java/lang/Throwable  , 94:java/lang/Throwable  ]
-TRY 87:[94:java/lang/Throwable  ]
-TRY 91:[89:java/lang/Throwable  , 94:java/lang/Throwable  ]
-TRY 93:[94:java/lang/Throwable  ]
-TRY 96:[94:java/lang/Throwable  ]
+TRY 30:[90:java/lang/Throwable  ]
+TRY 36:[90:java/lang/Throwable  ]
+TRY 39:[90:java/lang/Throwable  ]
+TRY 42:[90:java/lang/Throwable  ]
+TRY 44:[85:java/lang/Throwable  , 90:java/lang/Throwable  ]
+TRY 46:[85:java/lang/Throwable  , 90:java/lang/Throwable  ]
+TRY 47:[85:java/lang/Throwable  , 90:java/lang/Throwable  ]
+TRY 48:[85:java/lang/Throwable  , 90:java/lang/Throwable  ]
+TRY 51:[85:java/lang/Throwable  , 90:java/lang/Throwable  ]
+TRY 53:[85:java/lang/Throwable  , 90:java/lang/Throwable  ]
+TRY 54:[85:java/lang/Throwable  , 90:java/lang/Throwable  ]
+TRY 55:[85:java/lang/Throwable  , 90:java/lang/Throwable  ]
+TRY 59:[85:java/lang/Throwable  , 90:java/lang/Throwable  ]
+TRY 61:[90:java/lang/Throwable  ]
+TRY 63:[85:java/lang/Throwable  , 90:java/lang/Throwable  ]
+TRY 68:[85:java/lang/Throwable  , 90:java/lang/Throwable  ]
+TRY 70:[85:java/lang/Throwable  , 90:java/lang/Throwable  ]
+TRY 72:[85:java/lang/Throwable  , 90:java/lang/Throwable  ]
+TRY 74:[85:java/lang/Throwable  , 90:java/lang/Throwable  ]
+TRY 79:[85:java/lang/Throwable  , 90:java/lang/Throwable  ]
+TRY 81:[85:java/lang/Throwable  , 90:java/lang/Throwable  ]
+TRY 83:[90:java/lang/Throwable  ]
+TRY 87:[85:java/lang/Throwable  , 90:java/lang/Throwable  ]
+TRY 89:[90:java/lang/Throwable  ]
+TRY 92:[90:java/lang/Throwable  ]
@@ -55,0 +56 @@
+DUP
@@ -57,3 +57,0 @@
-ALOAD 6
-DUP
-ASTORE 7
@@ -66,3 +64,3 @@
-ASTORE 8
-ALOAD 8
-IFNONNULL L40
+ASTORE 7
+ALOAD 7
+IFNONNULL L38
@@ -70 +68 @@
-ALOAD 7
+ALOAD 6
@@ -73 +71 @@
-ALOAD 8
+ALOAD 7
@@ -75,2 +72,0 @@
-ASTORE 9
-ALOAD 9
@@ -78 +74 @@
-ASTORE 10
+ASTORE 8
@@ -80 +76 @@
-ALOAD 8
+ALOAD 7
@@ -86,2 +82,2 @@
-IFEQ L61
-ALOAD 8
+IFEQ L57
+ALOAD 7
@@ -93 +89 @@
-IFNE L67
+IFNE L63
@@ -95 +91 @@
-ALOAD 10
+ALOAD 8
@@ -97 +93 @@
-ALOAD 7
+ALOAD 6
@@ -104 +100 @@
-ALOAD 8
+ALOAD 7
@@ -108 +104 @@
-ALOAD 8
+ALOAD 7
@@ -117 +113 @@
-ALOAD 10
+ALOAD 8
@@ -119 +115 @@
-ALOAD 7
+ALOAD 6
@@ -122 +118,8 @@
-ASTORE 11
+ASTORE 9
+ALOAD 8
+MONITOREXIT
+ALOAD 9
+ATHROW
+ASTORE 10
+ALOAD 6
+MONITOREXIT
@@ -124,2 +126,0 @@
-MONITOREXIT
-ALOAD 11
@@ -127,5 +127,0 @@
-ASTORE 12
-ALOAD 7
-MONITOREXIT
-ALOAD 12
-ATHROW
```

</details>

### NativeCombatOrigin — SAME_PATHS

<details>
<summary>全部方法診斷</summary>

```json
[
  {
    "method": "contextValid(ZLjava/lang/Object;)Z",
    "status": "SAME_PATHS",
    "detail": "path multisets equal (21)"
  },
  {
    "method": "enterNpc(Llocal/lito381/server/ClientSession;J)Llocal/lito381/server/NativeCombatOrigin$Install;",
    "status": "SAME_PATHS",
    "detail": "path multisets equal (182)"
  },
  {
    "method": "enterWorldAction(Llocal/lito381/server/ClientSession;Ljava/lang/Object;)Llocal/lito381/server/NativeCombatAttacker$Ingress;",
    "status": "SAME_PATHS",
    "detail": "path multisets equal (15)"
  }
]
```

</details>
<details>
<summary>完整正規化差異</summary>

```diff
--- staged/NativeCombatOrigin/contextValid-d817941f.txt
+++ candidate/NativeCombatOrigin/contextValid-d817941f.txt
@@ -27 +26,0 @@
-DUP
@@ -28,0 +28 @@
+ALOAD 3
--- staged/NativeCombatOrigin/enterNpc-56591cfa.txt
+++ candidate/NativeCombatOrigin/enterNpc-56591cfa.txt
@@ -37 +36,0 @@
-DUP
@@ -38,0 +38 @@
+ALOAD 5
--- staged/NativeCombatOrigin/enterWorldAction-e2f021d5.txt
+++ candidate/NativeCombatOrigin/enterWorldAction-e2f021d5.txt
@@ -23 +22,0 @@
-DUP
@@ -25 +23,0 @@
-ASTORE 4
@@ -27 +25 @@
-IFNULL L25
+IFNULL L23
--- staged/NativeCombatOrigin/worldSnapshot-bdc5905d.txt
+++ candidate/NativeCombatOrigin/worldSnapshot-bdc5905d.txt
@@ -8,7 +8,7 @@
-TRY 15:[28:java/lang/Throwable  ]
-TRY 19:[28:java/lang/Throwable  ]
-TRY 22:[28:java/lang/Throwable  ]
-TRY 23:[28:java/lang/Throwable  ]
-TRY 24:[28:java/lang/Throwable  ]
-TRY 26:[28:java/lang/Throwable  ]
-TRY 30:[28:java/lang/Throwable  ]
+TRY 13:[26:java/lang/Throwable  ]
+TRY 17:[26:java/lang/Throwable  ]
+TRY 20:[26:java/lang/Throwable  ]
+TRY 21:[26:java/lang/Throwable  ]
+TRY 22:[26:java/lang/Throwable  ]
+TRY 24:[26:java/lang/Throwable  ]
+TRY 28:[26:java/lang/Throwable  ]
@@ -16 +16 @@
-IFEQ L33
+IFEQ L31
@@ -20,0 +21,2 @@
+ASTORE 2
+ALOAD 2
@@ -22 +23,0 @@
-ASTORE 2
@@ -24,3 +24,0 @@
-ALOAD 3
-DUP
-ASTORE 4
@@ -39,0 +38,6 @@
+ALOAD 3
+MONITOREXIT
+GOTO L31
+ASTORE 4
+ALOAD 3
+MONITOREXIT
@@ -41,6 +44,0 @@
-MONITOREXIT
-GOTO L33
-ASTORE 5
-ALOAD 4
-MONITOREXIT
-ALOAD 5
```

</details>

### NativeDeItemDefinitionsSql — DIFF

```json
{
  "where": "method",
  "member": "finish()V",
  "insnIndex": 54,
  "detail": "left=DUP right=ASTORE 9; paths rejected: normal control-flow cycle"
}
```
<details>
<summary>全部方法診斷</summary>

```json
[
  {
    "method": "finish()V",
    "status": "DIFF",
    "detail": "method finish()V insn[54]: left=DUP right=ASTORE 9; paths rejected: normal control-flow cycle"
  }
]
```

</details>
<details>
<summary>完整正規化差異</summary>

```diff
--- staged/NativeDeItemDefinitionsSql/finish-9b75.txt
+++ candidate/NativeDeItemDefinitionsSql/finish-9b75.txt
@@ -72 +71,0 @@
-DUP
@@ -73,0 +73 @@
+ALOAD 9
@@ -102 +101,0 @@
-DUP
@@ -103,0 +103 @@
+ALOAD 11
@@ -152 +151,0 @@
-DUP
@@ -153,0 +153 @@
+ALOAD 13
@@ -185 +184,0 @@
-DUP
@@ -186,0 +186 @@
+ALOAD 15
```

</details>

### NativeDeItemDefinitionsSql$Lease — DIFF

```json
{
  "where": "method",
  "member": "call(Ljava/lang/Object;Ljava/lang/reflect/Method;[Ljava/lang/Object;)Ljava/lang/Object;",
  "insnIndex": 60,
  "detail": "left=IFEQ L128 right=IFEQ L127; path event multisets differ (left=71, right=70)"
}
```
<details>
<summary>全部方法診斷</summary>

```json
[
  {
    "method": "call(Ljava/lang/Object;Ljava/lang/reflect/Method;[Ljava/lang/Object;)Ljava/lang/Object;",
    "status": "DIFF",
    "detail": "method call(Ljava/lang/Object;Ljava/lang/reflect/Method;[Ljava/lang/Object;)Ljava/lang/Object; insn[60]: left=IFEQ L128 right=IFEQ L127; path event multisets differ (left=71, right=70)"
  }
]
```

</details>
<details>
<summary>完整正規化差異</summary>

```diff
--- staged/NativeDeItemDefinitionsSql$Lease/call-99c7579a.txt
+++ candidate/NativeDeItemDefinitionsSql$Lease/call-99c7579a.txt
@@ -71 +71 @@
-IFEQ L128
+IFEQ L127
@@ -75 +75 @@
-IF_ICMPNE L128
+IF_ICMPNE L127
@@ -79 +78,0 @@
-DUP
@@ -80,0 +80 @@
+ALOAD 5
@@ -82 +82 @@
-IFEQ L128
+IFEQ L127
@@ -121 +120,0 @@
-CHECKCAST java/sql/PreparedStatement
@@ -145 +144 @@
-IFEQ L142
+IFEQ L141
```

</details>

### NativeEquipmentDefense — DIFF

```json
{
  "where": "method",
  "member": "apply(Llocal/lito381/server/ClientSession;Llocal/lito381/server/ClientSession;Llocal/lito381/server/NativePlayerCommit$Binding;Llocal/lito381/server/NativePlayerCommit$Binding;Llocal/lito381/server/NativeEquipmentDefense$Resolved;Llocal/lito381/server/NativeCombatAttacker$Ref;)V",
  "insnIndex": null,
  "detail": "left=35:[222:java/lang/RuntimeException  ] right=35:[224:java/lang/RuntimeException  ]; paths rejected: normal control-flow cycle"
}
```
<details>
<summary>全部方法診斷</summary>

```json
[
  {
    "method": "lambda$skill$0(Llocal/lito381/server/BattleAbilityStatus;Llocal/lito381/server/NativeArmorProc$Rule;JLlocal/lito381/server/BattleSpellCatalog$DamageProtectionSpell;)Z",
    "status": "ONLY_RIGHT",
    "detail": "only right"
  },
  {
    "method": "apply(Llocal/lito381/server/ClientSession;Llocal/lito381/server/ClientSession;Llocal/lito381/server/NativePlayerCommit$Binding;Llocal/lito381/server/NativePlayerCommit$Binding;Llocal/lito381/server/NativeEquipmentDefense$Resolved;Llocal/lito381/server/NativeCombatAttacker$Ref;)V",
    "status": "DIFF",
    "detail": "method apply(Llocal/lito381/server/ClientSession;Llocal/lito381/server/ClientSession;Llocal/lito381/server/NativePlayerCommit$Binding;Llocal/lito381/server/NativePlayerCommit$Binding;Llocal/lito381/server/NativeEquipmentDefense$Resolved;Llocal/lito381/server/NativeCombatAttacker$Ref;)V: left=35:[222:java/lang/RuntimeException  ] right=35:[224:java/lang/RuntimeException  ]; paths rejected: normal control-flow cycle"
  },
  {
    "method": "lambda$nativeSourceEquipmentProcOriginalSkill$0(Llocal/lito381/server/BattleAbilityStatus;Llocal/lito381/server/NativeArmorProc$Rule;JLlocal/lito381/server/BattleSpellCatalog$DamageProtectionSpell;)Z",
    "status": "ONLY_LEFT",
    "detail": "only left"
  },
  {
    "method": "nativeSourceEquipmentProcOriginalSkill(Llocal/lito381/server/ClientSession;Llocal/lito381/server/ClientSession;Llocal/lito381/server/NativeArmorProc$Rule;)V",
    "status": "DIFF",
    "detail": "method nativeSourceEquipmentProcOriginalSkill(Llocal/lito381/server/ClientSession;Llocal/lito381/server/ClientSession;Llocal/lito381/server/NativeArmorProc$Rule;)V insn[41]: left=INVOKEDYNAMIC getAsBoolean(Llocal/lito381/server/BattleAbilityStatus;Llocal/lito381/server/NativeArmorProc$Rule;JLlocal/lito381/server/BattleSpellCatalog$DamageProtectionSpell;)Ljava/util/function/BooleanSupplier; bsm=H6 java/lang/invoke/La… right=INVOKEDYNAMIC getAsBoolean(Llocal/lito381/server/BattleAbilityStatus;Llocal/lito381/server/NativeArmorProc$Rule;JLlocal/lito381/server/BattleSpellCatalog$DamageProtectionSpell;)Ljava/util/function/BooleanSupplier; bsm=H6 java/lang/invoke/La…; path event multisets differ (left=44, right=44)"
  },
  {
    "method": "prepare(Llocal/lito381/server/ClientSession;Z)Ljava/util/List;",
    "status": "DIFF",
    "detail": "method prepare(Llocal/lito381/server/ClientSession;Z)Ljava/util/List; insn[58]: left=IFEQ L75 right=IFNE L60; paths rejected: normal control-flow cycle"
  },
  {
    "method": "resolve(Llocal/lito381/server/ClientSession;Llocal/lito381/server/ClientSession;Ljava/util/List;Llocal/lito381/server/NativeCombatAttacker$Ref;)Llocal/lito381/server/NativeEquipmentDefense$Resolved;",
    "status": "DIFF",
    "detail": "method resolve(Llocal/lito381/server/ClientSession;Llocal/lito381/server/ClientSession;Ljava/util/List;Llocal/lito381/server/NativeCombatAttacker$Ref;)Llocal/lito381/server/NativeEquipmentDefense$Resolved; insn[27]: left=IFNE L34 right=IFEQ L13; paths rejected: normal control-flow cycle"
  }
]
```

</details>
<details>
<summary>完整正規化差異</summary>

```diff
--- staged/NativeEquipmentDefense/apply-cae6685.txt
+++ candidate/NativeEquipmentDefense/apply-cae6685.txt
@@ -8,72 +8,72 @@
-TRY 35:[222:java/lang/RuntimeException  ]
-TRY 43:[222:java/lang/RuntimeException  ]
-TRY 46:[222:java/lang/RuntimeException  ]
-TRY 53:[222:java/lang/RuntimeException  ]
-TRY 59:[222:java/lang/RuntimeException  ]
-TRY 73:[222:java/lang/RuntimeException  ]
-TRY 74:[222:java/lang/RuntimeException  ]
-TRY 79:[222:java/lang/RuntimeException  ]
-TRY 80:[222:java/lang/RuntimeException  ]
-TRY 89:[222:java/lang/RuntimeException  ]
-TRY 91:[222:java/lang/RuntimeException  ]
-TRY 104:[222:java/lang/RuntimeException  ]
-TRY 111:[222:java/lang/RuntimeException  ]
-TRY 114:[222:java/lang/RuntimeException  ]
-TRY 117:[222:java/lang/RuntimeException  ]
-TRY 123:[222:java/lang/RuntimeException  ]
-TRY 125:[222:java/lang/RuntimeException  ]
-TRY 126:[222:java/lang/RuntimeException  ]
-TRY 127:[222:java/lang/RuntimeException  ]
-TRY 129:[222:java/lang/RuntimeException  ]
-TRY 133:[222:java/lang/RuntimeException  ]
-TRY 138:[222:java/lang/RuntimeException  ]
-TRY 140:[222:java/lang/RuntimeException  ]
-TRY 141:[222:java/lang/RuntimeException  ]
-TRY 143:[222:java/lang/RuntimeException  ]
-TRY 145:[222:java/lang/RuntimeException  ]
-TRY 150:[222:java/lang/RuntimeException  ]
-TRY 155:[222:java/lang/RuntimeException  ]
-TRY 160:[222:java/lang/RuntimeException  ]
-TRY 162:[222:java/lang/RuntimeException  ]
-TRY 165:[222:java/lang/RuntimeException  ]
-TRY 166:[222:java/lang/RuntimeException  ]
-TRY 167:[222:java/lang/RuntimeException  ]
-TRY 168:[222:java/lang/RuntimeException  ]
-TRY 170:[222:java/lang/RuntimeException  ]
-TRY 173:[222:java/lang/RuntimeException  ]
-TRY 176:[222:java/lang/RuntimeException  ]
-TRY 179:[222:java/lang/RuntimeException  ]
-TRY 184:[222:java/lang/RuntimeException  ]
-TRY 186:[222:java/lang/RuntimeException  ]
-TRY 187:[222:java/lang/RuntimeException  ]
-TRY 189:[222:java/lang/RuntimeException  ]
-TRY 190:[222:java/lang/RuntimeException  ]
-TRY 191:[222:java/lang/RuntimeException  ]
-TRY 193:[222:java/lang/RuntimeException  ]
-TRY 197:[222:java/lang/RuntimeException  ]
-TRY 202:[222:java/lang/RuntimeException  ]
-TRY 204:[222:java/lang/RuntimeException  ]
-TRY 205:[222:java/lang/RuntimeException  ]
-TRY 207:[222:java/lang/RuntimeException  ]
-TRY 208:[222:java/lang/RuntimeException  ]
-TRY 209:[222:java/lang/RuntimeException  ]
-TRY 211:[222:java/lang/RuntimeException  ]
-TRY 214:[222:java/lang/RuntimeException  ]
-TRY 220:[222:java/lang/RuntimeException  ]
-TRY 252:[256:java/lang/RuntimeException  ]
-TRY 254:[256:java/lang/RuntimeException  ]
-TRY 280:[323:java/lang/RuntimeException  ]
-TRY 284:[323:java/lang/RuntimeException  ]
-TRY 288:[323:java/lang/RuntimeException  ]
-TRY 294:[323:java/lang/RuntimeException  ]
-TRY 298:[323:java/lang/RuntimeException  ]
-TRY 299:[323:java/lang/RuntimeException  ]
-TRY 300:[323:java/lang/RuntimeException  ]
-TRY 304:[323:java/lang/RuntimeException  ]
-TRY 308:[323:java/lang/RuntimeException  ]
-TRY 309:[323:java/lang/RuntimeException  ]
-TRY 313:[323:java/lang/RuntimeException  ]
-TRY 314:[323:java/lang/RuntimeException  ]
-TRY 318:[323:java/lang/RuntimeException  ]
-TRY 319:[323:java/lang/RuntimeException  ]
-TRY 320:[323:java/lang/RuntimeException  ]
+TRY 35:[224:java/lang/RuntimeException  ]
+TRY 43:[224:java/lang/RuntimeException  ]
+TRY 46:[224:java/lang/RuntimeException  ]
+TRY 53:[224:java/lang/RuntimeException  ]
+TRY 59:[224:java/lang/RuntimeException  ]
+TRY 73:[224:java/lang/RuntimeException  ]
+TRY 74:[224:java/lang/RuntimeException  ]
+TRY 79:[224:java/lang/RuntimeException  ]
+TRY 80:[224:java/lang/RuntimeException  ]
+TRY 89:[224:java/lang/RuntimeException  ]
+TRY 91:[224:java/lang/RuntimeException  ]
+TRY 104:[224:java/lang/RuntimeException  ]
+TRY 111:[224:java/lang/RuntimeException  ]
+TRY 114:[224:java/lang/RuntimeException  ]
+TRY 117:[224:java/lang/RuntimeException  ]
+TRY 123:[224:java/lang/RuntimeException  ]
+TRY 125:[224:java/lang/RuntimeException  ]
+TRY 126:[224:java/lang/RuntimeException  ]
+TRY 127:[224:java/lang/RuntimeException  ]
+TRY 129:[224:java/lang/RuntimeException  ]
+TRY 133:[224:java/lang/RuntimeException  ]
+TRY 138:[224:java/lang/RuntimeException  ]
+TRY 140:[224:java/lang/RuntimeException  ]
+TRY 141:[224:java/lang/RuntimeException  ]
+TRY 143:[224:java/lang/RuntimeException  ]
+TRY 145:[224:java/lang/RuntimeException  ]
+TRY 150:[224:java/lang/RuntimeException  ]
+TRY 156:[224:java/lang/RuntimeException  ]
+TRY 162:[224:java/lang/RuntimeException  ]
+TRY 165:[224:java/lang/RuntimeException  ]
+TRY 168:[224:java/lang/RuntimeException  ]
+TRY 169:[224:java/lang/RuntimeException  ]
+TRY 170:[224:java/lang/RuntimeException  ]
+TRY 171:[224:java/lang/RuntimeException  ]
+TRY 173:[224:java/lang/RuntimeException  ]
+TRY 176:[224:java/lang/RuntimeException  ]
+TRY 179:[224:java/lang/RuntimeException  ]
+TRY 182:[224:java/lang/RuntimeException  ]
+TRY 187:[224:java/lang/RuntimeException  ]
+TRY 189:[224:java/lang/RuntimeException  ]
+TRY 190:[224:java/lang/RuntimeException  ]
+TRY 192:[224:java/lang/RuntimeException  ]
+TRY 193:[224:java/lang/RuntimeException  ]
+TRY 194:[224:java/lang/RuntimeException  ]
+TRY 196:[224:java/lang/RuntimeException  ]
+TRY 200:[224:java/lang/RuntimeException  ]
+TRY 205:[224:java/lang/RuntimeException  ]
+TRY 207:[224:java/lang/RuntimeException  ]
+TRY 208:[224:java/lang/RuntimeException  ]
+TRY 210:[224:java/lang/RuntimeException  ]
+TRY 211:[224:java/lang/RuntimeException  ]
+TRY 212:[224:java/lang/RuntimeException  ]
+TRY 214:[224:java/lang/RuntimeException  ]
+TRY 217:[224:java/lang/RuntimeException  ]
+TRY 222:[224:java/lang/RuntimeException  ]
+TRY 254:[258:java/lang/RuntimeException  ]
+TRY 256:[258:java/lang/RuntimeException  ]
+TRY 282:[325:java/lang/RuntimeException  ]
+TRY 286:[325:java/lang/RuntimeException  ]
+TRY 290:[325:java/lang/RuntimeException  ]
+TRY 296:[325:java/lang/RuntimeException  ]
+TRY 300:[325:java/lang/RuntimeException  ]
+TRY 301:[325:java/lang/RuntimeException  ]
+TRY 302:[325:java/lang/RuntimeException  ]
+TRY 306:[325:java/lang/RuntimeException  ]
+TRY 310:[325:java/lang/RuntimeException  ]
+TRY 311:[325:java/lang/RuntimeException  ]
+TRY 315:[325:java/lang/RuntimeException  ]
+TRY 316:[325:java/lang/RuntimeException  ]
+TRY 320:[325:java/lang/RuntimeException  ]
+TRY 321:[325:java/lang/RuntimeException  ]
+TRY 322:[325:java/lang/RuntimeException  ]
@@ -89 +89 @@
-IFEQ L241
+IFEQ L243
@@ -96 +96 @@
-IFEQ L227
+IFEQ L229
@@ -103 +103 @@
-IFEQ L227
+IFEQ L229
@@ -107 +107 @@
-GOTO L227
+GOTO L229
@@ -185,3 +185,3 @@
-IFNE L169
-ALOAD 5
-IFNULL L169
+IFNE L172
+ALOAD 5
+IFNULL L172
@@ -189 +189 @@
-IFEQ L169
+IFEQ L172
@@ -226 +226 @@
-IFLE L169
+IFLE L172
@@ -231 +231,2 @@
-IFNE L169
+IFEQ L153
+GOTO L172
@@ -236 +237,2 @@
-IFNE L169
+IFEQ L159
+GOTO L172
@@ -241 +243,2 @@
-IFNE L169
+IFEQ L165
+GOTO L172
@@ -251 +254 @@
-IFLE L210
+IFLE L213
@@ -254,4 +257,4 @@
-IFEQ L210
-ALOAD 8
-INVOKEVIRTUAL local/lito381/server/NativeArmorProc$Rule.gfxMode()I
-IFEQ L182
+IFEQ L213
+ALOAD 8
+INVOKEVIRTUAL local/lito381/server/NativeArmorProc$Rule.gfxMode()I
+IFEQ L185
@@ -261 +264 @@
-IF_ICMPNE L192
+IF_ICMPNE L195
@@ -275 +278 @@
-IF_ICMPEQ L200
+IF_ICMPEQ L203
@@ -279 +282 @@
-IF_ICMPNE L210
+IF_ICMPNE L213
@@ -292 +295 @@
-IFLE L216
+IFLE L223
@@ -295,2 +298 @@
-IFNE L217
-GOTO L14
+IFEQ L223
@@ -301 +303 @@
-GOTO L226
+GOTO L228
@@ -309,4 +311,4 @@
-IFEQ L241
-ALOAD 0
-INVOKESTATIC local/lito381/server/NativeEquipmentDefense.alive(Llocal/lito381/server/ClientSession;)Z
-IFEQ L241
+IFEQ L243
+ALOAD 0
+INVOKESTATIC local/lito381/server/NativeEquipmentDefense.alive(Llocal/lito381/server/ClientSession;)Z
+IFEQ L243
@@ -323 +325 @@
-IFEQ L260
+IFEQ L262
@@ -326,3 +328,3 @@
-IFLE L260
-ALOAD 5
-IFNULL L260
+IFLE L262
+ALOAD 5
+IFNULL L262
@@ -335 +337 @@
-GOTO L260
+GOTO L262
@@ -341 +343 @@
-IFNONNULL L327
+IFNONNULL L329
@@ -344 +346 @@
-IFEQ L327
+IFEQ L329
@@ -347,3 +349,3 @@
-IFLE L327
-ALOAD 1
-IFNULL L327
+IFLE L329
+ALOAD 1
+IFNULL L329
@@ -351 +353 @@
-IFNULL L327
+IFNULL L329
@@ -355,4 +357,4 @@
-IFEQ L327
-ALOAD 1
-INVOKESTATIC local/lito381/server/NativeEquipmentDefense.alive(Llocal/lito381/server/ClientSession;)Z
-IFEQ L327
+IFEQ L329
+ALOAD 1
+INVOKESTATIC local/lito381/server/NativeEquipmentDefense.alive(Llocal/lito381/server/ClientSession;)Z
+IFEQ L329
@@ -361 +363 @@
-IFEQ L290
+IFEQ L292
@@ -369 +371 @@
-GOTO L322
+GOTO L324
@@ -402 +404 @@
-GOTO L327
+GOTO L329
--- staged/NativeEquipmentDefense/lambda$content$0ccb00a0ec8f3ec5449dc6ee24e7be473f790ad274826d674ac77d250d3f062d-82d11f14.txt
+++ candidate/NativeEquipmentDefense/lambda$content$0ccb00a0ec8f3ec5449dc6ee24e7be473f790ad274826d674ac77d250d3f062d-82d11f14.txt
@@ -0,0 +1,59 @@
+lambda$content$0ccb00a0ec8f3ec5449dc6ee24e7be473f790ad274826d674ac77d250d3f062d(Llocal/lito381/server/BattleAbilityStatus;Llocal/lito381/server/NativeArmorProc$Rule;JLlocal/lito381/server/BattleSpellCatalog$DamageProtectionSpell;)Z
+(Llocal/lito381/server/BattleAbilityStatus;Llocal/lito381/server/NativeArmorProc$Rule;JLlocal/lito381/server/BattleSpellCatalog$DamageProtectionSpell;)Z
+0xa
+
+
+||||pvis=|pinv=|lvtvis=|lvtinv=
+
+TRY 5:[36:java/lang/Throwable  ]
+TRY 9:[36:java/lang/Throwable  ]
+TRY 10:[36:java/lang/Throwable  ]
+TRY 14:[36:java/lang/Throwable  ]
+TRY 17:[36:java/lang/Throwable  ]
+TRY 20:[36:java/lang/Throwable  ]
+TRY 23:[36:java/lang/Throwable  ]
+TRY 29:[36:java/lang/Throwable  ]
+TRY 31:[36:java/lang/Throwable  ]
+TRY 34:[36:java/lang/Throwable  ]
+TRY 38:[36:java/lang/Throwable  ]
+ALOAD 0
+DUP
+ASTORE 5
+MONITORENTER
+ALOAD 1
+INVOKEVIRTUAL local/lito381/server/NativeArmorProc$Rule.selfSkill()Z
+IFEQ L16
+ALOAD 0
+LLOAD 2/3
+INVOKEVIRTUAL local/lito381/server/BattleAbilityStatus.immuneToHarm(J)Llocal/lito381/server/BattleAbilityStatus$Protection;
+INVOKEVIRTUAL local/lito381/server/BattleAbilityStatus$Protection.remainingSeconds()I
+IFLE L16
+CONST Integer:0
+ALOAD 5
+MONITOREXIT
+IRETURN
+ALOAD 1
+INVOKEVIRTUAL local/lito381/server/NativeArmorProc$Rule.seconds()I
+IFLE L22
+ALOAD 1
+INVOKEVIRTUAL local/lito381/server/NativeArmorProc$Rule.seconds()I
+GOTO L24
+ALOAD 4
+INVOKEVIRTUAL local/lito381/server/BattleSpellCatalog$DamageProtectionSpell.durationSeconds()I
+ISTORE 6
+ALOAD 0
+LLOAD 2/3
+ILOAD 6
+ALOAD 4
+INVOKEVIRTUAL local/lito381/server/BattleSpellCatalog$DamageProtectionSpell.damageDivisor()I
+ALOAD 1
+INVOKESTATIC local/lito381/server/NativeSourceProcTimedRefresh.activate68(Llocal/lito381/server/BattleAbilityStatus;JIILlocal/lito381/server/NativeArmorProc$Rule;)V
+CONST Integer:1
+ALOAD 5
+MONITOREXIT
+IRETURN
+ASTORE 7
+ALOAD 5
+MONITOREXIT
+ALOAD 7
+ATHROW
--- staged/NativeEquipmentDefense/lambda$content$1311779030f2ab5722f9414502c42fbc2703668de343a44483b5a2a1281816f1-82d11f14.txt
+++ candidate/NativeEquipmentDefense/lambda$content$1311779030f2ab5722f9414502c42fbc2703668de343a44483b5a2a1281816f1-82d11f14.txt
@@ -1,61 +0,0 @@
-lambda$content$1311779030f2ab5722f9414502c42fbc2703668de343a44483b5a2a1281816f1(Llocal/lito381/server/BattleAbilityStatus;Llocal/lito381/server/NativeArmorProc$Rule;JLlocal/lito381/server/BattleSpellCatalog$DamageProtectionSpell;)Z
-(Llocal/lito381/server/BattleAbilityStatus;Llocal/lito381/server/NativeArmorProc$Rule;JLlocal/lito381/server/BattleSpellCatalog$DamageProtectionSpell;)Z
-0xa
-
-
-||||pvis=|pinv=|lvtvis=|lvtinv=
-
-TRY 7:[38:java/lang/Throwable  ]
-TRY 11:[38:java/lang/Throwable  ]
-TRY 12:[38:java/lang/Throwable  ]
-TRY 16:[38:java/lang/Throwable  ]
-TRY 19:[38:java/lang/Throwable  ]
-TRY 22:[38:java/lang/Throwable  ]
-TRY 25:[38:java/lang/Throwable  ]
-TRY 31:[38:java/lang/Throwable  ]
-TRY 33:[38:java/lang/Throwable  ]
-TRY 36:[38:java/lang/Throwable  ]
-TRY 40:[38:java/lang/Throwable  ]
-ALOAD 0
-ASTORE 5
-ALOAD 5
-DUP
-ASTORE 6
-MONITORENTER
-ALOAD 1
-INVOKEVIRTUAL local/lito381/server/NativeArmorProc$Rule.selfSkill()Z
-IFEQ L18
-ALOAD 0
-LLOAD 2/3
-INVOKEVIRTUAL local/lito381/server/BattleAbilityStatus.immuneToHarm(J)Llocal/lito381/server/BattleAbilityStatus$Protection;
-INVOKEVIRTUAL local/lito381/server/BattleAbilityStatus$Protection.remainingSeconds()I
-IFLE L18
-CONST Integer:0
-ALOAD 6
-MONITOREXIT
-IRETURN
-ALOAD 1
-INVOKEVIRTUAL local/lito381/server/NativeArmorProc$Rule.seconds()I
-IFLE L24
-ALOAD 1
-INVOKEVIRTUAL local/lito381/server/NativeArmorProc$Rule.seconds()I
-GOTO L26
-ALOAD 4
-INVOKEVIRTUAL local/lito381/server/BattleSpellCatalog$DamageProtectionSpell.durationSeconds()I
-ISTORE 7
-ALOAD 0
-LLOAD 2/3
-ILOAD 7
-ALOAD 4
-INVOKEVIRTUAL local/lito381/server/BattleSpellCatalog$DamageProtectionSpell.damageDivisor()I
-ALOAD 1
-INVOKESTATIC local/lito381/server/NativeSourceProcTimedRefresh.activate68(Llocal/lito381/server/BattleAbilityStatus;JIILlocal/lito381/server/NativeArmorProc$Rule;)V
-CONST Integer:1
-ALOAD 6
-MONITOREXIT
-IRETURN
-ASTORE 8
-ALOAD 6
-MONITOREXIT
-ALOAD 8
-ATHROW
--- staged/NativeEquipmentDefense/nativeSourceEquipmentProcOriginalSkill-2cf0c5db.txt
+++ candidate/NativeEquipmentDefense/nativeSourceEquipmentProcOriginalSkill-2cf0c5db.txt
@@ -50 +50 @@
-INVOKEDYNAMIC getAsBoolean(Llocal/lito381/server/BattleAbilityStatus;Llocal/lito381/server/NativeArmorProc$Rule;JLlocal/lito381/server/BattleSpellCatalog$DamageProtectionSpell;)Ljava/util/function/BooleanSupplier; bsm=H6 java/lang/invoke/LambdaMetafactory.metafactory(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodHandle;Ljava/lang/invoke/MethodType;)Ljava/lang/invoke/CallSite; args=[type:()Z, handle:H6 local/lito381/server/NativeEquipmentDefense.lambda$content$1311779030f2ab5722f9414502c42fbc2703668de343a44483b5a2a1281816f1(Llocal/lito381/server/BattleAbilityStatus;Llocal/lito381/server/NativeArmorProc$Rule;JLlocal/lito381/server/BattleSpellCatalog$DamageProtectionSpell;)Z, type:()Z]
+INVOKEDYNAMIC getAsBoolean(Llocal/lito381/server/BattleAbilityStatus;Llocal/lito381/server/NativeArmorProc$Rule;JLlocal/lito381/server/BattleSpellCatalog$DamageProtectionSpell;)Ljava/util/function/BooleanSupplier; bsm=H6 java/lang/invoke/LambdaMetafactory.metafactory(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodHandle;Ljava/lang/invoke/MethodType;)Ljava/lang/invoke/CallSite; args=[type:()Z, handle:H6 local/lito381/server/NativeEquipmentDefense.lambda$content$0ccb00a0ec8f3ec5449dc6ee24e7be473f790ad274826d674ac77d250d3f062d(Llocal/lito381/server/BattleAbilityStatus;Llocal/lito381/server/NativeArmorProc$Rule;JLlocal/lito381/server/BattleSpellCatalog$DamageProtectionSpell;)Z, type:()Z]
--- staged/NativeEquipmentDefense/prepare-81fc7d59.txt
+++ candidate/NativeEquipmentDefense/prepare-81fc7d59.txt
@@ -67 +67,2 @@
-IFEQ L75
+IFNE L60
+GOTO L43
@@ -75,2 +76 @@
-IF_ICMPLT L68
-GOTO L43
+IF_ICMPGE L75
--- staged/NativeEquipmentDefense/resolve-dc9a6b22.txt
+++ candidate/NativeEquipmentDefense/resolve-dc9a6b22.txt
@@ -36,2 +36,2 @@
-IFNE L34
-GOTO L13
+IFEQ L13
+GOTO L34
```

</details>

### NativeLightAction — DIFF

```json
{
  "where": "method",
  "member": "commitPair(Llocal/lito381/server/ClientSession;Llocal/lito381/server/ClientSession;Llocal/lito381/server/NativeSelfCastAction$Task;)V",
  "insnIndex": null,
  "detail": "left=28:[124:java/lang/Throwable  ] right=28:[120:java/lang/Throwable  ]; paths rejected: path limit 4096 exceeded"
}
```
<details>
<summary>全部方法診斷</summary>

```json
[
  {
    "method": "lambda$commitPair$9(Llocal/lito381/server/NativeLightAction$Ingress;Llocal/lito381/server/ClientSession;Llocal/lito381/server/NativePlayerCommit$Binding;Llocal/lito381/server/ClientSession;Llocal/lito381/server/NativePlayerVitalCompletions$Receipt;Llocal/lito381/server/NativeSelfCastAction$Task;)V",
    "status": "ONLY_RIGHT",
    "detail": "only right"
  },
  {
    "method": "lambda$commitSelf$6(Llocal/lito381/server/NativeLightAction$Ingress;Llocal/lito381/server/ClientSession;Llocal/lito381/server/NativeSelfCastAction$Task;)V",
    "status": "ONLY_RIGHT",
    "detail": "only right"
  },
  {
    "method": "lambda$deferScoped$10(Llocal/lito381/server/NativeLightAction$Batch;Llocal/lito381/server/NativeSelfCastAction$Task;)V",
    "status": "ONLY_LEFT",
    "detail": "only left"
  },
  {
    "method": "lambda$deferWeaponTargetOutput$0(Llocal/lito381/server/NativeLightAction$Batch;Llocal/lito381/server/NativeSelfCastAction$Task;)V",
    "status": "ONLY_RIGHT",
    "detail": "only right"
  },
  {
    "method": "activate(Llocal/lito381/server/ClientSession;IJI)V",
    "status": "SAME_PATHS",
    "detail": "path multisets equal (5)"
  },
  {
    "method": "cancel(Llocal/lito381/server/ClientSession;)Z",
    "status": "SAME_PATHS",
    "detail": "path multisets equal (5)"
  },
  {
    "method": "clear(Llocal/lito381/server/ClientSession;)V",
    "status": "SAME_PATHS",
    "detail": "path multisets equal (5)"
  },
  {
    "method": "commitPair(Llocal/lito381/server/ClientSession;Llocal/lito381/server/ClientSession;Llocal/lito381/server/NativeSelfCastAction$Task;)V",
    "status": "DIFF",
    "detail": "method commitPair(Llocal/lito381/server/ClientSession;Llocal/lito381/server/ClientSession;Llocal/lito381/server/NativeSelfCastAction$Task;)V: left=28:[124:java/lang/Throwable  ] right=28:[120:java/lang/Throwable  ]; paths rejected: path limit 4096 exceeded"
  },
  {
    "method": "commitSelf(Llocal/lito381/server/ClientSession;Llocal/lito381/server/NativeSelfCastAction$Task;)V",
    "status": "DIFF",
    "detail": "method commitSelf(Llocal/lito381/server/ClientSession;Llocal/lito381/server/NativeSelfCastAction$Task;)V: left=38:[41:java/io/UncheckedIOException  , 65:java/lang/Throwable  ] right=37:[40:java/io/UncheckedIOException  , 62:java/lang/Throwable  ]; path event multisets differ (left=639, right=639)"
  },
  {
    "method": "deferScoped(Llocal/lito381/server/NativeSelfCastAction$Task;)V",
    "status": "DIFF",
    "detail": "method deferScoped(Llocal/lito381/server/NativeSelfCastAction$Task;)V insn[20]: left=INVOKEDYNAMIC run(Llocal/lito381/server/NativeLightAction$Batch;Llocal/lito381/server/NativeSelfCastAction$Task;)Llocal/lito381/server/NativeLightAction$Output; bsm=H6 java/lang/invoke/LambdaMetafactory.metafactory(Ljava/lang/invoke/MethodH… right=INVOKEDYNAMIC run(Llocal/lito381/server/NativeLightAction$Batch;Llocal/lito381/server/NativeSelfCastAction$Task;)Llocal/lito381/server/NativeLightAction$Output; bsm=H6 java/lang/invoke/LambdaMetafactory.metafactory(Ljava/lang/invoke/MethodH…; path event multisets differ (left=15, right=15)"
  },
  {
    "method": "deferWeaponTargetOutput(Llocal/lito381/server/NativeSelfCastAction$Task;)V",
    "status": "DIFF",
    "detail": "method deferWeaponTargetOutput(Llocal/lito381/server/NativeSelfCastAction$Task;)V insn[20]: left=INVOKEDYNAMIC run(Llocal/lito381/server/NativeLightAction$Batch;Llocal/lito381/server/NativeSelfCastAction$Task;)Llocal/lito381/server/NativeLightAction$Output; bsm=H6 java/lang/invoke/LambdaMetafactory.metafactory(Ljava/lang/invoke/MethodH… right=INVOKEDYNAMIC run(Llocal/lito381/server/NativeLightAction$Batch;Llocal/lito381/server/NativeSelfCastAction$Task;)Llocal/lito381/server/NativeLightAction$Output; bsm=H6 java/lang/invoke/LambdaMetafactory.metafactory(Ljava/lang/invoke/MethodH…; path event multisets differ (left=15, right=15)"
  },
  {
    "method": "expire(Llocal/lito381/server/ClientSession;J)I",
    "status": "SAME_PATHS",
    "detail": "path multisets equal (5)"
  },
  {
    "method": "honorPending(Llocal/lito381/server/ClientSession;JJ)Z",
    "status": "SAME_PATHS",
    "detail": "path multisets equal (5)"
  },
  {
    "method": "lambda$commitPair$9(Llocal/lito381/server/NativeLightAction$Ingress;Llocal/lito381/server/ClientSession;Llocal/lito381/server/NativeSelfCastAction$Task;Llocal/lito381/server/NativePlayerCommit$Binding;Llocal/lito381/server/ClientSession;Llocal/lito381/server/NativePlayerVitalCompletions$Receipt;)V",
    "status": "ONLY_LEFT",
    "detail": "only left"
  },
  {
    "method": "lambda$commitSelf$6(Llocal/lito381/server/NativeLightAction$Ingress;Llocal/lito381/server/ClientSession;Llocal/lito381/server/NativeLightAction$Ingress;Llocal/lito381/server/NativeSelfCastAction$Task;)V",
    "status": "ONLY_LEFT",
    "detail": "only left"
  },
  {
    "method": "rethrowCandidateFailure(Ljava/lang/Throwable;)V",
    "status": "ONLY_LEFT",
    "detail": "only left"
  },
  {
    "method": "storeReady(Llocal/lito381/server/ClientSession;IJ)V",
    "status": "SAME_PATHS",
    "detail": "path multisets equal (5)"
  }
]
```

</details>
<details>
<summary>完整正規化差異</summary>

```diff
--- staged/NativeLightAction/activate-2b9604dc.txt
+++ candidate/NativeLightAction/activate-2b9604dc.txt
@@ -8 +8 @@
-TRY 7:[9:java/lang/Throwable  ]
+TRY 7:[13:java/lang/Throwable  ]
@@ -17 +17,5 @@
-GOTO L16
+ALOAD 0
+ALOAD 5
+CONST null
+INVOKESTATIC local/lito381/server/NativeSourceManualScope.exitState(Llocal/lito381/server/ClientSession;Llocal/lito381/server/NativePlayerActionScope$Ticket;Ljava/lang/Throwable;)V
+GOTO L20
@@ -25,4 +28,0 @@
-ALOAD 0
-ALOAD 5
-CONST null
-INVOKESTATIC local/lito381/server/NativeSourceManualScope.exitState(Llocal/lito381/server/ClientSession;Llocal/lito381/server/NativePlayerActionScope$Ticket;Ljava/lang/Throwable;)V
--- staged/NativeLightAction/cancel-390f6c6a.txt
+++ candidate/NativeLightAction/cancel-390f6c6a.txt
@@ -8 +8 @@
-TRY 4:[7:java/lang/Throwable  ]
+TRY 4:[12:java/lang/Throwable  ]
@@ -15 +15,6 @@
-GOTO L14
+ALOAD 0
+ALOAD 1
+CONST null
+INVOKESTATIC local/lito381/server/NativeSourceManualScope.exitState(Llocal/lito381/server/ClientSession;Llocal/lito381/server/NativePlayerActionScope$Ticket;Ljava/lang/Throwable;)V
+ILOAD 2
+IRETURN
@@ -23,6 +27,0 @@
-ALOAD 0
-ALOAD 1
-CONST null
-INVOKESTATIC local/lito381/server/NativeSourceManualScope.exitState(Llocal/lito381/server/ClientSession;Llocal/lito381/server/NativePlayerActionScope$Ticket;Ljava/lang/Throwable;)V
-ILOAD 2
-IRETURN
--- staged/NativeLightAction/clear-390f6c66.txt
+++ candidate/NativeLightAction/clear-390f6c66.txt
@@ -8 +8 @@
-TRY 4:[6:java/lang/Throwable  ]
+TRY 4:[10:java/lang/Throwable  ]
@@ -14 +14,5 @@
-GOTO L13
+ALOAD 0
+ALOAD 1
+CONST null
+INVOKESTATIC local/lito381/server/NativeSourceManualScope.exitState(Llocal/lito381/server/ClientSession;Llocal/lito381/server/NativePlayerActionScope$Ticket;Ljava/lang/Throwable;)V
+GOTO L17
@@ -22,4 +25,0 @@
-ALOAD 0
-ALOAD 1
-CONST null
-INVOKESTATIC local/lito381/server/NativeSourceManualScope.exitState(Llocal/lito381/server/ClientSession;Llocal/lito381/server/NativePlayerActionScope$Ticket;Ljava/lang/Throwable;)V
--- staged/NativeLightAction/commit-56b84e01.txt
+++ candidate/NativeLightAction/commit-56b84e01.txt
@@ -8,2 +8,2 @@
-TRY 34:[56:java/lang/Throwable  ]
-TRY 35:[56:java/lang/Throwable  ]
+TRY 34:[54:java/lang/Throwable  ]
+TRY 35:[54:java/lang/Throwable  ]
@@ -57 +57 @@
-IFEQ L78
+IFEQ L74
@@ -60 +59,0 @@
-ASTORE 10
@@ -63 +61,0 @@
-ALOAD 10
@@ -65,2 +63,2 @@
-GOTO L78
-ASTORE 11
+GOTO L74
+ASTORE 10
@@ -70 +68 @@
-IFNONNULL L64
+IFNONNULL L62
@@ -73 +71 @@
-GOTO L67
+GOTO L65
@@ -78 +76 @@
-IFEQ L76
+IFEQ L72
@@ -81 +78,0 @@
-ASTORE 12
@@ -84 +80,0 @@
-ALOAD 12
@@ -86 +82 @@
-ALOAD 11
+ALOAD 10
--- staged/NativeLightAction/commitPair-177f2ac2.txt
+++ candidate/NativeLightAction/commitPair-177f2ac2.txt
@@ -8,30 +8,30 @@
-TRY 28:[124:java/lang/Throwable  ]
-TRY 29:[124:java/lang/Throwable  ]
-TRY 30:[124:java/lang/Throwable  ]
-TRY 35:[124:java/lang/Throwable  ]
-TRY 46:[124:java/lang/Throwable  ]
-TRY 47:[124:java/lang/Throwable  ]
-TRY 58:[124:java/lang/Throwable  ]
-TRY 59:[124:java/lang/Throwable  ]
-TRY 60:[124:java/lang/Throwable  ]
-TRY 70:[73:java/io/UncheckedIOException  , 97:java/lang/Throwable  , 124:java/lang/Throwable  ]
-TRY 71:[73:java/io/UncheckedIOException  , 97:java/lang/Throwable  , 124:java/lang/Throwable  ]
-TRY 75:[97:java/lang/Throwable  , 124:java/lang/Throwable  ]
-TRY 76:[97:java/lang/Throwable  , 124:java/lang/Throwable  ]
-TRY 78:[124:java/lang/Throwable  ]
-TRY 81:[124:java/lang/Throwable  ]
-TRY 82:[124:java/lang/Throwable  ]
-TRY 84:[124:java/lang/Throwable  ]
-TRY 86:[124:java/lang/Throwable  ]
-TRY 90:[124:java/lang/Throwable  ]
-TRY 93:[124:java/lang/Throwable  ]
-TRY 95:[124:java/lang/Throwable  ]
-TRY 99:[124:java/lang/Throwable  ]
-TRY 102:[124:java/lang/Throwable  ]
-TRY 103:[124:java/lang/Throwable  ]
-TRY 105:[124:java/lang/Throwable  ]
-TRY 107:[124:java/lang/Throwable  ]
-TRY 111:[124:java/lang/Throwable  ]
-TRY 114:[124:java/lang/Throwable  ]
-TRY 116:[124:java/lang/Throwable  ]
-TRY 118:[124:java/lang/Throwable  ]
+TRY 28:[120:java/lang/Throwable  ]
+TRY 29:[120:java/lang/Throwable  ]
+TRY 30:[120:java/lang/Throwable  ]
+TRY 35:[120:java/lang/Throwable  ]
+TRY 46:[120:java/lang/Throwable  ]
+TRY 47:[120:java/lang/Throwable  ]
+TRY 58:[120:java/lang/Throwable  ]
+TRY 59:[120:java/lang/Throwable  ]
+TRY 60:[120:java/lang/Throwable  ]
+TRY 70:[73:java/io/UncheckedIOException  , 95:java/lang/Throwable  , 120:java/lang/Throwable  ]
+TRY 71:[73:java/io/UncheckedIOException  , 95:java/lang/Throwable  , 120:java/lang/Throwable  ]
+TRY 75:[95:java/lang/Throwable  , 120:java/lang/Throwable  ]
+TRY 76:[95:java/lang/Throwable  , 120:java/lang/Throwable  ]
+TRY 78:[120:java/lang/Throwable  ]
+TRY 81:[120:java/lang/Throwable  ]
+TRY 82:[120:java/lang/Throwable  ]
+TRY 84:[120:java/lang/Throwable  ]
+TRY 86:[120:java/lang/Throwable  ]
+TRY 90:[120:java/lang/Throwable  ]
+TRY 92:[120:java/lang/Throwable  ]
+TRY 93:[120:java/lang/Throwable  ]
+TRY 97:[120:java/lang/Throwable  ]
+TRY 100:[120:java/lang/Throwable  ]
+TRY 101:[120:java/lang/Throwable  ]
+TRY 103:[120:java/lang/Throwable  ]
+TRY 105:[120:java/lang/Throwable  ]
+TRY 109:[120:java/lang/Throwable  ]
+TRY 111:[120:java/lang/Throwable  ]
+TRY 112:[120:java/lang/Throwable  ]
+TRY 114:[120:java/lang/Throwable  ]
@@ -104 +103,0 @@
-ALOAD 3
@@ -108 +107,2 @@
-INVOKEDYNAMIC run(Llocal/lito381/server/NativeLightAction$Ingress;Llocal/lito381/server/ClientSession;Llocal/lito381/server/NativeSelfCastAction$Task;Llocal/lito381/server/NativePlayerCommit$Binding;Llocal/lito381/server/ClientSession;Llocal/lito381/server/NativePlayerVitalCompletions$Receipt;)Ljava/lang/Runnable; bsm=H6 java/lang/invoke/LambdaMetafactory.metafactory(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodHandle;Ljava/lang/invoke/MethodType;)Ljava/lang/invoke/CallSite; args=[type:()V, handle:H6 local/lito381/server/NativeLightAction.lambda$content$af63fe6e68cfd3899283fefbe1d7b62e0a0f81208c854f8311239d75af7c1a51(Llocal/lito381/server/NativeLightAction$Ingress;Llocal/lito381/server/ClientSession;Llocal/lito381/server/NativeSelfCastAction$Task;Llocal/lito381/server/NativePlayerCommit$Binding;Llocal/lito381/server/ClientSession;Llocal/lito381/server/NativePlayerVitalCompletions$Receipt;)V, type:()V]
+ALOAD 3
+INVOKEDYNAMIC run(Llocal/lito381/server/NativeLightAction$Ingress;Llocal/lito381/server/ClientSession;Llocal/lito381/server/NativePlayerCommit$Binding;Llocal/lito381/server/ClientSession;Llocal/lito381/server/NativePlayerVitalCompletions$Receipt;Llocal/lito381/server/NativeSelfCastAction$Task;)Ljava/lang/Runnable; bsm=H6 java/lang/invoke/LambdaMetafactory.metafactory(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodHandle;Ljava/lang/invoke/MethodType;)Ljava/lang/invoke/CallSite; args=[type:()V, handle:H6 local/lito381/server/NativeLightAction.lambda$content$6b68235d2c8d50e26cae1ba81014eacf934e85f556cc515212e7a3bda2b6efa6(Llocal/lito381/server/NativeLightAction$Ingress;Llocal/lito381/server/ClientSession;Llocal/lito381/server/NativePlayerCommit$Binding;Llocal/lito381/server/ClientSession;Llocal/lito381/server/NativePlayerVitalCompletions$Receipt;Llocal/lito381/server/NativeSelfCastAction$Task;)V, type:()V]
@@ -126 +126 @@
-IFEQ L119
+IFEQ L115
@@ -129 +128,0 @@
-ASTORE 11
@@ -132 +130,0 @@
-ALOAD 11
@@ -134,2 +132,2 @@
-GOTO L119
-ASTORE 12
+GOTO L115
+ASTORE 11
@@ -139 +137 @@
-IFNONNULL L105
+IFNONNULL L103
@@ -142 +140 @@
-GOTO L108
+GOTO L106
@@ -147 +145 @@
-IFEQ L117
+IFEQ L113
@@ -150 +147,0 @@
-ASTORE 13
@@ -153 +150,12 @@
-ALOAD 13
+INVOKEVIRTUAL local/lito381/server/NativePlayerVitalCompletions$Receipt.close()V
+ALOAD 11
+ATHROW
+ALOAD 5
+IFNULL L127
+ALOAD 5
+INVOKEVIRTUAL local/lito381/server/NativePlayerVitalCompletions$Receipt.close()V
+GOTO L127
+ASTORE 12
+ALOAD 5
+IFNULL L125
+ALOAD 5
@@ -157,12 +164,0 @@
-ALOAD 5
-IFNULL L131
-ALOAD 5
-INVOKEVIRTUAL local/lito381/server/NativePlayerVitalCompletions$Receipt.close()V
-GOTO L131
-ASTORE 14
-ALOAD 5
-IFNULL L129
-ALOAD 5
-INVOKEVIRTUAL local/lito381/server/NativePlayerVitalCompletions$Receipt.close()V
-ALOAD 14
-ATHROW
--- staged/NativeLightAction/commitSelf-22e54451.txt
+++ candidate/NativeLightAction/commitSelf-22e54451.txt
@@ -8,4 +8,4 @@
-TRY 38:[41:java/io/UncheckedIOException  , 65:java/lang/Throwable  ]
-TRY 39:[41:java/io/UncheckedIOException  , 65:java/lang/Throwable  ]
-TRY 43:[65:java/lang/Throwable  ]
-TRY 44:[65:java/lang/Throwable  ]
+TRY 37:[40:java/io/UncheckedIOException  , 62:java/lang/Throwable  ]
+TRY 38:[40:java/io/UncheckedIOException  , 62:java/lang/Throwable  ]
+TRY 42:[62:java/lang/Throwable  ]
+TRY 43:[62:java/lang/Throwable  ]
@@ -48 +47,0 @@
-ALOAD 2
@@ -50 +49 @@
-INVOKEDYNAMIC run(Llocal/lito381/server/NativeLightAction$Ingress;Llocal/lito381/server/ClientSession;Llocal/lito381/server/NativeLightAction$Ingress;Llocal/lito381/server/NativeSelfCastAction$Task;)Ljava/lang/Runnable; bsm=H6 java/lang/invoke/LambdaMetafactory.metafactory(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodHandle;Ljava/lang/invoke/MethodType;)Ljava/lang/invoke/CallSite; args=[type:()V, handle:H6 local/lito381/server/NativeLightAction.lambda$content$5ceedc81c64b9bb7c004f88cb49bcf9b72491b75348d37c88d376c98356d2a23(Llocal/lito381/server/NativeLightAction$Ingress;Llocal/lito381/server/ClientSession;Llocal/lito381/server/NativeLightAction$Ingress;Llocal/lito381/server/NativeSelfCastAction$Task;)V, type:()V]
+INVOKEDYNAMIC run(Llocal/lito381/server/NativeLightAction$Ingress;Llocal/lito381/server/ClientSession;Llocal/lito381/server/NativeSelfCastAction$Task;)Ljava/lang/Runnable; bsm=H6 java/lang/invoke/LambdaMetafactory.metafactory(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodHandle;Ljava/lang/invoke/MethodType;)Ljava/lang/invoke/CallSite; args=[type:()V, handle:H6 local/lito381/server/NativeLightAction.lambda$content$c14421f5728713fd49de4ba61c87c1ee05dc40331444195d89a4ad9b48a6f726(Llocal/lito381/server/NativeLightAction$Ingress;Llocal/lito381/server/ClientSession;Llocal/lito381/server/NativeSelfCastAction$Task;)V, type:()V]
@@ -52 +51 @@
-GOTO L45
+GOTO L44
@@ -60 +59 @@
-IFNONNULL L52
+IFNONNULL L51
@@ -63 +62 @@
-GOTO L55
+GOTO L54
@@ -68 +67 @@
-IFEQ L87
+IFEQ L82
@@ -71 +69,0 @@
-ASTORE 8
@@ -74 +71,0 @@
-ALOAD 8
@@ -76,2 +73,2 @@
-GOTO L87
-ASTORE 9
+GOTO L82
+ASTORE 8
@@ -81 +78 @@
-IFNONNULL L73
+IFNONNULL L70
@@ -84 +81 @@
-GOTO L76
+GOTO L73
@@ -89 +86 @@
-IFEQ L85
+IFEQ L80
@@ -92 +88,0 @@
-ASTORE 10
@@ -95 +90,0 @@
-ALOAD 10
@@ -97 +92 @@
-ALOAD 9
+ALOAD 8
--- staged/NativeLightAction/currentMaterialProducer-7050a5b2.txt
+++ candidate/NativeLightAction/currentMaterialProducer-7050a5b2.txt
@@ -12,2 +12 @@
-ASTORE 0
-ALOAD 0
+DUP
@@ -14,0 +14 @@
+POP
@@ -17 +16,0 @@
-ALOAD 0
--- staged/NativeLightAction/deferScoped-202c7960.txt
+++ candidate/NativeLightAction/deferScoped-202c7960.txt
@@ -29 +29 @@
-INVOKEDYNAMIC run(Llocal/lito381/server/NativeLightAction$Batch;Llocal/lito381/server/NativeSelfCastAction$Task;)Llocal/lito381/server/NativeLightAction$Output; bsm=H6 java/lang/invoke/LambdaMetafactory.metafactory(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodHandle;Ljava/lang/invoke/MethodType;)Ljava/lang/invoke/CallSite; args=[type:()V, handle:H6 local/lito381/server/NativeLightAction.lambda$content$b93037e7b70cc60f32950364f5b80c1fc077fc616c9138dff25da83806318b36(Llocal/lito381/server/NativeLightAction$Batch;Llocal/lito381/server/NativeSelfCastAction$Task;)V, type:()V]
+INVOKEDYNAMIC run(Llocal/lito381/server/NativeLightAction$Batch;Llocal/lito381/server/NativeSelfCastAction$Task;)Llocal/lito381/server/NativeLightAction$Output; bsm=H6 java/lang/invoke/LambdaMetafactory.metafactory(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodHandle;Ljava/lang/invoke/MethodType;)Ljava/lang/invoke/CallSite; args=[type:()V, handle:H6 local/lito381/server/NativeLightAction.lambda$content$feb21681a7a728f6083a06149f5c2fd605497b164c3f5581edc8c1a2fe120f3c(Llocal/lito381/server/NativeLightAction$Batch;Llocal/lito381/server/NativeSelfCastAction$Task;)V, type:()V]
--- staged/NativeLightAction/deferWeaponTargetOutput-202c7960.txt
+++ candidate/NativeLightAction/deferWeaponTargetOutput-202c7960.txt
@@ -29 +29 @@
-INVOKEDYNAMIC run(Llocal/lito381/server/NativeLightAction$Batch;Llocal/lito381/server/NativeSelfCastAction$Task;)Llocal/lito381/server/NativeLightAction$Output; bsm=H6 java/lang/invoke/LambdaMetafactory.metafactory(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodHandle;Ljava/lang/invoke/MethodType;)Ljava/lang/invoke/CallSite; args=[type:()V, handle:H6 local/lito381/server/NativeLightAction.lambda$content$b93037e7b70cc60f32950364f5b80c1fc077fc616c9138dff25da83806318b36(Llocal/lito381/server/NativeLightAction$Batch;Llocal/lito381/server/NativeSelfCastAction$Task;)V, type:()V]
+INVOKEDYNAMIC run(Llocal/lito381/server/NativeLightAction$Batch;Llocal/lito381/server/NativeSelfCastAction$Task;)Llocal/lito381/server/NativeLightAction$Output; bsm=H6 java/lang/invoke/LambdaMetafactory.metafactory(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodHandle;Ljava/lang/invoke/MethodType;)Ljava/lang/invoke/CallSite; args=[type:()V, handle:H6 local/lito381/server/NativeLightAction.lambda$content$feb21681a7a728f6083a06149f5c2fd605497b164c3f5581edc8c1a2fe120f3c(Llocal/lito381/server/NativeLightAction$Batch;Llocal/lito381/server/NativeSelfCastAction$Task;)V, type:()V]
--- staged/NativeLightAction/dispatchResurrectionOriginal-32fc5fad.txt
+++ candidate/NativeLightAction/dispatchResurrectionOriginal-32fc5fad.txt
@@ -8 +8 @@
-TRY 163:[180:java/lang/Throwable  ]
+TRY 163:[178:java/lang/Throwable  ]
@@ -182,0 +183,4 @@
+ALOAD 4
+INVOKESTATIC local/lito381/server/NativeSourceSelfFinalFrame.retire(Ljava/lang/Object;)V
+INVOKEVIRTUAL local/lito381/server/NativePlayerVitalCompletions$Receipt.close()V
+GOTO L194
@@ -183,0 +188,10 @@
+ALOAD 5
+IFNONNULL L184
+GETSTATIC local/lito381/server/NativeLightAction.ingress:Ljava/lang/ThreadLocal;
+INVOKEVIRTUAL java/lang/ThreadLocal.remove()V
+GOTO L187
+GETSTATIC local/lito381/server/NativeLightAction.ingress:Ljava/lang/ThreadLocal;
+ALOAD 5
+INVOKEVIRTUAL java/lang/ThreadLocal.set(Ljava/lang/Object;)V
+ALOAD 4
+GETFIELD local/lito381/server/NativeLightAction$Ingress.receipt:Llocal/lito381/server/NativePlayerVitalCompletions$Receipt;
@@ -185,0 +200 @@
+INVOKEVIRTUAL local/lito381/server/NativePlayerVitalCompletions$Receipt.close()V
@@ -187,19 +201,0 @@
-INVOKEVIRTUAL local/lito381/server/NativePlayerVitalCompletions$Receipt.close()V
-GOTO L198
-ASTORE 7
-ALOAD 5
-IFNONNULL L186
-GETSTATIC local/lito381/server/NativeLightAction.ingress:Ljava/lang/ThreadLocal;
-INVOKEVIRTUAL java/lang/ThreadLocal.remove()V
-GOTO L189
-GETSTATIC local/lito381/server/NativeLightAction.ingress:Ljava/lang/ThreadLocal;
-ALOAD 5
-INVOKEVIRTUAL java/lang/ThreadLocal.set(Ljava/lang/Object;)V
-ALOAD 4
-GETFIELD local/lito381/server/NativeLightAction$Ingress.receipt:Llocal/lito381/server/NativePlayerVitalCompletions$Receipt;
-ASTORE 8
-ALOAD 4
-INVOKESTATIC local/lito381/server/NativeSourceSelfFinalFrame.retire(Ljava/lang/Object;)V
-ALOAD 8
-INVOKEVIRTUAL local/lito381/server/NativePlayerVitalCompletions$Receipt.close()V
-ALOAD 7
--- staged/NativeLightAction/expire-e8de9711.txt
+++ candidate/NativeLightAction/expire-e8de9711.txt
@@ -8 +8 @@
-TRY 5:[8:java/lang/Throwable  ]
+TRY 5:[13:java/lang/Throwable  ]
@@ -16 +16,6 @@
-GOTO L15
+ALOAD 0
+ALOAD 3
+CONST null
+INVOKESTATIC local/lito381/server/NativeSourceManualScope.exitState(Llocal/lito381/server/ClientSession;Llocal/lito381/server/NativePlayerActionScope$Ticket;Ljava/lang/Throwable;)V
+ILOAD 4
+IRETURN
@@ -24,6 +28,0 @@
-ALOAD 0
-ALOAD 3
-CONST null
-INVOKESTATIC local/lito381/server/NativeSourceManualScope.exitState(Llocal/lito381/server/ClientSession;Llocal/lito381/server/NativePlayerActionScope$Ticket;Ljava/lang/Throwable;)V
-ILOAD 4
-IRETURN
--- staged/NativeLightAction/honorPending-32f4c36a.txt
+++ candidate/NativeLightAction/honorPending-32f4c36a.txt
@@ -8 +8 @@
-TRY 6:[9:java/lang/Throwable  ]
+TRY 6:[14:java/lang/Throwable  ]
@@ -17 +17,6 @@
-GOTO L16
+ALOAD 0
+ALOAD 5
+CONST null
+INVOKESTATIC local/lito381/server/NativeSourceManualScope.exitState(Llocal/lito381/server/ClientSession;Llocal/lito381/server/NativePlayerActionScope$Ticket;Ljava/lang/Throwable;)V
+ILOAD 6
+IRETURN
@@ -25,6 +29,0 @@
-ALOAD 0
-ALOAD 5
-CONST null
-INVOKESTATIC local/lito381/server/NativeSourceManualScope.exitState(Llocal/lito381/server/ClientSession;Llocal/lito381/server/NativePlayerActionScope$Ticket;Ljava/lang/Throwable;)V
-ILOAD 6
-IRETURN
--- staged/NativeLightAction/lambda$content$2641308aca774de6380b15f79083d5416e0d9388fce2ad6ca53984b2624c4d40-800bed51.txt
+++ candidate/NativeLightAction/lambda$content$2641308aca774de6380b15f79083d5416e0d9388fce2ad6ca53984b2624c4d40-800bed51.txt
@@ -0,0 +1,27 @@
+lambda$content$2641308aca774de6380b15f79083d5416e0d9388fce2ad6ca53984b2624c4d40(Llocal/lito381/server/NativeLightAction$Batch;Llocal/lito381/server/NativeSelfCastAction$Task;)V
+(Llocal/lito381/server/NativeLightAction$Batch;Llocal/lito381/server/NativeSelfCastAction$Task;)V
+0xa
+
+java/io/IOException
+||||pvis=|pinv=|lvtvis=|lvtinv=
+
+TRY
+ALOAD 0
+GETFIELD local/lito381/server/NativeLightAction$Batch.accepted:Llocal/lito381/server/NativeLightAction$Ingress;
+GETFIELD local/lito381/server/NativeLightAction$Ingress.binding:Llocal/lito381/server/NativePlayerCommit$Binding;
+ALOAD 0
+GETFIELD local/lito381/server/NativeLightAction$Batch.pc:Llocal/lito381/server/ClientSession;
+INVOKEVIRTUAL local/lito381/server/NativePlayerCommit$Binding.matches(Llocal/lito381/server/ClientSession;)Z
+IFEQ L18
+ALOAD 0
+GETFIELD local/lito381/server/NativeLightAction$Batch.target:Llocal/lito381/server/ClientSession;
+IFNULL L16
+ALOAD 0
+GETFIELD local/lito381/server/NativeLightAction$Batch.targetBinding:Llocal/lito381/server/NativePlayerCommit$Binding;
+ALOAD 0
+GETFIELD local/lito381/server/NativeLightAction$Batch.target:Llocal/lito381/server/ClientSession;
+INVOKEVIRTUAL local/lito381/server/NativePlayerCommit$Binding.matches(Llocal/lito381/server/ClientSession;)Z
+IFEQ L18
+ALOAD 1
+INVOKEINTERFACE local/lito381/server/NativeSelfCastAction$Task.run()V itf
+RETURN
--- staged/NativeLightAction/lambda$content$5ceedc81c64b9bb7c004f88cb49bcf9b72491b75348d37c88d376c98356d2a23-6ae3e47d.txt
+++ candidate/NativeLightAction/lambda$content$5ceedc81c64b9bb7c004f88cb49bcf9b72491b75348d37c88d376c98356d2a23-6ae3e47d.txt
@@ -1,125 +0,0 @@
-lambda$content$5ceedc81c64b9bb7c004f88cb49bcf9b72491b75348d37c88d376c98356d2a23(Llocal/lito381/server/NativeLightAction$Ingress;Llocal/lito381/server/ClientSession;Llocal/lito381/server/NativeLightAction$Ingress;Llocal/lito381/server/NativeSelfCastAction$Task;)V
-(Llocal/lito381/server/NativeLightAction$Ingress;Llocal/lito381/server/ClientSession;Llocal/lito381/server/NativeLightAction$Ingress;Llocal/lito381/server/NativeSelfCastAction$Task;)V
-0xa
-
-
-||||pvis=|pinv=|lvtvis=|lvtinv=
-
-TRY 54:[80:java/io/IOException  , 86:java/lang/Throwable  ]
-TRY 81:[86:java/lang/Throwable  ]
-TRY 84:[86:java/lang/Throwable  ]
-TRY 85:[86:java/lang/Throwable  ]
-ALOAD 0
-ALOAD 1
-INVOKESTATIC local/lito381/server/NativeSourceMaterialMain.locked(Ljava/lang/Object;Llocal/lito381/server/ClientSession;)Z
-IFNE L5
-RETURN
-ALOAD 2
-GETFIELD local/lito381/server/NativeLightAction$Ingress.binding:Llocal/lito381/server/NativePlayerCommit$Binding;
-ALOAD 1
-ALOAD 3
-INVOKESTATIC local/lito381/server/NativeSourceSelfFinalFrame.matches(Llocal/lito381/server/NativePlayerCommit$Binding;Llocal/lito381/server/ClientSession;Llocal/lito381/server/NativeSelfCastAction$Task;)Z
-IFEQ L31
-ALOAD 1
-CONST str:"sourceItemDisconnected"
-INVOKESTATIC local/lito381/server/CombatAccess.field(Ljava/lang/Object;Ljava/lang/String;)Ljava/lang/Object;
-CHECKCAST java/lang/Boolean
-INVOKEVIRTUAL java/lang/Boolean.booleanValue()Z
-IFNE L31
-ALOAD 2
-GETFIELD local/lito381/server/NativeLightAction$Ingress.hub:Llocal/lito381/server/GameHub;
-CONST str:"sourceItemApplication"
-INVOKESTATIC local/lito381/server/CombatAccess.field(Ljava/lang/Object;Ljava/lang/String;)Ljava/lang/Object;
-ALOAD 2
-GETFIELD local/lito381/server/NativeLightAction$Ingress.application:Llocal/lito381/server/NativeDeItemApplication;
-IF_ACMPNE L31
-ALOAD 2
-GETFIELD local/lito381/server/NativeLightAction$Ingress.application:Llocal/lito381/server/NativeDeItemApplication;
-IFNULL L32
-ALOAD 2
-GETFIELD local/lito381/server/NativeLightAction$Ingress.application:Llocal/lito381/server/NativeDeItemApplication;
-GETFIELD local/lito381/server/NativeDeItemApplication.closed:Z
-IFEQ L32
-RETURN
-NEW local/lito381/server/NativeLightAction$Batch
-DUP
-ALOAD 1
-ALOAD 2
-GETFIELD local/lito381/server/NativeLightAction$Ingress.arrival:Llocal/lito381/server/NativeCastTimeline$Arrival;
-ALOAD 0
-INVOKESPECIAL local/lito381/server/NativeLightAction$Batch.<init>(Llocal/lito381/server/ClientSession;Llocal/lito381/server/NativeCastTimeline$Arrival;Llocal/lito381/server/NativeLightAction$Ingress;)V
-ASTORE 4
-ALOAD 4
-ALOAD 3
-INVOKESTATIC local/lito381/server/NativeSourceSelfFinalFrame.install(Ljava/lang/Object;Llocal/lito381/server/NativeSelfCastAction$Task;)V
-ALOAD 4
-CONST Integer:1
-PUTFIELD local/lito381/server/NativeLightAction$Batch.captureSelf:Z
-GETSTATIC local/lito381/server/NativeLightAction.current:Ljava/lang/ThreadLocal;
-INVOKEVIRTUAL java/lang/ThreadLocal.get()Ljava/lang/Object;
-CHECKCAST local/lito381/server/NativeLightAction$Batch
-ASTORE 5
-GETSTATIC local/lito381/server/NativeLightAction.current:Ljava/lang/ThreadLocal;
-ALOAD 4
-INVOKEVIRTUAL java/lang/ThreadLocal.set(Ljava/lang/Object;)V
-ALOAD 3
-INVOKEINTERFACE local/lito381/server/NativeSelfCastAction$Task.run()V itf
-ALOAD 5
-IFNONNULL L62
-ALOAD 4
-INVOKESTATIC local/lito381/server/NativeSourceSelfFinalFrame.produced(Ljava/lang/Object;)V
-GETSTATIC local/lito381/server/NativeLightAction.current:Ljava/lang/ThreadLocal;
-INVOKEVIRTUAL java/lang/ThreadLocal.remove()V
-GOTO L67
-ALOAD 4
-INVOKESTATIC local/lito381/server/NativeSourceSelfFinalFrame.produced(Ljava/lang/Object;)V
-GETSTATIC local/lito381/server/NativeLightAction.current:Ljava/lang/ThreadLocal;
-ALOAD 5
-INVOKEVIRTUAL java/lang/ThreadLocal.set(Ljava/lang/Object;)V
-ALOAD 4
-GETFIELD local/lito381/server/NativeLightAction$Batch.outputs:Ljava/util/List;
-INVOKEINTERFACE java/util/List.isEmpty()Z itf
-IFEQ L75
-ALOAD 4
-GETFIELD local/lito381/server/NativeLightAction$Batch.completions:Ljava/util/List;
-INVOKEINTERFACE java/util/List.isEmpty()Z itf
-IFNE L113
-ALOAD 2
-GETFIELD local/lito381/server/NativeLightAction$Ingress.receipt:Llocal/lito381/server/NativePlayerVitalCompletions$Receipt;
-ALOAD 4
-INVOKESTATIC local/lito381/server/NativePlayerVitalCompletions.enqueueLocked(Llocal/lito381/server/NativePlayerVitalCompletions$Receipt;Ljava/lang/Runnable;)V
-GOTO L113
-ASTORE 6
-NEW java/io/UncheckedIOException
-DUP
-ALOAD 6
-INVOKESPECIAL java/io/UncheckedIOException.<init>(Ljava/io/IOException;)V
-ATHROW
-ASTORE 7
-ALOAD 5
-IFNONNULL L94
-ALOAD 4
-INVOKESTATIC local/lito381/server/NativeSourceSelfFinalFrame.produced(Ljava/lang/Object;)V
-GETSTATIC local/lito381/server/NativeLightAction.current:Ljava/lang/ThreadLocal;
-INVOKEVIRTUAL java/lang/ThreadLocal.remove()V
-GOTO L99
-ALOAD 4
-INVOKESTATIC local/lito381/server/NativeSourceSelfFinalFrame.produced(Ljava/lang/Object;)V
-GETSTATIC local/lito381/server/NativeLightAction.current:Ljava/lang/ThreadLocal;
-ALOAD 5
-INVOKEVIRTUAL java/lang/ThreadLocal.set(Ljava/lang/Object;)V
-ALOAD 4
-GETFIELD local/lito381/server/NativeLightAction$Batch.outputs:Ljava/util/List;
-INVOKEINTERFACE java/util/List.isEmpty()Z itf
-IFEQ L107
-ALOAD 4
-GETFIELD local/lito381/server/NativeLightAction$Batch.completions:Ljava/util/List;
-INVOKEINTERFACE java/util/List.isEmpty()Z itf
-IFNE L111
-ALOAD 2
-GETFIELD local/lito381/server/NativeLightAction$Ingress.receipt:Llocal/lito381/server/NativePlayerVitalCompletions$Receipt;
-ALOAD 4
-INVOKESTATIC local/lito381/server/NativePlayerVitalCompletions.enqueueLocked(Llocal/lito381/server/NativePlayerVitalCompletions$Receipt;Ljava/lang/Runnable;)V
-ALOAD 7
-ATHROW
-RETURN
--- staged/NativeLightAction/lambda$content$6b68235d2c8d50e26cae1ba81014eacf934e85f556cc515212e7a3bda2b6efa6-70d1b00f.txt
+++ candidate/NativeLightAction/lambda$content$6b68235d2c8d50e26cae1ba81014eacf934e85f556cc515212e7a3bda2b6efa6-70d1b00f.txt
@@ -0,0 +1,163 @@
+lambda$content$6b68235d2c8d50e26cae1ba81014eacf934e85f556cc515212e7a3bda2b6efa6(Llocal/lito381/server/NativeLightAction$Ingress;Llocal/lito381/server/ClientSession;Llocal/lito381/server/NativePlayerCommit$Binding;Llocal/lito381/server/ClientSession;Llocal/lito381/server/NativePlayerVitalCompletions$Receipt;Llocal/lito381/server/NativeSelfCastAction$Task;)V
+(Llocal/lito381/server/NativeLightAction$Ingress;Llocal/lito381/server/ClientSession;Llocal/lito381/server/NativePlayerCommit$Binding;Llocal/lito381/server/ClientSession;Llocal/lito381/server/NativePlayerVitalCompletions$Receipt;Llocal/lito381/server/NativeSelfCastAction$Task;)V
+0xa
+
+
+||||pvis=|pinv=|lvtvis=|lvtinv=
+
+TRY 77:[108:java/io/IOException  , 114:java/lang/Throwable  ]
+TRY 99:[103:java/lang/RuntimeException  , 103:java/lang/Error  ]
+TRY 101:[103:java/lang/RuntimeException  , 103:java/lang/Error  ]
+TRY 109:[114:java/lang/Throwable  ]
+TRY 112:[114:java/lang/Throwable  ]
+TRY 113:[114:java/lang/Throwable  ]
+TRY 136:[140:java/lang/RuntimeException  , 140:java/lang/Error  ]
+TRY 138:[140:java/lang/RuntimeException  , 140:java/lang/Error  ]
+ALOAD 0
+ALOAD 1
+INVOKESTATIC local/lito381/server/NativeSourceMaterialMain.locked(Ljava/lang/Object;Llocal/lito381/server/ClientSession;)Z
+IFNE L5
+RETURN
+ALOAD 0
+GETFIELD local/lito381/server/NativeLightAction$Ingress.binding:Llocal/lito381/server/NativePlayerCommit$Binding;
+ALOAD 1
+ALOAD 5
+INVOKESTATIC local/lito381/server/NativeSourceMaterialPairGuard.matches(Llocal/lito381/server/NativePlayerCommit$Binding;Llocal/lito381/server/ClientSession;Llocal/lito381/server/NativeSelfCastAction$Task;)Z
+IFEQ L48
+ALOAD 2
+ALOAD 3
+ALOAD 5
+INVOKESTATIC local/lito381/server/NativeSourceMaterialPairGuard.matches(Llocal/lito381/server/NativePlayerCommit$Binding;Llocal/lito381/server/ClientSession;Llocal/lito381/server/NativeSelfCastAction$Task;)Z
+IFEQ L48
+ALOAD 3
+CONST str:"hub"
+INVOKESTATIC local/lito381/server/CombatAccess.field(Ljava/lang/Object;Ljava/lang/String;)Ljava/lang/Object;
+ALOAD 0
+GETFIELD local/lito381/server/NativeLightAction$Ingress.hub:Llocal/lito381/server/GameHub;
+IF_ACMPNE L48
+ALOAD 1
+CONST str:"sourceItemDisconnected"
+INVOKESTATIC local/lito381/server/CombatAccess.field(Ljava/lang/Object;Ljava/lang/String;)Ljava/lang/Object;
+CHECKCAST java/lang/Boolean
+INVOKEVIRTUAL java/lang/Boolean.booleanValue()Z
+IFNE L48
+ALOAD 3
+CONST str:"sourceItemDisconnected"
+INVOKESTATIC local/lito381/server/CombatAccess.field(Ljava/lang/Object;Ljava/lang/String;)Ljava/lang/Object;
+CHECKCAST java/lang/Boolean
+INVOKEVIRTUAL java/lang/Boolean.booleanValue()Z
+IFNE L48
+ALOAD 0
+GETFIELD local/lito381/server/NativeLightAction$Ingress.hub:Llocal/lito381/server/GameHub;
+CONST str:"sourceItemApplication"
+INVOKESTATIC local/lito381/server/CombatAccess.field(Ljava/lang/Object;Ljava/lang/String;)Ljava/lang/Object;
+ALOAD 0
+GETFIELD local/lito381/server/NativeLightAction$Ingress.application:Llocal/lito381/server/NativeDeItemApplication;
+IF_ACMPNE L48
+ALOAD 0
+GETFIELD local/lito381/server/NativeLightAction$Ingress.application:Llocal/lito381/server/NativeDeItemApplication;
+IFNULL L49
+ALOAD 0
+GETFIELD local/lito381/server/NativeLightAction$Ingress.application:Llocal/lito381/server/NativeDeItemApplication;
+GETFIELD local/lito381/server/NativeDeItemApplication.closed:Z
+IFEQ L49
+RETURN
+NEW local/lito381/server/NativeLightAction$Batch
+DUP
+ALOAD 1
+ALOAD 0
+GETFIELD local/lito381/server/NativeLightAction$Ingress.arrival:Llocal/lito381/server/NativeCastTimeline$Arrival;
+ALOAD 0
+INVOKESPECIAL local/lito381/server/NativeLightAction$Batch.<init>(Llocal/lito381/server/ClientSession;Llocal/lito381/server/NativeCastTimeline$Arrival;Llocal/lito381/server/NativeLightAction$Ingress;)V
+ASTORE 6
+ALOAD 6
+CONST Integer:1
+PUTFIELD local/lito381/server/NativeLightAction$Batch.captureSelf:Z
+ALOAD 6
+ALOAD 3
+PUTFIELD local/lito381/server/NativeLightAction$Batch.target:Llocal/lito381/server/ClientSession;
+ALOAD 6
+ALOAD 2
+PUTFIELD local/lito381/server/NativeLightAction$Batch.targetBinding:Llocal/lito381/server/NativePlayerCommit$Binding;
+ALOAD 6
+ALOAD 4
+PUTFIELD local/lito381/server/NativeLightAction$Batch.targetReceipt:Llocal/lito381/server/NativePlayerVitalCompletions$Receipt;
+GETSTATIC local/lito381/server/NativeLightAction.current:Ljava/lang/ThreadLocal;
+INVOKEVIRTUAL java/lang/ThreadLocal.get()Ljava/lang/Object;
+CHECKCAST local/lito381/server/NativeLightAction$Batch
+ASTORE 7
+GETSTATIC local/lito381/server/NativeLightAction.current:Ljava/lang/ThreadLocal;
+ALOAD 6
+INVOKEVIRTUAL java/lang/ThreadLocal.set(Ljava/lang/Object;)V
+ALOAD 5
+INVOKEINTERFACE local/lito381/server/NativeSelfCastAction$Task.run()V itf
+ALOAD 7
+IFNONNULL L83
+GETSTATIC local/lito381/server/NativeLightAction.current:Ljava/lang/ThreadLocal;
+INVOKEVIRTUAL java/lang/ThreadLocal.remove()V
+GOTO L86
+GETSTATIC local/lito381/server/NativeLightAction.current:Ljava/lang/ThreadLocal;
+ALOAD 7
+INVOKEVIRTUAL java/lang/ThreadLocal.set(Ljava/lang/Object;)V
+ALOAD 6
+GETFIELD local/lito381/server/NativeLightAction$Batch.outputs:Ljava/util/List;
+INVOKEINTERFACE java/util/List.isEmpty()Z itf
+IFEQ L94
+ALOAD 6
+GETFIELD local/lito381/server/NativeLightAction$Batch.completions:Ljava/util/List;
+INVOKEINTERFACE java/util/List.isEmpty()Z itf
+IFNE L147
+ALOAD 1
+ALOAD 3
+ALOAD 4
+INVOKESTATIC local/lito381/server/NativePairedCastSafety.transfer(Llocal/lito381/server/ClientSession;Llocal/lito381/server/ClientSession;Llocal/lito381/server/NativePlayerVitalCompletions$Receipt;)V
+ALOAD 0
+GETFIELD local/lito381/server/NativeLightAction$Ingress.receipt:Llocal/lito381/server/NativePlayerVitalCompletions$Receipt;
+ALOAD 6
+INVOKESTATIC local/lito381/server/NativePlayerVitalCompletions.enqueueLocked(Llocal/lito381/server/NativePlayerVitalCompletions$Receipt;Ljava/lang/Runnable;)V
+GOTO L147
+ASTORE 8
+ALOAD 4
+INVOKESTATIC local/lito381/server/NativePairedCastSafety.abandon(Llocal/lito381/server/NativePlayerVitalCompletions$Receipt;)V
+ALOAD 8
+ATHROW
+ASTORE 9
+NEW java/io/UncheckedIOException
+DUP
+ALOAD 9
+INVOKESPECIAL java/io/UncheckedIOException.<init>(Ljava/io/IOException;)V
+ATHROW
+ASTORE 10
+ALOAD 7
+IFNONNULL L120
+GETSTATIC local/lito381/server/NativeLightAction.current:Ljava/lang/ThreadLocal;
+INVOKEVIRTUAL java/lang/ThreadLocal.remove()V
+GOTO L123
+GETSTATIC local/lito381/server/NativeLightAction.current:Ljava/lang/ThreadLocal;
+ALOAD 7
+INVOKEVIRTUAL java/lang/ThreadLocal.set(Ljava/lang/Object;)V
+ALOAD 6
+GETFIELD local/lito381/server/NativeLightAction$Batch.outputs:Ljava/util/List;
+INVOKEINTERFACE java/util/List.isEmpty()Z itf
+IFEQ L131
+ALOAD 6
+GETFIELD local/lito381/server/NativeLightAction$Batch.completions:Ljava/util/List;
+INVOKEINTERFACE java/util/List.isEmpty()Z itf
+IFNE L145
+ALOAD 1
+ALOAD 3
+ALOAD 4
+INVOKESTATIC local/lito381/server/NativePairedCastSafety.transfer(Llocal/lito381/server/ClientSession;Llocal/lito381/server/ClientSession;Llocal/lito381/server/NativePlayerVitalCompletions$Receipt;)V
+ALOAD 0
+GETFIELD local/lito381/server/NativeLightAction$Ingress.receipt:Llocal/lito381/server/NativePlayerVitalCompletions$Receipt;
+ALOAD 6
+INVOKESTATIC local/lito381/server/NativePlayerVitalCompletions.enqueueLocked(Llocal/lito381/server/NativePlayerVitalCompletions$Receipt;Ljava/lang/Runnable;)V
+GOTO L145
+ASTORE 11
+ALOAD 4
+INVOKESTATIC local/lito381/server/NativePairedCastSafety.abandon(Llocal/lito381/server/NativePlayerVitalCompletions$Receipt;)V
+ALOAD 11
+ATHROW
+ALOAD 10
+ATHROW
+RETURN
--- staged/NativeLightAction/lambda$content$af63fe6e68cfd3899283fefbe1d7b62e0a0f81208c854f8311239d75af7c1a51-e050ba0f.txt
+++ candidate/NativeLightAction/lambda$content$af63fe6e68cfd3899283fefbe1d7b62e0a0f81208c854f8311239d75af7c1a51-e050ba0f.txt
@@ -1,163 +0,0 @@
-lambda$content$af63fe6e68cfd3899283fefbe1d7b62e0a0f81208c854f8311239d75af7c1a51(Llocal/lito381/server/NativeLightAction$Ingress;Llocal/lito381/server/ClientSession;Llocal/lito381/server/NativeSelfCastAction$Task;Llocal/lito381/server/NativePlayerCommit$Binding;Llocal/lito381/server/ClientSession;Llocal/lito381/server/NativePlayerVitalCompletions$Receipt;)V
-(Llocal/lito381/server/NativeLightAction$Ingress;Llocal/lito381/server/ClientSession;Llocal/lito381/server/NativeSelfCastAction$Task;Llocal/lito381/server/NativePlayerCommit$Binding;Llocal/lito381/server/ClientSession;Llocal/lito381/server/NativePlayerVitalCompletions$Receipt;)V
-0xa
-
-
-||||pvis=|pinv=|lvtvis=|lvtinv=
-
-TRY 77:[108:java/io/IOException  , 114:java/lang/Throwable  ]
-TRY 99:[103:java/lang/Error  , 103:java/lang/RuntimeException  ]
-TRY 101:[103:java/lang/Error  , 103:java/lang/RuntimeException  ]
-TRY 109:[114:java/lang/Throwable  ]
-TRY 112:[114:java/lang/Throwable  ]
-TRY 113:[114:java/lang/Throwable  ]
-TRY 136:[140:java/lang/Error  , 140:java/lang/RuntimeException  ]
-TRY 138:[140:java/lang/Error  , 140:java/lang/RuntimeException  ]
-ALOAD 0
-ALOAD 1
-INVOKESTATIC local/lito381/server/NativeSourceMaterialMain.locked(Ljava/lang/Object;Llocal/lito381/server/ClientSession;)Z
-IFNE L5
-RETURN
-ALOAD 0
-GETFIELD local/lito381/server/NativeLightAction$Ingress.binding:Llocal/lito381/server/NativePlayerCommit$Binding;
-ALOAD 1
-ALOAD 2
-INVOKESTATIC local/lito381/server/NativeSourceMaterialPairGuard.matches(Llocal/lito381/server/NativePlayerCommit$Binding;Llocal/lito381/server/ClientSession;Llocal/lito381/server/NativeSelfCastAction$Task;)Z
-IFEQ L48
-ALOAD 3
-ALOAD 4
-ALOAD 2
-INVOKESTATIC local/lito381/server/NativeSourceMaterialPairGuard.matches(Llocal/lito381/server/NativePlayerCommit$Binding;Llocal/lito381/server/ClientSession;Llocal/lito381/server/NativeSelfCastAction$Task;)Z
-IFEQ L48
-ALOAD 4
-CONST str:"hub"
-INVOKESTATIC local/lito381/server/CombatAccess.field(Ljava/lang/Object;Ljava/lang/String;)Ljava/lang/Object;
-ALOAD 0
-GETFIELD local/lito381/server/NativeLightAction$Ingress.hub:Llocal/lito381/server/GameHub;
-IF_ACMPNE L48
-ALOAD 1
-CONST str:"sourceItemDisconnected"
-INVOKESTATIC local/lito381/server/CombatAccess.field(Ljava/lang/Object;Ljava/lang/String;)Ljava/lang/Object;
-CHECKCAST java/lang/Boolean
-INVOKEVIRTUAL java/lang/Boolean.booleanValue()Z
-IFNE L48
-ALOAD 4
-CONST str:"sourceItemDisconnected"
-INVOKESTATIC local/lito381/server/CombatAccess.field(Ljava/lang/Object;Ljava/lang/String;)Ljava/lang/Object;
-CHECKCAST java/lang/Boolean
-INVOKEVIRTUAL java/lang/Boolean.booleanValue()Z
-IFNE L48
-ALOAD 0
-GETFIELD local/lito381/server/NativeLightAction$Ingress.hub:Llocal/lito381/server/GameHub;
-CONST str:"sourceItemApplication"
-INVOKESTATIC local/lito381/server/CombatAccess.field(Ljava/lang/Object;Ljava/lang/String;)Ljava/lang/Object;
-ALOAD 0
-GETFIELD local/lito381/server/NativeLightAction$Ingress.application:Llocal/lito381/server/NativeDeItemApplication;
-IF_ACMPNE L48
-ALOAD 0
-GETFIELD local/lito381/server/NativeLightAction$Ingress.application:Llocal/lito381/server/NativeDeItemApplication;
-IFNULL L49
-ALOAD 0
-GETFIELD local/lito381/server/NativeLightAction$Ingress.application:Llocal/lito381/server/NativeDeItemApplication;
-GETFIELD local/lito381/server/NativeDeItemApplication.closed:Z
-IFEQ L49
-RETURN
-NEW local/lito381/server/NativeLightAction$Batch
-DUP
-ALOAD 1
-ALOAD 0
-GETFIELD local/lito381/server/NativeLightAction$Ingress.arrival:Llocal/lito381/server/NativeCastTimeline$Arrival;
-ALOAD 0
-INVOKESPECIAL local/lito381/server/NativeLightAction$Batch.<init>(Llocal/lito381/server/ClientSession;Llocal/lito381/server/NativeCastTimeline$Arrival;Llocal/lito381/server/NativeLightAction$Ingress;)V
-ASTORE 6
-ALOAD 6
-CONST Integer:1
-PUTFIELD local/lito381/server/NativeLightAction$Batch.captureSelf:Z
-ALOAD 6
-ALOAD 4
-PUTFIELD local/lito381/server/NativeLightAction$Batch.target:Llocal/lito381/server/ClientSession;
-ALOAD 6
-ALOAD 3
-PUTFIELD local/lito381/server/NativeLightAction$Batch.targetBinding:Llocal/lito381/server/NativePlayerCommit$Binding;
-ALOAD 6
-ALOAD 5
-PUTFIELD local/lito381/server/NativeLightAction$Batch.targetReceipt:Llocal/lito381/server/NativePlayerVitalCompletions$Receipt;
-GETSTATIC local/lito381/server/NativeLightAction.current:Ljava/lang/ThreadLocal;
-INVOKEVIRTUAL java/lang/ThreadLocal.get()Ljava/lang/Object;
-CHECKCAST local/lito381/server/NativeLightAction$Batch
-ASTORE 7
-GETSTATIC local/lito381/server/NativeLightAction.current:Ljava/lang/ThreadLocal;
-ALOAD 6
-INVOKEVIRTUAL java/lang/ThreadLocal.set(Ljava/lang/Object;)V
-ALOAD 2
-INVOKEINTERFACE local/lito381/server/NativeSelfCastAction$Task.run()V itf
-ALOAD 7
-IFNONNULL L83
-GETSTATIC local/lito381/server/NativeLightAction.current:Ljava/lang/ThreadLocal;
-INVOKEVIRTUAL java/lang/ThreadLocal.remove()V
-GOTO L86
-GETSTATIC local/lito381/server/NativeLightAction.current:Ljava/lang/ThreadLocal;
-ALOAD 7
-INVOKEVIRTUAL java/lang/ThreadLocal.set(Ljava/lang/Object;)V
-ALOAD 6
-GETFIELD local/lito381/server/NativeLightAction$Batch.outputs:Ljava/util/List;
-INVOKEINTERFACE java/util/List.isEmpty()Z itf
-IFEQ L94
-ALOAD 6
-GETFIELD local/lito381/server/NativeLightAction$Batch.completions:Ljava/util/List;
-INVOKEINTERFACE java/util/List.isEmpty()Z itf
-IFNE L147
-ALOAD 1
-ALOAD 4
-ALOAD 5
-INVOKESTATIC local/lito381/server/NativePairedCastSafety.transfer(Llocal/lito381/server/ClientSession;Llocal/lito381/server/ClientSession;Llocal/lito381/server/NativePlayerVitalCompletions$Receipt;)V
-ALOAD 0
-GETFIELD local/lito381/server/NativeLightAction$Ingress.receipt:Llocal/lito381/server/NativePlayerVitalCompletions$Receipt;
-ALOAD 6
-INVOKESTATIC local/lito381/server/NativePlayerVitalCompletions.enqueueLocked(Llocal/lito381/server/NativePlayerVitalCompletions$Receipt;Ljava/lang/Runnable;)V
-GOTO L147
-ASTORE 8
-ALOAD 5
-INVOKESTATIC local/lito381/server/NativePairedCastSafety.abandon(Llocal/lito381/server/NativePlayerVitalCompletions$Receipt;)V
-ALOAD 8
-ATHROW
-ASTORE 9
-NEW java/io/UncheckedIOException
-DUP
-ALOAD 9
-INVOKESPECIAL java/io/UncheckedIOException.<init>(Ljava/io/IOException;)V
-ATHROW
-ASTORE 10
-ALOAD 7
-IFNONNULL L120
-GETSTATIC local/lito381/server/NativeLightAction.current:Ljava/lang/ThreadLocal;
-INVOKEVIRTUAL java/lang/ThreadLocal.remove()V
-GOTO L123
-GETSTATIC local/lito381/server/NativeLightAction.current:Ljava/lang/ThreadLocal;
-ALOAD 7
-INVOKEVIRTUAL java/lang/ThreadLocal.set(Ljava/lang/Object;)V
-ALOAD 6
-GETFIELD local/lito381/server/NativeLightAction$Batch.outputs:Ljava/util/List;
-INVOKEINTERFACE java/util/List.isEmpty()Z itf
-IFEQ L131
-ALOAD 6
-GETFIELD local/lito381/server/NativeLightAction$Batch.completions:Ljava/util/List;
-INVOKEINTERFACE java/util/List.isEmpty()Z itf
-IFNE L145
-ALOAD 1
-ALOAD 4
-ALOAD 5
-INVOKESTATIC local/lito381/server/NativePairedCastSafety.transfer(Llocal/lito381/server/ClientSession;Llocal/lito381/server/ClientSession;Llocal/lito381/server/NativePlayerVitalCompletions$Receipt;)V
-ALOAD 0
-GETFIELD local/lito381/server/NativeLightAction$Ingress.receipt:Llocal/lito381/server/NativePlayerVitalCompletions$Receipt;
-ALOAD 6
-INVOKESTATIC local/lito381/server/NativePlayerVitalCompletions.enqueueLocked(Llocal/lito381/server/NativePlayerVitalCompletions$Receipt;Ljava/lang/Runnable;)V
-GOTO L145
-ASTORE 11
-ALOAD 5
-INVOKESTATIC local/lito381/server/NativePairedCastSafety.abandon(Llocal/lito381/server/NativePlayerVitalCompletions$Receipt;)V
-ALOAD 11
-ATHROW
-ALOAD 10
-ATHROW
-RETURN
--- staged/NativeLightAction/lambda$content$b93037e7b70cc60f32950364f5b80c1fc077fc616c9138dff25da83806318b36-800bed51.txt
+++ candidate/NativeLightAction/lambda$content$b93037e7b70cc60f32950364f5b80c1fc077fc616c9138dff25da83806318b36-800bed51.txt
@@ -1,18 +0,0 @@
-lambda$content$b93037e7b70cc60f32950364f5b80c1fc077fc616c9138dff25da83806318b36(Llocal/lito381/server/NativeLightAction$Batch;Llocal/lito381/server/NativeSelfCastAction$Task;)V
-(Llocal/lito381/server/NativeLightAction$Batch;Llocal/lito381/server/NativeSelfCastAction$Task;)V
-0xa
-
-java/io/IOException
-||||pvis=|pinv=|lvtvis=|lvtinv=
-
-TRY
-ALOAD 0
-GETFIELD local/lito381/server/NativeLightAction$Batch.accepted:Llocal/lito381/server/NativeLightAction$Ingress;
-GETFIELD local/lito381/server/NativeLightAction$Ingress.binding:Llocal/lito381/server/NativePlayerCommit$Binding;
-ALOAD 0
-GETFIELD local/lito381/server/NativeLightAction$Batch.pc:Llocal/lito381/server/ClientSession;
-INVOKEVIRTUAL local/lito381/server/NativePlayerCommit$Binding.matches(Llocal/lito381/server/ClientSession;)Z
-IFEQ L9
-ALOAD 1
-INVOKEINTERFACE local/lito381/server/NativeSelfCastAction$Task.run()V itf
-RETURN
--- staged/NativeLightAction/lambda$content$c14421f5728713fd49de4ba61c87c1ee05dc40331444195d89a4ad9b48a6f726-6a05efe7.txt
+++ candidate/NativeLightAction/lambda$content$c14421f5728713fd49de4ba61c87c1ee05dc40331444195d89a4ad9b48a6f726-6a05efe7.txt
@@ -0,0 +1,125 @@
+lambda$content$c14421f5728713fd49de4ba61c87c1ee05dc40331444195d89a4ad9b48a6f726(Llocal/lito381/server/NativeLightAction$Ingress;Llocal/lito381/server/ClientSession;Llocal/lito381/server/NativeSelfCastAction$Task;)V
+(Llocal/lito381/server/NativeLightAction$Ingress;Llocal/lito381/server/ClientSession;Llocal/lito381/server/NativeSelfCastAction$Task;)V
+0xa
+
+
+||||pvis=|pinv=|lvtvis=|lvtinv=
+
+TRY 54:[80:java/io/IOException  , 86:java/lang/Throwable  ]
+TRY 81:[86:java/lang/Throwable  ]
+TRY 84:[86:java/lang/Throwable  ]
+TRY 85:[86:java/lang/Throwable  ]
+ALOAD 0
+ALOAD 1
+INVOKESTATIC local/lito381/server/NativeSourceMaterialMain.locked(Ljava/lang/Object;Llocal/lito381/server/ClientSession;)Z
+IFNE L5
+RETURN
+ALOAD 0
+GETFIELD local/lito381/server/NativeLightAction$Ingress.binding:Llocal/lito381/server/NativePlayerCommit$Binding;
+ALOAD 1
+ALOAD 2
+INVOKESTATIC local/lito381/server/NativeSourceSelfFinalFrame.matches(Llocal/lito381/server/NativePlayerCommit$Binding;Llocal/lito381/server/ClientSession;Llocal/lito381/server/NativeSelfCastAction$Task;)Z
+IFEQ L31
+ALOAD 1
+CONST str:"sourceItemDisconnected"
+INVOKESTATIC local/lito381/server/CombatAccess.field(Ljava/lang/Object;Ljava/lang/String;)Ljava/lang/Object;
+CHECKCAST java/lang/Boolean
+INVOKEVIRTUAL java/lang/Boolean.booleanValue()Z
+IFNE L31
+ALOAD 0
+GETFIELD local/lito381/server/NativeLightAction$Ingress.hub:Llocal/lito381/server/GameHub;
+CONST str:"sourceItemApplication"
+INVOKESTATIC local/lito381/server/CombatAccess.field(Ljava/lang/Object;Ljava/lang/String;)Ljava/lang/Object;
+ALOAD 0
+GETFIELD local/lito381/server/NativeLightAction$Ingress.application:Llocal/lito381/server/NativeDeItemApplication;
+IF_ACMPNE L31
+ALOAD 0
+GETFIELD local/lito381/server/NativeLightAction$Ingress.application:Llocal/lito381/server/NativeDeItemApplication;
+IFNULL L32
+ALOAD 0
+GETFIELD local/lito381/server/NativeLightAction$Ingress.application:Llocal/lito381/server/NativeDeItemApplication;
+GETFIELD local/lito381/server/NativeDeItemApplication.closed:Z
+IFEQ L32
+RETURN
+NEW local/lito381/server/NativeLightAction$Batch
+DUP
+ALOAD 1
+ALOAD 0
+GETFIELD local/lito381/server/NativeLightAction$Ingress.arrival:Llocal/lito381/server/NativeCastTimeline$Arrival;
+ALOAD 0
+INVOKESPECIAL local/lito381/server/NativeLightAction$Batch.<init>(Llocal/lito381/server/ClientSession;Llocal/lito381/server/NativeCastTimeline$Arrival;Llocal/lito381/server/NativeLightAction$Ingress;)V
+ASTORE 3
+ALOAD 3
+ALOAD 2
+INVOKESTATIC local/lito381/server/NativeSourceSelfFinalFrame.install(Ljava/lang/Object;Llocal/lito381/server/NativeSelfCastAction$Task;)V
+ALOAD 3
+CONST Integer:1
+PUTFIELD local/lito381/server/NativeLightAction$Batch.captureSelf:Z
+GETSTATIC local/lito381/server/NativeLightAction.current:Ljava/lang/ThreadLocal;
+INVOKEVIRTUAL java/lang/ThreadLocal.get()Ljava/lang/Object;
+CHECKCAST local/lito381/server/NativeLightAction$Batch
+ASTORE 4
+GETSTATIC local/lito381/server/NativeLightAction.current:Ljava/lang/ThreadLocal;
+ALOAD 3
+INVOKEVIRTUAL java/lang/ThreadLocal.set(Ljava/lang/Object;)V
+ALOAD 2
+INVOKEINTERFACE local/lito381/server/NativeSelfCastAction$Task.run()V itf
+ALOAD 4
+IFNONNULL L62
+ALOAD 3
+INVOKESTATIC local/lito381/server/NativeSourceSelfFinalFrame.produced(Ljava/lang/Object;)V
+GETSTATIC local/lito381/server/NativeLightAction.current:Ljava/lang/ThreadLocal;
+INVOKEVIRTUAL java/lang/ThreadLocal.remove()V
+GOTO L67
+ALOAD 3
+INVOKESTATIC local/lito381/server/NativeSourceSelfFinalFrame.produced(Ljava/lang/Object;)V
+GETSTATIC local/lito381/server/NativeLightAction.current:Ljava/lang/ThreadLocal;
+ALOAD 4
+INVOKEVIRTUAL java/lang/ThreadLocal.set(Ljava/lang/Object;)V
+ALOAD 3
+GETFIELD local/lito381/server/NativeLightAction$Batch.outputs:Ljava/util/List;
+INVOKEINTERFACE java/util/List.isEmpty()Z itf
+IFEQ L75
+ALOAD 3
+GETFIELD local/lito381/server/NativeLightAction$Batch.completions:Ljava/util/List;
+INVOKEINTERFACE java/util/List.isEmpty()Z itf
+IFNE L113
+ALOAD 0
+GETFIELD local/lito381/server/NativeLightAction$Ingress.receipt:Llocal/lito381/server/NativePlayerVitalCompletions$Receipt;
+ALOAD 3
+INVOKESTATIC local/lito381/server/NativePlayerVitalCompletions.enqueueLocked(Llocal/lito381/server/NativePlayerVitalCompletions$Receipt;Ljava/lang/Runnable;)V
+GOTO L113
+ASTORE 5
+NEW java/io/UncheckedIOException
+DUP
+ALOAD 5
+INVOKESPECIAL java/io/UncheckedIOException.<init>(Ljava/io/IOException;)V
+ATHROW
+ASTORE 6
+ALOAD 4
+IFNONNULL L94
+ALOAD 3
+INVOKESTATIC local/lito381/server/NativeSourceSelfFinalFrame.produced(Ljava/lang/Object;)V
+GETSTATIC local/lito381/server/NativeLightAction.current:Ljava/lang/ThreadLocal;
+INVOKEVIRTUAL java/lang/ThreadLocal.remove()V
+GOTO L99
+ALOAD 3
+INVOKESTATIC local/lito381/server/NativeSourceSelfFinalFrame.produced(Ljava/lang/Object;)V
+GETSTATIC local/lito381/server/NativeLightAction.current:Ljava/lang/ThreadLocal;
+ALOAD 4
+INVOKEVIRTUAL java/lang/ThreadLocal.set(Ljava/lang/Object;)V
+ALOAD 3
+GETFIELD local/lito381/server/NativeLightAction$Batch.outputs:Ljava/util/List;
+INVOKEINTERFACE java/util/List.isEmpty()Z itf
+IFEQ L107
+ALOAD 3
+GETFIELD local/lito381/server/NativeLightAction$Batch.completions:Ljava/util/List;
+INVOKEINTERFACE java/util/List.isEmpty()Z itf
+IFNE L111
+ALOAD 0
+GETFIELD local/lito381/server/NativeLightAction$Ingress.receipt:Llocal/lito381/server/NativePlayerVitalCompletions$Receipt;
+ALOAD 3
+INVOKESTATIC local/lito381/server/NativePlayerVitalCompletions.enqueueLocked(Llocal/lito381/server/NativePlayerVitalCompletions$Receipt;Ljava/lang/Runnable;)V
+ALOAD 6
+ATHROW
+RETURN
--- staged/NativeLightAction/lambda$content$feb21681a7a728f6083a06149f5c2fd605497b164c3f5581edc8c1a2fe120f3c-800bed51.txt
+++ candidate/NativeLightAction/lambda$content$feb21681a7a728f6083a06149f5c2fd605497b164c3f5581edc8c1a2fe120f3c-800bed51.txt
@@ -0,0 +1,19 @@
+lambda$content$feb21681a7a728f6083a06149f5c2fd605497b164c3f5581edc8c1a2fe120f3c(Llocal/lito381/server/NativeLightAction$Batch;Llocal/lito381/server/NativeSelfCastAction$Task;)V
+(Llocal/lito381/server/NativeLightAction$Batch;Llocal/lito381/server/NativeSelfCastAction$Task;)V
+0xa
+
+java/io/IOException
+||||pvis=|pinv=|lvtvis=|lvtinv=
+
+TRY
+ALOAD 0
+GETFIELD local/lito381/server/NativeLightAction$Batch.accepted:Llocal/lito381/server/NativeLightAction$Ingress;
+GETFIELD local/lito381/server/NativeLightAction$Ingress.binding:Llocal/lito381/server/NativePlayerCommit$Binding;
+ALOAD 0
+GETFIELD local/lito381/server/NativeLightAction$Batch.pc:Llocal/lito381/server/ClientSession;
+INVOKEVIRTUAL local/lito381/server/NativePlayerCommit$Binding.matches(Llocal/lito381/server/ClientSession;)Z
+IFEQ L10
+GOTO L8
+ALOAD 1
+INVOKEINTERFACE local/lito381/server/NativeSelfCastAction$Task.run()V itf
+RETURN
--- staged/NativeLightAction/rethrowCandidateFailure-879483a0.txt
+++ candidate/NativeLightAction/rethrowCandidateFailure-879483a0.txt
@@ -1,10 +0,0 @@
-rethrowCandidateFailure(Ljava/lang/Throwable;)V
-(Ljava/lang/Throwable;)V
-0xa
-<E:Ljava/lang/Throwable;>(Ljava/lang/Throwable;)V^TE;
-java/lang/Throwable
-||||pvis=|pinv=|lvtvis=|lvtinv=
-
-TRY
-ALOAD 0
-ATHROW
--- staged/NativeLightAction/storeReady-32f44f07.txt
+++ candidate/NativeLightAction/storeReady-32f44f07.txt
@@ -8 +8 @@
-TRY 6:[8:java/lang/Throwable  ]
+TRY 6:[12:java/lang/Throwable  ]
@@ -16 +16,5 @@
-GOTO L15
+ALOAD 0
+ALOAD 4
+CONST null
+INVOKESTATIC local/lito381/server/NativeSourceManualScope.exitState(Llocal/lito381/server/ClientSession;Llocal/lito381/server/NativePlayerActionScope$Ticket;Ljava/lang/Throwable;)V
+GOTO L19
@@ -24,4 +27,0 @@
-ALOAD 0
-ALOAD 4
-CONST null
-INVOKESTATIC local/lito381/server/NativeSourceManualScope.exitState(Llocal/lito381/server/ClientSession;Llocal/lito381/server/NativePlayerActionScope$Ticket;Ljava/lang/Throwable;)V
```

</details>

### NativeLightAction$Batch — DIFF

```json
{
  "where": "method",
  "member": "emit()V",
  "insnIndex": null,
  "detail": "left=1:[3:java/lang/Throwable  , 13:java/lang/Throwable  ] right=1:[6:java/lang/Throwable  ]; path event multisets differ (left=7, right=4)"
}
```
<details>
<summary>全部方法診斷</summary>

```json
[
  {
    "method": "emit()V",
    "status": "DIFF",
    "detail": "method emit()V: left=1:[3:java/lang/Throwable  , 13:java/lang/Throwable  ] right=1:[6:java/lang/Throwable  ]; path event multisets differ (left=7, right=4)"
  },
  {
    "method": "nativeSourceBatchOriginalEmit()V",
    "status": "DIFF",
    "detail": "method nativeSourceBatchOriginalEmit()V: left=java/io/IOException right="
  }
]
```

</details>
<details>
<summary>完整正規化差異</summary>

```diff
--- staged/NativeLightAction$Batch/emit-9b75.txt
+++ candidate/NativeLightAction$Batch/emit-9b75.txt
@@ -8,4 +8 @@
-TRY 1:[3:java/lang/Throwable  , 13:java/lang/Throwable  ]
-TRY 6:[13:java/lang/Throwable  ]
-TRY 8:[13:java/lang/Throwable  ]
-TRY 11:[13:java/lang/Throwable  ]
+TRY 1:[6:java/lang/Throwable  ]
@@ -13,2 +10,5 @@
-INVOKEVIRTUAL local/lito381/server/NativeLightAction$Batch.nativeSourceBatchOriginalEmit()V
-GOTO L9
+INVOKESPECIAL local/lito381/server/NativeLightAction$Batch.nativeSourceBatchOriginalEmit()V
+ALOAD 0
+CONST null
+INVOKESTATIC local/lito381/server/NativeSourceTargetIconBatch.finish(Ljava/lang/Object;Ljava/lang/Throwable;)V
+RETURN
@@ -21,8 +20,0 @@
-ALOAD 0
-CONST null
-INVOKESTATIC local/lito381/server/NativeSourceTargetIconBatch.finish(Ljava/lang/Object;Ljava/lang/Throwable;)V
-GOTO L16
-ASTORE 2
-ALOAD 2
-INVOKESTATIC local/lito381/server/NativeLightAction.rethrowCandidateFailure(Ljava/lang/Throwable;)V
-RETURN
--- staged/NativeLightAction$Batch/nativeSourceBatchOriginalEmit-9b75.txt
+++ candidate/NativeLightAction$Batch/nativeSourceBatchOriginalEmit-9b75.txt
@@ -5 +5 @@
-java/io/IOException
+
@@ -75 +75 @@
-IFNE L130
+IFNE L8
```

</details>

### NativeMassTeleportAnswerLife — SAME_PATHS

<details>
<summary>全部方法診斷</summary>

```json
[
  {
    "method": "clear(Llocal/lito381/server/ClientSession;Ljava/lang/Object;)V",
    "status": "SAME_PATHS",
    "detail": "path multisets equal (10)"
  },
  {
    "method": "token(Llocal/lito381/server/ClientSession;)Ljava/lang/Object;",
    "status": "SAME_PATHS",
    "detail": "path multisets equal (12)"
  }
]
```

</details>
<details>
<summary>完整正規化差異</summary>

```diff
--- staged/NativeMassTeleportAnswerLife/clear-bbb34ec2.txt
+++ candidate/NativeMassTeleportAnswerLife/clear-bbb34ec2.txt
@@ -8,2 +8,2 @@
-TRY 8:[14:java/lang/Throwable  ]
-TRY 12:[14:java/lang/Throwable  ]
+TRY 8:[19:java/lang/Throwable  ]
+TRY 12:[19:java/lang/Throwable  ]
@@ -23 +23,6 @@
-GOTO L22
+ALOAD 2
+ALOAD 0
+ALOAD 3
+CONST null
+INVOKESTATIC local/lito381/server/NativeSourceManualScope.unlock(Ljava/util/concurrent/locks/ReentrantLock;Llocal/lito381/server/ClientSession;Llocal/lito381/server/NativePlayerActionScope$Ticket;Ljava/lang/Throwable;)V
+GOTO L27
@@ -32,5 +36,0 @@
-ALOAD 2
-ALOAD 0
-ALOAD 3
-CONST null
-INVOKESTATIC local/lito381/server/NativeSourceManualScope.unlock(Ljava/util/concurrent/locks/ReentrantLock;Llocal/lito381/server/ClientSession;Llocal/lito381/server/NativePlayerActionScope$Ticket;Ljava/lang/Throwable;)V
--- staged/NativeMassTeleportAnswerLife/token-5acd438c.txt
+++ candidate/NativeMassTeleportAnswerLife/token-5acd438c.txt
@@ -8,5 +8,5 @@
-TRY 8:[19:java/lang/Throwable  ]
-TRY 11:[19:java/lang/Throwable  ]
-TRY 13:[19:java/lang/Throwable  ]
-TRY 14:[19:java/lang/Throwable  ]
-TRY 16:[19:java/lang/Throwable  ]
+TRY 8:[25:java/lang/Throwable  ]
+TRY 11:[25:java/lang/Throwable  ]
+TRY 13:[25:java/lang/Throwable  ]
+TRY 14:[25:java/lang/Throwable  ]
+TRY 16:[25:java/lang/Throwable  ]
@@ -31 +31,7 @@
-GOTO L27
+ALOAD 1
+ALOAD 0
+ALOAD 2
+CONST null
+INVOKESTATIC local/lito381/server/NativeSourceManualScope.unlock(Ljava/util/concurrent/locks/ReentrantLock;Llocal/lito381/server/ClientSession;Llocal/lito381/server/NativePlayerActionScope$Ticket;Ljava/lang/Throwable;)V
+ALOAD 3
+ARETURN
@@ -40,7 +45,0 @@
-ALOAD 1
-ALOAD 0
-ALOAD 2
-CONST null
-INVOKESTATIC local/lito381/server/NativeSourceManualScope.unlock(Ljava/util/concurrent/locks/ReentrantLock;Llocal/lito381/server/ClientSession;Llocal/lito381/server/NativePlayerActionScope$Ticket;Ljava/lang/Throwable;)V
-ALOAD 3
-ARETURN
```

</details>

### NativePlayerActionScope — SAME_PATHS

<details>
<summary>全部方法診斷</summary>

```json
[
  {
    "method": "blocked(Ljava/lang/Runnable;)Llocal/lito381/server/NativePlayerActionScope$Root;",
    "status": "SAME_PATHS",
    "detail": "path multisets equal (7)"
  }
]
```

</details>
<details>
<summary>完整正規化差異</summary>

```diff
--- staged/NativePlayerActionScope/blocked-61b6f7f7.txt
+++ candidate/NativePlayerActionScope/blocked-61b6f7f7.txt
@@ -11,3 +11 @@
-IFNE L5
-CONST null
-ARETURN
+IFEQ L13
@@ -20 +18,4 @@
-IFEQ L14
+IFNE L13
+ALOAD 1
+GETFIELD local/lito381/server/NativePlayerActionScope$Gated.root:Llocal/lito381/server/NativePlayerActionScope$Root;
+GOTO L14
@@ -23,5 +23,0 @@
-ALOAD 1
-GETFIELD local/lito381/server/NativePlayerActionScope$Gated.root:Llocal/lito381/server/NativePlayerActionScope$Root;
-ASTORE 2
-ALOAD 2
-ARETURN
--- staged/NativePlayerActionScope/resumeAfterDrain-32855332.txt
+++ candidate/NativePlayerActionScope/resumeAfterDrain-32855332.txt
@@ -8,5 +8,5 @@
-TRY 8:[22:java/lang/Throwable  ]
-TRY 12:[22:java/lang/Throwable  ]
-TRY 17:[22:java/lang/Throwable  ]
-TRY 20:[22:java/lang/Throwable  ]
-TRY 24:[22:java/lang/Throwable  ]
+TRY 6:[20:java/lang/Throwable  ]
+TRY 10:[20:java/lang/Throwable  ]
+TRY 15:[20:java/lang/Throwable  ]
+TRY 18:[20:java/lang/Throwable  ]
+TRY 22:[20:java/lang/Throwable  ]
@@ -14,0 +15 @@
+DUP
@@ -16,3 +16,0 @@
-ALOAD 2
-DUP
-ASTORE 3
@@ -22 +20 @@
-IFEQ L14
+IFEQ L12
@@ -24 +22 @@
-ALOAD 3
+ALOAD 2
@@ -32 +30 @@
-ALOAD 3
+ALOAD 2
@@ -35 +33,3 @@
-ASTORE 4
+ASTORE 3
+ALOAD 2
+MONITOREXIT
@@ -37,2 +36,0 @@
-MONITOREXIT
-ALOAD 4
```

</details>

### NativePlayerCommit — DIFF

```json
{
  "where": "method",
  "member": "acceptedTail(Llocal/lito381/server/NativePlayerCommit$Frame;)V",
  "insnIndex": null,
  "detail": "left=23:[133:local/lito381/server/NativeAcceptedState$Stale  , 191:java/lang/Throwable  ] right=23:[124:local/lito381/server/NativeAcceptedState$Stale  , 173:java/lang/Throwable  ]; paths rejected: path limit 4096 exceeded"
}
```
<details>
<summary>全部方法診斷</summary>

```json
[
  {
    "method": "acceptedTail(Llocal/lito381/server/NativePlayerCommit$Frame;)V",
    "status": "DIFF",
    "detail": "method acceptedTail(Llocal/lito381/server/NativePlayerCommit$Frame;)V: left=23:[133:local/lito381/server/NativeAcceptedState$Stale  , 191:java/lang/Throwable  ] right=23:[124:local/lito381/server/NativeAcceptedState$Stale  , 173:java/lang/Throwable  ]; paths rejected: path limit 4096 exceeded"
  },
  {
    "method": "assign(Llocal/lito381/server/ClientSession;Ljava/lang/Object;Ljava/lang/String;)V",
    "status": "DIFF",
    "detail": "method assign(Llocal/lito381/server/ClientSession;Ljava/lang/Object;Ljava/lang/String;)V: left=8:[18:java/lang/Throwable  ] right=8:[49:java/lang/Throwable  ]; path event multisets differ (left=14, right=18)"
  },
  {
    "method": "calculateWorld(Llocal/lito381/server/ClientSession;IJLlocal/lito381/server/NativePlayerCommit$WorldPlan;)V",
    "status": "DIFF",
    "detail": "method calculateWorld(Llocal/lito381/server/ClientSession;IJLlocal/lito381/server/NativePlayerCommit$WorldPlan;)V: left=11:[33:java/lang/Throwable  ] right=11:[34:java/lang/Throwable  ]; path event multisets differ (left=47, right=47)"
  },
  {
    "method": "captureBattleDefense(Llocal/lito381/server/ClientSession;IZLjava/lang/String;)I",
    "status": "SAME_PATHS",
    "detail": "path multisets equal (96)"
  },
  {
    "method": "captureWorldDefense(Llocal/lito381/server/ClientSession;IZLjava/lang/String;DLlocal/lito381/server/BattleAbilityStatus$IllusionAvatar;)I",
    "status": "SAME_PATHS",
    "detail": "path multisets equal (386)"
  },
  {
    "method": "drain(Llocal/lito381/server/ClientSession;Llocal/lito381/server/NativePlayerCommit$Queue;)V",
    "status": "DIFF",
    "detail": "method drain(Llocal/lito381/server/ClientSession;Llocal/lito381/server/NativePlayerCommit$Queue;)V: left=10:[258:java/lang/Throwable  ] right=10:[248:java/lang/Throwable  ]; paths rejected: normal control-flow cycle"
  },
  {
    "method": "drawDefense(Llocal/lito381/server/ClientSession;Ljava/lang/String;)Llocal/lito381/server/NativeMagicDollEffects$DefenseProc;",
    "status": "DIFF",
    "detail": "method drawDefense(Llocal/lito381/server/ClientSession;Ljava/lang/String;)Llocal/lito381/server/NativeMagicDollEffects$DefenseProc;: left=21:[27:java/lang/Throwable  ] right=19:[25:java/lang/Throwable  ]; paths rejected: normal control-flow cycle"
  },
  {
    "method": "earlyZero(Llocal/lito381/server/ClientSession;Llocal/lito381/server/ClientSession;ZJ)Z",
    "status": "DIFF",
    "detail": "method earlyZero(Llocal/lito381/server/ClientSession;Llocal/lito381/server/ClientSession;ZJ)Z: left=33:[101:java/lang/Throwable  ] right=33:[93:java/lang/Throwable  ]; paths rejected: path limit 4096 exceeded"
  },
  {
    "method": "nativeCompanionSceneAssign(Llocal/lito381/server/ClientSession;Ljava/lang/Object;Ljava/lang/String;)V",
    "status": "DIFF",
    "detail": "method nativeCompanionSceneAssign(Llocal/lito381/server/ClientSession;Ljava/lang/Object;Ljava/lang/String;)V: left=8:[18:java/lang/Throwable  ] right=8:[51:java/lang/Throwable  ]; path event multisets differ (left=14, right=18)"
  },
  {
    "method": "nativeCompanionSceneOriginalAdvance(Llocal/lito381/server/ClientSession;Ljava/lang/Object;)V",
    "status": "DIFF",
    "detail": "method nativeCompanionSceneOriginalAdvance(Llocal/lito381/server/ClientSession;Ljava/lang/Object;)V: left=8:[14:java/lang/Throwable  ] right=8:[88:java/lang/Throwable  ]; path event multisets differ (left=26, right=42)"
  },
  {
    "method": "nativeManaTransferRecover(Llocal/lito381/server/NativePlayerVitalCompletions$Receipt;Llocal/lito381/server/ClientSession;Llocal/lito381/server/NativePlayerCommit$Binding;IILjava/lang/String;)V",
    "status": "DIFF",
    "detail": "method nativeManaTransferRecover(Llocal/lito381/server/NativePlayerVitalCompletions$Receipt;Llocal/lito381/server/ClientSession;Llocal/lito381/server/NativePlayerCommit$Binding;IILjava/lang/String;)V: left=30:[144:java/lang/Throwable  ] right=32:[146:java/lang/Throwable  ]; paths rejected: path limit 4096 exceeded"
  },
  {
    "method": "nativeSourceProc68Queue(Llocal/lito381/server/ClientSession;Llocal/lito381/server/NativePlayerCommit$Binding;Llocal/lito381/server/BattleTimedStatus;Llocal/lito381/server/NativeSourceEquipmentProcLife$Proof;Llocal/lito381/server/NativeProcSkillCatalog$Catalog;Llocal/lito381/server/BattleAbilityStatus;)Llocal/lito381/server/NativePlayerCommit$Queue;",
    "status": "DIFF",
    "detail": "method nativeSourceProc68Queue(Llocal/lito381/server/ClientSession;Llocal/lito381/server/NativePlayerCommit$Binding;Llocal/lito381/server/BattleTimedStatus;Llocal/lito381/server/NativeSourceEquipmentProcLife$Proof;Llocal/lito381/server/NativeProcSkillCatalog$Catalog;Llocal/lito381/server/BattleAbilityStatus;)Llocal/lito381/server/NativePlayerCommit$Queue;: left=14:[64:java/lang/Throwable  ] right=12:[61:java/lang/Throwable  ]; path event multisets differ (left=47, right=47)"
  },
  {
    "method": "nativeSourceScalarQueue(Llocal/lito381/server/ClientSession;Llocal/lito381/server/NativePlayerCommit$Binding;Llocal/lito381/server/BattleTimedStatus;Llocal/lito381/server/NativeSourceEquipmentProcLife$Proof;Llocal/lito381/server/NativeProcSkillCatalog$Catalog;)Llocal/lito381/server/NativePlayerCommit$Queue;",
    "status": "DIFF",
    "detail": "method nativeSourceScalarQueue(Llocal/lito381/server/ClientSession;Llocal/lito381/server/NativePlayerCommit$Binding;Llocal/lito381/server/BattleTimedStatus;Llocal/lito381/server/NativeSourceEquipmentProcLife$Proof;Llocal/lito381/server/NativeProcSkillCatalog$Catalog;)Llocal/lito381/server/NativePlayerCommit$Queue;: left=13:[63:java/lang/Throwable  ] right=11:[60:java/lang/Throwable  ]; path event multisets differ (left=47, right=47)"
  },
  {
    "method": "nativeStaticOriginalAdvance(Llocal/lito381/server/ClientSession;)V",
    "status": "DIFF",
    "detail": "method nativeStaticOriginalAdvance(Llocal/lito381/server/ClientSession;)V: left=8:[14:java/lang/Throwable  ] right=8:[90:java/lang/Throwable  ]; path event multisets differ (left=29, right=48)"
  },
  {
    "method": "publishDefense(Llocal/lito381/server/ClientSession;)V",
    "status": "DIFF",
    "detail": "method publishDefense(Llocal/lito381/server/ClientSession;)V insn[100]: left=GOTO L28 right=GOTO L111; paths rejected: normal control-flow cycle"
  },
  {
    "method": "selected(Llocal/lito381/server/ClientSession;Ljava/lang/String;)V",
    "status": "SAME_PATHS",
    "detail": "path multisets equal (14)"
  },
  {
    "method": "worldPlayer(Llocal/lito381/server/ClientSession;ILlocal/lito381/server/ClientSession;ZZ)V",
    "status": "SAME_PATHS",
    "detail": "path multisets equal (210)"
  }
]
```

</details>
<details>
<summary>完整正規化差異</summary>

```diff
--- staged/NativePlayerCommit/acceptedTail-14aa78c7.txt
+++ candidate/NativePlayerCommit/acceptedTail-14aa78c7.txt
@@ -8,53 +8,53 @@
-TRY 23:[133:local/lito381/server/NativeAcceptedState$Stale  , 191:java/lang/Throwable  ]
-TRY 28:[133:local/lito381/server/NativeAcceptedState$Stale  , 191:java/lang/Throwable  ]
-TRY 34:[133:local/lito381/server/NativeAcceptedState$Stale  , 191:java/lang/Throwable  ]
-TRY 38:[133:local/lito381/server/NativeAcceptedState$Stale  , 191:java/lang/Throwable  ]
-TRY 39:[133:local/lito381/server/NativeAcceptedState$Stale  , 191:java/lang/Throwable  ]
-TRY 40:[133:local/lito381/server/NativeAcceptedState$Stale  , 191:java/lang/Throwable  ]
-TRY 44:[133:local/lito381/server/NativeAcceptedState$Stale  , 191:java/lang/Throwable  ]
-TRY 48:[133:local/lito381/server/NativeAcceptedState$Stale  , 191:java/lang/Throwable  ]
-TRY 49:[133:local/lito381/server/NativeAcceptedState$Stale  , 191:java/lang/Throwable  ]
-TRY 50:[133:local/lito381/server/NativeAcceptedState$Stale  , 191:java/lang/Throwable  ]
-TRY 54:[133:local/lito381/server/NativeAcceptedState$Stale  , 191:java/lang/Throwable  ]
-TRY 55:[133:local/lito381/server/NativeAcceptedState$Stale  , 191:java/lang/Throwable  ]
-TRY 56:[133:local/lito381/server/NativeAcceptedState$Stale  , 191:java/lang/Throwable  ]
-TRY 60:[133:local/lito381/server/NativeAcceptedState$Stale  , 191:java/lang/Throwable  ]
-TRY 61:[133:local/lito381/server/NativeAcceptedState$Stale  , 191:java/lang/Throwable  ]
-TRY 62:[133:local/lito381/server/NativeAcceptedState$Stale  , 191:java/lang/Throwable  ]
-TRY 66:[133:local/lito381/server/NativeAcceptedState$Stale  , 191:java/lang/Throwable  ]
-TRY 67:[133:local/lito381/server/NativeAcceptedState$Stale  , 191:java/lang/Throwable  ]
-TRY 71:[133:local/lito381/server/NativeAcceptedState$Stale  , 191:java/lang/Throwable  ]
-TRY 72:[133:local/lito381/server/NativeAcceptedState$Stale  , 191:java/lang/Throwable  ]
-TRY 73:[133:local/lito381/server/NativeAcceptedState$Stale  , 191:java/lang/Throwable  ]
-TRY 74:[133:local/lito381/server/NativeAcceptedState$Stale  , 191:java/lang/Throwable  ]
-TRY 78:[133:local/lito381/server/NativeAcceptedState$Stale  , 191:java/lang/Throwable  ]
-TRY 79:[133:local/lito381/server/NativeAcceptedState$Stale  , 191:java/lang/Throwable  ]
-TRY 93:[103:java/lang/Throwable  , 121:java/lang/Throwable  ]
-TRY 95:[103:java/lang/Throwable  , 121:java/lang/Throwable  ]
-TRY 98:[121:java/lang/Throwable  ]
-TRY 100:[121:java/lang/Throwable  ]
-TRY 101:[121:java/lang/Throwable  ]
-TRY 106:[121:java/lang/Throwable  ]
-TRY 108:[121:java/lang/Throwable  ]
-TRY 109:[121:java/lang/Throwable  ]
-TRY 111:[121:java/lang/Throwable  ]
-TRY 136:[191:java/lang/Throwable  ]
-TRY 137:[191:java/lang/Throwable  ]
-TRY 151:[161:java/lang/Throwable  , 179:java/lang/Throwable  ]
-TRY 153:[161:java/lang/Throwable  , 179:java/lang/Throwable  ]
-TRY 156:[179:java/lang/Throwable  ]
-TRY 158:[179:java/lang/Throwable  ]
-TRY 159:[179:java/lang/Throwable  ]
-TRY 164:[179:java/lang/Throwable  ]
-TRY 166:[179:java/lang/Throwable  ]
-TRY 167:[179:java/lang/Throwable  ]
-TRY 169:[179:java/lang/Throwable  ]
-TRY 204:[214:java/lang/Throwable  , 232:java/lang/Throwable  ]
-TRY 206:[214:java/lang/Throwable  , 232:java/lang/Throwable  ]
-TRY 209:[232:java/lang/Throwable  ]
-TRY 211:[232:java/lang/Throwable  ]
-TRY 212:[232:java/lang/Throwable  ]
-TRY 217:[232:java/lang/Throwable  ]
-TRY 219:[232:java/lang/Throwable  ]
-TRY 220:[232:java/lang/Throwable  ]
-TRY 222:[232:java/lang/Throwable  ]
+TRY 23:[124:local/lito381/server/NativeAcceptedState$Stale  , 173:java/lang/Throwable  ]
+TRY 28:[124:local/lito381/server/NativeAcceptedState$Stale  , 173:java/lang/Throwable  ]
+TRY 34:[124:local/lito381/server/NativeAcceptedState$Stale  , 173:java/lang/Throwable  ]
+TRY 38:[124:local/lito381/server/NativeAcceptedState$Stale  , 173:java/lang/Throwable  ]
+TRY 39:[124:local/lito381/server/NativeAcceptedState$Stale  , 173:java/lang/Throwable  ]
+TRY 40:[124:local/lito381/server/NativeAcceptedState$Stale  , 173:java/lang/Throwable  ]
+TRY 44:[124:local/lito381/server/NativeAcceptedState$Stale  , 173:java/lang/Throwable  ]
+TRY 48:[124:local/lito381/server/NativeAcceptedState$Stale  , 173:java/lang/Throwable  ]
+TRY 49:[124:local/lito381/server/NativeAcceptedState$Stale  , 173:java/lang/Throwable  ]
+TRY 50:[124:local/lito381/server/NativeAcceptedState$Stale  , 173:java/lang/Throwable  ]
+TRY 54:[124:local/lito381/server/NativeAcceptedState$Stale  , 173:java/lang/Throwable  ]
+TRY 55:[124:local/lito381/server/NativeAcceptedState$Stale  , 173:java/lang/Throwable  ]
+TRY 56:[124:local/lito381/server/NativeAcceptedState$Stale  , 173:java/lang/Throwable  ]
+TRY 60:[124:local/lito381/server/NativeAcceptedState$Stale  , 173:java/lang/Throwable  ]
+TRY 61:[124:local/lito381/server/NativeAcceptedState$Stale  , 173:java/lang/Throwable  ]
+TRY 62:[124:local/lito381/server/NativeAcceptedState$Stale  , 173:java/lang/Throwable  ]
+TRY 66:[124:local/lito381/server/NativeAcceptedState$Stale  , 173:java/lang/Throwable  ]
+TRY 67:[124:local/lito381/server/NativeAcceptedState$Stale  , 173:java/lang/Throwable  ]
+TRY 71:[124:local/lito381/server/NativeAcceptedState$Stale  , 173:java/lang/Throwable  ]
+TRY 72:[124:local/lito381/server/NativeAcceptedState$Stale  , 173:java/lang/Throwable  ]
+TRY 73:[124:local/lito381/server/NativeAcceptedState$Stale  , 173:java/lang/Throwable  ]
+TRY 74:[124:local/lito381/server/NativeAcceptedState$Stale  , 173:java/lang/Throwable  ]
+TRY 78:[124:local/lito381/server/NativeAcceptedState$Stale  , 173:java/lang/Throwable  ]
+TRY 79:[124:local/lito381/server/NativeAcceptedState$Stale  , 173:java/lang/Throwable  ]
+TRY 84:[94:java/lang/Throwable  , 112:java/lang/Throwable  ]
+TRY 86:[94:java/lang/Throwable  , 112:java/lang/Throwable  ]
+TRY 89:[112:java/lang/Throwable  ]
+TRY 91:[112:java/lang/Throwable  ]
+TRY 92:[112:java/lang/Throwable  ]
+TRY 97:[112:java/lang/Throwable  ]
+TRY 99:[112:java/lang/Throwable  ]
+TRY 100:[112:java/lang/Throwable  ]
+TRY 102:[112:java/lang/Throwable  ]
+TRY 127:[173:java/lang/Throwable  ]
+TRY 128:[173:java/lang/Throwable  ]
+TRY 133:[143:java/lang/Throwable  , 161:java/lang/Throwable  ]
+TRY 135:[143:java/lang/Throwable  , 161:java/lang/Throwable  ]
+TRY 138:[161:java/lang/Throwable  ]
+TRY 140:[161:java/lang/Throwable  ]
+TRY 141:[161:java/lang/Throwable  ]
+TRY 146:[161:java/lang/Throwable  ]
+TRY 148:[161:java/lang/Throwable  ]
+TRY 149:[161:java/lang/Throwable  ]
+TRY 151:[161:java/lang/Throwable  ]
+TRY 177:[187:java/lang/Throwable  , 205:java/lang/Throwable  ]
+TRY 179:[187:java/lang/Throwable  , 205:java/lang/Throwable  ]
+TRY 182:[205:java/lang/Throwable  ]
+TRY 184:[205:java/lang/Throwable  ]
+TRY 185:[205:java/lang/Throwable  ]
+TRY 190:[205:java/lang/Throwable  ]
+TRY 192:[205:java/lang/Throwable  ]
+TRY 193:[205:java/lang/Throwable  ]
+TRY 195:[205:java/lang/Throwable  ]
@@ -143,10 +143 @@
-IFNE L92
-ALOAD 1
-IFNONNULL L88
-GETSTATIC local/lito381/server/NativePlayerCommit.current:Ljava/lang/ThreadLocal;
-INVOKEVIRTUAL java/lang/ThreadLocal.remove()V
-GOTO L245
-GETSTATIC local/lito381/server/NativePlayerCommit.current:Ljava/lang/ThreadLocal;
-ALOAD 1
-INVOKEVIRTUAL java/lang/ThreadLocal.set(Ljava/lang/Object;)V
-GOTO L245
+IFEQ L103
@@ -163 +154 @@
-GOTO L112
+GOTO L103
@@ -174,8 +165,8 @@
-IFNONNULL L117
-GETSTATIC local/lito381/server/NativePlayerCommit.current:Ljava/lang/ThreadLocal;
-INVOKEVIRTUAL java/lang/ThreadLocal.remove()V
-GOTO L132
-GETSTATIC local/lito381/server/NativePlayerCommit.current:Ljava/lang/ThreadLocal;
-ALOAD 1
-INVOKEVIRTUAL java/lang/ThreadLocal.set(Ljava/lang/Object;)V
-GOTO L132
+IFNONNULL L108
+GETSTATIC local/lito381/server/NativePlayerCommit.current:Ljava/lang/ThreadLocal;
+INVOKEVIRTUAL java/lang/ThreadLocal.remove()V
+GOTO L123
+GETSTATIC local/lito381/server/NativePlayerCommit.current:Ljava/lang/ThreadLocal;
+ALOAD 1
+INVOKEVIRTUAL java/lang/ThreadLocal.set(Ljava/lang/Object;)V
+GOTO L123
@@ -184,4 +175,4 @@
-IFNONNULL L127
-GETSTATIC local/lito381/server/NativePlayerCommit.current:Ljava/lang/ThreadLocal;
-INVOKEVIRTUAL java/lang/ThreadLocal.remove()V
-GOTO L130
+IFNONNULL L118
+GETSTATIC local/lito381/server/NativePlayerCommit.current:Ljava/lang/ThreadLocal;
+INVOKEVIRTUAL java/lang/ThreadLocal.remove()V
+GOTO L121
@@ -193 +184 @@
-GOTO L245
+GOTO L218
@@ -196 +187 @@
-IFEQ L139
+IFEQ L130
@@ -201,10 +192 @@
-IFNE L150
-ALOAD 1
-IFNONNULL L146
-GETSTATIC local/lito381/server/NativePlayerCommit.current:Ljava/lang/ThreadLocal;
-INVOKEVIRTUAL java/lang/ThreadLocal.remove()V
-GOTO L245
-GETSTATIC local/lito381/server/NativePlayerCommit.current:Ljava/lang/ThreadLocal;
-ALOAD 1
-INVOKEVIRTUAL java/lang/ThreadLocal.set(Ljava/lang/Object;)V
-GOTO L245
+IFEQ L152
@@ -220,0 +203,24 @@
+GOTO L152
+ASTORE 10
+ALOAD 2
+ALOAD 5
+GETFIELD local/lito381/server/NativePlayerCommit$Event.attacker:Llocal/lito381/server/NativeCombatAttacker$Ref;
+ALOAD 5
+GETFIELD local/lito381/server/NativePlayerCommit$Event.npcFeedback:Llocal/lito381/server/NativeCombatAttacker$Feedback;
+INVOKESTATIC local/lito381/server/NativeCombatAttacker.acceptedFeedback(Llocal/lito381/server/ClientSession;Llocal/lito381/server/NativeCombatAttacker$Ref;Llocal/lito381/server/NativeCombatAttacker$Feedback;)V
+ALOAD 10
+ATHROW
+ALOAD 1
+IFNONNULL L157
+GETSTATIC local/lito381/server/NativePlayerCommit.current:Ljava/lang/ThreadLocal;
+INVOKEVIRTUAL java/lang/ThreadLocal.remove()V
+GOTO L172
+GETSTATIC local/lito381/server/NativePlayerCommit.current:Ljava/lang/ThreadLocal;
+ALOAD 1
+INVOKEVIRTUAL java/lang/ThreadLocal.set(Ljava/lang/Object;)V
+GOTO L172
+ASTORE 11
+ALOAD 1
+IFNONNULL L167
+GETSTATIC local/lito381/server/NativePlayerCommit.current:Ljava/lang/ThreadLocal;
+INVOKEVIRTUAL java/lang/ThreadLocal.remove()V
@@ -222,24 +227,0 @@
-ASTORE 10
-ALOAD 2
-ALOAD 5
-GETFIELD local/lito381/server/NativePlayerCommit$Event.attacker:Llocal/lito381/server/NativeCombatAttacker$Ref;
-ALOAD 5
-GETFIELD local/lito381/server/NativePlayerCommit$Event.npcFeedback:Llocal/lito381/server/NativeCombatAttacker$Feedback;
-INVOKESTATIC local/lito381/server/NativeCombatAttacker.acceptedFeedback(Llocal/lito381/server/ClientSession;Llocal/lito381/server/NativeCombatAttacker$Ref;Llocal/lito381/server/NativeCombatAttacker$Feedback;)V
-ALOAD 10
-ATHROW
-ALOAD 1
-IFNONNULL L175
-GETSTATIC local/lito381/server/NativePlayerCommit.current:Ljava/lang/ThreadLocal;
-INVOKEVIRTUAL java/lang/ThreadLocal.remove()V
-GOTO L190
-GETSTATIC local/lito381/server/NativePlayerCommit.current:Ljava/lang/ThreadLocal;
-ALOAD 1
-INVOKEVIRTUAL java/lang/ThreadLocal.set(Ljava/lang/Object;)V
-GOTO L190
-ASTORE 11
-ALOAD 1
-IFNONNULL L185
-GETSTATIC local/lito381/server/NativePlayerCommit.current:Ljava/lang/ThreadLocal;
-INVOKEVIRTUAL java/lang/ThreadLocal.remove()V
-GOTO L188
@@ -251 +233 @@
-GOTO L245
+GOTO L218
@@ -254,10 +236 @@
-IFNE L203
-ALOAD 1
-IFNONNULL L199
-GETSTATIC local/lito381/server/NativePlayerCommit.current:Ljava/lang/ThreadLocal;
-INVOKEVIRTUAL java/lang/ThreadLocal.remove()V
-GOTO L243
-GETSTATIC local/lito381/server/NativePlayerCommit.current:Ljava/lang/ThreadLocal;
-ALOAD 1
-INVOKEVIRTUAL java/lang/ThreadLocal.set(Ljava/lang/Object;)V
-GOTO L243
+IFEQ L196
@@ -274 +247 @@
-GOTO L223
+GOTO L196
@@ -285,8 +258,8 @@
-IFNONNULL L228
-GETSTATIC local/lito381/server/NativePlayerCommit.current:Ljava/lang/ThreadLocal;
-INVOKEVIRTUAL java/lang/ThreadLocal.remove()V
-GOTO L243
-GETSTATIC local/lito381/server/NativePlayerCommit.current:Ljava/lang/ThreadLocal;
-ALOAD 1
-INVOKEVIRTUAL java/lang/ThreadLocal.set(Ljava/lang/Object;)V
-GOTO L243
+IFNONNULL L201
+GETSTATIC local/lito381/server/NativePlayerCommit.current:Ljava/lang/ThreadLocal;
+INVOKEVIRTUAL java/lang/ThreadLocal.remove()V
+GOTO L216
+GETSTATIC local/lito381/server/NativePlayerCommit.current:Ljava/lang/ThreadLocal;
+ALOAD 1
+INVOKEVIRTUAL java/lang/ThreadLocal.set(Ljava/lang/Object;)V
+GOTO L216
@@ -295,4 +268,4 @@
-IFNONNULL L238
-GETSTATIC local/lito381/server/NativePlayerCommit.current:Ljava/lang/ThreadLocal;
-INVOKEVIRTUAL java/lang/ThreadLocal.remove()V
-GOTO L241
+IFNONNULL L211
+GETSTATIC local/lito381/server/NativePlayerCommit.current:Ljava/lang/ThreadLocal;
+INVOKEVIRTUAL java/lang/ThreadLocal.remove()V
+GOTO L214
--- staged/NativePlayerCommit/assign-9c9f0c0c.txt
+++ candidate/NativePlayerCommit/assign-9c9f0c0c.txt
@@ -8,7 +8,9 @@
-TRY 8:[18:java/lang/Throwable  ]
-TRY 16:[18:java/lang/Throwable  ]
-TRY 39:[50:java/lang/Throwable  ]
-TRY 42:[50:java/lang/Throwable  ]
-TRY 46:[50:java/lang/Throwable  ]
-TRY 48:[50:java/lang/Throwable  ]
-TRY 52:[50:java/lang/Throwable  ]
+TRY 8:[49:java/lang/Throwable  ]
+TRY 15:[49:java/lang/Throwable  ]
+TRY 25:[49:java/lang/Throwable  ]
+TRY 27:[38:java/lang/Throwable  , 49:java/lang/Throwable  ]
+TRY 30:[38:java/lang/Throwable  , 49:java/lang/Throwable  ]
+TRY 34:[38:java/lang/Throwable  , 49:java/lang/Throwable  ]
+TRY 36:[38:java/lang/Throwable  , 49:java/lang/Throwable  ]
+TRY 40:[38:java/lang/Throwable  , 49:java/lang/Throwable  ]
+TRY 42:[49:java/lang/Throwable  ]
@@ -26,2 +28 @@
-IFNULL L13
-GOTO L32
+IFNONNULL L22
@@ -32,9 +32,0 @@
-GOTO L26
-ASTORE 6
-ALOAD 3
-ALOAD 0
-ALOAD 4
-ALOAD 6
-INVOKESTATIC local/lito381/server/NativeSourceManualScope.unlock(Ljava/util/concurrent/locks/ReentrantLock;Llocal/lito381/server/ClientSession;Llocal/lito381/server/NativePlayerActionScope$Ticket;Ljava/lang/Throwable;)V
-ALOAD 6
-ATHROW
@@ -48,2 +39,0 @@
-ASTORE 7
-ALOAD 7
@@ -51 +41 @@
-ASTORE 8
+ASTORE 6
@@ -62 +52 @@
-ALOAD 8
+ALOAD 6
@@ -64,3 +54,3 @@
-GOTO L55
-ASTORE 9
-ALOAD 8
+GOTO L43
+ASTORE 7
+ALOAD 6
@@ -68 +58 @@
-ALOAD 9
+ALOAD 7
@@ -74,0 +65,9 @@
+GOTO L57
+ASTORE 8
+ALOAD 3
+ALOAD 0
+ALOAD 4
+ALOAD 8
+INVOKESTATIC local/lito381/server/NativeSourceManualScope.unlock(Ljava/util/concurrent/locks/ReentrantLock;Llocal/lito381/server/ClientSession;Llocal/lito381/server/NativePlayerActionScope$Ticket;Ljava/lang/Throwable;)V
+ALOAD 8
+ATHROW
--- staged/NativePlayerCommit/calculateWorld-d02672c7.txt
+++ candidate/NativePlayerCommit/calculateWorld-d02672c7.txt
@@ -8,6 +8,6 @@
-TRY 11:[33:java/lang/Throwable  ]
-TRY 15:[33:java/lang/Throwable  ]
-TRY 16:[33:java/lang/Throwable  ]
-TRY 20:[33:java/lang/Throwable  ]
-TRY 21:[33:java/lang/Throwable  ]
-TRY 22:[33:java/lang/Throwable  ]
+TRY 11:[34:java/lang/Throwable  ]
+TRY 16:[34:java/lang/Throwable  ]
+TRY 17:[34:java/lang/Throwable  ]
+TRY 21:[34:java/lang/Throwable  ]
+TRY 22:[34:java/lang/Throwable  ]
+TRY 23:[34:java/lang/Throwable  ]
@@ -29 +29,2 @@
-INVOKESTATIC java/lang/Integer.valueOf(I)Ljava/lang/Integer;
+I2D
+INVOKESTATIC java/lang/Double.valueOf(D)Ljava/lang/Double;
@@ -39 +40 @@
-IFNONNULL L29
+IFNONNULL L30
@@ -42 +43 @@
-GOTO L44
+GOTO L45
@@ -46 +47 @@
-GOTO L44
+GOTO L45
@@ -49 +50 @@
-IFNONNULL L39
+IFNONNULL L40
@@ -52 +53 @@
-GOTO L42
+GOTO L43
--- staged/NativePlayerCommit/captureBattleDefense-5e1bbd14.txt
+++ candidate/NativePlayerCommit/captureBattleDefense-5e1bbd14.txt
@@ -83 +82,0 @@
-DUP_X1
@@ -85 +83,0 @@
-ISTORE 6
@@ -87 +85 @@
-IFLE L83
+IFLE L81
@@ -89 +87 @@
-IFEQ L83
+IFEQ L81
--- staged/NativePlayerCommit/captureWorldDefense-83654350.txt
+++ candidate/NativePlayerCommit/captureWorldDefense-83654350.txt
@@ -90 +89,0 @@
-DUP_X1
@@ -92 +90,0 @@
-ISTORE 11
@@ -96 +94 @@
-IFLE L99
+IFLE L97
@@ -98 +96 @@
-IFNULL L97
+IFNULL L95
@@ -102 +100 @@
-IF_ICMPEQ L97
+IF_ICMPEQ L95
@@ -105 +103 @@
-IFNE L99
+IFNE L97
--- staged/NativePlayerCommit/debitMp-4614b1d8.txt
+++ candidate/NativePlayerCommit/debitMp-4614b1d8.txt
@@ -8,19 +8,19 @@
-TRY 22:[87:java/lang/Throwable  ]
-TRY 23:[87:java/lang/Throwable  ]
-TRY 25:[71:java/lang/Throwable  , 87:java/lang/Throwable  ]
-TRY 29:[71:java/lang/Throwable  , 87:java/lang/Throwable  ]
-TRY 33:[71:java/lang/Throwable  , 87:java/lang/Throwable  ]
-TRY 37:[71:java/lang/Throwable  , 87:java/lang/Throwable  ]
-TRY 41:[71:java/lang/Throwable  , 87:java/lang/Throwable  ]
-TRY 42:[71:java/lang/Throwable  , 87:java/lang/Throwable  ]
-TRY 43:[71:java/lang/Throwable  , 87:java/lang/Throwable  ]
-TRY 47:[71:java/lang/Throwable  , 87:java/lang/Throwable  ]
-TRY 53:[71:java/lang/Throwable  , 87:java/lang/Throwable  ]
-TRY 60:[71:java/lang/Throwable  , 87:java/lang/Throwable  ]
-TRY 61:[71:java/lang/Throwable  , 87:java/lang/Throwable  ]
-TRY 66:[71:java/lang/Throwable  , 87:java/lang/Throwable  ]
-TRY 68:[87:java/lang/Throwable  ]
-TRY 69:[87:java/lang/Throwable  ]
-TRY 73:[87:java/lang/Throwable  ]
-TRY 74:[87:java/lang/Throwable  ]
-TRY 76:[87:java/lang/Throwable  ]
+TRY 22:[85:java/lang/Throwable  ]
+TRY 23:[85:java/lang/Throwable  ]
+TRY 25:[71:java/lang/Throwable  , 85:java/lang/Throwable  ]
+TRY 29:[71:java/lang/Throwable  , 85:java/lang/Throwable  ]
+TRY 33:[71:java/lang/Throwable  , 85:java/lang/Throwable  ]
+TRY 37:[71:java/lang/Throwable  , 85:java/lang/Throwable  ]
+TRY 41:[71:java/lang/Throwable  , 85:java/lang/Throwable  ]
+TRY 42:[71:java/lang/Throwable  , 85:java/lang/Throwable  ]
+TRY 43:[71:java/lang/Throwable  , 85:java/lang/Throwable  ]
+TRY 47:[71:java/lang/Throwable  , 85:java/lang/Throwable  ]
+TRY 53:[71:java/lang/Throwable  , 85:java/lang/Throwable  ]
+TRY 60:[71:java/lang/Throwable  , 85:java/lang/Throwable  ]
+TRY 61:[71:java/lang/Throwable  , 85:java/lang/Throwable  ]
+TRY 66:[71:java/lang/Throwable  , 85:java/lang/Throwable  ]
+TRY 68:[85:java/lang/Throwable  ]
+TRY 69:[85:java/lang/Throwable  ]
+TRY 73:[85:java/lang/Throwable  ]
+TRY 74:[85:java/lang/Throwable  ]
+TRY 76:[85:java/lang/Throwable  ]
@@ -106,2 +105,0 @@
-ILOAD 11
-ISTORE 12
@@ -112 +110 @@
-ILOAD 12
+ILOAD 11
@@ -114 +112 @@
-ASTORE 13
+ASTORE 12
@@ -119 +117 @@
-ALOAD 13
+ALOAD 12
--- staged/NativePlayerCommit/drain-bdc7c992.txt
+++ candidate/NativePlayerCommit/drain-bdc7c992.txt
@@ -8,69 +8,69 @@
-TRY 10:[258:java/lang/Throwable  ]
-TRY 19:[258:java/lang/Throwable  ]
-TRY 27:[258:java/lang/Throwable  ]
-TRY 41:[229:java/lang/Throwable  , 258:java/lang/Throwable  ]
-TRY 42:[229:java/lang/Throwable  , 258:java/lang/Throwable  ]
-TRY 47:[229:java/lang/Throwable  , 258:java/lang/Throwable  ]
-TRY 50:[229:java/lang/Throwable  , 258:java/lang/Throwable  ]
-TRY 55:[229:java/lang/Throwable  , 258:java/lang/Throwable  ]
-TRY 58:[229:java/lang/Throwable  , 258:java/lang/Throwable  ]
-TRY 61:[229:java/lang/Throwable  , 258:java/lang/Throwable  ]
-TRY 63:[229:java/lang/Throwable  , 258:java/lang/Throwable  ]
-TRY 67:[229:java/lang/Throwable  , 258:java/lang/Throwable  ]
-TRY 72:[229:java/lang/Throwable  , 258:java/lang/Throwable  ]
-TRY 74:[229:java/lang/Throwable  , 258:java/lang/Throwable  ]
-TRY 75:[229:java/lang/Throwable  , 258:java/lang/Throwable  ]
-TRY 77:[229:java/lang/Throwable  , 258:java/lang/Throwable  ]
-TRY 80:[229:java/lang/Throwable  , 258:java/lang/Throwable  ]
-TRY 81:[229:java/lang/Throwable  , 258:java/lang/Throwable  ]
-TRY 86:[208:java/lang/Throwable  , 229:java/lang/Throwable  , 258:java/lang/Throwable  ]
-TRY 91:[208:java/lang/Throwable  , 229:java/lang/Throwable  , 258:java/lang/Throwable  ]
-TRY 94:[208:java/lang/Throwable  , 229:java/lang/Throwable  , 258:java/lang/Throwable  ]
-TRY 97:[208:java/lang/Throwable  , 229:java/lang/Throwable  , 258:java/lang/Throwable  ]
-TRY 99:[208:java/lang/Throwable  , 229:java/lang/Throwable  , 258:java/lang/Throwable  ]
-TRY 103:[208:java/lang/Throwable  , 229:java/lang/Throwable  , 258:java/lang/Throwable  ]
-TRY 104:[208:java/lang/Throwable  , 229:java/lang/Throwable  , 258:java/lang/Throwable  ]
-TRY 106:[208:java/lang/Throwable  , 229:java/lang/Throwable  , 258:java/lang/Throwable  ]
-TRY 111:[208:java/lang/Throwable  , 229:java/lang/Throwable  , 258:java/lang/Throwable  ]
-TRY 117:[208:java/lang/Throwable  , 229:java/lang/Throwable  , 258:java/lang/Throwable  ]
-TRY 121:[208:java/lang/Throwable  , 229:java/lang/Throwable  , 258:java/lang/Throwable  ]
-TRY 125:[208:java/lang/Throwable  , 229:java/lang/Throwable  , 258:java/lang/Throwable  ]
-TRY 135:[208:java/lang/Throwable  , 229:java/lang/Throwable  , 258:java/lang/Throwable  ]
-TRY 136:[208:java/lang/Throwable  , 229:java/lang/Throwable  , 258:java/lang/Throwable  ]
-TRY 137:[208:java/lang/Throwable  , 229:java/lang/Throwable  , 258:java/lang/Throwable  ]
-TRY 140:[229:java/lang/Throwable  , 258:java/lang/Throwable  ]
-TRY 142:[229:java/lang/Throwable  , 258:java/lang/Throwable  ]
-TRY 149:[208:java/lang/Throwable  , 229:java/lang/Throwable  , 258:java/lang/Throwable  ]
-TRY 151:[208:java/lang/Throwable  , 229:java/lang/Throwable  , 258:java/lang/Throwable  ]
-TRY 153:[208:java/lang/Throwable  , 229:java/lang/Throwable  , 258:java/lang/Throwable  ]
-TRY 156:[208:java/lang/Throwable  , 229:java/lang/Throwable  , 258:java/lang/Throwable  ]
-TRY 165:[208:java/lang/Throwable  , 229:java/lang/Throwable  , 258:java/lang/Throwable  ]
-TRY 169:[208:java/lang/Throwable  , 229:java/lang/Throwable  , 258:java/lang/Throwable  ]
-TRY 171:[208:java/lang/Throwable  , 229:java/lang/Throwable  , 258:java/lang/Throwable  ]
-TRY 172:[208:java/lang/Throwable  , 229:java/lang/Throwable  , 258:java/lang/Throwable  ]
-TRY 176:[208:java/lang/Throwable  , 229:java/lang/Throwable  , 258:java/lang/Throwable  ]
-TRY 177:[208:java/lang/Throwable  , 229:java/lang/Throwable  , 258:java/lang/Throwable  ]
-TRY 184:[208:java/lang/Throwable  , 229:java/lang/Throwable  , 258:java/lang/Throwable  ]
-TRY 185:[208:java/lang/Throwable  , 229:java/lang/Throwable  , 258:java/lang/Throwable  ]
-TRY 186:[208:java/lang/Throwable  , 229:java/lang/Throwable  , 258:java/lang/Throwable  ]
-TRY 196:[208:java/lang/Throwable  , 229:java/lang/Throwable  , 258:java/lang/Throwable  ]
-TRY 199:[208:java/lang/Throwable  , 229:java/lang/Throwable  , 258:java/lang/Throwable  ]
-TRY 201:[208:java/lang/Throwable  , 229:java/lang/Throwable  , 258:java/lang/Throwable  ]
-TRY 204:[229:java/lang/Throwable  , 258:java/lang/Throwable  ]
-TRY 206:[229:java/lang/Throwable  , 258:java/lang/Throwable  ]
-TRY 210:[229:java/lang/Throwable  , 258:java/lang/Throwable  ]
-TRY 212:[229:java/lang/Throwable  , 258:java/lang/Throwable  ]
-TRY 214:[229:java/lang/Throwable  , 258:java/lang/Throwable  ]
-TRY 216:[229:java/lang/Throwable  , 258:java/lang/Throwable  ]
-TRY 218:[229:java/lang/Throwable  , 258:java/lang/Throwable  ]
-TRY 231:[258:java/lang/Throwable  ]
-TRY 233:[258:java/lang/Throwable  ]
-TRY 237:[258:java/lang/Throwable  ]
-TRY 238:[258:java/lang/Throwable  ]
-TRY 240:[258:java/lang/Throwable  ]
-TRY 241:[258:java/lang/Throwable  ]
-TRY 243:[258:java/lang/Throwable  ]
-TRY 245:[258:java/lang/Throwable  ]
-TRY 246:[258:java/lang/Throwable  ]
-TRY 247:[258:java/lang/Throwable  ]
-TRY 249:[258:java/lang/Throwable  ]
+TRY 10:[248:java/lang/Throwable  ]
+TRY 19:[248:java/lang/Throwable  ]
+TRY 22:[248:java/lang/Throwable  ]
+TRY 31:[218:java/lang/Throwable  , 248:java/lang/Throwable  ]
+TRY 32:[218:java/lang/Throwable  , 248:java/lang/Throwable  ]
+TRY 37:[218:java/lang/Throwable  , 248:java/lang/Throwable  ]
+TRY 40:[218:java/lang/Throwable  , 248:java/lang/Throwable  ]
+TRY 45:[218:java/lang/Throwable  , 248:java/lang/Throwable  ]
+TRY 48:[218:java/lang/Throwable  , 248:java/lang/Throwable  ]
+TRY 51:[218:java/lang/Throwable  , 248:java/lang/Throwable  ]
+TRY 53:[218:java/lang/Throwable  , 248:java/lang/Throwable  ]
+TRY 57:[218:java/lang/Throwable  , 248:java/lang/Throwable  ]
+TRY 62:[218:java/lang/Throwable  , 248:java/lang/Throwable  ]
+TRY 64:[218:java/lang/Throwable  , 248:java/lang/Throwable  ]
+TRY 65:[218:java/lang/Throwable  , 248:java/lang/Throwable  ]
+TRY 67:[218:java/lang/Throwable  , 248:java/lang/Throwable  ]
+TRY 70:[218:java/lang/Throwable  , 248:java/lang/Throwable  ]
+TRY 71:[218:java/lang/Throwable  , 248:java/lang/Throwable  ]
+TRY 76:[197:java/lang/Throwable  , 218:java/lang/Throwable  , 248:java/lang/Throwable  ]
+TRY 81:[197:java/lang/Throwable  , 218:java/lang/Throwable  , 248:java/lang/Throwable  ]
+TRY 84:[197:java/lang/Throwable  , 218:java/lang/Throwable  , 248:java/lang/Throwable  ]
+TRY 87:[197:java/lang/Throwable  , 218:java/lang/Throwable  , 248:java/lang/Throwable  ]
+TRY 89:[197:java/lang/Throwable  , 218:java/lang/Throwable  , 248:java/lang/Throwable  ]
+TRY 93:[197:java/lang/Throwable  , 218:java/lang/Throwable  , 248:java/lang/Throwable  ]
+TRY 94:[197:java/lang/Throwable  , 218:java/lang/Throwable  , 248:java/lang/Throwable  ]
+TRY 96:[197:java/lang/Throwable  , 218:java/lang/Throwable  , 248:java/lang/Throwable  ]
+TRY 101:[197:java/lang/Throwable  , 218:java/lang/Throwable  , 248:java/lang/Throwable  ]
+TRY 104:[197:java/lang/Throwable  , 218:java/lang/Throwable  , 248:java/lang/Throwable  ]
+TRY 105:[197:java/lang/Throwable  , 218:java/lang/Throwable  , 248:java/lang/Throwable  ]
+TRY 106:[197:java/lang/Throwable  , 218:java/lang/Throwable  , 248:java/lang/Throwable  ]
+TRY 109:[218:java/lang/Throwable  , 248:java/lang/Throwable  ]
+TRY 111:[218:java/lang/Throwable  , 248:java/lang/Throwable  ]
+TRY 117:[197:java/lang/Throwable  , 218:java/lang/Throwable  , 248:java/lang/Throwable  ]
+TRY 121:[197:java/lang/Throwable  , 218:java/lang/Throwable  , 248:java/lang/Throwable  ]
+TRY 125:[197:java/lang/Throwable  , 218:java/lang/Throwable  , 248:java/lang/Throwable  ]
+TRY 138:[197:java/lang/Throwable  , 218:java/lang/Throwable  , 248:java/lang/Throwable  ]
+TRY 140:[197:java/lang/Throwable  , 218:java/lang/Throwable  , 248:java/lang/Throwable  ]
+TRY 142:[197:java/lang/Throwable  , 218:java/lang/Throwable  , 248:java/lang/Throwable  ]
+TRY 145:[197:java/lang/Throwable  , 218:java/lang/Throwable  , 248:java/lang/Throwable  ]
+TRY 154:[197:java/lang/Throwable  , 218:java/lang/Throwable  , 248:java/lang/Throwable  ]
+TRY 158:[197:java/lang/Throwable  , 218:java/lang/Throwable  , 248:java/lang/Throwable  ]
+TRY 160:[197:java/lang/Throwable  , 218:java/lang/Throwable  , 248:java/lang/Throwable  ]
+TRY 161:[197:java/lang/Throwable  , 218:java/lang/Throwable  , 248:java/lang/Throwable  ]
+TRY 165:[197:java/lang/Throwable  , 218:java/lang/Throwable  , 248:java/lang/Throwable  ]
+TRY 166:[197:java/lang/Throwable  , 218:java/lang/Throwable  , 248:java/lang/Throwable  ]
+TRY 173:[197:java/lang/Throwable  , 218:java/lang/Throwable  , 248:java/lang/Throwable  ]
+TRY 174:[197:java/lang/Throwable  , 218:java/lang/Throwable  , 248:java/lang/Throwable  ]
+TRY 175:[197:java/lang/Throwable  , 218:java/lang/Throwable  , 248:java/lang/Throwable  ]
+TRY 185:[197:java/lang/Throwable  , 218:java/lang/Throwable  , 248:java/lang/Throwable  ]
+TRY 188:[197:java/lang/Throwable  , 218:java/lang/Throwable  , 248:java/lang/Throwable  ]
+TRY 190:[197:java/lang/Throwable  , 218:java/lang/Throwable  , 248:java/lang/Throwable  ]
+TRY 193:[218:java/lang/Throwable  , 248:java/lang/Throwable  ]
+TRY 195:[218:java/lang/Throwable  , 248:java/lang/Throwable  ]
+TRY 199:[218:java/lang/Throwable  , 248:java/lang/Throwable  ]
+TRY 201:[218:java/lang/Throwable  , 248:java/lang/Throwable  ]
+TRY 203:[218:java/lang/Throwable  , 248:java/lang/Throwable  ]
+TRY 205:[218:java/lang/Throwable  , 248:java/lang/Throwable  ]
+TRY 207:[218:java/lang/Throwable  , 248:java/lang/Throwable  ]
+TRY 220:[248:java/lang/Throwable  ]
+TRY 222:[248:java/lang/Throwable  ]
+TRY 226:[248:java/lang/Throwable  ]
+TRY 227:[248:java/lang/Throwable  ]
+TRY 230:[248:java/lang/Throwable  ]
+TRY 231:[248:java/lang/Throwable  ]
+TRY 233:[248:java/lang/Throwable  ]
+TRY 235:[248:java/lang/Throwable  ]
+TRY 236:[248:java/lang/Throwable  ]
+TRY 237:[248:java/lang/Throwable  ]
+TRY 239:[248:java/lang/Throwable  ]
@@ -85 +85 @@
-IF_ICMPGE L252
+IF_ICMPGE L242
@@ -92 +92 @@
-IFLE L26
+IFLE L21
@@ -97,6 +97 @@
-ILOAD 3
-IFNE L25
-ALOAD 0
-ALOAD 1
-INVOKESTATIC local/lito381/server/NativePlayerCommit.resumeAccepted(Llocal/lito381/server/ClientSession;Llocal/lito381/server/NativePlayerCommit$Queue;)V
-RETURN
+GOTO L242
@@ -107,7 +102,2 @@
-IFNONNULL L37
-ILOAD 3
-IFNE L36
-ALOAD 0
-ALOAD 1
-INVOKESTATIC local/lito381/server/NativePlayerCommit.resumeAccepted(Llocal/lito381/server/ClientSession;Llocal/lito381/server/NativePlayerCommit$Queue;)V
-RETURN
+IFNONNULL L27
+GOTO L242
@@ -130 +120 @@
-IFEQ L77
+IFEQ L67
@@ -133 +123 @@
-IFNE L77
+IFNE L67
@@ -136 +126 @@
-IFNE L77
+IFNE L67
@@ -141 +131 @@
-IFEQ L77
+IFEQ L67
@@ -145 +135 @@
-IFLE L77
+IFLE L67
@@ -153 +143 @@
-GOTO L78
+GOTO L68
@@ -166 +156 @@
-IFNULL L134
+IFNULL L103
@@ -169 +159 @@
-IFNE L134
+IFNE L103
@@ -172 +162 @@
-IFNE L134
+IFNE L103
@@ -177 +167 @@
-IFEQ L134
+IFEQ L103
@@ -184 +174 @@
-IFEQ L110
+IFEQ L100
@@ -186 +176 @@
-GOTO L111
+GOTO L101
@@ -189,22 +179 @@
-IFEQ L134
-ALOAD 0
-ALOAD 7
-ALOAD 9
-ILOAD 10
-INVOKESTATIC local/lito381/server/NativeSourceGroundQueued.beforeEvent(Llocal/lito381/server/ClientSession;Llocal/lito381/server/NativePlayerCommit$Event;Llocal/lito381/server/ClientSession;Z)Z
-IFEQ L134
-ALOAD 0
-CONST str:"playerMaxHp"
-INVOKESTATIC local/lito381/server/NativePlayerCommit.integer(Ljava/lang/Object;Ljava/lang/String;)I
-ISTORE 14
-ALOAD 0
-CONST str:"playerHp"
-INVOKESTATIC local/lito381/server/NativePlayerCommit.integer(Ljava/lang/Object;Ljava/lang/String;)I
-ISTORE 15
-ILOAD 15
-IFLE L131
-CONST Integer:1
-GOTO L132
-CONST Integer:0
-ISTORE 8
-GOTO L144
+IFNE L113
@@ -220 +189,21 @@
-GOTO L250
+GOTO L240
+ALOAD 0
+ALOAD 7
+ALOAD 9
+ILOAD 10
+INVOKESTATIC local/lito381/server/NativeSourceGroundQueued.beforeEvent(Llocal/lito381/server/ClientSession;Llocal/lito381/server/NativePlayerCommit$Event;Llocal/lito381/server/ClientSession;Z)Z
+IFEQ L103
+ALOAD 0
+CONST str:"playerMaxHp"
+INVOKESTATIC local/lito381/server/NativePlayerCommit.integer(Ljava/lang/Object;Ljava/lang/String;)I
+ISTORE 14
+ALOAD 0
+CONST str:"playerHp"
+INVOKESTATIC local/lito381/server/NativePlayerCommit.integer(Ljava/lang/Object;Ljava/lang/String;)I
+ISTORE 15
+ILOAD 15
+IFLE L131
+CONST Integer:1
+GOTO L132
+CONST Integer:0
+ISTORE 8
@@ -222 +211 @@
-IFEQ L159
+IFEQ L148
@@ -235 +224 @@
-GOTO L174
+GOTO L163
@@ -237,3 +226,3 @@
-IFEQ L163
-ILOAD 15
-GOTO L164
+IFEQ L152
+ILOAD 15
+GOTO L153
@@ -257 +246 @@
-IF_ICMPEQ L186
+IF_ICMPEQ L175
@@ -284 +273 @@
-GOTO L215
+GOTO L204
@@ -296 +285 @@
-IFEQ L228
+IFEQ L217
@@ -300 +289 @@
-IFNE L227
+IFNE L216
@@ -305 +294 @@
-GOTO L250
+GOTO L240
@@ -311 +300 @@
-IFEQ L240
+IFEQ L230
@@ -313 +302 @@
-IFEQ L240
+IFEQ L229
@@ -316,0 +306 @@
+GOTO L240
@@ -330 +320 @@
-IFNE L257
+IFNE L256
@@ -334 +324 @@
-RETURN
+GOTO L256
@@ -337 +327 @@
-IFNE L264
+IFNE L254
@@ -342,0 +333 @@
+RETURN
--- staged/NativePlayerCommit/drawDefense-b49748c1.txt
+++ candidate/NativePlayerCommit/drawDefense-b49748c1.txt
@@ -8,20 +8,20 @@
-TRY 21:[27:java/lang/Throwable  ]
-TRY 22:[27:java/lang/Throwable  ]
-TRY 25:[27:java/lang/Throwable  ]
-TRY 29:[27:java/lang/Throwable  ]
-TRY 55:[101:java/lang/RuntimeException  ]
-TRY 57:[101:java/lang/RuntimeException  ]
-TRY 60:[101:java/lang/RuntimeException  ]
-TRY 62:[101:java/lang/RuntimeException  ]
-TRY 65:[101:java/lang/RuntimeException  ]
-TRY 69:[101:java/lang/RuntimeException  ]
-TRY 72:[101:java/lang/RuntimeException  ]
-TRY 73:[101:java/lang/RuntimeException  ]
-TRY 75:[101:java/lang/RuntimeException  ]
-TRY 77:[101:java/lang/RuntimeException  ]
-TRY 80:[101:java/lang/RuntimeException  ]
-TRY 81:[101:java/lang/RuntimeException  ]
-TRY 89:[101:java/lang/RuntimeException  ]
-TRY 90:[101:java/lang/RuntimeException  ]
-TRY 93:[101:java/lang/RuntimeException  ]
-TRY 98:[101:java/lang/RuntimeException  ]
+TRY 19:[25:java/lang/Throwable  ]
+TRY 20:[25:java/lang/Throwable  ]
+TRY 23:[25:java/lang/Throwable  ]
+TRY 27:[25:java/lang/Throwable  ]
+TRY 54:[100:java/lang/RuntimeException  ]
+TRY 56:[100:java/lang/RuntimeException  ]
+TRY 59:[100:java/lang/RuntimeException  ]
+TRY 61:[100:java/lang/RuntimeException  ]
+TRY 64:[100:java/lang/RuntimeException  ]
+TRY 68:[100:java/lang/RuntimeException  ]
+TRY 71:[100:java/lang/RuntimeException  ]
+TRY 72:[100:java/lang/RuntimeException  ]
+TRY 74:[100:java/lang/RuntimeException  ]
+TRY 76:[100:java/lang/RuntimeException  ]
+TRY 79:[100:java/lang/RuntimeException  ]
+TRY 80:[100:java/lang/RuntimeException  ]
+TRY 88:[100:java/lang/RuntimeException  ]
+TRY 89:[100:java/lang/RuntimeException  ]
+TRY 92:[100:java/lang/RuntimeException  ]
+TRY 97:[100:java/lang/RuntimeException  ]
@@ -41,0 +42 @@
+DUP
@@ -43,3 +43,0 @@
-ALOAD 3
-DUP
-ASTORE 4
@@ -50,0 +49,4 @@
+ASTORE 4
+ALOAD 3
+MONITOREXIT
+GOTO L30
@@ -52 +54 @@
-ALOAD 4
+ALOAD 3
@@ -54,5 +56 @@
-GOTO L32
-ASTORE 6
-ALOAD 4
-MONITOREXIT
-ALOAD 6
+ALOAD 5
@@ -61 +59 @@
-ISTORE 7
+ISTORE 6
@@ -65,4 +63,4 @@
-ASTORE 8
-ALOAD 5
-IFNULL L105
-ALOAD 5
+ASTORE 7
+ALOAD 4
+IFNULL L104
+ALOAD 4
@@ -71,0 +70,7 @@
+ASTORE 8
+ALOAD 8
+INVOKEINTERFACE java/util/Iterator.hasNext()Z itf
+IFEQ L104
+ALOAD 8
+INVOKEINTERFACE java/util/Iterator.next()Ljava/lang/Object; itf
+CHECKCAST local/lito381/server/SourceMagicDollPower
@@ -74,2 +79,2 @@
-INVOKEINTERFACE java/util/Iterator.hasNext()Z itf
-IFEQ L105
+IFNONNULL L53
+GOTO L43
@@ -77,6 +81,0 @@
-INVOKEINTERFACE java/util/Iterator.next()Ljava/lang/Object; itf
-CHECKCAST local/lito381/server/SourceMagicDollPower
-ASTORE 10
-ALOAD 10
-IFNULL L104
-ALOAD 10
@@ -86,2 +85,2 @@
-IFNE L68
-ALOAD 10
+IFNE L67
+ALOAD 9
@@ -91,2 +90,2 @@
-IFEQ L71
-ALOAD 10
+IFEQ L70
+ALOAD 9
@@ -95,2 +94,2 @@
-IF_ICMPNE L71
-ALOAD 10
+IF_ICMPNE L70
+ALOAD 9
@@ -98,2 +97,2 @@
-IFGT L72
-GOTO L45
+IFGT L71
+GOTO L43
@@ -107 +106 @@
-ALOAD 10
+ALOAD 9
@@ -110,2 +109,2 @@
-IF_ICMPLT L84
-GOTO L45
+IF_ICMPLT L83
+GOTO L43
@@ -113 +112 @@
-ILOAD 7
+ILOAD 6
@@ -116 +115 @@
-ALOAD 10
+ALOAD 9
@@ -123,3 +122,3 @@
-ISTORE 7
-ALOAD 8
-ALOAD 10
+ISTORE 6
+ALOAD 7
+ALOAD 9
@@ -128,3 +127,3 @@
-GOTO L104
-ASTORE 11
-ALOAD 11
+GOTO L103
+ASTORE 10
+ALOAD 10
@@ -132 +131 @@
-GOTO L45
+GOTO L43
@@ -135,2 +134,2 @@
-ILOAD 7
-ALOAD 8
+ILOAD 6
+ALOAD 7
@@ -137,0 +137,5 @@
+ASTORE 11
+ALOAD 4
+IFNULL L140
+ALOAD 7
+INVOKEVIRTUAL java/util/ArrayList.iterator()Ljava/util/Iterator;
@@ -139,6 +143 @@
-ALOAD 5
-IFNULL L141
-ALOAD 8
-INVOKEVIRTUAL java/util/ArrayList.iterator()Ljava/util/Iterator;
-ASTORE 13
-ALOAD 13
+ALOAD 12
@@ -146,2 +145,2 @@
-IFEQ L141
-ALOAD 13
+IFEQ L140
+ALOAD 12
@@ -150 +149 @@
-ASTORE 14
+ASTORE 13
@@ -155,2 +154,2 @@
-ALOAD 5
-ALOAD 14
+ALOAD 4
+ALOAD 13
@@ -168,2 +167,2 @@
-GOTO L116
-ALOAD 12
+GOTO L115
+ALOAD 11
--- staged/NativePlayerCommit/earlyZero-34579d8b.txt
+++ candidate/NativePlayerCommit/earlyZero-34579d8b.txt
@@ -8,9 +8,9 @@
-TRY 33:[101:java/lang/Throwable  ]
-TRY 36:[101:java/lang/Throwable  ]
-TRY 39:[101:java/lang/Throwable  ]
-TRY 41:[101:java/lang/Throwable  ]
-TRY 53:[101:java/lang/Throwable  ]
-TRY 70:[101:java/lang/Throwable  ]
-TRY 71:[101:java/lang/Throwable  ]
-TRY 72:[101:java/lang/Throwable  ]
-TRY 76:[101:java/lang/Throwable  ]
+TRY 33:[93:java/lang/Throwable  ]
+TRY 36:[93:java/lang/Throwable  ]
+TRY 39:[93:java/lang/Throwable  ]
+TRY 41:[93:java/lang/Throwable  ]
+TRY 51:[93:java/lang/Throwable  ]
+TRY 66:[93:java/lang/Throwable  ]
+TRY 67:[93:java/lang/Throwable  ]
+TRY 68:[93:java/lang/Throwable  ]
+TRY 72:[93:java/lang/Throwable  ]
@@ -59 +59 @@
-IFNE L52
+IFNE L50
@@ -61,0 +62,3 @@
+ALOAD 6
+GETFIELD local/lito381/server/NativePlayerCommit$Queue.vitalLock:Ljava/util/concurrent/locks/ReentrantLock;
+INVOKEVIRTUAL java/util/concurrent/locks/ReentrantLock.unlock()V
@@ -62,0 +66,5 @@
+IRETURN
+ALOAD 6
+GETFIELD local/lito381/server/NativePlayerCommit$Queue.terminal:Z
+IFEQ L60
+CONST Integer:1
@@ -69,14 +77,2 @@
-ALOAD 6
-GETFIELD local/lito381/server/NativePlayerCommit$Queue.terminal:Z
-IFEQ L64
-CONST Integer:1
-ISTORE 10
-ILOAD 10
-ISTORE 11
-ALOAD 6
-GETFIELD local/lito381/server/NativePlayerCommit$Queue.vitalLock:Ljava/util/concurrent/locks/ReentrantLock;
-INVOKEVIRTUAL java/util/concurrent/locks/ReentrantLock.unlock()V
-ILOAD 11
-IRETURN
-ILOAD 2
-IFNE L86
+ILOAD 2
+IFNE L80
@@ -84 +80 @@
-IFGT L86
+IFGT L80
@@ -90 +86 @@
-IFEQ L86
+IFEQ L80
@@ -95,7 +91,5 @@
-ISTORE 12
-ILOAD 12
-ISTORE 13
-ALOAD 6
-GETFIELD local/lito381/server/NativePlayerCommit$Queue.vitalLock:Ljava/util/concurrent/locks/ReentrantLock;
-INVOKEVIRTUAL java/util/concurrent/locks/ReentrantLock.unlock()V
-ILOAD 13
+ISTORE 10
+ALOAD 6
+GETFIELD local/lito381/server/NativePlayerCommit$Queue.vitalLock:Ljava/util/concurrent/locks/ReentrantLock;
+INVOKEVIRTUAL java/util/concurrent/locks/ReentrantLock.unlock()V
+ILOAD 10
@@ -104,19 +98,17 @@
-IFLE L97
-CONST Integer:0
-ISTORE 14
-ILOAD 14
-ISTORE 15
-ALOAD 6
-GETFIELD local/lito381/server/NativePlayerCommit$Queue.vitalLock:Ljava/util/concurrent/locks/ReentrantLock;
-INVOKEVIRTUAL java/util/concurrent/locks/ReentrantLock.unlock()V
-ILOAD 15
-IRETURN
-ALOAD 6
-GETFIELD local/lito381/server/NativePlayerCommit$Queue.vitalLock:Ljava/util/concurrent/locks/ReentrantLock;
-INVOKEVIRTUAL java/util/concurrent/locks/ReentrantLock.unlock()V
-GOTO L107
-ASTORE 16
-ALOAD 6
-GETFIELD local/lito381/server/NativePlayerCommit$Queue.vitalLock:Ljava/util/concurrent/locks/ReentrantLock;
-INVOKEVIRTUAL java/util/concurrent/locks/ReentrantLock.unlock()V
-ALOAD 16
+IFLE L89
+CONST Integer:0
+ISTORE 11
+ALOAD 6
+GETFIELD local/lito381/server/NativePlayerCommit$Queue.vitalLock:Ljava/util/concurrent/locks/ReentrantLock;
+INVOKEVIRTUAL java/util/concurrent/locks/ReentrantLock.unlock()V
+ILOAD 11
+IRETURN
+ALOAD 6
+GETFIELD local/lito381/server/NativePlayerCommit$Queue.vitalLock:Ljava/util/concurrent/locks/ReentrantLock;
+INVOKEVIRTUAL java/util/concurrent/locks/ReentrantLock.unlock()V
+GOTO L99
+ASTORE 12
+ALOAD 6
+GETFIELD local/lito381/server/NativePlayerCommit$Queue.vitalLock:Ljava/util/concurrent/locks/ReentrantLock;
+INVOKEVIRTUAL java/util/concurrent/locks/ReentrantLock.unlock()V
+ALOAD 12
@@ -126,3 +118,3 @@
-ASTORE 17
-ILOAD 2
-IFEQ L116
+ASTORE 13
+ILOAD 2
+IFEQ L108
@@ -132,3 +124,28 @@
-GOTO L117
-CONST null
-DUP
+GOTO L109
+CONST null
+ASTORE 14
+LLOAD 3/4
+CONST Long:0
+LCMP
+IFEQ L116
+LLOAD 3/4
+GOTO L129
+ALOAD 13
+IFNULL L121
+ALOAD 13
+GETFIELD local/lito381/server/NativePlayerCommit$Binding.object:J
+GOTO L129
+ALOAD 5
+GETFIELD local/lito381/server/NativePlayerCommit$CalcTicket.attacker:Llocal/lito381/server/NativeCombatAttacker$Ref;
+IFNONNULL L126
+CONST Long:0
+GOTO L129
+ALOAD 5
+GETFIELD local/lito381/server/NativePlayerCommit$CalcTicket.attacker:Llocal/lito381/server/NativeCombatAttacker$Ref;
+INVOKEVIRTUAL local/lito381/server/NativeCombatAttacker$Ref.id()J
+LSTORE 15/16
+CONST null
+ASTORE 17
+ILOAD 2
+IFEQ L178
+INVOKESTATIC local/lito381/server/NativeCombatOrigin.poison()Llocal/lito381/server/NativeCombatOrigin$Poison;
@@ -136,2 +153,26 @@
-ASTORE 19
-LLOAD 3/4
+ALOAD 18
+IFNULL L141
+ALOAD 18
+INVOKEVIRTUAL local/lito381/server/NativeCombatOrigin$Poison.snapshot()Llocal/lito381/server/ClassicWorldMonsterPopulation$Snapshot;
+GOTO L143
+LLOAD 15/16
+INVOKESTATIC local/lito381/server/NativeCombatAttacker.capturedSnapshot(J)Llocal/lito381/server/ClassicWorldMonsterPopulation$Snapshot;
+ASTORE 17
+ALOAD 17
+IFNONNULL L159
+ALOAD 5
+GETFIELD local/lito381/server/NativePlayerCommit$CalcTicket.attacker:Llocal/lito381/server/NativeCombatAttacker$Ref;
+IFNULL L159
+ALOAD 5
+GETFIELD local/lito381/server/NativePlayerCommit$CalcTicket.attacker:Llocal/lito381/server/NativeCombatAttacker$Ref;
+INVOKEVIRTUAL local/lito381/server/NativeCombatAttacker$Ref.kind()I
+CONST Integer:1
+IF_ICMPNE L159
+ALOAD 0
+ALOAD 5
+GETFIELD local/lito381/server/NativePlayerCommit$CalcTicket.attacker:Llocal/lito381/server/NativeCombatAttacker$Ref;
+INVOKESTATIC local/lito381/server/NativeCombatAttacker.worldSnapshot(Llocal/lito381/server/ClientSession;Llocal/lito381/server/NativeCombatAttacker$Ref;)Llocal/lito381/server/ClassicWorldMonsterPopulation$Snapshot;
+ASTORE 17
+ALOAD 17
+IFNONNULL L178
+LLOAD 15/16
@@ -140,54 +181 @@
-IFEQ L126
-LLOAD 3/4
-GOTO L139
-ALOAD 17
-IFNULL L131
-ALOAD 17
-GETFIELD local/lito381/server/NativePlayerCommit$Binding.object:J
-GOTO L139
-ALOAD 5
-GETFIELD local/lito381/server/NativePlayerCommit$CalcTicket.attacker:Llocal/lito381/server/NativeCombatAttacker$Ref;
-IFNONNULL L136
-CONST Long:0
-GOTO L139
-ALOAD 5
-GETFIELD local/lito381/server/NativePlayerCommit$CalcTicket.attacker:Llocal/lito381/server/NativeCombatAttacker$Ref;
-INVOKEVIRTUAL local/lito381/server/NativeCombatAttacker$Ref.id()J
-LSTORE 20/21
-CONST null
-ASTORE 22
-ILOAD 2
-IFEQ L190
-INVOKESTATIC local/lito381/server/NativeCombatOrigin.poison()Llocal/lito381/server/NativeCombatOrigin$Poison;
-ASTORE 23
-ALOAD 23
-IFNULL L151
-ALOAD 23
-INVOKEVIRTUAL local/lito381/server/NativeCombatOrigin$Poison.snapshot()Llocal/lito381/server/ClassicWorldMonsterPopulation$Snapshot;
-GOTO L153
-LLOAD 20/21
-INVOKESTATIC local/lito381/server/NativeCombatAttacker.capturedSnapshot(J)Llocal/lito381/server/ClassicWorldMonsterPopulation$Snapshot;
-DUP
-ASTORE 22
-ASTORE 24
-ALOAD 22
-IFNONNULL L171
-ALOAD 5
-GETFIELD local/lito381/server/NativePlayerCommit$CalcTicket.attacker:Llocal/lito381/server/NativeCombatAttacker$Ref;
-IFNULL L171
-ALOAD 5
-GETFIELD local/lito381/server/NativePlayerCommit$CalcTicket.attacker:Llocal/lito381/server/NativeCombatAttacker$Ref;
-INVOKEVIRTUAL local/lito381/server/NativeCombatAttacker$Ref.kind()I
-CONST Integer:1
-IF_ICMPNE L171
-ALOAD 0
-ALOAD 5
-GETFIELD local/lito381/server/NativePlayerCommit$CalcTicket.attacker:Llocal/lito381/server/NativeCombatAttacker$Ref;
-INVOKESTATIC local/lito381/server/NativeCombatAttacker.worldSnapshot(Llocal/lito381/server/ClientSession;Llocal/lito381/server/NativeCombatAttacker$Ref;)Llocal/lito381/server/ClassicWorldMonsterPopulation$Snapshot;
-ASTORE 22
-ALOAD 22
-IFNONNULL L190
-LLOAD 20/21
-CONST Long:0
-LCMP
-IFEQ L190
+IFEQ L178
@@ -204 +192 @@
-LLOAD 20/21
+LLOAD 15/16
@@ -206 +194 @@
-ASTORE 22
+ASTORE 17
@@ -211,0 +200,9 @@
+ALOAD 13
+ILOAD 7
+CONST Integer:0
+CONST Integer:1
+CONST Long:0
+CONST null
+CONST Integer:0
+ILOAD 2
+LLOAD 15/16
@@ -213,10 +210 @@
-ILOAD 7
-CONST Integer:0
-CONST Integer:1
-CONST Long:0
-CONST null
-CONST Integer:0
-ILOAD 2
-LLOAD 20/21
-ALOAD 22
-ALOAD 18
+ALOAD 14
--- staged/NativePlayerCommit/mutateOwned-6c792ff4.txt
+++ candidate/NativePlayerCommit/mutateOwned-6c792ff4.txt
@@ -8,4 +8,4 @@
-TRY 26:[50:java/lang/Throwable  ]
-TRY 30:[50:java/lang/Throwable  ]
-TRY 34:[50:java/lang/Throwable  ]
-TRY 37:[50:java/lang/Throwable  ]
+TRY 26:[48:java/lang/Throwable  ]
+TRY 30:[48:java/lang/Throwable  ]
+TRY 34:[48:java/lang/Throwable  ]
+TRY 37:[48:java/lang/Throwable  ]
@@ -55,2 +54,0 @@
-ILOAD 5
-ISTORE 6
@@ -60 +58 @@
-ILOAD 6
+ILOAD 5
@@ -62 +60 @@
-ASTORE 7
+ASTORE 6
@@ -66 +64 @@
-ALOAD 7
+ALOAD 6
--- staged/NativePlayerCommit/nativeCompanionSceneAssign-9c9f0c0c.txt
+++ candidate/NativePlayerCommit/nativeCompanionSceneAssign-9c9f0c0c.txt
@@ -8,7 +8,9 @@
-TRY 8:[18:java/lang/Throwable  ]
-TRY 16:[18:java/lang/Throwable  ]
-TRY 41:[52:java/lang/Throwable  ]
-TRY 44:[52:java/lang/Throwable  ]
-TRY 48:[52:java/lang/Throwable  ]
-TRY 50:[52:java/lang/Throwable  ]
-TRY 54:[52:java/lang/Throwable  ]
+TRY 8:[51:java/lang/Throwable  ]
+TRY 15:[51:java/lang/Throwable  ]
+TRY 25:[51:java/lang/Throwable  ]
+TRY 29:[40:java/lang/Throwable  , 51:java/lang/Throwable  ]
+TRY 32:[40:java/lang/Throwable  , 51:java/lang/Throwable  ]
+TRY 36:[40:java/lang/Throwable  , 51:java/lang/Throwable  ]
+TRY 38:[40:java/lang/Throwable  , 51:java/lang/Throwable  ]
+TRY 42:[40:java/lang/Throwable  , 51:java/lang/Throwable  ]
+TRY 44:[51:java/lang/Throwable  ]
@@ -26,2 +28 @@
-IFNULL L13
-GOTO L32
+IFNONNULL L22
@@ -32,9 +32,0 @@
-GOTO L26
-ASTORE 6
-ALOAD 3
-ALOAD 0
-ALOAD 4
-ALOAD 6
-INVOKESTATIC local/lito381/server/NativeSourceManualScope.unlock(Ljava/util/concurrent/locks/ReentrantLock;Llocal/lito381/server/ClientSession;Llocal/lito381/server/NativePlayerActionScope$Ticket;Ljava/lang/Throwable;)V
-ALOAD 6
-ATHROW
@@ -48,2 +39,0 @@
-ASTORE 7
-ALOAD 7
@@ -51 +41 @@
-ASTORE 8
+ASTORE 6
@@ -64 +54 @@
-ALOAD 8
+ALOAD 6
@@ -66,3 +56,3 @@
-GOTO L57
-ASTORE 9
-ALOAD 8
+GOTO L45
+ASTORE 7
+ALOAD 6
@@ -70 +60 @@
-ALOAD 9
+ALOAD 7
@@ -76,0 +67,9 @@
+GOTO L59
+ASTORE 8
+ALOAD 3
+ALOAD 0
+ALOAD 4
+ALOAD 8
+INVOKESTATIC local/lito381/server/NativeSourceManualScope.unlock(Ljava/util/concurrent/locks/ReentrantLock;Llocal/lito381/server/ClientSession;Llocal/lito381/server/NativePlayerActionScope$Ticket;Ljava/lang/Throwable;)V
+ALOAD 8
+ATHROW
--- staged/NativePlayerCommit/nativeCompanionSceneOriginalAdvance-bbb34ec2.txt
+++ candidate/NativePlayerCommit/nativeCompanionSceneOriginalAdvance-bbb34ec2.txt
@@ -8,4 +8,21 @@
-TRY 8:[14:java/lang/Throwable  ]
-TRY 57:[70:java/lang/Throwable  ]
-TRY 61:[70:java/lang/Throwable  ]
-TRY 65:[70:java/lang/Throwable  ]
+TRY 8:[88:java/lang/Throwable  ]
+TRY 21:[88:java/lang/Throwable  ]
+TRY 23:[77:java/lang/Throwable  , 88:java/lang/Throwable  ]
+TRY 26:[77:java/lang/Throwable  , 88:java/lang/Throwable  ]
+TRY 31:[77:java/lang/Throwable  , 88:java/lang/Throwable  ]
+TRY 40:[77:java/lang/Throwable  , 88:java/lang/Throwable  ]
+TRY 41:[77:java/lang/Throwable  , 88:java/lang/Throwable  ]
+TRY 46:[77:java/lang/Throwable  , 88:java/lang/Throwable  ]
+TRY 47:[77:java/lang/Throwable  , 88:java/lang/Throwable  ]
+TRY 50:[63:java/lang/Throwable  , 77:java/lang/Throwable  , 88:java/lang/Throwable  ]
+TRY 54:[63:java/lang/Throwable  , 77:java/lang/Throwable  , 88:java/lang/Throwable  ]
+TRY 58:[63:java/lang/Throwable  , 77:java/lang/Throwable  , 88:java/lang/Throwable  ]
+TRY 60:[77:java/lang/Throwable  , 88:java/lang/Throwable  ]
+TRY 61:[77:java/lang/Throwable  , 88:java/lang/Throwable  ]
+TRY 65:[77:java/lang/Throwable  , 88:java/lang/Throwable  ]
+TRY 66:[77:java/lang/Throwable  , 88:java/lang/Throwable  ]
+TRY 68:[77:java/lang/Throwable  , 88:java/lang/Throwable  ]
+TRY 73:[77:java/lang/Throwable  , 88:java/lang/Throwable  ]
+TRY 75:[77:java/lang/Throwable  , 88:java/lang/Throwable  ]
+TRY 79:[77:java/lang/Throwable  , 88:java/lang/Throwable  ]
+TRY 81:[88:java/lang/Throwable  ]
@@ -23,11 +40 @@
-IFNULL L13
-GOTO L28
-GOTO L22
-ASTORE 5
-ALOAD 2
-ALOAD 0
-ALOAD 3
-ALOAD 5
-INVOKESTATIC local/lito381/server/NativeSourceManualScope.unlock(Ljava/util/concurrent/locks/ReentrantLock;Llocal/lito381/server/ClientSession;Llocal/lito381/server/NativePlayerActionScope$Ticket;Ljava/lang/Throwable;)V
-ALOAD 5
-ATHROW
+IFNONNULL L18
@@ -41 +48,3 @@
-ASTORE 6
+DUP
+ASTORE 5
+MONITORENTER
@@ -47,4 +56,5 @@
-ASTORE 7
-ALOAD 7
-IFNULL L39
-GOTO L45
+ASTORE 6
+ALOAD 6
+IFNONNULL L38
+ALOAD 5
+MONITOREXIT
@@ -57 +67 @@
-ALOAD 7
+ALOAD 6
@@ -61,4 +71,4 @@
-ASTORE 8
-ALOAD 8
-IFNULL L77
-ALOAD 8
+ASTORE 7
+ALOAD 7
+IFNULL L70
+ALOAD 7
@@ -67 +77 @@
-ALOAD 8
+ALOAD 7
@@ -70 +80 @@
-ALOAD 7
+ALOAD 6
@@ -74 +84 @@
-ALOAD 7
+ALOAD 6
@@ -78 +88 @@
-ALOAD 8
+ALOAD 7
@@ -81,3 +91,3 @@
-GOTO L76
-ASTORE 9
-ALOAD 8
+GOTO L69
+ASTORE 8
+ALOAD 7
@@ -86 +96 @@
-ALOAD 9
+ALOAD 8
@@ -88,2 +98,2 @@
-GOTO L81
-ALOAD 7
+GOTO L74
+ALOAD 6
@@ -92,0 +103,8 @@
+ALOAD 5
+MONITOREXIT
+GOTO L82
+ASTORE 9
+ALOAD 5
+MONITOREXIT
+ALOAD 9
+ATHROW
@@ -97,0 +116,9 @@
+GOTO L96
+ASTORE 10
+ALOAD 2
+ALOAD 0
+ALOAD 3
+ALOAD 10
+INVOKESTATIC local/lito381/server/NativeSourceManualScope.unlock(Ljava/util/concurrent/locks/ReentrantLock;Llocal/lito381/server/ClientSession;Llocal/lito381/server/NativePlayerActionScope$Ticket;Ljava/lang/Throwable;)V
+ALOAD 10
+ATHROW
--- staged/NativePlayerCommit/nativeManaTransferRecover-406af235.txt
+++ candidate/NativePlayerCommit/nativeManaTransferRecover-406af235.txt
@@ -8,21 +8,21 @@
-TRY 30:[144:java/lang/Throwable  ]
-TRY 31:[144:java/lang/Throwable  ]
-TRY 33:[133:java/lang/Throwable  , 144:java/lang/Throwable  ]
-TRY 37:[133:java/lang/Throwable  , 144:java/lang/Throwable  ]
-TRY 41:[133:java/lang/Throwable  , 144:java/lang/Throwable  ]
-TRY 45:[133:java/lang/Throwable  , 144:java/lang/Throwable  ]
-TRY 49:[133:java/lang/Throwable  , 144:java/lang/Throwable  ]
-TRY 53:[133:java/lang/Throwable  , 144:java/lang/Throwable  ]
-TRY 57:[133:java/lang/Throwable  , 144:java/lang/Throwable  ]
-TRY 68:[133:java/lang/Throwable  , 144:java/lang/Throwable  ]
-TRY 82:[133:java/lang/Throwable  , 144:java/lang/Throwable  ]
-TRY 106:[133:java/lang/Throwable  , 144:java/lang/Throwable  ]
-TRY 107:[133:java/lang/Throwable  , 144:java/lang/Throwable  ]
-TRY 113:[133:java/lang/Throwable  , 144:java/lang/Throwable  ]
-TRY 114:[133:java/lang/Throwable  , 144:java/lang/Throwable  ]
-TRY 128:[133:java/lang/Throwable  , 144:java/lang/Throwable  ]
-TRY 130:[144:java/lang/Throwable  ]
-TRY 131:[144:java/lang/Throwable  ]
-TRY 135:[144:java/lang/Throwable  ]
-TRY 136:[144:java/lang/Throwable  ]
-TRY 138:[144:java/lang/Throwable  ]
+TRY 32:[146:java/lang/Throwable  ]
+TRY 33:[146:java/lang/Throwable  ]
+TRY 35:[135:java/lang/Throwable  , 146:java/lang/Throwable  ]
+TRY 39:[135:java/lang/Throwable  , 146:java/lang/Throwable  ]
+TRY 43:[135:java/lang/Throwable  , 146:java/lang/Throwable  ]
+TRY 47:[135:java/lang/Throwable  , 146:java/lang/Throwable  ]
+TRY 51:[135:java/lang/Throwable  , 146:java/lang/Throwable  ]
+TRY 55:[135:java/lang/Throwable  , 146:java/lang/Throwable  ]
+TRY 59:[135:java/lang/Throwable  , 146:java/lang/Throwable  ]
+TRY 70:[135:java/lang/Throwable  , 146:java/lang/Throwable  ]
+TRY 84:[135:java/lang/Throwable  , 146:java/lang/Throwable  ]
+TRY 108:[135:java/lang/Throwable  , 146:java/lang/Throwable  ]
+TRY 109:[135:java/lang/Throwable  , 146:java/lang/Throwable  ]
+TRY 115:[135:java/lang/Throwable  , 146:java/lang/Throwable  ]
+TRY 116:[135:java/lang/Throwable  , 146:java/lang/Throwable  ]
+TRY 130:[135:java/lang/Throwable  , 146:java/lang/Throwable  ]
+TRY 132:[146:java/lang/Throwable  ]
+TRY 133:[146:java/lang/Throwable  ]
+TRY 137:[146:java/lang/Throwable  ]
+TRY 138:[146:java/lang/Throwable  ]
+TRY 140:[146:java/lang/Throwable  ]
@@ -40,0 +41,2 @@
+ALOAD 1
+POP
@@ -44 +46 @@
-IFNONNULL L17
+IFNONNULL L19
@@ -63 +65 @@
-IFNE L129
+IFNE L131
@@ -67 +69 @@
-IFEQ L129
+IFEQ L131
@@ -71 +73 @@
-IFLE L129
+IFLE L131
@@ -89 +91 @@
-IFLE L71
+IFLE L73
@@ -99 +101 @@
-GOTO L72
+GOTO L74
@@ -103 +105 @@
-IFLE L85
+IFLE L87
@@ -113 +115 @@
-GOTO L86
+GOTO L88
@@ -118 +120 @@
-IF_ICMPEQ L92
+IF_ICMPEQ L94
@@ -120 +122 @@
-GOTO L93
+GOTO L95
@@ -125 +127 @@
-IF_ICMPEQ L99
+IF_ICMPEQ L101
@@ -127 +129 @@
-GOTO L100
+GOTO L102
@@ -131 +133 @@
-IFEQ L108
+IFEQ L110
@@ -138 +140 @@
-IFEQ L115
+IFEQ L117
@@ -145 +147 @@
-IFNE L119
+IFNE L121
@@ -147 +149 @@
-IFEQ L129
+IFEQ L131
@@ -161 +163 @@
-GOTO L139
+GOTO L141
@@ -172 +174 @@
-GOTO L151
+GOTO L153
--- staged/NativePlayerCommit/nativeSourceProc68MutateOwned-c2adc9d.txt
+++ candidate/NativePlayerCommit/nativeSourceProc68MutateOwned-c2adc9d.txt
@@ -8,4 +8,4 @@
-TRY 30:[54:java/lang/Throwable  ]
-TRY 34:[54:java/lang/Throwable  ]
-TRY 38:[54:java/lang/Throwable  ]
-TRY 41:[54:java/lang/Throwable  ]
+TRY 30:[52:java/lang/Throwable  ]
+TRY 34:[52:java/lang/Throwable  ]
+TRY 38:[52:java/lang/Throwable  ]
+TRY 41:[52:java/lang/Throwable  ]
@@ -59,2 +58,0 @@
-ILOAD 9
-ISTORE 10
@@ -64 +62 @@
-ILOAD 10
+ILOAD 9
@@ -66 +64 @@
-ASTORE 11
+ASTORE 10
@@ -70 +68 @@
-ALOAD 11
+ALOAD 10
--- staged/NativePlayerCommit/nativeSourceProc68Queue-640b4453.txt
+++ candidate/NativePlayerCommit/nativeSourceProc68Queue-640b4453.txt
@@ -8,19 +8,15 @@
-TRY 14:[64:java/lang/Throwable  ]
-TRY 19:[64:java/lang/Throwable  ]
-TRY 20:[64:java/lang/Throwable  ]
-TRY 22:[64:java/lang/Throwable  ]
-TRY 25:[64:java/lang/Throwable  ]
-TRY 29:[64:java/lang/Throwable  ]
-TRY 30:[64:java/lang/Throwable  ]
-TRY 35:[64:java/lang/Throwable  ]
-TRY 37:[64:java/lang/Throwable  ]
-TRY 43:[64:java/lang/Throwable  ]
-TRY 44:[64:java/lang/Throwable  ]
-TRY 48:[64:java/lang/Throwable  ]
-TRY 50:[64:java/lang/Throwable  ]
-TRY 51:[64:java/lang/Throwable  ]
-TRY 56:[64:java/lang/Throwable  ]
-TRY 59:[64:java/lang/Throwable  ]
-TRY 60:[64:java/lang/Throwable  ]
-TRY 62:[64:java/lang/Throwable  ]
-TRY 66:[64:java/lang/Throwable  ]
+TRY 12:[61:java/lang/Throwable  ]
+TRY 16:[61:java/lang/Throwable  ]
+TRY 22:[61:java/lang/Throwable  ]
+TRY 26:[61:java/lang/Throwable  ]
+TRY 27:[61:java/lang/Throwable  ]
+TRY 32:[61:java/lang/Throwable  ]
+TRY 34:[61:java/lang/Throwable  ]
+TRY 40:[61:java/lang/Throwable  ]
+TRY 41:[61:java/lang/Throwable  ]
+TRY 45:[61:java/lang/Throwable  ]
+TRY 47:[61:java/lang/Throwable  ]
+TRY 48:[61:java/lang/Throwable  ]
+TRY 53:[61:java/lang/Throwable  ]
+TRY 56:[61:java/lang/Throwable  ]
+TRY 63:[61:java/lang/Throwable  ]
@@ -27,0 +24,2 @@
+ASTORE 6
+ALOAD 6
@@ -29 +26,0 @@
-ASTORE 6
@@ -31,3 +27,0 @@
-ALOAD 7
-DUP
-ASTORE 8
@@ -42,2 +36 @@
-IFEQ L17
-GOTO L24
+IFNE L21
@@ -44,0 +38,2 @@
+ALOAD 7
+MONITOREXIT
@@ -48,2 +42,0 @@
-ALOAD 8
-MONITOREXIT
@@ -53,2 +46,2 @@
-ASTORE 9
-ALOAD 9
+ASTORE 8
+ALOAD 8
@@ -58,4 +51,4 @@
-ASTORE 10
-ALOAD 10
-IFNULL L39
-ALOAD 10
+ASTORE 9
+ALOAD 9
+IFNULL L36
+ALOAD 9
@@ -65,4 +58,4 @@
-IFNE L57
-ALOAD 10
-IFNULL L44
-ALOAD 10
+IFNE L54
+ALOAD 9
+IFNULL L41
+ALOAD 9
@@ -79 +72,3 @@
-ASTORE 10
+ASTORE 9
+ALOAD 8
+CONST str:"sourcePlayerQueue"
@@ -81,2 +75,0 @@
-CONST str:"sourcePlayerQueue"
-ALOAD 10
@@ -84 +77,3 @@
-ALOAD 10
+ALOAD 9
+ALOAD 7
+MONITOREXIT
@@ -88 +83,3 @@
-ALOAD 8
+ARETURN
+ASTORE 10
+ALOAD 7
@@ -90,5 +87 @@
-ARETURN
-ASTORE 11
-ALOAD 8
-MONITOREXIT
-ALOAD 11
+ALOAD 10
--- staged/NativePlayerCommit/nativeSourceScalarMutateOwned-a195e7a5.txt
+++ candidate/NativePlayerCommit/nativeSourceScalarMutateOwned-a195e7a5.txt
@@ -8,4 +8,4 @@
-TRY 29:[53:java/lang/Throwable  ]
-TRY 33:[53:java/lang/Throwable  ]
-TRY 37:[53:java/lang/Throwable  ]
-TRY 40:[53:java/lang/Throwable  ]
+TRY 29:[51:java/lang/Throwable  ]
+TRY 33:[51:java/lang/Throwable  ]
+TRY 37:[51:java/lang/Throwable  ]
+TRY 40:[51:java/lang/Throwable  ]
@@ -58,2 +57,0 @@
-ILOAD 8
-ISTORE 9
@@ -63 +61 @@
-ILOAD 9
+ILOAD 8
@@ -65 +63 @@
-ASTORE 10
+ASTORE 9
@@ -69 +67 @@
-ALOAD 10
+ALOAD 9
--- staged/NativePlayerCommit/nativeSourceScalarQueue-c1f827db.txt
+++ candidate/NativePlayerCommit/nativeSourceScalarQueue-c1f827db.txt
@@ -8,19 +8,15 @@
-TRY 13:[63:java/lang/Throwable  ]
-TRY 18:[63:java/lang/Throwable  ]
-TRY 19:[63:java/lang/Throwable  ]
-TRY 21:[63:java/lang/Throwable  ]
-TRY 24:[63:java/lang/Throwable  ]
-TRY 28:[63:java/lang/Throwable  ]
-TRY 29:[63:java/lang/Throwable  ]
-TRY 34:[63:java/lang/Throwable  ]
-TRY 36:[63:java/lang/Throwable  ]
-TRY 42:[63:java/lang/Throwable  ]
-TRY 43:[63:java/lang/Throwable  ]
-TRY 47:[63:java/lang/Throwable  ]
-TRY 49:[63:java/lang/Throwable  ]
-TRY 50:[63:java/lang/Throwable  ]
-TRY 55:[63:java/lang/Throwable  ]
-TRY 58:[63:java/lang/Throwable  ]
-TRY 59:[63:java/lang/Throwable  ]
-TRY 61:[63:java/lang/Throwable  ]
-TRY 65:[63:java/lang/Throwable  ]
+TRY 11:[60:java/lang/Throwable  ]
+TRY 15:[60:java/lang/Throwable  ]
+TRY 21:[60:java/lang/Throwable  ]
+TRY 25:[60:java/lang/Throwable  ]
+TRY 26:[60:java/lang/Throwable  ]
+TRY 31:[60:java/lang/Throwable  ]
+TRY 33:[60:java/lang/Throwable  ]
+TRY 39:[60:java/lang/Throwable  ]
+TRY 40:[60:java/lang/Throwable  ]
+TRY 44:[60:java/lang/Throwable  ]
+TRY 46:[60:java/lang/Throwable  ]
+TRY 47:[60:java/lang/Throwable  ]
+TRY 52:[60:java/lang/Throwable  ]
+TRY 55:[60:java/lang/Throwable  ]
+TRY 62:[60:java/lang/Throwable  ]
@@ -27,0 +24,2 @@
+ASTORE 5
+ALOAD 5
@@ -29 +26,0 @@
-ASTORE 5
@@ -31,3 +27,0 @@
-ALOAD 6
-DUP
-ASTORE 7
@@ -41,2 +35 @@
-IFEQ L16
-GOTO L23
+IFNE L20
@@ -43,0 +37,2 @@
+ALOAD 6
+MONITOREXIT
@@ -47,2 +41,0 @@
-ALOAD 7
-MONITOREXIT
@@ -52,2 +45,2 @@
-ASTORE 8
-ALOAD 8
+ASTORE 7
+ALOAD 7
@@ -57,4 +50,4 @@
-ASTORE 9
-ALOAD 9
-IFNULL L38
-ALOAD 9
+ASTORE 8
+ALOAD 8
+IFNULL L35
+ALOAD 8
@@ -64,4 +57,4 @@
-IFNE L56
-ALOAD 9
-IFNULL L43
-ALOAD 9
+IFNE L53
+ALOAD 8
+IFNULL L40
+ALOAD 8
@@ -78 +71,3 @@
-ASTORE 9
+ASTORE 8
+ALOAD 7
+CONST str:"sourcePlayerQueue"
@@ -80,2 +74,0 @@
-CONST str:"sourcePlayerQueue"
-ALOAD 9
@@ -83 +76,3 @@
-ALOAD 9
+ALOAD 8
+ALOAD 6
+MONITOREXIT
@@ -87 +82,3 @@
-ALOAD 7
+ARETURN
+ASTORE 9
+ALOAD 6
@@ -89,5 +86 @@
-ARETURN
-ASTORE 10
-ALOAD 7
-MONITOREXIT
-ALOAD 10
+ALOAD 9
--- staged/NativePlayerCommit/nativeStaticOriginalAdvance-390f6c66.txt
+++ candidate/NativePlayerCommit/nativeStaticOriginalAdvance-390f6c66.txt
@@ -8,5 +8,23 @@
-TRY 8:[14:java/lang/Throwable  ]
-TRY 57:[71:java/lang/Throwable  ]
-TRY 61:[71:java/lang/Throwable  ]
-TRY 65:[71:java/lang/Throwable  ]
-TRY 66:[71:java/lang/Throwable  ]
+TRY 8:[90:java/lang/Throwable  ]
+TRY 21:[90:java/lang/Throwable  ]
+TRY 23:[79:java/lang/Throwable  , 90:java/lang/Throwable  ]
+TRY 26:[79:java/lang/Throwable  , 90:java/lang/Throwable  ]
+TRY 31:[79:java/lang/Throwable  , 90:java/lang/Throwable  ]
+TRY 40:[79:java/lang/Throwable  , 90:java/lang/Throwable  ]
+TRY 41:[79:java/lang/Throwable  , 90:java/lang/Throwable  ]
+TRY 46:[79:java/lang/Throwable  , 90:java/lang/Throwable  ]
+TRY 47:[79:java/lang/Throwable  , 90:java/lang/Throwable  ]
+TRY 50:[64:java/lang/Throwable  , 79:java/lang/Throwable  , 90:java/lang/Throwable  ]
+TRY 54:[64:java/lang/Throwable  , 79:java/lang/Throwable  , 90:java/lang/Throwable  ]
+TRY 58:[64:java/lang/Throwable  , 79:java/lang/Throwable  , 90:java/lang/Throwable  ]
+TRY 59:[64:java/lang/Throwable  , 79:java/lang/Throwable  , 90:java/lang/Throwable  ]
+TRY 61:[79:java/lang/Throwable  , 90:java/lang/Throwable  ]
+TRY 62:[79:java/lang/Throwable  , 90:java/lang/Throwable  ]
+TRY 66:[79:java/lang/Throwable  , 90:java/lang/Throwable  ]
+TRY 67:[79:java/lang/Throwable  , 90:java/lang/Throwable  ]
+TRY 69:[79:java/lang/Throwable  , 90:java/lang/Throwable  ]
+TRY 74:[79:java/lang/Throwable  , 90:java/lang/Throwable  ]
+TRY 75:[79:java/lang/Throwable  , 90:java/lang/Throwable  ]
+TRY 77:[79:java/lang/Throwable  , 90:java/lang/Throwable  ]
+TRY 81:[79:java/lang/Throwable  , 90:java/lang/Throwable  ]
+TRY 83:[90:java/lang/Throwable  ]
@@ -24,11 +42 @@
-IFNULL L13
-GOTO L28
-GOTO L22
-ASTORE 4
-ALOAD 1
-ALOAD 0
-ALOAD 2
-ALOAD 4
-INVOKESTATIC local/lito381/server/NativeSourceManualScope.unlock(Ljava/util/concurrent/locks/ReentrantLock;Llocal/lito381/server/ClientSession;Llocal/lito381/server/NativePlayerActionScope$Ticket;Ljava/lang/Throwable;)V
-ALOAD 4
-ATHROW
+IFNONNULL L18
@@ -42 +50,3 @@
-ASTORE 5
+DUP
+ASTORE 4
+MONITORENTER
@@ -48,4 +58,5 @@
-ASTORE 6
-ALOAD 6
-IFNULL L39
-GOTO L45
+ASTORE 5
+ALOAD 5
+IFNONNULL L38
+ALOAD 4
+MONITOREXIT
@@ -58 +69 @@
-ALOAD 6
+ALOAD 5
@@ -62,4 +73,4 @@
-ASTORE 7
-ALOAD 7
-IFNULL L78
-ALOAD 7
+ASTORE 6
+ALOAD 6
+IFNULL L71
+ALOAD 6
@@ -68 +79 @@
-ALOAD 7
+ALOAD 6
@@ -71 +82 @@
-ALOAD 6
+ALOAD 5
@@ -75 +86 @@
-ALOAD 6
+ALOAD 5
@@ -80 +91 @@
-ALOAD 7
+ALOAD 6
@@ -83,3 +94,3 @@
-GOTO L77
-ASTORE 8
-ALOAD 7
+GOTO L70
+ASTORE 7
+ALOAD 6
@@ -88 +99 @@
-ALOAD 8
+ALOAD 7
@@ -90,2 +101,2 @@
-GOTO L83
-ALOAD 6
+GOTO L76
+ALOAD 5
@@ -95,0 +107,8 @@
+ALOAD 4
+MONITOREXIT
+GOTO L84
+ASTORE 8
+ALOAD 4
+MONITOREXIT
+ALOAD 8
+ATHROW
@@ -100,0 +120,9 @@
+GOTO L98
+ASTORE 9
+ALOAD 1
+ALOAD 0
+ALOAD 2
+ALOAD 9
+INVOKESTATIC local/lito381/server/NativeSourceManualScope.unlock(Ljava/util/concurrent/locks/ReentrantLock;Llocal/lito381/server/ClientSession;Llocal/lito381/server/NativePlayerActionScope$Ticket;Ljava/lang/Throwable;)V
+ALOAD 9
+ATHROW
--- staged/NativePlayerCommit/owner-5acd438c.txt
+++ candidate/NativePlayerCommit/owner-5acd438c.txt
@@ -8,6 +8,6 @@
-TRY 11:[30:java/lang/Throwable  ]
-TRY 15:[30:java/lang/Throwable  ]
-TRY 20:[30:java/lang/Throwable  ]
-TRY 25:[30:java/lang/Throwable  ]
-TRY 28:[30:java/lang/Throwable  ]
-TRY 32:[30:java/lang/Throwable  ]
+TRY 9:[28:java/lang/Throwable  ]
+TRY 13:[28:java/lang/Throwable  ]
+TRY 18:[28:java/lang/Throwable  ]
+TRY 23:[28:java/lang/Throwable  ]
+TRY 26:[28:java/lang/Throwable  ]
+TRY 30:[28:java/lang/Throwable  ]
@@ -15,0 +16,2 @@
+ASTORE 1
+ALOAD 1
@@ -17 +18,0 @@
-ASTORE 1
@@ -19,3 +19,0 @@
-ALOAD 2
-DUP
-ASTORE 3
@@ -25,0 +24,4 @@
+ASTORE 3
+ALOAD 3
+CONST str:"sourcePlayerOwner"
+INVOKESTATIC local/lito381/server/CombatAccess.field(Ljava/lang/Object;Ljava/lang/String;)Ljava/lang/Object;
@@ -28,5 +30 @@
-CONST str:"sourcePlayerOwner"
-INVOKESTATIC local/lito381/server/CombatAccess.field(Ljava/lang/Object;Ljava/lang/String;)Ljava/lang/Object;
-ASTORE 5
-ALOAD 5
-IFNONNULL L26
+IFNONNULL L24
@@ -35 +33,3 @@
-ASTORE 5
+ASTORE 4
+ALOAD 3
+CONST str:"sourcePlayerOwner"
@@ -37,2 +36,0 @@
-CONST str:"sourcePlayerOwner"
-ALOAD 5
@@ -40,2 +38,2 @@
-ALOAD 5
-ALOAD 3
+ALOAD 4
+ALOAD 2
@@ -44,2 +42,2 @@
-ASTORE 6
-ALOAD 3
+ASTORE 5
+ALOAD 2
@@ -47 +45 @@
-ALOAD 6
+ALOAD 5
--- staged/NativePlayerCommit/publishDefense-390f6c66.txt
+++ candidate/NativePlayerCommit/publishDefense-390f6c66.txt
@@ -177 +177 @@
-GOTO L28
+GOTO L111
--- staged/NativePlayerCommit/queue-4047f286.txt
+++ candidate/NativePlayerCommit/queue-4047f286.txt
@@ -8,15 +8,15 @@
-TRY 11:[55:java/lang/Throwable  ]
-TRY 15:[55:java/lang/Throwable  ]
-TRY 19:[55:java/lang/Throwable  ]
-TRY 23:[55:java/lang/Throwable  ]
-TRY 24:[55:java/lang/Throwable  ]
-TRY 29:[55:java/lang/Throwable  ]
-TRY 31:[55:java/lang/Throwable  ]
-TRY 37:[55:java/lang/Throwable  ]
-TRY 38:[55:java/lang/Throwable  ]
-TRY 42:[55:java/lang/Throwable  ]
-TRY 44:[55:java/lang/Throwable  ]
-TRY 45:[55:java/lang/Throwable  ]
-TRY 50:[55:java/lang/Throwable  ]
-TRY 53:[55:java/lang/Throwable  ]
-TRY 57:[55:java/lang/Throwable  ]
+TRY 9:[53:java/lang/Throwable  ]
+TRY 13:[53:java/lang/Throwable  ]
+TRY 17:[53:java/lang/Throwable  ]
+TRY 21:[53:java/lang/Throwable  ]
+TRY 22:[53:java/lang/Throwable  ]
+TRY 27:[53:java/lang/Throwable  ]
+TRY 29:[53:java/lang/Throwable  ]
+TRY 35:[53:java/lang/Throwable  ]
+TRY 36:[53:java/lang/Throwable  ]
+TRY 40:[53:java/lang/Throwable  ]
+TRY 42:[53:java/lang/Throwable  ]
+TRY 43:[53:java/lang/Throwable  ]
+TRY 48:[53:java/lang/Throwable  ]
+TRY 51:[53:java/lang/Throwable  ]
+TRY 55:[53:java/lang/Throwable  ]
@@ -24,0 +25,2 @@
+ASTORE 2
+ALOAD 2
@@ -26 +27,0 @@
-ASTORE 2
@@ -28,3 +28,0 @@
-ALOAD 3
-DUP
-ASTORE 4
@@ -35 +33 @@
-IFNE L17
+IFNE L15
@@ -37 +35 @@
-ALOAD 4
+ALOAD 3
@@ -43,2 +41,2 @@
-ASTORE 5
-ALOAD 5
+ASTORE 4
+ALOAD 4
@@ -48,4 +46,4 @@
-ASTORE 6
-ALOAD 6
-IFNULL L33
-ALOAD 6
+ASTORE 5
+ALOAD 5
+IFNULL L31
+ALOAD 5
@@ -55,4 +53,4 @@
-IFNE L51
-ALOAD 6
-IFNULL L38
-ALOAD 6
+IFNE L49
+ALOAD 5
+IFNULL L36
+ALOAD 5
@@ -69 +67,3 @@
-ASTORE 6
+ASTORE 5
+ALOAD 4
+CONST str:"sourcePlayerQueue"
@@ -71,2 +70,0 @@
-CONST str:"sourcePlayerQueue"
-ALOAD 6
@@ -74,2 +72,2 @@
-ALOAD 6
-ALOAD 4
+ALOAD 5
+ALOAD 3
@@ -78,2 +76,2 @@
-ASTORE 7
-ALOAD 4
+ASTORE 6
+ALOAD 3
@@ -81 +79 @@
-ALOAD 7
+ALOAD 6
--- staged/NativePlayerCommit/selected-93488b0.txt
+++ candidate/NativePlayerCommit/selected-93488b0.txt
@@ -8,5 +8,5 @@
-TRY 8:[22:java/lang/Throwable  ]
-TRY 10:[22:java/lang/Throwable  ]
-TRY 14:[22:java/lang/Throwable  ]
-TRY 17:[22:java/lang/Throwable  ]
-TRY 20:[22:java/lang/Throwable  ]
+TRY 8:[27:java/lang/Throwable  ]
+TRY 10:[27:java/lang/Throwable  ]
+TRY 14:[27:java/lang/Throwable  ]
+TRY 17:[27:java/lang/Throwable  ]
+TRY 20:[27:java/lang/Throwable  ]
@@ -34 +34,6 @@
-GOTO L30
+ALOAD 2
+ALOAD 0
+ALOAD 3
+CONST null
+INVOKESTATIC local/lito381/server/NativeSourceManualScope.unlock(Ljava/util/concurrent/locks/ReentrantLock;Llocal/lito381/server/ClientSession;Llocal/lito381/server/NativePlayerActionScope$Ticket;Ljava/lang/Throwable;)V
+GOTO L35
@@ -43,5 +47,0 @@
-ALOAD 2
-ALOAD 0
-ALOAD 3
-CONST null
-INVOKESTATIC local/lito381/server/NativeSourceManualScope.unlock(Ljava/util/concurrent/locks/ReentrantLock;Llocal/lito381/server/ClientSession;Llocal/lito381/server/NativePlayerActionScope$Ticket;Ljava/lang/Throwable;)V
--- staged/NativePlayerCommit/worldPlayer-a5890f4e.txt
+++ candidate/NativePlayerCommit/worldPlayer-a5890f4e.txt
@@ -62 +61,0 @@
-DUP
@@ -63,0 +63 @@
+ILOAD 1
```

</details>

### NativePlayerCommit$Binding — SAME_PATHS

<details>
<summary>全部方法診斷</summary>

```json
[
  {
    "method": "capture(Llocal/lito381/server/ClientSession;)Llocal/lito381/server/NativePlayerCommit$Binding;",
    "status": "SAME_PATHS",
    "detail": "path multisets equal (16)"
  }
]
```

</details>
<details>
<summary>完整正規化差異</summary>

```diff
--- staged/NativePlayerCommit$Binding/capture-30df8c1a.txt
+++ candidate/NativePlayerCommit$Binding/capture-30df8c1a.txt
@@ -8,14 +8,14 @@
-TRY 13:[42:java/lang/Throwable  ]
-TRY 16:[42:java/lang/Throwable  ]
-TRY 18:[42:java/lang/Throwable  ]
-TRY 20:[42:java/lang/Throwable  ]
-TRY 22:[42:java/lang/Throwable  ]
-TRY 24:[42:java/lang/Throwable  ]
-TRY 27:[42:java/lang/Throwable  ]
-TRY 28:[42:java/lang/Throwable  ]
-TRY 31:[42:java/lang/Throwable  ]
-TRY 34:[42:java/lang/Throwable  ]
-TRY 37:[42:java/lang/Throwable  ]
-TRY 38:[42:java/lang/Throwable  ]
-TRY 40:[42:java/lang/Throwable  ]
-TRY 44:[42:java/lang/Throwable  ]
+TRY 11:[40:java/lang/Throwable  ]
+TRY 14:[40:java/lang/Throwable  ]
+TRY 16:[40:java/lang/Throwable  ]
+TRY 18:[40:java/lang/Throwable  ]
+TRY 20:[40:java/lang/Throwable  ]
+TRY 22:[40:java/lang/Throwable  ]
+TRY 25:[40:java/lang/Throwable  ]
+TRY 26:[40:java/lang/Throwable  ]
+TRY 29:[40:java/lang/Throwable  ]
+TRY 32:[40:java/lang/Throwable  ]
+TRY 35:[40:java/lang/Throwable  ]
+TRY 36:[40:java/lang/Throwable  ]
+TRY 38:[40:java/lang/Throwable  ]
+TRY 42:[40:java/lang/Throwable  ]
@@ -27,0 +28,2 @@
+ASTORE 1
+ALOAD 1
@@ -29 +30,0 @@
-ASTORE 1
@@ -31,3 +31,0 @@
-ALOAD 2
-DUP
-ASTORE 3
@@ -61 +59 @@
-ALOAD 3
+ALOAD 2
@@ -64 +62,3 @@
-ASTORE 4
+ASTORE 3
+ALOAD 2
+MONITOREXIT
@@ -66,2 +65,0 @@
-MONITOREXIT
-ALOAD 4
```

</details>

### NativePlayerCommit$Queue — SAME_PATHS

<details>
<summary>全部方法診斷</summary>

```json
[
  {
    "method": "claim()Llocal/lito381/server/NativePlayerCommit$Event;",
    "status": "SAME_PATHS",
    "detail": "path multisets equal (31)"
  }
]
```

</details>
<details>
<summary>完整正規化差異</summary>

```diff
--- staged/NativePlayerCommit$Queue/claim-97951874.txt
+++ candidate/NativePlayerCommit$Queue/claim-97951874.txt
@@ -25 +24,0 @@
-DUP
@@ -27 +25,0 @@
-ASTORE 2
@@ -29 +27 @@
-IFNULL L33
+IFNULL L31
--- staged/NativePlayerCommit$Queue/drop-1385ff.txt
+++ candidate/NativePlayerCommit$Queue/drop-1385ff.txt
@@ -29,2 +29 @@
-ASTORE 3
-ALOAD 3
+DUP
@@ -32 +30,0 @@
-ALOAD 3
@@ -67 +65 @@
-IFNE L68
+IFNE L66
--- staged/NativePlayerCommit$Queue/nativeFreezeAcceptedOriginalOffer-874a847e.txt
+++ candidate/NativePlayerCommit$Queue/nativeFreezeAcceptedOriginalOffer-874a847e.txt
@@ -8,28 +8,28 @@
-TRY 9:[62:java/lang/Throwable  ]
-TRY 12:[62:java/lang/Throwable  ]
-TRY 15:[62:java/lang/Throwable  ]
-TRY 16:[62:java/lang/Throwable  ]
-TRY 19:[62:java/lang/Throwable  ]
-TRY 20:[62:java/lang/Throwable  ]
-TRY 21:[62:java/lang/Throwable  ]
-TRY 24:[62:java/lang/Throwable  ]
-TRY 25:[62:java/lang/Throwable  ]
-TRY 26:[62:java/lang/Throwable  ]
-TRY 27:[62:java/lang/Throwable  ]
-TRY 29:[62:java/lang/Throwable  ]
-TRY 30:[62:java/lang/Throwable  ]
-TRY 34:[62:java/lang/Throwable  ]
-TRY 37:[62:java/lang/Throwable  ]
-TRY 40:[62:java/lang/Throwable  ]
-TRY 41:[62:java/lang/Throwable  ]
-TRY 42:[62:java/lang/Throwable  ]
-TRY 45:[62:java/lang/Throwable  ]
-TRY 46:[62:java/lang/Throwable  ]
-TRY 47:[62:java/lang/Throwable  ]
-TRY 49:[62:java/lang/Throwable  ]
-TRY 50:[62:java/lang/Throwable  ]
-TRY 53:[62:java/lang/Throwable  ]
-TRY 54:[62:java/lang/Throwable  ]
-TRY 56:[62:java/lang/Throwable  ]
-TRY 60:[62:java/lang/Throwable  ]
-TRY 64:[62:java/lang/Throwable  ]
+TRY 7:[60:java/lang/Throwable  ]
+TRY 10:[60:java/lang/Throwable  ]
+TRY 13:[60:java/lang/Throwable  ]
+TRY 14:[60:java/lang/Throwable  ]
+TRY 17:[60:java/lang/Throwable  ]
+TRY 18:[60:java/lang/Throwable  ]
+TRY 19:[60:java/lang/Throwable  ]
+TRY 22:[60:java/lang/Throwable  ]
+TRY 23:[60:java/lang/Throwable  ]
+TRY 24:[60:java/lang/Throwable  ]
+TRY 25:[60:java/lang/Throwable  ]
+TRY 27:[60:java/lang/Throwable  ]
+TRY 28:[60:java/lang/Throwable  ]
+TRY 32:[60:java/lang/Throwable  ]
+TRY 35:[60:java/lang/Throwable  ]
+TRY 38:[60:java/lang/Throwable  ]
+TRY 39:[60:java/lang/Throwable  ]
+TRY 40:[60:java/lang/Throwable  ]
+TRY 43:[60:java/lang/Throwable  ]
+TRY 44:[60:java/lang/Throwable  ]
+TRY 45:[60:java/lang/Throwable  ]
+TRY 47:[60:java/lang/Throwable  ]
+TRY 48:[60:java/lang/Throwable  ]
+TRY 51:[60:java/lang/Throwable  ]
+TRY 52:[60:java/lang/Throwable  ]
+TRY 54:[60:java/lang/Throwable  ]
+TRY 58:[60:java/lang/Throwable  ]
+TRY 62:[60:java/lang/Throwable  ]
@@ -38,0 +39 @@
+DUP
@@ -40,3 +40,0 @@
-ALOAD 2
-DUP
-ASTORE 3
@@ -46 +44 @@
-IFNE L29
+IFNE L27
@@ -49 +47 @@
-IFNE L29
+IFNE L27
@@ -53 +51 @@
-IFEQ L29
+IFEQ L27
@@ -58 +56 @@
-IF_ICMPGE L29
+IF_ICMPGE L27
@@ -64 +62 @@
-IFNE L36
+IFNE L34
@@ -69 +67 @@
-ALOAD 3
+ALOAD 2
@@ -95 +93 @@
-ALOAD 3
+ALOAD 2
@@ -98 +96,3 @@
-ASTORE 4
+ASTORE 3
+ALOAD 2
+MONITOREXIT
@@ -100,2 +99,0 @@
-MONITOREXIT
-ALOAD 4
--- staged/NativePlayerCommit$Queue/requestDrain-9b75.txt
+++ candidate/NativePlayerCommit$Queue/requestDrain-9b75.txt
@@ -8,7 +8,7 @@
-TRY 7:[27:java/lang/Throwable  ]
-TRY 12:[27:java/lang/Throwable  ]
-TRY 16:[27:java/lang/Throwable  ]
-TRY 20:[27:java/lang/Throwable  ]
-TRY 22:[27:java/lang/Throwable  ]
-TRY 25:[27:java/lang/Throwable  ]
-TRY 29:[27:java/lang/Throwable  ]
+TRY 5:[25:java/lang/Throwable  ]
+TRY 10:[25:java/lang/Throwable  ]
+TRY 14:[25:java/lang/Throwable  ]
+TRY 18:[25:java/lang/Throwable  ]
+TRY 20:[25:java/lang/Throwable  ]
+TRY 23:[25:java/lang/Throwable  ]
+TRY 27:[25:java/lang/Throwable  ]
@@ -15,0 +16 @@
+DUP
@@ -17,3 +17,0 @@
-ALOAD 1
-DUP
-ASTORE 2
@@ -23,3 +21 @@
-ASTORE 3
-ALOAD 3
-IFNONNULL L14
+ASTORE 2
@@ -26,0 +23,2 @@
+IFNONNULL L12
+ALOAD 1
@@ -30 +28 @@
-ALOAD 3
+ALOAD 2
@@ -38 +36,11 @@
-ISTORE 4
+ISTORE 3
+ALOAD 1
+MONITOREXIT
+GOTO L30
+ASTORE 4
+ALOAD 1
+MONITOREXIT
+ALOAD 4
+ATHROW
+ILOAD 3
+IFEQ L37
@@ -40,10 +47,0 @@
-MONITOREXIT
-GOTO L32
-ASTORE 5
-ALOAD 2
-MONITOREXIT
-ALOAD 5
-ATHROW
-ILOAD 4
-IFEQ L39
-ALOAD 3
```

</details>

### NativePlayerLifetime — DIFF

```json
{
  "where": "method",
  "member": "shutdown(ZJ)Llocal/lito381/server/NativePlayerLifetime$Shutdown;",
  "insnIndex": null,
  "detail": "left=25:[44:java/lang/Throwable  ] right=23:[42:java/lang/Throwable  ]; paths rejected: normal control-flow cycle"
}
```
<details>
<summary>全部方法診斷</summary>

```json
[
  {
    "method": "arm(Llocal/lito381/server/ClientSession;Llocal/lito381/server/NativePlayerCommit$Queue;JI)V",
    "status": "SAME_PATHS",
    "detail": "path multisets equal (45)"
  },
  {
    "method": "shutdown(ZJ)Llocal/lito381/server/NativePlayerLifetime$Shutdown;",
    "status": "DIFF",
    "detail": "method shutdown(ZJ)Llocal/lito381/server/NativePlayerLifetime$Shutdown;: left=25:[44:java/lang/Throwable  ] right=23:[42:java/lang/Throwable  ]; paths rejected: normal control-flow cycle"
  }
]
```

</details>
<details>
<summary>完整正規化差異</summary>

```diff
--- staged/NativePlayerLifetime/arm-6b0a81d1.txt
+++ candidate/NativePlayerLifetime/arm-6b0a81d1.txt
@@ -37 +37 @@
-GOTO L41
+GOTO L40
@@ -47,2 +47 @@
-IFNE L35
-GOTO L41
+IFEQ L40
--- staged/NativePlayerLifetime/openHub-6a1ec0ce.txt
+++ candidate/NativePlayerLifetime/openHub-6a1ec0ce.txt
@@ -8,11 +8,11 @@
-TRY 9:[31:java/lang/Throwable  ]
-TRY 11:[31:java/lang/Throwable  ]
-TRY 14:[31:java/lang/Throwable  ]
-TRY 15:[31:java/lang/Throwable  ]
-TRY 16:[31:java/lang/Throwable  ]
-TRY 18:[31:java/lang/Throwable  ]
-TRY 23:[31:java/lang/Throwable  ]
-TRY 24:[31:java/lang/Throwable  ]
-TRY 26:[31:java/lang/Throwable  ]
-TRY 29:[31:java/lang/Throwable  ]
-TRY 33:[31:java/lang/Throwable  ]
+TRY 7:[29:java/lang/Throwable  ]
+TRY 9:[29:java/lang/Throwable  ]
+TRY 12:[29:java/lang/Throwable  ]
+TRY 13:[29:java/lang/Throwable  ]
+TRY 14:[29:java/lang/Throwable  ]
+TRY 16:[29:java/lang/Throwable  ]
+TRY 21:[29:java/lang/Throwable  ]
+TRY 22:[29:java/lang/Throwable  ]
+TRY 24:[29:java/lang/Throwable  ]
+TRY 27:[29:java/lang/Throwable  ]
+TRY 31:[29:java/lang/Throwable  ]
@@ -20,0 +21 @@
+DUP
@@ -22,3 +22,0 @@
-ALOAD 1
-DUP
-ASTORE 2
@@ -29 +27 @@
-IFNULL L16
+IFNULL L14
@@ -38 +36 @@
-ASTORE 3
+ASTORE 2
@@ -41 +39 @@
-ALOAD 3
+ALOAD 2
@@ -44 +42 @@
-ALOAD 3
+ALOAD 2
@@ -47 +45 @@
-ALOAD 2
+ALOAD 1
@@ -49,3 +47,3 @@
-GOTO L36
-ASTORE 4
-ALOAD 2
+GOTO L34
+ASTORE 3
+ALOAD 1
@@ -53 +51 @@
-ALOAD 4
+ALOAD 3
--- staged/NativePlayerLifetime/shutdown-964ada94.txt
+++ candidate/NativePlayerLifetime/shutdown-964ada94.txt
@@ -8,30 +8,30 @@
-TRY 25:[44:java/lang/Throwable  ]
-TRY 26:[44:java/lang/Throwable  ]
-TRY 31:[44:java/lang/Throwable  ]
-TRY 33:[44:java/lang/Throwable  ]
-TRY 34:[44:java/lang/Throwable  ]
-TRY 39:[44:java/lang/Throwable  ]
-TRY 40:[44:java/lang/Throwable  ]
-TRY 42:[44:java/lang/Throwable  ]
-TRY 46:[44:java/lang/Throwable  ]
-TRY 135:[137:java/lang/InterruptedException  ]
-TRY 150:[155:java/lang/Throwable  ]
-TRY 151:[155:java/lang/Throwable  ]
-TRY 153:[155:java/lang/Throwable  ]
-TRY 157:[155:java/lang/Throwable  ]
-TRY 224:[256:java/lang/InterruptedException  ]
-TRY 226:[256:java/lang/InterruptedException  ]
-TRY 227:[256:java/lang/InterruptedException  ]
-TRY 230:[256:java/lang/InterruptedException  ]
-TRY 231:[256:java/lang/InterruptedException  ]
-TRY 234:[256:java/lang/InterruptedException  ]
-TRY 236:[256:java/lang/InterruptedException  ]
-TRY 237:[256:java/lang/InterruptedException  ]
-TRY 240:[256:java/lang/InterruptedException  ]
-TRY 242:[256:java/lang/InterruptedException  ]
-TRY 243:[256:java/lang/InterruptedException  ]
-TRY 246:[256:java/lang/InterruptedException  ]
-TRY 247:[256:java/lang/InterruptedException  ]
-TRY 250:[256:java/lang/InterruptedException  ]
-TRY 252:[256:java/lang/InterruptedException  ]
-TRY 253:[256:java/lang/InterruptedException  ]
+TRY 23:[42:java/lang/Throwable  ]
+TRY 24:[42:java/lang/Throwable  ]
+TRY 29:[42:java/lang/Throwable  ]
+TRY 31:[42:java/lang/Throwable  ]
+TRY 32:[42:java/lang/Throwable  ]
+TRY 37:[42:java/lang/Throwable  ]
+TRY 38:[42:java/lang/Throwable  ]
+TRY 40:[42:java/lang/Throwable  ]
+TRY 44:[42:java/lang/Throwable  ]
+TRY 129:[131:java/lang/InterruptedException  ]
+TRY 141:[146:java/lang/Throwable  ]
+TRY 142:[146:java/lang/Throwable  ]
+TRY 144:[146:java/lang/Throwable  ]
+TRY 148:[146:java/lang/Throwable  ]
+TRY 215:[247:java/lang/InterruptedException  ]
+TRY 217:[247:java/lang/InterruptedException  ]
+TRY 218:[247:java/lang/InterruptedException  ]
+TRY 221:[247:java/lang/InterruptedException  ]
+TRY 222:[247:java/lang/InterruptedException  ]
+TRY 225:[247:java/lang/InterruptedException  ]
+TRY 227:[247:java/lang/InterruptedException  ]
+TRY 228:[247:java/lang/InterruptedException  ]
+TRY 231:[247:java/lang/InterruptedException  ]
+TRY 233:[247:java/lang/InterruptedException  ]
+TRY 234:[247:java/lang/InterruptedException  ]
+TRY 237:[247:java/lang/InterruptedException  ]
+TRY 238:[247:java/lang/InterruptedException  ]
+TRY 241:[247:java/lang/InterruptedException  ]
+TRY 243:[247:java/lang/InterruptedException  ]
+TRY 244:[247:java/lang/InterruptedException  ]
@@ -56,0 +57 @@
+DUP
@@ -58,3 +58,0 @@
-ALOAD 8
-DUP
-ASTORE 9
@@ -65 +63 @@
-IF_ACMPNE L36
+IF_ACMPNE L34
@@ -68 +66 @@
-IFEQ L33
+IFEQ L31
@@ -70 +68 @@
-GOTO L34
+GOTO L32
@@ -73 +71 @@
-GOTO L41
+GOTO L39
@@ -75 +73 @@
-IFNE L41
+IFNE L39
@@ -78,0 +77,6 @@
+ALOAD 8
+MONITOREXIT
+GOTO L47
+ASTORE 9
+ALOAD 8
+MONITOREXIT
@@ -80,6 +83,0 @@
-MONITOREXIT
-GOTO L49
-ASTORE 10
-ALOAD 9
-MONITOREXIT
-ALOAD 10
@@ -88,2 +86 @@
-ASTORE 11
-ALOAD 11
+DUP
@@ -91 +87,0 @@
-ALOAD 11
@@ -104 +100 @@
-LSTORE 12/13
+LSTORE 10/11
@@ -106 +102 @@
-IFEQ L142
+IFEQ L135
@@ -110 +106 @@
-IF_ACMPNE L142
+IF_ACMPNE L135
@@ -116,2 +112,2 @@
-ASTORE 14
-ALOAD 14
+ASTORE 12
+ALOAD 12
@@ -119,8 +115,8 @@
-ISTORE 15
-CONST Integer:0
-ISTORE 16
-ILOAD 16
-ILOAD 15
-IF_ICMPGE L95
-ALOAD 14
-ILOAD 16
+ISTORE 13
+CONST Integer:0
+ISTORE 14
+ILOAD 14
+ILOAD 13
+IF_ICMPGE L91
+ALOAD 12
+ILOAD 14
@@ -128,2 +124,2 @@
-ASTORE 17
-ALOAD 17
+ASTORE 15
+ALOAD 15
@@ -131,2 +127,2 @@
-IINC var=16 incr=1
-GOTO L84
+IINC var=14 incr=1
+GOTO L80
@@ -138,2 +134,2 @@
-ASTORE 18
-ALOAD 18
+ASTORE 16
+ALOAD 16
@@ -141,8 +137,8 @@
-ISTORE 19
-CONST Integer:0
-ISTORE 20
-ILOAD 20
-ILOAD 19
-IF_ICMPGE L117
-ALOAD 18
-ILOAD 20
+ISTORE 17
+CONST Integer:0
+ISTORE 18
+ILOAD 18
+ILOAD 17
+IF_ICMPGE L113
+ALOAD 16
+ILOAD 18
@@ -150,2 +146,2 @@
-ASTORE 21
-ALOAD 21
+ASTORE 19
+ALOAD 19
@@ -153,5 +149,4 @@
-IINC var=20 incr=1
-GOTO L106
-ALOAD 0
-ASTORE 22
-ALOAD 22
+IINC var=18 incr=1
+GOTO L102
+ALOAD 0
+DUP
@@ -159 +153,0 @@
-ALOAD 22
@@ -161 +155 @@
-IFEQ L127
+IFEQ L121
@@ -164 +158 @@
-GOTO L142
+GOTO L135
@@ -166 +160 @@
-LLOAD 12/13
+LLOAD 10/11
@@ -170,2 +164,2 @@
-IFLT L134
-GOTO L142
+IFLT L128
+GOTO L135
@@ -174,2 +168,2 @@
-GOTO L141
-ASTORE 23
+GOTO L69
+ASTORE 20
@@ -178,2 +172 @@
-GOTO L142
-GOTO L73
+GOTO L135
@@ -182,4 +175,2 @@
-ASTORE 24
-ALOAD 24
-DUP
-ASTORE 25
+DUP
+ASTORE 21
@@ -190 +181 @@
-ALOAD 25
+ALOAD 21
@@ -192,3 +183,3 @@
-GOTO L160
-ASTORE 26
-ALOAD 25
+GOTO L151
+ASTORE 22
+ALOAD 21
@@ -196 +187 @@
-ALOAD 26
+ALOAD 22
@@ -206,0 +198,22 @@
+ASTORE 23
+ALOAD 23
+ARRAYLENGTH
+ISTORE 24
+CONST Integer:0
+ISTORE 25
+ILOAD 25
+ILOAD 24
+IF_ICMPGE L177
+ALOAD 23
+ILOAD 25
+AALOAD
+ASTORE 26
+ALOAD 26
+INVOKEVIRTUAL local/lito381/server/NativePlayerCommit$Queue.stop()V
+IINC var=25 incr=1
+GOTO L166
+ALOAD 0
+GETFIELD local/lito381/server/NativePlayerLifetime.npcs:Ljava/util/Set;
+INVOKEDYNAMIC apply()Ljava/util/function/IntFunction; bsm=H6 java/lang/invoke/LambdaMetafactory.metafactory(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodHandle;Ljava/lang/invoke/MethodType;)Ljava/lang/invoke/CallSite; args=[type:(I)Ljava/lang/Object;, handle:H6 local/lito381/server/NativePlayerLifetime.lambda$content$4cc0bd50f5c7fd162b1842d935a24d556bb25ee07f378a80ea7c782095802936(I)[Llocal/lito381/server/NativeNpcLifetime$Lane;, type:(I)[Llocal/lito381/server/NativeNpcLifetime$Lane;]
+INVOKEINTERFACE java/util/Set.toArray(Ljava/util/function/IntFunction;)[Ljava/lang/Object; itf
+CHECKCAST [Llocal/lito381/server/NativeNpcLifetime$Lane;
@@ -215 +228 @@
-IF_ICMPGE L186
+IF_ICMPGE L199
@@ -221 +234 @@
-INVOKEVIRTUAL local/lito381/server/NativePlayerCommit$Queue.stop()V
+INVOKEVIRTUAL local/lito381/server/NativeNpcLifetime$Lane.stop()V
@@ -223,23 +236 @@
-GOTO L175
-ALOAD 0
-GETFIELD local/lito381/server/NativePlayerLifetime.npcs:Ljava/util/Set;
-INVOKEDYNAMIC apply()Ljava/util/function/IntFunction; bsm=H6 java/lang/invoke/LambdaMetafactory.metafactory(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodHandle;Ljava/lang/invoke/MethodType;)Ljava/lang/invoke/CallSite; args=[type:(I)Ljava/lang/Object;, handle:H6 local/lito381/server/NativePlayerLifetime.lambda$content$4cc0bd50f5c7fd162b1842d935a24d556bb25ee07f378a80ea7c782095802936(I)[Llocal/lito381/server/NativeNpcLifetime$Lane;, type:(I)[Llocal/lito381/server/NativeNpcLifetime$Lane;]
-INVOKEINTERFACE java/util/Set.toArray(Ljava/util/function/IntFunction;)[Ljava/lang/Object; itf
-CHECKCAST [Llocal/lito381/server/NativeNpcLifetime$Lane;
-ASTORE 31
-ALOAD 31
-ARRAYLENGTH
-ISTORE 32
-CONST Integer:0
-ISTORE 33
-ILOAD 33
-ILOAD 32
-IF_ICMPGE L208
-ALOAD 31
-ILOAD 33
-AALOAD
-ASTORE 34
-ALOAD 34
-INVOKEVIRTUAL local/lito381/server/NativeNpcLifetime$Lane.stop()V
-IINC var=33 incr=1
-GOTO L197
+GOTO L188
@@ -248 +239 @@
-IFNE L275
+IFNE L266
@@ -258 +249 @@
-IFLE L259
+IFLE L250
@@ -260 +251 @@
-IFNE L259
+IFNE L250
@@ -266 +257 @@
-IFNE L239
+IFNE L230
@@ -282 +273 @@
-IFNE L255
+IFNE L246
@@ -293,2 +284,2 @@
-GOTO L259
-ASTORE 35
+GOTO L250
+ASTORE 31
@@ -300 +291 @@
-IFNE L267
+IFNE L258
@@ -308 +299 @@
-IFNE L275
+IFNE L266
@@ -315,3 +306,3 @@
-ISTORE 36
-ILOAD 36
-IFEQ L284
+ISTORE 32
+ILOAD 32
+IFEQ L275
@@ -323 +314 @@
-IFEQ L288
+IFEQ L279
@@ -329 +320 @@
-ILOAD 36
+ILOAD 32
```

</details>

### NativePlayerVitalCompletions — DIFF

```json
{
  "where": "method",
  "member": "drain(Llocal/lito381/server/ClientSession;)V",
  "insnIndex": null,
  "detail": "left=22:[83:java/lang/Throwable  ] right=22:[85:java/lang/Throwable  ]; paths rejected: normal control-flow cycle"
}
```
<details>
<summary>全部方法診斷</summary>

```json
[
  {
    "method": "drain(Llocal/lito381/server/ClientSession;)V",
    "status": "DIFF",
    "detail": "method drain(Llocal/lito381/server/ClientSession;)V: left=22:[83:java/lang/Throwable  ] right=22:[85:java/lang/Throwable  ]; paths rejected: normal control-flow cycle"
  },
  {
    "method": "nativeSourceMaterialPairReserve(Llocal/lito381/server/ClientSession;Llocal/lito381/server/NativeSelfCastAction$Task;)Llocal/lito381/server/NativePlayerVitalCompletions$Receipt;",
    "status": "SAME_PATHS",
    "detail": "path multisets equal (130)"
  },
  {
    "method": "nativeSourceTargetNegativeReserve(Llocal/lito381/server/ClientSession;Llocal/lito381/server/NativeAreaMaterialAction$Context;Llocal/lito381/server/NativeSourceTargetNegativeLife$Proof;)Llocal/lito381/server/NativePlayerVitalCompletions$Receipt;",
    "status": "SAME_PATHS",
    "detail": "path multisets equal (130)"
  }
]
```

</details>
<details>
<summary>完整正規化差異</summary>

```diff
--- staged/NativePlayerVitalCompletions/drain-390f6c66.txt
+++ candidate/NativePlayerVitalCompletions/drain-390f6c66.txt
@@ -8,19 +8,19 @@
-TRY 22:[83:java/lang/Throwable  ]
-TRY 24:[83:java/lang/Throwable  ]
-TRY 27:[83:java/lang/Throwable  ]
-TRY 29:[53:java/lang/Throwable  , 83:java/lang/Throwable  ]
-TRY 30:[53:java/lang/Throwable  , 83:java/lang/Throwable  ]
-TRY 31:[53:java/lang/Throwable  , 83:java/lang/Throwable  ]
-TRY 36:[53:java/lang/Throwable  , 83:java/lang/Throwable  ]
-TRY 45:[53:java/lang/Throwable  , 83:java/lang/Throwable  ]
-TRY 46:[53:java/lang/Throwable  , 83:java/lang/Throwable  ]
-TRY 47:[53:java/lang/Throwable  , 83:java/lang/Throwable  ]
-TRY 48:[53:java/lang/Throwable  , 83:java/lang/Throwable  ]
-TRY 51:[83:java/lang/Throwable  ]
-TRY 55:[83:java/lang/Throwable  ]
-TRY 57:[83:java/lang/Throwable  ]
-TRY 59:[83:java/lang/Throwable  ]
-TRY 63:[83:java/lang/Throwable  ]
-TRY 64:[83:java/lang/Throwable  ]
-TRY 69:[71:java/lang/Throwable  , 83:java/lang/Throwable  ]
-TRY 74:[83:java/lang/Throwable  ]
+TRY 22:[85:java/lang/Throwable  ]
+TRY 24:[85:java/lang/Throwable  ]
+TRY 27:[85:java/lang/Throwable  ]
+TRY 29:[53:java/lang/Throwable  , 85:java/lang/Throwable  ]
+TRY 30:[53:java/lang/Throwable  , 85:java/lang/Throwable  ]
+TRY 31:[53:java/lang/Throwable  , 85:java/lang/Throwable  ]
+TRY 36:[53:java/lang/Throwable  , 85:java/lang/Throwable  ]
+TRY 45:[53:java/lang/Throwable  , 85:java/lang/Throwable  ]
+TRY 46:[53:java/lang/Throwable  , 85:java/lang/Throwable  ]
+TRY 47:[53:java/lang/Throwable  , 85:java/lang/Throwable  ]
+TRY 48:[53:java/lang/Throwable  , 85:java/lang/Throwable  ]
+TRY 51:[85:java/lang/Throwable  ]
+TRY 55:[85:java/lang/Throwable  ]
+TRY 57:[85:java/lang/Throwable  ]
+TRY 59:[85:java/lang/Throwable  ]
+TRY 63:[85:java/lang/Throwable  ]
+TRY 64:[85:java/lang/Throwable  ]
+TRY 69:[71:java/lang/Throwable  , 85:java/lang/Throwable  ]
+TRY 74:[85:java/lang/Throwable  ]
@@ -88 +88 @@
-GOTO L78
+GOTO L80
@@ -104 +104,3 @@
-IFNULL L22
+IFNULL L79
+GOTO L80
+GOTO L22
@@ -109 +111 @@
-GOTO L90
+GOTO L92
@@ -118 +120 @@
-IFNULL L98
+IFNULL L99
@@ -122,2 +124 @@
-IFEQ L97
-GOTO L98
+IFNE L99
@@ -128 +129 @@
-IFEQ L10
+IFEQ L104
@@ -129,0 +131 @@
+GOTO L10
--- staged/NativePlayerVitalCompletions/nativeSourceMaterialPairReserve-11dbd7e.txt
+++ candidate/NativePlayerVitalCompletions/nativeSourceMaterialPairReserve-11dbd7e.txt
@@ -8,23 +8,23 @@
-TRY 41:[102:java/lang/Throwable  ]
-TRY 44:[102:java/lang/Throwable  ]
-TRY 45:[102:java/lang/Throwable  ]
-TRY 46:[102:java/lang/Throwable  ]
-TRY 50:[102:java/lang/Throwable  ]
-TRY 53:[102:java/lang/Throwable  ]
-TRY 54:[102:java/lang/Throwable  ]
-TRY 61:[102:java/lang/Throwable  ]
-TRY 63:[102:java/lang/Throwable  ]
-TRY 64:[102:java/lang/Throwable  ]
-TRY 66:[102:java/lang/Throwable  ]
-TRY 73:[102:java/lang/Throwable  ]
-TRY 77:[102:java/lang/Throwable  ]
-TRY 80:[102:java/lang/Throwable  ]
-TRY 81:[90:java/lang/Throwable  , 102:java/lang/Throwable  ]
-TRY 86:[90:java/lang/Throwable  , 102:java/lang/Throwable  ]
-TRY 88:[102:java/lang/Throwable  ]
-TRY 93:[102:java/lang/Throwable  ]
-TRY 96:[102:java/lang/Throwable  ]
-TRY 98:[102:java/lang/Throwable  ]
-TRY 99:[102:java/lang/Throwable  ]
-TRY 101:[102:java/lang/Throwable  ]
-TRY 104:[102:java/lang/Throwable  ]
+TRY 39:[98:java/lang/Throwable  ]
+TRY 41:[98:java/lang/Throwable  ]
+TRY 42:[98:java/lang/Throwable  ]
+TRY 43:[98:java/lang/Throwable  ]
+TRY 47:[98:java/lang/Throwable  ]
+TRY 50:[98:java/lang/Throwable  ]
+TRY 51:[98:java/lang/Throwable  ]
+TRY 58:[98:java/lang/Throwable  ]
+TRY 60:[98:java/lang/Throwable  ]
+TRY 61:[98:java/lang/Throwable  ]
+TRY 63:[98:java/lang/Throwable  ]
+TRY 69:[98:java/lang/Throwable  ]
+TRY 73:[98:java/lang/Throwable  ]
+TRY 76:[98:java/lang/Throwable  ]
+TRY 77:[86:java/lang/Throwable  , 98:java/lang/Throwable  ]
+TRY 82:[86:java/lang/Throwable  , 98:java/lang/Throwable  ]
+TRY 84:[98:java/lang/Throwable  ]
+TRY 89:[98:java/lang/Throwable  ]
+TRY 92:[98:java/lang/Throwable  ]
+TRY 94:[98:java/lang/Throwable  ]
+TRY 95:[98:java/lang/Throwable  ]
+TRY 97:[98:java/lang/Throwable  ]
+TRY 100:[98:java/lang/Throwable  ]
@@ -63,0 +64 @@
+DUP
@@ -65,3 +65,0 @@
-ALOAD 4
-DUP
-ASTORE 5
@@ -73,2 +71 @@
-IFNE L44
-GOTO L71
+IFEQ L67
@@ -78,2 +75,2 @@
-ASTORE 6
-ALOAD 6
+ASTORE 5
+ALOAD 5
@@ -82 +79 @@
-IFNE L56
+IFNE L53
@@ -86 +83 @@
-IFEQ L58
+IFEQ L55
@@ -88 +85 @@
-GOTO L59
+GOTO L56
@@ -90,2 +87,2 @@
-ISTORE 7
-ALOAD 6
+ISTORE 6
+ALOAD 5
@@ -93 +90 @@
-IFEQ L65
+IFEQ L62
@@ -98,2 +94,0 @@
-IFEQ L75
-ILOAD 7
@@ -101 +96,2 @@
-GOTO L75
+ILOAD 6
+IFNE L71
@@ -103 +99 @@
-ALOAD 5
+ALOAD 4
@@ -118 +114 @@
-ALOAD 5
+ALOAD 4
@@ -121 +117 @@
-ASTORE 8
+ASTORE 7
@@ -130,0 +127,5 @@
+ALOAD 7
+ATHROW
+ASTORE 8
+ALOAD 4
+MONITOREXIT
@@ -133,5 +133,0 @@
-ASTORE 9
-ALOAD 5
-MONITOREXIT
-ALOAD 9
-ATHROW
--- staged/NativePlayerVitalCompletions/nativeSourceTargetNegativeReserve-f2f4d22b.txt
+++ candidate/NativePlayerVitalCompletions/nativeSourceTargetNegativeReserve-f2f4d22b.txt
@@ -8,23 +8,23 @@
-TRY 45:[106:java/lang/Throwable  ]
-TRY 48:[106:java/lang/Throwable  ]
-TRY 49:[106:java/lang/Throwable  ]
-TRY 50:[106:java/lang/Throwable  ]
-TRY 54:[106:java/lang/Throwable  ]
-TRY 57:[106:java/lang/Throwable  ]
-TRY 58:[106:java/lang/Throwable  ]
-TRY 65:[106:java/lang/Throwable  ]
-TRY 67:[106:java/lang/Throwable  ]
-TRY 68:[106:java/lang/Throwable  ]
-TRY 70:[106:java/lang/Throwable  ]
-TRY 77:[106:java/lang/Throwable  ]
-TRY 81:[106:java/lang/Throwable  ]
-TRY 84:[106:java/lang/Throwable  ]
-TRY 85:[94:java/lang/Throwable  , 106:java/lang/Throwable  ]
-TRY 90:[94:java/lang/Throwable  , 106:java/lang/Throwable  ]
-TRY 92:[106:java/lang/Throwable  ]
-TRY 97:[106:java/lang/Throwable  ]
-TRY 100:[106:java/lang/Throwable  ]
-TRY 102:[106:java/lang/Throwable  ]
-TRY 103:[106:java/lang/Throwable  ]
-TRY 105:[106:java/lang/Throwable  ]
-TRY 108:[106:java/lang/Throwable  ]
+TRY 43:[102:java/lang/Throwable  ]
+TRY 45:[102:java/lang/Throwable  ]
+TRY 46:[102:java/lang/Throwable  ]
+TRY 47:[102:java/lang/Throwable  ]
+TRY 51:[102:java/lang/Throwable  ]
+TRY 54:[102:java/lang/Throwable  ]
+TRY 55:[102:java/lang/Throwable  ]
+TRY 62:[102:java/lang/Throwable  ]
+TRY 64:[102:java/lang/Throwable  ]
+TRY 65:[102:java/lang/Throwable  ]
+TRY 67:[102:java/lang/Throwable  ]
+TRY 73:[102:java/lang/Throwable  ]
+TRY 77:[102:java/lang/Throwable  ]
+TRY 80:[102:java/lang/Throwable  ]
+TRY 81:[90:java/lang/Throwable  , 102:java/lang/Throwable  ]
+TRY 86:[90:java/lang/Throwable  , 102:java/lang/Throwable  ]
+TRY 88:[102:java/lang/Throwable  ]
+TRY 93:[102:java/lang/Throwable  ]
+TRY 96:[102:java/lang/Throwable  ]
+TRY 98:[102:java/lang/Throwable  ]
+TRY 99:[102:java/lang/Throwable  ]
+TRY 101:[102:java/lang/Throwable  ]
+TRY 104:[102:java/lang/Throwable  ]
@@ -66,0 +67 @@
+DUP
@@ -68,3 +68,0 @@
-ALOAD 5
-DUP
-ASTORE 6
@@ -77,2 +75 @@
-IFNE L48
-GOTO L75
+IFEQ L71
@@ -82,2 +79,2 @@
-ASTORE 7
-ALOAD 7
+ASTORE 6
+ALOAD 6
@@ -86 +83 @@
-IFNE L60
+IFNE L57
@@ -90 +87 @@
-IFEQ L62
+IFEQ L59
@@ -92 +89 @@
-GOTO L63
+GOTO L60
@@ -94,2 +91,2 @@
-ISTORE 8
-ALOAD 7
+ISTORE 7
+ALOAD 6
@@ -97 +94 @@
-IFEQ L69
+IFEQ L66
@@ -102,2 +98,0 @@
-IFEQ L79
-ILOAD 8
@@ -105 +100,2 @@
-GOTO L79
+ILOAD 7
+IFNE L75
@@ -107 +103 @@
-ALOAD 6
+ALOAD 5
@@ -122 +118 @@
-ALOAD 6
+ALOAD 5
@@ -125 +121 @@
-ASTORE 9
+ASTORE 8
@@ -134,0 +131,5 @@
+ALOAD 8
+ATHROW
+ASTORE 9
+ALOAD 5
+MONITOREXIT
@@ -137,5 +137,0 @@
-ASTORE 10
-ALOAD 6
-MONITOREXIT
-ALOAD 10
-ATHROW
--- staged/NativePlayerVitalCompletions/reserve-f5afb153.txt
+++ candidate/NativePlayerVitalCompletions/reserve-f5afb153.txt
@@ -8,22 +8,22 @@
-TRY 27:[84:java/lang/Throwable  ]
-TRY 28:[84:java/lang/Throwable  ]
-TRY 29:[84:java/lang/Throwable  ]
-TRY 33:[84:java/lang/Throwable  ]
-TRY 36:[84:java/lang/Throwable  ]
-TRY 37:[84:java/lang/Throwable  ]
-TRY 44:[84:java/lang/Throwable  ]
-TRY 46:[84:java/lang/Throwable  ]
-TRY 47:[84:java/lang/Throwable  ]
-TRY 49:[84:java/lang/Throwable  ]
-TRY 55:[84:java/lang/Throwable  ]
-TRY 59:[84:java/lang/Throwable  ]
-TRY 62:[84:java/lang/Throwable  ]
-TRY 63:[72:java/lang/Throwable  , 84:java/lang/Throwable  ]
-TRY 68:[72:java/lang/Throwable  , 84:java/lang/Throwable  ]
-TRY 70:[84:java/lang/Throwable  ]
-TRY 75:[84:java/lang/Throwable  ]
-TRY 78:[84:java/lang/Throwable  ]
-TRY 80:[84:java/lang/Throwable  ]
-TRY 81:[84:java/lang/Throwable  ]
-TRY 83:[84:java/lang/Throwable  ]
-TRY 86:[84:java/lang/Throwable  ]
+TRY 25:[82:java/lang/Throwable  ]
+TRY 26:[82:java/lang/Throwable  ]
+TRY 27:[82:java/lang/Throwable  ]
+TRY 31:[82:java/lang/Throwable  ]
+TRY 34:[82:java/lang/Throwable  ]
+TRY 35:[82:java/lang/Throwable  ]
+TRY 42:[82:java/lang/Throwable  ]
+TRY 44:[82:java/lang/Throwable  ]
+TRY 45:[82:java/lang/Throwable  ]
+TRY 47:[82:java/lang/Throwable  ]
+TRY 53:[82:java/lang/Throwable  ]
+TRY 57:[82:java/lang/Throwable  ]
+TRY 60:[82:java/lang/Throwable  ]
+TRY 61:[70:java/lang/Throwable  , 82:java/lang/Throwable  ]
+TRY 66:[70:java/lang/Throwable  , 82:java/lang/Throwable  ]
+TRY 68:[82:java/lang/Throwable  ]
+TRY 73:[82:java/lang/Throwable  ]
+TRY 76:[82:java/lang/Throwable  ]
+TRY 78:[82:java/lang/Throwable  ]
+TRY 79:[82:java/lang/Throwable  ]
+TRY 81:[82:java/lang/Throwable  ]
+TRY 84:[82:java/lang/Throwable  ]
@@ -51,0 +52 @@
+DUP
@@ -53,3 +53,0 @@
-ALOAD 3
-DUP
-ASTORE 4
@@ -60,2 +58,2 @@
-ASTORE 5
-ALOAD 5
+ASTORE 4
+ALOAD 4
@@ -64 +62 @@
-IFNE L39
+IFNE L37
@@ -68 +66 @@
-IFEQ L41
+IFEQ L39
@@ -70 +68 @@
-GOTO L42
+GOTO L40
@@ -72,2 +70,2 @@
-ISTORE 6
-ALOAD 5
+ISTORE 5
+ALOAD 4
@@ -75 +73 @@
-IFEQ L48
+IFEQ L46
@@ -80,3 +78,3 @@
-IFEQ L57
-ILOAD 6
-IFNE L57
+IFEQ L55
+ILOAD 5
+IFNE L55
@@ -84 +82 @@
-ALOAD 4
+ALOAD 3
@@ -99 +97 @@
-ALOAD 4
+ALOAD 3
@@ -102 +100 @@
-ASTORE 7
+ASTORE 6
@@ -111,0 +110,5 @@
+ALOAD 6
+ATHROW
+ASTORE 7
+ALOAD 3
+MONITOREXIT
@@ -114,5 +116,0 @@
-ASTORE 8
-ALOAD 4
-MONITOREXIT
-ALOAD 8
-ATHROW
```

</details>

### NativePlayerVitalCompletions$Receipt — SAME_PATHS

<details>
<summary>全部方法診斷</summary>

```json
[
  {
    "method": "run(Ljava/lang/Runnable;)V",
    "status": "SAME_PATHS",
    "detail": "path multisets equal (245)"
  }
]
```

</details>
<details>
<summary>完整正規化差異</summary>

```diff
--- staged/NativePlayerVitalCompletions$Receipt/release-9b75.txt
+++ candidate/NativePlayerVitalCompletions$Receipt/release-9b75.txt
@@ -8,11 +8,11 @@
-TRY 29:[49:java/lang/Throwable  ]
-TRY 31:[49:java/lang/Throwable  ]
-TRY 35:[49:java/lang/Throwable  ]
-TRY 37:[49:java/lang/Throwable  ]
-TRY 40:[49:java/lang/Throwable  ]
-TRY 41:[49:java/lang/Throwable  ]
-TRY 43:[49:java/lang/Throwable  ]
-TRY 44:[49:java/lang/Throwable  ]
-TRY 45:[49:java/lang/Throwable  ]
-TRY 47:[49:java/lang/Throwable  ]
-TRY 51:[49:java/lang/Throwable  ]
+TRY 27:[47:java/lang/Throwable  ]
+TRY 29:[47:java/lang/Throwable  ]
+TRY 33:[47:java/lang/Throwable  ]
+TRY 35:[47:java/lang/Throwable  ]
+TRY 38:[47:java/lang/Throwable  ]
+TRY 39:[47:java/lang/Throwable  ]
+TRY 41:[47:java/lang/Throwable  ]
+TRY 42:[47:java/lang/Throwable  ]
+TRY 43:[47:java/lang/Throwable  ]
+TRY 45:[47:java/lang/Throwable  ]
+TRY 49:[47:java/lang/Throwable  ]
@@ -38 +38 @@
-IFNULL L54
+IFNULL L52
@@ -41,0 +42 @@
+DUP
@@ -43,3 +43,0 @@
-ALOAD 1
-DUP
-ASTORE 2
@@ -55 +53 @@
-IFGE L42
+IFGE L40
@@ -64,0 +63,6 @@
+ALOAD 1
+MONITOREXIT
+GOTO L52
+ASTORE 2
+ALOAD 1
+MONITOREXIT
@@ -66,6 +69,0 @@
-MONITOREXIT
-GOTO L54
-ASTORE 3
-ALOAD 2
-MONITOREXIT
-ALOAD 3
--- staged/NativePlayerVitalCompletions$Receipt/run-a4fc3973.txt
+++ candidate/NativePlayerVitalCompletions$Receipt/run-a4fc3973.txt
@@ -8,19 +8,19 @@
-TRY 18:[49:java/lang/Throwable  ]
-TRY 21:[49:java/lang/Throwable  ]
-TRY 22:[49:java/lang/Throwable  ]
-TRY 26:[43:java/lang/Throwable  ]
-TRY 28:[43:java/lang/Throwable  ]
-TRY 30:[43:java/lang/Throwable  ]
-TRY 33:[43:java/lang/Throwable  ]
-TRY 34:[43:java/lang/Throwable  ]
-TRY 36:[43:java/lang/Throwable  ]
-TRY 38:[43:java/lang/Throwable  ]
-TRY 39:[43:java/lang/Throwable  ]
-TRY 53:[70:java/lang/Throwable  ]
-TRY 55:[70:java/lang/Throwable  ]
-TRY 57:[70:java/lang/Throwable  ]
-TRY 60:[70:java/lang/Throwable  ]
-TRY 61:[70:java/lang/Throwable  ]
-TRY 63:[70:java/lang/Throwable  ]
-TRY 65:[70:java/lang/Throwable  ]
-TRY 66:[70:java/lang/Throwable  ]
+TRY 16:[47:java/lang/Throwable  ]
+TRY 19:[47:java/lang/Throwable  ]
+TRY 20:[47:java/lang/Throwable  ]
+TRY 24:[41:java/lang/Throwable  ]
+TRY 26:[41:java/lang/Throwable  ]
+TRY 28:[41:java/lang/Throwable  ]
+TRY 31:[41:java/lang/Throwable  ]
+TRY 32:[41:java/lang/Throwable  ]
+TRY 34:[41:java/lang/Throwable  ]
+TRY 36:[41:java/lang/Throwable  ]
+TRY 37:[41:java/lang/Throwable  ]
+TRY 51:[68:java/lang/Throwable  ]
+TRY 53:[68:java/lang/Throwable  ]
+TRY 55:[68:java/lang/Throwable  ]
+TRY 58:[68:java/lang/Throwable  ]
+TRY 59:[68:java/lang/Throwable  ]
+TRY 61:[68:java/lang/Throwable  ]
+TRY 63:[68:java/lang/Throwable  ]
+TRY 64:[68:java/lang/Throwable  ]
@@ -35 +34,0 @@
-DUP
@@ -37 +35,0 @@
-ASTORE 3
@@ -39 +37 @@
-IFNULL L17
+IFNULL L15
@@ -51 +49 @@
-IFNULL L40
+IFNULL L38
@@ -56 +54 @@
-IF_ACMPEQ L35
+IF_ACMPEQ L33
@@ -64 +62 @@
-IFEQ L40
+IFEQ L38
@@ -69,2 +67,2 @@
-GOTO L48
-ASTORE 4
+GOTO L46
+ASTORE 3
@@ -73 +71 @@
-ALOAD 4
+ALOAD 3
@@ -75,2 +73,2 @@
-GOTO L77
-ASTORE 5
+GOTO L75
+ASTORE 4
@@ -78 +76 @@
-IFNULL L67
+IFNULL L65
@@ -83 +81 @@
-IF_ACMPEQ L62
+IF_ACMPEQ L60
@@ -91 +89 @@
-IFEQ L67
+IFEQ L65
@@ -96,2 +94,2 @@
-GOTO L75
-ASTORE 6
+GOTO L73
+ASTORE 5
@@ -100,2 +97,0 @@
-ALOAD 6
-ATHROW
@@ -103,0 +100,2 @@
+ALOAD 4
+ATHROW
```

</details>

### NativeProcHealingCompanion — DIFF

```json
{
  "where": "method",
  "member": "graphics(Llocal/lito381/server/GameHub;Llocal/lito381/server/NativeProcHealingCompanion$Plan;I)V",
  "insnIndex": 106,
  "detail": "left=IFEQ L141 right=IFEQ L140; paths rejected: normal control-flow cycle"
}
```
<details>
<summary>全部方法診斷</summary>

```json
[
  {
    "method": "graphics(Llocal/lito381/server/GameHub;Llocal/lito381/server/NativeProcHealingCompanion$Plan;I)V",
    "status": "DIFF",
    "detail": "method graphics(Llocal/lito381/server/GameHub;Llocal/lito381/server/NativeProcHealingCompanion$Plan;I)V insn[106]: left=IFEQ L141 right=IFEQ L140; paths rejected: normal control-flow cycle"
  },
  {
    "method": "nativeProcSceneOriginalWithTarget(Llocal/lito381/server/GameHub;Llocal/lito381/server/NativeProcHealingCompanion$Plan;ZLjava/util/function/Function;)Ljava/lang/Object;",
    "status": "DIFF",
    "detail": "method nativeProcSceneOriginalWithTarget(Llocal/lito381/server/GameHub;Llocal/lito381/server/NativeProcHealingCompanion$Plan;ZLjava/util/function/Function;)Ljava/lang/Object;: left=22:[281:java/lang/Throwable  ] right=20:[263:java/lang/Throwable  ]; paths rejected: normal control-flow cycle"
  }
]
```

</details>
<details>
<summary>完整正規化差異</summary>

```diff
--- staged/NativeProcHealingCompanion/apply-f0390597.txt
+++ candidate/NativeProcHealingCompanion/apply-f0390597.txt
@@ -62 +61,0 @@
-ALOAD 6
@@ -70 +68,0 @@
-DUP
@@ -71,0 +70,2 @@
+ALOAD 6
+ALOAD 7
--- staged/NativeProcHealingCompanion/enqueue-fd1c1fa0.txt
+++ candidate/NativeProcHealingCompanion/enqueue-fd1c1fa0.txt
@@ -8,13 +8,13 @@
-TRY 46:[81:java/lang/Throwable  ]
-TRY 50:[81:java/lang/Throwable  ]
-TRY 53:[81:java/lang/Throwable  ]
-TRY 57:[81:java/lang/Throwable  ]
-TRY 59:[81:java/lang/Throwable  ]
-TRY 63:[81:java/lang/Throwable  ]
-TRY 67:[81:java/lang/Throwable  ]
-TRY 69:[81:java/lang/Throwable  ]
-TRY 70:[81:java/lang/Throwable  ]
-TRY 72:[81:java/lang/Throwable  ]
-TRY 77:[81:java/lang/Throwable  ]
-TRY 79:[81:java/lang/Throwable  ]
-TRY 83:[81:java/lang/Throwable  ]
+TRY 44:[79:java/lang/Throwable  ]
+TRY 48:[79:java/lang/Throwable  ]
+TRY 51:[79:java/lang/Throwable  ]
+TRY 55:[79:java/lang/Throwable  ]
+TRY 57:[79:java/lang/Throwable  ]
+TRY 61:[79:java/lang/Throwable  ]
+TRY 65:[79:java/lang/Throwable  ]
+TRY 67:[79:java/lang/Throwable  ]
+TRY 68:[79:java/lang/Throwable  ]
+TRY 70:[79:java/lang/Throwable  ]
+TRY 75:[79:java/lang/Throwable  ]
+TRY 77:[79:java/lang/Throwable  ]
+TRY 81:[79:java/lang/Throwable  ]
@@ -58,0 +59 @@
+DUP
@@ -60,3 +60,0 @@
-ALOAD 7
-DUP
-ASTORE 8
@@ -68 +66 @@
-IFNE L52
+IFNE L50
@@ -70 +68 @@
-ALOAD 8
+ALOAD 7
@@ -75 +73 @@
-IFEQ L78
+IFEQ L76
@@ -79 +77 @@
-ASTORE 9
+ASTORE 8
@@ -85 +83,2 @@
-ASTORE 10
+ASTORE 9
+ALOAD 8
@@ -87 +85,0 @@
-ALOAD 10
@@ -89 +87 @@
-ALOAD 9
+ALOAD 8
@@ -92 +90 @@
-ALOAD 9
+ALOAD 8
@@ -94,0 +93 @@
+ALOAD 8
@@ -96 +94,0 @@
-ALOAD 10
@@ -99 +97 @@
-ALOAD 8
+ALOAD 7
@@ -101,3 +99,3 @@
-GOTO L86
-ASTORE 11
-ALOAD 8
+GOTO L84
+ASTORE 10
+ALOAD 7
@@ -105 +103 @@
-ALOAD 11
+ALOAD 10
--- staged/NativeProcHealingCompanion/graphics-cfdb57c7.txt
+++ candidate/NativeProcHealingCompanion/graphics-cfdb57c7.txt
@@ -115 +115 @@
-IFEQ L141
+IFEQ L140
@@ -125 +125 @@
-IFEQ L141
+IFEQ L140
@@ -134 +134 @@
-GOTO L141
+GOTO L140
@@ -138 +138 @@
-IFEQ L140
+IFEQ L139
@@ -141 +140,0 @@
-CHECKCAST [B
--- staged/NativeProcHealingCompanion/nativeProcSceneOriginalWithTarget-c938d738.txt
+++ candidate/NativeProcHealingCompanion/nativeProcSceneOriginalWithTarget-c938d738.txt
@@ -8,89 +8,83 @@
-TRY 22:[281:java/lang/Throwable  ]
-TRY 23:[281:java/lang/Throwable  ]
-TRY 29:[281:java/lang/Throwable  ]
-TRY 30:[281:java/lang/Throwable  ]
-TRY 42:[281:java/lang/Throwable  ]
-TRY 45:[281:java/lang/Throwable  ]
-TRY 50:[281:java/lang/Throwable  ]
-TRY 59:[281:java/lang/Throwable  ]
-TRY 65:[276:java/lang/Throwable  , 281:java/lang/Throwable  ]
-TRY 67:[271:java/lang/Throwable  , 276:java/lang/Throwable  , 281:java/lang/Throwable  ]
-TRY 69:[271:java/lang/Throwable  , 276:java/lang/Throwable  , 281:java/lang/Throwable  ]
-TRY 74:[271:java/lang/Throwable  , 276:java/lang/Throwable  , 281:java/lang/Throwable  ]
-TRY 75:[271:java/lang/Throwable  , 276:java/lang/Throwable  , 281:java/lang/Throwable  ]
-TRY 79:[271:java/lang/Throwable  , 276:java/lang/Throwable  , 281:java/lang/Throwable  ]
-TRY 81:[276:java/lang/Throwable  , 281:java/lang/Throwable  ]
-TRY 83:[281:java/lang/Throwable  ]
-TRY 88:[271:java/lang/Throwable  , 276:java/lang/Throwable  , 281:java/lang/Throwable  ]
-TRY 90:[271:java/lang/Throwable  , 276:java/lang/Throwable  , 281:java/lang/Throwable  ]
-TRY 95:[271:java/lang/Throwable  , 276:java/lang/Throwable  , 281:java/lang/Throwable  ]
-TRY 96:[271:java/lang/Throwable  , 276:java/lang/Throwable  , 281:java/lang/Throwable  ]
-TRY 99:[271:java/lang/Throwable  , 276:java/lang/Throwable  , 281:java/lang/Throwable  ]
-TRY 101:[271:java/lang/Throwable  , 276:java/lang/Throwable  , 281:java/lang/Throwable  ]
-TRY 102:[271:java/lang/Throwable  , 276:java/lang/Throwable  , 281:java/lang/Throwable  ]
-TRY 106:[271:java/lang/Throwable  , 276:java/lang/Throwable  , 281:java/lang/Throwable  ]
-TRY 108:[276:java/lang/Throwable  , 281:java/lang/Throwable  ]
-TRY 110:[281:java/lang/Throwable  ]
-TRY 114:[271:java/lang/Throwable  , 276:java/lang/Throwable  , 281:java/lang/Throwable  ]
-TRY 115:[271:java/lang/Throwable  , 276:java/lang/Throwable  , 281:java/lang/Throwable  ]
-TRY 122:[271:java/lang/Throwable  , 276:java/lang/Throwable  , 281:java/lang/Throwable  ]
-TRY 125:[266:java/lang/Throwable  , 271:java/lang/Throwable  , 276:java/lang/Throwable  , 281:java/lang/Throwable  ]
-TRY 126:[266:java/lang/Throwable  , 271:java/lang/Throwable  , 276:java/lang/Throwable  , 281:java/lang/Throwable  ]
-TRY 129:[266:java/lang/Throwable  , 271:java/lang/Throwable  , 276:java/lang/Throwable  , 281:java/lang/Throwable  ]
-TRY 130:[266:java/lang/Throwable  , 271:java/lang/Throwable  , 276:java/lang/Throwable  , 281:java/lang/Throwable  ]
-TRY 131:[266:java/lang/Throwable  , 271:java/lang/Throwable  , 276:java/lang/Throwable  , 281:java/lang/Throwable  ]
-TRY 133:[266:java/lang/Throwable  , 271:java/lang/Throwable  , 276:java/lang/Throwable  , 281:java/lang/Throwable  ]
-TRY 139:[266:java/lang/Throwable  , 271:java/lang/Throwable  , 276:java/lang/Throwable  , 281:java/lang/Throwable  ]
-TRY 142:[266:java/lang/Throwable  , 271:java/lang/Throwable  , 276:java/lang/Throwable  , 281:java/lang/Throwable  ]
-TRY 145:[266:java/lang/Throwable  , 271:java/lang/Throwable  , 276:java/lang/Throwable  , 281:java/lang/Throwable  ]
-TRY 148:[266:java/lang/Throwable  , 271:java/lang/Throwable  , 276:java/lang/Throwable  , 281:java/lang/Throwable  ]
-TRY 151:[266:java/lang/Throwable  , 271:java/lang/Throwable  , 276:java/lang/Throwable  , 281:java/lang/Throwable  ]
-TRY 154:[266:java/lang/Throwable  , 271:java/lang/Throwable  , 276:java/lang/Throwable  , 281:java/lang/Throwable  ]
-TRY 155:[266:java/lang/Throwable  , 271:java/lang/Throwable  , 276:java/lang/Throwable  , 281:java/lang/Throwable  ]
-TRY 156:[266:java/lang/Throwable  , 271:java/lang/Throwable  , 276:java/lang/Throwable  , 281:java/lang/Throwable  ]
-TRY 158:[266:java/lang/Throwable  , 271:java/lang/Throwable  , 276:java/lang/Throwable  , 281:java/lang/Throwable  ]
-TRY 169:[266:java/lang/Throwable  , 271:java/lang/Throwable  , 276:java/lang/Throwable  , 281:java/lang/Throwable  ]
-TRY 171:[271:java/lang/Throwable  , 276:java/lang/Throwable  , 281:java/lang/Throwable  ]
-TRY 173:[276:java/lang/Throwable  , 281:java/lang/Throwable  ]
-TRY 175:[281:java/lang/Throwable  ]
-TRY 182:[266:java/lang/Throwable  , 271:java/lang/Throwable  , 276:java/lang/Throwable  , 281:java/lang/Throwable  ]
-TRY 185:[261:java/lang/Throwable  , 266:java/lang/Throwable  , 271:java/lang/Throwable  , 276:java/lang/Throwable  , 281:java/lang/Throwable  ]
-TRY 186:[261:java/lang/Throwable  , 266:java/lang/Throwable  , 271:java/lang/Throwable  , 276:java/lang/Throwable  , 281:java/lang/Throwable  ]
-TRY 193:[261:java/lang/Throwable  , 266:java/lang/Throwable  , 271:java/lang/Throwable  , 276:java/lang/Throwable  , 281:java/lang/Throwable  ]
-TRY 195:[256:java/lang/Throwable  , 261:java/lang/Throwable  , 266:java/lang/Throwable  , 271:java/lang/Throwable  , 276:java/lang/Throwable  , 281:java/lang/Throwable  ]
-TRY 196:[256:java/lang/Throwable  , 261:java/lang/Throwable  , 266:java/lang/Throwable  , 271:java/lang/Throwable  , 276:java/lang/Throwable  , 281:java/lang/Throwable  ]
-TRY 199:[256:java/lang/Throwable  , 261:java/lang/Throwable  , 266:java/lang/Throwable  , 271:java/lang/Throwable  , 276:java/lang/Throwable  , 281:java/lang/Throwable  ]
-TRY 202:[256:java/lang/Throwable  , 261:java/lang/Throwable  , 266:java/lang/Throwable  , 271:java/lang/Throwable  , 276:java/lang/Throwable  , 281:java/lang/Throwable  ]
-TRY 205:[256:java/lang/Throwable  , 261:java/lang/Throwable  , 266:java/lang/Throwable  , 271:java/lang/Throwable  , 276:java/lang/Throwable  , 281:java/lang/Throwable  ]
-TRY 207:[256:java/lang/Throwable  , 261:java/lang/Throwable  , 266:java/lang/Throwable  , 271:java/lang/Throwable  , 276:java/lang/Throwable  , 281:java/lang/Throwable  ]
-TRY 211:[256:java/lang/Throwable  , 261:java/lang/Throwable  , 266:java/lang/Throwable  , 271:java/lang/Throwable  , 276:java/lang/Throwable  , 281:java/lang/Throwable  ]
-TRY 213:[256:java/lang/Throwable  , 261:java/lang/Throwable  , 266:java/lang/Throwable  , 271:java/lang/Throwable  , 276:java/lang/Throwable  , 281:java/lang/Throwable  ]
-TRY 215:[256:java/lang/Throwable  , 261:java/lang/Throwable  , 266:java/lang/Throwable  , 271:java/lang/Throwable  , 276:java/lang/Throwable  , 281:java/lang/Throwable  ]
-TRY 219:[256:java/lang/Throwable  , 261:java/lang/Throwable  , 266:java/lang/Throwable  , 271:java/lang/Throwable  , 276:java/lang/Throwable  , 281:java/lang/Throwable  ]
-TRY 221:[261:java/lang/Throwable  , 266:java/lang/Throwable  , 271:java/lang/Throwable  , 276:java/lang/Throwable  , 281:java/lang/Throwable  ]
-TRY 223:[266:java/lang/Throwable  , 271:java/lang/Throwable  , 276:java/lang/Throwable  , 281:java/lang/Throwable  ]
-TRY 225:[271:java/lang/Throwable  , 276:java/lang/Throwable  , 281:java/lang/Throwable  ]
-TRY 227:[276:java/lang/Throwable  , 281:java/lang/Throwable  ]
-TRY 229:[281:java/lang/Throwable  ]
-TRY 232:[256:java/lang/Throwable  , 261:java/lang/Throwable  , 266:java/lang/Throwable  , 271:java/lang/Throwable  , 276:java/lang/Throwable  , 281:java/lang/Throwable  ]
-TRY 237:[256:java/lang/Throwable  , 261:java/lang/Throwable  , 266:java/lang/Throwable  , 271:java/lang/Throwable  , 276:java/lang/Throwable  , 281:java/lang/Throwable  ]
-TRY 239:[256:java/lang/Throwable  , 261:java/lang/Throwable  , 266:java/lang/Throwable  , 271:java/lang/Throwable  , 276:java/lang/Throwable  , 281:java/lang/Throwable  ]
-TRY 241:[256:java/lang/Throwable  , 261:java/lang/Throwable  , 266:java/lang/Throwable  , 271:java/lang/Throwable  , 276:java/lang/Throwable  , 281:java/lang/Throwable  ]
-TRY 242:[256:java/lang/Throwable  , 261:java/lang/Throwable  , 266:java/lang/Throwable  , 271:java/lang/Throwable  , 276:java/lang/Throwable  , 281:java/lang/Throwable  ]
-TRY 244:[256:java/lang/Throwable  , 261:java/lang/Throwable  , 266:java/lang/Throwable  , 271:java/lang/Throwable  , 276:java/lang/Throwable  , 281:java/lang/Throwable  ]
-TRY 246:[261:java/lang/Throwable  , 266:java/lang/Throwable  , 271:java/lang/Throwable  , 276:java/lang/Throwable  , 281:java/lang/Throwable  ]
-TRY 248:[266:java/lang/Throwable  , 271:java/lang/Throwable  , 276:java/lang/Throwable  , 281:java/lang/Throwable  ]
-TRY 250:[271:java/lang/Throwable  , 276:java/lang/Throwable  , 281:java/lang/Throwable  ]
-TRY 252:[276:java/lang/Throwable  , 281:java/lang/Throwable  ]
-TRY 254:[281:java/lang/Throwable  ]
-TRY 258:[256:java/lang/Throwable  , 261:java/lang/Throwable  , 266:java/lang/Throwable  , 271:java/lang/Throwable  , 276:java/lang/Throwable  , 281:java/lang/Throwable  ]
-TRY 260:[261:java/lang/Throwable  , 266:java/lang/Throwable  , 271:java/lang/Throwable  , 276:java/lang/Throwable  , 281:java/lang/Throwable  ]
-TRY 263:[261:java/lang/Throwable  , 266:java/lang/Throwable  , 271:java/lang/Throwable  , 276:java/lang/Throwable  , 281:java/lang/Throwable  ]
-TRY 265:[266:java/lang/Throwable  , 271:java/lang/Throwable  , 276:java/lang/Throwable  , 281:java/lang/Throwable  ]
-TRY 268:[266:java/lang/Throwable  , 271:java/lang/Throwable  , 276:java/lang/Throwable  , 281:java/lang/Throwable  ]
-TRY 270:[271:java/lang/Throwable  , 276:java/lang/Throwable  , 281:java/lang/Throwable  ]
-TRY 273:[271:java/lang/Throwable  , 276:java/lang/Throwable  , 281:java/lang/Throwable  ]
-TRY 275:[276:java/lang/Throwable  , 281:java/lang/Throwable  ]
-TRY 278:[276:java/lang/Throwable  , 281:java/lang/Throwable  ]
-TRY 280:[281:java/lang/Throwable  ]
-TRY 283:[281:java/lang/Throwable  ]
+TRY 20:[263:java/lang/Throwable  ]
+TRY 21:[263:java/lang/Throwable  ]
+TRY 27:[263:java/lang/Throwable  ]
+TRY 28:[263:java/lang/Throwable  ]
+TRY 40:[263:java/lang/Throwable  ]
+TRY 43:[263:java/lang/Throwable  ]
+TRY 48:[263:java/lang/Throwable  ]
+TRY 55:[263:java/lang/Throwable  ]
+TRY 59:[258:java/lang/Throwable  , 263:java/lang/Throwable  ]
+TRY 61:[253:java/lang/Throwable  , 258:java/lang/Throwable  , 263:java/lang/Throwable  ]
+TRY 63:[253:java/lang/Throwable  , 258:java/lang/Throwable  , 263:java/lang/Throwable  ]
+TRY 68:[253:java/lang/Throwable  , 258:java/lang/Throwable  , 263:java/lang/Throwable  ]
+TRY 69:[253:java/lang/Throwable  , 258:java/lang/Throwable  , 263:java/lang/Throwable  ]
+TRY 73:[253:java/lang/Throwable  , 258:java/lang/Throwable  , 263:java/lang/Throwable  ]
+TRY 75:[258:java/lang/Throwable  , 263:java/lang/Throwable  ]
+TRY 77:[263:java/lang/Throwable  ]
+TRY 82:[253:java/lang/Throwable  , 258:java/lang/Throwable  , 263:java/lang/Throwable  ]
+TRY 84:[253:java/lang/Throwable  , 258:java/lang/Throwable  , 263:java/lang/Throwable  ]
+TRY 89:[253:java/lang/Throwable  , 258:java/lang/Throwable  , 263:java/lang/Throwable  ]
+TRY 90:[253:java/lang/Throwable  , 258:java/lang/Throwable  , 263:java/lang/Throwable  ]
+TRY 93:[253:java/lang/Throwable  , 258:java/lang/Throwable  , 263:java/lang/Throwable  ]
+TRY 95:[253:java/lang/Throwable  , 258:java/lang/Throwable  , 263:java/lang/Throwable  ]
+TRY 96:[253:java/lang/Throwable  , 258:java/lang/Throwable  , 263:java/lang/Throwable  ]
+TRY 100:[253:java/lang/Throwable  , 258:java/lang/Throwable  , 263:java/lang/Throwable  ]
+TRY 102:[258:java/lang/Throwable  , 263:java/lang/Throwable  ]
+TRY 104:[263:java/lang/Throwable  ]
+TRY 108:[253:java/lang/Throwable  , 258:java/lang/Throwable  , 263:java/lang/Throwable  ]
+TRY 109:[253:java/lang/Throwable  , 258:java/lang/Throwable  , 263:java/lang/Throwable  ]
+TRY 114:[253:java/lang/Throwable  , 258:java/lang/Throwable  , 263:java/lang/Throwable  ]
+TRY 117:[248:java/lang/Throwable  , 253:java/lang/Throwable  , 258:java/lang/Throwable  , 263:java/lang/Throwable  ]
+TRY 118:[248:java/lang/Throwable  , 253:java/lang/Throwable  , 258:java/lang/Throwable  , 263:java/lang/Throwable  ]
+TRY 121:[248:java/lang/Throwable  , 253:java/lang/Throwable  , 258:java/lang/Throwable  , 263:java/lang/Throwable  ]
+TRY 122:[248:java/lang/Throwable  , 253:java/lang/Throwable  , 258:java/lang/Throwable  , 263:java/lang/Throwable  ]
+TRY 124:[248:java/lang/Throwable  , 253:java/lang/Throwable  , 258:java/lang/Throwable  , 263:java/lang/Throwable  ]
+TRY 130:[248:java/lang/Throwable  , 253:java/lang/Throwable  , 258:java/lang/Throwable  , 263:java/lang/Throwable  ]
+TRY 133:[248:java/lang/Throwable  , 253:java/lang/Throwable  , 258:java/lang/Throwable  , 263:java/lang/Throwable  ]
+TRY 136:[248:java/lang/Throwable  , 253:java/lang/Throwable  , 258:java/lang/Throwable  , 263:java/lang/Throwable  ]
+TRY 139:[248:java/lang/Throwable  , 253:java/lang/Throwable  , 258:java/lang/Throwable  , 263:java/lang/Throwable  ]
+TRY 142:[248:java/lang/Throwable  , 253:java/lang/Throwable  , 258:java/lang/Throwable  , 263:java/lang/Throwable  ]
+TRY 145:[248:java/lang/Throwable  , 253:java/lang/Throwable  , 258:java/lang/Throwable  , 263:java/lang/Throwable  ]
+TRY 146:[248:java/lang/Throwable  , 253:java/lang/Throwable  , 258:java/lang/Throwable  , 263:java/lang/Throwable  ]
+TRY 148:[248:java/lang/Throwable  , 253:java/lang/Throwable  , 258:java/lang/Throwable  , 263:java/lang/Throwable  ]
+TRY 159:[248:java/lang/Throwable  , 253:java/lang/Throwable  , 258:java/lang/Throwable  , 263:java/lang/Throwable  ]
+TRY 161:[253:java/lang/Throwable  , 258:java/lang/Throwable  , 263:java/lang/Throwable  ]
+TRY 163:[258:java/lang/Throwable  , 263:java/lang/Throwable  ]
+TRY 165:[263:java/lang/Throwable  ]
+TRY 170:[248:java/lang/Throwable  , 253:java/lang/Throwable  , 258:java/lang/Throwable  , 263:java/lang/Throwable  ]
+TRY 173:[243:java/lang/Throwable  , 248:java/lang/Throwable  , 253:java/lang/Throwable  , 258:java/lang/Throwable  , 263:java/lang/Throwable  ]
+TRY 174:[243:java/lang/Throwable  , 248:java/lang/Throwable  , 253:java/lang/Throwable  , 258:java/lang/Throwable  , 263:java/lang/Throwable  ]
+TRY 179:[243:java/lang/Throwable  , 248:java/lang/Throwable  , 253:java/lang/Throwable  , 258:java/lang/Throwable  , 263:java/lang/Throwable  ]
+TRY 181:[238:java/lang/Throwable  , 243:java/lang/Throwable  , 248:java/lang/Throwable  , 253:java/lang/Throwable  , 258:java/lang/Throwable  , 263:java/lang/Throwable  ]
+TRY 184:[238:java/lang/Throwable  , 243:java/lang/Throwable  , 248:java/lang/Throwable  , 253:java/lang/Throwable  , 258:java/lang/Throwable  , 263:java/lang/Throwable  ]
+TRY 187:[238:java/lang/Throwable  , 243:java/lang/Throwable  , 248:java/lang/Throwable  , 253:java/lang/Throwable  , 258:java/lang/Throwable  , 263:java/lang/Throwable  ]
+TRY 190:[238:java/lang/Throwable  , 243:java/lang/Throwable  , 248:java/lang/Throwable  , 253:java/lang/Throwable  , 258:java/lang/Throwable  , 263:java/lang/Throwable  ]
+TRY 192:[238:java/lang/Throwable  , 243:java/lang/Throwable  , 248:java/lang/Throwable  , 253:java/lang/Throwable  , 258:java/lang/Throwable  , 263:java/lang/Throwable  ]
+TRY 197:[238:java/lang/Throwable  , 243:java/lang/Throwable  , 248:java/lang/Throwable  , 253:java/lang/Throwable  , 258:java/lang/Throwable  , 263:java/lang/Throwable  ]
+TRY 199:[238:java/lang/Throwable  , 243:java/lang/Throwable  , 248:java/lang/Throwable  , 253:java/lang/Throwable  , 258:java/lang/Throwable  , 263:java/lang/Throwable  ]
+TRY 203:[238:java/lang/Throwable  , 243:java/lang/Throwable  , 248:java/lang/Throwable  , 253:java/lang/Throwable  , 258:java/lang/Throwable  , 263:java/lang/Throwable  ]
+TRY 205:[243:java/lang/Throwable  , 248:java/lang/Throwable  , 253:java/lang/Throwable  , 258:java/lang/Throwable  , 263:java/lang/Throwable  ]
+TRY 207:[248:java/lang/Throwable  , 253:java/lang/Throwable  , 258:java/lang/Throwable  , 263:java/lang/Throwable  ]
+TRY 209:[253:java/lang/Throwable  , 258:java/lang/Throwable  , 263:java/lang/Throwable  ]
+TRY 211:[258:java/lang/Throwable  , 263:java/lang/Throwable  ]
+TRY 213:[263:java/lang/Throwable  ]
+TRY 216:[238:java/lang/Throwable  , 243:java/lang/Throwable  , 248:java/lang/Throwable  , 253:java/lang/Throwable  , 258:java/lang/Throwable  , 263:java/lang/Throwable  ]
+TRY 223:[238:java/lang/Throwable  , 243:java/lang/Throwable  , 248:java/lang/Throwable  , 253:java/lang/Throwable  , 258:java/lang/Throwable  , 263:java/lang/Throwable  ]
+TRY 224:[238:java/lang/Throwable  , 243:java/lang/Throwable  , 248:java/lang/Throwable  , 253:java/lang/Throwable  , 258:java/lang/Throwable  , 263:java/lang/Throwable  ]
+TRY 226:[238:java/lang/Throwable  , 243:java/lang/Throwable  , 248:java/lang/Throwable  , 253:java/lang/Throwable  , 258:java/lang/Throwable  , 263:java/lang/Throwable  ]
+TRY 228:[243:java/lang/Throwable  , 248:java/lang/Throwable  , 253:java/lang/Throwable  , 258:java/lang/Throwable  , 263:java/lang/Throwable  ]
+TRY 230:[248:java/lang/Throwable  , 253:java/lang/Throwable  , 258:java/lang/Throwable  , 263:java/lang/Throwable  ]
+TRY 232:[253:java/lang/Throwable  , 258:java/lang/Throwable  , 263:java/lang/Throwable  ]
+TRY 234:[258:java/lang/Throwable  , 263:java/lang/Throwable  ]
+TRY 236:[263:java/lang/Throwable  ]
+TRY 240:[238:java/lang/Throwable  , 243:java/lang/Throwable  , 248:java/lang/Throwable  , 253:java/lang/Throwable  , 258:java/lang/Throwable  , 263:java/lang/Throwable  ]
+TRY 242:[243:java/lang/Throwable  , 248:java/lang/Throwable  , 253:java/lang/Throwable  , 258:java/lang/Throwable  , 263:java/lang/Throwable  ]
+TRY 245:[243:java/lang/Throwable  , 248:java/lang/Throwable  , 253:java/lang/Throwable  , 258:java/lang/Throwable  , 263:java/lang/Throwable  ]
+TRY 247:[248:java/lang/Throwable  , 253:java/lang/Throwable  , 258:java/lang/Throwable  , 263:java/lang/Throwable  ]
+TRY 250:[248:java/lang/Throwable  , 253:java/lang/Throwable  , 258:java/lang/Throwable  , 263:java/lang/Throwable  ]
+TRY 252:[253:java/lang/Throwable  , 258:java/lang/Throwable  , 263:java/lang/Throwable  ]
+TRY 255:[253:java/lang/Throwable  , 258:java/lang/Throwable  , 263:java/lang/Throwable  ]
+TRY 257:[258:java/lang/Throwable  , 263:java/lang/Throwable  ]
+TRY 260:[258:java/lang/Throwable  , 263:java/lang/Throwable  ]
+TRY 262:[263:java/lang/Throwable  ]
+TRY 265:[263:java/lang/Throwable  ]
@@ -111,0 +106 @@
+DUP
@@ -113,2 +108,5 @@
-ALOAD 5
-DUP
+MONITORENTER
+ALOAD 0
+ALOAD 4
+INVOKEVIRTUAL local/lito381/server/NativeCombatAttacker$Ref.master()Llocal/lito381/server/NativePlayerCommit$Binding;
+INVOKESTATIC local/lito381/server/NativeProcHealingCompanion.find(Llocal/lito381/server/GameHub;Llocal/lito381/server/NativePlayerCommit$Binding;)Llocal/lito381/server/ClientSession;
@@ -116,6 +113,0 @@
-MONITORENTER
-ALOAD 0
-ALOAD 4
-INVOKEVIRTUAL local/lito381/server/NativeCombatAttacker$Ref.master()Llocal/lito381/server/NativePlayerCommit$Binding;
-INVOKESTATIC local/lito381/server/NativeProcHealingCompanion.find(Llocal/lito381/server/GameHub;Llocal/lito381/server/NativePlayerCommit$Binding;)Llocal/lito381/server/ClientSession;
-ASTORE 7
@@ -123 +115 @@
-IFEQ L32
+IFEQ L30
@@ -128,2 +120,15 @@
-GOTO L33
-CONST null
+GOTO L31
+CONST null
+ASTORE 7
+ALOAD 6
+IFNULL L38
+ILOAD 2
+IFEQ L42
+ALOAD 7
+IFNONNULL L42
+CONST null
+ALOAD 5
+MONITOREXIT
+ARETURN
+ALOAD 6
+INVOKESTATIC local/lito381/server/CombatPoisonAtomic.status(Llocal/lito381/server/ClientSession;)Llocal/lito381/server/BattleTimedStatus;
@@ -131,2 +135,0 @@
-ALOAD 7
-IFNULL L40
@@ -134 +137,4 @@
-IFEQ L44
+IFEQ L50
+ALOAD 7
+INVOKESTATIC local/lito381/server/CombatPoisonAtomic.status(Llocal/lito381/server/ClientSession;)Llocal/lito381/server/BattleTimedStatus;
+GOTO L51
@@ -136,7 +141,0 @@
-IFNONNULL L44
-CONST null
-ALOAD 6
-MONITOREXIT
-ARETURN
-ALOAD 7
-INVOKESTATIC local/lito381/server/CombatPoisonAtomic.status(Llocal/lito381/server/ClientSession;)Llocal/lito381/server/BattleTimedStatus;
@@ -143,0 +143,27 @@
+ALOAD 9
+DUP
+ASTORE 10
+MONITORENTER
+ALOAD 8
+DUP
+ASTORE 11
+MONITORENTER
+ALOAD 4
+INVOKEVIRTUAL local/lito381/server/NativeCombatAttacker$Ref.master()Llocal/lito381/server/NativePlayerCommit$Binding;
+ALOAD 6
+INVOKEVIRTUAL local/lito381/server/NativePlayerCommit$Binding.matches(Llocal/lito381/server/ClientSession;)Z
+IFEQ L71
+ALOAD 0
+ALOAD 6
+ALOAD 4
+INVOKEVIRTUAL local/lito381/server/NativeCombatAttacker$Ref.master()Llocal/lito381/server/NativePlayerCommit$Binding;
+INVOKESTATIC local/lito381/server/NativeProcHealingCompanion.registered(Llocal/lito381/server/GameHub;Llocal/lito381/server/ClientSession;Llocal/lito381/server/NativePlayerCommit$Binding;)Z
+IFNE L79
+CONST null
+ALOAD 11
+MONITOREXIT
+ALOAD 10
+MONITOREXIT
+ALOAD 5
+MONITOREXIT
+ARETURN
@@ -145,20 +171,3 @@
-IFEQ L52
-ALOAD 8
-INVOKESTATIC local/lito381/server/CombatPoisonAtomic.status(Llocal/lito381/server/ClientSession;)Llocal/lito381/server/BattleTimedStatus;
-GOTO L53
-ALOAD 9
-DUP
-ASTORE 10
-ASTORE 11
-ALOAD 11
-DUP
-ASTORE 12
-MONITORENTER
-ALOAD 9
-ASTORE 13
-ALOAD 13
-DUP
-ASTORE 14
-MONITORENTER
-ALOAD 4
-INVOKEVIRTUAL local/lito381/server/NativeCombatAttacker$Ref.master()Llocal/lito381/server/NativePlayerCommit$Binding;
+IFEQ L106
+ALOAD 1
+INVOKEVIRTUAL local/lito381/server/NativeProcHealingCompanion$Plan.caster()Llocal/lito381/server/NativePlayerCommit$Binding;
@@ -167,24 +176,3 @@
-IFEQ L77
-ALOAD 0
-ALOAD 7
-ALOAD 4
-INVOKEVIRTUAL local/lito381/server/NativeCombatAttacker$Ref.master()Llocal/lito381/server/NativePlayerCommit$Binding;
-INVOKESTATIC local/lito381/server/NativeProcHealingCompanion.registered(Llocal/lito381/server/GameHub;Llocal/lito381/server/ClientSession;Llocal/lito381/server/NativePlayerCommit$Binding;)Z
-IFNE L85
-CONST null
-ALOAD 14
-MONITOREXIT
-ALOAD 12
-MONITOREXIT
-ALOAD 6
-MONITOREXIT
-ARETURN
-ILOAD 2
-IFEQ L112
-ALOAD 1
-INVOKEVIRTUAL local/lito381/server/NativeProcHealingCompanion$Plan.caster()Llocal/lito381/server/NativePlayerCommit$Binding;
-ALOAD 8
-INVOKEVIRTUAL local/lito381/server/NativePlayerCommit$Binding.matches(Llocal/lito381/server/ClientSession;)Z
-IFEQ L104
-ALOAD 0
-ALOAD 8
+IFEQ L98
+ALOAD 0
+ALOAD 7
@@ -194 +182 @@
-IFEQ L104
+IFEQ L98
@@ -200,10 +188,10 @@
-IFNE L112
-CONST null
-ALOAD 14
-MONITOREXIT
-ALOAD 12
-MONITOREXIT
-ALOAD 6
-MONITOREXIT
-ARETURN
-ALOAD 7
+IFNE L106
+CONST null
+ALOAD 11
+MONITOREXIT
+ALOAD 10
+MONITOREXIT
+ALOAD 5
+MONITOREXIT
+ARETURN
+ALOAD 6
@@ -213,8 +201,6 @@
-DUP
-ASTORE 15
-ASTORE 16
-ALOAD 16
-DUP
-ASTORE 17
-MONITORENTER
-ALOAD 7
+ASTORE 12
+ALOAD 12
+DUP
+ASTORE 13
+MONITORENTER
+ALOAD 6
@@ -224,3 +210,2 @@
-ASTORE 18
-ALOAD 18
-CHECKCAST local/lito381/server/BattleCompanion
+ASTORE 14
+ALOAD 14
@@ -232,3 +217,6 @@
-IFEQ L165
-CONST null
-ASTORE 18
+IFEQ L155
+CONST null
+ASTORE 14
+ALOAD 12
+INVOKEINTERFACE java/util/List.iterator()Ljava/util/Iterator; itf
+ASTORE 15
@@ -236,3 +223,0 @@
-INVOKEINTERFACE java/util/List.iterator()Ljava/util/Iterator; itf
-ASTORE 19
-ALOAD 19
@@ -240,2 +225,2 @@
-IFEQ L165
-ALOAD 19
+IFEQ L155
+ALOAD 15
@@ -243,2 +228,2 @@
-ASTORE 20
-ALOAD 20
+ASTORE 16
+ALOAD 16
@@ -246,2 +231,2 @@
-IFEQ L164
-ALOAD 20
+IFEQ L154
+ALOAD 16
@@ -249,3 +234,2 @@
-DUP
-ASTORE 21
-CHECKCAST local/lito381/server/BattleCompanion
+ASTORE 17
+ALOAD 17
@@ -257,2 +241,19 @@
-IFNE L164
-ALOAD 21
+IFNE L154
+ALOAD 17
+ASTORE 14
+GOTO L155
+GOTO L132
+ALOAD 14
+IFNONNULL L167
+CONST null
+ALOAD 13
+MONITOREXIT
+ALOAD 11
+MONITOREXIT
+ALOAD 10
+MONITOREXIT
+ALOAD 5
+MONITOREXIT
+ARETURN
+ALOAD 14
+DUP
@@ -260,21 +261,2 @@
-GOTO L165
-GOTO L141
-ALOAD 18
-IFNONNULL L177
-CONST null
-ALOAD 17
-MONITOREXIT
-ALOAD 14
-MONITOREXIT
-ALOAD 12
-MONITOREXIT
-ALOAD 6
-MONITOREXIT
-ARETURN
-ALOAD 18
-ASTORE 22
-ALOAD 22
-DUP
-ASTORE 23
-MONITORENTER
-ALOAD 18
+MONITORENTER
+ALOAD 14
@@ -284,9 +266,6 @@
-ASTORE 24
-ALOAD 24
-ASTORE 25
-ALOAD 25
-DUP
-ASTORE 26
-MONITORENTER
-ALOAD 18
-CHECKCAST local/lito381/server/BattleCompanion
+ASTORE 19
+ALOAD 19
+DUP
+ASTORE 20
+MONITORENTER
+ALOAD 14
@@ -294,2 +273,2 @@
-ASTORE 27
-ALOAD 27
+ASTORE 21
+ALOAD 21
@@ -297,2 +276,2 @@
-IFEQ L217
-ALOAD 27
+IFEQ L201
+ALOAD 21
@@ -300,2 +279,2 @@
-IFLE L217
-ALOAD 27
+IFLE L201
+ALOAD 21
@@ -306,3 +285,2 @@
-IFNE L217
-ALOAD 24
-CHECKCAST local/lito381/server/BattleTimedStatus
+IFNE L201
+ALOAD 19
@@ -313,13 +291,13 @@
-IF_ACMPEQ L231
-CONST null
-ALOAD 26
-MONITOREXIT
-ALOAD 23
-MONITOREXIT
-ALOAD 17
-MONITOREXIT
-ALOAD 14
-MONITOREXIT
-ALOAD 12
-MONITOREXIT
-ALOAD 6
+IF_ACMPEQ L215
+CONST null
+ALOAD 20
+MONITOREXIT
+ALOAD 18
+MONITOREXIT
+ALOAD 13
+MONITOREXIT
+ALOAD 11
+MONITOREXIT
+ALOAD 10
+MONITOREXIT
+ALOAD 5
@@ -331,7 +309,5 @@
-ALOAD 8
-ALOAD 7
-ALOAD 18
-CHECKCAST local/lito381/server/BattleCompanion
-ALOAD 24
-CHECKCAST local/lito381/server/BattleTimedStatus
-ALOAD 27
+ALOAD 7
+ALOAD 6
+ALOAD 14
+ALOAD 19
+ALOAD 21
@@ -339,0 +316,36 @@
+ALOAD 20
+MONITOREXIT
+ALOAD 18
+MONITOREXIT
+ALOAD 13
+MONITOREXIT
+ALOAD 11
+MONITOREXIT
+ALOAD 10
+MONITOREXIT
+ALOAD 5
+MONITOREXIT
+ARETURN
+ASTORE 22
+ALOAD 20
+MONITOREXIT
+ALOAD 22
+ATHROW
+ASTORE 23
+ALOAD 18
+MONITOREXIT
+ALOAD 23
+ATHROW
+ASTORE 24
+ALOAD 13
+MONITOREXIT
+ALOAD 24
+ATHROW
+ASTORE 25
+ALOAD 11
+MONITOREXIT
+ALOAD 25
+ATHROW
+ASTORE 26
+ALOAD 10
+MONITOREXIT
@@ -341,42 +353,6 @@
-MONITOREXIT
-ALOAD 23
-MONITOREXIT
-ALOAD 17
-MONITOREXIT
-ALOAD 14
-MONITOREXIT
-ALOAD 12
-MONITOREXIT
-ALOAD 6
-MONITOREXIT
-ARETURN
-ASTORE 28
-ALOAD 26
-MONITOREXIT
-ALOAD 28
-ATHROW
-ASTORE 29
-ALOAD 23
-MONITOREXIT
-ALOAD 29
-ATHROW
-ASTORE 30
-ALOAD 17
-MONITOREXIT
-ALOAD 30
-ATHROW
-ASTORE 31
-ALOAD 14
-MONITOREXIT
-ALOAD 31
-ATHROW
-ASTORE 32
-ALOAD 12
-MONITOREXIT
-ALOAD 32
-ATHROW
-ASTORE 33
-ALOAD 6
-MONITOREXIT
-ALOAD 33
-ATHROW
+ATHROW
+ASTORE 27
+ALOAD 5
+MONITOREXIT
+ALOAD 27
+ATHROW
```

</details>

### NativeProcHealingEncounter — DIFF

```json
{
  "where": "method",
  "member": "graphics(Llocal/lito381/server/GameHub;Llocal/lito381/server/NativeProcHealingEncounter$Plan;I)V",
  "insnIndex": 36,
  "detail": "left=IFNE L85 right=IFNE L83; paths rejected: normal control-flow cycle"
}
```
<details>
<summary>全部方法診斷</summary>

```json
[
  {
    "method": "graphics(Llocal/lito381/server/GameHub;Llocal/lito381/server/NativeProcHealingEncounter$Plan;I)V",
    "status": "DIFF",
    "detail": "method graphics(Llocal/lito381/server/GameHub;Llocal/lito381/server/NativeProcHealingEncounter$Plan;I)V insn[36]: left=IFNE L85 right=IFNE L83; paths rejected: normal control-flow cycle"
  }
]
```

</details>
<details>
<summary>完整正規化差異</summary>

```diff
--- staged/NativeProcHealingEncounter/enqueue-6349559.txt
+++ candidate/NativeProcHealingEncounter/enqueue-6349559.txt
@@ -8,13 +8,13 @@
-TRY 39:[74:java/lang/Throwable  ]
-TRY 43:[74:java/lang/Throwable  ]
-TRY 46:[74:java/lang/Throwable  ]
-TRY 50:[74:java/lang/Throwable  ]
-TRY 52:[74:java/lang/Throwable  ]
-TRY 56:[74:java/lang/Throwable  ]
-TRY 60:[74:java/lang/Throwable  ]
-TRY 62:[74:java/lang/Throwable  ]
-TRY 63:[74:java/lang/Throwable  ]
-TRY 65:[74:java/lang/Throwable  ]
-TRY 70:[74:java/lang/Throwable  ]
-TRY 72:[74:java/lang/Throwable  ]
-TRY 76:[74:java/lang/Throwable  ]
+TRY 37:[72:java/lang/Throwable  ]
+TRY 41:[72:java/lang/Throwable  ]
+TRY 44:[72:java/lang/Throwable  ]
+TRY 48:[72:java/lang/Throwable  ]
+TRY 50:[72:java/lang/Throwable  ]
+TRY 54:[72:java/lang/Throwable  ]
+TRY 58:[72:java/lang/Throwable  ]
+TRY 60:[72:java/lang/Throwable  ]
+TRY 61:[72:java/lang/Throwable  ]
+TRY 63:[72:java/lang/Throwable  ]
+TRY 68:[72:java/lang/Throwable  ]
+TRY 70:[72:java/lang/Throwable  ]
+TRY 74:[72:java/lang/Throwable  ]
@@ -51,0 +52 @@
+DUP
@@ -53,3 +53,0 @@
-ALOAD 7
-DUP
-ASTORE 8
@@ -61 +59 @@
-IFNE L45
+IFNE L43
@@ -63 +61 @@
-ALOAD 8
+ALOAD 7
@@ -68 +66 @@
-IFEQ L71
+IFEQ L69
@@ -72 +70 @@
-ASTORE 9
+ASTORE 8
@@ -78 +76,2 @@
-ASTORE 10
+ASTORE 9
+ALOAD 8
@@ -80 +78,0 @@
-ALOAD 10
@@ -82 +80 @@
-ALOAD 9
+ALOAD 8
@@ -85 +83 @@
-ALOAD 9
+ALOAD 8
@@ -87,0 +86 @@
+ALOAD 8
@@ -89 +87,0 @@
-ALOAD 10
@@ -92 +90 @@
-ALOAD 8
+ALOAD 7
@@ -94,3 +92,3 @@
-GOTO L79
-ASTORE 11
-ALOAD 8
+GOTO L77
+ASTORE 10
+ALOAD 7
@@ -98 +96 @@
-ALOAD 11
+ALOAD 10
--- staged/NativeProcHealingEncounter/graphics-d8f3cd80.txt
+++ candidate/NativeProcHealingEncounter/graphics-d8f3cd80.txt
@@ -45 +45 @@
-IFNE L85
+IFNE L83
@@ -54 +53,0 @@
-CHECKCAST java/util/Iterator
@@ -56 +55 @@
-IFEQ L85
+IFEQ L83
@@ -58 +56,0 @@
-CHECKCAST java/util/Iterator
@@ -64 +62 @@
-IF_ACMPEQ L84
+IF_ACMPEQ L82
@@ -88 +86 @@
-IFGT L84
+IFGT L82
@@ -105 +103 @@
-IFEQ L135
+IFEQ L132
@@ -115 +113 @@
-IFEQ L135
+IFEQ L132
@@ -127,2 +125,2 @@
-IFNE L120
-GOTO L135
+IFNE L118
+GOTO L132
@@ -132 +130 @@
-IFEQ L134
+IFEQ L131
@@ -135 +132,0 @@
-CHECKCAST [B
@@ -143 +140 @@
-GOTO L94
+GOTO L92
--- staged/NativeProcHealingEncounter/withLive-a68bbf29.txt
+++ candidate/NativeProcHealingEncounter/withLive-a68bbf29.txt
@@ -8,55 +8,55 @@
-TRY 22:[190:java/lang/Throwable  ]
-TRY 23:[190:java/lang/Throwable  ]
-TRY 27:[190:java/lang/Throwable  ]
-TRY 31:[190:java/lang/Throwable  ]
-TRY 36:[190:java/lang/Throwable  ]
-TRY 38:[190:java/lang/Throwable  ]
-TRY 41:[190:java/lang/Throwable  ]
-TRY 43:[190:java/lang/Throwable  ]
-TRY 48:[190:java/lang/Throwable  ]
-TRY 51:[190:java/lang/Throwable  ]
-TRY 56:[190:java/lang/Throwable  ]
-TRY 59:[185:java/lang/Throwable  , 190:java/lang/Throwable  ]
-TRY 63:[185:java/lang/Throwable  , 190:java/lang/Throwable  ]
-TRY 64:[185:java/lang/Throwable  , 190:java/lang/Throwable  ]
-TRY 66:[185:java/lang/Throwable  , 190:java/lang/Throwable  ]
-TRY 70:[185:java/lang/Throwable  , 190:java/lang/Throwable  ]
-TRY 71:[185:java/lang/Throwable  , 190:java/lang/Throwable  ]
-TRY 73:[185:java/lang/Throwable  , 190:java/lang/Throwable  ]
-TRY 77:[185:java/lang/Throwable  , 190:java/lang/Throwable  ]
-TRY 79:[190:java/lang/Throwable  ]
-TRY 83:[185:java/lang/Throwable  , 190:java/lang/Throwable  ]
-TRY 90:[185:java/lang/Throwable  , 190:java/lang/Throwable  ]
-TRY 94:[180:java/lang/Throwable  , 185:java/lang/Throwable  , 190:java/lang/Throwable  ]
-TRY 99:[180:java/lang/Throwable  , 185:java/lang/Throwable  , 190:java/lang/Throwable  ]
-TRY 101:[185:java/lang/Throwable  , 190:java/lang/Throwable  ]
-TRY 103:[190:java/lang/Throwable  ]
-TRY 107:[180:java/lang/Throwable  , 185:java/lang/Throwable  , 190:java/lang/Throwable  ]
-TRY 113:[180:java/lang/Throwable  , 185:java/lang/Throwable  , 190:java/lang/Throwable  ]
-TRY 115:[185:java/lang/Throwable  , 190:java/lang/Throwable  ]
-TRY 117:[190:java/lang/Throwable  ]
-TRY 121:[180:java/lang/Throwable  , 185:java/lang/Throwable  , 190:java/lang/Throwable  ]
-TRY 122:[180:java/lang/Throwable  , 185:java/lang/Throwable  , 190:java/lang/Throwable  ]
-TRY 129:[180:java/lang/Throwable  , 185:java/lang/Throwable  , 190:java/lang/Throwable  ]
-TRY 132:[175:java/lang/Throwable  , 180:java/lang/Throwable  , 185:java/lang/Throwable  , 190:java/lang/Throwable  ]
-TRY 136:[175:java/lang/Throwable  , 180:java/lang/Throwable  , 185:java/lang/Throwable  , 190:java/lang/Throwable  ]
-TRY 138:[175:java/lang/Throwable  , 180:java/lang/Throwable  , 185:java/lang/Throwable  , 190:java/lang/Throwable  ]
-TRY 142:[175:java/lang/Throwable  , 180:java/lang/Throwable  , 185:java/lang/Throwable  , 190:java/lang/Throwable  ]
-TRY 146:[175:java/lang/Throwable  , 180:java/lang/Throwable  , 185:java/lang/Throwable  , 190:java/lang/Throwable  ]
-TRY 148:[180:java/lang/Throwable  , 185:java/lang/Throwable  , 190:java/lang/Throwable  ]
-TRY 150:[185:java/lang/Throwable  , 190:java/lang/Throwable  ]
-TRY 152:[190:java/lang/Throwable  ]
-TRY 155:[175:java/lang/Throwable  , 180:java/lang/Throwable  , 185:java/lang/Throwable  , 190:java/lang/Throwable  ]
-TRY 164:[175:java/lang/Throwable  , 180:java/lang/Throwable  , 185:java/lang/Throwable  , 190:java/lang/Throwable  ]
-TRY 165:[175:java/lang/Throwable  , 180:java/lang/Throwable  , 185:java/lang/Throwable  , 190:java/lang/Throwable  ]
-TRY 167:[175:java/lang/Throwable  , 180:java/lang/Throwable  , 185:java/lang/Throwable  , 190:java/lang/Throwable  ]
-TRY 169:[180:java/lang/Throwable  , 185:java/lang/Throwable  , 190:java/lang/Throwable  ]
-TRY 171:[185:java/lang/Throwable  , 190:java/lang/Throwable  ]
-TRY 173:[190:java/lang/Throwable  ]
-TRY 177:[175:java/lang/Throwable  , 180:java/lang/Throwable  , 185:java/lang/Throwable  , 190:java/lang/Throwable  ]
-TRY 179:[180:java/lang/Throwable  , 185:java/lang/Throwable  , 190:java/lang/Throwable  ]
-TRY 182:[180:java/lang/Throwable  , 185:java/lang/Throwable  , 190:java/lang/Throwable  ]
-TRY 184:[185:java/lang/Throwable  , 190:java/lang/Throwable  ]
-TRY 187:[185:java/lang/Throwable  , 190:java/lang/Throwable  ]
-TRY 189:[190:java/lang/Throwable  ]
-TRY 192:[190:java/lang/Throwable  ]
+TRY 20:[182:java/lang/Throwable  ]
+TRY 21:[182:java/lang/Throwable  ]
+TRY 25:[182:java/lang/Throwable  ]
+TRY 29:[182:java/lang/Throwable  ]
+TRY 34:[182:java/lang/Throwable  ]
+TRY 36:[182:java/lang/Throwable  ]
+TRY 39:[182:java/lang/Throwable  ]
+TRY 41:[182:java/lang/Throwable  ]
+TRY 46:[182:java/lang/Throwable  ]
+TRY 49:[182:java/lang/Throwable  ]
+TRY 52:[182:java/lang/Throwable  ]
+TRY 55:[177:java/lang/Throwable  , 182:java/lang/Throwable  ]
+TRY 59:[177:java/lang/Throwable  , 182:java/lang/Throwable  ]
+TRY 60:[177:java/lang/Throwable  , 182:java/lang/Throwable  ]
+TRY 62:[177:java/lang/Throwable  , 182:java/lang/Throwable  ]
+TRY 66:[177:java/lang/Throwable  , 182:java/lang/Throwable  ]
+TRY 67:[177:java/lang/Throwable  , 182:java/lang/Throwable  ]
+TRY 69:[177:java/lang/Throwable  , 182:java/lang/Throwable  ]
+TRY 73:[177:java/lang/Throwable  , 182:java/lang/Throwable  ]
+TRY 75:[182:java/lang/Throwable  ]
+TRY 79:[177:java/lang/Throwable  , 182:java/lang/Throwable  ]
+TRY 84:[177:java/lang/Throwable  , 182:java/lang/Throwable  ]
+TRY 88:[172:java/lang/Throwable  , 177:java/lang/Throwable  , 182:java/lang/Throwable  ]
+TRY 93:[172:java/lang/Throwable  , 177:java/lang/Throwable  , 182:java/lang/Throwable  ]
+TRY 95:[177:java/lang/Throwable  , 182:java/lang/Throwable  ]
+TRY 97:[182:java/lang/Throwable  ]
+TRY 101:[172:java/lang/Throwable  , 177:java/lang/Throwable  , 182:java/lang/Throwable  ]
+TRY 107:[172:java/lang/Throwable  , 177:java/lang/Throwable  , 182:java/lang/Throwable  ]
+TRY 109:[177:java/lang/Throwable  , 182:java/lang/Throwable  ]
+TRY 111:[182:java/lang/Throwable  ]
+TRY 115:[172:java/lang/Throwable  , 177:java/lang/Throwable  , 182:java/lang/Throwable  ]
+TRY 116:[172:java/lang/Throwable  , 177:java/lang/Throwable  , 182:java/lang/Throwable  ]
+TRY 121:[172:java/lang/Throwable  , 177:java/lang/Throwable  , 182:java/lang/Throwable  ]
+TRY 124:[167:java/lang/Throwable  , 172:java/lang/Throwable  , 177:java/lang/Throwable  , 182:java/lang/Throwable  ]
+TRY 128:[167:java/lang/Throwable  , 172:java/lang/Throwable  , 177:java/lang/Throwable  , 182:java/lang/Throwable  ]
+TRY 130:[167:java/lang/Throwable  , 172:java/lang/Throwable  , 177:java/lang/Throwable  , 182:java/lang/Throwable  ]
+TRY 134:[167:java/lang/Throwable  , 172:java/lang/Throwable  , 177:java/lang/Throwable  , 182:java/lang/Throwable  ]
+TRY 138:[167:java/lang/Throwable  , 172:java/lang/Throwable  , 177:java/lang/Throwable  , 182:java/lang/Throwable  ]
+TRY 140:[172:java/lang/Throwable  , 177:java/lang/Throwable  , 182:java/lang/Throwable  ]
+TRY 142:[177:java/lang/Throwable  , 182:java/lang/Throwable  ]
+TRY 144:[182:java/lang/Throwable  ]
+TRY 147:[167:java/lang/Throwable  , 172:java/lang/Throwable  , 177:java/lang/Throwable  , 182:java/lang/Throwable  ]
+TRY 156:[167:java/lang/Throwable  , 172:java/lang/Throwable  , 177:java/lang/Throwable  , 182:java/lang/Throwable  ]
+TRY 157:[167:java/lang/Throwable  , 172:java/lang/Throwable  , 177:java/lang/Throwable  , 182:java/lang/Throwable  ]
+TRY 159:[167:java/lang/Throwable  , 172:java/lang/Throwable  , 177:java/lang/Throwable  , 182:java/lang/Throwable  ]
+TRY 161:[172:java/lang/Throwable  , 177:java/lang/Throwable  , 182:java/lang/Throwable  ]
+TRY 163:[177:java/lang/Throwable  , 182:java/lang/Throwable  ]
+TRY 165:[182:java/lang/Throwable  ]
+TRY 169:[167:java/lang/Throwable  , 172:java/lang/Throwable  , 177:java/lang/Throwable  , 182:java/lang/Throwable  ]
+TRY 171:[172:java/lang/Throwable  , 177:java/lang/Throwable  , 182:java/lang/Throwable  ]
+TRY 174:[172:java/lang/Throwable  , 177:java/lang/Throwable  , 182:java/lang/Throwable  ]
+TRY 176:[177:java/lang/Throwable  , 182:java/lang/Throwable  ]
+TRY 179:[177:java/lang/Throwable  , 182:java/lang/Throwable  ]
+TRY 181:[182:java/lang/Throwable  ]
+TRY 184:[182:java/lang/Throwable  ]
@@ -77,0 +78 @@
+DUP
@@ -79,3 +79,0 @@
-ALOAD 5
-DUP
-ASTORE 6
@@ -86,0 +85,4 @@
+ASTORE 6
+ALOAD 0
+ALOAD 2
+INVOKESTATIC local/lito381/server/NativeProcHealingEncounter.find(Llocal/lito381/server/GameHub;Llocal/lito381/server/NativePlayerCommit$Binding;)Llocal/lito381/server/ClientSession;
@@ -88,5 +90 @@
-ALOAD 0
-ALOAD 2
-INVOKESTATIC local/lito381/server/NativeProcHealingEncounter.find(Llocal/lito381/server/GameHub;Llocal/lito381/server/NativePlayerCommit$Binding;)Llocal/lito381/server/ClientSession;
-ASTORE 8
-ALOAD 7
+ALOAD 6
@@ -95,3 +93,3 @@
-IFEQ L46
-ALOAD 8
-IFNULL L46
+IFEQ L44
+ALOAD 7
+IFNULL L44
@@ -102 +100 @@
-IFEQ L46
+IFEQ L44
@@ -108,6 +106,6 @@
-IFEQ L50
-CONST null
-ALOAD 6
-MONITOREXIT
-ARETURN
-ALOAD 8
+IFEQ L48
+CONST null
+ALOAD 5
+MONITOREXIT
+ARETURN
+ALOAD 7
@@ -115,7 +113,5 @@
-ASTORE 9
-ALOAD 9
-DUP
-ASTORE 10
-MONITORENTER
-ALOAD 2
-ALOAD 8
+DUP
+ASTORE 8
+MONITORENTER
+ALOAD 2
+ALOAD 7
@@ -123,2 +119,2 @@
-IFEQ L75
-ALOAD 7
+IFEQ L71
+ALOAD 6
@@ -128 +124 @@
-ALOAD 8
+ALOAD 7
@@ -130,2 +126,2 @@
-IFEQ L75
-ALOAD 7
+IFEQ L71
+ALOAD 6
@@ -135 +131 @@
-ALOAD 8
+ALOAD 7
@@ -137,8 +133,8 @@
-IFNE L81
-CONST null
-ALOAD 10
-MONITOREXIT
-ALOAD 6
-MONITOREXIT
-ARETURN
-ALOAD 7
+IFNE L77
+CONST null
+ALOAD 8
+MONITOREXIT
+ALOAD 5
+MONITOREXIT
+ARETURN
+ALOAD 6
@@ -147,6 +143,4 @@
-DUP
-ASTORE 11
-ASTORE 12
-ALOAD 12
-DUP
-ASTORE 13
+ASTORE 9
+ALOAD 9
+DUP
+ASTORE 10
@@ -155,2 +149,2 @@
-IFNULL L105
-ALOAD 11
+IFNULL L99
+ALOAD 9
@@ -159,10 +153,10 @@
-IF_ACMPEQ L105
-CONST null
-ALOAD 13
-MONITOREXIT
-ALOAD 10
-MONITOREXIT
-ALOAD 6
-MONITOREXIT
-ARETURN
-ALOAD 11
+IF_ACMPEQ L99
+CONST null
+ALOAD 10
+MONITOREXIT
+ALOAD 8
+MONITOREXIT
+ALOAD 5
+MONITOREXIT
+ARETURN
+ALOAD 9
@@ -171,12 +165,12 @@
-ASTORE 14
-ALOAD 14
-IFNONNULL L119
-CONST null
-ALOAD 13
-MONITOREXIT
-ALOAD 10
-MONITOREXIT
-ALOAD 6
-MONITOREXIT
-ARETURN
-ALOAD 14
+ASTORE 11
+ALOAD 11
+IFNONNULL L113
+CONST null
+ALOAD 10
+MONITOREXIT
+ALOAD 8
+MONITOREXIT
+ALOAD 5
+MONITOREXIT
+ARETURN
+ALOAD 11
@@ -186,8 +180,6 @@
-DUP
-ASTORE 15
-ASTORE 16
-ALOAD 16
-DUP
-ASTORE 17
-MONITORENTER
-ALOAD 14
+ASTORE 12
+ALOAD 12
+DUP
+ASTORE 13
+MONITORENTER
+ALOAD 11
@@ -196,2 +188,2 @@
-ASTORE 18
-ALOAD 15
+ASTORE 14
+ALOAD 12
@@ -202,2 +194,2 @@
-IF_ACMPNE L144
-ALOAD 18
+IF_ACMPNE L136
+ALOAD 14
@@ -206,4 +198,2 @@
-IFGT L154
-CONST null
-ALOAD 17
-MONITOREXIT
+IFGT L146
+CONST null
@@ -214 +204,3 @@
-ALOAD 6
+ALOAD 8
+MONITOREXIT
+ALOAD 5
@@ -221,2 +213,4 @@
-ALOAD 7
-ALOAD 11
+ALOAD 6
+ALOAD 9
+ALOAD 11
+ALOAD 12
@@ -224,3 +218 @@
-ALOAD 15
-ALOAD 18
-ALOAD 8
+ALOAD 7
@@ -228,0 +221,22 @@
+ALOAD 13
+MONITOREXIT
+ALOAD 10
+MONITOREXIT
+ALOAD 8
+MONITOREXIT
+ALOAD 5
+MONITOREXIT
+ARETURN
+ASTORE 15
+ALOAD 13
+MONITOREXIT
+ALOAD 15
+ATHROW
+ASTORE 16
+ALOAD 10
+MONITOREXIT
+ALOAD 16
+ATHROW
+ASTORE 17
+ALOAD 8
+MONITOREXIT
@@ -230,28 +244,6 @@
-MONITOREXIT
-ALOAD 13
-MONITOREXIT
-ALOAD 10
-MONITOREXIT
-ALOAD 6
-MONITOREXIT
-ARETURN
-ASTORE 19
-ALOAD 17
-MONITOREXIT
-ALOAD 19
-ATHROW
-ASTORE 20
-ALOAD 13
-MONITOREXIT
-ALOAD 20
-ATHROW
-ASTORE 21
-ALOAD 10
-MONITOREXIT
-ALOAD 21
-ATHROW
-ASTORE 22
-ALOAD 6
-MONITOREXIT
-ALOAD 22
-ATHROW
+ATHROW
+ASTORE 18
+ALOAD 5
+MONITOREXIT
+ALOAD 18
+ATHROW
```

</details>

### NativeProcHealingEncounter$Dispatch — DIFF

```json
{
  "where": "method",
  "member": "run()V",
  "insnIndex": null,
  "detail": "left=13:[90:java/lang/Throwable  , 98:java/lang/Throwable  ] right=13:[74:java/lang/Throwable  , 82:java/lang/Throwable  ]; paths rejected: normal control-flow cycle"
}
```
<details>
<summary>全部方法診斷</summary>

```json
[
  {
    "method": "run()V",
    "status": "DIFF",
    "detail": "method run()V: left=13:[90:java/lang/Throwable  , 98:java/lang/Throwable  ] right=13:[74:java/lang/Throwable  , 82:java/lang/Throwable  ]; paths rejected: normal control-flow cycle"
  }
]
```

</details>
<details>
<summary>完整正規化差異</summary>

```diff
--- staged/NativeProcHealingEncounter$Dispatch/run-9b75.txt
+++ candidate/NativeProcHealingEncounter$Dispatch/run-9b75.txt
@@ -8,24 +8,24 @@
-TRY 13:[90:java/lang/Throwable  , 98:java/lang/Throwable  ]
-TRY 15:[90:java/lang/Throwable  , 98:java/lang/Throwable  ]
-TRY 17:[90:java/lang/Throwable  , 98:java/lang/Throwable  ]
-TRY 25:[90:java/lang/Throwable  , 98:java/lang/Throwable  ]
-TRY 27:[90:java/lang/Throwable  , 98:java/lang/Throwable  ]
-TRY 29:[90:java/lang/Throwable  , 98:java/lang/Throwable  ]
-TRY 37:[90:java/lang/Throwable  , 98:java/lang/Throwable  ]
-TRY 39:[90:java/lang/Throwable  , 98:java/lang/Throwable  ]
-TRY 41:[90:java/lang/Throwable  , 98:java/lang/Throwable  ]
-TRY 49:[90:java/lang/Throwable  , 98:java/lang/Throwable  ]
-TRY 52:[90:java/lang/Throwable  , 98:java/lang/Throwable  ]
-TRY 54:[90:java/lang/Throwable  , 98:java/lang/Throwable  ]
-TRY 57:[90:java/lang/Throwable  , 98:java/lang/Throwable  ]
-TRY 58:[90:java/lang/Throwable  , 98:java/lang/Throwable  ]
-TRY 60:[90:java/lang/Throwable  , 98:java/lang/Throwable  ]
-TRY 63:[90:java/lang/Throwable  , 98:java/lang/Throwable  ]
-TRY 65:[90:java/lang/Throwable  , 98:java/lang/Throwable  ]
-TRY 67:[90:java/lang/Throwable  , 98:java/lang/Throwable  ]
-TRY 69:[90:java/lang/Throwable  , 98:java/lang/Throwable  ]
-TRY 72:[90:java/lang/Throwable  , 98:java/lang/Throwable  ]
-TRY 73:[90:java/lang/Throwable  , 98:java/lang/Throwable  ]
-TRY 83:[90:java/lang/Throwable  , 98:java/lang/Throwable  ]
-TRY 84:[90:java/lang/Throwable  , 98:java/lang/Throwable  ]
-TRY 92:[98:java/lang/Throwable  ]
+TRY 13:[74:java/lang/Throwable  , 82:java/lang/Throwable  ]
+TRY 15:[74:java/lang/Throwable  , 82:java/lang/Throwable  ]
+TRY 17:[74:java/lang/Throwable  , 82:java/lang/Throwable  ]
+TRY 21:[74:java/lang/Throwable  , 82:java/lang/Throwable  ]
+TRY 23:[74:java/lang/Throwable  , 82:java/lang/Throwable  ]
+TRY 25:[74:java/lang/Throwable  , 82:java/lang/Throwable  ]
+TRY 29:[74:java/lang/Throwable  , 82:java/lang/Throwable  ]
+TRY 31:[74:java/lang/Throwable  , 82:java/lang/Throwable  ]
+TRY 33:[74:java/lang/Throwable  , 82:java/lang/Throwable  ]
+TRY 37:[74:java/lang/Throwable  , 82:java/lang/Throwable  ]
+TRY 40:[74:java/lang/Throwable  , 82:java/lang/Throwable  ]
+TRY 43:[74:java/lang/Throwable  , 82:java/lang/Throwable  ]
+TRY 46:[74:java/lang/Throwable  , 82:java/lang/Throwable  ]
+TRY 49:[74:java/lang/Throwable  , 82:java/lang/Throwable  ]
+TRY 50:[74:java/lang/Throwable  , 82:java/lang/Throwable  ]
+TRY 52:[74:java/lang/Throwable  , 82:java/lang/Throwable  ]
+TRY 54:[74:java/lang/Throwable  , 82:java/lang/Throwable  ]
+TRY 56:[74:java/lang/Throwable  , 82:java/lang/Throwable  ]
+TRY 58:[74:java/lang/Throwable  , 82:java/lang/Throwable  ]
+TRY 61:[74:java/lang/Throwable  , 82:java/lang/Throwable  ]
+TRY 62:[74:java/lang/Throwable  , 82:java/lang/Throwable  ]
+TRY 63:[74:java/lang/Throwable  , 82:java/lang/Throwable  ]
+TRY 68:[74:java/lang/Throwable  , 82:java/lang/Throwable  ]
+TRY 76:[82:java/lang/Throwable  ]
@@ -36 +36 @@
-IF_ICMPGE L107
+IF_ICMPGE L91
@@ -50,6 +50,2 @@
-IFEQ L24
-ALOAD 0
-GETFIELD local/lito381/server/NativeProcHealingEncounter$Dispatch.queue:Llocal/lito381/server/CombatNpcDamageQueue$Queue;
-ALOAD 2
-INVOKEVIRTUAL local/lito381/server/CombatNpcDamageQueue$Queue.complete(Llocal/lito381/server/CombatNpcDamageQueue$Event;)V
-GOTO L105
+IFEQ L20
+GOTO L69
@@ -62,6 +58,2 @@
-IFEQ L36
-ALOAD 0
-GETFIELD local/lito381/server/NativeProcHealingEncounter$Dispatch.queue:Llocal/lito381/server/CombatNpcDamageQueue$Queue;
-ALOAD 2
-INVOKEVIRTUAL local/lito381/server/CombatNpcDamageQueue$Queue.complete(Llocal/lito381/server/CombatNpcDamageQueue$Event;)V
-GOTO L105
+IFEQ L28
+GOTO L69
@@ -74,6 +66,2 @@
-IFEQ L48
-ALOAD 0
-GETFIELD local/lito381/server/NativeProcHealingEncounter$Dispatch.queue:Llocal/lito381/server/CombatNpcDamageQueue$Queue;
-ALOAD 2
-INVOKEVIRTUAL local/lito381/server/CombatNpcDamageQueue$Queue.complete(Llocal/lito381/server/CombatNpcDamageQueue$Event;)V
-GOTO L105
+IFEQ L36
+GOTO L69
@@ -85 +73,5 @@
-IFNE L59
+IFEQ L46
+ALOAD 3
+CHECKCAST local/lito381/server/NativeProcHealingEncounter$Plan
+ASTORE 4
+GOTO L51
@@ -91,3 +82,0 @@
-ALOAD 3
-CHECKCAST local/lito381/server/NativeProcHealingEncounter$Plan
-ASTORE 4
@@ -105,0 +95 @@
+CHECKCAST local/lito381/server/NativeProcEncounterPublication$Context
@@ -108,6 +98 @@
-IFNONNULL L82
-ALOAD 0
-GETFIELD local/lito381/server/NativeProcHealingEncounter$Dispatch.queue:Llocal/lito381/server/CombatNpcDamageQueue$Queue;
-ALOAD 2
-INVOKEVIRTUAL local/lito381/server/CombatNpcDamageQueue$Queue.complete(Llocal/lito381/server/CombatNpcDamageQueue$Event;)V
-GOTO L105
+IFNULL L69
@@ -115 +99,0 @@
-CHECKCAST local/lito381/server/NativeProcEncounterPublication$Context
@@ -121 +105 @@
-GOTO L105
+GOTO L89
@@ -129 +113 @@
-GOTO L105
+GOTO L89
```

</details>

### NativeProcHealingPlayer — DIFF

```json
{
  "where": "method",
  "member": "graphics(Llocal/lito381/server/ClientSession;Llocal/lito381/server/NativePlayerCommit$Binding;Llocal/lito381/server/ClientSession;Llocal/lito381/server/NativePlayerCommit$Binding;II)V",
  "insnIndex": 114,
  "detail": "left=IFEQ L152 right=IFEQ L151; paths rejected: normal control-flow cycle"
}
```
<details>
<summary>全部方法診斷</summary>

```json
[
  {
    "method": "graphics(Llocal/lito381/server/ClientSession;Llocal/lito381/server/NativePlayerCommit$Binding;Llocal/lito381/server/ClientSession;Llocal/lito381/server/NativePlayerCommit$Binding;II)V",
    "status": "DIFF",
    "detail": "method graphics(Llocal/lito381/server/ClientSession;Llocal/lito381/server/NativePlayerCommit$Binding;Llocal/lito381/server/ClientSession;Llocal/lito381/server/NativePlayerCommit$Binding;II)V insn[114]: left=IFEQ L152 right=IFEQ L151; paths rejected: normal control-flow cycle"
  },
  {
    "method": "nativeSourceEquipmentProcOriginalApply(Llocal/lito381/server/ClientSession;Llocal/lito381/server/ClientSession;I)Z",
    "status": "DIFF",
    "detail": "method nativeSourceEquipmentProcOriginalApply(Llocal/lito381/server/ClientSession;Llocal/lito381/server/ClientSession;I)Z: left=114:[245:java/lang/Throwable  ] right=100:[223:java/lang/Throwable  ]; path event multisets differ (left=156, right=177)"
  }
]
```

</details>
<details>
<summary>完整正規化差異</summary>

```diff
--- staged/NativeProcHealingPlayer/graphics-ca867e35.txt
+++ candidate/NativeProcHealingPlayer/graphics-ca867e35.txt
@@ -123 +123 @@
-IFEQ L152
+IFEQ L151
@@ -134 +134 @@
-IFEQ L152
+IFEQ L151
@@ -138 +138 @@
-IFEQ L152
+IFEQ L151
@@ -142 +142 @@
-IFEQ L152
+IFEQ L151
@@ -147 +147 @@
-GOTO L152
+GOTO L151
@@ -151 +151 @@
-IFEQ L151
+IFEQ L150
@@ -154 +153,0 @@
-CHECKCAST [B
--- staged/NativeProcHealingPlayer/nativeSourceEquipmentProcOriginalApply-9e973510.txt
+++ candidate/NativeProcHealingPlayer/nativeSourceEquipmentProcOriginalApply-9e973510.txt
@@ -8,45 +8,42 @@
-TRY 114:[245:java/lang/Throwable  ]
-TRY 115:[245:java/lang/Throwable  ]
-TRY 121:[245:java/lang/Throwable  ]
-TRY 124:[150:java/lang/Throwable  , 245:java/lang/Throwable  ]
-TRY 128:[150:java/lang/Throwable  , 245:java/lang/Throwable  ]
-TRY 133:[150:java/lang/Throwable  , 245:java/lang/Throwable  ]
-TRY 134:[150:java/lang/Throwable  , 245:java/lang/Throwable  ]
-TRY 138:[150:java/lang/Throwable  , 245:java/lang/Throwable  ]
-TRY 148:[150:java/lang/Throwable  , 245:java/lang/Throwable  ]
-TRY 152:[150:java/lang/Throwable  , 245:java/lang/Throwable  ]
-TRY 154:[245:java/lang/Throwable  ]
-TRY 158:[245:java/lang/Throwable  ]
-TRY 160:[230:java/lang/Throwable  , 245:java/lang/Throwable  ]
-TRY 164:[230:java/lang/Throwable  , 245:java/lang/Throwable  ]
-TRY 168:[230:java/lang/Throwable  , 245:java/lang/Throwable  ]
-TRY 172:[230:java/lang/Throwable  , 245:java/lang/Throwable  ]
-TRY 177:[230:java/lang/Throwable  , 245:java/lang/Throwable  ]
-TRY 178:[230:java/lang/Throwable  , 245:java/lang/Throwable  ]
-TRY 182:[230:java/lang/Throwable  , 245:java/lang/Throwable  ]
-TRY 189:[221:java/lang/Throwable  , 230:java/lang/Throwable  , 245:java/lang/Throwable  ]
-TRY 195:[221:java/lang/Throwable  , 230:java/lang/Throwable  , 245:java/lang/Throwable  ]
-TRY 199:[221:java/lang/Throwable  , 230:java/lang/Throwable  , 245:java/lang/Throwable  ]
-TRY 202:[221:java/lang/Throwable  , 230:java/lang/Throwable  , 245:java/lang/Throwable  ]
-TRY 205:[221:java/lang/Throwable  , 230:java/lang/Throwable  , 245:java/lang/Throwable  ]
-TRY 206:[221:java/lang/Throwable  , 230:java/lang/Throwable  , 245:java/lang/Throwable  ]
-TRY 214:[221:java/lang/Throwable  , 230:java/lang/Throwable  , 245:java/lang/Throwable  ]
-TRY 216:[221:java/lang/Throwable  , 230:java/lang/Throwable  , 245:java/lang/Throwable  ]
-TRY 218:[230:java/lang/Throwable  , 245:java/lang/Throwable  ]
-TRY 219:[230:java/lang/Throwable  , 245:java/lang/Throwable  ]
-TRY 223:[230:java/lang/Throwable  , 245:java/lang/Throwable  ]
-TRY 224:[230:java/lang/Throwable  , 245:java/lang/Throwable  ]
-TRY 226:[230:java/lang/Throwable  , 245:java/lang/Throwable  ]
-TRY 228:[230:java/lang/Throwable  , 245:java/lang/Throwable  ]
-TRY 232:[230:java/lang/Throwable  , 245:java/lang/Throwable  ]
-TRY 234:[245:java/lang/Throwable  ]
-ALOAD 0
-IFNONNULL L4
-CONST Integer:0
-IRETURN
-ALOAD 1
-IFNONNULL L8
-CONST Integer:0
-IRETURN
-ILOAD 2
-IFGT L12
+TRY 100:[223:java/lang/Throwable  ]
+TRY 101:[223:java/lang/Throwable  ]
+TRY 105:[209:java/lang/Throwable  , 223:java/lang/Throwable  ]
+TRY 108:[200:java/lang/Throwable  , 209:java/lang/Throwable  , 223:java/lang/Throwable  ]
+TRY 112:[200:java/lang/Throwable  , 209:java/lang/Throwable  , 223:java/lang/Throwable  ]
+TRY 117:[200:java/lang/Throwable  , 209:java/lang/Throwable  , 223:java/lang/Throwable  ]
+TRY 119:[223:java/lang/Throwable  ]
+TRY 120:[223:java/lang/Throwable  ]
+TRY 130:[200:java/lang/Throwable  , 209:java/lang/Throwable  , 223:java/lang/Throwable  ]
+TRY 132:[192:java/lang/Throwable  , 200:java/lang/Throwable  , 209:java/lang/Throwable  , 223:java/lang/Throwable  ]
+TRY 136:[192:java/lang/Throwable  , 200:java/lang/Throwable  , 209:java/lang/Throwable  , 223:java/lang/Throwable  ]
+TRY 140:[192:java/lang/Throwable  , 200:java/lang/Throwable  , 209:java/lang/Throwable  , 223:java/lang/Throwable  ]
+TRY 144:[192:java/lang/Throwable  , 200:java/lang/Throwable  , 209:java/lang/Throwable  , 223:java/lang/Throwable  ]
+TRY 149:[192:java/lang/Throwable  , 200:java/lang/Throwable  , 209:java/lang/Throwable  , 223:java/lang/Throwable  ]
+TRY 151:[200:java/lang/Throwable  , 209:java/lang/Throwable  , 223:java/lang/Throwable  ]
+TRY 153:[223:java/lang/Throwable  ]
+TRY 154:[223:java/lang/Throwable  ]
+TRY 161:[192:java/lang/Throwable  , 200:java/lang/Throwable  , 209:java/lang/Throwable  , 223:java/lang/Throwable  ]
+TRY 167:[192:java/lang/Throwable  , 200:java/lang/Throwable  , 209:java/lang/Throwable  , 223:java/lang/Throwable  ]
+TRY 171:[192:java/lang/Throwable  , 200:java/lang/Throwable  , 209:java/lang/Throwable  , 223:java/lang/Throwable  ]
+TRY 174:[192:java/lang/Throwable  , 200:java/lang/Throwable  , 209:java/lang/Throwable  , 223:java/lang/Throwable  ]
+TRY 177:[192:java/lang/Throwable  , 200:java/lang/Throwable  , 209:java/lang/Throwable  , 223:java/lang/Throwable  ]
+TRY 178:[192:java/lang/Throwable  , 200:java/lang/Throwable  , 209:java/lang/Throwable  , 223:java/lang/Throwable  ]
+TRY 186:[192:java/lang/Throwable  , 200:java/lang/Throwable  , 209:java/lang/Throwable  , 223:java/lang/Throwable  ]
+TRY 188:[192:java/lang/Throwable  , 200:java/lang/Throwable  , 209:java/lang/Throwable  , 223:java/lang/Throwable  ]
+TRY 190:[192:java/lang/Throwable  , 200:java/lang/Throwable  , 209:java/lang/Throwable  , 223:java/lang/Throwable  ]
+TRY 194:[192:java/lang/Throwable  , 200:java/lang/Throwable  , 209:java/lang/Throwable  , 223:java/lang/Throwable  ]
+TRY 196:[200:java/lang/Throwable  , 209:java/lang/Throwable  , 223:java/lang/Throwable  ]
+TRY 198:[200:java/lang/Throwable  , 209:java/lang/Throwable  , 223:java/lang/Throwable  ]
+TRY 202:[200:java/lang/Throwable  , 209:java/lang/Throwable  , 223:java/lang/Throwable  ]
+TRY 204:[209:java/lang/Throwable  , 223:java/lang/Throwable  ]
+TRY 206:[223:java/lang/Throwable  ]
+TRY 207:[223:java/lang/Throwable  ]
+TRY 211:[223:java/lang/Throwable  ]
+TRY 212:[223:java/lang/Throwable  ]
+TRY 214:[223:java/lang/Throwable  ]
+ALOAD 0
+IFNULL L6
+ALOAD 1
+IFNULL L6
+ILOAD 2
+IFGT L8
@@ -60 +57 @@
-IFNE L20
+IFNE L16
@@ -70,7 +67,3 @@
-IFNONNULL L30
-CONST Integer:1
-IRETURN
-ALOAD 5
-IFNONNULL L34
-CONST Integer:1
-IRETURN
+IFNULL L42
+ALOAD 5
+IFNULL L42
@@ -80,3 +73 @@
-IFNE L40
-CONST Integer:1
-IRETURN
+IFEQ L42
@@ -86,3 +77 @@
-IFNE L46
-CONST Integer:1
-IRETURN
+IFEQ L42
@@ -92,3 +81 @@
-IFNE L52
-CONST Integer:1
-IRETURN
+IFEQ L42
@@ -98 +85 @@
-IFGT L58
+IFGT L44
@@ -106 +93 @@
-IFNONNULL L70
+IFNONNULL L56
@@ -138 +125 @@
-IFNONNULL L98
+IFNONNULL L84
@@ -153 +140 @@
-IFNONNULL L113
+IFNONNULL L99
@@ -159,0 +147 @@
+DUP
@@ -161,3 +148,0 @@
-ALOAD 15
-DUP
-ASTORE 16
@@ -168 +153 @@
-IFEQ L130
+IFEQ L114
@@ -172,15 +157,13 @@
-IFNE L145
-CONST Integer:1
-ISTORE 17
-ALOAD 11
-GETFIELD local/lito381/server/NativePlayerCommit$Queue.vitalLock:Ljava/util/concurrent/locks/ReentrantLock;
-INVOKEVIRTUAL java/util/concurrent/locks/ReentrantLock.unlock()V
-ILOAD 17
-ISTORE 18
-ALOAD 16
-MONITOREXIT
-ALOAD 14
-INVOKEVIRTUAL local/lito381/server/NativePlayerVitalCompletions$Receipt.close()V
-ALOAD 1
-INVOKESTATIC local/lito381/server/NativePlayerVitalCompletions.drain(Llocal/lito381/server/ClientSession;)V
-ILOAD 18
+IFNE L127
+CONST Integer:1
+ISTORE 16
+ALOAD 15
+MONITOREXIT
+ALOAD 11
+GETFIELD local/lito381/server/NativePlayerCommit$Queue.vitalLock:Ljava/util/concurrent/locks/ReentrantLock;
+INVOKEVIRTUAL java/util/concurrent/locks/ReentrantLock.unlock()V
+ALOAD 14
+INVOKEVIRTUAL local/lito381/server/NativePlayerVitalCompletions$Receipt.close()V
+ALOAD 1
+INVOKESTATIC local/lito381/server/NativePlayerVitalCompletions.drain(Llocal/lito381/server/ClientSession;)V
+ILOAD 16
@@ -189,12 +172,2 @@
-ASTORE 19
-ALOAD 16
-MONITOREXIT
-GOTO L155
-ASTORE 20
-ALOAD 16
-MONITOREXIT
-ALOAD 20
-ATHROW
-ALOAD 19
-DUP
-ASTORE 21
+DUP
+ASTORE 17
@@ -204 +177 @@
-IFNE L174
+IFNE L146
@@ -208 +181 @@
-IFEQ L174
+IFEQ L146
@@ -212 +185 @@
-IFLE L174
+IFLE L146
@@ -216,15 +189,15 @@
-IFNE L189
-CONST Integer:1
-ISTORE 22
-ALOAD 11
-GETFIELD local/lito381/server/NativePlayerCommit$Queue.vitalLock:Ljava/util/concurrent/locks/ReentrantLock;
-INVOKEVIRTUAL java/util/concurrent/locks/ReentrantLock.unlock()V
-ILOAD 22
-ISTORE 23
-ALOAD 21
-MONITOREXIT
-ALOAD 14
-INVOKEVIRTUAL local/lito381/server/NativePlayerVitalCompletions$Receipt.close()V
-ALOAD 1
-INVOKESTATIC local/lito381/server/NativePlayerVitalCompletions.drain(Llocal/lito381/server/ClientSession;)V
-ILOAD 23
+IFNE L161
+CONST Integer:1
+ISTORE 18
+ALOAD 17
+MONITOREXIT
+ALOAD 15
+MONITOREXIT
+ALOAD 11
+GETFIELD local/lito381/server/NativePlayerCommit$Queue.vitalLock:Ljava/util/concurrent/locks/ReentrantLock;
+INVOKEVIRTUAL java/util/concurrent/locks/ReentrantLock.unlock()V
+ALOAD 14
+INVOKEVIRTUAL local/lito381/server/NativePlayerVitalCompletions$Receipt.close()V
+ALOAD 1
+INVOKESTATIC local/lito381/server/NativePlayerVitalCompletions.drain(Llocal/lito381/server/ClientSession;)V
+ILOAD 18
@@ -239,2 +212,2 @@
-ASTORE 24
-ALOAD 24
+ASTORE 19
+ALOAD 19
@@ -247 +220 @@
-ALOAD 24
+ALOAD 19
@@ -257,0 +231,41 @@
+ALOAD 19
+INVOKESTATIC local/lito381/server/NativeEffectCompletions.healing(Llocal/lito381/server/NativePlayerVitalCompletions$Receipt;Llocal/lito381/server/ClientSession;Llocal/lito381/server/NativePlayerCommit$Binding;Llocal/lito381/server/ClientSession;Llocal/lito381/server/NativePlayerCommit$Binding;IILjava/lang/Object;)V
+ALOAD 17
+MONITOREXIT
+GOTO L197
+ASTORE 20
+ALOAD 17
+MONITOREXIT
+ALOAD 20
+ATHROW
+ALOAD 15
+MONITOREXIT
+GOTO L205
+ASTORE 21
+ALOAD 15
+MONITOREXIT
+ALOAD 21
+ATHROW
+ALOAD 11
+GETFIELD local/lito381/server/NativePlayerCommit$Queue.vitalLock:Ljava/util/concurrent/locks/ReentrantLock;
+INVOKEVIRTUAL java/util/concurrent/locks/ReentrantLock.unlock()V
+GOTO L215
+ASTORE 22
+ALOAD 11
+GETFIELD local/lito381/server/NativePlayerCommit$Queue.vitalLock:Ljava/util/concurrent/locks/ReentrantLock;
+INVOKEVIRTUAL java/util/concurrent/locks/ReentrantLock.unlock()V
+ALOAD 22
+ATHROW
+CONST Integer:1
+ISTORE 23
+ALOAD 14
+INVOKEVIRTUAL local/lito381/server/NativePlayerVitalCompletions$Receipt.close()V
+ALOAD 1
+INVOKESTATIC local/lito381/server/NativePlayerVitalCompletions.drain(Llocal/lito381/server/ClientSession;)V
+ILOAD 23
+IRETURN
+ASTORE 24
+ALOAD 14
+INVOKEVIRTUAL local/lito381/server/NativePlayerVitalCompletions$Receipt.close()V
+ALOAD 1
+INVOKESTATIC local/lito381/server/NativePlayerVitalCompletions.drain(Llocal/lito381/server/ClientSession;)V
@@ -259,36 +273 @@
-INVOKESTATIC local/lito381/server/NativeEffectCompletions.healing(Llocal/lito381/server/NativePlayerVitalCompletions$Receipt;Llocal/lito381/server/ClientSession;Llocal/lito381/server/NativePlayerCommit$Binding;Llocal/lito381/server/ClientSession;Llocal/lito381/server/NativePlayerCommit$Binding;IILjava/lang/Object;)V
-ALOAD 11
-GETFIELD local/lito381/server/NativePlayerCommit$Queue.vitalLock:Ljava/util/concurrent/locks/ReentrantLock;
-INVOKEVIRTUAL java/util/concurrent/locks/ReentrantLock.unlock()V
-GOTO L227
-ASTORE 25
-ALOAD 11
-GETFIELD local/lito381/server/NativePlayerCommit$Queue.vitalLock:Ljava/util/concurrent/locks/ReentrantLock;
-INVOKEVIRTUAL java/util/concurrent/locks/ReentrantLock.unlock()V
-ALOAD 25
-ATHROW
-ALOAD 21
-MONITOREXIT
-GOTO L235
-ASTORE 26
-ALOAD 21
-MONITOREXIT
-ALOAD 26
-ATHROW
-CONST Integer:1
-ISTORE 27
-ILOAD 27
-ISTORE 28
-ALOAD 14
-INVOKEVIRTUAL local/lito381/server/NativePlayerVitalCompletions$Receipt.close()V
-ALOAD 1
-INVOKESTATIC local/lito381/server/NativePlayerVitalCompletions.drain(Llocal/lito381/server/ClientSession;)V
-ILOAD 28
-IRETURN
-ASTORE 29
-ALOAD 14
-INVOKEVIRTUAL local/lito381/server/NativePlayerVitalCompletions$Receipt.close()V
-ALOAD 1
-INVOKESTATIC local/lito381/server/NativePlayerVitalCompletions.drain(Llocal/lito381/server/ClientSession;)V
-ALOAD 29
-ATHROW
+ATHROW
```

</details>
