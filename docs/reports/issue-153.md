# Issue #153 — T020i-3 比較器與 @AsmFinal

## 結論

第 2 次嘗試已修正審查退回的 P1，`ctor-prologue-order` 移動前會驗證取值依賴。有效 JVM 重現案例現在正確回報 DIFF，自測與三批固定輸入已重跑，整合提交 `ff50cca` 的 5 項 CI 全綠。#76 仍為 2 DIFF，依本次主控裁定，零 DIFF、槽位重用與一般 stack 暫存移交 #166。交付分支 `task/153`，沿用 [PR #164](https://github.com/chaoren0418-arch/lito-launcher-server/pull/164)。

## 第 2 次修正與驗證

- `Normalizations.ctorOrder` 只在跨越區間內沒有覆寫賦值輸入時移動。檢查五種 xSTORE 與 IINC，讀寫範圍均計入 long／double 的第二槽，也檢查接收者 ALOAD 0。重疊時保留原指令，後續照常比較。
- 無法用直線掃描證明安全的跳躍、switch、標籤邊界與提前退出，採保守拒絕移動。這是本次實作選擇。
- AdvancedSelfTest 新增 18 個案例，涵蓋審查提供的 `(I)V` 槽位覆寫、五種 xSTORE、IINC、雙槽讀寫的兩端重疊、WIDE 索引，以及互不重疊的正例。原有合法前後移動正例繼續通過。
- 先加負例並以舊實作執行，實際重現錯誤的 `DEVIATION=1 DIFF=0`，證據在 `scratch/issue-153/advanced-red.log`。修正後為 `ADVANCED SELFTEST ALL PASS checks=67`，原有自測為 `NORMALIZATION SELFTEST ALL PASS checks=517`。
- 只讀取主控留存的 `scratch/review-153/ReviewProbe.java`，複製到本 worktree 執行。`java -Xverify:all` 實際載入並呼叫兩側，印出 `left.value=2 right.value=7`。比較器輸出 `DIFF=1 DEVIATION=0`，共同 Parent 為 SAME。證據在 `scratch/issue-153/review-probe.log` 與 `review-probe/ctor-overwrite.json`。
- 三批 staged、正式／候選參考 JAR 及完整 Maven JAR 的 SHA-256 全部與下表原收據相符。重跑修改前、預設、核准清單三組比較，結果仍為下表數字。重新產出 `docs/server-semantic-gap.md`，完整 JAR 結果為 2125 SAME／424 DIFF。
- `as-record/selftest.py` 本次重跑為 `SELFTEST PASS`。本次未重建伺服器 JAR，使用雜湊相符的既有固定產物重跑比較。下方 Maven 接線及 package 建置紀錄屬首輪證據。
- 推送 `a1d08e1` 後，GitHub 顯示 PR 與 main 衝突，CI 沒有啟動。本次在原分支合併 `origin/main`（`c3d5f6e`），手動整合唯一衝突 `tools/as-record/README.md`，保留 #134 Maven 說明與 #153 AsmFinal 用法。POM、其他測試與 `docs/REPORT.md` 直接沿用上游內容，本任務相對 main 的變更仍限核准範圍。

## 做了什麼

- lambda 使用描述子與正規化本體／metadata 識別並合併同內容副本；呼叫與 invokedynamic 參數同步映射。保留精確核准方法的原名，延續 #135 的授權邊界。
- 同類 private 實例方法的 bootstrap H5/H7 統一；外部方法、public、LDC handle 與直接呼叫不改。
- 新增 `AsmFinal.java`，共用 `AsRecord.transform`／CLI／#134 Maven 入口補 final 並移除註解；支援一般、繼承、抽象類別及 static 欄位，錯誤欄位整批拒絕。
- 預設驗證並前移 Object super 呼叫；非 Object 保持原狀。SwitchMap 宣告與引用採每類一致映射。try 邊界裁掉指定無拋例外指令，保留 handler、刪除空區間。
- 新增 `--access-widen`、`--deviations`、`--layout-methods`，初始化指定清單。CFG 比對包含指令、正常／有序例外後繼、不可達節點及全域一對一槽位關係；分列 SAME_LAYOUT、DEVIATION 及逐方法結果。
- effects 補上對側 DUP 與單次寫入／單次讀取的直線暫存配對。讀兩次維持 DIFF。
- `ctor-prologue-order` 僅容許指定方向的自身欄位簡單賦值移動，不移動 `NativeDeInstanceNames.name` 或其他呼叫。
- 新增 deviations 說明及 A-land 單元測試要求；追加七個 #66 effects 條目、同步 #76 已核准的 forMap 條目。所有完整描述子由固定 JAR 查核。
- 更新 `docs/server-semantic-gap.md`。未改 `docs/REPORT.md`、其他伺服器 src/main、POM、機密設定、封存、硬連結、正式資源或原專案檔案。

## 已驗證

### 自測與建置

Windows／Microsoft JDK 17.0.17／ASM 9.7／Python 3.12／Maven 3.9.16。一般命令沙箱初始化失敗，改用已核准的提升執行；寫入仍限本 worktree。沒有啟動遊戲、登入器、伺服器或連正式 DB。

| 驗證 | 實測 |
|---|---|
| 原有 NormalizationSelfTest | `ALL PASS checks=517` |
| 新增 AdvancedSelfTest | 第 2 次修正後 `ALL PASS checks=67`，已接進 selftest.sh；涵蓋跨 super 取值依賴、保留非 this 堆疊值，另拒絕未支援的舊式 JSR／RET CFG |
| as-record/selftest.py | `SELFTEST PASS`；含新增 AsmFinal JVM／正反例及原有 record 回歸 |
| asm-final-maven-selftest.py | `BUILD SUCCESS`、`ASM FINAL MAVEN SELFTEST PASS` |
| 伺服器 `mvn -o -B -DskipTests package` | `BUILD SUCCESS`，含 testCompile，未執行伺服器測試 |
| git diff --check | 通過 |

新增正反例包含內容相同／不同與換目標 lambda、H5/H7 外部／public 反例、Object／非 Object super、非法 this、計算前綴、SwitchMap 使用對調、未列入與反向存取、左側額外方法及同名內容不同、try 多包 INVOKE／含 monitor 的 RETURN、空 try、CFG 重排／例外型別／不同指令／不可達區塊、DUP 暫存讀一次／兩次、建構子移動方向／額外呼叫／Name 呼叫位置、清單 typo／範圍外／萬用字元。

原 catch fixture 只保護 NOP，依新規則應刪除，所以改為保護一條可能拋例外的呼叫，繼續驗證原本 catch 型別／數量規則；另加空 try 刪除案例。ASM fixture 是比較器結構測試，不宣稱所有負向 bytecode 都可由 JVM 執行。

首輪 Maven fixture 實際跑 process-classes → package，使用 #134 相同的 `AsRecord.main`，核對 JAR 沒有 ASM／converter／com.lineage entry，再執行 `-Xverify:all` 檢查 final、父類、預設值與冪等性。首輪使用隔離接線，第 2 次修正整合 main 後已帶入 #134 正式 POM 接線。

### 固定 staged 三批對照

「修改前」為本分支起點的 #135 比較器；兩輪使用完全相同的輸入 JAR。最後一欄是新版加本批精確核准清單；所有輪次 ONLY_LEFT／ONLY_RIGHT = 0，missingLeft／extraLeft 皆空。

| 固定來源 | entries | 修改前 SAME／DIFF | 新版預設 SAME／DIFF | 新版核准 SAME／SAME_EFFECTS／DIFF | 本任務減少 DIFF |
|---|---:|---:|---:|---:|---:|
| #66 `be0b284` B05 | 120 | 111／9 | 115／5 | 115／1／4 | 5 |
| #72 `6f8a87b` A01 | 185 | 159／26 | 159／26 | 159／0／26 | 0 |
| #76 `e62d213` A05 | 160 | 157／3 | 158／2 | 158／0／2 | 1 |

這三批的類別級 SAME_LAYOUT／DEVIATION 皆為 0，原因是核准方法或同類其他方法仍有差異。各指定來源報告的舊工具計數分別是 10、48、4 DIFF；與本輪最終相比減少 6、22、2，但這包含 #132／#135 改善，不能全歸因本任務。

| 輸入 | SHA-256 |
|---|---|
| #66 留存 batch.jar，符合 `be0b284` 報告收據 | `79e9462b75590bb9cdb8767ebf333e69a0c8ff95bed0b1239edb11d97d753374` |
| #72 從 `6f8a87b` 的 49 份 staged 重編本輪 JAR | `ce93b4824175550f895cab6ebff487562f5cbfc4aeae4a08869bb4fc42e852af` |
| #76 留存 a05.jar，符合 `e62d213` 報告收據 | `3af155bc5c65e2413862fc1ceb1db598f9aa704fe91297c788288e6e0570e091` |
| 正式 base-0.6.71.jar | `e1498871c7be6b287821c57ef5d95933db1740d9b4d4a80f6c3465f21d99d498` |
| 候選 candidate-50635f7e.jar | `50635f7edca8ec6ce77787cc32371c7590b497e48ea202f8a679caf3f517b729` |

#72 原 worktree 的 JAR 已被後續輪次覆寫，雜湊不符指定提交，因此沒有把它當指定輸入。改用 `git show 6f8a87b:<staged路徑>` 提取到本 worktree scratch、以既有 compile-stubs 作編譯依賴，單次 javac 全批；輸出正好 185 entries。此重建 JAR 的封裝雜湊不冒稱等於歷史 JAR，但前後比較共用同一份重建產物。

### #76 的三個具體殘差

1. **forMap**：staged 的 ASTORE 1 後有兩次 ALOAD 1，分別供 IFNULL 與 ARETURN。題目第 11 項明定只讀一次，反例也要求讀兩次必須 DIFF，因此保留 `effects sequence[1]: left=ASTORE right=ARETURN`。後續依主控裁定移交 #166。
2. **Main.accept**：try 範圍已相同，但最後 handler 左側重用槽 9，右側用槽 10。前面的 socket 與 Exception handler 兩側都用槽 9。#135 的一對一槽位規則保留重用關係，因此這不是單純重編號；第 9 項未授權消除此差異。
3. **Main.main**：CFG 有 475／473 個有效節點。實際 staged 在 `System.out` 與 `SourceLogin.start` 之間有 ASTORE 17，呼叫後有 ALOAD 17；正式版保留在 operand stack，沒有這兩條。第 10 項只允許區塊排列不同，不能忽略不同指令。僅在診斷記憶體中移除這一對後，CFG 同構通過；未輸出修改 class／JAR，也未將該診斷列入驗收 SAME_LAYOUT。這確認 cleanup 排列本身已可比較。

`NativeCastTimeline`、`NativeAcceptedState` 本輪均 SAME。不能為了得到 0 DIFF 而放寬未核准的槽位別名、堆疊暫存或讀取次數。

### #66、#72 剩餘原因

#66 剩四類：ProcScalarSkills.expiry 同樣是兩次讀取的暫存；RawDamage.Member 還需要來源端採用 @AsmFinal、恢復候選建構子形狀，本任務沒有改 staged；WindTargets.run 多出 ASTORE；StaticNpcScene 仍有 ASTORE、呼叫與 MONITOREXIT 序列差異。SignedRecovery.member 為 SAME_EFFECTS；lambda 複用／前綴與 private H5/H7 的差異已消除。

#72 固定的是較早 `6f8a87b`，不是後續修復提交。get_showId 的存取差異通過後，同類還有 sourceDeleteNpc 差異，因此類別 DIFF 數未減。五個 InstanceState 建構子逐方法仍 DIFF：最基本版本已跨過 construction 前置賦值，但仍有 `_isBackgroundMode` 的 field owner 不同；其餘四個的 Name 呼叫順序仍不同。SwitchMap 名稱以外的 enum ordinal／switch 目標差異也保留。完整 26 類列表與逐方法結果在本機 JSON／Markdown。

### 完整 JAR 與快照

本輪 Maven JAR SHA-256：`e333d077499e5b5bc64a9b8c916250f17fdaac55001d61caeccbc0ddf02a48c4`。

| 工具 | SAME | DIFF | ONLY_LEFT | ONLY_RIGHT | BYTE_SAME |
|---|---:|---:|---:|---:|---:|
| 修改前 #135 | 2122 | 427 | 1382 | 5 | 1008 |
| 新版預設 | 2125 | 424 | 1382 | 5 | 1008 |

三類由 DIFF 變 SAME：NativeDeWorldCore$1、NativeDeWorldPlayerFilter、NativeDeWorldPlayerFilter$1；沒有反向退化。ONLY_LEFT 比 #135 報告多 1 是新增 AsmFinal 註解類別，並非漏比較。快照使用新版預設，不套用批次清單。

### CI 實際證據

- 第 2 次修正的整合提交 `ff50cca6df1eb87c931b3d155ea0bfd72a65897b` 已通過全部 5 項 job。Server tests 為 **6,036 tests／0 failures／0 errors／117 skipped**。[build](https://github.com/chaoren0418-arch/lito-launcher-server/actions/runs/41969851917)、[package](https://github.com/chaoren0418-arch/lito-launcher-server/actions/runs/41969851940)。
- 本次 Semantic gap job 實際 SUCCESS，日誌含 `NORMALIZATION SELFTEST ALL PASS checks=517`、`ADVANCED SELFTEST ALL PASS checks=67`、`SELFTEST ALL PASS`、`FALLBACK ALL PASS`，合併 #134 Maven 接線後的全新 CI JAR 比較仍為 2125 SAME／424 DIFF。原始日誌保存於 `scratch/issue-153/ci-retry-semantic.log`、`ci-retry-server-tests.log`。[Semantic gap job](https://github.com/chaoren0418-arch/lito-launcher-server/actions/runs/41969851917/job/127055288795)
- 以下為首輪歷史證據，程式修正以本次 `ff50cca` 驗證為準。最後報告補充提交僅更新本文件，可由 PR checks 查閱其執行結果。
- 初始程式提交 `e73fc58f6c631e4588be8815975544be7c4c24dc` 的 [build](https://github.com/chaoren0418-arch/lito-launcher-server/actions/runs/41951354158) 與 [package](https://github.com/chaoren0418-arch/lito-launcher-server/actions/runs/41951354253) 全部 5 項 job SUCCESS。Server tests 為 **6,033 tests／0 failures／0 errors／117 skipped**，已保存完整 log，沒有重跑失敗 job 來換取綠燈。
- 最後程式提交 `f8fe1210645b03268a3e2cb77d995eae1a9f1473` 的 [Semantic gap job](https://github.com/chaoren0418-arch/lito-launcher-server/actions/runs/41951737626/job/126997021398) 實際 SUCCESS，完整日誌含 `NORMALIZATION SELFTEST ALL PASS checks=517`、`ADVANCED SELFTEST ALL PASS checks=49`、`SELFTEST ALL PASS`、`FALLBACK ALL PASS`、2125 SAME／424 DIFF。不是只看 non-blocking 工作流總狀態。Server／Launcher 建置與 package 亦已 SUCCESS。
- 最後提交的整體狀態見 [PR #164 checks](https://github.com/chaoren0418-arch/lito-launcher-server/pull/164/checks)；報告補充提交不再修改程式。日誌在 `scratch/issue-153/ci-semantic-gap-final.log`、`ci-server-tests.log`。

## 推測

固定 #76 產物的剩餘差異可能有行為等價的來源表達或可另行核准的狹窄正規化，後續範圍由 #166 處理。SAME_EFFECTS 的核准摘要相符仍需搭配執行期驗證。雙槽部分覆寫與 WIDE 索引的新增 fixture 用於結構邊界測試，JVM 執行證據限於上述審查者的有效 int 案例。

## 待驗證／未完成

- #76 的 0 DIFF 由 #166 接續，本次維持核准邊界並保留三個具體殘差。
- 本機沒有 Bash；完整 shell 與冷啟動 fallback 已由上述 Linux CI 實跑通過。
- 本機伺服器 package 跳過測試執行；A-land 的 default debug 與建構子執行期測試尚未做。未啟動服務／遊戲、未連正式 DB。
- #134 正式 Maven 接線已隨 main 帶入，本次沒有額外編輯伺服器 POM。

## 重跑與產出路徑

```text
bash lito伺服器/LiTo381-Server/tools/semantic-diff/selftest.sh
bash lito伺服器/LiTo381-Server/tools/semantic-diff/selftest-fallback.sh
python lito伺服器/LiTo381-Server/tools/as-record/selftest.py
python lito伺服器/LiTo381-Server/tools/as-record/asm-final-maven-selftest.py --maven <mvn> --maven-repo <隔離快取> --offline
```

Windows 可用 javac 對 `semantic-diff/src/main/java`、`src/test/java` 全部 Java 編譯，classpath 為 ASM 9.7 asm／asm-tree，再直接執行 NormalizationSelfTest、AdvancedSelfTest，兩個參數分別為 scratch 報告目錄及 target fixture 目錄。

- 交付程式：兩個 tools 目錄與 `src/main/java/local/lito381/server/AsmFinal.java`。
- 交付清單與規格：`docs/combat-merge/{effects-approved,access-widen-approved,layout-approved,deviations}.txt`、`deviations.md`。
- 報告：本文件、`docs/server-semantic-gap.md`。
- 本機可重跑腳本與完整證據（不入 git）：`scratch/issue-153/verify.py`；`{66,72,76}-{before,after,approved}.{json,md,log}`；`inputs.json`、`full-inputs.json`；`selftest.log`、`advanced.log`、`as-record-selftest.log`、`asm-final-maven.log`、`server-package.log`。建置產物在各工具 target 與伺服器 target。
- 第 2 次修正重跑入口為 `scratch/issue-153/retry.py`，執行前先用 `verify.py test` 編譯工具與測試，再執行 AdvancedSelfTest。`retry.log` 保留輸入雜湊核對、有效 JVM probe 與三批／完整 JAR 比較結果。

## 給下一棒的提示

Claude 主控複審本次 P1 的槽位依賴保護與有效 JVM 負例，#166 執行器接續 #76 三項殘差。#66、#72、#76 執行器取得本工具後仍要對各自最終 staged 重跑，本報告驗證題目固定提交。A-land 再補 deviations.md 列出的執行期單元測試。使用者負責後續真機驗收。
