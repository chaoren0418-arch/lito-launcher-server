# Issue #166 — T020i-4 semantic-diff 第四批正規化

## 結論

六項規則預設啟用，程式及測試只修改 `tools/semantic-diff/`。固定 staged 重跑後，核准清單模式的 DIFF：#62 **8 → 5**、#66 **4 → 2**、#72 **4 → 5**、#76 **2 → 0**。#66 的 expiry／WindTargets.run、#72 的 `_isBackgroundMode`、#76 的三個指定方法殘差均已消除。#72 同類其他方法仍不同，另因拒絕未解析欄位誤匹配而新揭露 1 個 DIFF，詳見下表。

未修改 src/main、staged Java、核准清單、docs/REPORT.md、機密設定、封存、正式產物或原始專案目錄；沒有啟動伺服器／登入器／遊戲，也沒有連 DB。

## 做了什麼

- `FourthNormalizations`：標準 metafactory 轉呼叫 handle 收斂、無其他參照時移除合成方法、保留 handler 優先序的相鄰 try 合併、捕獲參數與呼叫點一致置換。巢狀 lambda 依子方法先、呼叫者後的順序處理；直接呼叫、LDC、condy、非標準 bootstrap、外類參照均阻止方法移除／捕獲置換。
- `LocalWebs`：ASM reaching definitions 分析正常與例外控制流程，合併同一讀取可達的定義；web 依首次出現順序編號。包含 wide、IINC、diamond、loop 與 handler。精確核准建構子保留實體槽，維持 #153 ctor-prologue-order 的覆寫與欄位寫入守門。
- `TransportComparison`／`LayoutComparison`：對單寫入 web 的每個 LOAD 證明定義來源，消去運送指令後，核對每個運算的有序值來源、每個來源的複本數、其餘指令順序、正常與有序例外邊。stack 值在未使用路徑的 POP／POP2 可以對應 local 不再讀取，但其產生指令仍保留。只有原本核准 layout 的方法容許 CFG 排列不同。
- `JarIndex`：分側解析 field owner，宣告／介面／父類查找保留同名遮蔽；主 JAR 優先於明確 classpath，解析失敗標為 `unresolved(原 owner)`，不讓未知符號偶然等於對側宣告。只讀平台 `java/*` metadata，不初始化應用類別。新增可重複的 `--left-classpath`（別名 `--classpath`）與 `--right-classpath`，接受 JAR／class 目錄並記錄於報告。
- Maven、Bash fallback、selftest 接入 ASM 9.7 `asm-analysis`，與其他 ASM 依賴一起校驗 SHA-256。原有核准 effects 摘要沿用 web 切分前指令，避免改變既有核准結果。

每項詳細前提與保守拒絕情況見 `tools/semantic-diff/README.md`。分析失敗、未解析值、local 型別註解、舊式 subroutine、未支援 stack shuffle 不會視為證明成功。

## 已驗證

### 固定輸入與比較方式

修改前比較器是此 worktree 起點 `10b6b2065fde545e9bbe36b6c9aa101ac2b2c4c4`，含已合併 #153。從該提交讀取比較器來源，編譯到本 worktree `target/before`；前後使用相同 staged JAR、entry 清單及核准清單交集。

#72 使用遠端 `task/72` 最新 `c460ce97a50b9a2cddeed5e727b37f14f53eb92c`，不是 #153 報告的舊 `6f8a87b`。本輪以 `gh api .../branches/task/72` 確認。其餘來源與題目指定提交一致。原專案 worktree 的留存 JAR 只讀；雜湊先與指定提交報告收據核對，複製到本 worktree target。

| 來源 | entries | staged SHA-256 |
|---|---:|---|
| #62 `4f0d709` B01 | 22 | `b2787953574abe3f134efcb8416cc66127643f567144f98fbc7b404344e2ac29` |
| #66 `be0b284` B05 | 120 | `79e9462b75590bb9cdb8767ebf333e69a0c8ff95bed0b1239edb11d97d753374` |
| #72 `c460ce9` A01 | 185 | `ba50147a61ce3f50a08aae1ea0f35aba09fd19efdb1a44b3db37d4c4d7fcea49` |
| #76 `e62d213` A05 | 160 | `3af155bc5c65e2413862fc1ceb1db598f9aa704fe91297c788288e6e0570e091` |

候選參考 SHA-256：`50635f7edca8ec6ce77787cc32371c7590b497e48ea202f8a679caf3f517b729`。正式參考 SHA-256：`e1498871c7be6b287821c57ef5d95933db1740d9b4d4a80f6c3465f21d99d498`。新工具為左側提供其原有 reference JAR 作 classpath；右側使用完整 reference JAR。沒有把 reference classes 加入 staged 比較集合或覆蓋 staged。

| 批次 | 修改前預設 SAME／DIFF | 新版預設 SAME／DIFF | 修改前核准 SAME／EFFECTS／LAYOUT／DIFF | 新版核准 SAME／EFFECTS／LAYOUT／DIFF |
|---|---:|---:|---:|---:|
| #62 | 14／8 | 17／5 | 14／0／0／8 | 17／0／0／5 |
| #66 | 115／5 | 117／3 | 115／1／0／4 | 115／3／0／2 |
| #72 | 181／4 | 180／5 | 181／0／0／4 | 180／0／0／5 |
| #76 | 158／2 | 159／1 | 158／0／0／2 | 158／1／1／0 |

四批每輪 ONLY_LEFT／ONLY_RIGHT 均為 0，missingLeft／extraLeft 皆空。#62／#66／#76 的原有 SAME／SAME_EFFECTS 沒有退回 DIFF；#72 唯一新增 DIFF 是下表的 CombatSkillDerived 未解析欄位參照。所有清單都是既有主控清單的精確交集，沒有新增核准或隱藏 entry。

### 每個原 DIFF entry 的前後對照

以下以核准模式為準；「第 N 項」對應 Issue #166 編號。部分方法已消除差異，但同類仍有其他 DIFF，不能把它算成整類 SAME。

| 批次／entry | 修改前 → 新版 | 歸因與剩餘差異 |
|---|---|---|
| #62 NativeBlindSceneAction | DIFF → DIFF | 第 1 項已收斂 player wrapper／handle。companion 中候選仍多 `Objects.requireNonNull` 呼叫，保留 DIFF。 |
| #62 NativeCompanionForestArea | DIFF → DIFF | 第 1 項已收斂 player wrapper／handle。apply 中候選仍多 `Objects.requireNonNull`，保留 DIFF。 |
| #62 NativeCompanionWindAction | DIFF → DIFF | 第 1 項消除 only-left 合成方法。publish 仍有候選額外的 requireNonNull，保留 DIFF。 |
| #62 NativeControlledNegativeMaterial | DIFF → SAME | 第 2 項處理相鄰保護區間；第 3＋5 項證明 boolean 暫存與運送／槽位關係，完整方法及其他類別屬性相同。 |
| #62 NativeSourceDollAccepted | DIFF → DIFF | physical 的 staged 重讀 `Plan.caster`／`Plan.target` GETFIELD；候選讀一次後 DUP。第 3 項保留欄位讀取次數及順序，不把重讀欄位當成 local 暫存。 |
| #62 NativeSourceHealingCast | DIFF → SAME | 第 3＋5 項消除 ready／body 暫存與 local web 差異，capture／owned 等呼叫順序保持一致。 |
| #62 NativeWeaponBreakBattleCompanion | DIFF → SAME | 第 2 項處理相鄰保護範圍，第 3＋5 項證明 accounts／snapshot 暫存、null 分支 stack 丟棄及 handler 的值來源一致。 |
| #62 NativeWeaponBreakTargetLifecycle | DIFF → DIFF | 第 1 項收斂 player handle；第 4 項連同巢狀 lambda 收斂 GameHub／Context，run 方法已 SAME。captureOutput 仍宣告左側獨有 IOException；其他 candidate requireNonNull 差異亦不豁免。 |
| #66 NativeSourceProcScalarSkills | DIFF → SAME_EFFECTS | expiry 的重複讀取由第 3 項證明；預設也為 SAME。結果名稱仍遵從原 effects 核准清單。 |
| #66 NativeSourceRawDamage$Member | DIFF → DIFF | 建構子仍有左側額外初始化，未修改 staged 或自動套 AsmFinal。 |
| #66 NativeSourceWindTargets | DIFF → SAME_EFFECTS | run 的 ASTORE 與 web 差異由第 3＋5 項消除；預設也為 SAME。 |
| #66 NativeStaticNpcScene | DIFF → DIFF | heading 仍有不同的呼叫、ASTORE 與 monitor 離開順序，沒有把核准摘要擴大成忽略順序。 |
| #72 NativeDeCharacterFields | DIFF → DIFF | 左側獨有 SKD1 等宣告欄位，owner 解析不刪宣告。 |
| #72 NativeDeCharacterFields$CombatSkillDerived | SAME → DIFF | 新揭露：compute 的 GETSTATIC SKU1／SKU2／SKD1／SKD2／SKD3 在左側可解析，正式 JAR 與父鏈沒有宣告。原始 symbolic owner 相同曾掩蓋此差異。#72 指定提交報告「已裁定事項與新增未決事項」已明列這五個空 HashMap／正式引用矛盾；新版保守拒絕把已解析與未解析欄位視為相同，未執行正式方法。 |
| #72 NativeDeClientViewer | DIFF → DIFF | sendPackets 仍有 CONST／ALOAD 與分支指令差異。 |
| #72 NativeDeDeletionState | DIFF → DIFF | sourceDeleteNpc 的 RuntimeException 保護範圍仍不同，未符合相鄰合併／資料流證明。 |
| #72 NativeDeInstanceState | DIFF → DIFF | 第 6 項消除 `_isBackgroundMode` owner 差異，兩參數基本建構子由既有 ctor-prologue-order 核准通過，逐方法為 DEVIATION；其他四個 Name 建構子仍有呼叫時序差異。 |
| #76 ClassicWorldMapCatalog | DIFF → SAME_EFFECTS | forMap 由第 3＋5 項證明 ASTORE／兩次 ALOAD 與 DUP／IFNULL／ARETURN（null 路徑 POP）一致；預設為 SAME。 |
| #76 Main | DIFF → SAME_LAYOUT | accept 由 #153 邊界收斂＋第 5 項消除槽 9／10 重用差異；main 由第 3 項消除 System.out 跨 SourceLogin.start 的暫存，再使用既有 layout-approved.txt 通過 CFG。預設 main 仍 DIFF，沒有自動授權 layout。 |

補充：#66 NativeSourceSignedRecovery 原本核准結果已為 SAME_EFFECTS，新版仍為 SAME_EFFECTS；預設仍 DIFF，並非本輪新增豁免。

### 正反例與回歸

Microsoft JDK 17.0.17、ASM 9.7。本機直接 javac 編譯全部工具／測試 Java，執行 NormalizationSelfTest、AdvancedSelfTest、FourthSelfTest；後者另以隔離 ClassLoader 真正載入執行有分支的 reference 暫存與 long／DUP2 正例，未載入伺服器類別。

- NormalizationSelfTest：517 項通過，既有旗標、metadata、wide 別名、核准邊界測試保留。
- AdvancedSelfTest：73 項通過。原「兩讀取對 DUP」由 #166 明確擴充為合法，更新為 SAME_EFFECTS，另加預設 SAME；原一讀取對兩複本未核准反例仍 DIFF。#153 的欄位覆寫、同欄位寫入順序及 wide 邊界反例均維持 DIFF。
- FourthSelfTest：56 項通過，涵蓋六項正反例、H6/H5/H9、額外指令／參數重排／直接與 LDC／外類參照、try 缺口與重疊 handler 優先序、讀次數與每來源複本數、呼叫順序、覆寫、錯誤消耗位置、monitor、純 LOAD／INVOKE／GETFIELD、巢狀捕獲、web 重用／重疊／diamond／loop／handler、四種欄位 opcode／遮蔽／缺少 classpath，以及 JVM 執行案例。
- 四種 field opcode 的「左側未知 Parent，右側 Child 成功解析到 Parent」反例先在初版重現錯誤 SAME，保存 `scratch/issue-166/field-unresolved-red.log`；新增未解析標記後全部 DIFF。四批固定產物已重跑，新增保守差異完整列於上表，沒有為維持原計數取消此守門。
- 診斷以 `-Dsemanticdiff.disable=<rule>` 逐項關閉六項規則。第 3／5 項關閉時，指定暫存差異重新出現；第 6 項關閉時基本建構子重新出現 field owner DIFF。第 1／4 項的改善在同類仍有其他差異時不改變類別計數。第 2 項的相鄰區間在第 3 項的有序例外邊證明中也可得到相同結果，因此不能把類別差額全歸給第 2 項。
- `git diff --check` 通過；本機完整命令、輸入收據、前後 JSON／Markdown 及逐項關閉規則的結果保存在下列 scratch 路徑。

### CI

推送後執行 GitHub Actions；最終 run、job 與測試結果會在完成後補入本節。不能把 non-blocking Semantic gap 工作流外層綠燈當成其內部自測成功。

## 推測

#62 requireNonNull、重讀欄位及 #72 Name 建構子時序可能在特定狀態下觀察到不同結果；本輪只證實指令／metadata 差異，沒有執行這些伺服器行為，也沒有把它們判定成可忽略。

## 待驗證／未驗證什麼

- #62 剩 5、#66 剩 2、#72 剩 5 個 DIFF 是來源、未解析欄位或其他未核准形狀，應由各批次執行器及主控處理；本任務沒有擴大核准範圍。
- 保守拒絕的舊式 subroutine、複雜 stack shuffle、local 型別註解與未解析依賴，不承諾消除差異。
- 尚未執行遊戲／登入／戰鬥真機驗收，未測正式 DB；比較器證據不等於部署驗收。

## 產出路徑與重跑

- 交付：`lito伺服器/LiTo381-Server/tools/semantic-diff/**`、本報告。
- 工具 classes、fixture JAR 及固定 staged 複本：`tools/semantic-diff/target/`，不入 git。
- 本機重跑入口：`scratch/issue-166/verify.py`，動作 `test`／`compare`／`ablate`。`before` 僅在原始起點使用，以免把新版誤當基準。
- 收據：`scratch/issue-166/inputs.json`、`{62,66,72,76}-{before,after}[-approved].{json,md,log}`、`*-after-approved-no-*.{json,md,log}`；逐方法診斷在 `*-dump.log`、`lifecycle-run.log` 與 `*-l.txt`／`*-r.txt`。
- Linux 重跑：`bash lito伺服器/LiTo381-Server/tools/semantic-diff/selftest.sh`，再執行同目錄 `selftest-fallback.sh`。

## 給下一棒的提示

Claude 主控複審資料流證明、handler 優先序、捕獲同步置換及 classpath 遮蔽守門。#62／#66／#72 執行器按上表繼續修 staged，尤其不要忽略 requireNonNull 或重讀欄位；#76 可用本工具與既有核准清單重跑其後續交付。PR 由 Claude 審查、使用者合併；本輪不合併、不部署。
