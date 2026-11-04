# Issue #77 / T020-A06 — 第三次修正與待裁定證據

**結論：未完成驗收，請維持 `needs:claude`，不得合併或部署。**

本次以 `0c36ca6c74c57b983ead2069d491749b4a12f138` 為修正起點。27 個 staged Java 直接編譯成功，精確產出 69 個驗收 entry；相較前次 40 SAME／29 DIFF，本次為 **61 SAME／8 DIFF**，`missingLeft=[]`、`extraLeft=[]`、`ONLY_LEFT=0`、`ONLY_RIGHT=0`。沒有取得任何驗收例外核准。本報告取代舊版的「29 筆皆無法用 javac 表達」判斷；本次已實際修好其中 21 筆，不能再把所有差異歸為編譯器雜訊。

## 做了什麼

所有變更限於 A06 staged Java 與本報告。本次修改 22 個 Java；另 5 個沿用前次 staged 檔，仍納入完整重編與驗證。未修改 `src/main`、`pom.xml`、比較工具、manifest 或 `docs/REPORT.md`。

### 來源規則與正式差異

下列七類已重新從本 worktree 的 `src/main/java/local/lito381/server/` 原檔複製為底，移植正式差異；保留原有可讀變數、註解與命名，撤回前次以反編譯稿取代原始碼的做法。

| 原始碼檔 | 本次移植 |
|---|---|
| `NativeNpcPhysicalSource.java` | 保留 `connection/database/rows`、快照註解；補 `physicalSize` 的空字串預設值。 |
| `NativePetWorldLifetime.java` | 保留 `pc/hub/target`、player → hub → population 鎖順序註解；加入正式 `NativeWorldMaterialPolicy.run` 呼叫。 |
| `NativeRoomRules.java` | 按正式版移除 GM 虛擬玩家計數及相關型別引用，保留其餘原始說明。 |
| `ResourceDatabase.java` | 依正式順序補 Proc catalog、heal bonus、armor proc 與 De 資源載入；保留原始載入與 snapshot 結構。 |
| `RuntimeInventory.java` | 補正式 `Map` 欄位／建構子型別、同步旗標、`nativeDeBind`、`shuffle` 與四處 `preserveCopy`；保留原始庫存程式與註解。 |
| `ServerConfig.java` | 補 `SharedDatabase.applySources`、`UnifiedConfiguration.loaded`；移除前次將所有 Exception 包成 IOException 的行為。剩餘差異另列，未宣稱 SAME。 |
| `SourceLogin.java` | 在原始可讀程式中移植 `bindSource/acceptSocket/submitAccepted/abortAccept`，包括例外路徑。剩餘 lambda／handler 差異另列。 |

19 個 DECOMPILED 類別保留 `// DECOMPILED from base-0.6.71.jar, TODO review` 標頭。以本機 CFR 0.152 重解正式 class、對照既有 Vineflower 稿與正式指令，修正 early-return、switch 分支、區域變數配置、monitor／try-with-resources 範圍、CAS 迴圈與 finally 路徑。`NativeSourceSkillConsumptionMath`、`NativeSourceClientActionTimeline`（含 Reservation）、`NativeServerLifetime`、所有本批 `NativeProc*`、`SharedClientResources` 現在均為 SAME。

`sources.file=null` 的 `SourceEquipmentPhysicalStats`：本次靜態定位到 `src/main/.../Accounts.java` 的 record 定義，抽出成獨立 staged 檔，保留可讀命名與註解，只移植正式 `SolidCarriageRules.equipment(shieldEquipped, guarderEquipped)`；結果 SAME。正式 class 無 SourceFile，本機未找到可供核對的 Maven `target/classes` class，故**未聲稱已重新驗證 Maven SourceFile 屬性**；此來源證據仍待下一棒補核。

## 已驗證

- Microsoft JDK `javac 17.0.17`，`--release 17 -encoding UTF-8`；classpath 僅正式 JAR 與 `lib/*`。
- 從 `a-batches.json` 的 A06 精選 27 檔，沒有使用 staged 全目錄萬用字元。清除並重建 worktree 內 `target/a06`，再用 javac 產出、用 jar 封裝。
- `left.jar` 只含本次 javac 輸出；沒有複製正式 class、沒有 ASM 後處理、沒有私人 stub，也沒有修改驗收工具。
- 用原 T020d 工具，完整 69 行 `--classes` 清單，核對集合與每 entry SHA-256。3 個 entry 自然編出與正式相同的 bytes（詳見原始結果），並非要求所有 SHA 都不同；編譯過程與乾淨輸出是來源證據。
- `ServerConfig` 以泛型 rethrow helper 原樣拋出原例外。獨立反射檢查確認 IOException、SQLException、IllegalArgumentException、AssertionError 四個物件不被替換，`load` 仍宣告 IOException。這只驗證 helper 與宣告，**未執行 `SharedDatabase.applySources` 或資料庫路徑**。
- `git diff --check` 通過；實際變更清單限本批允許範圍。

正式 JAR SHA-256：`e1498871c7be6b287821c57ef5d95933db1740d9b4d4a80f6c3465f21d99d498`。

本次 staged JAR SHA-256：`feabd1d4219edd2fbdb54291233cf8c3ec47f9d26b15a06406a5443791152c66`（封裝時間固定，完整 entry SHA 見附錄）。

## 八項 DIFF 與裁定請求

以下為完整 canonical diff 的摘要，並非只引用工具的 firstDiff。附錄逐一保留所有差異行，方向為 `- staged`／`+ formal`。唯讀 Dump 沿用原工具的指令、標籤、lambda 正規化與欄位格式，不修改 class 或比較規則。

| Entry（省略共同 package） | 已觀察到的完整差異範圍 | 待主控裁定 |
|---|---|---|
| `NativePcActionAuthority` | 僅 InnerClasses 多 `NativePlayerActionScope$Ticket`；方法已一致。 | 直接 Java 引用巢狀類產生的 metadata 如何納入驗收。 |
| `NativeSkillConsumptionBridges` | access `0x30` 對 `0x10`；javac 有隱式 `<init>`、正式沒有；javac 有 InnerClasses、正式為空。其餘方法一致。 | 正式 class 缺 ACC_SUPER／建構子，標準 Java 類別經本次 javac 無法重現此結構；請明確裁定可接受的合法產出與驗收方式。 |
| `NativeSourceSkillDelay` | 僅 InnerClasses 多 `NativeReuseLog$Logger`。 | 同 metadata 裁定。 |
| `NativeSourceSkillDelay$Lease` | 僅 InnerClasses 多 `NativeReuseLog$Logger`。 | 同 metadata 裁定。 |
| `RuntimeInventory` | `copy/sourceEquipmentOnly/withoutObjects` 四個 return 的 operand stack 順序；正式先取得 result，再 `ALOAD 0; SWAP`，Java 依參數順序先載入 this。呼叫的 `preserveCopy` 相同。 | 需審查等價性及驗收方法；未自行放寬比較。 |
| `RuntimeInventory$Item` | 僅 InnerClasses 多 `InventoryCatalog$NativeProjectileStatus`。 | 同 metadata 裁定。 |
| `ServerConfig` | `applyExternalSourceServers` 多 catch(Exception)＋原樣 rethrow helper（含其方法）；正式直接呼叫 throws Exception 的 `applySources`，但外層宣告 IOException。`load` 的正式插入點用 `DUP2/POP` 保留 Properties，本次 Java 重新 `ALOAD 3`。 | 本次已修正例外型別遭包裝的實際行為錯誤；新增 handler/helper 仍是實質差異，不以標籤雜訊當作通過，請審查／指示合法表達方式。 |
| `SourceLogin` | `start` 的 lambda capture 順序（hub 由原第八參數變第二參數），連動描述子、ALOAD 與 invokedynamic；Throwable handler 使用 local 8 對 10，RETURN 及 try 結束標籤位置不同。 | 捕獲值對應與 abort/rethrow 路徑已靜態核對，尚未實機驗證，不能宣告完全等價；請主控審查剩餘差異。 |

除 `NativeSkillConsumptionBridges` 的 Java 類別結構限制外，不把「本次仍有 DIFF」當成其他所有差異絕對不可表達的證明。若主控有可產生 SAME 的合法 Java 寫法，應繼續修正；若需變更比較規則／編譯輸入，須先有明確裁定與相應任務。本次不執行 class 後處理，也不把 SAME 數字改寫成通過。

依主控[例外處理指示](https://github.com/chaoren0418-arch/lito-launcher-server/issues/77#issuecomment-6636848102)，以完整最小差異提交 `needs:claude`。另依[共用 stub 閘門指示](https://github.com/chaoren0418-arch/lito-launcher-server/issues/77#issuecomment-6638802325)，#108 合併前不能宣告完成。2026-11-05 本次查詢 [#108](https://github.com/chaoren0418-arch/lito-launcher-server/issues/108) 仍為 open／task:running；即使本次 math 直接編譯已 SAME，也未自行解除此閘門。

## 怎麼重現

從本 worktree 根目錄，以 PowerShell 執行下列**完整指令**。會從本報告提取附錄中的 Python，不需要不存在的附件。Python／JDK／ASM 依賴路徑已在腳本中指定；ASM 先核對固定 SHA。腳本只清除本 worktree 的 `lito伺服器/LiTo381-Server/target/a06`，其他暫存進 `scratch/a06`。不啟動應用程式，不連 DB。

```powershell
$ErrorActionPreference = 'Stop'
$root = (Get-Location).Path
$report = Get-Content -Raw -Encoding UTF8 'docs/reports/issue-77.md'
$match = [regex]::Match($report, '(?s)<!-- A06-REPRODUCER -->\s*```python\r?\n(.*?)\r?\n```')
if (-not $match.Success) { throw '找不到報告內完整重現腳本' }
New-Item -ItemType Directory -Force 'scratch/a06' | Out-Null
[IO.File]::WriteAllText((Join-Path $root 'scratch/a06/reproduce.py'), $match.Groups[1].Value, [Text.UTF8Encoding]::new($false))
& 'D:/dev-orchestrator/tools/python/python.exe' -X utf8 'scratch/a06/reproduce.py'
$verificationExit = $LASTEXITCODE
Write-Output "A06 verification exit=$verificationExit"
if ($verificationExit -ne 0) { throw 'A06 尚未達到 69 SAME；查看 scratch/a06/result.json 與 *.diff' }
```

本次預期 Python exit **2**，表示完成報告但驗收 FAIL（61／8），不是 javac 失敗。T020d CLI 自身 exit 0 只表示產報告成功；腳本另外明確檢查 69 SAME，避免以 CLI exit 0 冒充驗收通過。先前的 `/tmp/a06` 路徑與 `<A06 verify_entries 69 行>` 佔位步驟均已撤回。

## 產出路徑與證據

- 追蹤：`lito伺服器/LiTo381-Server/merge-inputs/formal-0.6.71/staged/local/lito381/server/` 中 manifest A06 指定的 27 個 `.java`；本次其中 22 檔變更。
- 追蹤：`docs/reports/issue-77.md`（本檔）。所有 69 筆原始結果、來源／class SHA 與八項完整差異直接嵌在下方，GitHub 可查，不依賴本機暫存存在。
- 不入 git：`scratch/a06/{reproduce.py,sources.txt,verify_entries.txt,result.json,result.md,receipt.json,selection.json,hashes.json,source-hashes.json,javac.log,exception-check.log}`、`scratch/a06/{left,right}/*.txt`、`scratch/a06/*.diff`。
- 不入 git：`lito伺服器/LiTo381-Server/target/a06/{classes,tool,test,dependency,left.jar}`。

## 未驗證／給下一棒的提示

1. **Claude 主控先處理**：審查上述八項完整差異，回覆可接受的修正／驗收方式。現況是有新修正提交的未完成工作，不能標 `review:pass`、合併 PR #109 或發布。沒有核准例外前仍以 69 SAME 為準。
2. **#108 執行者／主控**：完成共用編譯期輸入並確認 A06 閘門；本批未引入私人 stub。請不要把本批獨立編譯成功當成 #108 已完成。
3. **下一位本機執行器**：依明確裁定修正剩餘 staged 類別並重跑同一腳本，保留已達 SAME 的 61 entries；補核 `SourceEquipmentPhysicalStats` 的 Maven SourceFile 證據。
4. **A-land／使用者驗收階段**：本批未執行單元回歸、啟動服務、登入器、遊戲或資料庫；整合／實機行為仍待統一驗證。沒有用假資料宣稱功能驗收。

## 附錄：可查驗收據

<details>
<summary>編譯、集合與 SHA 收據</summary>

```json
{
  "jdk": "javac 17.0.17",
  "formalSHA256": "e1498871c7be6b287821c57ef5d95933db1740d9b4d4a80f6c3465f21d99d498",
  "leftJarSHA256": "feabd1d4219edd2fbdb54291233cf8c3ec47f9d26b15a06406a5443791152c66",
  "selection": {
    "count": 69,
    "missingLeft": [],
    "extraLeft": []
  },
  "counts": {
    "SAME": 61,
    "DIFF": 8,
    "ONLY_LEFT": 0,
    "ONLY_RIGHT": 0,
    "BYTE_SAME": 3
  },
  "sourceFileCount": 27
}
```

</details>

<details>
<summary>javac.log</summary>

```text
Note: Some input files use unchecked or unsafe operations.
Note: Recompile with -Xlint:unchecked for details.
```

</details>

<details>
<summary>exception-check.log</summary>

```text
PASS: 4 exception identities preserved; load IOException API unchanged; no IO/DB executed
```

</details>

<details>
<summary>T020d 原始 result.json：完整 69 entries</summary>

```json
{
  "tool": "semantic-diff",
  "generatedAt": "2026-11-04T20:20:42Z",
  "left": "D:\\lineage開發\\3-lito開發\\scratch\\wt\\task-77\\lito伺服器\\LiTo381-Server\\target\\a06\\left.jar",
  "right": "D:\\lineage開發\\3-lito開發\\scratch\\wt\\task-77\\lito伺服器\\LiTo381-Server\\reference\\base-0.6.71.jar",
  "allowRight": null,
  "leftClassCount": 69,
  "rightClassCount": 2554,
  "comparedClassCount": 69,
  "classSelection": {
    "missingLeft": [],
    "extraLeft": []
  },
  "byteSameCount": 3,
  "counts": {
    "SAME": 61,
    "DIFF": 8,
    "ONLY_LEFT": 0,
    "ONLY_RIGHT": 0,
    "BYTE_SAME": 3
  },
  "classes": [
    {"name": "local/lito381/server/NativeNpcPhysicalSource", "status": "SAME", "byteSame": false, "allowMethodsApplied": false},
    {"name": "local/lito381/server/NativeNpcPhysicalSource$Snapshot", "status": "SAME", "byteSame": false, "allowMethodsApplied": false},
    {"name": "local/lito381/server/NativePcActionAuthority", "status": "DIFF", "byteSame": false, "allowMethodsApplied": false, "firstDiff": {"where": "class", "member": "innerClasses", "insnIndex": null, "detail": "left=local/lito381/server/NativeDeItemTransaction$Operation|local/lito381/server/NativeDeItemTransaction|Operation|0x608; local/lito381/server/NativePlayerActionScope$Ticket|local/lito381/server/NativePlayerActionScope|Ticket|0x18 right=local/lito381/server/NativeDeItemTransaction$Operation|local/lito381/server/NativeDeItemTransaction|Operation|0x608"}},
    {"name": "local/lito381/server/NativePetWorldLifetime", "status": "SAME", "byteSame": false, "allowMethodsApplied": false},
    {"name": "local/lito381/server/NativePetWorldLifetime$Target", "status": "SAME", "byteSame": false, "allowMethodsApplied": false},
    {"name": "local/lito381/server/NativePlayerVitals", "status": "SAME", "byteSame": false, "allowMethodsApplied": false},
    {"name": "local/lito381/server/NativePlayerVitals$Result", "status": "SAME", "byteSame": false, "allowMethodsApplied": false},
    {"name": "local/lito381/server/NativeProcEncounterCatalog", "status": "SAME", "byteSame": false, "allowMethodsApplied": false},
    {"name": "local/lito381/server/NativeProcEncounterCatalog$Catalog", "status": "SAME", "byteSame": false, "allowMethodsApplied": false},
    {"name": "local/lito381/server/NativeProcEncounterCatalog$Npc", "status": "SAME", "byteSame": false, "allowMethodsApplied": false},
    {"name": "local/lito381/server/NativeProcEncounterCatalog$Receiver", "status": "SAME", "byteSame": false, "allowMethodsApplied": false},
    {"name": "local/lito381/server/NativeProcEncounterCatalog$Type", "status": "SAME", "byteSame": false, "allowMethodsApplied": false},
    {"name": "local/lito381/server/NativeProcEncounterPublication", "status": "SAME", "byteSame": false, "allowMethodsApplied": false},
    {"name": "local/lito381/server/NativeProcEncounterPublication$Context", "status": "SAME", "byteSame": false, "allowMethodsApplied": false},
    {"name": "local/lito381/server/NativeProcEncounterPublication$Stale", "status": "SAME", "byteSame": false, "allowMethodsApplied": false},
    {"name": "local/lito381/server/NativeProcEncounterPublication$Viewer", "status": "SAME", "byteSame": false, "allowMethodsApplied": false},
    {"name": "local/lito381/server/NativeProcHealBonus", "status": "SAME", "byteSame": false, "allowMethodsApplied": false},
    {"name": "local/lito381/server/NativeProcHealBonus$Catalog", "status": "SAME", "byteSame": false, "allowMethodsApplied": false},
    {"name": "local/lito381/server/NativeProcHealingWorld", "status": "SAME", "byteSame": false, "allowMethodsApplied": false},
    {"name": "local/lito381/server/NativeProcHealingWorld$Plan", "status": "SAME", "byteSame": false, "allowMethodsApplied": false},
    {"name": "local/lito381/server/NativeProcHealingWorld$Target", "status": "SAME", "byteSame": false, "allowMethodsApplied": false},
    {"name": "local/lito381/server/NativeProcNpcCatalog", "status": "SAME", "byteSame": false, "allowMethodsApplied": false},
    {"name": "local/lito381/server/NativeProcNpcCatalog$1", "status": "SAME", "byteSame": false, "allowMethodsApplied": false},
    {"name": "local/lito381/server/NativeProcNpcCatalog$Catalog", "status": "SAME", "byteSame": false, "allowMethodsApplied": false},
    {"name": "local/lito381/server/NativeProcNpcCatalog$Npc", "status": "SAME", "byteSame": false, "allowMethodsApplied": false},
    {"name": "local/lito381/server/NativeProcNpcState", "status": "SAME", "byteSame": false, "allowMethodsApplied": false},
    {"name": "local/lito381/server/NativeProcPlayerState", "status": "SAME", "byteSame": false, "allowMethodsApplied": false},
    {"name": "local/lito381/server/NativeProcPlayerState$Identity", "status": "SAME", "byteSame": false, "allowMethodsApplied": false},
    {"name": "local/lito381/server/NativeReuseLog", "status": "SAME", "byteSame": false, "allowMethodsApplied": false},
    {"name": "local/lito381/server/NativeReuseLog$Log", "status": "SAME", "byteSame": false, "allowMethodsApplied": false},
    {"name": "local/lito381/server/NativeReuseLog$Logger", "status": "SAME", "byteSame": false, "allowMethodsApplied": false},
    {"name": "local/lito381/server/NativeRoomRules", "status": "SAME", "byteSame": false, "allowMethodsApplied": false},
    {"name": "local/lito381/server/NativeRoomRules$Policy", "status": "SAME", "byteSame": true, "allowMethodsApplied": false},
    {"name": "local/lito381/server/NativeServerLifetime", "status": "SAME", "byteSame": false, "allowMethodsApplied": false},
    {"name": "local/lito381/server/NativeSkillConsumptionBridges", "status": "DIFF", "byteSame": false, "allowMethodsApplied": false, "firstDiff": {"where": "class", "member": "access", "insnIndex": null, "detail": "left=0x30 right=0x10"}},
    {"name": "local/lito381/server/NativeSourceActionIntervalCalculator", "status": "SAME", "byteSame": false, "allowMethodsApplied": false},
    {"name": "local/lito381/server/NativeSourceActionIntervalCalculator$ActionType", "status": "SAME", "byteSame": false, "allowMethodsApplied": false},
    {"name": "local/lito381/server/NativeSourceActionIntervalCalculator$SpeedState", "status": "SAME", "byteSame": false, "allowMethodsApplied": false},
    {"name": "local/lito381/server/NativeSourceClientActionTimeline", "status": "SAME", "byteSame": false, "allowMethodsApplied": false},
    {"name": "local/lito381/server/NativeSourceClientActionTimeline$ActionBlockSnapshot", "status": "SAME", "byteSame": false, "allowMethodsApplied": false},
    {"name": "local/lito381/server/NativeSourceClientActionTimeline$ActionKind", "status": "SAME", "byteSame": false, "allowMethodsApplied": false},
    {"name": "local/lito381/server/NativeSourceClientActionTimeline$CombatDeferral", "status": "SAME", "byteSame": false, "allowMethodsApplied": false},
    {"name": "local/lito381/server/NativeSourceClientActionTimeline$Deadline", "status": "SAME", "byteSame": false, "allowMethodsApplied": false},
    {"name": "local/lito381/server/NativeSourceClientActionTimeline$Reservation", "status": "SAME", "byteSame": false, "allowMethodsApplied": false},
    {"name": "local/lito381/server/NativeSourceClientActionTimeline$VisibleWindow", "status": "SAME", "byteSame": false, "allowMethodsApplied": false},
    {"name": "local/lito381/server/NativeSourceMaterialPlan", "status": "SAME", "byteSame": false, "allowMethodsApplied": false},
    {"name": "local/lito381/server/NativeSourceMaterialPlan$ConsumeRequirement", "status": "SAME", "byteSame": false, "allowMethodsApplied": false},
    {"name": "local/lito381/server/NativeSourceMaterialPlan$Debit", "status": "SAME", "byteSame": false, "allowMethodsApplied": false},
    {"name": "local/lito381/server/NativeSourceMaterialPlan$ItemConsumeCommitResult", "status": "SAME", "byteSame": false, "allowMethodsApplied": false},
    {"name": "local/lito381/server/NativeSourceMaterialPlan$PlannedConsumption", "status": "SAME", "byteSame": false, "allowMethodsApplied": false},
    {"name": "local/lito381/server/NativeSourceMaterialPlan$Result", "status": "SAME", "byteSame": false, "allowMethodsApplied": false},
    {"name": "local/lito381/server/NativeSourceSkillCastAdmission", "status": "SAME", "byteSame": false, "allowMethodsApplied": false},
    {"name": "local/lito381/server/NativeSourceSkillCastAdmission$Reservation", "status": "SAME", "byteSame": false, "allowMethodsApplied": false},
    {"name": "local/lito381/server/NativeSourceSkillCastAdmission$Result", "status": "SAME", "byteSame": false, "allowMethodsApplied": false},
    {"name": "local/lito381/server/NativeSourceSkillConsumptionMath", "status": "SAME", "byteSame": false, "allowMethodsApplied": false},
    {"name": "local/lito381/server/NativeSourceSkillDelay", "status": "DIFF", "byteSame": false, "allowMethodsApplied": false, "firstDiff": {"where": "class", "member": "innerClasses", "insnIndex": null, "detail": "left=java/lang/invoke/MethodHandles$Lookup|java/lang/invoke/MethodHandles|Lookup|0x19; local/lito381/server/NativeReuseLog$Logger|local/lito381/server/NativeReuseLog|Logger|0x608; local/lito381/server/NativeSourceSkillDelay$Lease|local/lito381/s… right=java/lang/invoke/MethodHandles$Lookup|java/lang/invoke/MethodHandles|Lookup|0x19; local/lito381/server/NativeSourceSkillDelay$Lease|local/lito381/server/NativeSourceSkillDelay|Lease|0x19"}},
    {"name": "local/lito381/server/NativeSourceSkillDelay$Lease", "status": "DIFF", "byteSame": false, "allowMethodsApplied": false, "firstDiff": {"where": "class", "member": "innerClasses", "insnIndex": null, "detail": "left=local/lito381/server/NativeReuseLog$Logger|local/lito381/server/NativeReuseLog|Logger|0x608; local/lito381/server/NativeSourceSkillDelay$Lease|local/lito381/server/NativeSourceSkillDelay|Lease|0x19 right=local/lito381/server/NativeSourceSkillDelay$Lease|local/lito381/server/NativeSourceSkillDelay|Lease|0x19"}},
    {"name": "local/lito381/server/ResourceDatabase", "status": "SAME", "byteSame": false, "allowMethodsApplied": false},
    {"name": "local/lito381/server/ResourceDatabase$Key", "status": "SAME", "byteSame": true, "allowMethodsApplied": false},
    {"name": "local/lito381/server/ResourceDatabase$Snapshot", "status": "SAME", "byteSame": true, "allowMethodsApplied": false},
    {"name": "local/lito381/server/RuntimeInventory", "status": "DIFF", "byteSame": false, "allowMethodsApplied": false, "firstDiff": {"where": "method", "member": "copy()Llocal/lito381/server/RuntimeInventory;", "insnIndex": 0, "detail": "left=ALOAD 0 right=NEW local/lito381/server/RuntimeInventory"}},
    {"name": "local/lito381/server/RuntimeInventory$Item", "status": "DIFF", "byteSame": false, "allowMethodsApplied": false, "firstDiff": {"where": "class", "member": "innerClasses", "insnIndex": null, "detail": "left=java/lang/invoke/MethodHandles$Lookup|java/lang/invoke/MethodHandles|Lookup|0x19; local/lito381/server/InventoryCatalog$Definition|local/lito381/server/InventoryCatalog|Definition|0x18; local/lito381/server/InventoryCatalog$NativeArmorStatu… right=java/lang/invoke/MethodHandles$Lookup|java/lang/invoke/MethodHandles|Lookup|0x19; local/lito381/server/InventoryCatalog$Definition|local/lito381/server/InventoryCatalog|Definition|0x18; local/lito381/server/InventoryCatalog$NativeArmorStatu…"}},
    {"name": "local/lito381/server/RuntimeInventory$NativeArmorBonuses", "status": "SAME", "byteSame": false, "allowMethodsApplied": false},
    {"name": "local/lito381/server/RuntimeInventory$PersistentItem", "status": "SAME", "byteSame": false, "allowMethodsApplied": false},
    {"name": "local/lito381/server/ServerConfig", "status": "DIFF", "byteSame": false, "allowMethodsApplied": false, "firstDiff": {"where": "method", "member": "applyExternalSourceServers(Ljava/util/Properties;Ljava/nio/file/Path;)V", "insnIndex": null, "detail": "left=TRY L0 L1 L3 java/lang/Exception   right=TRY L3 L4 L5 java/lang/Throwable  "}},
    {"name": "local/lito381/server/SharedClientResources", "status": "SAME", "byteSame": false, "allowMethodsApplied": false},
    {"name": "local/lito381/server/SourceEquipmentPhysicalStats", "status": "SAME", "byteSame": false, "allowMethodsApplied": false},
    {"name": "local/lito381/server/SourceLogin", "status": "DIFF", "byteSame": false, "allowMethodsApplied": false, "firstDiff": {"where": "method", "member": "lambda$start$#0(Ljava/net/ServerSocket;Ljava/util/concurrent/ExecutorService;Llocal/lito381/server/ClientSession$Endpoint;Llocal/lito381/server/ServerConfig;Llocal/lito381/server/AccountRepository;Llocal/lito381/server/GameStateStore;Llocal/lito381/server/CaptureLog;Llocal/lito381/server/GameHub;)V", "insnIndex": null, "detail": "only right"}},
    {"name": "local/lito381/server/SourceLogin$Binding", "status": "SAME", "byteSame": false, "allowMethodsApplied": false}
  ]
}
```

</details>

<details>
<summary>27 個 staged 原始碼 SHA-256</summary>

```json
{
  "lito伺服器\\LiTo381-Server\\merge-inputs\\formal-0.6.71\\staged\\local\\lito381\\server\\NativeNpcPhysicalSource.java": "d9c983bb01d84824e3981d917d6752fbdd60bae001843a4728fbbf82585e50a1",
  "lito伺服器\\LiTo381-Server\\merge-inputs\\formal-0.6.71\\staged\\local\\lito381\\server\\NativePcActionAuthority.java": "a84d1dc0432a5fc32f69edc530ed248b99885676fa31676f82339d4ceb70e0f3",
  "lito伺服器\\LiTo381-Server\\merge-inputs\\formal-0.6.71\\staged\\local\\lito381\\server\\NativePetWorldLifetime.java": "66a186272d0fe393a2c5baabea5fca6d5d7b1223637002ea614e0c52bd5d7129",
  "lito伺服器\\LiTo381-Server\\merge-inputs\\formal-0.6.71\\staged\\local\\lito381\\server\\NativePlayerVitals.java": "e22e34800ad38a1bb7feabaa59a4c4593ec141a4d7c9ab5138d62ef9f82e0847",
  "lito伺服器\\LiTo381-Server\\merge-inputs\\formal-0.6.71\\staged\\local\\lito381\\server\\NativeProcEncounterCatalog.java": "3884c3b2a4883f9d1bcc05b41f05ca9597ea3248e2c4c94e2d026894a0bf8c29",
  "lito伺服器\\LiTo381-Server\\merge-inputs\\formal-0.6.71\\staged\\local\\lito381\\server\\NativeProcEncounterPublication.java": "8c35684e1d5233ed280750d78fc5b0417d1dcead1b01d59f92acfbb9c1c41d4a",
  "lito伺服器\\LiTo381-Server\\merge-inputs\\formal-0.6.71\\staged\\local\\lito381\\server\\NativeProcHealBonus.java": "ca692eb3cf5f7ff82c73387f2d8d05b896d27817e33ad9b4e66d545bae37705a",
  "lito伺服器\\LiTo381-Server\\merge-inputs\\formal-0.6.71\\staged\\local\\lito381\\server\\NativeProcHealingWorld.java": "59ac3a69011ce12b023b2503d7c1d71e3245901022befe567f5f4ddb5ab16247",
  "lito伺服器\\LiTo381-Server\\merge-inputs\\formal-0.6.71\\staged\\local\\lito381\\server\\NativeProcNpcCatalog.java": "711fe378e66b74ece5d3be7466df69357af18fbe7ccd185b6c9eed76c2e0ab56",
  "lito伺服器\\LiTo381-Server\\merge-inputs\\formal-0.6.71\\staged\\local\\lito381\\server\\NativeProcNpcState.java": "ce4383aaaf6df20863a45588135606005231f241f58b7f253f988f9a191027d2",
  "lito伺服器\\LiTo381-Server\\merge-inputs\\formal-0.6.71\\staged\\local\\lito381\\server\\NativeProcPlayerState.java": "9b90c2ba5b93d6650381534bd45a4cc92dc86375d0776d9bdf7a384d317badab",
  "lito伺服器\\LiTo381-Server\\merge-inputs\\formal-0.6.71\\staged\\local\\lito381\\server\\NativeReuseLog.java": "e1b31d231e4f7859a3c35fc8d6d1584ad24a16775f1d85c4c9ac5523c094088d",
  "lito伺服器\\LiTo381-Server\\merge-inputs\\formal-0.6.71\\staged\\local\\lito381\\server\\NativeRoomRules.java": "3532632f11a557960ec3512459a8ab2f3508a84cc9a24357f1bd9e3d934e3b04",
  "lito伺服器\\LiTo381-Server\\merge-inputs\\formal-0.6.71\\staged\\local\\lito381\\server\\NativeServerLifetime.java": "694696194051ffb2f616f725b0dd7d7f92cc9a87cf77c1901e312fd043b13b72",
  "lito伺服器\\LiTo381-Server\\merge-inputs\\formal-0.6.71\\staged\\local\\lito381\\server\\NativeSkillConsumptionBridges.java": "28a22f6858f61e208b8e768192d9263d106c7da997dea56af48eb5eb37b7f712",
  "lito伺服器\\LiTo381-Server\\merge-inputs\\formal-0.6.71\\staged\\local\\lito381\\server\\NativeSourceActionIntervalCalculator.java": "281d2f16eb14cb651c577ea384c4eeb9904e9508a2bc8507b0f09bfdcf1acb69",
  "lito伺服器\\LiTo381-Server\\merge-inputs\\formal-0.6.71\\staged\\local\\lito381\\server\\NativeSourceClientActionTimeline.java": "8ee7217f729a9b861390c96ca0e26a604fb7328987fd07dd4f1aab67bea2b4fc",
  "lito伺服器\\LiTo381-Server\\merge-inputs\\formal-0.6.71\\staged\\local\\lito381\\server\\NativeSourceMaterialPlan.java": "ea4bf62dae169afa30ca249d6da0fd463b1e711a31c8ca1e67e4aaa846be8e54",
  "lito伺服器\\LiTo381-Server\\merge-inputs\\formal-0.6.71\\staged\\local\\lito381\\server\\NativeSourceSkillCastAdmission.java": "3173281b4e0b1f5d227e2c4f9b0b5978fb71d60d2f9d09080294a596dc1207d5",
  "lito伺服器\\LiTo381-Server\\merge-inputs\\formal-0.6.71\\staged\\local\\lito381\\server\\NativeSourceSkillConsumptionMath.java": "f87e24cc4a4549e58ecc4867be9fc2226c2b6eabc94b887efaee8afeb614b9bd",
  "lito伺服器\\LiTo381-Server\\merge-inputs\\formal-0.6.71\\staged\\local\\lito381\\server\\NativeSourceSkillDelay.java": "ee3a198f746a85f059298b3107f13cb0e0c87b50e60ce45f8db54539a124ae98",
  "lito伺服器\\LiTo381-Server\\merge-inputs\\formal-0.6.71\\staged\\local\\lito381\\server\\ResourceDatabase.java": "b98c10b88b66049ea74b497cc7b23a4b099ddf7fc96c9b49f011500b364a668e",
  "lito伺服器\\LiTo381-Server\\merge-inputs\\formal-0.6.71\\staged\\local\\lito381\\server\\RuntimeInventory.java": "56abd33b073603cba648f521fb1d6e6767be54417cfb4499f6ca0e2957074088",
  "lito伺服器\\LiTo381-Server\\merge-inputs\\formal-0.6.71\\staged\\local\\lito381\\server\\ServerConfig.java": "f457fded2de768a68c6b4f7863c7a0d5881a418604c03c33c2f3a3f6eff5cb96",
  "lito伺服器\\LiTo381-Server\\merge-inputs\\formal-0.6.71\\staged\\local\\lito381\\server\\SharedClientResources.java": "a19a2bdc22a632c5f3dff1599c77a4d5ed7978471894a27b28705393263ae2e9",
  "lito伺服器\\LiTo381-Server\\merge-inputs\\formal-0.6.71\\staged\\local\\lito381\\server\\SourceEquipmentPhysicalStats.java": "2075ba867e94a84728bd5093ce152a13d1ddcc43f2dddb8f5339f974ee25f485",
  "lito伺服器\\LiTo381-Server\\merge-inputs\\formal-0.6.71\\staged\\local\\lito381\\server\\SourceLogin.java": "ef4f1fffb6227d093772a447de78afa90dc417093e13c4490c8bea12f6e7741a"
}
```

</details>

<details>
<summary>69 個 entry：staged／formal SHA-256</summary>

```json
{
  "local/lito381/server/NativeNpcPhysicalSource$Snapshot.class": {
    "left": "a0d6a7303c638151026d588844ebe1afb1302e076bfb55bca71db5bfe553da61",
    "right": "e9ee7a381fc276cb2dc58c92eb089165698d316ce1dfaa1b27a300ec10f0061a"
  },
  "local/lito381/server/NativeNpcPhysicalSource.class": {
    "left": "dcf20bf0bfebbeef177a9dbd4aa30b3f10666d35ef0de186ac0f813368d78c06",
    "right": "4483cc96b37a908280bbb4a90330bf4b3af21fcbff6e2d0d130aa62d48c1306f"
  },
  "local/lito381/server/NativePcActionAuthority.class": {
    "left": "47bbacd907fbea3c9de9fe6889b09cd67ba2f365d76774b788eec95726f7922d",
    "right": "f23ce6433408bca24495932afeb1395a4e5a79d39de9b2856062dc0c342a0a43"
  },
  "local/lito381/server/NativePetWorldLifetime$Target.class": {
    "left": "1ce767d76fee332e9614a2975f19fcd797cf823bbb2ed8400024fbf6c4dd45ee",
    "right": "eeffc3047bbba9e094d3e62ff685e99af998e026a8a1e55c2e0d0945ba64c133"
  },
  "local/lito381/server/NativePetWorldLifetime.class": {
    "left": "a18a9ceef346274e4840a4b43f669fcd7cc2e2571aea781fe90a87c7e1b2b456",
    "right": "78eac7b97859727759574406d1f2f17cdb276b4b42d08b38654f9aafb4725376"
  },
  "local/lito381/server/NativePlayerVitals$Result.class": {
    "left": "2f07c545ed4559a071056eff74156cce102719d016d867b3434bafdd1df14cde",
    "right": "dfdcc1091cdb465e3d0dd6cdfe52fc9c96a674350cc43f51195537477e564c96"
  },
  "local/lito381/server/NativePlayerVitals.class": {
    "left": "a8cb62cbb8d2591cd64d9fd7220df858162a205edaa075380d923423bdf8269f",
    "right": "6831350184b17a6b9994b617b0b8e9e34d2a602b6fc564d3d17eaa84547bd7ca"
  },
  "local/lito381/server/NativeProcEncounterCatalog$Catalog.class": {
    "left": "36318cba72467d302980057a52d5309214654f42be84114947772458239d05f6",
    "right": "97f39b05880aec013e905febd9f97f8256b01230bf37f4b1d9880e663608efe4"
  },
  "local/lito381/server/NativeProcEncounterCatalog$Npc.class": {
    "left": "3ef39aa6f0cd1102ec6e2f60592355f84f8027b6edf68dabf4732ac7d2a43678",
    "right": "93e8757af8fe912bc07315689fe46cf667d28fc00e8cca1ed712d324a7b1467b"
  },
  "local/lito381/server/NativeProcEncounterCatalog$Receiver.class": {
    "left": "228c6f8799b382c5b268d75a471c381a481ba631edc6ef6ed52dff0cfcc6b919",
    "right": "133daae7e3101178969c96543c1242729701a2bd0fc817cd5b1abdb167620051"
  },
  "local/lito381/server/NativeProcEncounterCatalog$Type.class": {
    "left": "3bff3fcaed940180802b2187bcb40fe04ecf772634404078d006df5b09a67b0b",
    "right": "b57332a2c567d6fc229e9dd5aeef8896098ea22d27e54a0cb71ce63a2f7da058"
  },
  "local/lito381/server/NativeProcEncounterCatalog.class": {
    "left": "26c5776a1c3c1af530281401c11c93acaeed9bb147c22b88b43658893a9f1535",
    "right": "b9443a850b5cc08be658f5780fd3f10a11c1ce0b2b2ac267fbceaa5be00f51c2"
  },
  "local/lito381/server/NativeProcEncounterPublication$Context.class": {
    "left": "822a2a8269b47bc1b5273e97a4d4fd96f724b9f76187566714f3b4607978143c",
    "right": "9b630f5987f2650f82d89efd8a8212f7dcf52ae3dd4f4024d894bbed7ed7f0de"
  },
  "local/lito381/server/NativeProcEncounterPublication$Stale.class": {
    "left": "8428c70a5d2bdc29715c4af028d6e4b10634ce81f3d5cbc878a0a1805198c762",
    "right": "cd98cb1e7581e887a4c66fbbc3b3f1c921a9e90ce2565e2ddfd7f04ae77b81cd"
  },
  "local/lito381/server/NativeProcEncounterPublication$Viewer.class": {
    "left": "0174e4f47a4426f797c3c82ad87a760083bfb651edc3c17030a36ea1fe6bf974",
    "right": "e955534228be4d36f905fce2fc5873b925d6ea41cc3ecac833eb18a8c52750ac"
  },
  "local/lito381/server/NativeProcEncounterPublication.class": {
    "left": "60b22bc8d179aa62060b5348f344796c399cd1964136d243981339847fdb4e4c",
    "right": "808bb506b46f32077705b710b6a896c3988e320581651e650b491410d5104ebc"
  },
  "local/lito381/server/NativeProcHealBonus$Catalog.class": {
    "left": "004bbb582c0eef8c5d57f8fa43dec4e382256be6e4ad7049ea2f1cf0672e6d90",
    "right": "2d29c9dda30dd647006cfb4f9deb7679c3c2a9a2e6c34aecf883919279471636"
  },
  "local/lito381/server/NativeProcHealBonus.class": {
    "left": "323fd4e284219d45e4c316340d543ddf934251b73528b8f6cd37302136c9afa7",
    "right": "462ac097ca94da2621c8418ba02e73860461bf78b5d22f2db38725f5566473c1"
  },
  "local/lito381/server/NativeProcHealingWorld$Plan.class": {
    "left": "e255972515dd344b1fe1758490fcabebc287c57f30d7b4c85ce8b2aae2975488",
    "right": "140a5f053bf29ad7a4522c8ceece9fda3efcaee6e335f3e641637da9dd3c25a2"
  },
  "local/lito381/server/NativeProcHealingWorld$Target.class": {
    "left": "52af8749e30012ada3f5e8fca6879791c02a0c97461f7273c6ad02afa55f69ba",
    "right": "985f6dcedb4b550ea50c4d9541de889b764248b3513039c5411d7e6b9414f389"
  },
  "local/lito381/server/NativeProcHealingWorld.class": {
    "left": "a6e79d0ecf1b8d41ba43f8a0642927fa3b8a8111b060e4a4d194fe90417439bb",
    "right": "3eee0a1e42c41b33c08a02f170dff3a2216f542d2802ee68f2c88ea1fb108aad"
  },
  "local/lito381/server/NativeProcNpcCatalog$1.class": {
    "left": "ed7856aa23d3cb1838eeea79ff29d56ea3e142fad2e58f3c6b9487ccbb37ba8c",
    "right": "c20624f35801d6e8d04a48414f531244199fd05ec0abdd33f79eb8f67f4805e6"
  },
  "local/lito381/server/NativeProcNpcCatalog$Catalog.class": {
    "left": "da3c562bf3c5c59c3eaf6e637a66ab919420fe7222eac13f9e86cb7502964b75",
    "right": "e1e28c933ad4d2caa3be04b2c484937698d21d749f8f99131567b14c8f961bb2"
  },
  "local/lito381/server/NativeProcNpcCatalog$Npc.class": {
    "left": "532c308fbe006c2e11ed9b1414aaff2a96ce48df48146c92490424f60ff63e58",
    "right": "aefca6ba8e6c184426725745b3c86fe2938bc25f6f5a0a1cabcfe19943aa6b78"
  },
  "local/lito381/server/NativeProcNpcCatalog.class": {
    "left": "eebb52c3ca63e17b75799aa5af36cb0074d07a2929d6c7600488cf0368bfca70",
    "right": "b820351f060983056923ef48c00034f915f520e63938c1e70488379e37e61c1e"
  },
  "local/lito381/server/NativeProcNpcState.class": {
    "left": "a69e340282b80751afdd45583a3af2b8fa400dd5029ba075b3b134543a024624",
    "right": "a4c0ad80383d7a4efc88835010dfac4e5da68f4f6d2fa4ec08af594c3bee7d20"
  },
  "local/lito381/server/NativeProcPlayerState$Identity.class": {
    "left": "e0d1032a2f552741adc49d5e53560e2a9194075df558c4c04e8631c75cd400b8",
    "right": "b043021fbcfa0495e16df8db4ca7885bb8490b0f1601289361b30042e7e72522"
  },
  "local/lito381/server/NativeProcPlayerState.class": {
    "left": "6d97095dcc2711bfc3e44561015cd6bcf82a87c55e0b67698db72caa1191dc8c",
    "right": "5366685a602c0b4627ea85f39a3d57b0cfe20989571f87e5bea746f677cbf51a"
  },
  "local/lito381/server/NativeReuseLog$Log.class": {
    "left": "2d944efd6bd0c30ef14d7bb2bdd33d85dbbb528bfaa9fdaa65ef49c87247ac46",
    "right": "7ee2d2e237560fcdcb87fe49230f87813b972fca462daf4df86be6404c3b16fb"
  },
  "local/lito381/server/NativeReuseLog$Logger.class": {
    "left": "2bf21886a3f096b079bcc8fc72e832bc73634ed561aad72957c02268a96abe3a",
    "right": "ca34143aed6a11922caf59efb6551e0c466ea026b116a8cdf835eabb299b6cf0"
  },
  "local/lito381/server/NativeReuseLog.class": {
    "left": "bd551b4124fac5e8ee0feffaa65400ae93ebcfb2f65252e66c7184e9bc0432e2",
    "right": "5a782bba7267c3d26ed1fb44fdee26fad8a1df9c2e5752eadb75ede0519b4c81"
  },
  "local/lito381/server/NativeRoomRules$Policy.class": {
    "left": "56c56a522540c177a99e0a614d588cbcf26e9145010bfcdee6c098d109d10d4e",
    "right": "56c56a522540c177a99e0a614d588cbcf26e9145010bfcdee6c098d109d10d4e"
  },
  "local/lito381/server/NativeRoomRules.class": {
    "left": "e8c124c93996ab3f842056185f7791ef2d4460c032117db289b8269c2d64a1be",
    "right": "5c3816334dbdcae11737137d9e2d31bd21a88f76a92c4976be2a429526160d38"
  },
  "local/lito381/server/NativeServerLifetime.class": {
    "left": "019a17bd6343472e578472acff259f213ba8e266718cc889ecbff5b358a7aa31",
    "right": "35b7a2ddde729dc1d14ba7c969126958291549f49c52dc9310472de4cbf1c0a8"
  },
  "local/lito381/server/NativeSkillConsumptionBridges.class": {
    "left": "b3f4711a5b66695f7ae2c161a6efdc5de23868c7b5733ed03944623f4fcd0b5d",
    "right": "32048edfe236e43fb8f07b3dac93cfdba6a46b3b872eed3ca8c20ec99a49e700"
  },
  "local/lito381/server/NativeSourceActionIntervalCalculator$ActionType.class": {
    "left": "da08a9e344a83d21571bd9ff4a0c217b61891810672d2267dd95d4a616e7145b",
    "right": "2bd9bd5594ff9d294d2e3206dad505151bdbee6179852df9a712d216d282ec0e"
  },
  "local/lito381/server/NativeSourceActionIntervalCalculator$SpeedState.class": {
    "left": "d959d487c07a3c638ee1c170a4512ef07bd39065cec94ae9695f71fb17d9cb9a",
    "right": "991bf922fc39292fa66113cf803bd7993fa0a571f310c9dcd2033393c64eb135"
  },
  "local/lito381/server/NativeSourceActionIntervalCalculator.class": {
    "left": "942a39d285507651e4c24d75be664f734d8c91979a4d757aa0c71cf874804bf1",
    "right": "6b3ee16beab64e85bc5437f35915be29284d9f2cd7b645a7cf00e458df16dd78"
  },
  "local/lito381/server/NativeSourceClientActionTimeline$ActionBlockSnapshot.class": {
    "left": "808b1d9abd41ed7210a2790d1fbea85aa8d651e1f07418f68317356aed5dcf28",
    "right": "86574e6cf88715552c7818e0b3ede5c391264e763961a89b52690d61d75fd172"
  },
  "local/lito381/server/NativeSourceClientActionTimeline$ActionKind.class": {
    "left": "02ab9f9fec99c150382af41ca9289e293d233ba0aae5b484d23d7185702764ff",
    "right": "2ea65f27b2d827b2780862f4782095ad3cf3700ba98c7d886a11ebd4a80a3ef6"
  },
  "local/lito381/server/NativeSourceClientActionTimeline$CombatDeferral.class": {
    "left": "1a95d2c3bc82070fa600ed4b6c9029a6518dbdceb755133fb0a6cd738bac3fea",
    "right": "574b4f2a8bdae959e428ede8ca8a88172c7566a28a7f73575af9e1e2c960189d"
  },
  "local/lito381/server/NativeSourceClientActionTimeline$Deadline.class": {
    "left": "7950f164c0724a7cc43f2618ba0f23fdc34331a961cf68d2b8704d14418be428",
    "right": "0d88d9d315161a6a86e70788e48a1faaccdde27e6c95280b2fb23e49527c7259"
  },
  "local/lito381/server/NativeSourceClientActionTimeline$Reservation.class": {
    "left": "aeb561417f27649fb783064e420914ec99ff0c24454c4d2dd3b21a3748d0926d",
    "right": "92bb6fac7967f359d7f16526364ae7f0d7c7ea851b33c0203fd861d207fb4b48"
  },
  "local/lito381/server/NativeSourceClientActionTimeline$VisibleWindow.class": {
    "left": "1e08d796c061a459ccd4114d45510e79dfbcbf381c313a33b799c65e86a3b4fc",
    "right": "46bd03b5d49c62418fe6f154ffaca43dfe8fe690ab2f52bdc0c8ce74292b51b4"
  },
  "local/lito381/server/NativeSourceClientActionTimeline.class": {
    "left": "68658f0baa812c4637059e8ed0ff27ce60d97e1c92cf4bca479c5a9e5c8b2a7f",
    "right": "8e62bcd57caf9b368ea2fb9fa8e4d8d78b9caf48798e71ff120d978416b219b9"
  },
  "local/lito381/server/NativeSourceMaterialPlan$ConsumeRequirement.class": {
    "left": "11f635ed2cf2fe4e512aaa4080eae9f2d938fc45f388bf5e8fe70a070d18f47e",
    "right": "e3449d655ef1c8164820c192061016c292cd9c2d6950ddb3de3c714fdbffe684"
  },
  "local/lito381/server/NativeSourceMaterialPlan$Debit.class": {
    "left": "1a1449ab45f02de8aa33e79253219b144ff89e2521d8fc38b0073482fba11493",
    "right": "4b65bc750a0287c29f36b97cea600ddfea19f18a560e134d976a1545368ee67d"
  },
  "local/lito381/server/NativeSourceMaterialPlan$ItemConsumeCommitResult.class": {
    "left": "ee1a5f9efaeb62450e498387b93498b372c64276551700decd9bc99ac3c327f2",
    "right": "3d8c734629300f6b5ac8e8d8d90f57d1dbdd9129850c72a21ae2114b101b7d32"
  },
  "local/lito381/server/NativeSourceMaterialPlan$PlannedConsumption.class": {
    "left": "224b1065871cce8b6039e9c56eed6af56f62bc2951f46e44d61f14aed6dadf31",
    "right": "9d5f44b54d411cc454986c21ec278efcdc9db3f747cc163e864cef251d22d003"
  },
  "local/lito381/server/NativeSourceMaterialPlan$Result.class": {
    "left": "931da8df600eb991db78b91dc6d42320891703e117ab6f9c23f5d88dc6c3861e",
    "right": "c8d6699f39cfdf06df5b3f7daa78b12f3bd2ea73107e8b94f6b891a6b5eb7317"
  },
  "local/lito381/server/NativeSourceMaterialPlan.class": {
    "left": "60c8cf9370ef369fa807a3183062745fc09a2aea7751f489ca002fb0ca2641c3",
    "right": "802fcb9425ecc2703b23e430a68ac108a29bcd355b3997dbd175123e7f33ec56"
  },
  "local/lito381/server/NativeSourceSkillCastAdmission$Reservation.class": {
    "left": "5367a42072a28f40c47d23839fcf8f1adf1319c0681606d1ad9cc5f6cf2f4233",
    "right": "50253d1dbedf15f6db5443c991bdc0a00b23be40a81b0d9233e9f5381931f6a2"
  },
  "local/lito381/server/NativeSourceSkillCastAdmission$Result.class": {
    "left": "2790693296bc793df55d649f4b7122b140bda9ea81e0cbb95f8aaca0bb5a2d6e",
    "right": "f5d17e8221c67073c6ae7e3fba610e598eaec65595cbd75ab4f6aac306689756"
  },
  "local/lito381/server/NativeSourceSkillCastAdmission.class": {
    "left": "38abe5d83efdc82862c804f80f87e06c3d10027d69f75c69f96bfee765458fb9",
    "right": "31a3aaf929476a54ce11e7e43d20f60428e96631d6e311ff08398356c8177d97"
  },
  "local/lito381/server/NativeSourceSkillConsumptionMath.class": {
    "left": "74287537265010b41a115d5c88c7dd74c3455e8b0437507e36969f75b3862f89",
    "right": "50d61503ba73feeeb829e1e8cf07507d561db232b759941403e95a352d047124"
  },
  "local/lito381/server/NativeSourceSkillDelay$Lease.class": {
    "left": "21179972f56d9fd3f18ec52f67247a1a7c44582e401abcfd788c24a52ea3590e",
    "right": "7a8bc642ab10108b19111579cb6c7be10e5031179afe230139531938452fce8f"
  },
  "local/lito381/server/NativeSourceSkillDelay.class": {
    "left": "e6b7e5820b31e45448b62019b9ee8e79f95431485381ce61a3c0f1ddb6f757b3",
    "right": "94f132c975dcce47449c14afcf067aa1d1b08b07c2234e1b784b57672280cbd3"
  },
  "local/lito381/server/ResourceDatabase$Key.class": {
    "left": "ea03e403786f5ca228707bd9aa030aacbb2d7ee0b4299a3c58fa1c76590f7ae4",
    "right": "ea03e403786f5ca228707bd9aa030aacbb2d7ee0b4299a3c58fa1c76590f7ae4"
  },
  "local/lito381/server/ResourceDatabase$Snapshot.class": {
    "left": "d4210470b1984884cfd22128081a2ab7117a79ebe1f49ad2871e069865f439ce",
    "right": "d4210470b1984884cfd22128081a2ab7117a79ebe1f49ad2871e069865f439ce"
  },
  "local/lito381/server/ResourceDatabase.class": {
    "left": "121ad811405b70811d969d897bd22e6d7a3c546e2e24343e25daa17ae397f6ed",
    "right": "98fb64a14b91a193d21ebb8f226f84a1fd1a55b18bf323fa21238eda9079136f"
  },
  "local/lito381/server/RuntimeInventory$Item.class": {
    "left": "e76471fb8cd2522376e837395fadfba325b034b83221e6ca503456a85c224186",
    "right": "55dd46973616eb70901d27fc0cef97dc1fc126b4e0f5fc205c1b457172356160"
  },
  "local/lito381/server/RuntimeInventory$NativeArmorBonuses.class": {
    "left": "b6ab6bb78b8195366ca8329d7917ef94acf1c6c45d5198ed05c67ff2940c2148",
    "right": "a0656e56268f5c1485e4dc4f0da644671e7b6439033e5db4858d44d68a85c1ec"
  },
  "local/lito381/server/RuntimeInventory$PersistentItem.class": {
    "left": "67975edf0c630bbdc66328bea9f1ccde670d10f5a9162d2198f9f073c7df15e5",
    "right": "f15d4161e7e304be1cb18ad737fc05379ee732aa8a2380f547a6d3af701a1b49"
  },
  "local/lito381/server/RuntimeInventory.class": {
    "left": "dfe50fa741e48558fa1483c9b1e76827564b9dc17004afb5da6df9d22b732076",
    "right": "0f27a328a8a448577543f832a16e7e0a9cf1e2abec0dacec84ffb4c5e279ac44"
  },
  "local/lito381/server/ServerConfig.class": {
    "left": "e55bdb1f8ddb9d4bb9fa11261b6df2e8cf83ef29ac2759b2384a72492b481993",
    "right": "8f60f1dc859ea435a541167121ccf71dfeb2725f3c7008411e0c04a917e9cfb4"
  },
  "local/lito381/server/SharedClientResources.class": {
    "left": "da5fc859f29e6580b1a99040a77a303b5ed2458175c891edbea880410bee54db",
    "right": "30ee07e57c92583169c48c53e7b100b888687dd9dff9f8adec46c51b39cb38fd"
  },
  "local/lito381/server/SourceEquipmentPhysicalStats.class": {
    "left": "b497f2ebab4d4130fa4f77e64565287584894a719604d3ef10094102c417700d",
    "right": "c8df6d7f751ea69d126bf4d4b1eda560dc45c73eb020d72c7781942f9ca3224e"
  },
  "local/lito381/server/SourceLogin$Binding.class": {
    "left": "b094b4a716aabe615af25dc78ba5bd1aaccac9a9f8c247dd5abecfc18e839235",
    "right": "b700d91ec1600091030b945d54b7bfcdd547d65000b7cac9f196d06481aea43f"
  },
  "local/lito381/server/SourceLogin.class": {
    "left": "8ad0e235dcb2a04aaf10355bb742cb20f1494046399935e40ca4794322002dc0",
    "right": "ee8eb030e64444bf9585a796b5ee8ff88fd8ad92c3036546bb22ee370dd977c1"
  }
}
```

</details>

## 附錄：八項完整 canonical diff

以下未截斷；Markdown 顯示僅移除行尾空白，重現腳本產生的原始 diff 保留原值。`NativeSkillConsumptionBridges` 的 InnerClasses 單行較長，可用水平捲動檢視。其餘 61 個 entry 的診斷 diff 為空。

<details>
<summary>NativePcActionAuthority</summary>

```diff
--- staged/NativePcActionAuthority
+++ formal/NativePcActionAuthority
@@ -3,5 +3,5 @@
 PERMITTED []
 ANNOTATIONS |||
-INNER local/lito381/server/NativeDeItemTransaction$Operation|local/lito381/server/NativeDeItemTransaction|Operation|0x608; local/lito381/server/NativePlayerActionScope$Ticket|local/lito381/server/NativePlayerActionScope|Ticket|0x18
+INNER local/lito381/server/NativeDeItemTransaction$Operation|local/lito381/server/NativeDeItemTransaction|Operation|0x608
 RECORD
 METHOD <init>()V access=2 signature=null exceptions=[]
```

</details>

<details>
<summary>NativeSkillConsumptionBridges</summary>

```diff
--- staged/NativeSkillConsumptionBridges
+++ formal/NativeSkillConsumptionBridges
@@ -1,14 +1,8 @@
-CLASS local/lito381/server/NativeSkillConsumptionBridges access=48 super=java/lang/Object signature=null interfaces=[]
+CLASS local/lito381/server/NativeSkillConsumptionBridges access=16 super=java/lang/Object signature=null interfaces=[]
 NEST null [] OUTER null null null
 PERMITTED []
 ANNOTATIONS |||
-INNER local/lito381/server/BattleSpellCatalog$AbilityBuffSpell|local/lito381/server/BattleSpellCatalog|AbilityBuffSpell|0x18; local/lito381/server/BattleSpellCatalog$AbsoluteBarrierSpell|local/lito381/server/BattleSpellCatalog|AbsoluteBarrierSpell|0x18; local/lito381/server/BattleSpellCatalog$AdditionalFireSpell|local/lito381/server/BattleSpellCatalog|AdditionalFireSpell|0x18; local/lito381/server/BattleSpellCatalog$AdvanceSpiritSpell|local/lito381/server/BattleSpellCatalog|AdvanceSpiritSpell|0x18; local/lito381/server/BattleSpellCatalog$AquaProtectSpell|local/lito381/server/BattleSpellCatalog|AquaProtectSpell|0x18; local/lito381/server/BattleSpellCatalog$AreaAttackSpell|local/lito381/server/BattleSpellCatalog|AreaAttackSpell|0x18; local/lito381/server/BattleSpellCatalog$AreaSilenceSpell|local/lito381/server/BattleSpellCatalog|AreaSilenceSpell|0x18; local/lito381/server/BattleSpellCatalog$ArmorBreakSpell|local/lito381/server/BattleSpellCatalog|ArmorBreakSpell|0x18; local/lito381/server/BattleSpellCatalog$AttackSpell|local/lito381/server/BattleSpellCatalog|AttackSpell|0x18; local/lito381/server/BattleSpellCatalog$AwakenSpell|local/lito381/server/BattleSpellCatalog|AwakenSpell|0x18; local/lito381/server/BattleSpellCatalog$BindSpell|local/lito381/server/BattleSpellCatalog|BindSpell|0x18; local/lito381/server/BattleSpellCatalog$BlindSpell|local/lito381/server/BattleSpellCatalog|BlindSpell|0x18; local/lito381/server/BattleSpellCatalog$BloodlustSpell|local/lito381/server/BattleSpellCatalog|BloodlustSpell|0x18; local/lito381/server/BattleSpellCatalog$BounceAttackSpell|local/lito381/server/BattleSpellCatalog|BounceAttackSpell|0x18; local/lito381/server/BattleSpellCatalog$BringStoneSpell|local/lito381/server/BattleSpellCatalog|BringStoneSpell|0x18; local/lito381/server/BattleSpellCatalog$CallClanSpell|local/lito381/server/BattleSpellCatalog|CallClanSpell|0x18; local/lito381/server/BattleSpellCatalog$CancellationSpell|local/lito381/server/BattleSpellCatalog|CancellationSpell|0x18; local/lito381/server/BattleSpellCatalog$ClearMindSpell|local/lito381/server/BattleSpellCatalog|ClearMindSpell|0x18; local/lito381/server/BattleSpellCatalog$CombatBuffSpell|local/lito381/server/BattleSpellCatalog|CombatBuffSpell|0x18; local/lito381/server/BattleSpellCatalog$CounterBarrierSpell|local/lito381/server/BattleSpellCatalog|CounterBarrierSpell|0x18; local/lito381/server/BattleSpellCatalog$CounterDetectionSpell|local/lito381/server/BattleSpellCatalog|CounterDetectionSpell|0x18; local/lito381/server/BattleSpellCatalog$CounterMagicSpell|local/lito381/server/BattleSpellCatalog|CounterMagicSpell|0x18; local/lito381/server/BattleSpellCatalog$CounterMirrorSpell|local/lito381/server/BattleSpellCatalog|CounterMirrorSpell|0x18; local/lito381/server/BattleSpellCatalog$CreateZombieSpell|local/lito381/server/BattleSpellCatalog|CreateZombieSpell|0x18; local/lito381/server/BattleSpellCatalog$CubeHarmonySpell|local/lito381/server/BattleSpellCatalog|CubeHarmonySpell|0x18; local/lito381/server/BattleSpellCatalog$CubeIgnitionSpell|local/lito381/server/BattleSpellCatalog|CubeIgnitionSpell|0x18; local/lito381/server/BattleSpellCatalog$CubeQuakeSpell|local/lito381/server/BattleSpellCatalog|CubeQuakeSpell|0x18; local/lito381/server/BattleSpellCatalog$CubeShockSpell|local/lito381/server/BattleSpellCatalog|CubeShockSpell|0x18; local/lito381/server/BattleSpellCatalog$CurseRemovalSpell|local/lito381/server/BattleSpellCatalog|CurseRemovalSpell|0x18; local/lito381/server/BattleSpellCatalog$DamageProtectionSpell|local/lito381/server/BattleSpellCatalog|DamageProtectionSpell|0x18; local/lito381/server/BattleSpellCatalog$DecayPotionSpell|local/lito381/server/BattleSpellCatalog|DecayPotionSpell|0x18; local/lito381/server/BattleSpellCatalog$DetectionSpell|local/lito381/server/BattleSpellCatalog|DetectionSpell|0x18; local/lito381/server/BattleSpellCatalog$DiamondGolemSpell|local/lito381/server/BattleSpellCatalog|DiamondGolemSpell|0x18; local/lito381/server/BattleSpellCatalog$DirectionalLineSpell|local/lito381/server/BattleSpellCatalog|DirectionalLineSpell|0x608; local/lito381/server/BattleSpellCatalog$DodgeBuffSpell|local/lito381/server/BattleSpellCatalog|DodgeBuffSpell|0x18; local/lito381/server/BattleSpellCatalog$EarthBlessSpell|local/lito381/server/BattleSpellCatalog|EarthBlessSpell|0x18; local/lito381/server/BattleSpellCatalog$ElementalFallDownSpell|local/lito381/server/BattleSpellCatalog|ElementalFallDownSpell|0x18; local/lito381/server/BattleSpellCatalog$ElementalProtectionSpell|local/lito381/server/BattleSpellCatalog|ElementalProtectionSpell|0x18; local/lito381/server/BattleSpellCatalog$ElementalSummonSpell|local/lito381/server/BattleSpellCatalog|ElementalSummonSpell|0x18; local/lito381/server/BattleSpellCatalog$EnchantVenomSpell|local/lito381/server/BattleSpellCatalog|EnchantVenomSpell|0x18; local/lito381/server/BattleSpellCatalog$EntangleSpell|local/lito381/server/BattleSpellCatalog|EntangleSpell|0x18; local/lito381/server/BattleSpellCatalog$EraseMagicSpell|local/lito381/server/BattleSpellCatalog|EraseMagicSpell|0x18; local/lito381/server/BattleSpellCatalog$ExoticVitalizeSpell|local/lito381/server/BattleSpellCatalog|ExoticVitalizeSpell|0x18; local/lito381/server/BattleSpellCatalog$FireBlessSpell|local/lito381/server/BattleSpellCatalog|FireBlessSpell|0x18; local/lito381/server/BattleSpellCatalog$FireWeaponSpell|local/lito381/server/BattleSpellCatalog|FireWeaponSpell|0x18; local/lito381/server/BattleSpellCatalog$FoeSlayerSpell|local/lito381/server/BattleSpellCatalog|FoeSlayerSpell|0x18; local/lito381/server/BattleSpellCatalog$FreezeAttackSpell|local/lito381/server/BattleSpellCatalog|FreezeAttackSpell|0x18; local/lito381/server/BattleSpellCatalog$GroundSpell|local/lito381/server/BattleSpellCatalog|GroundSpell|0x18; local/lito381/server/BattleSpellCatalog$GroupHealingSpell|local/lito381/server/BattleSpellCatalog|GroupHealingSpell|0x18; local/lito381/server/BattleSpellCatalog$GuardBrakeSpell|local/lito381/server/BattleSpellCatalog|GuardBrakeSpell|0x18; local/lito381/server/BattleSpellCatalog$HasteSpell|local/lito381/server/BattleSpellCatalog|HasteSpell|0x18; local/lito381/server/BattleSpellCatalog$HealingSpell|local/lito381/server/BattleSpellCatalog|HealingSpell|0x18; local/lito381/server/BattleSpellCatalog$IllusionAvatarSpell|local/lito381/server/BattleSpellCatalog|IllusionAvatarSpell|0x18; local/lito381/server/BattleSpellCatalog$IllusionLichSpell|local/lito381/server/BattleSpellCatalog|IllusionLichSpell|0x18; local/lito381/server/BattleSpellCatalog$IllusionOgreSpell|local/lito381/server/BattleSpellCatalog|IllusionOgreSpell|0x18; local/lito381/server/BattleSpellCatalog$InsightSpell|local/lito381/server/BattleSpellCatalog|InsightSpell|0x18; local/lito381/server/BattleSpellCatalog$InvisibilitySpell|local/lito381/server/BattleSpellCatalog|InvisibilitySpell|0x18; local/lito381/server/BattleSpellCatalog$IronSkinSpell|local/lito381/server/BattleSpellCatalog|IronSkinSpell|0x18; local/lito381/server/BattleSpellCatalog$ItemArmorEnchantSpell|local/lito381/server/BattleSpellCatalog|ItemArmorEnchantSpell|0x18; local/lito381/server/BattleSpellCatalog$ItemWeaponEnchantSpell|local/lito381/server/BattleSpellCatalog|ItemWeaponEnchantSpell|0x18; local/lito381/server/BattleSpellCatalog$JoyOfPainSpell|local/lito381/server/BattleSpellCatalog|JoyOfPainSpell|0x18; local/lito381/server/BattleSpellCatalog$LightSpell|local/lito381/server/BattleSpellCatalog|LightSpell|0x18; local/lito381/server/BattleSpellCatalog$LineAttackSpell|local/lito381/server/BattleSpellCatalog|LineAttackSpell|0x18; local/lito381/server/BattleSpellCatalog$MagicResistanceSpell|local/lito381/server/BattleSpellCatalog|MagicResistanceSpell|0x18; local/lito381/server/BattleSpellCatalog$MagicalWeaponSpell|local/lito381/server/BattleSpellCatalog|MagicalWeaponSpell|0x18; local/lito381/server/BattleSpellCatalog$ManaDrainSpell|local/lito381/server/BattleSpellCatalog|ManaDrainSpell|0x18; local/lito381/server/BattleSpellCatalog$MassSlowSpell|local/lito381/server/BattleSpellCatalog|MassSlowSpell|0x18; local/lito381/server/BattleSpellCatalog$MassTeleportSpell|local/lito381/server/BattleSpellCatalog|MassTeleportSpell|0x18; local/lito381/server/BattleSpellCatalog$MovementBuffSpell|local/lito381/server/BattleSpellCatalog|MovementBuffSpell|0x18; local/lito381/server/BattleSpellCatalog$NaturesBlessingSpell|local/lito381/server/BattleSpellCatalog|NaturesBlessingSpell|0x18; local/lito381/server/BattleSpellCatalog$NaturesTouchSpell|local/lito381/server/BattleSpellCatalog|NaturesTouchSpell|0x18; local/lito381/server/BattleSpellCatalog$OtherNoEffectSpell|local/lito381/server/BattleSpellCatalog|OtherNoEffectSpell|0x18; local/lito381/server/BattleSpellCatalog$PanicSpell|local/lito381/server/BattleSpellCatalog|PanicSpell|0x18; local/lito381/server/BattleSpellCatalog$ParalysisSpell|local/lito381/server/BattleSpellCatalog|ParalysisSpell|0x18; local/lito381/server/BattleSpellCatalog$PatienceSpell|local/lito381/server/BattleSpellCatalog|PatienceSpell|0x18; local/lito381/server/BattleSpellCatalog$PhantasmSpell|local/lito381/server/BattleSpellCatalog|PhantasmSpell|0x18; local/lito381/server/BattleSpellCatalog$PhysicalProcBuffSpell|local/lito381/server/BattleSpellCatalog|PhysicalProcBuffSpell|0x18; local/lito381/server/BattleSpellCatalog$PoisonResistanceSpell|local/lito381/server/BattleSpellCatalog|PoisonResistanceSpell|0x18; local/lito381/server/BattleSpellCatalog$PoisonSpell|local/lito381/server/BattleSpellCatalog|PoisonSpell|0x18; local/lito381/server/BattleSpellCatalog$PolymorphSpell|local/lito381/server/BattleSpellCatalog|PolymorphSpell|0x18; local/lito381/server/BattleSpellCatalog$RangedEvasionBuffSpell|local/lito381/server/BattleSpellCatalog|RangedEvasionBuffSpell|0x18; local/lito381/server/BattleSpellCatalog$ReductionArmorSpell|local/lito381/server/BattleSpellCatalog|ReductionArmorSpell|0x18; local/lito381/server/BattleSpellCatalog$RegenSpell|local/lito381/server/BattleSpellCatalog|RegenSpell|0x18; local/lito381/server/BattleSpellCatalog$ResistElementalSpell|local/lito381/server/BattleSpellCatalog|ResistElementalSpell|0x18; local/lito381/server/BattleSpellCatalog$ResistFearSpell|local/lito381/server/BattleSpellCatalog|ResistFearSpell|0x18; local/lito381/server/BattleSpellCatalog$ResurrectionSpell|local/lito381/server/BattleSpellCatalog|ResurrectionSpell|0x18; local/lito381/server/BattleSpellCatalog$ReturnToNatureSpell|local/lito381/server/BattleSpellCatalog|ReturnToNatureSpell|0x18; local/lito381/server/BattleSpellCatalog$RevealWeaknessSpell|local/lito381/server/BattleSpellCatalog|RevealWeaknessSpell|0x18; local/lito381/server/BattleSpellCatalog$RoyalAuraSpell|local/lito381/server/BattleSpellCatalog|RoyalAuraSpell|0x18; local/lito381/server/BattleSpellCatalog$RunClanSpell|local/lito381/server/BattleSpellCatalog|RunClanSpell|0x18; local/lito381/server/BattleSpellCatalog$SelfHasteSpell|local/lito381/server/BattleSpellCatalog|SelfHasteSpell|0x18; local/lito381/server/BattleSpellCatalog$ShieldSpell|local/lito381/server/BattleSpellCatalog|ShieldSpell|0x18; local/lito381/server/BattleSpellCatalog$ShockStunSpell|local/lito381/server/BattleSpellCatalog|ShockStunSpell|0x18; local/lito381/server/BattleSpellCatalog$SilenceSpell|local/lito381/server/BattleSpellCatalog|SilenceSpell|0x18; local/lito381/server/BattleSpellCatalog$SleepSpell|local/lito381/server/BattleSpellCatalog|SleepSpell|0x18; local/lito381/server/BattleSpellCatalog$SolidCarriageSpell|local/lito381/server/BattleSpellCatalog|SolidCarriageSpell|0x18; local/lito381/server/BattleSpellCatalog$StormEyeSpell|local/lito381/server/BattleSpellCatalog|StormEyeSpell|0x18; local/lito381/server/BattleSpellCatalog$StormShotSpell|local/lito381/server/BattleSpellCatalog|StormShotSpell|0x18; local/lito381/server/BattleSpellCatalog$SummonSpell|local/lito381/server/BattleSpellCatalog|SummonSpell|0x18; local/lito381/server/BattleSpellCatalog$TamingSpell|local/lito381/server/BattleSpellCatalog|TamingSpell|0x18; local/lito381/server/BattleSpellCatalog$TargetNegativeStatusSpell|local/lito381/server/BattleSpellCatalog|TargetNegativeStatusSpell|0x608; local/lito381/server/BattleSpellCatalog$TripleArrowSpell|local/lito381/server/BattleSpellCatalog|TripleArrowSpell|0x18; local/lito381/server/BattleSpellCatalog$TrueTargetSpell|local/lito381/server/BattleSpellCatalog|TrueTargetSpell|0x18; local/lito381/server/BattleSpellCatalog$TurnUndeadSpell|local/lito381/server/BattleSpellCatalog|TurnUndeadSpell|0x18; local/lito381/server/BattleSpellCatalog$WaterLifeSpell|local/lito381/server/BattleSpellCatalog|WaterLifeSpell|0x18; local/lito381/server/BattleSpellCatalog$WeaknessSpell|local/lito381/server/BattleSpellCatalog|WeaknessSpell|0x18; local/lito381/server/BattleSpellCatalog$WeaponBreakSpell|local/lito381/server/BattleSpellCatalog|WeaponBreakSpell|0x18; local/lito381/server/BattleSpellCatalog$WeaponBuffSpell|local/lito381/server/BattleSpellCatalog|WeaponBuffSpell|0x18; local/lito381/server/BattleSpellCatalog$WeightSpell|local/lito381/server/BattleSpellCatalog|WeightSpell|0x18; local/lito381/server/BattleSpellCatalog$WindShackleSpell|local/lito381/server/BattleSpellCatalog|WindShackleSpell|0x18; local/lito381/server/BattleSpellCatalog$WindShotSpell|local/lito381/server/BattleSpellCatalog|WindShotSpell|0x18; local/lito381/server/BattleSpellCatalog$WorldTreeCallSpell|local/lito381/server/BattleSpellCatalog|WorldTreeCallSpell|0x18
+INNER
 RECORD
-METHOD <init>()V access=0 signature=null exceptions=[]
-ANNOTATIONS ||||pvis=|pinv=|lvtvis=|lvtinv=
-DEFAULT
-ALOAD 0
-INVOKESPECIAL java/lang/Object.<init>()V
-RETURN
 METHOD AbilityBuffSpell_effectiveMpCost_IIZI(Llocal/lito381/server/BattleSpellCatalog$AbilityBuffSpell;IIZ)I access=8 signature=null exceptions=[]
 ANNOTATIONS ||||pvis=|pinv=|lvtvis=|lvtinv=
```

</details>

<details>
<summary>NativeSourceSkillDelay</summary>

```diff
--- staged/NativeSourceSkillDelay
+++ formal/NativeSourceSkillDelay
@@ -3,5 +3,5 @@
 PERMITTED []
 ANNOTATIONS |||
-INNER java/lang/invoke/MethodHandles$Lookup|java/lang/invoke/MethodHandles|Lookup|0x19; local/lito381/server/NativeReuseLog$Logger|local/lito381/server/NativeReuseLog|Logger|0x608; local/lito381/server/NativeSourceSkillDelay$Lease|local/lito381/server/NativeSourceSkillDelay|Lease|0x19
+INNER java/lang/invoke/MethodHandles$Lookup|java/lang/invoke/MethodHandles|Lookup|0x19; local/lito381/server/NativeSourceSkillDelay$Lease|local/lito381/server/NativeSourceSkillDelay|Lease|0x19
 RECORD
 FIELD ACTIVE_COUNT:Ljava/util/concurrent/atomic/AtomicLong; 26 null null annotations=|||
```

</details>

<details>
<summary>NativeSourceSkillDelay$Lease</summary>

```diff
--- staged/NativeSourceSkillDelay$Lease
+++ formal/NativeSourceSkillDelay$Lease
@@ -3,5 +3,5 @@
 PERMITTED []
 ANNOTATIONS |||
-INNER local/lito381/server/NativeReuseLog$Logger|local/lito381/server/NativeReuseLog|Logger|0x608; local/lito381/server/NativeSourceSkillDelay$Lease|local/lito381/server/NativeSourceSkillDelay|Lease|0x19
+INNER local/lito381/server/NativeSourceSkillDelay$Lease|local/lito381/server/NativeSourceSkillDelay|Lease|0x19
 RECORD
 FIELD _cancelOpen:Ljava/util/concurrent/atomic/AtomicBoolean; 18 null null annotations=|||
```

</details>

<details>
<summary>RuntimeInventory</summary>

```diff
--- staged/RuntimeInventory
+++ formal/RuntimeInventory
@@ -1321,5 +1321,4 @@
 ANNOTATIONS ||||pvis=|pinv=|lvtvis=|lvtinv=
 DEFAULT
-ALOAD 0
 NEW local/lito381/server/RuntimeInventory
 DUP
@@ -1332,4 +1331,6 @@
 GETFIELD local/lito381/server/RuntimeInventory.sourcePhysicalWeapons:Ljava/util/Map;
 INVOKESPECIAL local/lito381/server/RuntimeInventory.<init>(Ljava/util/Map;Ljava/util/Map;)V
+ALOAD 0
+SWAP
 INVOKESTATIC local/lito381/server/NativeDeItemInventory.preserveCopy(Llocal/lito381/server/RuntimeInventory;Llocal/lito381/server/RuntimeInventory;)Llocal/lito381/server/RuntimeInventory;
 ARETURN
@@ -4102,6 +4103,7 @@
 ALOAD 3
 INVOKEVIRTUAL local/lito381/server/RuntimeInventory.validateSourcePhysicalWeapons()V
-ALOAD 0
-ALOAD 3
+ALOAD 3
+ALOAD 0
+SWAP
 INVOKESTATIC local/lito381/server/NativeDeItemInventory.preserveCopy(Llocal/lito381/server/RuntimeInventory;Llocal/lito381/server/RuntimeInventory;)Llocal/lito381/server/RuntimeInventory;
 ARETURN
@@ -4721,6 +4723,7 @@
 LABEL L0
 ALOAD 0
-ALOAD 0
 INVOKEVIRTUAL local/lito381/server/RuntimeInventory.copy()Llocal/lito381/server/RuntimeInventory;
+ALOAD 0
+SWAP
 INVOKESTATIC local/lito381/server/NativeDeItemInventory.preserveCopy(Llocal/lito381/server/RuntimeInventory;Llocal/lito381/server/RuntimeInventory;)Llocal/lito381/server/RuntimeInventory;
 ARETURN
@@ -4778,5 +4781,4 @@
 GOTO L5
 LABEL L7
-ALOAD 0
 NEW local/lito381/server/RuntimeInventory
 DUP
@@ -4784,4 +4786,6 @@
 ALOAD 3
 INVOKESPECIAL local/lito381/server/RuntimeInventory.<init>(Ljava/util/Map;Ljava/util/Map;)V
+ALOAD 0
+SWAP
 INVOKESTATIC local/lito381/server/NativeDeItemInventory.preserveCopy(Llocal/lito381/server/RuntimeInventory;Llocal/lito381/server/RuntimeInventory;)Llocal/lito381/server/RuntimeInventory;
 ARETURN
```

</details>

<details>
<summary>RuntimeInventory$Item</summary>

```diff
--- staged/RuntimeInventory$Item
+++ formal/RuntimeInventory$Item
@@ -3,5 +3,5 @@
 PERMITTED []
 ANNOTATIONS |||
-INNER java/lang/invoke/MethodHandles$Lookup|java/lang/invoke/MethodHandles|Lookup|0x19; local/lito381/server/InventoryCatalog$Definition|local/lito381/server/InventoryCatalog|Definition|0x18; local/lito381/server/InventoryCatalog$NativeArmorStatus|local/lito381/server/InventoryCatalog|NativeArmorStatus|0x18; local/lito381/server/InventoryCatalog$NativeProjectileStatus|local/lito381/server/InventoryCatalog|NativeProjectileStatus|0x18; local/lito381/server/InventoryCatalog$NativeWeaponStatus|local/lito381/server/InventoryCatalog|NativeWeaponStatus|0x18; local/lito381/server/Protocol$InventoryWireItem|local/lito381/server/Protocol|InventoryWireItem|0x18; local/lito381/server/Protocol$Writer|local/lito381/server/Protocol|Writer|0x18; local/lito381/server/RuntimeInventory$Item|local/lito381/server/RuntimeInventory|Item|0x18; local/lito381/server/RuntimeInventory$PersistentItem|local/lito381/server/RuntimeInventory|PersistentItem|0x18
+INNER java/lang/invoke/MethodHandles$Lookup|java/lang/invoke/MethodHandles|Lookup|0x19; local/lito381/server/InventoryCatalog$Definition|local/lito381/server/InventoryCatalog|Definition|0x18; local/lito381/server/InventoryCatalog$NativeArmorStatus|local/lito381/server/InventoryCatalog|NativeArmorStatus|0x18; local/lito381/server/InventoryCatalog$NativeWeaponStatus|local/lito381/server/InventoryCatalog|NativeWeaponStatus|0x18; local/lito381/server/Protocol$InventoryWireItem|local/lito381/server/Protocol|InventoryWireItem|0x18; local/lito381/server/Protocol$Writer|local/lito381/server/Protocol|Writer|0x18; local/lito381/server/RuntimeInventory$Item|local/lito381/server/RuntimeInventory|Item|0x18; local/lito381/server/RuntimeInventory$PersistentItem|local/lito381/server/RuntimeInventory|PersistentItem|0x18
 RECORD objectId:J sig= anns=| typeAnns=|; definition:Llocal/lito381/server/InventoryCatalog$Definition; sig= anns=| typeAnns=|; quantity:I sig= anns=| typeAnns=|; equipped:Z sig= anns=| typeAnns=|; durability:I sig= anns=| typeAnns=|; enchantLevel:I sig= anns=| typeAnns=|; identified:Z sig= anns=| typeAnns=|; blessing:I sig= anns=| typeAnns=|; sourceBound:Z sig= anns=| typeAnns=|; tournamentReward:Z sig= anns=| typeAnns=|; chargeCount:I sig= anns=| typeAnns=|; expiresAtEpochMillis:J sig= anns=| typeAnns=|; sealed:Z sig= anns=| typeAnns=|; litoOwned:Z sig= anns=| typeAnns=|
 FIELD blessing:I 18 null null annotations=|||
```

</details>

<details>
<summary>ServerConfig</summary>

```diff
--- staged/ServerConfig
+++ formal/ServerConfig
@@ -924,21 +924,12 @@
 ANNOTATIONS ||||pvis=|pinv=|lvtvis=|lvtinv=
 DEFAULT
-TRY L0 L1 L3 java/lang/Exception
-TRY L7 L8 L9 java/lang/Throwable
-TRY L10 L11 L12 java/lang/Throwable
-LABEL L0
+TRY L3 L4 L5 java/lang/Throwable
+TRY L6 L7 L8 java/lang/Throwable
 ALOAD 0
 ALOAD 1
 INVOKESTATIC local/lito381/server/SharedDatabase.applySources(Ljava/util/Properties;Ljava/nio/file/Path;)Z
-IFEQ L2
-LABEL L1
+IFEQ L0
 RETURN
-LABEL L2
-GOTO L4
-LABEL L3
-ASTORE 2
-ALOAD 2
-INVOKESTATIC local/lito381/server/ServerConfig.rethrowSourceFailure(Ljava/lang/Throwable;)V
-LABEL L4
+LABEL L0
 ALOAD 0
 LDC str:"source.servers.file"
@@ -947,7 +938,7 @@
 ALOAD 2
 INVOKEVIRTUAL java/lang/String.isEmpty()Z
-IFEQ L5
+IFEQ L1
 RETURN
-LABEL L5
+LABEL L1
 ALOAD 1
 ALOAD 2
@@ -958,7 +949,7 @@
 ANEWARRAY java/nio/file/LinkOption
 INVOKESTATIC java/nio/file/Files.isRegularFile(Ljava/nio/file/Path;[Ljava/nio/file/LinkOption;)Z
-IFNE L6
+IFNE L2
 RETURN
-LABEL L6
+LABEL L2
 NEW java/util/Properties
 DUP
@@ -969,32 +960,32 @@
 INVOKESTATIC java/nio/file/Files.newBufferedReader(Ljava/nio/file/Path;Ljava/nio/charset/Charset;)Ljava/io/BufferedReader;
 ASTORE 5
-LABEL L7
+LABEL L3
 ALOAD 4
 ALOAD 5
 INVOKEVIRTUAL java/util/Properties.load(Ljava/io/Reader;)V
-LABEL L8
+LABEL L4
 ALOAD 5
-IFNULL L14
+IFNULL L10
 ALOAD 5
 INVOKEVIRTUAL java/io/Reader.close()V
-GOTO L14
-LABEL L9
+GOTO L10
+LABEL L5
 ASTORE 6
 ALOAD 5
-IFNULL L13
-LABEL L10
+IFNULL L9
+LABEL L6
 ALOAD 5
 INVOKEVIRTUAL java/io/Reader.close()V
-LABEL L11
-GOTO L13
-LABEL L12
+LABEL L7
+GOTO L9
+LABEL L8
 ASTORE 7
 ALOAD 6
 ALOAD 7
 INVOKEVIRTUAL java/lang/Throwable.addSuppressed(Ljava/lang/Throwable;)V
-LABEL L13
+LABEL L9
 ALOAD 6
 ATHROW
-LABEL L14
+LABEL L10
 ALOAD 4
 LDC str:"source.count"
@@ -1033,8 +1024,8 @@
 ICONST_1
 ISTORE 12
-LABEL L15
+LABEL L11
 ILOAD 12
 ILOAD 5
-IF_ICMPGT L18
+IF_ICMPGT L14
 ILOAD 12
 INVOKEDYNAMIC makeConcatWithConstants(I)Ljava/lang/String; bsm=H6 java/lang/invoke/StringConcatFactory.makeConcatWithConstants(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;Ljava/lang/String;[Ljava/lang/Object;)Ljava/lang/invoke/CallSite; args=[str:"source.\u0001."]
@@ -1073,10 +1064,10 @@
 ALOAD 14
 INVOKEVIRTUAL java/lang/String.isEmpty()Z
-IFNE L16
+IFNE L12
 ICONST_1
-GOTO L17
-LABEL L16
-ICONST_0
-LABEL L17
+GOTO L13
+LABEL L12
+ICONST_0
+LABEL L13
 IOR
 ISTORE 11
@@ -1090,13 +1081,13 @@
 POP
 IINC var=12 incr=1
-GOTO L15
-LABEL L18
+GOTO L11
+LABEL L14
 ILOAD 11
-IFEQ L19
+IFEQ L15
 ALOAD 9
 INVOKEINTERFACE java/util/List.stream()Ljava/util/stream/Stream; itf
 INVOKEDYNAMIC test()Ljava/util/function/Predicate; bsm=H6 java/lang/invoke/LambdaMetafactory.metafactory(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodHandle;Ljava/lang/invoke/MethodType;)Ljava/lang/invoke/CallSite; args=[type:(Ljava/lang/Object;)Z, handle:H5 java/lang/String.isEmpty()Z, type:(Ljava/lang/String;)Z]
 INVOKEINTERFACE java/util/stream/Stream.anyMatch(Ljava/util/function/Predicate;)Z itf
-IFEQ L19
+IFEQ L15
 NEW java/lang/IllegalArgumentException
 DUP
@@ -1104,5 +1095,5 @@
 INVOKESPECIAL java/lang/IllegalArgumentException.<init>(Ljava/lang/String;)V
 ATHROW
-LABEL L19
+LABEL L15
 ALOAD 0
 LDC str:"login.classic.server.names"
@@ -1134,5 +1125,5 @@
 POP
 ILOAD 11
-IFEQ L20
+IFEQ L16
 ALOAD 0
 LDC str:"database.source.resource.databases"
@@ -1142,5 +1133,5 @@
 INVOKEVIRTUAL java/util/Properties.setProperty(Ljava/lang/String;Ljava/lang/String;)Ljava/lang/Object;
 POP
-LABEL L20
+LABEL L16
 RETURN
 METHOD applyServiceMode(Ljava/util/Properties;Ljava/lang/String;)V access=10 signature=null exceptions=[]
@@ -2264,6 +2255,7 @@
 ALOAD 3
 ALOAD 4
+DUP2
 INVOKEVIRTUAL java/util/Properties.load(Ljava/io/Reader;)V
-ALOAD 3
+POP
 ALOAD 0
 INVOKESTATIC local/lito381/server/UnifiedConfiguration.loaded(Ljava/util/Properties;Ljava/nio/file/Path;)V
@@ -5136,9 +5128,4 @@
 INVOKEINTERFACE java/nio/file/Path.toAbsolutePath()Ljava/nio/file/Path; itf
 ARETURN
-METHOD rethrowSourceFailure(Ljava/lang/Throwable;)V access=10 signature=<E:Ljava/lang/Throwable;>(Ljava/lang/Throwable;)V^TE; exceptions=[java/lang/Throwable]
-ANNOTATIONS ||||pvis=|pinv=|lvtvis=|lvtinv=
-DEFAULT
-ALOAD 0
-ATHROW
 METHOD returnToNatureFixturePercentileRoll()I access=1 signature=null exceptions=[]
 ANNOTATIONS ||||pvis=|pinv=|lvtvis=|lvtinv=
```

</details>

<details>
<summary>SourceLogin</summary>

```diff
--- staged/SourceLogin
+++ formal/SourceLogin
@@ -176,10 +176,10 @@
 LABEL L3
 ARETURN
-METHOD lambda$start$#0(Ljava/net/ServerSocket;Llocal/lito381/server/GameHub;Ljava/util/concurrent/ExecutorService;Llocal/lito381/server/ClientSession$Endpoint;Llocal/lito381/server/ServerConfig;Llocal/lito381/server/AccountRepository;Llocal/lito381/server/GameStateStore;Llocal/lito381/server/CaptureLog;)V access=4106 signature=null exceptions=[]
+METHOD lambda$start$#0(Ljava/net/ServerSocket;Ljava/util/concurrent/ExecutorService;Llocal/lito381/server/ClientSession$Endpoint;Llocal/lito381/server/ServerConfig;Llocal/lito381/server/AccountRepository;Llocal/lito381/server/GameStateStore;Llocal/lito381/server/CaptureLog;Llocal/lito381/server/GameHub;)V access=4106 signature=null exceptions=[]
 ANNOTATIONS ||||pvis=|pinv=|lvtvis=|lvtinv=
 DEFAULT
 TRY L1 L2 L3 java/lang/Exception
 TRY L0 L5 L6 java/lang/Exception
-TRY L0 L8 L9 java/lang/Throwable
+TRY L0 L8 L8 java/lang/Throwable
 LABEL L0
 ALOAD 0
@@ -187,12 +187,13 @@
 IFNE L5
 ALOAD 0
-ALOAD 1
+ALOAD 7
 INVOKESTATIC local/lito381/server/NativeServerLifetime.acceptSocket(Ljava/net/ServerSocket;Llocal/lito381/server/GameHub;)Ljava/net/Socket;
 ASTORE 8
 LABEL L1
-ALOAD 2
+ALOAD 1
 NEW local/lito381/server/ClientSession
 DUP
 ALOAD 8
+ALOAD 2
 ALOAD 3
 ALOAD 4
@@ -200,5 +201,4 @@
 ALOAD 6
 ALOAD 7
-ALOAD 1
 INVOKESPECIAL local/lito381/server/ClientSession.<init>(Ljava/net/Socket;Llocal/lito381/server/ClientSession$Endpoint;Llocal/lito381/server/ServerConfig;Llocal/lito381/server/AccountRepository;Llocal/lito381/server/GameStateStore;Llocal/lito381/server/CaptureLog;Llocal/lito381/server/GameHub;)V
 INVOKESTATIC local/lito381/server/NativeServerLifetime.submitAccepted(Ljava/util/concurrent/ExecutorService;Ljava/lang/Runnable;)Ljava/util/concurrent/Future;
@@ -229,10 +229,9 @@
 LABEL L7
 INVOKESTATIC local/lito381/server/NativeServerLifetime.abortAccept()V
+RETURN
 LABEL L8
-RETURN
-LABEL L9
-ASTORE 8
+ASTORE 10
 INVOKESTATIC local/lito381/server/NativeServerLifetime.abortAccept()V
-ALOAD 8
+ALOAD 10
 ATHROW
 METHOD repository(Llocal/lito381/server/AccountRepository;Ljava/net/Socket;)Llocal/lito381/server/AccountRepository; access=8 signature=null exceptions=[]
@@ -572,5 +571,4 @@
 ALOAD 8
 ALOAD 18
-ALOAD 4
 ALOAD 5
 ALOAD 17
@@ -579,5 +577,6 @@
 ALOAD 2
 ALOAD 3
-INVOKEDYNAMIC run(Ljava/net/ServerSocket;Llocal/lito381/server/GameHub;Ljava/util/concurrent/ExecutorService;Llocal/lito381/server/ClientSession$Endpoint;Llocal/lito381/server/ServerConfig;Llocal/lito381/server/AccountRepository;Llocal/lito381/server/GameStateStore;Llocal/lito381/server/CaptureLog;)Ljava/lang/Runnable; bsm=H6 java/lang/invoke/LambdaMetafactory.metafactory(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodHandle;Ljava/lang/invoke/MethodType;)Ljava/lang/invoke/CallSite; args=[type:()V, handle:H6 local/lito381/server/SourceLogin.lambda$start$#0(Ljava/net/ServerSocket;Llocal/lito381/server/GameHub;Ljava/util/concurrent/ExecutorService;Llocal/lito381/server/ClientSession$Endpoint;Llocal/lito381/server/ServerConfig;Llocal/lito381/server/AccountRepository;Llocal/lito381/server/GameStateStore;Llocal/lito381/server/CaptureLog;)V, type:()V]
+ALOAD 4
+INVOKEDYNAMIC run(Ljava/net/ServerSocket;Ljava/util/concurrent/ExecutorService;Llocal/lito381/server/ClientSession$Endpoint;Llocal/lito381/server/ServerConfig;Llocal/lito381/server/AccountRepository;Llocal/lito381/server/GameStateStore;Llocal/lito381/server/CaptureLog;Llocal/lito381/server/GameHub;)Ljava/lang/Runnable; bsm=H6 java/lang/invoke/LambdaMetafactory.metafactory(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodHandle;Ljava/lang/invoke/MethodType;)Ljava/lang/invoke/CallSite; args=[type:()V, handle:H6 local/lito381/server/SourceLogin.lambda$start$#0(Ljava/net/ServerSocket;Ljava/util/concurrent/ExecutorService;Llocal/lito381/server/ClientSession$Endpoint;Llocal/lito381/server/ServerConfig;Llocal/lito381/server/AccountRepository;Llocal/lito381/server/GameStateStore;Llocal/lito381/server/CaptureLog;Llocal/lito381/server/GameHub;)V, type:()V]
 INVOKEINTERFACE java/util/List.add(Ljava/lang/Object;)Z itf
 POP
```

</details>

## 附錄：完整重現腳本

<details>
<summary>Python：精選、清理、編譯、集合／SHA 核對、原工具比較及完整差異</summary>

<!-- A06-REPRODUCER -->
```python
import difflib, hashlib, json, pathlib, shutil, subprocess, sys
ROOT = pathlib.Path.cwd().resolve()
manifest = json.loads((ROOT / "docs/combat-merge/a-batches.json").read_text("utf-8"))
batch = next(b for b in manifest["batches"] if b["id"] == "A06")
formal = ROOT / manifest["generator"]["formal"]
server = formal.parent.parent
staged = server / "merge-inputs/formal-0.6.71/staged/local/lito381/server"
target = server / "target/a06"
evidence = ROOT / "scratch/a06"
jdk = pathlib.Path("C:/Program Files/Microsoft/jdk-17.0.17.10-hotspot/bin")
cache = pathlib.Path("D:/dev-cache/maven/repository/org/ow2/asm")
evidence.mkdir(parents=True, exist_ok=True)
def sha(path): return hashlib.sha256(path.read_bytes()).hexdigest()
def run(args, logfile):
    p = subprocess.run([str(a) for a in args], stdout=subprocess.PIPE, stderr=subprocess.STDOUT)
    text = p.stdout.decode("utf-8", errors="replace").replace("\r\n", "\n")
    (evidence / logfile).write_text(text, "utf-8", newline="\n")
    if p.returncode: raise RuntimeError(f"{logfile}: exit {p.returncode}\n{text}")
    return text
assert sha(formal) == manifest["generator"]["formal_sha256"]
assert len(batch["classes"]) == 27 and len(set(batch["classes"])) == 27
assert len(batch["verify_entries"]) == 69 and len(set(batch["verify_entries"])) == 69
# Only clean this task's build output; never move/delete any original-project path.
assert target.resolve() == server.resolve() / "target/a06"
assert target.resolve().is_relative_to(ROOT)
if target.exists(): shutil.rmtree(target)
for directory in ["classes", "tool", "test", "dependency"]:
    (target / directory).mkdir(parents=True)
expected_asm = {
    "asm": "adf46d5e34940bdf148ecdd26a9ee8eea94496a72034ff7141066b3eea5c4e9d",
    "asm-tree": "62f4b3bc436045c1acb5c3ba2d8ec556ec3369093d7f5d06c747eb04b56d52b1",
}
for artifact, expected in expected_asm.items():
    source = cache / artifact / "9.7" / f"{artifact}-9.7.jar"
    assert sha(source) == expected
    shutil.copyfile(source, target / "dependency" / source.name)
cp = str(target / "dependency/*")
files = [staged / (name + ".java") for name in batch["classes"]]
assert all(f.is_file() for f in files)
(evidence / "sources.txt").write_text("\n".join(str(f.relative_to(ROOT)) for f in files) + "\n", "utf-8")
selection = evidence / "verify_entries.txt"
selection.write_text("\n".join(batch["verify_entries"]) + "\n", "utf-8")
run([jdk/"javac.exe", "-version"], "jdk.log")
run([jdk/"javac.exe", "-J-Dfile.encoding=UTF-8", "--release", "17", "-encoding", "UTF-8",
     "-cp", str(formal) + ";" + str(server/"lib/*"), "-d", target/"classes", *files], "javac.log")
# Pack ONLY the javac output; no formal class bytes are copied into left.jar.
run([jdk/"jar.exe", "--create", "--file", target/"left.jar", "--no-manifest", "--date", "2026-11-05T00:00:00Z", "-C", target/"classes", "."], "jar.log")
import zipfile
with zipfile.ZipFile(target/"left.jar") as left, zipfile.ZipFile(formal) as right:
    actual = {name for name in left.namelist() if name.endswith(".class")}
    expected = set(batch["verify_entries"])
    selection_receipt = {"count":len(actual),"missingLeft":sorted(expected-actual),"extraLeft":sorted(actual-expected)}
    assert actual == expected
    hashes = {name:{"left":hashlib.sha256(left.read(name)).hexdigest(),
                   "right":hashlib.sha256(right.read(name)).hexdigest()} for name in sorted(expected)}
(evidence/"selection.json").write_text(json.dumps(selection_receipt,indent=2),"utf-8")
(evidence/"hashes.json").write_text(json.dumps(hashes,indent=2),"utf-8")
source_hashes = {str(f.relative_to(ROOT)):sha(f) for f in files}
(evidence/"source-hashes.json").write_text(json.dumps(source_hashes,ensure_ascii=False,indent=2),"utf-8")
run([jdk/"javac.exe","-J-Dfile.encoding=UTF-8","--release","17","-encoding","UTF-8","-cp",cp,
     "-d",target/"tool",*sorted((server/"tools/semantic-diff/src").rglob("*.java"))],"tool-build.log")
toolcp = str(target/"tool") + ";" + cp
run([jdk/"java.exe","-Dfile.encoding=UTF-8","-cp",toolcp,"local.lito381.semanticdiff.SemanticDiff",
     "--left",target/"left.jar","--right",formal,"--classes",selection,
     "--json",evidence/"result.json","--md",evidence/"result.md"],"semantic.log")
result = json.loads((evidence/"result.json").read_text("utf-8"))
assert not result["classSelection"]["missingLeft"] and not result["classSelection"]["extraLeft"]
assert result["counts"]["BYTE_SAME"] == sum(h["left"] == h["right"] for h in hashes.values())

# Read-only diagnostics using the unchanged comparator; never writes class bytes.
DUMP_SOURCE = r"""
package local.lito381.semanticdiff;
import java.nio.file.*;import java.util.*;import java.lang.reflect.*;import org.objectweb.asm.tree.*;
public class Dump {
 static Object call(String name,Class<?>[] types,Object... args)throws Exception{var m=ClassComparer.class.getDeclaredMethod(name,types);m.setAccessible(true);return m.invoke(null,args);}
 public static void main(String[] args)throws Exception{
 var jar=JarIndex.load(Path.of(args[0]));var out=Path.of(args[2]);Files.createDirectories(out);
 for(String entry:Files.readAllLines(Path.of(args[1]))){String name=entry.replace(".class","");var c=jar.readTree(name);var lines=new ArrayList<String>();
 lines.add("CLASS "+c.name+" access="+c.access+" super="+c.superName+" signature="+c.signature+" interfaces="+new TreeSet<>(c.interfaces));
 lines.add("NEST "+c.nestHostClass+" "+(c.nestMembers==null?List.of():new TreeSet<>(c.nestMembers))+" OUTER "+c.outerClass+" "+c.outerMethod+" "+c.outerMethodDesc);
 lines.add("PERMITTED "+(c.permittedSubclasses==null?List.of():new TreeSet<>(c.permittedSubclasses)));
 lines.add("ANNOTATIONS "+call("annotations",new Class[]{ClassNode.class},c));
 lines.add("INNER "+call("innerClasses",new Class[]{ClassNode.class},c));
 lines.add("RECORD "+call("records",new Class[]{ClassNode.class,JarIndex.class},c,jar));
 var fields=new TreeMap<String,FieldNode>();for(var f:c.fields)fields.put(f.name+":"+f.desc,f);
 for(var f:fields.values())lines.add("FIELD "+f.name+":"+f.desc+" "+f.access+" "+f.signature+" "+f.value+" annotations="+call("fieldAnns",new Class[]{FieldNode.class},f));
 var methods=new TreeMap<String,MethodNode>();for(var m:c.methods)methods.put(jar.remapMethod(c.name,m.name)+m.desc,m);
 for(var e:methods.entrySet()){var m=e.getValue();lines.add("METHOD "+e.getKey()+" access="+m.access+" signature="+m.signature+" exceptions="+new TreeSet<>(m.exceptions));
 lines.add("ANNOTATIONS "+call("methodAnns",new Class[]{MethodNode.class,Map.class,JarIndex.class},m,call("labelIds",new Class[]{MethodNode.class},m),jar));
 lines.add("DEFAULT "+call("defaultValue",new Class[]{Object.class,JarIndex.class},m.annotationDefault,jar));lines.addAll((List<String>)call("tryCatch",new Class[]{MethodNode.class,JarIndex.class},m,jar));lines.addAll(ClassComparer.instructions(m,jar));}
 Files.write(out.resolve(name.substring(name.lastIndexOf('/')+1)+".txt"),lines);
 }
 }
}
"""
EXCEPTION_SOURCE = r"""
import java.io.IOException;
import java.sql.SQLException;
import java.lang.reflect.InvocationTargetException;
import java.nio.file.Path;
public class ExceptionIdentityCheck {
 public static void main(String[] args) throws Exception {
  Class<?> config=Class.forName("local.lito381.server.ServerConfig");
  var rethrow=config.getDeclaredMethod("rethrowSourceFailure",Throwable.class);
  rethrow.setAccessible(true);
  Throwable[] failures={new IOException("io"),new SQLException("sql"),new IllegalArgumentException("runtime"),new AssertionError("error")};
  for(Throwable failure:failures){
   try{rethrow.invoke(null,failure);throw new AssertionError("returned");}
   catch(InvocationTargetException result){if(result.getCause()!=failure)throw new AssertionError("identity changed");}
  }
  for(var method:config.getDeclaredMethods())if(method.getName().equals("load")){
   if(!java.util.Arrays.equals(method.getExceptionTypes(),new Class<?>[]{IOException.class}))throw new AssertionError("load API changed");
  }
  System.out.println("PASS: 4 exception identities preserved; load IOException API unchanged; no IO/DB executed");
 }
}
"""
(evidence/"Dump.java").write_text(DUMP_SOURCE,"utf-8",newline="\n")
(evidence/"ExceptionIdentityCheck.java").write_text(EXCEPTION_SOURCE,"utf-8",newline="\n")
run([jdk/"javac.exe","--release","17","-encoding","UTF-8","-cp",toolcp,"-d",target/"tool",evidence/"Dump.java"],"dump-build.log")
for side, jar in [("left",target/"left.jar"),("right",formal)]:
    run([jdk/"java.exe","-Dfile.encoding=UTF-8","-cp",toolcp,"local.lito381.semanticdiff.Dump",jar,selection,evidence/side],side+"-dump.log")
for name in batch["verify_entries"]:
    simple = name.split("/")[-1].replace(".class", "")
    text = "".join(difflib.unified_diff((evidence/"left"/(simple+".txt")).read_text("utf-8").splitlines(True),
        (evidence/"right"/(simple+".txt")).read_text("utf-8").splitlines(True),
        fromfile="staged/"+simple,tofile="formal/"+simple,n=2))
    (evidence/(simple+".diff")).write_text(text,"utf-8",newline="\n")
run([jdk/"javac.exe","--release","17","-encoding","UTF-8","-d",target/"test",evidence/"ExceptionIdentityCheck.java"],"exception-build.log")
run([jdk/"java.exe","-Dfile.encoding=UTF-8","-cp",str(target/"test")+";"+str(target/"classes")+";"+str(formal),"ExceptionIdentityCheck"],"exception-check.log")
receipt={"jdk":(evidence/"jdk.log").read_text().strip(),"formalSHA256":sha(formal),"leftJarSHA256":sha(target/"left.jar"),
    "selection":selection_receipt,"counts":result["counts"],"sourceFileCount":len(files)}
(evidence/"receipt.json").write_text(json.dumps(receipt,indent=2),"utf-8")
print(json.dumps(receipt,indent=2))
# The CLI returning 0 only means a report was generated. This is the actual gate.
passed=result["counts"]["SAME"]==69 and all(result["counts"][k]==0 for k in ["DIFF","ONLY_LEFT","ONLY_RIGHT"])
print("A06_ACCEPTANCE="+("PASS" if passed else "FAIL; needs:claude; do not merge"))
sys.exit(0 if passed else 2)
```

</details>
