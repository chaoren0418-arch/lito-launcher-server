# Issue #284：正式 0.6.71／P24 實驗啟動（第 3 次嘗試）

## 結論

**blocked／未完成，需 `needs:claude`；不得放行 #285／T011。** 2026-11-07，本次依派工訊息的既定設定啟動正式 JAR 副本，仍在 `NativeArmorProc.load` 因 `Unknown column 'id' in 'field list'` 退出（exit=1）。未 ready、未啟動登入器／遊戲、沒有大廳 PNG。前後兩表筆數為 2／0，原三庫 270 表 checksum 相同；P24Lab 已正常關閉。

本次未變更授權、匯入 SQL、補表、改欄位或改產品程式。修正了重跑 PowerShell 入口組合 Java 命令時的陣列運算優先順序錯誤。報告合併只代表阻塞證據入庫，不代表 T014 完成。

## 已驗證：範圍與最新裁定差異

- 工作目錄為 `D:\lineage開發\3-lito開發\scratch\wt\task-284`，沿用 `task/284`，未 checkout／建立分支。已讀 AGENTS.md、docs/REPORT.md、TASKS 的 T014、#83 與前次報告。未修改 docs/REPORT.md。
- 調度器 `/api/state` 的 `p24lab` lease 持有人為 `chaoren0418-arch/lito-launcher-server#284`，exp=`1794022350.5849743`（2026-11-07T03:32:30Z），與派工相符。實驗全程在有效期內。
- [02:43 最新主控裁定](https://github.com/chaoren0418-arch/lito-launcher-server/issues/284#issuecomment-6701189885) 要求匯入既有 `380_db.sql` 至新庫 `p24_380db_lito_exp`、新增 SELECT，必要時同樣處理 `380_lito.sql`。但本次直接派工仍明定「不擴大權限」「不複製資料」、static 庫固定 `p24_world_lito_exp`，且遇錯停止。已向使用者提出範圍確認；本報告記錄的是**原範圍重跑**，不是新裁定的完成證據。
- GitHub 連接工具不可用；一般沙箱中的 gh 回報 HTTP 401，權限提升後可讀取 Issue。未因這些工具問題更動任何憑證。
- 正式來源只讀取並普通複製 JAR／lib；未改原始專案、正式 config/server.properties、LiTo.bin、shared BIN 或客戶端資源；未連 2000／3306。#83 私密實驗設定僅供建立本輪設定，未輸出或提交帳密。

## 已驗證：成品、設定與 runtime

本輪目錄：`scratch/t014/run-20261107T024916Z/`。來源為 `D:\lineage開發\3-lito開發\lito登入器\LiTo381-Launcher\app\server`。JAR／四個 lib 的來源與副本 SHA-256 全部一致；詳見 [verification.json](issue-284/verification.json)。

| 檔案 | bytes | SHA-256 |
| --- | ---: | --- |
| `LiTo381-SharedResources-candidate.jar` | 20054975 | `E1498871C7BE6B287821C57EF5D95933DB1740D9B4D4A80F6C3465F21D99D498` |
| `lib/HikariCP-5.1.0.jar` | 161840 | `A47A6EE62379694EE52C30036F0931B72F9AEE2A801D590341ED82BD839E2134` |
| `lib/mysql-connector-j-8.0.33.jar` | 2481560 | `E2A3B2FC726A1AC64E998585DB86B30FA8BF3F706195B78BB77C5F99BF877BD9` |
| `lib/protobuf-java-3.21.9.jar` | 1671409 | `1B78B4A76A71512DEBFDFF8F8FC5AEF6BFD459F65758FECF7AFF245E6E6301E4` |
| `lib/slf4j-api-1.7.36.jar` | 41125 | `D3EF575E3E4979678DC01BF1DCCE51021493B4D11FB7F1BE8AD982877C16A1C0` |

正式 JAR 的 `UnifiedConfiguration.KEYS`／`validate` 驗證完整 22 鍵通過。[完整公開設定](issue-284/config-public.properties) 已遮除帳密：

```properties
battle.ai.enabled=false
battle.natural.mp.regen.enabled=true
battle.physical.hit.enabled=true
bind=127.0.0.1
capture.directory=../captures
cpu.limit=4
database.jdbc.base.url=jdbc:mysql://127.0.0.1:43324/
database.jdbc.options=?useUnicode=true&characterEncoding=UTF-8&serverTimezone=UTC&useSSL=false&allowPublicKeyRetrieval=true
database.lito.database=p24_lito_lito_exp
database.password=[REDACTED]
database.static.database=p24_world_lito_exp
database.user=[REDACTED]
game.bind=127.0.0.2
logs.directory=../logs
memory.initial=512m
memory.maximum=2048m
native.siege.roster.enforced=true
native.tournament.allowance=20000
port=42083
server.name=LiTo T014 P24 Experiment
startup.timeout.seconds=120
weight.rate=100.0
```

runtime 啟動前為全新空目錄（0 項），退出後僅 `battle-settlements/owner.lock`；無 pending journal，獨占開啟 owner.lock 通過，鎖已釋放。未刪除任何舊 runtime、備份、worktree 或 junction。

## 已驗證：P24Lab、資料與正常關閉

依既有流程，確認實驗埠無衝突、無既有 P24 mysqld、離線 UUID 相符後，建立冷備份 `D:\BinEngine\environments\p24\backups\issue-284-run-20261107T024916Z\db-data`，1,407 檔逐檔 SHA-256 相同。使用工具權限提升啟動本輪 P24Lab，PID=2908；未寫入原始專案。

線上確認 port=43324、bind=127.0.0.1、UUID=`e3443e40-d090-11f1-8040-4cedfb679789`、datadir=`D:\BinEngine\environments\p24\db-data\`，受限帳號 CURRENT_USER 相符。`lito_source_server` 僅一列：source_id=1、character_database=`p24_accounts_lito_exp`、enabled=1；前後相同，未修改路由。

| 核對 | 啟動前 02:51:39Z | 退出後 02:51:51Z |
| --- | ---: | ---: |
| `p24_accounts_lito_exp.character_lito` | 2 | 2 |
| `p24_lito_lito_exp.lito_battle_settlement` | 0 | 0 |

受限 JDBC 的完整唯讀結果見 [before.txt](issue-284/before.txt)、[after.txt](issue-284/after.txt)。[SHOW GRANTS 前值](issue-284/grants-before.txt)／[後值](issue-284/grants-after.txt) 完全一致；上輪三個實驗 schema 的 SELECT 已存在，本輪未執行 GRANT。

原 `p24_world`、`p24_accounts`、`p24_lito` 共 270 表的 `CHECKSUM TABLE EXTENDED` [前值](issue-284/original-checksum-before.tsv)／[後值](issue-284/original-checksum-after.tsv) 完全一致。沒有對原庫或 tw-bin 資料執行 DML／DDL。筆數一致不等同完整角色戰績或結算功能驗證。

02:52:11Z 核對本輪程序身分、線上實例及無其他客戶連線後正常 SHUTDOWN（exit=0）；PID 2908 已退出，43324／42061／42083／42084 均無監聽。未強殺程序。停止收據見 verification.json。

## 已驗證：啟動輸出與入口修正

Java 17：`C:\Program Files\Microsoft\jdk-17.0.17.10-hotspot\bin\java.exe`。實際 JVM 啟動 02:51:49.1129030Z，退出 02:51:50.5491961Z，約 1.44 秒，exit=1；未達 120 秒逾時。工作目錄為本輪 run：

```bat
java -Xms512m -Xmx2048m -XX:ActiveProcessorCount=4 -Dfile.encoding=UTF-8 "-Djava.io.tmpdir=%CD%\tmp" -jar LiTo381-SharedResources-candidate.jar config/experiment.properties
```

[完整合併 stdout／stderr](issue-284/server-console.txt) 保存全部輸出，主要錯誤：

```text
Exception in thread "main" java.lang.IllegalStateException: Cannot load LiTo room policy or qualified resource tables; inspect the nested database error
Caused by: java.sql.SQLSyntaxErrorException: Unknown column 'id' in 'field list'
    at local.lito381.server.NativeArmorProc.load(NativeArmorProc.java:14)
```

前輪已核實該載入器對 `裝備_armor_魔法` SELECT 英文欄位 id、item_id 等，而實驗表使用「編號、道具編號」等中文欄位；[armor-columns.tsv](issue-284/armor-columns.tsv) 為**前輪 metadata**，本輪未重新查詢欄位。本輪錯誤與前輪相同，未繼續修補或重啟。

首次包裝命令因 PowerShell 的 `@('字串','"'+$java+'參數','字串')` 將拼接拆為多個陣列元素，產生五行 cmd，出現 `C:\Program is not recognized`；JVM 未啟動，未產生 server-console.log，runtime 仍空。修正為對拼接表達式加括號後，cmd 為三行，才執行上述唯一一次 JVM 啟動。公開 run-experiment.ps1 同步修正；未變更產品 Java。

## 推測

更換靜態資源來源可能排除這個欄位缺口，但新快照是否符合全部載入器、map 12000、登入與結算需求均未驗證。不能把新裁定或快照存在當成能進大廳的證明。

## 待驗證、產出與給下一棒的提示

1. **Claude／本機調度器先對齊下一次派工範圍與 02:43 裁定**。本次訊息仍禁止擴權／複製資料，因此未實施新庫方案；使用者確認問題已提出。不要再以相同舊範圍自動重派期待不同結果。若採新裁定，明確交接新庫、匯入與 SELECT 範圍，並同步 Prepare／Probe／verify-run 的庫名守門；現有入口仍固定舊三庫。
2. 新裁定提及的兩個既有 SQL 檔經唯讀雜湊與 metadata 檢查，沒有匯入、複製進 repo 或改寫。380_db.sql：9,552,749 bytes，SHA-256 `0875F7CFF78D7D97A7380CC5D24F47B2C66B630D452377E5F8BFA6F3D29FD95E`；380_lito.sql：271,713 bytes，SHA-256 `78B6940C39A1A1EA87F3AEAABCA36D3281CE60879518073621DC1B52E24C4919`。兩檔 LinkType 均為 HardLink；未寫入或拆除。初步字串掃描不是 SQL 安全審查，不代表可直接匯入。
3. 未完成：伺服器 ready／42083 監聽、登入器連線、測試帳號登入大廳與 PNG、後續 M1 回歸。未讀測試登入密碼，未產生替代截圖。
4. [run-experiment.cmd](issue-284/run-experiment.cmd)／[run-experiment.ps1](issue-284/run-experiment.ps1) 不含帳密，每次建立新 run。需有效 #284 lease、依既有規則冷備份及啟動 P24Lab；入口不自動管理 DB、改權限或匯入資料。120 秒未 ready 須由值守者 Ctrl+C 正常停止；不能將設定值當成自動逾時。
5. 實際驗證：PrepareOnly、22 鍵、受限 JDBC 前後 Probe、verify-run、修正後 cmd 啟動及退出碼、正常 DB 關閉。完整公開包裝的 Tee 管線未端到端重跑；本次 console 由 cmd 合併重新導向。沒有產品程式變更，未跑 Maven 或額外重啟服務。
6. 公開產出為本報告與 `docs/reports/issue-284/`；本輪原始證據與私密設定留在 `scratch/t014/run-20261107T024916Z/`，不提交 JAR、lib、秘密或冷備份。前輪歷史由 Git 保留。

7. 交付檢查：16 份公開檔案以實際實驗 DB 密碼比對掃描通過；私密設定被 Git 忽略。PowerShell 語法、修正後三行 cmd、正式來源 JAR 雜湊及 git diff --check 通過。
