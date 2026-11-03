# LiTo381 總開發報告與接續指南

更新：2026-10-25 05:14（Asia/Taipei）。整體目標：完成真正 Windows x64 原生客戶端 G01–G12，優先 G06.3d 傳送、角色接續、舊世界退役與失敗恢復，再完成當版完整 library Debug／Release、Release 回歸、GUI 與可攜交付；自行核對無誤才交人工。**整體未完成，尚未交人工。**

**使用者目前要求：只做開發，測試與桌面操作暫緩。** 自動續行不代表恢復測試；不得啟動測試本文／--list、GUI、fixture／server／DB或播放。可做來源開發、唯讀位元碼／資料核對及編譯。不要求使用者登入或找視窗。

## 目前版本

- 正常來源 `3E58268A74B9908597221E71BDEC646C1DFEB82A8658444BDF1C24BFABE94628`，1048來源／1049編譯輸入；正常Debug `AB99BED1`／Release `4340DA5F`，未改。
- 正式唯讀JAR `E1498871C7BE6B287821C57EF5D95933DB1740D9B4D4A80F6C3465F21D99D498`；catalog `B64ED920`。[正式差異](<D:/lineage開發/3-lito開發/lito重建版/驗證紀錄/current/native-formal-reference-refresh/20261023-192435-953238/evidence.json>)。歷史75的runtime／API／GUI資格不可移記E149；E149完整owner／Main及runtime尚未通過。
- 最新隔離候選44檔：30 Runtime／11 Rust QA／3 Java，372 module／原六 nested；資源就緒與畫面失效守衛已接，仍不可套用。正常來源／registry／既有PE未改。[來源與20項編譯紀錄](<D:/lineage開發/3-lito開發/lito重建版/驗證紀錄/current/native-E149-static2941-ready-development/20261024-211350-252315/evidence.json>)；[最新八檔 checkpoint](<D:/lineage開發/3-lito開發/lito重建版/驗證紀錄/current/native-E149-static2941-ready-development/20261024-211350-252315/readback.json>)；[工作佇列](<D:/lineage開發/3-lito開發/lito重建版/工作佇列.json>)。來源未凍結，無活動批次。

## 已完成的來源開發與目前界線

**最新資源就緒 gate 與 Scene／History 接線已加入隔離候選**。#97 的流程改為收包／dispatch → 等待實際 SPR bank → 資源就緒證明 → fresh enqueue；尚未就緒不建立 deadline。證明持有嚴格核對過的 bank，不能用布林值或假時間代替。Scene 背景載入與原 fragments／Over 提交、FiniteHistory、view 交付及 controller queue 已接；cached／late-worker／paint／input 共用失效 token，退役後不能靠舊快照或 root ABA 復活。載入錯誤在同來源範圍保留。

**97 尚未完成，候選仍不可套用。** 原共享 callback、重入繪圖與固定 sample 的交錯尚未補齊；原生 clock 初始化／rollover、實際資源與像素均未驗。現代流程會在資源證明交付後排程並 drain，不能據此宣稱原生可見時機等價，也不能假定固定一幀或永遠不可見。32 秒狀態期限仍由 server 掌管。

本輪六個 Runtime 覆寫，44檔／30 Runtime／11 Rust QA／3 Java、372 module／原六 nested 不變。Debug／Release 各 library、library-test、七技能與 network 共 **20 項 metadata 編譯全部退出0**；只產生 metadata，未連結 PE、未執行測試。原8個 queue 案改寫，新增4案，共12案（8純／4需原資源）；累計 **475案全未跑**，不是完整 library inventory。候選描述沿用的新增資源案數0已另註：按實際 QA 為新增1、原3重分類；不改已編譯 contract。技能請求來源88／剩92不變，並非88技能完整。

準備工具曾在讀取新增 module 的索引處失敗（9e69f3），當時尚未啟動 Rust 編譯；原工具與 partial 目錄保留，v2只修候選／正常清單取值。前輪 extern 命名失敗與所有原收據亦保留。IngressReceipt 的 decode 時刻仍在解密後、backpressure前；與 dispatch／constructor／resource-ready／enqueue 時刻分開。bank 只在 MissingEntry 使用1120後備，IO／損壞／取消不得降級。

97請求／S34、98請求／2942有限特效及既有技能來源沿用；成本／材料／隱身／中毒／自然期限皆由 server 決定。69同socket、87持續4183、constructor／方向初始化、LT hurt/base及 G06 原物品／source/account／timer 等未驗界線保留。正常1049輸入、既有PE、正式JAR、registry及G06 binding／640次計畫未改；未啟動測試、GUI、server／DB、SPR解碼或播放。

完整前輪狀態及歷史原證據見[此前完整報告與歷史證據](<D:/lineage開發/3-lito開發/lito重建版/驗證紀錄/current/master-report-current-work/20261024-211404-339289/before.md>)。

## G06與尚缺前提

原十三檔registry＋fixture去重十四檔，仍未套用；第二次cancel保留端點／帳號／來源限定的一次性LOGIN恢復，缺原角色或逾期走一次C8清單確認，原8／30／60秒與自然33／31秒不變。[候選](<D:/lineage開發/3-lito開發/lito重建版/維護工具/candidates/native-E149-recovery-observer-20261023-194101-605228/candidate.json>)。

fixture binding v3＋observer stability extension、case／execution v4、suite v3已備：Debug／Release×八分支16案，每案32 setter＋8 setItem／setItemId，共640次，**全未執行**。原factory／資源初始化、18唯讀accessor、17物品欄／8永久屬性及14診斷已接；Java六class編譯0／Rust metadata不等於GAME。四元件inventory與102精確案例亦未跑。[目前binding](<D:/lineage開發/3-lito開發/lito重建版/驗證紀錄/current/native-E149-observer-stability-binding-development/20261024-024235-491711/fixture-binding.json>)；[16案計畫](<D:/lineage開發/3-lito開發/lito重建版/維護工具/candidates/native-E149-recovery-suite-plan-20261024-024515-538911/plan.json>)。

正常GAME的非預設物品／擴充屬性、source/account、timer正向建立前提仍缺；stable觀測不是原子授權，不排除ABA。未選入的StateCopy observer：任何非空ScheduledFuture（含done／cancelled）、EquipmentTimerLease、true pending或generation>=0（含0）都阻擋；不可造owner／模板／計時狀態或注入時鐘湊矩陣。正式JAR全Code掃描未找到直接建立／排程caller，不排除反射／外部路徑。[timer前提](<D:/lineage開發/3-lito開發/lito重建版/維護工具/native-E149-item-timer-routes.md>)；[StateCopy精確守衛](<D:/lineage開發/3-lito開發/lito重建版/維護工具/native-E149-state-copy-preconditions.md>)。

## 驗證界線與既有失敗

- 歷史75／正常3E限定結果保留：完整library兩版各1975過0敗488忽略、資源85／API16與#43限定資格；非E149當版結果。403個ignored尚須逐名前提審查，5個TW項缺TW13081901.pak，不可替代或算通過。
- 目前舊二十一檔版四PE及portable ZIP已過期，不含最新44檔；連--list也未跑。portable依賴使用VS18預覽runtime，僅內部候選，clean host／動態載入／最低Windows／G12未驗。[舊映像與原始記錄](<D:/lineage開發/3-lito開發/lito重建版/驗證紀錄/current/master-report-current-work/20261024-211404-339289/before.md>)；[可攜限制](<D:/lineage開發/3-lito開發/lito重建版/維護工具/native-E149-portable-candidate.md>)。
- GUI歷史75普通房兩版／租裝Release僅有限資格；Debug租裝23695 exit1保留。FrameArrived逾時／geometry unavailable未證明恢復，不能以CPU PNG取代GUI像素；旅館返回、Enter負向、EOF／取消／傳送恢復及E149完整GUI均未完成。
- 原編譯／QA／讀回錯誤完整保留；55／56 QA E0502修正及18metadata、FILETIME先比對的原22程序讀回分開記錄，未藉工具錯誤重跑測試或控制無關程序。詳細見[此前完整報告與歷史證據](<D:/lineage開發/3-lito開發/lito重建版/驗證紀錄/current/master-report-current-work/20261024-211404-339289/before.md>)。正常1049輸入、所有既有PE、正式JAR、registry與G06 binding／640次計畫不變。

前輪原PID查詢拒絕與CIM建立時間讀回分開封存。本輪接續以原FILETIME為先；無法開啟的PID只可由成功CIM觀測證明缺席或不同birth，不查詢無關影像、不控制或重啟程序。[唯讀原身分工具](<D:/lineage開發/3-lito開發/lito重建版/維護工具/native_E149_process_birth_readonly.py>)。

## 下一步，依序接續

1. 暫緩期間繼續開發：補97原共享 callback／重入繪圖 producer 與固定 sample／巢狀 drain 交錯；確保合法時機可提交，callback 後不能再發佈。不固定一幀或借32秒期限；原 clock／rollover及獨立像素驗證留待測試。97完成再99／100及剩92，續4183、G06物品／source/account／timer與LT hurt/base。測試與桌面仍待使用者明確恢復。
2. 使用者明確恢復測試後：先跑當版12個 gate 案（4案需原資源），四元件inventory、17原控制／6指定反例及候選案例；再跑G06兩版16案640次，核對精確原PID／FILETIME／Job／DB退出、自然33／31秒、32＋8雙trace。首個失敗保存原收據，不自動重試。
3. 驗新技能當版原資源、接受／拒絕／低MP／材料／機率／狀態／自然期限／重施放／舊目標／取消及音效、History重建／ring去重／失敗退役。獨立合併G06與玩法候選、核對後才套用正常來源；不得移記舊映像資格。
4. 重建最新映像，先完整library Debug／Release、必要資源與當版API／controller六分支96，再做Release廣域／main回歸。重新盤點新增整合目標，不盲用117固定數；不在計時GUI伺服器旁編譯。
5. 完整GUI：普通房、租裝入口、旅館返回、Enter負向、EOF／取消、新技能、owner退役與傳送恢復；核對實體輸入／像素、IME、多DPI、聲音、效能及長時間故障。共享鑰匙／訪客離房、3600／7200秒自然租期、普通服務與雙玩家戰鬥退出不可省略或縮時。
6. 完成其餘G01–G12、全地圖資源／遮擋／相機、clean host Release可攜交付與逐要求稽核，全部自行核對後才交人工。

## 接續規則

重建版、登入器、伺服器為獨立專案／產物，相鄰來源唯讀，登入器JAR替換提案已撤回。所有寫入限重建版，總報告為授權例外；不碰玩家實表／正式程序，不包裝LiTo.bin，不派代理／其他對話／Git。

唯一正常入口：啟動現代客戶端.ps1；Cargo --locked --offline -j1、x86_64-pc-windows-msvc、target/current，Release z／thin／16／strip。GUI恢復後才依Computer Use技能操作。下次先讀本報告、[AGENTS](<D:/lineage開發/3-lito開發/lito重建版/AGENTS.md>)、[README](<D:/lineage開發/3-lito開發/lito重建版/README.md>)、[架構](<D:/lineage開發/3-lito開發/lito重建版/docs/ARCHITECTURE.zh-TW.md>)、[歷史](<D:/lineage開發/3-lito開發/lito重建版/docs/HISTORY.md>)與最新checkpoint；報告工具為[update-master-report-E149-current.py](<D:/lineage開發/3-lito開發/lito重建版/維護工具/update-master-report-E149-current.py>)，不要用舊schema覆寫。
