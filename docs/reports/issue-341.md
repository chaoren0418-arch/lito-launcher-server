# Issue #341：M2 離線戰鬥回歸

## 結論

**阻塞：步驟 1 Maven 建置成功；步驟 2 在啟動回歸前確認既有入口不符合本輪無 DB 限制，因此停止。沒有執行任何行為案例，也沒有逐案比較結果。**

本報告為 2026-11-13 使用者核准後，在原 Codex 對話續跑的結果，覆蓋[舊阻塞結論](https://github.com/chaoren0418-arch/lito-launcher-server/issues/341#issuecomment-6852058817)。舊 git fetch 已在核准後成功；WMI 查詢拒絕只保留紀錄，未再次查詢或修改權限。此輪不是第二個並行執行者。

## 已驗證

- 電腦：`DESKTOP-CV2L01A`；repo：`chaoren0418-arch/lito-launcher-server`。
- 續跑時聊天清單僅本對話執行 #341，無既有 task/341 分支或 worktree。
- 基底為此次已 fetch 的 `origin/main`：`42808209aef907f5590b7a9800a96bf6aef319a7`。
- 獨立分支 `task/341`；worktree：`D:\lineage開發\3-lito開發\scratch\wt\task-341`。未改原 main 工作目錄。
- `mvn -B -DskipTests package` 在平台核准的同命令重試後 exit 0／BUILD SUCCESS，日誌記錄耗時 01:16。編譯主程式及測試來源；Surefire 顯示 `Tests are skipped.`，不等於測試通過。
- 新 JAR：`lito伺服器/LiTo381-Server/target/LiTo381-Server.jar`。
- 新 JAR SHA-256：`64f4e3ebc1776ade9523fbc92ef5ea5a8b47aeb68d2905015dac8661e469204e`。

### 步驟 2 的實際阻塞

1. 本次 main 不含 `tools/combat-continuation/regression_support.py`。其 `cancellation-battle-runtime/replay_targeted.py` 仍直接讀取 `build/build.json`、使用固定 `build/targeted` 輸出，沒有 #31 的 `Options`／`--jar`／`--out` 接線。
2. 唯讀核對既有 `scratch/wt/task-31`：該分支具有參數化入口；`regression_support.py:64–65` 明示 `--config` 為隔離設定且 probes require SQL resources。未將其複製、合併或修補到本任務。
3. 本次 main 的 `cancellation-battle-runtime/test.py:22` 把 `S/'config/server.properties'` 傳給 Java probe。`CancellationBattleReplacementProbe.java:70` 呼叫 `ServerConfig.load(...)` 與 `ResourceDatabase.load(base)`；故不能以未指定設定的入口實跑，否則會越過本輪禁止讀正式設定及連任何 DB 的界線。
4. #31 的[主控裁定](https://github.com/chaoren0418-arch/lito-launcher-server/issues/31#issuecomment-6636565720)也明確區分工具驗收與完整回歸，後者另外依賴隔離 SQL 前置。這是補充依據，不是本輪實跑結果。

因此停在步驟 2 的入口／依賴核對，未執行 targeted、player、composed 或 curse-clock；沒有嘗試連線、建立假設定、修工具或放寬斷言。

### 對照表

| 套件 | Maven 新 JAR | 候選 50635f7e… | 逐案相同／不同 |
|---|---|---|---|
| targeted＋player：2,400 案 | 未執行，0 案 | 未執行，0 案 | 待驗證 |
| composed：5,164 組合 | 未執行，0 組合 | 未執行，0 組合 | 待驗證 |
| curse-clock：24 案 | 未執行，0 案 | 未執行，0 案 | 待驗證 |

未產生差異案例，不能宣稱兩顆 JAR 相同，也不能對應 B09 方法或 #138 deviations。候選完整 SHA 本輪未量測，`50635f7e…` 僅為任務指定識別。

### 命令與證據

在 repo 根目錄建立 worktree：

```powershell
git worktree add -b task/341 'scratch/wt/task-341' origin/main
```

在 worktree 的 `lito伺服器/LiTo381-Server` 執行：

```powershell
$evidence = 'D:\lineage開發\3-lito開發\scratch\wt\task-341\scratch\issue-341'
New-Item -ItemType Directory -Path "$evidence\tmp" -Force | Out-Null
$env:MAVEN_OPTS = "$env:MAVEN_OPTS -Djava.io.tmpdir=$evidence\tmp"
mvn -B -DskipTests package *> "$evidence\maven-package.log"
```

首次 exit 1 原因如下；完整錯誤保留於 `scratch/issue-341/maven-package.log`：

```text
Plugin org.codehaus.mojo:build-helper-maven-plugin:3.5.0 or one of its dependencies could not be resolved:
The following artifacts could not be resolved: org.codehaus.mojo:build-helper-maven-plugin:pom:3.5.0 (absent): Could not transfer artifact org.codehaus.mojo:build-helper-maven-plugin:pom:3.5.0 from/to central (https://repo.maven.apache.org/maven2): Permission denied: getsockopt
```

平台要求的必要核准完成後，以相同 Maven 命令重試一次，輸出改存 `maven-package-approved.log`，exit 0。未修改依賴或系統設定；這次核准不能推定為全系統完全存取。

雜湊及靜態查核命令：

```powershell
Get-FileHash -Algorithm SHA256 -LiteralPath 'lito伺服器\LiTo381-Server\target\LiTo381-Server.jar'
Test-Path -LiteralPath 'lito伺服器\LiTo381-Server\tools\combat-continuation\regression_support.py'
rg -n 'config/server.properties|ResourceDatabase.load' 'lito伺服器/LiTo381-Server/tools/combat-continuation/cancellation-battle-runtime/test.py' 'lito伺服器/LiTo381-Server/tools/combat-continuation/cancellation-battle-runtime/CancellationBattleReplacementProbe.java'
```

必要證據僅放本 worktree 的 `scratch/issue-341/`，不進 git：

| 檔案 | SHA-256／用途 |
|---|---|
| `maven-package.log` | `ff46ce5fd209b22df29c0ef28c9429100d12a5b62ee1be8c94ea86356de969dc` |
| `maven-package-approved.log` | `5779bafe3f451b4595dc44d3d4f7466c211c2c26438087f4f2fc549762952f02` |
| `sha256.json` | JAR 與兩份建置日誌的完整路徑及雜湊 |
| `regression-preflight.txt` | main／task/31 入口指紋、缺件與 SQL 載入行證據 |

## 推測

本次 main 可能尚未納入 #31 的工具變更；沒有調查合併歷史或修復。即使提供參數化工具，也不能由此推定既有 SQL 相依的案例可在完全無 DB 下通過。

## 待驗證與給下一棒的提示

- 主控須先釐清「既有套件依賴 SQL」與本張「不連任何 DB」的衝突，提供符合既定界線的可執行入口與輸入；本張沒有授權修改工具，亦未自行擴張為資料庫工作。
- 工具與無 DB 前置未有新資訊前，不要重跑同一套件。Maven 本輪已成功，不必為同一阻塞重建。
- 待完成兩顆 JAR 各三套回歸、逐案比較、差異至 B09／#138 的對照；目前全部保持未驗證。
- Maven 程序已正常 exit 0；未啟動測試、DB、P24Lab、遊戲或伺服器程序，沒有需要清理的自啟測試服務。
- 未修改 src/tools、lock、正式設定、正式 BIN、既有程序、調度器或 docs/REPORT.md；未讀正式設定與 docs/REPORT.md，未操作原有封存及客戶端資源。
