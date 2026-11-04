# Issue #69 — T020-B08 戰鬥併入暫存（existing-native，30 類）

## 結論

B08 30 個外層類別已寫入 `merge-inputs/50635f7e/staged/`，**javac 成功、160 個 class 與 `verify_entries` 完全對齊**（無多餘、無缺漏）。產物是 staged Java 直接編出，**沒有**用候選／正式 `.class` bytes 覆蓋。

語意比對尚未全綠：`semantic-diff` vs `reference/candidate-50635f7e.jar` 為 **SAME=114／DIFF=46／ONLY_LEFT=0／ONLY_RIGHT=0／BYTE_SAME=1**。剩餘 46 筆以 javac 無法表達的 ASM 形狀為主（record 額外欄位、DUP-before-PUTFIELD、private `invokespecial` vs nestmate `invokevirtual`、InnerClasses 屬性），**needs:claude** 裁定是否接受這些 javac-unreachable 差異，或改寫驗收／補凍結 Java。

未改 `src/main`、`pom.xml`、`docs/REPORT.md`。

## 做了什麼

範圍只新增／修改：

- `lito伺服器/LiTo381-Server/merge-inputs/50635f7e/staged/local/lito381/server/` 下 B08 30 個 `.java`
- 本報告 `docs/reports/issue-69.md`

做法（三方移植）：

1. **19 個 DECOMPILED**（`src/main` 標頭為 CFR）：以候選 CFR 為底，轉成可編譯 Java（record 還原、CFR 產物修補），保留 `// DECOMPILED (CFR 0.152) from reference/base-0.6.71.jar` 標頭。
2. **11 個有原始碼**：從 `src/main` 複製後套候選相對正式版的行為差。`NativeCastMaterial` 用 `merge-inputs/50635f7e/sources/` 凍結版。`LegacyMySqlRepository` 從 `Accounts.java` 抽出獨立頂層類（候選／正式 JAR 皆為此頂層類，不是 `Accounts` 的內部類）。
3. `NativeLightAction` 候選相對 `src/main` 差很大（CFR ~935 行 vs 原始 ~108 行）：保留 ORIGINAL SOURCE 標頭，方法面依候選補齊（dispatch 鏈、ManualScope、Batch 額外欄位、`validTargets`／`commitPair`／`NativeSourceMaterialPairIngress` 等）。
4. 可寫成 Java 的候選欄位／條件已補上，例如：
   - `ClassicWorldMonsterPopulation`：`nativeStaticMotionHub`／`nativeStaticMotionInstance`、`Actor.nativeTamedDropInitialized`／`nativeTamedRespawnGeneration`；`TamedRelease` compact ctor 改為與候選 bytecode 相同的 `objectId ==` 拒絕條件。
   - `NativeBattleVisibility.afterMove`：非戰鬥與迴圈後呼叫 `NativeSourceCubeVisibility.refresh`。
   - `NativeCancellationNpcStatic$Target`：補 `pc`／`instance` 欄位。
   - `LegacyMySqlRepository`：拿掉 `src/main` 才有的 `LegacyJdbcNetworkDefaults`（不在候選／正式 JAR）；ctor 加 `UnifiedConfiguration.aliases(p)`。

未使用候選／正式 class bytes 覆蓋 staged 編譯產物。

## 怎麼驗證

### 編譯（已驗證）

環境：OpenJDK 21.0.12.1，`javac --release 17 -encoding UTF-8`。repo 內 **沒有** `lib/`。`LegacyMySqlRepository` 需要 HikariCP（與 `src/main` 相同，5.1.0），不入庫，只放 `scratch/issue-69/deps/`。

候選 `NativeSourceTargetIconBatch.finish/rescue/independent` 帶 ASM 後加的 `throws Throwable`／`throws IOException`，但候選呼叫端（含 `NativeLightAction$Batch.emit`）是在加 throws **之前**編的，方法 Exceptions 屬性為空。若直接對候選 JAR 編譯，javac 會要求呼叫端宣告／捕捉 checked exception，產物必然與候選不同。

編譯期依賴用「去掉 Exceptions 屬性」的 **非 B08** helper stub（`NativeSourceTargetIconBatch` 一份，不進入 `/tmp/b08-out`），**沒有**改 B08 產物 bytes：

```text
javac --release 17 -encoding UTF-8 \
  -cp /tmp/b08-throws-stub.jar:reference/candidate-50635f7e.jar:reference/base-0.6.71.jar:<scratch>/HikariCP-5.1.0.jar:<scratch>/slf4j-api-2.0.9.jar \
  -d /tmp/b08-out \
  merge-inputs/50635f7e/staged/local/lito381/server/{ClassicWorldMonsterPopulation,LegacyInventoryWeightLoader,LegacyMySqlRepository,NativeAcceptedHpCompletion,NativeArmorProc,NativeArmorState,NativeBattleVisibility,NativeCancellationBattleRuntime,NativeCancellationNpcStatic,NativeCastMaterial,NativeCombatAttacker,NativeCombatOrigin,NativeDeItemDefinitionsSql,NativeEquipmentDefense,NativeLightAction,NativeMassTeleportAnswerLife,NativeNpcLifetime,NativePlayerActionScope,NativePlayerCommit,NativePlayerLifetime,NativePlayerVitalCompletions,NativeProcHealingCompanion,NativeProcHealingEncounter,NativeProcHealingPlayer,NativeProcSkillCatalog,NativeReturnNatureTransfer,NativeReuseActor,NativeReuseShutdown,NativeRolePublication,NativeSourcePlayerActionLock}.java
```

- **exit 0**
- stderr：`Note: Some input files use unchecked or unsafe operations.`（無 error）
- `/tmp/b08-out` **160** 個 `.class`，與 `docs/combat-merge/batches.json` B08 `verify_entries` 集合相同（extra=0、missing=0）

### 語意（已驗證指令與計數；未全 SAME）

```text
bash tools/semantic-diff/semantic-diff \
  --left /tmp/b08-staged.jar \
  --right reference/candidate-50635f7e.jar \
  --classes scratch/issue-69/verify_entries.txt
```

```text
SUMMARY SAME=114 DIFF=46 ONLY_LEFT=0 ONLY_RIGHT=0 BYTE_SAME=1 ALLOW_METHODS=0
```

BYTE_SAME：`ClassicWorldMonsterPopulation$MonsterTameCommit`。

## 剩餘 DIFF（最小差異）— needs:claude

禁止 overlay。下列寫不出與候選 byte-equivalent 的 Java（或再改也只會換成另一種 javac 形狀）。請主控裁定：接受為 B08 javac-unreachable、或改驗收正規化、或另開凍結／ASM 任務。

### A. record／類別額外欄位（候選 ASM 加上，javac 的 record 不允許非 component 實例欄位）

| entry | 僅候選有的欄位 |
|---|---|
| `NativeLightAction$Ingress` | `nativeSourceSelfIngressFirst:Ljava/lang/Object;`（record） |
| `NativeCombatAttacker$Ref` | `nativeSourceNpcMasterProof:Ljava/lang/Object;`（record） |
| `NativePlayerCommit$DefenseVisual` | `nativeSourceDollDefenseProof:Ljava/lang/Object;`（record） |
| `NativeProcSkillCatalog$Catalog` | `nativeSourceIcons1Policy:Ljava/lang/Object;`（record） |
| `ClassicWorldMonsterPopulation$Snapshot` | `nativeTamedCapturePreparation:Ljava/lang/Object;`（record；另有 `sourceCombatOrigin`） |

compact ctor 可呼叫 `NativeSourceSelfFinalFrame.initialize(this)`，**不能**再宣告額外實例欄位。

### B. DUP-before-PUTFIELD／區域變數編號（ASM 保留 DUP；javac 先 ASTORE）

`NativeNpcLifetime`（`arm`／`$Dispatch.run`／`$Lane.requestDrain`／`$Retry.run`）、`NativePlayerVitalCompletions$Receipt.release`、`NativeReuseShutdown.defer`、`NativePlayerCommit$Binding.capture`、`NativePlayerCommit$Queue.claim`：firstDiff 皆 `ASTORE n` vs `DUP`。

### C. javac 配置／try 表／捕捉變數槽

| entry | firstDiff |
|---|---|
| `NativeLightAction$Batch.emit` | `invokevirtual` vs `invokespecial` `nativeSourceBatchOriginalEmit`（`--release 17` 對同 nest 的 private 用 invokevirtual；候選為 invokespecial） |
| `NativeArmorState` | InnerClasses 多了 `NativePlayerActionScope$Ticket`（呼叫 `enterState` 的 javac 必寫；候選 ASM 未同步 InnerClasses） |
| `NativeDeItemDefinitionsSql.finish`／`$Lease.call` | try 標籤／ASTORE 槽 |
| `NativeEquipmentDefense.apply` | try 範圍差 1 個標籤 |
| `NativeProcHealingCompanion`／`$Dispatch`、`NativeProcHealingEncounter`／`$Dispatch` | ASTORE 槽與 try 表 |
| `NativePlayerActionScope.blocked`／`$Root.finish` | IFEQ 標籤與 try 表 |
| `NativeAcceptedHpCompletion.enqueue`、`NativeSourcePlayerActionLock.call` | ALOAD vs LABEL（控制流展開） |
| `NativeCombatAttacker.advance`、`NativeCombatOrigin.contextValid`、`NativeProcSkillCatalog.load`、`NativeRolePublication.allowPublication` | ASTORE 槽 |
| InnerClasses 微差 | `NativeLightAction`、`NativeMassTeleportAnswerLife`、`NativePlayerCommit`／`$CalcTicket`／`$Event`、`NativePlayerLifetime`、`NativePlayerVitalCompletions`、`NativeProcHealingPlayer`、`NativeCancellationNpcStatic` |

### D. 仍可用 Java 逼近、但本批未寫成與候選相同的行為差

| entry | 差異 | 理由 |
|---|---|---|
| `ClassicWorldMonsterPopulation.actionLocked` | `IFNE` vs `ALOAD 0` | 候選在鎖路徑有額外 ASM 呼叫；未整段以 CFR 覆寫 5k+ 行原始碼 |
| `ClassicWorldMonsterPopulation$Actor.snapshot` | `ARETURN` vs `ALOAD 0` | 候選 `snapshot()` 經 `NativeCombatOrigin.worldSnapshot(...)` |
| `LegacyInventoryWeightLoader.load` | SQL `LDC` | 候選 SQL 把 armor／weapon／etcitem 展開成中文欄位 subquery；`src/main` 用 `LegacyEquipmentMagicLoader.qualified`。改寫會讓正式版 schema 路徑倒退，需主控決定 |
| `LegacyMySqlRepository.<init>` | `Properties.load` vs `DUP2` | 候選在 try-with-resources 用 dup2 把 load 與 `UnifiedConfiguration.aliases` 串在一起；已加 aliases，stack 形狀仍不同 |
| `NativeCancellationBattleRuntime.replace` | Snapshot `INVOKESPECIAL` vs `ALOAD 23` | 候選把 `nativeSourceNpcFactoryMemory` 當第 19 引數；`BattleTimeAttackEncounter$Snapshot` 公開 ctor 只有 18 參且該欄 `final`，javac 無法在 B08 類別賦值 |
| `NativeCancellationNpcStatic$Target.getHeading` | `GameHub.classicWorldNpcHeading` vs `NativeStaticNpcScene.heading` | 已補 `instance` 欄位；其餘方法仍走 `src/main` hub API |
| `NativeReturnNatureTransfer.lambda$release$#0` | `ALOAD 3` vs `ALOAD 0` | lambda 捕獲編號；語意路徑仍是 `src/main` SUMMON 轉移 |

## 未驗證什麼

- 實機登入／戰鬥／退出。
- 把 staged 搬進 `src/main` 後的 `mvn test` 與 B-land 回歸（本批規定不適用）。
- 在 Windows 以 `lib/*`（`;` classpath）重跑 javac（本環境無 `lib/`）。
- 用 JDK 17 原廠 javac（非 21 的 `--release 17`）是否能讓 `emit` 變成 `invokespecial`。

## 產出路徑

- staged Java：`lito伺服器/LiTo381-Server/merge-inputs/50635f7e/staged/local/lito381/server/`（上列 30 檔）
- 本報告：`docs/reports/issue-69.md`
- 暫存（不入庫）：`scratch/issue-69/`（CFR、deps、strip_exceptions.py）、`/tmp/b08-out`、`/tmp/b08-staged.jar`、`/tmp/b08-semantic.json`

## 給下一棒的提示

1. **主控（Claude）**：裁定上文 A–D 的 46 筆 DIFF。若接受 javac-unreachable，B08 可標完成並在 B-land 前保持 staged；若要求 SAME=160，需要允許的不是 overlay，而是「record 額外欄位的凍結 class／改 T020d 正規化 InnerClasses／DUP／try 表」。
2. 不要用候選 class bytes 覆蓋 `/tmp/b08-out` 再宣告 SAME（#62 已禁）。
3. `LegacyMySqlRepository` 編譯需要 HikariCP；不要把 JAR 放進 git 的 `lib/`。
4. `NativeLightAction$Ingress` 等 record 額外欄位與 `NativeArmorState` 的 Ticket InnerClasses 是同一類問題：候選呼叫了後來才 splice 的型別／欄位，class 屬性沒跟 javac 規則走。
5. B-land 才把 staged 覆寫進 `src/main`；本批產物可直接給後續批次當 classpath 參考，但尚未等於候選。

## 已驗證／推測／待驗證

- **已驗證**：javac exit 0；160 class 與 verify_entries 畢氏相等；semantic-diff 計數 SAME=114 DIFF=46；未 overlay class bytes；未改 src/main／pom／REPORT.md。
- **推測**：A–C 類 DIFF 無法用 `--release 17` 的 javac 消掉；D 類仍可能再靠近，但會擴大相對 `src/main` 的行為翻轉（尤其 WeightLoader SQL 與 TamedRelease objectId 條件——後者已依候選 bytecode 寫入）。
- **待驗證**：主控對 46 筆的裁定；JDK 17 原廠編譯；B-land 回歸。
