# Issue #273：task/83 隔離 MariaDB 停機觀察交付

## 結論與範圍

第三次派工的報告修正完成，待主控審查；停機有直接證據，歷史監聽對照及覆寫刪除依任務擁有者核准的驗收例外處理，未轉列為已驗證。本報告整理同一執行會話於 **2026-11-06 02:11–02:13 UTC** 取得的工具結果，不是文件交付輪重新查詢的即時狀態。

對應[第三次派工](https://github.com/chaoren0418-arch/lito-launcher-server/issues/273#issuecomment-6675490431)、[審查意見](https://github.com/chaoren0418-arch/lito-launcher-server/issues/273#issuecomment-6675445469)及[主控驗收例外](https://github.com/chaoren0418-arch/lito-launcher-server/issues/273#issuecomment-6675470711)。修正前 Issue 為 open／runner:dot／task:running；最新派工後未見同輪 RESULT 或其他接手留言。本會話沿用唯一登記的 task/273 隔離 worktree，工作樹乾淨、本機與遠端均為 2de2a0c5128d354c8eeb34f13bf097007365b992，未發現並行改動；未列舉程序來判斷其他工作者。

本輪只修改本報告及發布摘要，已讀適用 AGENTS.md／CLAUDE.md，所查 worktree／專案及父層 .agents/skills 均不存在。未重新列舉、啟停程序，未連任何 DB，未讀取憑證、搜尋副本、修改設定或安全權限，也未執行驗收。未讀改 docs/REPORT.md。

## 歷史證據定位與限制

下列識別碼是本會話既有工具回傳的 chunk_id，可供具會話存取權的審查者定位；不是公開可下載的紀錄，也不是本輪新實測。必要結果已在本報告轉錄，未另行讀取受保護會話檔。相對路徑以 task/83 工作樹為根；命令只作歷史說明，本輪均未執行。

| 識別碼／時間（UTC） | 歷史命令或來源 | 必要結果與限制 |
| --- | --- | --- |
| E1：`aeac4e`；2026-11-06 02:11:13.0621409Z | `Get-Date -AsUTC -Format o`、`Get-Process -Id 7632` 的 Id／ProcessName／Path／StartTime、`netstat -ano -p tcp` 篩選目標埠、指定檔案的 `Test-Path -LiteralPath` | PID 7632 為 mariadbd，建立時間 2026-11-05 20:17:12.7506513Z，目標監聽一致；指定憑證路徑 `Exists=false`。時間是該批命令起始時間，不是每一子命令個別時間戳 |
| E2：`d7bae8`；E1 之後、E4 之前，無獨立時間戳 | `Get-CimInstance Win32_Process -Filter 'ProcessId=7632'`，僅輸出身分選項與建立時間 | 執行檔、建立時間及 defaults-file 均屬 task/83 既有實例；未公開完整命令列 |
| E3：`834abe`；E1 之後、E4 之前，無獨立時間戳 | `Select-String` 篩選 `scratch/t020c/mysql-exp/my.ini` 的 datadir／port／bind-address／pid-file／log-error；既有 pid 檔及日誌摘要 | 指向 task/83 的資料目錄與預期埠；只保留非秘密身分資訊。pid 檔讀值另在 E1 為 7632 |
| E4：`58253b`；約 02:13:06Z（由關機日誌對應，工具本身無獨立時間戳） | Python ctypes：核對原程序路徑／建立時間，`OpenEventW(2, False, 'MySQLShutdown7632')`、`SetEvent`、`WaitForSingleObject` | `signal_sent=true`；關閉請求已送出，沒有強制終止 |
| E5：`5e251c`；`CheckedUtc=2026-11-06T02:13:09.4433126Z` | 同一執行 session 59822 的後續工具輸出；`GetExitCodeProcess`、`Get-Process`、`netstat`、雜湊比較及 `Test-Path` | `exited=true`、`exit_code=0`、`TargetPidPresent=false`、`Port3307Listeners=[]`、`DumpHashesUnchanged=true`、`DatadirExists=true`、`Port3306ListenerSnapshotUnchanged=true` |

### 本次關機日誌與官方機制證據分開

本次執行證據為 `scratch/t020c/mysql-exp/mysql-exp.err` **第 103–110 行**：此定位由[既有審查](https://github.com/chaoren0418-arch/lito-launcher-server/issues/273#issuecomment-6675445469)核對，本輪未重讀檔案。E5 當時以 `Get-Content ... -Tail 20` 篩出以下事件，與該定位相符：

- 2026-11-06 10:13:06（UTC+8）：Normal shutdown。
- 10:13:07：InnoDB Starting shutdown。
- 10:13:09：InnoDB Shutdown completed；MariaDB Shutdown complete。

官方原始碼只證明正常關閉機制；它本身不能證明本機曾送出事件或關機成功。

## 已驗證：歷史停機身分與方法

| 核對項目 | 當時直接觀察 |
| --- | --- |
| 程序 | PID 7632，映像 mariadbd.exe |
| 啟動時間 | 2026-11-05 20:17:12 UTC，與 task/83 前輪紀錄吻合；執行前再次檢查建立時間，避免 PID 重用 |
| 執行檔歸屬 | 位於 task/83 的 mysql-exp/MariaDB 10.11.15 套件內 |
| 設定與資料目錄歸屬 | 命令列的 defaults-file 指向 task/83 的 mysql-exp/my.ini；該檔非秘密欄位指定 task/83 的 mysql-exp/data、port 3307 及 bind-address 127.0.0.1 |
| 關閉前監聽 | 127.0.0.1:3307，LISTENING，PID 7632 |
| 交叉核對 | pid 檔為 7632；既有啟動日誌記錄 MariaDB 10.11.15 在 3307 ready for connections |

關閉方式為開啟程序既有的 Windows `MySQLShutdown7632` 事件並送出一次通知，等候原程序正常退出。發送前再次核對執行檔與建立時間，持有原程序 handle；沒有建立替代事件、強制終止、讀取 DB 憑證或建立 DB 連線。

此事件是該版本的原生正常關閉入口：MariaDB [handle_connections_win.cc 第 517–527 行](https://github.com/MariaDB/server/blob/mariadb-10.11.15/sql/handle_connections_win.cc#L517-L527) 建立依 PID 命名的事件。實際事件送出與退出結果分別由 E4／E5 支持：`signal_sent=true`，隨後 `exited=true`、`exit_code=0`；本次正常關機的檔案證據另見上節日誌。

## 已驗證：02:13 UTC 的結果與保留狀態

驗證時間為 **2026-11-06 02:13:09.4433126 UTC**。

| 項目 | 結果 | 證據限制 |
| --- | --- | --- |
| PID 7632 | 已不存在 | 是當時的程序查詢結果，不代表其後不可能被重用 |
| 3307 | 無 LISTENING 項目 | 是關閉後當時的 netstat 結果，不是本文件輪的新查詢 |
| MariaDB 日誌 | Normal shutdown → InnoDB Shutdown completed → Shutdown complete | 日誌本地時間 10:13:06–10:13:09（UTC+8），對應 02:13:06–02:13:09 UTC |
| dump | 兩份既有 dump 的關閉前後 SHA-256 比較相同 | 未刪除、搬移或改寫 dump；不公開內容 |
| datadir | 關閉後目錄仍存在 | 未額外修改、刪除或搬移；正常 shutdown 的 flush／內部狀態落盤已發生，不能宣稱所有資料檔逐位元不變 |

### dump 雜湊的命令、時間與保存限制

E4／E5 的同一批 PowerShell 執行先後兩次執行以下命令，結果分別存在記憶體變數 `$dumpBefore`／`$dumpAfter`：

```powershell
Get-FileHash -LiteralPath scratch/t020c/dump/380_db.sql,scratch/t020c/dump/380_lito.sql -Algorithm SHA256
```

第一次在關閉事件前，第二次在程序退出後、E5 的 `CheckedUtc` 之前，接著比較 `($dumpBefore.Hash -join ',') -eq ($dumpAfter.Hash -join ',')`。可定位的時間範圍為 E1 的 02:11:13Z 之後至 02:13:09.4433126Z；每次雜湊計算沒有單獨記錄開始／完成時間，不能捏造更精確秒數。

已保存的是 E5 的 `DumpHashesUnchanged=true`；兩次雜湊字串未在該停機批次輸出或獨立保存。因此可驗證本會話曾執行該命令且回傳比較相同，但公開報告不能提供可獨立重算的歷史前後雜湊組。舊的其他時間雜湊不能冒充本次前後值；本輪不重新讀 dump 或計算新雜湊。

## 已驗證：指定憑證檔的存在檢查

2026-11-06 02:11:13 UTC 對 Issue 指定的唯一 `production-once.cnf` 路徑執行存在檢查，結果為 **False**。僅核對指定路徑；沒有讀取內容、覆寫、刪除、複製或搜尋其他位置。

原檔大小未知，未取得大小，未覆寫、未讀取、未搜尋副本、未重建。因此本報告只直接確認「指定路徑當時不存在」，**不宣稱安全抹除完成**。

[主控驗收例外第 2 點](https://github.com/chaoren0418-arch/lito-launcher-server/issues/273#issuecomment-6675470711)陳述檔案已由前輪腳本刪除且未覆寫，並接受「先前不存在，覆寫無法執行」的驗收變更。這是任務擁有者提供的來源與驗收決定；本會話未直接驗證刪除時點、執行者或方法，不將其改寫為新增實測。

## 推測與待驗證

- 執行檔、建立時間、設定歸屬、pid 檔與監聽一致，支持目標就是 task/83 原實例；沒有使用帳密或線上 DB 查詢進一步取得資料庫身分。
- **3306／2000：無可供審查的執行前快照；依關機方式推定未受影響，列為推測。** 歷史指令以 `netstat -ano -p tcp` 擷取前後結果，僅對 3306 的 LISTENING 行比較；E5 留下 `Port3306ListenerSnapshotUnchanged=true`，但原始前後行未保存，無法獨立重建對照。2000 更沒有前後比較結果，維持未驗證。現在補查不能補成歷史證據。
- [主控驗收例外第 1 點](https://github.com/chaoren0418-arch/lito-launcher-server/issues/273#issuecomment-6675470711)已接受上述歷史快照缺口；這是驗收例外，不是實測證明正式服務不受影響。本次只向指定實例送事件、未連正式服務的操作範圍，支持推測但不取代快照。
- 原憑證檔大小與安全抹除歷程未驗證；路徑不存在不能證明不可恢復，也沒有檢查其他副本。
- 本報告不證明 #83 的隔離設定、實驗存取權或後續遊戲驗收已就緒。

## 給主控的交接

本次純報告修正沒有剩餘執行阻塞，請主控依已核准的例外及 E1–E5 來源限制送審。具會話權限的審查者可按 chunk_id 定位；若無權限或仍要求獨立封存的歷史輸出，應由主控決定是否接受現有證據，不越權讀取受保護記錄、不補造或重跑。未保存的原始監聽行、雜湊字串及憑證檔歷程仍不能獨立重建。

無須重做停機，不應為補報告重啟實例或處理不存在的檔案。本輪不合併、不關閉 Issue，不觸碰正式服務、P24Lab、原 BIN／客戶端或封存資源；#83 的 schema／帳號／設定工作仍 blocked。
