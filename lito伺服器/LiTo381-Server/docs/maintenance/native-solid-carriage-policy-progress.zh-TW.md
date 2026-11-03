# 堅固防護Source裝備模式外控

2026-10-20 隔離候選：`7c6a29baf443fb0f69f3411b5151422379bc3a9a3ff3291ac260833e03e4a0a0`，接續已驗證技能73候選fb2e26b5。已接入目前1-伺服器的SOLID_CARRIAGE_MODE：盾牌type7可使用，臂甲type13只在mode=1可使用。判斷同時涵蓋canonical native裝備與已選角色的唯讀Source裝備快照；原施法與移除最後合格裝備的檢查共用此判斷。設定併入原CombatRules不可變快照，載入／解析完成後才發布，沒有施法時SQL。

相同編譯fixture前後各160例：兩persistence旗標、WORLD／BATTLE、有無item application，配合無防具／Source盾牌／Source臂甲／真正native已裝備盾牌／真正native未裝備盾牌，以及mode=0／1／2／-1。24次裝備模式偏差歸零；直接走真正skill90 handle，核對效果、既有MP與reuse行為，最後合格裝備移除會取消lease。原mode=1的解密封包逐位元組相同，鎖內寫入零，原生生命週期全數idle且沒有狀態檔寫入。Source臂甲使用共用Source模板的真實ID與圖示建立唯讀裝備快照；目前native物品目錄尚未提供可匯入的臂甲定義，該物品匯入範圍仍開放。早期假設能在native目錄找到臂甲的失敗紀錄保留，沒有新增假物品或放寬production門檻。另11例真正CombatRules.snapshot解析與凍結Source完整_getInt方法一致，含缺值、空白、非法值、整數邊界與溢位。

只改ClientSession的裝備資格方法及CombatRules.snapshot，新增無狀態policy helper。整個PC與CombatRules逆向還原核對通過，其餘entry逐位元組不變，未新增PC欄位、worker、scheduler或ThreadLocal。CombatRules原來源含debug資訊差異，C0比較明確使用整類去除debug／frame後的完整bytecode核對；未把它寫成raw byte-equal。兩次只用凍結輸入／父JAR／library／ASM重建，SHA一致。

本收據驗證裝備模式外控，沒有宣稱技能90的完整Source受理、費用、效果語意或全部生命週期已對齊；失去裝備資格時保留既有native拒絕與費用順序。父版技能73的34組完整回歸屬於fb2e26b5，這裡依精確兩方法delta銜接，沒有將舊測試改標成本SHA的新測試。材料技能仍為24個、其餘34個未完成；Source動作入口仍為先前32個ID。

接續工作：INVIS_CAST_BREAK、Triple_Arrow_Dmg與SLAY_BREAK_NOT_TELEPORT的全部實際消費路徑；其餘職業／目標／Ctrl／範圍、Source Buff刷新與取消、accepted-cast／buffered attacks、NPC／live Item／能力／warehouse／啟動器等既有開放項目。完成最後生產變更後再執行必要560＋60數值、完整階段驗證與精確封存清理。

保留競技／隊伍／TA、記憶體角色／道具／技能／XP，只允許character_lito／settlement持久化。正式核心、固定base、聯合基準、canonical收據、Source專案與玩家SQL／WAL未改；未部署、未清理。父版：[技能73證據](native-magical-weapon-material-progress.zh-TW.md)。
