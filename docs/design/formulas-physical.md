# 物理命中／傷害／AC 減傷：1-伺服器 ↔ LiTo 公式對照（T101，Issue #303）

- 參考（唯讀）：`D:\lineage開發\1-伺服器`，下表寫 `S1:` 加相對路徑與行號，`~` = `src/com/lineage/server`。
- LiTo：`L:` = `lito伺服器/LiTo381-Server/src/main/java/local/lito381/server/`，行號是本分支（task/303）修改後的行號；`CS` = `ClientSession.java`。
- 對照測試：`lito伺服器/LiTo381-Server/src/test/java/local/lito381/server/PhysicalFormulaServer1ParityTest.java`（下表「測試」欄寫方法名）。期望值用 1-伺服器 公式另寫一份，亂數用「列舉 `nextInt(n)` 每一面」的方式固定。
- 判定：**一致**＝逐值相等；**已修正**＝本 PR 改了 LiTo 讓它跟 1-伺服器 一致；**不一致・待裁定**＝有差異，但改了會大幅影響平衡，或有證據顯示 LiTo 是刻意這樣做，所以本 PR 不改，交給使用者決定；**不適用**＝競技場沒有這個系統。
- 範圍：§1–§7 是 PC 打 PC、PC 打 NPC 的物理攻擊（`L1AttackPc`，T101／#303）。NPC、寵物、召喚物打人與打怪（`L1AttackNpc`）在 §8（T112／#317），§8 的行號是 task/317 分支修改後的行號。

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
| D12 補（T120／#339） | 祝福來源：樣板 vs. 物品實例；幻象 bless=3 | **對齊 1-伺服器（看樣板）**；幻象可載入 | `L:NativeDeItemInventory.updatePhysical` 不再用物品實例 bless 覆寫戰鬥祝福；`L:RuntimeInventory` 租借武器改用 `rental-item-definitions-380.tsv` 樣板 `bless` 經 `nativeBlessingFromSource` 換算；`SourceWeaponCombatTraits`／`SourceProjectilePhysicalStats` 接受 0..3 | `ItemBlessConsistencyT120Test` | 幻象之劍／弓裝備後不再丟例外、也不擲祝福 d4；13 把租借武器（樣板 bless=0）不再被當成祝福武器 |

§8.4 N1（NPC 打玩家的閃避）與 H7 是同一條規則：H7 維持，N1 也跟著維持 LiTo 直接扣。

## 8. NPC／寵物／召喚物攻擊（`L1AttackNpc`，T112／Issue #317）

- 參考（唯讀）：`S1:~/model/L1AttackNpc.java`（SHA256 `7BA89744…`）、`S1:~/model/L1AttackMode.java`（`F29C4D0B…`）。LiTo 的 `ClassicWorldCompanionNpcPhysicalAttack.L1_ATTACK_NPC_SOURCE_SHA256` 記的就是這兩個雜湊，與目前 1-伺服器 檔案相同（本機 `Get-FileHash` 比對），表示 LiTo 是對著同一版移植。
- 對照測試：`lito伺服器/LiTo381-Server/src/test/java/local/lito381/server/NpcAttackFormulaServer1ParityTest.java`（13 個測試方法，下表「測試」欄寫方法名）。寫法同 §1–§7：用 1-伺服器 公式另寫一份參考函式，列舉每個 `nextInt(n)` 面。
- `CS` = `ClientSession.java`；`NPA` = `BattleNpcPhysicalAttack.java`；`NPS` = `CombatNpcPhysicalSource.java`；`CNA` = `ClassicWorldCompanionNpcPhysicalAttack.java`；`NPD` = `CombatNpcPlayerDamage.java`；`NWB` = `NativeNpcWeaponBreakDamage.java`。

### 8.0 LiTo 的 NPC 物理攻擊路徑

| 代號 | 路徑 | 命中 | 傷害 | 對應 1-伺服器 |
|---|---|---|---|---|
| P1 | WORLD 怪物 → 玩家（普攻） | `CS:77326`（`NPA.hitChance`）、ER `CS:77332-77337` | `CS:77355-77374`（`NWB.sourceExact` → AC 減傷 → 防具） | `calcPcHit`／`calcPcDamage` |
| P2 | WORLD 怪物 → 玩家／寵物（物理技能） | `CS:28418` | `CS:28438-28484` | 同上；目標是寵物時應走 `calcNpcHit`／`calcNpcDamage` |
| P3 | WORLD 怪物 → 自己的寵物／召喚物 | `CS:77178` | `CS:77190-77216` | `calcNpcHit`／`calcNpcDamage` |
| P4 | WORLD 怪物 → 護送 NPC（Dspy） | `CS:77246` | `CS:77252-77268` | `calcNpcHit`／`calcNpcDamage` |
| P5 | WORLD 寵物／召喚物 → 玩家 | `CS:51923`、ER `CS:51926-51928` | `CS:51942` → `NPD.prepare:16-26`、`NPD.finish:28-35` | `calcPcHit`／`calcPcDamage` |
| P6 | WORLD 寵物／召喚物 → 怪物 | `CS:51992`（`CNA.hit`） | `CS:52001`（`CNA.damage`） | `calcNpcHit`／`calcNpcDamage` |
| P7 | 競技場召喚物 → 玩家／NPC | `CS:52317`、`CS:79194`、`CS:79390` | 玩家：`NPD.prepare`（`CS:52335`、`79210`、`79406`）；NPC：`companionNpcPhysicalDamage`（`CS:52324`、`79201`、`79397`） | 玩家：`calcPcHit`／`calcPcDamage`；NPC：`calcNpcHit`／`calcNpcDamage` |
| P8 | 競技場 NPC（Tournament／Time Attack）→ 玩家 | `CS:53588`、`53868`、`54129` | `BattleTournamentNpcPhysicalDamage.java:15-33`（`CS:53640`、`53917`；`BattleTimeAttackPhysicalDamage.java:38`） | `calcPcHit`／`calcPcDamage` |

### 8.1 命中

| # | 公式項目 | 1-伺服器 寫法 | LiTo 寫法 | 判定 | 測試 |
|---|---|---|---|---|---|
| N-H1 | NPC → PC 基礎命中 = Lv + 5 + hitup（寵物另加武器命中） | `L1AttackNpc.java:126-132` | `NPA:23`；P5／P7 `CS:52527-52532` | 一致。LiTo 的 hitup 一律傳 0：1-伺服器 的 NPC hitup 只由 GM 指令或 buff 寫入（`~/command/executor/L1Status.java:75`），`npc` 表沒有這個欄位；寵物武器命中＝不適用（3.81C 沒有寵物裝備封包，`CNA:7-10` 已註明） | `npcToPlayerHitEnvelopeMatchesCalcPcHitForEveryFixedDie` |
| N-H2 | 攻擊骰 = `nextInt(20)+1+hit−3` | `L1AttackNpc.java:135` | `NPA:24`；`CS:52495`（競技場 3 參數版） | 一致 | 同上 |
| N-H3 | 目標閃避：`attackerDice(_targetPc)`（dodge /8，至少 1、最多 20） | `L1AttackNpc.java:138`；`L1AttackMode.java:141-186` | 怪物路徑 `NPA:24-25` 直接扣（0..10）；**召喚物路徑原本完全不扣**（`companionHitChance` 沒有 dodge 參數） | /8 vs 直接扣：**不一致・待裁定**（與 §1 H7 同一條規則）。召喚物不扣閃避：**已修正**——P5／P7 打玩家時改用 `CS:52516-52532 companionHitChanceAgainst`，跟怪物路徑一樣扣 `effectivePhysicalDodge`／加 `effectivePhysicalDodgeDown`（超過 0..10 的值夾住，不丟例外） | `companionPlayerHitNowAppliesTargetDodgeLikeTheMonsterPath` |
| N-H4 | 玩家防禦骰：AC ≥ 0 → `10−AC`；AC < 0 → `10+nextInt(−AC)+1`（**不是** PC 打人的 ×1.5） | `L1AttackNpc.java:141-150` | `NPA:27-39`、`NPA:53-55`；`CS:52496-52504` | 一致 | `npcToPlayerHitEnvelope…` |
| N-H5 | 失手／必中：`fumble = hit`、`critical = hit+19`；其餘 `attacker > defender` → 100，否則 15 | `L1AttackNpc.java:152-171` | `NPA:40-42`；`CS:52505-52507` | 一致 | 同上 |
| N-H6 | BOSS `attackerDice += 30` | `L1AttackNpc.java:173-176`（在 `_hitRate` 決定後才加，之後沒有再讀 `attackerDice`） | 沒有 | 一致：靜態讀碼確認這行在 1-伺服器 **沒有效果**（解掉 `server1-inventory.md` §3 的推測） | — |
| N-H7 | 最終 `_hitRate >= rnd(1..100)` | `L1AttackNpc.java:179,187` | `NPA.hits:45-51`；`CS:51924` 等（`roll <= chance`） | 一致 | `npcHitRollAndRangedErUseTheReferenceComparisons` |
| N-H8 | 遠攻 ER：`ranged >= 10 && _hitRate > rnd && 距離 >= 2` → `er < rnd(1..100)` | `L1AttackNpc.java:181-185`；`L1AttackMode.java:224-228` | P1 `CS:77332-77337`；P5 `CS:51926-51928`；P8 `CS:53598`、`53874`、`54135`；`BattleRangedEvasion.java` | 一致 | 同上 |
| N-H9 | 千分比迴避 `calcEvasion` | `L1AttackNpc.java:121`；`L1AttackMode.java:234-244` | 沒有 | 不適用（同 §1 H13） | — |
| N-H10 | 物理傷害為 0 狀態、視線、和平區 | `L1AttackNpc.java:88-97,116` | 冰凍／絕對屏障／大地屏障 `CS:77338` 等 | 推測一致（狀態清單屬 T102） | — |
| N-H11 | NPC → NPC 命中 = Lv + **3** + 寵物武器命中 + hitup；BOSS +30 **在防禦骰之前**（有效） | `L1AttackNpc.java:200-250` | P6 `CNA:47-59` | 一致 | `npcToNpcHitEnvelopeMatchesCalcNpcHitIncludingBossBeforeDefense` |
| N-H12 | NPC → NPC 的其他路徑 | 同 N-H11 | P3 `CS:77178`、P4 `CS:77246`、P7 打 NPC（`CS:52317`、`79194`、`79390`）都用 NPC → PC 的 Lv + 5 包絡，BOSS 不加 30 | **不一致・待裁定**（見 §8.4 N2） | — |

### 8.2 傷害

| # | 公式項目 | 1-伺服器 寫法 | LiTo 寫法 | 判定 | 測試 |
|---|---|---|---|---|---|
| N-D1 | STR 傷害補正表 `STRD`（0..254） | `~/model/L1AttackList.java:517-543` | `NPS:13-21`；新增 `NPS:29-31 npcStrengthDamage`（夾到 0..254） | 表一致。**已修正**：WORLD 的 NPC 路徑原本用玩家的 `角色_力量成長`（`requiredPhysicalGrowthStats().damage(false, STR)`），STR 1..127 與 STRD 相同，但該表沒有 STR 0 列（夾到第 1 列 −5，STRD(0) = −6）；`380_db` 有 102 隻 STR 0 的怪物（最高 Lv 100）。`CS:28440`、`77194`、`77256`、`77357`、`52001` 改用 `npcStrengthDamage` | `strdTableMatchesL1AttackListForEveryKey`、`pcGrowthTableEqualsStrdOnlyForStrengthOneThroughOneTwentySeven` |
| N-D2 | NPC → PC 基礎 = `nextInt(Lv) + STR×0.8 + STRD` | `L1AttackNpc.java:318-322` | `NPS.base:32-37`；`NWB:7` | 一致 | `worldMonsterSourcePrefixMatchesCalcPcDamageBaseAndNpcDmgMode` |
| N-D3 | 寵物另加 `Lv/7` 與武器傷害 | `L1AttackNpc.java:324-327` | `NPS.base:35`（武器傷害傳 0，同 N-H1） | 一致 | `petBaseAddsLevelOverSevenAndWeaponDamage` |
| N-D4 | `npcDmgMode`：15% ×1.8 → +dmgup → 不死夜間 ×1.2 → `(int)(×leverage/10)` → BOSS ×1.8 → 武器破壞每層 −5%（最低 1） | `L1AttackNpc.java:274-302` | `NPS.mode:39-52`；`NWB:5-18` | 一致。**已修正**：P5／P7 的後備公式 `CS:52538-52556 companionPhysicalDamage`（`CombatRules` 未載入或 ARENA 召喚物時用）原本暴擊後沒有 `(int)` 截斷，STR 傷害用 `floorDiv(STR−11, 2)`（STR ≥ 25 起與 STRD 不同），改成直接呼叫 `NPS.base`／`NPS.mode` | `npcDamageModeMatchesNpcDmgModeForEveryCriticalFace`、`companionFallbackDamageMatchesCalcPcDamageThroughThePetMultiplier` |
| N-D5 | 不死系夜間判定（遊戲時間 UTC，非 6:00–17:59） | `~/model/gametime/L1GameTime.java:30-38,66-69` | `NPS.night:5-12`；`ClassicWorldNpcDamage.retainedNight:34-41` | 一致 | `undeadNightClockMatchesL1GameTime` |
| N-D6 | 玩家 AC 減傷 `calcPcDefense` | `L1AttackMode.java:250-269` | `NPS.defense:54-59`；P1 `CS:77369-77371` | 一致 | `playerAcDefenseMatchesCalcPcDefense` |
| N-D7 | 防具減免 `getDamageReductionByArmor`（> 10 時 `10 + nextInt(base−10) + 1`） | `L1AttackNpc.java:334`；`~/model/Instance/L1PcInstance.java:7784-7795` | `NPD.finish:31`（`BattlePhysicalDefense.armorReduction`）；P1 走 `combatPhysicalDamageAfterArmorAndCooking` | 推測一致（同 §3 R3 的函式，本路徑未逐值測） | — |
| N-D8 | 增幅防禦 `(max(Lv,50)−50)/5 + 1` | `L1AttackNpc.java:338-341` | `NPD.finish:31` | 一致（讀碼） | — |
| N-D9 | `getCombatDmgBonus`、聖結界 /2 | `L1AttackNpc.java:343-346`；`~/model/L1Character.java:1783-1792` | `NPD.finish:31-32`（料理／龍之皮膚／忍耐減免，`immuneToHarm` 減半） | 推測一致（buff 清單屬 T102） | — |
| N-D10 | 寵物、有主人的召喚物 × `PET_TO_PC_DAMAGE_MULTIPLIER` | `L1AttackNpc.java:348-356`；`~/../config/ConfigPcDamage.java:124`（程式預設 0.125），1-伺服器 實際設定檔 `config/其他控制端/5-職業傷害設定表.properties:90` = **0.3** | `NPD:25`、`CS:52556`（鍵名相同，預設 0.3） | 一致（與 1-伺服器 實際出貨設定相同；程式內預設值 0.125 不會生效） | `companionFallbackDamage…`（倍率用執行時值） |
| N-D11 | 假人倍率、紋章減免 `coatArms`、BOSS 減傷 `getBossRedux` | `L1AttackNpc.java:358-363,376-380` | 沒有 | 不適用（1-伺服器 自訂系統，同 §3 R6） | — |
| N-D12 | `dmg <= 0` → 未命中；回傳 `(int)` | `L1AttackNpc.java:365-382` | `Math.max(0, (int)…)`，`NPD.commit:10-15` 以 `value > 0` 判定命中 | 一致 | — |
| N-D13 | NPC 毒攻擊 15% | `L1AttackNpc.java:468-492` | `ClassicWorldMonsterPoisonAttack`、`CombatNpcPoison` | 不在本表（狀態異常屬 T102） | — |
| N-D14 | NPC → NPC：召喚 `nextInt(Lv) + STR/2 + STRD`；寵物 `nextInt(模板Lv) + STR/2 + 1 + Lv/14 + 武器`；`npcDmgMode`；`− calcNpcDamageReduction`；`/ IMMUNE_TO_HARM` | `L1AttackNpc.java:400-440`；`L1AttackMode.java:275-293` | P6 `CNA.damage:62-126` | 一致。**已修正**：STR 0..10 的 STRD 是負數，`CNA` 原本 `summonStrengthDamage < 0` 直接丟 `IllegalArgumentException`（寵物公式根本不用這個值也會丟）；`380_db` 有 162 隻 STR 1..10、102 隻 STR 0 的怪物，被迷魅或召喚後打怪就會例外。改成只拒絕 < −6（STRD 最小值） | `companionToNpcDamageMatchesCalcNpcDamageIncludingNegativeStrd` |
| N-D15 | NPC → NPC 的其他路徑 | 同 N-D14 | P3 `CS:77190-77216`、P4 `CS:77252-77268` 用 NPC → PC 的 `STR×0.8`；P7 打 NPC 用 `CS:52558-52568 companionNpcPhysicalDamage`（`STR×0.8`，且沒有 `(int)` 截斷、BOSS、不死夜間、武器破壞） | **不一致・待裁定**（見 §8.4 N3） | — |
| N-D16 | 競技場 NPC → 玩家（P8） | 同 N-D2–N-D6 | `BattleTournamentNpcPhysicalDamage.java:19-32`：STR 先 `max(1, …)` 再查 `角色_力量成長` | 一致（STR ≥ 1 時與 STRD 相同，見 N-D1）；未改 | — |

### 8.3 本節的 Java 變更（task/317）

1. `CombatNpcPhysicalSource.npcStrengthDamage`（新增）：NPC 的 STRD，夾到 0..254；WORLD 五個呼叫點改用它（N-D1）。
2. `ClassicWorldCompanionNpcPhysicalAttack.damage`：`summonStrengthDamage` 下限從 0 改 −6（N-D14）。
3. `ClientSession.companionPhysicalDamage`（7 參數）：改用 `CombatNpcPhysicalSource.base/mode`，補上暴擊後截斷與完整 STRD（N-D4）。既有 `BattleCompanionTest` 的 3 個期望值（4、5、7）計算後不變。
4. `ClientSession.companionHitChanceAgainst`／`companionPlayerHitChance`（新增）：寵物／召喚物打玩家時扣目標閃避，與怪物路徑同一套 LiTo 規則（N-H3）；四個呼叫點 `CS:51923`、`52317`、`79194`、`79390`，打 NPC／機器人時行為不變。

### 8.4 待使用者裁定（本節新增）

| 代號 | 差異 | 若要對齊 1-伺服器 要改哪裡 | 影響 |
|---|---|---|---|
| N1 | NPC 打玩家的閃避：LiTo 直接扣，1-伺服器 /8 | 同 §7 H7（`NPA.hitChance` 的 dodge 項） | 與 H7 一起決定 |
| N2 | 怪物打寵物／護送 NPC、競技場召喚物打 NPC 的命中用 Lv + 5（NPC → PC 包絡），1-伺服器 是 Lv + 3、BOSS +30 有效 | P3 `CS:77178`、P4 `CS:77246`、P7 三處改呼叫 `CNA.hit` | 寵物被怪打中的機率略降；BOSS 打寵物幾乎必中 |
| N3 | 同上路徑的傷害用 `STR×0.8 + STRD`，1-伺服器 是 `STR/2 + STRD`（寵物攻擊者另有公式），競技場召喚物打 NPC 還少了 `npcDmgMode` 的截斷／BOSS／不死／武器破壞 | P3、P4 改用 `CNA.damage` 的召喚公式；P7 打 NPC 改呼叫 `CNA.damage` | 怪物打寵物每下約少 0.3×STR（STR 20 少 6）；競技場召喚物（Lv 5、STR 15）打 NPC 從約 12 降到約 7，需重新平衡 |

判斷為「待裁定」而非直接改的原因：N2／N3 會明顯改變寵物存活與競技場召喚物輸出，而 LiTo 在這幾條路徑一致地把「受控 NPC」當成玩家目標處理（P3 甚至明確傳 dodge 0），看起來是刻意簡化；P6（寵物打怪）則已經是逐行移植，所以 1-伺服器 的 NPC → NPC 公式在 LiTo 裡現成可用（`CNA`），裁定後改呼叫點即可。
