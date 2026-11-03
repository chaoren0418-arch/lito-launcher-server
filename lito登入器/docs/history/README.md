# 歷史資料索引

歷史資料只供追溯或在隔離目錄重現，不是正式啟動入口。正式版本與遊戲基準為 0.6.66。

| 封存檔／保留位置 | 內容 |
| --- | --- |
| `development-evidence.zip` | 先前集中保存的早期開發證據 |
| `retired-references-and-runs-20261014.zip` | 本次去重封存的公開參考來源、已完成的舊驗收／偵錯與過期場次 |
| `maintenance-20261014.zip` | 本輪專案規範、整理工具、文件及少量退休 BAT 紀錄的補充回復包 |
| `../../LiTo381-Launcher/backups/baseline-0.6.66.zip` | 正式遊戲與兩專案基準 |
| `../../LiTo381-Launcher/backups/history/` | 0.6.60 前與 0.6.61～0.6.66 的獨有文字／來源及備份清單 |
| `../../舊版分析與驗收/` | 重建版仍引用的原生程式、反組譯、分析契約與必要工具；來源測試用 tw_tpai 仍保持原路徑 |

## 查詢及取回

在 `LiTo381-Launcher` 目錄執行：

```powershell
python -B -X utf8 tools/project_hygiene.py find --match "public-source-audit"
python -B -X utf8 tools/project_hygiene.py extract --match "檔名或原相對路徑" --to "D:/LiTo-history-review"
```

解壓目的地必須是全新目錄。工具先驗證壓縮檔及全部內容雜湊，再按原路徑重建指定檔案，不覆蓋正式專案。

封存格式為 `MANIFEST.json` 加 `blobs/<SHA-256>`：相同內容只存一次，清單保留每個原路徑、大小與原因。不要把 blobs 目錄當作原專案直接執行。外層檔案與刪除清單的雜湊收據位於 `LiTo381-Launcher/docs/baseline-0.6.66-hygiene-receipt.json`。

## 保留界線

`runtime/battle-settlements`、0.6.26 特別回復點、SQL 遷移前備份，以及 `0-客戶端` 的共用資源都不屬於退休偵錯檔。

舊 `.pytest_cache` 目錄目前因 Windows ACL 拒絕讀取／刪除而保留；本次未遞迴變更擁有權或權限。其他原生參考檔保留是因仍有依賴，不是清理遺漏。

單檔還原測試已確認內容相同。自動審查拒絕刪除該驗證樣本，因此 `LiTo381-Launcher/docs/baseline-0.6.66-hygiene-restore-check/` 保留一份已驗證 Java 原始碼；詳見 hygiene-exceptions.json。
