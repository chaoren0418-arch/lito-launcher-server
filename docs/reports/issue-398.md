# Issue #398 — T139：把 #31 的戰鬥回歸工具參數化（task/31）合併進 main

## 結論

`origin/task/31`（`314104b2`，8 個 commit、12 個檔案）已以 merge commit `4d77dc2c` 合併進 `task/398`（基底 `origin/main` `1cc1f7d3`），**無衝突**：merge base `1300366c` 之後，main 沒有改過這 12 個檔案中的任何一個，兩邊的功能都完整保留。另外在 `.github/workflows/build.yml` 新增離線 job `combat-regression-tools`，在 CI 跑 #31 的離線檢查。完整行為回歸**仍未執行**（需要隔離 SQL 設定，見下）。

## 做了什麼

1. `git merge --no-ff origin/task/31`：一次合併成功，沒有衝突。異動範圍跟 #31 原範圍一致：
   - `tools/combat-continuation/`：`full_regression.py`、`candidate_regression.py`、`affected_regressions.py`、`replay_composed_materials.py`、`reconcile_replay.py`、`regression_support.py`（新增）、`test_formal_protection.py`（新增）
   - `cancellation-battle-runtime/`：`replay_targeted.py`、`replay_player_cancellation.py`、`seal_validation.py`、`test_curse_clock_boundary.py`
   - `docs/reports/issue-31.md`（新增）
   - 沒有改 `src/main`、各層 `build.py` 或 `docs/REPORT.md`。
2. CI（`build.yml` 新 job `combat-regression-tools`，ubuntu、Python 3.12，不需要 DB、JAR 或 fixture）：
   - 用 `py_compile` 檢查 `regression_support.py`、`test_formal_protection.py` 與 9 支入口的語法。
   - 對 9 支入口執行 `--help`，確認都有 `--jar`／`--out`／`--config`／`--resources`；`reconcile_replay.py` 另外要有 `--manifest`。所有入口在 import 後第一行就建立 `Options()`，`--help` 在讀任何檔案之前就結束，所以 CI 不需要 fixture。
   - 在 `env -u JAVA_HOME` 下確認 `regression_support.java_tool('java')` 會解析成 PATH 上的 `java`，並執行 `java -version`；也確認 `adapt_driver` 會把舊的寫死路徑 `C:/…/jdk-17.0.17.10-hotspot/bin/java.exe` 換成 `java_tool('java')`。
   - `python -m unittest test_formal_protection`：#31 的 14 項正式檔保護離線測試，`pin` 全部經過 mock，不讀實際 JAR。
   - #31 報告提到的 `test_regression_support.py`、`test_seal_failure.py`、`test_replay_policy.py` **沒有進 git**（它們放在 #31 的 scratch），所以無法放進 CI。

## 各入口的輸入與 DB 需求（供 #341）

共同參數（`regression_support.Options`）：
- `--jar`：候選 JAR。省略時使用 `<stage>/build/build.json` 的歷史收據，並驗證 SHA。
- `--out`：隔離輸出根目錄。一旦指定就會啟用 `formal_protection`，也就是要求正式部署檔 `lito登入器/LiTo381-Launcher/app/server/LiTo381-SharedResources-candidate.jar` 與 `reference/base-0.6.71.jar` 都等於 `e1498871…`，並在執行前後量測。
- `--resources`：唯讀的 Server 目錄，提供未入 git 的 fixture。若不是目前 worktree，就必須同時給 `--out`。
- `--config`：隔離的 SQL 設定檔。省略時使用 `config/server.properties`。
- JDK：優先用 `JAVA_HOME/bin`，沒有設定時改用 PATH。

各入口都需要的未入 git fixture（位於 `--resources` 下，目前只在原專案 `D:\lineage開發\3-lito開發\lito伺服器\LiTo381-Server`，已確認存在）：
- `tools/combat-continuation/<stage>/build/build.json`（例如 `cancellation-battle-runtime`）
- `tools/combat-continuation/build/integration-regression/preparation.json`
- `tools/resource-database/build/`（舊子驅動與 fixture）
- `cancellation-battle-runtime/build/qualification-current.json`（seal 使用）
- 已進 git 的有 `cancellation-battle-runtime/build-repro/build.json` 與 `reference/base-0.6.71.jar`。

| 入口 | 主要輸入 | 會啟動 JVM probe | 需要 DB（透過 `--config`） |
|---|---|---|---|
| `cancellation-battle-runtime/replay_targeted.py` | 上述 build.json、各 stage 的 `test*.py`、resource-database fixture | 是 | **是**（probe 讀資源表） |
| `cancellation-battle-runtime/replay_player_cancellation.py` | 同上（cancellation-probability 3 組） | 是 | **是** |
| `replay_composed_materials.py <stage>` | build.json、組合素材 probe | 是 | **是** |
| `candidate_regression.py <stage>` | build.json、preparation.json；依序呼叫 `full_regression.py` → `affected_regressions.py`，失敗時呼叫 `reconcile_replay.py` | 是（透過子程序） | **是** |
| `full_regression.py` | `tools/resource-database/build/replay-*.py` 等 20 組 | 是 | **是** |
| `affected_regressions.py` | 前一步的 `qualification-current.json`（必須是 passed）、`de-native-item-weapon-clock-development` prototype | 是 | **是** |
| `cancellation-battle-runtime/test_curse_clock_boundary.py` | poison-publication／elemental-material 已編好的 classes、resource-database fixture；javac＋java | 是 | **是**（把 `--config` 傳給 probe） |
| `reconcile_replay.py --manifest` | `--out` 內失敗的 replay manifest（48 組、1,728 案收據） | 否 | 否（只核對雜湊與收據） |
| `cancellation-battle-runtime/seal_validation.py` | 前面各套件在 `--out` 內的收據、歷史 `qualification-current.json` | 否 | 否（只核對收據；前面的步驟需要 DB） |

建議 #341 的執行順序，以及要放在 worktree 執行的完整指令，見 `docs/reports/issue-31.md` 末段：targeted → player → composed → candidate（20＋18）→ curse-clock → seal。

## 已驗證

- 合併沒有衝突；merge base 之後 main 沒有改這 12 個檔案（`git diff --stat 1300366c origin/main -- <範圍>` 沒有輸出）。
- 本機用 `py_compile` 檢查 `regression_support.py`，語法正確（Python 3.12.10）；產生的 `__pycache__` 已經以非遞迴方式清掉。
- 靜態閱讀確認：9 支入口都是 import 後第一行就建立 `Options()`；`java_tool` 先找 `JAVA_HOME`，再找 PATH。
- 上述 fixture 在原專案路徑存在，在 git 裡不存在（`git ls-files` 確認）。原專案和 worktree 的 `scratch/t020c/isolated-test.properties` 都**不存在**。

## 推測

- `test_curse_clock_boundary.py`、`replay_composed_materials.py` 的 classpath 用 `;` 串接，只能在 Windows 執行；CI 的 Linux job 只做離線檢查，不受影響。
- `mvn -B test` 應該維持綠燈，因為這次沒有改任何 Java 或 Maven 設定；以 PR 的 CI 結果為準。

## 待驗證

- 新 CI job `combat-regression-tools` 與 `server-test`（`mvn -B test`）的結果：push 後看 PR CI。
- 所有需要 DB 的行為套件：仍然需要隔離 SQL 設定與只有 SELECT 權限的帳號（#31 遺留的前置），由 #341 處理。依本機規定，這次沒有在本機執行任何回歸、JVM 或 probe。

## 產出路徑

- 分支：`task/398`（merge commit `4d77dc2c`，加上 CI 與報告 commit）
- CI：`.github/workflows/build.yml` 的 job `combat-regression-tools`
- 報告：`docs/reports/issue-398.md`；合併進來的 `docs/reports/issue-31.md`

## 給下一棒的提示

1. **#341**：工具已在 main（本 PR 合併後）。先準備 `scratch/t020c/isolated-test.properties`（SELECT 專用帳號），再用 `--resources D:/lineage開發/3-lito開發/lito伺服器/LiTo381-Server --out <worktree 內 scratch> --config <隔離設定>` 依序執行。前 7 支需要 DB；reconcile 和 seal 只核對收據。
2. `--out` 模式會強制正式部署檔 SHA 等於 `e1498871…`（0.6.71）。如果正式版之後升級，要同步更新 `regression_support.CURRENT_FORMAL_SHA256` 與 `reference/`。
3. 若要把 #31 的其他 scratch 測試（`test_regression_support.py` 等）也放進 CI，需要另外立任務把它們提交進 repo。
