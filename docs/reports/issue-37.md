# Issue #37 / T012 — 修不穩定測試 `productionWorldCompanionClearsProvenUnreachableTargetOnTwentiethRetry`

## 做了什麼

PR #33 CI `Server tests`（run `41827854289`）只失敗 1 案：

`ClientSessionClassicWorldMonsterAggressionIntegrationTest.productionWorldCompanionClearsProvenUnreachableTargetOnTwentiethRetry:183 expected: <1> but was: <2>`

未刪測試、未 `@Disabled`、未 assume 跳過、未放寬既有斷言、未加重試註解。只改該測試的時鐘隔離；產品程式未改。

### 根因（已驗證：程式路徑；推測：CI 當下交錯時刻）

該案用**注入時鐘**呼叫正式路徑 `ClientSession.serverClassicWorldCompanionTick(mapId, nowNanos)`，從 `nowNanos = 1_000_000_000L`（1e9）起算，每步加 `CROWD_RETRY_MILLIS`（600ms），並斷言每次 `pathFailureCount()` 剛好等於迴圈序號。

同時間 `GameHub` 建構子把 `companionTickSafely` 掛在 `battleClock` 上，**每 50ms** 再用 `System.nanoTime()` 對已進 WORLD 的 session 呼叫同一個 tick：

```1271:1272:lito伺服器/LiTo381-Server/src/main/java/local/lito381/server/GameHub.java
        battleClock.scheduleWithFixedDelay(this::companionTickSafely,
                50, 50, TimeUnit.MILLISECONDS);
```

`selectTarget` 會把 `nextActionNanos` 重設為 0，companion 對**任何** `nowNanos >= 0` 都 `ready()`。測試把 companion 放進 `tamedCompanions` 之後，正式排程與測試執行緒搶同一隻給-up 計數器：

1. 測試第一次 tick（`nowNanos = 1e9`）→ `retryPath` 讓 `pathFailCount = 1`，並把 `nextActionNanos` 設成 `1.6e9`。
2. 若 50ms 排程在斷言前再 tick 一次，其 `nowNanos = System.nanoTime()`。GitHub-hosted runner／長時間 JVM 上這個值幾乎都遠大於 `1.6e9`，於是 `ready()` 仍為 true，`pathFailCount` 再加到 **2**。
3. 第 183 行 `assertEquals(attempt, companion.pathFailureCount())` 在 `attempt == 1` 時看到 2。這就是 CI 報告的數字。

同檔其他 WORLD companion 案（毒、麻痺）已用 `companion.defer(System.nanoTime(), 60_000)` 把正式排程推到 60 秒之後，所以不會踩這個窗。失敗案沒有 defer，又用固定 1e9，等於把測試時鐘放在牆鐘**後面**。

**推測（未在 CI 重放該次交錯）**：另一條次要路徑是 `ready()` 與 `retryPath()` 中間沒有佔住 companion——測試執行緒與 `battleClock` 可同時通過 `ready()`，然後各加一次。正式環境只有單執行緒 `battleClock` 呼叫此 tick，這條競態主要是測試與排程並存時才會出現。兩條路徑的可觀察結果都是「第一次斷言看到 2」。

產品語意本身（20 次 proven-unreachable 才清目標）沒有錯；`BattleCompanionPathRetryTest.twentiethProvenUnreachableAttemptClearsCompanionTarget` 已在沒有 `GameHub` 排程的情況下鎖住同一契約。不穩定的是整合測試把注入時鐘與正式 50ms 牆鐘疊在同一隻 companion 上。

### 修法

在放 companion、開始注入 tick **之前**，對該 fixture 的 `GameHub.battleClock` `shutdownNow()` 並 `awaitTermination(3s)`。之後只有測試呼叫的 `serverClassicWorldCompanionTick(..., nowNanos)` 會推進 `pathFailureCount`。加入 companion 後先斷言計數為 0，證明排程已停。

這是「注入時鐘」：正式 tick 方法仍跑，時戳由測試提供，不再跟牆鐘 50ms 迴圈競態。沒有 sleep、沒有放寬 `attempt == pathFailureCount`。

## 怎麼驗證

### 已驗證

- 環境：Ubuntu / OpenJDK 21.0.12.1 / Apache Maven 3.8.7。無 DB、無本機 `D:\` 資源、未啟停正式程序。
- 單案：

```bash
mvn -B -Dtest=ClientSessionClassicWorldMonsterAggressionIntegrationTest#productionWorldCompanionClearsProvenUnreachableTargetOnTwentiethRetry -Dsurefire.rerunFailingTestsCount=0 test
```

結果：**1 run / 0 failures / 0 errors / 0 skipped**（Surefire 0.702 s；含首次編譯約 40 s）。log 出現 `reason=proven-unreachable-20`。

- 全類 50 次連跑（編譯一次後只跑 Surefire）：

```bash
# 於 lito伺服器/LiTo381-Server
mvn -B -Dtest=ClientSessionClassicWorldMonsterAggressionIntegrationTest -Dsurefire.rerunFailingTestsCount=0 test
for i in $(seq 1 50); do
  mvn -B -Dtest=ClientSessionClassicWorldMonsterAggressionIntegrationTest \
      -Dsurefire.rerunFailingTestsCount=0 surefire:test
done
```

實際統計（本輪）：**50 / 50 BUILD SUCCESS**；每次 **Tests run: 19, Failures: 0, Errors: 0, Skipped: 0**；總牆鐘 **831 s**（約 16.6 s／次）。逐次 log 在 `target/issue-37-loop/run-1.log` … `run-50.log`（不進 git）。未改其他 18 案的斷言。

### 推測

- CI 當次失敗是「第一次注入 tick 之後、斷言之前」插入一次 `companionTickSafely`。與原始碼及 `expected: <1> but was: <2>` 吻合，但本輪沒有把未修版本在 CI runner 上重放到失敗。

### 待驗證

- 完整 CI `Server tests`（約 6033 run）是否全綠。本輪只連跑本測試類。
- CI 使用 Temurin 17；本輪連跑是 OpenJDK 21 編譯 `release 17`。語意應相同，仍以 GitHub job 為準。
- 未在本機重現「未修版本必現 2」；50ms 窗很窄，與「同 PR 前一個 commit 綠、下一個紅」一致。

## 未驗證什麼

- 真機進房／寵物 AI／正式伺服器排程。
- 產品 `retryPath` 在多執行緒同時 tick 時是否也該原子佔位（正式路徑目前只有 `battleClock` 單執行緒呼叫，不在本任務範圍）。
- 其他仍一邊注入 `nowNanos`、一邊讓 `battleClock` 跑的 WORLD companion 測試（毒／麻痺案用 60s `defer`，本輪不斷言它們）。

## 產出路徑

- `lito伺服器/LiTo381-Server/src/test/java/local/lito381/server/ClientSessionClassicWorldMonsterAggressionIntegrationTest.java`
- `docs/reports/issue-37.md`
- 分支：`task/37`

未改 `docs/REPORT.md`、未碰 `config/server.properties`、封存、releases、`LiTo.bin`。

## 給下一棒的提示

- 請 Claude 審此 PR：確認沒有放寬斷言、沒有跳過測試，且 `stopBattleClock` 發生在 `tamedCompanions.add` 之前。
- 合併條件：GitHub `Server tests` 全綠。
- 若還要從產品側杜絕「測試執行緒 + battleClock」雙重 tick，下一步才是讓 `serverClassicWorldCompanionTick` 在 `ready()` 到 `retryPath()` 之間原子佔位；本輪不需要。
- 之後寫同類「注入 nowNanos 的 WORLD companion 計數」測試，請先停 `battleClock`，或像毒案那樣 `defer` 到牆鐘之後再 `add`。
