# Issue #70 / T020-B09 — 最新工具重驗，仍待主控裁定

## 結論

**未通過驗收、不可 B-land。** 2026-11-06 在指定 worktree／`task/70`，先將最新 `origin/main@9678e97f4bff1dfab7cc820297e3ea6ff533375c` 合併為 `b3236ab808bb631ce154ed2f1575f9b0f3869124`，再修改與重編。本次工具已包含 #174（`6b769a4d`）、#176（`a32d30db`）、#186（`bccc2cb7`），並非沿用前輪舊工具。

全部 **362 entries：353 SAME、2 SAME_PATHS、7 DIFF**；類別 missing／extra／ONLY_LEFT／ONLY_RIGHT 皆 0。方法層級仍有 **398 DIFF、63 ONLY_RIGHT、9 ONLY_LEFT**；缺漏與額外方法全含 `lambda$`，普通方法缺漏為 0。**類別齊全不代表方法全部通過。** T029 為 **5 SAME、1 SAME_EFFECTS、1 SAME_PATHS、1 DIFF**。

依[主控裁定](https://github.com/chaoren0418-arch/lito-launcher-server/issues/70#issuecomment-6660410007)，剩餘差異分型、完整代表差異附於本報告，交 `needs:claude`。未把 CLI exit 0、CI 綠燈或局部診斷當作驗收通過。

## 做了什麼

- `sourceApplicationRun` 開頭補回 `SourceLogin.allowed(socket, config)`；try 只包含呼叫與條件，catch `Exception` 後呼叫 private static `rethrowUnchecked(Throwable)`。泛型 helper 本體只有 `throw (E) t;`，沒有包裝、記錄或吞例外；完整描述子及核准連結已加入 `extra-methods-approved.txt`。
- `nativeLightCommit` 從讀取 cooldown 開始補上 `catch (Throwable failure) { throw failure; }`，覆蓋拒絕條件、admit、狀態與 MP 更新；成功路徑的 Integer boxing 保持在 try 之外。保留既有 `throws IOException`，沒有加入 Exceptions 豁免。候選的自我涵蓋 handler 尚未被 Java／比較器重現，詳見下節。
- 四個治療用 local record 的 EnclosingMethod-only 核准以 **pending 註解**記錄在 `metadata-exempt.txt`；目前工具只能整組豁免，沒有啟用超出授權的條目。
- 保留三方移植、5 個 `@AsRecord`、`@AsmFinal`、T029 正式方法；本輪未改 Protocol、GameHub，也未以 Java 調整 poison record 的 SWAP、private 呼叫 opcode 或既有 stack 暫存。
- 自行修改限 ClientSession staged Java、上述兩份核准清單與本報告。main 合併帶入的上游檔案不算本輪自行修改；未編輯 `docs/REPORT.md`、`src/main`、`pom.xml`、設定、封存、releases 或硬連結。

## 已驗證

Microsoft OpenJDK 17.0.17；ASM 9.7。直接從合併後來源編譯 semantic-diff、AsRecord 與兩個註解類；再用候選、正式、註解與 lib classpath 編譯本批三個 staged 檔，僅執行官方 AsRecord 轉換器，原位打包後直接比較。沒有複製候選 class bytes、另寫產物修補器、換名或替換待驗 JAR。

```text
javac --release 17 -encoding UTF-8: exit 0（既有 unchecked 提示）
as-record: scanned=362 converted=6; exit 0
entries=362 missing=0 extra=0
SUMMARY SAME=353 SAME_EFFECTS=0 SAME_PATHS=2 ORPHAN_IGNORED=0 SAME_LAYOUT=0 DEVIATION=0 DIFF=7 ONLY_LEFT=0 ONLY_RIGHT=0 BYTE_SAME=3 ALLOW_METHODS=2
semantic-diff: exit 0（報告已產生，不表示驗收成功）
```

converted=6 為 5 個 record 加 ClientSession 的 AsmFinal，不是 6 個 record。四個 local record 由 javac 直接產生；Protocol 的 162 個 entries 仍全部 SAME。兩個 poison record 均 **SAME_PATHS**，Java 未改。

正式驗收使用完整 362 entries、完整 T029 清單與正式 allow-right、裁切到 B09 的兩個 effects 條目及一個 extra helper。沒有傳入 metadata-exempt、exceptions-exempt、layout 或 deviations；前兩者在本批沒有可依法啟用的條目。

### 固定輸入／產物

| 檔案 | SHA-256 |
|---|---|
| `lito伺服器/LiTo381-Server/reference/base-0.6.71.jar` | `e1498871c7be6b287821c57ef5d95933db1740d9b4d4a80f6c3465f21d99d498` |
| `lito伺服器/LiTo381-Server/reference/candidate-50635f7e.jar` | `50635f7edca8ec6ce77787cc32371c7590b497e48ea202f8a679caf3f517b729` |
| `scratch/issue-70/target/latest-final.jar` | `2111ea1efeb4996b3293cc9d720398dd4ed0861c2353f55f637de0cbe4f1e302` |

### T029（完整原名與描述子）

| 方法 | 正式版比較結果 |
|---|---|
| `local/lito381/server/ClientSession#exitRoom(Z)V` | SAME |
| `local/lito381/server/ClientSession#exitRoomBeforePolyMe(Z)V` | SAME_EFFECTS |
| `local/lito381/server/ClientSession#run()V` | SAME_PATHS |
| `local/lito381/server/ClientSession#sendRoomParticipant(JLlocal/lito381/server/ClientSession;Llocal/lito381/server/Protocol$LitoForce;Ljava/lang/String;)V` | SAME |
| `local/lito381/server/ClientSession#sourcePoisonBody$useInventoryItemBeforeTravel([BLlocal/lito381/server/CombatPoisonLifecycle$Scope;)V` | DIFF |
| `local/lito381/server/Protocol#litoRoomParticipant(JLjava/lang/String;Llocal/lito381/server/Protocol$LitoForce;IILlocal/lito381/server/GameStateStore$TournamentStatistics;Ljava/lang/String;IILlocal/lito381/server/Protocol$LitoParticipantState;ILlocal/lito381/server/GameStateStore$TournamentAwards;)[B` | SAME |
| `local/lito381/server/Protocol#magicDollCustomIcon(IIZ)[B` | SAME |
| `local/lito381/server/Protocol#magicDollIcon(I)[B` | SAME |

`run()V` 的工具結果為 SAME_PATHS，沒有冒稱嚴格 SAME；`sourcePoisonBody$useInventoryItemBeforeTravel` 仍在核准 effects 下失敗，因此 T029 整體未通過。

### EnclosingMethod-only 的工具界線

`SemanticDiff.parseMetadataExempt` 只接受類名，不支援屬性選擇；`ClassComparer.compare(..., boolean metadataExempt)` 會同時忽略 ACC_SYNTHETIC、outerClass／outerMethod、NestHost／NestMembers、InnerClasses 指向自身的條目。這超出本次只忽略 outerMethod／outerMethodDesc 的授權。四筆 pending 註解不會成為生效豁免，正式結果仍為 DIFF。

| entry（前綴 `local/lito381/server/ClientSession$`） | staged 的 outerMethod | 候選的 outerMethod |
|---|---|---|
| `1CompanionHealing`、`1PlayerHealing` | `nativeSourceHealingOriginalNature([BLlocal/lito381/server/BattleSpellCatalog$NaturesBlessingSpell;I)V` | `useNaturesBlessingSpell([BLlocal/lito381/server/BattleSpellCatalog$NaturesBlessingSpell;I)V` |
| `2CompanionHealing`、`2PlayerHealing` | `nativeSourceHealingOriginalGroup([BLlocal/lito381/server/BattleSpellCatalog$GroupHealingSpell;)V` | `useGroupHealingSpell([BLlocal/lito381/server/BattleSpellCatalog$GroupHealingSpell;)V` |

補充診斷只在記憶體暫時對齊這兩個屬性，再呼叫未修改的完整 ClassComparer；四個結果皆 `REST_EXCEPT_TWO_PROPERTIES SAME`，涵蓋其他 metadata、欄位、InnerClasses 與方法。診斷隨即還原屬性，沒有序列化／寫回 class，也沒有取代正式 CLI 結果。請主控新增精確屬性工具任務；不可直接取消 pending 註解。

### nativeLightCommit 仍非「只差 throws」

補回 try/catch 後，正式結果先指出 Exceptions：left=`java/io/IOException`、right=空。**唯讀診斷**暫時忽略此方法的 Exceptions，仍然 DIFF：

```text
left=10:[121:java/lang/Throwable  ] right=12:[120:java/lang/Throwable  ]; path event multisets differ (left=110, right=90)
```

候選原始 javap 的例外表為：

```text
from  to   target type
16    149  245    any
151   239  245    any
245   250  245    any
```

第三列保護 handler 自身；候選有 ALOAD／POP 的舊 monitor 暫存，javac 的 catch 原樣轉拋沒有這個自我涵蓋區間。此外完整差異仍見 CHECKCAST、stack 運送與 handler 涵蓋的指令差異。未宣稱全是 harmless。

因此主控「只剩 throws 才試原樣轉拋、再考慮 Exceptions 豁免」的前提不成立，本輪沒有新增 exceptions-exempt，也沒有拿不存在的 javac 失敗證據申請豁免。#186 的 helper 消除規則要求右側對應位置無 handler，本方法右側有 handler，不能直接借登入守門的核准擴大放行。完整正規化差異見附錄。

### sourceApplicationRun 的守門已恢復，但方法仍 DIFF

新增守門位於連線日誌／hub.connect／後續處理 try 之前，拒絕即 return，checked Exception 保持原例外物件轉拋。編譯成功，未新增 throws。正式比較仍看到守門 handler，且其餘 try／控制流程仍不同；#186 的成對正規化未在此整個方法消去它。不能把「helper 已核准」理解成整個方法自動通過。完整差異見附錄，交主控判定工具邊界與既有 Java 缺口。

### 尚餘差異分型

下表按官方 `methodResults` 的**第一個診斷特徵**互斥歸類，不代表已證明根因，也不表示每個方法只有該一種差異。398 個 DIFF 加 72 個方法配對缺口全部納入；四個 metadata DIFF 另列上表。

| 類型 | 數量 |
|---|---:|
| lambda內容配對缺口 | 72 |
| switch-map欄位對應 | 4 |
| 指令資料流與控制流程 | 170 |
| 例外涵蓋區間與handler | 211 |
| lambda或invokedynamic內容 | 8 |
| 其他Exceptions宣告 | 2 |
| nativeLightCommit的Exceptions與handler | 1 |
| 登入守門轉拋與整體handler | 1 |
| T029副作用摘要 | 1 |

其他後備方法結果為 {'ONLY_LEFT': 9, 'ONLY_RIGHT': 63, 'ORPHAN_IGNORED': 8, 'SAME_PATHS': 71, 'DIFF': 398}。其中 ORPHAN_IGNORED 8 是工具依全 JAR 引用掃描的輸出，不當作使用者要求的 SAME／SAME_EFFECTS／SAME_PATHS；仍需主控處理最終驗收口徑。

## 推測

- 同一來源 enum 的 switch-map 排序與 lambda 的內容識別可能造成連帶 DIFF，但不能只因方法名包含 lambda 就宣稱等價。現有工具仍保留 63 ONLY_RIGHT／9 ONLY_LEFT，未用額外方法清單掩蓋。
- 迴圈、路徑上限或暫存不同可能包含工具尚不能證明的等價情況；也可能是真實重建錯誤。此輪沒有足夠證據把 398 DIFF 一概歸因於工具。

## 待驗證／給下一棒的提示

1. **Claude 主控**：先裁定 metadata 精確豁免的工具任務、nativeLightCommit 的自我涵蓋 handler、登入守門的成對正規化邊界，以及其餘分型；Issue #70 應保留 `needs:claude`。
2. **本機執行器**：依主控裁定續修本批／更新工具後，從 staged 重新編譯並重驗完整 362 entries。不能拿本輪 353 SAME 加上手工推測湊成通過。
3. **T029**：保留正式來源，優先消除 inventory 方法的 effects ASTORE 差異；`run()` 的 SAME_PATHS 如需更嚴格模式，交主控明確裁定，沒有自行擴大 effects。
4. **B-land**：本批尚不可落地。未執行單元測試、回歸或實機；依 §6-5 暫存區不執行，統一在 B-land 處理。未啟動伺服器／登入器／遊戲，未連正式 DB。

## 產出路徑與重現

- 版本控制產出：`merge-inputs/50635f7e/staged/local/lito381/server/ClientSession.java`（位於伺服器目錄）、`docs/combat-merge/extra-methods-approved.txt`、`docs/combat-merge/metadata-exempt.txt`、本報告。
- 本機可重建證據：`scratch/issue-70/verify-latest.py`、`run-audit.py`、`LatestAudit.java`、`latest-command.json`、`latest-final-{diff.json,diff.md,audit.txt,javac.log,record.log,semantic.log}`、`target/latest-final.jar`。scratch／target 不進 git；本報告附錄保留各類完整代表差異，不依賴本機檔案才能審查。
- 最新 main 來源的工具未修改；官方 CLI 的 JSON 已列完整 `leftClasspath`、驗收清單及核准旗標。重新跑 `verify-latest.py` 須使用尚不存在的輸出名稱，避免舊 class 汙染。

以下 PowerShell 從 repo 根目錄可重建正式驗收，不依賴前輪 scratch 腳本（JDK 路徑依本機安裝）：

```powershell
$ErrorActionPreference = 'Stop'
$root = (Get-Location).Path
$s = Join-Path $root 'lito伺服器/LiTo381-Server'
$w = Join-Path $root 'scratch/issue-70/reproduce-latest'
if (Test-Path $w) { throw '必須使用全新輸出目錄' }
New-Item -ItemType Directory -Path "$w/target/dependency" -Force | Out-Null
$jdk = 'C:/Program Files/Microsoft/jdk-17.0.17.10-hotspot/bin'
$utf8 = [Text.UTF8Encoding]::new($false)
$jars = foreach ($a in @('asm','asm-tree','asm-analysis')) {
  $p = "D:/dev-cache/maven/repository/org/ow2/asm/$a/9.7/$a-9.7.jar"
  if (!(Test-Path $p)) {
    $p = "$w/target/dependency/$a-9.7.jar"
    Invoke-WebRequest "https://repo.maven.apache.org/maven2/org/ow2/asm/$a/9.7/$a-9.7.jar" -OutFile $p
  }
  $p
}
$asm = $jars -join ';'
$toolSources = @(Get-ChildItem "$s/tools/semantic-diff/src/main/java","$s/tools/as-record/src/main/java" -Recurse -Filter '*.java').FullName
& "$jdk/javac.exe" --release 17 -encoding UTF-8 -cp $asm -d "$w/target/tool" @toolSources
if ($LASTEXITCODE) { throw 'tool javac failed' }
& "$jdk/javac.exe" --release 17 -encoding UTF-8 -d "$w/target/annotations" "$s/src/main/java/local/lito381/server/AsRecord.java" "$s/src/main/java/local/lito381/server/AsmFinal.java"
if ($LASTEXITCODE) { throw 'annotation javac failed' }
$sources = @('ClientSession','GameHub','Protocol') | ForEach-Object { "$s/merge-inputs/50635f7e/staged/local/lito381/server/$_.java" }
$candidate = "$s/reference/candidate-50635f7e.jar"
$formal = "$s/reference/base-0.6.71.jar"
& "$jdk/javac.exe" --release 17 -encoding UTF-8 -cp "$candidate;$formal;$w/target/annotations;$s/lib/*" -d "$w/target/classes" @sources
if ($LASTEXITCODE) { throw 'staged javac failed' }
& "$jdk/java.exe" -cp "$w/target/tool;$asm" local.lito381.asrecord.AsRecord "$w/target/classes"
if ($LASTEXITCODE) { throw 'AsRecord failed' }
& "$jdk/jar.exe" cf "$w/target/left.jar" -C "$w/target/classes" .
if ($LASTEXITCODE) { throw 'jar failed' }
$batch = (Get-Content -Raw -Encoding UTF8 docs/combat-merge/batches.json | ConvertFrom-Json).batches | Where-Object id -eq B09
[IO.File]::WriteAllLines("$w/entries.txt", [string[]]$batch.verify_entries, $utf8)
$actual = @(& "$jdk/jar.exe" tf "$w/target/left.jar" | Where-Object { $_.EndsWith('.class') })
if ($actual.Count -ne 362 -or (Compare-Object $batch.verify_entries $actual)) { throw 'entry mismatch' }
foreach ($policy in @('effects-approved','extra-methods-approved')) {
  $lines = Get-Content -Encoding UTF8 "docs/combat-merge/$policy.txt" | Where-Object { $_.StartsWith('local/lito381/server/ClientSession#') }
  [IO.File]::WriteAllLines("$w/$policy.txt", [string[]]$lines, $utf8)
}
& "$jdk/java.exe" -Xmx3g -cp "$w/target/tool;$asm" local.lito381.semanticdiff.SemanticDiff --left "$w/target/left.jar" --right $candidate --left-classpath $candidate --left-classpath $formal --classes "$w/entries.txt" --allow-methods "$root/docs/combat-merge/t029-methods.txt" --allow-right $formal --effects-methods "$w/effects-approved.txt" --extra-methods "$w/extra-methods-approved.txt" --json "$w/result.json" --md "$w/result.md"
if ($LASTEXITCODE) { throw 'semantic CLI failed' }
(Get-Content -Raw -Encoding UTF8 "$w/result.json" | ConvertFrom-Json).counts
```

上述 jar 工具時間戳可不同，語意結果應相同；本輪固定日期 Python 打包的 SHA 另列上表。**必須檢查 JSON 的狀態與缺漏，exit 0 不足以放行。**

## 附錄：每類一個完整代表差異

以下由官方 JarIndex／ClassComparer 正規化內容產生 unified diff；`staged` 為左、`candidate` 為右，T029 inventory 的右側為 `formal`。列出每個代表方法的全部差異 hunks，未裁切 bootstrap 內容、handler coverage 或 instructions。印出的指令流水序號移除以免平移產生假差異；L 標籤、槽位與 operands 保留。只做診斷文字比對，不改寫 bytes，也不取代官方結果。`TRY COVERAGE` 逐列列出工具的可拋出指令與有序 handler；`META` 含 access、signature、Exceptions。

### nativeLightCommit的Exceptions與handler

`local/lito381/server/ClientSession#nativeLightCommit(Llocal/lito381/server/BattleSpellCatalog$LightSpell;Ljava/lang/Object;J)Ljava/lang/Integer;`

<details>
<summary>完整差異（零行未變更上下文，保留所有 hunks）</summary>

```diff
--- staged/local/lito381/server/ClientSession#nativeLightCommit(Llocal/lito381/server/BattleSpellCatalog$LightSpell;Ljava/lang/Object;J)Ljava/lang/Integer;
+++ candidate/local/lito381/server/ClientSession#nativeLightCommit(Llocal/lito381/server/BattleSpellCatalog$LightSpell;Ljava/lang/Object;J)Ljava/lang/Integer;
@@ -1 +1 @@
-META 0 null [java/io/IOException]
+META 0 null []
@@ -3,45 +3,37 @@
-10:[121:java/lang/Throwable  ]
-12:[121:java/lang/Throwable  ]
-13:[121:java/lang/Throwable  ]
-14:[121:java/lang/Throwable  ]
-16:[121:java/lang/Throwable  ]
-17:[121:java/lang/Throwable  ]
-18:[121:java/lang/Throwable  ]
-19:[121:java/lang/Throwable  ]
-26:[121:java/lang/Throwable  ]
-28:[121:java/lang/Throwable  ]
-30:[121:java/lang/Throwable  ]
-34:[121:java/lang/Throwable  ]
-37:[121:java/lang/Throwable  ]
-40:[121:java/lang/Throwable  ]
-44:[121:java/lang/Throwable  ]
-47:[121:java/lang/Throwable  ]
-50:[121:java/lang/Throwable  ]
-53:[121:java/lang/Throwable  ]
-56:[121:java/lang/Throwable  ]
-58:[121:java/lang/Throwable  ]
-59:[121:java/lang/Throwable  ]
-61:[121:java/lang/Throwable  ]
-66:[121:java/lang/Throwable  ]
-67:[121:java/lang/Throwable  ]
-68:[121:java/lang/Throwable  ]
-73:[121:java/lang/Throwable  ]
-79:[121:java/lang/Throwable  ]
-80:[121:java/lang/Throwable  ]
-81:[121:java/lang/Throwable  ]
-82:[121:java/lang/Throwable  ]
-84:[121:java/lang/Throwable  ]
-87:[121:java/lang/Throwable  ]
-88:[121:java/lang/Throwable  ]
-89:[121:java/lang/Throwable  ]
-95:[121:java/lang/Throwable  ]
-97:[121:java/lang/Throwable  ]
-100:[121:java/lang/Throwable  ]
-102:[121:java/lang/Throwable  ]
-103:[121:java/lang/Throwable  ]
-105:[121:java/lang/Throwable  ]
-109:[121:java/lang/Throwable  ]
-110:[121:java/lang/Throwable  ]
-113:[121:java/lang/Throwable  ]
-114:[121:java/lang/Throwable  ]
-119:[121:java/lang/Throwable  ]
+12:[120:java/lang/Throwable  ]
+14:[120:java/lang/Throwable  ]
+15:[120:java/lang/Throwable  ]
+17:[120:java/lang/Throwable  ]
+18:[120:java/lang/Throwable  ]
+19:[120:java/lang/Throwable  ]
+20:[120:java/lang/Throwable  ]
+27:[120:java/lang/Throwable  ]
+30:[120:java/lang/Throwable  ]
+33:[120:java/lang/Throwable  ]
+37:[120:java/lang/Throwable  ]
+40:[120:java/lang/Throwable  ]
+44:[120:java/lang/Throwable  ]
+47:[120:java/lang/Throwable  ]
+50:[120:java/lang/Throwable  ]
+53:[120:java/lang/Throwable  ]
+55:[120:java/lang/Throwable  ]
+57:[120:java/lang/Throwable  ]
+59:[120:java/lang/Throwable  ]
+64:[120:java/lang/Throwable  ]
+65:[120:java/lang/Throwable  ]
+66:[120:java/lang/Throwable  ]
+73:[120:java/lang/Throwable  ]
+79:[120:java/lang/Throwable  ]
+80:[120:java/lang/Throwable  ]
+81:[120:java/lang/Throwable  ]
+83:[120:java/lang/Throwable  ]
+86:[120:java/lang/Throwable  ]
+87:[120:java/lang/Throwable  ]
+90:[120:java/lang/Throwable  ]
+94:[120:java/lang/Throwable  ]
+97:[120:java/lang/Throwable  ]
+100:[120:java/lang/Throwable  ]
+102:[120:java/lang/Throwable  ]
+106:[120:java/lang/Throwable  ]
+109:[120:java/lang/Throwable  ]
+114:[120:java/lang/Throwable  ]
@@ -55,0 +48 @@
+DUP
@@ -56,0 +50 @@
+POP
@@ -60 +53,0 @@
-CHECKCAST local/lito381/server/BattleSpellCatalog$LightSpell
@@ -72 +65 @@
-IFLT L56
+IFLT L55
@@ -76 +68,0 @@
-CHECKCAST local/lito381/server/BattleSpellCatalog$LightSpell
@@ -79 +70,0 @@
-DUP
@@ -83 +73,0 @@
-DUP
@@ -84,0 +75,2 @@
+ILOAD 12
+ILOAD 13
@@ -86 +78 @@
-IF_ICMPLT L56
+IF_ICMPLT L55
@@ -90 +82 @@
-IF_ACMPNE L56
+IF_ACMPNE L55
@@ -96 +88 @@
-IF_ACMPNE L56
+IF_ACMPNE L55
@@ -99 +91 @@
-IFNE L56
+IFNE L55
@@ -102,2 +94 @@
-IFLE L56
-GOTO L71
+IFGT L71
@@ -106 +96,0 @@
-CHECKCAST local/lito381/server/BattleSpellCatalog$LightSpell
@@ -116,0 +107,2 @@
+ALOAD 9
+POP
@@ -127 +118,0 @@
-CHECKCAST local/lito381/server/BattleSpellCatalog$LightSpell
@@ -135 +125,0 @@
-CHECKCAST local/lito381/server/BattleSpellCatalog$LightSpell
@@ -139,4 +129 @@
-ASTORE 14
-ALOAD 14
-ALOAD 14
-ALOAD 14
+DUP
@@ -145 +131,0 @@
-CHECKCAST local/lito381/server/BattleSpellCatalog$LightSpell
@@ -149,0 +136 @@
+DUP2
@@ -150,0 +138,2 @@
+SWAP
+POP
@@ -154 +143 @@
-ISTORE 15
+ISTORE 14
@@ -157 +145,0 @@
-CHECKCAST local/lito381/server/BattleSpellCatalog$LightSpell
@@ -161 +148,0 @@
-CHECKCAST local/lito381/server/BattleSpellCatalog$LightSpell
@@ -168,5 +155,3 @@
-GOTO L124
-ASTORE 16
-ALOAD 16
-ATHROW
-ILOAD 15
+ALOAD 9
+POP
+ILOAD 14
@@ -174,0 +160,5 @@
+ASTORE 15
+ALOAD 9
+POP
+ALOAD 15
+ATHROW
```

</details>

### switch-map欄位對應

`local/lito381/server/ClientSession#acceptsLitoRosterBroadcast()Z`

<details>
<summary>完整差異（零行未變更上下文，保留所有 hunks）</summary>

```diff
--- staged/local/lito381/server/ClientSession#acceptsLitoRosterBroadcast()Z
+++ candidate/local/lito381/server/ClientSession#acceptsLitoRosterBroadcast()Z
@@ -13 +13 @@
-GETSTATIC local/lito381/server/ClientSession$1.$SwitchMap$#19:[I
+GETSTATIC local/lito381/server/ClientSession$1.$SwitchMap$#20:[I
```

</details>

### 指令資料流與控制流程

`local/lito381/server/GameHub#endBattle(J)V`

<details>
<summary>完整差異（零行未變更上下文，保留所有 hunks）</summary>

```diff
--- staged/local/lito381/server/GameHub#endBattle(J)V
+++ candidate/local/lito381/server/GameHub#endBattle(J)V
@@ -80 +80 @@
-ALOAD 3
+DUP
```

</details>

### 例外涵蓋區間與handler

`local/lito381/server/ClientSession#login([B)V`

<details>
<summary>完整差異（零行未變更上下文，保留所有 hunks）</summary>

```diff
--- staged/local/lito381/server/ClientSession#login([B)V
+++ candidate/local/lito381/server/ClientSession#login([B)V
@@ -6,21 +6,21 @@
-261:[303:java/lang/Exception  ]
-262:[303:java/lang/Exception  ]
-265:[303:java/lang/Exception  ]
-267:[303:java/lang/Exception  ]
-268:[303:java/lang/Exception  ]
-269:[303:java/lang/Exception  ]
-272:[303:java/lang/Exception  ]
-274:[303:java/lang/Exception  ]
-276:[303:java/lang/Exception  ]
-279:[303:java/lang/Exception  ]
-283:[303:java/lang/Exception  ]
-286:[303:java/lang/Exception  ]
-287:[303:java/lang/Exception  ]
-289:[303:java/lang/Exception  ]
-290:[303:java/lang/Exception  ]
-292:[303:java/lang/Exception  ]
-294:[303:java/lang/Exception  ]
-295:[303:java/lang/Exception  ]
-297:[303:java/lang/Exception  ]
-298:[303:java/lang/Exception  ]
-299:[303:java/lang/Exception  ]
+286:[328:java/lang/Exception  ]
+287:[328:java/lang/Exception  ]
+290:[328:java/lang/Exception  ]
+292:[328:java/lang/Exception  ]
+293:[328:java/lang/Exception  ]
+294:[328:java/lang/Exception  ]
+297:[328:java/lang/Exception  ]
+299:[328:java/lang/Exception  ]
+301:[328:java/lang/Exception  ]
+304:[328:java/lang/Exception  ]
+308:[328:java/lang/Exception  ]
+311:[328:java/lang/Exception  ]
+312:[328:java/lang/Exception  ]
+314:[328:java/lang/Exception  ]
+315:[328:java/lang/Exception  ]
+317:[328:java/lang/Exception  ]
+319:[328:java/lang/Exception  ]
+320:[328:java/lang/Exception  ]
+322:[328:java/lang/Exception  ]
+323:[328:java/lang/Exception  ]
+324:[328:java/lang/Exception  ]
@@ -185,0 +186,3 @@
+ALOAD 0
+SWAP
+INVOKESTATIC local/lito381/server/SourceLogin.account(Llocal/lito381/server/ClientSession;Ljava/lang/String;)Ljava/lang/String;
@@ -187 +190 @@
-IFEQ L166
+IFEQ L169
@@ -210 +213 @@
-IF_ACMPNE L190
+IF_ACMPNE L199
@@ -214,0 +218,6 @@
+ASTORE 12
+ASTORE 13
+ALOAD 13
+ALOAD 0
+ALOAD 12
+INVOKESTATIC local/lito381/server/SourceLogin.account(Llocal/lito381/server/ClientSession;Ljava/lang/String;)Ljava/lang/String;
@@ -216 +225 @@
-GOTO L259
+GOTO L284
@@ -220,0 +230,6 @@
+ASTORE 14
+ASTORE 15
+ALOAD 15
+ALOAD 0
+ALOAD 14
+INVOKESTATIC local/lito381/server/SourceLogin.account(Llocal/lito381/server/ClientSession;Ljava/lang/String;)Ljava/lang/String;
@@ -224 +239 @@
-IFNULL L222
+IFNULL L238
@@ -228,5 +243,5 @@
-ASTORE 12
-ALOAD 12
-IFNULL L214
-ALOAD 0
-ALOAD 12
+ASTORE 16
+ALOAD 16
+IFNULL L230
+ALOAD 0
+ALOAD 16
@@ -234,4 +249,5 @@
-IFEQ L214
-ALOAD 0
-ALOAD 12
-PUTFIELD local/lito381/server/ClientSession.selectedClassicServerName:Ljava/lang/String;
+IFEQ L230
+ALOAD 0
+ALOAD 16
+CONST str:"selectedClassicServerName"
+INVOKESTATIC local/lito381/server/NativePlayerCommit.scene(Llocal/lito381/server/ClientSession;Ljava/lang/Object;Ljava/lang/String;)V
@@ -240 +256 @@
-GOTO L222
+GOTO L238
@@ -254,0 +271,8 @@
+ISTORE 17
+ASTORE 18
+ASTORE 19
+ALOAD 19
+ALOAD 0
+ALOAD 18
+INVOKESTATIC local/lito381/server/SourceLogin.account(Llocal/lito381/server/ClientSession;Ljava/lang/String;)Ljava/lang/String;
+ILOAD 17
@@ -258 +282 @@
-IFNULL L259
+IFNULL L284
@@ -263,5 +287,5 @@
-ASTORE 13
-ALOAD 13
-IFNULL L243
-ALOAD 0
-ALOAD 13
+ASTORE 20
+ALOAD 20
+IFNULL L267
+ALOAD 0
+ALOAD 20
@@ -269 +293 @@
-IFNE L254
+IFNE L278
@@ -280,4 +304,5 @@
-GOTO L259
-ALOAD 0
-ALOAD 13
-PUTFIELD local/lito381/server/ClientSession.selectedClassicServerName:Ljava/lang/String;
+GOTO L284
+ALOAD 0
+ALOAD 20
+CONST str:"selectedClassicServerName"
+INVOKESTATIC local/lito381/server/NativePlayerCommit.scene(Llocal/lito381/server/ClientSession;Ljava/lang/Object;Ljava/lang/String;)V
@@ -290 +315 @@
-ASTORE 14
+ASTORE 21
@@ -297 +322 @@
-ASTORE 15
+ASTORE 22
@@ -302 +327 @@
-ALOAD 15
+ALOAD 22
@@ -305 +330 @@
-ALOAD 14
+ALOAD 21
@@ -308 +333 @@
-IFNULL L302
+IFNULL L327
@@ -311 +336 @@
-ASTORE 16
+ASTORE 23
@@ -315 +340 @@
-ALOAD 16
+ALOAD 23
@@ -318 +343 @@
-IFEQ L302
+IFEQ L327
@@ -329,2 +354,2 @@
-GOTO L325
-ASTORE 17
+GOTO L350
+ASTORE 24
@@ -340 +365 @@
-ALOAD 17
+ALOAD 24
@@ -343 +368 @@
-ALOAD 17
+ALOAD 24
@@ -360 +385 @@
-IFNE L353
+IFNE L378
@@ -383 +408 @@
-IF_ACMPNE L376
+IF_ACMPNE L409
@@ -385 +410 @@
-IFNULL L376
+IFNULL L409
@@ -391,0 +417,8 @@
+ASTORE 25
+ASTORE 26
+ASTORE 27
+ALOAD 27
+ALOAD 0
+ALOAD 26
+INVOKESTATIC local/lito381/server/SourceLogin.account(Llocal/lito381/server/ClientSession;Ljava/lang/String;)Ljava/lang/String;
+ALOAD 25
@@ -393 +426 @@
-ISTORE 18
+ISTORE 28
@@ -397 +430 @@
-ILOAD 18
+ILOAD 28
@@ -406,3 +439,3 @@
-IF_ACMPNE L413
-ALOAD 11
-IFNULL L413
+IF_ACMPNE L446
+ALOAD 11
+IFNULL L446
@@ -423 +456 @@
-IFGE L401
+IFGE L434
@@ -427 +460 @@
-GOTO L406
+GOTO L439
@@ -442 +475,2 @@
-PUTFIELD local/lito381/server/ClientSession.phase:Llocal/lito381/server/ClientSession$Phase;
+CONST str:"phase"
+INVOKESTATIC local/lito381/server/NativePlayerCommit.scene(Llocal/lito381/server/ClientSession;Ljava/lang/Object;Ljava/lang/String;)V
@@ -446 +480 @@
-IF_ACMPNE L425
+IF_ACMPNE L459
@@ -451 +485 @@
-GOTO L427
+GOTO L461
```

</details>

### lambda或invokedynamic內容

`local/lito381/server/ClientSession#useClassicWorldLineAttackSpell([BLlocal/lito381/server/BattleSpellCatalog$LineAttackSpell;)V`

<details>
<summary>完整差異（零行未變更上下文，保留所有 hunks）</summary>

```diff
--- staged/local/lito381/server/ClientSession#useClassicWorldLineAttackSpell([BLlocal/lito381/server/BattleSpellCatalog$LineAttackSpell;)V
+++ candidate/local/lito381/server/ClientSession#useClassicWorldLineAttackSpell([BLlocal/lito381/server/BattleSpellCatalog$LineAttackSpell;)V
@@ -9 +9 @@
-INVOKEDYNAMIC run(Llocal/lito381/server/ClientSession;[BLlocal/lito381/server/BattleSpellCatalog$LineAttackSpell;)Llocal/lito381/server/NativeSelfCastAction$Task; bsm=H6 java/lang/invoke/LambdaMetafactory.metafactory(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodHandle;Ljava/lang/invoke/MethodType;)Ljava/lang/invoke/CallSite; args=[type:()V, handle:H7 local/lito381/server/ClientSession.lambda$content$dfb3791738c5b4d0ec1d5ae51a7b2311d8001284ea4625b6d859dc694635b012([BLlocal/lito381/server/BattleSpellCatalog$LineAttackSpell;)V, type:()V]
+INVOKEDYNAMIC run(Llocal/lito381/server/ClientSession;[BLlocal/lito381/server/BattleSpellCatalog$LineAttackSpell;)Llocal/lito381/server/NativeSelfCastAction$Task; bsm=H6 java/lang/invoke/LambdaMetafactory.metafactory(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodHandle;Ljava/lang/invoke/MethodType;)Ljava/lang/invoke/CallSite; args=[type:()V, handle:H7 local/lito381/server/ClientSession.nativeCompanionTargetuseClassicWorldLineAttackSpell([BLlocal/lito381/server/BattleSpellCatalog$LineAttackSpell;)V, type:()V]
```

</details>

### 其他Exceptions宣告

`local/lito381/server/ClientSession#movementUnlock([B)V`

<details>
<summary>完整差異（零行未變更上下文，保留所有 hunks）</summary>

```diff
--- staged/local/lito381/server/ClientSession#movementUnlock([B)V
+++ candidate/local/lito381/server/ClientSession#movementUnlock([B)V
@@ -1 +1 @@
-META 2 null [java/io/IOException]
+META 2 null []
@@ -18,0 +19,2 @@
+CONST Integer:0
+ALOAD 0
@@ -22,2 +23,0 @@
-ALOAD 0
-CONST Integer:0
```

</details>

### lambda內容配對缺口

`local/lito381/server/ClientSession#repair021$lambda$attack$414()I`

<details>
<summary>完整差異（零行未變更上下文，保留所有 hunks）</summary>

```diff
--- staged/local/lito381/server/ClientSession#repair021$lambda$attack$414()I
+++ candidate/local/lito381/server/ClientSession#repair021$lambda$attack$414()I
@@ -1 +1,7 @@
-MISSING
+META 4106 null []
+TRY COVERAGE
+
+INVOKESTATIC java/util/concurrent/ThreadLocalRandom.current()Ljava/util/concurrent/ThreadLocalRandom;
+CONST Integer:1000
+INVOKEVIRTUAL java/util/concurrent/ThreadLocalRandom.nextInt(I)I
+IRETURN
```

</details>

### T029副作用摘要

`local/lito381/server/ClientSession#sourcePoisonBody$useInventoryItemBeforeTravel([BLlocal/lito381/server/CombatPoisonLifecycle$Scope;)V`

<details>
<summary>完整差異（零行未變更上下文，保留所有 hunks）</summary>

```diff
--- staged/local/lito381/server/ClientSession#sourcePoisonBody$useInventoryItemBeforeTravel([BLlocal/lito381/server/CombatPoisonLifecycle$Scope;)V
+++ formal/local/lito381/server/ClientSession#sourcePoisonBody$useInventoryItemBeforeTravel([BLlocal/lito381/server/CombatPoisonLifecycle$Scope;)V
@@ -3,16 +3,16 @@
-940:[953:java/io/IOException  ]
-942:[953:java/io/IOException  ]
-944:[953:java/io/IOException  ]
-946:[953:java/io/IOException  ]
-947:[953:java/io/IOException  ]
-949:[953:java/io/IOException  ]
-950:[953:java/io/IOException  ]
-951:[953:java/io/IOException  ]
-1075:[1088:java/io/IOException  ]
-1077:[1088:java/io/IOException  ]
-1079:[1088:java/io/IOException  ]
-1081:[1088:java/io/IOException  ]
-1082:[1088:java/io/IOException  ]
-1084:[1088:java/io/IOException  ]
-1085:[1088:java/io/IOException  ]
-1086:[1088:java/io/IOException  ]
+942:[955:java/io/IOException  ]
+944:[955:java/io/IOException  ]
+946:[955:java/io/IOException  ]
+948:[955:java/io/IOException  ]
+949:[955:java/io/IOException  ]
+951:[955:java/io/IOException  ]
+952:[955:java/io/IOException  ]
+953:[955:java/io/IOException  ]
+1077:[1090:java/io/IOException  ]
+1079:[1090:java/io/IOException  ]
+1081:[1090:java/io/IOException  ]
+1083:[1090:java/io/IOException  ]
+1084:[1090:java/io/IOException  ]
+1086:[1090:java/io/IOException  ]
+1087:[1090:java/io/IOException  ]
+1088:[1090:java/io/IOException  ]
@@ -100,0 +101,6 @@
+ALOAD 0
+INVOKEVIRTUAL local/lito381/server/ClientSession.accountName()Ljava/lang/String;
+ALOAD 0
+INVOKESPECIAL local/lito381/server/ClientSession.stateCharacter()Ljava/lang/String;
+ALOAD 0
+GETFIELD local/lito381/server/ClientSession.rentalTownInventory:Llocal/lito381/server/RuntimeInventory;
@@ -102,2 +107,0 @@
-ALOAD 0
-INVOKEVIRTUAL local/lito381/server/ClientSession.accountName()Ljava/lang/String;
@@ -105,2 +108,0 @@
-ALOAD 0
-INVOKESPECIAL local/lito381/server/ClientSession.stateCharacter()Ljava/lang/String;
@@ -108,2 +109,0 @@
-ALOAD 0
-GETFIELD local/lito381/server/ClientSession.rentalTownInventory:Llocal/lito381/server/RuntimeInventory;
@@ -110,0 +111,5 @@
+ALOAD 11
+ALOAD 0
+ALOAD 10
+INVOKESTATIC local/lito381/server/SourceLogin.account(Llocal/lito381/server/ClientSession;Ljava/lang/String;)Ljava/lang/String;
+ALOAD 9
@@ -112,5 +116,0 @@
-ALOAD 0
-ALOAD 9
-INVOKESTATIC local/lito381/server/SourceLogin.account(Llocal/lito381/server/ClientSession;Ljava/lang/String;)Ljava/lang/String;
-ALOAD 10
-ALOAD 11
@@ -697,10 +697,11 @@
-IF_ACMPNE L724
-ALOAD 0
-INVOKESTATIC local/lito381/server/NativeWorldPotionPhase.allowed(Llocal/lito381/server/ClientSession;)Z
-IFEQ L688
-ALOAD 0
-GETFIELD local/lito381/server/ClientSession.observer:Z
-IFNE L688
-ALOAD 0
-GETFIELD local/lito381/server/ClientSession.playerHp:I
-IFGT L699
+IF_ACMPNE L725
+ALOAD 0
+GETFIELD local/lito381/server/ClientSession.phase:Llocal/lito381/server/ClientSession$Phase;
+GETSTATIC local/lito381/server/ClientSession$Phase.BATTLE:Llocal/lito381/server/ClientSession$Phase;
+IF_ACMPNE L689
+ALOAD 0
+GETFIELD local/lito381/server/ClientSession.observer:Z
+IFNE L689
+ALOAD 0
+GETFIELD local/lito381/server/ClientSession.playerHp:I
+IFGT L700
@@ -746 +747 @@
-IF_ACMPNE L732
+IF_ACMPNE L733
@@ -754 +755 @@
-IF_ACMPNE L740
+IF_ACMPNE L741
@@ -762 +763 @@
-IF_ACMPNE L748
+IF_ACMPNE L749
@@ -769 +770 @@
-IFEQ L762
+IFEQ L763
@@ -772 +773 @@
-IFEQ L758
+IFEQ L759
@@ -776 +777 @@
-GOTO L761
+GOTO L762
@@ -783,10 +784,11 @@
-IFEQ L822
-ALOAD 0
-INVOKESTATIC local/lito381/server/NativeWorldPotionPhase.allowed(Llocal/lito381/server/ClientSession;)Z
-IFEQ L774
-ALOAD 0
-GETFIELD local/lito381/server/ClientSession.observer:Z
-IFNE L774
-ALOAD 0
-GETFIELD local/lito381/server/ClientSession.playerHp:I
-IFGT L785
+IFEQ L824
+ALOAD 0
+GETFIELD local/lito381/server/ClientSession.phase:Llocal/lito381/server/ClientSession$Phase;
+GETSTATIC local/lito381/server/ClientSession$Phase.BATTLE:Llocal/lito381/server/ClientSession$Phase;
+IF_ACMPNE L776
+ALOAD 0
+GETFIELD local/lito381/server/ClientSession.observer:Z
+IFNE L776
+ALOAD 0
+GETFIELD local/lito381/server/ClientSession.playerHp:I
+IFGT L787
@@ -812 +814 @@
-IFEQ L795
+IFEQ L797
@@ -843 +845 @@
-IFEQ L1014
+IFEQ L1016
@@ -846,7 +848,7 @@
-IFEQ L834
-ALOAD 0
-GETFIELD local/lito381/server/ClientSession.observer:Z
-IFNE L834
-ALOAD 0
-GETFIELD local/lito381/server/ClientSession.playerHp:I
-IFGT L845
+IFEQ L836
+ALOAD 0
+GETFIELD local/lito381/server/ClientSession.observer:Z
+IFNE L836
+ALOAD 0
+GETFIELD local/lito381/server/ClientSession.playerHp:I
+IFGT L847
@@ -871 +873 @@
-IFNE L855
+IFNE L857
@@ -873 +875 @@
-GOTO L856
+GOTO L858
@@ -880 +882 @@
-IFEQ L888
+IFEQ L890
@@ -882 +884 @@
-IFNULL L888
+IFNULL L890
@@ -887 +889 @@
-IFNE L888
+IFNE L890
@@ -908 +910 @@
-IFEQ L907
+IFEQ L909
@@ -910 +912 @@
-IFNULL L907
+IFNULL L909
@@ -915 +917 @@
-IFNE L907
+IFNE L909
@@ -928 +930 @@
-IFEQ L917
+IFEQ L919
@@ -931 +933 @@
-IFNE L917
+IFNE L919
@@ -951 +953 @@
-IFNE L936
+IFNE L938
@@ -957 +959 @@
-IFNE L963
+IFNE L965
@@ -971 +973 @@
-GOTO L963
+GOTO L965
@@ -984 +986 @@
-IFNE L969
+IFNE L971
@@ -990 +992 @@
-IFNE L975
+IFNE L977
@@ -999 +1001 @@
-IFEQ L990
+IFEQ L992
@@ -1008 +1010 @@
-GOTO L978
+GOTO L980
@@ -1019 +1021 @@
-IFNULL L1010
+IFNULL L1012
@@ -1026 +1028 @@
-IFEQ L1010
+IFEQ L1012
@@ -1028 +1030 @@
-GOTO L1011
+GOTO L1013
@@ -1035 +1037 @@
-IFEQ L1155
+IFEQ L1157
@@ -1038,7 +1040,7 @@
-IFEQ L1026
-ALOAD 0
-GETFIELD local/lito381/server/ClientSession.observer:Z
-IFNE L1026
-ALOAD 0
-GETFIELD local/lito381/server/ClientSession.playerHp:I
-IFGT L1037
+IFEQ L1028
+ALOAD 0
+GETFIELD local/lito381/server/ClientSession.observer:Z
+IFNE L1028
+ALOAD 0
+GETFIELD local/lito381/server/ClientSession.playerHp:I
+IFGT L1039
@@ -1061 +1063 @@
-IFEQ L1050
+IFEQ L1052
@@ -1066 +1068 @@
-IFEQ L1050
+IFEQ L1052
@@ -1068 +1070 @@
-GOTO L1051
+GOTO L1053
@@ -1083 +1085 @@
-IFEQ L1074
+IFEQ L1076
@@ -1106 +1108 @@
-GOTO L1095
+GOTO L1097
@@ -1128 +1130 @@
-IFEQ L1117
+IFEQ L1119
@@ -1133 +1135 @@
-IFEQ L1117
+IFEQ L1119
@@ -1135 +1137 @@
-GOTO L1118
+GOTO L1120
@@ -1140 +1142 @@
-IFEQ L1135
+IFEQ L1137
@@ -1143 +1145 @@
-IF_ICMPEQ L1135
+IF_ICMPEQ L1137
@@ -1177 +1179 @@
-IF_ACMPNE L1386
+IF_ACMPNE L1388
@@ -1180,7 +1182,7 @@
-IFEQ L1168
-ALOAD 0
-GETFIELD local/lito381/server/ClientSession.observer:Z
-IFNE L1168
-ALOAD 0
-GETFIELD local/lito381/server/ClientSession.playerHp:I
-IFGT L1179
+IFEQ L1170
+ALOAD 0
+GETFIELD local/lito381/server/ClientSession.observer:Z
+IFNE L1170
+ALOAD 0
+GETFIELD local/lito381/server/ClientSession.playerHp:I
+IFGT L1181
@@ -1208 +1210 @@
-IFNE L1191
+IFNE L1193
@@ -1223 +1225 @@
-IF_ICMPNE L1209
+IF_ICMPNE L1211
@@ -1227 +1229 @@
-GOTO L1219
+GOTO L1221
@@ -1269 +1271 @@
-IFLT L1260
+IFLT L1262
@@ -1302 +1304 @@
-IFLE L1340
+IFLE L1342
@@ -1305 +1307 @@
-IFNE L1340
+IFNE L1342
@@ -1327 +1329 @@
-IFEQ L1340
+IFEQ L1342
@@ -1334 +1336 @@
-IF_ACMPEQ L1306
+IF_ACMPEQ L1308
@@ -1349,2 +1351,2 @@
-IF_ICMPLE L1332
-GOTO L1306
+IF_ICMPLE L1334
+GOTO L1308
@@ -1358 +1360 @@
-GOTO L1306
+GOTO L1308
@@ -1360 +1362 @@
-IFGE L1349
+IFGE L1351
@@ -1367 +1369 @@
-GOTO L1371
+GOTO L1373
@@ -1408 +1410 @@
-IF_ACMPNE L1529
+IF_ACMPNE L1531
@@ -1412 +1414 @@
-IF_ACMPEQ L1398
+IF_ACMPEQ L1400
@@ -1416,7 +1418,7 @@
-IF_ACMPNE L1404
-ALOAD 0
-GETFIELD local/lito381/server/ClientSession.observer:Z
-IFNE L1404
-ALOAD 0
-GETFIELD local/lito381/server/ClientSession.playerHp:I
-IFGT L1415
+IF_ACMPNE L1406
+ALOAD 0
+GETFIELD local/lito381/server/ClientSession.observer:Z
+IFNE L1406
+ALOAD 0
+GETFIELD local/lito381/server/ClientSession.playerHp:I
+IFGT L1417
@@ -1444 +1446 @@
-IFNE L1427
+IFNE L1429
@@ -1485 +1487 @@
-IF_ACMPNE L1472
+IF_ACMPNE L1474
@@ -1490 +1492 @@
-GOTO L1477
+GOTO L1479
@@ -1502 +1504 @@
-IFEQ L1514
+IFEQ L1516
@@ -1510 +1512 @@
-IF_ACMPNE L1507
+IF_ACMPNE L1509
@@ -1514 +1516 @@
-IFEQ L1481
+IFEQ L1483
@@ -1524,2 +1526,2 @@
-IFNE L1507
-GOTO L1481
+IFNE L1509
+GOTO L1483
@@ -1532 +1534 @@
-GOTO L1481
+GOTO L1483
@@ -1551,10 +1553,11 @@
-IF_ACMPNE L1631
-ALOAD 0
-INVOKESTATIC local/lito381/server/NativeWorldPotionPhase.allowed(Llocal/lito381/server/ClientSession;)Z
-IFEQ L1542
-ALOAD 0
-GETFIELD local/lito381/server/ClientSession.observer:Z
-IFNE L1542
-ALOAD 0
-GETFIELD local/lito381/server/ClientSession.playerHp:I
-IFGT L1553
+IF_ACMPNE L1634
+ALOAD 0
+GETFIELD local/lito381/server/ClientSession.phase:Llocal/lito381/server/ClientSession$Phase;
+GETSTATIC local/lito381/server/ClientSession$Phase.BATTLE:Llocal/lito381/server/ClientSession$Phase;
+IF_ACMPNE L1545
+ALOAD 0
+GETFIELD local/lito381/server/ClientSession.observer:Z
+IFNE L1545
+ALOAD 0
+GETFIELD local/lito381/server/ClientSession.playerHp:I
+IFGT L1556
@@ -1582 +1585 @@
-IFNE L1565
+IFNE L1568
@@ -1627 +1630 @@
-IFEQ L1620
+IFEQ L1623
@@ -1638 +1641 @@
-GOTO L1606
+GOTO L1609
@@ -1653,10 +1656,11 @@
-IF_ACMPNE L1745
-ALOAD 0
-INVOKESTATIC local/lito381/server/NativeWorldPotionPhase.allowed(Llocal/lito381/server/ClientSession;)Z
-IFEQ L1644
-ALOAD 0
-GETFIELD local/lito381/server/ClientSession.observer:Z
-IFNE L1644
-ALOAD 0
-GETFIELD local/lito381/server/ClientSession.playerHp:I
-IFGT L1655
+IF_ACMPNE L1749
+ALOAD 0
+GETFIELD local/lito381/server/ClientSession.phase:Llocal/lito381/server/ClientSession$Phase;
+GETSTATIC local/lito381/server/ClientSession$Phase.BATTLE:Llocal/lito381/server/ClientSession$Phase;
+IF_ACMPNE L1648
+ALOAD 0
+GETFIELD local/lito381/server/ClientSession.observer:Z
+IFNE L1648
+ALOAD 0
+GETFIELD local/lito381/server/ClientSession.playerHp:I
+IFGT L1659
@@ -1677 +1681 @@
-IFNE L1669
+IFNE L1673
@@ -1698 +1702 @@
-IFNE L1681
+IFNE L1685
@@ -1742 +1746 @@
-IFEQ L1735
+IFEQ L1739
@@ -1753 +1757 @@
-GOTO L1721
+GOTO L1725
@@ -1767,10 +1771,11 @@
-IF_ACMPNE L1862
-ALOAD 0
-INVOKESTATIC local/lito381/server/NativeWorldPotionPhase.allowed(Llocal/lito381/server/ClientSession;)Z
-IFEQ L1758
-ALOAD 0
-GETFIELD local/lito381/server/ClientSession.observer:Z
-IFNE L1758
-ALOAD 0
-GETFIELD local/lito381/server/ClientSession.playerHp:I
-IFGT L1769
+IF_ACMPNE L1867
+ALOAD 0
+GETFIELD local/lito381/server/ClientSession.phase:Llocal/lito381/server/ClientSession$Phase;
+GETSTATIC local/lito381/server/ClientSession$Phase.BATTLE:Llocal/lito381/server/ClientSession$Phase;
+IF_ACMPNE L1763
+ALOAD 0
+GETFIELD local/lito381/server/ClientSession.observer:Z
+IFNE L1763
+ALOAD 0
+GETFIELD local/lito381/server/ClientSession.playerHp:I
+IFGT L1774
@@ -1791 +1796 @@
-IFNE L1783
+IFNE L1788
@@ -1812 +1817 @@
-IFNE L1795
+IFNE L1800
@@ -1858 +1863 @@
-IFEQ L1851
+IFEQ L1856
@@ -1869 +1874 @@
-GOTO L1837
+GOTO L1842
@@ -1884,10 +1889,11 @@
-IF_ACMPNE L1997
-ALOAD 0
-INVOKESTATIC local/lito381/server/NativeWorldPotionPhase.allowed(Llocal/lito381/server/ClientSession;)Z
-IFEQ L1875
-ALOAD 0
-GETFIELD local/lito381/server/ClientSession.observer:Z
-IFNE L1875
-ALOAD 0
-GETFIELD local/lito381/server/ClientSession.playerHp:I
-IFGT L1886
+IF_ACMPNE L2003
+ALOAD 0
+GETFIELD local/lito381/server/ClientSession.phase:Llocal/lito381/server/ClientSession$Phase;
+GETSTATIC local/lito381/server/ClientSession$Phase.BATTLE:Llocal/lito381/server/ClientSession$Phase;
+IF_ACMPNE L1881
+ALOAD 0
+GETFIELD local/lito381/server/ClientSession.observer:Z
+IFNE L1881
+ALOAD 0
+GETFIELD local/lito381/server/ClientSession.playerHp:I
+IFGT L1892
@@ -1915 +1921 @@
-IFNE L1898
+IFNE L1904
@@ -1968 +1974 @@
-IFEQ L1980
+IFEQ L1986
@@ -1975 +1981 @@
-IF_ACMPEQ L1973
+IF_ACMPEQ L1979
@@ -1990,2 +1996,2 @@
-IF_ICMPLE L1973
-GOTO L1947
+IF_ICMPLE L1979
+GOTO L1953
@@ -1998 +2004 @@
-GOTO L1947
+GOTO L1953
@@ -2019 +2025 @@
-IF_ACMPNE L2005
+IF_ACMPNE L2011
@@ -2027 +2033 @@
-IF_ACMPNE L2013
+IF_ACMPNE L2019
@@ -2035 +2041 @@
-IF_ACMPNE L2021
+IF_ACMPNE L2027
@@ -2043 +2049 @@
-IF_ACMPNE L2029
+IF_ACMPNE L2035
@@ -2051 +2057 @@
-IF_ACMPNE L2041
+IF_ACMPNE L2047
@@ -2063 +2069 @@
-IF_ACMPNE L2245
+IF_ACMPNE L2254
@@ -2067 +2073 @@
-IF_ACMPEQ L2053
+IF_ACMPEQ L2059
@@ -2071,7 +2077,7 @@
-IF_ACMPNE L2059
-ALOAD 0
-GETFIELD local/lito381/server/ClientSession.observer:Z
-IFNE L2059
-ALOAD 0
-GETFIELD local/lito381/server/ClientSession.playerHp:I
-IFGT L2070
+IF_ACMPNE L2065
+ALOAD 0
+GETFIELD local/lito381/server/ClientSession.observer:Z
+IFNE L2065
+ALOAD 0
+GETFIELD local/lito381/server/ClientSession.playerHp:I
+IFGT L2076
@@ -2099 +2105 @@
-IFNE L2082
+IFNE L2088
@@ -2104,0 +2111 @@
+DUP
@@ -2106 +2113 @@
-ALOAD 0
+SWAP
@@ -2110,0 +2118 @@
+DUP
@@ -2112 +2120 @@
-ALOAD 0
+SWAP
@@ -2119,2 +2127,3 @@
-IF_ACMPNE L2107
-ALOAD 0
+IF_ACMPNE L2116
+ALOAD 0
+DUP
@@ -2122 +2131 @@
-ALOAD 0
+SWAP
@@ -2125 +2134 @@
-GOTO L2112
+GOTO L2121
@@ -2133 +2142 @@
-IFNE L2120
+IFNE L2129
@@ -2135 +2144 @@
-IFNE L2120
+IFNE L2129
@@ -2138 +2147 @@
-IFEQ L2122
+IFEQ L2131
@@ -2140 +2149 @@
-GOTO L2123
+GOTO L2132
@@ -2144 +2153 @@
-IFEQ L2129
+IFEQ L2138
@@ -2149 +2158 @@
-IFEQ L2141
+IFEQ L2150
@@ -2153 +2162 @@
-IF_ACMPNE L2141
+IF_ACMPNE L2150
@@ -2181 +2190 @@
-IF_ACMPNE L2168
+IF_ACMPNE L2177
@@ -2186 +2195 @@
-GOTO L2173
+GOTO L2182
@@ -2198 +2207 @@
-IFEQ L2206
+IFEQ L2215
@@ -2206 +2215 @@
-IF_ACMPNE L2199
+IF_ACMPNE L2208
@@ -2216,2 +2225,2 @@
-IFNE L2199
-GOTO L2177
+IFNE L2208
+GOTO L2186
@@ -2224 +2233 @@
-GOTO L2177
+GOTO L2186
@@ -2232 +2241 @@
-IFEQ L2219
+IFEQ L2228
@@ -2239 +2248 @@
-IFEQ L2229
+IFEQ L2238
@@ -2249 +2258 @@
-IFEQ L2238
+IFEQ L2247
@@ -2267,10 +2276,11 @@
-IF_ACMPNE L2318
-ALOAD 0
-INVOKESTATIC local/lito381/server/NativeWorldPotionPhase.allowed(Llocal/lito381/server/ClientSession;)Z
-IFEQ L2258
-ALOAD 0
-GETFIELD local/lito381/server/ClientSession.observer:Z
-IFNE L2258
-ALOAD 0
-GETFIELD local/lito381/server/ClientSession.playerHp:I
-IFGT L2269
+IF_ACMPNE L2328
+ALOAD 0
+GETFIELD local/lito381/server/ClientSession.phase:Llocal/lito381/server/ClientSession$Phase;
+GETSTATIC local/lito381/server/ClientSession$Phase.BATTLE:Llocal/lito381/server/ClientSession$Phase;
+IF_ACMPNE L2268
+ALOAD 0
+GETFIELD local/lito381/server/ClientSession.observer:Z
+IFNE L2268
+ALOAD 0
+GETFIELD local/lito381/server/ClientSession.playerHp:I
+IFGT L2279
@@ -2298 +2308 @@
-IFNE L2281
+IFNE L2291
@@ -2340,10 +2350,11 @@
-IF_ACMPNE L2428
-ALOAD 0
-INVOKESTATIC local/lito381/server/NativeWorldPotionPhase.allowed(Llocal/lito381/server/ClientSession;)Z
-IFEQ L2331
-ALOAD 0
-GETFIELD local/lito381/server/ClientSession.observer:Z
-IFNE L2331
-ALOAD 0
-GETFIELD local/lito381/server/ClientSession.playerHp:I
-IFGT L2342
+IF_ACMPNE L2439
+ALOAD 0
+GETFIELD local/lito381/server/ClientSession.phase:Llocal/lito381/server/ClientSession$Phase;
+GETSTATIC local/lito381/server/ClientSession$Phase.BATTLE:Llocal/lito381/server/ClientSession$Phase;
+IF_ACMPNE L2342
+ALOAD 0
+GETFIELD local/lito381/server/ClientSession.observer:Z
+IFNE L2342
+ALOAD 0
+GETFIELD local/lito381/server/ClientSession.playerHp:I
+IFGT L2353
@@ -2371 +2382 @@
-IFNE L2354
+IFNE L2365
@@ -2409 +2420 @@
-IFEQ L2402
+IFEQ L2413
@@ -2420 +2431 @@
-GOTO L2388
+GOTO L2399
@@ -2450 +2461 @@
-IF_ACMPNE L2437
+IF_ACMPNE L2448
@@ -2459,10 +2470,11 @@
-IF_ACMPNE L2490
-ALOAD 0
-INVOKESTATIC local/lito381/server/NativeWorldPotionPhase.allowed(Llocal/lito381/server/ClientSession;)Z
-IFEQ L2450
-ALOAD 0
-GETFIELD local/lito381/server/ClientSession.observer:Z
-IFNE L2450
-ALOAD 0
-GETFIELD local/lito381/server/ClientSession.playerHp:I
-IFGT L2461
+IF_ACMPNE L2502
+ALOAD 0
+GETFIELD local/lito381/server/ClientSession.phase:Llocal/lito381/server/ClientSession$Phase;
+GETSTATIC local/lito381/server/ClientSession$Phase.BATTLE:Llocal/lito381/server/ClientSession$Phase;
+IF_ACMPNE L2462
+ALOAD 0
+GETFIELD local/lito381/server/ClientSession.observer:Z
+IFNE L2462
+ALOAD 0
+GETFIELD local/lito381/server/ClientSession.playerHp:I
+IFGT L2473
@@ -2493 +2505 @@
-IFEQ L2484
+IFEQ L2496
@@ -2502 +2514 @@
-GOTO L2472
+GOTO L2484
@@ -2512,10 +2524,11 @@
-IF_ACMPNE L2565
-ALOAD 0
-INVOKESTATIC local/lito381/server/NativeWorldPotionPhase.allowed(Llocal/lito381/server/ClientSession;)Z
-IFEQ L2503
-ALOAD 0
-GETFIELD local/lito381/server/ClientSession.observer:Z
-IFNE L2503
-ALOAD 0
-GETFIELD local/lito381/server/ClientSession.playerHp:I
-IFGT L2514
+IF_ACMPNE L2578
+ALOAD 0
+GETFIELD local/lito381/server/ClientSession.phase:Llocal/lito381/server/ClientSession$Phase;
+GETSTATIC local/lito381/server/ClientSession$Phase.BATTLE:Llocal/lito381/server/ClientSession$Phase;
+IF_ACMPNE L2516
+ALOAD 0
+GETFIELD local/lito381/server/ClientSession.observer:Z
+IFNE L2516
+ALOAD 0
+GETFIELD local/lito381/server/ClientSession.playerHp:I
+IFGT L2527
@@ -2543 +2556 @@
-IFNE L2526
+IFNE L2539
```

</details>

### 登入守門轉拋與整體handler

`local/lito381/server/ClientSession#sourceApplicationRun()V`

<details>
<summary>完整差異（零行未變更上下文，保留所有 hunks）</summary>

```diff
--- staged/local/lito381/server/ClientSession#sourceApplicationRun()V
+++ candidate/local/lito381/server/ClientSession#sourceApplicationRun()V
@@ -3,90 +3,81 @@
-1:[8:java/lang/Exception  ]
-3:[8:java/lang/Exception  ]
-4:[8:java/lang/Exception  ]
-28:[433:java/lang/Exception  , 755:java/lang/Throwable  ]
-31:[106:java/lang/Throwable  , 433:java/lang/Exception  , 755:java/lang/Throwable  ]
-33:[106:java/lang/Throwable  , 433:java/lang/Exception  , 755:java/lang/Throwable  ]
-35:[106:java/lang/Throwable  , 433:java/lang/Exception  , 755:java/lang/Throwable  ]
-37:[106:java/lang/Throwable  , 433:java/lang/Exception  , 755:java/lang/Throwable  ]
-38:[106:java/lang/Throwable  , 433:java/lang/Exception  , 755:java/lang/Throwable  ]
-40:[106:java/lang/Throwable  , 433:java/lang/Exception  , 755:java/lang/Throwable  ]
-41:[106:java/lang/Throwable  , 433:java/lang/Exception  , 755:java/lang/Throwable  ]
-43:[106:java/lang/Throwable  , 433:java/lang/Exception  , 755:java/lang/Throwable  ]
-45:[106:java/lang/Throwable  , 433:java/lang/Exception  , 755:java/lang/Throwable  ]
-47:[106:java/lang/Throwable  , 433:java/lang/Exception  , 755:java/lang/Throwable  ]
-49:[106:java/lang/Throwable  , 433:java/lang/Exception  , 755:java/lang/Throwable  ]
-50:[106:java/lang/Throwable  , 433:java/lang/Exception  , 755:java/lang/Throwable  ]
-54:[106:java/lang/Throwable  , 433:java/lang/Exception  , 755:java/lang/Throwable  ]
-56:[106:java/lang/Throwable  , 433:java/lang/Exception  , 755:java/lang/Throwable  ]
-58:[106:java/lang/Throwable  , 433:java/lang/Exception  , 755:java/lang/Throwable  ]
-61:[106:java/lang/Throwable  , 433:java/lang/Exception  , 755:java/lang/Throwable  ]
-62:[106:java/lang/Throwable  , 433:java/lang/Exception  , 755:java/lang/Throwable  ]
-64:[106:java/lang/Throwable  , 433:java/lang/Exception  , 755:java/lang/Throwable  ]
-67:[106:java/lang/Throwable  , 433:java/lang/Exception  , 755:java/lang/Throwable  ]
-68:[106:java/lang/Throwable  , 433:java/lang/Exception  , 755:java/lang/Throwable  ]
-70:[106:java/lang/Throwable  , 433:java/lang/Exception  , 755:java/lang/Throwable  ]
-71:[106:java/lang/Throwable  , 433:java/lang/Exception  , 755:java/lang/Throwable  ]
-73:[106:java/lang/Throwable  , 433:java/lang/Exception  , 755:java/lang/Throwable  ]
-74:[106:java/lang/Throwable  , 433:java/lang/Exception  , 755:java/lang/Throwable  ]
-76:[106:java/lang/Throwable  , 433:java/lang/Exception  , 755:java/lang/Throwable  ]
-82:[106:java/lang/Throwable  , 433:java/lang/Exception  , 755:java/lang/Throwable  ]
-84:[106:java/lang/Throwable  , 433:java/lang/Exception  , 755:java/lang/Throwable  ]
-87:[106:java/lang/Throwable  , 433:java/lang/Exception  , 755:java/lang/Throwable  ]
-91:[106:java/lang/Throwable  , 433:java/lang/Exception  , 755:java/lang/Throwable  ]
-95:[106:java/lang/Throwable  , 433:java/lang/Exception  , 755:java/lang/Throwable  ]
-96:[106:java/lang/Throwable  , 433:java/lang/Exception  , 755:java/lang/Throwable  ]
-99:[106:java/lang/Throwable  , 433:java/lang/Exception  , 755:java/lang/Throwable  ]
-101:[106:java/lang/Throwable  , 433:java/lang/Exception  , 755:java/lang/Throwable  ]
-111:[113:java/lang/Throwable  , 433:java/lang/Exception  , 755:java/lang/Throwable  ]
-116:[433:java/lang/Exception  , 755:java/lang/Throwable  ]
-118:[433:java/lang/Exception  , 755:java/lang/Throwable  ]
-123:[433:java/lang/Exception  , 755:java/lang/Throwable  ]
-150:[152:java/io/IOException  ]
-164:[166:java/io/IOException  ]
-177:[179:java/io/IOException  , 179:java/lang/RuntimeException  ]
-186:[188:java/io/IOException  , 188:java/lang/RuntimeException  ]
-195:[197:java/io/IOException  , 197:java/lang/RuntimeException  ]
-204:[206:java/io/IOException  , 206:java/lang/RuntimeException  ]
-213:[215:java/io/IOException  , 215:java/lang/RuntimeException  ]
-222:[224:java/io/IOException  , 224:java/lang/RuntimeException  ]
-231:[233:java/io/IOException  , 233:java/lang/RuntimeException  ]
-240:[242:java/io/IOException  , 242:java/lang/RuntimeException  ]
-249:[251:java/io/IOException  , 251:java/lang/RuntimeException  ]
-258:[260:java/io/IOException  , 260:java/lang/RuntimeException  ]
-267:[269:java/io/IOException  , 269:java/lang/RuntimeException  ]
-276:[278:java/io/IOException  , 278:java/lang/RuntimeException  ]
-285:[287:java/io/IOException  , 287:java/lang/RuntimeException  ]
-299:[307:java/io/IOException  , 307:java/lang/RuntimeException  ]
-302:[307:java/io/IOException  , 307:java/lang/RuntimeException  ]
-305:[307:java/io/IOException  , 307:java/lang/RuntimeException  ]
-314:[331:java/io/IOException  , 331:java/lang/RuntimeException  ]
-317:[331:java/io/IOException  , 331:java/lang/RuntimeException  ]
-321:[325:java/lang/Throwable  , 331:java/io/IOException  , 331:java/lang/RuntimeException  ]
-323:[325:java/lang/Throwable  , 331:java/io/IOException  , 331:java/lang/RuntimeException  ]
-327:[325:java/lang/Throwable  , 331:java/io/IOException  , 331:java/lang/RuntimeException  ]
-329:[331:java/io/IOException  , 331:java/lang/RuntimeException  ]
-338:[340:java/lang/Exception  ]
-350:[352:java/io/IOException  ]
-400:[402:java/io/IOException  ]
-422:[424:java/io/IOException  ]
-434:[755:java/lang/Throwable  ]
-436:[755:java/lang/Throwable  ]
-437:[755:java/lang/Throwable  ]
-440:[755:java/lang/Throwable  ]
-441:[755:java/lang/Throwable  ]
-443:[755:java/lang/Throwable  ]
-444:[755:java/lang/Throwable  ]
-445:[755:java/lang/Throwable  ]
-472:[474:java/io/IOException  ]
-486:[488:java/io/IOException  ]
-499:[501:java/io/IOException  , 501:java/lang/RuntimeException  ]
-508:[510:java/io/IOException  , 510:java/lang/RuntimeException  ]
-517:[519:java/io/IOException  , 519:java/lang/RuntimeException  ]
-526:[528:java/io/IOException  , 528:java/lang/RuntimeException  ]
-535:[537:java/io/IOException  , 537:java/lang/RuntimeException  ]
-544:[546:java/io/IOException  , 546:java/lang/RuntimeException  ]
-553:[555:java/io/IOException  , 555:java/lang/RuntimeException  ]
-562:[564:java/io/IOException  , 564:java/lang/RuntimeException  ]
-571:[573:java/io/IOException  , 573:java/lang/RuntimeException  ]
-580:[582:java/io/IOException  , 582:java/lang/RuntimeException  ]
-589:[591:java/io/IOException  , 591:java/lang/RuntimeException  ]
+24:[740:java/lang/Exception  , 1062:java/lang/Throwable  ]
+27:[727:java/lang/Throwable  , 740:java/lang/Exception  , 1062:java/lang/Throwable  ]
+29:[727:java/lang/Throwable  , 740:java/lang/Exception  , 1062:java/lang/Throwable  ]
+31:[727:java/lang/Throwable  , 740:java/lang/Exception  , 1062:java/lang/Throwable  ]
+33:[727:java/lang/Throwable  , 740:java/lang/Exception  , 1062:java/lang/Throwable  ]
+34:[727:java/lang/Throwable  , 740:java/lang/Exception  , 1062:java/lang/Throwable  ]
+36:[727:java/lang/Throwable  , 740:java/lang/Exception  , 1062:java/lang/Throwable  ]
+37:[727:java/lang/Throwable  , 740:java/lang/Exception  , 1062:java/lang/Throwable  ]
+39:[727:java/lang/Throwable  , 740:java/lang/Exception  , 1062:java/lang/Throwable  ]
+41:[727:java/lang/Throwable  , 740:java/lang/Exception  , 1062:java/lang/Throwable  ]
+43:[727:java/lang/Throwable  , 740:java/lang/Exception  , 1062:java/lang/Throwable  ]
+45:[727:java/lang/Throwable  , 740:java/lang/Exception  , 1062:java/lang/Throwable  ]
+46:[727:java/lang/Throwable  , 740:java/lang/Exception  , 1062:java/lang/Throwable  ]
+50:[727:java/lang/Throwable  , 740:java/lang/Exception  , 1062:java/lang/Throwable  ]
+52:[727:java/lang/Throwable  , 740:java/lang/Exception  , 1062:java/lang/Throwable  ]
+54:[727:java/lang/Throwable  , 740:java/lang/Exception  , 1062:java/lang/Throwable  ]
+57:[727:java/lang/Throwable  , 740:java/lang/Exception  , 1062:java/lang/Throwable  ]
+58:[727:java/lang/Throwable  , 740:java/lang/Exception  , 1062:java/lang/Throwable  ]
+60:[727:java/lang/Throwable  , 740:java/lang/Exception  , 1062:java/lang/Throwable  ]
+63:[727:java/lang/Throwable  , 740:java/lang/Exception  , 1062:java/lang/Throwable  ]
+64:[727:java/lang/Throwable  , 740:java/lang/Exception  , 1062:java/lang/Throwable  ]
+66:[727:java/lang/Throwable  , 740:java/lang/Exception  , 1062:java/lang/Throwable  ]
+67:[727:java/lang/Throwable  , 740:java/lang/Exception  , 1062:java/lang/Throwable  ]
+69:[727:java/lang/Throwable  , 740:java/lang/Exception  , 1062:java/lang/Throwable  ]
+70:[727:java/lang/Throwable  , 740:java/lang/Exception  , 1062:java/lang/Throwable  ]
+72:[727:java/lang/Throwable  , 740:java/lang/Exception  , 1062:java/lang/Throwable  ]
+80:[740:java/lang/Exception  , 1062:java/lang/Throwable  ]
+107:[109:java/io/IOException  ]
+121:[123:java/io/IOException  ]
+134:[136:java/io/IOException  , 136:java/lang/RuntimeException  ]
+143:[145:java/io/IOException  , 145:java/lang/RuntimeException  ]
+152:[154:java/io/IOException  , 154:java/lang/RuntimeException  ]
+161:[163:java/io/IOException  , 163:java/lang/RuntimeException  ]
+170:[172:java/io/IOException  , 172:java/lang/RuntimeException  ]
+179:[181:java/io/IOException  , 181:java/lang/RuntimeException  ]
+188:[190:java/io/IOException  , 190:java/lang/RuntimeException  ]
+197:[199:java/io/IOException  , 199:java/lang/RuntimeException  ]
+206:[208:java/io/IOException  , 208:java/lang/RuntimeException  ]
+215:[217:java/io/IOException  , 217:java/lang/RuntimeException  ]
+224:[226:java/io/IOException  , 226:java/lang/RuntimeException  ]
+233:[235:java/io/IOException  , 235:java/lang/RuntimeException  ]
+242:[244:java/io/IOException  , 244:java/lang/RuntimeException  ]
+256:[264:java/io/IOException  , 264:java/lang/RuntimeException  ]
+259:[264:java/io/IOException  , 264:java/lang/RuntimeException  ]
+262:[264:java/io/IOException  , 264:java/lang/RuntimeException  ]
+271:[288:java/io/IOException  , 288:java/lang/RuntimeException  ]
+274:[288:java/io/IOException  , 288:java/lang/RuntimeException  ]
+278:[282:java/lang/Throwable  , 288:java/io/IOException  , 288:java/lang/RuntimeException  ]
+280:[282:java/lang/Throwable  , 288:java/io/IOException  , 288:java/lang/RuntimeException  ]
+284:[282:java/lang/Throwable  , 288:java/io/IOException  , 288:java/lang/RuntimeException  ]
+286:[288:java/io/IOException  , 288:java/lang/RuntimeException  ]
+295:[297:java/lang/Exception  ]
+307:[309:java/io/IOException  ]
+357:[359:java/io/IOException  ]
+379:[381:java/io/IOException  ]
+391:[727:java/lang/Throwable  , 740:java/lang/Exception  , 1062:java/lang/Throwable  ]
+393:[727:java/lang/Throwable  , 740:java/lang/Exception  , 1062:java/lang/Throwable  ]
+396:[727:java/lang/Throwable  , 740:java/lang/Exception  , 1062:java/lang/Throwable  ]
+400:[727:java/lang/Throwable  , 740:java/lang/Exception  , 1062:java/lang/Throwable  ]
+404:[727:java/lang/Throwable  , 740:java/lang/Exception  , 1062:java/lang/Throwable  ]
+405:[727:java/lang/Throwable  , 740:java/lang/Exception  , 1062:java/lang/Throwable  ]
+408:[727:java/lang/Throwable  , 740:java/lang/Exception  , 1062:java/lang/Throwable  ]
+410:[727:java/lang/Throwable  , 740:java/lang/Exception  , 1062:java/lang/Throwable  ]
+416:[740:java/lang/Exception  , 1062:java/lang/Throwable  ]
+443:[445:java/io/IOException  ]
+457:[459:java/io/IOException  ]
+470:[472:java/io/IOException  , 472:java/lang/RuntimeException  ]
+479:[481:java/io/IOException  , 481:java/lang/RuntimeException  ]
+488:[490:java/io/IOException  , 490:java/lang/RuntimeException  ]
+497:[499:java/io/IOException  , 499:java/lang/RuntimeException  ]
+506:[508:java/io/IOException  , 508:java/lang/RuntimeException  ]
+515:[517:java/io/IOException  , 517:java/lang/RuntimeException  ]
+524:[526:java/io/IOException  , 526:java/lang/RuntimeException  ]
+533:[535:java/io/IOException  , 535:java/lang/RuntimeException  ]
+542:[544:java/io/IOException  , 544:java/lang/RuntimeException  ]
+551:[553:java/io/IOException  , 553:java/lang/RuntimeException  ]
+560:[562:java/io/IOException  , 562:java/lang/RuntimeException  ]
+569:[571:java/io/IOException  , 571:java/lang/RuntimeException  ]
+578:[580:java/io/IOException  , 580:java/lang/RuntimeException  ]
+592:[600:java/io/IOException  , 600:java/lang/RuntimeException  ]
+595:[600:java/io/IOException  , 600:java/lang/RuntimeException  ]
@@ -94,42 +85,77 @@
-607:[609:java/io/IOException  , 609:java/lang/RuntimeException  ]
-621:[629:java/io/IOException  , 629:java/lang/RuntimeException  ]
-624:[629:java/io/IOException  , 629:java/lang/RuntimeException  ]
-627:[629:java/io/IOException  , 629:java/lang/RuntimeException  ]
-636:[653:java/io/IOException  , 653:java/lang/RuntimeException  ]
-639:[653:java/io/IOException  , 653:java/lang/RuntimeException  ]
-643:[647:java/lang/Throwable  , 653:java/io/IOException  , 653:java/lang/RuntimeException  ]
-645:[647:java/lang/Throwable  , 653:java/io/IOException  , 653:java/lang/RuntimeException  ]
-649:[647:java/lang/Throwable  , 653:java/io/IOException  , 653:java/lang/RuntimeException  ]
-651:[653:java/io/IOException  , 653:java/lang/RuntimeException  ]
-660:[662:java/lang/Exception  ]
-672:[674:java/io/IOException  ]
-722:[724:java/io/IOException  ]
-744:[746:java/io/IOException  ]
-782:[784:java/io/IOException  ]
-796:[798:java/io/IOException  ]
-809:[811:java/io/IOException  , 811:java/lang/RuntimeException  ]
-818:[820:java/io/IOException  , 820:java/lang/RuntimeException  ]
-827:[829:java/io/IOException  , 829:java/lang/RuntimeException  ]
-836:[838:java/io/IOException  , 838:java/lang/RuntimeException  ]
-845:[847:java/io/IOException  , 847:java/lang/RuntimeException  ]
-854:[856:java/io/IOException  , 856:java/lang/RuntimeException  ]
-863:[865:java/io/IOException  , 865:java/lang/RuntimeException  ]
-872:[874:java/io/IOException  , 874:java/lang/RuntimeException  ]
-881:[883:java/io/IOException  , 883:java/lang/RuntimeException  ]
-890:[892:java/io/IOException  , 892:java/lang/RuntimeException  ]
-899:[901:java/io/IOException  , 901:java/lang/RuntimeException  ]
-908:[910:java/io/IOException  , 910:java/lang/RuntimeException  ]
-917:[919:java/io/IOException  , 919:java/lang/RuntimeException  ]
-931:[939:java/io/IOException  , 939:java/lang/RuntimeException  ]
-934:[939:java/io/IOException  , 939:java/lang/RuntimeException  ]
-937:[939:java/io/IOException  , 939:java/lang/RuntimeException  ]
-946:[963:java/io/IOException  , 963:java/lang/RuntimeException  ]
-949:[963:java/io/IOException  , 963:java/lang/RuntimeException  ]
-953:[957:java/lang/Throwable  , 963:java/io/IOException  , 963:java/lang/RuntimeException  ]
-955:[957:java/lang/Throwable  , 963:java/io/IOException  , 963:java/lang/RuntimeException  ]
-959:[957:java/lang/Throwable  , 963:java/io/IOException  , 963:java/lang/RuntimeException  ]
-961:[963:java/io/IOException  , 963:java/lang/RuntimeException  ]
-970:[972:java/lang/Exception  ]
-982:[984:java/io/IOException  ]
-1032:[1034:java/io/IOException  ]
-1054:[1056:java/io/IOException  ]
+607:[624:java/io/IOException  , 624:java/lang/RuntimeException  ]
+610:[624:java/io/IOException  , 624:java/lang/RuntimeException  ]
+614:[618:java/lang/Throwable  , 624:java/io/IOException  , 624:java/lang/RuntimeException  ]
+616:[618:java/lang/Throwable  , 624:java/io/IOException  , 624:java/lang/RuntimeException  ]
+620:[618:java/lang/Throwable  , 624:java/io/IOException  , 624:java/lang/RuntimeException  ]
+622:[624:java/io/IOException  , 624:java/lang/RuntimeException  ]
+631:[633:java/lang/Exception  ]
+643:[645:java/io/IOException  ]
+693:[695:java/io/IOException  ]
+715:[717:java/io/IOException  ]
+732:[734:java/lang/Throwable  , 740:java/lang/Exception  , 1062:java/lang/Throwable  ]
+737:[740:java/lang/Exception  , 1062:java/lang/Throwable  ]
+739:[740:java/lang/Exception  , 1062:java/lang/Throwable  ]
+741:[1062:java/lang/Throwable  ]
+743:[1062:java/lang/Throwable  ]
+744:[1062:java/lang/Throwable  ]
+747:[1062:java/lang/Throwable  ]
+748:[1062:java/lang/Throwable  ]
+750:[1062:java/lang/Throwable  ]
+751:[1062:java/lang/Throwable  ]
+752:[1062:java/lang/Throwable  ]
+779:[781:java/io/IOException  ]
+793:[795:java/io/IOException  ]
+806:[808:java/io/IOException  , 808:java/lang/RuntimeException  ]
+815:[817:java/io/IOException  , 817:java/lang/RuntimeException  ]
+824:[826:java/io/IOException  , 826:java/lang/RuntimeException  ]
+833:[835:java/io/IOException  , 835:java/lang/RuntimeException  ]
+842:[844:java/io/IOException  , 844:java/lang/RuntimeException  ]
+851:[853:java/io/IOException  , 853:java/lang/RuntimeException  ]
+860:[862:java/io/IOException  , 862:java/lang/RuntimeException  ]
+869:[871:java/io/IOException  , 871:java/lang/RuntimeException  ]
+878:[880:java/io/IOException  , 880:java/lang/RuntimeException  ]
+887:[889:java/io/IOException  , 889:java/lang/RuntimeException  ]
+896:[898:java/io/IOException  , 898:java/lang/RuntimeException  ]
+905:[907:java/io/IOException  , 907:java/lang/RuntimeException  ]
+914:[916:java/io/IOException  , 916:java/lang/RuntimeException  ]
+928:[936:java/io/IOException  , 936:java/lang/RuntimeException  ]
+931:[936:java/io/IOException  , 936:java/lang/RuntimeException  ]
+934:[936:java/io/IOException  , 936:java/lang/RuntimeException  ]
+943:[960:java/io/IOException  , 960:java/lang/RuntimeException  ]
+946:[960:java/io/IOException  , 960:java/lang/RuntimeException  ]
+950:[954:java/lang/Throwable  , 960:java/io/IOException  , 960:java/lang/RuntimeException  ]
+952:[954:java/lang/Throwable  , 960:java/io/IOException  , 960:java/lang/RuntimeException  ]
+956:[954:java/lang/Throwable  , 960:java/io/IOException  , 960:java/lang/RuntimeException  ]
+958:[960:java/io/IOException  , 960:java/lang/RuntimeException  ]
+967:[969:java/lang/Exception  ]
+979:[981:java/io/IOException  ]
+1029:[1031:java/io/IOException  ]
+1051:[1053:java/io/IOException  ]
+1089:[1091:java/io/IOException  ]
+1103:[1105:java/io/IOException  ]
+1116:[1118:java/io/IOException  , 1118:java/lang/RuntimeException  ]
+1125:[1127:java/io/IOException  , 1127:java/lang/RuntimeException  ]
+1134:[1136:java/io/IOException  , 1136:java/lang/RuntimeException  ]
+1143:[1145:java/io/IOException  , 1145:java/lang/RuntimeException  ]
+1152:[1154:java/io/IOException  , 1154:java/lang/RuntimeException  ]
+1161:[1163:java/io/IOException  , 1163:java/lang/RuntimeException  ]
+1170:[1172:java/io/IOException  , 1172:java/lang/RuntimeException  ]
+1179:[1181:java/io/IOException  , 1181:java/lang/RuntimeException  ]
+1188:[1190:java/io/IOException  , 1190:java/lang/RuntimeException  ]
+1197:[1199:java/io/IOException  , 1199:java/lang/RuntimeException  ]
+1206:[1208:java/io/IOException  , 1208:java/lang/RuntimeException  ]
+1215:[1217:java/io/IOException  , 1217:java/lang/RuntimeException  ]
+1224:[1226:java/io/IOException  , 1226:java/lang/RuntimeException  ]
+1238:[1246:java/io/IOException  , 1246:java/lang/RuntimeException  ]
+1241:[1246:java/io/IOException  , 1246:java/lang/RuntimeException  ]
+1244:[1246:java/io/IOException  , 1246:java/lang/RuntimeException  ]
+1253:[1270:java/io/IOException  , 1270:java/lang/RuntimeException  ]
+1256:[1270:java/io/IOException  , 1270:java/lang/RuntimeException  ]
+1260:[1264:java/lang/Throwable  , 1270:java/io/IOException  , 1270:java/lang/RuntimeException  ]
+1262:[1264:java/lang/Throwable  , 1270:java/io/IOException  , 1270:java/lang/RuntimeException  ]
+1266:[1264:java/lang/Throwable  , 1270:java/io/IOException  , 1270:java/lang/RuntimeException  ]
+1268:[1270:java/io/IOException  , 1270:java/lang/RuntimeException  ]
+1277:[1279:java/lang/Exception  ]
+1289:[1291:java/io/IOException  ]
+1339:[1341:java/io/IOException  ]
+1361:[1363:java/io/IOException  ]
@@ -143,4 +168,0 @@
-GOTO L11
-ASTORE 1
-ALOAD 1
-INVOKESTATIC local/lito381/server/ClientSession.rethrowUnchecked(Ljava/lang/Throwable;)V
@@ -151 +173 @@
-ASTORE 2
+ASTORE 1
@@ -156 +178 @@
-ALOAD 2
+ALOAD 1
@@ -165 +187 @@
-ASTORE 3
+ASTORE 2
@@ -178 +200 @@
-ISTORE 4
+ISTORE 3
@@ -184 +206 @@
-ILOAD 4
+ILOAD 3
@@ -187,3 +209,3 @@
-ASTORE 5
-ALOAD 0
-ALOAD 5
+ASTORE 4
+ALOAD 0
+ALOAD 4
@@ -196 +218 @@
-ILOAD 4
+ILOAD 3
@@ -202 +224 @@
-ILOAD 4
+ILOAD 3
@@ -213,4 +235,317 @@
-ASTORE 6
-ALOAD 6
-IFNONNULL L81
-GOTO L105
+ASTORE 5
+ALOAD 5
+IFNONNULL L390
+ALOAD 2
+IFNULL L81
+ALOAD 2
+ALOAD 0
+INVOKESTATIC local/lito381/server/NativeServerLifetime.closeSessionSocket(Ljava/net/Socket;Llocal/lito381/server/ClientSession;)V
+ALOAD 0
+INVOKESTATIC local/lito381/server/NativeServerLifetime.awaitCleanup(Llocal/lito381/server/ClientSession;)V
+ALOAD 0
+GETFIELD local/lito381/server/ClientSession.currentRoomId:J
+LSTORE 6/7
+ALOAD 0
+GETFIELD local/lito381/server/ClientSession.phase:Llocal/lito381/server/ClientSession$Phase;
+GETSTATIC local/lito381/server/ClientSession$Phase.ROOM:Llocal/lito381/server/ClientSession$Phase;
+IF_ACMPNE L102
+LLOAD 6/7
+CONST Long:0
+LCMP
+IFEQ L102
+ALOAD 0
+GETFIELD local/lito381/server/ClientSession.hub:Llocal/lito381/server/GameHub;
+LLOAD 6/7
+ALOAD 0
+INVOKEVIRTUAL local/lito381/server/GameHub.isOwner(JLlocal/lito381/server/ClientSession;)Z
+IFEQ L102
+CONST Integer:1
+GOTO L103
+CONST Integer:0
+ISTORE 8
+ALOAD 0
+CONST str:"disconnect"
+CONST Integer:0
+INVOKESPECIAL local/lito381/server/ClientSession.cancelActivePlayerTrade(Ljava/lang/String;Z)V
+GOTO L115
+ASTORE 9
+GETSTATIC java/lang/System.out:Ljava/io/PrintStream;
+ALOAD 9
+INVOKEVIRTUAL java/io/IOException.getMessage()Ljava/lang/String;
+INVOKEDYNAMIC makeConcatWithConstants(Ljava/lang/String;)Ljava/lang/String; bsm=H6 java/lang/invoke/StringConcatFactory.makeConcatWithConstants(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;Ljava/lang/String;[Ljava/lang/Object;)Ljava/lang/invoke/CallSite; args=[str:"[LiTo] trade disconnect cleanup failed: \u0001"]
+INVOKEVIRTUAL java/io/PrintStream.println(Ljava/lang/String;)V
+ALOAD 0
+GETFIELD local/lito381/server/ClientSession.classicPartyParticipant:Z
+IFEQ L129
+ALOAD 0
+CONST str:"disconnect"
+CONST Integer:0
+INVOKESPECIAL local/lito381/server/ClientSession.leaveClassicParty(Ljava/lang/String;Z)V
+GOTO L129
+ASTORE 10
+GETSTATIC java/lang/System.out:Ljava/io/PrintStream;
+ALOAD 10
+INVOKEVIRTUAL java/io/IOException.getMessage()Ljava/lang/String;
+INVOKEDYNAMIC makeConcatWithConstants(Ljava/lang/String;)Ljava/lang/String; bsm=H6 java/lang/invoke/StringConcatFactory.makeConcatWithConstants(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;Ljava/lang/String;[Ljava/lang/Object;)Ljava/lang/invoke/CallSite; args=[str:"[LiTo] party disconnect cleanup failed: \u0001"]
+INVOKEVIRTUAL java/io/PrintStream.println(Ljava/lang/String;)V
+ALOAD 0
+GETFIELD local/lito381/server/ClientSession.phase:Llocal/lito381/server/ClientSession$Phase;
+GETSTATIC local/lito381/server/ClientSession$Phase.WORLD:Llocal/lito381/server/ClientSession$Phase;
+IF_ACMPNE L151
+ALOAD 0
+INVOKESPECIAL local/lito381/server/ClientSession.collectClassicWorldPetsOnDisconnect()V
+GOTO L142
+ASTORE 11
+GETSTATIC java/lang/System.out:Ljava/io/PrintStream;
+ALOAD 11
+INVOKEVIRTUAL java/lang/Exception.getMessage()Ljava/lang/String;
+INVOKEDYNAMIC makeConcatWithConstants(Ljava/lang/String;)Ljava/lang/String; bsm=H6 java/lang/invoke/StringConcatFactory.makeConcatWithConstants(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;Ljava/lang/String;[Ljava/lang/Object;)Ljava/lang/invoke/CallSite; args=[str:"[LiTo] persistent pet disconnect cleanup failed: \u0001"]
+INVOKEVIRTUAL java/io/PrintStream.println(Ljava/lang/String;)V
+ALOAD 0
+INVOKESPECIAL local/lito381/server/ClientSession.cleanupClassicWorldTransientCompanionsOnDisconnect()V
+GOTO L151
+ASTORE 12
+GETSTATIC java/lang/System.out:Ljava/io/PrintStream;
+ALOAD 12
+INVOKEVIRTUAL java/lang/Exception.getMessage()Ljava/lang/String;
+INVOKEDYNAMIC makeConcatWithConstants(Ljava/lang/String;)Ljava/lang/String; bsm=H6 java/lang/invoke/StringConcatFactory.makeConcatWithConstants(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;Ljava/lang/String;[Ljava/lang/Object;)Ljava/lang/invoke/CallSite; args=[str:"[LiTo] transient companion disconnect cleanup failed: \u0001"]
+INVOKEVIRTUAL java/io/PrintStream.println(Ljava/lang/String;)V
+ALOAD 0
+INVOKESPECIAL local/lito381/server/ClientSession.persistExperienceGemOnDisconnect()V
+GOTO L160
+ASTORE 13
+GETSTATIC java/lang/System.out:Ljava/io/PrintStream;
+ALOAD 13
+INVOKEVIRTUAL java/lang/Exception.getMessage()Ljava/lang/String;
+INVOKEDYNAMIC makeConcatWithConstants(Ljava/lang/String;)Ljava/lang/String; bsm=H6 java/lang/invoke/StringConcatFactory.makeConcatWithConstants(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;Ljava/lang/String;[Ljava/lang/Object;)Ljava/lang/invoke/CallSite; args=[str:"[LiTo] Dragon Gem disconnect persistence failed: \u0001"]
+INVOKEVIRTUAL java/io/PrintStream.println(Ljava/lang/String;)V
+ALOAD 0
+INVOKESPECIAL local/lito381/server/ClientSession.persistFireWeaponOnDisconnect()V
+GOTO L169
+ASTORE 14
+GETSTATIC java/lang/System.out:Ljava/io/PrintStream;
+ALOAD 14
+INVOKEVIRTUAL java/lang/Exception.getMessage()Ljava/lang/String;
+INVOKEDYNAMIC makeConcatWithConstants(Ljava/lang/String;)Ljava/lang/String; bsm=H6 java/lang/invoke/StringConcatFactory.makeConcatWithConstants(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;Ljava/lang/String;[Ljava/lang/Object;)Ljava/lang/invoke/CallSite; args=[str:"[LiTo] Fire Weapon disconnect persistence failed: \u0001"]
+INVOKEVIRTUAL java/io/PrintStream.println(Ljava/lang/String;)V
+ALOAD 0
+INVOKESPECIAL local/lito381/server/ClientSession.persistBurningWeaponOnDisconnect()V
+GOTO L178
+ASTORE 15
+GETSTATIC java/lang/System.out:Ljava/io/PrintStream;
+ALOAD 15
+INVOKEVIRTUAL java/lang/Exception.getMessage()Ljava/lang/String;
+INVOKEDYNAMIC makeConcatWithConstants(Ljava/lang/String;)Ljava/lang/String; bsm=H6 java/lang/invoke/StringConcatFactory.makeConcatWithConstants(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;Ljava/lang/String;[Ljava/lang/Object;)Ljava/lang/invoke/CallSite; args=[str:"[LiTo] Burning Weapon disconnect persistence failed: \u0001"]
+INVOKEVIRTUAL java/io/PrintStream.println(Ljava/lang/String;)V
+ALOAD 0
+INVOKESPECIAL local/lito381/server/ClientSession.persistWindShotOnDisconnect()V
+GOTO L187
+ASTORE 16
+GETSTATIC java/lang/System.out:Ljava/io/PrintStream;
+ALOAD 16
+INVOKEVIRTUAL java/lang/Exception.getMessage()Ljava/lang/String;
+INVOKEDYNAMIC makeConcatWithConstants(Ljava/lang/String;)Ljava/lang/String; bsm=H6 java/lang/invoke/StringConcatFactory.makeConcatWithConstants(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;Ljava/lang/String;[Ljava/lang/Object;)Ljava/lang/invoke/CallSite; args=[str:"[LiTo] Wind Shot disconnect persistence failed: \u0001"]
+INVOKEVIRTUAL java/io/PrintStream.println(Ljava/lang/String;)V
+ALOAD 0
+INVOKESPECIAL local/lito381/server/ClientSession.persistFireBlessOnDisconnect()V
+GOTO L196
+ASTORE 17
+GETSTATIC java/lang/System.out:Ljava/io/PrintStream;
+ALOAD 17
+INVOKEVIRTUAL java/lang/Exception.getMessage()Ljava/lang/String;
+INVOKEDYNAMIC makeConcatWithConstants(Ljava/lang/String;)Ljava/lang/String; bsm=H6 java/lang/invoke/StringConcatFactory.makeConcatWithConstants(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;Ljava/lang/String;[Ljava/lang/Object;)Ljava/lang/invoke/CallSite; args=[str:"[LiTo] Fire Bless disconnect persistence failed: \u0001"]
+INVOKEVIRTUAL java/io/PrintStream.println(Ljava/lang/String;)V
+ALOAD 0
+INVOKESPECIAL local/lito381/server/ClientSession.persistStormEyeOnDisconnect()V
+GOTO L205
+ASTORE 18
+GETSTATIC java/lang/System.out:Ljava/io/PrintStream;
+ALOAD 18
+INVOKEVIRTUAL java/lang/Exception.getMessage()Ljava/lang/String;
+INVOKEDYNAMIC makeConcatWithConstants(Ljava/lang/String;)Ljava/lang/String; bsm=H6 java/lang/invoke/StringConcatFactory.makeConcatWithConstants(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;Ljava/lang/String;[Ljava/lang/Object;)Ljava/lang/invoke/CallSite; args=[str:"[LiTo] Storm Eye disconnect persistence failed: \u0001"]
+INVOKEVIRTUAL java/io/PrintStream.println(Ljava/lang/String;)V
+ALOAD 0
+INVOKESPECIAL local/lito381/server/ClientSession.persistStormShotOnDisconnect()V
+GOTO L214
+ASTORE 19
+GETSTATIC java/lang/System.out:Ljava/io/PrintStream;
+ALOAD 19
+INVOKEVIRTUAL java/lang/Exception.getMessage()Ljava/lang/String;
+INVOKEDYNAMIC makeConcatWithConstants(Ljava/lang/String;)Ljava/lang/String; bsm=H6 java/lang/invoke/StringConcatFactory.makeConcatWithConstants(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;Ljava/lang/String;[Ljava/lang/Object;)Ljava/lang/invoke/CallSite; args=[str:"[LiTo] Storm Shot disconnect persistence failed: \u0001"]
+INVOKEVIRTUAL java/io/PrintStream.println(Ljava/lang/String;)V
+ALOAD 0
+INVOKESPECIAL local/lito381/server/ClientSession.persistEarthBlessOnDisconnect()V
+GOTO L223
+ASTORE 20
+GETSTATIC java/lang/System.out:Ljava/io/PrintStream;
+ALOAD 20
+INVOKEVIRTUAL java/lang/Exception.getMessage()Ljava/lang/String;
+INVOKEDYNAMIC makeConcatWithConstants(Ljava/lang/String;)Ljava/lang/String; bsm=H6 java/lang/invoke/StringConcatFactory.makeConcatWithConstants(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;Ljava/lang/String;[Ljava/lang/Object;)Ljava/lang/invoke/CallSite; args=[str:"[LiTo] Earth Bless disconnect persistence failed: \u0001"]
+INVOKEVIRTUAL java/io/PrintStream.println(Ljava/lang/String;)V
+ALOAD 0
+INVOKESPECIAL local/lito381/server/ClientSession.persistIronSkinOnDisconnect()V
+GOTO L232
+ASTORE 21
+GETSTATIC java/lang/System.out:Ljava/io/PrintStream;
+ALOAD 21
+INVOKEVIRTUAL java/lang/Exception.getMessage()Ljava/lang/String;
+INVOKEDYNAMIC makeConcatWithConstants(Ljava/lang/String;)Ljava/lang/String; bsm=H6 java/lang/invoke/StringConcatFactory.makeConcatWithConstants(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;Ljava/lang/String;[Ljava/lang/Object;)Ljava/lang/invoke/CallSite; args=[str:"[LiTo] Iron Skin disconnect persistence failed: \u0001"]
+INVOKEVIRTUAL java/io/PrintStream.println(Ljava/lang/String;)V
+ALOAD 0
+INVOKESPECIAL local/lito381/server/ClientSession.persistMovementBuffOnDisconnect()V
+GOTO L241
+ASTORE 22
+GETSTATIC java/lang/System.out:Ljava/io/PrintStream;
+ALOAD 22
+INVOKEVIRTUAL java/lang/Exception.getMessage()Ljava/lang/String;
+INVOKEDYNAMIC makeConcatWithConstants(Ljava/lang/String;)Ljava/lang/String; bsm=H6 java/lang/invoke/StringConcatFactory.makeConcatWithConstants(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;Ljava/lang/String;[Ljava/lang/Object;)Ljava/lang/invoke/CallSite; args=[str:"[LiTo] movement buff disconnect persistence failed: \u0001"]
+INVOKEVIRTUAL java/io/PrintStream.println(Ljava/lang/String;)V
+ALOAD 0
+INVOKESPECIAL local/lito381/server/ClientSession.persistArmorProtectionOnDisconnect()V
+GOTO L250
+ASTORE 23
+GETSTATIC java/lang/System.out:Ljava/io/PrintStream;
+ALOAD 23
+INVOKEVIRTUAL java/lang/Exception.getMessage()Ljava/lang/String;
+INVOKEDYNAMIC makeConcatWithConstants(Ljava/lang/String;)Ljava/lang/String; bsm=H6 java/lang/invoke/StringConcatFactory.makeConcatWithConstants(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;Ljava/lang/String;[Ljava/lang/Object;)Ljava/lang/invoke/CallSite; args=[str:"[LiTo] armor protection disconnect persistence failed: \u0001"]
+INVOKEVIRTUAL java/io/PrintStream.println(Ljava/lang/String;)V
+ALOAD 0
+GETFIELD local/lito381/server/ClientSession.phase:Llocal/lito381/server/ClientSession$Phase;
+GETSTATIC local/lito381/server/ClientSession$Phase.WORLD:Llocal/lito381/server/ClientSession$Phase;
+IF_ACMPNE L270
+ALOAD 0
+CONST str:"owner-disconnect"
+INVOKESPECIAL local/lito381/server/ClientSession.abandonClassicWorldRescueFollower(Ljava/lang/String;)V
+ALOAD 0
+CONST str:"owner-disconnect"
+INVOKESPECIAL local/lito381/server/ClientSession.abandonClassicWorldDspyFollower(Ljava/lang/String;)V
+ALOAD 0
+CONST str:"owner-disconnect"
+INVOKESPECIAL local/lito381/server/ClientSession.abandonClassicWorldRoiFollower(Ljava/lang/String;)V
+GOTO L270
+ASTORE 24
+GETSTATIC java/lang/System.out:Ljava/io/PrintStream;
+ALOAD 24
+INVOKEVIRTUAL java/lang/Exception.getMessage()Ljava/lang/String;
+INVOKEDYNAMIC makeConcatWithConstants(Ljava/lang/String;)Ljava/lang/String; bsm=H6 java/lang/invoke/StringConcatFactory.makeConcatWithConstants(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;Ljava/lang/String;[Ljava/lang/Object;)Ljava/lang/invoke/CallSite; args=[str:"[LiTo] quest follower disconnect cleanup failed: \u0001"]
+INVOKEVIRTUAL java/io/PrintStream.println(Ljava/lang/String;)V
+ALOAD 0
+GETFIELD local/lito381/server/ClientSession.activeMagicDollObjectIds:Ljava/util/Set;
+DUP
+ASTORE 25
+MONITORENTER
+ALOAD 0
+CONST str:"owner-disconnect"
+CONST Integer:0
+INVOKESPECIAL local/lito381/server/ClientSession.recallNativeMagicDollLocked(Ljava/lang/String;Z)V
+ALOAD 25
+MONITOREXIT
+GOTO L287
+ASTORE 26
+ALOAD 25
+MONITOREXIT
+ALOAD 26
+ATHROW
+GOTO L294
+ASTORE 27
+GETSTATIC java/lang/System.out:Ljava/io/PrintStream;
+ALOAD 27
+INVOKEVIRTUAL java/lang/Exception.getMessage()Ljava/lang/String;
+INVOKEDYNAMIC makeConcatWithConstants(Ljava/lang/String;)Ljava/lang/String; bsm=H6 java/lang/invoke/StringConcatFactory.makeConcatWithConstants(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;Ljava/lang/String;[Ljava/lang/Object;)Ljava/lang/invoke/CallSite; args=[str:"[LiTo] magic-doll disconnect cleanup failed: \u0001"]
+INVOKEVIRTUAL java/io/PrintStream.println(Ljava/lang/String;)V
+ALOAD 0
+INVOKESPECIAL local/lito381/server/ClientSession.persistLitoCharacterOnDisconnect()V
+GOTO L306
+ASTORE 28
+GETSTATIC java/lang/System.out:Ljava/io/PrintStream;
+ALOAD 28
+INVOKEVIRTUAL java/lang/Object.getClass()Ljava/lang/Class;
+INVOKEVIRTUAL java/lang/Class.getSimpleName()Ljava/lang/String;
+ALOAD 28
+INVOKEVIRTUAL java/lang/Exception.getMessage()Ljava/lang/String;
+INVOKEDYNAMIC makeConcatWithConstants(Ljava/lang/String;Ljava/lang/String;)Ljava/lang/String; bsm=H6 java/lang/invoke/StringConcatFactory.makeConcatWithConstants(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;Ljava/lang/String;[Ljava/lang/Object;)Ljava/lang/invoke/CallSite; args=[str:"[LiTo] MySQL character disconnect persistence failed: \u0001: \u0001"]
+INVOKEVIRTUAL java/io/PrintStream.println(Ljava/lang/String;)V
+ALOAD 0
+INVOKESPECIAL local/lito381/server/ClientSession.notifyPeersOfDisconnect()V
+GOTO L315
+ASTORE 29
+GETSTATIC java/lang/System.out:Ljava/io/PrintStream;
+ALOAD 29
+INVOKEVIRTUAL java/io/IOException.getMessage()Ljava/lang/String;
+INVOKEDYNAMIC makeConcatWithConstants(Ljava/lang/String;)Ljava/lang/String; bsm=H6 java/lang/invoke/StringConcatFactory.makeConcatWithConstants(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;Ljava/lang/String;[Ljava/lang/Object;)Ljava/lang/invoke/CallSite; args=[str:"[LiTo] disconnect broadcast failed: \u0001"]
+INVOKEVIRTUAL java/io/PrintStream.println(Ljava/lang/String;)V
+ALOAD 0
+CONST str:"disconnect"
+INVOKESPECIAL local/lito381/server/ClientSession.resetNativeTournamentEconomy(Ljava/lang/String;)V
+ALOAD 0
+CONST null
+PUTFIELD local/lito381/server/ClientSession.pendingLitoRoomInvitation:Llocal/lito381/server/ClientSession$PendingLitoRoomInvitation;
+ALOAD 0
+CONST null
+PUTFIELD local/lito381/server/ClientSession.acceptedLitoRoomInvitation:Llocal/lito381/server/ClientSession$PendingLitoRoomInvitation;
+ALOAD 0
+GETSTATIC local/lito381/server/Protocol$LitoBuddyPresence.OFFLINE:Llocal/lito381/server/Protocol$LitoBuddyPresence;
+INVOKESPECIAL local/lito381/server/ClientSession.notifyBuddyPresence(Llocal/lito381/server/Protocol$LitoBuddyPresence;)V
+ALOAD 0
+GETFIELD local/lito381/server/ClientSession.selectedCharacter:Ljava/lang/String;
+INVOKEVIRTUAL java/lang/String.isBlank()Z
+IFNE L345
+ALOAD 0
+GETFIELD local/lito381/server/ClientSession.state:Llocal/lito381/server/GameStateStore;
+ALOAD 0
+INVOKESPECIAL local/lito381/server/ClientSession.stateCharacter()Ljava/lang/String;
+ALOAD 0
+GETFIELD local/lito381/server/ClientSession.sqlTimedBuffHook:Llocal/lito381/server/GameStateStore$TimedBuffPersistenceHook;
+INVOKEVIRTUAL local/lito381/server/GameStateStore.unregisterTimedBuffPersistence(Ljava/lang/String;Llocal/lito381/server/GameStateStore$TimedBuffPersistenceHook;)V
+ALOAD 0
+GETFIELD local/lito381/server/ClientSession.hub:Llocal/lito381/server/GameHub;
+ALOAD 0
+INVOKESPECIAL local/lito381/server/ClientSession.stateCharacter()Ljava/lang/String;
+ALOAD 0
+GETFIELD local/lito381/server/ClientSession.sqlBuddyHook:Llocal/lito381/server/GameHub$BuddyPersistenceHook;
+INVOKEVIRTUAL local/lito381/server/GameHub.unregisterBuddyPersistence(Ljava/lang/String;Llocal/lito381/server/GameHub$BuddyPersistenceHook;)V
+ALOAD 0
+GETFIELD local/lito381/server/ClientSession.hub:Llocal/lito381/server/GameHub;
+ALOAD 0
+INVOKEVIRTUAL local/lito381/server/GameHub.disconnect(Llocal/lito381/server/ClientSession;)Ljava/util/List;
+ALOAD 0
+INVOKESTATIC local/lito381/server/NativeRoomRules.restoreBot(Llocal/lito381/server/ClientSession;)V
+ASTORE 30
+ILOAD 8
+IFEQ L366
+ALOAD 0
+LLOAD 6/7
+CONST str:"disconnect"
+INVOKESPECIAL local/lito381/server/ClientSession.broadcastRoomOwner(JLjava/lang/String;)V
+GOTO L366
+ASTORE 31
+GETSTATIC java/lang/System.out:Ljava/io/PrintStream;
+LLOAD 6/7
+ALOAD 31
+INVOKEVIRTUAL java/io/IOException.getMessage()Ljava/lang/String;
+INVOKEDYNAMIC makeConcatWithConstants(JLjava/lang/String;)Ljava/lang/String; bsm=H6 java/lang/invoke/StringConcatFactory.makeConcatWithConstants(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;Ljava/lang/String;[Ljava/lang/Object;)Ljava/lang/invoke/CallSite; args=[str:"[LiTo] room-owner broadcast failed room=\u0001: \u0001"]
+INVOKEVIRTUAL java/io/PrintStream.println(Ljava/lang/String;)V
+ALOAD 30
+INVOKEINTERFACE java/util/List.iterator()Ljava/util/Iterator; itf
+ASTORE 32
+ALOAD 32
+INVOKEINTERFACE java/util/Iterator.hasNext()Z itf
+IFEQ L389
+ALOAD 32
+INVOKEINTERFACE java/util/Iterator.next()Ljava/lang/Object; itf
+CHECKCAST java/lang/Long
+INVOKEVIRTUAL java/lang/Long.longValue()J
+LSTORE 33/34
+ALOAD 0
+LLOAD 33/34
+INVOKESPECIAL local/lito381/server/ClientSession.broadcastRoomDeleted(J)V
+GOTO L388
+ASTORE 35
+GETSTATIC java/lang/System.out:Ljava/io/PrintStream;
+LLOAD 33/34
+ALOAD 35
+INVOKEVIRTUAL java/io/IOException.getMessage()Ljava/lang/String;
+INVOKEDYNAMIC makeConcatWithConstants(JLjava/lang/String;)Ljava/lang/String; bsm=H6 java/lang/invoke/StringConcatFactory.makeConcatWithConstants(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;Ljava/lang/String;[Ljava/lang/Object;)Ljava/lang/invoke/CallSite; args=[str:"[LiTo] room-delete broadcast failed room=\u0001: \u0001"]
+INVOKEVIRTUAL java/io/PrintStream.println(Ljava/lang/String;)V
+GOTO L369
+RETURN
@@ -219 +554 @@
-ALOAD 6
+ALOAD 5
@@ -221 +556 @@
-ASTORE 7
+ASTORE 36
@@ -225,2 +560,2 @@
-ALOAD 7
-ALOAD 6
+ALOAD 36
+ALOAD 5
@@ -230 +565 @@
-ALOAD 7
+ALOAD 36
@@ -234 +569 @@
-ALOAD 7
+ALOAD 36
@@ -238,8 +573,4 @@
-IFEQ L104
-GOTO L105
-GOTO L69
-GOTO L119
-ASTORE 8
-ALOAD 3
-IFNULL L117
-ALOAD 3
+IFEQ L726
+ALOAD 2
+IFNULL L417
+ALOAD 2
@@ -248,12 +578,0 @@
-GOTO L117
-ASTORE 9
-ALOAD 8
-ALOAD 9
-INVOKEVIRTUAL java/lang/Throwable.addSuppressed(Ljava/lang/Throwable;)V
-ALOAD 8
-ATHROW
-ALOAD 3
-IFNULL L124
-ALOAD 3
-ALOAD 0
-INVOKESTATIC local/lito381/server/NativeServerLifetime.closeSessionSocket(Ljava/net/Socket;Llocal/lito381/server/ClientSession;)V
@@ -264 +583 @@
-LSTORE 10/11
+LSTORE 37/38
@@ -268,2 +587,2 @@
-IF_ACMPNE L145
-LLOAD 10/11
+IF_ACMPNE L438
+LLOAD 37/38
@@ -272 +591 @@
-IFEQ L145
+IFEQ L438
@@ -275 +594 @@
-LLOAD 10/11
+LLOAD 37/38
@@ -278 +597 @@
-IFEQ L145
+IFEQ L438
@@ -280,3 +599,3 @@
-GOTO L146
-CONST Integer:0
-ISTORE 12
+GOTO L439
+CONST Integer:0
+ISTORE 39
@@ -287,4 +606,4 @@
-GOTO L158
-ASTORE 13
-GETSTATIC java/lang/System.out:Ljava/io/PrintStream;
-ALOAD 13
+GOTO L451
+ASTORE 40
+GETSTATIC java/lang/System.out:Ljava/io/PrintStream;
+ALOAD 40
@@ -296 +615 @@
-IFEQ L172
+IFEQ L465
@@ -301,4 +620,4 @@
-GOTO L172
-ASTORE 14
-GETSTATIC java/lang/System.out:Ljava/io/PrintStream;
-ALOAD 14
+GOTO L465
+ASTORE 41
+GETSTATIC java/lang/System.out:Ljava/io/PrintStream;
+ALOAD 41
@@ -311 +630 @@
-IF_ACMPNE L194
+IF_ACMPNE L487
@@ -314,4 +633,4 @@
-GOTO L185
-ASTORE 15
-GETSTATIC java/lang/System.out:Ljava/io/PrintStream;
-ALOAD 15
+GOTO L478
+ASTORE 42
+GETSTATIC java/lang/System.out:Ljava/io/PrintStream;
+ALOAD 42
@@ -323,4 +642,4 @@
-GOTO L194
-ASTORE 16
-GETSTATIC java/lang/System.out:Ljava/io/PrintStream;
-ALOAD 16
+GOTO L487
+ASTORE 43
+GETSTATIC java/lang/System.out:Ljava/io/PrintStream;
+ALOAD 43
@@ -332,4 +651,4 @@
-GOTO L203
-ASTORE 17
-GETSTATIC java/lang/System.out:Ljava/io/PrintStream;
-ALOAD 17
+GOTO L496
+ASTORE 44
+GETSTATIC java/lang/System.out:Ljava/io/PrintStream;
+ALOAD 44
@@ -341,4 +660,4 @@
-GOTO L212
-ASTORE 18
-GETSTATIC java/lang/System.out:Ljava/io/PrintStream;
-ALOAD 18
+GOTO L505
+ASTORE 45
+GETSTATIC java/lang/System.out:Ljava/io/PrintStream;
+ALOAD 45
@@ -350,4 +669,4 @@
-GOTO L221
-ASTORE 19
-GETSTATIC java/lang/System.out:Ljava/io/PrintStream;
-ALOAD 19
+GOTO L514
+ASTORE 46
+GETSTATIC java/lang/System.out:Ljava/io/PrintStream;
+ALOAD 46
@@ -359,4 +678,4 @@
-GOTO L230
-ASTORE 20
-GETSTATIC java/lang/System.out:Ljava/io/PrintStream;
-ALOAD 20
+GOTO L523
+ASTORE 47
+GETSTATIC java/lang/System.out:Ljava/io/PrintStream;
+ALOAD 47
@@ -368,4 +687,4 @@
-GOTO L239
-ASTORE 21
-GETSTATIC java/lang/System.out:Ljava/io/PrintStream;
-ALOAD 21
+GOTO L532
+ASTORE 48
+GETSTATIC java/lang/System.out:Ljava/io/PrintStream;
+ALOAD 48
@@ -377,4 +696,4 @@
-GOTO L248
-ASTORE 22
-GETSTATIC java/lang/System.out:Ljava/io/PrintStream;
-ALOAD 22
+GOTO L541
+ASTORE 49
+GETSTATIC java/lang/System.out:Ljava/io/PrintStream;
+ALOAD 49
@@ -386,4 +705,4 @@
-GOTO L257
-ASTORE 23
-GETSTATIC java/lang/System.out:Ljava/io/PrintStream;
-ALOAD 23
+GOTO L550
+ASTORE 50
+GETSTATIC java/lang/System.out:Ljava/io/PrintStream;
+ALOAD 50
@@ -395,4 +714,4 @@
-GOTO L266
-ASTORE 24
-GETSTATIC java/lang/System.out:Ljava/io/PrintStream;
-ALOAD 24
+GOTO L559
+ASTORE 51
+GETSTATIC java/lang/System.out:Ljava/io/PrintStream;
+ALOAD 51
@@ -404,4 +723,4 @@
-GOTO L275
-ASTORE 25
-GETSTATIC java/lang/System.out:Ljava/io/PrintStream;
-ALOAD 25
+GOTO L568
+ASTORE 52
+GETSTATIC java/lang/System.out:Ljava/io/PrintStream;
+ALOAD 52
@@ -413,4 +732,4 @@
-GOTO L284
-ASTORE 26
-GETSTATIC java/lang/System.out:Ljava/io/PrintStream;
-ALOAD 26
+GOTO L577
+ASTORE 53
+GETSTATIC java/lang/System.out:Ljava/io/PrintStream;
+ALOAD 53
@@ -422,4 +741,4 @@
-GOTO L293
-ASTORE 27
-GETSTATIC java/lang/System.out:Ljava/io/PrintStream;
-ALOAD 27
+GOTO L586
+ASTORE 54
+GETSTATIC java/lang/System.out:Ljava/io/PrintStream;
+ALOAD 54
@@ -432 +751 @@
-IF_ACMPNE L313
+IF_ACMPNE L606
@@ -442,4 +761,4 @@
-GOTO L313
-ASTORE 28
-GETSTATIC java/lang/System.out:Ljava/io/PrintStream;
-ALOAD 28
+GOTO L606
+ASTORE 55
+GETSTATIC java/lang/System.out:Ljava/io/PrintStream;
+ALOAD 55
@@ -452 +771 @@
-ASTORE 29
+ASTORE 56
@@ -458 +777 @@
-ALOAD 29
+ALOAD 56
@@ -460,3 +779,3 @@
-GOTO L330
-ASTORE 30
-ALOAD 29
+GOTO L623
+ASTORE 57
+ALOAD 56
@@ -464 +783 @@
-ALOAD 30
+ALOAD 57
@@ -466,4 +785,4 @@
-GOTO L337
-ASTORE 31
-GETSTATIC java/lang/System.out:Ljava/io/PrintStream;
-ALOAD 31
+GOTO L630
+ASTORE 58
+GETSTATIC java/lang/System.out:Ljava/io/PrintStream;
+ALOAD 58
@@ -475,4 +794,4 @@
-GOTO L349
-ASTORE 32
-GETSTATIC java/lang/System.out:Ljava/io/PrintStream;
-ALOAD 32
+GOTO L642
+ASTORE 59
+GETSTATIC java/lang/System.out:Ljava/io/PrintStream;
+ALOAD 59
@@ -481 +800 @@
-ALOAD 32
+ALOAD 59
@@ -487,4 +806,4 @@
-GOTO L358
-ASTORE 33
-GETSTATIC java/lang/System.out:Ljava/io/PrintStream;
-ALOAD 33
+GOTO L651
+ASTORE 60
+GETSTATIC java/lang/System.out:Ljava/io/PrintStream;
+ALOAD 60
@@ -509 +828 @@
-IFNE L388
+IFNE L681
@@ -528 +846,0 @@
-ASTORE 34
@@ -531,4 +849,5 @@
-ILOAD 12
-IFEQ L409
-ALOAD 0
-LLOAD 10/11
+ASTORE 61
+ILOAD 39
+IFEQ L702
+ALOAD 0
+LLOAD 37/38
@@ -537,5 +856,5 @@
-GOTO L409
-ASTORE 35
-GETSTATIC java/lang/System.out:Ljava/io/PrintStream;
-LLOAD 10/11
-ALOAD 35
+GOTO L702
+ASTORE 62
+GETSTATIC java/lang/System.out:Ljava/io/PrintStream;
+LLOAD 37/38
+ALOAD 62
@@ -545 +864 @@
-ALOAD 34
+ALOAD 61
@@ -547,2 +866,2 @@
-ASTORE 36
-ALOAD 36
+ASTORE 63
+ALOAD 63
@@ -550,2 +869,2 @@
-IFEQ L432
-ALOAD 36
+IFEQ L725
+ALOAD 63
@@ -555,3 +874,3 @@
-LSTORE 37/38
-ALOAD 0
-LLOAD 37/38
+LSTORE 64/65
+ALOAD 0
+LLOAD 64/65
@@ -559,5 +878,5 @@
-GOTO L431
-ASTORE 39
-GETSTATIC java/lang/System.out:Ljava/io/PrintStream;
-LLOAD 37/38
-ALOAD 39
+GOTO L724
+ASTORE 66
+GETSTATIC java/lang/System.out:Ljava/io/PrintStream;
+LLOAD 64/65
+ALOAD 66
@@ -567,3 +886,17 @@
-GOTO L412
-GOTO L1066
-ASTORE 40
+GOTO L705
+RETURN
+GOTO L65
+ASTORE 67
+ALOAD 2
+IFNULL L738
+ALOAD 2
+ALOAD 0
+INVOKESTATIC local/lito381/server/NativeServerLifetime.closeSessionSocket(Ljava/net/Socket;Llocal/lito381/server/ClientSession;)V
+GOTO L738
+ASTORE 68
+ALOAD 67
+ALOAD 68
+INVOKEVIRTUAL java/lang/Throwable.addSuppressed(Ljava/lang/Throwable;)V
+ALOAD 67
+ATHROW
+ASTORE 69
@@ -574,2 +907,2 @@
-ALOAD 2
-ALOAD 40
+ALOAD 1
+ALOAD 69
@@ -578 +911 @@
-ALOAD 40
+ALOAD 69
@@ -586 +919 @@
-LSTORE 41/42
+LSTORE 70/71
@@ -590,2 +923,2 @@
-IF_ACMPNE L467
-LLOAD 41/42
+IF_ACMPNE L774
+LLOAD 70/71
@@ -594 +927 @@
-IFEQ L467
+IFEQ L774
@@ -597 +930 @@
-LLOAD 41/42
+LLOAD 70/71
@@ -600 +933 @@
-IFEQ L467
+IFEQ L774
@@ -602,3 +935,3 @@
-GOTO L468
-CONST Integer:0
-ISTORE 43
+GOTO L775
+CONST Integer:0
+ISTORE 72
@@ -609,4 +942,4 @@
-GOTO L480
-ASTORE 44
-GETSTATIC java/lang/System.out:Ljava/io/PrintStream;
-ALOAD 44
+GOTO L787
+ASTORE 73
+GETSTATIC java/lang/System.out:Ljava/io/PrintStream;
+ALOAD 73
@@ -618 +951 @@
-IFEQ L494
+IFEQ L801
@@ -623,4 +956,4 @@
-GOTO L494
-ASTORE 45
-GETSTATIC java/lang/System.out:Ljava/io/PrintStream;
-ALOAD 45
+GOTO L801
+ASTORE 74
+GETSTATIC java/lang/System.out:Ljava/io/PrintStream;
+ALOAD 74
@@ -633 +966 @@
-IF_ACMPNE L516
+IF_ACMPNE L823
@@ -636,4 +969,4 @@
-GOTO L507
-ASTORE 46
-GETSTATIC java/lang/System.out:Ljava/io/PrintStream;
-ALOAD 46
+GOTO L814
+ASTORE 75
+GETSTATIC java/lang/System.out:Ljava/io/PrintStream;
+ALOAD 75
@@ -645,4 +978,4 @@
-GOTO L516
-ASTORE 47
-GETSTATIC java/lang/System.out:Ljava/io/PrintStream;
-ALOAD 47
+GOTO L823
+ASTORE 76
+GETSTATIC java/lang/System.out:Ljava/io/PrintStream;
+ALOAD 76
@@ -654,4 +987,4 @@
-GOTO L525
-ASTORE 48
-GETSTATIC java/lang/System.out:Ljava/io/PrintStream;
-ALOAD 48
+GOTO L832
+ASTORE 77
+GETSTATIC java/lang/System.out:Ljava/io/PrintStream;
+ALOAD 77
@@ -663,4 +996,4 @@
-GOTO L534
-ASTORE 49
-GETSTATIC java/lang/System.out:Ljava/io/PrintStream;
-ALOAD 49
+GOTO L841
+ASTORE 78
+GETSTATIC java/lang/System.out:Ljava/io/PrintStream;
+ALOAD 78
@@ -672,4 +1005,4 @@
-GOTO L543
-ASTORE 50
-GETSTATIC java/lang/System.out:Ljava/io/PrintStream;
-ALOAD 50
+GOTO L850
+ASTORE 79
+GETSTATIC java/lang/System.out:Ljava/io/PrintStream;
+ALOAD 79
@@ -681,4 +1014,4 @@
-GOTO L552
-ASTORE 51
-GETSTATIC java/lang/System.out:Ljava/io/PrintStream;
-ALOAD 51
+GOTO L859
+ASTORE 80
+GETSTATIC java/lang/System.out:Ljava/io/PrintStream;
+ALOAD 80
@@ -690,4 +1023,4 @@
-GOTO L561
-ASTORE 52
-GETSTATIC java/lang/System.out:Ljava/io/PrintStream;
-ALOAD 52
+GOTO L868
+ASTORE 81
+GETSTATIC java/lang/System.out:Ljava/io/PrintStream;
+ALOAD 81
@@ -699,4 +1032,4 @@
-GOTO L570
-ASTORE 53
-GETSTATIC java/lang/System.out:Ljava/io/PrintStream;
-ALOAD 53
+GOTO L877
+ASTORE 82
+GETSTATIC java/lang/System.out:Ljava/io/PrintStream;
+ALOAD 82
@@ -708,4 +1041,4 @@
-GOTO L579
-ASTORE 54
-GETSTATIC java/lang/System.out:Ljava/io/PrintStream;
-ALOAD 54
+GOTO L886
+ASTORE 83
+GETSTATIC java/lang/System.out:Ljava/io/PrintStream;
+ALOAD 83
@@ -717,4 +1050,4 @@
-GOTO L588
-ASTORE 55
-GETSTATIC java/lang/System.out:Ljava/io/PrintStream;
-ALOAD 55
+GOTO L895
+ASTORE 84
+GETSTATIC java/lang/System.out:Ljava/io/PrintStream;
+ALOAD 84
@@ -726,4 +1059,4 @@
-GOTO L597
-ASTORE 56
-GETSTATIC java/lang/System.out:Ljava/io/PrintStream;
-ALOAD 56
+GOTO L904
+ASTORE 85
+GETSTATIC java/lang/System.out:Ljava/io/PrintStream;
+ALOAD 85
@@ -735,4 +1068,4 @@
-GOTO L606
-ASTORE 57
-GETSTATIC java/lang/System.out:Ljava/io/PrintStream;
-ALOAD 57
+GOTO L913
+ASTORE 86
+GETSTATIC java/lang/System.out:Ljava/io/PrintStream;
+ALOAD 86
@@ -744,4 +1077,4 @@
-GOTO L615
-ASTORE 58
-GETSTATIC java/lang/System.out:Ljava/io/PrintStream;
-ALOAD 58
+GOTO L922
+ASTORE 87
+GETSTATIC java/lang/System.out:Ljava/io/PrintStream;
+ALOAD 87
@@ -754 +1087 @@
-IF_ACMPNE L635
+IF_ACMPNE L942
@@ -764,4 +1097,4 @@
-GOTO L635
-ASTORE 59
-GETSTATIC java/lang/System.out:Ljava/io/PrintStream;
-ALOAD 59
+GOTO L942
+ASTORE 88
+GETSTATIC java/lang/System.out:Ljava/io/PrintStream;
+ALOAD 88
@@ -774 +1107 @@
-ASTORE 60
+ASTORE 89
@@ -780 +1113 @@
-ALOAD 60
+ALOAD 89
@@ -782,3 +1115,3 @@
-GOTO L652
-ASTORE 61
-ALOAD 60
+GOTO L959
+ASTORE 90
+ALOAD 89
@@ -786 +1119 @@
-ALOAD 61
+ALOAD 90
@@ -788,4 +1121,4 @@
-GOTO L659
-ASTORE 62
-GETSTATIC java/lang/System.out:Ljava/io/PrintStream;
-ALOAD 62
+GOTO L966
+ASTORE 91
+GETSTATIC java/lang/System.out:Ljava/io/PrintStream;
+ALOAD 91
@@ -797,4 +1130,4 @@
-GOTO L671
-ASTORE 63
-GETSTATIC java/lang/System.out:Ljava/io/PrintStream;
-ALOAD 63
+GOTO L978
+ASTORE 92
+GETSTATIC java/lang/System.out:Ljava/io/PrintStream;
+ALOAD 92
@@ -803 +1136 @@
-ALOAD 63
+ALOAD 92
@@ -809,4 +1142,4 @@
-GOTO L680
-ASTORE 64
-GETSTATIC java/lang/System.out:Ljava/io/PrintStream;
-ALOAD 64
+GOTO L987
+ASTORE 93
+GETSTATIC java/lang/System.out:Ljava/io/PrintStream;
+ALOAD 93
@@ -831 +1164 @@
-IFNE L710
+IFNE L1017
@@ -850 +1182,0 @@
-ASTORE 65
@@ -853,4 +1185,5 @@
-ILOAD 43
-IFEQ L731
-ALOAD 0
-LLOAD 41/42
+ASTORE 94
+ILOAD 72
+IFEQ L1038
+ALOAD 0
+LLOAD 70/71
@@ -859,5 +1192,5 @@
-GOTO L731
-ASTORE 66
-GETSTATIC java/lang/System.out:Ljava/io/PrintStream;
-LLOAD 41/42
-ALOAD 66
+GOTO L1038
+ASTORE 95
+GETSTATIC java/lang/System.out:Ljava/io/PrintStream;
+LLOAD 70/71
+ALOAD 95
@@ -867 +1200 @@
-ALOAD 65
+ALOAD 94
@@ -869,2 +1202,2 @@
-ASTORE 67
-ALOAD 67
+ASTORE 96
+ALOAD 96
@@ -872,2 +1205,2 @@
-IFEQ L754
-ALOAD 67
+IFEQ L1061
+ALOAD 96
@@ -877,3 +1210,3 @@
-LSTORE 68/69
-ALOAD 0
-LLOAD 68/69
+LSTORE 97/98
+ALOAD 0
+LLOAD 97/98
@@ -881,5 +1214,5 @@
-GOTO L753
-ASTORE 70
-GETSTATIC java/lang/System.out:Ljava/io/PrintStream;
-LLOAD 68/69
-ALOAD 70
+GOTO L1060
+ASTORE 99
+GETSTATIC java/lang/System.out:Ljava/io/PrintStream;
+LLOAD 97/98
+ALOAD 99
@@ -889,3 +1222,3 @@
-GOTO L734
-GOTO L1066
-ASTORE 71
+GOTO L1041
+GOTO L1373
+ASTORE 100
@@ -896 +1229 @@
-LSTORE 72/73
+LSTORE 101/102
@@ -900,2 +1233,2 @@
-IF_ACMPNE L777
-LLOAD 72/73
+IF_ACMPNE L1084
+LLOAD 101/102
@@ -904 +1237 @@
-IFEQ L777
+IFEQ L1084
@@ -907 +1240 @@
-LLOAD 72/73
+LLOAD 101/102
@@ -910 +1243 @@
-IFEQ L777
+IFEQ L1084
@@ -912,3 +1245,3 @@
-GOTO L778
-CONST Integer:0
-ISTORE 74
+GOTO L1085
+CONST Integer:0
+ISTORE 103
@@ -919,4 +1252,4 @@
-GOTO L790
-ASTORE 75
-GETSTATIC java/lang/System.out:Ljava/io/PrintStream;
-ALOAD 75
+GOTO L1097
+ASTORE 104
+GETSTATIC java/lang/System.out:Ljava/io/PrintStream;
+ALOAD 104
@@ -928 +1261 @@
-IFEQ L804
+IFEQ L1111
@@ -933,4 +1266,4 @@
-GOTO L804
-ASTORE 76
-GETSTATIC java/lang/System.out:Ljava/io/PrintStream;
-ALOAD 76
+GOTO L1111
+ASTORE 105
+GETSTATIC java/lang/System.out:Ljava/io/PrintStream;
+ALOAD 105
@@ -943 +1276 @@
-IF_ACMPNE L826
+IF_ACMPNE L1133
@@ -946,4 +1279,4 @@
-GOTO L817
-ASTORE 77
-GETSTATIC java/lang/System.out:Ljava/io/PrintStream;
-ALOAD 77
+GOTO L1124
+ASTORE 106
+GETSTATIC java/lang/System.out:Ljava/io/PrintStream;
+ALOAD 106
@@ -955,4 +1288,4 @@
-GOTO L826
-ASTORE 78
-GETSTATIC java/lang/System.out:Ljava/io/PrintStream;
-ALOAD 78
+GOTO L1133
+ASTORE 107
+GETSTATIC java/lang/System.out:Ljava/io/PrintStream;
+ALOAD 107
@@ -964,4 +1297,4 @@
-GOTO L835
-ASTORE 79
-GETSTATIC java/lang/System.out:Ljava/io/PrintStream;
-ALOAD 79
+GOTO L1142
+ASTORE 108
+GETSTATIC java/lang/System.out:Ljava/io/PrintStream;
+ALOAD 108
@@ -973,4 +1306,4 @@
-GOTO L844
-ASTORE 80
-GETSTATIC java/lang/System.out:Ljava/io/PrintStream;
-ALOAD 80
+GOTO L1151
+ASTORE 109
+GETSTATIC java/lang/System.out:Ljava/io/PrintStream;
+ALOAD 109
@@ -982,4 +1315,4 @@
-GOTO L853
-ASTORE 81
-GETSTATIC java/lang/System.out:Ljava/io/PrintStream;
-ALOAD 81
+GOTO L1160
+ASTORE 110
+GETSTATIC java/lang/System.out:Ljava/io/PrintStream;
+ALOAD 110
@@ -991,4 +1324,4 @@
-GOTO L862
-ASTORE 82
-GETSTATIC java/lang/System.out:Ljava/io/PrintStream;
-ALOAD 82
+GOTO L1169
+ASTORE 111
+GETSTATIC java/lang/System.out:Ljava/io/PrintStream;
+ALOAD 111
@@ -1000,4 +1333,4 @@
-GOTO L871
-ASTORE 83
-GETSTATIC java/lang/System.out:Ljava/io/PrintStream;
-ALOAD 83
+GOTO L1178
+ASTORE 112
+GETSTATIC java/lang/System.out:Ljava/io/PrintStream;
+ALOAD 112
@@ -1009,4 +1342,4 @@
-GOTO L880
-ASTORE 84
-GETSTATIC java/lang/System.out:Ljava/io/PrintStream;
-ALOAD 84
+GOTO L1187
+ASTORE 113
+GETSTATIC java/lang/System.out:Ljava/io/PrintStream;
+ALOAD 113
@@ -1018,4 +1351,4 @@
-GOTO L889
-ASTORE 85
-GETSTATIC java/lang/System.out:Ljava/io/PrintStream;
-ALOAD 85
+GOTO L1196
+ASTORE 114
+GETSTATIC java/lang/System.out:Ljava/io/PrintStream;
+ALOAD 114
@@ -1027,4 +1360,4 @@
-GOTO L898
-ASTORE 86
-GETSTATIC java/lang/System.out:Ljava/io/PrintStream;
-ALOAD 86
+GOTO L1205
+ASTORE 115
+GETSTATIC java/lang/System.out:Ljava/io/PrintStream;
+ALOAD 115
@@ -1036,4 +1369,4 @@
-GOTO L907
-ASTORE 87
-GETSTATIC java/lang/System.out:Ljava/io/PrintStream;
-ALOAD 87
+GOTO L1214
+ASTORE 116
+GETSTATIC java/lang/System.out:Ljava/io/PrintStream;
+ALOAD 116
@@ -1045,4 +1378,4 @@
-GOTO L916
-ASTORE 88
-GETSTATIC java/lang/System.out:Ljava/io/PrintStream;
-ALOAD 88
+GOTO L1223
+ASTORE 117
+GETSTATIC java/lang/System.out:Ljava/io/PrintStream;
+ALOAD 117
@@ -1054,4 +1387,4 @@
-GOTO L925
-ASTORE 89
-GETSTATIC java/lang/System.out:Ljava/io/PrintStream;
-ALOAD 89
+GOTO L1232
+ASTORE 118
+GETSTATIC java/lang/System.out:Ljava/io/PrintStream;
+ALOAD 118
@@ -1064 +1397 @@
-IF_ACMPNE L945
+IF_ACMPNE L1252
@@ -1074,4 +1407,4 @@
-GOTO L945
-ASTORE 90
-GETSTATIC java/lang/System.out:Ljava/io/PrintStream;
-ALOAD 90
+GOTO L1252
+ASTORE 119
+GETSTATIC java/lang/System.out:Ljava/io/PrintStream;
+ALOAD 119
@@ -1084 +1417 @@
-ASTORE 91
+ASTORE 120
@@ -1090 +1423 @@
-ALOAD 91
+ALOAD 120
@@ -1092,3 +1425,3 @@
-GOTO L962
-ASTORE 92
-ALOAD 91
+GOTO L1269
+ASTORE 121
+ALOAD 120
@@ -1096 +1429 @@
-ALOAD 92
+ALOAD 121
@@ -1098,4 +1431,4 @@
-GOTO L969
-ASTORE 93
-GETSTATIC java/lang/System.out:Ljava/io/PrintStream;
-ALOAD 93
+GOTO L1276
+ASTORE 122
+GETSTATIC java/lang/System.out:Ljava/io/PrintStream;
+ALOAD 122
@@ -1107,4 +1440,4 @@
-GOTO L981
-ASTORE 94
-GETSTATIC java/lang/System.out:Ljava/io/PrintStream;
-ALOAD 94
+GOTO L1288
+ASTORE 123
+GETSTATIC java/lang/System.out:Ljava/io/PrintStream;
+ALOAD 123
@@ -1113 +1446 @@
-ALOAD 94
+ALOAD 123
@@ -1119,4 +1452,4 @@
-GOTO L990
-ASTORE 95
-GETSTATIC java/lang/System.out:Ljava/io/PrintStream;
-ALOAD 95
+GOTO L1297
+ASTORE 124
+GETSTATIC java/lang/System.out:Ljava/io/PrintStream;
+ALOAD 124
@@ -1141 +1474 @@
-IFNE L1020
+IFNE L1327
@@ -1160 +1492,0 @@
-ASTORE 96
@@ -1163,4 +1495,5 @@
-ILOAD 74
-IFEQ L1041
-ALOAD 0
-LLOAD 72/73
+ASTORE 125
+ILOAD 103
+IFEQ L1348
+ALOAD 0
+LLOAD 101/102
@@ -1169,5 +1502,5 @@
-GOTO L1041
-ASTORE 97
-GETSTATIC java/lang/System.out:Ljava/io/PrintStream;
-LLOAD 72/73
-ALOAD 97
+GOTO L1348
+ASTORE 126
+GETSTATIC java/lang/System.out:Ljava/io/PrintStream;
+LLOAD 101/102
+ALOAD 126
@@ -1177 +1510 @@
-ALOAD 96
+ALOAD 125
@@ -1179,2 +1512,2 @@
-ASTORE 98
-ALOAD 98
+ASTORE 127
+ALOAD 127
@@ -1182,2 +1515,2 @@
-IFEQ L1064
-ALOAD 98
+IFEQ L1371
+ALOAD 127
@@ -1187,3 +1520,3 @@
-LSTORE 99/100
-ALOAD 0
-LLOAD 99/100
+LSTORE 128/129
+ALOAD 0
+LLOAD 128/129
@@ -1191,5 +1524,5 @@
-GOTO L1063
-ASTORE 101
-GETSTATIC java/lang/System.out:Ljava/io/PrintStream;
-LLOAD 99/100
-ALOAD 101
+GOTO L1370
+ASTORE 130
+GETSTATIC java/lang/System.out:Ljava/io/PrintStream;
+LLOAD 128/129
+ALOAD 130
@@ -1199,2 +1532,2 @@
-GOTO L1044
-ALOAD 71
+GOTO L1351
+ALOAD 100
```

</details>
