# 0.6.68 戰鬥／技能同源稽核

日期：2026-10-14。結論：**共用來源資料與已移植的規則很多，但 LiTo 並不等同目前「1-伺服器」的完整戰鬥引擎，也沒有完整同步其所有外控。**

本輪只讀檢查；未修改正式 JAR、DLL、SQL、設定或正在執行的遊戲。比較的是 LiTo 正式發布 JAR，以及磁碟上「1-伺服器」的原始碼／設定與目前共用 380_db；沒有宣稱已驗證「1-伺服器」正在執行的 JAR 與其原始碼完全一致。

## 核對方法

正式 JAR SHA-256：`c02aaae8d54a27e30460be2e3c47709ee64d35f007a8be27ac74a327268b9539`，與 deployment 發布清單一致。使用該 JAR 執行唯讀反射稽核，先載入正式資源映射，再比對共用 SQL。不是用完整 src 重新建置；Modern 候選檔案不算原生正式功能。

對 212 個具有相應 catalog 欄位且 SQL 有定義的技能，比對 576 個 MP／HP 基礎消耗、冷卻毫秒、持續秒數欄位：575 相符、1 個表面差異。此統計不涵蓋全部目標判定、額外消耗修正、傷害公式、狀態疊加、動畫與技能實測，也不是「212 個技能全部通過」。部分技能／保留槽沒有這四種可比欄位，不能依缺少欄位判為未實作。

唯一欄位差異為 skill208 骷髏毀壞：LiTo 的暈眩 2 秒、skills.buffDuration 為 3；但 `1-伺服器/src/com/lineage/server/model/skill/skillmode/BONE_BREAK.java:34` 同樣固定 `time=2`。這是 SQL 與技能特殊執行器的語義差異，不是已證實的 LiTo 行為缺陷。

## 具體發現

| 項目 | 判定與影響 |
| --- | --- |
| 基礎技能參數 | 本輪 576 欄位比對高度一致；光箭、加速術、聖結界、三重矢、龍之護鎧等抽樣記錄在 evidence.txt。但 LiTo 多數值是 Java catalog 中固定的已核對版本，沒有通用載入 skills 數值的接線；改 SQL 後不能假設兩端會同步 |
| 基礎物理／魔法流程 | 有物理命中、AC 隨機減傷、武器／箭矢、屬性、MR、裝備減免、無視減免、遠近減免、負面與 BUFF 等實作。這些是 LiTo 自己的 Battle*／ClientSession 實作，不是直接執行 L1AttackPc／L1MagicPc／L1SkillUse |
| 生命的祝福 | **確認缺少目前來源的額外補血 +100。** 來源 ConfigSkill 設定 NATURES_BLESSING_HEAL=100，L1SkillUse.java:2935 在治癒流程追加；LiTo useNaturesBlessingSpell 直接使用擲骰＋正義修正後進入水之元氣／恢復量修正，未套用該額外項。正式 bytecode 已核對 |
| 玩家 AC 額外減傷 | **確認防禦核心少了目前來源的額外 abs(AC)/10 層。** 來源 L1AttackPc.java:979 在原本 calcPcDefense 後再扣這一層；目前 ConfigPcDamage.AC_REDUCTION_DIVISOR=10。LiTo BattlePhysicalDefense.againstPlayer 只合計 AC 隨機減傷、有效裝備減免及遠近減免，沒有此額外層。不能將所有 AC 減傷都稱為缺少；缺的是來源新增那一層 |
| 職業／技能外控 | 來源 5-職業傷害設定表與 4-職業_技能設定表沒有直接載入 LiTo。當前某些值剛好相同，例如聖結界除數 2、雙重破壞 15%／1.5 倍、召喚物對玩家係數 0.3；LiTo 是固定值，使用者修改來源外控不會自動反映 |
| 負面技能與 BUFF 資料表 | 來源會使用 skills_負面技能控制、skills_buff_ability、skills_負面boss免疫。LiTo 有對應技能效果／固定參數，但正式 JAR 未發現這三張表的讀取名稱，現行 SQL loader 也未載入它們。不是「沒有負面魔法」，而是尚未做到表控完全共用；來源調整機率、CON/WIS 修正、耐性、能力值或免疫時會有漂移風險 |
| 特效、圖示、地圖 | ResourceDatabase 確實讀取 LiTo 專用資源映射。runtime-values 欄位是 actionId、effectGfx 等呈現資料；圖示／特效共用不代表上述戰鬥數值也已共用 |
| 模式／敵友／結算 | 刻意不同。LiTo 用房間、隊伍、Time Attack 合作規則及本次 Ctrl 指定目標例外；角色、商店、狀態在記憶體，只保存 LiTo 戰績。天堂世界的 PK、安全區、死亡與持久化不可不分場合直接照搬 |

## 同步建議與驗收界線

先補 **生命的祝福額外治癒、AC 額外減傷** 這兩項已證實差異，再統一「戰鬥數值來源」：技能 MP／HP／冷卻／時間／傷害參數、BUFF 能力、負面機率、BOSS 免疫，以及已選定要共用的職業／技能外控。LiTo 專有資源映射與房間／結算規則應保持獨立。

驗證要在相同角色、裝備、目標、技能狀態與固定隨機數下，逐層比較命中、基礎傷害、倍率、減傷、最終 HP／MP、狀態與到期。現有回歸證明個別功能可運作，不等於兩套伺服器逐項完全相同。本輪沒有修正、部署或全面重新跑遊戲測試。

## 證據

- `combat-parity-0.6.68-evidence.txt`：正式 JAR catalog 與 SQL 唯讀輸出、欄位數量及唯一差異。
- `tools/resource-database/CombatParityAudit.java`／`run_combat_parity_audit.py`：可重跑稽核；輸出是當下 DB 的快照，重跑可能隨管理者調整而變化。
- `tools/resource-database/build/combat-parity-selected.javap.txt`：正式 ClientSession 治癒、魔法、召喚物傷害的必要 bytecode 片段；可重建產物。
- `1-伺服器/src/com/lineage/server/model/L1AttackPc.java:979`、`L1MagicPc.java:399`、`L1MagicPc.java:739`、`skill/L1SkillUse.java:2935`。
- `1-伺服器/config/其他控制端/4-職業_技能設定表.properties`、`5-職業傷害設定表.properties`。
- LiTo `BattleSpellCatalog.java`、`BattlePhysicalDefense.java:90`、`ClientSession.java:73125`、`ResourceDatabase.java`。
