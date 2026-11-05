# Issue #76 — A05 審查退回修正與依賴阻塞交接

## 結論

**未完成、不可合併或移入 A-land。** 本輪直接編譯 27 個 staged Java 成功，160 個 entry 無缺漏、無多出；以目前已合併的 #132 比較器重跑，嚴格模式與加入核准 forMap 的模式均為 **SAME=156／SAME_EFFECTS=0／DIFF=4**。這是本輪新產物結果，不沿用先前 verified／recheck 或 CI 綠作為驗收證據。

本輪承接 `task/76` @ `05d926f50f25ebd5e1c5eab400483fff8e63fe96`，未切換／新建分支。主控已明定等待 #135、#153 合併後重驗：2026-11-05 本輪查得 [PR #152](https://github.com/chaoren0418-arch/lito-launcher-server/pull/152) 仍 `open, merged=false`（HEAD `9c9eb599b15bed4c78c961e3b8b3ba378dd5cedc`）；[#153](https://github.com/chaoren0418-arch/lito-launcher-server/issues/153) 仍 `open, task:queued`，依賴 #135。沒有借用未合併的比較器宣告通過。

## 做了什麼

- `Main.java`：addListener 第二個參數改為 `(ServerSocket) (Object) gameServer`。經直接 javac 與 javap 確認，正式與 staged 均在 offset 482 產生 `CHECKCAST java/net/ServerSocket`，485 呼叫 addListener。先轉為 Object 使 javac 保留 CHECKCAST；單純轉回已知 ServerSocket 型別不足以重現該指令。
- `docs/combat-merge/effects-approved.txt`：依[主控逐項裁定](https://github.com/chaoren0418-arch/lito-launcher-server/issues/76#issuecomment-6645328994)與使用者提供的審查回饋，加入 `ClassicWorldMapCatalog#forMap(I)Llocal/lito381/server/ClassicWorldMapCatalog$ClassicMap;`，附核准連結及 `# batch: A05, category: dup-pop-local`。這是原 Java／報告範圍之外，審查明確要求的單一核准清單增補。
- 保留既有四層 `thrown.addSuppressed(cleanupFailure)` 與 login socket 清理；兩邊仍各 5 次呼叫。沒有把失敗候選的 try 改寫搬入 staged。
- `Main.main` 的 readyOutput 暫存仍保留在 SourceLogin.start 前，以維持 System.out 讀取順序。主控只核准這一處 B 類差異；main 尚有 C 類 cleanup 差異，**未將整個 main 加入 effects 清單**，避免把局部核准擴張成整方法例外。
- `NativeAcceptedState`、`NativeCastTimeline` 未為 metadata／lambda 差異改寫 Java。重新 javap 確認 `lambda$admitSelf$4` flags 為 `0x100a ACC_PRIVATE, ACC_STATIC, ACC_SYNTHETIC`，是來源 lambda，不是手寫具名 helper。
- 更新本報告，列出新產物、實際指令、四種隔離試編、每個 Main 方法的完整正規化差異，以及另外三個類別的完整差異。

## 已驗證

Microsoft OpenJDK `javac 17.0.17`，`--release 17 -encoding UTF-8`；classpath 為本 worktree 的 `reference/base-0.6.71.jar;lib/*`（lib 不存在），工具只讀 `D:/dev-cache/maven/repository/org/ow2/asm/` 的 ASM 9.7。比較器從本 worktree `tools/semantic-diff/src/main/java` 重新編譯，未改工具來源。

```text
javac exit 0
Note: Some input files use unchecked or unsafe operations.
Note: Recompile with -Xlint:unchecked for details.
expected=160 actual=160 missing=[] extra=[]
strict:   SAME=156 SAME_EFFECTS=0 DIFF=4 ONLY_LEFT=0 ONLY_RIGHT=0 BYTE_SAME=4
approved: SAME=156 SAME_EFFECTS=0 DIFF=4 ONLY_LEFT=0 ONLY_RIGHT=0 BYTE_SAME=4
leftClassCount=160 comparedClassCount=160
classSelection.missingLeft=[] classSelection.extraLeft=[]
```

23 個外層及 133 個內部 entry 為 SAME。8 個 DECOMPILED 標頭仍保留。既有 `NativeCancellationSpeedImmunity`、`NativeCastMaterialPolicy`、`NativeAcceptedHpContext` 本輪均 SAME。沒有改 class bytes、套用 ASM 後處理、從正式 JAR 覆蓋 class、增加清單外產物或使用 `--allow-methods`。

核准 forMap 仍未通過的具體原因：目前 `ClassComparer.effectsSequence` 保留 `ASTORE`（只忽略槽位數字），因此實際回報 `effects sequence[1]: left=ASTORE right=ARETURN`。這不是未載入核准清單；`approved.json` 的 `effectsMethods` 已列出精確描述子。需要比較器負責者確認核准的 dup-pop-local 類型如何處理，不能只新增清單就宣稱 SAME_EFFECTS。

`Main` 的完整正規化檢查：類別屬性、`<init>`、`serverVersion`、`lambda$main$0` 全部相同；僅 `accept` 與 `main` 不同。對全部 160 entry 輸出完整正規化內容後，只有附錄四類有差異；不是只取 firstDiff。

## Main 巢狀結構試編與尚未解決事項

所有試編只在 `scratch/issue-76/review-final/<variant>/Main.java`，產物在 `target/issue-76/review-final/<variant>/`；沒有執行 Main。下表是實際 javap 結果，失敗候選未取代 staged。

| 寫法 | 實際結果 | 判斷 |
|---|---|---|
| staged 現行顯式 return | accept 的 Throwable `[0,79)`、handler 80；正式 `[0,80)`、handler 80 | 多出的保護範圍恰為 offset 79 的 RETURN，差異仍保留；不自行核准 |
| `accept-implicit-return`，移除顯式 return | Throwable `[0,79)`、handler 82，正常路徑增加 GOTO | 未重現正式版 |
| `accept-finally`，外層改 finally | catch-any `[0,76)`、handler 82，另有 `[82,84)` 的自保護 | 正常 abortAccept 位於保護範圍外；與正式版不同，不採用 |
| `login-existing-resource`，既有變數 `try (loginServer)` | 正常 close 移到 handler 前，但 javac 新增資源別名／null 檢查，主區間起點變成 283（正式 279） | 不能只用槽位重編號消除新增指令；其餘四層 custom close 亦未解，不採用 |
| `login-close-inside-try`，直接把 normal close 移入 try | close 的指令變成 `[950,955)`，同層 Throwable 保護區間延伸到 955 | normal close 自己失敗會再進 handler 執行 close，改變行為，不採用 |

正式版 login／hub／capture／maps／accounts 的 normal cleanup 在各自 handler 之前，且各自排除於自己的 Throwable 保護區間外；目前 staged 的 normal cleanup 在 handler 之後、同樣排除於自己的保護區間外。對應所有正規化標籤與指令的差異見附錄，不以「行為看起來相同」消除此項 C 類要求。

**推測**：上述排列與保護 RETURN 的差異可能來自正式 ASM 對 javac try-with-resources 的改寫。但本輪試編沒有證明所有 Java 寫法都不可能重現，也沒有獲准擴大例外。尚需主控指定可接受的 Java 表達或裁定／安排比較器的受控處理；`Main` 仍是 C 類。

## 可重跑的編譯與比較

本輪實際腳本為 `scratch/issue-76/review-followup.py`（27 檔直接編譯、封裝、重建比較器、strict 與 approved 比較、完整 dump），`variants.py` 與 `check-review.py` 產生試編及方法級證據。皆只在本 worktree 輸出。以下 PowerShell 可從 repo 根目錄重跑；必須使用全新輸出目錄。核准清單需先依 A05 範圍篩選，直接傳含 ClientSession 的整份清單會被比較器的 scope 驗證拒絕。

本輪亦實際執行下方 PowerShell 區塊，重新編譯並再現 156／0／4，最後因驗收閘門以 exit 1 結束。工具 exit 0 只表示產生報表；最後驗收條件必須失敗，不能把目前 156／0／4 當完成。#135、#153 合併後仍須重新編譯本批，而非沿用此 JAR。

```powershell
$ErrorActionPreference = 'Stop'
$server = Join-Path (Get-Location) 'lito伺服器/LiTo381-Server'
$batch = (Get-Content -Raw -Encoding UTF8 docs/combat-merge/a-batches.json |
    ConvertFrom-Json).batches | Where-Object id -eq 'A05'
$out = Join-Path $server 'target/issue-76/review-recheck'
$evidence = Join-Path (Get-Location) 'scratch/issue-76/review-recheck'
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
$effects = @(Get-Content -Encoding UTF8 docs/combat-merge/effects-approved.txt |
    Where-Object { $_.StartsWith('local/lito381/server/ClassicWorldMapCatalog#') })
[IO.File]::WriteAllLines("$evidence/effects-a05.txt", [string[]]$effects, $utf8)
& "$jdk/java.exe" '-Dfile.encoding=UTF-8' -cp "$out/tool;$asm" `
    local.lito381.semanticdiff.SemanticDiff --left "$out/a05.jar" `
    --right "$server/reference/base-0.6.71.jar" --classes "$evidence/entries.txt" `
    --effects-methods "$evidence/effects-a05.txt" `
    --json "$evidence/result.json" --md "$evidence/result.md"
if ($LASTEXITCODE -ne 0) { throw '比較器執行失敗' }
$r = Get-Content -Raw -Encoding UTF8 "$evidence/result.json" | ConvertFrom-Json
$r.counts
if ($r.classSelection.missingLeft.Count -or $r.classSelection.extraLeft.Count) {
    throw '比較器 entry 集合驗證失敗'
}
if ($r.counts.DIFF -or $r.counts.ONLY_LEFT -or $r.counts.ONLY_RIGHT -or
    ($r.counts.SAME + $r.counts.SAME_EFFECTS) -ne 160) { throw 'A05 尚未通過：請查看完整差異並交主控裁定' }
```

## 本輪輸入與產物 SHA-256

| 路徑 | SHA-256 |
|---|---|
| `lito伺服器/LiTo381-Server/reference/base-0.6.71.jar` | `e1498871c7be6b287821c57ef5d95933db1740d9b4d4a80f6c3465f21d99d498` |
| `lito伺服器/LiTo381-Server/target/issue-76/review-final/a05.jar` | `3af155bc5c65e2413862fc1ceb1db598f9aa704fe91297c788288e6e0570e091` |
| `scratch/issue-76/review-final/entries.txt` | `3916f41ecd5fb67979ac5522d564fcf193e7b0689c7320f77dfabe3e4756a462` |

## 未驗證什麼

- #135、#153 尚未合併，未完成更新比較器下的最終 A05 驗收；當前結果是診斷，不是 done。
- Main catch／cleanup 的 C 類差異未消除，readyOutput 不代表整個 main 已獲例外。
- 未啟動伺服器、登入器或遊戲，未連 DB；未做執行時故障注入。單元／回歸測試依任務在 A-land 統一執行。
- 未修改 src/main、pom.xml、docs/REPORT.md、機密設定、原始專案、封存、硬連結或正式部署檔。未合併 PR 或關閉 Issue。
- 本輪未把既有 PR CI 綠當作 staged 驗收，也未重跑與暫存區無關的 Maven 測試。

## 產出路徑與給下一棒的提示

- 本輪提交：A05 staged `Main.java`、核准清單 `docs/combat-merge/effects-approved.txt`、本報告 `docs/reports/issue-76.md`。其餘 26 個 A05 Java 沿用前輪修正，已一起重新編譯。
- 本機新證據（不進 git）：`scratch/issue-76/review-final/` 下 `strict.{json,md,log}`、`approved.{json,md,log}`、`entries.json`、`effects-a05.txt`、`javac.log`、`tool-javac.log`、`normalized-{left,right}/`、四份 `.diff`、`main-methods.json`、`{left,right}-Main.javap.log`、`NativeCastTimeline.javap.txt`、`variants.json` 及四個試編子目錄。
- 本機編譯產物：`lito伺服器/LiTo381-Server/target/issue-76/review-final/{classes,a05.jar,tool}`。沒有修改先前 verified/recheck 產物。
- Issue #76 已加上 `needs:claude` 與 `needs:orchestrator`，保留既有標籤。
- **調度器**：依主控要求把 #76 的依賴更新為 #135、#153，兩者合併前不要反覆派送最終驗收；本輪仍屬 blocked／未完成。
- **Claude 主控**：依附錄逐方法裁定 Main 殘留問題，不把 readyOutput 的局部 B 類核准擴大。並請比較器負責者處理已核准 forMap 在現有 effects 模式仍因 ASTORE 失敗的情況；本任務不能自行修改工具。
- **本機執行器**：依賴合併與主控裁定到位後，接續 `task/76` 重新編譯 27 檔、核對 160 entry，套用有效核准清單，分列 SAME／SAME_EFFECTS／DIFF。未清除 C 類差異前不宣告完成。

## 附錄：Main 每個方法的完整正規化差異

以下取自本輪 #132 比較器的正規化函式（報告僅去除行尾空白），包含所有例外表與方法指令，不在第一處差異停止。`-` 為本輪 staged，`+` 為正式版。區域槽位與引用 InnerClasses 尚未套用未合併的 #135。

### `<init>()V`

SAME，無差異。

### `CLASS METADATA`

SAME，無差異。

### `accept(Ljava/net/ServerSocket;Llocal/lito381/server/ClientSession$Endpoint;ILlocal/lito381/server/ServerConfig;Llocal/lito381/server/AccountRepository;Llocal/lito381/server/GameStateStore;Llocal/lito381/server/CaptureLog;Llocal/lito381/server/GameHub;Ljava/util/concurrent/ExecutorService;)V`

```diff
--- staged/accept(Ljava/net/ServerSocket;Llocal/lito381/server/ClientSession$Endpoint;ILlocal/lito381/server/ServerConfig;Llocal/lito381/server/AccountRepository;Llocal/lito381/server/GameStateStore;Llocal/lito381/server/CaptureLog;Llocal/lito381/server/GameHub;Ljava/util/concurrent/ExecutorService;)V
+++ formal/accept(Ljava/net/ServerSocket;Llocal/lito381/server/ClientSession$Endpoint;ILlocal/lito381/server/ServerConfig;Llocal/lito381/server/AccountRepository;Llocal/lito381/server/GameStateStore;Llocal/lito381/server/CaptureLog;Llocal/lito381/server/GameHub;Ljava/util/concurrent/ExecutorService;)V
@@ -3,7 +3,7 @@
 ANNOTATIONS ||||pvis=|pinv=|lvtvis=|lvtinv=
 DEFAULT
 TRY L0 L1 L2 java/lang/Exception
-TRY L0 L4 L5 java/lang/Throwable
+TRY L0 L4 L4 java/lang/Throwable
 LABEL L0
 ALOAD 0
 INVOKEVIRTUAL java/net/ServerSocket.isClosed()Z
@@ -43,11 +43,10 @@
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
```

### `lambda$main$#0(Ljava/net/ServerSocket;ILlocal/lito381/server/ServerConfig;Llocal/lito381/server/AccountRepository;Llocal/lito381/server/GameStateStore;Llocal/lito381/server/CaptureLog;Llocal/lito381/server/GameHub;Ljava/util/concurrent/ExecutorService;)V`

SAME，無差異。

### `main([Ljava/lang/String;)V`

```diff
--- staged/main([Ljava/lang/String;)V
+++ formal/main([Ljava/lang/String;)V
@@ -4,18 +4,18 @@
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
@@ -363,7 +363,6 @@
 INVOKEVIRTUAL java/io/PrintStream.println(Ljava/lang/String;)V
 LABEL L23
 GETSTATIC java/lang/System.out:Ljava/io/PrintStream;
-ASTORE 17
 ALOAD 3
 ALOAD 8
 ALOAD 7
@@ -371,7 +370,6 @@
 ALOAD 11
 ALOAD 5
 INVOKESTATIC local/lito381/server/SourceLogin.start(Llocal/lito381/server/ServerConfig;Llocal/lito381/server/AccountRepository;Llocal/lito381/server/GameStateStore;Llocal/lito381/server/CaptureLog;Llocal/lito381/server/GameHub;Ljava/util/concurrent/ExecutorService;)V
-ALOAD 17
 CONST str:"[LiTo] ready"
 INVOKEVIRTUAL java/io/PrintStream.println(Ljava/lang/String;)V
 CONST Integer:0
@@ -432,6 +430,8 @@
 POP
 GOTO L27
 LABEL L28
+ALOAD 12
+INVOKEVIRTUAL java/net/ServerSocket.close()V
 GOTO L34
 LABEL L29
 ASTORE 13
@@ -449,151 +449,144 @@
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
-ATHROW
+ALOAD 10
+INVOKESTATIC local/lito381/server/NativeServerLifetime.closeResource(Ljava/lang/AutoCloseable;)V
+GOTO L46
 LABEL L41
-ALOAD 11
-INVOKESTATIC local/lito381/server/NativeServerLifetime.closeHub(Llocal/lito381/server/GameHub;)V
+ASTORE 11
 LABEL L42
-GOTO L48
+ALOAD 10
+INVOKESTATIC local/lito381/server/NativeServerLifetime.closeResource(Ljava/lang/AutoCloseable;)V
 LABEL L43
-ASTORE 11
+GOTO L45
 LABEL L44
-ALOAD 10
+ASTORE 12
+ALOAD 11
+ALOAD 12
+INVOKEVIRTUAL java/lang/Throwable.addSuppressed(Ljava/lang/Throwable;)V
+LABEL L45
+ALOAD 11
+ATHROW
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
-ATHROW
-LABEL L55
-ALOAD 9
-IFNULL L56
-ALOAD 9
-INVOKESTATIC local/lito381/server/NativeServerLifetime.closeResource(Ljava/lang/AutoCloseable;)V
-LABEL L56
-GOTO L62
 LABEL L57
-ASTORE 9
-ALOAD 8
-IFNULL L61
+ALOAD 9
+ATHROW
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
```

### `serverVersion()Ljava/lang/String;`

SAME，無差異。

## 附錄：其他三個類別的完整正規化差異

### `ClassicWorldMapCatalog`

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

### `NativeAcceptedState`

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

### `NativeCastTimeline`

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

## 附錄：完整正規化輸出器

儲存於 `scratch/issue-76/DumpNormalized.java`；只讀 class tree、呼叫現有 ClassComparer 正規化函式並輸出文字，沒有產生或改寫 class。#135／#153 合併後若私有介面改動，須依當時比較器更新診斷輸出器，不可改比較器以配合本報告。

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
