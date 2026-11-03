# 歷史資源與維護索引

目前正式版本：**0.6.71**；封存遊戲基準：**0.6.66**。本頁提供現存資料的查找入口；舊文件內的版本、資料庫名稱與逐版 ZIP 路徑僅代表當時狀態，不可作為現行部署命令。

施工中候選的現行結果與未完成範圍見 [戰鬥技能主要進度表](maintenance/combat-continuation-current.zh-TW.md)；尚未部署，不改變本頁正式版本或封存基準。

最近發布：[0.6.71 L1PolyMe](maintenance/gm-polyme-0.6.71.md)；0.6.70 職業圖示發布收據位於相鄰登入器 docs/maintenance/release-0.6.70.json。

先前發布：[0.6.69 BUFF 相容與八角色分頁、驗收及精簡回復包](maintenance/buff-schema-and-character-pages-20261018.zh-TW.md)。

## 現存回復與歷史資料

| 位置 | 用途 |
| --- | --- |
| [戰鬥報告精簡前封存](history/combat-reports-before-simplification-20261019.zip) | 2026-10-19 三份原報告、原歷史索引與舊報告產生器；附原路徑及 SHA-256。現行進度以主要進度表為準 |
| 相鄰登入器 `backups/baseline-0.6.66.zip` | 兩個專案的封存遊戲基準；原始碼、發布檔、設定、固定建置輸入 |
| 相鄰登入器 `backups/0.6.26-before-shared-resources-20261011/` | 共用資源改造前特別保留的回復點 |
| `backups/resource-database-before-0.6.63.zip` | 原 24 張資源表的結構、資料與還原 SQL |
| `backups/history/server-development-evidence.zip` | 早期開發、設定、分析與驗收原文 |
| `backups/history/layout-before-20261012.zip` | 舊目錄、入口、設定、協定與主文件證據 |
| `backups/history/diagnostic-tools-20261012.zip` | 已退休的分析與診斷工具；需依原版本在隔離副本使用 |
| 相鄰登入器 `backups/history/through-0.6.60-evidence.zip`、`0.6.61-0.6.66-evidence.zip` | 舊版本獨有來源、文件與備份清單 |
| 登入器上層 `docs/history/retired-references-and-runs-20261014.zip` | 公開參考原始碼、完成的舊驗收與偵錯場次；原路徑索引、SHA-256 內容去重 |
| 登入器上層 `docs/history/maintenance-20261014.zip` | 本輪更新的規範、說明及維護工具補充包；不重寫遊戲基準 |
| `docs/history/readme-before-baseline-0.6.66.md` | 精簡前完整首頁與逐版敘述；歷史狀態不能覆蓋目前文件 |

逐版完整備份不再全部保留。0.6.35／0.6.60 舊完整 ZIP 及 `before-*.zip` 的獨有文字、來源與檔案清單已集中歷史包；現行回復請使用 0.6.66。封存資料可能含歷史連線設定，按本機備份管理，不公開上傳。

## 更新脈絡

| 階段 | 重點 |
| --- | --- |
| 0.6.26 | 保留共用資源改造前可運作的原生版本 |
| 0.6.27～0.6.35 | 共用 `0-客戶端`，技能與箭矢資源對應、道具說明及 GM 工具 |
| 0.6.36～0.6.42 | 先選服再登入、來源隔離、即時戰績與排行、單一設定、SQL 管理來源及 GM 權限 |
| 0.6.43～0.6.51 | 龍騎士／幻術師選角、技能指向、連擊與原生 BUFF 圖示 |
| 0.6.52～0.6.60 | 多開、房間同步、玩家移動／進出場與可重試結算 |
| 0.6.61～0.6.64 | SQL 測試房規則、原生資源接線、24 表精簡為 10 表、假人人數顯示 |
| 0.6.65～0.6.66 | 裝備切換狀態修正、移動校正回覆後釋放原生動作鎖定 |
| 2026-10-14 整理 | 0.6.66 聯合基準、分離固定輸入、精簡主文件、去重封存歷史來源及完成場次 |

個別修正的證據及限制請讀 `docs/maintenance/` 與 [VALIDATION.md](../VALIDATION.md)，本表不取代測試結果。

## 維護與回復

共同規範：[原生 LiTo 專案維護規範](../../../lito登入器/LiTo381-Launcher/docs/PROJECT-MAINTENANCE.zh-TW.md)。

登入器 `tools/project_hygiene.py find --match 關鍵字` 可找原路徑，`extract --match 關鍵字 --to 全新目錄` 可解壓指定資料。0.6.66 還原由登入器 `tools/restore_current.py` 驗證及解壓至全新目錄，禁止覆蓋結算 WAL 或 SQL 資料。

0.6.26 特別回復仍由 `tools/Restore-LegacyRuntime.py` 與登入器 `tools/restore_rollback26.py` 原配方管理。它是歷史相容性回復，不是目前啟動入口。
