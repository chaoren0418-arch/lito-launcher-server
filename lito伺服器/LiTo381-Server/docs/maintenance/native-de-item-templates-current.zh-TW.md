# 完整物品模板數值載入

完整物品模板數值載入（2026-10-17，最新隔離候選）：共用SQL實際5,107模板、23個來源方法／12欄位inverse、32案／15,700項與既有定義78案／6,742項通過。背包物件轉接、Item生命週期與執行器工廠仍開放，未部署。

候選SHA `5c78dfbf5244f37f0576a85789eb1c5467cb79dfe4666c72016e79e629a95bea`，接續靜態能力定義9ee6ece2…；canonical仍2ad485…，未部署。檢查實際native背包後確認RuntimeInventory.Item是不可變record，其InventoryCatalog.Definition只保存封包／既有行為摘要，不能直接當成完整來源模板。來源可變Item的到期與能力重算必須接到同一份真正的背包／戰鬥狀態，不能另生平行物品後只修改自己的欄位。本階段先補齊這個轉接所缺的完整模板。

ItemTable的完整道具／武器／防具載入、箭矢LEFT JOIN與設定驗證、分類、型別、所有setter欄位、fast lookup、名稱及套裝搜尋均沿用來源body。3個createItem方法明列deferred，沒有接入來源IdFactory／World或ItemGenerationModifier，也未以空回呼代替。來源23個載入／查詢方法、12個欄位在反向還原owned get、1個constructor owner、8個載入guard、16個receiver logger、3個static template map、6個唯讀分類map及1個debug logger adapter後精確相同。

三段來源SELECT含etcitem／etcitem_arrow聯結、weapon、armor，共用一筆readOnly=true、autoCommit=false、REPEATABLE_READ交易，三組lease／statement／result set都完成並先rollback、close才發布。共享SQL工具增加明確的三查詢family；原十五查詢family維持預設且不能接受任意SQL。五個既有helper差異包含debug metadata，原方法經有限operand還原後相同；其餘父JAR entry逐位元保留。兩次乾淨建置一致，新增14個class，沒有新executor或控制資源。

共用靜態資料實際載入道具3,321、武器364、防具1,422，共5,107份。來源ItemClass.addList的邊界改為捕捉完整itemId／模式／原classname／來源完整類別名及參數描述，共2,245筆；不載入外部來源class，**這些描述尚不是原生可執行的工廠／物品執行器**。原始來源get／static map保留在test-only oracle，數值載入body未修改，同樣明列執行器metadata邊界。

發布後全部模板是保留原DTO父型別的不可變副類別：道具65、武器82、防具90個setter逐一封鎖，mode及setIds陣列clone隔離；原actor的可變模板DTO API仍保留。所有5,107模板的每個來源欄位，在raw source、離線SQL重播及live JDBC之間一致；另外驗名稱／ID／套裝搜尋、相同fast lookup物件、immutable setter／array／分類map、query／必需列／中途讀取、設定與關閉失敗、logger Exception／Error、舊快照保留、nested owner，合計32案／15,700項。箭矢聯結欄位的JDBC label與CachedRowSet column-name處理不同，測試擷取已保留alias名稱；未為此改寫production來源計算或放寬斷言。

同一份已編譯的上一階段完整定義fixture，在本候選重播78案／6,742項並逐結果相同，證明共享SQL工具的原十五查詢與故障／freeze行為仍保留。沒有冒稱本階段重新執行全部舊TCP／12組回歸。

後續仍需把真正RuntimeInventory的不可變snapshot API與唯一可變來源Item、原生鎖、copy／trade／drop身分及生命週期接妥；完成60個Item方法、PC刷新與記憶體保存回呼、來源status／packet、三個factory與物品執行器／generation modifier，再接入實際戰鬥／技能消費端。完整職業技能、Ctrl／範圍、NPC／ground／factory仍未完成，L1De維持UNSUPPORTED，沒有部署。

來源服、玩家SQL／WAL及四固定基準未改。收據：tools/resource-database/build/native-de-item-templates-current.json；重驗：`python -B -X utf8 tools/resource-database/build/audit-de-item-templates.py`。依精確路徑及SHA驗證封存後清理126個重複／失敗檔案，保留22個凍結輸入及必要證據。
