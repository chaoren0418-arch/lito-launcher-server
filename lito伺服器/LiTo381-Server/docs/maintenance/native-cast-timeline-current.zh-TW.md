# 原生光術的來源施法時間窗、入口與角色生命週期

2026-10-18 隔離候選：`aa700801efd51e049a13217c34b76b3ddcb35678b973abfb2797550ac988d2ff`。在實際光術 skill2 消費端接入目前來源的時間窗／入口 context／admission 排序與速度倍率核心，修正只等10ms reuse就能在1480ms動畫內再次施法的缺口。**完整 Source cast admission、全部職業技能與整體目標仍未完成，未部署。** 正式核心、四個固定基準、來源專案、玩家 SQL 與結算 WAL 沒有由本階段改寫。

實際 socket 迴圈在原 Protocol.readFrame 完成後、解密與 capture 紀錄之前保存不可變的 nano／epoch ingress；實際 handle 在原 lifetime wrapper 外安裝並還原該入口。每個真正 PC 只有一個 final State，保存完整投影的 Source ClientActionTimeline。時間窗錨定既有 canonical role Owner：戰鬥 epoch 更新保留同角色窗口，同名角色替換得到新窗口。沒有新增 HP／MP 權威、worker 或 timer。

來源 ClientActionTimeline、PacketExecutionContext、SkillCastAdmission 與速度核心共15個 class 做完整 inverse；速度 class 只排除兩個依賴 Source L1Character 的 actor adapter，其純計算、enum與record保持原樣。來源 SprTable 的動畫換算使用整數截斷，與舊原生 Math.round 的差異有明確 golden cases。九個目前來源原檔與輸入雜湊保留；沒有執行來源伺服器的 L1PcInstance。參考速度矩陣執行未修改的來源純函式，再與候選中的投影函式逐項比較；編譯用 Source character 簽名 stub 不放入候選，也不建立角色或執行其方法。

原光術的 selector／狀態拒絕、數值、timed status與完整輸出 body 保留，Source admission插在第一個原 mutation之前；Light無必要材料的分支使用無材料 callback，這不代表已完成其他技能的材料交易。reuse的原數值時基在拿到真正 action lock後更新。原隱身／motion／effect／sound／light／MP仍透過既有receipt與FIFO輸出。光術入口跳過舊的前置 Wind Shackle預算，改由同一Source admission套用速度倍率；其他技能仍呼叫原Wind Shackle body的直接alias，保留例外identity。

光術進入 Source action／drain前暫停原生 packet context，結束後還原；來源 PacketExecutionContext僅圍住純admission。既有 accepted HP context保留原16個FIELDS規則與測試，另以instance slot在原清空成功後detach新packet入口、close時反向還原，避免HP回呼繼承drainer封包。完整callback context census仍未宣稱完成。

驗證證據：

- 相同編譯的四組實際handler時間對照，兩個memory旗標與BATTLE／WORLD：父版各案兩次施法、64 frame；候選各案一次、32 frame，第二次MP保持96。每個獨立時間情境使用新建的真正角色，情境內沒有清除或繞過時間窗。
- 40案、352個獨立解密frame、40組canonical速度對應：實際action lock等待超過動畫、實際讀包後延遲、尚未過期但與先前窗口重疊的ingress、拒絕施法不占窗口、到期後新封包、同名角色替換、輸出IOException不重開窗口、Wind Shackle MP拒絕後可立即有效施法，以及戰鬥epoch不重開同角色窗口。
- 原32個Light exercise的斷言與期限完整保留，每個獨立情境使用新建真正角色；before／after各288 frame與plaintext snapshots相同，各8次真正handler鎖競跑，持action/status鎖的socket write均為0。
- 候選內Source時間窗10案52檢查、admission排序6案16檢查，以及4,608次未修改Source速度函式對照與20個動畫整數換算golden case。包含零／負／wrap ingress、精確stale／到期、runtime rebase、exact token取消、ATTACK→SKILL、獨立MOVE債與同時reservation單一勝者；這是核心控制，並未證明所有真正ATTACK／MOVE消費端已接入。
- 相同編譯的真正HP回呼context對照各9案、20 frame：初版會繼承新typed packet入口，修正版在回呼清空、之後以原identity還原；原16項typed控制及期限不變。
- 實際Main normal／interrupt各7 frame，repository只關閉一次、關閉後零呼叫；STOPPED各8案；舊MP socket停住時的原I/O exercise各8案、52 frame；accepted-state50、Proc healing514與外層效果12控制通過。
- 目前SHA的原20組完整回歸、14組scope／HP／MP／Main／context／角色生命週期重播、兩次相同凍結輸入建置及嚴格稽核通過。原斷言／期限不變，damage資源限制維持2／256／64／64。

失敗與修正保留：初版f221雖過20組回歸，專門測試仍證明Wind Shackle在MP拒絕前消耗舊預算；中間版508雖過10組控制，仍會因combat Binding更新而清除角色窗口，實際第二次扣魔至92，故改用canonical角色Owner。兩個唯一JAR與原失敗證據保留。第一次工具回傳執行中後兩個control短暫重疊，結果明確排除，正式驗收依序重跑。舊I/O與四時間fixture在獨立情境之間只重設MP／reuse卻沿用同角色，新的窗口正確拒絕後續情境；原失敗保留，改為每情境新建真正角色，沒有改斷言／期限或在情境內繞過時間窗。

仍需完成：其他技能與cross-skill時間窗消費、真正ATTACK／MOVE入口、完整reuse lease／scheduler失敗與expiry、記憶體材料交易、所有MP／HP／INT與原始減免、獨立STATUS_BRAVE3／同時速度family、完整目前common SprTable／所有sprite／缺失動畫對應，以及其餘全部職業、Ctrl、範圍、NPC、live Item、ability、交易／rollback、canonical SQL、SharedResourceMain、啟動器、UI與關服。Source純時間窗支持負值／wrap不等於所有legacy native status timer已支援。保留競技／隊伍／TA、記憶體角色／道具／技能／XP，僅character_lito／settlement可持久化；先前actor-idle與Main握手逾時根因仍未由此宣稱解決。

清理依精確路徑、連結、SHA與封存逐內容驗證後，移除4099個重複生成檔，保留63個凍結輸入。較早已封存的時間窗缺口控制共用此development目錄，其gap／Source-contract原路徑保留，不破壞歷史audit。收據：`tools/resource-database/build/native-cast-timeline-current.json`。重驗：`python -B -X utf8 tools/resource-database/build/audit-native-cast-timeline.py`。
