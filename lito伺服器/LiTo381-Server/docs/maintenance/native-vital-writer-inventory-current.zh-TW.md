# 實際 PC vital 寫入盤點與無 frame 對照

2026-10-18：延續 accepted 狀態／Proc completion 候選，未改 runtime、未部署。這是後續盤點與現象對照，**不是新修正版或完整 writer 驗收**。

實際 JAR 在 ClientSession 類別中有 243 個 HP／MP／上限欄位 PUTFIELD 指令，分布於 170 個方法：HP 60、MP 171、HP 上限 6、MP 上限 6。包含 constructor、保留的 legacy body 與已有 native guard 的分支；尚未建立完整可達圖，不得解讀為 243 個缺漏或 170 個不安全的活動 writer。另有十個以十二指令常數視窗找出的 CombatAccess.set 候選，可能誤判且不完整。255 個狀態／reuse／reveal 呼叫點與九個選定 actual PC 方法的原指令、access flags 與 catch table 已凍結；同方法出現 monitor 並非死鎖證明。

無 production overlay 的實際 JVM 對照：直接經原 NativeAcceptedState 的 frame-null 分支呼叫實際 PC sleep／reveal，外包真正 NativeDeItemTransaction。兩種 memory-only 旗標、BATTLE／WORLD、正常／外層 IOException 共八案九十六項檢查，確認原狀態已取消、HP 與 accepted-frame 計數不變、實際 TCP 輸出仍在 action／status／admission／inventory 外層鎖內。十六個實際封包由原 cipher 與 Python 分別解密，sleep release 再 invisibility reveal 的內容與順序精確保留。這證明該外層組合仍需處理，不代表所有遊戲入口都採用這個組合。

實際 commitSpellReady 先寫 reuse clock，再呼叫 revealInvisibility。後續修正必須維持呼叫端資源提交、施法動作／效果及例外邊界；只把 reveal 移到 queue，可能讓後續施法封包先於 reveal，尚不可視為完成。完整 Source run／call／pair／巢狀、雙角色最終 drain、同 ID 不同實體、其他 writer 與 callback／I/O 鎖順序均繼續開放。

收據：`tools/resource-database/build/native-vital-writer-inventory-current.json`。重驗：`python -B -X utf8 tools/resource-database/build/audit-native-vital-writer-inventory.py`。runtime 與前階段相同，完整目標與 frame-null 修正皆為 false。

依精確路徑、連結與 SHA 封存逐內容驗證後，清理 6 個前一輪盤點生成檔；最新盤點、實際對照與凍結輸入保留。清理收據：`tools/resource-database/build/native-vital-writer-inventory-cleanup-current.json`。
