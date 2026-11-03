# 實際 PC 的 MP／恢復 completion 佇列

2026-10-17 隔離候選：原生 MP 扣除、HP／MP 裝備恢復接入目前來源 L1Character 的 completion drain 語意。**原生 HP／fatal／Proc 等其他尾段、完整 PlayerActionLock／pair 契約仍開放，未部署。**

候選 SHA-256：`c0cfcd86c41c01a877f83bc7a4c961e0295a2be2050518949d527638afc64338`。父版為六條 action／vital → timedStatus 路徑；canonical、四個固定基準及來源專案未變。

父版只釋放內層 vital lock 後便發送 MP／恢復封包；當 NativeDeItemTransaction 的外層 action、timedStatus、admission／inventory 區段仍持有時，輸出可能在這些鎖內執行。本輪將 completion 在實際 HP／MP mutation 的鎖內排入每個 ClientSession 一次建立的 ConcurrentLinkedQueue／AtomicBoolean 狀態。沒有另一份 HP／MP 數值或 surrogate PC。批次取出先跨過同一個實際 PC vital lock 屏障，再解鎖執行；單一 callback 拋 Throwable 記錄後仍處理其餘項目，持續排入則以迴圈重試。

drain 本體來自目前「1-伺服器」L1Character，僅替換實際 PC lock／state／log adapter；凍結來源與編譯輸入，原文與 adapter 後函式逐字核對。五個原方法先證明來源鏡像編譯結果與父 JAR 相同，再只替換 debitMp／recover、item 最外層 unlock drain、lifetime idle／terminated。新增 frame 與 item-publication context 保存／還原方法，保留原 MP／恢復算式與封包內容。獨立 inverse 核對其餘方法、宣告與 entry；五個父 entry 改變、三個 helper entry 新增，沒有新 worker／timer。兩次建置相同 SHA。

每個 reservation 在 mutation 前取得既有 NativeServerLifetime handler 收據；callback 保留原生 frame、worker／commit 與 item-publication context，finally 還原並釋放。關服時拒絕新的未接納外部 MP／恢復操作，已接納 packet／worker 可繼續；completion 計數也參與 lifetime idle／terminated，避免其他執行緒 drain 時資源提早關閉。

首次第二次建置曾因新增方法使用 Set.of 的無序迭代，僅 NativeDeItemPublication 的 completion／lambda 方法附加排列不同而得到不同雜湊。保留兩個 build 的差異紀錄，改為明確 List 順序後，兩次乾淨建置輸入相同、SHA 相同，且逐位元重現已通過全部行為測試的原候選；未以不同 JAR 沿用測試。

驗證：同一份已編譯 actual PC 探針，父版四案重現外層鎖內輸出；候選八案通過延後到外層釋放、回傳值與 primary exception 保留、FIFO、另一執行緒的實際 lock barrier、callback 失敗隔離、重入排入下一批，以及 no-op reservation 釋放。兩種狀態旗標均為 memory-only；八個實際 frame 經 Java／Python 獨立解密。首次探針誤用 manaPoint(0,0) 非法測試最大值，修正為有效最大值後重跑通過，保留其失敗輸入／log，不視為原生行為修正。

真實 Main 探針保留原 Main／SourceLogin 與 TWR；僅使用既有 typed memory config／repository／port／handle overlay。父版在實際 MP 輸出暫停時先關閉資源。候選讓第一個 callback 暫停、第二個 producer 提交後返回，確認兩個獨立 handler 收據仍阻止資源 close；normal／interrupt 均通過，primary failure 與中斷旗標保留、callback context 還原、關服後新外部 MP 在 mutation 前被拒絕。釋放後原數值 99→98 的四個 TCP frame 獨立解密，repository 各只關一次、關閉後呼叫零次。

原十二組回歸及八組 authority／publication／Main／transaction／admission／root 重播全部通過，原斷言與期限不變，傷害工作上限明列 2／256／64／64。來源 SQL 只讀；玩家 SQL／WAL、正式部署未寫入。先前 actor idle 與 normal Main 握手逾時根因仍未證實，本輪通過不代表已修復。

下一步仍須將 accepted HP／fatal、Proc healing、sleep／reveal 及其他 callback 接入完整 completion 語意，再處理完整來源 PlayerActionLock run／call／pair、巢狀、同 ID 不同 session 順序，以及其餘交易／live Item／timer／全職業技能／Ctrl／範圍／NPC／canonical SQL／登入器與 UI 等完整目標。不得以這兩條 consumer 的驗證宣稱全部 action 或 HP completion 完成。

清理 269 個經精確路徑、連結與 SHA 核對並封存逐內容驗證的重複生成檔；保留 20 個凍結輸入及現行／既有失敗證據。收據：`tools/resource-database/build/native-vital-completions-current.json`。重驗：`python -B -X utf8 tools/resource-database/build/audit-vital-completions.py`。
