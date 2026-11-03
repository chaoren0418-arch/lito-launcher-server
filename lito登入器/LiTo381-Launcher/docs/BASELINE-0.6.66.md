# 0.6.66 聯合基準與清理規則

依使用者指定，0.6.66 為目前最新基準。遊戲正式檔未因本次整理更換。

## 唯一現行回復入口

- 登入器 `backups/baseline-0.6.66.zip`：`launcher/` 與 `server/` 兩個專案，含 MANIFEST.json 逐檔 SHA-256。
- `docs/baseline-0.6.66.json`：壓縮檔雜湊、清理清單雜湊與保留檔案資訊。
- `tools/backup_current.py`、`tools/restore_current.py`、伺服器 `tools/Verify-ProjectLayout.py` 均使用 0.6.66。
- 備份包含本機資料庫連線設定，不應公開上傳。
- 不包含外部 `0-客戶端`、SQL 資料庫、執行場次或戰績結算 WAL。這些資料須按各自的資料備份流程處理。
- 還原工具預設只驗證；指定全新目錄才解壓，不覆蓋正在使用的專案或玩家資料。

## 正式建置依賴

正式 JAR 的 SHA-256：`6928a147df1e784304b9080632c56121b173bf60b172023ebd263e82fd448628`。
伺服器現行增量建置入口：`tools/resource-database/build.py`。
固定輸入已移至 `tools/resource-database/inputs/base-0.6.61.jar`，與可再生成的 `build/` 分開；缺少輸入會立即停止，不會誤拿現版 JAR 當舊版基底。
此固定輸入不是供玩家啟動的舊版本，必須保留。其他歷史修補工具的 base.jar、原生測試 DLL 與必要 helper 也保留，不按副檔名一律刪除。

## 清理範圍

以 `baseline-0.6.66-cleanup-plan.json` 列出的精確路徑及雜湊為準：舊 0.6.60 完整基準、已被新基準取代的部署回復 ZIP、.NET/Python 暫存、Java 生成類別與候選 JAR、可再生成的反組譯輸出，以及已有已驗證備份的 0.6.63 遷移暫存。
清理前逐一驗證新備份、來源雜湊、範圍及連結；不使用遞迴刪除。結果寫入 `baseline-0.6.66-cleanup-result.json`。

## 保留資料

- `backups/0.6.26-before-shared-resources-20261011/`：先前明確保留的共用資源改造前回復點。
- `backups/history/through-0.6.60-evidence.zip` 與 `0.6.61-0.6.66-evidence.zip`：歷史證據、舊備份清單與獨有文字／來源版本；不再重複保留過期執行檔。
- 伺服器 `backups/history/`、`backups/resource-database-before-0.6.63.zip`：原始開發與資料表遷移前資料。
- 伺服器 `runtime/battle-settlements/`：結算重試／去重收據，禁止清理或用程式基準覆蓋。
- 現場保留最新兩場及未解問題紀錄；已完成的舊場次移至上層 `docs/history/retired-references-and-runs-20261014.zip`，原路徑及內容雜湊保留於索引。

日後修版請建立新版本基準；不要重寫本次已封存的壓縮檔或把歷史工具當成現行發布入口。

後續目錄整理不重寫本基準。現行文件與維護工具的變更另存小型補充包，詳見 [共同維護規範](PROJECT-MAINTENANCE.zh-TW.md)。
