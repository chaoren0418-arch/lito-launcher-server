# Issue #235：semantic-diff private 方法改名配對

## 結論／做了什麼

完成 `private-rename` 解析、方向性配對、本類呼叫與遞迴改名、完整本體驗收及 JSON／Markdown 對應資訊。#73 的 `NativeDeNpcConstructionState` 由 DIFF 轉為 DEVIATION，完整 104 entry 為 **86 SAME、16 SAME_PATHS、2 DEVIATION，0 DIFF／ONLY_LEFT／ONLY_RIGHT**。#72、#74、#77 的 counts、classes（含逐方法結果）、classSelection 與修改前完全一致。

沿用 worktree `D:/lineage開發/3-lito開發/scratch/wt/task-235`、分支 `task/235`；修改前工具基準為 `43a4f7a349099d638dcd8258dd6c0ae441a6c431`。只修改指定工具、追加唯一 private-rename 核准與說明、本報告。沒有修改 src/main、staged Java、docs/REPORT.md、機密設定、封存、releases 或硬連結；原專案與其他 worktree 僅讀取，未啟動正式程序、遊戲或 DB。

## 實作界線

- 格式為 `類別#左側方法描述子 private-rename 右側方法名 # approved: <url>`；左右名稱均固定原始識別，不參與 lambda 合成名稱重編。
- 用 class header 檢查兩側均 private 實例方法、兩側祖先有右側同名同描述子的非 private 實例方法、左側完全沒有右側原名宣告（包括 overload）；右側不得另有左側名稱與描述子，避免配對碰撞。祖先可來自 classpath，不初始化應用類別。
- 守門失敗保留原 ONLY_LEFT／ONLY_RIGHT 方法差異，類別為 DIFF；右側目標即使為 synthetic，也不能被 orphan 規則消去。範圍外、格式錯誤、缺核准、重複端點、與其他方法豁免重疊均拒絕。
- 在右側比較樹中，只統一本類 owner 的 INVOKEVIRTUAL／INVOKESPECIAL 呼叫名稱（含遞迴）；保留原宣告名稱供報表配對。祖先／外類 owner、STATIC／INTERFACE、method handle 與字串不改名。原始 JAR bytes 不變。
- 配對後仍走完整 metadata、本體、try／控制流程比較，不做 method-diff 豁免。成功方法列 DEVIATION；本體不同仍 DIFF。JSON `privateRenames` 是核准清單，`methodResults` 才是實際配對狀態；Markdown 同時列出對應。
- 唯一新核准引用 [#73 主控留言](https://github.com/chaoren0418-arch/lito-launcher-server/issues/73#issuecomment-6668602650)。理由為 Java 無法用 private 遮蔽祖先非 private 同簽名方法，改名後保留正式版的祖先派送。

## 已驗證／怎麼驗證

### 自我測試

Microsoft OpenJDK 17.0.17，ASM／asm-tree／asm-analysis 9.7（三個依賴均核對工具既有 SHA-256）。九套 Java 自測以 `-Xverify:all` 執行：**517、73、119、169、159、96、265、193、106，共 1697 checks 全過**。新增 `TenthSelfTest` 已接入正式 `selftest.sh`。

新增測試包括兩種 private 呼叫 opcode、遞迴、呼叫者、無核准與反方向、左右非 private、保留原名／overload、本體與 metadata 不同、缺方法／祖先、private 祖先、外部 owner、method handle、classpath 補足，以及解析與核准重疊拒絕。比較與 JVM 使用同一份 framed fixture JAR，隔離 ClassLoader 的 parent 是 platform loader；沒有用 VerifyError 代替可執行反例。

fixture 的公開 `run()` 結果為 `祖先 dispatch() * 10 + 本類 private 結果`；祖先結果 1、本類結果 2，遞迴也保留。

| 案例 | JVM 左／右 | 工具結果 |
|---|---|---|
| 正確 private 改名 | 12／12 | DEVIATION |
| 右側改為 public，覆寫祖先 | 12／22 | DIFF |
| 左側保留原名 public 覆寫，回傳 3 | 32／12 | DIFF |
| 左側 private 本體改回傳 3 | 13／12 | DIFF |

上述正反例對 INVOKEVIRTUAL 與 INVOKESPECIAL 各跑一次。

### 固定 staged 回歸

| Issue／批次 | entries | 最終結果 | 修改前後 |
|---|---:|---|---|
| #72／A01 | 185 | 183 SAME、2 DEVIATION | 完整 classes／counts／selection 不變 |
| #73／A02 | 104 | 86 SAME、16 SAME_PATHS、2 DEVIATION | 1 DIFF → 0 DIFF |
| #74／A03 | 88 | 87 SAME、1 DEVIATION | 完整 classes／counts／selection 不變 |
| #77／A06 | 69 | 67 SAME、2 SAME_EFFECTS | 完整 classes／counts／selection 不變 |

四批 DIFF／ONLY_LEFT／ONLY_RIGHT 皆 0；missingLeft／extraLeft 皆空，JAR class 集合精確等於 a-batches.json 的 verify_entries。各批沿用既有核准，#73 另加本次唯一 private-rename；沒有把新版完整核准清單套到其他批次。

| 輸入 | SHA-256 |
|---|---|
| 正式 reference | `e1498871c7be6b287821c57ef5d95933db1740d9b4d4a80f6c3465f21d99d498` |
| #72 staged | `63e970a0ed1bd26f94586b79df3de840e6231b46977ce63215d5427e55363808` |
| #73 本輪重新編譯 staged | `1f0cbef567459be4721bd3aeeea13ad24bd7d8f72b06744b2bb70351d3afdb52` |
| #74 staged | `9f3555fed2db3e6d61db3213189f74f0b5d283e21997774982be2bd92ac3ea45` |
| #77 staged | `2fb2717cbad047df6ff642790f3dfb1cd683324ccaf1e31c6de5877ce2af5b43` |

#72／#74／#77 唯讀取得各 worktree 的既有 JAR，核對上述雜湊與其已合併報告一致，再一般複製至本工具 target。來源分別是 `task-72/scratch/issue-72/verify-203/target/staged.jar`、`task-74/scratch/issue-74/final/target/a03.jar`、`task-77/lito伺服器/LiTo381-Server/target/a06-reverify/left.jar`。

#73 的 worktree HEAD 仍為 `d0a15501d5b979618da6fabd4ae50ab8c863d6f0`，staged 尚未套用改名。為遵守本任務不修改 staged，讀取審查者既有 `D:/lineage開發/3-lito開發/scratch/review-73/r3/rename-src/NativeDeNpcConstructionState.java`；逐字斷言其只與原 staged 相差核准的兩處改名，SHA-256 為 `c2bce1412a147fc8b526d7062298b6d2a63d62337d6d7c259e36d70b9bb73d81`。其餘 48 份仍讀取 task-73 的 staged；本輪將完整 49 份以 javac --release 17 重新編至本 worktree 全新 target，未重用審查者的 class，也未補正式 class。

沿用 #73 已核准的建置流程，以本分支既有 AsRecord 工具完成 @AsmFinal：104 class 中僅 `NativeDeItemTable.class` 改變。這是既有 final 欄位轉換，不是改名 patch；本輪改名 class 由 Java 直接編出。classpath 使用 #73 既有的 A03／註解與 legacy-stubs 產物，stub 不打包。49 份來源的逐檔 SHA-256 留在本機收據。

同份 #73 JAR 對舊工具為 86 SAME、16 SAME_PATHS、1 DEVIATION、1 DIFF；新版加核准後，`NativeDeNpcConstructionState` 為 DEVIATION，`cancelNpcChatTimer$own()V → cancelNpcChatTimer()V` 完整本體通過，`sourceChat$startChat(I)V` 為 SAME_PATHS（1310 個路徑多重集合相等）。該類 176 個 VISIBILITY_BRIDGE 保留。另一個 DEVIATION 是既有的 `NativeDeItemWeaponProcTable.reload()V`。

新版工具移除 private-rename 核准再跑同份 JAR，恢復 1 DIFF：原名為 ONLY_RIGHT、新名為 ONLY_LEFT、`sourceChat$startChat(I)V` 為 DIFF。javap 同時確認兩側 offset 139 均為 invokevirtual，左側目標為 `cancelNpcChatTimer$own:()V`，右側為 `cancelNpcChatTimer:()V`，宣告均 private；`sourceChat$cancelNpcChatTimer()` 保留原名。

### CI

程式提交 `2ea0999be1b1a570b315b77ec40dc7d5300019a4` 的 CI 收據：

| Job | 收據 |
|---|---|
| Semantic gap (non-blocking) | [127553943494](https://github.com/chaoren0418-arch/lito-launcher-server/actions/runs/42115505840/job/127553943494) |
| Server tests | [127553943430](https://github.com/chaoren0418-arch/lito-launcher-server/actions/runs/42115505840/job/127553943430) |
| Server (Java 17 / Maven) | [127553943273](https://github.com/chaoren0418-arch/lito-launcher-server/actions/runs/42115505840/job/127553943273) |
| Launcher (.NET 10 / WinForms) | [127553942918](https://github.com/chaoren0418-arch/lito-launcher-server/actions/runs/42115505840/job/127553942918) |
| Build-Package.ps1 zip | [127553943272](https://github.com/chaoren0418-arch/lito-launcher-server/actions/runs/42115505846/job/127553943272) |

已讀取完成的 Semantic gap 日誌：九套自測同為 517／73／119／169／159／96／265／193／106，共 **1697 checks 全過**；明確包含 `SELFTEST ALL PASS` 與 `FALLBACK ALL PASS`。沒有以 non-blocking job 總狀態代替自測證據。最後僅補本報告，不改程式／測試；最終 HEAD 的五項檢查以 [PR #252 checks](https://github.com/chaoren0418-arch/lito-launcher-server/pull/252/checks) 為準，交付前另行核對。

## 推測

結果限於固定輸入及工具的保守等價模型，不能推出遊戲或並行情境已驗收。缺少祖先 classpath、method handle 改名等未支援形狀可保留假陰性；本任務沒有擴張這些規則。

## 待驗證／未驗證什麼

- 未部署、未啟動伺服器／登入器／遊戲或連 DB。
- #73 的正式 staged 檔案仍需該任務執行者依主控裁定修改並重編。本輪驗證的是審查者既有兩處改名來源與其餘 staged 組成的完整批次。
- #78 的「刪除 NPC 時會呼叫 chat retire」應用層派送測試仍須完成，fixture 執行不代替該測試。

## 產出路徑／重跑

- 交付：`lito伺服器/LiTo381-Server/tools/semantic-diff/**`、`docs/combat-merge/deviations.txt`／`deviations.md`（僅追加）、`docs/reports/issue-235.md`。
- 本機暫存：`scratch/issue-235/`，含 `verify.py`、`batches.py`、自測日誌、每批完整 JSON／Markdown／argv、`batches-receipt.json`、`a02-source-sha.json`。
- 建置與 fixture：`lito伺服器/LiTo381-Server/tools/semantic-diff/target/issue-235/`。暫存與建置產物不進 git。

```powershell
& D:/dev-orchestrator/tools/python/python.exe -B -X utf8 scratch/issue-235/verify.py classes tests
& D:/dev-orchestrator/tools/python/python.exe -B -X utf8 scratch/issue-235/batches.py
```

`batches.py` 故意拒絕重用既有 A02 classes 目錄，重新完整編譯時須指定本 worktree 內新的 target 子目錄；舊工具 baseline 是修改前先行編譯的快照。跨平台自測入口為 `selftest.sh`／`selftest-fallback.sh`。批次 CLI 為 `--left <staged> --right <formal> --left-classpath <formal> --classes <entries>`，再加該批精確核准；完整 argv 留在上述 scratch 收據。

## 給下一棒的提示

Claude 審查 private 方向性守門、只有本類指令改名、完整本體比較與三個實際 JVM 反例。通過後由使用者合併；調度器交 #73 執行者將兩處改名落入其 staged、使用新版工具重跑完整 104 entries，並保留 reload 的既有核准。#78 接手應用層 chat retire 派送測試。
