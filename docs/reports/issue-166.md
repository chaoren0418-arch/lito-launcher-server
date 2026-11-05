# Issue #166 — T020i-4 semantic-diff 第四批正規化

## 結論

第三次嘗試補足 PR #170 退回的兩項驗收：#66 `NativeSourceProcScalarSkills#expiry` 已消除；#76 使用既有 `layout-approved.txt` 後 **0 DIFF**。沒有新增核准、恢復多來源 union 放行或修改 staged Java。709 項自測通過，多來源分支互換、左右交換、DUP 與 layout 反例仍 DIFF。

本次新增可獨立證明安全的局部 stack 改寫，再比較完整指令／CFG；一般資料流路徑仍拒絕多來源 operand。四批核准模式 DIFF（#153 基準 → 上次 HEAD → 本次）：#62 **8 → 7 → 7**、#66 **4 → 3 → 2**、#72 **4 → 5 → 5**、#76 **2 → 1 → 0**。其餘差異與保守限制如下，不宣稱所有 staged 已等價。

未修改 src/main、staged Java、核准清單、docs/REPORT.md、機密設定、封存、正式產物或原始專案工作檔；git commit／push 僅依指示更新共用 Git 中繼資料。沒有啟動伺服器／登入器／遊戲，也沒有連 DB。

交付分支 `task/166`，[PR #170](https://github.com/chaoren0418-arch/lito-launcher-server/pull/170)。本次 CI 證據見下方「CI」，前次綠燈不當作本次驗證。

## 做了什麼

- `FourthNormalizations`：標準 metafactory 轉呼叫 handle 收斂、無其他參照時移除合成方法、保留 handler 優先序的相鄰 try 合併、捕獲參數與呼叫點一致置換。巢狀 lambda 依子方法先、呼叫者後的順序處理；直接呼叫、LDC、condy、非標準 bootstrap、外類參照均阻止方法移除／捕獲置換。
- `LocalWebs`：ASM reaching definitions 分析正常與例外控制流程，合併同一讀取可達的定義；web 依首次出現順序編號。包含 wide、IINC、diamond、loop 與 handler。精確核准建構子保留實體槽，維持 #153 ctor-prologue-order 的覆寫與欄位寫入守門。
- `LocalTransport`（本次新增）：先對唯一寫入、reaching definition 精確匹配的 web 嘗試局部改寫。單次讀取的 `xSTORE n; ...; xLOAD n` 區段必須無分支及額外正常／handler 入口；以空 stack 執行中間指令，不得 underflow，抵達 LOAD 時必須再次為空，證明不碰原 stack 前綴。保存值可以留在 stack，呼叫、欄位及 monitor 順序不變。例外清空 stack，唯一讀取限制排除 handler 觀察被移除 local。STORE 的值可來自多個前驅，原前驅與取值對應完全保留。
- 第二個局部形狀是 `ASTORE n; ALOAD n; IFNULL target; ALOAD n; ARETURN; target: ...`，改為 `DUP; IFNULL target; ARETURN; target: POP; ...`。n 恰有兩次讀取，區段無額外入口，target 緊接 ARETURN 且唯一前驅就是 IFNULL、不是 handler（包含別名標籤）。兩條路徑的實際取值不變，後續 switch 與匯合完整保留。共同拒絕參數槽、重疊 wide、IINC、被移除指令註解、local 型別註解、JSR／RET；每次改寫後重新分析 frames。
- `TransportComparison`／`LayoutComparison`：局部改寫後預設須完整指令與 try 表相同；只有原本精確 layout 核准才允許 CFG 排列不同。未通過局部證明時，原 singleton 資料流證明仍核對運算的有序來源、複本數與正常／例外邊；保留 operand 或被複製值有多個 producer 時仍拒絕，不恢復 union 放行。`ClassComparer.tryCatch` 僅改為套件可見供完整 try 表核對，沒有擴大 effects 摘要。`-Dsemanticdiff.disable=transport` 同時停用局部與 singleton 證明。
- `JarIndex`：分側解析 field owner，宣告／介面／父類查找保留同名遮蔽；主 JAR 優先於明確 classpath，解析失敗標為 `unresolved(原 owner)`，不讓未知符號偶然等於對側宣告。只讀平台 `java/*` metadata，不初始化應用類別。新增可重複的 `--left-classpath`（別名 `--classpath`）與 `--right-classpath`，接受 JAR／class 目錄並記錄於報告。
- Maven、Bash fallback、selftest 接入 ASM 9.7 `asm-analysis`，與其他 ASM 依賴一起校驗 SHA-256。原有核准 effects 摘要沿用 web 切分前指令，避免改變既有核准結果。

每項詳細前提與保守拒絕情況見 `tools/semantic-diff/README.md`。分析失敗、未解析值、local 型別註解、舊式 subroutine、未支援 stack shuffle 不會視為證明成功。

## 已驗證

### 固定輸入與比較方式

三方比較器均從 Java 來源重新編譯：#153 基準 `10b6b2065fde545e9bbe36b6c9aa101ac2b2c4c4`、上次 HEAD `686a5aa42a008a69c767e5956c3df59f0d8b6b18`、本次工作樹。每版重跑四批預設／既有核准模式，共 24 次批次比較；相同批次使用相同 JAR、entry 清單與既有核准交集。沒有沿用舊 JSON 冒充重跑。

#72 經 `gh api repos/chaoren0418-arch/lito-launcher-server/branches/task/72 --jq .commit.sha` 確認仍為最新 `c460ce97a50b9a2cddeed5e727b37f14f53eb92c`。沿用本 worktree 前次已核對指定提交報告收據的 staged 複本，本次再次核對四份 staged 與兩份 reference 的 SHA-256，未讀寫封存。

| 來源 | entries | staged SHA-256 |
|---|---:|---|
| #62 `4f0d709` B01 | 22 | `b2787953574abe3f134efcb8416cc66127643f567144f98fbc7b404344e2ac29` |
| #66 `be0b284` B05 | 120 | `79e9462b75590bb9cdb8767ebf333e69a0c8ff95bed0b1239edb11d97d753374` |
| #72 `c460ce9` A01 | 185 | `ba50147a61ce3f50a08aae1ea0f35aba09fd19efdb1a44b3db37d4c4d7fcea49` |
| #76 `e62d213` A05 | 160 | `3af155bc5c65e2413862fc1ceb1db598f9aa704fe91297c788288e6e0570e091` |

候選參考 SHA-256：`50635f7edca8ec6ce77787cc32371c7590b497e48ea202f8a679caf3f517b729`。正式參考 SHA-256：`e1498871c7be6b287821c57ef5d95933db1740d9b4d4a80f6c3465f21d99d498`。新工具為左側提供其原有 reference JAR 作 classpath；右側使用完整 reference JAR。沒有把 reference classes 加入 staged 比較集合或覆蓋 staged。

| 批次 | 基準預設 SAME／DIFF | 上次預設 SAME／DIFF | 本次預設 SAME／DIFF | 本次核准 SAME／EFFECTS／LAYOUT／DIFF |
|---|---:|---:|---:|---:|
| #62 | 14／8 | 15／7 | 15／7 | 15／0／0／7 |
| #66 | 115／5 | 116／4 | 117／3 | 115／3／0／2 |
| #72 | 181／4 | 180／5 | 180／5 | 180／0／0／5 |
| #76 | 158／2 | 159／1 | 159／1 | 158／1／1／0 |

四批共 487 個 entry；每輪 ONLY_LEFT／ONLY_RIGHT 均為 0，missingLeft／extraLeft 皆空。相對上次 HEAD，唯一 DIFF 狀態改善是 #66 ProcScalarSkills（預設及核准）和 #76 Main（僅既有 layout 核准）。其餘原 DIFF 均保留，原已通過 entry 無退回 DIFF；未新增任何核准或隱藏 entry。相對 #153 基準，#72 唯一新增保守 DIFF 仍是下表未解析的 CombatSkillDerived。完整三方逐 entry 結果在 `third/entry-audit.json`。

### 每個原 DIFF entry 的前後對照

以下以核准模式為準，修改前指 #153 基準；本次相對上次 HEAD 只有 ProcScalarSkills／Main 改善，其餘狀態完全相同。「第 N 項」對應 Issue #166 編號。部分方法已消除差異，但同類仍有其他 DIFF，不能把它算成整類 SAME。

| 批次／entry | 修改前 → 新版 | 歸因與剩餘差異 |
|---|---|---|
| #62 NativeBlindSceneAction | DIFF → DIFF | 第 1 項已收斂 player wrapper／handle。companion 中候選仍多 `Objects.requireNonNull` 呼叫，保留 DIFF。 |
| #62 NativeCompanionForestArea | DIFF → DIFF | 第 1 項已收斂 player wrapper／handle。apply 中候選仍多 `Objects.requireNonNull`，保留 DIFF。 |
| #62 NativeCompanionWindAction | DIFF → DIFF | 第 1 項消除 only-left 合成方法。publish 仍有候選額外的 requireNonNull，保留 DIFF。 |
| #62 NativeControlledNegativeMaterial | DIFF → DIFF（最初 union 版曾 SAME，上次已恢復 DIFF） | 第 2、5 項保留；cast 多來源運送未符合本次局部前提，一般 transport 仍保守拒絕。回到既有比較後第一差異為 TRY 邊界，boolean 暫存與保護範圍的整體等價尚未證明。 |
| #62 NativeSourceDollAccepted | DIFF → DIFF | physical 的 staged 重讀 `Plan.caster`／`Plan.target` GETFIELD；候選讀一次後 DUP。第 3 項保留欄位讀取次數及順序，不把重讀欄位當成 local 暫存。 |
| #62 NativeSourceHealingCast | DIFF → DIFF（最初 union 版曾 SAME，上次已恢復 DIFF） | run 多來源運送未符合本次局部前提，一般 transport 仍保守拒絕；第一差異為 ALOAD 13／15。ready／body 等暫存不能再以來源 union 當作證明。 |
| #62 NativeWeaponBreakBattleCompanion | DIFF → SAME | 第 2 項處理相鄰保護範圍，第 3＋5 項證明 accounts／snapshot 暫存、null 分支 stack 丟棄及 handler 的值來源一致。 |
| #62 NativeWeaponBreakTargetLifecycle | DIFF → DIFF | 第 1 項收斂 player handle；第 4 項連同巢狀 lambda 收斂 GameHub／Context，run 方法已 SAME。captureOutput 仍宣告左側獨有 IOException；其他 candidate requireNonNull 差異亦不豁免。 |
| #66 NativeSourceProcScalarSkills | DIFF → SAME_EFFECTS（上次 DIFF） | 第 3 項局部 null-return 證明消除 expiry，後段四路 switch 匯合完整保留。預設整類亦 SAME，不靠 effects 豁免才能通過。 |
| #66 NativeSourceRawDamage$Member | DIFF → DIFF | 建構子仍有左側額外初始化，未修改 staged 或自動套 AsmFinal。 |
| #66 NativeSourceWindTargets | DIFF → SAME_EFFECTS | run 的 ASTORE 與 web 差異由第 3＋5 項消除；預設也為 SAME。 |
| #66 NativeStaticNpcScene | DIFF → DIFF | heading 仍有不同的呼叫、ASTORE 與 monitor 離開順序，沒有把核准摘要擴大成忽略順序。 |
| #72 NativeDeCharacterFields | DIFF → DIFF | 左側獨有 SKD1 等宣告欄位，owner 解析不刪宣告。 |
| #72 NativeDeCharacterFields$CombatSkillDerived | SAME → DIFF | 新揭露：compute 的 GETSTATIC SKU1／SKU2／SKD1／SKD2／SKD3 在左側可解析，正式 JAR 與父鏈沒有宣告。原始 symbolic owner 相同曾掩蓋此差異。#72 指定提交報告「已裁定事項與新增未決事項」已明列這五個空 HashMap／正式引用矛盾；新版保守拒絕把已解析與未解析欄位視為相同，未執行正式方法。 |
| #72 NativeDeClientViewer | DIFF → DIFF | sendPackets 仍有 CONST／ALOAD 與分支指令差異。 |
| #72 NativeDeDeletionState | DIFF → DIFF | sourceDeleteNpc 的 RuntimeException 保護範圍仍不同，未符合相鄰合併／資料流證明。 |
| #72 NativeDeInstanceState | DIFF → DIFF | 第 6 項消除 `_isBackgroundMode` owner 差異，兩參數基本建構子由既有 ctor-prologue-order 核准通過，逐方法為 DEVIATION；其他四個 Name 建構子仍有呼叫時序差異。 |
| #76 ClassicWorldMapCatalog | DIFF → SAME_EFFECTS | forMap 由第 3＋5 項證明 ASTORE／兩次 ALOAD 與 DUP／IFNULL／ARETURN（null 路徑 POP）一致；預設為 SAME。 |
| #76 Main | DIFF → SAME_LAYOUT（上次 DIFF） | accept 的 #153 邊界收斂＋第 5 項仍通過；main 由第 3 項局部直線證明移除 System.out STORE／LOAD，再由既有 layout 核准對齊 CFG。預設仍 DIFF，未擴大核准。 |

補充：#66 NativeSourceSignedRecovery 原本核准結果已為 SAME_EFFECTS，新版仍為 SAME_EFFECTS；預設仍 DIFF，並非本輪新增豁免。

### 回歸與證明限制

Microsoft JDK 17.0.17、ASM 9.7，javac 從來源編譯後以 `-Xverify:all` 執行：NormalizationSelfTest **517**、AdvancedSelfTest **73**、FourthSelfTest **119**，合計 **709**。原六項正反例、wide、web、欄位遮蔽／未解析、lambda 額外參照、try gap／優先序與核准邊界等檢查全保留。

本次新增 47 項檢查。新 fixture 直接以 CLI 比較落地 JAR，並以隔離 ClassLoader 實際執行，不載入伺服器類別。以下正例在上次 HEAD 都 DIFF，本次通過，能捕捉此次缺口：

| fixture | 上次 HEAD | 本次 | 驗證 |
|---|---|---|---|
| expiry-merge | DIFF | SAME | null-return 暫存＋後段四路 switch 匯合；null／非 null、五種 key 的 JVM 結果符合預期。 |
| merged-temp | DIFF | SAME | STORE 本身接收兩個前驅的值；true／false JVM 結果分別 1／2。 |
| main-layout-temp | DIFF | SAME_LAYOUT | System.out 跨帶副作用的 start、其他 merged operand 與區塊重排；兩種輸入結果與 start 執行次數相同。未核准仍 DIFF。 |

三個正例另測左右交換與停用 transport，分別維持成功與 DIFF。新反例包含 switch arm／merged 來源互換、呼叫先後變更（含 layout＋反向比較）、額外前驅、消耗保存值下方 stack、帶註解 STORE。另以 JVM 執行局部改寫後的正常及 ArithmeticException 路徑，證明保護範圍內的 stack 搬運不改變兩種結果。

原 `branch-merge-swap` 與 DUP 變體的 true／false 結果仍為左 -1／1、右 1／-1；預設、左右交換、精確 layout 核准均 DIFF。兩版 CLI 對照確認上次 HEAD 與本次都拒絕，沒有恢復最初版本的 union 誤判。

`git diff --check` 通過。逐方法診斷另確認 expiry、WindTargets.run、forMap、Main.accept 通過，Main.main 僅既有 layout 核准通過；未將整類核准結果冒充所有其他方法的資料流證明。

### CI

本次提交推送後核對新 HEAD 的 build／package，以及 Semantic gap 內 Self-test 與冷啟動 fallback 實際步驟。既有 CI 是 non-blocking，不能只看外層綠燈；實際驗證連結於推送後補入本節，不沿用前次 HEAD 的綠燈。

## 推測

#62 requireNonNull、重讀欄位及 #72 Name 建構子時序可能在特定狀態下觀察到不同結果；本輪只證實指令／metadata 差異，沒有執行這些伺服器行為，也沒有把它們判定成可忽略。

## 待驗證／未驗證什麼

- 核准模式仍有 #62 七個、#66 兩個、#72 五個 DIFF，逐項原因見表。#62 ControlledNegativeMaterial／HealingCast 的一般多來源暫存等價尚未證明；不符合本次局部形狀時仍保守拒絕。
- 保守拒絕的舊式 subroutine、複雜 stack shuffle、local 型別註解與未解析依賴，不承諾消除差異。
- 尚未執行遊戲／登入／戰鬥真機驗收，未測正式 DB；比較器證據不等於部署驗收。

## 產出路徑與重跑

- 交付：`lito伺服器/LiTo381-Server/tools/semantic-diff/{src,README.md}`、本報告。
- 工具 classes、fixture JAR、固定 staged 複本：同工具 `target/`，不入 git。
- 本機入口：`scratch/issue-166/verify.py test` 編譯與自測，再執行 `scratch/issue-166/third.py` 重建兩版歷史工具、驗證六份雜湊、24 次批次比較、逐 entry audit、前後 fixture CLI 與三套 `-Xverify:all` 自測。Python 使用 `-B -X utf8`；本機路徑為 `C:/Users/CHAOREN/AppData/Local/Programs/Python/Python312/python.exe`。
- 本次證據：`scratch/issue-166/third/{62,66,72,76}-{baseline,previous,fixed}[-approved].{json,md,log}`、`third/entry-audit.json`、`third/{expiry-merge,merged-temp,main-layout-temp,branch-merge-swap,branch-merge-swap-dup}-{previous,fixed}.*`、`third/*SelfTest.log`；fixture 重跑可用 `third_fixtures.py`。
- 輸入收據：`scratch/issue-166/inputs.json`、`issue-{62,66,72,76}-source.md`。逐方法及原始指令診斷亦留在本 worktree scratch。
- Linux／CI：`bash lito伺服器/LiTo381-Server/tools/semantic-diff/selftest.sh`，再執行同目錄 `selftest-fallback.sh`。

## 給下一棒的提示

Claude 複審兩個局部改寫前提與完整 CFG 核對，確認 expiry 與 #76 零 DIFF、新正例的舊版 DIFF／新版通過以及所有分支互換反例。其餘 #62／#66／#72 差異交各批執行器處理；若擴充一般多來源運送，仍須保留前驅與值的對應，不能放行 union 或追加核准湊零。PR 由 Claude 審查、使用者合併；不合併、不部署。
