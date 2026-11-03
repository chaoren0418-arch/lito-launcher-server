# 原生 action／vital 與狀態鎖順序

2026-10-17 隔離候選：六個原生方法改採 action／vital → timedStatus；原數值、條件、生命週期與輸出呼叫保留。**完整來源 PlayerActionLock API、completion drain、其他 callback／交易契約仍未完成，未部署。**

候選 SHA-256：`792b3883148ccb20a1ef813ded7574a5ba4a01e774fe1f3f49e63799f143b277`。父版為穩定實際 PC HP／MP 與週期物品 authority；canonical 與四個固定基準未變。

本輪修改 NativePlayerCommit.advance／assign、NativeAcceptedState.sleep／reveal、NativeProcHealingPlayer.apply 與 NativePcActionAuthority.item。advance／assign 最外層先取得實際 PC 穩定鎖，其餘保留原區段；sleep／reveal／Proc healing 將原 vital acquisition 移到狀態 monitor 外，封包尾段仍在解鎖後；item 提交改採穩定鎖 → timedStatus → 原 transaction。尚未新增任意 Source action 執行器、completion 替身、worker 或 timer。

建置前的獨立比對攔下來源鏡像缺少的兩個既有呼叫：viewer retire 與 Proc selectedField。先將這兩個呼叫補進本輪凍結鏡像，再確認六個原方法的編譯位元碼與父 JAR 完全相同。僅拼接六個修改後的方法；四個父 class entry 改變、沒有新 helper，其餘每個方法、宣告、資源及 entry 保留。保留 runnable frames；兩次建置得到相同 SHA。

驗證結果：

- 同一份已編譯探針使用真實 ClientSession、queue、Binding.capture 與 scene，模擬預計來源 API 的 vital-first callback 查詢角色 binding。父版兩種記憶體設定均由 ThreadMXBean 確認 action 與 scene 互鎖；寫出堆疊後只停止該 owned memory fixture JVM，未宣稱其資源正常 drain。候選兩種設定均完成 callback 與 scene，保留 owner／queue 退役及同一實際 PC authority。這是預計 callback 順序的反例，並非現存裝備 callback 死鎖的宣告；裝備免傷仍使用 abilityStatus。
- 原十二支來源／可視／狀態世界／元件／known／corpse-application／原生戰鬥／裝備／sprite／startup／root／同伴回歸通過，原斷言與期限不變。
- 原 authority 探針 8 案／87 項與 8 frame、物品 publication 10 案／285 項與 34 frame、真實 Main normal／interrupt 輸出收據、transaction／rollback 8 案／104 項與 12 frame、背包／rental、四組來源狀態／Object root／TCP、原中斷 Main 2 案／17 項及 normal／packet／SQL failure Main 重播通過。TCP 證據保留獨立解密；實際 I/O 邊界沿用原檢查。
- 全部 fixture 明列 2／256／64／64 傷害工作上限，使用原 memory-only 狀態建構；不寫玩家 SQL／WAL 或正式部署。

這只完成六條原生路徑的順序遷移，未證明所有傳遞 monitor／callback／I/O 順序。serverHonorRespawnDue 仍有 PC monitor → advance 前段；任意 Source callback 的 I/O、完整 L1Character completion queue／最外層解鎖 drain／失敗隔離、巢狀與雙角色 pair 順序仍待實作。其餘物品 writer／原地寫入／貨幣／轉移、canonical live Item／active timer／World、完整能力狀態、60 個 Item 方法及 factories／executor，與全職業技能／Ctrl／範圍／NPC／同伴／canonical SQL／登入器／UI／關服等完整目標仍開放。前階段一次 actor idle 與一次 normal Main 握手逾時的根因仍未證實，未因本輪通過宣稱已修復。

依精確路徑、連結與 SHA 封存並逐內容驗證後，清理 249 個重複建置／觀察生成檔；保留 18 個凍結輸入、目前驗證與既有失敗證據。收據：`tools/resource-database/build/native-action-order-current.json`。重驗：`python -B -X utf8 tools/resource-database/build/audit-action-order.py`。
