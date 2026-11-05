# Issue #75：A04 正式 0.6.71 暫存對齊（仍有 2 DIFF）

## 結論與已驗證

2026-11-05 本輪以合併 #135、#153、#166 後的工具重驗，結果為 **83 SAME／1 SAME_EFFECTS／2 DIFF**；25 份 staged Java 編譯成功、`@AsRecord` 轉換 1 類，產物恰為 86 個驗收 entry，缺少及多出皆為空。**尚未達到本批全部 SAME／SAME_EFFECTS，不可進 A-land。**

工作分支維持 `task/75`，未 checkout 或建立新分支。已 merge `origin/main` 的 `450178d59b0157fddf2b9456b7c03d20207993fc`，合併基準為 `346800a5162218dd97c3d217784f21e6efd2121d`，含所需 #102、#132、#135、#153、#166。以下輸入是此基準加本輪 staged 修改；來源雜湊另列，不能把基準 commit 當成未修改的驗收輸入。

## 做了什麼

- 保留上一輪 A04 的 25 份暫存。`a-batches.json` 中本批全為原始碼，沿用 src/main 為底的修正；本輪不需要重新替換為反編譯來源。既有 Target 的 private 建構子、public accessor、`@AsRecord` 與 `nativeRoleOwner` 保留。前輪已補回 Removal 身分檢查與 MonsterCurse 的 allow／resetWorld／offer。
- 將 `CombatCurseLifecycle.Removal.drain` 的送出迴圈寫成 `if (!context.valid() || !lease.target.valid(context.pc)) continue; r.send();`。仍先檢查 context，通過才檢查 target；任一失敗就跳過當次送出，兩者通過才送出一次。沒有新增求值或不可達程式碼。
- 依 [#75 主控裁定](https://github.com/chaoren0418-arch/lito-launcher-server/issues/75#issuecomment-6644525369) 與 [#62 B 類四項條件](https://github.com/chaoren0418-arch/lito-launcher-server/issues/62#issuecomment-6644529572)，只新增 `CombatCurseLifecycle$Removal#drain(Z)V` 的 effects 條目，註明 batch、category 與 PR #106。完整正規化差異見下方；副作用序列、常數集合、例外型別與數量、分支數皆一致，結果 SAME_EFFECTS。
- `BattlePolymorphCatalog.randomForm`、`CombatCurseLifecycle$Target.take` 已由 #166 的 transport 比較直接驗為 SAME，沒有替它們增加 effects 核准。
- `canOverwrite` 與 `CombatPoisonAtomic.npc/player` 的 effects 診斷仍失敗，**未將它們加入正式 effects-approved.txt**。前者不符合 B 類第 2 項，後者尚未滿足主控的條件核准；診斷清單只放 scratch。

相對最新 origin/main，變更範圍為 A04 25 份 staged Java、`docs/combat-merge/effects-approved.txt`、本報告。`src/main`、`pom.xml`、`docs/REPORT.md` 的更新只來自必要的 main 合併，對最新 origin/main 沒有本批差異；本輪未直接編輯這些檔案。

## 怎麼驗證

Windows／Microsoft OpenJDK 17.0.17；ASM 9.7 的 asm／asm-tree 由既有 Maven 快取唯讀取用，asm-analysis 下載至 worktree 的 `target/issue-75/dependency`，三者均驗 SHA-256。工具由本次 main 原始碼重新編譯，沒有改比對器。

1. 獨立編譯 AsRecord annotation 至 `target/issue-75/annotation`，不混入本批產物。
2. `javac --release 17 -encoding UTF-8 -cp <formal.jar>;<annotation>;<lib/*> -d <classes> <A04 的 25 份 staged Java>`。
3. 執行 `tools/as-record` 的 CLI，僅做核准的 record 轉換；沒有以正式 class bytes 覆蓋產物。
4. 只打包 classes，JAR entry 時間固定為 1980-01-01，便於重現雜湊。
5. 以 T020d `--left <a04.jar> --right <formal.jar> --left-classpath <formal.jar> --classes <86 entries> --effects-methods <裁切至 A04 的正式清單>` 比較。左 classpath 是實際編譯依賴，讓 #166 正確解析欄位宣告，不增加驗收 entry。其餘核准清單沒有本批適用項，不開 allow-right、layout 或 deviation。
6. 獨立比較 class 路徑集合與 verify_entries，必須完全相等；另做無 effects 對照及四方法的 effects 診斷。附錄用正式比較器逐方法輸出全部差異，避免只看 firstDiff。

| 比對 | SAME | SAME_EFFECTS | DIFF | 說明 |
| --- | ---: | ---: | ---: | --- |
| 上輪工具 | 81 | 0 | 5 | 上輪報告，歷史見 git |
| 合併 main 後、尚未修改 Java | 83 | 0 | 3 | randomForm／take 已通過 |
| 本輪最終 Java、嚴格比對 | 83 | 0 | 3 | drain 的剩餘 receiver／分支形狀差仍需核准 |
| 本輪最終 Java、正式裁切 effects 清單 | 83 | 1 | 2 | 本批正式驗收結果 |
| 四方法診斷 effects 清單 | 83 | 1 | 2 | Eye、npc、player 仍各為 DIFF |

實際最終輸出（工具皆退出 0；驗收 wrapper 因 2 DIFF 退出 2）：

```text
javac exit=0
as-record: scanned=86 converted=1
SUMMARY SAME=83 SAME_EFFECTS=1 SAME_LAYOUT=0 DEVIATION=0 DIFF=2 ONLY_LEFT=0 ONLY_RIGHT=0 BYTE_SAME=13 ALLOW_METHODS=0
classSelection: {"missingLeft": [], "extraLeft": []}
ENTRY SET expected=86 actual=86 missing=[] extra=[]
INPUT COMMIT 346800a5162218dd97c3d217784f21e6efd2121d
FORMAL SHA256 e1498871c7be6b287821c57ef5d95933db1740d9b4d4a80f6c3465f21d99d498
OUTPUT SHA256 b38e15c5bf978981a53b4f5bf729b733b0fc4bfe545d1667fcabaef4dc7a3f2b
CLASS CONTENT SHA256 aef7fbccd5ca3356e0da7096e8ca081c84534596d7761bb2240a6d3a2cdbbd80
ACCEPTANCE NOT MET
```

唯一 javac 診斷為既有 CombatMonsterCurseLifecycle unchecked/unsafe operations。CLASS CONTENT 雜湊依 entry 名排序，串接 UTF-8 名稱、NUL、class bytes。semantic-diff exit 0 只表示成功產生報告，不能當驗收通過。

已從本報告抽出六份附錄檔實際重跑：驗收 wrapper 退出 2、完整指令診斷與 effects 診斷皆退出 0；上述 JAR／class-content 雜湊及計數一致。`git diff --check` 通過。

## 未完成、推測與待驗證

| 方法 | 已驗證的阻擋 | 下一步 |
| --- | --- | --- |
| `BattleDragonEyeCatalog$Eye.canOverwrite()Z` | Java 的 `? 1 : 0` 與 `!= 0` 多出兩個 0、兩個 1 和四個分支；effects 常數集合及分支數不相同，依現有工具仍 DIFF。讀欄位、eyeField 呼叫與回傳的副作用序列相同。 | 主控處理已核准 boolean/int 表達與工具能力的落差，並確認 eyeField 回傳值域；本批無權修改工具、忽略常數或直接判 SAME_EFFECTS。 |
| `CombatPoisonAtomic.npc(..., Scope)` | effects 第 4 個元素（從 0 計）為左 ASTORE／右 leaveInstall 呼叫；完整差異還有正常／handler 區塊順序、分支 3／2、catch 數量 4／3（皆 Throwable）。 | 主控判斷需要來源重構或另案控制流程／例外保護範圍比對；現有條件核准未滿足。 |
| `CombatPoisonAtomic.player(..., Scope)` | effects 第 9 個元素為左 activatePlayer 呼叫／右 MONITOREXIT；正式 occupied 路徑有獨立退出、清理、return，staged 匯合後共用退出。分支 5／3，catch 都是 4 個 Throwable，但區間、monitor／清理／return 的靜態序列不同。 | 不能僅憑執行路徑可能等價而宣告摘要一致；需主控裁定及後續驗證。 |

推測：部分 PoisonAtomic 差異來自 javac 的回傳暫存、例外表切分與區塊排列；**本輪沒有證明這三個方法整體語意等價**。未將 PoisonAtomic 改成一般 finally 來湊 monitor 序列：正式正常路徑的 leaveInstall 本身位於外層 catch 範圍，簡單改成 finally 會改變清理拋例外時是否再清理的行為。

單元測試／回歸依任務規定不適用，未執行遊戲、登入器、伺服器或連線 DB。未修改正式資源、機密設定、封存、硬連結或原始專案目錄。未部署，未宣稱實機行為通過。

## 產出路徑與給下一棒的提示

- `lito伺服器/LiTo381-Server/merge-inputs/formal-0.6.71/staged/local/lito381/server/`：A04 25 類；本輪只改 CombatCurseLifecycle.java。
- `docs/combat-merge/effects-approved.txt`：新增 drain 的精確核准。
- `docs/reports/issue-75.md`：本報告、完整差異與可重現腳本。
- 未提交生成檔：`scratch/issue-75/` 的清單／報告／診斷；`lito伺服器/LiTo381-Server/target/issue-75/` 的 class／JAR／ASM 依賴。ignored 產物不加入 git。

交主控 Claude（needs:claude）處理上述兩類剩餘差異。請勿用全綠 CI 取代 staged 86-entry 驗收，也不要在同一工具／來源上重複派工後宣稱已完成。**A-land 仍須補主控指定測試**：leaveInstall 丟出例外時，狀態 monitor 已釋放，原例外照樣往外拋；正式版與移植版須行為一致。這項要求不因本批暫存驗收而取消。

## 完整正規化差異

以下為全部 86 類的 class／field／method metadata、try 表與完整指令逐項比較產生的所有不同 hunk，無截斷。`-` 是 staged，`+` 是正式。這個診斷輸出的是 ClassComparer 基礎正規化指令；randomForm／take 額外由正式工具的 TransportComparison 證明為 SAME，所以仍會在此看到其堆疊表達差異。drain 僅有送出迴圈的 receiver／等價條件跳躍差；其餘完整方法（含 try 表）相同。

```diff
--- staged/BattleDragonEyeCatalog$Eye.txt
+++ formal/BattleDragonEyeCatalog$Eye.txt
@@ -253,21 +253,9 @@
 annotationDefault
 ALOAD 0
 GETFIELD local/lito381/server/BattleDragonEyeCatalog$Eye.canOverwrite:Z
-IFEQ L0
-CONST Integer:1
-GOTO L1
-LABEL L0
-CONST Integer:0
-LABEL L1
 ALOAD 0
 CONST str:"canOverwrite"
 INVOKESTATIC local/lito381/server/CombatSpecialBuffs.eyeField(ILlocal/lito381/server/BattleDragonEyeCatalog$Eye;Ljava/lang/String;)I
-IFEQ L2
-CONST Integer:1
-GOTO L3
-LABEL L2
-CONST Integer:0
-LABEL L3
 IRETURN
 END METHOD
 METHOD castGfx()I

--- staged/BattlePolymorphCatalog.txt
+++ formal/BattlePolymorphCatalog.txt
@@ -457,12 +457,11 @@
 annotationDefault
 ILOAD 0
 INVOKESTATIC local/lito381/server/NativePolymorphSource.selectOrNull(I)Llocal/lito381/server/BattlePolymorphCatalog$Form;
-ASTORE 1
-ALOAD 1
+DUP
 IFNULL L0
-ALOAD 1
-ARETURN
-LABEL L0
+ARETURN
+LABEL L0
+POP
 ILOAD 0
 INVOKESTATIC local/lito381/server/BattlePolymorphCatalog.nativePackagedRandomForm(I)Llocal/lito381/server/BattlePolymorphCatalog$Form;
 ARETURN

--- staged/CombatCurseLifecycle$Removal.txt
+++ formal/CombatCurseLifecycle$Removal.txt
@@ -535,10 +535,11 @@
 INVOKEINTERFACE java/util/Iterator.next()Ljava/lang/Object; itf
 CHECKCAST local/lito381/server/CombatCurseLifecycle$Release
 ASTORE 21
+ALOAD 21
 ALOAD 0
 GETFIELD local/lito381/server/CombatCurseLifecycle$Removal.context:Llocal/lito381/server/CombatCurseLifecycle$Context;
 INVOKEVIRTUAL local/lito381/server/CombatCurseLifecycle$Context.valid()Z
-IFEQ L61
+IFEQ L62
 ALOAD 0
 GETFIELD local/lito381/server/CombatCurseLifecycle$Removal.lease:Llocal/lito381/server/CombatCurseLifecycle$Curse;
 GETFIELD local/lito381/server/CombatCurseLifecycle$Curse.target:Llocal/lito381/server/CombatCurseLifecycle$Target;
@@ -546,11 +547,11 @@
 GETFIELD local/lito381/server/CombatCurseLifecycle$Removal.context:Llocal/lito381/server/CombatCurseLifecycle$Context;
 GETFIELD local/lito381/server/CombatCurseLifecycle$Context.pc:Llocal/lito381/server/ClientSession;
 INVOKEVIRTUAL local/lito381/server/CombatCurseLifecycle$Target.valid(Llocal/lito381/server/ClientSession;)Z
-IFNE L62
-GOTO L61
+IFEQ L62
+INVOKEVIRTUAL local/lito381/server/CombatCurseLifecycle$Release.send()V
+GOTO L63
 LABEL L62
-ALOAD 21
-INVOKEVIRTUAL local/lito381/server/CombatCurseLifecycle$Release.send()V
+POP
 LABEL L63
 GOTO L61
 LABEL L64

--- staged/CombatCurseLifecycle$Target.txt
+++ formal/CombatCurseLifecycle$Target.txt
@@ -220,12 +220,10 @@
 CONST str:"classicWorldInstanceId"
 INVOKESTATIC local/lito381/server/CombatCurseLifecycle.number(Ljava/lang/Object;Ljava/lang/String;)J
 INVOKESPECIAL local/lito381/server/CombatCurseLifecycle$Target.<init>(JLjava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/lang/Object;JJJ)V
-ASTORE 1
-ALOAD 1
+DUP
 ALOAD 0
 INVOKESTATIC local/lito381/server/NativePlayerCommit.owner(Llocal/lito381/server/ClientSession;)Ljava/lang/Object;
 PUTFIELD local/lito381/server/CombatCurseLifecycle$Target.nativeRoleOwner:Ljava/lang/Object;
-ALOAD 1
 ARETURN
 END METHOD
 METHOD toString()Ljava/lang/String;

--- staged/CombatPoisonAtomic.txt
+++ formal/CombatPoisonAtomic.txt
@@ -263,17 +263,16 @@
 exceptions
 annotations ||||pvis=|pinv=|lvtvis=|lvtinv=
 annotationDefault
-TRY L4 L6 L7 java/lang/Throwable
-TRY L8 L9 L7 java/lang/Throwable
-TRY L0 L1 L12 java/lang/Throwable
-TRY L3 L11 L12 java/lang/Throwable
+TRY L2 L4 L5 java/lang/Throwable
+TRY L6 L7 L5 java/lang/Throwable
+TRY L0 L8 L8 java/lang/Throwable
 ALOAD 0
 LLOAD 1/2
 INVOKESTATIC local/lito381/server/NativeCombatOrigin.enterNpc(Llocal/lito381/server/ClientSession;J)Llocal/lito381/server/NativeCombatOrigin$Install;
 ASTORE 10
 LABEL L0
 INVOKESTATIC local/lito381/server/CombatRules.loaded()Z
-IFNE L2
+IFNE L1
 ALOAD 0
 LLOAD 1/2
 ILOAD 3
@@ -282,18 +281,14 @@
 LLOAD 6/7
 ALOAD 8
 INVOKESTATIC local/lito381/server/NativeCombatOrigin.npcState(Llocal/lito381/server/ClientSession;JIIIJLjava/lang/String;)Llocal/lito381/server/CombatPoisonAtomic$NpcState;
+ALOAD 10
+INVOKESTATIC local/lito381/server/NativeCombatOrigin.leaveInstall(Llocal/lito381/server/NativeCombatOrigin$Install;)V
+ARETURN
+LABEL L1
+ALOAD 0
+INVOKESTATIC local/lito381/server/CombatPoisonAtomic.status(Llocal/lito381/server/ClientSession;)Llocal/lito381/server/BattleTimedStatus;
+DUP
 ASTORE 11
-ALOAD 10
-INVOKESTATIC local/lito381/server/NativeCombatOrigin.leaveInstall(Llocal/lito381/server/NativeCombatOrigin$Install;)V
-LABEL L1
-ALOAD 11
-ARETURN
-LABEL L2
-ALOAD 0
-LABEL L3
-INVOKESTATIC local/lito381/server/CombatPoisonAtomic.status(Llocal/lito381/server/ClientSession;)Llocal/lito381/server/BattleTimedStatus;
-DUP
-ASTORE 12
 MONITORENTER
 ALOAD 0
 LLOAD 1/2
@@ -302,12 +297,12 @@
 ILOAD 5
 LLOAD 6/7
 ALOAD 8
-LABEL L4
+LABEL L2
 INVOKESTATIC local/lito381/server/NativeCombatOrigin.npcState(Llocal/lito381/server/ClientSession;JIIIJLjava/lang/String;)Llocal/lito381/server/CombatPoisonAtomic$NpcState;
-ASTORE 13
-ALOAD 13
+ASTORE 12
+ALOAD 12
 INVOKEVIRTUAL local/lito381/server/CombatPoisonAtomic$NpcState.applied()Z
-IFEQ L5
+IFEQ L3
 ALOAD 0
 CONST str:"poisonAttacker"
 CONST null
@@ -316,30 +311,27 @@
 ALOAD 9
 LLOAD 6/7
 INVOKESTATIC local/lito381/server/CombatPoisonLifecycle.installed(Llocal/lito381/server/ClientSession;Llocal/lito381/server/CombatPoisonLifecycle$Scope;J)V
+LABEL L3
+ALOAD 12
+ALOAD 11
+MONITOREXIT
+LABEL L4
+ALOAD 10
+INVOKESTATIC local/lito381/server/NativeCombatOrigin.leaveInstall(Llocal/lito381/server/NativeCombatOrigin$Install;)V
+ARETURN
 LABEL L5
-ALOAD 12
-MONITOREXIT
+ASTORE 13
+ALOAD 11
 LABEL L6
-GOTO L10
+MONITOREXIT
 LABEL L7
+ALOAD 13
+ATHROW
+LABEL L8
 ASTORE 14
-ALOAD 12
-LABEL L8
-MONITOREXIT
-LABEL L9
+ALOAD 10
+INVOKESTATIC local/lito381/server/NativeCombatOrigin.leaveInstall(Llocal/lito381/server/NativeCombatOrigin$Install;)V
 ALOAD 14
-ATHROW
-LABEL L10
-ALOAD 10
-INVOKESTATIC local/lito381/server/NativeCombatOrigin.leaveInstall(Llocal/lito381/server/NativeCombatOrigin$Install;)V
-LABEL L11
-ALOAD 13
-ARETURN
-LABEL L12
-ASTORE 15
-ALOAD 10
-INVOKESTATIC local/lito381/server/NativeCombatOrigin.leaveInstall(Llocal/lito381/server/NativeCombatOrigin$Install;)V
-ALOAD 15
 ATHROW
 END METHOD
 METHOD object(Llocal/lito381/server/ClientSession;J)V
@@ -453,52 +445,53 @@
 exceptions
 annotations ||||pvis=|pinv=|lvtvis=|lvtinv=
 annotationDefault
-TRY L4 L7 L8 java/lang/Throwable
+TRY L2 L3 L8 java/lang/Throwable
+TRY L5 L7 L8 java/lang/Throwable
 TRY L9 L10 L8 java/lang/Throwable
-TRY L0 L1 L13 java/lang/Throwable
-TRY L3 L12 L13 java/lang/Throwable
+TRY L0 L11 L11 java/lang/Throwable
 ALOAD 5
 ALOAD 6
 INVOKESTATIC local/lito381/server/NativeCombatOrigin.enterPlayer(Llocal/lito381/server/ClientSession;Llocal/lito381/server/ClientSession;)Llocal/lito381/server/NativeCombatOrigin$Install;
 ASTORE 8
 LABEL L0
 INVOKESTATIC local/lito381/server/CombatRules.loaded()Z
-IFNE L2
+IFNE L1
 ALOAD 0
 LLOAD 1/2
 ILOAD 3
 ILOAD 4
 ALOAD 5
 INVOKESTATIC local/lito381/server/NativeCombatOrigin.activatePlayer(Llocal/lito381/server/BattleTimedStatus;JIILlocal/lito381/server/ClientSession;)Z
-ISTORE 9
-ALOAD 8
-INVOKESTATIC local/lito381/server/NativeCombatOrigin.leaveInstall(Llocal/lito381/server/NativeCombatOrigin$Install;)V
-LABEL L1
-ILOAD 9
+ALOAD 8
+INVOKESTATIC local/lito381/server/NativeCombatOrigin.leaveInstall(Llocal/lito381/server/NativeCombatOrigin$Install;)V
 IRETURN
-LABEL L2
-ALOAD 0
+LABEL L1
+ALOAD 0
+DUP
+ASTORE 9
+MONITORENTER
+ALOAD 5
+LLOAD 1/2
+LABEL L2
+INVOKESTATIC local/lito381/server/CombatPoisonAtomic.occupied(Llocal/lito381/server/ClientSession;J)Z
+IFEQ L4
+CONST Integer:0
+ALOAD 9
+MONITOREXIT
 LABEL L3
-DUP
-ASTORE 10
-MONITORENTER
-ALOAD 5
-LLOAD 1/2
+ALOAD 8
+INVOKESTATIC local/lito381/server/NativeCombatOrigin.leaveInstall(Llocal/lito381/server/NativeCombatOrigin$Install;)V
+IRETURN
 LABEL L4
-INVOKESTATIC local/lito381/server/CombatPoisonAtomic.occupied(Llocal/lito381/server/ClientSession;J)Z
-IFEQ L5
-CONST Integer:0
-ISTORE 11
-GOTO L6
+ALOAD 0
+LLOAD 1/2
+ILOAD 3
+ILOAD 4
+ALOAD 5
 LABEL L5
-ALOAD 0
-LLOAD 1/2
-ILOAD 3
-ILOAD 4
-ALOAD 5
 INVOKESTATIC local/lito381/server/NativeCombatOrigin.activatePlayer(Llocal/lito381/server/BattleTimedStatus;JIILlocal/lito381/server/ClientSession;)Z
-ISTORE 11
-ILOAD 11
+ISTORE 10
+ILOAD 10
 IFEQ L6
 ALOAD 5
 INVOKESTATIC local/lito381/server/CombatPoisonAtomic.clear(Llocal/lito381/server/ClientSession;)V
@@ -511,29 +504,26 @@
 LLOAD 1/2
 INVOKESTATIC local/lito381/server/CombatPoisonLifecycle.installed(Llocal/lito381/server/ClientSession;Llocal/lito381/server/CombatPoisonLifecycle$Scope;J)V
 LABEL L6
-ALOAD 10
+ILOAD 10
+ALOAD 9
 MONITOREXIT
 LABEL L7
-GOTO L11
+ALOAD 8
+INVOKESTATIC local/lito381/server/NativeCombatOrigin.leaveInstall(Llocal/lito381/server/NativeCombatOrigin$Install;)V
+IRETURN
 LABEL L8
+ASTORE 11
+ALOAD 9
+LABEL L9
+MONITOREXIT
+LABEL L10
+ALOAD 11
+ATHROW
+LABEL L11
 ASTORE 12
-ALOAD 10
-LABEL L9
-MONITOREXIT
-LABEL L10
+ALOAD 8
+INVOKESTATIC local/lito381/server/NativeCombatOrigin.leaveInstall(Llocal/lito381/server/NativeCombatOrigin$Install;)V
 ALOAD 12
-ATHROW
-LABEL L11
-ALOAD 8
-INVOKESTATIC local/lito381/server/NativeCombatOrigin.leaveInstall(Llocal/lito381/server/NativeCombatOrigin$Install;)V
-LABEL L12
-ILOAD 11
-IRETURN
-LABEL L13
-ASTORE 13
-ALOAD 8
-INVOKESTATIC local/lito381/server/NativeCombatOrigin.leaveInstall(Llocal/lito381/server/NativeCombatOrigin$Install;)V
-ALOAD 13
 ATHROW
 END METHOD
 METHOD poll(Llocal/lito381/server/BattleTimedStatus;JILlocal/lito381/server/ClientSession;)Llocal/lito381/server/BattleTimedStatus$PoisonPoll;
```

## 完整 effects 摘要差異

以下四方法採工具相同的 beforeWebs、tempStores、effectsSequence、constants、catchTypes 與 branches；drain 完全一致，因此沒有該方法的 hunk。這是診斷，不是新增核准。

```diff
--- staged/effects
+++ formal/effects
@@ -1,6 +1,6 @@
 METHOD local/lito381/server/BattleDragonEyeCatalog$Eye#canOverwrite()Z
-CONSTANTS {Integer:0=2, Integer:1=2, str:"canOverwrite"=1}
-BRANCHES 4
+CONSTANTS {str:"canOverwrite"=1}
+BRANCHES 0
 CATCHES []
 GETFIELD local/lito381/server/BattleDragonEyeCatalog$Eye.canOverwrite
 INVOKESTATIC local/lito381/server/CombatSpecialBuffs.eyeField(ILlocal/lito381/server/BattleDragonEyeCatalog$Eye;Ljava/lang/String;)I
@@ -207,13 +207,12 @@
 END METHOD
 METHOD local/lito381/server/CombatPoisonAtomic#npc(Llocal/lito381/server/ClientSession;JIIIJLjava/lang/String;Llocal/lito381/server/CombatPoisonLifecycle$Scope;)Llocal/lito381/server/CombatPoisonAtomic$NpcState;
 CONSTANTS {null=1, str:"poisonAttacker"=1}
-BRANCHES 3
-CATCHES [java/lang/Throwable  , java/lang/Throwable  , java/lang/Throwable  , java/lang/Throwable  ]
+BRANCHES 2
+CATCHES [java/lang/Throwable  , java/lang/Throwable  , java/lang/Throwable  ]
 INVOKESTATIC local/lito381/server/NativeCombatOrigin.enterNpc(Llocal/lito381/server/ClientSession;J)Llocal/lito381/server/NativeCombatOrigin$Install;
 ASTORE
 INVOKESTATIC local/lito381/server/CombatRules.loaded()Z
 INVOKESTATIC local/lito381/server/NativeCombatOrigin.npcState(Llocal/lito381/server/ClientSession;JIIIJLjava/lang/String;)Llocal/lito381/server/CombatPoisonAtomic$NpcState;
-ASTORE
 INVOKESTATIC local/lito381/server/NativeCombatOrigin.leaveInstall(Llocal/lito381/server/NativeCombatOrigin$Install;)V
 ARETURN
 INVOKESTATIC local/lito381/server/CombatPoisonAtomic.status(Llocal/lito381/server/ClientSession;)Llocal/lito381/server/BattleTimedStatus;
@@ -225,18 +224,17 @@
 INVOKESTATIC local/lito381/server/CombatAccess.set(Ljava/lang/Object;Ljava/lang/String;Ljava/lang/Object;)V
 INVOKESTATIC local/lito381/server/CombatPoisonLifecycle.installed(Llocal/lito381/server/ClientSession;Llocal/lito381/server/CombatPoisonLifecycle$Scope;J)V
 MONITOREXIT
-ASTORE
-MONITOREXIT
-ATHROW
 INVOKESTATIC local/lito381/server/NativeCombatOrigin.leaveInstall(Llocal/lito381/server/NativeCombatOrigin$Install;)V
 ARETURN
 ASTORE
+MONITOREXIT
+ATHROW
 INVOKESTATIC local/lito381/server/NativeCombatOrigin.leaveInstall(Llocal/lito381/server/NativeCombatOrigin$Install;)V
 ATHROW
 END METHOD
 METHOD local/lito381/server/CombatPoisonAtomic#player(Llocal/lito381/server/BattleTimedStatus;JIILlocal/lito381/server/ClientSession;Llocal/lito381/server/ClientSession;Llocal/lito381/server/CombatPoisonLifecycle$Scope;)Z
 CONSTANTS {Integer:0=1, str:"poisonAttacker"=1}
-BRANCHES 5
+BRANCHES 3
 CATCHES [java/lang/Throwable  , java/lang/Throwable  , java/lang/Throwable  , java/lang/Throwable  ]
 INVOKESTATIC local/lito381/server/NativeCombatOrigin.enterPlayer(Llocal/lito381/server/ClientSession;Llocal/lito381/server/ClientSession;)Llocal/lito381/server/NativeCombatOrigin$Install;
 ASTORE
@@ -247,17 +245,19 @@
 ASTORE
 MONITORENTER
 INVOKESTATIC local/lito381/server/CombatPoisonAtomic.occupied(Llocal/lito381/server/ClientSession;J)Z
+MONITOREXIT
+INVOKESTATIC local/lito381/server/NativeCombatOrigin.leaveInstall(Llocal/lito381/server/NativeCombatOrigin$Install;)V
+IRETURN
 INVOKESTATIC local/lito381/server/NativeCombatOrigin.activatePlayer(Llocal/lito381/server/BattleTimedStatus;JIILlocal/lito381/server/ClientSession;)Z
 INVOKESTATIC local/lito381/server/CombatPoisonAtomic.clear(Llocal/lito381/server/ClientSession;)V
 INVOKESTATIC local/lito381/server/CombatAccess.set(Ljava/lang/Object;Ljava/lang/String;Ljava/lang/Object;)V
 INVOKESTATIC local/lito381/server/CombatPoisonLifecycle.installed(Llocal/lito381/server/ClientSession;Llocal/lito381/server/CombatPoisonLifecycle$Scope;J)V
 MONITOREXIT
-ASTORE
-MONITOREXIT
-ATHROW
-INVOKESTATIC local/lito381/server/NativeCombatOrigin.leaveInstall(Llocal/lito381/server/NativeCombatOrigin$Install;)V
-IRETURN
-ASTORE
-INVOKESTATIC local/lito381/server/NativeCombatOrigin.leaveInstall(Llocal/lito381/server/NativeCombatOrigin$Install;)V
-ATHROW
-END METHOD
+INVOKESTATIC local/lito381/server/NativeCombatOrigin.leaveInstall(Llocal/lito381/server/NativeCombatOrigin$Install;)V
+IRETURN
+ASTORE
+MONITOREXIT
+ATHROW
+INVOKESTATIC local/lito381/server/NativeCombatOrigin.leaveInstall(Llocal/lito381/server/NativeCombatOrigin$Install;)V
+ATHROW
+END METHOD
```

## 本次來源 SHA-256

| 輸入 | SHA-256 |
| --- | --- |
| BattleAnimationCatalog.java | `1c348553a0ca7c2d7fd5fcc48c44d04f23cbbf04be9db66204ee1d899300ba8c` |
| BattleArmBreakerStatus.java | `6b51a037b0001ee185fa6cf139e9b18b7eae229b05c01f872f4d53d09c3725dd` |
| BattleArmorBreak.java | `8cc1158d6681072db0207ca2505b5e206bdef1a95236b1873332595a23a59297` |
| BattleClearMindStatus.java | `aa99549aa2e80a11e274a0d2162262e51127375eb175705ebcb90453d2777c17` |
| BattleDragonEyeCatalog.java | `8763d6a02a9b634254ca7bfe02824828856ac74a3672e13481ca2248aaa9698b` |
| BattleEarthBlessStatus.java | `2a203a17bd456b77bb170c47d15d68433eb6d88b0d7a83ccc25e730c908385b8` |
| BattleFreezeEffects.java | `ece006e9ebeaec12b89bda8af45bbeb9bac6627d792364305e7197e763c2cddb` |
| BattleInvisibilityStatus.java | `f09fabc5e974124101a51a30f4b5404f85552f48460a60208f79f78c6a11d149` |
| BattleIronSkinStatus.java | `820199a8b723abfbcccd5df84acec6b04bf63f16fdeb57d215a3bb4a8348407c` |
| BattleJoyOfPain.java | `fce4531a4ec4e9246b53ea14eb955a0dd382f0a6dcbe4ef23d668408a6a3a1a9` |
| BattleMortalBody.java | `b57810c00babc8fc0db35c595c15d2442d82044cb01d0c76aaefb2fc2f260593` |
| BattlePatienceStatus.java | `678fc5d7309bb93afbbdfcb2e40a1ec40218bae767224479d365aa86206c5b29` |
| BattlePetRaceCourseCatalog.java | `2643d6d4c152a5743a52726c1d288ff1aa530c22e6e13fb4fb514c4d1e2e178d` |
| BattlePhysicalDefense.java | `94793b280dbf610e5e3900ff40d3b3af5cdfdf26d4894fa23721c0675bcf77ca` |
| BattlePhysicalProcBuff.java | `7e1719bb3fa630674130e5d41077cc3e47b0b2aacb8e375ba751db938792928f` |
| BattlePolymorphCatalog.java | `62945bad014fde12493de91790e0032ed6f3a0b1ea4550bff21588e981489365` |
| BattleRules.java | `18653d9e823f18e62b9163feead143f59ab31156c24a2dbd17fee27c4a666c97` |
| BattleShockStunEffects.java | `8842131374ce50889b18a1c030cf9b6a8a9d456df645a32f7eecf15f8216abd6` |
| BattleStrikerGale.java | `9b7dd6becf943a252f086a86739a395a4c837f6465faf6bcca9f8670bb26f0f9` |
| BattleTurnUndead.java | `b5faf3e19ef4e84d90b3f5bc152b397a22667eb7eef313a2ae2a2cc6344d0f8b` |
| BattleWeaponBreak.java | `468f05fdca64e1f56c8ad7471135dfc4c3620ebff3e6670f141d002724b3fcec` |
| CombatCurseLifecycle.java | `5f42dbae2764204a0e76b1049416517e44425e785836d1bf99aa2a205e592413` |
| CombatMonsterCurseLifecycle.java | `6a6a84a1f300a4dd1afd9de670ae99c2e5af56f08bc54fce0d92bf6e50ed94dc` |
| CombatNpcDamageQueue.java | `97ef4e2d278462a50c1f2a16c2a46e496c7ebf66cd30d51a9e92510da68b8ea2` |
| CombatPoisonAtomic.java | `f9d64610e0aaaa6ebd60352e93cfbec70269d67bf029fbe5023d119bef270237` |
| docs/combat-merge/a-batches.json | `ac1c17ec1a446564c6fb912235ecd9222d06bef1a3a91cdcb3836fda158ad65d` |
| docs/combat-merge/effects-approved.txt | `b48431e80e508f795e2dd9c26c316a335e2149f82bbb4269326f132e85953e58` |

## 可重現步驟

從 worktree 根目錄執行下列 PowerShell。全部附錄檔會取出到 scratch，工具與 staged 重新編譯；不依賴本機前輪生成檔。JDK／Python 路徑可改成同版本位置；verify.py 內 JAVA_HOME 亦須對應。`verify.py --effects` 本次預期回傳 2（NOT MET），不要把它改成成功。

```powershell
$taskText = Get-Content -Raw -Encoding UTF8 docs/reports/issue-75.md
$taskOut = Join-Path (Get-Location) 'scratch/issue-75'
New-Item -ItemType Directory -Force $taskOut | Out-Null
$taskUtf8 = New-Object System.Text.UTF8Encoding($false)
foreach ($taskName in @('verify.py','DumpNormalized.java','dump.py','DumpEffects.java','effects-diagnostic.py','effects-candidate.txt')) {
    $taskPattern = '(?s)<!-- FILE: ' + [regex]::Escape($taskName) + ' -->\s*```[^\r\n]*\r?\n(.*?)\r?\n```'
    $taskMatch = [regex]::Match($taskText, $taskPattern)
    if (-not $taskMatch.Success) { throw "找不到附錄 $taskName" }
    [IO.File]::WriteAllText((Join-Path $taskOut $taskName), $taskMatch.Groups[1].Value + "`n", $taskUtf8)
}
& 'D:/dev-orchestrator/tools/python/python.exe' -B -X utf8 scratch/issue-75/verify.py --effects
if ($LASTEXITCODE -ne 2) { throw '與本次 NOT MET 診斷不符，請檢查輸出' }
& 'D:/dev-orchestrator/tools/python/python.exe' -B -X utf8 scratch/issue-75/dump.py
if ($LASTEXITCODE -ne 0) { throw '正規化診斷失敗' }
& 'D:/dev-orchestrator/tools/python/python.exe' -B -X utf8 scratch/issue-75/effects-diagnostic.py
if ($LASTEXITCODE -ne 0) { throw 'effects 診斷失敗' }
```

verify.py 只清除已確認位於此 worktree 的 `target/issue-75/classes` 中可重建 class。診斷程式只讀取 JAR，不改寫驗收產物或正式比較器。

## 附錄：verify.py

<!-- FILE: verify.py -->
```python
from pathlib import Path
import json, subprocess, os, sys, zipfile, hashlib, urllib.request
ROOT=Path.cwd(); SERVER=ROOT/'lito伺服器/LiTo381-Server'; WORK=ROOT/'scratch/issue-75'; OUT=SERVER/'target/issue-75'; OUT.mkdir(parents=True,exist_ok=True)
os.environ['JAVA_HOME']='C:/Program Files/Microsoft/jdk-17.0.17.10-hotspot'
os.environ['AS_RECORD_MAVEN_REPO']='D:/dev-cache/maven/repository'
os.environ['TEMP']=os.environ['TMP']=str(OUT)
sys.dont_write_bytecode=True
sys.path.insert(0,str(SERVER/'tools/as-record'))
import as_record as tool
BATCH=next(b for b in json.loads((ROOT/'docs/combat-merge/a-batches.json').read_text('utf-8'))['batches'] if b['id']=='A04')
STAGED=SERVER/'merge-inputs/formal-0.6.71/staged/local/lito381/server'
def run(args):
 p=subprocess.run([str(a) for a in args],stdout=subprocess.PIPE,stderr=subprocess.STDOUT)
 print(p.stdout.decode('utf-8',errors='replace')); p.check_returncode(); return p.stdout
if __name__=='__main__':
 STAGED.mkdir(parents=True,exist_ok=True)
 for name in BATCH['classes']:
  dest=STAGED/(name+'.java')
  if not dest.exists(): raise RuntimeError('Missing staged source: '+str(dest))
 analysis=OUT/'dependency/asm-analysis-9.7.jar'
 analysis.parent.mkdir(parents=True,exist_ok=True)
 if not analysis.exists(): analysis.write_bytes(urllib.request.urlopen('https://repo.maven.apache.org/maven2/org/ow2/asm/asm-analysis/9.7/asm-analysis-9.7.jar',timeout=60).read())
 assert hashlib.sha256(analysis.read_bytes()).hexdigest()=='7bc6bcbc21379948a0c8c467fb0f864206e5b818f6bc0b546872f5c9f941556f'
 deps=tool.dependencies()+os.pathsep+str(analysis)
 tool.compile_java([SERVER/'src/main/java/local/lito381/server/AsRecord.java'],OUT/'annotation','')
 tool.compile_java(sorted((SERVER/'tools/semantic-diff/src/main/java').rglob('*.java')),OUT/'diff-tool',deps)
 (WORK/'entries.txt').write_text('\n'.join(BATCH['verify_entries'])+'\n','utf-8')
 classes=OUT/'classes'; classes.mkdir(exist_ok=True)
 # Remove only generated class files in this exact worktree output directory.
 assert classes.resolve().is_relative_to(ROOT.resolve())
 for p in classes.rglob('*.class'): p.unlink()
 log=run([tool.jdk('javac'),'-J-Dfile.encoding=UTF-8','--release','17','-encoding','UTF-8','-cp',str(SERVER/'reference/base-0.6.71.jar')+';'+str(SERVER/'lib/*')+';'+str(OUT/'annotation'),'-d',classes,*[STAGED/(n+'.java') for n in BATCH['classes']]])
 (WORK/'javac.log').write_bytes(log)
 log=run([tool.jdk('java'),'-Dfile.encoding=UTF-8','-cp',tool.build(),'local.lito381.asrecord.AsRecord',classes]); (WORK/'as-record.log').write_bytes(log)
 jar=OUT/'a04.jar'
 with zipfile.ZipFile(jar,'w') as z:
  for p in sorted(classes.rglob('*.class')):
   z.writestr(zipfile.ZipInfo(p.relative_to(classes).as_posix(),(1980,1,1,0,0,0)),p.read_bytes())
 flags=[]
 if '--effects' in sys.argv:
  owners={e.removesuffix('.class') for e in BATCH['verify_entries']}
  approved=[line for line in (ROOT/'docs/combat-merge/effects-approved.txt').read_text('utf-8-sig').splitlines() if line.split('#',1)[0].strip() in owners]
  assert approved, 'No A04 effects approvals'
  (WORK/'effects.txt').write_text('\n'.join(approved)+'\n','utf-8')
  flags=['--effects-methods',WORK/'effects.txt']
 log=run([tool.jdk('java'),'-Dfile.encoding=UTF-8','-cp',str(OUT/'diff-tool')+';'+deps,'local.lito381.semanticdiff.SemanticDiff','--left',jar,'--right',SERVER/'reference/base-0.6.71.jar','--left-classpath',SERVER/'reference/base-0.6.71.jar','--classes',WORK/'entries.txt',*flags,'--json',WORK/'diff.json','--md',WORK/'diff.md']); (WORK/'semantic.log').write_bytes(log)
 data=json.loads((WORK/'diff.json').read_text('utf-8'))
 print(json.dumps(data['counts'])); print(json.dumps(data['classSelection']))
 for c in data['classes']:
  if c['status']!='SAME': print(json.dumps(c,ensure_ascii=False))

 actual={p.relative_to(classes).as_posix() for p in classes.rglob('*.class')}
 expected=set(BATCH['verify_entries'])
 assert actual==expected, {'missing':sorted(expected-actual),'extra':sorted(actual-expected)}
 print('ENTRY SET expected='+str(len(expected))+' actual='+str(len(actual))+' missing=[] extra=[]')
 print('INPUT COMMIT '+subprocess.check_output(['git','rev-parse','HEAD'],text=True).strip())
 for label,path in [('FORMAL',SERVER/'reference/base-0.6.71.jar'),('OUTPUT',jar)]:
  print(label+' SHA256 '+hashlib.sha256(path.read_bytes()).hexdigest())
 content=hashlib.sha256()
 for entry in sorted(expected):
  content.update(entry.encode('utf-8')+b'\0'+(classes/entry).read_bytes())
 print('CLASS CONTENT SHA256 '+content.hexdigest())
 passed=data['counts']['SAME']+data['counts']['SAME_EFFECTS']==86 and not any(data['counts'][k] for k in ['DIFF','ONLY_LEFT','ONLY_RIGHT','SAME_LAYOUT','DEVIATION'])
 print('ACCEPTANCE '+('PASS' if passed else 'NOT MET'))
 sys.exit(0 if passed else 2)
```

## 附錄：DumpNormalized.java

<!-- FILE: DumpNormalized.java -->
```java
package local.lito381.semanticdiff;
import java.nio.file.*;
import java.util.*;
import java.lang.reflect.*;
import org.objectweb.asm.tree.*;
public class DumpNormalized {
 static Object call(String name, Object... args) throws Exception {
  for(Method m:ClassComparer.class.getDeclaredMethods()) if(m.getName().equals(name)&&m.getParameterCount()==args.length){m.setAccessible(true);return m.invoke(null,args);}
  throw new IllegalArgumentException(name);
 }
 static void line(List<String> o,String k,Object v){o.add(k+" "+v);}
 public static void main(String[] args) throws Exception {
  JarIndex jar=JarIndex.load(Path.of(args[0]));jar.addClasspath(Path.of(args[3]));Path dest=Path.of(args[2]);Files.createDirectories(dest);
  for(String name:Files.readAllLines(Path.of(args[1]))){name=name.replace(".class","").trim();if(name.isEmpty())continue;ClassNode c=jar.readTree(name);if(c==null)continue;
   List<String> o=new ArrayList<>();
   line(o,"CLASS",name);line(o,"access",c.access);line(o,"super",c.superName);line(o,"signature",c.signature);line(o,"interfaces",call("joinSorted",c.interfaces));line(o,"outerClass",c.outerClass);line(o,"outerMethod",c.outerMethod+" "+c.outerMethodDesc);line(o,"nestHost",c.nestHostClass);line(o,"nestMembers",call("joinSorted",c.nestMembers));line(o,"permittedSubclasses",call("joinSorted",c.permittedSubclasses));line(o,"annotations",call("annotations",c));
   for(String s:call("innerClasses",c,false).toString().split("; "))line(o,"innerClass",s);
   line(o,"recordComponents",call("records",c,jar));
   Map<String,FieldNode> fields=(Map<String,FieldNode>)call("fieldMap",c,jar);
   for(var e:fields.entrySet()){FieldNode f=e.getValue();line(o,"FIELD",e.getKey());line(o,"access",call("memberAccess",f.access));line(o,"signature",f.signature);line(o,"value",call("value",f.value));line(o,"annotations",call("fieldAnns",f));}
   Map<String,MethodNode> methods=(Map<String,MethodNode>)call("methodMap",c,jar);
   for(var e:methods.entrySet()){MethodNode m=e.getValue();line(o,"METHOD",e.getKey());line(o,"access",call("memberAccess",m.access));line(o,"signature",m.signature);line(o,"exceptions",call("joinSorted",m.exceptions));line(o,"annotations",call("methodAnns",m,call("labelIds",m),jar));line(o,"annotationDefault",call("defaultValue",m.annotationDefault,jar));o.addAll((List<String>)call("tryCatch",m,jar));o.addAll(ClassComparer.instructions(m,jar));o.add("END METHOD");}
   Files.write(dest.resolve(name.substring(name.lastIndexOf('/')+1)+".txt"),o);
  }
 }
}
```

## 附錄：dump.py

<!-- FILE: dump.py -->
```python
import sys,difflib
sys.path.insert(0,'scratch/issue-75')
from verify import *
p=WORK/'DumpNormalized.java';p.write_text(p.read_text('utf-8-sig'),'utf-8')
cp=str(OUT/'diff-tool')+';'+tool.dependencies()+';'+str(OUT/'dependency/asm-analysis-9.7.jar')
tool.compile_java([p],OUT/'diff-tool',cp)
for side,jar in [('left',OUT/'a04.jar'),('right',SERVER/'reference/base-0.6.71.jar')]:
 run([tool.jdk('java'),'-cp',cp,'local.lito381.semanticdiff.DumpNormalized',jar,WORK/'entries.txt',WORK/side,SERVER/'reference/base-0.6.71.jar'])
result=[]
for p in sorted((WORK/'right').glob('*.txt')):
 a=(WORK/'left'/p.name).read_text('utf-8').splitlines(True);b=p.read_text('utf-8').splitlines(True)
 diff=''.join(difflib.unified_diff(a,b,fromfile='staged/'+p.name,tofile='formal/'+p.name))
 if diff:result.append(diff)
(WORK/'full.diff').write_text('\n'.join(result),'utf-8');print('\n'.join(result))
```

## 附錄：DumpEffects.java

<!-- FILE: DumpEffects.java -->
```java
package local.lito381.semanticdiff;
import java.nio.file.*;
import java.util.*;
import org.objectweb.asm.tree.*;

/** Read-only diagnostics using the production comparator's exact effects summary. */
public class DumpEffects {
 @SuppressWarnings("unchecked")
 public static void main(String[] args) throws Exception {
  JarIndex left=JarIndex.load(Path.of(args[0])),right=JarIndex.load(Path.of(args[1]));
  left.addClasspath(Path.of(args[1]));
  List<String> lo=new ArrayList<>(),ro=new ArrayList<>();
  for(String entry:Files.readAllLines(Path.of(args[2]))){
   if(entry.isBlank()||entry.startsWith("#"))continue;
   int sep=entry.indexOf('#');String owner=entry.substring(0,sep),key=entry.substring(sep+1).split(" #",2)[0];
   MethodNode lm=left.readTree(owner).methods.stream().filter(m->(m.name+m.desc).equals(key)).findFirst().orElseThrow();
   MethodNode rm=right.readTree(owner).methods.stream().filter(m->(m.name+m.desc).equals(key)).findFirst().orElseThrow();
   lm=left.beforeWebs.getOrDefault(lm,lm);rm=right.beforeWebs.getOrDefault(rm,rm);
   for(int side=0;side<2;side++){
    MethodNode m=side==0?lm:rm,other=side==0?rm:lm;JarIndex jar=side==0?left:right;List<String> out=side==0?lo:ro;
    out.add("METHOD "+entry);
    out.add("CONSTANTS "+DumpNormalized.call("constants",m,jar));
    out.add("BRANCHES "+DumpNormalized.call("branches",m));
    out.add("CATCHES "+DumpNormalized.call("catchTypes",m,jar));
    out.addAll((List<String>)DumpNormalized.call("effectsSequence",m,jar,DumpNormalized.call("tempStores",m,other)));
    out.add("END METHOD");
   }
  }
  Files.write(Path.of(args[3]),lo);Files.write(Path.of(args[4]),ro);
 }
}
```

## 附錄：effects-diagnostic.py

<!-- FILE: effects-diagnostic.py -->
```python
import difflib, sys
sys.path.insert(0,'scratch/issue-75')
from verify import *
cp=str(OUT/'diff-tool')+os.pathsep+tool.dependencies()+os.pathsep+str(OUT/'dependency/asm-analysis-9.7.jar')
args=[tool.jdk('java'),'-Dfile.encoding=UTF-8','-cp',cp,'local.lito381.semanticdiff.SemanticDiff','--left',OUT/'a04.jar','--right',SERVER/'reference/base-0.6.71.jar','--left-classpath',SERVER/'reference/base-0.6.71.jar','--classes',WORK/'entries.txt','--effects-methods',WORK/'effects-candidate.txt','--json',WORK/'effects-diagnostic.json','--md',WORK/'effects-diagnostic.md']
run(args)
tool.compile_java([WORK/'DumpNormalized.java',WORK/'DumpEffects.java'],OUT/'diff-tool',cp)
run([tool.jdk('java'),'-Dfile.encoding=UTF-8','-cp',cp,'local.lito381.semanticdiff.DumpEffects',OUT/'a04.jar',SERVER/'reference/base-0.6.71.jar',WORK/'effects-candidate.txt',WORK/'effects-left.txt',WORK/'effects-right.txt'])
a=(WORK/'effects-left.txt').read_text('utf-8').splitlines(True)
b=(WORK/'effects-right.txt').read_text('utf-8').splitlines(True)
diff=''.join(difflib.unified_diff(a,b,fromfile='staged/effects',tofile='formal/effects'))
(WORK/'effects-full.diff').write_text(diff,'utf-8')
print(diff)
```

## 附錄：effects-candidate.txt

<!-- FILE: effects-candidate.txt -->
```text
local/lito381/server/BattleDragonEyeCatalog$Eye#canOverwrite()Z
local/lito381/server/CombatCurseLifecycle$Removal#drain(Z)V
local/lito381/server/CombatPoisonAtomic#npc(Llocal/lito381/server/ClientSession;JIIIJLjava/lang/String;Llocal/lito381/server/CombatPoisonLifecycle$Scope;)Llocal/lito381/server/CombatPoisonAtomic$NpcState;
local/lito381/server/CombatPoisonAtomic#player(Llocal/lito381/server/BattleTimedStatus;JIILlocal/lito381/server/ClientSession;Llocal/lito381/server/ClientSession;Llocal/lito381/server/CombatPoisonLifecycle$Scope;)Z
```
