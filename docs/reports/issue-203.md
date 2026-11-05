# Issue #203：T020i-8 semantic-diff 後備證明（第 2 次修正）

## 結論／做了什麼

已重現並修復 CFG 的 stack 誤判：舊工具把 `ILOAD; GOTO` 當作空區塊收縮，導致實際回傳不同值的方法被判 SAME_CFG。現在只有入口／出口 stack 表示式完全相同的 GOTO 區塊才可收縮，並明確核對全部正常／例外邊的 stack 邊界與前驅來源。七套 Java 自我測試共 **1398 checks 全過**；指定七批共 **1016 entries** 的核准模式均為 **0 DIFF**，完整逐類別及逐方法結果與本輪修正前完全相同，沒有原本 0 DIFF 變回 DIFF 的方法。

工作目錄為 `D:/lineage開發/3-lito開發/scratch/wt/task-203`，沿用 `task/203`，本輪比較器基線為 `1f5900d7c7cb292e3e4cd724aeebb1379f74cbfe`（前次交付）；首次實作基線 `bccc2cb746e408bc8a496cf46c224d61616b9076` 的對照保留於下文。只修改 `tools/semantic-diff/**` 及本報告；未改 src/main、staged Java、核准清單、docs/REPORT.md 或正式程式。沒有啟動伺服器、登入器、遊戲，沒有讀取機密設定、封存或連線 DB。

### 審查疑慮的實際重現與修正

先用尚未修改的前次程式編譯至 `target/issue-203/review-baseline`，再以 ASM 產生有效 class；同一份 framed JAR 交給隔離 ClassLoader 與比較器。JVM 使用 `-Xverify:all`。

| 案例 | JVM 左／右（input=2） | 修正前 CLI | 修正後 CLI |
|---|---|---|---|
| 常數 `c ? 1 : 2` 對 `c ? 2 : 2` | 1／2 | DIFF | DIFF |
| 相同分支，但以 local 載入值後 GOTO 匯合 | 1／2 | **SAME_CFG（誤判）** | DIFF |
| 兩個分支的 local stack 值互換 | 1／2；input=0 為 2／1 | **SAME_CFG（誤判）** | DIFF |
| 匯合點 stack 深度左 1／右 2，右側相加後回傳 | 1／3；input=0 為 2／4 | DIFF | DIFF |
| 迴圈 stack 累積值不同 | 3／5 | DIFF | DIFF |
| long 值由不同 local 經 GOTO 傳入匯合點 | 1／0 | **SAME_CFG（誤判）** | DIFF |

常數案例沒有重現誤判，不能把審查推論直接記成既成事實。實際漏洞在 `CfgComparison.target`：#174 將純 LOAD 表示成值來源，因此 `ILOAD n; GOTO join` 的事件清單只剩 GOTO；舊收縮條件沒有檢查入口／出口 stack，刪掉了推入的 local。兩邊只剩相同的入口與 `IRETURN stack[0]`，分支值關聯遺失。

最小 local 反例的共同前綴為 `ICONST_1; ISTORE 1; ICONST_2; ISTORE 2; ILOAD 0; IFEQ no`；true 分支左側 `ILOAD 1; GOTO join`，右側 `ILOAD 2; GOTO join`；false 分支兩邊均 `ILOAD 2; GOTO join`；`join: IRETURN`。這是有效且可執行的 bytecode，並非用 VerifyError 冒充行為差異。

修正包含：

1. `BlockExpressions.Block` 明確保留入口 stack 位置與 category 寬度；原有區塊比較也檢查入口形狀。
2. GOTO 收縮除了原有事件／註解／copy 條件，另要求入口與出口 stack 表示式逐項完全相同。帶 LOAD 的推值區塊保留；非空 stack 上純 GOTO 的正例仍可收縮。
3. CFG 完成入口確定雙射後，逐一建立每個入口的「前驅區塊＋邊標籤＋stack 位置」來源。值使用區塊內表示式 DAG；event 引用連同 opcode／有序 operands 已逐項比較，繼承值引用該前驅的入口位置。保持每條邊的關聯，不把來源混成無序 union。
4. 正常邊比對出口 stack 深度、category 寬度與內容，並檢查其符合目的區塊入口；例外邊以事件位置／handler 次序識別，清空正常 stack、傳入該事件拋出的單一例外值。迴圈比較同一套邊界方程，無法證明即拒絕 SAME_CFG。

新增三個正例為：分支反向並互換後繼的 local stack 匯合、帶非空 stack 的迴圈與 GOTO trampoline、long stack 匯合。以上六個反例與三個正例均雙向直接呼叫 CFG；反例另驗證預設 CLI，正例關閉其他證明以確認確實走 SAME_CFG。每案用 input=0、2 實際執行，回傳型別須為 Integer，避免把相同例外誤當通過。

深度反例左右各自是有效方法：左側每個前驅都推 1 值，右側每個前驅都推 2 值；右側在匯合點相加再返回。若單一方法的同一匯合點收到不同深度，JVM 本來就會拒絕，不能滿足「實際執行且行為不同」的要求。

### 實作範圍

- `LambdaCallsites`：內容雜湊未配上時，從相同外層方法的 INVOKEDYNAMIC 次序取得候選，檢查完整 bootstrap／SAM／捕獲描述子／實作 handle；沿用 #166 捕獲排列。從內向外執行完整 `compareMethod`，成功才建立共同身分。雙射、所有共用呼叫點及巢狀相依均須一致；失敗回復整個類別的候選交易。已經由內容雜湊證明相等的共用 Lambda 保持既有規則，不加入後備候選的雙射。
- `CfgComparison`：在線性與 paths 失敗後預設執行。沿用 #174 區塊值來源，收掉純 GOTO 區塊，反向 opcode 同時交換後繼；從入口按正常邊及有序例外邊建立確定雙射。保留操作順序、值來源、STORE／IINC 版本、出口 stack、switch key 與 handler 優先序，不任意配對區塊。
- `CfgTransport`：為 #67 新增精確的重複保存消去，只有連續 `DUP[2]; STORE a; STORE b`、非參數槽、唯一寫入及每次讀取的唯一 reaching definition 都成立才合併。覆寫、分支寫入、IINC、註解及 wide 重疊一律拒絕。CFG 比較以每個實際使用點及出口 stack 的值來源為準，允許 DUP 與重新讀取未覆寫 local 的複本數不同。
- `VisibilityBridge`：僅左側 0x1041、public 本類、順序載入參數、INVOKESPECIAL 直接父類同名同描述子及對應 RETURN；兩側解析到同一非 public 宣告祖先。右側缺宣告、本類只有唯一同名同描述子方法；cast、轉接其他方法、public 祖先等均拒絕。
- JSON／Markdown／CLI 分列 SAME_CFG、LAMBDA_PAIRED_BY_CALLSITE、VISIBILITY_BRIDGE；`proofMethodCounts` 顯示方法數，避免被類別 DEVIATION 優先狀態遮住。保留配對本體的證明理由與路徑數。
- `EighthSelfTest` 接入正式 `selftest.sh`。舊兩項消融測試加上關閉 cfg，避免另一個獨立證明遮住停用原規則的效果。

## 已驗證／怎麼驗證

### 自我測試

Microsoft OpenJDK 17.0.17；ASM、asm-tree、asm-analysis 9.7。全部 Java 自測以 `-Xverify:all` 執行，七套 checks 分別為 **517、73、119、169、159、96、265**，總計 **1398**。本輪增加 105 checks。新增測試的比較器與 platform-parent 隔離 ClassLoader 讀取同一份 framed JAR；只執行測試 Fixture，未載入遊戲應用程式。下列原任務的反例亦全部重跑通過。

| 必要反例 | JVM 左／右 | 工具 |
|---|---|---|
| 同描述子 Lambda 交換呼叫點 | 1234／3412 | DIFF |
| Lambda 某條路徑呼叫次序不同 | 1234／2134 | DIFF |
| 巢狀內層 Lambda 不同 | 12／21 | DIFF |
| 共用 Lambda 的呼叫點對應不一致 | 1212／1234 | DIFF |
| 兩個 Lambda 候選對到同一個目標 | 1234／1212 | DIFF |
| Lambda 內的迴圈少一個呼叫 | 12121／111 | DIFF |
| 迴圈條件反向但後繼未互換 | 12121／1 | DIFF |
| 迴圈本體少一個呼叫 | 12121／111 | DIFF |
| 回邊跳過原先入口操作 | 12121／122 | DIFF |
| handler 行為不同 | 42／99 | DIFF |
| 別名其中一槽被覆寫 | 88／154 | DIFF |
| 分支內改寫別名槽 | 88／154 | DIFF |
| DUP 換成不同值來源 | 42／32 | DIFF |
| bridge 轉呼叫其他方法 | 4／3 | DIFF、保留 ONLY_LEFT |
| bridge 帶 CHECKCAST | ClassCastException／1 | DIFF、保留 ONLY_LEFT |
| public 祖先出現額外 bridge，觀察 declared methods 數 | 2／1 | DIFF、保留 ONLY_LEFT |
| 子類覆寫：super-forwarding 與虛擬派送不同 | 1／2 | DIFF |

正例涵蓋：跨分支 local／stack 的 Lambda 本體、巢狀 Lambda、所有呼叫點一致共用 Lambda、Lambda 本體走 SAME_CFG、含迴圈的條件反向加後繼互換、GOTO trampoline、有例外邊的 CFG、重複保存別名、DUP 與 local 讀取、可見性 bridge、其他描述子的 overload。Lambda 與 CFG 消融各自恢復 DIFF；既有 #174／#176／#186 的記憶體版本、例外涵蓋、monitor 與繼承參照反例仍全部通過。

另有兩個保守拒絕案例不宣稱執行行為不同：bridge 指向祖父但 ACC_SUPER 仍選中直接父類，左右均回傳 5；以及右側祖先方法非 public 且未提供既有可見性核准，左右均回傳 3。工具皆保留 DIFF；後者只有加入精確 access-widen 核准才接受。

### 固定輸入

先核對各指定提交報告中的 SHA-256，再將六個既有 staged JAR 以一般檔案複製至本 worktree `tools/semantic-diff/target/issue-203/`。#76 原 worktree 已不存在，因此用目前已合併的 27 份 staged Java 與相同 JDK／固定 JAR 時間重新編譯，重建 SHA 與既有報告完全相同。沒有複製候選 class 覆蓋輸出或改動 staged。

| 批次 | 來源提交 | entries | staged JAR SHA-256 |
|---|---|---:|---|
| #62 B01 | e2753408400367b14306a79f2e0403304537952b | 22 | e05d3f432413ecc65aa3f4f5a1d68b196de634f954935230763b443bfa92501f |
| #65 B04 | 86a4f4b3ab8c66a702a1da84ffe9194f8153eddc | 98 | da35c6b2d6cf165b1afdfefb6c1409bf69c4aff7a66f5917e9cb9bc6fe0cc92e |
| #67 B06 | b900eae0157336d916d6a76b445aeb81ee7e018b | 323 | 8820abf5684b6fd6c9c55826372a21bad10f5f965880d4a954e57fe22d1640c6 |
| #72 A01 | 2ac8ba54a915b9a746c07a61b3d355fb772bae69 | 185 | 63e970a0ed1bd26f94586b79df3de840e6231b46977ce63215d5427e55363808 |
| #63 B02 最新 | 23200d027c806204010234dba3f33622dc419b72 | 108 | 9cc4d4cbac153d7f13ffea5864fb7ac34994618bd7717c4a2900bd36080f38ef |
| #66 B05 最新 | 5380ca3a1ea7999493875103bfb987c18327fbf6 | 120 | b8d54f8315093327943495503edb9e7dbb0a725d73ea10da012d37738f1d52e4 |
| #76 A05 最新 | 0d3101a7d048b116d925022952ba5d3912e82ef8 | 160 | e347988dcf41b60cb915195ffa98dc2eff74303ae3c9f0400c2800bf5bc2b553 |

B 批 reference 為 candidate：`50635f7edca8ec6ce77787cc32371c7590b497e48ea202f8a679caf3f517b729`；A 批為正式 0.6.71：`e1498871c7be6b287821c57ef5d95933db1740d9b4d4a80f6c3465f21d99d498`。兩者皆只讀；left classpath 指向該批相同 reference。

每批清單從其指定提交 `batches.json`／`a-batches.json` 取得，JAR class 集合精確等於 verify_entries。核准檔亦從相同提交裁切至精確 owner；#62 為 effects 1 條及 exceptions 1 條；#65 為既有 method-diff 2 條；#67／#63 沒有核准；#66 為既有 effects 7 條；#76 為 effects／layout 各 1 條。#72 使用指定提交的既有 access-widen 與 deviations（包含四個建構子及 compute 的 method-diff），未新增任何核准。

本輪以 `git ls-remote` 核對 #63／#66／#76 分支最新提交，均為表內 SHA。#76 的 staged JAR 與該提交報告 SHA 完全相同；該較早提交尚無 exceptions-exempt 檔，不虛構或新增核准，其 effects／layout 清單與前次相同。

### 本輪修正前後對照（類別數）

修正前為 `1f5900d`。七批的完整 `classes`（含 methodResults）、`counts`、`classSelection` 均逐項相等；核准模式全部 DIFF／ONLY_LEFT／ONLY_RIGHT 為 0。無核准模式也完整相同。

| 批次 | 核准模式 DIFF 前→後 | 保留的類別證明 | 無核准 DIFF 前→後 |
|---|---|---|---|
| #62 | 0→0 | 20 SAME、1 SAME_EFFECTS、1 LAMBDA_PAIRED_BY_CALLSITE | 1→1 |
| #65 | 0→0 | 95 SAME、1 SAME_PATHS、1 SAME_CFG、1 DEVIATION | 1→1 |
| #67 | 0→0 | 311 SAME、10 SAME_PATHS、2 SAME_CFG | 0→0 |
| #72 | 0→0 | 183 SAME、2 DEVIATION；方法另列 169 VISIBILITY_BRIDGE | 3→3 |
| #63 | 0→0 | 107 SAME、1 SAME_PATHS | 0→0 |
| #66 | 0→0 | 115 SAME、3 SAME_EFFECTS、1 SAME_PATHS、1 ORPHAN_IGNORED | 0→0 |
| #76 | 0→0 | 158 SAME、1 SAME_EFFECTS、1 SAME_LAYOUT | 0→0 |

七批沒有方法因本輪修正變回 DIFF。這只描述固定輸入的結果，不以維持 0 DIFF 為由放寬 stack 規則。

### 相對首次實作前 bccc2cb 的核准模式對照（類別數）

簡寫：S=SAME、E=SAME_EFFECTS、P=SAME_PATHS、C=SAME_CFG、L=LAMBDA_PAIRED_BY_CALLSITE、D=DEVIATION、O=ORPHAN_IGNORED、Y=SAME_LAYOUT。

| 批次 | 修改前 | 修改後 |
|---|---|---|
| #62 | 20 S、1 E、1 DIFF | 20 S、1 E、1 L、0 DIFF |
| #65 | 95 S、1 P、1 D、1 DIFF | 95 S、1 P、1 C、1 D、0 DIFF |
| #67 | 311 S、10 P、2 DIFF | 311 S、10 P、2 C、0 DIFF |
| #72 | 183 S、1 D、1 DIFF | 183 S、2 D、0 DIFF |
| #63 | 107 S、1 P、0 DIFF | 完整 classes JSON 相同 |
| #66 | 115 S、3 E、1 P、1 O、0 DIFF | 完整 classes JSON 相同 |
| #76 | 158 S、1 E、1 Y、0 DIFF | 完整 classes JSON 相同 |

七批 ONLY_LEFT／ONLY_RIGHT 類別數均為 0，缺少及多出 entry 均為空。工具 exit 0 本身不作為驗收；另檢查 counts、classSelection、完整 classes 與 methodResults。

| 原阻塞方法 | 新證明 |
|---|---|
| #62 `NativeWeaponBreakTargetLifecycle.lambda$captureOutput$8` → `$6` | LAMBDA_PAIRED_BY_CALLSITE；SAME_PATHS，87 條路徑。外層兩層 Lambda 的內容身分隨內層對齊，整條鏈通過。 |
| #65 `NativeSourceNpcEscape.nativeSourceFirstViewHandled` | SAME_CFG |
| #65 `NativeSourceNpcEscape.nativeSourceNpcPlayerFirstHandled` | SAME_CFG |
| #67 `BattleGroundEffects.cast` | SAME_CFG，經精確重複保存消去 |
| #67 `BattleTimedStatus.cancelMagicEffects` | SAME_CFG，保留所有實際取用點，消除 DUP／ALOAD 複本數差異 |
| #72 `NativeDeInstanceState.lambda$sourceAi$scheduleAiTick$22(JJZ)V` → `lambda$scheduleAiTick$0(JJZ)V` | LAMBDA_PAIRED_BY_CALLSITE；完整線性／既有正規化比較成功 |
| #72 `NativeDeInstanceState` 的 169 個獨有 bridge | 169 VISIBILITY_BRIDGE；類別因原有核准方法差異仍為 DEVIATION |

`get_showId()I` 的特例有實際 metadata 證據：正式 `NativeDeDeletionState` 宣告旗標為 0，staged 為 public；兩側都解析到這個非 public 類別。此差異已在 #153 的精確 access-widen 核准中。未套核准時維持 ONLY_LEFT；套核准後才認為 bridge 是 javac 對該已核准 public 方法產生的可見性轉接。其餘 168 個 bridge 不依賴這個例外。

### 不傳核准清單的對照

| 批次 | 修改前 DIFF | 修改後 DIFF | 說明 |
|---|---:|---:|---|
| #62 | 1 | 1 | captureOutput 的 Exceptions 差異仍需原核准 |
| #65 | 2 | 1 | 僅 Escape 經 CFG 通過，PhysicalNpc 真正方法差異保留 |
| #67 | 2 | 0 | 本批原本就沒有核准 |
| #72 | 3 | 3 | 未核准建構子／compute／access 差異仍保留 |
| #63 | 0 | 0 | 結果不變 |
| #66 | 0 | 0 | 117 S、2 P、1 O 不變 |
| #76 | 1 | 0 | Main.main 原已由 layout-approved 證明同構，現在由預設 CFG 證明；核准模式結果不變 |

## CI

本輪程式尚待推送後的 CI；以前次綠燈作為歷史紀錄，不能當成本輪 stack 修正的驗收。交付前會檢查 [PR #215 checks](https://github.com/chaoren0418-arch/lito-launcher-server/pull/215/checks)，包含 Semantic gap 實際 selftest／fallback 日誌，並更新本節收據。

## 推測

SAME_CFG 與 Lambda 配對是本工具保守事件模型下的證明；不能推論成真機戰鬥已驗收。CFG 不涵蓋的 shuffle、不可達區塊或不唯一候選可能仍有假陰性，會保留差異。

## 待驗證／未驗證什麼

- 本輪 CI 待推送後驗證；本機七套自測與七批固定輸入重跑已通過。
- 本任務未執行 B-land 應用回歸或遊戲實機驗收，也未部署任何產物。

## 產出路徑與重跑

- 交付：`lito伺服器/LiTo381-Server/tools/semantic-diff/**`、`docs/reports/issue-203.md`。
- 本機腳本／證據：`scratch/issue-203/{verify.py,batches-review.py,review-inputs.json,*-review-baseline-*.json,*-after-*.json,stack-before/,StackProbe.java,*.log}`。
- 工具、fixture 及固定輸入複本：`lito伺服器/LiTo381-Server/tools/semantic-diff/target/issue-203/`；不進 git。
- 原始六批 JAR 依次位於各 `scratch/wt/task-N`：#62 `target/issue-62/final-174-176/staged.jar`、#65 `target/issue-65/recheck-176/b04.jar`、#67 `target/issue-67/replay/staged.jar`、#72 `scratch/issue-72/target/final-reproduce.jar`、#63 `target/issue-63/final-174-176/compiled.jar`、#66 `target/issue-66-verified-176/batch.jar`。除 #72 外，target 前綴為 `lito伺服器/LiTo381-Server/`。

```powershell
& D:/dev-orchestrator/tools/python/python.exe -B -X utf8 scratch/issue-203/verify.py build tests
& D:/dev-orchestrator/tools/python/python.exe -B -X utf8 -c "import sys,runpy;sys.path.insert(0,'scratch/issue-203');runpy.run_path('scratch/issue-203/batches-review.py')"
```

跨平台自測入口為工具的 `selftest.sh` 與 `selftest-fallback.sh`。固定批次 CLI：

```text
semantic-diff --left <staged.jar> --right <reference.jar>
  --left-classpath <reference.jar> --classes <verify_entries.txt>
  --json <result.json> --md <result.md>
  [僅加入指定提交、該批精確裁切的既有核准旗標]
```

## 給下一棒的提示

由 Claude 優先審查 GOTO 收縮的入口／出口 stack identity、正常／例外邊的 predecessor 與表示式關聯，以及非空 stack 的回邊；三個已實際重現的舊誤判均須維持 DIFF。另審查候選交易回復、共享呼叫點雙射、別名消去前提及 #72 get_showId 的既有核准邊界。核對 `proofMethodCounts` 與逐方法證明，不要把 #72 的 DEVIATION 類別誤當成 SAME 或漏看其 169 個 bridge。審查通過後由使用者合併；調度器可讓 #62／#65／#67／#72 用新版工具更新驗收並接續既有落地流程，無需再改 staged Java 或新增豁免。
