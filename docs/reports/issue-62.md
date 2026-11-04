# Issue #62 / T020-B01 — 戰鬥併入暫存（new-asm，17 類）第 2 次

## 結論

B01 17 個外層類別在暫存區，標頭 `// DECOMPILED from candidate-50635f7e.jar, TODO review`。**javac 成功**（staged Java 直接編出，無候選／正式 `.class` overlay、無改名、無後處理）。`verify_entries` **22 個 class 無缺漏、無多出**。T020d semantic-diff：**12 SAME／10 DIFF**。

本輪依主控補充修正行為簡化：`c::player` 已改回 `NativeSourceIndirectPlayerProof.player`；`Draft` 改 nested class 並在建構時呼叫 `NativeSourceCubeTemplate.freeze`，保留 `nativeSourceCubeTemplate` 供 `ready()` 讀取。剩餘 10 筆是 javac 無法發出的 ASM／編譯器形狀差，**不是漏移植**。請主控裁定（Issue 需標 `needs:claude`）。本環境 `gh` 唯讀，無法代貼該標。

尚未達到「每個 entry 都 SAME」。未自行改變驗收方式。

## 做了什麼

範圍只改 `lito伺服器/LiTo381-Server/merge-inputs/50635f7e/staged/local/lito381/server/` 下 B01 的 17 個 `.java`，以及本報告。未改 `src/main`、`pom.xml`、`docs/REPORT.md`。

來源以 `tools/combat-continuation/integration-lifecycle/` 原始 Java 為底，把能用 Java 表達的 ASM splice 寫進原始碼。本輪相對第 1 次的行為修正：

| 位置 | 第 1 次（退回） | 本輪 |
|---|---|---|
| Blind `companion`、Forest `apply`、Wind `publish` | `c.players.stream().filter(c::player)` | `filter(p->NativeSourceIndirectPlayerProof.player(c,p))` |
| WeaponBreakTargetLifecycle `captureOutput` 的 `allMatch` | `context::player` | `p->NativeSourceIndirectPlayerProof.player(context,p)` |
| `NativeSourceCubePlacement.Draft` | 5 欄位 record，缺 freeze 欄位 | nested class；5 個 component + `final Object nativeSourceCubeTemplate`；`<init>` 末 `freeze(engine)` |

已套用、且對應 entry 已 SAME 的 splice（與第 1 次相同）包括：PairedCastSafety／CompanionRefScene／BatchTerminal／PendingAdmission／SummonTerminalLifecycle 的 wrapper 或 capture 改寫；CubePlacement 外層 `ready()`／`cadence` 三元／`seal`；DollSkills／View／Slow／Pending／Scope；HealingCast$Cast 的 volatile 欄位。

## 怎麼驗證（已驗證）

候選 JAR SHA-256：`50635f7edca8ec6ce77787cc32371c7590b497e48ea202f8a679caf3f517b729`。

JDK：Temurin 17.0.21（`/tmp/jdk17/bin/javac`）。本機無 `lib/`，classpath 未展開空 glob。

```text
cd lito伺服器/LiTo381-Server
javac --release 17 -encoding UTF-8 \
  -cp reference/candidate-50635f7e.jar:reference/base-0.6.71.jar \
  -d <tmp>/classes \
  merge-inputs/50635f7e/staged/local/lito381/server/Native{BlindSceneAction,CompanionForestArea,CompanionRefScene,CompanionWindAction,ControlledNegativeMaterial,PairedCastSafety,SourceBatchTerminal,SourceCubePlacement,SourceDollAccepted,SourceDollNpc,SourceDollSkills,SourceDollSlow,SourceHealingCast,SourcePendingAdmission,SummonTerminalLifecycle,WeaponBreakBattleCompanion,WeaponBreakTargetLifecycle}.java
```

javac 輸出（exit 0）：

```text
Note: Some input files use unchecked or unsafe operations.
Note: Recompile with -Xlint:unchecked for details.
```

產出恰好 22 個 `.class`，與 `docs/combat-merge/batches.json` B01 `verify_entries` 相同。`jar cf` 後：

```text
bash tools/semantic-diff/semantic-diff \
  --left <tmp>/left.jar \
  --right reference/candidate-50635f7e.jar \
  --classes <B01 verify_entries 22 行>
```

```text
SUMMARY SAME=12 DIFF=10 ONLY_LEFT=0 ONLY_RIGHT=0 BYTE_SAME=0
classSelection.missingLeft []
classSelection.extraLeft []
```

**SAME（12）**：`NativeCompanionRefScene`、`NativePairedCastSafety`、`NativeSourceBatchTerminal`、`NativeSourceCubePlacement`（外層；Draft 改 class 後外層仍 SAME）、`NativeSourceDollAccepted$Pending`、`NativeSourceDollNpc$Scope`、`NativeSourceDollSkills`、`NativeSourceDollSkills$View`、`NativeSourceDollSlow`、`NativeSourceHealingCast$Cast`、`NativeSourcePendingAdmission`、`NativeSummonTerminalLifecycle`。

驗收 JAR 的 `.class` 全部來自上述 javac，沒有從候選或正式 JAR 複製 bytes。

## 不可約 DIFF（已驗證；交主控裁定）

下列 10 個 entry 經合理努力仍無法讓 **javac --release 17** 產出與候選 SAME 的 class。行為已按候選移植；差異是 bytecode 形狀。請主控裁定：接受「Java 行為等價」或為這 10 筆開例外——**不要**在未裁定前 overlay class bytes 或改驗收指令。

| Entry | 正規化 firstDiff | 最小差異與理由 |
|---|---|---|
| `NativeBlindSceneAction` | `companion` insn[330]：left=`INVOKEDYNAMIC test(Context)Predicate`（impl=`lambda$companion$5`）／right=`DUP` | `SourceIndirectPlayerProofMethods` 把 `c::player` 的 indy **impl Handle** 改成 `INVOKESTATIC NativeSourceIndirectPlayerProof.player(Context,ClientSession)Z`（候選 BootstrapMethods #3）。Java 沒有「把已捕獲第一參數的實例方法參照改寫成靜態雙參數方法」的語法；寫 lambda 才能保留生命 token／斷線／accepted freeze 檢查。代價是多出合成方法，指令流與候選 method-ref 錯位。另：Temurin 為 `valid` 的 anyMatch 另合成 `lambda$valid$4`；候選把同簽名 lambda 接到 `lambda$run$0`。 |
| `NativeCompanionForestArea` | `apply` insn[164]：同上 indy vs `DUP` | 同上，PORT=`apply`。多出 `lambda$apply$3`。 |
| `NativeCompanionWindAction` | `lambda$publish$#0(Context,ClientSession)Z` **only left** | 同上，PORT=`publish`。候選 `publish` 無合成 lambda，只改 method-ref Handle。 |
| `NativeSourceCubePlacement$Draft` | class access：left=`0x30`（`ACC_SUPER\|ACC_FINAL`）／right=`0x10030`（加上 `ACC_RECORD`） | 候選是 **record + 非 component 欄位** `final Object nativeSourceCubeTemplate`，`<init>` 在 `Record.<init>` 之後 `freeze(engine)`。Java 17 record 禁止額外 instance 欄位。本輪改 nested class：欄位集合、freeze 呼叫、`ready()` 所需欄位都在（javap 已對過）；外層 `NativeSourceCubePlacement` 仍 SAME。無法同時保留 `ACC_RECORD`／`recordComponents`／`ObjectMethods` indy。 |
| `NativeSourceDollAccepted` | `physical` insn[45]：left=`ALOAD 8`／right=`DUP` | `SourceAcceptedPlayersMethods` 在 `Actor.life()` 前插入 `DUP`，再把 `new Member` 改成 `fromActor`。javac 對同一區域變數發兩次 `ALOAD`，不發 `DUP`。呼叫仍是 `fromActor(caster, binding, actor, actor.life())`。 |
| `NativeSourceDollNpc` | `lambda$nativeSourcePhysicalOriginalNatural$#0` **only left** | `SourcePhysicalNpcMethods` 先以方法名 `natural` 編譯（合成名 `lambda$natural$N`），再把方法改名為 `nativeSourcePhysicalOriginalNatural` 並加 wrapper。javac 若本體就叫新名，合成名帶新方法名；T020d 依 `lambda$方法名$` 前綴正規化後對不上。wrapper 仍必須叫 `natural`／`naturalCommit`（commit 走 wrapper）。 |
| `NativeSourceHealingCast` | `run` insn[172]：left=`ALOAD 7`／right=`ALOAD 11` | `SourceControlledHealingMethods` 在 **owned 三個引數已推上 stack 之後**、`INVOKESTATIC owned` 之前插入 `capture(c,cast)`。Java 無法在「引數求值」與「呼叫」之間插入陳述。源碼把 `capture` 放在 `owned(...)` 之前；lambda／method-ref 建構無副作用，執行順序與插入點等價，只是局部變數編號不同。 |
| `NativeWeaponBreakTargetLifecycle` | `captureOutput` tryCatch length left=1／right=0 | 候選在無 `throws`、無 try 的方法裡直接呼叫 `NativeSourceWeaponFinalFrame.capture`（該方法 `throws IOException`）。合法 Java 必須 catch 或宣告 throws。本輪 `try/catch` 再包成 `UncheckedIOException`，故例外表多一筆。`allMatch` 已走 IndirectPlayerProof（行為層）。 |
| `NativeWeaponBreakBattleCompanion` | class `innerClasses` | `SourceWeaponTargetLifeMethods` copy `prepare`→`nativeSourceWeaponFirstPrepare(Context)` **不更新** innerClasses。javac 只要描述子提到 `$Context` 就會寫入 `NativeAreaMaterialAction$Context`。候選 innerClasses 無此項。方法本體已含 Context 參數與 snapshot splice。 |
| `NativeControlledNegativeMaterial` | `cast` TRY 標籤 `L9–L11` vs `L11–L13` | 此類沒有對自身的方法 rename splice。差異是 Microsoft JDK 17.0.17（原始建置）與 Temurin 17.0.21 的例外表／標籤配置。`try/finally` 結構與 `c.player(p)` 直接呼叫（非 method-ref，ASM 不改）均保留。 |

## 未驗證／待驗證

- 實機登入／戰鬥（本批規定不啟動遊戲、不連正式 DB）。
- 單元測試／回歸（未動 `src/main`，B-land 再跑）。
- 主控是否接受上表 10 筆為 B01 驗收例外（待 `needs:claude` 裁定）。
- 用原始 Windows `jdk-17.0.17.10-hotspot` 重編是否能消掉 `NativeControlledNegativeMaterial` 的 try 標籤差（推測：即使標籤重合，其餘 9 筆仍在）。

## 產出路徑

- `lito伺服器/LiTo381-Server/merge-inputs/50635f7e/staged/local/lito381/server/Native{BlindSceneAction,CompanionForestArea,CompanionRefScene,CompanionWindAction,ControlledNegativeMaterial,PairedCastSafety,SourceBatchTerminal,SourceCubePlacement,SourceDollAccepted,SourceDollNpc,SourceDollSkills,SourceDollSlow,SourceHealingCast,SourcePendingAdmission,SummonTerminalLifecycle,WeaponBreakBattleCompanion,WeaponBreakTargetLifecycle}.java`
- `docs/reports/issue-62.md`
- 本機驗證產物（不進 git）：`scratch/b01-out/{left.jar,result.json,result.md,javac.log}`

## 給下一棒的提示

1. **不要 overlay 候選／正式 `.class`，也不要在 semantic-diff 前改名或 ASM 後處理產物。** 驗收左 JAR 必須是 staged Java 直接 javac 的結果。
2. 行為層不要再改回 `c::player`；Draft 不要刪掉 `nativeSourceCubeTemplate`／`freeze`（`NativeSourceCubeTemplate.ready()` 用 `CombatAccess.field(draft,"nativeSourceCubeTemplate")`）。
3. 上表 10 筆不是「留給下一棒做 ASM」的工作項，是 **javac 無法 SAME** 的清單，等主控裁定。
4. 不要改 `docs/REPORT.md`。不要從 CFR 重頭反編譯這 17 類。
5. 本環境無法寫 Issue 標籤；請主控或調度器在 #62 加上 `needs:claude`。
