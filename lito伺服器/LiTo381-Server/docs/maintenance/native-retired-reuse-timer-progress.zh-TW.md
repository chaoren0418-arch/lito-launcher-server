# Source延遲計時器退休與Main驗證

2026-10-20 隔離候選：`0269195c2bd3626a5a2ec9366dd655362045c4fc16ba138a40a209572716f0c2`。接續四個Source外控候選，修正已STOPPED且retry scheduler已關閉後，原Source lease重排程被拒絕並寫入錯誤的競態。只改NativeReuseActor.pcSchedule這個原生port；完整NativeSourceSkillDelay／Lease及角色token演算法逐位元組不變，目前Source L1SkillDelay與凍結版本SHA仍為cef6fabf。retired port回傳已取消、沒有actor／task欄位的Future，不再提交任務，也不打開舊token。

兩persistence旗標、WORLD／BATTLE、有無item application及四種排程狀態的同一fixture前後各32例：8次退休重排程拒絕歸零；8個STOPPED但receipt尚未釋放的已受理排程仍真正執行一次。8個刻意製造的OPEN scheduler故障仍增加原Source rejected／error計數並保留closed token，沒有把執行中故障吞掉。pending receipt用盡後executor終止、queue清空，新施法不能重入STOPPED。只改1個原entry、新增2個無欄位helper class，原PC與生命週期方法均不動；整類重新產生及獨立actor逆向還原、兩次凍結重建SHA一致。

最新本SHA重新執行真正Main的技能73八組：regular／GM／policy讀取失敗／角色退場，各normal與interrupt。初始policy讀取被阻擋時Main保持資源；放行後按原受理提交或拒絕，repository只關閉一次、close後呼叫零，handler／receipt清零，executor與生命週期終止。角色退場輸出零、policy失敗只有299；沒有PC overlay，Main／SourceLogin僅使用既有7個經整類inverse核對的memory／port／observer測試接點。

本SHA另實跑完整448案隱身外控回放，差異零、9個Source parser對照通過，開啟政策的解密goldens與7fbc49e7逐位元組相同；完整log的retired timer rejection與rebase unavailable均零，鎖內輸出零，無狀態檔。四個已確認外控的前後矩陣分別為SOLID_CARRIAGE_MODE 160／160、SLAY_BREAK_NOT_TELEPORT 256／256、Triple_Arrow_Dmg 108／108、INVIS_CAST_BREAK 448／448；前面三項是各自父候選的針對性證據，並未改標成最新SHA的新測試。

技能73已完成Main／生命週期及fb2e26b5上的34組完整回歸；材料進度仍為24個，其餘34個未完成。四個外控的完整技能受理／費用／目標、斗篷匯入及雙隱身lease、Source Buff刷新取消、accepted-cast／buffered attacks、post-install補償／quarantine、NPC／live Item／能力／warehouse／canonical SQL／啟動器，以及既有正式0.6.69讀取類別的語意合併仍開放。祖先完整回歸未改標；最後變更仍需560＋60數值、完整階段驗證與精確封存清理。

競技／隊伍／TA與記憶體角色／道具／技能／XP限制保留，只允許character_lito／settlement持久化。正式核心、固定輸入、Source、玩家SQL與WAL未寫入；未部署，未清理。[技能73](native-magical-weapon-material-progress.zh-TW.md)；[隱身外控](native-invisibility-cast-policy-progress.zh-TW.md)；[三重矢](native-triple-arrow-policy-progress.zh-TW.md)；[傳送](native-thunder-teleport-policy-progress.zh-TW.md)；[堅固防護](native-solid-carriage-policy-progress.zh-TW.md)。
