# LiTo 資料來源索引與資料庫補足清單

核對日期：2026-10-13。適用基準：原生登入器／伺服器 **0.6.60**。

本次完成來源盤點及現場 SQL 唯讀比對，沒有修改資料庫、遊戲設定、JAR、DLL 或已封存基準。結構化證據見 [data-sources-0.6.60.json](maintenance/data-sources-0.6.60.json)：21 項來源分類、檔案 SHA-256、定位行、正式 JAR 內類別／資源雜湊及 SQL 比對結果。

## 1. 判讀方式

每項資料同時看「來源」與「驗證程度」，不能只根據檔名含 Native 或資料表 note 寫著原生就當作官方數值。

| 來源 | 可以支持的結論 | 限制 |
| --- | --- | --- |
| 原生程式分析 | 指令分支、封包格式、欄位用途、資源索引方式 | 需綁定 bin 雜湊與分析位址；客戶端未包含的伺服器規則無法由此證明 |
| 客戶端資源 | 地圖名稱、圖示、文字、地形、傳送資料 | 語系、版本及實際載入位置須一致；不同資源的 ID 命名空間不能混用 |
| 資源推導／執行校正 | 可站立座標、方向通行、可運作配置 | 推導出的安全出生點不等於原服預設出生點 |
| 共用天堂資料庫 | 目前 `380_db` 的物品、技能、NPC 定義 | 為目前部署的數值權威；不因此證明是當年 LiTo 原服數值 |
| 保留歷史資料／其他伺服器 | 某時期玩法說明或相容模板 | 版本可能不同；本次沒有重新上網核驗歷史報導 |
| 專案預設／管理設定 | 目前採用的規則、價格、權限與來源 | 可以維護，但應清楚標成設定，不能冒充 bin 原始資料 |

「SQL 值與範例一致」「JAR 含有此類別」「原生靜態分析成立」「實際遊戲通過」是不同證據。本次不以類別存在推論功能已在正式流程啟用，也沒有重跑所有遊戲模式。

## 2. 現場資料庫核對

使用現行 `config/server.properties` 指定的連線，以 JDBC 唯讀連線執行 SELECT。僅擷取結構、筆數、設定資料的比較雜湊；沒有匯出帳密、玩家資料或個別結算內容。這是核對時的資料快照，並非資料庫備份，也不是跨表交易快照。

| `380_lito` 資料表 | 現場筆數 | 核對結果 |
| --- | ---: | --- |
| `lito_battle_rule` | 40 | 範例 SQL 涵蓋欄位逐列完全一致 |
| `lito_battle_object` | 26 | 同上 |
| `lito_battle_spawn` | 25 | 同上 |
| `lito_battle_player_spawn` | 380 | 同上；380 列註記皆為「原生預設出生點」 |
| `lito_tournament_shop` | 16 | 同上 |
| `lito_source_server` | 8 | 讀取結構與筆數；不是 8 服全部啟用的意思 |
| `commands` | 3 | 讀取結構與筆數 |
| `lito_battle_settlement` | 0 | 讀取結構與筆數；不代表舊有角色勝敗為零 |

上述八張表均為 InnoDB。來源角色庫的 `character_lito` 不包含在這次引擎核對內。

`380_db` 唯讀計數：weapon 364、armor 1422、etcitem 3320、skills 807、npc 4138、spr_action 30812、道具_物品詳細說明 52。只確認資料存在，沒有逐列重新驗證數值或技能實作完整性。

範例來源：[380_lito_seed_example.sql](../db/380_lito_seed_example.sql)。它是預設資料，不是正式庫備份；歷史註解所提 `db/generated/lito_static.sql` 已去重，保留檔為 `db/lito_static.sql`。本次未重匯種子或改動其歷史內容。

## 3. 現有資料的欄位來源

### 模式與戰鬥規則

`lito_battle_rule.mode_id/map_id` 由 [BattleRules](../src/main/java/local/lito381/server/BattleRules.java) 的原生解析／呈現分支與地圖資源分類對應。分析定位包含 `sub_46F9C0`、`sub_46DFB0`，模式與地圖獨立傳送的定位為 `sub_46B7D0/sub_4A9E50`。

`time_limit_seconds/goal/minimum_players/enabled` 必須另外判讀。競速圈數與時間、限時挑戰時間，以及攻城的人數／時間有保留歷史說明；其他預設並未全部取得同版本原服設定。攻城版本差異記錄於 [NativeLitoSiegePolicy](../src/main/java/local/lito381/server/NativeLitoSiegePolicy.java)，不能把整張表都標為從 bin 擷取。

### 玩家出生點

來源鏈是：地圖 `LiTo/LTMap/<map-id>/*.s32` → [ArenaMapCatalog](../src/main/java/local/lito381/server/ArenaMapCatalog.java)／模式專屬部署 → [BattleControlCatalog.defaultPlayerSpawns](../src/main/java/local/lito381/server/BattleControlCatalog.java) → 發布 JAR 匯出種子 → SQL。

- 一般地圖：使用資源推導的安全候選點，部分以原生執行紀錄調整。
- 競速：結合 Layer8 起點方向與通行圖；伺服器另定順序檢查規則。
- 限時挑戰：`BattleTimeAttackSpawnCatalog.DEPLOYMENT_SOURCE` 明確標示 **provisional / exact-server-coordinates-unrecovered**。

因此 380 列「原生預設出生點」宜解讀為「LiTo 相容部署預設」。目前沒有足夠證據，把每一列都認定為原服座標。這次以索引釐清，保留現場設定不變。

### 目標物與刷怪

`lito_battle_object` 的 role、位置、gfx、maximum_hp 應分別記錄來源。範例中 26 個目標物使用 3000 HP；尚未找到證明這是同版本原服 HP 的資料。地形可站立驗證只能驗證位置可用。

`lito_battle_spawn` 現有 25 筆為任務戰鬥共用波次，`map_id=0`、座標為 NULL；NPC 基礎模板來自共用庫。這些列不是完整原服刷怪表，也未提供每張地圖的精確生成座標。載入及驗證入口同為 [BattleControlCatalog](../src/main/java/local/lito381/server/BattleControlCatalog.java)。

### 商店與共用靜態資料

[TournamentShopCatalog](../src/main/java/local/lito381/server/TournamentShopCatalog.java) 說明：選定客戶端支持「每場固定配額／不累積／場次消耗品」的機制；**20000 金幣及 16 個商品與價格取自保留的同系列 2003 資料，不是 bin 內嵌商品表或 3.81C 原服抓包**。現行金幣額度仍以 `server.properties` 設定為準。

weapon、armor、etcitem、skills、npc 與自訂道具說明以共用 `380_db` 為權威，不複製到 `380_lito` 建立第二套基礎數值。技能「數值／效果規則」與「圖示／動畫／游標對應」須分開追溯。

### 來源、指令、戰績

`lito_source_server`、`commands` 是本專案管理設定。`character_lito` 與 `lito_battle_settlement` 是本專案結算資料，不是原生 bin 的資料表。讀寫邊界見 [SQL 規範](../SQL-PERSISTENCE-GATES.zh-TW.md)。

## 4. 已整理但尚可資料化的內容

以下類別均查到存在於正式 0.6.60 JAR，索引保存各 class 的雜湊。這證明檔案隨版提供；是否可由目前大廳進入該功能仍需分別驗證。

| 項目 | 已有內容與來源入口 | 尚缺什麼 |
| --- | --- | --- |
| 地圖目錄 | [NativeLitoMapTableCatalog](../src/main/java/local/lito381/server/NativeLitoMapTableCatalog.java)：47 列名稱、分類、map ID、原始 slot | 專用表、載入接線與可開放旗標；47 列包含城鎮、成對／事件地圖，不能直接視為 47 張可開放戰鬥地圖 |
| 地圖傳送 | [ArenaPortalCatalog](../src/main/java/local/lito381/server/ArenaPortalCatalog.java)：S32 Layer7 入口與目的地 | SQL 覆寫與目的地／場次驗證；原始 tag 0x64～0x66 不可自行解讀成隊伍權限 |
| 區域色彩 | [NativeLitoZoneCatalog](../src/main/java/local/lito381/server/NativeLitoZoneCatalog.java)：LTzone-c.tbl，2 區域／2 色彩 | 專用表及場景接線；色彩不等於安全區或禁止攻擊規則 |
| 地圖道具 | [NativeMapItemCatalog](../src/main/java/local/lito381/server/NativeMapItemCatalog.java)：18 個道具／地圖頁碼對應 | 逐筆回查原始資源與共用物品 ID，再建立對應表 |
| 技能呈現 | BattleSpellCatalog、龍騎士／幻術師修補：施法、飛行、命中、狀態圖示、目標游標 | 統一且帶資源版本的 LiTo 相容映射；SQL 與 DLL／伺服器都要接線 |
| 租賃城鎮 NPC | [NativeLitoRentalTownNpcCatalog](../src/main/java/local/lito381/server/NativeLitoRentalTownNpcCatalog.java)：名稱、HTML、動作文字、特殊圖示 | 原服 actor／位置未恢復；目前模型和位置包含相容選擇，不能標成原版座標 |
| 租賃商店 | [NativeLitoRentalMerchantCatalog](../src/main/java/local/lito381/server/NativeLitoRentalMerchantCatalog.java)：保留 2010 L1J 商品／價格來源雜湊 | 專用交易規則與入口驗證；`HISTORICAL_OFFICIAL_EXACT=false`，不能稱為官方原價 |

地圖資源的獨立靜態證據：[地圖來源契約](../../../lito脫殼資源/map-table-source-contract.json)、[地圖候選索引契約](../../../lito脫殼資源/map-candidate-index-contract.json)。這兩份契約仍明列執行期資源綁定未知，不能用程式註解的「exact」抹去這個限制。

技能面板、封包與 UI 的更多分析入口：[脫殼資源閱讀入口](../../../lito脫殼資源/閱讀入口.md)。舊 JSON 的部分來源路徑存在編碼損壞；本索引使用實際可讀路徑與現場雜湊，不將損壞路徑當作已驗證檔案。

## 5. 建議補足順序

以下名稱是建議 schema，**本次尚未建表或匯入**。新增表時仍需同時設計載入、驗證與生效時機。

| 優先 | 建議內容 | 欄位方向／前提 |
| --- | --- | --- |
| 1 | `lito_data_source` 來源紀錄 | 以表名＋主鍵＋欄位定位；來源類型、檔案／資源名稱、SHA-256、bin 位址／資源列號、版本、驗證程度、說明。先補現有五張表的來源，不更動玩法值 |
| 2 | `lito_map` 地圖目錄 | map_id、category、name、原始 slot、資源版本、enabled。分開記錄「資源存在」與「目前玩法支援」，不得只新增模式數字就開放 |
| 3 | `lito_map_portal` 傳送配置 | 來源／目的地 map,x,y、raw_tag、enabled、來源紀錄；先與通行圖和場次限制比對，tag 僅保留原值 |
| 4 | `lito_skill_presentation` 技能相容映射 | skill_id、資源版本、施法／飛行／命中特效、BUFF圖示、選取類型；共用技能數值維持 `380_db.skills`，逐項驗證DLL及伺服器消費方式 |
| 5 | 完善既有刷怪／目標物表 | 補地圖專屬波次、明確位置、數量、目標角色／模型；HP、時間及數量屬管理設定或有來源的規則，禁止猜成官方值 |
| 6 | 地圖道具與區域表 | 地圖頁碼、區域名稱／色彩；先完成資源對應核驗，不能由區域顏色自行生成戰鬥限制 |
| 7 | 城鎮NPC／租賃商品表 | 可作後續功能；先確定正式流程範圍與交易生命週期，保留相容來源標籤，不寫回天堂角色／道具資料 |

不建議搬入 SQL 的內容：每格通行位元、完整動畫幀表、封包二進位布局、原生 hook 位址。通行／動畫保留經雜湊固定的資源檔；SQL 保存可管理的覆寫與來源索引即可，避免資料庫膨脹與兩份數據不同步。

目前仍不足以稱為「原版已還原」的內容：各模式精確平手規則、完整 NPC AI／生成表、原服傷害與掉落公式、目標物原始 HP、租賃 NPC 原始 actor／座標，以及程式明列未恢復的攻城補給與恢復圈規則。

## 6. 版本與再核對

- 正式 JAR SHA-256：`bbc59308c43df89ee5029d3b3c802be8ad55fee65c41faca99a76912dd5dce43`，已與 `deployment/release-inputs.json` 核對。
- 共用客戶端 `0-客戶端/LiTo.bin` SHA-256：`ce6ee68ada8fd37e6c096b1a27e46cb6888f1179f85defb5007ac60287360292`，與地圖分析契約列出的 bin 相符。靜態分析的脫殼映像另外有自己的雜湊，位址不能直接當作磁碟檔案偏移。
- 原生正式啟動檢查預期載入 `battle/spr-action-tw13081901.csv`；現場共用庫 `spr_action` 的筆數不等同這份 JAR 資源。不可因舊類別預設值指向 `spr-action-380.csv` 就回退到舊動畫表。
- 變更資料／資源時更新對應索引項；原始碼版本與 JAR class 分別固定，不假設工作目錄所有新原始碼都已正式發布。
- 種子比對使用列內容排序後 SHA-256，包含種子列出的全部欄位及 NULL；比對一致只證明兩份資料相同，不證明來源是原生。
- 本文件及 JSON 是基準封存後新增的維護資料；未重寫 0.6.60 基準 ZIP。重新核對時不要為消除差異而覆寫現場設定。
