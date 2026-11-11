# 騎士／黑暗妖精／王族／龍騎士／幻術師 62 個技能：1-伺服器 ↔ LiTo 行為對照（T109，Issue #309）

- 參考（唯讀）：`D:\lineage開發\1-伺服器`。引用寫「相對路徑：行號」。縮寫：`U` = `src/com/lineage/server/model/skill/L1SkillUse.java`，`M` = `src/com/lineage/server/model/skill/L1SkillMode.java`（`_skillMode.put` 那一行），`sm/` = `src/com/lineage/server/model/skill/skillmode/`，`AP` = `src/com/lineage/server/model/L1AttackPc.java`，`PI` = `src/com/lineage/server/model/Instance/L1PcInstance.java`，`CK` = `src/com/lineage/config/ConfigSkill.java`，`cfg` = `config/其他控制端/4-職業_技能設定表.properties`，`db` = `db/380_db.sql`，`ab` = `db` 的 `skills_buff_ability`。
- LiTo：路徑相對於 `lito伺服器/LiTo381-Server/src/main/java/local/lito381/server/`。`C` = `BattleSpellCatalog.java`（本分支行號，catalog 紀錄 `new XxxSpell(` 那一行），`CS` = `ClientSession.java`。
- 範圍：`lito伺服器/LiTo381-Server/docs/maintenance/combat-coverage-current.md` 的 SQL 分組——騎士 87～91（5）、黑暗妖精 97～112（16）、王族 113～118（6）、龍騎士 181～192（12）、幻術師 193～195＋201～220（23），共 62 個（保留槽 92～96、119～121、196～200 不在內）。註：193～195 在 SQL 分組是幻術師等級 25，但 `src/com/lineage/list/PcLvSkillList.java:230` 由龍騎士學習；本文沿用覆蓋表分組，不影響比對。
- 做法與 T107／T108 相同（`docs/design/skills-wizard.md`、`docs/design/skills-elf.md`）：目標判定由 T106（`docs/design/skill-targeting.md`），機率公式由 T102（`docs/design/formulas-magic.md`），本文不重複。
- 測試：`lito伺服器/LiTo381-Server/src/test/java/local/lito381/server/OtherClassSkillServer1ParityTest.java`；輸入表 `src/test/resources/server1/skills-others-62.tsv`（`tools/skill-targeting/extract_others.py` 從 `db` 唯讀擷取，16 欄與 T107 相同）。
- 判定：**一致**＝本文列出的面向逐值相等（有單元測試）；**已修正**＝本 PR 改 LiTo；**不一致・未改**＝理由寫在 §4；效果本體的封包與實機行為一律**待驗證**。

## 1. 共通流程（62 個技能）

| # | 面向 | 1-伺服器 | LiTo | 判定 | 測試 |
|---|---|---|---|---|---|
| O1 | 數值來源 | `db` `skills` | 正式環境 `CombatDatabase.java:62-75` 讀同一張表覆蓋 12 個欄位（`:12-15`）；208 期限另有固定值（`CombatDatabase.java:147`） | 62 個都有 catalog 紀錄；消耗／冷卻／期限／材料後備值與 `db` 逐欄相等 | `consumptionCooldownAndDurationMatchSource` |
| O2 | MP 消耗 | `U:1869-1971`：INT 減免只到技能 80；騎士 87～91 再減 `INT−12`（`U:1913-1916`）；SQL MP>0 時最低 1；之後減原始 INT 減免 | `NativeSourceSkillConsumptionMath.mpCost` | 一致（INT 1..40 × 減免 0..2 × 頭盔，62 個逐值） | `runtimeMpCostMatchesIsHpMpConsume` |
| O3 | 原始 INT 減免 | `src/com/lineage/server/model/Instance/L1PcOriginal.java:1013-1062`：王族 11–12→1、≥13→2；騎士 9–10→1、≥11→2；黑妖 13–14→1、≥15→2；龍騎 0；幻術 14→1、≥15→2 | `NativeSourceSkillConsumptionMath.originalReduction`（type 0/1/4/5/6） | 一致（0..30 逐值） | `originalIntelligenceReductionMatchesL1PcOriginal` |
| O4 | HP 消耗 | `U:1974`：`currentHp < hpConsume+1` 拒絕（訊息 279） | 各 record `hpCost`；`CS:89366` `playerHp <= hpCost` 拒絕 | 一致；龍騎士 12 個 MP 全為 0、只扣 HP | `consumptionCooldownAndDurationMatchSource` |
| O5 | 108 會心一擊 | `U:2036-2038`：扣費改成 HP=1、MP=0 | `AttackSpell.finalBurn`、`hpCost()`=1（`C:8394-8395`）；`CS:89386-89390` | 一致 | `finalBurnCommitsHpOneAndMpZero` |
| O6 | 材料 | 黑暗精靈石 40321 ×1／×2；覺醒骨片 49157 ×1／×2；魔法立方 49156 ×1／×2 | `InventoryCatalog.SECOND_LEVEL_DARK_STONE`、`AWAKENING_BONE_FRAGMENT`、`49_156L` | 一致（11 個有材料的技能逐列相等） | 同上 |
| O7 | **正義值** | `U:2047` 每次施放加 `skills.lawful`；62 列只有 103 為 −10 | 有 `lawfulDelta` 的 record 才會呼叫 `applySpellLawfulDelta`（`CS:46731`，delta=0 不送封包） | **已修正** 208（catalog +45 → 0，§3.1）；103 由 #325 補上（`BlindSpell.lawfulDelta`=−10，§4.1） | `damageAttributeTypeAndLawfulMatchSource`、`lawfulGapListIsExact`、`boneBreakDoesNotChangeLawfulAndKeepsModeDuration` |
| O8 | 傷害骰 | `damage_value`＋`damage_dice`×`damage_dice_count` | `baseDamage`／`dieSides`／`diceCount` | 一致（87、108、184、189、192、194、203、207、208、213）；202 是 TYPE_CURSE 不算傷害，catalog 以 `dormantDatabaseDamageValue`=11 保留 | `damageAttributeTypeAndLawfulMatchSource`、`confusionKeepsDormantDatabaseDamageValue` |
| O9 | 屬性 | `attr`；`src/com/lineage/server/model/L1MagicMode.java:188-230` 只對 1/2/4/8 算抗性 | `attribute`；108、202 用 `ATTRIBUTE_NONE`(16) | 一致（0 與 16 都是無屬性） | 同上 |
| O10 | type | `skills.type` | 有 `type` 欄位的 record（龍騎士、幻術師大多數） | 一致 | 同上 |
| O11 | 期限 | `addMagicList`（`U:2060-2161`）：有 SkillMode 的技能由 mode 自己 `setSkillEffect`（`U:2139-2149`） | 各 `Battle*Status.activate(now, durationSeconds)` | 一致；208 依 `sm/BONE_BREAK.java:35` 固定 2 秒（SQL 3） | `consumptionCooldownAndDurationMatchSource` |
| O12 | BUFF 能力值 | `ab` 15 個技能的列；另有 `U:3384` 114 MR+20 | catalog 後備值；DB 載入時 `CombatBuffLeases`／`SourceSkillControls.ability` | 一致（逐項，§2） | `buffAbilityFallbacksMatchSkillsBuffAbility` |
| O13 | 外控倍率 | `CK:93-133` 讀 `cfg`，缺鍵用程式預設 | 正式環境經 `lito伺服器/LiTo381-Server/config/combat.properties:5` **直接讀同一份 `cfg`**；缺鍵用 `NativeSourceSkillDefaults`（須等於 `CK` 缺鍵預設）；欄位對應在 `CombatRules.intField/doubleField`（`CombatRules.java:219-253`） | 一致（18 個鍵的缺鍵預設逐值相等；§2.2） | `missingConfigSkillKeysUseSourceDefaults`、`physicalProcFallbacksMatchConfigSkill`、`thunderGrabBindMatchesConfiguredSlayBreakTimer` |

## 2. 效果數值

### 2.1 `skills_buff_ability` 與 skillmode 固定值

| ID | 名稱 | 1-伺服器 | LiTo（catalog 後備值） | 判定 |
|---|---|---|---|---|
| 89 | 尖刺盔甲 | `ab` AC−2、HIT+6 | `armorClassModifier`−2、`meleeHitModifier`+6 | 一致 |
| 99 | 影之防護 | `ab` AC−3（`U:3422`） | −3 | 一致 |
| 107 | 暗影之牙 | `ab` DMG+5（只算近戰；`U:3455-3470`） | `meleeDamage`5、`rangedDamage`5 | 近戰一致；遠程後備值不同（§4.2） |
| 109／110 | 力量／敏捷提升 | `ab` STR+2／DEX+2（`U:3444`、`U:3433`） | `AbilityBuffSpell` STRENGTH／DEXTERITY +2 | 一致 |
| 114 | 激勵士氣 | `ab` HIT+5、BOW_HIT+5；`U:3384` MR+20 | 5／5／20 | 一致 |
| 115 | 鋼鐵士氣 | `ab` AC−8 | −8 | 一致 |
| 117 | 衝擊士氣 | `ab` DMG+5 | 5 | 一致 |
| 183 | 護衛毀滅 | `ab` AC+15（`U:3204-3210`） | +15 | 一致 |
| 193 | 驚悚死神 | `U:3212-3219` 註解寫 STR−5、INT−5「已遷移至 Buff 系統」，但 `ab` **沒有 193 的列** → 實際 0 | `armorClassModifier` 0，無能力值 | 一致（兩邊都沒有效果數值；是否為 S1 漏表屬 T100 裁定） |
| 185 | 覺醒：安塔瑞斯 | `ab` AC−12、HP+127 | −12、`maximumHpBonus`127 | 一致 |
| 190 | 覺醒：法利昂 | `sm/AWAKEN_FAFURION.java:42-47` MR+30、四屬性+30 | 30／30 | 一致 |
| 195 | 覺醒：巴拉卡斯 | `ab` 六項能力 +5 | `allAbilityScoreBonus`5 | 一致 |
| 204 | 幻覺：歐吉 | `ab` DMG+4、HIT+4（`U:3788`） | 4／4 | 一致 |
| 213 | 武器破壞者 | `ab` DMG−2 | −2 | 一致 |
| 214 | 幻覺：鑽石高崙 | `ab` AC−20（`U:3804`） | −20 | 一致 |
| 219 | 幻覺：化身 | `ab` DMG+10、BOW_DMG+10；`sm/ILLUSION_AVATAR.java:31` SP+8 | 10／10／8 | 一致 |
| 209 | 幻覺：巫妖 | `sm/ILLUSION_LICH.java:29` SP+2 | 2 | 一致 |
| 216／217 | 洞察／恐慌 | `sm/INSIGHT.java:29-33` 五項 +1；`sm/PANIC.java:29-33` 五項 −1 | +1／−1 | 一致 |
| 106／201 | 暗影閃避／鏡像 | `sm/UNCANNY_DODGE.java:26`、`sm/MIRROR_IMAGE.java:26` dodge+5 | 5／5 | 一致 |
| 188 | 恐懼無助 | `sm/RESIST_FEAR.java:26` dodge_down+5 | 5 | 一致 |
| 90／111 | 堅固防護／閃避提升 | `PI:4720-4721` ER+15；`PI:4716-4717` ER+12 | 15／12 | 一致 |
| 88 | 增幅防禦 | `AP:1021-1023` 玩家物理 `(lv−50)/5+10`；`L1AttackNpc.java:338-340`、`L1MagicPc.java:874-876` 其他 `+1` | 門檻 50、除數 5、10／1 | 一致 |
| 98 | 附加劇毒 | `AP:1525-1528` 10%、每 3 秒 HP−5 | 10%、3000 ms、5 | 一致 |
| 101 | 行走加速 | `U:3779-3786` `setBraveSpeed(4)` | `BRAVE_HOLY_WALK` | 一致（與 52／150 同組，T108 E9） |
| 208 | 骷髏毀壞 | `sm/BONE_BREAK.java:28-38`：先算傷害；沒有 208 狀態時 1..1000 擲骰 >800 → 2 秒暈眩、NPC 86123 | `failureThreshold`800、`maximumRoll`1000、2 秒、86123 | 一致 |

### 2.2 外控倍率（`cfg` 實際值；正式環境 LiTo 讀同一份檔）

| 鍵 | `cfg` 行 | 值 | 技能 | LiTo 取值 | 判定 |
|---|---|---|---|---|---|
| `SHOCK_STUN_TIMER` | :13 | 2,3（2～5 秒，`sm/SHOCK_STUN.java:41-50`） | 87 | `CombatRules.java:237-241`；離線後備 2,2 | 正式一致；離線後備＝`CK` 缺鍵預設 |
| `COUNTER_BARRIER_DMG` | :16 | 1.0（`AP:1876`） | 91 | `CombatRules.java:251-252`；後備 1.0 | 一致 |
| `SOLID_CARRIAGE_MODE` | :19 | 1（盾或臂甲，`sm/SOLID_CARRIAGE.java:27`） | 90 | 外控鍵 | 正式一致；處理器條件**待驗證** |
| `BURNING_CHANCE`／`BURNING_DMG` | :64／:67 | 33／1.5（`AP:1344-1345`） | 102 | `BattlePhysicalProcBuff.java:5-6` | 一致 |
| `DOUBLE_BREAK_CHANCE`／`_DMG` | :70／:73 | 15／1.5（`AP:812-813`） | 105 | `BattlePhysicalProcBuff.java:10-11` | 一致 |
| `FOE_SLAYER_DMG` | :77 | 10（`sm/FOE_SLAYER.java:65-66`） | 187 | 外控鍵；正式 `hitCount`=5、`exposureDamageStep`=0（`CombatRules.java:220-223`） | 正式一致；離線後備 3 次＋20 不同（只影響離線測試） |
| `VULNERABILITY_ROM`／`_1..3` | :80-87 | 33／10／20／30（`AP:1253-1300` 鎖鏈劍弱點） | 龍騎士 | `BattleChainswordWeakness.java:106-114` | 正式一致 |
| `SLAY_BREAK_TIMER` | :90 | 2,2（2～4 秒，`U:3833-3838`） | 192 | `CombatRules.java:242-246`；後備 2／4 | 一致 |
| `MORTAL_BODY_DMG` | :96 | 20（`PI:3357`） | 191 | `CombatRules.java:231`；後備 20 | 一致 |
| `MORTAL_BODY_ROM` | 缺鍵 | 23（`CK:132`；`L1MagicPc.java:502`） | 191 | 缺鍵預設 23 | 預設一致；機率屬 T102 |
| `JOY_OF_PAIN_COUNTDMG`／`_DMG` | :6／:9 | 3／1500（`PI:3367-3373`） | 218 | `BattleJoyOfPain.java:20-22` | 一致 |
| `ILLUSION_AVATAR_DAMAGE_RATE` | :3 | **1.05**（`PI:3390-3391`） | 219 | `CombatRules.avatarIncomingRate`；catalog 後備 1.5 | 正式一致；離線後備＝`CK` 缺鍵預設 1.5 |

## 3. 逐技能對照

「S1」欄寫效果所在；「LiTo」欄寫 catalog 紀錄行。所有列的消耗／冷卻／期限／材料／傷害骰／屬性／type／正義值已由 §1 的參數化測試逐值比對。

| ID | 名稱 | S1 效果類別／行號 | LiTo | 判定 |
|---|---|---|---|---|
| 87 | 衝擊之暈 | `M:186` `sm/SHOCK_STUN.java:36-50`；`U:3841-3843` | `C:375` ShockStunSpell | 一致（MP15、8000 ms、10+0d10）；機率屬 T102 |
| 88 | 增幅防禦 | `AP:1021-1023` | `C:407` ReductionArmorSpell | 一致（HP50、192 秒） |
| 89 | 尖刺盔甲 | `ab` | `C:418` BounceAttackSpell | 一致（HP120、64 秒） |
| 90 | 堅固防護 | `M:187` `sm/SOLID_CARRIAGE.java:27`；`PI:4720` | `C:430` SolidCarriageSpell | 一致（HP100、64 秒） |
| 91 | 反擊屏障 | `combat/PcPhysicalAttackFlow.java:90-95`；`AP:1876` | `C:444` CounterBarrierSpell | 一致（HP100、64 秒、倍率 1.0）；機率屬 T102 |
| 97 | 暗隱術 | `U:3960-3974`（與隱身術同路徑） | `C:2725` InvisibilitySpell | 一致（9990 ms、32 秒） |
| 98 | 附加劇毒 | `AP:1525-1528` | `C:2736` EnchantVenomSpell | 一致（HP1、石×1、320 秒） |
| 99 | 影之防護 | `U:3422-3431` | `C:320` ShieldSpell | 一致（HP5、960 秒） |
| 100 | 提煉魔石 | `U:504`、`U:3268` | `C:2747` BringStoneSpell | 一致（MP5）；產出機率**待驗證** |
| 101 | 行走加速 | `U:3779-3786` | `C:1360` MovementBuffSpell | 一致 |
| 102 | 燃燒鬥志 | `AP:1344-1345` | `C:1385` PhysicalProcBuffSpell | 一致（2990 ms、320 秒） |
| 103 | 暗黑盲咒 | `M:203`（CURSE_BLIND）；`U:2047` lawful−10 | `C:2238` BlindSpell | 消耗／期限一致；正義值 **已修正（#325）**（§4.1） |
| 104 | 毒性抵抗 | 預設 | `C:1453` PoisonResistanceSpell | 一致（320 秒） |
| 105 | 雙重破壞 | `AP:812-813` | `C:1396` PhysicalProcBuffSpell | 一致（石×1、192 秒） |
| 106 | 暗影閃避 | `M:202` `sm/UNCANNY_DODGE.java:26` | `C:1467` DodgeBuffSpell | 一致 |
| 107 | 暗影之牙 | `U:3455-3470` | `C:1506` ItemWeaponEnchantSpell | 一致（石×1、192 秒）；遠程後備值見 §4.2 |
| 108 | 會心一擊 | `U:2036-2038` | `C:1675` AttackSpell(finalBurn) | 一致（O5） |
| 109 | 力量提升 | `U:3444` | `C:1264` AbilityBuffSpell | 一致 |
| 110 | 敏捷提升 | `U:3433` | `C:1273` AbilityBuffSpell | 一致 |
| 111 | 閃避提升 | `PI:4716` | `C:1287` RangedEvasionBuffSpell | 一致（32 秒） |
| 112 | 破壞盔甲 | 預設（`U:338`）；`AP:1027-1029` | `C:392` ArmorBreakSpell | 消耗一致（HP30、石×2、8 秒）；×1.58 一致（§6.1）；料理減傷順序**已修正**（#337）；機率屬 T102 |
| 113 | 精準目標 | `M:183` `sm/TRUE_TARGET.java` | `C:2774` TrueTargetSpell | 一致（MP1） |
| 114 | 激勵士氣 | `U:3374-3386` | `C:2784` RoyalAuraSpell | 一致 |
| 115 | 鋼鐵士氣 | `U:3388-3397` | `C:2792` RoyalAuraSpell | 一致 |
| 116 | 呼喚盟友 | `M:181` `sm/CALL_CLAN.java` | `C:2814` CallClanSpell | 一致（MP30、729 問答、落點與面向，§6.5）；LiTo 額外限制見 §6.5；傳送畫面**待驗證** |
| 117 | 衝擊士氣 | `U:3399-3409` | `C:2801` RoyalAuraSpell | 一致 |
| 118 | 援護盟友 | `M:182` `sm/RUN_CLAN.java` | `C:2827` RunClanSpell | 旗區戰爭例外**已修正**（#337，§6.5）；被拒時扣 MP **待使用者裁定**（§6.6） |
| 181 | 龍之護鎧 | 預設（`U:342`）；`AL:284` | `C:972` DragonSkinSpell | 一致（HP15、10 ms、1200 秒、減傷 10，§6.2）；怪物魔法漏扣**已修正**（#337） |
| 182 | 燃燒擊砍 | 預設 | `C:1094` BurningSlashSpell | 一致（HP10、火、60 秒） |
| 183 | 護衛毀滅 | `U:3204-3210` | `C:1111` GuardBrakeSpell | 一致；機率屬 T102 |
| 184 | 岩漿噴吐 | 預設；覺醒限制 `U:597-614` | `C:2160` AreaAttackSpell | 一致（HP35、45、火） |
| 185 | 覺醒：安塔瑞斯 | `M:206` `sm/AWAKEN_ANTHARAS.java` | `C:988` AwakenSpell | 一致（骨片×1） |
| 186 | 血之渴望 | `M:210` `sm/BLOODLUST.java`；`U:2514` | `C:1056` BloodlustSpell | 一致（HP30、骨片×1、60 秒） |
| 187 | 屠宰者 | `M:209` `sm/FOE_SLAYER.java:65-66` | `C:1076` FoeSlayerSpell | 一致（HP20、2000 ms）；離線後備 3 次與正式 5 次不同（§2.2） |
| 188 | 恐懼無助 | `M:211` `sm/RESIST_FEAR.java:26` | `C:1146` ResistFearSpell | 一致 |
| 189 | 岩漿之箭 | 預設 | `C:2173` AreaAttackSpell | 一致（HP40、45、風） |
| 190 | 覺醒：法利昂 | `M:207` `sm/AWAKEN_FAFURION.java:42-47` | `C:1005` AwakenSpell | 一致 |
| 191 | 致命身軀 | `PI:3354-3364`；`L1MagicPc.java:502` | `C:1041` MortalBodySpell | 一致（HP50、300 秒、反彈 20）；機率屬 T102 |
| 192 | 奪命之雷 | `U:3830-3838` | `C:1688` ThunderGrabSpell | 一致（HP45、80+2d10 風、束縛 2～4 秒） |
| 193 | 驚悚死神 | `U:3212-3219` | `C:1126` GuardBrakeSpell | 一致（HP50、60 秒；兩邊都沒有能力值） |
| 194 | 寒冰噴吐 | `U:2103-2107`（凍結）、`U:3112` | `C:2057` FreezeAttackSpell | 一致（HP50、45、水、3 秒） |
| 195 | 覺醒：巴拉卡斯 | `M:208` `sm/AWAKEN_VALAKAS.java` | `C:1020` AwakenSpell | 一致（骨片×2） |
| 201 | 鏡像 | `M:223` `sm/MIRROR_IMAGE.java:26` | `C:1477` DodgeBuffSpell | 一致（1200 秒） |
| 202 | 混亂 | `M:214` `sm/CONFUSION.java` | `C:1706` ConfusionSpell | 一致（HP10、MP15、8 秒）；機率屬 T102 |
| 203 | 粉碎能量 | 預設 TYPE_ATTACK | `C:1721` SmashSpell | 一致（50+2d10、600 ms） |
| 204 | 幻覺：歐吉 | `U:3788-3797` | `C:1782` IllusionOgreSpell | 一致 |
| 205 | 立方：燃燒 | `U:572-595`、`U:2735` | `C:1831` CubeIgnitionSpell | 一致（立方×1、20 秒、傷害 10，§6.4）；經典世界安全區盟友**已修正**（#337） |
| 206 | 專注 | 預設；`PR:73` | `C:1573` RegenSpell | 一致（600 秒、MPR+2 逐值，§6.3） |
| 207 | 心靈破壞 | `M:220` `sm/MIND_BREAK.java` | `C:1739` MindBreakSpell | 一致（HP15、MP20、基礎 5） |
| 208 | 骷髏毀壞 | `M:219` `sm/BONE_BREAK.java:28-38` | `C:1762` BoneBreakSpell | **已修正**：正義值 +45 → 0（§3.1） |
| 209 | 幻覺：巫妖 | `M:222` `sm/ILLUSION_LICH.java:29` | `C:1797` IllusionLichSpell | 一致 |
| 210 | 立方：地裂 | `U:2741` | `C:1850` CubeQuakeSpell | 一致（§6.4）；經典世界安全區盟友**已修正**（#337） |
| 211 | 耐力 | 預設（`U:343`）；`AL:285` | `C:1906` PatienceSpell | 正式一致（600 秒、減傷 15；離線後備 2，§6.2）；怪物魔法漏扣**已修正**（#337） |
| 212 | 幻想 | `M:215` `sm/PHANTASM.java` | `C:1923` PhantasmSpell | 一致（HP25、5 秒）；機率屬 T102 |
| 213 | 武器破壞者 | `M:216` `sm/ARM_BREAKER.java` | `C:1942` ArmBreakerSpell | 一致（12 秒、DMG−2） |
| 214 | 幻覺：鑽石高崙 | `U:3804-3812` | `C:1960` DiamondGolemSpell | 一致 |
| 215 | 立方：衝擊 | `U:2747` | `C:1869` CubeShockSpell | 一致（§6.4）；經典世界 MR−10 **已修正**（#337） |
| 216 | 洞察 | `M:218` `sm/INSIGHT.java:29-33` | `C:1976` InsightSpell | 一致 |
| 217 | 恐慌 | `M:217` `sm/PANIC.java:29-33` | `C:1992` PanicSpell | 一致（HP30、64 秒）；機率屬 T102 |
| 218 | 疼痛的歡愉 | `PI:3367-3373` | `C:2010` JoyOfPainSpell | 一致（2 秒；外控 3／1500） |
| 219 | 幻覺：化身 | `M:221` `sm/ILLUSION_AVATAR.java:31`；`PI:3390-3391` | `C:1815` IllusionAvatarSpell | 一致 |
| 220 | 立方：和諧 | `U:2753` | `C:1888` CubeHarmonySpell | 一致（立方×2；MP+5／傷害 25，§6.4） |

統計：一致 60（其中多列附「待驗證」的效果本體）、已修正 2（208；#325 補 103 正義值）、不一致・未改 0（107 遠程後備值見 §4.2）。#337 補驗效果本體後再修正 112、118、181、205、210、211、215 的效果路徑（§6），118 被拒扣 MP 待使用者裁定（§6.6）。

### 3.1 208 正義值的根因
LiTo catalog 註解寫「adds lawful45」，`BoneBreakSpell` 建構子也把 `lawfulDelta != 45` 當成不合法；處理器在 `CS:89405`（競技場）與 `CS:81542`（經典世界）每次施放都 `applySpellLawfulDelta(+45)`。但 `db` 第 208 列 `lawful=0`，45 是同一列的 `probability_value`；`NativeBoneBreakSourceAuditTest` 引用的主要／次要兩份 SQL 也都是 `lawful=0`。S1 `U:2047` 加的是 `skills.lawful`，所以 S1 施放不改正義值。本 PR 把 `lawfulDelta` 改成 0（`C:1766`），建構子檢查同步改為 0；`applySpellLawfulDelta` 在 delta=0 時直接返回、不送 `S_CHANGE_ALIGNMENT`（`CS:46732`）。

## 4. 不一致與未改的理由

### 4.1 103 暗黑盲咒正義值 −10——#325 已補上
#325 與 T107 的 8 個技能一起處理：`BlindSpell` 加 `lawfulDelta`（20／40／103 皆 −10），盲咒的戰鬥／世界／寵物目標處理器扣 MP 後呼叫 `applySpellLawfulDelta`（見 `skills-wizard.md` §4.1）。`lawfulGapListIsExact` 的清單改為空集合。

### 4.2 107 暗影之牙遠程傷害後備值
S1 `ab` 只有 `(107,DMG,5)`，DMG 只算近戰（`src/com/lineage/server/model/Instance/L1PcBuffAbilityCalc.java:294-295`）。LiTo 正式環境（DB 載入）走 `CombatBuffLeases` 讀同一張表（`CombatWeaponBuffs.java:13-15`），結果相同；只有沒載入 DB 時 catalog 後備值 `rangedDamage=5` 會讓遠程也 +5。與 T107 §4.3 同類，只影響離線測試，未改。

### 4.3 離線後備值與 `cfg` 不同（不影響正式環境）
87（2,2 vs `cfg` 2,3）、187（3 次＋20 vs 正式 5 次＋0）、219（1.5 vs `cfg` 1.05）的 catalog 後備值等於 `CK` 的缺鍵預設或舊版本，正式環境由 `CombatRules` 讀 `cfg`。未改；若之後要移除重複常數，可在 T002 一併處理。

## 5. 未驗證（交給實機／後續任務）

- 各處理器的封包順序、狀態圖示、實際 HP／MP 入帳。
- 機率類（87、91、103、112、183、188、191、192、193、202、212、217）屬 T102。
- 100 提煉魔石產出、90 `SOLID_CARRIAGE_MODE` 裝備條件。
- T117（#337）已從原始碼補驗立方、116／118、覺醒扣 MP、112 ×1.58、181／206／211（§6）；剩下的實機項目列在 §6.7。

## 6. 效果數值補驗（T117，Issue #337）

縮寫（1-伺服器，唯讀）：`AL` = `src/com/lineage/server/model/L1AttackList.java`，`MN` = `src/com/lineage/server/model/L1MagicNpc.java`，`MP` = `src/com/lineage/server/model/L1MagicPc.java`，`PR` = `src/com/lineage/server/timecontroller/pc/PcRegenExecutor.java`，`AT` = `src/com/lineage/server/timecontroller/skill/Skill_Awake_Timer.java`，`CX` = `src/com/lineage/server/timecontroller/skill/EffectCubeExecutor.java`，`CB` = `src/com/lineage/server/model/L1Cube.java`，`CC` = `src/com/lineage/server/model/skill/skillmode/CALL_CLAN.java`，`RC` = `src/com/lineage/server/model/skill/skillmode/RUN_CLAN.java`，`CA` = `src/com/lineage/server/clientpackets/C_Attr.java`，`ch` = `config/charsettings.properties`。測試：`lito伺服器/LiTo381-Server/src/test/java/local/lito381/server/OtherClassSkillEffectServer1ParityTest.java`（13 個）。

### 6.1 112 破壞盔甲 ×1.58 與 PC 物理減傷順序

| 面向 | 1-伺服器 | LiTo | 判定 |
|---|---|---|---|
| 倍率 | `AP:1027-1029` 弓(20)／鐵手甲(62) 以外 `(int)(dmg*1.58)` | `BattleArmorBreak.damageAfterBreak`（`BattleArmorBreak.java:75-79`） | 一致（傷害 0..5000 × 14 種武器逐值） |
| 只對 PC 目標 | 只有 `L1AttackPc` 的 PC 分支有；NPC 目標沒有對應程式 | 正式環境 NPC 目標走 `CombatNpcDebuffs.armorActive(active, null)` → false（`CombatNpcDebuffs.java:17`；`ClientSession.java` 世界怪物與戰鬥房 NPC 兩處） | 正式一致；離線後備會放大 NPC（只影響離線測試） |
| 減傷順序 | `AP:1021-1034`：增幅防禦 → ×1.58 → `getCombatDmgBonus()`（SKD3：料理 −5／−2、龍之護鎧 −10、耐力 −15）→ 聖結界 ÷2 | 改前：**料理** → 增幅防禦 → ×1.58 → 龍之護鎧 → 耐力 → 聖結界 | **已修正**：料理移到 ×1.58 之後（`ClientSession.combatDamageAfterCooking`）。只在「目標中破壞盔甲＋料理減傷」時有差：例 100 傷害、料理 5，改前 150、改後 153（＝S1） |

### 6.2 181 龍之護鎧／211 耐力 減傷

| 面向 | 1-伺服器 | LiTo | 判定 |
|---|---|---|---|
| 數值 | `AL:284` DRAGON_SKIN −10（註解：2011-11-26 前 −2）；`AL:285` PATIENCE −15（註解：原始 −2） | `CombatSourceReductions.java:5-6`：正式 15／10；離線後備 2／10（`BattlePatienceStatus.PHYSICAL_REDUCTION`=2） | 正式一致；211 離線後備 2 是「原始值」，不影響正式 |
| PC 物理（PC、NPC 攻擊者） | `AP:1031`、`L1AttackNpc.java:343` | `combatDamageAfterCooking`（`physicalDamage`）、`CombatNpcPlayerDamage.java:33` | 一致 |
| PC 被玩家魔法打 | `MP:880-907` | `CombatPlayerMagic.java:22-23`（正式） | 一致 |
| PC 被**怪物魔法**打 | `MN:554-586`：防具減免、增幅防禦、SKD3 一起在 `calcMrDefense`（`MN:526`）之前扣 | 改前：三條怪物魔法路徑（經典世界、任務、限時）只扣防具與增幅防禦，**沒扣 181／211** | **已修正**：新增 `ClientSession.sourceNpcMagicSkillReduction`（只在正式環境），加進 MR 前的減免總和 |
| 火牢 | `src/com/lineage/server/timecontroller/skill/EffectFirewallTimer.java:251-260` 也套 SKD3 | 舊路徑 `combatDamageAfterCooking(damage, source)` 不套 181／211 | 不一致・未改（T116 已列推測；火牢屬法師，留給 T116 後續） |

### 6.3 206 專注 MPR+2

`PR:72-73` 技能表 冥想 5、專注 2；`PR:159-213` 每次 MP 回復 = WIS 基礎（≤14→1、15–16→2、≥17→3）＋藍水（WIS<11→1，否則 WIS−10）＋技能表＋料理＋原始 MPR＋裝備。LiTo `BattleNaturalMpRegen.amount`（`CONCENTRATION_BONUS`=2）逐值一致（7 職業 × WIS 0..60 × 原始 WIS 0..20 × 裝備 0..3 × 8 種狀態 × 料理 0／2／3）；catalog `RegenSpell.mprBonus`=2、行動不中斷。

### 6.4 205／210／215／220 立方

| 面向 | 1-伺服器 | LiTo（正式） | 判定 |
|---|---|---|---|
| 立方本體 | `U:2735-2757` NPC 80149～80152、期限 `buff_duration` 20 秒；`U:571-594` 施放者 3 格內同種立方 → 1412 | `BattleCube*Effects` `NPC_ID`、`DURATION_SECONDS`=20；1412 在扣耗材前 | 一致 |
| 範圍與節拍 | `CX:53` 半徑 3；`StartTimer_Skill.java:73-82` 每 1000 ms 掃描；狀態 8000 ms（`L1EffectInstance.java:28`）不重複；`CB:181-182` 每目標 900／1000 ms | `EFFECT_RADIUS`=3、`STATUS_SECONDS`=8、900／1000 ms；`NativeSourceCubeClock` 全域相位 | 一致 |
| 對象 | `CX:72-98`：自己／同血盟／同隊伍＝盟友（不看安全區）；其他玩家在安全區只在攻城戰中算敵人；`CX:100-105` 只有怪物（寵物、召喚不受影響） | 戰鬥房：`NativeSourceCubeRelation`；經典世界：`GameHub` 的 Alliance lambda | 戰鬥房一致（另依房間 PvP／隊伍規則，刻意）；經典世界燃燒、地裂**已修正**（改前先判安全區，盟友在安全區拿不到 +30）；攻城戰中安全區內的敵人仍被略過（未改，§6.7） |
| 盟友 | `CX:127-134` 火／地／風屬性 +30 | `ALLY_*_RESISTANCE`=30 | 一致 |
| 燃燒 | `CB:233-266` 第 4、8 跳 10 傷害，非魔法、不經 MR；凍結／絕對屏障／大地屏障等免疫 | `ENEMY_DAMAGE`=10、每 4 跳；`NativeSourceCubePlayer.java:58-62` | 一致 |
| 地裂 | `CB:269-300` 第 4、8 跳束縛 1000 ms，必中 | `ENEMY_BIND_EVERY_TICKS`=4、`ENEMY_BIND_MILLIS`=1000 | 一致 |
| 衝擊 | `CB:303-304` 只設 STATUS_MR_REDUCTION，全原始碼沒有讀取點 → **MR 不變** | 戰鬥房 `NativeSourceCubeStatus.read` query 3 回 0；經典世界改前仍套 −10 | **已修正**：經典世界在正式環境也回 0（`effectiveCubeShockMagicResistanceModifier`）；離線後備 −10 是 LiTo 刻意「修復」，保留 |
| 和諧 | `CB:307-319` tick%4 MP+5、tick%5 傷害 25（施放者自己也會挨） | `MP_RECOVERY`=5、`HP_DAMAGE`=25、每 4／5 跳 | 一致 |

### 6.5 116 呼喚盟友／118 援護盟友

| 面向 | 1-伺服器 | LiTo | 判定 |
|---|---|---|---|
| 目標 | `C_UseSkill.java:564-579` 依名稱；不在線 73、不同血盟 414 | `ClientSession.useCallClanSpell`／`useRunClanSpell` 同訊息 | 一致；LiTo 兩人都沒血盟也回 414（S1 `0==0` 會通過，視為 S1 漏洞，未改） |
| 116 問答 | `CC:38-40` `S_Message_YN(729)`；`CA:699-701` No 清除不回訊息 | `BattleCallClan` 729；No 不回訊息 | 一致 |
| 116 Yes | `CA:1301-1304` 被呼喚者地圖不可瞬移 647；`CA:1316-1329` 施放者不在 0／4／304 或在旗區且非戰爭中 629；`CA:1346-1357` 施放者座標＋面向偏移一格、被呼喚者面向 `(h+4)&7`；`CA:1363-1378` 落點被占 627 | 同一套條件、偏移表與面向公式（`BattleCallClan.java:8-46`） | 一致 |
| 116 LiTo 額外限制 | 無逾時；施放時不檢查目標狀態 | 30 秒逾時；施放時要求同房／同世界（629，不扣 MP） | 刻意（競技場房間與防卡邀請），未改 |
| 118 旗區 | `RC:31-46` `L1CastleLocation.checkInAllWarArea`：任一旗區**一律** 1192（不看戰爭時間）；目標地圖不是 0／4／304 也 1192 | 改前：攻城戰中可進旗區（把 116 的戰爭例外套到 118） | **已修正**（`runClanClassicWorldDestinationAllowed`） |
| 118 落點 | `RC:40` 目標座標、面向 5 | `BattleRunClan.java:24-26` | 一致 |
| 扣 MP 時機 | `L1SkillUse.java:979-983` 先播動作、執行 mode，`:1196-1198` 再扣 HP／MP；118 被 mode 拒絕（647／1192）時**照扣** MP 30 | 116 施放即扣（一致）；118 的 647／1192 在扣費前拒絕，不扣 MP、不播動作 | **待使用者裁定**（§6.6） |

### 6.6 待使用者裁定

| 項目 | 1-伺服器 | LiTo 現況 | 建議 |
|---|---|---|---|
| 118 被 647／1192 拒絕 | 扣 MP 30、播動作 19、無效果 | 扣費前拒絕，不扣 MP | 與 T116 的 66 同類。對齊 S1 需把兩個拒絕移到扣費之後；或維持 LiTo（#321 S3「扣 MP 沒效果是缺陷」）。測試 `runClanMapRejectionsCurrentlyHappenBeforeCost` 鎖住現況 |
| 覺醒模式 | `ch:105` `DragonKnightAwakeClassicMode = false`（自建：不變龍、不限技能、**不扣覺醒 MP**） | 正式環境讀同一份 `ch`（`CombatRules.java:44-47`），結果相同 | 一致，不需裁定；若之後改成 `true`（仿正），兩邊都會每 4 秒扣最多 8 MP（`AT:106-123`、`AT:163-168`；LiTo `BattleAwakenStatus.pollMpDrainTicks`），已有逐秒測試 |

### 6.7 推測／未驗證

- 推測：S1 `L1AttackPc` 的 `dmg` 是 double，×1.58 前可能已有小數（職業倍率等）；LiTo 在此處的輸入是 int。只差在小數截斷，屬 T113 範圍。
- 推測：怪物魔法打 PC 時，S1 的料理減傷與聖結界 ÷2 也在 MR 之前（`MN:565-607`），LiTo 仍在 MR 之後（`combatDamageAfterCookingWithoutReductionArmor`）。料理不是本任務範圍，未改。
- 未改：攻城戰中，立方對安全區內敵人仍略過（S1 會當敵人）；只在攻城戰＋安全區＋經典世界同時成立時有差。
- 待驗證（實機）：立方圖示／特效、地裂束縛封包；116 的 729 對話框文字與傳送畫面；118 傳送後面向 5 與解除鎖定；181／211 在怪物魔法下的實際傷害；破壞盔甲＋料理時的實際傷害。
