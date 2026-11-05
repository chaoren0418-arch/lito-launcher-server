# Issue #69 — T020-B08 戰鬥併入暫存

## 本輪結論（2026-11-06，Asia/Taipei）

**已合併 #254、修正 lambda 並重編全部 30 份 Java；160 個 entry 中 159 個可接受，仍有 1 個 DIFF，B08 尚未通過，不可 B-land。**

正式結果：**141 SAME、10 SAME_PATHS、7 SAME_CFG、1 LAMBDA_PAIRED_BY_CALLSITE、1 DIFF**。missing=0、extra=0；類別及方法級 ONLY_LEFT／ONLY_RIGHT 均為 0。唯一剩餘項是 `NativeLightAction$Batch#emit()V`。

此項已核對 javap：核心 try 的範圍兩側皆是 `[0,4)`，只保護 `nativeSourceBatchOriginalEmit()`。本輪診斷定位到核准 helper 正規化的**實體指令排列限制**；未自行修改工具、增加豁免或改寫正式驗收結果。依[本輪主控裁定](https://github.com/chaoren0418-arch/lito-launcher-server/issues/69#issuecomment-6670460666)交 `needs:claude`，完整差異如下。

## 已驗證：做了什麼

- 沿用既有 worktree 與 `task/69`，從 `5434695aefe5ee91a874c7a6bf26c044f566d009` 開始，未 checkout／建分支。
- fetch 後合併 main `8df7c89c3a9b4dbdb8eb01b40a5b53462dbac0f8`，合併提交 `47e6d398ad659f97148a0e5c99128bcb2da6abd5`，無衝突；包含 #254／PR #260 的 merge `5de170c`。semantic-diff 程式提交為 `45bbbe4cfd4f21b0080caa2726ac2928d0b8cc1c`，由本工作樹重建工具。
- `commitPair` 在原有 locked 守門之後，先將 binding、target、receipt 複製到 lambda 內部別名，再引用 task；捕獲順序變為 `Ingress, ClientSession, Binding, ClientSession, Receipt, Task`，與候選一致。只讀區域參照，沒有提前執行 getter、守門、鎖或任務。
- 同一 lambda 的 multi-catch 對齊為 `RuntimeException | Error`。兩型別互不隸屬、共用同一 handler，副作用不變；原先反向排序會造成工具保留的有序例外邊不同。本體以 423 條路徑證明後，成功按呼叫點配對。
- `deferScoped`／`deferWeaponTargetOutput` 明確共用候選既有名稱 `lambda$deferWeaponTargetOutput$0` 的 private static 本體。Java lambda 只轉呼叫，既有正規化可還原為同一 handle；沒有新增核准。其邏輯依實際 bootstrap 目標，只檢查 accepted binding 後執行 task。候選舊 `lambda$deferScoped$10` 含 target 檢查，但已無引用，正式工具列 ORPHAN_IGNORED，沒有將舊邏輯帶回。
- 共用 defer 本體與候選差異只剩候選多餘的 `GOTO`；正式工具以 6 條路徑證明後按呼叫點配對。外層 `NativeLightAction` 整類為 LAMBDA_PAIRED_BY_CALLSITE。
- `Batch.emit` 完整比對後確認既有核心 try 涵蓋已一致，因此保留執行邏輯並加註解標出範圍。沒有把正常 `finish(null)` 納入該 catch，避免 finish 失敗時再執行一次 finish。**本項未達到 DIFF=0，不能宣稱已完成修復。**
- `NativePlayerCommit`、`NativeProcHealingEncounter` 依裁定未改 Java。#254 已使 drain、acceptedTail、Dispatch.run 為 SAME_CFG；nativeManaTransferRecover 不再列方法差異，按線性／既有正規化通過。
- 30 份來源相對起點逐檔比對，只有 `NativeLightAction.java` 改動；其他 29 份完全一致。保留六個 `@AsRecord`、治療巢狀 monitor／vitalLock finally、package-private Output、核准轉拋 helper 及 Exceptions 條目。`NativeProcHealingPlayer` 與 Output 仍為 SAME。
- 最終 JAR 相對前輪僅 NativeLightAction、Batch、Ingress 三個 class bytes 變更（含來源行號變動）；其餘 157 個相同。沒有以容器雜湊代替 class 比對。
- 本輪手工交付僅指定 Java 與本報告；`src/main`、`pom.xml`、`docs/REPORT.md` 相對本輪合併 main 無差異，`git diff --check` 通過。main 帶入其他批次不算本輪手工修改。

## 怎麼驗證

Microsoft OpenJDK 17.0.17，ASM／asm-tree／asm-analysis 9.7。前兩項使用既有 Maven 快取；asm-analysis 使用 worktree `target/issue-69/asm-analysis-9.7.jar`；HikariCP 5.1.0 使用既有 Maven 快取，沒有新增 stub。

先用 #254 重建原來源取得 **141 SAME＋10 SAME_PATHS＋7 SAME_CFG＋2 DIFF**；修正後在全新 `target/issue-69/final-254/` 重建工具、註解與全部 30 份 Java，AsRecord `scanned=160 converted=6`。最後核對 ZIP 集合恰好等於 B08.verify_entries 的 160 個 class。

以下以伺服器目錄為基準，`<W>` 為本 worktree `scratch/issue-69/final-254`，`<T>` 為 `target/issue-69/final-254`；`<ASM>` 為三個 ASM 9.7 JAR 以分號串接。argfile 是 UTF-8 無 BOM，每行雙引號包住絕對路徑；30 個來源及 160 個 entry 均從 batches.json 的 B08 產生。

```text
javac -J-Dfile.encoding=UTF-8 --release 17 -encoding UTF-8 -cp "<ASM>"
  -d <T>/tools @<W>/tools.args
# semantic-diff 與 as-record 的 main Java，退出碼 0
javac -J-Dfile.encoding=UTF-8 --release 17 -encoding UTF-8 -cp ""
  -d <T>/annotations @<W>/annotations.args
# AsRecord.java、AsmFinal.java，退出碼 0
javac -J-Dfile.encoding=UTF-8 --release 17 -encoding UTF-8
  -cp "reference/candidate-50635f7e.jar;reference/base-0.6.71.jar;<T>/annotations;lib/*;D:/dev-cache/maven/repository/com/zaxxer/HikariCP/5.1.0/HikariCP-5.1.0.jar"
  -d <T>/classes @<W>/staged.args
Note: Some input files use unchecked or unsafe operations.
Note: Recompile with -Xlint:unchecked for details.
# 退出碼 0
java -cp "<T>/tools;<ASM>" local.lito381.asrecord.AsRecord <T>/classes
as-record: scanned=160 converted=6
# 退出碼 0；只打包 classes 下 class，排序 entry 並固定 ZIP 時間
java -Dfile.encoding=UTF-8 -Xmx2g -cp "<T>/tools;<ASM>" local.lito381.semanticdiff.SemanticDiff
  --left <T>/b08.jar --left-classpath reference/candidate-50635f7e.jar
  --right reference/candidate-50635f7e.jar --classes <W>/entries.txt
  --access-widen <W>/access-widen-approved.txt
  --extra-methods <W>/extra-methods-approved.txt
  --exceptions-exempt <W>/exceptions-exempt.txt
  --json <W>/semantic.json --md <W>/semantic.md
SUMMARY SAME=141 SAME_EFFECTS=0 SAME_PATHS=10 ORPHAN_IGNORED=0 SAME_CFG=7 LAMBDA_PAIRED_BY_CALLSITE=1 VISIBILITY_BRIDGE=0 SAME_LAYOUT=0 DEVIATION=0 DIFF=1 ONLY_LEFT=0 ONLY_RIGHT=0 BYTE_SAME=1 ALLOW_METHODS=0
# 工具退出碼 0 不代表驗收通過；DIFF 仍為 1
ZIP entries=160; expected=160; missing=0; extra=0
classSelection.missingLeft=[]; classSelection.extraLeft=[]
```

方法級另有 17 SAME_CFG、2 LAMBDA_PAIRED_BY_CALLSITE；不是額外 class 數。

### 原核准清單（全部套用，沒有新增）

任務只交付 staged Java 與本報告，因此未修改共用清單；下列精確條目存於 scratch，供 B-land 接手。access-widen 只取 B08 owner，避免整份跨批清單被工具拒絕。無 effects、layout、deviation、metadata 或 allow-methods 豁免。

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

### 輸入與最終產物 SHA-256

- `lito伺服器/LiTo381-Server/reference/base-0.6.71.jar`：`e1498871c7be6b287821c57ef5d95933db1740d9b4d4a80f6c3465f21d99d498`
- `lito伺服器/LiTo381-Server/reference/candidate-50635f7e.jar`：`50635f7edca8ec6ce77787cc32371c7590b497e48ea202f8a679caf3f517b729`
- `lito伺服器/LiTo381-Server/target/issue-69/final-254/b08.jar`：`fc7697767d2da777f8e6bfd21bf8297701da1a2698f2795367f724c23859066f`
- `scratch/issue-69/final-254/semantic.json`：`9988a5f60ca25f2c73f0fd357c734c1c6e8f1fde88f0ca800ace0ae1e12ec0f1`
- `lito伺服器/LiTo381-Server/merge-inputs/50635f7e/staged/local/lito381/server/NativeLightAction.java`：`8561ad87c710bb8ce8faccbf634f221e8dad128a498bcfee3654802a1ed60ca8`

## 唯一剩餘差異與診斷（已驗證）

正式工具的完整 Batch 方法結果：

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
      "detail": "method emit()V: left=1:[3:java/lang/Throwable  ] right=1:[6:java/lang/Throwable  ]; path event multisets differ (left=6, right=4); labelled CFGs differ"
    },
    {
      "method": "nativeSourceBatchOriginalEmit()V",
      "status": "SAME_CFG",
      "detail": "entry-rooted labelled CFGs equal"
    }
  ],
  "effectsMethods": [],
  "approvedMethods": [],
  "firstDiff": {
    "where": "method",
    "member": "emit()V",
    "insnIndex": null,
    "detail": "left=1:[3:java/lang/Throwable  ] right=1:[6:java/lang/Throwable  ]; path event multisets differ (left=6, right=4); labelled CFGs differ"
  }
}
```

### javap 完整 emit 與例外表

命令：`javap -p -c -s -classpath <staged 或 candidate JAR> 'local.lito381.server.NativeLightAction$Batch'`。以下從最終產物直接取得，未改寫 bytecode。

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

scratch 的 EmitProbe254 直接呼叫既有 handler body 檢查，僅在記憶體複本移除其核准的兩個 handler，再交既有完整方法比較器，得到 SAME_PATHS（4）。它沒有修改 Java、JAR、工具來源或核准清單，**這是定位證據，不能作為本批正式通過收據**。完整診斷輸出：

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

證據支持剩餘差異位於 helper 消除的實體事件排列限制，而非核心 try 範圍。建議主控裁定工具後續：保留 handler 原樣轉拋與覆蓋證明，改以控制流程對應驗證 coverage，之後再執行完整比較；不能只刪除事件排序檢查而省略覆蓋安全條件。此方向未實作、未由主控核准，正式 DIFF 保留。

## 待驗證／給下一棒的提示

1. **Claude 主控**：依完整 emit 證據裁定剩餘工具缺口；Issue #69 加 `needs:claude`。#254 已合併且指定四個方法已過，不再等待或重派同一依賴。不要擴大 emit 的核心 try 來追求字面形狀相同。
2. **工具後續執行器**：檢查 `RethrowHandlers.normalize` 的事件對齊與 coverage 證明，加入正常／例外區塊順序互換的正例及錯誤涵蓋反例；修好工具後再由 B08 重編全部 30 份、轉換並重驗 160 entries。正式 DIFF=0 才可交 B-land。
3. **B-land 整合者**：保留上述 helper／Exceptions 核准、六個 AsRecord 與 Output 可見性；本輪沒有新增豁免。單元測試／遊戲回歸依指定留 B-land 統一處理。
4. 本輪未執行 staged 應用程式、未做實機驗收、未部署，未啟停正式程序、連 DB 或讀取機密設定／封存／releases。Git 操作僅必要地更新 worktree 共用 `.git`；原專案工作檔未寫入。

## 產出路徑

- Git 交付：`lito伺服器/LiTo381-Server/merge-inputs/50635f7e/staged/local/lito381/server/NativeLightAction.java`、`docs/reports/issue-69.md`。PR 累計範圍仍是 B08 的 30 個 Java 與報告。
- 重現腳本：`scratch/issue-69/final_254.py`；診斷與保留檢查：`scratch/issue-69/evidence_254.py`、`EmitProbe254.java`。
- 最終核准檔／argfile／完整 argv 與退出碼 audit／來源雜湊／semantic JSON、Markdown／方法 dump／javap：`scratch/issue-69/final-254/`。
- 最終建置產物：`lito伺服器/LiTo381-Server/target/issue-69/final-254/`，不入 git。
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
| `NativeLightAction$Batch` | DIFF |
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
