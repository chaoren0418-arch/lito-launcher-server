# 三重矢Source倍率外控

2026-10-20 隔離候選：`c2a68fccea6a9a2f80cf288867bcf59a7c12c926173123e610e052499fa8c0d9`，接續奪命之雷傳送外控363ceed9。Triple_Arrow_Dmg已加入CombatRules不可變快照。真正技能132受理與三次attack沿用原流程，僅在BATTLE、WORLD玩家與WORLD怪物三條原生producer的BattleWeaponCritical.apply前加入候選倍率，和原技能／武器倍率取最大值後只乘一次。射擊時仍檢查武器type4／13；一般攻擊、反射與其他攻擊入口維持原方法。

倍率在原三次迴圈之前讀取一次，以primitive double參數沿精確三方法呼叫圖傳遞。沒有PC欄位、ThreadLocal、全域caster或需要finally清理的可變狀態；例外自然退出stack，後續一般攻擊不帶此參數。原三方法保持完整，只有三重矢呼叫新增參數方法。

同一fixture前後各108例：兩persistence旗標、BATTLE／WORLD玩家／WORLD怪物、有無item application、0.5／1／2.5／5倍率、武器雙擊0／100百分比，另每組含實際輸出完整第二個封包後的IOException。36個倍率偏差歸零；每次成功施法恰好扣除3箭，一般攻擊再扣1箭，MP／reuse只受理一次，成功與例外後的一般攻擊均無倍率殘留。前後各2292個實際封包獨立解密，鎖內寫入零、無production PC overlay，原生生命週期idle且沒有狀態檔。

另150個數值案對照凍結Source完整三重矢max片段：武器type1／4／11／13／17、負值／小於1／大於1、既有倍率及武器雙擊，確認只取最大而不連乘，保留原fixed bonus與傷害溢位檢查。14例解析含10個有限值／缺值／空白／非法值與Source完整_getDouble一致；NaN、正負Infinity及溢位成Infinity的設定則在發布前明確拒絕，保留既有數值安全限制，這4例不宣稱與寬鬆Source相同。原current快照在拒絕後保持原物件。

只改ClientSession與CombatRules兩個entry，新增一個無狀態helper。整類重新產生、原PC／CombatRules獨立逆向還原及兩次凍結重建SHA一致。本SHA僅有這項針對性資格；祖先fb2e26b5的完整34組回歸沒有改標。初期fixture的private enum、投射物useType／critical snapshot、INT及輸出分段假設失敗均保留；只校正fixture，未放寬production或斷言。

仍待INVIS_CAST_BREAK、其餘34材料技能、完整技能受理／費用／目標／Ctrl／範圍、Source Buff刷新取消、accepted-cast／buffered attacks、NPC／live Item／能力／warehouse／啟動器及既有正式0.6.69讀取類別語意合併。三重矢全Source語意並未因此宣稱完成。最後變更仍需560＋60數值與完整階段驗證。

競技／隊伍／TA及記憶體角色／道具／技能／XP限制保留，持久化仍僅character_lito／settlement。正式核心、固定輸入、Source、玩家SQL及WAL未寫入；未部署，未清理。[技能73](native-magical-weapon-material-progress.zh-TW.md)；[傳送外控](native-thunder-teleport-policy-progress.zh-TW.md)。
