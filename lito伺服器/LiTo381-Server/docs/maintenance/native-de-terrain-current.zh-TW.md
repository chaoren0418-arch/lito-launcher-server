# 來源地形與World覆寫證據

來源地形、通行與World覆寫（2026-10-17，最新隔離候選）：117個來源方法、55個欄位與93個既有方法inverse核對通過；兩次乾淨建置SHA `aef9717416bf2d7d14e21d4daef9ea9065f797bf6bc744b416d8469d4a298e2e` 相同。731筆只讀SQL定義、715份來源地圖資產及169份經典地圖共9,494,528格已核對；更新11張過期經典地圖，保留競技場獨立物理。地形探針1,573,531項、12組既有回歸、兩輪constructor及實際TCP均通過。完整商店座標權限、PC刷新、門的NPC動作、行為／receiver／factory與技能生命週期仍開放，未部署。

以來源L1V1Map、Point、MapData、PerformanceTimer、MapsTable與TextMapReader投影地形與讀取主體。獨立verifier逐方法比對117個來源方法、55個欄位，並移除明列的啟動／World掛點及反向替換22個雜湊常數後，核對93個既有方法。空抽象map／reader parent改用Object，isNull繼承主體與readOne入口另行驗證；MapsTable的靜態資料改由載入範圍持有，不建立全域可變World地形。

目前共用靜態庫mapids有731筆、16筆CopyMapId，對應715份實體來源TXT。專用連線先設read-only、autocommit=false與repeatable-read，rollback及close完成後才發布不可變定義；必要欄位、逐筆或查詢錯誤皆拒絕部分快照，可選is_arrows／CopyMapId欄位缺省保留來源行為。故障測試包含open、query、row、next、rollback與close；修正前02439…候選在row fault後發布部分快照的負向對照已重現。實際載入只用一條owned連線並回滾關閉；戰鬥／AI不查SQL。

全部731個邏輯地圖、39,807,952格已實際完成來源reader與constructor載入，reader錯誤0。715份TXT以原始內容SHA校驗並封裝GZIP，依來源TextMapReader使用signed Byte.parseByte、StringTokenizer、空行／註解、欄列溢出續行與解析錯誤部分資料規則。每個讀取範圍追蹤attempt/open/close，例外與Error後關閉所屬reader、恢復原範圍；巢狀呼叫不關閉外層reader。GZIP建立失敗會關閉原stream，未讀完的資產在close時補驗完整SHA，失敗拒絕發布。

169份經典l38資源共9,494,528格皆與目前來源TXT及SQL範圍逐格相同。11張舊資源確有資料差異：77～82、104、105、108～110；更新其內容及兩個分區目錄的22個source/resource SHA常數。其他原始JAR項目除ResourceDatabase與NativeDeWorldContext兩個有限掛點外逐位元保留。原生ArenaMapCatalog、ArenaPassabilityCatalog及S32不變；實際測試504號地圖在CLASSIC與ARENA域維持不同物理，呼叫端必須明確選擇域。

World按domain/show/map持有lazy地形與獨立可變陣列，門狀態在source ConcurrentHashMap覆寫層，箭線例外在source boolean陣列。兩種門均作用三格、開門移除覆寫還原原值，動態關門優先於箭線例外；靜態tile=3仍是方向格。source copy constructor複製底圖／門／箭線陣列但不複製權限旗標的行為亦保留。探針窮舉256×256×8方向、0x80、standing與PC/NPC共用語意，測讀檔及SQL錯誤、Point與完整public reader，並於真正World驗證不同show／World隔離與close後拒收。

完整來源AI實際使用NativeDeTerrainEnvironment與共用actor：合法出生點不搬動，關門使出生點阻塞後由來源checkSpawnLocation搜尋並透過真正World relocation修正位置，位置revision增加一次；既有worker與共享credit正常退休。兩種設定均使用memory store。兩輪原constructor各1,912案／27,030項，向量965f9dcd…一致；兩輪真正TCP各8案／293項、24個frame並獨立解密。12組既有driver通過，未放寬等待門檻。初次補測誤用重啟AI，來源只在首次tick檢查出生點；改用全新actor驗證，未改來源規則。

本階段僅資格化已測的通行／門覆寫／箭線API及地形provider。保留來源isUsableShop(II)Z的位元碼仍引用未封裝MapShopTable，該座標商店入口明確未資格化，須接續移植MapShopTable／L1MapShopData及mapids_shop快照；不能據此啟用完整地圖權限。來源isDoorPassable的NPC開門動作未移植；完整PC updateObject刷新仍是明列Consumer邊界，所有permission／LOS／terrain writer與movement producer尚未接完。Point投影亦未宣稱原始物件繼承階層完成。

完整行為／profile工廠、legacy encounter與共用actor銜接、De receiver／onHit／pet／group／death／transport、所有writer及完整技能生命週期仍開放。先前NPC deadline／reset偶發排空逾時根因仍未證明。De仍UNSUPPORTED，canonical runtime仍2ad485…；完整目標未完成，未部署。來源服、玩家SQL／WAL與四固定基準未改。

主收據：tools/resource-database/build/native-de-terrain-current.json；重驗：python -B -X utf8 tools/resource-database/build/audit-de-terrain.py。7832個舊嘗試／重複檔案依精確路徑及SHA封存、逐內容驗證後移除，38個凍結輸入保留，見native-de-terrain-cleanup-current.json。
