# Issue #83：P24Lab 隔離資料庫前置

## 結論

2026-11-06 UTC，依使用者對本次實驗帳號、權限及測試登入的明確確認，**資料庫隔離設定已就緒**：三個實驗 schema、受限本機帳號、兩組普通測試登入／角色及非 git 設定已建立，JDBC 與權限驗證通過。P24Lab 已正常關閉。

此結論限於 DB 前置，不代表 #84 戰鬥回歸、#225 候選啟動或原生遊戲登入驗收通過。候選 shared-source 接線與既有 map 12000 資產缺口仍需下一棒處理。

## 已驗證：來源與隔離

- 唯一來源為既有 P24Lab `127.0.0.1:43324` 的 `p24_world`、`p24_accounts`、`p24_lito`。線上核對 port、datadir、server UUID、bind 均符合既有 P24Lab 身分；MySQL 8.0.40，三庫為 utf8mb4／utf8mb4_0900_ai_ci。
- 新建 `p24_world_lito_exp`（181 表）、`p24_accounts_lito_exp`（70 表）、`p24_lito_lito_exp`（19 表），共 270 表。
- world 全表及 lito 的 17 張資源／規則表，共 198 表複製後 checksum 與來源一致。帳號庫只複製結構，不搬既有帳號、角色、密碼或 tw-bin 狀態；結算收據從空表開始。
- 三個來源庫無 view、trigger、routine、event。兩個 world 外鍵已重建到實驗 world 庫，無跨回原庫的外鍵。
- 新 `lito_source_server` 僅有一條啟用路由，指向 `p24_accounts_lito_exp`；未複製舊 `380_ac_*` 路由。該表四欄不含登入憑證。
- 原三庫全部 **270 表**的 `CHECKSUM TABLE EXTENDED`，在複製前、複製後、帳號建立後及 JDBC 寫入測試後均一致；未修改原庫或 tw-bin 測試資料。
- 兩階段啟動前各有 P24 自有目錄冷備份：`backups/issue-83-before-isolation/db-data`（893 檔）及 `backups/issue-83-before-credentials/db-data`（1407 檔），逐檔 SHA-256 一致。保留備份，不自動刪除。
- 未連正式 2000／3306，未重用舊 3307、正式來源 dump、舊秘密設定；未讀寫 `config/server.properties` 或 `docs/REPORT.md`，未啟動 Java 伺服器、遊戲或 CUA。

## 已建立設定與測試資料

相對 task/83 worktree：

- `scratch/t020c/isolated-test.properties`：全新 P24Lab 設定，指向 43324、新 world／lito 庫及新實驗 DB 帳號；配置 bind 為 127.0.0.1、候選遊戲埠為 42083，**本輪未啟動該埠**。
- `scratch/t020c/test-accounts-private.properties`：兩個普通測試登入及角色的本機交接資訊。
- 兩個檔案均經 git 忽略規則核對。舊同名檔案未讀取，已用此次全新內容取代；沒有複製舊帳密。`combat.properties` 未讀、未改，下一棒不可直接沿用。
- 測試資料只在新 accounts 庫：accounts、characters、character_lito 各兩列，普通權限、未封鎖、離線，初始勝敗／版本皆零；沒有既有玩家資料或 GM 身分。
- 新秘密只保存在指定非 git 檔案，未印出、提交或放入本報告。帳號與憑證會持續存在；關閉 MySQL 不代表撤權或刪除。

## 實際權限：逐表 80 SELECT，僅兩表可新增／更新

DB 身分為 **`'lito83_exp'@'127.0.0.1'`**。建立前以不限定 Host 的同名使用者計數確認為零；建立後確認無其他同名 Host、角色繼承或轉授權限。

讀回伺服器 metadata，核對為 **84 筆表級 privilege**：80 筆 SELECT，加兩張表各 INSERT／UPDATE。全域權限只有 USAGE，schema 級權限為零；沒有 DELETE、DDL、FILE、PROCESS、管理權或 GRANT OPTION。不是 schema.* 授權。

以下程式位置相對 `lito伺服器/LiTo381-Server/src/main/java/local/lito381/server/`。清單是 shared-source／read-only 路徑的具體表集合；候選接線限制見後文。

### p24_world_lito_exp：39 表，只讀

| 表名（每個名稱各授 SELECT） | 讀取依據 |
| --- | --- |
| `skills` | CombatDatabase.java:65；Accounts.java:6191 |
| `server_event`、`skills_相消免疫技能`、`skills_負面技能控制`、`skills_負面boss免疫`、`skills_buff_ability`、`npc` | SourceSkillControls.java:79、82、85、97、104、115 |
| `etcitem`、`etcitem_buff藥水設置`、`etcitem_buff藥水能力` | SourceEyeControls.java:20、40 |
| `系統_劇情任務`、`spawnlist_npc`、`mapids` | SourceBlessingControls.java:14、18、32 |
| `armor`、`weapon`、`裝備_weapon_屬性`、`裝備_weapon_greater_類型`、`裝備_armor_greater_類型`、`裝備_weapon_bless_類型`、`裝備_armor_bless_類型`、`裝備_weapon_bless_指定`、`裝備_armor_bless_指定`、`裝備_weapon_enchant_指定`、`裝備_armor_enchant_指定`、`裝備_weapon_enchant_類型`、`裝備_armor_enchant_類型`、`裝備_特殊_卡片_能力`、`裝備_特殊_卡片_組合`、`裝備_特殊_套裝_能力`、`裝備_特殊_魔石_魔法` | LegacyEquipmentMagicLoader.java:36 的 REQUIRED_RESOURCE_TABLES、:100 起 healthCheck；Accounts.java:6206；同集合中的 npc／etcitem 不重複計數 |
| `道具_自訂穿脫能力`、`etcitem_arrow` | LegacyEquipmentMagicLoader.java:152、158；兩表確實存在，啟動檢查會查欄位 |
| `pettypes` | Accounts.java:6193、6268、12459 |
| `角色_精神成長`、`角色_體質成長`、`角色_力量成長`、`角色_敏捷成長` | Accounts.java:6273–6280 |
| `etcitem_doll_type`、`etcitem_doll_power_new` | Accounts.java:15066 的背包查詢／啟動驗表；:12106、15109 的角色背包載入 |

### p24_accounts_lito_exp：22 表

以下 20 表在 `Accounts.java:6173` 起的 healthCheck 無條件讀取（最後四表位於 :6281–6284）：

`accounts`、`characters`、`character_other`、`character_skills`、`character_quests`、`character_items`、`character_pets`、`character_teleport`、`character_buff`、`character_buddys`、`character_letter`、`character_warehouse`、`character_elf_warehouse`、`clan_warehouse`、`character_items_card`、`character_items_dark`、`clan_data`、`server_城堡`、`server_盟屋_拍賣`、`server_盟屋_佈告`。

另外：

- `character_items_time`：Accounts.java:14995 背包 JOIN，:6239 啟動驗證。
- `character_lito`：SharedDatabase.java:74、88；SettlementSql.java:7、35、49。

上述皆 SELECT；**只有 character_lito 另有 INSERT／UPDATE**。登入與普通帳號權限查詢見 Accounts.java:6627、6642；角色投影見 :6900–6916。

本輪確認 `character_buff` 沒有 snapshot_version，走 SourceBuffRead.java:11–26 的舊單表分支，不需要不存在的 character_buff_snapshot。未授予其他公會徽章、盟屋紀錄或獎勵功能的額外表。

### p24_lito_lito_exp：19 表

| 表名（每個名稱各授 SELECT） | 讀取依據 |
| --- | --- |
| `lito_source_server` | UnifiedConfiguration.java:51 |
| `lito_resource_release`、`lito_resource_pending`、`lito_resource_skill_value`、`lito_resource_buff`、`lito_resource_map`、`lito_resource_zone`、`lito_resource_npc_identity`、`lito_resource_map_page`、`lito_resource_portal`、`lito_resource_projectile` | ResourceDatabase.java:24、35、36、43、46、49、52、53、54、63 |
| `commands`、`lito_tournament_shop`、`lito_battle_rule`、`lito_battle_object`、`lito_battle_spawn`、`lito_battle_player_spawn` | Accounts.java:6195–6205、6656、6674、6698、6711、6724、6740；由 SharedDatabase.java:64–68 導向 controls 庫 |
| `lito_room_policy` | NativeRoomRules.java:20 |
| `lito_battle_settlement` | SettlementSql.java:7、16、23、30、32 |

**只有 lito_battle_settlement 另有 INSERT／UPDATE**。

### 寫入邊界

- character_lito：SettlementSql.java:38–39；舊 CAS 路徑 SharedDatabase.java:139–140，受 SETTLEMENT 守門。DB 權限限制表與操作，不會自行限制呼叫時機。
- lito_battle_settlement：SettlementSql.java:14 的 INSERT ON DUPLICATE KEY UPDATE、:42 的 applied／applied_at UPDATE。
- 隱含讀取 characters、character_lito 及收據皆已列入；還有 GET_LOCK／RELEASE_LOCK、commit／rollback，以及受權物件的 information_schema metadata 查詢，不需額外管理權。
- DurableBattleSettlement.java:19–23 在啟動時會 replay 本機 WAL／drain 收據。後續候選須使用無舊待補存檔的實驗 WAL 路徑；本輪未啟動該流程。

## 已驗證：JDBC 與權限

以固定 Java 17 及 mysql-connector-j 8.0.33，使用新實驗帳密直接連線；沒有執行候選伺服器。

| 檢查 | 結果 |
| --- | --- |
| JDBC authenticated user、43324、server UUID | 相符 |
| 逐表 SELECT LIMIT 0 | 80／80 通過 |
| 資源 qualified_version／skills 非空 | 0.6.61／通過 |
| 唯一來源路由 | 僅新 accounts 庫 |
| 兩組普通登入資料、兩個角色投影 | 通過；採 Accounts.java 對應 SQL 與直接密碼比較規則；不等於遊戲協定登入測試 |
| 實驗 accounts INSERT／UPDATE、world.skills UPDATE、lito_source_server UPDATE | 4 項均被權限拒絕；使用零列語句，無越權資料變更 |
| 原 p24_accounts／p24_world／p24_lito 的代表表 SELECT LIMIT 0 | 3 項均被權限拒絕；未嘗試原庫寫入 |
| character_lito INSERT／UPDATE | 受限帳號成功建立並更新兩筆初始零戰績 |
| lito_battle_settlement INSERT／UPDATE | 在已核實的 InnoDB 交易內成功；ROLLBACK 後 0 列，未使用 DELETE 清理 |
| JDBC 後原庫 checksum／實際 grants／外鍵及路由 | 全部符合；原 270 表資料未變 |

## 正常關閉與交付證據

本輪兩階段啟動皆在動作前後核對身分與使用者；沒有停止他人的程序。最後於 03:38:00Z 正常 SHUTDOWN（exit=0），本輪程序已退出，43324／42061 無監聽；證據見本機 `p24-credentials-shutdown-verification.json`。

證據只保存在非 git 的 `scratch/t020c/`：

- `p24-backup.json`、`p24-credentials-backup.json`：冷備份驗證。
- `p24-identity.json`、`p24-credentials-identity.json`：線上身分。
- `p24-clone-verification.json`、`p24-source-before.json`：來源／副本校驗。
- `p24-access-verification.json`、`p24-select-tables.tsv`：精確實際授權。
- `p24-jdbc-verification.json`、`p24-final-verification.json`：JDBC、拒絕寫入及來源不變。
- `p24-shutdown-verification.json`、`p24-credentials-shutdown-verification.json`：兩階段停機。

公開僅提交本報告。原有 `D scratch/t020c/cleanup-exp-db.sql` 已核對為遠端 71189b9b 的既有清理提交；本輪以 fast-forward 保留該提交，新提交只含報告。腳本、秘密、資料列、dump、備份及原始紀錄不公開。`git diff --check` 與秘密排除檢查通過。

## 未驗證與給下一棒的提示

1. **候選接線是具體程式缺口**：本 checkout 的 Accounts.java:522 openLegacy 尚未呼叫 SharedDatabase.open，:14856 resourceTable 仍直接使用 resourceDatabase；UnifiedConfiguration.java:73 設為 lito_mysql，會進 Accounts.java:6285 的額外擴充表檢查及 :6422 的遷移 DDL。舊 tools/shared-database/PatchShared.java:22–45 有對應掛鉤，但目前 pom.xml 未接入 PatchShared。需由 #225／主控核實指定候選的實際接線，不以擴權、建額外擴充表或授予 DDL 掩蓋。不得把這份設定直接用在未核實的任意 Maven 產物。
2. 已完成表存在、JDBC 投影及指定資源版本檢查，尚未完整執行 ResourceDatabase／所有 legacy healthCheck 欄位與資料語義檢查；#84、#225 及原生遊戲登入均未執行。
3. 舊 combat.properties 不沿用；後續需按候選需求建立安全來源配置。map12000 缺資產是前輪既有觀察，本輪未重跑或修復。
4. 恢復使用時只依既有 P24Lab 流程啟動 43324，重新核對身分與衝突；實驗帳號不具啟停或管理權。用完正常關閉，不自動刪庫、撤權或刪憑證。
5. 若將來擴至公會／盟屋／獎勵等額外功能，先列出實際讀取表與需求；現有權限不涵蓋那些其他路徑，不使用 schema.* 作為猜測替代。

先前 3307 殘留與正式匯出事件的歷史盤點保留於 Git 歷史（65080484）及 #273；本輪沒有使用那些資料或設定。此次完成不抹除前輪事件，也不宣稱正式環境完整稽核完成。
