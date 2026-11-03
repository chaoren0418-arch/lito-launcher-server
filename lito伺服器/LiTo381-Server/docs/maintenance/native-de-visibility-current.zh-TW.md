# 來源PC可見物件刷新核心、計時與血條

來源PC可見物件刷新核心／計時／血條（2026-10-17，最新隔離候選）：52個來源方法（50新增、2既有）與234個舊方法inverse核對；兩次乾淨建置SHA `7d370ac72ddf3d9a80f77fec6199388a93f8d47ce08ed67a6cc553c8b8d9bc0f` 相同。22案／107項、兩輪實際TCP各32案／125項／32 frames，以及12組既有回歸通過。其他NPC感知／隱藏處理、PC效果writer、自動場景factory仍開放；完整PC刷新與完整目標尚未完成，未部署。

完整投影VisibilityCoordinator 9方法、UpdateObjectCheck 3方法、PcUpdateObjectTimer 9方法、PC enum switch初始化1方法、PC刷新／known清理4方法、通用封包安全寫入閉包15方法、S_HPMeter 5方法與Character血條4方法，共50個新增來源方法；另外逐指令證明既有ViewerHpState 2方法仍與來源相同。39個具來源owner／opcode／descriptor的明列actor別名保留對照。原JAR只改NativeDeCharacterFields、NativeDeWorldContext、NativeDeClientViewer、NativeDeTerrainEnvironment四個helper，新增24個helper；234個舊方法在反向移除掛點後完全一致。原資源、控制檔及executor不變。

來源known清理先處理已銷毀／未註冊NPC及NORMAL_RACE／STALL／LEAK分類，再檢查map／show隔離與recognize距離；完成清理後走已投影的原World空間索引forEachNearbyObject。原來源status 1／2／3可刷新，UpdateObjectCheck會拒絕offline／無connection／teleport。修正原生bindViewer初始化source onlineStatus為0而被來源timer跳過的問題，實際登入後設為1。真正native PC每次刷新會以當前ClientSession位置同步同一viewer與空間索引。地形環境新增兩參數建構入口，預設呼叫完整來源刷新；既有三參數Consumer仍保留。

來源300ms計時器以既有retry／worker pool執行，不新增executor。週期票據與最多一個排隊／執行中的工作共享同一個pending credit；63個外部credit下仍可用最後1個名額啟動與執行，總上限64。同步刷新、啟動／重啟及worker交接都有生命週期收據；queued close移除排隊工作並釋放，running close直到finally才釋放。場景admission gate不跨來源callback或網路I/O。明列start／stop入口已測，尚未自動接上完整場景factory。來源GeneralThreadPool整個類別沒有宣稱移植。

Character血條沿用同一個_secHpByViewer快取與來源ratio／去重／1024容量／hide255／清除行為。S_HPMeter沿用來源封包內容與padding，原生opcode為104，保留hide255；普通Protocol.hitRatio會clamp到100，故不能代替此來源封包。真實PC的HP／maxHP在既有NativePlayerCommit.Queue.vitalLock內讀取ClientSession當前值，沒有複製成另一組actor HP。TCP probe特別讓viewer scaffold maxHP為0、真正native HP為50／200，仍收到25%。非NPC remove使用原生5-byte封包，包含負Java int對應unsigned object ID；輸出僅持native PC monitor，拒絕closed／retired／foreign binding。

22案／107項probe涵蓋known先清理再AOI、未註冊NPC leak計數、map／show／status隔離、GM2001血條、同一快取去重與hide255、來源例外繼續與Error scope恢復、Merchant／Dwarf／Field早退、hidden分支routing、map400 follower master identity、Item非Character角色判斷、三種repair enum、queued／running close與實際300ms callback。其他NPC與hidden分支使用typed fixture service，只證明來源分支與參數，不能宣稱真實producer已完成。

兩輪真正TCP各32案／125項／32 frames，兩種設定均memory store；以實際靜態庫有效mob skill ID建完整De，經預設地形刷新出現／known登記，移動真正native PC離開與返回，檢查remove／reappear，再由真實300ms timer送remove。另測HP去重、當前PC HP、generic item remove；第二輪NPC ID使用Integer.MIN_VALUE。所有frame另以獨立解密公式核對，14個production類別code origin皆為本候選JAR。相同已編譯control在52b5e7…舊候選證明source status=0且缺updateObject入口，在新候選證明source status=1且真正TCP送generic remove。

原12組來源／感知／World／helper／known／刪除／native／gear／sprite／startup／廣域／同伴driver順序通過；兩輪constructor各1,912案／27,030項、向量965f9dcd…相同。商店22案／23,018項與地形1,573,531項仍通過；731個邏輯地圖共39,807,952格讀取、reader error=0，169份經典地圖共9,494,528格仍與來源一致。未放寬等待或飽和門檻。首次application使用固定73010但當前靜態庫沒有該mob skill，已修正fixture選真正存在的ID；首次wire capture在decrypt後才記seed，已改為先記unsigned seed。repair probe寫final clock失敗，改用既有fixture反射工具；這些修正未改候選production runtime，失敗證據一併封存。

本階段僅資格化已列出的PC刷新核心／De實際感知／計時／HP入口。預設其他NPC onPerceive與hidden approach仍明確拋UnsupportedOperationException；完整NPC感知／chat／ground-item及hidden reveal、PC skill-effect snapshot真實writer、原始角色繼承／自動場景factory、權限／LOS／地形writer與門動作仍待實作。GM2001 fixture注入不是writer資格證明。De AI／receiver／onHit／pet／group／death／transport、完整技能生命週期及全部職業／Ctrl／範圍仍開放。先前NPC deadline／reset偶發排空逾時根因仍未證明。De維持UNSUPPORTED，canonical runtime維持2ad485…，完整目標未完成，未部署；來源服、玩家SQL／WAL與四個固定基準未改。

主收據：tools/resource-database/build/native-de-visibility-current.json；重驗：python -B -X utf8 tools/resource-database/build/audit-de-visibility.py。633個重複／失敗產物依精確路徑與SHA封存並逐內容驗證後移除，43個凍結輸入保留，詳見native-de-visibility-cleanup-current.json。
