# Issue #166 — T020i-4 semantic-diff 第四批正規化

## 結論

第二次嘗試修正 PR #170 的 P1：transport 不再用多來源 union 證明等價，無法保留前驅邊／取值關係時保守拒絕。六項規則仍預設啟用，程式及測試只修改 `tools/semantic-diff/`。固定 staged 重跑後，核准清單模式的 DIFF（#153 基準 → 退回版 → 本次修正）：#62 **8 → 5 → 7**、#66 **4 → 2 → 3**、#72 **4 → 5 → 5**、#76 **2 → 0 → 1**。未新增任何核准。

分支取值互換反例已從錯誤 SAME 恢復 DIFF；662 項 Java 自測通過。代價是四個 entry 恢復 DIFF：#62 ControlledNegativeMaterial／HealingCast、#66 ProcScalarSkills、#76 Main。**原驗收的 expiry 消除與 #76 零 DIFF 尚未達成**；WindTargets.run、`_isBackgroundMode`、forMap、Main.accept 的改善保留。這是審查允許的保守拒絕方案，不宣稱已證明這四個 entry 不等價。

未修改 src/main、staged Java、核准清單、docs/REPORT.md、機密設定、封存、正式產物或原始專案工作檔；git commit／push 僅依指示更新共用 Git 中繼資料。沒有啟動伺服器／登入器／遊戲，也沒有連 DB。

交付分支 `task/166`，[PR #170](https://github.com/chaoren0418-arch/lito-launcher-server/pull/170)。本次 CI 證據見下方「CI」，前次綠燈不當作本次驗證。

## 做了什麼

- `FourthNormalizations`：標準 metafactory 轉呼叫 handle 收斂、無其他參照時移除合成方法、保留 handler 優先序的相鄰 try 合併、捕獲參數與呼叫點一致置換。巢狀 lambda 依子方法先、呼叫者後的順序處理；直接呼叫、LDC、condy、非標準 bootstrap、外類參照均阻止方法移除／捕獲置換。
- `LocalWebs`：ASM reaching definitions 分析正常與例外控制流程，合併同一讀取可達的定義；web 依首次出現順序編號。包含 wide、IINC、diamond、loop 與 handler。精確核准建構子保留實體槽，維持 #153 ctor-prologue-order 的覆寫與欄位寫入守門。
- `TransportComparison`／`LayoutComparison`：對單寫入 web 的每個 LOAD 證明定義來源，消去運送指令後，核對每個運算的有序值來源、每個來源的複本數、其餘指令順序、正常與有序例外邊。本次新增硬性前提：保留指令的每個 operand 與每個被複製值都必須恰有一個 producer；否則整個方法拒絕 transport 證明。圖建立時在 label 映射之前再次守門，避免不同來源被折成同一節點。預設與 layout 核准模式共用此限制，既有逐指令／核准比較仍照常執行。stack 值在未使用路徑的 POP／POP2 可以對應 local 不再讀取，但其產生指令仍保留。只有原本核准 layout 的方法容許 CFG 排列不同。
- `JarIndex`：分側解析 field owner，宣告／介面／父類查找保留同名遮蔽；主 JAR 優先於明確 classpath，解析失敗標為 `unresolved(原 owner)`，不讓未知符號偶然等於對側宣告。只讀平台 `java/*` metadata，不初始化應用類別。新增可重複的 `--left-classpath`（別名 `--classpath`）與 `--right-classpath`，接受 JAR／class 目錄並記錄於報告。
- Maven、Bash fallback、selftest 接入 ASM 9.7 `asm-analysis`，與其他 ASM 依賴一起校驗 SHA-256。原有核准 effects 摘要沿用 web 切分前指令，避免改變既有核准結果。

每項詳細前提與保守拒絕情況見 `tools/semantic-diff/README.md`。分析失敗、未解析值、local 型別註解、舊式 subroutine、未支援 stack shuffle 不會視為證明成功。

## 已驗證

### 固定輸入與比較方式

基準比較器是 `10b6b2065fde545e9bbe36b6c9aa101ac2b2c4c4`（含 #153）；退回版為 `3c16f096378a7ea3f8de8ee8e25df9c7e0caf201`。本次從兩個固定提交重新讀取全部比較器 Java 來源，編譯到本 worktree `target/retry-baseline`／`target/retry-prior`，再與修正版 `target/classes` 三方重跑；前後使用相同 staged JAR、entry 清單及核准清單交集。未沿用舊 JSON 冒充重跑結果。

#72 使用遠端 `task/72` 最新 `c460ce97a50b9a2cddeed5e727b37f14f53eb92c`，不是 #153 報告的舊 `6f8a87b`。本輪以 `gh api .../branches/task/72` 確認。其餘來源與題目指定提交一致。沿用前次從原專案 worktree 唯讀取得、已與指定提交報告收據核對的本 worktree target JAR；本次再次核對四份 staged 與兩份 reference 的 SHA-256。

| 來源 | entries | staged SHA-256 |
|---|---:|---|
| #62 `4f0d709` B01 | 22 | `b2787953574abe3f134efcb8416cc66127643f567144f98fbc7b404344e2ac29` |
| #66 `be0b284` B05 | 120 | `79e9462b75590bb9cdb8767ebf333e69a0c8ff95bed0b1239edb11d97d753374` |
| #72 `c460ce9` A01 | 185 | `ba50147a61ce3f50a08aae1ea0f35aba09fd19efdb1a44b3db37d4c4d7fcea49` |
| #76 `e62d213` A05 | 160 | `3af155bc5c65e2413862fc1ceb1db598f9aa704fe91297c788288e6e0570e091` |

候選參考 SHA-256：`50635f7edca8ec6ce77787cc32371c7590b497e48ea202f8a679caf3f517b729`。正式參考 SHA-256：`e1498871c7be6b287821c57ef5d95933db1740d9b4d4a80f6c3465f21d99d498`。新工具為左側提供其原有 reference JAR 作 classpath；右側使用完整 reference JAR。沒有把 reference classes 加入 staged 比較集合或覆蓋 staged。

| 批次 | 基準預設 SAME／DIFF | 修正版預設 SAME／DIFF | 基準核准 SAME／EFFECTS／LAYOUT／DIFF | 修正版核准 SAME／EFFECTS／LAYOUT／DIFF |
|---|---:|---:|---:|---:|
| #62 | 14／8 | 15／7 | 14／0／0／8 | 15／0／0／7 |
| #66 | 115／5 | 116／4 | 115／1／0／4 | 115／2／0／3 |
| #72 | 181／4 | 180／5 | 181／0／0／4 | 180／0／0／5 |
| #76 | 158／2 | 159／1 | 158／0／0／2 | 158／1／0／1 |

四批共 487 個 entry；每輪 ONLY_LEFT／ONLY_RIGHT 均為 0，missingLeft／extraLeft 皆空。與 #153 基準相比，#62／#66／#76 的原有 SAME／SAME_EFFECTS 沒有退回 DIFF；#72 唯一新增 DIFF 是下表的 CombatSkillDerived 未解析欄位參照。與退回版相比，本次只增加四個保守 DIFF，沒有任何 DIFF → SAME 或其他放寬。所有清單都是既有主控清單的精確交集，沒有新增核准或隱藏 entry。

### 每個原 DIFF entry 的前後對照

以下以核准模式為準；「第 N 項」對應 Issue #166 編號。部分方法已消除差異，但同類仍有其他 DIFF，不能把它算成整類 SAME。

| 批次／entry | 修改前 → 新版 | 歸因與剩餘差異 |
|---|---|---|
| #62 NativeBlindSceneAction | DIFF → DIFF | 第 1 項已收斂 player wrapper／handle。companion 中候選仍多 `Objects.requireNonNull` 呼叫，保留 DIFF。 |
| #62 NativeCompanionForestArea | DIFF → DIFF | 第 1 項已收斂 player wrapper／handle。apply 中候選仍多 `Objects.requireNonNull`，保留 DIFF。 |
| #62 NativeCompanionWindAction | DIFF → DIFF | 第 1 項消除 only-left 合成方法。publish 仍有候選額外的 requireNonNull，保留 DIFF。 |
| #62 NativeControlledNegativeMaterial | DIFF → DIFF（退回版 SAME） | 第 2、5 項保留；cast 的多來源 operand 使第 3 項保守拒絕。回到既有比較後第一差異為 TRY 邊界，boolean 暫存與保護範圍的整體等價尚未證明。 |
| #62 NativeSourceDollAccepted | DIFF → DIFF | physical 的 staged 重讀 `Plan.caster`／`Plan.target` GETFIELD；候選讀一次後 DUP。第 3 項保留欄位讀取次數及順序，不把重讀欄位當成 local 暫存。 |
| #62 NativeSourceHealingCast | DIFF → DIFF（退回版 SAME） | run 的多來源 operand 使第 3 項保守拒絕；第一差異為 ALOAD 13／15。ready／body 等暫存不能再以來源 union 當作證明。 |
| #62 NativeWeaponBreakBattleCompanion | DIFF → SAME | 第 2 項處理相鄰保護範圍，第 3＋5 項證明 accounts／snapshot 暫存、null 分支 stack 丟棄及 handler 的值來源一致。 |
| #62 NativeWeaponBreakTargetLifecycle | DIFF → DIFF | 第 1 項收斂 player handle；第 4 項連同巢狀 lambda 收斂 GameHub／Context，run 方法已 SAME。captureOutput 仍宣告左側獨有 IOException；其他 candidate requireNonNull 差異亦不豁免。 |
| #66 NativeSourceProcScalarSkills | DIFF → DIFF（退回版 SAME_EFFECTS） | expiry 有多來源 operand，拒絕 transport 後原 effects 比較仍為 ASTORE／ARETURN 差異；預設也恢復 DIFF。原「expiry 消除」驗收尚未達成。 |
| #66 NativeSourceRawDamage$Member | DIFF → DIFF | 建構子仍有左側額外初始化，未修改 staged 或自動套 AsmFinal。 |
| #66 NativeSourceWindTargets | DIFF → SAME_EFFECTS | run 的 ASTORE 與 web 差異由第 3＋5 項消除；預設也為 SAME。 |
| #66 NativeStaticNpcScene | DIFF → DIFF | heading 仍有不同的呼叫、ASTORE 與 monitor 離開順序，沒有把核准摘要擴大成忽略順序。 |
| #72 NativeDeCharacterFields | DIFF → DIFF | 左側獨有 SKD1 等宣告欄位，owner 解析不刪宣告。 |
| #72 NativeDeCharacterFields$CombatSkillDerived | SAME → DIFF | 新揭露：compute 的 GETSTATIC SKU1／SKU2／SKD1／SKD2／SKD3 在左側可解析，正式 JAR 與父鏈沒有宣告。原始 symbolic owner 相同曾掩蓋此差異。#72 指定提交報告「已裁定事項與新增未決事項」已明列這五個空 HashMap／正式引用矛盾；新版保守拒絕把已解析與未解析欄位視為相同，未執行正式方法。 |
| #72 NativeDeClientViewer | DIFF → DIFF | sendPackets 仍有 CONST／ALOAD 與分支指令差異。 |
| #72 NativeDeDeletionState | DIFF → DIFF | sourceDeleteNpc 的 RuntimeException 保護範圍仍不同，未符合相鄰合併／資料流證明。 |
| #72 NativeDeInstanceState | DIFF → DIFF | 第 6 項消除 `_isBackgroundMode` owner 差異，兩參數基本建構子由既有 ctor-prologue-order 核准通過，逐方法為 DEVIATION；其他四個 Name 建構子仍有呼叫時序差異。 |
| #76 ClassicWorldMapCatalog | DIFF → SAME_EFFECTS | forMap 由第 3＋5 項證明 ASTORE／兩次 ALOAD 與 DUP／IFNULL／ARETURN（null 路徑 POP）一致；預設為 SAME。 |
| #76 Main | DIFF → DIFF（退回版 SAME_LAYOUT） | accept 的 #153 邊界收斂＋第 5 項改善保留；main 有多來源 operand，整個方法拒絕 transport 證明。原 layout-approved.txt 仍不足以消除 System.out 暫存造成的節點差（475／473），因此無法達成 #76 零 DIFF。 |

補充：#66 NativeSourceSignedRecovery 原本核准結果已為 SAME_EFFECTS，新版仍為 SAME_EFFECTS；預設仍 DIFF，並非本輪新增豁免。

### 第二次嘗試：分支匯合反例與回歸

新增有效 Java 17 ASM fixture `public static Fixture.m(Z)I`：先把常數 1、2 存入 local 1、2；左側 true 載入 1／2，false 載入 2／1，右側反過來；共用匯合點執行 ISUB／IRETURN。兩邊的 CFG、producer 集合與複本數相同，但前驅邊對應不同。另加匯合點 DUP／POP 變體。fixture 先計算 stack-map frames，CLI 與隔離 ClassLoader 使用完全相同 JAR bytes。

| 驗證 | #153 基準 | 退回版 `3c16f09` | 修正版 |
|---|---|---|---|
| 未核准預設 CLI | DIFF=1、SAME=0 | **DIFF=0、SAME=1（誤判）** | DIFF=1、SAME=0 |
| JVM `m(true)` | 同一 fixture | 左 -1、右 1 | 左 -1、右 1 |
| JVM `m(false)` | 同一 fixture | 左 1、右 -1 | 左 1、右 -1 |

新測試先在退回版實作上失敗（`AssertionError: CLI branch-merge-swap DIFF`），再套修正通過。原版與 DUP 變體均驗證預設 CLI、左右交換、精確 layout 核准仍 DIFF；不以 layout 核准繞過資料流守門。既有 `branch-temp` 分支暫存正例、wide 正例、diamond／loop／handler 測試全部保留且通過。

Microsoft JDK 17.0.17、ASM 9.7。本機直接 javac 編譯全部工具／測試 Java，以 `-Xverify:all` 執行 NormalizationSelfTest、AdvancedSelfTest、FourthSelfTest；後者以隔離 ClassLoader 真正載入執行分支互換反例、reference 暫存與 long／DUP2 正例，未載入伺服器類別。

- NormalizationSelfTest：517 項通過，既有旗標、metadata、wide 別名、核准邊界測試保留。
- AdvancedSelfTest：73 項通過。原「兩讀取對 DUP」由 #166 明確擴充為合法，更新為 SAME_EFFECTS，另加預設 SAME；原一讀取對兩複本未核准反例仍 DIFF。#153 的欄位覆寫、同欄位寫入順序及 wide 邊界反例均維持 DIFF。
- FourthSelfTest：72 項通過（原 56＋本次 16）；三套共 662 項。涵蓋六項正反例、H6/H5/H9、額外指令／參數重排／直接與 LDC／外類參照、try 缺口與重疊 handler 優先序、讀次數與每來源複本數、呼叫順序、覆寫、錯誤消耗位置、monitor、純 LOAD／INVOKE／GETFIELD、巢狀捕獲、web 重用／重疊／diamond／loop／handler、四種欄位 opcode／遮蔽／缺少 classpath，以及 JVM 執行案例。
- 四種 field opcode 的「左側未知 Parent，右側 Child 成功解析到 Parent」反例先在初版重現錯誤 SAME，保存 `scratch/issue-166/field-unresolved-red.log`；新增未解析標記後全部 DIFF。四批固定產物已重跑，新增保守差異完整列於上表，沒有為維持原計數取消此守門。
- 前次逐項停用規則診斷保存在 `*-after-approved-no-*.{json,md,log}`，只作前次歸因參考。本次以固定兩版來源及修正版完整重跑預設／核准模式，逐 entry 三方結果在 `retry/entry-audit.json`；本次四個恢復 DIFF 的變動不能沿用前次成功證明。
- `git diff --check` 通過；本機完整命令、輸入收據、前後 JSON／Markdown 及逐項關閉規則的結果保存在下列 scratch 路徑。

### CI

- 本次程式提交 `3f7c409744410ffd43f90b4e291520609ab09381`：[build run 41988923959](https://github.com/chaoren0418-arch/lito-launcher-server/actions/runs/41988923959)、[package run 41988924043](https://github.com/chaoren0418-arch/lito-launcher-server/actions/runs/41988924043) 均 SUCCESS；Server、Server tests、Launcher、Semantic gap、Build-Package.ps1 zip 五個 job 全部通過。
- [Semantic gap job 127116227327](https://github.com/chaoren0418-arch/lito-launcher-server/actions/runs/41988923959/job/127116227327) 的實際日誌確認 `NORMALIZATION SELFTEST ALL PASS checks=517`、`ADVANCED SELFTEST ALL PASS checks=73`、`FOURTH SELFTEST ALL PASS checks=72`、`SELFTEST ALL PASS`、`FALLBACK ALL PASS`，以及分支互換兩種 fixture 的 JVM 相反結果與六種 CLI DIFF。這是 662 項 Java 檢查加完整 Bash／冷啟動 fallback，不是只看 non-blocking 外層工作流結果。
- CI 全新 Maven JAR 的預設比較完成：2151 SAME、398 DIFF、1382 ONLY_LEFT、5 ONLY_RIGHT、1008 BYTE_SAME。這是完整伺服器的來源缺口盤點，不是本任務四個固定 staged 批次的驗收計數；未把 CI 產生的 docs/server-semantic-gap.md 寫入本分支。
- [Server tests job 127116227059](https://github.com/chaoren0418-arch/lito-launcher-server/actions/runs/41988923959/job/127116227059) SUCCESS；執行既有 CI `mvn -B test`，依工作流略過 MySQL／本機檔案依賴測試。不以舊任務的測試總數冒稱本次總數。
- 本次 semantic job 原始日誌保存於 `scratch/issue-166/retry/ci-semantic.log`。逐方法確認 Main.accept、forMap、WindTargets.run 仍為 SAME 的日誌亦在 `retry/`；`retry_audit.py` 已核對全部 entry 的狀態變化、六份輸入雜湊、修改範圍與檔案連結。後續只補本報告的提交，最新檢查見 [PR checks](https://github.com/chaoren0418-arch/lito-launcher-server/pull/170/checks)。

## 推測

#62 requireNonNull、重讀欄位及 #72 Name 建構子時序可能在特定狀態下觀察到不同結果；本輪只證實指令／metadata 差異，沒有執行這些伺服器行為，也沒有把它們判定成可忽略。

## 待驗證／未驗證什麼

- 核准模式 #62 剩 7、#66 剩 3、#72 剩 5、#76 剩 1 個 DIFF；其中四個是本次保守拒絕多來源證明而恢復。若要消除，需實作並審查保留前驅邊／值對應的證明，不能只放行 union 或追加核准。
- 保守拒絕的舊式 subroutine、複雜 stack shuffle、local 型別註解與未解析依賴，不承諾消除差異。
- 尚未執行遊戲／登入／戰鬥真機驗收，未測正式 DB；比較器證據不等於部署驗收。

## 產出路徑與重跑

- 交付：`lito伺服器/LiTo381-Server/tools/semantic-diff/**`、本報告。
- 工具 classes、fixture JAR 及固定 staged 複本：`tools/semantic-diff/target/`，不入 git。
- 本次本機重跑入口：先 `scratch/issue-166/verify.py test` 編譯，再 `scratch/issue-166/retry.py`。後者固定讀取兩個歷史提交來源、驗證 staged 雜湊，重跑 24 次批次比較、三版反例 CLI、三套 `-Xverify:all` 自測。Python 使用 `-B -X utf8`，避免在來源目錄產生快取。
- 本次證據：`scratch/issue-166/branch-merge-red.{log,json}`、`scratch/issue-166/retry/{62,66,72,76}-{baseline,prior,fixed}[-approved].{json,md,log}`、`retry/branch-merge-{baseline,prior,fixed}.{json,md,log}`、`retry/entry-audit.json`、`retry/*SelfTest.log`。
- 收據：`scratch/issue-166/inputs.json`、`{62,66,72,76}-{before,after}[-approved].{json,md,log}`、`*-after-approved-no-*.{json,md,log}`；逐方法診斷在 `*-dump.log`、`lifecycle-run.log` 與 `*-l.txt`／`*-r.txt`。
- Linux 重跑：`bash lito伺服器/LiTo381-Server/tools/semantic-diff/selftest.sh`，再執行同目錄 `selftest-fallback.sh`。

## 給下一棒的提示

Claude 主控複審此次「多來源即拒絕 transport」的安全修正與新增反例；裁定是否接受四個恢復 DIFF 的保守交付。要達成原 expiry／#76 零 DIFF 目標，後續比較器任務須保留前驅邊與值的對應並補路徑敏感證明；不可直接恢復來源 union 比較。#62／#66／#72 來源差異仍交各批執行器處理。PR 由 Claude 審查、使用者合併；本輪不合併、不部署。
