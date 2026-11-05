# Issue #216：semantic-diff 單一屬性 metadata、純轉拋與 public owner 解析

## 結論／做了什麼

完成三項規則與測試。#70 四個 EnclosingMethod entry 均 SAME；#68 全部 56 entries 為 **54 SAME、2 SAME_CFG、0 DIFF**；#69 的 `NativeCombatAttacker.calculated(Llocal/lito381/server/NativePlayerCommit$Binding;)V` 經 public 虛擬呼叫 owner 證明通過。#62、#65、#67、#72 完整 classes JSON（包含逐方法結果）、counts 與 classSelection 均與修改前相同，維持 0 DIFF。

沿用既有 worktree `D:/lineage開發/3-lito開發/scratch/wt/task-216` 與 `task/216`；比較器修改前基線為 `567b0cb7762864b10ef876f90749e74a66e9bb01`。只修改 `tools/semantic-diff/**`、追加指定的四條 metadata 核准及本報告。未修改 src/main、staged Java、docs/REPORT.md，未讀取機密設定、封存或 releases，未建立或修改硬連結，未啟停正式程序、遊戲或 DB。

## 實作與邊界

1. `metadata-exempt` 解析新增 `類別 enclosing-method # approved: <url>`，獨立保存及輸出 JSON `enclosingMethodExempt`；僅忽略 `outerMethod`／`outerMethodDesc`。`outerClass`、Nest、InnerClasses、旗標、註解、欄位與方法照比，舊純類名行為不變。未知屬性、過多欄位及範圍外類別拒絕。四條新增均引用 [#70 核准](https://github.com/chaoren0418-arch/lito-launcher-server/issues/70#issuecomment-6660410007)。
2. `PureRethrowHandlers` 在兩側載入樹、建立 local webs 之前執行；不修改 JAR bytes。直線本體以 stack／local 值追蹤證明 ATHROW 拋的是入口保存的同一例外；只允許安全搬移、常數、POP 與 dead stores，不接受呼叫、欄位、monitor、配置、cast 或其他可能拋出指令。禁止正常跳躍／fallthrough 進入本體。共用同一 handler 的相同 catch 型別條目一起移除；每個受保護拋出點的剩餘有序 handler 鏈必須與 ATHROW 的鏈相同。另以正常／例外 CFG liveness 確保 catch 寫入不被外層觀察；迴圈透過 worklist 處理，JSR／RET 拒絕。
3. #68 實際還留有正常路徑的鎖搬移殘骸。僅在純轉拋成功移除的方法中，消除相鄰 LOAD／POP、DUP／POP，以及從被刪 handler 辨識到、此後不再被讀取的鎖暫存。STORE 改為 POP，不刪除產生值的呼叫；不跨 label 或有註解的指令。保留原 #186 核准 helper 的單向限制與行為。
4. `VirtualCalls` 對名稱／描述子一致、按呼叫序列提出的候選，在兩側 classpath 都核對 public 非 static 解析、owner 父子關係、各側 stack receiver 型別為兩者子型別。只改比較用副本；再做完整線性／paths／CFG 比較。非 public 方法、SPECIAL／STATIC、不明確的介面解析、未知 receiver、缺 classpath 均保留差異。另檢查 owner 類別可見性與 class/interface opcode 一致性，避免掩蓋 linkage error；不初始化應用類別。
5. #68 接續 CFG 時還遇到 `DUP; STORE` 與 `STORE; LOAD` 的值來源差異。CFG 仍保留每次 STORE 的有序事件及槽位，但 LOAD 取得的是該 STORE 保存的原值；跨區塊 stack 邊界、IINC／覆寫事件與全部例外邊仍檢查。這項調整只作用於 CFG 的 view，不改其他比較模式。

診斷開關為 `-Dsemanticdiff.disable=pure-rethrow` 與 `-Dsemanticdiff.disable=virtual-owner`。不提高路徑或搜尋上限、不加整類豁免。

## 已驗證／怎麼驗證

### 自我測試

Microsoft OpenJDK 17.0.17、ASM／asm-tree／asm-analysis 9.7。八套 Java 自測均以 `-Xverify:all` 執行：**517、73、119、169、159、96、265、193，共 1591 checks 全過**。`NinthSelfTest` 已接入正式 `selftest.sh`。反例的比較器與 platform-parent 隔離 ClassLoader 使用完全相同的 framed JAR；沒有以 VerifyError 冒充行為差異。

| 反例 | JVM 左／右 | 比較 |
|---|---|---|
| handler 的副作用呼叫後 POP | 3／1 | 雙向 DIFF |
| handler 改拋新例外 | 0／1 | 雙向 DIFF |
| handler 真正執行 MONITOREXIT | 0／1；左側未持有 monitor，外層收到 IllegalMonitorStateException | 雙向 DIFF |
| 轉拋後外層目的不同 | 5／1 | 雙向 DIFF |
| 外層讀取 catch 寫過的 local | 2／1 | 雙向 DIFF |
| 清理鎖暫存時，正常路徑仍有呼叫後 POP | 3／1 | 雙向 DIFF |
| 跨套件 package-private Base.f 與 Child.f | 1／2 | 雙向 DIFF |
| 兩個直接介面的 INVOKESPECIAL owner | 1／2 | 雙向 DIFF |
| 不同繼承鏈的 interface owner | 1／IncompatibleClassChangeError | 雙向 DIFF |
| 無法解析方法 | NoSuchMethodError／2 | 雙向 DIFF |
| private 方法 | IllegalAccessError／2 | 雙向 DIFF |
| INVOKESTATIC owner 不同 | 1／2 | 雙向 DIFF |
| int／long 暫存被覆寫 | 2／0 | 雙向 DIFF，直接 CFG 亦拒絕 |

正例包含 catch-any／typed catch 純轉拋、共享 handler、dead local／wide POP2、原有外層 handler、public 覆寫與 interface 父子鏈，以及跨區塊仍存活的 int／long 暫存。消融純轉拋及 owner 正規化後，對應正例恢復 DIFF。metadata 正例涵蓋改名與描述子；outerClass／nestHost／synthetic／InnerClasses／欄位／方法內容反例仍 DIFF，並核對舊純類名行為。

### 固定 staged 輸入與回歸

以 `git ls-remote` 核對各 task 分支最新提交；#68 固定使用指定 `f8e3e9e`。從原 worktree 唯讀取得既有 staged JAR，再以一般檔案複製至本 worktree 的工具 target；逐一確認 SHA-256 出現在該提交報告，class 集合精確等於該提交 batches.json 的 verify_entries。沒有修改 staged 或以候選 class 覆蓋輸出。

| 批次 | 來源提交 | entries | DIFF 前→後 | 最終類別計數 |
|---|---|---:|---|---|
| #62 | `e01c6b966fee` | 22 | 0 → 0 | 20 SAME、1 SAME_EFFECTS、1 LAMBDA_PAIRED_BY_CALLSITE |
| #65 | `6fbbe7a373a2` | 98 | 0 → 0 | 95 SAME、1 SAME_PATHS、1 SAME_CFG、1 DEVIATION |
| #67 | `1c887286fef4` | 323 | 0 → 0 | 311 SAME、10 SAME_PATHS、2 SAME_CFG |
| #68 | `f8e3e9e6aa96` | 56 | 2 → 0 | 54 SAME、2 SAME_CFG |
| #69 | `b05465a2bf04` | 160 | 5 → 4 | 141 SAME、10 SAME_PATHS、5 SAME_CFG、4 DIFF |
| #70 | `b63c655e1c50` | 362 | 7 → 3 | 357 SAME、2 SAME_PATHS、3 DIFF |
| #72 | `09af151ea7eb` | 185 | 0 → 0 | 183 SAME、2 DEVIATION |

各批 ONLY_LEFT／ONLY_RIGHT 均為 0，missingLeft／extraLeft 均為空。#72 仍含原有 DEVIATION，並保留 169 個方法級 VISIBILITY_BRIDGE；不能稱整批每個方法都是 SAME。#62、#65、#67、#72 的完整 classes JSON 不變，並非只核對摘要 0 DIFF。

| staged JAR | SHA-256 |
|---|---|
| #62 | `e05d3f432413ecc65aa3f4f5a1d68b196de634f954935230763b443bfa92501f` |
| #65 | `da35c6b2d6cf165b1afdfefb6c1409bf69c4aff7a66f5917e9cb9bc6fe0cc92e` |
| #67 | `8820abf5684b6fd6c9c55826372a21bad10f5f965880d4a954e57fe22d1640c6` |
| #68 | `ef49e8d94855fac2d6503c18f18b285f40bc63a9cb3e0471d7c9ed1dc77d3727` |
| #69 | `88425cbf5dd6bd5e2881dbac1c838d2c56829e0ef80c2a3f9f26956aabc9d77b` |
| #70 | `2111ea1efeb4996b3293cc9d720398dd4ed0861c2353f55f637de0cbe4f1e302` |
| #72 | `63e970a0ed1bd26f94586b79df3de840e6231b46977ce63215d5427e55363808` |

B 批 reference 為 `50635f7edca8ec6ce77787cc32371c7590b497e48ea202f8a679caf3f517b729`；A 批與 T029 allow-right 為正式 `e1498871c7be6b287821c57ef5d95933db1740d9b4d4a80f6c3465f21d99d498`。classpath 指向對應 reference，#70 另加正式 JAR。

核准只取該批既有精確條目：#62 effects／exceptions 各 1；#65 既有兩條 method-diff；#67／#68 無核准；#72 既有 access-widen 與 deviations。#69 使用其最新報告列出的 access-widen 1、extra-method 1、exceptions 3（後兩者原存該批 scratch 清單，逐行核對在該提交報告中），不新增核准。#70 沿用其既有 T029 allow-right、effects／extra 清單，另加本任務四條 enclosing-method。曾另跑無核准的 #69 探查，該結果未混入上表正式核准結果。

### 三項指定證據

- #70 的 `ClientSession$1CompanionHealing`、`$1PlayerHealing`、`$2CompanionHealing`、`$2PlayerHealing`：修改前皆只因 outerMethod DIFF；精確核准後四者皆 SAME。整批仍有其他未證明差異，沒有因這四條核准忽略外層類別。
- #68：`CombatCompanionSpells#nativeShockCompanionBody` 與 `CombatNpcDebuffs#nativeMaterialControlledNegativeBody` 均 **SAME_CFG**。停用 pure-rethrow 即恢復兩個 DIFF，證明結果不是由核准清單消掉。
- #69：第 3 項實際消除 **一個方法**：`NativeCombatAttacker#calculated(Llocal/lito381/server/NativePlayerCommit$Binding;)V`，完整比較通過；該類其他方法沿用 SAME_PATHS，因此類別 DIFF → SAME_PATHS。停用 virtual-owner 恢復該方法 DIFF，其他規則仍開啟。javap 確認該呼叫 staged owner 是 `java/lang/Object`，candidate owner 是 `NativeCombatAttacker$Ref`，名稱／描述子均為 `equals(Ljava/lang/Object;)Z`。

核准模式下 #69 剩餘四個 DIFF 類別是 `NativeLightAction`、`NativeLightAction$Batch`、`NativePlayerCommit`、`NativeProcHealingEncounter$Dispatch`；#70 剩餘三個為 `ClientSession`、`ClientSession$1`、`GameHub`。保留其逐方法診斷，不推定為等價。執行期間 #67／#72 分支完成新一輪 #203 重驗，本輪已改用其最新報告所列 `cfg-final/staged.jar`／`verify-203/target/staged.jar` 再跑；兩個 JAR 的 SHA-256 與原固定輸入相同。

## CI

程式提交 `743c55ee924dd4942bfc0bd717e53081fd319fdd` 的 CI 收據如下：

| Job | 收據 |
|---|---|
| Semantic gap (non-blocking) | [127524490448](https://github.com/chaoren0418-arch/lito-launcher-server/actions/runs/42107044943/job/127524490448) |
| Server tests | [127524490838](https://github.com/chaoren0418-arch/lito-launcher-server/actions/runs/42107044943/job/127524490838) |
| Launcher (.NET 10 / WinForms) | [127524490890](https://github.com/chaoren0418-arch/lito-launcher-server/actions/runs/42107044943/job/127524490890) |
| Server (Java 17 / Maven) | [127524491022](https://github.com/chaoren0418-arch/lito-launcher-server/actions/runs/42107044943/job/127524491022) |
| Build-Package.ps1 zip | [127524490556](https://github.com/chaoren0418-arch/lito-launcher-server/actions/runs/42107044939/job/127524490556) |

已讀取 Semantic gap 完整日誌，八套自測為 517／73／119／169／159／96／265／193，共 **1591 checks 全過**，並確認 `SELFTEST ALL PASS`、`FALLBACK ALL PASS`；未以 non-blocking job 總狀態代替自測證據。本機摘錄為 `scratch/issue-216/ci-semantic-proof.txt`。

CI 整包 Maven 產物對正式 reference 為 2160 SAME、28 SAME_PATHS、19 SAME_CFG、3 LAMBDA_PAIRED_BY_CALLSITE、339 DIFF、1382 ONLY_LEFT、5 ONLY_RIGHT；這與本任務固定 staged 的輸入不同，不能混入批次驗收。

最後僅補報告的提交不改工具或測試。最終 HEAD 的五項檢查以 [PR #238 checks](https://github.com/chaoren0418-arch/lito-launcher-server/pull/238/checks) 為準，交付前另外核對。

## 推測

這些結果是固定 JAR 與本工具保守比較模型下的證明，不能推出遊戲戰鬥或並行行為已實機驗收。未明確的 receiver 匯合、介面方法解析或控制流程可能保留假陰性。

## 待驗證／未驗證什麼

- 未部署、未執行遊戲／伺服器／登入器或連 DB。
- #69、#70 尚有既有 DIFF；本任務只交付工具修正與指定消除項，不能當作這兩批 B-land 全量驗收完成。
- 最後的報告提交之 CI 於交付前核對；上列固定輸入與自測已完成。

## 產出路徑／重跑

- 交付：`lito伺服器/LiTo381-Server/tools/semantic-diff/**`、`docs/combat-merge/metadata-exempt.txt`（僅四條追加）、`docs/reports/issue-216.md`。
- 暫存腳本與完整 JSON／Markdown／日誌：`scratch/issue-216/`；建置、fixture、staged 複本：`lito伺服器/LiTo381-Server/tools/semantic-diff/target/issue-216/`，均不進 git。
- 原始 JAR 的精確路徑、分支 SHA、核准文字見 `scratch/issue-216/review-inputs.json`；#69 的核准清單另逐行保留於該目錄。

```powershell
& D:/dev-orchestrator/tools/python/python.exe -B -X utf8 scratch/issue-216/verify.py build tests
& D:/dev-orchestrator/tools/python/python.exe -B -X utf8 -c "import sys,runpy;sys.path.insert(0,'scratch/issue-216');runpy.run_path('scratch/issue-216/batches.py')"
```

跨平台入口為工具 `selftest.sh`、`selftest-fallback.sh`。批次比較使用 `--left <staged.jar> --right <reference.jar> --left-classpath <reference.jar> --classes <verify_entries.txt>`，再加上該批精確核准；#70 加 `--allow-methods <T029> --allow-right <formal.jar>`。

## 給下一棒的提示

由 Claude 審查純轉拋的外層 dispatch／liveness、正常路徑鎖暫存清理、public owner 的 classpath 與 receiver 證據，以及 CFG 保留 STORE 事件而沿用原值的邊界。全部有效反例須持續 DIFF。通過後交使用者合併，由調度器讓 #68 更新 56-entry 驗收；#69／#70 接手者使用新版工具保留其餘差異，無需為本任務改 staged Java 或擴張豁免。
