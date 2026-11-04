# 伺服器語意缺口（T020d / Issue #44）

> 快照來源：GitHub Actions `build` run [41843825080](https://github.com/chaoren0418-arch/lito-launcher-server/actions/runs/41843825080) job `Semantic gap (non-blocking)`（Temurin 17）。 artifact [`server-semantic-gap`](https://github.com/chaoren0418-arch/lito-launcher-server/actions/runs/41843825080/artifacts/13329967838)。

以 ASM tree 正規化後比對兩個 JAR 的 `.class`。
此檔由 `lito伺服器/LiTo381-Server/tools/semantic-diff` 產生。

## 比對基準

| 項目 | 值 |
|---|---|
| left | `lito伺服器/LiTo381-Server/target/LiTo381-Server.jar`（3929 類） |
| right | `lito伺服器/LiTo381-Server/reference/base-0.6.71.jar`（2554 類） |
| 產生時間 | 2026-11-04T14:05:12Z |
| 正規化 | 移除 LineNumberTable／LocalVariableTable／LocalVariableTypeTable／SourceFile／SourceDebugExtension；不比較 max_stack／max_locals／常數池順序；`lambda$xxx$N`、`access$NNN` 依出現順序正規化 |
| 比較項 | 類別存取旗標、父類別、介面、欄位（名稱／描述／旗標／常數值）、方法（名稱／描述／旗標／例外／指令序列含解析後運算元、try-catch、標籤序號）、註解、內部類別屬性 |

## 摘要

| 狀態 | 數量 | 含義 |
|---|---:|---|
| SAME | 2064 | 兩邊都有，正規化後語意相同 |
| DIFF | 485 | 兩邊都有，正規化後仍不同 |
| ONLY_LEFT | 1380 | 只在 left |
| ONLY_RIGHT | 5 | 只在 right |
| 比較類別 | 3934 | left∪right |

## 已驗證／推測／待驗證

- **已驗證**：同一 JAR 對自己 DIFF=0；同一原始碼 `-g` 與 `-g:none` DIFF=0；故意改常數會標 DIFF。本表由本工具對上述 left／right 跑出。
- **推測**：DIFF 是 javac／反編譯來源／後來手改造成的真實指令或結構差，不是 debug 屬性或常數池編號。ONLY_* 與 `docs/server-source-gap.md` 的類別清單差對得上（Modern*、inner／匿名形狀、缺頂層類）。
- **待驗證**：語意相同 ≠ 執行行為已對過回歸；未跑 `1.啟動伺服器.bat --check`／`--smoke`，未連正式 DB，未啟動遊戲。

## 與 Issue #43 javap 粗估（約 430）的關係

T007／#2 的 `jar tf` 交集是 **2,538**。T020a／#43 記：Maven 產物與 `reference/base-0.6.71.jar` **class bytes 完全相同 1,008**；對位元組不同的交集類別跑 `javap -c -p` 去掉行號、常數池編號與空白後，**仍約 430 類不同**（當時當上限估計，未扣 lambda 編號等編譯器雜訊）。

本工具比 javap 粗估多做了：整段丟掉 debug 屬性、標籤改序號、`lambda$`／`access$` 依出現順序改名、略過 max_stack／max_locals；並且比較 innerClasses／nestMembers／欄位常數／註解，不只 `javap -c` 反組譯文字。本輪計數：

- 交集 SAME+DIFF = **2549**（應對齊 2,538）。
- **SAME=2064**，比 bytes 全同的 1,008 多 **1056**——這些是位元組不同但正規化後語意相同（debug／合成名／常數池編號）。
- **DIFF=485**（第一處差異：method 343、field 27、class 屬性 115）。較 javap 粗估 430 多 55：javap 文字正規化會漏掉 innerClasses／nestMembers／access／欄位常數等；本工具把它們算進 DIFF。
- ONLY_* 不在 430 裡：430 只估計**兩邊都有**的類別。ONLY_LEFT／ONLY_RIGHT 應對齊 `docs/server-source-gap.md`。

## DIFF 類別清單

| 類別 | 第一處差異 |
|---|---|
| `local/lito381/server/AccountRepository` | method openLegacy(Llocal/lito381/server/ServerConfig;)Llocal/lito381/server/AccountRepository; insn[1]: left=INVOKEVIRTUAL local/lito381/server/ServerConfig.legacySourceDatabases()Ljava/util/List; right=INVOKESTATIC local/lito381/server/SharedDatabase.enabled(Llocal/lito381/server/ServerConfig;)Z |
| `local/lito381/server/BattleAbilityStatus` | field sourceBuffLeases:Llocal/lito381/server/CombatBuffLeases;: only right |
| `local/lito381/server/BattleAbilityStatus$IllusionAvatar` | method applyIncomingDamage(I)I insn[12]: left=DMUL right=ALOAD 0 |
| `local/lito381/server/BattleAbilityStatus$Protection` | method applyTo(I)I insn[17]: left=IDIV right=POP |
| `local/lito381/server/BattleAnimationCatalog$Animation` | method durationMillis()J insn[8]: left=LRETURN right=LSTORE 1 |
| `local/lito381/server/BattleAnimationCatalog$Holder` | method <clinit>()V insn[1]: left=INVOKESTATIC local/lito381/server/BattleAnimationCatalog.load(Ljava/lang/String;)Llocal/lito381/server/BattleAnimationCatalog; right=INVOKESTATIC local/lito381/server/SharedClientResources.animationResource(Ljava/lang/String;)Ljava/lang/String; |
| `local/lito381/server/BattleArmBreakerStatus` | method activateIfAbsent(JI)Llocal/lito381/server/BattleArmBreakerStatus$Activation; insn[5]: left=BIPUSH 12 right=INVOKESTATIC local/lito381/server/CombatBuffLeases.validArmDuration(I)Z |
| `local/lito381/server/BattleArmorBreak` | method supportsWeaponType(I)Z insn[1]: left=BIPUSH 20 right=INVOKESTATIC local/lito381/server/CombatRules.weaponActionType(I)I |
| `local/lito381/server/BattleBurningWeaponStatus` | method snapshot(J)Llocal/lito381/server/BattleBurningWeaponStatus$Snapshot; insn[51]: left=ALOAD 0 right=LDC Integer:163 |
| `local/lito381/server/BattleClearMindStatus` | method snapshot(J)Llocal/lito381/server/BattleClearMindStatus$Snapshot; insn[54]: left=ILOAD 7 right=LDC Integer:137 |
| `local/lito381/server/BattleCompanion` | method activate(IIII)V insn[29]: left=PUTFIELD local/lito381/server/BattleCompanion.active:Z right=ALOAD 0 |
| `local/lito381/server/BattleCubeHarmonyEffects` | method nativeMaterialPlace(JIIJLlocal/lito381/server/ClientSession;)Llocal/lito381/server/BattleCubeHarmonyEffects$Placement;: only right |
| `local/lito381/server/BattleCubeIgnitionEffects` | method nativeMaterialPlace(JIIJLlocal/lito381/server/ClientSession;)Llocal/lito381/server/BattleCubeIgnitionEffects$Placement;: only right |
| `local/lito381/server/BattleCubeQuakeEffects` | method nativeMaterialPlace(JIIJLlocal/lito381/server/ClientSession;)Llocal/lito381/server/BattleCubeQuakeEffects$Placement;: only right |
| `local/lito381/server/BattleCubeShockEffects` | method nativeMaterialPlace(JIIJLlocal/lito381/server/ClientSession;)Llocal/lito381/server/BattleCubeShockEffects$Placement;: only right |
| `local/lito381/server/BattleDragonEyeCatalog$Eye` | method buffId()I insn[2]: left=IRETURN right=ALOAD 0 |
| `local/lito381/server/BattleEarthBlessStatus` | method snapshot(J)Llocal/lito381/server/BattleEarthBlessStatus$Snapshot; insn[51]: left=ILOAD 7 right=LDC Integer:159 |
| `local/lito381/server/BattleFireBlessStatus` | method snapshot(J)Llocal/lito381/server/BattleFireBlessStatus$Snapshot; insn[51]: left=ILOAD 7 right=LDC Integer:155 |
| `local/lito381/server/BattleFireWeaponStatus` | method snapshot(J)Llocal/lito381/server/BattleFireWeaponStatus$Snapshot; insn[51]: left=ILOAD 7 right=LDC Integer:148 |
| `local/lito381/server/BattleFreezeEffects` | method place(JIIJI)Llocal/lito381/server/BattleFreezeEffects$Placement; insn[0]: left=LLOAD 1 right=ILOAD 7 |
| `local/lito381/server/BattleGuardBrakeStatus` | method snapshot(J)Llocal/lito381/server/BattleGuardBrakeStatus$Snapshot; insn[51]: left=ILOAD 7 right=LDC Integer:183 |
| `local/lito381/server/BattleInvisibilityStatus` | class nestMembers: left= right=local/lito381/server/BattleInvisibilityStatus$Lease,local/lito381/server/BattleInvisibilityStatus$Snapshot |
| `local/lito381/server/BattleIronSkinStatus` | method snapshot(J)Llocal/lito381/server/BattleIronSkinStatus$Snapshot; insn[51]: left=ILOAD 7 right=LDC Integer:168 |
| `local/lito381/server/BattleJoyOfPain` | method feedbackDamage(III)I insn[22]: left=IDIV right=LDC str:"JOY_OF_PAIN_COUNTDMG" |
| `local/lito381/server/BattleMortalBody$Decision` | method <init>(ZIIZIII)V insn[40]: left=IFLE L2 right=IFLT L2 |
| `local/lito381/server/BattlePatienceStatus` | method physicalReduction(JJ)I insn[6]: left=GOTO L1 right=INVOKESTATIC local/lito381/server/CombatSourceReductions.patience(I)I |
| `local/lito381/server/BattlePetRaceCourseCatalog$Runner` | method <init>()V: only right |
| `local/lito381/server/BattlePetRaceCourseCatalog$State` | method begin(ILjava/util/List;JJJ)V insn[55]: left=LLOAD 3 right=INVOKESPECIAL local/lito381/server/BattlePetRaceCourseCatalog$Runner.<init>()V |
| `local/lito381/server/BattlePhysicalDefense` | method againstPlayer(IIIIIIII)Llocal/lito381/server/BattlePhysicalDefense$PlayerOutcome; insn[70]: left=NEW local/lito381/server/BattlePhysicalDefense$PlayerOutcome right=ILOAD 14 |
| `local/lito381/server/BattlePhysicalProcBuff` | method burningSpirit(ZZI)Llocal/lito381/server/BattlePhysicalProcBuff$Outcome; insn[16]: left=INVOKESTATIC local/lito381/server/BattlePhysicalProcBuff$Outcome.inactive(ZZI)Llocal/lito381/server/BattlePhysicalProcBuff$Outcome; right=LDC str:"BURNING_CHANCE" |
| `local/lito381/server/BattlePolymorphCatalog` | method nativePackagedRandomForm(I)Llocal/lito381/server/BattlePolymorphCatalog$Form;: only right |
| `local/lito381/server/BattleQuestEncounter` | field sourceProcEncounterGeneration:Ljava/lang/Object;: only right |
| `local/lito381/server/BattleQuestEncounter$Monster` | method damage(IZ)Llocal/lito381/server/BattleQuestEncounter$Damage; insn[6]: left=IFEQ L1 right=ALOAD 0 |
| `local/lito381/server/BattleRules` | method decode(Llocal/lito381/server/BattleRules$Mode;ILlocal/lito381/server/BattleControlCatalog;)Llocal/lito381/server/BattleRules$RuleSet;: only left |
| `local/lito381/server/BattleShockStunEffects` | method place(JIIJI)Llocal/lito381/server/BattleShockStunEffects$Placement; insn[21]: left=ICONST_4 right=LDC Integer:65535 |
| `local/lito381/server/BattleSpellCatalog` | method zombieTemplate(II)Llocal/lito381/server/BattleSpellCatalog$ZombieTemplate; insn[3]: left=ICONST_3 right=ICONST_1 |
| `local/lito381/server/BattleSpellCatalog$AbilityBuffSpell` | method actionId()I insn[2]: left=IRETURN right=ALOAD 0 |
| `local/lito381/server/BattleSpellCatalog$AbsoluteBarrierSpell` | method actionId()I insn[2]: left=IRETURN right=ALOAD 0 |
| `local/lito381/server/BattleSpellCatalog$AdditionalFireSpell` | method actionId()I insn[2]: left=IRETURN right=ALOAD 0 |
| `local/lito381/server/BattleSpellCatalog$AdvanceSpiritSpell` | method actionId()I insn[2]: left=IRETURN right=ALOAD 0 |
| `local/lito381/server/BattleSpellCatalog$AquaProtectSpell` | method actionId()I insn[2]: left=IRETURN right=ALOAD 0 |
| `local/lito381/server/BattleSpellCatalog$AreaAttackSpell` | method actionId()I insn[2]: left=IRETURN right=ALOAD 0 |
| `local/lito381/server/BattleSpellCatalog$AreaSilenceSpell` | method actionId()I insn[2]: left=IRETURN right=ALOAD 0 |
| `local/lito381/server/BattleSpellCatalog$ArmBreakerSpell` | method durationSeconds()I insn[2]: left=IRETURN right=ALOAD 0 |
| `local/lito381/server/BattleSpellCatalog$ArmorBreakSpell` | method actionId()I insn[2]: left=IRETURN right=ALOAD 0 |
| `local/lito381/server/BattleSpellCatalog$AttackSpell` | method actionId()I insn[2]: left=IRETURN right=ALOAD 0 |
| `local/lito381/server/BattleSpellCatalog$AwakenSpell` | method actionId()I insn[2]: left=IRETURN right=ALOAD 0 |
| `local/lito381/server/BattleSpellCatalog$BindSpell` | method actionId()I insn[2]: left=IRETURN right=ALOAD 0 |
| `local/lito381/server/BattleSpellCatalog$BlindSpell` | method actionId()I insn[2]: left=IRETURN right=ALOAD 0 |
| `local/lito381/server/BattleSpellCatalog$BloodlustSpell` | method actionId()I insn[2]: left=IRETURN right=ALOAD 0 |
| `local/lito381/server/BattleSpellCatalog$BodyToMindSpell` | method actionId()I insn[2]: left=IRETURN right=ALOAD 0 |
| `local/lito381/server/BattleSpellCatalog$BoneBreakSpell` | method durationSeconds()I insn[2]: left=IRETURN right=ALOAD 0 |
| `local/lito381/server/BattleSpellCatalog$BounceAttackSpell` | method actionId()I insn[2]: left=IRETURN right=ALOAD 0 |
| `local/lito381/server/BattleSpellCatalog$BringStoneSpell` | method actionId()I insn[2]: left=IRETURN right=ALOAD 0 |
| `local/lito381/server/BattleSpellCatalog$BurningSlashSpell` | method actionId()I insn[2]: left=IRETURN right=ALOAD 0 |
| `local/lito381/server/BattleSpellCatalog$CallClanSpell` | method actionId()I insn[2]: left=IRETURN right=ALOAD 0 |
| `local/lito381/server/BattleSpellCatalog$CancellationSpell` | method actionId()I insn[2]: left=IRETURN right=ALOAD 0 |
| `local/lito381/server/BattleSpellCatalog$ClearMindSpell` | method actionId()I insn[2]: left=IRETURN right=ALOAD 0 |
| `local/lito381/server/BattleSpellCatalog$CombatBuffSpell` | method actionId()I insn[2]: left=IRETURN right=ALOAD 0 |
| `local/lito381/server/BattleSpellCatalog$ConfusionSpell` | method durationSeconds()I insn[2]: left=IRETURN right=ALOAD 0 |
| `local/lito381/server/BattleSpellCatalog$CounterBarrierSpell` | method actionId()I insn[2]: left=IRETURN right=ALOAD 0 |
| `local/lito381/server/BattleSpellCatalog$CounterDetectionSpell` | method actionId()I insn[2]: left=IRETURN right=ALOAD 0 |
| `local/lito381/server/BattleSpellCatalog$CounterMagicSpell` | method actionId()I insn[2]: left=IRETURN right=ALOAD 0 |
| `local/lito381/server/BattleSpellCatalog$CounterMirrorSpell` | method actionId()I insn[2]: left=IRETURN right=ALOAD 0 |
| `local/lito381/server/BattleSpellCatalog$CreateZombieSpell` | method actionId()I insn[2]: left=IRETURN right=ALOAD 0 |
| `local/lito381/server/BattleSpellCatalog$CubeHarmonySpell` | method actionId()I insn[2]: left=IRETURN right=ALOAD 0 |
| `local/lito381/server/BattleSpellCatalog$CubeIgnitionSpell` | method actionId()I insn[2]: left=IRETURN right=ALOAD 0 |
| `local/lito381/server/BattleSpellCatalog$CubeQuakeSpell` | method actionId()I insn[2]: left=IRETURN right=ALOAD 0 |
| `local/lito381/server/BattleSpellCatalog$CubeShockSpell` | method actionId()I insn[2]: left=IRETURN right=ALOAD 0 |
| `local/lito381/server/BattleSpellCatalog$CurseRemovalSpell` | method actionId()I insn[2]: left=IRETURN right=ALOAD 0 |
| `local/lito381/server/BattleSpellCatalog$DamageProtectionSpell` | method actionId()I insn[2]: left=IRETURN right=ALOAD 0 |
| `local/lito381/server/BattleSpellCatalog$DecayPotionSpell` | method actionId()I insn[2]: left=IRETURN right=ALOAD 0 |
| `local/lito381/server/BattleSpellCatalog$DetectionSpell` | method actionId()I insn[2]: left=IRETURN right=ALOAD 0 |
| `local/lito381/server/BattleSpellCatalog$DiamondGolemSpell` | method actionId()I insn[2]: left=IRETURN right=ALOAD 0 |
| `local/lito381/server/BattleSpellCatalog$DodgeBuffSpell` | method actionId()I insn[2]: left=IRETURN right=ALOAD 0 |
| `local/lito381/server/BattleSpellCatalog$DragonSkinSpell` | method actionId()I insn[2]: left=IRETURN right=ALOAD 0 |
| `local/lito381/server/BattleSpellCatalog$EarthBlessSpell` | method actionId()I insn[2]: left=IRETURN right=ALOAD 0 |
| `local/lito381/server/BattleSpellCatalog$ElementalFallDownSpell` | method actionId()I insn[2]: left=IRETURN right=ALOAD 0 |
| `local/lito381/server/BattleSpellCatalog$ElementalProtectionSpell` | method actionId()I insn[2]: left=IRETURN right=ALOAD 0 |
| `local/lito381/server/BattleSpellCatalog$ElementalSummonSpell` | method actionId()I insn[2]: left=IRETURN right=ALOAD 0 |
| `local/lito381/server/BattleSpellCatalog$EnchantVenomSpell` | method actionId()I insn[2]: left=IRETURN right=ALOAD 0 |
| `local/lito381/server/BattleSpellCatalog$EntangleSpell` | method actionId()I insn[2]: left=IRETURN right=ALOAD 0 |
| `local/lito381/server/BattleSpellCatalog$EraseMagicSpell` | method actionId()I insn[2]: left=IRETURN right=ALOAD 0 |
| `local/lito381/server/BattleSpellCatalog$ExoticVitalizeSpell` | method actionId()I insn[2]: left=IRETURN right=ALOAD 0 |
| `local/lito381/server/BattleSpellCatalog$FireBlessSpell` | method actionId()I insn[2]: left=IRETURN right=ALOAD 0 |
| `local/lito381/server/BattleSpellCatalog$FireWeaponSpell` | method actionId()I insn[2]: left=IRETURN right=ALOAD 0 |
| `local/lito381/server/BattleSpellCatalog$FoeSlayerSpell` | method actionId()I insn[2]: left=IRETURN right=ALOAD 0 |
| `local/lito381/server/BattleSpellCatalog$FreezeAttackSpell` | method actionId()I insn[2]: left=IRETURN right=ALOAD 0 |
| `local/lito381/server/BattleSpellCatalog$GroundSpell` | method actionId()I insn[2]: left=IRETURN right=ALOAD 0 |
| `local/lito381/server/BattleSpellCatalog$GroupHealingSpell` | method actionId()I insn[2]: left=IRETURN right=ALOAD 0 |
| `local/lito381/server/BattleSpellCatalog$GuardBrakeSpell` | method actionId()I insn[2]: left=IRETURN right=ALOAD 0 |
| `local/lito381/server/BattleSpellCatalog$HasteSpell` | method actionId()I insn[2]: left=IRETURN right=ALOAD 0 |
| `local/lito381/server/BattleSpellCatalog$HealingSpell` | method actionId()I insn[2]: left=IRETURN right=ALOAD 0 |
| `local/lito381/server/BattleSpellCatalog$IllusionAvatarSpell` | method actionId()I insn[2]: left=IRETURN right=ALOAD 0 |
| `local/lito381/server/BattleSpellCatalog$IllusionLichSpell` | method actionId()I insn[2]: left=IRETURN right=ALOAD 0 |
| `local/lito381/server/BattleSpellCatalog$IllusionOgreSpell` | method actionId()I insn[2]: left=IRETURN right=ALOAD 0 |
| `local/lito381/server/BattleSpellCatalog$InsightSpell` | method actionId()I insn[2]: left=IRETURN right=ALOAD 0 |
| `local/lito381/server/BattleSpellCatalog$InvisibilitySpell` | method actionId()I insn[2]: left=IRETURN right=ALOAD 0 |
| `local/lito381/server/BattleSpellCatalog$IronSkinSpell` | method actionId()I insn[2]: left=IRETURN right=ALOAD 0 |
| `local/lito381/server/BattleSpellCatalog$ItemArmorEnchantSpell` | method actionId()I insn[2]: left=IRETURN right=ALOAD 0 |
| `local/lito381/server/BattleSpellCatalog$ItemWeaponEnchantSpell` | method actionId()I insn[2]: left=IRETURN right=ALOAD 0 |
| `local/lito381/server/BattleSpellCatalog$JoyOfPainSpell` | method actionId()I insn[2]: left=IRETURN right=ALOAD 0 |
| `local/lito381/server/BattleSpellCatalog$LightSpell` | method actionId()I insn[2]: left=IRETURN right=ALOAD 0 |
| `local/lito381/server/BattleSpellCatalog$LineAttackSpell` | method actionId()I insn[2]: left=IRETURN right=ALOAD 0 |
| `local/lito381/server/BattleSpellCatalog$MagicResistanceSpell` | method actionId()I insn[2]: left=IRETURN right=ALOAD 0 |
| `local/lito381/server/BattleSpellCatalog$MagicalWeaponSpell` | method actionId()I insn[2]: left=IRETURN right=ALOAD 0 |
| `local/lito381/server/BattleSpellCatalog$ManaDrainSpell` | method actionId()I insn[2]: left=IRETURN right=ALOAD 0 |
| `local/lito381/server/BattleSpellCatalog$MassSlowSpell` | method actionId()I insn[2]: left=IRETURN right=ALOAD 0 |
| `local/lito381/server/BattleSpellCatalog$MassTeleportSpell` | method actionId()I insn[2]: left=IRETURN right=ALOAD 0 |
| `local/lito381/server/BattleSpellCatalog$MindBreakSpell` | method hpCost()I insn[2]: left=IRETURN right=ALOAD 0 |
| `local/lito381/server/BattleSpellCatalog$MortalBodySpell` | method actionId()I insn[2]: left=IRETURN right=ALOAD 0 |
| `local/lito381/server/BattleSpellCatalog$MovementBuffSpell` | method actionId()I insn[2]: left=IRETURN right=ALOAD 0 |
| `local/lito381/server/BattleSpellCatalog$NaturesBlessingSpell` | method actionId()I insn[2]: left=IRETURN right=ALOAD 0 |
| `local/lito381/server/BattleSpellCatalog$NaturesTouchSpell` | method actionId()I insn[2]: left=IRETURN right=ALOAD 0 |
| `local/lito381/server/BattleSpellCatalog$OtherNoEffectSpell` | method actionId()I insn[2]: left=IRETURN right=ALOAD 0 |
| `local/lito381/server/BattleSpellCatalog$PanicSpell` | method actionId()I insn[2]: left=IRETURN right=ALOAD 0 |
| `local/lito381/server/BattleSpellCatalog$ParalysisSpell` | method actionId()I insn[2]: left=IRETURN right=ALOAD 0 |
| `local/lito381/server/BattleSpellCatalog$PatienceSpell` | method actionId()I insn[2]: left=IRETURN right=ALOAD 0 |
| `local/lito381/server/BattleSpellCatalog$PhantasmSpell` | method actionId()I insn[2]: left=IRETURN right=ALOAD 0 |
| `local/lito381/server/BattleSpellCatalog$PhysicalProcBuffSpell` | method actionId()I insn[2]: left=IRETURN right=ALOAD 0 |
| `local/lito381/server/BattleSpellCatalog$PoisonResistanceSpell` | method actionId()I insn[2]: left=IRETURN right=ALOAD 0 |
| `local/lito381/server/BattleSpellCatalog$PoisonSpell` | method actionId()I insn[2]: left=IRETURN right=ALOAD 0 |
| `local/lito381/server/BattleSpellCatalog$PolluteWaterSpell` | method actionId()I insn[2]: left=IRETURN right=ALOAD 0 |
| `local/lito381/server/BattleSpellCatalog$PolymorphSpell` | method actionId()I insn[2]: left=IRETURN right=ALOAD 0 |
| `local/lito381/server/BattleSpellCatalog$RangedEvasionBuffSpell` | method actionId()I insn[2]: left=IRETURN right=ALOAD 0 |
| `local/lito381/server/BattleSpellCatalog$ReductionArmorSpell` | method actionId()I insn[2]: left=IRETURN right=ALOAD 0 |
| `local/lito381/server/BattleSpellCatalog$RegenSpell` | method actionId()I insn[2]: left=IRETURN right=ALOAD 0 |
| `local/lito381/server/BattleSpellCatalog$ReservedSkillSlot` | method durationSeconds()I insn[2]: left=IRETURN right=ALOAD 0 |
| `local/lito381/server/BattleSpellCatalog$ResistElementalSpell` | method actionId()I insn[2]: left=IRETURN right=ALOAD 0 |
| `local/lito381/server/BattleSpellCatalog$ResistFearSpell` | method actionId()I insn[2]: left=IRETURN right=ALOAD 0 |
| `local/lito381/server/BattleSpellCatalog$ResurrectionSpell` | method actionId()I insn[2]: left=IRETURN right=ALOAD 0 |
| `local/lito381/server/BattleSpellCatalog$ReturnToNatureSpell` | method actionId()I insn[2]: left=IRETURN right=ALOAD 0 |
| `local/lito381/server/BattleSpellCatalog$RevealWeaknessSpell` | method actionId()I insn[2]: left=IRETURN right=ALOAD 0 |
| `local/lito381/server/BattleSpellCatalog$RoyalAuraSpell` | method actionId()I insn[2]: left=IRETURN right=ALOAD 0 |
| `local/lito381/server/BattleSpellCatalog$RunClanSpell` | method actionId()I insn[2]: left=IRETURN right=ALOAD 0 |
| `local/lito381/server/BattleSpellCatalog$SelfHasteSpell` | method actionId()I insn[2]: left=IRETURN right=ALOAD 0 |
| `local/lito381/server/BattleSpellCatalog$ShieldSpell` | method actionId()I insn[2]: left=IRETURN right=ALOAD 0 |
| `local/lito381/server/BattleSpellCatalog$ShockStunSpell` | method baseDamage()I insn[2]: left=IRETURN right=ALOAD 0 |
| `local/lito381/server/BattleSpellCatalog$SilenceSpell` | method actionId()I insn[2]: left=IRETURN right=ALOAD 0 |
| `local/lito381/server/BattleSpellCatalog$SleepSpell` | method actionId()I insn[2]: left=IRETURN right=ALOAD 0 |
| `local/lito381/server/BattleSpellCatalog$SolidCarriageSpell` | method actionId()I insn[2]: left=IRETURN right=ALOAD 0 |
| `local/lito381/server/BattleSpellCatalog$StormEyeSpell` | method actionId()I insn[2]: left=IRETURN right=ALOAD 0 |
| `local/lito381/server/BattleSpellCatalog$StormShotSpell` | method actionId()I insn[2]: left=IRETURN right=ALOAD 0 |
| `local/lito381/server/BattleSpellCatalog$StrikerGaleSpell` | method actionId()I insn[2]: left=IRETURN right=ALOAD 0 |
| `local/lito381/server/BattleSpellCatalog$SummonSpell` | method actionId()I insn[2]: left=IRETURN right=ALOAD 0 |
| `local/lito381/server/BattleSpellCatalog$TamingSpell` | method actionId()I insn[2]: left=IRETURN right=ALOAD 0 |
| `local/lito381/server/BattleSpellCatalog$ThunderGrabSpell` | method bindDurationSeconds(I)I insn[3]: left=GETFIELD local/lito381/server/BattleSpellCatalog$ThunderGrabSpell.minimumBindSeconds:I right=INVOKEVIRTUAL local/lito381/server/BattleSpellCatalog$ThunderGrabSpell.skillId()I |
| `local/lito381/server/BattleSpellCatalog$TripleArrowSpell` | method durationSeconds()I insn[2]: left=IRETURN right=ALOAD 0 |
| `local/lito381/server/BattleSpellCatalog$TrueTargetSpell` | method actionId()I insn[2]: left=IRETURN right=ALOAD 0 |
| `local/lito381/server/BattleSpellCatalog$TurnUndeadSpell` | method actionId()I insn[2]: left=IRETURN right=ALOAD 0 |
| `local/lito381/server/BattleSpellCatalog$WaterLifeSpell` | method actionId()I insn[2]: left=IRETURN right=ALOAD 0 |
| `local/lito381/server/BattleSpellCatalog$WeaknessSpell` | method actionId()I insn[2]: left=IRETURN right=ALOAD 0 |
| `local/lito381/server/BattleSpellCatalog$WeaponBreakSpell` | method actionId()I insn[2]: left=IRETURN right=ALOAD 0 |
| `local/lito381/server/BattleSpellCatalog$WeaponBuffSpell` | method actionId()I insn[2]: left=IRETURN right=ALOAD 0 |
| `local/lito381/server/BattleSpellCatalog$WeightSpell` | method actionId()I insn[2]: left=IRETURN right=ALOAD 0 |
| `local/lito381/server/BattleSpellCatalog$WindShackleSpell` | method actionId()I insn[2]: left=IRETURN right=ALOAD 0 |
| `local/lito381/server/BattleSpellCatalog$WindShotSpell` | method actionId()I insn[2]: left=IRETURN right=ALOAD 0 |
| `local/lito381/server/BattleSpellCatalog$WorldTreeCallSpell` | method actionId()I insn[2]: left=IRETURN right=ALOAD 0 |
| `local/lito381/server/BattleStormEyeStatus` | method snapshot(J)Llocal/lito381/server/BattleStormEyeStatus$Snapshot; insn[51]: left=ALOAD 0 right=LDC Integer:156 |
| `local/lito381/server/BattleStormShotStatus` | method snapshot(J)Llocal/lito381/server/BattleStormShotStatus$Snapshot; insn[51]: left=ALOAD 0 right=LDC Integer:166 |
| `local/lito381/server/BattleStrikerGale` | method damageAfterStatus(IZZ)I insn[21]: left=DMUL right=LDC str:"STRIKER_DMG2" |
| `local/lito381/server/BattleTimeAttackEncounter` | field sourceProcEncounterGeneration:Ljava/lang/Object;: only right |
| `local/lito381/server/BattleTimeAttackEncounter$Monster` | method damage(IZ)Llocal/lito381/server/BattleTimeAttackEncounter$Damage; insn[6]: left=IFEQ L1 right=ALOAD 0 |
| `local/lito381/server/BattleTimedStatus` | class innerClasses: left=local/lito381/server/BattleTimedStatus$1\|\|\|0x1008; local/lito381/server/BattleTimedStatus$Brave\|local/lito381/server/BattleTimedStatus\|Brave\|0x18; local/lito381/server/BattleTimedStatus$Breath\|local/lito381/server/BattleTimedStatus\|Breath\|0… right=local/lito381/server/BattleTimedStatus$1\|\|\|0x1008; local/lito381/server/BattleTimedStatus$Brave\|local/lito381/server/BattleTimedStatus\|Brave\|0x18; local/lito381/server/BattleTimedStatus$Breath\|local/lito381/server/BattleTimedStatus\|Breath\|0… |
| `local/lito381/server/BattleTurnUndead` | method magicLevel(II)I insn[0]: left=ILOAD 1 right=ILOAD 0 |
| `local/lito381/server/BattleWeaponBreak` | method damageAfterStacks(II)I insn[14]: left=ILOAD 0 right=ILOAD 1 |
| `local/lito381/server/BattleWindShotStatus` | method snapshot(J)Llocal/lito381/server/BattleWindShotStatus$Snapshot; insn[51]: left=ILOAD 7 right=LDC Integer:149 |
| `local/lito381/server/CaptureLog` | class nestMembers: left=local/lito381/server/CaptureLog$Connection right= |
| `local/lito381/server/ClassicWorldAdenToiCatalog` | method <clinit>()V insn[279]: left=LDC str:"5DFCF849F13FC240637C137DECDBD146F732D24E7A82EF687F7CF80F595B182F" right=LDC str:"53F6D1C7AA4597C73DF8D1708B301B47756CCBE8FF7784448CA5A5CDEE1CBD7D" |
| `local/lito381/server/ClassicWorldIvoryTowerCatalog` | method <clinit>()V insn[183]: left=LDC str:"34D2B2A2470E3494509B5146EB2533B5D82C182CAC50B5AE6DDD2C9C78D01657" right=LDC str:"5B0053AF99F0F66A103D8D292E68118924C5709BD9B16FBD189F7A9E460B593C" |
| `local/lito381/server/ClassicWorldMapCatalog` | method forMap(I)Llocal/lito381/server/ClassicWorldMapCatalog$ClassicMap; insn[0]: left=GETSTATIC local/lito381/server/ClassicWorldMapCatalog.MAPS:Ljava/util/Map; right=ILOAD 0 |
| `local/lito381/server/ClassicWorldMonsterPopulation` | method actionLocked(Llocal/lito381/server/ClassicWorldMonsterPopulation$Actor;J)Z insn[17]: left=IFNE L0 right=ALOAD 0 |
| `local/lito381/server/ClassicWorldMonsterPopulation$Actor` | method snapshot()Llocal/lito381/server/ClassicWorldMonsterPopulation$Snapshot; insn[57]: left=ARETURN right=ALOAD 0 |
| `local/lito381/server/ClassicWorldMonsterPopulation$Snapshot` | field sourceCombatOrigin:Ljava/lang/Object;: only right |
| `local/lito381/server/ClassicWorldNpcCatalog` | method all(I)Ljava/util/List; insn[43]: left=ARETURN right=ILOAD 0 |
| `local/lito381/server/ClientSession` | class innerClasses: left=java/lang/invoke/MethodHandles$Lookup\|java/lang/invoke/MethodHandles\|Lookup\|0x19; java/util/Map$Entry\|java/util/Map\|Entry\|0x609; java/util/concurrent/ConcurrentHashMap$KeySetView\|java/util/concurrent/ConcurrentHashMap\|KeySetView\|0x9; local/… right=java/lang/invoke/MethodHandles$Lookup\|java/lang/invoke/MethodHandles\|Lookup\|0x19; java/util/Map$Entry\|java/util/Map\|Entry\|0x609; java/util/concurrent/ConcurrentHashMap$KeySetView\|java/util/concurrent/ConcurrentHashMap\|KeySetView\|0x9; local/… |
| `local/lito381/server/ClientSession$PendingPetRename` | field sourceCompanionCallback:Ljava/lang/Object;: only right |
| `local/lito381/server/ClientSession$PendingSlaveTarget` | field sourceCompanionCallback:Ljava/lang/Object;: only right |
| `local/lito381/server/CombatCompanionSpells` | method shock(Llocal/lito381/server/ClientSession;[BLlocal/lito381/server/BattleSpellCatalog$ShockStunSpell;Ljava/util/function/IntUnaryOperator;)Z insn[178]: left=INVOKEVIRTUAL local/lito381/server/BattleSpellCatalog$ShockStunSpell.effectiveMpCost(II)I right=INVOKESTATIC local/lito381/server/NativeSkillConsumptionBridges.ShockStunSpell_effectiveMpCost_III(Llocal/lito381/server/BattleSpellCatalog$ShockStunSpell;II)I |
| `local/lito381/server/CombatCurseLifecycle$Removal` | method drain(Z)V: left=TRY L55 L56 L57    right=TRY L57 L58 L59    |
| `local/lito381/server/CombatCurseLifecycle$Target` | field nativeRoleOwner:Ljava/lang/Object;: only right |
| `local/lito381/server/CombatDatabase` | method load(Llocal/lito381/server/ServerConfig;)V insn[0]: left=GETSTATIC local/lito381/server/CombatDatabase.current:Llocal/lito381/server/CombatDatabase$Snapshot; right=ACONST_NULL |
| `local/lito381/server/CombatMonsterCurseLifecycle` | method damageOwner(Llocal/lito381/server/ClientSession;Llocal/lito381/server/ClassicWorldMonsterPopulation$PoisonUpdate;Llocal/lito381/server/CombatPoisonLifecycle$Scope;)Llocal/lito381/server/ClientSession; insn[7]: left=ARETURN right=ALOAD 3 |
| `local/lito381/server/CombatMonsterCurseLifecycle$Context` | method valid()Z insn[7]: left=IRETURN right=ALOAD 0 |
| `local/lito381/server/CombatNpcDamageQueue$Queue` | field sourceDamageLifetime:Ljava/lang/Object;: only right |
| `local/lito381/server/CombatNpcDebuffs` | method negative(Llocal/lito381/server/ClientSession;[BLlocal/lito381/server/BattleSpellCatalog$TargetNegativeStatusSpell;I)Z insn[287]: left=INVOKEINTERFACE local/lito381/server/BattleSpellCatalog$TargetNegativeStatusSpell.effectiveMpCost(I)I itf right=ALOAD 0 |
| `local/lito381/server/CombatPoisonAtomic` | method npc(Llocal/lito381/server/ClientSession;JIIIJLjava/lang/String;Llocal/lito381/server/CombatPoisonLifecycle$Scope;)Llocal/lito381/server/CombatPoisonAtomic$NpcState;: left=TRY L1 L3 L4    right=TRY L2 L4 L5    |
| `local/lito381/server/CombatPoisonLifecycle` | method battle(Llocal/lito381/server/ClientSession;ILlocal/lito381/server/ClientSession;Llocal/lito381/server/CombatPoisonLifecycle$Scope;)V: left=TRY L4 L9 L10    right=TRY L5 L10 L11    |
| `local/lito381/server/CombatPoisonLifecycle$Context` | method valid()Z insn[49]: left=IRETURN right=ALOAD 0 |
| `local/lito381/server/CombatPoisonLifecycle$Lease` | field sourceCombatOrigin:Ljava/lang/Object;: only right |
| `local/lito381/server/CombatPoisonLifecycle$State` | field sourceNpcOwner:Ljava/lang/Object;: only right |
| `local/lito381/server/CombatRules` | method <clinit>()V insn[0]: left=BIPUSH 28 right=BIPUSH 32 |
| `local/lito381/server/CombatWorldCommit` | method owner(Llocal/lito381/server/CombatMonsterCurseLifecycle$Context;)Llocal/lito381/server/ClientSession; insn[7]: left=ARETURN right=ALOAD 0 |
| `local/lito381/server/CombatWorldCommit$Plan` | class innerClasses: left=local/lito381/server/CombatWorldCommit$Doll\|local/lito381/server/CombatWorldCommit\|Doll\|0x18; local/lito381/server/CombatWorldCommit$Physical\|local/lito381/server/CombatWorldCommit\|Physical\|0x18; local/lito381/server/CombatWorldCommit$Plan\|… right=local/lito381/server/CombatWorldCommit$Doll\|local/lito381/server/CombatWorldCommit\|Doll\|0x18; local/lito381/server/CombatWorldCommit$Physical\|local/lito381/server/CombatWorldCommit\|Physical\|0x18; local/lito381/server/CombatWorldCommit$Plan\|… |
| `local/lito381/server/GameHub` | class innerClasses: left=java/lang/invoke/MethodHandles$Lookup\|java/lang/invoke/MethodHandles\|Lookup\|0x19; java/util/Map$Entry\|java/util/Map\|Entry\|0x609; local/lito381/server/ArenaMapCatalog$Arena\|local/lito381/server/ArenaMapCatalog\|Arena\|0x18; local/lito381/serve… right=java/lang/invoke/MethodHandles$Lookup\|java/lang/invoke/MethodHandles\|Lookup\|0x19; java/util/Map$Entry\|java/util/Map\|Entry\|0x609; local/lito381/server/ArenaMapCatalog$Arena\|local/lito381/server/ArenaMapCatalog\|Arena\|0x18; local/lito381/serve… |
| `local/lito381/server/GameHub$Channel` | field members:Ljava/util/Set;: only right |
| `local/lito381/server/GameHub$MonsterAiTick` | field sourceCombatOrigin:Ljava/lang/Object;: only right |
| `local/lito381/server/GameHub$RentalBattleHandoff` | method inventory()Llocal/lito381/server/RuntimeInventory; insn[3]: left=ARETURN right=ALOAD 0 |
| `local/lito381/server/GameStateStore` | method ensureRankingSnapshot()V insn[1]: left=INVOKEVIRTUAL local/lito381/server/GameStateStore.currentRankingBoundary()Ljava/time/ZonedDateTime; right=INVOKESTATIC local/lito381/server/LiveBattleStats.beforeSnapshot(Llocal/lito381/server/GameStateStore;)V |
| `local/lito381/server/InventoryCatalog$Definition` | method controlAbilityType()I insn[2]: left=LDC Long:20288 right=LDC Long:120284 |
| `local/lito381/server/InventoryCatalog$NativeArmorStatus` | method <init>(IIIIIIIIIIIIIIIZIIIIIIIIIII)V insn[5]: left=SIPUSH 255 right=LDC Integer:2147483647 |
| `local/lito381/server/LegacyMySqlRepository` | method <init>(Ljava/nio/file/Path;Ljava/lang/String;ILjava/lang/String;Ljava/lang/String;DZLjava/lang/String;)V insn[33]: left=INVOKEVIRTUAL java/util/Properties.load(Ljava/io/Reader;)V right=DUP2 |
| `local/lito381/server/LiToGmCommands` | method handle(Llocal/lito381/server/ClientSession;Ljava/lang/String;)Z insn[90]: left=INVOKEINTERFACE local/lito381/server/AccountRepository.accessLevel(Ljava/lang/String;)I itf right=ALOAD 0 |
| `local/lito381/server/LiToItemDetails` | field SOURCE:Ljava/lang/reflect/Method;: only right |
| `local/lito381/server/LitoMapRepository` | method <init>(Llocal/lito381/server/ServerConfig;)V insn[65]: left=LDC str:"URL1" right=ALOAD 2 |
| `local/lito381/server/Main` | method accept(Ljava/net/ServerSocket;Llocal/lito381/server/ClientSession$Endpoint;ILlocal/lito381/server/ServerConfig;Llocal/lito381/server/AccountRepository;Llocal/lito381/server/GameStateStore;Llocal/lito381/server/CaptureLog;Llocal/lito381/server/GameHub;Ljava/util/concurrent/ExecutorService;)V: length left=1 right=2 |
| `local/lito381/server/MemoryAccountRepository` | field accessLevels:Ljava/util/concurrent/ConcurrentHashMap;: only left |
| `local/lito381/server/NativeAcceptedHpCompletion` | method enqueue(Llocal/lito381/server/NativePlayerCommit$Frame;)Llocal/lito381/server/NativeAcceptedHpCompletion; insn[22]: left=ALOAD 3 right=LABEL L1 |
| `local/lito381/server/NativeAcceptedHpContext` | method resolve()[Ljava/lang/ThreadLocal;: left=TRY L0 L6 L7 java/lang/ReflectiveOperationException   right=TRY L0 L5 L6 java/lang/ReflectiveOperationException   |
| `local/lito381/server/NativeAcceptedState` | class innerClasses: left=java/lang/invoke/MethodHandles$Lookup\|java/lang/invoke/MethodHandles\|Lookup\|0x19; local/lito381/server/NativeAcceptedState$Incoming\|local/lito381/server/NativeAcceptedState\|Incoming\|0x18; local/lito381/server/NativeAcceptedState$Publication… right=java/lang/invoke/MethodHandles$Lookup\|java/lang/invoke/MethodHandles\|Lookup\|0x19; local/lito381/server/NativeAcceptedState$Incoming\|local/lito381/server/NativeAcceptedState\|Incoming\|0x18; local/lito381/server/NativeAcceptedState$Publication… |
| `local/lito381/server/NativeAreaMaterialAction` | class innerClasses: left=java/lang/invoke/MethodHandles$Lookup\|java/lang/invoke/MethodHandles\|Lookup\|0x19; java/util/Map$Entry\|java/util/Map\|Entry\|0x609; local/lito381/server/BattleCompanion$Snapshot\|local/lito381/server/BattleCompanion\|Snapshot\|0x18; local/lito381… right=java/lang/invoke/MethodHandles$Lookup\|java/lang/invoke/MethodHandles\|Lookup\|0x19; java/util/Map$Entry\|java/util/Map\|Entry\|0x609; local/lito381/server/BattleCompanion$Snapshot\|local/lito381/server/BattleCompanion\|Snapshot\|0x18; local/lito381… |
| `local/lito381/server/NativeCancellationMaterialAction` | class innerClasses: left=java/lang/invoke/MethodHandles$Lookup\|java/lang/invoke/MethodHandles\|Lookup\|0x19; local/lito381/server/BattleSpellCatalog$CancellationSpell\|local/lito381/server/BattleSpellCatalog\|CancellationSpell\|0x18; local/lito381/server/CombatPoisonLif… right=java/lang/invoke/MethodHandles$Lookup\|java/lang/invoke/MethodHandles\|Lookup\|0x19; local/lito381/server/BattleSpellCatalog$CancellationSpell\|local/lito381/server/BattleSpellCatalog\|CancellationSpell\|0x18; local/lito381/server/CombatPoisonLif… |
| `local/lito381/server/NativeCancellationSpeedImmunity` | method magic(Llocal/lito381/server/BattleTimedStatus;Llocal/lito381/server/ClientSession;Llocal/lito381/server/CombatPoisonLifecycle$Scope;)Llocal/lito381/server/BattleTimedStatus$MagicCancellation;: left=TRY L5 L6 L8    right=TRY L15 L16 L23    |
| `local/lito381/server/NativeCastMaterial` | class nestMembers: left=local/lito381/server/NativeCastMaterial$Cast,local/lito381/server/NativeCastMaterial$ItemView right=local/lito381/server/NativeCastMaterial$Cast,local/lito381/server/NativeCastMaterial$Effect,local/lito381/server/NativeCastMaterial$ItemView |
| `local/lito381/server/NativeCastMaterial$Cast` | class innerClasses: left=local/lito381/server/NativeCastMaterial$Cast\|local/lito381/server/NativeCastMaterial\|Cast\|0x18; local/lito381/server/NativeDeItemApplication$Owner\|local/lito381/server/NativeDeItemApplication\|Owner\|0x18; local/lito381/server/NativeDeItemTra… right=local/lito381/server/NativeCastMaterial$Cast\|local/lito381/server/NativeCastMaterial\|Cast\|0x18; local/lito381/server/NativeCastMaterial$Effect\|local/lito381/server/NativeCastMaterial\|Effect\|0x608; local/lito381/server/NativeDeItemApplicatio… |
| `local/lito381/server/NativeCastMaterialPolicy` | method nativeOriginalGm(Llocal/lito381/server/ClientSession;)Z: left=TRY L11 L13 L18 java/lang/Throwable   right=TRY L9 L11 L12 java/lang/Throwable   |
| `local/lito381/server/NativeCastTimeline` | method admit(Llocal/lito381/server/ClientSession;Llocal/lito381/server/BattleSpellCatalog$LightSpell;Llocal/lito381/server/NativeCastTimeline$Arrival;)Z insn[12]: left=ASTORE 4 right=ASTORE 3 |
| `local/lito381/server/NativeCastTimeline$1` | field val$action:Llocal/lito381/server/NativeSourceClientActionTimeline$Reservation;: only right |
| `local/lito381/server/NativeCastTimeline$2` | field val$action:Llocal/lito381/server/NativeSourceClientActionTimeline$Reservation;: only right |
| `local/lito381/server/NativeCombatAttacker` | method advance(Ljava/lang/Object;)V insn[12]: left=ASTORE 2 right=ASTORE 1 |
| `local/lito381/server/NativeCombatOrigin` | method contextValid(ZLjava/lang/Object;)Z insn[11]: left=ASTORE 3 right=ASTORE 2 |
| `local/lito381/server/NativeCompanionCallbacks` | method advance(Ljava/lang/Object;)V insn[2]: left=ASTORE 2 right=ASTORE 1 |
| `local/lito381/server/NativeCompanionCommands` | method registered(Llocal/lito381/server/ClientSession;)Z insn[40]: left=IF_ACMPEQ L3 right=IF_ACMPNE L3 |
| `local/lito381/server/NativeCompanionCommit` | method valid(Llocal/lito381/server/ClientSession;Llocal/lito381/server/BattleCompanion;Llocal/lito381/server/NativePlayerCommit$Binding;)Z: left=TRY L4 L9 L10    right=TRY L4 L8 L9    |
| `local/lito381/server/NativeCompanionMutation` | method current(Llocal/lito381/server/ClientSession;Ljava/lang/Object;Llocal/lito381/server/BattleCompanion;)Z insn[9]: left=ASTORE 3 right=DUP |
| `local/lito381/server/NativeDeActorState` | method acquire(Llocal/lito381/server/BattleTimedStatus;)Llocal/lito381/server/NativeDeActorState$Lease; insn[1]: left=ASTORE 1 right=DUP |
| `local/lito381/server/NativeDeActorState$1` | field val$arrayList:Ljava/util/ArrayList;: only left |
| `local/lito381/server/NativeDeAiState` | method acquire(Llocal/lito381/server/BattleTimedStatus;)Llocal/lito381/server/NativeDeAiState$Lease; insn[1]: left=ASTORE 1 right=DUP |
| `local/lito381/server/NativeDeAiState$1` | field val$arrayList:Ljava/util/ArrayList;: only left |
| `local/lito381/server/NativeDeAiTimeline` | method background(Llocal/lito381/server/NativeDeAiTimeline$State;Llocal/lito381/server/NativeDeAiTimeline$Port;J)V insn[2]: left=ASTORE 4 right=DUP |
| `local/lito381/server/NativeDeAiTimerProtocol` | method schedule(Llocal/lito381/server/NativeDeAiTimeline$State;Llocal/lito381/server/NativeDeAiTimerProtocol$Port;JIZ)Ljava/lang/Object; insn[12]: left=ASTORE 8 right=DUP |
| `local/lito381/server/NativeDeAiTimers` | method claimReady(Llocal/lito381/server/GameHub;)Ljava/util/List; insn[3]: left=ASTORE 2 right=ASTORE 1 |
| `local/lito381/server/NativeDeAiTimers$1` | field val$life:Llocal/lito381/server/NativePlayerLifetime;: only right |
| `local/lito381/server/NativeDeAiTimers$2` | field val$gameHub:Llocal/lito381/server/GameHub;: only left |
| `local/lito381/server/NativeDeAiTimers$Entry` | method close(Z)V insn[9]: left=ASTORE 3 right=DUP |
| `local/lito381/server/NativeDeAiWorkerProtocol` | method dispatch(Llocal/lito381/server/NativeDeAiTimeline$State;Llocal/lito381/server/NativeDeAiWorkerProtocol$Port;JZ)V insn[2]: left=ASTORE 5 right=DUP |
| `local/lito381/server/NativeDeAiWorkers` | method enable(Llocal/lito381/server/GameHub;Z)V insn[7]: left=ASTORE 3 right=DUP |
| `local/lito381/server/NativeDeAiWorkers$1` | field val$atomicBoolean:Ljava/util/concurrent/atomic/AtomicBoolean;: only left |
| `local/lito381/server/NativeDeCachedFactoryServices` | class innerClasses: left=java/lang/invoke/MethodHandles$Lookup\|java/lang/invoke/MethodHandles\|Lookup\|0x19; local/lito381/server/NativeDeControlCatalog$Abilities\|local/lito381/server/NativeDeControlCatalog\|Abilities\|0x18; local/lito381/server/NativeDeControlCatalog$… right=java/lang/invoke/MethodHandles$Lookup\|java/lang/invoke/MethodHandles\|Lookup\|0x19; local/lito381/server/NativeDeControlCatalog$Abilities\|local/lito381/server/NativeDeControlCatalog\|Abilities\|0x18; local/lito381/server/NativeDeControlCatalog$… |
| `local/lito381/server/NativeDeCharacterActor` | class innerClasses: left=local/lito381/server/NativeDeCharacterFields$Snapshot\|local/lito381/server/NativeDeCharacterFields\|Snapshot\|0x18; local/lito381/server/NativeDeCorpseTimers$Owner\|local/lito381/server/NativeDeCorpseTimers\|Owner\|0x18; local/lito381/server/Nat… right=local/lito381/server/NativeDeCharacterFields$Snapshot\|local/lito381/server/NativeDeCharacterFields\|Snapshot\|0x18; local/lito381/server/NativeDeCorpseTimers$Owner\|local/lito381/server/NativeDeCorpseTimers\|Owner\|0x18; local/lito381/server/Nat… |
| `local/lito381/server/NativeDeCharacterControls` | method clientType()I insn[9]: left=ASTORE 1 right=DUP |
| `local/lito381/server/NativeDeCharacterFields` | class innerClasses: left=java/lang/invoke/MethodHandles$Lookup\|java/lang/invoke/MethodHandles\|Lookup\|0x19; java/util/Map$Entry\|java/util/Map\|Entry\|0x609; local/lito381/server/NativeDeCharacterFields$CombatSkillDerived\|local/lito381/server/NativeDeCharacterFields\|Co… right=java/lang/invoke/MethodHandles$Lookup\|java/lang/invoke/MethodHandles\|Lookup\|0x19; java/util/Map$Entry\|java/util/Map\|Entry\|0x609; local/lito381/server/NativeDeCharacterFields$CombatSkillDerived\|local/lito381/server/NativeDeCharacterFields\|Co… |
| `local/lito381/server/NativeDeCharacterFields$CombatSkillDerived` | method compute(Ljava/lang/Iterable;)Llocal/lito381/server/NativeDeCharacterFields$CombatSkillDerived; insn[31]: left=ASTORE 13 right=ASTORE 9 |
| `local/lito381/server/NativeDeCharacterFields$DamageMutationReceipt` | method <init>(Llocal/lito381/server/NativeDeCharacterFields;)V insn[6]: left=ALOAD 0 right=ALOAD 1 |
| `local/lito381/server/NativeDeCharacterFields$ViewerHpState` | class innerClasses: left=local/lito381/server/NativeDeCharacterFields$ViewerHpState\|local/lito381/server/NativeDeCharacterFields\|ViewerHpState\|0x1a; local/lito381/server/NativeDeDeletionPorts$Viewer\|local/lito381/server/NativeDeDeletionPorts\|Viewer\|0x408 right=local/lito381/server/NativeDeCharacterFields$ViewerHpState\|local/lito381/server/NativeDeCharacterFields\|ViewerHpState\|0x1a |
| `local/lito381/server/NativeDeChatBroadcast` | class innerClasses: left=java/lang/invoke/MethodHandles$Lookup\|java/lang/invoke/MethodHandles\|Lookup\|0x19; local/lito381/server/NativeDeDeletionPorts$ObjectRef\|local/lito381/server/NativeDeDeletionPorts\|ObjectRef\|0x608; local/lito381/server/NativeDeDeletionPorts$Vi… right= |
| `local/lito381/server/NativeDeChatChat` | class innerClasses: left=java/lang/invoke/MethodHandles$Lookup\|java/lang/invoke/MethodHandles\|Lookup\|0x19; local/lito381/server/NativeDeDeletionPorts$ObjectRef\|local/lito381/server/NativeDeDeletionPorts\|ObjectRef\|0x608; local/lito381/server/NativeDeDeletionPorts$Vi… right=java/lang/invoke/MethodHandles$Lookup\|java/lang/invoke/MethodHandles\|Lookup\|0x19 |
| `local/lito381/server/NativeDeChatChatGlobal` | class innerClasses: left=java/lang/invoke/MethodHandles$Lookup\|java/lang/invoke/MethodHandles\|Lookup\|0x19; local/lito381/server/NativeDeDeletionPorts$ObjectRef\|local/lito381/server/NativeDeDeletionPorts\|ObjectRef\|0x608 right=java/lang/invoke/MethodHandles$Lookup\|java/lang/invoke/MethodHandles\|Lookup\|0x19 |
| `local/lito381/server/NativeDeChatChatShouting` | class innerClasses: left=java/lang/invoke/MethodHandles$Lookup\|java/lang/invoke/MethodHandles\|Lookup\|0x19; local/lito381/server/NativeDeDeletionPorts$ObjectRef\|local/lito381/server/NativeDeDeletionPorts\|ObjectRef\|0x608 right=java/lang/invoke/MethodHandles$Lookup\|java/lang/invoke/MethodHandles\|Lookup\|0x19 |
| `local/lito381/server/NativeDeChatJobs` | method call(Llocal/lito381/server/NativeDeChatJobs$Owner;Llocal/lito381/server/NativeDeNpcConstructionState;Ljava/util/function/Supplier;)Ljava/lang/Object; insn[20]: left=ALOAD 4 right=LABEL L2 |
| `local/lito381/server/NativeDeChatJobs$Ticket` | method discard()V insn[1]: left=ASTORE 1 right=DUP |
| `local/lito381/server/NativeDeChatResources` | method load(Llocal/lito381/server/ServerConfig;)V insn[7]: left=ASTORE 2 right=ASTORE 1 |
| `local/lito381/server/NativeDeChatScope` | method call(Llocal/lito381/server/NativeDeChatJobs$Owner;Llocal/lito381/server/NativeDeNpcConstructionState;Ljava/util/function/Supplier;)Ljava/lang/Object; insn[31]: left=ALOAD 5 right=LABEL L2 |
| `local/lito381/server/NativeDeChatTable` | class innerClasses: left=java/lang/invoke/MethodHandles$Lookup\|java/lang/invoke/MethodHandles\|Lookup\|0x19; local/lito381/server/NativeDeChatScope$Data\|local/lito381/server/NativeDeChatScope\|Data\|0x18; local/lito381/server/NativeDeChatScope$Logs\|local/lito381/server… right=java/lang/invoke/MethodHandles$Lookup\|java/lang/invoke/MethodHandles\|Lookup\|0x19 |
| `local/lito381/server/NativeDeChatTimer` | class innerClasses: left=java/lang/invoke/MethodHandles$Lookup\|java/lang/invoke/MethodHandles\|Lookup\|0x19; local/lito381/server/NativeDeChatScope$Clock\|local/lito381/server/NativeDeChatScope\|Clock\|0x18; local/lito381/server/NativeDeChatScope$Logs\|local/lito381/serv… right=java/lang/invoke/MethodHandles$Lookup\|java/lang/invoke/MethodHandles\|Lookup\|0x19; local/lito381/server/NativeDeChatTimer$RunSequenceTask\|local/lito381/server/NativeDeChatTimer\|RunSequenceTask\|0x12 |
| `local/lito381/server/NativeDeChatTimer$RunSequenceTask` | class innerClasses: left=local/lito381/server/NativeDeChatScope$Pool$ShutdownDiscardable\|local/lito381/server/NativeDeChatScope$Pool\|ShutdownDiscardable\|0x608; local/lito381/server/NativeDeChatScope$Pool\|local/lito381/server/NativeDeChatScope\|Pool\|0x18; local/lito3… right=local/lito381/server/NativeDeChatScope$Pool$ShutdownDiscardable\|local/lito381/server/NativeDeChatScope$Pool\|ShutdownDiscardable\|0x609; local/lito381/server/NativeDeChatTimer$RunSequenceTask\|local/lito381/server/NativeDeChatTimer\|RunSequence… |
| `local/lito381/server/NativeDeChatWire` | method content(Llocal/lito381/server/NativeDeVisibilityPacket;Llocal/lito381/server/NativeDeDeletionPorts$ObjectRef;)[B: left=TRY L0 L6 L7    right=TRY L0 L5 L6    |
| `local/lito381/server/NativeDeClientViewer` | class innerClasses: left=java/lang/invoke/MethodHandles$Lookup\|java/lang/invoke/MethodHandles\|Lookup\|0x19; local/lito381/server/NativeDeCorpseTimers$Owner\|local/lito381/server/NativeDeCorpseTimers\|Owner\|0x18; local/lito381/server/NativeDeDeletionPorts$ObjectRef\|loc… right=java/lang/invoke/MethodHandles$Lookup\|java/lang/invoke/MethodHandles\|Lookup\|0x19; local/lito381/server/NativeDeCorpseTimers$Owner\|local/lito381/server/NativeDeCorpseTimers\|Owner\|0x18; local/lito381/server/NativeDeDeletionPorts$ObjectRef\|loc… |
| `local/lito381/server/NativeDeControlCatalog` | class innerClasses: left=java/lang/invoke/MethodHandles$Lookup\|java/lang/invoke/MethodHandles\|Lookup\|0x19; local/lito381/server/NativeDeControlCatalog$Abilities\|local/lito381/server/NativeDeControlCatalog\|Abilities\|0x18; local/lito381/server/NativeDeControlCatalog$… right=java/lang/invoke/MethodHandles$Lookup\|java/lang/invoke/MethodHandles\|Lookup\|0x19; local/lito381/server/NativeDeControlCatalog$Abilities\|local/lito381/server/NativeDeControlCatalog\|Abilities\|0x18; local/lito381/server/NativeDeControlCatalog$… |
| `local/lito381/server/NativeDeControlCatalog$Names` | class innerClasses: left=java/lang/invoke/MethodHandles$Lookup\|java/lang/invoke/MethodHandles\|Lookup\|0x19; local/lito381/server/NativeDeControlCatalog$Names\|local/lito381/server/NativeDeControlCatalog\|Names\|0x18; local/lito381/server/NativeDeInitializerKernel$Name\|… right=java/lang/invoke/MethodHandles$Lookup\|java/lang/invoke/MethodHandles\|Lookup\|0x19; local/lito381/server/NativeDeControlCatalog$Names\|local/lito381/server/NativeDeControlCatalog\|Names\|0x18; local/lito381/server/NativeDeInitializerKernel$Name\|… |
| `local/lito381/server/NativeDeControlCatalog$Reporter` | class innerClasses: left=local/lito381/server/NativeDeControlCatalog$Reporter\|local/lito381/server/NativeDeControlCatalog\|Reporter\|0x608; local/lito381/server/NativeDeInitializerKernel$Name\|local/lito381/server/NativeDeInitializerKernel\|Name\|0x8 right=local/lito381/server/NativeDeControlCatalog$Reporter\|local/lito381/server/NativeDeControlCatalog\|Reporter\|0x608; local/lito381/server/NativeDeInitializerKernel$Name\|local/lito381/server/NativeDeInitializerKernel\|Name\|0x18 |
| `local/lito381/server/NativeDeCorpseCore` | method run()V: left=TRY L3 L4 L12 java/lang/RuntimeException   right=TRY L3 L4 L7 java/lang/RuntimeException   |
| `local/lito381/server/NativeDeCorpseCore$Task` | method run()V insn[43]: left=INVOKESTATIC java/lang/Short.valueOf(S)Ljava/lang/Short; right=INVOKESTATIC java/lang/Integer.valueOf(I)Ljava/lang/Integer; |
| `local/lito381/server/NativeDeCorpseTimers` | method of(Llocal/lito381/server/NativePlayerLifetime;Llocal/lito381/server/NativeDeCorpseTimers$Deletion;Ljava/util/function/LongSupplier;)Llocal/lito381/server/NativeDeCorpseTimers$Owner; insn[4]: left=ASTORE 5 right=DUP |
| `local/lito381/server/NativeDeCorpseTimers$Job` | method run()V insn[2]: left=DUP right=ASTORE 1 |
| `local/lito381/server/NativeDeCorpseTimers$Owner` | method <init>(Llocal/lito381/server/NativePlayerLifetime;Llocal/lito381/server/NativeDeCorpseTimers$Deletion;Ljava/util/function/LongSupplier;)V insn[34]: left=ALOAD 2 right=ALOAD 1 |
| `local/lito381/server/NativeDeCorpseTimers$Owner$1` | field val$deletion:Llocal/lito381/server/NativeDeCorpseTimers$Deletion;: only left |
| `local/lito381/server/NativeDeCorpseTimers$Ticket` | method close(Z)V insn[3]: left=ASTORE 5 right=DUP |
| `local/lito381/server/NativeDeDeletionDeState` | class innerClasses: left=java/lang/invoke/MethodHandles$Lookup\|java/lang/invoke/MethodHandles\|Lookup\|0x19; local/lito381/server/NativeDeDeletionPorts$DeWorld\|local/lito381/server/NativeDeDeletionPorts\|DeWorld\|0x408; local/lito381/server/NativeDeDeletionPorts$Invent… right=java/lang/invoke/MethodHandles$Lookup\|java/lang/invoke/MethodHandles\|Lookup\|0x19; local/lito381/server/NativeDeDeletionPorts$ObjectRef\|local/lito381/server/NativeDeDeletionPorts\|ObjectRef\|0x608; local/lito381/server/NativeDeDeletionPorts$Se… |
| `local/lito381/server/NativeDeDeletionOwner` | method delete(Llocal/lito381/server/NativeDeCorpseState;)V insn[2]: left=IFNE L0 right=IFEQ L0 |
| `local/lito381/server/NativeDeDeletionPorts$Services` | class innerClasses: left=local/lito381/server/NativeDeDeletionPorts$Boss\|local/lito381/server/NativeDeDeletionPorts\|Boss\|0x408; local/lito381/server/NativeDeDeletionPorts$DeWorld\|local/lito381/server/NativeDeDeletionPorts\|DeWorld\|0x408; local/lito381/server/NativeD… right=local/lito381/server/NativeDeDeletionPorts$Boss\|local/lito381/server/NativeDeDeletionPorts\|Boss\|0x408; local/lito381/server/NativeDeDeletionPorts$DeWorld\|local/lito381/server/NativeDeDeletionPorts\|DeWorld\|0x408; local/lito381/server/NativeD… |
| `local/lito381/server/NativeDeDeletionScope` | method call(Llocal/lito381/server/NativeDeDeletionOwner;Ljava/util/function/Supplier;)Ljava/lang/Object; insn[22]: left=ALOAD 3 right=LABEL L2 |
| `local/lito381/server/NativeDeDeletionState` | class innerClasses: left=java/lang/invoke/MethodHandles$Lookup\|java/lang/invoke/MethodHandles\|Lookup\|0x19; local/lito381/server/NativeDeCharacterFields$Snapshot\|local/lito381/server/NativeDeCharacterFields\|Snapshot\|0x18; local/lito381/server/NativeDeCorpseTimers$Ow… right=java/lang/invoke/MethodHandles$Lookup\|java/lang/invoke/MethodHandles\|Lookup\|0x19; local/lito381/server/NativeDeCharacterFields$Snapshot\|local/lito381/server/NativeDeCharacterFields\|Snapshot\|0x18; local/lito381/server/NativeDeCorpseTimers$Ow… |
| `local/lito381/server/NativeDeDeletionVisibility` | method classifyRepair(Llocal/lito381/server/NativeDeDeletionVisibility$Snapshot;ZJ)Llocal/lito381/server/NativeDeDeletionVisibility$RepairKind; insn[5]: left=GETSTATIC local/lito381/server/NativeDeDeletionVisibility$1.$SwitchMap$local$lito381$server$NativeDeDeletionVisibility$Phase:[I right=GETSTATIC local/lito381/server/NativeDeDeletionVisibility$1.$SwitchMap$com$lineage$server$model$Instance$NpcDeletionVisibilityState$Phase:[I |
| `local/lito381/server/NativeDeDeletionVisibility$1` | field $SwitchMap$com$lineage$server$model$Instance$NpcDeletionVisibilityState$Phase:[I: only right |
| `local/lito381/server/NativeDeDeletionVisibility$Snapshot` | method deletionInProgress()Z insn[0]: left=GETSTATIC local/lito381/server/NativeDeDeletionVisibility$1.$SwitchMap$local$lito381$server$NativeDeDeletionVisibility$Phase:[I right=GETSTATIC local/lito381/server/NativeDeDeletionVisibility$1.$SwitchMap$com$lineage$server$model$Instance$NpcDeletionVisibilityState$Phase:[I |
| `local/lito381/server/NativeDeEquipment` | method bind(Llocal/lito381/server/BattleTimedStatus;Llocal/lito381/server/NativeDeActorState$Lease;Llocal/lito381/server/NativeDeEquipment$Values;)Llocal/lito381/server/NativeDeEquipment$Lease; insn[4]: left=ASTORE 3 right=DUP |
| `local/lito381/server/NativeDeEquipment$1` | method <clinit>()V insn[6]: left=GETSTATIC local/lito381/server/NativeDeEquipment$Op.APPLY_WEAPON:Llocal/lito381/server/NativeDeEquipment$Op; right=GETSTATIC local/lito381/server/NativeDeEquipment$Op.TEMP_GFX:Llocal/lito381/server/NativeDeEquipment$Op; |
| `local/lito381/server/NativeDeEquipment$Holder` | method set(Llocal/lito381/server/NativeDeEquipment$Op;I)V insn[4]: left=TABLESWITCH min=3 max=10 default=L8 labels=L0,L1,L2,L3,L4,L5,L6,L7 right=TABLESWITCH min=1 max=8 default=L8 labels=L0,L1,L2,L3,L4,L5,L6,L7 |
| `local/lito381/server/NativeDeFactoryControls` | class innerClasses: left=java/lang/System$Logger\|java/lang/System\|Logger\|0x609; java/lang/invoke/MethodHandles$Lookup\|java/lang/invoke/MethodHandles\|Lookup\|0x19; local/lito381/server/NativeDeControlCatalog$Abilities\|local/lito381/server/NativeDeControlCatalog\|Abili… right=java/lang/System$Logger\|java/lang/System\|Logger\|0x609; java/lang/invoke/MethodHandles$Lookup\|java/lang/invoke/MethodHandles\|Lookup\|0x19; local/lito381/server/NativeDeControlCatalog$Abilities\|local/lito381/server/NativeDeControlCatalog\|Abili… |
| `local/lito381/server/NativeDeInitializerKernel` | method init(Llocal/lito381/server/NativeDeInitializerKernel$Services;Llocal/lito381/server/NativeDeInitializerKernel$Actor;Llocal/lito381/server/NativeDeInitializerKernel$Name;ZLjava/lang/String;)V: left=TRY L3 L4 L7 java/lang/NumberFormatException   right=TRY L3 L4 L5 java/lang/NumberFormatException   |
| `local/lito381/server/NativeDeInitializerKernel$Actor` | class innerClasses: left=local/lito381/server/NativeDeInitializerKernel$Actor\|local/lito381/server/NativeDeInitializerKernel\|Actor\|0x608; local/lito381/server/NativeDeInitializerKernel$Emblem\|local/lito381/server/NativeDeInitializerKernel\|Emblem\|0x18; local/lito381… right=local/lito381/server/NativeDeInitializerKernel$Actor\|local/lito381/server/NativeDeInitializerKernel\|Actor\|0x608; local/lito381/server/NativeDeInitializerKernel$Emblem\|local/lito381/server/NativeDeInitializerKernel\|Emblem\|0x18; local/lito381… |
| `local/lito381/server/NativeDeInitializerKernel$Services` | class innerClasses: left=local/lito381/server/NativeDeInitializerKernel$Ability\|local/lito381/server/NativeDeInitializerKernel\|Ability\|0x18; local/lito381/server/NativeDeInitializerKernel$Actor\|local/lito381/server/NativeDeInitializerKernel\|Actor\|0x608; local/lito3… right=local/lito381/server/NativeDeInitializerKernel$Ability\|local/lito381/server/NativeDeInitializerKernel\|Ability\|0x18; local/lito381/server/NativeDeInitializerKernel$Actor\|local/lito381/server/NativeDeInitializerKernel\|Actor\|0x608; local/lito3… |
| `local/lito381/server/NativeDeInstanceBroadcast` | class innerClasses: left=java/lang/invoke/MethodHandles$Lookup\|java/lang/invoke/MethodHandles\|Lookup\|0x19; local/lito381/server/NativeDeDeletionPorts$Viewer\|local/lito381/server/NativeDeDeletionPorts\|Viewer\|0x408; local/lito381/server/NativeDeInitializerKernel$Log\|… right=java/lang/invoke/MethodHandles$Lookup\|java/lang/invoke/MethodHandles\|Lookup\|0x19 |
| `local/lito381/server/NativeDeInstanceName` | class innerClasses: left=local/lito381/server/NativeDeInitializerKernel$Name\|local/lito381/server/NativeDeInitializerKernel\|Name\|0x8 right= |
| `local/lito381/server/NativeDeInstanceNames` | method close(Llocal/lito381/server/NativeDeWorldContext;)V insn[1]: left=ASTORE 1 right=DUP |
| `local/lito381/server/NativeDeInstanceScene` | method publish(Llocal/lito381/server/NativeDeInstanceState;Llocal/lito381/server/NativeDeInitializerKernel$Packet;)V insn[5]: left=IF_ACMPNE L1 right=IF_ACMPNE L0 |
| `local/lito381/server/NativeDeInstanceScope$World` | method forEachNearbyPlayer(Llocal/lito381/server/NativeDeObjectFields;ILlocal/lito381/server/NativeDeWorldPlayerFilter;Ljava/util/function/Consumer;)V insn[1]: left=ASTORE 6 right=ASTORE 5 |
| `local/lito381/server/NativeDeInstanceState` | class innerClasses: left=java/lang/invoke/MethodHandles$Lookup\|java/lang/invoke/MethodHandles\|Lookup\|0x19; local/lito381/server/NativeDeDeletionPorts$ObjectRef\|local/lito381/server/NativeDeDeletionPorts\|ObjectRef\|0x608; local/lito381/server/NativeDeDeletionPorts$Pa… right=local/lito381/server/NativeDeInstanceState$DeAiWorkerTask\|local/lito381/server/NativeDeInstanceState\|DeAiWorkerTask\|0x12; local/lito381/server/NativeDeInstanceState$DeRepathThrottle\|local/lito381/server/NativeDeInstanceState\|DeRepathThrottl… |
| `local/lito381/server/NativeDeInstanceState$DeAiWorkerTask` | class innerClasses: left=local/lito381/server/NativeDeInstanceState$DeAiWorkerTask\|local/lito381/server/NativeDeInstanceState\|DeAiWorkerTask\|0x12; local/lito381/server/NativeDeLiveAiScope$Pool$ShutdownDiscardable\|local/lito381/server/NativeDeLiveAiScope$Pool\|Shutdo… right=local/lito381/server/NativeDeInstanceState$DeAiWorkerTask\|local/lito381/server/NativeDeInstanceState\|DeAiWorkerTask\|0x12; local/lito381/server/NativeDeLiveAiScope$Pool$ShutdownDiscardable\|local/lito381/server/NativeDeLiveAiScope$Pool\|Shutdo… |
| `local/lito381/server/NativeDeItemAbility` | class innerClasses: left=java/lang/invoke/MethodHandles$Lookup\|java/lang/invoke/MethodHandles\|Lookup\|0x19; local/lito381/server/NativeDeItemAbility$AbilityType\|local/lito381/server/NativeDeItemAbility\|AbilityType\|0x4019; local/lito381/server/NativeDeItemMetadata$Lo… right=java/lang/invoke/MethodHandles$Lookup\|java/lang/invoke/MethodHandles\|Lookup\|0x19; local/lito381/server/NativeDeItemAbility$AbilityType\|local/lito381/server/NativeDeItemAbility\|AbilityType\|0x4019 |
| `local/lito381/server/NativeDeItemApplication` | method install(Llocal/lito381/server/GameHub;Llocal/lito381/server/NativeDeItemTemplatesResources$Snapshot;Llocal/lito381/server/NativeDeItemDefinitionsResources$Snapshot;)Llocal/lito381/server/NativeDeItemApplication;: left=TRY L12 L15 L16    right=TRY L11 L14 L15    |
| `local/lito381/server/NativeDeItemApplication$Owner` | method adopt(Llocal/lito381/server/RuntimeInventory;)V insn[32]: left=ASTORE 3 right=DUP |
| `local/lito381/server/NativeDeItemBlessStatsTable` | class innerClasses: left=java/lang/invoke/MethodHandles$Lookup\|java/lang/invoke/MethodHandles\|Lookup\|0x19; local/lito381/server/NativeDeItemAbility$AbilityType\|local/lito381/server/NativeDeItemAbility\|AbilityType\|0x4019; local/lito381/server/NativeDeItemDefinitions… right=java/lang/invoke/MethodHandles$Lookup\|java/lang/invoke/MethodHandles\|Lookup\|0x19; local/lito381/server/NativeDeItemAbility$AbilityType\|local/lito381/server/NativeDeItemAbility\|AbilityType\|0x4019 |
| `local/lito381/server/NativeDeItemCommonEnchantTable` | class innerClasses: left=java/lang/invoke/MethodHandles$Lookup\|java/lang/invoke/MethodHandles\|Lookup\|0x19; local/lito381/server/NativeDeItemAbility$AbilityType\|local/lito381/server/NativeDeItemAbility\|AbilityType\|0x4019; local/lito381/server/NativeDeItemDefinitions… right=java/lang/invoke/MethodHandles$Lookup\|java/lang/invoke/MethodHandles\|Lookup\|0x19; local/lito381/server/NativeDeItemAbility$AbilityType\|local/lito381/server/NativeDeItemAbility\|AbilityType\|0x4019 |
| `local/lito381/server/NativeDeItemDefinitionsResources` | method freeze(Llocal/lito381/server/NativeDeItemDefinitionsScope$Data;)V: left=TRY L0 L7 L8 java/lang/ReflectiveOperationException   right=TRY L0 L6 L7 java/lang/ReflectiveOperationException   |
| `local/lito381/server/NativeDeItemDefinitionsResources$Owner` | method reload(Ljava/lang/String;Llocal/lito381/server/NativeDeTerrainScope$Sql;Llocal/lito381/server/NativeDeWorldLog;)Llocal/lito381/server/NativeDeItemDefinitionsResources$Snapshot; insn[0]: left=ALOAD 0 right=ALOAD 1 |
| `local/lito381/server/NativeDeItemDefinitionsScope` | method run(Llocal/lito381/server/NativeDeItemDefinitionsScope$Context;Ljava/util/function/Supplier;)Ljava/lang/Object; insn[11]: left=ALOAD 3 right=LABEL L1 |
| `local/lito381/server/NativeDeItemDefinitionsSql` | method finish()V: left=TRY L4 L5 L6 java/lang/Throwable   right=TRY L3 L4 L5 java/lang/Throwable   |
| `local/lito381/server/NativeDeItemDefinitionsSql$Lease` | method call(Ljava/lang/Object;Ljava/lang/reflect/Method;[Ljava/lang/Object;)Ljava/lang/Object; insn[12]: left=ASTORE 5 right=ASTORE 4 |
| `local/lito381/server/NativeDeItemEnchantBonusTable` | class innerClasses: left=java/lang/invoke/MethodHandles$Lookup\|java/lang/invoke/MethodHandles\|Lookup\|0x19; local/lito381/server/NativeDeItemAbility$AbilityType\|local/lito381/server/NativeDeItemAbility\|AbilityType\|0x4019; local/lito381/server/NativeDeItemDefinitions… right=java/lang/invoke/MethodHandles$Lookup\|java/lang/invoke/MethodHandles\|Lookup\|0x19; local/lito381/server/NativeDeItemAbility$AbilityType\|local/lito381/server/NativeDeItemAbility\|AbilityType\|0x4019 |
| `local/lito381/server/NativeDeItemEquipProc` | method addZeroToInclusive(IILjava/util/Random;)I: only left |
| `local/lito381/server/NativeDeItemEquipProcTable` | class innerClasses: left=java/lang/invoke/MethodHandles$Lookup\|java/lang/invoke/MethodHandles\|Lookup\|0x19; local/lito381/server/NativeDeItemDefinitionsScope$Database\|local/lito381/server/NativeDeItemDefinitionsScope\|Database\|0x18; local/lito381/server/NativeDeItemD… right=java/lang/invoke/MethodHandles$Lookup\|java/lang/invoke/MethodHandles\|Lookup\|0x19 |
| `local/lito381/server/NativeDeItemGreaterTierTable` | class innerClasses: left=java/lang/invoke/MethodHandles$Lookup\|java/lang/invoke/MethodHandles\|Lookup\|0x19; local/lito381/server/NativeDeItemAbility$AbilityType\|local/lito381/server/NativeDeItemAbility\|AbilityType\|0x4019; local/lito381/server/NativeDeItemDefinitions… right=java/lang/invoke/MethodHandles$Lookup\|java/lang/invoke/MethodHandles\|Lookup\|0x19; local/lito381/server/NativeDeItemAbility$AbilityType\|local/lito381/server/NativeDeItemAbility\|AbilityType\|0x4019; local/lito381/server/NativeDeItemGreaterTier… |
| `local/lito381/server/NativeDeItemIdStatsTable` | class innerClasses: left=java/lang/invoke/MethodHandles$Lookup\|java/lang/invoke/MethodHandles\|Lookup\|0x19; local/lito381/server/NativeDeItemAbility$AbilityType\|local/lito381/server/NativeDeItemAbility\|AbilityType\|0x4019; local/lito381/server/NativeDeItemDefinitions… right=java/lang/invoke/MethodHandles$Lookup\|java/lang/invoke/MethodHandles\|Lookup\|0x19; local/lito381/server/NativeDeItemAbility$AbilityType\|local/lito381/server/NativeDeItemAbility\|AbilityType\|0x4019 |
| `local/lito381/server/NativeDeItemInlayCardTable` | class innerClasses: left=java/lang/invoke/MethodHandles$Lookup\|java/lang/invoke/MethodHandles\|Lookup\|0x19; local/lito381/server/NativeDeItemDefinitionsScope$Database\|local/lito381/server/NativeDeItemDefinitionsScope\|Database\|0x18; local/lito381/server/NativeDeItemD… right=java/lang/invoke/MethodHandles$Lookup\|java/lang/invoke/MethodHandles\|Lookup\|0x19 |
| `local/lito381/server/NativeDeItemInventory` | method actor(J)Llocal/lito381/server/NativeDeWorldKinds$Item; insn[2]: left=ASTORE 3 right=DUP |
| `local/lito381/server/NativeDeItemInventory$BoundItem` | method apply(Llocal/lito381/server/RuntimeInventory$Item;Llocal/lito381/server/NativeDeItemTemplate;)V insn[8]: left=DUP right=ISTORE 3 |
| `local/lito381/server/NativeDeItemMagicStoneTable` | class innerClasses: left=java/lang/invoke/MethodHandles$Lookup\|java/lang/invoke/MethodHandles\|Lookup\|0x19; local/lito381/server/NativeDeItemDefinitionsScope$Database\|local/lito381/server/NativeDeItemDefinitionsScope\|Database\|0x18; local/lito381/server/NativeDeItemD… right=java/lang/invoke/MethodHandles$Lookup\|java/lang/invoke/MethodHandles\|Lookup\|0x19 |
| `local/lito381/server/NativeDeItemPublication` | class innerClasses: left=java/lang/invoke/MethodHandles$Lookup\|java/lang/invoke/MethodHandles\|Lookup\|0x19; local/lito381/server/NativeDeItemPublication$Ticket\|local/lito381/server/NativeDeItemPublication\|Ticket\|0x1a; local/lito381/server/NativeDeItemTransaction$Cha… right=local/lito381/server/NativeDeItemPublication$Ticket\|local/lito381/server/NativeDeItemPublication\|Ticket\|0x1a; local/lito381/server/NativeDeItemTransaction$Change\|local/lito381/server/NativeDeItemTransaction\|Change\|0x18 |
| `local/lito381/server/NativeDeItemStateCopy` | method copy(Llocal/lito381/server/NativeDeWorldKinds$Item;Llocal/lito381/server/NativeDeWorldKinds$Item;)V insn[74]: left=IFGE L7 right=IFLT L8 |
| `local/lito381/server/NativeDeItemTable` | class innerClasses: left=java/lang/invoke/MethodHandles$Lookup\|java/lang/invoke/MethodHandles\|Lookup\|0x19; local/lito381/server/NativeDeItemTemplatesScope$Classes\|local/lito381/server/NativeDeItemTemplatesScope\|Classes\|0x18; local/lito381/server/NativeDeItemTemplat… right=com/lineage/server/model/item/ItemGenerationContext$Source\|com/lineage/server/model/item/ItemGenerationContext\|Source\|0x4019; java/lang/invoke/MethodHandles$Lookup\|java/lang/invoke/MethodHandles\|Lookup\|0x19 |
| `local/lito381/server/NativeDeItemTemplatesResources` | method freeze(Llocal/lito381/server/NativeDeItemTable;)I insn[4]: left=ASTORE 2 right=ASTORE 1 |
| `local/lito381/server/NativeDeItemTemplatesResources$Owner` | method reload(Ljava/lang/String;Llocal/lito381/server/NativeDeTerrainScope$Sql;Llocal/lito381/server/NativeDeWorldLog;)Llocal/lito381/server/NativeDeItemTemplatesResources$Snapshot; insn[0]: left=ALOAD 0 right=ALOAD 1 |
| `local/lito381/server/NativeDeItemTemplatesScope` | method run(Llocal/lito381/server/NativeDeItemTemplatesScope$Context;Ljava/util/function/Supplier;)Ljava/lang/Object; insn[11]: left=ALOAD 3 right=LABEL L1 |
| `local/lito381/server/NativeDeItemTemplatesScope$Classes` | method addList(ILjava/lang/String;I)V insn[7]: left=ASTORE 5 right=ASTORE 4 |
| `local/lito381/server/NativeDeItemTransaction` | method nativeCall(Llocal/lito381/server/ClientSession;Llocal/lito381/server/NativeDeItemTransaction$Operation;)Ljava/lang/Object;: length left=2 right=1 |
| `local/lito381/server/NativeDeItemWeaponAttrTable` | class innerClasses: left=java/lang/invoke/MethodHandles$Lookup\|java/lang/invoke/MethodHandles\|Lookup\|0x19; local/lito381/server/NativeDeItemDefinitionsScope$Database\|local/lito381/server/NativeDeItemDefinitionsScope\|Database\|0x18; local/lito381/server/NativeDeItemD… right=java/lang/invoke/MethodHandles$Lookup\|java/lang/invoke/MethodHandles\|Lookup\|0x19 |
| `local/lito381/server/NativeDeItemWeaponProc` | method addOneToInclusive(IILjava/util/Random;)I: only left |
| `local/lito381/server/NativeDeItemWeaponProcTable` | class innerClasses: left=java/lang/invoke/MethodHandles$Lookup\|java/lang/invoke/MethodHandles\|Lookup\|0x19; local/lito381/server/NativeDeItemDefinitionsScope$Database\|local/lito381/server/NativeDeItemDefinitionsScope\|Database\|0x18; local/lito381/server/NativeDeItemD… right=java/lang/invoke/MethodHandles$Lookup\|java/lang/invoke/MethodHandles\|Lookup\|0x19 |
| `local/lito381/server/NativeDeKnownState` | class innerClasses: left=local/lito381/server/NativeDeDeletionPorts$Viewer\|local/lito381/server/NativeDeDeletionPorts\|Viewer\|0x408 right=local/lito381/server/NativeDeDeletionPorts$ObjectRef\|local/lito381/server/NativeDeDeletionPorts\|ObjectRef\|0x608; local/lito381/server/NativeDeDeletionPorts$Viewer\|local/lito381/server/NativeDeDeletionPorts\|Viewer\|0x408 |
| `local/lito381/server/NativeDeLiveAiJobs` | method background(Llocal/lito381/server/NativeDeInstanceState;Z)V: left=TRY L5 L6 L7 java/lang/Error   right=TRY L5 L6 L7 java/lang/RuntimeException   |
| `local/lito381/server/NativeDeLiveAiJobs$Credit` | method release()V insn[4]: left=ASTORE 1 right=DUP |
| `local/lito381/server/NativeDeLiveAiJobs$Ticket` | method cancel(Z)Z insn[1]: left=ASTORE 2 right=DUP |
| `local/lito381/server/NativeDeLiveAiPorts$Config` | method load()Llocal/lito381/server/NativeDeLiveAiPorts$Config;: left=TRY L0 L2 L4 java/lang/Throwable   right=TRY L1 L3 L5 java/lang/Throwable   |
| `local/lito381/server/NativeDeLiveAiScope` | method call(Llocal/lito381/server/NativeDeInstanceState;Ljava/util/function/Supplier;)Ljava/lang/Object; insn[14]: left=ALOAD 3 right=LABEL L1 |
| `local/lito381/server/NativeDeNpcConstructionState` | class access: left=0x21 right=0x1 |
| `local/lito381/server/NativeDeNpcExecutor` | class innerClasses: left=local/lito381/server/NativeDeDeletionPorts$Viewer\|local/lito381/server/NativeDeDeletionPorts\|Viewer\|0x408 right= |
| `local/lito381/server/NativeDeNpcHateList` | class innerClasses: left=java/lang/invoke/MethodHandles$Lookup\|java/lang/invoke/MethodHandles\|Lookup\|0x19; java/util/Map$Entry\|java/util/Map\|Entry\|0x609; local/lito381/server/NativeDeDeletionPorts$Hate\|local/lito381/server/NativeDeDeletionPorts\|Hate\|0x608; local/li… right=java/lang/invoke/MethodHandles$Lookup\|java/lang/invoke/MethodHandles\|Lookup\|0x19; java/util/Map$Entry\|java/util/Map\|Entry\|0x609 |
| `local/lito381/server/NativeDeNpcInventory` | class innerClasses: left=java/lang/invoke/MethodHandles$Lookup\|java/lang/invoke/MethodHandles\|Lookup\|0x19; local/lito381/server/NativeDeDeletionPorts$Inventory\|local/lito381/server/NativeDeDeletionPorts\|Inventory\|0x608; local/lito381/server/NativeDeNpcConstructionP… right=java/lang/invoke/MethodHandles$Lookup\|java/lang/invoke/MethodHandles\|Lookup\|0x19; local/lito381/server/NativeDeDeletionPorts$Inventory\|local/lito381/server/NativeDeDeletionPorts\|Inventory\|0x608; local/lito381/server/NativeDeNpcConstructionP… |
| `local/lito381/server/NativeDeNpcKnowledge` | method admitted(Llocal/lito381/server/NativeDeDeletionState;Llocal/lito381/server/NativeDeDeletionPorts$ObjectRef;)Z insn[4]: left=ASTORE 3 right=ASTORE 2 |
| `local/lito381/server/NativeDeNpcMobSkill` | method isTriggerDistance(II)Z insn[18]: left=GOTO L3 right=IRETURN |
| `local/lito381/server/NativeDeNpcMobSkillUse` | class innerClasses: left=local/lito381/server/NativeDeNpcTemplatePorts$MobSkills\|local/lito381/server/NativeDeNpcTemplatePorts\|MobSkills\|0x18 right= |
| `local/lito381/server/NativeDeNpcMoveState` | class innerClasses: left=local/lito381/server/NativeDeNpcConstructionPorts$Movement\|local/lito381/server/NativeDeNpcConstructionPorts\|Movement\|0x608; local/lito381/server/NativeDeNpcConstructionPorts$Services\|local/lito381/server/NativeDeNpcConstructionPorts\|Servic… right=local/lito381/server/NativeDeNpcMoveState$PathFailure\|local/lito381/server/NativeDeNpcMoveState\|PathFailure\|0x4019; local/lito381/server/NativeDeNpcMoveState$PathState\|local/lito381/server/NativeDeNpcMoveState\|PathState\|0x1a |
| `local/lito381/server/NativeDeNpcTemplate` | class innerClasses: left=java/lang/invoke/MethodHandles$Lookup\|java/lang/invoke/MethodHandles\|Lookup\|0x19; local/lito381/server/NativeDeDeletionPorts$Template\|local/lito381/server/NativeDeDeletionPorts\|Template\|0x608 right=java/lang/invoke/MethodHandles$Lookup\|java/lang/invoke/MethodHandles\|Lookup\|0x19 |
| `local/lito381/server/NativeDeNpcTemplateLoadScope` | method call(Llocal/lito381/server/NativeDeNpcTemplateLoadScope$Context;Ljava/util/function/Supplier;)Ljava/lang/Object;: length left=2 right=1 |
| `local/lito381/server/NativeDeNpcTemplateMobLoader` | class innerClasses: left=java/lang/invoke/MethodHandles$Lookup\|java/lang/invoke/MethodHandles\|Lookup\|0x19; local/lito381/server/NativeDeNpcTemplateLoadScope$Database\|local/lito381/server/NativeDeNpcTemplateLoadScope\|Database\|0x18; local/lito381/server/NativeDeNpcTe… right= |
| `local/lito381/server/NativeDeNpcTemplateNpcLoader` | class innerClasses: left=java/lang/invoke/MethodHandles$Lookup\|java/lang/invoke/MethodHandles\|Lookup\|0x19; local/lito381/server/NativeDeNpcTemplateLoadScope$Context\|local/lito381/server/NativeDeNpcTemplateLoadScope\|Context\|0x18; local/lito381/server/NativeDeNpcTemp… right=local/lito381/server/NativeDeNpcTemplateLoadScope$Context\|local/lito381/server/NativeDeNpcTemplateLoadScope\|Context\|0x18 |
| `local/lito381/server/NativeDeNpcTemplateResources` | method lambda$owned$#0(Ljava/util/concurrent/atomic/AtomicBoolean;Ljava/sql/Connection;Llocal/lito381/server/NativeDeNpcTemplateResources$Counts;Ljava/lang/Object;Ljava/lang/reflect/Method;[Ljava/lang/Object;)Ljava/lang/Object; insn[14]: left=ASTORE 8 right=ASTORE 6 |
| `local/lito381/server/NativeDeNpcTemplateState` | class innerClasses: left=java/lang/invoke/MethodHandles$Lookup\|java/lang/invoke/MethodHandles\|Lookup\|0x19; local/lito381/server/NativeDeDeletionPorts$Template\|local/lito381/server/NativeDeDeletionPorts\|Template\|0x608; local/lito381/server/NativeDeNpcTemplatePorts$S… right=java/lang/invoke/MethodHandles$Lookup\|java/lang/invoke/MethodHandles\|Lookup\|0x19; local/lito381/server/NativeDeDeletionPorts$Template\|local/lito381/server/NativeDeDeletionPorts\|Template\|0x608; local/lito381/server/NativeDeNpcTemplatePorts$S… |
| `local/lito381/server/NativeDeNpcVitalState` | method getX()I: left=0x1 right=0x4 |
| `local/lito381/server/NativeDeNpcVitalState$1` | method <clinit>()V: length left=5 right=7 |
| `local/lito381/server/NativeDeObjectBindings` | method showChanged(Llocal/lito381/server/NativeDeObjectFields;I)V insn[4]: left=DUP right=CHECKCAST local/lito381/server/NativeDeObjectFields |
| `local/lito381/server/NativeDeObjectFields` | class innerClasses: left=java/lang/invoke/MethodHandles$Lookup\|java/lang/invoke/MethodHandles\|Lookup\|0x19; local/lito381/server/NativeDeDeletionPorts$ObjectRef\|local/lito381/server/NativeDeDeletionPorts\|ObjectRef\|0x608; local/lito381/server/NativeDeDeletionPorts$Vi… right=java/lang/invoke/MethodHandles$Lookup\|java/lang/invoke/MethodHandles\|Lookup\|0x19; local/lito381/server/NativeDeDeletionPorts$ObjectRef\|local/lito381/server/NativeDeDeletionPorts\|ObjectRef\|0x608; local/lito381/server/NativeDeDeletionPorts$Vi… |
| `local/lito381/server/NativeDeObjectScope` | method context(Llocal/lito381/server/NativeDeObjectFields;)Llocal/lito381/server/NativeDeWorldContext; insn[4]: left=ASTORE 2 right=CHECKCAST local/lito381/server/NativeDeObjectFields |
| `local/lito381/server/NativeDeObjectScope$World` | method updateObjectSector(Llocal/lito381/server/NativeDeObjectFields;IIIIII)V insn[2]: left=IFNE L0 right=IFEQ L0 |
| `local/lito381/server/NativeDePerceptionAccess` | method Pc_tryAddKnownObject(Llocal/lito381/server/NativeDeDeletionPorts$Viewer;Llocal/lito381/server/NativeDeDeletionPorts$ObjectRef;)Z insn[2]: left=IFNE L0 right=IFEQ L0 |
| `local/lito381/server/NativeDePerceptionBadText` | class innerClasses: left=java/lang/invoke/MethodHandles$Lookup\|java/lang/invoke/MethodHandles\|Lookup\|0x19; local/lito381/server/NativeDePerceptionResource$File\|local/lito381/server/NativeDePerceptionResource\|File\|0x18; local/lito381/server/NativeDePerceptionResourc… right=java/lang/invoke/MethodHandles$Lookup\|java/lang/invoke/MethodHandles\|Lookup\|0x19 |
| `local/lito381/server/NativeDePerceptionControls` | method load()Llocal/lito381/server/NativeDePerceptionControls$Data;: left=TRY L1 L3 L4    right=TRY L1 L3 L4 java/lang/Throwable   |
| `local/lito381/server/NativeDePerceptionEmblem` | class innerClasses: left= right=java/lang/invoke/MethodHandles$Lookup\|java/lang/invoke/MethodHandles\|Lookup\|0x19 |
| `local/lito381/server/NativeDePerceptionJobs` | method owner(Llocal/lito381/server/NativeDeWorldContext;)Llocal/lito381/server/NativeDePerceptionJobs$Owner; insn[5]: left=ASTORE 2 right=DUP |
| `local/lito381/server/NativeDePerceptionJobs$Task` | method cancel()V insn[1]: left=ASTORE 1 right=DUP |
| `local/lito381/server/NativeDePerceptionNPCPack_De` | class innerClasses: left=local/lito381/server/NativeDePerceptionPorts$Poison\|local/lito381/server/NativeDePerceptionPorts\|Poison\|0x408; local/lito381/server/NativeDePerceptionSpeed$SpeedState\|local/lito381/server/NativeDePerceptionSpeed\|SpeedState\|0x19 right=local/lito381/server/NativeDePerceptionSpeed$SpeedState\|local/lito381/server/NativeDePerceptionSpeed\|SpeedState\|0x19 |
| `local/lito381/server/NativeDePerceptionOutput` | method send(Llocal/lito381/server/NativeDeClientViewer;Llocal/lito381/server/NativeDePerceptionPacket;)V insn[69]: left=ASTORE 5 right=DUP |
| `local/lito381/server/NativeDePerceptionPacket` | class innerClasses: left=java/lang/invoke/MethodHandles$Lookup\|java/lang/invoke/MethodHandles\|Lookup\|0x19; local/lito381/server/NativeDeDeletionPorts$Log\|local/lito381/server/NativeDeDeletionPorts\|Log\|0x608; local/lito381/server/NativeDeDeletionPorts$Packet\|local/l… right=com/lineage/server/utils/TeleportTrace$PacketTrace\|com/lineage/server/utils/TeleportTrace\|PacketTrace\|0x19; java/lang/invoke/MethodHandles$Lookup\|java/lang/invoke/MethodHandles\|Lookup\|0x19 |
| `local/lito381/server/NativeDePerceptionScope` | method call(Llocal/lito381/server/NativeDePerceptionState;Ljava/util/function/Supplier;)Ljava/lang/Object; insn[22]: left=ALOAD 3 right=LABEL L2 |
| `local/lito381/server/NativeDePerceptionSpeed` | method calculate(ILlocal/lito381/server/NativeDePerceptionSpeed$ActionType;Llocal/lito381/server/NativeDePerceptionSpeed$SpeedState;)I insn[3]: left=ISTORE 4 right=ISTORE 3 |
| `local/lito381/server/NativeDePerceptionState` | class innerClasses: left=java/lang/invoke/MethodHandles$Lookup\|java/lang/invoke/MethodHandles\|Lookup\|0x19; local/lito381/server/NativeDeDeletionPorts$Log\|local/lito381/server/NativeDeDeletionPorts\|Log\|0x608; local/lito381/server/NativeDeDeletionPorts$ObjectRef\|loca… right=java/lang/invoke/MethodHandles$Lookup\|java/lang/invoke/MethodHandles\|Lookup\|0x19; local/lito381/server/NativeDeDeletionPorts$Services\|local/lito381/server/NativeDeDeletionPorts\|Services\|0x608; local/lito381/server/NativeDeDeletionPorts$View… |
| `local/lito381/server/NativeDeProfileKernel` | method initialize(Llocal/lito381/server/NativeDeProfileKernel$Target;)V insn[62]: left=LABEL L15 right=GOTO L15 |
| `local/lito381/server/NativeDeShopData` | method isInRange(II)Z insn[34]: left=IF_ICMPGT L3 right=IF_ICMPLE L4 |
| `local/lito381/server/NativeDeShopResources` | method lambda$read$#0(Ljava/lang/Object;)I: only left |
| `local/lito381/server/NativeDeShopTable` | class innerClasses: left=java/lang/invoke/MethodHandles$Lookup\|java/lang/invoke/MethodHandles\|Lookup\|0x19; java/util/Map$Entry\|java/util/Map\|Entry\|0x609; local/lito381/server/NativeDeTerrainScope$Database\|local/lito381/server/NativeDeTerrainScope\|Database\|0x18; loc… right=java/lang/invoke/MethodHandles$Lookup\|java/lang/invoke/MethodHandles\|Lookup\|0x19; java/util/Map$Entry\|java/util/Map\|Entry\|0x609 |
| `local/lito381/server/NativeDeSpriteCatalog` | method read(Ljava/sql/Connection;Llocal/lito381/server/NativeDeSpriteCatalog$Catalog;Llocal/lito381/server/NativeDeSpriteCatalog$Reporter;)Llocal/lito381/server/NativeDeSpriteCatalog$Catalog;: left=TRY L2 L12 L13    right=TRY L2 L13 L14 java/lang/Throwable   |
| `local/lito381/server/NativeDeTerrainPoint` | method isInScreen(Llocal/lito381/server/NativeDeTerrainPoint;)Z insn[41]: left=GOTO L3 right=IRETURN |
| `local/lito381/server/NativeDeTerrainResources` | method assets()Ljava/util/Map;: left=TRY L3 L5 L6 java/lang/Throwable   right=TRY L3 L6 L7 java/lang/Throwable   |
| `local/lito381/server/NativeDeTerrainResources$1` | field val$checked:Ljava/security/DigestInputStream;: only right |
| `local/lito381/server/NativeDeTerrainScope` | method call(Llocal/lito381/server/NativeDeTerrainScope$Context;Ljava/util/function/Supplier;)Ljava/lang/Object;: left=TRY L3 L4 L7 java/lang/Throwable   right=TRY L3 L4 L5 java/lang/Throwable   |
| `local/lito381/server/NativeDeTerrainTable` | class innerClasses: left=java/lang/invoke/MethodHandles$Lookup\|java/lang/invoke/MethodHandles\|Lookup\|0x19; local/lito381/server/NativeDeTerrainScope$Database\|local/lito381/server/NativeDeTerrainScope\|Database\|0x18; local/lito381/server/NativeDeTerrainScope$Logs\|loc… right=java/lang/invoke/MethodHandles$Lookup\|java/lang/invoke/MethodHandles\|Lookup\|0x19 |
| `local/lito381/server/NativeDeTerrainText` | class innerClasses: left=java/lang/invoke/MethodHandles$Lookup\|java/lang/invoke/MethodHandles\|Lookup\|0x19; local/lito381/server/NativeDeTerrainScope$Logs\|local/lito381/server/NativeDeTerrainScope\|Logs\|0x18 right=java/lang/invoke/MethodHandles$Lookup\|java/lang/invoke/MethodHandles\|Lookup\|0x19 |
| `local/lito381/server/NativeDeTerrainV1` | class innerClasses: left=java/lang/invoke/MethodHandles$Lookup\|java/lang/invoke/MethodHandles\|Lookup\|0x19; local/lito381/server/NativeDeTerrainScope$Logs\|local/lito381/server/NativeDeTerrainScope\|Logs\|0x18 right=java/lang/invoke/MethodHandles$Lookup\|java/lang/invoke/MethodHandles\|Lookup\|0x19 |
| `local/lito381/server/NativeDeTerrainWorld` | method call(Llocal/lito381/server/NativeDeWorldContext;Ljava/util/function/Supplier;)Ljava/lang/Object; insn[12]: left=ALOAD 3 right=LABEL L1 |
| `local/lito381/server/NativeDeVisibilityAccess` | method L1NpcInstance$getNpcId(Llocal/lito381/server/NativeDeDeletionPorts$ObjectRef;)I insn[5]: left=ASTORE 2 right=ASTORE 1 |
| `local/lito381/server/NativeDeVisibilityCheck` | class innerClasses: left=local/lito381/server/NativeDeDeletionPorts$ObjectRef\|local/lito381/server/NativeDeDeletionPorts\|ObjectRef\|0x608; local/lito381/server/NativeDeDeletionPorts$Viewer\|local/lito381/server/NativeDeDeletionPorts\|Viewer\|0x408; local/lito381/server… right= |
| `local/lito381/server/NativeDeVisibilityCoordinator` | class innerClasses: left=java/lang/invoke/MethodHandles$Lookup\|java/lang/invoke/MethodHandles\|Lookup\|0x19; local/lito381/server/NativeDeDeletionPorts$ObjectRef\|local/lito381/server/NativeDeDeletionPorts\|ObjectRef\|0x608; local/lito381/server/NativeDeDeletionPorts$Vi… right=java/lang/invoke/MethodHandles$Lookup\|java/lang/invoke/MethodHandles\|Lookup\|0x19 |
| `local/lito381/server/NativeDeVisibilityHp` | class innerClasses: left=local/lito381/server/NativeDeDeletionPorts$ObjectRef\|local/lito381/server/NativeDeDeletionPorts\|ObjectRef\|0x608 right= |
| `local/lito381/server/NativeDeVisibilityJobs` | method schedule(Llocal/lito381/server/NativeDeVisibilityRuntime$Owner;Ljava/lang/Runnable;JJ)Llocal/lito381/server/NativeDeVisibilityJobs$Ticket;: left=TRY L3 L4 L5 java/lang/Error   right=TRY L3 L4 L5 java/lang/RuntimeException   |
| `local/lito381/server/NativeDeVisibilityJobs$Credit` | method release()V insn[5]: left=ASTORE 1 right=DUP |
| `local/lito381/server/NativeDeVisibilityJobs$Dispatch` | method run()V insn[36]: left=ASTORE 1 right=DUP |
| `local/lito381/server/NativeDeVisibilityJobs$Ticket` | method end(ZLjava/lang/Throwable;)Z insn[1]: left=ASTORE 4 right=DUP |
| `local/lito381/server/NativeDeVisibilityOutput` | method accepts(Llocal/lito381/server/NativeDeDeletionPorts$Packet;)Z insn[2]: left=IFEQ L0 right=IFNE L0 |
| `local/lito381/server/NativeDeVisibilityPacket` | class innerClasses: left=java/lang/invoke/MethodHandles$Lookup\|java/lang/invoke/MethodHandles\|Lookup\|0x19; local/lito381/server/NativeDeDeletionPorts$Packet\|local/lito381/server/NativeDeDeletionPorts\|Packet\|0x8 right=com/lineage/server/utils/TeleportTrace$PacketTrace\|com/lineage/server/utils/TeleportTrace\|PacketTrace\|0x19; java/lang/invoke/MethodHandles$Lookup\|java/lang/invoke/MethodHandles\|Lookup\|0x19 |
| `local/lito381/server/NativeDeVisibilityPc` | class nestMembers: left=local/lito381/server/NativeDeVisibilityPc$1 right= |
| `local/lito381/server/NativeDeVisibilityRuntime` | method call(Llocal/lito381/server/NativeDeWorldContext;Ljava/util/function/Supplier;)Ljava/lang/Object; insn[63]: left=DUP right=ISTORE 4 |
| `local/lito381/server/NativeDeVisibilityScope` | method call(Llocal/lito381/server/NativeDeVisibilityRuntime$Owner;Ljava/util/function/Supplier;)Ljava/lang/Object; insn[22]: left=ALOAD 3 right=LABEL L2 |
| `local/lito381/server/NativeDeVisibilityTimer` | class innerClasses: left=java/lang/invoke/MethodHandles$Lookup\|java/lang/invoke/MethodHandles\|Lookup\|0x19; local/lito381/server/NativeDeVisibilityScope$Logs\|local/lito381/server/NativeDeVisibilityScope\|Logs\|0x18 right=java/lang/invoke/MethodHandles$Lookup\|java/lang/invoke/MethodHandles\|Lookup\|0x19 |
| `local/lito381/server/NativeDeWorldContext` | class innerClasses: left=java/lang/System$Logger$Level\|java/lang/System$Logger\|Level\|0x4019; java/lang/System$Logger\|java/lang/System\|Logger\|0x609; java/lang/invoke/MethodHandles$Lookup\|java/lang/invoke/MethodHandles\|Lookup\|0x19; java/util/concurrent/locks/Reentran… right=java/lang/System$Logger$Level\|java/lang/System$Logger\|Level\|0x4019; java/lang/System$Logger\|java/lang/System\|Logger\|0x609; java/lang/invoke/MethodHandles$Lookup\|java/lang/invoke/MethodHandles\|Lookup\|0x19; java/util/concurrent/locks/Reentran… |
| `local/lito381/server/NativeDeWorldContext$Entry` | class innerClasses: left=local/lito381/server/NativeDeDeletionPorts$ObjectRef\|local/lito381/server/NativeDeDeletionPorts\|ObjectRef\|0x608; local/lito381/server/NativeDeWorldContext$Entry\|local/lito381/server/NativeDeWorldContext\|Entry\|0x18 right=local/lito381/server/NativeDeWorldContext$Entry\|local/lito381/server/NativeDeWorldContext\|Entry\|0x18 |
| `local/lito381/server/NativeDeWorldControls` | method load()I: left=TRY L2 L3 L4 java/lang/Throwable   right=TRY L3 L4 L5 java/lang/Throwable   |
| `local/lito381/server/NativeDeWorldCore` | class innerClasses: left=java/lang/invoke/MethodHandles$Lookup\|java/lang/invoke/MethodHandles\|Lookup\|0x19; java/util/Map$Entry\|java/util/Map\|Entry\|0x609; local/lito381/server/NativeDeDeletionPorts$Monster\|local/lito381/server/NativeDeDeletionPorts\|Monster\|0x408; lo… right=java/lang/invoke/MethodHandles$Lookup\|java/lang/invoke/MethodHandles\|Lookup\|0x19; java/util/Map$Entry\|java/util/Map\|Entry\|0x609; local/lito381/server/NativeDeWorldCore$1\|\|\|0x1008; local/lito381/server/NativeDeWorldCore$SpatialIndexAuditSnap… |
| `local/lito381/server/NativeDeWorldCore$1` | field $SwitchMap$com$lineage$server$world$World$SpatialReconcileResult:[I: only right |
| `local/lito381/server/NativeDeWorldCore$VisibleBinding` | class innerClasses: left=java/lang/invoke/MethodHandles$Lookup\|java/lang/invoke/MethodHandles\|Lookup\|0x19; local/lito381/server/NativeDeDeletionPorts$ObjectRef\|local/lito381/server/NativeDeDeletionPorts\|ObjectRef\|0x608; local/lito381/server/NativeDeWorldCore$Visibl… right=java/lang/invoke/MethodHandles$Lookup\|java/lang/invoke/MethodHandles\|Lookup\|0x19; local/lito381/server/NativeDeWorldCore$VisibleBinding\|local/lito381/server/NativeDeWorldCore\|VisibleBinding\|0x1a |
| `local/lito381/server/NativeDeWorldKinds` | class innerClasses: left=local/lito381/server/NativeDeWorldKinds$Bow\|local/lito381/server/NativeDeWorldKinds\|Bow\|0x8; local/lito381/server/NativeDeWorldKinds$Doll\|local/lito381/server/NativeDeWorldKinds\|Doll\|0x8; local/lito381/server/NativeDeWorldKinds$Dwarf\|local/… right=local/lito381/server/NativeDeWorldKinds$Bow\|local/lito381/server/NativeDeWorldKinds\|Bow\|0x8; local/lito381/server/NativeDeWorldKinds$Doll\|local/lito381/server/NativeDeWorldKinds\|Doll\|0x8; local/lito381/server/NativeDeWorldKinds$Dwarf\|local/… |
| `local/lito381/server/NativeDeWorldKinds$Item` | class innerClasses: left=local/lito381/server/NativeDeDeletionPorts$Viewer\|local/lito381/server/NativeDeDeletionPorts\|Viewer\|0x408; local/lito381/server/NativeDeItemAbility$AbilityType\|local/lito381/server/NativeDeItemAbility\|AbilityType\|0x4019; local/lito381/serve… right=local/lito381/server/NativeDeDeletionPorts$ObjectRef\|local/lito381/server/NativeDeDeletionPorts\|ObjectRef\|0x608; local/lito381/server/NativeDeWorldKinds$Item$EquipmentTimerLease\|local/lito381/server/NativeDeWorldKinds$Item\|EquipmentTimerLea… |
| `local/lito381/server/NativeDeWorldKinds$Item$EquipmentTimerLease` | class innerClasses: left=local/lito381/server/NativeDeDeletionPorts$Viewer\|local/lito381/server/NativeDeDeletionPorts\|Viewer\|0x408; local/lito381/server/NativeDeWorldKinds$Item$EquipmentTimerLease\|local/lito381/server/NativeDeWorldKinds$Item\|EquipmentTimerLease\|0x1… right=local/lito381/server/NativeDeWorldKinds$Item$EquipmentTimerLease\|local/lito381/server/NativeDeWorldKinds$Item\|EquipmentTimerLease\|0x1a |
| `local/lito381/server/NativeDeWorldKinds$Item$LastStatus` | class innerClasses: left=local/lito381/server/NativeDeWorldKinds$Item$LastStatus\|local/lito381/server/NativeDeWorldKinds$Item\|LastStatus\|0x1; local/lito381/server/NativeDeWorldKinds$Item\|local/lito381/server/NativeDeWorldKinds\|Item\|0x8 right=local/lito381/server/NativeDeWorldKinds$Item$LastStatus\|local/lito381/server/NativeDeWorldKinds$Item\|LastStatus\|0x1 |
| `local/lito381/server/NativeDeWorldLocation` | class innerClasses: left=local/lito381/server/NativeDeObjectScope$Maps\|local/lito381/server/NativeDeObjectScope\|Maps\|0x18 right= |
| `local/lito381/server/NativeDeWorldLog` | method debug(Ljava/lang/String;)V: only left |
| `local/lito381/server/NativeDeWorldPlayerFilter` | class innerClasses: left=local/lito381/server/NativeDeDeletionPorts$ObjectRef\|local/lito381/server/NativeDeDeletionPorts\|ObjectRef\|0x608; local/lito381/server/NativeDeDeletionPorts$Viewer\|local/lito381/server/NativeDeDeletionPorts\|Viewer\|0x408; local/lito381/server… right=local/lito381/server/NativeDeWorldPlayerFilter$1\|\|\|0x1008 |
| `local/lito381/server/NativeDeWorldPlayerFilter$1` | field $SwitchMap$com$lineage$server$world$PlayerFilter:[I: only right |
| `local/lito381/server/NativeDeWorldPoint` | method isInScreen(Llocal/lito381/server/NativeDeWorldPoint;)Z insn[41]: left=GOTO L3 right=IRETURN |
| `local/lito381/server/NativeDeWorldRegistryBow` | class innerClasses: left=local/lito381/server/NativeDeWorldKinds$Bow\|local/lito381/server/NativeDeWorldKinds\|Bow\|0x8 right= |
| `local/lito381/server/NativeDeWorldRegistryCrown` | class innerClasses: left=local/lito381/server/NativeDeDeletionPorts$Viewer\|local/lito381/server/NativeDeDeletionPorts\|Viewer\|0x408 right= |
| `local/lito381/server/NativeDeWorldRegistryDarkelf` | class innerClasses: left=local/lito381/server/NativeDeDeletionPorts$Viewer\|local/lito381/server/NativeDeDeletionPorts\|Viewer\|0x408 right= |
| `local/lito381/server/NativeDeWorldRegistryDoll` | class innerClasses: left=local/lito381/server/NativeDeWorldKinds$Doll\|local/lito381/server/NativeDeWorldKinds\|Doll\|0x8 right= |
| `local/lito381/server/NativeDeWorldRegistryDragonKnight` | class innerClasses: left=local/lito381/server/NativeDeDeletionPorts$Viewer\|local/lito381/server/NativeDeDeletionPorts\|Viewer\|0x408 right= |
| `local/lito381/server/NativeDeWorldRegistryEffect` | class innerClasses: left=local/lito381/server/NativeDeWorldKinds$Effect\|local/lito381/server/NativeDeWorldKinds\|Effect\|0x8 right=java/lang/invoke/MethodHandles$Lookup\|java/lang/invoke/MethodHandles\|Lookup\|0x19 |
| `local/lito381/server/NativeDeWorldRegistryElf` | class innerClasses: left=local/lito381/server/NativeDeDeletionPorts$Viewer\|local/lito381/server/NativeDeDeletionPorts\|Viewer\|0x408 right= |
| `local/lito381/server/NativeDeWorldRegistryIllusionist` | class innerClasses: left=local/lito381/server/NativeDeDeletionPorts$Viewer\|local/lito381/server/NativeDeDeletionPorts\|Viewer\|0x408 right= |
| `local/lito381/server/NativeDeWorldRegistryItem` | class innerClasses: left=local/lito381/server/NativeDeWorldKinds$Item\|local/lito381/server/NativeDeWorldKinds\|Item\|0x8 right= |
| `local/lito381/server/NativeDeWorldRegistryKnight` | class innerClasses: left=local/lito381/server/NativeDeDeletionPorts$Viewer\|local/lito381/server/NativeDeDeletionPorts\|Viewer\|0x408 right= |
| `local/lito381/server/NativeDeWorldRegistryMob` | class innerClasses: left=local/lito381/server/NativeDeDeletionPorts$Monster\|local/lito381/server/NativeDeDeletionPorts\|Monster\|0x408 right= |
| `local/lito381/server/NativeDeWorldRegistryPet` | class innerClasses: left=local/lito381/server/NativeDeWorldKinds$Pet\|local/lito381/server/NativeDeWorldKinds\|Pet\|0x8 right= |
| `local/lito381/server/NativeDeWorldRegistryQuest` | class innerClasses: left=local/lito381/server/NativeDeDeletionPorts$ObjectRef\|local/lito381/server/NativeDeDeletionPorts\|ObjectRef\|0x608 right=com/lineage/server/datatables/QuestInstanceTable$Definition\|com/lineage/server/datatables/QuestInstanceTable\|Definition\|0x19 |
| `local/lito381/server/NativeDeWorldRegistrySummons` | class innerClasses: left=local/lito381/server/NativeDeDeletionPorts$Summon\|local/lito381/server/NativeDeDeletionPorts\|Summon\|0x408 right= |
| `local/lito381/server/NativeDeWorldRegistryTrap` | class innerClasses: left=java/lang/invoke/MethodHandles$Lookup\|java/lang/invoke/MethodHandles\|Lookup\|0x19; java/util/concurrent/ConcurrentHashMap$KeySetView\|java/util/concurrent/ConcurrentHashMap\|KeySetView\|0x9; local/lito381/server/NativeDeDeletionPorts$ObjectRef\|… right=java/lang/invoke/MethodHandles$Lookup\|java/lang/invoke/MethodHandles\|Lookup\|0x19; java/util/concurrent/ConcurrentHashMap$KeySetView\|java/util/concurrent/ConcurrentHashMap\|KeySetView\|0x9; local/lito381/server/NativeDeWorldRegistryTrap$TrapCe… |
| `local/lito381/server/NativeDeWorldRegistryTrap$TrapCell` | class innerClasses: left=java/lang/invoke/MethodHandles$Lookup\|java/lang/invoke/MethodHandles\|Lookup\|0x19; local/lito381/server/NativeDeDeletionPorts$ObjectRef\|local/lito381/server/NativeDeDeletionPorts\|ObjectRef\|0x608; local/lito381/server/NativeDeWorldKinds$Trap\|… right=java/lang/invoke/MethodHandles$Lookup\|java/lang/invoke/MethodHandles\|Lookup\|0x19; local/lito381/server/NativeDeWorldRegistryTrap$TrapCell\|local/lito381/server/NativeDeWorldRegistryTrap\|TrapCell\|0x1a |
| `local/lito381/server/NativeDeWorldRegistryWizard` | class innerClasses: left=local/lito381/server/NativeDeDeletionPorts$Viewer\|local/lito381/server/NativeDeDeletionPorts\|Viewer\|0x408 right= |
| `local/lito381/server/NativeDeWorldScope` | method call(Llocal/lito381/server/NativeDeWorldContext;Ljava/util/function/Supplier;)Ljava/lang/Object; insn[22]: left=ALOAD 3 right=LABEL L2 |
| `local/lito381/server/NativeEquipmentDefense` | method apply(Llocal/lito381/server/ClientSession;Llocal/lito381/server/ClientSession;Llocal/lito381/server/NativePlayerCommit$Binding;Llocal/lito381/server/NativePlayerCommit$Binding;Llocal/lito381/server/NativeEquipmentDefense$Resolved;Llocal/lito381/server/NativeCombatAttacker$Ref;)V: left=TRY L3 L7 L28 java/lang/RuntimeException   right=TRY L3 L7 L29 java/lang/RuntimeException   |
| `local/lito381/server/NativeLightAction` | method currentMaterial(Llocal/lito381/server/ClientSession;)Llocal/lito381/server/NativeCastMaterial$Cast; insn[0]: left=GETSTATIC local/lito381/server/NativeLightAction.current:Ljava/lang/ThreadLocal; right=ALOAD 0 |
| `local/lito381/server/NativeLightAction$Batch` | method run()V: left=TRY L0 L2 L13 java/io/IOException   right=TRY L4 L8 L9 java/lang/Throwable   |
| `local/lito381/server/NativeLitoMapTableCatalog` | method active()Ljava/util/List; insn[1]: left=ARETURN right=INVOKESTATIC local/lito381/server/ResourceDatabase.maps(Ljava/util/List;)Ljava/util/List; |
| `local/lito381/server/NativeLitoRentalTownNpcCatalog$Npc` | method name()Ljava/lang/String; insn[2]: left=ARETURN right=ALOAD 0 |
| `local/lito381/server/NativeLitoTownPortalCatalog` | method find(III)Llocal/lito381/server/NativeLitoTownPortalCatalog$Route; insn[20]: left=ARETURN right=ILOAD 0 |
| `local/lito381/server/NativeLitoZoneCatalog` | method colors()Ljava/util/List; insn[1]: left=ARETURN right=INVOKESTATIC local/lito381/server/ResourceDatabase.colors(Ljava/util/List;)Ljava/util/List; |
| `local/lito381/server/NativeMagicalWeaponAction` | class innerClasses: left=java/lang/invoke/MethodHandles$Lookup\|java/lang/invoke/MethodHandles\|Lookup\|0x19; local/lito381/server/BattleSpellCatalog$MagicalWeaponSpell\|local/lito381/server/BattleSpellCatalog\|MagicalWeaponSpell\|0x18; local/lito381/server/NativeCastMat… right=java/lang/invoke/MethodHandles$Lookup\|java/lang/invoke/MethodHandles\|Lookup\|0x19; local/lito381/server/BattleSpellCatalog$MagicalWeaponSpell\|local/lito381/server/BattleSpellCatalog\|MagicalWeaponSpell\|0x18; local/lito381/server/NativeCastMat… |
| `local/lito381/server/NativeNpcLifetime` | method arm(Llocal/lito381/server/NativeNpcLifetime$Lane;Ljava/lang/Runnable;JI)V insn[11]: left=ASTORE 7 right=DUP |
| `local/lito381/server/NativeNpcLifetime$Dispatch` | method run()V insn[15]: left=ASTORE 4 right=DUP |
| `local/lito381/server/NativeNpcLifetime$Lane` | method requestDrain()V insn[2]: left=ASTORE 2 right=DUP |
| `local/lito381/server/NativeNpcLifetime$Retry` | method run()V insn[3]: left=ASTORE 1 right=DUP |
| `local/lito381/server/NativeNpcPhysicalSource` | method physicalSize(Ljava/lang/String;)Ljava/lang/String;: only right |
| `local/lito381/server/NativePcActionAuthority` | class innerClasses: left=local/lito381/server/NativeDeItemTransaction$Operation\|local/lito381/server/NativeDeItemTransaction\|Operation\|0x608; local/lito381/server/NativePlayerActionScope$Ticket\|local/lito381/server/NativePlayerActionScope\|Ticket\|0x18 right=local/lito381/server/NativeDeItemTransaction$Operation\|local/lito381/server/NativeDeItemTransaction\|Operation\|0x608 |
| `local/lito381/server/NativePetMaterialAction` | method taming(Llocal/lito381/server/ClientSession;[BLlocal/lito381/server/BattleSpellCatalog$TamingSpell;)V insn[1]: left=ALOAD 0 right=ALOAD 1 |
| `local/lito381/server/NativePetWorldLifetime` | method lambda$run$#0(Llocal/lito381/server/ClientSession;Llocal/lito381/server/NativePetWorldLifetime$Target;[BLlocal/lito381/server/NativeSelfCastAction$Task;)V: only right |
| `local/lito381/server/NativePlayerActionScope` | method blocked(Ljava/lang/Runnable;)Llocal/lito381/server/NativePlayerActionScope$Root; insn[2]: left=IFNE L0 right=IFEQ L0 |
| `local/lito381/server/NativePlayerActionScope$Root` | method finish()V: left=TRY L7 L15 L16    right=TRY L7 L13 L14    |
| `local/lito381/server/NativePlayerCommit` | class innerClasses: left=java/lang/invoke/MethodHandles$Lookup\|java/lang/invoke/MethodHandles\|Lookup\|0x19; local/lito381/server/BattleAbilityStatus$IllusionAvatar\|local/lito381/server/BattleAbilityStatus\|IllusionAvatar\|0x18; local/lito381/server/BattleCompanion$Kin… right=java/lang/invoke/MethodHandles$Lookup\|java/lang/invoke/MethodHandles\|Lookup\|0x19; local/lito381/server/BattleAbilityStatus$IllusionAvatar\|local/lito381/server/BattleAbilityStatus\|IllusionAvatar\|0x18; local/lito381/server/BattleCompanion$Kin… |
| `local/lito381/server/NativePlayerCommit$Binding` | method capture(Llocal/lito381/server/ClientSession;)Llocal/lito381/server/NativePlayerCommit$Binding; insn[7]: left=DUP right=ASTORE 1 |
| `local/lito381/server/NativePlayerCommit$Queue` | method claim()Llocal/lito381/server/NativePlayerCommit$Event; insn[19]: left=DUP right=ASTORE 1 |
| `local/lito381/server/NativePlayerLifetime` | method allIdle()Z insn[14]: left=IFEQ L1 right=IFNE L1 |
| `local/lito381/server/NativePlayerVitalCompletions` | class innerClasses: left=java/lang/System$Logger$Level\|java/lang/System$Logger\|Level\|0x4019; java/lang/System$Logger\|java/lang/System\|Logger\|0x609; java/lang/invoke/MethodHandles$Lookup\|java/lang/invoke/MethodHandles\|Lookup\|0x19; local/lito381/server/NativePlayerAc… right=java/lang/System$Logger$Level\|java/lang/System$Logger\|Level\|0x4019; java/lang/System$Logger\|java/lang/System\|Logger\|0x609; java/lang/invoke/MethodHandles$Lookup\|java/lang/invoke/MethodHandles\|Lookup\|0x19; local/lito381/server/NativePlayerCo… |
| `local/lito381/server/NativePlayerVitalCompletions$Receipt` | method release()V insn[24]: left=ASTORE 1 right=DUP |
| `local/lito381/server/NativePlayerVitals` | method calculate(IIIIZ)Llocal/lito381/server/NativePlayerVitals$Result; insn[8]: left=LSTORE 6 right=LSTORE 5 |
| `local/lito381/server/NativePoisonMaterialAction` | class innerClasses: left=java/lang/invoke/MethodHandles$Lookup\|java/lang/invoke/MethodHandles\|Lookup\|0x19; local/lito381/server/BattleSpellCatalog$ParalysisSpell\|local/lito381/server/BattleSpellCatalog\|ParalysisSpell\|0x18; local/lito381/server/BattleSpellCatalog$Po… right=java/lang/invoke/MethodHandles$Lookup\|java/lang/invoke/MethodHandles\|Lookup\|0x19; local/lito381/server/BattleSpellCatalog$ParalysisSpell\|local/lito381/server/BattleSpellCatalog\|ParalysisSpell\|0x18; local/lito381/server/BattleSpellCatalog$Po… |
| `local/lito381/server/NativeProcEncounterCatalog` | method load(Llocal/lito381/server/ServerConfig;)V insn[8]: left=ASTORE 2 right=ASTORE 1 |
| `local/lito381/server/NativeProcEncounterPublication` | method capture(Ljava/util/List;Llocal/lito381/server/NativeProcEncounterPublication$Context;)Ljava/util/List;: left=TRY L1 L5 L6    right=TRY L1 L6 L7    |
| `local/lito381/server/NativeProcEncounterPublication$Context` | method valid()Z insn[6]: left=ASTORE 2 right=DUP |
| `local/lito381/server/NativeProcHealBonus` | method load(Llocal/lito381/server/ServerConfig;)V insn[7]: left=ASTORE 2 right=ASTORE 1 |
| `local/lito381/server/NativeProcHealingCompanion` | method apply(Llocal/lito381/server/ClientSession;Llocal/lito381/server/NativeCombatAttacker$Ref;Llocal/lito381/server/NativeArmorProc$Rule;)Z insn[14]: left=ASTORE 4 right=ASTORE 3 |
| `local/lito381/server/NativeProcHealingCompanion$Dispatch` | method run()V: left=TRY L1 L3 L6 java/lang/Throwable   right=TRY L1 L4 L5 java/lang/Throwable   |
| `local/lito381/server/NativeProcHealingEncounter` | method commit(Llocal/lito381/server/NativeProcHealingEncounter$Live;Llocal/lito381/server/NativeProcHealingEncounter$Plan;I)Llocal/lito381/server/NativeProcEncounterPublication$Context; insn[13]: left=ASTORE 5 right=ASTORE 3 |
| `local/lito381/server/NativeProcHealingEncounter$Dispatch` | method run()V: left=TRY L1 L3 L6 java/lang/Throwable   right=TRY L1 L4 L5 java/lang/Throwable   |
| `local/lito381/server/NativeProcHealingPlayer` | class innerClasses: left=java/lang/invoke/MethodHandles$Lookup\|java/lang/invoke/MethodHandles\|Lookup\|0x19; local/lito381/server/NativePlayerCommit$Binding\|local/lito381/server/NativePlayerCommit\|Binding\|0x18; local/lito381/server/NativePlayerCommit$Queue\|local/lito… right=java/lang/invoke/MethodHandles$Lookup\|java/lang/invoke/MethodHandles\|Lookup\|0x19; local/lito381/server/NativePlayerCommit$Binding\|local/lito381/server/NativePlayerCommit\|Binding\|0x18; local/lito381/server/NativePlayerCommit$Queue\|local/lito… |
| `local/lito381/server/NativeProcHealingWorld` | method allow(Llocal/lito381/server/CombatMonsterCurseLifecycle$Actor;Llocal/lito381/server/CombatNpcDamageQueue$Event;)Z insn[2]: left=ASTORE 2 right=ASTORE 3 |
| `local/lito381/server/NativeProcNpcCatalog` | method load(Llocal/lito381/server/ServerConfig;)V insn[8]: left=ASTORE 2 right=ASTORE 1 |
| `local/lito381/server/NativeProcNpcCatalog$Catalog` | method kind(I)Llocal/lito381/server/NativeProcHealingKernel$Kind; insn[3]: left=ASTORE 3 right=ASTORE 2 |
| `local/lito381/server/NativeProcNpcState` | method activate(Llocal/lito381/server/BattleTimedStatus;IJI)V insn[34]: left=ASTORE 7 right=DUP |
| `local/lito381/server/NativeProcPlayerState` | method activate(Llocal/lito381/server/ClientSession;Llocal/lito381/server/NativePlayerCommit$Binding;IJI)Z insn[35]: left=ASTORE 8 right=DUP |
| `local/lito381/server/NativeProcSkillCatalog` | method load(Llocal/lito381/server/ServerConfig;)V insn[7]: left=ASTORE 2 right=ASTORE 1 |
| `local/lito381/server/NativeResurrectionMaterialAction` | class nestMembers: left= right=local/lito381/server/NativeResurrectionMaterialAction$Target |
| `local/lito381/server/NativeReturnNatureMaterialAction` | method caster(Llocal/lito381/server/ClientSession;Llocal/lito381/server/BattleCompanion;)Llocal/lito381/server/ClientSession; insn[5]: left=IFNULL L0 right=IFNONNULL L0 |
| `local/lito381/server/NativeReuseLog$Log` | method emit(Ljava/lang/System$Logger$Level;Ljava/lang/String;[Ljava/lang/Object;)V insn[12]: left=IF_ICMPGE L4 right=IF_ICMPGE L3 |
| `local/lito381/server/NativeReuseShutdown` | method defer(Llocal/lito381/server/NativePlayerLifetime;)Z insn[2]: left=ASTORE 1 right=DUP |
| `local/lito381/server/NativeRolePublication` | method allowPublication(Llocal/lito381/server/ClientSession;)Z insn[3]: left=ASTORE 2 right=ASTORE 1 |
| `local/lito381/server/NativeRoomRules` | class innerClasses: left=java/lang/invoke/MethodHandles$Lookup\|java/lang/invoke/MethodHandles\|Lookup\|0x19; local/lito381/server/BattleRules$MatchState\|local/lito381/server/BattleRules\|MatchState\|0x18; local/lito381/server/BattleRules$Mode\|local/lito381/server/Battl… right=java/lang/invoke/MethodHandles$Lookup\|java/lang/invoke/MethodHandles\|Lookup\|0x19; local/lito381/server/BattleRules$MatchState\|local/lito381/server/BattleRules\|MatchState\|0x18; local/lito381/server/BattleRules$Mode\|local/lito381/server/Battl… |
| `local/lito381/server/NativeServerLifetime` | method acceptSocket(Ljava/net/ServerSocket;Llocal/lito381/server/GameHub;)Ljava/net/Socket; insn[13]: left=ASTORE 4 right=DUP |
| `local/lito381/server/NativeSkillConsumptionBridges` | class access: left=0x30 right=0x10 |
| `local/lito381/server/NativeSourceActionIntervalCalculator` | method calculate(ILlocal/lito381/server/NativeSourceActionIntervalCalculator$ActionType;Llocal/lito381/server/NativeSourceActionIntervalCalculator$SpeedState;)I insn[3]: left=ISTORE 4 right=ISTORE 3 |
| `local/lito381/server/NativeSourceClientActionTimeline` | method canReserve(Ljava/util/concurrent/atomic/AtomicReference;Llocal/lito381/server/NativeSourceClientActionTimeline$ActionKind;Z)Z insn[6]: left=ASTORE 7 right=DUP |
| `local/lito381/server/NativeSourceClientActionTimeline$Reservation` | method cancel()Z insn[2]: left=ASTORE 1 right=DUP |
| `local/lito381/server/NativeSourceMaterialPlan` | method appendGeneralConsumePlan(Ljava/util/List;Llocal/lito381/server/NativeSourceMaterialPlan$ConsumeRequirement;)Llocal/lito381/server/NativeSourceMaterialPlan$ItemConsumeCommitResult; insn[2]: left=ISTORE 5 right=ISTORE 3 |
| `local/lito381/server/NativeSourcePlayerActionLock` | method call(Llocal/lito381/server/ClientSession;Ljava/util/function/Supplier;)Ljava/lang/Object; insn[21]: left=ALOAD 3 right=LABEL L1 |
| `local/lito381/server/NativeSourceSkillCastAdmission` | method admit(Ljava/util/function/BooleanSupplier;Ljava/util/function/Supplier;Ljava/util/function/BooleanSupplier;)Llocal/lito381/server/NativeSourceSkillCastAdmission$Result;: left=TRY L1 L2 L5 java/lang/Error   right=TRY L1 L2 L5 java/lang/RuntimeException   |
| `local/lito381/server/NativeSourceSkillConsumptionMath` | method mpCost(IIIIZ)I insn[96]: left=LOOKUPSWITCH default=L25 keys=1,12,13,19,26,42,43,54 labels=L13,L17,L19,L15,L8,L21,L10,L23 right=LOOKUPSWITCH default=L17 keys=1,12,13,19,26,42,43,54 labels=L11,L13,L14,L12,L8,L15,L9,L16 |
| `local/lito381/server/NativeSourceSkillDelay` | class innerClasses: left=java/lang/invoke/MethodHandles$Lookup\|java/lang/invoke/MethodHandles\|Lookup\|0x19; local/lito381/server/NativeReuseLog$Logger\|local/lito381/server/NativeReuseLog\|Logger\|0x608; local/lito381/server/NativeSourceSkillDelay$Lease\|local/lito381/s… right=java/lang/invoke/MethodHandles$Lookup\|java/lang/invoke/MethodHandles\|Lookup\|0x19; local/lito381/server/NativeSourceSkillDelay$Lease\|local/lito381/server/NativeSourceSkillDelay\|Lease\|0x19 |
| `local/lito381/server/NativeSourceSkillDelay$Lease` | class innerClasses: left=local/lito381/server/NativeReuseLog$Logger\|local/lito381/server/NativeReuseLog\|Logger\|0x608; local/lito381/server/NativeSourceSkillDelay$Lease\|local/lito381/server/NativeSourceSkillDelay\|Lease\|0x19 right=local/lito381/server/NativeSourceSkillDelay$Lease\|local/lito381/server/NativeSourceSkillDelay\|Lease\|0x19 |
| `local/lito381/server/NativeWeaponBreakMaterialAction` | method battle(Llocal/lito381/server/ClientSession;[BLlocal/lito381/server/BattleSpellCatalog$WeaponBreakSpell;)V insn[1]: left=ALOAD 0 right=LDC str:"phase" |
| `local/lito381/server/Protocol` | method attackRequest([BI)Llocal/lito381/server/Protocol$AttackRequest; insn[0]: left=ILOAD 1 right=ALOAD 0 |
| `local/lito381/server/ResourceDatabase` | method load(Llocal/lito381/server/ServerConfig;)V insn[0]: left=GETSTATIC local/lito381/server/ResourceDatabase.current:Llocal/lito381/server/ResourceDatabase$Snapshot; right=ALOAD 0 |
| `local/lito381/server/RuntimeInventory` | field items:Ljava/util/LinkedHashMap;: only left |
| `local/lito381/server/RuntimeInventory$Item` | class innerClasses: left=java/lang/invoke/MethodHandles$Lookup\|java/lang/invoke/MethodHandles\|Lookup\|0x19; local/lito381/server/InventoryCatalog$Definition\|local/lito381/server/InventoryCatalog\|Definition\|0x18; local/lito381/server/InventoryCatalog$NativeArmorStatu… right=java/lang/invoke/MethodHandles$Lookup\|java/lang/invoke/MethodHandles\|Lookup\|0x19; local/lito381/server/InventoryCatalog$Definition\|local/lito381/server/InventoryCatalog\|Definition\|0x18; local/lito381/server/InventoryCatalog$NativeArmorStatu… |
| `local/lito381/server/ServerConfig` | method applyExternalSourceServers(Ljava/util/Properties;Ljava/nio/file/Path;)V: left=TRY L2 L3 L4 java/lang/Throwable   right=TRY L3 L4 L5 java/lang/Throwable   |
| `local/lito381/server/SharedClientResources` | method loadDurations()Ljava/util/Map; insn[113]: left=IFNONNULL L7 right=IFNULL L8 |
| `local/lito381/server/SourceEquipmentPhysicalStats` | method supportsSolidCarriage()Z insn[2]: left=IFNE L0 right=ALOAD 0 |
| `local/lito381/server/SourceLogin` | method lambda$start$#0(Ljava/net/ServerSocket;Ljava/util/concurrent/ExecutorService;Llocal/lito381/server/ClientSession$Endpoint;Llocal/lito381/server/ServerConfig;Llocal/lito381/server/AccountRepository;Llocal/lito381/server/GameStateStore;Llocal/lito381/server/CaptureLog;Llocal/lito381/server/GameHub;)V: length left=2 right=3 |

## ONLY_LEFT

只在 left（1380）。完整類別清單見 JSON artifact；與 `docs/server-source-gap.md` 的「只在 target」應對齊。

```
local/lito381/server/CaptureLog$Connection
local/lito381/server/EquipmentRules
local/lito381/server/EquipmentRules$Actor
local/lito381/server/EquipmentRules$ArmorFacts
local/lito381/server/EquipmentRules$ArmorSlot
local/lito381/server/EquipmentRules$Change
local/lito381/server/EquipmentRules$CharacterClass
local/lito381/server/EquipmentRules$CurseState
local/lito381/server/EquipmentRules$Facts
local/lito381/server/EquipmentRules$Item
local/lito381/server/EquipmentRules$LevelRange
local/lito381/server/EquipmentRules$Loadout
local/lito381/server/EquipmentRules$NonEquipmentFacts
local/lito381/server/EquipmentRules$Policy
local/lito381/server/EquipmentRules$Reason
local/lito381/server/EquipmentRules$Rejection
local/lito381/server/EquipmentRules$Result
local/lito381/server/EquipmentRules$SealState
local/lito381/server/EquipmentRules$SealedItemPolicy
local/lito381/server/EquipmentRules$Transition
local/lito381/server/EquipmentRules$UnknownFacts
local/lito381/server/EquipmentRules$WeaponFacts
local/lito381/server/LegacyJdbcNetworkDefaults
local/lito381/server/ModernAccountAuthentication
local/lito381/server/ModernAccountAuthentication$AccountAuthenticator
local/lito381/server/ModernAccountAuthentication$AuthenticationException
local/lito381/server/ModernAccountAuthentication$CharacterKey
local/lito381/server/ModernAccountAuthentication$CredentialVerifier
local/lito381/server/ModernAccountAuthentication$Failure
local/lito381/server/ModernAccountAuthentication$Principal
local/lito381/server/ModernAccountAuthentication$Source
local/lito381/server/ModernAccountAuthentication$SourceBinding
local/lito381/server/ModernAmmo
local/lito381/server/ModernAmmo$Facts
local/lito381/server/ModernAmmo$Kind
local/lito381/server/ModernAmmo$State
local/lito381/server/ModernAmmo$Stock
local/lito381/server/ModernAmmoProtocol
local/lito381/server/ModernAmmoProtocol$Action
local/lito381/server/ModernAmmoProtocol$Reason
local/lito381/server/ModernAmmoProtocol$Reply
local/lito381/server/ModernAmmoProtocol$Request
local/lito381/server/ModernAmmoProtocol$Status
local/lito381/server/ModernApplicationProtocol
local/lito381/server/ModernApplicationProtocol$AmmoRequest
local/lito381/server/ModernApplicationProtocol$AmmoResult
local/lito381/server/ModernApplicationProtocol$Auth
local/lito381/server/ModernApplicationProtocol$AuthResult
local/lito381/server/ModernApplicationProtocol$BattleActionRequest
local/lito381/server/ModernApplicationProtocol$BattleActionResult
local/lito381/server/ModernApplicationProtocol$BattleEquipmentRequest
local/lito381/server/ModernApplicationProtocol$BattleEquipmentResult
local/lito381/server/ModernApplicationProtocol$BattleOperationRequest
local/lito381/server/ModernApplicationProtocol$BattleOperationResult
local/lito381/server/ModernApplicationProtocol$BattleReadRequest
local/lito381/server/ModernApplicationProtocol$BattleReadResult
local/lito381/server/ModernApplicationProtocol$BattleRevivalRequest
local/lito381/server/ModernApplicationProtocol$BattleRevivalResult
local/lito381/server/ModernApplicationProtocol$BattleWorldRequest
local/lito381/server/ModernApplicationProtocol$BattleWorldResult
local/lito381/server/ModernApplicationProtocol$CastRequest
local/lito381/server/ModernApplicationProtocol$CastResult
local/lito381/server/ModernApplicationProtocol$ChannelEnter
local/lito381/server/ModernApplicationProtocol$ChannelRow
local/lito381/server/ModernApplicationProtocol$CharacterDataRequest
local/lito381/server/ModernApplicationProtocol$CharacterDataResult
local/lito381/server/ModernApplicationProtocol$CharacterRequest
local/lito381/server/ModernApplicationProtocol$CharacterResult
local/lito381/server/ModernApplicationProtocol$CharacterReturn
local/lito381/server/ModernApplicationProtocol$CharacterRow
local/lito381/server/ModernApplicationProtocol$CharacterSelection
local/lito381/server/ModernApplicationProtocol$ChatRequest
local/lito381/server/ModernApplicationProtocol$ChatResult
local/lito381/server/ModernApplicationProtocol$CombatRequest
local/lito381/server/ModernApplicationProtocol$CombatResult
local/lito381/server/ModernApplicationProtocol$DirectChatRequest
local/lito381/server/ModernApplicationProtocol$DirectChatResult
local/lito381/server/ModernApplicationProtocol$DisintegrateRequest
local/lito381/server/ModernApplicationProtocol$DisintegrateResult
local/lito381/server/ModernApplicationProtocol$EquipmentRequest
local/lito381/server/ModernApplicationProtocol$EquipmentResult
local/lito381/server/ModernApplicationProtocol$FilteredChatRequest
local/lito381/server/ModernApplicationProtocol$FilteredChatResult
local/lito381/server/ModernApplicationProtocol$GmRequest
local/lito381/server/ModernApplicationProtocol$GmResult
local/lito381/server/ModernApplicationProtocol$GreaterHasteRequest
local/lito381/server/ModernApplicationProtocol$GreaterHasteResult
local/lito381/server/ModernApplicationProtocol$HasteRequest
local/lito381/server/ModernApplicationProtocol$HasteResult
local/lito381/server/ModernApplicationProtocol$ItemRequest
local/lito381/server/ModernApplicationProtocol$ItemResult
local/lito381/server/ModernApplicationProtocol$LobbyLeave
local/lito381/server/ModernApplicationProtocol$MatchmakingRequest
local/lito381/server/ModernApplicationProtocol$MatchmakingResult
local/lito381/server/ModernApplicationProtocol$Message
local/lito381/server/ModernApplicationProtocol$ModeratedDirectRequest
local/lito381/server/ModernApplicationProtocol$ModeratedDirectResult
local/lito381/server/ModernApplicationProtocol$NativeActorWorldRequest
local/lito381/server/ModernApplicationProtocol$NativeActorWorldResult
local/lito381/server/ModernApplicationProtocol$NativeCanonicalWorldRequest
local/lito381/server/ModernApplicationProtocol$NativeCanonicalWorldResult
local/lito381/server/ModernApplicationProtocol$NativeCatalogWorldRequest
local/lito381/server/ModernApplicationProtocol$NativeCatalogWorldResult
local/lito381/server/ModernApplicationProtocol$NativeWorldRequest
local/lito381/server/ModernApplicationProtocol$NativeWorldResult
local/lito381/server/ModernApplicationProtocol$NavigationResult
local/lito381/server/ModernApplicationProtocol$NpcDisintegrateRequest
local/lito381/server/ModernApplicationProtocol$NpcDisintegrateResult
local/lito381/server/ModernApplicationProtocol$NpcFireStormRequest
local/lito381/server/ModernApplicationProtocol$NpcFireStormResult
local/lito381/server/ModernApplicationProtocol$NpcRequest
local/lito381/server/ModernApplicationProtocol$NpcResult
local/lito381/server/ModernApplicationProtocol$NpcSpellRequest
local/lito381/server/ModernApplicationProtocol$NpcSpellResult
local/lito381/server/ModernApplicationProtocol$Ping
local/lito381/server/ModernApplicationProtocol$PlayerHealingRequest
local/lito381/server/ModernApplicationProtocol$PlayerHealingResult
local/lito381/server/ModernApplicationProtocol$Pong
local/lito381/server/ModernApplicationProtocol$ProtocolException
local/lito381/server/ModernApplicationProtocol$RecordRequest
local/lito381/server/ModernApplicationProtocol$RecordResult
local/lito381/server/ModernApplicationProtocol$RoomRequest
local/lito381/server/ModernApplicationProtocol$RoomResult
local/lito381/server/ModernApplicationProtocol$ShopRequest
local/lito381/server/ModernApplicationProtocol$ShopResult
local/lito381/server/ModernApplicationProtocol$SiegeRequest
local/lito381/server/ModernApplicationProtocol$SiegeResult
local/lito381/server/ModernApplicationProtocol$SkillsRequest
local/lito381/server/ModernApplicationProtocol$SkillsResult
local/lito381/server/ModernApplicationProtocol$SlowRequest
local/lito381/server/ModernApplicationProtocol$SlowResult
local/lito381/server/ModernApplicationProtocol$Source
local/lito381/server/ModernApplicationProtocol$VipEquipmentRequest
local/lito381/server/ModernApplicationProtocol$VipEquipmentResult
local/lito381/server/ModernApplicationProtocol$VipWorldRequest
local/lito381/server/ModernApplicationProtocol$VipWorldResult
local/lito381/server/ModernApplicationProtocol$WorldRequest
local/lito381/server/ModernApplicationProtocol$WorldResult
local/lito381/server/ModernApplicationSession
local/lito381/server/ModernArmorPresentation
local/lito381/server/ModernArmorPresentation$Anchor
local/lito381/server/ModernArmorPresentation$Batch
local/lito381/server/ModernArmorPresentation$Candidate
local/lito381/server/ModernArmorPresentation$Requirements
local/lito381/server/ModernArmorPresentation$Role
local/lito381/server/ModernArmorPresentation$Visual
local/lito381/server/ModernArmorProcProtection
local/lito381/server/ModernArmorProcProtection$Actor
local/lito381/server/ModernArmorProcProtection$Candidate
local/lito381/server/ModernArmorProcProtection$Equipped
local/lito381/server/ModernArmorProcProtection$Failure
local/lito381/server/ModernArmorProcProtection$ProcException
local/lito381/server/ModernArmorProcProtection$Selection
local/lito381/server/ModernArmorProcProtection$State
local/lito381/server/ModernArmorProcProtection$Trigger
local/lito381/server/ModernArmorProcs
local/lito381/server/ModernArmorProcs$Catalog
local/lito381/server/ModernArmorProcs$Rule
local/lito381/server/ModernArmorProcsJdbc
local/lito381/server/ModernArmorProtectionCatalog
local/lito381/server/ModernArmorProtectionCatalog$BoundSource
local/lito381/server/ModernArmorProtectionCatalog$Snapshot
local/lito381/server/ModernArmorProtectionCatalog$Status
local/lito381/server/ModernArmorProtectionEffects
local/lito381/server/ModernArmorProtectionEffects$Effect
local/lito381/server/ModernArmorProtectionEffects$State
local/lito381/server/ModernArmorProtectionLoadout
local/lito381/server/ModernBackendExecutor
local/lito381/server/ModernBackendExecutor$1
local/lito381/server/ModernBackendExecutor$2
local/lito381/server/ModernBackendExecutor$BackendException
local/lito381/server/ModernBackendExecutor$Failure
local/lito381/server/ModernBackendExecutor$Job
local/lito381/server/ModernBackendExecutor$JobState
local/lito381/server/ModernBackendExecutor$SessionGate
local/lito381/server/ModernBackendExecutor$Work
local/lito381/server/ModernBackendScope
local/lito381/server/ModernBackendScope$AbortScheduler
local/lito381/server/ModernBackendScope$ConnectionLease
local/lito381/server/ModernBackendScope$SocketLease
local/lito381/server/ModernBattleActionProtocol
local/lito381/server/ModernBattleActionProtocol$1
local/lito381/server/ModernBattleActionProtocol$Kind
local/lito381/server/ModernBattleActionProtocol$Request
local/lito381/server/ModernBattleActionProtocol$Target
local/lito381/server/ModernBattleActionReceipt
local/lito381/server/ModernBattleArmorPresentationWire
local/lito381/server/ModernBattleArmorWire
local/lito381/server/ModernBattleAudience
local/lito381/server/ModernBattleAudience$Equipment
local/lito381/server/ModernBattleAudience$Lane
local/lito381/server/ModernBattleAudience$Modes
local/lito381/server/ModernBattleAudience$Npc
local/lito381/server/ModernBattleAudience$Npcs
local/lito381/server/ModernBattleAudience$Personal
local/lito381/server/ModernBattleAudience$Quest
local/lito381/server/ModernBattleAudience$Race
local/lito381/server/ModernBattleAudience$TimeAttack
local/lito381/server/ModernBattleAudience$View
local/lito381/server/ModernBattleAudience$World
local/lito381/server/ModernBattleCommand
local/lito381/server/ModernBattleCommandLife
local/lito381/server/ModernBattleCommandLife$Proof
local/lito381/server/ModernBattleCommandLife$Rejection
local/lito381/server/ModernBattleCommandLife$Result
local/lito381/server/ModernBattleEngine
local/lito381/server/ModernBattleEngine$1
local/lito381/server/ModernBattleEngine$AbortReason
local/lito381/server/ModernBattleEngine$Actor
local/lito381/server/ModernBattleEngine$AmmoResult
local/lito381/server/ModernBattleEngine$ArmorGuard
local/lito381/server/ModernBattleEngine$ArmorHit
local/lito381/server/ModernBattleEngine$ArmorProtectionView
local/lito381/server/ModernBattleEngine$ArmorStamp
local/lito381/server/ModernBattleEngine$AttackResult
local/lito381/server/ModernBattleEngine$AttackStatus
local/lito381/server/ModernBattleEngine$BotPrepared
local/lito381/server/ModernBattleEngine$CastResult
local/lito381/server/ModernBattleEngine$ChangedCommandLife
local/lito381/server/ModernBattleEngine$DisintegrateResult
local/lito381/server/ModernBattleEngine$EquipmentAdmission
local/lito381/server/ModernBattleEngine$EquipmentChange
local/lito381/server/ModernBattleEngine$EquipmentDisposition
local/lito381/server/ModernBattleEngine$EquipmentPublication
local/lito381/server/ModernBattleEngine$EquipmentResources
local/lito381/server/ModernBattleEngine$FireCandidate
local/lito381/server/ModernBattleEngine$GreaterHasteResult
local/lito381/server/ModernBattleEngine$HasteResult
local/lito381/server/ModernBattleEngine$ItemResult
local/lito381/server/ModernBattleEngine$Limits
local/lito381/server/ModernBattleEngine$MoveResult
local/lito381/server/ModernBattleEngine$MoveStatus
local/lito381/server/ModernBattleEngine$MutableActor
local/lito381/server/ModernBattleEngine$Observed
local/lito381/server/ModernBattleEngine$Phase
local/lito381/server/ModernBattleEngine$PlayerHealingResult
local/lito381/server/ModernBattleEngine$PreparedMember
local/lito381/server/ModernBattleEngine$SiegePortalResult
local/lito381/server/ModernBattleEngine$SlowResult
local/lito381/server/ModernBattleEngine$Snapshot
local/lito381/server/ModernBattleEngine$UseHistories
local/lito381/server/ModernBattleEngine$VipOperation
local/lito381/server/ModernBattleEngine$VipReplacement
local/lito381/server/ModernBattleEngine$VipStaleCalculation
local/lito381/server/ModernBattleEquipmentProtocol
local/lito381/server/ModernBattleEquipmentProtocol$1
local/lito381/server/ModernBattleEquipmentProtocol$Disposition
local/lito381/server/ModernBattleEquipmentProtocol$Reason
local/lito381/server/ModernBattleEquipmentProtocol$Reply
local/lito381/server/ModernBattleEquipmentProtocol$Request
local/lito381/server/ModernBattleHasteWire
local/lito381/server/ModernBattleHealingWire
local/lito381/server/ModernBattleHonorProof
local/lito381/server/ModernBattleHonorProof$1
local/lito381/server/ModernBattleLifeProof
local/lito381/server/ModernBattleLifeProof$Life
local/lito381/server/ModernBattleLifecycle
local/lito381/server/ModernBattleLoadout
local/lito381/server/ModernBattleLoadout$1
local/lito381/server/ModernBattleLoadout$Features
local/lito381/server/ModernBattleModeProof
local/lito381/server/ModernBattleObservation
local/lito381/server/ModernBattleObservation$Actor
local/lito381/server/ModernBattleObservation$Capture
local/lito381/server/ModernBattleObservation$CapturedActor
local/lito381/server/ModernBattleObservation$Clock
local/lito381/server/ModernBattleObservation$Features
local/lito381/server/ModernBattleObservation$Honor
local/lito381/server/ModernBattleObservation$Impact
local/lito381/server/ModernBattleObservation$Modes
local/lito381/server/ModernBattleObservation$Npcs
local/lito381/server/ModernBattleObservation$OwnerView
local/lito381/server/ModernBattleObservation$PlayerHealing
local/lito381/server/ModernBattleObservation$PlayerHealingReceipt
local/lito381/server/ModernBattleObservation$PrivateOwner
local/lito381/server/ModernBattleObservation$Quest
local/lito381/server/ModernBattleObservation$RegenAxis
local/lito381/server/ModernBattleObservation$Regeneration
local/lito381/server/ModernBattleObservation$SpellReceipt
local/lito381/server/ModernBattleObservation$Spells
local/lito381/server/ModernBattleObservation$Structure
local/lito381/server/ModernBattleObservation$Supplies
local/lito381/server/ModernBattleObservation$Vital
local/lito381/server/ModernBattleObservation$World
local/lito381/server/ModernBattleOperationProtocol
local/lito381/server/ModernBattleOperationProtocol$Disposition
local/lito381/server/ModernBattleOperationProtocol$Outcome
local/lito381/server/ModernBattleOperationProtocol$Reason
local/lito381/server/ModernBattlePresentation
local/lito381/server/ModernBattlePresentation$Item
local/lito381/server/ModernBattlePresentation$Shot
local/lito381/server/ModernBattlePresentation$State
local/lito381/server/ModernBattlePublication
local/lito381/server/ModernBattleQuestProof
local/lito381/server/ModernBattleRaceProof
local/lito381/server/ModernBattleRaceProof$1
local/lito381/server/ModernBattleReadProtocol
local/lito381/server/ModernBattleReadProtocol$Reply
local/lito381/server/ModernBattleReadProtocol$Request
local/lito381/server/ModernBattleReadProtocol$State
local/lito381/server/ModernBattleRevivalProtocol
local/lito381/server/ModernBattleRevivalProtocol$1
local/lito381/server/ModernBattleRevivalProtocol$Reason
local/lito381/server/ModernBattleRevivalProtocol$Reply
local/lito381/server/ModernBattleRevivalProtocol$Request
local/lito381/server/ModernBattleSpeedWire
local/lito381/server/ModernBattleTimeAttackProof
local/lito381/server/ModernBattleVipCommandState
local/lito381/server/ModernBattleVipCommandState$EquipmentAdmission
local/lito381/server/ModernBattleVipCommandState$Proof
local/lito381/server/ModernBattleVipCommandState$Rejection
local/lito381/server/ModernBattleVipObservation
local/lito381/server/ModernBattleVipObservation$Appearance
local/lito381/server/ModernBattleVipObservation$Equipment
local/lito381/server/ModernBattleVipObservation$Item
local/lito381/server/ModernBattleVipObservation$Personal
local/lito381/server/ModernBattleVipObservation$Publication
local/lito381/server/ModernBattleVipObservation$Versions
local/lito381/server/ModernBattleVipObservation$View
local/lito381/server/ModernBattleVipObservation$World
local/lito381/server/ModernBattleVipWire
local/lito381/server/ModernBattleVipWire$Payload
local/lito381/server/ModernBattleWire
local/lito381/server/ModernBattleWire$1
local/lito381/server/ModernBattleWire$Encoding
local/lito381/server/ModernBattleWire$Writer
local/lito381/server/ModernBattleWireModes
local/lito381/server/ModernBattleWirePersonal
local/lito381/server/ModernBattleWorldProtocol
local/lito381/server/ModernBattleWorldProtocol$1
local/lito381/server/ModernBattleWorldProtocol$Action
local/lito381/server/ModernBattleWorldProtocol$Reply
local/lito381/server/ModernBattleWorldProtocol$Request
local/lito381/server/ModernBattleWorldProtocol$State
local/lito381/server/ModernBotTemplate
local/lito381/server/ModernBuffProtocol
local/lito381/server/ModernCharacterArmor
local/lito381/server/ModernCharacterData
local/lito381/server/ModernCharacterData$InventoryItem
local/lito381/server/ModernCharacterData$ItemKind
local/lito381/server/ModernCharacterData$ItemTemplate
local/lito381/server/ModernCharacterData$PersistedValues
local/lito381/server/ModernCharacterData$Snapshot
local/lito381/server/ModernCharacterDataJdbc
local/lito381/server/ModernCharacterDataJdbc$ItemObserver
local/lito381/server/ModernCharacterDataProtocol
local/lito381/server/ModernCharacterDataRepository
local/lito381/server/ModernCharacterDataRepository$Source
local/lito381/server/ModernCharacterRecord
local/lito381/server/ModernCharacterRecord$Snapshot
local/lito381/server/ModernCharacterRecordJdbc
local/lito381/server/ModernCharacterRecordProtocol
local/lito381/server/ModernCharacterRecordProtocol$Context
local/lito381/server/ModernCharacterRecordProtocol$Reply
local/lito381/server/ModernCharacterRecordProtocol$Status
local/lito381/server/ModernCharacterRecordRepository
local/lito381/server/ModernCharacterRepository
local/lito381/server/ModernChat
local/lito381/server/ModernChat$Audience
local/lito381/server/ModernChat$Ledger
local/lito381/server/ModernChat$Log
local/lito381/server/ModernChat$Membership
local/lito381/server/ModernChat$Message
local/lito381/server/ModernChat$Result
local/lito381/server/ModernChat$Snapshot
local/lito381/server/ModernChat$Status
local/lito381/server/ModernChatProtocol
local/lito381/server/ModernChatProtocol$Action
local/lito381/server/ModernChatProtocol$Request
local/lito381/server/ModernCombatProfile
local/lito381/server/ModernCombatProfile$ArmorPolicy
local/lito381/server/ModernCombatProfile$Effective
local/lito381/server/ModernCombatProfile$Input
local/lito381/server/ModernCombatProfile$VitalPolicy
local/lito381/server/ModernCombatProfile$Weapon
local/lito381/server/ModernCombatProfile$WeaponFacts
local/lito381/server/ModernCombatProfile$WeaponState
local/lito381/server/ModernCombatProtocol
local/lito381/server/ModernCombatProtocol$Action
local/lito381/server/ModernCombatProtocol$Frame
local/lito381/server/ModernCombatProtocol$Reason
local/lito381/server/ModernCombatProtocol$Reply
local/lito381/server/ModernCombatProtocol$Request
local/lito381/server/ModernCombatProtocol$Status
local/lito381/server/ModernCombatProtocol$Vital
local/lito381/server/ModernCombatSourceJdbc
local/lito381/server/ModernCombatSourceJdbc$Collector
local/lito381/server/ModernCombatSourceJdbc$Source
local/lito381/server/ModernCombatant
local/lito381/server/ModernDamageSpellProtocol
local/lito381/server/ModernDamageSpellProtocol$State
local/lito381/server/ModernDeathmatchTeleportSpace
local/lito381/server/ModernDirectChat
local/lito381/server/ModernDirectChat$1
local/lito381/server/ModernDirectChat$Action
local/lito381/server/ModernDirectChat$Address
local/lito381/server/ModernDirectChat$Ledger
local/lito381/server/ModernDirectChat$Message
local/lito381/server/ModernDirectChat$Peer
local/lito381/server/ModernDirectChat$Preferences
local/lito381/server/ModernDirectChat$Request
local/lito381/server/ModernDirectChat$Result
local/lito381/server/ModernDirectChat$Snapshot
local/lito381/server/ModernDirectChat$Status
local/lito381/server/ModernDirectChatProtocol
local/lito381/server/ModernDirectChatProtocol$Request
local/lito381/server/ModernDirectChatProtocol$Target
local/lito381/server/ModernDisintegrate
local/lito381/server/ModernDisintegrate$CombatException
local/lito381/server/ModernDisintegrate$Failure
local/lito381/server/ModernDisintegrate$Outcome
local/lito381/server/ModernDisintegrate$Profile
local/lito381/server/ModernDisintegrateCatalog
local/lito381/server/ModernDisintegrateCatalog$Snapshot
local/lito381/server/ModernDisintegrateCatalog$Status
local/lito381/server/ModernDisintegrateProtocol
local/lito381/server/ModernDisintegrateProtocol$Reply
local/lito381/server/ModernDisintegrateRepository
local/lito381/server/ModernDisintegrateRepository$Source
local/lito381/server/ModernDisintegrateSkills
local/lito381/server/ModernDisintegrateState
local/lito381/server/ModernDisintegrateState$Hit
local/lito381/server/ModernDisintegrateState$Reason
local/lito381/server/ModernDisintegrateState$Request
local/lito381/server/ModernDisintegrateState$State
local/lito381/server/ModernEquipmentAuthority
local/lito381/server/ModernEquipmentAuthority$CombatLoaded
local/lito381/server/ModernEquipmentAuthority$CombatProvider
local/lito381/server/ModernEquipmentAuthority$Equipped
local/lito381/server/ModernEquipmentAuthority$Loaded
local/lito381/server/ModernEquipmentAuthority$MatchResult
local/lito381/server/ModernEquipmentAuthority$PhysicalLoaded
local/lito381/server/ModernEquipmentAuthority$PhysicalProvider
local/lito381/server/ModernEquipmentAuthority$Provider
local/lito381/server/ModernEquipmentAuthority$Request
local/lito381/server/ModernEquipmentAuthority$Result
local/lito381/server/ModernEquipmentAuthority$Snapshot
local/lito381/server/ModernEquipmentBinding
local/lito381/server/ModernEquipmentBinding$BindingException
local/lito381/server/ModernEquipmentBinding$Canonical
local/lito381/server/ModernEquipmentBinding$CatalogGeneration
local/lito381/server/ModernEquipmentBinding$Failure
local/lito381/server/ModernEquipmentCatalogJdbc
local/lito381/server/ModernEquipmentCatalogJdbc$1
local/lito381/server/ModernEquipmentCatalogJdbc$AbsenceKey
local/lito381/server/ModernEquipmentCatalogJdbc$Field
local/lito381/server/ModernEquipmentCatalogJdbc$MissingCatalogElement
local/lito381/server/ModernEquipmentCatalogJdbc$Reader
local/lito381/server/ModernEquipmentEvaluator
local/lito381/server/ModernEquipmentEvaluator$Evaluation
local/lito381/server/ModernEquipmentFacts
local/lito381/server/ModernEquipmentFacts$1
local/lito381/server/ModernEquipmentFacts$Ability
local/lito381/server/ModernEquipmentFacts$Catalog
local/lito381/server/ModernEquipmentFacts$ComboRule
local/lito381/server/ModernEquipmentFacts$Instance
local/lito381/server/ModernEquipmentFacts$Qualified
local/lito381/server/ModernEquipmentFacts$RuleKey
local/lito381/server/ModernEquipmentFacts$RuleKind
local/lito381/server/ModernEquipmentFacts$RuleValue
local/lito381/server/ModernEquipmentFacts$SetRule
local/lito381/server/ModernEquipmentFacts$Stat
local/lito381/server/ModernEquipmentFacts$Stone
local/lito381/server/ModernEquipmentFacts$Template
local/lito381/server/ModernEquipmentFacts$TemplatePhysical
local/lito381/server/ModernEquipmentFacts$VipTableState
local/lito381/server/ModernEquipmentHistory
local/lito381/server/ModernEquipmentHistory$Identity
local/lito381/server/ModernEquipmentHistory$Profile
local/lito381/server/ModernEquipmentHistory$State
local/lito381/server/ModernEquipmentHistoryWire
local/lito381/server/ModernEquipmentHistoryWire$1
local/lito381/server/ModernEquipmentLease
local/lito381/server/ModernEquipmentLease$Issuer
local/lito381/server/ModernEquipmentLease$Operation
local/lito381/server/ModernEquipmentOverrideStore
local/lito381/server/ModernEquipmentOverrideStore$1
local/lito381/server/ModernEquipmentOverrideStore$Commit
local/lito381/server/ModernEquipmentOverrideStore$CommitGuard
local/lito381/server/ModernEquipmentOverrideStore$Expected
local/lito381/server/ModernEquipmentOverrideStore$Failure
local/lito381/server/ModernEquipmentOverrideStore$FileOps
local/lito381/server/ModernEquipmentOverrideStore$Fingerprint
local/lito381/server/ModernEquipmentOverrideStore$ItemOverride
local/lito381/server/ModernEquipmentOverrideStore$Replacement
local/lito381/server/ModernEquipmentOverrideStore$StoreException
local/lito381/server/ModernEquipmentOverrideStore$Stored
local/lito381/server/ModernEquipmentProtocol
local/lito381/server/ModernEquipmentProtocol$1
local/lito381/server/ModernEquipmentReconciliation
local/lito381/server/ModernEquipmentReconciliation$Canonical
local/lito381/server/ModernEquipmentReconciliation$ExpectedBinding
local/lito381/server/ModernEquipmentReconciliation$Failure
local/lito381/server/ModernEquipmentReconciliation$ReconciliationException
local/lito381/server/ModernEquipmentReconciliation$Result
local/lito381/server/ModernEquipmentReconciliation$StaleItem
local/lito381/server/ModernEquipmentReconciliation$StaleReason
local/lito381/server/ModernEquipmentRepository
local/lito381/server/ModernEquipmentRepository$1
local/lito381/server/ModernEquipmentRepository$Source
local/lito381/server/ModernEquipmentSourceJdbc
local/lito381/server/ModernEquipmentSourceJdbc$MissingOptionalColumn
local/lito381/server/ModernEquipmentSourceJdbc$Source
local/lito381/server/ModernEquipmentVitals
local/lito381/server/ModernEquipmentVitals$Vitals
local/lito381/server/ModernFilteredChat
local/lito381/server/ModernFilteredChat$Action
local/lito381/server/ModernFilteredChat$Kind
local/lito381/server/ModernFilteredChat$Ledger
local/lito381/server/ModernFilteredChat$Message
local/lito381/server/ModernFilteredChat$Receipt
local/lito381/server/ModernFilteredChat$Request
local/lito381/server/ModernFilteredChat$Result
local/lito381/server/ModernFilteredChat$Snapshot
local/lito381/server/ModernFilteredChat$SpeechPolicy
local/lito381/server/ModernFilteredChat$Status
local/lito381/server/ModernFilteredChat$Token
local/lito381/server/ModernFilteredChatProtocol
local/lito381/server/ModernFireStorm
local/lito381/server/ModernFireStorm$CombatException
local/lito381/server/ModernFireStorm$Failure
local/lito381/server/ModernFireStorm$Outcome
local/lito381/server/ModernFireStorm$Profile
local/lito381/server/ModernFireStorm$Values
local/lito381/server/ModernFireStormProtocol
local/lito381/server/ModernFireStormProtocol$State
local/lito381/server/ModernFireStormProtocol$Target
local/lito381/server/ModernGmCommandJdbc
local/lito381/server/ModernGmCommandProtocol
local/lito381/server/ModernGmCommandProtocol$Delivery
local/lito381/server/ModernGmCommandProtocol$Reply
local/lito381/server/ModernGmCommandProtocol$Request
local/lito381/server/ModernGmCommandRepository
local/lito381/server/ModernGmCommandRepository$Source
local/lito381/server/ModernGmCommands
local/lito381/server/ModernGmCommands$Decision
local/lito381/server/ModernGmCommands$Definition
local/lito381/server/ModernGmCommands$Snapshot
local/lito381/server/ModernGmCommands$SpawnRequest
local/lito381/server/ModernGreaterHaste
local/lito381/server/ModernGreaterHaste$Profile
local/lito381/server/ModernGreaterHasteCast
local/lito381/server/ModernGreaterHasteCast$Reason
local/lito381/server/ModernGreaterHasteCast$Request
local/lito381/server/ModernGreaterHasteCatalog
local/lito381/server/ModernGreaterHasteCatalog$Snapshot
local/lito381/server/ModernGreaterHasteCatalog$Status
local/lito381/server/ModernGreaterHasteProtocol
local/lito381/server/ModernGreaterHasteProtocol$Reply
local/lito381/server/ModernGreaterHasteProtocol$Request
local/lito381/server/ModernGreaterHasteRepository
local/lito381/server/ModernGreaterHasteRepository$Source
local/lito381/server/ModernGreaterHasteSkills
local/lito381/server/ModernHaste
local/lito381/server/ModernHaste$Profile
local/lito381/server/ModernHasteCast
local/lito381/server/ModernHasteCast$Npc
local/lito381/server/ModernHasteCast$Player
local/lito381/server/ModernHasteCast$Reason
local/lito381/server/ModernHasteCast$Request
local/lito381/server/ModernHasteCast$Target
local/lito381/server/ModernHasteCatalog
local/lito381/server/ModernHasteCatalog$Snapshot
local/lito381/server/ModernHasteCatalog$Status
local/lito381/server/ModernHasteEffects
local/lito381/server/ModernHasteEffects$Entry
local/lito381/server/ModernHasteEffects$Grant
local/lito381/server/ModernHasteEffects$Motion
local/lito381/server/ModernHasteEffects$Npc
local/lito381/server/ModernHasteEffects$Npcs
local/lito381/server/ModernHasteEffects$Player
local/lito381/server/ModernHasteEffects$State
local/lito381/server/ModernHasteEffects$Target
local/lito381/server/ModernHasteProtocol
local/lito381/server/ModernHasteProtocol$Reply
local/lito381/server/ModernHasteProtocol$Request
local/lito381/server/ModernHasteReceipt
local/lito381/server/ModernHasteReceipt$Npc
local/lito381/server/ModernHasteReceipt$Player
local/lito381/server/ModernHasteReceipt$Target
local/lito381/server/ModernHasteRepository
local/lito381/server/ModernHasteRepository$Source
local/lito381/server/ModernHasteSkills
local/lito381/server/ModernHasteTargets
local/lito381/server/ModernHasteTargets$Entry
local/lito381/server/ModernHasteTargets$Key
local/lito381/server/ModernHasteTargets$Npc
local/lito381/server/ModernHasteTargets$Player
local/lito381/server/ModernHasteTargets$State
local/lito381/server/ModernHonor
local/lito381/server/ModernHonor$Config
local/lito381/server/ModernHonor$Heal
local/lito381/server/ModernHonor$Life
local/lito381/server/ModernHonor$Objective
local/lito381/server/ModernHonor$Snapshot
local/lito381/server/ModernHonor$Tower
local/lito381/server/ModernHonorHealing
local/lito381/server/ModernHonorHealing$Growth
local/lito381/server/ModernHonorHealing$Outcome
local/lito381/server/ModernHonorHealing$Profile
local/lito381/server/ModernHonorHealing$Row
local/lito381/server/ModernHonorHealingJdbc
local/lito381/server/ModernHonorProtocol
local/lito381/server/ModernIncomingDamage
local/lito381/server/ModernIncomingDamage$Impact
local/lito381/server/ModernIncomingDamage$Protection
local/lito381/server/ModernIncomingDamage$Resolved
local/lito381/server/ModernInventoryWeight
local/lito381/server/ModernInventoryWeight$Basis
local/lito381/server/ModernInventoryWeight$Carried
local/lito381/server/ModernInventoryWeight$Parameters
local/lito381/server/ModernInventoryWeight$Snapshot
local/lito381/server/ModernInventoryWeight$Unit
local/lito381/server/ModernInventoryWeight$View
local/lito381/server/ModernItemLifetime
local/lito381/server/ModernItemLifetime$Fact
local/lito381/server/ModernItemLifetime$Observation
local/lito381/server/ModernItemLifetime$State
local/lito381/server/ModernItemLifetime$View
local/lito381/server/ModernItemLifetimeJdbc
local/lito381/server/ModernItemProtocol
local/lito381/server/ModernItemProtocol$Action
local/lito381/server/ModernItemProtocol$HealingKind
local/lito381/server/ModernItemProtocol$Reason
local/lito381/server/ModernItemProtocol$Reply
local/lito381/server/ModernItemProtocol$Request
local/lito381/server/ModernItemProtocol$State
local/lito381/server/ModernItemProtocol$Status
local/lito381/server/ModernItemProtocol$Stock
local/lito381/server/ModernItemRuntime
local/lito381/server/ModernItemRuntime$State
local/lito381/server/ModernJdbcBackend
local/lito381/server/ModernJdbcBackend$Attempt
local/lito381/server/ModernJdbcBackend$Connector
local/lito381/server/ModernJdbcBackend$Read
local/lito381/server/ModernJdbcBackend$ScopedSocketFactory
local/lito381/server/ModernJdbcNumbers
local/lito381/server/ModernLargeMaps
local/lito381/server/ModernLegacyBattleProjection
local/lito381/server/ModernLegacySpellProjection
local/lito381/server/ModernLegacySpellProjection$Projection
local/lito381/server/ModernMagicCombat
local/lito381/server/ModernMagicCombat$CombatException
local/lito381/server/ModernMagicCombat$Defense
local/lito381/server/ModernMagicCombat$Failure
local/lito381/server/ModernMagicCombat$Fighter
local/lito381/server/ModernMagicCombat$Growth
local/lito381/server/ModernMagicCombat$Offense
local/lito381/server/ModernMagicCombat$Outcome
local/lito381/server/ModernMagicCombat$UnsupportedReason
local/lito381/server/ModernMagicCombat$Values
local/lito381/server/ModernMagicCombat$WisdomRow
local/lito381/server/ModernMagicCombatSourceJdbc
local/lito381/server/ModernMain
local/lito381/server/ModernMain$QualifiedWorldPolicy
local/lito381/server/ModernMatchDescriptor
local/lito381/server/ModernMatchDescriptor$Ruleset
local/lito381/server/ModernMatchOutbox
local/lito381/server/ModernMatchOutbox$1
local/lito381/server/ModernMatchOutbox$Failure
local/lito381/server/ModernMatchOutbox$FileOps
local/lito381/server/ModernMatchOutbox$OutboxException
local/lito381/server/ModernMatchOutbox$Snapshot
local/lito381/server/ModernMatchSettlement
local/lito381/server/ModernMatchSettlement$Event
local/lito381/server/ModernMatchSettlement$Failure
local/lito381/server/ModernMatchSettlement$JournaledStore
local/lito381/server/ModernMatchSettlement$Participant
local/lito381/server/ModernMatchSettlement$ReceiptState
local/lito381/server/ModernMatchSettlement$Reservation
local/lito381/server/ModernMatchSettlement$SettlementException
local/lito381/server/ModernMatchSettlement$Store
local/lito381/server/ModernMatchSettlement$Verdict
local/lito381/server/ModernMatchSettlementJdbc
local/lito381/server/ModernMatchSettlementJdbc$Column
local/lito381/server/ModernMatchSettlementJdbc$ConnectionFactory
local/lito381/server/ModernMatchmaking
local/lito381/server/ModernMatchmaking$Criteria
local/lito381/server/ModernMatchmaking$Distance
local/lito381/server/ModernMatchmaking$Result
local/lito381/server/ModernMatchmaking$State
local/lito381/server/ModernMatchmakingProtocol
local/lito381/server/ModernMatchmakingProtocol$1
local/lito381/server/ModernMatchmakingProtocol$Action
local/lito381/server/ModernMatchmakingProtocol$Reply
local/lito381/server/ModernMatchmakingProtocol$Request
local/lito381/server/ModernMatchmakingProtocol$Status
local/lito381/server/ModernNativeActorWorldProtocol
local/lito381/server/ModernNativeActorWorldProtocol$Identity
local/lito381/server/ModernNativeActorWorldProtocol$Request
local/lito381/server/ModernNativeActorWorldProtocol$Result
local/lito381/server/ModernNativeCanonicalWorldProtocol
local/lito381/server/ModernNativeCanonicalWorldProtocol$Request
local/lito381/server/ModernNativeCanonicalWorldProtocol$Result
local/lito381/server/ModernNativeCanonicalWorldProtocol$SourceAttachment
local/lito381/server/ModernNativeCatalogWorldProtocol
local/lito381/server/ModernNativeCatalogWorldProtocol$Catalog
local/lito381/server/ModernNativeCatalogWorldProtocol$Request
local/lito381/server/ModernNativeCatalogWorldProtocol$Result
local/lito381/server/ModernNativeResourceLedger
local/lito381/server/ModernNativeResourceLedger$Binding
local/lito381/server/ModernNativeResourceLedger$Generation
local/lito381/server/ModernNativeResourceLedger$LoadAdmission
local/lito381/server/ModernNativeResourceLedger$Pending
local/lito381/server/ModernNativeResourceLedger$PrepareAdmission
local/lito381/server/ModernNaturalRegen
local/lito381/server/ModernNaturalRegen$Activity
local/lito381/server/ModernNaturalRegen$Clock
local/lito381/server/ModernNaturalRegen$ConRow
local/lito381/server/ModernNaturalRegen$Counter
local/lito381/server/ModernNaturalRegen$Due
local/lito381/server/ModernNaturalRegen$Growth
local/lito381/server/ModernNaturalRegen$Outcome
local/lito381/server/ModernNaturalRegen$Profile
local/lito381/server/ModernNaturalRegen$UnsupportedReason
local/lito381/server/ModernNaturalRegen$Values
local/lito381/server/ModernNaturalRegenSourceJdbc
local/lito381/server/ModernNavigationSession
local/lito381/server/ModernNavigationSession$1
local/lito381/server/ModernNavigationSession$10
local/lito381/server/ModernNavigationSession$11
local/lito381/server/ModernNavigationSession$2
local/lito381/server/ModernNavigationSession$3
local/lito381/server/ModernNavigationSession$4
local/lito381/server/ModernNavigationSession$5
local/lito381/server/ModernNavigationSession$6
local/lito381/server/ModernNavigationSession$7
local/lito381/server/ModernNavigationSession$8
local/lito381/server/ModernNavigationSession$9
local/lito381/server/ModernNavigationSession$EquipmentAction
local/lito381/server/ModernNavigationSession$EquipmentSelection
local/lito381/server/ModernNpcBattle
local/lito381/server/ModernNpcBattle$Config
local/lito381/server/ModernNpcBattle$Direction
local/lito381/server/ModernNpcBattle$Exchange
local/lito381/server/ModernNpcBattle$Frame
local/lito381/server/ModernNpcBattle$Reason
local/lito381/server/ModernNpcBattle$Result
local/lito381/server/ModernNpcCatalog
local/lito381/server/ModernNpcCatalog$1
local/lito381/server/ModernNpcCatalog$Catalog
local/lito381/server/ModernNpcCatalog$Facts
local/lito381/server/ModernNpcCatalog$Field
local/lito381/server/ModernNpcCatalog$Key
local/lito381/server/ModernNpcCatalog$Npc
local/lito381/server/ModernNpcCatalog$Pending
local/lito381/server/ModernNpcCatalogJdbc
local/lito381/server/ModernNpcConfiguration
local/lito381/server/ModernNpcConfiguration$Spawn
local/lito381/server/ModernNpcConfiguration$Spec
local/lito381/server/ModernNpcDisintegrate
local/lito381/server/ModernNpcDisintegrate$Incoming
local/lito381/server/ModernNpcDisintegrateProtocol
local/lito381/server/ModernNpcDisintegrateProtocol$Reply
local/lito381/server/ModernNpcDisintegrateState
local/lito381/server/ModernNpcDisintegrateState$Hit
local/lito381/server/ModernNpcDisintegrateState$Reason
local/lito381/server/ModernNpcDisintegrateState$Request
local/lito381/server/ModernNpcDisintegrateState$Result
local/lito381/server/ModernNpcDisintegrateState$State
local/lito381/server/ModernNpcDisintegrateWire
local/lito381/server/ModernNpcEncounter
local/lito381/server/ModernNpcFireStorm
local/lito381/server/ModernNpcFireStorm$Hit
local/lito381/server/ModernNpcFireStorm$Incoming
local/lito381/server/ModernNpcFireStormFrame
local/lito381/server/ModernNpcFireStormProtocol
local/lito381/server/ModernNpcFireStormProtocol$Reply
local/lito381/server/ModernNpcFireStormState
local/lito381/server/ModernNpcFireStormState$Reason
local/lito381/server/ModernNpcFireStormState$Request
local/lito381/server/ModernNpcFireStormState$Result
local/lito381/server/ModernNpcFireStormState$State
local/lito381/server/ModernNpcFireStormState$Target
local/lito381/server/ModernNpcFireStormStructures
local/lito381/server/ModernNpcFireStormStructures$Hit
local/lito381/server/ModernNpcFireStormStructures$QuestHit
local/lito381/server/ModernNpcFireStormStructures$Receipt
local/lito381/server/ModernNpcFireStormStructures$TowerHit
local/lito381/server/ModernNpcFireStormWire
local/lito381/server/ModernNpcHaste
local/lito381/server/ModernNpcHaste$Incoming
local/lito381/server/ModernNpcMagic
local/lito381/server/ModernNpcMagic$CastState
local/lito381/server/ModernNpcMagic$Incoming
local/lito381/server/ModernNpcMagic$Reason
local/lito381/server/ModernNpcMagic$Result
local/lito381/server/ModernNpcMelee
local/lito381/server/ModernNpcMelee$BlessingBinding
local/lito381/server/ModernNpcMelee$BlessingConvention
local/lito381/server/ModernNpcMelee$CombatException
local/lito381/server/ModernNpcMelee$Failure
local/lito381/server/ModernNpcMelee$Incoming
local/lito381/server/ModernNpcMelee$Outcome
local/lito381/server/ModernNpcMelee$Player
local/lito381/server/ModernNpcMelee$Victim
local/lito381/server/ModernNpcMotion
local/lito381/server/ModernNpcMotion$Pace
local/lito381/server/ModernNpcMotion$Payload
local/lito381/server/ModernNpcMotion$State
local/lito381/server/ModernNpcMotion$View
local/lito381/server/ModernNpcMotion$Walk
local/lito381/server/ModernNpcNames
local/lito381/server/ModernNpcNames$Name
local/lito381/server/ModernNpcNames$Table
local/lito381/server/ModernNpcProtocol
local/lito381/server/ModernNpcProtocol$Action
local/lito381/server/ModernNpcProtocol$Actor
local/lito381/server/ModernNpcProtocol$Frame
local/lito381/server/ModernNpcProtocol$Reason
local/lito381/server/ModernNpcProtocol$Reply
local/lito381/server/ModernNpcProtocol$Request
local/lito381/server/ModernNpcProtocol$Resources
local/lito381/server/ModernNpcProtocol$Status
local/lito381/server/ModernNpcRoster
local/lito381/server/ModernNpcRoster$Action
local/lito381/server/ModernNpcRoster$Actor
local/lito381/server/ModernNpcRoster$Id
local/lito381/server/ModernNpcRoster$PathNode
local/lito381/server/ModernNpcRoster$Plan
local/lito381/server/ModernNpcRoster$PlanKind
local/lito381/server/ModernNpcRoster$Pursuit
local/lito381/server/ModernNpcRoster$Scope
local/lito381/server/ModernNpcRoster$Snapshot
local/lito381/server/ModernNpcRoster$Spawn
local/lito381/server/ModernNpcRoster$Target
local/lito381/server/ModernNpcRoster$Template
local/lito381/server/ModernNpcSlow
local/lito381/server/ModernNpcSlow$Incoming
local/lito381/server/ModernNpcSpeed
local/lito381/server/ModernNpcSpeed$State
local/lito381/server/ModernNpcSpellProtocol
local/lito381/server/ModernNpcSpellProtocol$Reply
local/lito381/server/ModernNpcSpellProtocol$Request
local/lito381/server/ModernParticipantId
local/lito381/server/ModernParticipantId$Kind
local/lito381/server/ModernPetRace
local/lito381/server/ModernPetRace$Course
local/lito381/server/ModernPetRace$Dnf
local/lito381/server/ModernPetRace$Entrant
local/lito381/server/ModernPetRace$Gate
local/lito381/server/ModernPetRace$Phase
local/lito381/server/ModernPetRace$Runner
local/lito381/server/ModernPetRace$State
local/lito381/server/ModernPetRace$Work
local/lito381/server/ModernPetRaceOutbox
local/lito381/server/ModernPetRaceOutbox$1
local/lito381/server/ModernPetRaceOutbox$Failure
local/lito381/server/ModernPetRaceOutbox$FileOps
local/lito381/server/ModernPetRaceOutbox$OutboxException
local/lito381/server/ModernPetRaceOutbox$Snapshot
local/lito381/server/ModernPetRaceProtocol
local/lito381/server/ModernPetRaceProtocol$1
local/lito381/server/ModernPetRaceProtocol$ResultRow
local/lito381/server/ModernPetRaceProtocol$Settlement
local/lito381/server/ModernPetRaceProtocol$Snapshot
local/lito381/server/ModernPetRaceSettlement
local/lito381/server/ModernPetRaceSettlement$Event
local/lito381/server/ModernPetRaceSettlement$Failure
local/lito381/server/ModernPetRaceSettlement$JournaledStore
local/lito381/server/ModernPetRaceSettlement$Reservation
local/lito381/server/ModernPetRaceSettlement$SettlementException
local/lito381/server/ModernPetRaceSettlement$Store
local/lito381/server/ModernPetRaceSettlementCoordinator
local/lito381/server/ModernPetRaceSettlementCoordinator$Task
local/lito381/server/ModernPetRaceSettlementCoordinator$Token
local/lito381/server/ModernPetRaceSettlementJdbc
local/lito381/server/ModernPetRaceSettlementJdbc$Column
local/lito381/server/ModernPetRaceSettlementJdbc$ConnectionFactory
local/lito381/server/ModernPhysicalCombat
local/lito381/server/ModernPhysicalCombat$BoundedRandom
local/lito381/server/ModernPhysicalCombat$CombatException
local/lito381/server/ModernPhysicalCombat$Defense
local/lito381/server/ModernPhysicalCombat$Failure
local/lito381/server/ModernPhysicalCombat$Fighter
local/lito381/server/ModernPhysicalCombat$Growth
local/lito381/server/ModernPhysicalCombat$GrowthRow
local/lito381/server/ModernPhysicalCombat$Offense
local/lito381/server/ModernPhysicalCombat$Outcome
local/lito381/server/ModernPhysicalCombat$Rational
local/lito381/server/ModernPhysicalCombat$Source
local/lito381/server/ModernPhysicalCombat$TemplatePolicy
local/lito381/server/ModernPhysicalCombat$UnsupportedReason
local/lito381/server/ModernPhysicalCombat$Values
local/lito381/server/ModernPhysicalCombatSourceJdbc
local/lito381/server/ModernPhysicalCombatSourceJdbc$1
local/lito381/server/ModernPhysicalCombatSourceJdbc$BowRows
local/lito381/server/ModernPlayerHealing
local/lito381/server/ModernPlayerHealing$Effects
local/lito381/server/ModernPlayerHealing$Outcome
local/lito381/server/ModernPlayerHealing$Profile
local/lito381/server/ModernPlayerHealingCast
local/lito381/server/ModernPlayerHealingCast$Reason
local/lito381/server/ModernPlayerHealingCast$Request
local/lito381/server/ModernPlayerHealingCast$Target
local/lito381/server/ModernPlayerHealingHistory
local/lito381/server/ModernPlayerHealingHistory$Entry
local/lito381/server/ModernPlayerHealingHistory$Impact
local/lito381/server/ModernPlayerHealingHistory$Incoming
local/lito381/server/ModernPlayerHealingHistory$Outgoing
local/lito381/server/ModernPlayerHealingProtocol
local/lito381/server/ModernPlayerHealingProtocol$Reply
local/lito381/server/ModernPlayerHealingProtocol$Request
local/lito381/server/ModernPlayerHealingTransaction
local/lito381/server/ModernPlayerHealingTransaction$Actor
local/lito381/server/ModernPlayerHealingTransaction$Complete
local/lito381/server/ModernPlayerHealingTransaction$Prepared
local/lito381/server/ModernPlayerHealingTransaction$Receipt
local/lito381/server/ModernPlayerHealingTransaction$State
local/lito381/server/ModernQuestConfiguration
local/lito381/server/ModernQuestConfiguration$Spec
local/lito381/server/ModernQuestLife
local/lito381/server/ModernQuestLife$Reason
local/lito381/server/ModernQuestLife$Result
local/lito381/server/ModernQuestLife$State
local/lito381/server/ModernQuestObjectives
local/lito381/server/ModernQuestObjectives$Actor
local/lito381/server/ModernQuestObjectives$Config
local/lito381/server/ModernQuestObjectives$Exchange
local/lito381/server/ModernQuestObjectives$Id
local/lito381/server/ModernQuestObjectives$Kind
local/lito381/server/ModernQuestObjectives$Snapshot
local/lito381/server/ModernQuestObjectives$Target
local/lito381/server/ModernQuestProgress
local/lito381/server/ModernQuestProgress$Flow
local/lito381/server/ModernQuestProgress$Lane
local/lito381/server/ModernQuestProgress$Phase
local/lito381/server/ModernQuestProgress$Plan
local/lito381/server/ModernQuestProgress$SpawnStep
local/lito381/server/ModernQuestProgress$State
local/lito381/server/ModernQuestProgress$Status
local/lito381/server/ModernQuestProgress$Wave
local/lito381/server/ModernQuestProtocol
local/lito381/server/ModernQuestProtocol$Actor
local/lito381/server/ModernQuestProtocol$Data
local/lito381/server/ModernQuestProtocol$Exchange
local/lito381/server/ModernQuestProtocol$Lane
local/lito381/server/ModernQuestSpace
local/lito381/server/ModernQuestSpace$Cell
local/lito381/server/ModernQuestVisibility
local/lito381/server/ModernRangedCombat
local/lito381/server/ModernRangedCombat$Fighter
local/lito381/server/ModernRangedCombat$Offense
local/lito381/server/ModernRangedCombat$Unsupported
local/lito381/server/ModernRangedCombat$Values
local/lito381/server/ModernRegenProtocol
local/lito381/server/ModernRegenProtocol$State
local/lito381/server/ModernRoomProtocol
local/lito381/server/ModernRoomProtocol$1
local/lito381/server/ModernRoomProtocol$Echo
local/lito381/server/ModernRooms
local/lito381/server/ModernRooms$1
local/lito381/server/ModernRooms$Action
local/lito381/server/ModernRooms$Binding
local/lito381/server/ModernRooms$BotResult
local/lito381/server/ModernRooms$Command
local/lito381/server/ModernRooms$Config
local/lito381/server/ModernRooms$Draft
local/lito381/server/ModernRooms$FrozenPreparation
local/lito381/server/ModernRooms$FrozenPreparation$Guard
local/lito381/server/ModernRooms$MatchWaiter
local/lito381/server/ModernRooms$Member
local/lito381/server/ModernRooms$Option
local/lito381/server/ModernRooms$Participant
local/lito381/server/ModernRooms$PreparationResult
local/lito381/server/ModernRooms$Reason
local/lito381/server/ModernRooms$Result
local/lito381/server/ModernRooms$Room
local/lito381/server/ModernRooms$RoomSnapshot
local/lito381/server/ModernRooms$Scope
local/lito381/server/ModernRooms$Seat
local/lito381/server/ModernRooms$Status
local/lito381/server/ModernRooms$Summary
local/lito381/server/ModernRooms$View
local/lito381/server/ModernServer
local/lito381/server/ModernServer$Admission
local/lito381/server/ModernServer$Admission$Bucket
local/lito381/server/ModernServer$Limits
local/lito381/server/ModernServer$SessionTask
local/lito381/server/ModernServer$Snapshot
local/lito381/server/ModernServerConfig
local/lito381/server/ModernServerConfig$ConfigException
local/lito381/server/ModernServerConfig$DbConfig
local/lito381/server/ModernServerConfig$Failure
local/lito381/server/ModernServerConfig$Limits
local/lito381/server/ModernServerConfig$PropertyFailure
local/lito381/server/ModernServerConfig$SettlementConfig
local/lito381/server/ModernServerConfig$SettlementInput
local/lito381/server/ModernServerConfig$SourceConfig
local/lito381/server/ModernServerConfig$SourceInput
local/lito381/server/ModernServerConfig$StrictProperties
local/lito381/server/ModernServerConfig$TlsConfig
local/lito381/server/ModernServerConfig$WorldPolicyConfig
local/lito381/server/ModernServerConfig$WorldPolicyInput
local/lito381/server/ModernServerConsole
local/lito381/server/ModernServerConsole$1
local/lito381/server/ModernServerConsole$Command
local/lito381/server/ModernSessionBoundary
local/lito381/server/ModernSessionBoundary$Negotiated
local/lito381/server/ModernSessionBoundary$Policy
local/lito381/server/ModernSessionBoundary$State
local/lito381/server/ModernSessionExtensions
local/lito381/server/ModernSessionExtensions$Offer
local/lito381/server/ModernSessionFailures
local/lito381/server/ModernSessionFailures$Count
local/lito381/server/ModernSessionFailures$Kind
local/lito381/server/ModernSessionFailures$Point
local/lito381/server/ModernSessionFailures$Stage
local/lito381/server/ModernSessionProtocol
local/lito381/server/ModernSessionProtocol$Ack
local/lito381/server/ModernSessionProtocol$ErrorCode
local/lito381/server/ModernSessionProtocol$ExtendedAck
local/lito381/server/ModernSessionProtocol$ExtendedHello
local/lito381/server/ModernSessionProtocol$Hello
local/lito381/server/ModernSessionProtocol$Message
local/lito381/server/ModernSessionProtocol$ProtocolException
local/lito381/server/ModernSettlementConnections
local/lito381/server/ModernSettlementConnections$1
local/lito381/server/ModernSettlementConnections$Connector
local/lito381/server/ModernSettlementConnections$Entry
local/lito381/server/ModernSettlementConnections$ScopedSocketFactory
local/lito381/server/ModernSettlementCoordinator
local/lito381/server/ModernSettlementCoordinator$Task
local/lito381/server/ModernSettlementCoordinator$Token
local/lito381/server/ModernSharedDeathmatchPreset
local/lito381/server/ModernSharedSiegePreset
local/lito381/server/ModernShopAuthority
local/lito381/server/ModernShopAuthority$1
local/lito381/server/ModernShopAuthority$Action
local/lito381/server/ModernShopAuthority$Outcome
local/lito381/server/ModernShopAuthority$Owned
local/lito381/server/ModernShopAuthority$Product
local/lito381/server/ModernShopAuthority$Reason
local/lito381/server/ModernShopAuthority$Snapshot
local/lito381/server/ModernShopAuthority$Stack
local/lito381/server/ModernShopAuthority$Status
local/lito381/server/ModernShopAuthority$TimedCatalog
local/lito381/server/ModernShopCatalog
local/lito381/server/ModernShopCatalog$1
local/lito381/server/ModernShopCatalog$Arrow
local/lito381/server/ModernShopCatalog$Etc
local/lito381/server/ModernShopCatalog$Healing
local/lito381/server/ModernShopCatalog$Offer
local/lito381/server/ModernShopCatalog$Snapshot
local/lito381/server/ModernShopCatalog$Support
local/lito381/server/ModernShopCatalog$Template
local/lito381/server/ModernShopCatalog$Timed
local/lito381/server/ModernShopCatalogJdbc
local/lito381/server/ModernShopCatalogRepository
local/lito381/server/ModernShopCatalogRepository$Source
local/lito381/server/ModernShopProtocol
local/lito381/server/ModernShopProtocol$Reply
local/lito381/server/ModernShopProtocol$Request
local/lito381/server/ModernSiege
local/lito381/server/ModernSiege$Config
local/lito381/server/ModernSiege$Life
local/lito381/server/ModernSiege$Objective
local/lito381/server/ModernSiege$Phase
local/lito381/server/ModernSiege$Portal
local/lito381/server/ModernSiege$PortalReason
local/lito381/server/ModernSiege$Scope
local/lito381/server/ModernSiege$Slot
local/lito381/server/ModernSiege$Snapshot
local/lito381/server/ModernSiege$Space
local/lito381/server/ModernSiege$State
local/lito381/server/ModernSiege$Target
local/lito381/server/ModernSiege$Tower
local/lito381/server/ModernSiegeConfiguration
local/lito381/server/ModernSiegeProtocol
local/lito381/server/ModernSiegeProtocol$Action
local/lito381/server/ModernSiegeProtocol$Data
local/lito381/server/ModernSiegeProtocol$Reason
local/lito381/server/ModernSiegeProtocol$Receipt
local/lito381/server/ModernSiegeProtocol$Reply
local/lito381/server/ModernSiegeProtocol$Request
local/lito381/server/ModernSiegeSettlement
local/lito381/server/ModernSiegeSettlement$End
local/lito381/server/ModernSiegeSettlement$Proof
local/lito381/server/ModernSkillSpending
local/lito381/server/ModernSkillSpending$Ledger
local/lito381/server/ModernSkillSpending$Totals
local/lito381/server/ModernSkills
local/lito381/server/ModernSkills$Skill
local/lito381/server/ModernSkills$Snapshot
local/lito381/server/ModernSkills$Support
local/lito381/server/ModernSkillsJdbc
local/lito381/server/ModernSkillsJdbc$AbsentColumn
local/lito381/server/ModernSkillsRepository
local/lito381/server/ModernSkillsRepository$Source
local/lito381/server/ModernSlow
local/lito381/server/ModernSlow$Profile
local/lito381/server/ModernSlowCast
local/lito381/server/ModernSlowCast$Npc
local/lito381/server/ModernSlowCast$Player
local/lito381/server/ModernSlowCast$Reason
local/lito381/server/ModernSlowCast$Request
local/lito381/server/ModernSlowCast$Target
local/lito381/server/ModernSlowCatalog
local/lito381/server/ModernSlowCatalog$Snapshot
local/lito381/server/ModernSlowCatalog$Status
local/lito381/server/ModernSlowNativeRules
local/lito381/server/ModernSlowNativeRules$MissingEffect
local/lito381/server/ModernSlowProtocol
local/lito381/server/ModernSlowProtocol$Reply
local/lito381/server/ModernSlowProtocol$Request
local/lito381/server/ModernSlowReceipt
local/lito381/server/ModernSlowReceipt$Npc
local/lito381/server/ModernSlowReceipt$Outcome
local/lito381/server/ModernSlowReceipt$Player
local/lito381/server/ModernSlowReceipt$Target
local/lito381/server/ModernSlowRepository
local/lito381/server/ModernSlowRepository$Source
local/lito381/server/ModernSlowSkills
local/lito381/server/ModernSlowTargets
local/lito381/server/ModernSlowTargets$Entry
local/lito381/server/ModernSlowTargets$Key
local/lito381/server/ModernSlowTargets$Npc
local/lito381/server/ModernSlowTargets$Player
local/lito381/server/ModernSlowTargets$State
local/lito381/server/ModernSlowTransaction
local/lito381/server/ModernSlowTransaction$Prepared
local/lito381/server/ModernSlowTransaction$State
local/lito381/server/ModernSpeechAuthority
local/lito381/server/ModernSpeechAuthority$Source
local/lito381/server/ModernSpeechAuthority$Timers
local/lito381/server/ModernSpeedEffects
local/lito381/server/ModernSpeedEffects$Effect
local/lito381/server/ModernSpeedEffects$Entry
local/lito381/server/ModernSpeedEffects$Grant
local/lito381/server/ModernSpeedEffects$Motion
local/lito381/server/ModernSpeedEffects$Npc
local/lito381/server/ModernSpeedEffects$NpcProof
local/lito381/server/ModernSpeedEffects$Npcs
local/lito381/server/ModernSpeedEffects$Player
local/lito381/server/ModernSpeedEffects$PlayerProof
local/lito381/server/ModernSpeedEffects$Proof
local/lito381/server/ModernSpeedEffects$State
local/lito381/server/ModernSpeedEffects$Target
local/lito381/server/ModernSpellMaterials
local/lito381/server/ModernSpellMaterials$Facts
local/lito381/server/ModernSpellMaterials$State
local/lito381/server/ModernSpellMaterials$Stock
local/lito381/server/ModernSpellProtocol
local/lito381/server/ModernSpellProtocol$CastState
local/lito381/server/ModernSpellProtocol$Context
local/lito381/server/ModernSpellProtocol$Reason
local/lito381/server/ModernSpellProtocol$Reply
local/lito381/server/ModernSpellProtocol$Request
local/lito381/server/ModernSpellProtocol$SkillsReply
local/lito381/server/ModernSpellProtocol$Status
local/lito381/server/ModernSpellRuntime
local/lito381/server/ModernSpellRuntime$Arrow
local/lito381/server/ModernSpellRuntime$Detail
local/lito381/server/ModernSpellRuntime$Disintegrate
local/lito381/server/ModernSpellRuntime$FireStorm
local/lito381/server/ModernSpellRuntime$GreaterHaste
local/lito381/server/ModernSpellRuntime$GreaterHasteNeutralized
local/lito381/server/ModernSpellRuntime$NpcFireStorm
local/lito381/server/ModernSpellRuntime$Ordinary
local/lito381/server/ModernSpellRuntime$PlayerHealingReceipt
local/lito381/server/ModernSpellRuntime$Receipt
local/lito381/server/ModernSpellRuntime$Snapshot
local/lito381/server/ModernSpellRuntime$Stamp
local/lito381/server/ModernSpellRuntime$State
local/lito381/server/ModernSpellRuntime$Structure
local/lito381/server/ModernSpellRuntime$Teleport
local/lito381/server/ModernStandaloneMain
local/lito381/server/ModernTargetHistory
local/lito381/server/ModernTargetHistory$1
local/lito381/server/ModernTargetHistory$Batch
local/lito381/server/ModernTargetHistory$Effect
local/lito381/server/ModernTargetHistory$Health
local/lito381/server/ModernTargetHistory$History
local/lito381/server/ModernTargetHistory$Honor
local/lito381/server/ModernTargetHistory$Kind
local/lito381/server/ModernTargetHistory$Npc
local/lito381/server/ModernTargetHistory$Participant
local/lito381/server/ModernTargetHistory$Quest
local/lito381/server/ModernTargetHistory$Target
local/lito381/server/ModernTargetHistory$TimeTower
local/lito381/server/ModernTargetHistory$World
local/lito381/server/ModernTimeAttack
local/lito381/server/ModernTimeAttack$End
local/lito381/server/ModernTimeAttack$Exchange
local/lito381/server/ModernTimeAttack$Kind
local/lito381/server/ModernTimeAttack$Phase
local/lito381/server/ModernTimeAttack$Plan
local/lito381/server/ModernTimeAttack$Snapshot
local/lito381/server/ModernTimeAttack$SpawnStep
local/lito381/server/ModernTimeAttack$State
local/lito381/server/ModernTimeAttack$Tower
local/lito381/server/ModernTimeAttack$TowerId
local/lito381/server/ModernTimeAttack$Wave
local/lito381/server/ModernTimeAttackConfiguration
local/lito381/server/ModernTimeAttackConfiguration$Spec
local/lito381/server/ModernTimeAttackProtocol
local/lito381/server/ModernTimeAttackProtocol$Data
local/lito381/server/ModernTimeAttackProtocol$Receipt
local/lito381/server/ModernTimeAttackSettlement
local/lito381/server/ModernTimeAttackSettlement$Proof
local/lito381/server/ModernTimedEffects
local/lito381/server/ModernTimedEffects$1
local/lito381/server/ModernTimedEffects$Action
local/lito381/server/ModernTimedEffects$HasteOrigin
local/lito381/server/ModernTimedEffects$Kind
local/lito381/server/ModernTimedEffects$State
local/lito381/server/ModernTlsIdentity
local/lito381/server/ModernTlsSessionTransport
local/lito381/server/ModernTlsSessionTransport$1
local/lito381/server/ModernTlsSessionTransport$AuthenticationAttempt
local/lito381/server/ModernTlsSessionTransport$BackendBinding
local/lito381/server/ModernTlsSessionTransport$Limits
local/lito381/server/ModernTlsSessionTransport$State
local/lito381/server/ModernTlsSessionTransport$TlsInfo
local/lito381/server/ModernTransportDeadline
local/lito381/server/ModernTransportDeadline$Cancellation
local/lito381/server/ModernTransportDeadline$Outcome
local/lito381/server/ModernTransportDeadline$Scheduler
local/lito381/server/ModernUseHistory
local/lito381/server/ModernUseHistory$Anchor
local/lito381/server/ModernUseHistory$Cast
local/lito381/server/ModernUseHistory$Items
local/lito381/server/ModernUseHistory$Lane
local/lito381/server/ModernUseHistory$Spell
local/lito381/server/ModernUseHistory$Use
local/lito381/server/ModernVipCharacterProfile
local/lito381/server/ModernVipCharacterState
local/lito381/server/ModernVipCharacterState$1
local/lito381/server/ModernVipCharacterState$Candidate
local/lito381/server/ModernVipCombatProfile
local/lito381/server/ModernVipEquipment
local/lito381/server/ModernVipEquipment$Abilities
local/lito381/server/ModernVipEquipment$Catalog
local/lito381/server/ModernVipEquipment$Field
local/lito381/server/ModernVipEquipment$Polymorph
local/lito381/server/ModernVipEquipment$Vip
local/lito381/server/ModernVipEquipmentAuthority
local/lito381/server/ModernVipEquipmentAuthority$Input
local/lito381/server/ModernVipEquipmentAuthority$Inspection
local/lito381/server/ModernVipEquipmentAuthority$Quote
local/lito381/server/ModernVipEquipmentAuthority$Request
local/lito381/server/ModernVipEquipmentAuthority$Result
local/lito381/server/ModernVipEquipmentAuthority$Status
local/lito381/server/ModernVipEquipmentJdbc
local/lito381/server/ModernVipEquipmentProtocol
local/lito381/server/ModernVipEquipmentProtocol$1
local/lito381/server/ModernVipEquipmentProtocol$Action
local/lito381/server/ModernVipEquipmentProtocol$Denial
local/lito381/server/ModernVipEquipmentProtocol$Outcome
local/lito381/server/ModernVipEquipmentProtocol$Reply
local/lito381/server/ModernVipEquipmentProtocol$Request
local/lito381/server/ModernVipLifecycle
local/lito381/server/ModernVipLifecycle$Actor
local/lito381/server/ModernVipLifecycle$Candidate
local/lito381/server/ModernVipLifecycle$Operation
local/lito381/server/ModernVipLifecycle$State
local/lito381/server/ModernVipLoadout
local/lito381/server/ModernVipLoadout$1
local/lito381/server/ModernVipLoadout$Applied
local/lito381/server/ModernVipRegenTimeline
local/lito381/server/ModernVipRegenTimeline$State
local/lito381/server/ModernVipWorldProtocol
local/lito381/server/ModernVipWorldProtocol$1
local/lito381/server/ModernVitalBalance
local/lito381/server/ModernVitalBalance$Flows
local/lito381/server/ModernVitalBalance$State
local/lito381/server/ModernWeightProtocol
local/lito381/server/ModernWeightProtocol$State
local/lito381/server/ModernWorldBinding
local/lito381/server/ModernWorldBinding$Key
local/lito381/server/ModernWorldConfiguration
local/lito381/server/ModernWorldConfiguration$Geometry
local/lito381/server/ModernWorldConfiguration$Input
local/lito381/server/ModernWorldConfiguration$RaceInput
local/lito381/server/ModernWorldMap
local/lito381/server/ModernWorldMap$Cell
local/lito381/server/ModernWorldMap$Failure
local/lito381/server/ModernWorldMap$Manifest
local/lito381/server/ModernWorldMap$MapException
local/lito381/server/ModernWorldMap$Reader
local/lito381/server/ModernWorldMap$Sector
local/lito381/server/ModernWorldMap$SectorBounds
local/lito381/server/ModernWorldMap$SectorKey
local/lito381/server/ModernWorldMap$SectorManifest
local/lito381/server/ModernWorldMapQualification
local/lito381/server/ModernWorldMapQualification$Expected
local/lito381/server/ModernWorldPolicy
local/lito381/server/ModernWorldPolicy$Pin
local/lito381/server/ModernWorldPolicy$Row
local/lito381/server/ModernWorldPolicy$Snapshot
local/lito381/server/ModernWorldPolicyJdbc
local/lito381/server/ModernWorldPolicyJdbc$Column
local/lito381/server/ModernWorldPolicyReadProbe
local/lito381/server/ModernWorldProtocol
local/lito381/server/ModernWorldProtocol$1
local/lito381/server/ModernWorldProtocol$AbortReason
local/lito381/server/ModernWorldProtocol$Action
local/lito381/server/ModernWorldProtocol$Completion
local/lito381/server/ModernWorldProtocol$Frame
local/lito381/server/ModernWorldProtocol$Phase
local/lito381/server/ModernWorldProtocol$Reason
local/lito381/server/ModernWorldProtocol$Reply
local/lito381/server/ModernWorldProtocol$Request
local/lito381/server/ModernWorldProtocol$SettlementState
local/lito381/server/ModernWorldProtocol$Status
local/lito381/server/ModernWorldResourceProtocol
local/lito381/server/ModernWorldResourceProtocol$ArchivePin
local/lito381/server/ModernWorldResourceProtocol$Plan
local/lito381/server/ModernWorldResourceProtocol$Request
local/lito381/server/ModernWorldResourceProtocol$Result
local/lito381/server/ModernWorldService
local/lito381/server/ModernWorldService$1
local/lito381/server/ModernWorldService$2
local/lito381/server/ModernWorldService$ActionAdmission
local/lito381/server/ModernWorldService$ActionExecution
local/lito381/server/ModernWorldService$ActionRejected
local/lito381/server/ModernWorldService$AmmoDispatch
local/lito381/server/ModernWorldService$CastDispatch
local/lito381/server/ModernWorldService$ClockOverrides
local/lito381/server/ModernWorldService$CombatDispatch
local/lito381/server/ModernWorldService$DisintegrateDispatch
local/lito381/server/ModernWorldService$DisintegrateReader
local/lito381/server/ModernWorldService$Entry
local/lito381/server/ModernWorldService$FreshReader
local/lito381/server/ModernWorldService$FullVipReader
local/lito381/server/ModernWorldService$GreaterHasteReader
local/lito381/server/ModernWorldService$HasteReader
local/lito381/server/ModernWorldService$ItemDispatch
local/lito381/server/ModernWorldService$JobState
local/lito381/server/ModernWorldService$JournalPublicationException
local/lito381/server/ModernWorldService$LegacyViews
local/lito381/server/ModernWorldService$Limits
local/lito381/server/ModernWorldService$Mapping
local/lito381/server/ModernWorldService$MatchItems
local/lito381/server/ModernWorldService$NativeContext
local/lito381/server/ModernWorldService$NpcDisintegrateDispatch
local/lito381/server/ModernWorldService$NpcDispatch
local/lito381/server/ModernWorldService$NpcFireStormDispatch
local/lito381/server/ModernWorldService$NpcSpellDispatch
local/lito381/server/ModernWorldService$PhysicalReader
local/lito381/server/ModernWorldService$PreparationJob
local/lito381/server/ModernWorldService$Publication
local/lito381/server/ModernWorldService$Registration
local/lito381/server/ModernWorldService$RevivalAdmission
local/lito381/server/ModernWorldService$SiegeDispatch
local/lito381/server/ModernWorldService$SkillsReader
local/lito381/server/ModernWorldService$SlowReader
local/lito381/server/ModernWorldService$VipWorldReply
local/lito381/server/ModernWorldService$WorldExchange
local/lito381/server/ModernWorldSpawns
local/lito381/server/ModernWorldSpawns$Budget
local/lito381/server/ModernWorldSpawns$Graph
local/lito381/server/ModernWorldSpawns$Plan
local/lito381/server/NativeAppearanceDependencyResolver
local/lito381/server/NativeAppearanceDependencyResolver$1
local/lito381/server/NativeAppearanceDependencyResolver$Action
local/lito381/server/NativeAppearanceDependencyResolver$Binding
local/lito381/server/NativeAppearanceDependencyResolver$Frame
local/lito381/server/NativeAppearanceDependencyResolver$Input
local/lito381/server/NativeAppearanceDependencyResolver$Member
local/lito381/server/NativeAppearanceDependencyResolver$Plan
local/lito381/server/NativeAppearanceDependencyResolver$PlannedFrame
local/lito381/server/NativeAppearanceDependencyResolver$Row
local/lito381/server/NativeAppearanceDependencyResolver$State
local/lito381/server/NativeDeVisibilityPc$1
local/lito381/server/NativePlayerLoadoutAdapter
local/lito381/server/NativePlayerLoadoutAdapter$Captured
local/lito381/server/TournamentPresence
local/lito381/server/TournamentPresence$ChannelSnapshot
local/lito381/server/TournamentPresence$CharacterKey
local/lito381/server/TournamentPresence$DeletionGeneration
local/lito381/server/TournamentPresence$Owner
local/lito381/server/TournamentPresence$Presence
local/lito381/server/TournamentPresence$SelectStatus
local/lito381/server/TournamentPresence$Selection
local/lito381/server/TournamentPresence$SelectionRetention
local/lito381/server/TournamentPresence$SelectionTicket
local/lito381/server/TournamentPresence$Tier
local/lito381/server/TournamentRoomPolicy
local/lito381/server/TournamentRoomPolicy$1
```

## ONLY_RIGHT

只在 right（5）。與 `docs/server-source-gap.md` 的「只在 reference」應對齊。

```
local/lito381/server/BattleInvisibilityStatus$Lease
local/lito381/server/BattleInvisibilityStatus$Snapshot
local/lito381/server/NativeCastMaterial$Effect
local/lito381/server/NativeDeVisibilitySwitch
local/lito381/server/NativeResurrectionMaterialAction$Target
```

