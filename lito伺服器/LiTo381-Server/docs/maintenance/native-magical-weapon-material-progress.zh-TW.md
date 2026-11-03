# 技能73材料、物品效果及Main生命週期驗證

2026-10-20 隔離候選：`fb2e26b5ef4b7304d45904a15d32558c1a314c53d07138573ff3e171d8d92555`。技能73的40318×10材料消耗與真正RuntimeInventory.createMagicalWeapon物品強化，已合併到同一筆canonical staged記憶體交易。GM可免材料，仍須提交物品效果；保存失敗時背包、強化、MP與reuse保留原狀，僅回299並可立即重試。累計24個材料技能、27個本材料系列已審閱ID，連同既有Light與四個自身技能共32個Source入口ID；這些數字僅表示受理與提交範圍。**其餘34個材料技能、全部Source語意與完整目標仍未完成；未部署。**

同一份真正PC fixture前後各88例：兩persistence旗標、WORLD／BATTLE、有無item application，涵蓋正常／共用Source時間窗／缺料／不存在物件／已強化／MP不足／非武器／未裝備／未鑑定／不可安全強化／材料堆疊耗盡。時間窗繞過8→0，原正常與物品變體解密封包相同；888／792 frame，鎖內寫入零。真正保存前故障8／8例的部分提交8→0；GM前後40／40例、無材料GM保存故障8例均通過。16例已受理STOPPED及32例角色／場景／首次socket失敗邊界通過，不重扣、不重播，receipt與FIFO排空。

實際Main共8例：一般角色、GM、政策讀取失敗、政策等待中角色切換，各含正常關閉與中斷。政策等待時資源不提前關閉，新封包不能取得受理；成功提交一次，失敗不扣料／MP／強化，角色切換後零舊封包。所有案例資源僅關閉一次，關閉後repository呼叫零，client executor與原生生命週期終止，handlers與pending歸零。Main／SourceLogin只使用既有七個記憶體／loopback／觀察替換點，整類inverse核對通過，沒有PC覆蓋或真實玩家SQL。

Main首次角色切換反例揭露：批次在政策回呼後才入列，原publication lease可能捕捉新角色。修正既有Batch.run，對每個輸出重核對原始accepted binding；其餘輸出及物品授權機制保留。相對前一技能73候選，JAR只改Batch entry。相對已驗證父版df9464f0，總計五個changed、兩個fresh entry；完整PC／Light／Batch／material core／Cast inverse、精確欄位與凍結C0／C1比對通過，其他entry逐位元組一致，未增加PC欄位、worker、scheduler或ThreadLocal。

先前武器、立方體、自身、龍騎士與材料選取、GM、故障、停止控制均使用本SHA重新驗證。34個依序執行的driver通過，含完整20組、生命週期14組及每個實際子證據、既有控制10組。兩次只使用凍結輸入／父JAR／ASM／library重建SHA一致；2177項進度證據稽核通過。靜態普查仍有45個方法／46次舊material呼叫。

目前1-伺服器與前次凍結內容的差異已逐檔保留：L1SkillUse完整不變；L1PcInventory新增道具持久化、補償、升級扣除與quarantine處理，但完整appendGeneralConsumePlan選取器不變；L1ItemInstance只新增護甲期限初始化；CharacterTable只更動貢獻值快照與載入。Source材料選取與角色AccessLevel判斷保持一致。Source的玩家道具持久化不複製到LiTo記憶體角色生命週期；安裝後補償／quarantine完整對齊仍開放。

首次完整回歸在正式檔保護核對失敗：前階段收據使用c02aaae8，但目前正式檔75da69b0屬於本次工作前的0.6.69發布，release-inputs與正式啟動腳本均一致。發布前備份pre-0.6.69-buff-pages-20261018.zip的原路徑索引與entry內容完整保留c02。新驗證對該歷史路徑／SHA使用精確凍結舊內容，同時獨立保護目前75／發布收據／啟動腳本／共同目錄，沒有忽略不匹配或改標runtime。其餘錯誤不允許套用這個銜接。舊失敗與後續回歸均保留。本次未改正式檔；既有0.6.69的LegacyMySqlRepository／SourceBuffRead變更併入實驗候選仍列為最終發布前開放工作。

仍需完成：其餘34材料技能與全部職業／Ctrl／目標／範圍／NPC／item／trigger；Source Buff訊息、刷新／取消、其他producer時鐘、accepted-cast state與buffered attack取消；opaque／long數量與執行中GM傳播；NPC工廠、live Item／能力／warehouse／canonical SQL／SharedResourceMain／launcher UI及Source外控。外控已讀取的值不能當成所有消費路徑完成。下一批需核對SOLID_CARRIAGE_MODE、INVIS_CAST_BREAK、Triple_Arrow_Dmg與SLAY_BREAK_NOT_TELEPORT等當前Source消費位置。最後生產修改後仍需必要560＋60數值重播、完整階段稽核及精確路徑／SHA／內容封存清理。

保留競技／隊伍／TA與記憶體角色／道具／技能／XP；只允許character_lito／settlement持久化。正式核心、固定base、聯合基準與canonical收據未改，未寫Source專案、玩家SQL或WAL。失敗反例與本階段依賴仍保留，未清理。

進度收據：`tools/resource-database/build/native-magical-weapon-material-progress-current.json`。稽核：`python -B -X utf8 tools/resource-database/build/audit-native-magical-weapon-progress.py`。父版：[武器附魔材料](native-item-weapon-material-progress.zh-TW.md)。
