# 奪命之雷傳送外控

2026-10-20 隔離候選：`363ceed9643540e4845e66a8a5098c0dc6fcb8e27e314d0c5e34785ad80992a9`，接續堅固防護外控7c6a29ba。SLAY_BREAK_NOT_TELEPORT已加入原CombatRules不可變Source設定快照。Source的THUNDER_GRAB只限制移動；針對技能5／131，保留其他睡眠、凍結、麻痺、暈眩、骷髏毀壞及競技資格檢查，排除僅移動束縛造成的全施法拒絕。其餘技能與移動仍走原有鎖定。未載入Source設定時保留原native語意。

政策開啟且仍有奪命之雷時，沿用原受理的MP與reuse扣除，再送出Source中文錯誤、傳送解鎖及MP封包，不建立portal、不清除原有pending ask；政策關閉則依原傳送尾段建立目的地。Source技能在runSkill之後仍會useConsume，因此沒有把政策拒絕改成免費的受理前拒絕。本階段只對齊這項外控與束縛分類，並未宣稱技能5／131全部Source費用、取消、失敗語意均已完成。

同一編譯fixture以前後真實JAR各256例執行：兩persistence旗標、WORLD／BATTLE、有無item application、技能5／131、政策開關，搭配無束縛／有效奪命之雷／合併睡眠／已到期／MP不足／觀戰／不合法目的地或停用相容性／單獨睡眠。32個外控差異歸零；另外9例實際snapshot解析與凍結Source完整_getBoolean一致。無束縛、到期及完整麻痺路徑的解密封包逐位元組相同。共448／648個實際封包獨立解密核對，鎖內寫入零，沒有production PC overlay，所有lease及生命週期收束，沒有狀態檔寫入。初次fixture誤用非可站立的測試座標，失敗紀錄保留；改用真實競技spawn／世界樹座標及server bookmark，未更改production或放寬斷言。

精確delta為ClientSession三個原方法、BattleTimedStatus新增傳送專用分類方法、CombatRules.snapshot及無狀態helper。整類重新產生與獨立PC／status／CombatRules逆向還原通過，其餘entry不變，沒有新增PC欄位、worker、scheduler或ThreadLocal。兩次只用凍結輸入重建SHA一致。完整34組回歸仍屬祖先fb2e26b5，這裡僅報告本SHA的針對性測試及精確delta，沒有把舊回歸改標。

材料技能進度仍為24個，其餘34個及完整目標／Ctrl／範圍尚待完成。接續INVIS_CAST_BREAK、Triple_Arrow_Dmg、Source Buff刷新與取消、各技能完整Source受理／費用／目標、accepted-cast與buffered attacks、NPC／live Item／能力／warehouse／啟動器，以及既有正式0.6.69讀取新增類別的語意合併。最後生產變更後仍需560＋60數值與完整階段驗證。

保留競技／隊伍／TA及記憶體角色／道具／技能／XP；持久化限制仍為character_lito／settlement。正式核心、固定輸入、Source、玩家SQL及WAL未寫入；未部署，未清理。[技能73生命週期與完整回歸](native-magical-weapon-material-progress.zh-TW.md)；[堅固防護外控](native-solid-carriage-policy-progress.zh-TW.md)。
