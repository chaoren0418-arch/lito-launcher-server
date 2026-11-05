# Issue #68 — T020-B07 戰鬥併入暫存

## 結論

2026-11-06 本輪獨立重建通過全部 **56 個 entry，54 SAME、2 SAME_CFG、0 DIFF、0 ONLY_LEFT、0 ONLY_RIGHT**，missing／extra 均為 0。23 份 staged Java 成功編譯，AsRecord 轉換器掃描 56 個 class、轉換 2 個。兩個指定方法均取得 `entry-rooted labelled CFGs equal` 證明，B07 可交 Claude 審查。

## 做了什麼

- 在既有 `task/68` 從 `f8e3e9e6aa96` 繼續，合併最新 `origin/main` 的 `54c908ab43d048ca6b9fe5e069c15734d8f9e8b4`，merge commit 為 `b2c7703ce18b82f9f03a8b50a513b700211029b4`。
- GitHub REST 核對 #203 的 PR #215、#216 的 PR #238 都已合併，merge SHA 分別為 `5ae1eac20d75b75b2bf9fbf302e8d878a32c62bd`、`54c908ab43d048ca6b9fe5e069c15734d8f9e8b4`。本次驗收使用 main 已合併的工具。[PR #215](https://github.com/chaoren0418-arch/lito-launcher-server/pull/215) [PR #238](https://github.com/chaoren0418-arch/lito-launcher-server/pull/238)
- 依裁定沿用 `CombatCompanionSpells#nativeShockCompanionBody` 與 `CombatNpcDebuffs#nativeMaterialControlledNegativeBody` 的既有 Java，透過 #216 的純轉拋 handler 正規化及 #203 的 CFG 比對完成驗收。[主控裁定](https://github.com/chaoren0418-arch/lito-launcher-server/issues/68#issuecomment-6666102445)
- 延續前輪三方移植結果，包括 `CombatRules.load` 的求值順序及 `HashMap.put` 呼叫、`NativeAreaMaterialAction` 的短路條件與區域發布鎖邊界，以及 `NativeReturnNatureMaterialAction.Target` 的 `@AsRecord`、`NativeAreaMaterialAction.Context` 的 `@AsmFinal`。
- 23 份來源逐檔核對 `f8e3e9e` 的 Git 內容，並與前輪 audit 比較原始 SHA-256，全部相符。轉換前後各 56 個 class 的逐 entry SHA-256 也全部與前輪相符。本輪交付更新集中於此報告，PR 相對 main 的淨交付為 23 份 staged Java 及此報告。

## 已驗證／怎麼驗證

### 環境與輸入

Windows、Microsoft OpenJDK 17.0.17、Python 3.12、ASM／asm-tree／asm-analysis 9.7。比較器與轉換器均由合併後來源重新 javac。semantic-diff 最新來源提交為 `743c55ee924dd4942bfc0bd717e53081fd319fdd`，AsRecord 最新來源提交為 `ff50cca6df1eb87c931b3d155ea0bfd72a65897b`。

| 輸入／產物 | SHA-256 |
|---|---|
| `reference/base-0.6.71.jar` | `e1498871c7be6b287821c57ef5d95933db1740d9b4d4a80f6c3465f21d99d498` |
| `reference/candidate-50635f7e.jar` | `50635f7edca8ec6ce77787cc32371c7590b497e48ea202f8a679caf3f517b729` |
| `target/issue-68/reverify/staged.jar` | `af7f16ed2373d96069dbedb059e2bb57ebb5a247eeca66b380ebed87a3f032f8` |

`docs/combat-merge/batches.json` 的 B07 `classes` 精確提供 23 份來源，`verify_entries` 提供 56 個唯一 entry。編譯目錄全新建立，轉換前後的 class 集合均精確等於清單。打包逐 entry 讀取該 classes 目錄，再逐項核對 JAR 內 bytes 的 SHA-256。

class 產物由 javac 生成，後處理只執行 `tools/as-record`。轉換前後 bytes 有變化的 entry 恰為 `NativeAreaMaterialAction$Context.class` 及 `NativeReturnNatureMaterialAction$Target.class`。本輪 JAR 容器雜湊因重新打包而更新，內部 56 個 class 與前輪完全相同。

### 核准清單

以 entry owner 精確裁切八份既有清單，B07 交集全部為空，紀錄於 `scratch/issue-68/reverify/approved-selection.json`。清單包括 `effects-approved.txt`、`extra-methods-approved.txt`、`metadata-exempt.txt`、`access-widen-approved.txt`、`layout-approved.txt`、`deviations.txt`、`exceptions-exempt.txt`、`t029-methods.txt`。實際命令因此只使用預設比較規則。

### 建置與比較命令

本機重建入口為 `scratch/issue-68/reverify.py`，使用全新 `target/issue-68/reverify/`。八個子命令完整 argv、日誌檔名及退出碼保留於 `scratch/issue-68/reverify/recheck-commands.json`。入口及所有子命令退出碼均為 0。

```powershell
& 'C:/Users/CHAOREN/AppData/Local/Programs/Python/Python312/python.exe' -B -X utf8 scratch/issue-68/reverify.py
```

以下為入口的建置與預設驗收等價命令，從 worktree 根目錄執行。`$out` 指向全新的產物目錄。

```powershell
$server = Join-Path (Get-Location) 'lito伺服器/LiTo381-Server'
$jdk = 'C:/Program Files/Microsoft/jdk-17.0.17.10-hotspot/bin'
$out = Join-Path $server 'target/issue-68/reverify'
$evidence = Join-Path (Get-Location) 'scratch/issue-68/reverify'
$batch = (Get-Content -Raw -Encoding UTF8 docs/combat-merge/batches.json | ConvertFrom-Json).batches |
  Where-Object id -eq 'B07'
$sources = @($batch.classes | ForEach-Object {
  Join-Path $server "merge-inputs/50635f7e/staged/local/lito381/server/$_.java"
})
$deps = 'D:/dev-cache/maven/repository/org/ow2/asm/asm/9.7/asm-9.7.jar;' +
        'D:/dev-cache/maven/repository/org/ow2/asm/asm-tree/9.7/asm-tree-9.7.jar;' +
        "$server/target/issue-68/asm-analysis-9.7.jar"
$converterSources = @(Get-ChildItem "$server/tools/as-record/src/main/java" -Recurse -Filter *.java |
  Sort-Object FullName | ForEach-Object FullName)
$toolSources = @(Get-ChildItem "$server/tools/semantic-diff/src/main/java" -Recurse -Filter *.java |
  Sort-Object FullName | ForEach-Object FullName)
& "$jdk/javac.exe" --release 17 -encoding UTF-8 -cp $deps -d "$out/as-record" @converterSources
& "$jdk/javac.exe" --release 17 -encoding UTF-8 -cp $deps -d "$out/semantic-tool" @toolSources
& "$jdk/javac.exe" --release 17 -encoding UTF-8 -d "$out/annotations" `
  "$server/src/main/java/local/lito381/server/AsRecord.java" `
  "$server/src/main/java/local/lito381/server/AsmFinal.java"
$cp = "$server/reference/candidate-50635f7e.jar;$server/reference/base-0.6.71.jar;$out/annotations;$server/lib/*"
& "$jdk/javac.exe" --release 17 -encoding UTF-8 -cp $cp -d "$out/classes" @sources
& "$jdk/java.exe" -Dfile.encoding=UTF-8 -cp "$out/as-record;$deps" local.lito381.asrecord.AsRecord "$out/classes"
# Python ZipFile 將 $out/classes 的 56 個 entry 打包為 $out/staged.jar。
# entries.txt 由 $batch.verify_entries 逐行以 UTF-8 寫入。
& "$jdk/java.exe" -Dfile.encoding=UTF-8 -cp "$out/semantic-tool;$deps" local.lito381.semanticdiff.SemanticDiff `
  --left "$out/staged.jar" --right "$server/reference/candidate-50635f7e.jar" `
  --left-classpath "$server/reference/candidate-50635f7e.jar" `
  --classes "$evidence/entries.txt" `
  --json "$evidence/recheck-default.json" --md "$evidence/recheck-default.md"
```

### 原始輸出與結果

```text
Note: Some input files use unchecked or unsafe operations.
Note: Recompile with -Xlint:unchecked for details.
javac exit=0
as-record: scanned=56 converted=2
as-record exit=0
SUMMARY SAME=54 SAME_EFFECTS=0 SAME_PATHS=0 ORPHAN_IGNORED=0 SAME_CFG=2 LAMBDA_PAIRED_BY_CALLSITE=0 VISIBILITY_BRIDGE=0 SAME_LAYOUT=0 DEVIATION=0 DIFF=0 ONLY_LEFT=0 ONLY_RIGHT=0 BYTE_SAME=0 ALLOW_METHODS=0
semantic-diff exit=0
missingLeft=[] extraLeft=[]
```

驗收同時檢查 JSON 計數、56-entry 集合及逐方法結果。CLI 退出 0 亦可能伴隨 DIFF，因此以這些資料共同判定通過。

| 模式 | SAME | SAME_CFG | DIFF | 用途 |
|---|---:|---:|---:|---|
| 預設 | 54 | 2 | 0 | 正式批次驗收 |
| `-Dsemanticdiff.disable=pure-rethrow` | 54 | 0 | 2 | 確認 #216 規則的作用 |
| `-Dsemanticdiff.disable=cfg` | 54 | 0 | 2 | 確認 #203 CFG 後備的作用 |

兩個診斷模式恢復 DIFF 的類別均為 CombatCompanionSpells 與 CombatNpcDebuffs。診斷的完整 JSON／Markdown 保留於 `recheck-no-pure-rethrow.*`、`recheck-no-cfg.*`。

### 指定方法的完整結果

```text
local/lito381/server/CombatCompanionSpells#nativeShockCompanionBody(Llocal/lito381/server/ClientSession;[BLlocal/lito381/server/BattleSpellCatalog$ShockStunSpell;Ljava/util/function/IntUnaryOperator;Ljava/lang/Object;)Z
SAME_CFG
entry-rooted labelled CFGs equal
```

```text
local/lito381/server/CombatNpcDebuffs#nativeMaterialControlledNegativeBody(Llocal/lito381/server/ClientSession;[BLlocal/lito381/server/BattleSpellCatalog$TargetNegativeStatusSpell;ILlocal/lito381/server/CombatNpcDebuffs$Focus;Z)Z
SAME_CFG
entry-rooted labelled CFGs equal
```

### 全部 56 個 entry

共用前綴為 `local/lito381/server/`，表格省略 `.class`。

| Entry | 結果 |
|---|---|
| `CombatAccess` | SAME |
| `CombatAccess$1` | SAME |
| `CombatCompanionSpells` | SAME_CFG |
| `CombatCompanionSpells$Attempt` | SAME |
| `CombatCompanionSpells$Batch` | SAME |
| `CombatCompanionSpells$Hit` | SAME |
| `CombatDatabase` | SAME |
| `CombatDatabase$Skill` | SAME |
| `CombatDatabase$Snapshot` | SAME |
| `CombatHealing` | SAME |
| `CombatNpcDebuffs` | SAME_CFG |
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

## 推測

本輪結論依據獨立編譯、entry 與雜湊核對及工具證明。

## 待驗證／未驗證什麼

單元測試及行為回歸依本批規則留待 B-land 統一執行。真機登入、進房、戰鬥結算與正常退出由後續驗收階段處理。本次完成範圍為 staged 批次的建置與語意驗收。

## 產出路徑

- Git 交付報告位於 `docs/reports/issue-68.md`。
- 23 份 staged Java 位於 `lito伺服器/LiTo381-Server/merge-inputs/50635f7e/staged/local/lito381/server/`，精確清單依 B07 `classes`。
- 本機產物位於 `lito伺服器/LiTo381-Server/target/issue-68/reverify/`，包含 annotations、as-record、semantic-tool、classes 及 staged.jar。
- 本機完整證據位於 `scratch/issue-68/reverify/`，包含八個命令的收據、日誌、三組比較結果、核准裁切清單與 `audit.json`。audit 包含 23 份來源雜湊、56-entry 清單、轉換前後逐 class 雜湊、reference 及產物雜湊。

## 給下一棒的提示

請 Claude 依 #68 的裁定審查 54 SAME＋2 SAME_CFG 的結果與 PR #92。B07 的兩個既有阻擋已由合併後工具證明通過，可銜接 #138 B-land，並在全批落地後統一執行測試與回歸。[PR #92](https://github.com/chaoren0418-arch/lito-launcher-server/pull/92)
