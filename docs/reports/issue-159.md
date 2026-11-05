# Issue #159 — T041 離房後計數斷言競態

## 做了什麼／結論

根因分類：**測試**。`ModernWorldService` 的 LEAVE 確認終止發布及房間票券釋放，但不保證準備 worker 已執行 finally 退役。`matchCount()` 計入仍受服務持有的 entry／reservation；取消不等於工作完成。保留產品實作，不修改任何 `src/main` 類別。

起始提交：`33f3f5c5570a4d8fc4633e5b62e5ff617a2401ef`，既有 worktree／分支 `task/159`，未 checkout 或建立分支。

## 已驗證

### 受控根因證明

新增 `ModernBattleWorldProtocolTest.leaveWaitsForActualRetirementBeforeAssertingNoMatches`。使用既有 `Scene(true)` 的 reader barrier，於 worker 仍被阻擋時，在 service gate 下以測試專用 CountDownLatch 子類替換該 entry 的 `terminationDone`。子類先執行真實 `countDown()`，再以另一個有五秒上限的 latch 卡住 worker；不替換 entry、engine、資料或計數。

此時失敗準備已發布 ABORTED，兩名玩家經真實 canonical LEAVE 都得到 DETACHED；`releaseDone=0`、`terminationDone=0`，但 `active=1`、`matchCount=1`。準備 worker 尚未走到 執行器 `afterExecute` 的 finally／`finishWorker`，所以 `entry.retired=false`。這證明 #135 原先只懷疑兩個完成 latch 的推測不夠精確：即使兩者完成，仍然存在 worker 退役窗口。

產品依據：`removeRetiredEntry` 要求 `!retained && retired && releaseDone==0 && (!terminal || terminationDone==0)`；`finishWorker` 才把 worker 標為 RETIRED 並重試移除。既有 `cancelledReaderWhichIgnoresInterruptStillConsumesCapacityUntilRealFinally` 也明確要求取消後仍保留容量，不能為了測試把計數提前歸零。

修正前先讓共用測試 helper 使用舊的立即 `assertEquals(0, service.matchCount())`。同一受控回歸的 Future 立刻失敗，完整例外鏈保留 expected 0／actual 1。修正後 helper 最多等三秒，每次以同步的 `matchCount()` 觀察實際移除；到期仍執行嚴格 `assertEquals(0, ...)`，中斷也會失敗。沒有放寬斷言、刪案或延長 sleep。

回歸測試確認：worker 被卡住時等待 Future 不會提早成功（有上限 `get`）；解除 barrier 後 Future 成功，active／matchCount 都為 0。finally 必定放行 barrier 並 join checker。這測試使用反射作排程注入，僅影響測試 fixture，未增加產品測試鉤子。

修正前 log 節錄（`scratch/issue-159/regression-before.log`，exit 1）：

```text
controlled retirement: releaseDone=0 terminationDone=0 active=1 matchCount=1
Unexpected exception type thrown, expected: <java.util.concurrent.TimeoutException> but was: <java.util.concurrent.ExecutionException>
Caused by: java.util.concurrent.ExecutionException: org.opentest4j.AssertionFailedError: expected: <0> but was: <1>
Caused by: org.opentest4j.AssertionFailedError: expected: <0> but was: <1>
at local.lito381.server.ModernWorldServiceTest.awaitNoMatches(ModernWorldServiceTest.java:92)
Tests run: 1, Failures: 1, Errors: 0, Skipped: 0
BUILD FAILURE
```

修正後 log 節錄（`scratch/issue-159/regression-after.log`，exit 0；含原先兩支問題測試類別）：

```text
Running local.lito381.server.ModernArmorProtectionServiceTest
Tests run: 5, Failures: 0, Errors: 0, Skipped: 0
Running local.lito381.server.ModernBattleWorldProtocolTest
controlled retirement: releaseDone=0 terminationDone=0 active=1 matchCount=1
Tests run: 7, Failures: 0, Errors: 0, Skipped: 0
Tests run: 12, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
```

### 同型斷言盤點

先搜尋全部測試來源 `matchCount()`，逐項檢查前置流程；以下是修改前行號。各項最終仍要求精確 0，只將「已離房」改為等待實際移除。JUnit／既有 JUnit peer 的共用 helper 位於 `ModernWorldServiceTest`；6 個 Native 獨立 probe 沿用原有 `NativeCanonicalServiceProbeMain.await`（四秒上限）及原 require，不引入 JUnit 相依。

| 測試檔（均位於 `src/test/java/local/lito381/server/`） | 原行號 | 處理 |
|---|---|---|
| ModernArmorProtectionServiceTest | 53、74 | 等待 worker 退役後歸零 |
| ModernBattleWorldProtocolTest | 77、97 | 同上；新增受控回歸 |
| ModernBattleCanonicalActionServiceTest | 64 | 同上 |
| ModernBattleLifecycleServiceTest | 42、59 | 同上 |
| ModernBattleWirePeer | 572、608、847 | 同上 |
| ModernGreaterHasteServiceTest | 52 | 同上 |
| ModernHasteNpcServiceTest | 27 | 同上 |
| ModernHasteServiceTest | 52、99 | 同上 |
| ModernPlayerHealingServiceTest | 65、204 | 同上 |
| ModernSlowProtocolTest | 56 | 同上 |
| ModernSlowServiceTest | 34、55 | 同上 |
| ModernVipWorldServiceTest | 179、187、198 | 同上 |
| ModernVipWorldTlsTest | 108 | 同上 |
| ModernWorldServicePetRaceTest | 175、239 | 同上 |
| ModernPlayerHealingRuntimeTestPeer | 31 | sessions 歸零後仍須等 worker |
| ModernVipTransportFaultTestPeer | 44 | 同上 |
| ModernVipWorldRuntimeTestPeer | 26 | 同上 |
| NativeActorSqlTlsPeer／NativeCatalogSqlTlsPeer | 各 171 | 等待後保留原 require |
| NativeActorTlsProbeMain | 85 | 同上 |
| NativeCanonicalSqlTlsPeer | 131 | 同上 |
| NativeCanonicalTlsProbeMain | 81 | 同上 |
| NativeCatalogTlsProbeMain | 83 | 同上 |

保留不改的斷言：入房前能力／容量拒絕（Armor、GreaterHaste、Haste、Healing、Slow、TimeAttack、Bot、Item、PetRace）、未驗證連線（PlayerHealingTls）、已 `awaitTermination` 的 fixture close，以及已等待 `isQuiescent`／`matchCount==0` 的 Bot、VIP cancellation、TimeAttack、PetRace、WorldSettlement。預期保留 1 的容量／終局保留測試也不放寬。

### 本機五輪完整 Server 測試

執行環境：Windows、Microsoft OpenJDK 17.0.17、Maven 3.9.16。Maven 快取由 task/135 的 scratch 唯讀複製到本 worktree；`MAVEN_OPTS` 的 `maven.repo.local` 與 `java.io.tmpdir`、Surefire `argLine`、TEMP／TMP 均指向本 worktree。未複製私密設定或外部遊戲資源；真實 MySQL 測試未設定 `LITO_MODERN_MYSQL_HOME`，保留既有 opt-in／資源守門。

首次完整預跑的 Surefire XML 為 6,034 案、0 failures／0 errors；PowerShell 收錄器因把 SLF4J stderr 警告視為終止錯誤，未保存完整 log 與 Maven 退出碼。保留 `server-preflight.log`，修正收錄器僅依 `$LASTEXITCODE` 判定後重新連跑五輪；該預跑不計入驗收五輪，測試來源沒有變更。

| 輪次 | 時間（2026-11-05，UTC+8） | Maven 耗時 | Tests run | Failures | Errors | Skipped | Exit |
|---|---|---|---:|---:|---:|---:|---:|
| 1 | 08:59:33–09:09:12 | 09:37 | 6034 | 0 | 0 | 110 | 0 |
| 2 | 09:09:12–09:19:25 | 10:11 | 6034 | 0 | 0 | 110 | 0 |
| 3 | 09:19:25–09:28:39 | 09:12 | 6034 | 0 | 0 | 110 | 0 |
| 4 | 09:28:39–09:37:31 | 08:50 | 6034 | 0 | 0 | 110 | 0 |
| 5 | 09:37:31–09:47:07 | 09:34 | 6034 | 0 | 0 | 110 | 0 |

五輪皆為 `BUILD SUCCESS`，由同一腳本依序執行；每輪比 #135 的 6033 案多一個受控回歸。本機 skipped 為 110，先前 CI 為 117；未修改任何資源守門或 skip 規則，不將環境差異寫成額外功能驗收。

第 4 輪執行期間，靜態複查將 6 個不由 Surefire 執行的 Native main 入口改為沿用原有 await，避免引入 JUnit runtime 相依；JUnit 案例與受控回歸在五輪間皆未改動。第 5 輪重新 testCompile 全部入口；另外以 JDK 17 對 6 個 Native probe 執行獨立 javac，classpath 僅含 target/classes 與 target/test-classes、不含 JUnit JAR，exit 0（`scratch/issue-159/native-probe-compile-result.txt`）。Native 外部整合仍列為未驗證。

| 證據檔（scratch/issue-159/） | SHA-256 |
|---|---|
| regression-before.log | `0e3626a3016b2bfbbdf091c65f3f170e29e00b154434cdc46651b7f590c7df6b` |
| regression-after.log | `1dff91e99f639d82b4bfe48b2d1992d4e2e46d98f80c27db5dccc2c604aa18ac` |
| server-round-1.log | `462cdcf29cc020971cf4cf48f9e07fe9701297d6686fe5db7cc1c127683fb764` |
| server-round-2.log | `0fc868ddd3bc559d6e3e35d753dfdc946adf6fa29ac719cf14b8056e4ff43643` |
| server-round-3.log | `ac947f780dd191d1b533b3c5c4aac416c58e11374ec32f24aa6227f14ead9ac1` |
| server-round-4.log | `248cc6d91a063f285dfe3673f266c10ef3150955840da5cb782381b09d6a9b47` |
| server-round-5.log | `5539436bef179477c9d2c38466a3095ba85bc41189d76dc01c6ba588e066a747` |

`git diff --check` 通過；本次變更只包含 24 支測試／peer 原始碼與此報告，沒有建置產物入 git。

## 推測

受控測試證明上述時序可以穩定產生 #135 相同的 0／1 失敗，並證明修正後等待能正確跨過該窗口；#135 過去兩次 CI 沒有執行緒追蹤，無法宣稱還原當時每一步排程。

## 待驗證／未驗證什麼

- 本機指定五輪已完成；未補 GitHub Actions 證據。
- GitHub Actions 依任務背景受 #158 帳務上限阻擋，本次以本機驗收為準；未變更帳務、工作流或其他批次。
- runtime peer／Native TLS probe 的外部用戶端及 SQL 整合入口僅編譯，未啟動其外部整合驗收；六個 Native 入口另有不含 JUnit JAR 的獨立編譯證據。
- 未啟動遊戲、正式伺服器或 DB，未修改正式 JAR、`merge-inputs/`、`config/server.properties`、`docs/REPORT.md`、封存或原始專案檔案（git worktree 提交所需中繼資料除外）。Maven 的 loopback fixture 不代表真機遊戲驗收。

## 產出路徑／重跑方式／給下一棒的提示

- 報告：`docs/reports/issue-159.md`。
- 程式變更僅在 Server `src/test/java/local/lito381/server/`，包含 23 個同型斷言檔與共用 helper 所在的 `ModernWorldServiceTest`。
- 本機完整證據：`scratch/issue-159/regression-before.log`、`regression-after.log`、`server-round-1.log` 至 `server-round-5.log`、`runs.txt`。scratch／target 不入 git；前後失敗鏈與五輪摘要保留在本報告供遠端審查。
- 在 Server 目錄執行 `mvn -B test`；任務重跑腳本 `scratch/issue-159/run-tests.ps1 -Label server-round -Rounds 5` 額外把快取／暫存導向工作樹。單獨受控回歸使用 `-Dtest=ModernBattleWorldProtocolTest#leaveWaitsForActualRetirementBeforeAssertingNoMatches`。
- 重建紅燈證據：保留回歸測試，僅把 `awaitNoMatches` 方法體暫時換回 `assertEquals(0, service.matchCount());`，執行上述單案，應出現本報告的 expected 0／actual 1；恢復 helper 後同案通過。無須修改產品程式或切分支。
- 下一棒由 Claude 審查測試同步語意；調度器於合併後更新 REPORT 索引，待 #158 解決後補 CI 證據。使用者不需操作 DB 或提供本機資源。
