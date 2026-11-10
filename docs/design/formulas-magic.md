# 魔法傷害／MR／技能機率：1-伺服器 ↔ LiTo 公式對照（T102，Issue #304）

- 參考（唯讀）：`D:\lineage開發\1-伺服器`。下表寫 `S1:` 加相對路徑與行號，`~` = `src/com/lineage/server`，`db` = `db/380_db.sql`。
- LiTo：`L:` = `lito伺服器/LiTo381-Server/src/main/java/local/lito381/server/`，行號是本分支（task/304，已合併 #303 後的 main）修改後的行號；`CS` = `ClientSession.java`。
- 對照測試：`lito伺服器/LiTo381-Server/src/test/java/local/lito381/server/MagicFormulaServer1ParityTest.java`（「測試」欄寫方法名）。期望值由測試裡的 `ref*` 函式依 1-伺服器 公式另寫一份；參考表（精神成長、智力成長、負面技能控制、BOSS 免疫、skill_level）照 `db` 的 INSERT 列原樣抄入。
- 判定：**一致**＝逐值相等；**已修正**＝本 PR 改 LiTo 對齊 1-伺服器；**不一致・待裁定**＝有差異但本 PR 不改；**不適用**＝競技場沒有這個系統，或在正式設定下不會走到。
- 範圍：玩家施法（`L1MagicPc`）打玩家、打 NPC。NPC 施法（`L1MagicNpc`）、治癒量（`calcHealing`）、各技能的效果本體（T107～T109）不在本任務。
- 1-伺服器 的入口：`S1:~/model/skill/L1SkillUse.java:2838`（`new L1Magic`）→ 傷害 `:2875`、`:3003`（`calcMagicDamage`），機率 `:2886`、`:3137`、`:3831`、`:3842`（`calcProbabilityMagic`）；`skillmode/*` 51 個檔案中真正呼叫 `calcMagicDamage` 的只有 `ARM_BREAKER.java:21`、`BONE_BREAK.java:28`、`MIND_BREAK.java:31`（`<< 2`），其餘都是註解掉的 `dmg = 0`。

## 1. 魔法傷害（`L1MagicPc.calcMagicDamage` S1:`~/model/L1MagicPc.java:825-840`）

| # | 公式項目 | 1-伺服器 寫法 | LiTo 寫法 | 判定 | 測試 |
|---|---|---|---|---|---|
| M1 | 骰值 = Σ(`nextInt(dice)+1`)×count + value | `L1MagicPc.java:1009-1019`（`skills.damage_dice/damage_dice_count/damage_value`） | 正式環境直接讀同一張 `skills` 表：`L:CombatDatabase.java:62-75`（SQL）、`:143-152`（`value()` 覆蓋 `BattleSpellCatalog` 的常數） | 一致（同一張表；catalog 常數只是沒載入 DB 時的後備值） | — |
| M2 | 施法者「邪惡的守護」Lv1～3（正義值 −10000～−39999）骰值 +1～+3 | `L1MagicPc.java:1021-1033`；`~/model/Instance/L1PcInstance.java:1126-1147` | 本 PR 新增：`CS:74039-74041`（打玩家）、`CS:74061-74062`（打 NPC），`L:CombatPlayerMagic.java:11` | **已修正**（原本沒有） | `lawfulGuardianMrAndEvilGuardianBonusMatchGuardianEncounter` |
| M3 | INT 項 `max(INT + 道具SP − 12, 1)`；道具SP = `getSp − getTrueSp` + 邪惡守護 +1～3 + 風／生命／形象魔眼各 +1 | `L1MagicPc.java:1035-1036`；`~/model/L1MagicMode.java:82-126` | `CS:74037-74038`（`magicIntelligenceTerm` + 料理 SP），邪惡守護本 PR 加在 `CS:74041`；`L:BattleElementalResistance.java:50` | 一致（邪惡守護已補）；魔眼 SP **待驗證**（LiTo 的 `dragonEyeStatus` 有魔命、耐性，SP 是否已算進 `spellPowerModifier` 沒查） | `attributeAndIntelligenceCoefficientMatchesCalcMagicDiceDamage` |
| M4 | 屬性抗性：`floor = (int)(0.32×|r|)`（負抗性取負），`attrDef = floor/32`；只看地 1／火 2／水 4／風 8 | `~/model/L1MagicMode.java:188-251` | `L:BattleElementalResistance.java:35-54`；屬性只取 1/2/4/8：`CS:56257-56271` | 一致（−128..127 全測） | 同上 |
| M5 | 係數 `max(1 − attrDef + INT項×3/32, 0)`，`magicDamage *= 係數`（int 截斷） | `L1MagicPc.java:1042-1049` | `L:BattleElementalResistance.java:51-53` | 一致 | 同上 |
| M6 | 魔法爆擊：`skill_level ≤ 6` 且 `rnd(1..100) ≤ 10 + 原始魔爆` → ×1.5（截斷） | `L1MagicPc.java:1051-1057` | `L:BattleMagicResistance.java:138-154`；`skill_level` 由 `L:BattleMagicResistance.java:132-135` 計算（本 PR 從 `CS` 三處行內算式抽出） | 一致；`((id−1)>>3)+1` 與 `db` 的 181 筆非佔位 `skills.skill_level` 全部相等（36 筆不符都是名稱 `none` 的空列） | `criticalGateAndOriginalAdditionsMatchCalcMagicDiceDamage`、`skillLevelMatchesEveryNonPlaceholderSkillsRow` |
| M7 | + 原始魔傷（`originalMagicDamage`）+ 裝備魔傷（`get_magic_modifier_dmg`） | `L1MagicPc.java:1059-1062` | `L:BattleMagicResistance.java:149-152`；裝備值 = `totalMagicDamage + 娃娃 PASSIVE_MAGIC_DMG_MODIFIER`（`CS:74047`） | 一致 | 同 M6 |
| M8 | 天賦魔傷 `TalentDamageCalculator` | `L1MagicPc.java:1065` | 沒有 | 不適用（1-伺服器 自訂天賦系統，3.81C 沒有） | — |
| M9 | `leverage/10` 倍率 | `L1MagicPc.java:867`、`:970`；`~/model/L1MagicMode.java:41,74` | 沒有 | 一致：玩家施法 leverage 恆為 10（`setLeverage` 只在怪物技能 `~/model/L1MobSkillUse.java:500,656`） | `defaultProbabilityBranchMatchesCalcProbabilityDefault`（機率端同一倍率） |
| M10 | PvP 防具減免：`base ≤ 10 → base`，否則 `10 + nextInt(base−10) + 1` | `L1MagicPc.java:870`；`~/model/Instance/L1PcInstance.java:7784-7795` | `L:BattleMagicResistance.java:157-164`；擲骰 `CS:74048-74049`；base `CS:56344-56346` | 一致 | `armorReductionMatchesGetDamageReductionByArmor` |
| M11 | 增幅防禦 `−((max(Lv,50)−50)/5+1)`；`SKD3` 技能減免（相加或減半） | `L1MagicPc.java:874-907` | `L:CombatPlayerMagic.java:14-26`（料理、增幅防禦、龍之護鎧、堅忍、聖結界減半） | 一致（推測：`SKD3` 清單內容未逐筆比對，屬 T107～T109） | — |
| M12 | 魔法反射 `COUNTER_MIRROR`：`WIS ≥ nextInt(100)` 時反彈並歸零 | `L1MagicPc.java:910-920` | `L:CombatPlayerMirror.java:10-16` | 一致 | — |
| M13 | 職業倍率 `Other_ADD_is{職業}_MagicPC`，`(int)(dmg×倍率)` | `L1MagicPc.java:923`；`~/../config/ConfigPcDamage.java:113-119,191` | `L:CombatMagicDamage.java:9-10`（鍵）、`:39-44`（套用） | 一致 | `magicClassMultiplierKeysMatchConfigPcDamage` |
| M14 | 究極光裂術 `DISINTEGRATE_CRITICAL_RND`（預設 40）×2 | `L1MagicPc.java:926-930` | `L:CombatMagicDamage.java:42`；機率鍵 `L:CombatRules.java:225-229` | 一致 | `naturesBlessingHealIsZeroEvenWhenTheSkillFileSaysOneHundred`（同時驗 40） |
| M15 | `max(dmg, 0)` 後才做 MR 減傷 | `L1MagicPc.java:932,838` | `L:CombatMagicDamage.java:43` | 一致 | — |
| M16 | PvE：無防具減免；寵物／召喚物 ×`PC_TO_PET_DAMAGE_MULTIPLIER`；聖結界 ÷`IMMUNE_TO_HARM`；光裂 ×2 | `L1MagicPc.java:940-1001` | NPC 呼叫點防具參數一律傳 0（例：`CS:86921-86930`）；`L:CombatNpcMagic.java:23`；寵物倍率 `L:CombatCompanionDamage.java` | 一致（推測：寵物／聖結界未逐值測） | — |
| M17 | `dmg0`（`isSkillEffectPhysicalDamageZero`）→ 0 | `L1MagicPc.java:857,945`；`~/model/L1MagicMode.java:50-60` | `L:CombatPlayerMagic.java:12`（絕對屏障、大地屏障、冰凍） | 推測一致；1-伺服器 的「傷害為 0」狀態清單**待驗證** | — |
| M18 | 魔力奪取之焰（FINAL_BURN）傷害 = 施法者現 MP | `L1MagicPc.java:862-863,965-966` | `L:CombatMagicDamage.java:29-35`、`L:BattleFinalBurn.java` | 一致（MR 的正義守護本 PR 一併補上） | — |

## 2. MR 減傷與 MR 來源

| # | 公式項目 | 1-伺服器 寫法 | LiTo 寫法 | 判定 | 測試 |
|---|---|---|---|---|---|
| R1 | `MRDMG` 表（0→1/1.0、1-50→2/1.0、51-100→3/0.9、…、241+→11/0.5），`dmg *= 係數 − 0.01×floor((MR − 原始魔命)/floor)` | `~/model/L1AttackList.java:160-209`；`L1MagicPc.java:1106-1126` | `L:BattleMagicResistance.java:170-215`（打玩家）；`L:CombatNpcMagic.java:18-22`（打 NPC） | 一致（MR 0..300 × 原始魔命 0..4 × 10 種傷害全測）。差異：1-伺服器 負 MR 查不到表 → 11/0.5；LiTo 直接拒絕負 MR，但 MR 來源已先夾到 ≥ 0（`CS:56338`），不會走到 | `mrDamageReductionMatchesCalcMrDefenseForEveryMrAndOriginalMagicHit` |
| R2 | 職業基礎 MR：王 10、騎 0、妖 25、法 15、黑妖 10、龍 18、幻 20 | `~/model/Instance/L1PcInstance.java:8109-8128` | `L:BattleMagicResistance.java:14-25` | 一致 | `classBaseAndWisdomMrMatchResetBaseMrAndTheWisdomTable` |
| R3 | WIS → MR（`角色_精神成長.魔防`，key 夾 0..50） | `~/datatables/BaseStatTable.java:78-85,187-194`；`db:12194` 起 | `L:BattleMagicResistance.java:28-49`；正式環境讀表並比對 `L:LegacyMySqlRepository.java:6319-6320` | 一致（51 列） | 同上 |
| R4 | + 等級/2 | `L1PcInstance.java:8130` | `L:Accounts.java:2065` | 一致 | 同上 |
| R5 | 原始 WIS MR（7 職業） | `~/model/Instance/L1PcOriginal.java:796-865` | `L:BattleMagicResistance.java:51-67` | 一致（7 職業 × 0..127） | `originalIntelligenceAndWisdomBonusesMatchL1PcOriginal` |
| R6 | 原始 INT 魔命／魔爆／魔傷 | `L1PcOriginal.java:872-968`、`:970-1011`、`:1064-1107` | `L:BattleMagicResistance.java:69-125` | 一致 | 同上 |
| R7 | 目標「正義的守護」Lv1～3（正義值 10000～39999）MR +3／+6／+9；只加在魔法計算用的 `getTargetMr`，不改角色面板 MR | `~/model/L1MagicMode.java:132-155`；`L1PcInstance.java:1126-1147` | 本 PR 前只有魔法相消術有加（`L:NativeCancellationProbability.java:11,29`）。本 PR：傷害 `L:CombatMagicDamage.java:20,34` → `L:CombatPlayerMagic.java:8-9`；機率 `L:CombatProbability.java:185-187`（表列技能，`playerTarget`）、`:23,162-171`（非表列技能） | **已修正** | `lawfulGuardianMrAndEvilGuardianBonusMatchGuardianEncounter` |
| R8 | 裝備、娃娃、buff、魔法消除後的 MR | `getMr()` 綜合值 | `CS:56327-56342` | 推測一致（各 buff 的 MR 值屬 T107～T109） | — |

## 3. 技能成功機率（`L1MagicPc.calcProbabilityMagic` S1:`L1MagicPc.java:139-330`）

| # | 公式項目 | 1-伺服器 寫法 | LiTo 寫法 | 判定 | 測試 |
|---|---|---|---|---|---|
| P1 | 前置：PvP 安全區（`NZONE`）、大地屏障、千分比迴避；PvE 安全區、特殊 NPC | `L1MagicPc.java:144-254` | 各技能自己判；相消術 `L:NativeCancellationProbability.java:40-44` | 推測一致；千分比迴避 LiTo 只有相消術的 `evaded`（`:12`），**待驗證**競技場是否有千分比迴避來源（同 T101 H13） | — |
| P2 | 有 `skills_負面技能控制` 列 → 新公式：等級高／同／低三檔 + STR/DEX/INT×倍率 + 魔命 + 魔法等級 − MR×倍率 − CON/x − WIS/y − 耐性 | `L1MagicPc.java:400-409,739-791`；載入 `~/datatables/SkillsProbabilityTable.java:47-61` | `L:SourceSkillControls.java:149-165`；載入 `:85-96`；分派 `L:CombatProbability.java:22,35-37` | 一致（`db` 34 列 × 432 組輸入全測） | `configuredProbabilityMatchesCalcProbabilityWithConfigForEveryServer1Row` |
| P3 | STR、DEX、INT 項逐項累加（double） | `L1MagicPc.java:753-757` | 本 PR 改成逐項累加：`L:SourceSkillControls.java:152-153` | **已修正**（只是浮點相加順序；現行表的 STR/DEX 倍率都是 0，實際結果不變） | `configuredProbabilityUsesFractionalConDivisorsAndStatBonusesLikeTheReference` |
| P4 | 耐性類型 1 寒冰、2 石化、3 睡眠、4 暗黑、5 昏迷、6 支撐 | `~/templates/L1SkillProbabilityConfig.java:176-189`；`L1MagicPc.java:796-817` | `L:CombatProbability.java:181-184`（玩家目標）；`L:SourceSkillControls.java:159` | 一致 | 同 P2 |
| P5 | 衝擊之暈（87）表控機率後 + 裝備暈眩成功，再夾 1..100 | `L1MagicPc.java:404-407` | `L:SourceSkillControls.java:161` | 一致 | 同 P2 |
| P6 | 封頂 90 → BOSS ÷ modifier → 保底 1；BOSS modifier 0 = 免疫 | `L1MagicPc.java:196-201,260-269`；`~/datatables/BossSkillImmuneTable.java:40-45,60` | 表列：`L:SourceSkillControls.java:150,162-164`；非表列 NPC：`L:CombatProbability.java:24-33` | 一致（BOSS 表 40 列） | 同 P2 |
| P7 | 成功 = `probability ≥ rnd(1..100)` | `L1MagicPc.java:276-283` | `roll <= chance`：`L:CombatProbability.java:129,138`、`L:NativeCancellationProbability.java:13` | 一致 | — |
| P8 | 沒有表列 → `default` 舊公式：骰數 = `max(MagicBonus + 魔法等級 + (法師?+1:−1), 1)`，Σd(`probability_dice`) + 2×原始魔命 + 魔命 − MR | `L1MagicPc.java:648-693` | `L:BattleTurnUndead.java:39-66`（弱化、集體緩速、魔力奪取等共用）；相消術 `L:NativeCancellationProbability.java:49-57` | 一致 | `defaultProbabilityBranchMatchesCalcProbabilityDefault` |
| P9 | MagicBonus（`角色_智力成長.魔法加成`）：INT 40～43 = 15，**INT ≥ 44 = INT − 28**（表到 127 = 99） | `~/datatables/BaseStatTable.java:102-121,268-275`；`db:12052` 起；NPC 同值 `~/model/L1Character.java:3378-3446,3541-3542` | 本 PR 前 `L:BattleTurnUndead.java:14-33` 在 INT ≥ 40 封頂 15（另一份 `L:CombatKiringku.java:15` 才有 INT−28）。本 PR：`BattleTurnUndead.magicBonus` 改成表值，`CombatKiringku.magicBonus` 改為委派 | **已修正** | `magicBonusMatchesTheIntelligenceGrowthTableIncludingRowsAboveFortyThree` |
| P10 | 魔法等級（7 職業 `getMagicLevel`） | `~/model/classes/L1*ClassFeature.java:16-17` | `L:CombatRules.java:270-283` | 一致 | `magicLevelMatchesEveryClassFeature` |
| P11 | MR 項用 `getTargetMr`（含正義守護） | `L1MagicPc.java:424,444,554,583,619,645,678,768` | 見 R7 | **已修正** | 見 R7 |
| P12 | 舊公式特例：大地屏障／衝擊之暈／奪命之雷三檔、反擊屏障、致命身軀、恐慌、幻想、混亂、恐懼無助、護衛毀滅、驚悚死神、魔法封印／壞物術／緩速、冰矛 | `L1MagicPc.java:412-646` | 各技能的 `probability(...)`（`L:BattleSpellCatalog.java`）只在沒載入控制表時用 | 不適用：這 24 個技能在 `db` 的 `skills_負面技能控制` 都有列，正式環境一律走 P2 | — |
| P13 | 舊公式尾段 PvP 耐性 `>>1` | `L1MagicPc.java:696-732` | — | 不適用：只作用在 P12 的技能，已被 P2 蓋過 | — |

## 4. B12 結論：生命的祝福 +100

**結論：`NATURES_BLESSING_HEAL` 的實際值是 0，LiTo 目前的 0 是對的；「缺 +100」的說法不成立。**

- 1-伺服器 `~/../config/ConfigSkill.java:50` 宣告 `NATURES_BLESSING_HEAL`，但 `_loadFromProperties`（`:93-134`）**沒有讀這個鍵**，只有找不到設定檔時的 `_setDefaults`（`:207`）把它設成 0；正常載入時它維持 Java 預設值 0。
- 設定檔 `config/其他控制端/4-職業_技能設定表.properties:45` 寫 `NATURES_BLESSING_HEAL = 100`，但這行從沒被讀取，是無效設定。舊報告（LC B12 引用的 `combat-parity-0.6.68.md:21`）看的是設定檔文字，不是載入程式，所以結論錯。
- 兩個使用處在值為 0 時都沒有效果：`~/model/skill/L1SkillUse.java:2938-2939`（`!= 0` 才扣）、`~/model/proc/ProcSkillEffectHelper.java:206-207`（`+= 0`）。
- LiTo：`L:CombatRules.java:111-113` 不讀檔、固定 0 並記為 defaulted；使用處 `L:CombatHealing.java:7,22`、`L:CombatRules.java:214-216`。
- 測試 `naturesBlessingHealIsZeroEvenWhenTheSkillFileSaysOneHundred`：設定檔給 100，快照仍是 0。
- 若廠商**想要**生命的祝福 +100，那是新的設計決定（1-伺服器 也沒有這個效果），要另開任務並同時改 LiTo 的載入器與 `NativeNaturesBlessingSourceAuditTest`。

## 5. `CombatProbability`／`CombatMagicDamage` 呼叫點（確認在原始碼中）

| # | 項目 | 位置 | 判定 |
|---|---|---|---|
| C1 | `CombatMagicDamage` | `CS` 44 處（例：`:45065-45081` 單體、`:45914-45930` 範圍、`:82404-82420` 直線、`:84072-84093` 衝擊之暈 `direct`、`:86876-86892` 冰系範圍）；另有 `L:CombatCompanionDamage.java:33,40,47`、`L:CombatCounterDetection.java:9,23`、`L:CombatWorldFreeze.java:16`、`L:NativeSourceDollSkills.java:37` | **已確認**：都在 `src/main`（B-land #138 後），不依賴 `tools/*/build.py` patch |
| C2 | `CombatProbability` | `CS`：`dispatch` 62 處（例：`:76512-76532`）、`counterDecision` 8 處（例：`:48797`）、`mortalDecision`（`:27795`）、`npcMortalDecision`、`thunderSucceeds`（`:44823`）、`confusionChance／Succeeds`、`npcThunderDuration`、`bossGate` 103 處；另有 12 個類別（`CombatCompanionSpells`、`CombatNpcDebuffs`、`CombatReturnNature`、`NativeCancellationProbability`、`NativeWeaponBreakBattleCompanion` 等） | **已確認**：同上（LC B10 的「patch 接線」已不成立） |
| C3 | 控制表載入 | `L:CombatDatabase.java:42-96`（`skills`、`skills_負面技能控制`、`skills_負面boss免疫`、`npc`） | 已確認；只讀、單一 REPEATABLE READ 交易 |

## 6. 已修正項目摘要（本 PR 的 Java 變更）

1. **P9 MagicBonus**：`BattleTurnUndead.magicBonus` 在 INT ≥ 44 回傳 `min(INT,127) − 28`（原本封頂 15）；`CombatKiringku.magicBonus` 改為委派，兩份合一。影響：INT ≥ 44 的玩家施放走舊公式的技能（弱化術、集體緩速、魔力奪取、疾病術等）骰數增加。
2. **R7／P11 正義守護 MR**：新增 `CombatPlayerMagic.guardianMr/targetMr`。只在載入 1-伺服器 規則（`CombatRules.loaded()`）時生效：魔法傷害（`CombatMagicDamage.ordinary`、`finalBurn`）與機率（`CombatProbability.playerTarget`、非表列技能的 MR 參數）。相消術改傳基礎 MR，避免重複加。
3. **M2／M3 邪惡守護**：`ClientSession.sourceMagicDamageBeforeResistance`／`sourceNpcMagicBeforeResistance` 的骰值與 SP 各 +1～+3。刻意規則：只在骰值 > 0 時才加，避免把「沒有傷害」的技能變成有傷害（1-伺服器 這類技能根本不呼叫 `calcMagicDamage`）。
4. **M6 skill_level**：抽出 `BattleMagicResistance.skillLevel`，`ClientSession` 三處改用它（行為不變）。
5. **P3**：`SourceSkillControls.chance` 的素質項改為逐項累加（行為在現行表下不變）。

## 7. 待驗證／待裁定

| 代號 | 內容 | 下一步 |
|---|---|---|
| M3 | 風／生命／形象魔眼的 +1 SP 是否已算在 LiTo 的 `spellPowerModifier` | 讀 `BattleDragonEyeStatus` 與 `spellPowerModifier`，T107 一併確認 |
| M11 | `L1AttackList.SKD3` 清單與 `CombatPlayerMagic.reductions` 的逐筆對照 | T107～T109 |
| M17 | `isSkillEffectPhysicalDamageZero` 的狀態清單 | T107～T109 |
| P1 | 千分比迴避（`get_evasion`）在競技場是否有來源 | 與 T101 H13 一起裁定 |
| 實機 | 正義值 ≥ 10000 的角色被魔法打的傷害／被控機率略降；正義值 ≤ −10000 的角色魔法傷害略升；INT ≥ 44 的法師舊公式技能成功率上升 | 交 Codex 實機觀察 |

## 8. 表比對紀錄

- 方法：唯讀讀取 `D:\lineage開發\1-伺服器\db\380_db.sql`，用 `py -3 -I scratch/t304_skill_level.py`（skills）與 PowerShell `Select-String`（精神成長、智力成長、負面技能控制、BOSS 免疫）抽出 INSERT 列，原樣放進測試常數。
- `角色_精神成長.魔防`：51 列（0..50），與 `BattleMagicResistance.wisdomMagicResistance` 0 筆不一致。
- `角色_智力成長.魔法加成`：127 列；INT 1..43 與 LiTo 舊表相同，44..127 = INT−28，LiTo 本 PR 前 84 筆不一致（INT 44..127），本 PR 後 0 筆。
- `skills.skill_level`：219 筆 id < 1000，181 筆非佔位列全部等於 `((id−1)>>3)+1`；36 筆不符都是名稱 `none` 的空列（例：24、81～86、177～180、196～200）。
- `skills_負面技能控制` 34 列、`skills_負面boss免疫` 40 列：全數抄入測試。
