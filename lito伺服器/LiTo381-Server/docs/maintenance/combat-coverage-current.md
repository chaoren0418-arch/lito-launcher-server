# 戰鬥／技能覆蓋矩陣（實作中）

日期：2026-10-16。此表是來源／候選核心盤點；**有 catalog、有 handler 或基本欄位相符均不代表行為通過**。正式核心仍為 0.6.68，尚未部署此候選。

共用庫 skills 共 807 筆；catalog 技能 214 個。完整證據及外控讀取點見 [JSON](combat-coverage-current.json)。

SQL skill_level 僅用於分組，尚需驗證各職業實際可學／可施放權限。沒有定義的槽位不自動視為保留槽。

| ID | 名稱 | SQL 分組 | catalog 類型 | 處理器存在 | 分類 | 完整行為 |
| --- | --- | --- | --- | --- | --- | --- |
| 1 | 初級治癒術 | 共通魔法／法師 | HealingSpell | 是 | 已定義，待行為驗證 | 待驗證 |
| 2 | 日光術 | 共通魔法／法師 | LightSpell | 是 | 已定義，待行為驗證 | 待驗證 |
| 3 | 保護罩 | 共通魔法／法師 | ShieldSpell | 是 | 已定義，待行為驗證 | 待驗證 |
| 4 | 光箭 | 共通魔法／法師 | AttackSpell | 是 | 已定義，待行為驗證 | 待驗證 |
| 5 | 指定傳送 | 共通魔法／法師 |  | 是 | 獨立處理器，待行為驗證 | 待驗證 |
| 6 | 冰箭 | 共通魔法／法師 | AttackSpell | 是 | 已定義，待行為驗證 | 待驗證 |
| 7 | 風刃 | 共通魔法／法師 | AttackSpell | 是 | 已定義，待行為驗證 | 待驗證 |
| 8 | 神聖武器 | 共通魔法／法師 | WeaponBuffSpell | 是 | 已定義，待行為驗證 | 待驗證 |
| 9 | 解毒術 | 共通魔法／法師 | CurseRemovalSpell | 是 | 已定義，待行為驗證 | 待驗證 |
| 10 | 寒冷戰慄 | 共通魔法／法師 | AttackSpell | 是 | 已定義，待行為驗證 | 待驗證 |
| 11 | 毒咒 | 共通魔法／法師 | PoisonSpell | 是 | 已定義，待行為驗證 | 待驗證 |
| 12 | 擬似魔法武器 | 共通魔法／法師 | ItemWeaponEnchantSpell | 是 | 已定義，待行為驗證 | 待驗證 |
| 13 | 無所遁形術 | 共通魔法／法師 | DetectionSpell | 是 | 已定義，待行為驗證 | 待驗證 |
| 14 | 負重強化 | 共通魔法／法師 | WeightSpell | 是 | 已定義，待行為驗證 | 待驗證 |
| 15 | 火箭 | 共通魔法／法師 | AttackSpell | 是 | 已定義，待行為驗證 | 待驗證 |
| 16 | 地獄之牙 | 共通魔法／法師 | AttackSpell | 是 | 已定義，待行為驗證 | 待驗證 |
| 17 | 極光雷電 | 共通魔法／法師 | LineAttackSpell | 是 | 已定義，待行為驗證 | 待驗證 |
| 18 | 起死回生術 | 共通魔法／法師 | TurnUndeadSpell | 是 | 已定義，待行為驗證 | 待驗證 |
| 19 | 中級治癒術 | 共通魔法／法師 | HealingSpell | 是 | 已定義，待行為驗證 | 待驗證 |
| 20 | 闇盲咒術 | 共通魔法／法師 | BlindSpell | 是 | 已定義，待行為驗證 | 待驗證 |
| 21 | 鎧甲護持 | 共通魔法／法師 | ItemArmorEnchantSpell | 是 | 已定義，待行為驗證 | 待驗證 |
| 22 | 寒冰氣息 | 共通魔法／法師 | AreaAttackSpell | 是 | 已定義，待行為驗證 | 待驗證 |
| 23 | 能量感測 | 共通魔法／法師 | RevealWeaknessSpell | 是 | 已定義，待行為驗證 | 待驗證 |
| 24 | none | 來源其他技能／NPC／道具 | ReservedSkillSlot | 待查 | SQL 保留槽（name=none，無來源技能常量／執行器） | 待驗證 |
| 25 | 燃燒的火球 | 共通魔法／法師 | AreaAttackSpell | 是 | 已定義，待行為驗證 | 待驗證 |
| 26 | 通暢氣脈術 | 共通魔法／法師 | AbilityBuffSpell | 是 | 已定義，待行為驗證 | 待驗證 |
| 27 | 壞物術 | 共通魔法／法師 | WeaponBreakSpell | 是 | 已定義，待行為驗證 | 待驗證 |
| 28 | 吸血鬼之吻 | 共通魔法／法師 | AttackSpell | 是 | 已定義，待行為驗證 | 待驗證 |
| 29 | 緩速術 | 共通魔法／法師 | EntangleSpell | 是 | 已定義，待行為驗證 | 待驗證 |
| 30 | 巖牢 | 共通魔法／法師 | AreaAttackSpell | 是 | 已定義，待行為驗證 | 待驗證 |
| 31 | 魔法屏障 | 共通魔法／法師 | CounterMagicSpell | 是 | 已定義，待行為驗證 | 待驗證 |
| 32 | 冥想術 | 共通魔法／法師 | RegenSpell | 是 | 已定義，待行為驗證 | 待驗證 |
| 33 | 木乃伊的詛咒 | 共通魔法／法師 | ParalysisSpell | 是 | 已定義，待行為驗證 | 待驗證 |
| 34 | 極道落雷 | 共通魔法／法師 | AttackSpell | 是 | 已定義，待行為驗證 | 待驗證 |
| 35 | 高級治癒術 | 共通魔法／法師 | HealingSpell | 是 | 已定義，待行為驗證 | 待驗證 |
| 36 | 迷魅術 | 共通魔法／法師 | TamingSpell | 是 | 已定義，待行為驗證 | 待驗證 |
| 37 | 聖潔之光 | 共通魔法／法師 | CurseRemovalSpell | 是 | 已定義，待行為驗證 | 待驗證 |
| 38 | 冰錐 | 共通魔法／法師 | AttackSpell | 是 | 已定義，待行為驗證 | 待驗證 |
| 39 | 魔力奪取 | 共通魔法／法師 | ManaDrainSpell | 是 | 已定義，待行為驗證 | 待驗證 |
| 40 | 黑闇之影 | 共通魔法／法師 | BlindSpell | 是 | 已定義，待行為驗證 | 待驗證 |
| 41 | 造屍術 | 共通魔法／法師 | CreateZombieSpell | 是 | 已定義，待行為驗證 | 待驗證 |
| 42 | 體魄強健術 | 共通魔法／法師 | AbilityBuffSpell | 是 | 已定義，待行為驗證 | 待驗證 |
| 43 | 加速術 | 共通魔法／法師 | HasteSpell | 是 | 已定義，待行為驗證 | 待驗證 |
| 44 | 魔法相消術 | 共通魔法／法師 | CancellationSpell | 是 | 已定義，待行為驗證 | 待驗證 |
| 45 | 地裂術 | 共通魔法／法師 | AttackSpell | 是 | 已定義，待行為驗證 | 待驗證 |
| 46 | 烈炎術 | 共通魔法／法師 | AttackSpell | 是 | 已定義，待行為驗證 | 待驗證 |
| 47 | 弱化術 | 共通魔法／法師 | WeaknessSpell | 是 | 已定義，待行為驗證 | 待驗證 |
| 48 | 祝福魔法武器 | 共通魔法／法師 | WeaponBuffSpell | 是 | 已定義，待行為驗證 | 待驗證 |
| 49 | 體力回復術 | 共通魔法／法師 | GroupHealingSpell | 是 | 已定義，待行為驗證 | 待驗證 |
| 50 | 冰矛圍籬 | 共通魔法／法師 | FreezeAttackSpell | 是 | 已定義，待行為驗證 | 待驗證 |
| 51 | 召喚術 | 共通魔法／法師 | SummonSpell | 是 | 已定義，待行為驗證 | 待驗證 |
| 52 | 神聖疾走 | 共通魔法／法師 | MovementBuffSpell | 是 | 已定義，待行為驗證 | 待驗證 |
| 53 | 龍捲風 | 共通魔法／法師 | AreaAttackSpell | 是 | 已定義，待行為驗證 | 待驗證 |
| 54 | 強力加速術 | 共通魔法／法師 | SelfHasteSpell | 是 | 已定義，待行為驗證 | 待驗證 |
| 55 | 狂暴術 | 共通魔法／法師 | CombatBuffSpell | 是 | 已定義，待行為驗證 | 待驗證 |
| 56 | 疾病術 | 共通魔法／法師 | WeaknessSpell | 是 | 已定義，待行為驗證 | 待驗證 |
| 57 | 全部治癒術 | 共通魔法／法師 | HealingSpell | 是 | 已定義，待行為驗證 | 待驗證 |
| 58 | 火牢 | 共通魔法／法師 | GroundSpell | 是 | 已定義，待行為驗證 | 待驗證 |
| 59 | 冰雪暴 | 共通魔法／法師 | AreaAttackSpell | 是 | 已定義，待行為驗證 | 待驗證 |
| 60 | 隱身術 | 共通魔法／法師 | InvisibilitySpell | 是 | 已定義，待行為驗證 | 待驗證 |
| 61 | 返生術 | 共通魔法／法師 | ResurrectionSpell | 是 | 已定義，待行為驗證 | 待驗證 |
| 62 | 震裂術 | 共通魔法／法師 | AreaAttackSpell | 是 | 已定義，待行為驗證 | 待驗證 |
| 63 | 治癒能量風暴 | 共通魔法／法師 | GroundSpell | 是 | 已定義，待行為驗證 | 待驗證 |
| 64 | 魔法封印 | 共通魔法／法師 | SilenceSpell | 是 | 已定義，待行為驗證 | 待驗證 |
| 65 | 雷霆風暴 | 共通魔法／法師 | AreaAttackSpell | 是 | 已定義，待行為驗證 | 待驗證 |
| 66 | 沉睡之霧 | 共通魔法／法師 | SleepSpell | 是 | 已定義，待行為驗證 | 待驗證 |
| 67 | 變形術 | 共通魔法／法師 | PolymorphSpell | 是 | 已定義，待行為驗證 | 待驗證 |
| 68 | 聖結界 | 共通魔法／法師 | DamageProtectionSpell | 是 | 已定義，待行為驗證 | 待驗證 |
| 69 | 集體傳送術 | 共通魔法／法師 | MassTeleportSpell | 是 | 已定義，待行為驗證 | 待驗證 |
| 70 | 火風暴 | 共通魔法／法師 | AreaAttackSpell | 是 | 已定義，待行為驗證 | 待驗證 |
| 71 | 藥水霜化術 | 共通魔法／法師 | DecayPotionSpell | 是 | 已定義，待行為驗證 | 待驗證 |
| 72 | 強力無所遁形術 | 共通魔法／法師 | CounterDetectionSpell | 是 | 已定義，待行為驗證 | 待驗證 |
| 73 | 創造魔法武器 | 共通魔法／法師 | MagicalWeaponSpell | 是 | 已定義，待行為驗證 | 待驗證 |
| 74 | 流星雨 | 共通魔法／法師 | AreaAttackSpell | 是 | 已定義，待行為驗證 | 待驗證 |
| 75 | 終極返生術 | 共通魔法／法師 | ResurrectionSpell | 是 | 已定義，待行為驗證 | 待驗證 |
| 76 | 集體緩速術 | 共通魔法／法師 | MassSlowSpell | 是 | 已定義，待行為驗證 | 待驗證 |
| 77 | 究極光裂術 | 共通魔法／法師 | AttackSpell | 是 | 已定義，待行為驗證 | 待驗證 |
| 78 | 絕對屏障 | 共通魔法／法師 | AbsoluteBarrierSpell | 是 | 已定義，待行為驗證 | 待驗證 |
| 79 | 靈魂昇華 | 共通魔法／法師 | AdvanceSpiritSpell | 是 | 已定義，待行為驗證 | 待驗證 |
| 80 | 冰雪颶風 | 共通魔法／法師 | FreezeAttackSpell | 是 | 已定義，待行為驗證 | 待驗證 |
| 81 | none | 來源其他技能／NPC／道具 | ReservedSkillSlot | 待查 | SQL 保留槽（name=none，無來源技能常量／執行器） | 待驗證 |
| 82 | none | 來源其他技能／NPC／道具 | ReservedSkillSlot | 待查 | SQL 保留槽（name=none，無來源技能常量／執行器） | 待驗證 |
| 83 | none | 來源其他技能／NPC／道具 | ReservedSkillSlot | 待查 | SQL 保留槽（name=none，無來源技能常量／執行器） | 待驗證 |
| 84 | none | 來源其他技能／NPC／道具 | ReservedSkillSlot | 待查 | SQL 保留槽（name=none，無來源技能常量／執行器） | 待驗證 |
| 85 | none | 來源其他技能／NPC／道具 | ReservedSkillSlot | 待查 | SQL 保留槽（name=none，無來源技能常量／執行器） | 待驗證 |
| 86 | none | 來源其他技能／NPC／道具 | ReservedSkillSlot | 待查 | SQL 保留槽（name=none，無來源技能常量／執行器） | 待驗證 |
| 87 | 衝擊之暈 | 騎士 | ShockStunSpell | 是 | 已定義，待行為驗證 | 待驗證 |
| 88 | 增幅防禦 | 騎士 | ReductionArmorSpell | 是 | 已定義，待行為驗證 | 待驗證 |
| 89 | 尖刺盔甲 | 騎士 | BounceAttackSpell | 是 | 已定義，待行為驗證 | 待驗證 |
| 90 | 堅固防護 | 騎士 | SolidCarriageSpell | 是 | 已定義，待行為驗證 | 待驗證 |
| 91 | 反擊屏障 | 騎士 | CounterBarrierSpell | 是 | 已定義，待行為驗證 | 待驗證 |
| 92 | none | 來源其他技能／NPC／道具 | ReservedSkillSlot | 待查 | SQL 保留槽（name=none，無來源技能常量／執行器） | 待驗證 |
| 93 | none | 來源其他技能／NPC／道具 | ReservedSkillSlot | 待查 | SQL 保留槽（name=none，無來源技能常量／執行器） | 待驗證 |
| 94 | none | 來源其他技能／NPC／道具 | ReservedSkillSlot | 待查 | SQL 保留槽（name=none，無來源技能常量／執行器） | 待驗證 |
| 95 | none | 來源其他技能／NPC／道具 | ReservedSkillSlot | 待查 | SQL 保留槽（name=none，無來源技能常量／執行器） | 待驗證 |
| 96 | none | 來源其他技能／NPC／道具 | ReservedSkillSlot | 待查 | SQL 保留槽（name=none，無來源技能常量／執行器） | 待驗證 |
| 97 | 暗隱術 | 黑暗妖精 | InvisibilitySpell | 是 | 已定義，待行為驗證 | 待驗證 |
| 98 | 附加劇毒 | 黑暗妖精 | EnchantVenomSpell | 是 | 已定義，待行為驗證 | 待驗證 |
| 99 | 影之防護 | 黑暗妖精 | ShieldSpell | 是 | 已定義，待行為驗證 | 待驗證 |
| 100 | 提煉魔石 | 黑暗妖精 | BringStoneSpell | 是 | 已定義，待行為驗證 | 待驗證 |
| 101 | 行走加速 | 黑暗妖精 | MovementBuffSpell | 是 | 已定義，待行為驗證 | 待驗證 |
| 102 | 燃燒鬥志 | 黑暗妖精 | PhysicalProcBuffSpell | 是 | 已定義，待行為驗證 | 待驗證 |
| 103 | 暗黑盲咒 | 黑暗妖精 | BlindSpell | 是 | 已定義，待行為驗證 | 待驗證 |
| 104 | 毒性抵抗 | 黑暗妖精 | PoisonResistanceSpell | 是 | 已定義，待行為驗證 | 待驗證 |
| 105 | 雙重破壞 | 黑暗妖精 | PhysicalProcBuffSpell | 是 | 已定義，待行為驗證 | 待驗證 |
| 106 | 暗影閃避 | 黑暗妖精 | DodgeBuffSpell | 是 | 已定義，待行為驗證 | 待驗證 |
| 107 | 暗影之牙 | 黑暗妖精 | ItemWeaponEnchantSpell | 是 | 已定義，待行為驗證 | 待驗證 |
| 108 | 會心一擊 | 黑暗妖精 | AttackSpell | 是 | 已定義，待行為驗證 | 待驗證 |
| 109 | 力量提升 | 黑暗妖精 | AbilityBuffSpell | 是 | 已定義，待行為驗證 | 待驗證 |
| 110 | 敏捷提升 | 黑暗妖精 | AbilityBuffSpell | 是 | 已定義，待行為驗證 | 待驗證 |
| 111 | 閃避提升 | 黑暗妖精 | RangedEvasionBuffSpell | 是 | 已定義，待行為驗證 | 待驗證 |
| 112 | 破壞盔甲 | 黑暗妖精 | ArmorBreakSpell | 是 | 已定義，待行為驗證 | 待驗證 |
| 113 | 精準目標 | 王族 | TrueTargetSpell | 是 | 已定義，待行為驗證 | 待驗證 |
| 114 | 激勵士氣 | 王族 | RoyalAuraSpell | 是 | 已定義，待行為驗證 | 待驗證 |
| 115 | 鋼鐵士氣 | 王族 | RoyalAuraSpell | 是 | 已定義，待行為驗證 | 待驗證 |
| 116 | 呼喚盟友 | 王族 | CallClanSpell | 是 | 已定義，待行為驗證 | 待驗證 |
| 117 | 衝擊士氣 | 王族 | RoyalAuraSpell | 是 | 已定義，待行為驗證 | 待驗證 |
| 118 | 援護盟友 | 王族 | RunClanSpell | 是 | 已定義，待行為驗證 | 待驗證 |
| 119 | none | 王族 | ReservedSkillSlot | 待查 | SQL 保留槽（name=none，無來源技能常量／執行器） | 待驗證 |
| 120 | none | 王族 | ReservedSkillSlot | 待查 | SQL 保留槽（name=none，無來源技能常量／執行器） | 待驗證 |
| 121 | none | 來源其他技能／NPC／道具 | ReservedSkillSlot | 待查 | SQL 保留槽（name=none，無來源技能常量／執行器） | 待驗證 |
| 122 |  | 來源其他技能／NPC／道具 | ReservedSkillSlot | 待查 | catalog 明確保留槽，無 SQL 定義 | 待驗證 |
| 123 | none | 來源其他技能／NPC／道具 | ReservedSkillSlot | 待查 | SQL 保留槽（name=none，無來源技能常量／執行器） | 待驗證 |
| 124 | none | 來源其他技能／NPC／道具 | ReservedSkillSlot | 待查 | SQL 保留槽（name=none，無來源技能常量／執行器） | 待驗證 |
| 125 | none | 來源其他技能／NPC／道具 | ReservedSkillSlot | 待查 | SQL 保留槽（name=none，無來源技能常量／執行器） | 待驗證 |
| 126 | none | 來源其他技能／NPC／道具 | ReservedSkillSlot | 待查 | SQL 保留槽（name=none，無來源技能常量／執行器） | 待驗證 |
| 127 | none | 來源其他技能／NPC／道具 | ReservedSkillSlot | 待查 | SQL 保留槽（name=none，無來源技能常量／執行器） | 待驗證 |
| 128 | none | 來源其他技能／NPC／道具 | ReservedSkillSlot | 待查 | SQL 保留槽（name=none，無來源技能常量／執行器） | 待驗證 |
| 129 | 魔法防禦 | 妖精 | MagicResistanceSpell | 是 | 已定義，待行為驗證 | 待驗證 |
| 130 | 心靈轉換 | 妖精 | BodyToMindSpell | 是 | 已定義，待行為驗證 | 待驗證 |
| 131 | 世界樹的呼喚 | 妖精 | WorldTreeCallSpell | 是 | 已定義，待行為驗證 | 待驗證 |
| 132 | 三重矢 | 妖精 | TripleArrowSpell | 是 | 已定義，待行為驗證 | 待驗證 |
| 133 | 弱化屬性 | 妖精 | ElementalFallDownSpell | 是 | 已定義，待行為驗證 | 待驗證 |
| 134 | 鏡反射 | 妖精 | CounterMirrorSpell | 是 | 已定義，待行為驗證 | 待驗證 |
| 135 | none | 來源其他技能／NPC／道具 | ReservedSkillSlot | 待查 | SQL 保留槽（name=none，無來源技能常量／執行器） | 待驗證 |
| 136 | none | 來源其他技能／NPC／道具 | ReservedSkillSlot | 待查 | SQL 保留槽（name=none，無來源技能常量／執行器） | 待驗證 |
| 137 | 淨化精神 | 妖精 | ClearMindSpell | 是 | 已定義，待行為驗證 | 待驗證 |
| 138 | 屬性防禦 | 妖精 | ResistElementalSpell | 是 | 已定義，待行為驗證 | 待驗證 |
| 139 | none | 來源其他技能／NPC／道具 | ReservedSkillSlot | 待查 | SQL 保留槽（name=none，無來源技能常量／執行器） | 待驗證 |
| 140 | none | 來源其他技能／NPC／道具 | ReservedSkillSlot | 待查 | SQL 保留槽（name=none，無來源技能常量／執行器） | 待驗證 |
| 141 | none | 來源其他技能／NPC／道具 | ReservedSkillSlot | 待查 | SQL 保留槽（name=none，無來源技能常量／執行器） | 待驗證 |
| 142 | none | 來源其他技能／NPC／道具 | ReservedSkillSlot | 待查 | SQL 保留槽（name=none，無來源技能常量／執行器） | 待驗證 |
| 143 | none | 來源其他技能／NPC／道具 | ReservedSkillSlot | 待查 | SQL 保留槽（name=none，無來源技能常量／執行器） | 待驗證 |
| 144 | none | 來源其他技能／NPC／道具 | ReservedSkillSlot | 待查 | SQL 保留槽（name=none，無來源技能常量／執行器） | 待驗證 |
| 145 | 釋放元素 | 妖精 | ReturnToNatureSpell | 是 | 已定義，待行為驗證 | 待驗證 |
| 146 | 魂體轉換 | 妖精 | BodyToMindSpell | 是 | 已定義，待行為驗證 | 待驗證 |
| 147 | 單屬性防禦 | 妖精 | ElementalProtectionSpell | 是 | 已定義，待行為驗證 | 待驗證 |
| 148 | 火焰武器 | 妖精 | FireWeaponSpell | 是 | 已定義，待行為驗證 | 待驗證 |
| 149 | 風之神射 | 妖精 | WindShotSpell | 是 | 已定義，待行為驗證 | 待驗證 |
| 150 | 風之疾走 | 妖精 | MovementBuffSpell | 是 | 已定義，待行為驗證 | 待驗證 |
| 151 | 大地防護 | 妖精 | ShieldSpell | 是 | 已定義，待行為驗證 | 待驗證 |
| 152 | 地面障礙 | 妖精 | EntangleSpell | 是 | 已定義，待行為驗證 | 待驗證 |
| 153 | 魔法消除 | 妖精 | EraseMagicSpell | 是 | 已定義，待行為驗證 | 待驗證 |
| 154 | 召喚屬性精靈 | 妖精 | ElementalSummonSpell | 是 | 已定義，待行為驗證 | 待驗證 |
| 155 | 烈炎氣息 | 妖精 | FireBlessSpell | 是 | 已定義，待行為驗證 | 待驗證 |
| 156 | 暴風之眼 | 妖精 | StormEyeSpell | 是 | 已定義，待行為驗證 | 待驗證 |
| 157 | 大地屏障 | 妖精 | BindSpell | 是 | 已定義，待行為驗證 | 待驗證 |
| 158 | 生命之泉 | 妖精 | NaturesTouchSpell | 是 | 已定義，待行為驗證 | 待驗證 |
| 159 | 大地的祝福 | 妖精 | EarthBlessSpell | 是 | 已定義，待行為驗證 | 待驗證 |
| 160 | 水之防護 | 妖精 | AquaProtectSpell | 是 | 已定義，待行為驗證 | 待驗證 |
| 161 | 封印禁地 | 妖精 | AreaSilenceSpell | 是 | 已定義，待行為驗證 | 待驗證 |
| 162 | 召喚強力屬性精靈 | 妖精 | ElementalSummonSpell | 是 | 已定義，待行為驗證 | 待驗證 |
| 163 | 烈炎武器 | 妖精 | FireWeaponSpell | 是 | 已定義，待行為驗證 | 待驗證 |
| 164 | 生命的祝福 | 妖精 | NaturesBlessingSpell | 是 | 已定義，待行為驗證 | 待驗證 |
| 165 | 生命呼喚 | 妖精 | ResurrectionSpell | 是 | 已定義，待行為驗證 | 待驗證 |
| 166 | 暴風神射 | 妖精 | StormShotSpell | 是 | 已定義，待行為驗證 | 待驗證 |
| 167 | 風之枷鎖 | 妖精 | WindShackleSpell | 是 | 已定義，待行為驗證 | 待驗證 |
| 168 | 鋼鐵防護 | 妖精 | IronSkinSpell | 是 | 已定義，待行為驗證 | 待驗證 |
| 169 | 體能激發 | 妖精 | ExoticVitalizeSpell | 是 | 已定義，待行為驗證 | 待驗證 |
| 170 | 水之元氣 | 妖精 | WaterLifeSpell | 是 | 已定義，待行為驗證 | 待驗證 |
| 171 | 屬性之火 | 妖精 | PhysicalProcBuffSpell | 是 | 已定義，待行為驗證 | 待驗證 |
| 172 | 暴風疾走 | 妖精 | OtherNoEffectSpell | 是 | 已定義，待行為驗證 | 待驗證 |
| 173 | 污濁之水 | 妖精 | PolluteWaterSpell | 是 | 已定義，待行為驗證 | 待驗證 |
| 174 | 精準射擊 | 妖精 | StrikerGaleSpell | 是 | 已定義，待行為驗證 | 待驗證 |
| 175 | 烈焰之魂 | 妖精 | PhysicalProcBuffSpell | 是 | 已定義，待行為驗證 | 待驗證 |
| 176 | 能量激發 | 妖精 | AdditionalFireSpell | 是 | 已定義，待行為驗證 | 待驗證 |
| 177 | none | 來源其他技能／NPC／道具 | ReservedSkillSlot | 待查 | SQL 保留槽（name=none，無來源技能常量／執行器） | 待驗證 |
| 178 | none | 來源其他技能／NPC／道具 | ReservedSkillSlot | 待查 | SQL 保留槽（name=none，無來源技能常量／執行器） | 待驗證 |
| 179 | none | 來源其他技能／NPC／道具 | ReservedSkillSlot | 待查 | SQL 保留槽（name=none，無來源技能常量／執行器） | 待驗證 |
| 180 | none | 來源其他技能／NPC／道具 | ReservedSkillSlot | 待查 | SQL 保留槽（name=none，無來源技能常量／執行器） | 待驗證 |
| 181 | 龍之護鎧 | 龍騎士 | DragonSkinSpell | 是 | 已定義，待行為驗證 | 待驗證 |
| 182 | 燃燒擊砍 | 龍騎士 | BurningSlashSpell | 是 | 已定義，待行為驗證 | 待驗證 |
| 183 | 護衛毀滅 | 龍騎士 | GuardBrakeSpell | 是 | 已定義，待行為驗證 | 待驗證 |
| 184 | 岩漿噴吐 | 龍騎士 | AreaAttackSpell | 是 | 已定義，待行為驗證 | 待驗證 |
| 185 | 覺醒：安塔瑞斯 | 龍騎士 | AwakenSpell | 是 | 已定義，待行為驗證 | 待驗證 |
| 186 | 血之渴望 | 龍騎士 | BloodlustSpell | 是 | 已定義，待行為驗證 | 待驗證 |
| 187 | 屠宰者 | 龍騎士 | FoeSlayerSpell | 是 | 已定義，待行為驗證 | 待驗證 |
| 188 | 恐懼無助 | 龍騎士 | ResistFearSpell | 是 | 已定義，待行為驗證 | 待驗證 |
| 189 | 岩漿之箭 | 龍騎士 | AreaAttackSpell | 是 | 已定義，待行為驗證 | 待驗證 |
| 190 | 覺醒：法利昂 | 龍騎士 | AwakenSpell | 是 | 已定義，待行為驗證 | 待驗證 |
| 191 | 致命身軀 | 龍騎士 | MortalBodySpell | 是 | 已定義，待行為驗證 | 待驗證 |
| 192 | 奪命之雷 | 龍騎士 | ThunderGrabSpell | 待查 | 已定義，待行為驗證 | 待驗證 |
| 193 | 驚悚死神 | 幻術師 | GuardBrakeSpell | 是 | 已定義，待行為驗證 | 待驗證 |
| 194 | 暴龍之眼 | 幻術師 | FreezeAttackSpell | 是 | 已定義，待行為驗證 | 待驗證 |
| 195 | 覺醒：巴拉卡斯 | 幻術師 | AwakenSpell | 是 | 已定義，待行為驗證 | 待驗證 |
| 196 | none | 來源其他技能／NPC／道具 |  | 待查 | SQL 保留槽（name=none，無來源技能常量／執行器） | 待驗證 |
| 197 | none | 來源其他技能／NPC／道具 |  | 待查 | SQL 保留槽（name=none，無來源技能常量／執行器） | 待驗證 |
| 198 | none | 來源其他技能／NPC／道具 |  | 待查 | SQL 保留槽（name=none，無來源技能常量／執行器） | 待驗證 |
| 199 | none | 來源其他技能／NPC／道具 |  | 待查 | SQL 保留槽（name=none，無來源技能常量／執行器） | 待驗證 |
| 200 | none | 來源其他技能／NPC／道具 |  | 待查 | SQL 保留槽（name=none，無來源技能常量／執行器） | 待驗證 |
| 201 | 鏡像 | 幻術師 | DodgeBuffSpell | 是 | 已定義，待行為驗證 | 待驗證 |
| 202 | 混亂 | 幻術師 | ConfusionSpell | 待查 | 已定義，待行為驗證 | 待驗證 |
| 203 | 粉碎能量 | 幻術師 | SmashSpell | 待查 | 已定義，待行為驗證 | 待驗證 |
| 204 | 幻覺：歐吉 | 幻術師 | IllusionOgreSpell | 是 | 已定義，待行為驗證 | 待驗證 |
| 205 | 立方：燃燒 | 幻術師 | CubeIgnitionSpell | 是 | 已定義，待行為驗證 | 待驗證 |
| 206 | 專注 | 幻術師 | RegenSpell | 是 | 已定義，待行為驗證 | 待驗證 |
| 207 | 心靈破壞 | 幻術師 | MindBreakSpell | 待查 | 已定義，待行為驗證 | 待驗證 |
| 208 | 骷髏毀壞 | 幻術師 | BoneBreakSpell | 待查 | 已定義，待行為驗證 | 待驗證 |
| 209 | 幻覺：巫妖 | 幻術師 | IllusionLichSpell | 是 | 已定義，待行為驗證 | 待驗證 |
| 210 | 立方：地裂 | 幻術師 | CubeQuakeSpell | 是 | 已定義，待行為驗證 | 待驗證 |
| 211 | 耐力 | 幻術師 | PatienceSpell | 是 | 已定義，待行為驗證 | 待驗證 |
| 212 | 幻想 | 幻術師 | PhantasmSpell | 是 | 已定義，待行為驗證 | 待驗證 |
| 213 | 武器破壞者 | 幻術師 | ArmBreakerSpell | 待查 | 已定義，待行為驗證 | 待驗證 |
| 214 | 幻覺：鑽石高崙 | 幻術師 | DiamondGolemSpell | 是 | 已定義，待行為驗證 | 待驗證 |
| 215 | 立方：衝擊 | 幻術師 | CubeShockSpell | 是 | 已定義，待行為驗證 | 待驗證 |
| 216 | 洞察 | 幻術師 | InsightSpell | 是 | 已定義，待行為驗證 | 待驗證 |
| 217 | 恐慌 | 幻術師 | PanicSpell | 是 | 已定義，待行為驗證 | 待驗證 |
| 218 | 疼痛的歡愉 | 幻術師 | JoyOfPainSpell | 是 | 已定義，待行為驗證 | 待驗證 |
| 219 | 幻覺：化身 | 幻術師 | IllusionAvatarSpell | 是 | 已定義，待行為驗證 | 待驗證 |
| 220 | 立方：和諧 | 幻術師 | CubeHarmonySpell | 是 | 已定義，待行為驗證 | 待驗證 |

## effect_skill_id 別名盤點

目前共用庫有下列7筆別名；mobskill有9條呼叫，分屬7個NPC，並非僅水龍副本。無mobskill引用的別名：49012。完整呼叫資料保存在JSON；這份盤點不代表候選已動態依別名派送效果，這些NPC的完整原生施法入口仍待整合。
| SQL技能 | 效果ID | 名稱 |
| --- | --- | --- |
| 49011 | 4011 | 藥水侵蝕術 |
| 49012 | 4012 | 污濁的水流 |
| 49013 | 4013 | 治癒侵蝕術 |
| 49014 | 202 | 群體魔法封印 |
| 49015 | 71 | 群體藥水霜化術 |
| 49016 | 167 | 群體風之枷鎖 |
| 49017 | 33 | 群體木乃伊的詛咒 |

## 尚待整合

BATTLE中心冰凍skill80已在原生方法內加入目標捕獲、傷害／封包合併及提交三個掛鉤，沿用同一次消耗與type0多目標封包。七職業與四種同伴有原生封包驗證；不可變batch中的角色引用只屬該次方法local，不存入角色欄位或全域快取。BATTLE直線17／194另補受控主目標與三層跨種類分支；七職業、四種受控主目標與玩家、榮譽模式塔／玩家共同提交已有原生封包驗證，仍未宣稱全部模式或學習資格完成。
BATTLE衝暈已補原生受控目標入口，沿用同伴解析、隊伍／Ctrl與傷害／特效管理；四種同伴均有原生封包驗證。新入口讀SQL的HP／材料及既有MP調整，拒絕不改資源或目標；來源NPC暈眩刷新不累加，動作與特效在同一期限解除。固定擲骰在helper參數邊界驗證，不代表全部職業學習資格；舊玩家／WORLD衝暈的完整HP／材料成本與各來源保護狀態仍待逐路徑補齊。
精準射擊與破壞盔甲已補原生武器細類4／13／10到來源getType1的20／62轉接；精準射擊在玩家原生物理入口延至職業倍率int之後；普通WORLD打怪另改在S_ATTACK發布後讀目前NPC狀態，並把有號傷害與娃娃附加傷害送入毒傷共用的目標佇列。BATTLE／WORLD玩家與其他目標仍保留既有方法內狀態捕獲。BATTLE／WORLD玩家、WORLD真正怪物及BATTLE召喚物的原生弓／鐵手甲／近戰攻擊驗證通過；隔離來源武器及直接負面狀態啟用不等於全部SQL裝備與負面施法資格。受控目標精準射擊174／污濁之水173的BATTLE與WORLD狀態入口已補；其餘負面狀態與來源順序仍待補。
PC_TO_PET_DAMAGE_MULTIPLIER已接入既有BATTLE受控目標物理路徑與BATTLE／WORLD魔法的13處準備、7處範圍／直線計算。只對PET、SUMMON、TAMED套用，ARENA同伴排除。物理先保留爆擊／奇古獸小數、減免後乘寵物倍率，再走硬皮遠攻／職業倍率；魔法先int寵物倍率、光裂加倍、MR，再走SkillMode倍率。WORLD物理已另接原生普通攻擊的受控目標投影／提交；七職業、三種同伴、近遠攻／硬皮／爆擊／奇古獸／安全區／死亡有回歸，未完成NPC負面狀態及附加傷害Proc；BATTLE直線17／194也已另補入口與跨種類分支，精準射擊174及污濁之水173另補BATTLE／WORLD受控施放與狀態消費；單體治癒在原生不死系轉換前讀受控目標Pollute／水龍既有狀態，奇數15減半回8，原生onGetItem藥水回7；一般WORLD／Quest／TimeNPC已有減半，保持原處理且不重複；群體49／生命祝福164已補受控狀態與不死處理，164來源欄位額外量在減半／反轉後才追加；來源ConfigSkill未載入此鍵，現行有效值為0，設定檔100不生效；49玩家Water Life倍增／一次消耗已補，170依現行target_to=1只接受玩家且有真正啟用／NPC拒絕驗證，其餘回復家族資格與NPC水龍別名啟用者仍待補；破壞盔甲112按來源target_to=1只接受玩家，NPC不套1.58倍，NPC聖結界技能入口待補；不能把數值消費端當成所有目標已能施放。
奇古獸已在三個原生物理入口改用目前來源的INT＋真實SP＋裝備魔法命中、武器小型骰／強化值及0.9倍；只對玩家套原始MR的額外外控，保留小數至防禦／聖結界／職業倍率後。來源get_add_dmg目前無setter呼叫而為0，並無對應weapon.add_dmg欄位；屬性武器Proc及呈現外控仍須另補。
七職業對玩家／NPC的14個物理傷害倍率已接入3個原生攻擊入口，逐職業驗證BATTLE／WORLD近戰的實際傷害、整數截斷及玩家／NPC各自為零。另接七個對玩家魔法倍率，以單次施法值快照延後MR，依序處理鏡反射、職業倍率、光裂整數加倍、MR與執行器倍率；涵蓋單體／範圍／冰凍／衝暈及娃娃計算入口。一般NPC魔法已補光裂先加倍再MR，以及移開NPC計算副本的提前裁零；七種施法fixture及ARENA／WORLD／Quest／Time有原生施法，27個範圍／直線分支另有typed來源比對；NPC特殊保護／全部狀態與Proc、其他武器家族、全部來源傷害順序及學習／施法資格尚未完整驗收。
負面技能／BOSS 控制表已接主要與特殊機率呼叫。BUFF 表79個現有能力欄位皆有數值消費端；其中72列有原生啟用入口。卡瑞／莎爾已接來源NPC出生點及原生C_DIALOG，另唯讀載入卡瑞map1005。4016的7列保留SkillMode邊界，來源啟用者尚未證明，不能擅造NPC觸發。七魔眼另讀道具設定／能力表相加。任意跨能力新增列、完整共存／相消與全目標施法仍待整合。普通世界死亡的物品／技能／經驗懲罰總入口已停用。完整實作與限制見 [施工紀錄](combat-integration-current.md)。不能宣稱全技能同源完成。
完整外控包括來源 ConfigAlt／ConfigOther 等其他計算，以及技能特殊执行器；具體讀取點保留於 JSON。
玩家／NPC／ArenaBot／召喚物、Ctrl／範圍、疊加／到期、死亡無懲罰及斷線結算等全路徑驗證仍需完成。
