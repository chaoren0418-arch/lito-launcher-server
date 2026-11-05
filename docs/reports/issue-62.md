# Issue #62／T020-B01：移除 Forest 核准後全批驗證通過

## 結論

**B01 的 17 份 staged Java 重新編譯、轉換後，全部 22 個 entry 通過本批核准語意驗收：21 SAME、1 LAMBDA_PAIRED_BY_CALLSITE；DIFF、ONLY_LEFT、ONLY_RIGHT、missing、extra 全為 0。** 比對時間 `2026-11-05T21:29:57Z`（臺北 2026-11-06）。本輪 Java 不改；前輪的 1 DIFF 已由 #203 正式 CLI 的呼叫點配對解決。

## 做了什麼（已驗證）

- 工作目錄 `D:/lineage開發/3-lito開發/scratch/wt/task-62`，沿用 `task/62`，未 checkout／建立分支。
- 開始 HEAD 為 `a4ce7702ade0b2ae93ce4dd36209baff72742333`；fetch 後將 main `54c908ab43d048ca6b9fe5e069c15734d8f9e8b4` 合入，合併提交 `8b0adbdb3c3e55f86ba6e520f29f4c2128026f3a`，沒有衝突。
- [#203 的 PR #215](https://github.com/chaoren0418-arch/lito-launcher-server/pull/215) 已於 `2026-11-05T20:19:36Z` 合併；`git merge-base --is-ancestor` 確認 merge commit `5ae1eac20d75b75b2bf9fbf302e8d878a32c62bd` 已在本次 HEAD。
- 依[主控裁定](https://github.com/chaoren0418-arch/lito-launcher-server/issues/62#issuecomment-6662268131)，逐一以 bytes 核對 17 份 B01 Java 與 `a4ce770`、`e01c6b9`、`574c188` 完全相同，全部保留指定 DECOMPILED 標頭。
- 保留 DollAccepted 讀取方式、TargetLifecycle null 檢查與 throws、Wind varargs，以及 Draft 的 `@AsRecord`。
- 依[本次退回 P1](https://github.com/chaoren0418-arch/lito-launcher-server/issues/62#issuecomment-6669836396)，刪除 `effects-approved.txt` 的 Forest apply 行。不帶 `--effects-methods` 重驗，Forest 直接為 SAME；已不需要 effects 核准，因此沒有繼續保留 B 類核准或以舊差異作為驗收證據。
- 本輪自行修改的受追蹤檔案為 effects 清單與本報告。effects 清單移除該行後與 main 相同；整個 PR 相對 main 僅 **18 檔：17 份 B01 Java 與本報告**。`docs/REPORT.md` 僅隨 main 合入，與 main 相同，未由本任務編輯。
- 退回 P2 由 PR #79 描述同步處理：提交推送後以最終 head、18 檔與本輪結果取代過期描述。

## 怎麼驗證（已驗證）

使用 Microsoft OpenJDK 17.0.17、ASM／asm-tree／asm-analysis 9.7。註解獨立編譯，僅將 B01 的 17 份 staged Java 傳給 javac，classpath 順序為候選、正式、AsRecord 註解、lib。轉換器輸出 `scanned=22 converted=1`，Draft 驗證為 SAME。語意工具由合併後原始碼重新編譯到全新的 `target/issue-62/review-final/tool-classes`，不使用先前編譯的工具 class。

對 class 目錄與打包 JAR 各自核對完整 entry 集合，均精確等於 B01.verify_entries 的 22 筆；JAR 非目錄 entry 沒有額外資源。另檢查 JSON 的 22 個類別狀態全部屬允許集合、methodDiffs 為空、missingLeft／extraLeft 均為空；不以 CLI 退出碼 0 單獨判定通過。

### 編譯與語意比對命令

下列是本輪命令的 PowerShell 等價表示。實際執行器為 `scratch/issue-62/verify-review-final.py`，以 Python subprocess 傳入 argv；每步 cwd、完整 argv、退出碼記在 `scratch/issue-62/review-final/commands.jsonl`。臨時腳本與產物不進 git。

```powershell
$ROOT = (Get-Location).Path # task-62 worktree 根目錄
$SERVER = Join-Path $ROOT 'lito伺服器/LiTo381-Server'
$JDK = 'C:/Program Files/Microsoft/jdk-17.0.17.10-hotspot'
$PYTHON = 'C:/Users/CHAOREN/AppData/Local/Programs/Python/Python312/python.exe'
$OUT = Join-Path $SERVER 'target/issue-62/review-final'
$WORK = Join-Path $ROOT 'scratch/issue-62/review-final'
$batch = (Get-Content -Raw -Encoding UTF8 docs/combat-merge/batches.json | ConvertFrom-Json).batches | Where-Object id -eq B01
$sources = @($batch.classes | ForEach-Object { Join-Path $SERVER "merge-inputs/50635f7e/staged/local/lito381/server/$_.java" })
$annotation = Join-Path $OUT 'annotation'
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
# entries.txt 精確取 batch.verify_entries；唯一適用的核准清單內容見下節。
& "$JDK/bin/java.exe" -Dfile.encoding=UTF-8 -cp "$OUT/tool-classes;$asm" local.lito381.semanticdiff.SemanticDiff `
  --left "$OUT/staged.jar" --right "$SERVER/reference/candidate-50635f7e.jar" `
  --left-classpath "$SERVER/reference/candidate-50635f7e.jar" --classes "$WORK/entries.txt" `
  --exceptions-exempt "$WORK/exceptions-exempt.txt" `
  --json "$WORK/review-final.json" --md "$WORK/review-final.md"
```

### 核准清單

以 B01.verify_entries 裁切所有適用核准清單，僅以下 exceptions-exempt 條目命中，只豁免指定方法的 Exceptions 屬性。effects-methods、extra-methods、metadata-exempt、access-widen、layout、deviations 均未啟用；預設正規化與 #203 呼叫點配對保持啟用。JSON 的 `effectsMethodsFile=null`、所有類別的 `effectsMethods=[]`，Forest 為 SAME。

```text
local/lito381/server/NativeWeaponBreakTargetLifecycle#captureOutput(Llocal/lito381/server/ClientSession;[BLjava/lang/String;)Z  # approved: https://github.com/chaoren0418-arch/lito-launcher-server/issues/62#issuecomment-6654588607
```

### 全部 22 個 entry

共同前綴 `local/lito381/server/`。

| Entry | 狀態 |
|---|---|
| `NativeBlindSceneAction.class` | SAME |
| `NativeCompanionForestArea.class` | SAME |
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

正式 CLI 現在完成 `$8` 與 `$6` 的呼叫點配對，完整方法比較確認 87 條路徑多重集合相同，整類其餘比較亦通過。本輪沒有未解 DIFF；Forest 已按 SAME 驗收，沒有需要附完整差異的 B 類核准。

## 原始輸出

### `jdk.log`

```text
openjdk version "17.0.17" 2025-10-21 LTS

OpenJDK Runtime Environment Microsoft-12574423 (build 17.0.17+10-LTS)

OpenJDK 64-Bit Server VM Microsoft-12574423 (build 17.0.17+10-LTS, mixed mode, sharing)


exit=0
```

### `review-final-annotation.log`

```text
exit=0
```

### `review-final-javac.log`

```text
Note: Some input files use unchecked or unsafe operations.

Note: Recompile with -Xlint:unchecked for details.


exit=0
```

### `review-final-as-record.log`

```text
as-record: scanned=22 converted=1


exit=0
```

### `review-final-jar.log`

```text
exit=0
```

### `review-final-tool.log`

```text
exit=0
```

### `review-final-semantic.log`

```text
SUMMARY SAME=21 SAME_EFFECTS=0 SAME_PATHS=0 ORPHAN_IGNORED=0 SAME_CFG=0 LAMBDA_PAIRED_BY_CALLSITE=1 VISIBILITY_BRIDGE=0 SAME_LAYOUT=0 DEVIATION=0 DIFF=0 ONLY_LEFT=0 ONLY_RIGHT=0 BYTE_SAME=0 ALLOW_METHODS=0

wrote D:\lineage開發\3-lito開發\scratch\wt\task-62\scratch\issue-62\review-final\review-final.json

wrote D:\lineage開發\3-lito開發\scratch\wt\task-62\scratch\issue-62\review-final\review-final.md


exit=0
```

## 產出路徑與雜湊

- 交付報告：`docs/reports/issue-62.md`。
- 17 份 staged Java：`lito伺服器/LiTo381-Server/merge-inputs/50635f7e/staged/local/lito381/server/`，本輪未改。
- 原始證據：`scratch/issue-62/review-final/` 的 JSON、Markdown、log、argv 收據及 `validation.json`（含 17 份來源 SHA-256、entry 完整性與依賴查核）。
- 隔離建置產物：`lito伺服器/LiTo381-Server/target/issue-62/review-final/`；沒有部署或打包遊戲客戶端。

| 路徑 | SHA-256 |
|---|---|
| `lito伺服器/LiTo381-Server/reference/candidate-50635f7e.jar` | `50635f7edca8ec6ce77787cc32371c7590b497e48ea202f8a679caf3f517b729` |
| `lito伺服器/LiTo381-Server/reference/base-0.6.71.jar` | `e1498871c7be6b287821c57ef5d95933db1740d9b4d4a80f6c3465f21d99d498` |
| `lito伺服器/LiTo381-Server/target/issue-62/review-final/staged.jar` | `a79c74b4cab4b6714bf241953feeacb8201b2a125578ae2f31702ca8ff6d19c7` |
| `scratch/issue-62/review-final/review-final.json` | `939b6515ed7facc304268fbb340eef4989f6683bb77aa08d5e27d90eacc51721` |
| `scratch/issue-62/review-final/validation.json` | `fd9c26a5ea8937eaf877958bfa5e65942348daab98621d02438f130d9e3b25f4` |
| `scratch/issue-62/review-final/commands.jsonl` | `8fcc701ce9d3ca2842a5157581b3c08283dfc55922d44dded994c4816009500a` |

## 推測

無。本批結論依本輪重編、轉換及正式 CLI 全批結果；未使用 SAME_EFFECTS 或推測補足驗收。

## 未驗證什麼／待驗證

- 依設計 §6-5，本批單元測試與行為回歸不適用，留待 B-land 統一處理。
- 未執行整個伺服器 Maven 建置、正式程序、登入器、遊戲或真機驗收；未讀機密設定、封存、releases，未連 DB。
- #203 工具自測已有該任務的合併證據，本輪僅重新編譯工具並驗證 B01，不把上游自測列作本輪執行。

## 給下一棒的提示

請 Claude 審查既有 [PR #79](https://github.com/chaoren0418-arch/lito-launcher-server/pull/79) 與本輪 22-entry 證據；本批已無差異阻塞，無需新增 needs:claude 差異裁定。由使用者依流程合併後，B-land 執行器在其餘批次齊備時統一搬入 src/main、執行單元測試與回歸。請保留 captureOutput 的既有 Exceptions 豁免與 Draft 轉換；Forest 不再需要 effects 核准，不再為 lambda 命名調整 Java。
