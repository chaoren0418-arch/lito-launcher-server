# Issue #284：正式 0.6.71／P24 實驗啟動預檢

## 結論

**阻塞，已標記 `needs:claude`。** 2026-11-07，本機執行器以指定正式 JAR、#83 實驗設定及全新 runtime 執行一次啟動。JDBC 連線、實例身分、來源路由與統一設定驗證通過；伺服器在資源初始化階段因 `角色_智力成長` 的 SELECT 權限不足而以退出碼 **1** 結束，未出現 `[LiTo] ready`。

依任務失敗處置立即停止，沒有擴權、修程式、補表或搬資料。登入器、大廳登入與截圖未執行，不能作為 T011 的已完成前置。P24Lab 已正常關閉。

## 已驗證：範圍、隔離與產物

- 工作目錄：`D:\lineage開發\3-lito開發\scratch\wt\task-284`；分支維持 `task/284`，未 checkout／另建分支。
- 已讀根 `AGENTS.md`、`docs/REPORT.md`、T014、#83 報告與 P24Lab 既有啟停腳本。`docs/REPORT.md` 未修改。
- `http://localhost:8787/api/state` 核實 lease：`p24lab`，持有人 `chaoren0418-arch/lito-launcher-server#284`，取得 `2026-11-07T01:22:47.2077813Z`，到期 `2026-11-07T02:07:47.2077813Z`。本次 DB 使用全在有效期間內；RESULT 交回調度器處理 lease，不自行變更 SQLite。
- 本次實際啟動目錄：`scratch/t014/run-20261107T012544Z/`。來源 JAR／lib 只讀取、普通複製並逐檔核對 SHA-256，未使用硬連結。
- 正式 JAR 來源：`D:\lineage開發\3-lito開發\lito登入器\LiTo381-Launcher\app\server\LiTo381-SharedResources-candidate.jar`。
- 以正式 JAR 內 `UnifiedConfiguration.KEYS` 與 `validate` 驗證完整 **22 鍵**；帳密僅在記憶體讀取 #83 `isolated-test.properties` 並寫入本次 Git 忽略的私密實驗設定，未讀正式 `config/server.properties` 或測試登入密碼。
- `runtime/` 為新建空目錄，啟動前項目數 **0**；退出後僅有 `battle-settlements/owner.lock`，沒有待 replay 的 `.pending` journal；以獨占方式開啟 lock 檔成功，確認已釋放。
- 使用 Java 17：`C:\Program Files\Microsoft\jdk-17.0.17.10-hotspot\bin\java.exe`。未使用 Maven 產物／戰鬥候選替代。
- 交付前再次核對正式來源 JAR 雜湊相同；7 份公開檔案的實際密碼字串掃描、PowerShell 語法解析與 `git diff --check` 通過，私密設定確實被 Git 忽略。沒有產品程式碼變更，未做 Maven 建置。

| 檔案 | bytes | 來源與副本相同的 SHA-256 |
| --- | ---: | --- |
| `LiTo381-SharedResources-candidate.jar` | 20054975 | `E1498871C7BE6B287821C57EF5D95933DB1740D9B4D4A80F6C3465F21D99D498` |
| `lib/HikariCP-5.1.0.jar` | 161840 | `A47A6EE62379694EE52C30036F0931B72F9AEE2A801D590341ED82BD839E2134` |
| `lib/mysql-connector-j-8.0.33.jar` | 2481560 | `E2A3B2FC726A1AC64E998585DB86B30FA8BF3F706195B78BB77C5F99BF877BD9` |
| `lib/protobuf-java-3.21.9.jar` | 1671409 | `1B78B4A76A71512DEBFDFF8F8FC5AEF6BFD459F65758FECF7AFF245E6E6301E4` |
| `lib/slf4j-api-1.7.36.jar` | 41125 | `D3EF575E3E4979678DC01BF1DCCE51021493B4D11FB7F1BE8AD982877C16A1C0` |

## 已驗證：設定（帳密遮蔽）

設定路徑為本次 `run-20261107T012544Z/config/experiment.properties`；路徑值相對該 `config/`，所以 captures、logs 與 runtime 都落在本次新目錄。

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

## 已驗證：P24Lab、路由與前後筆數

沿用 #83 的 P24Lab 啟停方式，先核對離線 UUID／埠無衝突，再建立冷備份 `D:\BinEngine\environments\p24\backups\issue-284-run-20261107T012544Z\db-data`，**1,407 檔逐檔 SHA-256 相同**。一般沙箱在建立備份目錄時拒絕寫入；經工具權限提升後完成既定備份及啟動。沒有搬入／複製 SQL 資料到任何 schema，也沒有修改原有庫或 tw-bin 測試資料的 SQL 操作。

P24Lab 由本次啟動：mysqld 監督程序 PID **23644**，監聽子程序 PID **21324**。受限 JDBC 帳號於啟動前後皆核實 `CURRENT_USER()`、43324、bind `127.0.0.1`、UUID `e3443e40-d090-11f1-8040-4cedfb679789`、datadir `D:\BinEngine\environments\p24\db-data\`。

來源路由 SELECT 結果只有一列：`source_id=1`、`character_database=p24_accounts_lito_exp`、`enabled=1`。**無須修正，修改前後相同。** 因此預期來源遊戲入口為 `127.0.0.2:42084`，本次未成功建立監聽。

| 表 | 前：01:27:18Z | 後：01:27:54Z | 差異 |
| --- | ---: | ---: | ---: |
| `p24_accounts_lito_exp.character_lito` | 2 | 2 | 0 |
| `p24_lito_lito_exp.lito_battle_settlement` | 0 | 0 | 0 |

查詢使用公開附件 [Probe.java](issue-284/Probe.java)，只有 SELECT；筆數不變不代表所有欄位皆已逐值比對，也不代表已驗證登入或結算。

Java 主命令於 **01:27:41.3092457Z** 開始，**01:27:42.9124075Z** 結束，約 **1.60 秒**，未達 120 秒期限。JAR 執行一次；先前命令包裝曾因 Windows 引號失敗，當時 Java 尚未啟動，未算作伺服器重試。準備腳本也曾因 PowerShell 5.1 無 BOM 解讀中文路徑而誤報來源雜湊不符，改為 UTF-8 BOM 後核對通過；沒有更換 JAR。

退出後核對兩個 mysqld 的生命週期與已記錄的可執行路徑、啟動時間；正常 SHUTDOWN 前再次核實 DB 身分且沒有其他客戶連線。**01:28:28.2872676Z**，SHUTDOWN exit=0，本次程序已退出；43324、42061、42083、42084 均無監聽。停機守門最初因 Windows PowerShell JSON 將啟動時間截為毫秒而拒絕；核實差異僅為小數精度後，以相同毫秒精度比對成功，未放寬 PID／路徑守門、未強殺程序。

## 已驗證：啟動命令與完整主控台輸出

工作目錄為本次 run 目錄；stdout／stderr 合併保存於 `logs/server-console.log`，公開附件 [server-console.txt](issue-284/server-console.txt) 與以下全文相同，僅統一編碼及換行。

```cmd
"C:\Program Files\Microsoft\jdk-17.0.17.10-hotspot\bin\java.exe" -Xms512m -Xmx2048m -XX:ActiveProcessorCount=4 -Dfile.encoding=UTF-8 -jar LiTo381-SharedResources-candidate.jar config/experiment.properties
```

```text
[LiTo] resource-profile=lito-legacy
SLF4J: Failed to load class "org.slf4j.impl.StaticLoggerBinder".
SLF4J: Defaulting to no-operation (NOP) logger implementation
SLF4J: See http://www.slf4j.org/codes.html#StaticLoggerBinder for further details.
Exception in thread "main" java.lang.IllegalStateException: Cannot load LiTo room policy or qualified resource tables; inspect the nested database error
	at local.lito381.server.NativeRoomRules.install(NativeRoomRules.java:35)
	at local.lito381.server.GameHub.<init>(Unknown Source)
	at local.lito381.server.Main.main(Unknown Source)
	at local.lito381.server.SharedResourceMain.main(SharedResourceMain.java:7)
Caused by: java.sql.SQLSyntaxErrorException: SELECT command denied to user 'lito83_exp'@'127.0.0.1' for table '角色_智力成長'
	at com.mysql.cj.jdbc.exceptions.SQLError.createSQLException(SQLError.java:121)
	at com.mysql.cj.jdbc.exceptions.SQLExceptionsMapping.translateException(SQLExceptionsMapping.java:122)
	at com.mysql.cj.jdbc.StatementImpl.executeQuery(StatementImpl.java:1200)
	at local.lito381.server.NativeProcHealBonus.read(NativeProcHealBonus.java:19)
	at local.lito381.server.NativeProcHealBonus.load(NativeProcHealBonus.java:27)
	at local.lito381.server.ResourceDatabase.load(Unknown Source)
	at local.lito381.server.NativeRoomRules.install(NativeRoomRules.java:33)
	... 3 more
```

## 推測

呼叫鏈顯示正式 JAR 的 `NativeProcHealBonus.load/read` 會在 `ResourceDatabase.load` 階段讀取智力成長表；#83 公開逐表授權清單列了精神／體質／力量／敏捷成長，沒有智力成長。這很可能是正式 JAR 與 #83 授權需求盤點之間的缺口；**不是本輪擴權的授權依據**。

本次錯誤先於 ready，不能推論排除此項後就能成功，也不能推論 map 12000 或其他欄位／表問題已排除。

## 待驗證、產出路徑與給下一棒的提示

1. **Claude 接手裁定**正式 JAR 的精確讀取表需求與 #83 權限差異，另派經核准的前置修復。本輪禁止擴權，沒有執行 GRANT、DDL、UPDATE 或資料補製。#285／T011 仍需等待 T014 真正就緒。
2. 未啟動登入器／遊戲、未測試登入大廳、未取得 PNG；`docs/reports/issue-284/` 沒有用假圖補交。map 12000、完整 healthCheck、技能與結算均未驗證。
3. 公開交付為本報告、完整主控台、[run-experiment.cmd](issue-284/run-experiment.cmd)、[run-experiment.ps1](issue-284/run-experiment.ps1)、[verify-run.ps1](issue-284/verify-run.ps1)、[Prepare.java](issue-284/Prepare.java)、[Probe.java](issue-284/Probe.java)。命令不含帳密；Java 輔助程式只做設定準備／唯讀核對，不修改伺服器程式。既有 verify-run 守門同步至本輪設定與 Probe 收據格式；重跑 JVM 暫存固定到新 run 的 tmp。
4. 重跑前由獲授權的 P24Lab 管理者重新取得 **#284 有效 lease**、先冷備份並啟動既有 43324 實例；不得使用本報告已到期的 lease，也不得接手他人程序。執行 `docs\reports\issue-284\run-experiment.cmd`，每次自建新 `run-<UTC時間>/`、複製核對正式 JAR／lib、驗證 22 鍵、唯讀核對路由／筆數，再執行相同 `java -jar`。已有收據、來源非單一實驗路由、埠衝突或錯誤會停止。此入口不管理 DB 生命週期；使用完由管理者依 #83 流程核實身分與連線後正常 SHUTDOWN。
5. **重跑入口為有人值守的主控台入口**：`startup.timeout.seconds=120` 是設定值，不會讓直接 `java -jar` 自動退出。操作者須計時，120 秒內未 ready 即在該主控台 Ctrl+C，保存輸出並停止；本次實際錯誤在 1.60 秒內自然退出。成功後只做授權範圍內的登入檢查並 Ctrl+C 正常關閉。若 Ctrl+C 中止 PowerShell 管線使後置 SELECT 未執行，使用 Probe.java 補做筆數核對，不可直接宣稱已清理。
6. `run-experiment.cmd -PrepareOnly` 已在 P24Lab 關閉狀態實測成功，生成 `scratch/t014/run-20261107T013007Z/`；只驗證複製與設定，不重啟 DB／伺服器。正式主命令與 Probe 已在本輪實際執行；公開包裝的完整再次啟動未重跑，遵守遇錯停止要求。
7. 本機原始證據保留於 `scratch/t014/run-20261107T012544Z/`：`artifact-hashes.json`、`preparation.json`、`lease.json`、`config-public.properties`、`before.txt`、`after.txt`、`logs/server-console.log`、`logs/server-exit.json`、`p24-backup.json`、`p24-owned-start.json`、`p24-mysql.*.log`、`p24-shutdown-verification.json`。私密設定、二進位、冷備份與原始紀錄均不提交；不刪 worktree、junction、runtime 或備份。
