# 背包、物品與陷阱的共用物件根

背包／物品／陷阱的共用物件根（2026-10-17，前階段隔離候選）：三種來源父節點、單一World綁定、387個舊方法inverse與兩次乾淨建置一致；3案／52項、兩輪真正TCP各12案／73項／12 frames及12組既有回歸通過。完整物品／背包／陷阱行為與感知／factory仍開放，未部署。

候選SHA `cb229205ec301696c52f137fad90a37ca5d6be4467776b3643494fdd3131dd08`。NativeDeNpcInventory、NativeDeWorldKinds.Item及Trap改為繼承既有NativeDeObjectFields，與來源L1Inventory／L1ItemInstance／L1TrapInstance的L1Object父節點一致。這只資格化角色父節點及必要World入口，沒有冒稱標記類別已擁有完整來源物品或陷阱實作。原生Character仍沿同一父鏈，HP／MP／已知物件／技能欄位沒有複製到物品或背包。

原本只宣告在Character的sourceDeWorldEntry移到共用Object根，保留一份protected volatile Object欄位；Character不留shadow欄位，既有Character欄位／方法在反向還原欄位位置後相同。Inventory內部ID沿用來源預設0，但不得綁入World；真正的非角色物件使用不可變非零dword ID，保留負int表示的unsigned ID。物品／陷阱各有一份private final原生ID。原生setId維持既有不可變邊界，沒有重新加入可任意改ID的source欄位。

原有World身分、object scope／空間索引通知、show mirror、可見物件角色getter、預設onPerceive、generic remove，以及NPC知識的foreign-World驗證，改為辨識共用物件根。17個明列adapter方法對照；14個既有helper變更，387個舊方法在反向還原有限掛點／型別與native ID constructor後相同，新增1個identity guard；其餘原JAR entry、資源與控制檔逐位元保留，沒有新executor。原來的NPC／PC生命週期和來源constructor body不重建。

探針3案／52項在真正memory World與false／true設定下檢查：來源Object初始null map／show-1／單一location、內部inventory zero ID拒收、原5個inventory instance欄位與同一item list／mutex、來源clearItems語意、真正非Character的Item／Trap registry、不可變World綁定、空間查詢與位置移動、show隔離、known cleanup／重新進入AOI、外來非角色item在NPC known新增前拒絕。來源clearItems只標記重量dirty並移除World物件，不清空_items；第一次probe的錯誤預期已依原來源修正，production body未因此改寫。

兩輪真正native登入／遊戲TCP各12案／73項／12 frames，測inventory根、Item及Trap的實際source known cleanup與原生5-byte remove；兩種設定均memory store，包含Integer.MIN_VALUE系列ID，另以完整payload獨立解密比對。ground角色在本probe使用明列的fixture子類，沒有宣稱完整L1GroundInventory producer已實作。新舊相同已編譯control證明舊12f446…World拒絕真正非角色根，新候選接受並在同一location／binding發布。

第一輪World fixture的id(ObjectRef)強制轉Character，在物品根修正後失敗；只把該方法的cast與getId owner兩個operand改成Object根，整個fixture反向還原的normalized位元相同，未修改斷言／case／向量，也未編譯production覆蓋。另一項known測試找出實作缺口：NativeDeNpcKnowledge.admitted仍只檢查Character的foreign-World綁定，讓外來非角色item繞過。已停止確定隸屬本任務的舊測試程序樹，留下失敗與中止證據，補成共用Object根檢查後重新建置／驗證；沒有放寬4秒釋放或任何等待門檻。

12組既有回歸與兩輪constructor各1,912案／27,030項、原向量965f9dcd…僅移除5,428個C.sourceDeWorldEntry=null反射標籤後，逐位元等於新向量6ae5b1b6…；實際欄位已獨立證明移至Object根且只有一份；PC刷新TCP每輪32案／125項／32 frames、NPC對話37案／338項，以及商店22案／23,018項、地形1,573,531項仍通過。731個邏輯圖39,807,952格／reader error0與169份經典圖9,494,528格仍通過。八個class／Java來源宣告輸入與三種父節點由當前1-伺服器凍結比對。

完整L1Item模板／instance、堆疊／重量／count overflow／store／trade／drop、ground onPerceive與insert／update／delete、物品封包與刪除timer／房屋排除、trap觸發與完整技能／材料連動仍未實作。本階段沒有啟用來源信件／家具／玩家附加資料的SQL寫入，相關儲存回呼仍需自有ephemeral狀態或允許的只讀定義。完整NPC approach／appear／searchItemFromAir、hate／AI／PC效果writer與自動factory、全職業技能／Ctrl／範圍及完整原始繼承鏈仍開放。De維持UNSUPPORTED，canonical仍2ad485…；完整目標未完成，未部署，來源服、玩家SQL／WAL及四固定基準未改。

主收據：tools/resource-database/build/native-de-inventory-root-current.json；重驗：python -B -X utf8 tools/resource-database/build/audit-de-inventory-root.py。1998個重複／失敗產物按精確路徑與SHA封存並逐內容驗證後移除，36個輸入凍結保留。完整物品／背包依賴盤點見native-ground-items-development.zh-TW.md及de-ground-items-development/review-current.json；後續仍會使用，不清理。
