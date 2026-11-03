# 0.6.64 測試房假人人數

修正大廳 S_CC 房間列表的新增列與人數更新欄位，統一採用與房間名單相同的 ArenaBot 顯示條件：SQL 啟用假人、預設死亡競賽測試房、實際參戰人數少於 2。

| 狀態 | 顯示人數 |
| --- | ---: |
| 空測試房，ArenaBot 已列入名單 | 1 |
| 1 真人＋ArenaBot | 2 |
| 2 真人，ArenaBot 退出 | 2 |
| 假人停用的空測試房 | 0 |

只修正 NativeRoomDirectoryRow 的顯示值；不修改 GameHub 的真人數、容量、開戰門檻、觀戰計數方式或結算。玩家自建房不加 ArenaBot。測試房仍不保存勝敗。

驗證：實際加密封包與原生列表模型涵蓋空房、單人、第二真人加入、假人停用；既有建房、更新、退出、跨來源通知、一般房間門檻及斷線結算通過。現行十張資源表不變。

建置：tools/resource-database/build.py；回歸：tools/resource-database/test.py。

發布狀態：已部署 0.6.64，啟動器雜湊及正式 SQL READY／端點／測試程序清理通過（bot-room-count-064-smoke）。遊戲內人工顯示驗收待使用者測試。
