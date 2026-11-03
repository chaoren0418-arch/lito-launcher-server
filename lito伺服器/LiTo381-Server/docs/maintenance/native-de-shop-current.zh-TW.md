# 來源商店區域與貨幣依賴

來源商店區域與貨幣依賴（2026-10-17，最新隔離候選）：14個來源方法、8個欄位及81個既有方法inverse核對通過；兩次乾淨建置SHA `52b5e76e2dec6bd0adc00aeee3f0df2531458846b584470b6235a27340cca57a` 相同。原本地形isUsableShop座標入口的未封裝依賴已接上，9筆／7圖商店區域以專用只讀SQL載入；22案／23,018項、原地形探針及12組既有回歸、兩輪constructor與實際TCP通過。完整行為／PC刷新／receiver／factory與技能生命週期仍開放，未部署。

MapShopTable與L1MapShopData完整來源主體已投影，僅把原本get()的lazy全域singleton改為已完成owned SQL生命週期的發布快照存取；另加明列的private constructor factory bridge。來源load、checkShop、getCurrency、初始化、lambda與全部DTO方法共14個逐指令對照，8個欄位保留來源型別／屬性；source singleton get未宣稱相同實作。移除一個ResourceDatabase啟動掛點、反向還原NativeDeTerrainV1對MapShopTable的型別後，81個既有方法完全相同。原JAR僅上述兩個helper改動，新增5個helper；原資源、競技場幾何及控制檔逐位元保留，沒有新增executor。

目前共用靜態庫mapids_shop有9筆、涵蓋7圖：0、4、340、350、360、370、800；目前貨幣皆40308。沿用來源SELECT返回次序，區域串列不排序，重疊時checkShop以任一命中、getCurrency以第一筆命中為準。上下界由來源DTO Math.min／max校正；全零矩形是整張地圖的明確sentinel；邊界包含端點。來源isUsableShop(II)Z仍先檢查mapids.usable_shop，再查區域；未自行加入地形可走條件。

載入前設read-only=true、autocommit=false、repeatable-read；一條連線rollback及close成功後才發布。來源load可在局部物件內先完成Map.copyOf，但外層會拒絕任何逐筆／query／result close／statement close／rollback／connection close或必要設定失敗的部分快照。空資料表保留來源語意，發布不可變空表以拒絕所有區域。來源失敗reload保留既有完整快照的行為另行測試；map與每張圖的list均不可修改，DTO沒有公開寫入方法。World查詢只讀快取，不開SQL。

22案／23,018項包含所有DTO getter、填反上下界、零矩形、整數極值、4圖座標網格、重疊貨幣優先序、不可變map/list、空表、失敗reload、open／三個transaction setup／query／row／next／result close／statement close／rollback／close以及logger Exception／Error，並核對scope恢復與SQL關閉。實際靜態庫的9筆資料以邊界內外點比對完整來源Table與舊地形商店入口；兩種旗標的真正memory World測classicAllowed與classicCurrency、show gate及close後拒收，關閉不變動共用定義。

修正前aef971…候選執行真正地形商店座標入口會因未封裝com/lineage/server/datatables/MapShopTable而失敗；相同fixture在新版實際啟動後可命中共用SQL區域，正反對照均保留。兩次乾淨建置相同，14個來源方法與81個舊方法獨立驗證。原169份經典地圖共9,494,528格逐格一致，731個邏輯地圖、39,807,952格實際讀取、reader錯誤0；既有1,573,531項地形探針仍通過。

12組既有來源／感知／World／helper／known／刪除／native／gear／sprite／startup／廣域／同伴driver順序通過；兩輪constructor各1,912案／27,030項，向量965f9dcd…一致。兩輪真正TCP各8案／293項、24個frame，兩種設定皆memory store，另以獨立解密核對。沒有放寬等待或飽和門檻。初次verifier編譯的Java pattern variable重複名稱已修正，失敗輸入與log保留；候選runtime未因此改寫。

本階段完成來源區域／貨幣依賴與已測的classic World查詢，不等同開啟原生玩家商店功能。Shop AI／profile producer與其必要背包／貨幣效果仍待接線；PC權限／LOS／地形writer、門的NPC動作與完整updateObject刷新仍未完成，刷新仍是明列Consumer邊界。完整行為factory、legacy encounter與共用actor銜接、De receiver／onHit／pet／group／death／transport及完整技能生命週期仍開放。先前NPC deadline／reset偶發排空逾時根因仍未證明。De仍UNSUPPORTED，canonical runtime仍2ad485…；完整目標未完成，未部署，來源服、玩家SQL／WAL與四固定基準未改。

主收據：tools/resource-database/build/native-de-shop-current.json；重驗：python -B -X utf8 tools/resource-database/build/audit-de-shop.py。74個重複／失敗產物依精確路徑與SHA封存並逐內容驗證後移除，34個凍結輸入保留，詳見native-de-shop-cleanup-current.json。
