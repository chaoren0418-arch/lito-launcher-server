# Issue #77 — A06 依賴合併後重驗與剩餘阻擋

**尚未達成驗收：67 SAME／1 SAME_EFFECTS／1 DIFF，請由 Claude 主控處理剩餘 ServerConfig 比較規則；不得以本報告宣告 A06 完成。**

2026-11-05，在既有 `task/77`、起點 `b6d3f919184c585256963bc0e43b2fbc5be1c3a9` 上修正。27 個 staged Java 直接經 javac 編譯，精確產出 69 entries，missingLeft／extraLeft 均空。3 個 BYTE_SAME 為自然編譯結果，未複製正式 class、未做 ASM 後處理、未加入私人 stub。

## 做了什麼

本輪只修改 A06 的 `SourceLogin.java` 與本報告，另外 26 個 staged 檔沿用前次提交並全部重編。整個 PR 仍為 manifest 指定的 27 個 Java 加本報告。未改 src/main、pom.xml、docs/REPORT.md、比對器或核准清單；未切換／新建分支。

`SourceLogin` 在 `acceptSocket` 首次引用 hub 之前加入編譯期常數 false 分支，按 clients、endpoint、config、accounts、state、capture 的順序引用變數。javac 的 lambda capture 掃描保留此順序，但不產生該分支的陣列或其他執行指令。這是為重現正式 ASM 補丁保留的 capture 描述子；不是執行期功能。直接查驗左右 JAR 的原始描述子相同，且停用 #166 的 captures 正規化後，SourceLogin 仍為 SAME。既有 Throwable／RETURN 結構通過已合併的邊界及槽位正規化，沒有為 SourceLogin 加入 effects／layout／deviation 例外。

來源規則沿用已修正的 `b6d3f91`：NativeNpcPhysicalSource、NativePetWorldLifetime、NativeRoomRules、ResourceDatabase、RuntimeInventory、ServerConfig、SourceLogin 以 src/main 原始碼為底移植；19 個 DECOMPILED 類保留指定標頭；SourceEquipmentPhysicalStats 從 Accounts.java 的原始 record 定義抽出。本輪已補齊最後一項 Maven SourceFile 核證，詳見下節。

## 已驗證

### 依賴及環境

比對工具與共用 stub 固定取自已合併提交 `450178d59b0157fddf2b9456b7c03d20207993fc`。本 task 分支較舊，因此用 git archive 將該版本的工具、compile-stubs、核准檔解至本 worktree 的 scratch；沒有將其他任務的檔案變更混入本 PR。

| 依賴 | 已合併證據 |
|---|---|
| #108 | PR #147，merge `a5f9f66` |
| #132 | PR #142，merge `e76e1fe` |
| #135 | PR #152，merge `4a8d7e5` |
| #153 | PR #164，merge `bf50dd7` |
| #166 | PR #170，merge `5089b6b` |

Microsoft JDK 17.0.17；javac 使用 `--release 17 -encoding UTF-8`。#108 原版 build.ps1 成功編譯 9 個 compile-only Java，放在隔離副本的 target/legacy-stubs，只加入編譯 classpath，不包進 A06 JAR。

每次從 manifest 精選 27 檔，清除本工作目錄專用 classes 下既有 class，再 javac 與固定時間 jar 封裝。未使用 staged 全目錄萬用字元。semantic-diff 使用完整 69 行清單、正式 JAR 作左側依賴，並將共用核准檔精確裁切為 A06 的 5 個 effects 方法與 1 個 extra helper。

工具正常產報告會退出 0，即使仍有 DIFF。本報告重驗腳本另外以所有 69 個 status 必須為 SAME／SAME_EFFECTS 作閘門，目前 Python **退出 2／A06_ACCEPTANCE=FAIL**。

正式 JAR SHA-256：`e1498871c7be6b287821c57ef5d95933db1740d9b4d4a80f6c3465f21d99d498`。

本輪 staged JAR SHA-256：`2fb2717cbad047df6ff642790f3dfb1cd683324ccaf1e31c6de5877ce2af5b43`。

```text
Note: Some input files use unchecked or unsafe operations.

Note: Recompile with -Xlint:unchecked for details.
Compiled 9 compile-only sources -> target/legacy-stubs (never packaged)
PASS: raw lambda descriptors equal; no array allocation emitted; SAME with captures normalization disabled
descriptor: (Ljava/net/ServerSocket;Ljava/util/concurrent/ExecutorService;Llocal/lito381/server/ClientSession$Endpoint;Llocal/lito381/server/ServerConfig;Llocal/lito381/server/AccountRepository;Llocal/lito381/server/GameStateStore;Llocal/lito381/server/CaptureLog;Llocal/lito381/server/GameHub;)V
PASS: 4 exception identities preserved; load IOException API unchanged; no IO/DB executed
```

### 核准方法的精確範圍

依[主控裁定](https://github.com/chaoren0418-arch/lito-launcher-server/issues/77#issuecomment-6641248257)及已合併清單，RuntimeInventory 為 **3 個方法、4 處 return**，withoutObjects 有兩處，沒有第四個方法。

```text
local/lito381/server/RuntimeInventory#copy()Llocal/lito381/server/RuntimeInventory; # approved: https://github.com/chaoren0418-arch/lito-launcher-server/issues/77#issuecomment-6641248257
local/lito381/server/RuntimeInventory#sourceEquipmentOnly()Llocal/lito381/server/RuntimeInventory; # approved: https://github.com/chaoren0418-arch/lito-launcher-server/issues/77#issuecomment-6641248257
local/lito381/server/RuntimeInventory#withoutObjects(Ljava/util/Set;)Llocal/lito381/server/RuntimeInventory; # approved: https://github.com/chaoren0418-arch/lito-launcher-server/issues/77#issuecomment-6641248257
local/lito381/server/ServerConfig#applyExternalSourceServers(Ljava/util/Properties;Ljava/nio/file/Path;)V # approved: https://github.com/chaoren0418-arch/lito-launcher-server/issues/77#issuecomment-6641248257
local/lito381/server/ServerConfig#load(Ljava/nio/file/Path;Ljava/lang/String;)Llocal/lito381/server/ServerConfig; # approved: https://github.com/chaoren0418-arch/lito-launcher-server/issues/77#issuecomment-6641248257
local/lito381/server/ServerConfig#rethrowSourceFailure(Ljava/lang/Throwable;)V # approved: https://github.com/chaoren0418-arch/lito-launcher-server/issues/77#issuecomment-6641248257
```

RuntimeInventory 三個方法均 SAME_EFFECTS；ServerConfig.load 為 SAME_EFFECTS；applyExternalSourceServers 仍 DIFF。helper 的額外方法核准已生效，但它的呼叫點不因此被忽略。沒有擴張核准範圍。

### SourceEquipmentPhysicalStats 的來源證據

唯讀檢查已完成 #134 的 Maven 產物（沒有重跑其建置）。其 package 日誌記錄 Maven 3.9.16／JDK 17.0.17，`BUILD SUCCESS`，完成於 2026-11-05T08:06:38+08:00。實際 JAR 內 pom.properties 為 local.lito381:lito381-server:0.6.71；JAR 的該 class bytes 與 target/classes 完全相同。Accounts.java 與本 task 的 src/main 相同，也與 #134 HEAD 的 git blob（統一 CRLF 後）相同。

```json
{
  "worktree": "D:\\lineage開發\\3-lito開發\\scratch\\wt\\task-134",
  "head": "e560c7c3c73bd57516d3e7f08efc3821f727760a",
  "artifact": "D:\\lineage開發\\3-lito開發\\scratch\\wt\\task-134\\lito伺服器\\LiTo381-Server\\target\\LiTo381-Server.jar",
  "artifactSHA256": "0f030726ef0c0bda675501a449f06071baf1095453cf23eaeb91155e29b84dcf",
  "classSHA256": "07b8506e34576801d22e7ab35a10d5bd3079c6e9cd3d7e473282c28b02857749",
  "AccountsSHA256": "25454eea727e3bcdb97324d009e3dee09ee7bfba67e56592787ac657faacfb5f",
  "metadata": "artifactId=lito381-server\r\ngroupId=local.lito381\r\nversion=0.6.71\r\n",
  "sourceFile": "Accounts.java",
  "jarEntryEqualsTargetClass": true,
  "sourceEqualsTask77": true,
  "mavenLogSHA256": "96d786daf03d28c6085789ccf2453142c590a9c6c90834ef3a982baea2e747f8"
}
```

```powershell
& 'C:/Program Files/Microsoft/jdk-17.0.17.10-hotspot/bin/javap.exe' -J-Dfile.encoding=UTF-8 -v -classpath 'D:/lineage開發/3-lito開發/scratch/wt/task-134/lito伺服器/LiTo381-Server/target/LiTo381-Server.jar' local.lito381.server.SourceEquipmentPhysicalStats
```

```text
  Compiled from "Accounts.java"
  SHA-256 checksum 07b8506e34576801d22e7ab35a10d5bd3079c6e9cd3d7e473282c28b02857749
  #321 = Utf8               SourceFile
SourceFile: "Accounts.java"
```

### 唯一剩餘 DIFF：ServerConfig.applyExternalSourceServers

針對 ServerConfig 的 263 個方法逐一呼叫未修改的 compareMethod：261 個共同方法通過（含 load 的核准效果比較），僅 applyExternalSourceServers 失敗，另 1 個為核准的左側 helper。以下為**完整效果摘要差異**，包含 sequence、常數、catch 型別及分支數；不是只抄 firstDiff。方法旗標、泛型 signature、throws metadata 相同，常數摘要相同。

```diff
--- staged
+++ formal
@@ -1,8 +1,6 @@
 METHOD applyExternalSourceServers(Ljava/util/Properties;Ljava/nio/file/Path;)V access=10 signature=null exceptions=[java/io/IOException]
 INVOKESTATIC local/lito381/server/SharedDatabase.applySources(Ljava/util/Properties;Ljava/nio/file/Path;)Z
 RETURN
-ASTORE
-INVOKESTATIC local/lito381/server/ServerConfig.rethrowSourceFailure(Ljava/lang/Throwable;)V
 INVOKESTATIC local/lito381/server/ServerConfig.optional(Ljava/util/Properties;Ljava/lang/String;)Ljava/lang/String;
 ASTORE
 INVOKEVIRTUAL java/lang/String.isEmpty()Z
@@ -77,5 +75,5 @@
 INVOKEVIRTUAL java/util/Properties.setProperty(Ljava/lang/String;Ljava/lang/String;)Ljava/lang/Object;
 RETURN
 CONSTANTS {IINC Integer:1=1, Integer:0=4, Integer:1=3, Integer:16=1, str:","=5, str:"100.0"=1, str:"database.dynamic.databases"=1, str:"database.source.resource.databases"=1, str:"database.source.weight.rates"=1, str:"every external source must define resource.database when one does"=1, str:"login.classic.server.ids"=1, str:"login.classic.server.names"=1, str:"source.count"=1, str:"source.servers.file"=1}
-CATCH_TYPES [java/lang/Exception  , java/lang/Throwable  , java/lang/Throwable  ]
-BRANCHES 15
+CATCH_TYPES [java/lang/Throwable  , java/lang/Throwable  ]
+BRANCHES 14
```

原因：正式 applyExternalSourceServers 宣告 throws IOException，卻由 ASM 直接呼叫宣告 throws Exception 的 SharedDatabase.applySources。現有 Java 用已核准 helper 原樣轉拋，因而多了 ASTORE、helper 呼叫、Exception handler 與 1 個 GOTO。#135 的 extra-methods 只豁免 helper 的存在，不會內聯 helper 或消去 catch；#153／#166 亦未使這個效果摘要一致。

已在 scratch 的 ServerConfig 複本試驗只移除 catch，保留正式 JAR classpath、既有 IOException 宣告；javac 失敗如下。此試驗未替換 staged 或正式 class。

```text
D:\lineage開發\3-lito開發\scratch\wt\task-77\scratch\a06-current\direct-call\ServerConfig.java:911: error: unreported exception Exception; must be caught or declared to be thrown

        if (SharedDatabase.applySources(target, base)) return;

                                       ^

1 error
```

另外重新執行隔離反射檢查，IOException、SQLException、IllegalArgumentException、AssertionError 四個物件皆由 helper 原樣拋出，load 的 IOException 宣告不變。這只驗證 helper，沒有執行 applySources／連線資料庫，不能替代整個方法的行為驗收。

**推測／待裁定**：主控可以要求比較工具針對精確核准的 catch→原樣 rethrow helper 形狀增加有反例保護的證明，或指定另一種合法 Java 寫法。單純再把方法加入 effects 清單無效，因為它已在清單中。直接放寬到忽略 handler／呼叫、修改 reference 的 checked-exception 宣告或私人 stub 都不在本任務授權範圍，本輪未做。此處不主張所有可能的 Java 寫法都已被證明不可行。

## 未驗證什麼／給下一棒的提示

1. **Claude 主控接手**：處理唯一剩餘的 ServerConfig 比較阻擋，維持 needs:claude；提供精確修正或開比較器後續任務，不能以 68／69 當驗收通過。依賴 [108,132,135,153,166] 已合併，不再以等待舊依賴作阻擋。
2. **下一位本機執行器**：使用本報告腳本重驗，保持 69 entry 集合完全一致；確認唯一差異消失後再宣告 A06 完成。不修改 src/main、核准檔或比較器來湊通過。
3. **A-land 接手整合驗證**：本輪未做 Maven 單元／回歸、伺服器啟動、登入器、遊戲或 DB 測試；符合本批不動 src/main 的規則。SourceFile 核證是讀取既有 Maven 產物，不是本輪重跑 Maven。

## 產出路徑與重現

- 提交：`lito伺服器/LiTo381-Server/merge-inputs/formal-0.6.71/staged/local/lito381/server/SourceLogin.java`、`docs/reports/issue-77.md`。其餘本批 staged Java 沿用 PR 既有內容。
- 不提交：`scratch/a06-current/` 下腳本、固定依賴副本、清單、逐項 JSON、javap、效果摘要、直接呼叫的失敗編譯探針及來源收據。
- 不提交：本 worktree `lito伺服器/LiTo381-Server/target/a06-current/` 的 class／JAR／ASM 依賴；共用 stub 的建置輸出在 `scratch/a06-current/dependencies/lito伺服器/LiTo381-Server/target/legacy-stubs/`。

以下從報告本身提取兩份完整腳本即可重現核心 69 entry 驗收、停用 captures 對照、描述子與完整效果差異；不依賴上輪 scratch。工具原始碼由固定 git 提交重新解出，不從本 task 的舊工具編譯。ASM 9.7 三個依賴使用 Maven 快取或下載，均核對固定 SHA。需要 git、PowerShell、Microsoft JDK 17 與指定 Python 可用；所有寫入限本 worktree。

```powershell
$ErrorActionPreference = 'Stop'
$root = (Get-Location).Path
$report = Get-Content -Raw -Encoding UTF8 docs/reports/issue-77.md
New-Item -ItemType Directory -Force scratch/a06-current | Out-Null
foreach ($spec in @(@('A06-VERIFY','python','verify.py'), @('A06-DIAGNOSTIC','java','EffectsDiagnostic.java'))) {
    $pattern = '(?s)<!-- ' + $spec[0] + ' -->\s*```' + $spec[1] + '\r?\n(.*?)\r?\n```'
    $match = [regex]::Match($report, $pattern)
    if (-not $match.Success) { throw ('Missing embedded script: ' + $spec[0]) }
    [IO.File]::WriteAllText((Join-Path $root ('scratch/a06-current/' + $spec[2])), $match.Groups[1].Value, [Text.UTF8Encoding]::new($false))
}
& 'D:/dev-orchestrator/tools/python/python.exe' -X utf8 scratch/a06-current/verify.py
$verificationExit = $LASTEXITCODE
Write-Output "Python acceptance exit=$verificationExit"
if ($verificationExit -ne 0) { throw 'A06 acceptance failed; inspect scratch/a06-current/result.json and effects.diff' }
```

## 完整 69 entry 結果

| Entry（省略 local/lito381/server/） | 結果 | BYTE_SAME |
|---|---|---|
| `NativeNpcPhysicalSource` | SAME | false |
| `NativeNpcPhysicalSource$Snapshot` | SAME | false |
| `NativePcActionAuthority` | SAME | false |
| `NativePetWorldLifetime` | SAME | false |
| `NativePetWorldLifetime$Target` | SAME | false |
| `NativePlayerVitals` | SAME | false |
| `NativePlayerVitals$Result` | SAME | false |
| `NativeProcEncounterCatalog` | SAME | false |
| `NativeProcEncounterCatalog$Catalog` | SAME | false |
| `NativeProcEncounterCatalog$Npc` | SAME | false |
| `NativeProcEncounterCatalog$Receiver` | SAME | false |
| `NativeProcEncounterCatalog$Type` | SAME | false |
| `NativeProcEncounterPublication` | SAME | false |
| `NativeProcEncounterPublication$Context` | SAME | false |
| `NativeProcEncounterPublication$Stale` | SAME | false |
| `NativeProcEncounterPublication$Viewer` | SAME | false |
| `NativeProcHealBonus` | SAME | false |
| `NativeProcHealBonus$Catalog` | SAME | false |
| `NativeProcHealingWorld` | SAME | false |
| `NativeProcHealingWorld$Plan` | SAME | false |
| `NativeProcHealingWorld$Target` | SAME | false |
| `NativeProcNpcCatalog` | SAME | false |
| `NativeProcNpcCatalog$1` | SAME | false |
| `NativeProcNpcCatalog$Catalog` | SAME | false |
| `NativeProcNpcCatalog$Npc` | SAME | false |
| `NativeProcNpcState` | SAME | false |
| `NativeProcPlayerState` | SAME | false |
| `NativeProcPlayerState$Identity` | SAME | false |
| `NativeReuseLog` | SAME | false |
| `NativeReuseLog$Log` | SAME | false |
| `NativeReuseLog$Logger` | SAME | false |
| `NativeRoomRules` | SAME | false |
| `NativeRoomRules$Policy` | SAME | true |
| `NativeServerLifetime` | SAME | false |
| `NativeSkillConsumptionBridges` | SAME | false |
| `NativeSourceActionIntervalCalculator` | SAME | false |
| `NativeSourceActionIntervalCalculator$ActionType` | SAME | false |
| `NativeSourceActionIntervalCalculator$SpeedState` | SAME | false |
| `NativeSourceClientActionTimeline` | SAME | false |
| `NativeSourceClientActionTimeline$ActionBlockSnapshot` | SAME | false |
| `NativeSourceClientActionTimeline$ActionKind` | SAME | false |
| `NativeSourceClientActionTimeline$CombatDeferral` | SAME | false |
| `NativeSourceClientActionTimeline$Deadline` | SAME | false |
| `NativeSourceClientActionTimeline$Reservation` | SAME | false |
| `NativeSourceClientActionTimeline$VisibleWindow` | SAME | false |
| `NativeSourceMaterialPlan` | SAME | false |
| `NativeSourceMaterialPlan$ConsumeRequirement` | SAME | false |
| `NativeSourceMaterialPlan$Debit` | SAME | false |
| `NativeSourceMaterialPlan$ItemConsumeCommitResult` | SAME | false |
| `NativeSourceMaterialPlan$PlannedConsumption` | SAME | false |
| `NativeSourceMaterialPlan$Result` | SAME | false |
| `NativeSourceSkillCastAdmission` | SAME | false |
| `NativeSourceSkillCastAdmission$Reservation` | SAME | false |
| `NativeSourceSkillCastAdmission$Result` | SAME | false |
| `NativeSourceSkillConsumptionMath` | SAME | false |
| `NativeSourceSkillDelay` | SAME | false |
| `NativeSourceSkillDelay$Lease` | SAME | false |
| `ResourceDatabase` | SAME | false |
| `ResourceDatabase$Key` | SAME | true |
| `ResourceDatabase$Snapshot` | SAME | true |
| `RuntimeInventory` | SAME_EFFECTS | false |
| `RuntimeInventory$Item` | SAME | false |
| `RuntimeInventory$NativeArmorBonuses` | SAME | false |
| `RuntimeInventory$PersistentItem` | SAME | false |
| `ServerConfig` | DIFF | false |
| `SharedClientResources` | SAME | false |
| `SourceEquipmentPhysicalStats` | SAME | false |
| `SourceLogin` | SAME | false |
| `SourceLogin$Binding` | SAME | false |

<details>
<summary>工具原始 JSON（含精確核准清單及逐方法結果）</summary>

```json
{
  "tool": "semantic-diff",
  "generatedAt": "2026-11-05T12:32:22Z",
  "left": "D:\\lineage開發\\3-lito開發\\scratch\\wt\\task-77\\lito伺服器\\LiTo381-Server\\target\\a06-current\\left.jar",
  "right": "D:\\lineage開發\\3-lito開發\\scratch\\wt\\task-77\\lito伺服器\\LiTo381-Server\\reference\\base-0.6.71.jar",
  "leftClasspath": [
    "D:\\lineage開發\\3-lito開發\\scratch\\wt\\task-77\\lito伺服器\\LiTo381-Server\\reference\\base-0.6.71.jar"
  ],
  "rightClasspath": [],
  "allowRight": null,
  "effectsMethodsFile": "D:\\lineage開發\\3-lito開發\\scratch\\wt\\task-77\\scratch\\a06-current\\effects-approved.txt",
  "extraMethodsFile": "D:\\lineage開發\\3-lito開發\\scratch\\wt\\task-77\\scratch\\a06-current\\extra-methods-approved.txt",
  "metadataExemptFile": null,
  "layoutMethods": [],
  "accessWiden": [],
  "ctorPrologueOrder": [],
  "deviations": [],
  "extraMethods": [
    "local/lito381/server/ServerConfig#rethrowSourceFailure(Ljava/lang/Throwable;)V"
  ],
  "metadataExempt": [],
  "leftClassCount": 69,
  "rightClassCount": 2554,
  "comparedClassCount": 69,
  "classSelection": {
    "missingLeft": [],
    "extraLeft": []
  },
  "byteSameCount": 3,
  "counts": {
    "SAME": 67,
    "SAME_EFFECTS": 1,
    "SAME_LAYOUT": 0,
    "DEVIATION": 0,
    "DIFF": 1,
    "ONLY_LEFT": 0,
    "ONLY_RIGHT": 0,
    "BYTE_SAME": 3
  },
  "classes": [
    {
      "name": "local/lito381/server/NativeNpcPhysicalSource",
      "status": "SAME",
      "byteSame": false,
      "allowMethodsApplied": false,
      "effectsMethods": [],
      "approvedMethods": []
    },
    {
      "name": "local/lito381/server/NativeNpcPhysicalSource$Snapshot",
      "status": "SAME",
      "byteSame": false,
      "allowMethodsApplied": false,
      "effectsMethods": [],
      "approvedMethods": []
    },
    {
      "name": "local/lito381/server/NativePcActionAuthority",
      "status": "SAME",
      "byteSame": false,
      "allowMethodsApplied": false,
      "effectsMethods": [],
      "approvedMethods": []
    },
    {
      "name": "local/lito381/server/NativePetWorldLifetime",
      "status": "SAME",
      "byteSame": false,
      "allowMethodsApplied": false,
      "effectsMethods": [],
      "approvedMethods": []
    },
    {
      "name": "local/lito381/server/NativePetWorldLifetime$Target",
      "status": "SAME",
      "byteSame": false,
      "allowMethodsApplied": false,
      "effectsMethods": [],
      "approvedMethods": []
    },
    {
      "name": "local/lito381/server/NativePlayerVitals",
      "status": "SAME",
      "byteSame": false,
      "allowMethodsApplied": false,
      "effectsMethods": [],
      "approvedMethods": []
    },
    {
      "name": "local/lito381/server/NativePlayerVitals$Result",
      "status": "SAME",
      "byteSame": false,
      "allowMethodsApplied": false,
      "effectsMethods": [],
      "approvedMethods": []
    },
    {
      "name": "local/lito381/server/NativeProcEncounterCatalog",
      "status": "SAME",
      "byteSame": false,
      "allowMethodsApplied": false,
      "effectsMethods": [],
      "approvedMethods": []
    },
    {
      "name": "local/lito381/server/NativeProcEncounterCatalog$Catalog",
      "status": "SAME",
      "byteSame": false,
      "allowMethodsApplied": false,
      "effectsMethods": [],
      "approvedMethods": []
    },
    {
      "name": "local/lito381/server/NativeProcEncounterCatalog$Npc",
      "status": "SAME",
      "byteSame": false,
      "allowMethodsApplied": false,
      "effectsMethods": [],
      "approvedMethods": []
    },
    {
      "name": "local/lito381/server/NativeProcEncounterCatalog$Receiver",
      "status": "SAME",
      "byteSame": false,
      "allowMethodsApplied": false,
      "effectsMethods": [],
      "approvedMethods": []
    },
    {
      "name": "local/lito381/server/NativeProcEncounterCatalog$Type",
      "status": "SAME",
      "byteSame": false,
      "allowMethodsApplied": false,
      "effectsMethods": [],
      "approvedMethods": []
    },
    {
      "name": "local/lito381/server/NativeProcEncounterPublication",
      "status": "SAME",
      "byteSame": false,
      "allowMethodsApplied": false,
      "effectsMethods": [],
      "approvedMethods": []
    },
    {
      "name": "local/lito381/server/NativeProcEncounterPublication$Context",
      "status": "SAME",
      "byteSame": false,
      "allowMethodsApplied": false,
      "effectsMethods": [],
      "approvedMethods": []
    },
    {
      "name": "local/lito381/server/NativeProcEncounterPublication$Stale",
      "status": "SAME",
      "byteSame": false,
      "allowMethodsApplied": false,
      "effectsMethods": [],
      "approvedMethods": []
    },
    {
      "name": "local/lito381/server/NativeProcEncounterPublication$Viewer",
      "status": "SAME",
      "byteSame": false,
      "allowMethodsApplied": false,
      "effectsMethods": [],
      "approvedMethods": []
    },
    {
      "name": "local/lito381/server/NativeProcHealBonus",
      "status": "SAME",
      "byteSame": false,
      "allowMethodsApplied": false,
      "effectsMethods": [],
      "approvedMethods": []
    },
    {
      "name": "local/lito381/server/NativeProcHealBonus$Catalog",
      "status": "SAME",
      "byteSame": false,
      "allowMethodsApplied": false,
      "effectsMethods": [],
      "approvedMethods": []
    },
    {
      "name": "local/lito381/server/NativeProcHealingWorld",
      "status": "SAME",
      "byteSame": false,
      "allowMethodsApplied": false,
      "effectsMethods": [],
      "approvedMethods": []
    },
    {
      "name": "local/lito381/server/NativeProcHealingWorld$Plan",
      "status": "SAME",
      "byteSame": false,
      "allowMethodsApplied": false,
      "effectsMethods": [],
      "approvedMethods": []
    },
    {
      "name": "local/lito381/server/NativeProcHealingWorld$Target",
      "status": "SAME",
      "byteSame": false,
      "allowMethodsApplied": false,
      "effectsMethods": [],
      "approvedMethods": []
    },
    {
      "name": "local/lito381/server/NativeProcNpcCatalog",
      "status": "SAME",
      "byteSame": false,
      "allowMethodsApplied": false,
      "effectsMethods": [],
      "approvedMethods": []
    },
    {
      "name": "local/lito381/server/NativeProcNpcCatalog$1",
      "status": "SAME",
      "byteSame": false,
      "allowMethodsApplied": false,
      "effectsMethods": [],
      "approvedMethods": []
    },
    {
      "name": "local/lito381/server/NativeProcNpcCatalog$Catalog",
      "status": "SAME",
      "byteSame": false,
      "allowMethodsApplied": false,
      "effectsMethods": [],
      "approvedMethods": []
    },
    {
      "name": "local/lito381/server/NativeProcNpcCatalog$Npc",
      "status": "SAME",
      "byteSame": false,
      "allowMethodsApplied": false,
      "effectsMethods": [],
      "approvedMethods": []
    },
    {
      "name": "local/lito381/server/NativeProcNpcState",
      "status": "SAME",
      "byteSame": false,
      "allowMethodsApplied": false,
      "effectsMethods": [],
      "approvedMethods": []
    },
    {
      "name": "local/lito381/server/NativeProcPlayerState",
      "status": "SAME",
      "byteSame": false,
      "allowMethodsApplied": false,
      "effectsMethods": [],
      "approvedMethods": []
    },
    {
      "name": "local/lito381/server/NativeProcPlayerState$Identity",
      "status": "SAME",
      "byteSame": false,
      "allowMethodsApplied": false,
      "effectsMethods": [],
      "approvedMethods": []
    },
    {
      "name": "local/lito381/server/NativeReuseLog",
      "status": "SAME",
      "byteSame": false,
      "allowMethodsApplied": false,
      "effectsMethods": [],
      "approvedMethods": []
    },
    {
      "name": "local/lito381/server/NativeReuseLog$Log",
      "status": "SAME",
      "byteSame": false,
      "allowMethodsApplied": false,
      "effectsMethods": [],
      "approvedMethods": []
    },
    {
      "name": "local/lito381/server/NativeReuseLog$Logger",
      "status": "SAME",
      "byteSame": false,
      "allowMethodsApplied": false,
      "effectsMethods": [],
      "approvedMethods": []
    },
    {
      "name": "local/lito381/server/NativeRoomRules",
      "status": "SAME",
      "byteSame": false,
      "allowMethodsApplied": false,
      "effectsMethods": [],
      "approvedMethods": []
    },
    {
      "name": "local/lito381/server/NativeRoomRules$Policy",
      "status": "SAME",
      "byteSame": true,
      "allowMethodsApplied": false,
      "effectsMethods": [],
      "approvedMethods": []
    },
    {
      "name": "local/lito381/server/NativeServerLifetime",
      "status": "SAME",
      "byteSame": false,
      "allowMethodsApplied": false,
      "effectsMethods": [],
      "approvedMethods": []
    },
    {
      "name": "local/lito381/server/NativeSkillConsumptionBridges",
      "status": "SAME",
      "byteSame": false,
      "allowMethodsApplied": false,
      "effectsMethods": [],
      "approvedMethods": []
    },
    {
      "name": "local/lito381/server/NativeSourceActionIntervalCalculator",
      "status": "SAME",
      "byteSame": false,
      "allowMethodsApplied": false,
      "effectsMethods": [],
      "approvedMethods": []
    },
    {
      "name": "local/lito381/server/NativeSourceActionIntervalCalculator$ActionType",
      "status": "SAME",
      "byteSame": false,
      "allowMethodsApplied": false,
      "effectsMethods": [],
      "approvedMethods": []
    },
    {
      "name": "local/lito381/server/NativeSourceActionIntervalCalculator$SpeedState",
      "status": "SAME",
      "byteSame": false,
      "allowMethodsApplied": false,
      "effectsMethods": [],
      "approvedMethods": []
    },
    {
      "name": "local/lito381/server/NativeSourceClientActionTimeline",
      "status": "SAME",
      "byteSame": false,
      "allowMethodsApplied": false,
      "effectsMethods": [],
      "approvedMethods": []
    },
    {
      "name": "local/lito381/server/NativeSourceClientActionTimeline$ActionBlockSnapshot",
      "status": "SAME",
      "byteSame": false,
      "allowMethodsApplied": false,
      "effectsMethods": [],
      "approvedMethods": []
    },
    {
      "name": "local/lito381/server/NativeSourceClientActionTimeline$ActionKind",
      "status": "SAME",
      "byteSame": false,
      "allowMethodsApplied": false,
      "effectsMethods": [],
      "approvedMethods": []
    },
    {
      "name": "local/lito381/server/NativeSourceClientActionTimeline$CombatDeferral",
      "status": "SAME",
      "byteSame": false,
      "allowMethodsApplied": false,
      "effectsMethods": [],
      "approvedMethods": []
    },
    {
      "name": "local/lito381/server/NativeSourceClientActionTimeline$Deadline",
      "status": "SAME",
      "byteSame": false,
      "allowMethodsApplied": false,
      "effectsMethods": [],
      "approvedMethods": []
    },
    {
      "name": "local/lito381/server/NativeSourceClientActionTimeline$Reservation",
      "status": "SAME",
      "byteSame": false,
      "allowMethodsApplied": false,
      "effectsMethods": [],
      "approvedMethods": []
    },
    {
      "name": "local/lito381/server/NativeSourceClientActionTimeline$VisibleWindow",
      "status": "SAME",
      "byteSame": false,
      "allowMethodsApplied": false,
      "effectsMethods": [],
      "approvedMethods": []
    },
    {
      "name": "local/lito381/server/NativeSourceMaterialPlan",
      "status": "SAME",
      "byteSame": false,
      "allowMethodsApplied": false,
      "effectsMethods": [],
      "approvedMethods": []
    },
    {
      "name": "local/lito381/server/NativeSourceMaterialPlan$ConsumeRequirement",
      "status": "SAME",
      "byteSame": false,
      "allowMethodsApplied": false,
      "effectsMethods": [],
      "approvedMethods": []
    },
    {
      "name": "local/lito381/server/NativeSourceMaterialPlan$Debit",
      "status": "SAME",
      "byteSame": false,
      "allowMethodsApplied": false,
      "effectsMethods": [],
      "approvedMethods": []
    },
    {
      "name": "local/lito381/server/NativeSourceMaterialPlan$ItemConsumeCommitResult",
      "status": "SAME",
      "byteSame": false,
      "allowMethodsApplied": false,
      "effectsMethods": [],
      "approvedMethods": []
    },
    {
      "name": "local/lito381/server/NativeSourceMaterialPlan$PlannedConsumption",
      "status": "SAME",
      "byteSame": false,
      "allowMethodsApplied": false,
      "effectsMethods": [],
      "approvedMethods": []
    },
    {
      "name": "local/lito381/server/NativeSourceMaterialPlan$Result",
      "status": "SAME",
      "byteSame": false,
      "allowMethodsApplied": false,
      "effectsMethods": [],
      "approvedMethods": []
    },
    {
      "name": "local/lito381/server/NativeSourceSkillCastAdmission",
      "status": "SAME",
      "byteSame": false,
      "allowMethodsApplied": false,
      "effectsMethods": [],
      "approvedMethods": []
    },
    {
      "name": "local/lito381/server/NativeSourceSkillCastAdmission$Reservation",
      "status": "SAME",
      "byteSame": false,
      "allowMethodsApplied": false,
      "effectsMethods": [],
      "approvedMethods": []
    },
    {
      "name": "local/lito381/server/NativeSourceSkillCastAdmission$Result",
      "status": "SAME",
      "byteSame": false,
      "allowMethodsApplied": false,
      "effectsMethods": [],
      "approvedMethods": []
    },
    {
      "name": "local/lito381/server/NativeSourceSkillConsumptionMath",
      "status": "SAME",
      "byteSame": false,
      "allowMethodsApplied": false,
      "effectsMethods": [],
      "approvedMethods": []
    },
    {
      "name": "local/lito381/server/NativeSourceSkillDelay",
      "status": "SAME",
      "byteSame": false,
      "allowMethodsApplied": false,
      "effectsMethods": [],
      "approvedMethods": []
    },
    {
      "name": "local/lito381/server/NativeSourceSkillDelay$Lease",
      "status": "SAME",
      "byteSame": false,
      "allowMethodsApplied": false,
      "effectsMethods": [],
      "approvedMethods": []
    },
    {
      "name": "local/lito381/server/ResourceDatabase",
      "status": "SAME",
      "byteSame": false,
      "allowMethodsApplied": false,
      "effectsMethods": [],
      "approvedMethods": []
    },
    {
      "name": "local/lito381/server/ResourceDatabase$Key",
      "status": "SAME",
      "byteSame": true,
      "allowMethodsApplied": false,
      "effectsMethods": [],
      "approvedMethods": []
    },
    {
      "name": "local/lito381/server/ResourceDatabase$Snapshot",
      "status": "SAME",
      "byteSame": true,
      "allowMethodsApplied": false,
      "effectsMethods": [],
      "approvedMethods": []
    },
    {
      "name": "local/lito381/server/RuntimeInventory",
      "status": "SAME_EFFECTS",
      "byteSame": false,
      "allowMethodsApplied": false,
      "effectsMethods": [
        {
          "method": "copy()Llocal/lito381/server/RuntimeInventory;",
          "status": "SAME_EFFECTS",
          "detail": null
        },
        {
          "method": "sourceEquipmentOnly()Llocal/lito381/server/RuntimeInventory;",
          "status": "SAME_EFFECTS",
          "detail": null
        },
        {
          "method": "withoutObjects(Ljava/util/Set;)Llocal/lito381/server/RuntimeInventory;",
          "status": "SAME_EFFECTS",
          "detail": null
        }
      ],
      "approvedMethods": []
    },
    {
      "name": "local/lito381/server/RuntimeInventory$Item",
      "status": "SAME",
      "byteSame": false,
      "allowMethodsApplied": false,
      "effectsMethods": [],
      "approvedMethods": []
    },
    {
      "name": "local/lito381/server/RuntimeInventory$NativeArmorBonuses",
      "status": "SAME",
      "byteSame": false,
      "allowMethodsApplied": false,
      "effectsMethods": [],
      "approvedMethods": []
    },
    {
      "name": "local/lito381/server/RuntimeInventory$PersistentItem",
      "status": "SAME",
      "byteSame": false,
      "allowMethodsApplied": false,
      "effectsMethods": [],
      "approvedMethods": []
    },
    {
      "name": "local/lito381/server/ServerConfig",
      "status": "DIFF",
      "byteSame": false,
      "allowMethodsApplied": false,
      "effectsMethods": [
        {
          "method": "applyExternalSourceServers(Ljava/util/Properties;Ljava/nio/file/Path;)V",
          "status": "DIFF",
          "detail": "method applyExternalSourceServers(Ljava/util/Properties;Ljava/nio/file/Path;)V: effects sequence[2]: left=ASTORE right=INVOKESTATIC local/lito381/server/ServerConfig.optional(Ljava/util/Properties;Ljava/lang/String;)Ljava/lang/String;"
        },
        {
          "method": "load(Ljava/nio/file/Path;Ljava/lang/String;)Llocal/lito381/server/ServerConfig;",
          "status": "SAME_EFFECTS",
          "detail": null
        }
      ],
      "approvedMethods": [],
      "firstDiff": {
        "where": "method",
        "member": "applyExternalSourceServers(Ljava/util/Properties;Ljava/nio/file/Path;)V",
        "insnIndex": null,
        "detail": "effects sequence[2]: left=ASTORE right=INVOKESTATIC local/lito381/server/ServerConfig.optional(Ljava/util/Properties;Ljava/lang/String;)Ljava/lang/String;"
      }
    },
    {
      "name": "local/lito381/server/SharedClientResources",
      "status": "SAME",
      "byteSame": false,
      "allowMethodsApplied": false,
      "effectsMethods": [],
      "approvedMethods": []
    },
    {
      "name": "local/lito381/server/SourceEquipmentPhysicalStats",
      "status": "SAME",
      "byteSame": false,
      "allowMethodsApplied": false,
      "effectsMethods": [],
      "approvedMethods": []
    },
    {
      "name": "local/lito381/server/SourceLogin",
      "status": "SAME",
      "byteSame": false,
      "allowMethodsApplied": false,
      "effectsMethods": [],
      "approvedMethods": []
    },
    {
      "name": "local/lito381/server/SourceLogin$Binding",
      "status": "SAME",
      "byteSame": false,
      "allowMethodsApplied": false,
      "effectsMethods": [],
      "approvedMethods": []
    }
  ]
}
```

</details>

<details>
<summary>27 個來源與 69 個 class 的 SHA-256 收據</summary>

```json
{
  "toolCommit": "450178d59b0157fddf2b9456b7c03d20207993fc",
  "formalSHA256": "e1498871c7be6b287821c57ef5d95933db1740d9b4d4a80f6c3465f21d99d498",
  "leftJarSHA256": "2fb2717cbad047df6ff642790f3dfb1cd683324ccaf1e31c6de5877ce2af5b43",
  "counts": {
    "SAME": 67,
    "SAME_EFFECTS": 1,
    "SAME_LAYOUT": 0,
    "DEVIATION": 0,
    "DIFF": 1,
    "ONLY_LEFT": 0,
    "ONLY_RIGHT": 0,
    "BYTE_SAME": 3
  },
  "selection": {
    "missingLeft": [],
    "extraLeft": []
  },
  "sourceHashes": {
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
    "lito伺服器\\LiTo381-Server\\merge-inputs\\formal-0.6.71\\staged\\local\\lito381\\server\\SourceLogin.java": "f60bf58cdcb398361c90dd264d5e712001fb6e96c2229aa52c6e9607f40ce33c"
  },
  "classHashes": {
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
      "left": "8707d01157a19ece9cd1031b6daf410c1fce7f85bbf90f15ebf3d15801f5d730",
      "right": "ee8eb030e64444bf9585a796b5ee8ff88fd8ad92c3036546bb22ee370dd977c1"
    }
  }
}
```

</details>

## 完整重驗腳本

<!-- A06-VERIFY -->

```python
import hashlib, json, pathlib, subprocess, urllib.request, zipfile, os, sys, difflib
ROOT = pathlib.Path.cwd().resolve()
E = ROOT / 'scratch/a06-current'
D = E / 'dependencies'
S = ROOT / 'lito伺服器/LiTo381-Server'
DS = D / 'lito伺服器/LiTo381-Server'
T = S / 'target/a06-current'
J = pathlib.Path('C:/Program Files/Microsoft/jdk-17.0.17.10-hotspot/bin')
TOOL_COMMIT = '450178d59b0157fddf2b9456b7c03d20207993fc'
B = next(b for b in json.loads((ROOT/'docs/combat-merge/a-batches.json').read_text('utf-8'))['batches'] if b['id']=='A06')
E.mkdir(parents=True,exist_ok=True)
def sha(p): return hashlib.sha256(p.read_bytes()).hexdigest()
def run(args, log):
    p = subprocess.run([str(a) for a in args], stdout=subprocess.PIPE, stderr=subprocess.STDOUT)
    text = p.stdout.decode('utf-8', errors='replace')
    (E/log).write_text(text, 'utf-8')
    if p.returncode: raise RuntimeError(f'{log}: {p.returncode}\n{text}')
    return text
run(['git','archive',TOOL_COMMIT,'-o',E/'dependencies.zip','--','docs/combat-merge','lito伺服器/LiTo381-Server/tools/semantic-diff','lito伺服器/LiTo381-Server/tools/legacy-stubs','lito伺服器/LiTo381-Server/compile-stubs'],'archive.log')
with zipfile.ZipFile(E/'dependencies.zip') as z:
    for name in z.namelist():
        assert (D/name).resolve().is_relative_to(D.resolve())
    z.extractall(D)
for name in ['tool','dependency','classes','test']:
    (T/name).mkdir(parents=True, exist_ok=True)
for art, expected in {
 'asm':'adf46d5e34940bdf148ecdd26a9ee8eea94496a72034ff7141066b3eea5c4e9d',
 'asm-tree':'62f4b3bc436045c1acb5c3ba2d8ec556ec3369093d7f5d06c747eb04b56d52b1',
 'asm-analysis':'7bc6bcbc21379948a0c8c467fb0f864206e5b818f6bc0b546872f5c9f941556f'}.items():
    dest=T/'dependency'/f'{art}-9.7.jar'
    if not dest.exists():
        cache=pathlib.Path('D:/dev-cache/maven/repository/org/ow2/asm')/art/'9.7'/dest.name
        dest.write_bytes(cache.read_bytes() if cache.exists() else urllib.request.urlopen(f'https://repo.maven.apache.org/maven2/org/ow2/asm/{art}/9.7/{dest.name}').read())
    assert sha(dest)==expected
run([J/'javac.exe','--release','17','-encoding','UTF-8','-cp',T/'dependency/*','-d',T/'tool',*sorted((DS/'tools/semantic-diff/src/main/java').rglob('*.java'))],'tool-build.log')
run(['powershell','-NoProfile','-ExecutionPolicy','Bypass','-File',DS/'tools/legacy-stubs/build.ps1'],'stub-build.log')
expected=set(B['verify_entries'])
for kind in ['effects','extra-methods']:
    lines=(D/f'docs/combat-merge/{kind}-approved.txt').read_text('utf-8').splitlines()
    lines=[line for line in lines if line.split('#',1)[0]+'.class' in expected]
    assert len(lines)==(5 if kind=='effects' else 1)
    (E/f'{kind}-approved.txt').write_text('\n'.join(lines)+'\n','utf-8')
(E/'verify_entries.txt').write_text('\n'.join(B['verify_entries'])+'\n','utf-8')
files=[S/'merge-inputs/formal-0.6.71/staged/local/lito381/server'/f'{n}.java' for n in B['classes']]
formal=S/'reference/base-0.6.71.jar'
assert sha(formal)=='e1498871c7be6b287821c57ef5d95933db1740d9b4d4a80f6c3465f21d99d498'
# Delete only previous class files under this worktree's exact task output.
assert (T/'classes').resolve().is_relative_to(ROOT)
for p in (T/'classes').rglob('*.class'):
    assert not p.is_symlink()
    p.unlink()
cp=';'.join(map(str,[formal,DS/'target/legacy-stubs',S/'lib/*']))
run([J/'javac.exe','-J-Dfile.encoding=UTF-8','--release','17','-encoding','UTF-8','-cp',cp,'-d',T/'classes',*files],'javac.log')
run([J/'jar.exe','--create','--file',T/'left.jar','--no-manifest','--date','2026-11-05T00:00:00Z','-C',T/'classes','.'],'jar.log')
with zipfile.ZipFile(T/'left.jar') as z, zipfile.ZipFile(formal) as r:
    actual={n for n in z.namelist() if n.endswith('.class')}
    assert actual==expected, (actual-expected,expected-actual)
    hashes={n:{'left':hashlib.sha256(z.read(n)).hexdigest(),'right':hashlib.sha256(r.read(n)).hexdigest()} for n in sorted(expected)}
toolcp=';'.join(map(str,[T/'tool',T/'dependency/*']))
run([J/'java.exe','-Dfile.encoding=UTF-8','-cp',toolcp,'local.lito381.semanticdiff.SemanticDiff','--left',T/'left.jar','--right',formal,'--left-classpath',formal,'--classes',E/'verify_entries.txt','--effects-methods',E/'effects-approved.txt','--extra-methods',E/'extra-methods-approved.txt','--json',E/'result.json','--md',E/'result.md'],'semantic.log')
result=json.loads((E/'result.json').read_text('utf-8'))
receipt={'toolCommit':TOOL_COMMIT,'formalSHA256':sha(formal),'leftJarSHA256':sha(T/'left.jar'),'counts':result['counts'],'selection':result['classSelection'],'sourceHashes':{str(f.relative_to(ROOT)):sha(f) for f in files},'classHashes':hashes}
(E/'receipt.json').write_text(json.dumps(receipt,ensure_ascii=False,indent=2),'utf-8')
print(json.dumps(result['counts']))
for c in result['classes']:
    if c['status'] not in ['SAME','SAME_EFFECTS']: print(json.dumps(c,ensure_ascii=False))
run([J/'java.exe','-Dfile.encoding=UTF-8','-Dsemanticdiff.disable=captures','-cp',toolcp,'local.lito381.semanticdiff.SemanticDiff','--left',T/'left.jar','--right',formal,'--left-classpath',formal,'--classes',E/'verify_entries.txt','--effects-methods',E/'effects-approved.txt','--extra-methods',E/'extra-methods-approved.txt','--json',E/'no-captures.json','--md',E/'no-captures.md'],'no-captures.log')
no_captures=json.loads((E/'no-captures.json').read_text('utf-8'))
assert next(c for c in no_captures['classes'] if c['name']=='local/lito381/server/SourceLogin')['status']=='SAME'
descriptors=[]
for side,jar in [('left',T/'left.jar'),('right',formal)]:
    text=run([J/'javap.exe','-J-Dfile.encoding=UTF-8','-p','-s','-c','-classpath',jar,'local.lito381.server.SourceLogin'],f'{side}-SourceLogin.javap.txt')
    body=text.split('private static void lambda$start$0(',1)[1].split('static {};',1)[0]
    descriptors.append(next(line.strip() for line in body.splitlines() if 'descriptor:' in line))
    assert 'anewarray' not in body and 'captureOrder' not in body
assert descriptors[0]==descriptors[1]
(E/'capture-check.txt').write_text('PASS: raw lambda descriptors equal; no array allocation emitted; SAME with captures normalization disabled\n'+descriptors[0]+'\n','utf-8')
run([J/'javac.exe','--release','17','-encoding','UTF-8','-cp',toolcp,'-d',T/'tool',E/'EffectsDiagnostic.java'],'diagnostic-build.log')
run([J/'java.exe','-Dfile.encoding=UTF-8','-cp',toolcp,'local.lito381.semanticdiff.EffectsDiagnostic',T/'left.jar',formal,E],'diagnostic.log')
diff=''.join(difflib.unified_diff((E/'effects-left.txt').read_text('utf-8').splitlines(True),(E/'effects-right.txt').read_text('utf-8').splitlines(True),fromfile='staged',tofile='formal'))
(E/'effects.diff').write_text(diff,'utf-8')
assert result['comparedClassCount']==69 and not any(result['classSelection'].values())
passed=all(c['status'] in ['SAME','SAME_EFFECTS'] for c in result['classes']) and len(result['classes'])==69
print('A06_ACCEPTANCE='+('PASS' if passed else 'FAIL'))
sys.exit(0 if passed else 2)
```

<!-- A06-DIAGNOSTIC -->

```java
package local.lito381.semanticdiff;
import java.nio.file.*;
import java.util.*;
import org.objectweb.asm.tree.*;
public class EffectsDiagnostic {
    static Object call(String name, Class<?>[] types, Object... args) throws Exception {
        var method=ClassComparer.class.getDeclaredMethod(name,types);
        method.setAccessible(true);
        return method.invoke(null,args);
    }
    public static void main(String[] args) throws Exception {
        var left=JarIndex.load(Path.of(args[0]));
        var right=JarIndex.load(Path.of(args[1]));
        left.addClasspath(Path.of(args[1]));
        String owner="local/lito381/server/ServerConfig";
        String apply="applyExternalSourceServers(Ljava/util/Properties;Ljava/nio/file/Path;)V";
        String load="load(Ljava/nio/file/Path;Ljava/lang/String;)Llocal/lito381/server/ServerConfig;";
        String helper="rethrowSourceFailure(Ljava/lang/Throwable;)V";
        var exact=Map.of(owner,Set.of(apply,load,helper));
        left.preserveExactMethods(exact);right.preserveExactMethods(exact);
        var lc=left.readTree(owner);var rc=right.readTree(owner);
        var summary=new ArrayList<String>();
        for(var lm:lc.methods){
            String key=lm.name+lm.desc;
            var rm=rc.methods.stream().filter(m->(m.name+m.desc).equals(key)).findFirst().orElse(null);
            if(rm==null){summary.add(key+" = ONLY_LEFT (approved helper="+key.equals(helper)+")");continue;}
            var diff=ClassComparer.compareMethod(key,lm,rm,left,right,Set.of(apply,load).contains(key));
            summary.add(key+" = "+(diff==null?"SAME":diff.detail));
            if(!key.equals(apply))continue;
            for(int side=0;side<2;side++){
                var jar=side==0?left:right;
                var m=jar.beforeWebs.getOrDefault(side==0?lm:rm,side==0?lm:rm);
                var otherJar=side==0?right:left;
                var other=otherJar.beforeWebs.getOrDefault(side==0?rm:lm,side==0?rm:lm);
                var lines=new ArrayList<String>();
                lines.add("METHOD "+key+" access="+m.access+" signature="+m.signature+" exceptions="+m.exceptions);
                var skipped=call("tempStores",new Class[]{MethodNode.class,MethodNode.class},m,other);
                lines.addAll((List<String>)call("effectsSequence",new Class[]{MethodNode.class,JarIndex.class,Set.class},m,jar,skipped));
                lines.add("CONSTANTS "+call("constants",new Class[]{MethodNode.class,JarIndex.class},m,jar));
                lines.add("CATCH_TYPES "+call("catchTypes",new Class[]{MethodNode.class,JarIndex.class},m,jar));
                lines.add("BRANCHES "+call("branches",new Class[]{MethodNode.class},m));
                Files.write(Path.of(args[2],side==0?"effects-left.txt":"effects-right.txt"),lines);
            }
        }
        Files.write(Path.of(args[2],"server-config-methods.txt"),summary);
    }
}
```
