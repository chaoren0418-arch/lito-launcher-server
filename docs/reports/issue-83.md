# Issue #83 隔離 SQL 設定準備

## 結論

2026-11-06 第 2 次嘗試目前阻塞。已完成本機實例候選盤點、設定讀取路徑核對與清理腳本。管理連線在認證階段遭拒，隔離帳號、schema、測試資料與可用設定仍待建立。

本輪以使用者直接提供的選項 A 作為操作授權，依其六項限定操作執行。已向使用者詢問可用的本機管理憑證入口，等待回覆。

另已確認目前程式的設定與連線共用方式無法僅以 properties 達成資源唯讀帳號與實驗角色寫入帳號分流。此項需由主控安排程式支援後續處理。

## 工作位置與範圍

- 工作目錄為 `D:\lineage開發\3-lito開發\scratch\wt\task-83`。
- 沿用分支 `task/83`，檢查起點為 `295c19c14b06c73fdb4dc1d8e151dbd84e217bce`。
- 已讀根目錄 `AGENTS.md`、`docs/REPORT.md`、`TASKS.md`，並核對 GitHub #83、#84、#225 的需求。
- 檔案產出限於本 worktree 的指定報告與 `scratch/t020c/`。

## 已驗證

### 本機實例候選識別

透過 Windows 服務、程序與監聽埠的唯讀查詢取得下列結果。

| 欄位 | 觀察結果 |
| --- | --- |
| 連線候選 host | `127.0.0.1` |
| 連線候選 port | `3306` |
| 監聽位址 | `::` |
| 程序 PID | `4188` |
| 服務名稱／狀態 | `MySQL`／`Running` |
| 服務執行檔 | `C:\Program Files\MySQL\MySQL Server 5.5\bin\mysqld` |
| 服務參數指向的設定檔 | `C:\Program Files\MySQL\MySQL Server 5.5\my.ini`，僅由服務命令列取得路徑 |
| `@@server_uuid` | 待驗證，認證失敗 |
| `@@datadir` | 待驗證，認證失敗 |
| `@@version` | 待驗證，認證失敗，服務路徑名稱只能作為線索 |
| 三個既有 schema 是否在此實例 | 待驗證，認證失敗 |

另外觀察到 `127.0.0.1:43324` 的 MySQL 程序 PID `24452`，執行檔位於 `D:\BinEngine\environments\p24\mysql\bin\mysqld.exe`。本輪只盤點其程序資訊，尚無證據將它識別為選項 A 的目標。

已嘗試透過本機 MySQL client 以 TCP 連至 `127.0.0.1:3306`。認證回應為 `ERROR 1045 (28000)`，程序 exit code 為 `1`。管理帳號識別資訊從本報告省略。

已檢查標準 MySQL login-path 檔案是否存在、目前執行中的常見 DB 管理工具，以及 Windows 已存憑證的入口名稱。這些檢查未找到可用的 MySQL 管理憑證入口。GitHub CLI 的沙箱內認證錯誤，經沙箱外使用既有 keyring 登入後已排除。

### SQL 執行紀錄

本輪唯一交給 client 的查詢如下。client 在認證階段遭拒，SQL 尚未送達伺服器執行。

```sql
SELECT @@server_uuid, @@datadir, @@version;
```

| 項目 | 結果 |
| --- | --- |
| 成功執行 SQL 數 | 0 |
| `CREATE USER`／`GRANT` | 待執行 |
| `CREATE DATABASE`／複製結構 | 待執行 |
| 實驗帳號與角色 `INSERT` | 待執行 |
| ro 的 `SELECT COUNT(*)` | 待驗證 |
| ro 的 `INSERT` 拒絕測試 | 待驗證 |
| rw 權限範圍 | 待驗證 |

清理腳本內的 SQL 是未執行的交付內容，與本節執行紀錄分開管理。

### 設定與程式讀取路徑

以下路徑均以 `lito伺服器/LiTo381-Server/` 為根。

| 程式位置 | 已核對行為 | 對本任務的影響 |
| --- | --- | --- |
| `src/main/java/local/lito381/server/UnifiedConfiguration.java` 的 `KEYS`、`validate` | 接受單一 `database.user`／`database.password`，拒絕未知鍵，要求完整的統一設定鍵集合 | 自行新增 ro／rw 設定鍵會被拒絕 |
| 同檔 `aliases` | 將同一帳密同時寫入 `Login_LOGIN`／`Login`、`Password_LOGIN`／`Password` | 別名亦共用帳密 |
| 同檔 `sources`、`apply` | 在 `database.lito.database` 查詢 `lito_source_server`，且要求至少一個啟用來源 | 只複製結構後的空表不足以通過來源載入 |
| `src/main/java/local/lito381/server/ServerConfig.java:293-299,378-399` | `database.connection.properties` 是共用連線設定來源，另指定動態庫與靜態庫 | 有 schema 分類，仍需檢查實際連線與權限分流 |
| `src/main/java/local/lito381/server/ResourceDatabase.java:16,22-25` | 用 `database.user`／`database.password` 連 `database.lito.database`，讀 `lito_resource_release`，要求 `qualified_version='0.6.61'` | 改指向只有空結構的 `380_lito_exp` 將無法通過此資源檢查 |
| `src/main/java/local/lito381/server/SharedDatabase.java:41-51` | 來源 repository 與寫回 Route 都從同一份 `legacySqlProperties` 取得憑證 | 尚無獨立寫回憑證設定 |
| 同檔 `Route` 建構式及 `connection`，約 159-168 行 | JDBC URL 用來源 `database` 組成，帳密取 `Login_LOGIN`／`Password_LOGIN` | `database.lito.database` 的 controls 值未成為角色寫回連線的預設 schema |
| 同檔 `persist` | 在上述 Route 連線對未限定 schema 的 `character_lito` 執行寫入 | 僅修改 controls schema 無法把既有來源角色的寫入轉到實驗庫 |

另以 `javap` 唯讀檢查 `reference/base-0.6.71.jar` 的 `ResourceDatabase`，確認同樣讀取 `database.lito.database` 與單一帳密。未啟動伺服器。

## 推測與待驗證

- `127.0.0.1:3306` 是選項 A 的實例候選，仍須成功登入並核對三個 schema、datadir、版本及實例識別。
- 真實 `380_lito` 的表結構、帳號密碼格式、角色登入必需欄位尚待唯讀結構查詢確認，避免先產生無法登入的實驗資料。
- 空結構與測試帳號以外，來源目錄及必要資源資料的取得方式需配合程式分流設計。現有授權限定複製結構與建立少量實驗帳號角色，後續應沿用既有資源庫唯讀資料。
- #84 指定的 `8d26c9d7…` 候選與 #225 的最終 JAR，仍須分別確認設定讀取路徑。當前 worktree 缺少 `docs/reports/issue-31.md`，可由主控提供該分支的交接資料。
- 登入、大廳、戰鬥回歸及畫面驗收仍待各後續任務執行。

## 產出與清理方式

| 產出 | 狀態 |
| --- | --- |
| `docs/reports/issue-83.md` | 本報告 |
| `scratch/t020c/cleanup-exp-db.sql` | 已寫入，尚未執行，精確指定兩個 localhost 帳號與實驗 schema |
| `scratch/t020c/isolated-test.properties` | 待建立，需可用憑證與程式支援，該路徑已確認由 `.gitignore` 排除 |

本輪尚未建立 DB 物件。後續建立完成並結束實驗後，由獲授權的本機執行者先核對實例識別與物件歸屬，再以管理連線執行清理腳本。其內容如下。

```sql
DROP USER 'lito_exp_ro'@'localhost';
DROP USER 'lito_exp_rw'@'localhost';
DROP DATABASE `380_lito_exp`;
```

本輪未讀寫 `config/server.properties`、未修改既有資料庫、未複製玩家資料、未啟停 MySQL 或遊戲服務、未修改 `docs/REPORT.md`。

## 給下一棒的提示

1. 本機對話取得可用的管理憑證入口後，先執行實例識別與 schema 核對，將結果補入本報告。憑證留在本機，SQL 紀錄遮蔽密碼。若實際版本不支援 `@@server_uuid`，記錄原錯誤並取得可用的版本及 datadir 識別證據。
2. 主控安排 code 任務處理資源讀取與實驗帳號角色寫回的連線分流，涵蓋 `UnifiedConfiguration`、`ResourceDatabase`、`SharedDatabase.Route` 及其他共用連線消費端。維持 ro 僅有 SELECT、rw 僅可操作 `380_lito_exp.*` 的權限需求。
3. 本機執行者確認既有同名使用者與 schema 狀態後，依原授權建立帳號、複製結構、建立 1～2 組最小實驗帳號角色，逐條補記實際 SQL。若同名物件已存在，先核對歸屬與權限。
4. ro 除了指定的兩個資源庫，如確實需要額外 SELECT 範圍，先在本報告列出實際程式查詢依據。rw 維持 `SELECT, INSERT, UPDATE, DELETE ON 380_lito_exp.*`。
5. 以實際設定讀取入口驗證 properties、ro 的資源計數與實驗庫 INSERT 拒絕，再發布就緒留言並關閉 #83。#84 與 #225 繼續等待此前置。
