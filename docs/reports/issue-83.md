# Issue #83 測試資料來源與前輪產物盤點

## 結論

2026-11-05 UTC，依重新派工 [6668529427](https://github.com/chaoren0418-arch/lito-launcher-server/issues/83#issuecomment-6668529427) 及本輪委派的較窄範圍，**部分唯讀盤點完成；隔離設定 blocked，尚未就緒**。本報告取代先前 943657d 的現況描述，舊版保留在 Git 歷史。

P24Lab 有既有 SQL 檔、datadir 與冷備份候選，不能宣稱「沒有測試資料」。但目前僅驗證檔案與程式依賴，未證明結構、內容版本、受限帳號及寫回隔離可用。未執行 #84／#225 的依賴驗收。

重要發現：前輪本機紀錄顯示已從正式 3306 匯出兩個庫並匯入 3307；本輪未執行這些操作。20:47:59Z 被動查詢仍見 127.0.0.1:3307 由 PID 7632 監聽，程序路徑與本 task/83 的 MariaDB 相符。這是前輪殘留，交主控處理，本輪不連線、不停止、不刪資料。

## 範圍與去重

- 工作目錄：`D:\lineage開發\3-lito開發\scratch\wt\task-83`；沿用 `task/83`。
- 起點及 GitHub 遠端 SHA：`943657d8c1556a8a41bca798da282434ce68b8e4`。
- 已讀工作根目錄及 task/83 的 AGENTS.md／CLAUDE.md、TASKS.md 相關 T020 項，以及 P24Lab 所屬 `D:\BinEngine\CLAUDE.md`。所查專案／父層 `.agents/skills` 不存在，未改記憶。
- Issue 為 open／runner:dot／task:running；最新派工後尚無本輪 RESULT。調度器有終止 PID 25000 的留言，本機 Get-Process 未找到該 PID。
- 未發現可識別的重複執行工作，但不能完整排除：Win32_Process 命令列盤點遭作業系統拒絕存取，未重試或提升權限。3307 殘留程序不等於另一代理仍在執行。
- 工作樹原有 `D scratch/t020c/cleanup-exp-db.sql`，本輪不復原、不暫存、不提交此刪除。
- 不讀寫正式設定、docs/REPORT.md、原 BIN／正式客戶端或封存目錄；不連正式 3306／2000；不建立帳號、GRANT、schema、角色或持續存取設定；不啟動 3307／遊戲；不刪資料、不還原備份、不重做孤兒紀錄清除。

## 已驗證：environments 資料候選

本次定位到 `D:\BinEngine\environments\p24`。以下日期為檔案系統 UTC 修改時間，並非已證實的資料抽取時間。

| 候選 | 日期／大小 | 來源與可用性 |
| --- | --- | --- |
| `sql/380_db.sql` | 2026-10-25 15:59:58Z；9,552,749 bytes | P24 現有檔；識別為 HardLink，只讀檔案資訊，未讀內容或追查另一連結，原始匯出來源待核實 |
| `sql/380_lito.sql` | 2026-10-25 15:59:58Z；271,713 bytes | 同上；不能僅以檔名判斷結構完整 |
| `sql/380_ac.sql`、`380_cross.sql`、`380_transit.sql` | 2026-10-25 | 另有帳號／跨服／轉移候選，僅盤點名稱；未輸出資料列 |
| `db-data/` | 目錄 2026-11-05 19:41:19Z | 現有 P24 datadir；存在 p24_world、p24_accounts、p24_lito、p24_cross、p24_transit |
| `backups/issue-23-current-20261105-040643/` | 2026-11-04 20:07:07Z | 備份候選，含 db-data、db-logs、mysql、server、manifest 等；未還原或重驗完整性 |
| `backups/issue-71-before-demo/db-data/` | 2026-11-05 19:18:21Z | 存在上述 p24_* 庫；既有 #71 報告稱為啟動前冷備份，893 檔 SHA-256 相符；本輪只核對目錄，未重驗 893 檔 |

既有備份來源證據：[tw-bin-engine #71 報告](https://github.com/chaoren0418-arch/tw-bin-engine/blob/c09cc75231248750eea9976d66ca7bb7dda35559/docs/reports/issue-71.md)。該歷史報告的實例身分及關閉紀錄不當成本輪線上驗證。

### 三個庫與必要表

| 需求 | 本輪靜態證據 | 缺口／限制 |
| --- | --- | --- |
| 380_db 資源／角色來源 | SQL 檔存在；p24_world 目錄有 npc、skills、weapon、armor、etcitem、mapids 等表檔 | p24_world 不見 character_lito；角色資料實際位於另一 p24_accounts 候選，不能直接用庫名替換判定相容 |
| 380_lito 與角色寫回 | p24_lito 有 lito_source_server、lito_battle_*、lito_room_policy 等表檔；p24_accounts 有 accounts、characters、character_lito、character_items、character_skills 表檔 | p24_lito 未見 character_lito；需核對來源路由與寫回位置。檔名存在不證明欄位、資料或存取權限 |
| lito_resource | 未見同名 SQL／datadir；p24_lito 有十個 lito_resource_* 表檔 | 不是已證實缺庫阻塞；目前 ResourceDatabase 使用 database.lito.database，不要求獨立 lito_resource schema |

已在本分支 `ResourceDatabase.java` 核對：資源表應為 `lito_resource_skill_value`、`buff`、`projectile`、`map`、`zone`、`portal`、`npc_identity`、`map_page`、`release`、`pending`（後九項均加相同前綴）。p24_lito 目錄有這十個表檔。

程式另要求 `lito_resource_release.release_id=1` 的 `qualified_version='0.6.61'`、剛好十個資源表、必需欄位及 qualified 資料；這些條件都未在線驗證。來源目錄 `lito_source_server` 與角色寫回 `character_lito` 仍需在同一套安全設定中核對，不能僅複製空表宣稱就緒。

### 版本、字元集與登入方式

- P24 的 mysql/bin/mysqld.exe 檔案版本為 8.0.40.0；這是二進位檔版本，未查詢 `@@version`。
- P24 mysql.ini 的非秘密欄位為 port 43324、bind-address 127.0.0.1、datadir 指向 environments/p24/db-data、character-set-server=utf8mb4、collation-server=utf8mb4_unicode_ci。
- 本分支 pom.xml 使用 mysql-connector-j 8.0.33。版本與字元集表面上可作候選；尚未驗證 JDBC options、各表 collation、字元往返及真實連線。
- 既有 private-root.cnf／private-game.cnf 入口存在；本輪未讀內容、未測登入，無法聲稱已具備可用的受限帳號。未要求、建立、傳送或配置新秘密。
- P24 本輪未啟動，未發出 SQL。被動 netstat 未見 43324／42061 監聽，因此無本輪啟動殘留。線上結構盤點保留未驗證。

## 已驗證：前輪 task/83 產物及風險

以下為本地既有檔案所記載的結果，不是本輪重跑，也不能單靠紀錄證明正式環境完全沒有其他變更。

| 產物 | 靜態核對結果 |
| --- | --- |
| `export-production.ps1` | 匯出命令明確指定 127.0.0.1:3306、mysqldump 與三個庫；未執行此腳本 |
| `export-status.json`，20:14:37Z | 380_db、380_lito 為 complete／exitCode 0；lito_resource 為 absent-skipped／exitCode 2 |
| `dump/380_db.sql` | 5,782,453 bytes；SHA-256 AF0C983356780F392795B7AA4A2CFC1F9C987706462CA2D116A93AA2E46334C8，重算與 status 一致；未輸出內容 |
| `dump/380_lito.sql` | 211,899 bytes；SHA-256 55242638AF8EEF517CA22D3526F4A6ECABA796C58D2F7694CCB58F9FB4504975，重算與 status 一致；未輸出內容 |
| `import-status.json`，20:18:45Z | 上述兩庫均 imported／exitCode 0，記錄的輸入 SHA 相符 |
| `mysql-exp/` | MariaDB 10.11.15 套件、data、start.cmd、stop.cmd、控制腳本及設定入口存在；秘密設定未讀取 |
| `isolated-test.properties`、`combat.properties`、`test-accounts-private.properties` | 已存在，與舊報告「待建立」不同；只查檔案資訊，不公開或使用憑證 |
| `test-seed-status.json` | 記錄來源 380_db、建立來源表、兩筆帳號及兩筆角色；未公開帳號／角色內容 |
| `compatibility-status.json`、`set-card-compatibility-status.json` | 前輪相容別名處理紀錄存在；未重跑 SQL／腳本 |
| `java-probe.log`，20:26:21Z | 記錄設定讀取、結構檢查與兩組測試登入 lookup 通過；最後失敗：`source map physical asset not packaged: 12000 -> 12000`。不是完整啟動或驗收通過 |

上述腳本、status、dump 雜湊與匯入紀錄共同支持「前輪曾對 3306 進行匯出，並建立／使用實驗資料」的判斷；原始授權及正式環境完整影響範圍仍須主控稽核。本輪不得把這兩份新匯出當作重新獲授權的測試來源，也未重用它們。

### 前輪殘留程序

- 被動 netstat：`127.0.0.1:3307 LISTENING PID 7632`。
- Get-Process：mariadbd，路徑位於本 worktree 的 `scratch/t020c/mysql-exp/mariadb-10.11.15-winx64/bin/mariadbd.exe`，啟動時間 2026-11-05 20:17:12Z。
- `mysql-exp/mysql-exp.pid` 內容亦為 7632。
- 未連線核對其線上 datadir、帳號、schema 或權限；未執行 stop.cmd。不能將「PID 25000 已終止」寫成「資料庫已停止」。
- 早先 Get-NetTCPConnection 沒有可用輸出，不能作為無監聽證據；以上以 netstat 與程序路徑交叉核對為準。

## 未驗證與下一步

1. **主控先處理前輪 3307 殘留及 3306 匯出稽核**：由明確獲授權的執行者核對 PID 7632 歸屬後安排正常關閉，保留證據；本輪無關閉／刪除授權，不執行清理。不得為稽核而再連正式 3306。
2. **資料缺口是相容性證據，不是宣稱三個庫全缺**：確認 P24 的 p24_world／p24_accounts／p24_lito 路由、必需欄位、資源版本及測試角色所需非私人資料。若補資料，使用既有授權且去識別的 P24／快照來源，不新增正式匯出步驟。
3. **帳號與隔離設定仍 blocked**：未確認可用的既有受限登入、SELECT 範圍及寫回隔離；新增 DB 帳號、GRANT、schema／副本、測試帳號角色或持續使用憑證均不在本輪委派範圍。主控應提供已備妥且可驗證的受限入口，或另取得具體目標及操作的必要授權後再派工。
4. **線上盤點待續派**：若沿用合法既有登入入口，先核對 43324 的身分再只查結構／版本；若需要新憑證／權限則停下回報。P24 若啟動，結束須正常關閉並核對 43324／42061。
5. **驗收缺口**：前輪 probe 的 map 12000 資產錯誤未解；本輪不重跑 probe、伺服器、遊戲或 #84／#225。主控需指定實際候選 JAR／來源版本，另行確認資料與 map 資產相容性。
6. 只提交本報告；dump、秘密設定、玩家資料、完整執行紀錄均不公開。Issue 保持 open，RESULT 明確標示 blocked，不發布「隔離設定已就緒」。

本輪沒有資料庫操作遭核准系統拒絕，因為未提出 DB 操作；程序命令列查詢是作業系統存取拒絕。Git 遠端查詢曾遇 executor transport 中斷，未重開工作環境，改以 GitHub API 確認遠端 SHA。此限制與任務仍未獲准的隔離建置範圍分開記錄。
