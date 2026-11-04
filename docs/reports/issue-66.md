# Issue #66 — T020-B05 戰鬥併入暫存（new-java，48 類）

**結論：B05 暫存完成，並已回應 Codex P2。** 48 個外層類別在 `merge-inputs/50635f7e/staged/`；可寫進 Java 的 trampoline 已寫進 staged；無法合法表達的剩餘 ASM 由入庫的 `overlay/apply-b05.sh` 逐成員疊回。逐檔 `javac --release 17` 產出剛好 120 個 verify entry（無多、無缺）；對候選 JAR 做 T020d 語意比對 **SAME=120、DIFF=0、ONLY_*=0、清單 extra/missing 皆空、BYTE_SAME=0**。未動 `src/main`、`pom.xml`、`docs/REPORT.md`。

## 做了什麼

1. 依 `docs/combat-merge/batches.json` 的 `B05.classes` 與 `manifest.json` 的 `stored_source`，從 `merge-inputs/50635f7e/sources/` 複製 48 個外層 `.java` 到 `staged/local/lito381/server/`。外層為 `asm-only` 者取該類非 `_variants` 的 Java 來源（凍結最後一份完整原始碼）。`NativeSummonRetirement`／`NativeTamedWorldLifecycle` 使用外層 canonical 來源，不用 `_variants`。
2. 凍結 Java 不是最終候選（T020a：12 個外層＋部分內部類最後變動層為 ASM）。把能合法寫進 Java 的額外成員與 trampoline 補進 staged（**39** 檔與凍結來源位元組相同；**9** 檔有後續移植）：
   - `NativeSourceTargetIconBatch.deferTarget` 與 `Work` 的 Participant 建構子
   - `NativeStaticNpcMotion.Motion.nativeStaticCancellationRetired`
   - `NativeStaticNpcScene.Actor`／`State` 的額外 volatile 欄位
   - `NativeSourceSignedRecovery.member`：`HealingCast.scoped/member`、`AcceptedPlayers.fromActor`、`FirstPlayers.member` 回退
   - `NativeSourceProcScalarSkills.supports`／`expiry`／`activate`：先問 `ProcScalarExtras` 再走原 switch
   - `NativeSourceTargetNegativeLife.close`：先 `ControlledNegativeFinal.close`
   - `NativeSourceSingleNpc.early`：先 `TargetNpc.early`
   - `NativeSourcePoisonNpc.current(Status)`：先 `PoisonAudience.current`
   - `NativeSourceWindTargets.Aim.valid`：先 `MaterialFirstLife.valid`
3. 無法用合法 Java 表達的剩餘 splice（record 額外實例欄位、未在所有建構子賦值的 `final` 欄位、方法本體／lambda／InnerClasses、javac 與候選 codegen 不同）在 javac 後以 ASM **逐成員**疊回候選版，沒有整顆換成候選 class（`BYTE_SAME=0`）。
4. 回應 Codex P2：overlay 不再只活在 `scratch/`。可重跑實作入庫於 `merge-inputs/50635f7e/overlay/`（`OverlayB05.java`、`apply-b05.sh`）。乾淨 checkout 執行 `bash merge-inputs/50635f7e/overlay/apply-b05.sh` 可重現 SAME=120。

B05 混合類（外層或內部 `source_kind=asm-only`，共 13 個外層家族）：`NativeSourcePoisonNpc`、`NativeSourceRawDamage`、`NativeSourceRemainingMaterials`、`NativeSourceSignedRecovery`、`NativeSourceSingleNpc`、`NativeSourceTargetNegativeLife`、`NativeSourceWeaponBuffTargets`、`NativeSourceWeaponFinalFrame`、`NativeSourceWindTargets`、`NativeStaticCancellationReplacement`、`NativeStaticNpcMotion`、`NativeStaticNpcScene`、`NativeSummonGroundTransfer`。內部類由外層 Java 編出後再與候選比對。

## 編譯

環境：`javac 21.0.12.1`、`--release 17`。repo 無 `lib/`，classpath 為 `reference/candidate-50635f7e.jar:reference/base-0.6.71.jar`。

### 設計書寫的「整批一次 javac」（失敗，已記錄）

```text
PWD=lito伺服器/LiTo381-Server
javac --release 17 -encoding UTF-8 \
  -cp reference/candidate-50635f7e.jar:reference/base-0.6.71.jar \
  -d <tmp> merge-inputs/50635f7e/staged/local/lito381/server/*.java
exit 1
```

失敗原因（已驗證，21 errors，見 `scratch/issue-66/javac-batch.err`）：同批 sibling 原始碼蓋住候選 JAR 上「凍結 Java 沒有、ASM 才加上」的成員。例如 `NativeSourceRawAudience` 讀 `Member.nativeSourceNpcHpObserver` 與 `Output.nativeSourceRawAudience`（record 額外欄位，Java 不能宣告）；`NativeSourceWandReaction` 呼叫 `NativeSourceRawDamage.nativeSourceWandReactionOriginalHurt`（trampoline，同批 RawDamage 原始碼沒有）。這不是漏複製。

### 實際採用（成功）

對候選 JAR **逐檔** javac，讓本批原始碼不能遮住候選上的額外成員：

```text
javac --release 17 -encoding UTF-8 \
  -cp reference/candidate-50635f7e.jar:reference/base-0.6.71.jar \
  -d <tmp> staged/local/lito381/server/<每檔>.java
```

48 檔皆 exit 0。產出 **120** class、**extra=0**、**missing=0**，對齊 `B05.verify_entries`。完整逐檔紀錄見 `scratch/issue-66/javac-each.log`（不入庫）。一鍵重跑：

```bash
bash lito伺服器/LiTo381-Server/merge-inputs/50635f7e/overlay/apply-b05.sh scratch/issue-66
```

## 語意

移植 trampoline 後，javac 產物（尚未 overlay）對候選為 **SAME=93 DIFF=27**（先前凍結 Java 為 SAME=89 DIFF=31）。`NativeSourceTargetNegativeLife.close` 與已寫進 Java 的 Motion／Actor／State 額外欄位現在 javac 即 SAME，不必 overlay。`SignedRecovery.member`／`ProcScalarSkills.expiry` 等已有對應 Java，但 javac codegen 與候選仍 DIFF（`ALOAD`／`ASTORE` vs `DUP`），所以仍走 overlay。

疊上候選獨有的 ASM 成員後得到 `b05-merged.jar`。27 個 overlay 類皆 `BYTE_SAME=0`（不是整顆 class 換成候選 bytes）。

```bash
bash lito伺服器/LiTo381-Server/tools/semantic-diff/semantic-diff \
  --left scratch/issue-66/b05-merged.jar \
  --right lito伺服器/LiTo381-Server/reference/candidate-50635f7e.jar \
  --classes scratch/issue-66/verify_entries.txt \
  --json scratch/issue-66/semantic-merged.json \
  --md scratch/issue-66/semantic-merged.md
```

**已驗證結果：**

```text
SUMMARY SAME=120 DIFF=0 ONLY_LEFT=0 ONLY_RIGHT=0 BYTE_SAME=0 ALLOW_METHODS=0
```

`classSelection.missingLeft`／`extraLeft` 皆空。候選 JAR SHA256 `50635f7edca8ec6ce77787cc32371c7590b497e48ea202f8a679caf3f517b729`；正式基準 `base-0.6.71.jar` SHA256 `e1498871c7be6b287821c57ef5d95933db1740d9b4d4a80f6c3465f21d99d498`。

## 已驗證／推測／待驗證

- **已驗證：** 48 檔複製自 manifest 指定來源；9 檔含 Java 可表達的移植；逐檔 javac 產出剛好 120 entry；未 overlay 時 SAME=93 DIFF=27；`apply-b05.sh` overlay 後 T020d 對 `b05-merged.jar` vs `candidate-50635f7e.jar` 本批 verify_entries 全 SAME、無多無缺、BYTE_SAME=0；overlay 原始碼與腳本已入庫；未改 `src/main`／`pom.xml`／`docs/REPORT.md`／機密設定。
- **推測：** 剩餘 27 個 DIFF 均為後來 ASM splice 或 javac／候選 codegen 差異（trampoline、record 額外欄位、`allowFrame` 改寫、InnerClasses）。Record 不能合法放額外實例欄位（`Output.nativeSourceRawAudience` 等）。
- **待驗證：** 語意 SAME ≠ 已跑回歸；未跑 `1.啟動伺服器.bat --check`／`--smoke`，未連正式 DB，未啟動遊戲。單元測試／回歸依設計在 B-land 統一處理。未在 Windows／JDK 17 重跑同一組 javac。

## 產出路徑

- `lito伺服器/LiTo381-Server/merge-inputs/50635f7e/staged/local/lito381/server/`（48 個外層 `.java`）
- `lito伺服器/LiTo381-Server/merge-inputs/50635f7e/overlay/OverlayB05.java`
- `lito伺服器/LiTo381-Server/merge-inputs/50635f7e/overlay/apply-b05.sh`
- `docs/reports/issue-66.md`（本檔）
- 驗證產物（不入庫）：`scratch/issue-66/javac-batch.err`、`javac-each.log`、`b05-compiled.jar`、`b05-merged.jar`、`overlay.log`、`semantic-compiled.json`、`semantic-merged.json`

## 給下一棒的提示

- **B04／B06 起**（同為分批暫存）：不要假設「整批一次 javac vs 裸 candidate」能過。混合類務必逐檔編，否則同批原始碼會遮住 ASM 才有的成員。
- Record 不能合法放額外實例欄位（本批 `NativeSourceRawDamage$Output.nativeSourceRawAudience`）。`Member.nativeSourceNpcHpObserver` 在候選是 `final transient` 且 3-arg／5-arg 建構子未賦值，Java 不合法，只能 ASM。
- 可寫的 trampoline 已在 staged Java。B-land 不能只搬 Java 再重編就當完成：必須跑 `merge-inputs/50635f7e/overlay/apply-b05.sh`（或等價流程）。剩餘 javac 後仍 DIFF、需 overlay 的 27 個 class：

```text
NativeSourcePoisonNpc / $Commit
NativeSourceProc68Icon
NativeSourceProcScalarSkills
NativeSourceRawDamage / $Action / $Member / $Npc / $Output
NativeSourceRemainingMaterials / $Aim
NativeSourceSignedRecovery
NativeSourceSingleNpc
NativeSourceTargetIconBatch / $Work
NativeSourceTargetMask
NativeSourceWeaponBuffTargets / $Aim
NativeSourceWeaponFinalFrame
NativeSourceWindTargets / $Aim
NativeStaticCancellationReplacement / $Retired
NativeStaticNpcMotion
NativeStaticNpcScene
NativeSummonGroundTransfer$Stage
NativeTamedWorldLifecycle$Respawn
```

- `NativeSourceTargetNegativeLife`、`NativeStaticNpcMotion$Motion`、`NativeStaticNpcScene$Actor`／`$State` 現在 javac 即 SAME，不必 overlay。
- **B-land** 才把 staged 搬進 `src/main`；搬時必須一併保留 overlay 目錄並重跑語意比對。
- 不要改 `docs/REPORT.md`（調度器合併後加索引）。
- 本任務不是 plan／design，沒有寫 `docs/plans/issue-66.json`。
