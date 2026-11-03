# 戰鬥技能同源化：目前進度

更新：2026-10-24。本頁是續作的主要進度表；已完成項目沿用既有候選與測試，不重新開發。逐階段證據以[機器索引](../../tools/combat-continuation/continuation-current.json)為準。

## 版本與驗證界線

| 項目 | 現況 |
| --- | --- |
| 正式版 | **0.6.71**；由另一發布流程更新，職業圖示／GM外型功能依其發布紀錄管理。本戰鬥候選未部署，未來須隔離整合。[唯讀核對](../../tools/combat-continuation/integration-lifecycle/build/blind-external-formal-release-audit-current.json)。 |
| 最近完整後端驗證候選 | **`8d26c9d7…`**：同 SHA 通過 2,400 案／45,156 檢查、5,164 案組合及 20＋18 組回歸；SQL 啟動／所屬程序清理、重建一致性通過。另 24 案延遲送包邊界通過。**未發布，原生介面待集中實測**。 |
| 本輪增量候選 | **`3bda9d5db34b…`**：同 SHA 通過 **1,206 案／已計數 28,520 項檢查**，含 Main 68／73 的 10＋8 案；新增登出 owner-first／純落地 worker／佇列及計時器拒絕／STOPPED／shutdown／場景與世代清理。跨主人單體／直線／範圍、Ctrl、群組、造屍／馴服及成對回歸已重播。凍結重建及八個既有／四個新增 class 隔離通過；完整技能／Source 與最終全回歸仍開放。[索引](../../tools/combat-continuation/integration-lifecycle/current.json)。 |
| 材料進度 | 66／76／161／167、69／165 及 BATTLE NPC 相消的既有專項保留，各自 SHA 證據未併入本輪 1,138 案；完整材料覆蓋仍待補齊；目前剩一個舊呼叫定位（靜態NPC後備）；87受控BATTLE NPC的准入與發布缺口已限定修補；173／174原稱「預檢」的實際缺口已修補，不能以呼叫數代表完成。 |
| 本次更新 | 候選 **`fc67db5a0fa4…`**：13 post-MP damage／healing target roll loops＋2 none primary data ports，原HP FIFO／audience不變。2504 inverse／274 no-init／33新＋同SHA1737純契約／凍結重建；真bound cost／RNG／HP／life／role／IO／Main／DB／game0，完整Source未完、不部署。 |
| **產品場景澄清** | 使用者確認 **LiTo只有競技場，沒有WORLD地圖**。全部目標／Ctrl與游標以競技模式、實際隊伍／關係及回大廳驗收；WORLD只保留既有後端回歸，不再列產品完成門檻。歷史收據維持原樣。[範圍](../../tools/combat-continuation/integration-lifecycle/build/competition-only-product-scope-current.json)。 |
| 奇古獸PC外控與生命週期 | **756案／9,280檢查同SHA**：含既有632案、Main73八案及新增116案；32真實成對等待、44最終接收端等待。舊24特效／32延後HP准入落差歸0。設定關閉不關閉成對安全，旁觀接收者退役不誤刪合法目標HP。[收據](../../tools/combat-continuation/integration-lifecycle/build/source-kiringku-progress-current.json)。 |
| 角色裝備中文欄位續作 | `b85b71d8…`已通過兩個來源的實際MySQL原生唯讀重載：33案／94檢查、6次靜態防具映射與6次空背包重載；24連線／36 statement／36 ResultSet全部關閉，0寫入嘗試。唯讀Source背包不再要求不存在的lito_item_origin；可寫模式保留。新程式候選保留20個相關SQL class entries逐位元相同；未驗真實裝備角色，使用者延後此驗證。 |
| 17／148／149目標續作 | **同SHA640案／7,830檢查**；17的66範圍差異、48生命週期差異及148／149的32目標語意差異歸0；含48＋84個扣費前等待、48＋72個最終Frame等待，及163十二案、Main73八案。更早3,636案屬父SHA，未混加。[收據](../../tools/combat-continuation/integration-lifecycle/build/source-area-weapon-progress-current.json)。 |
| 裝備中文欄位相容 | 使用者確認已完成改名；隔離候選依凍結899欄對照，把既有18個Owner唯讀查詢投影回原讀取標籤，另適配armor Proc原啟動查詢。白名單、交易與資源關閉保持；未改資料／schema／WAL。此範圍不是全部正式SQL消費者資格化。 |
| 外控下一步 | 已重新盤點807技能列／58材料列、實際HP呼叫及252個Source明確鍵引用；鍵字／模型／呼叫存在不等於消費端完成。其餘proc／cube／doll／週期／NPC工廠AI／受傷動作、完整材料／Source與生命週期仍開放。[更新後目標與工作清單](source-completion-work-current.zh-TW.md)。程式全部完成後集中驗證，未部署。 |
| 29／152／153目標與生命週期 | **同SHA3,636原生案／已計數80,229檢查**；216案重現72准入／138狀態差異後歸0，153遠距36差異與自施12差異歸0；144PC邊界／84真實等待、36PCFrame案／28實際Frame等待、1044NPC邊界／504等待、72材料故障立即重試通過。PC移動等待漏洞已重現並修正；不混加歷史SHA。[收據](../../tools/combat-continuation/integration-lifecycle/build/source-negative-progress-current.json)。 |
| 通暢氣脈術離線路徑 | 26／42／68走既有7-byte物件分派；歷史原始x86 callback24案／85檢查，含12自身正常／Ctrl、18個精確明文封包。未模擬上游游標、前景、正式DLL實際程序及現場加密送包，**未宣稱回報已修好**；未開遊戲UI。[證據](../../tools/combat-continuation/integration-lifecycle/build/client-object-callback-reference-current.json)。 |
| 退休接線界線 | 歷史 `1ad4cd9c…` 元件 48 案未累算；登出與工作持有已在本輪接線。父版六個原始缺口中的 World 兩個無落點移位案例仍開放，不能將斷線證據當成傳送驗收。競技、131 停用及超容量整筆拒絕保全資料的規則保留。 |
| 傳送與跨角色目標續作 | `1281cf6542ee…` 的 632 案保留；後續到期候選 **`f66e13a0ead5…`** 同 SHA **656 案**，補活躍原場景中一般 SUMMON 的正數期限、回復／重試、16 次等待與原場景結算。另技能27候選 **`94eab3441762…`** 新增36案、同 SHA 回歸2,312案，合計 **2,348案**；新圖誤命中／舊圖漏找皆0，保留正常／Ctrl、競技、玩家與扣料順序。兩個 SHA 不混加，整合檢查點仍是3bda9d5d的1,206案；其餘材料技能、視野／AI／毒傷、NPC回饋Ref及完整Source／最終全回歸仍開放。[收據](../../tools/combat-continuation/integration-lifecycle/build/companion-expiry-weapon-progress-current.json)。 |
| 負向與地縛跨角色續作 | 29／152／153 **`b4e9f0d8ca8d…`** 同 SHA **1,238案**，157 **`aaec3b60b70b…`** 同 SHA **576案**；補原場景查詢、施法前捕捉及主人收據，驗角色／生命／位置／同ID替換、STOPPED、提交後送包退役、正常／Ctrl、競技、材料回滾與Main68／73。速度封包退役4案缺口已重現並修復；不同SHA不相加，完整34技能與Source／最終全回歸仍開放。[收據](../../tools/combat-continuation/integration-lifecycle/build/companion-negative-earth-progress-current.json)。 |
| 回歸自然跨角色續作 | 145 **`6a5a36c55e4a…`** 同SHA **684案**；補原場景查詢及施法前角色／生命／位置捕捉，保留遠端主人原背包、實際HP與原場景移除。4案提交後同ID過期刪除缺口已重現修復；8案滿庫存／4次主人儲存故障完整回滾。含既有Nature／競技、成對／範圍、Main68／73及124案共用Stage回歸；不混加前輪SHA，完整Source及其餘技能仍開放。[收據](../../tools/combat-continuation/integration-lifecycle/build/companion-nature-progress-current.json)。 |
| 復活跨角色續作 | 61／75 **`63f38c850e28…`** 同SHA **1,208案**；原場景PET／NPC屍體、正常／Ctrl、成對等待及復活後生命／位置／同ID送包重驗。外層其他技能Context不再繞過捕捉，結束完整還原；700案既有行為與332案成對／範圍／Main68／73通過。完整Source、其餘技能及最終全回歸仍開放。[收據](../../tools/combat-continuation/integration-lifecycle/build/companion-resurrection-progress-current.json)。 |
| 範圍主次目標續作 | 66／76 **`6ffd0fc0df88…`** 同SHA **1,276案**；主／次各8個漏找歸0。64案含48次主人鎖等待與STOPPED，32案提交後生命／位置／同ID／角色送包重驗；16案補混合目標主人退役／STOPPED。800案原範圍及332案成對／範圍／Main68／73通過；其餘材料與完整Source仍開放。[收據](../../tools/combat-continuation/integration-lifecycle/build/companion-area-progress-current.json)。 |
| WORLD角色生命續作 | **`104716cdf0ad…`** 同SHA **3,440案**；真傳送後8次已提交效果取消、入鎖前16次誤拒皆歸0。bounded角色marker保留正常換圖，玩家動作／退役／ABA仍嚴格失效，無新增玩家欄位／工作池／名冊。3,340案既有技能／stage／Main回歸通過；特化stamp、Ref及完整Source仍開放。[收據](../../tools/combat-continuation/integration-lifecycle/build/companion-role-progress-current.json)。 |
| WORLD共用Ref續作 | **`718abe6b7a49…`** 同SHA **3,460案**；10個主人場景錯記及2次有效Ref／Joy反饋取消歸0。10筆真實PC佇列各提交一次，角色／生命／跨場景／同ID替換拒絕舊反饋；3,440案原跨角色／Ctrl／範圍／Main回歸通過。特化Proc／AI／毒傷等完整Source餘項仍開放。[收據](../../tools/combat-continuation/integration-lifecycle/build/companion-ref-progress-current.json)。 |
| WORLD特化Proc續作 | **`799c2001838a…`** 同SHA **1,285案**；2次漏治療、2次主人場景誤治療及4次傳送／drain誤取消歸0。實際成對動作鎖＋原容器／物件鎖，22案佇列生命、18案提交後主人表重驗；887案原Proc及352案Ref／成對／Main回歸通過。完整Source、其餘材料與全HP送包排序仍開放。[收據](../../tools/combat-continuation/integration-lifecycle/build/companion-proc-progress-current.json)。 |
| WORLD隨從MP提交續作 | **`30d414f0c0c8…`** 同SHA **480案**；JDI原JAR查詢後8次過期扣款、4次主人場景誤扣及6次退役Frame孤立扣款歸0。延續真實成對／原容器／物件／生命鎖，原數值核心保留；90案九家族及352案Ref／成對／Main回歸通過。完整HP／MP排序與其餘Source仍開放。[收據](../../tools/combat-continuation/integration-lifecycle/build/companion-combat-progress-current.json)。 |
| WORLD隨從MP轉移生命週期 | **`c8520dd42e49…`** 同SHA **498案**；原生查詢後STOPPED造成的6次扣除／收取分裂歸0，6案真實關閉等待，12案STOPPED／DRAINING與6案退役Frame通過。原數值／完成本體保留、所有內部鎖外送包；含352案成對／範圍／Main68及73。完整HP與其餘Source仍開放。[收據](../../tools/combat-continuation/integration-lifecycle/build/companion-mana-progress-current.json)。 |
| WORLD反射HP與真正型別驗證 | **`3386173a0868…`** 同SHA **492案**；6次退役Frame及6次STOPPED後過期HP提交修復，12次真實成對鎖等待。更正舊SUMMON標籤其實是zombie／TAMED，新生命週期與MP測試逐案驗真正型別；真正SUMMON致死保留同一背包與完整歸還，玩家資料寫入0。含Main68／73；完整34技能與Source仍開放。[收據](../../tools/combat-continuation/integration-lifecycle/build/companion-reflection-progress-current.json)。 |
| 技能161 NPC／Ctrl成對生命週期 | **`c6611ff7b009…`** 同SHA **985案**；跨圖、真實等待、提交後退役、競技三型別、普通WORLD與through／攻擊位元驗證，含Main68／73。161完整消費端、167與其餘材料／Source仍開放。[收據](../../tools/combat-continuation/integration-lifecycle/build/companion-forest-progress-current.json)。 |
| 技能161 NPC相消外控 | **`f213c87f8ee2…`** 同SHA **112案**；原始44正常／Ctrl於三種真正NPC與自己／他人遵守161免疫設定，修復24次非免疫殘留。其餘Source仍開放。[收據](../../tools/combat-continuation/integration-lifecycle/build/companion-forest-cancellation-progress-current.json)。 |
| 技能167 Source單體目標修正 | **`4627ff770917…`** 同SHA **626案**；唯讀確認Source area0／through0，非選定角色／NPC誤中降為0；807列不可變啟動目標快照，含Main68／73。舊167第三人斷言更正為行為紀錄；受控NPC狀態／間隔及完整Source仍開放。[收據](../../tools/combat-continuation/integration-lifecycle/build/wind-source-target-progress-current.json)。 |
| 受控NPC167狀態／時鐘／成對生命週期 | **`f158a61a3da7…`** 同SHA **795案**；真實三型別正常／Ctrl、32案含24次跨角色等待、提交後退役、實際Source時鐘參考與33條件矩陣，含Main68／73。NPC施法AI與完整Source仍開放。[收據](../../tools/combat-continuation/integration-lifecycle/build/companion-wind-progress-current.json)。 |
| 材料40／103跨角色／Source單體與相消別名 | **`17d49e194896…`** 同SHA **1128個原生案**＋54個獨立Source參考案；132漏找、玩家16／NPC4次103遠距誤生效歸0，48次真成對等待、提交後退役、40相消別名及48次材料故障回復，含Main68／73。完整材料與Source仍開放。[收據](../../tools/combat-continuation/integration-lifecycle/build/blind-scene-progress-current.json)。 |
| 受控NPC173／174材料交易與成對生命週期 | **`3e8532141119…`** 同SHA **1212案**；原始16次帳本錯誤／8次共用時間窗旁路歸0，三真型別／正常Ctrl、64跨圖情境／提交後退役、48次儲存故障，原PC golden及Main68／73通過。完整材料與Source仍開放。[收據](../../tools/combat-continuation/integration-lifecycle/build/controlled-negative-material-progress-current.json)。 |
| 受控BATTLE NPC87成對准入與發布生命週期 | **`7a982b611cd3…` 同SHA 1671案**；共用時間窗、96次主人鎖等待、舊封包與合法死亡impact、96案記憶體外控、原Main68／73及範圍回歸通過。完整材料／Source仍開放。[收據](../../tools/combat-continuation/integration-lifecycle/build/shock-companion-progress-current.json)。 |
| PC87 BATTLE／WORLD與復活HP生命週期 | **`f460b2b6f310…` 同SHA 2421案**；跨角色／Ctrl、80次目標鎖等待、原生致死／61／75問答復活／再次死亡與原Main／範圍回歸通過。完整材料／Source及整合全回歸仍開放。[收據](../../tools/combat-continuation/integration-lifecycle/build/shock-player-progress-current.json)。 |
| PC87接受後暈眩與特效生命週期 | **`4828aad9f694…` 同SHA 2489原生案**；等待HP時提早狀態／特效33→0，致死與舊生命拒絕，另400個獨立Source片段參考案。完整材料／Source仍開放。[收據](../../tools/combat-continuation/integration-lifecycle/build/shock-accepted-progress-current.json)。 |
| 材料44 WORLD靜態type2替換 | **`7129e48e157a…` 同SHA 1,100原生案**；三個Source固定出生NPC的原NPC重生、新世代與舊封包隔離、跨角色／Ctrl／範圍及Main68／73限定回歸通過。另24個Source片段案獨立計數；隨機／限時／其他工廠及完整材料／Source／最終整合仍開放。[收據](../../tools/combat-continuation/integration-lifecycle/build/static-respawn-progress-current.json)。 |
| PC50正傷害接受後冰凍／消魔與最終送包 | **`f4d1e83ab9a1…` 同SHA 1,074原生案**＋320獨立Source片段；48次成對／32次原生送包鎖等待，提早消魔16／冰凍13與舊封包8均歸0。期限重載錯讀8→0，保留友敵／Ctrl及Main68／73；PC80／194、零／負傷與完整Source仍開放。[收據](../../tools/combat-continuation/integration-lifecycle/build/freeze-duration-progress-current.json)。 |
| PC80範圍冰凍／獨立目標與動畫交接 | **`619ed21e286f…` 同SHA 1,202原生案**；28次範圍取鎖、16次狀態＋12次施法原生送包等待，消魔8／冰凍7、過期扣MP4與動畫前HP4缺口均歸0。原NPC／PC50／87／Main68／73回歸通過；完整Source／零負傷及194仍開放。[收據](../../tools/combat-continuation/integration-lifecycle/build/area-freeze-accepted-progress-current.json)。 |
| 194目前Source單體／射程及接受生命週期 | **`5945812d27e8…` 同SHA 1,434原生案**；實表area0／through0／ranged10，原片段32組合單體。正常／Ctrl、競技及WORLD非指定PC誤中8→0，48次成對／32次送包等待與Main68／73回歸通過；完整Source餘項仍開放。[收據](../../tools/combat-continuation/integration-lifecycle/build/freeze-source-target-progress-current.json)。 |
| 29／44／76 Source視野射程 | **`26552f69b4ef…` 同SHA 1,842原生案**；−1依Source視野及方向邊界，正常／Ctrl、跨角色鎖等待與Main回歸通過。保留29競技場視線。[收據](../../tools/combat-continuation/integration-lifecycle/build/source-visibility-range-progress-current.json)。 |
| BATTLE44受控NPC成對與最終送包 | **`246ef565a3fd…` 同SHA 2,442原生案**；三真型別、正常／Ctrl、競技敵我與自身、原NPC本體重用。168次取鎖、84次提交後退役、72次原生輸出鎖等待、24次真正材料儲存故障／立即重試與Main68／73通過；完整NPC機率／Source仍開放。[收據](../../tools/combat-continuation/integration-lifecycle/build/battle-controlled-cancellation-progress-current.json)。 |
| 76成對／Ctrl及生命週期 | **`de15938d60d2…` 同SHA 3,750原生案**；跨角色NPC與玩家主目標、392次成對等待、192次原生輸出鎖等待、48次材料記憶體故障回復；既有Main68／73及凍結／相消／範圍回歸通過。完整Source管線仍開放。[收據](../../tools/combat-continuation/integration-lifecycle/build/source-mass-slow-lifecycle-progress-current.json)。 |
| 76主目標計算與範圍buff顯示 | **`b2ebe000cd51…` 同SHA 3,814原生案**；64個自身PC／自身三種受控NPC主目標，16個扣費、40個顯示、24個狀態與24個範圍誤中缺口皆歸0。36個Source完整管線原片段參考另計；只改PC與既有helper，原數值／扣費／角色與最終Frame／清理方法皆保留。[收據](../../tools/combat-continuation/integration-lifecycle/build/source-mass-slow-pipeline-progress-current.json)。 |
| 167受控失敗與成對生命週期 | **`df82862d3751…` 同SHA1,712原生案／已計數56,205檢查**；72狀態案重現66個舊消耗／效果差異後歸0，392成對等待、192原生Frame等待、48材料故障重試與Main68／73回歸通過。PC／受控主目標採Source畫面範圍；自然NPC及完整167／Source仍開放。[收據](../../tools/combat-continuation/integration-lifecycle/build/source-wind-target-progress-current.json)。 |
| 競技玩家主目標明確Ctrl | **`323d708e69f7…` 同SHA1,096原生案／已計數12,955檢查**；81個實際解碼端點、7技能76個自身／友方／敵方正常Ctrl施法；正常合法輔助、敵方需明確意圖、原條件及NPC規則保留。56成對、180原生Frame等待及48材料故障重試通過；其餘技能模式／客戶端游標尚未驗收。[收據](../../tools/combat-continuation/integration-lifecycle/build/selected-player-intent-progress-current.json)。 |
| 全技能Source與封包路徑 | 807列加入185列Source符號、48列註冊執行器、180列候選封包解析路徑；234條靜態路徑屬可能呼叫圖，反射、ID分支及逐技能准入仍須驗證，不能當完整覆蓋。[矩陣](../../tools/combat-continuation/integration-lifecycle/build/all-targeted-source-paths-current.json)。 |

## 已完成，沿用不重做

以下「通過」限列出的路徑與測試範圍；完整職業／目標覆蓋仍列於待辦。

| 工作 | 已有成果與界線 |
| --- | --- |
| 共用施法基礎 | 來源 MP／消耗公式、共用動作間隔、材料原子交易、雙玩家固定鎖順序、角色／場景生命週期及延後送包已有候選；續作重用既有 helper。 |
| 四項外控 | `SOLID_CARRIAGE_MODE`、`INVIS_CAST_BREAK`、`Triple_Arrow_Dmg`、`SLAY_BREAK_NOT_TELEPORT` 已接線並有回歸。詳見[外控索引](../../tools/combat-continuation/control-gaps.json)。 |
| 指定技能材料／效果 | 既有治癒、增益、負面、毒性、復活、召喚、變形等專項保留；各技能的玩家／NPC／場景範圍依機器索引，不能把單一路徑推廣成全部完成。 |
| 寵物與變形 | 69 項來源寵物控制、CHA／容量／項圈等已測；變形隨機表唯讀 SQL 21 列與 272 案消費端重播通過。WORLD 馴服／造屍的材料、NPC 世代、延後移除及鎖外權限查詢已修。 |
| 回歸自然 | SUMMON 舊專項保留；WORLD TAMED 已沿用入口建立新物件 ID、移交同一背包、保留 HP／MP、設定零經驗並避免重抽掉落，含排程／登記／模板失敗及跨捕捉世代驗證。BATTLE 普通 Quest／Time Attack 的直接生命值與同一背包已測，含失敗登記回復、重試及超上限受擊；捕捉推進失敗的關卡／計分／目標物回復與既有 GM 模式亦已測。晚到重生回呼的 Hub 關閉邊界已補檢查；Source 馴服的 3,600 秒、十秒共用期限、到期與主人解放已補 100 案；造屍亦已補 TAMED 種類、預載 Source 模板、同一背包、期限及解放，含競爭復活的背包保留。最新 GM 規則物件退役缺口已修；主人狀態 1～5 的群組與收據已補；其 AI 消費端仍待補；一般召喚指令／到期及兩個共用傷害核心的 HP0／背包／期限／送包已通過限定驗證；斷線／移位、其餘致死生產端及完整矩陣仍開放。更廣建構／到期，以及 NPC 超出原生容量的 Source 行為仍待補齊。現行超容量捕捉整筆拒絕保全原物品。 |
| 武器破壞 | 技能 27 已補封包入口的 NPC／主人身分與目標收據，WORLD／BATTLE 跨主人召喚物、Ctrl／STOPPED／延後輸出、材料記憶體失敗回復與 MP 封包撞值已測；父層 `ef781449…` 的限定 2,312 案含七職業矩陣與 Main 68／73 通過；後續 `b20896e6…` 的晚到重生關閉 44 案及 `b6a6accc…` 馴服期限／主人解放 622 案，各自凍結並保留其 SHA 界線。更多效果與外控邊界仍開放。Source BOSS 小數與層數順序已修，五個數值消費端及四條實際 HP 路徑已測；NPC 普攻／伴隨物／跟隨者／物理範圍回呼沿用既有角色鎖與輸出邊界，同 SHA 的 238 案通過。更廣 NPC 傷害核心及最後同 SHA 全回歸仍開放。 |
| NPC 基礎讀取 | 20 項屬性納入啟動唯讀快照，4,138 筆模板可讀；空白體型依來源語意處理，避免逐擊 SQL。這不等同全部 NPC 執行核心已完成。 |
| 玩家相消材料 | BATTLE／WORLD 玩家路徑材料先提交再生效；96 案、8 案故障、56 案生命週期通過，保留原毒性清理。新成功機率專項見下列；NPC 消費端仍待補。 |
| 相消免疫 | 速度狀態 43／54／29／76／152，以及 47／56／71／173／174 已依實際技能 ID 保留免疫與期限；各 240 案通過，材料及既有免疫重播通過。其他別名狀態尚未全覆蓋。 |
| 負重強化 14 | 支援技能表自訂材料；56 案、8 案記憶體故障、40 案生命週期通過。預設零材料封包不變；舊版是先拒絕非零材料，不能誤述成已發生重複扣料。 |

## 接續工作與驗收範圍

| 優先項目 | 現況與下一步 |
| --- | --- |
| **新增使用者回報：全部指定技能目標／Ctrl** | 通暢氣脈術已從共用skills確認為**26、CLAN目標**。已逐列盤點807列與180個原生模型定位；目前只屬靜態清單，客戶端游標／新現場封包／伺服器准入及效果仍待逐技能驗證。合法自身／友方輔助不得要求Ctrl，敵方玩家需明確意圖；保留Source類型／死亡／距離／LOS／免疫、主次範圍及原Ctrl權限邊界。**既有3,814案未驗收這項新政策**。[盤點](../../tools/combat-continuation/integration-lifecycle/build/all-targeted-skill-inventory-current.json)。 |
| 26／42伺服器自施定位 | 正式0.6.71與隔離候選各16個合成封包案：正常／Ctrl、BATTLE／WORLD、兩種材料模式，正確自身object ID可生效，DEX／STR狀態與圖示、MP／HP／冷卻通過。僅證明伺服器合成封包基線；現場游標／送包與新敵方Ctrl政策未驗證，**未宣告使用者回報已修復**。[收據](../../tools/combat-continuation/integration-lifecycle/build/all-targeted-skill-self-server-diagnostic-current.json)。 |
| **新確認的受控NPC缺口** | BATTLE44已完成限定路由／生命週期，BATTLE76原始封包漏找及限定生命週期已補，Source76自身主目標整段計算／範圍buff顯示已限定補齊；其他Source與新指定目標政策仍開放；Source原片段1,472組目標失敗＋1,472組正常施法順序另計，PET／SUMMON不是L1MonsterInstance，不能沿用怪物isErase資格或把效果失敗當成免費拒絕。既有144個NPC射程回歸只證明射程／生命週期，不證明這些Source語意。優先補成對路由及逐目標失敗／扣料／MP／效果順序。[收據](../../tools/combat-continuation/integration-lifecycle/build/source-controlled-failure-analysis-current.json)。 |
| **相消成功機率（44）** | BATTLE／WORLD 玩家已接：自身／血盟／隊伍例外、GM、區域、機率外控、MR／正義及 66／212 身分。176 案封包、8 案材料故障、56 案同盟＋56 案非同盟生命週期、103,063 項公式／邊界比對通過；同 SHA 的 5,164 案與 20＋18 組也通過。來源無非零迴避生產端，不虛構接線。[收據](../../tools/combat-continuation/cancellation-probability/build/qualification-current.json) |
| **相消 NPC 轉換** | type1 名稱／外型／速度、場景隔離及一般動態座標／感知／碰撞已接線；四個 Source 停用 AI ID 與零 HP 啟動限制保留。障礙物回家路徑／LOS 選向及更廣出生／移位語意仍待 Source 全對照。type2 **WORLD＋BATTLE（Quest／Time Attack／GM）** 施法、期限、死亡／房間／關閉清理已接；本輪 276 案替換、12 案材料故障、128 案 NPC 身分與生命週期通過。動畫／模板無效時保留原 NPC 並走一般相消。限已支援的普通 NPC 建構，非完整 NPC factory。[整合收據](../../tools/combat-continuation/cancellation-battle-runtime/build/qualification-current.json)。 |
| **範圍／群體技能** | 66／76／161／167 已補材料一次提交、固定目標與鎖外送包；69 含邀請角色綁定、跨圖回覆／失效清理；165 含玩家、PET、WORLD NPC、Quest／Time Attack 屍體及競態。全部在最終 SHA 重播通過，[收據](../../tools/combat-continuation/cancellation-battle-runtime/build/targeted/receipt.json)。其餘技能／目標仍依待辦。 |
| 其他相消與 NPC／寵物路徑 | WORLD NPC 材料與 PET／SUMMON 已有專項，BATTLE NPC 材料及等待期間換角／死亡／復活／房間重設已納入本輪。玩家相消另重播 296 案機率、材料故障及同盟／非同盟生命週期，[收據](../../tools/combat-continuation/cancellation-battle-runtime/build/player-cancellation/receipt.json)。召喚物轉移／復活競態、毒性調色盤、40／103 與其他別名仍待補。 |
| 傷害與轉換餘項 | 更廣 NPC 建構／到期／關閉、NPC 超容量與長計數 Source 一致性、變形 cause／選單／投射物欄位、尚未證明的 NPC 執行核心。已補四條 NPC 物理回呼的鎖定、角色／物件生命週期與實際 I/O 故障；此專項不代表其他傷害核心已完成。 |
| 外控與完整覆蓋 | 其餘顯示控制尚未完成原生介面驗證；131 傳送仍停用，不能宣稱已驗收。限時物品匯入／穿戴／到期及非材料交易餘項，連同全職業、各目標、Ctrl 強制施法及範圍流程完成最終覆蓋。 |

副本驗證以三個來源任務 NPC 模板在測試記憶體中放入原生支援副本的 1011 地圖；未改 SQL、正式放置資料或放寬地圖限制。TAMED 已有程式與來源差異見[續作稽核](../../tools/combat-continuation/cancellation-static-runtime/build/remaining-source-audit.json)，避免重新建立既有入口。

材料尚有未驗收路徑的技能 ID：`27, 36, 41, 44`，包含部分路徑已完成的技能；其餘未覆蓋目標與競態仍以上表為準。

[最新靜態掃描](../../tools/combat-continuation/cancellation-battle-runtime/build/material-census/receipt.json)剩 **3 個舊呼叫**：[明細](../../tools/combat-continuation/cancellation-battle-runtime/build/material-census/bodies.tsv)。分別為 WORLD 靜態相消後備及兩個 NPC／召喚物預檢方法；不能只依呼叫數判定技能完成。

## 發布門檻與接手方式

1. 先讀本頁及機器索引，驗證輸入 JAR／收據雜湊；以最新候選為起點，重用已完成的交易、權限與生命週期工具。
2. 每個餘項採限定方法修正並保留失敗證據；既有測試只因改動範圍、失敗或尚未驗收而重跑，不重新開發已證明的部分。
3. 使用者指定全部完成後集中人工測試；前次候選遊戲已正常收回，清理已驗證，不再要求中途實測。**同一最終 SHA** 必須通過全部組合與受影響回歸、正式 SQL 啟動、關閉清理、原生介面及必要人工操作，再更新正式版。前層結果不能代替此門檻。
4. 保持來源資料唯讀、戰鬥與購買在記憶體、僅結算寫戰績；無死亡噴裝／掉經驗懲罰。本次未替換正式程式或修改來源資料。

計時驗證紀錄：舊麻痺測試曾碰到 150 毫秒牆鐘容差失敗；未修改原測試的 24 案重跑通過，另以實際延遲送包 250 毫秒的 24 案確認完整持續時間與正確解除。首次失敗、診斷及程式未變證據均[保留](../../tools/combat-continuation/call-nature-material/build/curse-timing-investigation.json)，不把固定延遲容差誤當技能持續時間規則。

## 證據入口

- [候選測試入口](../../tools/combat-continuation/cancellation-battle-runtime/Test-Candidate.bat)：200% 視窗，先核對封存候選與 21 項原生啟動輸入；本輪未自動開啟遊戲。
- [正式版 BUFF／選角驗收](buff-schema-and-character-pages-20261018.zh-TW.md)、[精簡驗收總表](../../VALIDATION.md)。
- [相消來源語意核對](../../tools/combat-continuation/cancellation-material/build/source-control-audit/source-semantics-current.json)、[負重專項](../../tools/combat-continuation/weight-material/build/tests/result.json)、[故障與生命週期](../../tools/combat-continuation/weight-material/build/boundaries/result.json)。
- [精簡前報告封存](../history/combat-reports-before-simplification-20261019.zip)：保留原文與 SHA-256 清單，僅供追溯；舊待辦以本頁及較新收據為準。

維護方式：在本頁更新對應列，其他報告只保留摘要及連結。舊 `update_return_nature_notes.py` 屬逐階段長篇產生方式，後續續作請勿直接執行來覆蓋本頁。


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
