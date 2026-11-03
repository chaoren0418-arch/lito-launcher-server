# 五個零基礎 MP 技能的來源消耗

2026-10-18 隔離候選：`2e07efc0130d9e8d2982f1b36f81ff7b9c837c1c9624ff1efa4f9a5d90fe2228`。補上心靈轉換130、魂體轉換146、龍之護鎧181、燃燒劈砍182、致命身軀191在四個實際方法中缺少的來源MP消耗。**完整Source action／admission／材料／reuse、全部職業與整體目標仍開放，未部署。**

來源 L1SkillUse 的共通公式在baseMP為零時，仍扣除原始智力減免，得到0至-2的消耗；L1Character以long差額計算並夾於0～maxMP。兩個轉換效果先加MP，之後才套用共通消耗。前階段已投影來源公式，本輪直接沿用同一個純核心。

只改ClientSession四個方法：useBodyToMindSpell、useDragonSkinSpell、useBurningSlashSpell、useMortalBodySpell。原selector／狀態／HP不足拒絕、轉換效果、HP提交及reuse body保留；成功路徑在原效果之後套用共通MP消耗。轉換沿用原MP尾段，三個HP技能僅在MP確實改變時新增MP通知；封包內容在原數值區段內保存，交由原sendEncrypted在原PC monitor之外輸出。Source afterCurrentMpMutation對未改變MP不發更新的規則也有對照。沒有新增PC欄位、其他HP／MP權威、worker或timer。

一個新stateless helper與一個既有PC class變更；整個PC class inverse、四個精確方法／store／輸出anchor及其餘父版JAR entry逐位元核對通過。helper只接受本輪五個、原目錄契約baseMP0的技能；其他metadata不會靜默套用這條無MP不足分支的路徑。

同編譯真正handle對照，每邊560個新建角色情境，涵蓋兩種persistence旗標、BATTLE／WORLD、七個職業的原始智力減免、MP0／97／100及HP不足：

- 父版有144個來源數值差異；候選零差異。兩邊各420個成功施法、140個原HP不足拒絕，拒絕者MP不增加、reuse不提交且保留279封包。
- 父版7,621項／2,996 frame；候選8,137項／3,092 frame。96個新增MP frame恰好對應三個HP技能真正改變MP的情境；上限或零變化不多發。轉換的原MP封包反映額外消耗後的真正提交值。
- 所有frame獨立解密，實際PC／common snapshot／completion來自指定JAR；沒有替換PC、執行Source PC、寫入來源或玩家SQL。兩種旗標均使用memory-only store，無state檔案。
- 同一SHA的原20組完整回歸、14組scope／HP／MP／Main／context／角色生命週期與10組既有Light／時間窗／Source核心／輸出I/O／效果控制通過。延續父階段的獨立來源MP oracle，其餘時間、競跑、順序、frame數、角色輸入與期限不變；damage限制2／256／64／64，native fixture嚴格依序執行。
- 兩次相同凍結輸入建置SHA相同，完整稽核及清理後複驗通過。來源兩個轉換executor、ConfigSkill與目前外控檔亦有凍結；BLOODY_SOULADDMP目前20，本輪保留原native轉換效果與控制映射。

本輪證明這四個方法的數值與既有guard／輸出，不等於完整移植它們的Source action鎖、材料交易或scheduler。其他NPC／管理／道具／trigger／area producer、HP／GM特例、每個外控值與並發角色轉換仍需逐項驗收。其餘完整目標與來源倉庫refreshAfterMigration的未移植差異保留在收據；先前actor-idle／Main握手逾時根因沒有由本輪宣稱解決。

保留LiTo競技／隊伍／TA、記憶體角色／道具／技能／XP，僅character_lito／settlement可持久化。正式核心、四個固定基準、來源專案、玩家SQL與結算WAL沒有由本輪改寫。逐路徑／連結／SHA／封存內容核對後清理1929個重複生成檔，保留41個凍結輸入與現行證據。收據：`tools/resource-database/build/native-zero-mp-current.json`；重驗：`python -B -X utf8 tools/resource-database/build/audit-native-zero-mp.py`。
