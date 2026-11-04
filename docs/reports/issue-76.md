# Issue #76 — T020-A05 正式 0.6.71 暫存與差異裁定交接

## 結論

**尚未通過 A05 驗收：156 SAME／4 DIFF，160 個 entry 無缺漏、無多出。**
已修正上一輪 `Main` 四層清理覆蓋原始例外的缺陷，並修正完整比較揭露的取消免疫與材料政策缺口。
依[主控指示](https://github.com/chaoren0418-arch/lito-launcher-server/issues/76#issuecomment-6640676228)，已將 Issue #76 加上 `needs:claude`，請主控裁定下列四筆；沒有核准例外前不得標 done 或移入 A-land。

本輪以 `task/76` 的 main 基底 `33f3f5c` 接續上一輪 `521456f5430035379eb737f56770334a1d54fd57`，保留雙方歷史，不切換分支、不強制推送。#132 已由 `e76e1fe` 合併，使用目前工作樹的原版比較器，未修改正規化規則。

## 做了什麼

- 範圍為 A05 的 27 個 staged Java 與本報告；保留原始碼類別的既有註解／結構，沒有修改 `src/main`、`pom.xml`、`docs/REPORT.md`。
- 重新以 CFR 0.152 與 Vineflower 1.11.2 反編譯本 worktree 的正式 JAR，核對上一輪 staged；8 個 DECOMPILED 類別均保留標頭。`NativeAcceptedHpContext`、`NativeCastMaterialPolicy` 採新 Vineflower 輸出並修正控制流程，其餘保留已修整的 CFR 版本並核對新輸出。
- `Main`：hub／capture／maps／accounts 四層捕捉清理失敗，呼叫原始 `thrown.addSuppressed(cleanupFailure)` 後重拋；maps／accounts 保留 null 判斷。正常清理在自己的 catch 之外，不因清理失敗重複 close。同時對齊登入 socket 註冊在內層 try 之前，以及在 `SourceLogin.start` 前擷取 `System.out` 的順序；登入 socket 也保留 suppressed 例外。
- `NativeCancellationSpeedImmunity`：補上 weakness（47）、disease（56）、decayPotion（71）、polluteWater（173）、strikerGale（174）的到期值保存、finally 復原及結果遮罩。上一輪只保留 haste／slow，這是實際行為缺漏，不能稱為純 try 標籤差異。**此類現為 SAME。**
- `NativeCastMaterialPolicy`：使用正式版 `LegacyMySqlRepository.nativeMaterialCharacterConnection()` 直接呼叫，修正來源分支、角色一致性判斷、變數範圍及 JDBC try-with-resources；保留 rollback 與 suppressed 清理。**此類現為 SAME。**
- `NativeAcceptedHpContext.resolve()`：修正字串／索引的求值及變數順序、null 分支、try 外 return。**此類現為 SAME。**
- `NativeAcceptedState`：修正 monitor 暫存重複、`isStale` 分支、布林重複賦值；保留正式 JAR 中未再被引用的兩個 lambda 方法。全方法內容已一致，只剩 InnerClasses 多一列。
- `ClassicWorldMapCatalog.forMap`：縮小來源地圖暫存變數範圍，使後續區域變數槽與正式版一致；剩餘差異只有入口 stack／local 的寫法。
- `AccountRepository`、`MemoryAccountRepository`：重新用既存 Maven class 的 `javap -v` 確認兩者 `SourceFile: "Accounts.java"`，維持從該原始檔拆出的獨立 staged Java，避免帶入清單外類別。唯讀證據來源為 `D:/lineage開發/3-lito開發/scratch/wt/task-30/lito伺服器/LiTo381-Server/target/classes/local/lito381/server/`；沒有修改該 worktree。

## 已驗證

Microsoft OpenJDK `javac 17.0.17`、`--release 17 -encoding UTF-8`；工具使用本機唯讀 ASM 9.7。`lib/` 不存在，編譯 classpath 仍按任務包含 `reference/base-0.6.71.jar;lib/*`。

```text
javac exit 0
Note: Some input files use unchecked or unsafe operations.
Note: Recompile with -Xlint:unchecked for details.
expected=160 actual=160 missing=[] extra=[]
SUMMARY SAME=156 SAME_EFFECTS=0 DIFF=4 ONLY_LEFT=0 ONLY_RIGHT=0 BYTE_SAME=4 ALLOW_METHODS=0
leftClassCount=160 comparedClassCount=160
classSelection.missingLeft=[] classSelection.extraLeft=[]
```

23 個外層及全部 133 個內部 entry 為 SAME。沒有使用 `--allow-methods`、`--effects-methods`、ASM 後處理、正式 `.class` 覆蓋或替換驗收 JAR。比較器 exit 0 只代表報告產生成功，四筆 DIFF 仍是驗收失敗。

已實際執行下方 PowerShell 重跑區塊：再現同一 156／4 結果，最後依驗收閘門以 exit 1 回報未通過；不是編譯失敗。`git diff --check` 通過，28 個變更路徑符合 A05 白名單，8 個 DECOMPILED 標頭完整。

對全部 160 entry 另以同一 `ClassComparer` 的正規化函式輸出所有方法、例外表與類別／欄位屬性，沒有在 firstDiff 停止；全部差異均在下方附錄。這修正上一輪只列首差異、過度宣稱「javac 無法 SAME」的問題。

`javap -c -p Main` 確認 staged 與 formal 各有 **5 次** `Throwable.addSuppressed`。正式版其中四次位於 offset 1000／1030／1070／1110；staged 的偏移不同，但原始 Throwable 與 cleanup Throwable 分別作為 receiver／argument，並重拋原始 Throwable。這是靜態結構核對，沒有啟動 Main 或注入執行時故障。

最終編譯來源經 LF／尾端空白整理後，從全新 `target/issue-76/verified/classes` 編譯。產物雜湊如下（ZIP 包裝時間可能使重跑 JAR 雜湊不同，語意與 entry 驗收才是門檻）：

| 輸入／產物 | SHA-256 |
|---|---|
| `正式 base-0.6.71.jar` | `e1498871c7be6b287821c57ef5d95933db1740d9b4d4a80f6c3465f21d99d498` |
| `直接 javac 產物 target/issue-76/verified/a05.jar` | `6f53f0f9f522e1a2a5b6aab0b04357602b6d4fa8a90a6959435a13b232f20884` |
| `驗收清單 scratch/issue-76/entries.txt` | `3916f41ecd5fb67979ac5522d564fcf193e7b0689c7320f77dfabe3e4756a462` |

## 剩餘四筆與待裁定事項

| Entry（皆為 `local/lito381/server/`） | 已確認的完整差異 | 狀態 |
|---|---|---|
| `ClassicWorldMapCatalog.class` | 只有 `forMap(I)` 的 `ASTORE/ALOAD` 與正式 `DUP/POP` 差異；後續查表／fallback 指令一致 | Java 需要暫存單次呼叫的結果；未找到零 local 的同形寫法，待裁定 |
| `Main.class` | `accept` 的 catch 範圍、handler local；`main` 正常 cleanup 與 handler 的排列／標籤、正式多出的 ServerSocket CHECKCAST、readyOutput 暫存的 ASTORE/ALOAD | 四層 suppressed 缺陷已修；仍不能宣告整類等價或 SAME，請逐方法審查附錄 |
| `NativeAcceptedState.class` | 唯一差異是 javac 增加 `NativePlayerVitalCompletions$Receipt` 的 InnerClasses 列；所有方法／欄位／其餘屬性一致 | 未找到標準 Java 省略該列的方法，待裁定 |
| `NativeCastTimeline.class` | `lambda$admitSelf$#0` bootstrap 指向自己的布林 lambda，正式共用 `lambda$admit$#3`；左側另多一個相同邏輯的布林 helper | 未找到在指定標準 javac 命令下共用 captured lambda 實作的方法，待裁定 |

以上「可能是編譯器／ASM 形狀差異」屬**推測**，不是主控已核准的等價例外，更不是所有 Java 寫法都不可能的證明。尤其 Main 仍須人工審查其完整控制流程；不可單看 firstDiff 或五次 addSuppressed 就宣稱整類通過。

## 可重跑的編譯與語意驗收

以下在 repo 根目錄 PowerShell 執行，只讀外部 ASM 快取，所有新輸出都留在 worktree。`recheck` 目錄必須不存在；重跑請改一個新的目錄名稱，避免混入舊 class。命令刻意在最後對四筆 DIFF 回報失敗，不把 exit 0 當驗收通過。

```powershell
$ErrorActionPreference = 'Stop'
$server = Join-Path (Get-Location) 'lito伺服器/LiTo381-Server'
$batch = (Get-Content -Raw -Encoding UTF8 docs/combat-merge/a-batches.json |
    ConvertFrom-Json).batches | Where-Object id -eq 'A05'
$out = Join-Path $server 'target/issue-76/recheck'
$evidence = Join-Path (Get-Location) 'scratch/issue-76/recheck'
if ((Test-Path $out) -or (Test-Path $evidence)) { throw '請使用全新輸出目錄' }
New-Item -ItemType Directory "$out/classes", "$out/tool", $evidence | Out-Null
$jdk = 'C:/Program Files/Microsoft/jdk-17.0.17.10-hotspot/bin'
$utf8 = New-Object System.Text.UTF8Encoding($false)
[IO.File]::WriteAllLines("$evidence/entries.txt", [string[]]$batch.verify_entries, $utf8)
$stage = Join-Path $server 'merge-inputs/formal-0.6.71/staged/local/lito381/server'
$sources = @($batch.classes | ForEach-Object { Join-Path $stage "$_.java" })
& "$jdk/javac.exe" --release 17 -encoding UTF-8 `
    -cp "$server/reference/base-0.6.71.jar;$server/lib/*" -d "$out/classes" $sources
if ($LASTEXITCODE -ne 0) { throw 'javac 失敗' }
$classRoot = (Resolve-Path "$out/classes").Path
$actual = @(Get-ChildItem $classRoot -Recurse -Filter '*.class' | ForEach-Object {
    $_.FullName.Substring($classRoot.Length + 1).Replace('\', '/')
} | Sort-Object)
$expected = @($batch.verify_entries | Sort-Object)
$delta = @(Compare-Object $expected $actual)
if ($actual.Count -ne 160 -or $delta.Count -ne 0) { $delta; throw 'entry 集合不符' }
& "$jdk/jar.exe" --create --file "$out/a05.jar" -C "$out/classes" .
if ($LASTEXITCODE -ne 0) { throw 'jar 失敗' }
$asm = 'D:/dev-cache/maven/repository/org/ow2/asm/asm/9.7/asm-9.7.jar;' +
       'D:/dev-cache/maven/repository/org/ow2/asm/asm-tree/9.7/asm-tree-9.7.jar'
$toolSources = @(Get-ChildItem "$server/tools/semantic-diff/src/main/java" -Recurse `
    -Filter '*.java' | Select-Object -ExpandProperty FullName)
& "$jdk/javac.exe" --release 17 -encoding UTF-8 -cp $asm -d "$out/tool" $toolSources
if ($LASTEXITCODE -ne 0) { throw '比較器編譯失敗' }
& "$jdk/java.exe" '-Dfile.encoding=UTF-8' -cp "$out/tool;$asm" `
    local.lito381.semanticdiff.SemanticDiff --left "$out/a05.jar" `
    --right "$server/reference/base-0.6.71.jar" --classes "$evidence/entries.txt" `
    --json "$evidence/result.json" --md "$evidence/result.md"
if ($LASTEXITCODE -ne 0) { throw '比較器執行失敗' }
$r = Get-Content -Raw -Encoding UTF8 "$evidence/result.json" | ConvertFrom-Json
$r.counts
if ($r.classSelection.missingLeft.Count -or $r.classSelection.extraLeft.Count) {
    throw '比較器 entry 集合驗證失敗'
}
if ($r.counts.DIFF -or $r.counts.ONLY_LEFT -or $r.counts.ONLY_RIGHT -or
    $r.counts.SAME -ne 160) { throw 'A05 尚未通過：請查看完整差異並交主控裁定' }
```

## 未驗證什麼

- 尚未達到 160 SAME，沒有變更工具或套用未核准的 effects 白名單。
- 單元測試／執行時回歸依任務不適用，待 A-land 統一執行；未啟動伺服器、登入器、遊戲，未連 DB。
- 未修改原始專案、機密設定、封存、硬連結或正式部署檔。
- 本報告的本機結果不是 PR CI 結果。既有 Semantic gap CI 以 Maven `src/main` 為輸入，不能代替本批 staged 的驗收。

## 產出路徑與給下一棒的提示

- 提交：`lito伺服器/LiTo381-Server/merge-inputs/formal-0.6.71/staged/local/lito381/server/` 下清單限定的 27 個 `.java`，以及 `docs/reports/issue-76.md`。
- 本機證據（不入 git）：`scratch/issue-76/verified.{json,md}`、`verified-entries.json`、`verified-javac.log`、`verified-semantic.log`、`{staged,formal}-Main.javap.txt`、兩份 `*-sourcefile.txt`、`normalized-{left,right}/` 及四份 `.diff`。
- 直接 javac 產物：`lito伺服器/LiTo381-Server/target/issue-76/verified/{classes,a05.jar}`。
- Claude 主控下一步：審查四筆完整差異，決定可接受的工具正規化規則或繼續 Java 修正；若另行批准，先修改對應工具任務再重派本批。不得默默將 DIFF 當 SAME，也不得用正式 class／ASM 覆蓋。
- A-land 前保留 `AccountRepository`／`MemoryAccountRepository` 的拆檔處理需求，避免與 `Accounts.java` 重複定義。
- 下方完整差異已隨報告入 git；審查者即使沒有本機 scratch 仍可閱讀。完整正規化輸出產生器也附在文末，可自行重現，無需強加忽略檔案。

## 附錄：全部正規化差異（未截斷）

左側是直接 javac 的 staged，右側是正式版。依 class／field／method 鍵排序，使用既有比較器的 label、常數、合成方法名及 catch-any 正規化；此處只略去相同的行，保留全部不同片段。程式碼區塊行尾的展示空白已去除，不改變 opcode、常數、旗標、例外表或 method descriptor。

### ClassicWorldMapCatalog

```diff
--- staged/ClassicWorldMapCatalog.txt
+++ formal/ClassicWorldMapCatalog.txt
@@ -2992,12 +2992,11 @@
 DEFAULT
 ILOAD 0
 INVOKESTATIC local/lito381/server/CombatSpecialBuffs.sourceMap(I)Llocal/lito381/server/ClassicWorldMapCatalog$ClassicMap;
-ASTORE 1
-ALOAD 1
+DUP
 IFNULL L0
-ALOAD 1
 ARETURN
 LABEL L0
+POP
 GETSTATIC local/lito381/server/ClassicWorldMapCatalog.MAPS:Ljava/util/Map;
 ILOAD 0
 INVOKESTATIC java/lang/Integer.valueOf(I)Ljava/lang/Integer;
```

### Main

```diff
--- staged/Main.txt
+++ formal/Main.txt
@@ -22,7 +22,7 @@
 ANNOTATIONS ||||pvis=|pinv=|lvtvis=|lvtinv=
 DEFAULT
 TRY L0 L1 L2 java/lang/Exception
-TRY L0 L4 L5 java/lang/Throwable
+TRY L0 L4 L4 java/lang/Throwable
 LABEL L0
 ALOAD 0
 INVOKEVIRTUAL java/net/ServerSocket.isClosed()Z
@@ -62,12 +62,11 @@
 INVOKEVIRTUAL java/io/PrintStream.println(Ljava/lang/String;)V
 LABEL L3
 INVOKESTATIC local/lito381/server/NativeServerLifetime.abortAccept()V
+RETURN
 LABEL L4
-RETURN
-LABEL L5
-ASTORE 9
+ASTORE 10
 INVOKESTATIC local/lito381/server/NativeServerLifetime.abortAccept()V
-ALOAD 9
+ALOAD 10
 ATHROW
 END METHOD
 METHOD lambda$main$#0(Ljava/net/ServerSocket;ILlocal/lito381/server/ServerConfig;Llocal/lito381/server/AccountRepository;Llocal/lito381/server/GameStateStore;Llocal/lito381/server/CaptureLog;Llocal/lito381/server/GameHub;Ljava/util/concurrent/ExecutorService;)V
@@ -92,18 +91,18 @@
 DEFAULT
 TRY L11 L28 L29 java/lang/Throwable
 TRY L30 L31 L32 java/lang/Throwable
-TRY L10 L35 L36 java/lang/Throwable
-TRY L37 L38 L39 java/lang/Throwable
-TRY L9 L42 L43 java/lang/Throwable
-TRY L44 L45 L46 java/lang/Throwable
-TRY L8 L49 L50 java/lang/Throwable
-TRY L51 L52 L53 java/lang/Throwable
-TRY L7 L56 L57 java/lang/Throwable
-TRY L58 L59 L60 java/lang/Throwable
-TRY L65 L66 L67 java/lang/Exception
-TRY L6 L63 L70 java/lang/Throwable
-TRY L73 L74 L75 java/lang/Exception
-TRY L70 L71 L70 java/lang/Throwable
+TRY L10 L34 L35 java/lang/Throwable
+TRY L36 L37 L38 java/lang/Throwable
+TRY L9 L40 L41 java/lang/Throwable
+TRY L42 L43 L44 java/lang/Throwable
+TRY L8 L46 L47 java/lang/Throwable
+TRY L48 L49 L50 java/lang/Throwable
+TRY L7 L52 L53 java/lang/Throwable
+TRY L54 L55 L56 java/lang/Throwable
+TRY L60 L61 L62 java/lang/Exception
+TRY L6 L58 L65 java/lang/Throwable
+TRY L68 L69 L70 java/lang/Exception
+TRY L65 L66 L65 java/lang/Throwable
 ALOAD 0
 ARRAYLENGTH
 IFLE L0
@@ -337,6 +336,7 @@
 INVOKESTATIC local/lito381/server/NativeServerLifetime.bindMain(Ljava/net/ServerSocket;Ljava/net/SocketAddress;I)V
 ALOAD 6
 ALOAD 18
+CHECKCAST java/net/ServerSocket
 INVOKESTATIC local/lito381/server/NativeServerLifetime.addListener(Ljava/util/List;Ljava/net/ServerSocket;)Z
 POP
 IINC var=17 incr=1
@@ -450,7 +450,6 @@
 INVOKEVIRTUAL java/io/PrintStream.println(Ljava/lang/String;)V
 LABEL L23
 GETSTATIC java/lang/System.out:Ljava/io/PrintStream;
-ASTORE 17
 ALOAD 3
 ALOAD 8
 ALOAD 7
@@ -458,7 +457,6 @@
 ALOAD 11
 ALOAD 5
 INVOKESTATIC local/lito381/server/SourceLogin.start(Llocal/lito381/server/ServerConfig;Llocal/lito381/server/AccountRepository;Llocal/lito381/server/GameStateStore;Llocal/lito381/server/CaptureLog;Llocal/lito381/server/GameHub;Ljava/util/concurrent/ExecutorService;)V
-ALOAD 17
 CONST str:"[LiTo] ready"
 INVOKEVIRTUAL java/io/PrintStream.println(Ljava/lang/String;)V
 CONST Integer:0
@@ -519,6 +517,8 @@
 POP
 GOTO L27
 LABEL L28
+ALOAD 12
+INVOKEVIRTUAL java/net/ServerSocket.close()V
 GOTO L34
 LABEL L29
 ASTORE 13
@@ -536,152 +536,145 @@
 ALOAD 13
 ATHROW
 LABEL L34
-ALOAD 12
-INVOKEVIRTUAL java/net/ServerSocket.close()V
+ALOAD 11
+INVOKESTATIC local/lito381/server/NativeServerLifetime.closeHub(Llocal/lito381/server/GameHub;)V
+GOTO L40
 LABEL L35
-GOTO L41
+ASTORE 12
 LABEL L36
-ASTORE 12
+ALOAD 11
+INVOKESTATIC local/lito381/server/NativeServerLifetime.closeHub(Llocal/lito381/server/GameHub;)V
 LABEL L37
-ALOAD 11
-INVOKESTATIC local/lito381/server/NativeServerLifetime.closeHub(Llocal/lito381/server/GameHub;)V
+GOTO L39
 LABEL L38
-GOTO L40
-LABEL L39
 ASTORE 13
 ALOAD 12
 ALOAD 13
 INVOKEVIRTUAL java/lang/Throwable.addSuppressed(Ljava/lang/Throwable;)V
+LABEL L39
+ALOAD 12
+ATHROW
 LABEL L40
-ALOAD 12
+ALOAD 10
+INVOKESTATIC local/lito381/server/NativeServerLifetime.closeResource(Ljava/lang/AutoCloseable;)V
+GOTO L46
+LABEL L41
+ASTORE 11
+LABEL L42
+ALOAD 10
+INVOKESTATIC local/lito381/server/NativeServerLifetime.closeResource(Ljava/lang/AutoCloseable;)V
+LABEL L43
+GOTO L45
+LABEL L44
+ASTORE 12
+ALOAD 11
+ALOAD 12
+INVOKEVIRTUAL java/lang/Throwable.addSuppressed(Ljava/lang/Throwable;)V
+LABEL L45
+ALOAD 11
 ATHROW
-LABEL L41
-ALOAD 11
-INVOKESTATIC local/lito381/server/NativeServerLifetime.closeHub(Llocal/lito381/server/GameHub;)V
-LABEL L42
-GOTO L48
-LABEL L43
-ASTORE 11
-LABEL L44
-ALOAD 10
+LABEL L46
+ALOAD 9
+IFNULL L52
+ALOAD 9
 INVOKESTATIC local/lito381/server/NativeServerLifetime.closeResource(Ljava/lang/AutoCloseable;)V
-LABEL L45
-GOTO L47
-LABEL L46
-ASTORE 12
-ALOAD 11
-ALOAD 12
-INVOKEVIRTUAL java/lang/Throwable.addSuppressed(Ljava/lang/Throwable;)V
+GOTO L52
 LABEL L47
-ALOAD 11
-ATHROW
+ASTORE 10
+ALOAD 9
+IFNULL L51
 LABEL L48
-ALOAD 10
+ALOAD 9
 INVOKESTATIC local/lito381/server/NativeServerLifetime.closeResource(Ljava/lang/AutoCloseable;)V
 LABEL L49
-GOTO L55
+GOTO L51
 LABEL L50
+ASTORE 11
+ALOAD 10
+ALOAD 11
+INVOKEVIRTUAL java/lang/Throwable.addSuppressed(Ljava/lang/Throwable;)V
+LABEL L51
+ALOAD 10
+ATHROW
+LABEL L52
+ALOAD 8
+IFNULL L58
+ALOAD 8
+INVOKESTATIC local/lito381/server/NativeServerLifetime.closeResource(Ljava/lang/AutoCloseable;)V
+GOTO L58
+LABEL L53
+ASTORE 9
+ALOAD 8
+IFNULL L57
+LABEL L54
+ALOAD 8
+INVOKESTATIC local/lito381/server/NativeServerLifetime.closeResource(Ljava/lang/AutoCloseable;)V
+LABEL L55
+GOTO L57
+LABEL L56
 ASTORE 10
 ALOAD 9
-IFNULL L54
-LABEL L51
-ALOAD 9
-INVOKESTATIC local/lito381/server/NativeServerLifetime.closeResource(Ljava/lang/AutoCloseable;)V
-LABEL L52
-GOTO L54
-LABEL L53
-ASTORE 11
-ALOAD 10
-ALOAD 11
+ALOAD 10
 INVOKEVIRTUAL java/lang/Throwable.addSuppressed(Ljava/lang/Throwable;)V
-LABEL L54
-ALOAD 10
+LABEL L57
+ALOAD 9
 ATHROW
-LABEL L55
-ALOAD 9
-IFNULL L56
-ALOAD 9
-INVOKESTATIC local/lito381/server/NativeServerLifetime.closeResource(Ljava/lang/AutoCloseable;)V
-LABEL L56
-GOTO L62
-LABEL L57
-ASTORE 9
-ALOAD 8
-IFNULL L61
 LABEL L58
-ALOAD 8
-INVOKESTATIC local/lito381/server/NativeServerLifetime.closeResource(Ljava/lang/AutoCloseable;)V
-LABEL L59
-GOTO L61
-LABEL L60
-ASTORE 10
-ALOAD 9
-ALOAD 10
-INVOKEVIRTUAL java/lang/Throwable.addSuppressed(Ljava/lang/Throwable;)V
-LABEL L61
-ALOAD 9
-ATHROW
-LABEL L62
-ALOAD 8
-IFNULL L63
-ALOAD 8
-INVOKESTATIC local/lito381/server/NativeServerLifetime.closeResource(Ljava/lang/AutoCloseable;)V
-LABEL L63
 ALOAD 6
 INVOKEINTERFACE java/util/List.iterator()Ljava/util/Iterator; itf
 ASTORE 8
-LABEL L64
+LABEL L59
 ALOAD 8
 INVOKEINTERFACE java/util/Iterator.hasNext()Z itf
-IFEQ L69
+IFEQ L64
 ALOAD 8
 INVOKEINTERFACE java/util/Iterator.next()Ljava/lang/Object; itf
 CHECKCAST java/net/ServerSocket
 ASTORE 9
+LABEL L60
+ALOAD 9
+INVOKEVIRTUAL java/net/ServerSocket.close()V
+LABEL L61
+GOTO L63
+LABEL L62
+ASTORE 10
+LABEL L63
+GOTO L59
+LABEL L64
+ALOAD 5
+INVOKESTATIC local/lito381/server/NativeServerLifetime.finishClients(Ljava/util/concurrent/ExecutorService;)Ljava/util/List;
+POP
+GOTO L73
 LABEL L65
-ALOAD 9
-INVOKEVIRTUAL java/net/ServerSocket.close()V
+ASTORE 20
 LABEL L66
-GOTO L68
-LABEL L67
-ASTORE 10
-LABEL L68
-GOTO L64
-LABEL L69
-ALOAD 5
-INVOKESTATIC local/lito381/server/NativeServerLifetime.finishClients(Ljava/util/concurrent/ExecutorService;)Ljava/util/List;
-POP
-GOTO L78
-LABEL L70
-ASTORE 20
-LABEL L71
 ALOAD 6
 INVOKEINTERFACE java/util/List.iterator()Ljava/util/Iterator; itf
 ASTORE 21
-LABEL L72
+LABEL L67
 ALOAD 21
 INVOKEINTERFACE java/util/Iterator.hasNext()Z itf
-IFEQ L77
+IFEQ L72
 ALOAD 21
 INVOKEINTERFACE java/util/Iterator.next()Ljava/lang/Object; itf
 CHECKCAST java/net/ServerSocket
 ASTORE 22
-LABEL L73
+LABEL L68
 ALOAD 22
 INVOKEVIRTUAL java/net/ServerSocket.close()V
-LABEL L74
-GOTO L76
-LABEL L75
+LABEL L69
+GOTO L71
+LABEL L70
 ASTORE 23
-LABEL L76
-GOTO L72
-LABEL L77
+LABEL L71
+GOTO L67
+LABEL L72
 ALOAD 5
 INVOKESTATIC local/lito381/server/NativeServerLifetime.finishClients(Ljava/util/concurrent/ExecutorService;)Ljava/util/List;
 POP
 ALOAD 20
 ATHROW
-LABEL L78
+LABEL L73
 RETURN
 END METHOD
 METHOD serverVersion()Ljava/lang/String;
```

### NativeAcceptedState

```diff
--- staged/NativeAcceptedState.txt
+++ formal/NativeAcceptedState.txt
@@ -14,7 +14,6 @@
 INNER local/lito381/server/NativePlayerCommit$Event|local/lito381/server/NativePlayerCommit|Event|0x18
 INNER local/lito381/server/NativePlayerCommit$Frame|local/lito381/server/NativePlayerCommit|Frame|0x18
 INNER local/lito381/server/NativePlayerCommit$Queue|local/lito381/server/NativePlayerCommit|Queue|0x18
-INNER local/lito381/server/NativePlayerVitalCompletions$Receipt|local/lito381/server/NativePlayerVitalCompletions|Receipt|0x18
 INNER local/lito381/server/Protocol$ParalysisMode|local/lito381/server/Protocol|ParalysisMode|0x4018
 FIELD INVIS:Ljava/lang/String; ACCESS 26 SIGNATURE null VALUE str:"sourceInvisibilityLease" ANNOTATIONS |||
 FIELD SLEEP:Ljava/lang/String; ACCESS 26 SIGNATURE null VALUE str:"sourceSleepLease" ANNOTATIONS |||
```

### NativeCastTimeline

```diff
--- staged/NativeCastTimeline.txt
+++ formal/NativeCastTimeline.txt
@@ -558,7 +558,7 @@
 ALOAD 0
 ALOAD 1
 ALOAD 2
-INVOKEDYNAMIC getAsBoolean(Llocal/lito381/server/NativeReuseActor;Llocal/lito381/server/ClientSession;Llocal/lito381/server/NativeCastTimeline$State;)Ljava/util/function/BooleanSupplier; bsm=H6 java/lang/invoke/LambdaMetafactory.metafactory(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodHandle;Ljava/lang/invoke/MethodType;)Ljava/lang/invoke/CallSite; args=[type:()Z, handle:H6 local/lito381/server/NativeCastTimeline.lambda$admitSelf$#3(Llocal/lito381/server/NativeReuseActor;Llocal/lito381/server/ClientSession;Llocal/lito381/server/NativeCastTimeline$State;)Z, type:()Z]
+INVOKEDYNAMIC getAsBoolean(Llocal/lito381/server/NativeReuseActor;Llocal/lito381/server/ClientSession;Llocal/lito381/server/NativeCastTimeline$State;)Ljava/util/function/BooleanSupplier; bsm=H6 java/lang/invoke/LambdaMetafactory.metafactory(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodHandle;Ljava/lang/invoke/MethodType;)Ljava/lang/invoke/CallSite; args=[type:()Z, handle:H6 local/lito381/server/NativeCastTimeline.lambda$admit$#3(Llocal/lito381/server/NativeReuseActor;Llocal/lito381/server/ClientSession;Llocal/lito381/server/NativeCastTimeline$State;)Z, type:()Z]
 ALOAD 0
 ALOAD 2
 ILOAD 3
@@ -635,50 +635,6 @@
 INVOKESPECIAL local/lito381/server/NativeCastTimeline$2.<init>(Llocal/lito381/server/NativeSourceClientActionTimeline$Reservation;ILlocal/lito381/server/ClientSession;Llocal/lito381/server/NativeSourceSkillDelay$Lease;)V
 ARETURN
 END METHOD
-METHOD lambda$admitSelf$#3(Llocal/lito381/server/NativeReuseActor;Llocal/lito381/server/ClientSession;Llocal/lito381/server/NativeCastTimeline$State;)Z
-ACCESS 10 SIGNATURE null EXCEPTIONS []
-ANNOTATIONS ||||pvis=|pinv=|lvtvis=|lvtinv=
-DEFAULT
-ALOAD 0
-INVOKEVIRTUAL local/lito381/server/NativeReuseActor.isSkillDelay()Z
-IFEQ L0
-CONST Integer:0
-IRETURN
-LABEL L0
-INVOKESTATIC java/lang/System.nanoTime()J
-LSTORE 3
-ALOAD 1
-CONST str:"spellReadyNanos"
-INVOKESTATIC local/lito381/server/CombatAccess.field(Ljava/lang/Object;Ljava/lang/String;)Ljava/lang/Object;
-CHECKCAST java/util/Map
-INVOKEINTERFACE java/util/Map.values()Ljava/util/Collection; itf
-INVOKEINTERFACE java/util/Collection.iterator()Ljava/util/Iterator; itf
-ASTORE 5
-LABEL L1
-ALOAD 5
-INVOKEINTERFACE java/util/Iterator.hasNext()Z itf
-IFEQ L3
-ALOAD 5
-INVOKEINTERFACE java/util/Iterator.next()Ljava/lang/Object; itf
-CHECKCAST java/lang/Long
-ASTORE 6
-ALOAD 6
-INVOKEVIRTUAL java/lang/Long.longValue()J
-LLOAD 3
-LSUB
-CONST Long:0
-LCMP
-IFLE L2
-CONST Integer:0
-IRETURN
-LABEL L2
-GOTO L1
-LABEL L3
-ALOAD 2
-GETFIELD local/lito381/server/NativeCastTimeline$State.timeline:Llocal/lito381/server/NativeSourceClientActionTimeline;
-INVOKEVIRTUAL local/lito381/server/NativeSourceClientActionTimeline.canReserveSkillAction()Z
-IRETURN
-END METHOD
 METHOD legacyWindShackle(Llocal/lito381/server/ClientSession;ILjava/lang/String;)Z
 ACCESS 8 SIGNATURE null EXCEPTIONS []
 ANNOTATIONS ||||pvis=|pinv=|lvtvis=|lvtinv=
```

## 附錄：完整正規化輸出產生器

將以下 Java 存為本 worktree 的 `scratch/issue-76/DumpNormalized.java`。此輔助程式只讀取 JAR、呼叫現有比較器的函式並輸出文字，不修改類別。沿用上述 `$out`、`$evidence`、`$jdk`、`$asm`：

```powershell
& "$jdk/javac.exe" --release 17 -encoding UTF-8 -cp "$out/tool;$asm" `
    -d "$out/tool" scratch/issue-76/DumpNormalized.java
if ($LASTEXITCODE -ne 0) { throw '輸出輔助程式編譯失敗' }
& "$jdk/java.exe" '-Dfile.encoding=UTF-8' -cp "$out/tool;$asm" `
    local.lito381.semanticdiff.DumpNormalized "$out/a05.jar" `
    "$evidence/entries.txt" "$evidence/left"
if ($LASTEXITCODE -ne 0) { throw '左側正規化輸出失敗' }
& "$jdk/java.exe" '-Dfile.encoding=UTF-8' -cp "$out/tool;$asm" `
    local.lito381.semanticdiff.DumpNormalized "$server/reference/base-0.6.71.jar" `
    "$evidence/entries.txt" "$evidence/right"
if ($LASTEXITCODE -ne 0) { throw '右側正規化輸出失敗' }
git diff --no-index -- "$evidence/left" "$evidence/right"
# git diff exit 1 在此表示確有上列差異。
```

```java
package local.lito381.semanticdiff;

import java.nio.file.*;
import java.util.*;
import java.lang.reflect.Method;
import org.objectweb.asm.tree.*;

public class DumpNormalized {
    static Object reflect(String name, Class<?>[] types, Object... args) throws Exception {
        Method m = ClassComparer.class.getDeclaredMethod(name, types);
        m.setAccessible(true);
        return m.invoke(null, args);
    }
    public static void main(String[] args) throws Exception {
        JarIndex jar = JarIndex.load(Path.of(args[0]));
        Path out = Path.of(args[2]);
        Files.createDirectories(out);
        for(String entry: Files.readAllLines(Path.of(args[1]))) {
            String name = entry.trim().replace(".class", "");
            ClassNode c = jar.readTree(name);
            List<String> lines = new ArrayList<>();
            lines.add("CLASS " + name);
            lines.add("ACCESS " + c.access + " SUPER " + c.superName + " SIGNATURE " + c.signature);
            lines.add("INTERFACES " + c.interfaces);
            lines.add("OUTER " + c.outerClass + " " + c.outerMethod + " " + c.outerMethodDesc);
            lines.add("NESTHOST " + c.nestHostClass);
            lines.add("NESTMEMBERS " + new TreeSet<>(c.nestMembers == null ? List.of() : c.nestMembers));
            lines.add("PERMITTED " + c.permittedSubclasses);
            lines.add("ANNOTATIONS " + reflect("annotations",new Class<?>[]{ClassNode.class},c));
            lines.add("RECORDS " + reflect("records",new Class<?>[]{ClassNode.class,JarIndex.class},c,jar));
            String inner = (String)reflect("innerClasses",new Class<?>[]{ClassNode.class},c);
            for (String item: inner.split("; ")) lines.add("INNER " + item);
            TreeMap<String,FieldNode> fields=new TreeMap<>();
            for(FieldNode f:c.fields) fields.put(f.name+":"+f.desc,f);
            for(var e:fields.entrySet()) {
                FieldNode f=e.getValue();
                lines.add("FIELD " + e.getKey()+" ACCESS " +(f.access & ~0x1000)+" SIGNATURE "+f.signature+" VALUE "+reflect("cst",new Class<?>[]{Object.class,JarIndex.class},f.value,jar)+" ANNOTATIONS "+reflect("fieldAnns",new Class<?>[]{FieldNode.class},f));
            }
            TreeMap<String,MethodNode> methods=new TreeMap<>();
            for(MethodNode m:c.methods) methods.put(jar.remapMethod(name,m.name)+m.desc,m);
            for(var e:methods.entrySet()) {
                MethodNode m=e.getValue();
                lines.add("METHOD " + e.getKey());
                lines.add("ACCESS " +(m.access & ~0x1000)+" SIGNATURE "+m.signature+" EXCEPTIONS "+new TreeSet<>(m.exceptions));
                Object labels=reflect("labelIds",new Class<?>[]{MethodNode.class},m);
                lines.add("ANNOTATIONS "+reflect("methodAnns",new Class<?>[]{MethodNode.class,Map.class,JarIndex.class},m,labels,jar));
                lines.add("DEFAULT "+reflect("defaultValue",new Class<?>[]{Object.class,JarIndex.class},m.annotationDefault,jar));
                lines.addAll((List<String>)reflect("tryCatch",new Class<?>[]{MethodNode.class,JarIndex.class},m,jar));
                lines.addAll(ClassComparer.instructions(m,jar));
                lines.add("END METHOD");
            }
            Files.write(out.resolve(name.substring(name.lastIndexOf('/')+1)+".txt"),lines);
        }
    }
}
```
