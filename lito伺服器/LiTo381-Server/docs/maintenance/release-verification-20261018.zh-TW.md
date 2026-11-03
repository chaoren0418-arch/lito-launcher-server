# 正式發布驗證：SQL 結構與原生依賴阻礙

日期：2026-10-18。範圍為既有正式版及先前通過階段測試的候選發布驗證；沒有接續技能 73 開發，沒有覆蓋正式 JAR、修改來源 SQL 或啟動遊戲視窗。

## 實測結果

| 檢查 | 結果 |
| --- | --- |
| 正式 BAT `--check` 對應的 `Start-LiToServer.ps1 -Mode check` | 通過；設定、Java、正式 JAR、資源及埠檢查成功，SQL 來源為公牛、天使，來源角色唯讀 |
| 正式 BAT `--smoke` 對應的 `-Mode smoke` | 失敗；資源目錄及 SQL 資源載入後，`380_ac_1.character_buff_snapshot` 不存在 |
| 先前 `combat-acceptance-current.json` 指定候選的正式 SQL 啟動 | 失敗；使用正式設定、共用目錄與既有伺服器 lib，發生相同缺表錯誤；退出碼 1 |
| 原生啟動器 `tools/verify_current.py` | 失敗；`0-客戶端/TW13081901.txt` 缺失，未執行原生啟動 |
| 啟動檔及 BIN 個別雜湊核對 | 21 項中 20 項相符，唯一缺失為上述 TXT |
| 失敗程序／埠清理 | 本輪測試程序均退出；兩個 bind 地址各三個埠可獨占 bind，共 6/6；只剩驗證前已存在的 Java PID 54036，未操作該程序 |
| 成功啟動後的正常關閉與強制關閉 | 未完成；不能把啟動失敗後退出當成正常關閉驗收 |
| 原生選服、登入、大廳及結算介面 | 未執行；必備檔與 SQL 啟動先失敗 |

正式 JAR SHA-256：`c02aaae8d54a27e30460be2e3c47709ee64d35f007a8be27ac74a327268b9539`，仍與發布設定一致。

受測候選 SHA-256：`efd4bc36b47db13cc0bd52426355a4250ea7db4b88ac724afa653a416954ab11`，與其原階段驗收收據一致。其 `LegacyMySqlRepository` 及 `SharedResourceMain` class 與正式版相同；較新的材料候選不能直接沿用本次候選結果。

## SQL 原因

以正式設定進行唯讀 metadata 查詢，未讀出帳密或角色資料：

- `380_ac_1.character_buff` 為四欄：`char_obj_id`、`skill_id`、`remaining_time`、`poly_id`；沒有 `character_buff_snapshot`。
- `380_ac_2.character_buff` 仍包含第五欄 `snapshot_version`，且 `character_buff_snapshot` 仍存在。
- `1-伺服器/db/維護工具/20261016_character_buff_single_table.sql` 明確把 BUFF 遷移為單表並刪除快照表。故兩個來源目前具有不同結構，LiTo 舊 repository 的快照表檢查與新版來源不相容。

不能為了通過檢查重建空快照表、停用來源或略過 healthCheck；需單獨處理兩種來源結構的正確唯讀 BUFF 載入，並驗證保留舊版完成快照語義。這是後續相容性修正，並非本次已有測試通過的發布成果。

## 原生依賴

缺少的精確路徑：`D:/lineage開發/0-客戶端/TW13081901.txt`。

現有啟動器期待 SHA-256：`7ebe2b464c8b1ea9c58ac985dd8ff4c823784d8231894262eddcd7691ca94008`。

0.6.66 基準 ZIP 未包含該外部 TXT；已查到的一份歷史來源 TXT 雜湊不同，不能直接替代或改寫 pins。未降低檔案校驗，也未改共用客戶端。

## 證據與發布決定

- [本輪結構化結果](../../runtime/release-verification-20261018/verification.json)
- [候選正式 SQL 啟動錯誤](../../runtime/release-verification-20261018/candidate-classpath-startup.stderr.log)
- [正式版啟動錯誤](../../loginfo/startup-check-20261018-101226-051.stderr.log)
- 唯讀查表工具：`runtime/release-verification-20261018/ReadOnlyBuffSchema.java`；使用既有 JDBC library，連線由 try-with-resources 關閉。

首次候選驗證直接以 `-jar` 執行時，候選所在 build 目錄缺少 Manifest 相對 lib，導致 JDBC driver 無法載入；修正測試 classpath 指向既有伺服器 lib 後才得到上述 SQL 結果。首次紀錄保留，不將驗證工具位置問題誤記為候選技能失敗。

目前沒有本次可通過完整發布檢查的新版本，因此更新數為零；全部施工候選保留。既有技能實作未重做，沒有匯入 SQL、清理戰績 WAL、終止既有其他 Java 或修改發布 pins。
