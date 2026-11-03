# 原生技能 MP 消耗的來源公式與實際呼叫點

2026-10-18 隔離候選：`ca620a2df1e62704bb93ba84dce52b53557b8c9b7d59e59772a50200f854e079`。修正實際玩家技能消費端的 MP 算式，涵蓋153個既有呼叫點與兩個 NPC／召喚相關玩家施法 helper。**四條只處理 HP 的施法路徑、完整 Source admission／材料／reuse 與整體目標仍未完成，未部署。**

從目前來源 L1SkillUse 的 PC MP 數值區段，以及 L1PcOriginal 的原始智力減免，做有限詞彙置換產生純計算核心；原始片段以獨立 data-only accessor 執行作為參考。涵蓋一般技能 INT 分段、騎士87–91的INT減免、正確技能與頭盔的半耗、baseMP大於零時的最小1，最後才扣除原始智力減免。來源允許負消耗；候選在真正 playerMp 寫入處套用來源的0～maxMP邊界，沒有把負消耗先歸零。

三個既有 class 的整體 inverse 核對通過：實際 ClientSession、CombatCompanionSpells 與 CombatNpcDebuffs。新增三個 stateless helper；沒有新增 PC 欄位、worker、timer 或其他 vital 權威。112個具型別 bridge 先直接執行原目錄方法，保留原驗證、例外型別／訊息及直接呼叫語義，再使用共用技能資料與來源數值。其餘父版 JAR entry（含全部技能目錄）逐位元相同。58個原本缺少 INT 的方法額外保存真正 effective INT／原始智力減免；152個真正 PC MP 寫入與兩個原 reflected MP 寫入使用實際接收者 maxMP 夾界，後續 MP snapshot 保存夾界後的值。

驗證：

- 同一編譯的真正 handle 對照，兩種 persistence 旗標及 BATTLE／WORLD，每邊60案。父版44案與目前來源數值不同，候選零差異；每邊468個 frame 獨立解密，封包 MP 等於真正提交值。包含Light／Shield／Weight、原始減免0／1／2，以及只在 JVM 內替換不可變共用快照的baseMP0／1／2、MP99／100邊界。沒有替換 PC、寫入 SQL 或執行來源 PC。
- 全部112個 bridge、173組真正共用目錄資料對照未改寫的來源片段，共18,835項；涵蓋16個INT邊界、減免0～2、頭盔兩值、七職業原始INT0～254、326組與原方法比較的驗證行為、972個負數結果與2,066個舊數值差異。原 interface default 方法有接受部分邊界值者，候選保留其原行為。
- 同一候選雜湊的20組原回歸、14組scope／HP／MP／Main／context／角色生命週期，以及10組既有Light／時間窗／輸出I/O／效果控制通過。前兩組原斷言與期限不變；Light控制的MP預期改由獨立來源片段計算，其餘時間窗、競跑、拒絕、順序、frame數與期限保留。damage資源限制為2／256／64／64，真正native fixture依序執行。
- Light32案、40案時間窗邊界、Source核心52／16／4,628項、HP typed packet context、Main normal／interrupt、STOPPED、輸出停住及50／514／12效果控制均保留。兩次相同凍結輸入建置SHA相同，完整稽核通過。

來源期間另有 L1PcInstance 變更：兩個倉庫 getter 各新增一次 refreshAfterMigration。已保存目前完整來源、歷史雜湊與僅插入這兩行即可還原的逐位元證據。本輪四份消耗公式來源未變；該倉庫遷移刷新行為未由本階段移植或驗收。歷史收據維持原來雜湊。

保留過程紀錄：工具初次編譯的複合var宣告、stdout編碼，以及matrix fixture對繼承interface與非法參數的錯誤假設均已修正並重驗。舊時間窗fixture寫死第一次MP96，來源減免後實際應為98，原失敗保留。六支Light控制只將MP預期換成原始來源片段與真正角色輸入的獨立oracle，不改角色資料或期限；32案原plaintext比較僅正規化已逐案驗證的MP值，其餘bytes與情境資料保持相同。沒有更改目錄驗證規則。原失敗與凍結輸入可透過封存索引追溯。

下一缺口是 useBodyToMindSpell（130／146）、useDragonSkinSpell181、useBurningSlashSpell182、useMortalBodySpell191。它們只有HP消費或轉換效果，未呼叫本輪替換的MP成本方法；來源即使baseMP0仍扣原始減免，心靈／魂體轉換則先加MP再進共通消耗。需完成實際handler正反對照、原HP拒絕／reuse guard及MP輸出snapshot後，才能擴大消耗覆蓋宣告。BLOODY_SOULADDMP目前來源外控為20；既有native控制映射需按實際JAR消費端驗證，不能只從落後的source mirror推論。

完整目標仍包含所有職業技能效果、Ctrl／範圍、完整材料／reuse lease／scheduler、速度狀態與common SprTable、ATTACK／MOVE、NPC／live Item／ability、角色／viewer／callback、交易／rollback、canonical SQL、SharedResourceMain、啟動器／UI及關服。保留LiTo競技／隊伍／TA與記憶體角色／道具／技能／XP，僅character_lito／settlement持久化。先前actor-idle／Main握手逾時根因未由本階段宣稱解決。

依精確路徑、連結、SHA及封存逐內容驗證後清理1467個重複生成檔，保留54個凍結輸入與現行證據。正式核心、四個固定基準、來源專案、玩家SQL與結算WAL沒有由本階段改寫。收據：`tools/resource-database/build/native-skill-consumption-current.json`。重驗：`python -B -X utf8 tools/resource-database/build/audit-native-skill-consumption.py`。
