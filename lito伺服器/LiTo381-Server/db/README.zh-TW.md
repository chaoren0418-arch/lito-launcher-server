# SQL 檔案用途

正式部署共用天堂 `380_db` 與來源角色庫；LiTo 規則放 `380_lito`。執行期僅結算寫入來源庫 `character_lito` 與 LiTo 結算收據，詳見 [資料庫規範](../SQL-PERSISTENCE-GATES.zh-TW.md)。

[資料來源索引與補足清單](../docs/DATA-SOURCES.zh-TW.md)：區分原生程式、客戶端資源、共用天堂資料與專案預設，包含 0.6.60 現場唯讀比對。出生點的「原生預設」註記包含地形推導值，不能視為全部出自原服座標表。

| 檔案 | 用途 |
| --- | --- |
| `lito_room_policy.sql` | 0.6.61 測試房開關／ArenaBot；初次安裝由資料庫管理帳號執行，重跑保留既有設定，修改後重啟 |
| `lito_battle_settlement.sql` | 原生 0.6.60 的 InnoDB 結算收據與來源去重標記；新增來源時也需遷移 |
| `lito_commands.sql` | 建立 `380_lito.commands`，遷移／補入 `.item`、`.spawn`、`.de`；重跑保留既有 LiTo 設定，不改天堂指令表 |
| `lito_gm_polyme.sql` | 新增 `.polyme 外型代號`／L1PolyMe，預設 GM 門檻 200；重跑不覆寫既有列 |
| `lito_source_server.sql` | 來源表及八格初始設定，前三服啟用；既有安裝應先檢查內容再操作 |
| `380_lito_seed_example.sql` | LiTo 戰鬥／商店等範例資料，並非現場資料庫的即時備份 |
| `lito_static.sql`、`380_ac_1.sql`、`380_ac_2.sql` | 歷史獨立 schema／重建與測試來源；不要匯入覆蓋共用的正式角色庫 |
| `modern_*.sql` | Modern 候選功能的 schema，不屬原生 LiTo 正式安裝要求 |

`generated/` 三份 SQL 與上述歷史檔案完全相同，已核對 SHA-256 後移除重複副本。`tools/Build-LiToSqlLayout.ps1` 是舊版固定輸入產生器，重跑仍需相符的來源雜湊；不應為通過驗證而直接改掉雜湊。

完整舊版資料表演進與寫入計畫已封存至 `backups/history/layout-before-20261012.zip` 的 `server/db/README.zh-TW.md`，不作現行部署規範。

## 0.6.63 原生資源精簡

正式 `380_lito.lito_resource_*` 共 10 表／651 列；統一待補／備註為 `lito_resource_pending`，包含 22 筆。研究表已封存並移除。

新安裝使用 `lito_resource_compact.sql`，只有十表，不會覆蓋現存表。舊 ImportResources 已停用，prepare_import.py 改為只輸出十表。原候選包及舊24表SQL皆不是現行安裝入口。

[表格用途、封存與驗證](../docs/maintenance/resource-database-0.6.63.md)。
