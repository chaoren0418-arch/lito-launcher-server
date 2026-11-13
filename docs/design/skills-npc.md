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
| C1 | 觸發機率 TriRnd：`nextInt(100)+1 < TriRnd`（嚴格小於） | `MSU:688-699` | `CAT:27-33` `triggerRoll` | 已實作且一致 |
| C2 | 觸發距離 TriRange：負值 `≤|r|`、正值 `≥r` | `R/.../templates/L1MobSkill.java:265-273` | `CAT:20-25` | 已實作且一致 |
| C3 | 多列同時可用時隨機選一列（`nextInt(count)`） | `MSU:224-229` | `S/BattleTimeAttackEncounter.java` `candidateSelector % eligible.size()` | 已實作且一致 |
| C4 | **攻擊位置的 50% 普攻分支**：有可觸發技能時先 `nextInt(2)>=1` → 普攻，否則才擲 TriRnd | `NPC:894-907` | 原本沒有（每次都直接擲 TriRnd，貼身時放技能機率約為 1-伺服器 的 2 倍） | **已實作但不同 → 本次修正**：`CAT` `ordinaryAttackBranch`；Quest／TA 的隨機入口（`aiAttackFor` 與兩個 `nativeSource*AiAttackFor`）呼叫 `applyOrdinaryAttackCoin` |
| C5 | **技能 reuseDelay 屬於施法者**：`L1SkillDelay.tryReserveSkillUse(_user, reuseDelay)`，延遲期間該 NPC 所有 `L1SkillUse` 施法都被拒 | `SU:2675-2676`、`SU:2642`、`SU:2658` | 原本以 actNo 分開計（死亡騎士放完 10056 後仍可立刻放 28） | **已實作但不同 → 本次修正**：`CAT` `CASTER_SKILL_DELAY_KEY`，Quest／TA 的 magic／visual／ground ready 共用同一把鑰匙 |
| C6 | `mobskill.reuseDelay`（每列冷卻） | `MSU:291`、`MSU:684-686` | 33 列全部是 0，LiTo 沒有獨立欄位 | 已實作且一致（值全 0） |
| C7 | leverage：`Leverage>0` 才覆寫，否則 `SU` 預設 10 | `MSU:499-501`、`MSU:655-657`、`SU:150` | `CAT` 45812 寫 10，其餘照表 | 已實作且一致 |
| C8 | 魔法後的動作時間：`target=attack`（且非 18）用 `atk_magic_speed`，其餘用 `sub_magic_speed` | `MSU:517-523` | `CAT` 每列 `actionDelayMillis` | 已實作且一致（逐列見 §4） |
| C9 | ChangeTarget=3：在仇恨名單、觸發距離內、視線可及的玩家中隨機換目標；沒有候選就用原目標 | `MSU:162-170`、`MSU:777-842` | T132（#383）補上：`CAT` `changeTarget`／`changeTargetIndex`；`S/BattleTimeAttackEncounter.java` `aiAttackWithChangeTarget`、`changeTarget`、`rememberHate`；`S/GameHub.java` `timeAttackChangeTargetCandidates`（由 `nativeSourceNpcPlayerFirstAppend` 呼叫） | **已實作且一致（T132 修正）**，細節與剩餘差異見 §6.3。單人時只有原目標一個候選，結果不變 |
| C10 | 選中列之後才檢查 MP／延遲，失敗就改普攻 | `MSU:226`、`NPC:900-906` | LiTo 先把 MP 不足或延遲中的列排除再選 | **已實作但不同（未修，影響小）**：只有「多列可用且其中一列被延遲」時選列機率不同 |
| C11 | 反制魔法（COUNTER_MAGIC）擋 NPC 魔法；`EXCEPT_COUNTER_MAGIC` 與 `SkillCounterMagicExceptionTable` 例外 | `SU:334-344`、`SU:2767-2775`、`SU:2871`、`SU:2899`、`SU:4243-4245` | T132（#383）補上：`CAT` `counterMagicBlocks`／`absorbedByCounterMagic`／`COUNTER_MAGIC_DB_EXCEPTIONS`；`S/ClientSession.java` `consumeCounterMagicAgainstNpcSkill`，由 `serverQuestMonsterMagicAttack` 與 `serverTimeAttackMonsterMagicAttack` 在每個目標算傷害之前呼叫 | **已實作且一致（T132 修正）**，細節見 §6.1 |

## 4. 逐列對照（33 列 `mobskill`）

欄位：Type（1 物理、2 魔法、3 召喚）、Rnd＝TriRnd、Rg＝TriRange、技能＝SkillId。技能數值引用 `SQL` 的 `skills` 列（`dmg`＝damage_value＋dice_count d dice）。

| npc/act | 1-伺服器 `mobskill` | Type／Rnd／Rg／leverage | 技能（`skills` 列） | 目標／範圍 | 傷害／效果 | 動作／特效／延遲 | LiTo | 結論 |
|---|---|---|---|---|---|---|---|---|
| 45278/0 歐吉 | `SQL:34296` | 1／30／-2／20；Range2、AW1、AH2、**ActId30** | — | 前方 1×2 方框（`MSU:590-597`） | 物理 ×2 倍率 | **action 30**（`MSU:660-661`）；atkspeed | `CAT:518` | **已實作但不同 → 本次修正**：原本 ActId 0（送普攻動作 1），改為 30；gfx1020 有 action 30（`src/main/resources/battle/spr-action-380.csv`）。世界版 `S/ClassicWorldMonsterPhysicalSkillCatalog.java:78-79` 本來就是 30 |
| 45584/0 巨大牛人 | `SQL:34448` | 2／20／-3／40 | 62 震裂術 `SQL:48087`（none、area3、mp40、reuse1500、dmg 32+6d10、地） | 自身 3 格 | 地屬性魔法 | 18／1812／sub 1520 | `CAT:414` | 已實作且一致（reuse 1500 現在也是施法者共用，C5） |
| 45584/1 | `SQL:34449` | 1／100／-1／30；Range1 | — | 單體 | 物理 ×3 | 普攻 | `CAT:523` | 已實作且一致 |
| 45601/0 死亡騎士 | `SQL:34475` | 2／20／-3／50 | 28 吸血鬼之吻 `SQL:48053`（attack、range4、mp12、reuse50、dmg 12+3d10、水、吸血） | 單體 | 水屬性＋回血 | 18／236／atk 900 | `CAT:425` | 已實作且一致 |
| 45601/1 | `SQL:34476` | 2／60／-6／30 | 10056 `SQL:48301`（none、area4、mp5、**reuse3000**、dmg 100+10d10、火） | 自身 4 格 | 火屬性 | 19／245／sub 960 | `CAT:431` | 已實作但不同 → **本次修正 C5**：3 秒內不能再放 28 |
| 45601/2 | `SQL:34477` | 3／5／-3；TriCount2；45161×(nextInt(3)+1) | — | 召喚 | 1～3 隻史巴托，最多 2 次 | 19／761／sub 960（`MSU:389-416`） | `CAT:540` | 已實作且一致 |
| 45601/3 | `SQL:34478` | 1／100／-1／20；Range1 | — | 單體 | 物理 ×2 | 普攻 | `CAT:525` | 已實作且一致 |
| 45610/0 古代巨人 | `SQL:34496` | 2／30／-15／30 | 10019 `SQL:48264`（none、area -1＝全畫面、mp5、dmg 100+10d10、地） | 畫面內全部 | 地屬性 | 18／2483／sub 1880 | `CAT:419` | 已實作且一致 |
| 45610/1 | `SQL:34497` | 1／100／-2／30；Range2 | — | 單體 | 物理 ×3 | 普攻 | `CAT:524` | 已實作且一致 |
| 45650/0 殭屍王 | `SQL:34566` | 2／30／-6／20 | 10000 `SQL:48245`（none、area6、mp5、dmg 100+10d10、attr0） | 自身 6 格 | 無屬性 | 表值 action **18**／2806／sub 1800 | `CAT:440` | 已實作但不同（**刻意，不改**）：gfx2803 沒有 action 18，LiTo 送 19、保留 sourceActionId 18。傷害與範圍一致 |
| 45650/1 | `SQL:34567` | 1／40／-2／30；Range2 | — | 單體 | 物理 ×3 | 普攻 | `CAT:526` | 已實作且一致 |
| 45682/0 安塔瑞斯 | `SQL:35109` | 2／60／-5／40 | 31001 `SQL:48770`（attack、range4、area6、dmg 200+8d10、水） | 目標周圍 6 格 | 水屬性 | 30／4156／atk 2240 | `CAT:450` | 已實作且一致（mobskill Range3 魔法不讀，`MSU:475-528`） |
| 45682/1 | `SQL:35110` | 2／10／-3／40 | 10027 安息 `SQL:48272`（attack、range20、dmg 1500+50d10、地、through1） | 單體 | 地屬性；**NPC 喊「$3717」**（`SU:3037-3041`）；不可被反制魔法（`SU:343`） | 30／4156／atk 2240 | `CAT:455` | 已實作且一致（T132 補上 `$3717` 喊話，§6.2）；through 欄未對照 |
| 45682/2 | `SQL:34614` | 2／20／-3／40 | 31004 `SQL:48773`（attack、range6、dmg 75+3d5、地） | 單體 | 地屬性 | 25／129／atk 2240 | `CAT:460` | 已實作且一致 |
| 45682/3 | `SQL:34615` | 2／60／-5／40 | 31002 `SQL:48771`（none、area17、dmg 150+3d5、地） | 自身 17 格 | 地屬性 | 19／2510／sub 2240 | `CAT:465` | 已實作且一致 |
| 45682/4 CT3 | `SQL:34616` | 2／45／-4／40；ChangeTarget3 | 31003 毒霧 `SQL:48772`（buff、range10、type128、無傷害、無持續） | 單體 | `SU` 沒有 31003 特例 → 只有動作與特效 | 18／2510／sub 2240 | `CAT:564`（VISUAL） | 已實作且一致（目標選擇見 C9） |
| 45682/5 CT3 | `SQL:34617` | 1／50／-3／15；Range3、AW3、AH3、ActId5 | — | 前方 3×3 方框 | 物理 ×1.5 | action 5 | `CAT:530` | 已實作且一致（C9） |
| 45682/6 CT3 | `SQL:34613` | 1／80／-3／15；Range3、AW0、AH3、ActId1 | — | 前方直線 3 格 | 物理 ×1.5 | action 1 | `CAT:531` | 已實作且一致（C9） |
| 45682/7 | `SQL:34612` | 2／100／**+8**／40 | 31001（range4） | — | 觸發要 ≥8 格、技能射程 4，永遠放不出 | — | `CAT:572`（INERT） | 已實作且一致 |
| 45683/0 法利昂 | `SQL:34622` | 2／60／-5／29 | 33001 `SQL:48778`（attack、range4、area6、dmg 200+8d10、水） | 目標周圍 6 格 | 水屬性 | 30／4163／atk 1600 | `CAT:470` | 已實作且一致 |
| 45683/1 | `SQL:34623` | 2／10／-3／29 | 10029 安息 `SQL:48274`（attack、range20、dmg 1500+50d10、水、through1） | 單體 | 同 45682/1（喊話、不可反制） | 30／4163／atk 1600 | `CAT:475` | 已實作且一致（T132 補上 `$3717` 喊話，§6.2） |
| 45683/2 | `SQL:34624` | 2／60／-5／29 | 33002 `SQL:48779`（none、area17、dmg 200+3d5、火） | 自身 17 格 | 火屬性 | 19／2510／sub 1600 | `CAT:480` | 已實作且一致 |
| 45683/3 CT3 | `SQL:34625` | 2／45／-4／29；ChangeTarget3 | 33003 `SQL:48780`（attack、range4、area3、dmg 75+3d5、風） | 目標周圍 3 格 | 風屬性 | 18／2510／atk 1600 | `CAT:485` | 已實作且一致（C9） |
| 45683/4 CT3 | `SQL:34618` | 1／50／-3／15；Range3、AW3、AH3、ActId5 | — | 前方 3×3 | 物理 ×1.5 | action 5 | `CAT:532` | 已實作且一致（C9） |
| 45684/0 巴拉卡斯 | `SQL:34499` | 2／60／-5／40 | 31001（標籤寫安塔瑞斯） | 目標周圍 6 格 | 水屬性 | 30／4156／atk 1840 | `CAT:492` | 已實作且一致 |
| 45684/1 | `SQL:34908` | 2／10／-3／40 | 10028 安息 `SQL:48273`（attack、range20、dmg 1500+50d10、火、through1） | 單體 | 同 45682/1 | 30／4160／atk 1840 | `CAT:497` | 已實作且一致（T132 補上 `$3717` 喊話，§6.2） |
| 45684/2 | `SQL:34909` | 2／20／-3／40 | 30004 火牢 `SQL:48769`（buff、range10、type128、**無傷害**、action25、gfx168） | 單體 | 1-伺服器 `SU` 沒有 30004 特例（全原始碼搜不到 30004）→ 推測只有動作與特效 | 25／168／sub 1840 | `CAT:551`（GROUND：3×3 火場 10 秒、每 1.2 秒 25 傷害 ×8，依「同系列」來源） | **已實作但不同（未修，需決策）**：LiTo 比 1-伺服器 多了持續火傷；改成只有特效會削弱 Boss，交主控／使用者決定 |
| 45684/3 | `SQL:34910` | 2／60／-5／40 | 30002 `SQL:48767`（none、area17、dmg 150+3d5、火） | 自身 17 格 | 火屬性 | 19／2510／sub 1840 | `CAT:502` | 已實作且一致 |
| 45684/4 CT3 | `SQL:34911` | 2／45／-4／40；ChangeTarget3 | 30003 `SQL:48768`（attack、range4、area3、dmg 75+3d5、火） | 目標周圍 3 格 | 火屬性 | 18／2510／atk 1840 | `CAT:507` | 已實作且一致（C9） |
| 45684/5 CT3 | `SQL:35106` | 1／50／-3／15；Range3、AW3、AH3、ActId5 | — | 前方 3×3 | 物理 ×1.5 | action 5 | `CAT:533` | 已實作且一致（C9） |
| 45684/6 CT3 | `SQL:35107` | 1／80／-3／15；Range3、AH3、ActId1 | — | 前方直線 3 格 | 物理 ×1.5 | action 1 | `CAT:534` | 已實作且一致（C9） |
| 45684/7 | `SQL:35108` | 2／100／+8／40 | 31000（**`skills` 表沒有這列**） | — | `checkUseSkill` 找不到模板即回 false | — | `CAT:579`（INERT） | 已實作且一致 |
| 45812/0 希爾黛斯 | `SQL:34691` | 2／100／-8／**0**（→ 預設 10） | 38 冰錐 `SQL:48063`（attack、range6、mp18、dmg 32+5d6、水） | 單體 | 水屬性 | 18／11738／atk 1440 | `CAT:409` | 已實作且一致（leverage 見 C7；數值由 `S/BattleSpellCatalog.java:1641-1642` 帶入，gfx 11738、mp18、32+5d6 相同） |

統計（T132 之後）：33 列中 **一致 29 列**（含 T132 補上喊話的 3 列安息）；**T122 修正 2 列**（45278/0、45601/1）＋共通 C4、C5；**T132 修正**共通 C9、C11；**刻意不同 1 列**（45650/0 動作）；**未修 1 列**（45684/2 火牢，需使用者決定）＋共通 C10。

## 5. 已驗證／推測／待驗證

- **已驗證（讀程式與 SQL，行號確認）**：§1 總數（807／588／972／33 以腳本計數）、§3 C1–C11 的 1-伺服器 行為、§4 每列的表值與 LiTo 目錄值；§6 的 DB 例外表內容、`$3717` 在客戶端 `desc-c.tbl` 的文字、8 列 ChangeTarget=3。
- **推測**：
  - 30004 火牢在 1-伺服器 只有特效（搜不到任何 30004 特例；但沒有實際跑 1-伺服器 確認）。
  - 物理技能後的動作時間：1-伺服器 用 `getAtkspeed()`（`MSU:673`），LiTo 走 `NativeSourceNpcBaseSpeeds.npcMillis`（有 stamp 時取 attack interval，與動作編號無關）；沒有逐隻比數字。
  - 10027–10029 的 `through=1` 欄位意義沒有追。
- **待驗證**：
  - 實機：Quest 歐吉放技能時的動作（action 30）是否正常播放；貼身時 Boss 放技能頻率是否約為原本一半；死亡騎士放完 10056 後 3 秒內不再放吸血鬼之吻。
  - Quest 波次被 `BattleControlCatalog` 覆寫時出現的其他 npc。
  - T132 的三項實機觀察（`docs/acceptance/M4-RUNBOOK.md` V39）。

## 6. T132（Issue #383）：反制魔法、安息喊話、換目標

本節的 LiTo 位置用方法名，不寫行號（`S/ClientSession.java`、`S/GameHub.java` 行號常位移）。

### 6.1 反制魔法擋 NPC 魔法（C11）

1-伺服器 的流程：

| 步驟 | 內容 | 出處 |
|---|---|---|
| 1 | `_isCounterMagic` 預設 `true` | `SU:152` |
| 2 | 技能在硬編碼表 `EXCEPT_COUNTER_MAGIC` 內 → 改成 `false` | `SU:334-344`、`SU:2767-2772` |
| 3 | 技能在 DB 表 `skills_魔屏無效技能` 內 → 也改成 `false`（兩張表合併） | `SU:2773-2775`；`R/src/com/lineage/server/datatables/SkillCounterMagicExceptionTable.java:51` |
| 4 | 只有兩種技能類型會問反制魔法：`TYPE_ATTACK`（目標不是施法者，**在算傷害之前**）與 `TYPE_CURSE`／`TYPE_PROBABILITY`（機率判定成功之後） | `SU:2868-2874`、`SU:2884-2902` |
| 5 | 目標有 COUNTER_MAGIC 且 `_isCounterMagic` 為真 → 移除狀態、對周圍與本人播 COUNTER_MAGIC 的 castgfx、把目標從命中清單移除 | `SU:4243-4256`、`SU:2871-2873` |

DB 表 `skills_魔屏無效技能` 的內容（`SQL:49020-49030`，共 11 列）：

| skill_id | 備註欄 | 也在硬編碼表？ |
|---|---|---|
| 44 | 魔法相消術 | 是 |
| 87 | 衝擊之暈 | 是（SHOCK_STUN） |
| 112 | 破壞盔甲 | 是（ARMOR_BREAK） |
| 132 | 三重矢 | 是 |
| 153 | 魔法消除 | **否** |
| 161 | 封印禁地 | 是 |
| 173 | 污濁之水 | **否** |
| 174 | 精準射擊 | **否** |
| 187 | 屠宰者 | **否** |
| 188 | 恐懼無助 | **否** |
| 192 | 奪命之雷 | **否** |

對本範圍的影響：`CAT` 的 18 列魔法（`Definition`）在 `skills` 表的 `type` 全部是 64（`TYPE_ATTACK`，`SQL:48053`、`:48063`、`:48087`、`:48245`、`:48264`、`:48272-48274`、`:48301`、`:48767-48768`、`:48770-48771`、`:48773`、`:48778-48780`），目標都是玩家，所以都會問反制魔法。其中 10027／10028／10029 在硬編碼表內 → 不擋；其餘 15 列會被擋。DB 表的 11 個技能都不是本範圍的怪物技能，所以 DB 表對這 18 列沒有影響；LiTo 仍把它照抄成常數 `COUNTER_MAGIC_DB_EXCEPTIONS`，之後若加入新怪物技能不會漏。31003（毒霧）與 30004（火牢）是 `type` 128，1-伺服器 不問反制魔法，LiTo 的 VISUAL／GROUND 流程也沒有加。

LiTo 的做法：
- 判定：`CAT.counterMagicBlocks(skillId)`＝`SourceSkillTargeting.counterMagicApplies`（既有的硬編碼表，T106）且不在 `COUNTER_MAGIC_DB_EXCEPTIONS`。
- 消耗：`CAT.absorbedByCounterMagic` 呼叫既有的 `BattleTimedStatus.consumeCounterMagic`（玩家對玩家用的同一個）。例外技能不會呼叫它，狀態保留。
- 位置：`ClientSession.serverQuestMonsterMagicAttack` 與 `serverTimeAttackMonsterMagicAttack` 的每個目標迴圈開頭（擲傷害骰之前）。被擋的目標：送 `publishCounterMagicConsumption`（既有的 `S_EFFECT`，gfx 取 `CounterMagicSpell.consumeEffectGfx`），傷害 0，不做後續的受傷、反射、吸血處理。
- 範圍技：每個目標各自判定，只有開著反制魔法的那一位被擋。
- 玩家對玩家的反制魔法程式沒有改。

與 1-伺服器 的差異（刻意）：被擋的目標仍留在攻擊封包的目標清單裡（命中旗標 0），與既有的經典世界怪物魔法流程 `serverClassicWorldMonsterMagicAttack` 相同；1-伺服器 是把目標從清單拿掉。玩家看到的結果都是「沒有受傷＋反制魔法特效」。

### 6.2 安息 10027／10028／10029 的喊話

- 1-伺服器：`SU:3037-3041`，10026–10029 每個進入效果計算的目標執行一次 `_user.broadcastPacketX8(new S_NpcChat(_npc, "$3717"))`。`S_NpcChat`（`R/src/com/lineage/server/serverpackets/S_NpcChat.java:20-26`）＝`S_OPCODE_NPCSHOUT`、color 0、npc id、字串 `nameId + ": " + chat`。X8＝8 格內的玩家（`R/src/com/lineage/server/model/Instance/L1NpcInstance.java:4138-4140`）。
- LiTo 的封包：**有**。`S/Protocol.java` `sayCode`（`S_SAY_CODE` **0x2D**，格式 `cdshh`：type、object id、字串、x、y；註解記錄的客戶端 receiver 是 `sub_4A3140`）。既有用法：精靈守護者警告（`ClientSession.serverClassicWorldGuardianWarning`，字串同樣是 `nameId + ": $" + 訊息編號`）、修理 NPC 台詞；測試 `ProtocolNpcSpeechTest`。`docs/reference/client-protocol-needs.md` 沒有這個 opcode 的列（該文件範圍是角色／道具／戰鬥／技能，沒有收聊天類）；本次沒有另外反組譯 `LiTo.bin`，證據等級同該文件的 B。
- 文字：`$3717` 在客戶端 `Text.pak` 的 `desc-c.tbl` 是「給你安息」；`$1116`／`$1426`／`$1605` 是「安塔瑞斯」／「法利昂」／「巴拉卡斯」（用 PakViewer CLI 解出後以 CP950 讀取，第 N+2 行對應 `$N`）。
- LiTo 的做法：`ClientSession.publishNpcRestShout`，在 Quest／TA 兩個魔法流程裡對每個沒被反制魔法擋下的目標送一次 `sayCode(0, 龍的 object id, "<nameId>: $3717", 龍的 x, y)`，只送給 8 格內的收件人（`CAT.restShoutReaches`）。三個技能都是單體，所以每次施放一則。
- 待驗證：實機畫面上文字的位置與顏色（客戶端 type 0 的顯示方式沒有另外查）。

### 6.3 ChangeTarget=3（C9）

1-伺服器（`MSU:182-232`、`:777-842`）：
1. 逐列檢查前，ChangeTarget>0 的列先呼叫 `changeTarget`，把 `_target` 換掉；其餘列用 AI 原目標（`:206-213`）。之後的觸發距離、機率檢查都對換過的目標做（`:215`）。
2. `CHANGE_TARGET_RANDOM`（3）的候選：怪物看得到的玩家／寵物／召喚獸中，在該列觸發距離內（`:800`）、`glanceCheck` 通過（`:805`）、在仇恨名單（`:809`）、沒死（`:813`）、不是幽靈（`:818-822`）。
3. 沒有候選 → 維持目前目標（`:827-828`）；有 → `nextInt(size*100)/100`（`:831-833`）。
4. 仇恨名單的來源：AI 選到的目標（`R/src/com/lineage/server/model/Instance/L1MonsterInstance.java:619`）與打過它的人（`:981`、`:1183-1209`）。

本範圍 ChangeTarget=3 的 8 列：45682 的 act 4／5／6、45683 的 act 3／4、45684 的 act 4／5／6（`SQL:34616`、`:34617`、`:34613`、`:34625`、`:34618`、`:34911`、`:35106`、`:35107`）；其餘 25 列是 0。

LiTo 的做法：
- `GameHub.nativeSourceNpcPlayerFirstAppend`（Time Attack 分支）把房間內所有 `battleAiTargetEligible` 的玩家做成候選（object id、座標、`BattleSpellCatalog.hasNativeLineOfSight` 的結果），交給 `BattleTimeAttackEncounter.aiAttackWithChangeTarget`。
- encounter 對每一列：ChangeTarget=3 時用該列的觸發距離、視線、仇恨名單過濾候選，再以 `changeTargetIndex(size, nextInt(size*100))` 選一位；該列的觸發距離、施法距離、面向都改用這位玩家。沒有候選就用 AI 原目標。
- 仇恨名單（每隻怪一份 `hateObjectIds`）：AI 每次鎖定的目標；`GameHub.damageTimeAttackActor` 與 `nativeSourcePhysicalTimeDamage` 成功造成傷害的玩家。
- 選中的列換了目標時，`MonsterAiTick` 的目標、面向與來源檢查物件（`NativeSourceNpcPlayerView.retarget`）一起換成那位玩家，之後的傷害流程不變。
- 固定骰模式（`timeAttackMonsterActNoFixture`／`…MagicFixtureTriggerRoll`）下，候選索引＝`timeAttackMonsterCandidateFixtureSelector % size`。
- 單人：候選只有原目標，選到的還是他，結果與修改前相同（測試 `changeTargetRandomKeepsSinglePlayerBehaviour`）。

與 1-伺服器 的剩餘差異：
- 候選只有玩家；寵物與召喚獸不列入（LiTo 的競技場怪物 AI 本來就只鎖玩家）。
- 隱身的玩家不是候選（`battleAiTargetEligible` 排除隱身；1-伺服器 `changeTarget` 沒有排除）。
- 毒（`nativeSourcePoisonTimeDamage`）與立方（`nativeSourceCubeTimeDamage`）造成的傷害沒有記進仇恨名單；只用這兩種方式打過龍、又從未被龍鎖定的玩家不會成為候選。
- LiTo 的 AI 每一拍重新選最近的玩家當原目標，被選過的都進仇恨名單；1-伺服器 只有在重新搜尋目標時才加。
- Quest Battle 沒有接（Quest 的怪沒有 ChangeTarget 列）。
