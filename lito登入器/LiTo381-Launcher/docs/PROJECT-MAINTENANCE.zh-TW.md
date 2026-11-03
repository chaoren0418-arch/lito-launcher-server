# 原生 LiTo 專案維護規範

適用範圍：`lito登入器`、`lito伺服器`。最新遊戲基準為 0.6.66；現代重建版與外部共用客戶端有獨立生命週期，不能順手搬動或清理。

## 檔案分工

| 類型 | 唯一位置與處理方式 |
| --- | --- |
| 正式啟動器 | 登入器 `release/current/`；只保留一份現行發布目錄 |
| 正式伺服器核心 | 登入器 `app/server/LiTo381-SharedResources-candidate.jar`，由 deployment 雜湊驗證 |
| 遊戲外控 | 伺服器 `config/server.properties`；來源服與遊戲規則依資料庫設計管理 |
| 原始碼與固定輸入 | `src/`、`native/`、`lib/`、必要 `inputs/`；不可當暫存清理 |
| 可重建生成檔 | 指定 `build/`、`bin/`、`obj/`；通過驗證後可清理，勿混入固定輸入 |
| 執行紀錄 | 登入器 `app/runs`、`app/server/runs`、`app/gui-runs`；伺服器 `runtime`、`loginfo` |
| 整理後的證據 | 每次修正一份簡明驗證結果與必要封包／指令片段，放 `docs/maintenance`；大量原始資料放單一歷史封存檔 |
| 基準與遷移回復 | `backups/`；用途、版本、雜湊與還原方法必須有索引 |

## 紀錄保留與清理

1. 完成且無爭議的同類測試只保留最近一輪；客戶端／啟動器現場保留最近兩場，以及尚未解決的失敗場次。此規則是人工維護判準，不會自動按日期刪除。
2. 已被文件引用的原始證據，先集中封存並提供原路徑索引，再移除散落副本。不要把全部 stdout、記憶體 dump、反組譯全文反覆貼入 Markdown。
3. 可用的原始碼、來源清單、原生契約與封包證據必須可追溯。參考專案內容相同時，封存時按 SHA-256 合併內容，索引仍保留每個原路徑。
4. 通用暫存／log 清理必須先排除所有 `backups/`，即使其中存在 __pycache__ 也不能套用暫存規則。備份汰換必須另做有回復證據的清單。新基準確認後移除無獨有內容的逐版完整回復包；不為每次日誌清理再複製整份遊戲。0.6.26 特別回復點、資料庫遷移前備份與現行 0.6.66 基準仍保留。
5. 清理只處理已核對的精確檔案清單：先核對依賴、路徑、連結與雜湊，封存後逐內容驗證，再刪除。禁止沿 junction 或 symbolic link 遞迴清除；不刪執行中紀錄。
6. `runtime/battle-settlements/` 是戰績一致性資料，不是偵錯 log。不得因清理、基準還原或版本更新刪除。資料庫與 `0-客戶端` 不在本清理範圍內。
7. 重建版仍引用的 `舊版分析與驗收/runtime/LiTo.unpacked-oep-current.exe`、`ida-client-oep-v2/`、原生分析契約與必要工具保留原路徑。來源測試使用的 `public-source-audit-0.7.660/tw_tpai/` 也保留。
8. 遇到權限不足要記錄精確剩餘項目；不得為清快取而遞迴接管整個專案的擁有權或 ACL。

## 基準與現行說明的關係

`baseline-0.6.66.zip` 保持封存時內容與雜湊，不因文件更新重寫。現行 Markdown、維護工具與歷史索引可繼續整理，另存小型維護補充包。基準檢查仍核對所有正式程式、原始碼、設定與固定建置輸入；已移入歷史包的原始證據，須以雜湊相符的清理收據銜接。基準不是資料庫備份，也不是所有功能已完成的宣告。

## 工具

- `python -B -X utf8 tools/verify_current.py`：正式發布接線檢查。
- `python -B -X utf8 tools/backup_current.py`：封存基準與目前核心一致性檢查。
- `python -B -X utf8 tools/project_hygiene.py verify`：歷史封存內容及本次清理清單驗證。
- `python -B -X utf8 tools/project_hygiene.py find --match 關鍵字`：依原路徑尋找封存資料。
- `python -B -X utf8 tools/project_hygiene.py extract --match 關鍵字 --to 全新目錄`：解壓指定資料供檢查，不覆寫正式專案。
- `tools/Apply-ProjectHygiene.ps1 -VerifyOnly`：只驗證本次精確清單。日後新資料不在這份清單內，須重新盤點，不能擴大舊清理腳本的範圍。

## 文件維護

首頁只放目前版本、入口、結構與常用命令；驗證結果放 `VALIDATION.md`／`docs/maintenance`；協定支援與 SQL 寫入邊界各有獨立文件。歷史敘述集中索引，不再逐版累加至首頁。每次工作結束記錄實際測試、未解問題、保留理由與清理結果，不把推測寫成通過。
