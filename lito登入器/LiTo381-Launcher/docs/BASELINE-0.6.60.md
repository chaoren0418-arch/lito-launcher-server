> 歷史記錄：現行基準已改為 0.6.66，舊完整 ZIP 已由新基準與歷史證據取代。請使用 [0.6.66 說明](BASELINE-0.6.66.md)。

# 0.6.60 聯合基準

現行正式版以 `release/current` 為唯一啟動入口，桌面使用「LiTo 0.6.60 正式啟動器」。本次整理不變更遊戲功能、DLL 或正式伺服器 JAR。

## 保存內容

- `backups/baseline-0.6.60.zip`：登入器與伺服器的程式、設定、正式發佈、SQL 遷移、原始碼及維護工具。ZIP 根目錄分成 `launcher/`、`server/`，`MANIFEST.json` 記錄每個檔案的 SHA-256。
- `backups/history/through-0.6.60-evidence.zip`：已移除逐版備份中的文字原始碼、文件及已結束的測試紀錄；不保留每版重複的執行環境。舊文件提及的逐版 rollback ZIP 已由此歷史封存及新基準取代。
- `backups/0.6.26-before-shared-resources-20261011/`：保留先前特別指定的舊客戶端離線回復資料。既有伺服器歷史封存仍保留。
- 測試工具需要的固定 `base.jar`、原生測試 DLL 與 helper 維持原位；它們是重建／驗證輸入，不是另一個正式發佈版本。

基準不含 SQL 資料庫快照，也不複製共用客戶端的完整遊戲資源；仍依 `D:/lineage開發/0-客戶端` 與已配置的 MySQL 來源運作。來源帳密與本機設定包含於本機備份，不應公開上傳。

## 驗證與還原

執行 `python tools/baseline_060.py verify`，核對兩份 ZIP 內容、雜湊、正式資源與設定。伺服器的 `tools/Verify-ProjectLayout.py` 也改為檢查此基準。修改設定後若與封存不符，驗證會如實報告差異，不會覆寫設定。

`python tools/restore_current.py` 僅驗證還原內容；加上 `--extract-to 絕對路徑`，可解出到尚不存在的獨立資料夾，供比對並還原 `launcher/`、`server/`。不直接覆寫目前專案、不回復資料庫、不刪除新產生的檔案。

伺服器 `runtime/battle-settlements` 為未完成戰績的補存資料，**不列入清理或程式還原**。不得用基準覆蓋它；0.6.60 收據表與 `last_result_id` 的 SQL 遷移也須保留。

清理採每個檔案的絕對路徑與雜湊驗證，拒絕符號連結及目錄連結。`Complete-Cleanup.ps1 -VerifyOnly` 僅驗證；不帶參數才執行已封存的清單。原有已完成的清理計畫不再重跑。
