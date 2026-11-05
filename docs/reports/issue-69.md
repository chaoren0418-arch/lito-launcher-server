# Issue #69 — T020-B08 戰鬥併入暫存

## 結論

已完成本輪 Java 修補及完整重驗；**仍未通過整批語意驗收，不可交付 B-land**。

30 個 staged Java 用 JDK 17 編譯成功，經 `@AsRecord` 轉換後恰好 160 個 class entry，沒有缺漏或多出。結果：**SAME 141、SAME_PATHS 9、DIFF 10、ONLY_LEFT 0、ONLY_RIGHT 0**。相較前次 16 個 DIFF，減少 6 個；不是將剩餘差異豁免成通過。

分支 `task/69`；本輪起點 `e274b35`，沿用原 worktree，未 checkout／另建分支。合併 `origin/main` 的提交為 `e389785`（主線 `295c19c`），取得已合併的 #186 工具。PR 為 [#94](https://github.com/chaoren0418-arch/lito-launcher-server/pull/94)。

Java 修正提交 `2fe58f6` 已用 `git push -u origin task/69` 成功推送。PR 標題與描述已透過 GitHub 連接器更新為本輪結果。依規則先寫 UTF-8 `scratch/issue-69/comment.md`，再執行 `gh issue comment 69 --body-file ...`，但 gh 回覆 HTTP 401，因此沒有新增 Issue 留言；`needs:claude` 標籤已由連接器成功加入。`gh pr edit --body-file ...` 亦為 401，PR 描述改由連接器以同一份預備文字更新成功。

依 [主控裁定](https://github.com/chaoren0418-arch/lito-launcher-server/issues/69#issuecomment-6666508373) 接受 SAME_PATHS；依 [最後更正](https://github.com/chaoren0418-arch/lito-launcher-server/issues/69#issuecomment-6666511561)，不等 #203／#216 才改 Java，現有工具未證明的方法列為待重驗。Issue 已成功加上 `needs:claude`。不是因迴圈／路徑上限而推定等價。

## 已驗證：做了什麼

- `NativeProcHealingPlayer.nativeSourceEquipmentProcOriginalApply`：恢復巢狀 status／waterLife monitor，完整涵蓋 Target 建立、核心 apply、completion 登記；`vitalLock.lock()` 成功後的所有檢查由 finally 保護。該方法及整個外層類別現為 **SAME**。
- `NativePlayerCommit.assign`／`nativeCompanionSceneAssign`：同步區段亦納入帶 failure 的 action-lock 解鎖保護；`nativeStaticOriginalAdvance`／`nativeCompanionSceneOriginalAdvance`：恢復原本只剩 CFR 註解的 monitor，以及涵蓋完整工作區段的例外解鎖。這四個方法現已通過。
- `NativePlayerCommit.nativeSourceScalarQueue`／`nativeSourceProc68Queue`：`queueValue` 移至 monitor 釋放後，與候選一致；`calculateWorld` 改以 double 裝箱傳入宣告為 `(DJ)` 的反射呼叫。這三個方法現已通過。
- `NativePlayerCommit.drain`：stale 分支不再落入 failed 計數／錯誤列印；仍有控制流程排列差異，保留 DIFF。`earlyZero` 移除多餘的局部變數複本後通過。
- `ClassicWorldMonsterPopulation.npcPoison`：switch 表達式回存實際效果結果，修正先前局部 `bl3` 遺失結果的問題；兩個 cancellation spawn 方法改在占位與 actors 登記成功後才遞增 ID。這三個方法現已通過。
- `LegacyMySqlRepository.rewardArmorDefinition`：恢復整段 ResultSet try-with-resources，讀取欄位／建立回傳值失敗時也會 close，保留 suppressed exception 與外層 Statement 關閉順序；該方法已通過。
- `NativeLightAction.commitSelf`：lambda 全程使用新取得的 `ingress2`，修正錯誤捕獲舊 `ingress`（可能為 null）；該方法已通過。Batch 的純轉拋 helper 改放在呼叫者同類，三處新增 catch 只包住 rescue／finish 個別呼叫，helper 本體只有 `throw (E) failure;`。
- 移除 CFR 造成的多餘 cast、monitor 暫存、錯誤靜態型別及 lambda 捕獲型別；`NativeCancellationNpcStatic` 與 `NativeEquipmentDefense` 原本多筆 lambda 差異消除；三種 Healing graphics、`NativeDeItemDefinitionsSql` 外層及 Lease、`NativeProcHealingCompanion` 現已通過。
- 保留 30 份 staged 原始碼、19 份 DECOMPILED 標頭、五個帶額外欄位的 `@AsRecord`，以及既有普通 `NativePlayerCommit.Event` 的轉換方式；轉換器為 scanned=160、converted=6。
- `NativeLightAction.Output` 仍是 package-private，該 entry 為 SAME；B03 可引用 staged Java 作為編譯依賴，但本批尚未整批驗收完成。
- 本輪只編譯／讀取 class，未執行 staged 程式，沒有連 DB 或啟動伺服器／登入器／遊戲。沒有新增 stub、覆蓋候選 class 或修改比對工具。
- 手工變更限本批 10 個 Java 與本報告；`src/main`、`pom.xml`、`docs/REPORT.md` 相對同步後主線無本任務差異。同步主線帶入其他已合併文件，不是本輪手工改寫。原始專案檔案只讀，只有 git worktree 共用 `.git` 中繼資料因 fetch／merge／commit／push 更新。

## 怎麼驗證

JDK：`C:/Program Files/Microsoft/jdk-17.0.17.10-hotspot/bin`。以下路徑以伺服器目錄為基準，`<repo>` 指本 worktree。

```text
javac -J-Dfile.encoding=UTF-8 --release 17 -encoding UTF-8
  -cp "<ASM 9.7 asm;asm-tree;asm-analysis>"
  -d target/issue-69/tools
  <tools/semantic-diff/src/main/java/**/*.java> <tools/as-record/src/main/java/**/*.java>
退出碼 0，無診斷輸出

javac -J-Dfile.encoding=UTF-8 --release 17 -encoding UTF-8
  -d target/issue-69/annotations
  src/main/java/local/lito381/server/AsRecord.java
  src/main/java/local/lito381/server/AsmFinal.java
退出碼 0，無診斷輸出

javac -J-Dfile.encoding=UTF-8 --release 17 -encoding UTF-8
  -cp "reference/candidate-50635f7e.jar;reference/base-0.6.71.jar;lib/*;target/issue-69/annotations;D:/dev-cache/maven/repository/com/zaxxer/HikariCP/5.1.0/HikariCP-5.1.0.jar"
  -d target/issue-69/classes @<repo>/scratch/issue-69/javac.args
退出碼 0
Note: Some input files use unchecked or unsafe operations.
Note: Recompile with -Xlint:unchecked for details.

java -cp "target/issue-69/tools;<ASM classpath>"
  local.lito381.asrecord.AsRecord target/issue-69/classes
as-record: scanned=160 converted=6
退出碼 0

# 僅打包 classes/ 所有 .class，沒有加入 annotations、工具或候選 bytes。
java -Dfile.encoding=UTF-8 -Xmx2g -cp "target/issue-69/tools;<ASM classpath>"
  local.lito381.semanticdiff.SemanticDiff
  --left target/issue-69/b08.jar
  --left-classpath reference/candidate-50635f7e.jar
  --right reference/candidate-50635f7e.jar
  --classes <repo>/scratch/issue-69/entries.txt
  --access-widen <repo>/scratch/issue-69/access-widen-approved.txt
  --extra-methods <repo>/scratch/issue-69/extra-methods-approved.txt
  --exceptions-exempt <repo>/scratch/issue-69/exceptions-exempt.txt
  --json <repo>/scratch/issue-69/semantic.json
  --md <repo>/scratch/issue-69/semantic.md
SUMMARY SAME=141 SAME_EFFECTS=0 SAME_PATHS=9 ORPHAN_IGNORED=0 SAME_LAYOUT=0 DEVIATION=0 DIFF=10 ONLY_LEFT=0 ONLY_RIGHT=0 BYTE_SAME=1 ALLOW_METHODS=0
退出碼 0（僅工具完成，並非整批通過）
```

另用 ZIP entry 集合獨立核對 B08.verify_entries：160=160、missing=0、extra=0；工具 classSelection 同樣為空。`javac.args` 是 B08.classes 對應 30 個 Java 的 UTF-8 無 BOM argfile，每個絕對路徑加雙引號；`entries.txt` 逐行取自 B08.verify_entries。既有 `check.py` 會重建 argfile；`full_dump.py` 會重用該檔編譯診斷工具，重跑批次須先由 check.py 產生，不可直接沿用最後的 argfile。

ASM asm／asm-tree 使用 Maven 快取 9.7，asm-analysis 使用本 worktree `target/issue-69/asm-analysis-9.7.jar`。worktree 沒有 lib/，額外使用既有 HikariCP 快取，未改依賴或 pom。註解 class 不進 B08 產物。

### 核准清單與 checked exception 證據

任務限定只交付 staged Java 與本報告，所以沒有改共用核准清單。下列精確條目依主控核准連結寫入 scratch 的同名清單，且完整保留於本報告，供重驗及 B-land 併入共用清單。沒有新增 effects／layout／deviation／metadata 豁免。

`access-widen-approved.txt` 是 `docs/combat-merge/access-widen-approved.txt` 的 B08 適用條目；整份清單包含其他批次，直接傳入會因未選取類別而被工具拒絕。

```text
local/lito381/server/NativeLightAction$Output # approved: https://github.com/chaoren0418-arch/lito-launcher-server/issues/64#issuecomment-6654963452
```

`extra-methods-approved.txt`：

```text
local/lito381/server/NativeLightAction$Batch#rethrowCandidateFailure(Ljava/lang/Throwable;)V # approved: https://github.com/chaoren0418-arch/lito-launcher-server/issues/69#issuecomment-6666508373
```

`exceptions-exempt.txt`（只豁免 throws 屬性，方法本體仍完整比對）：

```text
local/lito381/server/LegacyMySqlRepository#healthCheck()V # approved: https://github.com/chaoren0418-arch/lito-launcher-server/issues/69#issuecomment-6666508373
local/lito381/server/LegacyMySqlRepository#persistTournamentProfile(JLjava/lang/String;Llocal/lito381/server/RepositoryTournamentProfile;Llocal/lito381/server/GameStateStore$TournamentProfileState;)Llocal/lito381/server/RepositoryTournamentMutation; # approved: https://github.com/chaoren0418-arch/lito-launcher-server/issues/69#issuecomment-6666508373
local/lito381/server/LegacyMySqlRepository#tournamentProfiles()Llocal/lito381/server/RepositoryTournamentRoster; # approved: https://github.com/chaoren0418-arch/lito-launcher-server/issues/69#issuecomment-6666508373
```

把三個方法宣告暫改回候選的 `throws SQLException`，只在 scratch 副本執行 javac，取得下列失敗；正式 staged 保留可編譯的 `throws Exception`。沒有將 probe 產物放入驗收 JAR。

```text
D:\lineage開發\3-lito開發\scratch\wt\task-69\scratch\issue-69\checked-probe\LegacyMySqlRepository.java:239: error: unreported exception Exception; must be caught or declared to be thrown
        SharedDatabase.check(this);
                            ^
D:\lineage開發\3-lito開發\scratch\wt\task-69\scratch\issue-69\checked-probe\LegacyMySqlRepository.java:5202: error: unreported exception Exception; must be caught or declared to be thrown
        if (SharedDatabase.registered(this)) return SharedDatabase.profiles(this);
                                                                           ^
D:\lineage開發\3-lito開發\scratch\wt\task-69\scratch\issue-69\checked-probe\LegacyMySqlRepository.java:5246: error: unreported exception Exception; must be caught or declared to be thrown
        if (SharedDatabase.registered(this)) return SharedDatabase.persist(this, characterObjectId, characterName, expected, replacement);
                                                                          ^
3 errors
javac exit code 1
```

套用三個精確 Exceptions 條目後，`LegacyMySqlRepository` 整個類別為 SAME_PATHS，沒有剩餘方法 DIFF。helper 經工具核准，Batch 的方法差異仍保留，未藉核准 helper 略過本體。

### 治療方法 javap 鎖定結構對照

執行 `javap -p -c -s -classpath <staged 或 candidate JAR> local.lito381.server.NativeProcHealingPlayer`，擷取 `nativeSourceEquipmentProcOriginalApply`。

| 操作 | staged offset | candidate offset |
|---|---|---|
| vitalLock.lock | 212 | 212 |
| status monitorenter | 220 | 220 |
| waterLife monitorenter | 269 | 269 |
| 建立 Target／呼叫建構子 | 333／344 | 333／344 |
| 核心 apply | 372 | 372 |
| 登記 healing completion | 391 | 391 |
| 正常釋放 waterLife／status | 396／410 | 396／410 |
| 正常 vitalLock.unlock | 427 | 427 |
| finally vitalLock.unlock | 440 | 440 |
| receipt close／drain（正常） | 451／455 | 451／455 |
| receipt close／drain（例外） | 465／469 | 465／469 |

兩側均 2 個 monitorenter、7 個 monitorexit。以下 exception table **兩側逐字相同**；400 為 waterLife handler、414 為 status handler、433 為 vitalLock finally、461 為 receipt finally。第一個 monitor 前的 `215..244` 等區段亦受 vitalLock finally 保護。正常及提早返回均先釋放內層 monitor、外層 monitor，再 unlock，最後 close／drain。

```text
       from    to  target type
         270   310   400   any
         333   397   400   any
         400   405   400   any
         221   244   414   any
         264   313   414   any
         333   411   414   any
         414   419   414   any
         215   244   433   any
         264   313   433   any
         333   422   433   any
         433   435   433   any
         207   252   461   any
         264   321   461   any
         333   449   461   any
         461   463   461   any
```

已以程式核對兩側所有 monitor、鎖／檢查／Target／apply／completion／close／drain 指令 offset 及 exception table，在移除常數池編號與空白差異後完全一致。整段 javap 仍有局部槽位與常數池編號差異，因此沒有宣稱 byte-identical；正式 semantic-diff 將該整類列為 SAME。

### 輸入與產物 SHA-256

- `reference/base-0.6.71.jar`：`e1498871c7be6b287821c57ef5d95933db1740d9b4d4a80f6c3465f21d99d498`
- `reference/candidate-50635f7e.jar`：`50635f7edca8ec6ce77787cc32371c7590b497e48ea202f8a679caf3f517b729`
- `target/issue-69/b08.jar`：`88425cbf5dd6bd5e2881dbac1c838d2c56829e0ef80c2a3f9f26956aabc9d77b`

## 推測

剩餘差異的人工檢查顯示主要是 CFG 排列、lambda 捕獲排列／跳轉與純轉拋 handler；這只是後續工具驗證方向，不是等價證明。`NativePlayerCommit.nativeManaTransferRecover` 候選還有 `ALOAD clientSession; POP`，現工具未能證明；`NativeCombatAttacker.calculated` 的 Object／Ref.equals owner 依裁定保留原 Java。

## 待驗證／未完成

- 剩餘 10 個 DIFF entry 必須在 #203／#216 合併後重驗；未通過者繼續逐方法修補，不得把本報告當成全部等價證明。
- 主控整合本報告列出的 1 個 helper 與 3 個 Exceptions 精確核准條目，再安排 B-land 共用驗收；本輪遵守路徑範圍而只在 scratch 套用。
- 單元測試／遊戲回歸不適用本 staged 批次，依設計在 B-land 統一執行；未執行實機驗收或部署。

## 產出路徑與給下一棒的提示

- 來源：`lito伺服器/LiTo381-Server/merge-inputs/50635f7e/staged/local/lito381/server/`，保留 B08 全部 30 份，本輪修改 10 份。
- 報告：`docs/reports/issue-69.md`，本檔取代過期裁定與舊差異。
- 本機證據：`scratch/issue-69/` 的 check.py、approve.py、semantic.json/md、final-dump/、checked-probe-compile.log、staged/candidate-healing-javap.txt、audit.txt；建置產物只在 `lito伺服器/LiTo381-Server/target/issue-69/`，均未入 git。
- B03（#64）可取 staged `NativeLightAction.java`；Output 的 package-private 保留，AsRecord 註解依原流程供編譯。本批不能因該介面通過就整批落地。
- 下一棒由 Claude 主控審查實際 Java 修復、核准條目與下列完整差異，再由本機執行器使用 #203／#216 新工具重驗全部 160 entries。

## 剩餘差異：逐方法交接與完整證據

下方保留每個 DIFF entry 的全部 methodResults（含已成功的後備證明），並附所有仍有差異的方法完整正規化差異。未能配對的 lambda 以各側完整方法表示保留，沒有只列第一條差異。清單內 9 個 SAME_PATHS 依主控契約算通過，未列為未完成。

SAME_PATHS：`ClassicWorldMonsterPopulation$Snapshot`、`LegacyMySqlRepository`、`NativeArmorState`、`NativeCombatOrigin`、`NativeMassTeleportAnswerLife`、`NativePlayerActionScope`、`NativePlayerCommit$Binding`、`NativePlayerCommit$Queue`、`NativePlayerVitalCompletions$Receipt`。

### ClassicWorldMonsterPopulation — DIFF；待 #203（CFG／迴圈與資料流）

<details>
<summary>全部方法診斷</summary>

```json
{
  "name": "local/lito381/server/ClassicWorldMonsterPopulation",
  "status": "DIFF",
  "byteSame": false,
  "allowMethodsApplied": false,
  "methodResults": [
    {
      "method": "damage(IJLlocal/lito381/server/ClassicWorldMonsterPopulation$Contribution;I)Llocal/lito381/server/ClassicWorldMonsterPopulation$Hit;",
      "status": "SAME_PATHS",
      "detail": "path multisets equal (528)"
    },
    {
      "method": "idleMove(IJLjava/util/Set;JII)Llocal/lito381/server/ClassicWorldMonsterPopulation$IdleMovement;",
      "status": "DIFF",
      "detail": "method idleMove(IJLjava/util/Set;JII)Llocal/lito381/server/ClassicWorldMonsterPopulation$IdleMovement; insn[308]: left=IF_ICMPGE L427 right=IF_ICMPGE L428; paths rejected: normal control-flow cycle"
    },
    {
      "method": "itemAction(IJLjava/util/List;Ljava/util/Set;JI)Llocal/lito381/server/ClassicWorldMonsterPopulation$ItemAction;",
      "status": "DIFF",
      "detail": "method itemAction(IJLjava/util/List;Ljava/util/Set;JI)Llocal/lito381/server/ClassicWorldMonsterPopulation$ItemAction; insn[200]: left=IF_ICMPGE L330 right=IF_ICMPGE L331; paths rejected: normal control-flow cycle"
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
      "detail": "method returnGuardHome(IJLjava/util/Set;J)Llocal/lito381/server/ClassicWorldMonsterPopulation$GuardHomecoming; insn[74]: left=IFLT L236 right=IFLT L237; paths rejected: normal control-flow cycle"
    }
  ],
  "effectsMethods": [],
  "approvedMethods": [],
  "firstDiff": {
    "where": "method",
    "member": "idleMove(IJLjava/util/Set;JII)Llocal/lito381/server/ClassicWorldMonsterPopulation$IdleMovement;",
    "insnIndex": 308,
    "detail": "left=IF_ICMPGE L427 right=IF_ICMPGE L428; paths rejected: normal control-flow cycle"
  }
}
```

</details>

<details>
<summary>完整正規化差異（staged → candidate）</summary>

```diff
--- staged/ClassicWorldMonsterPopulation/idleMove-eb9151bf.txt
+++ candidate/ClassicWorldMonsterPopulation/idleMove-eb9151bf.txt
@@ -314,7 +314,7 @@
 ISTORE 19
 ILOAD 19
 ILOAD 18
-IF_ICMPGE L427
+IF_ICMPGE L428
 ALOAD 17
 ILOAD 19
 IALOAD
@@ -348,11 +348,11 @@
 ILOAD 21
 ILOAD 22
 INVOKEVIRTUAL local/lito381/server/ClassicWorldMapCatalog$ClassicMap.canStep(IIII)Z
-IFEQ L425
+IFEQ L426
 ALOAD 0
 ALOAD 23
 INVOKESPECIAL local/lito381/server/ClassicWorldMonsterPopulation.isOccupied(Llocal/lito381/server/ClassicWorldMonsterPopulation$Cell;)Z
-IFNE L425
+IFNE L426
 ALOAD 4
 NEW local/lito381/server/ClassicWorldMonsterPopulation$GridPoint
 DUP
@@ -360,13 +360,14 @@
 ILOAD 22
 INVOKESPECIAL local/lito381/server/ClassicWorldMonsterPopulation$GridPoint.<init>(II)V
 INVOKEINTERFACE java/util/Set.contains(Ljava/lang/Object;)Z itf
-IFNE L425
+IFNE L426
 ILOAD 1
 ILOAD 21
 ILOAD 22
 ALOAD 0
 INVOKESTATIC local/lito381/server/NativeStaticNpcMotion.occupies(IIILlocal/lito381/server/ClassicWorldMonsterPopulation;)Z
-IFNE L425
+IFEQ L362
+GOTO L426
 ALOAD 9
 INVOKEVIRTUAL local/lito381/server/ClassicWorldMonsterPopulation$Actor.snapshot()Llocal/lito381/server/ClassicWorldMonsterPopulation$Snapshot;
 ASTORE 24
--- staged/ClassicWorldMonsterPopulation/itemAction-2b3a0435.txt
+++ candidate/ClassicWorldMonsterPopulation/itemAction-2b3a0435.txt
@@ -206,7 +206,7 @@
 ISTORE 17
 ILOAD 17
 ILOAD 16
-IF_ICMPGE L330
+IF_ICMPGE L331
 ALOAD 15
 ILOAD 17
 IALOAD
@@ -240,11 +240,11 @@
 ILOAD 19
 ILOAD 20
 INVOKEVIRTUAL local/lito381/server/ClassicWorldMapCatalog$ClassicMap.canStep(IIII)Z
-IFEQ L328
+IFEQ L329
 ALOAD 0
 ALOAD 21
 INVOKESPECIAL local/lito381/server/ClassicWorldMonsterPopulation.isOccupied(Llocal/lito381/server/ClassicWorldMonsterPopulation$Cell;)Z
-IFNE L328
+IFNE L329
 ALOAD 5
 NEW local/lito381/server/ClassicWorldMonsterPopulation$GridPoint
 DUP
@@ -252,13 +252,14 @@
 ILOAD 20
 INVOKESPECIAL local/lito381/server/ClassicWorldMonsterPopulation$GridPoint.<init>(II)V
 INVOKEINTERFACE java/util/Set.contains(Ljava/lang/Object;)Z itf
-IFNE L328
+IFNE L329
 ILOAD 1
 ILOAD 19
 ILOAD 20
 ALOAD 0
 INVOKESTATIC local/lito381/server/NativeStaticNpcMotion.occupies(IIILlocal/lito381/server/ClassicWorldMonsterPopulation;)Z
-IFNE L328
+IFEQ L254
+GOTO L329
 ALOAD 9
 INVOKEVIRTUAL local/lito381/server/ClassicWorldMonsterPopulation$Actor.snapshot()Llocal/lito381/server/ClassicWorldMonsterPopulation$Snapshot;
 ASTORE 22
--- staged/ClassicWorldMonsterPopulation/pollParalyses-9bc5a531.txt
+++ candidate/ClassicWorldMonsterPopulation/pollParalyses-9bc5a531.txt
@@ -34,19 +34,20 @@
 GETFIELD local/lito381/server/ClassicWorldMonsterPopulation$Actor.definition:Llocal/lito381/server/ClassicWorldMonsterCatalog$SpawnDefinition;
 INVOKEVIRTUAL local/lito381/server/ClassicWorldMonsterCatalog$SpawnDefinition.mapId()I
 ILOAD 1
-IF_ICMPNE L80
+IF_ICMPNE L17
 ALOAD 6
 GETFIELD local/lito381/server/ClassicWorldMonsterPopulation$Actor.present:Z
-IFEQ L80
+IFEQ L17
 ALOAD 6
 GETFIELD local/lito381/server/ClassicWorldMonsterPopulation$Actor.alive:Z
-IFEQ L80
+IFNE L36
+GOTO L17
 ALOAD 6
 GETFIELD local/lito381/server/ClassicWorldMonsterPopulation$Actor.poisonKind:Llocal/lito381/server/ClassicWorldMonsterPoisonAttack$Kind;
 GETSTATIC local/lito381/server/ClassicWorldMonsterPoisonAttack$Kind.PARALYSIS:Llocal/lito381/server/ClassicWorldMonsterPoisonAttack$Kind;
-IF_ACMPNE L41
+IF_ACMPNE L42
 CONST Integer:1
-GOTO L42
+GOTO L43
 CONST Integer:0
 ISTORE 7
 ALOAD 6
@@ -56,9 +57,9 @@
 ASTORE 8
 ALOAD 8
 INVOKEVIRTUAL local/lito381/server/CombatMonsterParalysis$Poll.poisonExpired()Z
-IFEQ L56
+IFEQ L57
 ILOAD 7
-IFEQ L56
+IFEQ L57
 ALOAD 6
 ALOAD 0
 INVOKESTATIC local/lito381/server/CombatMonsterCurseLifecycle.clearSource(Ljava/lang/Object;Llocal/lito381/server/ClassicWorldMonsterPopulation;)V
@@ -67,10 +68,9 @@
 IFNE L65
 ALOAD 8
 INVOKEVIRTUAL local/lito381/server/CombatMonsterParalysis$Poll.activated()Z
-IFEQ L17
+IFEQ L80
 ILOAD 7
-IFNE L65
-GOTO L17
+IFEQ L80
 ALOAD 4
 NEW local/lito381/server/ClassicWorldMonsterPopulation$ParalysisUpdate
 DUP
--- staged/ClassicWorldMonsterPopulation/pollPoisons-37356651.txt
+++ candidate/ClassicWorldMonsterPopulation/pollPoisons-37356651.txt
@@ -39,13 +39,14 @@
 GETFIELD local/lito381/server/ClassicWorldMonsterPopulation$Actor.definition:Llocal/lito381/server/ClassicWorldMonsterCatalog$SpawnDefinition;
 INVOKEVIRTUAL local/lito381/server/ClassicWorldMonsterCatalog$SpawnDefinition.mapId()I
 ILOAD 1
-IF_ICMPNE L138
+IF_ICMPNE L22
 ALOAD 8
 GETFIELD local/lito381/server/ClassicWorldMonsterPopulation$Actor.present:Z
-IFEQ L138
+IFEQ L22
 ALOAD 8
 GETFIELD local/lito381/server/ClassicWorldMonsterPopulation$Actor.alive:Z
-IFEQ L138
+IFNE L41
+GOTO L22
 ALOAD 8
 GETFIELD local/lito381/server/ClassicWorldMonsterPopulation$Actor.poisonAttackerObjectId:J
 LSTORE 9/10
@@ -73,10 +74,10 @@
 IFNULL L22
 ALOAD 14
 INVOKEVIRTUAL local/lito381/server/BattleTimedStatus$PoisonPoll.damageTicks()I
-IFNE L72
+IFNE L73
 ALOAD 14
 INVOKEVIRTUAL local/lito381/server/BattleTimedStatus$PoisonPoll.expired()Z
-IFNE L72
+IFNE L73
 GOTO L22
 NEW java/util/ArrayList
 DUP
@@ -87,10 +88,10 @@
 ILOAD 16
 ALOAD 14
 INVOKEVIRTUAL local/lito381/server/BattleTimedStatus$PoisonPoll.damageTicks()I
-IF_ICMPGE L103
+IF_ICMPGE L104
 ALOAD 8
 GETFIELD local/lito381/server/ClassicWorldMonsterPopulation$Actor.alive:Z
-IFEQ L103
+IFEQ L104
 ALOAD 0
 ILOAD 1
 ALOAD 8
@@ -102,19 +103,19 @@
 INVOKEVIRTUAL local/lito381/server/ClassicWorldMonsterPopulation.damage(IJLlocal/lito381/server/ClassicWorldMonsterPopulation$ContributorKind;JJI)Llocal/lito381/server/ClassicWorldMonsterPopulation$Hit;
 ASTORE 17
 ALOAD 17
-IFNULL L101
+IFNULL L102
 ALOAD 15
 ALOAD 17
 INVOKEINTERFACE java/util/List.add(Ljava/lang/Object;)Z itf
 POP
 IINC var=16 incr=1
-GOTO L78
+GOTO L79
 ALOAD 15
 INVOKEINTERFACE java/util/List.isEmpty()Z itf
-IFEQ L109
+IFEQ L110
 ALOAD 8
 INVOKEVIRTUAL local/lito381/server/ClassicWorldMonsterPopulation$Actor.snapshot()Llocal/lito381/server/ClassicWorldMonsterPopulation$Snapshot;
-GOTO L117
+GOTO L118
 ALOAD 15
 ALOAD 15
 INVOKEINTERFACE java/util/List.size()I itf
@@ -139,8 +140,7 @@
 POP
 ALOAD 14
 INVOKEVIRTUAL local/lito381/server/BattleTimedStatus$PoisonPoll.expired()Z
-IFNE L135
-GOTO L22
+IFEQ L138
 ALOAD 8
 ALOAD 0
 INVOKESTATIC local/lito381/server/CombatMonsterCurseLifecycle.clearSource(Ljava/lang/Object;Llocal/lito381/server/ClassicWorldMonsterPopulation;)V
--- staged/ClassicWorldMonsterPopulation/returnGuardHome-55d0ea6f.txt
+++ candidate/ClassicWorldMonsterPopulation/returnGuardHome-55d0ea6f.txt
@@ -80,7 +80,7 @@
 INVOKESTATIC local/lito381/server/ClassicWorldMonsterPopulation.heading(IIII)I
 ISTORE 10
 ILOAD 10
-IFLT L236
+IFLT L237
 CONST Integer:5
 NEWARRAY 10
 DUP
@@ -132,7 +132,7 @@
 ISTORE 15
 ILOAD 15
 ILOAD 14
-IF_ICMPGE L236
+IF_ICMPGE L237
 ALOAD 13
 ILOAD 15
 IALOAD
@@ -166,11 +166,11 @@
 ILOAD 17
 ILOAD 18
 INVOKEVIRTUAL local/lito381/server/ClassicWorldMapCatalog$ClassicMap.canStep(IIII)Z
-IFEQ L234
+IFEQ L235
 ALOAD 0
 ALOAD 19
 INVOKESPECIAL local/lito381/server/ClassicWorldMonsterPopulation.isOccupied(Llocal/lito381/server/ClassicWorldMonsterPopulation$Cell;)Z
-IFNE L234
+IFNE L235
 ALOAD 4
 NEW local/lito381/server/ClassicWorldMonsterPopulation$GridPoint
 DUP
@@ -178,13 +178,14 @@
 ILOAD 18
 INVOKESPECIAL local/lito381/server/ClassicWorldMonsterPopulation$GridPoint.<init>(II)V
 INVOKEINTERFACE java/util/Set.contains(Ljava/lang/Object;)Z itf
-IFNE L234
+IFNE L235
 ILOAD 1
 ILOAD 17
 ILOAD 18
 ALOAD 0
 INVOKESTATIC local/lito381/server/NativeStaticNpcMotion.occupies(IIILlocal/lito381/server/ClassicWorldMonsterPopulation;)Z
-IFNE L234
+IFEQ L180
+GOTO L235
 ALOAD 7
 INVOKEVIRTUAL local/lito381/server/ClassicWorldMonsterPopulation$Actor.snapshot()Llocal/lito381/server/ClassicWorldMonsterPopulation$Snapshot;
 ASTORE 20
@@ -250,7 +251,7 @@
 INVOKESPECIAL local/lito381/server/ClassicWorldMonsterPopulation.nearestGuardHomeLanding(IIILjava/util/Set;)Llocal/lito381/server/ClassicWorldMonsterPopulation$GridPoint;
 ASTORE 21
 ALOAD 21
-IFNONNULL L252
+IFNONNULL L253
 ALOAD 7
 LLOAD 5/6
 CONST Long:250000000
```

</details>

### NativeCancellationNpcStatic — DIFF；待 #203（CFG／迴圈與資料流）

<details>
<summary>全部方法診斷</summary>

```json
{
  "name": "local/lito381/server/NativeCancellationNpcStatic",
  "status": "DIFF",
  "byteSame": false,
  "allowMethodsApplied": false,
  "methodResults": [
    {
      "method": "cast(Llocal/lito381/server/ClientSession;Llocal/lito381/server/GameHub;Llocal/lito381/server/ClassicWorldNpcCatalog$StaticNpc;Llocal/lito381/server/Protocol$ObjectSpellRequest;Llocal/lito381/server/BattleSpellCatalog$CancellationSpell;Llocal/lito381/server/NativeCancellationNpcRules$Rule;)V",
      "status": "DIFF",
      "detail": "method cast(Llocal/lito381/server/ClientSession;Llocal/lito381/server/GameHub;Llocal/lito381/server/ClassicWorldNpcCatalog$StaticNpc;Llocal/lito381/server/Protocol$ObjectSpellRequest;Llocal/lito381/server/BattleSpellCatalog$CancellationSpell;Llocal/lito381/server/NativeCancellationNpcRules$Rule;)V insn[337]: left=IFEQ L447 right=IFEQ L448; paths rejected: normal control-flow cycle"
    }
  ],
  "effectsMethods": [],
  "approvedMethods": [],
  "firstDiff": {
    "where": "method",
    "member": "cast(Llocal/lito381/server/ClientSession;Llocal/lito381/server/GameHub;Llocal/lito381/server/ClassicWorldNpcCatalog$StaticNpc;Llocal/lito381/server/Protocol$ObjectSpellRequest;Llocal/lito381/server/BattleSpellCatalog$CancellationSpell;Llocal/lito381/server/NativeCancellationNpcRules$Rule;)V",
    "insnIndex": 337,
    "detail": "left=IFEQ L447 right=IFEQ L448; paths rejected: normal control-flow cycle"
  }
}
```

</details>

<details>
<summary>完整正規化差異（staged → candidate）</summary>

```diff
--- staged/NativeCancellationNpcStatic/cast-d79ca2be.txt
+++ candidate/NativeCancellationNpcStatic/cast-d79ca2be.txt
@@ -343,7 +343,7 @@
 ASTORE 19
 ALOAD 19
 INVOKEINTERFACE java/util/Iterator.hasNext()Z itf
-IFEQ L447
+IFEQ L448
 ALOAD 19
 INVOKEINTERFACE java/util/Iterator.next()Ljava/lang/Object; itf
 CHECKCAST local/lito381/server/ClientSession
@@ -360,7 +360,7 @@
 INVOKESTATIC local/lito381/server/CombatAccess.call(Ljava/lang/Object;Ljava/lang/String;Ljava/lang/String;[Ljava/lang/Object;)Ljava/lang/Object;
 CHECKCAST java/lang/Boolean
 INVOKEVIRTUAL java/lang/Boolean.booleanValue()Z
-IFEQ L446
+IFEQ L335
 ALOAD 20
 CONST str:"positionX"
 INVOKESTATIC local/lito381/server/CombatAccess.field(Ljava/lang/Object;Ljava/lang/String;)Ljava/lang/Object;
@@ -378,16 +378,17 @@
 ALOAD 0
 INVOKESTATIC local/lito381/server/NativeStaticNpcMotion.y(Llocal/lito381/server/ClassicWorldNpcCatalog$StaticNpc;Llocal/lito381/server/ClientSession;)I
 INVOKESTATIC local/lito381/server/BattleSpellCatalog.isInNativeScreen(IIII)Z
-IFEQ L446
+IFNE L374
+GOTO L335
 ALOAD 0
 CONST str:"invisible"
 INVOKESTATIC local/lito381/server/CombatAccess.field(Ljava/lang/Object;Ljava/lang/String;)Ljava/lang/Object;
 CHECKCAST java/lang/Boolean
 INVOKEVIRTUAL java/lang/Boolean.booleanValue()Z
-IFEQ L382
-ALOAD 20
-ALOAD 0
-IF_ACMPNE L390
+IFEQ L383
+ALOAD 20
+ALOAD 0
+IF_ACMPNE L391
 ALOAD 20
 ALOAD 0
 INVOKEVIRTUAL local/lito381/server/ClientSession.playerObjectId()J
@@ -412,14 +413,14 @@
 ASTORE 21
 ALOAD 21
 INVOKEINTERFACE java/util/Iterator.hasNext()Z itf
-IFEQ L446
+IFEQ L447
 ALOAD 21
 INVOKEINTERFACE java/util/Iterator.next()Ljava/lang/Object; itf
 CHECKCAST local/lito381/server/NativeCancellationNpcTransform$Notice
 ASTORE 22
 ALOAD 22
 INSTANCEOF local/lito381/server/NativeCancellationNpcTransform$Name
-IFEQ L428
+IFEQ L429
 ALOAD 22
 CHECKCAST local/lito381/server/NativeCancellationNpcTransform$Name
 ASTORE 23
@@ -433,10 +434,10 @@
 INVOKESTATIC local/lito381/server/Protocol.changeDescription(JLjava/lang/String;)[B
 CONST str:"S_CHANGE_NAME static-Cancellation"
 INVOKESTATIC local/lito381/server/NativeCancellationNpcStatic.sendNpc(Llocal/lito381/server/ClientSession;Llocal/lito381/server/GameHub;Llocal/lito381/server/ClassicWorldNpcCatalog$StaticNpc;[BLjava/lang/String;)V
-GOTO L404
+GOTO L446
 ALOAD 22
 INSTANCEOF local/lito381/server/NativeCancellationNpcTransform$Shape
-IFEQ L445
+IFEQ L446
 ALOAD 22
 CHECKCAST local/lito381/server/NativeCancellationNpcTransform$Shape
 ASTORE 24
@@ -451,7 +452,7 @@
 INVOKESTATIC local/lito381/server/Protocol.classicStaticNpcShape(JII)[B
 CONST str:"S_POLYMORPH static-Cancellation"
 INVOKESTATIC local/lito381/server/NativeCancellationNpcStatic.sendNpc(Llocal/lito381/server/ClientSession;Llocal/lito381/server/GameHub;Llocal/lito381/server/ClassicWorldNpcCatalog$StaticNpc;[BLjava/lang/String;)V
-GOTO L404
+GOTO L405
 GOTO L335
 ALOAD 0
 CONST str:"sendInventoryDelta"
```

</details>

### NativeCombatAttacker — DIFF；待 #216（public 虛擬呼叫 owner）

<details>
<summary>全部方法診斷</summary>

```json
{
  "name": "local/lito381/server/NativeCombatAttacker",
  "status": "DIFF",
  "byteSame": false,
  "allowMethodsApplied": false,
  "methodResults": [
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
  ],
  "effectsMethods": [],
  "approvedMethods": [],
  "firstDiff": {
    "where": "method",
    "member": "calculated(Llocal/lito381/server/NativePlayerCommit$Binding;)V",
    "insnIndex": 17,
    "detail": "left=INVOKEVIRTUAL java/lang/Object.equals(Ljava/lang/Object;)Z right=INVOKEVIRTUAL local/lito381/server/NativeCombatAttacker$Ref.equals(Ljava/lang/Object;)Z; paths rejected: normal control-flow cycle"
  }
}
```

</details>

<details>
<summary>完整正規化差異（staged → candidate）</summary>

```diff
--- staged/NativeCombatAttacker/calculated-76c9af.txt
+++ candidate/NativeCombatAttacker/calculated-76c9af.txt
@@ -23,7 +23,7 @@
 GETFIELD local/lito381/server/NativeCombatAttacker$Ingress.attacker:Llocal/lito381/server/NativeCombatAttacker$Ref;
 ALOAD 2
 GETFIELD local/lito381/server/NativeCombatAttacker$Ingress.attacker:Llocal/lito381/server/NativeCombatAttacker$Ref;
-INVOKEVIRTUAL java/lang/Object.equals(Ljava/lang/Object;)Z
+INVOKEVIRTUAL local/lito381/server/NativeCombatAttacker$Ref.equals(Ljava/lang/Object;)Z
 IFEQ L26
 ALOAD 2
 ALOAD 0
```

</details>

### NativeEquipmentDefense — DIFF；待 #203（CFG／迴圈與資料流）

<details>
<summary>全部方法診斷</summary>

```json
{
  "name": "local/lito381/server/NativeEquipmentDefense",
  "status": "DIFF",
  "byteSame": false,
  "allowMethodsApplied": false,
  "methodResults": [
    {
      "method": "apply(Llocal/lito381/server/ClientSession;Llocal/lito381/server/ClientSession;Llocal/lito381/server/NativePlayerCommit$Binding;Llocal/lito381/server/NativePlayerCommit$Binding;Llocal/lito381/server/NativeEquipmentDefense$Resolved;Llocal/lito381/server/NativeCombatAttacker$Ref;)V",
      "status": "DIFF",
      "detail": "method apply(Llocal/lito381/server/ClientSession;Llocal/lito381/server/ClientSession;Llocal/lito381/server/NativePlayerCommit$Binding;Llocal/lito381/server/NativePlayerCommit$Binding;Llocal/lito381/server/NativeEquipmentDefense$Resolved;Llocal/lito381/server/NativeCombatAttacker$Ref;)V: left=35:[222:java/lang/RuntimeException  ] right=35:[224:java/lang/RuntimeException  ]; paths rejected: normal control-flow cycle"
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
  ],
  "effectsMethods": [],
  "approvedMethods": [],
  "firstDiff": {
    "where": "method",
    "member": "apply(Llocal/lito381/server/ClientSession;Llocal/lito381/server/ClientSession;Llocal/lito381/server/NativePlayerCommit$Binding;Llocal/lito381/server/NativePlayerCommit$Binding;Llocal/lito381/server/NativeEquipmentDefense$Resolved;Llocal/lito381/server/NativeCombatAttacker$Ref;)V",
    "insnIndex": null,
    "detail": "left=35:[222:java/lang/RuntimeException  ] right=35:[224:java/lang/RuntimeException  ]; paths rejected: normal control-flow cycle"
  }
}
```

</details>

<details>
<summary>完整正規化差異（staged → candidate）</summary>

```diff
--- staged/NativeEquipmentDefense/apply-cae6685.txt
+++ candidate/NativeEquipmentDefense/apply-cae6685.txt
@@ -5,78 +5,78 @@

 ||||pvis=|pinv=|lvtvis=|lvtinv=

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
 ALOAD 0
 INVOKESTATIC local/lito381/server/NativePlayerCommit.validFrame(Llocal/lito381/server/ClientSession;)Z
 IFNE L4
@@ -86,25 +86,25 @@
 POP2
 ALOAD 0
 INVOKESTATIC local/lito381/server/NativeEquipmentDefense.alive(Llocal/lito381/server/ClientSession;)Z
-IFEQ L241
+IFEQ L243
 ALOAD 4
 GETFIELD local/lito381/server/NativeEquipmentDefense$Resolved.effects:Ljava/util/List;
 INVOKEINTERFACE java/util/List.iterator()Ljava/util/Iterator; itf
 ASTORE 6
 ALOAD 6
 INVOKEINTERFACE java/util/Iterator.hasNext()Z itf
-IFEQ L227
+IFEQ L229
 ALOAD 6
 INVOKEINTERFACE java/util/Iterator.next()Ljava/lang/Object; itf
 CHECKCAST local/lito381/server/NativeEquipmentDefense$Effect
 ASTORE 7
 ALOAD 0
 INVOKESTATIC local/lito381/server/NativePlayerCommit.validFrame(Llocal/lito381/server/ClientSession;)Z
-IFEQ L227
+IFEQ L229
 ALOAD 0
 INVOKESTATIC local/lito381/server/NativeEquipmentDefense.alive(Llocal/lito381/server/ClientSession;)Z
 IFNE L28
-GOTO L227
+GOTO L229
 ALOAD 7
 GETFIELD local/lito381/server/NativeEquipmentDefense$Effect.rule:Llocal/lito381/server/NativeArmorProc$Rule;
 ASTORE 8
@@ -182,11 +182,11 @@
 ASTORE 11
 ALOAD 8
 INVOKEVIRTUAL local/lito381/server/NativeArmorProc$Rule.selfSkill()Z
-IFNE L169
-ALOAD 5
-IFNULL L169
+IFNE L172
+ALOAD 5
+IFNULL L172
 ILOAD 9
-IFEQ L169
+IFEQ L172
 ALOAD 8
 INVOKEVIRTUAL local/lito381/server/NativeArmorProc$Rule.gfx()I
 IFLE L144
@@ -223,22 +223,25 @@
 INVOKESTATIC local/lito381/server/NativePlayerCommit.safeAuxSend(Llocal/lito381/server/ClientSession;[BLjava/lang/String;)V
 ALOAD 8
 INVOKEVIRTUAL local/lito381/server/NativeArmorProc$Rule.skill()I
-IFLE L169
+IFLE L172
 ALOAD 0
 ALOAD 5
 ALOAD 8
 INVOKESTATIC local/lito381/server/NativeProcHealingEncounter.apply(Llocal/lito381/server/ClientSession;Llocal/lito381/server/NativeCombatAttacker$Ref;Llocal/lito381/server/NativeArmorProc$Rule;)Z
-IFNE L169
+IFEQ L153
+GOTO L172
 ALOAD 0
 ALOAD 5
 ALOAD 8
 INVOKESTATIC local/lito381/server/NativeProcHealingCompanion.apply(Llocal/lito381/server/ClientSession;Llocal/lito381/server/NativeCombatAttacker$Ref;Llocal/lito381/server/NativeArmorProc$Rule;)Z
-IFNE L169
+IFEQ L159
+GOTO L172
 ALOAD 0
 ALOAD 5
 ALOAD 8
 INVOKESTATIC local/lito381/server/NativeProcHealingWorld.apply(Llocal/lito381/server/ClientSession;Llocal/lito381/server/NativeCombatAttacker$Ref;Llocal/lito381/server/NativeArmorProc$Rule;)Z
-IFNE L169
+IFEQ L165
+GOTO L172
 NEW java/lang/IllegalArgumentException
 DUP
 ALOAD 8
@@ -248,17 +251,17 @@
 ATHROW
 ALOAD 8
 INVOKEVIRTUAL local/lito381/server/NativeArmorProc$Rule.gfx()I
-IFLE L210
+IFLE L213
 ALOAD 11
 INVOKESTATIC local/lito381/server/NativeEquipmentDefense.alive(Llocal/lito381/server/ClientSession;)Z
-IFEQ L210
-ALOAD 8
-INVOKEVIRTUAL local/lito381/server/NativeArmorProc$Rule.gfxMode()I
-IFEQ L182
+IFEQ L213
+ALOAD 8
+INVOKEVIRTUAL local/lito381/server/NativeArmorProc$Rule.gfxMode()I
+IFEQ L185
 ALOAD 8
 INVOKEVIRTUAL local/lito381/server/NativeArmorProc$Rule.gfxMode()I
 CONST Integer:2
-IF_ICMPNE L192
+IF_ICMPNE L195
 ALOAD 11
 ALOAD 11
 INVOKEVIRTUAL local/lito381/server/ClientSession.playerObjectId()J
@@ -272,11 +275,11 @@
 ALOAD 8
 INVOKEVIRTUAL local/lito381/server/NativeArmorProc$Rule.gfxMode()I
 CONST Integer:1
-IF_ICMPEQ L200
+IF_ICMPEQ L203
 ALOAD 8
 INVOKEVIRTUAL local/lito381/server/NativeArmorProc$Rule.gfxMode()I
 CONST Integer:2
-IF_ICMPNE L210
+IF_ICMPNE L213
 ALOAD 0
 ALOAD 0
 INVOKEVIRTUAL local/lito381/server/ClientSession.playerObjectId()J
@@ -289,16 +292,15 @@
 INVOKESTATIC local/lito381/server/NativePlayerCommit.safeAuxSend(Llocal/lito381/server/ClientSession;[BLjava/lang/String;)V
 ALOAD 8
 INVOKEVIRTUAL local/lito381/server/NativeArmorProc$Rule.skill()I
-IFLE L216
+IFLE L223
 ALOAD 11
 INVOKESTATIC local/lito381/server/NativeEquipmentDefense.alive(Llocal/lito381/server/ClientSession;)Z
-IFNE L217
-GOTO L14
+IFEQ L223
 ALOAD 0
 ALOAD 11
 ALOAD 8
 INVOKESTATIC local/lito381/server/NativeEquipmentDefense.skill(Llocal/lito381/server/ClientSession;Llocal/lito381/server/ClientSession;Llocal/lito381/server/NativeArmorProc$Rule;)V
-GOTO L226
+GOTO L228
 ASTORE 12
 CONST str:"equipment-hurt action"
 ALOAD 12
@@ -306,10 +308,10 @@
 GOTO L14
 ALOAD 0
 INVOKESTATIC local/lito381/server/NativePlayerCommit.validFrame(Llocal/lito381/server/ClientSession;)Z
-IFEQ L241
-ALOAD 0
-INVOKESTATIC local/lito381/server/NativeEquipmentDefense.alive(Llocal/lito381/server/ClientSession;)Z
-IFEQ L241
+IFEQ L243
+ALOAD 0
+INVOKESTATIC local/lito381/server/NativeEquipmentDefense.alive(Llocal/lito381/server/ClientSession;)Z
+IFEQ L243
 ALOAD 0
 ALOAD 2
 ALOAD 4
@@ -320,45 +322,45 @@
 INVOKESTATIC local/lito381/server/NativePlayerCommit.recover(Llocal/lito381/server/ClientSession;Llocal/lito381/server/NativePlayerCommit$Binding;IILjava/lang/String;)V
 ALOAD 0
 INVOKESTATIC local/lito381/server/NativePlayerCommit.validFrame(Llocal/lito381/server/ClientSession;)Z
-IFEQ L260
+IFEQ L262
 ALOAD 4
 GETFIELD local/lito381/server/NativeEquipmentDefense$Resolved.reflect:I
-IFLE L260
-ALOAD 5
-IFNULL L260
+IFLE L262
+ALOAD 5
+IFNULL L262
 ALOAD 0
 ALOAD 5
 ALOAD 4
 GETFIELD local/lito381/server/NativeEquipmentDefense$Resolved.reflect:I
 CONST str:"equipment-hurt reflect"
 INVOKESTATIC local/lito381/server/NativeCombatAttacker.damage(Llocal/lito381/server/ClientSession;Llocal/lito381/server/NativeCombatAttacker$Ref;ILjava/lang/String;)V
-GOTO L260
+GOTO L262
 ASTORE 13
 CONST str:"equipment NPC reflect"
 ALOAD 13
 INVOKESTATIC local/lito381/server/NativePlayerCommit.secondaryFailure(Ljava/lang/String;Ljava/lang/RuntimeException;)V
 ALOAD 5
-IFNONNULL L327
+IFNONNULL L329
 ALOAD 0
 INVOKESTATIC local/lito381/server/NativePlayerCommit.validFrame(Llocal/lito381/server/ClientSession;)Z
-IFEQ L327
+IFEQ L329
 ALOAD 4
 GETFIELD local/lito381/server/NativeEquipmentDefense$Resolved.reflect:I
-IFLE L327
-ALOAD 1
-IFNULL L327
+IFLE L329
+ALOAD 1
+IFNULL L329
 ALOAD 3
-IFNULL L327
+IFNULL L329
 ALOAD 3
 ALOAD 1
 INVOKEVIRTUAL local/lito381/server/NativePlayerCommit$Binding.matches(Llocal/lito381/server/ClientSession;)Z
-IFEQ L327
-ALOAD 1
-INVOKESTATIC local/lito381/server/NativeEquipmentDefense.alive(Llocal/lito381/server/ClientSession;)Z
-IFEQ L327
+IFEQ L329
+ALOAD 1
+INVOKESTATIC local/lito381/server/NativeEquipmentDefense.alive(Llocal/lito381/server/ClientSession;)Z
+IFEQ L329
 ALOAD 0
 INVOKESTATIC local/lito381/server/CombatWorldPhysical.world(Llocal/lito381/server/ClientSession;)Z
-IFEQ L290
+IFEQ L292
 ALOAD 1
 ALOAD 4
 GETFIELD local/lito381/server/NativeEquipmentDefense$Resolved.reflect:I
@@ -366,7 +368,7 @@
 CONST Integer:0
 CONST Integer:1
 INVOKESTATIC local/lito381/server/NativePlayerCommit.worldPlayer(Llocal/lito381/server/ClientSession;ILlocal/lito381/server/ClientSession;ZZ)V
-GOTO L322
+GOTO L324
 ALOAD 1
 CONST str:"takePlayerDamage"
 CONST str:"(ILlocal/lito381/server/ClientSession;ZZJ)V"
@@ -399,7 +401,7 @@
 AASTORE
 INVOKESTATIC local/lito381/server/CombatAccess.call(Ljava/lang/Object;Ljava/lang/String;Ljava/lang/String;[Ljava/lang/Object;)Ljava/lang/Object;
 POP
-GOTO L327
+GOTO L329
 ASTORE 14
 CONST str:"equipment-hurt reflect"
 ALOAD 14
--- staged/NativeEquipmentDefense/prepare-81fc7d59.txt
+++ candidate/NativeEquipmentDefense/prepare-81fc7d59.txt
@@ -64,7 +64,8 @@
 INVOKESTATIC java/lang/Math.min(II)I
 ISTORE 9
 ILOAD 9
-IFEQ L75
+IFNE L60
+GOTO L43
 GETSTATIC local/lito381/server/NativeEquipmentDefense.probabilityDraws:Ljava/util/concurrent/atomic/AtomicLong;
 INVOKEVIRTUAL java/util/concurrent/atomic/AtomicLong.incrementAndGet()J
 POP2
@@ -72,8 +73,7 @@
 CONST Integer:100
 INVOKEVIRTUAL java/util/concurrent/ThreadLocalRandom.nextInt(I)I
 ILOAD 9
-IF_ICMPLT L68
-GOTO L43
+IF_ICMPGE L75
 ALOAD 2
 ALOAD 8
 INVOKEVIRTUAL java/util/ArrayList.add(Ljava/lang/Object;)Z
--- staged/NativeEquipmentDefense/resolve-dc9a6b22.txt
+++ candidate/NativeEquipmentDefense/resolve-dc9a6b22.txt
@@ -33,8 +33,8 @@
 IFNONNULL L29
 ALOAD 1
 INVOKESTATIC local/lito381/server/NativeEquipmentDefense.alive(Llocal/lito381/server/ClientSession;)Z
-IFNE L34
-GOTO L13
+IFEQ L13
+GOTO L34
 ALOAD 0
 ALOAD 3
 INVOKESTATIC local/lito381/server/NativeCombatAttacker.alive(Llocal/lito381/server/ClientSession;Llocal/lito381/server/NativeCombatAttacker$Ref;)Z
```

</details>

### NativeLightAction — DIFF；待 #203／#216（CFG、lambda 或轉拋）

<details>
<summary>全部方法診斷</summary>

```json
{
  "name": "local/lito381/server/NativeLightAction",
  "status": "DIFF",
  "byteSame": false,
  "allowMethodsApplied": false,
  "methodResults": [
    {
      "method": "lambda$commitPair$9(Llocal/lito381/server/NativeLightAction$Ingress;Llocal/lito381/server/ClientSession;Llocal/lito381/server/NativePlayerCommit$Binding;Llocal/lito381/server/ClientSession;Llocal/lito381/server/NativePlayerVitalCompletions$Receipt;Llocal/lito381/server/NativeSelfCastAction$Task;)V",
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
      "method": "storeReady(Llocal/lito381/server/ClientSession;IJ)V",
      "status": "SAME_PATHS",
      "detail": "path multisets equal (5)"
    }
  ],
  "effectsMethods": [],
  "approvedMethods": [],
  "firstDiff": {
    "where": "method",
    "member": "commitPair(Llocal/lito381/server/ClientSession;Llocal/lito381/server/ClientSession;Llocal/lito381/server/NativeSelfCastAction$Task;)V",
    "insnIndex": null,
    "detail": "left=28:[124:java/lang/Throwable  ] right=28:[120:java/lang/Throwable  ]; paths rejected: path limit 4096 exceeded"
  }
}
```

</details>

<details>
<summary>完整正規化差異（staged → candidate）</summary>

```diff
--- staged/NativeLightAction/commitPair-177f2ac2.txt
+++ candidate/NativeLightAction/commitPair-177f2ac2.txt
@@ -5,36 +5,36 @@
 java/io/IOException
 ||||pvis=|pinv=|lvtvis=|lvtinv=

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
 ALOAD 0
 ALOAD 1
 ALOAD 2
@@ -101,11 +101,11 @@
 ALOAD 1
 ALOAD 8
 ALOAD 0
-ALOAD 3
 ALOAD 4
 ALOAD 1
 ALOAD 5
-INVOKEDYNAMIC run(Llocal/lito381/server/NativeLightAction$Ingress;Llocal/lito381/server/ClientSession;Llocal/lito381/server/NativeSelfCastAction$Task;Llocal/lito381/server/NativePlayerCommit$Binding;Llocal/lito381/server/ClientSession;Llocal/lito381/server/NativePlayerVitalCompletions$Receipt;)Ljava/lang/Runnable; bsm=H6 java/lang/invoke/LambdaMetafactory.metafactory(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodHandle;Ljava/lang/invoke/MethodType;)Ljava/lang/invoke/CallSite; args=[type:()V, handle:H6 local/lito381/server/NativeLightAction.lambda$content$af63fe6e68cfd3899283fefbe1d7b62e0a0f81208c854f8311239d75af7c1a51(Llocal/lito381/server/NativeLightAction$Ingress;Llocal/lito381/server/ClientSession;Llocal/lito381/server/NativeSelfCastAction$Task;Llocal/lito381/server/NativePlayerCommit$Binding;Llocal/lito381/server/ClientSession;Llocal/lito381/server/NativePlayerVitalCompletions$Receipt;)V, type:()V]
+ALOAD 3
+INVOKEDYNAMIC run(Llocal/lito381/server/NativeLightAction$Ingress;Llocal/lito381/server/ClientSession;Llocal/lito381/server/NativePlayerCommit$Binding;Llocal/lito381/server/ClientSession;Llocal/lito381/server/NativePlayerVitalCompletions$Receipt;Llocal/lito381/server/NativeSelfCastAction$Task;)Ljava/lang/Runnable; bsm=H6 java/lang/invoke/LambdaMetafactory.metafactory(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodHandle;Ljava/lang/invoke/MethodType;)Ljava/lang/invoke/CallSite; args=[type:()V, handle:H6 local/lito381/server/NativeLightAction.lambda$content$6b68235d2c8d50e26cae1ba81014eacf934e85f556cc515212e7a3bda2b6efa6(Llocal/lito381/server/NativeLightAction$Ingress;Llocal/lito381/server/ClientSession;Llocal/lito381/server/NativePlayerCommit$Binding;Llocal/lito381/server/ClientSession;Llocal/lito381/server/NativePlayerVitalCompletions$Receipt;Llocal/lito381/server/NativeSelfCastAction$Task;)V, type:()V]
 INVOKESTATIC local/lito381/server/NativePairedCastSafety.run(Llocal/lito381/server/ClientSession;Llocal/lito381/server/ClientSession;Ljava/lang/Runnable;)V
 GOTO L77
 ASTORE 10
@@ -123,47 +123,43 @@
 ALOAD 6
 INVOKEVIRTUAL java/lang/ThreadLocal.set(Ljava/lang/Object;)V
 ILOAD 7
-IFEQ L119
+IFEQ L115
 ALOAD 8
 GETFIELD local/lito381/server/NativeLightAction$Ingress.receipt:Llocal/lito381/server/NativePlayerVitalCompletions$Receipt;
-ASTORE 11
 ALOAD 8
 INVOKESTATIC local/lito381/server/NativeSourceSelfFinalFrame.retire(Ljava/lang/Object;)V
-ALOAD 11
 INVOKEVIRTUAL local/lito381/server/NativePlayerVitalCompletions$Receipt.close()V
-GOTO L119
-ASTORE 12
+GOTO L115
+ASTORE 11
 ALOAD 9
 INVOKESTATIC local/lito381/server/NativeCastTimeline.restore(Llocal/lito381/server/NativeCastTimeline$Arrival;)V
 ALOAD 6
-IFNONNULL L105
+IFNONNULL L103
 GETSTATIC local/lito381/server/NativeLightAction.ingress:Ljava/lang/ThreadLocal;
 INVOKEVIRTUAL java/lang/ThreadLocal.remove()V
-GOTO L108
+GOTO L106
 GETSTATIC local/lito381/server/NativeLightAction.ingress:Ljava/lang/ThreadLocal;
 ALOAD 6
 INVOKEVIRTUAL java/lang/ThreadLocal.set(Ljava/lang/Object;)V
 ILOAD 7
-IFEQ L117
+IFEQ L113
 ALOAD 8
 GETFIELD local/lito381/server/NativeLightAction$Ingress.receipt:Llocal/lito381/server/NativePlayerVitalCompletions$Receipt;
-ASTORE 13
 ALOAD 8
 INVOKESTATIC local/lito381/server/NativeSourceSelfFinalFrame.retire(Ljava/lang/Object;)V
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
 INVOKEVIRTUAL local/lito381/server/NativePlayerVitalCompletions$Receipt.close()V
 ALOAD 12
 ATHROW
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
 RETURN
--- staged/NativeLightAction/deferScoped-202c7960.txt
+++ candidate/NativeLightAction/deferScoped-202c7960.txt
@@ -26,7 +26,7 @@
 GETFIELD local/lito381/server/NativeLightAction$Batch.outputs:Ljava/util/List;
 ALOAD 1
 ALOAD 0
-INVOKEDYNAMIC run(Llocal/lito381/server/NativeLightAction$Batch;Llocal/lito381/server/NativeSelfCastAction$Task;)Llocal/lito381/server/NativeLightAction$Output; bsm=H6 java/lang/invoke/LambdaMetafactory.metafactory(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodHandle;Ljava/lang/invoke/MethodType;)Ljava/lang/invoke/CallSite; args=[type:()V, handle:H6 local/lito381/server/NativeLightAction.lambda$content$b93037e7b70cc60f32950364f5b80c1fc077fc616c9138dff25da83806318b36(Llocal/lito381/server/NativeLightAction$Batch;Llocal/lito381/server/NativeSelfCastAction$Task;)V, type:()V]
+INVOKEDYNAMIC run(Llocal/lito381/server/NativeLightAction$Batch;Llocal/lito381/server/NativeSelfCastAction$Task;)Llocal/lito381/server/NativeLightAction$Output; bsm=H6 java/lang/invoke/LambdaMetafactory.metafactory(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodHandle;Ljava/lang/invoke/MethodType;)Ljava/lang/invoke/CallSite; args=[type:()V, handle:H6 local/lito381/server/NativeLightAction.lambda$content$feb21681a7a728f6083a06149f5c2fd605497b164c3f5581edc8c1a2fe120f3c(Llocal/lito381/server/NativeLightAction$Batch;Llocal/lito381/server/NativeSelfCastAction$Task;)V, type:()V]
 INVOKEINTERFACE java/util/List.add(Ljava/lang/Object;)Z itf
 POP
 RETURN
--- staged/NativeLightAction/deferWeaponTargetOutput-202c7960.txt
+++ candidate/NativeLightAction/deferWeaponTargetOutput-202c7960.txt
@@ -26,7 +26,7 @@
 GETFIELD local/lito381/server/NativeLightAction$Batch.outputs:Ljava/util/List;
 ALOAD 1
 ALOAD 0
-INVOKEDYNAMIC run(Llocal/lito381/server/NativeLightAction$Batch;Llocal/lito381/server/NativeSelfCastAction$Task;)Llocal/lito381/server/NativeLightAction$Output; bsm=H6 java/lang/invoke/LambdaMetafactory.metafactory(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodHandle;Ljava/lang/invoke/MethodType;)Ljava/lang/invoke/CallSite; args=[type:()V, handle:H6 local/lito381/server/NativeLightAction.lambda$content$b93037e7b70cc60f32950364f5b80c1fc077fc616c9138dff25da83806318b36(Llocal/lito381/server/NativeLightAction$Batch;Llocal/lito381/server/NativeSelfCastAction$Task;)V, type:()V]
+INVOKEDYNAMIC run(Llocal/lito381/server/NativeLightAction$Batch;Llocal/lito381/server/NativeSelfCastAction$Task;)Llocal/lito381/server/NativeLightAction$Output; bsm=H6 java/lang/invoke/LambdaMetafactory.metafactory(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodHandle;Ljava/lang/invoke/MethodType;)Ljava/lang/invoke/CallSite; args=[type:()V, handle:H6 local/lito381/server/NativeLightAction.lambda$content$feb21681a7a728f6083a06149f5c2fd605497b164c3f5581edc8c1a2fe120f3c(Llocal/lito381/server/NativeLightAction$Batch;Llocal/lito381/server/NativeSelfCastAction$Task;)V, type:()V]
 INVOKEINTERFACE java/util/List.add(Ljava/lang/Object;)Z itf
 POP
 RETURN
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
```

</details>

### NativeLightAction$Batch — DIFF；待 #203／#216（CFG、lambda 或轉拋）

<details>
<summary>全部方法診斷</summary>

```json
{
  "name": "local/lito381/server/NativeLightAction$Batch",
  "status": "DIFF",
  "byteSame": false,
  "allowMethodsApplied": false,
  "methodResults": [
    {
      "method": "emit()V",
      "status": "DIFF",
      "detail": "method emit()V: left=1:[3:java/lang/Throwable  ] right=1:[6:java/lang/Throwable  ]; path event multisets differ (left=6, right=4)"
    },
    {
      "method": "nativeSourceBatchOriginalEmit()V",
      "status": "DIFF",
      "detail": "method nativeSourceBatchOriginalEmit()V insn[18]: left=IFNE L130 right=IFNE L8; paths rejected: normal control-flow cycle"
    }
  ],
  "effectsMethods": [],
  "approvedMethods": [],
  "firstDiff": {
    "where": "method",
    "member": "emit()V",
    "insnIndex": null,
    "detail": "left=1:[3:java/lang/Throwable  ] right=1:[6:java/lang/Throwable  ]; path event multisets differ (left=6, right=4)"
  }
}
```

</details>

<details>
<summary>完整正規化差異（staged → candidate）</summary>

```diff
--- staged/NativeLightAction$Batch/emit-9b75.txt
+++ candidate/NativeLightAction$Batch/emit-9b75.txt
@@ -5,27 +5,16 @@

 ||||pvis=|pinv=|lvtvis=|lvtinv=

-TRY 1:[3:java/lang/Throwable  ]
-TRY 6:[8:java/lang/Throwable  ]
-TRY 15:[17:java/lang/Throwable  ]
+TRY 1:[6:java/lang/Throwable  ]
 ALOAD 0
 INVOKESPECIAL local/lito381/server/NativeLightAction$Batch.nativeSourceBatchOriginalEmit()V
-GOTO L13
+ALOAD 0
+CONST null
+INVOKESTATIC local/lito381/server/NativeSourceTargetIconBatch.finish(Ljava/lang/Object;Ljava/lang/Throwable;)V
+RETURN
 ASTORE 1
 ALOAD 0
 ALOAD 1
 INVOKESTATIC local/lito381/server/NativeSourceTargetIconBatch.finish(Ljava/lang/Object;Ljava/lang/Throwable;)V
-GOTO L11
-ASTORE 2
-ALOAD 2
-INVOKESTATIC local/lito381/server/NativeLightAction$Batch.rethrowCandidateFailure(Ljava/lang/Throwable;)V
 ALOAD 1
 ATHROW
-ALOAD 0
-CONST null
-INVOKESTATIC local/lito381/server/NativeSourceTargetIconBatch.finish(Ljava/lang/Object;Ljava/lang/Throwable;)V
-GOTO L20
-ASTORE 3
-ALOAD 3
-INVOKESTATIC local/lito381/server/NativeLightAction$Batch.rethrowCandidateFailure(Ljava/lang/Throwable;)V
-RETURN
--- staged/NativeLightAction$Batch/nativeSourceBatchOriginalEmit-9b75.txt
+++ candidate/NativeLightAction$Batch/nativeSourceBatchOriginalEmit-9b75.txt
@@ -5,58 +5,55 @@

 ||||pvis=|pinv=|lvtvis=|lvtinv=

-TRY 5:[228:java/io/IOException  , 236:java/lang/RuntimeException  , 236:java/lang/Error  , 241:java/lang/Throwable  ]
-TRY 6:[228:java/io/IOException  , 236:java/lang/RuntimeException  , 236:java/lang/Error  , 241:java/lang/Throwable  ]
-TRY 9:[228:java/io/IOException  , 236:java/lang/RuntimeException  , 236:java/lang/Error  , 241:java/lang/Throwable  ]
-TRY 12:[228:java/io/IOException  , 236:java/lang/RuntimeException  , 236:java/lang/Error  , 241:java/lang/Throwable  ]
-TRY 13:[228:java/io/IOException  , 236:java/lang/RuntimeException  , 236:java/lang/Error  , 241:java/lang/Throwable  ]
-TRY 17:[228:java/io/IOException  , 236:java/lang/RuntimeException  , 236:java/lang/Error  , 241:java/lang/Throwable  ]
-TRY 20:[228:java/io/IOException  , 236:java/lang/RuntimeException  , 236:java/lang/Error  , 241:java/lang/Throwable  ]
-TRY 21:[228:java/io/IOException  , 236:java/lang/RuntimeException  , 236:java/lang/Error  , 241:java/lang/Throwable  ]
-TRY 23:[228:java/io/IOException  , 236:java/lang/RuntimeException  , 236:java/lang/Error  , 241:java/lang/Throwable  ]
-TRY 24:[228:java/io/IOException  , 236:java/lang/RuntimeException  , 236:java/lang/Error  , 241:java/lang/Throwable  ]
-TRY 27:[228:java/io/IOException  , 236:java/lang/RuntimeException  , 236:java/lang/Error  , 241:java/lang/Throwable  ]
-TRY 28:[228:java/io/IOException  , 236:java/lang/RuntimeException  , 236:java/lang/Error  , 241:java/lang/Throwable  ]
-TRY 31:[228:java/io/IOException  , 236:java/lang/RuntimeException  , 236:java/lang/Error  , 241:java/lang/Throwable  ]
-TRY 34:[228:java/io/IOException  , 236:java/lang/RuntimeException  , 236:java/lang/Error  , 241:java/lang/Throwable  ]
-TRY 36:[228:java/io/IOException  , 236:java/lang/RuntimeException  , 236:java/lang/Error  , 241:java/lang/Throwable  ]
-TRY 38:[228:java/io/IOException  , 236:java/lang/RuntimeException  , 236:java/lang/Error  , 241:java/lang/Throwable  ]
-TRY 39:[228:java/io/IOException  , 236:java/lang/RuntimeException  , 236:java/lang/Error  , 241:java/lang/Throwable  ]
-TRY 44:[46:java/io/IOException  ]
-TRY 64:[85:java/lang/Throwable  ]
-TRY 65:[85:java/lang/Throwable  ]
-TRY 67:[85:java/lang/Throwable  ]
-TRY 68:[85:java/lang/Throwable  ]
-TRY 71:[85:java/lang/Throwable  ]
-TRY 74:[85:java/lang/Throwable  ]
-TRY 76:[85:java/lang/Throwable  ]
-TRY 77:[85:java/lang/Throwable  ]
-TRY 83:[85:java/lang/Throwable  ]
-TRY 133:[228:java/io/IOException  , 236:java/lang/RuntimeException  , 236:java/lang/Error  , 241:java/lang/Throwable  ]
-TRY 140:[142:java/io/IOException  ]
-TRY 160:[181:java/lang/Throwable  ]
-TRY 161:[181:java/lang/Throwable  ]
-TRY 163:[181:java/lang/Throwable  ]
-TRY 164:[181:java/lang/Throwable  ]
-TRY 167:[181:java/lang/Throwable  ]
-TRY 170:[181:java/lang/Throwable  ]
-TRY 172:[181:java/lang/Throwable  ]
-TRY 173:[181:java/lang/Throwable  ]
-TRY 179:[181:java/lang/Throwable  ]
-TRY 231:[241:java/lang/Throwable  ]
-TRY 234:[241:java/lang/Throwable  ]
-TRY 235:[241:java/lang/Throwable  ]
-TRY 240:[241:java/lang/Throwable  ]
-TRY 245:[247:java/io/IOException  ]
-TRY 265:[286:java/lang/Throwable  ]
-TRY 266:[286:java/lang/Throwable  ]
-TRY 268:[286:java/lang/Throwable  ]
-TRY 269:[286:java/lang/Throwable  ]
-TRY 272:[286:java/lang/Throwable  ]
-TRY 275:[286:java/lang/Throwable  ]
-TRY 277:[286:java/lang/Throwable  ]
-TRY 278:[286:java/lang/Throwable  ]
-TRY 284:[286:java/lang/Throwable  ]
+TRY 5:[220:java/io/IOException  , 228:java/lang/RuntimeException  , 228:java/lang/Error  , 233:java/lang/Throwable  ]
+TRY 6:[220:java/io/IOException  , 228:java/lang/RuntimeException  , 228:java/lang/Error  , 233:java/lang/Throwable  ]
+TRY 9:[220:java/io/IOException  , 228:java/lang/RuntimeException  , 228:java/lang/Error  , 233:java/lang/Throwable  ]
+TRY 12:[220:java/io/IOException  , 228:java/lang/RuntimeException  , 228:java/lang/Error  , 233:java/lang/Throwable  ]
+TRY 13:[220:java/io/IOException  , 228:java/lang/RuntimeException  , 228:java/lang/Error  , 233:java/lang/Throwable  ]
+TRY 17:[220:java/io/IOException  , 228:java/lang/RuntimeException  , 228:java/lang/Error  , 233:java/lang/Throwable  ]
+TRY 20:[220:java/io/IOException  , 228:java/lang/RuntimeException  , 228:java/lang/Error  , 233:java/lang/Throwable  ]
+TRY 21:[220:java/io/IOException  , 228:java/lang/RuntimeException  , 228:java/lang/Error  , 233:java/lang/Throwable  ]
+TRY 23:[220:java/io/IOException  , 228:java/lang/RuntimeException  , 228:java/lang/Error  , 233:java/lang/Throwable  ]
+TRY 24:[220:java/io/IOException  , 228:java/lang/RuntimeException  , 228:java/lang/Error  , 233:java/lang/Throwable  ]
+TRY 27:[220:java/io/IOException  , 228:java/lang/RuntimeException  , 228:java/lang/Error  , 233:java/lang/Throwable  ]
+TRY 28:[220:java/io/IOException  , 228:java/lang/RuntimeException  , 228:java/lang/Error  , 233:java/lang/Throwable  ]
+TRY 31:[220:java/io/IOException  , 228:java/lang/RuntimeException  , 228:java/lang/Error  , 233:java/lang/Throwable  ]
+TRY 34:[220:java/io/IOException  , 228:java/lang/RuntimeException  , 228:java/lang/Error  , 233:java/lang/Throwable  ]
+TRY 36:[220:java/io/IOException  , 228:java/lang/RuntimeException  , 228:java/lang/Error  , 233:java/lang/Throwable  ]
+TRY 38:[220:java/io/IOException  , 228:java/lang/RuntimeException  , 228:java/lang/Error  , 233:java/lang/Throwable  ]
+TRY 39:[220:java/io/IOException  , 228:java/lang/RuntimeException  , 228:java/lang/Error  , 233:java/lang/Throwable  ]
+TRY 60:[81:java/lang/Throwable  ]
+TRY 61:[81:java/lang/Throwable  ]
+TRY 63:[81:java/lang/Throwable  ]
+TRY 64:[81:java/lang/Throwable  ]
+TRY 67:[81:java/lang/Throwable  ]
+TRY 70:[81:java/lang/Throwable  ]
+TRY 72:[81:java/lang/Throwable  ]
+TRY 73:[81:java/lang/Throwable  ]
+TRY 79:[81:java/lang/Throwable  ]
+TRY 129:[220:java/io/IOException  , 228:java/lang/RuntimeException  , 228:java/lang/Error  , 233:java/lang/Throwable  ]
+TRY 152:[173:java/lang/Throwable  ]
+TRY 153:[173:java/lang/Throwable  ]
+TRY 155:[173:java/lang/Throwable  ]
+TRY 156:[173:java/lang/Throwable  ]
+TRY 159:[173:java/lang/Throwable  ]
+TRY 162:[173:java/lang/Throwable  ]
+TRY 164:[173:java/lang/Throwable  ]
+TRY 165:[173:java/lang/Throwable  ]
+TRY 171:[173:java/lang/Throwable  ]
+TRY 223:[233:java/lang/Throwable  ]
+TRY 226:[233:java/lang/Throwable  ]
+TRY 227:[233:java/lang/Throwable  ]
+TRY 232:[233:java/lang/Throwable  ]
+TRY 253:[274:java/lang/Throwable  ]
+TRY 254:[274:java/lang/Throwable  ]
+TRY 256:[274:java/lang/Throwable  ]
+TRY 257:[274:java/lang/Throwable  ]
+TRY 260:[274:java/lang/Throwable  ]
+TRY 263:[274:java/lang/Throwable  ]
+TRY 265:[274:java/lang/Throwable  ]
+TRY 266:[274:java/lang/Throwable  ]
+TRY 272:[274:java/lang/Throwable  ]
 CONST Integer:0
 ISTORE 1
 CONST null
@@ -67,7 +64,7 @@
 ASTORE 3
 ALOAD 3
 INVOKEINTERFACE java/util/Iterator.hasNext()Z itf
-IFEQ L135
+IFEQ L131
 ALOAD 3
 INVOKEINTERFACE java/util/Iterator.next()Ljava/lang/Object; itf
 CHECKCAST local/lito381/server/NativeLightAction$Output
@@ -75,7 +72,7 @@
 ALOAD 4
 ALOAD 0
 INVOKESTATIC local/lito381/server/NativeSourceTargetIconBatch.independent(Ljava/lang/Object;Ljava/lang/Object;)Z
-IFNE L134
+IFNE L8
 ALOAD 0
 GETFIELD local/lito381/server/NativeLightAction$Batch.accepted:Llocal/lito381/server/NativeLightAction$Ingress;
 GETFIELD local/lito381/server/NativeLightAction$Ingress.binding:Llocal/lito381/server/NativePlayerCommit$Binding;
@@ -89,7 +86,7 @@
 IFEQ L41
 ALOAD 0
 GETFIELD local/lito381/server/NativeLightAction$Batch.target:Llocal/lito381/server/ClientSession;
-IFNULL L132
+IFNULL L128
 ALOAD 0
 GETFIELD local/lito381/server/NativeLightAction$Batch.targetBinding:Llocal/lito381/server/NativePlayerCommit$Binding;
 ALOAD 0
@@ -97,90 +94,86 @@
 ALOAD 0
 GETFIELD local/lito381/server/NativeLightAction$Batch.material:Llocal/lito381/server/NativeCastMaterial$Cast;
 INVOKESTATIC local/lito381/server/NativeWeaponBreakTargetLifecycle.targetPublication(Llocal/lito381/server/NativePlayerCommit$Binding;Llocal/lito381/server/ClientSession;Llocal/lito381/server/NativeCastMaterial$Cast;)Z
-IFNE L132
+IFNE L128
 CONST null
 ASTORE 5
 ALOAD 0
 INVOKESTATIC local/lito381/server/NativeSourceTargetIconBatch.rescue(Ljava/lang/Object;)V
-GOTO L49
+ALOAD 0
+GETFIELD local/lito381/server/NativeLightAction$Batch.completions:Ljava/util/List;
+INVOKEINTERFACE java/util/List.iterator()Ljava/util/Iterator; itf
 ASTORE 6
 ALOAD 6
-INVOKESTATIC local/lito381/server/NativeLightAction$Batch.rethrowCandidateFailure(Ljava/lang/Throwable;)V
-ALOAD 0
-GETFIELD local/lito381/server/NativeLightAction$Batch.completions:Ljava/util/List;
-INVOKEINTERFACE java/util/List.iterator()Ljava/util/Iterator; itf
+INVOKEINTERFACE java/util/Iterator.hasNext()Z itf
+IFEQ L94
+ALOAD 6
+INVOKEINTERFACE java/util/Iterator.next()Ljava/lang/Object; itf
+CHECKCAST local/lito381/server/NativeLightAction$Completion
 ASTORE 7
 ALOAD 7
-INVOKEINTERFACE java/util/Iterator.hasNext()Z itf
-IFEQ L98
-ALOAD 7
-INVOKEINTERFACE java/util/Iterator.next()Ljava/lang/Object; itf
-CHECKCAST local/lito381/server/NativeLightAction$Completion
-ASTORE 8
-ALOAD 8
 ILOAD 1
-IFEQ L81
+IFEQ L77
 ALOAD 0
 GETFIELD local/lito381/server/NativeLightAction$Batch.accepted:Llocal/lito381/server/NativeLightAction$Ingress;
 GETFIELD local/lito381/server/NativeLightAction$Ingress.binding:Llocal/lito381/server/NativePlayerCommit$Binding;
 ALOAD 0
 GETFIELD local/lito381/server/NativeLightAction$Batch.pc:Llocal/lito381/server/ClientSession;
 INVOKESTATIC local/lito381/server/NativeSourceControlledRoleFrame.current(Llocal/lito381/server/NativePlayerCommit$Binding;Llocal/lito381/server/ClientSession;)Z
-IFEQ L81
-ALOAD 0
-GETFIELD local/lito381/server/NativeLightAction$Batch.target:Llocal/lito381/server/ClientSession;
-IFNULL L79
+IFEQ L77
+ALOAD 0
+GETFIELD local/lito381/server/NativeLightAction$Batch.target:Llocal/lito381/server/ClientSession;
+IFNULL L75
 ALOAD 0
 GETFIELD local/lito381/server/NativeLightAction$Batch.targetBinding:Llocal/lito381/server/NativePlayerCommit$Binding;
 ALOAD 0
 GETFIELD local/lito381/server/NativeLightAction$Batch.target:Llocal/lito381/server/ClientSession;
 INVOKESTATIC local/lito381/server/NativeSourceControlledRoleFrame.current(Llocal/lito381/server/NativePlayerCommit$Binding;Llocal/lito381/server/ClientSession;)Z
-IFEQ L81
+IFEQ L77
 CONST Integer:1
-GOTO L82
+GOTO L78
 CONST Integer:0
 ALOAD 2
 INVOKEINTERFACE local/lito381/server/NativeLightAction$Completion.finish(ZLjava/lang/Throwable;)V itf
-GOTO L97
+GOTO L93
+ASTORE 8
+ALOAD 5
+IFNONNULL L87
+ALOAD 8
+ASTORE 5
+GOTO L93
+ALOAD 5
+ALOAD 8
+IF_ACMPEQ L93
+ALOAD 5
+ALOAD 8
+INVOKEVIRTUAL java/lang/Throwable.addSuppressed(Ljava/lang/Throwable;)V
+GOTO L49
+ALOAD 5
+IFNULL L127
+ALOAD 2
+IFNULL L105
+ALOAD 5
+ALOAD 2
+IF_ACMPEQ L127
+ALOAD 2
+ALOAD 5
+INVOKEVIRTUAL java/lang/Throwable.addSuppressed(Ljava/lang/Throwable;)V
+GOTO L127
+ALOAD 5
+INSTANCEOF java/lang/RuntimeException
+IFEQ L113
+ALOAD 5
+CHECKCAST java/lang/RuntimeException
 ASTORE 9
-ALOAD 5
-IFNONNULL L91
 ALOAD 9
-ASTORE 5
-GOTO L97
-ALOAD 5
-ALOAD 9
-IF_ACMPEQ L97
-ALOAD 5
-ALOAD 9
-INVOKEVIRTUAL java/lang/Throwable.addSuppressed(Ljava/lang/Throwable;)V
-GOTO L53
-ALOAD 5
-IFNULL L131
-ALOAD 2
-IFNULL L109
-ALOAD 5
-ALOAD 2
-IF_ACMPEQ L131
-ALOAD 2
-ALOAD 5
-INVOKEVIRTUAL java/lang/Throwable.addSuppressed(Ljava/lang/Throwable;)V
-GOTO L131
-ALOAD 5
-INSTANCEOF java/lang/RuntimeException
-IFEQ L117
-ALOAD 5
-CHECKCAST java/lang/RuntimeException
+ATHROW
+ALOAD 5
+INSTANCEOF java/lang/Error
+IFEQ L121
+ALOAD 5
+CHECKCAST java/lang/Error
 ASTORE 10
 ALOAD 10
-ATHROW
-ALOAD 5
-INSTANCEOF java/lang/Error
-IFEQ L125
-ALOAD 5
-CHECKCAST java/lang/Error
-ASTORE 11
-ALOAD 11
 ATHROW
 NEW java/lang/IllegalStateException
 DUP
@@ -193,202 +186,194 @@
 INVOKEINTERFACE local/lito381/server/NativeLightAction$Output.run()V itf
 GOTO L8
 CONST Integer:1
-ISTORE 12
+ISTORE 11
 CONST null
+ASTORE 12
+ALOAD 0
+INVOKESTATIC local/lito381/server/NativeSourceTargetIconBatch.rescue(Ljava/lang/Object;)V
+ALOAD 0
+GETFIELD local/lito381/server/NativeLightAction$Batch.completions:Ljava/util/List;
+INVOKEINTERFACE java/util/List.iterator()Ljava/util/Iterator; itf
 ASTORE 13
-ALOAD 0
-INVOKESTATIC local/lito381/server/NativeSourceTargetIconBatch.rescue(Ljava/lang/Object;)V
-GOTO L145
+ALOAD 13
+INVOKEINTERFACE java/util/Iterator.hasNext()Z itf
+IFEQ L186
+ALOAD 13
+INVOKEINTERFACE java/util/Iterator.next()Ljava/lang/Object; itf
+CHECKCAST local/lito381/server/NativeLightAction$Completion
 ASTORE 14
 ALOAD 14
-INVOKESTATIC local/lito381/server/NativeLightAction$Batch.rethrowCandidateFailure(Ljava/lang/Throwable;)V
-ALOAD 0
-GETFIELD local/lito381/server/NativeLightAction$Batch.completions:Ljava/util/List;
-INVOKEINTERFACE java/util/List.iterator()Ljava/util/Iterator; itf
+ILOAD 11
+IFEQ L169
+ALOAD 0
+GETFIELD local/lito381/server/NativeLightAction$Batch.accepted:Llocal/lito381/server/NativeLightAction$Ingress;
+GETFIELD local/lito381/server/NativeLightAction$Ingress.binding:Llocal/lito381/server/NativePlayerCommit$Binding;
+ALOAD 0
+GETFIELD local/lito381/server/NativeLightAction$Batch.pc:Llocal/lito381/server/ClientSession;
+INVOKESTATIC local/lito381/server/NativeSourceControlledRoleFrame.current(Llocal/lito381/server/NativePlayerCommit$Binding;Llocal/lito381/server/ClientSession;)Z
+IFEQ L169
+ALOAD 0
+GETFIELD local/lito381/server/NativeLightAction$Batch.target:Llocal/lito381/server/ClientSession;
+IFNULL L167
+ALOAD 0
+GETFIELD local/lito381/server/NativeLightAction$Batch.targetBinding:Llocal/lito381/server/NativePlayerCommit$Binding;
+ALOAD 0
+GETFIELD local/lito381/server/NativeLightAction$Batch.target:Llocal/lito381/server/ClientSession;
+INVOKESTATIC local/lito381/server/NativeSourceControlledRoleFrame.current(Llocal/lito381/server/NativePlayerCommit$Binding;Llocal/lito381/server/ClientSession;)Z
+IFEQ L169
+CONST Integer:1
+GOTO L170
+CONST Integer:0
+ALOAD 2
+INVOKEINTERFACE local/lito381/server/NativeLightAction$Completion.finish(ZLjava/lang/Throwable;)V itf
+GOTO L185
 ASTORE 15
+ALOAD 12
+IFNONNULL L179
 ALOAD 15
-INVOKEINTERFACE java/util/Iterator.hasNext()Z itf
-IFEQ L194
+ASTORE 12
+GOTO L185
+ALOAD 12
 ALOAD 15
-INVOKEINTERFACE java/util/Iterator.next()Ljava/lang/Object; itf
-CHECKCAST local/lito381/server/NativeLightAction$Completion
+IF_ACMPEQ L185
+ALOAD 12
+ALOAD 15
+INVOKEVIRTUAL java/lang/Throwable.addSuppressed(Ljava/lang/Throwable;)V
+GOTO L141
+ALOAD 12
+IFNULL L219
+ALOAD 2
+IFNULL L197
+ALOAD 12
+ALOAD 2
+IF_ACMPEQ L219
+ALOAD 2
+ALOAD 12
+INVOKEVIRTUAL java/lang/Throwable.addSuppressed(Ljava/lang/Throwable;)V
+GOTO L219
+ALOAD 12
+INSTANCEOF java/lang/RuntimeException
+IFEQ L205
+ALOAD 12
+CHECKCAST java/lang/RuntimeException
 ASTORE 16
 ALOAD 16
-ILOAD 12
-IFEQ L177
-ALOAD 0
-GETFIELD local/lito381/server/NativeLightAction$Batch.accepted:Llocal/lito381/server/NativeLightAction$Ingress;
-GETFIELD local/lito381/server/NativeLightAction$Ingress.binding:Llocal/lito381/server/NativePlayerCommit$Binding;
-ALOAD 0
-GETFIELD local/lito381/server/NativeLightAction$Batch.pc:Llocal/lito381/server/ClientSession;
-INVOKESTATIC local/lito381/server/NativeSourceControlledRoleFrame.current(Llocal/lito381/server/NativePlayerCommit$Binding;Llocal/lito381/server/ClientSession;)Z
-IFEQ L177
-ALOAD 0
-GETFIELD local/lito381/server/NativeLightAction$Batch.target:Llocal/lito381/server/ClientSession;
-IFNULL L175
-ALOAD 0
-GETFIELD local/lito381/server/NativeLightAction$Batch.targetBinding:Llocal/lito381/server/NativePlayerCommit$Binding;
-ALOAD 0
-GETFIELD local/lito381/server/NativeLightAction$Batch.target:Llocal/lito381/server/ClientSession;
-INVOKESTATIC local/lito381/server/NativeSourceControlledRoleFrame.current(Llocal/lito381/server/NativePlayerCommit$Binding;Llocal/lito381/server/ClientSession;)Z
-IFEQ L177
-CONST Integer:1
-GOTO L178
-CONST Integer:0
-ALOAD 2
-INVOKEINTERFACE local/lito381/server/NativeLightAction$Completion.finish(ZLjava/lang/Throwable;)V itf
-GOTO L193
+ATHROW
+ALOAD 12
+INSTANCEOF java/lang/Error
+IFEQ L213
+ALOAD 12
+CHECKCAST java/lang/Error
 ASTORE 17
-ALOAD 13
-IFNONNULL L187
 ALOAD 17
-ASTORE 13
-GOTO L193
-ALOAD 13
-ALOAD 17
-IF_ACMPEQ L193
-ALOAD 13
-ALOAD 17
-INVOKEVIRTUAL java/lang/Throwable.addSuppressed(Ljava/lang/Throwable;)V
-GOTO L149
-ALOAD 13
-IFNULL L227
-ALOAD 2
-IFNULL L205
-ALOAD 13
-ALOAD 2
-IF_ACMPEQ L227
-ALOAD 2
-ALOAD 13
-INVOKEVIRTUAL java/lang/Throwable.addSuppressed(Ljava/lang/Throwable;)V
-GOTO L227
-ALOAD 13
-INSTANCEOF java/lang/RuntimeException
-IFEQ L213
-ALOAD 13
-CHECKCAST java/lang/RuntimeException
-ASTORE 18
-ALOAD 18
-ATHROW
-ALOAD 13
-INSTANCEOF java/lang/Error
-IFEQ L221
-ALOAD 13
-CHECKCAST java/lang/Error
-ASTORE 19
-ALOAD 19
 ATHROW
 NEW java/lang/IllegalStateException
 DUP
 CONST str:"cast completion failed"
-ALOAD 13
+ALOAD 12
 INVOKESPECIAL java/lang/IllegalStateException.<init>(Ljava/lang/String;Ljava/lang/Throwable;)V
 ATHROW
-GOTO L334
-ASTORE 20
-ALOAD 20
+GOTO L322
+ASTORE 18
+ALOAD 18
 ASTORE 2
 NEW java/io/UncheckedIOException
 DUP
-ALOAD 20
+ALOAD 18
 INVOKESPECIAL java/io/UncheckedIOException.<init>(Ljava/io/IOException;)V
 ATHROW
+ASTORE 19
+ALOAD 19
+ASTORE 2
+ALOAD 19
+ATHROW
+ASTORE 20
+CONST null
 ASTORE 21
-ALOAD 21
-ASTORE 2
-ALOAD 21
-ATHROW
-ASTORE 22
-CONST null
-ASTORE 23
 ALOAD 0
 INVOKESTATIC local/lito381/server/NativeSourceTargetIconBatch.rescue(Ljava/lang/Object;)V
-GOTO L250
-ASTORE 24
-ALOAD 24
-INVOKESTATIC local/lito381/server/NativeLightAction$Batch.rethrowCandidateFailure(Ljava/lang/Throwable;)V
 ALOAD 0
 GETFIELD local/lito381/server/NativeLightAction$Batch.completions:Ljava/util/List;
 INVOKEINTERFACE java/util/List.iterator()Ljava/util/Iterator; itf
+ASTORE 22
+ALOAD 22
+INVOKEINTERFACE java/util/Iterator.hasNext()Z itf
+IFEQ L287
+ALOAD 22
+INVOKEINTERFACE java/util/Iterator.next()Ljava/lang/Object; itf
+CHECKCAST local/lito381/server/NativeLightAction$Completion
+ASTORE 23
+ALOAD 23
+ILOAD 1
+IFEQ L270
+ALOAD 0
+GETFIELD local/lito381/server/NativeLightAction$Batch.accepted:Llocal/lito381/server/NativeLightAction$Ingress;
+GETFIELD local/lito381/server/NativeLightAction$Ingress.binding:Llocal/lito381/server/NativePlayerCommit$Binding;
+ALOAD 0
+GETFIELD local/lito381/server/NativeLightAction$Batch.pc:Llocal/lito381/server/ClientSession;
+INVOKESTATIC local/lito381/server/NativeSourceControlledRoleFrame.current(Llocal/lito381/server/NativePlayerCommit$Binding;Llocal/lito381/server/ClientSession;)Z
+IFEQ L270
+ALOAD 0
+GETFIELD local/lito381/server/NativeLightAction$Batch.target:Llocal/lito381/server/ClientSession;
+IFNULL L268
+ALOAD 0
+GETFIELD local/lito381/server/NativeLightAction$Batch.targetBinding:Llocal/lito381/server/NativePlayerCommit$Binding;
+ALOAD 0
+GETFIELD local/lito381/server/NativeLightAction$Batch.target:Llocal/lito381/server/ClientSession;
+INVOKESTATIC local/lito381/server/NativeSourceControlledRoleFrame.current(Llocal/lito381/server/NativePlayerCommit$Binding;Llocal/lito381/server/ClientSession;)Z
+IFEQ L270
+CONST Integer:1
+GOTO L271
+CONST Integer:0
+ALOAD 2
+INVOKEINTERFACE local/lito381/server/NativeLightAction$Completion.finish(ZLjava/lang/Throwable;)V itf
+GOTO L286
+ASTORE 24
+ALOAD 21
+IFNONNULL L280
+ALOAD 24
+ASTORE 21
+GOTO L286
+ALOAD 21
+ALOAD 24
+IF_ACMPEQ L286
+ALOAD 21
+ALOAD 24
+INVOKEVIRTUAL java/lang/Throwable.addSuppressed(Ljava/lang/Throwable;)V
+GOTO L242
+ALOAD 21
+IFNULL L320
+ALOAD 2
+IFNULL L298
+ALOAD 21
+ALOAD 2
+IF_ACMPEQ L320
+ALOAD 2
+ALOAD 21
+INVOKEVIRTUAL java/lang/Throwable.addSuppressed(Ljava/lang/Throwable;)V
+GOTO L320
+ALOAD 21
+INSTANCEOF java/lang/RuntimeException
+IFEQ L306
+ALOAD 21
+CHECKCAST java/lang/RuntimeException
 ASTORE 25
 ALOAD 25
-INVOKEINTERFACE java/util/Iterator.hasNext()Z itf
-IFEQ L299
-ALOAD 25
-INVOKEINTERFACE java/util/Iterator.next()Ljava/lang/Object; itf
-CHECKCAST local/lito381/server/NativeLightAction$Completion
+ATHROW
+ALOAD 21
+INSTANCEOF java/lang/Error
+IFEQ L314
+ALOAD 21
+CHECKCAST java/lang/Error
 ASTORE 26
 ALOAD 26
-ILOAD 1
-IFEQ L282
-ALOAD 0
-GETFIELD local/lito381/server/NativeLightAction$Batch.accepted:Llocal/lito381/server/NativeLightAction$Ingress;
-GETFIELD local/lito381/server/NativeLightAction$Ingress.binding:Llocal/lito381/server/NativePlayerCommit$Binding;
-ALOAD 0
-GETFIELD local/lito381/server/NativeLightAction$Batch.pc:Llocal/lito381/server/ClientSession;
-INVOKESTATIC local/lito381/server/NativeSourceControlledRoleFrame.current(Llocal/lito381/server/NativePlayerCommit$Binding;Llocal/lito381/server/ClientSession;)Z
-IFEQ L282
-ALOAD 0
-GETFIELD local/lito381/server/NativeLightAction$Batch.target:Llocal/lito381/server/ClientSession;
-IFNULL L280
-ALOAD 0
-GETFIELD local/lito381/server/NativeLightAction$Batch.targetBinding:Llocal/lito381/server/NativePlayerCommit$Binding;
-ALOAD 0
-GETFIELD local/lito381/server/NativeLightAction$Batch.target:Llocal/lito381/server/ClientSession;
-INVOKESTATIC local/lito381/server/NativeSourceControlledRoleFrame.current(Llocal/lito381/server/NativePlayerCommit$Binding;Llocal/lito381/server/ClientSession;)Z
-IFEQ L282
-CONST Integer:1
-GOTO L283
-CONST Integer:0
-ALOAD 2
-INVOKEINTERFACE local/lito381/server/NativeLightAction$Completion.finish(ZLjava/lang/Throwable;)V itf
-GOTO L298
-ASTORE 27
-ALOAD 23
-IFNONNULL L292
-ALOAD 27
-ASTORE 23
-GOTO L298
-ALOAD 23
-ALOAD 27
-IF_ACMPEQ L298
-ALOAD 23
-ALOAD 27
-INVOKEVIRTUAL java/lang/Throwable.addSuppressed(Ljava/lang/Throwable;)V
-GOTO L254
-ALOAD 23
-IFNULL L332
-ALOAD 2
-IFNULL L310
-ALOAD 23
-ALOAD 2
-IF_ACMPEQ L332
-ALOAD 2
-ALOAD 23
-INVOKEVIRTUAL java/lang/Throwable.addSuppressed(Ljava/lang/Throwable;)V
-GOTO L332
-ALOAD 23
-INSTANCEOF java/lang/RuntimeException
-IFEQ L318
-ALOAD 23
-CHECKCAST java/lang/RuntimeException
-ASTORE 28
-ALOAD 28
-ATHROW
-ALOAD 23
-INSTANCEOF java/lang/Error
-IFEQ L326
-ALOAD 23
-CHECKCAST java/lang/Error
-ASTORE 29
-ALOAD 29
 ATHROW
 NEW java/lang/IllegalStateException
 DUP
 CONST str:"cast completion failed"
-ALOAD 23
+ALOAD 21
 INVOKESPECIAL java/lang/IllegalStateException.<init>(Ljava/lang/String;Ljava/lang/Throwable;)V
 ATHROW
-ALOAD 22
+ALOAD 20
 ATHROW
 RETURN
```

</details>

### NativePlayerCommit — DIFF；待 #203（CFG／迴圈與資料流）

<details>
<summary>全部方法診斷</summary>

```json
{
  "name": "local/lito381/server/NativePlayerCommit",
  "status": "DIFF",
  "byteSame": false,
  "allowMethodsApplied": false,
  "methodResults": [
    {
      "method": "acceptedTail(Llocal/lito381/server/NativePlayerCommit$Frame;)V",
      "status": "DIFF",
      "detail": "method acceptedTail(Llocal/lito381/server/NativePlayerCommit$Frame;)V: left=23:[133:local/lito381/server/NativeAcceptedState$Stale  , 191:java/lang/Throwable  ] right=23:[124:local/lito381/server/NativeAcceptedState$Stale  , 173:java/lang/Throwable  ]; paths rejected: path limit 4096 exceeded"
    },
    {
      "method": "assign(Llocal/lito381/server/ClientSession;Ljava/lang/Object;Ljava/lang/String;)V",
      "status": "SAME_PATHS",
      "detail": "path multisets equal (18)"
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
      "detail": "method drain(Llocal/lito381/server/ClientSession;Llocal/lito381/server/NativePlayerCommit$Queue;)V: left=10:[259:java/lang/Throwable  ] right=10:[248:java/lang/Throwable  ]; paths rejected: normal control-flow cycle"
    },
    {
      "method": "drawDefense(Llocal/lito381/server/ClientSession;Ljava/lang/String;)Llocal/lito381/server/NativeMagicDollEffects$DefenseProc;",
      "status": "DIFF",
      "detail": "method drawDefense(Llocal/lito381/server/ClientSession;Ljava/lang/String;)Llocal/lito381/server/NativeMagicDollEffects$DefenseProc;: left=21:[27:java/lang/Throwable  ] right=19:[25:java/lang/Throwable  ]; paths rejected: normal control-flow cycle"
    },
    {
      "method": "nativeCompanionSceneAssign(Llocal/lito381/server/ClientSession;Ljava/lang/Object;Ljava/lang/String;)V",
      "status": "SAME_PATHS",
      "detail": "path multisets equal (18)"
    },
    {
      "method": "nativeCompanionSceneOriginalAdvance(Llocal/lito381/server/ClientSession;Ljava/lang/Object;)V",
      "status": "SAME_PATHS",
      "detail": "path multisets equal (42)"
    },
    {
      "method": "nativeManaTransferRecover(Llocal/lito381/server/NativePlayerVitalCompletions$Receipt;Llocal/lito381/server/ClientSession;Llocal/lito381/server/NativePlayerCommit$Binding;IILjava/lang/String;)V",
      "status": "DIFF",
      "detail": "method nativeManaTransferRecover(Llocal/lito381/server/NativePlayerVitalCompletions$Receipt;Llocal/lito381/server/ClientSession;Llocal/lito381/server/NativePlayerCommit$Binding;IILjava/lang/String;)V: left=30:[144:java/lang/Throwable  ] right=32:[146:java/lang/Throwable  ]; paths rejected: path limit 4096 exceeded"
    },
    {
      "method": "nativeSourceProc68Queue(Llocal/lito381/server/ClientSession;Llocal/lito381/server/NativePlayerCommit$Binding;Llocal/lito381/server/BattleTimedStatus;Llocal/lito381/server/NativeSourceEquipmentProcLife$Proof;Llocal/lito381/server/NativeProcSkillCatalog$Catalog;Llocal/lito381/server/BattleAbilityStatus;)Llocal/lito381/server/NativePlayerCommit$Queue;",
      "status": "SAME_PATHS",
      "detail": "path multisets equal (47)"
    },
    {
      "method": "nativeSourceScalarQueue(Llocal/lito381/server/ClientSession;Llocal/lito381/server/NativePlayerCommit$Binding;Llocal/lito381/server/BattleTimedStatus;Llocal/lito381/server/NativeSourceEquipmentProcLife$Proof;Llocal/lito381/server/NativeProcSkillCatalog$Catalog;)Llocal/lito381/server/NativePlayerCommit$Queue;",
      "status": "SAME_PATHS",
      "detail": "path multisets equal (47)"
    },
    {
      "method": "nativeStaticOriginalAdvance(Llocal/lito381/server/ClientSession;)V",
      "status": "SAME_PATHS",
      "detail": "path multisets equal (48)"
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
  ],
  "effectsMethods": [],
  "approvedMethods": [],
  "firstDiff": {
    "where": "method",
    "member": "acceptedTail(Llocal/lito381/server/NativePlayerCommit$Frame;)V",
    "insnIndex": null,
    "detail": "left=23:[133:local/lito381/server/NativeAcceptedState$Stale  , 191:java/lang/Throwable  ] right=23:[124:local/lito381/server/NativeAcceptedState$Stale  , 173:java/lang/Throwable  ]; paths rejected: path limit 4096 exceeded"
  }
}
```

</details>

<details>
<summary>完整正規化差異（staged → candidate）</summary>

```diff
--- staged/NativePlayerCommit/acceptedTail-14aa78c7.txt
+++ candidate/NativePlayerCommit/acceptedTail-14aa78c7.txt
@@ -5,59 +5,59 @@

 ||||pvis=|pinv=|lvtvis=|lvtinv=

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
 GETSTATIC local/lito381/server/NativePlayerCommit.current:Ljava/lang/ThreadLocal;
 INVOKEVIRTUAL java/lang/ThreadLocal.get()Ljava/lang/Object;
 CHECKCAST local/lito381/server/NativePlayerCommit$Frame
@@ -140,16 +140,7 @@
 INVOKEVIRTUAL java/util/concurrent/atomic/AtomicLong.incrementAndGet()J
 POP2
 ILOAD 6
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
 ALOAD 2
 INVOKESTATIC local/lito381/server/NativePlayerCommit.publishDefense(Llocal/lito381/server/ClientSession;)V
 ALOAD 0
@@ -160,7 +151,7 @@
 ALOAD 5
 GETFIELD local/lito381/server/NativePlayerCommit$Event.npcFeedback:Llocal/lito381/server/NativeCombatAttacker$Feedback;
 INVOKESTATIC local/lito381/server/NativeCombatAttacker.acceptedFeedback(Llocal/lito381/server/ClientSession;Llocal/lito381/server/NativeCombatAttacker$Ref;Llocal/lito381/server/NativeCombatAttacker$Feedback;)V
-GOTO L112
+GOTO L103
 ASTORE 7
 ALOAD 2
 ALOAD 5
@@ -171,43 +162,34 @@
 ALOAD 7
 ATHROW
 ALOAD 1
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
 ASTORE 8
 ALOAD 1
-IFNONNULL L127
-GETSTATIC local/lito381/server/NativePlayerCommit.current:Ljava/lang/ThreadLocal;
-INVOKEVIRTUAL java/lang/ThreadLocal.remove()V
-GOTO L130
+IFNONNULL L118
+GETSTATIC local/lito381/server/NativePlayerCommit.current:Ljava/lang/ThreadLocal;
+INVOKEVIRTUAL java/lang/ThreadLocal.remove()V
+GOTO L121
 GETSTATIC local/lito381/server/NativePlayerCommit.current:Ljava/lang/ThreadLocal;
 ALOAD 1
 INVOKEVIRTUAL java/lang/ThreadLocal.set(Ljava/lang/Object;)V
 ALOAD 8
 ATHROW
-GOTO L245
+GOTO L218
 ASTORE 9
 ILOAD 6
-IFEQ L139
+IFEQ L130
 GETSTATIC local/lito381/server/NativePlayerCommit.committed:Ljava/util/concurrent/atomic/AtomicLong;
 INVOKEVIRTUAL java/util/concurrent/atomic/AtomicLong.incrementAndGet()J
 POP2
 ILOAD 6
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
 ALOAD 2
 INVOKESTATIC local/lito381/server/NativePlayerCommit.publishDefense(Llocal/lito381/server/ClientSession;)V
 ALOAD 0
@@ -218,49 +200,40 @@
 ALOAD 5
 GETFIELD local/lito381/server/NativePlayerCommit$Event.npcFeedback:Llocal/lito381/server/NativeCombatAttacker$Feedback;
 INVOKESTATIC local/lito381/server/NativeCombatAttacker.acceptedFeedback(Llocal/lito381/server/ClientSession;Llocal/lito381/server/NativeCombatAttacker$Ref;Llocal/lito381/server/NativeCombatAttacker$Feedback;)V
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
 GOTO L170
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
 GETSTATIC local/lito381/server/NativePlayerCommit.current:Ljava/lang/ThreadLocal;
 ALOAD 1
 INVOKEVIRTUAL java/lang/ThreadLocal.set(Ljava/lang/Object;)V
 ALOAD 11
 ATHROW
-GOTO L245
+GOTO L218
 ASTORE 12
 ILOAD 6
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
 ALOAD 2
 INVOKESTATIC local/lito381/server/NativePlayerCommit.publishDefense(Llocal/lito381/server/ClientSession;)V
 ALOAD 0
@@ -271,7 +244,7 @@
 ALOAD 5
 GETFIELD local/lito381/server/NativePlayerCommit$Event.npcFeedback:Llocal/lito381/server/NativeCombatAttacker$Feedback;
 INVOKESTATIC local/lito381/server/NativeCombatAttacker.acceptedFeedback(Llocal/lito381/server/ClientSession;Llocal/lito381/server/NativeCombatAttacker$Ref;Llocal/lito381/server/NativeCombatAttacker$Feedback;)V
-GOTO L223
+GOTO L196
 ASTORE 13
 ALOAD 2
 ALOAD 5
@@ -282,20 +255,20 @@
 ALOAD 13
 ATHROW
 ALOAD 1
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
 ASTORE 14
 ALOAD 1
-IFNONNULL L238
-GETSTATIC local/lito381/server/NativePlayerCommit.current:Ljava/lang/ThreadLocal;
-INVOKEVIRTUAL java/lang/ThreadLocal.remove()V
-GOTO L241
+IFNONNULL L211
+GETSTATIC local/lito381/server/NativePlayerCommit.current:Ljava/lang/ThreadLocal;
+INVOKEVIRTUAL java/lang/ThreadLocal.remove()V
+GOTO L214
 GETSTATIC local/lito381/server/NativePlayerCommit.current:Ljava/lang/ThreadLocal;
 ALOAD 1
 INVOKEVIRTUAL java/lang/ThreadLocal.set(Ljava/lang/Object;)V
--- staged/NativePlayerCommit/drain-bdc7c992.txt
+++ candidate/NativePlayerCommit/drain-bdc7c992.txt
@@ -5,75 +5,75 @@

 ||||pvis=|pinv=|lvtvis=|lvtinv=

-TRY 10:[259:java/lang/Throwable  ]
-TRY 19:[259:java/lang/Throwable  ]
-TRY 27:[259:java/lang/Throwable  ]
-TRY 41:[229:java/lang/Throwable  , 259:java/lang/Throwable  ]
-TRY 42:[229:java/lang/Throwable  , 259:java/lang/Throwable  ]
-TRY 47:[229:java/lang/Throwable  , 259:java/lang/Throwable  ]
-TRY 50:[229:java/lang/Throwable  , 259:java/lang/Throwable  ]
-TRY 55:[229:java/lang/Throwable  , 259:java/lang/Throwable  ]
-TRY 58:[229:java/lang/Throwable  , 259:java/lang/Throwable  ]
-TRY 61:[229:java/lang/Throwable  , 259:java/lang/Throwable  ]
-TRY 63:[229:java/lang/Throwable  , 259:java/lang/Throwable  ]
-TRY 67:[229:java/lang/Throwable  , 259:java/lang/Throwable  ]
-TRY 72:[229:java/lang/Throwable  , 259:java/lang/Throwable  ]
-TRY 74:[229:java/lang/Throwable  , 259:java/lang/Throwable  ]
-TRY 75:[229:java/lang/Throwable  , 259:java/lang/Throwable  ]
-TRY 77:[229:java/lang/Throwable  , 259:java/lang/Throwable  ]
-TRY 80:[229:java/lang/Throwable  , 259:java/lang/Throwable  ]
-TRY 81:[229:java/lang/Throwable  , 259:java/lang/Throwable  ]
-TRY 86:[208:java/lang/Throwable  , 229:java/lang/Throwable  , 259:java/lang/Throwable  ]
-TRY 91:[208:java/lang/Throwable  , 229:java/lang/Throwable  , 259:java/lang/Throwable  ]
-TRY 94:[208:java/lang/Throwable  , 229:java/lang/Throwable  , 259:java/lang/Throwable  ]
-TRY 97:[208:java/lang/Throwable  , 229:java/lang/Throwable  , 259:java/lang/Throwable  ]
-TRY 99:[208:java/lang/Throwable  , 229:java/lang/Throwable  , 259:java/lang/Throwable  ]
-TRY 103:[208:java/lang/Throwable  , 229:java/lang/Throwable  , 259:java/lang/Throwable  ]
-TRY 104:[208:java/lang/Throwable  , 229:java/lang/Throwable  , 259:java/lang/Throwable  ]
-TRY 106:[208:java/lang/Throwable  , 229:java/lang/Throwable  , 259:java/lang/Throwable  ]
-TRY 111:[208:java/lang/Throwable  , 229:java/lang/Throwable  , 259:java/lang/Throwable  ]
-TRY 117:[208:java/lang/Throwable  , 229:java/lang/Throwable  , 259:java/lang/Throwable  ]
-TRY 121:[208:java/lang/Throwable  , 229:java/lang/Throwable  , 259:java/lang/Throwable  ]
-TRY 125:[208:java/lang/Throwable  , 229:java/lang/Throwable  , 259:java/lang/Throwable  ]
-TRY 135:[208:java/lang/Throwable  , 229:java/lang/Throwable  , 259:java/lang/Throwable  ]
-TRY 136:[208:java/lang/Throwable  , 229:java/lang/Throwable  , 259:java/lang/Throwable  ]
-TRY 137:[208:java/lang/Throwable  , 229:java/lang/Throwable  , 259:java/lang/Throwable  ]
-TRY 140:[229:java/lang/Throwable  , 259:java/lang/Throwable  ]
-TRY 142:[229:java/lang/Throwable  , 259:java/lang/Throwable  ]
-TRY 149:[208:java/lang/Throwable  , 229:java/lang/Throwable  , 259:java/lang/Throwable  ]
-TRY 151:[208:java/lang/Throwable  , 229:java/lang/Throwable  , 259:java/lang/Throwable  ]
-TRY 153:[208:java/lang/Throwable  , 229:java/lang/Throwable  , 259:java/lang/Throwable  ]
-TRY 156:[208:java/lang/Throwable  , 229:java/lang/Throwable  , 259:java/lang/Throwable  ]
-TRY 165:[208:java/lang/Throwable  , 229:java/lang/Throwable  , 259:java/lang/Throwable  ]
-TRY 169:[208:java/lang/Throwable  , 229:java/lang/Throwable  , 259:java/lang/Throwable  ]
-TRY 171:[208:java/lang/Throwable  , 229:java/lang/Throwable  , 259:java/lang/Throwable  ]
-TRY 172:[208:java/lang/Throwable  , 229:java/lang/Throwable  , 259:java/lang/Throwable  ]
-TRY 176:[208:java/lang/Throwable  , 229:java/lang/Throwable  , 259:java/lang/Throwable  ]
-TRY 177:[208:java/lang/Throwable  , 229:java/lang/Throwable  , 259:java/lang/Throwable  ]
-TRY 184:[208:java/lang/Throwable  , 229:java/lang/Throwable  , 259:java/lang/Throwable  ]
-TRY 185:[208:java/lang/Throwable  , 229:java/lang/Throwable  , 259:java/lang/Throwable  ]
-TRY 186:[208:java/lang/Throwable  , 229:java/lang/Throwable  , 259:java/lang/Throwable  ]
-TRY 196:[208:java/lang/Throwable  , 229:java/lang/Throwable  , 259:java/lang/Throwable  ]
-TRY 199:[208:java/lang/Throwable  , 229:java/lang/Throwable  , 259:java/lang/Throwable  ]
-TRY 201:[208:java/lang/Throwable  , 229:java/lang/Throwable  , 259:java/lang/Throwable  ]
-TRY 204:[229:java/lang/Throwable  , 259:java/lang/Throwable  ]
-TRY 206:[229:java/lang/Throwable  , 259:java/lang/Throwable  ]
-TRY 210:[229:java/lang/Throwable  , 259:java/lang/Throwable  ]
-TRY 212:[229:java/lang/Throwable  , 259:java/lang/Throwable  ]
-TRY 214:[229:java/lang/Throwable  , 259:java/lang/Throwable  ]
-TRY 216:[229:java/lang/Throwable  , 259:java/lang/Throwable  ]
-TRY 218:[229:java/lang/Throwable  , 259:java/lang/Throwable  ]
-TRY 231:[259:java/lang/Throwable  ]
-TRY 233:[259:java/lang/Throwable  ]
-TRY 237:[259:java/lang/Throwable  ]
-TRY 238:[259:java/lang/Throwable  ]
-TRY 241:[259:java/lang/Throwable  ]
-TRY 242:[259:java/lang/Throwable  ]
-TRY 244:[259:java/lang/Throwable  ]
-TRY 246:[259:java/lang/Throwable  ]
-TRY 247:[259:java/lang/Throwable  ]
-TRY 248:[259:java/lang/Throwable  ]
-TRY 250:[259:java/lang/Throwable  ]
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
 CONST null
 ASTORE 2
 CONST Integer:0
@@ -82,35 +82,25 @@
 ISTORE 4
 ILOAD 4
 CONST Integer:256
-IF_ICMPGE L253
+IF_ICMPGE L242
 ALOAD 1
 INVOKEVIRTUAL local/lito381/server/NativePlayerCommit$Queue.waitNanos()J
 LSTORE 5/6
 LLOAD 5/6
 CONST Long:0
 LCMP
-IFLE L26
+IFLE L21
 ALOAD 0
 ALOAD 1
 LLOAD 5/6
 INVOKESTATIC local/lito381/server/NativePlayerLifetime.deadline(Llocal/lito381/server/ClientSession;Llocal/lito381/server/NativePlayerCommit$Queue;J)V
-ILOAD 3
-IFNE L25
-ALOAD 0
-ALOAD 1
-INVOKESTATIC local/lito381/server/NativePlayerCommit.resumeAccepted(Llocal/lito381/server/ClientSession;Llocal/lito381/server/NativePlayerCommit$Queue;)V
-RETURN
+GOTO L242
 ALOAD 1
 INVOKEVIRTUAL local/lito381/server/NativePlayerCommit$Queue.claim()Llocal/lito381/server/NativePlayerCommit$Event;
 ASTORE 7
 ALOAD 7
-IFNONNULL L37
-ILOAD 3
-IFNE L36
-ALOAD 0
-ALOAD 1
-INVOKESTATIC local/lito381/server/NativePlayerCommit.resumeAccepted(Llocal/lito381/server/ClientSession;Llocal/lito381/server/NativePlayerCommit$Queue;)V
-RETURN
+IFNONNULL L27
+GOTO L242
 CONST Integer:0
 ISTORE 8
 ALOAD 0
@@ -127,22 +117,22 @@
 INVOKESTATIC local/lito381/server/NativePlayerVitals.gm(Llocal/lito381/server/ClientSession;)Z
 ISTORE 11
 ILOAD 10
-IFEQ L77
+IFEQ L67
 ALOAD 1
 GETFIELD local/lito381/server/NativePlayerCommit$Queue.closed:Z
-IFNE L77
+IFNE L67
 ALOAD 1
 GETFIELD local/lito381/server/NativePlayerCommit$Queue.terminal:Z
-IFNE L77
+IFNE L67
 ALOAD 7
 GETFIELD local/lito381/server/NativePlayerCommit$Event.target:Llocal/lito381/server/NativePlayerCommit$Binding;
 ALOAD 0
 INVOKEVIRTUAL local/lito381/server/NativePlayerCommit$Binding.matchesFast(Llocal/lito381/server/ClientSession;)Z
-IFEQ L77
+IFEQ L67
 ALOAD 0
 CONST str:"playerHp"
 INVOKESTATIC local/lito381/server/NativePlayerCommit.integer(Ljava/lang/Object;Ljava/lang/String;)I
-IFLE L77
+IFLE L67
 ALOAD 0
 ALOAD 9
 ALOAD 7
@@ -150,7 +140,7 @@
 ALOAD 7
 GETFIELD local/lito381/server/NativePlayerCommit$Event.attacker:Llocal/lito381/server/NativeCombatAttacker$Ref;
 INVOKESTATIC local/lito381/server/NativeEquipmentDefense.resolve(Llocal/lito381/server/ClientSession;Llocal/lito381/server/ClientSession;Ljava/util/List;Llocal/lito381/server/NativeCombatAttacker$Ref;)Llocal/lito381/server/NativeEquipmentDefense$Resolved;
-GOTO L78
+GOTO L68
 INVOKESTATIC local/lito381/server/NativeEquipmentDefense$Resolved.empty()Llocal/lito381/server/NativeEquipmentDefense$Resolved;
 ASTORE 12
 ALOAD 1
@@ -163,51 +153,30 @@
 INVOKESTATIC local/lito381/server/NativeHpCompletionAdmission.reserve(Llocal/lito381/server/ClientSession;Llocal/lito381/server/NativePlayerCommit$Queue;)Llocal/lito381/server/NativePlayerVitalCompletions$Receipt;
 ASTORE 13
 ALOAD 13
-IFNULL L134
+IFNULL L103
 ALOAD 1
 GETFIELD local/lito381/server/NativePlayerCommit$Queue.closed:Z
-IFNE L134
+IFNE L103
 ALOAD 1
 GETFIELD local/lito381/server/NativePlayerCommit$Queue.terminal:Z
-IFNE L134
+IFNE L103
 ALOAD 7
 GETFIELD local/lito381/server/NativePlayerCommit$Event.target:Llocal/lito381/server/NativePlayerCommit$Binding;
 ALOAD 0
 INVOKEVIRTUAL local/lito381/server/NativePlayerCommit$Binding.matchesFast(Llocal/lito381/server/ClientSession;)Z
-IFEQ L134
+IFEQ L103
 ALOAD 0
 CONST str:"phase"
 INVOKESTATIC local/lito381/server/CombatAccess.field(Ljava/lang/Object;Ljava/lang/String;)Ljava/lang/Object;
 INVOKESTATIC java/lang/String.valueOf(Ljava/lang/Object;)Ljava/lang/String;
 ALOAD 7
 GETFIELD local/lito381/server/NativePlayerCommit$Event.world:Z
-IFEQ L110
+IFEQ L100
 CONST str:"WORLD"
-GOTO L111
+GOTO L101
 CONST str:"BATTLE"
 INVOKEVIRTUAL java/lang/String.equals(Ljava/lang/Object;)Z
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
 ALOAD 7
 INVOKESTATIC local/lito381/server/NativeAcceptedFreezeMutation.abandon(Llocal/lito381/server/NativePlayerCommit$Event;)V
 GETSTATIC local/lito381/server/NativePlayerCommit.dropped:Ljava/util/concurrent/atomic/AtomicLong;
@@ -217,9 +186,29 @@
 GETFIELD local/lito381/server/NativePlayerCommit$Queue.vitalLock:Ljava/util/concurrent/locks/ReentrantLock;
 ALOAD 13
 INVOKESTATIC local/lito381/server/NativeHpCompletionAdmission.unlock(Ljava/util/concurrent/locks/ReentrantLock;Llocal/lito381/server/NativePlayerVitalCompletions$Receipt;)V
-GOTO L251
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
 ILOAD 8
-IFEQ L159
+IFEQ L148
 ILOAD 15
 ILOAD 14
 ALOAD 7
@@ -232,11 +221,11 @@
 ALOAD 16
 INVOKEVIRTUAL local/lito381/server/NativePlayerVitals$Result.after()I
 ISTORE 17
-GOTO L174
+GOTO L163
 ILOAD 11
-IFEQ L163
-ILOAD 15
-GOTO L164
+IFEQ L152
+ILOAD 15
+GOTO L153
 CONST Integer:0
 ISTORE 17
 NEW local/lito381/server/NativePlayerVitals$Result
@@ -254,7 +243,7 @@
 PUTFIELD local/lito381/server/NativePlayerCommit$Queue.terminal:Z
 ILOAD 17
 ILOAD 15
-IF_ICMPEQ L186
+IF_ICMPEQ L175
 ALOAD 0
 CONST str:"playerHp"
 ILOAD 17
@@ -281,7 +270,7 @@
 GETFIELD local/lito381/server/NativePlayerCommit$Queue.vitalLock:Ljava/util/concurrent/locks/ReentrantLock;
 ALOAD 13
 INVOKESTATIC local/lito381/server/NativeHpCompletionAdmission.unlock(Ljava/util/concurrent/locks/ReentrantLock;Llocal/lito381/server/NativePlayerVitalCompletions$Receipt;)V
-GOTO L215
+GOTO L204
 ASTORE 19
 ALOAD 1
 GETFIELD local/lito381/server/NativePlayerCommit$Queue.vitalLock:Ljava/util/concurrent/locks/ReentrantLock;
@@ -293,28 +282,28 @@
 INVOKESTATIC local/lito381/server/NativePlayerVitalCompletions.drain(Llocal/lito381/server/ClientSession;)V
 ALOAD 18
 INVOKEVIRTUAL local/lito381/server/NativeAcceptedHpCompletion.suspend()Z
-IFEQ L228
+IFEQ L217
 CONST Integer:1
 ISTORE 20
 ILOAD 20
-IFNE L227
+IFNE L216
 ALOAD 0
 ALOAD 1
 INVOKESTATIC local/lito381/server/NativePlayerCommit.resumeAccepted(Llocal/lito381/server/ClientSession;Llocal/lito381/server/NativePlayerCommit$Queue;)V
 RETURN
-GOTO L251
+GOTO L240
 ASTORE 21
 ALOAD 7
 INVOKESTATIC local/lito381/server/NativeAcceptedFreezeMutation.abandon(Llocal/lito381/server/NativePlayerCommit$Event;)V
 ALOAD 21
 INVOKESTATIC local/lito381/server/NativeAcceptedState.isStale(Ljava/lang/Throwable;)Z
-IFEQ L241
+IFEQ L230
 ILOAD 8
-IFEQ L251
+IFEQ L229
 GETSTATIC local/lito381/server/NativePlayerCommit.committed:Ljava/util/concurrent/atomic/AtomicLong;
 INVOKEVIRTUAL java/util/concurrent/atomic/AtomicLong.incrementAndGet()J
 POP2
-GOTO L251
+GOTO L240
 GETSTATIC local/lito381/server/NativePlayerCommit.failed:Ljava/util/concurrent/atomic/AtomicLong;
 INVOKEVIRTUAL java/util/concurrent/atomic/AtomicLong.incrementAndGet()J
 POP2
@@ -328,16 +317,17 @@
 IINC var=4 incr=1
 GOTO L6
 ILOAD 3
-IFNE L258
+IFNE L256
 ALOAD 0
 ALOAD 1
 INVOKESTATIC local/lito381/server/NativePlayerCommit.resumeAccepted(Llocal/lito381/server/ClientSession;Llocal/lito381/server/NativePlayerCommit$Queue;)V
-RETURN
+GOTO L256
 ASTORE 22
 ILOAD 3
-IFNE L265
+IFNE L254
 ALOAD 0
 ALOAD 1
 INVOKESTATIC local/lito381/server/NativePlayerCommit.resumeAccepted(Llocal/lito381/server/ClientSession;Llocal/lito381/server/NativePlayerCommit$Queue;)V
 ALOAD 22
 ATHROW
+RETURN
--- staged/NativePlayerCommit/drawDefense-b49748c1.txt
+++ candidate/NativePlayerCommit/drawDefense-b49748c1.txt
@@ -5,26 +5,26 @@

 ||||pvis=|pinv=|lvtvis=|lvtinv=

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
 GETSTATIC local/lito381/server/NativePlayerCommit.calculation:Ljava/lang/ThreadLocal;
 INVOKEVIRTUAL java/lang/ThreadLocal.get()Ljava/lang/Object;
 CHECKCAST local/lito381/server/NativePlayerCommit$CalcTicket
@@ -39,64 +39,63 @@
 ALOAD 0
 CONST str:"activeMagicDollObjectIds"
 INVOKESTATIC local/lito381/server/CombatAccess.field(Ljava/lang/Object;Ljava/lang/String;)Ljava/lang/Object;
+DUP
 ASTORE 3
-ALOAD 3
-DUP
-ASTORE 4
 MONITORENTER
 ALOAD 0
 CONST str:"activeMagicDoll"
 INVOKESTATIC local/lito381/server/NativeSourceDollDefense.dollField(Ljava/lang/Object;Ljava/lang/String;)Ljava/lang/Object;
 CHECKCAST local/lito381/server/NativeMagicDoll
+ASTORE 4
+ALOAD 3
+MONITOREXIT
+GOTO L30
 ASTORE 5
-ALOAD 4
+ALOAD 3
 MONITOREXIT
-GOTO L32
-ASTORE 6
-ALOAD 4
-MONITOREXIT
-ALOAD 6
+ALOAD 5
 ATHROW
 CONST Integer:0
-ISTORE 7
+ISTORE 6
 NEW java/util/ArrayList
 DUP
 INVOKESPECIAL java/util/ArrayList.<init>()V
-ASTORE 8
-ALOAD 5
-IFNULL L105
-ALOAD 5
+ASTORE 7
+ALOAD 4
+IFNULL L104
+ALOAD 4
 INVOKEVIRTUAL local/lito381/server/NativeMagicDoll.type()Llocal/lito381/server/SourceMagicDollType;
 INVOKEVIRTUAL local/lito381/server/SourceMagicDollType.powerDefinitions()Ljava/util/List;
 INVOKEINTERFACE java/util/List.iterator()Ljava/util/Iterator; itf
+ASTORE 8
+ALOAD 8
+INVOKEINTERFACE java/util/Iterator.hasNext()Z itf
+IFEQ L104
+ALOAD 8
+INVOKEINTERFACE java/util/Iterator.next()Ljava/lang/Object; itf
+CHECKCAST local/lito381/server/SourceMagicDollPower
 ASTORE 9
 ALOAD 9
-INVOKEINTERFACE java/util/Iterator.hasNext()Z itf
-IFEQ L105
+IFNONNULL L53
+GOTO L43
 ALOAD 9
-INVOKEINTERFACE java/util/Iterator.next()Ljava/lang/Object; itf
-CHECKCAST local/lito381/server/SourceMagicDollPower
-ASTORE 10
-ALOAD 10
-IFNULL L104
-ALOAD 10
 INVOKEVIRTUAL local/lito381/server/SourceMagicDollPower.className()Ljava/lang/String;
 CONST str:"ACTIVE_REDUX"
 INVOKEVIRTUAL java/lang/String.equals(Ljava/lang/Object;)Z
-IFNE L68
-ALOAD 10
+IFNE L67
+ALOAD 9
 INVOKEVIRTUAL local/lito381/server/SourceMagicDollPower.className()Ljava/lang/String;
 CONST str:"ACTIVE_DMG"
 INVOKEVIRTUAL java/lang/String.equals(Ljava/lang/Object;)Z
-IFEQ L71
-ALOAD 10
+IFEQ L70
+ALOAD 9
 INVOKEVIRTUAL local/lito381/server/SourceMagicDollPower.mode()I
 CONST Integer:2
-IF_ICMPNE L71
-ALOAD 10
+IF_ICMPNE L70
+ALOAD 9
 INVOKEVIRTUAL local/lito381/server/SourceMagicDollPower.probability()I
-IFGT L72
-GOTO L45
+IFGT L71
+GOTO L43
 GETSTATIC local/lito381/server/NativePlayerCommit.defenseRolls:Ljava/util/concurrent/atomic/AtomicLong;
 INVOKEVIRTUAL java/util/concurrent/atomic/AtomicLong.incrementAndGet()J
 POP2
@@ -104,56 +103,56 @@
 CONST Integer:1000
 INVOKEVIRTUAL java/util/concurrent/ThreadLocalRandom.nextInt(I)I
 CONST Integer:1000
-ALOAD 10
+ALOAD 9
 INVOKEVIRTUAL local/lito381/server/SourceMagicDollPower.probability()I
 INVOKESTATIC java/lang/Math.min(II)I
-IF_ICMPLT L84
-GOTO L45
+IF_ICMPLT L83
+GOTO L43
 CONST Long:2147483647
-ILOAD 7
+ILOAD 6
 I2L
 CONST Integer:0
-ALOAD 10
+ALOAD 9
 INVOKEVIRTUAL local/lito381/server/SourceMagicDollPower.valueSub()I
 INVOKESTATIC java/lang/Math.max(II)I
 I2L
 LADD
 INVOKESTATIC java/lang/Math.min(JJ)J
 L2I
-ISTORE 7
-ALOAD 8
-ALOAD 10
+ISTORE 6
+ALOAD 7
+ALOAD 9
 INVOKEVIRTUAL java/util/ArrayList.add(Ljava/lang/Object;)Z
 POP
-GOTO L104
-ASTORE 11
-ALOAD 11
+GOTO L103
+ASTORE 10
+ALOAD 10
 INVOKESTATIC local/lito381/server/NativePlayerCommit.defenseCalcFailure(Ljava/lang/RuntimeException;)V
-GOTO L45
+GOTO L43
 NEW local/lito381/server/NativeMagicDollEffects$DefenseProc
 DUP
-ILOAD 7
-ALOAD 8
+ILOAD 6
+ALOAD 7
 INVOKESPECIAL local/lito381/server/NativeMagicDollEffects$DefenseProc.<init>(ILjava/util/List;)V
+ASTORE 11
+ALOAD 4
+IFNULL L140
+ALOAD 7
+INVOKEVIRTUAL java/util/ArrayList.iterator()Ljava/util/Iterator;
 ASTORE 12
-ALOAD 5
-IFNULL L141
-ALOAD 8
-INVOKEVIRTUAL java/util/ArrayList.iterator()Ljava/util/Iterator;
-ASTORE 13
-ALOAD 13
+ALOAD 12
 INVOKEINTERFACE java/util/Iterator.hasNext()Z itf
-IFEQ L141
-ALOAD 13
+IFEQ L140
+ALOAD 12
 INVOKEINTERFACE java/util/Iterator.next()Ljava/lang/Object; itf
 CHECKCAST local/lito381/server/SourceMagicDollPower
-ASTORE 14
+ASTORE 13
 ALOAD 2
 GETFIELD local/lito381/server/NativePlayerCommit$CalcTicket.defense:Ljava/util/List;
 NEW local/lito381/server/NativePlayerCommit$DefenseVisual
 DUP
-ALOAD 5
-ALOAD 14
+ALOAD 4
+ALOAD 13
 INVOKEVIRTUAL local/lito381/server/SourceMagicDollPower.id()I
 CONST Integer:0
 ALOAD 2
@@ -165,6 +164,6 @@
 INVOKESPECIAL local/lito381/server/NativePlayerCommit$DefenseVisual.<init>(Llocal/lito381/server/NativeMagicDoll;IIJLjava/lang/String;Llocal/lito381/server/ClientSession;)V
 INVOKEINTERFACE java/util/List.add(Ljava/lang/Object;)Z itf
 POP
-GOTO L116
-ALOAD 12
+GOTO L115
+ALOAD 11
 ARETURN
--- staged/NativePlayerCommit/nativeManaTransferRecover-406af235.txt
+++ candidate/NativePlayerCommit/nativeManaTransferRecover-406af235.txt
@@ -5,27 +5,27 @@

 ||||pvis=|pinv=|lvtvis=|lvtinv=

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
 ILOAD 3
 IFGT L5
 ILOAD 4
@@ -38,10 +38,12 @@
 ALOAD 6
 IFNONNULL L12
 RETURN
+ALOAD 1
+POP
 ALOAD 0
 ASTORE 7
 ALOAD 7
-IFNONNULL L17
+IFNONNULL L19
 RETURN
 CONST Integer:0
 ISTORE 8
@@ -60,15 +62,15 @@
 INVOKEVIRTUAL java/util/concurrent/locks/ReentrantLock.lock()V
 ALOAD 6
 GETFIELD local/lito381/server/NativePlayerCommit$Queue.closed:Z
-IFNE L129
+IFNE L131
 ALOAD 2
 ALOAD 1
 INVOKEVIRTUAL local/lito381/server/NativePlayerCommit$Binding.matchesFast(Llocal/lito381/server/ClientSession;)Z
-IFEQ L129
+IFEQ L131
 ALOAD 1
 CONST str:"playerHp"
 INVOKESTATIC local/lito381/server/NativePlayerCommit.integer(Ljava/lang/Object;Ljava/lang/String;)I
-IFLE L129
+IFLE L131
 ALOAD 1
 CONST str:"playerHp"
 INVOKESTATIC local/lito381/server/NativePlayerCommit.integer(Ljava/lang/Object;Ljava/lang/String;)I
@@ -86,7 +88,7 @@
 INVOKESTATIC local/lito381/server/NativePlayerCommit.integer(Ljava/lang/Object;Ljava/lang/String;)I
 ISTORE 17
 ILOAD 3
-IFLE L71
+IFLE L73
 ILOAD 16
 I2L
 ILOAD 14
@@ -96,11 +98,11 @@
 LADD
 INVOKESTATIC java/lang/Math.min(JJ)J
 L2I
-GOTO L72
+GOTO L74
 ILOAD 14
 ISTORE 18
 ILOAD 4
-IFLE L85
+IFLE L87
 ILOAD 17
 I2L
 ILOAD 15
@@ -110,41 +112,41 @@
 LADD
 INVOKESTATIC java/lang/Math.min(JJ)J
 L2I
-GOTO L86
+GOTO L88
 ILOAD 15
 ISTORE 19
 ILOAD 18
 ILOAD 14
-IF_ICMPEQ L92
+IF_ICMPEQ L94
 CONST Integer:1
-GOTO L93
+GOTO L95
 CONST Integer:0
 ISTORE 20
 ILOAD 19
 ILOAD 15
-IF_ICMPEQ L99
+IF_ICMPEQ L101
 CONST Integer:1
-GOTO L100
+GOTO L102
 CONST Integer:0
 ISTORE 21
 ILOAD 20
-IFEQ L108
+IFEQ L110
 ALOAD 1
 CONST str:"playerHp"
 ILOAD 18
 INVOKESTATIC java/lang/Integer.valueOf(I)Ljava/lang/Integer;
 INVOKESTATIC local/lito381/server/CombatAccess.set(Ljava/lang/Object;Ljava/lang/String;Ljava/lang/Object;)V
 ILOAD 21
-IFEQ L115
+IFEQ L117
 ALOAD 1
 CONST str:"playerMp"
 ILOAD 19
 INVOKESTATIC java/lang/Integer.valueOf(I)Ljava/lang/Integer;
 INVOKESTATIC local/lito381/server/CombatAccess.set(Ljava/lang/Object;Ljava/lang/String;Ljava/lang/Object;)V
 ILOAD 20
-IFNE L119
+IFNE L121
 ILOAD 21
-IFEQ L129
+IFEQ L131
 ALOAD 7
 ALOAD 2
 ILOAD 18
@@ -158,7 +160,7 @@
 ALOAD 6
 GETFIELD local/lito381/server/NativePlayerCommit$Queue.vitalLock:Ljava/util/concurrent/locks/ReentrantLock;
 INVOKEVIRTUAL java/util/concurrent/locks/ReentrantLock.unlock()V
-GOTO L139
+GOTO L141
 ASTORE 22
 ALOAD 6
 GETFIELD local/lito381/server/NativePlayerCommit$Queue.vitalLock:Ljava/util/concurrent/locks/ReentrantLock;
@@ -169,7 +171,7 @@
 INVOKEVIRTUAL local/lito381/server/NativePlayerVitalCompletions$Receipt.close()V
 ALOAD 1
 INVOKESTATIC local/lito381/server/NativePlayerVitalCompletions.drain(Llocal/lito381/server/ClientSession;)V
-GOTO L151
+GOTO L153
 ASTORE 23
 ALOAD 7
 INVOKEVIRTUAL local/lito381/server/NativePlayerVitalCompletions$Receipt.close()V
--- staged/NativePlayerCommit/publishDefense-390f6c66.txt
+++ candidate/NativePlayerCommit/publishDefense-390f6c66.txt
@@ -174,7 +174,7 @@
 ALOAD 0
 INVOKESTATIC local/lito381/server/NativePlayerCommit.validFrame(Llocal/lito381/server/ClientSession;)Z
 IFNE L101
-GOTO L28
+GOTO L111
 ALOAD 7
 ALOAD 4
 ALOAD 5
```

</details>

### NativePlayerLifetime — DIFF；待 #203（CFG／迴圈與資料流）

<details>
<summary>全部方法診斷</summary>

```json
{
  "name": "local/lito381/server/NativePlayerLifetime",
  "status": "DIFF",
  "byteSame": false,
  "allowMethodsApplied": false,
  "methodResults": [
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
  ],
  "effectsMethods": [],
  "approvedMethods": [],
  "firstDiff": {
    "where": "method",
    "member": "shutdown(ZJ)Llocal/lito381/server/NativePlayerLifetime$Shutdown;",
    "insnIndex": null,
    "detail": "left=25:[44:java/lang/Throwable  ] right=23:[42:java/lang/Throwable  ]; paths rejected: normal control-flow cycle"
  }
}
```

</details>

<details>
<summary>完整正規化差異（staged → candidate）</summary>

```diff
--- staged/NativePlayerLifetime/shutdown-964ada94.txt
+++ candidate/NativePlayerLifetime/shutdown-964ada94.txt
@@ -5,36 +5,36 @@

 ||||pvis=|pinv=|lvtvis=|lvtinv=

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
 GETSTATIC local/lito381/server/NativePlayerLifetime.worker:Ljava/lang/ThreadLocal;
 INVOKEVIRTUAL java/lang/ThreadLocal.get()Ljava/lang/Object;
 ALOAD 0
@@ -54,41 +54,37 @@
 ISTORE 7
 ALOAD 0
 GETFIELD local/lito381/server/NativePlayerLifetime.admission:Ljava/lang/Object;
+DUP
 ASTORE 8
-ALOAD 8
-DUP
-ASTORE 9
 MONITORENTER
 ALOAD 0
 GETFIELD local/lito381/server/NativePlayerLifetime.mode:Llocal/lito381/server/NativePlayerLifetime$Mode;
 GETSTATIC local/lito381/server/NativePlayerLifetime$Mode.OPEN:Llocal/lito381/server/NativePlayerLifetime$Mode;
-IF_ACMPNE L36
+IF_ACMPNE L34
 ALOAD 0
 ILOAD 1
-IFEQ L33
+IFEQ L31
 GETSTATIC local/lito381/server/NativePlayerLifetime$Mode.DRAINING:Llocal/lito381/server/NativePlayerLifetime$Mode;
-GOTO L34
+GOTO L32
 GETSTATIC local/lito381/server/NativePlayerLifetime$Mode.STOPPED:Llocal/lito381/server/NativePlayerLifetime$Mode;
 PUTFIELD local/lito381/server/NativePlayerLifetime.mode:Llocal/lito381/server/NativePlayerLifetime$Mode;
-GOTO L41
+GOTO L39
 ILOAD 1
-IFNE L41
+IFNE L39
 ALOAD 0
 GETSTATIC local/lito381/server/NativePlayerLifetime$Mode.STOPPED:Llocal/lito381/server/NativePlayerLifetime$Mode;
 PUTFIELD local/lito381/server/NativePlayerLifetime.mode:Llocal/lito381/server/NativePlayerLifetime$Mode;
+ALOAD 8
+MONITOREXIT
+GOTO L47
+ASTORE 9
+ALOAD 8
+MONITOREXIT
 ALOAD 9
-MONITOREXIT
-GOTO L49
-ASTORE 10
-ALOAD 9
-MONITOREXIT
-ALOAD 10
 ATHROW
 ALOAD 0
-ASTORE 11
-ALOAD 11
+DUP
 INVOKESTATIC local/lito381/server/NativeSummonRetirement.closing(Llocal/lito381/server/NativePlayerLifetime;)V
-ALOAD 11
 INVOKESTATIC local/lito381/server/NativeDeCorpseTimers.closeAll(Llocal/lito381/server/NativePlayerLifetime;)V
 ALOAD 0
 INVOKESTATIC local/lito381/server/NativeDePerceptionJobs.closeAll(Llocal/lito381/server/NativePlayerLifetime;)V
@@ -101,99 +97,94 @@
 INVOKESTATIC java/lang/Math.max(JJ)J
 INVOKEVIRTUAL java/util/concurrent/TimeUnit.toNanos(J)J
 LADD
-LSTORE 12/13
+LSTORE 10/11
 ILOAD 1
-IFEQ L142
+IFEQ L135
 ALOAD 0
 GETFIELD local/lito381/server/NativePlayerLifetime.mode:Llocal/lito381/server/NativePlayerLifetime$Mode;
 GETSTATIC local/lito381/server/NativePlayerLifetime$Mode.DRAINING:Llocal/lito381/server/NativePlayerLifetime$Mode;
-IF_ACMPNE L142
+IF_ACMPNE L135
 ALOAD 0
 GETFIELD local/lito381/server/NativePlayerLifetime.active:Ljava/util/Set;
 INVOKEDYNAMIC apply()Ljava/util/function/IntFunction; bsm=H6 java/lang/invoke/LambdaMetafactory.metafactory(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodHandle;Ljava/lang/invoke/MethodType;)Ljava/lang/invoke/CallSite; args=[type:(I)Ljava/lang/Object;, handle:H6 local/lito381/server/NativePlayerLifetime.lambda$content$1cc49100a34d1206c805b86551e739b3bc87ee7a208c1696f77a8bf39875d020(I)[Llocal/lito381/server/NativePlayerCommit$Queue;, type:(I)[Llocal/lito381/server/NativePlayerCommit$Queue;]
 INVOKEINTERFACE java/util/Set.toArray(Ljava/util/function/IntFunction;)[Ljava/lang/Object; itf
 CHECKCAST [Llocal/lito381/server/NativePlayerCommit$Queue;
-ASTORE 14
-ALOAD 14
+ASTORE 12
+ALOAD 12
 ARRAYLENGTH
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
 AALOAD
-ASTORE 17
-ALOAD 17
+ASTORE 15
+ALOAD 15
 INVOKEVIRTUAL local/lito381/server/NativePlayerCommit$Queue.requestDrain()V
-IINC var=16 incr=1
-GOTO L84
+IINC var=14 incr=1
+GOTO L80
 ALOAD 0
 GETFIELD local/lito381/server/NativePlayerLifetime.npcs:Ljava/util/Set;
 INVOKEDYNAMIC apply()Ljava/util/function/IntFunction; bsm=H6 java/lang/invoke/LambdaMetafactory.metafactory(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodHandle;Ljava/lang/invoke/MethodType;)Ljava/lang/invoke/CallSite; args=[type:(I)Ljava/lang/Object;, handle:H6 local/lito381/server/NativePlayerLifetime.lambda$content$4cc0bd50f5c7fd162b1842d935a24d556bb25ee07f378a80ea7c782095802936(I)[Llocal/lito381/server/NativeNpcLifetime$Lane;, type:(I)[Llocal/lito381/server/NativeNpcLifetime$Lane;]
 INVOKEINTERFACE java/util/Set.toArray(Ljava/util/function/IntFunction;)[Ljava/lang/Object; itf
 CHECKCAST [Llocal/lito381/server/NativeNpcLifetime$Lane;
-ASTORE 18
-ALOAD 18
+ASTORE 16
+ALOAD 16
 ARRAYLENGTH
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
 AALOAD
-ASTORE 21
-ALOAD 21
+ASTORE 19
+ALOAD 19
 INVOKEVIRTUAL local/lito381/server/NativeNpcLifetime$Lane.requestDrain()V
-IINC var=20 incr=1
-GOTO L106
-ALOAD 0
-ASTORE 22
-ALOAD 22
+IINC var=18 incr=1
+GOTO L102
+ALOAD 0
+DUP
 INVOKESTATIC local/lito381/server/NativeSummonRetirement.drain(Llocal/lito381/server/NativePlayerLifetime;)V
-ALOAD 22
 INVOKEVIRTUAL local/lito381/server/NativePlayerLifetime.idle()Z
-IFEQ L127
+IFEQ L121
 CONST Integer:1
 ISTORE 7
-GOTO L142
+GOTO L135
 INVOKESTATIC java/lang/System.nanoTime()J
-LLOAD 12/13
+LLOAD 10/11
 LSUB
 CONST Long:0
 LCMP
-IFLT L134
-GOTO L142
+IFLT L128
+GOTO L135
 CONST Long:1
 INVOKESTATIC java/lang/Thread.sleep(J)V
-GOTO L141
-ASTORE 23
+GOTO L69
+ASTORE 20
 CONST Integer:1
 ISTORE 6
-GOTO L142
-GOTO L73
+GOTO L135
 ALOAD 0
 GETFIELD local/lito381/server/NativePlayerLifetime.admission:Ljava/lang/Object;
-ASTORE 24
-ALOAD 24
-DUP
-ASTORE 25
+DUP
+ASTORE 21
 MONITORENTER
 ALOAD 0
 GETSTATIC local/lito381/server/NativePlayerLifetime$Mode.STOPPED:Llocal/lito381/server/NativePlayerLifetime$Mode;
 PUTFIELD local/lito381/server/NativePlayerLifetime.mode:Llocal/lito381/server/NativePlayerLifetime$Mode;
-ALOAD 25
+ALOAD 21
 MONITOREXIT
-GOTO L160
-ASTORE 26
-ALOAD 25
+GOTO L151
+ASTORE 22
+ALOAD 21
 MONITOREXIT
-ALOAD 26
+ALOAD 22
 ATHROW
 ALOAD 0
 INVOKESTATIC local/lito381/server/NativeDeCorpseTimers.stop(Llocal/lito381/server/NativePlayerLifetime;)V
@@ -204,6 +195,28 @@
 INVOKEDYNAMIC apply()Ljava/util/function/IntFunction; bsm=H6 java/lang/invoke/LambdaMetafactory.metafactory(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodHandle;Ljava/lang/invoke/MethodType;)Ljava/lang/invoke/CallSite; args=[type:(I)Ljava/lang/Object;, handle:H6 local/lito381/server/NativePlayerLifetime.lambda$content$1cc49100a34d1206c805b86551e739b3bc87ee7a208c1696f77a8bf39875d020(I)[Llocal/lito381/server/NativePlayerCommit$Queue;, type:(I)[Llocal/lito381/server/NativePlayerCommit$Queue;]
 INVOKEINTERFACE java/util/Set.toArray(Ljava/util/function/IntFunction;)[Ljava/lang/Object; itf
 CHECKCAST [Llocal/lito381/server/NativePlayerCommit$Queue;
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
 ASTORE 27
 ALOAD 27
 ARRAYLENGTH
@@ -212,40 +225,18 @@
 ISTORE 29
 ILOAD 29
 ILOAD 28
-IF_ICMPGE L186
+IF_ICMPGE L199
 ALOAD 27
 ILOAD 29
 AALOAD
 ASTORE 30
 ALOAD 30
-INVOKEVIRTUAL local/lito381/server/NativePlayerCommit$Queue.stop()V
+INVOKEVIRTUAL local/lito381/server/NativeNpcLifetime$Lane.stop()V
 IINC var=29 incr=1
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
 ALOAD 0
 INVOKESTATIC local/lito381/server/NativeReuseShutdown.defer(Llocal/lito381/server/NativePlayerLifetime;)Z
-IFNE L275
+IFNE L266
 ALOAD 0
 GETFIELD local/lito381/server/NativePlayerLifetime.retry:Ljava/util/concurrent/ScheduledThreadPoolExecutor;
 INVOKEVIRTUAL java/util/concurrent/ScheduledThreadPoolExecutor.shutdown()V
@@ -255,15 +246,15 @@
 LLOAD 2/3
 CONST Long:0
 LCMP
-IFLE L259
+IFLE L250
 ILOAD 6
-IFNE L259
+IFNE L250
 ALOAD 0
 GETFIELD local/lito381/server/NativePlayerLifetime.retry:Ljava/util/concurrent/ScheduledThreadPoolExecutor;
 CONST Long:2
 GETSTATIC java/util/concurrent/TimeUnit.SECONDS:Ljava/util/concurrent/TimeUnit;
 INVOKEVIRTUAL java/util/concurrent/ScheduledThreadPoolExecutor.awaitTermination(JLjava/util/concurrent/TimeUnit;)Z
-IFNE L239
+IFNE L230
 ALOAD 0
 GETFIELD local/lito381/server/NativePlayerLifetime.retry:Ljava/util/concurrent/ScheduledThreadPoolExecutor;
 INVOKEVIRTUAL java/util/concurrent/ScheduledThreadPoolExecutor.shutdownNow()Ljava/util/List;
@@ -279,7 +270,7 @@
 CONST Long:3
 GETSTATIC java/util/concurrent/TimeUnit.SECONDS:Ljava/util/concurrent/TimeUnit;
 INVOKEVIRTUAL java/util/concurrent/ThreadPoolExecutor.awaitTermination(JLjava/util/concurrent/TimeUnit;)Z
-IFNE L255
+IFNE L246
 ALOAD 0
 GETFIELD local/lito381/server/NativePlayerLifetime.workers:Ljava/util/concurrent/ThreadPoolExecutor;
 INVOKEVIRTUAL java/util/concurrent/ThreadPoolExecutor.shutdownNow()Ljava/util/List;
@@ -290,14 +281,14 @@
 GETSTATIC java/util/concurrent/TimeUnit.SECONDS:Ljava/util/concurrent/TimeUnit;
 INVOKEVIRTUAL java/util/concurrent/ThreadPoolExecutor.awaitTermination(JLjava/util/concurrent/TimeUnit;)Z
 POP
-GOTO L259
-ASTORE 35
+GOTO L250
+ASTORE 31
 CONST Integer:1
 ISTORE 6
 ALOAD 0
 GETFIELD local/lito381/server/NativePlayerLifetime.retry:Ljava/util/concurrent/ScheduledThreadPoolExecutor;
 INVOKEVIRTUAL java/util/concurrent/ScheduledThreadPoolExecutor.isTerminated()Z
-IFNE L267
+IFNE L258
 ALOAD 0
 GETFIELD local/lito381/server/NativePlayerLifetime.retry:Ljava/util/concurrent/ScheduledThreadPoolExecutor;
 INVOKEVIRTUAL java/util/concurrent/ScheduledThreadPoolExecutor.shutdownNow()Ljava/util/List;
@@ -305,28 +296,28 @@
 ALOAD 0
 GETFIELD local/lito381/server/NativePlayerLifetime.workers:Ljava/util/concurrent/ThreadPoolExecutor;
 INVOKEVIRTUAL java/util/concurrent/ThreadPoolExecutor.isTerminated()Z
-IFNE L275
+IFNE L266
 ALOAD 0
 GETFIELD local/lito381/server/NativePlayerLifetime.workers:Ljava/util/concurrent/ThreadPoolExecutor;
 INVOKEVIRTUAL java/util/concurrent/ThreadPoolExecutor.shutdownNow()Ljava/util/List;
 POP
 ALOAD 0
 INVOKEVIRTUAL local/lito381/server/NativePlayerLifetime.terminated()Z
-ISTORE 36
-ILOAD 36
-IFEQ L284
+ISTORE 32
+ILOAD 32
+IFEQ L275
 GETSTATIC local/lito381/server/NativePlayerLifetime.lives:Ljava/util/Set;
 ALOAD 0
 INVOKEINTERFACE java/util/Set.remove(Ljava/lang/Object;)Z itf
 POP
 ILOAD 6
-IFEQ L288
+IFEQ L279
 INVOKESTATIC java/lang/Thread.currentThread()Ljava/lang/Thread;
 INVOKEVIRTUAL java/lang/Thread.interrupt()V
 NEW local/lito381/server/NativePlayerLifetime$Shutdown
 DUP
 ILOAD 7
-ILOAD 36
+ILOAD 32
 ALOAD 0
 GETFIELD local/lito381/server/NativePlayerLifetime.dropped:Ljava/util/concurrent/atomic/AtomicLong;
 INVOKEVIRTUAL java/util/concurrent/atomic/AtomicLong.get()J
```

</details>

### NativePlayerVitalCompletions — DIFF；待 #203（CFG／迴圈與資料流）

<details>
<summary>全部方法診斷</summary>

```json
{
  "name": "local/lito381/server/NativePlayerVitalCompletions",
  "status": "DIFF",
  "byteSame": false,
  "allowMethodsApplied": false,
  "methodResults": [
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
  ],
  "effectsMethods": [],
  "approvedMethods": [],
  "firstDiff": {
    "where": "method",
    "member": "drain(Llocal/lito381/server/ClientSession;)V",
    "insnIndex": null,
    "detail": "left=22:[83:java/lang/Throwable  ] right=22:[85:java/lang/Throwable  ]; paths rejected: normal control-flow cycle"
  }
}
```

</details>

<details>
<summary>完整正規化差異（staged → candidate）</summary>

```diff
--- staged/NativePlayerVitalCompletions/drain-390f6c66.txt
+++ candidate/NativePlayerVitalCompletions/drain-390f6c66.txt
@@ -5,25 +5,25 @@

 ||||pvis=|pinv=|lvtvis=|lvtinv=

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
 ALOAD 0
 INVOKESTATIC local/lito381/server/NativePlayerActionScope.deferDrain(Llocal/lito381/server/ClientSession;)Z
 IFEQ L4
@@ -85,7 +85,7 @@
 ALOAD 4
 INVOKEVIRTUAL java/util/ArrayDeque.isEmpty()Z
 IFEQ L62
-GOTO L78
+GOTO L80
 ALOAD 4
 INVOKEVIRTUAL java/util/ArrayDeque.pollFirst()Ljava/lang/Object;
 CHECKCAST java/lang/Runnable
@@ -101,12 +101,14 @@
 INVOKESTATIC local/lito381/server/NativePlayerVitalCompletions.logCompletionFailure(Llocal/lito381/server/ClientSession;Ljava/lang/Throwable;)V
 GOTO L62
 ALOAD 3
-IFNULL L22
+IFNULL L79
+GOTO L80
+GOTO L22
 ALOAD 2
 GETFIELD local/lito381/server/NativePlayerVitalCompletions$State.draining:Ljava/util/concurrent/atomic/AtomicBoolean;
 CONST Integer:0
 INVOKEVIRTUAL java/util/concurrent/atomic/AtomicBoolean.set(Z)V
-GOTO L90
+GOTO L92
 ASTORE 10
 ALOAD 2
 GETFIELD local/lito381/server/NativePlayerVitalCompletions$State.draining:Ljava/util/concurrent/atomic/AtomicBoolean;
@@ -115,15 +117,15 @@
 ALOAD 10
 ATHROW
 ALOAD 3
-IFNULL L98
+IFNULL L99
 ALOAD 0
 ALOAD 3
 INVOKESTATIC local/lito381/server/NativePlayerActionScope.resumeAfterDrain(Llocal/lito381/server/ClientSession;Llocal/lito381/server/NativePlayerActionScope$Root;)Z
-IFEQ L97
-GOTO L98
+IFNE L99
 RETURN
 ALOAD 2
 GETFIELD local/lito381/server/NativePlayerVitalCompletions$State.queue:Ljava/util/concurrent/ConcurrentLinkedQueue;
 INVOKEVIRTUAL java/util/concurrent/ConcurrentLinkedQueue.isEmpty()Z
-IFEQ L10
+IFEQ L104
 RETURN
+GOTO L10
```

</details>

### NativeProcHealingEncounter$Dispatch — DIFF；待 #203（CFG／迴圈與資料流）

<details>
<summary>全部方法診斷</summary>

```json
{
  "name": "local/lito381/server/NativeProcHealingEncounter$Dispatch",
  "status": "DIFF",
  "byteSame": false,
  "allowMethodsApplied": false,
  "methodResults": [
    {
      "method": "run()V",
      "status": "DIFF",
      "detail": "method run()V: left=13:[91:java/lang/Throwable  , 99:java/lang/Throwable  ] right=13:[74:java/lang/Throwable  , 82:java/lang/Throwable  ]; paths rejected: normal control-flow cycle"
    }
  ],
  "effectsMethods": [],
  "approvedMethods": [],
  "firstDiff": {
    "where": "method",
    "member": "run()V",
    "insnIndex": null,
    "detail": "left=13:[91:java/lang/Throwable  , 99:java/lang/Throwable  ] right=13:[74:java/lang/Throwable  , 82:java/lang/Throwable  ]; paths rejected: normal control-flow cycle"
  }
}
```

</details>

<details>
<summary>完整正規化差異（staged → candidate）</summary>

```diff
--- staged/NativeProcHealingEncounter$Dispatch/run-9b75.txt
+++ candidate/NativeProcHealingEncounter$Dispatch/run-9b75.txt
@@ -5,35 +5,35 @@

 ||||pvis=|pinv=|lvtvis=|lvtinv=

-TRY 13:[91:java/lang/Throwable  , 99:java/lang/Throwable  ]
-TRY 15:[91:java/lang/Throwable  , 99:java/lang/Throwable  ]
-TRY 17:[91:java/lang/Throwable  , 99:java/lang/Throwable  ]
-TRY 25:[91:java/lang/Throwable  , 99:java/lang/Throwable  ]
-TRY 27:[91:java/lang/Throwable  , 99:java/lang/Throwable  ]
-TRY 29:[91:java/lang/Throwable  , 99:java/lang/Throwable  ]
-TRY 37:[91:java/lang/Throwable  , 99:java/lang/Throwable  ]
-TRY 39:[91:java/lang/Throwable  , 99:java/lang/Throwable  ]
-TRY 41:[91:java/lang/Throwable  , 99:java/lang/Throwable  ]
-TRY 49:[91:java/lang/Throwable  , 99:java/lang/Throwable  ]
-TRY 52:[91:java/lang/Throwable  , 99:java/lang/Throwable  ]
-TRY 55:[91:java/lang/Throwable  , 99:java/lang/Throwable  ]
-TRY 58:[91:java/lang/Throwable  , 99:java/lang/Throwable  ]
-TRY 61:[91:java/lang/Throwable  , 99:java/lang/Throwable  ]
-TRY 62:[91:java/lang/Throwable  , 99:java/lang/Throwable  ]
-TRY 64:[91:java/lang/Throwable  , 99:java/lang/Throwable  ]
-TRY 66:[91:java/lang/Throwable  , 99:java/lang/Throwable  ]
-TRY 68:[91:java/lang/Throwable  , 99:java/lang/Throwable  ]
-TRY 70:[91:java/lang/Throwable  , 99:java/lang/Throwable  ]
-TRY 73:[91:java/lang/Throwable  , 99:java/lang/Throwable  ]
-TRY 74:[91:java/lang/Throwable  , 99:java/lang/Throwable  ]
-TRY 75:[91:java/lang/Throwable  , 99:java/lang/Throwable  ]
-TRY 85:[91:java/lang/Throwable  , 99:java/lang/Throwable  ]
-TRY 93:[99:java/lang/Throwable  ]
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
 CONST Integer:0
 ISTORE 1
 ILOAD 1
 CONST Integer:256
-IF_ICMPGE L108
+IF_ICMPGE L91
 ALOAD 0
 GETFIELD local/lito381/server/NativeProcHealingEncounter$Dispatch.queue:Llocal/lito381/server/CombatNpcDamageQueue$Queue;
 INVOKEVIRTUAL local/lito381/server/CombatNpcDamageQueue$Queue.claim()Llocal/lito381/server/CombatNpcDamageQueue$Event;
@@ -47,46 +47,34 @@
 GETFIELD local/lito381/server/NativeProcHealingEncounter$Dispatch.queue:Llocal/lito381/server/CombatNpcDamageQueue$Queue;
 ALOAD 2
 INVOKESTATIC local/lito381/server/NativeSourcePoisonNpc.consume(Llocal/lito381/server/GameHub;Llocal/lito381/server/CombatNpcDamageQueue$Queue;Llocal/lito381/server/CombatNpcDamageQueue$Event;)Z
-IFEQ L24
-ALOAD 0
-GETFIELD local/lito381/server/NativeProcHealingEncounter$Dispatch.queue:Llocal/lito381/server/CombatNpcDamageQueue$Queue;
-ALOAD 2
-INVOKEVIRTUAL local/lito381/server/CombatNpcDamageQueue$Queue.complete(Llocal/lito381/server/CombatNpcDamageQueue$Event;)V
-GOTO L106
+IFEQ L20
+GOTO L69
 ALOAD 0
 GETFIELD local/lito381/server/NativeProcHealingEncounter$Dispatch.hub:Llocal/lito381/server/GameHub;
 ALOAD 0
 GETFIELD local/lito381/server/NativeProcHealingEncounter$Dispatch.queue:Llocal/lito381/server/CombatNpcDamageQueue$Queue;
 ALOAD 2
 INVOKESTATIC local/lito381/server/NativeSourcePhysicalNpc.consume(Llocal/lito381/server/GameHub;Llocal/lito381/server/CombatNpcDamageQueue$Queue;Llocal/lito381/server/CombatNpcDamageQueue$Event;)Z
-IFEQ L36
-ALOAD 0
-GETFIELD local/lito381/server/NativeProcHealingEncounter$Dispatch.queue:Llocal/lito381/server/CombatNpcDamageQueue$Queue;
-ALOAD 2
-INVOKEVIRTUAL local/lito381/server/CombatNpcDamageQueue$Queue.complete(Llocal/lito381/server/CombatNpcDamageQueue$Event;)V
-GOTO L106
+IFEQ L28
+GOTO L69
 ALOAD 0
 GETFIELD local/lito381/server/NativeProcHealingEncounter$Dispatch.hub:Llocal/lito381/server/GameHub;
 ALOAD 0
 GETFIELD local/lito381/server/NativeProcHealingEncounter$Dispatch.queue:Llocal/lito381/server/CombatNpcDamageQueue$Queue;
 ALOAD 2
 INVOKESTATIC local/lito381/server/NativeSourceCubeNpcQueue.consume(Llocal/lito381/server/GameHub;Llocal/lito381/server/CombatNpcDamageQueue$Queue;Llocal/lito381/server/CombatNpcDamageQueue$Event;)Z
-IFEQ L48
-ALOAD 0
-GETFIELD local/lito381/server/NativeProcHealingEncounter$Dispatch.queue:Llocal/lito381/server/CombatNpcDamageQueue$Queue;
-ALOAD 2
-INVOKEVIRTUAL local/lito381/server/CombatNpcDamageQueue$Queue.complete(Llocal/lito381/server/CombatNpcDamageQueue$Event;)V
-GOTO L106
+IFEQ L36
+GOTO L69
 ALOAD 2
 INVOKEVIRTUAL local/lito381/server/CombatNpcDamageQueue$Event.physical()Llocal/lito381/server/CombatWorldCommit$Plan;
 ASTORE 3
 ALOAD 3
 INSTANCEOF local/lito381/server/NativeProcHealingEncounter$Plan
-IFEQ L58
+IFEQ L46
 ALOAD 3
 CHECKCAST local/lito381/server/NativeProcHealingEncounter$Plan
 ASTORE 4
-GOTO L63
+GOTO L51
 NEW java/lang/IllegalStateException
 DUP
 CONST str:"foreign encounter Proc queue plan"
@@ -107,19 +95,14 @@
 CHECKCAST local/lito381/server/NativeProcEncounterPublication$Context
 ASTORE 5
 ALOAD 5
-IFNONNULL L84
-ALOAD 0
-GETFIELD local/lito381/server/NativeProcHealingEncounter$Dispatch.queue:Llocal/lito381/server/CombatNpcDamageQueue$Queue;
-ALOAD 2
-INVOKEVIRTUAL local/lito381/server/CombatNpcDamageQueue$Queue.complete(Llocal/lito381/server/CombatNpcDamageQueue$Event;)V
-GOTO L106
+IFNULL L69
 ALOAD 5
 INVOKESTATIC local/lito381/server/NativeProcEncounterPublication.publish(Llocal/lito381/server/NativeProcEncounterPublication$Context;)V
 ALOAD 0
 GETFIELD local/lito381/server/NativeProcHealingEncounter$Dispatch.queue:Llocal/lito381/server/CombatNpcDamageQueue$Queue;
 ALOAD 2
 INVOKEVIRTUAL local/lito381/server/CombatNpcDamageQueue$Queue.complete(Llocal/lito381/server/CombatNpcDamageQueue$Event;)V
-GOTO L106
+GOTO L89
 ASTORE 6
 ALOAD 6
 INVOKESTATIC local/lito381/server/CombatNpcDamageQueue.failed(Ljava/lang/Throwable;)V
@@ -127,7 +110,7 @@
 GETFIELD local/lito381/server/NativeProcHealingEncounter$Dispatch.queue:Llocal/lito381/server/CombatNpcDamageQueue$Queue;
 ALOAD 2
 INVOKEVIRTUAL local/lito381/server/CombatNpcDamageQueue$Queue.complete(Llocal/lito381/server/CombatNpcDamageQueue$Event;)V
-GOTO L106
+GOTO L89
 ASTORE 7
 ALOAD 0
 GETFIELD local/lito381/server/NativeProcHealingEncounter$Dispatch.queue:Llocal/lito381/server/CombatNpcDamageQueue$Queue;
```

</details>
