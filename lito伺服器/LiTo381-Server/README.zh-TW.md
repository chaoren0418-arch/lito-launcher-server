# LiTo 伺服器

現行部署：**登入器 0.6.71／共用天堂資料來源／只保存 LiTo 勝敗**。本目錄保存 Java 原始碼、設定、SQL 與維護工具；正式核心由相鄰登入器專案集中保存。

基準備份與整理：登入器 `backups/baseline-0.6.66.zip` 同時保存本專案與登入器；還原／排除項目見相鄰登入器 `docs/BASELINE-0.6.66.md`。舊逐版備份已整合，歷史文件中的舊 ZIP 路徑不再作現行回復入口。

## 啟動與設定

- 雙擊 `Start-LiTo381-Server.bat`，或上層 `1.啟動伺服器.bat`。加 `--check` 預檢，`--smoke` 做短暫 SQL 啟動驗證；`Ctrl+C` 停止。
- BAT 與登入器內建伺服器使用同一組埠，擇一啟動。BAT 不會終止占用埠的其他程序。
- 唯一操作設定是 [`config/server.properties`](config/server.properties)，22 項均有中文註解。[各項用途與範圍](docs/maintenance/configuration-audit-0.6.42.md)。修改後重啟生效。
- 伺服器來源在 `380_lito.lito_source_server` 管理。固定 ID 1～8，目前公牛與天使啟用，水蛇保留停用；新增來源需準備角色庫、`character_lito` 與資料庫權限。
- `weight.rate=100.0` 代表 100 倍負重上限，所有來源共用。JVM 記憶體與 CPU 設定同時適用 BAT／登入器。

管理權限只取所選來源動態庫 `accounts.access_level`：正數為管理員；`.item`／`.spawn`／`.de` 仍須符合 `commands.access_level`，最低要求為正數。權限於登入後首次需要時讀取，隨帳號／來源變更及斷線清除；修改資料庫權限後重新登入生效。已移除 `administration.accounts`，舊鍵會被拒絕。

## 房間規則

`380_lito.lito_room_policy` 控制各頻道 Local Lobby 測試房及 ArenaBot。預設兩者啟用；只有死亡競賽單人測試加入假人，測試房不寫入勝敗。玩家自建房不加假人，依 `lito_battle_rule.minimum_players` 檢查真人數；隊伍模式檢查兩隊及人數一致條件。開始倒數與進場前均重新驗證。[詳細規則與驗證](docs/maintenance/room-rules-0.6.61.md)。

測試房列表人數包含實際列出的 ArenaBot：空房 1 人，單人加假人 2 人，兩位真人時顯示 2 人；真人開戰門檻不變。[人數顯示修正](docs/maintenance/bot-room-count-0.6.64.md)。

## 原生資源資料庫

`380_lito` 的資源表已精簡為 10 張 `lito_resource_*` 表（9 張執行表＋1 張統一待補／備註表）；有證據的技能呈現、BUFF selector、箭矢、地圖目錄與既有城鎮傳送於啟動時載入記憶體。缺證保留 NULL／停用及中文備註，查 `lito_resource_pending`。[接線範圍與驗證](docs/maintenance/resource-database-0.6.63.md)。資源資料庫基礎建置入口為 `tools/resource-database/build.py`，固定輸入為 `tools/resource-database/inputs/base-0.6.61.jar`，`build/` 內的候選與 class 檔可重建，舊修補工具不可直接覆蓋現版。0.6.71 正式 JAR 來自已驗證的世界資源／職業圖示配對候選，最終雜湊依 `deployment/release-inputs.json`；不得以較早的資源資料庫基礎輸出直接覆蓋。

## 資料流

`選伺服器 → 輸入帳密 → 選角色 → 頻道／大廳 → 戰鬥 → 結算 → 大廳`

選服先固定資料來源，同名帳號以來源 ID 隔離。登入讀取天堂已保存的 SQL 資料；購買、消耗、技能與戰鬥狀態在記憶體。結算先保存可重試收據，再更新角色所屬庫的 `character_lito` 勝敗，刷新大廳與排行榜並清理場次資料。積分、月排行仍不跨伺服器重啟保存。[完整寫入邊界](SQL-PERSISTENCE-GATES.zh-TW.md)。

| 目錄／檔案 | 用途 |
| --- | --- |
| `config/` | 唯一外控 `server.properties` |
| `deployment/` | 發佈雜湊清單；`memory/` 為隔離記憶體診斷入口與模板 |
| `src/`、`lib/` | Java 主程式、測試與相依函式庫 |
| `db/` | 現行來源表 SQL、LiTo 範例資料與保留的歷史 schema；先讀 [SQL 說明](db/README.zh-TW.md) |
| `tools/` | BAT 實作、驗證及建置工具；歷史診斷需依對應版本執行 |
| `runtime/`、`loginfo/` | 執行輸出、診斷場次與 BAT 紀錄；不再存固定啟動依賴 |
| `docs/` | 歷史索引、維護規範及最新驗證紀錄 |
| `backups/` | 基準與遷移前備份；`history/` 集中舊資料與原始文件 |
| `LiTo381-Server.jar` | 原始 0.7.864／隔離記憶體診斷核心，非現行正式核心 |

## 維護入口

戰鬥／技能同源化候選正在實作，尚未更新正式核心。[實作、差分測試及接續項目](docs/maintenance/combat-integration-current.md)，[逐技能覆蓋矩陣](docs/maintenance/combat-coverage-current.md)。候選另以 `config/combat.properties` 指向來源職業／技能外控，正式 0.6.71 暫仍使用原有設定行為。

正式 JAR：`../../lito登入器/LiTo381-Launcher/app/server/LiTo381-SharedResources-candidate.jar`。資源目錄與客戶端共用 `0-客戶端`；雜湊固定於 `deployment/release-inputs.json`，此檔是部署驗證資料，不是遊戲外控。

房間規則 0.6.61 修補入口為 `tools/room-rules/build.py`，使用已驗證的 0.6.60 正式核心作固定基底；`tools/room-rules/test.py` 驗證原生加密封包、測試假人與開戰條件。發布需同步正式核心、deployment 雜湊、正式啟動 helper 雜湊，再執行相鄰登入器 `tools/build_current.py`。舊版工具只適用其各自保留的基底，不能直接覆蓋現行核心。

`Build-Server.ps1` 與 `tools/shared-database/build.py` 分別是原始核心、舊版資料庫修補的候選建置；不可直接覆蓋現行正式 JAR。舊版 `publish.py` 已停用，避免覆蓋後丟失後續修正。

- [驗證結果與限制](VALIDATION.md)
- [封包／功能支援範圍](SERVER-PROTOCOL-COVERAGE.zh-TW.md)
- [資料庫與記憶體規範](SQL-PERSISTENCE-GATES.zh-TW.md)
- [資料來源索引、原生證據界線與資料庫補足清單](docs/DATA-SOURCES.zh-TW.md)
- [歷史資源、維護規範、更新日誌及回復方式](docs/HISTORY.md)

## LiTo 專用 GM 指令

`.item`、`.spawn`、`.de`、`.loc`、`.move`、`.polyme` 只讀取 `database.lito.database` 指定庫的 `commands` 表（目前為 `380_lito.commands`）。初始門檻皆為 200，身分依所選來源帳號 `accounts.access_level` 判斷。安裝／遷移 SQL：`db/lito_commands.sql`；天堂 `380_db.commands` 不再控制 LiTo 指令。表格不能替代尚未實作的指令處理器。

`.loc` 查詢座標、`.move X Y [目前地圖ID]` 同圖傳送；`wand.Storm_Walk` 道具（目前 42501）支援點地面傳送。[用法、安裝 SQL 與驗證](docs/maintenance/gm-travel-0.6.68.md)。

## 版本歷史

歷史更新敘述已集中至 `docs/history/readme-before-baseline-0.6.66.md`；各項原始驗證仍保留於 `docs/maintenance/`。現行移動鎖定修正見 `docs/maintenance/movement-lock-0.6.66.md`。

專案整理與紀錄保留統一依 [原生 LiTo 專案維護規範](../../lito登入器/LiTo381-Launcher/docs/PROJECT-MAINTENANCE.zh-TW.md)；歷史資料可用登入器 `tools/project_hygiene.py` 按原路徑查詢與解壓。

0.6.67 新增 Ctrl 強制指定敵友目標；[接線與驗證範圍](docs/maintenance/forced-combat-0.6.67.md)。

`.polyme 外型代號`：L1PolyMe 自身變身，持續 7200 秒，例如 `.polyme 6080`；使用共用 polymorphs，結算／離房清除，不寫回角色庫。[用法、驗證與限制](docs/maintenance/gm-polyme-0.6.71.md)。
