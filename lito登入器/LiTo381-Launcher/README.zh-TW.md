# LiTo 0.6.71（基準備份 0.6.66）

正式入口：桌面「LiTo 0.6.71 正式啟動器」或 `release/current/LiTo381Launcher.exe`。
遊戲載入 `D:/lineage開發/0-客戶端/LiTo.bin`，共用該目錄的資源；伺服器設定統一位於相鄰伺服器專案的 `config/server.properties`。選服、帳密、角色、大廳與戰鬥流程沿用目前版本。

0.6.71 新增 GM `.polyme 外型代號`（L1PolyMe、7200 秒），沿用 0.6.70 已實測的龍騎士／幻術師名單圖示、專用 `litoddraw.dll` 與世界資源接線，保留 BUFF 相容及八角色翻頁。圖示包位於 `app/resources/class-icons/`，原始客戶端 PAK／BIN 與來源資料庫未改動。職業圖示已通過使用者大廳／戰鬥室驗收；其他戰鬥技能項目不因本次發布一併宣告完成。[發布驗證](docs/maintenance/release-0.6.71.json)。

## 專案結構

| 目錄 | 用途 |
| --- | --- |
| `release/current/` | 唯一正式啟動器發布目錄，含執行相依檔 |
| `app/` | 原生啟動、正式伺服器核心、共用資源目錄；runs 為執行紀錄 |
| `src/` | 啟動器 C# 原始碼；bin、obj 是可重建暫存 |
| `native/` | LiTo 相容 DLL 原始碼與建置依賴 |
| `server-patches/`、`server-profiles/` | 保留的修補來源、固定歷史建置輸入與測試設定；不可直接發布舊版 |
| `tools/` | 現行建置、驗證、備份、還原與維護工具 |
| `docs/` | 驗證證據與維護說明；舊入口說明集中 `docs/history/` |
| `backups/` | 最新聯合基準、特別保留的 0.6.26 回復資料；歷史證據集中 `history/` |

## 建置與驗證

- `Build.ps1`：建置目前啟動器，檢查發布雜湊，清理 .NET 生成檔。
- `python -B -X utf8 tools/verify_current.py`：驗證正式伺服器、客戶端與資源接線。
- `python -B -X utf8 tools/backup_current.py`：驗證已封存的 0.6.66 基準；不覆寫既有備份。
- `python -B -X utf8 tools/restore_current.py`：驗證還原內容。加 `--extract-to 新目錄` 可解壓至獨立目錄，供人工核對。
- `Complete-Cleanup.ps1 -VerifyOnly`：驗證本次精確清理清單；不會掃描或刪除日後新產生的檔案。

## 基準與回復

最新聯合備份：`backups/baseline-0.6.66.zip`，同時包含登入器與伺服器的程式、設定、正式發布檔及固定建置輸入。
詳見 [基準與保留規則](docs/BASELINE-0.6.66.md)。原生移動鎖定修正證據位於伺服器 `docs/maintenance/movement-lock-0.6.66.md`；封存本版本不等同宣告所有遊戲功能均已完成驗收。

舊版完整入口說明保存在 `docs/history/readme-before-baseline-0.6.66.md`。歷史文件內的舊版本、雜湊及回復路徑僅供追溯，以本頁與 0.6.66 基準說明為準。

專案整理與紀錄保留統一依 [原生 LiTo 專案維護規範](docs/PROJECT-MAINTENANCE.zh-TW.md)；歷史資料可用登入器 `tools/project_hygiene.py` 按原路徑查詢與解壓。

0.6.67：Ctrl 強制攻擊意圖隨每次封包送出；詳見相鄰伺服器 `docs/maintenance/forced-combat-0.6.67.md`。尚待雙視窗手動驗收。
