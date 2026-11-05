# Issue #186 — T020i-7 semantic-diff 對稱 switch、原樣轉拋與 private 呼叫

## 結論與做了什麼

三項修正已完成，六套 Java 自測 **1,133 項全過**。#74 全部 88 entries 為 **87 SAME＋1 DEVIATION、DIFF 0**；#77 全部 69 entries 為 **67 SAME＋2 SAME_EFFECTS、DIFF 0**。#70 消除 10 個方法的 DIFF；#72、#76 完整 class rows（含逐方法結果及差異細節）與修改前完全相同。

分支為 `task/186`，起點 `a32d30d`，沿用指定 worktree，未 checkout／另建分支。修改限 `tools/semantic-diff/**` 與本報告；未改伺服器 src/main、staged Java、核准清單或 docs/REPORT.md，未操作正式程序、遊戲、DB、機密設定、封存、releases 或硬連結。

- `$SwitchMap$` 改名以名稱前綴與 `[I` 描述子為準，兩側依宣告類別的欄位順序編號，所有欄位指令及 handle 共用映射。欄位 flags 沿用 #132 忽略 SYNTHETIC；類別 metadata 沿用既有豁免。額外以 descriptor 限制參照映射，避免誤改同名不同型別欄位。
- 新增 `RethrowHandlers`：只對左側建立成對比較副本。驗證 handler 的 ASTORE／ALOAD／核准 INVOKESTATIC，以及 helper 的 ALOAD 0／安全 CHECKCAST／ATHROW。逐指令檢查右側對應操作沒有 handler，才刪除例外表項與 handler，保留原 try 中的業務指令；處理不可達尾端與 javac 跳過 catch 的 goto-next。候選仍交既有完整比較或原先核准的 effects 比較。
- 同類宣告的 private 實例方法，182／183 統一為 INVOKESPECIAL；其他 owner、nestmate、非 private、static 與建構子照常比較。保留 `private-calls`、`rethrow` 消融開關供診斷。
- 新增 `SeventhSelfTest` 並接入既有 `selftest.sh`；README 與 CLI Markdown 說明同步更新。

## 已驗證

### 自我測試

使用 Microsoft JDK 17.0.17、ASM 9.7，從本次來源重編全部工具及測試，六套均以 `-Xverify:all` 執行。測試只載入人工 fixture，不執行伺服器類別。

| 測試 | 通過數 |
|---|---:|
| NormalizationSelfTest | 517 |
| AdvancedSelfTest | 73 |
| FourthSelfTest | 119 |
| FifthSelfTest | 169 |
| SixthSelfTest | 159 |
| SeventhSelfTest | 96 |
| 合計 | 1,133 |

新測試包含 SYNTHETIC 單側缺失、名稱不同、跨類 switch 引用、使用點交換、enum／ordinal 變更、非 `[I` 欄位；同類 private 183/182 正例與非 private、其他 owner、實際 private nestmate 目標反例；handler 的 RETURN／ATHROW／GOTO／共用 continuation 正例及未核准、額外指令、synchronized、共用入口／別名入口反例。

第 2 項指定反例使用**同一份 framed JAR bytes** 分別交 CLI 與 platform-parent 隔離 ClassLoader，每側使用新 loader；傳入 IOException 及 IllegalArgumentException，檢查例外型別、原物件 identity、記錄計數及正常返回。以下以 IOException 說明：

| 反例 | 左側實際行為 | 右側實際行為 | 比較 |
|---|---|---|---|
| helper 包裝 RuntimeException | 新 RuntimeException，identity=false | 原 IOException，identity=true | DIFF |
| helper 先呼叫 log | 原 IOException，logs=1 | 原 IOException，logs=0 | DIFF |
| handler 先呼叫 log | 原 IOException，logs=1 | 原 IOException，logs=0 | DIFF |
| 右側已有 handler | 左側原 IOException | 右側正常返回 | DIFF |
| helper 窄化 CHECKCAST RuntimeException | ClassCastException | 原 IOException | DIFF |

純轉拋與安全 CHECKCAST Exception 均 SAME，例外 identity 保持。未核准純 helper 雖然執行相同，仍 DIFF；左右互換亦不套用左側專屬規則。

### 五批固定輸入回歸

修改前工具從起點 `a32d30d` 的 git blob 匯出至 scratch 重編，已包含 #176。兩套工具使用相同 staged bytes、參考 JAR、驗收清單及核准旗標。#70／#72／#76 的遠端 HEAD 經 `git ls-remote` 核對；#74／#77 使用題目指定提交。五批共 **864 entries**，每輪 missingLeft、extraLeft、ONLY_LEFT、ONLY_RIGHT 皆為 0。

| 批次／提交 | entries | 修改前 | 修改後 |
|---|---:|---|---|
| #74 `1b38344` | 88 | 85 SAME、1 DEVIATION、2 DIFF | 87 SAME、1 DEVIATION、0 DIFF |
| #77 `ff99993` | 69 | 67 SAME、1 SAME_EFFECTS、1 DIFF | 67 SAME、2 SAME_EFFECTS、0 DIFF |
| #70 `a58800f` | 362 | 353 SAME、2 SAME_PATHS、7 DIFF | 類別結果相同；10 個方法消除 DIFF |
| #72 `f4b20cb` | 185 | 182 SAME、3 DIFF | 完整 class rows 相同 |
| #76 `0d3101a` | 160 | 158 SAME、1 SAME_EFFECTS、1 SAME_LAYOUT | 完整 class rows 相同，DIFF 0 |

#74 的 NativeDeVisibilitySwitch 與 NativeDeVisibilityPc 恢復 SAME，保留 NativeDeWorldLog 的既有 DEVIATION。#77 的 ServerConfig 恢復 SAME_EFFECTS，沒有新增核准條目，也沒有把 effects 結果稱為嚴格 SAME。

#72 使用該最新提交已核准的 6 筆 deviations 與 1 筆 access-widen；#70 保持原批的 T029 allow-methods／正式 allow-right 及 effects 範圍，其餘取本分支已合併清單與驗收集合交集。沒有為通過測試擴大 metadata、effects、layout 或其他豁免。

四批既有 staged JAR 的 SHA-256 均符合指定提交報告。#76 原 worktree 已不存在，從 `0d3101a` 的 git blob 擷取 manifest 指定 27 份 Java 至本 worktree scratch，用 reference／lib 唯讀 classpath 重編，固定 jar 日期為 2026-11-05T00:00:00Z，產物雜湊與該提交報告完全相同。

| 批次 | staged SHA-256 |
|---|---|
| #74 | `9f3555fed2db3e6d61db3213189f74f0b5d283e21997774982be2bd92ac3ea45` |
| #77 | `2fb2717cbad047df6ff642790f3dfb1cd683324ccaf1e31c6de5877ce2af5b43` |
| #70 | `c4ea3dddd328d629e98577d9ca75712ee8ecd6245cf51c85d9eeee452edb9ffd` |
| #72 | `6b0f198773388779c8ea56d39836b33bc55716eff5858febc878d1e030760894` |
| #76 | `e347988dcf41b60cb915195ffa98dc2eff74303ae3c9f0400c2800bf5bc2b553` |

正式 reference：`e1498871c7be6b287821c57ef5d95933db1740d9b4d4a80f6c3465f21d99d498`；候選 reference：`50635f7edca8ec6ce77787cc32371c7590b497e48ea202f8a679caf3f517b729`。

### #70 因 private 呼叫規則消除的方法

全部屬於 `local/lito381/server/ClientSession`。停用 `private-calls` 後，逐方法狀態表與修改前完全一致；啟用後只有下列 10 筆狀態改變，沒有其他方法由 DIFF 轉成通過。逐方法 DIFF 由 408 降為 398，ONLY_RIGHT 63、ONLY_LEFT 9、ORPHAN_IGNORED 8 不變。一般 SAME 不寫入 methodResults，因此透過表中缺項與完整比較成功判讀。

| 方法（完整描述子） | 啟用後 |
|---|---|
| `nativeSourceCubeVisibilitySend([BLjava/lang/String;)V` | SAME |
| `nativeTimelineHandle([B)V` | SAME |
| `nativeSkillConsumptionIntelligence()I` | SAME |
| `nativeSkillConsumptionOriginalReduction()I` | SAME |
| `isAdministrator()Z` | SAME |
| `nativeSourceNpcPlayerOriginalDamage(ILlocal/lito381/server/ClientSession;)V` | SAME |
| `nativeSourceFatalityOriginalDamage(ILlocal/lito381/server/ClientSession;)V` | SAME |
| `nativeSourceFatalityOriginalSend([BLjava/lang/String;)V` | SAME |
| `nativeSourceCubePresentationStatus(JLjava/lang/String;)V` | SAME |
| `run()V` | SAME_PATHS |

### CI

本機六套自測及 git diff --check 已通過。PR 建立後核對 GitHub Actions 的 Server、Server tests、Launcher，以及 Semantic gap 中 Self-test／cold fallback 的實際 step 結果；後者是 non-blocking job，不以外層綠燈代替測試通過。CI 收據於交付前補入。

## 推測

本輪未以推測替代通過。#70 與 #72 的其餘差異屬既有批次後續工作，本任務沒有宣稱它們全部等價，也未證明任意未測 bytecode 的全面正確性。

## 待驗證與保守限制

- 尚待 PR CI 完成；真機行為與部署驗收由後續 A-land／B-land 流程處理。本任務不啟動遊戲或伺服器。
- 原樣轉拋只接受本類 private static helper；跨類 helper 可能引發 class initialization／解析副作用，因此保守不移除。CHECKCAST 需有已知 superclass 證據；未知型別、額外副作用、入口、註解、其他 handler 涵蓋及無法對齊的可能拋例外指令均保持原比較。
- #70 仍有 7 個類別 DIFF、#72 仍有 3 個類別 DIFF；這不是本工具任務未完成的三項規則，不能據此授權它們進 land。

## 產出路徑與重跑

- 提交：`lito伺服器/LiTo381-Server/tools/semantic-diff/**` 與 `docs/reports/issue-186.md`。
- 本機證據：`scratch/issue-186/` 的六套自測 log／JSON、`*-before.json`／`*-after.json`、`70-no-private.json`、`private-call-audit.json`、`inputs.json`（完整提交、雜湊、classes 及實際旗標）。只保留本任務目錄，不入 git。
- 可重建產物：`lito伺服器/LiTo381-Server/tools/semantic-diff/target/issue-186/` 的 classes、baseline、inputs、rebuild76 及 fixture JAR，不入 git。
- 本機腳本：`scratch/issue-186/verify.py`、`batches.py`、`72-policy.py`、`final-batches.py`、`audit-final.py`。先跑 verify.py 全套；batches.py 固定五批輸入及修改前／後比較，再跑 72-policy.py 套用該批最新核准與 audit-final.py 斷言狀態表。一般 CI 使用已接線的 `bash lito伺服器/LiTo381-Server/tools/semantic-diff/selftest.sh`。
- 單批重跑以 inputs.json 的 jar／reference／classes／flags 傳給 `local.lito381.semanticdiff.SemanticDiff`，左側 classpath 為該 reference；#70 另有正式 allow-right。消融只追加 `-Dsemanticdiff.disable=private-calls`，不更換輸入。

## 給下一棒的提示

- Claude 審查本工具 PR 的移除條件、覆蓋證明與反例，再由使用者合併。
- #74／#77 執行器可用本次固定輸入結果更新批次驗收，沿用既有 DEVIATION／SAME_EFFECTS，不需修改 staged Java。
- #70 執行器續修其餘 398 個逐方法 DIFF 及既有 lambda 配對／metadata 問題；#72 執行器續修原 3 個類別，不能把本次 10 個方法的改善擴張為全批通過。#76 保持已驗收狀態。
