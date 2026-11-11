# 共通魔法／法師 79 個技能：1-伺服器 ↔ LiTo 行為對照（T107，Issue #307）

- 參考（唯讀）：`D:\lineage開發\1-伺服器`。引用寫「相對路徑：行號」。縮寫：`U` = `src/com/lineage/server/model/skill/L1SkillUse.java`，`M` = `src/com/lineage/server/model/skill/L1SkillMode.java`，`sm/` = `src/com/lineage/server/model/skill/skillmode/`，`db` = `db/380_db.sql`。
- LiTo：路徑相對於 `lito伺服器/LiTo381-Server/src/main/java/local/lito381/server/`。`C` = `BattleSpellCatalog.java`（本分支行號），`CS` = `ClientSession.java`（方法宣告行）。
- 範圍：`db` `skills` 第 1～80 列去掉保留槽 24，共 79 個（`docs/reports/lito-capability-current.md` §4.2；`lito伺服器/LiTo381-Server/docs/maintenance/combat-coverage-current.md:11-90`）。
- 目標判定（target_to／area／ranged）已由 T106 完成，見 `docs/design/skill-targeting.md`，本文不重複。
- 測試：`lito伺服器/LiTo381-Server/src/test/java/local/lito381/server/WizardSkillServer1ParityTest.java`；輸入表 `src/test/resources/server1/skills-wizard-79.tsv`（由 `tools/skill-targeting/extract_wizard.py` 從 `db` 唯讀擷取 16 欄）。
- 判定：**一致**＝本文列出的面向逐值相等（有單元測試）；**已修正**＝本 PR 改 LiTo；**不一致・未改**＝有差異，本 PR 不改（理由寫在 §4）；效果本體的封包與實機行為一律是**待驗證**。
- 效果數值補驗（T116，Issue #336）：10／28 吸血、39 吸魔、58 火牢每跳、44 相消清單、29／76 緩速免疫、66 自施，見 §6；測試 `src/test/java/local/lito381/server/WizardSkillEffectServer1ParityTest.java`。

## 1. 共通流程（所有 79 個技能）

| # | 面向 | 1-伺服器 | LiTo | 判定 | 測試 |
|---|---|---|---|---|---|
| F1 | 數值來源 | `skills` 表（`db`） | 正式環境 `CombatDatabase.java:62-75` 讀同一張表，`:143-152` 覆蓋 catalog 的 mpCost／hpCost／reuseDelayMillis／durationSeconds／baseDamage／baseDiceCount／diceCount／dieSides／itemConsumeId／itemConsumeCount／probabilityDice／probabilityValue（`:12-15`）；catalog 常數只是沒載入 DB 時的後備值 | 一致：後備值與 `db` 逐欄相等（78 個有 catalog 紀錄；5 號走獨立處理器） | `consumptionCooldownAndDurationMatchSource`、`damageProbabilityAttributeAndLawfulMatchSource` |
| F2 | MP 消耗 | `U:1869-1971`：INT>12..18 依技能等級每級 −1（門檻 8／16／23／32／40／48／56）；頭盔減半（`:1919-1962`，技能 26／43／1／19／12／13／42／54）；SQL MP>0 時最低 1；最後減原始 INT 減免（不夾 0） | 所有施法處理器都經 `NativeSkillConsumptionBridges.java` 呼叫 `NativeSourceSkillConsumptionMath.mpCost`（`:8-67`）；catalog 各自的 `effectiveMpCost` 只用來驗參數，回傳值被丟棄 | 一致（INT 1..40 × 減免 0..2 × 頭盔有無，79 個逐值） | `runtimeMpCostMatchesIsHpMpConsume`、`minimumOneIsAppliedBeforeOriginalReduction` |
| F3 | 原始 INT 減免 | `src/com/lineage/server/model/Instance/L1PcOriginal.java:1012-1060` | `NativeSourceSkillConsumptionMath.java:69-88` | 一致（7 職業 × 原始 INT 0..30） | `originalIntelligenceReductionMatchesL1PcOriginal` |
| F4 | HP 消耗 | `U:1974-1979`（HP < 消耗+1 不能放）、`U:2041` 扣除 | 各處理器讀 `hpCost`（DB 覆蓋） | 數值一致（14、32、36、39、41、49、51、59 的 hpCost 與 `db` 相同）；「HP 剛好等於消耗+1」的邊界未逐處理器驗 | `consumptionCooldownAndDurationMatchSource` |
| F5 | 材料 | `U:1997-2015` 檢查、`U:2570-2641` 施放前先扣 | `CombatDatabase.consumeMaterial`（`:157-`） | 材料 ID／數量一致（40318 魔法寶石 × 1／2／3／10） | 同上 |
| F6 | 冷卻 | `skills.reuseDelay`；`U:2570` 起的動作鎖與冷卻並行（取較大者） | `spellReadyNanos.put(skillId, now + reuseDelayMillis)`（各處理器） | 數值一致；並行語意未驗 | 同上 |
| F7 | 期限 | `U:2060-2161 addMagicList`：`buffDuration×1000`；例外：11 交給 `L1DamagePoison`、33 交給 `L1CurseParalysis`、8／12／21／48 武器防具、50／80 凍結失敗不設、67 變形 | 各 `Battle*Status.activate*(now, durationSeconds)`，`now >= 到期` 即解除 | 一致（數值）；11、33 特例另測 | `consumptionCooldownAndDurationMatchSource`、`paralysisAndPoisonTimingMatchSourceExecutors`、`hasteExpiresExactlyAtSqlDuration` |
| F8 | 正義值 | `U:2047` 每次施放 `addLawful(skills.lawful)`（`U:1196-1201` 在 `runSkill` 之後無條件執行） | `ClientSession.applySpellLawfulDelta`（`CS:46732`），各處理器扣 MP 之後、送 `S_MANA_POINT` 之前呼叫；delta=0 直接返回不送封包 | **一致**：11、33、47（#307 修正）、51、56、59、80；10、20、27、28、40、41、66、77 由 #325 補上（§4.1） | `lawfulGapListIsExact` |
| F9 | 扣費時序 | `U:1123-1142`：`runSkill()` 之後一定呼叫 `applySideEffectsAfterNormalCast()` 扣 MP／正義值；`runSkill` 在 `:2779`（`isTargetCalc` 失敗）提早返回時仍扣費 | — | 回答 T106 的 G05 疑問：S1 對 76 自施「扣 MP、播動作、不生效」（§4.2） | — |
| F10 | BUFF 互斥（必定） | `U:2512-2517` `ALWAYS_REPEATED_SKILLS`：{52,101,150,勇水×3,186}、{43,54,STATUS_HASTE} | 速度單一欄位 `BattleTimedStatus.activateHaste/activateSlow`（`:97-144`）；勇敢單一欄位 `activateBrave`（`:195-213`） | 一致 | `hasteAndSlowCancelEachOtherLikeSource`、`holyWalkReplacesBravePotionWithBraveMode4` |
| F11 | BUFF 互斥（原廠覆蓋） | `U:2520-2527` `ORIGINAL_REPEATED_SKILLS`，只有 `SkillCoexistSet=0` 時生效；`db` `server_event` 116 `eventstart=1`（共存），所以預設不生效 | `CombatBuffLeases.java:11-13` `GROUPS`，`:59` 只在 `!skillCoexist()` 時清同組 | 一致（同組 ⇔ S1 同組）；159／168（妖精）在 LiTo 是獨立狀態，不在 `GROUPS`（交 T108） | `buffReplacementGroupsMatchOriginalRepeatedSkills` |
| F12 | BUFF 能力值 | `db` `skills_buff_ability`（3 AC−2；8 DMG1 HIT1；12 DMG2；21 AC−3；26 DEX5；42 STR5；47 DMG−5 HIT−1；48 BOW_HIT2 DMG2 HIT2；55 AC10 DMG5 HIT2；56 AC12 DMG−6） | 載入 DB 時 `SourceSkillControls.ability`（`CombatBuffLeases.java:17-20`、`CombatWeaponBuffs.java`）；後備值在 catalog | 一致（DB 路徑讀同一張表；後備值逐項相等，12 的弓傷害例外見 §4.3） | `buffAbilityFallbacksMatchSkillsBuffAbility` |

## 2. 速度與勇敢（F10 細節）

| 情況 | 1-伺服器 | LiTo `BattleTimedStatus` | 判定 |
|---|---|---|---|
| 正常時 43／54 | `sm/HASTE.java:31-41`、`U:3753-3761`：移動速度 1 | `activateHaste` → `HASTE`，記錄來源技能 | 一致 |
| 加速中再放 43／54 | `U:2864` 先刪同組（43／54／綠水），再設新的 | 同一欄位覆蓋，`hasteSkillId` 變成新技能 | 一致 |
| 緩速中放 43／54 | `sm/HASTE.java:43-60`、`U:3762-3776`：移除緩速與加速，速度 0 | 回傳 `NORMAL`，`stoppedSkillId`＝緩速技能，兩者皆清空 | 一致 |
| 加速中放 29／76 | `U:3072-3089`：移除加速與緩速，速度 0 | `activateSlow` 回傳 `NORMAL`，兩者皆清空 | 一致 |
| 正常時 29／76 | `U:3063-3070`：速度 2 | `SLOW` | 一致 |
| 緩速中再放 29／76 | `U:3062` 無 case 2，直接 `addMagicList` 刷新；兩個技能效果可並存 | 單一欄位刷新為新技能 | 數值一致；S1 允許 29 與 76 兩個效果同時存在、各自到期，LiTo 只記一個——**推測**影響極小（同為速度 2） |
| 目標裝備加速道具／強化勇水 | `U:3053-3061`：緩速完全不生效 | 29 `nativeMaterialEntangleBody`、76 WORLD `nativeAreaWorldSlowBody` 原本就擋；76 BATTLE `nativeAreaBattleSlowBody` 沒擋 | **已修正**（T116，§6.5） |
| 52 神聖疾走 | `U:3779-3786`：勇敢速度 4；與勇水同組互刪 | catalog `braveMode=4`（`C:1348`），`activateBrave` 覆蓋單一欄位 | 一致 |

## 3. 逐技能對照

「S1」欄寫效果類別（`M` 的註冊行＋`sm/` 檔）或 `U` 的分支行；「LiTo」欄寫 catalog 紀錄行與主要處理器。所有列的消耗／冷卻／期限／傷害骰／機率骰都已由 §1 的參數化測試逐值比對，下表「判定」只另外標出差異。

| ID | 名稱 | S1 效果類別／行號 | LiTo 實作位置 | 判定 |
|---|---|---|---|---|
| 1 | 初級治癒術 | TYPE_HEAL `U:2915-2941`、`U:3019-3031`；頭盔 20014 減半 `U:1932-1936` | `C:250` HealingSpell；`CS:47407` useHealingSpell、`CS:46965` | 一致（骰 (2+MB)d4、頭盔） |
| 2 | 日光術 | `U:3371`＋預設 `U:2139-2141` | `C:305` LightSpell；`CS:32102` | 一致（3600 秒） |
| 3 | 保護罩 | `U:3411-3420`，AC−2 來自 `skills_buff_ability` | `C:312` ShieldSpell；`CS:32118` | 一致 |
| 4 | 光箭 | TYPE_ATTACK `U:2868-2882` | `C:1592` AttackSpell；`CS:44785`、`CS:46262` | 一致 |
| 5 | 指定傳送 | `M:164` `sm/TELEPORT.java` | 無 catalog；`CS:25612-25688` 直接讀 `skills` 第 5 列 MP | MP 一致；傳送本體不在本文（無 catalog 紀錄，`NO_CATALOG_RECORD`） |
| 6 | 冰箭 | TYPE_ATTACK | `C:1596` | 一致 |
| 7 | 風刃 | TYPE_ATTACK | `C:1598` | 一致 |
| 8 | 神聖武器 | `U:3488`（玩家 BUFF）；DMG1 HIT1 | `C:1532` WeaponBuffSpell；`CS:36413` | 一致 |
| 9 | 解毒術 | `M:168` `sm/CURE_POISON.java` | `C:2691` CurseRemovalSpell；`CS:46532`、`CS:46520` | 一致 |
| 10 | 寒冷戰慄 | TYPE_ATTACK＋`U:3034-3035`（傷害轉為施法者回血） | `C:1606` AttackSpell | **已修正（#325）**：lawful −2 施放時套用（§4.1）；吸血量一致（§6.1） |
| 11 | 毒咒 | `U:3104-3105` `L1DamagePoison.doInfection(3000ms, 5)`；`L1DamagePoison.java:147` 30 秒 | `C:2650` PoisonSpell；`CS:65667`、`CS:65956` | 一致（3000ms／5／30 秒／lawful −10） |
| 12 | 擬似魔法武器 | `U:3471`；DMG2；力量頭盔 20015 減半 | `C:1490` ItemWeaponEnchantSpell；`CS:68650`、`CS:43462` | 一致（DB 路徑）；後備值多一個弓傷害 +2（§4.3） |
| 13 | 無所遁形術 | `U:2992-2999`、`U:3946` | `C:2369` DetectionSpell；`CS:43734` | 一致 |
| 14 | 負重強化 | 預設 `U:2141`；`src/com/lineage/server/model/Instance/L1PcInstance.java:6892` | `C:1579` WeightSpell；`CS:39333` | 一致（HP 50、1800 秒） |
| 15 | 火箭 | TYPE_ATTACK | `C:1620` | 一致 |
| 16 | 地獄之牙 | TYPE_ATTACK | `C:1622` | 一致 |
| 17 | 極光雷電 | TYPE_ATTACK（範圍 3，T106 G07） | `C:2178` LineAttackSpell；`CS:44389`、`CS:44865` | 一致 |
| 18 | 起死回生術 | `U:3162-3166`（不死系 1／3 → 傷害＝目前 HP） | `C:2190` TurnUndeadSpell；`CS:40790`、`CS:40794` | **已修正**：catalog `probabilityValue` 1 → 0（`db` 為 0；正式環境本來就讀 DB，後備值修正） |
| 19 | 中級治癒術 | TYPE_HEAL；頭盔 20014 | `C:256` | 一致 |
| 20 | 闇盲咒術 | `M:176` `sm/CURSE_BLIND.java` | `C:2211` BlindSpell；`CS:41416`、`CS:70975` | **已修正（#325）**：lawful −10 施放時套用（§4.1） |
| 21 | 鎧甲護持 | `U:3524`；AC−3 | `C:1520` ItemArmorEnchantSpell；`CS:43469` | 一致 |
| 22 | 寒冰氣息 | TYPE_ATTACK 範圍 | `C:2069` AreaAttackSpell；`CS:44734` | 一致 |
| 23 | 能量感測 | `U:3871-3888` | `C:2384` RevealWeaknessSpell；`CS:43550` | 一致 |
| 25 | 燃燒的火球 | TYPE_ATTACK 範圍 | `C:2075` | 一致 |
| 26 | 通暢氣脈術 | `U:3636`；DEX+5；敏捷頭盔 20013 減半 | `C:1247` AbilityBuffSpell；`CS:36166` | 一致 |
| 27 | 壞物術 | `U:3176-3195`（PC 武器耐久、NPC 疊層） | `C:2246` WeaponBreakSpell；`CS:44332`、`CS:64778` | **已修正（#325）**：lawful −10 施放時套用（§4.1） |
| 28 | 吸血鬼之吻 | TYPE_ATTACK＋`U:3034-3035` | `C:1615` | **已修正（#325）**：lawful −5 施放時套用（§4.1）；吸血量一致（§6.1） |
| 29 | 緩速術 | `U:3050-3091` | `C:2474` EntangleSpell；`CS:41432`、`CS:62757` | 一致（§2） |
| 30 | 巖牢 | TYPE_ATTACK 範圍 | `C:2086` | 一致 |
| 31 | 魔法屏障 | 預設 `U:2141`；抵銷 `U:4243-4256` | `C:339` CounterMagicSpell；`CS:32122`、`CS:57986` | 一致（執行期 MP 走 F2；catalog `effectiveMpCost` 少了 INT 減免，但沒被使用，§4.4） |
| 32 | 冥想術 | 預設 `U:2141` | `C:1559` RegenSpell；`CS:39394` | 一致（HP 30、640 秒） |
| 33 | 木乃伊的詛咒 | `M:174` `sm/CURSE_PARALYZE.java:38-44`（`ConfigSkill` CURSE_READY_TIME 5、CURSE_TIME 10；`config/4-職業_技能設定表.properties:29,32`） | `C:2666` ParalysisSpell；`CS:66284`、`CS:66571` | 一致（5000／10000 ms、lawful −500；SQL buffDuration 20 S1 不用） |
| 34 | 極道落雷 | TYPE_ATTACK | `C:1630` | 一致 |
| 35 | 高級治癒術 | TYPE_HEAL | `C:262` | 一致 |
| 36 | 迷魅術 | `U:3851-3860` | `C:2679` TamingSpell；`CS:46782`、`CS:64206` | 一致 |
| 37 | 聖潔之光 | `M:169` `sm/REMOVE_CURSE.java` | `C:2698` CurseRemovalSpell | 一致 |
| 38 | 冰錐 | TYPE_ATTACK | `C:1637` | 一致 |
| 39 | 魔力奪取 | `U:3168-3174`（5..14 + INT/2） | `C:2201` ManaDrainSpell；`CS:41408`、`CS:41058` | 一致（數值欄位、吸取量公式，§6.2） |
| 40 | 黑闇之影 | `M:177`（CURSE_BLIND） | `C:2221` BlindSpell | **已修正（#325）**：lawful −10 施放時套用（§4.1） |
| 41 | 造屍術 | `U:3861-3870` | `C:2411` CreateZombieSpell；`CS:46725`、`CS:64501` | **已修正（#325）**：lawful −10 施放時套用（§4.1） |
| 42 | 體魄強健術 | `U:3626`；STR+5；力量頭盔 20015 | `C:1251` AbilityBuffSpell | 一致 |
| 43 | 加速術 | `M:166` `sm/HASTE.java`；頭盔 20013／20008 | `C:1295` HasteSpell；`CS:36788` | 一致（§2） |
| 44 | 魔法相消術 | `M:167` `sm/CANCELLATION.java` | `C:1549` CancellationSpell；`CS:38644` | 一致（數值欄位；免疫清單來源同表，法師技能存活／清除一致，§6.4）；「裝備加速道具的速度被相消」**待使用者裁定**（§6.4） |
| 45 | 地裂術 | TYPE_ATTACK | `C:1644` | 一致 |
| 46 | 烈炎術 | TYPE_ATTACK | `C:1651` | 一致 |
| 47 | 弱化術 | `U:3121-3125`；DMG−5 HIT−1 | `C:2264` WeaknessSpell；`CS:44344`、`CS:68972`（`applySpellLawfulDelta` 在 `CS:69291`） | **已修正**：`lawfulDelta` 0 → −10（`db` lawful −10，`U:2047`） |
| 48 | 祝福魔法武器 | `U:3506` | `C:1540` WeaponBuffSpell | 一致 |
| 49 | 體力回復術 | TYPE_HEAL | `C:281` GroupHealingSpell；`CS:46961` | 一致 |
| 50 | 冰矛圍籬 | `U:3133-3138`＋`U:2101-2107` | `C:2019` FreezeAttackSpell；`CS:44754` | 一致 |
| 51 | 召喚術 | `M:170` `sm/SUMMON_MONSTER.java` | `C:2558` SummonSpell；`CS:46548`、`CS:62463` | 一致（lawful −10） |
| 52 | 神聖疾走 | `U:3779-3786` | `C:1348` MovementBuffSpell；`CS:37511` | 一致（§2） |
| 53 | 龍捲風 | TYPE_ATTACK 範圍 | `C:2095` | 一致 |
| 54 | 強力加速術 | `U:3753-3777`；風之頭盔 20023 | `C:1304` SelfHasteSpell；`CS:37163` | 一致 |
| 55 | 狂暴術 | `U:3721-3731`；AC+10 DMG+5 HIT+2 | `C:1313` CombatBuffSpell；`CS:37221` | 一致 |
| 56 | 疾病術 | `U:3127-3131`；AC+12 DMG−6 | `C:2276` WeaknessSpell | 一致 |
| 57 | 全部治癒術 | TYPE_HEAL | `C:269` | 一致 |
| 58 | 火牢 | `U:2759-2762` `doSpawnFireWall` | `C:1585` GroundSpell；`CS:46900`、`CS:69728` | **已修正**（T116）：每跳擲骰改用有效 INT（§6.3）；減傷算式、SKD3 修正一致 |
| 59 | 冰雪暴 | TYPE_ATTACK 範圍 | `C:2104` | 一致（lawful −10） |
| 60 | 隱身術 | `U:3967-3990`、`U:2064-2066` | `C:2706` InvisibilitySpell；`CS:40645` | 一致 |
| 61 | 返生術 | `M:171` `sm/RESURRECTION.java` | `C:2423` ResurrectionSpell；`CS:46757` | 一致 |
| 62 | 震裂術 | TYPE_ATTACK 範圍 | `C:2113` | 一致 |
| 63 | 治癒能量風暴 | `U:2729-2733` spawnEffect 81169 | `C:1587` GroundSpell | 一致（數值欄位） |
| 64 | 魔法封印 | 預設 `U:2141`；`src/com/lineage/server/clientpackets/C_UseSkill.java:142` | `C:2289` SilenceSpell；`CS:41424` | 一致 |
| 65 | 雷霆風暴 | TYPE_ATTACK 範圍 | `C:2132` | 一致 |
| 66 | 沉睡之霧 | `U:3197-3202`、`U:2905-2909` | `C:2613` SleepSpell；`CS:44385` | **已修正（#325）**：lawful −50 施放時套用（§4.1）；自施主目標 LiTo 不扣 MP、1-伺服器 扣 MP（**待使用者裁定**，§6.6） |
| 67 | 變形術 | `U:3733-3751` | `C:2626` PolymorphSpell；`CS:32098` | 一致 |
| 68 | 聖結界 | 預設 `U:2141`；`src/com/lineage/server/model/L1AttackPc.java:1183` 等 | `C:1323` DamageProtectionSpell；`CS:37391` | 一致 |
| 69 | 集體傳送術 | `M:165` `sm/MASS_TELEPORT.java` | `C:1334` MassTeleportSpell；`CS:37395` | 一致 |
| 70 | 火風暴 | TYPE_ATTACK 範圍 | `C:2123` | 一致 |
| 71 | 藥水霜化術 | `M:178` `sm/DECAY_POTION.java` | `C:2305` DecayPotionSpell；`CS:43458` | 一致 |
| 72 | 強力無所遁形術 | `U:3001-3016` | `C:2355` CounterDetectionSpell；`CS:44028` | 一致 |
| 73 | 創造魔法武器 | `U:3225-` | `C:2399` MagicalWeaponSpell；`CS:43546` | 一致（10 個魔法寶石） |
| 74 | 流星雨 | TYPE_ATTACK 範圍 | `C:2141` | 一致 |
| 75 | 終極返生術 | `M:172` `sm/GREATER_RESURRECTION.java` | `C:2433` ResurrectionSpell | 一致 |
| 76 | 集體緩速術 | `U:3050-3091`；自施 `U:2779` 不生效但仍扣費（F9） | `C:2460` MassSlowSpell；`CS:44377` → `NativeSourceMassSlowTargets` | 自施：T114（#321）已對齊（扣費、不生效，§4.2）；BATTLE 緩速免疫 **已修正**（T116，§6.5） |
| 77 | 究極光裂術 | TYPE_ATTACK；`U:565` 正義值 ≥500；`U:2876` 爆擊 | `C:1660` AttackSpell（`minimumLawful=500`） | **已修正（#325）**：lawful +1 施放時套用（§4.1） |
| 78 | 絕對屏障 | `U:3360-3368` | `C:349` AbsoluteBarrierSpell；`CS:32126` | 一致 |
| 79 | 靈魂昇華 | `M:173` `sm/ADVANCE_SPIRIT.java` | `C:361` AdvanceSpiritSpell；`CS:32133` | 一致 |
| 80 | 冰雪颶風 | `U:3133-3138` | `C:2030` FreezeAttackSpell | 一致（lawful −10） |

統計（T107＋#325）：一致 68、已修正 10（18、47；#325 補正義值 10、20、27、28、40、41、66、77）、不一致・未改 1（76）。「一致」只代表本文列出的面向；效果本體（封包、傷害實際入帳、狀態圖示）見 §5。
T116 之後：76 自施已由 T114（#321）對齊；58、76（BATTLE 緩速免疫）在 T116 修正；66 自施、44 加速道具速度列待使用者裁定（§6）。

## 4. 不一致與未改的理由

### 4.1 正義值（10、20、27、28、40、41、66、77）——#325 已補上
S1 `U:2047` 每次 TYPE_NORMAL 施放都加 `skills.lawful`（`U:1196-1201` 在 `runSkill` 之後無條件執行，命中與否都改）。#307 時這 8 個技能的 record 沒有 `lawfulDelta`。#325：`AttackSpell`、`BlindSpell`、`WeaponBreakSpell`、`CreateZombieSpell`、`SleepSpell` 加 `lawfulDelta` 欄位（10＝−2、28＝−5、77＝+1、20／40＝−10、27＝−10、41＝−10、66＝−50；其他攻擊魔法 0），並在 11 個處理器扣 MP 之後呼叫 `applySpellLawfulDelta`：攻擊魔法戰鬥／世界（`nativeSourceControlledOriginal_useAttackSpell`、`nativeCompanionTargetuseClassicWorldAttackSpell`）、盲咒戰鬥／世界／寵物目標（`nativeMaterialBlindBody`、`nativeMaterialWorldBlindBody`、`NativeBlindSceneAction.companion`）、壞物術戰鬥／世界／寵物目標（`nativeMaterialWeaponBreakBody`、`nativeMaterialWorldWeaponBreakBody`、`NativeWeaponBreakBattleCompanion.cast`）、沉睡之霧戰鬥／世界（`nativeAreaBattleSleepBody`、`nativeAreaWorldSleepBody`）、造屍術（`nativeMaterialZombieBody`）。正義值只改 `GameStateStore`（LiTo 自己的狀態檔）與記憶體，不寫回來源角色庫；變動時送 `S_CHANGE_ALIGNMENT`（0x59 `cdh`，`docs/reference/client-protocol-needs.md` §1.2）。`lawfulGapListIsExact` 的清單改為空集合，`issue325EveryCastHandlerAppliesLawful` 釘住呼叫位置。

### 4.2 76 集體緩速術自施
S1：`U:2779` 對自己（或自己的寵物）為主目標時整個 `runSkill` 返回，但 `U:1140` 仍扣 MP、正義值並播動作 → 「扣費、不生效」。LiTo：`NativeSourceMassSelfTarget`（T106 G05）保留「自施仍作用於範圍內其他人」。T106 已列為使用者決定項；本 PR 補上 S1 的扣費時序，未改程式。（後續：T114／#321 已改為對齊 1-伺服器「扣費、不生效」；66 的同類問題見 §6.6。）

### 4.3 12 擬似魔法武器的後備值
`db` `skills_buff_ability` 只有 `(12,'DMG',2)`；S1 `L1PcBuffAbilityCalc.java:294-295` 把 DMG 與 BOW_DMG 分開。LiTo 正式環境（DB 載入）走 `CombatBuffLeases` 讀同一張表，結果相同；只有沒載入 DB 時 catalog 後備值 `rangedDamage=2` 會讓弓也 +2。只影響離線測試，未改。

### 4.4 catalog `effectiveMpCost` 與執行期不同（不影響行為）
執行期 MP 一律由 `NativeSourceSkillConsumptionMath.mpCost` 計算（F2）。catalog 中下列 record 自己的 `effectiveMpCost` 與 S1 不同，但回傳值在 `NativeSkillConsumptionBridges` 被丟棄：31（沒有 INT 減免）、51／61／75（少了 >48、>56 兩級）、56（少了 >48）、60（少了 >40..>56）、77（多了 >64、>72 兩級）、3（不減原始 INT）。建議之後刪掉這些重複公式，避免誤用。

## 5. 未驗證（交給實機／後續任務）

- 各處理器的封包順序、狀態圖示、實際 HP／MP 入帳（需要實機或整合測試）。
- ~~10／28 吸血、39 吸魔量、58 火牢每跳傷害、44 相消清單、29／76 對「加速道具」「強化勇水」目標免疫~~：T116 已比對，見 §6；實機入帳仍待驗證（§6.7）。
- 機率類（20、29、33、36、40、44、47、56、64、66、71、76）的機率公式屬 T102（`docs/design/formulas-magic.md`），本文只比對 `probability_dice`／`probability_value` 欄位。

## 6. 效果數值補驗（T116，Issue #336）

另加縮寫：`EFT` = `src/com/lineage/server/timecontroller/skill/EffectFirewallTimer.java`，`AL` = `src/com/lineage/server/model/L1AttackList.java`，`MHC` = `src/com/lineage/server/model/combat/MagicHitCommitPlan.java`，`SU` = `src/com/lineage/server/utils/L1SpawnUtil.java`。LiTo 行號為本分支修改後。測試名稱都在 `WizardSkillEffectServer1ParityTest`。

| # | 項目 | 1-伺服器 | LiTo | 判定 | 測試 |
|---|---|---|---|---|---|
| 6.1 | 10／28 吸血 | `U:3034-3035` `heal = this._dmg`（整筆計算傷害）；傷害被接受後 `U:781-782`、`U:898-899` `adjustCurrentHpIfAlive(heal)` | `C:8403` `drainsLife()` 只認 10、28；`CS:46444` `applyAcceptedLifeDrain`：施法者死亡不回、`min(maxHp, hp+damage)`；BATTLE 玩家 `CS:89837` 只在目標 HP 下降時回；NPC 走 `CS:73948`、`NativeSourceSingleNpc.java:54` | 一致 | `lifeDrainSkillsAreExactlyChillTouchAndVampiricTouch`、`lifeDrainHealsTheFullCalculatedDamageCappedAtMaximumHp` |
| 6.2 | 39 吸魔量 | `U:3168-3174`：`nextInt(10)+5 + getInt()/2`，夾到目標目前 MP；`MHC:41-56` 施法者只加實際吸到的量 | `BattleManaDrain.java:27` `d10+4+INT/2`（INT＝有效 INT，與 MP 消耗同一值）；`CS:92633` 夾目標 MP、`CS:92674` 施法者加實際量 | 一致（INT 0..127 × 10 種骰 × 5 種目標 MP） | `manaDrainAmountMatchesL1SkillUseForEveryRollAndIntelligence`、`manaDrainTransfersOnlyTheClampedAmount` |
| 6.3 | 58 火牢每跳 | `StartTimer_Skill.java:70` 每 1000 ms；`EFT:244`（PC）／`:197`（NPC）`19 + nextInt(max(getInt()/2,1))`；火屬性 >0 才減 `floor(0.32×fire)/32`（`EFT:293-304`）；PC 再套 `AL:256-286` SKD3（料理 −5／−2、181 −10、211 −15、68 最後減半）；`AL:289-294` SKM0（凍結、50、80、194、78、157）傷害 0；同血盟、安全區不傷（`EFT:223-231`）；`SU:883-945` 8 格、最多 24 格、期限 `buffDuration` | 減傷：`BattleElementalResistance.java:13`、Source PC 路徑 `NativeSourceGroundPlayer.java:11`（SKD3 `:6`，免疫 `:17`）；擲骰：Source 路徑 `NativeSourceGroundPlayer.java:39` 用有效 INT；**舊路徑** `CS:56308` `fireWallDamage()`（BATTLE 非 Source、WORLD 玩家 `CS:55483`、WORLD 怪物 `CS:55537`）原本用資料庫基礎 INT | **已修正**：`fireWallDamage()` 改用有效 INT（`nativeSkillConsumptionIntelligence`，`CS:73201`；裝備快照未就緒時退回基礎 INT）；減傷與 SKD3 一致 | `fireWallResistanceArithmeticMatchesEffectFirewallTimer`、`fireWallPlayerModifiersMatchSkd3`、`fireWallTicksUseEffectiveIntelligence` |
| 6.4 | 44 相消清單 | `CANCELLATION.java:51-77`：SKILLS／STATUS／COOKING 全清，跳過 `L1SkillMode.isNotCancelable`（`L1SkillMode.java:144-155`：禁言等 5 個寫死＋`skills_相消免疫技能`，`SkillNotCancelableTable.java:51`）；`db:48847-48858` 12 筆：12、21、78、79、87、88、90、91、107、185、190、195；PC 有加速道具 → 速度歸 0（`CANCELLATION.java:83-86`） | 正式環境讀同一張表（`SourceSkillControls.java:82`，同樣 INNER JOIN `skills`）；BATTLE `CS:66972` `nativeMaterialCancellationBody`：加速／緩速／47／56／71／173／174 依表保留（`NativeCancellationSpeedImmunity.java:10`），武器 BUFF 8／48、能力 BUFF（3、26、42…）清除；12／107（道具武器附魔）、21（道具防具附魔）、78、79 是獨立狀態不在清除清單 | 法師技能一致（存活：12、21、78、79；清除：其餘）；12／21／78／79 在 LiTo 是**結構上**不清，不會跟著 DB 清單變動（推測影響：只有改 DB 清單時才會不同）；**加速道具速度歸 0：LiTo 沒有做（待使用者裁定）** | `cancellationImmuneListComesFromTheSameTable`、`wizardCancellationSurvivorsMatchTheSourceTable` |
| 6.5 | 29／76 緩速免疫 | `U:3053-3061`：目標 `getHasteItemEquipped()>0` 或 `braveSpeed==5`（強化勇水）→ `continue`（不緩速、不刪加速）；順序在成功判定、反制魔法之後 | 29：`CS:62839` `nativeMaterialEntangleBody`（`CS:63186` 擋）；76 WORLD：`CS:60615`（`CS:60886` 擋）；76 BATTLE：`CS:61095` `nativeAreaBattleSlowBody` **原本沒擋** | **已修正**：BATTLE 76 在反制魔法之後、`activateSlow` 之前加 `hasEquippedHasteArmor() \|\| brave.mode()==5`（`CS:61314`），算失敗、不改速度 | `slowSpellsSkipHasteItemAndStrengthenedBraveTargets`、`slowSkillsRouteThroughTheGatedBodies` |
| 6.6 | 66 沉睡之霧自施 | `U:4079-4083` 主目標是自己 → `isTargetCalc` 失敗 → `U:2779` 返回：**扣 MP、播動作、無人生效**；逐目標時自己也排除（`U:2833`） | BATTLE `CS:60066`：`CS:60085` `var13.player() != this` 不通過 → 扣費前拒絕；WORLD `CS:59650`：主目標不在候選（自己被排除）→ `CS:59698` 扣費前拒絕；逐目標兩邊都排除自己 | **不一致・待使用者裁定**：無人生效相同；差在 1-伺服器 扣 MP／冷卻／播動作，LiTo 不扣 | `fogOfSleepingSelfPrimaryIsRejectedBeforeCostAndSelfIsNeverATarget`（鎖住 LiTo 現況） |

### 6.6 為什麼 66 自施不直接改
T114（#321 S2）對 76 採「對齊 1-伺服器（扣費、不生效）」；但同一 Issue 的 S3（103 射程外）使用者原意是「扣了 MP 卻沒效果屬於缺陷」，最後維持 LiTo 不扣。66 自施兩種方向都有前例，且改成扣費要把扣費搬到目標判定前（BATTLE／WORLD 兩個反編譯大方法），屬於刻意的設計選擇，所以列給使用者決定。建議：比照 76（T114）對齊 1-伺服器，以保持兩個範圍緩控技能一致；若選維持 LiTo，則不用改。

### 6.4 補充：相消對「裝備加速道具」
1-伺服器 相消會把裝備加速道具玩家的移動速度歸 0（送 `S_SkillHaste(0)`），要重新穿脫才恢復；LiTo 的加速道具速度不是存在 `BattleTimedStatus` 的租約，相消後不變。這會影響 PvP 平衡（相消是常用的拆加速手段），而且 LiTo 沒有對應的「道具速度暫停」狀態，需要新設計，列「待使用者裁定」。

### 6.7 推測與待驗證
- 推測：火牢舊路徑（BATTLE 非 Source、WORLD 玩家）只套料理減傷、凍結、大地屏障；1-伺服器 SKD3 的 181／211／68 與 SKM0 的 78 在這兩條路徑可能由後續的傷害提交（`NativePlayerCommit.worldPlayer` 等）處理，本次未逐行追到，未改。
- 推測：1-伺服器 `getHasteItemEquipped` 也算武器；LiTo `RuntimeInventory.java:1256` 只看防具（`sourceArmorStatus().haste()`）。3.81 的加速武器極少，未改。
- 推測：NPC 目標的 `braveSpeed==5` 在 LiTo 不存在（NPC 沒有強化勇水），所以 NPC 路徑不需要這個判斷。
- 待驗證（實機）：10／28 回血封包與數值、39 雙方 MP 封包、58 每秒一跳且數值隨裝備 INT 變化、76 BATTLE 打穿戴加速防具或強化勇水的玩家不會被緩速、44 相消後 12／21／78／79 仍在。
