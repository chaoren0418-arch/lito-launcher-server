# LiTo 伺服器專案

雙擊 **1.啟動伺服器.bat** 啟動正式伺服器；也可使用桌面 LiTo 0.6.71 正式啟動器。兩者共用設定與連線埠，擇一啟動。BAT 加 `--check` 做預檢，`--smoke` 做短暫啟動驗證。

- [專案結構、啟動與設定](LiTo381-Server/README.zh-TW.md)
- [驗證結果](LiTo381-Server/VALIDATION.md)
- [SQL 與記憶體規範](LiTo381-Server/SQL-PERSISTENCE-GATES.zh-TW.md)
- [歷史資源、維護規範與回復](LiTo381-Server/docs/HISTORY.md)

操作設定只有 `LiTo381-Server/config/server.properties`。來源在 `380_lito.lito_source_server` 管理，預留八格；目前公牛、天使啟用，水蛇保留停用，實際以資料表為準。天堂角色資料只讀取，僅戰鬥結算保存 `character_lito` 勝敗。

固定部署依賴已移至 `deployment`；舊 runtime 已封存於 `backups/history`。上層工作佇列及監製狀態保留原內容。

最新聯合基準：相鄰登入器 `LiTo381-Launcher/backups/baseline-0.6.66.zip`；結算 WAL 與外部 SQL 資料庫不包含在程式基準內。

共同維護規範：[檔案分工與紀錄保留](../lito登入器/LiTo381-Launcher/docs/PROJECT-MAINTENANCE.zh-TW.md)。`runtime/battle-settlements/` 不屬於可清理 log。
