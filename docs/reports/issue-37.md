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

在放 companion、開始注入 tick **之前**，對該 fixture 的 `GameHub.battleClock` `shutdownNow()` 並 `awaitTermination(3s)`。之後只有測試呼叫的 `serverClassicWorldCompanionTick(..., nowNanos)` 會推進 `pathFailureCount`。成功 awaitTermination 證明排程已停；加入 companion 後另斷言初始計數為 0。

這是「注入時鐘」：正式 tick 方法仍跑，時戳由測試提供，不再跟牆鐘 50ms 迴圈競態。沒有 sleep、沒有放寬 `attempt == pathFailureCount`。

### 第 3 次嘗試：範圍整理

- 保留前次已審查的測試修正，未再修改 Java 檔案或其他測試斷言。
- 移除 `.github/workflows/issue-37-loop.yml` 與 `docs/reports/issue-37-loop.sh`，採證腳本改以受測 commit 的固定連結保留。
- 保留原 artifact 連結、受測 SHA、50 列統計與根因說明；不要求重跑已核實的 50 次。
- 本機 worktree 起於較新的 main 基底，與遠端 task/37 分歧；整合既有 task/37 歷史後提交，不重建分支或強制推送。

## 怎麼驗證

### 已驗證

- 環境：Ubuntu / OpenJDK 21.0.12.1 / Apache Maven 3.8.7。無 DB、無本機 `D:\` 資源、未啟停正式程序。
- 單案：

```bash
mvn -B -Dtest=ClientSessionClassicWorldMonsterAggressionIntegrationTest#productionWorldCompanionClearsProvenUnreachableTargetOnTwentiethRetry -Dsurefire.rerunFailingTestsCount=0 test
```

結果：**1 run / 0 failures / 0 errors / 0 skipped**（Surefire 0.702 s；含首次編譯約 40 s）。log 出現 `reason=proven-unreachable-20`。

- 全類 50 次連跑（審查退回後重跑；先前 `target/issue-37-loop/` 在 ephemeral 工作區、未進 git，無法核對）。

實際採證指令（會編譯一次、連跑 50 輪、**每輪獨立 log**、記錄 **exit code**、解析該類 `Tests run / Failures / Errors / Skipped`，任一輪失敗則腳本 exit 1；log 不進 git）：

歷史採證使用的完整腳本與 workflow 已固定在受測版本，可由下列永久連結核對：

- [採證腳本（218d05b）](https://github.com/chaoren0418-arch/lito-launcher-server/blob/218d05b5d3df64a2440004a4fa7f81d6fab4a522/docs/reports/issue-37-loop.sh)
- [採證 workflow（218d05b）](https://github.com/chaoren0418-arch/lito-launcher-server/blob/218d05b5d3df64a2440004a4fa7f81d6fab4a522/.github/workflows/issue-37-loop.yml)

當時 workflow 在上述 commit 的 repo 根目錄執行 `bash docs/reports/issue-37-loop.sh`。這是歷史命令；本輪已移除這兩個越界檔案。若日後要在目前 checkout 重跑，可把歷史腳本取到未追蹤的 `scratch/issue-37/`，保持其向上兩層定位 repo 的行為：

```bash
# repo 根目錄；Git Bash / Linux，需 Java、Maven 與 python3
mkdir -p scratch/issue-37
git show 218d05b5d3df64a2440004a4fa7f81d6fab4a522:docs/reports/issue-37-loop.sh > scratch/issue-37/loop.sh
bash scratch/issue-37/loop.sh
```

此命令測試目前 checkout，腳本會記錄實際 HEAD；不能把新跑的結果當成歷史 SHA 的證據。一次性腳本留在 `scratch/`，不得強制加入 git。

腳本會把證據寫到 `lito伺服器/LiTo381-Server/target/issue-37-loop/`（gitignore）：

| 檔案 | 內容 |
|---|---|
| `tested-revision.txt` | 受測 commit SHA |
| `meta.txt` | 時間、java/mvn 版本、hostname |
| `compile.log` | `mvn -B -DskipTests compile test-compile` |
| `run-1.log` … `run-50.log` | 各輪 `mvn -B -Dtest=ClientSessionClassicWorldMonsterAggressionIntegrationTest -Dsurefire.rerunFailingTestsCount=0 surefire:test` 完整輸出 |
| `summary.tsv` / `summary.json` / `summary.md` | 每輪 exit_code、Tests run、Failures、Errors、Skipped、elapsed_s、PASS/FAIL |

上述歷史 workflow 已把採證目錄上傳為 artifact **`issue-37-loop-evidence`**。移除 workflow 與腳本不影響既有 artifact 連結；最終 PR 差異僅保留目標測試類與本報告。不把 `target/`、`*.log` 或 zip 加進 git。

**50 次連跑結果（已驗證，CI Temurin 17 / Maven 3.10.0）**

| 來源 | run | artifact | 受測 SHA | 結果 |
|---|---|---|---|---|
| PR #39 `pull_request` | [41836898186](https://github.com/chaoren0418-arch/lito-launcher-server/actions/runs/41836898186) | [`issue-37-loop-evidence` 13328845723](https://github.com/chaoren0418-arch/lito-launcher-server/actions/runs/41836898186/artifacts/13328845723) | `4a2265a`（Actions 把 `218d05b` merge 進當時 `main` 的 merge commit） | **50 / 50 PASS** |
| `push` `task/37` | [41836893478](https://github.com/chaoren0418-arch/lito-launcher-server/actions/runs/41836893478) | [`issue-37-loop-evidence` 13327854133](https://github.com/chaoren0418-arch/lito-launcher-server/actions/runs/41836893478/artifacts/13327854133) | `218d05b5d3df64a2440004a4fa7f81d6fab4a522`（分支 head，含腳本與測試修正） | **50 / 50 PASS** |

下載：

```bash
gh run download 41836898186 -R chaoren0418-arch/lito-launcher-server -n issue-37-loop-evidence -D scratch/issue-37/evidence-pr
# 或
gh run download 41836893478 -R chaoren0418-arch/lito-launcher-server -n issue-37-loop-evidence -D scratch/issue-37/evidence-push
```

兩份 artifact 的 `summary.json` 皆為 **pass=50 fail=0**；每輪 **exit_code=0**、**Tests run=19 / Failures=0 / Errors=0 / Skipped=0**。PR 那份 `summary.tsv` 牆鐘加總 **1154 s**（22–24 s／輪）。環境見 artifact 內 `meta.txt`（`Temurin-17.0.21+9`）。

PR merge commit 的逐輪表（與 artifact `summary.md` 相同）：

| run | exit_code | Tests run | Failures | Errors | Skipped | elapsed_s | status |
|---:|---:|---:|---:|---:|---:|---:|:---|
| 1 | 0 | 19 | 0 | 0 | 0 | 24 | PASS |
| 2 | 0 | 19 | 0 | 0 | 0 | 23 | PASS |
| 3 | 0 | 19 | 0 | 0 | 0 | 23 | PASS |
| 4 | 0 | 19 | 0 | 0 | 0 | 23 | PASS |
| 5 | 0 | 19 | 0 | 0 | 0 | 23 | PASS |
| 6 | 0 | 19 | 0 | 0 | 0 | 23 | PASS |
| 7 | 0 | 19 | 0 | 0 | 0 | 23 | PASS |
| 8 | 0 | 19 | 0 | 0 | 0 | 24 | PASS |
| 9 | 0 | 19 | 0 | 0 | 0 | 23 | PASS |
| 10 | 0 | 19 | 0 | 0 | 0 | 23 | PASS |
| 11 | 0 | 19 | 0 | 0 | 0 | 23 | PASS |
| 12 | 0 | 19 | 0 | 0 | 0 | 24 | PASS |
| 13 | 0 | 19 | 0 | 0 | 0 | 23 | PASS |
| 14 | 0 | 19 | 0 | 0 | 0 | 23 | PASS |
| 15 | 0 | 19 | 0 | 0 | 0 | 23 | PASS |
| 16 | 0 | 19 | 0 | 0 | 0 | 24 | PASS |
| 17 | 0 | 19 | 0 | 0 | 0 | 23 | PASS |
| 18 | 0 | 19 | 0 | 0 | 0 | 23 | PASS |
| 19 | 0 | 19 | 0 | 0 | 0 | 24 | PASS |
| 20 | 0 | 19 | 0 | 0 | 0 | 22 | PASS |
| 21 | 0 | 19 | 0 | 0 | 0 | 24 | PASS |
| 22 | 0 | 19 | 0 | 0 | 0 | 22 | PASS |
| 23 | 0 | 19 | 0 | 0 | 0 | 23 | PASS |
| 24 | 0 | 19 | 0 | 0 | 0 | 24 | PASS |
| 25 | 0 | 19 | 0 | 0 | 0 | 23 | PASS |
| 26 | 0 | 19 | 0 | 0 | 0 | 23 | PASS |
| 27 | 0 | 19 | 0 | 0 | 0 | 23 | PASS |
| 28 | 0 | 19 | 0 | 0 | 0 | 23 | PASS |
| 29 | 0 | 19 | 0 | 0 | 0 | 23 | PASS |
| 30 | 0 | 19 | 0 | 0 | 0 | 23 | PASS |
| 31 | 0 | 19 | 0 | 0 | 0 | 23 | PASS |
| 32 | 0 | 19 | 0 | 0 | 0 | 23 | PASS |
| 33 | 0 | 19 | 0 | 0 | 0 | 23 | PASS |
| 34 | 0 | 19 | 0 | 0 | 0 | 23 | PASS |
| 35 | 0 | 19 | 0 | 0 | 0 | 22 | PASS |
| 36 | 0 | 19 | 0 | 0 | 0 | 23 | PASS |
| 37 | 0 | 19 | 0 | 0 | 0 | 23 | PASS |
| 38 | 0 | 19 | 0 | 0 | 0 | 23 | PASS |
| 39 | 0 | 19 | 0 | 0 | 0 | 23 | PASS |
| 40 | 0 | 19 | 0 | 0 | 0 | 23 | PASS |
| 41 | 0 | 19 | 0 | 0 | 0 | 23 | PASS |
| 42 | 0 | 19 | 0 | 0 | 0 | 23 | PASS |
| 43 | 0 | 19 | 0 | 0 | 0 | 23 | PASS |
| 44 | 0 | 19 | 0 | 0 | 0 | 24 | PASS |
| 45 | 0 | 19 | 0 | 0 | 0 | 22 | PASS |
| 46 | 0 | 19 | 0 | 0 | 0 | 23 | PASS |
| 47 | 0 | 19 | 0 | 0 | 0 | 24 | PASS |
| 48 | 0 | 19 | 0 | 0 | 0 | 23 | PASS |
| 49 | 0 | 19 | 0 | 0 | 0 | 23 | PASS |
| 50 | 0 | 19 | 0 | 0 | 0 | 22 | PASS |

- GitHub CI [run 41836898182](https://github.com/chaoren0418-arch/lito-launcher-server/actions/runs/41836898182)（PR #39，head `218d05b`）：**Server tests** pass（10m32s）；Server package 與 Launcher 亦綠。前一筆測試修正 [run 41833234796](https://github.com/chaoren0418-arch/lito-launcher-server/actions/runs/41833234796)（`b92cf51`）同樣全綠：**6033 run / 0 failure / 117 skipped**，目標類 19/0/0/0（17.11 s）；artifact `server-test-evidence` 13325365446。

### 推測

- CI 當次失敗是「第一次注入 tick 之後、斷言之前」插入一次 `companionTickSafely`。與原始碼及 `expected: <1> but was: <2>` 吻合，但本輪沒有把未修版本在 CI runner 上重放到失敗。
- 未在本機重現「未修版本必現 2」；50ms 窗很窄，與「同 PR 前一個 commit 綠、下一個紅」一致。

### 待驗證

- 真機進房／寵物 AI／正式伺服器排程（本任務不要求）。

## 未驗證什麼

- 真機進房／寵物 AI／正式伺服器排程。
- 產品 `retryPath` 在多執行緒同時 tick 時是否也該原子佔位（正式路徑目前只有 `battleClock` 單執行緒呼叫，不在本任務範圍）。
- 其他仍一邊注入 `nowNanos`、一邊讓 `battleClock` 跑的 WORLD companion 測試（毒／麻痺案用 60s `defer`，本輪不斷言它們）。
- 未把未修版本在 CI runner 上重放到 `expected: <1> but was: <2>`。

## 產出路徑

- `lito伺服器/LiTo381-Server/src/test/java/local/lito381/server/ClientSessionClassicWorldMonsterAggressionIntegrationTest.java`
- `docs/reports/issue-37.md`
- 分支：`task/37`

未改 `docs/REPORT.md`、未碰 `config/server.properties`、封存、releases、`LiTo.bin`。

## 給下一棒的提示

- 請核對 artifact `issue-37-loop-evidence`（PR run 41836898186 / 13328845723，或 push run 41836893478 / 13327854133）：`tested-revision.txt`、`summary.tsv` 50 列 exit_code=0、各 `run-N.log` 的 Surefire 行 19/0/0/0。
- 本輪已從最終差異移除越界 workflow 與腳本；採證內容請查看上方受測 commit 的永久連結，需要重跑時取到未追蹤的 `scratch/`。
- 請 Claude 審此 PR：確認沒有放寬斷言、沒有跳過測試，且 `stopBattleClock` 發生在 `tamedCompanions.add` 之前。
- 既有 50 次連跑證據已經前次審查逐輪核實；依退回回饋，本次範圍整理不重跑 50 次。最新提交仍須通過既有 build workflow 的 Server tests；重送審查時以 PR #39 最新 head 的檢查為準，不以舊 run 替代。
- 若還要從產品側杜絕「測試執行緒 + battleClock」雙重 tick，下一步才是讓 `serverClassicWorldCompanionTick` 在 `ready()` 到 `retryPath()` 之間原子佔位；本輪不需要。
- 之後寫同類「注入 nowNanos 的 WORLD companion 計數」測試，請先停 `battleClock`，或像毒案那樣 `defer` 到牆鐘之後再 `add`。
