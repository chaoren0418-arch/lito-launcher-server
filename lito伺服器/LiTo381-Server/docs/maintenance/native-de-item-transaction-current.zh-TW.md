# 原生物品 copy／economy／背包發布交易

2026-10-17 隔離候選；**已驗證週期授予物品的一條原生交易路徑，其餘交易、完整 PC action／物品身分／計時器仍開放，未部署。**

候選 SHA-256：`64099931ddde50a9e4a8d5112a3e4d60a749b0892f7eb31cbe7ffeecae76e2d3`。父階段為實際 PC 背包所有權與 Main 接入；canonical、正式部署與四個固定基準雜湊未改。

實際 `grantNativeMagicDollPeriodicItem` 原先順序為複製背包、增加物品、寫入記憶體 economy，再發布新背包。來源 Item 在複製後變更數量時，既有 revision 檢查會拒絕發布，但 economy 已變更。相同已編譯探針在父候選的兩種記憶體設定均重現：來源數量已接受為 10、economy 寫成 5，原生授予因過期 copy 被拒絕。

修正抽出原方法的提交前段，由可重用的 item-only transaction helper 依既有 admission gate → item Owner → 當前 RuntimeInventory monitor 的順序涵蓋 copy／add／economy／角色欄位發布。原有輸入驗證、數值計算、持久化呼叫與錯誤傳播保留；封包尾段在所有交易鎖釋放後執行，使用該次已提交的新背包，不再於輸出時重讀角色背包欄位。未建立 executor 或計時器。來源 PlayerActionLock／原生 vital authority 的完整接入仍待完成。

驗證：

- 同一份已編譯探針跑父候選與修正版：父候選 2 案／14 項重現不一致；修正版 8 案／104 項通過。涵蓋來源寫入先成功、原生交易先成功、實際 memory save 失敗及原輸入驗證失敗後釋鎖。競爭時使用 JVM ThreadInfo 確認來源寫入者阻塞於確切舊背包 monitor。
- 原生封包輸出時檢查 admission gate、Owner、前後背包與 vital 鎖均未持有；12 個實際 TCP frame 經 Java 與 Python 獨立解密核對，數量、名稱同步及原授予訊息符合提交結果。
- 觀察 overlay 僅在 GameStateStore 的一個 `saveEconomyState` 入口插入測試 rendezvous，並在該方法已修改 profile、呼叫原 save 前插入故障點；移除觀察呼叫後與原實作逐位元重建一致。使用真實 ClientSession 與原 GameStateStore 計算及儲存流程。
- 兩次乾淨建置 SHA 相同。獨立 inverse 將抽出的提交前段與輸出尾段重接，2,260 個原有 ClientSession 方法與父候選完整一致；原 NativeDeItemApplication 方法亦一致。父 JAR 僅兩個 entry 改變、三個新 helper，其餘 entry 逐位元相同。
- 既有 12 支回歸全部通過，原斷言／期限保留，傷害工作上限明列 2／256／64／64。父階段已編譯背包 58 案／187 項及 rental 8 案／31 項、四組來源狀態／Object root／TCP 重播、原中斷 Main 2 案／17 項，以及相同已編譯 Main normal／packet／SQL failure 探針均通過。
- 兩種設定均使用 `GameStateStore(path,false,true)`，測試 state 檔未建立；來源模板與能力定義經既有 owned 唯讀 SQL 載入。來源專案、玩家 SQL、結算 WAL 與正式部署未寫入。

已從父候選 JAR 盤點 262 個直接呼叫 RuntimeInventory 的 PC 方法（包含讀取）；完整發布點清單涵蓋 161 個方法、185 個角色欄位發布點；60 個方法包含發布前的 GameStateStore mutation 呼叫，150 個方法含網路呼叫。這些是指令順序的觀察，尚未逐方法重現或修復。此輪只接入 `grantNativeMagicDollPeriodicItem(II)V`；交易清單可作後續擴大覆蓋與檢查依據。

仍待覆蓋未替換角色欄位的原地背包寫入、其餘原生類別的背包寫入，並完成全部交易與多角色物品轉移的原子性／重試／回復、真正 PC action／vital 統一與已接受工作排空、跨交易封包順序及角色切換；每個實際 ID 的 canonical live Item、活躍 timer／World 發布、60 個來源 Item 方法與全部能力／狀態／PC callbacks、唯讀附件、工廠與 2,245 個 executor 描述。來源 setter 改變活背包尚不代表所有 economy／輸出端點已整體接入。父階段保留的一次 normal Main 明文握手逾時仍無已證實根因。其餘全職業／技能／Ctrl／範圍、NPC／同伴與 canonical SQL／登入器／UI／關服證據繼承父收據。

依精確路徑、連結及 SHA 檢查，封存後逐內容驗證再清理 154 個生成檔，保留 20 個凍結輸入及必要測試證據。收據：`tools/resource-database/build/native-de-item-transaction-current.json`；重驗：`python -B -X utf8 tools/resource-database/build/audit-de-item-transaction.py`。
