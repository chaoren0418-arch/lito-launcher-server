# Issue #70 / T020-B09 — #216 後 Java 修正與完整重驗

## 結論

**尚未通過 B09 驗收、不可 B-land；剩餘差異交 `needs:claude` 裁定。** 已在指定 worktree／`task/70`，由 `31ddb09` 合併 `origin/main@43a4f7a`（含 #203／#216；合併提交 `d8c4d97`），沒有切換或建立分支。#216 已到齊，不再列為等待合併的阻塞。

最終完整 **362 entries：357 SAME、2 SAME_PATHS、3 DIFF**；類別 missing／extra／ONLY_LEFT／ONLY_RIGHT 全 0。方法診斷仍有 **346 DIFF、9 ONLY_LEFT、63 ONLY_RIGHT**，72 個配對缺口全含 `lambda$`，不可視為通過。T029 八方法為 **5 SAME、1 SAME_EFFECTS、1 SAME_PATHS、1 DIFF**。CLI exit 0 只表示報告成功產生。

## 做了什麼

- `ClientSession` 的 T029 inventory：檢查正式版完整 javap 指令與完整 effects 後，修正 MAP、light、BRAVE、WISDOM、ELVEN_BRAVE、BLUE_MPR、BLIND、BREATH、WHISTLE、LIQUOR 十處條件，從 `NativeWorldPotionPhase.allowed(this)` 恢復正式版 `phase == BATTLE`。HEAL 是正式版唯一保留該 helper 的分支，維持原樣。
- `nativeLightCommit`：改用具型別的 LightSpell 暫存，消除八次多餘 CHECKCAST；保留 Phase、Long 轉型。將 checked exception 的原樣轉拋縮到 `commitSpellReady` 呼叫，沿用已核准的 `rethrowUnchecked`，移除多出的 IOException 宣告；**完整方法 SAME_PATHS，不用 Exceptions 豁免**。
- `GameHub#battleDisconnectWork`：刪房通知接收者改成候選的 `new ArrayList<>(channel.members)`，不再重新呼叫 `channelMembers(room.channelId)`。其餘堆疊／local 差異仍保留 DIFF，沒有冒稱整個方法通過。
- 合併衝突僅在 metadata 清單：移除舊 pending，採用 #216 已核准的四筆 `enclosing-method`。僅豁免 outerMethod／outerMethodDesc；outerClass、其他 metadata、欄位、方法照比。最終清單內容與 main 相同。
- 修掉 Protocol.java 原第 17405 行的檔尾多餘空白行。保留前輪 31 個方法移植、登入 SourceLogin.allowed 守門、核准 helper、5 個 AsRecord 與 AsmFinal。
- 本輪只手改三份本批 staged Java、核准條目及本報告；沒有改工具、src/main、pom.xml 或 docs/REPORT.md。main 合併帶入的上游改動另計。

## 已驗證／怎麼驗證

Microsoft OpenJDK 17.0.17、ASM 9.7。每次使用全新輸出目錄，由來源編譯 semantic-diff、AsRecord、註解與三份 staged Java；候選在 classpath 前、正式版在後。官方 AsRecord `scanned=362 converted=6`（五個 record 加 AsmFinal）。實際 class 集合與 batches.json B09 的 362 個 verify_entries 精確相等。未覆寫候選 class bytes、未以二進位修補代替 Java。

使用完整 `--allow-methods docs/combat-merge/t029-methods.txt --allow-right reference/base-0.6.71.jar`；effects 僅兩個既有核准方法、extra-method 僅 rethrowUnchecked，metadata 僅四筆 enclosing-method。沒有整類 metadata、Exceptions、layout、method-diff 或 deviation 豁免。

| 階段 | 類別結果 | 方法 DIFF | 方法 ONLY_LEFT／ONLY_RIGHT |
|---|---|---:|---|
| 前輪 #203 | 353 SAME、2 SAME_PATHS、7 DIFF | 356 | 9／63 |
| 本輪 #216 基線，未改 Java | 357 SAME、2 SAME_PATHS、3 DIFF | 347 | 9／63 |
| 本輪 Java 修正後 | 357 SAME、2 SAME_PATHS、3 DIFF | 346 | 9／63 |

方法結果列只包含後備證明與失敗診斷，不是全部方法數。最終診斷分布：

| 狀態 | 數量 |
|---|---:|
| DIFF | 346 |
| ONLY_LEFT | 9 |
| ONLY_RIGHT | 63 |
| ORPHAN_IGNORED | 8 |
| SAME_CFG | 51 |
| SAME_PATHS | 72 |

四個 healing record 全 SAME；Protocol 的 162 entries 全 SAME。三個未過 entry 是 ClientSession、ClientSession$1、GameHub。51 個 SAME_CFG 方法仍位於有其他差異的類別中，因此類別摘要 SAME_CFG=0 不表示沒有方法通過。

### T029 八方法

| 方法 | 對正式版結果 |
|---|---|
| `local/lito381/server/ClientSession#exitRoom(Z)V` | SAME |
| `local/lito381/server/ClientSession#exitRoomBeforePolyMe(Z)V` | SAME_EFFECTS |
| `local/lito381/server/ClientSession#run()V` | SAME_PATHS |
| `local/lito381/server/ClientSession#sendRoomParticipant(JLlocal/lito381/server/ClientSession;Llocal/lito381/server/Protocol$LitoForce;Ljava/lang/String;)V` | SAME |
| `local/lito381/server/ClientSession#sourcePoisonBody$useInventoryItemBeforeTravel([BLlocal/lito381/server/CombatPoisonLifecycle$Scope;)V` | DIFF |
| `local/lito381/server/Protocol#litoRoomParticipant(JLjava/lang/String;Llocal/lito381/server/Protocol$LitoForce;IILlocal/lito381/server/GameStateStore$TournamentStatistics;Ljava/lang/String;IILlocal/lito381/server/Protocol$LitoParticipantState;ILlocal/lito381/server/GameStateStore$TournamentAwards;)[B` | SAME |
| `local/lito381/server/Protocol#magicDollCustomIcon(IIZ)[B` | SAME |
| `local/lito381/server/Protocol#magicDollIcon(I)[B` | SAME |

### T029 inventory：剩餘最小完整 effects 差異

正式版完整方法的 javap 已檢查。handoff 區段的求值順序是 `hub → accountName() → stateCharacter() → rentalTownInventory → SourceLogin.account() → stageRentalBattleHandoff()`；staged 已維持同一呼叫／欄位讀取順序，差別是 javac 在每次求值後立即 ASTORE，正式 ASM patch 先把四個值留在 stack，再反序存到 slots 34、33、32、35。不能直接改成 `SourceLogin.account(this, accountName())` 作第一個引數：那會將 SourceLogin.account 提前到 stateCharacter／inventory 之前，改變求值及例外順序。

本輪沒有為消除 ASTORE 診斷而加入未核准 helper、移動 SourceLogin.account 或直接 patch class。十處階段條件修正後，**完整 effects sequence 的所有差異只剩下面這一個 hunk**；不再含 NativeWorldPotionPhase 差異。正式有四次連續 ASTORE，既有 tempStores 正規化略去其中一次，因此 effects 右側為三次。此證據不是 SAME_EFFECTS，方法仍為 DIFF：`effects sequence[43]`；paths 因 normal control-flow cycle 拒絕，CFG 回報 **unsupported shuffle/subroutine**。依本輪明示退回規則交主控判定是否需擴充保守堆疊證明；不自行新增核准。

```diff
--- staged-effects
+++ formal-effects
@@ -40,14 +40,13 @@
 INVOKEDYNAMIC makeConcatWithConstants(J)Ljava/lang/String; bsm=H6 java/lang/invoke/StringConcatFactory.makeConcatWithConstants(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;Ljava/lang/String;[Ljava/lang/Object;)Ljava/lang/invoke/CallSite; args=[str:"[LiTo] C_USE_ITEM rental-town transfer-stone deferred object=\u0001 result=exactly-one-configured-battle-endpoint-required"]
 INVOKEVIRTUAL java/io/PrintStream.println(Ljava/lang/String;)V
 RETURN
 GETFIELD local/lito381/server/ClientSession.hub
-ASTORE
 INVOKEVIRTUAL local/lito381/server/ClientSession.accountName()Ljava/lang/String;
-ASTORE
 INVOKESPECIAL local/lito381/server/ClientSession.stateCharacter()Ljava/lang/String;
-ASTORE
 GETFIELD local/lito381/server/ClientSession.rentalTownInventory
+ASTORE
+ASTORE
 ASTORE
 INVOKESTATIC local/lito381/server/SourceLogin.account(Llocal/lito381/server/ClientSession;Ljava/lang/String;)Ljava/lang/String;
 INVOKEVIRTUAL local/lito381/server/GameHub.stageRentalBattleHandoff(Ljava/lang/String;Ljava/lang/String;Llocal/lito381/server/RuntimeInventory;)Z
 GETSTATIC java/lang/System.out
```

正式 handoff 區段完整指令（188–224；後續是 boolean 結果存取與既有拒絕分支）：

```text
     188: aload_0
     189: getfield      #3441               // Field hub:Llocal/lito381/server/GameHub;
     192: aload_0
     193: invokevirtual #4839               // Method accountName:()Ljava/lang/String;
     196: aload_0
     197: invokevirtual #4562               // Method stateCharacter:()Ljava/lang/String;
     200: aload_0
     201: getfield      #13611              // Field rentalTownInventory:Llocal/lito381/server/RuntimeInventory;
     204: astore        34
     206: astore        33
     208: astore        32
     210: astore        35
     212: aload         35
     214: aload_0
     215: aload         32
     217: invokestatic  #10597              // Method local/lito381/server/SourceLogin.account:(Llocal/lito381/server/ClientSession;Ljava/lang/String;)Ljava/lang/String;
     220: aload         33
     222: aload         34
     224: invokevirtual #13779              // Method local/lito381/server/GameHub.stageRentalBattleHandoff:(Ljava/lang/String;Ljava/lang/String;Llocal/lito381/server/RuntimeInventory;)Z
```

### light：CHECKCAST 與逐路徑事件

候選 javap 的 Phase cast 在 offset 4、Long cast 在 offset 36，沒有 LightSpell cast。修正前 staged 多八次 LightSpell cast（skillId 四處、mpCost 兩處、durationSeconds 一處、reuseDelayMillis 一處），會增加比較模型的可拋出路徑。原來 110／90 是**路徑多重集合的筆數**，不是「多出 20 個指令事件」。

修正後官方比較器先套用核准純轉拋／helper 正規化，完整 PathComparison 為 **90 對 90、每條路徑的有序事件、引數 expression 與 multiplicity 全相等**，不是只比較數目。另外將兩邊完整排序輸出逐字核對，相同；CHECKCAST 皆恰好為 Phase、Long。`nativeLightCommit` 官方 CLI 與獨立逐方法診斷都 SAME_PATHS，沒有啟用 Exceptions 豁免。

### 登入、disconnect、switch-map 與 lambda

- `sourceApplicationRun` 保留守門與窄 try，#216 後仍是整個方法 handler／控制流程 DIFF，paths 有迴圈、CFG 為 labelled CFGs differ；完整代表差異見附錄。
- `battleDisconnectWork` 修正 channel 成員來源後，第一個差異仍為 ASTORE／ALOAD 對 DUP；另有 botHp receiver 的 ALOAD／DUP，完整差異見附錄。沒有據此認定為等價。
- switch-map 四個使用方法仍為 DIFF：acceptsLitoRosterBroadcast、serverClassicWorldDspyFollowerTick、attack、sourceNpcPoisonState；ClientSession$1 的 `<clinit>` 也保留 DIFF。附 accepts 方法與整個 holder 初始化的完整差異。
- 72 個 lambda 配對缺口未解決；附兩個方向的完整本體代表，以及 invokedynamic 呼叫點代表。不將它們與已核准 ORPHAN_IGNORED 混計。

以下以第一個診斷互斥分型，**不是根因裁定**；涵蓋所有 346 DIFF 與 72 配對缺口：

| 類型 | 數量 |
|---|---:|
| lambda 配對缺口 | 72 |
| switch-map 欄位對應 | 4 |
| 指令資料流與控制流程 | 159 |
| 例外涵蓋與 handler | 171 |
| lambda／invokedynamic 內容 | 8 |
| 其他 Exceptions 宣告 | 2 |
| 登入守門與整體 handler | 1 |
| T029 effects 暫存 | 1 |

### 固定輸入與產物

| 檔案 | SHA-256 |
|---|---|
| `lito伺服器/LiTo381-Server/reference/base-0.6.71.jar` | `e1498871c7be6b287821c57ef5d95933db1740d9b4d4a80f6c3465f21d99d498` |
| `lito伺服器/LiTo381-Server/reference/candidate-50635f7e.jar` | `50635f7edca8ec6ce77787cc32371c7590b497e48ea202f8a679caf3f517b729` |
| `scratch/issue-70/target/verified216.jar` | `eeb1b8d0bba82e6852bc6020eb17f119de44ae41d27147040cd4deae7e0d75f3` |

### 完整 PR 格式檢查

修掉 Protocol 檔尾後，對 **完整 PR 累積差異**執行 `git diff --check origin/main`（包含工作樹修改），提交後再執行 `git diff --check origin/main...HEAD`；兩者皆須 exit 0。範圍包含前輪累積的三份 staged Java、extra-methods-approved 與本報告，並非只檢查本輪未提交差異。metadata-exempt 最終與 main 相同，不產生 PR 差異。實際收據為 `scratch/issue-70/full-pr-diff-check.txt`。

## 推測

T029 handoff 與 disconnect 的暫存／堆疊差異可能屬工具尚未證明的等價形狀；本輪不把它們當成已證明。其餘 DIFF 可能仍含 Java 移植問題，不能全部歸因於比較器。light 的證明只適用固定 JAR 與此比較模型，不等於實機驗收。

## 待驗證／未驗證什麼／給下一棒的提示

1. **Claude 主控**：依 [本輪沿用裁定](https://github.com/chaoren0418-arch/lito-launcher-server/issues/70#issuecomment-6665159356)，審查以下完整代表差異；優先處理 T029 effects 暫存與 unsupported shuffle/subroutine、disconnect 堆疊差異、switch-map holder、lambda 配對及登入 handler。需要工具擴充或新核准時由主控裁定，本輪不改工具或擴大清單。
2. **本機 B09 執行器**：保留已修正的十處 T029 條件、light SAME_PATHS、channel 成員來源與四個 metadata 通過結果；後續從最新 main 重建，不復用舊 class。須等全部 362 entries、T029 八方法按各自核准模式通過，且所有方法配對缺口清零後才可 B-land。
3. **B-land**：依指定範圍，本批未跑單元／回歸／實機；集中於 B-land 處理。未啟動伺服器、登入器或遊戲，未連 DB，未讀機密設定，未動封存、releases 或硬連結。

## 產出路徑與重現

- 交付：三份 `lito伺服器/LiTo381-Server/merge-inputs/50635f7e/staged/local/lito381/server/{ClientSession,GameHub,Protocol}.java`、本報告；核准四條已由 main 帶入。PR 累積尚含前輪 extra-methods-approved。
- 本機證據：`scratch/issue-70/verify-216.py`、`audit-216.py`、`Focused216.java`、`LatestAudit.java`、`report216.py`、`verified216-{diff.json,diff.md,audit.txt,tool.log,annotations.log,javac.log,record.log,semantic.log}`、`focus216-verified.txt`、`inventory-verified-effects.diff`、完整 base／candidate javap、`latest-command.json`；建置在 `scratch/issue-70/target/`，均不進 git。
- 附錄直接保存每類完整差異，審查不依賴本機 scratch。下列指令從 repo 根目錄重建；請同時檢查 JSON 方法診斷，不能只看 CLI exit 0。

```powershell
$ErrorActionPreference = 'Stop'
$root = (Get-Location).Path
$s = Join-Path $root 'lito伺服器/LiTo381-Server'
$w = Join-Path $root 'scratch/issue-70/reproduce-216'
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
$meta = Get-Content -Encoding UTF8 "$root/docs/combat-merge/metadata-exempt.txt" | Where-Object { $_.StartsWith('local/lito381/server/ClientSession$') }
[IO.File]::WriteAllLines("$w/metadata-exempt.txt", [string[]]$meta, $utf8)
& "$jdk/java.exe" -Xmx3g -cp "$w/target/tool;$asm" local.lito381.semanticdiff.SemanticDiff --left "$w/target/left.jar" --right $candidate --left-classpath $candidate --left-classpath $formal --classes "$w/entries.txt" --allow-methods "$root/docs/combat-merge/t029-methods.txt" --allow-right $formal --effects-methods "$w/effects-approved.txt" --extra-methods "$w/extra-methods-approved.txt" --metadata-exempt "$w/metadata-exempt.txt" --json "$w/result.json" --md "$w/result.md"
if ($LASTEXITCODE) { throw 'semantic CLI failed' }
(Get-Content -Raw -Encoding UTF8 "$w/result.json" | ConvertFrom-Json).counts
```

## 附錄：各類完整代表差異

由本輪 JarIndex／ClassComparer 產生的正規化指令。左為 staged、右為 candidate；T029 inventory 右為正式版。保留全部 hunks、槽位、標籤、bootstrap、META 與 TRY COVERAGE；未裁切或只挑首項。沒有把此文字診斷當作驗收通過。

### `local/lito381/server/ClientSession#acceptsLitoRosterBroadcast()Z`

<details>
<summary>完整差異（全部 hunks）</summary>

```diff
--- staged/local/lito381/server/ClientSession#acceptsLitoRosterBroadcast()Z
+++ candidate/local/lito381/server/ClientSession#acceptsLitoRosterBroadcast()Z
@@ -9,7 +9,7 @@
 IFEQ L9
 CONST Integer:0
 IRETURN
-GETSTATIC local/lito381/server/ClientSession$1.$SwitchMap$#19:[I
+GETSTATIC local/lito381/server/ClientSession$1.$SwitchMap$#20:[I
 ALOAD 0
 GETFIELD local/lito381/server/ClientSession.phase:Llocal/lito381/server/ClientSession$Phase;
 INVOKEVIRTUAL local/lito381/server/ClientSession$Phase.ordinal()I
```

</details>

### `local/lito381/server/ClientSession#login([B)V`

<details>
<summary>完整差異（全部 hunks）</summary>

```diff
--- staged/local/lito381/server/ClientSession#login([B)V
+++ candidate/local/lito381/server/ClientSession#login([B)V
@@ -1,5 +1,5 @@
 META 2 null [java/lang/Exception]
-TRY [6:[9:java/lang/IllegalArgumentException  ], 117:[123:java/lang/Exception  ], 120:[123:java/lang/Exception  ], 261:[303:java/lang/Exception  ], 262:[303:java/lang/Exception  ], 265:[303:java/lang/Exception  ], 267:[303:java/lang/Exception  ], 268:[303:java/lang/Exception  ], 269:[303:java/lang/Exception  ], 272:[303:java/lang/Exception  ], 274:[303:java/lang/Exception  ], 276:[303:java/lang/Exception  ], 279:[303:java/lang/Exception  ], 283:[303:java/lang/Exception  ], 286:[303:java/lang/Exception  ], 287:[303:java/lang/Exception  ], 289:[303:java/lang/Exception  ], 290:[303:java/lang/Exception  ], 292:[303:java/lang/Exception  ], 294:[303:java/lang/Exception  ], 295:[303:java/lang/Exception  ], 297:[303:java/lang/Exception  ], 298:[303:java/lang/Exception  ], 299:[303:java/lang/Exception  ]]
+TRY [6:[9:java/lang/IllegalArgumentException  ], 117:[123:java/lang/Exception  ], 120:[123:java/lang/Exception  ], 286:[328:java/lang/Exception  ], 287:[328:java/lang/Exception  ], 290:[328:java/lang/Exception  ], 292:[328:java/lang/Exception  ], 293:[328:java/lang/Exception  ], 294:[328:java/lang/Exception  ], 297:[328:java/lang/Exception  ], 299:[328:java/lang/Exception  ], 301:[328:java/lang/Exception  ], 304:[328:java/lang/Exception  ], 308:[328:java/lang/Exception  ], 311:[328:java/lang/Exception  ], 312:[328:java/lang/Exception  ], 314:[328:java/lang/Exception  ], 315:[328:java/lang/Exception  ], 317:[328:java/lang/Exception  ], 319:[328:java/lang/Exception  ], 320:[328:java/lang/Exception  ], 322:[328:java/lang/Exception  ], 323:[328:java/lang/Exception  ], 324:[328:java/lang/Exception  ]]
 ALOAD 0
 GETFIELD local/lito381/server/ClientSession.phase:Llocal/lito381/server/ClientSession$Phase;
 GETSTATIC local/lito381/server/ClientSession$Phase.VERSION_OK:Llocal/lito381/server/ClientSession$Phase;
@@ -159,8 +159,11 @@
 GETFIELD local/lito381/server/ClientSession.state:Llocal/lito381/server/GameStateStore;
 ALOAD 8
 INVOKEVIRTUAL local/lito381/server/AuthResult.account()Ljava/lang/String;
+ALOAD 0
+SWAP
+INVOKESTATIC local/lito381/server/SourceLogin.account(Llocal/lito381/server/ClientSession;Ljava/lang/String;)Ljava/lang/String;
 INVOKEVIRTUAL local/lito381/server/GameStateStore.isAccountBanned(Ljava/lang/String;)Z
-IFEQ L166
+IFEQ L169
 ALOAD 0
 GETSTATIC local/lito381/server/Protocol$LoginCheckResult.ACCOUNT_BANNED:Llocal/lito381/server/Protocol$LoginCheckResult;
 CONST str:"sidecar-account-banned"
@@ -183,37 +186,50 @@
 ALOAD 0
 GETFIELD local/lito381/server/ClientSession.endpoint:Llocal/lito381/server/ClientSession$Endpoint;
 GETSTATIC local/lito381/server/ClientSession$Endpoint.LOGIN:Llocal/lito381/server/ClientSession$Endpoint;
-IF_ACMPNE L190
-ALOAD 0
-GETFIELD local/lito381/server/ClientSession.hub:Llocal/lito381/server/GameHub;
-ALOAD 0
-GETFIELD local/lito381/server/ClientSession.account:Ljava/lang/String;
+IF_ACMPNE L199
+ALOAD 0
+GETFIELD local/lito381/server/ClientSession.hub:Llocal/lito381/server/GameHub;
+ALOAD 0
+GETFIELD local/lito381/server/ClientSession.account:Ljava/lang/String;
+ASTORE 12
+ASTORE 13
+ALOAD 13
+ALOAD 0
+ALOAD 12
+INVOKESTATIC local/lito381/server/SourceLogin.account(Llocal/lito381/server/ClientSession;Ljava/lang/String;)Ljava/lang/String;
 INVOKEVIRTUAL local/lito381/server/GameHub.clearClassicSourceHandoff(Ljava/lang/String;)V
-GOTO L259
-ALOAD 0
-GETFIELD local/lito381/server/ClientSession.hub:Llocal/lito381/server/GameHub;
-ALOAD 0
-GETFIELD local/lito381/server/ClientSession.account:Ljava/lang/String;
+GOTO L284
+ALOAD 0
+GETFIELD local/lito381/server/ClientSession.hub:Llocal/lito381/server/GameHub;
+ALOAD 0
+GETFIELD local/lito381/server/ClientSession.account:Ljava/lang/String;
+ASTORE 14
+ASTORE 15
+ALOAD 15
+ALOAD 0
+ALOAD 14
+INVOKESTATIC local/lito381/server/SourceLogin.account(Llocal/lito381/server/ClientSession;Ljava/lang/String;)Ljava/lang/String;
 INVOKEVIRTUAL local/lito381/server/GameHub.pendingClassicSourceHandoff(Ljava/lang/String;)Ljava/lang/String;
 ASTORE 10
 ALOAD 10
-IFNULL L222
+IFNULL L238
 ALOAD 0
 ALOAD 10
 INVOKESPECIAL local/lito381/server/ClientSession.canonicalClassicSource(Ljava/lang/String;)Ljava/lang/String;
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
 INVOKESPECIAL local/lito381/server/ClientSession.isClassicSourceAuthorized(Ljava/lang/String;)Z
-IFEQ L214
-ALOAD 0
-ALOAD 12
-PUTFIELD local/lito381/server/ClientSession.selectedClassicServerName:Ljava/lang/String;
+IFEQ L230
+ALOAD 0
+ALOAD 16
+CONST str:"selectedClassicServerName"
+INVOKESTATIC local/lito381/server/NativePlayerCommit.scene(Llocal/lito381/server/ClientSession;Ljava/lang/Object;Ljava/lang/String;)V
 ALOAD 0
 INVOKESTATIC local/lito381/server/SourceAdministration.clear(Llocal/lito381/server/ClientSession;)V
-GOTO L222
+GOTO L238
 GETSTATIC java/lang/System.out:Ljava/io/PrintStream;
 ALOAD 0
 GETFIELD local/lito381/server/ClientSession.account:Ljava/lang/String;
@@ -228,21 +244,29 @@
 GETFIELD local/lito381/server/ClientSession.account:Ljava/lang/String;
 ALOAD 0
 GETFIELD local/lito381/server/ClientSession.boundChannelId:I
+ISTORE 17
+ASTORE 18
+ASTORE 19
+ALOAD 19
+ALOAD 0
+ALOAD 18
+INVOKESTATIC local/lito381/server/SourceLogin.account(Llocal/lito381/server/ClientSession;Ljava/lang/String;)Ljava/lang/String;
+ILOAD 17
 INVOKEVIRTUAL local/lito381/server/GameHub.pendingChannelPortHandoff(Ljava/lang/String;I)Llocal/lito381/server/GameHub$ChannelPortHandoff;
 ASTORE 11
 ALOAD 11
-IFNULL L259
+IFNULL L284
 ALOAD 0
 ALOAD 11
 INVOKEVIRTUAL local/lito381/server/GameHub$ChannelPortHandoff.source()Ljava/lang/String;
 INVOKESPECIAL local/lito381/server/ClientSession.canonicalClassicSource(Ljava/lang/String;)Ljava/lang/String;
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
 INVOKESPECIAL local/lito381/server/ClientSession.isClassicSourceAuthorized(Ljava/lang/String;)Z
-IFNE L254
+IFNE L278
 GETSTATIC java/lang/System.out:Ljava/io/PrintStream;
 ALOAD 0
 GETFIELD local/lito381/server/ClientSession.account:Ljava/lang/String;
@@ -253,45 +277,46 @@
 INVOKEVIRTUAL java/io/PrintStream.println(Ljava/lang/String;)V
 CONST null
 ASTORE 11
-GOTO L259
-ALOAD 0
-ALOAD 13
-PUTFIELD local/lito381/server/ClientSession.selectedClassicServerName:Ljava/lang/String;
+GOTO L284
+ALOAD 0
+ALOAD 20
+CONST str:"selectedClassicServerName"
+INVOKESTATIC local/lito381/server/NativePlayerCommit.scene(Llocal/lito381/server/ClientSession;Ljava/lang/Object;Ljava/lang/String;)V
 ALOAD 0
 INVOKESTATIC local/lito381/server/SourceAdministration.clear(Llocal/lito381/server/ClientSession;)V
 ALOAD 0
 ALOAD 0
 GETFIELD local/lito381/server/ClientSession.selectedClassicServerName:Ljava/lang/String;
 INVOKESPECIAL local/lito381/server/ClientSession.loadCharacterDirectory(Ljava/lang/String;)Llocal/lito381/server/ClientSession$SourceCharacterDirectory;
-ASTORE 14
+ASTORE 21
 ALOAD 0
 GETFIELD local/lito381/server/ClientSession.accounts:Llocal/lito381/server/AccountRepository;
 ALOAD 0
 GETFIELD local/lito381/server/ClientSession.selectedClassicServerName:Ljava/lang/String;
 INVOKEINTERFACE local/lito381/server/AccountRepository.repositoryForClassicSource(Ljava/lang/String;)Llocal/lito381/server/AccountRepository; itf
 INVOKEINTERFACE local/lito381/server/AccountRepository.castleWarSchedules()Ljava/util/List; itf
-ASTORE 15
+ASTORE 22
 ALOAD 0
 GETFIELD local/lito381/server/ClientSession.hub:Llocal/lito381/server/GameHub;
 ALOAD 0
 GETFIELD local/lito381/server/ClientSession.selectedClassicServerName:Ljava/lang/String;
-ALOAD 15
+ALOAD 22
 INVOKEVIRTUAL local/lito381/server/GameHub.registerClassicCastleWarSchedules(Ljava/lang/String;Ljava/util/List;)V
 ALOAD 0
-ALOAD 14
+ALOAD 21
 INVOKESPECIAL local/lito381/server/ClientSession.applyCharacterDirectory(Llocal/lito381/server/ClientSession$SourceCharacterDirectory;)V
 ALOAD 11
-IFNULL L302
+IFNULL L327
 ALOAD 11
 INVOKEVIRTUAL local/lito381/server/GameHub$ChannelPortHandoff.character()Ljava/lang/String;
-ASTORE 16
+ASTORE 23
 ALOAD 0
 GETFIELD local/lito381/server/ClientSession.characters:Ljava/util/List;
 INVOKEINTERFACE java/util/List.stream()Ljava/util/stream/Stream; itf
-ALOAD 16
+ALOAD 23
 INVOKEDYNAMIC test(Ljava/lang/String;)Ljava/util/function/Predicate; bsm=H6 java/lang/invoke/LambdaMetafactory.metafactory(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodHandle;Ljava/lang/invoke/MethodType;)Ljava/lang/invoke/CallSite; args=[type:(Ljava/lang/Object;)Z, handle:H6 local/lito381/server/ClientSession.lambda$content$a208176d3f7b650dea1a06c7ea93ad0fcff75da53918965d83817c5a7e27316f(Ljava/lang/String;Llocal/lito381/server/CharacterSummary;)Z, type:(Llocal/lito381/server/CharacterSummary;)Z]
 INVOKEINTERFACE java/util/stream/Stream.noneMatch(Ljava/util/function/Predicate;)Z itf
-IFEQ L302
+IFEQ L327
 GETSTATIC java/lang/System.out:Ljava/io/PrintStream;
 ALOAD 0
 GETFIELD local/lito381/server/ClientSession.account:Ljava/lang/String;
@@ -302,8 +327,8 @@
 INVOKEVIRTUAL java/io/PrintStream.println(Ljava/lang/String;)V
 CONST null
 ASTORE 11
-GOTO L325
-ASTORE 17
+GOTO L350
+ASTORE 24
 ALOAD 0
 CONST null
 PUTFIELD local/lito381/server/ClientSession.account:Ljava/lang/String;
@@ -313,10 +338,10 @@
 INVOKESTATIC java/util/Set.of()Ljava/util/Set; itf
 PUTFIELD local/lito381/server/ClientSession.authenticatedClassicSources:Ljava/util/Set;
 GETSTATIC java/lang/System.out:Ljava/io/PrintStream;
-ALOAD 17
+ALOAD 24
 INVOKEVIRTUAL java/lang/Object.getClass()Ljava/lang/Class;
 INVOKEVIRTUAL java/lang/Class.getSimpleName()Ljava/lang/String;
-ALOAD 17
+ALOAD 24
 INVOKEVIRTUAL java/lang/Exception.getMessage()Ljava/lang/String;
 INVOKEDYNAMIC makeConcatWithConstants(Ljava/lang/String;Ljava/lang/String;)Ljava/lang/String; bsm=H6 java/lang/invoke/StringConcatFactory.makeConcatWithConstants(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;Ljava/lang/String;[Ljava/lang/Object;)Ljava/lang/invoke/CallSite; args=[str:"[LiTo] character lookup failed: \u0001: \u0001"]
 INVOKEVIRTUAL java/io/PrintStream.println(Ljava/lang/String;)V
@@ -333,7 +358,7 @@
 ALOAD 0
 GETFIELD local/lito381/server/ClientSession.account:Ljava/lang/String;
 INVOKEVIRTUAL local/lito381/server/GameHub.acceptAuthenticatedAccount(Llocal/lito381/server/ClientSession;Llocal/lito381/server/ClientSession$Endpoint;Ljava/lang/String;)Z
-IFNE L353
+IFNE L378
 ALOAD 0
 CONST null
 PUTFIELD local/lito381/server/ClientSession.account:Ljava/lang/String;
@@ -356,21 +381,29 @@
 ALOAD 0
 GETFIELD local/lito381/server/ClientSession.endpoint:Llocal/lito381/server/ClientSession$Endpoint;
 GETSTATIC local/lito381/server/ClientSession$Endpoint.GAME:Llocal/lito381/server/ClientSession$Endpoint;
-IF_ACMPNE L376
+IF_ACMPNE L409
 ALOAD 10
-IFNULL L376
+IFNULL L409
 ALOAD 0
 GETFIELD local/lito381/server/ClientSession.hub:Llocal/lito381/server/GameHub;
 ALOAD 0
 GETFIELD local/lito381/server/ClientSession.account:Ljava/lang/String;
 ALOAD 0
 GETFIELD local/lito381/server/ClientSession.selectedClassicServerName:Ljava/lang/String;
+ASTORE 25
+ASTORE 26
+ASTORE 27
+ALOAD 27
+ALOAD 0
+ALOAD 26
+INVOKESTATIC local/lito381/server/SourceLogin.account(Llocal/lito381/server/ClientSession;Ljava/lang/String;)Ljava/lang/String;
+ALOAD 25
 INVOKEVIRTUAL local/lito381/server/GameHub.consumeClassicSourceHandoff(Ljava/lang/String;Ljava/lang/String;)Z
-ISTORE 18
+ISTORE 28
 GETSTATIC java/lang/System.out:Ljava/io/PrintStream;
 ALOAD 0
 GETFIELD local/lito381/server/ClientSession.selectedClassicServerName:Ljava/lang/String;
-ILOAD 18
+ILOAD 28
 ALOAD 0
 GETFIELD local/lito381/server/ClientSession.account:Ljava/lang/String;
 INVOKESTATIC local/lito381/server/ClientSession.mask(Ljava/lang/String;)Ljava/lang/String;
@@ -379,9 +412,9 @@
 ALOAD 0
 GETFIELD local/lito381/server/ClientSession.endpoint:Llocal/lito381/server/ClientSession$Endpoint;
 GETSTATIC local/lito381/server/ClientSession$Endpoint.GAME:Llocal/lito381/server/ClientSession$Endpoint;
-IF_ACMPNE L413
-ALOAD 11
-IFNULL L413
+IF_ACMPNE L446
+ALOAD 11
+IFNULL L446
 ALOAD 0
 ALOAD 11
 INVOKEVIRTUAL local/lito381/server/GameHub$ChannelPortHandoff.channelId()I
@@ -396,11 +429,11 @@
 INVOKEVIRTUAL local/lito381/server/ServerConfig.gameBind()Ljava/lang/String;
 ALOAD 0
 GETFIELD local/lito381/server/ClientSession.boundChannelId:I
-IFGE L401
+IFGE L434
 ALOAD 0
 GETFIELD local/lito381/server/ClientSession.config:Llocal/lito381/server/ServerConfig;
 INVOKEVIRTUAL local/lito381/server/ServerConfig.port()I
-GOTO L406
+GOTO L439
 ALOAD 0
 GETFIELD local/lito381/server/ClientSession.config:Llocal/lito381/server/ServerConfig;
 ALOAD 0
@@ -415,16 +448,17 @@
 INVOKEVIRTUAL java/io/PrintStream.println(Ljava/lang/String;)V
 ALOAD 0
 GETSTATIC local/lito381/server/ClientSession$Phase.AUTHENTICATED:Llocal/lito381/server/ClientSession$Phase;
-PUTFIELD local/lito381/server/ClientSession.phase:Llocal/lito381/server/ClientSession$Phase;
+CONST str:"phase"
+INVOKESTATIC local/lito381/server/NativePlayerCommit.scene(Llocal/lito381/server/ClientSession;Ljava/lang/Object;Ljava/lang/String;)V
 ALOAD 0
 GETFIELD local/lito381/server/ClientSession.endpoint:Llocal/lito381/server/ClientSession$Endpoint;
 GETSTATIC local/lito381/server/ClientSession$Endpoint.GAME:Llocal/lito381/server/ClientSession$Endpoint;
-IF_ACMPNE L425
+IF_ACMPNE L459
 ALOAD 0
 INVOKESPECIAL local/lito381/server/ClientSession.beginGameBootstrap()V
 ALOAD 0
 INVOKESPECIAL local/lito381/server/ClientSession.notifyWantedAccountLogin()V
-GOTO L427
+GOTO L461
 ALOAD 0
 INVOKESPECIAL local/lito381/server/ClientSession.sendLoginBootstrap()V
 GETSTATIC java/lang/System.out:Ljava/io/PrintStream;
```

</details>

### `local/lito381/server/ClientSession#useClassicWorldLineAttackSpell([BLlocal/lito381/server/BattleSpellCatalog$LineAttackSpell;)V`

<details>
<summary>完整差異（全部 hunks）</summary>

```diff
--- staged/local/lito381/server/ClientSession#useClassicWorldLineAttackSpell([BLlocal/lito381/server/BattleSpellCatalog$LineAttackSpell;)V
+++ candidate/local/lito381/server/ClientSession#useClassicWorldLineAttackSpell([BLlocal/lito381/server/BattleSpellCatalog$LineAttackSpell;)V
@@ -5,6 +5,6 @@
 ALOAD 0
 ALOAD 1
 ALOAD 2
-INVOKEDYNAMIC run(Llocal/lito381/server/ClientSession;[BLlocal/lito381/server/BattleSpellCatalog$LineAttackSpell;)Llocal/lito381/server/NativeSelfCastAction$Task; bsm=H6 java/lang/invoke/LambdaMetafactory.metafactory(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodHandle;Ljava/lang/invoke/MethodType;)Ljava/lang/invoke/CallSite; args=[type:()V, handle:H7 local/lito381/server/ClientSession.lambda$content$dfb3791738c5b4d0ec1d5ae51a7b2311d8001284ea4625b6d859dc694635b012([BLlocal/lito381/server/BattleSpellCatalog$LineAttackSpell;)V, type:()V]
+INVOKEDYNAMIC run(Llocal/lito381/server/ClientSession;[BLlocal/lito381/server/BattleSpellCatalog$LineAttackSpell;)Llocal/lito381/server/NativeSelfCastAction$Task; bsm=H6 java/lang/invoke/LambdaMetafactory.metafactory(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodHandle;Ljava/lang/invoke/MethodType;)Ljava/lang/invoke/CallSite; args=[type:()V, handle:H7 local/lito381/server/ClientSession.nativeCompanionTargetuseClassicWorldLineAttackSpell([BLlocal/lito381/server/BattleSpellCatalog$LineAttackSpell;)V, type:()V]
 INVOKESTATIC local/lito381/server/NativeCompanionTargets.cast(Llocal/lito381/server/ClientSession;ILlocal/lito381/server/NativeSelfCastAction$Task;)V
 RETURN
```

</details>

### `local/lito381/server/ClientSession#movementUnlock([B)V`

<details>
<summary>完整差異（全部 hunks）</summary>

```diff
--- staged/local/lito381/server/ClientSession#movementUnlock([B)V
+++ candidate/local/lito381/server/ClientSession#movementUnlock([B)V
@@ -1,4 +1,4 @@
-META 2 null [java/io/IOException]
+META 2 null []
 TRY []
 ALOAD 1
 INVOKESTATIC local/lito381/server/Protocol.movementUnlock([B)V
@@ -15,11 +15,11 @@
 INVOKEVIRTUAL java/io/PrintStream.println(Ljava/lang/String;)V
 RETURN
 ALOAD 0
+CONST Integer:0
+ALOAD 0
 INVOKESTATIC local/lito381/server/Protocol.movementLockRelease()[B
 CONST str:"S_PARALYSE movement-lock-release mode=17"
 INVOKESPECIAL local/lito381/server/ClientSession.sendEncrypted([BLjava/lang/String;)V
-ALOAD 0
-CONST Integer:0
 PUTFIELD local/lito381/server/ClientSession.movementLockPending:Z
 GETSTATIC java/lang/System.out:Ljava/io/PrintStream;
 ALOAD 0
```

</details>

### `local/lito381/server/ClientSession#repair021$lambda$attack$414()I`

<details>
<summary>完整差異（全部 hunks）</summary>

```diff
--- staged/local/lito381/server/ClientSession#repair021$lambda$attack$414()I
+++ candidate/local/lito381/server/ClientSession#repair021$lambda$attack$414()I
@@ -1 +1,6 @@
-MISSING
+META 4106 null []
+TRY []
+INVOKESTATIC java/util/concurrent/ThreadLocalRandom.current()Ljava/util/concurrent/ThreadLocalRandom;
+CONST Integer:1000
+INVOKEVIRTUAL java/util/concurrent/ThreadLocalRandom.nextInt(I)I
+IRETURN
```

</details>

### `local/lito381/server/ClientSession#sourcePoisonBody$useInventoryItemBeforeTravel([BLlocal/lito381/server/CombatPoisonLifecycle$Scope;)V`

<details>
<summary>完整差異（全部 hunks）</summary>

```diff
--- staged/local/lito381/server/ClientSession#sourcePoisonBody$useInventoryItemBeforeTravel([BLlocal/lito381/server/CombatPoisonLifecycle$Scope;)V
+++ formal/local/lito381/server/ClientSession#sourcePoisonBody$useInventoryItemBeforeTravel([BLlocal/lito381/server/CombatPoisonLifecycle$Scope;)V
@@ -82,22 +82,22 @@
 RETURN
 ALOAD 0
 GETFIELD local/lito381/server/ClientSession.hub:Llocal/lito381/server/GameHub;
+ALOAD 0
+INVOKEVIRTUAL local/lito381/server/ClientSession.accountName()Ljava/lang/String;
+ALOAD 0
+INVOKESPECIAL local/lito381/server/ClientSession.stateCharacter()Ljava/lang/String;
+ALOAD 0
+GETFIELD local/lito381/server/ClientSession.rentalTownInventory:Llocal/lito381/server/RuntimeInventory;
 ASTORE 8
-ALOAD 0
-INVOKEVIRTUAL local/lito381/server/ClientSession.accountName()Ljava/lang/String;
 ASTORE 9
-ALOAD 0
-INVOKESPECIAL local/lito381/server/ClientSession.stateCharacter()Ljava/lang/String;
 ASTORE 10
-ALOAD 0
-GETFIELD local/lito381/server/ClientSession.rentalTownInventory:Llocal/lito381/server/RuntimeInventory;
 ASTORE 11
+ALOAD 11
+ALOAD 0
+ALOAD 10
+INVOKESTATIC local/lito381/server/SourceLogin.account(Llocal/lito381/server/ClientSession;Ljava/lang/String;)Ljava/lang/String;
+ALOAD 9
 ALOAD 8
-ALOAD 0
-ALOAD 9
-INVOKESTATIC local/lito381/server/SourceLogin.account(Llocal/lito381/server/ClientSession;Ljava/lang/String;)Ljava/lang/String;
-ALOAD 10
-ALOAD 11
 INVOKEVIRTUAL local/lito381/server/GameHub.stageRentalBattleHandoff(Ljava/lang/String;Ljava/lang/String;Llocal/lito381/server/RuntimeInventory;)Z
 ISTORE 12
 ILOAD 12
@@ -2050,7 +2050,7 @@
 ALOAD 18
 INVOKEVIRTUAL local/lito381/server/InventoryCatalog$Definition.effect()Llocal/lito381/server/InventoryCatalog$Effect;
 GETSTATIC local/lito381/server/InventoryCatalog$Effect.ANTIDOTE:Llocal/lito381/server/InventoryCatalog$Effect;
-IF_ACMPNE L2251
+IF_ACMPNE L2254
 ALOAD 0
 GETFIELD local/lito381/server/ClientSession.phase:Llocal/lito381/server/ClientSession$Phase;
 GETSTATIC local/lito381/server/ClientSession$Phase.BATTLE:Llocal/lito381/server/ClientSession$Phase;
@@ -2092,27 +2092,30 @@
 CONST str:"item-antidote"
 INVOKESPECIAL local/lito381/server/ClientSession.cancelAbsoluteBarrier(Ljava/lang/String;)V
 ALOAD 0
+DUP
 GETFIELD local/lito381/server/ClientSession.timedStatus:Llocal/lito381/server/BattleTimedStatus;
-ALOAD 0
+SWAP
 ALOAD 2
 INVOKESTATIC local/lito381/server/CombatPoisonLifecycle.cure(Llocal/lito381/server/BattleTimedStatus;Llocal/lito381/server/ClientSession;Llocal/lito381/server/CombatPoisonLifecycle$Scope;)Z
 ISTORE 98
 ALOAD 0
+DUP
 GETFIELD local/lito381/server/ClientSession.timedStatus:Llocal/lito381/server/BattleTimedStatus;
-ALOAD 0
+SWAP
 ALOAD 2
 INVOKESTATIC local/lito381/server/CombatPoisonLifecycle.silence(Llocal/lito381/server/BattleTimedStatus;Llocal/lito381/server/ClientSession;Llocal/lito381/server/CombatPoisonLifecycle$Scope;)Z
 ISTORE 99
 ALOAD 0
 GETFIELD local/lito381/server/ClientSession.classicWorldPoisonKind:Llocal/lito381/server/ClassicWorldMonsterPoisonAttack$Kind;
 GETSTATIC local/lito381/server/ClassicWorldMonsterPoisonAttack$Kind.PARALYSIS:Llocal/lito381/server/ClassicWorldMonsterPoisonAttack$Kind;
-IF_ACMPNE L2113
-ALOAD 0
+IF_ACMPNE L2116
+ALOAD 0
+DUP
 GETFIELD local/lito381/server/ClientSession.timedStatus:Llocal/lito381/server/BattleTimedStatus;
-ALOAD 0
+SWAP
 ALOAD 2
 INVOKESTATIC local/lito381/server/CombatPoisonLifecycle.npcCancel(Llocal/lito381/server/BattleTimedStatus;Llocal/lito381/server/ClientSession;Llocal/lito381/server/CombatPoisonLifecycle$Scope;)Llocal/lito381/server/BattleTimedStatus$ParalysisCancellation;
-GOTO L2118
+GOTO L2121
 NEW local/lito381/server/BattleTimedStatus$ParalysisCancellation
 DUP
 CONST Integer:0
@@ -2120,27 +2123,27 @@
 INVOKESPECIAL local/lito381/server/BattleTimedStatus$ParalysisCancellation.<init>(ZZ)V
 ASTORE 100
 ILOAD 98
-IFNE L2126
+IFNE L2129
 ILOAD 99
-IFNE L2126
+IFNE L2129
 ALOAD 100
 INVOKEVIRTUAL local/lito381/server/BattleTimedStatus$ParalysisCancellation.present()Z
-IFEQ L2128
+IFEQ L2131
 CONST Integer:1
-GOTO L2129
+GOTO L2132
 CONST Integer:0
 ISTORE 101
 ILOAD 98
-IFEQ L2135
+IFEQ L2138
 ALOAD 0
 CONST null
 INVOKESTATIC local/lito381/server/CombatPoisonAtomic.attacker(Llocal/lito381/server/ClientSession;Llocal/lito381/server/ClientSession;)V
 ILOAD 101
-IFEQ L2147
+IFEQ L2150
 ALOAD 0
 GETFIELD local/lito381/server/ClientSession.phase:Llocal/lito381/server/ClientSession$Phase;
 GETSTATIC local/lito381/server/ClientSession$Phase.WORLD:Llocal/lito381/server/ClientSession$Phase;
-IF_ACMPNE L2147
+IF_ACMPNE L2150
 ALOAD 0
 CONST Long:0
 INVOKESTATIC local/lito381/server/CombatPoisonAtomic.object(Llocal/lito381/server/ClientSession;J)V
@@ -2168,12 +2171,12 @@
 ALOAD 0
 GETFIELD local/lito381/server/ClientSession.phase:Llocal/lito381/server/ClientSession$Phase;
 GETSTATIC local/lito381/server/ClientSession$Phase.WORLD:Llocal/lito381/server/ClientSession$Phase;
-IF_ACMPNE L2174
+IF_ACMPNE L2177
 ALOAD 0
 ALOAD 0
 GETFIELD local/lito381/server/ClientSession.classicWorldMapId:I
 INVOKESPECIAL local/lito381/server/ClientSession.classicWorldMembers(I)Ljava/util/List;
-GOTO L2179
+GOTO L2182
 ALOAD 0
 GETFIELD local/lito381/server/ClientSession.hub:Llocal/lito381/server/GameHub;
 ALOAD 0
@@ -2185,7 +2188,7 @@
 ASTORE 104
 ALOAD 104
 INVOKEINTERFACE java/util/Iterator.hasNext()Z itf
-IFEQ L2212
+IFEQ L2215
 ALOAD 104
 INVOKEINTERFACE java/util/Iterator.next()Ljava/lang/Object; itf
 CHECKCAST local/lito381/server/ClientSession
@@ -2193,7 +2196,7 @@
 ALOAD 0
 GETFIELD local/lito381/server/ClientSession.phase:Llocal/lito381/server/ClientSession$Phase;
 GETSTATIC local/lito381/server/ClientSession$Phase.WORLD:Llocal/lito381/server/ClientSession$Phase;
-IF_ACMPNE L2205
+IF_ACMPNE L2208
 ALOAD 105
 GETFIELD local/lito381/server/ClientSession.positionX:I
 ALOAD 105
@@ -2203,15 +2206,15 @@
 ALOAD 0
 GETFIELD local/lito381/server/ClientSession.positionY:I
 INVOKESTATIC local/lito381/server/BattleSpellCatalog.isInNativeScreen(IIII)Z
-IFNE L2205
-GOTO L2183
+IFNE L2208
+GOTO L2186
 ALOAD 105
 ALOAD 102
 ALOAD 0
 INVOKEVIRTUAL local/lito381/server/ClientSession.playerObjectId()J
 INVOKEDYNAMIC makeConcatWithConstants(J)Ljava/lang/String; bsm=H6 java/lang/invoke/StringConcatFactory.makeConcatWithConstants(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;Ljava/lang/String;[Ljava/lang/Object;)Ljava/lang/invoke/CallSite; args=[str:"S_EFFECT antidote object=\u0001 gfx=192"]
 INVOKESPECIAL local/lito381/server/ClientSession.sendEncrypted([BLjava/lang/String;)V
-GOTO L2183
+GOTO L2186
 ALOAD 0
 ALOAD 97
 ALOAD 0
@@ -2219,14 +2222,14 @@
 INVOKESPECIAL local/lito381/server/ClientSession.sendInventoryDelta(Llocal/lito381/server/RuntimeInventory;Llocal/lito381/server/RuntimeInventory;)V
 ALOAD 100
 INVOKEVIRTUAL local/lito381/server/BattleTimedStatus$ParalysisCancellation.unlockRequired()Z
-IFEQ L2225
+IFEQ L2228
 ALOAD 0
 GETSTATIC local/lito381/server/Protocol$ParalysisMode.PARALYSIS_RELEASE:Llocal/lito381/server/Protocol$ParalysisMode;
 INVOKESTATIC local/lito381/server/Protocol.paralysis(Llocal/lito381/server/Protocol$ParalysisMode;)[B
 CONST str:"S_PARALYSE mode=3 reason=antidote"
 INVOKESTATIC local/lito381/server/CombatPoisonParalysis.release(Llocal/lito381/server/ClientSession;[BLjava/lang/String;)V
 ILOAD 99
-IFEQ L2235
+IFEQ L2238
 ALOAD 0
 CONST Integer:311
 CONST Integer:0
@@ -2236,7 +2239,7 @@
 ALOAD 2
 INVOKESTATIC local/lito381/server/CombatPoisonLifecycle.silenceDeathSend(Llocal/lito381/server/ClientSession;[BLjava/lang/String;Llocal/lito381/server/CombatPoisonLifecycle$Scope;)V
 ILOAD 101
-IFEQ L2244
+IFEQ L2247
 ALOAD 0
 ALOAD 0
 INVOKESTATIC java/lang/System.nanoTime()J
@@ -2254,17 +2257,17 @@
 ALOAD 18
 INVOKEVIRTUAL local/lito381/server/InventoryCatalog$Definition.effect()Llocal/lito381/server/InventoryCatalog$Effect;
 GETSTATIC local/lito381/server/InventoryCatalog$Effect.BLIND:Llocal/lito381/server/InventoryCatalog$Effect;
-IF_ACMPNE L2325
+IF_ACMPNE L2328
 ALOAD 0
 GETFIELD local/lito381/server/ClientSession.phase:Llocal/lito381/server/ClientSession$Phase;
 GETSTATIC local/lito381/server/ClientSession$Phase.BATTLE:Llocal/lito381/server/ClientSession$Phase;
-IF_ACMPNE L2265
-ALOAD 0
-GETFIELD local/lito381/server/ClientSession.observer:Z
-IFNE L2265
-ALOAD 0
-GETFIELD local/lito381/server/ClientSession.playerHp:I
-IFGT L2276
+IF_ACMPNE L2268
+ALOAD 0
+GETFIELD local/lito381/server/ClientSession.observer:Z
+IFNE L2268
+ALOAD 0
+GETFIELD local/lito381/server/ClientSession.playerHp:I
+IFGT L2279
 GETSTATIC java/lang/System.out:Ljava/io/PrintStream;
 ALOAD 0
 GETFIELD local/lito381/server/ClientSession.phase:Llocal/lito381/server/ClientSession$Phase;
@@ -2286,7 +2289,7 @@
 INVOKEVIRTUAL local/lito381/server/RuntimeInventory$Item.objectId()J
 CONST Long:1
 INVOKEVIRTUAL local/lito381/server/RuntimeInventory.remove(JJ)Z
-IFNE L2288
+IFNE L2291
 RETURN
 ALOAD 0
 CONST str:"item-blindness"
@@ -2328,17 +2331,17 @@
 ALOAD 18
 INVOKEVIRTUAL local/lito381/server/InventoryCatalog$Definition.effect()Llocal/lito381/server/InventoryCatalog$Effect;
 GETSTATIC local/lito381/server/InventoryCatalog$Effect.BREATH:Llocal/lito381/server/InventoryCatalog$Effect;
-IF_ACMPNE L2436
+IF_ACMPNE L2439
 ALOAD 0
 GETFIELD local/lito381/server/ClientSession.phase:Llocal/lito381/server/ClientSession$Phase;
 GETSTATIC local/lito381/server/ClientSession$Phase.BATTLE:Llocal/lito381/server/ClientSession$Phase;
-IF_ACMPNE L2339
-ALOAD 0
-GETFIELD local/lito381/server/ClientSession.observer:Z
-IFNE L2339
-ALOAD 0
-GETFIELD local/lito381/server/ClientSession.playerHp:I
-IFGT L2350
+IF_ACMPNE L2342
+ALOAD 0
+GETFIELD local/lito381/server/ClientSession.observer:Z
+IFNE L2342
+ALOAD 0
+GETFIELD local/lito381/server/ClientSession.playerHp:I
+IFGT L2353
 GETSTATIC java/lang/System.out:Ljava/io/PrintStream;
 ALOAD 0
 GETFIELD local/lito381/server/ClientSession.phase:Llocal/lito381/server/ClientSession$Phase;
@@ -2360,7 +2363,7 @@
 INVOKEVIRTUAL local/lito381/server/RuntimeInventory$Item.objectId()J
 CONST Long:1
 INVOKEVIRTUAL local/lito381/server/RuntimeInventory.remove(JJ)Z
-IFNE L2362
+IFNE L2365
 RETURN
 ALOAD 0
 CONST str:"item-underwater-breath"
@@ -2398,7 +2401,7 @@
 ASTORE 110
 ALOAD 110
 INVOKEINTERFACE java/util/Iterator.hasNext()Z itf
-IFEQ L2410
+IFEQ L2413
 ALOAD 110
 INVOKEINTERFACE java/util/Iterator.next()Ljava/lang/Object; itf
 CHECKCAST local/lito381/server/ClientSession
@@ -2409,7 +2412,7 @@
 INVOKEVIRTUAL local/lito381/server/ClientSession.playerObjectId()J
 INVOKEDYNAMIC makeConcatWithConstants(J)Ljava/lang/String; bsm=H6 java/lang/invoke/StringConcatFactory.makeConcatWithConstants(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;Ljava/lang/String;[Ljava/lang/Object;)Ljava/lang/invoke/CallSite; args=[str:"S_EFFECT underwater-breath object=\u0001 gfx=190"]
 INVOKESPECIAL local/lito381/server/ClientSession.sendEncrypted([BLjava/lang/String;)V
-GOTO L2396
+GOTO L2399
 ALOAD 0
 ALOAD 107
 ALOAD 0
@@ -2439,7 +2442,7 @@
 ALOAD 18
 INVOKEVIRTUAL local/lito381/server/InventoryCatalog$Definition.effect()Llocal/lito381/server/InventoryCatalog$Effect;
 GETSTATIC local/lito381/server/InventoryCatalog$Effect.POLYMORPH:Llocal/lito381/server/InventoryCatalog$Effect;
-IF_ACMPNE L2445
+IF_ACMPNE L2448
 ALOAD 0
 ALOAD 17
 ALOAD 26
@@ -2448,17 +2451,17 @@
 ALOAD 18
 INVOKEVIRTUAL local/lito381/server/InventoryCatalog$Definition.effect()Llocal/lito381/server/InventoryCatalog$Effect;
 GETSTATIC local/lito381/server/InventoryCatalog$Effect.WHISTLE:Llocal/lito381/server/InventoryCatalog$Effect;
-IF_ACMPNE L2499
+IF_ACMPNE L2502
 ALOAD 0
 GETFIELD local/lito381/server/ClientSession.phase:Llocal/lito381/server/ClientSession$Phase;
 GETSTATIC local/lito381/server/ClientSession$Phase.BATTLE:Llocal/lito381/server/ClientSession$Phase;
-IF_ACMPNE L2459
-ALOAD 0
-GETFIELD local/lito381/server/ClientSession.observer:Z
-IFNE L2459
-ALOAD 0
-GETFIELD local/lito381/server/ClientSession.playerHp:I
-IFGT L2470
+IF_ACMPNE L2462
+ALOAD 0
+GETFIELD local/lito381/server/ClientSession.observer:Z
+IFNE L2462
+ALOAD 0
+GETFIELD local/lito381/server/ClientSession.playerHp:I
+IFGT L2473
 GETSTATIC java/lang/System.out:Ljava/io/PrintStream;
 ALOAD 0
 GETFIELD local/lito381/server/ClientSession.phase:Llocal/lito381/server/ClientSession$Phase;
@@ -2483,7 +2486,7 @@
 ASTORE 113
 ALOAD 113
 INVOKEINTERFACE java/util/Iterator.hasNext()Z itf
-IFEQ L2493
+IFEQ L2496
 ALOAD 113
 INVOKEINTERFACE java/util/Iterator.next()Ljava/lang/Object; itf
 CHECKCAST local/lito381/server/ClientSession
@@ -2492,7 +2495,7 @@
 ALOAD 112
 CONST str:"S_SOUND_EFFECT repeat=0 sound=437 item=whistle"
 INVOKESPECIAL local/lito381/server/ClientSession.sendEncrypted([BLjava/lang/String;)V
-GOTO L2481
+GOTO L2484
 GETSTATIC java/lang/System.out:Ljava/io/PrintStream;
 ALOAD 17
 INVOKEVIRTUAL local/lito381/server/RuntimeInventory$Item.objectId()J
@@ -2502,17 +2505,17 @@
 ALOAD 18
 INVOKEVIRTUAL local/lito381/server/InventoryCatalog$Definition.effect()Llocal/lito381/server/InventoryCatalog$Effect;
 GETSTATIC local/lito381/server/InventoryCatalog$Effect.LIQUOR:Llocal/lito381/server/InventoryCatalog$Effect;
-IF_ACMPNE L2575
+IF_ACMPNE L2578
 ALOAD 0
 GETFIELD local/lito381/server/ClientSession.phase:Llocal/lito381/server/ClientSession$Phase;
 GETSTATIC local/lito381/server/ClientSession$Phase.BATTLE:Llocal/lito381/server/ClientSession$Phase;
-IF_ACMPNE L2513
-ALOAD 0
-GETFIELD local/lito381/server/ClientSession.observer:Z
-IFNE L2513
-ALOAD 0
-GETFIELD local/lito381/server/ClientSession.playerHp:I
-IFGT L2524
+IF_ACMPNE L2516
+ALOAD 0
+GETFIELD local/lito381/server/ClientSession.observer:Z
+IFNE L2516
+ALOAD 0
+GETFIELD local/lito381/server/ClientSession.playerHp:I
+IFGT L2527
 GETSTATIC java/lang/System.out:Ljava/io/PrintStream;
 ALOAD 0
 GETFIELD local/lito381/server/ClientSession.phase:Llocal/lito381/server/ClientSession$Phase;
@@ -2534,7 +2537,7 @@
 INVOKEVIRTUAL local/lito381/server/RuntimeInventory$Item.objectId()J
 CONST Long:1
 INVOKEVIRTUAL local/lito381/server/RuntimeInventory.remove(JJ)Z
-IFNE L2536
+IFNE L2539
 RETURN
 ALOAD 0
 CONST str:"item-liquor"
```

</details>

### `local/lito381/server/ClientSession#sourceApplicationRun()V`

<details>
<summary>完整差異（全部 hunks）</summary>

```diff
--- staged/local/lito381/server/ClientSession#sourceApplicationRun()V
+++ candidate/local/lito381/server/ClientSession#sourceApplicationRun()V
@@ -1,5 +1,5 @@
 META 2 null []
-TRY [1:[8:java/lang/Exception  ], 3:[8:java/lang/Exception  ], 4:[8:java/lang/Exception  ], 28:[433:java/lang/Exception  , 755:java/lang/Throwable  ], 31:[106:java/lang/Throwable  , 433:java/lang/Exception  , 755:java/lang/Throwable  ], 33:[106:java/lang/Throwable  , 433:java/lang/Exception  , 755:java/lang/Throwable  ], 35:[106:java/lang/Throwable  , 433:java/lang/Exception  , 755:java/lang/Throwable  ], 37:[106:java/lang/Throwable  , 433:java/lang/Exception  , 755:java/lang/Throwable  ], 38:[106:java/lang/Throwable  , 433:java/lang/Exception  , 755:java/lang/Throwable  ], 40:[106:java/lang/Throwable  , 433:java/lang/Exception  , 755:java/lang/Throwable  ], 41:[106:java/lang/Throwable  , 433:java/lang/Exception  , 755:java/lang/Throwable  ], 43:[106:java/lang/Throwable  , 433:java/lang/Exception  , 755:java/lang/Throwable  ], 45:[106:java/lang/Throwable  , 433:java/lang/Exception  , 755:java/lang/Throwable  ], 47:[106:java/lang/Throwable  , 433:java/lang/Exception  , 755:java/lang/Throwable  ], 49:[106:java/lang/Throwable  , 433:java/lang/Exception  , 755:java/lang/Throwable  ], 50:[106:java/lang/Throwable  , 433:java/lang/Exception  , 755:java/lang/Throwable  ], 54:[106:java/lang/Throwable  , 433:java/lang/Exception  , 755:java/lang/Throwable  ], 56:[106:java/lang/Throwable  , 433:java/lang/Exception  , 755:java/lang/Throwable  ], 58:[106:java/lang/Throwable  , 433:java/lang/Exception  , 755:java/lang/Throwable  ], 61:[106:java/lang/Throwable  , 433:java/lang/Exception  , 755:java/lang/Throwable  ], 62:[106:java/lang/Throwable  , 433:java/lang/Exception  , 755:java/lang/Throwable  ], 64:[106:java/lang/Throwable  , 433:java/lang/Exception  , 755:java/lang/Throwable  ], 67:[106:java/lang/Throwable  , 433:java/lang/Exception  , 755:java/lang/Throwable  ], 68:[106:java/lang/Throwable  , 433:java/lang/Exception  , 755:java/lang/Throwable  ], 70:[106:java/lang/Throwable  , 433:java/lang/Exception  , 755:java/lang/Throwable  ], 71:[106:java/lang/Throwable  , 433:java/lang/Exception  , 755:java/lang/Throwable  ], 73:[106:java/lang/Throwable  , 433:java/lang/Exception  , 755:java/lang/Throwable  ], 74:[106:java/lang/Throwable  , 433:java/lang/Exception  , 755:java/lang/Throwable  ], 76:[106:java/lang/Throwable  , 433:java/lang/Exception  , 755:java/lang/Throwable  ], 82:[106:java/lang/Throwable  , 433:java/lang/Exception  , 755:java/lang/Throwable  ], 84:[106:java/lang/Throwable  , 433:java/lang/Exception  , 755:java/lang/Throwable  ], 87:[106:java/lang/Throwable  , 433:java/lang/Exception  , 755:java/lang/Throwable  ], 91:[106:java/lang/Throwable  , 433:java/lang/Exception  , 755:java/lang/Throwable  ], 95:[106:java/lang/Throwable  , 433:java/lang/Exception  , 755:java/lang/Throwable  ], 96:[106:java/lang/Throwable  , 433:java/lang/Exception  , 755:java/lang/Throwable  ], 99:[106:java/lang/Throwable  , 433:java/lang/Exception  , 755:java/lang/Throwable  ], 101:[106:java/lang/Throwable  , 433:java/lang/Exception  , 755:java/lang/Throwable  ], 111:[113:java/lang/Throwable  , 433:java/lang/Exception  , 755:java/lang/Throwable  ], 116:[433:java/lang/Exception  , 755:java/lang/Throwable  ], 118:[433:java/lang/Exception  , 755:java/lang/Throwable  ], 123:[433:java/lang/Exception  , 755:java/lang/Throwable  ], 150:[152:java/io/IOException  ], 164:[166:java/io/IOException  ], 177:[179:java/io/IOException  , 179:java/lang/RuntimeException  ], 186:[188:java/io/IOException  , 188:java/lang/RuntimeException  ], 195:[197:java/io/IOException  , 197:java/lang/RuntimeException  ], 204:[206:java/io/IOException  , 206:java/lang/RuntimeException  ], 213:[215:java/io/IOException  , 215:java/lang/RuntimeException  ], 222:[224:java/io/IOException  , 224:java/lang/RuntimeException  ], 231:[233:java/io/IOException  , 233:java/lang/RuntimeException  ], 240:[242:java/io/IOException  , 242:java/lang/RuntimeException  ], 249:[251:java/io/IOException  , 251:java/lang/RuntimeException  ], 258:[260:java/io/IOException  , 260:java/lang/RuntimeException  ], 267:[269:java/io/IOException  , 269:java/lang/RuntimeException  ], 276:[278:java/io/IOException  , 278:java/lang/RuntimeException  ], 285:[287:java/io/IOException  , 287:java/lang/RuntimeException  ], 299:[307:java/io/IOException  , 307:java/lang/RuntimeException  ], 302:[307:java/io/IOException  , 307:java/lang/RuntimeException  ], 305:[307:java/io/IOException  , 307:java/lang/RuntimeException  ], 314:[331:java/io/IOException  , 331:java/lang/RuntimeException  ], 317:[331:java/io/IOException  , 331:java/lang/RuntimeException  ], 321:[325:java/lang/Throwable  , 331:java/io/IOException  , 331:java/lang/RuntimeException  ], 323:[325:java/lang/Throwable  , 331:java/io/IOException  , 331:java/lang/RuntimeException  ], 327:[325:java/lang/Throwable  , 331:java/io/IOException  , 331:java/lang/RuntimeException  ], 329:[331:java/io/IOException  , 331:java/lang/RuntimeException  ], 338:[340:java/lang/Exception  ], 350:[352:java/io/IOException  ], 400:[402:java/io/IOException  ], 422:[424:java/io/IOException  ], 434:[755:java/lang/Throwable  ], 436:[755:java/lang/Throwable  ], 437:[755:java/lang/Throwable  ], 440:[755:java/lang/Throwable  ], 441:[755:java/lang/Throwable  ], 443:[755:java/lang/Throwable  ], 444:[755:java/lang/Throwable  ], 445:[755:java/lang/Throwable  ], 472:[474:java/io/IOException  ], 486:[488:java/io/IOException  ], 499:[501:java/io/IOException  , 501:java/lang/RuntimeException  ], 508:[510:java/io/IOException  , 510:java/lang/RuntimeException  ], 517:[519:java/io/IOException  , 519:java/lang/RuntimeException  ], 526:[528:java/io/IOException  , 528:java/lang/RuntimeException  ], 535:[537:java/io/IOException  , 537:java/lang/RuntimeException  ], 544:[546:java/io/IOException  , 546:java/lang/RuntimeException  ], 553:[555:java/io/IOException  , 555:java/lang/RuntimeException  ], 562:[564:java/io/IOException  , 564:java/lang/RuntimeException  ], 571:[573:java/io/IOException  , 573:java/lang/RuntimeException  ], 580:[582:java/io/IOException  , 582:java/lang/RuntimeException  ], 589:[591:java/io/IOException  , 591:java/lang/RuntimeException  ], 598:[600:java/io/IOException  , 600:java/lang/RuntimeException  ], 607:[609:java/io/IOException  , 609:java/lang/RuntimeException  ], 621:[629:java/io/IOException  , 629:java/lang/RuntimeException  ], 624:[629:java/io/IOException  , 629:java/lang/RuntimeException  ], 627:[629:java/io/IOException  , 629:java/lang/RuntimeException  ], 636:[653:java/io/IOException  , 653:java/lang/RuntimeException  ], 639:[653:java/io/IOException  , 653:java/lang/RuntimeException  ], 643:[647:java/lang/Throwable  , 653:java/io/IOException  , 653:java/lang/RuntimeException  ], 645:[647:java/lang/Throwable  , 653:java/io/IOException  , 653:java/lang/RuntimeException  ], 649:[647:java/lang/Throwable  , 653:java/io/IOException  , 653:java/lang/RuntimeException  ], 651:[653:java/io/IOException  , 653:java/lang/RuntimeException  ], 660:[662:java/lang/Exception  ], 672:[674:java/io/IOException  ], 722:[724:java/io/IOException  ], 744:[746:java/io/IOException  ], 782:[784:java/io/IOException  ], 796:[798:java/io/IOException  ], 809:[811:java/io/IOException  , 811:java/lang/RuntimeException  ], 818:[820:java/io/IOException  , 820:java/lang/RuntimeException  ], 827:[829:java/io/IOException  , 829:java/lang/RuntimeException  ], 836:[838:java/io/IOException  , 838:java/lang/RuntimeException  ], 845:[847:java/io/IOException  , 847:java/lang/RuntimeException  ], 854:[856:java/io/IOException  , 856:java/lang/RuntimeException  ], 863:[865:java/io/IOException  , 865:java/lang/RuntimeException  ], 872:[874:java/io/IOException  , 874:java/lang/RuntimeException  ], 881:[883:java/io/IOException  , 883:java/lang/RuntimeException  ], 890:[892:java/io/IOException  , 892:java/lang/RuntimeException  ], 899:[901:java/io/IOException  , 901:java/lang/RuntimeException  ], 908:[910:java/io/IOException  , 910:java/lang/RuntimeException  ], 917:[919:java/io/IOException  , 919:java/lang/RuntimeException  ], 931:[939:java/io/IOException  , 939:java/lang/RuntimeException  ], 934:[939:java/io/IOException  , 939:java/lang/RuntimeException  ], 937:[939:java/io/IOException  , 939:java/lang/RuntimeException  ], 946:[963:java/io/IOException  , 963:java/lang/RuntimeException  ], 949:[963:java/io/IOException  , 963:java/lang/RuntimeException  ], 953:[957:java/lang/Throwable  , 963:java/io/IOException  , 963:java/lang/RuntimeException  ], 955:[957:java/lang/Throwable  , 963:java/io/IOException  , 963:java/lang/RuntimeException  ], 959:[957:java/lang/Throwable  , 963:java/io/IOException  , 963:java/lang/RuntimeException  ], 961:[963:java/io/IOException  , 963:java/lang/RuntimeException  ], 970:[972:java/lang/Exception  ], 982:[984:java/io/IOException  ], 1032:[1034:java/io/IOException  ], 1054:[1056:java/io/IOException  ]]
+TRY [24:[740:java/lang/Exception  , 1062:java/lang/Throwable  ], 27:[727:java/lang/Throwable  , 740:java/lang/Exception  , 1062:java/lang/Throwable  ], 29:[727:java/lang/Throwable  , 740:java/lang/Exception  , 1062:java/lang/Throwable  ], 31:[727:java/lang/Throwable  , 740:java/lang/Exception  , 1062:java/lang/Throwable  ], 33:[727:java/lang/Throwable  , 740:java/lang/Exception  , 1062:java/lang/Throwable  ], 34:[727:java/lang/Throwable  , 740:java/lang/Exception  , 1062:java/lang/Throwable  ], 36:[727:java/lang/Throwable  , 740:java/lang/Exception  , 1062:java/lang/Throwable  ], 37:[727:java/lang/Throwable  , 740:java/lang/Exception  , 1062:java/lang/Throwable  ], 39:[727:java/lang/Throwable  , 740:java/lang/Exception  , 1062:java/lang/Throwable  ], 41:[727:java/lang/Throwable  , 740:java/lang/Exception  , 1062:java/lang/Throwable  ], 43:[727:java/lang/Throwable  , 740:java/lang/Exception  , 1062:java/lang/Throwable  ], 45:[727:java/lang/Throwable  , 740:java/lang/Exception  , 1062:java/lang/Throwable  ], 46:[727:java/lang/Throwable  , 740:java/lang/Exception  , 1062:java/lang/Throwable  ], 50:[727:java/lang/Throwable  , 740:java/lang/Exception  , 1062:java/lang/Throwable  ], 52:[727:java/lang/Throwable  , 740:java/lang/Exception  , 1062:java/lang/Throwable  ], 54:[727:java/lang/Throwable  , 740:java/lang/Exception  , 1062:java/lang/Throwable  ], 57:[727:java/lang/Throwable  , 740:java/lang/Exception  , 1062:java/lang/Throwable  ], 58:[727:java/lang/Throwable  , 740:java/lang/Exception  , 1062:java/lang/Throwable  ], 60:[727:java/lang/Throwable  , 740:java/lang/Exception  , 1062:java/lang/Throwable  ], 63:[727:java/lang/Throwable  , 740:java/lang/Exception  , 1062:java/lang/Throwable  ], 64:[727:java/lang/Throwable  , 740:java/lang/Exception  , 1062:java/lang/Throwable  ], 66:[727:java/lang/Throwable  , 740:java/lang/Exception  , 1062:java/lang/Throwable  ], 67:[727:java/lang/Throwable  , 740:java/lang/Exception  , 1062:java/lang/Throwable  ], 69:[727:java/lang/Throwable  , 740:java/lang/Exception  , 1062:java/lang/Throwable  ], 70:[727:java/lang/Throwable  , 740:java/lang/Exception  , 1062:java/lang/Throwable  ], 72:[727:java/lang/Throwable  , 740:java/lang/Exception  , 1062:java/lang/Throwable  ], 80:[740:java/lang/Exception  , 1062:java/lang/Throwable  ], 107:[109:java/io/IOException  ], 121:[123:java/io/IOException  ], 134:[136:java/io/IOException  , 136:java/lang/RuntimeException  ], 143:[145:java/io/IOException  , 145:java/lang/RuntimeException  ], 152:[154:java/io/IOException  , 154:java/lang/RuntimeException  ], 161:[163:java/io/IOException  , 163:java/lang/RuntimeException  ], 170:[172:java/io/IOException  , 172:java/lang/RuntimeException  ], 179:[181:java/io/IOException  , 181:java/lang/RuntimeException  ], 188:[190:java/io/IOException  , 190:java/lang/RuntimeException  ], 197:[199:java/io/IOException  , 199:java/lang/RuntimeException  ], 206:[208:java/io/IOException  , 208:java/lang/RuntimeException  ], 215:[217:java/io/IOException  , 217:java/lang/RuntimeException  ], 224:[226:java/io/IOException  , 226:java/lang/RuntimeException  ], 233:[235:java/io/IOException  , 235:java/lang/RuntimeException  ], 242:[244:java/io/IOException  , 244:java/lang/RuntimeException  ], 256:[264:java/io/IOException  , 264:java/lang/RuntimeException  ], 259:[264:java/io/IOException  , 264:java/lang/RuntimeException  ], 262:[264:java/io/IOException  , 264:java/lang/RuntimeException  ], 271:[288:java/io/IOException  , 288:java/lang/RuntimeException  ], 274:[288:java/io/IOException  , 288:java/lang/RuntimeException  ], 278:[282:java/lang/Throwable  , 288:java/io/IOException  , 288:java/lang/RuntimeException  ], 280:[282:java/lang/Throwable  , 288:java/io/IOException  , 288:java/lang/RuntimeException  ], 284:[282:java/lang/Throwable  , 288:java/io/IOException  , 288:java/lang/RuntimeException  ], 286:[288:java/io/IOException  , 288:java/lang/RuntimeException  ], 295:[297:java/lang/Exception  ], 307:[309:java/io/IOException  ], 357:[359:java/io/IOException  ], 379:[381:java/io/IOException  ], 391:[727:java/lang/Throwable  , 740:java/lang/Exception  , 1062:java/lang/Throwable  ], 393:[727:java/lang/Throwable  , 740:java/lang/Exception  , 1062:java/lang/Throwable  ], 396:[727:java/lang/Throwable  , 740:java/lang/Exception  , 1062:java/lang/Throwable  ], 400:[727:java/lang/Throwable  , 740:java/lang/Exception  , 1062:java/lang/Throwable  ], 404:[727:java/lang/Throwable  , 740:java/lang/Exception  , 1062:java/lang/Throwable  ], 405:[727:java/lang/Throwable  , 740:java/lang/Exception  , 1062:java/lang/Throwable  ], 408:[727:java/lang/Throwable  , 740:java/lang/Exception  , 1062:java/lang/Throwable  ], 410:[727:java/lang/Throwable  , 740:java/lang/Exception  , 1062:java/lang/Throwable  ], 416:[740:java/lang/Exception  , 1062:java/lang/Throwable  ], 443:[445:java/io/IOException  ], 457:[459:java/io/IOException  ], 470:[472:java/io/IOException  , 472:java/lang/RuntimeException  ], 479:[481:java/io/IOException  , 481:java/lang/RuntimeException  ], 488:[490:java/io/IOException  , 490:java/lang/RuntimeException  ], 497:[499:java/io/IOException  , 499:java/lang/RuntimeException  ], 506:[508:java/io/IOException  , 508:java/lang/RuntimeException  ], 515:[517:java/io/IOException  , 517:java/lang/RuntimeException  ], 524:[526:java/io/IOException  , 526:java/lang/RuntimeException  ], 533:[535:java/io/IOException  , 535:java/lang/RuntimeException  ], 542:[544:java/io/IOException  , 544:java/lang/RuntimeException  ], 551:[553:java/io/IOException  , 553:java/lang/RuntimeException  ], 560:[562:java/io/IOException  , 562:java/lang/RuntimeException  ], 569:[571:java/io/IOException  , 571:java/lang/RuntimeException  ], 578:[580:java/io/IOException  , 580:java/lang/RuntimeException  ], 592:[600:java/io/IOException  , 600:java/lang/RuntimeException  ], 595:[600:java/io/IOException  , 600:java/lang/RuntimeException  ], 598:[600:java/io/IOException  , 600:java/lang/RuntimeException  ], 607:[624:java/io/IOException  , 624:java/lang/RuntimeException  ], 610:[624:java/io/IOException  , 624:java/lang/RuntimeException  ], 614:[618:java/lang/Throwable  , 624:java/io/IOException  , 624:java/lang/RuntimeException  ], 616:[618:java/lang/Throwable  , 624:java/io/IOException  , 624:java/lang/RuntimeException  ], 620:[618:java/lang/Throwable  , 624:java/io/IOException  , 624:java/lang/RuntimeException  ], 622:[624:java/io/IOException  , 624:java/lang/RuntimeException  ], 631:[633:java/lang/Exception  ], 643:[645:java/io/IOException  ], 693:[695:java/io/IOException  ], 715:[717:java/io/IOException  ], 732:[734:java/lang/Throwable  , 740:java/lang/Exception  , 1062:java/lang/Throwable  ], 737:[740:java/lang/Exception  , 1062:java/lang/Throwable  ], 739:[740:java/lang/Exception  , 1062:java/lang/Throwable  ], 741:[1062:java/lang/Throwable  ], 743:[1062:java/lang/Throwable  ], 744:[1062:java/lang/Throwable  ], 747:[1062:java/lang/Throwable  ], 748:[1062:java/lang/Throwable  ], 750:[1062:java/lang/Throwable  ], 751:[1062:java/lang/Throwable  ], 752:[1062:java/lang/Throwable  ], 779:[781:java/io/IOException  ], 793:[795:java/io/IOException  ], 806:[808:java/io/IOException  , 808:java/lang/RuntimeException  ], 815:[817:java/io/IOException  , 817:java/lang/RuntimeException  ], 824:[826:java/io/IOException  , 826:java/lang/RuntimeException  ], 833:[835:java/io/IOException  , 835:java/lang/RuntimeException  ], 842:[844:java/io/IOException  , 844:java/lang/RuntimeException  ], 851:[853:java/io/IOException  , 853:java/lang/RuntimeException  ], 860:[862:java/io/IOException  , 862:java/lang/RuntimeException  ], 869:[871:java/io/IOException  , 871:java/lang/RuntimeException  ], 878:[880:java/io/IOException  , 880:java/lang/RuntimeException  ], 887:[889:java/io/IOException  , 889:java/lang/RuntimeException  ], 896:[898:java/io/IOException  , 898:java/lang/RuntimeException  ], 905:[907:java/io/IOException  , 907:java/lang/RuntimeException  ], 914:[916:java/io/IOException  , 916:java/lang/RuntimeException  ], 928:[936:java/io/IOException  , 936:java/lang/RuntimeException  ], 931:[936:java/io/IOException  , 936:java/lang/RuntimeException  ], 934:[936:java/io/IOException  , 936:java/lang/RuntimeException  ], 943:[960:java/io/IOException  , 960:java/lang/RuntimeException  ], 946:[960:java/io/IOException  , 960:java/lang/RuntimeException  ], 950:[954:java/lang/Throwable  , 960:java/io/IOException  , 960:java/lang/RuntimeException  ], 952:[954:java/lang/Throwable  , 960:java/io/IOException  , 960:java/lang/RuntimeException  ], 956:[954:java/lang/Throwable  , 960:java/io/IOException  , 960:java/lang/RuntimeException  ], 958:[960:java/io/IOException  , 960:java/lang/RuntimeException  ], 967:[969:java/lang/Exception  ], 979:[981:java/io/IOException  ], 1029:[1031:java/io/IOException  ], 1051:[1053:java/io/IOException  ], 1089:[1091:java/io/IOException  ], 1103:[1105:java/io/IOException  ], 1116:[1118:java/io/IOException  , 1118:java/lang/RuntimeException  ], 1125:[1127:java/io/IOException  , 1127:java/lang/RuntimeException  ], 1134:[1136:java/io/IOException  , 1136:java/lang/RuntimeException  ], 1143:[1145:java/io/IOException  , 1145:java/lang/RuntimeException  ], 1152:[1154:java/io/IOException  , 1154:java/lang/RuntimeException  ], 1161:[1163:java/io/IOException  , 1163:java/lang/RuntimeException  ], 1170:[1172:java/io/IOException  , 1172:java/lang/RuntimeException  ], 1179:[1181:java/io/IOException  , 1181:java/lang/RuntimeException  ], 1188:[1190:java/io/IOException  , 1190:java/lang/RuntimeException  ], 1197:[1199:java/io/IOException  , 1199:java/lang/RuntimeException  ], 1206:[1208:java/io/IOException  , 1208:java/lang/RuntimeException  ], 1215:[1217:java/io/IOException  , 1217:java/lang/RuntimeException  ], 1224:[1226:java/io/IOException  , 1226:java/lang/RuntimeException  ], 1238:[1246:java/io/IOException  , 1246:java/lang/RuntimeException  ], 1241:[1246:java/io/IOException  , 1246:java/lang/RuntimeException  ], 1244:[1246:java/io/IOException  , 1246:java/lang/RuntimeException  ], 1253:[1270:java/io/IOException  , 1270:java/lang/RuntimeException  ], 1256:[1270:java/io/IOException  , 1270:java/lang/RuntimeException  ], 1260:[1264:java/lang/Throwable  , 1270:java/io/IOException  , 1270:java/lang/RuntimeException  ], 1262:[1264:java/lang/Throwable  , 1270:java/io/IOException  , 1270:java/lang/RuntimeException  ], 1266:[1264:java/lang/Throwable  , 1270:java/io/IOException  , 1270:java/lang/RuntimeException  ], 1268:[1270:java/io/IOException  , 1270:java/lang/RuntimeException  ], 1277:[1279:java/lang/Exception  ], 1289:[1291:java/io/IOException  ], 1339:[1341:java/io/IOException  ], 1361:[1363:java/io/IOException  ]]
 ALOAD 0
 GETFIELD local/lito381/server/ClientSession.socket:Ljava/net/Socket;
 ALOAD 0
@@ -7,20 +7,16 @@
 INVOKESTATIC local/lito381/server/SourceLogin.allowed(Ljava/net/Socket;Llocal/lito381/server/ServerConfig;)Z
 IFNE L7
 RETURN
-GOTO L11
-ASTORE 1
-ALOAD 1
-INVOKESTATIC local/lito381/server/ClientSession.rethrowUnchecked(Ljava/lang/Throwable;)V
 ALOAD 0
 GETFIELD local/lito381/server/ClientSession.socket:Ljava/net/Socket;
 INVOKEVIRTUAL java/net/Socket.getRemoteSocketAddress()Ljava/net/SocketAddress;
 INVOKEVIRTUAL java/lang/Object.toString()Ljava/lang/String;
-ASTORE 2
+ASTORE 1
 GETSTATIC java/lang/System.out:Ljava/io/PrintStream;
 ALOAD 0
 GETFIELD local/lito381/server/ClientSession.endpoint:Llocal/lito381/server/ClientSession$Endpoint;
 INVOKESTATIC java/lang/String.valueOf(Ljava/lang/Object;)Ljava/lang/String;
-ALOAD 2
+ALOAD 1
 INVOKEDYNAMIC makeConcatWithConstants(Ljava/lang/String;Ljava/lang/String;)Ljava/lang/String; bsm=H6 java/lang/invoke/StringConcatFactory.makeConcatWithConstants(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;Ljava/lang/String;[Ljava/lang/Object;)Ljava/lang/invoke/CallSite; args=[str:"[LiTo] connected endpoint=\u0001 peer=\u0001"]
 INVOKEVIRTUAL java/io/PrintStream.println(Ljava/lang/String;)V
 ALOAD 0
@@ -29,7 +25,7 @@
 INVOKEVIRTUAL local/lito381/server/GameHub.connect(Llocal/lito381/server/ClientSession;)V
 ALOAD 0
 GETFIELD local/lito381/server/ClientSession.socket:Ljava/net/Socket;
-ASTORE 3
+ASTORE 2
 ALOAD 0
 GETFIELD local/lito381/server/ClientSession.socket:Ljava/net/Socket;
 CONST Integer:1
@@ -42,31 +38,31 @@
 DUP
 INVOKESPECIAL java/security/SecureRandom.<init>()V
 INVOKEVIRTUAL java/security/SecureRandom.nextInt()I
-ISTORE 4
+ISTORE 3
 NEW local/lito381/server/Protocol$Writer
 DUP
 INVOKESPECIAL local/lito381/server/Protocol$Writer.<init>()V
 CONST Integer:1
 INVOKEVIRTUAL local/lito381/server/Protocol$Writer.c(I)Llocal/lito381/server/Protocol$Writer;
-ILOAD 4
+ILOAD 3
 INVOKESTATIC java/lang/Integer.toUnsignedLong(I)J
 INVOKEVIRTUAL local/lito381/server/Protocol$Writer.d(J)Llocal/lito381/server/Protocol$Writer;
-ASTORE 5
-ALOAD 0
-ALOAD 5
+ASTORE 4
+ALOAD 0
+ALOAD 4
 INVOKEVIRTUAL local/lito381/server/Protocol$Writer.bytes()[B
 CONST str:"S_KEY"
 INVOKESPECIAL local/lito381/server/ClientSession.sendPlain([BLjava/lang/String;)V
 ALOAD 0
 NEW local/lito381/server/Protocol$CipherState
 DUP
-ILOAD 4
+ILOAD 3
 INVOKESPECIAL local/lito381/server/Protocol$CipherState.<init>(I)V
 PUTFIELD local/lito381/server/ClientSession.incoming:Llocal/lito381/server/Protocol$CipherState;
 ALOAD 0
 NEW local/lito381/server/Protocol$CipherState
 DUP
-ILOAD 4
+ILOAD 3
 INVOKESPECIAL local/lito381/server/Protocol$CipherState.<init>(I)V
 PUTFIELD local/lito381/server/ClientSession.outgoing:Llocal/lito381/server/Protocol$CipherState;
 ALOAD 0
@@ -77,226 +73,523 @@
 INVOKEVIRTUAL local/lito381/server/ServerConfig.maxFrame()I
 ALOAD 0
 INVOKESTATIC local/lito381/server/NativeCastTimeline.readFrame(Ljava/io/InputStream;ILlocal/lito381/server/ClientSession;)[B
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
 ALOAD 0
 GETFIELD local/lito381/server/ClientSession.incoming:Llocal/lito381/server/Protocol$CipherState;
-ALOAD 6
+ALOAD 5
 INVOKEVIRTUAL local/lito381/server/Protocol$CipherState.decrypt([B)[B
-ASTORE 7
+ASTORE 36
 ALOAD 0
 GETFIELD local/lito381/server/ClientSession.capture:Llocal/lito381/server/CaptureLog;
 CONST str:"client_to_server"
-ALOAD 7
-ALOAD 6
+ALOAD 36
+ALOAD 5
 ARRAYLENGTH
 CONST Integer:2
 IADD
-ALOAD 7
+ALOAD 36
 INVOKESTATIC local/lito381/server/ClientSession.packetNote([B)Ljava/lang/String;
 INVOKEVIRTUAL local/lito381/server/CaptureLog.frame(Ljava/lang/String;[BILjava/lang/String;)V
 ALOAD 0
-ALOAD 7
+ALOAD 36
 INVOKESPECIAL local/lito381/server/ClientSession.handle([B)V
 ALOAD 0
 GETFIELD local/lito381/server/ClientSession.quitRequested:Z
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
 ALOAD 0
 INVOKESTATIC local/lito381/server/NativeServerLifetime.closeSessionSocket(Ljava/net/Socket;Llocal/lito381/server/ClientSession;)V
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
 ALOAD 0
 INVOKESTATIC local/lito381/server/NativeServerLifetime.awaitCleanup(Llocal/lito381/server/ClientSession;)V
 ALOAD 0
 GETFIELD local/lito381/server/ClientSession.currentRoomId:J
-LSTORE 10/11
+LSTORE 37/38
 ALOAD 0
 GETFIELD local/lito381/server/ClientSession.phase:Llocal/lito381/server/ClientSession$Phase;
 GETSTATIC local/lito381/server/ClientSession$Phase.ROOM:Llocal/lito381/server/ClientSession$Phase;
-IF_ACMPNE L145
-LLOAD 10/11
+IF_ACMPNE L438
+LLOAD 37/38
 CONST Long:0
 LCMP
-IFEQ L145
+IFEQ L438
 ALOAD 0
 GETFIELD local/lito381/server/ClientSession.hub:Llocal/lito381/server/GameHub;
-LLOAD 10/11
+LLOAD 37/38
 ALOAD 0
 INVOKEVIRTUAL local/lito381/server/GameHub.isOwner(JLlocal/lito381/server/ClientSession;)Z
-IFEQ L145
+IFEQ L438
 CONST Integer:1
-GOTO L146
-CONST Integer:0
-ISTORE 12
+GOTO L439
+CONST Integer:0
+ISTORE 39
 ALOAD 0
 CONST str:"disconnect"
 CONST Integer:0
 INVOKESPECIAL local/lito381/server/ClientSession.cancelActivePlayerTrade(Ljava/lang/String;Z)V
-GOTO L158
-ASTORE 13
-GETSTATIC java/lang/System.out:Ljava/io/PrintStream;
-ALOAD 13
+GOTO L451
+ASTORE 40
+GETSTATIC java/lang/System.out:Ljava/io/PrintStream;
+ALOAD 40
 INVOKEVIRTUAL java/io/IOException.getMessage()Ljava/lang/String;
 INVOKEDYNAMIC makeConcatWithConstants(Ljava/lang/String;)Ljava/lang/String; bsm=H6 java/lang/invoke/StringConcatFactory.makeConcatWithConstants(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;Ljava/lang/String;[Ljava/lang/Object;)Ljava/lang/invoke/CallSite; args=[str:"[LiTo] trade disconnect cleanup failed: \u0001"]
 INVOKEVIRTUAL java/io/PrintStream.println(Ljava/lang/String;)V
 ALOAD 0
 GETFIELD local/lito381/server/ClientSession.classicPartyParticipant:Z
-IFEQ L172
+IFEQ L465
 ALOAD 0
 CONST str:"disconnect"
 CONST Integer:0
 INVOKESPECIAL local/lito381/server/ClientSession.leaveClassicParty(Ljava/lang/String;Z)V
-GOTO L172
-ASTORE 14
-GETSTATIC java/lang/System.out:Ljava/io/PrintStream;
-ALOAD 14
+GOTO L465
+ASTORE 41
+GETSTATIC java/lang/System.out:Ljava/io/PrintStream;
+ALOAD 41
 INVOKEVIRTUAL java/io/IOException.getMessage()Ljava/lang/String;
 INVOKEDYNAMIC makeConcatWithConstants(Ljava/lang/String;)Ljava/lang/String; bsm=H6 java/lang/invoke/StringConcatFactory.makeConcatWithConstants(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;Ljava/lang/String;[Ljava/lang/Object;)Ljava/lang/invoke/CallSite; args=[str:"[LiTo] party disconnect cleanup failed: \u0001"]
 INVOKEVIRTUAL java/io/PrintStream.println(Ljava/lang/String;)V
 ALOAD 0
 GETFIELD local/lito381/server/ClientSession.phase:Llocal/lito381/server/ClientSession$Phase;
 GETSTATIC local/lito381/server/ClientSession$Phase.WORLD:Llocal/lito381/server/ClientSession$Phase;
-IF_ACMPNE L194
+IF_ACMPNE L487
 ALOAD 0
 INVOKESPECIAL local/lito381/server/ClientSession.collectClassicWorldPetsOnDisconnect()V
-GOTO L185
-ASTORE 15
-GETSTATIC java/lang/System.out:Ljava/io/PrintStream;
-ALOAD 15
+GOTO L478
+ASTORE 42
+GETSTATIC java/lang/System.out:Ljava/io/PrintStream;
+ALOAD 42
 INVOKEVIRTUAL java/lang/Exception.getMessage()Ljava/lang/String;
 INVOKEDYNAMIC makeConcatWithConstants(Ljava/lang/String;)Ljava/lang/String; bsm=H6 java/lang/invoke/StringConcatFactory.makeConcatWithConstants(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;Ljava/lang/String;[Ljava/lang/Object;)Ljava/lang/invoke/CallSite; args=[str:"[LiTo] persistent pet disconnect cleanup failed: \u0001"]
 INVOKEVIRTUAL java/io/PrintStream.println(Ljava/lang/String;)V
 ALOAD 0
 INVOKESPECIAL local/lito381/server/ClientSession.cleanupClassicWorldTransientCompanionsOnDisconnect()V
-GOTO L194
-ASTORE 16
-GETSTATIC java/lang/System.out:Ljava/io/PrintStream;
-ALOAD 16
+GOTO L487
+ASTORE 43
+GETSTATIC java/lang/System.out:Ljava/io/PrintStream;
+ALOAD 43
 INVOKEVIRTUAL java/lang/Exception.getMessage()Ljava/lang/String;
 INVOKEDYNAMIC makeConcatWithConstants(Ljava/lang/String;)Ljava/lang/String; bsm=H6 java/lang/invoke/StringConcatFactory.makeConcatWithConstants(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;Ljava/lang/String;[Ljava/lang/Object;)Ljava/lang/invoke/CallSite; args=[str:"[LiTo] transient companion disconnect cleanup failed: \u0001"]
 INVOKEVIRTUAL java/io/PrintStream.println(Ljava/lang/String;)V
 ALOAD 0
 INVOKESPECIAL local/lito381/server/ClientSession.persistExperienceGemOnDisconnect()V
-GOTO L203
-ASTORE 17
-GETSTATIC java/lang/System.out:Ljava/io/PrintStream;
-ALOAD 17
+GOTO L496
+ASTORE 44
+GETSTATIC java/lang/System.out:Ljava/io/PrintStream;
+ALOAD 44
 INVOKEVIRTUAL java/lang/Exception.getMessage()Ljava/lang/String;
 INVOKEDYNAMIC makeConcatWithConstants(Ljava/lang/String;)Ljava/lang/String; bsm=H6 java/lang/invoke/StringConcatFactory.makeConcatWithConstants(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;Ljava/lang/String;[Ljava/lang/Object;)Ljava/lang/invoke/CallSite; args=[str:"[LiTo] Dragon Gem disconnect persistence failed: \u0001"]
 INVOKEVIRTUAL java/io/PrintStream.println(Ljava/lang/String;)V
 ALOAD 0
 INVOKESPECIAL local/lito381/server/ClientSession.persistFireWeaponOnDisconnect()V
-GOTO L212
-ASTORE 18
-GETSTATIC java/lang/System.out:Ljava/io/PrintStream;
-ALOAD 18
+GOTO L505
+ASTORE 45
+GETSTATIC java/lang/System.out:Ljava/io/PrintStream;
+ALOAD 45
 INVOKEVIRTUAL java/lang/Exception.getMessage()Ljava/lang/String;
 INVOKEDYNAMIC makeConcatWithConstants(Ljava/lang/String;)Ljava/lang/String; bsm=H6 java/lang/invoke/StringConcatFactory.makeConcatWithConstants(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;Ljava/lang/String;[Ljava/lang/Object;)Ljava/lang/invoke/CallSite; args=[str:"[LiTo] Fire Weapon disconnect persistence failed: \u0001"]
 INVOKEVIRTUAL java/io/PrintStream.println(Ljava/lang/String;)V
 ALOAD 0
 INVOKESPECIAL local/lito381/server/ClientSession.persistBurningWeaponOnDisconnect()V
-GOTO L221
-ASTORE 19
-GETSTATIC java/lang/System.out:Ljava/io/PrintStream;
-ALOAD 19
+GOTO L514
+ASTORE 46
+GETSTATIC java/lang/System.out:Ljava/io/PrintStream;
+ALOAD 46
 INVOKEVIRTUAL java/lang/Exception.getMessage()Ljava/lang/String;
 INVOKEDYNAMIC makeConcatWithConstants(Ljava/lang/String;)Ljava/lang/String; bsm=H6 java/lang/invoke/StringConcatFactory.makeConcatWithConstants(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;Ljava/lang/String;[Ljava/lang/Object;)Ljava/lang/invoke/CallSite; args=[str:"[LiTo] Burning Weapon disconnect persistence failed: \u0001"]
 INVOKEVIRTUAL java/io/PrintStream.println(Ljava/lang/String;)V
 ALOAD 0
 INVOKESPECIAL local/lito381/server/ClientSession.persistWindShotOnDisconnect()V
-GOTO L230
-ASTORE 20
-GETSTATIC java/lang/System.out:Ljava/io/PrintStream;
-ALOAD 20
+GOTO L523
+ASTORE 47
+GETSTATIC java/lang/System.out:Ljava/io/PrintStream;
+ALOAD 47
 INVOKEVIRTUAL java/lang/Exception.getMessage()Ljava/lang/String;
 INVOKEDYNAMIC makeConcatWithConstants(Ljava/lang/String;)Ljava/lang/String; bsm=H6 java/lang/invoke/StringConcatFactory.makeConcatWithConstants(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;Ljava/lang/String;[Ljava/lang/Object;)Ljava/lang/invoke/CallSite; args=[str:"[LiTo] Wind Shot disconnect persistence failed: \u0001"]
 INVOKEVIRTUAL java/io/PrintStream.println(Ljava/lang/String;)V
 ALOAD 0
 INVOKESPECIAL local/lito381/server/ClientSession.persistFireBlessOnDisconnect()V
-GOTO L239
-ASTORE 21
-GETSTATIC java/lang/System.out:Ljava/io/PrintStream;
-ALOAD 21
+GOTO L532
+ASTORE 48
+GETSTATIC java/lang/System.out:Ljava/io/PrintStream;
+ALOAD 48
 INVOKEVIRTUAL java/lang/Exception.getMessage()Ljava/lang/String;
 INVOKEDYNAMIC makeConcatWithConstants(Ljava/lang/String;)Ljava/lang/String; bsm=H6 java/lang/invoke/StringConcatFactory.makeConcatWithConstants(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;Ljava/lang/String;[Ljava/lang/Object;)Ljava/lang/invoke/CallSite; args=[str:"[LiTo] Fire Bless disconnect persistence failed: \u0001"]
 INVOKEVIRTUAL java/io/PrintStream.println(Ljava/lang/String;)V
 ALOAD 0
 INVOKESPECIAL local/lito381/server/ClientSession.persistStormEyeOnDisconnect()V
-GOTO L248
-ASTORE 22
-GETSTATIC java/lang/System.out:Ljava/io/PrintStream;
-ALOAD 22
+GOTO L541
+ASTORE 49
+GETSTATIC java/lang/System.out:Ljava/io/PrintStream;
+ALOAD 49
 INVOKEVIRTUAL java/lang/Exception.getMessage()Ljava/lang/String;
 INVOKEDYNAMIC makeConcatWithConstants(Ljava/lang/String;)Ljava/lang/String; bsm=H6 java/lang/invoke/StringConcatFactory.makeConcatWithConstants(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;Ljava/lang/String;[Ljava/lang/Object;)Ljava/lang/invoke/CallSite; args=[str:"[LiTo] Storm Eye disconnect persistence failed: \u0001"]
 INVOKEVIRTUAL java/io/PrintStream.println(Ljava/lang/String;)V
 ALOAD 0
 INVOKESPECIAL local/lito381/server/ClientSession.persistStormShotOnDisconnect()V
-GOTO L257
-ASTORE 23
-GETSTATIC java/lang/System.out:Ljava/io/PrintStream;
-ALOAD 23
+GOTO L550
+ASTORE 50
+GETSTATIC java/lang/System.out:Ljava/io/PrintStream;
+ALOAD 50
 INVOKEVIRTUAL java/lang/Exception.getMessage()Ljava/lang/String;
 INVOKEDYNAMIC makeConcatWithConstants(Ljava/lang/String;)Ljava/lang/String; bsm=H6 java/lang/invoke/StringConcatFactory.makeConcatWithConstants(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;Ljava/lang/String;[Ljava/lang/Object;)Ljava/lang/invoke/CallSite; args=[str:"[LiTo] Storm Shot disconnect persistence failed: \u0001"]
 INVOKEVIRTUAL java/io/PrintStream.println(Ljava/lang/String;)V
 ALOAD 0
 INVOKESPECIAL local/lito381/server/ClientSession.persistEarthBlessOnDisconnect()V
-GOTO L266
-ASTORE 24
-GETSTATIC java/lang/System.out:Ljava/io/PrintStream;
-ALOAD 24
+GOTO L559
+ASTORE 51
+GETSTATIC java/lang/System.out:Ljava/io/PrintStream;
+ALOAD 51
 INVOKEVIRTUAL java/lang/Exception.getMessage()Ljava/lang/String;
 INVOKEDYNAMIC makeConcatWithConstants(Ljava/lang/String;)Ljava/lang/String; bsm=H6 java/lang/invoke/StringConcatFactory.makeConcatWithConstants(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;Ljava/lang/String;[Ljava/lang/Object;)Ljava/lang/invoke/CallSite; args=[str:"[LiTo] Earth Bless disconnect persistence failed: \u0001"]
 INVOKEVIRTUAL java/io/PrintStream.println(Ljava/lang/String;)V
 ALOAD 0
 INVOKESPECIAL local/lito381/server/ClientSession.persistIronSkinOnDisconnect()V
-GOTO L275
-ASTORE 25
-GETSTATIC java/lang/System.out:Ljava/io/PrintStream;
-ALOAD 25
+GOTO L568
+ASTORE 52
+GETSTATIC java/lang/System.out:Ljava/io/PrintStream;
+ALOAD 52
 INVOKEVIRTUAL java/lang/Exception.getMessage()Ljava/lang/String;
 INVOKEDYNAMIC makeConcatWithConstants(Ljava/lang/String;)Ljava/lang/String; bsm=H6 java/lang/invoke/StringConcatFactory.makeConcatWithConstants(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;Ljava/lang/String;[Ljava/lang/Object;)Ljava/lang/invoke/CallSite; args=[str:"[LiTo] Iron Skin disconnect persistence failed: \u0001"]
 INVOKEVIRTUAL java/io/PrintStream.println(Ljava/lang/String;)V
 ALOAD 0
 INVOKESPECIAL local/lito381/server/ClientSession.persistMovementBuffOnDisconnect()V
-GOTO L284
-ASTORE 26
-GETSTATIC java/lang/System.out:Ljava/io/PrintStream;
-ALOAD 26
+GOTO L577
+ASTORE 53
+GETSTATIC java/lang/System.out:Ljava/io/PrintStream;
+ALOAD 53
 INVOKEVIRTUAL java/lang/Exception.getMessage()Ljava/lang/String;
 INVOKEDYNAMIC makeConcatWithConstants(Ljava/lang/String;)Ljava/lang/String; bsm=H6 java/lang/invoke/StringConcatFactory.makeConcatWithConstants(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;Ljava/lang/String;[Ljava/lang/Object;)Ljava/lang/invoke/CallSite; args=[str:"[LiTo] movement buff disconnect persistence failed: \u0001"]
 INVOKEVIRTUAL java/io/PrintStream.println(Ljava/lang/String;)V
 ALOAD 0
 INVOKESPECIAL local/lito381/server/ClientSession.persistArmorProtectionOnDisconnect()V
-GOTO L293
-ASTORE 27
-GETSTATIC java/lang/System.out:Ljava/io/PrintStream;
-ALOAD 27
+GOTO L586
+ASTORE 54
+GETSTATIC java/lang/System.out:Ljava/io/PrintStream;
+ALOAD 54
 INVOKEVIRTUAL java/lang/Exception.getMessage()Ljava/lang/String;
 INVOKEDYNAMIC makeConcatWithConstants(Ljava/lang/String;)Ljava/lang/String; bsm=H6 java/lang/invoke/StringConcatFactory.makeConcatWithConstants(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;Ljava/lang/String;[Ljava/lang/Object;)Ljava/lang/invoke/CallSite; args=[str:"[LiTo] armor protection disconnect persistence failed: \u0001"]
 INVOKEVIRTUAL java/io/PrintStream.println(Ljava/lang/String;)V
 ALOAD 0
 GETFIELD local/lito381/server/ClientSession.phase:Llocal/lito381/server/ClientSession$Phase;
 GETSTATIC local/lito381/server/ClientSession$Phase.WORLD:Llocal/lito381/server/ClientSession$Phase;
-IF_ACMPNE L313
+IF_ACMPNE L606
 ALOAD 0
 CONST str:"owner-disconnect"
 INVOKESPECIAL local/lito381/server/ClientSession.abandonClassicWorldRescueFollower(Ljava/lang/String;)V
@@ -306,55 +599,55 @@
 ALOAD 0
 CONST str:"owner-disconnect"
 INVOKESPECIAL local/lito381/server/ClientSession.abandonClassicWorldRoiFollower(Ljava/lang/String;)V
-GOTO L313
-ASTORE 28
-GETSTATIC java/lang/System.out:Ljava/io/PrintStream;
-ALOAD 28
+GOTO L606
+ASTORE 55
+GETSTATIC java/lang/System.out:Ljava/io/PrintStream;
+ALOAD 55
 INVOKEVIRTUAL java/lang/Exception.getMessage()Ljava/lang/String;
 INVOKEDYNAMIC makeConcatWithConstants(Ljava/lang/String;)Ljava/lang/String; bsm=H6 java/lang/invoke/StringConcatFactory.makeConcatWithConstants(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;Ljava/lang/String;[Ljava/lang/Object;)Ljava/lang/invoke/CallSite; args=[str:"[LiTo] quest follower disconnect cleanup failed: \u0001"]
 INVOKEVIRTUAL java/io/PrintStream.println(Ljava/lang/String;)V
 ALOAD 0
 GETFIELD local/lito381/server/ClientSession.activeMagicDollObjectIds:Ljava/util/Set;
 DUP
-ASTORE 29
+ASTORE 56
 MONITORENTER
 ALOAD 0
 CONST str:"owner-disconnect"
 CONST Integer:0
 INVOKESPECIAL local/lito381/server/ClientSession.recallNativeMagicDollLocked(Ljava/lang/String;Z)V
-ALOAD 29
+ALOAD 56
 MONITOREXIT
-GOTO L330
-ASTORE 30
-ALOAD 29
+GOTO L623
+ASTORE 57
+ALOAD 56
 MONITOREXIT
-ALOAD 30
+ALOAD 57
 ATHROW
-GOTO L337
-ASTORE 31
-GETSTATIC java/lang/System.out:Ljava/io/PrintStream;
-ALOAD 31
+GOTO L630
+ASTORE 58
+GETSTATIC java/lang/System.out:Ljava/io/PrintStream;
+ALOAD 58
 INVOKEVIRTUAL java/lang/Exception.getMessage()Ljava/lang/String;
 INVOKEDYNAMIC makeConcatWithConstants(Ljava/lang/String;)Ljava/lang/String; bsm=H6 java/lang/invoke/StringConcatFactory.makeConcatWithConstants(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;Ljava/lang/String;[Ljava/lang/Object;)Ljava/lang/invoke/CallSite; args=[str:"[LiTo] magic-doll disconnect cleanup failed: \u0001"]
 INVOKEVIRTUAL java/io/PrintStream.println(Ljava/lang/String;)V
 ALOAD 0
 INVOKESPECIAL local/lito381/server/ClientSession.persistLitoCharacterOnDisconnect()V
-GOTO L349
-ASTORE 32
-GETSTATIC java/lang/System.out:Ljava/io/PrintStream;
-ALOAD 32
+GOTO L642
+ASTORE 59
+GETSTATIC java/lang/System.out:Ljava/io/PrintStream;
+ALOAD 59
 INVOKEVIRTUAL java/lang/Object.getClass()Ljava/lang/Class;
 INVOKEVIRTUAL java/lang/Class.getSimpleName()Ljava/lang/String;
-ALOAD 32
+ALOAD 59
 INVOKEVIRTUAL java/lang/Exception.getMessage()Ljava/lang/String;
 INVOKEDYNAMIC makeConcatWithConstants(Ljava/lang/String;Ljava/lang/String;)Ljava/lang/String; bsm=H6 java/lang/invoke/StringConcatFactory.makeConcatWithConstants(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;Ljava/lang/String;[Ljava/lang/Object;)Ljava/lang/invoke/CallSite; args=[str:"[LiTo] MySQL character disconnect persistence failed: \u0001: \u0001"]
 INVOKEVIRTUAL java/io/PrintStream.println(Ljava/lang/String;)V
 ALOAD 0
 INVOKESPECIAL local/lito381/server/ClientSession.notifyPeersOfDisconnect()V
-GOTO L358
-ASTORE 33
-GETSTATIC java/lang/System.out:Ljava/io/PrintStream;
-ALOAD 33
+GOTO L651
+ASTORE 60
+GETSTATIC java/lang/System.out:Ljava/io/PrintStream;
+ALOAD 60
 INVOKEVIRTUAL java/io/IOException.getMessage()Ljava/lang/String;
 INVOKEDYNAMIC makeConcatWithConstants(Ljava/lang/String;)Ljava/lang/String; bsm=H6 java/lang/invoke/StringConcatFactory.makeConcatWithConstants(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;Ljava/lang/String;[Ljava/lang/Object;)Ljava/lang/invoke/CallSite; args=[str:"[LiTo] disconnect broadcast failed: \u0001"]
 INVOKEVIRTUAL java/io/PrintStream.println(Ljava/lang/String;)V
@@ -373,7 +666,7 @@
 ALOAD 0
 GETFIELD local/lito381/server/ClientSession.selectedCharacter:Ljava/lang/String;
 INVOKEVIRTUAL java/lang/String.isBlank()Z
-IFNE L388
+IFNE L681
 ALOAD 0
 GETFIELD local/lito381/server/ClientSession.state:Llocal/lito381/server/GameStateStore;
 ALOAD 0
@@ -392,57 +685,71 @@
 GETFIELD local/lito381/server/ClientSession.hub:Llocal/lito381/server/GameHub;
 ALOAD 0
 INVOKEVIRTUAL local/lito381/server/GameHub.disconnect(Llocal/lito381/server/ClientSession;)Ljava/util/List;
-ASTORE 34
 ALOAD 0
 INVOKESTATIC local/lito381/server/NativeRoomRules.restoreBot(Llocal/lito381/server/ClientSession;)V
-ILOAD 12
-IFEQ L409
-ALOAD 0
-LLOAD 10/11
+ASTORE 61
+ILOAD 39
+IFEQ L702
+ALOAD 0
+LLOAD 37/38
 CONST str:"disconnect"
 INVOKESPECIAL local/lito381/server/ClientSession.broadcastRoomOwner(JLjava/lang/String;)V
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
 INVOKEVIRTUAL java/io/IOException.getMessage()Ljava/lang/String;
 INVOKEDYNAMIC makeConcatWithConstants(JLjava/lang/String;)Ljava/lang/String; bsm=H6 java/lang/invoke/StringConcatFactory.makeConcatWithConstants(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;Ljava/lang/String;[Ljava/lang/Object;)Ljava/lang/invoke/CallSite; args=[str:"[LiTo] room-owner broadcast failed room=\u0001: \u0001"]
 INVOKEVIRTUAL java/io/PrintStream.println(Ljava/lang/String;)V
-ALOAD 34
+ALOAD 61
 INVOKEINTERFACE java/util/List.iterator()Ljava/util/Iterator; itf
-ASTORE 36
-ALOAD 36
+ASTORE 63
+ALOAD 63
 INVOKEINTERFACE java/util/Iterator.hasNext()Z itf
-IFEQ L432
-ALOAD 36
+IFEQ L725
+ALOAD 63
 INVOKEINTERFACE java/util/Iterator.next()Ljava/lang/Object; itf
 CHECKCAST java/lang/Long
 INVOKEVIRTUAL java/lang/Long.longValue()J
-LSTORE 37/38
-ALOAD 0
-LLOAD 37/38
+LSTORE 64/65
+ALOAD 0
+LLOAD 64/65
 INVOKESPECIAL local/lito381/server/ClientSession.broadcastRoomDeleted(J)V
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
 INVOKEVIRTUAL java/io/IOException.getMessage()Ljava/lang/String;
 INVOKEDYNAMIC makeConcatWithConstants(JLjava/lang/String;)Ljava/lang/String; bsm=H6 java/lang/invoke/StringConcatFactory.makeConcatWithConstants(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;Ljava/lang/String;[Ljava/lang/Object;)Ljava/lang/invoke/CallSite; args=[str:"[LiTo] room-delete broadcast failed room=\u0001: \u0001"]
 INVOKEVIRTUAL java/io/PrintStream.println(Ljava/lang/String;)V
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
 GETSTATIC java/lang/System.out:Ljava/io/PrintStream;
 ALOAD 0
 GETFIELD local/lito381/server/ClientSession.endpoint:Llocal/lito381/server/ClientSession$Endpoint;
 INVOKESTATIC java/lang/String.valueOf(Ljava/lang/Object;)Ljava/lang/String;
-ALOAD 2
-ALOAD 40
+ALOAD 1
+ALOAD 69
 INVOKEVIRTUAL java/lang/Object.getClass()Ljava/lang/Class;
 INVOKEVIRTUAL java/lang/Class.getSimpleName()Ljava/lang/String;
-ALOAD 40
+ALOAD 69
 INVOKEVIRTUAL java/lang/Exception.getMessage()Ljava/lang/String;
 INVOKEDYNAMIC makeConcatWithConstants(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;)Ljava/lang/String; bsm=H6 java/lang/invoke/StringConcatFactory.makeConcatWithConstants(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;Ljava/lang/String;[Ljava/lang/Object;)Ljava/lang/invoke/CallSite; args=[str:"[LiTo] session ended endpoint=\u0001 peer=\u0001: \u0001: \u0001"]
 INVOKEVIRTUAL java/io/PrintStream.println(Ljava/lang/String;)V
@@ -450,175 +757,175 @@
 INVOKESTATIC local/lito381/server/NativeServerLifetime.awaitCleanup(Llocal/lito381/server/ClientSession;)V
 ALOAD 0
 GETFIELD local/lito381/server/ClientSession.currentRoomId:J
-LSTORE 41/42
+LSTORE 70/71
 ALOAD 0
 GETFIELD local/lito381/server/ClientSession.phase:Llocal/lito381/server/ClientSession$Phase;
 GETSTATIC local/lito381/server/ClientSession$Phase.ROOM:Llocal/lito381/server/ClientSession$Phase;
-IF_ACMPNE L467
-LLOAD 41/42
+IF_ACMPNE L774
+LLOAD 70/71
 CONST Long:0
 LCMP
-IFEQ L467
+IFEQ L774
 ALOAD 0
 GETFIELD local/lito381/server/ClientSession.hub:Llocal/lito381/server/GameHub;
-LLOAD 41/42
+LLOAD 70/71
 ALOAD 0
 INVOKEVIRTUAL local/lito381/server/GameHub.isOwner(JLlocal/lito381/server/ClientSession;)Z
-IFEQ L467
+IFEQ L774
 CONST Integer:1
-GOTO L468
-CONST Integer:0
-ISTORE 43
+GOTO L775
+CONST Integer:0
+ISTORE 72
 ALOAD 0
 CONST str:"disconnect"
 CONST Integer:0
 INVOKESPECIAL local/lito381/server/ClientSession.cancelActivePlayerTrade(Ljava/lang/String;Z)V
-GOTO L480
-ASTORE 44
-GETSTATIC java/lang/System.out:Ljava/io/PrintStream;
-ALOAD 44
+GOTO L787
+ASTORE 73
+GETSTATIC java/lang/System.out:Ljava/io/PrintStream;
+ALOAD 73
 INVOKEVIRTUAL java/io/IOException.getMessage()Ljava/lang/String;
 INVOKEDYNAMIC makeConcatWithConstants(Ljava/lang/String;)Ljava/lang/String; bsm=H6 java/lang/invoke/StringConcatFactory.makeConcatWithConstants(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;Ljava/lang/String;[Ljava/lang/Object;)Ljava/lang/invoke/CallSite; args=[str:"[LiTo] trade disconnect cleanup failed: \u0001"]
 INVOKEVIRTUAL java/io/PrintStream.println(Ljava/lang/String;)V
 ALOAD 0
 GETFIELD local/lito381/server/ClientSession.classicPartyParticipant:Z
-IFEQ L494
+IFEQ L801
 ALOAD 0
 CONST str:"disconnect"
 CONST Integer:0
 INVOKESPECIAL local/lito381/server/ClientSession.leaveClassicParty(Ljava/lang/String;Z)V
-GOTO L494
-ASTORE 45
-GETSTATIC java/lang/System.out:Ljava/io/PrintStream;
-ALOAD 45
+GOTO L801
+ASTORE 74
+GETSTATIC java/lang/System.out:Ljava/io/PrintStream;
+ALOAD 74
 INVOKEVIRTUAL java/io/IOException.getMessage()Ljava/lang/String;
 INVOKEDYNAMIC makeConcatWithConstants(Ljava/lang/String;)Ljava/lang/String; bsm=H6 java/lang/invoke/StringConcatFactory.makeConcatWithConstants(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;Ljava/lang/String;[Ljava/lang/Object;)Ljava/lang/invoke/CallSite; args=[str:"[LiTo] party disconnect cleanup failed: \u0001"]
 INVOKEVIRTUAL java/io/PrintStream.println(Ljava/lang/String;)V
 ALOAD 0
 GETFIELD local/lito381/server/ClientSession.phase:Llocal/lito381/server/ClientSession$Phase;
 GETSTATIC local/lito381/server/ClientSession$Phase.WORLD:Llocal/lito381/server/ClientSession$Phase;
-IF_ACMPNE L516
+IF_ACMPNE L823
 ALOAD 0
 INVOKESPECIAL local/lito381/server/ClientSession.collectClassicWorldPetsOnDisconnect()V
-GOTO L507
-ASTORE 46
-GETSTATIC java/lang/System.out:Ljava/io/PrintStream;
-ALOAD 46
+GOTO L814
+ASTORE 75
+GETSTATIC java/lang/System.out:Ljava/io/PrintStream;
+ALOAD 75
 INVOKEVIRTUAL java/lang/Exception.getMessage()Ljava/lang/String;
 INVOKEDYNAMIC makeConcatWithConstants(Ljava/lang/String;)Ljava/lang/String; bsm=H6 java/lang/invoke/StringConcatFactory.makeConcatWithConstants(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;Ljava/lang/String;[Ljava/lang/Object;)Ljava/lang/invoke/CallSite; args=[str:"[LiTo] persistent pet disconnect cleanup failed: \u0001"]
 INVOKEVIRTUAL java/io/PrintStream.println(Ljava/lang/String;)V
 ALOAD 0
 INVOKESPECIAL local/lito381/server/ClientSession.cleanupClassicWorldTransientCompanionsOnDisconnect()V
-GOTO L516
-ASTORE 47
-GETSTATIC java/lang/System.out:Ljava/io/PrintStream;
-ALOAD 47
+GOTO L823
+ASTORE 76
+GETSTATIC java/lang/System.out:Ljava/io/PrintStream;
+ALOAD 76
 INVOKEVIRTUAL java/lang/Exception.getMessage()Ljava/lang/String;
 INVOKEDYNAMIC makeConcatWithConstants(Ljava/lang/String;)Ljava/lang/String; bsm=H6 java/lang/invoke/StringConcatFactory.makeConcatWithConstants(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;Ljava/lang/String;[Ljava/lang/Object;)Ljava/lang/invoke/CallSite; args=[str:"[LiTo] transient companion disconnect cleanup failed: \u0001"]
 INVOKEVIRTUAL java/io/PrintStream.println(Ljava/lang/String;)V
 ALOAD 0
 INVOKESPECIAL local/lito381/server/ClientSession.persistExperienceGemOnDisconnect()V
-GOTO L525
-ASTORE 48
-GETSTATIC java/lang/System.out:Ljava/io/PrintStream;
-ALOAD 48
+GOTO L832
+ASTORE 77
+GETSTATIC java/lang/System.out:Ljava/io/PrintStream;
+ALOAD 77
 INVOKEVIRTUAL java/lang/Exception.getMessage()Ljava/lang/String;
 INVOKEDYNAMIC makeConcatWithConstants(Ljava/lang/String;)Ljava/lang/String; bsm=H6 java/lang/invoke/StringConcatFactory.makeConcatWithConstants(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;Ljava/lang/String;[Ljava/lang/Object;)Ljava/lang/invoke/CallSite; args=[str:"[LiTo] Dragon Gem disconnect persistence failed: \u0001"]
 INVOKEVIRTUAL java/io/PrintStream.println(Ljava/lang/String;)V
 ALOAD 0
 INVOKESPECIAL local/lito381/server/ClientSession.persistFireWeaponOnDisconnect()V
-GOTO L534
-ASTORE 49
-GETSTATIC java/lang/System.out:Ljava/io/PrintStream;
-ALOAD 49
+GOTO L841
+ASTORE 78
+GETSTATIC java/lang/System.out:Ljava/io/PrintStream;
+ALOAD 78
 INVOKEVIRTUAL java/lang/Exception.getMessage()Ljava/lang/String;
 INVOKEDYNAMIC makeConcatWithConstants(Ljava/lang/String;)Ljava/lang/String; bsm=H6 java/lang/invoke/StringConcatFactory.makeConcatWithConstants(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;Ljava/lang/String;[Ljava/lang/Object;)Ljava/lang/invoke/CallSite; args=[str:"[LiTo] Fire Weapon disconnect persistence failed: \u0001"]
 INVOKEVIRTUAL java/io/PrintStream.println(Ljava/lang/String;)V
 ALOAD 0
 INVOKESPECIAL local/lito381/server/ClientSession.persistBurningWeaponOnDisconnect()V
-GOTO L543
-ASTORE 50
-GETSTATIC java/lang/System.out:Ljava/io/PrintStream;
-ALOAD 50
+GOTO L850
+ASTORE 79
+GETSTATIC java/lang/System.out:Ljava/io/PrintStream;
+ALOAD 79
 INVOKEVIRTUAL java/lang/Exception.getMessage()Ljava/lang/String;
 INVOKEDYNAMIC makeConcatWithConstants(Ljava/lang/String;)Ljava/lang/String; bsm=H6 java/lang/invoke/StringConcatFactory.makeConcatWithConstants(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;Ljava/lang/String;[Ljava/lang/Object;)Ljava/lang/invoke/CallSite; args=[str:"[LiTo] Burning Weapon disconnect persistence failed: \u0001"]
 INVOKEVIRTUAL java/io/PrintStream.println(Ljava/lang/String;)V
 ALOAD 0
 INVOKESPECIAL local/lito381/server/ClientSession.persistWindShotOnDisconnect()V
-GOTO L552
-ASTORE 51
-GETSTATIC java/lang/System.out:Ljava/io/PrintStream;
-ALOAD 51
+GOTO L859
+ASTORE 80
+GETSTATIC java/lang/System.out:Ljava/io/PrintStream;
+ALOAD 80
 INVOKEVIRTUAL java/lang/Exception.getMessage()Ljava/lang/String;
 INVOKEDYNAMIC makeConcatWithConstants(Ljava/lang/String;)Ljava/lang/String; bsm=H6 java/lang/invoke/StringConcatFactory.makeConcatWithConstants(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;Ljava/lang/String;[Ljava/lang/Object;)Ljava/lang/invoke/CallSite; args=[str:"[LiTo] Wind Shot disconnect persistence failed: \u0001"]
 INVOKEVIRTUAL java/io/PrintStream.println(Ljava/lang/String;)V
 ALOAD 0
 INVOKESPECIAL local/lito381/server/ClientSession.persistFireBlessOnDisconnect()V
-GOTO L561
-ASTORE 52
-GETSTATIC java/lang/System.out:Ljava/io/PrintStream;
-ALOAD 52
+GOTO L868
+ASTORE 81
+GETSTATIC java/lang/System.out:Ljava/io/PrintStream;
+ALOAD 81
 INVOKEVIRTUAL java/lang/Exception.getMessage()Ljava/lang/String;
 INVOKEDYNAMIC makeConcatWithConstants(Ljava/lang/String;)Ljava/lang/String; bsm=H6 java/lang/invoke/StringConcatFactory.makeConcatWithConstants(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;Ljava/lang/String;[Ljava/lang/Object;)Ljava/lang/invoke/CallSite; args=[str:"[LiTo] Fire Bless disconnect persistence failed: \u0001"]
 INVOKEVIRTUAL java/io/PrintStream.println(Ljava/lang/String;)V
 ALOAD 0
 INVOKESPECIAL local/lito381/server/ClientSession.persistStormEyeOnDisconnect()V
-GOTO L570
-ASTORE 53
-GETSTATIC java/lang/System.out:Ljava/io/PrintStream;
-ALOAD 53
+GOTO L877
+ASTORE 82
+GETSTATIC java/lang/System.out:Ljava/io/PrintStream;
+ALOAD 82
 INVOKEVIRTUAL java/lang/Exception.getMessage()Ljava/lang/String;
 INVOKEDYNAMIC makeConcatWithConstants(Ljava/lang/String;)Ljava/lang/String; bsm=H6 java/lang/invoke/StringConcatFactory.makeConcatWithConstants(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;Ljava/lang/String;[Ljava/lang/Object;)Ljava/lang/invoke/CallSite; args=[str:"[LiTo] Storm Eye disconnect persistence failed: \u0001"]
 INVOKEVIRTUAL java/io/PrintStream.println(Ljava/lang/String;)V
 ALOAD 0
 INVOKESPECIAL local/lito381/server/ClientSession.persistStormShotOnDisconnect()V
-GOTO L579
-ASTORE 54
-GETSTATIC java/lang/System.out:Ljava/io/PrintStream;
-ALOAD 54
+GOTO L886
+ASTORE 83
+GETSTATIC java/lang/System.out:Ljava/io/PrintStream;
+ALOAD 83
 INVOKEVIRTUAL java/lang/Exception.getMessage()Ljava/lang/String;
 INVOKEDYNAMIC makeConcatWithConstants(Ljava/lang/String;)Ljava/lang/String; bsm=H6 java/lang/invoke/StringConcatFactory.makeConcatWithConstants(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;Ljava/lang/String;[Ljava/lang/Object;)Ljava/lang/invoke/CallSite; args=[str:"[LiTo] Storm Shot disconnect persistence failed: \u0001"]
 INVOKEVIRTUAL java/io/PrintStream.println(Ljava/lang/String;)V
 ALOAD 0
 INVOKESPECIAL local/lito381/server/ClientSession.persistEarthBlessOnDisconnect()V
-GOTO L588
-ASTORE 55
-GETSTATIC java/lang/System.out:Ljava/io/PrintStream;
-ALOAD 55
+GOTO L895
+ASTORE 84
+GETSTATIC java/lang/System.out:Ljava/io/PrintStream;
+ALOAD 84
 INVOKEVIRTUAL java/lang/Exception.getMessage()Ljava/lang/String;
 INVOKEDYNAMIC makeConcatWithConstants(Ljava/lang/String;)Ljava/lang/String; bsm=H6 java/lang/invoke/StringConcatFactory.makeConcatWithConstants(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;Ljava/lang/String;[Ljava/lang/Object;)Ljava/lang/invoke/CallSite; args=[str:"[LiTo] Earth Bless disconnect persistence failed: \u0001"]
 INVOKEVIRTUAL java/io/PrintStream.println(Ljava/lang/String;)V
 ALOAD 0
 INVOKESPECIAL local/lito381/server/ClientSession.persistIronSkinOnDisconnect()V
-GOTO L597
-ASTORE 56
-GETSTATIC java/lang/System.out:Ljava/io/PrintStream;
-ALOAD 56
+GOTO L904
+ASTORE 85
+GETSTATIC java/lang/System.out:Ljava/io/PrintStream;
+ALOAD 85
 INVOKEVIRTUAL java/lang/Exception.getMessage()Ljava/lang/String;
 INVOKEDYNAMIC makeConcatWithConstants(Ljava/lang/String;)Ljava/lang/String; bsm=H6 java/lang/invoke/StringConcatFactory.makeConcatWithConstants(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;Ljava/lang/String;[Ljava/lang/Object;)Ljava/lang/invoke/CallSite; args=[str:"[LiTo] Iron Skin disconnect persistence failed: \u0001"]
 INVOKEVIRTUAL java/io/PrintStream.println(Ljava/lang/String;)V
 ALOAD 0
 INVOKESPECIAL local/lito381/server/ClientSession.persistMovementBuffOnDisconnect()V
-GOTO L606
-ASTORE 57
-GETSTATIC java/lang/System.out:Ljava/io/PrintStream;
-ALOAD 57
+GOTO L913
+ASTORE 86
+GETSTATIC java/lang/System.out:Ljava/io/PrintStream;
+ALOAD 86
 INVOKEVIRTUAL java/lang/Exception.getMessage()Ljava/lang/String;
 INVOKEDYNAMIC makeConcatWithConstants(Ljava/lang/String;)Ljava/lang/String; bsm=H6 java/lang/invoke/StringConcatFactory.makeConcatWithConstants(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;Ljava/lang/String;[Ljava/lang/Object;)Ljava/lang/invoke/CallSite; args=[str:"[LiTo] movement buff disconnect persistence failed: \u0001"]
 INVOKEVIRTUAL java/io/PrintStream.println(Ljava/lang/String;)V
 ALOAD 0
 INVOKESPECIAL local/lito381/server/ClientSession.persistArmorProtectionOnDisconnect()V
-GOTO L615
-ASTORE 58
-GETSTATIC java/lang/System.out:Ljava/io/PrintStream;
-ALOAD 58
+GOTO L922
+ASTORE 87
+GETSTATIC java/lang/System.out:Ljava/io/PrintStream;
+ALOAD 87
 INVOKEVIRTUAL java/lang/Exception.getMessage()Ljava/lang/String;
 INVOKEDYNAMIC makeConcatWithConstants(Ljava/lang/String;)Ljava/lang/String; bsm=H6 java/lang/invoke/StringConcatFactory.makeConcatWithConstants(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;Ljava/lang/String;[Ljava/lang/Object;)Ljava/lang/invoke/CallSite; args=[str:"[LiTo] armor protection disconnect persistence failed: \u0001"]
 INVOKEVIRTUAL java/io/PrintStream.println(Ljava/lang/String;)V
 ALOAD 0
 GETFIELD local/lito381/server/ClientSession.phase:Llocal/lito381/server/ClientSession$Phase;
 GETSTATIC local/lito381/server/ClientSession$Phase.WORLD:Llocal/lito381/server/ClientSession$Phase;
-IF_ACMPNE L635
+IF_ACMPNE L942
 ALOAD 0
 CONST str:"owner-disconnect"
 INVOKESPECIAL local/lito381/server/ClientSession.abandonClassicWorldRescueFollower(Ljava/lang/String;)V
@@ -628,55 +935,55 @@
 ALOAD 0
 CONST str:"owner-disconnect"
 INVOKESPECIAL local/lito381/server/ClientSession.abandonClassicWorldRoiFollower(Ljava/lang/String;)V
-GOTO L635
-ASTORE 59
-GETSTATIC java/lang/System.out:Ljava/io/PrintStream;
-ALOAD 59
+GOTO L942
+ASTORE 88
+GETSTATIC java/lang/System.out:Ljava/io/PrintStream;
+ALOAD 88
 INVOKEVIRTUAL java/lang/Exception.getMessage()Ljava/lang/String;
 INVOKEDYNAMIC makeConcatWithConstants(Ljava/lang/String;)Ljava/lang/String; bsm=H6 java/lang/invoke/StringConcatFactory.makeConcatWithConstants(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;Ljava/lang/String;[Ljava/lang/Object;)Ljava/lang/invoke/CallSite; args=[str:"[LiTo] quest follower disconnect cleanup failed: \u0001"]
 INVOKEVIRTUAL java/io/PrintStream.println(Ljava/lang/String;)V
 ALOAD 0
 GETFIELD local/lito381/server/ClientSession.activeMagicDollObjectIds:Ljava/util/Set;
 DUP
-ASTORE 60
+ASTORE 89
 MONITORENTER
 ALOAD 0
 CONST str:"owner-disconnect"
 CONST Integer:0
 INVOKESPECIAL local/lito381/server/ClientSession.recallNativeMagicDollLocked(Ljava/lang/String;Z)V
-ALOAD 60
+ALOAD 89
 MONITOREXIT
-GOTO L652
-ASTORE 61
-ALOAD 60
+GOTO L959
+ASTORE 90
+ALOAD 89
 MONITOREXIT
-ALOAD 61
+ALOAD 90
 ATHROW
-GOTO L659
-ASTORE 62
-GETSTATIC java/lang/System.out:Ljava/io/PrintStream;
-ALOAD 62
+GOTO L966
+ASTORE 91
+GETSTATIC java/lang/System.out:Ljava/io/PrintStream;
+ALOAD 91
 INVOKEVIRTUAL java/lang/Exception.getMessage()Ljava/lang/String;
 INVOKEDYNAMIC makeConcatWithConstants(Ljava/lang/String;)Ljava/lang/String; bsm=H6 java/lang/invoke/StringConcatFactory.makeConcatWithConstants(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;Ljava/lang/String;[Ljava/lang/Object;)Ljava/lang/invoke/CallSite; args=[str:"[LiTo] magic-doll disconnect cleanup failed: \u0001"]
 INVOKEVIRTUAL java/io/PrintStream.println(Ljava/lang/String;)V
 ALOAD 0
 INVOKESPECIAL local/lito381/server/ClientSession.persistLitoCharacterOnDisconnect()V
-GOTO L671
-ASTORE 63
-GETSTATIC java/lang/System.out:Ljava/io/PrintStream;
-ALOAD 63
+GOTO L978
+ASTORE 92
+GETSTATIC java/lang/System.out:Ljava/io/PrintStream;
+ALOAD 92
 INVOKEVIRTUAL java/lang/Object.getClass()Ljava/lang/Class;
 INVOKEVIRTUAL java/lang/Class.getSimpleName()Ljava/lang/String;
-ALOAD 63
+ALOAD 92
 INVOKEVIRTUAL java/lang/Exception.getMessage()Ljava/lang/String;
 INVOKEDYNAMIC makeConcatWithConstants(Ljava/lang/String;Ljava/lang/String;)Ljava/lang/String; bsm=H6 java/lang/invoke/StringConcatFactory.makeConcatWithConstants(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;Ljava/lang/String;[Ljava/lang/Object;)Ljava/lang/invoke/CallSite; args=[str:"[LiTo] MySQL character disconnect persistence failed: \u0001: \u0001"]
 INVOKEVIRTUAL java/io/PrintStream.println(Ljava/lang/String;)V
 ALOAD 0
 INVOKESPECIAL local/lito381/server/ClientSession.notifyPeersOfDisconnect()V
-GOTO L680
-ASTORE 64
-GETSTATIC java/lang/System.out:Ljava/io/PrintStream;
-ALOAD 64
+GOTO L987
+ASTORE 93
+GETSTATIC java/lang/System.out:Ljava/io/PrintStream;
+ALOAD 93
 INVOKEVIRTUAL java/io/IOException.getMessage()Ljava/lang/String;
 INVOKEDYNAMIC makeConcatWithConstants(Ljava/lang/String;)Ljava/lang/String; bsm=H6 java/lang/invoke/StringConcatFactory.makeConcatWithConstants(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;Ljava/lang/String;[Ljava/lang/Object;)Ljava/lang/invoke/CallSite; args=[str:"[LiTo] disconnect broadcast failed: \u0001"]
 INVOKEVIRTUAL java/io/PrintStream.println(Ljava/lang/String;)V
@@ -695,7 +1002,7 @@
 ALOAD 0
 GETFIELD local/lito381/server/ClientSession.selectedCharacter:Ljava/lang/String;
 INVOKEVIRTUAL java/lang/String.isBlank()Z
-IFNE L710
+IFNE L1017
 ALOAD 0
 GETFIELD local/lito381/server/ClientSession.state:Llocal/lito381/server/GameStateStore;
 ALOAD 0
@@ -714,221 +1021,221 @@
 GETFIELD local/lito381/server/ClientSession.hub:Llocal/lito381/server/GameHub;
 ALOAD 0
 INVOKEVIRTUAL local/lito381/server/GameHub.disconnect(Llocal/lito381/server/ClientSession;)Ljava/util/List;
-ASTORE 65
 ALOAD 0
 INVOKESTATIC local/lito381/server/NativeRoomRules.restoreBot(Llocal/lito381/server/ClientSession;)V
-ILOAD 43
-IFEQ L731
-ALOAD 0
-LLOAD 41/42
+ASTORE 94
+ILOAD 72
+IFEQ L1038
+ALOAD 0
+LLOAD 70/71
 CONST str:"disconnect"
 INVOKESPECIAL local/lito381/server/ClientSession.broadcastRoomOwner(JLjava/lang/String;)V
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
 INVOKEVIRTUAL java/io/IOException.getMessage()Ljava/lang/String;
 INVOKEDYNAMIC makeConcatWithConstants(JLjava/lang/String;)Ljava/lang/String; bsm=H6 java/lang/invoke/StringConcatFactory.makeConcatWithConstants(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;Ljava/lang/String;[Ljava/lang/Object;)Ljava/lang/invoke/CallSite; args=[str:"[LiTo] room-owner broadcast failed room=\u0001: \u0001"]
 INVOKEVIRTUAL java/io/PrintStream.println(Ljava/lang/String;)V
-ALOAD 65
+ALOAD 94
 INVOKEINTERFACE java/util/List.iterator()Ljava/util/Iterator; itf
-ASTORE 67
-ALOAD 67
+ASTORE 96
+ALOAD 96
 INVOKEINTERFACE java/util/Iterator.hasNext()Z itf
-IFEQ L754
-ALOAD 67
+IFEQ L1061
+ALOAD 96
 INVOKEINTERFACE java/util/Iterator.next()Ljava/lang/Object; itf
 CHECKCAST java/lang/Long
 INVOKEVIRTUAL java/lang/Long.longValue()J
-LSTORE 68/69
-ALOAD 0
-LLOAD 68/69
+LSTORE 97/98
+ALOAD 0
+LLOAD 97/98
 INVOKESPECIAL local/lito381/server/ClientSession.broadcastRoomDeleted(J)V
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
 INVOKEVIRTUAL java/io/IOException.getMessage()Ljava/lang/String;
 INVOKEDYNAMIC makeConcatWithConstants(JLjava/lang/String;)Ljava/lang/String; bsm=H6 java/lang/invoke/StringConcatFactory.makeConcatWithConstants(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;Ljava/lang/String;[Ljava/lang/Object;)Ljava/lang/invoke/CallSite; args=[str:"[LiTo] room-delete broadcast failed room=\u0001: \u0001"]
 INVOKEVIRTUAL java/io/PrintStream.println(Ljava/lang/String;)V
-GOTO L734
-GOTO L1066
-ASTORE 71
+GOTO L1041
+GOTO L1373
+ASTORE 100
 ALOAD 0
 INVOKESTATIC local/lito381/server/NativeServerLifetime.awaitCleanup(Llocal/lito381/server/ClientSession;)V
 ALOAD 0
 GETFIELD local/lito381/server/ClientSession.currentRoomId:J
-LSTORE 72/73
+LSTORE 101/102
 ALOAD 0
 GETFIELD local/lito381/server/ClientSession.phase:Llocal/lito381/server/ClientSession$Phase;
 GETSTATIC local/lito381/server/ClientSession$Phase.ROOM:Llocal/lito381/server/ClientSession$Phase;
-IF_ACMPNE L777
-LLOAD 72/73
+IF_ACMPNE L1084
+LLOAD 101/102
 CONST Long:0
 LCMP
-IFEQ L777
+IFEQ L1084
 ALOAD 0
 GETFIELD local/lito381/server/ClientSession.hub:Llocal/lito381/server/GameHub;
-LLOAD 72/73
+LLOAD 101/102
 ALOAD 0
 INVOKEVIRTUAL local/lito381/server/GameHub.isOwner(JLlocal/lito381/server/ClientSession;)Z
-IFEQ L777
+IFEQ L1084
 CONST Integer:1
-GOTO L778
-CONST Integer:0
-ISTORE 74
+GOTO L1085
+CONST Integer:0
+ISTORE 103
 ALOAD 0
 CONST str:"disconnect"
 CONST Integer:0
 INVOKESPECIAL local/lito381/server/ClientSession.cancelActivePlayerTrade(Ljava/lang/String;Z)V
-GOTO L790
-ASTORE 75
-GETSTATIC java/lang/System.out:Ljava/io/PrintStream;
-ALOAD 75
+GOTO L1097
+ASTORE 104
+GETSTATIC java/lang/System.out:Ljava/io/PrintStream;
+ALOAD 104
 INVOKEVIRTUAL java/io/IOException.getMessage()Ljava/lang/String;
 INVOKEDYNAMIC makeConcatWithConstants(Ljava/lang/String;)Ljava/lang/String; bsm=H6 java/lang/invoke/StringConcatFactory.makeConcatWithConstants(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;Ljava/lang/String;[Ljava/lang/Object;)Ljava/lang/invoke/CallSite; args=[str:"[LiTo] trade disconnect cleanup failed: \u0001"]
 INVOKEVIRTUAL java/io/PrintStream.println(Ljava/lang/String;)V
 ALOAD 0
 GETFIELD local/lito381/server/ClientSession.classicPartyParticipant:Z
-IFEQ L804
+IFEQ L1111
 ALOAD 0
 CONST str:"disconnect"
 CONST Integer:0
 INVOKESPECIAL local/lito381/server/ClientSession.leaveClassicParty(Ljava/lang/String;Z)V
-GOTO L804
-ASTORE 76
-GETSTATIC java/lang/System.out:Ljava/io/PrintStream;
-ALOAD 76
+GOTO L1111
+ASTORE 105
+GETSTATIC java/lang/System.out:Ljava/io/PrintStream;
+ALOAD 105
 INVOKEVIRTUAL java/io/IOException.getMessage()Ljava/lang/String;
 INVOKEDYNAMIC makeConcatWithConstants(Ljava/lang/String;)Ljava/lang/String; bsm=H6 java/lang/invoke/StringConcatFactory.makeConcatWithConstants(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;Ljava/lang/String;[Ljava/lang/Object;)Ljava/lang/invoke/CallSite; args=[str:"[LiTo] party disconnect cleanup failed: \u0001"]
 INVOKEVIRTUAL java/io/PrintStream.println(Ljava/lang/String;)V
 ALOAD 0
 GETFIELD local/lito381/server/ClientSession.phase:Llocal/lito381/server/ClientSession$Phase;
 GETSTATIC local/lito381/server/ClientSession$Phase.WORLD:Llocal/lito381/server/ClientSession$Phase;
-IF_ACMPNE L826
+IF_ACMPNE L1133
 ALOAD 0
 INVOKESPECIAL local/lito381/server/ClientSession.collectClassicWorldPetsOnDisconnect()V
-GOTO L817
-ASTORE 77
-GETSTATIC java/lang/System.out:Ljava/io/PrintStream;
-ALOAD 77
+GOTO L1124
+ASTORE 106
+GETSTATIC java/lang/System.out:Ljava/io/PrintStream;
+ALOAD 106
 INVOKEVIRTUAL java/lang/Exception.getMessage()Ljava/lang/String;
 INVOKEDYNAMIC makeConcatWithConstants(Ljava/lang/String;)Ljava/lang/String; bsm=H6 java/lang/invoke/StringConcatFactory.makeConcatWithConstants(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;Ljava/lang/String;[Ljava/lang/Object;)Ljava/lang/invoke/CallSite; args=[str:"[LiTo] persistent pet disconnect cleanup failed: \u0001"]
 INVOKEVIRTUAL java/io/PrintStream.println(Ljava/lang/String;)V
 ALOAD 0
 INVOKESPECIAL local/lito381/server/ClientSession.cleanupClassicWorldTransientCompanionsOnDisconnect()V
-GOTO L826
-ASTORE 78
-GETSTATIC java/lang/System.out:Ljava/io/PrintStream;
-ALOAD 78
+GOTO L1133
+ASTORE 107
+GETSTATIC java/lang/System.out:Ljava/io/PrintStream;
+ALOAD 107
 INVOKEVIRTUAL java/lang/Exception.getMessage()Ljava/lang/String;
 INVOKEDYNAMIC makeConcatWithConstants(Ljava/lang/String;)Ljava/lang/String; bsm=H6 java/lang/invoke/StringConcatFactory.makeConcatWithConstants(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;Ljava/lang/String;[Ljava/lang/Object;)Ljava/lang/invoke/CallSite; args=[str:"[LiTo] transient companion disconnect cleanup failed: \u0001"]
 INVOKEVIRTUAL java/io/PrintStream.println(Ljava/lang/String;)V
 ALOAD 0
 INVOKESPECIAL local/lito381/server/ClientSession.persistExperienceGemOnDisconnect()V
-GOTO L835
-ASTORE 79
-GETSTATIC java/lang/System.out:Ljava/io/PrintStream;
-ALOAD 79
+GOTO L1142
+ASTORE 108
+GETSTATIC java/lang/System.out:Ljava/io/PrintStream;
+ALOAD 108
 INVOKEVIRTUAL java/lang/Exception.getMessage()Ljava/lang/String;
 INVOKEDYNAMIC makeConcatWithConstants(Ljava/lang/String;)Ljava/lang/String; bsm=H6 java/lang/invoke/StringConcatFactory.makeConcatWithConstants(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;Ljava/lang/String;[Ljava/lang/Object;)Ljava/lang/invoke/CallSite; args=[str:"[LiTo] Dragon Gem disconnect persistence failed: \u0001"]
 INVOKEVIRTUAL java/io/PrintStream.println(Ljava/lang/String;)V
 ALOAD 0
 INVOKESPECIAL local/lito381/server/ClientSession.persistFireWeaponOnDisconnect()V
-GOTO L844
-ASTORE 80
-GETSTATIC java/lang/System.out:Ljava/io/PrintStream;
-ALOAD 80
+GOTO L1151
+ASTORE 109
+GETSTATIC java/lang/System.out:Ljava/io/PrintStream;
+ALOAD 109
 INVOKEVIRTUAL java/lang/Exception.getMessage()Ljava/lang/String;
 INVOKEDYNAMIC makeConcatWithConstants(Ljava/lang/String;)Ljava/lang/String; bsm=H6 java/lang/invoke/StringConcatFactory.makeConcatWithConstants(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;Ljava/lang/String;[Ljava/lang/Object;)Ljava/lang/invoke/CallSite; args=[str:"[LiTo] Fire Weapon disconnect persistence failed: \u0001"]
 INVOKEVIRTUAL java/io/PrintStream.println(Ljava/lang/String;)V
 ALOAD 0
 INVOKESPECIAL local/lito381/server/ClientSession.persistBurningWeaponOnDisconnect()V
-GOTO L853
-ASTORE 81
-GETSTATIC java/lang/System.out:Ljava/io/PrintStream;
-ALOAD 81
+GOTO L1160
+ASTORE 110
+GETSTATIC java/lang/System.out:Ljava/io/PrintStream;
+ALOAD 110
 INVOKEVIRTUAL java/lang/Exception.getMessage()Ljava/lang/String;
 INVOKEDYNAMIC makeConcatWithConstants(Ljava/lang/String;)Ljava/lang/String; bsm=H6 java/lang/invoke/StringConcatFactory.makeConcatWithConstants(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;Ljava/lang/String;[Ljava/lang/Object;)Ljava/lang/invoke/CallSite; args=[str:"[LiTo] Burning Weapon disconnect persistence failed: \u0001"]
 INVOKEVIRTUAL java/io/PrintStream.println(Ljava/lang/String;)V
 ALOAD 0
 INVOKESPECIAL local/lito381/server/ClientSession.persistWindShotOnDisconnect()V
-GOTO L862
-ASTORE 82
-GETSTATIC java/lang/System.out:Ljava/io/PrintStream;
-ALOAD 82
+GOTO L1169
+ASTORE 111
+GETSTATIC java/lang/System.out:Ljava/io/PrintStream;
+ALOAD 111
 INVOKEVIRTUAL java/lang/Exception.getMessage()Ljava/lang/String;
 INVOKEDYNAMIC makeConcatWithConstants(Ljava/lang/String;)Ljava/lang/String; bsm=H6 java/lang/invoke/StringConcatFactory.makeConcatWithConstants(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;Ljava/lang/String;[Ljava/lang/Object;)Ljava/lang/invoke/CallSite; args=[str:"[LiTo] Wind Shot disconnect persistence failed: \u0001"]
 INVOKEVIRTUAL java/io/PrintStream.println(Ljava/lang/String;)V
 ALOAD 0
 INVOKESPECIAL local/lito381/server/ClientSession.persistFireBlessOnDisconnect()V
-GOTO L871
-ASTORE 83
-GETSTATIC java/lang/System.out:Ljava/io/PrintStream;
-ALOAD 83
+GOTO L1178
+ASTORE 112
+GETSTATIC java/lang/System.out:Ljava/io/PrintStream;
+ALOAD 112
 INVOKEVIRTUAL java/lang/Exception.getMessage()Ljava/lang/String;
 INVOKEDYNAMIC makeConcatWithConstants(Ljava/lang/String;)Ljava/lang/String; bsm=H6 java/lang/invoke/StringConcatFactory.makeConcatWithConstants(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;Ljava/lang/String;[Ljava/lang/Object;)Ljava/lang/invoke/CallSite; args=[str:"[LiTo] Fire Bless disconnect persistence failed: \u0001"]
 INVOKEVIRTUAL java/io/PrintStream.println(Ljava/lang/String;)V
 ALOAD 0
 INVOKESPECIAL local/lito381/server/ClientSession.persistStormEyeOnDisconnect()V
-GOTO L880
-ASTORE 84
-GETSTATIC java/lang/System.out:Ljava/io/PrintStream;
-ALOAD 84
+GOTO L1187
+ASTORE 113
+GETSTATIC java/lang/System.out:Ljava/io/PrintStream;
+ALOAD 113
 INVOKEVIRTUAL java/lang/Exception.getMessage()Ljava/lang/String;
 INVOKEDYNAMIC makeConcatWithConstants(Ljava/lang/String;)Ljava/lang/String; bsm=H6 java/lang/invoke/StringConcatFactory.makeConcatWithConstants(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;Ljava/lang/String;[Ljava/lang/Object;)Ljava/lang/invoke/CallSite; args=[str:"[LiTo] Storm Eye disconnect persistence failed: \u0001"]
 INVOKEVIRTUAL java/io/PrintStream.println(Ljava/lang/String;)V
 ALOAD 0
 INVOKESPECIAL local/lito381/server/ClientSession.persistStormShotOnDisconnect()V
-GOTO L889
-ASTORE 85
-GETSTATIC java/lang/System.out:Ljava/io/PrintStream;
-ALOAD 85
+GOTO L1196
+ASTORE 114
+GETSTATIC java/lang/System.out:Ljava/io/PrintStream;
+ALOAD 114
 INVOKEVIRTUAL java/lang/Exception.getMessage()Ljava/lang/String;
 INVOKEDYNAMIC makeConcatWithConstants(Ljava/lang/String;)Ljava/lang/String; bsm=H6 java/lang/invoke/StringConcatFactory.makeConcatWithConstants(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;Ljava/lang/String;[Ljava/lang/Object;)Ljava/lang/invoke/CallSite; args=[str:"[LiTo] Storm Shot disconnect persistence failed: \u0001"]
 INVOKEVIRTUAL java/io/PrintStream.println(Ljava/lang/String;)V
 ALOAD 0
 INVOKESPECIAL local/lito381/server/ClientSession.persistEarthBlessOnDisconnect()V
-GOTO L898
-ASTORE 86
-GETSTATIC java/lang/System.out:Ljava/io/PrintStream;
-ALOAD 86
+GOTO L1205
+ASTORE 115
+GETSTATIC java/lang/System.out:Ljava/io/PrintStream;
+ALOAD 115
 INVOKEVIRTUAL java/lang/Exception.getMessage()Ljava/lang/String;
 INVOKEDYNAMIC makeConcatWithConstants(Ljava/lang/String;)Ljava/lang/String; bsm=H6 java/lang/invoke/StringConcatFactory.makeConcatWithConstants(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;Ljava/lang/String;[Ljava/lang/Object;)Ljava/lang/invoke/CallSite; args=[str:"[LiTo] Earth Bless disconnect persistence failed: \u0001"]
 INVOKEVIRTUAL java/io/PrintStream.println(Ljava/lang/String;)V
 ALOAD 0
 INVOKESPECIAL local/lito381/server/ClientSession.persistIronSkinOnDisconnect()V
-GOTO L907
-ASTORE 87
-GETSTATIC java/lang/System.out:Ljava/io/PrintStream;
-ALOAD 87
+GOTO L1214
+ASTORE 116
+GETSTATIC java/lang/System.out:Ljava/io/PrintStream;
+ALOAD 116
 INVOKEVIRTUAL java/lang/Exception.getMessage()Ljava/lang/String;
 INVOKEDYNAMIC makeConcatWithConstants(Ljava/lang/String;)Ljava/lang/String; bsm=H6 java/lang/invoke/StringConcatFactory.makeConcatWithConstants(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;Ljava/lang/String;[Ljava/lang/Object;)Ljava/lang/invoke/CallSite; args=[str:"[LiTo] Iron Skin disconnect persistence failed: \u0001"]
 INVOKEVIRTUAL java/io/PrintStream.println(Ljava/lang/String;)V
 ALOAD 0
 INVOKESPECIAL local/lito381/server/ClientSession.persistMovementBuffOnDisconnect()V
-GOTO L916
-ASTORE 88
-GETSTATIC java/lang/System.out:Ljava/io/PrintStream;
-ALOAD 88
+GOTO L1223
+ASTORE 117
+GETSTATIC java/lang/System.out:Ljava/io/PrintStream;
+ALOAD 117
 INVOKEVIRTUAL java/lang/Exception.getMessage()Ljava/lang/String;
 INVOKEDYNAMIC makeConcatWithConstants(Ljava/lang/String;)Ljava/lang/String; bsm=H6 java/lang/invoke/StringConcatFactory.makeConcatWithConstants(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;Ljava/lang/String;[Ljava/lang/Object;)Ljava/lang/invoke/CallSite; args=[str:"[LiTo] movement buff disconnect persistence failed: \u0001"]
 INVOKEVIRTUAL java/io/PrintStream.println(Ljava/lang/String;)V
 ALOAD 0
 INVOKESPECIAL local/lito381/server/ClientSession.persistArmorProtectionOnDisconnect()V
-GOTO L925
-ASTORE 89
-GETSTATIC java/lang/System.out:Ljava/io/PrintStream;
-ALOAD 89
+GOTO L1232
+ASTORE 118
+GETSTATIC java/lang/System.out:Ljava/io/PrintStream;
+ALOAD 118
 INVOKEVIRTUAL java/lang/Exception.getMessage()Ljava/lang/String;
 INVOKEDYNAMIC makeConcatWithConstants(Ljava/lang/String;)Ljava/lang/String; bsm=H6 java/lang/invoke/StringConcatFactory.makeConcatWithConstants(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;Ljava/lang/String;[Ljava/lang/Object;)Ljava/lang/invoke/CallSite; args=[str:"[LiTo] armor protection disconnect persistence failed: \u0001"]
 INVOKEVIRTUAL java/io/PrintStream.println(Ljava/lang/String;)V
 ALOAD 0
 GETFIELD local/lito381/server/ClientSession.phase:Llocal/lito381/server/ClientSession$Phase;
 GETSTATIC local/lito381/server/ClientSession$Phase.WORLD:Llocal/lito381/server/ClientSession$Phase;
-IF_ACMPNE L945
+IF_ACMPNE L1252
 ALOAD 0
 CONST str:"owner-disconnect"
 INVOKESPECIAL local/lito381/server/ClientSession.abandonClassicWorldRescueFollower(Ljava/lang/String;)V
@@ -938,55 +1245,55 @@
 ALOAD 0
 CONST str:"owner-disconnect"
 INVOKESPECIAL local/lito381/server/ClientSession.abandonClassicWorldRoiFollower(Ljava/lang/String;)V
-GOTO L945
-ASTORE 90
-GETSTATIC java/lang/System.out:Ljava/io/PrintStream;
-ALOAD 90
+GOTO L1252
+ASTORE 119
+GETSTATIC java/lang/System.out:Ljava/io/PrintStream;
+ALOAD 119
 INVOKEVIRTUAL java/lang/Exception.getMessage()Ljava/lang/String;
 INVOKEDYNAMIC makeConcatWithConstants(Ljava/lang/String;)Ljava/lang/String; bsm=H6 java/lang/invoke/StringConcatFactory.makeConcatWithConstants(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;Ljava/lang/String;[Ljava/lang/Object;)Ljava/lang/invoke/CallSite; args=[str:"[LiTo] quest follower disconnect cleanup failed: \u0001"]
 INVOKEVIRTUAL java/io/PrintStream.println(Ljava/lang/String;)V
 ALOAD 0
 GETFIELD local/lito381/server/ClientSession.activeMagicDollObjectIds:Ljava/util/Set;
 DUP
-ASTORE 91
+ASTORE 120
 MONITORENTER
 ALOAD 0
 CONST str:"owner-disconnect"
 CONST Integer:0
 INVOKESPECIAL local/lito381/server/ClientSession.recallNativeMagicDollLocked(Ljava/lang/String;Z)V
-ALOAD 91
+ALOAD 120
 MONITOREXIT
-GOTO L962
-ASTORE 92
-ALOAD 91
+GOTO L1269
+ASTORE 121
+ALOAD 120
 MONITOREXIT
-ALOAD 92
+ALOAD 121
 ATHROW
-GOTO L969
-ASTORE 93
-GETSTATIC java/lang/System.out:Ljava/io/PrintStream;
-ALOAD 93
+GOTO L1276
+ASTORE 122
+GETSTATIC java/lang/System.out:Ljava/io/PrintStream;
+ALOAD 122
 INVOKEVIRTUAL java/lang/Exception.getMessage()Ljava/lang/String;
 INVOKEDYNAMIC makeConcatWithConstants(Ljava/lang/String;)Ljava/lang/String; bsm=H6 java/lang/invoke/StringConcatFactory.makeConcatWithConstants(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;Ljava/lang/String;[Ljava/lang/Object;)Ljava/lang/invoke/CallSite; args=[str:"[LiTo] magic-doll disconnect cleanup failed: \u0001"]
 INVOKEVIRTUAL java/io/PrintStream.println(Ljava/lang/String;)V
 ALOAD 0
 INVOKESPECIAL local/lito381/server/ClientSession.persistLitoCharacterOnDisconnect()V
-GOTO L981
-ASTORE 94
-GETSTATIC java/lang/System.out:Ljava/io/PrintStream;
-ALOAD 94
+GOTO L1288
+ASTORE 123
+GETSTATIC java/lang/System.out:Ljava/io/PrintStream;
+ALOAD 123
 INVOKEVIRTUAL java/lang/Object.getClass()Ljava/lang/Class;
 INVOKEVIRTUAL java/lang/Class.getSimpleName()Ljava/lang/String;
-ALOAD 94
+ALOAD 123
 INVOKEVIRTUAL java/lang/Exception.getMessage()Ljava/lang/String;
 INVOKEDYNAMIC makeConcatWithConstants(Ljava/lang/String;Ljava/lang/String;)Ljava/lang/String; bsm=H6 java/lang/invoke/StringConcatFactory.makeConcatWithConstants(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;Ljava/lang/String;[Ljava/lang/Object;)Ljava/lang/invoke/CallSite; args=[str:"[LiTo] MySQL character disconnect persistence failed: \u0001: \u0001"]
 INVOKEVIRTUAL java/io/PrintStream.println(Ljava/lang/String;)V
 ALOAD 0
 INVOKESPECIAL local/lito381/server/ClientSession.notifyPeersOfDisconnect()V
-GOTO L990
-ASTORE 95
-GETSTATIC java/lang/System.out:Ljava/io/PrintStream;
-ALOAD 95
+GOTO L1297
+ASTORE 124
+GETSTATIC java/lang/System.out:Ljava/io/PrintStream;
+ALOAD 124
 INVOKEVIRTUAL java/io/IOException.getMessage()Ljava/lang/String;
 INVOKEDYNAMIC makeConcatWithConstants(Ljava/lang/String;)Ljava/lang/String; bsm=H6 java/lang/invoke/StringConcatFactory.makeConcatWithConstants(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;Ljava/lang/String;[Ljava/lang/Object;)Ljava/lang/invoke/CallSite; args=[str:"[LiTo] disconnect broadcast failed: \u0001"]
 INVOKEVIRTUAL java/io/PrintStream.println(Ljava/lang/String;)V
@@ -1005,7 +1312,7 @@
 ALOAD 0
 GETFIELD local/lito381/server/ClientSession.selectedCharacter:Ljava/lang/String;
 INVOKEVIRTUAL java/lang/String.isBlank()Z
-IFNE L1020
+IFNE L1327
 ALOAD 0
 GETFIELD local/lito381/server/ClientSession.state:Llocal/lito381/server/GameStateStore;
 ALOAD 0
@@ -1024,46 +1331,46 @@
 GETFIELD local/lito381/server/ClientSession.hub:Llocal/lito381/server/GameHub;
 ALOAD 0
 INVOKEVIRTUAL local/lito381/server/GameHub.disconnect(Llocal/lito381/server/ClientSession;)Ljava/util/List;
-ASTORE 96
 ALOAD 0
 INVOKESTATIC local/lito381/server/NativeRoomRules.restoreBot(Llocal/lito381/server/ClientSession;)V
-ILOAD 74
-IFEQ L1041
-ALOAD 0
-LLOAD 72/73
+ASTORE 125
+ILOAD 103
+IFEQ L1348
+ALOAD 0
+LLOAD 101/102
 CONST str:"disconnect"
 INVOKESPECIAL local/lito381/server/ClientSession.broadcastRoomOwner(JLjava/lang/String;)V
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
 INVOKEVIRTUAL java/io/IOException.getMessage()Ljava/lang/String;
 INVOKEDYNAMIC makeConcatWithConstants(JLjava/lang/String;)Ljava/lang/String; bsm=H6 java/lang/invoke/StringConcatFactory.makeConcatWithConstants(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;Ljava/lang/String;[Ljava/lang/Object;)Ljava/lang/invoke/CallSite; args=[str:"[LiTo] room-owner broadcast failed room=\u0001: \u0001"]
 INVOKEVIRTUAL java/io/PrintStream.println(Ljava/lang/String;)V
-ALOAD 96
+ALOAD 125
 INVOKEINTERFACE java/util/List.iterator()Ljava/util/Iterator; itf
-ASTORE 98
-ALOAD 98
+ASTORE 127
+ALOAD 127
 INVOKEINTERFACE java/util/Iterator.hasNext()Z itf
-IFEQ L1064
-ALOAD 98
+IFEQ L1371
+ALOAD 127
 INVOKEINTERFACE java/util/Iterator.next()Ljava/lang/Object; itf
 CHECKCAST java/lang/Long
 INVOKEVIRTUAL java/lang/Long.longValue()J
-LSTORE 99/100
-ALOAD 0
-LLOAD 99/100
+LSTORE 128/129
+ALOAD 0
+LLOAD 128/129
 INVOKESPECIAL local/lito381/server/ClientSession.broadcastRoomDeleted(J)V
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
 INVOKEVIRTUAL java/io/IOException.getMessage()Ljava/lang/String;
 INVOKEDYNAMIC makeConcatWithConstants(JLjava/lang/String;)Ljava/lang/String; bsm=H6 java/lang/invoke/StringConcatFactory.makeConcatWithConstants(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;Ljava/lang/String;[Ljava/lang/Object;)Ljava/lang/invoke/CallSite; args=[str:"[LiTo] room-delete broadcast failed room=\u0001: \u0001"]
 INVOKEVIRTUAL java/io/PrintStream.println(Ljava/lang/String;)V
-GOTO L1044
-ALOAD 71
+GOTO L1351
+ALOAD 100
 ATHROW
 RETURN
```

</details>

### `local/lito381/server/ClientSession#setReady([B)V`

<details>
<summary>完整差異（全部 hunks）</summary>

```diff
--- staged/local/lito381/server/ClientSession#setReady([B)V
+++ candidate/local/lito381/server/ClientSession#setReady([B)V
@@ -3,30 +3,32 @@
 ALOAD 0
 GETFIELD local/lito381/server/ClientSession.phase:Llocal/lito381/server/ClientSession$Phase;
 GETSTATIC local/lito381/server/ClientSession$Phase.ROOM:Llocal/lito381/server/ClientSession$Phase;
-IF_ACMPEQ L5
+IF_ACMPEQ L7
+ALOAD 0
+INVOKESTATIC local/lito381/server/NativeRoomRules.feedback(Llocal/lito381/server/ClientSession;)V
 RETURN
 ALOAD 1
 INVOKESTATIC local/lito381/server/Protocol.litoReadyRequest([B)Llocal/lito381/server/Protocol$LitoReadyRequest;
 ASTORE 2
 ALOAD 2
 INVOKEVIRTUAL local/lito381/server/Protocol$LitoReadyRequest.ownerStart()Z
-IFEQ L13
+IFEQ L15
 GETSTATIC local/lito381/server/Protocol$LitoParticipantState.READY:Llocal/lito381/server/Protocol$LitoParticipantState;
-GOTO L15
+GOTO L17
 ALOAD 2
 INVOKEVIRTUAL local/lito381/server/Protocol$LitoReadyRequest.participantState()Llocal/lito381/server/Protocol$LitoParticipantState;
 ASTORE 3
 ALOAD 0
 GETFIELD local/lito381/server/ClientSession.rentalTownRoomState:Z
-IFEQ L43
+IFEQ L47
 ALOAD 0
 CONST Integer:0
 PUTFIELD local/lito381/server/ClientSession.ready:Z
 ALOAD 3
 GETSTATIC local/lito381/server/Protocol$LitoParticipantState.SHOP_LIST:Llocal/lito381/server/Protocol$LitoParticipantState;
-IF_ACMPNE L27
-GETSTATIC local/lito381/server/Protocol$LitoParticipantState.SHOP_LIST:Llocal/lito381/server/Protocol$LitoParticipantState;
-GOTO L28
+IF_ACMPNE L29
+GETSTATIC local/lito381/server/Protocol$LitoParticipantState.SHOP_LIST:Llocal/lito381/server/Protocol$LitoParticipantState;
+GOTO L30
 GETSTATIC local/lito381/server/Protocol$LitoParticipantState.NOT_READY:Llocal/lito381/server/Protocol$LitoParticipantState;
 ASTORE 4
 ALOAD 0
@@ -42,17 +44,19 @@
 INVOKEVIRTUAL local/lito381/server/Protocol$LitoParticipantState.wireValue()I
 INVOKEDYNAMIC makeConcatWithConstants(Ljava/lang/String;II)Ljava/lang/String; bsm=H6 java/lang/invoke/StringConcatFactory.makeConcatWithConstants(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;Ljava/lang/String;[Ljava/lang/Object;)Ljava/lang/invoke/CallSite; args=[str:"S_LITO_READY_GAME rental-town player=\u0001 requested=\u0001 published=\u0001"]
 INVOKESPECIAL local/lito381/server/ClientSession.sendEncrypted([BLjava/lang/String;)V
+ALOAD 0
+INVOKESTATIC local/lito381/server/NativeRoomRules.feedback(Llocal/lito381/server/ClientSession;)V
 RETURN
 ALOAD 2
 INVOKEVIRTUAL local/lito381/server/Protocol$LitoReadyRequest.ownerStart()Z
-IFEQ L53
+IFEQ L57
 ALOAD 0
 GETFIELD local/lito381/server/ClientSession.hub:Llocal/lito381/server/GameHub;
 ALOAD 0
 GETFIELD local/lito381/server/ClientSession.currentRoomId:J
 ALOAD 0
 INVOKEVIRTUAL local/lito381/server/GameHub.requestOwnerStart(JLlocal/lito381/server/ClientSession;)Llocal/lito381/server/GameHub$ReadyUpdate;
-GOTO L61
+GOTO L65
 ALOAD 0
 GETFIELD local/lito381/server/ClientSession.hub:Llocal/lito381/server/GameHub;
 ALOAD 0
@@ -64,14 +68,14 @@
 ASTORE 5
 ALOAD 2
 INVOKEVIRTUAL local/lito381/server/Protocol$LitoReadyRequest.ownerStart()Z
-IFEQ L79
+IFEQ L83
 ALOAD 0
 GETFIELD local/lito381/server/ClientSession.hub:Llocal/lito381/server/GameHub;
 ALOAD 0
 GETFIELD local/lito381/server/ClientSession.currentRoomId:J
 ALOAD 0
 INVOKEVIRTUAL local/lito381/server/GameHub.isOwner(JLlocal/lito381/server/ClientSession;)Z
-IFNE L79
+IFNE L83
 GETSTATIC java/lang/System.out:Ljava/io/PrintStream;
 ALOAD 0
 INVOKEVIRTUAL local/lito381/server/ClientSession.playerName()Ljava/lang/String;
@@ -85,14 +89,14 @@
 PUTFIELD local/lito381/server/ClientSession.ready:Z
 ALOAD 3
 GETSTATIC local/lito381/server/Protocol$LitoParticipantState.SHOP_LIST:Llocal/lito381/server/Protocol$LitoParticipantState;
-IF_ACMPNE L88
-GETSTATIC local/lito381/server/Protocol$LitoParticipantState.SHOP_LIST:Llocal/lito381/server/Protocol$LitoParticipantState;
-GOTO L94
+IF_ACMPNE L92
+GETSTATIC local/lito381/server/Protocol$LitoParticipantState.SHOP_LIST:Llocal/lito381/server/Protocol$LitoParticipantState;
+GOTO L98
 ALOAD 0
 GETFIELD local/lito381/server/ClientSession.ready:Z
-IFEQ L93
+IFEQ L97
 GETSTATIC local/lito381/server/Protocol$LitoParticipantState.READY:Llocal/lito381/server/Protocol$LitoParticipantState;
-GOTO L94
+GOTO L98
 GETSTATIC local/lito381/server/Protocol$LitoParticipantState.NOT_READY:Llocal/lito381/server/Protocol$LitoParticipantState;
 ASTORE 6
 ALOAD 0
@@ -111,7 +115,7 @@
 ASTORE 9
 ALOAD 9
 INVOKEINTERFACE java/util/Iterator.hasNext()Z itf
-IFEQ L128
+IFEQ L132
 ALOAD 9
 INVOKEINTERFACE java/util/Iterator.next()Ljava/lang/Object; itf
 CHECKCAST local/lito381/server/ClientSession
@@ -127,10 +131,10 @@
 INVOKESTATIC java/lang/String.valueOf(Ljava/lang/Object;)Ljava/lang/String;
 INVOKEDYNAMIC makeConcatWithConstants(Ljava/lang/String;ZLjava/lang/String;)Ljava/lang/String; bsm=H6 java/lang/invoke/StringConcatFactory.makeConcatWithConstants(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;Ljava/lang/String;[Ljava/lang/Object;)Ljava/lang/invoke/CallSite; args=[str:"S_LITO_READY_GAME player=\u0001 ready=\u0001 request=\u0001"]
 INVOKESPECIAL local/lito381/server/ClientSession.sendEncrypted([BLjava/lang/String;)V
-GOTO L109
+GOTO L113
 ALOAD 5
 INVOKEVIRTUAL local/lito381/server/GameHub$ReadyUpdate.minimumPlayersPerForceWarning()I
-IFLE L139
+IFLE L143
 ALOAD 0
 ALOAD 5
 INVOKEVIRTUAL local/lito381/server/GameHub$ReadyUpdate.minimumPlayersPerForceWarning()I
@@ -141,7 +145,7 @@
 INVOKESPECIAL local/lito381/server/ClientSession.sendEncrypted([BLjava/lang/String;)V
 ALOAD 0
 GETFIELD local/lito381/server/ClientSession.ready:Z
-IFEQ L164
+IFEQ L168
 ALOAD 0
 GETFIELD local/lito381/server/ClientSession.hub:Llocal/lito381/server/GameHub;
 ALOAD 0
@@ -150,14 +154,14 @@
 INVOKEVIRTUAL local/lito381/server/BattleRules$RuleSet.mode()Llocal/lito381/server/BattleRules$Mode;
 ALOAD 0
 INVOKESTATIC local/lito381/server/NativeRoomRules.bot(Llocal/lito381/server/BattleRules$Mode;Llocal/lito381/server/ClientSession;)Z
-IFEQ L164
+IFEQ L168
 ALOAD 0
 GETFIELD local/lito381/server/ClientSession.hub:Llocal/lito381/server/GameHub;
 ALOAD 0
 GETFIELD local/lito381/server/ClientSession.currentRoomId:J
 INVOKEVIRTUAL local/lito381/server/GameHub.roomCombatantCount(J)I
 CONST Integer:2
-IF_ICMPGE L164
+IF_ICMPGE L168
 ALOAD 0
 CONST str:"ArenaBot"
 CONST Integer:1
@@ -169,13 +173,13 @@
 ASTORE 11
 ALOAD 11
 GETSTATIC local/lito381/server/GameHub$ReadyTransition.COUNTDOWN_STARTED:Llocal/lito381/server/GameHub$ReadyTransition;
-IF_ACMPNE L185
+IF_ACMPNE L189
 ALOAD 8
 INVOKEINTERFACE java/util/List.iterator()Ljava/util/Iterator; itf
 ASTORE 12
 ALOAD 12
 INVOKEINTERFACE java/util/Iterator.hasNext()Z itf
-IFEQ L184
+IFEQ L188
 ALOAD 12
 INVOKEINTERFACE java/util/Iterator.next()Ljava/lang/Object; itf
 CHECKCAST local/lito381/server/ClientSession
@@ -183,17 +187,17 @@
 ALOAD 13
 CONST Integer:3
 INVOKEVIRTUAL local/lito381/server/ClientSession.serverCountdown(I)V
-GOTO L173
-GOTO L202
+GOTO L177
+GOTO L206
 ALOAD 11
 GETSTATIC local/lito381/server/GameHub$ReadyTransition.COUNTDOWN_CANCELLED:Llocal/lito381/server/GameHub$ReadyTransition;
-IF_ACMPNE L202
+IF_ACMPNE L206
 ALOAD 8
 INVOKEINTERFACE java/util/List.iterator()Ljava/util/Iterator; itf
 ASTORE 14
 ALOAD 14
 INVOKEINTERFACE java/util/Iterator.hasNext()Z itf
-IFEQ L202
+IFEQ L206
 ALOAD 14
 INVOKEINTERFACE java/util/Iterator.next()Ljava/lang/Object; itf
 CHECKCAST local/lito381/server/ClientSession
@@ -201,7 +205,7 @@
 ALOAD 15
 CONST Integer:0
 INVOKEVIRTUAL local/lito381/server/ClientSession.serverCountdown(I)V
-GOTO L191
+GOTO L195
 ALOAD 0
 INVOKESTATIC local/lito381/server/NativeRoomRules.feedback(Llocal/lito381/server/ClientSession;)V
 RETURN
```

</details>

### `local/lito381/server/GameHub#battleDisconnectWork(JJ)Llocal/lito381/server/GameHub$BattleDisconnectWork;`

<details>
<summary>完整差異（全部 hunks）</summary>

```diff
--- staged/local/lito381/server/GameHub#battleDisconnectWork(JJ)Llocal/lito381/server/GameHub$BattleDisconnectWork;
+++ candidate/local/lito381/server/GameHub#battleDisconnectWork(JJ)Llocal/lito381/server/GameHub$BattleDisconnectWork;
@@ -276,18 +276,17 @@
 ALOAD 25
 ALOAD 8
 INVOKESTATIC local/lito381/server/TeamDisconnectSettlement.continuingWork(Llocal/lito381/server/GameHub;JLjava/util/List;Llocal/lito381/server/BattleRules$Resolution;Ljava/util/Set;)Ljava/lang/Object;
-ASTORE 26
-ALOAD 26
-IFNULL L282
-ALOAD 26
+DUP
+IFNULL L280
 CHECKCAST local/lito381/server/GameHub$BattleDisconnectWork
 ARETURN
+POP
 NEW java/util/ArrayList
 DUP
 ALOAD 5
 GETFIELD local/lito381/server/GameHub$Room.members:Ljava/util/Set;
 INVOKESPECIAL java/util/ArrayList.<init>(Ljava/util/Collection;)V
-ASTORE 27
+ASTORE 26
 ALOAD 5
 CONST Integer:0
 PUTFIELD local/lito381/server/GameHub$Room.battleStarted:Z
@@ -298,9 +297,9 @@
 ALOAD 5
 GETFIELD local/lito381/server/GameHub$Room.members:Ljava/util/Set;
 INVOKEINTERFACE java/util/Set.isEmpty()Z itf
-IFNE L301
+IFNE L300
 CONST Integer:1
-GOTO L302
+GOTO L301
 CONST Integer:0
 PUTFIELD local/lito381/server/GameHub$Room.resultPending:Z
 ALOAD 5
@@ -310,7 +309,7 @@
 GETFIELD local/lito381/server/GameHub$Room.ready:Ljava/util/Set;
 INVOKEINTERFACE java/util/Set.clear()V itf
 ALOAD 5
-ALOAD 5
+DUP
 ALOAD 0
 INVOKESTATIC local/lito381/server/NativeRoomRules.botHp(Ljava/lang/Object;Llocal/lito381/server/GameHub;)I
 PUTFIELD local/lito381/server/GameHub$Room.botHp:I
@@ -393,33 +392,33 @@
 ALOAD 5
 GETFIELD local/lito381/server/GameHub$Room.channelId:I
 INVOKESPECIAL local/lito381/server/GameHub.requiredChannel(I)Llocal/lito381/server/GameHub$Channel;
-ASTORE 28
+ASTORE 27
 ALOAD 5
 GETFIELD local/lito381/server/GameHub$Room.members:Ljava/util/Set;
 INVOKEINTERFACE java/util/Set.isEmpty()Z itf
-IFEQ L406
+IFEQ L405
 ALOAD 5
 GETFIELD local/lito381/server/GameHub$Room.id:J
-ALOAD 28
+ALOAD 27
 GETFIELD local/lito381/server/GameHub$Channel.defaultRoomId:J
 LCMP
-IFEQ L406
+IFEQ L405
 CONST Integer:1
-GOTO L407
-CONST Integer:0
-ISTORE 29
-ILOAD 29
-IFEQ L416
+GOTO L406
+CONST Integer:0
+ISTORE 28
+ILOAD 28
+IFEQ L415
 NEW java/util/ArrayList
 DUP
-ALOAD 28
+ALOAD 27
 GETFIELD local/lito381/server/GameHub$Channel.members:Ljava/util/Set;
 INVOKESPECIAL java/util/ArrayList.<init>(Ljava/util/Collection;)V
-GOTO L417
+GOTO L416
 INVOKESTATIC java/util/List.of()Ljava/util/List; itf
-ASTORE 30
-ILOAD 29
-IFEQ L435
+ASTORE 29
+ILOAD 28
+IFEQ L434
 ALOAD 0
 GETFIELD local/lito381/server/GameHub.rooms:Ljava/util/Map;
 ALOAD 5
@@ -427,18 +426,18 @@
 INVOKESTATIC java/lang/Long.valueOf(J)Ljava/lang/Long;
 INVOKEINTERFACE java/util/Map.remove(Ljava/lang/Object;)Ljava/lang/Object; itf
 POP
-ALOAD 28
+ALOAD 27
 GETFIELD local/lito381/server/GameHub$Channel.roomIds:Ljava/util/Set;
 ALOAD 5
 GETFIELD local/lito381/server/GameHub$Room.id:J
 INVOKESTATIC java/lang/Long.valueOf(J)Ljava/lang/Long;
 INVOKEINTERFACE java/util/Set.remove(Ljava/lang/Object;)Z itf
 POP
-GOTO L442
+GOTO L441
 ALOAD 5
 GETFIELD local/lito381/server/GameHub$Room.members:Ljava/util/Set;
 INVOKEINTERFACE java/util/Set.isEmpty()Z itf
-IFEQ L442
+IFEQ L441
 ALOAD 0
 ALOAD 5
 INVOKESPECIAL local/lito381/server/GameHub.resetOrRemove(Llocal/lito381/server/GameHub$Room;)V
@@ -455,10 +454,10 @@
 NEW local/lito381/server/GameHub$BattleDisconnectWork
 DUP
 LLOAD 1/2
-ALOAD 27
+ALOAD 26
 ALOAD 6
 ALOAD 25
-ALOAD 30
-ILOAD 29
+ALOAD 29
+ILOAD 28
 INVOKESPECIAL local/lito381/server/GameHub$BattleDisconnectWork.<init>(JLjava/util/List;Ljava/util/List;Llocal/lito381/server/BattleRules$Resolution;Ljava/util/List;Z)V
 ARETURN
```

</details>

### `local/lito381/server/ClientSession$1#<clinit>()V`

<details>
<summary>完整差異（全部 hunks）</summary>

```diff
--- staged/local/lito381/server/ClientSession$1#<clinit>()V
+++ candidate/local/lito381/server/ClientSession$1#<clinit>()V
@@ -1,165 +1,165 @@
 META 8 null []
-TRY [4:[10:java/lang/NoSuchFieldError  ], 5:[10:java/lang/NoSuchFieldError  ], 6:[10:java/lang/NoSuchFieldError  ], 8:[10:java/lang/NoSuchFieldError  ], 11:[17:java/lang/NoSuchFieldError  ], 12:[17:java/lang/NoSuchFieldError  ], 13:[17:java/lang/NoSuchFieldError  ], 15:[17:java/lang/NoSuchFieldError  ], 18:[24:java/lang/NoSuchFieldError  ], 19:[24:java/lang/NoSuchFieldError  ], 20:[24:java/lang/NoSuchFieldError  ], 22:[24:java/lang/NoSuchFieldError  ], 25:[31:java/lang/NoSuchFieldError  ], 26:[31:java/lang/NoSuchFieldError  ], 27:[31:java/lang/NoSuchFieldError  ], 29:[31:java/lang/NoSuchFieldError  ], 36:[42:java/lang/NoSuchFieldError  ], 37:[42:java/lang/NoSuchFieldError  ], 38:[42:java/lang/NoSuchFieldError  ], 40:[42:java/lang/NoSuchFieldError  ], 43:[49:java/lang/NoSuchFieldError  ], 44:[49:java/lang/NoSuchFieldError  ], 45:[49:java/lang/NoSuchFieldError  ], 47:[49:java/lang/NoSuchFieldError  ], 50:[56:java/lang/NoSuchFieldError  ], 51:[56:java/lang/NoSuchFieldError  ], 52:[56:java/lang/NoSuchFieldError  ], 54:[56:java/lang/NoSuchFieldError  ], 57:[63:java/lang/NoSuchFieldError  ], 58:[63:java/lang/NoSuchFieldError  ], 59:[63:java/lang/NoSuchFieldError  ], 61:[63:java/lang/NoSuchFieldError  ], 64:[70:java/lang/NoSuchFieldError  ], 65:[70:java/lang/NoSuchFieldError  ], 66:[70:java/lang/NoSuchFieldError  ], 68:[70:java/lang/NoSuchFieldError  ], 71:[77:java/lang/NoSuchFieldError  ], 72:[77:java/lang/NoSuchFieldError  ], 73:[77:java/lang/NoSuchFieldError  ], 75:[77:java/lang/NoSuchFieldError  ], 78:[84:java/lang/NoSuchFieldError  ], 79:[84:java/lang/NoSuchFieldError  ], 80:[84:java/lang/NoSuchFieldError  ], 82:[84:java/lang/NoSuchFieldError  ], 85:[91:java/lang/NoSuchFieldError  ], 86:[91:java/lang/NoSuchFieldError  ], 87:[91:java/lang/NoSuchFieldError  ], 89:[91:java/lang/NoSuchFieldError  ], 92:[98:java/lang/NoSuchFieldError  ], 93:[98:java/lang/NoSuchFieldError  ], 94:[98:java/lang/NoSuchFieldError  ], 96:[98:java/lang/NoSuchFieldError  ], 103:[109:java/lang/NoSuchFieldError  ], 104:[109:java/lang/NoSuchFieldError  ], 105:[109:java/lang/NoSuchFieldError  ], 107:[109:java/lang/NoSuchFieldError  ], 110:[116:java/lang/NoSuchFieldError  ], 111:[116:java/lang/NoSuchFieldError  ], 112:[116:java/lang/NoSuchFieldError  ], 114:[116:java/lang/NoSuchFieldError  ], 117:[123:java/lang/NoSuchFieldError  ], 118:[123:java/lang/NoSuchFieldError  ], 119:[123:java/lang/NoSuchFieldError  ], 121:[123:java/lang/NoSuchFieldError  ], 124:[130:java/lang/NoSuchFieldError  ], 125:[130:java/lang/NoSuchFieldError  ], 126:[130:java/lang/NoSuchFieldError  ], 128:[130:java/lang/NoSuchFieldError  ], 131:[137:java/lang/NoSuchFieldError  ], 132:[137:java/lang/NoSuchFieldError  ], 133:[137:java/lang/NoSuchFieldError  ], 135:[137:java/lang/NoSuchFieldError  ], 142:[148:java/lang/NoSuchFieldError  ], 143:[148:java/lang/NoSuchFieldError  ], 144:[148:java/lang/NoSuchFieldError  ], 146:[148:java/lang/NoSuchFieldError  ], 149:[155:java/lang/NoSuchFieldError  ], 150:[155:java/lang/NoSuchFieldError  ], 151:[155:java/lang/NoSuchFieldError  ], 153:[155:java/lang/NoSuchFieldError  ], 156:[162:java/lang/NoSuchFieldError  ], 157:[162:java/lang/NoSuchFieldError  ], 158:[162:java/lang/NoSuchFieldError  ], 160:[162:java/lang/NoSuchFieldError  ], 167:[173:java/lang/NoSuchFieldError  ], 168:[173:java/lang/NoSuchFieldError  ], 169:[173:java/lang/NoSuchFieldError  ], 171:[173:java/lang/NoSuchFieldError  ], 174:[180:java/lang/NoSuchFieldError  ], 175:[180:java/lang/NoSuchFieldError  ], 176:[180:java/lang/NoSuchFieldError  ], 178:[180:java/lang/NoSuchFieldError  ], 181:[187:java/lang/NoSuchFieldError  ], 182:[187:java/lang/NoSuchFieldError  ], 183:[187:java/lang/NoSuchFieldError  ], 185:[187:java/lang/NoSuchFieldError  ], 188:[194:java/lang/NoSuchFieldError  ], 189:[194:java/lang/NoSuchFieldError  ], 190:[194:java/lang/NoSuchFieldError  ], 192:[194:java/lang/NoSuchFieldError  ], 195:[201:java/lang/NoSuchFieldError  ], 196:[201:java/lang/NoSuchFieldError  ], 197:[201:java/lang/NoSuchFieldError  ], 199:[201:java/lang/NoSuchFieldError  ], 202:[208:java/lang/NoSuchFieldError  ], 203:[208:java/lang/NoSuchFieldError  ], 204:[208:java/lang/NoSuchFieldError  ], 206:[208:java/lang/NoSuchFieldError  ], 209:[215:java/lang/NoSuchFieldError  ], 210:[215:java/lang/NoSuchFieldError  ], 211:[215:java/lang/NoSuchFieldError  ], 213:[215:java/lang/NoSuchFieldError  ], 216:[222:java/lang/NoSuchFieldError  ], 217:[222:java/lang/NoSuchFieldError  ], 218:[222:java/lang/NoSuchFieldError  ], 220:[222:java/lang/NoSuchFieldError  ], 223:[229:java/lang/NoSuchFieldError  ], 224:[229:java/lang/NoSuchFieldError  ], 225:[229:java/lang/NoSuchFieldError  ], 227:[229:java/lang/NoSuchFieldError  ], 234:[240:java/lang/NoSuchFieldError  ], 235:[240:java/lang/NoSuchFieldError  ], 236:[240:java/lang/NoSuchFieldError  ], 238:[240:java/lang/NoSuchFieldError  ], 241:[247:java/lang/NoSuchFieldError  ], 242:[247:java/lang/NoSuchFieldError  ], 243:[247:java/lang/NoSuchFieldError  ], 245:[247:java/lang/NoSuchFieldError  ], 248:[254:java/lang/NoSuchFieldError  ], 249:[254:java/lang/NoSuchFieldError  ], 250:[254:java/lang/NoSuchFieldError  ], 252:[254:java/lang/NoSuchFieldError  ], 259:[265:java/lang/NoSuchFieldError  ], 260:[265:java/lang/NoSuchFieldError  ], 261:[265:java/lang/NoSuchFieldError  ], 263:[265:java/lang/NoSuchFieldError  ], 266:[272:java/lang/NoSuchFieldError  ], 267:[272:java/lang/NoSuchFieldError  ], 268:[272:java/lang/NoSuchFieldError  ], 270:[272:java/lang/NoSuchFieldError  ], 273:[279:java/lang/NoSuchFieldError  ], 274:[279:java/lang/NoSuchFieldError  ], 275:[279:java/lang/NoSuchFieldError  ], 277:[279:java/lang/NoSuchFieldError  ], 284:[290:java/lang/NoSuchFieldError  ], 285:[290:java/lang/NoSuchFieldError  ], 286:[290:java/lang/NoSuchFieldError  ], 288:[290:java/lang/NoSuchFieldError  ], 291:[297:java/lang/NoSuchFieldError  ], 292:[297:java/lang/NoSuchFieldError  ], 293:[297:java/lang/NoSuchFieldError  ], 295:[297:java/lang/NoSuchFieldError  ], 298:[304:java/lang/NoSuchFieldError  ], 299:[304:java/lang/NoSuchFieldError  ], 300:[304:java/lang/NoSuchFieldError  ], 302:[304:java/lang/NoSuchFieldError  ], 309:[315:java/lang/NoSuchFieldError  ], 310:[315:java/lang/NoSuchFieldError  ], 311:[315:java/lang/NoSuchFieldError  ], 313:[315:java/lang/NoSuchFieldError  ], 316:[322:java/lang/NoSuchFieldError  ], 317:[322:java/lang/NoSuchFieldError  ], 318:[322:java/lang/NoSuchFieldError  ], 320:[322:java/lang/NoSuchFieldError  ], 323:[329:java/lang/NoSuchFieldError  ], 324:[329:java/lang/NoSuchFieldError  ], 325:[329:java/lang/NoSuchFieldError  ], 327:[329:java/lang/NoSuchFieldError  ], 330:[336:java/lang/NoSuchFieldError  ], 331:[336:java/lang/NoSuchFieldError  ], 332:[336:java/lang/NoSuchFieldError  ], 334:[336:java/lang/NoSuchFieldError  ], 337:[343:java/lang/NoSuchFieldError  ], 338:[343:java/lang/NoSuchFieldError  ], 339:[343:java/lang/NoSuchFieldError  ], 341:[343:java/lang/NoSuchFieldError  ], 344:[350:java/lang/NoSuchFieldError  ], 345:[350:java/lang/NoSuchFieldError  ], 346:[350:java/lang/NoSuchFieldError  ], 348:[350:java/lang/NoSuchFieldError  ], 351:[357:java/lang/NoSuchFieldError  ], 352:[357:java/lang/NoSuchFieldError  ], 353:[357:java/lang/NoSuchFieldError  ], 355:[357:java/lang/NoSuchFieldError  ], 358:[364:java/lang/NoSuchFieldError  ], 359:[364:java/lang/NoSuchFieldError  ], 360:[364:java/lang/NoSuchFieldError  ], 362:[364:java/lang/NoSuchFieldError  ], 369:[375:java/lang/NoSuchFieldError  ], 370:[375:java/lang/NoSuchFieldError  ], 371:[375:java/lang/NoSuchFieldError  ], 373:[375:java/lang/NoSuchFieldError  ], 376:[382:java/lang/NoSuchFieldError  ], 377:[382:java/lang/NoSuchFieldError  ], 378:[382:java/lang/NoSuchFieldError  ], 380:[382:java/lang/NoSuchFieldError  ], 383:[389:java/lang/NoSuchFieldError  ], 384:[389:java/lang/NoSuchFieldError  ], 385:[389:java/lang/NoSuchFieldError  ], 387:[389:java/lang/NoSuchFieldError  ], 390:[396:java/lang/NoSuchFieldError  ], 391:[396:java/lang/NoSuchFieldError  ], 392:[396:java/lang/NoSuchFieldError  ], 394:[396:java/lang/NoSuchFieldError  ], 397:[403:java/lang/NoSuchFieldError  ], 398:[403:java/lang/NoSuchFieldError  ], 399:[403:java/lang/NoSuchFieldError  ], 401:[403:java/lang/NoSuchFieldError  ], 408:[414:java/lang/NoSuchFieldError  ], 409:[414:java/lang/NoSuchFieldError  ], 410:[414:java/lang/NoSuchFieldError  ], 412:[414:java/lang/NoSuchFieldError  ], 415:[421:java/lang/NoSuchFieldError  ], 416:[421:java/lang/NoSuchFieldError  ], 417:[421:java/lang/NoSuchFieldError  ], 419:[421:java/lang/NoSuchFieldError  ], 422:[428:java/lang/NoSuchFieldError  ], 423:[428:java/lang/NoSuchFieldError  ], 424:[428:java/lang/NoSuchFieldError  ], 426:[428:java/lang/NoSuchFieldError  ], 429:[435:java/lang/NoSuchFieldError  ], 430:[435:java/lang/NoSuchFieldError  ], 431:[435:java/lang/NoSuchFieldError  ], 433:[435:java/lang/NoSuchFieldError  ], 440:[446:java/lang/NoSuchFieldError  ], 441:[446:java/lang/NoSuchFieldError  ], 442:[446:java/lang/NoSuchFieldError  ], 444:[446:java/lang/NoSuchFieldError  ], 447:[453:java/lang/NoSuchFieldError  ], 448:[453:java/lang/NoSuchFieldError  ], 449:[453:java/lang/NoSuchFieldError  ], 451:[453:java/lang/NoSuchFieldError  ], 454:[460:java/lang/NoSuchFieldError  ], 455:[460:java/lang/NoSuchFieldError  ], 456:[460:java/lang/NoSuchFieldError  ], 458:[460:java/lang/NoSuchFieldError  ], 465:[471:java/lang/NoSuchFieldError  ], 466:[471:java/lang/NoSuchFieldError  ], 467:[471:java/lang/NoSuchFieldError  ], 469:[471:java/lang/NoSuchFieldError  ], 472:[478:java/lang/NoSuchFieldError  ], 473:[478:java/lang/NoSuchFieldError  ], 474:[478:java/lang/NoSuchFieldError  ], 476:[478:java/lang/NoSuchFieldError  ], 479:[485:java/lang/NoSuchFieldError  ], 480:[485:java/lang/NoSuchFieldError  ], 481:[485:java/lang/NoSuchFieldError  ], 483:[485:java/lang/NoSuchFieldError  ], 490:[496:java/lang/NoSuchFieldError  ], 491:[496:java/lang/NoSuchFieldError  ], 492:[496:java/lang/NoSuchFieldError  ], 494:[496:java/lang/NoSuchFieldError  ], 497:[503:java/lang/NoSuchFieldError  ], 498:[503:java/lang/NoSuchFieldError  ], 499:[503:java/lang/NoSuchFieldError  ], 501:[503:java/lang/NoSuchFieldError  ], 504:[510:java/lang/NoSuchFieldError  ], 505:[510:java/lang/NoSuchFieldError  ], 506:[510:java/lang/NoSuchFieldError  ], 508:[510:java/lang/NoSuchFieldError  ], 511:[517:java/lang/NoSuchFieldError  ], 512:[517:java/lang/NoSuchFieldError  ], 513:[517:java/lang/NoSuchFieldError  ], 515:[517:java/lang/NoSuchFieldError  ], 518:[524:java/lang/NoSuchFieldError  ], 519:[524:java/lang/NoSuchFieldError  ], 520:[524:java/lang/NoSuchFieldError  ], 522:[524:java/lang/NoSuchFieldError  ], 529:[535:java/lang/NoSuchFieldError  ], 530:[535:java/lang/NoSuchFieldError  ], 531:[535:java/lang/NoSuchFieldError  ], 533:[535:java/lang/NoSuchFieldError  ], 536:[542:java/lang/NoSuchFieldError  ], 537:[542:java/lang/NoSuchFieldError  ], 538:[542:java/lang/NoSuchFieldError  ], 540:[542:java/lang/NoSuchFieldError  ], 543:[549:java/lang/NoSuchFieldError  ], 544:[549:java/lang/NoSuchFieldError  ], 545:[549:java/lang/NoSuchFieldError  ], 547:[549:java/lang/NoSuchFieldError  ], 550:[556:java/lang/NoSuchFieldError  ], 551:[556:java/lang/NoSuchFieldError  ], 552:[556:java/lang/NoSuchFieldError  ], 554:[556:java/lang/NoSuchFieldError  ], 557:[563:java/lang/NoSuchFieldError  ], 558:[563:java/lang/NoSuchFieldError  ], 559:[563:java/lang/NoSuchFieldError  ], 561:[563:java/lang/NoSuchFieldError  ], 568:[574:java/lang/NoSuchFieldError  ], 569:[574:java/lang/NoSuchFieldError  ], 570:[574:java/lang/NoSuchFieldError  ], 572:[574:java/lang/NoSuchFieldError  ], 575:[581:java/lang/NoSuchFieldError  ], 576:[581:java/lang/NoSuchFieldError  ], 577:[581:java/lang/NoSuchFieldError  ], 579:[581:java/lang/NoSuchFieldError  ], 582:[588:java/lang/NoSuchFieldError  ], 583:[588:java/lang/NoSuchFieldError  ], 584:[588:java/lang/NoSuchFieldError  ], 586:[588:java/lang/NoSuchFieldError  ], 589:[595:java/lang/NoSuchFieldError  ], 590:[595:java/lang/NoSuchFieldError  ], 591:[595:java/lang/NoSuchFieldError  ], 593:[595:java/lang/NoSuchFieldError  ], 596:[602:java/lang/NoSuchFieldError  ], 597:[602:java/lang/NoSuchFieldError  ], 598:[602:java/lang/NoSuchFieldError  ], 600:[602:java/lang/NoSuchFieldError  ], 603:[609:java/lang/NoSuchFieldError  ], 604:[609:java/lang/NoSuchFieldError  ], 605:[609:java/lang/NoSuchFieldError  ], 607:[609:java/lang/NoSuchFieldError  ], 610:[616:java/lang/NoSuchFieldError  ], 611:[616:java/lang/NoSuchFieldError  ], 612:[616:java/lang/NoSuchFieldError  ], 614:[616:java/lang/NoSuchFieldError  ], 621:[627:java/lang/NoSuchFieldError  ], 622:[627:java/lang/NoSuchFieldError  ], 623:[627:java/lang/NoSuchFieldError  ], 625:[627:java/lang/NoSuchFieldError  ], 628:[634:java/lang/NoSuchFieldError  ], 629:[634:java/lang/NoSuchFieldError  ], 630:[634:java/lang/NoSuchFieldError  ], 632:[634:java/lang/NoSuchFieldError  ], 635:[641:java/lang/NoSuchFieldError  ], 636:[641:java/lang/NoSuchFieldError  ], 637:[641:java/lang/NoSuchFieldError  ], 639:[641:java/lang/NoSuchFieldError  ], 646:[652:java/lang/NoSuchFieldError  ], 647:[652:java/lang/NoSuchFieldError  ], 648:[652:java/lang/NoSuchFieldError  ], 650:[652:java/lang/NoSuchFieldError  ], 653:[659:java/lang/NoSuchFieldError  ], 654:[659:java/lang/NoSuchFieldError  ], 655:[659:java/lang/NoSuchFieldError  ], 657:[659:java/lang/NoSuchFieldError  ], 660:[666:java/lang/NoSuchFieldError  ], 661:[666:java/lang/NoSuchFieldError  ], 662:[666:java/lang/NoSuchFieldError  ], 664:[666:java/lang/NoSuchFieldError  ], 667:[673:java/lang/NoSuchFieldError  ], 668:[673:java/lang/NoSuchFieldError  ], 669:[673:java/lang/NoSuchFieldError  ], 671:[673:java/lang/NoSuchFieldError  ], 674:[680:java/lang/NoSuchFieldError  ], 675:[680:java/lang/NoSuchFieldError  ], 676:[680:java/lang/NoSuchFieldError  ], 678:[680:java/lang/NoSuchFieldError  ], 681:[687:java/lang/NoSuchFieldError  ], 682:[687:java/lang/NoSuchFieldError  ], 683:[687:java/lang/NoSuchFieldError  ], 685:[687:java/lang/NoSuchFieldError  ], 688:[694:java/lang/NoSuchFieldError  ], 689:[694:java/lang/NoSuchFieldError  ], 690:[694:java/lang/NoSuchFieldError  ], 692:[694:java/lang/NoSuchFieldError  ], 695:[701:java/lang/NoSuchFieldError  ], 696:[701:java/lang/NoSuchFieldError  ], 697:[701:java/lang/NoSuchFieldError  ], 699:[701:java/lang/NoSuchFieldError  ], 706:[712:java/lang/NoSuchFieldError  ], 707:[712:java/lang/NoSuchFieldError  ], 708:[712:java/lang/NoSuchFieldError  ], 710:[712:java/lang/NoSuchFieldError  ], 713:[719:java/lang/NoSuchFieldError  ], 714:[719:java/lang/NoSuchFieldError  ], 715:[719:java/lang/NoSuchFieldError  ], 717:[719:java/lang/NoSuchFieldError  ], 720:[726:java/lang/NoSuchFieldError  ], 721:[726:java/lang/NoSuchFieldError  ], 722:[726:java/lang/NoSuchFieldError  ], 724:[726:java/lang/NoSuchFieldError  ], 727:[733:java/lang/NoSuchFieldError  ], 728:[733:java/lang/NoSuchFieldError  ], 729:[733:java/lang/NoSuchFieldError  ], 731:[733:java/lang/NoSuchFieldError  ], 738:[744:java/lang/NoSuchFieldError  ], 739:[744:java/lang/NoSuchFieldError  ], 740:[744:java/lang/NoSuchFieldError  ], 742:[744:java/lang/NoSuchFieldError  ], 745:[751:java/lang/NoSuchFieldError  ], 746:[751:java/lang/NoSuchFieldError  ], 747:[751:java/lang/NoSuchFieldError  ], 749:[751:java/lang/NoSuchFieldError  ], 752:[758:java/lang/NoSuchFieldError  ], 753:[758:java/lang/NoSuchFieldError  ], 754:[758:java/lang/NoSuchFieldError  ], 756:[758:java/lang/NoSuchFieldError  ], 759:[765:java/lang/NoSuchFieldError  ], 760:[765:java/lang/NoSuchFieldError  ], 761:[765:java/lang/NoSuchFieldError  ], 763:[765:java/lang/NoSuchFieldError  ], 766:[772:java/lang/NoSuchFieldError  ], 767:[772:java/lang/NoSuchFieldError  ], 768:[772:java/lang/NoSuchFieldError  ], 770:[772:java/lang/NoSuchFieldError  ], 777:[783:java/lang/NoSuchFieldError  ], 778:[783:java/lang/NoSuchFieldError  ], 779:[783:java/lang/NoSuchFieldError  ], 781:[783:java/lang/NoSuchFieldError  ], 784:[790:java/lang/NoSuchFieldError  ], 785:[790:java/lang/NoSuchFieldError  ], 786:[790:java/lang/NoSuchFieldError  ], 788:[790:java/lang/NoSuchFieldError  ], 791:[797:java/lang/NoSuchFieldError  ], 792:[797:java/lang/NoSuchFieldError  ], 793:[797:java/lang/NoSuchFieldError  ], 795:[797:java/lang/NoSuchFieldError  ]]
-INVOKESTATIC local/lito381/server/ClassicWorldMonsterPoisonAttack$Kind.values()[Llocal/lito381/server/ClassicWorldMonsterPoisonAttack$Kind;
+TRY [4:[10:java/lang/NoSuchFieldError  ], 5:[10:java/lang/NoSuchFieldError  ], 6:[10:java/lang/NoSuchFieldError  ], 8:[10:java/lang/NoSuchFieldError  ], 11:[17:java/lang/NoSuchFieldError  ], 12:[17:java/lang/NoSuchFieldError  ], 13:[17:java/lang/NoSuchFieldError  ], 15:[17:java/lang/NoSuchFieldError  ], 18:[24:java/lang/NoSuchFieldError  ], 19:[24:java/lang/NoSuchFieldError  ], 20:[24:java/lang/NoSuchFieldError  ], 22:[24:java/lang/NoSuchFieldError  ], 25:[31:java/lang/NoSuchFieldError  ], 26:[31:java/lang/NoSuchFieldError  ], 27:[31:java/lang/NoSuchFieldError  ], 29:[31:java/lang/NoSuchFieldError  ], 32:[38:java/lang/NoSuchFieldError  ], 33:[38:java/lang/NoSuchFieldError  ], 34:[38:java/lang/NoSuchFieldError  ], 36:[38:java/lang/NoSuchFieldError  ], 39:[45:java/lang/NoSuchFieldError  ], 40:[45:java/lang/NoSuchFieldError  ], 41:[45:java/lang/NoSuchFieldError  ], 43:[45:java/lang/NoSuchFieldError  ], 46:[52:java/lang/NoSuchFieldError  ], 47:[52:java/lang/NoSuchFieldError  ], 48:[52:java/lang/NoSuchFieldError  ], 50:[52:java/lang/NoSuchFieldError  ], 53:[59:java/lang/NoSuchFieldError  ], 54:[59:java/lang/NoSuchFieldError  ], 55:[59:java/lang/NoSuchFieldError  ], 57:[59:java/lang/NoSuchFieldError  ], 60:[66:java/lang/NoSuchFieldError  ], 61:[66:java/lang/NoSuchFieldError  ], 62:[66:java/lang/NoSuchFieldError  ], 64:[66:java/lang/NoSuchFieldError  ], 71:[77:java/lang/NoSuchFieldError  ], 72:[77:java/lang/NoSuchFieldError  ], 73:[77:java/lang/NoSuchFieldError  ], 75:[77:java/lang/NoSuchFieldError  ], 78:[84:java/lang/NoSuchFieldError  ], 79:[84:java/lang/NoSuchFieldError  ], 80:[84:java/lang/NoSuchFieldError  ], 82:[84:java/lang/NoSuchFieldError  ], 85:[91:java/lang/NoSuchFieldError  ], 86:[91:java/lang/NoSuchFieldError  ], 87:[91:java/lang/NoSuchFieldError  ], 89:[91:java/lang/NoSuchFieldError  ], 92:[98:java/lang/NoSuchFieldError  ], 93:[98:java/lang/NoSuchFieldError  ], 94:[98:java/lang/NoSuchFieldError  ], 96:[98:java/lang/NoSuchFieldError  ], 99:[105:java/lang/NoSuchFieldError  ], 100:[105:java/lang/NoSuchFieldError  ], 101:[105:java/lang/NoSuchFieldError  ], 103:[105:java/lang/NoSuchFieldError  ], 110:[116:java/lang/NoSuchFieldError  ], 111:[116:java/lang/NoSuchFieldError  ], 112:[116:java/lang/NoSuchFieldError  ], 114:[116:java/lang/NoSuchFieldError  ], 117:[123:java/lang/NoSuchFieldError  ], 118:[123:java/lang/NoSuchFieldError  ], 119:[123:java/lang/NoSuchFieldError  ], 121:[123:java/lang/NoSuchFieldError  ], 124:[130:java/lang/NoSuchFieldError  ], 125:[130:java/lang/NoSuchFieldError  ], 126:[130:java/lang/NoSuchFieldError  ], 128:[130:java/lang/NoSuchFieldError  ], 135:[141:java/lang/NoSuchFieldError  ], 136:[141:java/lang/NoSuchFieldError  ], 137:[141:java/lang/NoSuchFieldError  ], 139:[141:java/lang/NoSuchFieldError  ], 142:[148:java/lang/NoSuchFieldError  ], 143:[148:java/lang/NoSuchFieldError  ], 144:[148:java/lang/NoSuchFieldError  ], 146:[148:java/lang/NoSuchFieldError  ], 149:[155:java/lang/NoSuchFieldError  ], 150:[155:java/lang/NoSuchFieldError  ], 151:[155:java/lang/NoSuchFieldError  ], 153:[155:java/lang/NoSuchFieldError  ], 156:[162:java/lang/NoSuchFieldError  ], 157:[162:java/lang/NoSuchFieldError  ], 158:[162:java/lang/NoSuchFieldError  ], 160:[162:java/lang/NoSuchFieldError  ], 167:[173:java/lang/NoSuchFieldError  ], 168:[173:java/lang/NoSuchFieldError  ], 169:[173:java/lang/NoSuchFieldError  ], 171:[173:java/lang/NoSuchFieldError  ], 174:[180:java/lang/NoSuchFieldError  ], 175:[180:java/lang/NoSuchFieldError  ], 176:[180:java/lang/NoSuchFieldError  ], 178:[180:java/lang/NoSuchFieldError  ], 181:[187:java/lang/NoSuchFieldError  ], 182:[187:java/lang/NoSuchFieldError  ], 183:[187:java/lang/NoSuchFieldError  ], 185:[187:java/lang/NoSuchFieldError  ], 188:[194:java/lang/NoSuchFieldError  ], 189:[194:java/lang/NoSuchFieldError  ], 190:[194:java/lang/NoSuchFieldError  ], 192:[194:java/lang/NoSuchFieldError  ], 195:[201:java/lang/NoSuchFieldError  ], 196:[201:java/lang/NoSuchFieldError  ], 197:[201:java/lang/NoSuchFieldError  ], 199:[201:java/lang/NoSuchFieldError  ], 202:[208:java/lang/NoSuchFieldError  ], 203:[208:java/lang/NoSuchFieldError  ], 204:[208:java/lang/NoSuchFieldError  ], 206:[208:java/lang/NoSuchFieldError  ], 209:[215:java/lang/NoSuchFieldError  ], 210:[215:java/lang/NoSuchFieldError  ], 211:[215:java/lang/NoSuchFieldError  ], 213:[215:java/lang/NoSuchFieldError  ], 216:[222:java/lang/NoSuchFieldError  ], 217:[222:java/lang/NoSuchFieldError  ], 218:[222:java/lang/NoSuchFieldError  ], 220:[222:java/lang/NoSuchFieldError  ], 223:[229:java/lang/NoSuchFieldError  ], 224:[229:java/lang/NoSuchFieldError  ], 225:[229:java/lang/NoSuchFieldError  ], 227:[229:java/lang/NoSuchFieldError  ], 234:[240:java/lang/NoSuchFieldError  ], 235:[240:java/lang/NoSuchFieldError  ], 236:[240:java/lang/NoSuchFieldError  ], 238:[240:java/lang/NoSuchFieldError  ], 241:[247:java/lang/NoSuchFieldError  ], 242:[247:java/lang/NoSuchFieldError  ], 243:[247:java/lang/NoSuchFieldError  ], 245:[247:java/lang/NoSuchFieldError  ], 248:[254:java/lang/NoSuchFieldError  ], 249:[254:java/lang/NoSuchFieldError  ], 250:[254:java/lang/NoSuchFieldError  ], 252:[254:java/lang/NoSuchFieldError  ], 259:[265:java/lang/NoSuchFieldError  ], 260:[265:java/lang/NoSuchFieldError  ], 261:[265:java/lang/NoSuchFieldError  ], 263:[265:java/lang/NoSuchFieldError  ], 266:[272:java/lang/NoSuchFieldError  ], 267:[272:java/lang/NoSuchFieldError  ], 268:[272:java/lang/NoSuchFieldError  ], 270:[272:java/lang/NoSuchFieldError  ], 273:[279:java/lang/NoSuchFieldError  ], 274:[279:java/lang/NoSuchFieldError  ], 275:[279:java/lang/NoSuchFieldError  ], 277:[279:java/lang/NoSuchFieldError  ], 284:[290:java/lang/NoSuchFieldError  ], 285:[290:java/lang/NoSuchFieldError  ], 286:[290:java/lang/NoSuchFieldError  ], 288:[290:java/lang/NoSuchFieldError  ], 291:[297:java/lang/NoSuchFieldError  ], 292:[297:java/lang/NoSuchFieldError  ], 293:[297:java/lang/NoSuchFieldError  ], 295:[297:java/lang/NoSuchFieldError  ], 298:[304:java/lang/NoSuchFieldError  ], 299:[304:java/lang/NoSuchFieldError  ], 300:[304:java/lang/NoSuchFieldError  ], 302:[304:java/lang/NoSuchFieldError  ], 309:[315:java/lang/NoSuchFieldError  ], 310:[315:java/lang/NoSuchFieldError  ], 311:[315:java/lang/NoSuchFieldError  ], 313:[315:java/lang/NoSuchFieldError  ], 316:[322:java/lang/NoSuchFieldError  ], 317:[322:java/lang/NoSuchFieldError  ], 318:[322:java/lang/NoSuchFieldError  ], 320:[322:java/lang/NoSuchFieldError  ], 323:[329:java/lang/NoSuchFieldError  ], 324:[329:java/lang/NoSuchFieldError  ], 325:[329:java/lang/NoSuchFieldError  ], 327:[329:java/lang/NoSuchFieldError  ], 330:[336:java/lang/NoSuchFieldError  ], 331:[336:java/lang/NoSuchFieldError  ], 332:[336:java/lang/NoSuchFieldError  ], 334:[336:java/lang/NoSuchFieldError  ], 337:[343:java/lang/NoSuchFieldError  ], 338:[343:java/lang/NoSuchFieldError  ], 339:[343:java/lang/NoSuchFieldError  ], 341:[343:java/lang/NoSuchFieldError  ], 344:[350:java/lang/NoSuchFieldError  ], 345:[350:java/lang/NoSuchFieldError  ], 346:[350:java/lang/NoSuchFieldError  ], 348:[350:java/lang/NoSuchFieldError  ], 351:[357:java/lang/NoSuchFieldError  ], 352:[357:java/lang/NoSuchFieldError  ], 353:[357:java/lang/NoSuchFieldError  ], 355:[357:java/lang/NoSuchFieldError  ], 358:[364:java/lang/NoSuchFieldError  ], 359:[364:java/lang/NoSuchFieldError  ], 360:[364:java/lang/NoSuchFieldError  ], 362:[364:java/lang/NoSuchFieldError  ], 369:[375:java/lang/NoSuchFieldError  ], 370:[375:java/lang/NoSuchFieldError  ], 371:[375:java/lang/NoSuchFieldError  ], 373:[375:java/lang/NoSuchFieldError  ], 376:[382:java/lang/NoSuchFieldError  ], 377:[382:java/lang/NoSuchFieldError  ], 378:[382:java/lang/NoSuchFieldError  ], 380:[382:java/lang/NoSuchFieldError  ], 383:[389:java/lang/NoSuchFieldError  ], 384:[389:java/lang/NoSuchFieldError  ], 385:[389:java/lang/NoSuchFieldError  ], 387:[389:java/lang/NoSuchFieldError  ], 390:[396:java/lang/NoSuchFieldError  ], 391:[396:java/lang/NoSuchFieldError  ], 392:[396:java/lang/NoSuchFieldError  ], 394:[396:java/lang/NoSuchFieldError  ], 397:[403:java/lang/NoSuchFieldError  ], 398:[403:java/lang/NoSuchFieldError  ], 399:[403:java/lang/NoSuchFieldError  ], 401:[403:java/lang/NoSuchFieldError  ], 408:[414:java/lang/NoSuchFieldError  ], 409:[414:java/lang/NoSuchFieldError  ], 410:[414:java/lang/NoSuchFieldError  ], 412:[414:java/lang/NoSuchFieldError  ], 415:[421:java/lang/NoSuchFieldError  ], 416:[421:java/lang/NoSuchFieldError  ], 417:[421:java/lang/NoSuchFieldError  ], 419:[421:java/lang/NoSuchFieldError  ], 422:[428:java/lang/NoSuchFieldError  ], 423:[428:java/lang/NoSuchFieldError  ], 424:[428:java/lang/NoSuchFieldError  ], 426:[428:java/lang/NoSuchFieldError  ], 429:[435:java/lang/NoSuchFieldError  ], 430:[435:java/lang/NoSuchFieldError  ], 431:[435:java/lang/NoSuchFieldError  ], 433:[435:java/lang/NoSuchFieldError  ], 440:[446:java/lang/NoSuchFieldError  ], 441:[446:java/lang/NoSuchFieldError  ], 442:[446:java/lang/NoSuchFieldError  ], 444:[446:java/lang/NoSuchFieldError  ], 447:[453:java/lang/NoSuchFieldError  ], 448:[453:java/lang/NoSuchFieldError  ], 449:[453:java/lang/NoSuchFieldError  ], 451:[453:java/lang/NoSuchFieldError  ], 454:[460:java/lang/NoSuchFieldError  ], 455:[460:java/lang/NoSuchFieldError  ], 456:[460:java/lang/NoSuchFieldError  ], 458:[460:java/lang/NoSuchFieldError  ], 465:[471:java/lang/NoSuchFieldError  ], 466:[471:java/lang/NoSuchFieldError  ], 467:[471:java/lang/NoSuchFieldError  ], 469:[471:java/lang/NoSuchFieldError  ], 472:[478:java/lang/NoSuchFieldError  ], 473:[478:java/lang/NoSuchFieldError  ], 474:[478:java/lang/NoSuchFieldError  ], 476:[478:java/lang/NoSuchFieldError  ], 479:[485:java/lang/NoSuchFieldError  ], 480:[485:java/lang/NoSuchFieldError  ], 481:[485:java/lang/NoSuchFieldError  ], 483:[485:java/lang/NoSuchFieldError  ], 490:[496:java/lang/NoSuchFieldError  ], 491:[496:java/lang/NoSuchFieldError  ], 492:[496:java/lang/NoSuchFieldError  ], 494:[496:java/lang/NoSuchFieldError  ], 497:[503:java/lang/NoSuchFieldError  ], 498:[503:java/lang/NoSuchFieldError  ], 499:[503:java/lang/NoSuchFieldError  ], 501:[503:java/lang/NoSuchFieldError  ], 504:[510:java/lang/NoSuchFieldError  ], 505:[510:java/lang/NoSuchFieldError  ], 506:[510:java/lang/NoSuchFieldError  ], 508:[510:java/lang/NoSuchFieldError  ], 511:[517:java/lang/NoSuchFieldError  ], 512:[517:java/lang/NoSuchFieldError  ], 513:[517:java/lang/NoSuchFieldError  ], 515:[517:java/lang/NoSuchFieldError  ], 518:[524:java/lang/NoSuchFieldError  ], 519:[524:java/lang/NoSuchFieldError  ], 520:[524:java/lang/NoSuchFieldError  ], 522:[524:java/lang/NoSuchFieldError  ], 529:[535:java/lang/NoSuchFieldError  ], 530:[535:java/lang/NoSuchFieldError  ], 531:[535:java/lang/NoSuchFieldError  ], 533:[535:java/lang/NoSuchFieldError  ], 536:[542:java/lang/NoSuchFieldError  ], 537:[542:java/lang/NoSuchFieldError  ], 538:[542:java/lang/NoSuchFieldError  ], 540:[542:java/lang/NoSuchFieldError  ], 543:[549:java/lang/NoSuchFieldError  ], 544:[549:java/lang/NoSuchFieldError  ], 545:[549:java/lang/NoSuchFieldError  ], 547:[549:java/lang/NoSuchFieldError  ], 550:[556:java/lang/NoSuchFieldError  ], 551:[556:java/lang/NoSuchFieldError  ], 552:[556:java/lang/NoSuchFieldError  ], 554:[556:java/lang/NoSuchFieldError  ], 557:[563:java/lang/NoSuchFieldError  ], 558:[563:java/lang/NoSuchFieldError  ], 559:[563:java/lang/NoSuchFieldError  ], 561:[563:java/lang/NoSuchFieldError  ], 568:[574:java/lang/NoSuchFieldError  ], 569:[574:java/lang/NoSuchFieldError  ], 570:[574:java/lang/NoSuchFieldError  ], 572:[574:java/lang/NoSuchFieldError  ], 575:[581:java/lang/NoSuchFieldError  ], 576:[581:java/lang/NoSuchFieldError  ], 577:[581:java/lang/NoSuchFieldError  ], 579:[581:java/lang/NoSuchFieldError  ], 582:[588:java/lang/NoSuchFieldError  ], 583:[588:java/lang/NoSuchFieldError  ], 584:[588:java/lang/NoSuchFieldError  ], 586:[588:java/lang/NoSuchFieldError  ], 589:[595:java/lang/NoSuchFieldError  ], 590:[595:java/lang/NoSuchFieldError  ], 591:[595:java/lang/NoSuchFieldError  ], 593:[595:java/lang/NoSuchFieldError  ], 596:[602:java/lang/NoSuchFieldError  ], 597:[602:java/lang/NoSuchFieldError  ], 598:[602:java/lang/NoSuchFieldError  ], 600:[602:java/lang/NoSuchFieldError  ], 603:[609:java/lang/NoSuchFieldError  ], 604:[609:java/lang/NoSuchFieldError  ], 605:[609:java/lang/NoSuchFieldError  ], 607:[609:java/lang/NoSuchFieldError  ], 610:[616:java/lang/NoSuchFieldError  ], 611:[616:java/lang/NoSuchFieldError  ], 612:[616:java/lang/NoSuchFieldError  ], 614:[616:java/lang/NoSuchFieldError  ], 621:[627:java/lang/NoSuchFieldError  ], 622:[627:java/lang/NoSuchFieldError  ], 623:[627:java/lang/NoSuchFieldError  ], 625:[627:java/lang/NoSuchFieldError  ], 628:[634:java/lang/NoSuchFieldError  ], 629:[634:java/lang/NoSuchFieldError  ], 630:[634:java/lang/NoSuchFieldError  ], 632:[634:java/lang/NoSuchFieldError  ], 635:[641:java/lang/NoSuchFieldError  ], 636:[641:java/lang/NoSuchFieldError  ], 637:[641:java/lang/NoSuchFieldError  ], 639:[641:java/lang/NoSuchFieldError  ], 646:[652:java/lang/NoSuchFieldError  ], 647:[652:java/lang/NoSuchFieldError  ], 648:[652:java/lang/NoSuchFieldError  ], 650:[652:java/lang/NoSuchFieldError  ], 653:[659:java/lang/NoSuchFieldError  ], 654:[659:java/lang/NoSuchFieldError  ], 655:[659:java/lang/NoSuchFieldError  ], 657:[659:java/lang/NoSuchFieldError  ], 660:[666:java/lang/NoSuchFieldError  ], 661:[666:java/lang/NoSuchFieldError  ], 662:[666:java/lang/NoSuchFieldError  ], 664:[666:java/lang/NoSuchFieldError  ], 667:[673:java/lang/NoSuchFieldError  ], 668:[673:java/lang/NoSuchFieldError  ], 669:[673:java/lang/NoSuchFieldError  ], 671:[673:java/lang/NoSuchFieldError  ], 674:[680:java/lang/NoSuchFieldError  ], 675:[680:java/lang/NoSuchFieldError  ], 676:[680:java/lang/NoSuchFieldError  ], 678:[680:java/lang/NoSuchFieldError  ], 681:[687:java/lang/NoSuchFieldError  ], 682:[687:java/lang/NoSuchFieldError  ], 683:[687:java/lang/NoSuchFieldError  ], 685:[687:java/lang/NoSuchFieldError  ], 688:[694:java/lang/NoSuchFieldError  ], 689:[694:java/lang/NoSuchFieldError  ], 690:[694:java/lang/NoSuchFieldError  ], 692:[694:java/lang/NoSuchFieldError  ], 695:[701:java/lang/NoSuchFieldError  ], 696:[701:java/lang/NoSuchFieldError  ], 697:[701:java/lang/NoSuchFieldError  ], 699:[701:java/lang/NoSuchFieldError  ], 706:[712:java/lang/NoSuchFieldError  ], 707:[712:java/lang/NoSuchFieldError  ], 708:[712:java/lang/NoSuchFieldError  ], 710:[712:java/lang/NoSuchFieldError  ], 713:[719:java/lang/NoSuchFieldError  ], 714:[719:java/lang/NoSuchFieldError  ], 715:[719:java/lang/NoSuchFieldError  ], 717:[719:java/lang/NoSuchFieldError  ], 720:[726:java/lang/NoSuchFieldError  ], 721:[726:java/lang/NoSuchFieldError  ], 722:[726:java/lang/NoSuchFieldError  ], 724:[726:java/lang/NoSuchFieldError  ], 727:[733:java/lang/NoSuchFieldError  ], 728:[733:java/lang/NoSuchFieldError  ], 729:[733:java/lang/NoSuchFieldError  ], 731:[733:java/lang/NoSuchFieldError  ], 738:[744:java/lang/NoSuchFieldError  ], 739:[744:java/lang/NoSuchFieldError  ], 740:[744:java/lang/NoSuchFieldError  ], 742:[744:java/lang/NoSuchFieldError  ], 745:[751:java/lang/NoSuchFieldError  ], 746:[751:java/lang/NoSuchFieldError  ], 747:[751:java/lang/NoSuchFieldError  ], 749:[751:java/lang/NoSuchFieldError  ], 752:[758:java/lang/NoSuchFieldError  ], 753:[758:java/lang/NoSuchFieldError  ], 754:[758:java/lang/NoSuchFieldError  ], 756:[758:java/lang/NoSuchFieldError  ], 759:[765:java/lang/NoSuchFieldError  ], 760:[765:java/lang/NoSuchFieldError  ], 761:[765:java/lang/NoSuchFieldError  ], 763:[765:java/lang/NoSuchFieldError  ], 766:[772:java/lang/NoSuchFieldError  ], 767:[772:java/lang/NoSuchFieldError  ], 768:[772:java/lang/NoSuchFieldError  ], 770:[772:java/lang/NoSuchFieldError  ], 777:[783:java/lang/NoSuchFieldError  ], 778:[783:java/lang/NoSuchFieldError  ], 779:[783:java/lang/NoSuchFieldError  ], 781:[783:java/lang/NoSuchFieldError  ], 784:[790:java/lang/NoSuchFieldError  ], 785:[790:java/lang/NoSuchFieldError  ], 786:[790:java/lang/NoSuchFieldError  ], 788:[790:java/lang/NoSuchFieldError  ], 791:[797:java/lang/NoSuchFieldError  ], 792:[797:java/lang/NoSuchFieldError  ], 793:[797:java/lang/NoSuchFieldError  ], 795:[797:java/lang/NoSuchFieldError  ]]
+INVOKESTATIC local/lito381/server/ClientSession$Phase.values()[Llocal/lito381/server/ClientSession$Phase;
 ARRAYLENGTH
 NEWARRAY 10
 PUTSTATIC local/lito381/server/ClientSession$1.$SwitchMap$#20:[I
 GETSTATIC local/lito381/server/ClientSession$1.$SwitchMap$#20:[I
-GETSTATIC local/lito381/server/ClassicWorldMonsterPoisonAttack$Kind.DAMAGE:Llocal/lito381/server/ClassicWorldMonsterPoisonAttack$Kind;
-INVOKEVIRTUAL local/lito381/server/ClassicWorldMonsterPoisonAttack$Kind.ordinal()I
+GETSTATIC local/lito381/server/ClientSession$Phase.CHANNEL:Llocal/lito381/server/ClientSession$Phase;
+INVOKEVIRTUAL local/lito381/server/ClientSession$Phase.ordinal()I
 CONST Integer:1
 IASTORE
 GOTO L11
 ASTORE 0
 GETSTATIC local/lito381/server/ClientSession$1.$SwitchMap$#20:[I
-GETSTATIC local/lito381/server/ClassicWorldMonsterPoisonAttack$Kind.SILENCE:Llocal/lito381/server/ClassicWorldMonsterPoisonAttack$Kind;
-INVOKEVIRTUAL local/lito381/server/ClassicWorldMonsterPoisonAttack$Kind.ordinal()I
+GETSTATIC local/lito381/server/ClientSession$Phase.ROOM:Llocal/lito381/server/ClientSession$Phase;
+INVOKEVIRTUAL local/lito381/server/ClientSession$Phase.ordinal()I
 CONST Integer:2
 IASTORE
 GOTO L18
 ASTORE 1
 GETSTATIC local/lito381/server/ClientSession$1.$SwitchMap$#20:[I
-GETSTATIC local/lito381/server/ClassicWorldMonsterPoisonAttack$Kind.PARALYSIS:Llocal/lito381/server/ClassicWorldMonsterPoisonAttack$Kind;
-INVOKEVIRTUAL local/lito381/server/ClassicWorldMonsterPoisonAttack$Kind.ordinal()I
+GETSTATIC local/lito381/server/ClientSession$Phase.BATTLE:Llocal/lito381/server/ClientSession$Phase;
+INVOKEVIRTUAL local/lito381/server/ClientSession$Phase.ordinal()I
 CONST Integer:3
 IASTORE
 GOTO L25
 ASTORE 2
 GETSTATIC local/lito381/server/ClientSession$1.$SwitchMap$#20:[I
-GETSTATIC local/lito381/server/ClassicWorldMonsterPoisonAttack$Kind.NONE:Llocal/lito381/server/ClassicWorldMonsterPoisonAttack$Kind;
-INVOKEVIRTUAL local/lito381/server/ClassicWorldMonsterPoisonAttack$Kind.ordinal()I
+GETSTATIC local/lito381/server/ClientSession$Phase.RESULT:Llocal/lito381/server/ClientSession$Phase;
+INVOKEVIRTUAL local/lito381/server/ClientSession$Phase.ordinal()I
 CONST Integer:4
 IASTORE
 GOTO L32
 ASTORE 3
-INVOKESTATIC local/lito381/server/ClientSession$Phase.values()[Llocal/lito381/server/ClientSession$Phase;
-ARRAYLENGTH
-NEWARRAY 10
-PUTSTATIC local/lito381/server/ClientSession$1.$SwitchMap$#19:[I
-GETSTATIC local/lito381/server/ClientSession$1.$SwitchMap$#19:[I
-GETSTATIC local/lito381/server/ClientSession$Phase.CHANNEL:Llocal/lito381/server/ClientSession$Phase;
-INVOKEVIRTUAL local/lito381/server/ClientSession$Phase.ordinal()I
-CONST Integer:1
-IASTORE
-GOTO L43
-ASTORE 4
-GETSTATIC local/lito381/server/ClientSession$1.$SwitchMap$#19:[I
-GETSTATIC local/lito381/server/ClientSession$Phase.ROOM:Llocal/lito381/server/ClientSession$Phase;
-INVOKEVIRTUAL local/lito381/server/ClientSession$Phase.ordinal()I
-CONST Integer:2
-IASTORE
-GOTO L50
-ASTORE 5
-GETSTATIC local/lito381/server/ClientSession$1.$SwitchMap$#19:[I
-GETSTATIC local/lito381/server/ClientSession$Phase.BATTLE:Llocal/lito381/server/ClientSession$Phase;
-INVOKEVIRTUAL local/lito381/server/ClientSession$Phase.ordinal()I
-CONST Integer:3
-IASTORE
-GOTO L57
-ASTORE 6
-GETSTATIC local/lito381/server/ClientSession$1.$SwitchMap$#19:[I
-GETSTATIC local/lito381/server/ClientSession$Phase.RESULT:Llocal/lito381/server/ClientSession$Phase;
-INVOKEVIRTUAL local/lito381/server/ClientSession$Phase.ordinal()I
-CONST Integer:4
-IASTORE
-GOTO L64
-ASTORE 7
-GETSTATIC local/lito381/server/ClientSession$1.$SwitchMap$#19:[I
+GETSTATIC local/lito381/server/ClientSession$1.$SwitchMap$#20:[I
 GETSTATIC local/lito381/server/ClientSession$Phase.WORLD:Llocal/lito381/server/ClientSession$Phase;
 INVOKEVIRTUAL local/lito381/server/ClientSession$Phase.ordinal()I
 CONST Integer:5
 IASTORE
-GOTO L71
-ASTORE 8
-GETSTATIC local/lito381/server/ClientSession$1.$SwitchMap$#19:[I
+GOTO L39
+ASTORE 4
+GETSTATIC local/lito381/server/ClientSession$1.$SwitchMap$#20:[I
 GETSTATIC local/lito381/server/ClientSession$Phase.KEY_SENT:Llocal/lito381/server/ClientSession$Phase;
 INVOKEVIRTUAL local/lito381/server/ClientSession$Phase.ordinal()I
 CONST Integer:6
 IASTORE
+GOTO L46
+ASTORE 5
+GETSTATIC local/lito381/server/ClientSession$1.$SwitchMap$#20:[I
+GETSTATIC local/lito381/server/ClientSession$Phase.VERSION_OK:Llocal/lito381/server/ClientSession$Phase;
+INVOKEVIRTUAL local/lito381/server/ClientSession$Phase.ordinal()I
+CONST Integer:7
+IASTORE
+GOTO L53
+ASTORE 6
+GETSTATIC local/lito381/server/ClientSession$1.$SwitchMap$#20:[I
+GETSTATIC local/lito381/server/ClientSession$Phase.AUTHENTICATED:Llocal/lito381/server/ClientSession$Phase;
+INVOKEVIRTUAL local/lito381/server/ClientSession$Phase.ordinal()I
+CONST Integer:8
+IASTORE
+GOTO L60
+ASTORE 7
+GETSTATIC local/lito381/server/ClientSession$1.$SwitchMap$#20:[I
+GETSTATIC local/lito381/server/ClientSession$Phase.LOBBY:Llocal/lito381/server/ClientSession$Phase;
+INVOKEVIRTUAL local/lito381/server/ClientSession$Phase.ordinal()I
+CONST Integer:9
+IASTORE
+GOTO L67
+ASTORE 8
+INVOKESTATIC local/lito381/server/ClassicWorldDspyEscorts$TransitionKind.values()[Llocal/lito381/server/ClassicWorldDspyEscorts$TransitionKind;
+ARRAYLENGTH
+NEWARRAY 10
+PUTSTATIC local/lito381/server/ClientSession$1.$SwitchMap$#19:[I
+GETSTATIC local/lito381/server/ClientSession$1.$SwitchMap$#19:[I
+GETSTATIC local/lito381/server/ClassicWorldDspyEscorts$TransitionKind.NONE:Llocal/lito381/server/ClassicWorldDspyEscorts$TransitionKind;
+INVOKEVIRTUAL local/lito381/server/ClassicWorldDspyEscorts$TransitionKind.ordinal()I
+CONST Integer:1
+IASTORE
 GOTO L78
 ASTORE 9
 GETSTATIC local/lito381/server/ClientSession$1.$SwitchMap$#19:[I
-GETSTATIC local/lito381/server/ClientSession$Phase.VERSION_OK:Llocal/lito381/server/ClientSession$Phase;
-INVOKEVIRTUAL local/lito381/server/ClientSession$Phase.ordinal()I
-CONST Integer:7
+GETSTATIC local/lito381/server/ClassicWorldDspyEscorts$TransitionKind.MOVED:Llocal/lito381/server/ClassicWorldDspyEscorts$TransitionKind;
+INVOKEVIRTUAL local/lito381/server/ClassicWorldDspyEscorts$TransitionKind.ordinal()I
+CONST Integer:2
 IASTORE
 GOTO L85
 ASTORE 10
 GETSTATIC local/lito381/server/ClientSession$1.$SwitchMap$#19:[I
-GETSTATIC local/lito381/server/ClientSession$Phase.AUTHENTICATED:Llocal/lito381/server/ClientSession$Phase;
-INVOKEVIRTUAL local/lito381/server/ClientSession$Phase.ordinal()I
-CONST Integer:8
+GETSTATIC local/lito381/server/ClassicWorldDspyEscorts$TransitionKind.ABANDONED:Llocal/lito381/server/ClassicWorldDspyEscorts$TransitionKind;
+INVOKEVIRTUAL local/lito381/server/ClassicWorldDspyEscorts$TransitionKind.ordinal()I
+CONST Integer:3
 IASTORE
 GOTO L92
 ASTORE 11
 GETSTATIC local/lito381/server/ClientSession$1.$SwitchMap$#19:[I
-GETSTATIC local/lito381/server/ClientSession$Phase.LOBBY:Llocal/lito381/server/ClientSession$Phase;
-INVOKEVIRTUAL local/lito381/server/ClientSession$Phase.ordinal()I
-CONST Integer:9
+GETSTATIC local/lito381/server/ClassicWorldDspyEscorts$TransitionKind.ARRIVED:Llocal/lito381/server/ClassicWorldDspyEscorts$TransitionKind;
+INVOKEVIRTUAL local/lito381/server/ClassicWorldDspyEscorts$TransitionKind.ordinal()I
+CONST Integer:4
 IASTORE
 GOTO L99
 ASTORE 12
-INVOKESTATIC local/lito381/server/ClassicWorldDspyEscorts$TransitionKind.values()[Llocal/lito381/server/ClassicWorldDspyEscorts$TransitionKind;
+GETSTATIC local/lito381/server/ClientSession$1.$SwitchMap$#19:[I
+GETSTATIC local/lito381/server/ClassicWorldDspyEscorts$TransitionKind.DEAD_REMOVED:Llocal/lito381/server/ClassicWorldDspyEscorts$TransitionKind;
+INVOKEVIRTUAL local/lito381/server/ClassicWorldDspyEscorts$TransitionKind.ordinal()I
+CONST Integer:5
+IASTORE
+GOTO L106
+ASTORE 13
+INVOKESTATIC local/lito381/server/ClientSession$AttackEnvelope.values()[Llocal/lito381/server/ClientSession$AttackEnvelope;
 ARRAYLENGTH
 NEWARRAY 10
 PUTSTATIC local/lito381/server/ClientSession$1.$SwitchMap$#18:[I
 GETSTATIC local/lito381/server/ClientSession$1.$SwitchMap$#18:[I
-GETSTATIC local/lito381/server/ClassicWorldDspyEscorts$TransitionKind.NONE:Llocal/lito381/server/ClassicWorldDspyEscorts$TransitionKind;
-INVOKEVIRTUAL local/lito381/server/ClassicWorldDspyEscorts$TransitionKind.ordinal()I
-CONST Integer:1
-IASTORE
-GOTO L110
-ASTORE 13
-GETSTATIC local/lito381/server/ClientSession$1.$SwitchMap$#18:[I
-GETSTATIC local/lito381/server/ClassicWorldDspyEscorts$TransitionKind.MOVED:Llocal/lito381/server/ClassicWorldDspyEscorts$TransitionKind;
-INVOKEVIRTUAL local/lito381/server/ClassicWorldDspyEscorts$TransitionKind.ordinal()I
-CONST Integer:2
+GETSTATIC local/lito381/server/ClientSession$AttackEnvelope.DIRECT:Llocal/lito381/server/ClientSession$AttackEnvelope;
+INVOKEVIRTUAL local/lito381/server/ClientSession$AttackEnvelope.ordinal()I
+CONST Integer:1
 IASTORE
 GOTO L117
 ASTORE 14
 GETSTATIC local/lito381/server/ClientSession$1.$SwitchMap$#18:[I
-GETSTATIC local/lito381/server/ClassicWorldDspyEscorts$TransitionKind.ABANDONED:Llocal/lito381/server/ClassicWorldDspyEscorts$TransitionKind;
-INVOKEVIRTUAL local/lito381/server/ClassicWorldDspyEscorts$TransitionKind.ordinal()I
-CONST Integer:3
+GETSTATIC local/lito381/server/ClientSession$AttackEnvelope.FAR:Llocal/lito381/server/ClientSession$AttackEnvelope;
+INVOKEVIRTUAL local/lito381/server/ClientSession$AttackEnvelope.ordinal()I
+CONST Integer:2
 IASTORE
 GOTO L124
 ASTORE 15
 GETSTATIC local/lito381/server/ClientSession$1.$SwitchMap$#18:[I
-GETSTATIC local/lito381/server/ClassicWorldDspyEscorts$TransitionKind.ARRIVED:Llocal/lito381/server/ClassicWorldDspyEscorts$TransitionKind;
-INVOKEVIRTUAL local/lito381/server/ClassicWorldDspyEscorts$TransitionKind.ordinal()I
-CONST Integer:4
+GETSTATIC local/lito381/server/ClientSession$AttackEnvelope.ALTERNATE:Llocal/lito381/server/ClientSession$AttackEnvelope;
+INVOKEVIRTUAL local/lito381/server/ClientSession$AttackEnvelope.ordinal()I
+CONST Integer:3
 IASTORE
 GOTO L131
 ASTORE 16
-GETSTATIC local/lito381/server/ClientSession$1.$SwitchMap$#18:[I
-GETSTATIC local/lito381/server/ClassicWorldDspyEscorts$TransitionKind.DEAD_REMOVED:Llocal/lito381/server/ClassicWorldDspyEscorts$TransitionKind;
-INVOKEVIRTUAL local/lito381/server/ClassicWorldDspyEscorts$TransitionKind.ordinal()I
-CONST Integer:5
-IASTORE
-GOTO L138
-ASTORE 17
-INVOKESTATIC local/lito381/server/ClientSession$AttackEnvelope.values()[Llocal/lito381/server/ClientSession$AttackEnvelope;
+INVOKESTATIC local/lito381/server/ClassicWorldMonsterPoisonAttack$Kind.values()[Llocal/lito381/server/ClassicWorldMonsterPoisonAttack$Kind;
 ARRAYLENGTH
 NEWARRAY 10
 PUTSTATIC local/lito381/server/ClientSession$1.$SwitchMap$#17:[I
 GETSTATIC local/lito381/server/ClientSession$1.$SwitchMap$#17:[I
-GETSTATIC local/lito381/server/ClientSession$AttackEnvelope.DIRECT:Llocal/lito381/server/ClientSession$AttackEnvelope;
-INVOKEVIRTUAL local/lito381/server/ClientSession$AttackEnvelope.ordinal()I
-CONST Integer:1
+GETSTATIC local/lito381/server/ClassicWorldMonsterPoisonAttack$Kind.DAMAGE:Llocal/lito381/server/ClassicWorldMonsterPoisonAttack$Kind;
+INVOKEVIRTUAL local/lito381/server/ClassicWorldMonsterPoisonAttack$Kind.ordinal()I
+CONST Integer:1
+IASTORE
+GOTO L142
+ASTORE 17
+GETSTATIC local/lito381/server/ClientSession$1.$SwitchMap$#17:[I
+GETSTATIC local/lito381/server/ClassicWorldMonsterPoisonAttack$Kind.SILENCE:Llocal/lito381/server/ClassicWorldMonsterPoisonAttack$Kind;
+INVOKEVIRTUAL local/lito381/server/ClassicWorldMonsterPoisonAttack$Kind.ordinal()I
+CONST Integer:2
 IASTORE
 GOTO L149
 ASTORE 18
 GETSTATIC local/lito381/server/ClientSession$1.$SwitchMap$#17:[I
-GETSTATIC local/lito381/server/ClientSession$AttackEnvelope.FAR:Llocal/lito381/server/ClientSession$AttackEnvelope;
-INVOKEVIRTUAL local/lito381/server/ClientSession$AttackEnvelope.ordinal()I
-CONST Integer:2
+GETSTATIC local/lito381/server/ClassicWorldMonsterPoisonAttack$Kind.PARALYSIS:Llocal/lito381/server/ClassicWorldMonsterPoisonAttack$Kind;
+INVOKEVIRTUAL local/lito381/server/ClassicWorldMonsterPoisonAttack$Kind.ordinal()I
+CONST Integer:3
 IASTORE
 GOTO L156
 ASTORE 19
 GETSTATIC local/lito381/server/ClientSession$1.$SwitchMap$#17:[I
-GETSTATIC local/lito381/server/ClientSession$AttackEnvelope.ALTERNATE:Llocal/lito381/server/ClientSession$AttackEnvelope;
-INVOKEVIRTUAL local/lito381/server/ClientSession$AttackEnvelope.ordinal()I
-CONST Integer:3
+GETSTATIC local/lito381/server/ClassicWorldMonsterPoisonAttack$Kind.NONE:Llocal/lito381/server/ClassicWorldMonsterPoisonAttack$Kind;
+INVOKEVIRTUAL local/lito381/server/ClassicWorldMonsterPoisonAttack$Kind.ordinal()I
+CONST Integer:4
 IASTORE
 GOTO L163
 ASTORE 20
```

</details>

### `local/lito381/server/ClientSession#lambda$autoSelectPendingRentalBattleHandoff$30(Llocal/lito381/server/CharacterSummary;)Z`

<details>
<summary>完整差異（全部 hunks）</summary>

```diff
--- staged/local/lito381/server/ClientSession#lambda$autoSelectPendingRentalBattleHandoff$30(Llocal/lito381/server/CharacterSummary;)Z
+++ candidate/local/lito381/server/ClientSession#lambda$autoSelectPendingRentalBattleHandoff$30(Llocal/lito381/server/CharacterSummary;)Z
@@ -1,15 +1,15 @@
 META 4098 null []
 TRY []
+ALOAD 0
+GETFIELD local/lito381/server/ClientSession.hub:Llocal/lito381/server/GameHub;
+ALOAD 0
+INVOKEVIRTUAL local/lito381/server/ClientSession.accountName()Ljava/lang/String;
 ALOAD 0
 ALOAD 1
 INVOKEVIRTUAL local/lito381/server/CharacterSummary.name()Ljava/lang/String;
 INVOKESPECIAL local/lito381/server/ClientSession.stateCharacter(Ljava/lang/String;)Ljava/lang/String;
 ASTORE 2
-ALOAD 0
-INVOKEVIRTUAL local/lito381/server/ClientSession.accountName()Ljava/lang/String;
 ASTORE 3
-ALOAD 0
-GETFIELD local/lito381/server/ClientSession.hub:Llocal/lito381/server/GameHub;
 ASTORE 4
 ALOAD 4
 ALOAD 0
```

</details>
