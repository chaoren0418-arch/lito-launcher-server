# Issue #62／T020-B01：#203 合併後全批驗證通過

## 結論

**B01 的 17 份 staged Java 重新編譯、轉換後，全部 22 個 entry 通過本批核准語意驗收：20 SAME、1 SAME_EFFECTS、1 LAMBDA_PAIRED_BY_CALLSITE；DIFF、ONLY_LEFT、ONLY_RIGHT、missing、extra 全為 0。** 比對時間 `2026-11-05T21:00:36Z`（臺北 2026-11-06）。本輪 Java 不改；前輪的 1 DIFF 已由 #203 正式 CLI 的呼叫點配對解決。

## 做了什麼（已驗證）

- 工作目錄 `D:/lineage開發/3-lito開發/scratch/wt/task-62`，沿用 `task/62`，未 checkout／建立分支。
- 開始 HEAD 為 `e01c6b9`；fetch 後將 main `567b0cb7762864b10ef876f90749e74a66e9bb01` 合入，合併提交 `68504262d2ec921285ab329df7ac28a55539f34b`，沒有衝突。
- [#203 的 PR #215](https://github.com/chaoren0418-arch/lito-launcher-server/pull/215) 已於 `2026-11-05T20:19:36Z` 合併，merge commit `5ae1eac20d75b75b2bf9fbf302e8d878a32c62bd`；`git merge-base --is-ancestor` 確認已在本次 HEAD。
- 依[主控裁定](https://github.com/chaoren0418-arch/lito-launcher-server/issues/62#issuecomment-6662268131)，逐一以 bytes 核對 17 份 B01 Java 與 `e01c6b9`、`574c188` 完全相同，全部保留指定 DECOMPILED 標頭。
- 保留 DollAccepted 讀取方式、TargetLifecycle null 檢查與 throws、Wind varargs、Forest 既有 effects 核准，以及 Draft 的 `@AsRecord`。本輪沒有擴大核准清單。
- 本輪自行修改的受追蹤檔案只有本報告；其他合併內容均來自 main。`docs/REPORT.md` 與 main 相同，未由本任務編輯。整個 PR 相對 main 仍為原有 17 份 B01 Java、Forest 核准行與本報告。

## 怎麼驗證（已驗證）

使用 Microsoft OpenJDK 17.0.17、ASM／asm-tree／asm-analysis 9.7。註解獨立編譯，僅將 B01 的 17 份 staged Java 傳給 javac，classpath 順序為候選、正式、AsRecord 註解、lib。轉換器輸出 `scanned=22 converted=1`，Draft 驗證為 SAME。語意工具由合併後原始碼重新編譯到全新的 `target/issue-62/current-203/tool-classes`，不使用先前編譯的工具 class。

對 class 目錄與打包 JAR 各自核對完整 entry 集合，均精確等於 B01.verify_entries 的 22 筆；JAR 非目錄 entry 沒有額外資源。另檢查 JSON 的 22 個類別狀態全部屬允許集合、methodDiffs 為空、missingLeft／extraLeft 均為空；不以 CLI 退出碼 0 單獨判定通過。

### 編譯與語意比對命令

下列是本輪命令的 PowerShell 等價表示。實際執行器為 `scratch/issue-62/verify-current.py`，以 Python subprocess 傳入 argv；每步 cwd、完整 argv、退出碼記在 `scratch/issue-62/current/commands.jsonl`。臨時腳本與產物不進 git。

```powershell
$ROOT = (Get-Location).Path # task-62 worktree 根目錄
$SERVER = Join-Path $ROOT 'lito伺服器/LiTo381-Server'
$JDK = 'C:/Program Files/Microsoft/jdk-17.0.17.10-hotspot'
$PYTHON = 'C:/Users/CHAOREN/AppData/Local/Programs/Python/Python312/python.exe'
$OUT = Join-Path $SERVER 'target/issue-62/current-203'
$WORK = Join-Path $ROOT 'scratch/issue-62/current'
$batch = (Get-Content -Raw -Encoding UTF8 docs/combat-merge/batches.json | ConvertFrom-Json).batches | Where-Object id -eq B01
$sources = @($batch.classes | ForEach-Object { Join-Path $SERVER "merge-inputs/50635f7e/staged/local/lito381/server/$_.java" })
$annotation = Join-Path $SERVER 'target/issue-62/annotation'
$cp = "$SERVER/reference/candidate-50635f7e.jar;$SERVER/reference/base-0.6.71.jar;$annotation;$SERVER/lib/*"
# 輸出目錄先建立；重跑請選新的 OUT，避免沿用舊 class。
& "$JDK/bin/javac.exe" --release 17 -encoding UTF-8 -d $annotation "$SERVER/src/main/java/local/lito381/server/AsRecord.java"
& "$JDK/bin/javac.exe" --release 17 -encoding UTF-8 -cp $cp -d "$OUT/classes" @sources
$env:JAVA_HOME = $JDK
$env:AS_RECORD_MAVEN_REPO = 'D:/dev-cache/maven/repository'
& $PYTHON -B "$SERVER/tools/as-record/as_record.py" "$OUT/classes"
& "$JDK/bin/jar.exe" --create --file "$OUT/staged.jar" --no-manifest -C "$OUT/classes" .
$asm = 'D:/dev-cache/maven/repository/org/ow2/asm/asm/9.7/asm-9.7.jar;D:/dev-cache/maven/repository/org/ow2/asm/asm-tree/9.7/asm-tree-9.7.jar;' + "$SERVER/target/issue-62/dependency/asm-analysis-9.7.jar"
$toolSources = @(Get-ChildItem "$SERVER/tools/semantic-diff/src/main/java" -Recurse -Filter '*.java' | ForEach-Object FullName)
& "$JDK/bin/javac.exe" --release 17 -encoding UTF-8 -cp $asm -d "$OUT/tool-classes" @toolSources
# entries.txt 精確取 batch.verify_entries；兩份核准清單內容見下節。
& "$JDK/bin/java.exe" -Dfile.encoding=UTF-8 -cp "$OUT/tool-classes;$asm" local.lito381.semanticdiff.SemanticDiff `
  --left "$OUT/staged.jar" --right "$SERVER/reference/candidate-50635f7e.jar" `
  --left-classpath "$SERVER/reference/candidate-50635f7e.jar" --classes "$WORK/entries.txt" `
  --effects-methods "$WORK/effects-approved.txt" --exceptions-exempt "$WORK/exceptions-exempt.txt" `
  --json "$WORK/current-203.json" --md "$WORK/current-203.md"
```

### 核准清單

以 B01.verify_entries 裁切所有適用核准清單，只有以下兩筆命中。extra-methods、metadata-exempt、access-widen、layout、deviations 均未啟用；預設正規化與 #203 呼叫點配對保持啟用。Forest 條目沿用本分支既有核准，exceptions-exempt 只豁免指定方法的 Exceptions 屬性。

```text
local/lito381/server/NativeCompanionForestArea#apply(Llocal/lito381/server/ClientSession;Llocal/lito381/server/BattleSpellCatalog$AreaSilenceSpell;IIIIJ)V # batch: B01, category: dup-pop-local, pr: https://github.com/chaoren0418-arch/lito-launcher-server/pull/79
local/lito381/server/NativeWeaponBreakTargetLifecycle#captureOutput(Llocal/lito381/server/ClientSession;[BLjava/lang/String;)Z  # approved: https://github.com/chaoren0418-arch/lito-launcher-server/issues/62#issuecomment-6654588607
```

### 全部 22 個 entry

共同前綴 `local/lito381/server/`。

| Entry | 狀態 |
|---|---|
| `NativeBlindSceneAction.class` | SAME |
| `NativeCompanionForestArea.class` | SAME_EFFECTS |
| `NativeCompanionRefScene.class` | SAME |
| `NativeCompanionWindAction.class` | SAME |
| `NativeControlledNegativeMaterial.class` | SAME |
| `NativePairedCastSafety.class` | SAME |
| `NativeSourceBatchTerminal.class` | SAME |
| `NativeSourceCubePlacement.class` | SAME |
| `NativeSourceCubePlacement$Draft.class` | SAME |
| `NativeSourceDollAccepted.class` | SAME |
| `NativeSourceDollAccepted$Pending.class` | SAME |
| `NativeSourceDollNpc.class` | SAME |
| `NativeSourceDollNpc$Scope.class` | SAME |
| `NativeSourceDollSkills.class` | SAME |
| `NativeSourceDollSkills$View.class` | SAME |
| `NativeSourceDollSlow.class` | SAME |
| `NativeSourceHealingCast.class` | SAME |
| `NativeSourceHealingCast$Cast.class` | SAME |
| `NativeSourcePendingAdmission.class` | SAME |
| `NativeSummonTerminalLifecycle.class` | SAME |
| `NativeWeaponBreakBattleCompanion.class` | SAME |
| `NativeWeaponBreakTargetLifecycle.class` | LAMBDA_PAIRED_BY_CALLSITE |

SAME_PATHS、SAME_CFG、SAME_LAYOUT、DEVIATION、ORPHAN_IGNORED、VISIBILITY_BRIDGE 的類別計數均為 0。呼叫點配對的方法數為 1；其本體走 SAME_PATHS，不應把此方法證明誤記為另一個類別。

### TargetLifecycle 正式配對證據

左側方法：`lambda$captureOutput$8(Llocal/lito381/server/NativeAreaMaterialAction$Context;Llocal/lito381/server/NativePlayerCommit$Binding;Llocal/lito381/server/ClientSession;Ljava/util/List;ZJ[BLjava/lang/String;)V`。

```text
lambda$captureOutput$6(Llocal/lito381/server/NativeAreaMaterialAction$Context;Llocal/lito381/server/NativePlayerCommit$Binding;Llocal/lito381/server/ClientSession;Ljava/util/List;ZJ[BLjava/lang/String;)V; MethodResult[status=SAME_PATHS, detail=path multisets equal (87)]
```

正式 CLI 現在完成 `$8` 與 `$6` 的呼叫點配對，完整方法比較確認 87 條路徑多重集合相同，整類其餘比較亦通過。沒有未解 DIFF 可附；舊差異診斷保留在 Git 歷史，不再混入本輪結果。

## 原始輸出

### `jdk.log`

```text
openjdk version "17.0.17" 2025-10-21 LTS

OpenJDK Runtime Environment Microsoft-12574423 (build 17.0.17+10-LTS)

OpenJDK 64-Bit Server VM Microsoft-12574423 (build 17.0.17+10-LTS, mixed mode, sharing)


exit=0
```

### `current-203-annotation.log`

```text
exit=0
```

### `current-203-javac.log`

```text
Note: Some input files use unchecked or unsafe operations.

Note: Recompile with -Xlint:unchecked for details.


exit=0
```

### `current-203-as-record.log`

```text
as-record: scanned=22 converted=1


exit=0
```

### `current-203-jar.log`

```text
exit=0
```

### `current-203-tool.log`

```text
exit=0
```

### `current-203-semantic.log`

```text
SUMMARY SAME=20 SAME_EFFECTS=1 SAME_PATHS=0 ORPHAN_IGNORED=0 SAME_CFG=0 LAMBDA_PAIRED_BY_CALLSITE=1 VISIBILITY_BRIDGE=0 SAME_LAYOUT=0 DEVIATION=0 DIFF=0 ONLY_LEFT=0 ONLY_RIGHT=0 BYTE_SAME=0 ALLOW_METHODS=0

wrote D:\lineage開發\3-lito開發\scratch\wt\task-62\scratch\issue-62\current\current-203.json

wrote D:\lineage開發\3-lito開發\scratch\wt\task-62\scratch\issue-62\current\current-203.md


exit=0
```

## 產出路徑與雜湊

- 交付報告：`docs/reports/issue-62.md`。
- 17 份 staged Java：`lito伺服器/LiTo381-Server/merge-inputs/50635f7e/staged/local/lito381/server/`，本輪未改。
- 原始證據：`scratch/issue-62/current/` 的 JSON、Markdown、log、argv 收據及 `validation.json`（含 17 份來源 SHA-256、entry 完整性與依賴查核）。
- 隔離建置產物：`lito伺服器/LiTo381-Server/target/issue-62/current-203/`；沒有部署或打包遊戲客戶端。

| 路徑 | SHA-256 |
|---|---|
| `lito伺服器/LiTo381-Server/reference/candidate-50635f7e.jar` | `50635f7edca8ec6ce77787cc32371c7590b497e48ea202f8a679caf3f517b729` |
| `lito伺服器/LiTo381-Server/reference/base-0.6.71.jar` | `e1498871c7be6b287821c57ef5d95933db1740d9b4d4a80f6c3465f21d99d498` |
| `lito伺服器/LiTo381-Server/target/issue-62/current-203/staged.jar` | `23e2811aa37aefd09ff9047b56c55569777c789730f10be6e4d9065e4401bfa4` |
| `scratch/issue-62/current/current-203.json` | `89945161e15e936b6dcdecc2c8de6ca6c41e641930cbf8ec1be352819ef17cb7` |
| `scratch/issue-62/current/validation.json` | `1c08edd7af71a2fde9fec778a5fbede6181d88c613de76c8b859b54d525dc2b4` |
| `scratch/issue-62/current/commands.jsonl` | `4ef777c6939ebfb4fe8bb258512b4be27a590f6420ff341c5b76c872705516e3` |

## 推測

無需以推測補足本批驗收。SAME_EFFECTS 仍是既有核准的副作用摘要比對，不單獨證明參數資料流或分支目的地等價；本報告僅宣告通過主控指定的核准模式。

## 未驗證什麼／待驗證

- 依設計 §6-5，本批單元測試與行為回歸不適用，留待 B-land 統一處理。
- 未執行整個伺服器 Maven 建置、正式程序、登入器、遊戲或真機驗收；未讀機密設定、封存、releases，未連 DB。
- #203 工具自測已有該任務的合併證據，本輪僅重新編譯工具並驗證 B01，不把上游自測列作本輪執行。

## 給下一棒的提示

請 Claude 審查既有 [PR #79](https://github.com/chaoren0418-arch/lito-launcher-server/pull/79) 與本輪 22-entry 證據；本批已無差異阻塞，無需新增 needs:claude 差異裁定。由使用者依流程合併後，B-land 執行器在其餘批次齊備時統一搬入 src/main、執行單元測試與回歸。請保留既有兩筆核准及 Draft 轉換，不再為 lambda 命名調整 Java。
