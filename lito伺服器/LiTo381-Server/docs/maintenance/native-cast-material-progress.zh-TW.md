# 施法材料提交施工紀錄

2026-10-18 隔離原型：`a9c108568d567ae6d4304201dfb69a34c3258502a375733650842880d513e955`。先前14材料技能原型為 `adf4c3e8…`，retained材料原型為 `7de9584c…`，已封階段父版仍是 reuse lease `35853617…`。目前真正入口涵蓋18個材料技能與97／102兩個無材料變體；**其餘40個材料技能、全部目標／範圍／trigger 及材料專屬 Main／阻塞I/O／異常提交驗證仍未完成，未部署，整體目標未完成**。

十四個真正自施方法保留原數值、狀態、動作與封包 body，材料扣除與保存位置改由既有 NativeDeItemTransaction 處理，依 Source 順序先取得 action／reuse reservation，再提交材料，失敗則取消 reservation。技能為31／60／78／97／98／102／105／129／134／137／138／147／169／171／175／176／185／186／190／195；97與102不需材料，仍須經過同一動作與reuse門檻。材料交易保存真正記憶體 economy，再安裝 canonical inventory；失敗訊息及成功封包沿既有 FIFO 在鎖外輸出。ClientSession 與 repository 整類比對僅容許列出的材料邊界、wrapper、snapshot／save 及唯讀 connection bridge。相對已封父版共十個既有 entry 改變、十三個新增 entry，沒有新 PC 欄位、worker、scheduler 或 ThreadLocal。

已接受的材料施法沿用原 ingress receipt、binding、application、canonical owner、原／暫存背包及實際 Operation 身分，在真正 action lock 與 lifetime admission monitor 內完成。STOPPED 不再丟失這筆已接受工作。一般 Owner.requireActive 完全不改，只有該材料暫存背包可沿 retained adoption 接點安裝；交易 finally 清除暫存授權。巢狀其他 Operation 不能借用或清除此授權，Source item setter 與無關背包替換也不能藉此繞過 STOPPED。

完整原始 Source appendGeneralConsumePlan、需求／計畫型別與結果 enum 從凍結 L1PcInventory 投影，選取順序仍是 enchant 再 object ID，排除封印、裝備中、不同持有者、無模板及無效數量，保留 identity 去重、混合 stackable 拒絕及 long 計算。這是材料選取器的資料驗證，不能視為完整 Source removeItem／副作用／quarantine 的移植。

材料 GM 豁免採 Source CharacterTable 的角色旗標 `characters.AccessLevel >= 200`，不是帳號 administrator ACL；100 是 monitor。真正 LegacyMySqlRepository 只新增既有 connection 的型別接點；角色查詢採唯讀、repeatable read、關閉 autocommit，rollback 並關閉全部 JDBC 資源後才發佈角色快取。相同角色不重查，更換角色 ID 會退役舊快取。此階段只涵蓋初始角色旗標，執行期間 GM 指令切換仍開放。

已完成的原型檢查：

- 新增龍騎士覺醒185／190／195與血之渴望186：同一份編譯前後各192案，涵蓋兩種persistence旗標、WORLD／BATTLE、有／無item application及正常／Source時間窗／缺料／零MP／HP臨界值／再次施放。32次時間窗繞過歸零；正常、零MP及再次施放封包逐位元組相同，前後2568／2112 frame，鎖內寫入零。覺醒仍在再次施放時關閉；原HP／MP不足分支先執行，未取得材料或action授權時不讀成功snapshot。
- 四龍騎士技能另32案preaccepted STOPPED與128案角色／場景／真正socket失敗通過。獨立範圍核對確認相對14材料技能原型只改PC與NativeSelfCastAction兩個entry；PC只改兩個原方法並新增兩個body，自施接點只擴充supported／admit與兩個typed wrapper及其編譯器lambda和catalog inner-class metadata。其他PC方法／欄位及整套item／lifetime／transaction／SQL／timeline類別維持。
- 初次龍騎士測試把MP設定高於角色正常上限，覺醒重算上限造成MP截斷；已將fixture初值改為角色正常100MP，production未因測試改動，原失敗保留。獨立範圍檢查最初漏列新增wrapper產生的兩個lambda及兩個catalog inner-class descriptor；已明列精確集合，再核對其餘完整class，失敗紀錄保留。
- 同一份編譯、真正 handle 入口，前後各32組：flags × BATTLE／WORLD × 有／無 item application × 時間窗阻擋／正常／缺料／MP0。共用動作窗的8次繞過歸零；正常封包逐位元組相同，缺料精確299。各184／136 frame，鎖內網路寫入均零。
- 真正 GameStateStore 保存前故障，前後各8組：父版8次留下扣料／MP／效果／reuse，原型歸零。原型保留原背包 identity 及 economy，只輸出299，取消 provisional action／reuse，立即重試成功。只對 GameStateStore 安裝已驗證 inverse 的兩個觀察 hook，PC／item／effect／admission 皆來自真正 JAR；原儲存指令保留。各24／8 frame。
- 真正 Source item setter 與 Counter Magic，前後各8組：封印材料被消耗4次歸零；合法祝福值2且未封印仍可使用，正常封包相同。各56／32 frame，鎖內寫入零。最初測試選用129，真正原生 item 驗證拒絕，因此改用合法值2；失敗原文保留，未修改 production 來接受測試值。
- 真正 repository／policy／角色metadata、模擬 JDBC 接點40組：角色旗標0／100／199／200／255、真正缺料施法、缺角色／query／rollback／close 失敗及相同名字不同角色 ID；52 frame 獨立解密核對，鎖內寫入零。沒有連線真實玩家 SQL，也沒有 SQL write operation。
- 材料選取器171組、511檢查：共用靜態表實際58個材料技能、五種材料，以及封印／裝備／持有者／去重／排序／非堆疊／long 邊界。沒有執行 Source PC，也不表示58個真正 handler 已接入。
- 同一份編譯的16技能真正 handle，前後各384組：兩種persistence旗標、BATTLE／WORLD、有／無item application、正常／跨技能時間窗／缺料。120次共用動作窗繞過歸零；正常解密封包完全相同，MP成本另由原Source資料片段計算。前後3280／2288 frame，鎖內寫入零。新增接點最初放在材料分支內，使無材料97繞過；目前已移到原分支之前，102相同結構一併納入，失敗原型保留。
- 全16技能128組已接受 STOPPED 控制通過：原HP／MP／材料／reuse及輸出一次完成，1056 frame、鎖內寫入零；新施法與一般物品修改仍拒絕。另512組涵蓋提交後角色退役、排隊中角色／場景切換與真正首次socket輸出故障，確認不重扣材料、不洩漏收據、不向替換角色發舊封包。
- 保存前驗證 receiver 已改成暫存 map 對 live map 驗證。真正 Source item setter 在 copy 後獲准修改canonical數量，前後各4組；舊版8次保存／回復呼叫歸零，修正版保存前即回299，保留Source已接受修改且可立即重試。僅對原RuntimeInventory copy-return及GameStateStore save-entry裝觀察hook，兩個整類 inverse 保留原指令，沒有PC覆蓋。
- 初版與本版同編譯 STOPPED 前後各8組：已接受施法丟失8次歸零，各8／56 frame；後續新施法、一般道具發放及 Source item 修改仍拒絕。另8組真實記憶體保存回呼觀察確認巢狀一般交易、Source setter、無關暫存替換不能借用材料授權，原材料交易仍完成，56 frame、鎖內寫入零。
- 先前生命週期原型的獨立反向整類比對僅允許兩個精確 lifetime predicate 與一個 transaction wrapper，Owner.requireActive及其他item方法／欄位保留。本版相對該原型只改PC、材料helper與自施接點三個class；全套一般item授權／生命週期class逐位元組相同。PC新增13個方法body的材料邊界由整類比對與正常封包對照驗證，不能再宣稱PC等於只接Counter Magic的舊版。
- 本版同一 SHA 的完整20回歸、14生命週期與10既有時間窗／Light／效果控制全部通過。所有 native fixture 依序執行，原斷言與期限保留；材料動作窗、保存失敗、封印、GM與選取器也已在本版重跑。
- 兩次重現只讀凍結來源、父 JAR、ASM 及 library，不執行可變 Source preparer、不使用 Source classpath；兩次 SHA 與上述真正入口原型相同。最新進度稽核確認必要證據、凍結輸入、十改／十三新增 entry、Source 五個檔案與四個保護檔及 canonical 收據。
- 512組native斷言通過後，整理器誤把預期零封包的空白列當成三欄資料。原失敗manifest／log及完整觀察保留；修正只核對512／3253／零frame／零鎖內寫入與凍結輸入，另存postcheck，未重跑native、未放寬原斷言。首次過期副本控制也曾遇到同類空輸出整理問題，已留存原文。

幻術師四立方體205／210／215／220已完成原始body與Source前置拒絕的盤點。原LiTo在placeCube成功後才安裝扣料副本；Source先拒絕附近同種立方體，再發動效果。後續需協調placement與材料交易，先證明重複／無效放置及材料保存失敗不留下錯誤扣料或效果，不能直接套用目前自施提交位置。盤點收據為 `de-native-cast-material-development/remaining-material-census-current.json`。

仍需完成：其餘40個材料技能及所有 profession／Ctrl／area／item／trigger 入口；其他application退役／併發／阻塞I/O／真正Main邊界；物品保存後安裝異常的回復與隔離策略；opaque material actor及canonical int數量與Source long數量的整合；Source buffered physical attack cancel／accepted cast state與執行期間GM旗標。實際JAR普查仍有51個方法、52個舊material呼叫，含PC與兩個非PC輔助類別。後續改動需在最終同一SHA重跑必要完整回歸、生命週期、既有控制與560＋60數值重播，再作完整稽核及封存清理；本紀錄不表示完整材料或整體目標完成。

保留 LiTo 競技／隊伍／TA、記憶體角色／道具／技能／XP，僅 character_lito／settlement 可持久化。正式核心、固定 base、聯合基準、canonical runtime、來源專案、玩家 SQL 與 WAL 未改。施工中的原型、反例、未解失敗及凍結輸入保留原路徑，尚未清理；完成本階段後依精確依賴、SHA 與封存內容核對整理，不套用廣泛刪除。

進度收據：`tools/resource-database/build/native-cast-material-progress-current.json`。進度核對：`python -B -X utf8 tools/resource-database/build/audit-native-cast-material-progress.py`。此命令只稽核上述進度，不是完整材料階段驗收。
