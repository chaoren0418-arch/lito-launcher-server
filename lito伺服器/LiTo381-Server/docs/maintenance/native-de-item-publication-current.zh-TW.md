# 原生物品交易輸出順序與 Main 生命週期

2026-10-17 隔離候選；**目前只接入週期授予物品路徑，完整 item action／其他寫入者／source timer 尚未完成，未部署。**

候選 SHA-256：`e20eef63628f7a1a8d4024a8eb6ff48deb02c320c295dff8900f7c313c3fbc3a`。父階段為物品 copy／economy／背包發布交易；canonical 與四個固定基準維持原雜湊。

父候選雖已讓同一路徑的物品提交互斥，輸出仍可能被後來的提交超前，且輸出保留的 live inventory 參照會隨來源 setter 改變。同一已編譯探針在父候選重現兩案：第一筆已提交 5 並停在輸出前，來源 setter 接受 10，第二筆提交 12 並先送出，第一筆再送 10；client 最後收到的數量落後於 server／economy 的 12。父對照的 12 個已核對 frame 與 log 保留。

本輪修正：

- 每個實際 ClientSession 建立獨立公平發送鎖，涵蓋該路徑的提交與完整輸出尾段；等待發送時不持有 admission、item Owner、背包或 native vital 鎖。實際網路輸出也在上述狀態鎖釋放後執行。
- 原生新增／檢查完成後、economy 寫入前，擷取前後背包的獨立 native DTO view。數量 delta、原自動名稱同步與既有擴充物品描述均使用同次提交的 view。DTO row map 不可修改，view 不保留 live source actor；這不是完整 Source Item／所有 PC status 的凍結證明。
- 請求擷取角色 admission；等待中斷保留中斷狀態且不提交，角色撤銷後舊請求不能重新接入；尚未輸出的舊角色封包會被抑制。失敗時釋放發送鎖、ThreadLocal 與生命週期收據。
- 使用既有 NativeServerLifetime handler counter／ThreadLocal stack 保留已接入的物品輸出，Main 依原有關閉流程等待它完成。未建立 executor、重試 clock 或額外背景排程。已接入的外層 packet handler 可保留其關閉期間的子操作；關閉後的新外部請求拒絕。
- 目前在同一發送 context 中的巢狀 publication 會於修改資料前明確拒絕，避免部分加密 frame 被穿插及自我等待。合法巢狀與跨 PC 的 deferred／pair-ordered 語意仍待完成。

驗證：

- 兩次乾淨建置 SHA 相同。獨立 inverse 還原 2,261 個父 ClientSession 方法、原 transaction helper／record 及 LiToItemDetails 流程，僅允許明列的鎖初始化、原方法 bridge、snapshot／admission／output view 轉接。父 JAR 四個 entry 改變、兩個新 helper；其餘 entry 逐位元相同。
- 同一已編譯競爭探針：父候選 2 案／101 項重現倒序及 live view alias；修正版 10 案／285 項、34 個實際 frame 通過，包含擴充描述 view、輸出失敗後接續、等待中斷、巢狀拒絕及同名角色重新接入。兩種設定均為記憶體 GameStateStore，未建立 state 檔。
- 觀察 overlay 僅加入原授予尾段與實際 LiToItemDetails view 兩個觀察點；移除觀察呼叫後與各候選原實作完整重建一致。Java 核對原生 payload，Python 獨立解密 34 個 frame。
- 真實 Main 的同一已編譯探針：父候選 11 項重現物品輸出停住時 repository 已先關閉；修正版 normal 12 項／interrupt 14 項通過，各核對 3 個實際 frame。Main 中斷保留原錯誤與中斷狀態，資源延後到輸出離開原收據後安全關閉一次，repository close 後呼叫為零。Main 與 SourceLogin 使用父階段已驗證的記憶體／埠觀察 overlay，原生啟動與 TWR 主體保留。
- 原 12 支回歸、父階段已編譯交易／rollback 8 案／104 項與 12 frame、背包 58 案／187 項及 rental 8 案／31 項、四組來源狀態／Object root／TCP、原中斷 Main 2 案／17 項及原 normal／packet／SQL failure Main 探針均通過。原斷言／期限保留，傷害工作上限明列 2／256／64／64。

建構子計數更正：目前 ClientSession 有兩個建構子，只有一個主建構子。本輪的新發送鎖只在主建構子初始化一次。父階段 verifier 的三個 admission gate initializer 全都位於同一主建構子，舊文件將其寫成三個建構子不精確；本輪 inverse 已核對實際計數，封存父收據與其程式保持原樣。

尚待完成所有其他交易與原地寫入者的 action／輸出接入。父清單涵蓋 262 個直接 RuntimeInventory PC 呼叫方法（含讀取）、161 個角色欄位寫入方法及 185 個發布點，還有其他原生類別的寫入；未接入的方法仍可能自行交錯。完整 native PC action／HP authority、多角色轉移、scene-specific binding、重試／回復與已接受 Source job 排空仍待驗證。另待 canonical live Item 身分、活躍 timer／World 遷移、60 個 Source Item 方法、所有能力／status／equipment／weight 端點、唯讀附件、工廠與 2,245 executor 描述，以及其餘全職業／技能／Ctrl／範圍、NPC／同伴、canonical SQL／登入器／UI／關服要求。父階段保留的一次 normal Main 握手逾時仍無已證實根因。

來源服、玩家 SQL／WAL、正式部署及固定基準未改。依精確路徑／連結／SHA 封存、逐內容驗證後清理 295 個生成檔，保留 21 個凍結輸入與必要證據。收據：`tools/resource-database/build/native-de-item-publication-current.json`；重驗：`python -B -X utf8 tools/resource-database/build/audit-de-item-publication.py`。
