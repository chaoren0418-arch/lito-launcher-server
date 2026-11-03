"""Replace one bounded status section; historical evidence stays at its pinned paths."""
from pathlib import Path
R=Path(__file__).resolve().parent;S=R.parents[1]
p=S/'docs/maintenance/combat-continuation-current.zh-TW.md';text=p.read_text('utf-8');marker='## 2026-10-19 變形與回歸自然續作'
if marker in text:text=text[:text.index(marker)].rstrip()+'\n'
text+='''
## 2026-10-19 變形與回歸自然續作

正式維持 0.6.69，未修改來源資料庫、來源服、正式 JAR 或 DLL。以下候選與材料專項計數不代表全技能同源化完成；完整發布仍須同一最終 SHA 的回歸、SQL 啟動／關閉清理及原生介面驗證。

| 候選 | 已驗證範圍 | 尚未通過的範圍 |
| --- | --- | --- |
| `polymorph-material`／`a1b7d339…` | 材料前後各 128 案、故障前後各 8 案、生命週期 56 案、原生選單 80 案；4,712 案前層組合重播、241 案寵物消費端重播通過。材料專項 47／58。 | 未完成這個 SHA 的 20＋18 組發布回歸；隨機資料同源另列下方。 |
| `return-nature-material`／`280dcb25…` | 材料前後各 136 案、故障前後各 8 案、36 案角色／實體／STOPPED／送包生命週期通過。 | 主人物品返還故障確實存在，不能單獨發布。 |
| `return-nature-transfer`／`54a781b5…` | 同一施法者與另一主人、BATTLE／WORLD、物品核心有無安裝均驗證；修正後材料 136 案、主人物品故障前後各 8 案及生命週期 36 案通過。另 20 案驗證關服前已接納返還、背包滿載、兩筆以上地面物品的共同回滾；原 8 次部分返還歸零，故障後冷卻結束可重試。C0、方法逆向比對及獨立重建一致。 | 5,164 案組合重播與 241 案寵物控制重播通過；最終整合／發布回歸未完成。TAMED 的 WORLD／Quest／Time Attack 原 NPC 恢復交易不屬於此 SUMMON 修正，仍待補。 |
| `polymorph-source`／`32d88c6b…` | 已建置及重建一致；來源 `WandPolyTable.getRandomPolyId` 原文比對 5,088 案／5,097 檢查通過。新讀取放入現有唯讀 SQL 交易，固定百萬分母，未配置機率回到 29，不重新正規化。 | 實際唯讀 SQL 的 6 案／1,100 檢查及 1,063 次選取通過，讀取 21 筆權重；272 案原生材料、故障、生命週期與選單重播通過。不同 cause 的不變身回應與變身投射物欄位仍未驗收。目前不支援的 cause 會於啟動檢查明確拒絕，沒有默默套錯外觀。 |

新增 `pet-material`／`16879896…` 已通過獨立重建、241 案既有寵物控制重播，以及 WORLD 馴服術／造屍術前後各 72 案：共用動作間隔繞過 8 → 0，材料記憶體故障後部分扣款 8 → 0，正常封包相同。此 SHA 的 20＋18 組回歸已全部通過；不能因此宣稱 Quest／Time Attack 或 NPC 世代切換已驗收。

`pet-world-lifetime`／`18738d9d…` 已建置、重建一致，WORLD 原生前後各 32 案通過：過期目標／送包問題 16 → 0，沒有持鎖送包。修正以既有 NPC 身分 token 辨識死亡／重生；不再借用可被取消流程清除的 curse marker。首次測試抓到馴服／造屍封包格式差異及舊標記不可靠，失敗版本與紀錄已封存。72 案材料、241 案寵物及 5,164 案組合回歸均已通過。保護範圍是 WORLD NPC 排隊期間身分／重生世代、怪物群替換與舊移除封包；BATTLE encounter 尚未納入。尚不能發布或視為整個技能已完成。

`pet-world-publication`／`09e3ce8c…` 補上同一 actor 槽位恢復可見時的延後移除封包檢查；前後各 4 案實測，過期移除封包 2 → 0；32 案生命週期、72 案材料及 241 案寵物重播通過。尚未完成此 SHA 的 20＋18 組發布回歸。

`weapon-break-numeric`／`a58a6d1f…` 僅修正武器破壞的數值函式：整數乘法改為來源的浮點乘除，保留有層數時至少 1 點。實際候選與抽出的來源公式前後各 121,110 案，差異 98,616 → 0。ASM 語意 C0、單方法逆向驗證與獨立重建通過；原 class 的 debug metadata 與重新編譯不同，因此沒有以整份原始碼覆蓋 class。原生 BattleTournamentNpcPhysicalDamage 計算消費端前後各 3,696 案，差異 1,022 → 0；另四個消費端、實際 HP 提交與完整回歸仍待驗收，不能宣稱完整 NPC 傷害公式或 skill27 材料已完成。來源資料庫沒有改動。

`npc-physical-source`／`b6a149aa…` 將 NPC 屬性放入既有唯讀啟動交易。9 案／4,181 檢查，以及 20＋18 組回歸均通過；原生 repository 查詢不再建立逐次 SQL 連線或累積 NPC 快取。首次共用來源不符會在查詢前拒絕。方法逆向驗證確認 BUFF 讀取未被改動。

`weapon-break-material`／`d3e31773…` 材料 96 案與施法者記憶體故障前後各 8 案通過，但停服測試失敗，保留失敗證據。`pet-world-policy`／`3dd19a51…` 前後各 32 案確認冷啟動權限查詢離開 WORLD 監視鎖，持鎖查詢 44 → 0，SQL 故障沒有半完成施法，NPC 在查詢期間更換後會拒絕舊目標。

`weapon-break-lifetime`／`221518da…` 修復 WORLD 經 BATTLE 轉送時重複建立施法接納的問題。96 案材料、8 案施法者故障、56 案角色／場景／STOPPED／送包生命週期，以及 32 案權限讀取重播通過。另 8 案實際目標耐久記憶體故障，已證明原有目標回滾有效，未多改一套物品核心。NPC／召喚物生命週期尚未完整驗收。

`npc-physical-size`／`9636aaa6…` 查明 1,524 筆原拒絕資料全是空白體型；來源 L1AttackPc 對近戰回到 small 傷害、投射物取非 large 分支。候選僅在 LiTo 的單一 large 布林屬性轉換相容空字串，未改 SQL。10 案／5,706 檢查通過，4,138 筆均可讀，沒有無效資料；同一 SHA 的 5,164 案組合回歸與 20＋18 組回歸已全部通過。較新的相消候選須另行驗證，不能直接沿用這個結果。

`cancellation-material`／`08b49d26…` 已建置與重建一致，將 BATTLE／WORLD 玩家相消的材料交易放到效果前，維持原毒素清理範圍與封包尾段。兩個指定方法逆向驗證、前後各 96 案正常封包、前後各 8 案記憶體故障與 56 案生命週期均通過；故障後部分提交 8 → 0。舊版故障場景本來會先送相消特效，fixture 現在明確檢查這個差異，原失敗紀錄保留。特殊 WORLD NPC 相消仍是另一條路徑，尚未納入，不列為全技能完成。

相消唯讀 SQL 核對（`cancellation-material/build/sql-audit/result.json`）確認：skill44 為 TYPE_PROBABILITY（1），負面技能表基礎機率 20、包含魔法命中；目前玩家原生相消消費端沒有機率判斷，須另補，不能因材料測試通過便宣稱同源。來源硬編碼與 `skills_魔屏無效技能` 均讓相消穿過魔法屏障，此行為應保留。`skills_相消怪物轉換` 有 4 列，現有特殊 NPC 分支尚未證明完整接入此表；保留待辦，不修改來源 SQL。

`cancellation-speed-immunity`／`345640f9…` 僅接入玩家 skill44 的兩個指定呼叫點，讓加速／強力加速及緩速／集體緩速／地面障礙按實際技能 ID 套用既有免疫清單，保留原期限。其他狀態、NPC、死亡與離場清理不被這個 adapter 改寫。已建置、重建一致及兩呼叫逆向驗證，前後各 240 案實際封包測試通過，免疫失效 80 → 0；208 案材料、故障、生命週期及毒性清理回歸通過；未列為整個相消技能完成。

`cancellation-debuff-immunity`／`9eb812b8…` 只補弱化術 47、疾病術 56、藥水霜化術 71、污濁之水 173、精準射擊 174 的明確單一狀態欄位，沿用共用免疫清單並保留原期限。前後各 240 案，免疫失效 80 → 0；208 案材料／故障／生命週期／毒性重播與 240 案既有速度免疫重播通過。只有原免疫 adapter 的 class 改動，其餘 entry 相同；未聲稱所有相消狀態或機率已完成。

`weight-material`／`f2217019…` 修正負重強化 14 不接受共用技能表自訂材料的限制，同時移除接納後的舊扣料／儲存段。前後各 56 案確認預設零材料封包相同，自訂材料不再拋出 metadata 例外，剛好足量能施法、材料不足回 299、HP／MP 不足及共用動作間隔保留；8 案實際記憶體故障及 40 案角色／場景／STOPPED／送包生命週期通過。舊版先被 metadata guard 擋住，不能描述為舊版實際重複扣料；初版修補遺漏 guard 的失敗原型已封存。只改原 ClientSession 的一個方法並加入定向接納 helper，逆向驗證及獨立重建一致。材料的 58 項完成計數不因此增加，因預設 skill14 不需材料。最新靜態掃描剩 12 個舊材料呼叫，不等於 12 個未完成技能。

物品故障證據：`return-nature-material/build/owner-transfer-audit/result.json`。原流程已移動背包與召喚物物品後才存入主人的記憶體紀錄，故障時只回滾紀錄。新流程持有已接納施法的雙方身分與鎖定，在記憶體提交成功後才改背包與召喚物；溢出地面物品使用既有可回滾入口，失敗保留整包，沿用原生失敗訊息。未引入玩家來源 SQL 寫入。

本輪 fixture 修正保留原失敗：選單應送原生 `death 52`，`death knight` 常數不是 monlist 選項；溢出測試改用已在實際目錄中的 20281 戒指，不以未載入的 4 號武器作替身。未因此放寬伺服器選單或物品驗證。各 build 收據保留候選、凍結輸入與結果雜湊，最新總覽為 `tools/combat-continuation/continuation-current.json`。
'''
p.write_text(text,encoding='utf-8')
for relative in ['VALIDATION.md','docs/maintenance/combat-integration-current.md']:
 p=S/relative;t=p.read_text('utf-8');parts=t.split('\n\n',2)
 parts[1]='戰鬥續作（2026-10-19，未發布）：已核對既有候選，未重做前層實作。NPC 空白體型相容候選 `9636aaa6…` 通過同一 SHA 的 5,164 案組合及 20＋18 組回歸。其後玩家相消材料候選通過正常、故障與生命週期測試；五種加減速免疫子候選 `345640f9…` 通過前後各 240 案及 208 案重播。較新的子候選與尚未接入的相消成功機率、NPC 轉換、其餘材料／範圍技能等仍有獨立門檻，不能將前層通過視為全部技能完成。正式維持 0.6.69，來源資料庫未改動。'+('[候選、證據與待辦](docs/maintenance/combat-continuation-current.zh-TW.md)。'if relative=='VALIDATION.md'else'[候選、證據與待辦](combat-continuation-current.zh-TW.md)。')
 t='\n\n'.join(parts).replace('尚未發佈的正式 0.6.68 不會因此改變行為','正式 0.6.69 尚未接入這批候選，不會因此改變行為');p.write_text(t,encoding='utf-8')
