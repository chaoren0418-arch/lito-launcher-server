# Issue #69 — T020-B08 戰鬥併入暫存

## 本輪結論（2026-11-06，Asia/Taipei）

**依最新主控裁定登記 `Batch#emit` 的 method-diff，沿用現有 JAR 重跑正式 semantic-diff；160 個 entry 全數符合核准狀態，可交審查及 B-land 接手。**

正式結果：**141 SAME、10 SAME_PATHS、7 SAME_CFG、1 LAMBDA_PAIRED_BY_CALLSITE、1 DEVIATION**，即 159 個原可接受項加 1 個核准偏差。DIFF=0、missing=0、extra=0，類別及方法級 ONLY_LEFT／ONLY_RIGHT 均為 0。唯一 DEVIATION 是 `NativeLightAction$Batch#emit()V`，不是 SAME，也不代表執行期回歸已完成。

[主控裁定](https://github.com/chaoren0418-arch/lito-launcher-server/issues/69#issuecomment-6672200596)要求不改 Java、不改工具、沿用現有 JAR，只追加此精確方法的核准。這項較晚裁定已擴大文件範圍至 `deviations.txt`、`deviations.md`；本輪直接完成，不再等待範圍確認，也不另開工具任務。

## 已驗證：做了什麼

- 沿用 `task/69`，本輪起點 `4955c7b8580fdebf7e12c35447bda38b215a87a7`，沒有 checkout／建分支。fetch 後 `origin/main` 仍為 `8df7c89c3a9b4dbdb8eb01b40a5b53462dbac0f8`；`git merge origin/main` 回覆 `Already up to date.`。先前合併提交 `47e6d398ad659f97148a0e5c99128bcb2da6abd5` 已包含 #254／PR #260。
- 只在 `docs/combat-merge/deviations.txt` 追加指定一條，於 `deviations.md` 記錄三點理由、兩側例外表、診斷路徑，並更新本報告。PR 累計範圍為 30 份 staged Java 加這三份文件，共 33 檔。
- 對前輪 audit 逐檔核對全部 30 份 Java 的 SHA-256 均相同，現有 `b08.jar` SHA-256 也一致；本輪沒有重編 Java、執行轉換器或修改工具。前輪已完成 30 份編譯、AsRecord `scanned=160 converted=6`，原始命令與退出碼仍在 `scratch/issue-69/final-254/audit.json`。
- 使用原 semantic-diff 重驗全部 160 entries，核准檔皆套用；重新執行 javap，確認核心 try 仍只保護 `[0,4)` 的 originalEmit 呼叫。沒有將 `finish(null)` 移入核心 try。
- 收尾核對：160 項報告表格逐項等於正式 JSON，SHA 與兩側例外表一致；`git diff --check` 通過。本輪恰改三份文件，PR 累計恰為核准的 33 檔，收據見 `scratch/issue-69/closeout/verification.log`。

### 前輪已完成並保留的來源修正

- `commitPair` 在原有 locked 守門之後，先將 binding、target、receipt 複製到 lambda 內部別名，再引用 task；捕獲順序變為 `Ingress, ClientSession, Binding, ClientSession, Receipt, Task`，與候選一致。只讀區域參照，沒有提前執行 getter、守門、鎖或任務。
- 同一 lambda 的 multi-catch 對齊為 `RuntimeException | Error`。兩型別互不隸屬、共用同一 handler，副作用不變；原先反向排序會造成工具保留的有序例外邊不同。本體以 423 條路徑證明後，成功按呼叫點配對。
- `deferScoped`／`deferWeaponTargetOutput` 明確共用候選既有名稱 `lambda$deferWeaponTargetOutput$0` 的 private static 本體。Java lambda 只轉呼叫，既有正規化可還原為同一 handle；沒有新增核准。其邏輯依實際 bootstrap 目標，只檢查 accepted binding 後執行 task。候選舊 `lambda$deferScoped$10` 含 target 檢查，但已無引用，正式工具列 ORPHAN_IGNORED，沒有將舊邏輯帶回。
- 共用 defer 本體與候選差異只剩候選多餘的 `GOTO`；正式工具以 6 條路徑證明後按呼叫點配對。外層 `NativeLightAction` 整類為 LAMBDA_PAIRED_BY_CALLSITE。
- `Batch.emit` 完整比對後確認既有核心 try 涵蓋已一致，因此保留執行邏輯並加註解標出範圍。沒有把正常 `finish(null)` 納入該 catch，避免 finish 失敗時再執行一次 finish。此次以主控核准的精確 method-diff 登記，正式結果為 DEVIATION。
- `NativePlayerCommit`、`NativeProcHealingEncounter` 依裁定未改 Java。#254 已使 drain、acceptedTail、Dispatch.run 為 SAME_CFG；nativeManaTransferRecover 不再列方法差異，按線性／既有正規化通過。
- 前輪 30 份來源相對 `5434695a` 逐檔比對，只有 `NativeLightAction.java` 改動；其他 29 份完全一致。保留六個 `@AsRecord`、治療巢狀 monitor／vitalLock finally、package-private Output、核准轉拋 helper 及 Exceptions 條目。`NativeProcHealingPlayer` 與 Output 仍為 SAME。
- 前輪 `final-254/b08.jar` 相對更早產物僅 NativeLightAction、Batch、Ingress 三個 class bytes 變更（含來源行號變動）；其餘 157 個相同。沒有以容器雜湊代替 class 比對。

## 怎麼驗證

Microsoft OpenJDK 17.0.17，ASM／asm-tree／asm-analysis 9.7。semantic-diff 來源最後提交為 `45bbbe4cfd4f21b0080caa2726ac2928d0b8cc1c`；沿用 `target/issue-69/final-254/tools` 的既有編譯工具。工具 class 的逐檔 SHA-256、完整 argv 與退出碼記於 `scratch/issue-69/closeout/audit.json`。本輪不宣稱重新建置；前輪工具建置輸出仍在 `scratch/issue-69/final-254/tools.log`。

以下以伺服器目錄為基準，`<W>` 為本 worktree 的 `scratch/issue-69/closeout`，`<T>` 為 `target/issue-69/final-254`；`<ASM>` 為三個 ASM 9.7 JAR 以分號串接。entries 從 `docs/combat-merge/batches.json` 的 B08.verify_entries 產生。

```text
java -Dfile.encoding=UTF-8 -Xmx2g -cp "<T>/tools;<ASM>" local.lito381.semanticdiff.SemanticDiff
  --left <T>/b08.jar --left-classpath reference/candidate-50635f7e.jar
  --right reference/candidate-50635f7e.jar --classes <W>/entries.txt
  --access-widen <W>/access-widen-approved.txt
  --extra-methods <W>/extra-methods-approved.txt
  --exceptions-exempt <W>/exceptions-exempt.txt
  --deviations <W>/deviations-b08.txt
  --json <W>/semantic.json --md <W>/semantic.md
SUMMARY SAME=141 SAME_EFFECTS=0 SAME_PATHS=10 ORPHAN_IGNORED=0 SAME_CFG=7 LAMBDA_PAIRED_BY_CALLSITE=1 VISIBILITY_BRIDGE=0 SAME_LAYOUT=0 DEVIATION=1 DIFF=0 ONLY_LEFT=0 ONLY_RIGHT=0 BYTE_SAME=1 ALLOW_METHODS=0
# 退出碼 0，另以 JSON 斷言每個 class 為核准狀態，沒有僅憑退出碼判定通過
ZIP entries=160; expected=160; missing=0; extra=0
classSelection.missingLeft=[]; classSelection.extraLeft=[]
```

完整原始輸出為 `scratch/issue-69/closeout/semantic.log`；JSON／Markdown 同目錄。方法級另有 17 SAME_CFG、2 LAMBDA_PAIRED_BY_CALLSITE，不是額外 class 數。

### 核准清單與 B08 範圍抽取

原 access-widen、extra-methods、exceptions-exempt 三份清單逐位元複製自 `scratch/issue-69/final-254/`，本輪新增且只新增一個 method-diff。無 effects、layout、metadata 或 allow-methods 豁免。

**不能直接將共用 `deviations.txt` 全檔傳入**，其他批次 owner 會觸發 `class outside comparison scope`。`scratch/issue-69/closeout.py` 由 B08.verify_entries 去除 `.class` 建立精確 owner 集合；逐行以 `local/...#` 擷取方法 owner，僅保留 owner 在該集合內的原行，再斷言恰為指定一條。此法也支援共用清單中類型前置的行，不按模糊名稱前綴選取。輸出 `scratch/issue-69/closeout/deviations-b08.txt` 的完整內容：

```text
local/lito381/server/NativeLightAction$Batch#emit()V  method-diff  # approved: https://github.com/chaoren0418-arch/lito-launcher-server/issues/69#issuecomment-6672200596
```

`access-widen-approved.txt`：

```text
local/lito381/server/NativeLightAction$Output # approved: https://github.com/chaoren0418-arch/lito-launcher-server/issues/64#issuecomment-6654963452
```

`extra-methods-approved.txt`：

```text
local/lito381/server/NativeLightAction$Batch#rethrowCandidateFailure(Ljava/lang/Throwable;)V # approved: https://github.com/chaoren0418-arch/lito-launcher-server/issues/69#issuecomment-6666508373
```

`exceptions-exempt.txt`：

```text
local/lito381/server/LegacyMySqlRepository#healthCheck()V # approved: https://github.com/chaoren0418-arch/lito-launcher-server/issues/69#issuecomment-6666508373
local/lito381/server/LegacyMySqlRepository#persistTournamentProfile(JLjava/lang/String;Llocal/lito381/server/RepositoryTournamentProfile;Llocal/lito381/server/GameStateStore$TournamentProfileState;)Llocal/lito381/server/RepositoryTournamentMutation; # approved: https://github.com/chaoren0418-arch/lito-launcher-server/issues/69#issuecomment-6666508373
local/lito381/server/LegacyMySqlRepository#tournamentProfiles()Llocal/lito381/server/RepositoryTournamentRoster; # approved: https://github.com/chaoren0418-arch/lito-launcher-server/issues/69#issuecomment-6666508373
```

Exceptions 三條只豁免 throws 屬性，仍比對方法本體；前輪已記錄移除 throws 會產生三個 unreported exception 編譯錯誤，本輪沒有重做該 probe。LegacyMySqlRepository 仍為 SAME_PATHS。


### 輸入、產物與本輪輸出 SHA-256

- `lito伺服器/LiTo381-Server/target/issue-69/final-254/b08.jar`：`fc7697767d2da777f8e6bfd21bf8297701da1a2698f2795367f724c23859066f`
- `lito伺服器/LiTo381-Server/reference/candidate-50635f7e.jar`：`50635f7edca8ec6ce77787cc32371c7590b497e48ea202f8a679caf3f517b729`
- `lito伺服器/LiTo381-Server/reference/base-0.6.71.jar`：`e1498871c7be6b287821c57ef5d95933db1740d9b4d4a80f6c3465f21d99d498`
- `scratch/issue-69/closeout/semantic.json`：`e2308c19e6e66ff0fda224c268f145b1142a4cc132a4f6bcb020f3de82cce189`
- `scratch/issue-69/closeout/semantic.log`：`c6a59bd0ca032228c4e3897840e24c20c096f348215ab0c2e6f6bd9d66ab1be0`
- `scratch/issue-69/closeout/deviations-b08.txt`：`d09667f45a440b32d1493ea5c3eea4c6692808a8f272c90b8f732c3ea901243d`

- `NativeLightAction.java`：`8561ad87c710bb8ce8faccbf634f221e8dad128a498bcfee3654802a1ed60ca8`；其餘 29 份也與前輪 audit 相同。

## 核准偏差與完整證據（已驗證）

正式 `Batch` class 與 `emit()V` 均為 DEVIATION；`nativeSourceBatchOriginalEmit()V` 仍為 SAME_CFG。完整方法結果（含 LEFT／RIGHT 正規化內容）在 `scratch/issue-69/closeout/batch-result.json` 及 `semantic.json`。以下保留同一 JAR 的 javap 與完整 unified diff，說明核准內容，不將其抹成 SAME。

### javap 完整 emit 與例外表

命令：`javap -p -c -s -classpath <staged 或 candidate JAR> 'local.lito381.server.NativeLightAction$Batch'`。本輪重新執行 javap 核對既有 JAR，例外表與前輪相同，未改寫 bytecode。原始輸出為 `scratch/issue-69/closeout/staged-Batch-javap.log` 與 `candidate-Batch-javap.log`。

**staged**：

```text
  private void emit();
    descriptor: ()V
    Code:
       0: aload_0
       1: invokevirtual #144                // Method nativeSourceBatchOriginalEmit:()V
       4: goto          23
       7: astore_1
       8: aload_0
       9: aload_1
      10: invokestatic  #147                // Method local/lito381/server/NativeSourceTargetIconBatch.finish:(Ljava/lang/Object;Ljava/lang/Throwable;)V
      13: goto          21
      16: astore_2
      17: aload_2
      18: invokestatic  #111                // Method rethrowCandidateFailure:(Ljava/lang/Throwable;)V
      21: aload_1
      22: athrow
      23: aload_0
      24: aconst_null
      25: invokestatic  #147                // Method local/lito381/server/NativeSourceTargetIconBatch.finish:(Ljava/lang/Object;Ljava/lang/Throwable;)V
      28: goto          36
      31: astore_1
      32: aload_1
      33: invokestatic  #111                // Method rethrowCandidateFailure:(Ljava/lang/Throwable;)V
      36: return
    Exception table:
       from    to  target type
           0     4     7   Class java/lang/Throwable
           8    13    16   Class java/lang/Throwable
          23    28    31   Class java/lang/Throwable
```

**candidate**：

```text
  private void emit();
    descriptor: ()V
    Code:
       0: aload_0
       1: invokespecial #191                // Method nativeSourceBatchOriginalEmit:()V
       4: aload_0
       5: aconst_null
       6: invokestatic  #194                // Method local/lito381/server/NativeSourceTargetIconBatch.finish:(Ljava/lang/Object;Ljava/lang/Throwable;)V
       9: return
      10: astore_1
      11: aload_0
      12: aload_1
      13: invokestatic  #194                // Method local/lito381/server/NativeSourceTargetIconBatch.finish:(Ljava/lang/Object;Ljava/lang/Throwable;)V
      16: aload_1
      17: athrow
    Exception table:
       from    to  target type
           0     4    10   Class java/lang/Throwable
```


核心 Throwable handler 的 from/to 兩側都是 0/4；target staged=7、candidate=10 是區塊排列差異。staged 的額外 `[8,13)` 與 `[23,28)` 只保護兩個 finish 呼叫，handler 呼叫核准的原樣轉拋 helper。candidate 的 finish 宣告 `throws Throwable`，因此 Java 需要此編譯期橋接。

### 完整正規化 unified diff

dump 使用 JarIndex 與 ClassComparer 的 methodIdentity／tryCatch／instructions；此單側 dump 尚未套用兩側的核准 helper 消除，不可覆蓋正式結果。

```diff
--- staged/emit
+++ candidate/emit
@@ -1,31 +1,20 @@
 emit()V
 ()V
 0x2


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
```

### helper 消除為何沒有生效

`RethrowHandlers.normalize` 先辨識核准原樣轉拋 handler，再以實體順序逐一對齊可能拋例外的 events。兩個 helper handler 都通過既有 body 證明；但移除後的左側順序是 `originalEmit, finish(error), ATHROW, finish(null)`，候選為 `originalEmit, finish(null), finish(error), ATHROW`，在第三項拒絕，因此整個 normalize 回傳原方法。

scratch 的 EmitProbe254 直接呼叫既有 handler body 檢查，僅在記憶體複本移除其核准的兩個 handler，再交既有完整方法比較器，得到 SAME_PATHS（4）。它沒有修改 Java、JAR、工具來源或核准清單，**這是定位證據，不能作為本批正式通過收據**。前輪完整診斷輸出保留於 `scratch/issue-69/final-254/emit-probe.log`；本輪未重做記憶體修改診斷，正式結果來自帶核准清單的原工具：

```text
official normalization returns original=true
handler 5 approved identity rethrow=false
handler 13 approved identity rethrow=true
handler 24 approved identity rethrow=true
diagnostic left events=[INVOKESPECIAL local/lito381/server/NativeLightAction$Batch.nativeSourceBatchOriginalEmit()V, INVOKESTATIC local/lito381/server/NativeSourceTargetIconBatch.finish(Ljava/lang/Object;Ljava/lang/Throwable;)V, ATHROW, INVOKESTATIC local/lito381/server/NativeSourceTargetIconBatch.finish(Ljava/lang/Object;Ljava/lang/Throwable;)V]
candidate right events=[INVOKESPECIAL local/lito381/server/NativeLightAction$Batch.nativeSourceBatchOriginalEmit()V, INVOKESTATIC local/lito381/server/NativeSourceTargetIconBatch.finish(Ljava/lang/Object;Ljava/lang/Throwable;)V, INVOKESTATIC local/lito381/server/NativeSourceTargetIconBatch.finish(Ljava/lang/Object;Ljava/lang/Throwable;)V, ATHROW]
after approved handler removal=equivalent
diagnostic results={local/lito381/server/NativeLightAction$Batch={emit()V=MethodResult[status=SAME_PATHS, detail=path multisets equal (4)]}}
```


## 推測

前輪診斷及主控獨立審查支持差異來自 helper 消除的實體事件排列限制。正式驗收本輪依精確 method-diff 核准通過，並未實作更一般的 CFG／handler 等價證明，不把此核准延伸至其他方法或候選 JAR。

## 待驗證／給下一棒的提示

1. **Claude 審查者**：本輪已完成指定收尾，可依 33 檔累計範圍、正式 159＋1 DEVIATION 收據審查 PR #94；主控已裁定，不再等待 #254 或新增工具任務。
2. **B-land #138 整合者**：保留 helper／Exceptions 核准、六個 AsRecord、治療巢狀 monitor／vitalLock finally 與 package-private Output；正式驗證須帶入本次精確 emit method-diff。不得將 `finish(null)` 放進核心 try，否則 finish 失敗可能再執行一次 finish。
3. **B-land #138**：手寫 `lambda$deferWeaponTargetOutput$0` 併入 `src/main` 時，確認不會與 javac 自動產生的 lambda 編號撞名。單元測試、整合／遊戲回歸仍由 B-land 統一處理，本輪未執行；實機驗收依專案規則由使用者執行。
4. 本輪未部署、未執行 staged 應用程式、未啟停正式程序或連 DB，未讀取機密設定／封存／releases。Git 僅必要更新 worktree 共用 `.git`，原專案工作檔未寫入。`src/main`、`pom.xml`、`docs/REPORT.md` 無本輪修改。

## 產出路徑

- 本輪 Git 交付：`docs/combat-merge/deviations.txt`、`docs/combat-merge/deviations.md`、`docs/reports/issue-69.md`。
- 正式重驗腳本：`scratch/issue-69/closeout.py`；本輪核准清單、完整 argv／退出碼／SHA 收據、semantic JSON／Markdown／log、Batch 方法結果與 javap：`scratch/issue-69/closeout/`。
- 保留前輪編譯與診斷證據：`scratch/issue-69/final-254/`，其中 `emit-probe.log` 為核准引用的原診斷。
- 沿用建置產物：`lito伺服器/LiTo381-Server/target/issue-69/final-254/b08.jar`，不入 git。
- [PR #94](https://github.com/chaoren0418-arch/lito-launcher-server/pull/94)。

## 全部 160 個 entry 正式結果

<details>
<summary>完整 class 清單（省略 local/lito381/server/ 前綴與 .class）</summary>

| entry | 狀態 |
|---|---|
| `ClassicWorldMonsterPopulation` | SAME_CFG |
| `ClassicWorldMonsterPopulation$1` | SAME |
| `ClassicWorldMonsterPopulation$1Candidate` | SAME |
| `ClassicWorldMonsterPopulation$1Triggered` | SAME |
| `ClassicWorldMonsterPopulation$1TriggeredMagic` | SAME |
| `ClassicWorldMonsterPopulation$Actor` | SAME |
| `ClassicWorldMonsterPopulation$AirItemLanding` | SAME |
| `ClassicWorldMonsterPopulation$Attack` | SAME |
| `ClassicWorldMonsterPopulation$Capture` | SAME |
| `ClassicWorldMonsterPopulation$Cell` | SAME |
| `ClassicWorldMonsterPopulation$CkewSkillAttack` | SAME |
| `ClassicWorldMonsterPopulation$Contribution` | SAME |
| `ClassicWorldMonsterPopulation$ContributorKey` | SAME |
| `ClassicWorldMonsterPopulation$ContributorKind` | SAME |
| `ClassicWorldMonsterPopulation$DigestRemoval` | SAME |
| `ClassicWorldMonsterPopulation$DoppelTransformation` | SAME |
| `ClassicWorldMonsterPopulation$GiveAdmission` | SAME |
| `ClassicWorldMonsterPopulation$GridPoint` | SAME |
| `ClassicWorldMonsterPopulation$GuardHomecoming` | SAME |
| `ClassicWorldMonsterPopulation$Healing` | SAME |
| `ClassicWorldMonsterPopulation$Hit` | SAME |
| `ClassicWorldMonsterPopulation$HomeReset` | SAME |
| `ClassicWorldMonsterPopulation$IdleMovement` | SAME |
| `ClassicWorldMonsterPopulation$ItemAction` | SAME |
| `ClassicWorldMonsterPopulation$ItemActionKind` | SAME |
| `ClassicWorldMonsterPopulation$ItemPickup` | SAME |
| `ClassicWorldMonsterPopulation$ItemUse` | SAME |
| `ClassicWorldMonsterPopulation$ItemUseKind` | SAME |
| `ClassicWorldMonsterPopulation$MagicAttack` | SAME |
| `ClassicWorldMonsterPopulation$MagicCancellation` | SAME |
| `ClassicWorldMonsterPopulation$ManaDrain` | SAME |
| `ClassicWorldMonsterPopulation$MonsterItemReceipt` | SAME |
| `ClassicWorldMonsterPopulation$MonsterTameCommit` | SAME |
| `ClassicWorldMonsterPopulation$Movement` | SAME |
| `ClassicWorldMonsterPopulation$NearTeleport` | SAME |
| `ClassicWorldMonsterPopulation$NearTeleportKind` | SAME |
| `ClassicWorldMonsterPopulation$NpcPoison` | SAME |
| `ClassicWorldMonsterPopulation$ParalysisUpdate` | SAME |
| `ClassicWorldMonsterPopulation$PhysicalSkillAttack` | SAME |
| `ClassicWorldMonsterPopulation$PoisonUpdate` | SAME |
| `ClassicWorldMonsterPopulation$Refinement` | SAME |
| `ClassicWorldMonsterPopulation$Respawn` | SAME |
| `ClassicWorldMonsterPopulation$Resurrection` | SAME |
| `ClassicWorldMonsterPopulation$Reveal` | SAME |
| `ClassicWorldMonsterPopulation$Snapshot` | SAME_PATHS |
| `ClassicWorldMonsterPopulation$StatusSkillAttack` | SAME |
| `ClassicWorldMonsterPopulation$TamedItemReceipt` | SAME |
| `ClassicWorldMonsterPopulation$TamedRelease` | SAME |
| `LegacyInventoryWeightLoader` | SAME |
| `LegacyMySqlRepository` | SAME_PATHS |
| `LegacyMySqlRepository$1` | SAME |
| `LegacyMySqlRepository$CastleWarRewardTarget` | SAME |
| `LegacyMySqlRepository$CharacterClanRow` | SAME |
| `LegacyMySqlRepository$HotelSqlLease` | SAME |
| `LegacyMySqlRepository$HouseSettlementClan` | SAME |
| `LegacyMySqlRepository$HouseSettlementRecord` | SAME |
| `LegacyMySqlRepository$HouseTaxOwner` | SAME |
| `LegacyMySqlRepository$MarriageCharacterRow` | SAME |
| `LegacyMySqlRepository$PositionedWarehouseItem` | SAME |
| `LegacyMySqlRepository$SqlBookmark` | SAME |
| `LegacyMySqlRepository$SqlBuddyMetadata` | SAME |
| `LegacyMySqlRepository$SqlCastleSecuritySettlement` | SAME |
| `LegacyMySqlRepository$SqlStoredItem` | SAME |
| `LegacyMySqlRepository$StoredBoardTopic` | SAME |
| `NativeAcceptedHpCompletion` | SAME |
| `NativeArmorProc` | SAME |
| `NativeArmorProc$Catalog` | SAME |
| `NativeArmorProc$Rule` | SAME |
| `NativeArmorState` | SAME_PATHS |
| `NativeBattleVisibility` | SAME |
| `NativeCancellationBattleRuntime` | SAME |
| `NativeCancellationBattleRuntime$Lease` | SAME |
| `NativeCancellationBattleRuntime$Notice` | SAME |
| `NativeCancellationBattleRuntime$State` | SAME |
| `NativeCancellationBattleRuntime$Viewer` | SAME |
| `NativeCancellationNpcStatic` | SAME_CFG |
| `NativeCancellationNpcStatic$Appearance` | SAME |
| `NativeCancellationNpcStatic$State` | SAME |
| `NativeCancellationNpcStatic$Target` | SAME |
| `NativeCastMaterial` | SAME |
| `NativeCastMaterial$Cast` | SAME |
| `NativeCastMaterial$Effect` | SAME |
| `NativeCastMaterial$ItemView` | SAME |
| `NativeCombatAttacker` | SAME_PATHS |
| `NativeCombatAttacker$Feedback` | SAME |
| `NativeCombatAttacker$Ingress` | SAME |
| `NativeCombatAttacker$Live` | SAME |
| `NativeCombatAttacker$Ref` | SAME |
| `NativeCombatAttacker$Reflection` | SAME |
| `NativeCombatOrigin` | SAME_PATHS |
| `NativeCombatOrigin$Clock` | SAME |
| `NativeCombatOrigin$Install` | SAME |
| `NativeCombatOrigin$Life` | SAME |
| `NativeCombatOrigin$Poison` | SAME |
| `NativeCombatOrigin$Tick` | SAME |
| `NativeDeItemDefinitionsSql` | SAME |
| `NativeDeItemDefinitionsSql$Counts` | SAME |
| `NativeDeItemDefinitionsSql$Lease` | SAME |
| `NativeDeItemDefinitionsSql$ResultLease` | SAME |
| `NativeDeItemDefinitionsSql$StatementLease` | SAME |
| `NativeEquipmentDefense` | SAME_CFG |
| `NativeEquipmentDefense$Effect` | SAME |
| `NativeEquipmentDefense$Resolved` | SAME |
| `NativeLightAction` | LAMBDA_PAIRED_BY_CALLSITE |
| `NativeLightAction$Batch` | DEVIATION |
| `NativeLightAction$Completion` | SAME |
| `NativeLightAction$Ingress` | SAME |
| `NativeLightAction$Output` | SAME |
| `NativeMassTeleportAnswerLife` | SAME_PATHS |
| `NativeMassTeleportAnswerLife$Lease` | SAME |
| `NativeNpcLifetime` | SAME |
| `NativeNpcLifetime$Dispatch` | SAME |
| `NativeNpcLifetime$Lane` | SAME |
| `NativeNpcLifetime$Retry` | SAME |
| `NativePlayerActionScope` | SAME_PATHS |
| `NativePlayerActionScope$Gated` | SAME |
| `NativePlayerActionScope$Root` | SAME |
| `NativePlayerActionScope$Ticket` | SAME |
| `NativePlayerCommit` | SAME_CFG |
| `NativePlayerCommit$Binding` | SAME_PATHS |
| `NativePlayerCommit$CalcTicket` | SAME |
| `NativePlayerCommit$DefenseVisual` | SAME |
| `NativePlayerCommit$Event` | SAME |
| `NativePlayerCommit$Frame` | SAME |
| `NativePlayerCommit$Queue` | SAME_PATHS |
| `NativePlayerCommit$WorldPlan` | SAME |
| `NativePlayerLifetime` | SAME_CFG |
| `NativePlayerLifetime$Dispatch` | SAME |
| `NativePlayerLifetime$Mode` | SAME |
| `NativePlayerLifetime$Registration` | SAME |
| `NativePlayerLifetime$Shutdown` | SAME |
| `NativePlayerLifetime$Snapshot` | SAME |
| `NativePlayerVitalCompletions` | SAME_CFG |
| `NativePlayerVitalCompletions$Receipt` | SAME_PATHS |
| `NativePlayerVitalCompletions$State` | SAME |
| `NativeProcHealingCompanion` | SAME |
| `NativeProcHealingCompanion$Dispatch` | SAME |
| `NativeProcHealingCompanion$Live` | SAME |
| `NativeProcHealingCompanion$Meter` | SAME |
| `NativeProcHealingCompanion$Plan` | SAME |
| `NativeProcHealingCompanion$Receipt` | SAME |
| `NativeProcHealingCompanion$Target` | SAME |
| `NativeProcHealingEncounter` | SAME |
| `NativeProcHealingEncounter$Dispatch` | SAME_CFG |
| `NativeProcHealingEncounter$Live` | SAME |
| `NativeProcHealingEncounter$Plan` | SAME |
| `NativeProcHealingEncounter$Target` | SAME |
| `NativeProcHealingPlayer` | SAME |
| `NativeProcHealingPlayer$Target` | SAME |
| `NativeProcSkillCatalog` | SAME |
| `NativeProcSkillCatalog$Catalog` | SAME |
| `NativeProcSkillCatalog$Skill` | SAME |
| `NativeReturnNatureTransfer` | SAME |
| `NativeReturnNatureTransfer$Overflow` | SAME |
| `NativeReturnNatureTransfer$Scope` | SAME |
| `NativeReuseActor` | SAME |
| `NativeReuseShutdown` | SAME |
| `NativeRolePublication` | SAME |
| `NativeRolePublication$Lease` | SAME |
| `NativeSourcePlayerActionLock` | SAME |

</details>
