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

已完成一輪完整 CLI 重跑，類別由 **141 SAME、10 SAME_PATHS、5 SAME_CFG、4 DIFF** 改為 **141 SAME、10 SAME_PATHS、7 SAME_CFG、2 DIFF**。四個指定方法皆從 DIFF 改為 **SAME_CFG**：

| 類別 | 方法 |
|---|---|
| NativeProcHealingEncounter$Dispatch | `run()V` |
| NativePlayerCommit | `drain(ClientSession, Queue)V` |
| NativePlayerCommit | `acceptedTail(Frame)V` |
| NativePlayerCommit | `nativeManaTransferRecover`（完整描述子見重跑 JSON） |

剩餘 DIFF 為 `NativeLightAction` 與 `NativeLightAction$Batch`，未宣稱 B08 全批通過。

### 其餘批次與 CI

九批最終固定輸入前後對照及 CI 收據正在核對，完成後更新本節。

## 推測

上述 SAME_CFG 是固定 bytecode 在本工具模型下的等價證明，不能推論遊戲戰鬥或並行行為已實機驗收。非唯一入口值與工具不支援的控制流程仍可能保留假陰性。

## 待驗證／未驗證什麼

- 尚待記錄九批最終回歸收據與本分支 CI。
- 未部署、未啟動伺服器／登入器／遊戲、未連 DB。未讀取機密設定、封存、releases 或修改硬連結。
- #69 剩餘兩類及 #70 既有差異由各批後續任務處理。

## 產出路徑與給下一棒的提示

- 交付：`lito伺服器/LiTo381-Server/tools/semantic-diff/**`、`docs/reports/issue-254.md`。
- 暫存腳本與完整前後 JSON／Markdown／日誌：`scratch/issue-254/`。工具、fixtures 及 staged 複本：工具 `target/issue-254/`，均不進 git。原 worktree 僅讀取，一般複製後核對 JAR SHA 及完整 class 集合。
- 重跑：`python -B -X utf8 scratch/issue-254/verify.py build tests`；批次入口 `scratch/issue-254/batches.py`（需將同目錄加入 Python module path）。跨平台自測為工具 `selftest.sh`、`selftest-fallback.sh`。
- Claude 審查重點：入口 stack 來源限定、例外 handler 次序、固定點重新導向、空 stack 切分與既有 handler 清理順序。使用者合併後，由調度器讓 #69 用新版比較器更新剩餘兩類診斷；不需修改本次四個方法的 staged Java 或新增豁免。
