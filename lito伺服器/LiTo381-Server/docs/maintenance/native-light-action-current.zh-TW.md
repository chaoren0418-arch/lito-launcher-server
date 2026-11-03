# native 光術實際入口與 Source action 邊界

2026-10-18 隔離候選：實際 `C_USE_SPELL` handler／dispatcher 的光術（skill 2）開始使用既有 native Source PlayerActionLock。原數值、條件與輸出 body 從父版 bytecode 投影保留，提交段與原隱身發布／原施法尾段分開，使用既有 completion receipt／FIFO／root readiness。**這是實際消費端的進展；完整 Source API、Source 施法 admission／時間線與全部職業技能仍未完成，未部署。**

候選 SHA-256：`389663ea3259789c12206b9c7774b6dff0000dd90c27558e1c19268e8a9cb69a`。父版為角色發布／選角與 owner 關閉次序候選。canonical、四個固定發布／基準、來源專案與玩家 SQL／WAL 均未由本階段改寫。

原光術可在另一執行緒持有實際 action lock 時扣魔與送包；直接外包 action lock 後，舊 PC monitor 又把共用 cooldown 讀取、光術提交與隱身操作綁到 synchronized 的 socket sender。原 MP write 停住時，新光術甚至尚未保留 receipt 就卡在 useSpellBeforeDragonTargets 的 PC monitor。

候選於實際 useSpell 入口為光術擷取原 Binding、應用 generation 與 lifetime receipt；原 dispatcher body 保留為 alias，其餘技能仍走原分派。進入 Source action 後重查 Binding、斷線與應用 generation，排隊期間同名選角／場景變更／應用退役會在數值提交前拒絕。producer ingress 在 Source drain 前暫時移除，之後恢復並在 dispatcher finally 清除。

共用 cooldown Map 的 read／put、四個 canonical 隱身操作與 honor respawn predicate 改用實際 action lock，純狀態 helper 不加入 drain。原光術與復活提交段的 PC monitor stack 操作改為無 monitor 的等價 stack adapter，由外層實際 action lock 保護；不再等候 socket sender 的 PC monitor。原隱身 token、布林更新與原 cooldown 數值不重寫。原隱身發布與整段原光術尾端按原順序排入同一既有 receipt，最外層 Source action 完成後才執行。沒有新增 HP／MP／PC 權威、worker 或 timer。

修改範圍為兩個既有 entry、十二個原方法：

- PC 的 useSpell、useSpellBeforeDragonTargets、commitSpellReady：入口 receipt／Binding 與共用 reuse Map 鎖。
- PC 的 useLightSpell、publishInvisibilityReveal：原提交／原發布／原尾段投影與延後順序。
- PC 的 activate／cancel／clear／expireInvisibilityState 四個方法：canonical 狀態使用實際 action lock。
- PC 的 honorRespawnPending、serverHonorRespawnDue：predicate 與原復活提交段鎖序。
- NativeDeItemApplication.publicationOwner：已受理工作停止後的 canonical metadata capture。

新增 NativeLightAction 外層、Batch、Output 與 Ingress 四個 helper entry。PC 只增加必要 alias 方法，沒有新欄位；compile-only PC 只提供編譯簽名，絕不放入 JAR。selected 方法的 StackMapFrames 以實際父版 hierarchy 單獨重算，其餘原方法／欄位／宣告／entry 完整 inverse 核對。兩次同一凍結輸入建置得相同 SHA。

驗證：

- 同編譯 before／after 各 32 案，兩個 memory-only 旗標、BATTLE／WORLD、隱身／非隱身，涵蓋 private 原光術、實際 useSpell 與實際 handle。各 288 個自己／旁觀者原生 frame 獨立解密，內容與順序完全相同。共用靜態資料的原扣魔 4 保留（100→96）；自己順序為可選的隱身解除、motion、effect、sound、light、MP。持實際 action/status lock 的 socket write call 由 648 降為 0（此為 write call 數，不是 frame 數）；各八次實際 handler 鎖競跑，候選等候 physical lock 才改數值。
- 68 案／304 frame：排隊前保留 receipt 後同名選角、場景、斷線、應用關閉；提交後退役、主例外 identity、原 malformed selector、原 MP／reuse／dead／observer 拒絕條件與故意 output IOException。原提交不回滾／重播，輸出失敗不繼續後續 batch action，下一個原光術仍能執行；receipt、queue 與兩個新 ThreadLocal 均釋放。延後輸出的 IOException 依既有 completion failure isolation 記錄；不宣稱可同步回拋給已返回的 packet caller。
- 初版 77b205… 在有 item application、尚無 owner 的第一筆光術已保留 receipt 卻碰到 native STOPPED 時，MP 已扣至 96 而沒有 frame。修正版只允許此既有受理工作的 canonical Owner(app,pc) metadata，沒有重新綁定 inventory，也不開放 item admission。同編譯各八案：初版 20 frame、修正版 40 frame；新光術與真正的 item grant 仍被拒絕，transactionOwner 仍為 null、Owner.requireActive 仍拒絕。
- 初版 852016… 的原 MP socket write 停住時，後續光術被 PC monitor 卡住。修正版同編譯各八案，原 MP 仍停在 write 時光術已提交 99→95 並返回，兩筆 receipt 保留；釋放後各 52 frame 的原 MP／光術 FIFO 與 snapshots 保留。before 有一筆既有 receipt，after 有兩筆，因原 dispatcher 在新 reservation 前已被舊 monitor 擋住。
- 實際 Main normal／interrupt 各七個 frame：舊 MP receipt 與光術獨立 receipt 一起保留，Main 等待原工作；關閉後 direct dispatcher 不改數值，關閉後 handler 依原 gate 等待 cleanup，再返回且不改數值。資源只關閉一次，repository 關閉後零呼叫。第一次 fixture 同步呼叫關閉後 handler，同時扣著原 write，因而在原 awaitCleanup 合約下逾時；保留 180 秒 timeout 與 thread dump，改用受控獨立 request thread 觀察原 gate，未改 native 程式或期限。
- 真實復活鎖序對照各兩個 memory 旗標。父版實際 Source action／PC monitor cycle 由 ThreadMXBean 確認，owned counterexample JVM 明確 exit 釋放故意製造的 cycle；候選的死者光術不扣 MP，原復活完成 HP=5000、token=0，各兩個原 frame，兩執行緒完成。四個 native run 均通過；初次 driver 僅在列印結果時 KeyError(name)，以原 SHA／原四份 observations／deadlock dump／解密證據做明確 finalizer，沒有重跑或更改 native 斷言。
- 原 20 組完整回歸、14 組 scope／HP／MP／Main／context／角色重播，以及 accepted-state 50、Proc healing 514 與外層效果 12 控制通過。原斷言／期限不變，damage worker 參數維持 2／256／64／64。

來源 PlayerActionLock 來自既有凍結 Source class 的完整投影；這裡沒有執行來源伺服器 L1PcInstance runtime。另凍結目前 L1SkillUse、L1SkillId、SkillCastAdmission 與 L1SkillDelay 供後續完整 cast admission／material／action-time／reuse 對齊；它們不是本階段已完整移植的宣告。父階段已記錄的 L1PcInstance.java migration-only 差異、新 source／舊 compiled PC 與 MigrationDeparture 功能維持未完成，歷史父收據不改寫。

仍須完成 Source 施法時間線／material／reuse gate 的全量對齊、其餘真正技能與物品／timer／status 消費端、任意 nested／raw lock 邊界、每位 viewer admission 與完整 callback context。父版 census 的 243 literal vital write／170 methods 包括初始化、保留 legacy 與已守護分支，不是缺漏 writer 數。其餘全部職業技能／Ctrl／範圍／NPC／live Item／ability／executor／transaction／rollback／canonical SQL／啟動器／UI／關服目標仍持續；先前 actor-idle 與 Main 握手逾時根因不由本階段宣稱解決。保留 competition／team／TA、記憶體角色／道具／技能／XP，僅 character_lito／settlement 可持久化。

逐路徑／連結／SHA 核對，封存逐內容驗證後清理 1961 個重複生成檔，保留 48 個凍結輸入與兩個唯一 counterexample JAR。收據：`tools/resource-database/build/native-light-action-current.json`。重驗：`python -B -X utf8 tools/resource-database/build/audit-native-light-action.py`。
