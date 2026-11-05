# Issue #247：#83 正式 3306 操作唯讀稽核

結論：**通過**（指定範圍的稽核完成）。現存腳本、匯出／匯入狀態與調度器日誌相互吻合：正式 3306 有三個資料庫的 dump 嘗試，兩個成功、一個不存在而跳過；未見對 3306 執行獨立 SQL 或寫入正式 DB 的跡象。這是本機證據的結論，不是正式 DB 全時段零寫入的保證。

## 做了什麼／怎麼驗證

本輪僅讀取指定日誌、腳本與狀態檔，並執行一次 `netstat -ano | findstr :3307`。未連任何 DB、未執行 SQL、未執行匯出／匯入腳本、未啟停服務或程序、未刪檔，未接觸 P24Lab（43324／42061）、機密設定、封存目錄、正式客戶端或 dump 內容。

稽核期間為 2026-11-06（UTC+8）；歷史目標為 2026-11-05 20:05–20:26 UTC。證據位置：

- 調度器日誌：`D:\dev-orchestrator\data\logs\chaoren0418-arch_lito-launcher-server-83-1793909123.log`，共 10,547 行；末次修改為 2026-11-05 20:26:21 UTC。下文「日誌行」均指此檔。
- 前輪實際工作目錄：`D:\lineage開發\3-lito開發\scratch\wt\task-83`（日誌第 3 行）。下文腳本／狀態檔相對路徑均以此為基準。
- 已讀 `scratch/t020c/export-production.ps1`、`export-status.json`、`import-status.json`；另讀 `LocalExportPrompt.cs`、`exp-client.ps1`、`import-dumps.ps1`，確認匯出入口及匯入目的端。
- 本輪 `task-247` 無 `scratch/t020c`；原始專案根目錄的 `scratch/t020c` 未找到指定名稱的匯出腳本／狀態檔。實體證據位於上述前輪 worktree，未搬移或改寫。

## 已驗證

### 1. 憑證讀取檔案

`D:\lineage開發\3-lito開發\scratch\wt\task-83\scratch\t020c\production-once.cnf`

### 2. 對 3306 的 dump 指令與結果

`export-production.ps1` 第 4、68–75 行指定上述 option 檔、MySQL 5.5 的 dump 工具及 `127.0.0.1:3306`。以下是供稽核閱讀的參數模板，非本輪執行指令；`<option檔>` 為上一節路徑，`<輸出檔>` 為前輪 worktree 的 `scratch/t020c/dump/<庫名>.sql`：

```text
"C:\Program Files\MySQL\MySQL Server 5.5\bin\mysqldump.exe" --defaults-extra-file="<option檔>" -h 127.0.0.1 -P 3306 --single-transaction --skip-lock-tables --no-tablespaces --routines --triggers --events --hex-blob --default-character-set=utf8mb4 --databases <庫名> --result-file="<輸出檔>"
```

| 順序／庫名 | export-status 結果 | Exit code | 匯出大小 | 移除參數 |
|---|---|---:|---:|---|
| 1. `380_db` | `complete` | 0 | 5,782,453 bytes | 無 |
| 2. `380_lito` | `complete` | 0 | 211,899 bytes | 無 |
| 3. `lito_resource` | `absent-skipped` | 2 | 不列成功產物 | 無 |

`export-status.json` 第 1–33 行與日誌第 4639–4671 行一致。三筆 `omitted` 都是空陣列，因此未見移除 `--no-tablespaces` 的相容性重試。腳本第 80–88 行雖有此重試分支，不能把分支存在當成已執行。第三筆的跳過條件為錯誤文字含 `Unknown database`（腳本第 89–92 行）。

日誌第 221、326 行另有 `mysqldump.exe --no-defaults --version` 及版本回覆 `10.13 Distrib 5.5.22, for Win64 (x86)`；這是本機工具版本查詢，不是對 3306 執行 SQL。早期數次啟動匯出介面仍停在 `waiting-local-credentials`，不能算成額外成功匯出。

**非 dump SQL：**在本輪目標日誌與匯出腳本中，未見對 3306 執行獨立的 SQL client 查詢、DDL、DML、授權或匯入指令。日誌開頭讀出的舊報告、原始碼及 SQL 檔文字不等於當輪執行 SQL。dump 工具內部實際送出的協定／SQL 序列沒有逐句追蹤，故不宣稱「3306 完全沒有 SQL」。

### 3. 正式 DB 寫入跡象

**未見寫入正式 DB 的跡象。**可追溯的正式端操作是上述 dump，結果寫入本機輸出檔；兩筆成功匯出的結果有日誌與狀態檔交叉支持。

日誌確實包含後續匯入、建表、測試資料、授權與 `ALTER TABLE` 等修改，但不能僅以庫名相同就判為正式端寫入：

- `exp-client.ps1` 第 8 行將 SQL client 固定為 TCP、`127.0.0.1`、`-P 3307`；第 41–46 行檢查連接埠、隔離 datadir、loopback 綁定及事件排程器狀態。
- `import-dumps.ps1` 第 2–3、10–15 行透過該 helper 驗證目的端並匯入。日誌第 5150 行記錄啟動此匯入腳本，`import-status.json` 第 1–16 行記錄兩庫均為 `imported`／exit code 0，且與匯出狀態所記錄的檔案 SHA-256 一致。
- 日誌第 3014 行的直接 SQL client 也明列 `-P 3307`。後續 `Invoke-ExpSql` 修改使用上述固定 3307 的 helper；例如日誌第 8699–8722 行記錄的欄位查詢、`ALTER TABLE` 與筆數檢查。

### 4. 3307 唯讀觀察

觀察時間：**2026-11-05 21:37:43 UTC（2026-11-06 05:37:43 UTC+8）**。

```text
netstat -ano | findstr :3307
  TCP    127.0.0.1:3307         0.0.0.0:0              LISTENING       7632
```

當下有 PID 7632 在 loopback 3307 監聽；本輪沒有停止它，也沒有連線確認其服務身分。此觀察不代表歷史時段的程序狀態。

## 推測／未驗證什麼

- 腳本第 4、72 行可確認 dump 指定讀取第一節的 option 檔；該檔內容未讀取，也未查讀任何其他機密設定。
- `LocalExportPrompt.cs` 第 38–46 行顯示本機表單產生 option 檔並以 `-UseLocalOption` 啟動匯出；日誌第 4674 行聲稱已收到本機輸入。這支持表單輸入流程，但不能獨立證明實際由誰輸入，或輸入者是否另從其他檔案取得內容。**現有證據不足以另指認上游憑證來源檔案**，不推定讀過正式伺服器設定。
- 沒有 DB 端 general log／audit log／binlog 或操作前後資料比對，因此不能排除日誌外的其他程序、其他操作者或未記錄行為；也未核實 dump 隱含讀取的其他預設選項。本文「未見」限於所列證據。
- 匯出與匯入雜湊一致性來自兩份狀態檔；本輪未讀取 dump 重算其雜湊。狀態檔記錄成功不等於本輪重跑驗證。

## 證據指紋

以下 SHA-256 為本輪唯讀計算，供下一棒辨識相同證據；不含憑證檔指紋或內容。

| 檔案 | SHA-256 |
|---|---|
| 上述調度器日誌 | `2889B40AD8456B4AF8BF8B8B56AA0DC9D1EFAA55835472C40C98DD85D6489F2D` |
| `scratch/t020c/export-production.ps1` | `4584ED382C87E267AC013FAAFA6D6588A794A36ECAA745A1ADB6DBA1DD823333` |
| `scratch/t020c/export-status.json` | `CE5B154D938AF6823B3BB4EF5925B96710DE1B31973C476D25553230F85844FF` |
| `scratch/t020c/import-status.json` | `96F0E510863456CBA5C4DB7B61A89E390ABE7E7336DBFD2A1BC4AE1CAD7C40C9` |

## 產出路徑／給下一棒的提示

唯一交付檔案：`docs/reports/issue-247.md`。由 Claude／調度器審查本報告；引用時請保留「未見寫入跡象」及證據限制。3307 的停止由另行授權的 dot 任務處理，不屬本輪稽核或交付動作。
