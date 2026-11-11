# 物理命中／傷害／AC 減傷：1-伺服器 ↔ LiTo 公式對照（T101，Issue #303）

- 參考（唯讀）：`D:\lineage開發\1-伺服器`，下表寫 `S1:` 加相對路徑與行號，`~` = `src/com/lineage/server`。
- LiTo：`L:` = `lito伺服器/LiTo381-Server/src/main/java/local/lito381/server/`，行號是本分支（task/303）修改後的行號；`CS` = `ClientSession.java`。
- 對照測試：`lito伺服器/LiTo381-Server/src/test/java/local/lito381/server/PhysicalFormulaServer1ParityTest.java`（下表「測試」欄寫方法名）。期望值用 1-伺服器 公式另寫一份，亂數用「列舉 `nextInt(n)` 每一面」的方式固定。
- 判定：**一致**＝逐值相等；**已修正**＝本 PR 改了 LiTo 讓它跟 1-伺服器 一致；**不一致・待裁定**＝有差異，但改了會大幅影響平衡，或有證據顯示 LiTo 是刻意這樣做，所以本 PR 不改，交給使用者決定；**不適用**＝競技場沒有這個系統。
- 範圍：PC 打 PC、PC 打 NPC 的物理攻擊（`L1AttackPc`）。NPC／召喚物打人（`L1AttackNpc`、`CS:52452 companionHitChance`）不在本任務範圍。

## 1. 命中（`L1AttackPc.calcPcHit` S1:`~/model/L1AttackPc.java:294-403`、`calcNpcHit` :409-534）

| # | 公式項目 | 1-伺服器 寫法 | LiTo 寫法 | 判定 | 測試 |
|---|---|---|---|---|---|
| H1 | 基礎命中 = 等級 + 職業命中補正 | `L1AttackPc.java:313-314`；`~/model/classes/L1*ClassFeature.java:26-27`（王族 /15、騎士 /10、妖精 /30、法師 /40、黑妖 /30、龍騎 /25、幻術 /40） | `L:BattlePhysicalAttack.java:121`、`:200-210` | 一致 | `classHitAndAttackLevelCorrectionsEqualTheClassFeatureDivisors` |
| H2 | 近戰用 STR 命中、遠攻（弓 20／鐵手甲 62）用 DEX 命中 | `L1AttackPc.java:316-333`；`~/utils/CalcStat.java:218/227` → `~/datatables/BaseStatTable.java:86-100,204-243`（查 `角色_力量成長`／`角色_敏捷成長`） | 執行時：`L:LegacyMySqlRepository.java:6394-6397` 讀同一張表；記憶體後備表：`L:Accounts.java:2392-2404`（`BattlePhysicalAttack.strengthHit/dexterityHit` `:175-198`） | 一致（127 個 STR、127 個 DEX，命中與傷害全部相等，見 §6） | `strengthAndDexterityTablesEqualThe380DbRowsForEveryStat` |
| H3 | 武器命中 + hitup + 原始 hitup + 強化×0.6 + 防具命中 | `L1AttackPc.java:321-323,330-332`（`_hitRate += (... + _weaponEnchant * 0.6)`，複合指定在整數加總後才截斷） | `L:SourceEquipmentPhysicalStats.java:248-258`（`(int)(enchant*0.6)` 單獨截斷再相加） | 強化值 ≥ 0 時一致；**負強化**時有 0～1 的差（1-伺服器 對整體截斷，LiTo 只截斷強化項）。影響極小，不改，記錄為推測 | — |
| H4 | 負重命中修正（weight240 81-120 −1、121-160 −3、161-200 −5） | `L1AttackPc.java:335-346` | 沒有 | 不適用：競技場 LiTo 不計負重命中（`L:BattleInventoryWeight` 只管負重上限）。**待驗證**：WORLD 模式是否需要 | — |
| H5 | 媽祖祝福 +1、`hitUp()`（`getCombatHitBonus`） | `L1AttackPc.java:348-352,540-543` | 各 buff 個別加：`CS:48755-48771`（WORLD）、`CS:85864-85901`（競技場） | 推測一致（buff 清單是 T102 範圍）；媽祖不適用 | — |
| H6 | 攻擊骰 = `nextInt(20)+2+hit−10` | `L1AttackPc.java:354` | `L:BattlePhysicalAttack.java:126-127`（`d20+1+hit−10`，d20 ∈ 1..20） | 一致（兩者都是 2..21） | `playerTargetHitEnvelopeMatchesCalcPcHitForEveryFixedDie` |
| H7 | 目標閃避：`attackerDice -= clamp((int)(dodge/8),1,20)`、`+= (int)(dodgeDown/8)` | `~/model/L1AttackMode.java:141-186`（迴避／魅影等技能 `add_dodge(5)`：`~/model/skill/skillmode/UNCANNY_DODGE.java:26`、`MIRROR_IMAGE.java:26`、`RESIST_FEAR.java:26`） | `L:BattlePhysicalAttack.java:126-127`（直接減 dodge、加 dodgeDown，數值 0..10） | **不一致・待裁定**：LiTo 是 3.81 原版語意（迴避 5 點 = 命中骰 −5），而 1-伺服器 為了 100 刻度的裝備閃避（`~/model/L1EquipmentSlot.java:650`）改成 /8，結果同樣的技能只扣 1。LiTo 有測試明確釘住「直接扣」（`BattlePhysicalAttackTest.targetDodgePointsDirectlyLowerTheAttackerD20Envelope`），判定為刻意保留。**T113（#318）裁定：維持 LiTo（刻意規則）**——閃避點數 0..10 直接加減攻擊骰，不做 /8 | `PhysicalBalanceDecisionT113Test.h7DodgePointsStillSubtractDirectlyInsteadOfDividingByEight`、`BattlePhysicalAttackTest.targetDodgePointsDirectlyLowerTheAttackerD20Envelope` |
| H8 | 龍騎 `attackerDice *= 1.01`；火屬性妖精 `*= 1.02` | `L1AttackPc.java:372-379`、`:491-498` | 本 PR 新增 `L:BattlePhysicalAttack.java:163-167`，在 `:128` 套用 | **已修正**（原本 LiTo 沒有） | `dragonKnightAndFireElfAttackerDieScalesTruncateLikeTheCompoundAssignment`、`playerTargetHitEnvelope…` |
| H9 | 玩家目標防禦骰：AC ≥ 0 → `10−AC`；AC < 0 → `10+nextInt((int)(AC*1.5)*−1)+1` | `L1AttackPc.java:359-368` | `L:BattlePhysicalAttack.java:131-142`、`:169-173` | 一致 | `playerTargetHitEnvelope…` |
| H10 | NPC 目標防禦骰 = `10 − NPC AC`（**沒有**負 AC 亂數） | `L1AttackPc.java:487` | 本 PR 前：NPC 也走玩家的負 AC 亂數（`CS:47800-47805`、`CS:85925-85930`）。本 PR 後：`L:BattlePhysicalAttack.java:125-130`，呼叫點 `CS:47801`（WORLD 打怪）、`CS:85923-85929`（競技場：只有玩家目標 `var10 != null` 才擲負 AC 骰） | **已修正** | `npcTargetHitEnvelopeUsesTenMinusAcWithoutTheNegativeAcRoll` |
| H11 | 失手／必中門檻：`fumble = hit−9`、`critical = hit+10`，其餘 `attacker > defender` 才 100，否則 15 | `L1AttackPc.java:370-394` | `L:BattlePhysicalAttack.java:144-148` | 一致 | `playerTargetHitEnvelope…` |
| H12 | 最終判定 `_hitRate >= rnd(1..100)`；弓且 `_hitRate > rnd` 時再判 ER：`er < rnd` | `L1AttackPc.java:396-402`、`L1AttackMode.java:224-228` | `L:BattlePhysicalAttack.java:151-156`；`L:BattleRangedEvasion.java:53-58`；呼叫 `CS:48787-48791`、`CS:85933-85940`（條件 `bow() && chance > roll`） | 一致 | `finalPercentRollAndBowErMatchTheReferenceComparisons` |
| H13 | 迴避（evasion，千分比）前置判定 | `L1AttackMode.java:234-244` | 沒有獨立 evasion 欄位 | 不適用（LiTo 沒有千分比迴避來源）；**待驗證** | — |
| H14 | 特定 NPC 需技能／變身（`L1AttackList.SKNPC/PLNPC`）、南瓜影子 | `L1AttackPc.java:411-418,515-529` | 沒有 | 不適用（競技場沒有這些 NPC） | — |
| H15 | 奇古獸（weaponType2 17）必中 | `L1AttackPc.java:309-311,436-438` | `L:CombatKiringku.java:10`（`hit(previous, weapon)`），`CS:48794`、`CS:86006` | 一致 | — |

## 2. 武器骰與基礎傷害（`weaponDamage1` S1:`L1AttackPc.java:607-700`、`weaponDamage2` :707-833、`calcPcDamage` :909-968、`calcNpcDamage` :1088-1167）

| # | 公式項目 | 1-伺服器 寫法 | LiTo 寫法 | 判定 | 測試 |
|---|---|---|---|---|---|
| D1 | 近戰武器骰：有 `dmg_max_chance` 時，中 → 最大值、沒中 → `nextInt(max−1)+1`（max=1 時為 1）；沒設定 → `nextInt(max)+1` | `L1AttackPc.java:646-666` | `L:BattleWeaponCritical.java:31-65`；擲骰 `CS:85962-85982` | 一致 | `meleeWeaponDieMatchesWeaponDamage1WithAndWithoutMaximumControl` |
| D2 | 鋼爪（11）：基礎 0，`dmg_max_chance` 觸發時才是最大值；`weaponDamage2` 再 +5 | `L1AttackPc.java:626-629,671-679,727-734` | `L:BattleWeaponCritical.java:50-52`、`:103-104` | 一致 | `clawIsZeroUntilMaximumControlTriggersThenAddsFiveBeforeTheMultiplier` |
| D3 | 弓／鐵手甲：骰子來自箭／飛刀（大小目標分開），觸發最大值時取最大 | `L1AttackPc.java:614-624,765-783,840-848` | `L:BattlePhysicalDamage.java:41-57`；`L:BattleWeaponCritical.java:42-49` | 一致 | `BattleWeaponCriticalTest.rangedMaximumControlSelectsTheProjectileMaximum`（既有） |
| D4 | **弓的 DEX 傷害**：PvP ×2.5、PvE ×1.5（只限弓 4／13，不含鐵手甲） | `L1AttackPc.java:748-764` | 沒有倍率：`CS:85908`（`requiredPhysicalGrowthStats().damage(...)` 直接加）、`CS:48814` | **不一致・待裁定**：影響妖精平衡很大（DEX 傷害 8 → PvP 20）。新對戰弓規則 `L:ModernRangedCombat.java:7-8` 寫明「not current classic's DEX*2.5」，表示 LiTo 知道這條規則而沒採用；傳統路徑是否也要套用由使用者決定。**T113（#318）裁定：維持 LiTo（刻意規則）**——弓的 DEX 傷害直接相加，不乘 2.5／1.5 | `PhysicalBalanceDecisionT113Test.d4BowDexterityDamageIsAddedWithoutThePvpOrPveMultiplier` |
| D5 | 魔法弓沒箭時 1～15 | `L1AttackPc.java:769-773` | 沒有（沒箭直接拒絕攻擊 `CS:85831-85843`） | 不一致・待驗證（要先確認 3.81C 客戶端有沒有魔法弓物品） | — |
| D6 | 空手 `(nextInt(5)+4) >> 2` | `L1AttackPc.java:744-746` | `L:BattlePhysicalDamage.java:76-81` | 一致 | `unarmedDamageIsTheShiftedFourThroughEightRoll` |
| D7 | 傷害小計 = 骰 + 武器追加 + 強化 + dmgup + 防具傷害 + STR／DEX 傷害 | `L1AttackPc.java:938-963`、`:1131-1156` | `L:BattlePhysicalDamage.java:22-73`；裝備加總 `L:SourceEquipmentPhysicalStats.java:264-274` | 一致（項目對應；buff 清單屬 T102） | `strengthAndDexterityTables…`（STR/DEX 傷害） |
| D8 | PvP 武器骰固定用小型目標骰（`_weaponSmall`）；PvE 依 NPC 大小 | `L1AttackPc.java:927`、`:1106-1121` | `L:BattlePhysicalDamage.java:7-11`；`CS:48808`（PvP `false`）、`CS:85956`（`var24.large()`） | 一致 | — |
| D9 | **職業攻擊等級補正** `getAttackLevel`（王族 /10、騎士 /6、妖精 /12、法師 /20、黑妖 /16、龍騎 /13、幻術 /16），所有武器都加 | `L1AttackPc.java:860`；`~/model/classes/L1*ClassFeature.java:21-22` | 只有奇古獸加：`CS:85985`（`var191 ? classAttackLevel : STR傷害`）、`L:CombatKiringku.java:49`；一般武器沒有 | **不一致・待裁定**：除數本身一致（`classHitAndAttackLevel…` 已測），但一般武器少加這一項，騎士 52 級每下少 8。改動會讓整個近戰傷害上升、牽動大量既有自測，本 PR 不改。**T113（#318）已修正：對齊 1-伺服器**——`L:BattlePhysicalDamage.withClassAttackLevel` 在倍率之後、減傷之前加上（同 `pcDmgMode`）；呼叫點：競技場（`var147`）、WORLD 打怪（`var79`）、WORLD PvP（`n35`）；奇古獸維持自己的算法（`CombatKiringku.finish` 本來就有加） | `classHitAndAttackLevelCorrectionsEqualTheClassFeatureDivisors`、`PhysicalBalanceDecisionT113Test.d9*` |
| D10 | 被動屬性傷害：`floor = (int)(0.32×|resist|)`（負抗性取負），`damage *= 1 − floor/32` | `L1AttackPc.java:1399-1516` | `L:BattlePhysicalOffense.java:76-88` | 一致 | `passiveAttributeDamageMatchesCalcAttrEnchantDmgCoefficient` |
| D11 | 屬性抗性來源：PvP 用目標地火水風（光暗聖邪用 MR）；PvE 用 NPC 的地火水風 | `L1AttackPc.java:1429-1502` | PvP：`CS:85999`（`effectiveWeaponAttributeResistance`）；PvE：`L:BattlePhysicalOffense.java:53-66`（光暗聖邪用 MR，地火水風用 `weakAttribute` ±50） | PvP 一致（推測）；**PvE 不一致・待驗證**：LiTo 用弱點屬性 ±50 代替 NPC 實際四屬性抗性，需要先確認 `380_db` npc 表的抗性欄位。**T113（#318）裁定：維持 LiTo（刻意規則）**——PvE 地火水風用弱點屬性 ±50，光暗聖邪用 MR | `PhysicalBalanceDecisionT113Test.d11NpcElementalResistanceStaysTheWeakAttributePlusMinusFifty` |
| D12 | 材質：不死 1/3/5 對銀／米索莉／奧里哈魯根 d20、惡魔 2 對米索莉／奧里哈魯根 d3；祝福 d4（1/2/3） | `L1AttackPc.java:1356-1393` | `L:BattlePhysicalOffense.java:27-51` | 骰子大小一致；**祝福旗標待驗證**：1-伺服器 判 `_weaponBless == 1`（註解「內部 1=祝福」），LiTo 判 `attackBlessing() == 0`（3.81 原版 0=祝福），要確認兩邊讀進來的值是否已經換算過。**T113（#318）已修正**：`380_db` 的 weapon／etcitem `bless` 是 1-伺服器 內部值（0=一般、1=祝福；`380_db.sql` weapon 表 361 把一般武器都存 0），LiTo `LegacyEquipmentMagicLoader` 原本沒換算，結果是**一般武器打不死系都多擲祝福 d4，真正的祝福武器反而沒有**。現在載入時用 `BattlePhysicalOffense.nativeBlessingFromSource` 換成 LiTo 原生慣例（0=祝福） | `undeadMaterialBonusMatchesCalcMaterialBlessDmgDieSizes`、`PhysicalBalanceDecisionT113Test.d12*` |
| D13 | 倍率（武器雙擊 `dmg_cri`、雙重破壞、屠宰 Lv3、double_dmg、三重矢）只取最高，不連乘；在加完小計後才乘 | `L1AttackPc.java:810-823,875-902` | `L:BattleWeaponCritical.java:78-115`；候選倍率 `CS:86029-86061` | 一致 | `multipliersTakeTheHighestCandidateInsteadOfMultiplying` |

## 3. 防禦與減傷

| # | 公式項目 | 1-伺服器 寫法 | LiTo 寫法 | 判定 | 測試 |
|---|---|---|---|---|---|
| R1 | 玩家 AC 減傷：`ac = max(0,10−AC)`，`max = 職業除數`（王族／妖精／龍騎 /3、騎士 >>1、法師 /5、黑妖／幻術 >>2），`nextInt(max) + max(1, max>>3)` | `~/model/L1AttackMode.java:250-269`；`~/model/classes/L1*ClassFeature.java:11-12` | `L:BattlePhysicalDefense.java:42-51`、`:99-104` | 一致 | `playerAcReductionMatchesCalcPcDefenseForEveryClassAcAndRoll` |
| R2 | AC 額外減傷：`AC_REDUCTION_DIVISOR > 0` 且 AC < 0 時 `(int)(|AC|/除數)`，並夾到 0 | `L1AttackPc.java:978-983`；`~/../config/ConfigPcDamage.java:59,122` | `L:CombatRules.java:254-264` | **已修正**：原本只排除除數 = 0，負的除數會變成「加傷」；現在只接受 > 0 | `acExtraReductionOnlyAppliesAStrictlyPositiveDivisor` |
| R3 | 裝備減免（`getDamageReductionByArmor`）− 無視減免，最低 0；再扣近／遠距離減免 | `L1AttackPc.java:985-1013` | `L:BattlePhysicalDefense.java:73-116` | 一致（各項皆非負時，分段夾 0 與 LiTo 一次夾 0 結果相同） | `armorReductionIgnoreAndDirectionalReductionSubtractLikeCalcPcDamage` |
| R4 | NPC 減傷：`max = max(0,10−AC)/7`，有 max 時 `nextInt(max)+max(1,max)+damagereduction`，否則只有 damagereduction | `~/model/L1AttackMode.java:275-293` | `L:BattlePhysicalDefense.java:16-39` | 一致 | `npcAcReductionMatchesCalcNpcDamageReductionIncludingDamageReduction` |
| R5 | 硬皮怪：遠攻且沒有貫穿 → 減傷後 /2 | `L1AttackPc.java:1187-1201` | `L:BattlePhysicalDefense.java:35-36`；貫穿 `CS:86084` | 一致 | `hardSkinHalvesRangedDamageAfterReductionUnlessPierced` |
| R6 | PvP 加傷／減免（`getPvpDmg`／`getPvpRedux`）、紋章減免 `coatArms`、天賦傷害、`getCombatDmgBonus`／`isCombatDmgHalve`、對 BOSS 增傷 | `L1AttackPc.java:974,1015-1016,1031-1038,1054-1056,1209-1212` | 沒有 | 不適用（1-伺服器 自訂系統，3.81C 競技場沒有）；**待驗證**：客戶是否要求 | — |
| R7 | 增幅防禦（REDUCTION_ARMOR）`(max(Lv,50)−50)/5+10`、破壞盔甲近戰 ×1.58 | `L1AttackPc.java:1020-1029` | `L:CombatKiringku.java:54-57`（奇古獸）；一般武器走 `L:BattleSpellCatalog` 的技能狀態 | 屬技能範圍（T102），本表不判定 | — |
| R8 | 寵物／召喚物目標 ×`PC_TO_PET_DAMAGE_MULTIPLIER`（預設 0.125）、聖結界 /IMMUNE_TO_HARM | `L1AttackPc.java:1172-1185`；`ConfigPcDamage.java:125` | `L:CombatCompanionDamage.java`（`KEY` 預設 0.125，`L:CombatRules.java:126`）；`L:CombatRules.java:266-269` | 一致（推測，未逐值測） | — |

## 4. 職業傷害倍率（`ConfigPcDamage`）與 `CombatClassDamage` 呼叫點

| # | 公式項目 | 1-伺服器 寫法 | LiTo 寫法 | 判定 | 測試 |
|---|---|---|---|---|---|
| C1 | 鍵名 `Other_To_is{Crown,Knight,Elf,Wizard,Darkelf,DragonKnight,Illusionist}_{pc,npc}`；空值或格式錯 → 1.0 | `~/../config/ConfigPcDamage.java:95-110,128-139` | `L:CombatClassDamage.java:8-24` | 一致；LiTo 另外拒絕 NaN／無限大（1-伺服器 會吃進 NaN）——LiTo 較嚴格，屬安全性差異，不改 | `classDamageMultiplierKeysAndFallbackMatchConfigPcDamage` |
| C2 | 套用位置：PvP 在所有減傷之後 `dmg *= 倍率`（`:1041-1042`）；PvE 在硬皮之後（`:1206-1207`） | `L1AttackPc.java:1040-1042,1205-1207` | `L:CombatClassDamage.java:25-29` 由 `L:CombatKiringku.java:36`（一般武器）／`:62`（奇古獸）呼叫 | 一致（位置相同：減傷後、最終截斷前） | — |
| C3 | **呼叫點在 `src/main` 原始碼**（B-land 後，非 patch） | — | 競技場：`CS:86160-86175` → `L:CombatCompanionDamage.java:24-29` → `L:CombatKiringku.java:36` → `CombatClassDamage.physical`；WORLD PvP：`CS:48847` → `CombatKiringku.finish` → `:36`；WORLD 打怪：`CS:47930` → `L:CombatWorldPhysical.java:50` → `CombatKiringku.finish`；`CombatClassDamage.hit`：`CS:47948`、`CS:48848`、`CS:86175` | **已確認**：三條物理路徑都在 `src/main` 呼叫，不依賴 `tools/*/build.py` patch | 本機 `javac` 全量編譯通過；CI |

## 5. 已修正項目摘要（本 PR 的 Java 變更）

1. `BattlePhysicalAttack.hitChanceAgainst(npcTarget, type, elfAttribute, …)`：NPC 目標用 `10 − AC`；套用龍騎 1.01／火妖精 1.02 攻擊骰倍率。舊的 `hitChanceWithStatBonus` 多載保留，改成委派（`npcTarget=false, elfAttribute=0`），既有測試與新對戰交叉測試的行為不變（龍騎 ×1.01 只在攻擊骰 ≥ 100 時才多 1，且那時已經是必中）。
2. `ClientSession` 三個呼叫點改用 `hitChanceAgainst`，傳入 `this.elfAttribute`；WORLD 打怪、競技場打 NPC 不再擲負 AC 骰。
3. `CombatRules.extraAcReduction(ac, divisor)`：只有除數 > 0 才生效。

## 6. 素質表比對紀錄

- 方法：`scratch/t303_growth_compare.py`（唯讀解析 `1-伺服器/db/380_db.sql` 的 `角色_力量成長`、`角色_敏捷成長` INSERT，`py -3 -I` 執行），對照 LiTo 記憶體後備表的公式。
- 結果：兩張表各 127 列（id 1..127），命中與傷害 **0 筆不一致**。表內容也原封寫進測試常數 `REF_STR_TABLE`／`REF_DEX_TABLE`。
- 執行時 LiTo 直接讀 `380_db` 的同一張表（`L:LegacyMySqlRepository.java:6394-6397`），所以正式環境與後備表都一致。

## 7. 裁定結果（T113，Issue #318）

使用者在 #318 沒有回覆，調度器依逾時預設處理（H7、D4、D11 維持；D9 對齊 1-伺服器；D12 是正確性修正）。「維持」的項目是**刻意的 LiTo 規則**，不是遺漏；以後要改請另開任務，並同步修改下表的釘住測試。

| 代號 | 差異 | 裁定 | 實作／規則位置 | 釘住測試（`PhysicalBalanceDecisionT113Test`） | 影響 |
|---|---|---|---|---|---|
| H7 | 閃避：LiTo 直接扣，1-伺服器 /8（至少 1、最多 20） | **維持 LiTo**（3.81 原版語意：迴避 5 點 = 攻擊骰 −5） | `L:BattlePhysicalAttack.hitChanceAgainst`（dodge 0..10 直接加減） | `h7DodgePointsStillSubtractDirectlyInsteadOfDividingByEight` | 不變 |
| D4 | 弓 DEX 傷害 PvP ×2.5、PvE ×1.5 | **維持 LiTo**（不乘倍率） | `L:BattlePhysicalDamage.sourceDamage` 的 `statDamage` 直接相加；`L:ModernRangedCombat.java:7-8` | `d4BowDexterityDamageIsAddedWithoutThePvpOrPveMultiplier` | 不變 |
| D9 | 一般武器少了職業攻擊等級補正 | **對齊 1-伺服器** | `L:BattlePhysicalDamage.withClassAttackLevel`：倍率之後、減傷之前加 `getAttackLevel`（同 S1:`~/model/L1AttackPc.java:860` `pcDmgMode`）；接在競技場、WORLD 打怪、WORLD PvP 三條物理路徑；沒命中、奇古獸不加 | `d9ClassAttackLevelMatchesGetAttackLevelForEveryClass`、`d9TermIsAddedAfterTheMultiplierLikePcDmgMode`、`d9SkipsMissesAndKiringkuWhichAlreadyAddsItsOwnTerm` | 全職業物理每下 +Lv/6～Lv/20（52 級騎士 +8） |
| D11 | PvE 屬性抗性用弱點 ±50 代替 NPC 四屬性抗性 | **維持 LiTo** | `L:BattlePhysicalOffense.attributeResistance` | `d11NpcElementalResistanceStaysTheWeakAttributePlusMinusFifty` | 不變 |
| D12 | 祝福旗標 0／1 慣例 | **正確性修正**（不需裁定） | `L:BattlePhysicalOffense.nativeBlessingFromSource`；`L:LegacyEquipmentMagicLoader` 的武器樣板 `bless` 與箭／飛刀 `etcitem.bless` 在載入時換算 | `d12SourceBlessIsSwappedFromInternalToNativeConvention`、`d12OnlyADatabaseBlessedWeaponRollsTheD4AgainstUndeadLikeCalcMaterialBlessDmg`、`d12ProjectileBlessFromEtcitemUsesTheSameConversion` | 一般武器打不死系不再多 d4；祝福武器才有 |
