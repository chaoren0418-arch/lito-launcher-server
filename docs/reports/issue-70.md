# Issue #70 / T020-B09 — 依 #102、#132、#153、#166 續修

## 結論

**本批仍未達驗收，維持待修。** 本輪完整重編譯並使用已合併的工具驗證，得到 **357 SAME、5 DIFF、362 個 entry 齊全**。`Protocol` 及其全部內部類別共 **162 個 entry 全 SAME**，5 個 `@AsRecord` 類別也全部 SAME。

T029 為 **5 SAME、1 SAME_EFFECTS、2 DIFF**。已補入原先缺漏的 35 個普通方法，仍有 31 個普通方法缺漏，以及外層類別的欄位、建構子、lambda 與其他逐方法差異。已補入的方法亦有尚待校正者，逐方法結果見下文。

主控仍須裁定本報告的 SWAP、private 呼叫 opcode、核准 effects 的 ASTORE 範圍與 checked-exception metadata 差異。普通方法移植工作另列，應繼續由本機執行器完成。整批通過前保留於 staged。

## 做了什麼

- 起始分支為既有 `task/70`，起始 HEAD 為 `e480f77b7d60347f53dd22cd42b066fbe27908f3`。採用前次三方移植成果續修。
- 分支中的工具仍停留舊版，因此從本機 `origin/main` 的固定提交 `450178d59b0157fddf2b9456b7c03d20207993fc` 匯出工具與核准清單到 scratch，作為本輪驗證依賴。該提交已包含 #102、#132、#153、#166。
- 以 CFR 0.152 重新反編譯正式與候選的 ClientSession、GameHub、Protocol 共 6 份來源，逐 bytes 核對與前次反編譯來源一致。依正式／候選差異修改既有 staged 複本。
- `MonsterAiTick`、`CompanionTick`、`GroundTick` 依 #102 使用 `finalFields`，還原候選建構鏈及欄位賦值順序。連同 `PendingSlaveTarget`、`PendingPetRename`，5 個 record 經官方轉換器後全 SAME。
- `Protocol` 補正 `effectObject`、`movementLockRelease`、`questMonster`、`rangedPhysicalAttack`、`showMap`、`timeAttackActor` 的候選邏輯與控制流程。含 3 個 T029 正式方法在內，整類及內部類別均通過。
- `GameHub` 補正登入帳號轉換、channel member 查找、companion clock 入口與 5 個 material placement 方法的 synchronized 旗標。
- `ClientSession` 補入 35 個指定簽名，包括 group LOS、poison clock/state、companion clock/action、NPC weapon、frame publication、ground commit、time damage commit 等方法。`nativeLightCommit` 改成完整 Integer 回傳與資源消耗流程，`nativeLightTail` 補齊封包與狀態流程。
- `exitRoomBeforePolyMe` 使用具體型別保留正式呼叫與欄位寫入順序，通過 SAME_EFFECTS。道具方法的 handoff 改為先評估 hub、accountName、stateCharacter、rentalTownInventory，再執行 SourceLogin.account，保留正式的呼叫求值順序。其暫存寫入順序仍有差異。
- CFR 輸出曾出現泛型、變數重用、未初始化與不可達區段等編譯問題。可明確重建者已修正，其餘列為缺漏並保留在 scratch 診斷紀錄。

## 已驗證

驗證使用 Microsoft OpenJDK 17.0.17，`javac --release 17 -encoding UTF-8`。執行範圍為編譯、record 轉換與唯讀 JAR 比較。

| 輸入／產物 | SHA-256 |
|---|---|
| `正式 base-0.6.71.jar` | `e1498871c7be6b287821c57ef5d95933db1740d9b4d4a80f6c3465f21d99d498` |
| `候選 candidate-50635f7e.jar` | `50635f7edca8ec6ce77787cc32371c7590b497e48ea202f8a679caf3f517b729` |
| `CFR 0.152` | `f686e8f3ded377d7bc87d216a90e9e9512df4156e75b06c655a16648ae8765b2` |
| `本輪 final-readable.jar` | `ac0fc97126c9a159c3f84d1cf1b56f952670a71b7452f7d5ae2f59a53d48244e` |

編譯 classpath 依序為 candidate、base、`lib/*`，另加入從上述固定提交的原始註解編出的 annotation support 目錄。support 提供 `AsRecord`／`AsmFinal` 的編譯契約，本批 left JAR 的 362 個 entry 全部來自本批 3 個 Java 檔，轉換只使用官方 `local.lito381.asrecord.AsRecord`。

### 實測輸出

```text
javac exit=0
Note: ClientSession.java uses unchecked or unsafe operations.
Note: Recompile with -Xlint:unchecked for details.
as-record: scanned=362 converted=5
as-record exit=0
entries=362 missing=0 extra=0
SUMMARY SAME=357 SAME_EFFECTS=0 SAME_LAYOUT=0 DEVIATION=0 DIFF=5 ONLY_LEFT=0 ONLY_RIGHT=0 BYTE_SAME=3 ALLOW_METHODS=2
semantic-diff exit=0
```

類別計數中的 SAME_EFFECTS 為 0，是因 ClientSession 整類仍 DIFF。官方 JSON 的 `effectsMethods` 已獨立列出 `exitRoomBeforePolyMe` 為 SAME_EFFECTS。`ALLOW_METHODS=2` 計算套用清單的外層類別數。

| 驗證 | 本輪修改前，同一新版工具 | 本輪修改後 |
|---|---:|---:|
| 類別 SAME / DIFF | 353 / 9 | 357 / 5 |
| 缺漏／多出 entry | 0 / 0 | 0 / 0 |
| 逐方法 SAME | 5743 | 5793 |
| 逐方法 SAME_EFFECTS | 0 | 1 |
| 逐方法 DIFF | 535 | 523 |
| 逐方法 MISSING_LEFT | 146 | 107 |
| 逐方法 EXTRA_LEFT | 9 | 7 |
| 普通方法缺漏 | 66 | 31 |

逐方法診斷由附錄 `CurrentAudit` 呼叫同一版 `ClassComparer.compareMethod`，保留本側 classpath、lambda 對應、T029 精確方法與核准 effects。診斷用來展開整類第一處差異之後的待修項目。整批驗收依上列官方 CLI 與完整 entry 檢查。

### T029 八方法

| 方法 | 結果 |
|---|---|
| ClientSession `exitRoom(Z)V` | SAME |
| ClientSession `exitRoomBeforePolyMe(Z)V` | SAME_EFFECTS |
| ClientSession `run()V` | DIFF，private 呼叫 opcode |
| ClientSession `sendRoomParticipant(J…ClientSession…)V` | SAME |
| ClientSession `sourcePoisonBody$useInventoryItemBeforeTravel([B…Scope;)V` | DIFF，effects ASTORE 順序 |
| Protocol `litoRoomParticipant(J…TournamentAwards;)[B` | SAME |
| Protocol `magicDollIcon(I)[B` | SAME |
| Protocol `magicDollCustomIcon(IIZ)[B` | SAME |

### 五個 DIFF entry 的第一處差異

| 類別 | 第一處差異 |
|---|---|
| `local/lito381/server/ClientSession` | `nativeArmorStateExpiresAt:J`，only left |
| `local/lito381/server/ClientSession$1` | `<clinit>()V`，left=TRY L13 L14 L15 java/lang/NoSuchFieldError   right=TRY L12 L13 L14 java/lang/NoSuchFieldError   |
| `local/lito381/server/GameHub` | `<init>(Llocal/lito381/server/ServerConfig;Llocal/lito381/server/GameStateStore;Llocal/lito381/server/AccountRepository;)V`，left=PUTFIELD local/lito381/server/GameHub.deletingCharacters:Ljava/util/Set; right=PUTFIELD local/lito381/server/GameHub.connectedSessions:Ljava/util/Set; |
| `local/lito381/server/GameHub$QuestMonsterPoisonUpdate` | `<init>(Llocal/lito381/server/BattleQuestEncounter$PoisonUpdate;Llocal/lito381/server/ClientSession;)V`，left=ALOAD 1 right=ALOAD 2 |
| `local/lito381/server/GameHub$TimeAttackPoisonUpdate` | `<init>(Llocal/lito381/server/BattleTimeAttackEncounter$PoisonUpdate;Llocal/lito381/server/ClientSession;Llocal/lito381/server/BattleRules$Resolution;)V`，left=ALOAD 1 right=ALOAD 2 |

## 需要主控裁定的具體差異

### GameHub 兩個 poison record 的 SWAP

完整簽名如下。

```text
local/lito381/server/GameHub$QuestMonsterPoisonUpdate#<init>(Llocal/lito381/server/BattleQuestEncounter$PoisonUpdate;Llocal/lito381/server/ClientSession;)V
local/lito381/server/GameHub$TimeAttackPoisonUpdate#<init>(Llocal/lito381/server/BattleTimeAttackEncounter$PoisonUpdate;Llocal/lito381/server/ClientSession;Llocal/lito381/server/BattleRules$Resolution;)V
```

第一個 constructor 的完整有效指令如下，第二個另有 resolution 欄位賦值。

```text
共同前綴
ALOAD 0
INVOKESPECIAL java/lang/Record.<init>()V
ALOAD 0
ALOAD 1
PUTFIELD GameHub$QuestMonsterPoisonUpdate.update
ALOAD 0

left
ALOAD 1
ALOAD 2

candidate
ALOAD 2
ALOAD 1
SWAP

共同後綴
INVOKESTATIC NativeSourceArenaPoison.attacker(Object,ClientSession)ClientSession
PUTFIELD GameHub$QuestMonsterPoisonUpdate.attacker
RETURN
```

此片段的引數與寫入一致。#166 的 `TransportComparison.java` 第 44–46 行對 SWAP 直接拒絕進行 transport 證明，故官方仍 DIFF。需主控決定受控支援方式。

### T029 run 的 private 呼叫 opcode

`ClientSession#run()V` 第 5 個正規化指令，left 為 `INVOKEVIRTUAL ClientSession.sourceApplicationRun()V`，正式為 `INVOKESPECIAL`。兩側被呼叫的方法皆為 private instance method。#153 的 H5/H7 正規化適用 bootstrap method handle，普通呼叫保留 opcode。這個差異也需要另行裁定。`sourceApplicationRun` 本體還有獨立內容差異須續修。

整個 run 的有效指令比較另有兩處 DUP／重複 ALOAD 差異，全部差異如下。

```diff
-INVOKEVIRTUAL ClientSession.sourceApplicationRun()V
+INVOKESPECIAL ClientSession.sourceApplicationRun()V

 ALOAD 0
+DUP
 INVOKESTATIC SelectionRecovery.finish(ClientSession)V
-ALOAD 0
 INVOKESTATIC NativeServerLifetime.leaveSession(ClientSession)V
```

後一段分別出現在正常路徑與 Throwable handler。完整指令差異另存 `scratch/issue-70/run-normalized.diff`。

### 道具方法的 ASTORE

正式 `sourcePoisonBody$useInventoryItemBeforeTravel` 的 rental handoff 先讀 hub、呼叫 accountName、呼叫 stateCharacter、讀 rentalTownInventory，接著連續四次 ASTORE，再呼叫 SourceLogin.account 與 stageRentalBattleHandoff。

本輪 Java 以四個具名暫存保留上述求值與呼叫次序，javac 在每個求值之後立即 ASTORE。官方 effects 第一處為 `effects sequence[43] left=ASTORE right=INVOKEVIRTUAL ClientSession.accountName()`。#153 的單次使用暫存規則遇到中間另一個 STORE 就停止，這種四個暫存排列仍待主控裁定。通過該段後仍需重跑完整方法，其他差異保留待驗證。

### nativeLightCommit 的 checked exception

候選 `nativeLightCommit(L…LightSpell;Ljava/lang/Object;J)Ljava/lang/Integer;` 的 Exceptions 為空，方法體呼叫宣告 `throws IOException` 的 `commitSpellReady`。Java 實作加入 `throws IOException` 才能編譯，官方目前在 Exceptions 屬性回報 DIFF。此方法已具備完整消耗與狀態邏輯，metadata 的還原方式需主控裁定。

## 推測

- 部分其餘方法的差異可能仍與 CFR 的暫存、try 範圍、lambda 形狀有關，需逐方法查證。
- SWAP 與 private 呼叫的局部片段可單獨分析，無法由這些片段推論 ClientSession 或 GameHub 的其餘方法已等價。

## 待驗證／未完成

- 31 個普通方法缺漏，完整簽名見附錄。
- 523 個逐方法 DIFF、76 個 lambda 內容識別缺漏、7 個 extra method，以及整類欄位差異。逐方法數量包含 T029 與已補入但仍需校正的方法。
- ClientSession 的額外 `nativeArmorStateExpiresAt` 等欄位、兩個 constructor、`sourceApplicationRun` 與 switch helper 對應。
- GameHub constructor 初始化順序與其他尚未通過的方法。
- 362 個 entry 的整批通過，以及 T029 的完整通過。
- 單元測試與執行回歸依本批規格由 B-land 統一處理，真機驗收留待使用者執行。

## 產出路徑

本輪提交 4 檔。

```text
lito伺服器/LiTo381-Server/merge-inputs/50635f7e/staged/local/lito381/server/ClientSession.java
lito伺服器/LiTo381-Server/merge-inputs/50635f7e/staged/local/lito381/server/GameHub.java
lito伺服器/LiTo381-Server/merge-inputs/50635f7e/staged/local/lito381/server/Protocol.java
docs/reports/issue-70.md
```

本機證據位於 `scratch/issue-70/`，包括 `final-readable-{javac,record,semantic}.log`、`final-readable-diff.{json,md}`、`final-readable-methods.tsv`、`target/final-readable.jar`、`cfr-recheck.json`、`current-tools/source-ref.txt`。`verify_current.py`、`bootstrap_current.py`、`recheck_cfr.py`、`CurrentAudit.java` 與移植診斷腳本亦在同處，供本機續修使用。

## 給下一棒的提示

1. Claude 主控處理以上新增的具體比較邊界，建議標為 `needs:claude`。目前分支仍有實作工作，應維持待修狀態。
2. 本機執行器續補附錄 31 個方法及逐方法差異。CFR 不完整輸出與編譯診斷保留於 scratch，優先恢復可讀的原始方法再移植 ASM 呼叫。
3. 使用固定新版工具與本側 candidate／base classpath 重跑。逐方法 audit 與整類 CLI 需要分別檢查，existing PR #95 的 Maven CI 另有自己的驗證範圍。
4. 全部通過後再交 B-land。主控可由本報告與本輪新增提交辨識實際進度。

## 可重跑的完整編譯與官方驗收

以下從 worktree 根目錄執行，使用全新的 `scratch/issue-70/review-repro`。工具取自固定 Git 提交，所有生成檔都留在 worktree scratch。ASM 9.7 的 SHA 依該版 `build-support.sh` 核對。

```powershell
$ErrorActionPreference = 'Stop'
$root = (Get-Location).Path
$w = Join-Path $root 'scratch/issue-70/review-repro'
if (Test-Path $w) { throw '請使用全新輸出路徑' }
New-Item -ItemType Directory -Path $w | Out-Null
$ref = '450178d59b0157fddf2b9456b7c03d20207993fc'
$serverRel = 'lito伺服器/LiTo381-Server'
$server = Join-Path $root $serverRel
$zip = Join-Path $w 'tools.zip'
git archive --format=zip "--output=$zip" $ref -- "$serverRel/tools/semantic-diff" "$serverRel/tools/as-record" "$serverRel/src/main/java/local/lito381/server/AsRecord.java" "$serverRel/src/main/java/local/lito381/server/AsmFinal.java" docs/combat-merge
if ($LASTEXITCODE) { throw 'git archive failed' }
Expand-Archive -LiteralPath $zip -DestinationPath "$w/tool-inputs"
$toolServer = Join-Path "$w/tool-inputs" $serverRel
$jdk = 'C:/Program Files/Microsoft/jdk-17.0.17.10-hotspot/bin'
$utf8 = [Text.UTF8Encoding]::new($false)
$dep = "$w/target/dependency"
New-Item -ItemType Directory -Force -Path $dep | Out-Null
$hashes = @{
  'asm' = 'adf46d5e34940bdf148ecdd26a9ee8eea94496a72034ff7141066b3eea5c4e9d'
  'asm-tree' = '62f4b3bc436045c1acb5c3ba2d8ec556ec3369093d7f5d06c747eb04b56d52b1'
  'asm-analysis' = '7bc6bcbc21379948a0c8c467fb0f864206e5b818f6bc0b546872f5c9f941556f'
}
foreach ($art in $hashes.Keys) {
  $p = "$dep/$art-9.7.jar"
  $cached = "D:/dev-cache/maven/repository/org/ow2/asm/$art/9.7/$art-9.7.jar"
  if (Test-Path $cached) { Copy-Item -LiteralPath $cached -Destination $p }
  else { Invoke-WebRequest "https://repo.maven.apache.org/maven2/org/ow2/asm/$art/9.7/$art-9.7.jar" -OutFile $p }
  if ((Get-FileHash $p -Algorithm SHA256).Hash.ToLowerInvariant() -ne $hashes[$art]) { throw 'ASM SHA mismatch' }
}
$asm = (@('asm','asm-tree','asm-analysis') | ForEach-Object { "$dep/$_-9.7.jar" }) -join ';'
$tool = "$w/target/tool"
$toolSources = @(Get-ChildItem "$toolServer/tools/semantic-diff/src/main/java" -Recurse -Filter '*.java').FullName
$toolSources += @(Get-ChildItem "$toolServer/tools/as-record/src/main/java" -Recurse -Filter '*.java').FullName
& "$jdk/javac.exe" --release 17 -encoding UTF-8 -cp $asm -d $tool @toolSources
if ($LASTEXITCODE) { throw 'tool javac failed' }
$annotations = "$w/target/annotations"
$annotationSources = @(Get-ChildItem "$toolServer/src/main/java/local/lito381/server" -Filter '*.java').FullName
& "$jdk/javac.exe" --release 17 -encoding UTF-8 -d $annotations @annotationSources
if ($LASTEXITCODE) { throw 'annotation javac failed' }
$candidate = "$server/reference/candidate-50635f7e.jar"
$formal = "$server/reference/base-0.6.71.jar"
$classes = "$w/target/classes"
$sources = @('ClientSession','GameHub','Protocol') | ForEach-Object { "$server/merge-inputs/50635f7e/staged/local/lito381/server/$_.java" }
& "$jdk/javac.exe" --release 17 -encoding UTF-8 -cp "$candidate;$formal;$server/lib/*;$annotations" -d $classes @sources
if ($LASTEXITCODE) { throw 'staged javac failed' }
& "$jdk/java.exe" -cp "$tool;$asm" local.lito381.asrecord.AsRecord $classes
if ($LASTEXITCODE) { throw 'AsRecord failed' }
$left = "$w/target/left.jar"
& "$jdk/jar.exe" cf $left -C $classes .
if ($LASTEXITCODE) { throw 'jar failed' }
$batch = (Get-Content -Raw -Encoding UTF8 docs/combat-merge/batches.json | ConvertFrom-Json).batches | Where-Object id -eq B09
[IO.File]::WriteAllLines("$w/entries.txt", [string[]]$batch.verify_entries, $utf8)
$actual = @(& "$jdk/jar.exe" tf $left | Where-Object { $_.EndsWith('.class') })
if ($actual.Count -ne 362 -or (Compare-Object $batch.verify_entries $actual)) { throw 'entry mismatch' }
$effectLines = Get-Content -Encoding UTF8 "$w/tool-inputs/docs/combat-merge/effects-approved.txt" | Where-Object { $_.StartsWith('local/lito381/server/ClientSession#') }
[IO.File]::WriteAllLines("$w/effects.txt", [string[]]$effectLines, $utf8)
& "$jdk/java.exe" -Xmx3g -cp "$tool;$asm" local.lito381.semanticdiff.SemanticDiff --left $left --right $candidate --left-classpath $candidate --left-classpath $formal --classes "$w/entries.txt" --allow-methods "$root/docs/combat-merge/t029-methods.txt" --allow-right $formal --effects-methods "$w/effects.txt" --json "$w/result.json" --md "$w/result.md"
if ($LASTEXITCODE) { throw 'semantic-diff CLI failed' }
(Get-Content -Raw -Encoding UTF8 "$w/result.json" | ConvertFrom-Json).counts
```

## 附錄一　仍缺少的 31 個普通方法

```text
local/lito381/server/ClientSession#nativeAreaFreezeOriginalWorld([BLlocal/lito381/server/BattleSpellCatalog$FreezeAttackSpell;)V
local/lito381/server/ClientSession#nativeBattleControlledCancellationBody(Llocal/lito381/server/Protocol$ObjectSpellRequest;Llocal/lito381/server/ClientSession;Llocal/lito381/server/BattleCompanion;Llocal/lito381/server/BattleCompanion$Snapshot;Llocal/lito381/server/ClassicWorldMonsterPopulation$Snapshot;Llocal/lito381/server/BattleSpellCatalog$CancellationSpell;Llocal/lito381/server/CombatPoisonLifecycle$Scope;)V
local/lito381/server/ClientSession#nativeCompanionTargetuseClassicWorldAreaAttackSpell(Llocal/lito381/server/Protocol$AttackSpellRequest;Llocal/lito381/server/BattleSpellCatalog$AreaAttackSpell;)V
local/lito381/server/ClientSession#nativeCompanionTargetuseClassicWorldAttackSpell(Llocal/lito381/server/Protocol$AttackSpellRequest;Llocal/lito381/server/BattleSpellCatalog$AttackSpell;)V
local/lito381/server/ClientSession#nativeCompanionTargetuseClassicWorldLineAttackSpell([BLlocal/lito381/server/BattleSpellCatalog$LineAttackSpell;)V
local/lito381/server/ClientSession#nativeFreezeAcceptedOriginalWorld([BLlocal/lito381/server/BattleSpellCatalog$FreezeAttackSpell;)V
local/lito381/server/ClientSession#nativeMaterialMassTeleportBody([BLlocal/lito381/server/BattleSpellCatalog$MassTeleportSpell;)V
local/lito381/server/ClientSession#nativeMaterialTargetNegativeBody([BLlocal/lito381/server/BattleSpellCatalog$TargetNegativeStatusSpell;ILlocal/lito381/server/ClientSession$TargetNegativeStatusKind;)V
local/lito381/server/ClientSession#nativeRawOriginal_useLightningWand(Llocal/lito381/server/RuntimeInventory$Item;Llocal/lito381/server/Protocol$TargetLocationInventoryUse;)V
local/lito381/server/ClientSession#nativeShockPlayerBattleBody([BLlocal/lito381/server/BattleSpellCatalog$ShockStunSpell;)V
local/lito381/server/ClientSession#nativeShockPlayerOriginalBattle([BLlocal/lito381/server/BattleSpellCatalog$ShockStunSpell;)V
local/lito381/server/ClientSession#nativeShockPlayerOriginalWorld([BLlocal/lito381/server/BattleSpellCatalog$ShockStunSpell;)V
local/lito381/server/ClientSession#nativeShockPlayerWorldBody([BLlocal/lito381/server/BattleSpellCatalog$ShockStunSpell;)V
local/lito381/server/ClientSession#nativeSourceControlledOriginal_attack(Llocal/lito381/server/Protocol$AttackRequest;Llocal/lito381/server/ClientSession$AttackEnvelope;)V
local/lito381/server/ClientSession#nativeSourceControlledOriginal_nativeAreaFreezeOriginalBattle([BLlocal/lito381/server/BattleSpellCatalog$FreezeAttackSpell;)V
local/lito381/server/ClientSession#nativeSourceControlledOriginal_nativeFreezeAcceptedOriginalBattle([BLlocal/lito381/server/BattleSpellCatalog$FreezeAttackSpell;)V
local/lito381/server/ClientSession#nativeSourceControlledOriginal_nativeSourceLightningOriginalBody(Llocal/lito381/server/Protocol$AttackSpellRequest;Llocal/lito381/server/BattleSpellCatalog$DirectionalLineSpell;)V
local/lito381/server/ClientSession#nativeSourceControlledOriginal_useAreaAttackSpell(Llocal/lito381/server/Protocol$AttackSpellRequest;Llocal/lito381/server/BattleSpellCatalog$AreaAttackSpell;)V
local/lito381/server/ClientSession#nativeSourceControlledOriginal_useAttackSpell(Llocal/lito381/server/Protocol$AttackSpellRequest;Llocal/lito381/server/BattleSpellCatalog$AttackSpell;IIII)V
local/lito381/server/ClientSession#nativeSourceControlledOriginal_useCenteredAreaAttackSpell([BLlocal/lito381/server/BattleSpellCatalog$AreaAttackSpell;)V
local/lito381/server/ClientSession#nativeSourceControlledOriginal_useHealingSpell([BLlocal/lito381/server/BattleSpellCatalog$HealingSpell;)V
local/lito381/server/ClientSession#nativeSourceFireWeaponOriginalBody([BLlocal/lito381/server/BattleSpellCatalog$FireWeaponSpell;)V
local/lito381/server/ClientSession#nativeSourceHealingOriginalGroup([BLlocal/lito381/server/BattleSpellCatalog$GroupHealingSpell;)V
local/lito381/server/ClientSession#nativeSourceHealingOriginalNature([BLlocal/lito381/server/BattleSpellCatalog$NaturesBlessingSpell;I)V
local/lito381/server/ClientSession#nativeSourceRemainingOriginal133([BLlocal/lito381/server/BattleSpellCatalog$ElementalFallDownSpell;I)V
local/lito381/server/ClientSession#nativeSourceRemainingOriginal18([BLlocal/lito381/server/BattleSpellCatalog$TurnUndeadSpell;)V
local/lito381/server/ClientSession#nativeSourceRemainingOriginal39([BLlocal/lito381/server/BattleSpellCatalog$ManaDrainSpell;)V
local/lito381/server/ClientSession#nativeSourceWindShotOriginalBody([BLlocal/lito381/server/BattleSpellCatalog$WindShotSpell;)V
local/lito381/server/ClientSession#nativeStaticOriginalDialog(J)V
local/lito381/server/ClientSession#sourcePlayerWorldCommit(DJLlocal/lito381/server/CombatPoisonLifecycle$Scope;Llocal/lito381/server/ClassicWorldMonsterPopulation$Snapshot;Llocal/lito381/server/BattleAbilityStatus$IllusionAvatar;I)Llocal/lito381/server/ClientSession$ClassicWorldFatalContext;
local/lito381/server/ClientSession#sourcePoisonBody$characterSelect([BZLlocal/lito381/server/CombatPoisonLifecycle$Scope;)V
```

## 附錄二　唯讀逐方法診斷器

把下列 Java 存到 scratch 的 `CurrentAudit.java`，以驗收工具的 classes 與 ASM classpath 編譯。執行引數依序為 left JAR、candidate JAR、formal JAR、T029 清單、本批 effects 清單。程式只讀 JAR 並輸出逐方法 TSV。

```java
package local.lito381.semanticdiff;

import java.nio.file.*;
import java.util.*;
import org.objectweb.asm.tree.*;

/** Read-only per-method diagnostics using the unchanged official comparer. */
public class CurrentAudit {
    public static void main(String[] args) throws Exception {
        JarIndex left = JarIndex.load(Path.of(args[0]));
        JarIndex candidate = JarIndex.load(Path.of(args[1]));
        JarIndex formal = JarIndex.load(Path.of(args[2]));
        left.addClasspath(Path.of(args[1]));
        left.addClasspath(Path.of(args[2]));
        Set<String> allowed = lines(args[3]), effects = lines(args[4]);
        Map<String, Set<String>> exact = new HashMap<>();
        for (String s : allowed) exact.computeIfAbsent(s.substring(0, s.indexOf('#')), k -> new HashSet<>()).add(s.substring(s.indexOf('#') + 1));
        for (JarIndex j : List.of(left, candidate, formal)) j.preserveExactMethods(exact);
        for (String owner : left.classes.keySet()) {
            ClassNode l = left.readTree(owner), c = candidate.readTree(owner), f = formal.readTree(owner);
            if (c == null) continue;
            Map<String, MethodNode> lm = map(l, left), cm = map(c, candidate), fm = f == null ? Map.of() : map(f, formal);
            Set<String> keys = new TreeSet<>(lm.keySet()); keys.addAll(cm.keySet());
            keys.addAll(exact.getOrDefault(owner, Set.of()));
            for (String key : keys) {
                boolean allow = allowed.contains(owner + "#" + key);
                boolean effect = effects.contains(owner + "#" + key);
                MethodNode a = lm.get(key), b = (allow ? fm : cm).get(key);
                String status;
                if (a == null) status = "MISSING_LEFT";
                else if (b == null) status = "EXTRA_LEFT";
                else {
                    var diff = ClassComparer.compareMethod(key, a, b, left, allow ? formal : candidate, effect);
                    status = diff == null ? (effect ? "SAME_EFFECTS" : "SAME") : "DIFF " + diff.oneLine();
                }
                System.out.println(owner + "#" + key + "\t" + (allow ? "FORMAL" : "CANDIDATE") + "\t" + status);
            }
        }
    }
    static Set<String> lines(String path) throws Exception {
        Set<String> out = new HashSet<>();
        for (String s : Files.readAllLines(Path.of(path))) {
            s = s.replace("\ufeff", "").strip();
            if (!s.isEmpty() && !s.startsWith("#")) out.add(s.split(" #", 2)[0].strip());
        }
        return out;
    }
    static Map<String, MethodNode> map(ClassNode node, JarIndex jar) {
        Map<String, MethodNode> out = new TreeMap<>();
        for (MethodNode m : node.methods) out.put(jar.remapMethod(node.name, m.name, m.desc) + m.desc, m);
        return out;
    }
}
```
