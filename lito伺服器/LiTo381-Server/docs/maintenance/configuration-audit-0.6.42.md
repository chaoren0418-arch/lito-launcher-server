# 0.6.42 設定稽核

所有來源均以資料庫為準；沒有檔案清單回退。以下為現行22項有效外控，新增未知鍵會拒絕啟動。

| 設定 | 實際消費位置／範圍 |
|---|---|
| `bind` | ServerConfig.bind → SourceLogin LOGIN listener |
| `game.bind` | ServerConfig.gameBind → SourceLogin GAME listener / advertisedHost |
| `port` | ServerConfig.port、SourceLogin base+id、GUI nativeSourcePortBase |
| `server.name` | ServerConfig.serverName/originSourceName |
| `database.jdbc.base.url` | UnifiedConfiguration.sources、LegacyMySqlRepository JDBC |
| `database.jdbc.options` | UnifiedConfiguration.sources、LegacyMySqlRepository JDBC |
| `database.user` | UnifiedConfiguration.sources、LegacyMySqlRepository credentials |
| `database.password` | UnifiedConfiguration.sources、LegacyMySqlRepository credentials |
| `database.static.database` | LegacyMySqlRepository.resourceDatabase |
| `database.lito.database` | SQL source directory / SharedDatabase.resourceTable |
| `weight.rate` | ServerConfig.legacySourceWeightRates → repository.serverWeightRate → BattleInventoryWeight.maxWeight |
| `capture.directory` | ServerConfig.captureDirectory；GUI 場次固定覆寫 |
| `battle.ai.enabled` | GameHub.aiEnabled / ClientSession battle AI branches |
| `battle.physical.hit.enabled` | ClientSession physical hit calculation |
| `battle.natural.mp.regen.enabled` | ClientSession natural MP regeneration |
| `native.siege.roster.enforced` | GameHub.siegeRosterEnforced |
| `native.tournament.allowance` | ClientSession resetNativeTournamentEconomy |
| `memory.initial` | GUI Python JVM args / BAT JVM -Xms；C# full7驗證 |
| `memory.maximum` | GUI Python JVM args / BAT JVM -Xmx；C# full7驗證 |
| `cpu.limit` | GUI/BAT ActiveProcessorCount；C# full7驗證 |
| `logs.directory` | BAT logRoot / HeapDumpPath |
| `startup.timeout.seconds` | BAT --smoke deadline |

已移除或內部化：來源 names/ids/databases/個別倍率（改 SQL＋共用倍率）；advertised.host/port 及 login.source.port.base（由 game.bind/port 推導）；JDBC LOGIN/靜態連線重複鍵（統一帳密）；database.mode/read.only/persistence.policy/state.persistence（固定唯讀戰績架構）；bootstrap/delay 與 character directory（固定原生握手）；classic-world/租賃/兼容演員相關欄位（不適用正式 arena）；停用的 classic-world AI tick；state.file（記憶體模式內部虛擬路徑）；Java/JAR/catalog 路徑（發佈程式及雜湊固定）。

驗證：ConfigurationRegression 對正式 JDBC 來源讀取、三服倍率及變更倍率、遊戲開關、管理帳號、金幣額度、來源與埠推導、不可寫政策、參數範圍、重複鍵/未知鍵、secret-free 場次檔做斷言；BAT check/smoke 與 GUI helper 原生啟動驗證入口接線。這是設定消費與啟動測試，不代表每種戰鬥模式都重新完成手動實測。

SQL 清單修改不會更新玩家資料。讀取失敗／空清單會明確拒絕啟動。新增服仍需要存在的角色庫、character_lito 表及執行帳號權限。

2026-10-12 權限統一：administration.accounts 已退役並拒絕載入。管理員身分僅依來源 accounts.access_level，所有原生 isAdministrator 判斷與 .item/.spawn/.de 共用來源權限快照；GM 指令仍要求 commands.access_level。權限快照在帳號／來源改變及斷線清除，修改資料庫後需重新登入。本日權限與來源隔離測試見 source-administration-validation.json；先前 ConfigurationRegression 的管理白名單測試屬歷史版本。
