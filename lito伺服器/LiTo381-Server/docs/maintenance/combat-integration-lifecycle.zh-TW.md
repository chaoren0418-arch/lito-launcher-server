# 戰鬥整合：成對鎖定、Main 關閉與 TAMED

現行證據入口：`tools/combat-continuation/integration-lifecycle/current.json`。完整目標仍未完成；未部署、未啟動遊戲，沒有人工 UI 驗收宣告。

整合沿用 `743b35d17ea4…` 靜態 NPC 候選與 `175022207c3c…` 的成對鎖定／關閉修正。保留既有 NPC、範圍、召喚、輸出 completion 與 Source 外控入口；沒有複製另一套技能派送器。

成對鎖定候選 `a63b57311659…` 通過 288 案，涵蓋一般／Ctrl、自身／跨角色、角色退役、輸出失敗與互相巢狀鎖定；技能 68 Main 10 案及技能 73 Main 8 案通過。退休排程候選另通過 32 案。這些是各自 SHA 的證據，不能累算為最新候選的完整回歸。

父層 WORLD TAMED 候選 `4dd7876008d6d6d8de54c958ec247934e50c59d242dba79a7b83a72c38fe2b92` 在同一 SHA 通過 24 案、520 項檢查：

- WORLD 解放建立新 ID，移交同一份背包，HP／MP 直接保留，模板上限不膨脹，經驗值為零。
- 捕捉時以既有掉落資源初始化一次；已初始化的 NPC 不再重抽。完整暫存放不下時保留原怪物及全部物品。
- 原生成點在捕捉後獨立重生，解放不重複排程。重生回呼以弱參照、場景／物件身分與 NPC 世代檢查，舊計時器不會復活後一輪捕捉的同一實體。
- 模板建立失敗恢復原怪物可見狀態、保留已初始化物品並取消預約。物件登記失敗與 OPEN 排程拒絕有回復檢查。
- 死亡只釋出既有背包；重複呼叫不複製掉落。輸出不持有玩家動作鎖，雙方 receipt 歸零。

NPC 父層候選 `6dae16ef0087f9d52419b7e29fc1ac690ae437545b8454266253ff8e0dce33c7` 保留上述改動，同 SHA 通過 NPC 238 案／1,539 項檢查：四條實際 HP 路徑 176 案、普攻三條回呼生命週期 36 案、物理範圍回呼 26 案。涵蓋跨角色／場景更換、NPC 死亡及同 ID 重生、伴隨物同 ID 替換與世代更新、等鎖逾限／中斷及八次實際封包 I/O 故障。沒有新增玩家欄位、排程器、ThreadLocal 或物件登記表。範圍伴隨物直接沿用實際 JAR 已有的 Context 身分收據。

武器破壞改為在 Source 的 BOSS 小數計算完成後才套用層數；父層獨立 Source 公式比對的 248,832 組前綴及 34,496 組 Tournament 消費端，原有 30,958／6,692 個差異均歸零；實際 HP 對照的 46 個差異也歸零。這限已列的五個數值消費端，不代表所有 NPC 傷害核心已驗收。Source 動畫可先於非同步 HP 提交；已換角的同場景觀看者可收到目前主要角色的即時頭頂血條，但不得接收舊範圍動畫或自己的舊 HP 更新。

TAMED 增量候選 `fe869f8fbb7d44da695b9a346aac83313b347bde077a0fcc52d51fb9bd8302d9` 同 SHA 通過 **188 案／3,298 檢查**：BATTLE 36→145 的 Quest／Time Attack 封包 24 案、36／41 目標生命週期與建立失敗 80 案、解放登記後失敗與重試 24 案、最後一隻 NPC 捕捉觸發下一波／目標物失敗回復及重試 24 案、既有 GM 模式捕捉／解放 12 案，以及既有 WORLD TAMED 的 24 案／520 檢查。BATTLE 背包改為同一份實體移交，MP 為零／已耗損及 HP／MP 超過上限均直接保留；未膨脹模板上限。NPC 血條只在封包顯示時限制為原生比例，實際單點傷害與生命值保留另有驗證；玩家血條驗證未放寬。快照中的建立暫存於發布後清空，未增加玩家欄位或工作執行緒。失敗回復涵蓋怪物清單、原背包、關卡、目標物、隊伍計分與 AI 發布時限；GM 解放沿用既有 GM 模式判定，保留一般競技模式門檻。

前一增量候選為 `0c3a5758b964abb4f8944f82b22e8306a1217a557813dff5d8df952aadb16322`。一般 Quest NPC 的動態座標、畫面感知、目標與占格接線已加入既有時鐘；玩家與伴隨物落格在同一 Hub 邊界重查，怪物的九個占格消費端讀取不可變座標快照。Source 四個停用 AI ID 與零 HP 啟動限制均保留；休息節拍跨場景清理／重入保持共用。角色切換、同 ID 伴隨物替換、首次出現／移動競態、相消後外型、晚到送包與關閉後不重建均已測。沒有新增玩家欄位、ThreadLocal、工作執行緒或排程；Hub 僅增加關閉旗標與休息時點，場景與觀看者資料沿用既有容器及弱參照。

該 SHA 通過 **50 案**（32 個移動／生命週期案例＋技能 68／73 的 Main 10＋8 案），另有 **294,917 個移動／關門／占格核心案例**。前一份 `c8e9f69b…` 同 SHA 已重播 **764 案**（含 288 成對／Ctrl、18 Main、188 TAMED、238 NPC、32 移動案例），已凍結逐項收據。最後休息節拍調整只改兩個 class；除明列的 `state/tick/restPhase` 外，1,051 個方法與所有其餘封存項目完全相同。這是有限增量比對，並非最新 SHA 的全套回歸。

仍待補齊：Source 障礙物回家路徑及 LOS／繞路選向的完整對照、出生／強制移位／傳送占格、更廣 NPC 建構／到期／關閉路徑，以及所有技能的職業／目標／Ctrl／範圍／效果生命週期矩陣。技能 27 的本輪限定驗證見下段；BATTLE 伴隨物仍待補齊。NPC 背包超出現行原生容量的 Source 等價行為仍開放；目前採整筆拒絕捕捉保全資料，沒有宣稱容量已完全對齊。最新候選尚未完成全套同 SHA 回歸。

技能 27 父層開發檢查點 `ef781449052476748ec5bd64a77954bd681f9e0388c2c52428354974f09f3867` 同 SHA 通過 **2,312 案／已計數 42,643 項檢查**（Main 檢查未併入檢查數）：技能 27 四類 NPC 的一般／Ctrl 各 96 案、WORLD 與 BATTLE 自身／跨主人召喚物的一般／Ctrl 各 60 案、兩場景材料／效果／競技語意各 60 案、玩家材料／故障／生命週期／權限 200 案、共用成對／Ctrl 288 案、NPC 物理範圍 26 案、Main 68／73 的 10＋8 案、八種 NPC／伴隨物的材料儲存故障與立即重試 32 案，MP 數值與 NPC ID 撞值 20 案，以及七職業／三組原始 INT／14 類實際目標／一般與 Ctrl／兩種物品套用模式的 1,176 案。[固定收據](../../tools/combat-continuation/integration-lifecycle/build/weapon-target-suite-current.json)已封存逐項入口；此為限定範圍，並非全部技能或完整回歸。最新凍結增量可重建為相同 SHA，完整父層鏈重建仍待最終階段。

技能 27 在第一個玩家鎖等待之前擷取 NPC／主人角色身分及所需目標收據，避免同 ID 重生、主人換角或 STOPPED 期間遲取收據後仍扣料。沿用實際 Area Context、原技能主體及既有成對鎖；在鎖外讀取冷權限。NPC 世代或觀看者角色退役時丟棄舊特效，仍保留施法者已提交 MP 的確認。此確認例外限定技能 27，其他技能保留既有退役規則。I/O 故障、MP 不足、沉默、觀戰、主人鎖等待、晚到輸出與收據歸零皆有原始封包案例。沒有新增玩家欄位、ThreadLocal、工作執行緒、排程或登記表；Context 的三個暫存欄位隨施法作用域釋放。BATTLE 召喚物已補沿用現有材料／Source 機率／輸出權限的新消費端，模板與冷權限在 Hub／記憶體監視鎖外讀取。一般同隊門檻、只放寬選中目標的 Ctrl、原生 PET_RACE 施法拒絕、GM 僅免材料、地縛／消魔／睡眠、PET 與 TAMED 均已測。兩種持久化旗標的記憶體儲存故障不會部分扣料或生效，立即重試只提交一次。

合法 MP／最大 MP 的四個資料位元組可剛好等於 NPC ID；舊版 20 案均漏發目前施法者的 MP 確認，修正後歸零，舊 NPC 特效仍被擋下。輸出先辨識 S_ACTION／S_EFFECT／S_POISON 才讀取物件 ID；這份增量只改一個判讀方法，其餘 helper 方法／欄位及所有其他封存項目均相同。上述七職業矩陣已驗證消耗與實際耐久／NPC 層數；更多效果與外控邊界仍須逐項驗證。

造屍／馴服的整合候選 `4982a8c0…` 同 SHA 通過 **770 案／已計數 19,752 項檢查**，含 Main 68／73 的 10＋8 案。新增造屍的 BATTLE 七職業／期限／主人 56 案、WORLD 七職業／原怪物重生／期限／主人 28 案、WORLD 排程及名冊失敗回復／重試 28 案、既有 GM 標記下的造屍 28 案；36／41 跨角色等待、換角／死亡／房間／同 ID／復活競態與建構失敗共 88 案。馴服的 100 案、共用成對／Ctrl 288 案、物理範圍 26 案及既有 TAMED 回歸亦納入。[凍結逐項收據](../../tools/combat-continuation/integration-lifecycle/build/create-zombie-suite-current.json)

造屍沿用既有建構、屍體消費、名冊及解放入口，使用預載 Source NPC 模板的 HP／等級／外型／名稱；物件種類為 Source 的 TAMED，3,600 秒採既有 Hub 時鐘的共用十秒節拍。任務推進先移除舊怪物時，由既有期限屍體快照持有原背包及舊物件弱參照；造屍、競爭復活及解放均移交同一背包，不複製或移入玩家經濟。WORLD 先受理弱參照／世代重生回呼再移除屍體，失敗保留原物品；舊出生點先返回後，仍以正確殭屍模板、直接 HP／MP／朝向與零經驗解放。沒有新增玩家欄位、執行緒、排程、ThreadLocal 或物件登記表；四個暫態欄位限既有屍體快照。

父層檢查點 `420eeb7d4b6fed1c61cc310cfd691b3e9e50075578d1c2ff605c8ce34ce294d7` 同 SHA 通過 **942 案／已計數 22,893 項檢查**，包含 Main 68／73 的 10＋8 案。Source 狀態 1～5 與既有文字別名現在更新同一主人的 SUMMON／TAMED（含造屍）群組，排除 PET 及其他主人；兩個既有指令入口傳入原先捕捉的主人／物件收據，群組在等鎖前固定。100 案中的 80 個舊差異歸零，另 64 案驗證換角、場景／連線退役、選中物件或同伴死亡、同 ID 替換及等鎖後新增成員隔離。已驗證目標與動作時點重設；**跟隨、防禦、伸展、警戒回家及完整 hate/path 消費端未因此視為完成**。[凍結逐項收據](../../tools/combat-continuation/integration-lifecycle/build/summon-group-suite-current.json)

此增量只改兩個指令呼叫邊與共用 helper 的既有 apply／release 入口，加入原收據及群體處理；helper 全部欄位、其他既有行為與所有其他封存項目保留，僅正規化 javac 合成 lambda 編號作比較。沒有新增玩家欄位、排程、執行緒、ThreadLocal 或登記表。凍結輸入可重建相同 SHA。前一 GM 規則身分修補 `7d46ff2c…` 的 36 案，以及造屍整合 `4982a8c0…` 的 770 案，保留各自 SHA 證據，不與最新案例累算。

父層檢查點 `09ea36d1ce95eb15504715d16f243c0f7f3cb6748cabb2a4cd6c6dce5b8eda4c` 同 SHA 通過 **1,026 案／已計數 24,970 項檢查**，含 Main 68／73 的 10＋8 案。一般 SUMMON 的原始數字 6 與既有到期入口先執行真實 HP0 死亡，再沿用既有原子移交及地面後備；失敗時保留同一個已死亡物件、原背包及原角色／場景／生命週期收據，由既有時鐘重試。角色退役、同 ID 替換、場景切換、斷線或 Hub 關閉後不會把原背包存入新上下文。移交成功後清除收據，Source 特效 169 與移除各發送一次，沒有再次扣料或施法。新增 8 案直接指令／到期、64 案滿載／交易失敗／重試／生命週期（實際注入 56 次儲存失敗）；另 12 案以真正技能 51／154／162 封包驗證三個既有 factory 的 1,800 秒共用十秒期限及到期完整移交。[凍結逐項收據](../../tools/combat-continuation/integration-lifecycle/build/summon-terminal-suite-current.json)

此增量只改四個既有 class，新增一個無狀態 helper；既有伴隨物增加一個 volatile 清理收據，沒有新增玩家欄位、工作執行緒、排程、ThreadLocal 或全域物件登記表。原群組及 TAMED 解放方法保留，三個 factory 與兩個時鐘的精確接點已驗證，凍結輸入可重建相同 SHA。先前父層的 4 案數字 6 不清理／4 案到期孤立背包，及活體移交能力觀察均保留為差異證據，未與新版本累算。

父層檢查點 `9b8d9567da87634738b57b76d2f3e739808e1e6b328c1fd084dc3388d18eaf9c` 同 SHA 通過 **1,138 案／已計數 27,270 項檢查**，包含 Main 68／73 的 10＋8 案。新增 WORLD 受擊與 BATTLE 傷害回饋兩個實際共用核心的 8 案、技能 4／17／25 單體／直線／範圍跨主人及一般／Ctrl 原始封包 24 案、滿載地面後備／交易失敗／重試／角色與場景／生命週期／STOPPED 的 80 案；其中實際注入 64 次儲存失敗、8 案在已受理動作中切至 STOPPED 後仍完成結算。父版兩場景的 8 個背包未移交差異歸零。[凍結逐項收據](../../tools/combat-continuation/integration-lifecycle/build/summon-combat-suite-current.json)

一般 SUMMON 受擊致死先保留原背包與死亡世代，再沿用原子移交／地面後備及 Source169／移除；成功後略過舊保留屍體動畫與重複移除。BATTLE 的呼叫者可為施法者，依既有同場景名冊及物件身分找主人，使用原本已受理的施法者收據與雙方鎖完成物品交易，不另受理一個施法。失敗保留原主人收據及原背包，由既有時鐘重試；已死亡後施法者退役不會取消仍有效的主人結算。直線技能也命中玩家時，原始封包驗證會等待玩家傷害工作及輸出收據都歸零，再判定鎖外送包與清理結果。

此增量只改四個既有 class，沒有新 class、欄位、工作執行緒、排程、ThreadLocal 或登記表；兩個傷害呼叫與兩個末端判斷的精確接點、既有作用域的兩個存取入口已比對。原主人／到期、群組、PET／TAMED 路徑及所有其餘封存項目保留，凍結輸入可重建相同 SHA。**這仍不是完整 Source／全部材料技能驗收。** 斷線／移位與主人退役後仍持有物品的最終結算（Source worker deleteMe 僅落地、logout deleteAfterInventoryReturn 優先還主人，兩者不能混用或讓 NPC worker 進玩家 JDBC）、其餘真正 NPC 致死生產端、全部攻擊與材料技能的施法受理／職業／目標／Ctrl／範圍／延後 I/O 矩陣、指令空白／前置零正規化、fake 類型，以及跟隨／防禦／警戒回家／hate/path AI 均保持開放。下一個實際 JAR 的[方法快照](../../tools/combat-continuation/integration-lifecycle/build/summon-combat-next-methods.txt)為唯讀稽核，沒有將靜態呼叫清單當作執行驗收。

完整材料／職業／目標／Ctrl／範圍／效果／Source 外控矩陣、NPC 初始掉落／非同步屍體移交 provenance、無限容量／長計數、更廣 NPC 建構及 AI 路由，以及最終同 SHA 全回歸與集中原生介面驗證仍開放。保留整筆拒絕超容量馴服及全部原物品的限制，131 仍停用。未部署、未啟動遊戲、未寫 Source／玩家 SQL／WAL。[目前稽核與下一入口](../../tools/combat-continuation/integration-lifecycle/build/source-summon-lifetime-audit-current.json)


續作中的 worker 純落地元件另以 `1ad4cd9c…` 通過 **48 案／692 檢查**：4 案基本交易、44 案故障及生命週期，含 12 次三層預約／世代／場景失敗、實際 Hub 斷線及關閉、主人換角／移場、晚到觀看者退役與弱參照失效。玩家 Repository 呼叫為零；原背包原地排空、HP 不偽造為零，落地保留原數量與資料；失敗保留原背包並回滾預約。場景資料只以弱參照及原身份持有，沒有增加執行緒、排程、ThreadLocal 或登記表。父版全部封存項目逐位元相同，只新增兩個未接線 class，凍結輸入可重建相同 SHA。

**此元件尚未接入正式派送或退休入口，不能宣告斷線／移位清理完成，也沒有把這 48 案與 `9b8d9567…` 的 1,138 案累算。** 父版實際清理／Hub 斷線及 16 個落點由真正伴隨物占用的移位入口，已重現 6 案背包仍留在不可用舊物件。接續須在移除舊持有者之前受理並持有清理工作；logout 優先還主人，worker 僅落地且禁止玩家 JDBC，並補齊排程拒絕／失敗重試、末位成員與場景關閉及原生 Main 的完整生命週期。現行整合檢查點維持 `9b8d9567…`，未部署、未寫 Source／玩家 SQL／WAL。[元件與原始缺口收據](../../tools/combat-continuation/integration-lifecycle/build/summon-retirement-progress-current.json)


目前召喚物退休檢查點 `3bda9d5db34b…` 同 SHA 通過 **1,206 案／已計數 28,520 項檢查**，含 Main 68／73 的 10＋8 案。新增 64 案真正 Hub 斷線／shutdown／close、主人優先返還、工作池及計時器拒絕、STOPPED、角色／場景／物件退役與重複清理，另 4 案驗證生命週期尚未安裝的早到時鐘及失敗後重新滿載的重試節流；實際注入 52 次主人提交失敗、16 次三層落地預約失敗，worker Repository 呼叫為零。前段 `1ad4cd9c…` 的 48 案僅為歷史未接線元件證據，未累加到本輪。

斷線在原 synchronized Hub 移除主人的容器之前取得清理責任；能返還時沿用原子主人交易及地面後備，失敗交由既有工作池及計時器持有同一背包，沒有退役後玩家背包／JDBC 存取。Source 普通刪除保留真實 HP；死亡仍走既有 HP0 路徑。地面完成後只發布一次 Source169／移除，場景或物件世代終止則取消工作並釋放引用。新增欄位限原伴隨物、短期 Target 及既有 application lifetime 各一項；沒有新增玩家欄位、執行緒、排程器、ThreadLocal 或全域登錄表。精確八個既有／四個新增 class 差分與凍結重建同 SHA 已驗證。移位無落點、實際 socket／換角與更廣 Main 退休、其餘材料技能／Source／AI 與最終全回歸仍開放；競技、超容量整筆拒絕及 SQL／WAL 限制保留，未部署。[逐項收據](../../tools/combat-continuation/integration-lifecycle/build/summon-retirement-suite-current.json)


傳送續作已用現行 `3bda9d5d…` 的真正 `C_GOTO_PORTAL` 重現 **12 案／4,198 檢查**：兩種物品套用模式、同圖／跨圖、原 16 格受阻、整個 Source 搜尋範圍受阻及正常落點。8 案留下被停用的原召喚物與同一背包；正常 4 案保留活體。12 案都更新主人動作 token，即使地圖／副本不變也一樣；只有停用的 8 案更新伴隨物 token。未放寬副本編號限制，跨圖案例使用真正支援的 4→0 地圖。最初以不相容的高 HP／MP 測試初始化及普通地圖的非合法副本編號造成的失敗，均保留原始紀錄；正式案例沿用原生重建生命值並通過原有地圖規則。

Source `CheckUtil.findNearestLivingClearLanding` 的完整方法體另抽成獨立參考，只投影原子落點提交入口及日誌；未重寫中心／外圈順序、半徑或例外邊界。尚未接線的 `b81dbbb52b6f…` 搜尋核心通過 **16,518 組／68,125 檢查**（8 次提交失敗），包含每格唯一可用、所有格受阻、半徑外格、連續多物件占格及完整候選座標軌跡。父版全部封存項目不變，只有兩個無欄位 class，沒有玩家／Hub／SQL／檔案／執行緒／排程／ThreadLocal／全域登錄表依賴；凍結輸入可重建相同 SHA。此為未接線搜尋端口，原生地形、占格、場景持有與並發原子提交尚未驗收。

實際 JAR 已盤出 61 個伴隨物快照呼叫、4 個單一快照讀取、4 個占格呼叫與唯一 portal 移位入口；包括跨角色／範圍材料、復活、回歸自然、治癒、普攻、視野、NPC 目標、期限與掉落等消費端。必須一併處理原場景物件位置與主人合法傳送收據，不能僅略過 deactivate 讓舊物件被新圖查詢。上述 12 案差異及搜尋核心案例均未併入整合版 1,206 案，也未宣告傳送或完整 Source 已完成。[獨立收據及消費端清單](../../tools/combat-continuation/integration-lifecycle/build/companion-placement-progress-current.json)


傳送及跨場景目標續作另存 `1281cf6542ee…` 實驗候選，保留整合檢查點 `3bda9d5d…`。真正 portal 已接上物件自己的場景；找不到落點時保留活體、HP、原位與同一背包，成功移位才從舊場景移除。普通攻擊魔法 4、直線 17、範圍 25 的伴隨物迴圈獨立納入仍留在舊圖的主人；玩家目標與觀眾名冊仍用真正場景。範圍環境在等待前捕捉主人綁定、原物件、物件生命與位置世代，沿用現有 Source 鎖排序；主人憑證在等待鎖定前保留，施法完成後釋放；已獲准動作在 STOPPED 仍可結算。死亡只新增供送包驗證的結果憑證，不會再次成為可施法的活體目標；物件生命、主人、同 ID 新物件及場景替換仍會使送包失效。普通 NPC 查詢與致死維持既有入口，未放寬交易或生命週期閘門。

同 SHA 已通過 **632 案**（檢查計數不含 Main 未彙總部分）：24 案實際封包／Ctrl 對照，新圖誤命中與舊圖漏找均為 0；36 次捕捉失效檢查與 12 次主人鎖占用；另 36 案讓真正封包在等待鎖定時觸發主人角色、物件生命或移位世代退休，全部無傷害、MP／材料扣除或背包變動。回歸含 Main73 的 8 案、Main68 的 10 案、競技寵物 88 案、NPC 物理範圍 26 案，以及 portal／交接／競技移位／成對 Ctrl／召喚物致死故障 416 案；另 24 案中 12 案於真正施法中觸發 STOPPED，仍一次結算並送出 Source169／移除，且所有主人憑證及引用於結束時釋放。凍結重建同 SHA，四個既有 class 與三個 helper／憑證 class 以外的封存項目完全相同；僅新增施法環境的一個暫時憑證欄位，不新增玩家欄位、執行緒、排程、ThreadLocal 或全域名冊。

仍須補其餘技能與材料目標消費端、舊場景視野／AI／仇恨／移動／毒傷／到期、Source 隨機落點和地形占格精確投影，以及 PET／TAMED 移位與解放；本次限定通過不代表完整 Source 或最終全回歸。正式程式、固定輸入、基準與原整合指標雜湊未變；未部署、未啟動遊戲、未寫 Source／玩家 SQL／WAL。[限定接線與待辦收據](../../tools/combat-continuation/integration-lifecycle/build/companion-actor-target-progress-current.json)


舊場景到期續作 `f66e13a0ead5…` 已接進真正 WORLD 伴隨物時鐘。Source `SummonTimer` 每十秒直接巡覽召喚物，正數期限的一般召喚物到期走 `Death(null)`，不以主人所在場景過濾；原候選 8 案均漏掉跨圖後仍留原圖的到期物件。新接線先捕捉角色、原物件、場景、位置世代與生命，再取得既有主人鎖，於 Hub／物件鎖內重驗並沿用原終止交易；等待主人時不持有 Hub，不新增時鐘、工作池、ThreadLocal、玩家欄位或全域名冊。24 案／22,961 檢查含兩次巢狀主人／落地回復、16 次真正等待，以及期限前、未初始化、同 ID 替換、原背包身分、角色／生命／位置／場景退役和 STOPPED；合法移位後由新場景時鐘結算，舊場景工作不送包。

該到期候選同 SHA 已重播 **656 案／已計數 101,257 項檢查**，包含前輪跨場景目標／Ctrl、等待中退役、Source169、STOPPED、Main73 8 案、Main68 10 案、競技及結算故障；一個 PC 時鐘前綴與兩個 helper class 以外的父版項目完全不變，凍結 Source／輸入可重建同 SHA。此範圍僅涵蓋仍存活原場景中的正數期限一般 SUMMON；TAMED／PET 到期、永久結束場景與更廣 AI 等消費端尚未完成。

技能 27 另存 `94eab3441762…`：原先已有施法前物件與主人收據，但後段 WORLD 查詢再次沿主人名冊，跨圖後 4 案漏找。只調整伴隨物迴圈／快照與兩個伴隨物場景檢查，玩家條件及 BATTLE 原方法保留。新增 36 案／34,327 檢查已使新圖誤命中、舊圖漏找均歸零，包含 16 次等待中的角色／生命／位置／同 ID 替換，及缺料、MP 不足、地縛、正常／Ctrl；保留 Source 原機率、扣料、扣 MP 與效果順序。再於同 SHA 重播 2,312 案 NPC／玩家／七職業矩陣、GM、材料故障、成對／Ctrl、競技與 Main，合計 **2,348 案**，僅既有 PC 的該 WORLD 方法改動，無新增 class／欄位／執行緒。

上述兩個候選各自計數，不相加，不取代整合版 `3bda9d5d…` 的 1,206 案。其餘材料、跨場景 NPC 傷害／回饋 Ref、視野／AI／毒傷、NPC 建構與容量等 Source 語意及最終全回歸仍開放；原生 Ref 的地圖／副本仍取自主人的問題已定位，原角色查找本來就有已連線名冊回退，不新增重複登錄表。另已凍結 Source AI／傳送呼叫鏈：noTarget 的三參數 teleport 實際仍用 NPC 當前圖，不能照註解誤接成跨圖；portal 在嘗試落點前先改 showId，失敗未還原，跨副本失敗仍需另建有效案例。[唯讀契約](../../tools/combat-continuation/integration-lifecycle/build/companion-source-ai-audit-current.json)。正式程式、固定輸入、基準及原整合指標保持原雜湊，未部署、未啟動遊戲、未寫 Source／玩家 SQL／WAL。[限定續作收據](../../tools/combat-continuation/integration-lifecycle/build/companion-expiry-weapon-progress-current.json)


負向材料與地縛的跨角色續作另存兩個候選，未提升整合檢查點。29／152／153 `b4e9f0d8ca8d…` 同 SHA **1,238 案**：新增96案含48次主人鎖等待，12案實際 STOPPED 後既有收據完成／新施法拒絕，6案提交後送包前退役，以及既有792案玩家／NPC／伴隨物／競技／故障／生命週期與332案成對／範圍／Main68／73。原場景漏找的24案父版重現中有12案失敗；新圖誤命中與舊圖漏找皆0。另重現速度封包未驗目標生命的4案缺口，補入既有送包分類後歸0；效果仍只送一次、鎖外輸出，角色／生命／位置／同ID替換在等待後均拒絕。正常封包保留真正原生隨機結果與Source90%上限，不假定設定100即必定成功；提交後退役則使用既有typed roll1以確定測到速度封包。原玩家／競技判定、機率、扣料、MP及效果本體保留，既有預期封包逐一相等。

地縛157 `aaec3b60b70b…` 同 SHA **576案**：32案跨場景／Ctrl／缺料／MP與16次等待，4案STOPPED，208案既有玩家／NPC／競技／故障／生命週期及332案成對／範圍／Main；父版8案中4案漏找，新候選歸0。原先只選玩家作成對鎖的缺口已補為捕捉到的伴隨物主人；只改WORLD伴隨物查詢及入場接線，BATTLE方法與玩家判定保留。兩候選皆可從凍結輸入重建同SHA，不新增玩家欄位、ThreadLocal、工作池、時鐘、全域名冊；正式程式、基準、固定輸入及整合版3bda9d5d的1,206案保持原雜湊。不同SHA不相加，34技能／完整Source、其餘範圍與回歸自然／復活、視野／AI／毒傷／Ref／容量／完整外控及最後全回歸仍開放；131維持停用，超容量馴服整筆拒絕。未部署、未寫Source／玩家SQL／WAL。[限定續作收據](../../tools/combat-continuation/integration-lifecycle/build/companion-negative-earth-progress-current.json)。

既有生命週期測試保留原檔，凍結副本只調整兩項與現行成對契約不符的假設：等待中由私有數量1改為確有收據（父版與新候選實測均為2）；送包前目標退役改驗整批舊封包取消、已提交扣費恰一次，並由真正獨立MP生產端驗一次可解密更新。父版與新候選另案均通過，原鎖等待、角色／場景失效、STOPPED、效果、socket失敗及最終收據歸零保留。外控另做唯讀快照：目前69鍵，既有對照表57列Source技能／傷害鍵均對應；這僅證明設定載入，不代表完整Source公式、消費端或全部外控完成。


回歸自然145跨角色續作 `6a5a36c55e4a…` 同SHA **684案／已計數86,742項檢查**。父版8案重現4個原場景漏找；新32案含16次真正主人鎖等待、角色／生命／位置／同ID替換及正常／Ctrl、缺料／MP，漏找與誤命中皆0；另4案STOPPED只完成已持有收據的施法。原背包交回實際遠端主人、刪除保留實際HP，效果與移除只送原場景。新增8案真滿庫存與4次主人儲存故障：兩個原非堆疊物件只落原位置／原圖，失敗完整保留原背包、主人庫存／記憶體帳與所有巢狀落地保留；結束無殘留施法Context／主人權限。

提交刪除後、送包前的同ID替換另重現4案過期特效／移除：原Stage只掃原場景玩家，未見遠端主人持有的原場景新物件。現在沿用既有已連線名冊按物件場景補入主人，4案歸0；保留原已提交刪除／Source169與背包交易，不以活體判斷取消成功刪除。原Nature的136案封包與預期值逐一相等，8次材料故障、36案生命週期／競技、332案成對／範圍／Main68／73皆通過；共用Stage另重播124案主人退休／worker／回復重試／死亡送包／期限。凍結輸入可重建同SHA，僅PC的Nature目標查詢、Nature入口／入場接線及Stage刪除主人查詢三個既有class與一個無狀態helper；不新增玩家欄位、ThreadLocal、工作池、時鐘、全域名冊或SQL／檔案交易。

舊生命週期測試的凍結副本保留原斷言與原檔，僅把私有收據數1改為有保留收據、將原始封包STOPPED等待點明確置於真正成對鎖，並區分同ID替換仍可送施法者MP與主人角色退役取消整批舊封包；已提交扣費仍恰一次。不同SHA的前輪證據不相加，整合檢查點仍3bda9d5d的1,206案；其餘材料／完整Source、復活、範圍次目標、視野／AI／毒傷／Ref／NPC建構／長數量容量與外控及最終全回歸持續開放。競技／記憶體／資料限制保留，131停用、超容量馴服整筆拒絕。未部署、未寫Source／玩家SQL／WAL。[限定續作收據](../../tools/combat-continuation/integration-lifecycle/build/companion-nature-progress-current.json)。


復活61／75跨角色續作 `63f38c850e28…` 同SHA **1,208案／已計數123,784項檢查**。父版16案重現8個原場景屍體漏找；64案正常／Ctrl、兩技能與兩種app情境包含32次真正主人鎖等待、角色／死亡再復活／位置／同ID替換、缺材料與MP，漏找及誤命中均0；另8案STOPPED只完成已取得收據的施法。保留Source復活HP、原背包與主人狀態、玩家同意回覆及競技規則。

普通NPC另64案：父版重現32次過期屍體入場及24次過期復活送包；捕捉實際actor／population／epoch／位置，在等待後及復活後送包前重驗，皆歸0。PET復活後另24案，死亡、同ID新物件或位置變更均撤銷過期效果／復活包；父版24次過期送包歸0，已提交材料與MP仍恰一次。巢狀施法16案含8次真鎖等待，先重現外層其他技能Context繞過屍體捕捉；以每次施法Context專用boolean辨識自己的範圍，復活建立獨立捕捉且完整還原外層狀態。

700案既有玩家／NPC／PET／回覆競態／競技／材料故障／生命週期通過，材料256案原封包逐一相等；另332案成對鎖、範圍及Main68／73關閉通過。生命週期凍結副本沿用現行收據／整批取消契約，STOPPED等待點限真正成對鎖；原始測試不改寫。可重建同SHA，僅改PC、既有施法Context及復活入口三個class，新增helper與兩個每次施法捕捉class；唯一新增欄位在施法Context，無玩家欄位、ThreadLocal、工作池、時鐘、全域名冊或資料寫入機制。

不同SHA案例不相加，整合檢查點仍3bda9d5d的1,206案。其餘材料技能、完整Source外控／範圍次目標／AI與最終全回歸持續開放；競技與記憶體限制保留，131停用、超容量馴服整筆拒絕。正式檔與固定基準雜湊不變；未部署，未寫Source／玩家SQL／WAL。[限定續作收據](../../tools/combat-continuation/integration-lifecycle/build/companion-resurrection-progress-current.json)。

成對鎖舊工具將改寫前來源雜湊誤指向改寫後同一路徑；另存原檔與正規化收據，核對唯一差異為過時runtime-origin class名稱。原收據、已編譯class與所有觀察結果完整保留，其他引用逐一雜湊核對；此證據路徑更正新增案例數為0。


範圍睡霧66／集體緩速76續作 `6ffd0fc0df88…` 同SHA **1,276案／已計數157,090項檢查**。實際失敗主人傳送後寵物留原圖：父版主目標16案與次目標16案各重現8個漏找；新主／次各16案的原圖命中與新圖隔離皆通過，正常與Ctrl、兩種app情境一致，MP／材料恰一次。次目標案必須同時真正影響有效玩家主目標，保留Source單一主目標施法特效，緩速次目標另驗原圖S_SPEED且遠端主人收不到。

64案生命週期包含48次真正主人Source鎖等待：角色、生命token、位置、原場景同ID替換、缺材料／MP，以及STOPPED前已持有收據者完成、新封包不扣費不送包。另32案於效果提交後、送包前變更生命／位置／同ID／主人角色，過期特效與速度封包皆撤銷；已接受扣費仍恰一次，結束無Context／工作／角色收據殘留，無鎖內IO。

只在WORLD的66／76沿用既有成對Scope與主人收據，在等待前固定同一個Context，再執行原有全玩家鎖／Hub／population路徑；不在等待後重抓新生命。私有patience／phantasm目標查詢另作限定副本，只擴充寵物迭代與物件場景判斷；原查詢、真玩家與BATTLE分支保留。施法本體期間才開啟原範圍輸出捕捉，送包前還原外層階段，修正延後送包再次捕捉而缺實際鎖的錯誤。僅改PC與NativeAreaMaterialAction兩個class、新增一個無狀態helper；不新增玩家／Context欄位、ThreadLocal、工作池、時鐘或全域名冊。凍結輸入可重建同SHA。

混合目標另16案含16次真正等待：前候選重現8次次目標主人退役竟取消有效玩家主目標。現在只排除失效寵物，保留玩家主目標與一次扣費；另8案STOPPED仍完成已持有權限的玩家主目標及原場景寵物次目標。不新增名冊或玩家欄位，不沿用新角色／新物件。800案原範圍玩家／普通NPC／寵物／競技／生命週期本體不改寫；兩組材料共288案封包與原預期逐位元相等。另332案成對／範圍／Main68／73關閉回歸通過。成對測試來源原檔雜湊直接指向未改寫的原始檔，避免先前舊工具的路徑別名問題。不同SHA證據不相加，整合檢查點仍3bda9d5d的1,206案；其餘材料、完整Source外控／其他範圍與最終全回歸維持開放。競技、記憶體、資料限制保留；131停用、超容量馴服整筆拒絕。正式檔與固定基準不變，未部署，未寫Source／玩家SQL／WAL。[限定續作收據](../../tools/combat-continuation/integration-lifecycle/build/companion-area-progress-current.json)。


WORLD角色／場景動作識別續作 `104716cdf0ad…` 同SHA **3,440案／已計數437,554項檢查**。共用動作token原先同時代表換圖與角色退役，40案真實獨立主人C_GOTO_PORTAL重現8次已提交原場景寵物效果被誤取消；新40案歸0，真正角色退役、生命、位置或同ID替換仍不送過期特效／速度。另48案在實際入鎖呼叫前以測試鎖注入排程停頓，完成真實主人傳送再續原封包：16次正常／STOPPED誤拒歸0，材料／MP只扣一次、無鎖內IO或殘留。此48案是可控制的入鎖前排程屏障，不混稱為另有鎖持有人造成的阻塞。

沿用原sourcePlayerOwner欄位，以每次動作Epoch指向單一Role。WORLD內場景寫入仍更新動作版本、仍執行原排隊取消與退休fence，只保留角色marker；重新選取同名角色、退役、離開WORLD／競技邊界及未知外部owner皆不延續舊角色。12案檢查4,096次WORLD場景更新與ABA／邊界／外部owner，舊玩家Binding仍嚴格失效；Epoch只有Role指標、Role只有volatile布林，不連前一代、玩家或Hub。沿用既有來源狀態slot並改為volatile，角色檢查可讀既有volatile owner，避免在Hub下取得主人status鎖。

共用WORLD物件捕捉與成對Scope改用角色、原Actor／生命／場景／位置，三個攻擊根共用同一份等待前Context；寵物State.role與傳送Transition.character同步辨識角色，禁止同名重選ABA後重新綁定舊物件。真玩家Binding、NativePairedCastSafety及NativePlayerLifetime的class逐位元不變；不全域放寬玩家換圖fence。僅改七個既有class、新增三個helper／bounded marker class，沒有新增玩家或既有class欄位、工作池、時鐘、ThreadLocal、全域名冊、SQL／檔案／玩家庫存依賴；凍結重建同SHA。

舊測試凍結副本只修正新入鎖入口的堆疊名稱，並在提交時另存Scope以核對關閉／收據清空／弱參照釋放，同時要求Context指標已清空；原始Java與所有行為／封包／扣費／STOPPED斷言保留。另3,340案原技能本體／既有適配斷言回歸包含範圍、單體／線／區域攻擊、復活回覆競態、負向、地縛、回歸自然、故障、背包交回／worker／死亡與到期，以及成對／範圍／Main68／73。不同SHA不相加，整合檢查點仍3bda9d5d的1,206案。唯讀Ref普查定位17個class、153個方法／建構／欄位入口；此為後續AI／毒傷等入口定位，非新增行為案例。特化復活／刪除等主人stamp、完整Source／外控／其餘材料與最終全回歸仍開放；131停用、超容量馴服整筆拒絕，競技／記憶體／資料限制保留。正式檔與固定基準不變，未部署，未寫Source／玩家SQL／WAL。[限定續作收據](../../tools/combat-continuation/integration-lifecycle/build/companion-role-progress-current.json)。


共用WORLD隨從Ref續作 `718abe6b7a49…` 同SHA **3,460案／已計數470,994項檢查**。原Ref使用主人場景，主人真正傳送後又以嚴格玩家動作Binding尋找，10案重現10個場景錯記與2次原場景有效Ref遺失；新10案皆歸0。現在只對WORLD kind2沿用既有連線名冊解析同角色主人，重新核對原物件、生命token、canonical custody及物件目前場景／弱Hub與population；正常主人換圖保留原場景物件，真正換角、生命更新、成功跨場景移動與同ID替換拒絕舊Ref。場景檢查限目前map／instance，未宣稱永久場景ABA失效。

另10案先在兩個原生提交worker的受控屏障前，經原生Calc與enqueueWorld送入真實PC傷害，再完成真正主人傳送／生命變動後放行；原10筆傷害皆恰提交一次並由原drain建立accepted Frame，不注入假的Frame。舊版2次有效Joy反饋被取消，新版恢復；其餘角色／生命／跨場景／替換案例不消耗舊Joy且不傷害新物件，按原抑制表核對DAMAGE action僅送物件原場景，遠端主人不收到場景動作；主人私有HP比例封包仍依原契約。所有收據／工作／Context清空，無鎖內IO、背包或主人HP／MP改寫。此為10筆佇列事件，不混稱為施法成對鎖等待。

只改NativeCombatAttacker與NativePlayerCommit兩個class，新增無欄位helper。NativePlayerCommit所有原有方法的正規化bytecode逐一一致；僅另增隨從解析分支。Ref record、ClientSession、玩家Binding、NativePairedCastSafety、NativePlayerLifetime逐位元不變；WORLD live查詢維持Hub在pet之前，BATTLE沿用原分支，無新玩家欄位／工作池／時鐘／ThreadLocal／全域名冊／SQL／檔案／玩家庫存依賴；凍結輸入重建相同SHA。另對兩個實際新fixture classpath各核對23個遊戲class皆來自此JAR，唯讀載入不啟動伺服器、不另計行為案例。

另3,440案原角色／傳送、跨角色、Ctrl、範圍、復活、負向、地縛、自然／背包、STOPPED及Main68／73回歸在此同一SHA通過，與歷史候選不混加。唯讀普查定位18個class、167個方法／建構／欄位入口，非額外行為案例。ProcHealingCompanion等特化主人stamp、AI／仇恨／毒傷、MP扣除及傷害提交／送包競態，連同完整Source／外控／其餘材料仍待補；131停用、超容量馴服整筆拒絕及競技／記憶體／資料限制保留。整合檢查點仍3bda9d5d的1,206案，正式檔與固定基準不變，未部署、未寫Source／玩家SQL／WAL。[限定續作收據](../../tools/combat-continuation/integration-lifecycle/build/companion-ref-progress-current.json)。


WORLD隨從Proc治療續作 `799c2001838a…` 同SHA **1,285案／已計數124,818項檢查**。6案真傳送配置重現2次原場景有效治療漏接與2次依主人場景誤治療遠端物件，新6案皆歸0；22案先以原生NPC FIFO接受治療，再完成真實主人傳送／角色、生命、物件、場景、名冊及關閉變動，4次正常傳送／DRAINING的有效治療取消歸0。兩個已取得原生claim並真正阻塞在actor監視器的STOPPED案例仍完成原HP一次；排程屏障與實際監視器等待分別記錄，不混稱為額外施法鎖等待。

WORLD特化withTarget在取得施法者／主人的實際Source成對動作鎖後才取Hub、原custody容器、actor及NPC狀態鎖。施法者保持完整動作世代與物理場景／名冊檢查；主人沿用bounded角色識別並核對原物件、生命、原場景與Hub／population。玩家狀態監視器不再於Hub內取得；舊887案的凍結副本僅把WORLD換角寫入的結構觀察改成確實等待同一原生工作者的Source動作鎖，背包容器與全部BATTLE監視器觀察，以及原HP、封包、材料／資源、來源向量、名冊、容量、死亡與關閉斷言保留。原始Java未覆寫。

另18案在已提交HP、尚未取得最後主人送包鎖時控制原工作者：8次有效私有HP表送出，10次角色／生命／同ID物件／名冊／背包替換過期封包取消，HP不回滾或重放。已提交HP的私有主人表不因施法者退役或同一活物件正常換場景而取消；施法准入仍必須符合物件場景。最後送包捕捉Source角色Lease並再次解析原生命／custody；延後IO閉包只持弱主人／弱Hub及Ref、封包等值證據，無鎖內IO。測試夾具對STOPPED預期中斷及RuntimeInventory替換型別的失敗已保留，通過收據不含失敗場次。

887案原Proc真實AI／accepted Frame／16個治療技能／全部狀態組合／來源HP向量／owner-only meter／1024共同容量與drain/drop通過；另352案含20案共用Ref／真實反饋及332案成對／範圍／Main68／73通過，全部為本候選SHA。3組新原生fixture classpath各29個遊戲class確認來自此JAR，唯讀載入不另計行為案例。只改NativeProcHealingCompanion、新增無欄位helper；原BATTLE withTarget本體、其餘原方法除指定publish橋接外、全部共用玩家／Ref／成對／生命週期／治療Kernel與私有receiver class均保留，凍結重建相同SHA。沒有新增玩家欄位、工作池、時鐘、ThreadLocal、全域名冊或SQL／檔案／玩家庫存依賴。

完整HP寫入／送包排序、非治療Proc、AI／仇恨／毒傷、其餘NPC factory與全部材料／職業／目標／Ctrl／範圍／外控及最終同SHA全回歸仍開放。本輪不混加前輪718abe6b的3,460案；整合檢查點仍3bda9d5d的1,206案，131停用、超容量馴服整筆拒絕、競技與記憶體／資料限制保留。正式檔與固定基準不變，未部署、未寫Source／玩家SQL／WAL。[限定續作收據](../../tools/combat-continuation/integration-lifecycle/build/companion-proc-progress-current.json)。


WORLD隨從MP提交續作 `30d414f0c0c8…` 同SHA **480案／已計數63,678項檢查**。JDK JDI僅停住原JAR的實際事件執行緒，未改寫或覆蓋遊戲class；8案精準停在原debit的成功live查詢後，再由另一條真實執行緒完成主人／施法者角色退役、同ID替換或原生成功傳送，重現8次過期MP扣除。新8案在相同原數值核心位置暫停時，競爭寫入確實排在同一工作者的實際Source動作鎖後，原有效扣款恰一次，再完成退役／替換／換圖，過期扣款歸0。此為可控制的原生bytecode事件點與真實鎖等待，非以覆蓋PC或虛構Frame替代。

另24案實際物件／主人分離場景與請求負值、零、正常、INT_MAX對照，4次依主人場景誤扣遠端MP歸0，原requested／available-MP截斷與正數核心不變。再6案由真正AI進入原Calc、原PC佇列並先提交HP，在原debit入口bytecode 0前退役施法者的凍結Frame：舊版6次NPC被扣MP而玩家無法收取，新版6案全部拒絕NPC副作用，已提交HP不重放。Frame這組事件點是debit入口，與前8案成功查詢後的事件點分開記錄。

WORLD分支把施法者／主人的實際成對動作鎖、Hub、原custody、物件與NPC狀態鎖延續至原MP改值完成；已有accepted Frame沿用原目標Binding與validFrame，不重新捕捉新角色。正常主人換圖可維持原場景有效物件，施法者仍需符合物件場景與名冊。只改NativeCombatAttacker並新增無欄位helper；全部原數值、live、傷害與特效方法逐一正規化bytecode一致，原debit僅改名後由WORLD邊界委派，其他家族走原本體。整個其餘JAR逐位元不變，凍結重建同SHA，沒有新玩家欄位／工作池／時鐘／ThreadLocal／名冊／SQL／檔案／玩家庫存依賴。

90案凍結九家族真實AI／MP上下限／反射／Mortal／Joy／角色與新生命回歸，另352案共用Ref／真實反饋／成對／範圍／Main68／73全部同SHA通過。3組新原生fixture classpath各31個遊戲class確認來自此JAR，唯讀來源核對不另計行為案例。夾具final繼承及傳送外加鎖造成的失敗保留，新版傳送採真實原生入口並保留無鎖內IO斷言；原有已封存Java／測試未覆寫。

反射HP／特效完整提交與送包、已擁有MP臨界區中途STOPPED時的扣除／玩家收取合併、全部HP寫入／送包排序，以及AI／仇恨／毒傷、NPC factory、其餘材料／職業／目標／Ctrl／範圍／外控與最終同SHA全回歸仍開放。不同SHA不混加，整合檢查點仍3bda9d5d的1,206案；131停用、超容量馴服整筆拒絕、競技／記憶體／資料限制保留。正式檔與固定基準不變，未部署、未寫Source／玩家SQL／WAL。[限定續作收據](../../tools/combat-continuation/integration-lifecycle/build/companion-combat-progress-current.json)。


WORLD隨從MP扣除／玩家收取合併續作 `c8520dd42e49…` 同SHA **498案／已計數64,237項檢查**。6案原生AI→Calc→accepted PC Frame在原JAR成功NPC查詢後由JDI停住實際工作者：父層30d414f0有6次NPC扣7MP而玩家未收到；新候選6案關閉執行緒確實阻塞在該工作者持有的既有admission fence，NPC扣7、玩家收7、只送一次原生MP封包。另12案在新轉移入口bytecode 0先完成真實生命週期切換，STOPPED的6案雙方MP均不動，DRAINING的6案讓既有工作雙方恰一次完成；6案退役Frame亦全部拒絕NPC副作用。三組均為真正排程的accepted Frame，未虛構Frame、覆蓋遊戲class或新增工作者。

原子區先保留原生完成收據，再取得實際成對Source動作鎖、Hub、原custody、物件／NPC狀態，最後延續既有admission及該Frame原queue的vital鎖；重驗角色、物件生命與實際場景後執行原數值debit。玩家收取沿用原recover數值／FIFO完成本體的精確複本，只改為既有Frame queue與預留收據，避免Hub內重新取得玩家status monitor。既有Source邊界將IO延後至全部動作、Hub、容器、物件、狀態、vital及admission鎖外；實際加密MP封包與主人資源隔離已斷言。

只改equipment的一個呼叫及新增原recover複本；全部原PlayerCommit方法、數值與欄位均正規化一致，其他JAR逐位元不變，僅新增無欄位helper。凍結輸入重建同SHA，3組原生classpath各32個遊戲class確認來自同一JAR。8案查詢後真實角色／同ID／傳送競爭、24案實際場景／數值、90案凍結九家族與352案Ref／成對／範圍／Main68及73同SHA重播通過。失敗試跑保留；不同SHA不混加，整合檢查點仍3bda9d5d的1,206案。

完整反射HP／特效與所有HP寫入／送包排序、AI／仇恨／毒傷、其餘材料／職業／Ctrl／範圍／Source外控及最終全回歸仍開放。131停用、超容量馴服整筆拒絕、競技與記憶體限制保留。未部署、未啟動遊戲、未寫Source／玩家SQL／WAL。[限定收據](../../tools/combat-continuation/integration-lifecycle/build/companion-mana-progress-current.json)。


WORLD隨從反射HP續作 `3386173a0868…` 同SHA **492案／已計數41,718項檢查**。真正AI／Calc／accepted Frame的原JAR JDI對照重現6次成功查詢後退役仍扣7HP，以及6次STOPPED後仍提交HP；新候選在新入口先退役的6案全部拒絕，原查詢後的12案則讓真實主人／施法者退役執行緒確實排在同一工作者持有的兩把Source鎖後，原有效傷害恰一次。6案關閉真實等待原數值核心的既有admission fence，正確HP封包只在所有內部鎖外送出。原反射／數值／死亡／輸出本體保留，重用NativeLightAction既有雙方收據與成對鎖，新增核心護欄於Hub／物件／NPC狀態後取得admission；SUMMON原入口兩次身分捕捉在已有動作鎖下用相同嚴格欄位，避免新增Hub內玩家狀態monitor。兩個既有class、一個無欄位helper以外逐位元不變；凍結重建同SHA。

**夾具型別更正**：既有90案及其衍生夾具的SUMMON標籤呼叫zombie工廠，現行核心實際建立TAMED，故舊family數是標籤組數，不能據以宣稱真正SUMMON已驗收。所有舊收據／失敗試跑保留；新Frame／成對／關閉／MP回歸改用lesserElemental並逐案斷言Snapshot.kind，真正PET／SUMMON／TAMED均已重跑。真正SUMMON的兩案致死反射與父層對照均保留同一背包、歸還3藥水及2戒指、成功後才移除物件，Source169與刪除各一次。背景僅有repositoryForClassicSource／accessLevel權限唯讀查詢，玩家資料寫入0；這與deleteMe純落地worker的零Repository契約分開記錄。

90案凍結原標籤組、352案Ref／成對／範圍／Main68及73，以及24案真正型別的MP關閉／DRAINING／退役Frame在此SHA通過；3組原生classpath各33個遊戲class確認來自同一JAR。完整提交後特效／HP meter退役與跨場景排序、其餘HP寫入、AI／仇恨／毒傷、完整34材料與Source外控及最終全回歸仍開放，不同SHA不混加。整合檢查點維持3bda9d5d的1,206案；競技、記憶體、131停用、超容量馴服整筆拒絕及資料限制保留。未部署、未啟動遊戲、未寫Source／玩家SQL／WAL。[限定收據](../../tools/combat-continuation/integration-lifecycle/build/companion-reflection-progress-current.json)。


技能161受控NPC續作 `c6611ff7b009…` 同SHA **985案／已計數104,888項檢查**，屬限定範圍驗證。原場景正常／無目標Ctrl、跨角色Source鎖等待、角色／NPC世代／pose／同ID替換、缺材料／MP、STOPPED及提交後過期特效均有實際原生案例；競技真正PET／SUMMON／TAMED的敵方／隊友／自己18案保留原權限。普通WORLD真正型別與自己／他人12案，以及2案原生through=1隔牆施法與6筆非法資料列拒絕通過。Source的TYPE_PROBABILITY=1、TYPE_ATTACK=64，161 through=1可穿透；攻擊位元與Source常數一致，原資料列仍拒絕不支援的type／through組合；錯誤的早期位元試版收據保留，不混加。

沿用既有Context收集、弱主人身分、雙方收據與Source成對鎖，再依Hub／NPC／狀態／既有admission驗證；原161材料、MP、機率核心與generic特效發布保留。空目標及原NPC效果階段補入真正受控物件；只開放已審查的161無目標Ctrl，10種有效定向封包的簽名碰撞維持原解析。僅3個既有class改動、2個無欄位helper，Context與其餘JAR逐位元不變；獨立逆向差異與凍結重建同SHA。304案既有161／167封包及生命週期、352案Ref／成對／範圍／Main68及73在同SHA通過。

唯讀載入盤點為58個含材料資料列；歷史34是待補方法盤點，兩者均不是技能完成數。161的完整相消外控、NPC施法／AI消費端、重複／到期／重置與所有出生工廠，以及167和其餘材料／完整Source外控仍開放。整合檢查點仍為3bda9d5d的1,206案，不與其他SHA混加。保留競技、記憶體、131停用及超容量馴服整筆拒絕限制；未部署、未啟動遊戲、未寫Source／玩家SQL／WAL。[限定收據](../../tools/combat-continuation/integration-lifecycle/build/companion-forest-progress-current.json)。


技能161 NPC相消外控續作 `f213c87f8ee2…` 同SHA **112案／1,533項檢查**。真正PET／SUMMON／TAMED、自己／他人、正常／Ctrl與161相消免疫開／關的48案以原始技能44封包對照：父層24次未受保護的161殘留，新候選全部清除，免疫開啟者保留；原相消仍取消非免疫加速並只扣一次材料／MP。另64案凍結原NPC相消回歸通過，鎖內送包0。

只把BattleCompanion原cancelMagicEffects的一個既有呼叫接至無欄位helper，先執行原173／174處理，再依既有cancellationImmune清單清除161；不新增時鐘、註冊表、狀態欄位或執行緒。父層其餘JAR及全部原方法／欄位保留，獨立逆向稽核與凍結重建同SHA。父層c6611ff7的985案含Main68／73，與此112案分開記錄。161其餘消費端／狀態與出生矩陣、167及其餘材料／完整Source外控仍開放。整合檢查點及正式檔不動，競技／記憶體／131／容量與資料限制保留；未部署，Source／玩家SQL／WAL寫入0。[限定收據](../../tools/combat-continuation/integration-lifecycle/build/companion-forest-cancellation-progress-current.json)。


技能167 Source目標語意修正 `4627ff770917…` 同SHA **626案／已計數44,008項檢查**。直接在既有共用380_db做唯讀查詢，167為target=buff／target_to=3／ranged=-1／area=0／through=0／type=1／id=64；Source SkillsTable直讀此欄位，makeTargetList以area0走單體。舊原生卻用area=-1，28情境32次實際正常／Ctrl施法重現94次非選定角色中招；新候選28情境33次施法為0，隔牆不套狀態且保留Source空目標仍消耗一次材料／MP。兩個真正普通NPC同場另以8情境對照，舊9次施法38次非選定角色／NPC中招，新8次全部為0。

既有CombatDatabase啟動唯讀交易載入807列不可變目標資料，保留合法空目標佔位列；807是資料列數，不是技能驗收數。六個167欄位讀取器接至此快照，原資料列及建構契約area改為0；只在已完成選定、入場與扣款後的四種原效果清單套Source目標／穿透篩選，原機率、數值、既有競技規則及輸出本體保留。沒有角色／NPC註冊表、新玩家欄位、時鐘或施法時SQL；資料快照只含字串／數值，啟動觀察11次SELECT、資料寫入0。獨立精確逆向稽核及凍結重建同SHA。

**舊167夾具語意更正**：AreaMaterialProbe明確要求第三人中招；其記錄只能證明舊程式行為，不能作為目前Source單體語意驗收。舊夾具與golden完整保留，不改斷言湊綠燈。新Source原始封包對照、232案原161材料／普通NPC／生命週期及352案Ref／成對／範圍／Main68與73通過。受控NPC的167選定／狀態／攻擊及施法間隔、成對生命週期、其餘目標形狀／34材料與完整Source外控仍開放；此候選不提升整合檢查點、不與其他SHA混加。未部署、未啟動遊戲；131／容量、競技、記憶體及Source／玩家SQL／WAL資料限制保留。[限定收據](../../tools/combat-continuation/integration-lifecycle/build/wind-source-target-progress-current.json)。


受控NPC技能167續作 `f158a61a3da7…` 同SHA **795案／已計數93,242項檢查**。舊候選72情境均無法選中真正受控NPC；新候選72情境81次原始正常／Ctrl施法均可選定與施加狀態，9次原生機率未中逐次保留且各只扣一次材料／MP。真正PET／SUMMON／TAMED、WORLD／競技、自己／隊友／敵方依現有buff目標政策驗證，非選定人物狀態保持不變。

失敗傳送後主人新圖／物件原圖的32案含24次實際等待另一角色Source鎖；角色／NPC世代／pose／同ID替換／缺材料／MP與STOPPED拒絕均通過。提交後16情境17次施法驗證退役特效不外漏。9案原生beginStrike／移動時鐘與隔離載入的實際Source ActionIntervalCalculator比對，wind於既有速度計算後乘1.25、移動不變、期限邊界回復原間隔；只使用原nextActionNanos時鐘。33案原167／原始44入口驗證重複不刷新、相消免疫、反制／外控豁免、魔消、束縛、屏障、機率失敗與死亡／離場／dismiss清理。

只新增一個NPC自有延遲配置狀態欄位、兩個無持有者helper；保留原選定檢查、材料／MP／機率與輸出本體，原機率橋逐位元複製。七個原有同步重置入口接清理；所有玩家欄位、Context、攻擊權限函式及其餘JAR保留。精確逆向稽核及凍結重建同SHA。232案原材料／NPC／生命週期、352案Ref／成對／範圍／Main68及73，以及Source單體人物／普通NPC原始封包回歸通過。

目前Source L1PcInventory新增兩處物品時數轉秒防溢位檢查；新唯讀審查以全檔逆向精確還原至6583，再還原既有dark輔助儲存失敗傳播至445a，材料／角色方法未變，未改原測試與golden。Source時鐘參考的連結stub只在platform-parent隔離loader，未建Source角色、不混入原生classpath。

此為限定NPC167物理時鐘／成對生命週期驗證，不等於完整Source：NPC施法AI及其施法間隔、全部進化／贈物／復活工廠分支、其餘材料／職業／目標外控與最終同SHA全回歸仍開放。整合檢查點維持3bda9d5d／1,206案，不與其他SHA混加；保留競技、記憶體、131停用與超容量馴服整筆拒絕。未部署、未啟動遊戲，Source／玩家SQL／WAL寫入0。[限定收據](../../tools/combat-continuation/integration-lifecycle/build/companion-wind-progress-current.json)。


材料40／103受控NPC與Source單體續作 `17d49e194896…` 同SHA **1128個原生驗證案／已計數144,357項檢查**，另54案隔離執行目前Source原始CURSE_BLIND，兩者分別計數。舊候選144案中132個合法受控目標漏找；新候選PET／SUMMON／TAMED、WORLD／競技、自己／隊友／敵方及正常／Ctrl皆依限定矩陣通過。保留既有競技權限與原生點選範圍，前置凍結主目標／主人後才等待成對Source鎖。

Source目前103為ranged=0、area=0；原生3格點選上限保持，效果階段依目前Source旗標／射程／LOS篩選。修正玩家16次與普通WORLD NPC4次遠距誤生效；Source空清單仍一次扣料／MP，不消耗消魔／反魔法或喚醒玩家。玩家64案、普通NPC16案；更廣普通NPC睡眠／AI及其他target shape仍待驗。

實際失敗傳送後主人新圖／身體原圖64案含48次真正主人鎖等待；角色／生命／pose／同ID替換、缺料／MP與STOPPED皆通過，提交後32案確認退役不送舊特效。96案NPC條件、80案玩家相消別名，以原始44封包驗證共用DARKNESS40狀態：保護40才保留期限，僅保護20／103仍解除。重複施法刷新共用期限，死亡／離場清理；反魔法外控豁免、魔消、束縛、屏障、缺料及MP等限定條件通過。48案原始材料記憶體儲存故障均完整回復材料／MP／狀態／Source冷卻並立即重試，僅疊加可精確逆向的GameStateStore觀察鉤，未替換玩家／NPC實作。

精確逆向稽核及凍結重建同SHA；只改ClientSession、BattleCompanion、BattleTimedStatus三個class及新增兩個無欄位helper。無新增玩家／NPC欄位、ThreadLocal、工作池或名冊。352案Ref／成對／物理範圍／Main68的10案及Main73的8案，與232案原161材料／普通NPC／範圍生命週期回歸通過。Source致盲參考使用隔離連結替身，未建立真Source角色；浮眼mode2只核對Source原始程式，原生生產／消費端仍開放。舊103距離1應生效的golden屬舊行為，不改寫舊測試湊綠。

本輪唯讀58個含材料技能都有對應record getter；補areaRadius別名後仍5個射程／屬性欄位差異待釐清，涵蓋29／44／76／103。29的一般魔法attr與妖精屬性門檻不可混用，不直接改getter。807列快照不等於807技能接線。Source L1ItemInstance新變動僅強化值volatile宣告／註解，原始位元組反向還原至91f48a0e；材料／角色方法未變，Source唯讀閘門通過，完整原生Proc可見性另待稽核。

完整34／58材料、Source全語意、其餘目標形狀／through互動與最終全回歸仍開放。整合檢查點維持3bda9d5d／1,206案，不與其他SHA混加；保留競技、記憶體、131停用與超容量馴服整筆拒絕。未部署、未啟動遊戲，Source／玩家SQL／WAL寫入0。[限定收據](../../tools/combat-continuation/integration-lifecycle/build/blind-scene-progress-current.json)。

封存期間另有0.6.71發布流程更新正式核心為e1498871；已唯讀核對其發布紀錄及deployment雜湊，正式JAR不含本候選兩個新helper。本工作未部署／還原正式檔；舊75da保護值保存在前一收據及差異稽核，新current-state保護錨點依獨立發布證據記錄e149。未來整合前需與0.6.70／71功能隔離合併及同SHA全回歸。[唯讀核對](../../tools/combat-continuation/integration-lifecycle/build/blind-external-formal-release-audit-current.json)。


受控NPC材料173／174續作 `3e8532141119…` 同SHA **1212案／已計數148,129項檢查**。原先視為預檢的CombatNpcDebuffs.negative其實在共用材料入口前直接替換背包及扣MP；原始封包重現16次帳本不一致與8次Light共用時間窗旁路，新候選皆歸0。先凍結真正主目標與主人、保留收據，再取成對Source鎖；NPC與權限唯讀查詢放Hub外，Hub→pet→timed→既有admission內重驗原場景／角色／生命／pose。

144情境159次真PET／SUMMON／TAMED正常／Ctrl施法，15次原生機率未中逐次保留，合法嘗試均一次扣料／MP且帳本一致，競技自己／隊友／敵方門檻保持。64個跨圖分離情境65次施法、49次真正主人鎖等待，及32個提交後送包情境36次施法通過；角色／生命／pose／同ID替換、缺料／MP、STOPPED與延後輸出界線保留。48次真實材料儲存故障與立即原始重試通過；重試的正常機率未中另記，不要求隨機命中湊綠。

原304案玩家／普通WORLD NPC／玩家材料故障、原玩家封包golden逐位元相同；352案Ref／成對／物理範圍／Main68的10案及Main73的8案，以及232案原161材料／普通NPC／範圍生命週期回歸通過。凍結重建同SHA，獨立逆向恢復所有原方法／欄位與數值／效果本體。只改CombatNpcDebuffs及NativeTargetNegativeMaterialAction兩class、新增一個無欄位helper；無新增玩家／NPC欄位、ThreadLocal、工作池、名冊或假NPC／物理投影。

限定修補不代表完整173／174或34／58材料與Source覆蓋；其他外控／狀態／AI／工廠、27／36／41／44路徑、與另發布0.6.71功能隔離整合及最終同SHA全回歸仍開放。整合檢查點仍為3bda9d5d／1,206案，不與其他SHA混加。保留競技、記憶體生命、131停用與超容量馴服整筆拒絕；本工作未部署、未啟動遊戲，Source／玩家SQL／WAL寫入0。[限定收據](../../tools/combat-continuation/integration-lifecycle/build/controlled-negative-material-progress-current.json)。


受控BATTLE NPC技能87續作 `7a982b611cd3…` 同SHA **1671案／已計數156,758項檢查**，其中460案為本輪87原始封包、實際持有者鎖及記憶體外控驗證。先凍結真正NPC與主人，再取成對Source鎖；NPC唯讀查詢放Hub外，Hub→pet→timed→既有admission內重驗。原先Light共用時間窗仍被87繞過的2案歸0。三真型別／自己隊友敵方／一般Ctrl、96次實際等待，以及生命／同ID換體／角色／斷線／資源／距離／STOPPED拒絕通過。

延後發布原有24案舊攻擊封包外洩與12案合法死亡impact被擋均歸0；PET保留屍體與SUMMON／TAMED退役依原生命週期，死亡證據只活在本次Context。96案只在fixture記憶體覆寫87的HP、MP、材料、傷害、骰數與reuse；正數材料一次扣除且帳本一致，缺料／正ID零量／HP邊界／MP不足／共用時間窗皆整筆拒絕，無資料庫設定寫入。原173／174、範圍161、成對／物理／Main68及Main73回歸循序執行；重建同SHA且獨立逆向保留原數值、傷害、機率、暈眩與效果本體。

限定入口完成不代表完整87、34／58材料或807技能Source語意完成；PC／WORLD87、其他外控／狀態／AI／工廠與最終整合全回歸仍開放。舊consumeMaterial只剩靜態相消後備定位；現行三筆靜態Quest配置已唯讀確認會先走新路徑，其他Source外控形狀不算通過。整合檢查點仍3bda9d5d，不與不同SHA混加；正式0.6.71保持原狀，保留競技、記憶體生命與資料安全限制，未部署、未啟動遊戲，Source／玩家SQL／WAL寫入0。[限定收據](../../tools/combat-continuation/integration-lifecycle/build/shock-companion-progress-current.json)。


PC技能87 BATTLE／WORLD續作 `f460b2b6f310…` 同SHA **2421案／已計數166,960項檢查**；本次352案驗證原始87封包、跨角色目標、一般／Ctrl、80次真正成對鎖等待、角色／位置／斷線／資源／STOPPED邊界及64案記憶體外控。Light共用時間窗原4案繞過歸0；准入仍在成對Source鎖後、Hub→目標timed→既有admission內重驗，冷Source查詢留Hub外。原傷害、機率、暈眩與效果本體以獨立ASM逆向核對。

真正HP事件提供死亡證據，既有HP completion完成後才放行本次捕捉輸出；修正非同步死亡使合法impact偶發消失的順序問題，舊角色／舊位置／舊生命的STUN與特效不放行。實際61／75復活測出死亡HP佇列未重新建立的缺口，已在原成功回血位置退役舊佇列並轉換既有屍體epoch，不改所選角色binding。8案實際87致死→另一角色61／75原生問答復活→同角色第二次原生致死通過，舊死亡權限不復用；另368案復活材料、生命週期及native／direct問答通過，既有封包golden相符；舊生命週期fixture把目前兩張成對收據誤寫成一張，已在父7a重現，僅以可整檔逆向的WORLD及BATTLE目標等待2張／BATTLE施法者等待1張期望與診斷資訊修正；STOPPED時只有原先雙方已准入的WORLD續作可完成，未取得目標收據的BATTLE施法者等待整筆拒絕，其餘斷言保留。另4案在87已扣款而HP工作仍等待Source鎖時強制停止HP協調器，重現了通用PC HP先寫入再發現完成收據已無法准入的競爭。已改在原角色鎖與life.admission內、HP變更前取得既有完成收據，沿原完成流程轉交，所有原unlock／異常路徑均釋放；舊HP／impact不提交、新施法拒絕且全部idle。4案真實HP完成佇列故障保留原始例外、單次釋放收據、已提交HP不重播，下一筆事件仍完成。26案原signed／零HP／FIFO／完成尾端故障與已准入Hub關閉，加2案實際記憶體Main的正常／中斷HP完成交接與資源關閉回歸通過；61／75在目標退役後仍保留施法者自己已提交的MP更新，舊目標效果仍拒絕。Main68／73、成對／物理及範圍回歸均循序跑在同SHA。

這是限定入口與生命週期修補，完整87 Source公式／狀態／AI／工廠、其他復活／回血根、所有34／58材料及807技能Source語意、已准入後完整關閉／背壓競爭與最終整合全回歸仍開放。整合檢查點仍3bda9d5d，正式0.6.71保持原狀；保留競技、131停用、馴服容量整筆拒絕與記憶體限制。未部署、未啟動遊戲，Source／玩家SQL／WAL寫入0。[限定收據](../../tools/combat-continuation/integration-lifecycle/build/shock-player-progress-current.json)。


PC87接受後狀態續作 `4828aad9f694…` 同SHA **2489個原生案／已計數166,271項檢查**、25組循序回歸；另400案直接編譯目前Source提交計畫與SHOCK_STUN原文的記憶體片段參考，獨立計數。原始87封包在兩個HP工作均等待時，64案提早暈眩／建立特效NPC由33案歸0，並觀察到9次合法接受後暈眩；致死、換角、clear、移位、STOPPED及真正完成佇列故障均不套用新狀態或建立新特效。原機率判斷保留於生產端，原時間抽選、狀態、特效工廠與封包指令移入既有HP完成流程，僅在accepted且非致死時執行；角色、場景、生命與精確暈眩期限身分控制輸出。沒有新工作者、TLS、靜態角色表或SQL路徑。可重建相同JAR SHA，獨立逆向核對所有原方法與移植指令。發布64案以可整檔逆向的新fixture改驗等待HP時無狀態、完成後才觀察；正向暈眩證據由獨立64案提供，舊fixture／證據未改。原成對／Ctrl、受控三型別、材料故障、範圍、368復活、26一般HP及Main68／73與兩案實際記憶體Main正常／中斷關閉回歸同SHA通過。

限定PC87時機修補，完整87外控／NPC路徑、提交後clear／重施／IO與關閉的完整交錯、其餘27／36／41／44材料路徑、34／58／807全面Source語意及最終整合全回歸仍開放。整合點3bda9d5d、正式0.6.71及固定輸入未變；保留競技、131停用、馴服超容量整筆拒絕與記憶體限制。未部署或啟動正式遊戲；Source／玩家SQL／WAL寫入0。[限定收據](../../tools/combat-continuation/integration-lifecycle/build/shock-accepted-progress-current.json)。


材料44 WORLD靜態原NPC重生續作 `7129e48e157a…` 同SHA **1,100個原生案／已計數54,066項檢查**；另18案Main68／73重播不重複計數，294,917個移動核心案與24案Source片段獨立列示。原WIP e1的「原NPC永久退休」缺口已在三個目前Source固定出生NPC71092／80013／81209接齊；不是其餘材料或完整Source均已完成。沿既有唯讀啟動交易讀取1,181列出生資料及時間設定，原Hub排程器負責零／非零延遲；重生時更換真正Scene.Actor世代，保留原生靜態物件ID槽。舊施法、舊回呼及等待原生socket monitor的封包不可套入新世代。新增168案涵蓋正常／Ctrl、材料／MP、建構／排程故障、角色／場景與ID重用、私人副本隔離、空場／STOPPED／close及實際零延遲排程。包含4次真正舊目標鎖等待、4次原生送包鎖等待及8案關閉後直接入口；原相消120＋108＋60、移動8＋24、HP26＋2、Ref／成對／範圍／Main352及範圍232案循序通過。私有108沿用已記錄的兩處斷言修正，未改原golden。

限定驗證已凍結，可重建相同JAR，獨立逆向核對十個修改／五個新增封存項目與原方法；沒有新增玩家欄位、工作者、TLS、靜態角色表或玩家持久化。Source刪除完成尾段與重生計算的24案是隔離原文片段，不能代替完整Source生命週期。隨機／限時出生、其他建構種類／腳本／AI、其餘材料及全部Source外控仍開放。正式0.6.71、整合3bda及歷史PC87限定收據保持原狀；後續須隔離合併已發布功能，完成最終同SHA全回歸後再交集中人工實測，人工通過後才發布及建立新聯合基準。競技、131停用、馴服超容量整筆拒絕、無死亡掉裝／經驗懲罰與記憶體／資料安全限制保留。未部署，本工作未啟動正式遊戲、未寫Source／玩家SQL／WAL；觀察到的外部Java程序未操作。[限定收據](../../tools/combat-continuation/integration-lifecycle/build/static-respawn-progress-current.json)。


PC50正傷害接受後狀態續作 `f4d1e83ab9a1…` 同SHA **1,074原生案／已計數51,174項檢查**，另320案目前Source提交計畫／消魔lease／冰凍特效原文片段獨立計數。新增200案含48次實際成對鎖等待、32次真正原生socket monitor等待、正常／Ctrl及友敵權限；16案原生冰矛提早消魔16／冰凍13次降為0，部分修正版另重現8次舊冰凍封包，最終歸0。另8案在HP等待期間把記憶體技能列期限7秒重載為1秒，錯讀8→0；狀態、特效及原麻痺封包統一採同一份已捕捉期限。原HP26＋Main2、PC87接受／發布64＋64、原生致死復活8、Ref／成對／範圍／Main352、範圍232及WORLD NPC數值126案與額外原斷言通過。NPC舊fixture在父7129同樣因舊工作池失敗；新版本可整檔逆向，僅改類名、四個Gate接點、既有Hub工作池gate與原故障注入的worker識別，不改數值／效果斷言。

只驗收PC50正傷害的接受後時機與生命週期；PC80／194已觀察到的同類缺口、奪命之雷及其他攻擊消魔消費端、零／負傷與Source零期限／原計時器／效果順序等完整語意仍開放。八個既有／四個新增class與所有原方法、原冰凍狀態／特效／封包指令已獨立比對，可重建相同JAR；未新增玩家欄位、worker、TLS、靜態角色表或玩家持久化。最後原生frame只讀既有生命／角色與volatile冰凍期限，不取得Hub／狀態鎖。其餘34材料矩陣、Source外控與0.6.71隔離整合／最終全回歸仍待完成；自動化全通過後才交集中人工實測，人工通過後才發布及建立聯合基準。競技、記憶體、131停用、容量整筆拒絕與資料安全限制保留；正式檔、Source／玩家SQL／WAL未改，未部署。[限定收據](../../tools/combat-continuation/integration-lifecycle/build/freeze-duration-progress-current.json)。


PC80正傷害接受後與動畫交接限定續作：候選`619ed21e286f…`同SHA1,202原生案／已計數56,383項檢查。新增128案包含28次原生範圍鎖取得等待、48案120筆獨立PC傷害事件、4案固定期限、16次接受後狀態及12次施法輸出的原生送包鎖等待；HP等待前消魔8／冰凍7→0，舊角色或STOPPED後封包0。等待期間施法者移位扣MP4→0，動畫前PC提早HP4→0；WORLD NPC原126個數值施法與原動畫閘門失敗已修復，既有50／87、HP Main正常及中斷、352案Ref／成對／Main68／73與232範圍回歸通過。共用同一角色快照，每個PC有獨立既有HP計畫；原動畫輸出後以既有成對鎖重驗再交付，NPC亦保留原CombatWorldFreeze計畫／動畫順序；保留原公式、機率及效果本體，無新玩家欄位／工作池／TLS／actor名冊。236純封包與320獨立Source片段另計。這是限定正傷害／時序／生命週期證據，194、零／負傷、完整Source計時／工廠／效果順序及其他材料仍開放；正式0.6.71及資料安全界線未變。收據：`tools/combat-continuation/integration-lifecycle/build/area-freeze-accepted-progress-current.json`。


194目前Source選靶與接受後狀態限定續作：`5945812d27e8…`同SHA1,434原生案／已計數60,470項檢查。實際唯讀列為target attack／target_to3／ranged10／area0／through0；原L1SkillUse以_calcType1～4判斷17／194分支，32個原片段194組合均為單體，另32個17假設area0及32個不可能calcType對照不列原生數。8次正常／Ctrl、BATTLE／WORLD第二名玩家誤打→0；16案確認10格可用與11格拒絕，另8案6格通過。沿用原單體、材料／MP與HP接受計畫，48次成對、32次最終原生送包等待、致死／退役／清除／新erase世代／STOPPED／期限重載通過；原NPC126次數值施法保留，唯兩個194第二目標預期依Source修正，原D100測試設定、零傷害／raw0／長期限仍通過。只改三個既有class並新增一個無狀態helper，原方法獨立逆向及相同位元重建通過。PC0／負傷、完整Source計時／效果／其他消費端、17實際area3與其餘材料及正式0.6.71隔離整合仍開放；未部署、無玩家SQL或WAL寫入。收據：`tools/combat-continuation/integration-lifecycle/build/freeze-source-target-progress-current.json`。


29／44／76 Source視野射程限定續作：`26552f69b4ef…`同SHA 1,842原生案／已計數65,135項檢查，其中264案為PC新射程／正常Ctrl／跨角色等待，另144案保留既有NPC准入：29的BATTLE／WORLD與44／76的WORLD三種真正NPC；24次PC及40次NPC主人鎖等待後退役均無扣料、扣MP或傷害。BATTLE44／76受控NPC漏找及完整Source目標失敗資格仍未完成，未將該路徑列入通過。Source原片段45,387組／151,292檢查另計，確認−1是含有方向差異的19格視野邊界；29競技場既有視線規則保留，44／76未新增視線規則。保留PC50／80／194、NPC、HP、87、範圍成對與Main關閉回歸；僅三個getter與原消費端14處射程投影，四個既有class及一個無狀態helper，獨立逆向與相同位元重建通過。完整材料技能、Source語意及正式整合仍開放，未部署，無玩家SQL／WAL寫入。收據：`tools/combat-continuation/integration-lifecycle/build/source-visibility-range-progress-current.json`。


Source受控NPC餘項核對（非新增原生通過）：原始isTargetFailure、TYPE_NORMAL分支與applySideEffectsAfterNormalCast原片段，實際8個技能type、Source NPC繼承關係，以記憶體替身完成1,472組判定及同組施法呼叫順序／共5,889檢查；與1,842個原生案分開計數。Source PET／SUMMON不繼承Monster，TAMED建立Summon；18／29／39／76／152／153／167有必敗目標條件，而44為對照。已准入的正常施法先扣材料／動畫，runSkill移除失敗目標後仍走MP與後續通知，不能簡化為免費拒絕。BATTLE44／76受控NPC漏找的兩份實際失敗收據保留，相關路由／成對提交及Source失敗效果消費端仍未完成。既有NPC射程回歸不是完整Source資格驗收。未部署、無Source／玩家SQL或WAL寫入。索引：`tools/combat-continuation/integration-lifecycle/build/source-controlled-failure-analysis-current.json`。


BATTLE44受控NPC限定續作：`246ef565a3fd…`同SHA 2,442原生案／已計數72,411項檢查，其中600新案（420提交、84提交後退役、72原生同步frame真等待、24記憶體材料失敗／立即重試），其餘1,842案重播含PC50／80／194、NPC、87、範圍成對與Main68／73。三個真正型別、正常／Ctrl、競技敵我與自身，保留原NPC數值／效果本體；補固定成對鎖前角色／生命／座標捕捉、STOPPED與最終原生Frame嚴格角色／Actor重驗。首輪移位／STOPPED仍扣35MP＋2材料、提交後移位舊特效、Frame等待後主人換角舊位元均有失敗收據，已修正。原NPC相消明確重設速度，即使43狀態列受免疫保護；288個原片段參考另計，未宣稱剩餘43期限與其他Source消費端完成。只改四個既有class，新增每次施法的Context Object欄位及三個helper class，無玩家欄位／新工作池／TLS／Actor名冊，沿用原輸出批次並於完成清除；所有舊方法與NPC複製本體獨立逆向、相同位元重建、7個class無初始化驗證通過。測試換角曾反向取鎖，保留堆疊並僅終止該所屬測試，改為tryLock並在不可合法改動施法者時測主人退役，仍有真正施法者角色／位置等待。BATTLE76、完整Source失敗目標／機率／狀態／AI／其他材料及正式整合仍開放，未部署，Source／玩家SQL及WAL寫入0。收據：`tools/combat-continuation/integration-lifecycle/build/battle-controlled-cancellation-progress-current.json`。


技能76限定生命週期續作：`de15938d60d2…`同SHA 3,750原生案／已計數224,055檢查，其中1,308新案。沿用既有固定順序角色鎖、範圍收據、材料交易與輸出批次；補NPC主目標辨識、NPC失敗前保留消魔／睡眠／魔法屏障、正常／Ctrl、392次實際取鎖與192次原生Frame等待、48次材料故障完整回復及立即重試。Ctrl指向友方NPC曾誤放行其主人作為玩家範圍目標，已改用既有指定物件判定。玩家主目標STOPPED等待後仍生效的缺口已補；原生3750案含既有2442案、Main68／73與144個經完整檔案逆向的66／76跨圖案例。只變三個既有class並新增三個helper class、每次施法一個Context Object欄位，無新玩家欄位／工作池／TLS／Actor名冊；10個class無初始化檢查、獨立原方法逆向、相同位元重建與最終送包不取角色／Hub／狀態鎖稽核通過。**這不是完整Source76完成**：後續36個完整原片段管線參考（另計，無Hub／SQL）證實範圍buff保留施法者清單項，NPC單獨效果失敗仍有主目標特效；自身PC／寵物／召喚物主目標則跳過整段效果計算。目前候選的顯示與自身整段計算仍待修，先前無NPC特效斷言須另存有Source依據的版本。完整34材料、其他Source失敗目標／控制／AI及正式整合仍開放；未部署，Source／玩家SQL及WAL寫入0。收據：`tools/combat-continuation/integration-lifecycle/build/source-mass-slow-lifecycle-progress-current.json`。


技能76 Source主目標／顯示管線續作：`b2ebe000cd51…`同SHA 3,814原生案／已計數225,295檢查。36個原片段參考直接沿用makeTargetList、主目標／逐目標isTargetCalc、isTargetFailure、TYPE_NORMAL與buff顯示片段（機率結果與isTarget前段明確為記憶體stub，另計0個Hub／SQL），證實施法者isCalc(false)仍留在清單，因此失敗NPC仍有一次主目標特效；自身PC／寵物／召喚物主目標跳過整段效果，但照付已准入消耗。64個真正自身主目標前後比對，16未扣費、40顯示、24狀態與24緩速誤中全部歸0；全數角色／成對鎖、192原生Frame等待、材料故障及既有Main68／73／範圍／凍結／相消重播通過。只更動PC與既有helper兩個class，四個無狀態方法，無新欄位／工作池／TLS／Actor名冊；原數值、消耗、准入、最終Frame及清理方法獨立逆向不變，相同位元重建、10個class無初始化及非阻塞Frame稽核通過。舊無NPC特效期待依36案Source證據另存完整檔案逆向版本，66與其餘成本／生命／狀態斷言保留。

新增使用者回報已納入同一工作：共用skills實際名稱確認通暢氣脈術為26（CLAN），不是42；需檢查全部指定技能的傷害／增益／恢復／負面／複合分類，自身／友方／敵方與特殊目標，正常／Ctrl、游標、object ID、准入、效果、一次成本及前景多視窗／返回場景。807列靜態清單透過既有唯讀啟動交易完整盤點，180列有原生模型定位；其餘及所有逐技能證據格均標待補，不作全面覆蓋宣告。尚無新現場封包，未判定客戶端或伺服器原因。新規則為合法自身／友方輔助不得要求Ctrl、敵方玩家需明確強制意圖；既有3,814案使用先前敵我政策，不能驗收此新規則。仍保留0.6.71與競技／記憶體／來源唯讀限制，未部署／未建新基準。收據：`tools/combat-continuation/integration-lifecycle/build/source-mass-slow-pipeline-progress-current.json`及`all-targeted-skill-inventory-current.json`。


全部指定技能／Ctrl新增診斷：正式0.6.71與b2ebe000cd51候選各16個合成26／42自施封包通過，覆蓋正常／Ctrl、BATTLE／WORLD及兩種材料應用模式；實際自身object ID、效果與DEX／STR狀態圖示、一次MP／HP及冷卻均確認。兩個不同SHA不併入3814原生管線數。只屬伺服器合成封包基線，未操作現場游標、未取得新現場封包，不能認定客戶端或伺服器原因，也未宣告使用者回報已修復；全部807技能與其他目標／敵方新Ctrl規則仍待補。正式litoddraw.dll唯讀雜湊仍為e47bd6c6ec99…；序列化後才附Ctrl意圖的程式只作定位，未當作游標根因。收據：`tools/combat-continuation/integration-lifecycle/build/all-targeted-skill-self-server-diagnostic-current.json`。


Source167／全部指定技能目標續作：`df82862d3751…`同SHA1,712原生案／已計數56,205檢查，72案重現66個受控NPC狀態消耗差異並修正；保留原一次扣款，Source類型失敗於機率／消魔／睡眠／屏障前返回。補PC與受控主目標的Source畫面範圍、392真實成對等待、192原生同步Frame等待及48材料故障／立即重試；Main68／73與Source76自身及送包回歸通過。只改3個既有class及新增3個bounded cast class，原數值／扣費／其他技能方法完整逆向，相同位元重建、13個class無初始化及非阻塞Frame稽核通過。自然NPC、其餘Source控制／目標／普通建構與計時語意仍待補。

另一份`323d708e69f7…`候選只更動PC／ForcedCombat及一個無狀態helper；在81個真實物件／攻擊解析或已解碼參數端點加入競技選定PC的明確意圖讀取，只讀原request-scoped Ctrl且不建立新TLS／actor名冊。當前房間模式／隊伍規則辨識敵方；不是以另一玩家或同來源當敵我。正常自身／友方輔助沿用原Source類型與血盟／隊伍條件，Ctrl仍不能繞過死亡／旁觀／距離／LOS／免疫等原門檻，NPC／ArenaBot／寵召及範圍次目標保留原規則。同SHA1,096案／已計數12,955檢查（7技能1/4/26/42/50/76/167的76個真實封包與效果案、96PC邊界／36PCFrame／696NPC邊界／144NPCFrame／48材料故障）；56個玩家成對等待、180個原生Frame等待通過。獨立全archive／schema／原方法逆向、相同位元重建及16class無初始化通過。WORLD敵我／中立關係、其他技能模式及實際游標尚未驗收，不能把此SHA當完整新政策或把舊1712／3814案相加。

807列另補Source符號185列／註冊執行器48列／180列原生解析候選、234靜態呼叫路徑；明確標示靜態過度近似、反射與ID條件未證。僅7個已測技能加入限定伺服器證據。通暢氣脈術26仍無實際游標／現場封包證據，不宣稱使用者回報已修復。正式0.6.71JAR／特殊載入DLL／基準未動，Source與玩家SQL／WAL寫入0；沒有部署或新基準。收據：`source-wind-target-progress-current.json`、`selected-player-intent-progress-current.json`及`all-targeted-source-paths-current.json`。


使用者場景澄清：「lito只有競技場沒有WORLD 地圖」。以此作現行產品驗收範圍；自身／友方／敵方、正常／Ctrl、跨來源角色、範圍主次目標、NPC／ArenaBot／寵召、前景多視窗及回大廳均以競技場路徑驗證。WORLD只保留既有後端行為及補充回歸，不列新產品完成條件；前面歷史收據的WORLD待驗文字不代表仍要求加入WORLD產品。未部署／未開遊戲UI／未改正式DLL或資料庫。


29／152／153目標與生命週期續作：`609456ce5a74…`同SHA3,636原生案例／已計數80,229檢查。原216受控狀態案重現72個准入與138個狀態差異；按Source pet/summon非Monster的確定失敗，已在保留一次扣費後跳過原RNG／消魔／睡眠／屏障消耗，差異歸零。153原selectionRange固定10與Source−1不符，36個畫面內遠距准入差異重現後歸零，畫面外拒絕108案保留0扣費／0冷卻。29／152／153明確自身目標被原filter排除的12差異已重現並以只讀、限定競技／三技能helper修正；保留原kind／死亡／旁觀／LOS／距離／免疫及PET_RACE限制，未擴大Ctrl權限。

跨角色等待時技能29的目標位置變更曾使舊流程扣17MP／1材料，已保留失敗紀錄；現在PC／受控NPC主目標於既有排序鎖邊界前捕捉角色／場景／位置／生命週期，144PC邊界案／84真實等待通過。36PC原生Frame案例包含28個實際等待；153原機率失敗不產生觀察者封包的8案另計，未假造封包或改RNG。另1044NPC邊界／504等待、144NPCFrame（僅29／152；153受控類型確定失敗無觀察者封包，未虛構Frame）及72材料故障／立即重試均通過。保留7技能76個玩家Ctrl案、167六組1092案、76自身64／NPCFrame144案、成對288／物理範圍26／Ref20／Main68十案與Main73八案；各項都直接選此SHA，不借祖先比較包或混加舊SHA。完整archive／schema／原方法或整檔inverse、相同位元重建及最終Frame無blocking呼叫稽核通過。

通暢氣脈術26的歷史PE靜態分派與原始x86 callback另有24參考案／85檢查（12自身正常／Ctrl，18精確7-byte明文封包），native Hub案0；只證明該callback參考路徑，不涵蓋上游游標／前景／正式DLL程序／現場加密送包，不宣稱使用者症狀已修復。807列靜態定位補入限定10個技能伺服器及26／42／68離線客戶端證據，並非807或原34材料技能完整覆蓋。產品只有競技場，WORLD只留補充後端回歸。其餘材料、Source18／39等目標／範圍／機率／普通NPC建構AI計時、外控顯示資源與實際消費者、正式0.6.71整合及集中實機驗收仍待完成。正式JAR／特殊載入DLL／固定輸入／基準／玩家與Source SQL／WAL未改；未部署或啟動遊戲UI。收據：`source-negative-progress-current.json`。


2026-10-24：`a2668390bcf8…` 同SHA640原生案例／7,830檢查。17以Source calcType分支與World Chebyshev半徑原文核對，修正area3幾何（128案，66差異歸0）；56扣費前捕捉案含48真實等待，48個最終Frame案保留一次已准入費用。另將17既有accepted HP event的原defense清單加有界marker，保留原HP提交／worker權威，在最終原生Frame重驗角色、位置、生命與STOPPED；重現的舊角色HP條與過晚捕捉life問題已修正。有效次目標自己的已提交HP條不因另一NPC主目標移動或清狀態而被誤刪。148／149於原FireWeapon／WindShot body加限定receiver端點，把實際選定PC的狀態、效果、成功訊息與記憶體狀態回存綁定到原目標；保留原施法者屬性／MP／冷卻、Source變化技能LOS例外、無封包改目標。216案的32偏差歸0，100案含84成對等待，72最終Frame等待通過；163原自施12案與Main73八案通過。每階段全archive／原class或原方法inverse與同位元重建分開保存；父SHA3,636案未加到本SHA計數。

使用者確認已將裝備靜態表欄位中文化；既有英文字段讀取失敗保留原始紀錄。本候選只改armor Proc既有啟動SELECT及NativeDeItemTemplates／Definitions Owner白名單內的prepare端點，按凍結21表899欄對照投影回原canonical標籤，保留原值／順序／可選缺欄行為、18查詢白名單、readonly／repeatable-read／rollback與statement/resultset/connection關閉檢查；未增加SQL連線或寫入。Owner啟動實載與640原生案通過；其他帳號／裝備查詢與正式0.6.71隔離整合仍需審核。Source來源路徑若被外部清理，凍結inputs仍是重建依據。

材料靜態掃描現在僅剩useWorldRoiCancellation的一個舊consumeMaterial，沒有競技舊材料呼叫；這不是完整34技能執行期或其他Source消費者完成宣告。HPBAR／傷害數字／奇古獸16鍵的設定reader另建`8066b87fb63b…`，只讀設定與純plan，還沒安裝顯示consumer。完整Source18／39／133等、其他競技模式／來源角色／自然NPC及17以外accepted HP最終輸出、全部外控／資源receiver／最終同SHA全回歸與正式0.6.71功能整合仍開放。26現場Ctrl自施症狀仍未宣稱修好；全部補齊後集中實機游標／前景多視窗／回大廳驗收。未部署、未開遊戲UI、未建新基準、未寫Source／玩家SQL或WAL。收據`source-area-weapon-progress-current.json`。


2026-10-24：`89e2b6877c62…` 同SHA756原生案／9,280檢查，收據`source-kiringku-progress-current.json`。632個既有17／148／149／163案例與Main73八案保留；新增奇古獸PC的36呈現、44最終接收端生命週期及36成對准入案例（32真實等待、12施法者退役、0目標後備）。Source的開關與SPR只決定命中後攻擊者自身特效，不改傷害、MP或背包；關閉／0／負SPR、自訂SPR、非奇古獸、免疫、死亡與距離分支通過。舊候選24特效語意差異、32送包後延遲捕捉新角色的HP准入差異均重現並歸0。新准入在原攻擊開始捕捉實際PC身份／位置／life，對原毒、反彈耐久、HP與娃娃呼叫使用既有排序成對鎖及收據，沒有新PC欄位、TLS、worker、actor registry或SQL權威；全部原方法inverse與同位元重建分開保存。旁觀接收者單獨退役時仍保留獨立合法HP提交。此範圍不代表NPC奇古獸或全部物理入口／accepted HP最終Frame已完成。

Source17回歸有一輪停在舊測試限定「request執行緒必須卡在送包鎖」；版本化fixture現在核對實際receiver monitor identity及目前fixture鎖owner，接受既有accepted-HP publisher承接相同送包等待；所有退役與封包期待不變，48案通過。其他失敗場次保留。4800個Source純顯示計畫與16鍵reader另列，HPBAR／傷害數字的原生消費、延遲／drop-only派送及實際資源receiver仍未完成。

使用者已完成裝備欄位中文化；另盤點出Legacy角色背包／防具火抗／獎勵防具／負重仍含英文欄位。隔離`856e1124c8ed…`僅改5條既有SELECT字串，以6個armor／weapon子查詢投影回舊canonical欄名，保留原角色表條件、concat／format參數、?綁定、交易與資源關閉路徑；沒有新增class／連線／查詢或寫入權威。40個自有SQLite記憶體schema前後比較／1,200檢查通過，重現40個舊英文欄名失敗；兩個修改類別以JVM不初始化驗證，完整archive／schema／逐原方法inverse及同位元重建通過。這不是MySQL／JDBC metadata或實際Source角色重載資格化，也未混入756原生案。其餘材料與18／39／133等完整Source、其他競技模式／自然NPC、全部外控、正式0.6.71功能隔離整合及集中實機仍開放；26現場Ctrl症狀未宣稱修好。未部署、未開遊戲UI、未建新基準、未寫Source／玩家SQL或WAL。


2026-10-24 程式先行續作：`54583f1b9a0e…`只完成編譯／JVM不初始化類別檢查、凍結同位元重建與原方法inverse，新增遊戲執行案為0；先前89e2b687的756案／9,280檢查保留原SHA界線，沒有移算到本候選。Source18／39／133取得原始目標與life／pose後，沿用既有全玩家排序鎖、材料、收據及鎖外輸出；18／39受控NPC在一次扣費後走Source類型必敗，不抽機率或消耗目標消魔／睡眠。133讀Source負射程的螢幕範圍與buff自施規則；受控NPC成功時依Source ELEMENTAL_FALL_DOWN只保存技能期限，不誤套僅PC／Monster分支的減抗，保留反制、消魔、睡眠、材料與PET_RACE限制。期限只有一個actor-owned記錄，沿用取消及既有到期輪詢，沒有新增timer／TLS。

HPBAR限定任務／限時怪物，單播攻擊者、1024上限弱觀察者百分比去重、死亡0xFF隱藏與清空；玩家HUD及ArenaBot原流程不受HPBAR總開關誤擋。PC物理／魔法數字、miss／critical、dmg_spr記憶體偏好走既有HP ticket／event／accepted tail；256個有界延後顯示工作依既有lifetime worker／timer管理，拒絕或關閉只丟視覺。怪物兩個物理與18個明確魔法生產端取得原生HP提交收據；物理NPC身分在第一個輸出前固定，奇古獸SPR與HP准入均核對該生命／位置／房間世代。顯示失敗不撤銷HP或中止後續戰鬥效果。受控NPC數字／奇古獸、更廣生產端、自然NPC／AI與完整Source矩陣仍開放。

中文裝備欄位已在實際MySQL透過既有原生唯讀讀取器完成33案／94檢查：兩個來源、6次靜態防具與6次空背包重載，24連線／36 statement／36 ResultSet全部關閉，沒有寫入／額外權威嘗試。首次實庫讀取找到來源庫沒有lito_item_origin；候選僅讓readOnly Source使用原有零origin分支，可寫模式保持，沒有建表或遷移。該SQL資格化在b85b71d8，最新程式保留20個相關class entries逐位元相同，但不是最新SHA重跑，也未驗真實裝備角色；使用者要求先更新程式、後續再驗證。完整材料／Source、正式0.6.71隔離組合與同SHA全回歸仍須完成，最後才集中實機游標／前景／多視窗／回大廳驗收。26不按Ctrl回報仍未宣告修復。未部署，未改Source／玩家SQL或WAL。收據：`source-program-update-progress-current.json`。


2026-10-24 受控NPC程式續作：`0ed154021887…`加入PET／SUMMON／TAMED實際主人、物件／bag／生命／位置／placement與master證明。8個PC入口加87入鎖前入口，覆蓋物理、一般魔法、治癒轉傷害、閃電、範圍／中心範圍與單體／範圍凍結；既有6個直接與2個精確反射HP呼叫沿用原傷害、材料、MP／HP、權限與反制。主目標在成本前檢查，HP提交再次核對；未捕捉的受控目標不可在輸出後重新取得權威。原父Context與scope保留／還原，子Context只繼承本次暫態動作。

受控奇古獸依Source命中、開關／SPR與觀察者螢幕檢查；miss／critical及數字沿用既有planner，死亡只在原HP接受後固定該次post-life。既有256有界顯示工作／關閉流程保留，延後證明不持有Context或主人收據，沒有新worker／TLS／全域名冊。新最終Frame guard只讀既有角色與volatile狀態，不在receiver內拿Hub／物件鎖；已提交箭矢及HP／MP確認仍可發布，過期目標特效丟棄。受控血條原流程保留，不誤套Monster-only HPBAR開關。

本候選僅程式更新：17個實際類別JVM不初始化檢查、凍結輸入同位元重建、archive／欄位／每個原方法inverse與最終guard靜態稽核通過，新增遊戲案0、資料庫連線0。舊SHA的756案／9,280檢查與b85唯讀MySQL證據沒有移算；20個SQL消費class及原lifetime／Source顯示工作／外控保留逐位元相同。正常／Ctrl、跨角色等待、範圍主次、死復活、提交後IO／Frame、Main關閉及真實角色／游標驗收依使用者要求後續集中處理。魔杖、反射／反饋、毒／cube／doll／週期、NPC工廠／AI與完整材料／Source仍開放。未部署，未改Source／玩家SQL／schema／WAL。收據：`source-program-update-progress-current.json`。


2026-10-24 raw生產端程式續作：`30c3754cf462…`固定魔杖、PC／受控／任務限時怪物致命身軀與疼痛反饋、反擊屏障／魔法鏡、NPC接受後反饋及PC毒傷時鐘，共13入口。角色／生命／位置在原排序成對等待前捕捉，毒傷保留同地圖一般移動；真正PC／施毒者優先作為成對對象，不以同數字ID覆蓋；接受後反饋沿用原事件角色與可用施術生命證明，並拒絕observer。借用既有pair／材料／收據／clock origin，沒有新TLS或工作池。PC raw身分隨原CalcTicket／Event攜帶，證明不持有Context／poison scope／收據；在drain原vital-lock扣HP之前核對，拒絕沿用原abandon／drop／unlock路徑，另在原Frame與輸出尾端重驗。

實際Source的Lightning_Magic_Wand、L1DamagePoison、DamageEvent、LegacyDefenseFeedbackPlan、L1AttackPc與L1MagicPc片段已凍結並可離線重放：這些raw事件沒有MagicHitCommitPlan，維持無額外數字；空效果metadata不建立延後顯示工作。魔杖PC與致命身軀已算好的原始值走原beginCalc／enqueueBattle／endCalc，不重套一般受傷倍率或反饋。魔杖對Source PET／SUMMON／TAMED只有地點特效、不扣HP；71100／91072也依Source轉地點特效後回原充能扣除尾段，保留原RNG／權限／LOS及其他免疫判斷。使用前核對實際inventory／item身分，已扣充能／箭矢／HP／MP確認不因舊目標特效被丟棄而消失。原Source其他傷害數字、HPBAR、奇古獸、256有界工作與關閉流程保留。

只完成程式檢查：19個實際類別JVM不初始化、凍結同位元重建、archive／欄位／每個原方法inverse、HP實際拒絕位置與Source片段核對通過；新增遊戲案0、資料庫連線0。舊SHA的756案／9,280檢查及b85唯讀MySQL證據沒有移算；20個SQL消費class與原lifetime／Source外控／顯示工作／origin、受控數字元件保留逐位元相同。正常／Ctrl、所有競技模式、跨角色實際等待、死復活／同ID替換、Frame／IO、DRAINING／STOPPED／Main關閉與故障重試仍待後續集中原生驗證。完整NPC毒傷族群、其他裝備Proc／cube／doll／週期、NPC工廠／AI與受傷動作等完整Source仍開放。未部署、未建立基準、未寫Source／玩家SQL／schema／WAL。收據：`source-program-update-progress-current.json`。


2026-10-24 完整缺口盤點與競技怪物毒傷續作：`2712a020f55a…`補Quest／Time怪物毒術成對前捕捉、原安裝身分與HP葉節點前重驗、結果建構時固定實際施毒者、獎勵動作鎖及九個送包點的輪詢時接收身分／延後發布／真正Frame檢查。Proof不持有Context／Scope／Receipt；weak來源與單怪物lease、既有HPBAR有界快取、計時及關閉流程保留。新施毒安裝也會使舊發布證明失效；普通同地圖移動不被凍結。原數值／成本／毒傷時鐘／GM分支／波次／結算本體經3959原方法inverse保留。17類JVM不初始化與凍結重建同位元；20個SQL class保持原位元，0新遊戲案／0資料庫連線。全技能／Source仍未完成；詳見同目錄source-completion-work-current.zh-TW.md與source-completion-inventory-current.json。系統目標已依使用者指示重新建立並啟用。維持BATTLE、Ctrl、資料安全及先程式後集中驗收；未部署。


2026-10-24 娃娃Source提交順序續作：`a6dead23ef17…`只在Source BATTLE走新分支；技能HP／特效及加傷順序依UnifiedProcExecutor，PC技能true／false、加傷false／false；加傷保持獨立HP與預先數字。原RNG／計算／WORLD完整原方法透過2450方法inverse保留。PC消費端雙方成對前快照與actual HP guard、致死數字token；自然NPC以實際receipt接受token，proof不持有Context／Receipt。13類不初始化、凍結同位元重建，20 SQL class原位元；0新遊戲案／0DB連線。首次物理捕捉、接受後proc回呼、受控NPC、SKILL29等與減傷完整路徑仍未完成；先更新程式後集中驗收，不部署。


2026-10-24 PC娃娃物理接受後續作：`7819f085fc0f…`沿用原rolled proc，PC物理事件攜帶無Context／Scope／Receipt的Pending，由既有accepted completion一次領取並清除，再執行娃娃、發布物理數字；拒絕事件沒有回呼。成對消費端與actual HP guard、fatal dead-target SKILL在magic RNG前略過，沿用既有工作者／關閉流程。2452原方法inverse、13類不初始化及凍結同位元重建，20 SQL class原位元；0遊戲案／0DB連線。首次動作捕捉、自然／受控NPC、SKILL29、減傷與特效最終接收端仍未完整；目標active，不部署。


2026-10-24 自然及受控NPC娃娃接受流程續作：`8f8c3dfada7f…`自然Quest／Time四個HP返回點於Hub鎖後固定receipt，原命中死亡／波次／獎勵發布完成後才執行娃娃，避免巢狀proc改變原結算；再發布物理數字。受控NPC原physical本體保留sibling，只在原正值appliedDamage及Target.seal處接proc。SKILL走既有寵物／NPC計算、bonus raw；同actor的後續接受生命token更新數字Proof，不重新查數字ID。同步Scope不進延遲Plan，不新增worker／TLS／SQL；2487原方法inverse、16類不初始化及凍結同位元重建，20 SQL class原位元。0遊戲案／0DB連線；最初PC攻擊／娃娃實例、NPC免疫buff、SKILL29、減傷、特效Frame與生命週期矩陣仍未完整。目標active，不部署。


2026-10-24 物理入口與娃娃召喚身分續作：`a9b4753bfc92…`Source BATTLE物理入口於action-lock等待前固定來源與選定目標；原攻擊body/RNG前重驗。activeMagicDoll volatile、召喚tuple識別位置紀錄；背包Item在動作鎖內proc RNG前封存，能力getter沿用原本體fallback並固定本次原召喚或空能力。娃娃到期／收回不拒絕普通物理HP；queued proc攜帶獨立Stamp，不持有Context／Scope／Receipt。受控目標保持原owner鎖流程。2447方法inverse、15類不初始化、凍結同位元重建及11純判定，20 SQL class原位元、0遊戲案／0DB連線。20個Source參考方法揭露泛用娃娃SKILL數值／metadata與29魔法計算依賴，尚未宣告29或全部娃娃完成；外控、減傷、NPC免疫、最終Frame與完整同SHA矩陣仍開放。不部署。


2026-10-24 泛用娃娃SKILL及proc觀看續作：`6eb1f97bf454…`同一既有readonly啟動交易新增1條SELECT，發布不可變技能metadata；泛用SKILL不再只收area模型，原魔法sibling只改用同列技能等級。loop逐筆傷害／目標castgfx／娃娃67／29速度，最後raw加傷；失敗續作、不加普通材料MP／機率；proc_spr暫存偏好預設true並接既有Source action，目標10格／娃娃可見畫面。2457原方法inverse、16類不初始化、凍結同位元重建與18純案例，20 SQL class原位元、0遊戲案／0真實DB連線。未宣告29全路徑或泛用魔法數值完成：最終Frame、角色reset、PC多事件接受次序、NPC免疫buff／負傷害、守護天賦等仍開放；新reader需後續中文MySQL唯讀重載。目標active，不部署。


2026-10-24 娃娃最終Frame與已接受速度續作：`d4a18f6d7cd0…`三迴圈固定viewer binding，沿用owned scope於實際Frame驗角色／生命／原娃娃／偏好／範圍；速度結果獨立綁接受的實際target與加速／緩速到期，允許同場景移動，status proof不依賴仍存續的娃娃或proc_spr，但原scope的施攻者／peer角色發佈gate仍保留，來源退役送達尚待實跑。8個既有timer欄位volatile、原狀態本體不變；角色selected邊界reset proc_spr、本人速度包最終秒數更新。Output Proof無Context／Scope／Receipt，無新worker／TLS／SQL，Frame不拿actor／Hub／status鎖。2995方法inverse、22類不初始化、凍結同位元重建及15純timer／wire案例；20 SQL class原位元、0遊戲／0真實DB連線。29方法Source證據修正子HP假設：enqueue為非阻塞入列，Source不逐筆等待；不新增同步／continuation模型，改驗原FIFO與接受guard。NPC完整Source數值及真正Frame／角色／生命／Main矩陣仍開放。不部署。


2026-10-24 娃娃29時間與狀態代次續作：`65733ea0e8b7…`原Source BATTLE娃娃29使用既有status／pet／Hub鎖與timer scalar；零／非正int毫秒為永久、長期限有限狀態與低16位writeH分開，原PC speed同步查詢補有界前綴。受控NPC原生加速無可移除技能則保持原狀；既有取消／到期／生命清理本體保留。4個owner的volatile Object代次與Speed proof的final代次拒絕解除後重施相同永久期限的舊包，無角色／Context保留、無新registry／worker／SQL。Frame沿用原receiver／角色／生命／場景與速度proof，只更新封包本地秒數；有效0與到期分開。2900方法inverse、23類不初始化、凍結同位元重建、25純案例及20 SQL class原位元；0遊戲案／0真實DB連線。完整NPC數值／免疫／自然原生速度、減傷、其餘材料／外控及實跑矩陣仍開放。目標active，不部署。


2026-10-24 娃娃減傷原召喚與最終Frame續作：`90e028b54be2…`保留NativePlayerCommit原減傷Calc／RNG及接受後一次發布／裝備尾段；Source BATTLE原Context固定各角色原召喚與binding，原drawDefense於既有受擊動作鎖下封存Item，不給舊動作替換娃娃能力。DefenseVisual只攜獨立身分／召喚／觀眾proof，真HP接受點封存fatal生命，最終Frame重驗角色／原娃娃／proc_spr／10格目標gfx或娃娃可見畫面；失效不拒絕普通HP、不中斷原裝備尾段。動態Frame載體finally復原，無新worker／TLS／SQL、無最終actor鎖。2566方法inverse、31類不初始化、凍結同位元重建與10純觀看案例；20 SQL class原位元、0遊戲／0真實DB連線。Source35方法證實零差額仍接受、減傷特效在接受後。無Context／無已證明動作鎖入口與被動數值尚待覆蓋；完整Source數值／NPC／材料／外控及實跑矩陣未完成。目標active，不部署。


2026-10-24 被動娃娃原角色能力選定續作：`28d4c0960ad3…`原物理Action powers分支保留；共同getter後備在已捕捉Source BATTLE Context且持有原角色鎖時，按原角色Snapshot的Choice固定完整Source能力或空能力。第一次後不混替換娃娃、未封存前失效固定空；角色／生命／位置／背包換代仍拒絕。Snapshot內Choice表依原參與者有界、子Context共用，本次娃娃proof未增加Choice／Snapshot／Context欄位；其他既有Context事件仍待關閉矩陣；無新鎖／RNG／worker／TLS／SQL。原ClientSession數值／成本／HP／queue位元不變；兩類22原方法inverse、37類不初始化、凍結重建與10純resolver案例，20 SQL class原位元、0遊戲／0真實DB連線。盤點31 getter呼叫點及22直接HP方法不代表覆蓋；真正Source attacker與Java參數null須逐入口區分，cube需實際效果NPC。無Context入口、NPC完整數值、58材料／外控／AI及實跑矩陣未完成；目標active，不部署。


2026-10-24 立方體原效果與精確狀態來源續作：`af5e7f5c5ac2…`Cube205／220保留原Cube Entity及NPC80149／80152、engine clear代次、原Room與建立者關係binding；原Hub掃描於engine等待前固定PC／Quest／Time角色或actor生命，未綁定BOT不冒充Source Monster。StatusLease與Event攜帶精確原實例，同ID重裝及舊到期分開；只有剛安裝列讀既有鎖內map，八秒狀態保留cube消失後的_effect物件。臨時安裝／scan載體finally復原，無新鎖／worker／TLS／SQL，原數值／時鐘／關係／HP指令經1063方法inverse保留。22類不初始化與實際schema核對、凍結重建及15純引擎案例，20 SQL class原位元、0遊戲／0真實DB連線。**這是來源資料基礎，HP施攻者及PC／NPC提交／最終Frame仍未改接，不宣告Cube Source完成。**完整材料／外控／NPC／AI及同SHA矩陣仍開放；目標active，不部署。


2026-10-24 PC立方體原效果NPC與HP提交續作：`00c8f86d6370…`Source BATTLE PC cube205／220使用Ref kind5 Effect(Entity,lifetime)，cube id為實際來源，無玩家master／caster。raw10／25 false／true走原typed HP Calc／防禦／Avatar／佇列與空數字Plan；燃燒先邏輯發布受傷動作，和諧不增一般魔法數字。精確原lease於入列前驗，入列後允許狀態到期／cube消失及同場景移動，仍驗效果代次／角色／生命／Main；原實際precommit、fatal接受封存與最終receiver Frame守衛接線。queued proof無Context／Scope／Receipt／Frame／Plan，無新worker／TLS／SQL；原engine鎖內退休已換代PC lease，kind5不觸發反應型回授。2668方法inverse、35類不初始化、凍結重建與22純契約案例；20 SQL class原位元、0遊戲／0真實DB連線。實際wire／佇列／Main矩陣未驗，NPC HP／建立者退役派送／MP／aura／狀態與完整材料／外控仍開放；目標active，不部署。


2026-10-24 自然NPC立方體狀態退役續作：`3133ff410169…`原Hub／engine掃描點將MONSTER狀態依原實際StatusLease／Cube、Quest／Time actor／owner／生命／generation退役；同ID換代不繼承或阻擋新lease，移動出範圍不取消原狀態。PC／BOT與WORLD／未綁定後備及既有HP／queue／輸出本體保留，無新鎖／worker／carrier／TLS／SQL。30原方法inverse、36類不初始化、27個真實engine-map與metadata純案例、凍結同位元重建及20 SQL class原位元；0遊戲／0DB。這不是實際NPC HP提交guard：已發出callback、建立者退役派送、HPBAR／動作、MP／狀態／aura與完整材料／外控及同SHA Main矩陣仍未完成。重設請求被目標工具以既有unfinished goal拒絕，既有完整目標仍active，未虛報重設或完成，不部署。


2026-10-24 自然NPC立方體實際HP與原master續作：`7bb76a3895bc…`兩個時鐘與兩個舊非玩家入口接原effect／精確lease／Quest-Time actor，HP raw10／25不依建立者在線，Quest安裝時固定派系。原master另驗binding供HPBAR／lawful，Source45方法確認effect先入列後解析master；原仇恨／AI仍待接。Hub HP兩個與發布三個受限副本保留原死亡／波次／spawn／邊界／公告／結束；無借用PC，接收IO不回滾HP。燃燒10格受傷動作走既有gfx抑制catalog，和諧不加普通earth／魔法公式。HPBAR比例／死亡hide與最終receiver／target／spawn guard、accepted life封存及carrier finally接線；原pending計數／executeCommitted納入同步in-flight，無新worker／queue／timer／TLS／registry／SQL。3508方法inverse、41類不初始化、37純契約、凍結重建及20 SQL class原位元；0遊戲／0DB。尚未完整對齊Source NPC非同步FIFO，MP／狀態／aura／表reader／AI／全部材料外控與同SHA Main矩陣仍開放；目標active、不部署。


2026-10-24 自然NPC立方體原FIFO與claim續作：`7e7efec497ac…`原cube producer之prepare／hurt後只非阻塞入原actor status-owned queue，原sealed Plan明確加Ticket，原proc drainer混合派送並走相同try/finally complete；無新queue／worker／timer／TLS／registry。HP guard要求精確原queue active event／queued-claimed旗標及effect／target生命，producer finally不早退役或重複扣queue名額。PC-NPC使用原GLOBAL及同一CAS，失敗PC發布釋放預留，NPC queue不再等admission；七個原非傷害credit writer不變。清列取消Ticket、獎勵IO失敗仍走結束尾段；queued欄位無Context／Scope／Receipt／Frame／Runnable。Source50方法確認pending在dequeue、HP前釋放，保留native claim時點；Source容量domain與其他直接NPC HP仍待統一。116方法inverse、52類不初始化、33純CAS／真實未綁定FIFO與混合drainer／drop案例、凍結重建及20 SQL class原位元；0遊戲／0DB。MP／狀態／aura／AI／全材料外控／正式合成及同SHA Main矩陣未完成，完整目標active、不部署。


2026-10-24 傷害專用容量與Main歸零邊界續作：`db68987e55c4…`原lifetime新增一個final AtomicLong傷害ledger，super後即初始化；PC-NPC CAS只預留此domain，成功發布仍加原work credit。精確原dequeue／drop／PC發布失敗釋放對應名額，七個原非傷害credit writer及AI程式位元不變；原Source設定鍵／GLOBAL／queued-only target限制保留。idle／terminated加傷害歸零fence以涵蓋admission窗口，原Main／worker／retry／close本體透過70方法inverse保留；無新registry／queue／worker／timer／TLS／SQL。53類不初始化、16容量隔離／並行／失敗案例＋同SHA33未綁定queue契約、凍結重建及20 SQL class原位元，0遊戲／0DB。其他直接NPC HP仍須共用FIFO及接受計畫，真實owner容量／設定／關閉、材料外控／MP狀態／AI／正式合成／游標矩陣未完成；完整目標active，不部署。


2026-10-24 自然NPC毒傷共用FIFO與原施毒者續作：`dcd2564fb8f6…`原poison Lease固定原Room／actor／owner／生命／generation，原tick不直接HP；typed毒傷Ticket共用cube／原proc的同一target FIFO、舊worker／drainer／try-finally complete／傷害ledger與Main。實際claim重驗目標及場景，HP不依賴施毒者仍存活，已入列後不因毒到期／替換撤銷；原binding另控HPBAR／lawful與最終receiver，不給同ID新角色權限。五個有界HP／發布sibling保留poison=true、屍體／波次／spawn／AI-arm／獎勵／結束，fatal生命及status到期proof分開，無queued Context／Scope／Receipt或新queue／worker／timer／TLS／SQL。3804方法inverse、62類不初始化、15毒傷混合queue／drainer／drop＋同SHA33佇列／16容量純案例、57 Source方法凍結與同位元重建、20 SQL class原位元；0遊戲／真HP／Main／DB。其餘直接NPC FIFO、PC毒傷、仇恨AI／數值／全部58材料外控／正式合成及集中驗收未完成；目標active、不部署。


2026-10-24 自然NPC物理及娃娃原FIFO接受後續作：`a3a7c084eb46…`自然Quest／Time物理與Scope娃娃SKILL／加傷共用cube／毒傷／原proc的同一target FIFO及舊drainer；原物理兩端POP回傳值，改非阻塞入列不假造同步接受。原來源／NPC生命位置／場景及roster於actual active claimed Ticket再驗；原已擲AttackProc／召喚Stamp／觀眾binding獨立保留，HP票據無Context／Scope／Receipt。五個有界HP／發布sibling保留死亡／波次／spawn／AI-arm／獎勵／結束後，一次重建固定原觀眾的同步Context並執行原proc，再發布物理數字；子HP非阻塞共用尾端，原SKILL／bonus數字時點與fatal proof保留。結構輸出移至HP／來源動作／admission鎖外，娃娃沿既有owned publication、最終Frame重驗；原OPEN／因果DRAINING／STOPPED／Main／容量／drop本體不變。3501方法inverse、76類不初始化、30因果數字生命／混合／巢狀原FIFO＋同SHA33佇列／16容量／15毒傷純案例、58 Source方法凍結與同位元重建、20 SQL class原位元，0真HP／遊戲／Main／DB。一般魔法與其他NPC生產端、Source全部物理接受後副作用／數值／AI／58材料外控／正式合成及集中驗收仍未完成；目標active、不部署。


2026-10-25 自然NPC範圍魔法原快照與接受魔消續作：`a7a7526f4aeb…`兩個Source BATTLE範圍入口於原等待前固定caster／實際NPC／owner／生命位置／generation及原觀眾binding，子Context沿用，不刷新權限；四個原POP回傳的NPC魔法HP入同一原FIFO／舊worker-drainer，精確erase-map lease存既有Commit的獨立After，無queued Context／Scope／Receipt／Frame／Runnable或整份Snapshot。原accepted callback先邏輯發布魔法數字、再以actual accepted actor／life／generation消耗舊lease，保留重施ABA。固定Source正傷害等接受、非正傷害原副作用分支只沿舊Hub模式／active與原生命guard消耗精確lease，不造HP／數字；producer temporary finally復原，WORLD／原primary-Ctrl／型別距離／成本RNG／Main保留。2552方法inverse、81類不初始化、16身分／真實狀態契約＋同SHA30物理／33佇列／16容量／15毒傷純案例、64 Source方法與凍結同位元重建、20 SQL class原位元；0真HP／遊戲／Main／DB。單體吸血／混亂雷霆、方向／冰凍／暈眩接受計畫、其他NPC生產端／全58材料外控／完整數值AI／正式合成與集中驗收仍未完成；目標active、不部署。


2026-10-25 自然NPC單體吸血混亂雷霆接受續作：`1d2c29ae2fe4…`自然NPC普通單體／吸血混亂雷霆葉節點共用原FIFO及等待前原Source／NPC／觀眾快照，body前重驗原身分；心靈／骨頭／護甲破壞仍留完整同步舊路，待HP前MP／狀態轉真正接受計畫，不假造結果。Single只帶原spell／lease、算好秒數與原傷害回復量、一次claim，無queued Context／Scope／Receipt／Frame／calculator／snapshot；混亂沿原roll，雷霆入列前原calculator一次，接受後不重擲。三個原接受finally邊界在HP鎖外、native波次結束前一次發布固定副作用，致死HP仍可回復；原alive／long上限／PC monitor的回復sibling只改本人HP及peer ratio輸出，原receiver／Source生命與封包現值於Frame重驗，owned publication及carrier finally、Main／容量原機制保留。2533方法inverse＋1回復sibling、84類不初始化、14新＋同SHA110純契約、66 Source方法與凍結同位元重建、20 SQL class原位元；0真HP／遊戲／Main／DB。方向／冰凍／暈眩、其他前置副作用／數值機率／NPC AI-factory／58材料外控／正式合成與集中驗收未完成，目標active、不部署。


2026-10-25 自然NPC方向冰凍暈眩接受續作：`181d56f96ba3…`方向／單體及範圍冰凍／兩個Shock BATTLE body的10個NPC HP端接原FIFO，5個body前重驗，保留等待前原source／actual NPC／生命位置／generation及觀眾快照。原計算chance／roll／boss固定，取消snapshot HP存活推估，真正接受後且native波次結束前一次狀態；Source暈眩時間保留接受副作用階段抽一次。18個原expiry寫點、4個volatile expiry及4個有界lease欄位覆蓋啟用／取消／到期／reset同值ABA，舊特效／palette輸出帶原receiver／Source／NPC生命與精確狀態至最終Frame，IO走既有owned publication。無新queue／worker／TLS／SQL，無queued Context／Scope／Receipt／snapshot；成本／RNG計算／PC／受控／WORLD原本體inverse保留，完整數值機率／屍體語意仍待稽核。2679方法inverse、90類不初始化、18新＋同SHA124純契約、68 Source方法與凍結同位元重建、20 SQL class原位元；0綁定HP／遊戲／Main／DB。心靈骨頭護甲破壞前置副作用、其他生產端／全58材料外控／正式合成與集中驗收仍未完成，目標active、不部署。


2026-10-25 自然NPC心靈骨頭護甲破壞Source前置階段續作：`3a00039c9b75…`四個Source新方法確認三技skillmode先扣MP／套狀態再回傳傷害，L1SkillUse在HP前呼叫，修正原待辦的accepted-only假設。Mind／Bone精確原本體在原Source動作／admission／Hub／實際owner下重驗後回傳真正native result，Arm兩葉同守原身分；owned publication scope先於Hub／owner建立。單一有界Context carrier每cast重設，只讓真正執行前置的自然NPC HP進原Single FIFO；stale／未執行直接handled drop，無假成功或同步HP fallback，不替Source增設前置回滾。Bone8原expiry寫點、2個volatile scalar／2個有界lease及2個原輸出端補最終Frame；PC／受控／WORLD及原RNG計算成本inverse保留。2644方法inverse、100類不初始化、24新＋同SHA142純契約、72 Source方法與凍結同位元重建、20 SQL class原位元；0綁定HP／遊戲／Main／DB。其餘生產端／完整數值與外控／全58材料／正式合成與集中驗收仍未完成，目標active、不部署。


2026-10-25 PC凍結DAMAGE毒傷原施毒者peer續作：`1888a77e921f…`Source不以施毒者存活／在線撤回target-owned tick；原scoped DAMAGE本體已null caster及raw false／true，外層Action仍supplied caster作pair peer。只改一個建構參數，Source BATTLE且Scope.find(target)原context／clockActive／frozen DAMAGE才無施毒者peer。原target／role／life／scene、clock／HP旗標／queue／admission／Main／cleanup／歸屬本體保留，無新欄位／worker／registry／SQL／HP／輸出。34原raw方法inverse、105類不初始化、同SHA166既有純契約、72 Source方法與同位元凍結重建、20 SQL class原位元；新PC毒傷案例0、綁定HP／遊戲／Main／DB0。無Scope／其他kind／nested與完整歸屬、其餘生產端／全材料外控／正式合成與集中驗收仍開放，目標active、不部署。


2026-10-25 自然NPC raw魔杖反射反饋原佇列續作：`d3832ce2967e…`魔杖／Mortal／Joy六個原POP自然NPC raw葉端共用原FIFO／worker／claim／容量／admission／Main與mode0，無普通數字／娃娃proc。原Origin七component保留，只新增final detached raw Source proof，沿原Member accepted life／role／position／dead規則，保留Source致死defender已接受反饋；普通未標記Origin原source policy不變。精確source Actor／actual target／owner／life／generation匹配暫存Admission，觀眾原快照、finally復原，無queued Action／Context／Scope／Receipt／snapshot；不額外加OPEN-only葉端檢查。共同accepted HPBAR／死亡波次／獎勵結束，存活HPBAR producer及Frame保留來源存活，死亡hide獨立。126原方法inverse、108類不初始化、12metadata＋同SHA166既有純契約、74 Source方法與凍結同位元重建、20 SQL class原位元；綁定raw HP／致死defender／遊戲／Main／DB0。其餘raw／AI／治癒／全技能外控／正式合成及集中驗收仍開放，目標active、不部署。


2026-10-25 真正接受Ref反饋自然NPC反射端續作：`c9375eddaba3…`acceptedTail→acceptedFeedback→validated Ref→Quest/Time反射HP，六個legacy/static POP不是本生產端證據。本批只補一個深層native反射端，原validFrame／live後傳同Ref／Live，Source BATTLE匹配raw Action再驗accepted非WORLD Frame、原method／descriptor／id／operands／room／owner／same actor，再進既有raw mode0 FIFO；stale handled drop，unscoped／受控／WORLD原fallback，RNG／已算結果／反應抑制／reflecting finally及原live本體保留，無新欄位／worker／queue／registry／SQL／queued Ref-Context。原PC HP尾段直接送死亡確認，無直接endBattle，未新增child wait，真實確認／場景／Main矩陣仍待驗。47方法inverse、109類不初始化、12 Ref ABI＋同SHA178純契約、74 Source方法與凍結同位元重建、20 SQL class原位元；綁定反饋／致死defender／遊戲／Main／DB0。其餘生產端／技能外控／正式合成與集中驗收仍開放，目標active、不部署。


2026-10-25 Source受傷動作抑制唯讀啟動接線：`9d664e8ced7e…`於既有readonly且autocommit=false交易新增Source gfx表SELECT，無新Connection或交易／命中SQL。immutable Set依Source空表／重複／NULL=>0／int集合及SQLException空或部分列行為發布；未載入後備本體保留。八個既有NPC reaction caller與全部HP／生命／Frame／Main原class保留，其他ACTION_Damage／RO target-sync／多resource路由未完成。10原方法inverse、112不初始化、14 JDBC代理＋同SHA190純契約、80 Source方法及凍結重建，20 SQL class原位元；實際MySQL／遊戲／綁定HP／Main0。完整Source與材料、正式合成及集中驗收仍開放，不部署、不寫玩家SQL／schema／WAL。


2026-10-25 NPC反擊屏障鏡反射原身分受傷send接線：`fe33a55e3eb2…`補兩個shared NPC publisher的第二個受傷encrypted send，Source BATTLE沿原companion／Quest／Time ingress Ref，id不符drop，原private live與typed snapshot gfx套唯讀清單。WORLD／未載入／unscoped／unsupported後備、HP／防禦特效／機率成本狀態／觀眾畫面／sender／順序inverse保留，無新欄位／retained Ref-Live-Context／worker／queue／SQL，驗證鎖於IO前釋放。2495方法inverse、113不初始化、16 Ref-snapshot＋同SHA204純契約、82 Source方法及凍結重建，20 SQL class原位元；綁定反射HP／接收端／Main／遊戲／MySQL0。Source完整數值與phase仍開放，其他封包同步／多resource／全技能與正式合成未完，不部署、不寫玩家SQL／schema／WAL。


2026-10-25 魔杖自然NPC原raw動作受傷外控接線：`03299c1ac82e…`只在Source BATTLE action2原自然Quest／Time raw Action中，沿原frame與id／kind／exact actor／snapshot typed gfx套唯讀抑制；空byte沿原captureOutput加密前略過，原defer／final Frame保持binding／來源目標life／scene／Main。無正HP／OPEN-only／扣次後item新gate，PC／受控／unsupported／WORLD／unscoped／unloaded原後備；HP／扣次RNG／item／特效／queue／worker／SQL原位元。35原方法inverse、114不初始化、16 unbound metadata-packet＋同SHA220純契約、84 Source方法與凍結重建，20 SQL class原位元；綁定wand HP／扣次／致死目標／接收端／Main／遊戲／MySQL0。其餘Source數值成本時序／週期治癒AI／全材料外控／正式合成與集中驗收開放，不部署、不寫玩家SQL／schema／WAL。


2026-10-25 原地面租約週期事件與PC火牢raw接線：`bb22536f4f9b…`原PC58工廠綁scene／epoch／creator Binding，原lease／pulse／GroundTick final detached metadata，在synchronized advance固定角色life。原時鐘PC端驗同lease後走既有ordered raw pair，RNG前重驗；HP葉端raw false／true共用FIFO，原reaction final Frame加scene carrier、原Action.close在原輸出完成後清理。競技／原geometry-timer／計算gate／非Source-WORLD-BOT-NPC後備inverse保留，無新worker／queue／clock／TLS／registry／SQL。3531方法inverse、125不初始化、16 unbound metadata＋同SHA236純契約、91 Source與凍結重建，20 SQL class原位元；綁定PC HP／pair／角色／receiver／Main／遊戲／MySQL0。下一步補queued HP獨立scene-lease metadata，現只沿raw member guard；完整NPC火牢、Source數值間隔型別免疫／主人死亡／effect顯示及全技能正式合成未完成，不部署、不寫玩家SQL／schema／WAL。


2026-10-25 ground排隊HP原場景與接受後生命證明接線：`d2b6165cfb20…`原raw HP Plan單一volatile detached PulseProof，不保存Context／Action。drain用原resolved caster在防禦RNG前檢查並保留primitive拒絕local，原actual HP guard再驗actor binding-life／false-true／原lease Entity指標-scene-epoch-Main；unmarked原行為保留。final Frame用原HP鎖內acceptedState之target acceptedLife保留合法致死輸出，拒絕後續life ABA／role／scene；不以個別expiry或新OPEN-only撤回已准入HP，原DRAINING與STOPPED／Main保留。131方法inverse、126不初始化、18 unbound metadata＋同SHA252純契約、91 Source與凍結重建，20 SQL class原位元；實際HP／致死／pair／角色／receiver／Main／遊戲／MySQL0。完整場景線性化與first-wait factory、NPC火牢與Source數值顯示／全技能正式合成仍未資格化，不部署、不寫玩家SQL／schema／WAL。


2026-10-25 ground真正材料副本成本安裝工廠接線更正：`814ce91426dd…`前批只覆蓋舊direct GameHub.placeGroundSpell，實際材料走nativeMaterialPlaceGroundSpell→NativeGroundMaterialAction.castBattle兩副本／成本／install，新lease原未有Source metadata。本批沿原current Batch.accepted Ingress.binding（reserve於首次player-lock等待前保存），驗同原caster／room／live engine，不fresh capture。detached Draft進兩暫存copy，new Entity指向live engine；live scene僅在原accepted install發布，原cost callback／Rejected／recalc／install順序inverse保留，finally還原installing slot，無retained Batch-Ingress-Context／新TLS-registry-worker-SQL。81方法inverse、131不初始化、18 unbound staged-metadata＋同SHA270純契約、91 Source與凍結重建、20 SQL class原位元；綁定角色／MP-reagent-rollback／HP／Main／遊戲／MySQL0。完整Source與NPC火牢／全材料／正式合成／集中驗收仍開放，不部署、不寫玩家SQL／schema／WAL。


2026-10-25 自然NPC火牢原tick與raw FIFO接線：`789ec430d21e…`原ground advance追加自然Quest／Time actor／owner／life-generation／pose與原觀眾，Entity固定原creator lane，pulse／原Tick在null-PC分支前排程派送；正常PC／BOT後備保留。原來源lock後、RNG前驗原場景生命／NPC位置與freeze-earth-bind，唯讀stats與有效INT按Source正抗火／屬性弱化計算；反應先於原mode0 FIFO raw HP，無普通魔法數字／娃娃proc。HP及最終輸出沿detached Origin proof／原接受後生命，保留移動與個別到期後的已准入傷害及DRAINING／STOPPED／Main；不新增queue-worker-TLS-SQL或保留Context。1122 inverse、135 no-init、26新＋同SHA288純契約、93 Source方法／25檔、凍結重建及20 SQL class原位元；0綁定HP／角色／Main／遊戲／MySQL。完整型別-SKM0-PC SKD3／其他來源／顯示／材料外控及正式合成集中驗收仍開放，不部署、不寫玩家SQL-schema-WAL。


2026-10-25 PC火牢Source SKD3有效數值與反應範圍：`0753f32b6bbc…`原paired raw action的PC58 prefix在RNG前驗精確lease／scene／source-target role-life與競技RUNNING canAttack、freeze-barrier-bind；有效INT／正抗火後依料理及dragon-patience-immune原lease選Source SKD3固定鍵，先加減再整數half。保留原receiveDamageInternal reductionOnly防禦proc／武器-化身修正，原raw false-true FIFO／accepted fatal-life／Main，未啟用directRaw。反應使用原frozen audience／deferred scope且原ground final Frame再驗native screen，無新增fields-scope-TLS-queue-worker-SQL。2469 inverse、136 no-init、35新（含Source31列及其餘ID oracle）＋同SHA314純契約、93 Source方法／26檔、凍結重建及20 SQL class原位元；0綁定pair-HP-RNG-fatal-receiver-competition-Main-game-MySQL。完整Source／材料／型別／競技對應與正式隔離合成集中驗收仍開放，未部署或寫SQL-schema-WAL。


2026-10-25 cube真正材料工廠與Source掃描節拍更正：`b3550c62416c…`前批cube-origin只接legacy place；真正NativeCubeCastAction→Hub.nativeMaterialPlaceCube→engine.nativeMaterialPlace用plain ctor原未有Entity／faction，Source HP端因而捨棄。本批兩Hub口用原prewait Batch Ingress.binding與detached Draft，兩原ctor接既有metadata overload，原geometry-admit-cost-ID-list保持；accepted return才發布scene、finally復原slot。只改Source scene scan500ms，原L1Cube900／1000 status／4th ignition-MP與5th harmony HP保持，原實際effect-NPC HP及role-life-FIFO-final-output保持。1047 inverse、138 no-init、20新＋同SHA349純契約、101 Source方法／29檔、凍結重建及20 SQL class原位元；0綁定MP-material-rollback-HP-role-Main-game-MySQL。Quake／Shock工廠、完整Source／材料／顯示MP端／正式合成與集中驗收仍開放，不部署、不寫SQL-schema-WAL。


2026-10-25 地裂衝擊原材料狀態與跨角色secondary生命：`a50f46d3682f…`原Quake／Shock材料口接prewait Batch binding與actual Entity／faction、accepted scene；原成本geometry不變。primary／secondary租約存原target role-life-generation及MR refresh link，原advance發布bounded immutable volatile索引供數值與final Frame重驗；舊expiry／recast不借新cell，clear換epoch清索引。NPC地裂接原actor1秒freeze；固定Source衝擊只有4000 marker、MR0，WORLD／BOT／未綁定原數值保留；500 scan與900／1000 status保留。原typed clock與raw self-output-close承接原receiver、scene／epoch／life／index／range，最終不遍歷mutable map／新增locks，無新HP queue-worker-timer-TLS-SQL或queued Context。3542 inverse、151 no-init、24新＋同SHA369純契約、106 Source方法／32檔及凍結重建、20 SQL class原位元；0綁定cost-HP-freeze-IO-Main-game-MySQL。完整Source／材料／MP與顯示／正式合成集中驗收仍開放，不部署、不寫SQL-schema-WAL。


2026-10-25 燃燒和諧原狀態與MP接受生命：`a335499de0a3…`燃燒／和諧原advance有界immutable identity索引、nonHP實際租約／target／原receiver；屬性／狀態／final Frame拒絕舊角色或替換cell。PC回魔原store接受tag與實際MP agreement；接受後到期不撤銷已提交通知。自然NPC用原actor／life／generation／faction與原admission-Hub-encounter回魔sibling，不借caster或新ID。Source long bounded加法、原後備overflow及原HP FIFO／成本／節拍保留。原raw close清同步proof；無新queue-worker-timer-TLS-SQL。3671 inverse、154 no-init、25新＋同SHA393純契約、108 Source方法／32檔、凍結重建及20 SQL class原位元；0綁定角色／實際MP／IO／Main／遊戲／MySQL。完整Source與集中驗收仍開放，不部署、不寫SQL-schema-WAL。


2026-10-25 立方體Source狀態GFX與原關係：`7cd7d86e67db…`八個PC／自然NPC原狀態GFX口讀既有唯讀metadata castgfx／castgfx2；和諧原status發布前存實際relation，same-id／關係變更不改舊cell，Source自然怪選敵方特效。Source友方屬性經typed bridge原status body先於effect，略過同scope原尾端重複通知；到期／未綁定／WORLD／BOT保留。Source0 GFX沿原object-word，正值資源remapping／界限保留；原raw／final Frame／close與HP成本節拍不變，Quake／Shock installed加等待後到期拒絕。一個原status volatile metadata與無狀態helper，無新queue-worker-timer-TLS-SQL。2536 inverse、157 no-init、27新＋同SHA418純契約、109 Source方法／33檔、凍結重建及20 SQL class原位元；0綁定角色／IO／HP／MP／Main／game／MySQL。完整Source及集中驗收仍開放，不部署、不寫SQL-schema-WAL。


2026-10-25 立方體原Source角色與競技新租約：`5bcf516cc7df…`四cube advance原self／Alliance口沿原creator Binding及prewait target Scan；獨立確認Binding.source已是qualified Source角色，不再caster ID查當前session或重複組合。self驗原source-account-selected-owner-role與target life，same-id新角色不承接舊self。原clan-party ally保留，新hostile遵守原PvP／team及原force、實際target force，不借Ctrl；新套用沿原Hub／engine驗epoch-Main-OPEN-RUNNING，natural kind/life/gen/faction及BOT／WORLD後備保留。原installed status／queued HP／cost／RNG／clock／output不變；無新鎖／欄位／queue-worker-timer-TLS-SQL。66 inverse、161 no-init、33新＋同SHA445純契約、109 Source方法／33檔、凍結重建及20 SQL class原位元；0綁定角色／HP／IO／Main／game／MySQL。完整Source及集中驗收仍開放，不部署、不寫SQL-schema-WAL。


2026-10-25 立方體真正Source啟動與全域相位更正：`6f7a0186eb3a…`固定Source GameServer→StartTimer_Skill直接排四Runnable initial0／period1000；先前start500葉方法當生產路徑的判斷取代，1個既有cadence oracle更正，Source檔不改。原Hub四個bounded primitive Phase於ground task註冊前init、原type collector在rooms前推進一次、close清空；空場／room-clear／不同placement保留服務相位，同slot跨room，延遲掃live一次且保留fixed-rate origin。四原new-effect compare接slot、Source cadence1000；原100ms owned drainer與900／1000 status、HP／FIFO／cost／RNG／output保留，無新schedule-worker-timer-TLS-queue-lock或actor／Context保留。1078 inverse、163 no-init、33新＋同SHA478純契約（477原預期＋1更正）、111 Source方法／35檔、凍結重建及20 SQL class原位元；0綁定Hub／startup／多場／角色／HP／IO／Main／game／MySQL。全Source與集中驗收仍開放，不部署、不寫SQL-schema-WAL。


2026-10-25 立方體原輸出與寫出後known-role配對：`373087ebfb85…`四實際放置＋四重建口帶原觀眾／pre-Hub receiver與effect；accepted publish沿原Hub-engine seal index。detached Work走原Output，原loop及三completion尾端once rescue，creator退場不借新角色；其他output／completion保留。原move perceive管native screen進出、明確重建可refresh，final加密前驗instance／epoch／index／角色／到期／range／object-geometry；實際writeFrame成功後才mark／remove known。expiry只remove真正收到的原角色，same-id／新角色／unseen不借舊事件，初始audience釋放與stale seen prune。Work無Context-Batch-Action-Frame-Receipt、同步Context隔離finally還原；原encoders／資源rewrite／HP／成本／RNG／0-1000與900-1000節拍保留，無新queue-worker-timer-TLS；compiler-only visibility stubs未入jar。2582 inverse、169 no-init、39新＋同SHA511純契約、113 Source方法／36檔、凍結重建及20 SQL class原位元；0綁定queue／rescue／角色／IO／Main／game／MySQL。Source template／GFX／visibility外控等完整目標開放，不部署、不寫SQL-schema-WAL。


2026-10-25 立方體原靜態模板初始呈現續作：`220bd23c04b6…`原pre-cost Draft凍結既有snapshot純值，四cube一次生成Source initial GFX/status/exp/tempLawful/name，四encoder走actual effect owned值、原native tail與資源rewrite；模板lightSize不誤當初始chaLight0。缺列／錯型／失敗／非法bound先拒絕，無new SQL或actor，5個final純值欄位隨原生命。呈現random順序與Source executable oracle對照，原戰鬥cost/chance RNG／HP／角色known-output／Main位元保留。534 inverse、175 no-init、371新＋同SHA550純契約、120 Source方法／38檔、凍結重建及20 SQL class原位元；0綁定factory／成本／角色／reload／IO／Main／game／MySQL。custom work/spawn、完整屬性與visibility/resource外控等全目標開放，不部署、不寫SQL-schema-WAL。


2026-10-25 Fatality原角色成對抽籤與明確空攻擊者續作：`65a8a74fad50…`single/area pre-Hub binding-life-pose與原target/receiver快照，兩die在ordered pair／admission／target commitSelf重驗後一次抽籤並raw入原FIFO，舊sink handled-only。queued Plan只加detached NONE Proof，HP不依trigger活著；fatal接受沿原Member life seal。只有原target-guard／caster-null／Ref-null明確proof略過attacker-specific Calc，PC舊binding／NPC-effect／未知來源不轉NONE，外層145/149及Avatar保留。原deferred cosmetic帶detached root/subject/receiver到final加密前，resource GFX header相容，Context同步隔離finally還原；無新queue/worker/timer/TLS/SQL，原native catalog/RNG/predicate/Ctrl/WORLD保留。2555 inverse、179 no-init、32新＋同SHA921純契約、120 Source方法／38檔、凍結重建及20 SQL class原位元；0真HP／角色／wait／fatal-area／IO／Main／game／MySQL。native provisional catalog Source mapping與全材料外控AI／生命週期集中驗收仍開放，不部署、不寫SQL-schema-WAL。


2026-10-25 NPC對玩家原目標與防禦快照續作：`9608e7709b18…`原canonical tick在NPC lookup前固定target binding-life與room audience，validTick加舊目標gate；原ingress帶detached Members／Ref／defense snapshot，leave清精確scope，不持有Context或新TLS。6個真正NPC-PC sink走target actionlock／原admission-commitSelf／同FIFO，Source物理false/false與magic true/false；raw Guard原target生命／actual fatal seal保留，所有raw final結果另驗accepted-life防復活再死ABA，queued HP不另要求source NPC或master活著。既有doll proof-final frame用原scope召喚與receiver；原AI／RNG／WORLD fallback／成本Ctrl保持。2603 inverse、186 no-init、32新＋同SHA953純契約、122 Source方法／38檔、凍結重建及20 SQL class原位元；0真AI／HP／角色／doll／IO／Main／game／MySQL。上游NPC完整數值AI-factory／magic accepted plan、bot-ground-heal與全材料外控集中驗收仍開放，不部署、不寫SQL-schema-WAL。


2026-10-25 NPC魔法接受後吸血魔消與追蹤HPBAR續作：`bfd2fdb1c68c…`兩magic原paired Plan凍結requested heal與erase，Source10/28常數依固定Source，不採native僅28compatibility；原accepted completion once，拒絕不執行，fatal可heal但不mutation，proven scope原aggregate heal略過。原NPC/owner/life/gen在admission-Hub-encounter下走2個原heal sibling long cap，dead不復活；只既有tracked weak viewers/control/range/ratio/life/receiver final IO，actual writeFrame後記cache，原completionFrame與area Context隔離finally還原。無queued Context/Scope/Definition/Frame/Receipt或新queue/worker/timer/TLS/SQL。2674 inverse＋2 sibling、194 no-init、33新＋同SHA985純契約、124 Source方法／40檔、凍結重建及20 SQL class原位元；0真HP/heal/角色/IO/Main/game/MySQL。ALT_ATKMSG與其他mutation／全數值AI-factory／全材料外控集中驗收仍開放，不部署、不寫SQL-schema-WAL。


2026-10-25 NPC非正傷害精確魔消與零HP事件修正：`0ac22a1acffa…`fixed Source正傷害才gate acceptance，零damage不commitHP，非正傷害direct target mutation。原paired target lock/admission/commitSelf前置handled gate：已知攻擊零damage執行exact erase並return，不產生HP/After；signed negative捕捉lease於既有Plan，原owned HP返回後apply再finally清除。正值accepted路徑與不明metadata/非attack/物理/WORLD完整保留。Direct重驗原source-account-role-life/status/hub/target alive/OPEN/Main；recast/close/draining提交間替換不刪新狀態。無新field/queue/worker/timer/TLS/SQL/RNG/cost。14原方法inverse、其餘父archive全位元一致、196 no-init、37新＋同SHA1018純契約、124 Source方法/40檔、凍結重建及20 SQL class保留；0真HP/角色/IO/Main/game/MySQL。有號HP/治癒/不死系/mana-only/其他mutation/ALT_ATKMSG/上游數值AI-factory/全材料外控與集中驗收仍開放，不部署、不寫SQL-schema-WAL。


2026-10-25 共用負傷害治癒生產端與原目標FIFO續作：`bf517bd826d7…`實際NPC signed CalcTicket/NativePlayerVitals原已支援；缺口為CombatPlayerRecovery Source BATTLE負值仍直接HP/IO。本批private receive gate保留完整原fallback sibling；negative捕捉原bindings並繼承raw Member/physical Stamp/magic Snapshot life，target/caster ordered pair/OPEN admission/commitPair重驗，原signed calc/raw Plan入原target FIFO。queued Guard只target，來源退休換角不撤回已入列HP；target role-life/STOPPED/Main及final Frame沿原guard，DRAINING排空。無新field/queue/worker/timer/TLS/actor/SQL/RNG/cost/directHP/directIO。5原方法inverse＋1 fallback、其餘父archive全位元相同、201 no-init、31新＋同SHA1055純契約、420 exact compiled Source HP比較、124 Source方法/40檔與20 SQL class保持、凍結重建；0真HP/heal/pair/角色/IO/Main/game/MySQL。外層cast prewait target-life與全治癒/不死系/數值、NPC mutation/ALT_ATKMSG/全材料外控AI-factory/正式隔離合成及集中驗收仍開放，不部署、不寫SQL-schema-WAL。


2026-10-25 三個主要治癒原參與者與cost前生命續作：`3530b2a574dd…`確認單體/集體/大地祝福3個直接PC HP尚未經前批shared signed FIFO；本批三root原caster pre-Context binding-life-pose、原room participants在player-action wait前固定。單體原selected-PC resolve後cost/RNG前gate，兩target list逐一排除舊secondary/new entrant，保留原順序/競技/Ctrl/party/clan/range/type/LOS與視覺receivers。既有locks32次try-release/all-owned scope，actions先於OPEN admission，不持外層Hub等admission；原commitPair/finish/failure/exception清members/field。signed member沿同一原Member；無新queue/worker/timer/TLS/SQL/RNG/cost。2482 inverse＋3完整root sibling、203 no-init、32新＋同SHA1086純契約/420既有Source HP比較、124 Source方法/40檔/20 SQL class及凍結重建；0真target-wait/cost/RNG/heal/HP/IO/Main/game/MySQL。三直接PC HP與預覽輸出、first Hub discovery生命、全治癒不死系/controlled/NPC/bot/數值及全材料Source外控AI-factory/formal隔離合成集中驗收仍開放，不部署、不寫SQL-schema-WAL。


2026-10-25 三主要PC治癒signed FIFO與接受後HP輸出續作：`b0ee8fd70e04…`三healing stores在proven cast改送原requested signed HP，原fallback加法保留；zero不造HP，三reversal依compiled Source PC magic true/false。common display entry在既有Plan加Recovery原negative-input proof，不覆蓋NPC mutation；actual BATTLE completion對非正/非fatal recovery只真HP/ratio，unchanged/rejected無輸出，避免原markCombat/lastDamage污染，positive/fatal/unmarked/WORLD全留。target-only raw Guard跨caster退休/cast close保留，target role-life/Main/STOPPED沿原final Frame。22 send只filter精確PC preview，3 PC ratio攔截，HP/MP cost及NPC/companion/single bot ratio保留。無新HP lane/worker/timer/TLS/SQL；原成本RNG/status/競技Ctrl/原計算本體保留。2491 inverse、205 no-init、42新＋同SHA1118純契約/420既有Source HP比較、125 Source方法/42檔與20 SQL class保持、凍結重建；0真heal/HP/cost/RNG/IO/Main/game/MySQL。全治癒不死系/數值型別免疫/first Hub life/其他NPC-controlled-bot-periodic與Source外控AI-factory/formal隔離合成集中驗收仍開放，不部署、不寫SQL-schema-WAL。


2026-10-25 治癒INT中文成長唯讀快照與Source整數公式續作：`06edf8bbf856…`實際CombatDatabase.load傳入原readonly/non-autocommit conn，在NativeSkillTargetMetadata.read借同交易1個有界INT growth SELECT，原驗證成功才install immutable純值snapshot；專用治癒骰無cap10、INT<1 -2/127 clamp、缺欄/列依Source getter fallback，無新conn/tx/write。標記Source healing cast的shared INT/two statuses/three lawful與two die sums/one water multiplication匹配int wrap/negative shift；原RNG順序次數bounds/fixture/cost與WORLD本體inverse。2681 inverse、213 no-init、25新＋同SHA1160純契約/160 status/30 lawful/420既有HP比較、129 Source方法/43檔/20 SQL class與凍結重建；0真DB/INT reload/actor/heal/HP/role/IO/Main/game。其他effective stats/leverage/producer/undead/type/immunity/first Hub life與全材料外控NPC AI-factory/formal隔離合成集中驗收仍開放，不部署、不寫SQL-schema-WAL。


2026-10-25 治癒實際受控型別與模板不死系續作：`96c8cdd0e3d7…`Source只對L1MonsterInstance讀undead，pet／summon／tamed不繼承模板1／3反轉免疫。修正單體＋共用group／Nature的3原讀取點，限原Source healing cast，使用actual selected record／actor final Kind；ARENA／null／unmarked／WORLD原本體保留。2原class、1無欄位無配置helper，無新lookup／SQL／RNG／HP／鎖／worker；原成本／status／HP／lifetime／Main與output inverse保留。2472 inverse、214 no-init、40新＋同SHA1185純契約、112型別signed比較（84受控Source＋28建模Monster）、131 Source方法／45檔、20 SQL class及凍結重建。0真actor／heal／HP／role／IO／Main／DB／game；自然factory／tower-door／bot、受控正治癒提交final output與週期、first-Hub生命、全材料Source及正式隔離合成集中驗收仍開放，不部署、不寫SQL-schema-WAL。


2026-10-25 受控治癒原角色生命與接受後HPBAR續作：`41ed4198e5e8…`原cast try/finally內、玩家鎖等待前固定原owner Member與actor life／bag／placement／pose，1個bounded volatile Object carrier在close清除。3原正heal口驗原source-owner pair／admission／OPEN／Main後用原synchronized sibling，僅long-safe sum，原WORLD／ARENA／unmarked保留。接受changed HP排既有scope output，detached proof／原receiver沿generic marker與真正final frame，驗owner-actor-receiver生命與bag／pose／HP-max／hpbar／screen／DRAINING／STOPPED／Main；原預覽ratio按固定id攔截，其他cost／status／RNG／visual留原本體，無新FIFO／worker／TLS／SQL。2638 inverse、218 no-init、28新＋同SHA1225純契約、90 Source HP比較、133方法／45檔／20 SQL class及凍結重建。離線替身初始化原completion State，無null-state cleanup錯誤。0真bound actor／heal／HP／role／IO／Main／DB／game；full signed／owner-meter／週期／自然factory／tower-door-bot／first-Hub及全材料Source、正式隔離合成集中驗收仍開放，不部署、不寫SQL-schema-WAL。


2026-10-25 原Hub玩家名單生命先於綁定與治癒共用續作：`d8174e9669ec…`原Context Hub內玩家binding點Source BATTLE先讀life再原capture一次；原caster pre-Hub／名單put順序／鎖與finally保留。1個bounded Object carrier，原constructor成功封immutable identity map、copy沿同snapshot；shared signed最末fresh fallback借原Member／missing-mismatch null，較早healing/raw/doll/magic優先序與unmarked/WORLD原體保留。17 inverse／221 no-init／26新＋同SHA1253純契約、90／112／160／30／420繼承Source比較、133方法／45檔／20 SQL class與凍結重建，0真bound Hub／HP／role／IO／Main／DB／game。其他factory自行建證口與full first-Hub競態仍未完成，全材料Source／自然factory-AI／塔門bot週期／controlled owner-meter／正式隔離合成與集中驗收仍開放；不部署、不寫SQL-schema-WAL。


2026-10-25 原Context各玩家工廠與生命位置續作：`460fb2d2c7a3…`pre-Hub caster先life後原binding，原magic snapshot前seal-inherit；5一般Member（raw roster/root/peer、doll、Fatality）、2 Actor、3 natural/material capture、shared Actor與material PC initial-token接原first proof，較早raw/cast/doll/magic pose與accepted-death token優先，missing明確stale不新接。原accepted-frame及WORLD、HP／Ctrl／cost／RNG／鎖／Main本體inverse；raw peer只原同步compute借Action，實際LambdaMetafactory形狀執行，無新欄位／queue／worker／TLS／registry／SQL。85 inverse／227 no-init／34新＋同SHA1279純契約、133方法／45檔／20 SQL class及凍結重建；0真bound cast／HP／role／IO／Main／DB／game。剩餘constructor全盤點附收據，accepted／explicit-life／NPC tick-master／cube／其他ingress-pose及NPC discovery、全材料Source／factory-AI／正式隔離合成集中驗收仍開放；不部署、不寫SQL-schema-WAL。


2026-10-25 HP接受玩家值與原Actor位置續作：`f047928e5efa…`原acceptedState HP Queue vitalLock持有時Frame once封immutable values，target沿event targetLife／exact Guard acceptedLife與HP接受pose，source沿原Guard／Actor。2 raw accepted口複製原值與approved corpse token，5 doll/signed Actor轉Member留原pose；新增explicit-pose constructor完整原初始化inverse。NPC主目標及Fatality pre-Hub入口life／pose先於binding；unknown／WORLD原fallback，原HP drain／queue／completion-CAS／admission class位元、cost／RNG／Ctrl／鎖／Main保留。91 inverse／230 no-init／28新＋同SHA1313純契約、133方法／45檔／20 SQL class及凍結重建；0真bound HP／role／IO／Main／DB／game。無Frame／Context／Plan／Member back-reference、新worker／SQL；unknown provenance／NPC viewer-master／cube／NPC discovery／全材料Source與正式隔離合成集中驗收仍開放，不部署、不寫SQL-schema-WAL。


2026-10-25 NPC名單與weak原主人Ref續作：`82e443092c1c…`Source-only origin sibling原Hub內名單／life-before-binding，沿原map與較早primary，無Hub內新player lock；WORLD／unloaded留完整原體。Ref/pet wait前主人life-binding-pose封immutable Ref sibling，1 final Object僅weak主人，原8 identity／equals-hash／ctor／NPC owner token空間保留；actual master ingress沿原證明、換角復活weak失效不新接。既有queued HP／Ref-live不讀新marker，core class位元保留，無source退場新撤回／queue／worker／TLS／registry／SQL。32 inverse／233 no-init／27新＋同SHA1341純契約、133方法／45檔／20 SQL class與凍結重建；0真bound Ref／AI／HP／role／IO／Main／DB／game。birth-lifetime／manual Ref／NPC discovery-factory-AI／全材料Source與正式隔離合成集中驗收仍開放，不部署、不寫SQL-schema-WAL。


2026-10-25 NPC初始目標沿首次生命續作：`026508f9d2bf…`controlled/raw/material/healing 6初始口沿原Context record與actor、自然epoch／controlled Ref.owner，主人沿舊玩家證明；unsupported stale。原accepted死亡seal、live重驗／Ctrl／鎖／HP／Main／WORLD保留。30 inverse／234 no-init／30新＋同SHA1368純契約與同位元重建通過，133 Source方法／45檔與20 SQL class保留。真bound AI／HP／role／IO／Main／DB／game0；完整Source與factory／owner-birth/manual-Ref仍開放，程式全補齊再集中驗收；未部署、不寫Source／玩家SQL／schema／WAL。


2026-10-25 材料主目標主人接收端首次生命續作：`7f92c74bdb50…`negative29/152/153、mass-slow76、wind167及buff148/149四Aim的5初始life沿原Context較早玩家／Ref；11 valid／position／final-frame gate核對原source／primary／master／receiver，receiver保留observer／HP／移動資格。3 final attachment支持原battleCompanion或tamed；WORLD保留原list-only。97 inverse／243 no-init／39新＋同SHA1398純契約／既有Source比較與同位元重建通過，133 Source方法／45檔、20 SQL class保留。真bound cast／status／HP／role／IO／Main／DB／game0，完整Source／58材料／factory-AI／owner-meter／正式隔離合成開放，程式全部完成後再集中驗收；未部署、不寫Source／玩家SQL／schema／WAL。


2026-10-25 共用範圍玩家名單與施法前來源生命續作：`b53685073019…`area原bound Context.player predicate借較早玩家／accepted corpse證明，保留原順序／duplicate／observer-dead-moving audience；2 pre-body scene沿原caster life-pose，不全域改Context或accepted回呼。原32 lock attempts／scope／admission／Hub-population／try-finally inverse保留。65 inverse／245 no-init／25新＋同SHA1437純契約與同位元重建通過，133 Source方法／45檔、20 SQL class保留；新增本地方法參照定位census，非完成案數。真bound cast／HP／role／IO／Main／DB／game0，完整Source／58材料／其餘accepted-output method refs／factory-AI／正式隔離合成仍開放；程式全補齊後集中驗收，不部署、不連MySQL、不寫Source／玩家SQL／schema／WAL。


2026-10-25 間接名單與凍結接受目標生命續作：`9dffc456ad1c…`10 applicable indirect roster借原玩家／accepted freeze生命；剩1 classic-WORLD NPC physical members原位元保留，不擴WORLD。原Frame HP-vital-lock once-proof為原Event Linked freeze補無visual Plan accepted target，已知proof優先、unknown attacker不捏造；later deathLife不能重設accepted生命。freeze／cancellation 2初始life與相消final source-owner-actor-receiver／single-or-tamed附件接原proof，原HP／queue／CAS／source-retirement／locks／Ctrl／成本／status／Main／WORLD留原。188 inverse／257 no-init／38新＋同SHA1462純契約／既有Source比較與同位元重建，133 Source方法／45檔、20 SQL class保留。真bound cast／HP／role／IO／Main／DB／game0；完整Source／58材料／freeze政策／factory-AI／正式隔離合成開放，程式完成後集中驗收；未部署、不連MySQL、不寫Source／玩家SQL／schema／WAL。


2026-10-25 NPC出生工廠型別與signed治療續作：`f85ed3c0636a…`3 actor各1 final Object／9 leaf ctor保存純量Source impl-undead；delegator不重取，controlled與transfer沿Pet／Summon。2 Source healing signed口沿出生型別，32原類別與Source factory/type-only oracle896比較，保留MonsterGuard非Monster、Tower-Door只擋治療方向、int wrap。2777 inverse／259 no-init／30新＋同SHA1500純契約／既有Source比較與同位元重建，73 Source檔／133 bounded參考及20 SQL class保留。原RNG／成本／HP／鎖／AI／生命／WORLD留原；不保留row／snapshot／registry。原完整factory／AI／restore-rollback／獨立守護塔／bot／periodic／免疫與全Source仍開放，真bound factory／AI／HP／role／IO／Main／DB／game0，程式完成後集中驗收；未部署、不連MySQL、不寫Source／玩家SQL／schema／WAL。


2026-10-25 NPC型別記憶體快照與原還原續作：`f0e9ae625401…`3原Snapshot各final transient Object＋immutable ctor sibling，原record components／identity留原。4 producer（含相消新目標Snapshot copy）與4 Quest-Time restore借primary型別／NPC id，不查目前模板／借暫時tame class，legacy未知不promote。已知controlled pet進化只調整copy id，不改birth型別／讀表。403 inverse／264 no-init／30新＋同SHA1530純契約與896繼承Source型別比較及同位元重建，73 Source檔／133 bounded參考、20 SQL class保留。原HP／life／inventory／lock／WORLD完整inverse；無actor／Context／Frame／table registry，真bound restore／rollback／AI／HP／role／IO／Main／DB／game0。完整factory政策／AI／守護塔／bot／免疫／Source／正式隔離合成仍開放；程式全部完成後集中驗收，未部署、不連MySQL、不寫Source／玩家SQL／schema／WAL。


2026-10-25 NPC原型別免疫與失敗階段續作：`283a56445b9a…`同原row新增2個immutable出生旗標，原record identity及Snapshot記憶體還原留原；8個原phase ports涵蓋集體緩速／風枷／自然聖光與魔力吸取選定路徑。2537 inverse／266 no-init／37新＋同SHA1560純契約／4608 exact Source比較及凍結同位元重建，73 Source檔／133 bounded參考，20 SQL class保留。原PC／Ctrl／競技／成本重播／RNG／HP／生命／鎖／WORLD留原；真bound cost／RNG／HP／life／role／IO／Main／MySQL／game0。單體緩速／消魔早期RNG、mass-slow self-primary、其餘root／full factory-AI／58材料／完整Source與正式隔離合成未完成。全部程式完成後集中驗收；未部署／不連MySQL／不寫Source或玩家SQL／schema／WAL。本輪目標服務拒絕新目標，舊目標仍active，沒有虛報complete。


2026-10-25 單體負面技能材料接受與機率階段續作：`3d3a92463f27…`固定Source材料／動作先接受，runSkill後才扣HP／MP。29／152／153兩個原完整RNG區塊只在已證Source BATTLE移至原admit與cancelMeditation後、MP debit前，boolean local凍結分支；WORLD／unmarked原位置保留。6原flags接已知Quest-Time出生型別／IsErase，失敗不擲機率也不消目標狀態。原受控失敗／PC／Ctrl／競技／fixture／公式／成本回滾／鎖／生命／Main保留。2468 inverse／267 no-init／23新＋同SHA1597純契約／4608繼承Source比較與同位元重建，73 Source檔／133 bounded參考、20 SQL class保留；真bound material／admission／RNG／HP／life／role／IO／Main／DB／game0。完整Source／mass-slow self-primary／其餘技能／factory-AI／外控／正式隔離合成仍開放，程式完成後集中驗收；未部署、不連MySQL、不寫Source／玩家SQL／schema／WAL。


2026-10-25 集體緩速自選主目標與範圍清單續作：`dc4679627487…`3原ports讓已證BATTLE的完整self-PC或原owned-controlled primary保留已選計算清單，不再全部empty。已選self-PC primary置前、secondary與重複維持原順序；缺席caster不新增，替換Npc record不採認。原PC／Ctrl／競技、phase／HP／invisibility／radius、NPC／controlled class failure、成本／RNG／成對鎖／生命／Main／WORLD保留。2496 inverse／268 no-init／26新＋同SHA1620純契約／4608繼承Source比較與凍結重建，73 Source檔／133 bounded參考、20 SQL class保留。self-specific Source oracle不覆蓋完整visibility／早期class-zone-ghost／主次目標，其他技能／factory-AI／外控／正式隔離合成仍開放；真bound material／RNG／mutation／HP／life／role／IO／Main／DB／game0。程式完成後集中驗收，未部署、不連MySQL、不寫Source／玩家SQL／schema／WAL。


2026-10-25 Source none目標原施法者投影續作：`21a4115fb74b…`45個原typed attack-request parser與4原post-Context／pre-Aim request沿原caster Binding／earlier Member pose，只改Source BATTLE的target=none；row／column／skill及原封包／Ctrl／指定目標不變，missing／closed／WORLD／unmarked不採新角色或生命。2556 inverse／269 no-init／22新＋同SHA1646純契約／4608繼承Source比較及同位元重建，73 Source檔／133 bounded參考、20 SQL class保留。固定Source PC empty target list仍可能消耗，NPC class veto須保原成本，不能前置免費return；其他封包kind／root自施header／geometry／class-ghost-zone-hidden-corpse／factory-AI／外控／正式隔離合成仍未完，真bound cast／selection／cost／RNG／HP／life／role／IO／Main／DB／game0。程式完成後集中驗收，未部署、不連MySQL、不寫Source／玩家SQL／schema／WAL。


2026-10-25 Source物件封包角色與原物品ID分流續作：`d4ba8178361f…`30原typed object parser投影none角色目標，2原object／1 Blind captured pre-Aim解析沿原caster證明；固定Source5個_itemobjid selector（73／100／21／12／107）保留原始物品ID，獨立Source none＋raw item／bookmark switch oracle通過。原row／column／skill／封包／一般目標／Ctrl／競技／消耗／RNG／生命／鎖／Main／WORLD保留，不新採角色／生命或inventory ownership。2533 inverse／270 no-init／39新＋同SHA1668純契約／4608繼承Source比較與凍結重建，73 Source檔／133 bounded參考、20 SQL class保留。75 parser ports不是75技能／58材料完成；其他packet kinds／root自施與geometry／NPC class-empty-PC成本／完整Source／factory-AI／外控／正式隔離合成仍開放。真bound inventory／cast／cost／RNG／HP／life／role／IO／Main／DB／game0，程式完成後集中驗收，未部署、不連MySQL、不寫Source／玩家SQL／schema／WAL。


2026-10-25 範圍計算目標Source純類別排除續作：`0298847230bb…`4原area roots／12原probability＋d100 target iterator接已知Source純類別排除，原accepted material/action支配入口（含exception CFG），audience-only iterator不改。Doll／unbreakable Door／primary natural-vs-PC-Pet-Summon／Guard／none-attack8類及tame-zombie-restore純類別比較91476組。原Snapshot birth proof須一致，legacy／同ID replacement不採新class；Sleep wrapper同物件、原order／duplicates保留。2468 inverse／272 no-init／30新＋同SHA1707純契約與同位元重建，73 Source檔／133 bounded參考、20 SQL class保留；原Ctrl／競技／成本／RNG／HP／生命／鎖／Main／WORLD留原，PC empty list不免消耗。這是class-only子集，不代表完整isTarget或全部roots，真bound cost／RNG／mutation／HP／life／role／IO／Main／DB／game0；完整Source／factory-AI／58材料／外控／正式隔離合成未完，程式完成後集中驗收，未部署、不連MySQL、不寫Source／玩家SQL／schema／WAL。


2026-10-25 傷害與治癒原計算清單類別排除續作：`fc67db5a0fa4…`4原damage／healing roots／13原post-MP per-target roll iterator用已知Source純類別排除（含exception CFG），原audience／HP-application／FIFO不動。原centered／targeted wrapper沿既有Snapshot與birth proof，legacy／同kind替換不借新class，已知Pet／Summon進化沿固定class。2 none資料ports（原targeted body request與原Action constructor primary）同用原caster證明，避免原NPC gate攔自施；原physical／pointed／args／constructor／discovery／lock順序保留。2504 inverse／274 no-init／33新＋同SHA1737純契約及91476繼承class-only比較與同位元重建，73 Source檔／133 bounded參考、20 SQL class保留。原Ctrl／競技／成本／RNG／HP／生命／Main／WORLD不變，真bound cost／RNG／mutation／HP／life／role／IO／Main／DB／game0；單體／guardian-tower／bot／完整Source／factory-AI／58材料／外控／正式隔離合成未完，程式完成後集中驗收，未部署、不連MySQL、不寫Source／玩家SQL／schema／WAL。


2026-10-25 Source LOS type／through原資料接線續作：候選`b1282be6835f524fbfcf6516c7fe753cb8744b5a45f083613556d9915a478af9`，19原技能入口33個LOS呼叫＋材料Aim through＋致盲BATTLE幾何，共35點；原成本／RNG／Ctrl／競技／HP／鎖／生命／Main／WORLD inverse。2493方法inverse、275 no-init、22新＋1770同SHA繼承純契約、2080固定Source條件比較與同位元重建通過。73 Source檔／133參考／20 SQL class保留。proc僅純條件比較，上游與未具名群補lambda、完整isTarget／58材料／外控／NPC-AI及實際角色生命／Main／DB／game仍開放；先更新程式再集中驗收，未部署。證據：[20261025T074018412574Z](D:/lineage開發/3-lito開發/lito伺服器/LiTo381-Server/tools/combat-continuation/integration-lifecycle/build/source-completion-progress/20261025T074018412574Z/manifest.json)。


2026-10-25 群補原predicate技能資料續作：候選`d0aae13f37885e04af37fac204b6f08189115a405ec0f6f7d9706e10f29777e5`，兩個原stream factory補原caster／model，兩個有界原lambda sibling，保留PC／Snapshot原位置及WORLD幾何。2468原方法inverse、275 no-init、27新＋1792同SHA繼承純契約、同位元重建通過；76 Source檔／133參考／20 SQL class保留。三檔固定Source proc分流另存，不將娃娃直接L1Magic誤套正常施法LOS；魔法石／裝備proc與其餘完整Source／58材料／生命／Main／DB／game仍開放。先更新程式再集中驗收，未部署。證據：[20261025T074851504117Z](D:/lineage開發/3-lito開發/lito伺服器/LiTo381-Server/tools/combat-continuation/integration-lifecycle/build/source-completion-progress/20261025T074851504117Z/manifest.json)。


2026-10-25 裝備proc原接受生命續作：候選`cd211fc6d7970d2a3c29a4906fb682236332e9b6c1dec0d3adffff3f3b7d6b39`，玩家補血與68原入口／RNG前／vital內／完成／graphics／status supplier共10點借原accepted participants。巢狀缺鎖先tryLock，拒絕前不開scope；不採新角色／生命，DRAINING保留、STOPPED／Main關閉拒絕。23原方法inverse、277 no-init、37新＋1819同SHA繼承純契約及同位元重建通過；76 Source檔／133參考／20 SQL class保留。其他shared scope競爭、完整equipment／stone／weapon proc及全Source／58材料／實際HP／最終IO／Main／DB／game仍開放。未部署。證據：[20261025T081200626747Z](D:/lineage開發/3-lito開發/lito伺服器/LiTo381-Server/tools/combat-continuation/integration-lifecycle/build/source-completion-progress/20261025T081200626747Z/manifest.json)。


2026-10-25 裝備補血原接受輸出續作：候選`ecf2e94ead845b5d1fae387bf9d37d64fd0bf717293c59b9a78eba8300013fb9`，原snapshot／HP後seal／完成／Hub觀眾／capture／final receiver共12點。HP封包只保留原目標，後續caster退役獨立；gfx保留原actor控制；observer保留原角色接收權。排隊值無Frame／Context／觀眾表／calculator；最後原receiver monitor驗packet／id／HP／MAX及生命／Main／STOPPED，DRAINING保留。2487 inverse、288 no-init、62新＋1856同SHA繼承純契約與同位元重建通過；76 Source檔／133既有參考／20 SQL class。僅一個實際unbound負向frame案，正向bound IO／角色／Main與全Source／58材料／其餘proc仍開放，未部署。證據：[20261025T085910438349Z](D:/lineage開發/3-lito開發/lito伺服器/LiTo381-Server/tools/combat-continuation/integration-lifecycle/build/source-completion-progress/20261025T085910438349Z/manifest.json)。


2026-10-25 共用成對鎖續作：候選`c9956026448f4f2e2588f730c47c36af86948c80052709e9db3c3753300f2925`，loaded BATTLE 巢狀競爭改在原 scope 前 tryLock；拒絕不執行 body／加入 peer，成功保留完整原方法、釋放實際同一把鎖與原 unlocked。5 原方法 inverse、290 no-init、31 新＋1918 同 SHA 繼承純契約與同位元重建；5 直接 caller 只定位，真 bound HP／角色／IO／Main／DB／game 均0。其他 receipt／鎖路徑及完整58材料／252外控／Source／NPC-AI／正式合成仍開放。未部署。證據：[20261025T092026726883Z](D:/lineage開發/3-lito開發/lito伺服器/LiTo381-Server/tools/combat-continuation/integration-lifecycle/build/source-completion-progress/20261025T092026726883Z/manifest.json)。


2026-10-25 共用commitPair生命接線：候選`6ca741c7c1e4ab7c57b465c7013faa80a2f92acefdf02cc4cf7c400e13344182`，原receipt／鎖等待前固定Task雙方角色生命；借canonical accepted或既有discovery，缺少舊角色不刷新，保留corpse／復活／退役原權限。47 inverse、293 no-init、48新＋1949同SHA繼承純契約與同位元重建；8個隔離原入口案確認receipt轉交／清理／雙向競爭／鎖等待換生命。68上游edge只定位，真bound HP／角色／IO／Main／DB／game0，完整Source與6處歷史索引仍開放，未部署。證據：[20261025T093540925672Z](D:/lineage開發/3-lito開發/lito伺服器/LiTo381-Server/tools/combat-continuation/integration-lifecycle/build/source-completion-progress/20261025T093540925672Z/manifest.json)。


2026-10-25 兩處pre-commit outer pair：候選`b2a3858d165556abac884aa2964c6de4349214312fd370a8de5a529de62b419a`，娃娃enter／nonnull奇古獸Aim owned各一個原呼叫接線；鎖競爭先於discovery／scope拒絕，成功保留first-life與原ordered scope／OPEN／成本／RNG／HP／Ctrl／WORLD，parent scope管理完成排空。33 inverse、294 no-init、55新＋1997同SHA繼承純契約與同位元重建；4個隔離Aim負向入口，正向bound施放／HP／IO／Main／DB／game0。全域pair與已接受回呼class byte-retained；廣泛wrapper原型因未證明accepted語意而未升候選。其餘accepted continuation／手動鎖／tie monitor／完整Source仍開放，未部署。證據：[20261025T095613610612Z](D:/lineage開發/3-lito開發/lito伺服器/LiTo381-Server/tools/combat-continuation/integration-lifecycle/build/source-completion-progress/20261025T095613610612Z/manifest.json)。


2026-10-25 娃娃29 Source wire續作：候選`c986a17d58edb363f7a2b13ee46da4f41ad64315752521d4086daeda4c046edc`，原緩速packet改所有receiver均保留原buffDuration writeH；最終hook核對捕捉完整bytes與原角色／生命／status epoch／Main，不重寫私人countdown。11 inverse、296 no-init、28新＋2092同SHA繼承／重驗純契約與192精確Source payload比較、同位元重建；79 Source檔／133既有bounded參考與新增packet契約／20 SQL class。底層0／long／int-overflow timer既有行為保留；自然NPC innate speed／AI、全Source與真bound IO／Main／DB／game仍開放，未部署。證據：[20261025T101748011379Z](D:/lineage開發/3-lito開發/lito伺服器/LiTo381-Server/tools/combat-continuation/integration-lifecycle/build/source-completion-progress/20261025T101748011379Z/manifest.json)。


2026-10-25 Source target_to cursor續作：候選`7e584e8d1bae702aab48c62180010722c7b2398c2f2637eeee03814f5c354265`，機率與傷害／補血原Row與predicate共4點，兩個final Integer／ctor sibling固定targetTo與原type／none，重載不混用；原ctor與generic ABI保留。PC／NPC／own-Pet／self／HEAL_ALL投影在原成本後、RNG前，保留既有關係／競技／Ctrl／生命／WORLD。17 inverse、297 no-init、29新＋2122同SHA繼承／重驗檢查與30720精確Source投影、同位元重建；修正1個舊class-only攻擊self預期。79 Source檔／133既有參考與新增projection契約／20 SQL class。完整isTarget與全Source、真bound選取／HP／IO／Main／DB／game仍開放，未部署。證據：[20261025T105205046721Z](D:/lineage開發/3-lito開發/lito伺服器/LiTo381-Server/tools/combat-continuation/integration-lifecycle/build/source-completion-progress/20261025T105205046721Z/manifest.json)。


2026-10-25 Source目標建構優先序續作：候選`3014ae5e6e7a0ce3e85b50f26f6a900318e313b9c275dc515e63cd2fbd274a13`，原Row／cursor共4點保留TARGET_TO_ME與非攻擊area明確caster規則，再進入既有class-mask判定；沿用scalar metadata，不新增目標、欄位、角色或生命擷取。19 inverse、298 no-init、22新＋2151同SHA繼承／重驗與16128精確Source分支、同位元重建。79 Source檔／133既有參考與新增建構契約／20 SQL class保留。完整makeTargetList／isTarget／全Source與真bound目標／HP／IO／Main／DB／game仍開放，未部署。證據：[20261025T110804414277Z](D:/lineage開發/3-lito開發/lito伺服器/LiTo381-Server/tools/combat-continuation/integration-lifecycle/build/source-completion-progress/20261025T110804414277Z/manifest.json)。


2026-10-25 Source範圍caster清單續作：候選`c302d8f7942d158f975942cdc5d1177c4b8d26cc585eaa47e394cce0e1e28d17`，Silence／MassSlow原PC清單在成本前依Source條件補原caster，保留首次角色／生命／位置及原動作鎖；兩個局部唯讀carrier固定Row／skill，含空表重載與中間iterator，明確PC主目標先於caster。2,481 inverse、301 no-init、39新＋2173同SHA繼承／重驗、16128精確Source分支比較與同位元重建；79 Source檔／133既有參考及新契約／20 SQL class保留。完整目標建構／其餘root／全Source與真bound目標／HP／IO／Main／DB／game仍開放，未部署。證據：[20261025T113217814824Z](D:/lineage開發/3-lito開發/lito伺服器/LiTo381-Server/tools/combat-continuation/integration-lifecycle/build/source-completion-progress/20261025T113217814824Z/manifest.json)。


2026-10-25 Source Sleep建構續作：候選`463b7dbe0c6a08dc5a391d775d679dac5a0439a08863b88bcdbe6211fe0573cd`，原root11點、兩個原factory4點與1個scalar record factory，固定首次source Member／Row／skill，補self-primary／ME-only跳過NPC列舉／mixed主目標與caster順序；兩個identity.player self競技點保留其他玩家與companionOwner規則。2,481正規化instruction／control-flow inverse、stack-map正向與JVM核對、305 no-init、37新＋2212同SHA繼承／重驗、4032精確Source分支與同位元重建；79 Source檔／133既有參考及新契約／20 SQL class保留。完整Source狀態權限／其餘root與真bound施法／HP／IO／Main／DB／game仍開放，未部署。證據：[20261025T121202983299Z](D:/lineage開發/3-lito開發/lito伺服器/LiTo381-Server/tools/combat-continuation/integration-lifecycle/build/source-completion-progress/20261025T121202983299Z/manifest.json)。


2026-10-25 其餘範圍PC／NPC計算Row續作：候選`4d58eaf524729e8a4eb1ed9ee141476cfc540d54cbc2bc9bde87c8fc91312094`，Group／Nature／targeted／centered原PC清單4點，加14個非PC iterator跨6條root共用原Row／skill；自然祝福保留可變LinkedHashSet及iterator remove，空PC的NPC-only anchor有效。2,482正規化inverse、stack-map正向／JVM、309 no-init、32新＋2249同SHA繼承／重驗及同位元重建；79 Source檔／133既有參考與新契約／20 SQL class保留。完整前段選取／失敗／幾何與後段數值狀態／proc、全Source及真bound施法／HP／IO／Main／DB／game仍開放，未部署。證據：[20261025T124906411597Z](D:/lineage開發/3-lito開發/lito伺服器/LiTo381-Server/tools/combat-continuation/integration-lifecycle/build/source-completion-progress/20261025T124906411597Z/manifest.json)。


2026-10-25 Source76 NPC失敗／距離metadata續作：候選`a51a6635161fa2e182e0b15d1c709947a07771806c79639307458c19e3f9ef05`，原root7點＋2個完整幾何lambda sibling沿用原PC anchor Row，補選取／NPC filter／accepted顯示與距離，保留NPC出生／IsErase／權限。2,471正規化原方法＋2完整sibling inverse、stack-map／JVM、310 no-init、40新＋2281同SHA繼承／重驗、99原生距離比較、實際private lambda連結及同位元重建；79 Source檔／133既有參考與新契約／20 SQL class。完整前段主目標／PC選取與其他range／LOS／數值狀態／proc、全Source及真bound施法／HP／IO／Main／DB／game仍開放，未部署。證據：[20261025T131103081352Z](D:/lineage開發/3-lito開發/lito伺服器/LiTo381-Server/tools/combat-continuation/integration-lifecycle/build/source-completion-progress/20261025T131103081352Z/manifest.json)。


2026-10-25 Source NPC勇敢／AI排程續作：候選`7e741fa67732667210c50435ff2733cf0c54313b9f8dab97784b79acdd6269bd`。出生不可變brave與八個原排程點接線，保留MP／reuse／count／nextAction；brave與haste分開。185原方法inverse＋1委派constructor、311 no-init、13新＋2321同SHA重驗、1200 Source公式／192原方法隔離排程與同位元重建；80 Source檔／133 bounded參考及新增契約／20 SQL classes。完整base／status／工廠AI／顯示及全Source與真bound驗收仍開放。使用者重設範圍已存本地，系統工具因舊目標仍啟用拒絕重建，未虛報完成；未部署。證據：[20261025T133928272694Z](D:/lineage開發/3-lito開發/lito伺服器/LiTo381-Server/tools/combat-continuation/integration-lifecycle/build/source-completion-progress/20261025T133928272694Z/manifest.json)。


2026-10-25 Source NPC移動／攻擊基底續作：候選`e24fb30dccf9c93d8bdb8e128f0541751a72ce71cc3bba5ddedf57194eafe7cc`，原出生凍結passispeed／atkspeed，2個原normal排程採Source模板基底／既有倍率，保留MP／nextAction／生命週期與原記憶體proof。186原方法inverse＋1委派constructor、312 no-init、13新＋2334同SHA重驗、960原方法排程／32舊後備比較與同位元重建；81 Source檔／133 bounded參考及新契約／20 SQL classes。上游zero-speed准入／custom-work／magic基底／完整AI顯示及全Source與bound驗收仍開放，未部署。證據：[20261025T134858109597Z](D:/lineage開發/3-lito開發/lito伺服器/LiTo381-Server/tools/combat-continuation/integration-lifecycle/build/source-completion-progress/20261025T134858109597Z/manifest.json)。


2026-10-25 Source NPC魔法基底／mobskill路由續作：候選`af20195916500212aa4a0077ad021dd9d3040a3de87f5f52dac2d67c8ddf0ed4`，原出生不可變兩魔法基底／actNo路由，6個原排程點採精確area／target attack／18例外／summon選擇，保留MP／reuse／count／nextAction／原生命週期與記憶體proof。187原方法inverse＋1constructor、315 no-init、24新＋2347同SHA重驗、9000 Source選擇／1440原方法排程與同位元重建；83 Source檔／133 bounded參考及新契約／20 SQL classes。Source脫逃／custom-work／原trigger施法／完整AI顯示與全Source及bound驗收仍開放，未部署。證據：[20261025T140634587106Z](D:/lineage開發/3-lito開發/lito伺服器/LiTo381-Server/tools/combat-continuation/integration-lifecycle/build/source-completion-progress/20261025T140634587106Z/manifest.json)。


2026-10-25 Source NPC魔法基底／mobskill路由續作：候選`3ef57c70a0478e96562e69a6094831ba44e6198054c66553d9dd0816ea44a3c7`，原出生不可變兩魔法基底／actNo路由，6個原排程點採精確area／target attack／18例外／summon選擇，保留MP／reuse／count／nextAction／原生命週期與記憶體proof。已移除初稿對skills.id等於skill_id的錯誤額外門檻，保留原mobskill身分檢查。187原方法inverse＋1constructor、315 no-init、24新＋2347同SHA重驗、9000 Source選擇／1440原方法排程與同位元重建；83 Source檔／133 bounded參考及新契約／20 SQL classes。Source脫逃／custom-work／原trigger施法／完整AI顯示與全Source及bound驗收仍開放，未部署。證據：[20261025T141037700339Z](D:/lineage開發/3-lito開發/lito伺服器/LiTo381-Server/tools/combat-continuation/integration-lifecycle/build/source-completion-progress/20261025T141037700339Z/manifest.json)。


2026-10-25 Source NPC正速度移動准入續作：候選`46469a0946278df41a1e3347d3c65b2c4043463760fb08e8c6ad0e2077cdeb9d`，2個原moveForAi ready資料點按既有出生passispeed>0縮限；原geometry／collision／status／position／MP／nextAction／snapshot／attack／RNG／life／Main保留。316原方法inverse、316 no-init、11新＋2371同SHA重驗、900 Source比較／50原方法隔離呼叫及同位元重建；86 Source檔／133 bounded參考及新契約／20 SQL classes。完整ISASCAPE／heading／reverse／memory、AI與全Source及bound驗收仍開放，未部署。證據：[20261025T142932384658Z](D:/lineage開發/3-lito開發/lito伺服器/LiTo381-Server/tools/combat-continuation/integration-lifecycle/build/source-completion-progress/20261025T142932384658Z/manifest.json)。


2026-10-25 Source NPC原階段motion／memory續作：候選`7be148890418170db7cb0fd1b1ad454a901c040ff983a3f166b623f9d53fb63c`，2個actor-owned atomic transient motion slots，原移動／物理／target階段與7個既有memory資料點攜帶heading／sticky escaped；不改原決策／RNG／life／Main與Snapshot canonical schema，不promote companion。487原方法inverse、319 no-init、36新＋2382同SHA重驗、32768方向／1568sticky／24原動作、restore／release-tamed／legacy／invalid與同位元重建；86 Source檔／133 bounded參考及新契約／20 SQL classes。實際逃跑路由／spawn heading／最終IO、全Source與bound驗收仍開放，未部署。證據：[20261025T145449004332Z](D:/lineage開發/3-lito開發/lito伺服器/LiTo381-Server/tools/combat-continuation/integration-lifecycle/build/source-completion-progress/20261025T145449004332Z/manifest.json)。


2026-10-25 Source Monster pre-RNG逃跑路由：候選`b1b57557d333ead5c544cfd626aee7b72c5d8a9921da1300a218d16e13f0bb14`，原Hub post-status／pre-AI hook＋3完整move／occupancy sibling，保留原geometry／status／IO並分開門階段。footprint與constructor origin capture在encounter鎖外，回鎖比對actor／stamp／life／generation／motion／pose／arena。1318原方法＋3 sibling inverse、320 no-init、50新＋2418同SHA、648 reverse／2048 checkObject／40隔離呼叫、2private Hub與2生命期替換拒絕、no-RNG與同位元重建；86 Source檔／133 bounded參考及新契約／20 SQL classes。最早Snapshot provenance、hate／target-clear／其他class與完整AI／IO／Main及全Source／bound驗收仍開放，未部署。證據：[20261025T152946473178Z](D:/lineage開發/3-lito開發/lito伺服器/LiTo381-Server/tools/combat-continuation/integration-lifecycle/build/source-completion-progress/20261025T152946473178Z/manifest.json)。


2026-10-25 NPC original first-snapshot handoff：候選`3162d1c51d6acba41effb104370af909c29d94c71de50f3689479fce81c521bd`，snapshot entry弱actor／life／motion／stamp／pose＋原locked list owner generation，2個原PVE snapshot call operands經2完整body sibling於首次escape lookup驗證；原monitor cleanup／RNG／IO保留，memory restore建立fresh view。1543原方法＋2 sibling／2精確ctor inverse、322 no-init、48新＋2468同SHA、24handoff／22拒絕與同位元重建；86 Source檔／133 bounded參考及新契約／20 SQL classes。其他AI／PC target／final IO／Main與全Source及bound驗收仍開放，未部署。證據：[20261025T155559009741Z](D:/lineage開發/3-lito開發/lito伺服器/LiTo381-Server/tools/combat-continuation/integration-lifecycle/build/source-completion-progress/20261025T155559009741Z/manifest.json)。


2026-10-26 NPC first-view AI／move：候選`9b87fcfe0852fa57232974383f6b8f2b0234799d8fc61579fa9ea08cad46cd7c`，原快照傳至5 AI／2 move完整同步入口，在原RNG前驗證且原monitor涵蓋core／排程。1322原方法＋7 sibling inverse、323 no-init、205新＋2516繼承同SHA、97隔離呼叫／84拒絕／7次原鎖等待失效／6種族別決策案例與同位元重建。最終origin／IO、PC target／其他AI與完整Source／Main仍開放。未部署／未連MySQL。證據：[20261025T161234839766Z](D:/lineage開發/3-lito開發/lito伺服器/LiTo381-Server/tools/combat-continuation/integration-lifecycle/build/source-completion-progress/20261025T161234839766Z/manifest.json)。


2026-10-26 NPC first-view tick／Ref：候選`f2298f51a1c60a1af3a504a712331daa0a790d36393913ed5d8e2d0df1aa4dd5`，4原輸出operand→2完整原tick ctor→原attacker encounter capture／immutable Ref／live檢查；既有monitor／cleanup／WORLD與target-owned接受保留，成功動作的pose／motion變更仍允許。1103原方法＋2 ctor／1精確delegate inverse、324 no-init、188新＋2721同SHA繼承、72直接ctor／58拒絕／4原Hub root／2原encounter等待與同位元重建。PC初始發現／accepted-final HP-IO-Main及其他完整Source仍開放；未部署／未連MySQL。證據：[20261025T162545377325Z](D:/lineage開發/3-lito開發/lito伺服器/LiTo381-Server/tools/combat-continuation/integration-lifecycle/build/source-completion-progress/20261025T162545377325Z/manifest.json)。


2026-10-26 NPC first-PC discovery：候選`23059466f9835f2a052cb8dbc72824ce771fafec5d7d40c5b4f4b6a90a70a620`，2原PVE選取→append保留首次life-before-binding，12完整原selection／AI／escape／tick body接線；7原AI／move等待與escape兩次monitor驗證，原tick重用同一Origin／Member，接收名單不擴大。1368原方法＋12 sibling inverse、326 no-init、96新＋2909同SHA繼承、12原root／14索敵比較／7等待／2 footprint失效／14 ctor與同位元重建。完整accepted-final HP-IO-Main、其他AI與Source仍開放；未部署／未連MySQL。證據：[20261025T164234583772Z](D:/lineage開發/3-lito開發/lito伺服器/LiTo381-Server/tools/combat-continuation/integration-lifecycle/build/source-completion-progress/20261025T164234583772Z/manifest.json)。


2026-10-26 NPC accepted HP／HPBAR final receiver：候選`0de39b88bd0f89480a32d330877c2a738d1b02ec6d6be258e66fef7f7ac7caf0`，首次observer／life／binding→既有Plan→原vital-lock接受後target／death seal→原同步final frame，兩HP packet kinds保留原cipher／socket／log及既有Batch；無current NPC／caster存活新依賴。2575原方法＋1完整final frame／精確ctor／Batch bridge inverse、331 no-init、96新＋3005同SHA繼承、6 guarded＋6原路徑密文比對／2原接收鎖等待及同位元重建。其他producer／IO／Main shutdown／完整Source仍開放；未部署／未連MySQL。證據：[20261025T170616584041Z](D:/lineage開發/3-lito開發/lito伺服器/LiTo381-Server/tools/combat-continuation/integration-lifecycle/build/source-completion-progress/20261025T170616584041Z/manifest.json)。


2026-10-26 controlled first-owner clock：候選`6bec50c500189db9e28721f2796ff7718833e1ae574fcdb8c1ee5c9bbaf0a6a3`，原Hub首次主人／audience→CompanionTick→原action／Ref等待／scope→首次selected PC，保留較早全房間防禦快照。3551原方法＋4完整原sibling／1精確ctor inverse、333 no-init、121新＋3101同SHA繼承、24 stale clock／8 ingress／4原Ref等待／8 inner action與同位元重建。受控actor Snapshot→lookup生命期／完整成對鎖定／OPEN Context／HP／IO／Main／完整Source仍開放；未部署／未連MySQL。證據：[20261025T173304167796Z](D:/lineage開發/3-lito開發/lito伺服器/LiTo381-Server/tools/combat-continuation/integration-lifecycle/build/source-completion-progress/20261025T173304167796Z/manifest.json)。


2026-10-26 controlled first Snapshot actor life：候選`94af7185f4fb20a2b1dba5130d223530347dbf924e7bafd51731ae4a86e4341b`，原同步Snapshot弱本體／life→原lookup／action／Ref／locked live，保留同本體expiry／retirement及已admit後正常motion／command。2699原方法＋2完整原sibling／1精確ctor inverse、335 no-init、204新＋3222同SHA繼承、40原action／12原outer等待／4原Ref等待／16原live與同位元重建。完整成對鎖定／OPEN Context／HP／IO／Main／完整Source仍開放；未部署／未連MySQL。證據：[20261025T174719706969Z](D:/lineage開發/3-lito開發/lito伺服器/LiTo381-Server/tools/combat-continuation/integration-lifecycle/build/source-completion-progress/20261025T174719706969Z/manifest.json)。


2026-10-26 explicit new-action pair entry：候選`849e14dbee7a456f9a21805c6b8cf6726a18a0cc2cd26648cc18cf34e83efc0b`，148／149、raw accepted=false及無accepted parent娃娃在scope前處理peer競爭；共用BATTLE已持雙鎖略過tie，accepted／清理保留原同步語意。78原方法inverse、336 no-init、66候選新＋3426同SHA繼承、16競爭／6原raw-owned／4accepted等待；另7項test-only強制tie fixture獨立標記。同位元重建通過；其餘pair phase／完整Source／Main仍開放，未部署／未連MySQL。證據：[20261025T180626116375Z](D:/lineage開發/3-lito開發/lito伺服器/LiTo381-Server/tools/combat-continuation/integration-lifecycle/build/source-completion-progress/20261025T180626116375Z/manifest.json)。


2026-10-26 parent third-actor pair：候選`2813508eed2a066aa36cb7d7f663701330bba1493c6dd1b5fb573aa048e9e22d`，原parent fence／實際第三持鎖→三個原entry body，partial rollback不改先前scope名單／順序；accepted共用／outer仍走原等待。19原方法含3完整entry inverse＋3thunk／1唯讀bridge、337 no-init、203新＋3492同SHA繼承、15拒絕／9原等待／15原body／3三角色receipt／3預持鎖policy與同位元重建。其他pair phase／未登記手動鎖／完整Source／Main仍開放；未部署／未連MySQL。證據：[20261025T182415613307Z](D:/lineage開發/3-lito開發/lito伺服器/LiTo381-Server/tools/combat-continuation/integration-lifecycle/build/source-completion-progress/20261025T182415613307Z/manifest.json)。


2026-10-26 exact manual critical spans：候選`2051dc0adcd38b443ac19b0a1597c583f9ee5c557b06c16493cc38497bd161ce`，11 state／respawn＋7 role／token span沿用原Ticket至43原解鎖點；無整段cast scope或新registry。2637原方法inverse、340 no-init、57新＋3695同SHA繼承、9選定state／7scalar／2手動第三配對競爭／1原三角色receipt及同位元重建通過。正常角色切換／其他accepted phase／完整Source／Main仍開放；未部署／未連MySQL。證據：[20261025T184356120641Z](D:/lineage開發/3-lito開發/lito伺服器/LiTo381-Server/tools/combat-continuation/integration-lifecycle/build/source-completion-progress/20261025T184356120641Z/manifest.json)。


2026-10-26 detached doll output：候選`332c4127670480eac913733ad33271fb4b40d33e015e6a7a60c3549e52fed327`，原Task不再持有Context；完整原同步final body在原娃娃guard接收明確Output。2489原方法inverse、341 no-init、135新＋3752同SHA繼承、3加密IO／原路徑比對／接收鎖等待、1原producer與同位元重建通過。Pending召喚證明保留。完整Source／Main／MySQL／實機仍開放；未部署。證據：[20261025T190837058808Z](D:/lineage開發/3-lito開發/lito伺服器/LiTo381-Server/tools/combat-continuation/integration-lifecycle/build/source-completion-progress/20261025T190837058808Z/manifest.json)。


2026-10-26 first doll audience：候選`5b6f3b25528280577a5546ae5c4c4cbcbe0d4a850a8e93e3411848ee31604dd4`，原觀眾角色／生命／observer證明沿用至Snapshot與最終同步接收者；同事件既有accepted死亡seal與後續復活分開處理。31原方法／6port／1ctor inverse、344 no-init、40新＋3887同SHA繼承、2View／2Snapshot／2加密IO／3接收鎖等待／1accepted seal與同位元重建通過。完整Source Context／HP／Main／MySQL／實機仍開放；未部署。證據：[20261025T192323397204Z](D:/lineage開發/3-lito開發/lito伺服器/LiTo381-Server/tools/combat-continuation/integration-lifecycle/build/source-completion-progress/20261025T192323397204Z/manifest.json)。


2026-10-26 first defense audience：候選`71ee218ef5d8e29aade765b885eb21f38c0eacef3915da07738a862f07f795ed`，防禦原Receiver沿用首次角色／生命／observer與精確封包；原defender既有accepted死亡seal保留。17原方法／3port／1ctor inverse、345 no-init、123新＋3927同SHA繼承、3Snapshot／3visual ctor／8加密IO／6原路徑比對／6接收鎖等待／2致死seal與同位元重建通過。完整Calc／HP／Source／Main／MySQL／實機仍開放；未部署。證據：[20261025T193752224780Z](D:/lineage開發/3-lito開發/lito伺服器/LiTo381-Server/tools/combat-continuation/integration-lifecycle/build/source-completion-progress/20261025T193752224780Z/manifest.json)。


2026-10-26 Source skill defaults：候選`58c620c55339ee4089674e5e999a95fb8b1f4e7d98f5f9eeeb5fce595d9966b7`，31已接線技能外控的缺值／留白依固定Source預設，原validation／publication與數值限制保留。26原方法＋1完整snapshot inverse、346 no-init、321新＋4050同SHA繼承、31缺值／62空白／11原拒絕／3201 Source值比較及同位元重建通過。完整Source／啟動／Main／MySQL／實機仍開放；未部署。證據：[20261025T195350925694Z](D:/lineage開發/3-lito開發/lito伺服器/LiTo381-Server/tools/combat-continuation/integration-lifecycle/build/source-completion-progress/20261025T195350925694Z/manifest.json)。


2026-10-26 Source barrier target：候選`68235a6cca75f60e29165c52afeb0e521f6ca3d32d8f3df62e7ef49f9bf71817`，原PC絕對屏障short return改為Source22項清單，原自施／ME／area-caster捷徑保留。4原方法／1return inverse、347 no-init、1032新＋4371同SHA繼承、811 scalar／14598型別遮罩／5原cursor及同位元重建通過。完整施法／Proc／cost／HP／Main／MySQL／實機仍開放；未部署。證據：[20261025T201431324749Z](D:/lineage開發/3-lito開發/lito伺服器/LiTo381-Server/tools/combat-continuation/integration-lifecycle/build/source-completion-progress/20261025T201431324749Z/manifest.json)。


2026-10-26 equipment final output：候選`1addf015c70c85e10be4d01204edb7996ca35b5fcacbfb04082842f6f762614a`，原viewer沿用weak container／observer／life，傳送不再new Context；原同步frame與已接受HP／caster分離保留。2514原方法／4port／1ctor／1final frame inverse、349 no-init、81新＋5404同SHA繼承檢查（含1項新增Viewer欄位巡檢）、6加密IO／4原路徑比對／5接收鎖等待／1原capture與同位元重建通過。完整角色／observer ABA／Main／Source／MySQL／實機仍開放；未部署。證據：[20261025T205237040162Z](D:/lineage開發/3-lito開發/lito伺服器/LiTo381-Server/tools/combat-continuation/integration-lifecycle/build/source-completion-progress/20261025T205237040162Z/manifest.json)。


2026-10-26 Raw audience：候選`917f33c0c0f7311428695c9615f3eba45fbd4da8b15a5c20fc264e16d9384d08`，原captureOutput固定首次receiver life／observer，僅借原accepted目標death seal；原final guard／Batch／同步接收與HP／cost／RNG保留。44原方法／2port／1ctor inverse、351 no-init、77新＋5485同SHA繼承、4加密IO／4接收鎖等待／10原capture與同位元重建通過。完整Source／生產端／Main／memory／MySQL／實機仍開放；未部署。證據：[20261025T210753855244Z](D:/lineage開發/3-lito開發/lito伺服器/LiTo381-Server/tools/combat-continuation/integration-lifecycle/build/source-completion-progress/20261025T210753855244Z/manifest.json)。


2026-10-26 natural NPC audience：候選`e71cccb8c62b858657f277a16fd906241ff7838f04b3bb169532f33f5c44da43`，原Aim首次binding／life／observer經Commit到shared receiver，阻止較晚建構採用角色／生命替換；原HP／cost／RNG／queue／Batch／final frame保留。65原方法／2port／1ctor inverse、354 no-init、99新＋5562同SHA繼承、12原ctor／8seal／8capture／4加密IO／4接收鎖等待與同位元重建通過。未執行late Task Context；完整Source／生產端／Main／memory／MySQL／實機仍開放；未部署。證據：[20261025T211928711153Z](D:/lineage開發/3-lito開發/lito伺服器/LiTo381-Server/tools/combat-continuation/integration-lifecycle/build/source-completion-progress/20261025T211928711153Z/manifest.json)。


2026-10-26 natural NPC transport：候選`e057943bd93d271968c62a56129c93e2289cd789342ba095f9170438a28d384a`，原Task改用proof-only Context，不重新搜尋角色／NPC／Source或建立life；11原inherit、既有parent snapshot、原Task／Batch／owned／monitor／cipher／cleanup保留。71原方法／1port／1ctor inverse、354 no-init、264新＋5661同SHA繼承、24capture／18加密IO／16原路徑比對／4等待／4繼承變體／2IO失敗／12封包案例與同位元重建通過。完整Source／生產端／Main／memory／MySQL／實機仍開放；未部署。證據：[20261025T214226576419Z](D:/lineage開發/3-lito開發/lito伺服器/LiTo381-Server/tools/combat-continuation/integration-lifecycle/build/source-completion-progress/20261025T214226576419Z/manifest.json)。


2026-10-26 cube/poison transport：候選`2b4a379e25f3778ecf2b7771f5bc44d60cc19123a6ca3aeddf06ba9cfc8b5d89`，三個送包factory沿用既有proof，status Task固定16個capture-time guard再執行完整原body。121原方法inverse、356 no-init、251新＋5925同SHA繼承、28HP output／36加密IO／28原路徑比對／16capture／8等待／6IO失敗與同位元重建通過。首次觀眾生命、完整Source／Main／memory／MySQL／實機仍開放；未部署。證據：[20261026T084936561269Z](D:/lineage開發/3-lito開發/lito伺服器/LiTo381-Server/tools/combat-continuation/integration-lifecycle/build/source-completion-progress/20261026T084936561269Z/manifest.json)。


2026-10-26 cube/poison HP first audience：候選`5fee10f02b6e9b8106f68a7be6043a2f4a5a86cdcdc00c06e1bf72a2aeddb1eb`，原首次名單固定weak status／binding／life／observer，兩個shared receiver核對first proof；原schema與HP／cost／RNG／queue／final body保留。118原方法inverse、359 no-init、147新＋6176同SHA繼承、8constructor／8加密IO／20final等待／4discovery等待與同位元重建通過。status first viewer與完整Source／Main／memory／MySQL／實機仍開放；未部署。證據：[20261026T090327515678Z](D:/lineage開發/3-lito開發/lito伺服器/LiTo381-Server/tools/combat-continuation/integration-lifecycle/build/source-completion-progress/20261026T090327515678Z/manifest.json)。


2026-10-26 poison status first audience：候選`3c046fc33e3efae2fceece357814f9ad4c1c1a7d903c0c0157d1a0d508dfbc32`，原stamp固定first receiver，只在弱Viewer保留個別immutable proof；原arena Output／NPC Status兩個final guard核對first身分。85原方法inverse、360 no-init、94新＋6323同SHA繼承、4stamp／18capture／6加密IO／10final等待／2discovery等待與同位元重建通過。完整Source／HP生產端／Main／memory／MySQL／實機仍開放；未部署。證據：[20261026T091435850698Z](D:/lineage開發/3-lito開發/lito伺服器/LiTo381-Server/tools/combat-continuation/integration-lifecycle/build/source-completion-progress/20261026T091435850698Z/manifest.json)。


2026-10-26 controlled first receiver：候選`82cbc12966c95d9c0d75182d6f70a172014384c2a98d63926899c66e7de28c16`，原Output借用已seal first member，final guard核對receiver／packet／life／mode；原Action／Target／caster impact／screen／適用性／HP／cost／RNG／close保留。46原方法inverse、362 no-init、267新＋6417同SHA繼承、8Action／54capture／36加密IO／24等待／6原死亡seal與同位元重建通過。原Action／Context持有、實際Source／Main／memory／MySQL／實機仍開放；未部署。證據：[20261026T093035402581Z](D:/lineage開發/3-lito開發/lito伺服器/LiTo381-Server/tools/combat-continuation/integration-lifecycle/build/source-completion-progress/20261026T093035402581Z/manifest.json)。


2026-10-26 controlled readonly role：候選`e301ce19b718733c64f6382f5eb8588cfa43496d32ecb2416393e567f2fc4411`，原queued Task使用原status鎖內唯讀roleFrame，不建立失效角色／lifecycle／status。36原方法inverse、363 no-init、478新＋6684同SHA繼承、150matcher比對／36capture／12加密IO／12status等待／6IO失敗與同位元重建通過。Batch終止參照釋放及完整Source／Main／memory／MySQL／實機仍開放；未部署。證據：[20261026T094733469863Z](D:/lineage開發/3-lito開發/lito伺服器/LiTo381-Server/tools/combat-continuation/integration-lifecycle/build/source-completion-progress/20261026T094733469863Z/manifest.json)。


2026-10-26 Batch terminal：候選`8a5ad63ac92916835315e12f2f79a0f6abd5f326c8e955e63fe81f7883b88d81`，原emit／rescue／completion終止後釋放兩個list與四個mutable payload參照，receipt等待工作保留。3原方法inverse、364 no-init、154新＋7162同SHA繼承、9Batch／1實際receipt等待／6output／17completion／3加密IO／5原失敗分支與同位元重建通過。完整Source／HP／Main／memory／MySQL／實機仍開放；未部署。證據：[20261026T100740195462Z](D:/lineage開發/3-lito開發/lito伺服器/LiTo381-Server/tools/combat-continuation/integration-lifecycle/build/source-completion-progress/20261026T100740195462Z/manifest.json)。


2026-10-26 Batch publication readonly role：候選`64b445985e785155af1f51e2fa7cd14b670a6df961610f4d4c74986b17ede073`，7個emitter＋1個非技能27發布match沿用readonly status-role helper，保留技能27例外及BATTLE／WORLD政策。19原方法inverse、364繼承no-init＋2owner額外檢查、182新＋7316同SHA繼承、40scalar／20Batch／12output／40completion／10加密IO／2原失敗與同位元重建通過。captureOutput／更早Context／完整Source／Main／memory／MySQL／實機仍開放；未部署。證據：[20261026T102427778115Z](D:/lineage開發/3-lito開發/lito伺服器/LiTo381-Server/tools/combat-continuation/integration-lifecycle/build/source-completion-progress/20261026T102427778115Z/manifest.json)。


2026-10-26 Context/output readonly role：候選`ec07718ef88e7fd7bd96d81e3a75342a368bdafaeee22cdc26b17f2e20e10a24`，2個scene/player＋3個target-output match沿用readonly status-role helper，保留BATTLE／WORLD／場景／鎖／permit／send政策。28原方法inverse、364繼承no-init＋2owner額外檢查、162新＋7498同SHA繼承、12validation／26capture／4加密IO／10Hub-population等待與同位元重建通過。更早proof／正常角色／取消路徑／完整Source／Main／memory／MySQL／實機仍開放；未部署。證據：[20261026T104039902791Z](D:/lineage開發/3-lito開發/lito伺服器/LiTo381-Server/tools/combat-continuation/integration-lifecycle/build/source-completion-progress/20261026T104039902791Z/manifest.json)。


2026-10-26 Proc68 custom graphics：候選`cba48781be7313f90af2126992c89ca9e0d4ca2f4b6369f6c2a22c9098d44499`，自訂秒數保留原狀態提交，補Source castGfx／actor proc_spr／actor X10與隱身自己特效；獨立graphics-only accepted pair／first weak receiver接到完整原final frame。2495原方法inverse、79新＋7660同SHA繼承、8capture／5加密IO／12receiver等待與同位元重建通過。通用Proc／default SkillUse／icons／GM invis／完整Source／Main／memory／MySQL／實機仍開放，未部署。證據：[20261026T121216721339Z](D:/lineage開發/3-lito開發/lito伺服器/LiTo381-Server/tools/combat-continuation/integration-lifecycle/build/source-completion-progress/20261026T121216721339Z/manifest.json)。


2026-10-26 Proc68 dedicated icon：候選`de18fc2253ce43d14e7ab6ef902bdc8b41ac99b6d734307901e099352b2b9862`，補Source ICON_I2H及原狀態唯一標記／共同清除，target-only accepted proof沿完整final frame輸出，特效委派保留。2601原方法inverse、127新＋7739同SHA繼承、65536 Source封包比對、17capture／2加密IO／13receiver等待與同位元重建通過。全量ICONS1／OwnCharStatus／已顯示圖示提前清除／個別removeSkill68及其餘完整Source／Main／memory／MySQL／實機仍開放；未部署。證據：[20261026T123303043203Z](D:/lineage開發/3-lito開發/lito伺服器/LiTo381-Server/tools/combat-continuation/integration-lifecycle/build/source-completion-progress/20261026T123303043203Z/manifest.json)。


2026-10-26 Source ICONS1 scalar model：候選`f660957f0f7ce83e8470a6687dff903a8ef1c6ce4b7582c2a635c07ab5c60afa`，完整36種狀態與共用武器優先／覆寫／共存抑制／縮放／時間的純量編碼程式；6842 Source逐位元比對、226新＋7866同SHA繼承、所有原entry保留與同位元重建通過。尚無live producer／receiver port，待接角色投影／各來源coexist讀取／OwnCharStatus／生命週期，完整Source／Main／memory／MySQL／實機未完成，未部署。證據：[20261026T125407000090Z](D:/lineage開發/3-lito開發/lito伺服器/LiTo381-Server/tools/combat-continuation/integration-lifecycle/build/source-completion-progress/20261026T125407000090Z/manifest.json)。


2026-10-26 ICONS1 native inputs：候選`fc7454a04e1434ba49deee9754832810933f84a9f9ce6a98ed298229208c9b05`，36種原生status投影與原技能Catalog最後factory附加唯讀SkillCoexistSet政策，無新Connection權限。13原方法inverse、533新＋8092同SHA繼承、216原生投影／12 mock SELECT／1 Hub等待與同位元重建通過。輸出生產端／final receiver／OwnCharStatus、自動中文alias與實際MySQL／來源熱重載／完整Source／Main／memory／實機未完成，未部署。證據：[20261026T132451213024Z](D:/lineage開發/3-lito開發/lito伺服器/LiTo381-Server/tools/combat-continuation/integration-lifecycle/build/source-completion-progress/20261026T132451213024Z/manifest.json)。


2026-10-26 ICONS1 explicit fruit origin：候選`49ba8c77064861b50ca55e340c074423fd7d76f2f73eb7b5da913b352ea87406`，修正精靈1016／type3與果實1017混淆，四個原brave寫點清除明確來源，project沿原first timed完成純量來源校驗。149原方法inverse、33新＋8625同SHA繼承、10projection／1timed等待與同位元重建通過；533輸入檢查改35原生＋明確果實後全數重驗。Source wire4與內部speed1已分開，實際果實物品／移動攻速、送包／OwnCharStatus與完整Source／Main／memory／MySQL／實機未完成，未部署。證據：[20261026T134247623323Z](D:/lineage開發/3-lito開發/lito伺服器/LiTo381-Server/tools/combat-continuation/integration-lifecycle/build/source-completion-progress/20261026T134247623323Z/manifest.json)。


2026-10-26 ICONS1 scalar publication versions：候選`f8cb6460cfc40fee2e2e4dd653ec8587efab0bdc551f68d846879bb504e3a85a`，17狀態容器、122直接寫點＋共用setter成功點發布原子版本，弱cell current不取status鎖。391原方法inverse、123新＋8658同SHA繼承、35入口／2000並行更新與同位元重建通過；房間Patience／其他反射／完整輸出guard／OwnCharStatus／果實消費者及完整Source／Main／memory／MySQL／實機未完成，未部署。證據：[20261026T154208843808Z](D:/lineage開發/3-lito開發/lito伺服器/LiTo381-Server/tools/combat-continuation/integration-lifecycle/build/source-completion-progress/20261026T154208843808Z/manifest.json)。


2026-10-27 accepted proc68 full ICONS1：候選`1762b112e160071184799e290039885f5405815bce9cc88ef04f50b6738fb482`，完整Source snapshot接原已接受目標與最終receiver。2489原方法inverse、100新＋8781同SHA繼承、13capture／2memory frames／11receiver waits與同位元重建通過。完整Batch／room registry／其他producer／OwnCharStatus／Source／Main／memory／MySQL／實機未完成；未部署。證據：[20261026T161049541255Z](D:/lineage開發/3-lito開發/lito伺服器/LiTo381-Server/tools/combat-continuation/integration-lifecycle/build/source-completion-progress/20261026T161049541255Z/manifest.json)。


2026-10-27 target icons original Batch／receipt：候選`c0291b9a6516fa499d7f936519822d5f6ebbb734c3bc751d0d352bd4fdc943be`，兩個目標圖示task以原Batch sidecar執行一次，借用既有目標item owner與原角色發布scope，保留原guard及callback結果。20原方法inverse、317新＋8881繼承、25 Batch／44 callback／23目標memory frames／6 actual receipts及同位元重建通過；room registry／其他producer／OwnCharStatus／Source／Main／memory／MySQL／實機未完成。未部署。證據：[20261026T163115522246Z](D:/lineage開發/3-lito開發/lito伺服器/LiTo381-Server/tools/combat-continuation/integration-lifecycle/build/source-completion-progress/20261026T163115522246Z/manifest.json)。


2026-10-27 proc68 Source timer retention：候選`7c3abfbcbb558665a62588b2b39778e85bdb91fc3e75d0965aefccc24b3ef744`，較短／相同自訂請求保留原timer身分／expiry／divisor；原Proof接到同ability鎖後核對，失效不修改或送包。33原方法inverse、746新＋9198繼承、280 Source決策／16生產端／12memory frames／13實際monitor waits與同位元重建通過。其餘Proc／scheduler／talent／OwnCharStatus／完整Source／Main／memory／MySQL／實機未完成；未部署。證據：[20261026T170857492886Z](D:/lineage開發/3-lito開發/lito伺服器/LiTo381-Server/tools/combat-continuation/integration-lifecycle/build/source-completion-progress/20261026T170857492886Z/manifest.json)。


2026-10-27 four scalar PC Procs：候選`284bf809ad6616efa6764c749fc42d420f7a55818389aaf7a17bb672c7c265e8`，31／32／71／78自訂時間接原狀態、第一次queue metadata與完整Source ICONS1；保留Source重施／治療優先及原成本RNG。2746原方法inverse、832新＋9944繼承、52memory frames／56實際等待／28 actual receipts與同位元重建通過。其餘Proc／預設SkillUse／buff重算／OwnCharStatus／NPC／完整Source／Main／memory／MySQL／實機未完成；未部署。證據：[20261026T180325048999Z](D:/lineage開發/3-lito開發/lito伺服器/LiTo381-Server/tools/combat-continuation/integration-lifecycle/build/source-completion-progress/20261026T180325048999Z/manifest.json)。


2026-10-27 custom68 first timed queue：候選`e8cbff2bcda30460bc49982be9e9dfcf3656227dec3f5dc49b7bd0cd2e3a4d41`，原Proof／ability帶入第一次狀態、角色與lifetime的完整原queue／mutation副本，拒絕缺失／替換／退休資料。114原方法inverse、163新＋10776繼承、21實際queue waits／14memory frames及同位元重建通過。隔離Frame僅補既有queue身分欄位並留精確收據。其餘Source／Proc／Main／memory／MySQL／實機未完成；未部署。證據：[20261026T182357353653Z](D:/lineage開發/3-lito開發/lito伺服器/LiTo381-Server/tools/combat-continuation/integration-lifecycle/build/source-completion-progress/20261026T182357353653Z/manifest.json)。


2026-10-27 scalar Proc104/173/174/206：候選`070beb629e77d9a837f4475b09ffd4507b968b641c1b25c8e1763f430575816a`，15原方法inverse、3前綴、841新增＋10939繼承、56實際queue／final waits／28 receipts及同位元重建通過。其餘Source／Main／memory／MySQL／實機未完成；未部署。證據：[20261026T212843814278Z](D:/lineage開發/3-lito開發/lito伺服器/LiTo381-Server/tools/combat-continuation/integration-lifecycle/build/source-completion-progress/20261026T212843814278Z/manifest.json)。


2026-10-27 material173/174 first Context life：候選`cb6d1c4d54a9a38d4bc0532b7f655e59b4b87228626ec28721c219b8e20cf6fc`，24原方法inverse、3原方法接線、800新增＋11780繼承、90實際續行waits／16原cast及同位元重建通過。原成本／RNG／狀態／IO、peer reserve與完整Source生命週期仍未完成，未部署。證據：[20261026T214749711753Z](D:/lineage開發/3-lito開發/lito伺服器/LiTo381-Server/tools/combat-continuation/integration-lifecycle/build/source-completion-progress/20261026T214749711753Z/manifest.json)。


2026-10-27 material peer reserve first life/app fence：候選`93bd52e0eb71e15c4a2477df1062da826885bd9d3f5dc5fa92bcf217d6cacd5a`，24原方法inverse、1 caller／1完整reserve副本、453新增＋12580繼承、42直接＋32原caller fence waits、15原Receipt／1原constructor rollback及同位元重建通過。其他reserve、材料實際成本／狀態／IO、完整Source／Main／memory／MySQL／實機仍未完成，未部署。證據：[20261026T215827329049Z](D:/lineage開發/3-lito開發/lito伺服器/LiTo381-Server/tools/combat-continuation/integration-lifecycle/build/source-completion-progress/20261026T215827329049Z/manifest.json)。


2026-10-27 material27 first target Context：候選`8d753f5d0e351fecf603b80d497f871a04d66207469080badee8a558118bf4a2`，33原方法inverse、1 wrapper／3完整原副本、553新增＋13033繼承、46原dispatch／38 peer fence／13 reuse／20 controlled waits及同位元重建通過。Source查詢維持actor monitor外；其餘成本／狀態／publication／IO／完整Source與實機未完成，未部署。證據：[20261026T222637559561Z](D:/lineage開發/3-lito開發/lito伺服器/LiTo381-Server/tools/combat-continuation/integration-lifecycle/build/source-completion-progress/20261026T222637559561Z/manifest.json)。


2026-10-27 material27 controlled Counter Magic：候選`e82052c25c08e8716a64a38976cbe0b74818c86cc1042db28b231dffbdba54d1`；6原方法inverse、1原成功呼叫點、506新增＋13586同SHA繼承及同位元重建通過。8次完整材料施法、56個加密記憶體封包、12次原Source查詢等待；原成本／MP／機率／外控例外與失效拒絕保留。PC分支／其餘Source／後端／MySQL／實機未完成，未部署。證據：[20261026T231116899021Z](D:/lineage開發/3-lito開發/lito伺服器/LiTo381-Server/tools/combat-continuation/integration-lifecycle/build/source-completion-progress/20261026T231116899021Z/manifest.json)。


2026-10-27 material27 PC counter／controlled Earth／first pair：候選`30e222344d776e5cd0088bed8b6334736c22f403af5912c9f1222ce56d24094e`；2536原方法inverse、4原呼叫點、385新增＋14100繼承／更正同SHA及同位元重建。11次PC原施法／123記憶體封包／10原pair waits；受控Earth錯誤預期已明確更正，514檢查／8施法／60封包／12查詢等待。Source來源武器覆蓋、回滾／最終接收端及其餘Source／後端／MySQL／實機未完成，未部署。證據：[20261026T233808570180Z](D:/lineage開發/3-lito開發/lito伺服器/LiTo381-Server/tools/combat-continuation/integration-lifecycle/build/source-completion-progress/20261026T233808570180Z/manifest.json)。


2026-10-27 material27 detached final frame：候選`40cbc3691c2974af52f7c14cf8c4b81abb9e5de8218495c84480a4511ef456b0`；2563原方法inverse、1 capture／4完整原送包副本／9轉接點、540新增＋14485繼承同SHA與同位元重建通過。39次原生施法嘗試／299封包／10 pair／16原最終接收／13脫離原流程最終等待；Source覆蓋與不同SHA的38項原save回滾檢查。無新增TLS／全域索引／佇列／worker；其餘Source／物理消費端／後端／MySQL／實機未完成，未部署。證據：[20261027T004126141290Z](D:/lineage開發/3-lito開發/lito伺服器/LiTo381-Server/tools/combat-continuation/integration-lifecycle/build/source-completion-progress/20261027T004126141290Z/manifest.json)。


2026-10-27 shared material pair：候選`2d05b7bacebbbb95cb65f2fdffa4b78fdff7acb6c4bb9899fafe5300abde1d3f`；150新增＋15025繼承同SHA與同位元重建。173／174原生入口30次pair等待，首次角色／life／status守衛、兩個介面opcode修正，2539完整原方法inverse、0原欄位新增。其餘Source／材料／後端／MySQL／實機未完成，未部署。證據：[20261027T010505810899Z](D:/lineage開發/3-lito開發/lito伺服器/LiTo381-Server/tools/combat-continuation/integration-lifecycle/build/source-completion-progress/20261027T010505810899Z/manifest.json)。


2026-10-27 shared material pair ingress：候選`06f09c15900a31e0f7697f86437236634795783405cfdd234f996087a10b446d`；267新增＋15175繼承同SHA與同位元重建。首次binding／status／lifetime接入原收據與ingress；11原pair入口／2初始缺status／36原app等待／8 receipt／1建構回滾，79原方法inverse、4完整原副本、0原欄位新增。其餘Source／材料／後端／MySQL／實機未完成，未部署。證據：[20261027T011847142030Z](D:/lineage開發/3-lito開發/lito伺服器/LiTo381-Server/tools/combat-continuation/integration-lifecycle/build/source-completion-progress/20261027T011847142030Z/manifest.json)。


2026-10-27 material Main ingress：候選`751ebc72c7f17cdaa6e5efc599df4961e619a0eaf39a7f83bae545b345c52589`；144新增＋15442繼承同SHA與同位元重建。58模型／232原生入口含116 Main／STOPPED無成本／封包／例外；技能73另8原鎖等待。50原方法inverse、7 prefix、0原欄位／方法新增。完整材料效果／Source／最終接收／後端／MySQL／實機未完成，未部署。證據：[20261027T013928444182Z](D:/lineage開發/3-lito開發/lito伺服器/LiTo381-Server/tools/combat-continuation/integration-lifecycle/build/source-completion-progress/20261027T013928444182Z/manifest.json)。


2026-10-27 self final ingress：候選`07a334d7aa8da829af7e4b5543ac12e305b07541706e2b9ae254741766397d9b`；324新增＋15586繼承同SHA與同位元重建。首次入口證據直達原receiver cipher monitor；32原73最終viewer／observer、5 detached、6原source lock等待。2578原方法inverse、22呼叫點、2 transient欄位、原record／四sender方法保留；完整Source／其餘效果／後端／MySQL／實機未完成，未部署。證據：[20261027T022653634154Z](D:/lineage開發/3-lito開發/lito伺服器/LiTo381-Server/tools/combat-continuation/integration-lifecycle/build/source-completion-progress/20261027T022653634154Z/manifest.json)。


2026-10-27 material69 exit：候選`c4d8bdaf436050e7ea9e64bf1702256e7b529a67892b0c03b10baa32c695a1b6`；193新增＋15910繼承同SHA、124重編native plans、同位元重建；原退出發布鎖／首次角色生命／最終cipher補齊，13來源等待／13最終等待／5巢狀。58列232入口無未預期例外；完整效果／Source／後端／MySQL／實機仍開放，未部署。證據：[20261027T032152772802Z](D:/lineage開發/3-lito開發/lito伺服器/LiTo381-Server/tools/combat-continuation/integration-lifecycle/build/source-completion-progress/20261027T032152772802Z/manifest.json)。


追加decoded-request驗證：58材料列464次normal／Ctrl／self／enemy／Honor同隊／Main／STOPPED入口無未預期例外；87個非物件Ctrl layout明確不適用。27／173／174另有42次原Ctrl成對等待、312斷言，切角／status／life／lifetime／application／Main退役不扣款或送包，DRAINING保留。此為同候選附加證據；完整效果、Source意圖及實機仍未完成。 證據：[20261027T035029042081Z](D:/lineage開發/3-lito開發/lito伺服器/LiTo381-Server/tools/combat-continuation/integration-lifecycle/build/source-completion-progress/20261027T035029042081Z/manifest.json)。


2026-10-27 controlled NPC final：候選`e7701e624f503b7078714efc346c5481e6778acd6dd1ce5ce9e2399fa773bc4a`；173／174舊特效最終cipher缺口修正，695斷言／78最終等待、456／60 NPC成對；16103繼承普通檢查與固定輸入重建通過。全部Source／技能／後端／MySQL／實機仍開放，未部署。詳見[source-controlled-negative-final.zh-TW.md](source-controlled-negative-final.zh-TW.md)。


唯一NPC物件ID重驗：前版場景同時保留舊NPC123與真實元素NPC45306，兩者都為777，造成29／152／153原body先選舊actor並誤報距離；這是測試缺陷，未改生產選取／距離／競技規則。現以四角色各自不同且停用的primary、唯一真實45306重驗，60次成對／456斷言、78次最終等待／695斷言仍通過。464材料入口為64成本觀察、146拒絕、248非NPC封包layout、6無成本回應；116次Main／STOPPED無扣款送包。29／152／153敵方、自有及Ctrl同隊分別保留17／20／10MP和1材料；一般同隊仍拒絕。舊NPC入口統計由本證據取代，完整類別失敗／RNG／狀態／各技能生命週期仍待補齊。 證據：[20261027T051702830790Z](D:/lineage開發/3-lito開發/lito伺服器/LiTo381-Server/tools/combat-continuation/integration-lifecycle/build/source-completion-progress/20261027T051702830790Z/manifest.json)。


2026-10-27 negative29/152/153 first：候選`bf4aa2a61d488bd68c2cdaa0d9a7df3221f2e7848443f225c584ea137ddcbaf2`；首次status／life／lifetime跨原roster鎖重試修正，150次一般／Ctrl等待、1878斷言；原成本／Source類別失敗／不抽RNG與既有self-pair保留；16103普通檢查與固定重建通過。其餘Source／最終接收者／後端／MySQL／實機仍開放，未部署。詳見[source-negative-pair-first.zh-TW.md](source-negative-pair-first.zh-TW.md)。


2026-10-27 negative final：候選`50635f7edca8ec6ce77787cc32371c7590b497e48ea202f8a679caf3f517b729`；29／152玩家／觀察者240次最終等待2280斷言＋153原扣款後自接收60次480斷言，3清理故障17斷言；不可變首次值接原cipher，Aim／receipt在IO前釋放。16103普通繼承檢查與固定重建通過。其餘Source／後端／MySQL／實機仍開放，未部署。詳見[source-negative-final-frame.zh-TW.md](source-negative-final-frame.zh-TW.md)。
