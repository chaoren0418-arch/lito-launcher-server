# 隱身施法Source外控

2026-10-20 隔離候選：`7fbc49e745581aa3be40caae54d561010a43061a6d9f1362593b56d9fd9c5ed7`，接續三重矢c2a68fcc。INVIS_CAST_BREAK納入原CombatRules不可變Source設定快照，缺值／空白預設false，其他內容依Source Boolean.parseBoolean(trim)解析。共同revealInvisibility僅對spell-*與classic-world-spell-*施法原因套用政策，既有攻擊、受擊、偵測、死亡、重置與到期保留原規則。掃描真正候選的全部class，共131個解除／反射呼叫位置；只有兩種施法前綴，physical attack及非施法原因分開保留。

材料技能60／97沿用原已受理body的成本、材料、MP與reuse；政策關閉時，同技能重施刷新隱身，不先解除也不送出原先的顯形重建；政策開啟時保留原同技能toggle。canonical已裝備20077／120077／20062設有Source重施例外，但目前native物品目錄尚無這三件斗篷，完整裝備隱身匯入／持有／卸裝及該例外的實際整合測試仍開放，沒有建立假物品宣稱通過。Source可同時保有60／97兩個lease，native目前仍是單一lease；跨技能雙lease共存與完整可施法清單也未宣稱對齊。

同一fixture前後各448例：兩persistence旗標、WORLD／BATTLE、有無item application、技能2／31／60／97、政策開關，搭配既有60／97隱身、無隱身、MP不足、材料不足、真正到期與睡眠。64個開關／重施差異歸零，9例實際snapshot與完整Source布林parser一致。開啟政策的原施法解密封包逐位元組相同，材料只扣一次，拒絕保留原隱身與reuse；受擊解除另以原方法檢查不受外控影響。記錄的施法封包均獨立解密，鎖內寫入零、production PC overlay零，測試狀態檔不存在。

只改PC的共同reveal與nativeMaterialInvisibilityBody，加上CombatRules.snapshot及無狀態helper。整類重新產生與PC／CombatRules獨立逆向還原通過，其餘entry不變，沒有PC欄位、worker、scheduler或ThreadLocal；兩次凍結重建SHA一致。本SHA未重跑祖先fb2e26b5的34組完整回歸。擴大測試另看見既有Source延遲timer在關閉後重排程被拒絕的紀錄，未把它隱藏或當作生命週期全通過，接續獨立修正原生scheduler port並重驗Main。

仍有34材料技能及完整職業／目標／Ctrl／範圍、Source Buff刷新取消、accepted-cast／buffered attacks、NPC／live Item／能力／warehouse／啟動器、既有正式0.6.69讀取類別語意合併。四個確認的外控消費缺口均已各有實作，但各完整技能與裝備語意仍開放；最終變更後需560＋60數值與完整階段驗證。

競技／隊伍／TA與記憶體角色／道具／技能／XP限制保留，只允許character_lito／settlement持久化。正式核心、固定輸入、Source、玩家SQL及WAL未寫入；未部署，未清理。[技能73](native-magical-weapon-material-progress.zh-TW.md)；[三重矢](native-triple-arrow-policy-progress.zh-TW.md)。
