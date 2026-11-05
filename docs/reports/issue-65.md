# Issue #65 — T020-B04 戰鬥併入暫存

## 結論

**待主控裁定，B04 尚未通過全部語意驗收。** 51 個 staged Java 已完成本輪移植並整批編譯，直接 javac 產物經既有轉換器處理後有 98 個 class，清單完整。預設 semantic-diff 結果為 **SAME 95、DIFF 3、ONLY_LEFT 0、ONLY_RIGHT 0**。逐方法複核定位為 3 個 entry 的 6 個方法，下方附全部正規化差異，交 `needs:claude`。

本報告取代前次 `c30740d` 的完成宣告與 ASM 疊回交接。本輪驗收使用本次 Java 編譯及已合併轉換器的產物。驗收方法保持完整預設比較。

## 做了什麼

1. 在既有 `task/65` worktree，將主線快轉至 `450178d59b0157fddf2b9456b7c03d20207993fc`，取得已合併 #102／#135／#153／#166。再合入遠端 `task/65` 的 `c30740debf2308a7ecb47e9bbb911b73e5f7d407` 歷史，保留前次提交以供正常推送。
2. 依 `batches.json` 與凍結 `manifest.json` 重新選取 51 個 Java 起點。外層標為 asm-only 者取同檔內部類別的凍結來源，`NativeSourceNpcDamageDisplay` 選 `$Proof` 變體。25 檔與選定凍結來源 bytes 相同，12 檔僅整理行尾空白與換行，14 檔移植後補上可讀排版。
3. 以 CFR 0.152 反編譯指定 12 個 asm-only entry 所屬的 7 個外層類別，逐方法對照移植。另比對 FactoryType、FactoryMemory、NpcEscape、Pair trampoline 與 PlayerView 的候選改動，補齊整批來源依賴。
4. 補入目標 first-life／negative-final-frame、NPC 工廠欄位與重載建構子、原始目標與 clock scope、NPC HP／magic 輸出、physical audience／raw／ground guard、成對呼叫轉接等候選邏輯。
5. 三個 record 使用 `@AsRecord`，分別為 `NativeSourceNpcFactoryType$Stamp`、`NativeSourceNpcPlayer$Scope`、`NativeSourcePhysicalNpc$Origin`。Stamp 的六個額外 final 欄位交由 `finalFields` 還原，以保留部分建構子中的 JVM 預設值。
6. `NativeSourcePhysicalNpc$Commit.nativeSourcePhysicalAudience` 的舊建構子同樣保留預設值，新增重載才賦值。本輪依已合併 #153 的一般類別欄位方案使用 `@AsmFinal`，由同一支 `tools/as-record` 轉換器恢復 final，並保留 transient。該 Commit 已比對 SAME。此處是依 #153 通用契約作出的實作選擇，請主控一併確認本批適用性。
7. 保留候選轉接後仍存在的 private lambda producer 本體，實際 producer 呼叫轉向 `NativeSourceNegativeFinalFrame.produce`，全部候選方法納入逐項驗證。

14 個實際移植檔案如下，其餘 37 檔沿用凍結 Java。

- `NativeSourceMagicNpc.java`
- `NativeSourceMassSlowTargets.java`
- `NativeSourceMonsterHp.java`
- `NativeSourceNegativeTargets.java`
- `NativeSourceNpcDamageDisplay.java`
- `NativeSourceNpcEscape.java`
- `NativeSourceNpcFactoryMemory.java`
- `NativeSourceNpcFactoryType.java`
- `NativeSourceNpcPlayer.java`
- `NativeSourceNpcPlayerView.java`
- `NativeSourceOuterPair.java`
- `NativeSourcePairAdmission.java`
- `NativeSourcePairedScope.java`
- `NativeSourcePhysicalNpc.java`

## 已驗證

### 編譯、轉換與清單

Windows／Microsoft OpenJDK 17.0.17，`--release 17 -encoding UTF-8`，ASM 9.7。使用全新的 `target/issue-65/final/classes` 整批編譯 51 檔。支援 annotation 獨立編到 `annotations/` 並加入 javac classpath，以取得候選 JAR 尚未包含的 `AsRecord`／`AsmFinal` 定義。驗收 JAR 包含整個批次 classes 目錄。

```text
javac exit=0 sources=51
Note: NativeSourceNpcPlayerView.java uses unchecked or unsafe operations.
Note: Recompile with -Xlint:unchecked for details.
as-record: scanned=98 converted=4
converter changed=NativeSourceNpcFactoryType$Stamp.class,NativeSourceNpcPlayer$Scope.class,NativeSourcePhysicalNpc$Commit.class,NativeSourcePhysicalNpc$Origin.class
class inventory total=98 missing=0 extra=0
SUMMARY SAME=95 SAME_EFFECTS=0 SAME_LAYOUT=0 DEVIATION=0 DIFF=3 ONLY_LEFT=0 ONLY_RIGHT=0 BYTE_SAME=0 ALLOW_METHODS=0
{"missingLeft":[],"extraLeft":[]}
different methods=6
```

轉換前後 SHA-256 清單確認僅四個標註 class 有改變。候選 JAR 僅供 javac 依賴、反編譯及右側比較。另提供 `--left-classpath` candidate/base 供 #166 解析外部 field owner，這些依賴各自獨立於左側驗收集合。

已從下方報告區塊擷取 PowerShell 指令，使用另一個全新目錄 `target/issue-65/recheck-verified` 再編譯及比較，結果同為 95 SAME／3 DIFF，98 個 class bytes 逐一與 final 相同。複驗日誌為 `scratch/issue-65/recheck.log`。依賴首次在沙箱內下載遇 TLS 認證失敗，經核准重試下載後三個 SHA-256 全部通過。`git diff --check HEAD` 與變更範圍核對通過，檔案集合精確為 51 個 B04 Java 加本報告。

| 輸入或產物 | SHA-256 |
|---|---|
| candidate-50635f7e.jar | `50635f7edca8ec6ce77787cc32371c7590b497e48ea202f8a679caf3f517b729` |
| base-0.6.71.jar | `e1498871c7be6b287821c57ef5d95933db1740d9b4d4a80f6c3465f21d99d498` |
| 本次 final/b04.jar | `5ccec2e8573d427fd98a8de93776a6b4ef81966c45ae16b1d26d358f5d41b7e6` |
| CFR 0.152 | `f686e8f3ded377d7bc87d216a90e9e9512df4156e75b06c655a16648ae8765b2` |

JAR 雜湊包含封裝時間，重新打包時以清單和語意結果核對。class 比對已忽略 debug 資訊。

### 待裁定的三個 entry

| Entry，皆位於 `local/lito381/server/` | 方法 | 已觀察差異與理由 |
|---|---|---|
| `NativeSourceNpcDamageDisplay.class` | `questDamage`、`timeDamage` | 候選在各個正常 return 的 MONITOREXIT 之後、ARETURN 之前插入 `acceptedNatural(receipt)`，異常出口跳過回呼。CFR 對早退輸出 `MONITOREXIT @DISABLED`，直接照抄會把早退回呼放進 monitor。本輪 Java 將結果保存後離開 synchronized，再呼叫回呼，保留正常／異常出口與鎖界線，但共用出口的 try 區段與控制流程未通過預設線性比較。 |
| `NativeSourceNpcEscape.class` | `nativeSourceFirstViewHandled`、`nativeSourceNpcPlayerFirstHandled` | 保護區段及其餘正規化指令已一致。剩一組反向分支加 GOTO，相對候選的單一條件跳躍。以 frozen body、CFR body、巢狀標籤版移植後，最後仍保留此一排列差異。尚未證明 javac 完全無法產生目標排列，主控可裁定補正規化或指定另一個 Java 形狀。 |
| `NativeSourcePhysicalNpc.class` | `sourceCurrent`、`targetCurrent` | 候選呼叫回傳 int 的 `sourceState`／`targetState` 後，直接 `DUP; IFLT; IRETURN` 作 boolean 回傳，負值走原本 fallback。Java 的 boolean 方法必須以布林運算表達此值，因此使用 `state >= 0` 分支及 `state != 0`。已讀取候選兩個 producer，其返回常數集合為 -1／0／1。預設比較保留 int→boolean 指令形狀差異。 |

逐方法稽核使用同一份已合併 `JarIndex`／`ClassComparer.compareMethod(..., false)`，遍歷全部 98 entries 的正規化方法鍵，得到上述六筆，缺少方法數為零。下方 diff 由 `ClassComparer.tryCatch` 與 `ClassComparer.instructions` 原始完整列表產生，列出六個方法所有差異 hunk，左側是 staged、右側是 candidate，無裁切指令或縮略欄位／方法描述子。

## 推測

- Escape 的條件反向加 GOTO 可由保持分支目標的正規化或既有 CFG 比較處理，是否新增核准交主控。
- DamageDisplay 的共用正常出口應保留候選鎖界線與回呼時序，目前缺少經核准的語意證明。
- PhysicalNpc 的三態 producer 在本候選回傳集合下可由布林條件表達，但需主控裁定受限的 int／boolean 正規化。三項皆保留 DIFF。

## 待驗證／未驗證什麼

- 98 個 entry 全部 SAME 的驗收仍有上述三類阻塞。
- 單元測試與行為回歸依本批設計交 B-land。暫存區未執行應用邏輯。
- 尚待主控審查與本次推送後的 CI 狀態確認。

## 產出路徑

- `lito伺服器/LiTo381-Server/merge-inputs/50635f7e/staged/local/lito381/server/`，精確 B04 的 51 個 `.java`。
- `docs/reports/issue-65.md`，本報告。
- 本機可重建證據在 `scratch/issue-65/`，包含 `sources.json`、`entries.txt`、`final-verification.log`、`final-semantic.json`、`method-audit.log`、`remaining-differences.json`、`cfr/` 與 `final-left/`／`final-right/`。
- 建置產物在伺服器 `target/issue-65/final/`。scratch 與 target 由忽略規則管理。

## 給下一棒的提示

請 Claude 主控依本報告六個方法的完整差異處理 `needs:claude`，先裁定正規化、精確核准或 Java 寫法，再交本機重跑全批 98 個 entry。B-land 須等待全批驗收。請保留三個 AsRecord 宣告與 Commit 的 final 欄位契約，於整批 javac 後執行已合併轉換器。

## 可重現指令

在本 worktree 根目錄使用 PowerShell 7 執行。以下使用全新的 `target/issue-65/recheck`，若重跑請替換為另一個全新目錄。ASM 依賴從 Maven Central 下載到任務 target 並核對既有工具宣告的 SHA-256。可將區塊保存到 `scratch/issue-65/recheck.ps1` 執行，全部檔案寫入 worktree。執行後須檢查 counts 與 classSelection，CLI 的 exit 0 只表示報告成功產生。

```powershell
$ErrorActionPreference = 'Stop'
$root = (Get-Location).Path
$server = Join-Path $root 'lito伺服器/LiTo381-Server'
$evidence = Join-Path $root 'scratch/issue-65'
$output = Join-Path $server 'target/issue-65/recheck'
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
& "$jdk/java.exe" '-Dfile.encoding=UTF-8' -cp "$semantic;$deps" local.lito381.semanticdiff.SemanticDiff --left $jar --right $candidate --left-classpath $candidate --left-classpath $base --classes $entries --json "$evidence/final-semantic.json" --md "$evidence/final-semantic.md"
if ($LASTEXITCODE) { throw 'semantic tool failed' }
Get-FileHash $candidate,$base,$jar -Algorithm SHA256 | Format-Table -AutoSize
$result = Get-Content -Raw -Encoding UTF8 "$evidence/final-semantic.json" | ConvertFrom-Json
$result.counts | ConvertTo-Json -Compress
$result.classSelection | ConvertTo-Json -Compress
```

## 六個方法的完整正規化差異

### `questDamage(Llocal/lito381/server/GameHub;JLlocal/lito381/server/ClientSession;JILlocal/lito381/server/NativeSourceNpcDamageDisplay$Receipt;)Llocal/lito381/server/BattleQuestEncounter$Hit;`

```diff
--- staged/NativeSourceNpcDamageDisplay-questDamage-2198238914.txt
+++ candidate/NativeSourceNpcDamageDisplay-questDamage-2198238914.txt
@@ -1,54 +1,58 @@
 questDamage(Llocal/lito381/server/GameHub;JLlocal/lito381/server/ClientSession;JILlocal/lito381/server/NativeSourceNpcDamageDisplay$Receipt;)Llocal/lito381/server/BattleQuestEncounter$Hit;
-TRY L0 L3 L4 java/lang/Throwable
-TRY L5 L6 L4 java/lang/Throwable
+TRY L0 L1 L6 java/lang/Throwable
+TRY L3 L5 L6 java/lang/Throwable
+TRY L7 L8 L6 java/lang/Throwable
 ALOAD 0
 DUP
 ASTORE 8
 MONITORENTER
 ALOAD 7
 LABEL L0
-IFNULL L1
+IFNULL L2
 ALOAD 7
 GETFIELD local/lito381/server/NativeSourceNpcDamageDisplay$Receipt.aim:Llocal/lito381/server/NativeSourceMonsterHp$Aim;
-IFNULL L1
+IFNULL L2
 ALOAD 7
 INVOKEVIRTUAL local/lito381/server/NativeSourceNpcDamageDisplay$Receipt.before()Z
-IFNE L1
+IFNE L2
 CONST null
-ASTORE 9
-GOTO L2
+ALOAD 8
+MONITOREXIT
 LABEL L1
+ALOAD 7
+INVOKESTATIC local/lito381/server/NativeSourceDollNpc.acceptedNatural(Llocal/lito381/server/NativeSourceNpcDamageDisplay$Receipt;)V
+ARETURN
+LABEL L2
 ALOAD 0
 LLOAD 1/2
 ALOAD 3
 LLOAD 4/5
 ILOAD 6
+LABEL L3
 INVOKESTATIC local/lito381/server/NativeCombatAttacker.questDamage(Llocal/lito381/server/GameHub;JLlocal/lito381/server/ClientSession;JI)Llocal/lito381/server/BattleQuestEncounter$Hit;
 ASTORE 9
 ALOAD 9
-IFNULL L2
+IFNULL L4
 ALOAD 7
-IFNULL L2
+IFNULL L4
 ALOAD 7
 GETFIELD local/lito381/server/NativeSourceNpcDamageDisplay$Receipt.aim:Llocal/lito381/server/NativeSourceMonsterHp$Aim;
-IFNULL L2
+IFNULL L4
 ALOAD 7
 INVOKEVIRTUAL local/lito381/server/NativeSourceNpcDamageDisplay$Receipt.seal()V
-LABEL L2
+LABEL L4
+ALOAD 9
 ALOAD 8
 MONITOREXIT
-LABEL L3
-GOTO L7
-LABEL L4
+LABEL L5
+ALOAD 7
+INVOKESTATIC local/lito381/server/NativeSourceDollNpc.acceptedNatural(Llocal/lito381/server/NativeSourceNpcDamageDisplay$Receipt;)V
+ARETURN
+LABEL L6
 ASTORE 10
 ALOAD 8
-LABEL L5
+LABEL L7
 MONITOREXIT
-LABEL L6
+LABEL L8
 ALOAD 10
-ATHROW
-LABEL L7
-ALOAD 7
-INVOKESTATIC local/lito381/server/NativeSourceDollNpc.acceptedNatural(Llocal/lito381/server/NativeSourceNpcDamageDisplay$Receipt;)V
-ALOAD 9
-ARETURN+ATHROW
```

### `timeDamage(Llocal/lito381/server/GameHub;JLlocal/lito381/server/ClientSession;JILlocal/lito381/server/NativeSourceNpcDamageDisplay$Receipt;)Llocal/lito381/server/GameHub$TimeAttackActorHit;`

```diff
--- staged/NativeSourceNpcDamageDisplay-timeDamage-3873484134.txt
+++ candidate/NativeSourceNpcDamageDisplay-timeDamage-3873484134.txt
@@ -1,54 +1,58 @@
 timeDamage(Llocal/lito381/server/GameHub;JLlocal/lito381/server/ClientSession;JILlocal/lito381/server/NativeSourceNpcDamageDisplay$Receipt;)Llocal/lito381/server/GameHub$TimeAttackActorHit;
-TRY L0 L3 L4 java/lang/Throwable
-TRY L5 L6 L4 java/lang/Throwable
+TRY L0 L1 L6 java/lang/Throwable
+TRY L3 L5 L6 java/lang/Throwable
+TRY L7 L8 L6 java/lang/Throwable
 ALOAD 0
 DUP
 ASTORE 8
 MONITORENTER
 ALOAD 7
 LABEL L0
-IFNULL L1
+IFNULL L2
 ALOAD 7
 GETFIELD local/lito381/server/NativeSourceNpcDamageDisplay$Receipt.aim:Llocal/lito381/server/NativeSourceMonsterHp$Aim;
-IFNULL L1
+IFNULL L2
 ALOAD 7
 INVOKEVIRTUAL local/lito381/server/NativeSourceNpcDamageDisplay$Receipt.before()Z
-IFNE L1
+IFNE L2
 CONST null
-ASTORE 9
-GOTO L2
+ALOAD 8
+MONITOREXIT
 LABEL L1
+ALOAD 7
+INVOKESTATIC local/lito381/server/NativeSourceDollNpc.acceptedNatural(Llocal/lito381/server/NativeSourceNpcDamageDisplay$Receipt;)V
+ARETURN
+LABEL L2
 ALOAD 0
 LLOAD 1/2
 ALOAD 3
 LLOAD 4/5
 ILOAD 6
+LABEL L3
 INVOKESTATIC local/lito381/server/NativeCombatAttacker.timeDamage(Llocal/lito381/server/GameHub;JLlocal/lito381/server/ClientSession;JI)Llocal/lito381/server/GameHub$TimeAttackActorHit;
 ASTORE 9
 ALOAD 9
-IFNULL L2
+IFNULL L4
 ALOAD 7
-IFNULL L2
+IFNULL L4
 ALOAD 7
 GETFIELD local/lito381/server/NativeSourceNpcDamageDisplay$Receipt.aim:Llocal/lito381/server/NativeSourceMonsterHp$Aim;
-IFNULL L2
+IFNULL L4
 ALOAD 7
 INVOKEVIRTUAL local/lito381/server/NativeSourceNpcDamageDisplay$Receipt.seal()V
-LABEL L2
+LABEL L4
+ALOAD 9
 ALOAD 8
 MONITOREXIT
-LABEL L3
-GOTO L7
-LABEL L4
+LABEL L5
+ALOAD 7
+INVOKESTATIC local/lito381/server/NativeSourceDollNpc.acceptedNatural(Llocal/lito381/server/NativeSourceNpcDamageDisplay$Receipt;)V
+ARETURN
+LABEL L6
 ASTORE 10
 ALOAD 8
-LABEL L5
+LABEL L7
 MONITOREXIT
-LABEL L6
+LABEL L8
 ALOAD 10
-ATHROW
-LABEL L7
-ALOAD 7
-INVOKESTATIC local/lito381/server/NativeSourceDollNpc.acceptedNatural(Llocal/lito381/server/NativeSourceNpcDamageDisplay$Receipt;)V
-ALOAD 9
-ARETURN+ATHROW
```

### `nativeSourceFirstViewHandled(Llocal/lito381/server/GameHub;Ljava/util/List;Ljava/lang/Object;Llocal/lito381/server/BattleRules$Mode;JIIILlocal/lito381/server/ClientSession;JIILjava/lang/String;Ljava/lang/Object;)Z`

```diff
--- staged/NativeSourceNpcEscape-nativeSourceFirstViewHandled-2396358605.txt
+++ candidate/NativeSourceNpcEscape-nativeSourceFirstViewHandled-2396358605.txt
@@ -168,8 +168,7 @@
 CHECKCAST java/lang/Integer
 INVOKEVIRTUAL java/lang/Integer.intValue()I
 ILOAD 6
-IF_ICMPNE L21
-GOTO L23
+IF_ICMPEQ L23
 LABEL L21
 CONST Integer:1
 ALOAD 21
```

### `nativeSourceNpcPlayerFirstHandled(Llocal/lito381/server/GameHub;Ljava/util/List;Ljava/lang/Object;Llocal/lito381/server/BattleRules$Mode;JIIILlocal/lito381/server/ClientSession;JIILjava/lang/String;Ljava/lang/Object;Ljava/lang/Object;)Z`

```diff
--- staged/NativeSourceNpcEscape-nativeSourceNpcPlayerFirstHandled-3979114345.txt
+++ candidate/NativeSourceNpcEscape-nativeSourceNpcPlayerFirstHandled-3979114345.txt
@@ -170,8 +170,7 @@
 CHECKCAST java/lang/Integer
 INVOKEVIRTUAL java/lang/Integer.intValue()I
 ILOAD 6
-IF_ICMPNE L21
-GOTO L23
+IF_ICMPEQ L23
 LABEL L21
 CONST Integer:1
 ALOAD 22
```

### `sourceCurrent(Llocal/lito381/server/NativeSourcePhysicalNpc$Origin;)Z`

```diff
--- staged/NativeSourcePhysicalNpc-sourceCurrent-2617426321.txt
+++ candidate/NativeSourcePhysicalNpc-sourceCurrent-2617426321.txt
@@ -2,54 +2,47 @@

 ALOAD 0
 INVOKESTATIC local/lito381/server/NativeSourceRawNpc.sourceState(Llocal/lito381/server/NativeSourcePhysicalNpc$Origin;)I
-ISTORE 1
-ILOAD 1
-IFLT L2
-ILOAD 1
-IFEQ L0
-CONST Integer:1
-GOTO L1
+DUP
+IFLT L0
+IRETURN
 LABEL L0
-CONST Integer:0
-LABEL L1
-IRETURN
-LABEL L2
+POP
 ALOAD 0
 INVOKEVIRTUAL local/lito381/server/NativeSourcePhysicalNpc$Origin.source()Llocal/lito381/server/NativeSourceDamageDisplay$Actor;
 INVOKEVIRTUAL local/lito381/server/NativeSourceDamageDisplay$Actor.pc()Llocal/lito381/server/ClientSession;
-ASTORE 2
+ASTORE 1
 ALOAD 0
 INVOKEVIRTUAL local/lito381/server/NativeSourcePhysicalNpc$Origin.source()Llocal/lito381/server/NativeSourceDamageDisplay$Actor;
 ALOAD 0
 INVOKEVIRTUAL local/lito381/server/NativeSourcePhysicalNpc$Origin.source()Llocal/lito381/server/NativeSourceDamageDisplay$Actor;
 INVOKEVIRTUAL local/lito381/server/NativeSourceDamageDisplay$Actor.life()Ljava/lang/Object;
 INVOKEVIRTUAL local/lito381/server/NativeSourceDamageDisplay$Actor.valid(Ljava/lang/Object;)Z
-IFEQ L3
-ALOAD 2
+IFEQ L1
+ALOAD 1
 CONST str:"playerHp"
 INVOKESTATIC local/lito381/server/NativeSourceDamageDisplay.n(Llocal/lito381/server/ClientSession;Ljava/lang/String;)I
-IFLE L3
-ALOAD 2
+IFLE L1
+ALOAD 1
 CONST str:"observer"
 INVOKESTATIC local/lito381/server/CombatAccess.field(Ljava/lang/Object;Ljava/lang/String;)Ljava/lang/Object;
 CHECKCAST java/lang/Boolean
 INVOKEVIRTUAL java/lang/Boolean.booleanValue()Z
-IFNE L3
-ALOAD 2
+IFNE L1
+ALOAD 1
 CONST str:"hub"
 INVOKESTATIC local/lito381/server/CombatAccess.field(Ljava/lang/Object;Ljava/lang/String;)Ljava/lang/Object;
 ALOAD 0
 INVOKEVIRTUAL local/lito381/server/NativeSourcePhysicalNpc$Origin.scene()Llocal/lito381/server/NativeSourceCubeOrigin$Scene;
 INVOKEVIRTUAL local/lito381/server/NativeSourceCubeOrigin$Scene.hub()Llocal/lito381/server/GameHub;
-IF_ACMPNE L3
-ALOAD 2
+IF_ACMPNE L1
+ALOAD 1
 CONST str:"phase"
 INVOKESTATIC local/lito381/server/CombatAccess.field(Ljava/lang/Object;Ljava/lang/String;)Ljava/lang/Object;
 INVOKESTATIC java/lang/String.valueOf(Ljava/lang/Object;)Ljava/lang/String;
 CONST str:"BATTLE"
 INVOKEVIRTUAL java/lang/String.equals(Ljava/lang/Object;)Z
-IFEQ L3
-ALOAD 2
+IFEQ L1
+ALOAD 1
 CONST str:"currentRoomId"
 INVOKESTATIC local/lito381/server/CombatAccess.field(Ljava/lang/Object;Ljava/lang/String;)Ljava/lang/Object;
 CHECKCAST java/lang/Long
@@ -58,10 +51,10 @@
 INVOKEVIRTUAL local/lito381/server/NativeSourcePhysicalNpc$Origin.scene()Llocal/lito381/server/NativeSourceCubeOrigin$Scene;
 INVOKEVIRTUAL local/lito381/server/NativeSourceCubeOrigin$Scene.id()J
 LCMP
-IFNE L3
+IFNE L1
 CONST Integer:1
-GOTO L4
-LABEL L3
+GOTO L2
+LABEL L1
 CONST Integer:0
-LABEL L4
+LABEL L2
 IRETURN
```

### `targetCurrent(Llocal/lito381/server/NativeSourcePhysicalNpc$Origin;)Z`

```diff
--- staged/NativeSourcePhysicalNpc-targetCurrent-2617426321.txt
+++ candidate/NativeSourcePhysicalNpc-targetCurrent-2617426321.txt
@@ -2,52 +2,45 @@

 ALOAD 0
 INVOKESTATIC local/lito381/server/NativeSourceGroundNpc.targetState(Llocal/lito381/server/NativeSourcePhysicalNpc$Origin;)I
-ISTORE 1
-ILOAD 1
-IFLT L2
-ILOAD 1
-IFEQ L0
-CONST Integer:1
-GOTO L1
+DUP
+IFLT L0
+IRETURN
 LABEL L0
-CONST Integer:0
-LABEL L1
-IRETURN
-LABEL L2
+POP
 ALOAD 0
 INVOKEVIRTUAL local/lito381/server/NativeSourcePhysicalNpc$Origin.target()Llocal/lito381/server/NativeSourceCubeOrigin$Target;
-ASTORE 2
-ALOAD 2
+ASTORE 1
+ALOAD 1
 INVOKEVIRTUAL local/lito381/server/NativeSourceCubeOrigin$Target.owner()Ljava/lang/Object;
 CONST str:"sourceProcEncounterGeneration"
 INVOKESTATIC local/lito381/server/CombatAccess.field(Ljava/lang/Object;Ljava/lang/String;)Ljava/lang/Object;
-ALOAD 2
+ALOAD 1
 INVOKEVIRTUAL local/lito381/server/NativeSourceCubeOrigin$Target.generation()Ljava/lang/Object;
-IF_ACMPNE L3
-ALOAD 2
+IF_ACMPNE L1
+ALOAD 1
 INVOKEVIRTUAL local/lito381/server/NativeSourceCubeOrigin$Target.actor()Ljava/lang/Object;
 INVOKESTATIC local/lito381/server/NativeSourceArenaPoison.rawLife(Ljava/lang/Object;)Ljava/lang/Object;
-ALOAD 2
+ALOAD 1
 INVOKEVIRTUAL local/lito381/server/NativeSourceCubeOrigin$Target.life()Ljava/lang/Object;
-IF_ACMPNE L3
-ALOAD 2
+IF_ACMPNE L1
+ALOAD 1
 INVOKEVIRTUAL local/lito381/server/NativeSourceCubeOrigin$Target.actor()Ljava/lang/Object;
 CONST str:"objectId"
 INVOKESTATIC local/lito381/server/CombatAccess.field(Ljava/lang/Object;Ljava/lang/String;)Ljava/lang/Object;
 CHECKCAST java/lang/Long
 INVOKEVIRTUAL java/lang/Long.longValue()J
-ALOAD 2
+ALOAD 1
 INVOKEVIRTUAL local/lito381/server/NativeSourceCubeOrigin$Target.id()J
 LCMP
-IFNE L3
-ALOAD 2
+IFNE L1
+ALOAD 1
 INVOKEVIRTUAL local/lito381/server/NativeSourceCubeOrigin$Target.actor()Ljava/lang/Object;
 CONST str:"hp"
 INVOKESTATIC local/lito381/server/CombatAccess.field(Ljava/lang/Object;Ljava/lang/String;)Ljava/lang/Object;
 CHECKCAST java/lang/Integer
 INVOKEVIRTUAL java/lang/Integer.intValue()I
-IFLE L3
-ALOAD 2
+IFLE L1
+ALOAD 1
 INVOKEVIRTUAL local/lito381/server/NativeSourceCubeOrigin$Target.actor()Ljava/lang/Object;
 CONST str:"x"
 INVOKESTATIC local/lito381/server/CombatAccess.field(Ljava/lang/Object;Ljava/lang/String;)Ljava/lang/Object;
@@ -55,8 +48,8 @@
 INVOKEVIRTUAL java/lang/Integer.intValue()I
 ALOAD 0
 INVOKEVIRTUAL local/lito381/server/NativeSourcePhysicalNpc$Origin.x()I
-IF_ICMPNE L3
-ALOAD 2
+IF_ICMPNE L1
+ALOAD 1
 INVOKEVIRTUAL local/lito381/server/NativeSourceCubeOrigin$Target.actor()Ljava/lang/Object;
 CONST str:"y"
 INVOKESTATIC local/lito381/server/CombatAccess.field(Ljava/lang/Object;Ljava/lang/String;)Ljava/lang/Object;
@@ -64,13 +57,13 @@
 INVOKEVIRTUAL java/lang/Integer.intValue()I
 ALOAD 0
 INVOKEVIRTUAL local/lito381/server/NativeSourcePhysicalNpc$Origin.y()I
-IF_ICMPNE L3
-ALOAD 2
+IF_ICMPNE L1
+ALOAD 1
 INVOKESTATIC local/lito381/server/NativeSourceCubeNpc.member(Llocal/lito381/server/NativeSourceCubeOrigin$Target;)Z
-IFEQ L3
+IFEQ L1
 CONST Integer:1
-GOTO L4
-LABEL L3
+GOTO L2
+LABEL L1
 CONST Integer:0
-LABEL L4
+LABEL L2
 IRETURN
```
