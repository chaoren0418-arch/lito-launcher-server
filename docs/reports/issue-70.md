# Issue #70 / T020-B09 — 第 3 次修正，未達驗收

## 結論

**未完成，不可 B-land，不可宣告 362 SAME 或 T029 8 SAME。** 本輪有新的 Java 修正，並取代前次報告中「13 筆全是編譯器形狀、8 方法已保留」等未證實說法。

依 #102 主控裁定將 5 個內部 record 改為 `@AsRecord final class`，不自行實作 equals/hashCode/toString；補上候選額外欄位、accessor、建構子 overload 與生命週期呼叫。#102 在本輪查詢時仍 OPEN、尚未合併，沒有使用未核准的轉換器，也沒有任何 class 覆蓋／後處理。

原題 classpath 尚缺 `AsRecord`，不能宣告原題編譯命令通過。另以 #102 公開註解契約在 scratch 做**未轉換的編譯診斷**：javac 17 成功、輸出 362 entry、無缺漏／多出；原版 T020d 結果 **351 SAME、11 DIFF**。此數字不是最終驗收結果，也不表示剩下只有 11 個方法差異。

需 `needs:claude`：除了 #102 之外，已找到 T029 的 `SIPUSH 123`／javac `BIPUSH 123`、DUP2／SWAP 注入，以及 final 欄位 definite-assignment 的最小差異。這些不在目前唯一核准的 record 轉換範圍。另仍有普通方法未移植，不能將全部未完成事項歸因於 #102。

## 做了什麼

- 維持既有 `task/70`，不 checkout／建分支。初始 HEAD `5361545` 與遠端 `874e587` 分歧，採一般合併保留兩端歷史，再做修正；不 force push。
- 重新使用 CFR 0.152 對正式／候選 3 個外層與其內部類別反編譯；以既有 staged（基於 src/main）為底補差異。沒有整檔換成 CFR 輸出。
- `PendingSlaveTarget`／`PendingPetRename` 補 `private volatile Object sourceCompanionCallback`，前次報告稱其為 final 是錯的。
- `MonsterAiTick` 補 `sourceCombatOrigin`、`nativeSourceNpcPlayerOrigin`、`nativeSourceNpcTickView`，含 6 個候選建構子；`CompanionTick` 補 clock view／overload；`GroundTick` 補 ground proof／Pulse overload。3 個類別的 final 欄位初始化限制仍列為未解，見下節。
- 補指定完整簽名 `sourcePoisonBody$useInventoryItemBeforeTravel(byte[], CombatPoisonLifecycle.Scope)`：用原始可讀方法體逐一移植正式 clone 的帳號轉換、inventory replace、世界藥水允許條件、blue-potion 封包、poison scope／atomic／publication 呼叫；原入口改成候選的 scope wrapper。**方法存在不代表 SAME**。
- `exitRoomBeforePolyMe` 改採正式整個流程；`run` 補 enter/leave session 與 SelectionRecovery；原有連線處理本體移至 `sourceApplicationRun`，其完整候選一致性仍未完成。兩個 magic-doll 方法採正式封包欄位與數值。
- 移植候選短 wrapper、明確命名 helper、59 個 combatProbability helper、poison clone 相依方法與 NPC tick append；修復 CFR 的區域變數／泛型／條件賦值錯誤。逐方法 SAME 比基準增加 172，詳見下表；其餘移植未宣告等價。
- `QuestMonsterPoisonUpdate`／`TimeAttackPoisonUpdate` 補上真正缺漏的 `NativeSourceArenaPoison.attacker`，不是單純 compact constructor 形狀問題；候選的求值堆疊順序仍不同。

## 已驗證

JDK：`C:/Program Files/Microsoft/jdk-17.0.17.10-hotspot/bin`，javac `--release 17 -encoding UTF-8`。沒有啟動伺服器／遊戲，沒有連 DB。

| 輸入 | SHA-256 |
|---|---|
| 正式 `base-0.6.71.jar` | `e1498871c7be6b287821c57ef5d95933db1740d9b4d4a80f6c3465f21d99d498` |
| 候選 `candidate-50635f7e.jar` | `50635f7edca8ec6ce77787cc32371c7590b497e48ea202f8a679caf3f517b729` |
| CFR 0.152 | `f686e8f3ded377d7bc87d216a90e9e9512df4156e75b06c655a16648ae8765b2` |

候選在 classpath 前，正式專有類別由 base 提供；本機沒有 lib 目錄，保留 `lib/*` 在命令中。

| 驗證 | 接回前次提交，JDK 17 | 本輪未轉換診斷 |
|---|---:|---:|
| 編譯的 entry | 362 | 362 |
| 原版 T020d SAME / DIFF | 352 / 10 | 351 / 11 |
| missingLeft / extraLeft | 0 / 0 | 0 / 0 |
| 逐方法 SAME | 5594 | 5766 |
| 逐方法 DIFF | 755 | 632 |
| 逐方法 MISSING_LEFT | 300 | 251 |
| 逐方法 EXTRA_LEFT | 14 | 25 |

逐方法診斷器只在記憶體中將原版 `ClassComparer.compareMethods` 分別套用每個方法，不改比較規則、不寫 class、不替代整類驗收。表中包含方法旗標、exceptions、合成名稱、try/catch 與指令差異；SAME 並非行為回歸。251 個缺漏中，170 個名稱含 lambda、15 個是待 #102 生成的 ObjectMethods，剩 66 個列於附錄。新增／改名方法會改變 javac lambda helper 集合，故不能只比較 SAME 增量就稱整類改善。

### T029 八方法逐方法結果（對正式；全部已存在，仍有 5 DIFF）

| 方法 | 原版方法比較邏輯的診斷結果 |
|---|---|
| `exitRoom(Z)V` | SAME |
| `exitRoomBeforePolyMe(Z)V` | DIFF method exitRoomBeforePolyMe(Z)V insn[22]: left=INVOKESTATIC local/lito381/server/NativeResurrectionAnswerLife.capture(Llocal/lito381/server/ClientSession;Ljava/lang/Object;)V right=DUP2 |
| `run()V` | DIFF method run()V: left=TRY L0 L1 L2    right=TRY L0 L1 L2 java/lang/Throwable   |
| `sendRoomParticipant(JLlocal/lito381/server/ClientSession;Llocal/lito381/server/Protocol$LitoForce;Ljava/lang/String;)V` | SAME |
| `sourcePoisonBody$useInventoryItemBeforeTravel([BLlocal/lito381/server/CombatPoisonLifecycle$Scope;)V` | DIFF method sourcePoisonBody$useInventoryItemBeforeTravel([BLlocal/lito381/server/CombatPoisonLifecycle$Scope;)V insn[87]: left=ALOAD 0 right=INVOKEVIRTUAL local/lito381/server/ClientSession.accountName()Ljava/lang/String; |
| `litoRoomParticipant(JLjava/lang/String;Llocal/lito381/server/Protocol$LitoForce;IILlocal/lito381/server/GameStateStore$TournamentStatistics;Ljava/lang/String;IILlocal/lito381/server/Protocol$LitoParticipantState;ILlocal/lito381/server/GameStateStore$TournamentAwards;)[B` | SAME |
| `magicDollCustomIcon(IIZ)[B` | DIFF method magicDollCustomIcon(IIZ)[B insn[20]: left=BIPUSH 123 right=SIPUSH 123 |
| `magicDollIcon(I)[B` | DIFF method magicDollIcon(I)[B insn[15]: left=BIPUSH 123 right=SIPUSH 123 |

`ALLOW_METHODS=2` 是兩個外層類套用允許清單，不是只有兩個方法，也不是八個方法通過。官方整類比較仍可能在 InnerClasses 提前返回，以上八筆由附錄診斷器獨立核對。

### 需要主控裁定的最小差異

1. **正式圖示 opcode 載入**：`Protocol#magicDollIcon(I)[B` 第 15 個正規化指令、`magicDollCustomIcon(IIZ)[B` 第 20 個：javac `BIPUSH 123`，正式 `SIPUSH 123`。兩邊後續皆呼叫 `Writer.c(I)`。以 Java 寫數值 123 不能指定 javac 必須發 SIPUSH；現行 ClassComparer 不合併這兩種指令。這是可獨立重現、且與 record 無關的 T029 驗收阻礙。沒有改工具或 patch 產物來隱藏它。
2. **正式 clone 的堆疊插入**：`exitRoomBeforePolyMe` 在 `pendingResurrection=null` 前是 `ALOAD 0; ACONST_NULL; DUP2; capture; PUTFIELD`。Java 轉錄使用兩次取參數，診斷第一處在 insn[22]。道具 clone 的 SourceLogin 部分使用高位局部槽 ASTORE 34/33/32/35，poison 部分有 DUP/SWAP；當前 Java 保留呼叫與資料流，尚未重現指令形狀。
3. **record 額外 final 欄位**：候選 `CompanionTick(long, ClientSession)` 不初始化 `nativeSourceCompanionClockView`；另一 overload 先呼叫它再 PUTFIELD。`GroundTick` 相同；`MonsterAiTick` 舊 canonical ctor 不初始化 final transient tick view。javac 要求 final definite assignment；當前以明確 null 初始化舊 ctor，並在有值 overload 展開 component 賦值。這些是**已知不同指令**，不是 SAME；#102 規格禁止改其他方法，因此只套現有裁定仍無法消除此差異。欄位旗標未放寬。
4. **合成 helper**：例如 `combatProbability$0` 已有 Java body，但候選 access `0x100a`（private static synthetic），javac 明確宣告方法是 `0x000a`。Java 不能以修飾詞指定 ACC_SYNTHETIC；此項同樣不在 #102 規格。
5. **仍有內容工作**：66 個非 lambda／ObjectMethods 方法未補（附錄），以及既有長方法／建構子／lambda 呼叫目標的差異尚未逐一消除。不得據上述最小反例把所有 632 DIFF 當成編譯器問題。

原版整類 11 DIFF：ClientSession、ClientSession$1、PendingPetRename、PendingSlaveTarget、GameHub、CompanionTick、GroundTick、MonsterAiTick、QuestMonsterPoisonUpdate、TimeAttackPoisonUpdate、Protocol。五個 @AsRecord 類別尚未轉換，第一處是 ACC_RECORD 不同；其 constructor 差異亦須另外解決。ClientSession$1 比基準多一個 DIFF，與移植後 switch helper 集合改變有關，仍待補齊相應原 body，未獲豁免。

## 可直接重跑的命令

以下從指定 worktree 根目錄執行，不依賴未提交的移植腳本。使用全新 `scratch/issue-70/target/repro`；若已存在，腳本會停止，請將 `$out` 改為另一個全新名稱。原題編譯與診斷編譯分開記錄；診斷註解只作編譯期契約，未包含於 left.jar，不可拿它冒充 #102 已合併或 record 已轉換。

```powershell
$ErrorActionPreference = 'Stop'
$OutputEncoding = [Console]::OutputEncoding = [Text.UTF8Encoding]::new($false)
Set-Location 'D:\lineage開發\3-lito開發\scratch\wt\task-70'
$taskRoot = (Get-Location).Path
$server = Join-Path $taskRoot 'lito伺服器/LiTo381-Server'
$work = Join-Path $taskRoot 'scratch/issue-70'
$out = Join-Path $work 'target/repro'
if (Test-Path -LiteralPath $out) { throw 'target/repro 已存在；請使用另一個全新輸出目錄，避免混入舊 class。' }
New-Item -ItemType Directory -Force -Path $out | Out-Null
$utf8 = [Text.UTF8Encoding]::new($false)
$jdk = 'C:/Program Files/Microsoft/jdk-17.0.17.10-hotspot/bin'
$javac = Join-Path $jdk 'javac.exe'
$java = Join-Path $jdk 'java.exe'
$jar = Join-Path $jdk 'jar.exe'
$candidate = Join-Path $server 'reference/candidate-50635f7e.jar'
$formal = Join-Path $server 'reference/base-0.6.71.jar'
$batch = (Get-Content -Raw -Encoding UTF8 'docs/combat-merge/batches.json' | ConvertFrom-Json).batches | Where-Object id -eq B09
if ($batch.verify_entries.Count -ne 362) { throw 'B09 清單數不符' }
$entries = Join-Path $work 'verify_entries.txt'
[IO.File]::WriteAllLines($entries, [string[]]$batch.verify_entries, $utf8)
$staged = Join-Path $server 'merge-inputs/50635f7e/staged/local/lito381/server'
$sources = @('ClientSession','GameHub','Protocol') | ForEach-Object { Join-Path $staged "$_.java" }
$cp = "$candidate;$formal;$(Join-Path $server 'lib/*')"
# 原題命令：未合併 #102 時，這一步預期無法解析 AsRecord。保留實際輸出。
$strict = Join-Path $out 'strict'
New-Item -ItemType Directory -Path $strict | Out-Null
$ErrorActionPreference = 'Continue'
& $javac --release 17 -encoding UTF-8 -cp $cp -d $strict @sources *> (Join-Path $work 'strict-javac.log')
$strictCode = $LASTEXITCODE
$ErrorActionPreference = 'Stop'
Write-Output "strict javac exit=$strictCode"
# 診斷專用：只宣告 #102 公開的註解契約，不實作或執行轉換器。
$annotationSource = Join-Path $work 'annotation/AsRecord.java'
New-Item -ItemType Directory -Force -Path (Split-Path $annotationSource) | Out-Null
[IO.File]::WriteAllText($annotationSource, @'
package local.lito381.server;
@java.lang.annotation.Retention(java.lang.annotation.RetentionPolicy.CLASS)
@java.lang.annotation.Target(java.lang.annotation.ElementType.TYPE)
public @interface AsRecord { String[] components(); }
'@, $utf8)
$annotation = Join-Path $out 'annotation'
& $javac --release 17 -encoding UTF-8 -d $annotation $annotationSource
if ($LASTEXITCODE -ne 0) { throw 'annotation javac failed' }
$classes = Join-Path $out 'classes'
$ErrorActionPreference = 'Continue'
& $javac --release 17 -encoding UTF-8 -cp "$cp;$annotation" -d $classes @sources *> (Join-Path $work 'repro-javac.log')
$diagnosticCode = $LASTEXITCODE
$ErrorActionPreference = 'Stop'
if ($diagnosticCode -ne 0) { throw 'staged javac failed; see repro-javac.log' }
Write-Output 'diagnostic javac exit=0'
$leftJar = Join-Path $out 'left.jar'
& $jar cf $leftJar -C $classes .
if ($LASTEXITCODE -ne 0) { throw 'jar failed' }
$actual = @(& $jar tf $leftJar | Where-Object { $_.EndsWith('.class') })
$expected = @($batch.verify_entries | ForEach-Object { if ($_.EndsWith('.class')) { $_ } else { "$_.class" } })
if (Compare-Object $expected $actual) { throw 'entry 清單有缺漏或多出' }
Write-Output "entry count=$($actual.Count); missing=0; extra=0"
$asm = 'D:/dev-cache/maven/repository/org/ow2/asm/asm/9.7/asm-9.7.jar;D:/dev-cache/maven/repository/org/ow2/asm/asm-tree/9.7/asm-tree-9.7.jar'
$tool = Join-Path $out 'semantic-tool'
$toolSources = @(Get-ChildItem (Join-Path $server 'tools/semantic-diff/src/main/java/local/lito381/semanticdiff/*.java') | ForEach-Object FullName)
& $javac --release 17 -encoding UTF-8 -cp $asm -d $tool @toolSources
if ($LASTEXITCODE -ne 0) { throw 'semantic-diff javac failed' }
& $java -cp "$tool;$asm" local.lito381.semanticdiff.SemanticDiff --left $leftJar --right $candidate --classes $entries --allow-methods (Join-Path $taskRoot 'docs/combat-merge/t029-methods.txt') --allow-right $formal --json (Join-Path $work 'repro-diff.json') --md (Join-Path $work 'repro-diff.md')
if ($LASTEXITCODE -ne 0) { throw 'semantic-diff failed' }
# 從本報告附錄取得唯讀逐方法診斷器，呼叫原版 compareMethods，不寫回任何 class。
$report = Get-Content -Raw -Encoding UTF8 'docs/reports/issue-70.md'
$auditCode = [regex]::Match($report, '(?s)<!-- METHOD_AUDIT -->\s*```java\s*(.*?)\s*```').Groups[1].Value
if (-not $auditCode) { throw 'missing MethodAudit appendix' }
$auditPath = Join-Path $work 'MethodAudit.java'
[IO.File]::WriteAllText($auditPath, $auditCode, $utf8)
& $javac --release 17 -encoding UTF-8 -cp "$tool;$asm" -d $tool $auditPath
if ($LASTEXITCODE -ne 0) { throw 'MethodAudit javac failed' }
& $java -cp "$tool;$asm" local.lito381.semanticdiff.MethodAudit $leftJar $candidate $formal (Join-Path $taskRoot 'docs/combat-merge/t029-methods.txt') | Set-Content -Encoding UTF8 (Join-Path $work 'repro-methods.tsv')
if ($LASTEXITCODE -ne 0) { throw 'MethodAudit failed' }
Get-Content -Encoding UTF8 (Join-Path $work 'repro-methods.tsv') | Select-String "`tFORMAL`t"
```

本輪實測輸出（原題命令與未轉換診斷必須分開看）：

```text
strict javac exit=1: cannot find symbol AsRecord（5 處；#102 尚未合併）
diagnostic javac exit=0
Note: ClientSession.java uses unchecked or unsafe operations.
Note: Recompile with -Xlint:unchecked for details.
entry count=362; missing=0; extra=0
SUMMARY SAME=351 DIFF=11 ONLY_LEFT=0 ONLY_RIGHT=0 BYTE_SAME=3 ALLOW_METHODS=2
```

## 推測

- #102 合併後可解決 record 父類別／component／ObjectMethods 屬性，但不能據此推定 final constructor 或 T029 指令差異會消失。
- 某些指令雖可有相同行為，現有驗收是正規化指令相同；是否擴充正規化或另核准精確處理，必須由主控決策。本輪沒有擅自採用。

## 待驗證／未完成

- 原題 classpath 下直接編譯、#102 正式轉換與全 362 SAME。
- T029 剩餘 5 方法正式 SAME；其他方法全部候選 SAME。
- 附錄的缺漏方法、extra method、switch／lambda／constructor 與其餘逐方法 DIFF。
- 單元測試／回歸：依本批規定不適用，B-land 統一處理；本 staged 未執行。實機驗收亦未做。

## 產出路徑

提交僅 4 檔：

- `lito伺服器/LiTo381-Server/merge-inputs/50635f7e/staged/local/lito381/server/ClientSession.java`
- `lito伺服器/LiTo381-Server/merge-inputs/50635f7e/staged/local/lito381/server/GameHub.java`
- `lito伺服器/LiTo381-Server/merge-inputs/50635f7e/staged/local/lito381/server/Protocol.java`
- `docs/reports/issue-70.md`

本機可重建證據不進 git：`scratch/issue-70/target/repro/left.jar`、`strict-javac.log`、`repro-javac.log`、`repro-diff.json`、`repro-diff.md`、`repro-methods.tsv`、`verify_entries.txt`。反編譯原檔在 `scratch/issue-70/{candidate-src,formal-src}`；初次診斷與本輪中間結果亦在同一 scratch 目錄。沒有寫入原始專案資源目錄。

## 給下一棒的提示

1. Claude 主控先裁定上述具體 bytecode 差異與 #102 final constructor 規格缺口；此報告沒有請求通過剩餘普通方法缺口。
2. 本機執行器依裁定補 66 個普通缺漏與其餘方法；優先檢查 sourceApplicationRun、characterSelect、生命週期 constructor 與移植後 lambda helper 對應。用附錄逐方法診斷追蹤，整類驗收仍用原版 T020d。
3. #102 合併後只使用其唯一核准 CLI；不得重播其他 patch、覆蓋 class bytes 或私改比較器來通關。#102 可用不等於 B09 完成。
4. 全 362 SAME 且 T029 8 SAME 才交 B-land；本分支維持未完成。不要改 docs/REPORT.md。

## 附錄：仍缺少的 66 個普通／具名 helper 方法

此清單是本輪未轉換診斷中 MISSING_LEFT 扣掉名稱含 lambda 及 5 個 record ObjectMethods，並非斷言均不可表達。完整 251 筆可由 `repro-methods.tsv` 篩出。

```text
local/lito381/server/ClientSession#nativeAcceptedShockBattle(Llocal/lito381/server/BattleSpellCatalog$ShockStunSpell;Llocal/lito381/server/Protocol$AttackSpellRequest;Llocal/lito381/server/ClientSession;IILjava/util/List;)J
local/lito381/server/ClientSession#nativeAcceptedShockWorld(Llocal/lito381/server/BattleSpellCatalog$ShockStunSpell;Llocal/lito381/server/Protocol$AttackSpellRequest;Llocal/lito381/server/ClientSession;IIII)J
local/lito381/server/ClientSession#nativeAreaFreezeOriginalWorld([BLlocal/lito381/server/BattleSpellCatalog$FreezeAttackSpell;)V
local/lito381/server/ClientSession#nativeBattleControlledCancellationBody(Llocal/lito381/server/Protocol$ObjectSpellRequest;Llocal/lito381/server/ClientSession;Llocal/lito381/server/BattleCompanion;Llocal/lito381/server/BattleCompanion$Snapshot;Llocal/lito381/server/ClassicWorldMonsterPopulation$Snapshot;Llocal/lito381/server/BattleSpellCatalog$CancellationSpell;Llocal/lito381/server/CombatPoisonLifecycle$Scope;)V
local/lito381/server/ClientSession#nativeCompanionTargetuseClassicWorldAreaAttackSpell(Llocal/lito381/server/Protocol$AttackSpellRequest;Llocal/lito381/server/BattleSpellCatalog$AreaAttackSpell;)V
local/lito381/server/ClientSession#nativeCompanionTargetuseClassicWorldAttackSpell(Llocal/lito381/server/Protocol$AttackSpellRequest;Llocal/lito381/server/BattleSpellCatalog$AttackSpell;)V
local/lito381/server/ClientSession#nativeCompanionTargetuseClassicWorldLineAttackSpell([BLlocal/lito381/server/BattleSpellCatalog$LineAttackSpell;)V
local/lito381/server/ClientSession#nativeFreezeAcceptedOriginalWorld([BLlocal/lito381/server/BattleSpellCatalog$FreezeAttackSpell;)V
local/lito381/server/ClientSession#nativeLightCommit(Llocal/lito381/server/BattleSpellCatalog$LightSpell;Ljava/lang/Object;J)Ljava/lang/Integer;
local/lito381/server/ClientSession#nativeLightTail(Llocal/lito381/server/BattleSpellCatalog$LightSpell;ZI)V
local/lito381/server/ClientSession#nativeMaterialGroundBodyCommit(Llocal/lito381/server/RuntimeInventory;ILlocal/lito381/server/BattleSpellCatalog$GroundSpell;JI)V
local/lito381/server/ClientSession#nativeMaterialMassTeleportBody([BLlocal/lito381/server/BattleSpellCatalog$MassTeleportSpell;)V
local/lito381/server/ClientSession#nativeMaterialTargetNegativeBody([BLlocal/lito381/server/BattleSpellCatalog$TargetNegativeStatusSpell;ILlocal/lito381/server/ClientSession$TargetNegativeStatusKind;)V
local/lito381/server/ClientSession#nativeMaterialWorldGroundBodyCommit(Llocal/lito381/server/RuntimeInventory;ILlocal/lito381/server/BattleSpellCatalog$GroundSpell;JI)V
local/lito381/server/ClientSession#nativeNpcWeaponCompanionBody(Llocal/lito381/server/ClassicWorldMonsterPopulation$Attack;J)V
local/lito381/server/ClientSession#nativeNpcWeaponDspyBody(Llocal/lito381/server/ClassicWorldMonsterPopulation$Attack;J)V
local/lito381/server/ClientSession#nativeNpcWeaponMonsterBody(Llocal/lito381/server/ClassicWorldMonsterPopulation$Attack;)V
local/lito381/server/ClientSession#nativeNpcWeaponPhysicalBody(Llocal/lito381/server/ClassicWorldMonsterPopulation$PhysicalSkillAttack;)V
local/lito381/server/ClientSession#nativeRawOriginal_useLightningWand(Llocal/lito381/server/RuntimeInventory$Item;Llocal/lito381/server/Protocol$TargetLocationInventoryUse;)V
local/lito381/server/ClientSession#nativeShockPlayerBattleBody([BLlocal/lito381/server/BattleSpellCatalog$ShockStunSpell;)V
local/lito381/server/ClientSession#nativeShockPlayerOriginalBattle([BLlocal/lito381/server/BattleSpellCatalog$ShockStunSpell;)V
local/lito381/server/ClientSession#nativeShockPlayerOriginalWorld([BLlocal/lito381/server/BattleSpellCatalog$ShockStunSpell;)V
local/lito381/server/ClientSession#nativeShockPlayerWorldBody([BLlocal/lito381/server/BattleSpellCatalog$ShockStunSpell;)V
local/lito381/server/ClientSession#nativeSourceCompanionClockAction(Llocal/lito381/server/BattleCompanion;JJLjava/lang/Object;)V
local/lito381/server/ClientSession#nativeSourceCompanionClockTick(JJLjava/lang/Object;)V
local/lito381/server/ClientSession#nativeSourceCompanionFirstAction(Llocal/lito381/server/BattleCompanion;JJLjava/lang/Object;Ljava/lang/Object;)V
local/lito381/server/ClientSession#nativeSourceControlledOriginal_attack(Llocal/lito381/server/Protocol$AttackRequest;Llocal/lito381/server/ClientSession$AttackEnvelope;)V
local/lito381/server/ClientSession#nativeSourceControlledOriginal_nativeAreaFreezeOriginalBattle([BLlocal/lito381/server/BattleSpellCatalog$FreezeAttackSpell;)V
local/lito381/server/ClientSession#nativeSourceControlledOriginal_nativeFreezeAcceptedOriginalBattle([BLlocal/lito381/server/BattleSpellCatalog$FreezeAttackSpell;)V
local/lito381/server/ClientSession#nativeSourceControlledOriginal_nativeSourceLightningOriginalBody(Llocal/lito381/server/Protocol$AttackSpellRequest;Llocal/lito381/server/BattleSpellCatalog$DirectionalLineSpell;)V
local/lito381/server/ClientSession#nativeSourceControlledOriginal_useAreaAttackSpell(Llocal/lito381/server/Protocol$AttackSpellRequest;Llocal/lito381/server/BattleSpellCatalog$AreaAttackSpell;)V
local/lito381/server/ClientSession#nativeSourceControlledOriginal_useAttackSpell(Llocal/lito381/server/Protocol$AttackSpellRequest;Llocal/lito381/server/BattleSpellCatalog$AttackSpell;IIII)V
local/lito381/server/ClientSession#nativeSourceControlledOriginal_useCenteredAreaAttackSpell([BLlocal/lito381/server/BattleSpellCatalog$AreaAttackSpell;)V
local/lito381/server/ClientSession#nativeSourceControlledOriginal_useHealingSpell([BLlocal/lito381/server/BattleSpellCatalog$HealingSpell;)V
local/lito381/server/ClientSession#nativeSourceCubeTimeCommitted(Llocal/lito381/server/ClientSession;Llocal/lito381/server/GameHub$TimeAttackActorHit;ILjava/lang/String;ILlocal/lito381/server/NativeSourceCubeNpc$Commit;)I
local/lito381/server/ClientSession#nativeSourceDollDetachedFinalFrame([BLjava/lang/String;Ljava/lang/Object;)V
local/lito381/server/ClientSession#nativeSourceDollOriginalOrderedAttackProcs(Llocal/lito381/server/NativeMagicDollEffects$AttackProc;JLlocal/lito381/server/ClientSession;Llocal/lito381/server/BattleQuestEncounter$Snapshot;Llocal/lito381/server/BattleTimeAttackEncounter$Snapshot;Llocal/lito381/server/SourceNpcPhysicalStats;)V
local/lito381/server/ClientSession#nativeSourceEquipmentFinalFrame([BLjava/lang/String;Ljava/lang/Object;)V
local/lito381/server/ClientSession#nativeSourceFatalityOriginalArea(Llocal/lito381/server/BattleDeathmatchFatalityCatalog$Effect;I)V
local/lito381/server/ClientSession#nativeSourceFatalityOriginalSingle(Llocal/lito381/server/BattleDeathmatchFatalityCatalog$Effect;)V
local/lito381/server/ClientSession#nativeSourceFireWeaponOriginalBody([BLlocal/lito381/server/BattleSpellCatalog$FireWeaponSpell;)V
local/lito381/server/ClientSession#nativeSourceGroupLos$397(Llocal/lito381/server/ClientSession$Phase;Llocal/lito381/server/ClassicWorldMapCatalog$ClassicMap;IILlocal/lito381/server/ArenaMapCatalog$Arena;Llocal/lito381/server/ClientSession;Llocal/lito381/server/BattleSpellCatalog$GroupHealingSpell;Llocal/lito381/server/ClientSession;)Z
local/lito381/server/ClientSession#nativeSourceGroupLos$401(Llocal/lito381/server/ClientSession$Phase;Llocal/lito381/server/ClassicWorldMapCatalog$ClassicMap;IILlocal/lito381/server/ArenaMapCatalog$Arena;Llocal/lito381/server/ClientSession;Llocal/lito381/server/BattleSpellCatalog$GroupHealingSpell;Llocal/lito381/server/BattleCompanion$Snapshot;)Z
local/lito381/server/ClientSession#nativeSourceHealingOriginalGroup([BLlocal/lito381/server/BattleSpellCatalog$GroupHealingSpell;)V
local/lito381/server/ClientSession#nativeSourceHealingOriginalNature([BLlocal/lito381/server/BattleSpellCatalog$NaturesBlessingSpell;I)V
local/lito381/server/ClientSession#nativeSourceHpOriginalTimeDamage(JILjava/lang/String;Llocal/lito381/server/NativeSourceNpcDamageDisplay$Receipt;)I
local/lito381/server/ClientSession#nativeSourceIcons1FinalFrame([BLjava/lang/String;Ljava/lang/Object;)V
local/lito381/server/ClientSession#nativeSourceNpcHpFinalFrame([BLjava/lang/String;Ljava/lang/Object;)V
local/lito381/server/ClientSession#nativeSourcePhysicalTimeCommitted(Llocal/lito381/server/ClientSession;Llocal/lito381/server/GameHub$TimeAttackActorHit;ILjava/lang/String;ILlocal/lito381/server/NativeSourcePhysicalNpc$Commit;)I
local/lito381/server/ClientSession#nativeSourcePoisonTimeCommitted(Llocal/lito381/server/ClientSession;Llocal/lito381/server/GameHub$TimeAttackActorHit;ILjava/lang/String;ILlocal/lito381/server/NativeSourcePoisonNpc$Commit;)I
local/lito381/server/ClientSession#nativeSourceProc68FinalFrame([BLjava/lang/String;Ljava/lang/Object;)V
local/lito381/server/ClientSession#nativeSourceProc68IconFinalFrame([BLjava/lang/String;Ljava/lang/Object;)V
local/lito381/server/ClientSession#nativeSourceRemainingOriginal133([BLlocal/lito381/server/BattleSpellCatalog$ElementalFallDownSpell;I)V
local/lito381/server/ClientSession#nativeSourceRemainingOriginal18([BLlocal/lito381/server/BattleSpellCatalog$TurnUndeadSpell;)V
local/lito381/server/ClientSession#nativeSourceRemainingOriginal39([BLlocal/lito381/server/BattleSpellCatalog$ManaDrainSpell;)V
local/lito381/server/ClientSession#nativeSourceWeaponStaticFrame([BLjava/lang/String;Ljava/lang/Object;)V
local/lito381/server/ClientSession#nativeSourceWindShotOriginalBody([BLlocal/lito381/server/BattleSpellCatalog$WindShotSpell;)V
local/lito381/server/ClientSession#nativeStaticOriginalDialog(J)V
local/lito381/server/ClientSession#nativeStaticOriginalFrame([BLjava/lang/String;)V
local/lito381/server/ClientSession#nativeStaticOriginalMove(IIIII)V
local/lito381/server/ClientSession#sourceDamagePoisonClock(JZZLlocal/lito381/server/BattleSpellCatalog$PoisonSpell;Llocal/lito381/server/CombatPoisonLifecycle$Scope;)V
local/lito381/server/ClientSession#sourceNpcPoisonState(JIIIJLjava/lang/String;)Llocal/lito381/server/CombatPoisonAtomic$NpcState;
local/lito381/server/ClientSession#sourcePlayerBattleCommit(ILlocal/lito381/server/ClientSession;ZZJLlocal/lito381/server/BattleJoyOfPainStatus$Lease;I)V
local/lito381/server/ClientSession#sourcePlayerWorldCommit(DJLlocal/lito381/server/CombatPoisonLifecycle$Scope;Llocal/lito381/server/ClassicWorldMonsterPopulation$Snapshot;Llocal/lito381/server/BattleAbilityStatus$IllusionAvatar;I)Llocal/lito381/server/ClientSession$ClassicWorldFatalContext;
local/lito381/server/ClientSession#sourcePoisonBody$characterSelect([BZLlocal/lito381/server/CombatPoisonLifecycle$Scope;)V
local/lito381/server/ClientSession#sourceProcTimeCommitted(Llocal/lito381/server/GameHub$TimeAttackActorHit;ILjava/lang/String;ILlocal/lito381/server/NativeProcEncounterPublication$Context;)I
```

## 附錄：唯讀逐方法診斷器

此程式依 repo 既有 package-private 工具讀取 JAR，以反射呼叫**未修改**的 `compareMethods`，不寫 JAR 或 class。獨立方法結果用於追查，不能替代包含類別屬性的正式驗收。

<!-- METHOD_AUDIT -->
```java
package local.lito381.semanticdiff;
import java.nio.file.*;
import java.util.*;
import java.lang.reflect.Method;
import org.objectweb.asm.tree.*;

/** Read-only diagnostic: invoke the unchanged comparison logic for each method.
 * No class is serialized, rewritten, or used as an acceptance artifact. */
public class MethodAudit {
    public static void main(String[] args) throws Exception {
        JarIndex left = JarIndex.load(Path.of(args[0]));
        JarIndex candidate = JarIndex.load(Path.of(args[1]));
        JarIndex formal = JarIndex.load(Path.of(args[2]));
        Set<String> allowed = new HashSet<>(Files.readAllLines(Path.of(args[3])));
        Method compare = ClassComparer.class.getDeclaredMethod("compareMethods", ClassNode.class, ClassNode.class, ClassNode.class, Set.class, JarIndex.class, JarIndex.class, JarIndex.class);
        compare.setAccessible(true);
        for (String owner : left.classes.keySet()) {
            ClassNode l = left.readTree(owner), c = candidate.readTree(owner), f = formal.readTree(owner);
            if (c == null) continue;
            Map<String,MethodNode> lm = map(l,left), cm = map(c,candidate), fm = f == null ? Map.of() : map(f,formal);
            Set<String> keys = new TreeSet<>(lm.keySet()); keys.addAll(cm.keySet());
            for (String a : allowed) if (a.startsWith(owner + "#")) keys.add(a.substring(owner.length()+1));
            for (String key : keys) {
                boolean allow = allowed.contains(owner + "#" + key);
                MethodNode a = lm.get(key), b = (allow ? fm : cm).get(key);
                String status;
                if (a == null) status = "MISSING_LEFT";
                else if (b == null) status = "EXTRA_LEFT";
                else {
                    ClassNode la = new ClassNode(), rb = new ClassNode();
                    la.name = rb.name = owner;
                    la.methods = List.of(a); rb.methods = List.of(b);
                    Object diff = compare.invoke(null, la, rb, null, Set.of(), left, allow ? formal : candidate, null);
                    status = diff == null ? "SAME" : "DIFF " + ((ClassComparer.FirstDiff) diff).oneLine();
                }
                System.out.println(owner + "#" + key + "\t" + (allow ? "FORMAL" : "CANDIDATE") + "\t" + status);
            }
        }
    }
    static Map<String,MethodNode> map(ClassNode node, JarIndex jar) {
        Map<String,MethodNode> out = new TreeMap<>();
        for(MethodNode m:node.methods) out.put(jar.remapMethod(node.name,m.name)+m.desc,m);
        return out;
    }
}
```
