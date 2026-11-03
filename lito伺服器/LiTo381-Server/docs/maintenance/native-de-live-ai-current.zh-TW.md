# 來源AI主體與原生排程生命週期

來源AI排程、active／background主體與原生生命週期（2026-10-17，最新隔離候選）：46個來源方法與351個既有方法inverse核對通過，兩次乾淨建置SHA `04e228cde089b649108c77b646ce2b87bba2960c5762a866924a8b9d7cdb7d15` 相同。來源tick已接到同一實體與既有worker／timer，包含額度交接、關閉receipt及刪除退休；兩輪實際TCP各8案／293項、24個frame，12組既有回歸通過。完整行為／terrain／receiver／factory與技能生命週期仍開放，未部署。

同一個完整De實體現在使用來源NPC的owner token與generation，以及原有De排程欄位；沒有另建timeline狀態副本。新增來源getName覆寫、sprite速度設定與出生點搜尋主體，NPC的四個owner方法與calcSleepTime、Character兩個狀態getter、來源worker task的constructor／run／discard共46個方法，均以獨立inverse核對。351個既有方法保留；36個scope入口只同步轉呼完整來源主體，巢狀返回或Exception／Error會還原兩個subject。來源兩個byte[8]方向表、捕獲參數與本地方法／欄位ABI另行檢查。

native jobs使用現有兩worker與retry timer，在共享64額度設定下；同一實體的timer→worker／worker→timer以reference transfer持有同一credit，容量全滿時不額外索取第二份。queued、timer與running均有receipt；關閉先停producer與來源owner，running callback完成後才釋放。物件退役的timer帶來源捕獲owner，避免讀取後來的世代。完整來源刪除外層增加finally bridge，只在deletion已開始或destroyed後退休native binding；普通stop仍可restart。所有角色及背景manager皆屬於單一World，關閉清除namespace。

實際application lifecycle探針25案／181項涵蓋stop→start single-flight、stale timer、64額度全滿的真實timer／worker交接、queued取消、running close保留receipt、Exception／Error後重用worker、paralysis／sleep欄位、完整刪除及未註冊物件的timer退役，並等候真正10秒背景timer發出source tick。native背景manager依來源dead guard移除角色；它是有額度與關閉機制的native adapter，未宣稱原DeManager整個class已移植。

兩輪真正TCP application各8案／293項、24個cipher frame；ai_type=3依來源規則由感知入口啟動完整active tick並安裝下一個timer，經完整初始化外觀更新後刪除及退休AI。出現／外觀／刪除封包另行獨立解密。兩種statePersistenceEnabled設定皆使用實際memory store；actor tick只查已載入的sprite與控制快取，沒有戰鬥SQL。

原本1,912案／27,030項constructor測試bytecode未修改；oracle只加入這次來源getName覆寫，新增方法逐指令對照、其餘62方法與43獨立來源欄位保留。兩輪向量965f9dcd9b311848dfbb87a470cf91e6e33f33827c21361fc4a89e87453e7ddd一致。既有12組driver順序通過：來源NPC／template、感知、World、51,635案／1,049,774項helper、known、刪除、10,783案native、裝備與兩種啟動、48套／1,728案廣域及887案同伴。

新控制檔combat/de-ai-controls.properties只含目前來源DeWakeupEnable=true、DeWakeupInterval=180、DeWakeupDuration=15，與凍結來源逐值核對。五個既有helper改動、21個新helper、一份控制資源；原生class變更0，沒有新增executor。正確64額度旗標下，舊b72候選會因背景heartbeat額外索取credit而失敗，修正後25案／181項通過；背景轉交credit，滿額掃描保留角色等待並預約後才發handoff。初期探針未明確設定實際global／target上限，已補旗標與effective-capacity斷言；該輪不作容量飽和證據。早期兩個ABI映射錯誤、測試把attach前revision誤設為基準，以及刪除後binding未退休的失敗均保留。實際感知測試最初ai_type=0不會依來源啟動AI，已明列改測ai_type=3；沒有放寬原有等待門檻。

完整combat／shop／fishing／war／berserk／deathmatch／idle行為與profile factory、具體地形passability與viewer refresh、夥伴及自動場景producer仍待完成；當前Environment與behavior明列為typed邊界，spawn演算法雖使用真正World relocation，尚未證明完整地形等價。legacy encounter control-only AI仍須與這份common actor銜接，完整De receiveDamage／death handoff與全部writer／技能生命週期仍開放。先前NPC deadline／reset偶發排空逾時根因仍未證明；本輪通過不代表已修復。De仍UNSUPPORTED，canonical仍2ad485…，完整目標未完成，未部署；來源服、玩家SQL／WAL與四固定基準未改。

主收據：tools/resource-database/build/native-de-live-ai-current.json；重驗：python -B -X utf8 tools/resource-database/build/audit-de-live-ai.py。2850個舊嘗試／重複檔案逐SHA封存驗證後精確移除，33個凍結輸入保留，見native-de-live-ai-cleanup-current.json。
