# Issue #254：T020i-11 無作用載入與相同 CFG 區塊合併

## 做了什麼

在既有 worktree／`task/254` 實作兩側預設開啟的無作用載入消除與 CFG 相同區塊固定點合併。修改前基線 `d239e89`；未切換或建立分支。只修改 `tools/semantic-diff/**` 與本報告，未修改 src/main、staged Java、核准清單或 docs/REPORT.md。

1. `DiscardedLoads` 只刪除相鄰、寬度相符且沒有指令註解的 LOAD／純常數／DUP 與 POP 配對。任一 label 阻止配對；POP2 不能多消耗原 stack。LDC 僅接受數值與字串，class／MethodType／MethodHandle／ConstantDynamic 及呼叫等可能有副作用的生產者保留。
2. CFG 合併鍵包含正規化事件與 operands、正常後繼、逐事件有序例外後繼、入口／出口 stack 與複本資訊；區塊內暫存沿用 #174。非空入口逐前驅追蹤，必須每條邊都是同一來源或相同純常數；來源帶生產區塊身分，不用無序 union。混合 phi／循環／過深 transport 保守拒絕。合併後重新導向所有正常與例外前驅，直到固定點，再做同構及既有跨邊 stack 關聯檢查。
3. 原有 CFG 先嘗試；必要時在空 stack 的 try 邊界、其他空 stack 指令邊界切出 finally 後綴，再合併。這解決 `drain` 的 STORE／LOAD／IF 與 `Dispatch.run` 的內嵌 complete；沒有移動操作或刪除例外邊。
4. 在通用載入清理前保留既有純轉拋 handler 的鎖暫存辨識；初版過早消除 LOAD／POP 曾使 #68 回到 DIFF，已由前置 handler 證明修復，不放寬其 liveness 條件。
5. 新增 `EleventhSelfTest` 並接入正式 `selftest.sh`。舊 effects 測試中依賴無作用指令的個別案例，明確停用新規則以保留原核准閘門測試；新測試使用預設規則及消融交叉檢查。

## 已驗證／怎麼驗證

Microsoft OpenJDK 17.0.17、ASM／asm-tree／asm-analysis 9.7。本機十套 Java 自測全部使用 `-Xverify:all`，checks 為 **517、73、119、169、159、96、265、193、106、148，共 1845**。

新正反例的比較器與 platform-parent 隔離 ClassLoader 讀取同一份 framed JAR。反例必須觀察到不同結果，且雙向 CLI 與直接 CFG（適用者）拒絕等價；VerifyError 不算行為差異。

| 反例 | 實際 JVM 左／右 | 工具 |
|---|---|---|
| 有副作用呼叫的結果被 POP | 1／0 | 雙向 DIFF |
| 指令相同、正常後繼不同 | input=2：3／6 | 雙向 DIFF、直接 CFG 拒絕 |
| 指令相同、只有一塊受 try 保護 | input=0：ArithmeticException／-1 | 雙向 DIFF、直接 CFG 拒絕 |
| 指令相同、入口 stack 值不同 | input=1：1／2 | 雙向 DIFF、直接 CFG 拒絕，兩個 IRETURN 區塊保留 |

正例涵蓋五種 LOAD、寬常數與 POP2、DUP／POP、固定點迭代、迴圈、內嵌 finally、#174 區塊內暫存、非空 stack 的共同來源與不同生產點的相同常數。關閉其他證明後可觀察 SAME_CFG；再關閉 cfg-merge 恢復 DIFF。原有 #203／#216 stack、覆寫及 handler 反例全部通過。

### #69 指定輸入

固定 `task/69@5434695aefe5ee91a874c7a6bf26c044f566d009`，使用其報告的 `target/issue-69/recheck-216/b08.jar`，SHA-256 `de96b8890030b3bc1c993f8a97273a41cd702781d07b3557b98278b44653dea9`；160-entry 集合完全相符。沿用該報告的 access-widen 1、extra-method 1、Exceptions 3 條，不新增核准。

完整 CLI 重跑，類別由 **141 SAME、10 SAME_PATHS、5 SAME_CFG、4 DIFF** 改為 **141 SAME、10 SAME_PATHS、7 SAME_CFG、2 DIFF**。四個指定方法結果：

| 類別 | 方法 | 修改前 → 後 |
|---|---|---|
| NativeProcHealingEncounter$Dispatch | `run()V` | DIFF → SAME_CFG |
| NativePlayerCommit | `drain(ClientSession, Queue)V` | DIFF → SAME_CFG |
| NativePlayerCommit | `acceptedTail(Frame)V` | DIFF → SAME_CFG |
| NativePlayerCommit | `nativeManaTransferRecover(Receipt, ClientSession, Binding, int, int, String)V` | DIFF → SAME（線性／既有正規化通過，已不需 CFG） |

上表參數皆為 `local/lito381/server` 內原型別；Receipt 為 `NativePlayerVitalCompletions$Receipt`，Binding／Frame／Queue 為 `NativePlayerCommit` 的巢狀類別。JSON 僅列後備／差異方法，因此 recover 成功後從 methodResults 消失，不能誤記為 SAME_CFG；另直接 CFG 探查亦通過，但不是 CLI 最先使用的證明。

兩次獨立消融確認：停用 `discarded-loads` 僅 recover 恢復 DIFF（paths 仍超過 4096）；停用 `cfg-merge`，run／drain／acceptedTail 恢復 DIFF，recover 保持 SAME。沒有提高路徑上限。

剩餘 DIFF 為 `NativeLightAction` 與 `NativeLightAction$Batch`，未宣稱 B08 全批通過。

### 九批固定輸入與前後對照

於重跑前及完成後以 `git ls-remote` 核對九個 task 分支，提交均未變。每個 staged JAR 的 SHA-256 必須出現在該提交報告，class 集合精確等於該提交 batches.json 的 verify_entries；九批合計 **1434 entries**。修改前為 `d239e89`，修改後為程式提交 `45bbbe4`。

| 批次 | 來源提交 | entries | 核准模式 DIFF 前→後 | 修改後類別結果（不重列 BYTE_SAME） |
|---|---|---:|---|---|
| #62 | `58b2f00e40a3` | 22 | 0 → 0 | 21 SAME、1 LAMBDA_PAIRED_BY_CALLSITE |
| #63 | `23200d027c80` | 108 | 0 → 0 | 107 SAME、1 SAME_PATHS |
| #65 | `4d4cdd3d732a` | 98 | 0 → 0 | 95 SAME、1 SAME_PATHS、1 SAME_CFG、1 DEVIATION |
| #66 | `5380ca3a1ea7` | 120 | 0 → 0 | 115 SAME、3 SAME_EFFECTS、1 SAME_PATHS、1 ORPHAN_IGNORED |
| #67 | `1c887286fef4` | 323 | 0 → 0 | 311 SAME、10 SAME_PATHS、2 SAME_CFG |
| #68 | `6a255226e523` | 56 | 0 → 0 | 54 SAME、2 SAME_CFG |
| #69 | `5434695aefe5` | 160 | 4 → 2 | 141 SAME、10 SAME_PATHS、7 SAME_CFG、2 DIFF |
| #70 | `989417ab30b7` | 362 | 3 → 3 | 357 SAME、2 SAME_PATHS、3 DIFF |
| #72 | `09af151ea7eb` | 185 | 0 → 0 | 183 SAME、2 DEVIATION |

全部 ONLY_LEFT／ONLY_RIGHT、missingLeft／extraLeft 均為 0。#62、#63、#65、#66、#67、#68、#72 的完整 `classes`（含 methodResults）、counts 與 classSelection 逐項相同；既有 DEVIATION 不改列 SAME。沒有原先通過的方法變成 DIFF。

#70 類別狀態仍為 357 SAME、2 SAME_PATHS、3 DIFF；唯一新增證明是 `ClientSession#nativeStaticOriginalDialog(J)V`：DIFF → SAME_CFG。另行逐方法比較確認，原 view 左／右 226／225 區塊，收縮及合併後均 222，停用 cfg-merge 即恢復差異。`serverHonorRespawnDue`、`useSpellBeforeDragonTargets`、`useTeleportSpell` 只是候選的無作用指令移除後診斷 offset 改變，三者仍 DIFF。其餘所有方法結果不變；未執行應用方法或以遊戲觀測代替比較證明。

既有核准按該批精確 owner 裁切：#62 exceptions 1；#63／#67／#68 無；#65 method-diff 2；#66 effects 7；#69 如上；#70 effects 2、extra-method 1、enclosing-method 4，加既有 T029 八方法及正式 allow-right；#72 沿用原有 access-widen、建構子及 method-diff 核准。沒有新增核准。

另跑無核准模式：#62 1→1、#63 0→0、#65 1→1、#66 0→0、#67 0→0、#68 0→0、#69 6→4、#72 3→3（DIFF 類別數）；#70 未額外跑無核准模式，採完整既有 T029 核准驗收。

B 批 reference SHA-256 為 `50635f7edca8ec6ce77787cc32371c7590b497e48ea202f8a679caf3f517b729`；#72 與 #70 allow-right 為正式 `e1498871c7be6b287821c57ef5d95933db1740d9b4d4a80f6c3465f21d99d498`。#70 classpath 同時包含候選與正式，其他批次使用對應 reference。

| 批次 | staged JAR SHA-256 |
|---|---|
| #62 | `a79c74b4cab4b6714bf241953feeacb8201b2a125578ae2f31702ca8ff6d19c7` |
| #63 | `9cc4d4cbac153d7f13ffea5864fb7ac34994618bd7717c4a2900bd36080f38ef` |
| #65 | `ac5e21b26180b296dab457173b52cace3c0dfca63b250c8bc793859f77c80834` |
| #66 | `b8d54f8315093327943495503edb9e7dbb0a725d73ea10da012d37738f1d52e4` |
| #67 | `8820abf5684b6fd6c9c55826372a21bad10f5f965880d4a954e57fe22d1640c6` |
| #68 | `af7f16ed2373d96069dbedb059e2bb57ebb5a247eeca66b380ebed87a3f032f8` |
| #69 | `de96b8890030b3bc1c993f8a97273a41cd702781d07b3557b98278b44653dea9` |
| #70 | `eeb1b8d0bba82e6852bc6020eb17f119de44ae41d27147040cd4deae7e0d75f3` |
| #72 | `63e970a0ed1bd26f94586b79df3de840e6231b46977ce63215d5427e55363808` |

原始 JAR 路徑（相對 `D:/lineage開發/3-lito開發/scratch/wt/`；僅讀取）：

- #62：`task-62/lito伺服器/LiTo381-Server/target/issue-62/review-final/staged.jar`
- #63：`task-63/lito伺服器/LiTo381-Server/target/issue-63/final-174-176/compiled.jar`
- #65：`task-65/lito伺服器/LiTo381-Server/target/issue-65/recheck-203/b04.jar`
- #66：`task-66/lito伺服器/LiTo381-Server/target/issue-66-corrected-current/batch.jar`
- #67：`task-67/lito伺服器/LiTo381-Server/target/issue-67/cfg-final/staged.jar`
- #68：`task-68/lito伺服器/LiTo381-Server/target/issue-68/reverify/staged.jar`
- #69：`task-69/lito伺服器/LiTo381-Server/target/issue-69/recheck-216/b08.jar`
- #70：`task-70/scratch/issue-70/target/verified216.jar`
- #72：`task-72/scratch/issue-72/target/final-reproduce.jar`

### CI

程式提交 `45bbbe4cfd4f21b0080caa2726ac2928d0b8cc1c` 的 CI 工作如下；最後僅補報告的提交不改工具，最終 HEAD 狀態以 [PR #260 checks](https://github.com/chaoren0418-arch/lito-launcher-server/pull/260/checks) 為準，交付前另核對。

| 工作 | 收據 |
|---|---|
| Semantic gap | [127575699477](https://github.com/chaoren0418-arch/lito-launcher-server/actions/runs/42121774938/job/127575699477) |
| Server 建置 | [127575699597](https://github.com/chaoren0418-arch/lito-launcher-server/actions/runs/42121774938/job/127575699597) |
| Launcher 建置 | [127575699081](https://github.com/chaoren0418-arch/lito-launcher-server/actions/runs/42121774938/job/127575699081) |
| Server tests | [127575699727](https://github.com/chaoren0418-arch/lito-launcher-server/actions/runs/42121774938/job/127575699727) |
| Build-Package.ps1 zip | [127575698526](https://github.com/chaoren0418-arch/lito-launcher-server/actions/runs/42121774766/job/127575698526) |

已讀取 Semantic gap 實際日誌：十套自測 **1845 checks**、`SELFTEST ALL PASS` 與 `FALLBACK ALL PASS` 全部確認，包含新增反例的 JVM 收據；沒有只看 non-blocking 工作總狀態。摘錄保留 `scratch/issue-254/ci-semantic-proof.txt`。CI 整包對正式 reference 為 2160 SAME、28 SAME_PATHS、21 SAME_CFG、3 LAMBDA_PAIRED_BY_CALLSITE、337 DIFF、1382 ONLY_LEFT、5 ONLY_RIGHT；輸入不同，不能與本任務 staged 驗收混用。

## 推測

上述 SAME_CFG 是固定 bytecode 在本工具模型下的等價證明，不能推論遊戲戰鬥或並行行為已實機驗收。非唯一入口值與工具不支援的控制流程仍可能保留假陰性。

## 待驗證／未驗證什麼

- 九批重跑與本機／CI 自測已完成；最終報告提交的 checks 於交付前另外核對。
- 未部署、未啟動伺服器／登入器／遊戲、未連 DB。未讀取機密設定、封存、releases 或修改硬連結。
- #69 剩餘兩類及 #70 既有差異由各批後續任務處理。

## 產出路徑與給下一棒的提示

- 交付：`lito伺服器/LiTo381-Server/tools/semantic-diff/**`、`docs/reports/issue-254.md`。
- 暫存腳本與完整前後 JSON／Markdown／日誌：`scratch/issue-254/`。工具、fixtures 及 staged 複本：工具 `target/issue-254/`，均不進 git。原 worktree 僅讀取，一般複製後核對 JAR SHA 及完整 class 集合。
- 重跑：`python -B -X utf8 scratch/issue-254/verify.py build tests`；批次入口 `scratch/issue-254/batches.py`（需將同目錄加入 Python module path）。跨平台自測為工具 `selftest.sh`、`selftest-fallback.sh`。
- Claude 審查重點：入口 stack 來源限定、例外 handler 次序、固定點重新導向、空 stack 切分與既有 handler 清理順序。使用者合併後，由調度器讓 #69 用新版比較器更新剩餘兩類診斷；不需修改本次四個方法的 staged Java 或新增豁免。
