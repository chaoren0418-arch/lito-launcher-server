# 來源 reuse lease、原生角色接點與 scheduler 收據生命週期

2026-10-18 隔離候選：`35853617c9b92cfba01a55e64599454c697064b6f272b6d25285c38100295e9b`。完整原始L1SkillDelay／Lease算法已透過真正原生角色、既有scheduler與日誌接點執行，Light2、Shield3、Decrease Weight14、Shadow Armor99、Earth Skin151的真正入口已接入。**完整材料交易、其他職業／Ctrl／範圍／trigger及整體目標仍未完成，未部署。**

原Source兩個類別的整個body保留，僅映射型別與一個scheduler receiver；原L1Character五個token方法逐body比對。每個canonical角色持有獨立AtomicReference token，舊timer／cancel／相容clear不能釋放新token。provisional timer不會提前開門；commit以真正提交時間重新起算完整reuse，首次排程拒絕撤銷token並拒絕cast，重排程失敗維持關閉。

沿用原NativePlayerLifetime.retry，沒有新scheduler、worker或ThreadLocal。直接STOPPED原本會先關retry，即使已接受的cast正在等待真正動作鎖；在停止原有新工作、queue及NPC流程後，現在由原vital receipt數量保留既有executor，最後一筆receipt釋放才完成關閉。整個shutdown／release逆向比對只容許一個teardown分支與一個原counter後的hook，原drop、等待、interrupt與收據計算保留。兩筆收據控制確認第一筆釋放不能提前關閉scheduler；沒有重開物品交易授權。

接點的actor只保存真正PC、canonical Owner、原lifetime及建立時的角色ID與原Source token。角色替換時，舊actor仍保留原ID，並只能修改原token。原ClientSession class逐位元等於父版，原數值／效果／輸出body未在本輪重建；八個既有helper entry改變、八個新增class，既有timeline State只增加一個角色metadata引用。Light與四自施技能先保留原資源／狀態／解析guard，再依Source順序取得action及reuse reservation；排程拒絕會撤銷action，沒有先扣MP或提交原效果。原Source action runtime token遺失時保留記錄並commit reuse的fail-closed次序。

驗證摘要：

- 同一份編譯的真正入口120／120組，兩個persistence旗標、BATTLE／WORLD、有／無item application、五技能及正常／排程拒絕／MP拒絕。初版40組在scheduler已停止後仍施法，候選歸零；正常與資源拒絕的解密快照逐位元相同。10秒reuse只在JVM不可變共用快照中覆寫，SQL未改。
- lease kernel96／96組，包含真實provisional timer、提交重定期限、取消／提交競速、stale timer、角色退役、相容clear、首次與後續真實排程拒絕；STOPPED已接受請求的8個差異歸零。另32個metadata對照確認角色ID與雙收據關閉邊界，各自8個差異歸零。
- 既有self STOPPED32、I/O16／16、Main8、邊界256、跨技能時間窗16／16及Light STOPPED／Main通過。I/O每邊140 frame明文相同；八個Main共50 frame仍等於原父版golden，repository各關一次、關閉後零呼叫。
- 同一SHA完整20項回歸、14項scope／HP／MP／Main／context／角色生命週期及十項既有控制通過；另重播560個零MP與60個來源消耗案例，候選差異均零。native fixture依序執行，damage限制仍2／256／64／64，原斷言與期限保留。
- Source編譯只需L1Character／L1Object／GeneralThreadPool三個原始type class及log4j API；完整凍結後以隔離classpath編譯，這些Source type不會安裝進原生JAR。兩次重現只讀相同凍結輸入、完全不讀可變Source classpath，得到相同SHA；21個已驗證class另有精確清單，封存後不能藉由目錄不存在而略過核對。

過程中的工具失敗與反例保留：最初方法比對工具漏建MethodNode header；fixture誤將AtomicBoolean當boolean讀取；Light原有MP拒絕是silent，不能假設與self一樣送278；初次javac dependency log使用本機編碼，現明確UTF-8。這些修正沒有改原始Source算法、放寬期限或修改production來配合fixture。舊角色ID與STOPPED scheduler問題都有同編譯前後反例，早期候選不冒充現行SHA。

目前五個入口仍採已安裝的no-material範圍，完整Source材料memory transaction、rollback及全部cast producers尚未完成。其他外控值、STATUS_BRAVE3／SprTable、ATTACK／MOVE、raw／nested鎖、role／source／generation、viewer／callback census、NPC／live Item／ability、canonical SQL、SharedResourceMain／登入器／UI／關服及來源倉庫遷移刷新仍需完成。先前actor-idle／Main握手偶發逾時也未由本輪宣稱全部解決。

保留LiTo競技／隊伍／TA、記憶體角色／道具／技能／XP，僅character_lito／settlement可持久化。正式核心、基準、來源專案、玩家SQL與WAL未由本輪改寫。精確路徑、連結、SHA與封存內容核對後，整理2013個重複生成檔，保留69個凍結輸入。清理後完整稽核已通過。收據：`tools/resource-database/build/native-reuse-lease-current.json`；重驗：`python -B -X utf8 tools/resource-database/build/audit-native-reuse-lease.py`。
