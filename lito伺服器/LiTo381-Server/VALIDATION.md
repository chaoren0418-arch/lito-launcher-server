# 驗收現況

2026-10-23：正式 0.6.71 新增 L1PolyMe；34 項變身與既有傳送回歸通過。[驗證範圍](docs/maintenance/gm-polyme-0.6.71.md)。戰鬥同源化仍為候選，完整進度與待辦集中在[主要進度表](docs/maintenance/combat-continuation-current.zh-TW.md)，本頁只列最新驗收界線。

## 保留的既有驗收限制

| 項目 | 限制與證據 |
| --- | --- |
| 斷線結算 | 冪等與清理已有測試；不代表長時間壓力／災難復原與雙玩家人工斷線情境全驗收。見[結算證據](tools/durable-settlement/evidence.json)。 |
| Ctrl 強制攻擊 | 封包與伺服器測試通過；雙視窗人工操作仍須核對。[0.6.67 記錄](docs/maintenance/forced-combat-0.6.67.md) |
| GM 座標／Storm_Walk | 權限、座標與協定測試通過；原生滑鼠點選魔杖仍有人工驗收限制。[0.6.68 記錄](docs/maintenance/gm-travel-0.6.68.md) |

發布前必須在**同一最終候選 SHA**完成整合回歸、正式 SQL 啟動、關閉清理及原生介面驗證。此次候選續作未改動正式部署或來源資料庫。

較早逐階段結果（含 2026-10-20 各候選界線與已發布紀錄）原文保留於[歷史索引](docs/HISTORY.md)，不再重複貼入本頁。
