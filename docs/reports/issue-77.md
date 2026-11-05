# Issue #77 — A06 正式 0.6.71 暫存重驗通過

2026-11-06：**27 份 staged Java 重新編譯，全部 69 個 entry 為 67 SAME＋2 SAME_EFFECTS、DIFF 0；missingLeft／extraLeft 均空。** 本批暫存驗收完成，交 Claude 審查；尚未落地 src/main 或部署。

## 做了什麼

依[主控裁定](https://github.com/chaoren0418-arch/lito-launcher-server/issues/77#issuecomment-6658132109)，保留 `ff99993` 全部 A06 Java，包含 SourceLogin 捕獲順序、RuntimeInventory／ServerConfig effects 核准與 sneaky-throw helper。`ServerConfig#applyExternalSourceServers` 未修改。

在指定 worktree／`task/77` 上 fetch 後，將最新 `origin/main`（`9678e97f4bff1dfab7cc820297e3ea6ff533375c`）合入，合併提交為 `d120c4aeade17f3857dfcbba820d9f44d0ab2265`，無衝突。已包含 #174（PR #178，`6b769a4`）、#176（PR #193，`a32d30d`）及 #186（PR #202，`bccc2cb`）。未 checkout 或另建分支。

本輪手動修改只有本報告，main 帶入的其他任務內容保留。相對 main 的完整 PR 仍僅為 manifest 指定的 27 個 staged Java 加本報告。未自行修改 src/main、pom.xml、docs/REPORT.md、比較器或核准清單。

## 已驗證

以 Microsoft JDK 17.0.17、ASM 9.7，從合入的 main 原始碼重編 semantic-diff；三個 ASM 依賴均核對 SHA-256。已合併的 `tools/legacy-stubs/build.ps1` 成功編譯 9 個共用 compile-only stub，只加入編譯 classpath，沒有包進 A06 JAR；未新增私人 stub。

由 `a-batches.json` 的 A06 精選 27 份 Java，編到全新 `target/a06-reverify/classes`。使用 `javac --release 17 -encoding UTF-8`，classpath 依序為 `reference/base-0.6.71.jar;target/legacy-stubs;lib/*`。本 worktree 無 lib 目錄，javac 仍成功，無需其他函式庫。未複製正式 class，未做 ASM 後處理。

```text
Compiled 9 compile-only sources -> target/legacy-stubs (never packaged)
Note: Some input files use unchecked or unsafe operations.
Note: Recompile with -Xlint:unchecked for details.
```

固定 JAR 日期為 `2026-11-05T00:00:00Z`。先獨立核對 JAR 所有 class entries 精確等於 manifest 的 69 筆，再以同一完整清單執行 semantic-diff，正式 JAR 作 `--left-classpath`。工具 exit 0 僅代表成功產報告；腳本另外檢查 69 個 status 全為 SAME／SAME_EFFECTS，以及產物集合、missing／extra。

| 驗證 | 結果 |
|---|---:|
| Java 外層類別／輸出 entry | 27／69 |
| SAME／SAME_EFFECTS | 67／2 |
| DIFF／ONLY_LEFT／ONLY_RIGHT | 0／0／0 |
| SAME_LAYOUT／SAME_PATHS／ORPHAN_IGNORED／DEVIATION | 全部 0 |
| BYTE_SAME（包含在 69 筆內） | 3 |
| missingLeft／extraLeft | `[]`／`[]` |
| 腳本驗收閘門 | `A06_ACCEPTANCE=PASS`，exit 0 |

| 固定輸入／產物 | SHA-256 |
|---|---|
| 正式 reference JAR | `e1498871c7be6b287821c57ef5d95933db1740d9b4d4a80f6c3465f21d99d498` |
| 本輪新編 A06 JAR | `2fb2717cbad047df6ff642790f3dfb1cd683324ccaf1e31c6de5877ce2af5b43` |

新編 JAR 雜湊與 `ff99993` 報告完全一致，確認既有修正及編譯結果保留。

### 核准範圍與差異歸因

將 main 的七份核准清單與 A06 集合取交集，僅 effects 5 條、extra-methods 1 條適用；metadata-exempt、access-widen、layout、exceptions-exempt、deviations 均 0 條，因此不傳空清單，也未擴張核准範圍。

| 類別 | 方法 | 結果 |
|---|---|---|
| RuntimeInventory | `copy()`、`sourceEquipmentOnly()`、`withoutObjects(Set)` | 3 方法均 SAME_EFFECTS，withoutObjects 包含 2 處 return |
| ServerConfig | `load(Path,String)`、`applyExternalSourceServers(Properties,Path)` | 2 方法均 SAME_EFFECTS |
| ServerConfig | `rethrowSourceFailure(Throwable)` | 核准的左側額外 private static helper |

核准依據均為 [#77 主控裁定](https://github.com/chaoren0418-arch/lito-launcher-server/issues/77#issuecomment-6641248257)。下方腳本保留共用清單完整描述子及核准 URL。SAME_EFFECTS 只代表核准摘要相同，不證明完整參數資料流／分支目的地等價。

額外對同一份新編 JAR 加 `-Dsemanticdiff.disable=rethrow` 重跑：恢復 67 SAME、1 SAME_EFFECTS、1 DIFF，唯一 DIFF 為 ServerConfig.applyExternalSourceServers。預設啟用 #186 後為 0 DIFF；消融結果只供歸因，不是未完成項。本輪未重跑已由 #186 驗證的工具自測。

### 保留的來源證據

7 個非反編譯類別（NativeNpcPhysicalSource、NativePetWorldLifetime、NativeRoomRules、ResourceDatabase、RuntimeInventory、ServerConfig、SourceLogin）沿用以 src/main 為底移植的版本；19 個 DECOMPILED 類保留正式 JAR 來源標頭。SourceEquipmentPhysicalStats 沿用從 Accounts.java 抽出的 record。

來源、SourceLogin 原始 lambda 描述子相等／無額外陣列指令，以及 helper 例外物件 identity 的前輪證據見 `ff99993:docs/reports/issue-77.md`。前輪 #134 Maven JAR（`e560c7c`，SHA-256 `0f030726ef0c0bda675501a449f06071baf1095453cf23eaeb91155e29b84dcf`）的 javap 確認 `SourceFile: Accounts.java`。這些為歷史已驗證證據，本輪未重讀其他 worktree 或啟動應用。

## 未驗證什麼／推測

- 未落地 src/main，未跑 Maven 單元測試、應用回歸或真機驗收；依任務規格在 A-land 統一處理。
- 未啟動伺服器、登入器或遊戲，未連正式 DB，未碰機密設定、封存、硬連結或正式部署檔。
- 結論限於指定 69 個 entry 的編譯及核准語意驗收，不推論整體遊戲已通過回歸。

## 產出路徑／給下一棒的提示

- Git 交付：`docs/reports/issue-77.md`；既有 27 份 Java 位於 `lito伺服器/LiTo381-Server/merge-inputs/formal-0.6.71/staged/local/lito381/server/`。
- 本機證據（不進 git）：`scratch/a06-reverify/`，含腳本、核准交集、完整 69 筆 JSON／Markdown、來源雜湊收據、編譯日誌及 no-rethrow 消融結果。
- 編譯產物（不進 git）：`lito伺服器/LiTo381-Server/target/a06-reverify/`；共用 stub 在 `target/legacy-stubs/`。
- Claude 審查既有 [PR #109](https://github.com/chaoren0418-arch/lito-launcher-server/pull/109)。本輪已無 DIFF，無需新增 needs:claude 阻擋標籤；合併依既有流程處理。
- A-land 保留現有 Java、5 個 effects 與 1 個 helper 核准，使用含 #186 的工具；落地後執行完整 Maven 測試與 A 階段 692 entry 驗收。不要刪除 sneaky-throw helper 或擴大 throws 宣告。

## 69 個 entry 完整結果

下表由本輪 result.json 產生，名稱均位於 `local/lito381/server/`。

| Entry | 狀態 |
|---|---|

| `NativeNpcPhysicalSource.class` | SAME |
| `NativeNpcPhysicalSource$Snapshot.class` | SAME |
| `NativePcActionAuthority.class` | SAME |
| `NativePetWorldLifetime.class` | SAME |
| `NativePetWorldLifetime$Target.class` | SAME |
| `NativePlayerVitals.class` | SAME |
| `NativePlayerVitals$Result.class` | SAME |
| `NativeProcEncounterCatalog.class` | SAME |
| `NativeProcEncounterCatalog$Catalog.class` | SAME |
| `NativeProcEncounterCatalog$Npc.class` | SAME |
| `NativeProcEncounterCatalog$Receiver.class` | SAME |
| `NativeProcEncounterCatalog$Type.class` | SAME |
| `NativeProcEncounterPublication.class` | SAME |
| `NativeProcEncounterPublication$Context.class` | SAME |
| `NativeProcEncounterPublication$Stale.class` | SAME |
| `NativeProcEncounterPublication$Viewer.class` | SAME |
| `NativeProcHealBonus.class` | SAME |
| `NativeProcHealBonus$Catalog.class` | SAME |
| `NativeProcHealingWorld.class` | SAME |
| `NativeProcHealingWorld$Plan.class` | SAME |
| `NativeProcHealingWorld$Target.class` | SAME |
| `NativeProcNpcCatalog.class` | SAME |
| `NativeProcNpcCatalog$1.class` | SAME |
| `NativeProcNpcCatalog$Catalog.class` | SAME |
| `NativeProcNpcCatalog$Npc.class` | SAME |
| `NativeProcNpcState.class` | SAME |
| `NativeProcPlayerState.class` | SAME |
| `NativeProcPlayerState$Identity.class` | SAME |
| `NativeReuseLog.class` | SAME |
| `NativeReuseLog$Log.class` | SAME |
| `NativeReuseLog$Logger.class` | SAME |
| `NativeRoomRules.class` | SAME |
| `NativeRoomRules$Policy.class` | SAME |
| `NativeServerLifetime.class` | SAME |
| `NativeSkillConsumptionBridges.class` | SAME |
| `NativeSourceActionIntervalCalculator.class` | SAME |
| `NativeSourceActionIntervalCalculator$ActionType.class` | SAME |
| `NativeSourceActionIntervalCalculator$SpeedState.class` | SAME |
| `NativeSourceClientActionTimeline.class` | SAME |
| `NativeSourceClientActionTimeline$ActionBlockSnapshot.class` | SAME |
| `NativeSourceClientActionTimeline$ActionKind.class` | SAME |
| `NativeSourceClientActionTimeline$CombatDeferral.class` | SAME |
| `NativeSourceClientActionTimeline$Deadline.class` | SAME |
| `NativeSourceClientActionTimeline$Reservation.class` | SAME |
| `NativeSourceClientActionTimeline$VisibleWindow.class` | SAME |
| `NativeSourceMaterialPlan.class` | SAME |
| `NativeSourceMaterialPlan$ConsumeRequirement.class` | SAME |
| `NativeSourceMaterialPlan$Debit.class` | SAME |
| `NativeSourceMaterialPlan$ItemConsumeCommitResult.class` | SAME |
| `NativeSourceMaterialPlan$PlannedConsumption.class` | SAME |
| `NativeSourceMaterialPlan$Result.class` | SAME |
| `NativeSourceSkillCastAdmission.class` | SAME |
| `NativeSourceSkillCastAdmission$Reservation.class` | SAME |
| `NativeSourceSkillCastAdmission$Result.class` | SAME |
| `NativeSourceSkillConsumptionMath.class` | SAME |
| `NativeSourceSkillDelay.class` | SAME |
| `NativeSourceSkillDelay$Lease.class` | SAME |
| `ResourceDatabase.class` | SAME |
| `ResourceDatabase$Key.class` | SAME |
| `ResourceDatabase$Snapshot.class` | SAME |
| `RuntimeInventory.class` | SAME_EFFECTS |
| `RuntimeInventory$Item.class` | SAME |
| `RuntimeInventory$NativeArmorBonuses.class` | SAME |
| `RuntimeInventory$PersistentItem.class` | SAME |
| `ServerConfig.class` | SAME_EFFECTS |
| `SharedClientResources.class` | SAME |
| `SourceEquipmentPhysicalStats.class` | SAME |
| `SourceLogin.class` | SAME |
| `SourceLogin$Binding.class` | SAME |

## 完整重驗腳本

將下列內容存為 `scratch/a06-reverify/verify.ps1`（Windows PowerShell 5.1 使用 UTF-8 BOM），從 worktree 根目錄執行：

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File scratch/a06-reverify/verify.ps1
```

腳本拒絕覆用已有 class／JAR 的輸出。第二次重編時先將 `$out` 改為本 worktree 內全新 target 子目錄，不刪其他任務產物。ASM 依序使用 Maven 快取、本 worktree 前輪 target 快取或 Maven Central，均核對雜湊。

```powershell
$ErrorActionPreference = 'Stop'
$root = (Resolve-Path (Join-Path $PSScriptRoot '../..')).Path
$server = Join-Path $root 'lito伺服器/LiTo381-Server'
$out = Join-Path $server 'target/a06-reverify'
$jdk = 'C:/Program Files/Microsoft/jdk-17.0.17.10-hotspot/bin'
$utf8 = New-Object System.Text.UTF8Encoding($false)
function Write-Utf8($path, $value) { [IO.File]::WriteAllText($path, $value, $utf8) }
function Run($exe, $arguments, $log) {
    $old = $ErrorActionPreference
    $ErrorActionPreference = 'Continue'
    $lines = & $exe @arguments 2>&1
    $code = $LASTEXITCODE
    $ErrorActionPreference = $old
    Write-Utf8 (Join-Path $PSScriptRoot $log) (($lines | ForEach-Object { $_.ToString() }) -join "`n")
    if ($code -ne 0) { throw "$log failed ($code): $lines" }
}
if (@(Get-ChildItem "$out/classes", "$out/tool" -Filter *.class -Recurse -ErrorAction SilentlyContinue).Count -or (Test-Path "$out/left.jar")) { throw "Use a fresh output directory: $out" }
New-Item -ItemType Directory -Force -Path "$out/classes", "$out/tool", "$out/dependency" | Out-Null
$batch = (Get-Content -Encoding UTF8 "$root/docs/combat-merge/a-batches.json" | ConvertFrom-Json).batches | Where-Object id -eq A06
$expected = @($batch.verify_entries | Sort-Object)
if ($batch.classes.Count -ne 27 -or $expected.Count -ne 69) { throw 'Unexpected manifest' }
$formal = "$server/reference/base-0.6.71.jar"
$formalHash = (Get-FileHash $formal -Algorithm SHA256).Hash.ToLowerInvariant()
if ($formalHash -ne 'e1498871c7be6b287821c57ef5d95933db1740d9b4d4a80f6c3465f21d99d498') { throw 'Reference mismatch' }
$hashes = @{
    asm = 'adf46d5e34940bdf148ecdd26a9ee8eea94496a72034ff7141066b3eea5c4e9d'
    'asm-tree' = '62f4b3bc436045c1acb5c3ba2d8ec556ec3369093d7f5d06c747eb04b56d52b1'
    'asm-analysis' = '7bc6bcbc21379948a0c8c467fb0f864206e5b818f6bc0b546872f5c9f941556f'
}
foreach ($art in $hashes.Keys) {
    $dest = "$out/dependency/$art-9.7.jar"
    $cache = "D:/dev-cache/maven/repository/org/ow2/asm/$art/9.7/$art-9.7.jar"
    if (Test-Path $cache) { Copy-Item -LiteralPath $cache -Destination $dest }
    elseif (Test-Path "$server/target/a06-current/dependency/$art-9.7.jar") { Copy-Item -LiteralPath "$server/target/a06-current/dependency/$art-9.7.jar" -Destination $dest }
    else { Invoke-WebRequest "https://repo.maven.apache.org/maven2/org/ow2/asm/$art/9.7/$art-9.7.jar" -UseBasicParsing -OutFile $dest }
    if ((Get-FileHash $dest -Algorithm SHA256).Hash.ToLowerInvariant() -ne $hashes[$art]) { throw "ASM hash: $art" }
}
$toolSources = @(Get-ChildItem "$server/tools/semantic-diff/src/main/java" -Recurse -Filter '*.java' | Sort-Object FullName | ForEach-Object FullName)
Run "$jdk/javac.exe" (@('--release','17','-encoding','UTF-8','-cp',"$out/dependency/*",'-d',"$out/tool") + $toolSources) 'tool-build.log'
Run 'powershell' @('-NoProfile','-ExecutionPolicy','Bypass','-File',"$server/tools/legacy-stubs/build.ps1") 'stub-build.log'
$options = [ordered]@{
    'effects-approved.txt' = '--effects-methods'
    'extra-methods-approved.txt' = '--extra-methods'
    'metadata-exempt.txt' = '--metadata-exempt'
    'access-widen-approved.txt' = '--access-widen'
    'layout-approved.txt' = '--layout-methods'
    'exceptions-exempt.txt' = '--exceptions-exempt'
    'deviations.txt' = '--deviations'
}
$approvalArgs = @()
$approvalCounts = [ordered]@{}
foreach ($name in $options.Keys) {
    $selected = @(Get-Content -Encoding UTF8 "$root/docs/combat-merge/$name" | Where-Object {
        $line = $_.Trim()
        if (!$line -or $line.StartsWith('#')) { return $false }
        $owner = (($line -replace '^ctor-prologue-order\s+', '') -split '#')[0].Trim()
        $expected -contains "$owner.class"
    })
    $approvalCounts[$name] = $selected.Count
    if ($selected.Count) {
        $path = Join-Path $PSScriptRoot $name
        Write-Utf8 $path (($selected -join "`n") + "`n")
        $approvalArgs += @($options[$name], $path)
    }
}
if ($approvalCounts['effects-approved.txt'] -ne 5 -or $approvalCounts['extra-methods-approved.txt'] -ne 1) { throw 'Unexpected A06 approvals' }
Write-Utf8 "$PSScriptRoot/verify_entries.txt" (($batch.verify_entries -join "`n") + "`n")
$sources = @($batch.classes | ForEach-Object { "$server/merge-inputs/formal-0.6.71/staged/local/lito381/server/$_.java" })
Run "$jdk/javac.exe" (@('-J-Dfile.encoding=UTF-8','--release','17','-encoding','UTF-8','-cp',"$formal;$server/target/legacy-stubs;$server/lib/*",'-d',"$out/classes") + $sources) 'javac.log'
Run "$jdk/jar.exe" @('--create','--file',"$out/left.jar",'--no-manifest','--date','2026-11-05T00:00:00Z','-C',"$out/classes",'.') 'jar.log'
Add-Type -AssemblyName System.IO.Compression.FileSystem
$zip = [IO.Compression.ZipFile]::OpenRead("$out/left.jar")
try { $actual = @($zip.Entries.FullName | Where-Object { $_.EndsWith('.class') } | Sort-Object) } finally { $zip.Dispose() }
if (@(Compare-Object $expected $actual).Count -ne 0) { throw 'Missing or extra class entries' }
$toolcp = "$out/tool;$out/dependency/*"
$baseArgs = @('-Dfile.encoding=UTF-8','-cp',$toolcp,'local.lito381.semanticdiff.SemanticDiff','--left',"$out/left.jar",'--right',$formal,'--left-classpath',$formal,'--classes',"$PSScriptRoot/verify_entries.txt") + $approvalArgs
Run "$jdk/java.exe" ($baseArgs + @('--json',"$PSScriptRoot/result.json",'--md',"$PSScriptRoot/result.md")) 'semantic.log'
$result = Get-Content -Encoding UTF8 "$PSScriptRoot/result.json" | ConvertFrom-Json
$receipt = [ordered]@{
    toolCommit = (& git -C $root rev-parse origin/main)
    mergedHead = (& git -C $root rev-parse HEAD)
    formalSHA256 = $formalHash
    leftJarSHA256 = (Get-FileHash "$out/left.jar" -Algorithm SHA256).Hash.ToLowerInvariant()
    sourceCount = $sources.Count
    entryCount = $actual.Count
    approvals = $approvalCounts
    counts = $result.counts
    selection = $result.classSelection
    sourceHashes = @($sources | ForEach-Object { @{path=$_.Substring($root.Length+1); sha256=(Get-FileHash $_ -Algorithm SHA256).Hash.ToLowerInvariant()} })
}
Write-Utf8 "$PSScriptRoot/receipt.json" ($receipt | ConvertTo-Json -Depth 10)
$result.counts | ConvertTo-Json -Compress | Write-Output
$result.classSelection | ConvertTo-Json -Compress | Write-Output
if ($result.comparedClassCount -ne 69 -or @($result.classes | Where-Object { $_.status -notin @('SAME','SAME_EFFECTS') }).Count) { throw 'A06_ACCEPTANCE=FAIL' }
foreach ($p in $result.classSelection.PSObject.Properties) { if (@($p.Value).Count) { throw "Selection mismatch: $($p.Name)" } }
Write-Output 'A06_ACCEPTANCE=PASS'

```
