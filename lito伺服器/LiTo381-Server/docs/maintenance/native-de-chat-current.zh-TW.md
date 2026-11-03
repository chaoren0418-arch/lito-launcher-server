# 來源NPC對話資料、排程與原生輸出

來源NPC對話資料／排程／原生輸出（2026-10-17，最新隔離候選）：75個來源方法、126個既有方法inverse及兩次乾淨建置一致；37案／338項、兩輪實際TCP各32案／117項／28 frames與12組回歸通過。隱藏解除、地面背包、完整NPC／PC感知與factory仍開放，未部署。

候選SHA `12f446420be6a56f06dc666acde94907321c30c574977a21277587116712db5b`。來源NpcChatTable（lazy singleton get改為已發布快照存取）、完整L1NpcChat、完整L1NpcChatTimer與RunSequenceTask、NPC startChat／scheduleNpcChatTimer／cancelNpcChatTimer、必要的單封包WorldBroadcast閉包與完整World.broadcastPacketToAll，以及三種NPC對話封包全部來源主體，共75方法逐指令反向核對。12個來源呼叫別名明列owner／opcode／descriptor。四個既有helper改動：ResourceDatabase、NativeDeNpcConstructionState、NativeDeWorldContext、NativeDeVisibilityOutput；126個舊方法移除明列掛點後相同，新增23個helper，原資源與控制檔逐位元保留，沒有新增executor。NPC既有_monitorFuture沿用同一個來源volatile欄位。

實際共用靜態庫npcchat有26筆有效定義。完整來源SELECT與DTO讀取、四種timing分流、重複key最後一筆優先、未知timing忽略，以及game-time all／array存取沿用來源主體；每個table綁定自己的載入資料，SQL成功完成後map凍結。DTO保留來源setter，執行端僅讀取，未宣稱DTO語言層不可變。專用連線設read-only、autocommit=false、repeatable-read；rollback／close成功後才發布。逐筆／query／next／result／statement／rollback／close與logger Exception／Error等失敗均拒絕部分快照；空表合法，快照彼此獨立。執行中對話不開SQL。

來源runPending、coalescing、最多8次dispatch retry、五句對話與間隔／repeat計時，以及appear／dead／hide／game-time條件都保留在來源方法。原生adapter使用既有worker／retry pool及共用pending credit，週期producer、worker與延遲子工作交接同一收據，上限64。排隊close會呼叫來源discardOnShutdown並清runPending；執行中close保留credit到finally；actor刪除及World關閉回收延遲工作與owner slot。來源World空間索引決定一般8格與大喊50格範圍、map／show隔離，世界廣播沿用來源connection與online status分支。未宣稱完整GeneralThreadPool或自動game-time NPC producer已移植。

來源NPC對話封包使用opcode161且缺原生接收端需要的座標。原來源建構與文字格式保留，封包另有同一subject的metadata；輸出邊界解析來源頻道／ID／字串，核對來源ID，再呼叫既有Protocol.sayCode轉成原生opcode45及當前共享座標。沒有新增客戶端opcode。一般、無名稱、有名稱、PC來源、大喊與全服格式均測中文及unsigned負int ID。原生ByteCache同步轉為原生payload，避免getContent先定稿後prepareForWrite沿用來源161；原來源getContent保留為private raw方法供轉換。真正PC monitor內另驗證actor／World已退役，拒絕刪除後的延遲輸出。

開發探針37案／338項包含SQL生命週期失敗、四種timing生死條件、隱藏抑制、local／shout／global fanout、來源中文與座標、預先定稿快取、63個外部credit下repeat共享最後1格、queued coalesce／discard、running close與四個延遲子工作取消。兩輪真正native登入／遊戲TCP各32案／117項／28 frames，false／true兩種設定均memory store；兩輪payload逐位元獨立解密一致，真正輸出hook拒絕actor刪除後送包。封包採原生實際長度，沒有沿用舊測試的8-byte對齊假設。

12組既有回歸、兩輪constructor各1,912案／27,030項且向量965f9dcd…一致；上一階段PC刷新TCP每輪32案／125項／32 frames仍通過。商店22案／23,018項、地形1,573,531項、731圖39,807,952格／reader error0，以及169份經典圖9,494,528格仍通過。新舊對照用同一份已編譯程式確認舊7d370a…缺完整NPC startChat及catalog，新候選有入口且owned SQL載入26筆後關閉。第一次全服分支漏映射ClientExecutor，實際probe找出後已補NativeDeWorldConnection；初次wire driver沿用padding假設已依實際原生frame修正為完整相等檢查；失敗產物保留於精確封存。

完整感知依賴盤點包含34類、21個onPerceive覆寫與120方法／90欄位／323呼叫，68個class／Java輸入凍結後重跑得到相同依賴清單。來源approachPlayer／appearOnGround／searchItemFromAir需要ground inventory／item、generic NPC pack、hate／AI等完整鏈，本階段只完成其中必要的對話依賴。完整非De NPC／PC感知、隱藏解除、PC skill-effect writer、自動game-time事件與場景factory、原始hierarchy及完整receiver／AI／death／transport仍開放；De仍UNSUPPORTED。全部職業／Ctrl／範圍與完整技能生命週期尚未完成，canonical仍2ad485…，未部署；來源服、玩家SQL／WAL及四固定基準未改。

主收據：tools/resource-database/build/native-de-chat-current.json；重驗：python -B -X utf8 tools/resource-database/build/audit-de-chat.py。788個重複／失敗產物依精確路徑與SHA封存並逐內容驗證後移除，42個凍結工具／來源輸入保留；完整NPC感知仍會使用的依賴盤點保留於de-npc-perception-development，尚不清理。
