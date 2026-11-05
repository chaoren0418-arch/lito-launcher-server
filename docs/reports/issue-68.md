# Issue #68 — T020-B07 戰鬥併入暫存

## 結論

2026-11-06 重驗結果為 **54 SAME、0 SAME_EFFECTS、0 SAME_PATHS、2 DIFF、0 ONLY_LEFT、0 ONLY_RIGHT**。23 份 staged Java 編譯成功，產生的 56 個 entry 與 B07 `verify_entries` 完全一致。**驗收尚未完成，交雲端主控／Claude 裁定剩餘兩個方法。**

本輪從 `task/68` 的 `a9f1f6f` 繼續，合併 `origin/main` 的 `c724539`，merge commit 為 `7c7bdd5`，包含 #174、#176。既有三方移植保留，本輪修改 `CombatRules.java`、`NativeAreaMaterialAction.java` 及本報告。

## 做了什麼

- `CombatRules.load` 保留 receiver、`DragonKnightAwakeClassicMode` key、`read(character)`、`NativePetControls.loadFromProperties`、`awakeClassicMode`、`put` 的求值順序。把 destination 的靜態型別由 `Map` 改為 `var` 推導的 `HashMap`，讓呼叫與候選一致地使用 `INVOKEVIRTUAL HashMap.put`，並合併重複的 Properties 暫存。該類現為 SAME。
- `NativeAreaMaterialAction.nativeTargetOriginalCaptureOutput` 的發布 lambda 採用連續 `&&` 短路運算式，保留 `NativeSourceIndirectPlayerProof.player`。兩條路徑在進入 allMatch 前均已成功讀取 `context.hub`、`context.population`，因此省去冗餘 `Objects.requireNonNull(context)`，讓 stream 直接作為 allMatch receiver。#174 對候選中的同一多餘檢查進行既有正規化後，整類為 SAME。
- 區域發布仍先鎖 hub，再視 population 決定內層鎖，send 在鎖區塊外。這是本輪採用的 Java 表達方式，已透過完整類別比較驗證。
- `NativeReturnNatureMaterialAction.Target` 的 `@AsRecord` 及 `NativeAreaMaterialAction.Context` 的 `@AsmFinal` 持續使用既有轉換器，兩個 entry 均 SAME。
- 依主控裁定保留 `CombatCompanionSpells`、`CombatNpcDebuffs`、`CombatPoisonLifecycle` 的 Java。Poison 已由新版工具驗成 SAME，另兩類各有一個 DIFF。

## 已驗證

環境為 Windows、Microsoft OpenJDK 17.0.17、Python 3.12、ASM 9.7。比較器與 AsRecord 均由本分支工具來源重新 javac。最終產物獨立建立於全新的 `target/issue-68/recheck/`，轉換前後均核對 56 個 class entry，轉換器只改動 Context 與 Target 的 bytes，打包內容逐 entry 與轉換後 classes 雜湊一致。

| 輪次 | SAME | DIFF | 說明 |
| --- | ---: | ---: | --- |
| 前次提交 | 51 | 5 | `a9f1f6f` 的既有紀錄 |
| 合併 main 後首輪 | 52 | 4 | Poison 通過 |
| 修正 CombatRules | 53 | 3 | HashMap 呼叫型別及 Properties 暫存對齊 |
| 修正區域發布並獨立重建 | 54 | 2 | 區域發布整類通過 |

### 最終命令與輸出

最終建置／比較入口為 `scratch/issue-68/recheck.py`，逐方法診斷入口為 `scratch/issue-68/remaining.py`。完整 argv、退出碼與日誌路徑保留在 `recheck-commands.json`、`remaining-commands.json`。以下指令從 worktree 根目錄執行。

```powershell
& 'C:/Users/CHAOREN/AppData/Local/Programs/Python/Python312/python.exe' -B -X utf8 scratch/issue-68/recheck.py
& 'C:/Users/CHAOREN/AppData/Local/Programs/Python/Python312/python.exe' -B -X utf8 scratch/issue-68/remaining.py
```

`recheck.py` 刻意要求新的輸出目錄。後續重跑可傳入未使用的目錄名稱，例如 `recheck-repeat`，產物將位於 `target/issue-68/recheck-repeat/`。本報告的驗證證據固定對應 `recheck/`，診斷腳本預設也讀取該目錄。

本輪 staged 編譯及轉換等同下列指令，`$sources` 精確由 23 個 B07 外層名稱產生。annotation classes 另行編譯供 classpath 使用。

```powershell
$server = Join-Path (Get-Location) 'lito伺服器/LiTo381-Server'
$jdk = 'C:/Program Files/Microsoft/jdk-17.0.17.10-hotspot/bin'
$out = Join-Path $server 'target/issue-68/recheck'
$batch = (Get-Content -Raw -Encoding UTF8 docs/combat-merge/batches.json | ConvertFrom-Json).batches |
  Where-Object id -eq 'B07'
$sources = @($batch.classes | ForEach-Object {
  Join-Path $server "merge-inputs/50635f7e/staged/local/lito381/server/$_.java"
})
$cp = "$server/reference/candidate-50635f7e.jar;$server/reference/base-0.6.71.jar;$out/annotations;$server/lib/*"
& "$jdk/javac.exe" --release 17 -encoding UTF-8 -cp $cp -d "$out/classes" @sources
$deps = 'D:/dev-cache/maven/repository/org/ow2/asm/asm/9.7/asm-9.7.jar;' +
        'D:/dev-cache/maven/repository/org/ow2/asm/asm-tree/9.7/asm-tree-9.7.jar;' +
        "$server/target/issue-68/asm-analysis-9.7.jar"
& "$jdk/java.exe" -Dfile.encoding=UTF-8 -cp "$out/as-record;$deps" local.lito381.asrecord.AsRecord "$out/classes"
# Python ZipFile 只寫入 classes 目錄的 56 個檔案，得到 $out/staged.jar。
& "$jdk/java.exe" -Dfile.encoding=UTF-8 -cp "$out/semantic-tool;$deps" local.lito381.semanticdiff.SemanticDiff `
  --left "$out/staged.jar" --right "$server/reference/candidate-50635f7e.jar" `
  --left-classpath "$server/reference/candidate-50635f7e.jar" `
  --classes scratch/issue-68/entries.txt `
  --json scratch/issue-68/recheck-default.json --md scratch/issue-68/recheck-default.md
# 線性診斷使用相同命令，額外傳 -Dsemanticdiff.disable=paths，輸出 recheck-linear.*。
```

正式驗收使用所有預設正規化與逐路徑比對。八份共用核准清單逐行裁切後，B07 交集全部為空，因而旗標清單為空，紀錄見 `approved-selection.json`。清單包括 effects、extra-methods、metadata-exempt、access-widen、layout、deviations、exceptions-exempt、T029。沒有適用的豁免條目。

#### recheck-javac.log

```text
Note: Some input files use unchecked or unsafe operations.

Note: Recompile with -Xlint:unchecked for details.


exit=0
```

#### recheck-as-record.log

```text
as-record: scanned=56 converted=2


exit=0
```

#### recheck-default.log

```text
SUMMARY SAME=54 SAME_EFFECTS=0 SAME_PATHS=0 ORPHAN_IGNORED=0 SAME_LAYOUT=0 DEVIATION=0 DIFF=2 ONLY_LEFT=0 ONLY_RIGHT=0 BYTE_SAME=0 ALLOW_METHODS=0

wrote D:\lineage開發\3-lito開發\scratch\wt\task-68\scratch\issue-68\recheck-default.json

wrote D:\lineage開發\3-lito開發\scratch\wt\task-68\scratch\issue-68\recheck-default.md


exit=0
```

#### recheck-linear.log

```text
SUMMARY SAME=54 SAME_EFFECTS=0 SAME_PATHS=0 ORPHAN_IGNORED=0 SAME_LAYOUT=0 DEVIATION=0 DIFF=2 ONLY_LEFT=0 ONLY_RIGHT=0 BYTE_SAME=0 ALLOW_METHODS=0

wrote D:\lineage開發\3-lito開發\scratch\wt\task-68\scratch\issue-68\recheck-linear.json

wrote D:\lineage開發\3-lito開發\scratch\wt\task-68\scratch\issue-68\recheck-linear.md


exit=0
```

classSelection 的 `missingLeft`、`extraLeft` 都為空。類別計數總和為 56，所有方法級 ONLY_LEFT／ONLY_RIGHT 也為 0。

### 完整 entry 結果

所有名稱共用前綴 `local/lito381/server/`，表中省略 `.class`。

| Entry | 結果 |
| --- | --- |
| `CombatAccess` | SAME |
| `CombatAccess$1` | SAME |
| `CombatCompanionSpells` | DIFF |
| `CombatCompanionSpells$Attempt` | SAME |
| `CombatCompanionSpells$Batch` | SAME |
| `CombatCompanionSpells$Hit` | SAME |
| `CombatDatabase` | SAME |
| `CombatDatabase$Skill` | SAME |
| `CombatDatabase$Snapshot` | SAME |
| `CombatHealing` | SAME |
| `CombatNpcDebuffs` | DIFF |
| `CombatNpcDebuffs$Focus` | SAME |
| `CombatPlayerDamage` | SAME |
| `CombatPlayerRecovery` | SAME |
| `CombatPoisonLifecycle` | SAME |
| `CombatPoisonLifecycle$Actor` | SAME |
| `CombatPoisonLifecycle$Context` | SAME |
| `CombatPoisonLifecycle$Damage` | SAME |
| `CombatPoisonLifecycle$Death` | SAME |
| `CombatPoisonLifecycle$Lease` | SAME |
| `CombatPoisonLifecycle$Palette` | SAME |
| `CombatPoisonLifecycle$ParalysisSnapshot` | SAME |
| `CombatPoisonLifecycle$Plan` | SAME |
| `CombatPoisonLifecycle$Removal` | SAME |
| `CombatPoisonLifecycle$Scope` | SAME |
| `CombatPoisonLifecycle$SilenceNotice` | SAME |
| `CombatPoisonLifecycle$Snapshot` | SAME |
| `CombatPoisonLifecycle$State` | SAME |
| `CombatPoisonLifecycle$Transition` | SAME |
| `CombatRules` | SAME |
| `CombatRules$1` | SAME |
| `CombatRules$Snapshot` | SAME |
| `CombatSpecialBuffs` | SAME |
| `CombatWorldCommit` | SAME |
| `CombatWorldCommit$Attempt` | SAME |
| `CombatWorldCommit$Doll` | SAME |
| `CombatWorldCommit$Physical` | SAME |
| `CombatWorldCommit$Plan` | SAME |
| `ForcedCombat` | SAME |
| `ForcedCombat$Intent` | SAME |
| `NativeAreaMaterialAction` | SAME |
| `NativeAreaMaterialAction$Context` | SAME |
| `NativeAreaMaterialAction$Npc` | SAME |
| `NativeCancellationMaterialAction` | SAME |
| `NativeEarthBindMaterialAction` | SAME |
| `NativeGroundMaterialAction` | SAME |
| `NativeGroundMaterialAction$Rejected` | SAME |
| `NativeNegativeMaterialAction` | SAME |
| `NativePetMaterialAction` | SAME |
| `NativePoisonMaterialAction` | SAME |
| `NativeResurrectionMaterialAction` | SAME |
| `NativeResurrectionMaterialAction$Target` | SAME |
| `NativeReturnNatureMaterialAction` | SAME |
| `NativeReturnNatureMaterialAction$Target` | SAME |
| `NativeTargetNegativeMaterialAction` | SAME |
| `NativeWeaponBreakMaterialAction` | SAME |

### 輸入、產物與證據雜湊

| 檔案 | SHA-256 |
| --- | --- |
| `reference/base-0.6.71.jar` | `e1498871c7be6b287821c57ef5d95933db1740d9b4d4a80f6c3465f21d99d498` |
| `reference/candidate-50635f7e.jar` | `50635f7edca8ec6ce77787cc32371c7590b497e48ea202f8a679caf3f517b729` |
| `target/issue-68/recheck/staged.jar` | `eb531bfca6a5e45223e31dec5e8e35f804ca7c5cb092faf1a833faee7555d884` |
| `scratch/issue-68/recheck-default.json` | `1cbed17ca3c73c2a8f52f80bb36b3ffa6e4a53077eb8a7e9c41f608a1f8759f4` |
| `scratch/issue-68/recheck-linear.json` | `f16f9ead0dc02014f49350cc5510d28ff939102300a7c3e1cc9ad015876af66c` |
| `scratch/issue-68/remaining.log` | `5297c700f3ab58e20e8fb16f1576044a03701c6bd3d74ffb7e721a5bdffda2ee` |
| `scratch/issue-68/recheck-commands.json` | `1759eb618c6dd36ca284c6dab67634d7e27b5d60f2ccd02bfa4fc3edef54e083` |
| `scratch/issue-68/remaining/CombatCompanionSpells.diff` | `957387aae1e741386ba1928d8314d9bdabdc5488ee09a3006a3d352fa787224e` |
| `scratch/issue-68/remaining/CombatNpcDebuffs.diff` | `cf8a5ec07c783e980ae8cdaa462a3445497c4c63dd15d3ac3bfbd5cdb1aebd2e` |
| `scratch/issue-68/Remaining.java` | `49f51a768553c0b9b62bb102612f5b894980edf945018706d1249873921b31a3` |
| `scratch/issue-68/recheck.py` | `bac3fc60a5d4a488bd2d52a2eb37477cd66a84aeb8b1bf82ba7c693ff1887ae9` |

23 份 staged 來源的驗證雜湊如下。

| Java | SHA-256 |
| --- | --- |
| `CombatAccess.java` | `2a023f2bb87d6b48238529fcf7ad72f5723f3ed11983173f01f245bf0712db7d` |
| `CombatCompanionSpells.java` | `42056b2239189c69888888e0ef6b22a15ea83ec404848e214c48cf3c96b23302` |
| `CombatDatabase.java` | `12af2b331389cbc52df14720d801806aca41e19034f30f7bb54e28037672b2f0` |
| `CombatHealing.java` | `97abc39621fca6e5509ad3748d6f56d06cd637ae966d401f1b199d2aa852ced6` |
| `CombatNpcDebuffs.java` | `35c3284d7d0ceef029f3de33096714ee9d13c762c2856dd93deb29cf915b6be6` |
| `CombatPlayerDamage.java` | `17f4b79a3bf32812e57d2b8071befea6b543be124d74f01f324ab9c897a1ec0f` |
| `CombatPlayerRecovery.java` | `e9bb381652dee32e7ffcca2d100149cc156421e59232535e1b3a564d4e015175` |
| `CombatPoisonLifecycle.java` | `f32e0f285b1dd25f1cf962e8e610929b108e82d5cc70668ddcee0a733fb159df` |
| `CombatRules.java` | `aef302bc71ba932339aad6b1a050bf9f824c14a916ebe7d20a542960a5788dd0` |
| `CombatSpecialBuffs.java` | `d5a266cf40a826174c1884c15ef517b34a025e27f97953dfeb3634f3734d2edf` |
| `CombatWorldCommit.java` | `7c4ae4138531158e03a056c8dd4e480a3c2313fd9bd8515711fcd0d225508888` |
| `ForcedCombat.java` | `3a7d913d18ff2121d8f011da3a657a7b451801ddf22f980e83044d90c0ce519d` |
| `NativeAreaMaterialAction.java` | `6b74611657837ceed22bd4a9773c2339b09d3be3ccae58e8748dce92279bfbfe` |
| `NativeCancellationMaterialAction.java` | `5eb825527f16f1c10201dcd546c69b4563e2a69e8713636d009ff1341d19fc97` |
| `NativeEarthBindMaterialAction.java` | `59334a54a9235fae1814bde97492e592607795681d30360d4e38333d33d487d7` |
| `NativeGroundMaterialAction.java` | `860f3fa1b9dd280cb19a4457d5d14877128ae9e2fcd0d92cb0353ad2de29fa50` |
| `NativeNegativeMaterialAction.java` | `b919963329d4abe3984422dde14707d783b7399cd9321aed8d20e7d343c2f9db` |
| `NativePetMaterialAction.java` | `f920b7044d7ac42c7d086daf09f1491b5b7895ef7219e233cfdf0d08c4ecddb4` |
| `NativePoisonMaterialAction.java` | `259372e370d1f2dac35955a2fc883297450f80590d1166fdb1b51910e67a052e` |
| `NativeResurrectionMaterialAction.java` | `c4deb35fe225a4c11dd03c3fc3a455672a3b8db2dd08b8ffc1696ac853a3dac4` |
| `NativeReturnNatureMaterialAction.java` | `a79caf75d133da88b3cc796ab398256e4afd3cedd6596666bb1e5ac6e6e0723a` |
| `NativeTargetNegativeMaterialAction.java` | `d2225c30232ba4bab5bf003ab5111d06e9ff53286df16640521ae1976474f137` |
| `NativeWeaponBreakMaterialAction.java` | `0ceb73256e832ba2184bcd8f5f3845a152aff2cea75782834a110bea02924b23` |

## 剩餘差異與逐路徑結果

- `CombatCompanionSpells.nativeShockCompanionBody` 的第一差異為左側 `581:[597:java/lang/Throwable]`，右側 `194:[324:java/lang/Throwable]`。候選在早期 admission 區間仍有已移除 caster monitor 的 POP／catch-rethrow 結構，左側只有後段 controlled monitor 的例外涵蓋。
- `CombatNpcDebuffs.nativeMaterialControlledNegativeBody` 的第一差異為左側 `552:[592:java/lang/Throwable]`，右側 `293:[484:java/lang/Throwable]`。同樣涉及早期保護區段、handler 及堆疊運送。
- 兩個方法的逐路徑結果均為 `paths rejected: normal control-flow cycle`。直接呼叫目前工具的 `PathComparison.compare`，連左／左、右／右自比也都因相同理由被拒絕。原始方法包含傷害擲骰或收件者遍歷等正常迴圈，超出 #176 的無迴圈證明範圍。

附錄 A、B 由完整正規化 exception coverage 與 instructions 輸出建立 unified diff，保留全部差異 hunk，只有相同區段使用三行 context。完整兩側方法文字留在 `scratch/issue-68/remaining/`。以下是逐方法線性診斷與直接逐路徑比對的完整 stdout。

```text
local/lito381/server/CombatCompanionSpells#nativeShockCompanionBody(Llocal/lito381/server/ClientSession;[BLlocal/lito381/server/BattleSpellCatalog$ShockStunSpell;Ljava/util/function/IntUnaryOperator;Ljava/lang/Object;)Z

linear=method nativeShockCompanionBody(Llocal/lito381/server/ClientSession;[BLlocal/lito381/server/BattleSpellCatalog$ShockStunSpell;Ljava/util/function/IntUnaryOperator;Ljava/lang/Object;)Z: left=581:[597:java/lang/Throwable  ] right=194:[324:java/lang/Throwable  ]; paths disabled

paths left/right=Result[same=false, reason=paths rejected: normal control-flow cycle]

paths left/left=Result[same=false, reason=paths rejected: normal control-flow cycle]

paths right/right=Result[same=false, reason=paths rejected: normal control-flow cycle]

local/lito381/server/CombatNpcDebuffs#nativeMaterialControlledNegativeBody(Llocal/lito381/server/ClientSession;[BLlocal/lito381/server/BattleSpellCatalog$TargetNegativeStatusSpell;ILlocal/lito381/server/CombatNpcDebuffs$Focus;Z)Z

linear=method nativeMaterialControlledNegativeBody(Llocal/lito381/server/ClientSession;[BLlocal/lito381/server/BattleSpellCatalog$TargetNegativeStatusSpell;ILlocal/lito381/server/CombatNpcDebuffs$Focus;Z)Z: left=552:[592:java/lang/Throwable  ] right=293:[484:java/lang/Throwable  ]; paths disabled

paths left/right=Result[same=false, reason=paths rejected: normal control-flow cycle]

paths left/left=Result[same=false, reason=paths rejected: normal control-flow cycle]

paths right/right=Result[same=false, reason=paths rejected: normal control-flow cycle]


exit=0
```

## 推測

剩餘兩個方法可能需要比較器進一步處理移除 monitor 後留下的 catch-rethrow 與正常迴圈。現有線性與逐路徑結果仍為 DIFF，因此等價性尚待主控裁定與後續工具證明。

## 待驗證／未驗證什麼

- 兩個方法的完整語意等價性仍待驗證，B07 尚未達成 56 個 entry 全數通過。
- 單元測試與行為回歸依 §6-5 留待 B-land。
- 登入、戰鬥、正常退出的真機驗收及正式部署留待後續指定階段。
- GitHub PR 的一般 CI 與本批 staged 對候選的驗收分別記錄，本報告以獨立 56-entry 結果判定。

## 產出路徑

- 交付來源位於 `lito伺服器/LiTo381-Server/merge-inputs/50635f7e/staged/local/lito381/server/` 的 B07 23 份 Java。
- 本輪更新兩份來源與 `docs/reports/issue-68.md`。
- 暫存命令、日誌、完整 JSON、差異與診斷程式位於 `scratch/issue-68/`。
- 最終編譯產物位於 `lito伺服器/LiTo381-Server/target/issue-68/recheck/`。

## 給下一棒的提示

請雲端主控／Claude 依附錄審查兩個保留方法的完整差異，裁定比較器後續工作。#176 明確拒絕正常控制流程中的迴圈，本批兩個方法實際含迴圈。請接手時維持 callback／monitor／leave 與清理的原有語意，依裁定保留這兩份 Java。

工具更新後，從 23 份 staged Java 重編譯，僅執行既有 AsRecord 轉換器，再驗證全部 56 entries。完成條件為 SAME／SAME_EFFECTS／SAME_PATHS 合計 56，DIFF／ONLY_LEFT／ONLY_RIGHT 及 missingLeft／extraLeft 均為零。PR #92 同步本輪 54 SAME／2 DIFF，Issue #68 以 `needs:claude` 交接。

## 附錄 A — CombatCompanionSpells 完整正規化差異

```diff
--- CombatCompanionSpells staged
+++ CombatCompanionSpells candidate
@@ -1,9 +1,41 @@
 # exception coverage
-581:[597:java/lang/Throwable  ]
-587:[597:java/lang/Throwable  ]
-588:[597:java/lang/Throwable  ]
-595:[597:java/lang/Throwable  ]
-599:[597:java/lang/Throwable  ]
+194:[324:java/lang/Throwable  ]
+198:[324:java/lang/Throwable  ]
+199:[324:java/lang/Throwable  ]
+203:[324:java/lang/Throwable  ]
+207:[324:java/lang/Throwable  ]
+211:[324:java/lang/Throwable  ]
+216:[324:java/lang/Throwable  ]
+221:[324:java/lang/Throwable  ]
+232:[324:java/lang/Throwable  ]
+234:[324:java/lang/Throwable  ]
+235:[324:java/lang/Throwable  ]
+236:[324:java/lang/Throwable  ]
+237:[324:java/lang/Throwable  ]
+251:[324:java/lang/Throwable  ]
+258:[324:java/lang/Throwable  ]
+261:[324:java/lang/Throwable  ]
+263:[324:java/lang/Throwable  ]
+269:[324:java/lang/Throwable  ]
+273:[324:java/lang/Throwable  ]
+274:[324:java/lang/Throwable  ]
+287:[324:java/lang/Throwable  ]
+288:[324:java/lang/Throwable  ]
+293:[324:java/lang/Throwable  ]
+296:[324:java/lang/Throwable  ]
+297:[324:java/lang/Throwable  ]
+302:[324:java/lang/Throwable  ]
+306:[324:java/lang/Throwable  ]
+307:[324:java/lang/Throwable  ]
+312:[324:java/lang/Throwable  ]
+317:[324:java/lang/Throwable  ]
+318:[324:java/lang/Throwable  ]
+319:[324:java/lang/Throwable  ]
+597:[613:java/lang/Throwable  ]
+603:[613:java/lang/Throwable  ]
+604:[613:java/lang/Throwable  ]
+611:[613:java/lang/Throwable  ]
+615:[613:java/lang/Throwable  ]
 # instructions
 INVOKESTATIC local/lito381/server/CombatRules.loaded()Z
 IFNE L4
@@ -195,43 +227,47 @@
 ASTORE 26
 CONST null
 ASTORE 27
+ALOAD 0
+DUP
+ASTORE 28
+POP
 INVOKESTATIC java/lang/System.nanoTime()J
-LSTORE 28/29
+LSTORE 29/30
 ALOAD 0
 CONST str:"spellReadyNanos"
 INVOKESTATIC local/lito381/server/CombatAccess.field(Ljava/lang/Object;Ljava/lang/String;)Ljava/lang/Object;
 CHECKCAST java/util/Map
-ASTORE 30
+ASTORE 31
 ALOAD 0
 CONST str:"playerHp"
 INVOKESTATIC local/lito381/server/CombatCompanionSpells.integer(Llocal/lito381/server/ClientSession;Ljava/lang/String;)I
-ISTORE 31
+ISTORE 32
 ALOAD 0
 CONST str:"playerMp"
 INVOKESTATIC local/lito381/server/CombatCompanionSpells.integer(Llocal/lito381/server/ClientSession;Ljava/lang/String;)I
-ISTORE 32
+ISTORE 33
 ALOAD 0
 ALOAD 6
 INVOKESTATIC local/lito381/server/NativeShockCompanionAction.available(Llocal/lito381/server/ClientSession;Ljava/lang/Object;)Z
-IFEQ L236
+IFEQ L240
 ILOAD 14
 ALOAD 0
 CONST str:"positionX"
 INVOKESTATIC local/lito381/server/CombatCompanionSpells.integer(Llocal/lito381/server/ClientSession;Ljava/lang/String;)I
-IF_ICMPNE L236
+IF_ICMPNE L240
 ILOAD 15
 ALOAD 0
 CONST str:"positionY"
 INVOKESTATIC local/lito381/server/CombatCompanionSpells.integer(Llocal/lito381/server/ClientSession;Ljava/lang/String;)I
-IF_ICMPNE L236
-ILOAD 31
+IF_ICMPNE L240
+ILOAD 32
 ILOAD 23
-IF_ICMPLE L236
-ILOAD 32
+IF_ICMPLE L240
+ILOAD 33
 ILOAD 22
-IF_ICMPLT L236
-LLOAD 28/29
-ALOAD 30
+IF_ICMPLT L240
+LLOAD 29/30
+ALOAD 31
 CONST Integer:87
 INVOKESTATIC java/lang/Integer.valueOf(I)Ljava/lang/Integer;
 CONST Long:0
@@ -240,28 +276,32 @@
 CHECKCAST java/lang/Long
 INVOKEVIRTUAL java/lang/Long.longValue()J
 LCMP
-IFGE L238
-CONST Integer:1
+IFGE L244
+CONST Integer:1
+ALOAD 28
+POP
 IRETURN
 CONST null
-ASTORE 33
+ASTORE 34
 CONST null
-ASTORE 34
+ASTORE 35
 ALOAD 0
 ALOAD 6
 ALOAD 2
 INVOKESTATIC local/lito381/server/NativeShockCompanionAction.admit(Llocal/lito381/server/ClientSession;Ljava/lang/Object;Llocal/lito381/server/BattleSpellCatalog$ShockStunSpell;)Z
-IFNE L249
-CONST Integer:1
+IFNE L257
+CONST Integer:1
+ALOAD 28
+POP
 IRETURN
 ALOAD 0
 INVOKESTATIC local/lito381/server/NativeCastMaterial.previous(Llocal/lito381/server/ClientSession;)Llocal/lito381/server/RuntimeInventory;
-ASTORE 35
+ASTORE 36
 ALOAD 0
 INVOKESTATIC local/lito381/server/NativeCastMaterial.next(Llocal/lito381/server/ClientSession;)Llocal/lito381/server/RuntimeInventory;
-ASTORE 36
+ASTORE 37
 INVOKESTATIC java/lang/System.nanoTime()J
-LSTORE 37/38
+LSTORE 38/39
 ALOAD 0
 CONST str:"cancelMeditation"
 CONST str:"(Ljava/lang/String;)V"
@@ -273,26 +313,26 @@
 AASTORE
 INVOKESTATIC local/lito381/server/CombatAccess.call(Ljava/lang/Object;Ljava/lang/String;Ljava/lang/String;[Ljava/lang/Object;)Ljava/lang/Object;
 POP
-ILOAD 31
+ILOAD 32
 ILOAD 23
 ISUB
-ISTORE 39
-ILOAD 32
+ISTORE 40
+ILOAD 33
 ILOAD 22
 ISUB
-ISTORE 40
+ISTORE 41
 ALOAD 0
 CONST str:"playerHp"
-ILOAD 39
+ILOAD 40
 INVOKESTATIC java/lang/Integer.valueOf(I)Ljava/lang/Integer;
 INVOKESTATIC local/lito381/server/CombatAccess.set(Ljava/lang/Object;Ljava/lang/String;Ljava/lang/Object;)V
 ALOAD 0
 CONST str:"playerMp"
 ALOAD 0
-ILOAD 40
+ILOAD 41
 INVOKESTATIC local/lito381/server/NativeSkillConsumption.boundMp(Llocal/lito381/server/ClientSession;I)I
-DUP
-ISTORE 41
+ISTORE 42
+ILOAD 42
 INVOKESTATIC java/lang/Integer.valueOf(I)Ljava/lang/Integer;
 INVOKESTATIC local/lito381/server/CombatAccess.set(Ljava/lang/Object;Ljava/lang/String;Ljava/lang/Object;)V
 ALOAD 0
@@ -307,7 +347,7 @@
 AASTORE
 DUP
 CONST Integer:1
-LLOAD 37/38
+LLOAD 38/39
 ALOAD 2
 INVOKEVIRTUAL local/lito381/server/BattleSpellCatalog$ShockStunSpell.reuseDelayMillis()I
 I2L
@@ -318,8 +358,16 @@
 AASTORE
 INVOKESTATIC local/lito381/server/CombatAccess.call(Ljava/lang/Object;Ljava/lang/String;Ljava/lang/String;[Ljava/lang/Object;)Ljava/lang/Object;
 POP
-ALOAD 35
-IFNULL L330
+ALOAD 28
+POP
+GOTO L329
+ASTORE 43
+ALOAD 28
+POP
+ALOAD 43
+ATHROW
+ALOAD 36
+IFNULL L346
 ALOAD 0
 CONST str:"sendInventoryDelta"
 CONST str:"(Llocal/lito381/server/RuntimeInventory;Llocal/lito381/server/RuntimeInventory;)V"
@@ -327,37 +375,37 @@
 ANEWARRAY java/lang/Object
 DUP
 CONST Integer:0
-ALOAD 35
-AASTORE
-DUP
-CONST Integer:1
 ALOAD 36
 AASTORE
+DUP
+CONST Integer:1
+ALOAD 37
+AASTORE
 INVOKESTATIC local/lito381/server/CombatAccess.call(Ljava/lang/Object;Ljava/lang/String;Ljava/lang/String;[Ljava/lang/Object;)Ljava/lang/Object;
 POP
 ALOAD 2
 INVOKEVIRTUAL local/lito381/server/BattleSpellCatalog$ShockStunSpell.baseDamage()I
-ISTORE 42
-CONST Integer:0
-ISTORE 43
-ILOAD 43
+ISTORE 44
+CONST Integer:0
+ISTORE 45
+ILOAD 45
 ALOAD 2
 INVOKEVIRTUAL local/lito381/server/BattleSpellCatalog$ShockStunSpell.diceCount()I
-IF_ICMPGE L348
-ILOAD 42
+IF_ICMPGE L364
+ILOAD 44
 ALOAD 3
 ALOAD 2
 INVOKEVIRTUAL local/lito381/server/BattleSpellCatalog$ShockStunSpell.dieSides()I
 INVOKESTATIC local/lito381/server/CombatCompanionSpells.checkedRoll(Ljava/util/function/IntUnaryOperator;I)I
 INVOKESTATIC java/lang/Math.addExact(II)I
-ISTORE 42
-IINC var=43 incr=1
-GOTO L335
+ISTORE 44
+IINC var=45 incr=1
+GOTO L351
 ALOAD 0
 CONST Integer:87
 ALOAD 2
 INVOKEVIRTUAL local/lito381/server/BattleSpellCatalog$ShockStunSpell.attribute()I
-ILOAD 42
+ILOAD 44
 CONST Integer:0
 ALOAD 26
 INVOKEVIRTUAL local/lito381/server/CombatCompanionSpells$Attempt.magicResistance()I
@@ -367,13 +415,13 @@
 INVOKEDYNAMIC makeConcatWithConstants(J)Ljava/lang/String; bsm=H6 java/lang/invoke/StringConcatFactory.makeConcatWithConstants(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;Ljava/lang/String;[Ljava/lang/Object;)Ljava/lang/invoke/CallSite; args=[str:"battle-Shock-Stun-companion=\u0001"]
 ALOAD 8
 INVOKESTATIC local/lito381/server/CombatCompanionDamage.ordinaryComplete(Llocal/lito381/server/ClientSession;IIIIIILjava/lang/String;Ljava/lang/Object;)I
-ISTORE 44
+ISTORE 46
 ALOAD 8
-LLOAD 37/38
+LLOAD 38/39
 INVOKEVIRTUAL local/lito381/server/BattleCompanion.earthBindActive(J)Z
-IFEQ L369
-CONST Integer:0
-ISTORE 44
+IFEQ L385
+CONST Integer:0
+ISTORE 46
 ILOAD 14
 ILOAD 15
 ALOAD 9
@@ -381,21 +429,21 @@
 ALOAD 9
 INVOKEVIRTUAL local/lito381/server/BattleCompanion$Snapshot.y()I
 INVOKESTATIC local/lito381/server/CombatCompanionSpells.heading(IIII)I
-ISTORE 45
+ISTORE 47
 ALOAD 11
 LLOAD 12/13
 INVOKEVIRTUAL local/lito381/server/GameHub.roomMembers(J)Ljava/util/List;
-ASTORE 46
+ASTORE 48
 ALOAD 0
 CONST str:"playerHeading"
 INVOKESTATIC local/lito381/server/CombatCompanionSpells.integer(Llocal/lito381/server/ClientSession;Ljava/lang/String;)I
 CONST Integer:7
 IAND
-ILOAD 45
-IF_ICMPEQ L429
+ILOAD 47
+IF_ICMPEQ L445
 ALOAD 0
 CONST str:"playerHeading"
-ILOAD 45
+ILOAD 47
 INVOKESTATIC java/lang/Integer.valueOf(I)Ljava/lang/Integer;
 INVOKESTATIC local/lito381/server/CombatAccess.set(Ljava/lang/Object;Ljava/lang/String;Ljava/lang/Object;)V
 NEW local/lito381/server/Protocol$Writer
@@ -406,84 +454,84 @@
 ALOAD 0
 INVOKEVIRTUAL local/lito381/server/ClientSession.playerObjectId()J
 INVOKEVIRTUAL local/lito381/server/Protocol$Writer.d(J)Llocal/lito381/server/Protocol$Writer;
-ILOAD 45
+ILOAD 47
 INVOKEVIRTUAL local/lito381/server/Protocol$Writer.c(I)Llocal/lito381/server/Protocol$Writer;
 INVOKEVIRTUAL local/lito381/server/Protocol$Writer.bytes()[B
-ASTORE 47
-ALOAD 46
+ASTORE 49
+ALOAD 48
 INVOKEINTERFACE java/util/List.iterator()Ljava/util/Iterator; itf
-ASTORE 48
-ALOAD 48
+ASTORE 50
+ALOAD 50
 INVOKEINTERFACE java/util/Iterator.hasNext()Z itf
-IFEQ L429
-ALOAD 48
+IFEQ L445
+ALOAD 50
 INVOKEINTERFACE java/util/Iterator.next()Ljava/lang/Object; itf
 CHECKCAST local/lito381/server/ClientSession
-ASTORE 49
+ASTORE 51
 ALOAD 0
 CONST str:"invisible"
 INVOKESTATIC local/lito381/server/CombatAccess.field(Ljava/lang/Object;Ljava/lang/String;)Ljava/lang/Object;
 CHECKCAST java/lang/Boolean
 INVOKEVIRTUAL java/lang/Boolean.booleanValue()Z
-IFEQ L424
+IFEQ L440
+ALOAD 51
+ALOAD 0
+IF_ACMPNE L444
+ALOAD 51
 ALOAD 49
-ALOAD 0
-IF_ACMPNE L428
-ALOAD 49
-ALOAD 47
 CONST str:"S_CHANGE_DIRECTION controlled Shock Stun"
 INVOKESTATIC local/lito381/server/CombatCompanionSpells.send(Llocal/lito381/server/ClientSession;[BLjava/lang/String;)V
-GOTO L408
+GOTO L424
 ILOAD 18
 ALOAD 0
 INVOKEVIRTUAL local/lito381/server/ClientSession.playerObjectId()J
 ALOAD 9
 INVOKEVIRTUAL local/lito381/server/BattleCompanion$Snapshot.objectId()J
-ILOAD 44
-ILOAD 45
+ILOAD 46
+ILOAD 47
 INVOKESTATIC local/lito381/server/Protocol.shockStunAttack(IJJII)[B
-ASTORE 50
+ASTORE 52
 ALOAD 9
 INVOKEVIRTUAL local/lito381/server/BattleCompanion$Snapshot.objectId()J
 ALOAD 2
 INVOKEVIRTUAL local/lito381/server/BattleSpellCatalog$ShockStunSpell.impactGfx()I
 INVOKESTATIC local/lito381/server/Protocol.effectObject(JI)[B
-ASTORE 51
-ALOAD 46
+ASTORE 53
+ALOAD 48
 INVOKEINTERFACE java/util/List.iterator()Ljava/util/Iterator; itf
-ASTORE 52
-ALOAD 52
+ASTORE 54
+ALOAD 54
 INVOKEINTERFACE java/util/Iterator.hasNext()Z itf
-IFEQ L477
-ALOAD 52
+IFEQ L493
+ALOAD 54
 INVOKEINTERFACE java/util/Iterator.next()Ljava/lang/Object; itf
 CHECKCAST local/lito381/server/ClientSession
-ASTORE 53
+ASTORE 55
 ALOAD 0
 CONST str:"invisible"
 INVOKESTATIC local/lito381/server/CombatAccess.field(Ljava/lang/Object;Ljava/lang/String;)Ljava/lang/Object;
 CHECKCAST java/lang/Boolean
 INVOKEVIRTUAL java/lang/Boolean.booleanValue()Z
-IFEQ L463
-ALOAD 53
-ALOAD 0
-IF_ACMPNE L470
-ALOAD 53
-ALOAD 50
+IFEQ L479
+ALOAD 55
+ALOAD 0
+IF_ACMPNE L486
+ALOAD 55
+ALOAD 52
 ALOAD 9
 INVOKEVIRTUAL local/lito381/server/BattleCompanion$Snapshot.objectId()J
-ILOAD 44
+ILOAD 46
 INVOKEDYNAMIC makeConcatWithConstants(JI)Ljava/lang/String; bsm=H6 java/lang/invoke/StringConcatFactory.makeConcatWithConstants(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;Ljava/lang/String;[Ljava/lang/Object;)Ljava/lang/invoke/CallSite; args=[str:"S_ATTACK controlled Shock Stun target=\u0001 damage=\u0001"]
 INVOKESTATIC local/lito381/server/CombatCompanionSpells.send(Llocal/lito381/server/ClientSession;[BLjava/lang/String;)V
+ALOAD 55
 ALOAD 53
-ALOAD 51
 ALOAD 9
 INVOKEVIRTUAL local/lito381/server/BattleCompanion$Snapshot.objectId()J
 INVOKEDYNAMIC makeConcatWithConstants(J)Ljava/lang/String; bsm=H6 java/lang/invoke/StringConcatFactory.makeConcatWithConstants(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;Ljava/lang/String;[Ljava/lang/Object;)Ljava/lang/invoke/CallSite; args=[str:"S_EFFECT controlled Shock Stun target=\u0001"]
 INVOKESTATIC local/lito381/server/CombatCompanionSpells.send(Llocal/lito381/server/ClientSession;[BLjava/lang/String;)V
-GOTO L447
-ALOAD 0
-ILOAD 41
+GOTO L463
+ALOAD 0
+ILOAD 42
 ALOAD 0
 CONST str:"playerMaxMp"
 INVOKESTATIC local/lito381/server/CombatCompanionSpells.integer(Llocal/lito381/server/ClientSession;Ljava/lang/String;)I
@@ -491,14 +539,14 @@
 CONST str:"S_MANA_POINT controlled Shock Stun"
 INVOKESTATIC local/lito381/server/CombatCompanionSpells.send(Llocal/lito381/server/ClientSession;[BLjava/lang/String;)V
 ILOAD 23
-IFEQ L502
+IFEQ L518
 ALOAD 0
 NEW local/lito381/server/Protocol$Writer
 DUP
 INVOKESPECIAL local/lito381/server/Protocol$Writer.<init>()V
 CONST Integer:13
 INVOKEVIRTUAL local/lito381/server/Protocol$Writer.c(I)Llocal/lito381/server/Protocol$Writer;
-ILOAD 39
+ILOAD 40
 INVOKEVIRTUAL local/lito381/server/Protocol$Writer.h(I)Llocal/lito381/server/Protocol$Writer;
 ALOAD 0
 CONST str:"playerMaxHp"
@@ -518,7 +566,7 @@
 AASTORE
 DUP
 CONST Integer:1
-ILOAD 44
+ILOAD 46
 INVOKESTATIC java/lang/Integer.valueOf(I)Ljava/lang/Integer;
 AASTORE
 DUP
@@ -527,21 +575,21 @@
 INVOKESTATIC java/lang/Integer.valueOf(I)Ljava/lang/Integer;
 AASTORE
 INVOKESTATIC local/lito381/server/NativeSourceControlledDisplay.invokeMagic(Ljava/lang/Object;Ljava/lang/String;Ljava/lang/String;[Ljava/lang/Object;)I
-ISTORE 54
-ILOAD 54
-IFGT L531
-ILOAD 44
-IFNE L533
+ISTORE 56
+ILOAD 56
+IFGT L547
+ILOAD 46
+IFNE L549
 ALOAD 0
 ALOAD 6
 INVOKESTATIC local/lito381/server/NativeShockCompanionAction.available(Llocal/lito381/server/ClientSession;Ljava/lang/Object;)Z
-IFEQ L533
-CONST Integer:1
-GOTO L534
-CONST Integer:0
-ISTORE 55
-ILOAD 55
-IFEQ L545
+IFEQ L549
+CONST Integer:1
+GOTO L550
+CONST Integer:0
+ISTORE 57
+ILOAD 57
+IFEQ L561
 ALOAD 8
 ALOAD 26
 INVOKEVIRTUAL local/lito381/server/CombatCompanionSpells$Attempt.eraseMagic()Llocal/lito381/server/BattleEraseMagicStatus$Lease;
@@ -553,62 +601,62 @@
 ALOAD 3
 CONST Integer:100
 INVOKESTATIC local/lito381/server/CombatCompanionSpells.checkedRoll(Ljava/util/function/IntUnaryOperator;I)I
-ISTORE 56
-CONST Integer:0
-ISTORE 57
-CONST Integer:0
 ISTORE 58
-ILOAD 55
-IFEQ L654
+CONST Integer:0
+ISTORE 59
+CONST Integer:0
+ISTORE 60
+ILOAD 57
+IFEQ L670
 ALOAD 26
 INVOKEVIRTUAL local/lito381/server/CombatCompanionSpells$Attempt.probability()I
-IFLE L654
-ILOAD 56
+IFLE L670
+ILOAD 58
 ALOAD 26
 INVOKEVIRTUAL local/lito381/server/CombatCompanionSpells$Attempt.probability()I
-IF_ICMPGT L654
+IF_ICMPGT L670
 ALOAD 0
 ALOAD 6
 INVOKESTATIC local/lito381/server/NativeShockCompanionAction.available(Llocal/lito381/server/ClientSession;Ljava/lang/Object;)Z
-IFEQ L654
+IFEQ L670
 ALOAD 2
 ALOAD 3
 INVOKEDYNAMIC applyAsInt(Ljava/util/function/IntUnaryOperator;)Ljava/util/function/IntUnaryOperator; bsm=H6 java/lang/invoke/LambdaMetafactory.metafactory(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodHandle;Ljava/lang/invoke/MethodType;)Ljava/lang/invoke/CallSite; args=[type:(I)I, handle:H6 local/lito381/server/CombatCompanionSpells.checkedRoll(Ljava/util/function/IntUnaryOperator;I)I, type:(I)I]
 INVOKEVIRTUAL local/lito381/server/BattleSpellCatalog$ShockStunSpell.rollStunSeconds(Ljava/util/function/IntUnaryOperator;)I
-ISTORE 57
+ISTORE 59
 ALOAD 8
 CONST str:"timedStatus"
 INVOKESTATIC local/lito381/server/CombatAccess.field(Ljava/lang/Object;Ljava/lang/String;)Ljava/lang/Object;
 CHECKCAST local/lito381/server/BattleTimedStatus
-ASTORE 59
+ASTORE 61
 ALOAD 8
 DUP
-ASTORE 60
+ASTORE 62
 MONITORENTER
-ALOAD 59
+ALOAD 61
 INVOKEVIRTUAL local/lito381/server/BattleTimedStatus.cancelShockStun()Z
 POP
 ALOAD 8
-LLOAD 37/38
-ILOAD 57
+LLOAD 38/39
+ILOAD 59
 ALOAD 2
 INVOKEVIRTUAL local/lito381/server/BattleSpellCatalog$ShockStunSpell.maximumStunSeconds()I
 INVOKEVIRTUAL local/lito381/server/BattleCompanion.activateShockStun(JII)Llocal/lito381/server/BattleTimedStatus$ShockStunActivation;
-IFNULL L592
-CONST Integer:1
-GOTO L593
-CONST Integer:0
-ISTORE 58
-ALOAD 60
+IFNULL L608
+CONST Integer:1
+GOTO L609
+CONST Integer:0
+ISTORE 60
+ALOAD 62
 MONITOREXIT
-GOTO L602
-ASTORE 61
-ALOAD 60
+GOTO L618
+ASTORE 63
+ALOAD 62
 MONITOREXIT
-ALOAD 61
+ALOAD 63
 ATHROW
-ILOAD 58
-IFEQ L654
+ILOAD 60
+IFEQ L670
 ALOAD 11
 LLOAD 12/13
 ALOAD 9
@@ -617,62 +665,62 @@
 INVOKEVIRTUAL local/lito381/server/BattleCompanion$Snapshot.x()I
 ALOAD 9
 INVOKEVIRTUAL local/lito381/server/BattleCompanion$Snapshot.y()I
-LLOAD 37/38
-ILOAD 57
+LLOAD 38/39
+ILOAD 59
 INVOKEVIRTUAL local/lito381/server/GameHub.placeShockStunEffect(JJIIJI)Llocal/lito381/server/BattleShockStunEffects$Placement;
-ASTORE 62
-ALOAD 62
-IFNULL L654
-ALOAD 46
+ASTORE 64
+ALOAD 64
+IFNULL L670
+ALOAD 48
 INVOKEINTERFACE java/util/List.iterator()Ljava/util/Iterator; itf
-ASTORE 63
-ALOAD 63
+ASTORE 65
+ALOAD 65
 INVOKEINTERFACE java/util/Iterator.hasNext()Z itf
-IFEQ L654
-ALOAD 63
+IFEQ L670
+ALOAD 65
 INVOKEINTERFACE java/util/Iterator.next()Ljava/lang/Object; itf
 CHECKCAST local/lito381/server/ClientSession
-ASTORE 64
-ALOAD 62
+ASTORE 66
+ALOAD 64
 INVOKEVIRTUAL local/lito381/server/BattleShockStunEffects$Placement.previous()Llocal/lito381/server/BattleShockStunEffects$Effect;
-IFNULL L641
+IFNULL L657
+ALOAD 66
 ALOAD 64
-ALOAD 62
 INVOKEVIRTUAL local/lito381/server/BattleShockStunEffects$Placement.previous()Llocal/lito381/server/BattleShockStunEffects$Effect;
 INVOKEVIRTUAL local/lito381/server/BattleShockStunEffects$Effect.objectId()J
 INVOKESTATIC local/lito381/server/Protocol.removeObject(J)[B
-ALOAD 62
+ALOAD 64
 INVOKEVIRTUAL local/lito381/server/BattleShockStunEffects$Placement.previous()Llocal/lito381/server/BattleShockStunEffects$Effect;
 INVOKEVIRTUAL local/lito381/server/BattleShockStunEffects$Effect.objectId()J
 INVOKEDYNAMIC makeConcatWithConstants(J)Ljava/lang/String; bsm=H6 java/lang/invoke/StringConcatFactory.makeConcatWithConstants(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;Ljava/lang/String;[Ljava/lang/Object;)Ljava/lang/invoke/CallSite; args=[str:"S_REMOVE_OBJECT controlled Shock Stun effect=\u0001"]
 INVOKESTATIC local/lito381/server/CombatCompanionSpells.send(Llocal/lito381/server/ClientSession;[BLjava/lang/String;)V
+ALOAD 66
 ALOAD 64
-ALOAD 62
 INVOKEVIRTUAL local/lito381/server/BattleShockStunEffects$Placement.placed()Llocal/lito381/server/BattleShockStunEffects$Effect;
 INVOKESTATIC local/lito381/server/Protocol.shockStunEffect(Llocal/lito381/server/BattleShockStunEffects$Effect;)[B
-ALOAD 62
+ALOAD 64
 INVOKEVIRTUAL local/lito381/server/BattleShockStunEffects$Placement.placed()Llocal/lito381/server/BattleShockStunEffects$Effect;
 INVOKEVIRTUAL local/lito381/server/BattleShockStunEffects$Effect.objectId()J
 ALOAD 9
 INVOKEVIRTUAL local/lito381/server/BattleCompanion$Snapshot.objectId()J
-ILOAD 57
+ILOAD 59
 INVOKEDYNAMIC makeConcatWithConstants(JJI)Ljava/lang/String; bsm=H6 java/lang/invoke/StringConcatFactory.makeConcatWithConstants(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;Ljava/lang/String;[Ljava/lang/Object;)Ljava/lang/invoke/CallSite; args=[str:"S_PUT_OBJECT controlled Shock Stun effect=\u0001 target=\u0001 duration=\u0001"]
 INVOKESTATIC local/lito381/server/CombatCompanionSpells.send(Llocal/lito381/server/ClientSession;[BLjava/lang/String;)V
-GOTO L621
+GOTO L637
 GETSTATIC java/lang/System.out:Ljava/io/PrintStream;
 ALOAD 9
 INVOKEVIRTUAL local/lito381/server/BattleCompanion$Snapshot.objectId()J
 ALOAD 9
 INVOKEVIRTUAL local/lito381/server/BattleCompanion$Snapshot.kind()Llocal/lito381/server/BattleCompanion$Kind;
 INVOKESTATIC java/lang/String.valueOf(Ljava/lang/Object;)Ljava/lang/String;
-ILOAD 54
+ILOAD 56
 ALOAD 26
 INVOKEVIRTUAL local/lito381/server/CombatCompanionSpells$Attempt.probability()I
-ILOAD 56
+ILOAD 58
 ALOAD 26
 INVOKEVIRTUAL local/lito381/server/CombatCompanionSpells$Attempt.magicResistance()I
-ILOAD 58
-ILOAD 57
+ILOAD 60
+ILOAD 59
 ILOAD 22
 ILOAD 23
 INVOKEDYNAMIC makeConcatWithConstants(JLjava/lang/String;IIIIZIII)Ljava/lang/String; bsm=H6 java/lang/invoke/StringConcatFactory.makeConcatWithConstants(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;Ljava/lang/String;[Ljava/lang/Object;)Ljava/lang/invoke/CallSite; args=[str:"[LiTo] controlled BATTLE Shock Stun target=\u0001 kind=\u0001 damage=\u0001 probability=\u0001 roll=\u0001 mr=\u0001 applied=\u0001 seconds=\u0001 mp-cost=\u0001 hp-cost=\u0001"]
```

## 附錄 B — CombatNpcDebuffs 完整正規化差異

```diff
--- CombatNpcDebuffs staged
+++ CombatNpcDebuffs candidate
@@ -1,20 +1,68 @@
 # exception coverage
-552:[592:java/lang/Throwable  ]
-553:[592:java/lang/Throwable  ]
-556:[592:java/lang/Throwable  ]
-559:[592:java/lang/Throwable  ]
-562:[592:java/lang/Throwable  ]
-566:[592:java/lang/Throwable  ]
-573:[592:java/lang/Throwable  ]
-574:[592:java/lang/Throwable  ]
-577:[592:java/lang/Throwable  ]
-578:[592:java/lang/Throwable  ]
-581:[592:java/lang/Throwable  ]
-582:[592:java/lang/Throwable  ]
-585:[592:java/lang/Throwable  ]
-586:[592:java/lang/Throwable  ]
-590:[592:java/lang/Throwable  ]
-594:[592:java/lang/Throwable  ]
+293:[484:java/lang/Throwable  ]
+297:[484:java/lang/Throwable  ]
+301:[484:java/lang/Throwable  ]
+305:[484:java/lang/Throwable  ]
+306:[484:java/lang/Throwable  ]
+309:[484:java/lang/Throwable  ]
+312:[484:java/lang/Throwable  ]
+316:[484:java/lang/Throwable  ]
+321:[484:java/lang/Throwable  ]
+330:[484:java/lang/Throwable  ]
+331:[484:java/lang/Throwable  ]
+332:[484:java/lang/Throwable  ]
+334:[484:java/lang/Throwable  ]
+338:[484:java/lang/Throwable  ]
+339:[484:java/lang/Throwable  ]
+340:[484:java/lang/Throwable  ]
+352:[484:java/lang/Throwable  ]
+353:[484:java/lang/Throwable  ]
+354:[484:java/lang/Throwable  ]
+361:[484:java/lang/Throwable  ]
+363:[484:java/lang/Throwable  ]
+364:[484:java/lang/Throwable  ]
+365:[484:java/lang/Throwable  ]
+366:[484:java/lang/Throwable  ]
+371:[484:java/lang/Throwable  ]
+376:[484:java/lang/Throwable  ]
+389:[484:java/lang/Throwable  ]
+399:[484:java/lang/Throwable  ]
+410:[484:java/lang/Throwable  ]
+417:[484:java/lang/Throwable  ]
+420:[484:java/lang/Throwable  ]
+422:[484:java/lang/Throwable  ]
+428:[484:java/lang/Throwable  ]
+432:[484:java/lang/Throwable  ]
+433:[484:java/lang/Throwable  ]
+434:[484:java/lang/Throwable  ]
+447:[484:java/lang/Throwable  ]
+448:[484:java/lang/Throwable  ]
+453:[484:java/lang/Throwable  ]
+456:[484:java/lang/Throwable  ]
+457:[484:java/lang/Throwable  ]
+462:[484:java/lang/Throwable  ]
+466:[484:java/lang/Throwable  ]
+467:[484:java/lang/Throwable  ]
+472:[484:java/lang/Throwable  ]
+477:[484:java/lang/Throwable  ]
+478:[484:java/lang/Throwable  ]
+479:[484:java/lang/Throwable  ]
+568:[608:java/lang/Throwable  ]
+569:[608:java/lang/Throwable  ]
+572:[608:java/lang/Throwable  ]
+575:[608:java/lang/Throwable  ]
+578:[608:java/lang/Throwable  ]
+582:[608:java/lang/Throwable  ]
+589:[608:java/lang/Throwable  ]
+590:[608:java/lang/Throwable  ]
+593:[608:java/lang/Throwable  ]
+594:[608:java/lang/Throwable  ]
+597:[608:java/lang/Throwable  ]
+598:[608:java/lang/Throwable  ]
+601:[608:java/lang/Throwable  ]
+602:[608:java/lang/Throwable  ]
+606:[608:java/lang/Throwable  ]
+610:[608:java/lang/Throwable  ]
 # instructions
 ALOAD 2
 INVOKEINTERFACE local/lito381/server/BattleSpellCatalog$TargetNegativeStatusSpell.skillId()I itf
@@ -267,63 +315,63 @@
 CHECKCAST local/lito381/server/CharacterSummary
 ASTORE 28
 ALOAD 2
-ASTORE 29
 ALOAD 28
 INVOKEVIRTUAL local/lito381/server/CharacterSummary.type()I
 ALOAD 28
 INVOKEVIRTUAL local/lito381/server/CharacterSummary.originalIntelligence()I
 INVOKESTATIC local/lito381/server/BattleSpellCatalog.originalMagicConsumeReduction(II)I
+ALOAD 0
+INVOKEVIRTUAL local/lito381/server/ClientSession.nativeSkillConsumptionIntelligence()I
+ISTORE 29
+ALOAD 0
+INVOKEVIRTUAL local/lito381/server/ClientSession.nativeSkillConsumptionOriginalReduction()I
 ISTORE 30
-ALOAD 0
-INVOKEVIRTUAL local/lito381/server/ClientSession.nativeSkillConsumptionIntelligence()I
+ILOAD 29
+INVOKESTATIC local/lito381/server/NativeSkillConsumptionBridges.TargetNegativeStatusSpell_effectiveMpCost_II(Llocal/lito381/server/BattleSpellCatalog$TargetNegativeStatusSpell;II)I
 ISTORE 31
-ALOAD 0
-INVOKEVIRTUAL local/lito381/server/ClientSession.nativeSkillConsumptionOriginalReduction()I
-ISTORE 32
-ALOAD 29
-ILOAD 30
-ILOAD 31
-INVOKESTATIC local/lito381/server/NativeSkillConsumptionBridges.TargetNegativeStatusSpell_effectiveMpCost_II(Llocal/lito381/server/BattleSpellCatalog$TargetNegativeStatusSpell;II)I
-ISTORE 33
 CONST Integer:0
 ILOAD 6
 ALOAD 7
 CONST str:"hpCost"
 INVOKESTATIC local/lito381/server/CombatDatabase.value(IILjava/lang/String;Ljava/lang/String;)I
-ISTORE 34
+ISTORE 32
 CONST Integer:0
 ILOAD 6
 ALOAD 7
 CONST str:"itemConsumeId"
 INVOKESTATIC local/lito381/server/CombatDatabase.value(IILjava/lang/String;Ljava/lang/String;)I
-ISTORE 35
+ISTORE 33
 CONST Integer:0
 ILOAD 6
 ALOAD 7
 CONST str:"itemConsumeCount"
 INVOKESTATIC local/lito381/server/CombatDatabase.value(IILjava/lang/String;Ljava/lang/String;)I
-ISTORE 36
+ISTORE 34
 ALOAD 0
 ALOAD 10
 INVOKESTATIC local/lito381/server/NativeControlledNegativeMaterial.available(Llocal/lito381/server/ClientSession;Llocal/lito381/server/CombatNpcDebuffs$Focus;)Z
-IFNE L293
-CONST Integer:1
-IRETURN
+IFNE L289
+CONST Integer:1
+IRETURN
+ALOAD 0
+DUP
+ASTORE 35
+POP
 INVOKESTATIC java/lang/System.nanoTime()J
-LSTORE 37/38
+LSTORE 36/37
 ALOAD 0
 CONST str:"playerHp"
 INVOKESTATIC local/lito381/server/CombatNpcDebuffs.integer(Llocal/lito381/server/ClientSession;Ljava/lang/String;)I
+ISTORE 38
+ALOAD 0
+CONST str:"playerMp"
+INVOKESTATIC local/lito381/server/CombatNpcDebuffs.integer(Llocal/lito381/server/ClientSession;Ljava/lang/String;)I
 ISTORE 39
-ALOAD 0
-CONST str:"playerMp"
-INVOKESTATIC local/lito381/server/CombatNpcDebuffs.integer(Llocal/lito381/server/ClientSession;Ljava/lang/String;)I
-ISTORE 40
 ALOAD 0
 CONST str:"spellReadyNanos"
 INVOKESTATIC local/lito381/server/CombatAccess.field(Ljava/lang/Object;Ljava/lang/String;)Ljava/lang/Object;
 CHECKCAST java/util/Map
-ASTORE 41
+ASTORE 40
 ALOAD 10
 INVOKEVIRTUAL local/lito381/server/CombatNpcDebuffs$Focus.world()Z
 IFEQ L328
@@ -363,8 +411,8 @@
 CONST Integer:1
 GOTO L347
 CONST Integer:0
-ISTORE 42
-ILOAD 42
+ISTORE 41
+ILOAD 41
 IFEQ L379
 ALOAD 0
 CONST str:"observer"
@@ -372,10 +420,10 @@
 CHECKCAST java/lang/Boolean
 INVOKEVIRTUAL java/lang/Boolean.booleanValue()Z
 IFNE L379
-ILOAD 39
+ILOAD 38
 IFLE L379
-LLOAD 37/38
-ALOAD 41
+LLOAD 36/37
+ALOAD 40
 ILOAD 6
 INVOKESTATIC java/lang/Integer.valueOf(I)Ljava/lang/Integer;
 CONST Long:0
@@ -394,44 +442,52 @@
 CONST str:"positionY"
 INVOKESTATIC local/lito381/server/CombatNpcDebuffs.integer(Llocal/lito381/server/ClientSession;Ljava/lang/String;)I
 ILOAD 14
-IF_ICMPEQ L381
-CONST Integer:1
-IRETURN
-ILOAD 39
-ILOAD 34
-IF_ICMPGT L389
+IF_ICMPEQ L383
+CONST Integer:1
+ALOAD 35
+POP
+IRETURN
+ILOAD 38
+ILOAD 32
+IF_ICMPGT L393
 ALOAD 0
 CONST Integer:279
 ALOAD 8
 INVOKESTATIC local/lito381/server/CombatNpcDebuffs.rejected(Llocal/lito381/server/ClientSession;ILjava/lang/String;)Z
-IRETURN
-ILOAD 40
-ILOAD 33
-IF_ICMPGE L397
+ALOAD 35
+POP
+IRETURN
+ILOAD 39
+ILOAD 31
+IF_ICMPGE L403
 ALOAD 0
 CONST Integer:278
 ALOAD 8
 INVOKESTATIC local/lito381/server/CombatNpcDebuffs.rejected(Llocal/lito381/server/ClientSession;ILjava/lang/String;)Z
-IRETURN
+ALOAD 35
+POP
+IRETURN
+CONST null
+ASTORE 42
 CONST null
 ASTORE 43
-CONST null
+ALOAD 0
+ALOAD 10
+ALOAD 2
+INVOKESTATIC local/lito381/server/NativeControlledNegativeMaterial.admit(Llocal/lito381/server/ClientSession;Llocal/lito381/server/CombatNpcDebuffs$Focus;Llocal/lito381/server/BattleSpellCatalog$TargetNegativeStatusSpell;)Z
+IFNE L416
+CONST Integer:1
+ALOAD 35
+POP
+IRETURN
+ALOAD 0
+INVOKESTATIC local/lito381/server/NativeCastMaterial.previous(Llocal/lito381/server/ClientSession;)Llocal/lito381/server/RuntimeInventory;
 ASTORE 44
 ALOAD 0
-ALOAD 10
-ALOAD 2
-INVOKESTATIC local/lito381/server/NativeControlledNegativeMaterial.admit(Llocal/lito381/server/ClientSession;Llocal/lito381/server/CombatNpcDebuffs$Focus;Llocal/lito381/server/BattleSpellCatalog$TargetNegativeStatusSpell;)Z
-IFNE L408
-CONST Integer:1
-IRETURN
-ALOAD 0
-INVOKESTATIC local/lito381/server/NativeCastMaterial.previous(Llocal/lito381/server/ClientSession;)Llocal/lito381/server/RuntimeInventory;
+INVOKESTATIC local/lito381/server/NativeCastMaterial.next(Llocal/lito381/server/ClientSession;)Llocal/lito381/server/RuntimeInventory;
 ASTORE 45
-ALOAD 0
-INVOKESTATIC local/lito381/server/NativeCastMaterial.next(Llocal/lito381/server/ClientSession;)Llocal/lito381/server/RuntimeInventory;
-ASTORE 46
 INVOKESTATIC java/lang/System.nanoTime()J
-LSTORE 47/48
+LSTORE 46/47
 ALOAD 0
 CONST str:"cancelMeditation"
 CONST str:"(Ljava/lang/String;)V"
@@ -444,26 +500,26 @@
 AASTORE
 INVOKESTATIC local/lito381/server/CombatAccess.call(Ljava/lang/Object;Ljava/lang/String;Ljava/lang/String;[Ljava/lang/Object;)Ljava/lang/Object;
 POP
+ILOAD 38
+ILOAD 32
+ISUB
+ISTORE 48
 ILOAD 39
-ILOAD 34
+ILOAD 31
 ISUB
 ISTORE 49
-ILOAD 40
-ILOAD 33
-ISUB
-ISTORE 50
 ALOAD 0
 CONST str:"playerHp"
-ILOAD 49
+ILOAD 48
 INVOKESTATIC java/lang/Integer.valueOf(I)Ljava/lang/Integer;
 INVOKESTATIC local/lito381/server/CombatAccess.set(Ljava/lang/Object;Ljava/lang/String;Ljava/lang/Object;)V
 ALOAD 0
 CONST str:"playerMp"
 ALOAD 0
+ILOAD 49
+INVOKESTATIC local/lito381/server/NativeSkillConsumption.boundMp(Llocal/lito381/server/ClientSession;I)I
+ISTORE 50
 ILOAD 50
-INVOKESTATIC local/lito381/server/NativeSkillConsumption.boundMp(Llocal/lito381/server/ClientSession;I)I
-DUP
-ISTORE 51
 INVOKESTATIC java/lang/Integer.valueOf(I)Ljava/lang/Integer;
 INVOKESTATIC local/lito381/server/CombatAccess.set(Ljava/lang/Object;Ljava/lang/String;Ljava/lang/Object;)V
 ALOAD 0
@@ -478,7 +534,7 @@
 AASTORE
 DUP
 CONST Integer:1
-LLOAD 47/48
+LLOAD 46/47
 ALOAD 2
 INVOKEINTERFACE local/lito381/server/BattleSpellCatalog$TargetNegativeStatusSpell.reuseDelayMillis()I itf
 I2L
@@ -489,6 +545,14 @@
 AASTORE
 INVOKESTATIC local/lito381/server/CombatAccess.call(Ljava/lang/Object;Ljava/lang/String;Ljava/lang/String;[Ljava/lang/Object;)Ljava/lang/Object;
 POP
+ALOAD 35
+POP
+GOTO L489
+ASTORE 51
+ALOAD 35
+POP
+ALOAD 51
+ATHROW
 ALOAD 0
 CONST str:"revealInvisibility"
 CONST str:"(Ljava/lang/String;)V"
@@ -520,11 +584,11 @@
 ANEWARRAY java/lang/Object
 DUP
 CONST Integer:0
+ALOAD 44
+AASTORE
+DUP
+CONST Integer:1
 ALOAD 45
-AASTORE
-DUP
-CONST Integer:1
-ALOAD 46
 AASTORE
 INVOKESTATIC local/lito381/server/CombatAccess.call(Ljava/lang/Object;Ljava/lang/String;Ljava/lang/String;[Ljava/lang/Object;)Ljava/lang/Object;
 POP
@@ -535,16 +599,16 @@
 CONST Integer:0
 ISTORE 53
 ILOAD 3
-IFLE L522
+IFLE L538
 ILOAD 3
-GOTO L526
+GOTO L542
 INVOKESTATIC java/util/concurrent/ThreadLocalRandom.current()Ljava/util/concurrent/ThreadLocalRandom;
 CONST Integer:1
 CONST Integer:101
 INVOKEVIRTUAL java/util/concurrent/ThreadLocalRandom.nextInt(II)I
 ISTORE 54
 ILOAD 52
-IFEQ L597
+IFEQ L613
 ALOAD 10
 INVOKEVIRTUAL local/lito381/server/CombatNpcDebuffs$Focus.controlled()Llocal/lito381/server/BattleCompanion;
 ALOAD 27
@@ -557,11 +621,11 @@
 POP
 ALOAD 27
 INVOKEVIRTUAL local/lito381/server/CombatCompanionSpells$Attempt.probability()I
-IFLE L597
+IFLE L613
 ALOAD 27
 INVOKEVIRTUAL local/lito381/server/CombatCompanionSpells$Attempt.probability()I
 ILOAD 54
-IF_ICMPLT L597
+IF_ICMPLT L613
 ALOAD 10
 INVOKEVIRTUAL local/lito381/server/CombatNpcDebuffs$Focus.controlled()Llocal/lito381/server/BattleCompanion;
 DUP
@@ -573,33 +637,33 @@
 ASTORE 56
 ALOAD 56
 INVOKEVIRTUAL local/lito381/server/BattleCompanion$Snapshot.active()Z
-IFEQ L589
+IFEQ L605
 ALOAD 56
 INVOKEVIRTUAL local/lito381/server/BattleCompanion$Snapshot.hp()I
-IFLE L589
+IFLE L605
 ALOAD 56
 INVOKEVIRTUAL local/lito381/server/BattleCompanion$Snapshot.x()I
 ILOAD 23
-IF_ICMPNE L589
+IF_ICMPNE L605
 ALOAD 56
 INVOKEVIRTUAL local/lito381/server/BattleCompanion$Snapshot.y()I
 ILOAD 24
-IF_ICMPNE L589
+IF_ICMPNE L605
 ILOAD 6
 CONST Integer:173
-IF_ICMPNE L580
+IF_ICMPNE L596
 ALOAD 10
 INVOKEVIRTUAL local/lito381/server/CombatNpcDebuffs$Focus.controlled()Llocal/lito381/server/BattleCompanion;
 INVOKESTATIC local/lito381/server/CombatNpcDebuffs.timed(Llocal/lito381/server/BattleCompanion;)Llocal/lito381/server/BattleTimedStatus;
-LLOAD 47/48
+LLOAD 46/47
 ALOAD 2
 INVOKEINTERFACE local/lito381/server/BattleSpellCatalog$TargetNegativeStatusSpell.durationSeconds()I itf
 INVOKEVIRTUAL local/lito381/server/BattleTimedStatus.activatePolluteWater(JI)V
-GOTO L587
+GOTO L603
 ALOAD 10
 INVOKEVIRTUAL local/lito381/server/CombatNpcDebuffs$Focus.controlled()Llocal/lito381/server/BattleCompanion;
 INVOKESTATIC local/lito381/server/CombatNpcDebuffs.timed(Llocal/lito381/server/BattleCompanion;)Llocal/lito381/server/BattleTimedStatus;
-LLOAD 47/48
+LLOAD 46/47
 ALOAD 2
 INVOKEINTERFACE local/lito381/server/BattleSpellCatalog$TargetNegativeStatusSpell.durationSeconds()I itf
 INVOKEVIRTUAL local/lito381/server/BattleTimedStatus.activateStrikerGale(JI)V
@@ -607,7 +671,7 @@
 ISTORE 53
 ALOAD 55
 MONITOREXIT
-GOTO L597
+GOTO L613
 ASTORE 57
 ALOAD 55
 MONITOREXIT
@@ -615,7 +679,7 @@
 ATHROW
 ALOAD 10
 INVOKEVIRTUAL local/lito381/server/CombatNpcDebuffs$Focus.world()Z
-IFEQ L613
+IFEQ L629
 ALOAD 0
 CONST str:"classicWorldMembers"
 CONST str:"(I)Ljava/util/List;"
@@ -628,7 +692,7 @@
 AASTORE
 INVOKESTATIC local/lito381/server/CombatAccess.call(Ljava/lang/Object;Ljava/lang/String;Ljava/lang/String;[Ljava/lang/Object;)Ljava/lang/Object;
 CHECKCAST java/util/List
-GOTO L616
+GOTO L632
 ALOAD 19
 LLOAD 17/18
 INVOKEVIRTUAL local/lito381/server/GameHub.roomMembers(J)Ljava/util/List;
@@ -650,14 +714,14 @@
 ASTORE 61
 ALOAD 61
 INVOKEINTERFACE java/util/Iterator.hasNext()Z itf
-IFEQ L682
+IFEQ L698
 ALOAD 61
 INVOKEINTERFACE java/util/Iterator.next()Ljava/lang/Object; itf
 CHECKCAST local/lito381/server/ClientSession
 ASTORE 62
 ALOAD 10
 INVOKEVIRTUAL local/lito381/server/CombatNpcDebuffs$Focus.world()Z
-IFEQ L663
+IFEQ L679
 ALOAD 62
 CONST str:"positionX"
 INVOKESTATIC local/lito381/server/CombatNpcDebuffs.integer(Llocal/lito381/server/ClientSession;Ljava/lang/String;)I
@@ -667,7 +731,7 @@
 ILOAD 13
 ILOAD 14
 INVOKESTATIC local/lito381/server/BattleSpellCatalog.isInNativeScreen(IIII)Z
-IFNE L663
+IFNE L679
 ALOAD 62
 CONST str:"positionX"
 INVOKESTATIC local/lito381/server/CombatNpcDebuffs.integer(Llocal/lito381/server/ClientSession;Ljava/lang/String;)I
@@ -677,8 +741,8 @@
 ILOAD 23
 ILOAD 24
 INVOKESTATIC local/lito381/server/BattleSpellCatalog.isInNativeScreen(IIII)Z
-IFNE L663
-GOTO L632
+IFNE L679
+GOTO L648
 ALOAD 62
 ALOAD 59
 ALOAD 8
@@ -687,7 +751,7 @@
 INVOKEDYNAMIC makeConcatWithConstants(Ljava/lang/String;J)Ljava/lang/String; bsm=H6 java/lang/invoke/StringConcatFactory.makeConcatWithConstants(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;Ljava/lang/String;[Ljava/lang/Object;)Ljava/lang/invoke/CallSite; args=[str:"S_ACTION owned \u0001 caster=\u0001"]
 INVOKESTATIC local/lito381/server/CombatCompanionSpells.send(Llocal/lito381/server/ClientSession;[BLjava/lang/String;)V
 ILOAD 53
-IFEQ L681
+IFEQ L697
 ALOAD 62
 ALOAD 60
 ALOAD 8
@@ -697,9 +761,9 @@
 INVOKEINTERFACE local/lito381/server/BattleSpellCatalog$TargetNegativeStatusSpell.effectGfx()I itf
 INVOKEDYNAMIC makeConcatWithConstants(Ljava/lang/String;JI)Ljava/lang/String; bsm=H6 java/lang/invoke/StringConcatFactory.makeConcatWithConstants(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;Ljava/lang/String;[Ljava/lang/Object;)Ljava/lang/invoke/CallSite; args=[str:"S_EFFECT owned \u0001 target=\u0001 gfx=\u0001"]
 INVOKESTATIC local/lito381/server/CombatCompanionSpells.send(Llocal/lito381/server/ClientSession;[BLjava/lang/String;)V
-GOTO L632
-ALOAD 0
-ILOAD 51
+GOTO L648
+ALOAD 0
+ILOAD 50
 ALOAD 0
 CONST str:"playerMaxMp"
 INVOKESTATIC local/lito381/server/CombatNpcDebuffs.integer(Llocal/lito381/server/ClientSession;Ljava/lang/String;)I
@@ -715,15 +779,15 @@
 ILOAD 53
 INVOKEDYNAMIC makeConcatWithConstants(Ljava/lang/String;JZIIZ)Ljava/lang/String; bsm=H6 java/lang/invoke/StringConcatFactory.makeConcatWithConstants(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;Ljava/lang/String;[Ljava/lang/Object;)Ljava/lang/invoke/CallSite; args=[str:"S_MANA_POINT owned \u0001 target=\u0001 world=\u0001 chance=\u0001 roll=\u0001 applied=\u0001"]
 INVOKESTATIC local/lito381/server/CombatCompanionSpells.send(Llocal/lito381/server/ClientSession;[BLjava/lang/String;)V
-ILOAD 34
-IFEQ L717
+ILOAD 32
+IFEQ L733
 ALOAD 0
 NEW local/lito381/server/Protocol$Writer
 DUP
 INVOKESPECIAL local/lito381/server/Protocol$Writer.<init>()V
 CONST Integer:13
 INVOKEVIRTUAL local/lito381/server/Protocol$Writer.c(I)Llocal/lito381/server/Protocol$Writer;
-ILOAD 49
+ILOAD 48
 INVOKEVIRTUAL local/lito381/server/Protocol$Writer.h(I)Llocal/lito381/server/Protocol$Writer;
 ALOAD 0
 CONST str:"playerMaxHp"
@@ -734,7 +798,7 @@
 INVOKEDYNAMIC makeConcatWithConstants(Ljava/lang/String;)Ljava/lang/String; bsm=H6 java/lang/invoke/StringConcatFactory.makeConcatWithConstants(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;Ljava/lang/String;[Ljava/lang/Object;)Ljava/lang/invoke/CallSite; args=[str:"S_HIT_POINT owned \u0001"]
 INVOKESTATIC local/lito381/server/CombatCompanionSpells.send(Llocal/lito381/server/ClientSession;[BLjava/lang/String;)V
 ILOAD 53
-IFNE L728
+IFNE L744
 ALOAD 0
 ALOAD 2
 INVOKEINTERFACE local/lito381/server/BattleSpellCatalog$TargetNegativeStatusSpell.failureMessage()I itf
```

## 附錄 C — 逐方法診斷程式

此程式僅讀取輸入 JAR 並呼叫本分支比較器，輸出診斷文字。編譯及執行的 argv 保留於 `remaining-commands.json`。

```java
package local.lito381.semanticdiff;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import org.objectweb.asm.tree.MethodNode;

public class Remaining {
    public static void main(String[] args) throws Exception {
        JarIndex left = JarIndex.load(Path.of(args[0]));
        JarIndex right = JarIndex.load(Path.of(args[1]));
        left.addClasspath(Path.of(args[1]));
        Path output = Path.of(args[2]);
        Files.createDirectories(output);
        String[][] targets = {
            {"CombatCompanionSpells", "nativeShockCompanionBody"},
            {"CombatNpcDebuffs", "nativeMaterialControlledNegativeBody"}
        };
        for (String[] target : targets) {
            String owner = "local/lito381/server/" + target[0];
            MethodNode a = left.readTree(owner).methods.stream()
                    .filter(method -> method.name.equals(target[1])).findFirst().orElseThrow();
            MethodNode b = right.readTree(owner).methods.stream()
                    .filter(method -> method.name.equals(target[1]) && method.desc.equals(a.desc))
                    .findFirst().orElseThrow();
            System.out.println(owner + "#" + a.name + a.desc);
            System.setProperty("semanticdiff.disable", "paths");
            var linear = ClassComparer.compareMethod(a.name + a.desc, a, b, left, right, false);
            System.out.println("linear=" + (linear == null ? "SAME" : linear.oneLine()));
            System.clearProperty("semanticdiff.disable");
            System.out.println("paths left/right=" + PathComparison.compare(a, b, left, right));
            System.out.println("paths left/left=" + PathComparison.compare(a, a, left, left));
            System.out.println("paths right/right=" + PathComparison.compare(b, b, right, right));
            for (int side = 0; side < 2; side++) {
                MethodNode method = side == 0 ? a : b;
                JarIndex index = side == 0 ? left : right;
                List<String> lines = new ArrayList<>();
                lines.add("# exception coverage");
                lines.addAll(ClassComparer.tryCatch(method, index));
                lines.add("# instructions");
                lines.addAll(ClassComparer.instructions(method, index));
                Files.write(output.resolve(target[0] + (side == 0 ? "-left.txt" : "-right.txt")), lines);
            }
        }
    }
}
```
