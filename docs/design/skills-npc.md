# 競技場 PvE 怪物技能對照（Quest／Time Attack）

範圍依 `docs/design/gameplay.md` D5＝A：只盤點 Quest Battle（wire 2）與 Time Attack（wire 4）會出現的怪物與 Boss。T122（Issue #353）。

引用縮寫：
- `R/` ＝ `D:\lineage開發\1-伺服器`（唯讀參考）；`SQL` ＝ `R/db/380_db.sql`。
- `S/` ＝ `lito伺服器/LiTo381-Server/src/main/java/local/lito381/server/`；`CAT` ＝ `S/BattleTimeAttackMagicSkillCatalog.java`。
- 1-伺服器 怪物技能主流程：`R/src/com/lineage/server/model/L1MobSkillUse.java`（下稱 `MSU`）；施法：`R/src/com/lineage/server/model/skill/L1SkillUse.java`（下稱 `SU`）；AI 入口：`R/src/com/lineage/server/model/Instance/L1NpcInstance.java`（下稱 `NPC`）。

## 1. 總數

| 項目 | 數量 | 說明 |
|---|---|---|
| `skills` 表全部列 | 807 | `SQL:47987` 起 |
| 非玩家列（skill_id > 220） | **588** | 同上，與 `docs/reports/lito-capability-current.md:121` 一致 |
| 本範圍用到的非玩家列 | **16**（＋1 個不存在的 31000） | 10000、10019、10027、10028、10029、10056、30002、30003、30004、31001、31002、31003、31004、33001、33002、33003 |
| 本範圍用到的玩家列 | 3 | 28 吸血鬼之吻、38 冰錐、62 震裂術 |
| 本範圍以外的非玩家列 | **572** | 不做（D5 B／C 留 Backlog） |
| `mobskill` 表全部列 | 972 | `SQL:34258` 起 |
| 本範圍怪物的 `mobskill` 列 | **33** | 9 隻怪物；LiTo `CAT` 剛好 33 列（MAGIC 18、PHYSICAL 10、SUMMON 1、GROUND 1、VISUAL 1、INERT 2） |

## 2. 怪物清單

| 模式 | npc | 名稱 | 出處（LiTo） | 1-伺服器 `mobskill` 列 |
|---|---|---|---|---|
| Quest | 45262 烏龜龍、45207 冰人、45289 穆里安、45259 蠍子、45322 活鎧甲、45390 牛人、45380 梅杜莎 | — | `S/BattleQuestEncounter.java:379-394` | 無（`NativeQuestBattleMobskillSourceAuditTest` 已逐一確認） |
| Quest | 45278 | 歐吉 | `S/BattleQuestEncounter.java:387-388` | 1 列 |
| Quest／TA | 45601 | 死亡騎士 | `S/BattleQuestEncounter.java:395-396`；`S/BattleTimeAttackEncounter.java:474` | 4 列 |
| Quest／TA | 45161 | 史巴托（死亡騎士召喚物） | `S/BattleQuestEncounter.java:411-412`；`S/BattleTimeAttackEncounter.java:484` | 無 |
| TA | 45147、45182 | 一般波次 | `S/BattleTimeAttackEncounter.java:492-495` | 無 |
| TA | 45812 | 希爾黛斯 | `S/BattleTimeAttackEncounter.java:498` | 1 列 |
| TA | 45584 巨大牛人、45610 古代巨人、45650 殭屍王 | Boss | `S/BattleTimeAttackEncounter.java:470-476` | 2／2／2 列 |
| TA | 45682 安塔瑞斯、45683 法利昂、45684 巴拉卡斯 | 最終龍（隨機） | `S/BattleTimeAttackEncounter.java:478-482` | 8／5／8 列 |

Quest 的波次也可由 `BattleControlCatalog` 依 DB 覆寫（`S/BattleQuestEncounter.java:436-438`）；覆寫後若出現上表以外的 npc，**不在本表**（待驗證）。

NPC 施法速度（`SQL` npc 表）：45584 `:35464`、45601 `:35481`、45610 `:35490`、45650 `:35523`、45682–45684 `:35553-35555`、45812 `:35683`、45278 `:38888`。

## 3. 共通行為（每列都會經過）

| # | 行為 | 1-伺服器 | LiTo | 結論 |
|---|---|---|---|---|
| C1 | 觸發機率 TriRnd：`nextInt(100)+1 < TriRnd`（嚴格小於） | `MSU:688-699` | `CAT:25-31` `triggerRoll` | 已實作且一致 |
| C2 | 觸發距離 TriRange：負值 `≤|r|`、正值 `≥r` | `R/.../templates/L1MobSkill.java:265-273` | `CAT:18-23` | 已實作且一致 |
| C3 | 多列同時可用時隨機選一列（`nextInt(count)`） | `MSU:224-229` | `S/BattleTimeAttackEncounter.java` `candidateSelector % eligible.size()` | 已實作且一致 |
| C4 | **攻擊位置的 50% 普攻分支**：有可觸發技能時先 `nextInt(2)>=1` → 普攻，否則才擲 TriRnd | `NPC:894-907` | 原本沒有（每次都直接擲 TriRnd，貼身時放技能機率約為 1-伺服器 的 2 倍） | **已實作但不同 → 本次修正**：`CAT` `ordinaryAttackBranch`；Quest／TA 的隨機入口（`aiAttackFor` 與兩個 `nativeSource*AiAttackFor`）呼叫 `applyOrdinaryAttackCoin` |
| C5 | **技能 reuseDelay 屬於施法者**：`L1SkillDelay.tryReserveSkillUse(_user, reuseDelay)`，延遲期間該 NPC 所有 `L1SkillUse` 施法都被拒 | `SU:2675-2676`、`SU:2642`、`SU:2658` | 原本以 actNo 分開計（死亡騎士放完 10056 後仍可立刻放 28） | **已實作但不同 → 本次修正**：`CAT` `CASTER_SKILL_DELAY_KEY`，Quest／TA 的 magic／visual／ground ready 共用同一把鑰匙 |
| C6 | `mobskill.reuseDelay`（每列冷卻） | `MSU:291`、`MSU:684-686` | 33 列全部是 0，LiTo 沒有獨立欄位 | 已實作且一致（值全 0） |
| C7 | leverage：`Leverage>0` 才覆寫，否則 `SU` 預設 10 | `MSU:499-501`、`MSU:655-657`、`SU:150` | `CAT` 45812 寫 10，其餘照表 | 已實作且一致 |
| C8 | 魔法後的動作時間：`target=attack`（且非 18）用 `atk_magic_speed`，其餘用 `sub_magic_speed` | `MSU:517-523` | `CAT` 每列 `actionDelayMillis` | 已實作且一致（逐列見 §4） |
| C9 | ChangeTarget=3：在仇恨名單、觸發距離內、視線可及的玩家中隨機換目標；沒有候選就用原目標 | `MSU:162-170`、`MSU:777-842` | 沒有；永遠打 AI 當前目標 | **已實作但不同（未修）**：單人時結果相同；多人 TA 時龍的 8 列（見 §4 標 CT3）目標分布不同。需要 GameHub 傳入候選玩家，交下一棒 |
| C10 | 選中列之後才檢查 MP／延遲，失敗就改普攻 | `MSU:226`、`NPC:900-906` | LiTo 先把 MP 不足或延遲中的列排除再選 | **已實作但不同（未修，影響小）**：只有「多列可用且其中一列被延遲」時選列機率不同 |
| C11 | 反制魔法（COUNTER_MAGIC）擋 NPC 魔法；`EXCEPT_COUNTER_MAGIC` 與 `SkillCounterMagicExceptionTable` 例外 | `SU:334-344`、`SU:2767-2775`、`SU:2871`、`SU:2899`、`SU:4243-4245` | Quest／TA 的 NPC 魔法流程（`S/ClientSession.java` 約 `:54440-54600`）只有反射（counter mirror），沒有反制魔法 | **未實作（未修）**：玩家開反制魔法時，1-伺服器 會擋掉一次龍／Boss 魔法（10027–10029 例外）；交下一棒 |

## 4. 逐列對照（33 列 `mobskill`）

欄位：Type（1 物理、2 魔法、3 召喚）、Rnd＝TriRnd、Rg＝TriRange、技能＝SkillId。技能數值引用 `SQL` 的 `skills` 列（`dmg`＝damage_value＋dice_count d dice）。

| npc/act | 1-伺服器 `mobskill` | Type／Rnd／Rg／leverage | 技能（`skills` 列） | 目標／範圍 | 傷害／效果 | 動作／特效／延遲 | LiTo | 結論 |
|---|---|---|---|---|---|---|---|---|
| 45278/0 歐吉 | `SQL:34296` | 1／30／-2／20；Range2、AW1、AH2、**ActId30** | — | 前方 1×2 方框（`MSU:590-597`） | 物理 ×2 倍率 | **action 30**（`MSU:660-661`）；atkspeed | `CAT:417` | **已實作但不同 → 本次修正**：原本 ActId 0（送普攻動作 1），改為 30；gfx1020 有 action 30（`src/main/resources/battle/spr-action-380.csv`）。世界版 `S/ClassicWorldMonsterPhysicalSkillCatalog.java:78-79` 本來就是 30 |
| 45584/0 巨大牛人 | `SQL:34448` | 2／20／-3／40 | 62 震裂術 `SQL:48087`（none、area3、mp40、reuse1500、dmg 32+6d10、地） | 自身 3 格 | 地屬性魔法 | 18／1812／sub 1520 | `CAT:313` | 已實作且一致（reuse 1500 現在也是施法者共用，C5） |
| 45584/1 | `SQL:34449` | 1／100／-1／30；Range1 | — | 單體 | 物理 ×3 | 普攻 | `CAT:422` | 已實作且一致 |
| 45601/0 死亡騎士 | `SQL:34475` | 2／20／-3／50 | 28 吸血鬼之吻 `SQL:48053`（attack、range4、mp12、reuse50、dmg 12+3d10、水、吸血） | 單體 | 水屬性＋回血 | 18／236／atk 900 | `CAT:324` | 已實作且一致 |
| 45601/1 | `SQL:34476` | 2／60／-6／30 | 10056 `SQL:48301`（none、area4、mp5、**reuse3000**、dmg 100+10d10、火） | 自身 4 格 | 火屬性 | 19／245／sub 960 | `CAT:330` | 已實作但不同 → **本次修正 C5**：3 秒內不能再放 28 |
| 45601/2 | `SQL:34477` | 3／5／-3；TriCount2；45161×(nextInt(3)+1) | — | 召喚 | 1～3 隻史巴托，最多 2 次 | 19／761／sub 960（`MSU:389-416`） | `CAT:439` | 已實作且一致 |
| 45601/3 | `SQL:34478` | 1／100／-1／20；Range1 | — | 單體 | 物理 ×2 | 普攻 | `CAT:424` | 已實作且一致 |
| 45610/0 古代巨人 | `SQL:34496` | 2／30／-15／30 | 10019 `SQL:48264`（none、area -1＝全畫面、mp5、dmg 100+10d10、地） | 畫面內全部 | 地屬性 | 18／2483／sub 1880 | `CAT:318` | 已實作且一致 |
| 45610/1 | `SQL:34497` | 1／100／-2／30；Range2 | — | 單體 | 物理 ×3 | 普攻 | `CAT:423` | 已實作且一致 |
| 45650/0 殭屍王 | `SQL:34566` | 2／30／-6／20 | 10000 `SQL:48245`（none、area6、mp5、dmg 100+10d10、attr0） | 自身 6 格 | 無屬性 | 表值 action **18**／2806／sub 1800 | `CAT:339` | 已實作但不同（**刻意，不改**）：gfx2803 沒有 action 18，LiTo 送 19、保留 sourceActionId 18。傷害與範圍一致 |
| 45650/1 | `SQL:34567` | 1／40／-2／30；Range2 | — | 單體 | 物理 ×3 | 普攻 | `CAT:425` | 已實作且一致 |
| 45682/0 安塔瑞斯 | `SQL:35109` | 2／60／-5／40 | 31001 `SQL:48770`（attack、range4、area6、dmg 200+8d10、水） | 目標周圍 6 格 | 水屬性 | 30／4156／atk 2240 | `CAT:349` | 已實作且一致（mobskill Range3 魔法不讀，`MSU:475-528`） |
| 45682/1 | `SQL:35110` | 2／10／-3／40 | 10027 安息 `SQL:48272`（attack、range20、dmg 1500+50d10、地、through1） | 單體 | 地屬性；**NPC 喊「$3717」**（`SU:3037-3041`）；不可被反制魔法（`SU:343`） | 30／4156／atk 2240 | `CAT:354` | 已實作但不同（未修）：傷害一致；缺 `$3717` 喊話（玩家可見）；through 欄未對照 |
| 45682/2 | `SQL:34614` | 2／20／-3／40 | 31004 `SQL:48773`（attack、range6、dmg 75+3d5、地） | 單體 | 地屬性 | 25／129／atk 2240 | `CAT:359` | 已實作且一致 |
| 45682/3 | `SQL:34615` | 2／60／-5／40 | 31002 `SQL:48771`（none、area17、dmg 150+3d5、地） | 自身 17 格 | 地屬性 | 19／2510／sub 2240 | `CAT:364` | 已實作且一致 |
| 45682/4 CT3 | `SQL:34616` | 2／45／-4／40；ChangeTarget3 | 31003 毒霧 `SQL:48772`（buff、range10、type128、無傷害、無持續） | 單體 | `SU` 沒有 31003 特例 → 只有動作與特效 | 18／2510／sub 2240 | `CAT:463`（VISUAL） | 已實作且一致（目標選擇見 C9） |
| 45682/5 CT3 | `SQL:34617` | 1／50／-3／15；Range3、AW3、AH3、ActId5 | — | 前方 3×3 方框 | 物理 ×1.5 | action 5 | `CAT:429` | 已實作且一致（C9） |
| 45682/6 CT3 | `SQL:34613` | 1／80／-3／15；Range3、AW0、AH3、ActId1 | — | 前方直線 3 格 | 物理 ×1.5 | action 1 | `CAT:430` | 已實作且一致（C9） |
| 45682/7 | `SQL:34612` | 2／100／**+8**／40 | 31001（range4） | — | 觸發要 ≥8 格、技能射程 4，永遠放不出 | — | `CAT:471`（INERT） | 已實作且一致 |
| 45683/0 法利昂 | `SQL:34622` | 2／60／-5／29 | 33001 `SQL:48778`（attack、range4、area6、dmg 200+8d10、水） | 目標周圍 6 格 | 水屬性 | 30／4163／atk 1600 | `CAT:369` | 已實作且一致 |
| 45683/1 | `SQL:34623` | 2／10／-3／29 | 10029 安息 `SQL:48274`（attack、range20、dmg 1500+50d10、水、through1） | 單體 | 同 45682/1（喊話、不可反制） | 30／4163／atk 1600 | `CAT:374` | 已實作但不同（未修）：缺 `$3717` 喊話 |
| 45683/2 | `SQL:34624` | 2／60／-5／29 | 33002 `SQL:48779`（none、area17、dmg 200+3d5、火） | 自身 17 格 | 火屬性 | 19／2510／sub 1600 | `CAT:379` | 已實作且一致 |
| 45683/3 CT3 | `SQL:34625` | 2／45／-4／29；ChangeTarget3 | 33003 `SQL:48780`（attack、range4、area3、dmg 75+3d5、風） | 目標周圍 3 格 | 風屬性 | 18／2510／atk 1600 | `CAT:384` | 已實作且一致（C9） |
| 45683/4 CT3 | `SQL:34618` | 1／50／-3／15；Range3、AW3、AH3、ActId5 | — | 前方 3×3 | 物理 ×1.5 | action 5 | `CAT:431` | 已實作且一致（C9） |
| 45684/0 巴拉卡斯 | `SQL:34499` | 2／60／-5／40 | 31001（標籤寫安塔瑞斯） | 目標周圍 6 格 | 水屬性 | 30／4156／atk 1840 | `CAT:391` | 已實作且一致 |
| 45684/1 | `SQL:34908` | 2／10／-3／40 | 10028 安息 `SQL:48273`（attack、range20、dmg 1500+50d10、火、through1） | 單體 | 同 45682/1 | 30／4160／atk 1840 | `CAT:396` | 已實作但不同（未修）：缺 `$3717` 喊話 |
| 45684/2 | `SQL:34909` | 2／20／-3／40 | 30004 火牢 `SQL:48769`（buff、range10、type128、**無傷害**、action25、gfx168） | 單體 | 1-伺服器 `SU` 沒有 30004 特例（全原始碼搜不到 30004）→ 推測只有動作與特效 | 25／168／sub 1840 | `CAT:450`（GROUND：3×3 火場 10 秒、每 1.2 秒 25 傷害 ×8，依「同系列」來源） | **已實作但不同（未修，需決策）**：LiTo 比 1-伺服器 多了持續火傷；改成只有特效會削弱 Boss，交主控／使用者決定 |
| 45684/3 | `SQL:34910` | 2／60／-5／40 | 30002 `SQL:48767`（none、area17、dmg 150+3d5、火） | 自身 17 格 | 火屬性 | 19／2510／sub 1840 | `CAT:401` | 已實作且一致 |
| 45684/4 CT3 | `SQL:34911` | 2／45／-4／40；ChangeTarget3 | 30003 `SQL:48768`（attack、range4、area3、dmg 75+3d5、火） | 目標周圍 3 格 | 火屬性 | 18／2510／atk 1840 | `CAT:406` | 已實作且一致（C9） |
| 45684/5 CT3 | `SQL:35106` | 1／50／-3／15；Range3、AW3、AH3、ActId5 | — | 前方 3×3 | 物理 ×1.5 | action 5 | `CAT:432` | 已實作且一致（C9） |
| 45684/6 CT3 | `SQL:35107` | 1／80／-3／15；Range3、AH3、ActId1 | — | 前方直線 3 格 | 物理 ×1.5 | action 1 | `CAT:433` | 已實作且一致（C9） |
| 45684/7 | `SQL:35108` | 2／100／+8／40 | 31000（**`skills` 表沒有這列**） | — | `checkUseSkill` 找不到模板即回 false | — | `CAT:478`（INERT） | 已實作且一致 |
| 45812/0 希爾黛斯 | `SQL:34691` | 2／100／-8／**0**（→ 預設 10） | 38 冰錐 `SQL:48063`（attack、range6、mp18、dmg 32+5d6、水） | 單體 | 水屬性 | 18／11738／atk 1440 | `CAT:308` | 已實作且一致（leverage 見 C7；數值由 `S/BattleSpellCatalog.java:1641-1642` 帶入，gfx 11738、mp18、32+5d6 相同） |

統計：33 列中 **一致 26 列**；**本次修正 2 列**（45278/0、45601/1）＋共通 C4、C5；**刻意不同 1 列**（45650/0 動作）；**未修 4 列**（3 列安息喊話、45684/2 火牢）＋共通 C9、C10、C11。

## 5. 已驗證／推測／待驗證

- **已驗證（讀程式與 SQL，行號確認）**：§1 總數（807／588／972／33 以腳本計數）、§3 C1–C11 的 1-伺服器 行為、§4 每列的表值與 LiTo 目錄值。
- **推測**：
  - 30004 火牢在 1-伺服器 只有特效（搜不到任何 30004 特例；但沒有實際跑 1-伺服器 確認）。
  - 物理技能後的動作時間：1-伺服器 用 `getAtkspeed()`（`MSU:673`），LiTo 走 `NativeSourceNpcBaseSpeeds.npcMillis`（有 stamp 時取 attack interval，與動作編號無關）；沒有逐隻比數字。
  - 10027–10029 的 `through=1` 欄位意義沒有追。
- **待驗證**：
  - 實機：Quest 歐吉放技能時的動作（action 30）是否正常播放；貼身時 Boss 放技能頻率是否約為原本一半；死亡騎士放完 10056 後 3 秒內不再放吸血鬼之吻。
  - Quest 波次被 `BattleControlCatalog` 覆寫時出現的其他 npc。
