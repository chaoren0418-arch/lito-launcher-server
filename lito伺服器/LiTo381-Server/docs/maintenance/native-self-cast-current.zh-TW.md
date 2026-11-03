# 四個自施技能的來源 action、跨技能時間窗與輸出

2026-10-18 隔離候選：`71ea214a486cb7d25f5b2f01aa7a533a0dd7f4ea4d4b3cf993c251588ff8b664`。實際Shield3、Shadow Armor99、Earth Skin151、Decrease Weight14沿用原數值與效果body，進入真正PC既有Source action lock、角色Owner時間窗與完成輸出FIFO。**完整Source cast admission／材料／scheduler、其他職業／Ctrl／範圍與整體目標仍未完成，未部署。**

16組真正handler反例先確認：兩種persistence旗標、BATTLE／WORLD、Shield→Shield、Weight→Weight、Shield→Light及Light→Shield，在原reuse後仍能於第一個1480ms動畫窗內接受第二次。原Source時間窗以相同兩個實際時間拒絕第二次，共240個frame獨立解密。修正後同編譯對照各案僅一次，120個frame；沒有改角色、時間、reuse或在情境內清空時間窗。

既有NativeLightAction／NativeCastTimeline八個C0 helper class重新編譯與父版逐位元相同，才以有限來源差異產生C1。沿用同一組ingress、Batch、receipt及canonical角色Owner；dispatcher識別四個新增技能，保留實際socket／handle入口的Source packet arrival。沒有新增PC欄位、ThreadLocal、worker、timer或其他HP／MP權威，只在既有Batch增加一個captureSelf metadata boolean。

原useShieldSpell與useWeightSpell完整body保留為alias，原PC monitor entry／exit改為POP，由外層實際Source action管理生命週期。原selector、狀態、HP／MP拒絕、效果、數值、reuse與封包計算保留；Source admission放在原guard之後、首次作用之前。Weight特別放在原inventory.copy之前，拒絕時間窗時不先做背包複製或暫存材料移除。

原sendEncrypted整個body保留為nativeSelfOriginalSend；僅captureSelf生效時保存clone過的bytes與原接收者／note，實際輸出在解鎖後排出。原network clock／capture／bookkeeping仍在原alias執行，沒有重播數值。既有Batch逐輸出檢查角色publication權限，遇IOException停止後續輸出；producer的current／ingress／packet在Source drain之前還原。非自施範圍與原Light流程保留原路徑。

驗證：

- 16個相同編譯的真正MP socket barrier對照：父版卡在PC wire monitor，候選在舊MP尚未放行前完成原數值與reuse，保留兩個receipt且零輸出超車。每邊140個frame逐一明文相同，放行後FIFO、MP快照與scope還原通過。
- 256個新建真正角色的邊界，3,076項、1,088 frame：HP／MP拒絕及原訊息、同角色立即恢復有效請求、原解析例外型別／訊息、外層主要例外identity、提交前角色／場景替換、提交後同名角色退役，以及輸出失敗不重播／不重開Source時間窗。HP7只在JVM內替換不可變共用快照，SQL未改。一般角色original INT10用來確實觸發正消耗；未清空情境內時間窗。
- 32個STOPPED情境、542項、168 frame：四技能、兩旗標、BATTLE／WORLD、有／無應用程式。停服前保留的真正dispatcher完成並建立正確publication metadata；停服後新請求與物品授權仍拒絕，沒有重開背包交易權限。
- 八個真正Main情境：四技能各normal／interrupt，共50 frame，逐frame等於舊父版真正I/O情境的golden紀錄。Main仍等候既有MP與新self兩個accepted receipt，repository各關閉一次、關閉後零呼叫。新請求在Source窗口自然到期後才試closed gate，沒有藉由舊時間窗掩蓋關閉規則；原關閉、join與啟動期限保留。
- 同一SHA原20組完整回歸、14組scope／HP／MP／Main／context／角色生命週期與十組既有Light／時間窗／Source核心／效果控制通過。另重播560個零MP技能與60個來源消耗對照，候選差異均零。原damage限制2／256／64／64，native fixture依序執行；兩次建置SHA相同，完整稽核及清理後複驗通過。

過程證據保留：初版8e5雖通過20組回歸及16／16對照，bytecode盤點顯示Weight admission仍位於inventory.copy之後，故移到copy前並以新SHA重驗。初次工具的局部變數命名衝突保留；boundary fixture初次比較到反射傳輸用InvocationTargetException，改為像既有native控制一樣直接還原原例外cause，保留原解析型別／訊息與期限。這些舊結果不作為現行SHA的驗收。

本輪生命週期首次在舊gate測試失敗，原紀錄保留。舊觀測把source鎖任意重新被持有也計為原外層action尚未結束；刻意在該completion的root.closed為true之後，由另一執行緒取得真正source鎖，可使原斷言必定誤報。現階段只調整複製fixture的觀測，直接取queue上該completion的原root；每個peer write額外斷言root.closed，即使鎖當下沒人持有也檢查。原need斷言、封包、receipt與等待期限保留，歷史共用fixture和production均未改。相同編譯68／68情境的刻意重取鎖對照通過，1,454／2,134 checks，各136 frame；一般生命週期另以此有限觀測修正重驗。確切差異、原失敗與診斷收於gate-observation-correction.json。

目前四技能的共用metadata須itemConsumeId0；此階段沒有移植完整Source材料交易、reuse lease與scheduler失敗／expiry。其他技能／管理／道具／trigger／area producer、全部外控值、速度狀態（含STATUS_BRAVE3）／common SprTable、ATTACK／MOVE、raw／nested鎖、role/source/generation、viewer／callback census、NPC／live Item／ability、canonical SQL、SharedResourceMain／啟動器／UI／關服與來源倉庫遷移刷新仍需完成。先前actor-idle／Main握手偶發逾時根因沒有由本輪宣稱解決。

保留LiTo競技／隊伍／TA、記憶體角色／道具／技能／XP，僅character_lito／settlement可持久化。正式核心、四固定基準、來源專案、玩家SQL與WAL未由本輪改寫。依精確路徑、連結、SHA與封存內容驗證後清理2100個重複生成檔，保留65個凍結輸入。收據：`tools/resource-database/build/native-self-cast-current.json`；重驗：`python -B -X utf8 tools/resource-database/build/audit-native-self-cast.py`。
