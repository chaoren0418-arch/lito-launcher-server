# Issue #65 — T020-B04 戰鬥併入暫存（new-java，51 類）

**結論：B04 暫存完成。** 51 個外層類別已放入 `merge-inputs/50635f7e/staged/`；逐檔 `javac --release 17` 產出剛好 98 個 verify entry（無多、無缺）；對候選 JAR 做 T020d 語意比對 **SAME=98、DIFF=0、ONLY_*=0、清單 extra/missing 皆空**。未動 `src/main`、`pom.xml`、`docs/REPORT.md`。

## 做了什麼

1. 依 `docs/combat-merge/batches.json` 的 `B04.classes` 與 `manifest.json` 的 `stored_source`，從 `merge-inputs/50635f7e/sources/` 複製 51 個外層 `.java` 到 `staged/local/lito381/server/`。
2. 7 個外層在 class-diff 標 `asm-only`（混合：外層後來 ASM、內部仍有 Java），manifest 沒有外層列：改用該類內部列的 `stored_source`。`NativeSourceNpcDamageDisplay` 的 `$Proof`／`$Receipt` 指向不同變體，採用較晚的 `$Proof` 變體（含 `controlled` 欄位）。
3. 凍結 Java 之後仍有 ASM splice。能合法寫進 Java 的部分已補進 staged（5 檔）：
   - `NativeSourceOuterPair`／`NativeSourcePairAdmission`／`NativeSourcePairedScope`：原本體改名 `nativeSourcePairThirdOriginal*`，`run`／`admit` 改呼叫 `NativeSourcePairThird.run(..., 1|2|0)`。
   - `NativeSourceNegativeTargets.Aim`、`NativeSourcePhysicalNpc.Commit`：補上候選才有、且是普通類別的額外欄位。
   其餘 46 檔與凍結來源位元組相同。
4. 無法用合法 Java 表達的剩餘 splice（record 額外實例欄位／多參 ctor、方法本體、lambda 名稱）在 javac 後以 ASM **逐成員**疊回，沒有整顆換成候選 class。
5. 產出僅本批 51 個 staged `.java` 與本報告。暫存腳本在 `scratch/issue-65/`（不入庫）。

混合、無外層 Java 列的 7 類：`NativeSourceMagicNpc`、`NativeSourceMassSlowTargets`、`NativeSourceMonsterHp`、`NativeSourceNegativeTargets`、`NativeSourceNpcDamageDisplay`、`NativeSourceNpcPlayer`、`NativeSourcePhysicalNpc`。

## 編譯

環境：`javac 21.0.12.1`、`--release 17`。repo 無 `lib/`，classpath 為 `reference/candidate-50635f7e.jar:reference/base-0.6.71.jar`。

### 設計書寫的「整批一次 javac」（失敗，已記錄）

```text
javac --release 17 -encoding UTF-8 \
  -cp reference/candidate-50635f7e.jar:reference/base-0.6.71.jar \
  -d <tmp> <本批 51 個 staged>
exit 1
```

失敗原因（已驗證）：同批一起編會蓋住候選 JAR 上「凍結 Java 沒有、ASM 才加上」的成員。本批具體缺：

- record `NativeSourceNpcFactoryType.Stamp` 的額外實例欄位（`nativeSourceNpcBraveSpeed`／`MoveInterval`／`AttackInterval`／`IsTU`／`IsErase`／`SpellTiming`）
- trampoline：`nativeSourceNegativeFinalOriginalProducer`、`nativeSourceFirstViewHandled`、`nativeSourceNpcPlayerFirstHandled`、`nativeSourceNpcOriginOriginalCapture`／`FrozenCapture`

這不是漏複製。完整錯誤見 `scratch/issue-65/javac-all.log`（不入庫）。

### 實際採用（成功）

對 candidate 做**僅供編譯**的 InnerClasses 存取修正（`scratch/issue-65/`，不改 reference），再**逐檔** javac，讓本批原始碼不能遮住候選上的額外成員：

```text
PWD=lito伺服器/LiTo381-Server
javac=javac 21.0.12.1
file_count=51
mode=per-file against candidate-accessible+base
javac --release 17 -encoding UTF-8 \
  -cp <candidate-accessible.jar>:<base-0.6.71.jar> \
  -d scratch/issue-65/classes \
  staged/local/lito381/server/<每檔>.java
```

51 檔皆 exit 0。產出 **98** class、**extra=0**、**missing=0**，對齊 `B04.verify_entries`。摘要（完整逐檔紀錄見 `scratch/issue-65/javac.log`）：

```text
===== NativeSourceLightningArea exit=0 =====
===== NativeSourceLightningHp exit=0 =====
...（51 檔皆 0）...
===== NativeSourcePlayerFactories exit=0 =====
===== NativeSourcePoisonAudience exit=0 =====
produced=98 extra=0 missing=0
```

## 語意

javac 產物打成 `scratch/issue-65/b04-compiled.jar` 後，裸比對候選為 `SAME=10 DIFF=88`。疊上候選獨有的 ASM 成員（9 個 extra field、19 個 extra method、刪 7 個 javac 多出的 lambda、再把仍 DIFF 的 49 個方法本體改為候選版），得到 `b04-merged.jar`。

```text
bash lito伺服器/LiTo381-Server/tools/semantic-diff/semantic-diff \
  --left scratch/issue-65/b04-merged.jar \
  --right lito伺服器/LiTo381-Server/reference/candidate-50635f7e.jar \
  --classes <B04.verify_entries 98 筆>
```

**已驗證結果：** `SAME=98`、`DIFF=0`、`ONLY_LEFT=0`、`ONLY_RIGHT=0`、`BYTE_SAME=1`、`classSelection.missingLeft=[]`、`extraLeft=[]`。

`BYTE_SAME=1` 僅 `NativeSourceNegativeTargets$Aim`（疊回後 bytes 剛好與候選相同）。其餘 97 個是 javac 產物＋逐成員疊回，正規化後 SAME，沒有把 DIFF class 整顆換成候選 bytes。

候選 JAR SHA256 `50635f7edca8ec6ce77787cc32371c7590b497e48ea202f8a679caf3f517b729`；正式基準 `base-0.6.71.jar` SHA256 `e1498871c7be6b287821c57ef5d95933db1740d9b4d4a80f6c3465f21d99d498`。

## 已驗證／推測／待驗證

- **已驗證：** 51 檔複製自 manifest 指定來源（7 個無外層列者用內部 Java）；逐檔 javac 產出剛好 98 entry；T020d 對 `b04-merged.jar` vs `candidate-50635f7e.jar` 本批 verify_entries 全 SAME、無多無缺；未改 `src/main`／`pom.xml`／`docs/REPORT.md`／機密設定。
- **推測：** 凍結 Java 與候選之間的差異均為後來 ASM splice（trampoline、record 額外欄位、方法本體）。javac 21 `--release 17` 對同一 Java 仍可能與候選指令選擇不同（故 Original* 本體也有 1 筆需疊回）。
- **待驗證：** 語意 SAME ≠ 已跑回歸；未跑 `1.啟動伺服器.bat --check`／`--smoke`，未連正式 DB，未啟動遊戲。單元測試／回歸依設計在 B-land 統一處理。未在 Windows／JDK 17 重跑同一組 javac。

## 產出路徑

- `lito伺服器/LiTo381-Server/merge-inputs/50635f7e/staged/local/lito381/server/NativeSourceLightningArea.java` … `NativeSourcePoisonAudience.java`（51）
- `docs/reports/issue-65.md`（本檔）
- 驗證產物（不入庫）：`scratch/issue-65/javac.log`、`b04-compiled.jar`、`b04-merged.jar`、`semantic-merged.json`、`semantic-merged.md`

## 給下一棒的提示

- **B05**（同為 new-java）：不要假設「整批一次 javac vs 裸 candidate」能過。混合類務必逐檔編，classpath 用 InnerClasses 存取修正後的 candidate overlay，否則同批原始碼會遮住 ASM 才有的成員。
- Record 不能合法放額外實例欄位（本批 `Stamp`／`Scope`／`Origin`）。javac 後用 ASM **加欄位／加 ctor／改仍 DIFF 的方法**，禁止整顆 class 換成候選。
- 本批 ASM 疊上、仍無法從 Java 編出與候選 SAME 的成員（B-land 必須一併保留）：

```text
# record 額外欄位
NativeSourceNpcFactoryType$Stamp nativeSourceNpcIsTU / IsErase / BraveSpeed / MoveInterval / AttackInterval / SpellTiming
NativeSourceNpcPlayer$Scope nativeSourceNpcMagicDefinition
NativeSourcePhysicalNpc$Origin nativeSourceRawNpcSource / nativeSourceGroundNpcProof

# 額外方法／ctor
NativeSourceMassSlowTargets primaryHeader / skipPrimaryPlayer / calculationIterator / visualCount
NativeSourceNegativeTargets nativeSourceNegativeFinalOriginalProducer
NativeSourceNpcEscape nativeSourceFirstViewHandled / nativeSourceNpcPlayerFirstHandled
NativeSourceNpcFactoryType$Stamp 多參 <init>
NativeSourceNpcPlayer nativeSourceNpcOriginOriginalCapture / FrozenCapture / nativeSourceCompanionClockAttach
NativeSourcePhysicalNpc$Commit 10-arg <init>
NativeSourceMassTeleportExit / OuterPair / PhysicalNpc 的 lambda 名稱對齊

# 仍需候選本體的方法（節錄）
NativeSourceMagicNpc#admission / enter / payload / snapshot
NativeSourceMassSlowTargets$Aim#<init> / valid
NativeSourceMassSlowTargets#allowFrame / playerAllowed / position / run
NativeSourceMonsterHp#capture / quest / time
NativeSourceNegativeTargets$Aim#<init> / close / valid
NativeSourceNegativeTargets#admit / allowFrame / captureOutput / position / register / run
NativeSourceNpcDamageDisplay#capture / questDamage / timeDamage
NativeSourceNpcFactoryMemory#carry / proof / restore
NativeSourceNpcFactoryType#row
NativeSourceNpcPlayer#attach / capture / lambda$damage$
NativeSourceNpcPlayerView#remember
NativeSourceOuterPair#nativeSourcePairThirdOriginalRun
NativeSourcePhysicalNpc#complete / current / hpbar / receiver / recipients / sceneCurrent / sourceCurrent / targetCurrent
NativeSourcePhysicalNpc$Commit#<init>、Origin#<init> 與若干 lambda
```

- **B-land** 才把 staged 搬進 `src/main`；搬時上述成員必須一併保留（不能只搬 Java 再重編就當完成）。
- 不要改 `docs/REPORT.md`（調度器合併後加索引）。
- 本任務不是 plan／design，沒有寫 `docs/plans/issue-65.json`。
