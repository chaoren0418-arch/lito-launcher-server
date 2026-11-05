# Issue #65 — T020-B04 戰鬥併入暫存

## 結論

**B04 已通過本批編譯及語意驗收：98 entries = 95 SAME＋1 SAME_PATHS＋1 SAME_CFG＋1 DEVIATION；DIFF、ONLY_LEFT、ONLY_RIGHT、missing、extra 全部為 0。** 驗證日期為 2026-11-06（Asia/Taipei；工具時間 2026-11-05T20:53:02Z）。

依[主控裁定](https://github.com/chaoren0418-arch/lito-launcher-server/issues/65#issuecomment-6662562240)，保留 NpcEscape 的 Java 原樣，使用已合併 #203 的新版工具證明兩個含迴圈方法為 SAME_CFG。沒有新增豁免。既有 PhysicalNpc 兩條 method-diff 仍為核准偏離，不宣稱為 SAME。

## 做了什麼／已驗證

- 沿用 worktree 與 `task/65`，起點 `6fbbe7a`；fetch 後無衝突合併 `origin/main` 的 `567b0cb7762864b10ef876f90749e74a66e9bb01`，merge commit 為 `2269166833cad6876abf6d289fc2da2f93e6e764`。沒有 checkout／新建分支。
- #203 經 [PR #215](https://github.com/chaoren0418-arch/lito-launcher-server/pull/215) 合併，包含第二次 stack／CFG 修正。工具來源 Git tree：semantic-diff `8f2964bbd6eb65b442348f1f3fff433adb80fad5`；as-record `025d66c3be040820866e9e9ac9876600168c6f5e`。本輪重新編譯工具，沒有使用舊工具 classes。
- B04 的 51 份 Java 與起點完全相同，保留原有 12 個 asm-only entry 的 Java 移植、三個 `@AsRecord` 及 Commit 的 `@AsmFinal`。本輪自身只修改本報告；main 帶入的其他批次、工具與索引屬上游變更。`docs/REPORT.md` 與 main 一致，未手動修改。
- 全新輸出目錄 `lito伺服器/LiTo381-Server/target/issue-65/recheck-203/`：先編註解與兩套工具，再以候選、正式 base、註解 classes 及 lib 作 classpath，一次編譯全部 51 份 staged Java。產出全部 98 個 class 後執行 as-record，再原樣打包整個 classes 目錄。
- 沒有複製候選 class bytes、替換或改名驗收產物。轉換前後逐 class SHA-256 核對，只有核准轉換器改寫以下四個 entry，且四個最後皆 SAME：`NativeSourceNpcFactoryType$Stamp`、`NativeSourceNpcPlayer$Scope`、`NativeSourcePhysicalNpc$Origin`、`NativeSourcePhysicalNpc$Commit`。
- 掃描 `docs/combat-merge/` 八份核准 txt：只有 `deviations.txt` 的 PhysicalNpc `sourceCurrent`／`targetCurrent` 兩條適用 B04，其餘七份匹配為 0。精確裁切兩條供 CLI 使用，未擴大或修改核准清單。

### 編譯與完整性

Windows、Microsoft OpenJDK 17.0.17、ASM／asm-tree／asm-analysis 9.7；`javac --release 17 -encoding UTF-8`。唯一編譯提示為既有 `NativeSourceNpcPlayerView.java` unchecked／unsafe operations；PowerShell 日誌包裝為 NativeCommandError，但 javac 實際 exit=0，整個驗證腳本 exit=0。

```text
javac exit=0 sources=51
as-record: scanned=98 converted=4
class inventory total=98 missing=0 extra=0
SUMMARY SAME=95 SAME_EFFECTS=0 SAME_PATHS=1 ORPHAN_IGNORED=0 SAME_CFG=1 LAMBDA_PAIRED_BY_CALLSITE=0 VISIBILITY_BRIDGE=0 SAME_LAYOUT=0 DEVIATION=1 DIFF=0 ONLY_LEFT=0 ONLY_RIGHT=0 BYTE_SAME=0 ALLOW_METHODS=0
classSelection: missingLeft=[] extraLeft=[]
proofMethodCounts: SAME_CFG=2 LAMBDA_PAIRED_BY_CALLSITE=0 VISIBILITY_BRIDGE=0
```

實際編譯目錄的 class inventory 與 `batches.json` B04 的 `verify_entries` 精確相等；JSON 的 `comparedClassCount` 與逐 entry 結果亦皆為 98。驗收依據為結果與清單核對，不只是 CLI exit code。

| 輸入／產物 | SHA-256 |
|---|---|
| candidate-50635f7e.jar | `50635f7edca8ec6ce77787cc32371c7590b497e48ea202f8a679caf3f517b729` |
| base-0.6.71.jar | `e1498871c7be6b287821c57ef5d95933db1740d9b4d4a80f6c3465f21d99d498` |
| recheck-203/b04.jar | `ac5e21b26180b296dab457173b52cace3c0dfca63b250c8bc793859f77c80834` |

JAR 雜湊包含打包時間，重建後以 entry 完整性與語意結果驗收。

### 六個指定方法

| 類別／方法 | 本輪實測 | 證據／依據 |
|---|---|---|
| NativeSourceNpcDamageDisplay.questDamage | SAME_PATHS | `path multisets equal (36)`；#176 |
| NativeSourceNpcDamageDisplay.timeDamage | SAME_PATHS | `path multisets equal (36)`；#176 |
| NativeSourceNpcEscape.nativeSourceFirstViewHandled | SAME_CFG | `entry-rooted labelled CFGs equal`；#203 |
| NativeSourceNpcEscape.nativeSourceNpcPlayerFirstHandled | SAME_CFG | `entry-rooted labelled CFGs equal`；#203 |
| NativeSourcePhysicalNpc.sourceCurrent | DEVIATION | 既有精確 `method-diff`；#176 |
| NativeSourcePhysicalNpc.targetCurrent | DEVIATION | 既有精確 `method-diff`；#176 |

上表是方法數，摘要是 class 數。DamageDisplay 保留離開 monitor 後才呼叫 acceptedNatural 的時序；NpcEscape 兩個原 DIFF 已由合併後工具的完整 CFG 證明消除。

PhysicalNpc 核准源自[主控裁定](https://github.com/chaoren0418-arch/lito-launcher-server/issues/65#issuecomment-6655613888)。候選 consumer 對非負 int 直接 IRETURN，Java boolean 用 `state != 0`；負值均走 fallback。指定候選的 producer javap 證明已收錄於 [deviations.md](../combat-merge/deviations.md)：RawNpc sourceState 的正常返回為 -1／0／1，GroundNpc targetState 亦同，因此直接返回路徑僅 0／1。候選 SHA 與前次證明相同；本輪沒有重做執行期測試。完整本輪偏離內容保存在 semantic JSON／Markdown。

## 推測

沒有以推測替代本批驗收。SAME_CFG 為工具實測證明；兩條 DEVIATION 僅適用已核准的指定候選與完整方法描述子，不外推至其他版本。

## 待驗證／未驗證什麼

- 本批未動 src/main，依設計 §6-5 不執行單元測試／行為回歸；由 B-land 統一處理，包含 PhysicalNpc producer 各回傳 0、1 時兩個 consumer 與候選一致的測試。
- 未啟動伺服器、登入器或遊戲，未連正式 DB，未讀機密設定、封存或 releases；沒有部署。
- PR 審查與合併尚待 Claude／使用者處理；推送後 CI 由調度器檢查。本批通過不代表 B-land 或整體戰鬥合併已完成。

## 產出路徑

- 追蹤產出：本報告 `docs/reports/issue-65.md`；PR 保留 B04 的 51 份 `merge-inputs/50635f7e/staged/local/lito381/server/*.java`。
- 可重建產物：`lito伺服器/LiTo381-Server/target/issue-65/recheck-203/`（annotations、semantic、converter、classes、b04.jar）。
- 本機證據：`scratch/issue-65/recheck-203.ps1`、`recheck-203.log`、`recheck-203-semantic.json`／`.md`、`recheck-203-deviations.txt`、`entries.txt`。scratch／target 不提交。
- PR #87 描述同步為本輪最終驗收結果。舊輪歷史由 git 保留，本報告不再把 #203 列為阻塞。

## 給下一棒的提示

由 Claude 審查 PR #87；本輪無未核准差異，不需新增豁免或重寫 NpcEscape。使用者合併後由調度器解除 B04 依賴。B-land（#138）仍須保留三個 AsRecord、Commit 的 AsmFinal、兩條 PhysicalNpc method-diff 與對應回傳值測試；整批落地編譯後執行轉換器及全部 B entries 的語意比對。

## 可重現指令

在本 worktree 根目錄執行下列腳本；輸出目錄必須全新，重跑請換 `target/issue-65/` 下另一個名稱。ASM 依賴均核對固定 SHA-256。工具來源使用上述合併提交，完整核准清單先按 B04 篩選，僅兩條 deviations 適用。

```powershell
$ErrorActionPreference = 'Stop'
$root = (Get-Location).Path
$server = Join-Path $root 'lito伺服器/LiTo381-Server'
$evidence = Join-Path $root 'scratch/issue-65'
$output = Join-Path $server 'target/issue-65/recheck-203'
if (Test-Path -LiteralPath $output) { throw '請使用全新的驗證輸出目錄' }
$classes = Join-Path $output 'classes'
$support = Join-Path $output 'annotations'
$semantic = Join-Path $output 'semantic'
$converter = Join-Path $output 'converter'
New-Item -ItemType Directory -Force $evidence,$classes,$support,$semantic,$converter | Out-Null
$jdk = 'C:/Program Files/Microsoft/jdk-17.0.17.10-hotspot/bin'
$candidate = Join-Path $server 'reference/candidate-50635f7e.jar'
$base = Join-Path $server 'reference/base-0.6.71.jar'
$depDir = Join-Path $server 'target/issue-65/dependency'
New-Item -ItemType Directory -Force $depDir | Out-Null
$hashes = @{
    'asm' = 'adf46d5e34940bdf148ecdd26a9ee8eea94496a72034ff7141066b3eea5c4e9d'
    'asm-tree' = '62f4b3bc436045c1acb5c3ba2d8ec556ec3369093d7f5d06c747eb04b56d52b1'
    'asm-analysis' = '7bc6bcbc21379948a0c8c467fb0f864206e5b818f6bc0b546872f5c9f941556f'
}
foreach ($artifact in $hashes.Keys) {
    $path = Join-Path $depDir "$artifact-9.7.jar"
    if (!(Test-Path -LiteralPath $path)) {
        Invoke-WebRequest "https://repo.maven.apache.org/maven2/org/ow2/asm/$artifact/9.7/$artifact-9.7.jar" -OutFile $path
    }
    if ((Get-FileHash $path -Algorithm SHA256).Hash.ToLower() -ne $hashes[$artifact]) {
        throw "ASM SHA-256 mismatch $artifact"
    }
}
$deps = @('asm','asm-tree','asm-analysis') | ForEach-Object { Join-Path $depDir "$_-9.7.jar" }
$deps = $deps -join ';'
$batch = (Get-Content -Raw -Encoding UTF8 docs/combat-merge/batches.json | ConvertFrom-Json).batches | Where-Object id -eq B04
$approved = @(Get-Content -Encoding UTF8 docs/combat-merge/deviations.txt | Where-Object { $_ -match '^local/lito381/server/NativeSourcePhysicalNpc#(?:sourceCurrent|targetCurrent)\(' })
if ($approved.Count -ne 2) { throw 'expected exactly two approved methods' }
[IO.File]::WriteAllLines((Join-Path $evidence 'recheck-203-deviations.txt'), [string[]]$approved, [Text.UTF8Encoding]::new($false))
$sources = @($batch.classes | ForEach-Object { Join-Path $server "merge-inputs/50635f7e/staged/local/lito381/server/$_.java" })
$entries = Join-Path $evidence 'entries.txt'
[IO.File]::WriteAllLines($entries, [string[]]$batch.verify_entries, [Text.UTF8Encoding]::new($false))
& "$jdk/javac.exe" --release 17 -encoding UTF-8 -d $support "$server/src/main/java/local/lito381/server/AsRecord.java" "$server/src/main/java/local/lito381/server/AsmFinal.java"
if ($LASTEXITCODE) { throw 'annotation javac failed' }
$semanticSources = @(Get-ChildItem "$server/tools/semantic-diff/src/main/java" -Recurse -Filter '*.java' | ForEach-Object FullName)
& "$jdk/javac.exe" --release 17 -encoding UTF-8 -cp $deps -d $semantic $semanticSources
if ($LASTEXITCODE) { throw 'semantic tool javac failed' }
$converterSources = @(Get-ChildItem "$server/tools/as-record/src/main/java" -Recurse -Filter '*.java' | ForEach-Object FullName)
& "$jdk/javac.exe" --release 17 -encoding UTF-8 -cp $deps -d $converter $converterSources
if ($LASTEXITCODE) { throw 'converter javac failed' }
& "$jdk/javac.exe" --release 17 -encoding UTF-8 -cp "$candidate;$base;$server/lib/*;$support" -d $classes $sources
if ($LASTEXITCODE) { throw 'B04 javac failed' }
Write-Output 'javac exit=0 sources=51'
$before = @{}
Get-ChildItem $classes -Recurse -Filter '*.class' | ForEach-Object { $before[$_.FullName] = (Get-FileHash $_.FullName -Algorithm SHA256).Hash }
& "$jdk/java.exe" '-Dfile.encoding=UTF-8' -cp "$converter;$deps" local.lito381.asrecord.AsRecord $classes
if ($LASTEXITCODE) { throw 'conversion failed' }
$changed = @(Get-ChildItem $classes -Recurse -Filter '*.class' | Where-Object { $before[$_.FullName] -ne (Get-FileHash $_.FullName -Algorithm SHA256).Hash } | ForEach-Object Name)
Write-Output ('converter changed=' + ($changed -join ','))
$actual = @(Get-ChildItem $classes -Recurse -Filter '*.class' | ForEach-Object { $_.FullName.Substring($classes.Length + 1).Replace('\','/') })
$missing = @($batch.verify_entries | Where-Object { $_ -notin $actual })
$extra = @($actual | Where-Object { $_ -notin $batch.verify_entries })
Write-Output "class inventory total=$($actual.Count) missing=$($missing.Count) extra=$($extra.Count)"
if ($missing.Count -or $extra.Count) { throw 'entry inventory mismatch' }
$jar = Join-Path $output 'b04.jar'
& "$jdk/jar.exe" --create --file $jar -C $classes .
if ($LASTEXITCODE) { throw 'jar failed' }
& "$jdk/java.exe" '-Dfile.encoding=UTF-8' -cp "$semantic;$deps" local.lito381.semanticdiff.SemanticDiff --left $jar --right $candidate --left-classpath $candidate --left-classpath $base --classes $entries --deviations "$evidence/recheck-203-deviations.txt" --json "$evidence/recheck-203-semantic.json" --md "$evidence/recheck-203-semantic.md"
if ($LASTEXITCODE) { throw 'semantic tool failed' }
Get-FileHash $candidate,$base,$jar -Algorithm SHA256 | Format-Table -AutoSize
$result = Get-Content -Raw -Encoding UTF8 "$evidence/recheck-203-semantic.json" | ConvertFrom-Json
$result.counts | ConvertTo-Json -Compress
$result.classSelection | ConvertTo-Json -Compress

$bad = @($result.classes | Where-Object { $_.status -in @('DIFF','ONLY_LEFT','ONLY_RIGHT') })
if ($result.counts.DIFF -or $result.counts.ONLY_LEFT -or $result.counts.ONLY_RIGHT -or $bad.Count) { throw 'semantic acceptance failed' }
$result.proofMethodCounts | ConvertTo-Json -Compress
```

## 全部 98 個 entry 的本輪結果

以下名稱省略共同前綴 `local/lito381/server/` 及副檔名 `.class`。

| Entry | 狀態 |
|---|---|
| `NativeSourceLightningArea` | SAME |
| `NativeSourceLightningHp` | SAME |
| `NativeSourceLightningHp$Linked` | SAME |
| `NativeSourceLightningLifetime` | SAME |
| `NativeSourceLightningLifetime$Aim` | SAME |
| `NativeSourceLightningLifetime$Output` | SAME |
| `NativeSourceMagicNpc` | SAME |
| `NativeSourceMagicNpc$After` | SAME |
| `NativeSourceMagicNpc$Npc` | SAME |
| `NativeSourceMagicNpc$Snapshot` | SAME |
| `NativeSourceManualScope` | SAME |
| `NativeSourceMassSelfTarget` | SAME |
| `NativeSourceMassSlowTargets` | SAME |
| `NativeSourceMassSlowTargets$Aim` | SAME |
| `NativeSourceMassSlowTargets$Output` | SAME |
| `NativeSourceMassTeleportExit` | SAME |
| `NativeSourceMassTeleportExit$Proof` | SAME |
| `NativeSourceMaterialFirstLife` | SAME |
| `NativeSourceMaterialMain` | SAME |
| `NativeSourceMaterialPairGuard` | SAME |
| `NativeSourceMaterialPairGuard$Guarded` | SAME |
| `NativeSourceMaterialPairIngress` | SAME |
| `NativeSourceMonsterHp` | SAME |
| `NativeSourceMonsterHp$Aim` | SAME |
| `NativeSourceMonsterHp$Cache` | SAME |
| `NativeSourceMonsterHp$Viewer` | SAME |
| `NativeSourceNegativeFailure` | SAME |
| `NativeSourceNegativeFinalFrame` | SAME |
| `NativeSourceNegativeFinalFrame$Scope` | SAME |
| `NativeSourceNegativeFinalFrame$Value` | SAME |
| `NativeSourceNegativePairFirst` | SAME |
| `NativeSourceNegativePairFirst$First` | SAME |
| `NativeSourceNegativePhase` | SAME |
| `NativeSourceNegativeSelf` | SAME |
| `NativeSourceNegativeTargets` | SAME |
| `NativeSourceNegativeTargets$Aim` | SAME |
| `NativeSourceNegativeTargets$Output` | SAME |
| `NativeSourceNoneTargetRequest` | SAME |
| `NativeSourceNpcActionIntervals` | SAME |
| `NativeSourceNpcBaseSpeeds` | SAME |
| `NativeSourceNpcDamageDisplay` | SAME_PATHS |
| `NativeSourceNpcDamageDisplay$Proof` | SAME |
| `NativeSourceNpcDamageDisplay$Receipt` | SAME |
| `NativeSourceNpcEscape` | SAME_CFG |
| `NativeSourceNpcFactoryMemory` | SAME |
| `NativeSourceNpcFactoryType` | SAME |
| `NativeSourceNpcFactoryType$Stamp` | SAME |
| `NativeSourceNpcFirstLife` | SAME |
| `NativeSourceNpcFirstView` | SAME |
| `NativeSourceNpcFirstView$View` | SAME |
| `NativeSourceNpcHpOutput` | SAME |
| `NativeSourceNpcHpOutput$Accepted` | SAME |
| `NativeSourceNpcHpOutput$Input` | SAME |
| `NativeSourceNpcHpOutput$Output` | SAME |
| `NativeSourceNpcHpOutput$Viewer` | SAME |
| `NativeSourceNpcMagicDirect` | SAME |
| `NativeSourceNpcMagicDirect$Mutation` | SAME |
| `NativeSourceNpcMagicEffects` | SAME |
| `NativeSourceNpcMagicEffects$After` | SAME |
| `NativeSourceNpcMagicEffects$Output` | SAME |
| `NativeSourceNpcMagicEffects$Source` | SAME |
| `NativeSourceNpcMotion` | SAME |
| `NativeSourceNpcMotion$Motion` | SAME |
| `NativeSourceNpcMotion$Saved` | SAME |
| `NativeSourceNpcMovement` | SAME |
| `NativeSourceNpcOrigins` | SAME |
| `NativeSourceNpcOrigins$Master` | SAME |
| `NativeSourceNpcPlayer` | SAME |
| `NativeSourceNpcPlayer$Origin` | SAME |
| `NativeSourceNpcPlayer$Scope` | SAME |
| `NativeSourceNpcPlayerView` | SAME |
| `NativeSourceNpcPlayerView$Builder` | SAME |
| `NativeSourceNpcReaction` | SAME |
| `NativeSourceNpcSpellTiming` | SAME |
| `NativeSourceNpcSpellTiming$Data` | SAME |
| `NativeSourceNpcSpellTiming$Route` | SAME |
| `NativeSourceNpcTargetFailure` | SAME |
| `NativeSourceNpcTargetFailure$Cursor` | SAME |
| `NativeSourceNpcTickView` | SAME |
| `NativeSourceNpcViewAction` | SAME |
| `NativeSourceObjectTargetRequest` | SAME |
| `NativeSourceOtherControls` | SAME |
| `NativeSourceOtherControls$Snapshot` | SAME |
| `NativeSourceOuterPair` | SAME |
| `NativeSourcePairAdmission` | SAME |
| `NativeSourcePairThird` | SAME |
| `NativeSourcePairedScope` | SAME |
| `NativeSourcePhysicalAudience` | SAME |
| `NativeSourcePhysicalAudience$Audience` | SAME |
| `NativeSourcePhysicalAudience$Receiver` | SAME |
| `NativeSourcePhysicalNpc` | DEVIATION |
| `NativeSourcePhysicalNpc$Commit` | SAME |
| `NativeSourcePhysicalNpc$Origin` | SAME |
| `NativeSourcePhysicalNpc$Output` | SAME |
| `NativeSourcePhysicalNpc$Spawn` | SAME |
| `NativeSourcePhysicalNpc$Ticket` | SAME |
| `NativeSourcePlayerFactories` | SAME |
| `NativeSourcePoisonAudience` | SAME |
