# Issue #68 — T020-B07 戰鬥併入暫存

## 結論

**未達全部 SAME 的驗收條件，不可標記完成或交 B-land 落地。** 已交付 23 份可編譯 staged Java；56 個 class entries 完整，語意結果為 **51 SAME／5 DIFF／0 ONLY_LEFT／0 ONLY_RIGHT**。沒有新增比較豁免、修改比較器或以候選 class 覆蓋 javac 產物。

分支 `task/68`，工作基準 `450178d59b0157fddf2b9456b7c03d20207993fc`，包含 #102、#153、#166。所有寫入均在指定 worktree；沒有 checkout／新建分支。未修改 `src/main`、`pom.xml`、`docs/REPORT.md`、機密設定、封存、硬連結或正式產物，未啟動遊戲／伺服器，也未連 DB。

## 做了什麼

- 核對 `batches.json` B07 的 23 個外層類別及 56 個 `verify_entries`。全部有既有來源，以 `src/main` 複本為底，用 CFR 0.152 同時反編譯正式及候選 B07 entries，移植差異並補上既有來源落後正式版的內容；保留原始註解及未變動方法。
- 移植技能目標 metadata、傷害顯示、回血、負面狀態、管理員判定參數、技能消耗、候選轉呼叫入口、區域目標／發布／召喚生命週期。保留候選中的 original-body 方法與未被呼叫但仍存在的 lambda 本體。
- `NativeReturnNatureMaterialAction.Target` 使用 `@AsRecord(components={"caster","owner","pet","stamp"})`，保留額外召喚擁有權／發布／刪除欄位，建置後轉換為 record。
- `NativeAreaMaterialAction.Context` 使用既有 `@AsmFinal` 還原 `players`、`bindings`、`population`、`nativeSourceMagicSnapshot` 的 final 旗標，表達 ASM 建構子中的重設或未賦值路徑；沒有補上候選不存在的初始化。
- 對 CFR 的誤還原逐項核對 bytecode：修復 `ownerContext`／`ownerDeletionContext` 重複建構的假象、lambda 區域名稱遮蔽、迴圈分支及多重 catch 順序。未直接將 CFR 警告視為可忽略。

## 已驗證

環境：Windows、Microsoft OpenJDK 17.0.17、Python 3.12、CFR 0.152、ASM 9.7。比較器從本分支 Java 來源重新編譯，未使用舊的工具二進位檔。

| 輸入 | SHA-256 |
| --- | --- |
| `reference/base-0.6.71.jar` | `e1498871c7be6b287821c57ef5d95933db1740d9b4d4a80f6c3465f21d99d498` |
| `reference/candidate-50635f7e.jar` | `50635f7edca8ec6ce77787cc32371c7590b497e48ea202f8a679caf3f517b729` |
| CFR 0.152 | `f686e8f3ded377d7bc87d216a90e9e9512df4156e75b06c655a16648ae8765b2` |

本輪最後一次 javac／轉換器／semantic-diff 實際輸出：

```text
Note: Some input files use unchecked or unsafe operations.
Note: Recompile with -Xlint:unchecked for details.
[javac exit=0]
as-record: scanned=56 converted=2
SUMMARY SAME=51 SAME_EFFECTS=0 SAME_LAYOUT=0 DEVIATION=0 DIFF=5 ONLY_LEFT=0 ONLY_RIGHT=0 BYTE_SAME=0 ALLOW_METHODS=0
{"missingLeft": [], "extraLeft": []}
```

兩個轉換 entry 為 `NativeReturnNatureMaterialAction$Target` 與 `NativeAreaMaterialAction$Context`，兩者均 SAME。其他所有內部／匿名類亦 SAME。18 個外層類別整類 SAME；其餘 5 個外層的未通過方法如下。另以 class 目錄與 ZIP entry 集合核對，沒有將 annotation 類別或任何依賴混入本批產物。

### 尚未通過的方法與具體證據

| 類別／方法 | 已觀察的差異與後續處理 |
| --- | --- |
| `CombatCompanionSpells.nativeShockCompanionBody` | 候選保留原同步區塊的 `ALOAD; DUP; ASTORE; POP`、各出口 `ALOAD; POP` 與 Throwable handler，但已沒有 caster 的 MONITORENTER／MONITOREXIT。Java staged 不重新加入 caster monitor，目前例外表、局部暫存及堆疊運送仍 DIFF。已按 bytecode 修正 `spellReadyNanos`／HP／MP 的讀取順序與 boundMp 呼叫位置。 |
| `CombatNpcDebuffs.nativeMaterialControlledNegativeBody` | 同樣保留移除 caster monitor 後的 POP／多段 Throwable handler。候選使用第 6 個 boolean 參數作管理員判定，staged 已使用該參數；另保留技能消耗的 intelligence／original reduction 呼叫順序。仍有例外表與堆疊運送差異，不能只修第一個 TRY 後就認定全方法 SAME。 |
| `CombatPoisonLifecycle.nativeRawOriginalBattle` | 正常指令／呼叫順序接近，但候選外層 Throwable 保護為連續區間，涵蓋提前 return 與 leaveClock；javac 產物切成多段，#166 未將這些區間判成相同。正式與候選原本都有此結構，並非僅本批新增 bridge 的問題。 |
| `CombatRules.load` | 候選在 `Map.put` 的 receiver／key 已留在 stack 時，對 `read(character)` 結果 DUP，插入 `NativePetControls.loadFromProperties`，再繼續 `awakeClassicMode`。staged 使用具名暫存保留同一 Properties 與前綴資料；目前比較器未證明整段運送相等，第一差異為 ASTORE／字串常數。 |
| `NativeAreaMaterialAction.nativeTargetOriginalCaptureOutput` 及其發布 lambda | 候選由 bound method reference 改為靜態 proof handle，但保留 `Objects.requireNonNull`。staged 已在相同鎖分支與 stream 建立後加入檢查；lambda 的暫存、布林分支版面使內容雜湊不同，進而呼叫點仍 DIFF。其餘區域方法、`nativeCompanionAreaCapturedRun` 及所有 Context 建構子皆已通過逐方法診斷。 |

逐方法診斷使用同一 `JarIndex`／`ClassComparer.compareMethod`，不是另一套較寬鬆的驗收；它只用於列出整類第一差異以外的缺口。最後五類合計六個方法未通過（發布 lambda 左右各列一個 unmatched identity，視為同一待核對本體）。

## 怎麼驗證／重跑

本 worktree 的完整重跑入口（腳本僅在 scratch，不入 git）：

```powershell
& 'C:/Users/CHAOREN/AppData/Local/Programs/Python/Python312/python.exe' -B -X utf8 scratch/issue-68/verify.py
& 'C:/Users/CHAOREN/AppData/Local/Programs/Python/Python312/python.exe' -B -X utf8 scratch/issue-68/inspect.py
```

`verify.py` 每次刪除並重建本批 classes 目錄，先單獨編譯既有 `AsRecord.java`／`AsmFinal.java` 到 `target/issue-68/annotations`，再使用候選、正式、lib 與 annotation 目錄作 classpath。annotation 目錄只供編譯，不打入比較 JAR。其核心指令等同下列 PowerShell（從伺服器目錄執行，`$sources` 為 B07 的 23 個 staged 檔）：

```powershell
$jdk = 'C:/Program Files/Microsoft/jdk-17.0.17.10-hotspot/bin'
$cp = 'reference/candidate-50635f7e.jar;reference/base-0.6.71.jar;lib/*;target/issue-68/annotations'
& "$jdk/javac.exe" --release 17 -encoding UTF-8 -cp $cp -d target/issue-68/classes @sources
$env:JAVA_HOME = 'C:/Program Files/Microsoft/jdk-17.0.17.10-hotspot'
$env:AS_RECORD_MAVEN_REPO = 'D:/dev-cache/maven/repository'
& 'C:/Users/CHAOREN/AppData/Local/Programs/Python/Python312/python.exe' -B -X utf8 tools/as-record/as_record.py target/issue-68/classes
# 打包 target/issue-68/classes 下全部 class，不能從 reference 複製任何 class。
& "$jdk/jar.exe" --create --file target/issue-68/staged.jar -C target/issue-68/classes .
```

比較器主類別（`$toolCp` 為本分支編譯的 semantic-tool 目錄加 ASM／asm-tree／asm-analysis 9.7）：

```powershell
& "$jdk/java.exe" -cp $toolCp local.lito381.semanticdiff.SemanticDiff `
  --left target/issue-68/staged.jar --right reference/candidate-50635f7e.jar `
  --left-classpath reference/candidate-50635f7e.jar `
  --classes ../../scratch/issue-68/entries.txt `
  --json ../../scratch/issue-68/semantic.json --md ../../scratch/issue-68/semantic.md
```

`entries.txt` 必須從 `docs/combat-merge/batches.json` 的 B07 `verify_entries` 重新產生；完整陣列參數留在 `scratch/issue-68/compile-command.json`。語意 CLI 的退出碼不代表全部 SAME，必須檢查 counts 與 missingLeft／extraLeft。

## 推測

五類殘留差異可能需要額外且有正反例證明的比較器正規化，或受控的建置轉換，才能在保留候選完整語意的同時表達為 Java。這是接手方向，**不是等價證明或豁免**；目前尚未證明五類的全部參數資料流、例外邊與並行行為等價。

## 待驗證／未驗證什麼

- 上述 5 類尚未全數 SAME；本任務驗收未完成。
- 未執行單元測試／行為回歸，依 §6-5 在 B-land 統一處理；沒有執行 staged。
- 未做登入、戰鬥、正常退出的真機驗收或正式部署。
- GitHub connector 讀留言失敗（Unknown tool）；`gh issue view 68 --comments` 回報 HTTP 401。`Target` 的裁定以本次指派文字為依據，沒有宣稱已讀到主控完整留言。

## 產出路徑

- 交付：`lito伺服器/LiTo381-Server/merge-inputs/50635f7e/staged/local/lito381/server/` 下 B07 的 23 份 `.java`。
- 交付：`docs/reports/issue-68.md`。
- 未入 git 的診斷／證據：`scratch/issue-68/{base-0.6.71,candidate-50635f7e,raw,instructions}/`、`*.diff`、`javac.log`、`as-record.log`、`semantic.{json,md,log}`、`compile-command.json`、`audit.json`。
- 未入 git 的編譯產物：`lito伺服器/LiTo381-Server/target/issue-68/{annotations,classes,semantic-tool,staged.jar}`；轉換器自己的快取位於 `tools/as-record/target/`。

## 給下一棒的提示

請雲端主控／Claude 先審查六個未通過方法，決定是否另派 semantic-diff 或建置轉換器工作；不要直接放寬本批驗收或將 51 SAME 計為 56 SAME。原始 JAR、全部 staged 及前置工具已在 repo，可在有 JDK 17 的環境重建，無需正式 DB／遊戲或封存資源。若更新工具，接手執行器須重跑完整 56 entries，確認 SAME=56 且無缺漏／多出後，再交 B-land。
