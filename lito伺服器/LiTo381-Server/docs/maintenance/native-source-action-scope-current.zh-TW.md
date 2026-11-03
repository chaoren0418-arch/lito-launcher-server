# native 外層 action participant 與 peer readiness gate

2026-10-18 隔離候選：既有 item action 加入外層 scope，原 vital completion 依該 scope 的結束狀態排隊；新增實驗性 native Source run／call／pair helper，原來源鎖排序／條件／finally body 以凍結 bytecode 投影保留。**完整 Source API、其他 writer／角色生命週期與全部職業技能目標仍未完成，未部署。**

候選 SHA-256：`08f4b819b88299cd07a817424830c55a611f0353c443604ca69f19a5b82ef4b9`。父版為 accepted 狀態與玩家 Proc completion；canonical、四個固定基準、目前來源與玩家 SQL／WAL 未變。

父控制中 inner pair 因 outer 仍持有一把鎖而跳過雙方 drain，outer 單角色 finally 只排空自己。候選用 producer-thread 的 root/depth 與 physical-identity participants 記錄整個外層動作；最外層退出先移除 current scope、標記 ready，再依原 unlock 次序與其餘 participants 呼叫既有 drainer。回呼不在 root metadata monitor 或該 scope 自己持有的 action/status lock 內執行。返回值、主例外與同 ID 不同 PC 的鎖分離保留。

僅記 participants 不足以阻止另一 worker 提前取得 peer queue。enqueueLocked 保留原 receipt／publication lambda，外包 root readiness；原 drainer 先取得實際 target lock，收取 ready prefix，遇第一個尚未 ready 的 head 即停止，完成已取得的 batch 後釋放 draining flag。其後在 root fence 內重查：root 已結束則該 drainer 重試，否則由 root exit 觸發既有 drain。這避免忙等、停住 worker、跨過未就緒項目或失去接續。active root 內呼叫其他 PC 的 drain 也只登記，不執行回呼。

只修改 NativePcActionAuthority.item 與 NativePlayerVitalCompletions.enqueueLocked／drain 三個方法、兩個既有 entry；其餘父版方法、宣告與資源完全 inverse 保留，原數值 mutation、receipt lambda、frame／publication／application 包裝未改。新增四個 scope helper entry 與一個 native Source API class；沒有新 worker、timer、PC 或 HP/MP 權威。Source class 以凍結來源的完整投影為底，只將八個編譯後 drain 邊改為 unlocked 登記、加入三個 API wrapper 與 signed-ID 存取橋；原 body 與必要 InnerClasses metadata 另行 inverse。兩次最終凍結輸入建置相同 SHA，與全部測試產物一致。

驗證：

- 同編譯 SourceActionPairProbe 對照父版凍結 Source 投影與候選真實 native API，各十六案。父版十二個 nested 案留下 peer receipt；候選零筆。兩種 memory-only 旗標、BATTLE／WORLD、top-level pair、outer run／call、同 unsigned ID 不同實體與主例外均覆蓋，各三十二個原生 MP frame；原 debit 7／9、snapshot 93／91 保留。
- 同編譯實際 native worker gate 對照各六十八案。父控制四次確定性地在第一把 outer lock 仍持有時發布 peer MP；候選 worker 返回且 queue/receipt 保留，外層結束後排空。另六十四次以第二把實際鎖作 publication barrier，令已持有 draining flag 的 worker 與 root exit 競跑；候選無早發或遺留 receipt，各一百三十六個實際 frame。診斷計數使用 AtomicInteger，原初版通過場次亦保留於封存。
- 同編譯 ready-prefix 對照各八案：同一 PC queue 先有較早已提交的 MP=99，再有 outer root 的 MP=90。候選 worker 先發 99，留下未就緒的 head，outer 結束後才發 90；before 投影會一併發出。正常／主例外及兩旗標／模式覆蓋，各二十四個實際 frame，確保不是整個 queue 一律停住。
- 精確 flag seam 的同編譯對照各八案：inverse-verified fixture hook 將實際 worker 暫停在清除 peer draining flag 前，root exit 已 ready 並嘗試 drain，但 flag 仍由 worker 擁有。正常候選在 worker 清旗標後重查並自行排空；故意忽略 retry-return 的 fixture fault 會留下八筆 ready peer receipt，確認觀察點能抓住該遺失接續。fault 分支最後明確清理，不宣稱是正常流程；數值、callback 與其餘 class body 完整 inverse。正常／主例外、兩旗標／兩模式共各十六 frame，實際輸出亦檢查不持 root metadata monitor。fixture 的 latch 暫停屬觀察工具，production drainer 不等待該 latch。
- 原十二組回歸與八組 authority／item／Main 重播通過；再重播原 accepted-state 五十案、Proc-healing 五百一十四案、原外層 state/Proc 重施／退役控制、HP FIFO／handoff／fault／Main normal/interrupt、MP／恢復／Main、Joy context 與 context-acquisition-failure。原斷言與期限不變，傷害工作上限 2／256／64／64。

精確 flag observer 初輪因插入 hook 後派生的 maxStack 未在 inverse 比對時重算，於執行 native fixture 前被工具拒絕。修正為兩邊重新計算 maxStack 後完整 class inverse 一致，未改 native 產物。保留初輪失敗，沿同一 runtime SHA 重用已通過的 gate 控制與修正後精確 flag 對照，從未完成的 effect 重播續跑；原斷言與期限不變。

本階段的 Source 舊 body 來自 projection，不是執行來源伺服器 L1PcInstance/L1Character runtime；候選的 after 分支使用 JAR 中的實際 native API 與 PC。新 API class 尚未全面綁定遊戲 ingress／技能／道具／timer，source_PlayerActionLock_API_enabled 與 full_Source_action_contract_qualified 仍為 false。不能把 readiness 當成通用 owner/publication guard：frameless MP 的角色選取／scene retirement、應用 admission、raw/untracked 外層鎖，以及真正並行的倒序巢狀鎖／罕見 identity-hash tie 與所有 I/O hierarchy 仍需完整驗證。

frame-null sleep/reveal 及施法資源／reuse／動作封包順序保持開放；243 個 literal PC vital write 的可達性與既有 guard 仍待分類。其餘 item／currency／transfer／rollback、live Item／timer／World／status／ability／executor、全部職業技能／Ctrl／範圍／NPC／canonical SQL／啟動器／UI／關服仍待完成。先前 actor-idle 與 normal Main 握手逾時根因未由本階段解決。保留 competition/team/TA 與記憶體角色/道具/技能/XP，僅 character_lito／settlement 可持久化。

依精確路徑、連結與 SHA 封存逐內容驗證後清理 1477 個重複生成檔，保留 35 個凍結輸入及必要對照。收據：`tools/resource-database/build/native-source-action-scope-current.json`。重驗：`python -B -X utf8 tools/resource-database/build/audit-source-action-scope.py`。
