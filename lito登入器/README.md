# LiTo 登入器專案

目前發佈版本為 **0.6.70**，最新封存基準為 **0.6.66**；聯合基準位於 `LiTo381-Launcher/backups/baseline-0.6.66.zip`。

- 啟動：`LiTo381-Launcher/release/current/LiTo381Launcher.exe`，桌面「LiTo 0.6.70 正式啟動器」指向同一檔案。
- 開發、執行及備份：`LiTo381-Launcher/`，詳見其中的 `README.zh-TW.md`。
- 仍被引用的原生程式、反組譯與分析契約：`舊版分析與驗收/`，保留原路徑。已完成的驗收／偵錯與多份公開參考來源集中至 `docs/history/retired-references-and-runs-20261014.zip`。
- 舊相容層原始碼：`LiTo381-Compat/`，現行相容 DLL 原始碼以 `LiTo381-Launcher/native/` 為準。
- 歷史開發證據：`docs/history/development-evidence.zip`，包含逐檔雜湊清單。舊交接紀錄中的絕對路徑只代表當時位置，不能當作現行啟動路徑。
- `工作流採證/` 及四份工作流控制 JSON 保留作開發追蹤。

遊戲資源共用 `D:/lineage開發/0-客戶端`，不要再複製整套 pak、map、sprite 至登入器專案。伺服器使用共用 380_db、380_lito 與已啟用的角色來源庫；來源資料唯讀，僅回存 character_lito 勝敗戰績。

專案容量盤點以檔案邏輯大小計算，不跨越 junction 或 symbolic link；不同位置的硬連結可能重複計入，不能直接等同磁碟實際釋放容量。


共同規範：[檔案分工、紀錄保留與清理](LiTo381-Launcher/docs/PROJECT-MAINTENANCE.zh-TW.md)。歷史資料索引見 `docs/history/README.md`。
