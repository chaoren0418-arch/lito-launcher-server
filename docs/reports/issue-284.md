# Issue #284：T014 正式 0.6.71 實驗啟動

## 結論／做了什麼

**阻塞，需 `needs:claude` 裁定，未通過啟動到大廳驗收。** 2026-11-07 在 `task/284` worktree 複製正式 0.6.71 JAR 與四個 lib，建立全新實驗設定及空 runtime，以 #83 受限帳號連接 P24Lab。正式 JAR 約兩秒後以錯誤退出：`NativeProcHealBonus` 讀取 `角色_智力成長` 時 SELECT 被拒絕。依任務停止，沒有改程式、擴權、補表或複製資料來排除錯誤。

啟動前後 `character_lito` 均為 2 筆、`lito_battle_settlement` 均為 0 筆；來源路由正確，未修改。Java 已退出，P24Lab 已正常 SHUTDOWN。未啟動登入器／遊戲，沒有大廳截圖。

## 已驗證／怎麼驗證

- 已讀根目錄 `AGENTS.md`、`docs/REPORT.md`、`TASKS.md` T014、#83／#283 報告及 P24Lab 所屬專案規則；依本 Issue 的實驗啟動授權執行，不啟停正式環境。沒有修改 `docs/REPORT.md`。
- 工作目錄：`D:\lineage開發\3-lito開發\scratch\wt\task-284`；沿用 `task/284`，沒有 checkout、新建分支或移除 worktree。
- 本輪目錄：`scratch/t014/run-20261107T011047Z/`，下文相對此目錄。
- 啟動前與收尾核對 `http://localhost:8787/api/state`：lease 名稱 `p24lab`、持有人 `chaoren0418-arch/lito-launcher-server#284`、到期 `2026-11-07T01:53:22.9467552Z`，與派工的秒級時間相符；本輪 DB 使用期間有效。未自行更動調度器 lease，交 RESULT 後由調度器處理。

### 成品與設定

來源只讀：`D:\lineage開發\3-lito開發\lito登入器\LiTo381-Launcher\app\server\LiTo381-SharedResources-candidate.jar`。普通複製到本輪根目錄，連同同目錄 `lib/` 的四個 JAR；沒有建立硬連結、複製 runs 或原始正式設定。

| 檔案 | SHA-256 |
|---|---|
| 正式 JAR，20,054,975 bytes | `E1498871C7BE6B287821C57EF5D95933DB1740D9B4D4A80F6C3465F21D99D498` |
| HikariCP-5.1.0.jar | `A47A6EE62379694EE52C30036F0931B72F9AEE2A801D590341ED82BD839E2134` |
| mysql-connector-j-8.0.33.jar | `E2A3B2FC726A1AC64E998585DB86B30FA8BF3F706195B78BB77C5F99BF877BD9` |
| protobuf-java-3.21.9.jar | `1B78B4A76A71512DEBFDFF8F8FC5AEF6BFD459F65758FECF7AFF245E6E6301E4` |
| slf4j-api-1.7.36.jar | `D3EF575E3E4979678DC01BF1DCCE51021493B4D11FB7F1BE8AD982877C16A1C0` |

正式 JAR 來源、副本雜湊相同，收尾再核對來源仍相同。設定由 #83 的 `scratch/wt/task-83/scratch/t020c/isolated-test.properties` 讀取，再寫入本輪 `config/experiment.properties`；未讀取正式 `config/server.properties` 或舊 combat 設定。私密設定受 `scratch/` 忽略規則保護，未提交。

以反射呼叫**副本 JAR 的** `UnifiedConfiguration.validate(Properties)`，全部 22 個 KEYS 通過。以下為解碼後設定，兩個帳密欄位僅保留遮蔽標記，不能直接用此範例登入：

```properties
bind=127.0.0.1
game.bind=127.0.0.2
port=42083
server.name=LiTo83Exp
database.jdbc.base.url=jdbc:mysql://127.0.0.1:43324/
database.jdbc.options=?useUnicode=true&characterEncoding=UTF-8&serverTimezone=UTC&useSSL=false&allowPublicKeyRetrieval=true
database.user=<REDACTED>
database.password=<REDACTED>
database.static.database=p24_world_lito_exp
database.lito.database=p24_lito_lito_exp
weight.rate=1.0
capture.directory=../captures
battle.ai.enabled=false
battle.physical.hit.enabled=true
battle.natural.mp.regen.enabled=true
native.siege.roster.enforced=false
native.tournament.allowance=0
memory.initial=256m
memory.maximum=1g
cpu.limit=2
logs.directory=../logs
startup.timeout.seconds=120
```

`runtime/`、`captures/`、`logs/`、`tmp/` 都是本輪新建目錄；啟動前 runtime 項目數為 0，且收據表為空，沒有舊 journal 或 DB 收據可重播。JVM 使用 Microsoft OpenJDK 17.0.17，`-Xms256m -Xmx1g -XX:ActiveProcessorCount=2`，Java 暫存指向本輪 `tmp/`。

### DB 路由及前後筆數

透過 mysql-connector-j 8.0.33、#83 受限帳號，以唯讀 JDBC connection 核對：

- `@@port=43324`、`@@bind_address=127.0.0.1`。
- `@@server_uuid=e3443e40-d090-11f1-8040-4cedfb679789`。
- `@@datadir=D:\BinEngine\environments\p24\db-data\`。
- `p24_lito_lito_exp.lito_source_server` 僅一列：`source_id=1`、`character_database=p24_accounts_lito_exp`、`enabled=1`。前後相同，未執行 UPDATE；對應來源埠為 42084。

| 查詢目標 | 啟動前 | 退出後 | 差異 |
|---|---:|---:|---:|
| `p24_accounts_lito_exp.character_lito` | 2 | 2 | 0 |
| `p24_lito_lito_exp.lito_battle_settlement` | 0 | 0 | 0 |

筆數以各表 `SELECT COUNT(*)` 取得，不查帳號密碼或角色內容；筆數相同不代表逐列 checksum 相同。本輪沒有對原 P24 三庫、tw-bin 測試資料或正式 DB 發出查詢／修改。

### 實際啟動與完整錯誤

2026-11-07 **01:14:12–01:14:14 UTC** 執行 `java -jar`，主控台已在 120 秒上限內以例外結束，工具觀察退出碼 **1**。沒有出現啟動完成的 login-listen 訊息。完整合併 stdout／stderr（含 SLF4J 訊息、例外及全部堆疊）在 [server-console.txt](issue-284/server-console.txt)；原始 Windows CP950 位元組保留於本輪 `evidence/server-console.txt`，公開檔僅轉為 UTF-8／LF 換行，未截短。

核心錯誤：

```text
java.lang.IllegalStateException: Cannot load LiTo room policy or qualified resource tables; inspect the nested database error
Caused by: java.sql.SQLSyntaxErrorException: SELECT command denied to user 'lito83_exp'@'127.0.0.1' for table '角色_智力成長'
    at local.lito381.server.NativeProcHealBonus.read(NativeProcHealBonus.java:19)
    at local.lito381.server.NativeProcHealBonus.load(NativeProcHealBonus.java:27)
```

沒有對應大廳 PNG；原因是伺服器前置失敗，不以其他畫面代替。也未重跑伺服器來驗證未授權的權限修補。

### 執行器問題與正常收尾

- 第一次 P24Lab 啟動前的 `Get-CimInstance` 被沙箱拒絕，當次尚未啟動 DB；隨後一次前置 JDBC 查詢因此 connection refused。兩者是執行器前置失敗，不能當成 JAR 啟動結果。使用已授權的沙箱外執行後才成功啟動 P24Lab。
- P24Lab 的 mysqld wrapper PID 為 6432，實際服務 PID 為 7368。啟動腳本原以 wrapper PID 等待 listener，曾誤報 timeout；完整 DB 主控台已有 ready 訊息，後續 JDBC 身分核對通過才啟動 Java。沒有因此重啟另一份 DB。
- P24 主控台另有既有元件 DLL／ICU 目錄警告，但 DB 已 ready；本輪未修補這些環境檔案。
- 關機守門第一次因 JSON 序列化將 StartTime 截為毫秒而拒絕；僅修正本輪輔助腳本，採同路徑、同 PID、啟動時間差小於 1 毫秒核對。重新核對線上身分及無其他客戶端後，使用既有管理入口執行正常 `SHUTDOWN`。
- **01:14:55.3198421Z**：SHUTDOWN exit=0，所持 wrapper 已退出；43324、42061、42083、42084 均無監聽。沒有強殺其他程序，沒有撤權、刪庫或刪檔。
- CMD 初版的 `echo 1>file` 被當成輸出描述符，退出碼文字檔留下 `ECHO is off.`；不把該檔當退出碼證據。退出碼 1 來自執行工具；交付腳本已改成 `(echo %SERVER_EXIT%)>file`，未重跑正式 JAR。
- 交付前已核對主控台 CP950→UTF-8 全文一致、機密字串未進入四份交付檔、PowerShell 語法解析與 CMD 缺參數守門通過。交付腳本新增的設定檢查僅靜態驗證，未在修正後重啟 JAR；沒有產品程式碼變更，未做 Maven 建置。

## 可重跑入口與產出路徑

- [run-experiment.cmd](issue-284/run-experiment.cmd)：無帳密，接收全新 run 目錄與 Java 17 執行檔，將 stdout／stderr 合併寫入 `evidence/server-console.txt`。
- [verify-run.ps1](issue-284/verify-run.ps1)：同目錄配套，檢查 #284 有效 lease、正式 JAR 雜湊、22 個設定鍵、實驗端點與目錄、空 runtime、近期 DB 前置收據；拒絕覆蓋既有嘗試。
- [server-console.txt](issue-284/server-console.txt)：完整正式 JAR 錯誤。
- 本報告：`docs/reports/issue-284.md`。
- 本機忽略區：`scratch/t014/ExperimentProbe.java`／`target/` 是設定驗證與唯讀 JDBC helper；`start-p24.ps1`／`stop-p24.ps1` 是本次啟停輔助；`run-20261107T011047Z/evidence/` 保留 lease、lib 雜湊、遮蔽設定、DB 前後收據、P24 完整 stdout／stderr、停機收據及原始 Java 輸出。私密設定只留本機。

下一輪先由調度器取得有效 lease，按既有流程準備 P24Lab；建立**另一個全新** `scratch/t014/run-<時間>/`，複製同 SHA 正式 JAR／lib，建立 config、runtime、captures、logs、tmp、evidence。使用 #83 私密交接填滿上列設定，不重用本輪 runtime。可用保留的 `ExperimentProbe prepare <新run> <#83私密設定>` 產生設定、`ExperimentProbe before <新run>` 取得唯讀收據（classpath 為 helper target、正式 JAR、lib/*），將 before 輸出存為 `evidence/db-before.txt`；前置收據須在五分鐘內且符合本報告身分與路由。

```cmd
docs\reports\issue-284\run-experiment.cmd "<新run絕對路徑>" "C:\Program Files\Microsoft\jdk-17.0.17.10-hotspot\bin\java.exe"
```

入口是同步執行，**不會自動啟停 DB，也不內建自動 120 秒強殺**。操作者需監看完整輸出，120 秒仍未完成則正常 Ctrl+C 並記錄超時；不能只設定 `startup.timeout.seconds` 就宣稱 JVM 有超時機制。本輪例外在約兩秒內退出，不涉及超時關閉。成功才進行另有授權的登入／大廳驗收，最後依既有流程正常關閉本輪程序。

## 推測

- 依 `NativeProcHealBonus.load` 的程式碼與本輪設定，被拒絕的表應位於 `p24_world_lito_exp`；SQL 錯誤本身只列出表名。#83 公開權限清單含精神／體質／力量／敏捷成長，但未列智力成長，可能是正式 JAR 與前置授權清單的缺口。未進一步查 grants 或擴權來驗證此推測。
- 此錯誤是本輪第一個阻塞，不表示補單表 SELECT 後就一定可以啟動。

## 待驗證／未驗證什麼

- 正式 JAR 完整啟動、監聽實驗登入／來源埠、登入器連線、測試帳號登入到大廳及 PNG 均未完成。
- map 12000、其他資源／欄位／權限及 M1 T011 全部尚未驗證。
- 未逐列比對角色戰績內容或原 P24 庫 checksum；本輪只有指定兩表筆數與路由前後核對。

## 給下一棒的提示

1. **Claude** 審查本輪完整例外與正式 JAR 的 `NativeProcHealBonus` 讀取需求，裁定前置資料／權限清單如何處理；不要直接讓執行器擴充 `lito83_exp` 權限或用 schema.* 代替。
2. **調度器／本機執行者** 在裁定完成後重派持 lease 的 T014，使用新 run 目錄重跑；保留本輪失敗證據。T011 #285 仍受本項阻塞，不能當作已進大廳。
3. 若有後續授權的 DB 變更，依 P24Lab 規則先備份並另記前後差異；本輪沒有提供或執行 GRANT、DDL、資料複製修復。
