# accepted HP 的 drainer 上下文隔離

2026-10-18 隔離候選：原生 accepted HP／fatal tail 在其他 completion drainer 執行時，隔離十六個已盤點的戰鬥上下文，finally 以原物件身分還原。**完整 Source action／pair、其他 vital writer 與其他 scope 仍未完成，未部署。**

候選 SHA-256：`abc447dc42a91686c374a13cdb90cf31b46c3cbf2e5c4c3470646c354639ee7d`。父版為 accepted HP completion 交接；canonical、四個固定基準、來源專案與玩家 SQL／WAL 未變。

父版的 HP tail 已能交給既有 drainer，但會繼承其 producer 控制。實際反例以 NativePlayerLifetime.withMinimumCommitDelay 的三十秒 scope 包住原 MP producer，並使用原 poison install／clock／attacker ingress／calculation scope；另一個執行緒接納的原 HP event 本身沒有這個延遲。HP tail 在該 drainer 執行時，原 Joy 回饋錯誤捕捉了 drainer 的 deadline，caster HP 留在 4000，回饋留在原 retry clock。候選在相同原生流程中按原數值立即完成回饋，再將 drainer 的原 scope 逐一還原，未改傷害算式或 Joy 數值。

執行中 JAR 共盤點四十八個 ThreadLocal、二百五十八個直接存取點。策略為：十六個 combat producer／output carrier isolate-and-restore；五個 frame／worker／commit／publication／packet stack 由既有 wrapper 管理；四個 ingress／application owner 保留；二十三個其他 scope 維持原狀，尚不構成任意 Source callback 的完整隔離保證。原始欄位／直接存取清單與政策表均凍結，未把盤點當成完整 call graph 證明。

新增 helper 只引用既有 ThreadLocal，不建立另一份 HP／MP、actor、executor 或 timer。僅修改 NativeAcceptedHpCompletion.run 的執行邊界，保留原 executed／handoff／native failure sink／resume 邏輯。context 取得在原失敗保護內，無論取得或 callback 失敗，原 lane 仍能接續；恢復在原 handoff finally 後完成。獨立 inverse 確認一個原 method、另一個 helper entry 以外的全部父方法、宣告與 entry 保留；兩次建置輸入與 SHA 相同。

本輪初版曾把 context 取得放在原失敗保護外。故障注入用同一份編譯探針、實際 native HP lane 及一個可剝除且完整 inverse 的取得入口 hook，確認初版第一筆 HP 已到 3989、下一筆仍排隊，原 failure sink 未被呼叫。修正後第一筆不重播、下一筆到 3976，原失敗計數增加一次、下一 tail 完成一次。初版與正常驗證紀錄保留，未部署；before 分支最後僅 drop／terminate owned memory fixture，不宣稱該 lane 正常 drain。

驗證：

- 同編譯 actual PC 對照覆蓋 BATTLE／WORLD、兩種 memory-only 旗標。父版八案確認 drainer 控制外洩；候選九案通過實際 Joy 回饋、原 typed scope 物件身分還原、明確存入 event 的非致命 HP 下限仍為 1，以及 carrier-only opaque marker 的巢狀／例外還原。opaque marker 測試只驗證容器，未讓原生 core 消費測試標記。二十個實際 frame 再由 Python 獨立解密。
- 第一輪 Joy 對照在 retry 註冊尚未完成時就查詢 snapshot 而失敗；保留該場次，新增等待原 retry clock 註冊的觀察點後，保留原 HP／deadline 條件再通過。
- 修正後重跑原十二組回歸與八組 authority／publication／transaction／admission／root／Main 重播，原斷言與期限不變；再重播父階段 HP FIFO／worker handoff／失敗隔離、HP 真實 Main normal／interrupt，以及 MP／恢復與其真實 Main。傷害工作上限明列 2／256／64／64。

修正版資格驗證曾因 execution host／tool session 遺失而中斷。確認原 handle 不存在，並以作業系統查無該輪 Python／Java 程序作為終止證據；保留前四組同 SHA 的通過輸入／log，從第一個未確認完成的第五組續跑。未把中斷當成原生斷言失敗，也未因觀察逾時重啟仍存活的程序；中斷 manifest 與程序查核收據保留。

仍待完成直接 Proc healing、sleep／reveal 與其他 writer／outermost-unlock，完整 Source run／call／pair／巢狀／同 ID 不同 session 排序、雙角色 completion 與 callback／I/O lock hierarchy，以及其餘 item／currency／scene／role／transfer／rollback、live Item／active timer／World／能力狀態／剩餘 Item 方法與 executor、全部職業技能／Ctrl／範圍／NPC／canonical SQL／登入器／UI／關服目標。先前 actor idle 與 normal Main 握手逾時仍未證實根因。本輪十六個 carrier 的修正不能代表其他二十三個 scope 或完整 action API 已驗證。

依精確路徑、連結與 SHA 封存逐內容驗證後清理 3085 個重複生成檔，保留 22 個凍結輸入及必要失敗／通過證據。收據：`tools/resource-database/build/native-completion-context-current.json`。重驗：`python -B -X utf8 tools/resource-database/build/audit-completion-context.py`。
