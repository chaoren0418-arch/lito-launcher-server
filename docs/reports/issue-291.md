# Issue #291 — T090：盤點 1-伺服器 的角色／道具／戰鬥／技能／多來源實作（唯讀）

## 做了什麼
- 唯讀查閱 `D:\lineage開發\1-伺服器`（`pom.xml`、`src/com/lineage/**` 1899 個 Java 檔、`db/*.sql` 的 `CREATE TABLE` 段落）。未修改、未建置、未啟動、未連 DB；未開啟 `config/server.properties`、`config/sql.properties`、`config/其他控制端/*`（只從原始碼列出鍵名）。
- 產出 `docs/reference/server1-inventory.md`：
  - 0 專案概況：語言／建置／進入點／啟動順序／套件結構／設定檔鍵名／5 個 DB dump 的表與欄位（標明中英文欄位）。
  - 1～5 五個領域，每項列出類別／檔案:行號與主要方法，各領域附「LiTo 可以直接參考」與「LiTo 不需要」。
  - 6 命名與資料結構差異表。
- 未把 1-伺服器 原始碼複製進本 repo（只有路徑:行號與一句話描述）。

## 怎麼驗證
- 各領域行號都是用 Grep -n／Read 實際看到的。
- 另外獨立回讀抽驗：27 個 Java 行號（CharacterTable:497、L1SkillMode:128、L1SkillUse:1687/2726、L1AttackPc:294/354、L1AttackMode:250、L1PinkName:133、L1HateList:45、NpcAI:286、CalcExp:54、L1PcInstance:4114/4644/6883、ItemTable:402、EnchantManager:227、L1PcInventory:989、C_UseSkill:52、C_AttackBow:53、Skill_Studying:48、CharacterPersistenceService:27、CalcStat:75、GameServer:252、DatabaseFactoryLogin:15、PcPhysicalAttackFlow:71、L1MonsterInstance:626、L1AttackNpc:308），以及 6 個 SQL 行號（380_ac.sql:454/983/1322、380_db.sql:42925/47987/86124）。全部吻合。
- LiTo 端的對照（`SettlementSql.java`、`SharedDatabase.java`、`UnifiedConfiguration.java:51`、`server.properties.example` 鍵名）也是在本 repo Grep 看到的。

## 已驗證／推測／待驗證（重點）
- **已驗證**
  - 角色、道具、技能、BUFF 的玩家資料都在 **380_ac**（不在 380_db）。
  - `weapon`／`armor` 欄位是**中文**；`characters`、`character_items`、`skills`、`etcitem` 是英文。
  - `character_lito` 在 380_ac.sql:983，是 **MyISAM**。
  - 1-伺服器 會在移民／合併／洗白時處理 `character_lito`，但不讀寫勝敗；完全沒有引用 `380_lito`／`lito_*`。
  - 技能效果的分派是 `L1SkillMode` 的 HashMap 註冊表。
- **推測**
  - 自動存檔間隔的單位是秒。
  - 冷卻依 `reuseDelay`。
  - `model/skillUse/` 套件是死碼。
  - LiTo 程式註解裡的「Retained L1AttackPc」指的就是同一套公式。
- **待驗證**
  - `BaseStatTable` 的欄位。
  - `L1AttackNpc` 的 BOSS +30 命中是否有效。
  - 合併工具是否會重編 `character_lito.char_obj_id`。
  - LiTo 讀 weapon／armor 的中文欄位是否全部對齊。
  - LiTo 判職業用的是 `Class` 還是 `Type`。
  - 正式部署時 `lito_source_server.character_database` 的實際值。

## 未驗證什麼
- 沒有執行任何程式，所以公式只做靜態閱讀，沒有用數值驗證。
- `L1MobSkillUse`、`TargetStatus`、`SkillBuffIconHelper` 等只確認了類別位置，方法沒有細讀。
- LiTo 端的類別對應只粗略列出前綴與幾個檔名，沒有逐一比對。
- 本任務不涉及客戶端資源，所以沒有查 pak。

## 產出路徑
- `docs/reference/server1-inventory.md`
- `docs/reports/issue-291.md`（本檔）

## 給下一棒的提示
1. M1 後續若要比對戰鬥公式，從 `L1AttackPc.java:294`（calcPcHit）、`:909`（calcPcDamage）、`L1AttackMode.java:250`（calcPcDefense）入手，對照 LiTo 的 `BattlePhysicalAttack`／`ModernPhysicalCombat`。
2. 技能同源化（M2）可以比照 `L1SkillMode.java:157-242` 的註冊表加上 `effect_skill_id` 後備查詢，以及 `L1SkillUse.makeTargetList`（:1687）的 area／ranged 規則。
3. `character_lito` 是 MyISAM，LiTo 的樂觀鎖寫入（`SettlementSql.java:38`）在 MyISAM 下沒有交易保護，建議開一個 design 任務評估是否要求改成 InnoDB。這屬於 DB schema 決策，需要 `needs:user`。
4. 讀 weapon／armor 時一定要用中文欄位名，欄位對照在 `ItemTable.java:413-502,548-589`。
