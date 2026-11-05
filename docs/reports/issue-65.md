# Issue #65 — T020-B04 戰鬥併入暫存

## 結論

**#176 合併後已完成重驗，B04 仍未通過最終語意驗收，交 `needs:claude`。** 2026-11-05 將最新 `origin/main`（`c72453957ebe47e00ad024e874bbe31859457799`）合入既有 `task/65`，merge commit 為 `b2292eda2326854ecf0b44df9f28e7adb9d683fb`。#174／#176 都已在主線，#176 的 PR #193 包含最後修正 `48b5843`。

重新整批編譯 51 份 staged Java，執行已合併 as-record 轉換器，以新版 semantic-diff 加本批精確核准清單驗證全部 98 個 entry：**95 SAME、1 SAME_PATHS、1 DEVIATION、1 DIFF；ONLY_LEFT／ONLY_RIGHT 均為 0，missingLeft／extraLeft 均為空。** 剩下的是 `NativeSourceNpcEscape` 的兩個方法，工具明確拒絕一般控制流程迴圈。此結果與 [#176 報告](issue-176.md) 一致；不能再以等待 #176 合併解釋，也不能宣告全部通過。

## 做了什麼

1. 在原 `task/65` 合併最新 main，沒有切換或建立分支。合併沒有衝突；main 帶入的其他檔案屬上游變更，本輪任務修改只有本報告。
2. 保留 `e5fa958` 的全部 51 份 Java 移植，包含指定 12 個 asm-only entry 的 Java 表達。`git diff e5fa958 -- lito伺服器/LiTo381-Server/merge-inputs/50635f7e/staged/local/lito381/server` 為空。六個指定方法一律未修改。
3. 保留三個 `@AsRecord`：`NativeSourceNpcFactoryType$Stamp`、`NativeSourceNpcPlayer$Scope`、`NativeSourcePhysicalNpc$Origin`；另保留 `NativeSourcePhysicalNpc$Commit` 的 `@AsmFinal`、transient 及預設欄位值契約。這四個 entry 本輪皆 SAME。
4. 使用全新 `target/issue-65/recheck-176/`，由本 worktree 已合併工具來源重新編譯工具與註解，再一次編譯本批全部 Java。沒有複製候選 class、替換或改名產物；只有核准 as-record 轉換器改寫四個標註 class。
5. 掃描全部八份核准 txt，B04 只有 `deviations.txt` 的兩條 PhysicalNpc `method-diff` 適用，其餘匹配數皆 0。這兩條與 javap 證明已由 #176 加入 `deviations.txt`／`deviations.md`，本輪不重複新增或擴大核准。
6. 重新 javap 查核兩個 producer 與 consumer，並附上兩個剩餘方法的完整新版正規形差異。依[主控裁定](https://github.com/chaoren0418-arch/lito-launcher-server/issues/65#issuecomment-6655613888)保留 callback／monitor 時序及 Java 原樣。

## 已驗證

### 編譯與完整性

環境：Windows、Microsoft OpenJDK 17.0.17、`--release 17 -encoding UTF-8`、ASM 9.7。編譯唯一警告為既有 `NativeSourceNpcPlayerView.java` unchecked／unsafe operations。PowerShell 外層日誌把 javac 的 Note 包裝成 NativeCommandError，但 javac 實際 exit=0，後續轉換與比對均完成。

```text
javac exit=0 sources=51
as-record: scanned=98 converted=4
converter changed=NativeSourceNpcFactoryType$Stamp.class,NativeSourceNpcPlayer$Scope.class,NativeSourcePhysicalNpc$Commit.class,NativeSourcePhysicalNpc$Origin.class
class inventory total=98 missing=0 extra=0
SUMMARY SAME=95 SAME_EFFECTS=0 SAME_PATHS=1 ORPHAN_IGNORED=0 SAME_LAYOUT=0 DEVIATION=1 DIFF=1 ONLY_LEFT=0 ONLY_RIGHT=0 BYTE_SAME=0 ALLOW_METHODS=0
{"missingLeft":[],"extraLeft":[]}
```

轉換前後逐 class SHA-256 比對僅四個標註 class 改變；驗收 JAR 打包整個輸出目錄，精確等於 B04 `verify_entries`。檢查 counts，而非把 CLI exit=0 當成驗收通過。

| 輸入／產物 | SHA-256 |
|---|---|
| candidate-50635f7e.jar | `50635f7edca8ec6ce77787cc32371c7590b497e48ea202f8a679caf3f517b729` |
| base-0.6.71.jar | `e1498871c7be6b287821c57ef5d95933db1740d9b4d4a80f6c3465f21d99d498` |
| recheck-176/b04.jar | `da35c6b2d6cf165b1afdfefb6c1409bf69c4aff7a66f5917e9cb9bc6fe0cc92e` |

JAR 雜湊含封裝時間；重建後以 class 清單與語意結果驗收。

### 六個指定方法的實測結果

| 類別／方法 | 結果 | 工具證據與核准依據 |
|---|---|---|
| NativeSourceNpcDamageDisplay.questDamage | SAME_PATHS | `path multisets equal (36)`；主控第一列、#176 第 2 項 |
| NativeSourceNpcDamageDisplay.timeDamage | SAME_PATHS | `path multisets equal (36)`；主控第一列、#176 第 2 項 |
| NativeSourceNpcEscape.nativeSourceFirstViewHandled | DIFF | `left=47:[241:java/lang/Throwable  ] right=47:[240:java/lang/Throwable  ]; paths rejected: normal control-flow cycle` |
| NativeSourceNpcEscape.nativeSourceNpcPlayerFirstHandled | DIFF | `left=47:[243:java/lang/Throwable  ] right=47:[242:java/lang/Throwable  ]; paths rejected: normal control-flow cycle` |
| NativeSourcePhysicalNpc.sourceCurrent | DEVIATION | 精確 `method-diff`；主控第三列、#176 第 3 項 |
| NativeSourcePhysicalNpc.targetCurrent | DEVIATION | 精確 `method-diff`；主控第三列、#176 第 3 項 |

上表為方法數；摘要為 class 數。兩個 DamageDisplay 方法保留離開 monitor 後才呼叫 `acceptedNatural`，正常與例外路徑已由逐路徑比較通過。PhysicalNpc 使用已合併的兩條核准，並不列為 SAME。

NpcEscape 的一般回邊已重新以兩側 javap 查核：

| 方法 | staged | candidate |
|---|---|---|
| nativeSourceFirstViewHandled | `895: goto 720` | `898: goto 722` |
| nativeSourceNpcPlayerFirstHandled | `900: goto 725` | `903: goto 727` |

這不是標準 monitor handler 的自我涵蓋；#176 明確只證明沒有一般控制流程迴圈的方法。完整差異見附錄，不能用局部反向條件看似等價取代工具驗收。

### PhysicalNpc producer javap 證明

重新執行 `javap -c -p -s -classpath <candidate> local.lito381.server.NativeSourceRawNpc` 與 `NativeSourceGroundNpc`，確認兩個指定 producer 各只有兩個 IRETURN，所有一般前驅如下：

| producer | IRETURN offset | 所有可能運算元來源 |
|---|---:|---|
| sourceState(Origin)I | 25 | offset 24 `iconst_m1` → -1 |
| sourceState(Origin)I | 224 | offset 219 `iconst_1` → 220 `goto 224`，或 223 `iconst_0` → 0／1 |
| targetState(Origin)I | 21 | offset 20 `iconst_m1` → -1 |
| targetState(Origin)I | 85 | offset 80 `iconst_1` → 81 `goto 85`，或 84 `iconst_0` → 0／1 |

候選兩個 consumer 均在 offset 4 `dup`、5 `iflt 9`、8 `ireturn`、9 `pop`；負值走既有 fallback，直接返回的值域為 0／1。staged 使用 `state >= 0` 與 `state != 0`。完整 producer javap 附於文末，另有兩側 consumer 原始 javap 與新版工具 DEVIATION 完整左右內容保存在本機證據；本報告也收錄後者。

## 推測

- NpcEscape 的剩餘差異看似是條件反向加 GOTO 與衍生指令位置差異，但本輪沒有可接受的全方法等價證明，維持 DIFF。
- 指定候選 producer 的正常回傳值域已驗證；這不替代 B-land 的執行期 consumer 測試，也不代表換候選後可沿用核准。

## 待驗證／未驗證什麼

- B04 尚缺 `NativeSourceNpcEscape` 兩個方法的可接受驗收，未達全部 98 entry 通過。
- 本批未執行應用程式、伺服器、登入器、遊戲或 DB；單元測試與回歸依設計交 B-land。
- 推送後 CI 另由調度器檢查。CI 通過亦不能代替本批仍為 DIFF=1 的 staged 語意驗收。

## 產出路徑

- 追蹤產出：`docs/reports/issue-65.md`；分支保留 B04 的 51 個 staged Java，這輪 Java 無變更。
- 本輪建置：`lito伺服器/LiTo381-Server/target/issue-65/recheck-176/`，含 annotations、semantic、converter、classes、diagnostic、b04.jar。
- 本機證據：`scratch/issue-65/recheck-176.ps1`、`recheck-176.log`、`recheck-176-semantic.json`／`.md`、`recheck-176-deviations.txt`、`recheck-176-left/`／`recheck-176-right/`、`recheck-176-escape.diff`、`recheck-176-*.javap.txt`、兩份 `recheck-176-*-producer.txt`。全部 scratch／target 不提交。
- 核准沿用：`docs/combat-merge/deviations.txt`、`deviations.md` 的 PhysicalNpc 兩條；本輪未修改。

## 給下一棒的提示

請雲端 Claude 接手 `needs:claude`：#176 已合併且本批已重驗，但其「無一般控制流程迴圈」前提不涵蓋 NpcEscape 的兩個方法。請裁定後續比較器工作或其他精確驗收方式；目前不自行修改這兩個 Java 方法、不新增 effects／layout／method-diff 豁免。完整新版差異、回邊與描述子均已附在本報告。調度器應保留未通過狀態，不要再以等待已完成的 #176 合併反覆派同一重驗。

B-land（#138）須保留三個 AsRecord 與 Commit 的 AsmFinal，整批編譯後執行已合併轉換器，並補 `sourceState`／`targetState` 各回傳 0、1 時，`sourceCurrent`／`targetCurrent` 與候選一致的測試。B04 最終驗收前不宣告 B-land 前置完成。

## 可重現指令

以下在此 worktree 根目錄執行，工具來源為合入的 `c724539`；先確保 B04 Java 仍為 `e5fa958` 的內容。輸出目錄必須全新，重跑請換另一個 `target/issue-65/` 子目錄。ASM 依賴以既有 SHA 校驗。兩條核准從已合併清單精確裁切；其他核准 txt 對 B04 匹配數為 0。

```powershell
$ErrorActionPreference = 'Stop'
$root = (Get-Location).Path
$server = Join-Path $root 'lito伺服器/LiTo381-Server'
$evidence = Join-Path $root 'scratch/issue-65'
$output = Join-Path $server 'target/issue-65/recheck-176'
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
[IO.File]::WriteAllLines((Join-Path $evidence 'recheck-176-deviations.txt'), [string[]]$approved, [Text.UTF8Encoding]::new($false))
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
& "$jdk/java.exe" '-Dfile.encoding=UTF-8' -cp "$semantic;$deps" local.lito381.semanticdiff.SemanticDiff --left $jar --right $candidate --left-classpath $candidate --left-classpath $base --classes $entries --deviations "$evidence/recheck-176-deviations.txt" --json "$evidence/recheck-176-semantic.json" --md "$evidence/recheck-176-semantic.md"
if ($LASTEXITCODE) { throw 'semantic tool failed' }
Get-FileHash $candidate,$base,$jar -Algorithm SHA256 | Format-Table -AutoSize
$result = Get-Content -Raw -Encoding UTF8 "$evidence/recheck-176-semantic.json" | ConvertFrom-Json
$result.counts | ConvertTo-Json -Compress
$result.classSelection | ConvertTo-Json -Compress
```

## 附錄：全部 98 個 entry 實測狀態

共同前綴為 `local/lito381/server/`，名稱加 `.class` 即完整 verify entry。

| Entry | 結果 |
|---|---|
| `NativeSourceLightningArea.class` | SAME |
| `NativeSourceLightningHp.class` | SAME |
| `NativeSourceLightningHp$Linked.class` | SAME |
| `NativeSourceLightningLifetime.class` | SAME |
| `NativeSourceLightningLifetime$Aim.class` | SAME |
| `NativeSourceLightningLifetime$Output.class` | SAME |
| `NativeSourceMagicNpc.class` | SAME |
| `NativeSourceMagicNpc$After.class` | SAME |
| `NativeSourceMagicNpc$Npc.class` | SAME |
| `NativeSourceMagicNpc$Snapshot.class` | SAME |
| `NativeSourceManualScope.class` | SAME |
| `NativeSourceMassSelfTarget.class` | SAME |
| `NativeSourceMassSlowTargets.class` | SAME |
| `NativeSourceMassSlowTargets$Aim.class` | SAME |
| `NativeSourceMassSlowTargets$Output.class` | SAME |
| `NativeSourceMassTeleportExit.class` | SAME |
| `NativeSourceMassTeleportExit$Proof.class` | SAME |
| `NativeSourceMaterialFirstLife.class` | SAME |
| `NativeSourceMaterialMain.class` | SAME |
| `NativeSourceMaterialPairGuard.class` | SAME |
| `NativeSourceMaterialPairGuard$Guarded.class` | SAME |
| `NativeSourceMaterialPairIngress.class` | SAME |
| `NativeSourceMonsterHp.class` | SAME |
| `NativeSourceMonsterHp$Aim.class` | SAME |
| `NativeSourceMonsterHp$Cache.class` | SAME |
| `NativeSourceMonsterHp$Viewer.class` | SAME |
| `NativeSourceNegativeFailure.class` | SAME |
| `NativeSourceNegativeFinalFrame.class` | SAME |
| `NativeSourceNegativeFinalFrame$Scope.class` | SAME |
| `NativeSourceNegativeFinalFrame$Value.class` | SAME |
| `NativeSourceNegativePairFirst.class` | SAME |
| `NativeSourceNegativePairFirst$First.class` | SAME |
| `NativeSourceNegativePhase.class` | SAME |
| `NativeSourceNegativeSelf.class` | SAME |
| `NativeSourceNegativeTargets.class` | SAME |
| `NativeSourceNegativeTargets$Aim.class` | SAME |
| `NativeSourceNegativeTargets$Output.class` | SAME |
| `NativeSourceNoneTargetRequest.class` | SAME |
| `NativeSourceNpcActionIntervals.class` | SAME |
| `NativeSourceNpcBaseSpeeds.class` | SAME |
| `NativeSourceNpcDamageDisplay.class` | SAME_PATHS |
| `NativeSourceNpcDamageDisplay$Proof.class` | SAME |
| `NativeSourceNpcDamageDisplay$Receipt.class` | SAME |
| `NativeSourceNpcEscape.class` | DIFF |
| `NativeSourceNpcFactoryMemory.class` | SAME |
| `NativeSourceNpcFactoryType.class` | SAME |
| `NativeSourceNpcFactoryType$Stamp.class` | SAME |
| `NativeSourceNpcFirstLife.class` | SAME |
| `NativeSourceNpcFirstView.class` | SAME |
| `NativeSourceNpcFirstView$View.class` | SAME |
| `NativeSourceNpcHpOutput.class` | SAME |
| `NativeSourceNpcHpOutput$Accepted.class` | SAME |
| `NativeSourceNpcHpOutput$Input.class` | SAME |
| `NativeSourceNpcHpOutput$Output.class` | SAME |
| `NativeSourceNpcHpOutput$Viewer.class` | SAME |
| `NativeSourceNpcMagicDirect.class` | SAME |
| `NativeSourceNpcMagicDirect$Mutation.class` | SAME |
| `NativeSourceNpcMagicEffects.class` | SAME |
| `NativeSourceNpcMagicEffects$After.class` | SAME |
| `NativeSourceNpcMagicEffects$Output.class` | SAME |
| `NativeSourceNpcMagicEffects$Source.class` | SAME |
| `NativeSourceNpcMotion.class` | SAME |
| `NativeSourceNpcMotion$Motion.class` | SAME |
| `NativeSourceNpcMotion$Saved.class` | SAME |
| `NativeSourceNpcMovement.class` | SAME |
| `NativeSourceNpcOrigins.class` | SAME |
| `NativeSourceNpcOrigins$Master.class` | SAME |
| `NativeSourceNpcPlayer.class` | SAME |
| `NativeSourceNpcPlayer$Origin.class` | SAME |
| `NativeSourceNpcPlayer$Scope.class` | SAME |
| `NativeSourceNpcPlayerView.class` | SAME |
| `NativeSourceNpcPlayerView$Builder.class` | SAME |
| `NativeSourceNpcReaction.class` | SAME |
| `NativeSourceNpcSpellTiming.class` | SAME |
| `NativeSourceNpcSpellTiming$Data.class` | SAME |
| `NativeSourceNpcSpellTiming$Route.class` | SAME |
| `NativeSourceNpcTargetFailure.class` | SAME |
| `NativeSourceNpcTargetFailure$Cursor.class` | SAME |
| `NativeSourceNpcTickView.class` | SAME |
| `NativeSourceNpcViewAction.class` | SAME |
| `NativeSourceObjectTargetRequest.class` | SAME |
| `NativeSourceOtherControls.class` | SAME |
| `NativeSourceOtherControls$Snapshot.class` | SAME |
| `NativeSourceOuterPair.class` | SAME |
| `NativeSourcePairAdmission.class` | SAME |
| `NativeSourcePairThird.class` | SAME |
| `NativeSourcePairedScope.class` | SAME |
| `NativeSourcePhysicalAudience.class` | SAME |
| `NativeSourcePhysicalAudience$Audience.class` | SAME |
| `NativeSourcePhysicalAudience$Receiver.class` | SAME |
| `NativeSourcePhysicalNpc.class` | DEVIATION |
| `NativeSourcePhysicalNpc$Commit.class` | SAME |
| `NativeSourcePhysicalNpc$Origin.class` | SAME |
| `NativeSourcePhysicalNpc$Output.class` | SAME |
| `NativeSourcePhysicalNpc$Spawn.class` | SAME |
| `NativeSourcePhysicalNpc$Ticket.class` | SAME |
| `NativeSourcePlayerFactories.class` | SAME |
| `NativeSourcePoisonAudience.class` | SAME |

## 附錄：兩個 NpcEscape 方法的完整新版差異

以下使用本輪已合併工具的 `ClassComparer.tryCatch` 與 `instructions`，包含完整例外涵蓋位置及指令差異，沒有以局部分支摘要刪去其餘差異。左為本輪 javac＋as-record 的 b04.jar，右為固定候選。L 編號代表有效指令位置，故一個 GOTO 的增減也會改變後續標號。

<details>
<summary>展開完整 unified diff（兩個方法）</summary>

```diff
diff --git "scratch\\issue-65\\recheck-176-left\\NativeSourceNpcEscape-nativeSourceFirstViewHandled-2396358605.txt" scratch/issue-65/recheck-176-right/NativeSourceNpcEscape-nativeSourceFirstViewHandled-2396358605.txt
index 14e9dee..ad6b8e6 100644
--- "scratch\\issue-65\\recheck-176-left\\NativeSourceNpcEscape-nativeSourceFirstViewHandled-2396358605.txt"
+++ scratch/issue-65/recheck-176-right/NativeSourceNpcEscape-nativeSourceFirstViewHandled-2396358605.txt
@@ -1,172 +1,172 @@
 nativeSourceFirstViewHandled(Llocal/lito381/server/GameHub;Ljava/util/List;Ljava/lang/Object;Llocal/lito381/server/BattleRules$Mode;JIIILlocal/lito381/server/ClientSession;JIILjava/lang/String;Ljava/lang/Object;)Z
-47:[241:java/lang/Throwable  ]
-49:[241:java/lang/Throwable  ]
-53:[241:java/lang/Throwable  ]
-54:[241:java/lang/Throwable  ]
-55:[241:java/lang/Throwable  ]
-65:[241:java/lang/Throwable  ]
-71:[241:java/lang/Throwable  ]
-75:[241:java/lang/Throwable  ]
-78:[241:java/lang/Throwable  ]
-81:[241:java/lang/Throwable  ]
-82:[241:java/lang/Throwable  ]
-85:[241:java/lang/Throwable  ]
-86:[241:java/lang/Throwable  ]
-89:[241:java/lang/Throwable  ]
-90:[241:java/lang/Throwable  ]
-100:[241:java/lang/Throwable  ]
-105:[241:java/lang/Throwable  ]
-109:[241:java/lang/Throwable  ]
-113:[241:java/lang/Throwable  ]
-114:[241:java/lang/Throwable  ]
-115:[241:java/lang/Throwable  ]
-120:[241:java/lang/Throwable  ]
-121:[241:java/lang/Throwable  ]
-122:[241:java/lang/Throwable  ]
-129:[241:java/lang/Throwable  ]
-130:[241:java/lang/Throwable  ]
-131:[241:java/lang/Throwable  ]
-137:[241:java/lang/Throwable  ]
-142:[241:java/lang/Throwable  ]
-146:[241:java/lang/Throwable  ]
-147:[241:java/lang/Throwable  ]
-151:[241:java/lang/Throwable  ]
-154:[241:java/lang/Throwable  ]
-155:[241:java/lang/Throwable  ]
-159:[241:java/lang/Throwable  ]
-166:[241:java/lang/Throwable  ]
-170:[241:java/lang/Throwable  ]
-171:[241:java/lang/Throwable  ]
-172:[241:java/lang/Throwable  ]
-173:[241:java/lang/Throwable  ]
-174:[241:java/lang/Throwable  ]
-181:[241:java/lang/Throwable  ]
-182:[241:java/lang/Throwable  ]
-186:[241:java/lang/Throwable  ]
-192:[241:java/lang/Throwable  ]
-193:[241:java/lang/Throwable  ]
-195:[241:java/lang/Throwable  ]
-199:[241:java/lang/Throwable  ]
-200:[241:java/lang/Throwable  ]
-206:[241:java/lang/Throwable  ]
-209:[241:java/lang/Throwable  ]
-210:[241:java/lang/Throwable  ]
-215:[241:java/lang/Throwable  ]
-218:[241:java/lang/Throwable  ]
-222:[241:java/lang/Throwable  ]
-226:[241:java/lang/Throwable  ]
-232:[241:java/lang/Throwable  ]
-233:[241:java/lang/Throwable  ]
-234:[241:java/lang/Throwable  ]
-239:[241:java/lang/Throwable  ]
-243:[241:java/lang/Throwable  ]
-316:[483:java/lang/ReflectiveOperationException  ]
-319:[483:java/lang/ReflectiveOperationException  ]
-323:[483:java/lang/ReflectiveOperationException  ]
-324:[483:java/lang/ReflectiveOperationException  ]
-327:[483:java/lang/ReflectiveOperationException  ]
-328:[483:java/lang/ReflectiveOperationException  ]
-331:[483:java/lang/ReflectiveOperationException  ]
-332:[483:java/lang/ReflectiveOperationException  ]
-335:[483:java/lang/ReflectiveOperationException  ]
-336:[483:java/lang/ReflectiveOperationException  ]
-339:[483:java/lang/ReflectiveOperationException  ]
-340:[483:java/lang/ReflectiveOperationException  ]
-343:[483:java/lang/ReflectiveOperationException  ]
-344:[483:java/lang/ReflectiveOperationException  ]
-345:[483:java/lang/ReflectiveOperationException  ]
-349:[483:java/lang/ReflectiveOperationException  ]
-351:[483:java/lang/ReflectiveOperationException  ]
-355:[483:java/lang/ReflectiveOperationException  ]
-359:[483:java/lang/ReflectiveOperationException  ]
-363:[483:java/lang/ReflectiveOperationException  ]
-367:[483:java/lang/ReflectiveOperationException  ]
-371:[483:java/lang/ReflectiveOperationException  ]
-375:[483:java/lang/ReflectiveOperationException  ]
-379:[483:java/lang/ReflectiveOperationException  ]
-383:[483:java/lang/ReflectiveOperationException  ]
-386:[483:java/lang/ReflectiveOperationException  ]
-395:[483:java/lang/ReflectiveOperationException  ]
-406:[483:java/lang/ReflectiveOperationException  ]
-412:[483:java/lang/ReflectiveOperationException  ]
-420:[483:java/lang/ReflectiveOperationException  ]
-425:[483:java/lang/ReflectiveOperationException  ]
-428:[483:java/lang/ReflectiveOperationException  ]
-429:[483:java/lang/ReflectiveOperationException  ]
-430:[483:java/lang/ReflectiveOperationException  ]
-435:[483:java/lang/ReflectiveOperationException  ]
-440:[483:java/lang/ReflectiveOperationException  ]
-444:[483:java/lang/ReflectiveOperationException  ]
-448:[483:java/lang/ReflectiveOperationException  ]
-452:[483:java/lang/ReflectiveOperationException  ]
-453:[483:java/lang/ReflectiveOperationException  ]
-457:[483:java/lang/ReflectiveOperationException  ]
-458:[483:java/lang/ReflectiveOperationException  ]
-462:[483:java/lang/ReflectiveOperationException  ]
-463:[483:java/lang/ReflectiveOperationException  ]
-467:[483:java/lang/ReflectiveOperationException  ]
-468:[483:java/lang/ReflectiveOperationException  ]
-469:[483:java/lang/ReflectiveOperationException  ]
-470:[483:java/lang/ReflectiveOperationException  ]
-471:[483:java/lang/ReflectiveOperationException  ]
-501:[680:java/lang/Throwable  ]
-503:[680:java/lang/Throwable  ]
-507:[680:java/lang/Throwable  ]
-508:[680:java/lang/Throwable  ]
-509:[680:java/lang/Throwable  ]
-515:[680:java/lang/Throwable  ]
-519:[680:java/lang/Throwable  ]
-524:[680:java/lang/Throwable  ]
-529:[680:java/lang/Throwable  ]
-536:[680:java/lang/Throwable  ]
-537:[680:java/lang/Throwable  ]
-539:[680:java/lang/Throwable  ]
-543:[680:java/lang/Throwable  ]
-544:[680:java/lang/Throwable  ]
-551:[680:java/lang/Throwable  ]
-552:[680:java/lang/Throwable  ]
-553:[680:java/lang/Throwable  ]
-558:[680:java/lang/Throwable  ]
-559:[680:java/lang/Throwable  ]
-560:[680:java/lang/Throwable  ]
-566:[680:java/lang/Throwable  ]
-570:[680:java/lang/Throwable  ]
-574:[680:java/lang/Throwable  ]
-575:[680:java/lang/Throwable  ]
-576:[680:java/lang/Throwable  ]
-581:[680:java/lang/Throwable  ]
-582:[680:java/lang/Throwable  ]
-583:[680:java/lang/Throwable  ]
-588:[680:java/lang/Throwable  ]
-594:[680:java/lang/Throwable  ]
-598:[680:java/lang/Throwable  ]
-599:[680:java/lang/Throwable  ]
-600:[680:java/lang/Throwable  ]
-601:[680:java/lang/Throwable  ]
-602:[680:java/lang/Throwable  ]
-609:[680:java/lang/Throwable  ]
-616:[680:java/lang/Throwable  ]
-622:[680:java/lang/Throwable  ]
-626:[680:java/lang/Throwable  ]
-627:[680:java/lang/Throwable  ]
-631:[680:java/lang/Throwable  ]
-632:[680:java/lang/Throwable  ]
-633:[680:java/lang/Throwable  ]
-637:[680:java/lang/Throwable  ]
-646:[680:java/lang/Throwable  ]
-648:[680:java/lang/Throwable  ]
-652:[680:java/lang/Throwable  ]
-653:[680:java/lang/Throwable  ]
-657:[680:java/lang/Throwable  ]
-658:[680:java/lang/Throwable  ]
-662:[680:java/lang/Throwable  ]
-663:[680:java/lang/Throwable  ]
-667:[680:java/lang/Throwable  ]
-668:[680:java/lang/Throwable  ]
-669:[680:java/lang/Throwable  ]
-675:[680:java/lang/Throwable  ]
-678:[680:java/lang/Throwable  ]
-682:[680:java/lang/Throwable  ]
+47:[240:java/lang/Throwable  ]
+49:[240:java/lang/Throwable  ]
+53:[240:java/lang/Throwable  ]
+54:[240:java/lang/Throwable  ]
+55:[240:java/lang/Throwable  ]
+65:[240:java/lang/Throwable  ]
+71:[240:java/lang/Throwable  ]
+75:[240:java/lang/Throwable  ]
+78:[240:java/lang/Throwable  ]
+81:[240:java/lang/Throwable  ]
+82:[240:java/lang/Throwable  ]
+85:[240:java/lang/Throwable  ]
+86:[240:java/lang/Throwable  ]
+89:[240:java/lang/Throwable  ]
+90:[240:java/lang/Throwable  ]
+100:[240:java/lang/Throwable  ]
+105:[240:java/lang/Throwable  ]
+109:[240:java/lang/Throwable  ]
+113:[240:java/lang/Throwable  ]
+114:[240:java/lang/Throwable  ]
+115:[240:java/lang/Throwable  ]
+120:[240:java/lang/Throwable  ]
+121:[240:java/lang/Throwable  ]
+122:[240:java/lang/Throwable  ]
+129:[240:java/lang/Throwable  ]
+130:[240:java/lang/Throwable  ]
+131:[240:java/lang/Throwable  ]
+136:[240:java/lang/Throwable  ]
+141:[240:java/lang/Throwable  ]
+145:[240:java/lang/Throwable  ]
+146:[240:java/lang/Throwable  ]
+150:[240:java/lang/Throwable  ]
+153:[240:java/lang/Throwable  ]
+154:[240:java/lang/Throwable  ]
+158:[240:java/lang/Throwable  ]
+165:[240:java/lang/Throwable  ]
+169:[240:java/lang/Throwable  ]
+170:[240:java/lang/Throwable  ]
+171:[240:java/lang/Throwable  ]
+172:[240:java/lang/Throwable  ]
+173:[240:java/lang/Throwable  ]
+180:[240:java/lang/Throwable  ]
+181:[240:java/lang/Throwable  ]
+185:[240:java/lang/Throwable  ]
+191:[240:java/lang/Throwable  ]
+192:[240:java/lang/Throwable  ]
+194:[240:java/lang/Throwable  ]
+198:[240:java/lang/Throwable  ]
+199:[240:java/lang/Throwable  ]
+205:[240:java/lang/Throwable  ]
+208:[240:java/lang/Throwable  ]
+209:[240:java/lang/Throwable  ]
+214:[240:java/lang/Throwable  ]
+217:[240:java/lang/Throwable  ]
+221:[240:java/lang/Throwable  ]
+225:[240:java/lang/Throwable  ]
+231:[240:java/lang/Throwable  ]
+232:[240:java/lang/Throwable  ]
+233:[240:java/lang/Throwable  ]
+238:[240:java/lang/Throwable  ]
+242:[240:java/lang/Throwable  ]
+315:[482:java/lang/ReflectiveOperationException  ]
+318:[482:java/lang/ReflectiveOperationException  ]
+322:[482:java/lang/ReflectiveOperationException  ]
+323:[482:java/lang/ReflectiveOperationException  ]
+326:[482:java/lang/ReflectiveOperationException  ]
+327:[482:java/lang/ReflectiveOperationException  ]
+330:[482:java/lang/ReflectiveOperationException  ]
+331:[482:java/lang/ReflectiveOperationException  ]
+334:[482:java/lang/ReflectiveOperationException  ]
+335:[482:java/lang/ReflectiveOperationException  ]
+338:[482:java/lang/ReflectiveOperationException  ]
+339:[482:java/lang/ReflectiveOperationException  ]
+342:[482:java/lang/ReflectiveOperationException  ]
+343:[482:java/lang/ReflectiveOperationException  ]
+344:[482:java/lang/ReflectiveOperationException  ]
+348:[482:java/lang/ReflectiveOperationException  ]
+350:[482:java/lang/ReflectiveOperationException  ]
+354:[482:java/lang/ReflectiveOperationException  ]
+358:[482:java/lang/ReflectiveOperationException  ]
+362:[482:java/lang/ReflectiveOperationException  ]
+366:[482:java/lang/ReflectiveOperationException  ]
+370:[482:java/lang/ReflectiveOperationException  ]
+374:[482:java/lang/ReflectiveOperationException  ]
+378:[482:java/lang/ReflectiveOperationException  ]
+382:[482:java/lang/ReflectiveOperationException  ]
+385:[482:java/lang/ReflectiveOperationException  ]
+394:[482:java/lang/ReflectiveOperationException  ]
+405:[482:java/lang/ReflectiveOperationException  ]
+411:[482:java/lang/ReflectiveOperationException  ]
+419:[482:java/lang/ReflectiveOperationException  ]
+424:[482:java/lang/ReflectiveOperationException  ]
+427:[482:java/lang/ReflectiveOperationException  ]
+428:[482:java/lang/ReflectiveOperationException  ]
+429:[482:java/lang/ReflectiveOperationException  ]
+434:[482:java/lang/ReflectiveOperationException  ]
+439:[482:java/lang/ReflectiveOperationException  ]
+443:[482:java/lang/ReflectiveOperationException  ]
+447:[482:java/lang/ReflectiveOperationException  ]
+451:[482:java/lang/ReflectiveOperationException  ]
+452:[482:java/lang/ReflectiveOperationException  ]
+456:[482:java/lang/ReflectiveOperationException  ]
+457:[482:java/lang/ReflectiveOperationException  ]
+461:[482:java/lang/ReflectiveOperationException  ]
+462:[482:java/lang/ReflectiveOperationException  ]
+466:[482:java/lang/ReflectiveOperationException  ]
+467:[482:java/lang/ReflectiveOperationException  ]
+468:[482:java/lang/ReflectiveOperationException  ]
+469:[482:java/lang/ReflectiveOperationException  ]
+470:[482:java/lang/ReflectiveOperationException  ]
+500:[679:java/lang/Throwable  ]
+502:[679:java/lang/Throwable  ]
+506:[679:java/lang/Throwable  ]
+507:[679:java/lang/Throwable  ]
+508:[679:java/lang/Throwable  ]
+514:[679:java/lang/Throwable  ]
+518:[679:java/lang/Throwable  ]
+523:[679:java/lang/Throwable  ]
+528:[679:java/lang/Throwable  ]
+535:[679:java/lang/Throwable  ]
+536:[679:java/lang/Throwable  ]
+538:[679:java/lang/Throwable  ]
+542:[679:java/lang/Throwable  ]
+543:[679:java/lang/Throwable  ]
+550:[679:java/lang/Throwable  ]
+551:[679:java/lang/Throwable  ]
+552:[679:java/lang/Throwable  ]
+557:[679:java/lang/Throwable  ]
+558:[679:java/lang/Throwable  ]
+559:[679:java/lang/Throwable  ]
+565:[679:java/lang/Throwable  ]
+569:[679:java/lang/Throwable  ]
+573:[679:java/lang/Throwable  ]
+574:[679:java/lang/Throwable  ]
+575:[679:java/lang/Throwable  ]
+580:[679:java/lang/Throwable  ]
+581:[679:java/lang/Throwable  ]
+582:[679:java/lang/Throwable  ]
+587:[679:java/lang/Throwable  ]
+593:[679:java/lang/Throwable  ]
+597:[679:java/lang/Throwable  ]
+598:[679:java/lang/Throwable  ]
+599:[679:java/lang/Throwable  ]
+600:[679:java/lang/Throwable  ]
+601:[679:java/lang/Throwable  ]
+608:[679:java/lang/Throwable  ]
+615:[679:java/lang/Throwable  ]
+621:[679:java/lang/Throwable  ]
+625:[679:java/lang/Throwable  ]
+626:[679:java/lang/Throwable  ]
+630:[679:java/lang/Throwable  ]
+631:[679:java/lang/Throwable  ]
+632:[679:java/lang/Throwable  ]
+636:[679:java/lang/Throwable  ]
+645:[679:java/lang/Throwable  ]
+647:[679:java/lang/Throwable  ]
+651:[679:java/lang/Throwable  ]
+652:[679:java/lang/Throwable  ]
+656:[679:java/lang/Throwable  ]
+657:[679:java/lang/Throwable  ]
+661:[679:java/lang/Throwable  ]
+662:[679:java/lang/Throwable  ]
+666:[679:java/lang/Throwable  ]
+667:[679:java/lang/Throwable  ]
+668:[679:java/lang/Throwable  ]
+674:[679:java/lang/Throwable  ]
+677:[679:java/lang/Throwable  ]
+681:[679:java/lang/Throwable  ]
 ALOAD 0
 INVOKESTATIC java/lang/Thread.holdsLock(Ljava/lang/Object;)Z
 IFEQ L9
@@ -234,7 +234,7 @@ ILOAD 7
 ILOAD 8
 INVOKESTATIC local/lito381/server/NativeSourceNpcFirstView.accepted(Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;Llocal/lito381/server/BattleRules$Mode;JIII)Z
 IFNE L68
-GOTO L135
+GOTO L134
 ALOAD 22
 IFNULL L73
 ALOAD 22
@@ -273,35 +273,34 @@ ALOAD 17
 ALOAD 22
 ILOAD 16
 INVOKESTATIC local/lito381/server/NativeSourceNpcEscape.phase(Ljava/lang/Object;Ljava/lang/Object;Z)Z
-IFEQ L135
+IFEQ L134
 ALOAD 22
 LLOAD 10/11
 INVOKESTATIC local/lito381/server/NativeSourceNpcEscape.blocked(Ljava/lang/Object;J)Z
-IFNE L135
+IFNE L134
 ALOAD 22
 CONST str:"x"
 INVOKESTATIC local/lito381/server/CombatAccess.field(Ljava/lang/Object;Ljava/lang/String;)Ljava/lang/Object;
 CHECKCAST java/lang/Integer
 INVOKEVIRTUAL java/lang/Integer.intValue()I
 ILOAD 7
-IF_ICMPNE L135
+IF_ICMPNE L134
 ALOAD 22
 CONST str:"y"
 INVOKESTATIC local/lito381/server/CombatAccess.field(Ljava/lang/Object;Ljava/lang/String;)Ljava/lang/Object;
 CHECKCAST java/lang/Integer
 INVOKEVIRTUAL java/lang/Integer.intValue()I
 ILOAD 8
-IF_ICMPNE L135
+IF_ICMPNE L134
 ILOAD 16
-IFEQ L139
+IFEQ L138
 ALOAD 22
 CONST str:"force"
 INVOKESTATIC local/lito381/server/CombatAccess.field(Ljava/lang/Object;Ljava/lang/String;)Ljava/lang/Object;
 CHECKCAST java/lang/Integer
 INVOKEVIRTUAL java/lang/Integer.intValue()I
 ILOAD 6
-IF_ICMPNE L135
-GOTO L139
+IF_ICMPEQ L138
 CONST Integer:1
 ALOAD 21
 MONITOREXIT
@@ -323,7 +322,7 @@ POP
 ALOAD 22
 INVOKESTATIC local/lito381/server/NativeSourceNpcMotion.motion(Ljava/lang/Object;)Llocal/lito381/server/NativeSourceNpcMotion$Motion;
 INVOKEVIRTUAL local/lito381/server/NativeSourceNpcMotion$Motion.escaped()Z
-IFNE L161
+IFNE L160
 CONST Integer:0
 ALOAD 21
 MONITOREXIT
@@ -342,35 +341,35 @@ AASTORE
 INVOKESTATIC local/lito381/server/CombatAccess.call(Ljava/lang/Object;Ljava/lang/String;Ljava/lang/String;[Ljava/lang/Object;)Ljava/lang/Object;
 CHECKCAST java/lang/Boolean
 INVOKEVIRTUAL java/lang/Boolean.booleanValue()Z
-IFEQ L178
+IFEQ L177
 CONST Integer:1
-GOTO L179
+GOTO L178
 CONST Integer:17
-IF_ICMPGT L184
+IF_ICMPGT L183
 ALOAD 23
 GETFIELD local/lito381/server/NativeSourceNpcFactoryType$Stamp.nativeSourceNpcMoveInterval:Ljava/lang/Integer;
 INVOKEVIRTUAL java/lang/Integer.intValue()I
-IFGT L188
+IFGT L187
 CONST Integer:1
 ALOAD 21
 MONITOREXIT
 IRETURN
 ILOAD 16
-IFEQ L197
+IFEQ L196
 ALOAD 17
 CONST str:"arenas"
 INVOKESTATIC local/lito381/server/CombatAccess.field(Ljava/lang/Object;Ljava/lang/String;)Ljava/lang/Object;
 CHECKCAST [Llocal/lito381/server/ArenaMapCatalog$Arena;
 ILOAD 6
 AALOAD
-GOTO L201
+GOTO L200
 ALOAD 17
 CONST str:"arena"
 INVOKESTATIC local/lito381/server/CombatAccess.field(Ljava/lang/Object;Ljava/lang/String;)Ljava/lang/Object;
 CHECKCAST local/lito381/server/ArenaMapCatalog$Arena
 ASTORE 26
 ALOAD 26
-IFNONNULL L208
+IFNONNULL L207
 CONST Integer:1
 ALOAD 21
 MONITOREXIT
@@ -396,18 +395,18 @@ CONST str:"nativeSourceNpcMotion"
 INVOKESTATIC local/lito381/server/CombatAccess.field(Ljava/lang/Object;Ljava/lang/String;)Ljava/lang/Object;
 ASTORE 30
 ILOAD 16
-IFEQ L236
+IFEQ L235
 ALOAD 22
 CONST str:"roomNumber"
 INVOKESTATIC local/lito381/server/CombatAccess.field(Ljava/lang/Object;Ljava/lang/String;)Ljava/lang/Object;
 CHECKCAST java/lang/Integer
 INVOKEVIRTUAL java/lang/Integer.intValue()I
-GOTO L237
+GOTO L236
 CONST Integer:0
 ISTORE 31
 ALOAD 21
 MONITOREXIT
-GOTO L246
+GOTO L245
 ASTORE 32
 ALOAD 21
 MONITOREXIT
@@ -559,7 +558,7 @@ CONST Integer:0
 ISTORE 38
 ILOAD 38
 ILOAD 37
-IF_ICMPGE L482
+IF_ICMPGE L481
 ALOAD 36
 ILOAD 38
 IALOAD
@@ -588,9 +587,9 @@ ILOAD 8
 ILOAD 41
 ILOAD 42
 INVOKEVIRTUAL local/lito381/server/ArenaMapCatalog$Arena.nativeCanStep(IIII)Z
-IFEQ L480
+IFEQ L479
 ILOAD 16
-IFEQ L437
+IFEQ L436
 ALOAD 26
 INVOKEVIRTUAL local/lito381/server/ArenaMapCatalog$Arena.mapId()I
 ALOAD 22
@@ -603,7 +602,7 @@ ILOAD 8
 ILOAD 41
 ILOAD 42
 INVOKESTATIC local/lito381/server/BattleQuestBoundary.blocksMap(IIIIII)Z
-IFNE L480
+IFNE L479
 ALOAD 35
 CONST null
 CONST Integer:6
@@ -639,17 +638,17 @@ AASTORE
 INVOKEVIRTUAL java/lang/reflect/Method.invoke(Ljava/lang/Object;[Ljava/lang/Object;)Ljava/lang/Object;
 CHECKCAST java/lang/Boolean
 INVOKEVIRTUAL java/lang/Boolean.booleanValue()Z
-IFNE L480
+IFNE L479
 ILOAD 40
 ISTORE 20
 ILOAD 41
 ISTORE 18
 ILOAD 42
 ISTORE 19
-GOTO L482
+GOTO L481
 IINC var=38 incr=1
-GOTO L390
-GOTO L490
+GOTO L389
+GOTO L489
 ASTORE 43
 NEW java/lang/IllegalStateException
 DUP
@@ -664,9 +663,9 @@ MONITORENTER
 ALOAD 17
 CONST str:"monster"
 ILOAD 16
-IFEQ L500
+IFEQ L499
 CONST str:"BattleQuestEncounter"
-GOTO L501
+GOTO L500
 CONST str:"BattleTimeAttackEncounter"
 INVOKEDYNAMIC makeConcatWithConstants(Ljava/lang/String;)Ljava/lang/String; bsm=H6 java/lang/invoke/StringConcatFactory.makeConcatWithConstants(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;Ljava/lang/String;[Ljava/lang/Object;)Ljava/lang/invoke/CallSite; args=[str:"(J)Llocal/lito381/server/\u0001$Monster;"]
 CONST Integer:1
@@ -680,79 +679,79 @@ INVOKESTATIC local/lito381/server/CombatAccess.call(Ljava/lang/Object;Ljava/lang
 ASTORE 45
 ALOAD 45
 ALOAD 22
-IF_ACMPNE L586
+IF_ACMPNE L585
 ALOAD 22
 INVOKESTATIC local/lito381/server/NativeSourceNpcFactoryType.stamp(Ljava/lang/Object;)Llocal/lito381/server/NativeSourceNpcFactoryType$Stamp;
 ALOAD 23
-IF_ACMPNE L586
+IF_ACMPNE L585
 ALOAD 22
 INVOKESTATIC local/lito381/server/NativeSourceMonsterHp.life(Ljava/lang/Object;)Ljava/lang/Object;
 ALOAD 28
-IF_ACMPNE L586
+IF_ACMPNE L585
 ALOAD 17
 CONST str:"sourceProcEncounterGeneration"
 INVOKESTATIC local/lito381/server/CombatAccess.field(Ljava/lang/Object;Ljava/lang/String;)Ljava/lang/Object;
 ALOAD 29
-IF_ACMPNE L586
+IF_ACMPNE L585
 ALOAD 22
 CONST str:"nativeSourceNpcMotion"
 INVOKESTATIC local/lito381/server/CombatAccess.field(Ljava/lang/Object;Ljava/lang/String;)Ljava/lang/Object;
 ALOAD 30
-IF_ACMPNE L586
+IF_ACMPNE L585
 ILOAD 16
-IFEQ L541
+IFEQ L540
 ALOAD 17
 CONST str:"arenas"
 INVOKESTATIC local/lito381/server/CombatAccess.field(Ljava/lang/Object;Ljava/lang/String;)Ljava/lang/Object;
 CHECKCAST [Llocal/lito381/server/ArenaMapCatalog$Arena;
 ILOAD 6
 AALOAD
-GOTO L545
+GOTO L544
 ALOAD 17
 CONST str:"arena"
 INVOKESTATIC local/lito381/server/CombatAccess.field(Ljava/lang/Object;Ljava/lang/String;)Ljava/lang/Object;
 CHECKCAST local/lito381/server/ArenaMapCatalog$Arena
 ALOAD 26
-IF_ACMPNE L586
+IF_ACMPNE L585
 ILOAD 16
-IFEQ L563
+IFEQ L562
 ALOAD 22
 CONST str:"force"
 INVOKESTATIC local/lito381/server/CombatAccess.field(Ljava/lang/Object;Ljava/lang/String;)Ljava/lang/Object;
 CHECKCAST java/lang/Integer
 INVOKEVIRTUAL java/lang/Integer.intValue()I
 ILOAD 6
-IF_ICMPNE L586
+IF_ICMPNE L585
 ALOAD 22
 CONST str:"roomNumber"
 INVOKESTATIC local/lito381/server/CombatAccess.field(Ljava/lang/Object;Ljava/lang/String;)Ljava/lang/Object;
 CHECKCAST java/lang/Integer
 INVOKEVIRTUAL java/lang/Integer.intValue()I
 ILOAD 31
-IF_ICMPNE L586
+IF_ICMPNE L585
 ALOAD 17
 ALOAD 22
 ILOAD 16
 INVOKESTATIC local/lito381/server/NativeSourceNpcEscape.phase(Ljava/lang/Object;Ljava/lang/Object;Z)Z
-IFEQ L586
+IFEQ L585
 ALOAD 22
 LLOAD 10/11
 INVOKESTATIC local/lito381/server/NativeSourceNpcEscape.blocked(Ljava/lang/Object;J)Z
-IFNE L586
+IFNE L585
 ALOAD 22
 CONST str:"x"
 INVOKESTATIC local/lito381/server/CombatAccess.field(Ljava/lang/Object;Ljava/lang/String;)Ljava/lang/Object;
 CHECKCAST java/lang/Integer
 INVOKEVIRTUAL java/lang/Integer.intValue()I
 ILOAD 7
-IF_ICMPNE L586
+IF_ICMPNE L585
 ALOAD 22
 CONST str:"y"
 INVOKESTATIC local/lito381/server/CombatAccess.field(Ljava/lang/Object;Ljava/lang/String;)Ljava/lang/Object;
 CHECKCAST java/lang/Integer
 INVOKEVIRTUAL java/lang/Integer.intValue()I
 ILOAD 8
-IF_ICMPEQ L590
+IF_ICMPEQ L589
 CONST Integer:1
 ALOAD 44
 MONITOREXIT
@@ -770,21 +769,21 @@ AASTORE
 INVOKESTATIC local/lito381/server/CombatAccess.call(Ljava/lang/Object;Ljava/lang/String;Ljava/lang/String;[Ljava/lang/Object;)Ljava/lang/Object;
 CHECKCAST java/lang/Boolean
 INVOKEVIRTUAL java/lang/Boolean.booleanValue()Z
-IFEQ L611
+IFEQ L610
 ILOAD 25
 CONST Integer:1
-IF_ICMPLE L611
+IF_ICMPLE L610
 CONST Integer:1
 ALOAD 44
 MONITOREXIT
 IRETURN
 ILOAD 20
-IFLT L618
+IFLT L617
 ALOAD 2
 ILOAD 18
 ILOAD 19
 INVOKESTATIC local/lito381/server/NativeSourceNpcEscape.door(Ljava/lang/Object;II)Z
-IFEQ L639
+IFEQ L638
 ALOAD 22
 CONST str:"schedule"
 CONST str:"(JI)V"
@@ -809,9 +808,9 @@ IRETURN
 ALOAD 17
 CONST str:"nativeSourceEscapeMoveForAi"
 ILOAD 16
-IFEQ L645
+IFEQ L644
 CONST str:"BattleQuestEncounter"
-GOTO L646
+GOTO L645
 CONST str:"BattleTimeAttackEncounter"
 INVOKEDYNAMIC makeConcatWithConstants(Ljava/lang/String;)Ljava/lang/String; bsm=H6 java/lang/invoke/StringConcatFactory.makeConcatWithConstants(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;Ljava/lang/String;[Ljava/lang/Object;)Ljava/lang/invoke/CallSite; args=[str:"(JIIJ)Llocal/lito381/server/\u0001$Snapshot;"]
 CONST Integer:4
@@ -839,14 +838,14 @@ AASTORE
 INVOKESTATIC local/lito381/server/CombatAccess.call(Ljava/lang/Object;Ljava/lang/String;Ljava/lang/String;[Ljava/lang/Object;)Ljava/lang/Object;
 ASTORE 46
 ALOAD 46
-IFNONNULL L677
+IFNONNULL L676
 CONST Integer:1
 ALOAD 44
 MONITOREXIT
 IRETURN
 ALOAD 44
 MONITOREXIT
-GOTO L685
+GOTO L684
 ASTORE 47
 ALOAD 44
 MONITOREXIT
diff --git "scratch\\issue-65\\recheck-176-left\\NativeSourceNpcEscape-nativeSourceNpcPlayerFirstHandled-3979114345.txt" scratch/issue-65/recheck-176-right/NativeSourceNpcEscape-nativeSourceNpcPlayerFirstHandled-3979114345.txt
index 428ecd9..eacc574 100644
--- "scratch\\issue-65\\recheck-176-left\\NativeSourceNpcEscape-nativeSourceNpcPlayerFirstHandled-3979114345.txt"
+++ scratch/issue-65/recheck-176-right/NativeSourceNpcEscape-nativeSourceNpcPlayerFirstHandled-3979114345.txt
@@ -1,174 +1,174 @@
 nativeSourceNpcPlayerFirstHandled(Llocal/lito381/server/GameHub;Ljava/util/List;Ljava/lang/Object;Llocal/lito381/server/BattleRules$Mode;JIIILlocal/lito381/server/ClientSession;JIILjava/lang/String;Ljava/lang/Object;Ljava/lang/Object;)Z
-47:[243:java/lang/Throwable  ]
-49:[243:java/lang/Throwable  ]
-53:[243:java/lang/Throwable  ]
-54:[243:java/lang/Throwable  ]
-55:[243:java/lang/Throwable  ]
-65:[243:java/lang/Throwable  ]
-67:[243:java/lang/Throwable  ]
-73:[243:java/lang/Throwable  ]
-77:[243:java/lang/Throwable  ]
-80:[243:java/lang/Throwable  ]
-83:[243:java/lang/Throwable  ]
-84:[243:java/lang/Throwable  ]
-87:[243:java/lang/Throwable  ]
-88:[243:java/lang/Throwable  ]
-91:[243:java/lang/Throwable  ]
-92:[243:java/lang/Throwable  ]
-102:[243:java/lang/Throwable  ]
-107:[243:java/lang/Throwable  ]
-111:[243:java/lang/Throwable  ]
-115:[243:java/lang/Throwable  ]
-116:[243:java/lang/Throwable  ]
-117:[243:java/lang/Throwable  ]
-122:[243:java/lang/Throwable  ]
-123:[243:java/lang/Throwable  ]
-124:[243:java/lang/Throwable  ]
-131:[243:java/lang/Throwable  ]
-132:[243:java/lang/Throwable  ]
-133:[243:java/lang/Throwable  ]
-139:[243:java/lang/Throwable  ]
-144:[243:java/lang/Throwable  ]
-148:[243:java/lang/Throwable  ]
-149:[243:java/lang/Throwable  ]
-153:[243:java/lang/Throwable  ]
-156:[243:java/lang/Throwable  ]
-157:[243:java/lang/Throwable  ]
-161:[243:java/lang/Throwable  ]
-168:[243:java/lang/Throwable  ]
-172:[243:java/lang/Throwable  ]
-173:[243:java/lang/Throwable  ]
-174:[243:java/lang/Throwable  ]
-175:[243:java/lang/Throwable  ]
-176:[243:java/lang/Throwable  ]
-183:[243:java/lang/Throwable  ]
-184:[243:java/lang/Throwable  ]
-188:[243:java/lang/Throwable  ]
-194:[243:java/lang/Throwable  ]
-195:[243:java/lang/Throwable  ]
-197:[243:java/lang/Throwable  ]
-201:[243:java/lang/Throwable  ]
-202:[243:java/lang/Throwable  ]
-208:[243:java/lang/Throwable  ]
-211:[243:java/lang/Throwable  ]
-212:[243:java/lang/Throwable  ]
-217:[243:java/lang/Throwable  ]
-220:[243:java/lang/Throwable  ]
-224:[243:java/lang/Throwable  ]
-228:[243:java/lang/Throwable  ]
-234:[243:java/lang/Throwable  ]
-235:[243:java/lang/Throwable  ]
-236:[243:java/lang/Throwable  ]
-241:[243:java/lang/Throwable  ]
-245:[243:java/lang/Throwable  ]
-318:[485:java/lang/ReflectiveOperationException  ]
-321:[485:java/lang/ReflectiveOperationException  ]
-325:[485:java/lang/ReflectiveOperationException  ]
-326:[485:java/lang/ReflectiveOperationException  ]
-329:[485:java/lang/ReflectiveOperationException  ]
-330:[485:java/lang/ReflectiveOperationException  ]
-333:[485:java/lang/ReflectiveOperationException  ]
-334:[485:java/lang/ReflectiveOperationException  ]
-337:[485:java/lang/ReflectiveOperationException  ]
-338:[485:java/lang/ReflectiveOperationException  ]
-341:[485:java/lang/ReflectiveOperationException  ]
-342:[485:java/lang/ReflectiveOperationException  ]
-345:[485:java/lang/ReflectiveOperationException  ]
-346:[485:java/lang/ReflectiveOperationException  ]
-347:[485:java/lang/ReflectiveOperationException  ]
-351:[485:java/lang/ReflectiveOperationException  ]
-353:[485:java/lang/ReflectiveOperationException  ]
-357:[485:java/lang/ReflectiveOperationException  ]
-361:[485:java/lang/ReflectiveOperationException  ]
-365:[485:java/lang/ReflectiveOperationException  ]
-369:[485:java/lang/ReflectiveOperationException  ]
-373:[485:java/lang/ReflectiveOperationException  ]
-377:[485:java/lang/ReflectiveOperationException  ]
-381:[485:java/lang/ReflectiveOperationException  ]
-385:[485:java/lang/ReflectiveOperationException  ]
-388:[485:java/lang/ReflectiveOperationException  ]
-397:[485:java/lang/ReflectiveOperationException  ]
-408:[485:java/lang/ReflectiveOperationException  ]
-414:[485:java/lang/ReflectiveOperationException  ]
-422:[485:java/lang/ReflectiveOperationException  ]
-427:[485:java/lang/ReflectiveOperationException  ]
-430:[485:java/lang/ReflectiveOperationException  ]
-431:[485:java/lang/ReflectiveOperationException  ]
-432:[485:java/lang/ReflectiveOperationException  ]
-437:[485:java/lang/ReflectiveOperationException  ]
-442:[485:java/lang/ReflectiveOperationException  ]
-446:[485:java/lang/ReflectiveOperationException  ]
-450:[485:java/lang/ReflectiveOperationException  ]
-454:[485:java/lang/ReflectiveOperationException  ]
-455:[485:java/lang/ReflectiveOperationException  ]
-459:[485:java/lang/ReflectiveOperationException  ]
-460:[485:java/lang/ReflectiveOperationException  ]
-464:[485:java/lang/ReflectiveOperationException  ]
-465:[485:java/lang/ReflectiveOperationException  ]
-469:[485:java/lang/ReflectiveOperationException  ]
-470:[485:java/lang/ReflectiveOperationException  ]
-471:[485:java/lang/ReflectiveOperationException  ]
-472:[485:java/lang/ReflectiveOperationException  ]
-473:[485:java/lang/ReflectiveOperationException  ]
-503:[684:java/lang/Throwable  ]
-505:[684:java/lang/Throwable  ]
-509:[684:java/lang/Throwable  ]
-510:[684:java/lang/Throwable  ]
-511:[684:java/lang/Throwable  ]
-516:[684:java/lang/Throwable  ]
-519:[684:java/lang/Throwable  ]
-523:[684:java/lang/Throwable  ]
-528:[684:java/lang/Throwable  ]
-533:[684:java/lang/Throwable  ]
-540:[684:java/lang/Throwable  ]
-541:[684:java/lang/Throwable  ]
-543:[684:java/lang/Throwable  ]
-547:[684:java/lang/Throwable  ]
-548:[684:java/lang/Throwable  ]
-555:[684:java/lang/Throwable  ]
-556:[684:java/lang/Throwable  ]
-557:[684:java/lang/Throwable  ]
-562:[684:java/lang/Throwable  ]
-563:[684:java/lang/Throwable  ]
-564:[684:java/lang/Throwable  ]
-570:[684:java/lang/Throwable  ]
-574:[684:java/lang/Throwable  ]
-578:[684:java/lang/Throwable  ]
-579:[684:java/lang/Throwable  ]
-580:[684:java/lang/Throwable  ]
-585:[684:java/lang/Throwable  ]
-586:[684:java/lang/Throwable  ]
-587:[684:java/lang/Throwable  ]
-592:[684:java/lang/Throwable  ]
-598:[684:java/lang/Throwable  ]
-602:[684:java/lang/Throwable  ]
-603:[684:java/lang/Throwable  ]
-604:[684:java/lang/Throwable  ]
-605:[684:java/lang/Throwable  ]
-606:[684:java/lang/Throwable  ]
-613:[684:java/lang/Throwable  ]
-620:[684:java/lang/Throwable  ]
-626:[684:java/lang/Throwable  ]
-630:[684:java/lang/Throwable  ]
-631:[684:java/lang/Throwable  ]
-635:[684:java/lang/Throwable  ]
-636:[684:java/lang/Throwable  ]
-637:[684:java/lang/Throwable  ]
-641:[684:java/lang/Throwable  ]
-650:[684:java/lang/Throwable  ]
-652:[684:java/lang/Throwable  ]
-656:[684:java/lang/Throwable  ]
-657:[684:java/lang/Throwable  ]
-661:[684:java/lang/Throwable  ]
-662:[684:java/lang/Throwable  ]
-666:[684:java/lang/Throwable  ]
-667:[684:java/lang/Throwable  ]
-671:[684:java/lang/Throwable  ]
-672:[684:java/lang/Throwable  ]
-673:[684:java/lang/Throwable  ]
-679:[684:java/lang/Throwable  ]
-682:[684:java/lang/Throwable  ]
-686:[684:java/lang/Throwable  ]
+47:[242:java/lang/Throwable  ]
+49:[242:java/lang/Throwable  ]
+53:[242:java/lang/Throwable  ]
+54:[242:java/lang/Throwable  ]
+55:[242:java/lang/Throwable  ]
+65:[242:java/lang/Throwable  ]
+67:[242:java/lang/Throwable  ]
+73:[242:java/lang/Throwable  ]
+77:[242:java/lang/Throwable  ]
+80:[242:java/lang/Throwable  ]
+83:[242:java/lang/Throwable  ]
+84:[242:java/lang/Throwable  ]
+87:[242:java/lang/Throwable  ]
+88:[242:java/lang/Throwable  ]
+91:[242:java/lang/Throwable  ]
+92:[242:java/lang/Throwable  ]
+102:[242:java/lang/Throwable  ]
+107:[242:java/lang/Throwable  ]
+111:[242:java/lang/Throwable  ]
+115:[242:java/lang/Throwable  ]
+116:[242:java/lang/Throwable  ]
+117:[242:java/lang/Throwable  ]
+122:[242:java/lang/Throwable  ]
+123:[242:java/lang/Throwable  ]
+124:[242:java/lang/Throwable  ]
+131:[242:java/lang/Throwable  ]
+132:[242:java/lang/Throwable  ]
+133:[242:java/lang/Throwable  ]
+138:[242:java/lang/Throwable  ]
+143:[242:java/lang/Throwable  ]
+147:[242:java/lang/Throwable  ]
+148:[242:java/lang/Throwable  ]
+152:[242:java/lang/Throwable  ]
+155:[242:java/lang/Throwable  ]
+156:[242:java/lang/Throwable  ]
+160:[242:java/lang/Throwable  ]
+167:[242:java/lang/Throwable  ]
+171:[242:java/lang/Throwable  ]
+172:[242:java/lang/Throwable  ]
+173:[242:java/lang/Throwable  ]
+174:[242:java/lang/Throwable  ]
+175:[242:java/lang/Throwable  ]
+182:[242:java/lang/Throwable  ]
+183:[242:java/lang/Throwable  ]
+187:[242:java/lang/Throwable  ]
+193:[242:java/lang/Throwable  ]
+194:[242:java/lang/Throwable  ]
+196:[242:java/lang/Throwable  ]
+200:[242:java/lang/Throwable  ]
+201:[242:java/lang/Throwable  ]
+207:[242:java/lang/Throwable  ]
+210:[242:java/lang/Throwable  ]
+211:[242:java/lang/Throwable  ]
+216:[242:java/lang/Throwable  ]
+219:[242:java/lang/Throwable  ]
+223:[242:java/lang/Throwable  ]
+227:[242:java/lang/Throwable  ]
+233:[242:java/lang/Throwable  ]
+234:[242:java/lang/Throwable  ]
+235:[242:java/lang/Throwable  ]
+240:[242:java/lang/Throwable  ]
+244:[242:java/lang/Throwable  ]
+317:[484:java/lang/ReflectiveOperationException  ]
+320:[484:java/lang/ReflectiveOperationException  ]
+324:[484:java/lang/ReflectiveOperationException  ]
+325:[484:java/lang/ReflectiveOperationException  ]
+328:[484:java/lang/ReflectiveOperationException  ]
+329:[484:java/lang/ReflectiveOperationException  ]
+332:[484:java/lang/ReflectiveOperationException  ]
+333:[484:java/lang/ReflectiveOperationException  ]
+336:[484:java/lang/ReflectiveOperationException  ]
+337:[484:java/lang/ReflectiveOperationException  ]
+340:[484:java/lang/ReflectiveOperationException  ]
+341:[484:java/lang/ReflectiveOperationException  ]
+344:[484:java/lang/ReflectiveOperationException  ]
+345:[484:java/lang/ReflectiveOperationException  ]
+346:[484:java/lang/ReflectiveOperationException  ]
+350:[484:java/lang/ReflectiveOperationException  ]
+352:[484:java/lang/ReflectiveOperationException  ]
+356:[484:java/lang/ReflectiveOperationException  ]
+360:[484:java/lang/ReflectiveOperationException  ]
+364:[484:java/lang/ReflectiveOperationException  ]
+368:[484:java/lang/ReflectiveOperationException  ]
+372:[484:java/lang/ReflectiveOperationException  ]
+376:[484:java/lang/ReflectiveOperationException  ]
+380:[484:java/lang/ReflectiveOperationException  ]
+384:[484:java/lang/ReflectiveOperationException  ]
+387:[484:java/lang/ReflectiveOperationException  ]
+396:[484:java/lang/ReflectiveOperationException  ]
+407:[484:java/lang/ReflectiveOperationException  ]
+413:[484:java/lang/ReflectiveOperationException  ]
+421:[484:java/lang/ReflectiveOperationException  ]
+426:[484:java/lang/ReflectiveOperationException  ]
+429:[484:java/lang/ReflectiveOperationException  ]
+430:[484:java/lang/ReflectiveOperationException  ]
+431:[484:java/lang/ReflectiveOperationException  ]
+436:[484:java/lang/ReflectiveOperationException  ]
+441:[484:java/lang/ReflectiveOperationException  ]
+445:[484:java/lang/ReflectiveOperationException  ]
+449:[484:java/lang/ReflectiveOperationException  ]
+453:[484:java/lang/ReflectiveOperationException  ]
+454:[484:java/lang/ReflectiveOperationException  ]
+458:[484:java/lang/ReflectiveOperationException  ]
+459:[484:java/lang/ReflectiveOperationException  ]
+463:[484:java/lang/ReflectiveOperationException  ]
+464:[484:java/lang/ReflectiveOperationException  ]
+468:[484:java/lang/ReflectiveOperationException  ]
+469:[484:java/lang/ReflectiveOperationException  ]
+470:[484:java/lang/ReflectiveOperationException  ]
+471:[484:java/lang/ReflectiveOperationException  ]
+472:[484:java/lang/ReflectiveOperationException  ]
+502:[683:java/lang/Throwable  ]
+504:[683:java/lang/Throwable  ]
+508:[683:java/lang/Throwable  ]
+509:[683:java/lang/Throwable  ]
+510:[683:java/lang/Throwable  ]
+515:[683:java/lang/Throwable  ]
+518:[683:java/lang/Throwable  ]
+522:[683:java/lang/Throwable  ]
+527:[683:java/lang/Throwable  ]
+532:[683:java/lang/Throwable  ]
+539:[683:java/lang/Throwable  ]
+540:[683:java/lang/Throwable  ]
+542:[683:java/lang/Throwable  ]
+546:[683:java/lang/Throwable  ]
+547:[683:java/lang/Throwable  ]
+554:[683:java/lang/Throwable  ]
+555:[683:java/lang/Throwable  ]
+556:[683:java/lang/Throwable  ]
+561:[683:java/lang/Throwable  ]
+562:[683:java/lang/Throwable  ]
+563:[683:java/lang/Throwable  ]
+569:[683:java/lang/Throwable  ]
+573:[683:java/lang/Throwable  ]
+577:[683:java/lang/Throwable  ]
+578:[683:java/lang/Throwable  ]
+579:[683:java/lang/Throwable  ]
+584:[683:java/lang/Throwable  ]
+585:[683:java/lang/Throwable  ]
+586:[683:java/lang/Throwable  ]
+591:[683:java/lang/Throwable  ]
+597:[683:java/lang/Throwable  ]
+601:[683:java/lang/Throwable  ]
+602:[683:java/lang/Throwable  ]
+603:[683:java/lang/Throwable  ]
+604:[683:java/lang/Throwable  ]
+605:[683:java/lang/Throwable  ]
+612:[683:java/lang/Throwable  ]
+619:[683:java/lang/Throwable  ]
+625:[683:java/lang/Throwable  ]
+629:[683:java/lang/Throwable  ]
+630:[683:java/lang/Throwable  ]
+634:[683:java/lang/Throwable  ]
+635:[683:java/lang/Throwable  ]
+636:[683:java/lang/Throwable  ]
+640:[683:java/lang/Throwable  ]
+649:[683:java/lang/Throwable  ]
+651:[683:java/lang/Throwable  ]
+655:[683:java/lang/Throwable  ]
+656:[683:java/lang/Throwable  ]
+660:[683:java/lang/Throwable  ]
+661:[683:java/lang/Throwable  ]
+665:[683:java/lang/Throwable  ]
+666:[683:java/lang/Throwable  ]
+670:[683:java/lang/Throwable  ]
+671:[683:java/lang/Throwable  ]
+672:[683:java/lang/Throwable  ]
+678:[683:java/lang/Throwable  ]
+681:[683:java/lang/Throwable  ]
+685:[683:java/lang/Throwable  ]
 ALOAD 0
 INVOKESTATIC java/lang/Thread.holdsLock(Ljava/lang/Object;)Z
 IFEQ L9
@@ -238,7 +238,7 @@ INVOKESTATIC local/lito381/server/NativeSourceNpcFirstView.accepted(Ljava/lang/O
 ALOAD 16
 INVOKESTATIC local/lito381/server/NativeSourceNpcPlayerView.both(ZLjava/lang/Object;)Z
 IFNE L70
-GOTO L137
+GOTO L136
 ALOAD 23
 IFNULL L75
 ALOAD 23
@@ -277,35 +277,34 @@ ALOAD 18
 ALOAD 23
 ILOAD 17
 INVOKESTATIC local/lito381/server/NativeSourceNpcEscape.phase(Ljava/lang/Object;Ljava/lang/Object;Z)Z
-IFEQ L137
+IFEQ L136
 ALOAD 23
 LLOAD 10/11
 INVOKESTATIC local/lito381/server/NativeSourceNpcEscape.blocked(Ljava/lang/Object;J)Z
-IFNE L137
+IFNE L136
 ALOAD 23
 CONST str:"x"
 INVOKESTATIC local/lito381/server/CombatAccess.field(Ljava/lang/Object;Ljava/lang/String;)Ljava/lang/Object;
 CHECKCAST java/lang/Integer
 INVOKEVIRTUAL java/lang/Integer.intValue()I
 ILOAD 7
-IF_ICMPNE L137
+IF_ICMPNE L136
 ALOAD 23
 CONST str:"y"
 INVOKESTATIC local/lito381/server/CombatAccess.field(Ljava/lang/Object;Ljava/lang/String;)Ljava/lang/Object;
 CHECKCAST java/lang/Integer
 INVOKEVIRTUAL java/lang/Integer.intValue()I
 ILOAD 8
-IF_ICMPNE L137
+IF_ICMPNE L136
 ILOAD 17
-IFEQ L141
+IFEQ L140
 ALOAD 23
 CONST str:"force"
 INVOKESTATIC local/lito381/server/CombatAccess.field(Ljava/lang/Object;Ljava/lang/String;)Ljava/lang/Object;
 CHECKCAST java/lang/Integer
 INVOKEVIRTUAL java/lang/Integer.intValue()I
 ILOAD 6
-IF_ICMPNE L137
-GOTO L141
+IF_ICMPEQ L140
 CONST Integer:1
 ALOAD 22
 MONITOREXIT
@@ -327,7 +326,7 @@ POP
 ALOAD 23
 INVOKESTATIC local/lito381/server/NativeSourceNpcMotion.motion(Ljava/lang/Object;)Llocal/lito381/server/NativeSourceNpcMotion$Motion;
 INVOKEVIRTUAL local/lito381/server/NativeSourceNpcMotion$Motion.escaped()Z
-IFNE L163
+IFNE L162
 CONST Integer:0
 ALOAD 22
 MONITOREXIT
@@ -346,35 +345,35 @@ AASTORE
 INVOKESTATIC local/lito381/server/CombatAccess.call(Ljava/lang/Object;Ljava/lang/String;Ljava/lang/String;[Ljava/lang/Object;)Ljava/lang/Object;
 CHECKCAST java/lang/Boolean
 INVOKEVIRTUAL java/lang/Boolean.booleanValue()Z
-IFEQ L180
+IFEQ L179
 CONST Integer:1
-GOTO L181
+GOTO L180
 CONST Integer:17
-IF_ICMPGT L186
+IF_ICMPGT L185
 ALOAD 24
 GETFIELD local/lito381/server/NativeSourceNpcFactoryType$Stamp.nativeSourceNpcMoveInterval:Ljava/lang/Integer;
 INVOKEVIRTUAL java/lang/Integer.intValue()I
-IFGT L190
+IFGT L189
 CONST Integer:1
 ALOAD 22
 MONITOREXIT
 IRETURN
 ILOAD 17
-IFEQ L199
+IFEQ L198
 ALOAD 18
 CONST str:"arenas"
 INVOKESTATIC local/lito381/server/CombatAccess.field(Ljava/lang/Object;Ljava/lang/String;)Ljava/lang/Object;
 CHECKCAST [Llocal/lito381/server/ArenaMapCatalog$Arena;
 ILOAD 6
 AALOAD
-GOTO L203
+GOTO L202
 ALOAD 18
 CONST str:"arena"
 INVOKESTATIC local/lito381/server/CombatAccess.field(Ljava/lang/Object;Ljava/lang/String;)Ljava/lang/Object;
 CHECKCAST local/lito381/server/ArenaMapCatalog$Arena
 ASTORE 27
 ALOAD 27
-IFNONNULL L210
+IFNONNULL L209
 CONST Integer:1
 ALOAD 22
 MONITOREXIT
@@ -400,18 +399,18 @@ CONST str:"nativeSourceNpcMotion"
 INVOKESTATIC local/lito381/server/CombatAccess.field(Ljava/lang/Object;Ljava/lang/String;)Ljava/lang/Object;
 ASTORE 31
 ILOAD 17
-IFEQ L238
+IFEQ L237
 ALOAD 23
 CONST str:"roomNumber"
 INVOKESTATIC local/lito381/server/CombatAccess.field(Ljava/lang/Object;Ljava/lang/String;)Ljava/lang/Object;
 CHECKCAST java/lang/Integer
 INVOKEVIRTUAL java/lang/Integer.intValue()I
-GOTO L239
+GOTO L238
 CONST Integer:0
 ISTORE 32
 ALOAD 22
 MONITOREXIT
-GOTO L248
+GOTO L247
 ASTORE 33
 ALOAD 22
 MONITOREXIT
@@ -563,7 +562,7 @@ CONST Integer:0
 ISTORE 39
 ILOAD 39
 ILOAD 38
-IF_ICMPGE L484
+IF_ICMPGE L483
 ALOAD 37
 ILOAD 39
 IALOAD
@@ -592,9 +591,9 @@ ILOAD 8
 ILOAD 42
 ILOAD 43
 INVOKEVIRTUAL local/lito381/server/ArenaMapCatalog$Arena.nativeCanStep(IIII)Z
-IFEQ L482
+IFEQ L481
 ILOAD 17
-IFEQ L439
+IFEQ L438
 ALOAD 27
 INVOKEVIRTUAL local/lito381/server/ArenaMapCatalog$Arena.mapId()I
 ALOAD 23
@@ -607,7 +606,7 @@ ILOAD 8
 ILOAD 42
 ILOAD 43
 INVOKESTATIC local/lito381/server/BattleQuestBoundary.blocksMap(IIIIII)Z
-IFNE L482
+IFNE L481
 ALOAD 36
 CONST null
 CONST Integer:6
@@ -643,17 +642,17 @@ AASTORE
 INVOKEVIRTUAL java/lang/reflect/Method.invoke(Ljava/lang/Object;[Ljava/lang/Object;)Ljava/lang/Object;
 CHECKCAST java/lang/Boolean
 INVOKEVIRTUAL java/lang/Boolean.booleanValue()Z
-IFNE L482
+IFNE L481
 ILOAD 41
 ISTORE 21
 ILOAD 42
 ISTORE 19
 ILOAD 43
 ISTORE 20
-GOTO L484
+GOTO L483
 IINC var=39 incr=1
-GOTO L392
-GOTO L492
+GOTO L391
+GOTO L491
 ASTORE 44
 NEW java/lang/IllegalStateException
 DUP
@@ -668,9 +667,9 @@ MONITORENTER
 ALOAD 18
 CONST str:"monster"
 ILOAD 17
-IFEQ L502
+IFEQ L501
 CONST str:"BattleQuestEncounter"
-GOTO L503
+GOTO L502
 CONST str:"BattleTimeAttackEncounter"
 INVOKEDYNAMIC makeConcatWithConstants(Ljava/lang/String;)Ljava/lang/String; bsm=H6 java/lang/invoke/StringConcatFactory.makeConcatWithConstants(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;Ljava/lang/String;[Ljava/lang/Object;)Ljava/lang/invoke/CallSite; args=[str:"(J)Llocal/lito381/server/\u0001$Monster;"]
 CONST Integer:1
@@ -686,79 +685,79 @@ ALOAD 46
 ALOAD 23
 ALOAD 16
 INVOKESTATIC local/lito381/server/NativeSourceNpcPlayerView.identity(Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;)Z
-IFEQ L590
+IFEQ L589
 ALOAD 23
 INVOKESTATIC local/lito381/server/NativeSourceNpcFactoryType.stamp(Ljava/lang/Object;)Llocal/lito381/server/NativeSourceNpcFactoryType$Stamp;
 ALOAD 24
-IF_ACMPNE L590
+IF_ACMPNE L589
 ALOAD 23
 INVOKESTATIC local/lito381/server/NativeSourceMonsterHp.life(Ljava/lang/Object;)Ljava/lang/Object;
 ALOAD 29
-IF_ACMPNE L590
+IF_ACMPNE L589
 ALOAD 18
 CONST str:"sourceProcEncounterGeneration"
 INVOKESTATIC local/lito381/server/CombatAccess.field(Ljava/lang/Object;Ljava/lang/String;)Ljava/lang/Object;
 ALOAD 30
-IF_ACMPNE L590
+IF_ACMPNE L589
 ALOAD 23
 CONST str:"nativeSourceNpcMotion"
 INVOKESTATIC local/lito381/server/CombatAccess.field(Ljava/lang/Object;Ljava/lang/String;)Ljava/lang/Object;
 ALOAD 31
-IF_ACMPNE L590
+IF_ACMPNE L589
 ILOAD 17
-IFEQ L545
+IFEQ L544
 ALOAD 18
 CONST str:"arenas"
 INVOKESTATIC local/lito381/server/CombatAccess.field(Ljava/lang/Object;Ljava/lang/String;)Ljava/lang/Object;
 CHECKCAST [Llocal/lito381/server/ArenaMapCatalog$Arena;
 ILOAD 6
 AALOAD
-GOTO L549
+GOTO L548
 ALOAD 18
 CONST str:"arena"
 INVOKESTATIC local/lito381/server/CombatAccess.field(Ljava/lang/Object;Ljava/lang/String;)Ljava/lang/Object;
 CHECKCAST local/lito381/server/ArenaMapCatalog$Arena
 ALOAD 27
-IF_ACMPNE L590
+IF_ACMPNE L589
 ILOAD 17
-IFEQ L567
+IFEQ L566
 ALOAD 23
 CONST str:"force"
 INVOKESTATIC local/lito381/server/CombatAccess.field(Ljava/lang/Object;Ljava/lang/String;)Ljava/lang/Object;
 CHECKCAST java/lang/Integer
 INVOKEVIRTUAL java/lang/Integer.intValue()I
 ILOAD 6
-IF_ICMPNE L590
+IF_ICMPNE L589
 ALOAD 23
 CONST str:"roomNumber"
 INVOKESTATIC local/lito381/server/CombatAccess.field(Ljava/lang/Object;Ljava/lang/String;)Ljava/lang/Object;
 CHECKCAST java/lang/Integer
 INVOKEVIRTUAL java/lang/Integer.intValue()I
 ILOAD 32
-IF_ICMPNE L590
+IF_ICMPNE L589
 ALOAD 18
 ALOAD 23
 ILOAD 17
 INVOKESTATIC local/lito381/server/NativeSourceNpcEscape.phase(Ljava/lang/Object;Ljava/lang/Object;Z)Z
-IFEQ L590
+IFEQ L589
 ALOAD 23
 LLOAD 10/11
 INVOKESTATIC local/lito381/server/NativeSourceNpcEscape.blocked(Ljava/lang/Object;J)Z
-IFNE L590
+IFNE L589
 ALOAD 23
 CONST str:"x"
 INVOKESTATIC local/lito381/server/CombatAccess.field(Ljava/lang/Object;Ljava/lang/String;)Ljava/lang/Object;
 CHECKCAST java/lang/Integer
 INVOKEVIRTUAL java/lang/Integer.intValue()I
 ILOAD 7
-IF_ICMPNE L590
+IF_ICMPNE L589
 ALOAD 23
 CONST str:"y"
 INVOKESTATIC local/lito381/server/CombatAccess.field(Ljava/lang/Object;Ljava/lang/String;)Ljava/lang/Object;
 CHECKCAST java/lang/Integer
 INVOKEVIRTUAL java/lang/Integer.intValue()I
 ILOAD 8
-IF_ICMPEQ L594
+IF_ICMPEQ L593
 CONST Integer:1
 ALOAD 45
 MONITOREXIT
@@ -776,21 +775,21 @@ AASTORE
 INVOKESTATIC local/lito381/server/CombatAccess.call(Ljava/lang/Object;Ljava/lang/String;Ljava/lang/String;[Ljava/lang/Object;)Ljava/lang/Object;
 CHECKCAST java/lang/Boolean
 INVOKEVIRTUAL java/lang/Boolean.booleanValue()Z
-IFEQ L615
+IFEQ L614
 ILOAD 26
 CONST Integer:1
-IF_ICMPLE L615
+IF_ICMPLE L614
 CONST Integer:1
 ALOAD 45
 MONITOREXIT
 IRETURN
 ILOAD 21
-IFLT L622
+IFLT L621
 ALOAD 2
 ILOAD 19
 ILOAD 20
 INVOKESTATIC local/lito381/server/NativeSourceNpcEscape.door(Ljava/lang/Object;II)Z
-IFEQ L643
+IFEQ L642
 ALOAD 23
 CONST str:"schedule"
 CONST str:"(JI)V"
@@ -815,9 +814,9 @@ IRETURN
 ALOAD 18
 CONST str:"nativeSourceEscapeMoveForAi"
 ILOAD 17
-IFEQ L649
+IFEQ L648
 CONST str:"BattleQuestEncounter"
-GOTO L650
+GOTO L649
 CONST str:"BattleTimeAttackEncounter"
 INVOKEDYNAMIC makeConcatWithConstants(Ljava/lang/String;)Ljava/lang/String; bsm=H6 java/lang/invoke/StringConcatFactory.makeConcatWithConstants(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;Ljava/lang/String;[Ljava/lang/Object;)Ljava/lang/invoke/CallSite; args=[str:"(JIIJ)Llocal/lito381/server/\u0001$Snapshot;"]
 CONST Integer:4
@@ -845,14 +844,14 @@ AASTORE
 INVOKESTATIC local/lito381/server/CombatAccess.call(Ljava/lang/Object;Ljava/lang/String;Ljava/lang/String;[Ljava/lang/Object;)Ljava/lang/Object;
 ASTORE 47
 ALOAD 47
-IFNONNULL L681
+IFNONNULL L680
 CONST Integer:1
 ALOAD 45
 MONITOREXIT
 IRETURN
 ALOAD 45
 MONITOREXIT
-GOTO L689
+GOTO L688
 ASTORE 48
 ALOAD 45
 MONITOREXIT
```

</details>

## 附錄：PhysicalNpc 核准方法完整左右內容

<details>
<summary>sourceCurrent(Llocal/lito381/server/NativeSourcePhysicalNpc$Origin;)Z</summary>

```text
method sourceCurrent(Llocal/lito381/server/NativeSourcePhysicalNpc$Origin;)Z insn[2]: left=ISTORE 1 right=DUP
LEFT
(Llocal/lito381/server/NativeSourcePhysicalNpc$Origin;)Z
0x8


||||pvis=|pinv=|lvtvis=|lvtinv=

[]
[ALOAD 0, INVOKESTATIC local/lito381/server/NativeSourceRawNpc.sourceState(Llocal/lito381/server/NativeSourcePhysicalNpc$Origin;)I, ISTORE 1, ILOAD 1, IFLT L11, ILOAD 1, IFEQ L9, CONST Integer:1, GOTO L10, CONST Integer:0, IRETURN, ALOAD 0, INVOKEVIRTUAL local/lito381/server/NativeSourcePhysicalNpc$Origin.source()Llocal/lito381/server/NativeSourceDamageDisplay$Actor;, INVOKEVIRTUAL local/lito381/server/NativeSourceDamageDisplay$Actor.pc()Llocal/lito381/server/ClientSession;, ASTORE 2, ALOAD 0, INVOKEVIRTUAL local/lito381/server/NativeSourcePhysicalNpc$Origin.source()Llocal/lito381/server/NativeSourceDamageDisplay$Actor;, ALOAD 0, INVOKEVIRTUAL local/lito381/server/NativeSourcePhysicalNpc$Origin.source()Llocal/lito381/server/NativeSourceDamageDisplay$Actor;, INVOKEVIRTUAL local/lito381/server/NativeSourceDamageDisplay$Actor.life()Ljava/lang/Object;, INVOKEVIRTUAL local/lito381/server/NativeSourceDamageDisplay$Actor.valid(Ljava/lang/Object;)Z, IFEQ L58, ALOAD 2, CONST str:"playerHp", INVOKESTATIC local/lito381/server/NativeSourceDamageDisplay.n(Llocal/lito381/server/ClientSession;Ljava/lang/String;)I, IFLE L58, ALOAD 2, CONST str:"observer", INVOKESTATIC local/lito381/server/CombatAccess.field(Ljava/lang/Object;Ljava/lang/String;)Ljava/lang/Object;, CHECKCAST java/lang/Boolean, INVOKEVIRTUAL java/lang/Boolean.booleanValue()Z, IFNE L58, ALOAD 2, CONST str:"hub", INVOKESTATIC local/lito381/server/CombatAccess.field(Ljava/lang/Object;Ljava/lang/String;)Ljava/lang/Object;, ALOAD 0, INVOKEVIRTUAL local/lito381/server/NativeSourcePhysicalNpc$Origin.scene()Llocal/lito381/server/NativeSourceCubeOrigin$Scene;, INVOKEVIRTUAL local/lito381/server/NativeSourceCubeOrigin$Scene.hub()Llocal/lito381/server/GameHub;, IF_ACMPNE L58, ALOAD 2, CONST str:"phase", INVOKESTATIC local/lito381/server/CombatAccess.field(Ljava/lang/Object;Ljava/lang/String;)Ljava/lang/Object;, INVOKESTATIC java/lang/String.valueOf(Ljava/lang/Object;)Ljava/lang/String;, CONST str:"BATTLE", INVOKEVIRTUAL java/lang/String.equals(Ljava/lang/Object;)Z, IFEQ L58, ALOAD 2, CONST str:"currentRoomId", INVOKESTATIC local/lito381/server/CombatAccess.field(Ljava/lang/Object;Ljava/lang/String;)Ljava/lang/Object;, CHECKCAST java/lang/Long, INVOKEVIRTUAL java/lang/Long.longValue()J, ALOAD 0, INVOKEVIRTUAL local/lito381/server/NativeSourcePhysicalNpc$Origin.scene()Llocal/lito381/server/NativeSourceCubeOrigin$Scene;, INVOKEVIRTUAL local/lito381/server/NativeSourceCubeOrigin$Scene.id()J, LCMP, IFNE L58, CONST Integer:1, GOTO L59, CONST Integer:0, IRETURN]
RIGHT
(Llocal/lito381/server/NativeSourcePhysicalNpc$Origin;)Z
0x8


||||pvis=|pinv=|lvtvis=|lvtinv=

[]
[ALOAD 0, INVOKESTATIC local/lito381/server/NativeSourceRawNpc.sourceState(Llocal/lito381/server/NativeSourcePhysicalNpc$Origin;)I, DUP, IFLT L5, IRETURN, POP, ALOAD 0, INVOKEVIRTUAL local/lito381/server/NativeSourcePhysicalNpc$Origin.source()Llocal/lito381/server/NativeSourceDamageDisplay$Actor;, INVOKEVIRTUAL local/lito381/server/NativeSourceDamageDisplay$Actor.pc()Llocal/lito381/server/ClientSession;, ASTORE 1, ALOAD 0, INVOKEVIRTUAL local/lito381/server/NativeSourcePhysicalNpc$Origin.source()Llocal/lito381/server/NativeSourceDamageDisplay$Actor;, ALOAD 0, INVOKEVIRTUAL local/lito381/server/NativeSourcePhysicalNpc$Origin.source()Llocal/lito381/server/NativeSourceDamageDisplay$Actor;, INVOKEVIRTUAL local/lito381/server/NativeSourceDamageDisplay$Actor.life()Ljava/lang/Object;, INVOKEVIRTUAL local/lito381/server/NativeSourceDamageDisplay$Actor.valid(Ljava/lang/Object;)Z, IFEQ L53, ALOAD 1, CONST str:"playerHp", INVOKESTATIC local/lito381/server/NativeSourceDamageDisplay.n(Llocal/lito381/server/ClientSession;Ljava/lang/String;)I, IFLE L53, ALOAD 1, CONST str:"observer", INVOKESTATIC local/lito381/server/CombatAccess.field(Ljava/lang/Object;Ljava/lang/String;)Ljava/lang/Object;, CHECKCAST java/lang/Boolean, INVOKEVIRTUAL java/lang/Boolean.booleanValue()Z, IFNE L53, ALOAD 1, CONST str:"hub", INVOKESTATIC local/lito381/server/CombatAccess.field(Ljava/lang/Object;Ljava/lang/String;)Ljava/lang/Object;, ALOAD 0, INVOKEVIRTUAL local/lito381/server/NativeSourcePhysicalNpc$Origin.scene()Llocal/lito381/server/NativeSourceCubeOrigin$Scene;, INVOKEVIRTUAL local/lito381/server/NativeSourceCubeOrigin$Scene.hub()Llocal/lito381/server/GameHub;, IF_ACMPNE L53, ALOAD 1, CONST str:"phase", INVOKESTATIC local/lito381/server/CombatAccess.field(Ljava/lang/Object;Ljava/lang/String;)Ljava/lang/Object;, INVOKESTATIC java/lang/String.valueOf(Ljava/lang/Object;)Ljava/lang/String;, CONST str:"BATTLE", INVOKEVIRTUAL java/lang/String.equals(Ljava/lang/Object;)Z, IFEQ L53, ALOAD 1, CONST str:"currentRoomId", INVOKESTATIC local/lito381/server/CombatAccess.field(Ljava/lang/Object;Ljava/lang/String;)Ljava/lang/Object;, CHECKCAST java/lang/Long, INVOKEVIRTUAL java/lang/Long.longValue()J, ALOAD 0, INVOKEVIRTUAL local/lito381/server/NativeSourcePhysicalNpc$Origin.scene()Llocal/lito381/server/NativeSourceCubeOrigin$Scene;, INVOKEVIRTUAL local/lito381/server/NativeSourceCubeOrigin$Scene.id()J, LCMP, IFNE L53, CONST Integer:1, GOTO L54, CONST Integer:0, IRETURN]
```

</details>

<details>
<summary>targetCurrent(Llocal/lito381/server/NativeSourcePhysicalNpc$Origin;)Z</summary>

```text
method targetCurrent(Llocal/lito381/server/NativeSourcePhysicalNpc$Origin;)Z insn[2]: left=ISTORE 1 right=DUP
LEFT
(Llocal/lito381/server/NativeSourcePhysicalNpc$Origin;)Z
0x8


||||pvis=|pinv=|lvtvis=|lvtinv=

[]
[ALOAD 0, INVOKESTATIC local/lito381/server/NativeSourceGroundNpc.targetState(Llocal/lito381/server/NativeSourcePhysicalNpc$Origin;)I, ISTORE 1, ILOAD 1, IFLT L11, ILOAD 1, IFEQ L9, CONST Integer:1, GOTO L10, CONST Integer:0, IRETURN, ALOAD 0, INVOKEVIRTUAL local/lito381/server/NativeSourcePhysicalNpc$Origin.target()Llocal/lito381/server/NativeSourceCubeOrigin$Target;, ASTORE 2, ALOAD 2, INVOKEVIRTUAL local/lito381/server/NativeSourceCubeOrigin$Target.owner()Ljava/lang/Object;, CONST str:"sourceProcEncounterGeneration", INVOKESTATIC local/lito381/server/CombatAccess.field(Ljava/lang/Object;Ljava/lang/String;)Ljava/lang/Object;, ALOAD 2, INVOKEVIRTUAL local/lito381/server/NativeSourceCubeOrigin$Target.generation()Ljava/lang/Object;, IF_ACMPNE L67, ALOAD 2, INVOKEVIRTUAL local/lito381/server/NativeSourceCubeOrigin$Target.actor()Ljava/lang/Object;, INVOKESTATIC local/lito381/server/NativeSourceArenaPoison.rawLife(Ljava/lang/Object;)Ljava/lang/Object;, ALOAD 2, INVOKEVIRTUAL local/lito381/server/NativeSourceCubeOrigin$Target.life()Ljava/lang/Object;, IF_ACMPNE L67, ALOAD 2, INVOKEVIRTUAL local/lito381/server/NativeSourceCubeOrigin$Target.actor()Ljava/lang/Object;, CONST str:"objectId", INVOKESTATIC local/lito381/server/CombatAccess.field(Ljava/lang/Object;Ljava/lang/String;)Ljava/lang/Object;, CHECKCAST java/lang/Long, INVOKEVIRTUAL java/lang/Long.longValue()J, ALOAD 2, INVOKEVIRTUAL local/lito381/server/NativeSourceCubeOrigin$Target.id()J, LCMP, IFNE L67, ALOAD 2, INVOKEVIRTUAL local/lito381/server/NativeSourceCubeOrigin$Target.actor()Ljava/lang/Object;, CONST str:"hp", INVOKESTATIC local/lito381/server/CombatAccess.field(Ljava/lang/Object;Ljava/lang/String;)Ljava/lang/Object;, CHECKCAST java/lang/Integer, INVOKEVIRTUAL java/lang/Integer.intValue()I, IFLE L67, ALOAD 2, INVOKEVIRTUAL local/lito381/server/NativeSourceCubeOrigin$Target.actor()Ljava/lang/Object;, CONST str:"x", INVOKESTATIC local/lito381/server/CombatAccess.field(Ljava/lang/Object;Ljava/lang/String;)Ljava/lang/Object;, CHECKCAST java/lang/Integer, INVOKEVIRTUAL java/lang/Integer.intValue()I, ALOAD 0, INVOKEVIRTUAL local/lito381/server/NativeSourcePhysicalNpc$Origin.x()I, IF_ICMPNE L67, ALOAD 2, INVOKEVIRTUAL local/lito381/server/NativeSourceCubeOrigin$Target.actor()Ljava/lang/Object;, CONST str:"y", INVOKESTATIC local/lito381/server/CombatAccess.field(Ljava/lang/Object;Ljava/lang/String;)Ljava/lang/Object;, CHECKCAST java/lang/Integer, INVOKEVIRTUAL java/lang/Integer.intValue()I, ALOAD 0, INVOKEVIRTUAL local/lito381/server/NativeSourcePhysicalNpc$Origin.y()I, IF_ICMPNE L67, ALOAD 2, INVOKESTATIC local/lito381/server/NativeSourceCubeNpc.member(Llocal/lito381/server/NativeSourceCubeOrigin$Target;)Z, IFEQ L67, CONST Integer:1, GOTO L68, CONST Integer:0, IRETURN]
RIGHT
(Llocal/lito381/server/NativeSourcePhysicalNpc$Origin;)Z
0x8


||||pvis=|pinv=|lvtvis=|lvtinv=

[]
[ALOAD 0, INVOKESTATIC local/lito381/server/NativeSourceGroundNpc.targetState(Llocal/lito381/server/NativeSourcePhysicalNpc$Origin;)I, DUP, IFLT L5, IRETURN, POP, ALOAD 0, INVOKEVIRTUAL local/lito381/server/NativeSourcePhysicalNpc$Origin.target()Llocal/lito381/server/NativeSourceCubeOrigin$Target;, ASTORE 1, ALOAD 1, INVOKEVIRTUAL local/lito381/server/NativeSourceCubeOrigin$Target.owner()Ljava/lang/Object;, CONST str:"sourceProcEncounterGeneration", INVOKESTATIC local/lito381/server/CombatAccess.field(Ljava/lang/Object;Ljava/lang/String;)Ljava/lang/Object;, ALOAD 1, INVOKEVIRTUAL local/lito381/server/NativeSourceCubeOrigin$Target.generation()Ljava/lang/Object;, IF_ACMPNE L62, ALOAD 1, INVOKEVIRTUAL local/lito381/server/NativeSourceCubeOrigin$Target.actor()Ljava/lang/Object;, INVOKESTATIC local/lito381/server/NativeSourceArenaPoison.rawLife(Ljava/lang/Object;)Ljava/lang/Object;, ALOAD 1, INVOKEVIRTUAL local/lito381/server/NativeSourceCubeOrigin$Target.life()Ljava/lang/Object;, IF_ACMPNE L62, ALOAD 1, INVOKEVIRTUAL local/lito381/server/NativeSourceCubeOrigin$Target.actor()Ljava/lang/Object;, CONST str:"objectId", INVOKESTATIC local/lito381/server/CombatAccess.field(Ljava/lang/Object;Ljava/lang/String;)Ljava/lang/Object;, CHECKCAST java/lang/Long, INVOKEVIRTUAL java/lang/Long.longValue()J, ALOAD 1, INVOKEVIRTUAL local/lito381/server/NativeSourceCubeOrigin$Target.id()J, LCMP, IFNE L62, ALOAD 1, INVOKEVIRTUAL local/lito381/server/NativeSourceCubeOrigin$Target.actor()Ljava/lang/Object;, CONST str:"hp", INVOKESTATIC local/lito381/server/CombatAccess.field(Ljava/lang/Object;Ljava/lang/String;)Ljava/lang/Object;, CHECKCAST java/lang/Integer, INVOKEVIRTUAL java/lang/Integer.intValue()I, IFLE L62, ALOAD 1, INVOKEVIRTUAL local/lito381/server/NativeSourceCubeOrigin$Target.actor()Ljava/lang/Object;, CONST str:"x", INVOKESTATIC local/lito381/server/CombatAccess.field(Ljava/lang/Object;Ljava/lang/String;)Ljava/lang/Object;, CHECKCAST java/lang/Integer, INVOKEVIRTUAL java/lang/Integer.intValue()I, ALOAD 0, INVOKEVIRTUAL local/lito381/server/NativeSourcePhysicalNpc$Origin.x()I, IF_ICMPNE L62, ALOAD 1, INVOKEVIRTUAL local/lito381/server/NativeSourceCubeOrigin$Target.actor()Ljava/lang/Object;, CONST str:"y", INVOKESTATIC local/lito381/server/CombatAccess.field(Ljava/lang/Object;Ljava/lang/String;)Ljava/lang/Object;, CHECKCAST java/lang/Integer, INVOKEVIRTUAL java/lang/Integer.intValue()I, ALOAD 0, INVOKEVIRTUAL local/lito381/server/NativeSourcePhysicalNpc$Origin.y()I, IF_ICMPNE L62, ALOAD 1, INVOKESTATIC local/lito381/server/NativeSourceCubeNpc.member(Llocal/lito381/server/NativeSourceCubeOrigin$Target;)Z, IFEQ L62, CONST Integer:1, GOTO L63, CONST Integer:0, IRETURN]
```

</details>

## 附錄：兩個 producer 的完整 javap

<details>
<summary>NativeSourceRawNpc</summary>

```text
  static int sourceState(local.lito381.server.NativeSourcePhysicalNpc$Origin);
    descriptor: (Llocal/lito381/server/NativeSourcePhysicalNpc$Origin;)I
    Code:
       0: aload_0
       1: ifnull        24
       4: aload_0
       5: getfield      #200                // Field local/lito381/server/NativeSourcePhysicalNpc$Origin.nativeSourceRawNpcSource:Ljava/lang/Object;
       8: astore_2
       9: aload_2
      10: instanceof    #120                // class local/lito381/server/NativeSourceRawNpc$Source
      13: ifeq          24
      16: aload_2
      17: checkcast     #120                // class local/lito381/server/NativeSourceRawNpc$Source
      20: astore_1
      21: goto          26
      24: iconst_m1
      25: ireturn
      26: aload_1
      27: invokevirtual #171                // Method local/lito381/server/NativeSourceRawNpc$Source.actor:()Llocal/lito381/server/NativeSourceDamageDisplay$Actor;
      30: astore_2
      31: aload_2
      32: invokevirtual #204                // Method local/lito381/server/NativeSourceDamageDisplay$Actor.pc:()Llocal/lito381/server/ClientSession;
      35: astore_3
      36: aload_0
      37: invokevirtual #187                // Method local/lito381/server/NativeSourcePhysicalNpc$Origin.source:()Llocal/lito381/server/NativeSourceDamageDisplay$Actor;
      40: aload_2
      41: if_acmpne     211
      44: aload_2
      45: invokevirtual #233                // Method local/lito381/server/NativeSourceDamageDisplay$Actor.binding:()Llocal/lito381/server/NativePlayerCommit$Binding;
      48: aload_3
      49: invokestatic  #236                // Method local/lito381/server/NativeAcceptedFreezeMutation.roleFrame:(Llocal/lito381/server/NativePlayerCommit$Binding;Llocal/lito381/server/ClientSession;)Z
      52: ifeq          211
      55: aload_3
      56: ldc           #242                // String sourceItemDisconnected
      58: invokestatic  #104                // Method local/lito381/server/CombatAccess.field:(Ljava/lang/Object;Ljava/lang/String;)Ljava/lang/Object;
      61: checkcast     #244                // class java/lang/Boolean
      64: invokevirtual #246                // Method java/lang/Boolean.booleanValue:()Z
      67: ifne          211
      70: aload_3
      71: ldc           #249                // String observer
      73: invokestatic  #104                // Method local/lito381/server/CombatAccess.field:(Ljava/lang/Object;Ljava/lang/String;)Ljava/lang/Object;
      76: checkcast     #244                // class java/lang/Boolean
      79: invokevirtual #246                // Method java/lang/Boolean.booleanValue:()Z
      82: ifne          211
      85: aload_3
      86: invokestatic  #251                // Method local/lito381/server/NativeSourceControlledDisplay.rawLife:(Ljava/lang/Object;)Ljava/lang/Object;
      89: aload_2
      90: invokevirtual #256                // Method local/lito381/server/NativeSourceDamageDisplay$Actor.life:()Ljava/lang/Object;
      93: if_acmpne     211
      96: aload_1
      97: invokevirtual #257                // Method local/lito381/server/NativeSourceRawNpc$Source.position:()Z
     100: ifeq          131
     103: aload_3
     104: ldc_w         #259                // String positionX
     107: invokestatic  #209                // Method local/lito381/server/NativeSourceDamageDisplay.n:(Llocal/lito381/server/ClientSession;Ljava/lang/String;)I
     110: aload_2
     111: invokevirtual #261                // Method local/lito381/server/NativeSourceDamageDisplay$Actor.x:()I
     114: if_icmpne     211
     117: aload_3
     118: ldc_w         #263                // String positionY
     121: invokestatic  #209                // Method local/lito381/server/NativeSourceDamageDisplay.n:(Llocal/lito381/server/ClientSession;Ljava/lang/String;)I
     124: aload_2
     125: invokevirtual #265                // Method local/lito381/server/NativeSourceDamageDisplay$Actor.y:()I
     128: if_icmpne     211
     131: aload_1
     132: invokevirtual #267                // Method local/lito381/server/NativeSourceRawNpc$Source.dead:()Z
     135: ifeq          147
     138: aload_3
     139: ldc           #207                // String playerHp
     141: invokestatic  #209                // Method local/lito381/server/NativeSourceDamageDisplay.n:(Llocal/lito381/server/ClientSession;Ljava/lang/String;)I
     144: ifne          211
     147: aload_3
     148: ldc_w         #269                // String hub
     151: invokestatic  #104                // Method local/lito381/server/CombatAccess.field:(Ljava/lang/Object;Ljava/lang/String;)Ljava/lang/Object;
     154: aload_0
     155: invokevirtual #271                // Method local/lito381/server/NativeSourcePhysicalNpc$Origin.scene:()Llocal/lito381/server/NativeSourceCubeOrigin$Scene;
     158: invokevirtual #274                // Method local/lito381/server/NativeSourceCubeOrigin$Scene.hub:()Llocal/lito381/server/GameHub;
     161: if_acmpne     211
     164: aload_3
     165: ldc_w         #279                // String phase
     168: invokestatic  #104                // Method local/lito381/server/CombatAccess.field:(Ljava/lang/Object;Ljava/lang/String;)Ljava/lang/Object;
     171: invokestatic  #281                // Method java/lang/String.valueOf:(Ljava/lang/Object;)Ljava/lang/String;
     174: ldc_w         #286                // String BATTLE
     177: invokevirtual #288                // Method java/lang/String.equals:(Ljava/lang/Object;)Z
     180: ifeq          211
     183: aload_3
     184: ldc_w         #292                // String currentRoomId
     187: invokestatic  #104                // Method local/lito381/server/CombatAccess.field:(Ljava/lang/Object;Ljava/lang/String;)Ljava/lang/Object;
     190: checkcast     #24                 // class java/lang/Long
     193: invokevirtual #294                // Method java/lang/Long.longValue:()J
     196: aload_0
     197: invokevirtual #271                // Method local/lito381/server/NativeSourcePhysicalNpc$Origin.scene:()Llocal/lito381/server/NativeSourceCubeOrigin$Scene;
     200: invokevirtual #298                // Method local/lito381/server/NativeSourceCubeOrigin$Scene.id:()J
     203: lcmp
     204: ifne          211
     207: iconst_1
     208: goto          212
     211: iconst_0
     212: istore        4
     214: iload         4
     216: ifeq          223
     219: iconst_1
     220: goto          224
     223: iconst_0
     224: ireturn
```

</details>

<details>
<summary>NativeSourceGroundNpc</summary>

```text
  static int targetState(local.lito381.server.NativeSourcePhysicalNpc$Origin);
    descriptor: (Llocal/lito381/server/NativeSourcePhysicalNpc$Origin;)I
    Code:
       0: aload_0
       1: getfield      #540                // Field local/lito381/server/NativeSourcePhysicalNpc$Origin.nativeSourceGroundNpcProof:Ljava/lang/Object;
       4: astore_2
       5: aload_2
       6: instanceof    #64                 // class local/lito381/server/NativeSourceGroundNpc$Proof
       9: ifeq          20
      12: aload_2
      13: checkcast     #64                 // class local/lito381/server/NativeSourceGroundNpc$Proof
      16: astore_1
      17: goto          22
      20: iconst_m1
      21: ireturn
      22: aload_1
      23: invokevirtual #81                 // Method local/lito381/server/NativeSourceGroundNpc$Proof.entity:()Llocal/lito381/server/NativeSourceGroundOrigin$Entity;
      26: invokevirtual #1                  // Method local/lito381/server/NativeSourceGroundOrigin$Entity.scene:()Llocal/lito381/server/NativeSourceGroundOrigin$Scene;
      29: astore_2
      30: aload_0
      31: invokevirtual #527                // Method local/lito381/server/NativeSourcePhysicalNpc$Origin.target:()Llocal/lito381/server/NativeSourceCubeOrigin$Target;
      34: aload_1
      35: invokevirtual #63                 // Method local/lito381/server/NativeSourceGroundNpc$Proof.selected:()Llocal/lito381/server/NativeSourceGroundNpc$Selected;
      38: invokevirtual #69                 // Method local/lito381/server/NativeSourceGroundNpc$Selected.target:()Llocal/lito381/server/NativeSourceCubeOrigin$Target;
      41: if_acmpne     84
      44: aload_1
      45: invokestatic  #407                // Method allowed:(Llocal/lito381/server/NativeSourceGroundNpc$Proof;)Z
      48: ifeq          84
      51: aload_0
      52: invokevirtual #527                // Method local/lito381/server/NativeSourcePhysicalNpc$Origin.target:()Llocal/lito381/server/NativeSourceCubeOrigin$Target;
      55: new           #281                // class local/lito381/server/NativeSourceCubeOrigin$Scene
      58: dup
      59: aload_2
      60: invokevirtual #7                  // Method local/lito381/server/NativeSourceGroundOrigin$Scene.hub:()Llocal/lito381/server/GameHub;
      63: aload_2
      64: invokevirtual #47                 // Method local/lito381/server/NativeSourceGroundOrigin$Scene.room:()Ljava/lang/Object;
      67: aload_2
      68: invokevirtual #106                // Method local/lito381/server/NativeSourceGroundOrigin$Scene.id:()J
      71: invokespecial #283                // Method local/lito381/server/NativeSourceCubeOrigin$Scene."<init>":(Llocal/lito381/server/GameHub;Ljava/lang/Object;J)V
      74: invokestatic  #286                // Method local/lito381/server/NativeSourceCubeNpc.originalCurrent:(Llocal/lito381/server/NativeSourceCubeOrigin$Target;Llocal/lito381/server/NativeSourceCubeOrigin$Scene;)Z
      77: ifeq          84
      80: iconst_1
      81: goto          85
      84: iconst_0
      85: ireturn
```

</details>
