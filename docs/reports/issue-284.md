# Issue #284：T014 當版 JAR 建置與實驗交接

## 結論

**部分完成，T014 尚未驗收，不得放行 #285。** 2026-11-13 本輪同步 main、建置成功，準備全新 run 目錄、JAR／lib、22 鍵無帳密範本及空 runtime。未連資料庫、未補結構、未啟動 JAR／P24Lab／登入器／遊戲，沒有本輪大廳 PNG。

本輪派工末段明列「不得在本機執行任何編譯出來的程式」「不開 GUI 視窗」「不可碰機密設定」，因此僅完成編譯、檔案與靜態檢查。已讀取[主控實驗授權留言](https://github.com/chaoren0418-arch/lito-launcher-server/issues/284#issuecomment-6855355943)，但它與目前對話的明文限制不一致；沒有以外部留言覆蓋目前指示。這次不是沙盒或工具核准失敗，也不是缺資源。

## 已驗證：本輪建置與準備

- 工作樹：`D:\lineage開發\3-lito開發\scratch\wt\task-284`；沿用 `task/284`，沒有 checkout、建分支或移除 worktree。
- 開始 HEAD：`5cdebed6813e95730eeca05f5487181873e2b386`。執行 `git fetch origin`、`git merge origin/main`，無衝突；同步後 HEAD：`d423632fd9acd7227e8ae5c6ed23e0dfe7def136`。
- main：`ad12ee9cf63922aaba455272bc5fe3a7bc3b9556`。伺服器目錄與 main 無差異；沒有修改產品程式。`docs/REPORT.md` 僅隨 merge 帶入 main 版本，未人工修改，相對 main 無差異。
- `/api/state` 核對 `p24lab` holder=`chaoren0418-arch/lito-launcher-server#284`、exp=`1794584424.3461888`，對應派工到期時間 **2026-11-13T15:40:24Z**。14:59:05Z 準備時仍有效。只查詢租約，未續租、未用它操作 DB。
- 指定建置命令：`mvn -f lito伺服器/LiTo381-Server/pom.xml -B -DskipTests package`。**22:58:24 +08:00 BUILD SUCCESS，exit 0，耗時 1:36**。有編譯測試來源，沒有執行測試；Maven 的 AsRecord 是既有建置轉換步驟。
- JVM 暫存導向本次 run 的 `tmp/`；完整建置輸出在 `R/maven-package.log`。PowerShell 保存的原始輸出部分中文路徑編碼失真，未改寫為偽造原始輸出。

本輪 R：`scratch/t014/run-20261113T145646Z/`。

**JAR SHA-256：`FF4EAD8B222E7682A09C1C55979753A943B10402320E36C3F27B8B0E383F7064`**，15,086,958 bytes，與前次同一 main 建置一致。

| 成品 | 本輪檢查 |
|---|---|
| `R/LiTo381-Server.jar` | 從本工作樹 target 複製，來源／副本 SHA 相同 |
| `R/lib/` | HikariCP 5.1.0、mysql-connector-j 8.0.33、protobuf-java 3.21.9、slf4j-api 1.7.36，逐檔大小／SHA 相符 |
| JAR manifest | Main-Class 為 `local.lito381.server.SharedResourceMain`；Class-Path 四個 lib 全在；未打包 compile-only `com/lineage/` 或 AsRecord 工具類 |
| `R/config/experiment.properties.template` | 22 鍵與 `src/main` 的 `UnifiedConfiguration.KEYS` 完全一致；帳密僅佔位符，**不可直接啟動** |
| `R/runtime/` | 全新且 0 項目，沒有執行可寫 journal 的程式 |

收據：[preparation-current.json](issue-284/preparation-current.json)、[jar-manifest.txt](issue-284/jar-manifest.txt)。靜態檢查透過 `scratch/t014/prepare_current.py` 讀來源／ZIP／公開設定，未執行 JAR。原本 dot-source `m4-common.ps1` 被本機 PowerShell 執行原則拒絕，改以 Python 讀檔比對；未改執行原則或系統設定。

## 設定（不含帳密）

完整 22 鍵見 [config-public.properties](issue-284/config-public.properties)。本輪範本與之相同，帳密欄改為明確待填佔位符。

- `bind=127.0.0.1`、`game.bind=127.0.0.2`、`port=42083`。
- `database.jdbc.base.url=jdbc:mysql://127.0.0.1:43324/`。
- `database.static.database=p24_380db_lito_exp`：沿用[主控 2026-11-07 裁定](https://github.com/chaoren0418-arch/lito-launcher-server/issues/284#issuecomment-6701189885)，不是原始派工中結構不相容的 `p24_world_lito_exp`。
- `database.lito.database=p24_lito_lito_exp`；預期路由 `p24_accounts_lito_exp`，本輪未連 DB 驗證。
- `capture.directory=../captures`、`logs.directory=../logs`，相對 `R/config/` 解析到 R 下全新目錄；啟動逾時 120 秒。
- 本輪未讀 #83 私密設定、未建立含帳密的 `experiment.properties`，也未碰正式 `config/server.properties`。

## 前次實機證據（本輪未重測）

以下檔案承接自開始 HEAD `5cdebed6`，對應前次 `scratch/t014/run-20261113T133857Z/`，**不代表本輪執行**。

| 項目 | 前次紀錄 | 本輪 |
|---|---|---|
| `p24_accounts_lito_exp.character_lito` | 啟動前 2／後 2 | 未查詢，不能填成本輪筆數 |
| `p24_lito_lito_exp.lito_battle_settlement` | 啟動前 0／後 0 | 未查詢 |
| `lito_source_server` | source_id=1、enabled=1、路由 `p24_accounts_lito_exp`，前後相同 | 未查詢／修改 |
| 新靜態實驗庫 | 已匯入 `380_db.sql`，181 表，新增 SELECT 授權 | 未再匯入／授權 |
| 原 P24 三庫 | 270 表 checksum 前後一致 | 本輪未連線 |
| 啟動 | exit 1，`ranking_points` 缺欄位，沒有 ready | 未啟動 |
| 關閉 | 前次 MySQL 已正常關閉；收據見下方 | 本輪沒有啟動任何實驗程序，無需關閉 |

原始證據：[before.txt](issue-284/before.txt)、[after.txt](issue-284/after.txt)、[verification.json](issue-284/verification.json)、[完整主控台](issue-284/server-console.txt)、[關閉收據](issue-284/p24-shutdown-verification.json)、[冷備份收據](issue-284/p24-backup.json)。前次完整冷備份位於前次 R 的 `private-cold-backup/db-data/`，1,407 檔／255,481,289 bytes；本輪未讀寫該備份。

前次錯誤摘要：

```text
java.sql.SQLException: character_lito in p24_accounts_lito_exp lacks ranking columns; run db/lito_character_ranking.sql first
Caused by: java.sql.SQLSyntaxErrorException: Unknown column 'ranking_points' in 'field list'
```

## 已驗證：遷移方案的靜態核對

[主控已裁定補實驗結構](https://github.com/chaoren0418-arch/lito-launcher-server/issues/284#issuecomment-6855340338)，不再是「等待主控提供修復方案」。`tools/acceptance/m4-db-prepare.sql` 的 P1c 已包含 `ranking_points`、`disconnects`、`monthly_period`、`monthly_ranking_points`、`monthly_wins`、`monthly_losses` 六欄，按欄檢查是否存在，並有 43324／UUID／`_lito_exp` 守門。

`lito伺服器/LiTo381-Server/db/lito_character_ranking.sql` 原檔把 `@lito_source` 設為 **`380_ac_1`**；不能原樣執行，也不能只在同連線先 SET 實驗庫，因原檔會覆寫它。P1c 已實作等價六欄遷移，優先沿用此實驗版；若另需排名 SQL，必須在 scratch 複本改成實驗庫並加入相同守門。本輪沒有執行任何 SQL 或產生虛構還原收據。

## 推測

六欄遷移應能解決前次排名欄位錯誤，但尚未對線上實驗庫驗證；不能推論後續載入、監聽或大廳登入會成功。

## 待驗證／給下一棒的提示

由**調度器／主控**處理派工內容與末段執行禁令的衝突，再交具明確實機執行授權的 Codex 接手。不是要求使用者自行整理檔案或修 DB；一般自動測試交 GitHub Actions。

1. 重核當次有效 lease，依既有 P24Lab 流程取得當次完整備份，確認實驗實例身分。不要拿本輪收據當未來有效租約，不動正式 2000／3306、原 P24 庫或 tw-bin。
2. 依 `tools/acceptance/README.md` 先跑 `m4-db-verify.sql`，按 `generated/m4-required-columns.tsv` 列出全部缺口；套用有守門的 `m4-db-prepare.sql` 後重跑 verify。保存前後表結構、筆數、權限與還原方案；不要直接跑未改寫的排名 SQL。T014 不需要先執行 M4 全套 seed。
3. 靜態庫已匯入的前次證據完整；先查現況，不刪除重建。只在驗證需要時依原裁定處理實驗庫。角色路由維持 `p24_accounts_lito_exp`。
4. [run-experiment.cmd](issue-284/run-experiment.cmd) 與既有 PowerShell 入口仍含此 JAR 的正確固定 SHA，無帳密。它會新建另一個空 runtime、從 #83 私密檔讀憑證，**連 `-PrepareOnly` 都會執行 Java**，本輪沒有執行。僅供獲准後的 T014 初始狀態重跑；它的舊 Probe 要求收據 0 筆，若已有收據，不可刪表湊零，改用 T128 快照流程。M4 全套驗收不要沿用此舊 Probe。

   ```bat
   docs\reports\issue-284\run-experiment.cmd -CandidateDirectory "D:\lineage開發\3-lito開發\scratch\wt\task-284\scratch\t014\run-20261113T145646Z"
   ```

5. 憑證及 DB 前置就緒後，保存本次完整 Java 主控台，120 秒內判定啟動；用 T128 監聽檢查（該腳本要求 `scratch/m4/run-*`，不能直接傳本輪 t014 目錄）。登入到大廳，PNG 存 `docs/reports/issue-284/`，取得啟動前後兩張表筆數，正常關閉本次啟動的程序。既有入口的 120 秒是操作要求，沒有自動逾時終止功能。

尚缺：含憑證的實驗設定、當次 DB 核對／遷移及還原證據、server ready／監聽、登入大廳 PNG、當次筆數對照與關閉證據。JAR／lib／暫存檔均不入 git；未改正式設定、封存、硬連結，未進行遞迴刪除。
