# 實際 PC HP／MP 與物品提交的共用 authority

2026-10-17 隔離候選；**原生 HP／MP 佇列及已接入的週期授予路徑共用實際 PC 的穩定鎖。來源完整 PlayerActionLock API、其餘交易／callback／timer 尚未完成，未部署。**

候選 SHA-256：`22a22fc46adfa9fe1c59e733a0a511908eff6ab5f985b6817d345a0ab3341ce0`。父階段為物品輸出順序與 Main 生命週期；canonical 及四個固定基準維持原雜湊。

目前「1-伺服器」的 L1PcInstance 以一個 final ReentrantLock 作為 player action lock，L1Character 的 vital mutation 也使用該 authority；PlayerActionLock 在外層解鎖後處理 vital completion，pair 操作另外處理順序及同 ID 不同物件的情形。本輪凍結六個目前來源／編譯輸入供追溯。

原生父候選則在每個 NativePlayerCommit.Queue 分別建立 vital lock，scene queue 更換後鎖身分也跟著改變；已接入的 item transaction 只保護 admission／Owner／inventory，MP debit 可以在其提交前段穿插。本輪為每個實際 ClientSession 建立一次穩定的非公平 ReentrantLock；實際 queue factory 將同一欄位傳給新佇列。HP、MP 及 source visibility 的原生 HP 讀取仍沿用原 vitalLock 存取點，佇列 owner／closed／phase 機制保留。

已接入的 item transaction 前段依原生既有順序 timedStatus → 該 vital/action lock → 原 admission／Owner／inventory 執行；資料／ledger／背包發布仍為原計算與呼叫。回到輸出前已釋放上述鎖，沿用父階段的發送順序、凍結 DTO、角色撤銷及 Main 收據。未建立 executor 或計時器。舊兩參數 Queue 建構子保留，實際 PC factory 使用新增的 authority 建構子。

這是既有原生 authority 的接合，尚未啟用來源任意 action API。盤點 40 個相關方法後，確認 native accepted sleep/reveal 及 scene 使用 timedStatus → vital，Proc healing 還有 waterLifeStatus → vital；裝備免傷 callback 使用的是 abilityStatus，不能把它誤當成 timedStatus 的反向死鎖證明。直接套用來源 vital-first 任意 callback 與解鎖後 completion 行為，仍須完成完整順序及輸出驗證。

驗證：

- 兩次乾淨建置 SHA 相同。獨立 inverse 還原 2,262 個父 ClientSession 方法、原 NativePlayerCommit 算法、完整舊 Queue ABI 及原 item transaction 前段；僅允許一個欄位／初始化／getter、一個 factory 綁定與一個 item authority delegate。Java InnerClasses 宣告僅按名稱排序後比較，宣告內容仍逐項相同。父 JAR 四個 entry 改變、一個新 helper，其餘 entry 逐位元相同。
- 同一份已編譯 native probe：父候選 8 案／83 項確認 item 與 vital 分離；修正版 8 案／87 項通過。使用真實 PC queue、MP debit、HP 接收及原 memory economy；確認 MP 在提交區段等待、同 PC scene 佇列更換後鎖不變、同 ID 不同實際 session 鎖不同，以及 HP 原計算只提交一次。
- 8 個實際 MP／item TCP frame 經 Java 與 Python 獨立解密核對；實際發送時 timedStatus、vital、admission 鎖均未持有。兩種設定使用 `GameStateStore(path,false,true)`，未建立 state 檔。
- 原 12 支回歸最終通過。第 7 支最初曾在原 3 秒條件 `prototype real actor FIFO and workers idle` 失敗；失敗場次保留。兩次候選與一次父候選的相同 normal actor 探針原樣對照均通過，僅在原斷言前附加失敗診斷觀察，未改條件／期限／算法。根因仍未證實，不宣稱已修復。續跑沿用前六支通過證據，從失敗的第七支重新執行後續項目。
- 父階段 publication 10 案／285 項與 34 frame、真實 Main 物品輸出 normal／interrupt、交易／rollback 8 案／104 項與 12 frame、背包 58 案／187 項及 rental 8 案／31 項、四組來源狀態／Object root／TCP、原中斷 Main 2 案／17 項及原 normal／packet／SQL failure Main 重播通過。行為探針沿用相同編譯內容；原斷言／期限保留，傷害工作上限明列 2／256／64／64。

仍待接入其他交易、原地寫入、多角色轉移與完整 scene／角色原子性；來源 PlayerActionLock／L1Character completion／callback 全部語意；合法巢狀與 pair-ordered deferred output、完整 ability/status/equipment/weight 端點、canonical live Item、活躍 timer／World、60 個來源 Item 方法、唯讀附件、工廠及 2,245 executor 描述，以及其餘全職業／技能／Ctrl／範圍、NPC／同伴與 canonical SQL／登入器／UI／關服要求。父階段的一次 normal Main 握手逾時與本輪 idle 逾時均保留，尚無已證實根因。來源 API 未完成前不啟用相關 worker，不部署。

來源專案、玩家 SQL／WAL 與正式部署未寫入。依精確路徑、連結與 SHA 封存、逐內容驗證後清理 80 個生成檔，保留 27 個凍結輸入及必要失敗／通過證據。收據：`tools/resource-database/build/native-player-action-current.json`；重驗：`python -B -X utf8 tools/resource-database/build/audit-player-action-authority.py`。
