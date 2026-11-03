# 原生 accepted HP／fatal 尾段的 completion 交接

2026-10-17 隔離候選：原生戰場／世界 accepted HP 與死亡尾段，接入實際 PC 的 Source completion 佇列。**其他直接 HP writer、Proc／sleep／reveal 等 callback 與完整 PlayerActionLock／pair 契約仍開放，未部署。**

候選 SHA-256：`4d45fe23300907c08ab703c12e81c7f47b8952d7aca0da8dd9afd126da920d3e`。父版為實際 PC MP／恢復 completion；canonical、四個固定基準與來源專案不變。

父版 HP mutation 解鎖後直接執行原 native core，會越過另一執行緒尚未完成的同 PC completion，並可能在其 PC socket monitor 後占住 bounded commit worker。本輪在同一個實際 vital mutation 區段排入原 Frame 的 accepted tail，再解鎖由既有 Source drain 執行。接納條件、HP clamp／GM／minimum／fatal 判定、owner 檢查、tail／defense／feedback 呼叫與失敗後不重播 HP 的行為保留；frame 與 worker／commit／publication／應用程式收據沿用父版。

交接不新增執行緒、timer 或等待 latch。每筆 HP completion 以原子狀態區分 producer active、inline completed、lane handed off、continuation claimed。inline 完成則沿用原 drain 迴圈；若由其他既有 drainer 處理，原 worker 返回，但 queue.running／active 保留，下一筆同目標傷害繼續排隊。callback 最後只執行一次原 finish／start／dispatch，沿用既有 bounded dispatcher／retry／admission；失敗仍按原 native committed-tail 處理，不重算 HP。

凍結的原 drain 先編譯比對父 JAR；以確切文字轉換核對 mutation／接納區段及完整 tail 搬移，再獨立 inverse 驗證新方法來自固定編譯結果。僅一個父 class entry／原方法改變，新增三個方法、一個 helper；其餘所有父方法、宣告與 entry 保留。runnable frames 保留，新增方法採明確順序，兩次建置相同 SHA。

驗證：

- 同一份已編譯真實 PC 探針：父版八案確認兩個既有 MP completion 暫停時，兩個 HP core 提早進入並占住兩個原 commit worker；候選八案通過同一 Source FIFO 與 worker handoff。覆蓋 BATTLE／WORLD、兩種 memory-only 旗標；第二筆同目標傷害在前一 tail 完成前保持排隊，其他 fixture 工作可使用原 bounded pool。
- 候選 HP core 確實可在既有 MP drainer 執行，原 frame／worker context 正確，最終數值、committed 計數及 tail 次數一次不漏；注入一個 tail RuntimeException 後，HP 不重播、下一筆事件繼續。各分支的五十個實際 MP／HP／ratio frame 保留，候選由 Python 再獨立解密。PC overlay 僅在兩個原 core 入口觀察；剝除後逐方法／宣告 inverse 完全相同，未改條件、計算或期限。
- 真實 Main 使用既有 typed memory config／repository／port／handle overlay，保留原 Main／SourceLogin 與 TWR。父版確認 worker 等在 MP 後；候選 HP lane 已交接、worker 返回時仍有獨立 HP handler 收據與 active queue。後續傷害保持 FIFO；normal／interrupt 關服均等到原 MP drainer 及 HP continuation 完成，primary error／中斷旗標保留，repository 各關一次、關閉後呼叫零次，最終 HP 3973 與兩個 tail 不重播。
- 原十二組回歸、八組 authority／publication／transaction／admission／root／Main 重播，以及父版 MP／恢復八案與其 Main normal／interrupt 探針通過；原斷言與期限不變。傷害 worker 上限明列 2／256／64／64。

這不代表所有 HP writer 或完整 Source action API 已接入。仍須完成直接 Proc healing、sleep／reveal 與其他 callback／outermost-unlock，完整 run／call／pair／巢狀／同 ID 不同 session 排序及 callback／I/O lock hierarchy。任意 Source callback 的 poison／attacker-ingress／calculation context 隔離尚未驗證；本輪僅核對 frame／worker／publication context。另有其他 item writer／in-place／currency／scene／role／transfer／rollback、live Item／active timer／World／能力狀態／60 個 Item 方法與 executor，以及全部職業技能／Ctrl／範圍／NPC／canonical SQL／登入器／UI／關服目標。先前 actor idle 與 normal Main 握手逾時根因仍未證實，成功重播不作已修復宣告。

玩家 SQL／WAL、來源專案與正式部署未寫入。依精確路徑、連結與 SHA 封存逐內容驗證後清理 56 個重複生成檔，保留 17 個凍結輸入及必要證據。收據：`tools/resource-database/build/native-accepted-hp-completion-current.json`。重驗：`python -B -X utf8 tools/resource-database/build/audit-accepted-hp-completion.py`。
