# Issue #284：當版 main 實驗啟動驗測（2026-11-13）

## 結論

**阻塞，T014 尚未完成，不得放行 #285。** 當版 JAR 已實際啟動，但 `p24_accounts_lito_exp.character_lito` 缺少 `ranking_points`，在 `SettlementSql.check` exit 1，未 ready、未進入大廳，未啟動登入器／遊戲，沒有本輪 PNG。

已完成完整冷備份、新靜態實驗庫匯入與 SELECT 授權、22 鍵設定、全新空 runtime、前後資料核對及正常關閉本次 MySQL。依本次「失敗停止」指示，未繼續修改角色表或再次啟動；交 `needs:claude` 提供當版實驗角色庫的修復方案。

## 已驗證：授權、租約與成品

- 使用者 2026-11-13 新指示允許 server `127.0.0.1:42083`、P24Lab `127.0.0.1:43324`、實驗 schema 與指定 SELECT 授權，取代先前只讀及 42061 限制。本輪沒有再以舊限制阻擋執行。
- [本次派工](https://github.com/chaoren0418-arch/lito-launcher-server/issues/284#issuecomment-6854339016)、[專項範圍](https://github.com/chaoren0418-arch/lito-launcher-server/issues/284#issuecomment-6850874413)、[靜態庫裁定](https://github.com/chaoren0418-arch/lito-launcher-server/issues/284#issuecomment-6701189885)。只做 #284，未做 #285／#341。
- 對話查重：本對話 active，舊 #284 `01a1d4d4-c289-7cc0-967f-2a19d58c6860` idle；調度器 running 清單無另一個 #284 執行器。
- `/api/state` 的 `p24lab` holder=`chaoren0418-arch/lito-launcher-server#284`，exp=`1794579228.0751715`，即 **2026-11-13 22:13:48 +08:00**。啟動前再次核對有效，未自行續租。
- 工作樹 `D:\lineage開發\3-lito開發\scratch\wt\task-284`，分支 `task/284`，開始 HEAD `98d6106ffa8f36966befb40a1040fbaa0569915e`。
- 新 fetch 的 main=`ad12ee9cf63922aaba455272bc5fe3a7bc3b9556`。相對先前 `42808209` 有伺服器程式變更，因此必要重建一次；舊 JAR `64F4E3EB…` 保留。
- 從 main 匯出原始 pom、src、compile-stubs、tools/as-record 到 scratch，未改建置輸入；`mvn -f <匯出根>/lito伺服器/LiTo381-Server/pom.xml -B -DskipTests package` 於 **21:42:27 +08:00** BUILD SUCCESS，耗時 1 分 30 秒，測試未執行。
- 使用 workspace-write 與必要個別工具核准。未改安全設定、研究執行器或換工具繞過核准；未改產品程式、正式環境或 docs/REPORT.md。

本輪證據根（下稱 R）：`scratch/t014/run-20261113T133857Z/`。

JAR SHA-256：`FF4EAD8B222E7682A09C1C55979753A943B10402320E36C3F27B8B0E383F7064`。四個 lib 為 HikariCP 5.1.0、mysql-connector-j 8.0.33、protobuf-java 3.21.9、slf4j-api 1.7.36；來源 target 與副本逐檔雜湊相同，完整大小／SHA 見 [artifact-sha256.json](issue-284/artifact-sha256.json)。建置日誌留 R/maven-package.log，不提交 JAR／lib。

## 已驗證：完整備份與實驗庫變更

P24Lab 實際位置為 `D:\BinEngine\environments\p24`。啟動前無實驗埠監聽／既有 P24 mysqld，離線 UUID 正確。完整 `db-data` 冷備份保留於 **R/private-cold-backup/db-data/**，1,407 檔、255,481,289 bytes，逐檔 SHA-256 與來源相同；備份及資料內容不提交。**21:42:02 +08:00** 備份核對完成後，才啟動本輪 MySQL PID 16728。

線上身分：port 43324、bind 127.0.0.1、UUID `e3443e40-d090-11f1-8040-4cedfb679789`、datadir `D:\BinEngine\environments\p24\db-data\`，全部相符。

依主控裁定直接處理已知靜態庫相容性問題，未再重跑舊靜態庫相同失敗：

- 唯一來源 `D:\BinEngine\environments\p24\sql\380_db.sql`，9,552,749 bytes，SHA-256 `0875F7CFF78D7D97A7380CC5D24F47B2C66B630D452377E5F8BFA6F3D29FD95E`。
- 匯入前確認新庫不存在；快照無 CREATE DATABASE／USE、跨 schema qualified identifiers、GRANT、DEFINER、routine／trigger／event、LOAD DATA／OUTFILE。原快照只讀，不改、不複製進 repo。
- 僅新建 `p24_380db_lito_exp`，mysql 明確指定此 database 匯入；**21:50:21 +08:00** exit 0，共 181 表。未對既有庫執行快照的 DROP TABLE。
- 只新增 `GRANT SELECT ON p24_380db_lito_exp.* TO 'lito83_exp'@'127.0.0.1'`。SHOW GRANTS 前後差異只有此項，其他權限不變。
- 未建立 `p24_380lito_lito_exp`，未使用 `380_lito.sql`；尚未遇到需要替換 lito 控制庫的錯誤。

| 核對項 | 前 | 後 |
| --- | --- | --- |
| 新靜態庫 | 不存在（匯入前） | 181 表、匯入 exit 0 |
| lito_source_server | source_id=1、enabled=1、p24_accounts_lito_exp | 相同，未修改 |
| character_lito | 2 | 2 |
| lito_battle_settlement | 0 | 0 |
| 原 p24_world／p24_accounts／p24_lito | 270 表 checksum | 全部一致 |
| lito83_exp 權限 | 既有權限 | 只增加新靜態庫 SELECT |

證據：[before.txt](issue-284/before.txt)、[after.txt](issue-284/after.txt)、[匯入前](issue-284/static-import-before.json)、[匯入後](issue-284/static-import-after.json)、[verification.json](issue-284/verification.json)。checksum、grants、冷備份收據在同目錄。原其他 P24 庫沒有寫入，冷備份涵蓋整個 datadir。

## 已驗證：設定與啟動

以 #83 私密設定提供憑證，不輸出或提交秘密。候選 JAR 的 `UnifiedConfiguration.KEYS` 22 鍵反射核對及 validate 通過。設定見 [config-public.properties](issue-284/config-public.properties)：

- bind=127.0.0.1、game.bind=127.0.0.2、port=42083；DB base URL=`jdbc:mysql://127.0.0.1:43324/`。
- 靜態庫按裁定由 `p24_world_lito_exp` 改為 `p24_380db_lito_exp`；控制庫維持 `p24_lito_lito_exp`；角色路由維持 `p24_accounts_lito_exp`。
- captures／logs 指向 R 下新目錄；啟動前 R/runtime 全新且空，無舊 journal。

第一次命令因 PowerShell 拆開未加引號的 `-Dfile.encoding=UTF-8`，JVM 回報 `ClassNotFoundException: /encoding=UTF-8`，未執行產品 main。原始輸出留 R/logs/argument-error/（原中文編碼失真未改寫）。確認 runtime 仍空後，只修正引號重試一次，未改產品程式。

有效產品啟動時間 **21:51:15.591 +08:00**，**21:51:16.105 +08:00** exit 1；未 ready，120 秒內已判定。完整主控台見 [server-console.txt](issue-284/server-console.txt)：

```text
java.sql.SQLException: character_lito in p24_accounts_lito_exp lacks ranking columns; run db/lito_character_ranking.sql first
    at local.lito381.server.SettlementSql.check(SettlementSql.java:46)
    at local.lito381.server.DurableBattleSettlement.open(DurableBattleSettlement.java:17)
    at local.lito381.server.SharedDatabase.open(SharedDatabase.java:57)
Caused by: java.sql.SQLSyntaxErrorException: Unknown column 'ranking_points' in 'field list'
```

本輪依失敗停止要求，未繼續遷移角色表或重啟。此錯誤發生在舊 NativeArmorProc 缺欄位錯誤之前，不能宣稱靜態庫全部 runtime 讀取已通過。

## 已驗證：關閉與重跑入口

Java 自行 exit 1。停止 MySQL 前核對本輪 PID 16728、啟動時間、執行檔、線上身分及無其他 client，正常 SHUTDOWN exit 0；**21:52:24 +08:00** 已退出，43324／42061／42083／42084 無監聽，見 [關閉收據](issue-284/p24-shutdown-verification.json)。未停止他人程序。

[run-experiment.cmd](issue-284/run-experiment.cmd)／PowerShell／verify 入口已改用本次新 JAR、固定 SHA、新靜態庫及全新 runtime，不含帳密。重跑入口不啟停 MySQL、不執行 DDL／GRANT；前置為有效 #284 lease、當次完整備份、正確 Lab 及主控確認的新修復證據。

```bat
docs\reports\issue-284\run-experiment.cmd -CandidateDirectory "D:\lineage開發\3-lito開發\scratch\wt\task-284\scratch\t014\run-20261113T133857Z"
```

僅供有新證據後重現，本輪不再執行。入口語法與 Git diff／秘密排除檢查不等於大廳驗收。

## 推測

當版 JAR 的角色排名契約與 #83 建立的實驗角色表存在落差。現有證據只證實 `ranking_points` 缺失；未調查或套用遷移，也不推論其他欄位、啟動階段或登入會通過。

## 待驗證與交接

交主控 `needs:claude`：提供只作用於 `p24_accounts_lito_exp.character_lito` 的明確遷移方案及前後核對要求，再派回本對話；下次寫入前仍須完整備份。新靜態庫與 SELECT 已完成，勿刪除重建或無新資訊重跑同一失敗。

仍未完成：server ready／42083 監聽、測試登入器到大廳及 PNG。不得放行 #285。私密設定、備份、SQL 內容與二進位均留本機 scratch；未碰正式 2000／3306、正式 BIN、正式設定、封存或 docs/REPORT.md。
