# accepted 狀態取消與玩家 Proc 治療的延後輸出

2026-10-18 隔離候選：accepted incoming sleep／invisibility 取消與原玩家 Proc 治療，在實際 PC action lock 內提交原狀態並加入既有 vital completion queue，於外層 action 解鎖後輸出。**其他 writer、完整 Source action／pair 與全部職業技能目標仍開放，未部署。**

候選 SHA-256：`5289efadaedca600b115570486d6485c096ff1ccdd53fdb78034db8c4a840f8b`。父版為 accepted HP 上下文隔離；canonical、四個固定基準、來源專案與玩家 SQL／WAL 未變。

原 accepted HP tail 本身已經排隊，但在 tail 內再進入實際 item transaction 時，sleep／reveal 與 Proc healing 原方法只解開自己重入的 vital lock，仍可能在外層 action／status／admission／inventory 鎖內寫 TCP。本輪保留租約檢查、狀態取消、Water Life 消耗、亂數／INT／lawful／治療核心數值及 native 封包內容，在原 mutation lock 內加入完成回呼，沿既有外層解鎖 drain 發送。

只變更三個既有類別中的四個方法：NativeAcceptedState.sleep／reveal、NativeProcHealingPlayer.apply 與 NativePlayerCommit.defensePublishing；新增兩個原 publication tail bridge 與四個 helper class entry。實際 ClientSession、原 kernel lambda、其他方法／宣告／資源全部 inverse 保留；原 mutation／數值運算與 mirror getter 差異另以明確白名單檢查。兩次最終凍結輸入建置相同 SHA，並與全部 runtime 測試的候選相同。

初版延後 Proc 輸出時遺失 defense-publication 分類，原故障斷言因而無法命中指定輸出失敗。保留初版與失敗場次後，使用 finally 還原的 ThreadLocal，依原 Frame 的物件身分標記該輸出範圍；未修改共享 Frame.defenseActive，未改原斷言。這是新增的一個狹窄輸出 scope，不能當作其他尚未驗證 ThreadLocal 或任意 Source callback 的完整隔離保證。

驗證：

- 同一份編譯探針在父版／候選各十二案，覆蓋 BATTLE／WORLD、兩種 memory-only 旗標、正常／重施／同角色重新選取退役。探針在實際 incoming receiver 與原 Proc producer 外包實際 NativeDeItemTransaction，PC／Proc hook 均可剝除並完整 inverse。父版確認外層鎖內輸出，候選所有捕捉輸出均不持 action／status／admission／inventory 鎖。
- 原治療亂數保留，檢查實際 HP 已在交易內增加、Water Life 只消耗一次；正常尾端 HP 封包等於捕捉的原數值。重施後新 sleep／invisibility 租約保留、舊狀態封包丟棄。退役後排隊中的 state 與 Proc 封包皆不發布，已提交 HP 保留且不重播。候選四十六個實際 frame 另由 Python 獨立解密。
- 原 accepted-state 五十案與 Proc-healing 五百一十四案通過，包括既有失敗／重施／stale-owner／rebind 與技能／boundary 檢查；無數值或期限放寬。
- 十二組原生回歸與八組 authority／publication／transaction／admission／root／Main 重播通過；再重播 HP FIFO／worker handoff／故障隔離、HP 真實 Main normal／interrupt、MP／恢復與其 Main，以及前階段 Joy 上下文與 context-acquisition-failure 對照。保持傷害工作上限 2／256／64／64。

兩支沿用探針原先共用 probe-current 索引，六組生命週期重播後由獨立索引工具分別指向 authority 與 context 的原始 manifest；不改觀察結果、斷言或已執行的凍結探針。

本階段只涵蓋 accepted incoming cancellation 與既有玩家 Proc healing。frame-null sleep／reveal、其他 vital/status writer、完整 Source run／call／pair／巢狀與雙角色 drain、callback／I/O lock hierarchy 仍待完成；新增 helper 不建立新 worker、timer 或 HP／MP 權威。先前 actor-idle 與 normal Main 握手逾時根因仍未確認。其餘 item／currency／transfer／rollback、live Item／timer／World／status／ability／executor、全部職業技能／Ctrl／範圍／NPC／canonical SQL／啟動器／UI／關服目標繼續開放。

依精確路徑、連結與 SHA 封存逐內容驗證後清理 1544 個重複生成檔，保留 27 個凍結輸入及必要失敗／通過證據。收據：`tools/resource-database/build/native-effect-completion-current.json`。重驗：`python -B -X utf8 tools/resource-database/build/audit-effect-completion.py`。
