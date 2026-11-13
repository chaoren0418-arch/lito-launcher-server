# Issue #413：08 重測前置

結論：部分通過（✅ 2／❌ 0／⚠️ 1）。執行日期：2026-11-14（Asia/Taipei）；證據時間採 UTC。分支：`task/413`。

## 已驗證

| 編號 | 結果 | 實際觀察（現象） | 重現步驟 | 證據路徑 |
|---|---|---|---|---|
| 1：實驗庫來源／profile | ⚠️ | lease 持有人及到期時間相符且有效；P24Lab 啟動、SELECT、關閉整組指令在建立程序前被自動核准審查以 `blocked by policy` 拒絕，沒有連 DB，沒有產生查證過的 TSV／profile。 | 核對 `/api/state` 的 `p24lab`，再送出證據中的完整指令；本輪只嘗試一次，拒絕後未改途徑重試。 | [lease.json](issue-413/lease.json)、[p24-command.txt](issue-413/p24-command.txt)、[execution-results.txt](issue-413/execution-results.txt) |
| 2：最小 GUI 放行 | ✅ | **可**：記憶體內 WinForms 視窗觸發 Shown，HWND=14025832，PID=19548，約 0.6 秒後關閉，指令 exit 0；未出現自動核准拒絕。 | 以 `exec_command` 的 PowerShell 行內指令建立表單，500 ms Timer 關閉並 Dispose；不開文件、不寫 GUI 設定。第一次 `.ps1 -File` 因系統腳本執行政策失敗，兩次結果均保留，沒有修改執行政策。 | [gui-command.txt](issue-413/gui-command.txt)、[gui-probe.json](issue-413/gui-probe.json)、[execution-results.txt](issue-413/execution-results.txt) |
| 3：客戶端基準快照 | ✅ | 唯讀完成 669 檔、9,607,667,880 bytes；無略過連結；DLL 符合預期，ScalePercent=200；兩個 log 均有列出。 | 列舉 `0-客戶端` 頂層所有檔案（含隱藏檔），遞迴列舉 `LiTo/`；逐檔串流 SHA-256，核對雜湊前後大小／修改時間一致；其他子目錄不遞迴。 | [client-baseline.tsv](issue-413/client-baseline.tsv)、[client-baseline-summary.json](issue-413/client-baseline-summary.json) |

快照時間：2026-11-13 22:04:47–22:06:17 UTC；這是逐檔快照，並非檔案系統原子快照。檔案修改時間、完整 SHA-256 都在 TSV。

產物檢查通過：[artifact-validation.json](issue-413/artifact-validation.json)；確認快照範圍、列數、總大小、SHA 格式、GUI 證據及清理結果，並在完成後再次唯讀核對 669 檔的大小與修改時間均未變動。JSON 證據統一為 UTF-8／LF，內容不變。

| 重點檔案 | 大小（bytes） | SHA-256／內容 |
|---|---:|---|
| `litoddraw.dll` | 269824 | `E47BD6C6EC9979C8747A523E4457133802D19F673C8DC1E67707ABFA96216A8A`，符合指定值 |
| `lito381-compat.ini` | 121 | `E8D601C1AF08383FA15296824A67BFA49DD62209C47206E4C6454A5A395E25FD`；`ScalePercent=200` |
| `lito381-compat.log` | 0 | `E3B0C44298FC1C149AFBF4C8996FB92427AE41E4649B934CA495991B7852B855` |
| `LiToError.txt` | 16095 | `02FB33565EF849FE1846FF035315A85CD231F455B7D996614C12E2DFCDC8395A` |

結束檢查：[process-cleanup.json](issue-413/process-cleanup.json) 顯示 P24Lab 程序 0、43324 listener 0、GUI probe PID 已不存在。沒有啟動登入器／遊戲／LiTo Server，沒有連正式 3306／2000，沒有 SQL 寫入或需要還原的資料變動。報告與暫存腳本僅寫本 worktree；原始專案與客戶端僅讀取。沒有修改程式碼，無需建置／程式測試；驗證採實際 GUI 回傳與快照完整性檢查。

## 推測

無法從一般 WinForms 放行推定 `LiTo381Launcher.exe --external-server-profile` 一定放行；核准可能依指令內容個別判定。P24Lab 拒絕只回 `blocked by policy`，未提供原因，不推定是哪個子步驟觸發。

## 待驗證／未完成

- `source-server.tsv`、`scratch/t014/profile/p24-experiment-42083.json` 與報告中的 profile 副本均**未產生**；不以範本或歷史值冒充 live SELECT。
- 登入器專用命令的核准、連線、大廳與遊戲畫面均未測，遵守本張不啟動登入器／遊戲的範圍。

## 給下一棒的提示

已將 #413 加上 `needs:orchestrator`。請調度器處理 **P24Lab 啟動／唯讀查詢／正常關閉命令**的自動核准拒絕；本輪 GUI 基礎能力已通過，不能把 DB 拒絕誤報成 GUI 拒絕。

下一個 resource 任務只補第 1 項：取得有效 `p24lab` lease，在 127.0.0.1:43324 啟動實驗 MySQL、核對 UUID／datadir，以任務指定 SELECT 匯出三欄，從現有範本依 live 結果填 `sources`（cp950 名稱 ≤63 bytes、無逗號），再正常關閉自有 PID。本 worktree 的 `scratch/issue-413/query_sources.py` 已備妥但未執行；不應將未執行腳本視為查詢證據。GUI 與本次基準快照不需重跑；真正重測 08 時另拍當時的前後快照。

本張已完成可獨立執行的兩項，因此依真機 RESULT 規則列「部分通過」，不列「一項都沒測」的阻塞。`docs/REPORT.md` 僅隨 `git merge origin/main` 快轉取得主線內容，本任務沒有編輯索引。
