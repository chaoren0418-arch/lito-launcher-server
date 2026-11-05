# Issue #69 — T020-B08 戰鬥併入暫存

## 本輪結論（2026-11-06，Asia/Taipei）

**已完成 #203／#216 合併後的重新編譯與全批重驗；仍有 4 個 DIFF，B08 尚未通過驗收，不可交付 B-land。**

本輪結果為 **141 SAME、10 SAME_PATHS、5 SAME_CFG、4 DIFF**；30 個外層類別產出 **160 個 entry，missing=0、extra=0、ONLY_LEFT=0、ONLY_RIGHT=0**。共 156 個 entry 符合[主控接受狀態](https://github.com/chaoren0418-arch/lito-launcher-server/issues/69#issuecomment-6666508373)。方法級仍有未配對 lambda 的 ONLY_LEFT／ONLY_RIGHT，不能把類別集合無缺漏誤讀為每個方法都通過。

Issue #69 已加上 `needs:claude`。本輪不再以「等 #203／#216 合併」列為阻塞；兩項工具已取得，剩餘差異需主控依下列新證據裁定來源修補或工具後續工作。沒有新增豁免或將 DIFF 改列通過。

## 已驗證：做了什麼

- 沿用 worktree `D:/lineage開發/3-lito開發/scratch/wt/task-69` 與 `task/69`，起點 `b05465a2bf04a048ccf2454a3ccdabd7ec69a4b3`；未 checkout／建分支。
- `git fetch origin main` 後，以 `13682bb88c12efad235a0de1ed6bba8a9563b802` 合併 `origin/main` 的 `54c908ab43d048ca6b9fe5e069c15734d8f9e8b4`，無衝突。包含 #203／PR #215（merge `5ae1eac2`）與 #216／PR #238（merge `54c908a`）。
- semantic-diff 最新程式提交 `743c55ee924dd4942bfc0bd717e53081fd319fdd`，工具 tree `84e51c810a643288d978b476bde46a0c8acba999`；AsRecord 最近提交 `ff50cca6df1eb87c931b3d155ea0bfd72a65897b`。由當前工作樹原始碼重新編譯工具。
- 先完成一般重驗，再用全新 `target/issue-69/recheck-216/` 重建工具、註解與 30 份 staged Java，執行 AsRecord 並打包。兩次完整 `classes` JSON、`counts`、`classSelection` 一致；160 個 class bytes 亦逐筆一致。全新目錄的結果是本報告的正式收據。
- `git diff b05465a HEAD -- <B08 的 30 個 staged 檔>` 無差異。本輪未再修改 Java，保留前次修好的 `NativeProcHealingPlayer` 巢狀 monitor／vitalLock finally、`NativeLightAction.Output` package-private、六個 AsRecord 轉換及 helper／Exceptions 核准條目。治療類別與 Output 本輪仍為 SAME。
- 本輪手工修改只有本報告；PR 累計差異仍為 B08 的 30 份 staged Java 與本報告。合併 main 帶入的其他已核准變更不是本輪手工編輯；`src/main`、`pom.xml`、`docs/REPORT.md` 相對合併主線無任務差異。
- `git diff --check` 通過；以本輪合併的主線 `54c908a` 核對上述三個禁止修改路徑，差異為空。
- 沒有執行 staged 程式、啟動伺服器／登入器／遊戲、連正式 DB，亦未讀取機密設定或封存。原始專案工作檔只讀；必要 Git 操作更新 worktree 共用 `.git` 中繼資料。

### 前次 10 個 DIFF 的本輪結果

| entry（省略 local/lito381/server/） | 本輪狀態 |
|---|---|
| `ClassicWorldMonsterPopulation` | SAME_CFG |
| `NativeCancellationNpcStatic` | SAME_CFG |
| `NativeCombatAttacker` | SAME_PATHS |
| `NativeEquipmentDefense` | SAME_CFG |
| `NativeLightAction` | DIFF |
| `NativeLightAction$Batch` | DIFF |
| `NativePlayerCommit` | DIFF |
| `NativePlayerLifetime` | SAME_CFG |
| `NativePlayerVitalCompletions` | SAME_CFG |
| `NativeProcHealingEncounter$Dispatch` | DIFF |

`NativeCombatAttacker.calculated` 本輪不再列出差異；沒有改寫其 equals owner。方法級另有 **14 個 SAME_CFG**，不可把這個數量混作類別數。

## 怎麼驗證

JDK 為 Microsoft OpenJDK **17.0.17**。ASM／asm-tree／asm-analysis 為 **9.7**；前兩項只讀 Maven 快取，asm-analysis 沿用本 worktree `target/issue-69/asm-analysis-9.7.jar`。worktree 沒有 lib/，因此另外只讀既有 HikariCP 5.1.0 快取供編譯；未加 stub 或修改 pom。

下列命令以伺服器目錄為基準，`<repo>` 指本 worktree，`<W>` 為 `<repo>/scratch/issue-69/recheck-216`，`<T>` 為 `target/issue-69/recheck-216`，`<ASM>` 為上述三個 JAR 以分號串接。每個 argfile 都是 UTF-8 無 BOM、絕對路徑逐行加雙引號；`staged.args` 從本輪 B08.classes 產生 30 行，`entries.txt` 從 B08.verify_entries 產生全部 160 行。未沿用前次診斷工具覆寫過的 argfile。

```text
javac -J-Dfile.encoding=UTF-8 --release 17 -encoding UTF-8 -cp "<ASM>"
  -d <T>/tools @<W>/tools.args
# tools.args = tools/semantic-diff/src/main/java 與 tools/as-record/src/main/java 全部 Java

javac -J-Dfile.encoding=UTF-8 --release 17 -encoding UTF-8 -cp ""
  -d <T>/annotations @<W>/annotations.args
# annotations.args = AsRecord.java、AsmFinal.java

javac -J-Dfile.encoding=UTF-8 --release 17 -encoding UTF-8
  -cp "reference/candidate-50635f7e.jar;reference/base-0.6.71.jar;<T>/annotations;lib/*;D:/dev-cache/maven/repository/com/zaxxer/HikariCP/5.1.0/HikariCP-5.1.0.jar"
  -d <T>/classes @<W>/staged.args
Note: Some input files use unchecked or unsafe operations.
Note: Recompile with -Xlint:unchecked for details.
# 三次 javac 退出碼皆 0

java -cp "<T>/tools;<ASM>" local.lito381.asrecord.AsRecord <T>/classes
as-record: scanned=160 converted=6
# 退出碼 0；只將 classes 下的 class 打包到 <T>/b08.jar，不放入註解或工具

java -Dfile.encoding=UTF-8 -Xmx2g -cp "<T>/tools;<ASM>" local.lito381.semanticdiff.SemanticDiff
  --left <T>/b08.jar --left-classpath reference/candidate-50635f7e.jar
  --right reference/candidate-50635f7e.jar --classes <W>/entries.txt
  --access-widen <W>/access-widen-approved.txt
  --extra-methods <W>/extra-methods-approved.txt
  --exceptions-exempt <W>/exceptions-exempt.txt
  --json <W>/semantic.json --md <W>/semantic.md
SUMMARY SAME=141 SAME_EFFECTS=0 SAME_PATHS=10 ORPHAN_IGNORED=0 SAME_CFG=5 LAMBDA_PAIRED_BY_CALLSITE=0 VISIBILITY_BRIDGE=0 SAME_LAYOUT=0 DEVIATION=0 DIFF=4 ONLY_LEFT=0 ONLY_RIGHT=0 BYTE_SAME=1 ALLOW_METHODS=0
# 退出碼 0 表示工具完成，不代表 B08 通過
classSelection.missingLeft=[]
classSelection.extraLeft=[]
ZIP entries=160; expected=160; missing=0; extra=0
```

本輪核准僅 access-widen 1 條、extra-method 1 條、Exceptions 3 條；無 effects、layout、deviation、metadata 或 allow-methods 豁免。以下保留前次 checked-exception 編譯失敗證據與精確核准內容，並非宣稱本輪重做該 probe；本輪確認 staged 未變且實際套用相同清單。

### 核准清單與 checked exception 證據

任務限定只交付 staged Java 與本報告，所以沒有改共用核准清單。下列精確條目依主控核准連結寫入 scratch 的同名清單，且完整保留於本報告，供重驗及 B-land 併入共用清單。沒有新增 effects／layout／deviation／metadata 豁免。

`access-widen-approved.txt` 是 `docs/combat-merge/access-widen-approved.txt` 的 B08 適用條目；整份清單包含其他批次，直接傳入會因未選取類別而被工具拒絕。

```text
local/lito381/server/NativeLightAction$Output # approved: https://github.com/chaoren0418-arch/lito-launcher-server/issues/64#issuecomment-6654963452
```

`extra-methods-approved.txt`：

```text
local/lito381/server/NativeLightAction$Batch#rethrowCandidateFailure(Ljava/lang/Throwable;)V # approved: https://github.com/chaoren0418-arch/lito-launcher-server/issues/69#issuecomment-6666508373
```

`exceptions-exempt.txt`（只豁免 throws 屬性，方法本體仍完整比對）：

```text
local/lito381/server/LegacyMySqlRepository#healthCheck()V # approved: https://github.com/chaoren0418-arch/lito-launcher-server/issues/69#issuecomment-6666508373
local/lito381/server/LegacyMySqlRepository#persistTournamentProfile(JLjava/lang/String;Llocal/lito381/server/RepositoryTournamentProfile;Llocal/lito381/server/GameStateStore$TournamentProfileState;)Llocal/lito381/server/RepositoryTournamentMutation; # approved: https://github.com/chaoren0418-arch/lito-launcher-server/issues/69#issuecomment-6666508373
local/lito381/server/LegacyMySqlRepository#tournamentProfiles()Llocal/lito381/server/RepositoryTournamentRoster; # approved: https://github.com/chaoren0418-arch/lito-launcher-server/issues/69#issuecomment-6666508373
```

把三個方法宣告暫改回候選的 `throws SQLException`，只在 scratch 副本執行 javac，取得下列失敗；正式 staged 保留可編譯的 `throws Exception`。沒有將 probe 產物放入驗收 JAR。

```text
D:\lineage開發\3-lito開發\scratch\wt\task-69\scratch\issue-69\checked-probe\LegacyMySqlRepository.java:239: error: unreported exception Exception; must be caught or declared to be thrown
        SharedDatabase.check(this);
                            ^
D:\lineage開發\3-lito開發\scratch\wt\task-69\scratch\issue-69\checked-probe\LegacyMySqlRepository.java:5202: error: unreported exception Exception; must be caught or declared to be thrown
        if (SharedDatabase.registered(this)) return SharedDatabase.profiles(this);
                                                                           ^
D:\lineage開發\3-lito開發\scratch\wt\task-69\scratch\issue-69\checked-probe\LegacyMySqlRepository.java:5246: error: unreported exception Exception; must be caught or declared to be thrown
        if (SharedDatabase.registered(this)) return SharedDatabase.persist(this, characterObjectId, characterName, expected, replacement);
                                                                          ^
3 errors
javac exit code 1
```

套用三個精確 Exceptions 條目後，`LegacyMySqlRepository` 整個類別為 SAME_PATHS，沒有剩餘方法 DIFF。helper 經工具核准，Batch 的方法差異仍保留，未藉核准 helper 略過本體。


### 輸入與本輪產物 SHA-256

- `lito伺服器/LiTo381-Server/reference/base-0.6.71.jar`：`e1498871c7be6b287821c57ef5d95933db1740d9b4d4a80f6c3465f21d99d498`
- `lito伺服器/LiTo381-Server/reference/candidate-50635f7e.jar`：`50635f7edca8ec6ce77787cc32371c7590b497e48ea202f8a679caf3f517b729`
- `lito伺服器/LiTo381-Server/target/issue-69/recheck-216/b08.jar`：`de96b8890030b3bc1c993f8a97273a41cd702781d07b3557b98278b44653dea9`
- `scratch/issue-69/recheck-216/semantic.json`：`3941759d1949139f31352bf094983fc9413781472d2e4efa37e94389735ce7bc`

本輪 JAR 以排序 entry 與固定 ZIP 時間打包，故容器 SHA 不同於前次 JAR，不能據此推斷 class 有變；已核對與本輪第一次編譯的 160 個 class bytes 全部一致。`audit.json` 另存 30 份來源 SHA 與每一步的完整 argv／退出碼。

## 剩餘差異分類（已觀測；等價性待裁定）

| 類型 | 代表與範圍 | 新工具實際結果 |
|---|---|---|
| lambda 捕獲、內容與共用配對 | `NativeLightAction.commitPair`、`deferScoped`、`deferWeaponTargetOutput` | commitPair 捕獲 Task 位置不同；兩個 defer 的同一側共用 lambda，但左右內容身分不同；方法仍有 ONLY_LEFT／ONLY_RIGHT，外層 paths／CFG 未證明。 |
| 例外 handler 與核准轉拋 helper | `NativeLightAction$Batch.emit` | 已套用精確 helper 核准，仍回報 path event multisets differ／labelled CFGs differ。不能因 helper 符合規則就略過整個 emit。 |
| 迴圈與 finally 區塊排列 | `NativePlayerCommit.drain`、`acceptedTail`、`NativeProcHealingEncounter$Dispatch.run` | drain／run 有 cycle，acceptedTail 超過 paths 4096；三者 CFG 仍不同。Dispatch 的多個 continue 會展開多份 complete，候選共用正常出口。 |
| 未使用值與指令搬移 | `NativePlayerCommit.nativeManaTransferRecover` | 候選保留額外 `ALOAD 1; POP`；paths 超過 4096，CFG 仍不同。其他偏移／handler 編號隨指令數不同，完整差異見下方。 |

## 推測

部分差異可能是 Java 與 ASM 的控制流程表達不同，例如 Dispatch 的多份 finally 與 nativeManaTransferRecover 的未使用值；這些只是檢查方向。**本輪沒有證明四個類別等價，也沒有裁定全部是工具誤報。** lambda 內容、捕獲值、例外邊及完成順序仍須逐項核實。

## 待驗證／給下一棒的提示

1. **Claude 主控**：審查本報告四類新證據，分清應修 staged Java 或需工具新增證明的部分。#203／#216 已合併，不應再次以這兩項尚未完成為由重派相同工作。Issue 已標 `needs:claude`。
2. **後續本機執行器**：依裁定處理剩餘 4 個 DIFF，修改後重編全部 30 份、轉換並重驗全部 160 entries；必須 DIFF=0、missing=0、extra=0 且狀態均在核准範圍，才能宣告 B08 完成。
3. **B-land 整合者**：保留本報告的 helper 與 Exceptions 精確核准；任務路徑限制下它們只存 scratch 與本報告，未修改共用核准檔。B03 可繼續引用 package-private `NativeLightAction.Output`，不代表 B08 整批已過。
4. 單元測試及遊戲回歸依 §6-5 不適用本 staged 批次，交 B-land 統一處理；本輪未部署、未做實機驗收。

## 產出路徑

- Git 交付：`docs/reports/issue-69.md`；既有 B08 來源保留於 `lito伺服器/LiTo381-Server/merge-inputs/50635f7e/staged/local/lito381/server/`。
- 本機重現腳本：`scratch/issue-69/recheck_216.py`；argfiles、UTF-8 logs、核准清單、完整 semantic JSON／Markdown、audit 與方法 dump：`scratch/issue-69/recheck-216/`。
- 本機建置產物：`lito伺服器/LiTo381-Server/target/issue-69/recheck-216/`，未入 git。
- PR：[#94](https://github.com/chaoren0418-arch/lito-launcher-server/pull/94)。

## 方法級診斷與完整差異

以下是**本輪新工具**的四個 DIFF entry 與全部 methodResults；接著附全部仍有 DIFF 的非 lambda 方法之完整 unified diff（無截斷），並保留涉及未配對 lambda 的兩側完整方法內容。dump 使用本輪工具的 `JarIndex`、`ClassComparer.methodIdentity/tryCatch/instructions` 讀取 JAR，屬正規化診斷視圖；沒有套用跨兩側的 owner／呼叫點配對／核准 handler 刪除，**不以 dump 文本差異覆蓋 semantic.json 的裁定**。例如 emit dump 仍顯示核准 helper 的 catch，正是供主控檢查本體與涵蓋。

### NativeLightAction

<details>
<summary>本輪全部方法診斷</summary>

```json
{
  "name": "local/lito381/server/NativeLightAction",
  "status": "DIFF",
  "byteSame": false,
  "allowMethodsApplied": false,
  "methodResults": [
    {
      "method": "lambda$commitPair$9(Llocal/lito381/server/NativeLightAction$Ingress;Llocal/lito381/server/ClientSession;Llocal/lito381/server/NativePlayerCommit$Binding;Llocal/lito381/server/ClientSession;Llocal/lito381/server/NativePlayerVitalCompletions$Receipt;Llocal/lito381/server/NativeSelfCastAction$Task;)V",
      "status": "ONLY_RIGHT",
      "detail": "only right"
    },
    {
      "method": "lambda$deferScoped$10(Llocal/lito381/server/NativeLightAction$Batch;Llocal/lito381/server/NativeSelfCastAction$Task;)V",
      "status": "ONLY_LEFT",
      "detail": "only left"
    },
    {
      "method": "lambda$deferWeaponTargetOutput$0(Llocal/lito381/server/NativeLightAction$Batch;Llocal/lito381/server/NativeSelfCastAction$Task;)V",
      "status": "ONLY_RIGHT",
      "detail": "only right"
    },
    {
      "method": "activate(Llocal/lito381/server/ClientSession;IJI)V",
      "status": "SAME_PATHS",
      "detail": "path multisets equal (5)"
    },
    {
      "method": "cancel(Llocal/lito381/server/ClientSession;)Z",
      "status": "SAME_PATHS",
      "detail": "path multisets equal (5)"
    },
    {
      "method": "clear(Llocal/lito381/server/ClientSession;)V",
      "status": "SAME_PATHS",
      "detail": "path multisets equal (5)"
    },
    {
      "method": "commitPair(Llocal/lito381/server/ClientSession;Llocal/lito381/server/ClientSession;Llocal/lito381/server/NativeSelfCastAction$Task;)V",
      "status": "DIFF",
      "detail": "method commitPair(Llocal/lito381/server/ClientSession;Llocal/lito381/server/ClientSession;Llocal/lito381/server/NativeSelfCastAction$Task;)V: left=28:[124:java/lang/Throwable  ] right=28:[120:java/lang/Throwable  ]; paths rejected: path limit 4096 exceeded; labelled CFGs differ"
    },
    {
      "method": "deferScoped(Llocal/lito381/server/NativeSelfCastAction$Task;)V",
      "status": "DIFF",
      "detail": "method deferScoped(Llocal/lito381/server/NativeSelfCastAction$Task;)V insn[20]: left=INVOKEDYNAMIC run(Llocal/lito381/server/NativeLightAction$Batch;Llocal/lito381/server/NativeSelfCastAction$Task;)Llocal/lito381/server/NativeLightAction$Output; bsm=H6 java/lang/invoke/LambdaMetafactory.metafactory(Ljava/lang/invoke/MethodH… right=INVOKEDYNAMIC run(Llocal/lito381/server/NativeLightAction$Batch;Llocal/lito381/server/NativeSelfCastAction$Task;)Llocal/lito381/server/NativeLightAction$Output; bsm=H6 java/lang/invoke/LambdaMetafactory.metafactory(Ljava/lang/invoke/MethodH…; path event multisets differ (left=15, right=15); labelled CFGs differ"
    },
    {
      "method": "deferWeaponTargetOutput(Llocal/lito381/server/NativeSelfCastAction$Task;)V",
      "status": "DIFF",
      "detail": "method deferWeaponTargetOutput(Llocal/lito381/server/NativeSelfCastAction$Task;)V insn[20]: left=INVOKEDYNAMIC run(Llocal/lito381/server/NativeLightAction$Batch;Llocal/lito381/server/NativeSelfCastAction$Task;)Llocal/lito381/server/NativeLightAction$Output; bsm=H6 java/lang/invoke/LambdaMetafactory.metafactory(Ljava/lang/invoke/MethodH… right=INVOKEDYNAMIC run(Llocal/lito381/server/NativeLightAction$Batch;Llocal/lito381/server/NativeSelfCastAction$Task;)Llocal/lito381/server/NativeLightAction$Output; bsm=H6 java/lang/invoke/LambdaMetafactory.metafactory(Ljava/lang/invoke/MethodH…; path event multisets differ (left=15, right=15); labelled CFGs differ"
    },
    {
      "method": "expire(Llocal/lito381/server/ClientSession;J)I",
      "status": "SAME_PATHS",
      "detail": "path multisets equal (5)"
    },
    {
      "method": "honorPending(Llocal/lito381/server/ClientSession;JJ)Z",
      "status": "SAME_PATHS",
      "detail": "path multisets equal (5)"
    },
    {
      "method": "lambda$commitPair$9(Llocal/lito381/server/NativeLightAction$Ingress;Llocal/lito381/server/ClientSession;Llocal/lito381/server/NativeSelfCastAction$Task;Llocal/lito381/server/NativePlayerCommit$Binding;Llocal/lito381/server/ClientSession;Llocal/lito381/server/NativePlayerVitalCompletions$Receipt;)V",
      "status": "ONLY_LEFT",
      "detail": "only left"
    },
    {
      "method": "storeReady(Llocal/lito381/server/ClientSession;IJ)V",
      "status": "SAME_PATHS",
      "detail": "path multisets equal (5)"
    }
  ],
  "effectsMethods": [],
  "approvedMethods": [],
  "firstDiff": {
    "where": "method",
    "member": "commitPair(Llocal/lito381/server/ClientSession;Llocal/lito381/server/ClientSession;Llocal/lito381/server/NativeSelfCastAction$Task;)V",
    "insnIndex": null,
    "detail": "left=28:[124:java/lang/Throwable  ] right=28:[120:java/lang/Throwable  ]; paths rejected: path limit 4096 exceeded; labelled CFGs differ"
  }
}
```

</details>

<details>
<summary>commitPair：完整正規化差異</summary>

```diff
--- staged/NativeLightAction/commitPair(Llocal/lito381/server/ClientSession;Llocal/lito381/server/ClientSession;Llocal/lito381/server/NativeSelfCastAction$Task;)V
+++ candidate/NativeLightAction/commitPair(Llocal/lito381/server/ClientSession;Llocal/lito381/server/ClientSession;Llocal/lito381/server/NativeSelfCastAction$Task;)V
@@ -5,36 +5,36 @@
 java/io/IOException
 ||||pvis=|pinv=|lvtvis=|lvtinv=

-TRY 28:[124:java/lang/Throwable  ]
-TRY 29:[124:java/lang/Throwable  ]
-TRY 30:[124:java/lang/Throwable  ]
-TRY 35:[124:java/lang/Throwable  ]
-TRY 46:[124:java/lang/Throwable  ]
-TRY 47:[124:java/lang/Throwable  ]
-TRY 58:[124:java/lang/Throwable  ]
-TRY 59:[124:java/lang/Throwable  ]
-TRY 60:[124:java/lang/Throwable  ]
-TRY 70:[73:java/io/UncheckedIOException  , 97:java/lang/Throwable  , 124:java/lang/Throwable  ]
-TRY 71:[73:java/io/UncheckedIOException  , 97:java/lang/Throwable  , 124:java/lang/Throwable  ]
-TRY 75:[97:java/lang/Throwable  , 124:java/lang/Throwable  ]
-TRY 76:[97:java/lang/Throwable  , 124:java/lang/Throwable  ]
-TRY 78:[124:java/lang/Throwable  ]
-TRY 81:[124:java/lang/Throwable  ]
-TRY 82:[124:java/lang/Throwable  ]
-TRY 84:[124:java/lang/Throwable  ]
-TRY 86:[124:java/lang/Throwable  ]
-TRY 90:[124:java/lang/Throwable  ]
-TRY 93:[124:java/lang/Throwable  ]
-TRY 95:[124:java/lang/Throwable  ]
-TRY 99:[124:java/lang/Throwable  ]
-TRY 102:[124:java/lang/Throwable  ]
-TRY 103:[124:java/lang/Throwable  ]
-TRY 105:[124:java/lang/Throwable  ]
-TRY 107:[124:java/lang/Throwable  ]
-TRY 111:[124:java/lang/Throwable  ]
-TRY 114:[124:java/lang/Throwable  ]
-TRY 116:[124:java/lang/Throwable  ]
-TRY 118:[124:java/lang/Throwable  ]
+TRY 28:[120:java/lang/Throwable  ]
+TRY 29:[120:java/lang/Throwable  ]
+TRY 30:[120:java/lang/Throwable  ]
+TRY 35:[120:java/lang/Throwable  ]
+TRY 46:[120:java/lang/Throwable  ]
+TRY 47:[120:java/lang/Throwable  ]
+TRY 58:[120:java/lang/Throwable  ]
+TRY 59:[120:java/lang/Throwable  ]
+TRY 60:[120:java/lang/Throwable  ]
+TRY 70:[73:java/io/UncheckedIOException  , 95:java/lang/Throwable  , 120:java/lang/Throwable  ]
+TRY 71:[73:java/io/UncheckedIOException  , 95:java/lang/Throwable  , 120:java/lang/Throwable  ]
+TRY 75:[95:java/lang/Throwable  , 120:java/lang/Throwable  ]
+TRY 76:[95:java/lang/Throwable  , 120:java/lang/Throwable  ]
+TRY 78:[120:java/lang/Throwable  ]
+TRY 81:[120:java/lang/Throwable  ]
+TRY 82:[120:java/lang/Throwable  ]
+TRY 84:[120:java/lang/Throwable  ]
+TRY 86:[120:java/lang/Throwable  ]
+TRY 90:[120:java/lang/Throwable  ]
+TRY 92:[120:java/lang/Throwable  ]
+TRY 93:[120:java/lang/Throwable  ]
+TRY 97:[120:java/lang/Throwable  ]
+TRY 100:[120:java/lang/Throwable  ]
+TRY 101:[120:java/lang/Throwable  ]
+TRY 103:[120:java/lang/Throwable  ]
+TRY 105:[120:java/lang/Throwable  ]
+TRY 109:[120:java/lang/Throwable  ]
+TRY 111:[120:java/lang/Throwable  ]
+TRY 112:[120:java/lang/Throwable  ]
+TRY 114:[120:java/lang/Throwable  ]
 ALOAD 0
 ALOAD 1
 ALOAD 2
@@ -101,11 +101,11 @@
 ALOAD 1
 ALOAD 8
 ALOAD 0
-ALOAD 3
 ALOAD 4
 ALOAD 1
 ALOAD 5
-INVOKEDYNAMIC run(Llocal/lito381/server/NativeLightAction$Ingress;Llocal/lito381/server/ClientSession;Llocal/lito381/server/NativeSelfCastAction$Task;Llocal/lito381/server/NativePlayerCommit$Binding;Llocal/lito381/server/ClientSession;Llocal/lito381/server/NativePlayerVitalCompletions$Receipt;)Ljava/lang/Runnable; bsm=H6 java/lang/invoke/LambdaMetafactory.metafactory(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodHandle;Ljava/lang/invoke/MethodType;)Ljava/lang/invoke/CallSite; args=[type:()V, handle:H6 local/lito381/server/NativeLightAction.lambda$content$af63fe6e68cfd3899283fefbe1d7b62e0a0f81208c854f8311239d75af7c1a51(Llocal/lito381/server/NativeLightAction$Ingress;Llocal/lito381/server/ClientSession;Llocal/lito381/server/NativeSelfCastAction$Task;Llocal/lito381/server/NativePlayerCommit$Binding;Llocal/lito381/server/ClientSession;Llocal/lito381/server/NativePlayerVitalCompletions$Receipt;)V, type:()V]
+ALOAD 3
+INVOKEDYNAMIC run(Llocal/lito381/server/NativeLightAction$Ingress;Llocal/lito381/server/ClientSession;Llocal/lito381/server/NativePlayerCommit$Binding;Llocal/lito381/server/ClientSession;Llocal/lito381/server/NativePlayerVitalCompletions$Receipt;Llocal/lito381/server/NativeSelfCastAction$Task;)Ljava/lang/Runnable; bsm=H6 java/lang/invoke/LambdaMetafactory.metafactory(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodHandle;Ljava/lang/invoke/MethodType;)Ljava/lang/invoke/CallSite; args=[type:()V, handle:H6 local/lito381/server/NativeLightAction.lambda$content$6b68235d2c8d50e26cae1ba81014eacf934e85f556cc515212e7a3bda2b6efa6(Llocal/lito381/server/NativeLightAction$Ingress;Llocal/lito381/server/ClientSession;Llocal/lito381/server/NativePlayerCommit$Binding;Llocal/lito381/server/ClientSession;Llocal/lito381/server/NativePlayerVitalCompletions$Receipt;Llocal/lito381/server/NativeSelfCastAction$Task;)V, type:()V]
 INVOKESTATIC local/lito381/server/NativePairedCastSafety.run(Llocal/lito381/server/ClientSession;Llocal/lito381/server/ClientSession;Ljava/lang/Runnable;)V
 GOTO L77
 ASTORE 10
@@ -123,47 +123,43 @@
 ALOAD 6
 INVOKEVIRTUAL java/lang/ThreadLocal.set(Ljava/lang/Object;)V
 ILOAD 7
-IFEQ L119
+IFEQ L115
 ALOAD 8
 GETFIELD local/lito381/server/NativeLightAction$Ingress.receipt:Llocal/lito381/server/NativePlayerVitalCompletions$Receipt;
-ASTORE 11
 ALOAD 8
 INVOKESTATIC local/lito381/server/NativeSourceSelfFinalFrame.retire(Ljava/lang/Object;)V
-ALOAD 11
 INVOKEVIRTUAL local/lito381/server/NativePlayerVitalCompletions$Receipt.close()V
-GOTO L119
-ASTORE 12
+GOTO L115
+ASTORE 11
 ALOAD 9
 INVOKESTATIC local/lito381/server/NativeCastTimeline.restore(Llocal/lito381/server/NativeCastTimeline$Arrival;)V
 ALOAD 6
-IFNONNULL L105
+IFNONNULL L103
 GETSTATIC local/lito381/server/NativeLightAction.ingress:Ljava/lang/ThreadLocal;
 INVOKEVIRTUAL java/lang/ThreadLocal.remove()V
-GOTO L108
+GOTO L106
 GETSTATIC local/lito381/server/NativeLightAction.ingress:Ljava/lang/ThreadLocal;
 ALOAD 6
 INVOKEVIRTUAL java/lang/ThreadLocal.set(Ljava/lang/Object;)V
 ILOAD 7
-IFEQ L117
+IFEQ L113
 ALOAD 8
 GETFIELD local/lito381/server/NativeLightAction$Ingress.receipt:Llocal/lito381/server/NativePlayerVitalCompletions$Receipt;
-ASTORE 13
 ALOAD 8
 INVOKESTATIC local/lito381/server/NativeSourceSelfFinalFrame.retire(Ljava/lang/Object;)V
-ALOAD 13
+INVOKEVIRTUAL local/lito381/server/NativePlayerVitalCompletions$Receipt.close()V
+ALOAD 11
+ATHROW
+ALOAD 5
+IFNULL L127
+ALOAD 5
+INVOKEVIRTUAL local/lito381/server/NativePlayerVitalCompletions$Receipt.close()V
+GOTO L127
+ASTORE 12
+ALOAD 5
+IFNULL L125
+ALOAD 5
 INVOKEVIRTUAL local/lito381/server/NativePlayerVitalCompletions$Receipt.close()V
 ALOAD 12
 ATHROW
-ALOAD 5
-IFNULL L131
-ALOAD 5
-INVOKEVIRTUAL local/lito381/server/NativePlayerVitalCompletions$Receipt.close()V
-GOTO L131
-ASTORE 14
-ALOAD 5
-IFNULL L129
-ALOAD 5
-INVOKEVIRTUAL local/lito381/server/NativePlayerVitalCompletions$Receipt.close()V
-ALOAD 14
-ATHROW
 RETURN
```

</details>

<details>
<summary>deferScoped：完整正規化差異</summary>

```diff
--- staged/NativeLightAction/deferScoped(Llocal/lito381/server/NativeSelfCastAction$Task;)V
+++ candidate/NativeLightAction/deferScoped(Llocal/lito381/server/NativeSelfCastAction$Task;)V
@@ -26,7 +26,7 @@
 GETFIELD local/lito381/server/NativeLightAction$Batch.outputs:Ljava/util/List;
 ALOAD 1
 ALOAD 0
-INVOKEDYNAMIC run(Llocal/lito381/server/NativeLightAction$Batch;Llocal/lito381/server/NativeSelfCastAction$Task;)Llocal/lito381/server/NativeLightAction$Output; bsm=H6 java/lang/invoke/LambdaMetafactory.metafactory(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodHandle;Ljava/lang/invoke/MethodType;)Ljava/lang/invoke/CallSite; args=[type:()V, handle:H6 local/lito381/server/NativeLightAction.lambda$content$b93037e7b70cc60f32950364f5b80c1fc077fc616c9138dff25da83806318b36(Llocal/lito381/server/NativeLightAction$Batch;Llocal/lito381/server/NativeSelfCastAction$Task;)V, type:()V]
+INVOKEDYNAMIC run(Llocal/lito381/server/NativeLightAction$Batch;Llocal/lito381/server/NativeSelfCastAction$Task;)Llocal/lito381/server/NativeLightAction$Output; bsm=H6 java/lang/invoke/LambdaMetafactory.metafactory(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodHandle;Ljava/lang/invoke/MethodType;)Ljava/lang/invoke/CallSite; args=[type:()V, handle:H6 local/lito381/server/NativeLightAction.lambda$content$feb21681a7a728f6083a06149f5c2fd605497b164c3f5581edc8c1a2fe120f3c(Llocal/lito381/server/NativeLightAction$Batch;Llocal/lito381/server/NativeSelfCastAction$Task;)V, type:()V]
 INVOKEINTERFACE java/util/List.add(Ljava/lang/Object;)Z itf
 POP
 RETURN
```

</details>

<details>
<summary>deferWeaponTargetOutput：完整正規化差異</summary>

```diff
--- staged/NativeLightAction/deferWeaponTargetOutput(Llocal/lito381/server/NativeSelfCastAction$Task;)V
+++ candidate/NativeLightAction/deferWeaponTargetOutput(Llocal/lito381/server/NativeSelfCastAction$Task;)V
@@ -26,7 +26,7 @@
 GETFIELD local/lito381/server/NativeLightAction$Batch.outputs:Ljava/util/List;
 ALOAD 1
 ALOAD 0
-INVOKEDYNAMIC run(Llocal/lito381/server/NativeLightAction$Batch;Llocal/lito381/server/NativeSelfCastAction$Task;)Llocal/lito381/server/NativeLightAction$Output; bsm=H6 java/lang/invoke/LambdaMetafactory.metafactory(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodHandle;Ljava/lang/invoke/MethodType;)Ljava/lang/invoke/CallSite; args=[type:()V, handle:H6 local/lito381/server/NativeLightAction.lambda$content$b93037e7b70cc60f32950364f5b80c1fc077fc616c9138dff25da83806318b36(Llocal/lito381/server/NativeLightAction$Batch;Llocal/lito381/server/NativeSelfCastAction$Task;)V, type:()V]
+INVOKEDYNAMIC run(Llocal/lito381/server/NativeLightAction$Batch;Llocal/lito381/server/NativeSelfCastAction$Task;)Llocal/lito381/server/NativeLightAction$Output; bsm=H6 java/lang/invoke/LambdaMetafactory.metafactory(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodHandle;Ljava/lang/invoke/MethodType;)Ljava/lang/invoke/CallSite; args=[type:()V, handle:H6 local/lito381/server/NativeLightAction.lambda$content$feb21681a7a728f6083a06149f5c2fd605497b164c3f5581edc8c1a2fe120f3c(Llocal/lito381/server/NativeLightAction$Batch;Llocal/lito381/server/NativeSelfCastAction$Task;)V, type:()V]
 INVOKEINTERFACE java/util/List.add(Ljava/lang/Object;)Z itf
 POP
 RETURN
```

</details>

### NativeLightAction$Batch

<details>
<summary>本輪全部方法診斷</summary>

```json
{
  "name": "local/lito381/server/NativeLightAction$Batch",
  "status": "DIFF",
  "byteSame": false,
  "allowMethodsApplied": false,
  "methodResults": [
    {
      "method": "emit()V",
      "status": "DIFF",
      "detail": "method emit()V: left=1:[3:java/lang/Throwable  ] right=1:[6:java/lang/Throwable  ]; path event multisets differ (left=6, right=4); labelled CFGs differ"
    },
    {
      "method": "nativeSourceBatchOriginalEmit()V",
      "status": "SAME_CFG",
      "detail": "entry-rooted labelled CFGs equal"
    }
  ],
  "effectsMethods": [],
  "approvedMethods": [],
  "firstDiff": {
    "where": "method",
    "member": "emit()V",
    "insnIndex": null,
    "detail": "left=1:[3:java/lang/Throwable  ] right=1:[6:java/lang/Throwable  ]; path event multisets differ (left=6, right=4); labelled CFGs differ"
  }
}
```

</details>

<details>
<summary>emit：完整正規化差異</summary>

```diff
--- staged/NativeLightAction$Batch/emit()V
+++ candidate/NativeLightAction$Batch/emit()V
@@ -5,27 +5,16 @@

 ||||pvis=|pinv=|lvtvis=|lvtinv=

-TRY 1:[3:java/lang/Throwable  ]
-TRY 6:[8:java/lang/Throwable  ]
-TRY 15:[17:java/lang/Throwable  ]
+TRY 1:[6:java/lang/Throwable  ]
 ALOAD 0
 INVOKESPECIAL local/lito381/server/NativeLightAction$Batch.nativeSourceBatchOriginalEmit()V
-GOTO L13
+ALOAD 0
+CONST null
+INVOKESTATIC local/lito381/server/NativeSourceTargetIconBatch.finish(Ljava/lang/Object;Ljava/lang/Throwable;)V
+RETURN
 ASTORE 1
 ALOAD 0
 ALOAD 1
 INVOKESTATIC local/lito381/server/NativeSourceTargetIconBatch.finish(Ljava/lang/Object;Ljava/lang/Throwable;)V
-GOTO L11
-ASTORE 2
-ALOAD 2
-INVOKESTATIC local/lito381/server/NativeLightAction$Batch.rethrowCandidateFailure(Ljava/lang/Throwable;)V
 ALOAD 1
 ATHROW
-ALOAD 0
-CONST null
-INVOKESTATIC local/lito381/server/NativeSourceTargetIconBatch.finish(Ljava/lang/Object;Ljava/lang/Throwable;)V
-GOTO L20
-ASTORE 3
-ALOAD 3
-INVOKESTATIC local/lito381/server/NativeLightAction$Batch.rethrowCandidateFailure(Ljava/lang/Throwable;)V
-RETURN
```

</details>

### NativePlayerCommit

<details>
<summary>本輪全部方法診斷</summary>

```json
{
  "name": "local/lito381/server/NativePlayerCommit",
  "status": "DIFF",
  "byteSame": false,
  "allowMethodsApplied": false,
  "methodResults": [
    {
      "method": "acceptedTail(Llocal/lito381/server/NativePlayerCommit$Frame;)V",
      "status": "DIFF",
      "detail": "method acceptedTail(Llocal/lito381/server/NativePlayerCommit$Frame;)V: left=23:[133:local/lito381/server/NativeAcceptedState$Stale  , 191:java/lang/Throwable  ] right=23:[124:local/lito381/server/NativeAcceptedState$Stale  , 173:java/lang/Throwable  ]; paths rejected: path limit 4096 exceeded; labelled CFGs differ"
    },
    {
      "method": "assign(Llocal/lito381/server/ClientSession;Ljava/lang/Object;Ljava/lang/String;)V",
      "status": "SAME_PATHS",
      "detail": "path multisets equal (18)"
    },
    {
      "method": "captureBattleDefense(Llocal/lito381/server/ClientSession;IZLjava/lang/String;)I",
      "status": "SAME_PATHS",
      "detail": "path multisets equal (96)"
    },
    {
      "method": "captureWorldDefense(Llocal/lito381/server/ClientSession;IZLjava/lang/String;DLlocal/lito381/server/BattleAbilityStatus$IllusionAvatar;)I",
      "status": "SAME_PATHS",
      "detail": "path multisets equal (386)"
    },
    {
      "method": "drain(Llocal/lito381/server/ClientSession;Llocal/lito381/server/NativePlayerCommit$Queue;)V",
      "status": "DIFF",
      "detail": "method drain(Llocal/lito381/server/ClientSession;Llocal/lito381/server/NativePlayerCommit$Queue;)V: left=10:[259:java/lang/Throwable  ] right=10:[248:java/lang/Throwable  ]; paths rejected: normal control-flow cycle; labelled CFGs differ"
    },
    {
      "method": "drawDefense(Llocal/lito381/server/ClientSession;Ljava/lang/String;)Llocal/lito381/server/NativeMagicDollEffects$DefenseProc;",
      "status": "SAME_CFG",
      "detail": "entry-rooted labelled CFGs equal"
    },
    {
      "method": "nativeCompanionSceneAssign(Llocal/lito381/server/ClientSession;Ljava/lang/Object;Ljava/lang/String;)V",
      "status": "SAME_PATHS",
      "detail": "path multisets equal (18)"
    },
    {
      "method": "nativeCompanionSceneOriginalAdvance(Llocal/lito381/server/ClientSession;Ljava/lang/Object;)V",
      "status": "SAME_PATHS",
      "detail": "path multisets equal (42)"
    },
    {
      "method": "nativeManaTransferRecover(Llocal/lito381/server/NativePlayerVitalCompletions$Receipt;Llocal/lito381/server/ClientSession;Llocal/lito381/server/NativePlayerCommit$Binding;IILjava/lang/String;)V",
      "status": "DIFF",
      "detail": "method nativeManaTransferRecover(Llocal/lito381/server/NativePlayerVitalCompletions$Receipt;Llocal/lito381/server/ClientSession;Llocal/lito381/server/NativePlayerCommit$Binding;IILjava/lang/String;)V: left=30:[144:java/lang/Throwable  ] right=32:[146:java/lang/Throwable  ]; paths rejected: path limit 4096 exceeded; labelled CFGs differ"
    },
    {
      "method": "nativeSourceProc68Queue(Llocal/lito381/server/ClientSession;Llocal/lito381/server/NativePlayerCommit$Binding;Llocal/lito381/server/BattleTimedStatus;Llocal/lito381/server/NativeSourceEquipmentProcLife$Proof;Llocal/lito381/server/NativeProcSkillCatalog$Catalog;Llocal/lito381/server/BattleAbilityStatus;)Llocal/lito381/server/NativePlayerCommit$Queue;",
      "status": "SAME_PATHS",
      "detail": "path multisets equal (47)"
    },
    {
      "method": "nativeSourceScalarQueue(Llocal/lito381/server/ClientSession;Llocal/lito381/server/NativePlayerCommit$Binding;Llocal/lito381/server/BattleTimedStatus;Llocal/lito381/server/NativeSourceEquipmentProcLife$Proof;Llocal/lito381/server/NativeProcSkillCatalog$Catalog;)Llocal/lito381/server/NativePlayerCommit$Queue;",
      "status": "SAME_PATHS",
      "detail": "path multisets equal (47)"
    },
    {
      "method": "nativeStaticOriginalAdvance(Llocal/lito381/server/ClientSession;)V",
      "status": "SAME_PATHS",
      "detail": "path multisets equal (48)"
    },
    {
      "method": "publishDefense(Llocal/lito381/server/ClientSession;)V",
      "status": "SAME_CFG",
      "detail": "entry-rooted labelled CFGs equal"
    },
    {
      "method": "selected(Llocal/lito381/server/ClientSession;Ljava/lang/String;)V",
      "status": "SAME_PATHS",
      "detail": "path multisets equal (14)"
    },
    {
      "method": "worldPlayer(Llocal/lito381/server/ClientSession;ILlocal/lito381/server/ClientSession;ZZ)V",
      "status": "SAME_PATHS",
      "detail": "path multisets equal (210)"
    }
  ],
  "effectsMethods": [],
  "approvedMethods": [],
  "firstDiff": {
    "where": "method",
    "member": "acceptedTail(Llocal/lito381/server/NativePlayerCommit$Frame;)V",
    "insnIndex": null,
    "detail": "left=23:[133:local/lito381/server/NativeAcceptedState$Stale  , 191:java/lang/Throwable  ] right=23:[124:local/lito381/server/NativeAcceptedState$Stale  , 173:java/lang/Throwable  ]; paths rejected: path limit 4096 exceeded; labelled CFGs differ"
  }
}
```

</details>

<details>
<summary>acceptedTail：完整正規化差異</summary>

```diff
--- staged/NativePlayerCommit/acceptedTail(Llocal/lito381/server/NativePlayerCommit$Frame;)V
+++ candidate/NativePlayerCommit/acceptedTail(Llocal/lito381/server/NativePlayerCommit$Frame;)V
@@ -5,59 +5,59 @@

 ||||pvis=|pinv=|lvtvis=|lvtinv=

-TRY 23:[133:local/lito381/server/NativeAcceptedState$Stale  , 191:java/lang/Throwable  ]
-TRY 28:[133:local/lito381/server/NativeAcceptedState$Stale  , 191:java/lang/Throwable  ]
-TRY 34:[133:local/lito381/server/NativeAcceptedState$Stale  , 191:java/lang/Throwable  ]
-TRY 38:[133:local/lito381/server/NativeAcceptedState$Stale  , 191:java/lang/Throwable  ]
-TRY 39:[133:local/lito381/server/NativeAcceptedState$Stale  , 191:java/lang/Throwable  ]
-TRY 40:[133:local/lito381/server/NativeAcceptedState$Stale  , 191:java/lang/Throwable  ]
-TRY 44:[133:local/lito381/server/NativeAcceptedState$Stale  , 191:java/lang/Throwable  ]
-TRY 48:[133:local/lito381/server/NativeAcceptedState$Stale  , 191:java/lang/Throwable  ]
-TRY 49:[133:local/lito381/server/NativeAcceptedState$Stale  , 191:java/lang/Throwable  ]
-TRY 50:[133:local/lito381/server/NativeAcceptedState$Stale  , 191:java/lang/Throwable  ]
-TRY 54:[133:local/lito381/server/NativeAcceptedState$Stale  , 191:java/lang/Throwable  ]
-TRY 55:[133:local/lito381/server/NativeAcceptedState$Stale  , 191:java/lang/Throwable  ]
-TRY 56:[133:local/lito381/server/NativeAcceptedState$Stale  , 191:java/lang/Throwable  ]
-TRY 60:[133:local/lito381/server/NativeAcceptedState$Stale  , 191:java/lang/Throwable  ]
-TRY 61:[133:local/lito381/server/NativeAcceptedState$Stale  , 191:java/lang/Throwable  ]
-TRY 62:[133:local/lito381/server/NativeAcceptedState$Stale  , 191:java/lang/Throwable  ]
-TRY 66:[133:local/lito381/server/NativeAcceptedState$Stale  , 191:java/lang/Throwable  ]
-TRY 67:[133:local/lito381/server/NativeAcceptedState$Stale  , 191:java/lang/Throwable  ]
-TRY 71:[133:local/lito381/server/NativeAcceptedState$Stale  , 191:java/lang/Throwable  ]
-TRY 72:[133:local/lito381/server/NativeAcceptedState$Stale  , 191:java/lang/Throwable  ]
-TRY 73:[133:local/lito381/server/NativeAcceptedState$Stale  , 191:java/lang/Throwable  ]
-TRY 74:[133:local/lito381/server/NativeAcceptedState$Stale  , 191:java/lang/Throwable  ]
-TRY 78:[133:local/lito381/server/NativeAcceptedState$Stale  , 191:java/lang/Throwable  ]
-TRY 79:[133:local/lito381/server/NativeAcceptedState$Stale  , 191:java/lang/Throwable  ]
-TRY 93:[103:java/lang/Throwable  , 121:java/lang/Throwable  ]
-TRY 95:[103:java/lang/Throwable  , 121:java/lang/Throwable  ]
-TRY 98:[121:java/lang/Throwable  ]
-TRY 100:[121:java/lang/Throwable  ]
-TRY 101:[121:java/lang/Throwable  ]
-TRY 106:[121:java/lang/Throwable  ]
-TRY 108:[121:java/lang/Throwable  ]
-TRY 109:[121:java/lang/Throwable  ]
-TRY 111:[121:java/lang/Throwable  ]
-TRY 136:[191:java/lang/Throwable  ]
-TRY 137:[191:java/lang/Throwable  ]
-TRY 151:[161:java/lang/Throwable  , 179:java/lang/Throwable  ]
-TRY 153:[161:java/lang/Throwable  , 179:java/lang/Throwable  ]
-TRY 156:[179:java/lang/Throwable  ]
-TRY 158:[179:java/lang/Throwable  ]
-TRY 159:[179:java/lang/Throwable  ]
-TRY 164:[179:java/lang/Throwable  ]
-TRY 166:[179:java/lang/Throwable  ]
-TRY 167:[179:java/lang/Throwable  ]
-TRY 169:[179:java/lang/Throwable  ]
-TRY 204:[214:java/lang/Throwable  , 232:java/lang/Throwable  ]
-TRY 206:[214:java/lang/Throwable  , 232:java/lang/Throwable  ]
-TRY 209:[232:java/lang/Throwable  ]
-TRY 211:[232:java/lang/Throwable  ]
-TRY 212:[232:java/lang/Throwable  ]
-TRY 217:[232:java/lang/Throwable  ]
-TRY 219:[232:java/lang/Throwable  ]
-TRY 220:[232:java/lang/Throwable  ]
-TRY 222:[232:java/lang/Throwable  ]
+TRY 23:[124:local/lito381/server/NativeAcceptedState$Stale  , 173:java/lang/Throwable  ]
+TRY 28:[124:local/lito381/server/NativeAcceptedState$Stale  , 173:java/lang/Throwable  ]
+TRY 34:[124:local/lito381/server/NativeAcceptedState$Stale  , 173:java/lang/Throwable  ]
+TRY 38:[124:local/lito381/server/NativeAcceptedState$Stale  , 173:java/lang/Throwable  ]
+TRY 39:[124:local/lito381/server/NativeAcceptedState$Stale  , 173:java/lang/Throwable  ]
+TRY 40:[124:local/lito381/server/NativeAcceptedState$Stale  , 173:java/lang/Throwable  ]
+TRY 44:[124:local/lito381/server/NativeAcceptedState$Stale  , 173:java/lang/Throwable  ]
+TRY 48:[124:local/lito381/server/NativeAcceptedState$Stale  , 173:java/lang/Throwable  ]
+TRY 49:[124:local/lito381/server/NativeAcceptedState$Stale  , 173:java/lang/Throwable  ]
+TRY 50:[124:local/lito381/server/NativeAcceptedState$Stale  , 173:java/lang/Throwable  ]
+TRY 54:[124:local/lito381/server/NativeAcceptedState$Stale  , 173:java/lang/Throwable  ]
+TRY 55:[124:local/lito381/server/NativeAcceptedState$Stale  , 173:java/lang/Throwable  ]
+TRY 56:[124:local/lito381/server/NativeAcceptedState$Stale  , 173:java/lang/Throwable  ]
+TRY 60:[124:local/lito381/server/NativeAcceptedState$Stale  , 173:java/lang/Throwable  ]
+TRY 61:[124:local/lito381/server/NativeAcceptedState$Stale  , 173:java/lang/Throwable  ]
+TRY 62:[124:local/lito381/server/NativeAcceptedState$Stale  , 173:java/lang/Throwable  ]
+TRY 66:[124:local/lito381/server/NativeAcceptedState$Stale  , 173:java/lang/Throwable  ]
+TRY 67:[124:local/lito381/server/NativeAcceptedState$Stale  , 173:java/lang/Throwable  ]
+TRY 71:[124:local/lito381/server/NativeAcceptedState$Stale  , 173:java/lang/Throwable  ]
+TRY 72:[124:local/lito381/server/NativeAcceptedState$Stale  , 173:java/lang/Throwable  ]
+TRY 73:[124:local/lito381/server/NativeAcceptedState$Stale  , 173:java/lang/Throwable  ]
+TRY 74:[124:local/lito381/server/NativeAcceptedState$Stale  , 173:java/lang/Throwable  ]
+TRY 78:[124:local/lito381/server/NativeAcceptedState$Stale  , 173:java/lang/Throwable  ]
+TRY 79:[124:local/lito381/server/NativeAcceptedState$Stale  , 173:java/lang/Throwable  ]
+TRY 84:[94:java/lang/Throwable  , 112:java/lang/Throwable  ]
+TRY 86:[94:java/lang/Throwable  , 112:java/lang/Throwable  ]
+TRY 89:[112:java/lang/Throwable  ]
+TRY 91:[112:java/lang/Throwable  ]
+TRY 92:[112:java/lang/Throwable  ]
+TRY 97:[112:java/lang/Throwable  ]
+TRY 99:[112:java/lang/Throwable  ]
+TRY 100:[112:java/lang/Throwable  ]
+TRY 102:[112:java/lang/Throwable  ]
+TRY 127:[173:java/lang/Throwable  ]
+TRY 128:[173:java/lang/Throwable  ]
+TRY 133:[143:java/lang/Throwable  , 161:java/lang/Throwable  ]
+TRY 135:[143:java/lang/Throwable  , 161:java/lang/Throwable  ]
+TRY 138:[161:java/lang/Throwable  ]
+TRY 140:[161:java/lang/Throwable  ]
+TRY 141:[161:java/lang/Throwable  ]
+TRY 146:[161:java/lang/Throwable  ]
+TRY 148:[161:java/lang/Throwable  ]
+TRY 149:[161:java/lang/Throwable  ]
+TRY 151:[161:java/lang/Throwable  ]
+TRY 177:[187:java/lang/Throwable  , 205:java/lang/Throwable  ]
+TRY 179:[187:java/lang/Throwable  , 205:java/lang/Throwable  ]
+TRY 182:[205:java/lang/Throwable  ]
+TRY 184:[205:java/lang/Throwable  ]
+TRY 185:[205:java/lang/Throwable  ]
+TRY 190:[205:java/lang/Throwable  ]
+TRY 192:[205:java/lang/Throwable  ]
+TRY 193:[205:java/lang/Throwable  ]
+TRY 195:[205:java/lang/Throwable  ]
 GETSTATIC local/lito381/server/NativePlayerCommit.current:Ljava/lang/ThreadLocal;
 INVOKEVIRTUAL java/lang/ThreadLocal.get()Ljava/lang/Object;
 CHECKCAST local/lito381/server/NativePlayerCommit$Frame
@@ -140,16 +140,7 @@
 INVOKEVIRTUAL java/util/concurrent/atomic/AtomicLong.incrementAndGet()J
 POP2
 ILOAD 6
-IFNE L92
-ALOAD 1
-IFNONNULL L88
-GETSTATIC local/lito381/server/NativePlayerCommit.current:Ljava/lang/ThreadLocal;
-INVOKEVIRTUAL java/lang/ThreadLocal.remove()V
-GOTO L245
-GETSTATIC local/lito381/server/NativePlayerCommit.current:Ljava/lang/ThreadLocal;
-ALOAD 1
-INVOKEVIRTUAL java/lang/ThreadLocal.set(Ljava/lang/Object;)V
-GOTO L245
+IFEQ L103
 ALOAD 2
 INVOKESTATIC local/lito381/server/NativePlayerCommit.publishDefense(Llocal/lito381/server/ClientSession;)V
 ALOAD 0
@@ -160,7 +151,7 @@
 ALOAD 5
 GETFIELD local/lito381/server/NativePlayerCommit$Event.npcFeedback:Llocal/lito381/server/NativeCombatAttacker$Feedback;
 INVOKESTATIC local/lito381/server/NativeCombatAttacker.acceptedFeedback(Llocal/lito381/server/ClientSession;Llocal/lito381/server/NativeCombatAttacker$Ref;Llocal/lito381/server/NativeCombatAttacker$Feedback;)V
-GOTO L112
+GOTO L103
 ASTORE 7
 ALOAD 2
 ALOAD 5
@@ -171,43 +162,34 @@
 ALOAD 7
 ATHROW
 ALOAD 1
-IFNONNULL L117
-GETSTATIC local/lito381/server/NativePlayerCommit.current:Ljava/lang/ThreadLocal;
-INVOKEVIRTUAL java/lang/ThreadLocal.remove()V
-GOTO L132
-GETSTATIC local/lito381/server/NativePlayerCommit.current:Ljava/lang/ThreadLocal;
-ALOAD 1
-INVOKEVIRTUAL java/lang/ThreadLocal.set(Ljava/lang/Object;)V
-GOTO L132
+IFNONNULL L108
+GETSTATIC local/lito381/server/NativePlayerCommit.current:Ljava/lang/ThreadLocal;
+INVOKEVIRTUAL java/lang/ThreadLocal.remove()V
+GOTO L123
+GETSTATIC local/lito381/server/NativePlayerCommit.current:Ljava/lang/ThreadLocal;
+ALOAD 1
+INVOKEVIRTUAL java/lang/ThreadLocal.set(Ljava/lang/Object;)V
+GOTO L123
 ASTORE 8
 ALOAD 1
-IFNONNULL L127
-GETSTATIC local/lito381/server/NativePlayerCommit.current:Ljava/lang/ThreadLocal;
-INVOKEVIRTUAL java/lang/ThreadLocal.remove()V
-GOTO L130
+IFNONNULL L118
+GETSTATIC local/lito381/server/NativePlayerCommit.current:Ljava/lang/ThreadLocal;
+INVOKEVIRTUAL java/lang/ThreadLocal.remove()V
+GOTO L121
 GETSTATIC local/lito381/server/NativePlayerCommit.current:Ljava/lang/ThreadLocal;
 ALOAD 1
 INVOKEVIRTUAL java/lang/ThreadLocal.set(Ljava/lang/Object;)V
 ALOAD 8
 ATHROW
-GOTO L245
+GOTO L218
 ASTORE 9
 ILOAD 6
-IFEQ L139
+IFEQ L130
 GETSTATIC local/lito381/server/NativePlayerCommit.committed:Ljava/util/concurrent/atomic/AtomicLong;
 INVOKEVIRTUAL java/util/concurrent/atomic/AtomicLong.incrementAndGet()J
 POP2
 ILOAD 6
-IFNE L150
-ALOAD 1
-IFNONNULL L146
-GETSTATIC local/lito381/server/NativePlayerCommit.current:Ljava/lang/ThreadLocal;
-INVOKEVIRTUAL java/lang/ThreadLocal.remove()V
-GOTO L245
-GETSTATIC local/lito381/server/NativePlayerCommit.current:Ljava/lang/ThreadLocal;
-ALOAD 1
-INVOKEVIRTUAL java/lang/ThreadLocal.set(Ljava/lang/Object;)V
-GOTO L245
+IFEQ L152
 ALOAD 2
 INVOKESTATIC local/lito381/server/NativePlayerCommit.publishDefense(Llocal/lito381/server/ClientSession;)V
 ALOAD 0
@@ -218,49 +200,40 @@
 ALOAD 5
 GETFIELD local/lito381/server/NativePlayerCommit$Event.npcFeedback:Llocal/lito381/server/NativeCombatAttacker$Feedback;
 INVOKESTATIC local/lito381/server/NativeCombatAttacker.acceptedFeedback(Llocal/lito381/server/ClientSession;Llocal/lito381/server/NativeCombatAttacker$Ref;Llocal/lito381/server/NativeCombatAttacker$Feedback;)V
+GOTO L152
+ASTORE 10
+ALOAD 2
+ALOAD 5
+GETFIELD local/lito381/server/NativePlayerCommit$Event.attacker:Llocal/lito381/server/NativeCombatAttacker$Ref;
+ALOAD 5
+GETFIELD local/lito381/server/NativePlayerCommit$Event.npcFeedback:Llocal/lito381/server/NativeCombatAttacker$Feedback;
+INVOKESTATIC local/lito381/server/NativeCombatAttacker.acceptedFeedback(Llocal/lito381/server/ClientSession;Llocal/lito381/server/NativeCombatAttacker$Ref;Llocal/lito381/server/NativeCombatAttacker$Feedback;)V
+ALOAD 10
+ATHROW
+ALOAD 1
+IFNONNULL L157
+GETSTATIC local/lito381/server/NativePlayerCommit.current:Ljava/lang/ThreadLocal;
+INVOKEVIRTUAL java/lang/ThreadLocal.remove()V
+GOTO L172
+GETSTATIC local/lito381/server/NativePlayerCommit.current:Ljava/lang/ThreadLocal;
+ALOAD 1
+INVOKEVIRTUAL java/lang/ThreadLocal.set(Ljava/lang/Object;)V
+GOTO L172
+ASTORE 11
+ALOAD 1
+IFNONNULL L167
+GETSTATIC local/lito381/server/NativePlayerCommit.current:Ljava/lang/ThreadLocal;
+INVOKEVIRTUAL java/lang/ThreadLocal.remove()V
 GOTO L170
-ASTORE 10
-ALOAD 2
-ALOAD 5
-GETFIELD local/lito381/server/NativePlayerCommit$Event.attacker:Llocal/lito381/server/NativeCombatAttacker$Ref;
-ALOAD 5
-GETFIELD local/lito381/server/NativePlayerCommit$Event.npcFeedback:Llocal/lito381/server/NativeCombatAttacker$Feedback;
-INVOKESTATIC local/lito381/server/NativeCombatAttacker.acceptedFeedback(Llocal/lito381/server/ClientSession;Llocal/lito381/server/NativeCombatAttacker$Ref;Llocal/lito381/server/NativeCombatAttacker$Feedback;)V
-ALOAD 10
-ATHROW
-ALOAD 1
-IFNONNULL L175
-GETSTATIC local/lito381/server/NativePlayerCommit.current:Ljava/lang/ThreadLocal;
-INVOKEVIRTUAL java/lang/ThreadLocal.remove()V
-GOTO L190
-GETSTATIC local/lito381/server/NativePlayerCommit.current:Ljava/lang/ThreadLocal;
-ALOAD 1
-INVOKEVIRTUAL java/lang/ThreadLocal.set(Ljava/lang/Object;)V
-GOTO L190
-ASTORE 11
-ALOAD 1
-IFNONNULL L185
-GETSTATIC local/lito381/server/NativePlayerCommit.current:Ljava/lang/ThreadLocal;
-INVOKEVIRTUAL java/lang/ThreadLocal.remove()V
-GOTO L188
 GETSTATIC local/lito381/server/NativePlayerCommit.current:Ljava/lang/ThreadLocal;
 ALOAD 1
 INVOKEVIRTUAL java/lang/ThreadLocal.set(Ljava/lang/Object;)V
 ALOAD 11
 ATHROW
-GOTO L245
+GOTO L218
 ASTORE 12
 ILOAD 6
-IFNE L203
-ALOAD 1
-IFNONNULL L199
-GETSTATIC local/lito381/server/NativePlayerCommit.current:Ljava/lang/ThreadLocal;
-INVOKEVIRTUAL java/lang/ThreadLocal.remove()V
-GOTO L243
-GETSTATIC local/lito381/server/NativePlayerCommit.current:Ljava/lang/ThreadLocal;
-ALOAD 1
-INVOKEVIRTUAL java/lang/ThreadLocal.set(Ljava/lang/Object;)V
-GOTO L243
+IFEQ L196
 ALOAD 2
 INVOKESTATIC local/lito381/server/NativePlayerCommit.publishDefense(Llocal/lito381/server/ClientSession;)V
 ALOAD 0
@@ -271,7 +244,7 @@
 ALOAD 5
 GETFIELD local/lito381/server/NativePlayerCommit$Event.npcFeedback:Llocal/lito381/server/NativeCombatAttacker$Feedback;
 INVOKESTATIC local/lito381/server/NativeCombatAttacker.acceptedFeedback(Llocal/lito381/server/ClientSession;Llocal/lito381/server/NativeCombatAttacker$Ref;Llocal/lito381/server/NativeCombatAttacker$Feedback;)V
-GOTO L223
+GOTO L196
 ASTORE 13
 ALOAD 2
 ALOAD 5
@@ -282,20 +255,20 @@
 ALOAD 13
 ATHROW
 ALOAD 1
-IFNONNULL L228
-GETSTATIC local/lito381/server/NativePlayerCommit.current:Ljava/lang/ThreadLocal;
-INVOKEVIRTUAL java/lang/ThreadLocal.remove()V
-GOTO L243
-GETSTATIC local/lito381/server/NativePlayerCommit.current:Ljava/lang/ThreadLocal;
-ALOAD 1
-INVOKEVIRTUAL java/lang/ThreadLocal.set(Ljava/lang/Object;)V
-GOTO L243
+IFNONNULL L201
+GETSTATIC local/lito381/server/NativePlayerCommit.current:Ljava/lang/ThreadLocal;
+INVOKEVIRTUAL java/lang/ThreadLocal.remove()V
+GOTO L216
+GETSTATIC local/lito381/server/NativePlayerCommit.current:Ljava/lang/ThreadLocal;
+ALOAD 1
+INVOKEVIRTUAL java/lang/ThreadLocal.set(Ljava/lang/Object;)V
+GOTO L216
 ASTORE 14
 ALOAD 1
-IFNONNULL L238
-GETSTATIC local/lito381/server/NativePlayerCommit.current:Ljava/lang/ThreadLocal;
-INVOKEVIRTUAL java/lang/ThreadLocal.remove()V
-GOTO L241
+IFNONNULL L211
+GETSTATIC local/lito381/server/NativePlayerCommit.current:Ljava/lang/ThreadLocal;
+INVOKEVIRTUAL java/lang/ThreadLocal.remove()V
+GOTO L214
 GETSTATIC local/lito381/server/NativePlayerCommit.current:Ljava/lang/ThreadLocal;
 ALOAD 1
 INVOKEVIRTUAL java/lang/ThreadLocal.set(Ljava/lang/Object;)V
```

</details>

<details>
<summary>drain：完整正規化差異</summary>

```diff
--- staged/NativePlayerCommit/drain(Llocal/lito381/server/ClientSession;Llocal/lito381/server/NativePlayerCommit$Queue;)V
+++ candidate/NativePlayerCommit/drain(Llocal/lito381/server/ClientSession;Llocal/lito381/server/NativePlayerCommit$Queue;)V
@@ -5,75 +5,75 @@

 ||||pvis=|pinv=|lvtvis=|lvtinv=

-TRY 10:[259:java/lang/Throwable  ]
-TRY 19:[259:java/lang/Throwable  ]
-TRY 27:[259:java/lang/Throwable  ]
-TRY 41:[229:java/lang/Throwable  , 259:java/lang/Throwable  ]
-TRY 42:[229:java/lang/Throwable  , 259:java/lang/Throwable  ]
-TRY 47:[229:java/lang/Throwable  , 259:java/lang/Throwable  ]
-TRY 50:[229:java/lang/Throwable  , 259:java/lang/Throwable  ]
-TRY 55:[229:java/lang/Throwable  , 259:java/lang/Throwable  ]
-TRY 58:[229:java/lang/Throwable  , 259:java/lang/Throwable  ]
-TRY 61:[229:java/lang/Throwable  , 259:java/lang/Throwable  ]
-TRY 63:[229:java/lang/Throwable  , 259:java/lang/Throwable  ]
-TRY 67:[229:java/lang/Throwable  , 259:java/lang/Throwable  ]
-TRY 72:[229:java/lang/Throwable  , 259:java/lang/Throwable  ]
-TRY 74:[229:java/lang/Throwable  , 259:java/lang/Throwable  ]
-TRY 75:[229:java/lang/Throwable  , 259:java/lang/Throwable  ]
-TRY 77:[229:java/lang/Throwable  , 259:java/lang/Throwable  ]
-TRY 80:[229:java/lang/Throwable  , 259:java/lang/Throwable  ]
-TRY 81:[229:java/lang/Throwable  , 259:java/lang/Throwable  ]
-TRY 86:[208:java/lang/Throwable  , 229:java/lang/Throwable  , 259:java/lang/Throwable  ]
-TRY 91:[208:java/lang/Throwable  , 229:java/lang/Throwable  , 259:java/lang/Throwable  ]
-TRY 94:[208:java/lang/Throwable  , 229:java/lang/Throwable  , 259:java/lang/Throwable  ]
-TRY 97:[208:java/lang/Throwable  , 229:java/lang/Throwable  , 259:java/lang/Throwable  ]
-TRY 99:[208:java/lang/Throwable  , 229:java/lang/Throwable  , 259:java/lang/Throwable  ]
-TRY 103:[208:java/lang/Throwable  , 229:java/lang/Throwable  , 259:java/lang/Throwable  ]
-TRY 104:[208:java/lang/Throwable  , 229:java/lang/Throwable  , 259:java/lang/Throwable  ]
-TRY 106:[208:java/lang/Throwable  , 229:java/lang/Throwable  , 259:java/lang/Throwable  ]
-TRY 111:[208:java/lang/Throwable  , 229:java/lang/Throwable  , 259:java/lang/Throwable  ]
-TRY 117:[208:java/lang/Throwable  , 229:java/lang/Throwable  , 259:java/lang/Throwable  ]
-TRY 121:[208:java/lang/Throwable  , 229:java/lang/Throwable  , 259:java/lang/Throwable  ]
-TRY 125:[208:java/lang/Throwable  , 229:java/lang/Throwable  , 259:java/lang/Throwable  ]
-TRY 135:[208:java/lang/Throwable  , 229:java/lang/Throwable  , 259:java/lang/Throwable  ]
-TRY 136:[208:java/lang/Throwable  , 229:java/lang/Throwable  , 259:java/lang/Throwable  ]
-TRY 137:[208:java/lang/Throwable  , 229:java/lang/Throwable  , 259:java/lang/Throwable  ]
-TRY 140:[229:java/lang/Throwable  , 259:java/lang/Throwable  ]
-TRY 142:[229:java/lang/Throwable  , 259:java/lang/Throwable  ]
-TRY 149:[208:java/lang/Throwable  , 229:java/lang/Throwable  , 259:java/lang/Throwable  ]
-TRY 151:[208:java/lang/Throwable  , 229:java/lang/Throwable  , 259:java/lang/Throwable  ]
-TRY 153:[208:java/lang/Throwable  , 229:java/lang/Throwable  , 259:java/lang/Throwable  ]
-TRY 156:[208:java/lang/Throwable  , 229:java/lang/Throwable  , 259:java/lang/Throwable  ]
-TRY 165:[208:java/lang/Throwable  , 229:java/lang/Throwable  , 259:java/lang/Throwable  ]
-TRY 169:[208:java/lang/Throwable  , 229:java/lang/Throwable  , 259:java/lang/Throwable  ]
-TRY 171:[208:java/lang/Throwable  , 229:java/lang/Throwable  , 259:java/lang/Throwable  ]
-TRY 172:[208:java/lang/Throwable  , 229:java/lang/Throwable  , 259:java/lang/Throwable  ]
-TRY 176:[208:java/lang/Throwable  , 229:java/lang/Throwable  , 259:java/lang/Throwable  ]
-TRY 177:[208:java/lang/Throwable  , 229:java/lang/Throwable  , 259:java/lang/Throwable  ]
-TRY 184:[208:java/lang/Throwable  , 229:java/lang/Throwable  , 259:java/lang/Throwable  ]
-TRY 185:[208:java/lang/Throwable  , 229:java/lang/Throwable  , 259:java/lang/Throwable  ]
-TRY 186:[208:java/lang/Throwable  , 229:java/lang/Throwable  , 259:java/lang/Throwable  ]
-TRY 196:[208:java/lang/Throwable  , 229:java/lang/Throwable  , 259:java/lang/Throwable  ]
-TRY 199:[208:java/lang/Throwable  , 229:java/lang/Throwable  , 259:java/lang/Throwable  ]
-TRY 201:[208:java/lang/Throwable  , 229:java/lang/Throwable  , 259:java/lang/Throwable  ]
-TRY 204:[229:java/lang/Throwable  , 259:java/lang/Throwable  ]
-TRY 206:[229:java/lang/Throwable  , 259:java/lang/Throwable  ]
-TRY 210:[229:java/lang/Throwable  , 259:java/lang/Throwable  ]
-TRY 212:[229:java/lang/Throwable  , 259:java/lang/Throwable  ]
-TRY 214:[229:java/lang/Throwable  , 259:java/lang/Throwable  ]
-TRY 216:[229:java/lang/Throwable  , 259:java/lang/Throwable  ]
-TRY 218:[229:java/lang/Throwable  , 259:java/lang/Throwable  ]
-TRY 231:[259:java/lang/Throwable  ]
-TRY 233:[259:java/lang/Throwable  ]
-TRY 237:[259:java/lang/Throwable  ]
-TRY 238:[259:java/lang/Throwable  ]
-TRY 241:[259:java/lang/Throwable  ]
-TRY 242:[259:java/lang/Throwable  ]
-TRY 244:[259:java/lang/Throwable  ]
-TRY 246:[259:java/lang/Throwable  ]
-TRY 247:[259:java/lang/Throwable  ]
-TRY 248:[259:java/lang/Throwable  ]
-TRY 250:[259:java/lang/Throwable  ]
+TRY 10:[248:java/lang/Throwable  ]
+TRY 19:[248:java/lang/Throwable  ]
+TRY 22:[248:java/lang/Throwable  ]
+TRY 31:[218:java/lang/Throwable  , 248:java/lang/Throwable  ]
+TRY 32:[218:java/lang/Throwable  , 248:java/lang/Throwable  ]
+TRY 37:[218:java/lang/Throwable  , 248:java/lang/Throwable  ]
+TRY 40:[218:java/lang/Throwable  , 248:java/lang/Throwable  ]
+TRY 45:[218:java/lang/Throwable  , 248:java/lang/Throwable  ]
+TRY 48:[218:java/lang/Throwable  , 248:java/lang/Throwable  ]
+TRY 51:[218:java/lang/Throwable  , 248:java/lang/Throwable  ]
+TRY 53:[218:java/lang/Throwable  , 248:java/lang/Throwable  ]
+TRY 57:[218:java/lang/Throwable  , 248:java/lang/Throwable  ]
+TRY 62:[218:java/lang/Throwable  , 248:java/lang/Throwable  ]
+TRY 64:[218:java/lang/Throwable  , 248:java/lang/Throwable  ]
+TRY 65:[218:java/lang/Throwable  , 248:java/lang/Throwable  ]
+TRY 67:[218:java/lang/Throwable  , 248:java/lang/Throwable  ]
+TRY 70:[218:java/lang/Throwable  , 248:java/lang/Throwable  ]
+TRY 71:[218:java/lang/Throwable  , 248:java/lang/Throwable  ]
+TRY 76:[197:java/lang/Throwable  , 218:java/lang/Throwable  , 248:java/lang/Throwable  ]
+TRY 81:[197:java/lang/Throwable  , 218:java/lang/Throwable  , 248:java/lang/Throwable  ]
+TRY 84:[197:java/lang/Throwable  , 218:java/lang/Throwable  , 248:java/lang/Throwable  ]
+TRY 87:[197:java/lang/Throwable  , 218:java/lang/Throwable  , 248:java/lang/Throwable  ]
+TRY 89:[197:java/lang/Throwable  , 218:java/lang/Throwable  , 248:java/lang/Throwable  ]
+TRY 93:[197:java/lang/Throwable  , 218:java/lang/Throwable  , 248:java/lang/Throwable  ]
+TRY 94:[197:java/lang/Throwable  , 218:java/lang/Throwable  , 248:java/lang/Throwable  ]
+TRY 96:[197:java/lang/Throwable  , 218:java/lang/Throwable  , 248:java/lang/Throwable  ]
+TRY 101:[197:java/lang/Throwable  , 218:java/lang/Throwable  , 248:java/lang/Throwable  ]
+TRY 104:[197:java/lang/Throwable  , 218:java/lang/Throwable  , 248:java/lang/Throwable  ]
+TRY 105:[197:java/lang/Throwable  , 218:java/lang/Throwable  , 248:java/lang/Throwable  ]
+TRY 106:[197:java/lang/Throwable  , 218:java/lang/Throwable  , 248:java/lang/Throwable  ]
+TRY 109:[218:java/lang/Throwable  , 248:java/lang/Throwable  ]
+TRY 111:[218:java/lang/Throwable  , 248:java/lang/Throwable  ]
+TRY 117:[197:java/lang/Throwable  , 218:java/lang/Throwable  , 248:java/lang/Throwable  ]
+TRY 121:[197:java/lang/Throwable  , 218:java/lang/Throwable  , 248:java/lang/Throwable  ]
+TRY 125:[197:java/lang/Throwable  , 218:java/lang/Throwable  , 248:java/lang/Throwable  ]
+TRY 138:[197:java/lang/Throwable  , 218:java/lang/Throwable  , 248:java/lang/Throwable  ]
+TRY 140:[197:java/lang/Throwable  , 218:java/lang/Throwable  , 248:java/lang/Throwable  ]
+TRY 142:[197:java/lang/Throwable  , 218:java/lang/Throwable  , 248:java/lang/Throwable  ]
+TRY 145:[197:java/lang/Throwable  , 218:java/lang/Throwable  , 248:java/lang/Throwable  ]
+TRY 154:[197:java/lang/Throwable  , 218:java/lang/Throwable  , 248:java/lang/Throwable  ]
+TRY 158:[197:java/lang/Throwable  , 218:java/lang/Throwable  , 248:java/lang/Throwable  ]
+TRY 160:[197:java/lang/Throwable  , 218:java/lang/Throwable  , 248:java/lang/Throwable  ]
+TRY 161:[197:java/lang/Throwable  , 218:java/lang/Throwable  , 248:java/lang/Throwable  ]
+TRY 165:[197:java/lang/Throwable  , 218:java/lang/Throwable  , 248:java/lang/Throwable  ]
+TRY 166:[197:java/lang/Throwable  , 218:java/lang/Throwable  , 248:java/lang/Throwable  ]
+TRY 173:[197:java/lang/Throwable  , 218:java/lang/Throwable  , 248:java/lang/Throwable  ]
+TRY 174:[197:java/lang/Throwable  , 218:java/lang/Throwable  , 248:java/lang/Throwable  ]
+TRY 175:[197:java/lang/Throwable  , 218:java/lang/Throwable  , 248:java/lang/Throwable  ]
+TRY 185:[197:java/lang/Throwable  , 218:java/lang/Throwable  , 248:java/lang/Throwable  ]
+TRY 188:[197:java/lang/Throwable  , 218:java/lang/Throwable  , 248:java/lang/Throwable  ]
+TRY 190:[197:java/lang/Throwable  , 218:java/lang/Throwable  , 248:java/lang/Throwable  ]
+TRY 193:[218:java/lang/Throwable  , 248:java/lang/Throwable  ]
+TRY 195:[218:java/lang/Throwable  , 248:java/lang/Throwable  ]
+TRY 199:[218:java/lang/Throwable  , 248:java/lang/Throwable  ]
+TRY 201:[218:java/lang/Throwable  , 248:java/lang/Throwable  ]
+TRY 203:[218:java/lang/Throwable  , 248:java/lang/Throwable  ]
+TRY 205:[218:java/lang/Throwable  , 248:java/lang/Throwable  ]
+TRY 207:[218:java/lang/Throwable  , 248:java/lang/Throwable  ]
+TRY 220:[248:java/lang/Throwable  ]
+TRY 222:[248:java/lang/Throwable  ]
+TRY 226:[248:java/lang/Throwable  ]
+TRY 227:[248:java/lang/Throwable  ]
+TRY 230:[248:java/lang/Throwable  ]
+TRY 231:[248:java/lang/Throwable  ]
+TRY 233:[248:java/lang/Throwable  ]
+TRY 235:[248:java/lang/Throwable  ]
+TRY 236:[248:java/lang/Throwable  ]
+TRY 237:[248:java/lang/Throwable  ]
+TRY 239:[248:java/lang/Throwable  ]
 CONST null
 ASTORE 2
 CONST Integer:0
@@ -82,35 +82,25 @@
 ISTORE 4
 ILOAD 4
 CONST Integer:256
-IF_ICMPGE L253
+IF_ICMPGE L242
 ALOAD 1
 INVOKEVIRTUAL local/lito381/server/NativePlayerCommit$Queue.waitNanos()J
 LSTORE 5/6
 LLOAD 5/6
 CONST Long:0
 LCMP
-IFLE L26
+IFLE L21
 ALOAD 0
 ALOAD 1
 LLOAD 5/6
 INVOKESTATIC local/lito381/server/NativePlayerLifetime.deadline(Llocal/lito381/server/ClientSession;Llocal/lito381/server/NativePlayerCommit$Queue;J)V
-ILOAD 3
-IFNE L25
-ALOAD 0
-ALOAD 1
-INVOKESTATIC local/lito381/server/NativePlayerCommit.resumeAccepted(Llocal/lito381/server/ClientSession;Llocal/lito381/server/NativePlayerCommit$Queue;)V
-RETURN
+GOTO L242
 ALOAD 1
 INVOKEVIRTUAL local/lito381/server/NativePlayerCommit$Queue.claim()Llocal/lito381/server/NativePlayerCommit$Event;
 ASTORE 7
 ALOAD 7
-IFNONNULL L37
-ILOAD 3
-IFNE L36
-ALOAD 0
-ALOAD 1
-INVOKESTATIC local/lito381/server/NativePlayerCommit.resumeAccepted(Llocal/lito381/server/ClientSession;Llocal/lito381/server/NativePlayerCommit$Queue;)V
-RETURN
+IFNONNULL L27
+GOTO L242
 CONST Integer:0
 ISTORE 8
 ALOAD 0
@@ -127,22 +117,22 @@
 INVOKESTATIC local/lito381/server/NativePlayerVitals.gm(Llocal/lito381/server/ClientSession;)Z
 ISTORE 11
 ILOAD 10
-IFEQ L77
+IFEQ L67
 ALOAD 1
 GETFIELD local/lito381/server/NativePlayerCommit$Queue.closed:Z
-IFNE L77
+IFNE L67
 ALOAD 1
 GETFIELD local/lito381/server/NativePlayerCommit$Queue.terminal:Z
-IFNE L77
+IFNE L67
 ALOAD 7
 GETFIELD local/lito381/server/NativePlayerCommit$Event.target:Llocal/lito381/server/NativePlayerCommit$Binding;
 ALOAD 0
 INVOKEVIRTUAL local/lito381/server/NativePlayerCommit$Binding.matchesFast(Llocal/lito381/server/ClientSession;)Z
-IFEQ L77
+IFEQ L67
 ALOAD 0
 CONST str:"playerHp"
 INVOKESTATIC local/lito381/server/NativePlayerCommit.integer(Ljava/lang/Object;Ljava/lang/String;)I
-IFLE L77
+IFLE L67
 ALOAD 0
 ALOAD 9
 ALOAD 7
@@ -150,7 +140,7 @@
 ALOAD 7
 GETFIELD local/lito381/server/NativePlayerCommit$Event.attacker:Llocal/lito381/server/NativeCombatAttacker$Ref;
 INVOKESTATIC local/lito381/server/NativeEquipmentDefense.resolve(Llocal/lito381/server/ClientSession;Llocal/lito381/server/ClientSession;Ljava/util/List;Llocal/lito381/server/NativeCombatAttacker$Ref;)Llocal/lito381/server/NativeEquipmentDefense$Resolved;
-GOTO L78
+GOTO L68
 INVOKESTATIC local/lito381/server/NativeEquipmentDefense$Resolved.empty()Llocal/lito381/server/NativeEquipmentDefense$Resolved;
 ASTORE 12
 ALOAD 1
@@ -163,51 +153,30 @@
 INVOKESTATIC local/lito381/server/NativeHpCompletionAdmission.reserve(Llocal/lito381/server/ClientSession;Llocal/lito381/server/NativePlayerCommit$Queue;)Llocal/lito381/server/NativePlayerVitalCompletions$Receipt;
 ASTORE 13
 ALOAD 13
-IFNULL L134
+IFNULL L103
 ALOAD 1
 GETFIELD local/lito381/server/NativePlayerCommit$Queue.closed:Z
-IFNE L134
+IFNE L103
 ALOAD 1
 GETFIELD local/lito381/server/NativePlayerCommit$Queue.terminal:Z
-IFNE L134
+IFNE L103
 ALOAD 7
 GETFIELD local/lito381/server/NativePlayerCommit$Event.target:Llocal/lito381/server/NativePlayerCommit$Binding;
 ALOAD 0
 INVOKEVIRTUAL local/lito381/server/NativePlayerCommit$Binding.matchesFast(Llocal/lito381/server/ClientSession;)Z
-IFEQ L134
+IFEQ L103
 ALOAD 0
 CONST str:"phase"
 INVOKESTATIC local/lito381/server/CombatAccess.field(Ljava/lang/Object;Ljava/lang/String;)Ljava/lang/Object;
 INVOKESTATIC java/lang/String.valueOf(Ljava/lang/Object;)Ljava/lang/String;
 ALOAD 7
 GETFIELD local/lito381/server/NativePlayerCommit$Event.world:Z
-IFEQ L110
+IFEQ L100
 CONST str:"WORLD"
-GOTO L111
+GOTO L101
 CONST str:"BATTLE"
 INVOKEVIRTUAL java/lang/String.equals(Ljava/lang/Object;)Z
-IFEQ L134
-ALOAD 0
-ALOAD 7
-ALOAD 9
-ILOAD 10
-INVOKESTATIC local/lito381/server/NativeSourceGroundQueued.beforeEvent(Llocal/lito381/server/ClientSession;Llocal/lito381/server/NativePlayerCommit$Event;Llocal/lito381/server/ClientSession;Z)Z
-IFEQ L134
-ALOAD 0
-CONST str:"playerMaxHp"
-INVOKESTATIC local/lito381/server/NativePlayerCommit.integer(Ljava/lang/Object;Ljava/lang/String;)I
-ISTORE 14
-ALOAD 0
-CONST str:"playerHp"
-INVOKESTATIC local/lito381/server/NativePlayerCommit.integer(Ljava/lang/Object;Ljava/lang/String;)I
-ISTORE 15
-ILOAD 15
-IFLE L131
-CONST Integer:1
-GOTO L132
-CONST Integer:0
-ISTORE 8
-GOTO L144
+IFNE L113
 ALOAD 7
 INVOKESTATIC local/lito381/server/NativeAcceptedFreezeMutation.abandon(Llocal/lito381/server/NativePlayerCommit$Event;)V
 GETSTATIC local/lito381/server/NativePlayerCommit.dropped:Ljava/util/concurrent/atomic/AtomicLong;
@@ -217,9 +186,29 @@
 GETFIELD local/lito381/server/NativePlayerCommit$Queue.vitalLock:Ljava/util/concurrent/locks/ReentrantLock;
 ALOAD 13
 INVOKESTATIC local/lito381/server/NativeHpCompletionAdmission.unlock(Ljava/util/concurrent/locks/ReentrantLock;Llocal/lito381/server/NativePlayerVitalCompletions$Receipt;)V
-GOTO L251
+GOTO L240
+ALOAD 0
+ALOAD 7
+ALOAD 9
+ILOAD 10
+INVOKESTATIC local/lito381/server/NativeSourceGroundQueued.beforeEvent(Llocal/lito381/server/ClientSession;Llocal/lito381/server/NativePlayerCommit$Event;Llocal/lito381/server/ClientSession;Z)Z
+IFEQ L103
+ALOAD 0
+CONST str:"playerMaxHp"
+INVOKESTATIC local/lito381/server/NativePlayerCommit.integer(Ljava/lang/Object;Ljava/lang/String;)I
+ISTORE 14
+ALOAD 0
+CONST str:"playerHp"
+INVOKESTATIC local/lito381/server/NativePlayerCommit.integer(Ljava/lang/Object;Ljava/lang/String;)I
+ISTORE 15
+ILOAD 15
+IFLE L131
+CONST Integer:1
+GOTO L132
+CONST Integer:0
+ISTORE 8
 ILOAD 8
-IFEQ L159
+IFEQ L148
 ILOAD 15
 ILOAD 14
 ALOAD 7
@@ -232,11 +221,11 @@
 ALOAD 16
 INVOKEVIRTUAL local/lito381/server/NativePlayerVitals$Result.after()I
 ISTORE 17
-GOTO L174
+GOTO L163
 ILOAD 11
-IFEQ L163
-ILOAD 15
-GOTO L164
+IFEQ L152
+ILOAD 15
+GOTO L153
 CONST Integer:0
 ISTORE 17
 NEW local/lito381/server/NativePlayerVitals$Result
@@ -254,7 +243,7 @@
 PUTFIELD local/lito381/server/NativePlayerCommit$Queue.terminal:Z
 ILOAD 17
 ILOAD 15
-IF_ICMPEQ L186
+IF_ICMPEQ L175
 ALOAD 0
 CONST str:"playerHp"
 ILOAD 17
@@ -281,7 +270,7 @@
 GETFIELD local/lito381/server/NativePlayerCommit$Queue.vitalLock:Ljava/util/concurrent/locks/ReentrantLock;
 ALOAD 13
 INVOKESTATIC local/lito381/server/NativeHpCompletionAdmission.unlock(Ljava/util/concurrent/locks/ReentrantLock;Llocal/lito381/server/NativePlayerVitalCompletions$Receipt;)V
-GOTO L215
+GOTO L204
 ASTORE 19
 ALOAD 1
 GETFIELD local/lito381/server/NativePlayerCommit$Queue.vitalLock:Ljava/util/concurrent/locks/ReentrantLock;
@@ -293,28 +282,28 @@
 INVOKESTATIC local/lito381/server/NativePlayerVitalCompletions.drain(Llocal/lito381/server/ClientSession;)V
 ALOAD 18
 INVOKEVIRTUAL local/lito381/server/NativeAcceptedHpCompletion.suspend()Z
-IFEQ L228
+IFEQ L217
 CONST Integer:1
 ISTORE 20
 ILOAD 20
-IFNE L227
+IFNE L216
 ALOAD 0
 ALOAD 1
 INVOKESTATIC local/lito381/server/NativePlayerCommit.resumeAccepted(Llocal/lito381/server/ClientSession;Llocal/lito381/server/NativePlayerCommit$Queue;)V
 RETURN
-GOTO L251
+GOTO L240
 ASTORE 21
 ALOAD 7
 INVOKESTATIC local/lito381/server/NativeAcceptedFreezeMutation.abandon(Llocal/lito381/server/NativePlayerCommit$Event;)V
 ALOAD 21
 INVOKESTATIC local/lito381/server/NativeAcceptedState.isStale(Ljava/lang/Throwable;)Z
-IFEQ L241
+IFEQ L230
 ILOAD 8
-IFEQ L251
+IFEQ L229
 GETSTATIC local/lito381/server/NativePlayerCommit.committed:Ljava/util/concurrent/atomic/AtomicLong;
 INVOKEVIRTUAL java/util/concurrent/atomic/AtomicLong.incrementAndGet()J
 POP2
-GOTO L251
+GOTO L240
 GETSTATIC local/lito381/server/NativePlayerCommit.failed:Ljava/util/concurrent/atomic/AtomicLong;
 INVOKEVIRTUAL java/util/concurrent/atomic/AtomicLong.incrementAndGet()J
 POP2
@@ -328,16 +317,17 @@
 IINC var=4 incr=1
 GOTO L6
 ILOAD 3
-IFNE L258
+IFNE L256
 ALOAD 0
 ALOAD 1
 INVOKESTATIC local/lito381/server/NativePlayerCommit.resumeAccepted(Llocal/lito381/server/ClientSession;Llocal/lito381/server/NativePlayerCommit$Queue;)V
-RETURN
+GOTO L256
 ASTORE 22
 ILOAD 3
-IFNE L265
+IFNE L254
 ALOAD 0
 ALOAD 1
 INVOKESTATIC local/lito381/server/NativePlayerCommit.resumeAccepted(Llocal/lito381/server/ClientSession;Llocal/lito381/server/NativePlayerCommit$Queue;)V
 ALOAD 22
 ATHROW
+RETURN
```

</details>

<details>
<summary>nativeManaTransferRecover：完整正規化差異</summary>

```diff
--- staged/NativePlayerCommit/nativeManaTransferRecover(Llocal/lito381/server/NativePlayerVitalCompletions$Receipt;Llocal/lito381/server/ClientSession;Llocal/lito381/server/NativePlayerCommit$Binding;IILjava/lang/String;)V
+++ candidate/NativePlayerCommit/nativeManaTransferRecover(Llocal/lito381/server/NativePlayerVitalCompletions$Receipt;Llocal/lito381/server/ClientSession;Llocal/lito381/server/NativePlayerCommit$Binding;IILjava/lang/String;)V
@@ -5,27 +5,27 @@

 ||||pvis=|pinv=|lvtvis=|lvtinv=

-TRY 30:[144:java/lang/Throwable  ]
-TRY 31:[144:java/lang/Throwable  ]
-TRY 33:[133:java/lang/Throwable  , 144:java/lang/Throwable  ]
-TRY 37:[133:java/lang/Throwable  , 144:java/lang/Throwable  ]
-TRY 41:[133:java/lang/Throwable  , 144:java/lang/Throwable  ]
-TRY 45:[133:java/lang/Throwable  , 144:java/lang/Throwable  ]
-TRY 49:[133:java/lang/Throwable  , 144:java/lang/Throwable  ]
-TRY 53:[133:java/lang/Throwable  , 144:java/lang/Throwable  ]
-TRY 57:[133:java/lang/Throwable  , 144:java/lang/Throwable  ]
-TRY 68:[133:java/lang/Throwable  , 144:java/lang/Throwable  ]
-TRY 82:[133:java/lang/Throwable  , 144:java/lang/Throwable  ]
-TRY 106:[133:java/lang/Throwable  , 144:java/lang/Throwable  ]
-TRY 107:[133:java/lang/Throwable  , 144:java/lang/Throwable  ]
-TRY 113:[133:java/lang/Throwable  , 144:java/lang/Throwable  ]
-TRY 114:[133:java/lang/Throwable  , 144:java/lang/Throwable  ]
-TRY 128:[133:java/lang/Throwable  , 144:java/lang/Throwable  ]
-TRY 130:[144:java/lang/Throwable  ]
-TRY 131:[144:java/lang/Throwable  ]
-TRY 135:[144:java/lang/Throwable  ]
-TRY 136:[144:java/lang/Throwable  ]
-TRY 138:[144:java/lang/Throwable  ]
+TRY 32:[146:java/lang/Throwable  ]
+TRY 33:[146:java/lang/Throwable  ]
+TRY 35:[135:java/lang/Throwable  , 146:java/lang/Throwable  ]
+TRY 39:[135:java/lang/Throwable  , 146:java/lang/Throwable  ]
+TRY 43:[135:java/lang/Throwable  , 146:java/lang/Throwable  ]
+TRY 47:[135:java/lang/Throwable  , 146:java/lang/Throwable  ]
+TRY 51:[135:java/lang/Throwable  , 146:java/lang/Throwable  ]
+TRY 55:[135:java/lang/Throwable  , 146:java/lang/Throwable  ]
+TRY 59:[135:java/lang/Throwable  , 146:java/lang/Throwable  ]
+TRY 70:[135:java/lang/Throwable  , 146:java/lang/Throwable  ]
+TRY 84:[135:java/lang/Throwable  , 146:java/lang/Throwable  ]
+TRY 108:[135:java/lang/Throwable  , 146:java/lang/Throwable  ]
+TRY 109:[135:java/lang/Throwable  , 146:java/lang/Throwable  ]
+TRY 115:[135:java/lang/Throwable  , 146:java/lang/Throwable  ]
+TRY 116:[135:java/lang/Throwable  , 146:java/lang/Throwable  ]
+TRY 130:[135:java/lang/Throwable  , 146:java/lang/Throwable  ]
+TRY 132:[146:java/lang/Throwable  ]
+TRY 133:[146:java/lang/Throwable  ]
+TRY 137:[146:java/lang/Throwable  ]
+TRY 138:[146:java/lang/Throwable  ]
+TRY 140:[146:java/lang/Throwable  ]
 ILOAD 3
 IFGT L5
 ILOAD 4
@@ -38,10 +38,12 @@
 ALOAD 6
 IFNONNULL L12
 RETURN
+ALOAD 1
+POP
 ALOAD 0
 ASTORE 7
 ALOAD 7
-IFNONNULL L17
+IFNONNULL L19
 RETURN
 CONST Integer:0
 ISTORE 8
@@ -60,15 +62,15 @@
 INVOKEVIRTUAL java/util/concurrent/locks/ReentrantLock.lock()V
 ALOAD 6
 GETFIELD local/lito381/server/NativePlayerCommit$Queue.closed:Z
-IFNE L129
+IFNE L131
 ALOAD 2
 ALOAD 1
 INVOKEVIRTUAL local/lito381/server/NativePlayerCommit$Binding.matchesFast(Llocal/lito381/server/ClientSession;)Z
-IFEQ L129
+IFEQ L131
 ALOAD 1
 CONST str:"playerHp"
 INVOKESTATIC local/lito381/server/NativePlayerCommit.integer(Ljava/lang/Object;Ljava/lang/String;)I
-IFLE L129
+IFLE L131
 ALOAD 1
 CONST str:"playerHp"
 INVOKESTATIC local/lito381/server/NativePlayerCommit.integer(Ljava/lang/Object;Ljava/lang/String;)I
@@ -86,7 +88,7 @@
 INVOKESTATIC local/lito381/server/NativePlayerCommit.integer(Ljava/lang/Object;Ljava/lang/String;)I
 ISTORE 17
 ILOAD 3
-IFLE L71
+IFLE L73
 ILOAD 16
 I2L
 ILOAD 14
@@ -96,11 +98,11 @@
 LADD
 INVOKESTATIC java/lang/Math.min(JJ)J
 L2I
-GOTO L72
+GOTO L74
 ILOAD 14
 ISTORE 18
 ILOAD 4
-IFLE L85
+IFLE L87
 ILOAD 17
 I2L
 ILOAD 15
@@ -110,41 +112,41 @@
 LADD
 INVOKESTATIC java/lang/Math.min(JJ)J
 L2I
-GOTO L86
+GOTO L88
 ILOAD 15
 ISTORE 19
 ILOAD 18
 ILOAD 14
-IF_ICMPEQ L92
+IF_ICMPEQ L94
 CONST Integer:1
-GOTO L93
+GOTO L95
 CONST Integer:0
 ISTORE 20
 ILOAD 19
 ILOAD 15
-IF_ICMPEQ L99
+IF_ICMPEQ L101
 CONST Integer:1
-GOTO L100
+GOTO L102
 CONST Integer:0
 ISTORE 21
 ILOAD 20
-IFEQ L108
+IFEQ L110
 ALOAD 1
 CONST str:"playerHp"
 ILOAD 18
 INVOKESTATIC java/lang/Integer.valueOf(I)Ljava/lang/Integer;
 INVOKESTATIC local/lito381/server/CombatAccess.set(Ljava/lang/Object;Ljava/lang/String;Ljava/lang/Object;)V
 ILOAD 21
-IFEQ L115
+IFEQ L117
 ALOAD 1
 CONST str:"playerMp"
 ILOAD 19
 INVOKESTATIC java/lang/Integer.valueOf(I)Ljava/lang/Integer;
 INVOKESTATIC local/lito381/server/CombatAccess.set(Ljava/lang/Object;Ljava/lang/String;Ljava/lang/Object;)V
 ILOAD 20
-IFNE L119
+IFNE L121
 ILOAD 21
-IFEQ L129
+IFEQ L131
 ALOAD 7
 ALOAD 2
 ILOAD 18
@@ -158,7 +160,7 @@
 ALOAD 6
 GETFIELD local/lito381/server/NativePlayerCommit$Queue.vitalLock:Ljava/util/concurrent/locks/ReentrantLock;
 INVOKEVIRTUAL java/util/concurrent/locks/ReentrantLock.unlock()V
-GOTO L139
+GOTO L141
 ASTORE 22
 ALOAD 6
 GETFIELD local/lito381/server/NativePlayerCommit$Queue.vitalLock:Ljava/util/concurrent/locks/ReentrantLock;
@@ -169,7 +171,7 @@
 INVOKEVIRTUAL local/lito381/server/NativePlayerVitalCompletions$Receipt.close()V
 ALOAD 1
 INVOKESTATIC local/lito381/server/NativePlayerVitalCompletions.drain(Llocal/lito381/server/ClientSession;)V
-GOTO L151
+GOTO L153
 ASTORE 23
 ALOAD 7
 INVOKEVIRTUAL local/lito381/server/NativePlayerVitalCompletions$Receipt.close()V
```

</details>

### NativeProcHealingEncounter$Dispatch

<details>
<summary>本輪全部方法診斷</summary>

```json
{
  "name": "local/lito381/server/NativeProcHealingEncounter$Dispatch",
  "status": "DIFF",
  "byteSame": false,
  "allowMethodsApplied": false,
  "methodResults": [
    {
      "method": "run()V",
      "status": "DIFF",
      "detail": "method run()V: left=13:[91:java/lang/Throwable  , 99:java/lang/Throwable  ] right=13:[74:java/lang/Throwable  , 82:java/lang/Throwable  ]; paths rejected: normal control-flow cycle; labelled CFGs differ"
    }
  ],
  "effectsMethods": [],
  "approvedMethods": [],
  "firstDiff": {
    "where": "method",
    "member": "run()V",
    "insnIndex": null,
    "detail": "left=13:[91:java/lang/Throwable  , 99:java/lang/Throwable  ] right=13:[74:java/lang/Throwable  , 82:java/lang/Throwable  ]; paths rejected: normal control-flow cycle; labelled CFGs differ"
  }
}
```

</details>

<details>
<summary>run：完整正規化差異</summary>

```diff
--- staged/NativeProcHealingEncounter$Dispatch/run()V
+++ candidate/NativeProcHealingEncounter$Dispatch/run()V
@@ -5,35 +5,35 @@

 ||||pvis=|pinv=|lvtvis=|lvtinv=

-TRY 13:[91:java/lang/Throwable  , 99:java/lang/Throwable  ]
-TRY 15:[91:java/lang/Throwable  , 99:java/lang/Throwable  ]
-TRY 17:[91:java/lang/Throwable  , 99:java/lang/Throwable  ]
-TRY 25:[91:java/lang/Throwable  , 99:java/lang/Throwable  ]
-TRY 27:[91:java/lang/Throwable  , 99:java/lang/Throwable  ]
-TRY 29:[91:java/lang/Throwable  , 99:java/lang/Throwable  ]
-TRY 37:[91:java/lang/Throwable  , 99:java/lang/Throwable  ]
-TRY 39:[91:java/lang/Throwable  , 99:java/lang/Throwable  ]
-TRY 41:[91:java/lang/Throwable  , 99:java/lang/Throwable  ]
-TRY 49:[91:java/lang/Throwable  , 99:java/lang/Throwable  ]
-TRY 52:[91:java/lang/Throwable  , 99:java/lang/Throwable  ]
-TRY 55:[91:java/lang/Throwable  , 99:java/lang/Throwable  ]
-TRY 58:[91:java/lang/Throwable  , 99:java/lang/Throwable  ]
-TRY 61:[91:java/lang/Throwable  , 99:java/lang/Throwable  ]
-TRY 62:[91:java/lang/Throwable  , 99:java/lang/Throwable  ]
-TRY 64:[91:java/lang/Throwable  , 99:java/lang/Throwable  ]
-TRY 66:[91:java/lang/Throwable  , 99:java/lang/Throwable  ]
-TRY 68:[91:java/lang/Throwable  , 99:java/lang/Throwable  ]
-TRY 70:[91:java/lang/Throwable  , 99:java/lang/Throwable  ]
-TRY 73:[91:java/lang/Throwable  , 99:java/lang/Throwable  ]
-TRY 74:[91:java/lang/Throwable  , 99:java/lang/Throwable  ]
-TRY 75:[91:java/lang/Throwable  , 99:java/lang/Throwable  ]
-TRY 85:[91:java/lang/Throwable  , 99:java/lang/Throwable  ]
-TRY 93:[99:java/lang/Throwable  ]
+TRY 13:[74:java/lang/Throwable  , 82:java/lang/Throwable  ]
+TRY 15:[74:java/lang/Throwable  , 82:java/lang/Throwable  ]
+TRY 17:[74:java/lang/Throwable  , 82:java/lang/Throwable  ]
+TRY 21:[74:java/lang/Throwable  , 82:java/lang/Throwable  ]
+TRY 23:[74:java/lang/Throwable  , 82:java/lang/Throwable  ]
+TRY 25:[74:java/lang/Throwable  , 82:java/lang/Throwable  ]
+TRY 29:[74:java/lang/Throwable  , 82:java/lang/Throwable  ]
+TRY 31:[74:java/lang/Throwable  , 82:java/lang/Throwable  ]
+TRY 33:[74:java/lang/Throwable  , 82:java/lang/Throwable  ]
+TRY 37:[74:java/lang/Throwable  , 82:java/lang/Throwable  ]
+TRY 40:[74:java/lang/Throwable  , 82:java/lang/Throwable  ]
+TRY 43:[74:java/lang/Throwable  , 82:java/lang/Throwable  ]
+TRY 46:[74:java/lang/Throwable  , 82:java/lang/Throwable  ]
+TRY 49:[74:java/lang/Throwable  , 82:java/lang/Throwable  ]
+TRY 50:[74:java/lang/Throwable  , 82:java/lang/Throwable  ]
+TRY 52:[74:java/lang/Throwable  , 82:java/lang/Throwable  ]
+TRY 54:[74:java/lang/Throwable  , 82:java/lang/Throwable  ]
+TRY 56:[74:java/lang/Throwable  , 82:java/lang/Throwable  ]
+TRY 58:[74:java/lang/Throwable  , 82:java/lang/Throwable  ]
+TRY 61:[74:java/lang/Throwable  , 82:java/lang/Throwable  ]
+TRY 62:[74:java/lang/Throwable  , 82:java/lang/Throwable  ]
+TRY 63:[74:java/lang/Throwable  , 82:java/lang/Throwable  ]
+TRY 68:[74:java/lang/Throwable  , 82:java/lang/Throwable  ]
+TRY 76:[82:java/lang/Throwable  ]
 CONST Integer:0
 ISTORE 1
 ILOAD 1
 CONST Integer:256
-IF_ICMPGE L108
+IF_ICMPGE L91
 ALOAD 0
 GETFIELD local/lito381/server/NativeProcHealingEncounter$Dispatch.queue:Llocal/lito381/server/CombatNpcDamageQueue$Queue;
 INVOKEVIRTUAL local/lito381/server/CombatNpcDamageQueue$Queue.claim()Llocal/lito381/server/CombatNpcDamageQueue$Event;
@@ -47,46 +47,34 @@
 GETFIELD local/lito381/server/NativeProcHealingEncounter$Dispatch.queue:Llocal/lito381/server/CombatNpcDamageQueue$Queue;
 ALOAD 2
 INVOKESTATIC local/lito381/server/NativeSourcePoisonNpc.consume(Llocal/lito381/server/GameHub;Llocal/lito381/server/CombatNpcDamageQueue$Queue;Llocal/lito381/server/CombatNpcDamageQueue$Event;)Z
-IFEQ L24
-ALOAD 0
-GETFIELD local/lito381/server/NativeProcHealingEncounter$Dispatch.queue:Llocal/lito381/server/CombatNpcDamageQueue$Queue;
-ALOAD 2
-INVOKEVIRTUAL local/lito381/server/CombatNpcDamageQueue$Queue.complete(Llocal/lito381/server/CombatNpcDamageQueue$Event;)V
-GOTO L106
+IFEQ L20
+GOTO L69
 ALOAD 0
 GETFIELD local/lito381/server/NativeProcHealingEncounter$Dispatch.hub:Llocal/lito381/server/GameHub;
 ALOAD 0
 GETFIELD local/lito381/server/NativeProcHealingEncounter$Dispatch.queue:Llocal/lito381/server/CombatNpcDamageQueue$Queue;
 ALOAD 2
 INVOKESTATIC local/lito381/server/NativeSourcePhysicalNpc.consume(Llocal/lito381/server/GameHub;Llocal/lito381/server/CombatNpcDamageQueue$Queue;Llocal/lito381/server/CombatNpcDamageQueue$Event;)Z
-IFEQ L36
-ALOAD 0
-GETFIELD local/lito381/server/NativeProcHealingEncounter$Dispatch.queue:Llocal/lito381/server/CombatNpcDamageQueue$Queue;
-ALOAD 2
-INVOKEVIRTUAL local/lito381/server/CombatNpcDamageQueue$Queue.complete(Llocal/lito381/server/CombatNpcDamageQueue$Event;)V
-GOTO L106
+IFEQ L28
+GOTO L69
 ALOAD 0
 GETFIELD local/lito381/server/NativeProcHealingEncounter$Dispatch.hub:Llocal/lito381/server/GameHub;
 ALOAD 0
 GETFIELD local/lito381/server/NativeProcHealingEncounter$Dispatch.queue:Llocal/lito381/server/CombatNpcDamageQueue$Queue;
 ALOAD 2
 INVOKESTATIC local/lito381/server/NativeSourceCubeNpcQueue.consume(Llocal/lito381/server/GameHub;Llocal/lito381/server/CombatNpcDamageQueue$Queue;Llocal/lito381/server/CombatNpcDamageQueue$Event;)Z
-IFEQ L48
-ALOAD 0
-GETFIELD local/lito381/server/NativeProcHealingEncounter$Dispatch.queue:Llocal/lito381/server/CombatNpcDamageQueue$Queue;
-ALOAD 2
-INVOKEVIRTUAL local/lito381/server/CombatNpcDamageQueue$Queue.complete(Llocal/lito381/server/CombatNpcDamageQueue$Event;)V
-GOTO L106
+IFEQ L36
+GOTO L69
 ALOAD 2
 INVOKEVIRTUAL local/lito381/server/CombatNpcDamageQueue$Event.physical()Llocal/lito381/server/CombatWorldCommit$Plan;
 ASTORE 3
 ALOAD 3
 INSTANCEOF local/lito381/server/NativeProcHealingEncounter$Plan
-IFEQ L58
+IFEQ L46
 ALOAD 3
 CHECKCAST local/lito381/server/NativeProcHealingEncounter$Plan
 ASTORE 4
-GOTO L63
+GOTO L51
 NEW java/lang/IllegalStateException
 DUP
 CONST str:"foreign encounter Proc queue plan"
@@ -107,19 +95,14 @@
 CHECKCAST local/lito381/server/NativeProcEncounterPublication$Context
 ASTORE 5
 ALOAD 5
-IFNONNULL L84
-ALOAD 0
-GETFIELD local/lito381/server/NativeProcHealingEncounter$Dispatch.queue:Llocal/lito381/server/CombatNpcDamageQueue$Queue;
-ALOAD 2
-INVOKEVIRTUAL local/lito381/server/CombatNpcDamageQueue$Queue.complete(Llocal/lito381/server/CombatNpcDamageQueue$Event;)V
-GOTO L106
+IFNULL L69
 ALOAD 5
 INVOKESTATIC local/lito381/server/NativeProcEncounterPublication.publish(Llocal/lito381/server/NativeProcEncounterPublication$Context;)V
 ALOAD 0
 GETFIELD local/lito381/server/NativeProcHealingEncounter$Dispatch.queue:Llocal/lito381/server/CombatNpcDamageQueue$Queue;
 ALOAD 2
 INVOKEVIRTUAL local/lito381/server/CombatNpcDamageQueue$Queue.complete(Llocal/lito381/server/CombatNpcDamageQueue$Event;)V
-GOTO L106
+GOTO L89
 ASTORE 6
 ALOAD 6
 INVOKESTATIC local/lito381/server/CombatNpcDamageQueue.failed(Ljava/lang/Throwable;)V
@@ -127,7 +110,7 @@
 GETFIELD local/lito381/server/NativeProcHealingEncounter$Dispatch.queue:Llocal/lito381/server/CombatNpcDamageQueue$Queue;
 ALOAD 2
 INVOKEVIRTUAL local/lito381/server/CombatNpcDamageQueue$Queue.complete(Llocal/lito381/server/CombatNpcDamageQueue$Event;)V
-GOTO L106
+GOTO L89
 ASTORE 7
 ALOAD 0
 GETFIELD local/lito381/server/NativeProcHealingEncounter$Dispatch.queue:Llocal/lito381/server/CombatNpcDamageQueue$Queue;
```

</details>

### 未配對 lambda 的完整方法內容

<details>
<summary>left／NativeLightAction／lambda$content$af63fe6e68cfd3899283fefbe1d7b62e0a0f81208c854f8311239d75af7c1a51</summary>

```text
lambda$content$af63fe6e68cfd3899283fefbe1d7b62e0a0f81208c854f8311239d75af7c1a51(Llocal/lito381/server/NativeLightAction$Ingress;Llocal/lito381/server/ClientSession;Llocal/lito381/server/NativeSelfCastAction$Task;Llocal/lito381/server/NativePlayerCommit$Binding;Llocal/lito381/server/ClientSession;Llocal/lito381/server/NativePlayerVitalCompletions$Receipt;)V
(Llocal/lito381/server/NativeLightAction$Ingress;Llocal/lito381/server/ClientSession;Llocal/lito381/server/NativeSelfCastAction$Task;Llocal/lito381/server/NativePlayerCommit$Binding;Llocal/lito381/server/ClientSession;Llocal/lito381/server/NativePlayerVitalCompletions$Receipt;)V
0xa


||||pvis=|pinv=|lvtvis=|lvtinv=

TRY 77:[108:java/io/IOException  , 114:java/lang/Throwable  ]
TRY 99:[103:java/lang/Error  , 103:java/lang/RuntimeException  ]
TRY 101:[103:java/lang/Error  , 103:java/lang/RuntimeException  ]
TRY 109:[114:java/lang/Throwable  ]
TRY 112:[114:java/lang/Throwable  ]
TRY 113:[114:java/lang/Throwable  ]
TRY 136:[140:java/lang/Error  , 140:java/lang/RuntimeException  ]
TRY 138:[140:java/lang/Error  , 140:java/lang/RuntimeException  ]
ALOAD 0
ALOAD 1
INVOKESTATIC local/lito381/server/NativeSourceMaterialMain.locked(Ljava/lang/Object;Llocal/lito381/server/ClientSession;)Z
IFNE L5
RETURN
ALOAD 0
GETFIELD local/lito381/server/NativeLightAction$Ingress.binding:Llocal/lito381/server/NativePlayerCommit$Binding;
ALOAD 1
ALOAD 2
INVOKESTATIC local/lito381/server/NativeSourceMaterialPairGuard.matches(Llocal/lito381/server/NativePlayerCommit$Binding;Llocal/lito381/server/ClientSession;Llocal/lito381/server/NativeSelfCastAction$Task;)Z
IFEQ L48
ALOAD 3
ALOAD 4
ALOAD 2
INVOKESTATIC local/lito381/server/NativeSourceMaterialPairGuard.matches(Llocal/lito381/server/NativePlayerCommit$Binding;Llocal/lito381/server/ClientSession;Llocal/lito381/server/NativeSelfCastAction$Task;)Z
IFEQ L48
ALOAD 4
CONST str:"hub"
INVOKESTATIC local/lito381/server/CombatAccess.field(Ljava/lang/Object;Ljava/lang/String;)Ljava/lang/Object;
ALOAD 0
GETFIELD local/lito381/server/NativeLightAction$Ingress.hub:Llocal/lito381/server/GameHub;
IF_ACMPNE L48
ALOAD 1
CONST str:"sourceItemDisconnected"
INVOKESTATIC local/lito381/server/CombatAccess.field(Ljava/lang/Object;Ljava/lang/String;)Ljava/lang/Object;
CHECKCAST java/lang/Boolean
INVOKEVIRTUAL java/lang/Boolean.booleanValue()Z
IFNE L48
ALOAD 4
CONST str:"sourceItemDisconnected"
INVOKESTATIC local/lito381/server/CombatAccess.field(Ljava/lang/Object;Ljava/lang/String;)Ljava/lang/Object;
CHECKCAST java/lang/Boolean
INVOKEVIRTUAL java/lang/Boolean.booleanValue()Z
IFNE L48
ALOAD 0
GETFIELD local/lito381/server/NativeLightAction$Ingress.hub:Llocal/lito381/server/GameHub;
CONST str:"sourceItemApplication"
INVOKESTATIC local/lito381/server/CombatAccess.field(Ljava/lang/Object;Ljava/lang/String;)Ljava/lang/Object;
ALOAD 0
GETFIELD local/lito381/server/NativeLightAction$Ingress.application:Llocal/lito381/server/NativeDeItemApplication;
IF_ACMPNE L48
ALOAD 0
GETFIELD local/lito381/server/NativeLightAction$Ingress.application:Llocal/lito381/server/NativeDeItemApplication;
IFNULL L49
ALOAD 0
GETFIELD local/lito381/server/NativeLightAction$Ingress.application:Llocal/lito381/server/NativeDeItemApplication;
GETFIELD local/lito381/server/NativeDeItemApplication.closed:Z
IFEQ L49
RETURN
NEW local/lito381/server/NativeLightAction$Batch
DUP
ALOAD 1
ALOAD 0
GETFIELD local/lito381/server/NativeLightAction$Ingress.arrival:Llocal/lito381/server/NativeCastTimeline$Arrival;
ALOAD 0
INVOKESPECIAL local/lito381/server/NativeLightAction$Batch.<init>(Llocal/lito381/server/ClientSession;Llocal/lito381/server/NativeCastTimeline$Arrival;Llocal/lito381/server/NativeLightAction$Ingress;)V
ASTORE 6
ALOAD 6
CONST Integer:1
PUTFIELD local/lito381/server/NativeLightAction$Batch.captureSelf:Z
ALOAD 6
ALOAD 4
PUTFIELD local/lito381/server/NativeLightAction$Batch.target:Llocal/lito381/server/ClientSession;
ALOAD 6
ALOAD 3
PUTFIELD local/lito381/server/NativeLightAction$Batch.targetBinding:Llocal/lito381/server/NativePlayerCommit$Binding;
ALOAD 6
ALOAD 5
PUTFIELD local/lito381/server/NativeLightAction$Batch.targetReceipt:Llocal/lito381/server/NativePlayerVitalCompletions$Receipt;
GETSTATIC local/lito381/server/NativeLightAction.current:Ljava/lang/ThreadLocal;
INVOKEVIRTUAL java/lang/ThreadLocal.get()Ljava/lang/Object;
CHECKCAST local/lito381/server/NativeLightAction$Batch
ASTORE 7
GETSTATIC local/lito381/server/NativeLightAction.current:Ljava/lang/ThreadLocal;
ALOAD 6
INVOKEVIRTUAL java/lang/ThreadLocal.set(Ljava/lang/Object;)V
ALOAD 2
INVOKEINTERFACE local/lito381/server/NativeSelfCastAction$Task.run()V itf
ALOAD 7
IFNONNULL L83
GETSTATIC local/lito381/server/NativeLightAction.current:Ljava/lang/ThreadLocal;
INVOKEVIRTUAL java/lang/ThreadLocal.remove()V
GOTO L86
GETSTATIC local/lito381/server/NativeLightAction.current:Ljava/lang/ThreadLocal;
ALOAD 7
INVOKEVIRTUAL java/lang/ThreadLocal.set(Ljava/lang/Object;)V
ALOAD 6
GETFIELD local/lito381/server/NativeLightAction$Batch.outputs:Ljava/util/List;
INVOKEINTERFACE java/util/List.isEmpty()Z itf
IFEQ L94
ALOAD 6
GETFIELD local/lito381/server/NativeLightAction$Batch.completions:Ljava/util/List;
INVOKEINTERFACE java/util/List.isEmpty()Z itf
IFNE L147
ALOAD 1
ALOAD 4
ALOAD 5
INVOKESTATIC local/lito381/server/NativePairedCastSafety.transfer(Llocal/lito381/server/ClientSession;Llocal/lito381/server/ClientSession;Llocal/lito381/server/NativePlayerVitalCompletions$Receipt;)V
ALOAD 0
GETFIELD local/lito381/server/NativeLightAction$Ingress.receipt:Llocal/lito381/server/NativePlayerVitalCompletions$Receipt;
ALOAD 6
INVOKESTATIC local/lito381/server/NativePlayerVitalCompletions.enqueueLocked(Llocal/lito381/server/NativePlayerVitalCompletions$Receipt;Ljava/lang/Runnable;)V
GOTO L147
ASTORE 8
ALOAD 5
INVOKESTATIC local/lito381/server/NativePairedCastSafety.abandon(Llocal/lito381/server/NativePlayerVitalCompletions$Receipt;)V
ALOAD 8
ATHROW
ASTORE 9
NEW java/io/UncheckedIOException
DUP
ALOAD 9
INVOKESPECIAL java/io/UncheckedIOException.<init>(Ljava/io/IOException;)V
ATHROW
ASTORE 10
ALOAD 7
IFNONNULL L120
GETSTATIC local/lito381/server/NativeLightAction.current:Ljava/lang/ThreadLocal;
INVOKEVIRTUAL java/lang/ThreadLocal.remove()V
GOTO L123
GETSTATIC local/lito381/server/NativeLightAction.current:Ljava/lang/ThreadLocal;
ALOAD 7
INVOKEVIRTUAL java/lang/ThreadLocal.set(Ljava/lang/Object;)V
ALOAD 6
GETFIELD local/lito381/server/NativeLightAction$Batch.outputs:Ljava/util/List;
INVOKEINTERFACE java/util/List.isEmpty()Z itf
IFEQ L131
ALOAD 6
GETFIELD local/lito381/server/NativeLightAction$Batch.completions:Ljava/util/List;
INVOKEINTERFACE java/util/List.isEmpty()Z itf
IFNE L145
ALOAD 1
ALOAD 4
ALOAD 5
INVOKESTATIC local/lito381/server/NativePairedCastSafety.transfer(Llocal/lito381/server/ClientSession;Llocal/lito381/server/ClientSession;Llocal/lito381/server/NativePlayerVitalCompletions$Receipt;)V
ALOAD 0
GETFIELD local/lito381/server/NativeLightAction$Ingress.receipt:Llocal/lito381/server/NativePlayerVitalCompletions$Receipt;
ALOAD 6
INVOKESTATIC local/lito381/server/NativePlayerVitalCompletions.enqueueLocked(Llocal/lito381/server/NativePlayerVitalCompletions$Receipt;Ljava/lang/Runnable;)V
GOTO L145
ASTORE 11
ALOAD 5
INVOKESTATIC local/lito381/server/NativePairedCastSafety.abandon(Llocal/lito381/server/NativePlayerVitalCompletions$Receipt;)V
ALOAD 11
ATHROW
ALOAD 10
ATHROW
RETURN
```

</details>

<details>
<summary>left／NativeLightAction／lambda$content$b93037e7b70cc60f32950364f5b80c1fc077fc616c9138dff25da83806318b36</summary>

```text
lambda$content$b93037e7b70cc60f32950364f5b80c1fc077fc616c9138dff25da83806318b36(Llocal/lito381/server/NativeLightAction$Batch;Llocal/lito381/server/NativeSelfCastAction$Task;)V
(Llocal/lito381/server/NativeLightAction$Batch;Llocal/lito381/server/NativeSelfCastAction$Task;)V
0xa

java/io/IOException
||||pvis=|pinv=|lvtvis=|lvtinv=

TRY
ALOAD 0
GETFIELD local/lito381/server/NativeLightAction$Batch.accepted:Llocal/lito381/server/NativeLightAction$Ingress;
GETFIELD local/lito381/server/NativeLightAction$Ingress.binding:Llocal/lito381/server/NativePlayerCommit$Binding;
ALOAD 0
GETFIELD local/lito381/server/NativeLightAction$Batch.pc:Llocal/lito381/server/ClientSession;
INVOKEVIRTUAL local/lito381/server/NativePlayerCommit$Binding.matches(Llocal/lito381/server/ClientSession;)Z
IFEQ L9
ALOAD 1
INVOKEINTERFACE local/lito381/server/NativeSelfCastAction$Task.run()V itf
RETURN
```

</details>

<details>
<summary>right／NativeLightAction／lambda$content$6b68235d2c8d50e26cae1ba81014eacf934e85f556cc515212e7a3bda2b6efa6</summary>

```text
lambda$content$6b68235d2c8d50e26cae1ba81014eacf934e85f556cc515212e7a3bda2b6efa6(Llocal/lito381/server/NativeLightAction$Ingress;Llocal/lito381/server/ClientSession;Llocal/lito381/server/NativePlayerCommit$Binding;Llocal/lito381/server/ClientSession;Llocal/lito381/server/NativePlayerVitalCompletions$Receipt;Llocal/lito381/server/NativeSelfCastAction$Task;)V
(Llocal/lito381/server/NativeLightAction$Ingress;Llocal/lito381/server/ClientSession;Llocal/lito381/server/NativePlayerCommit$Binding;Llocal/lito381/server/ClientSession;Llocal/lito381/server/NativePlayerVitalCompletions$Receipt;Llocal/lito381/server/NativeSelfCastAction$Task;)V
0xa


||||pvis=|pinv=|lvtvis=|lvtinv=

TRY 77:[108:java/io/IOException  , 114:java/lang/Throwable  ]
TRY 99:[103:java/lang/RuntimeException  , 103:java/lang/Error  ]
TRY 101:[103:java/lang/RuntimeException  , 103:java/lang/Error  ]
TRY 109:[114:java/lang/Throwable  ]
TRY 112:[114:java/lang/Throwable  ]
TRY 113:[114:java/lang/Throwable  ]
TRY 136:[140:java/lang/RuntimeException  , 140:java/lang/Error  ]
TRY 138:[140:java/lang/RuntimeException  , 140:java/lang/Error  ]
ALOAD 0
ALOAD 1
INVOKESTATIC local/lito381/server/NativeSourceMaterialMain.locked(Ljava/lang/Object;Llocal/lito381/server/ClientSession;)Z
IFNE L5
RETURN
ALOAD 0
GETFIELD local/lito381/server/NativeLightAction$Ingress.binding:Llocal/lito381/server/NativePlayerCommit$Binding;
ALOAD 1
ALOAD 5
INVOKESTATIC local/lito381/server/NativeSourceMaterialPairGuard.matches(Llocal/lito381/server/NativePlayerCommit$Binding;Llocal/lito381/server/ClientSession;Llocal/lito381/server/NativeSelfCastAction$Task;)Z
IFEQ L48
ALOAD 2
ALOAD 3
ALOAD 5
INVOKESTATIC local/lito381/server/NativeSourceMaterialPairGuard.matches(Llocal/lito381/server/NativePlayerCommit$Binding;Llocal/lito381/server/ClientSession;Llocal/lito381/server/NativeSelfCastAction$Task;)Z
IFEQ L48
ALOAD 3
CONST str:"hub"
INVOKESTATIC local/lito381/server/CombatAccess.field(Ljava/lang/Object;Ljava/lang/String;)Ljava/lang/Object;
ALOAD 0
GETFIELD local/lito381/server/NativeLightAction$Ingress.hub:Llocal/lito381/server/GameHub;
IF_ACMPNE L48
ALOAD 1
CONST str:"sourceItemDisconnected"
INVOKESTATIC local/lito381/server/CombatAccess.field(Ljava/lang/Object;Ljava/lang/String;)Ljava/lang/Object;
CHECKCAST java/lang/Boolean
INVOKEVIRTUAL java/lang/Boolean.booleanValue()Z
IFNE L48
ALOAD 3
CONST str:"sourceItemDisconnected"
INVOKESTATIC local/lito381/server/CombatAccess.field(Ljava/lang/Object;Ljava/lang/String;)Ljava/lang/Object;
CHECKCAST java/lang/Boolean
INVOKEVIRTUAL java/lang/Boolean.booleanValue()Z
IFNE L48
ALOAD 0
GETFIELD local/lito381/server/NativeLightAction$Ingress.hub:Llocal/lito381/server/GameHub;
CONST str:"sourceItemApplication"
INVOKESTATIC local/lito381/server/CombatAccess.field(Ljava/lang/Object;Ljava/lang/String;)Ljava/lang/Object;
ALOAD 0
GETFIELD local/lito381/server/NativeLightAction$Ingress.application:Llocal/lito381/server/NativeDeItemApplication;
IF_ACMPNE L48
ALOAD 0
GETFIELD local/lito381/server/NativeLightAction$Ingress.application:Llocal/lito381/server/NativeDeItemApplication;
IFNULL L49
ALOAD 0
GETFIELD local/lito381/server/NativeLightAction$Ingress.application:Llocal/lito381/server/NativeDeItemApplication;
GETFIELD local/lito381/server/NativeDeItemApplication.closed:Z
IFEQ L49
RETURN
NEW local/lito381/server/NativeLightAction$Batch
DUP
ALOAD 1
ALOAD 0
GETFIELD local/lito381/server/NativeLightAction$Ingress.arrival:Llocal/lito381/server/NativeCastTimeline$Arrival;
ALOAD 0
INVOKESPECIAL local/lito381/server/NativeLightAction$Batch.<init>(Llocal/lito381/server/ClientSession;Llocal/lito381/server/NativeCastTimeline$Arrival;Llocal/lito381/server/NativeLightAction$Ingress;)V
ASTORE 6
ALOAD 6
CONST Integer:1
PUTFIELD local/lito381/server/NativeLightAction$Batch.captureSelf:Z
ALOAD 6
ALOAD 3
PUTFIELD local/lito381/server/NativeLightAction$Batch.target:Llocal/lito381/server/ClientSession;
ALOAD 6
ALOAD 2
PUTFIELD local/lito381/server/NativeLightAction$Batch.targetBinding:Llocal/lito381/server/NativePlayerCommit$Binding;
ALOAD 6
ALOAD 4
PUTFIELD local/lito381/server/NativeLightAction$Batch.targetReceipt:Llocal/lito381/server/NativePlayerVitalCompletions$Receipt;
GETSTATIC local/lito381/server/NativeLightAction.current:Ljava/lang/ThreadLocal;
INVOKEVIRTUAL java/lang/ThreadLocal.get()Ljava/lang/Object;
CHECKCAST local/lito381/server/NativeLightAction$Batch
ASTORE 7
GETSTATIC local/lito381/server/NativeLightAction.current:Ljava/lang/ThreadLocal;
ALOAD 6
INVOKEVIRTUAL java/lang/ThreadLocal.set(Ljava/lang/Object;)V
ALOAD 5
INVOKEINTERFACE local/lito381/server/NativeSelfCastAction$Task.run()V itf
ALOAD 7
IFNONNULL L83
GETSTATIC local/lito381/server/NativeLightAction.current:Ljava/lang/ThreadLocal;
INVOKEVIRTUAL java/lang/ThreadLocal.remove()V
GOTO L86
GETSTATIC local/lito381/server/NativeLightAction.current:Ljava/lang/ThreadLocal;
ALOAD 7
INVOKEVIRTUAL java/lang/ThreadLocal.set(Ljava/lang/Object;)V
ALOAD 6
GETFIELD local/lito381/server/NativeLightAction$Batch.outputs:Ljava/util/List;
INVOKEINTERFACE java/util/List.isEmpty()Z itf
IFEQ L94
ALOAD 6
GETFIELD local/lito381/server/NativeLightAction$Batch.completions:Ljava/util/List;
INVOKEINTERFACE java/util/List.isEmpty()Z itf
IFNE L147
ALOAD 1
ALOAD 3
ALOAD 4
INVOKESTATIC local/lito381/server/NativePairedCastSafety.transfer(Llocal/lito381/server/ClientSession;Llocal/lito381/server/ClientSession;Llocal/lito381/server/NativePlayerVitalCompletions$Receipt;)V
ALOAD 0
GETFIELD local/lito381/server/NativeLightAction$Ingress.receipt:Llocal/lito381/server/NativePlayerVitalCompletions$Receipt;
ALOAD 6
INVOKESTATIC local/lito381/server/NativePlayerVitalCompletions.enqueueLocked(Llocal/lito381/server/NativePlayerVitalCompletions$Receipt;Ljava/lang/Runnable;)V
GOTO L147
ASTORE 8
ALOAD 4
INVOKESTATIC local/lito381/server/NativePairedCastSafety.abandon(Llocal/lito381/server/NativePlayerVitalCompletions$Receipt;)V
ALOAD 8
ATHROW
ASTORE 9
NEW java/io/UncheckedIOException
DUP
ALOAD 9
INVOKESPECIAL java/io/UncheckedIOException.<init>(Ljava/io/IOException;)V
ATHROW
ASTORE 10
ALOAD 7
IFNONNULL L120
GETSTATIC local/lito381/server/NativeLightAction.current:Ljava/lang/ThreadLocal;
INVOKEVIRTUAL java/lang/ThreadLocal.remove()V
GOTO L123
GETSTATIC local/lito381/server/NativeLightAction.current:Ljava/lang/ThreadLocal;
ALOAD 7
INVOKEVIRTUAL java/lang/ThreadLocal.set(Ljava/lang/Object;)V
ALOAD 6
GETFIELD local/lito381/server/NativeLightAction$Batch.outputs:Ljava/util/List;
INVOKEINTERFACE java/util/List.isEmpty()Z itf
IFEQ L131
ALOAD 6
GETFIELD local/lito381/server/NativeLightAction$Batch.completions:Ljava/util/List;
INVOKEINTERFACE java/util/List.isEmpty()Z itf
IFNE L145
ALOAD 1
ALOAD 3
ALOAD 4
INVOKESTATIC local/lito381/server/NativePairedCastSafety.transfer(Llocal/lito381/server/ClientSession;Llocal/lito381/server/ClientSession;Llocal/lito381/server/NativePlayerVitalCompletions$Receipt;)V
ALOAD 0
GETFIELD local/lito381/server/NativeLightAction$Ingress.receipt:Llocal/lito381/server/NativePlayerVitalCompletions$Receipt;
ALOAD 6
INVOKESTATIC local/lito381/server/NativePlayerVitalCompletions.enqueueLocked(Llocal/lito381/server/NativePlayerVitalCompletions$Receipt;Ljava/lang/Runnable;)V
GOTO L145
ASTORE 11
ALOAD 4
INVOKESTATIC local/lito381/server/NativePairedCastSafety.abandon(Llocal/lito381/server/NativePlayerVitalCompletions$Receipt;)V
ALOAD 11
ATHROW
ALOAD 10
ATHROW
RETURN
```

</details>

<details>
<summary>right／NativeLightAction／lambda$content$feb21681a7a728f6083a06149f5c2fd605497b164c3f5581edc8c1a2fe120f3c</summary>

```text
lambda$content$feb21681a7a728f6083a06149f5c2fd605497b164c3f5581edc8c1a2fe120f3c(Llocal/lito381/server/NativeLightAction$Batch;Llocal/lito381/server/NativeSelfCastAction$Task;)V
(Llocal/lito381/server/NativeLightAction$Batch;Llocal/lito381/server/NativeSelfCastAction$Task;)V
0xa

java/io/IOException
||||pvis=|pinv=|lvtvis=|lvtinv=

TRY
ALOAD 0
GETFIELD local/lito381/server/NativeLightAction$Batch.accepted:Llocal/lito381/server/NativeLightAction$Ingress;
GETFIELD local/lito381/server/NativeLightAction$Ingress.binding:Llocal/lito381/server/NativePlayerCommit$Binding;
ALOAD 0
GETFIELD local/lito381/server/NativeLightAction$Batch.pc:Llocal/lito381/server/ClientSession;
INVOKEVIRTUAL local/lito381/server/NativePlayerCommit$Binding.matches(Llocal/lito381/server/ClientSession;)Z
IFEQ L10
GOTO L8
ALOAD 1
INVOKEINTERFACE local/lito381/server/NativeSelfCastAction$Task.run()V itf
RETURN
```

</details>

## 保留的治療鎖結構證據

下列 javap 證據來自前次已提交的修補報告；本輪沒有重新將這項歷史檢查計為新測試。對應 Java 未變，本輪重新編譯後整個 NativeProcHealingPlayer 仍為 SAME。

### 治療方法 javap 鎖定結構對照

執行 `javap -p -c -s -classpath <staged 或 candidate JAR> local.lito381.server.NativeProcHealingPlayer`，擷取 `nativeSourceEquipmentProcOriginalApply`。

| 操作 | staged offset | candidate offset |
|---|---|---|
| vitalLock.lock | 212 | 212 |
| status monitorenter | 220 | 220 |
| waterLife monitorenter | 269 | 269 |
| 建立 Target／呼叫建構子 | 333／344 | 333／344 |
| 核心 apply | 372 | 372 |
| 登記 healing completion | 391 | 391 |
| 正常釋放 waterLife／status | 396／410 | 396／410 |
| 正常 vitalLock.unlock | 427 | 427 |
| finally vitalLock.unlock | 440 | 440 |
| receipt close／drain（正常） | 451／455 | 451／455 |
| receipt close／drain（例外） | 465／469 | 465／469 |

兩側均 2 個 monitorenter、7 個 monitorexit。以下 exception table **兩側逐字相同**；400 為 waterLife handler、414 為 status handler、433 為 vitalLock finally、461 為 receipt finally。第一個 monitor 前的 `215..244` 等區段亦受 vitalLock finally 保護。正常及提早返回均先釋放內層 monitor、外層 monitor，再 unlock，最後 close／drain。

```text
       from    to  target type
         270   310   400   any
         333   397   400   any
         400   405   400   any
         221   244   414   any
         264   313   414   any
         333   411   414   any
         414   419   414   any
         215   244   433   any
         264   313   433   any
         333   422   433   any
         433   435   433   any
         207   252   461   any
         264   321   461   any
         333   449   461   any
         461   463   461   any
```

已以程式核對兩側所有 monitor、鎖／檢查／Target／apply／completion／close／drain 指令 offset 及 exception table，在移除常數池編號與空白差異後完全一致。整段 javap 仍有局部槽位與常數池編號差異，因此沒有宣稱 byte-identical；正式 semantic-diff 將該整類列為 SAME。
