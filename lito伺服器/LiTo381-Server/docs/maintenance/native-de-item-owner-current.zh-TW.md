# 實際 PC 背包所有權與 Main 啟動接入

2026-10-17 隔離候選；**實際 PC 欄位接入與啟動載入已驗證，完整 Item action transaction／能力／到期生命週期尚未完成，未部署。**

候選 SHA-256：`c7c03c09ec110bd18c7ec8d9d7862f9b5fafc84f9d602fb039c7b46d884848a4`；父收據為顯式 RuntimeInventory 綁定階段。canonical 與四個固定基準維持原雜湊。

實際 Main 在既有應用生命週期註冊後、原有 Hub 清理的 try 保護範圍內，載入完整凍結物品模板及能力定義，再建立 GameHub 自有資源接入。使用既有 owned 唯讀 SQL 載入器，rollback／close 後才發布，不新增 executor、控制檔案或遊戲中 SQL。

實際 ClientSession 的 185 個原生背包寫入點統一接入：一般背包 166、rental town 11、rental 備份 2、warehouse 6。以來源、帳號、角色、實際 PC 物件 ID 及 session 身分辨識所有權；保存對 session／application 的弱引用。暫存複製品、warehouse 及 rental 備份不能經來源 mapped setter 修改活躍角色；換包後舊引用失效，換角／斷線／關服撤銷修改權。斷線標記阻止重新接入，複製 revision 檢查在角色欄位發布前拒絕過期替換。

已修正並保留失敗對照：

- 初版關服撤銷重用了 ClientSession 輸出 monitor。實測堆疊顯示中斷 Main 被尚未完成的 PC commit 輸出阻塞；改成每 session 的獨立接入 gate，三個原建構子各初始化一次。原樣中斷測試通過 2 案／17 項，沒有放寬斷言或期限。此 gate 不是完整來源 PlayerActionLock 的替代品。
- 初版將合法的跨 session rental handoff 視為外來背包。保留來源限定帳號及角色的原消費流程，僅在 RentalBattleHandoff accessor 的新複製品建立待接入狀態；匹配認證身分後才能綁定。不同帳號／角色拒絕，接入前來源物品不可修改；休眠附加狀態不共用。既有跨程序 journal 格式保持原樣，完整額外狀態／計時器跨程序遷移尚未驗證。

- 初版 Main 載入點位於原有 Hub 清理保護範圍之前，物品 SQL 失敗會跳過同步 Hub 關閉；已移到既有保護入口內。保留失敗交易與清理斷言，使用同一份已編譯 Main 探針驗證修正後的錯誤保留、資源關閉及零殘留工作。

驗證結果：

- 兩次乾淨建置 SHA 一致。獨立 inverse 核對 ClientSession 2,258 個原方法及 GameHub／NativePlayerCommit／Main／RentalBattleHandoff 原流程，僅接受明列的接入轉接、七個可見性欄位、兩個欄位 bridge、四個生命週期掛鉤、gate 初始化及一個 rental accessor 轉接。父 JAR 八個 entry 改變、兩個新 helper；其餘 entry 逐位元相同。
- 真實 PC 測試 58 案／187 項與 8 個實際 TCP 封包通過；涵蓋原生物品授予自動接入、來源數量變更、獨立解密、複製／還原／來源競爭、rental／warehouse、同名重新選角、斷線與關服。兩種設定均使用記憶體 GameStateStore。
- 認證後跨 session 原生 rental 啟用 8 案／31 項通過；來源附加狀態、接手 PC、一次消費與負向認證檢查均有實測。
- 既有 12 支回歸沿用原凍結測試與期限，明列傷害佇列上限 2／256／64／64；父綁定 19 案／80 項、來源 Item 9 案／201 項、Object root 3 案／52 項及兩組各兩次 TCP 重播通過。
- 以既有記憶體／埠觀察 overlay 執行真實 Main，驗證 normal、packet 關閉及物品 SQL 讀取失敗：完整物品資源確實接入與關閉，失敗交易 rollback／close 各一次、不發布快照、不寫資料，原啟動錯誤與應用關閉保留。

本階段另保留一次 normal Main 探針在明文握手讀取時 SocketTimeoutException 的失敗紀錄；其他原樣重播通過，但該次逾時尚無已證實根因，不宣稱已修復。

仍待完成完整 PC action transaction、多背包與經濟操作的原子性／重試／回復；每個實際 ID 的 canonical live Item 身分、活躍計時器遷移與 source World 發布；60 個來源 Item 方法及所有能力／狀態／封包／PC refresh 端點；來源附件唯讀匯入、工廠、2,245 執行器描述及完整物品／技能使用。現有 revision fencing 不代表每個先前的經濟副作用都已回復，休眠 actor 複製證據也不代表完整 live timer／spatial transfer。其餘全職業／技能／Ctrl／範圍、NPC／同伴、canonical SQL／應用／登入器／UI／關服要求繼承父收據，完整目標尚未達成。

來源服、玩家 SQL／WAL、正式部署及固定基準未改。依精確路徑與 SHA 封存並逐內容驗證後清理 3242 個生成檔，保留 23 個凍結輸入及必要證據。

收據：`tools/resource-database/build/native-de-item-owner-current.json`；重驗：`python -B -X utf8 tools/resource-database/build/audit-de-item-owner.py`。
