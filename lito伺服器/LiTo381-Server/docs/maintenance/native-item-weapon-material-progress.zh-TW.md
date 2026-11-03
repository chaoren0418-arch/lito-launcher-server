# 武器附魔材料與動作提交施工紀錄

2026-10-18 隔離原型：`df9464f06c41821f3abf7c29ccc5876abe1ead66e570a40308f1274e90521a37`，以立方體原型 `218d54c4…` 建置。擬似魔法武器12（無材料）與暗影之牙107接入既有Source動作／reuse／材料交易／輸出FIFO，累計23個材料技能、26個本材料系列已審閱ID；連同原有Light與四個自身技能，31個ID已接入相應Source入口。**這是入口與提交範圍，並不代表這些技能的所有Source語意、全部職業／Ctrl／目標／範圍／item／trigger或整體目標已完成。其餘35個材料技能仍開放，未部署。**

保留真正PC原方法的費用、選取檢查、既有數值／效果區段與全部九個CombatWeaponBuffs橋接呼叫。Source目前將12／107作為玩家Buff，依已裝備武器判斷，舊選取物件不再決定Buff目標；未裝備武器仍保留原受理費用與錯誤封包。移除舊的copy／consume／replace／save片段，改由原材料交易完成後讀取canonical inventory，輸出使用原交易的凍結前後快照。沒有新增PC欄位、worker、scheduler或ThreadLocal；只改PC與既有Light dispatcher兩個entry，新增一個無狀態helper。一般item／lifetime／SQL／Source費用與timeline類別逐位元組不變。

同一份編譯fixture在父版與本版各96例，涵蓋兩persistence旗標、WORLD／BATTLE、有無item application、兩技能及正常／Source共用時間窗／缺料／不存在物件／未裝備武器／MP不足。16次時間窗繞過歸零，正常與物件／裝備變體的解密封包維持逐位元組相同；680／560 frame，鎖內寫入零。武器的object ID、template、數量、裝備與owner欄位保持正確。既有canonical replacement會採用staged BoundItem並退役舊actor，沒有宣稱沿用同一Java actor instance。

真正GameStateStore保存前故障前後各8例，父版8次材料／MP／Buff／reuse部分提交歸零；新版保留背包與原economy，只回299，取消Source provisional tokens且可立即重試。原item-weapon方法在save之後才輸出，因此父版此故障位置為零frame，新版8個299 frame。16例preaccepted STOPPED一次完成已受理施法，新施法、一般item與Source setter仍拒絕；64例涵蓋提交後退役、排隊角色／場景汰換與首次實際socket失敗，不重扣或向替換角色送舊封包，所有receipt／FIFO排空且鎖內寫入零。

首次材料政策讀取會阻塞。補充前後各32例在真實handle入口等待180ms，直接檢查真正CombatBuffLeases的deadline：16次壽命被等待時間吃掉的問題歸零。Source L1SkillUse在施放效果處呼叫setSkillEffect，L1Character當時建立並begin技能timer；完整四個來源檔已凍結。本版在成功通過Source action與材料提交後刷新原castNow local；相對未校時原型 `76f725dd…`，只有PC entry中的一次System.nanoTime與LSTORE23增加，獨立整類inverse及其他entry逐位元組比對通過。另含政策讀取中STOPPED、provider失敗／立即重試及兩封包排隊，各版40次政策讀取；原邊界解密封包保持一致。provider查詢及完整清理位於hub／state／lifetime monitor之外。

未校時原型的25組驗證保留在獨立目錄，校時版使用另一目錄與真實SHA重新執行。既有立方體192／192、保存故障32／32、STOPPED32與邊界128；立方體政策128／128及真正Main正常／中斷16例；自身384／384、龍騎士192／192、STOPPED128／32、角色／場景／輸出邊界512／128；Counter Magic、封印、GM政策40、Source選取器171、過期副本、授權與停止控制均通過。完整20組、生命週期14組及各實際子證據、既有控制10組均指向本SHA；兩次只使用凍結輸入／父JAR／ASM／library的重建一致。1798項進度稽核通過。所有原生fixture依序執行。

建置與fixture早期反例保留：Java pattern local命名衝突；執行期目錄不支援的測試武器4；把BoundItem staged replacement誤當成同一Java instance；誤沿用Counter Magic的固定MP與故障前送包假設；零frame摘要空行處理。已依實際目錄24、獨立Source費用計算、原方法保存／輸出順序及既有owner contract修正測試。沒有為測試放寬production判斷、斷言期限或停用原生生命週期。

武器時鐘工具摘要沿用cube描述標籤；另以凍結的真正NativeItemWeaponClockProbe／107入口、32／32觀察及全部528個frame獨立核對實際範圍，原native manifest／觀察與斷言未改，未因文字標籤重跑native。

下一入口73會修改物品強化值，必須與材料放在同一筆canonical staged交易；不能直接套用先扣料的自身Buff提交。其餘35個材料技能、46個仍有舊material呼叫的方法／47次呼叫、目標／範圍與全部producer仍需逐一完成。武器Buff的Source訊息、刷新與取消等其他語意仍待另外核對。保存後安裝異常回復／quarantine、opaque與long數量、執行中GM政策、Source accepted-cast state與buffered physical attack cancellation，以及先前NPC／live Item／能力外控／warehouse／canonical SQL／SharedResourceMain／launcher與UI工作仍開放。最後生產變更後仍需必要560＋60數值重播、完整階段稽核及精確SHA／路徑／內容封存清理。

保留競技／隊伍／TA與記憶體角色／道具／技能／XP，只允許character_lito／settlement持久化。正式核心、固定base、聯合基準、canonical收據、Source專案與玩家SQL／WAL未改。所有輸入、反例與原始紀錄尚保留，未清理。

進度收據：`tools/resource-database/build/native-item-weapon-material-progress-current.json`。核對：`python -B -X utf8 tools/resource-database/build/audit-native-item-weapon-clock-progress.py`。父版見[立方體材料紀錄](native-cube-material-progress.zh-TW.md)。
