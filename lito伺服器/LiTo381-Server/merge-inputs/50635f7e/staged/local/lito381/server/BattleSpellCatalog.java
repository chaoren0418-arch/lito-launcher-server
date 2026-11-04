package local.lito381.server;

import java.util.Map;
import java.util.function.IntUnaryOperator;

/**
 * Verified spell metadata shared by the native packet paths.
 *
 * <p>The C_USE_SPELL selector and S_EFFECT_LOC layout come from the recovered
 * LiTo 3.81C executable. Action/effect ids are corroborated by the matching
 * 3.80 classic server {@code skills} table: basic heal uses action 19/gfx
 * 11722, light arrow uses action 18/gfx 167, fire wall uses action 18/gfx
 * 168, and life stream uses action 19/gfx 2231. These are sprite/effect
 * resource ids, not Tournament room modes and not client-supplied values.</p>
 */
final class BattleSpellCatalog {
    static final int ATTRIBUTE_UNSPECIFIED = 0;
    static final int ATTRIBUTE_EARTH = 1;
    static final int ATTRIBUTE_FIRE = 2;
    static final int ATTRIBUTE_WATER = 4;
    static final int ATTRIBUTE_WIND = 8;
    static final int ATTRIBUTE_NONE = 16;

    static final int BASIC_HEAL = 1;
    static final int LIGHT = 2;
    static final int SHIELD = 3;
    static final int LIGHT_ARROW = 4;
    static final int TELEPORT = 5;
    static final int ICE_ARROW = 6;
    static final int WIND_CUTTER = 7;
    static final int HOLY_WEAPON = 8;
    static final int CURE_POISON = 9;
    static final int CHILL_TOUCH = 10;
    static final int CURSE_POISON = 11;
    static final int ENCHANT_WEAPON = 12;
    static final int DETECTION = 13;
    static final int DECREASE_WEIGHT = 14;
    static final int FIRE_ARROW = 15;
    static final int STALAC = 16;
    static final int LIGHTNING = 17;
    static final int TURN_UNDEAD = 18;
    static final int EXTRA_HEAL = 19;
    static final int CURSE_BLIND = 20;
    static final int BLESSED_ARMOR = 21;
    static final int FROZEN_CLOUD = 22;
    static final int REVEAL_WEAKNESS = 23;
    static final int RESERVED_WIZARD_24 = 24;
    static final int BURNING_FIREBALL = 25;
    static final int PHYSICAL_ENCHANT_DEX = 26;
    static final int WEAPON_BREAK = 27;
    static final int VAMPIRIC_TOUCH = 28;
    static final int SLOW = 29;
    static final int EARTH_JAIL = 30;
    static final int COUNTER_MAGIC = 31;
    static final int MEDITATION = 32;
    static final int CURSE_PARALYZE = 33;
    static final int CALL_LIGHTNING = 34;
    static final int GREATER_HEAL = 35;
    static final int TAMING_MONSTER = 36;
    static final int REMOVE_CURSE = 37;
    static final int CONE_OF_COLD = 38;
    static final int MANA_DRAIN = 39;
    static final int DARKNESS = 40;
    static final int CREATE_ZOMBIE = 41;
    static final int PHYSICAL_ENCHANT_STR = 42;
    static final int HASTE = 43;
    static final int CANCELLATION = 44;
    static final int ERUPTION = 45;
    static final int SUNBURST = 46;
    static final int WEAKNESS = 47;
    static final int BLESS_WEAPON = 48;
    static final int HEAL_ALL = 49;
    static final int ICE_LANCE = 50;
    static final int SUMMON_MONSTER = 51;
    static final int RESURRECTION = 61;
    static final int EARTHQUAKE = 62;
    static final int HOLY_WALK = 52;
    static final int TORNADO = 53;
    static final int GREATER_HASTE = 54;
    static final int BERSERKERS = 55;
    static final int DISEASE = 56;
    static final int FULL_HEAL = 57;
    static final int FIRE_WALL = 58;
    static final int BLIZZARD = 59;
    static final int INVISIBILITY = 60;
    static final int LIFE_STREAM = 63;
    static final int SILENCE = 64;
    static final int LIGHTNING_STORM = 65;
    static final int FOG_OF_SLEEPING = 66;
    static final int SHAPE_CHANGE = 67;
    static final int IMMUNE_TO_HARM = 68;
    static final int MASS_TELEPORT = 69;
    static final int FIRE_STORM = 70;
    static final int DECAY_POTION = 71;
    static final int COUNTER_DETECTION = 72;
    static final int CREATE_MAGICAL_WEAPON = 73;
    static final int METEOR_STRIKE = 74;
    static final int GREATER_RESURRECTION = 75;
    static final int MASS_SLOW = 76;
    static final int DISINTEGRATE = 77;
    // TW13081901.txt SPR 11746/11748 both declare 60 frames at 24 FPS.
    // LiTo.bin must keep the battle world alive until that terminal effect
    // has completed; sending S_D3 in the same burst as S_ACTION(DIE) leaves
    // the native effect controller pointing at a dismantled world.
    static final int DISINTEGRATE_EFFECT_FRAMES = 60;
    static final int DISINTEGRATE_EFFECT_FPS = 24;
    static final long DISINTEGRATE_EFFECT_COMPLETION_MILLIS =
            DISINTEGRATE_EFFECT_FRAMES * 1_000L / DISINTEGRATE_EFFECT_FPS;
    static final int ABSOLUTE_BARRIER = 78;
    static final int ADVANCE_SPIRIT = 79;
    static final int FREEZING_BLIZZARD = 80;
    static final int RESERVED_GENERAL_81 = 81;
    static final int RESERVED_GENERAL_82 = 82;
    static final int RESERVED_GENERAL_83 = 83;
    static final int RESERVED_GENERAL_84 = 84;
    static final int RESERVED_GENERAL_85 = 85;
    static final int RESERVED_GENERAL_86 = 86;
    static final int SHOCK_STUN = 87;
    static final int REDUCTION_ARMOR = 88;
    static final int BOUNCE_ATTACK = 89;
    static final int SOLID_CARRIAGE = 90;
    static final int COUNTER_BARRIER = 91;
    static final int RESERVED_KNIGHT_1 = 92;
    static final int RESERVED_KNIGHT_2 = 93;
    static final int RESERVED_KNIGHT_3 = 94;
    static final int RESERVED_KNIGHT_4 = 95;
    static final int RESERVED_KNIGHT_5 = 96;
    static final int BLIND_HIDING = 97;
    static final int ENCHANT_VENOM = 98;
    static final int SHADOW_ARMOR = 99;
    static final int BRING_STONE = 100;
    static final int MOVING_ACCELERATION = 101;
    static final int BURNING_SPIRIT = 102;
    static final int DARK_BLIND = 103;
    static final int VENOM_RESIST = 104;
    static final int DOUBLE_BRAKE = 105;
    static final int UNCANNY_DODGE = 106;
    static final int SHADOW_FANG = 107;
    static final int FINAL_BURN = 108;
    static final int DRESS_MIGHTY = 109;
    static final int DRESS_DEXTERITY = 110;
    static final int DRESS_EVASION = 111;
    static final int ARMOR_BREAK = 112;
    static final int TRUE_TARGET = 113;
    static final int GLOWING_AURA = 114;
    static final int SHINING_AURA = 115;
    static final int CALL_CLAN = 116;
    static final int BRAVE_AURA = 117;
    static final int RUN_CLAN = 118;
    static final int RESERVED_ELF_119 = 119;
    static final int RESERVED_ELF_120 = 120;
    static final int RESERVED_ELF_121 = 121;
    static final int RESERVED_ELF_122 = 122;
    static final int RESERVED_ELF_123 = 123;
    static final int RESERVED_ELF_124 = 124;
    static final int RESERVED_ELF_125 = 125;
    static final int RESERVED_ELF_126 = 126;
    static final int RESERVED_ELF_127 = 127;
    static final int RESERVED_ELF_128 = 128;
    static final int RESIST_MAGIC = 129;
    static final int BODY_TO_MIND = 130;
    static final int WORLD_TREE_CALL = 131;
    static final int TRIPLE_ARROW = 132;
    static final int ELEMENTAL_FALL_DOWN = 133;
    static final int COUNTER_MIRROR = 134;
    static final int RESERVED_ELF_135 = 135;
    static final int RESERVED_ELF_136 = 136;
    static final int CLEAR_MIND = 137;
    static final int RESIST_ELEMENTAL = 138;
    static final int RESERVED_ELF_139 = 139;
    static final int RESERVED_ELF_140 = 140;
    static final int RESERVED_ELF_141 = 141;
    static final int RESERVED_ELF_142 = 142;
    static final int RESERVED_ELF_143 = 143;
    static final int RESERVED_ELF_144 = 144;
    static final int RETURN_TO_NATURE = 145;
    static final int BLOODY_SOUL = 146;
    static final int ELEMENTAL_PROTECTION = 147;
    static final int FIRE_WEAPON = 148;
    static final int WIND_SHOT = 149;
    static final int WIND_WALK = 150;
    static final int EARTH_SKIN = 151;
    static final int ENTANGLE = 152;
    static final int ERASE_MAGIC = 153;
    static final int LESSER_ELEMENTAL = 154;
    static final int FIRE_BLESS = 155;
    static final int STORM_EYE = 156;
    static final int EARTH_BIND = 157;
    static final int NATURES_TOUCH = 158;
    static final int EARTH_BLESS = 159;
    static final int AQUA_PROTECTER = 160;
    static final int AREA_OF_SILENCE = 161;
    static final int GREATER_ELEMENTAL = 162;
    static final int BURNING_WEAPON = 163;
    static final int NATURES_BLESSING = 164;
    static final int CALL_OF_NATURE = 165;
    static final int STORM_SHOT = 166;
    static final int WIND_SHACKLE = 167;
    static final int IRON_SKIN = 168;
    static final int EXOTIC_VITALIZE = 169;
    static final int WATER_LIFE = 170;
    static final int ELEMENTAL_FIRE = 171;
    static final int STORM_WALK = 172;
    static final int POLLUTE_WATER = 173;
    static final int STRIKER_GALE = 174;
    static final int SOUL_OF_FLAME = 175;
    static final int ADDITIONAL_FIRE = 176;
    static final int RESERVED_DRAGON_177 = 177;
    static final int RESERVED_DRAGON_178 = 178;
    static final int RESERVED_DRAGON_179 = 179;
    static final int RESERVED_DRAGON_180 = 180;
    static final int DRAGON_SKIN = 181;
    static final int BURNING_SLASH = 182;
    static final int GUARD_BRAKE = 183;
    static final int MAGMA_BREATH = 184;
    static final int AWAKEN_ANTHARAS = 185;
    static final int BLOODLUST = 186;
    static final int FOE_SLAYER = 187;
    static final int RESIST_FEAR = 188;
    static final int SHOCK_SKIN = 189;
    static final int AWAKEN_FAFURION = 190;
    static final int MORTAL_BODY = 191;
    static final int THUNDER_GRAB = 192;
    static final int HORROR_OF_DEATH = 193;
    static final int FREEZING_BREATH = 194;
    static final int AWAKEN_VALAKAS = 195;
    static final int MIRROR_IMAGE = 201;
    static final int CONFUSION = 202;
    static final int SMASH = 203;
    static final int ILLUSION_OGRE = 204;
    static final int CUBE_IGNITION = 205;
    static final int CONCENTRATION = 206;
    static final int MIND_BREAK = 207;
    static final int BONE_BREAK = 208;
    static final int ILLUSION_LICH = 209;
    static final int CUBE_QUAKE = 210;
    static final int PATIENCE = 211;
    static final int PHANTASM = 212;
    static final int ARM_BREAKER = 213;
    static final int ILLUSION_DIAMOND_GOLEM = 214;
    static final int CUBE_SHOCK = 215;
    static final int INSIGHT = 216;
    static final int PANIC = 217;
    static final int JOY_OF_PAIN = 218;
    static final int ILLUSION_AVATAR = 219;
    static final int CUBE_HARMONY = 220;

    private static final Map<Integer, HealingSpell> HEALING_SPELLS = Map.of(
            BASIC_HEAL, new HealingSpell(BASIC_HEAL, 0, 0, 19, 11722,
                    4, 10, -1, 2, 4, ATTRIBUTE_WATER, 77, 280, "basic-heal"),
            // Exact 380_db.sql row 19: PC/NPC object target, native-screen
            // range, MP 15, 100 ms reuse, action 19, gfx 11723 and (3+MB)d8.
            // Exact LiTo.bin zero-based case 18 shares the cccd producer used
            // by skill 1: opcode, row, column and target object id only.
            EXTRA_HEAL, new HealingSpell(EXTRA_HEAL, 2, 2, 19, 11723,
                    15, 100, -1, 3, 8, ATTRIBUTE_WATER, 77, 280, "extra-heal"),
            // Exact selected row 35 and LiTo.bin zero-based case 34 share the
            // cccd object-only producer with skills 1/19.  The retained heal
            // formula uses damage_value as the base dice count: (8+MB)d8.
            // Healing Helm item 20014 does not reduce this skill's MP.
            GREATER_HEAL, new HealingSpell(GREATER_HEAL, 4, 2, 19, 11724,
                    20, 200, -1, 8, 8, ATTRIBUTE_WATER, 77, 280, "greater-heal"),
            // Exact selected row 57 and LiTo.bin zero-based case 56 share
            // loc_4CB675's cccd object-only producer with skills 1/19/35.
            // TYPE_HEAL targets PC/NPC on the native screen and applies
            // (15+external INT healing-dice bonus)d17.  The retained DB
            // publishes success message 77 to a PC target.
            FULL_HEAL, new HealingSpell(FULL_HEAL, 7, 0, 19, 11725,
                    48, 500, -1, 15, 17, ATTRIBUTE_WATER | ATTRIBUTE_WIND,
                    77, 280, "full-heal")
    );

    private static final Map<Integer, GroupHealingSpell> GROUP_HEALING_SPELLS = Map.of(
            // Exact selected row 49 and LiTo.bin zero-based case 48: the
            // loc_4CBBD4 producer writes only c(opcode), c(row), c(column).
            // target_to=20 means same-clan PCs plus the caster's own pets;
            // target=none and area=-1 center the search on the caster's
            // entire native visible screen while deliberately excluding self.
            // The two adjacent 8s are damage_value/damage_dice: (8+MB)d8.
            HEAL_ALL, new GroupHealingSpell(HEAL_ALL, 6, 0, 19, 759,
                    50, 30, 500, -1, 8, 8, "heal-all")
    );

    private static final Map<Integer, NaturesBlessingSpell>
            NATURES_BLESSING_SPELLS = Map.of(
            // Exact selected row164 and LiTo.bin zero-based case163:
            // loc_4CBBD4 emits selector-only ccc row20/column3.  target_to=24
            // is PARTY(8)|PET(16), target=none and area=-1, so retained
            // makeTargetList includes the caster, living visible party PCs and
            // only the caster's own pets/summons across the native screen.
            // Healing is independently rolled per target as (10+MB)d12 and
            // receives the positive-lawful multiplier.  The selected PE word
            // at 0x635774+(163*2) and primary DB both pin reuse at 300 ms.
            NATURES_BLESSING, new NaturesBlessingSpell(
                    NATURES_BLESSING, 20, 3, 21, 3, 19, 2244,
                    30, 300, 24, -1, 10, 12,
                    ATTRIBUTE_WATER, 16, 8, true, "natures-blessing")
    );

    private static final Map<Integer, LightSpell> LIGHT_SPELLS = Map.of(
            // Exact 380_db.sql row 2: selector-only self spell, action 19,
            // gfx 2510, MP 4, 3,600 seconds. Same-family turnOnOffLight
            // fixes the active light range at 0x0E and emits sound 145.
            LIGHT, new LightSpell(LIGHT, 0, 1, 19, 2510,
                    4, 10, 3_600, 0x0E, 145, "light")
    );

    private static final Map<Integer, ShieldSpell> SHIELD_SPELLS = Map.of(
            // Exact 380_db.sql row 3: self, action 19, gfx 221, MP 8,
            // 1,800 seconds. Same-family mechanics apply AC -2 and type 5.
            SHIELD, new ShieldSpell(SHIELD, 0, 2, 19, 221,
                    8, 0, 10, 1_800, 5, -2, false,
                    Targeting.SELF_SELECTOR, 0, 0, 0, "shield"),
            // Exact selected row 99 and LiTo.bin zero-based case 98:
            // row12/column2 selector-only ccc, HP5, MP10, no reagent/reuse,
            // 960 seconds, action19/gfx2943. The retained original mode
            // replaces the shared Shield/Earth-skin family, applies AC -3,
            // persists the skill and exposes native shield icon type 3.
            SHADOW_ARMOR, new ShieldSpell(SHADOW_ARMOR, 12, 2, 19, 2943,
                    10, 5, 0, 960, 3, -3, true,
                    Targeting.SELF_SELECTOR, 0, 0, 0, "shadow-armor"),
            // Exact selected row151 and LiTo.bin zero-based case150: byte
            // table entry4 enters loc_4CB71D/sub_4C9880 and emits cccd with
            // the PC object id. target_to=1 plus range0 makes ordinary use
            // self-only. Retained skills_buff_ability owns AC-6 and the
            // native S_MAGE_SHIELD receiver uses type6.
            EARTH_SKIN, new ShieldSpell(EARTH_SKIN, 18, 6, 19, 2249,
                    15, 0, 0, 960, 6, -6, false,
                    Targeting.PLAYER_OBJECT, 0, 725, 280, "earth-skin")
    );

    private static final Map<Integer, CounterMagicSpell> COUNTER_MAGIC_SPELLS = Map.of(
            // Exact 380_db.sql row 31: selector-only self spell, action 19,
            // activation gfx 10701, MP 20, one Magic Gem and 16 seconds.
            // The selected classic server consumes the ward with the same
            // castgfx used at activation.  This is an object S_EFFECT, not the
            // incompatible legacy S_PacketBox/S_FA status-icon body.
            COUNTER_MAGIC, new CounterMagicSpell(COUNTER_MAGIC, 3, 6, 19, 10701,
                    20, 10, 16, InventoryCatalog.MAGIC_GEM, 1,
                    10701, "counter-magic")
    );

    private static final Map<Integer, AbsoluteBarrierSpell> ABSOLUTE_BARRIER_SPELLS = Map.of(
            // Exact selected row 78 and LiTo.bin zero-based case 77: the
            // loc_4CBBD4 producer writes only c(opcode), c(row), c(column).
            // The selected client has no compatible legacy PacketBox/FA icon;
            // activation therefore uses only the native action/object effect.
            ABSOLUTE_BARRIER, new AbsoluteBarrierSpell(ABSOLUTE_BARRIER,
                    9, 5, 19, 2234, 30, 12_000, 12,
                    InventoryCatalog.MAGIC_GEM, 2, "absolute-barrier")
    );

    private static final Map<Integer, AdvanceSpiritSpell> ADVANCE_SPIRIT_SPELLS = Map.of(
            // Exact selected row 79 and LiTo.bin zero-based case 78: byte
            // table entry 4 enters loc_4CB71D/sub_4C9880 and writes cccd
            // (opcode, row, column, PC object id).  The retained skill mode
            // raises the PC target's maximum HP and MP by base/5 without
            // raising either current value.  The DB non-cancelable table
            // keeps the 1,200-second lease across skill 44 Cancellation.
            ADVANCE_SPIRIT, new AdvanceSpiritSpell(ADVANCE_SPIRIT,
                    9, 6, 19, 3935, 20, 10, 1_200,
                    -1, 1, 5, 280, "advance-spirit")
    );

    private static final Map<Integer, ShockStunSpell> SHOCK_STUN_SPELLS = Map.of(
            // Exact selected row 87 and LiTo.bin zero-based case 86.  The
            // client admits this case only while the actor's current weapon
            // walk family is 50 (two-hand sword) or 54 (edoryu), then emits
            // cccdhh through sub_4CA6E0: case86 >> 3 = row10 and
            // case86 & 7 = column6, therefore 14 0A 06 + target dword + x/y.
            // The retained server presents the cast as the weapon's ordinary
            // attack action, follows it with target gfx4434, and owns the
            // local-player action lock through S_PARALYSE modes 22/23.
            SHOCK_STUN, new ShockStunSpell(SHOCK_STUN,
                    10, 6, 19, 4434, 81162, 4183,
                    15, 8_000, 1, 10, 0, 10, ATTRIBUTE_UNSPECIFIED,
                    60, 45, 30, true, false, 0.05D, 10,
                    2, 2, 50, 54, 280, "shock-stun")
    );

    private static final Map<Integer, ArmorBreakSpell> ARMOR_BREAK_SPELLS = Map.of(
            // Exact selected row 112 and LiTo.bin zero-based case 111.  The
            // selected executable falls through its generic immediate branch
            // and emits selector-only 14 0D 07 even though the matching
            // official TW13081901 client enters the range-one object selector
            // and emits cccdhh.  The server therefore restores that missing
            // target intent conservatively without changing the wire bytes.
            // skills_負面技能控制 supplies the pure level tiers 50/40/30;
            // the boss row's divisor zero is full immunity, and the selected
            // attack path multiplies eligible melee damage by exactly 1.58.
            ARMOR_BREAK, new ArmorBreakSpell(ARMOR_BREAK,
                    13, 7, 19, 3400, 40, 30, 2_000, 1, 8,
                    50, 40, 30, 0,
                    InventoryCatalog.SECOND_LEVEL_DARK_STONE, 2,
                    280, "armor-break")
    );

    private static final Map<Integer, ReductionArmorSpell> REDUCTION_ARMOR_SPELLS = Map.of(
            // Exact selected row 88 and LiTo.bin zero-based case 87.  The
            // byte table enters loc_4CBC27, whose ccc producer writes opcode
            // 14 plus case87 >> 3 = row10 and case87 & 7 = column7.  The
            // selected retained damage paths intentionally distinguish a
            // player physical attacker (base 10) from NPC physical and all
            // magic paths (base 1), then add one point per five levels over
            // level 50.  skills_相消免疫技能 keeps this lease across skill44.
            REDUCTION_ARMOR, new ReductionArmorSpell(REDUCTION_ARMOR,
                    10, 7, 19, 4824, 5, 50, 3_000, 192,
                    50, 5, 10, 1, "reduction-armor")
    );

    private static final Map<Integer, BounceAttackSpell> BOUNCE_ATTACK_SPELLS = Map.of(
            // Exact selected row 89 and LiTo.bin zero-based case 88.  The
            // shared ccc producer writes opcode14 plus row11/column0.  Both
            // retained databases contribute AC-2 and melee HIT+6; accepted
            // melee hits against the protected player have the selected
            // retained 10% chance to damage the attacker's weapon.
            BOUNCE_ATTACK, new BounceAttackSpell(BOUNCE_ATTACK,
                    11, 0, 19, 4648, 5, 120, 5_000, 64,
                    -2, 6, 10, "bounce-attack")
    );

    private static final Map<Integer, SolidCarriageSpell> SOLID_CARRIAGE_SPELLS = Map.of(
            // Exact selected row 90 and LiTo.bin zero-based case 89. The
            // client first requires an inventory/equipment entry with native
            // use type 25, then writes selector-only 14 0B 01. The retained
            // server configuration accepts equipped armor type 7 (shield) or
            // type 13 (guarder), keeps the lease across Cancellation, and
            // L1PcInstance.getEr contributes +15 while it is active.
            SOLID_CARRIAGE, new SolidCarriageSpell(SOLID_CARRIAGE,
                    11, 1, 19, 5831, 10, 100, 3_000, 64,
                    15, "solid-carriage")
    );

    private static final Map<Integer, CounterBarrierSpell> COUNTER_BARRIER_SPELLS = Map.of(
            // Exact selected row 91 and LiTo.bin zero-based case 90. The
            // client requires the local actor's current weapon walk family to
            // be 0x32 (two-hand sword), then emits selector-only 14 0B 02.
            // The selected probability-control row is 55/45/35, includes
            // equipment/buff magic hit, omits magic level and subtracts
            // target MR * 0.05 before the retained global 90% cap. The
            // reflected source damage is (large+enchant+modifier)*2 and the
            // selected configuration multiplier is exactly 1.0.
            COUNTER_BARRIER, new CounterBarrierSpell(COUNTER_BARRIER,
                    11, 2, 19, 5832, 10, 100, 3_000, 64,
                    3, 55, 45, 35, false, true, 0.05D, 90,
                    0, 10710, 1.0D, "counter-barrier")
    );

    private static final Map<Integer, ReservedSkillSlot> RESERVED_SKILL_SLOTS = Map.ofEntries(
            // Both selected 380 databases and retained L1SkillId explicitly
            // classify row24/column7 as level-zero "none". Its skillbook row
            // has NULL item/count and there is no gameplay ability. Selected
            // LiTo.bin zero-based case23 enters the generic default producer,
            // so an externally forced invocation has exact selector-only
            // shape 14 02 07 and client clock0. Preserve and reject that wire
            // position; action19/gfx189 are inert row residue, not a spell.
            Map.entry(RESERVED_WIZARD_24, new ReservedSkillSlot(
                    RESERVED_WIZARD_24, 2, 7, 0, 0, 19, 189,
                    true, RequestShape.SELECTOR_ONLY)),
            // Both retained databases and L1SkillId classify 81..86 as
            // level-zero "none" slots.  Selected LiTo.bin zero-based cases
            // 80 and 82..85 enter the generic default ccc producer, while
            // case81 reaches the equivalent loc_4CBBD4 ccc producer.  Thus
            // all six exact requests (14 0A 00..05) are parseable native
            // traffic, but the rows are never mastery, advertised or run.
            // Historical reuse/duration/gfx residue remains source metadata
            // only and must not create gameplay effects or cooldowns.
            Map.entry(RESERVED_GENERAL_81, new ReservedSkillSlot(
                    RESERVED_GENERAL_81, 10, 0, 0, 0, 19, 0,
                    true, RequestShape.SELECTOR_ONLY)),
            Map.entry(RESERVED_GENERAL_82, new ReservedSkillSlot(
                    RESERVED_GENERAL_82, 10, 1, 0, 0, 19, 0,
                    true, RequestShape.SELECTOR_ONLY)),
            Map.entry(RESERVED_GENERAL_83, new ReservedSkillSlot(
                    RESERVED_GENERAL_83, 10, 2, 0, 0, 19, 0,
                    true, RequestShape.SELECTOR_ONLY)),
            Map.entry(RESERVED_GENERAL_84, new ReservedSkillSlot(
                    RESERVED_GENERAL_84, 10, 3, 0, 0, 19, 0,
                    true, RequestShape.SELECTOR_ONLY)),
            Map.entry(RESERVED_GENERAL_85, new ReservedSkillSlot(
                    RESERVED_GENERAL_85, 10, 4, 0, 900, 19, 2059,
                    true, RequestShape.SELECTOR_ONLY)),
            Map.entry(RESERVED_GENERAL_86, new ReservedSkillSlot(
                    RESERVED_GENERAL_86, 10, 5, 0, 30, 19, 760,
                    true, RequestShape.SELECTOR_ONLY)),
            // Both retained databases name 92..96 "none", assign skill level
            // zero, and provide no gameplay ability row.  L1SkillId labels the
            // entire interval none=92-96.  The cast-gfx values remain resource
            // metadata only; they do not make these rows learnable skills.
            // Recovered cases 91/93/94/95 use the generic selector-only ccc
            // producer, while case92 enters the object-selection callback and
            // would emit cccd.  All five are therefore parsed but never
            // advertised or executed by the server.
            Map.entry(RESERVED_KNIGHT_1, new ReservedSkillSlot(
                    RESERVED_KNIGHT_1, 11, 3, 0, 1_200, 19, 5906,
                    true, RequestShape.SELECTOR_ONLY)),
            Map.entry(RESERVED_KNIGHT_2, new ReservedSkillSlot(
                    RESERVED_KNIGHT_2, 11, 4, 0, 1_200, 19, 5907,
                    true, RequestShape.OBJECT_TARGET)),
            Map.entry(RESERVED_KNIGHT_3, new ReservedSkillSlot(
                    RESERVED_KNIGHT_3, 11, 5, 0, 1_200, 19, 5908,
                    true, RequestShape.SELECTOR_ONLY)),
            Map.entry(RESERVED_KNIGHT_4, new ReservedSkillSlot(
                    RESERVED_KNIGHT_4, 11, 6, 0, 1_200, 19, 5909,
                    true, RequestShape.SELECTOR_ONLY)),
            Map.entry(RESERVED_KNIGHT_5, new ReservedSkillSlot(
                    RESERVED_KNIGHT_5, 11, 7, 0, 1_200, 19, 5910,
                    true, RequestShape.SELECTOR_ONLY)),
            // Exact retained rows 119..128 are a second native gap.  Rows
            // 119/120 misleadingly retain positive class levels, while 121
            // and 123..128 are level zero and row 122 is absent altogether.
            // Both selected and official clients emit selector-only ccc for
            // 119/120; the selected client also routes 121..128 through its
            // generic selector-only producer.  None has retained gameplay
            // semantics, so every shape is parsed and rejected explicitly.
            Map.entry(RESERVED_ELF_119, new ReservedSkillSlot(
                    RESERVED_ELF_119, 14, 6, 15, 0, 19, 0,
                    true, RequestShape.SELECTOR_ONLY)),
            Map.entry(RESERVED_ELF_120, new ReservedSkillSlot(
                    RESERVED_ELF_120, 14, 7, 15, 0, 19, 2280,
                    true, RequestShape.SELECTOR_ONLY)),
            Map.entry(RESERVED_ELF_121, new ReservedSkillSlot(
                    RESERVED_ELF_121, 15, 0, 0, 0, 19, 0,
                    true, RequestShape.SELECTOR_ONLY)),
            Map.entry(RESERVED_ELF_122, new ReservedSkillSlot(
                    RESERVED_ELF_122, 15, 1, 0, 0, 19, 0,
                    false, RequestShape.SELECTOR_ONLY)),
            Map.entry(RESERVED_ELF_123, new ReservedSkillSlot(
                    RESERVED_ELF_123, 15, 2, 0, 0, 19, 0,
                    true, RequestShape.SELECTOR_ONLY)),
            Map.entry(RESERVED_ELF_124, new ReservedSkillSlot(
                    RESERVED_ELF_124, 15, 3, 0, 0, 19, 0,
                    true, RequestShape.SELECTOR_ONLY)),
            Map.entry(RESERVED_ELF_125, new ReservedSkillSlot(
                    RESERVED_ELF_125, 15, 4, 0, 0, 19, 0,
                    true, RequestShape.SELECTOR_ONLY)),
            Map.entry(RESERVED_ELF_126, new ReservedSkillSlot(
                    RESERVED_ELF_126, 15, 5, 0, 0, 19, 0,
                    true, RequestShape.SELECTOR_ONLY)),
            Map.entry(RESERVED_ELF_127, new ReservedSkillSlot(
                    RESERVED_ELF_127, 15, 6, 0, 0, 19, 0,
                    true, RequestShape.SELECTOR_ONLY)),
            Map.entry(RESERVED_ELF_128, new ReservedSkillSlot(
                    RESERVED_ELF_128, 15, 7, 0, 0, 19, 0,
                    true, RequestShape.SELECTOR_ONLY)),
            // Exact retained rows 135/136 are named "none", have level zero,
            // native id128 and no gameplay ability.  The recovered LiTo jump
            // table routes zero-based cases134/135 to its default/no-producer
            // branch, so unlike earlier reserved rows there is no legitimate
            // C_USE_SPELL shape for either selector.  Keeping the positions in
            // the catalog prevents them from being mistaken for missing work.
            Map.entry(RESERVED_ELF_135, new ReservedSkillSlot(
                    RESERVED_ELF_135, 16, 6, 0, 0, 19, 0,
                    true, RequestShape.NO_PRODUCER)),
            Map.entry(RESERVED_ELF_136, new ReservedSkillSlot(
                    RESERVED_ELF_136, 16, 7, 0, 0, 19, 0,
                    true, RequestShape.NO_PRODUCER)),
            // Exact retained rows139..144 are the six level-zero "none"
            // positions immediately after Resist Elemental. Their native IDs
            // preserve the selector bitmap only (4,8,16,32,64,128); there are
            // no ability or gameplay semantics. Recovered zero-based cases
            // 138..143 all route through entry31 to 0x004CBC27 with cast-clock
            // zero, the same no-producer branch as reserved135/136. The server
            // records and excludes these positions rather than inventing ccc
            // packets that the selected client cannot emit.
            Map.entry(RESERVED_ELF_139, new ReservedSkillSlot(
                    RESERVED_ELF_139, 17, 2, 0, 0, 19, 0,
                    true, RequestShape.NO_PRODUCER)),
            Map.entry(RESERVED_ELF_140, new ReservedSkillSlot(
                    RESERVED_ELF_140, 17, 3, 0, 0, 19, 0,
                    true, RequestShape.NO_PRODUCER)),
            Map.entry(RESERVED_ELF_141, new ReservedSkillSlot(
                    RESERVED_ELF_141, 17, 4, 0, 0, 19, 0,
                    true, RequestShape.NO_PRODUCER)),
            Map.entry(RESERVED_ELF_142, new ReservedSkillSlot(
                    RESERVED_ELF_142, 17, 5, 0, 0, 19, 0,
                    true, RequestShape.NO_PRODUCER)),
            Map.entry(RESERVED_ELF_143, new ReservedSkillSlot(
                    RESERVED_ELF_143, 17, 6, 0, 0, 19, 0,
                    true, RequestShape.NO_PRODUCER)),
            Map.entry(RESERVED_ELF_144, new ReservedSkillSlot(
                    RESERVED_ELF_144, 17, 7, 0, 0, 19, 0,
                    true, RequestShape.NO_PRODUCER)),
            // Exact retained rows177..180 are Dragon Knight bitmap positions
            // with level zero, target "none", action/gfx/duration zero and no
            // gameplay ability.  Selected LiTo.bin sends them through the
            // generic selector-only ccc producer (14 16 00..03), so preserve
            // the wire positions without advertising or inventing effects.
            Map.entry(RESERVED_DRAGON_177, new ReservedSkillSlot(
                    RESERVED_DRAGON_177, 22, 0, 0, 0, 0, 0,
                    true, RequestShape.SELECTOR_ONLY)),
            Map.entry(RESERVED_DRAGON_178, new ReservedSkillSlot(
                    RESERVED_DRAGON_178, 22, 1, 0, 0, 0, 0,
                    true, RequestShape.SELECTOR_ONLY)),
            Map.entry(RESERVED_DRAGON_179, new ReservedSkillSlot(
                    RESERVED_DRAGON_179, 22, 2, 0, 0, 0, 0,
                    true, RequestShape.SELECTOR_ONLY)),
            Map.entry(RESERVED_DRAGON_180, new ReservedSkillSlot(
                    RESERVED_DRAGON_180, 22, 3, 0, 0, 0, 0,
                    true, RequestShape.SELECTOR_ONLY))
    );

    private static final Map<Integer, MagicResistanceSpell>
            MAGIC_RESISTANCE_SPELLS = Map.of(
            // Exact retained row 129 and selected LiTo.bin zero-based case
            // 128: generic selector-only ccc emits 14 10 00.  The Elf
            // circle-17 self buff consumes MP5 and one Spirit Crystal,
            // lasts 1,200 seconds, broadcasts action19/gfx2186, adds MR10,
            // bypasses Counter Magic and publishes stop message 719.
            RESIST_MAGIC, new MagicResistanceSpell(
                    RESIST_MAGIC, 16, 0, 17, 0, 19, 2186,
                    5, 0, 0, 1_200, 0, 0, 0,
                    ATTRIBUTE_UNSPECIFIED, 2, 1,
                    InventoryCatalog.SPIRIT_CRYSTAL, 1,
                    10, 719, true, "resist-magic")
    );

    private static final Map<Integer, BodyToMindSpell>
            BODY_TO_MIND_SPELLS = Map.of(
            // Exact selected row 130 and LiTo.bin zero-based case129:
            // generic selector-only ccc emits 14 10 01.  The instant Elf
            // conversion consumes HP8 while preserving at least one HP,
            // adds MP2 up to maximum MP, broadcasts action19/gfx2179 and
            // publishes happen message702.  The selected 380_db reuse is
            // 100ms; the older 380yiwei_db row retains 400ms.
            BODY_TO_MIND, new BodyToMindSpell(
                    BODY_TO_MIND, 16, 1, 17, 1, 19, 2179,
                    0, 8, 100, 400, 0,
                    0, 0, 0, ATTRIBUTE_UNSPECIFIED, 2, 2,
                    0, 0, 702, 0, 0, 2,
                    true, "body-to-mind"),
            // Exact selected/secondary row146 and recovered LiTo zero-based
            // case145: the generic selector-only producer emits 14 12 01.
            // This is BLOOD_TO_SOUL, distinct from skill130 despite the
            // retained Chinese name sharing "魂體轉換".  It preserves at least
            // one HP, consumes HP40, gains the selected server-configured MP20
            // (both retained configurations agree), reuses after1,200ms and
            // broadcasts action19/gfx2178 without a happen message or lease.
            BLOODY_SOUL, new BodyToMindSpell(
                    BLOODY_SOUL, 18, 1, 19, 1, 19, 2178,
                    0, 40, 1_200, 1_200, 0,
                    0, 0, 0, ATTRIBUTE_UNSPECIFIED, 2, 2,
                    0, 0, 0, 0, 0, 20,
                    true, "bloody-soul")
    );

    private static final Map<Integer, WorldTreeCallSpell>
            WORLD_TREE_CALL_SPELLS = Map.of(
            // Exact selected row 131.  The product LiTo.bin shipped with its
            // zero-based case130 routed to a no-op branch; the guarded LiTo-
            // only compatibility repair restores the existing generic ccc
            // producer and emits 14 10 02.  The retained skill mode consumes
            // MP10 and teleports to the Mother Tree at map4:33051,32337
            // heading5 only when the optional classic-world compatibility
            // laboratory is enabled.  Exact LiTo.bin has no LTMap\\4 terrain;
            // production therefore returns message276 plus teleport unlock
            // without spending MP or emitting S_PORTAL.
            WORLD_TREE_CALL, new WorldTreeCallSpell(
                    WORLD_TREE_CALL, 16, 2, 17, 2, 19, 169,
                    10, 0, 0, 0, 0, 0, 0,
                    ATTRIBUTE_UNSPECIFIED, 128, 4,
                    0, 0, 4, 33_051, 32_337, 5,
                    276, true, "world-tree-call")
    );

    private static final Map<Integer, TripleArrowSpell>
            TRIPLE_ARROW_SPELLS = Map.of(
            // Exact selected row132 and LiTo.bin zero-based case131:
            // the bow/shape-gated cccdhh producer emits 14 10 03 + target
            // object/x/y. Retained TRIPLE_ARROW runs the ordinary physical
            // onAction path three times, consumes one projectile per admitted
            // ranged attack, then broadcasts S_SkillSound gfx4394. Selected
            // 380_db reuse100ms wins over the older 380yiwei_db reuse400ms.
            TRIPLE_ARROW, new TripleArrowSpell(
                    TRIPLE_ARROW, 16, 3, 17, 3, 18, 4_394,
                    11, 100, 400, 0, 3, 0, -1, 0,
                    ATTRIBUTE_UNSPECIFIED, 64, 8, 3,
                    true, "triple-arrow")
    );

    private static final Map<Integer, ElementalFallDownSpell>
            ELEMENTAL_FALL_DOWN_SPELLS = Map.of(
            // Exact selected row133 and LiTo.bin zero-based case132:
            // loc_4CB755 invokes the targeted cccdhh producer with native
            // click range10 and emits 14 10 04 + object/x/y.  The selected
            // source consumes MP10, reuses after100ms, lasts32s and uses
            // action19/gfx4396. Its exact negative-control row is
            // 55/45/35 + INT*0.30 + MagicHit - MR*0.05; bosses are immune.
            // Counter Magic is not bypassed. Retained start lowers the
            // caster ElfAttr's lowest set elemental bit by50; recasts only
            // refresh the existing lease and preserve its original element.
            ELEMENTAL_FALL_DOWN, new ElementalFallDownSpell(
                    ELEMENTAL_FALL_DOWN, 16, 4, 17, 4, 19, 4_396,
                    10, 100, 32, 3, 10, 0,
                    ATTRIBUTE_UNSPECIFIED, 1, 16, 280, 50,
                    55, 45, 35, 0.30D, 0.05D,
                    false, true, 0, false,
                    "elemental-fall-down")
    );

    private static final Map<Integer, ReturnToNatureSpell>
            RETURN_TO_NATURE_SPELLS = Map.of(
            // Exact selected/secondary row145 and LiTo.bin zero-based case144:
            // the ordinary cccdhh object/coordinate producer emits
            // 14 12 00 + d(target) + h(x) + h(y).  The Elf circle-19 spell
            // consumes MP30 and two Spirit Crystals, casts action19/gfx2245,
            // and accepts pet-domain objects on the complete native screen.
            // Its selected negative-control formula is 55/45/35 + INT*0.30
            // + MagicHit - MR*0.05, using the summon master's level as the
            // defense level.  Persistent pets are deliberately not summons:
            // the retained special mode reports message79 and leaves them.
            RETURN_TO_NATURE, new ReturnToNatureSpell(
                    RETURN_TO_NATURE, 18, 0, 19, 0, 19, 2_245,
                    30, 0, 0, 16, -1, 0,
                    ATTRIBUTE_UNSPECIFIED, 1, 1,
                    InventoryCatalog.SPIRIT_CRYSTAL, 2,
                    280, 79, 55, 45, 35, 0.30D, 0.05D,
                    false, true, 0, false,
                    "return-to-nature")
    );

    private static final Map<Integer, CounterMirrorSpell>
            COUNTER_MIRROR_SPELLS = Map.of(
            // Exact selected row134 and LiTo.bin zero-based case133:
            // generic selector-only ccc emits 14 10 05.  The self ward
            // consumes MP10 and one Spirit Crystal, reuses after1000ms,
            // lasts16s and casts action19/gfx4395 owner-only. Retained
            // L1MagicPc/L1MagicNpc compares WIS >= random.nextInt(100);
            // success reflects the complete resolved magic damage, zeros
            // the defender damage and consumes the lease. NPC magic is
            // eligible only for IsErase sources outside the dragon denylist.
            COUNTER_MIRROR, new CounterMirrorSpell(
                    COUNTER_MIRROR, 16, 5, 17, 5, 19, 4_395,
                    10, 1_000, 0, 16, 0, 0, 0,
                    ATTRIBUTE_UNSPECIFIED, 2, 32,
                    InventoryCatalog.SPIRIT_CRYSTAL, 1, 280,
                    "counter-mirror")
    );

    private static final Map<Integer, ClearMindSpell>
            CLEAR_MIND_SPELLS = Map.of(
            // Exact retained row137 and recovered LiTo zero-based case136:
            // generic selector-only ccc emits 14 11 00.  The Elf circle-18
            // self buff consumes MP10 and one Spirit Crystal, lasts 1,200s,
            // broadcasts action19/gfx2180 and publishes happen message722.
            // Both retained skills_buff_ability tables define the actual
            // ability as WIS+3; L1SkillUse then refreshes status and base MR.
            CLEAR_MIND, new ClearMindSpell(
                    CLEAR_MIND, 17, 0, 18, 0, 19, 2_180,
                    10, 0, 0, 1_200, 0, 0, 0,
                    ATTRIBUTE_UNSPECIFIED, 2, 1,
                    InventoryCatalog.SPIRIT_CRYSTAL, 1,
                    3, 722, true, "clear-mind")
    );

    private static final Map<Integer, ResistElementalSpell>
            RESIST_ELEMENTAL_SPELLS = Map.of(
            // Exact retained row138 and recovered LiTo zero-based case137:
            // generic selector-only ccc emits 14 11 01.  The Elf circle-18
            // self buff consumes MP10 and one Spirit Crystal, lasts 1,200s,
            // broadcasts action19/gfx2184 and publishes happen message721.
            // Retained L1SkillUse/L1SkillStop add/remove 10 from fire, water,
            // wind and earth resistance and publish S_OwnCharAttrDef.
            RESIST_ELEMENTAL, new ResistElementalSpell(
                    RESIST_ELEMENTAL, 17, 1, 18, 1, 19, 2_184,
                    10, 0, 0, 1_200, 0, 0, 0,
                    ATTRIBUTE_UNSPECIFIED, 2, 2,
                    InventoryCatalog.SPIRIT_CRYSTAL, 1,
                    10, 721, true, "resist-elemental")
    );

    private static final Map<Integer, ElementalProtectionSpell>
            ELEMENTAL_PROTECTION_SPELLS = Map.of(
            // Exact retained row147 and recovered LiTo zero-based case146:
            // generic selector-only ccc emits 14 12 02.  The Elf circle-19
            // self buff requires a nonzero ElfAttr, consumes MP6 and one
            // Spirit Crystal, lasts 64s, and broadcasts action19/gfx2285.
            // Retained L1SkillUse/L1SkillStop add/remove 50 for every set bit
            // in the cast-time ElfAttr mask and publish S_OwnCharAttrDef.
            ELEMENTAL_PROTECTION, new ElementalProtectionSpell(
                    ELEMENTAL_PROTECTION, 18, 2, 19, 2, 19, 2_285,
                    6, 0, 0, 64, 0, 0, 0,
                    ATTRIBUTE_UNSPECIFIED, 2, 4,
                    InventoryCatalog.SPIRIT_CRYSTAL, 1,
                    50, 0, true, "elemental-protection")
    );

    private static final Map<Integer, FireWeaponSpell>
            FIRE_WEAPON_SPELLS = Map.of(
            // Exact selected/secondary row148 and recovered LiTo zero-based
            // case147: object-only cccd emits 14 12 03 plus target object id.
            // maximumRange=0 restricts the PC target to the caster's tile;
            // ordinary collision therefore makes this a native self buff.
            // Both retained DB rows set attr=0, so isAttrAgrees bypasses the
            // ElfAttr gate.  It consumes MP15, lasts 960s, broadcasts
            // action19/gfx2182 and publishes happen message723.
            // skills_buff_ability defines a coexistable melee DMG +4 lease.
            FIRE_WEAPON, new FireWeaponSpell(
                    FIRE_WEAPON, 18, 3, 19, 3, 19, 2_182,
                    15, 0, 0, 960, 0, 0, 1,
                    ATTRIBUTE_UNSPECIFIED, 2, 8,
                    0, 4, 723, 280, true, "fire-weapon"),
            // Exact selected row163 and LiTo.bin zero-based case162:
            // loc_4CB71D -> sub_4C9880 emits object-only cccd with the caster
            // object selected by the target_to=0 flow.  It consumes MP30,
            // lasts 960s, broadcasts action19/gfx2242 and happen723.  Exact
            // attr=2 requires the caster's ElfAttr mask to include fire;
            // selected skills_buff_ability rows define melee HIT+3 and DMG+6.
            BURNING_WEAPON, new FireWeaponSpell(
                    BURNING_WEAPON, 20, 2, 21, 2, 19, 2_242,
                    30, 0, 0, 960, 0, 0, 0,
                    ATTRIBUTE_FIRE, 2, 4,
                    3, 6, 723, 280, true, "burning-weapon")
    );

    private static final Map<Integer, WindShotSpell>
            WIND_SHOT_SPELLS = Map.of(
            // Exact selected/secondary row149 and recovered LiTo zero-based
            // case148: object-only cccd emits 14 12 04 plus target object id.
            // Retained maximumRange=0 makes the PC target self-only in normal
            // collision.  Both retained DB rows set attr=0, so there is no
            // ElfAttr cast gate. MP15 and duration960 are exact.
            // skills_buff_ability defines only coexistable BOW_HIT +6.
            WIND_SHOT, new WindShotSpell(
                    WIND_SHOT, 18, 4, 19, 4, 19, 2_246,
                    15, 0, 0, 960, 0, 0, 1,
                    ATTRIBUTE_UNSPECIFIED, 2, 16,
                     6, 724, 280, true, "wind-shot")
    );

    private static final Map<Integer, FireBlessSpell>
            FIRE_BLESS_SPELLS = Map.of(
            // Exact selected/secondary row155 and recovered LiTo zero-based
            // case154: selector-only ccc emits 14 13 02. target="none",
            // target_to=PARTY(8) and area=-1 select the caster plus living
            // same-map party PCs in the native screen. MP40 and duration960
            // are fixed; action19/gfx2286 is rendered on every affected PC.
            // skills_buff_ability supplies melee-only DMG+4 and retained
            // CharBuffTable persists the lease. SkillCoexistSet=1 is active
            // in the selected source, so it does not delete Fire Weapon or
            // Wind Shot. The legacy aura icon body has no selected receiver.
            FIRE_BLESS, new FireBlessSpell(
                    FIRE_BLESS, 19, 2, 20, 2, 19, 2_286,
                    40, 0, 0, 960, 0, -1, 8,
                    ATTRIBUTE_UNSPECIFIED, 2, 4,
                    4, 723, true, "fire-bless")
    );

    private static final Map<Integer, StormEyeSpell>
            STORM_EYE_SPELLS = Map.of(
            // Exact selected/secondary row156 and recovered LiTo zero-based
            // case155: selector-only ccc emits 14 13 03. target="none",
            // target_to=PARTY(8) and area=-1 select the caster plus living
            // same-map party PCs in the native screen. MP40 and duration960
            // are fixed; action19/gfx2288 is rendered on every affected PC.
            // skills_buff_ability supplies ranged-only BOW_HIT+2/BOW_DMG+3.
            // CharBuffTable persists the lease and SkillCoexistSet=1 keeps it
            // independent from Wind Shot/Fire Bless. The later aura icon body
            // has no compatible receiver in the selected LiTo client.
            STORM_EYE, new StormEyeSpell(
                    STORM_EYE, 19, 3, 20, 3, 19, 2_288,
                    40, 0, 0, 960, 0, -1, 8,
                    ATTRIBUTE_UNSPECIFIED, 2, 8,
                    2, 3, 724, true, "storm-eye")
    );

    private static final Map<Integer, StormShotSpell>
            STORM_SHOT_SPELLS = Map.of(
            // Exact selected/secondary row166 and LiTo.bin zero-based case165:
            // loc_4CB71D -> sub_4C9880 emits object-only cccd with the caster
            // object selected by target_to=0. The unpacked PE cast-clock word
            // at VA 0x6358BE is zero. MP30, duration960, action19/gfx2248,
            // attr=0 and native id32 are exact. Retained isAttrAgrees therefore
            // has no execution-time element gate; the level/attribute skill
            // list is what grants this skill only to wind elves normally.
            // skills_buff_ability supplies ranged BOW_HIT-1/BOW_DMG+5; the
            // selected SkillCoexistSet=1 keeps this independent of Storm Eye.
            STORM_SHOT, new StormShotSpell(
                    STORM_SHOT, 20, 5, 21, 5, 19, 2_248,
                    30, 0, 0, 960, 0, 0, 0,
                    ATTRIBUTE_UNSPECIFIED, 2, 32,
                    -1, 5, 724, 280, true, "storm-shot")
    );

    private static final Map<Integer, WindShackleSpell>
            WIND_SHACKLE_SPELLS = Map.of(
            // Exact selected case166 enters loc_4CB78B/sub_4CA6E0 with
            // client click range8 and emits cccdhh: row20/column6, target
            // object id and coordinate snapshot. The unpacked PE word at
            // 0x6358C0 is 600ms. Primary/secondary DB server reuse values
            // are 100/0ms and remain distinct from that client cast clock.
            // area=-1 applies the probability spell to all valid PC/NPC
            // actors on the caster's native screen. Wind attr8, MP50,
            // Spirit Crystal40319x1, duration16, action19/gfx1799 and
            // happen/failure1001/280 are exact. Negative control supplies
            // 55/45/35 + INT*0.30 + MagicHit - MR*0.05; bosses are immune.
            WIND_SHACKLE, new WindShackleSpell(
                    WIND_SHACKLE, 20, 6, 21, 6, 19, 1_799,
                    50, 100, 0, 600, 16, -1, 8, -1, 3,
                    ATTRIBUTE_WIND, 1, 64, 1_001, 0, 280,
                    55, 45, 35, 0.30D, 0.05D,
                    false, true, 0, false,
                    InventoryCatalog.SPIRIT_CRYSTAL, 1, "wind-shackle")
    );

    private static final Map<Integer, NaturesTouchSpell>
            NATURES_TOUCH_SPELLS = Map.of(
            // Exact selected/secondary row158 and recovered LiTo zero-based
            // case157: object-only cccd emits 14 13 05 plus target object id.
            // The retained target is one living PC on the native screen. MP20,
            // duration320, action19 and gfx2243 are exact. PcRegenExecutor
            // contributes +15 to each due natural-HPR event. Applying this
            // buff consumes the target's one-shot Water Life lease. It is not
            // present in CharBuffTable and therefore is intentionally transient.
            NATURES_TOUCH, new NaturesTouchSpell(
                    NATURES_TOUCH, 19, 5, 20, 5, 19, 2_243,
                    20, 0, 0, 320, -1, 0, 1,
                    ATTRIBUTE_UNSPECIFIED, 2, 32,
                    BattleNaturesTouchStatus.NATIVE_HPR_BONUS,
                    0, 280, true, "natures-touch")
    );

    private static final Map<Integer, EarthBlessSpell>
            EARTH_BLESS_SPELLS = Map.of(
            // Exact selected/secondary row159 and LiTo zero-based case158:
            // selector-only ccc emits 14 13 06. target="none", PARTY bit8
            // and area=-1 select caster plus living same-map party PCs in the
            // native screen. MP40, duration960, action19, gfx2287, stop725
            // and AC-7 are exact. Selected SkillCoexistSet=1 requires this
            // lease to remain independent from Shield/Earth Skin. CharBuffTable
            // persists it. In selected coexist mode the retained server uses
            // custom S_IconConfig graphic527; legacy mode uses shield type7.
            EARTH_BLESS, new EarthBlessSpell(
                    EARTH_BLESS, 19, 6, 20, 6, 19, 2_287,
                    40, 0, 0, 960, 0, -1, 8,
                    ATTRIBUTE_UNSPECIFIED, 2, 64,
                    -7, 7, 527, 0, 725, 0,
                    true, "earth-bless")
    );

    private static final Map<Integer, IronSkinSpell>
            IRON_SKIN_SPELLS = Map.of(
            // Exact selected/secondary row168 and LiTo zero-based case167:
            // loc_4CB71D/sub_4C9880 emits object-only cccd row20/column7
            // with the caster object id. Both server rows and the selected
            // PE timing word at 0x6358C2 pin reuse0. MP30, duration960,
            // action19/gfx2252, happen714, stop725 and failure280 are exact.
            // AC-10 comes from skills_buff_ability. Selected
            // SkillCoexistSet=1 requires an independent lease alongside
            // Shield/Earth Skin/Earth Bless; retained CharBuffTable persists
            // it and custom coexist graphic528 is not a legacy shield icon.
            IRON_SKIN, new IronSkinSpell(
                    IRON_SKIN, 20, 7, 21, 7, 19, 2_252,
                    30, 0, 0, 960, 0, 0, 0,
                    ATTRIBUTE_UNSPECIFIED, 2, 128,
                    -10, 10, 528, 714, 725, 280,
                    true, "iron-skin")
    );

    private static final Map<Integer, DragonSkinSpell>
            DRAGON_SKIN_SPELLS = Map.of(
            // Exact selected/retained row181 and LiTo zero-based case180:
            // the post-175 generic ccc branch emits selector-only 14 16 04.
            // Selected PE cast-clock word is zero.  The primary retained row
            // costs HP15, reuses after 10ms, lasts 1,200 seconds and uses
            // action19/gfx6531. L1AttackList.SKD3 assigns flat -10 physical
            // damage for both PC and NPC/pet sources. L1SkillUse exempts this
            // skill from Counter Magic, while Cancellation does not exempt it.
            DRAGON_SKIN, new DragonSkinSpell(
                    DRAGON_SKIN, 22, 4, 23, 4, 19, 6_531,
                    0, 15, 10, 0, 1_200, 0, 0, 0,
                    ATTRIBUTE_UNSPECIFIED, 2, 16,
                     10, 0, 0, 0, true, "dragon-skin")
    );

    private static final Map<Integer, AwakenSpell> AWAKEN_SPELLS = Map.of(
            // Exact selected row185 and LiTo zero-based case184: the post-175
            // generic producer emits selector-only 14 17 00.  The selected
            // row consumes HP1 plus one item49157, has no initial MP cost,
            // no reuse/duration, action19/gfx6975 and native id1.  Selected
            // skills_buff_ability adds AC-12 and maximum HP+127.  The retained
            // classic awakening lifecycle uses dragon gfx9362, drains up to
            // 8 MP every 4 seconds, toggles off on the same cast, bypasses
            // Counter Magic and survives Cancellation.
            AWAKEN_ANTHARAS, new AwakenSpell(
                    AWAKEN_ANTHARAS, 23, 0, 24, 0, 19, 6_975,
                    0, 1, InventoryCatalog.AWAKENING_BONE_FRAGMENT, 1,
                    0, 0, 0, 0, 0, 0,
                    ATTRIBUTE_UNSPECIFIED, 2, 1,
                    -12, 127, 9_362, 8, 4,
                    0, 0, 0,
                    0, 0, 0, true, true, "awaken-antharas"),
            // Exact selected row190 and LiTo zero-based case189: the same
            // post-175 generic producer emits selector-only 14 17 05 and the
            // selected PE cast-clock word is zero. Both retained skill rows
            // consume HP1 plus item49157x1, use action19/gfx6976, native id32
            // and have no initial MP cost, reuse or finite duration. The 3.81
            // primary AWAKEN_FAFURION mode adds MR30 plus fire/water/wind/
            // earth resistance30, uses dragon gfx9364 and shares the retained
            // 8 MP / 4 second awakening drain, same-cast toggle, Counter Magic
            // bypass, Cancellation immunity and cross-awakening exclusion.
            AWAKEN_FAFURION, new AwakenSpell(
                    AWAKEN_FAFURION, 23, 5, 24, 5, 19, 6_976,
                    0, 1, InventoryCatalog.AWAKENING_BONE_FRAGMENT, 1,
                    0, 0, 0, 0, 0, 0,
                    ATTRIBUTE_UNSPECIFIED, 2, 32,
                    0, 0, 9_364, 8, 4,
                    30, 30, 0,
                    0, 0, 0, true, true, "awaken-fafurion"),
            // Exact selected row195 and LiTo zero-based case194: the generic
            // producer emits selector-only 14 18 02 and its PE cast clock is
            // zero. Both selected databases consume HP1 and item49157x2,
            // use action19/gfx6922/native-id4, and apply +5 to STR/CON/DEX/
            // CHA/INT/WIS. The retained lifecycle uses dragon gfx9363, drains
            // 8 MP every 4 seconds, toggles on the same cast, bypasses Counter
            // Magic, survives Cancellation and excludes another awakening.
            AWAKEN_VALAKAS, new AwakenSpell(
                    AWAKEN_VALAKAS, 24, 2, 25, 2, 19, 6_922,
                    0, 1, InventoryCatalog.AWAKENING_BONE_FRAGMENT, 2,
                    0, 0, 0, 0, 0, 0,
                    ATTRIBUTE_UNSPECIFIED, 2, 4,
                    0, 0, 9_363, 8, 4,
                    0, 0, 5,
                    0, 0, 0, true, true, "awaken-valakas")
    );

    private static final Map<Integer, MortalBodySpell> MORTAL_BODY_SPELLS = Map.of(
            // Exact selected row191 and LiTo zero-based case190. The generic
            // post-175 producer emits selector-only 14 17 06. Its PE cast
            // clock is 30ms, independently of the selected database's 1000ms
            // server reuse. The retained self buff costs HP50, lasts 300s and
            // uses action19/gfx6519/native-id64. Its negative-control row is
            // pure holder/attacker level tiers 35/25/15 with the global 90%
            // ceiling; boss modifier0 suppresses reflection against bosses.
            // The selected runtime property fixes base reflection at20, adds
            // a PC attacker's non-negative weapon enchant, then applies that
            // attacker's armor/PvP/melee reductions with a minimum of one.
            MORTAL_BODY, new MortalBodySpell(
                    MORTAL_BODY, 23, 6, 24, 6, 19, 6_519,
                    0, 50, 1_000, 30, 300, 64,
                    35, 25, 15, 90, 0, 20,
                    true, false, "mortal-body")
    );

    private static final Map<Integer, BloodlustSpell> BLOODLUST_SPELLS = Map.of(
            // Exact selected row186 and LiTo zero-based case185: the generic
            // post-175 producer emits selector-only 14 17 01 and its PE
            // cast-clock word is 0ms. Primary selected metadata wins over the
            // secondary historical reuse0 row: HP30, MP0, item49157x1,
            // reuse10ms, duration60, action19/gfx6523 and native id2.
            // Retained BLOODLUST replaces the complete brave/movement family,
            // emits S_SkillBrave mode6 and applies 0.775 only to ATTACK.
            BLOODLUST, new BloodlustSpell(
                    BLOODLUST, 23, 1, 24, 1, 19, 6_523,
                    0, 30, InventoryCatalog.AWAKENING_BONE_FRAGMENT, 1,
                    10, 0, 60, 0, 0, 0,
                    ATTRIBUTE_UNSPECIFIED, 2, 2,
                    0, 0, 0, true, false,
                    BattleTimedStatus.BRAVE_BLOODLUST,
                    BattleActionInterval.FAST_ATTACK_MULTIPLIER,
                    "bloodlust")
    );

    private static final Map<Integer, FoeSlayerSpell> FOE_SLAYER_SPELLS = Map.of(
            // Exact selected row187 and LiTo zero-based index186.  The generic
            // post-175 producer emits selector-only 14 17 02 and the selected
            // PE cast-clock word is 0ms.  Primary metadata supplies HP20,
            // reuse2000ms, range2, target_to3, action18 and native id4.  The
            // retained classic mode performs three ordinary physical onAction
            // calls, adds +20/+40/+60 for the consumed exposure level to each
            // successful strike, clears exposure after the third call, then
            // emits attacker gfx7020 and shared-source target gfx6509.
            FOE_SLAYER, new FoeSlayerSpell(
                    FOE_SLAYER, 23, 2, 24, 2, 18, 0,
                    0, 20, 2_000, 0, 0, 2, 0, 3,
                    ATTRIBUTE_UNSPECIFIED, 64, 4,
                    3, 20, 7_020, 6_509,
                    0, 0, 0, true, "foe-slayer")
    );

    private static final Map<Integer, BurningSlashSpell>
            BURNING_SLASH_SPELLS = Map.of(
            // Exact selected/secondary row182 and LiTo zero-based case181:
            // the post-175 generic ccc branch emits selector-only 14 16 05.
            // The selected PE cast-clock word is 3,600ms. Primary retained
            // metadata costs HP10, reuses after 2,000ms, lasts 60 seconds and
            // broadcasts action19/gfx6590. Four independent retained combat
            // sources agree that the next eligible ordinary melee hit adds
            // exactly 10 source damage, emits location gfx6591, then consumes
            // the lease. Bow, gauntlet and Kiringku paths return before it.
            BURNING_SLASH, new BurningSlashSpell(
                    BURNING_SLASH, 22, 5, 23, 5, 19, 6_590,
                    0, 10, 2_000, 3_600, 60, 0, 0, 0,
                    ATTRIBUTE_FIRE, 2, 32,
                    10, 6_591, 0, 0, 0, true, "burning-slash")
    );

    private static final Map<Integer, GuardBrakeSpell> GUARD_BRAKE_SPELLS = Map.of(
            // Exact selected/secondary row183 and selected LiTo zero-based
            // case182: sub_4CB5F0 routes every case above 175 through the
            // generic ccc producer, yielding selector-only 14 16 06.  The
            // selected database supplies MP0/HP25, range3, 60 seconds,
            // action19/gfx6508 and the player-only AC+15 lease.  Its negative
            // probability row is 55/45/35 + INT*0.35 + magic-hit - MR*0.05,
            // with no magic-level term; boss modifier zero means immunity.
            GUARD_BRAKE, new GuardBrakeSpell(
                    GUARD_BRAKE, 22, 6, 23, 6, 19, 6_508,
                    0, 25, 1_000, 1_800, 60, 3, 0, 26,
                    ATTRIBUTE_UNSPECIFIED, 3, 64,
                    55, 45, 35, 0.35D, true, false, 0.05D, 0,
                    15, 0, 0, 280, false, "guard-brake"),
            // Exact primary/secondary row193 and selected LiTo zero-based
            // case192: the same post-175 ccc producer emits selector-only
            // 14 18 00 and the selected PE cast-clock word is zero. Primary
            // metadata costs HP50/MP0, reuses after 1,000ms, lasts 60 seconds,
            // targets range3 and broadcasts action19/gfx6588. Its probability
            // row is 55/45/35 + INT*0.35 + magic-hit - MR*0.05 with no magic
            // level; boss modifier zero means immunity. The retained 3.81
            // player branch applies STR-5 and INT-5. Skill193 is not listed in
            // the selected Counter Magic bypass table.
            HORROR_OF_DEATH, new GuardBrakeSpell(
                    HORROR_OF_DEATH, 24, 0, 25, 0, 19, 6_588,
                    0, 50, 1_000, 0, 60, 3, 0, 26,
                    ATTRIBUTE_UNSPECIFIED, 3, 1,
                    55, 45, 35, 0.35D, true, false, 0.05D, 0,
                    0, 0, 0, 280, false, "horror-of-death")
    );

    private static final Map<Integer, ResistFearSpell> RESIST_FEAR_SPELLS = Map.of(
            // Exact selected/secondary row188 and selected LiTo zero-based
            // case187: the post-175 generic ccc producer emits selector-only
            // 14 17 03. The selected PE cast-clock word is 1,200ms. Primary
            // metadata costs HP30, reuses after 1,000ms, lasts 60 seconds and
            // broadcasts action19/gfx6586. The selected negative-control row
            // is 45/38/30 + INT*0.40 + magic-hit - MR*0.05, with no magic
            // level; boss modifier zero means immunity. Retained target-based
            // RESIST_FEAR adds five dodge-down points to a PC target. The
            // primary magic-screen exception table makes Counter Magic bypass.
            RESIST_FEAR, new ResistFearSpell(
                    RESIST_FEAR, 23, 3, 24, 3, 19, 6_586,
                    0, 30, 1_000, 1_200, 60, 3, 0, 35,
                    ATTRIBUTE_UNSPECIFIED, 3, 8,
                    45, 38, 30, 0.40D, true, false, 0.05D, 0,
                    5, 0, 0, 280, true, "resist-fear")
    );

    private static final Map<Integer, ExoticVitalizeSpell>
            EXOTIC_VITALIZE_SPELLS = Map.of(
            // Exact selected/secondary row169 and LiTo zero-based case168:
            // selector-only ccc emits 14 15 00. The selected PE timing word
            // at 0x6358C4 and primary reuse are both 100ms; secondary reuse
            // is 0. The earth-only acquisition rule is not a runtime attr
            // gate because the exact row attr is 0. Retained L1PcInstance
            // permits natural HP/MP regeneration at weight stages 120..196.
            EXOTIC_VITALIZE, new ExoticVitalizeSpell(
                    EXOTIC_VITALIZE, 21, 0, 22, 0, 19, 4_400,
                    30, 100, 0, 100, 960, 0, 0, 0,
                    ATTRIBUTE_UNSPECIFIED, 2, 1,
                    InventoryCatalog.SPIRIT_CRYSTAL, 1,
                    0, 0, 0, true, "exotic-vitalize")
    );

    private static final Map<Integer, AdditionalFireSpell>
            ADDITIONAL_FIRE_SPELLS = Map.of(
            // Exact selected/secondary row176 and LiTo zero-based case175:
            // selector-only ccc emits 14 15 07. The selected PE timing word
            // at 0x6358D2 and primary reuse are both 100ms; secondary reuse
            // is 0. Fire-only acquisition is therefore represented by the
            // learned-skill source and is not repeated as a cast-time gate.
            // Retained L1PcInstance gives this independent transient lease
            // the same weight-stage 120..196 natural HP/MP regen bypass as
            // Exotic Vitalize. No retained damage or recovery bonus exists.
            ADDITIONAL_FIRE, new AdditionalFireSpell(
                    ADDITIONAL_FIRE, 21, 7, 22, 7, 19, 5_825,
                    30, 100, 0, 100, 960, 0, 0, 0,
                    ATTRIBUTE_UNSPECIFIED, 2, 128,
                    InventoryCatalog.SPIRIT_CRYSTAL, 1,
                    0, 0, 0, true, "additional-fire")
    );

    private static final Map<Integer, WaterLifeSpell>
            WATER_LIFE_SPELLS = Map.of(
            // Exact selected row170 and LiTo zero-based case169:
            // loc_4CB71D/sub_4C9880 selects an object and sub_4C9430 emits
            // cccd row21/column1 plus the target object id. The selected PE
            // cast-clock word at VA 0x6358C6 is 400ms. Primary/secondary DB
            // reuse values are 100/0ms and both retained target_to values
            // are PC1, admitting one living PC on the native screen. MP1,
            // Spirit Crystal40319x1,
            // duration64, action19/gfx4401, water attr4, native id2 and
            // failure280 are exact. The retained Counter Magic exception
            // list contains skill170. Its transient one-shot lease doubles
            // the next heal before Pollute Water and is then consumed.
            WATER_LIFE, new WaterLifeSpell(
                    WATER_LIFE, 21, 1, 22, 1, 19, 4_401,
                    1, 100, 0, 400, 64, -1, 0, 1,
                    ATTRIBUTE_WATER, 2, 2,
                    InventoryCatalog.SPIRIT_CRYSTAL, 1,
                    0, 0, 280, true, "water-life")
    );

    private static final Map<Integer, AquaProtectSpell>
            AQUA_PROTECT_SPELLS = Map.of(
            // Exact selected/secondary row160 and LiTo zero-based case159:
            // object-only cccd emits 14 13 07 plus target object id. The
            // retained target_to PC bit1 admits the caster unconditionally
            // and another living PC.  Both retained SQL authorities pin
            // target_to=1 (TARGET_TO_PC), not the generic clan bit 4.
            // MP30, duration960, action19 and gfx5829 are exact. The retained
            // ranged attack table pins ER+5. This lease is absent from
            // CharBuffTable, so it is intentionally transient.
            AQUA_PROTECTER, new AquaProtectSpell(
                    AQUA_PROTECTER, 19, 7, 20, 7, 19, 5_829,
                    30, 0, 0, 960, -1, 0, 1,
                    ATTRIBUTE_UNSPECIFIED, 2, 128,
                    BattleAquaProtectStatus.NATIVE_ER_BONUS,
                    true, "aqua-protecter")
    );

    private static final Map<Integer, AreaSilenceSpell>
            AREA_SILENCE_SPELLS = Map.of(
            // Exact selected/secondary row161 and LiTo zero-based case160:
            // selector-only ccc emits 14 14 00. The caster-centred native
            // screen includes PC+NPC targets, consumes MP40 and eight Spirit
            // Crystals, reuses after 9000 ms, lasts 16 seconds and uses
            // action19/gfx2241. Selected negative-control row161 overrides
            // the legacy d30 formula with 55/45/35 + INT*0.3 + MagicHit
            // - MR*0.05. Boss modifier0 is immune and Counter Magic is bypassed.
            AREA_OF_SILENCE, new AreaSilenceSpell(
                    AREA_OF_SILENCE, 20, 0, 21, 0, 19, 2_241,
                    40, 9_000, 16, 0, -1, 3, 1,
                    ATTRIBUTE_UNSPECIFIED, 1, 1,
                    55, 45, 35, 0.3D, 0.05D,
                    true, 0, InventoryCatalog.SPIRIT_CRYSTAL, 8,
                    "area-of-silence")
    );

    private static final Map<Integer, AbilityBuffSpell> ABILITY_BUFF_SPELLS = Map.of(
            // Exact rows 26/42: same-clan object target, action 19, MP 50,
            // 1,200 seconds. Same-family IndependentSkillIcon sends type 5
            // while active and type 1 when the +5 modifier ends.
            PHYSICAL_ENCHANT_DEX, new AbilityBuffSpell(PHYSICAL_ENCHANT_DEX,
                    3, 1, 19, 750, 50, 10, 1_200,
                    Ability.DEXTERITY, 5, 5, 1, 0, 0,
                    RequestShape.OBJECT_TARGET, "physical-enchant-dexterity"),
            PHYSICAL_ENCHANT_STR, new AbilityBuffSpell(PHYSICAL_ENCHANT_STR,
                    5, 1, 19, 6342, 50, 10, 1_200,
                    Ability.STRENGTH, 5, 5, 1, 0, 0,
                    RequestShape.OBJECT_TARGET, "physical-enchant-strength"),
            // Exact selected row 109 and LiTo.bin zero-based case 108:
            // selector-only ccc, self, Dark Elf level 14/4, MP 10, reuse 0,
            // duration 960, action 19, gfx 3909, STR +2 and native type 2.
            // The retained original-mode family replaces skill 42 and uses
            // messages 292/293 for application/removal.
            DRESS_MIGHTY, new AbilityBuffSpell(DRESS_MIGHTY,
                    13, 4, 19, 3909, 10, 0, 960,
                    Ability.STRENGTH, 2, 2, 2, 292, 293,
                    RequestShape.SELECTOR_ONLY, "dress-mighty"),
            // Exact selected row 110 and LiTo.bin zero-based case 109:
            // selector-only ccc, self, Dark Elf level 14/5, MP 10, reuse 0,
            // duration 960, action 19, gfx 3910, DEX +2 and native type 2.
            // The retained original-mode family replaces skill 26 and uses
            // messages 294/295 for application/removal.
            DRESS_DEXTERITY, new AbilityBuffSpell(DRESS_DEXTERITY,
                    13, 5, 19, 3910, 10, 0, 960,
                    Ability.DEXTERITY, 2, 2, 2, 294, 295,
                    RequestShape.SELECTOR_ONLY, "dress-dexterity")
    );

    private static final Map<Integer, RangedEvasionBuffSpell>
            RANGED_EVASION_BUFF_SPELLS = Map.of(
            // Exact selected row 111 and LiTo.bin zero-based case 110:
            // selector-only ccc row13/column6, Dark Elf self target,
            // action19/gfx11766, MP20, reuse0 and duration32. Both retained
            // L1PcInstance.getEr implementations add exactly 12 while this
            // cancelable lease is active. The result participates only in the
            // independent bow ER roll; it is not Uncanny Dodge's physical d20.
            DRESS_EVASION, new RangedEvasionBuffSpell(
                    DRESS_EVASION, 13, 6, 19, 11766,
                    20, 0, 32, ATTRIBUTE_UNSPECIFIED, 2,
                    64, 12, "dress-evasion")
    );

    private static final Map<Integer, HasteSpell> HASTE_SPELLS = Map.of(
            // Exact selected 3.80 row 43 and LiTo.bin zero-based case 42:
            // row 5/column 2, cccd object-only request, native-screen target,
            // action 19/gfx 755, MP 40, 10 ms reuse and 1,200 seconds.
            // Same-family S_SkillHaste is the selected client's S_SPEED dch:
            // mode 1 starts Haste, while mode 0 restores normal movement.
            HASTE, new HasteSpell(HASTE, 5, 2, 19, 755,
                    40, 10, 1_200, -1, 184, 185, 280, "haste")
    );

    private static final Map<Integer, SelfHasteSpell> SELF_HASTE_SPELLS = Map.of(
            // Exact selected row 54 and LiTo.bin zero-based case 53:
            // selector-only row 6/column 5, self target, action19/gfx3104,
            // MP60, 10ms reuse and 2400 seconds. Wind Helm 20023 alone
            // halves this skill's cost; it must not affect ordinary Haste43.
            GREATER_HASTE, new SelfHasteSpell(GREATER_HASTE, 6, 5, 19, 3104,
                    60, 10, 2_400, 184, 185, "greater-haste")
    );

    private static final Map<Integer, CombatBuffSpell> COMBAT_BUFF_SPELLS = Map.of(
            // Exact selected row 55 and LiTo.bin zero-based case 54:
            // object-only row6/column6, clan-or-party PC target, action19/
            // gfx3943, MP40, selected reuse10ms and duration320. Retained
            // skills_buff_ability owns AC+10 (worse), melee HIT+2/DMG+5.
            BERSERKERS, new CombatBuffSpell(BERSERKERS, 6, 6, 19, 3943,
                    40, 10, 320, -1, 12, 10, 2, 5, 280, "berserkers")
    );

    private static final Map<Integer, DamageProtectionSpell> DAMAGE_PROTECTION_SPELLS = Map.of(
            // Exact selected row 68 and LiTo.bin zero-based case 67:
            // object-only row8/column3, same-clan PC target (self included),
            // action19/gfx228, MP30, 1,500ms reuse, 32 seconds and two Magic
            // Gems. ConfigSkill fixes both physical and magic damage divisor
            // at 2.0; positive integer damage therefore truncates after /2.
            IMMUNE_TO_HARM, new DamageProtectionSpell(IMMUNE_TO_HARM,
                    8, 3, 19, 228, 30, 1_500, 32, -1, 4, 2,
                    314, 315, 280, InventoryCatalog.MAGIC_GEM, 2,
                    "immune-to-harm")
    );

    private static final Map<Integer, MassTeleportSpell> MASS_TELEPORT_SPELLS = Map.of(
            // Exact selected row 69 and LiTo.bin zero-based case 68: row8/
            // column4, ccchhh request carrying map/x/y, action19/gfx2236,
            // MP60, selected reuse10ms and one Magic Gem. target_to=4 asks
            // nearby same-clan players before each accepted portal handshake.
            MASS_TELEPORT, new MassTeleportSpell(MASS_TELEPORT,
                    8, 4, 19, 2236, 60, 10,
                    InventoryCatalog.MAGIC_GEM, 1, 4,
                    BattleMassTeleport.RANDOM_RANGE, BattleMassTeleport.INVITE_RANGE,
                    748, 276, "mass-teleport")
    );

    private static final Map<Integer, MovementBuffSpell> MOVEMENT_BUFF_SPELLS = Map.of(
            // Exact selected row 52 and LiTo.bin zero-based case 51:
            // selector-only row 6/column 3, action 19/gfx 3936, MP 15,
            // 10 ms reuse and 32 seconds.  The retained S_SkillBrave path
            // uses mode 4 locally (duration 32) and mode 4/duration 0 for
            // observers.  It is a wind/type-2 movement-only accelerator:
            // MOVE intervals use 0.665 while ATTACK/SPELL do not.
            HOLY_WALK, new MovementBuffSpell(HOLY_WALK, 6, 3, 19, 3936,
                    15, 10, 32, ATTRIBUTE_WIND, 2,
                    BattleTimedStatus.BRAVE_HOLY_WALK, 0.665D, "holy-walk"),
            // Exact selected row 101 and LiTo.bin zero-based case 100:
            // selector-only row12/column4, action19/gfx2945, MP10,
            // reuse0 and 960 seconds. The retained Dark Elf path shares
            // mode4 and the movement-only 0.665 cadence with Holy Walk,
            // while the source DB keeps its attribute field at zero.
            MOVING_ACCELERATION, new MovementBuffSpell(
                    MOVING_ACCELERATION, 12, 4, 19, 2945,
                    10, 0, 960, ATTRIBUTE_UNSPECIFIED, 2,
                    BattleTimedStatus.BRAVE_HOLY_WALK, 0.665D,
                    "moving-acceleration"),
            // Exact selected/secondary row150 and LiTo.bin zero-based
            // case149: byte-table entry1 at 0x004CBD95 routes to the
            // selector-only 14 12 05 producer. Both retained DB rows use
            // attr0, MP15, duration960, action19/gfx2247 and native id32.
            // The retained always-repeated brave family replaces Holy Walk,
            // Moving Acceleration, potions/wafers and other brave leases;
            // S_SkillBrave mode4 accelerates only MOVE intervals by 0.665.
            WIND_WALK, new MovementBuffSpell(
                    WIND_WALK, 18, 5, 19, 2247,
                    15, 0, 960, ATTRIBUTE_UNSPECIFIED, 2,
                    BattleTimedStatus.BRAVE_HOLY_WALK, 0.665D,
                    "wind-walk")
    );

    private static final Map<Integer, PhysicalProcBuffSpell> PHYSICAL_PROC_BUFF_SPELLS = Map.of(
            // Exact selected row 102 and LiTo.bin zero-based case 101:
            // selector-only row12/column5, action19/gfx2946, MP20,
            // selected reuse2990ms and duration320. Retained L1AttackPc
            // excludes bow/gauntlet/Kiringku attacks, rolls 1..100 <= 33,
            // and contributes multiplier1.5 to the maximum-multiplier family.
            BURNING_SPIRIT, new PhysicalProcBuffSpell(
                    BURNING_SPIRIT, 12, 5, 19, 2946,
                    20, 2_990, 320, ATTRIBUTE_UNSPECIFIED, 2,
                    BattlePhysicalProcBuff.BURNING_SPIRIT_CHANCE,
                    BattlePhysicalProcBuff.BURNING_SPIRIT_MULTIPLIER,
                    32, 0, 0, 0, 0, "burning-spirit"),
            // Exact selected/secondary row 105 and LiTo.bin zero-based case
            // 104: selector-only row13/column0, action19/gfx2949, MP30,
            // second-level dark stone x1, reuse0 and duration192. Retained
            // L1AttackPc admits only claw/Edoryu hits, rolls 1..100 <= 15,
            // and contributes multiplier1.5 to the maximum-multiplier family.
            DOUBLE_BRAKE, new PhysicalProcBuffSpell(
                    DOUBLE_BRAKE, 13, 0, 19, 2949,
                    30, 0, 192, ATTRIBUTE_UNSPECIFIED, 2,
                    BattlePhysicalProcBuff.DOUBLE_BRAKE_CHANCE,
                    BattlePhysicalProcBuff.DOUBLE_BRAKE_MULTIPLIER,
                    1, 823, 829, InventoryCatalog.SECOND_LEVEL_DARK_STONE, 1,
                    "double-brake"),
            // Exact selected/secondary row171 and LiTo zero-based case170:
            // selector-only ccc emits 14 15 02; selected PE clock300ms.
            // Primary/secondary reuse values are100/0ms. Fire attr2, MP20,
            // Spirit Crystal40319x1, duration192, action19/gfx4402 and native
            // id4 are exact. Retained L1AttackPc shares Burning Spirit's
            // one-based roll, excludes bow/gauntlet/Kiringku attacks and adds
            // multiplier1.5 to the maximum-multiplier family at roll<=33.
            ELEMENTAL_FIRE, new PhysicalProcBuffSpell(
                    ELEMENTAL_FIRE, 21, 2, 19, 4_402,
                    20, 100, 192, ATTRIBUTE_FIRE, 2,
                    BattlePhysicalProcBuff.ELEMENTAL_FIRE_CHANCE,
                    BattlePhysicalProcBuff.ELEMENTAL_FIRE_MULTIPLIER,
                    4, 0, 0, InventoryCatalog.SPIRIT_CRYSTAL, 1,
                    "elemental-fire"),
            // Exact selected row175 and LiTo zero-based case174: selector-only
            // 14 15 06, client PE clock5000ms. Primary/secondary reuse is
            // 300/0ms. Fire attr2, MP30, Spirit Crystal40319x3, duration64,
            // action19/gfx5833 and native id64 are exact. Retained L1AttackPc
            // forces every armed weapon die to its maximum, contributes an
            // unconditional1.5 candidate to the maximum-multiplier family,
            // and retained hit commit suppresses weapon durability damage.
            SOUL_OF_FLAME, new PhysicalProcBuffSpell(
                    SOUL_OF_FLAME, 21, 6, 19, 5_833,
                    30, 300, 64, ATTRIBUTE_FIRE, 2,
                    0, BattlePhysicalProcBuff.SOUL_OF_FLAME_MULTIPLIER,
                    64, 0, 0, InventoryCatalog.SPIRIT_CRYSTAL, 3,
                    "soul-of-flame")
    );

    private static final Map<Integer, OtherNoEffectSpell> OTHER_NO_EFFECT_SPELLS = Map.of(
            // Exact selected/secondary row172 and LiTo zero-based case171:
            // the dispatcher default emits selector-only 14 15 03 and the
            // selected PE clock is zero.  Both retained rows pin wind attr8,
            // MP10, TYPE_OTHER128, native id8, duration/reuse/gfx/messages0.
            // The retained primary executor has no STORM_WALK SkillMode or
            // individual branch; its generic duration-zero and gfx-zero
            // paths therefore consume admitted MP but install no lease and
            // emit no action/effect.  This is deliberately not Wind Walk.
            STORM_WALK, new OtherNoEffectSpell(
                    STORM_WALK, 21, 3, 19, 0,
                    10, 0, 0, ATTRIBUTE_WIND, 128, 8,
                    0, 0, 0, 0, 0, "storm-walk")
    );

    private static final Map<Integer, PoisonResistanceSpell> POISON_RESISTANCE_SPELLS = Map.of(
            // Exact selected/secondary row 104 and LiTo.bin zero-based case
            // 103: selector-only row12/column7, action19/gfx2948, MP20,
            // reuse0 and duration320. Retained L1Poison.isValidTarget blocks
            // new damage, silence and paralysis poison without curing an
            // already installed poison. Stop/expiry message is 831.
            VENOM_RESIST, new PoisonResistanceSpell(
                    VENOM_RESIST, 12, 7, 19, 2948,
                    20, 0, 320, ATTRIBUTE_UNSPECIFIED, 2,
                    128, 0, 831, 0, "venom-resist")
    );

    private static final Map<Integer, DodgeBuffSpell> DODGE_BUFF_SPELLS = Map.of(
            // Exact selected/secondary row 106 and LiTo.bin zero-based case
            // 105: selector-only row13/column1, action19/gfx11765, MP20,
            // reuse0 and duration192.  The retained UNCANNY_DODGE mode adds
            // five dodge points only when the lease is absent, so an active
            // recast spends resources and plays the cast without refreshing
            // the original expiry.  Same-family attack code subtracts the
            // five points directly from PC and NPC physical attacker dice.
            UNCANNY_DODGE, new DodgeBuffSpell(
                    UNCANNY_DODGE, 13, 1, 19, 11765,
                    20, 0, 0, 192, ATTRIBUTE_UNSPECIFIED, 2,
                    2, 5, "uncanny-dodge"),
            // Skills196..200 are selected-database none rows. Skill201 is the
            // first subsequent real producer: exact selector-only 14 19 00,
            // client clock1800ms, selected MP10/reuse10ms/duration1200,
            // action19/gfx6813/native-id1. Retained MIRROR_IMAGE installs an
            // independent dodge+5 lease only when absent, so it stacks with
            // Uncanny Dodge while an active recast pays MP without refreshing.
            MIRROR_IMAGE, new DodgeBuffSpell(
                    MIRROR_IMAGE, 25, 0, 19, 6813,
                    10, 10, 1_800, 1_200, ATTRIBUTE_UNSPECIFIED, 2,
                    1, 5, "mirror-image")
    );

    private static final Map<Integer, ItemWeaponEnchantSpell>
            ITEM_WEAPON_ENCHANT_SPELLS = Map.ofEntries(
            // Exact selected row 12 and LiTo.bin zero-based case 11. The
            // client opens its inventory selector through sub_4C9DC0 and the
            // loc_4C9D76 callback emits cccd, row1/column3 plus the selected
            // inventory object id.  The selected row specifies MP20,
            // reuse10ms, duration1800, fire attribute, action19/gfx747 and no
            // material.  The older item-local implementation applies
            // getDmgByMagic()+2 to that exact weapon, sends messages 161/79,
            // expires with 308 and replaces the same object's former weapon
            // enchant lease.
            Map.entry(ENCHANT_WEAPON, new ItemWeaponEnchantSpell(
                    ENCHANT_WEAPON, 1, 3, 19, 747,
                    20, 10, 1_800, ATTRIBUTE_FIRE, 2,
                    8, 2, 2, 79, 161, 308,
                    0, 0, "enchant-weapon")),
            // Exact selected/secondary row 107 and LiTo.bin zero-based case
            // 106: the inventory callback emits cccd, row13/column2 plus the
            // selected inventory object id.  Both retained databases specify
            // action19/gfx2951, MP30, second-level dark stone x1, reuse0 and
            // duration192.  The original item-local implementation accepts
            // only a weapon, applies getDmgByMagic()+5 to that exact object,
            // and is listed as Cancellation-immune.
            Map.entry(SHADOW_FANG, new ItemWeaponEnchantSpell(
                    SHADOW_FANG, 13, 2, 19, 2951,
                    30, 0, 192, ATTRIBUTE_UNSPECIFIED, 2,
                    4, 5, 5, 79, 0, 308,
                    InventoryCatalog.SECOND_LEVEL_DARK_STONE, 1,
                    "shadow-fang"))
    );

    private static final Map<Integer, ItemArmorEnchantSpell>
            ITEM_ARMOR_ENCHANT_SPELLS = Map.of(
            // Exact selected row 21 and LiTo.bin zero-based case 20. The
            // client opens the shared inventory selector through sub_4C9DC0;
            // loc_4C9D76 emits cccd, row2/column4 plus the selected inventory
            // object id. Retained L1SkillUse accepts only armor type2/type2
            // (body armor), then L1ItemInstance binds AC-3 to that exact item.
            // The modifier only affects AC while the item is equipped, is
            // Cancellation-immune, refreshes on the same object and expires
            // with message308.
            BLESSED_ARMOR, new ItemArmorEnchantSpell(
                    BLESSED_ARMOR, 2, 4, 19, 748,
                    20, 10, 1_800, ATTRIBUTE_UNSPECIFIED, 2,
                    16, -3, 79, 161, 308, "blessed-armor")
    );

    private static final Map<Integer, WeaponBuffSpell> WEAPON_BUFF_SPELLS = Map.of(
            // Exact selected row 8 and LiTo.bin zero-based case 7: row 0/
            // column 7, cccd object-only request, native-screen PC/NPC target,
            // action 19, gfx 2165, MP 10, 10 ms reuse and 1,200 seconds.
            // skills_buff_ability applies melee HIT/DMG +1 only; unlike
            // Bless Weapon there is no BOW_HIT/BOW_DMG ability row.
            HOLY_WEAPON, new WeaponBuffSpell(HOLY_WEAPON,
                    0, 7, 19, 2165, 10, 10, 1_200, -1,
                    1, 1, 0, 0, 79, 161, "holy-weapon"),
            // Exact selected row 48 and LiTo.bin zero-based case 47: row 5/
            // column 7, cccd object-only request, native-screen PC/NPC target,
            // action 19, gfx 2176, MP 20, 10 ms reuse and 1,200 seconds.
            // skills_buff_ability applies melee HIT/DMG +2 and bow HIT +2;
            // the retained run path requires a PC target to have a weapon.
            BLESS_WEAPON, new WeaponBuffSpell(BLESS_WEAPON,
                    5, 7, 19, 2176, 20, 10, 1_200, -1,
                    2, 2, 2, 0, 79, 161, "bless-weapon")
    );

    private static final Map<Integer, CancellationSpell> CANCELLATION_SPELLS = Map.of(
            // Exact selected 3.80 row 44 and LiTo.bin zero-based case 43:
            // row 5/column 3, cccd object-only request, PC/NPC range 3,
            // action 19/gfx 870, MP 40, 500 ms reuse and two Magic Gems.
            CANCELLATION, new CancellationSpell(CANCELLATION, 5, 3, 19, 870,
                    40, 500, 3, 280, InventoryCatalog.MAGIC_GEM, 2,
                    "cancellation")
    );

    private static final Map<Integer, RegenSpell> REGEN_SPELLS = Map.of(
            // Exact 380_db.sql row 32: selector-only self spell, action 19,
            // gfx 2173, MP 5, HP 30, 10 ms reuse and 640 seconds. The
            // same-family PcRegenExecutor contributes +5 MPR while active;
            // accepted movement, attack, teleport or another spell ends it.
            MEDITATION, new RegenSpell(MEDITATION, 3, 7, 19, 2173,
                    5, 30, 10, 10, 640, RequestShape.SELECTOR_ONLY,
                    Targeting.SELF_SELECTOR, 0,
                    RegenEffect.MEDITATION, 5, true, "meditation"),
            // Exact 380_db.sql row 206: PC semantic target, action 19,
            // gfx 6527, MP 20, 1,000 ms reuse, 600 seconds and range 3.
            // Selected LiTo.bin index205 is above the 176-case jump table and
            // therefore emits only ccc (14 19 05); its local cast clock is
            // 1,200 ms. PcRegenExecutor contributes +2 MPR and activity does
            // not end the effect.
            CONCENTRATION, new RegenSpell(CONCENTRATION, 25, 5, 19, 6527,
                    20, 0, 1_000, 1_200, 600, RequestShape.SELECTOR_ONLY,
                    Targeting.PLAYER_OBJECT, 3,
                    RegenEffect.CONCENTRATION, 2, false, "concentration")
    );

    private static final Map<Integer, WeightSpell> WEIGHT_SPELLS = Map.of(
            // Exact 380_db.sql row 14: selector-only self spell, MP 10,
            // HP 50, 1,800 seconds, action 19 and gfx 2170. Same-family
            // getMaxWeight adds 180 before the configured server rate.
            DECREASE_WEIGHT, new WeightSpell(DECREASE_WEIGHT, 1, 5, 19, 2170,
                    10, 50, 10, 1_800, 0, 0,
                    WeightEffect.DECREASE_WEIGHT, "decrease-weight")
    );

    private static final Map<Integer, GroundSpell> GROUND_SPELLS = Map.of(
            FIRE_WALL, new GroundSpell(FIRE_WALL, 7, 1, 18, 168, 8,
                    60, 10, 10, 1_650, 1, 8, 0, 0, "fire-wall"),
            LIFE_STREAM, new GroundSpell(LIFE_STREAM, 7, 6, 19, 2231, -1,
                    20, 10, 320, 0, 3, 1, 40318, 1, "life-stream")
    );

    private static final Map<Integer, AttackSpell> ATTACK_SPELLS = Map.ofEntries(
            Map.entry(LIGHT_ARROW, new AttackSpell(LIGHT_ARROW, 0, 3, 18, 167,
                    3, 10, 10, 6, 1, 10, 16, "light-arrow")),
            // Exact rows 6/7 and the shared native case-5/case-6 producer:
            // cccdhh object/coordinates, action 18, range 10, 1d10+8 (formal SQL and 1-server skills rows).
            Map.entry(ICE_ARROW, new AttackSpell(ICE_ARROW, 0, 5, 18, 1797,
                    4, 10, 10, 8, 1, 10, 4, "ice-arrow")),
            Map.entry(WIND_CUTTER, new AttackSpell(WIND_CUTTER, 0, 6, 18, 1799,
                    4, 10, 10, 8, 1, 10, 8, "wind-cutter")),
            // Exact selected row10 and LiTo.bin zero-based case9:
            // loc_4CB8B4 fixes native acquisition radius2, then sub_4CA6E0
            // emits cccdhh row1/column1 plus target object and X/Y.  The
            // authoritative DB range remains three.  Retained L1SkillUse
            // heals the caster by the final positive magic damage only after
            // the target damage commit is accepted.
            Map.entry(CHILL_TOUCH, new AttackSpell(CHILL_TOUCH, 1, 1, 18, 252,
                    8, 10, 3, 8, 2, 10, ATTRIBUTE_WATER, "chill-touch")),
            // Exact selected row28 and LiTo.bin zero-based case27:
            // loc_4CB938 fixes native acquisition radius3, then sub_4CA6E0
            // emits cccdhh row3/column3 plus target object and X/Y.  The
            // selected DB is authoritative for MP12, reuse50ms, range4,
            // 12+3d10 water damage and action18/gfx236; the recovered client
            // cast clock is100ms.  Retained L1SkillUse applies the same
            // accepted-damage life drain used by Chill Touch.
            Map.entry(VAMPIRIC_TOUCH, new AttackSpell(VAMPIRIC_TOUCH, 3, 3, 18, 236,
                    12, 50, 4, 12, 3, 10, ATTRIBUTE_WATER, "vampiric-touch")),
            // Exact selected 3.80 rows 15/16.  Their attr values are kept as
            // source data (1/2) even though the localized names suggest the
            // opposite earth/fire pairing; do not silently rewrite the DB.
            Map.entry(FIRE_ARROW, new AttackSpell(FIRE_ARROW, 1, 6, 18, 1583,
                    6, 10, 10, 8, 2, 10, 1, "fire-arrow")),
            Map.entry(STALAC, new AttackSpell(STALAC, 1, 7, 18, 1801,
                    6, 10, 10, 8, 2, 10, 2, "stalac")),
            // Exact selected row 34 and LiTo.bin zero-based case 33:
            // row4/column1, cccdhh object/coordinates, MP18, range6,
            // action18/gfx11736 and 32+5d8 wind damage.  The native client
            // selection producer allows ten tiles and the recovered PE cast
            // clock is10ms; the server keeps the authoritative selected-DB
            // range of six and reuse of10ms (not yiwei's 1000ms row).
            Map.entry(CALL_LIGHTNING, new AttackSpell(CALL_LIGHTNING, 4, 1, 18, 11736,
                    18, 10, 6, 32, 5, 8, ATTRIBUTE_WIND, "call-lightning")),
            // Exact selected row 38 and LiTo.bin zero-based case 37 share
            // loc_4CB755/sub_4CA6E0: row4/column5, cccdhh object/coordinates,
            // MP18, range6, action18/gfx11738 and 32+5d6 water damage.  The
            // client selector accepts ten tiles; the database range remains
            // the authoritative combat admission boundary.
            Map.entry(CONE_OF_COLD, new AttackSpell(CONE_OF_COLD, 4, 5, 18, 11738,
                    18, 10, 6, 32, 5, 6, ATTRIBUTE_WATER, "cone-of-cold")),
            // Exact selected row 45 and LiTo.bin zero-based case 44 share
            // loc_4CB755/sub_4CA6E0: row5/column4, cccdhh object/coordinates,
            // MP20, 100 ms reuse, action18/gfx129 and 40+5d3 earth damage.
            // The client selector accepts ten tiles; the database range of
            // eight is the authoritative combat admission boundary.
            Map.entry(ERUPTION, new AttackSpell(ERUPTION, 5, 4, 18, 129,
                    20, 100, 8, 40, 5, 3, ATTRIBUTE_EARTH, "eruption")),
            // Exact selected row 46 and LiTo.bin zero-based case 45 share
            // loc_4CB96F/sub_4CA6E0: row5/column5, cccdhh object/coordinates,
            // MP20, 500 ms reuse, action18/gfx1811 and 32+16d5 fire damage.
            // The client selector accepts four tiles; the database range of
            // three remains the authoritative combat admission boundary.
            Map.entry(SUNBURST, new AttackSpell(SUNBURST, 5, 5, 18, 1811,
                    20, 500, 3, 32, 16, 5, ATTRIBUTE_FIRE, "sunburst")),
            // Exact selected row 77 and LiTo.bin zero-based case 76 share
            // loc_4CB755/sub_4CA6E0: row9/column4, cccdhh target plus
            // coordinates, action18, native/database range10, MP70, 5000 ms
            // reuse and neutral 190+8d15 damage. Retained cast admission
            // requires lawful >=500. The selected retained skill control
            // gives this spell an independent 40% x2 critical using gfx11748;
            // ordinary casts use the database gfx11746.
            Map.entry(DISINTEGRATE, new AttackSpell(DISINTEGRATE, 9, 4, 18, 11746,
                    70, 5_000, 10, 190, 8, 15, ATTRIBUTE_NONE,
                    500, 40, 11748, "disintegrate"))
    );

    private static final Map<Integer, AttackSpell> FINAL_BURN_ATTACK_SPELLS = Map.of(
            // Exact selected/secondary row108 and native zero-based case107:
            // loc_4CB92E fixes range1 and enters sub_4CA6E0, whose cccdhh
            // producer writes row13/column3 plus target object and X/Y.
            // The retained original uses the caster's MP snapshot as neutral
            // source damage, then commits the exceptional HP=1/MP=0 state.
            FINAL_BURN, new AttackSpell(FINAL_BURN, 13, 3, 18, 2952,
                    1, 0, 1, 0, 0, 0, ATTRIBUTE_NONE,
                    Short.MIN_VALUE, 0, 0, true, "final-burn")
    );

    private static final Map<Integer, ThunderGrabSpell> THUNDER_GRAB_SPELLS = Map.of(
            // Exact selected 3.80 row 192 and native zero-based case 191.
            // LiTo.bin takes the post-175 default producer and emits only
            // ccc = 14 17 07; the server restores the last admitted combat
            // target instead of reading the retained cccdhh tail. The source
            // skill consumes HP45/MP0, deals 80+2d10 wind magic and rolls the
            // selected 50/30/15 level tiers minus Sustain resistance. Bosses
            // are bind-immune and Counter Magic explicitly does not consume.
            THUNDER_GRAB, new ThunderGrabSpell(
                    new AttackSpell(THUNDER_GRAB, 23, 7, 18, 6512,
                            0, 4_000, 8, 80, 2, 10,
                            ATTRIBUTE_WIND, "thunder-grab"),
                    45, 1_800, 2, 4, 50, 30, 15,
                    90, true, 49_113L, "$5711", "thunder-grab")
    );

    private static final Map<Integer, ConfusionSpell> CONFUSION_SPELLS = Map.of(
            // Exact selected row202 and LiTo.bin zero-based case201. The
            // post-175 producer emits selector-only 14 19 01 and the selected
            // PE cast clock is 0ms. The primary probability table overrides
            // the unreachable legacy 20..29 branch with a fixed 25%; its boss
            // modifier0 makes bosses immune. TYPE_CURSE does not calculate
            // database damage_value11, and the selected/381n8 CONFUSION mode
            // returns zero while installing a non-refreshing skill64 Silence
            // lease for eight seconds. Counter Magic applies only after the
            // probability succeeds.
            CONFUSION, new ConfusionSpell(
                    new AttackSpell(CONFUSION, 25, 1, 18, 6_525,
                            15, 3_000, 3, 0, 0, 0,
                            ATTRIBUTE_NONE, "confusion"),
                    10, 0, 8, 25, 0, false, 1_339,
                    11, 49_118L, "$5682", "$5936", "confusion")
    );

    private static final Map<Integer, SmashSpell> SMASH_SPELLS = Map.of(
            // Exact selected row203 and LiTo.bin zero-based case202. The
            // post-175 producer emits selector-only 14 19 02 and the selected
            // PE cast clock is 0ms.  The primary row is a neutral TYPE_ATTACK
            // against PC/NPC at range1: MP7, reuse600ms and 50+2d10 damage.
            // The historical book/item and skills_item labels still say
            // "暴擊" while the selected skills row names it "粉碎能量".
            SMASH, new SmashSpell(
                    new AttackSpell(SMASH, 25, 2, 18, 6_526,
                            7, 600, 1, 50, 2, 10,
                            ATTRIBUTE_UNSPECIFIED, "smash-energy"),
                    0, 3, 64, 4, 49_119L,
                    "$5683", "$5937", "暴擊", "smash-energy")
    );

    private static final Map<Integer, MindBreakSpell> MIND_BREAK_SPELLS = Map.of(
            // Exact selected row207 and LiTo.bin zero-based case206. The
            // post-175 generic producer emits selector-only 14 19 06 and its
            // PE cast clock is 0ms. A PC caster pays HP15/MP20, then an
            // accepted PC/NPC target must have strictly more than a random
            // 5..50 MP debit. Only that exact debit enables the neutral base5
            // magic calculation; the retained MIND_BREAK mode multiplies the
            // resolved damage by four. Counter Magic runs before target MP is
            // touched, while Counter Mirror is evaluated by calcMagicDamage
            // before the mode's x4 multiplier.
            MIND_BREAK, new MindBreakSpell(
                    new AttackSpell(MIND_BREAK, 25, 6, 18, 6_553,
                            20, 3_000, 3, 5, 0, 0,
                            ATTRIBUTE_UNSPECIFIED, "mind-break"),
                    15, 0, 3, 64, 64,
                    5, 50, 4, 49_123L,
                    "$5687", "$5941", false, "mind-break")
    );

    private static final Map<Integer, BoneBreakSpell> BONE_BREAK_SPELLS = Map.of(
            // Exact selected row208 and LiTo.bin zero-based case207. The
            // post-175 generic producer emits selector-only 14 19 07 and its
            // PE cast clock is 0ms. The retained TYPE_ATTACK mode ignores the
            // probability/boss-control tables: every admitted PC cast pays
            // MP20, adds lawful45 and calculates neutral base10 magic damage.
            // Only when skill208 is absent does its private 1..1000 roll run;
            // values 801..1000 install a non-refreshing two-second stun and
            // temporary NPC86123/GFX6552. Counter Magic executes before the
            // mode, while Counter Mirror does not suppress its second damage
            // calculation or its stun roll.
            BONE_BREAK, new BoneBreakSpell(
                    new AttackSpell(BONE_BREAK, 25, 7, 18, 7_020,
                            20, 2_000, 3, 10, 0, 0,
                            ATTRIBUTE_UNSPECIFIED, "bone-break"),
                    0, 3, 64, 128, 45,
                    2, 1, 1_000, 800,
                    86_123, 6_552, 49_124L,
                    "$5688", "$5942",
                    55, 45, 35, 0.35D, 5, 0.05D,
                    10, 0, false, "bone-break")
    );

    private static final Map<Integer, IllusionOgreSpell> ILLUSION_OGRE_SPELLS = Map.of(
            // Exact selected row204 and LiTo.bin zero-based case203. The
            // post-175 default producer emits selector-only 14 19 03 and its
            // PE cast clock is 0ms. The retained PC-only skill permits self or
            // another PC through range3/LOS, consumes MP15, refreshes a
            // non-persistent 64-second lease and grants melee HIT+4/DMG+4.
            // It is not part of a repeated-skill exclusion group and bypasses
            // Counter Magic.
            ILLUSION_OGRE, new IllusionOgreSpell(
                    ILLUSION_OGRE, 25, 3, 19, 6_578,
                    15, 2_000, 0, 64, 3,
                    1, 2, 8, 4, 4,
                    49_120L, "$5684", "$5938", true,
                    "illusion-ogre")
    );

    private static final Map<Integer, IllusionLichSpell> ILLUSION_LICH_SPELLS = Map.of(
            // Exact selected row209 and LiTo.bin zero-based case208. The
            // post-175 default producer emits selector-only 14 1A 00 and its
            // PE client cast clock is 64ms. The retained PC-target buff
            // permits self or another PC through range3/LOS, consumes MP15,
            // refreshes a non-persistent 64-second lease and grants SP+2.
            // It bypasses Counter Magic and does not replace another illusion.
            ILLUSION_LICH, new IllusionLichSpell(
                    ILLUSION_LICH, 26, 0, 19, 6_577,
                    15, 2_000, 64, 64, 3,
                    1, 2, 1, 2,
                    49_125L, "$5689", "$5943", true,
                    "illusion-lich")
    );

    private static final Map<Integer, IllusionAvatarSpell> ILLUSION_AVATAR_SPELLS = Map.of(
            // Exact selected primary row219 and LiTo.bin zero-based case218.
            // The post-175 default producer emits selector-only 14 1B 02 and
            // its PE client cast clock is 0ms. Retained PC-target mode permits
            // self or another PC through range3/LOS, consumes MP35 and
            // refreshes a non-persistent 64-second lease. skills_buff_ability
            // contributes melee DMG+10 and bow DMG+10, the mode contributes
            // SP+8, and retained L1PcInstance multiplies every accepted
            // incoming damage value by 1.5 before the final int cast. It
            // bypasses Counter Magic and does not replace another illusion.
            ILLUSION_AVATAR, new IllusionAvatarSpell(
                    ILLUSION_AVATAR, 27, 2, 19, 6_575,
                    35, 5_000, 0, 64, 3,
                    1, 2, 4, 10, 10, 8, 1.5D,
                    49_135L, "$5699", "$5953", true, false,
                    "illusion-avatar")
    );

    private static final Map<Integer, CubeIgnitionSpell> CUBE_IGNITION_SPELLS = Map.of(
            // Exact selected primary row205 and LiTo.bin zero-based case204.
            // The post-175 default producer emits selector-only 14 19 04 and
            // its PE cast clock is 0ms. Retained target="none" restores the
            // caster's current tile, then spawns NPC80149/GFX6706 for 20s.
            // The 1s executor applies non-stacking 8s leases in radius3:
            // allies gain fire resistance +30/GFX6708; enemies receive
            // GFX6709 and 10 damage on scheduler ticks four and eight.
            CUBE_IGNITION, new CubeIgnitionSpell(
                    CUBE_IGNITION, 25, 4, 19,
                    6_706, 6_708, 6_709,
                    30, 5_000, 0, 20, 3,
                    32, 2, 16, 10,
                    49_156L, 1, 80_149,
                    49_121L, "$5685", "$5939",
                    "cube-ignition")
    );

    private static final Map<Integer, CubeQuakeSpell> CUBE_QUAKE_SPELLS = Map.of(
            // Exact selected primary row210 and LiTo.bin zero-based case209.
            // The post-175 default producer emits selector-only 14 1A 01 and
            // its PE cast clock is 0ms. Retained target="none" restores the
            // caster tile, then spawns NPC80150/GFX6712 for 20s. Radius3
            // applies independent 8s leases: allies gain earth resistance
            // +30/GFX6714; enemies receive GFX6715 and a one-second MOVE_STOP
            // on scheduler callbacks four and eight unless a protected status
            // (freeze, Absolute Barrier or Earth Bind family) is active.
            CUBE_QUAKE, new CubeQuakeSpell(
                    CUBE_QUAKE, 26, 1, 19,
                    6_712, 6_714, 6_715,
                    35, 5_000, 0, 20, 3,
                    32, 2, 2, 30,
                    49_156L, 1, 80_150,
                    49_126L, "$5690", "$5944",
                    "cube-quake")
    );

    private static final Map<Integer, CubeShockSpell> CUBE_SHOCK_SPELLS = Map.of(
            // Exact selected primary row215 and LiTo.bin zero-based case214.
            // The post-175 default producer emits selector-only 14 1A 06 and
            // its PE cast clock is 0ms. Retained target="none" restores the
            // caster tile, then spawns NPC80151/GFX6718 for 20s. Radius3
            // applies independent 8s leases: allies gain wind resistance
            // +30/GFX6720; enemies receive GFX6721 and a 900/1000ms rolling
            // four-second MR-reduction marker. The older retained source pins
            // the intended disabled numeric value at -10 MR.
            CUBE_SHOCK, new CubeShockSpell(
                    CUBE_SHOCK, 26, 6, 19,
                    6_718, 6_720, 6_721,
                    55, 5_000, 0, 20, 3,
                    32, 2, 64, 30, -10,
                    49_156L, 1, 80_151,
                    49_131L, "$5695", "$5949",
                    "cube-shock")
    );

    private static final Map<Integer, CubeHarmonySpell> CUBE_HARMONY_SPELLS = Map.of(
            // Exact selected primary row220 and LiTo.bin zero-based case219.
            // The post-175 default producer emits selector-only 14 1B 03 and
            // its PE cast clock is 32ms. Retained target="none" restores the
            // caster tile, then spawns NPC80152/GFX6724 for 20s. The one-second
            // radius3 scan applies the same GFX6727/status1025 to ally and
            // enemy for an independent non-stacking eight-second lease. Its
            // 900/1000ms clock restores MP5 on callbacks four/eight and deals
            // HP25 on callback five without a damage-action animation.
            CUBE_HARMONY, new CubeHarmonySpell(
                    CUBE_HARMONY, 27, 3, 19,
                    6_724, 6_727,
                    55, 5_000, 32, 20, 3,
                    32, 2, 8, 5, 25,
                    49_156L, 2, 80_152,
                    49_136L, "$5700", "$5954",
                    "cube-harmony")
    );

    private static final Map<Integer, PatienceSpell> PATIENCE_SPELLS = Map.of(
            // Exact selected primary row211 and LiTo.bin zero-based case210.
            // The post-175 generic producer emits selector-only 14 1A 02;
            // the selected PE cast-clock word at VA 0x635918 is 16ms.  The
            // retained row is an ordinary PC/NPC buff: MP25, reuse1000ms,
            // duration600s, range3, action19/GFX6507, no reagent.  Retained
            // L1AttackList marks the customised value -15 but explicitly
            // records the original value as -2, which is authoritative here.
            PATIENCE, new PatienceSpell(
                    PATIENCE, 26, 2, 19, 6_507,
                    25, 1_000, 16, 600, 3,
                    3, 2, 4, 2,
                    true, false,
                    49_127L, "$5691", "$5945", "patience")
    );

    private static final Map<Integer, PhantasmSpell> PHANTASM_SPELLS = Map.of(
            // Exact selected primary row212 and LiTo.bin zero-based case211.
            // The post-175 generic producer emits selector-only 14 1A 03;
            // selected PE cast-clock word VA 0x63591A is 640ms.  The retained
            // skill is a PC/NPC probability buff: MP30, HP25, reuse3000ms,
            // duration5s, range3, action19/GFX6530, no reagent.  Its selected
            // probability row is 35 + MagicHit - StoneResistance, explicitly
            // excluding INT, MagicLevel and MR; the outer retained cap is 90%.
            // PHANTASM.java installs FOG_OF_SLEEPING rather than skill212.
            PHANTASM, new PhantasmSpell(
                    PHANTASM, 26, 3, 19, 6_530,
                    30, 25, 3_000, 640, 5, 3,
                    3, 4, 8,
                    35, 35, 35,
                    true, true, false,
                    49_128L, "$5692", "$5946", "phantasm")
    );

    private static final Map<Integer, ArmBreakerSpell> ARM_BREAKER_SPELLS = Map.of(
            // Exact selected primary row213 and LiTo.bin zero-based case212.
            // The post-175 generic producer emits selector-only 14 1A 04;
            // selected PE cast-clock word VA 0x63591C is 32ms. The retained
            // TYPE_ATTACK mode calculates neutral base15 magic damage and,
            // when DMGUP2/status1026 is absent, installs a non-refreshing
            // twelve-second melee-only DMG -2 lease. Counter Magic executes
            // before both the mode and status. The probability/boss-control
            // row is dormant for TYPE_ATTACK and NPC casters do not run the
            // retained ARM_BREAKER mode.
            ARM_BREAKER, new ArmBreakerSpell(
                    new AttackSpell(ARM_BREAKER, 26, 4, 18, 6_551,
                            25, 1_200, 3, 15, 0, 0,
                            ATTRIBUTE_UNSPECIFIED, "arm-breaker"),
                    32, 12, 3, 64, 16, -2, 1_026,
                    49_129L, "$5693", "$5947",
                    0, false, "arm-breaker")
    );

    private static final Map<Integer, DiamondGolemSpell>
            DIAMOND_GOLEM_SPELLS = Map.of(
            // Exact selected primary row214 and LiTo.bin zero-based case213.
            // The post-175 generic producer emits selector-only 14 1A 05 and
            // selected PE cast-clock word VA 0x63591E is 0ms. The retained
            // PC-only skill permits self or another PC through range3/LOS,
            // consumes MP25, refreshes a non-persistent 64-second lease and
            // applies the selected skills_buff_ability AC modifier -20. It
            // bypasses Counter Magic and is removed by Cancellation.
            ILLUSION_DIAMOND_GOLEM, new DiamondGolemSpell(
                    ILLUSION_DIAMOND_GOLEM, 26, 5, 19, 6_576,
                    25, 2_000, 0, 64, 3,
                    1, 2, 32, -20,
                    49_130L, "$5694", "$5948", true, false,
                    "illusion-diamond-golem")
    );

    private static final Map<Integer, InsightSpell> INSIGHT_SPELLS = Map.of(
            // Exact selected primary row216 and LiTo.bin zero-based index215.
            // The post-175 generic producer emits selector-only 14 1A 07 and
            // selected PE cast-clock word VA 0x635922 is 3600ms. Retained
            // INSIGHT.java permits a PC target through range3, consumes MP40,
            // and installs a non-persistent 600-second +1 STR/CON/DEX/WIS/INT
            // lease. CHA is deliberately excluded. An admitted recast consumes
            // MP and reuse but neither stacks nor refreshes the existing lease.
            INSIGHT, new InsightSpell(
                    INSIGHT, 26, 7, 19, 6_505,
                    40, 1_000, 3_600, 600, 3,
                    1, 2, 128, 1,
                    49_132L, "$5696", "$5950", true, false,
                    "insight")
    );

    private static final Map<Integer, PanicSpell> PANIC_SPELLS = Map.of(
            // Exact selected primary row217 and LiTo.bin zero-based index216.
            // The post-175 generic producer emits selector-only 14 1B 00 and
            // selected PE cast-clock word VA 0x635924 is 0ms. Retained
            // PANIC.java applies a non-persistent, non-stacking and
            // non-refreshing -1 STR/CON/DEX/WIS/INT lease; CHA is excluded.
            // TYPE_CURSE uses 55/45/35 + INT*0.25 + MagicHit - MR*0.05,
            // excludes MagicLevel, caps at 90%, and marks bosses immune.
            PANIC, new PanicSpell(
                    PANIC, 27, 0, 19, 6_506,
                    30, 30, 4_000, 0, 64, 3,
                    3, 4, 1, -1,
                    55, 45, 35, 0.25D, false, true, 0.05D, 90, 0,
                    49_133L, "$5697", "$5951", false, false,
                    "panic")
    );

    private static final Map<Integer, JoyOfPainSpell> JOY_OF_PAIN_SPELLS = Map.of(
            // Exact selected primary row218 and LiTo.bin zero-based index217.
            // The post-175 generic producer emits selector-only 14 1B 01 and
            // selected PE cast-clock word VA 0x635926 is 0ms. Retained
            // TYPE_OTHER installs or refreshes a transient 2-second PC/NPC
            // lease. Counter Magic is not consulted. When the marked actor
            // next commits positive damage to a different PC, that exact lease
            // is consumed after Mortal Body and feeds back (pre-hit missing HP
            // minus equipment-added HP)/3, capped at 1500.
            JOY_OF_PAIN, new JoyOfPainSpell(
                    JOY_OF_PAIN, 27, 1, 19, 6_528,
                    40, 0, 3_000, 0, 2, 3,
                    3, 2, 2,
                    49_134L, "$5698", "$5952", true, false,
                    "joy-of-pain")
    );

    private static final Map<Integer, FreezeAttackSpell> FREEZE_ATTACK_SPELLS = Map.of(
            // Exact selected row 50 and LiTo.bin zero-based case 49:
            // row6/column1, cccdhh object/coordinates, range6, MP100,
            // 500 ms server reuse, LiTo.bin client clock 1000 ms, 16 seconds,
            // action18/gfx756 and 40+6d5 water
            // damage. The selected probability row is 55/45/35 + INT*0.4
            // + MagicHit - MR*0.05 - FreezeResist. Native S_PARALYSE modes
            // 12/13 start/end the client-side freeze action lock.
            ICE_LANCE, new FreezeAttackSpell(ICE_LANCE, 6, 1, 18, 756,
                    100, 0, 500, 1_000, 6, 0, false,
                    16, 40, 6, 5, ATTRIBUTE_WATER,
                    0,
                    55, 45, 35, 0.4D, 0.05D,
                    false, true, "ice-lance"),
            // Exact selected row 80 and LiTo.bin zero-based case 79.  Unlike
            // Ice Lance, this case takes loc_4CBB81/loc_4CBBDA and emits only
            // ccc(row9,column7): it is a caster-centred, no-direction attack.
            // The selected row's lawful delta is -10; its separate area
            // field is +10, so target="none" enumerates radius 10.
            FREEZING_BLIZZARD, new FreezeAttackSpell(
                    FREEZING_BLIZZARD, 9, 7, 18, 3933,
                    60, 0, 3_000, 0, 0, 10, false,
                    32, 2, 5, 10, ATTRIBUTE_WATER,
                    -10,
                    52, 44, 36, 0.38D, 0.05D,
                    false, true, "freezing-blizzard"),
            // Exact selected row194 and LiTo zero-based case193. Since the
            // selector exceeds 175, sub_4CB5F0 takes def_4CB66E and emits only
            // ccc row24/column1: 14 18 01. Its selected PE cast-clock word is
            // 1800ms. The retained C_UseSkill target DWORD/X/Y expectation is
            // therefore restored from the last admitted explicit combat target.
            // World.getVisibleLineObjects then enumerates the inclusive straight
            // line. Primary/secondary rows fix Dragon Knight level25/1, MP0,
            // HP50, duration3, range10, fixed damage45, action18/gfx6988 and
            // water attribute4; selected reuse500ms wins over secondary reuse0.
            // Negative control is 52/44/36 + INT*0.38 + MagicHit - MR*0.05 -
            // freeze resistance, with no MagicLevel. Counter Magic applies,
            // bosses are status-immune but remain damageable, and positive
            // accepted damage precedes the freeze lease.
            FREEZING_BREATH, new FreezeAttackSpell(
                    FREEZING_BREATH, 24, 1, 18, 6_988,
                    0, 50, 500, 1_800, 10, 0, true,
                    3, 45, 0, 0, ATTRIBUTE_WATER,
                    0,
                    52, 44, 36, 0.38D, 0.05D,
                    false, true, "freezing-breath")
    );

    private static final Map<Integer, AreaAttackSpell> AREA_ATTACK_SPELLS = Map.ofEntries(
            // Exact selected row22 and LiTo zero-based case21.  The recovered
            // dispatcher groups case21 with cases16/24/29 at loc_4CBA5C,
            // sets click range6 and calls sub_4CA6E0, whose descriptor is
            // cccdhh.  The selected client cast-clock word is 300 ms.  Both
            // retained databases fix MP20, target_to PC|NPC, target-centred
            // radius3, 0+2d2 water damage and action18/gfx1804.  This spell is
            // distinct from skill10 Chill Touch and skill80 Freezing Blizzard;
            // map508 Fatality traps reuse the same row22 effect metadata but
            // do not replace the player's native C_USE_SPELL producer.
            Map.entry(FROZEN_CLOUD, new AreaAttackSpell(
                    FROZEN_CLOUD, 2, 5, 18, 1_804,
                    20, 0, 300, 6, 3, 0, 2, 2,
                    ATTRIBUTE_WATER, 0, "frozen-cloud")),
            // Exact 380_db.sql skills row 25: attack, range 6, area 3,
            // action 18, gfx 171, MP 20, 5d9 fire damage and 500 ms reuse.
            Map.entry(BURNING_FIREBALL, new AreaAttackSpell(BURNING_FIREBALL, 3, 0, 18, 171,
                    20, 0, 500, 6, 3, 0, 5, 9, ATTRIBUTE_FIRE, 0,
                    "burning-fireball")),
            // Exact selected row30 and LiTo.bin zero-based case29.  The
            // recovered dispatcher groups case29 with cases16/21/24 at
            // loc_4CBA5C, fixes click mode6 and calls sub_4CA6E0, whose
            // descriptor is cccdhh.  The recovered client cast clock is
            // 100ms.  Selected 380_db.sql fixes MP24, server reuse500ms,
            // range8, target-centred radius3, independent 0+5d8 earth
            // damage and action18/gfx1805; secondary yiwei reuse400ms is
            // retained only as source-divergence evidence.
            Map.entry(EARTH_JAIL, new AreaAttackSpell(EARTH_JAIL, 3, 5, 18, 1_805,
                    24, 0, 500, 8, 3, 0, 5, 8, ATTRIBUTE_EARTH, 0,
                    "earth-jail")),
            // Exact selected row 53 and LiTo.bin zero-based case 52:
            // loc_4CBB81 -> loc_4CBBDA emits selector-only ccc row6/column4.
            // The client records its local cast clock through sub_4DFDC0 but
            // does not append a target. target="none", range0 and area4 make
            // this a caster-centred type-0 S_ATTACK_MANY spell. The selected
            // 380_db contract wins over the older source's 1000 ms reuse.
            Map.entry(TORNADO, new AreaAttackSpell(TORNADO, 6, 4, 18, 758,
                    40, 0, 1_500, 0, 4, 16, 5, 10, ATTRIBUTE_WIND, 0,
                    "tornado")),
            // Exact selected row 59 and LiTo.bin zero-based case 58 share
            // loc_4CBB81 -> loc_4CBBDA with Tornado: selector-only ccc
            // row7/column2 and no client target tail.  target="none",
            // range0 and area10 form a caster-centred type-0 S_ATTACK_MANY.
            // The retained source consumes HP/MP before applying the row's
            // lawful -10 on every accepted cast, independently per target.
            Map.entry(BLIZZARD, new AreaAttackSpell(BLIZZARD, 7, 2, 18, 757,
                    60, 10, 1_500, 0, 10, 2, 3, 10, ATTRIBUTE_WATER, -10,
                    "blizzard")),
            // Exact selected row 62 and LiTo.bin zero-based case 61 share
            // loc_4CBB81 -> loc_4CBBDA with Tornado/Blizzard: selector-only
            // ccc row7/column5 and no target DWORD. target="none", range0
            // and area3 make this a caster-centred type-0 S_ATTACK_MANY earth
            // attack. The selected 1500 ms reuse wins over the secondary
            // source's older 500 ms value; neither source consumes an item.
            Map.entry(EARTHQUAKE, new AreaAttackSpell(EARTHQUAKE, 7, 5, 18, 1812,
                    40, 0, 1_500, 0, 3, 32, 6, 10, ATTRIBUTE_EARTH, 0,
                    "earthquake")),
            // Exact selected row 70 and LiTo.bin zero-based case 69 share
            // loc_4CBB81 -> loc_4CBBDA with Tornado/Blizzard/Earthquake:
            // selector-only ccc row8/column5, cast-clock table byte 1 and no
            // target tail. target="none", range0 and area3 make this a
            // caster-centred type-0 S_ATTACK_MANY fire attack. The selected
            // 2,000 ms reuse wins over the secondary source's older 400 ms;
            // neither retained source consumes an item or has a special row.
            Map.entry(FIRE_STORM, new AreaAttackSpell(FIRE_STORM, 8, 5, 18, 1819,
                    48, 0, 2_000, 0, 3, 24, 8, 15, ATTRIBUTE_FIRE, 0,
                    "fire-storm")),
            // Exact selected row 65 and LiTo.bin zero-based case 64:
            // loc_4CB755 -> sub_4CA6E0 emits cccdhh row8/column0 with
            // native click mode10.  This is a primary-target-centred radius3
            // TYPE_DIR attack, not one of the selector-only caster-centred
            // spells.  The selected 2,000 ms reuse wins over the secondary
            // source's older 500 ms value.
            Map.entry(LIGHTNING_STORM, new AreaAttackSpell(LIGHTNING_STORM, 8, 0, 18, 3924,
                    48, 0, 2_000, 10, 3, 20, 5, 10, ATTRIBUTE_WIND, 0,
                    "lightning-storm")),
            // Exact selected row 74 and LiTo.bin zero-based case 73:
            // loc_4CB755 -> sub_4CA6E0 emits cccdhh row9/column1 with
            // native click mode10 and cast-clock table byte1. This is a
            // primary-target-centred radius3 TYPE_DIR attack. The selected
            // 3,000 ms reuse wins over the secondary source's older 1,500 ms;
            // neither retained source consumes an item or has a special row.
            Map.entry(METEOR_STRIKE, new AreaAttackSpell(METEOR_STRIKE, 9, 1, 18, 762,
                    60, 0, 3_000, 10, 3, 110, 8, 15, ATTRIBUTE_FIRE, 0,
                    "meteor-strike")),
            // Exact selected/secondary row184 and selected LiTo zero-based
            // case183.  sub_4CB5F0 routes every case above 175 through the
            // generic ccc producer, so the native request is selector-only
            // 14 16 07 even though both retained C_UseSkill copies expect a
            // target DWORD/X/Y tail.  The server restores that omitted primary
            // from the last admitted combat target, then applies the retained
            // target-centred radius-three attack list.  Selected row184 fixes
            // MP0/HP35, 1000 ms reuse, range10, fixed source damage45, fire
            // attribute2, action18/gfx6516 and no reagent or lawful delta.
            Map.entry(MAGMA_BREATH, new AreaAttackSpell(
                    MAGMA_BREATH, 22, 7, 18, 6_516,
                    0, 35, 1_000, 10, 3, 45, 0, 0,
                    ATTRIBUTE_FIRE, 0, "magma-breath")),
            // Exact selected row189 and LiTo.bin zero-based case188.  The
            // selected executable's generic post-175 producer emits only
            // ccc row23/column4 (14 17 04), with cast-clock word zero.  The
            // authoritative row is an instant caster-centred TYPE_ATTACK:
            // MP0/HP40, reuse10ms, range0, radius2, fixed source damage45,
            // wind attribute8 and action18/gfx6532.  Although constants,
            // skills_item and the book call this slot "Shock Skin", the
            // selected skills row calls it "Magma Arrow" and duration0 means
            // no timed status or teleport block may be fabricated.
            Map.entry(SHOCK_SKIN, new AreaAttackSpell(
                    SHOCK_SKIN, 23, 4, 18, 6_532,
                    0, 40, 10, 0, 2, 45, 0, 0,
                    ATTRIBUTE_WIND, 0, "shock-skin-magma-arrow"))
    );

    private static final Map<Integer, LineAttackSpell> LINE_ATTACK_SPELLS = Map.of(
            // Exact selected 3.80 row 17: attack, selector row 2/column 0,
            // range 6, source area value 3, action 18, gfx 170, MP 20,
            // 4d1 wind damage and 500 ms reuse (the selected SQL uses dice=1). The retained classic
            // World.createLineMap enumerates the inclusive caster-to-primary
            // Bresenham cells; this is not a radius-three area selection.
            LIGHTNING, new LineAttackSpell(LIGHTNING, 2, 0, 18, 170,
                    20, 500, 6, 3, 0, 4, 1, ATTRIBUTE_WIND, "lightning")
    );

    private static final Map<Integer, TurnUndeadSpell> TURN_UNDEAD_SPELLS = Map.of(
            // Exact selected 3.80 row 18: probability/attack, selector row
            // 2/column 1, range 10, action 19, gfx 754, MP 20, 500 ms reuse,
            // 7-sided probability dice with value 1 and failure message 280.
            // Retained L1SkillUse accepts only monsters with IsTU=true and
            // undead type 1 or 3, then replaces damage with current HP.
            TURN_UNDEAD, new TurnUndeadSpell(TURN_UNDEAD, 2, 1, 19, 754,
                    20, 500, 10, 7, 1, 280, "turn-undead")
    );

    private static final Map<Integer, ManaDrainSpell> MANA_DRAIN_SPELLS = Map.of(
            // Exact selected row 39 and LiTo.bin zero-based case 38:
            // row4/column6, cccdhh target/coordinates, MP1, HP50, range4,
            // 500 ms reuse, action19/gfx2171, d12 probability and failure
            // message280. Retained execution drains d10+4+INT/2 MP only
            // after a successful probability/Counter Magic gate. Selected
            // skills_負面boss免疫 row39 divides boss probability by five.
            MANA_DRAIN, new ManaDrainSpell(MANA_DRAIN, 4, 6, 19, 2171,
                    1, 50, 500, 4, 12, 5, 280, "mana-drain")
    );

    private static final Map<Integer, BlindSpell> BLIND_SPELLS = Map.of(
            // Exact selected row 20 and LiTo.bin zero-based case 19:
            // row2/column3, cccdhh, server range1, MP20, 500 ms reuse,
            // 16 seconds, action19, gfx746 and failure message280.  The
            // selected skills_負面技能控制 row overrides the older dice
            // formula with 55/45/35 + INT*0.35 + MagicHit - MR*0.05 - BlindResist.
            CURSE_BLIND, new BlindSpell(CURSE_BLIND, 2, 3, 19, 746,
                    20, 500, 1, 16, 280,
                    55, 45, 35, 0.35D, 0.05D,
                    false, true, 0, 0, 0, "curse-blind"),
            // Exact selected row 40 and LiTo.bin zero-based case 39:
            // row4/column7, cccdhh, native/server range3, MP15,
            // one Magic Gem, 500 ms reuse, 32 seconds, action19/gfx2175
            // and failure message280. The selected probability-control row
            // is the same blind-resistance formula as skill20; selected boss
            // row40 marks bosses immune (modifier zero).
            DARKNESS, new BlindSpell(DARKNESS, 4, 7, 19, 2175,
                    15, 500, 3, 32, 280,
                    55, 45, 35, 0.35D, 0.05D,
                    false, true, 0, InventoryCatalog.MAGIC_GEM, 1, "darkness"),
            // Exact selected row 103 and LiTo.bin zero-based case 102:
            // row12/column6, cccdhh target/coordinates, native range3,
            // MP20, one second-level dark stone, selected reuse990ms,
            // 64 seconds, action19/gfx2947 and failure message280. The
            // selected probability-control row shares the blind-resistance
            // formula with skills20/40; the boss row marks bosses immune.
            DARK_BLIND, new BlindSpell(DARK_BLIND, 12, 6, 19, 2947,
                    20, 990, 3, 64, 280,
                    55, 45, 35, 0.35D, 0.05D,
                    false, true, 0,
                    InventoryCatalog.SECOND_LEVEL_DARK_STONE, 1, "dark-blind")
    );

    private static final Map<Integer, WeaponBreakSpell> WEAPON_BREAK_SPELLS = Map.of(
            // Exact selected row 27 and LiTo.bin zero-based case 26:
            // row3/column2, cccdhh target/coordinates, range4, MP32,
            // one Magic Gem, 3000 ms reuse, action19/gfx172 and failure280.
            // selected skills_負面技能控制 supplies 55/45/35 + INT*0.3
            // + MagicHit - MR*0.05; selected bosses divide the capped result
            // by three. Retained execution adds 1..INT/3 weapon durability
            // to a PC or one of ten -5% physical-output stacks to an NPC.
            WEAPON_BREAK, new WeaponBreakSpell(WEAPON_BREAK,
                    3, 2, 19, 172, 32, 3_000, 4, 280,
                    55, 45, 35, 0.30D, 0.05D,
                    false, true, 3,
                    InventoryCatalog.MAGIC_GEM, 1, "weapon-break")
    );

    private static final Map<Integer, WeaknessSpell> WEAKNESS_SPELLS = Map.of(
            // Exact selected row 47 and LiTo.bin zero-based case 46:
            // row5/column6, cccdhh target/coordinates, range3, MP10,
            // one Magic Gem, 500 ms reuse, 64 seconds, action19/gfx2228,
            // happen692 and failure280. No negative-control override exists,
            // so retained default d12 probability applies; selected boss row
            // divides the capped probability by five. skills_buff_ability
            // applies melee DMG-5/HIT-1 to player targets only.
            WEAKNESS, new WeaknessSpell(WEAKNESS, 5, 6, 19, 2228,
                    10, 500, 3, 64, 12, 5,
                    -5, -1, 0, 0, 692, 280,
                    InventoryCatalog.MAGIC_GEM, 1, "weakness"),
            // Exact selected row 56 and LiTo.bin zero-based case 55:
            // zero-based row6/column7, cccdhh target/coordinates, range3, MP10,
            // one Magic Gem, 500 ms reuse, 64 seconds, action19/gfx2230,
            // no happen message and failure280. The retained default d10
            // probability applies and selected bosses divide the capped
            // probability by five. skills_buff_ability applies melee DMG-6
            // and AC+12 to player targets only; every accepted cast applies
            // the selected lawful delta -10, including probability failure.
            DISEASE, new WeaknessSpell(DISEASE, 6, 7, 19, 2230,
                    10, 500, 3, 64, 10, 5,
                    -6, 0, 12, -10, 0, 280,
                    InventoryCatalog.MAGIC_GEM, 1, "disease")
    );

    private static final Map<Integer, SilenceSpell> SILENCE_SPELLS = Map.of(
            // Exact selected row 64 and LiTo.bin zero-based case 63:
            // row7/column7, cccdhh target/coordinates, server range3, MP50,
            // one Magic Gem, 500 ms reuse, 16 seconds, action19/gfx2177 and
            // failure message280. The selected negative-control row overrides
            // the older probability dice with 55/45/35 + INT*0.35 + MagicHit
            // - MR*0.05. Selected boss modifier zero makes bosses immune.
            SILENCE, new SilenceSpell(SILENCE, 7, 7, 19, 2177,
                    50, 500, 3, 16, 280,
                    55, 45, 35, 0.35D, 0.05D,
                    false, true, 0,
                    InventoryCatalog.MAGIC_GEM, 1, "silence")
    );

    private static final Map<Integer, DecayPotionSpell> DECAY_POTION_SPELLS = Map.of(
            // Exact selected row 71 and LiTo.bin zero-based case 70:
            // loc_4CB938 -> sub_4CA6E0 emits cccdhh row8/column6 with native
            // click range3.  The spell consumes MP80 and two Magic Gems,
            // has a recovered 500 ms client cast clock, reuses after 500 ms,
            // lasts 16 seconds, uses action19/gfx2232 and reports failure
            // message280.  No selected negative-control
            // override exists, so retained default d8 probability applies;
            // selected boss modifier zero makes bosses immune.
            DECAY_POTION, new DecayPotionSpell(
                    DECAY_POTION, 8, 6, 19, 2232,
                    80, 500, 500, 3, 16, 8, 0, 280,
                    InventoryCatalog.MAGIC_GEM, 2, "decay-potion")
    );

    private static final Map<Integer, PolluteWaterSpell> POLLUTE_WATER_SPELLS = Map.of(
            // Exact selected row 173 and LiTo.bin zero-based case 172:
            // loc_4CB755 invokes sub_4CA6E0 with native click range10 and
            // emits cccdhh row21/column4. The authoritative server range is3.
            // Selected source consumes MP50 and one Spirit Crystal, reuses
            // after 1000ms, lasts 192s and uses action19/gfx5830. Its exact
            // negative-control row is 55/45/35 + INT*0.30 + MagicHit
            // - MR*0.05; boss modifier0 is immunity. Selected
            // skills_魔屏無效技能 contains173, so a successful cast bypasses
            // and preserves Counter Magic instead of consuming it.
            POLLUTE_WATER, new PolluteWaterSpell(
                    POLLUTE_WATER, 21, 4, 19, 5830,
                    50, 1000, 0, 1000, 3, 192, ATTRIBUTE_WATER, 280,
                    55, 45, 35, 0.30D, 0.05D,
                    false, true, 0, true,
                    InventoryCatalog.SPIRIT_CRYSTAL, 1, "pollute-water")
    );

    private static final Map<Integer, StrikerGaleSpell> STRIKER_GALE_SPELLS = Map.of(
            // Exact selected row 174 and LiTo.bin zero-based case 173:
            // loc_4CB755 invokes sub_4CA6E0 with native click range10 and
            // emits cccdhh row21/column5. The retained server ranged=-1 is
            // the exact native visible screen rather than a numeric radius.
            // Selected source consumes MP10 and five Spirit Crystals, reuses
            // after 3000ms, lasts 192s and uses action19/gfx5826. Its exact
            // negative-control row is 55/45/35 + INT*0.30 + MagicHit
            // - MR*0.05; boss modifier0 is immunity. Selected
            // skills_魔屏無效技能 contains174, while 380yiwei does not.
            STRIKER_GALE, new StrikerGaleSpell(
                    STRIKER_GALE, 21, 5, 19, 5826,
                    10, 3000, 0, 3000, 10, -1,
                    192, ATTRIBUTE_WIND, 280,
                    55, 45, 35, 0.30D, 0.05D,
                    false, true, 0, true,
                    InventoryCatalog.SPIRIT_CRYSTAL, 5, "striker-gale")
    );

    private static final Map<Integer, CounterDetectionSpell> COUNTER_DETECTION_SPELLS = Map.of(
            // Exact selected row 72 and LiTo.bin zero-based case 71:
            // loc_4CB51C emits selector-only ccc row8/column7.  The selected
            // source consumes MP30, has no reagent, reuses after 500 ms and
            // uses action19/gfx3934. The recovered client word clock is also
            // 500 ms. damage_value=24 plus 4d10, attribute0;
            // area=-1 means the exact native visible screen, not radius 24.
            COUNTER_DETECTION, new CounterDetectionSpell(
                    COUNTER_DETECTION, 8, 7, 19, 3934,
                    30, 500, 500, -1, 24, 4, 10, 0,
                    "counter-detection")
    );

    private static final Map<Integer, DetectionSpell> DETECTION_SPELLS = Map.of(
            // Exact selected row 13 and LiTo.bin zero-based case 12. The
            // shared loc_4CBBD4 producer emits selector-only ccc row1/column4.
            // Source data consumes MP15, has no reagent, reuses after 500 ms,
            // uses action19/gfx749 and area=-1 (the native visible screen).
            // Retained skill execution reveals invisible non-GM players,
            // surfaces HIDDEN_STATUS_SINK NPCs and asks WorldTrap to detect
            // detectionable traps; it never deals damage.
            DETECTION, new DetectionSpell(
                    DETECTION, 1, 4, 19, 749,
                    15, 500, -1, 2, 16, "detection")
    );

    private static final Map<Integer, RevealWeaknessSpell>
            REVEAL_WEAKNESS_SPELLS = Map.of(
            // Exact selected row23 and LiTo.bin zero-based case22.  The
            // loc_4CB71D/sub_4C9880 producer emits target-only cccd
            // 14 02 06 + d(target), while word_635774[22] fixes 10 ms.
            // The selected source consumes MP8, uses native-screen range,
            // action19/gfx2510 and target_to NPC. Retained L1SkillUse then
            // publishes one effect for every positive weakAttr bit in this
            // exact order: earth2169, fire2167, water2166, wind2168. It is a
            // TYPE_BUFF probe: it neither damages nor consumes Counter Magic.
            REVEAL_WEAKNESS, new RevealWeaknessSpell(
                    REVEAL_WEAKNESS, 2, 6, 19, 2_510,
                    8, 10, -1, 2, 0, 128, 64, 280,
                    2_169, 2_167, 2_166, 2_168,
                    "reveal-weakness")
    );

    private static final Map<Integer, MagicalWeaponSpell> MAGICAL_WEAPON_SPELLS = Map.of(
            // Exact selected row 73 and LiTo.bin zero-based case 72. The
            // inventory callback loc_4C9D76 writes cccd: row9/column0 plus
            // the selected inventory object id, with no coordinate or tick
            // tail. Source data consumes MP200 and ten Magic Gems, has zero
            // reuse/duration, action19/gfx763 and happen message685. Retained
            // runSkill changes only a safe +0 weapon to +1; all other object
            // ids report message79 after the ordinary costs are committed.
            CREATE_MAGICAL_WEAPON, new MagicalWeaponSpell(
                    CREATE_MAGICAL_WEAPON, 9, 0, 19, 763,
                    200, 0, InventoryCatalog.MAGIC_GEM, 10,
                    685, 79, 161, "create-magical-weapon")
    );

    private static final Map<Integer, CreateZombieSpell> CREATE_ZOMBIE_SPELLS = Map.of(
            // Exact selected row 41 and LiTo.bin zero-based case 40:
            // row5/column0, cccd corpse-object target, native-screen range,
            // MP5, HP80, one Magic Gem, 10 ms reuse, action19/gfx226 and
            // failure message280. Retained L1SkillUse accepts only a dead
            // monster and consumes six charisma points for the created summon.
            CREATE_ZOMBIE, new CreateZombieSpell(CREATE_ZOMBIE, 5, 0, 19, 226,
                    5, 80, 10, -1, 280,
                    InventoryCatalog.MAGIC_GEM, 1, 6, "create-zombie")
    );

    private static final Map<Integer, ResurrectionSpell> RESURRECTION_SPELLS = Map.of(
            // Exact selected 380_db row 61 plus LiTo.bin zero-based case 60:
            // row7/column4 and cccd(target object), MP50, no HP/item cost,
            // native-screen range, 10 ms reuse, action19/gfx3944 and failure
            // message280. Retained skillmode proves a dead player is asked with
            // question321 and revived at half HP on acceptance; an eligible
            // dead NPC is revived immediately at quarter HP.
            RESURRECTION, new ResurrectionSpell(RESURRECTION, 7, 4, 19, 3944,
                    50, 10, -1, 280, 0, 0,
                    321, 2, 4, false, 0, "resurrection"),
            // Exact selected row 75 and LiTo.bin zero-based case 74 share the
            // same cccd object-only producer: selector row9/column2 plus target
            // object id, with no coordinate or client-tick tail. The selected
            // source costs MP100 and one Magic Gem, uses action19/gfx3944 and
            // question322. Retained GREATER_RESURRECTION/C_Attr revives a PC
            // at full HP and restores lost open-world death EXP when such a
            // loss exists; eligible NPCs retain the ordinary quarter-HP path.
            GREATER_RESURRECTION, new ResurrectionSpell(
                    GREATER_RESURRECTION, 9, 2, 19, 3944,
                    100, 10, -1, 280, InventoryCatalog.MAGIC_GEM, 1,
                    322, 1, 4, true, 0, "greater-resurrection"),
            // Exact selected row165 and LiTo.bin zero-based case164:
            // loc_4CB71D -> sub_4C9880 emits cccd row20/column4 plus one
            // corpse object id.  Both selected DBs and the PE word at
            // 0x6358BC pin reuse to zero.  This water-elf restore spell costs
            // MP50 plus one Spirit Crystal, reaches ten cells and asks dead
            // players with question322.  Unlike Greater Resurrection it does
            // not set the retained G-RES flag, so it restores full HP but not
            // death EXP; eligible NPCs are restored immediately at full HP.
            CALL_OF_NATURE, new ResurrectionSpell(
                    CALL_OF_NATURE, 20, 4, 19, 2245,
                    50, 0, 10, 280, InventoryCatalog.SPIRIT_CRYSTAL, 1,
                    322, 1, 1, false, ATTRIBUTE_WATER, "call-of-nature")
    );

    private static final Map<Integer, MassSlowSpell> MASS_SLOW_SPELLS = Map.of(
            // Exact selected row 76 and LiTo.bin zero-based case 75:
            // loc_4CB989 calls sub_4CA6E0 with click range 10 and writes
            // cccdhh (row9/column3, target id and coordinate snapshot).
            // The server row centers a radius-three PC/NPC search on the
            // selected target, excludes the caster, consumes MP20 plus two
            // Magic Gems, reuses after 3000 ms and applies slow for 64 s.
            // No negative-control override exists: retained default d7
            // probability applies; selected bosses divide it by three.
            MASS_SLOW, new MassSlowSpell(MASS_SLOW, 9, 3, 19, 3932,
                    20, 3_000, 10, 3, 64, 7, 3, 280,
                    InventoryCatalog.MAGIC_GEM, 2, "mass-slow")
    );

    private static final Map<Integer, EntangleSpell> ENTANGLE_SPELLS = Map.of(
            // Exact selected row29 and LiTo.bin zero-based case28: the
            // loc_4CB755/sub_4CA6E0 producer uses a native click gate of ten
            // cells and emits cccdhh row3/column4. The selected server row is
            // authoritative for MP20, one Magic Gem, reuse500ms (the older
            // yiwei row says1000), duration64s, target_to3, range8,
            // action19/gfx752 and stop/failure messages277/280. Negative
            // control is 55/45/35 + INT*0.30 + MagicHit - MR*0.05; selected
            // bosses divide the capped probability by five.
            SLOW, new EntangleSpell(
                    SLOW, 3, 4, 4, 4, 19, 752,
                    20, 500, 64, 3, 8, 0, 0,
                    ATTRIBUTE_UNSPECIFIED, 1, 16,
                    0, 277, 280,
                    55, 45, 35, 0.30D, 0.05D,
                    false, true, 5, false,
                    InventoryCatalog.MAGIC_GEM, 1, "slow"),
            // Exact selected row152 and LiTo.bin zero-based case151: byte-table
            // entry2 enters loc_4CB755/sub_4CA6E0 and emits cccdhh with
            // row18/column7, object id and coordinate snapshot. The client
            // click gate and authoritative server range are both ten cells;
            // word_635774[151] contributes zero cast-clock delay. Selected
            // source consumes MP20 and one Spirit Crystal, has no reuse delay,
            // lasts 64s, uses action19/gfx2250 and stop/failure messages
            // 711/280. skills_負面技能控制 supplies 55/45/35 + INT*0.30
            // + MagicHit - MR*0.05; selected bosses are immune. target_to=3
            // admits players and erase-eligible NPCs, area=0 is one target.
            ENTANGLE, new EntangleSpell(
                    ENTANGLE, 18, 7, 19, 7, 19, 2250,
                    20, 0, 64, 3, 10, 0, 0,
                    ATTRIBUTE_UNSPECIFIED, 1, 128,
                    0, 711, 280,
                    55, 45, 35, 0.30D, 0.05D,
                    false, true, 0, false,
                    InventoryCatalog.SPIRIT_CRYSTAL, 1, "entangle")
    );

    private static final Map<Integer, EraseMagicSpell> ERASE_MAGIC_SPELLS = Map.of(
            // Exact selected row153 and LiTo.bin zero-based case152: the same
            // loc_4CB755/sub_4CA6E0 target producer emits cccdhh row19/column0,
            // object id and coordinate snapshot with a ten-cell client click
            // gate; word_635774[152] is zero. The selected DB is authoritative
            // for the 600 ms reuse divergence from the older secondary row.
            // It costs MP10 and one Spirit Crystal, lasts 32 seconds, uses
            // action19/gfx10706 and failure280. target_to3 admits PC plus
            // erase-eligible NPC, ranged=-1 means native screen. The negative
            // table fixes 55/45/35 + INT*0.35 + MagicHit - MR*0.05, bosses are
            // immune, and selected skills_魔屏無效技能 makes Counter Magic bypass.
            ERASE_MAGIC, new EraseMagicSpell(
                    ERASE_MAGIC, 19, 0, 20, 0, 19, 10_706,
                    10, 600, 32, 3, -1, 0, 0,
                    ATTRIBUTE_UNSPECIFIED, 1, 1,
                    0, 0, 280,
                    55, 45, 35, 0.35D, 0.05D,
                    false, true, 0, true,
                    InventoryCatalog.SPIRIT_CRYSTAL, 1, "erase-magic")
    );

    private static final Map<Integer, ElementalSummonSpell> ELEMENTAL_SUMMON_SPELLS = Map.of(
            // Exact selected row154 and LiTo.bin zero-based case153:
            // loc_4CBBD4 emits selector-only ccc row19/column1 and the PE
            // timing word is zero. The retained LESSER_ELEMENTAL mode selects
            // the lowest ElfAttr bit (earth/fire/water/wind), admits no second
            // pet-cost actor, and materializes NPC 45306/45303/45304/45305.
            // The normal L1SummonInstance constructor gives it haste, gfx755
            // spawn feedback and the active 1,800-second summon lifetime.
            LESSER_ELEMENTAL, new ElementalSummonSpell(
                    LESSER_ELEMENTAL, 19, 1, 20, 1, 19, 2510,
                    20, 0, 0, 0, 0, 0,
                    ATTRIBUTE_UNSPECIFIED, 128, 2,
                    InventoryCatalog.SPIRIT_CRYSTAL, 2,
                    353, 755, 1_800, true, "lesser-elemental"),
            // Exact selected row162 and LiTo.bin zero-based case161:
            // the same selector-only ccc branch emits row20/column1 and the PE
            // timing word remains zero. The retained GREATER_ELEMENTAL mode
            // preserves Lesser Elemental's post-commit gates and attribute-bit
            // priority, but consumes four Spirit Crystals and materializes
            // NPC 81053/81050/81051/81052 from the selected source database.
            GREATER_ELEMENTAL, new ElementalSummonSpell(
                    GREATER_ELEMENTAL, 20, 1, 21, 1, 19, 2510,
                    20, 0, 0, 0, 0, 0,
                    ATTRIBUTE_UNSPECIFIED, 128, 2,
                    InventoryCatalog.SPIRIT_CRYSTAL, 4,
                    353, 755, 1_800, true, "greater-elemental")
    );

    private static final Map<Integer, SummonSpell> SUMMON_SPELLS = Map.of(
            // Exact selected row 51 and LiTo.bin zero-based case 50:
            // row6/column2, cccd with one summon-selection dword, MP30,
            // HP30, three Magic Gems, 1,000 ms reuse, action19/gfx2510 and
            // lawful -10.  Without the control ring the native producer sends
            // the clamped level grade 7..13.  With the ring, summonlist is a
            // client-local menu and sends one of the 31 values below.
            SUMMON_MONSTER, new SummonSpell(SUMMON_MONSTER, 6, 2, 19, 2510,
                    30, 30, 1_000, InventoryCatalog.MAGIC_GEM, 3,
                    -10, 353, 319, 743, 755, 1_800, "summon-monster")
    );

    private static final Map<Integer, SummonChoice> SUMMON_CHOICES = Map.ofEntries(
            summon(7, 81210, 28, 8, true),
            summon(263, 81211, 28, 8, false),
            summon(519, 81212, 28, 8, false),
            summon(8, 81213, 32, 8, true),
            summon(264, 81214, 32, 8, false),
            summon(520, 81215, 32, 8, false),
            summon(9, 81216, 36, 8, true),
            summon(265, 81217, 36, 8, false),
            summon(521, 81218, 36, 8, false),
            summon(10, 81219, 40, 8, true),
            summon(266, 81220, 40, 8, false),
            summon(522, 81221, 40, 8, false),
            summon(11, 81222, 44, 8, true),
            summon(267, 81223, 44, 8, false),
            summon(523, 81224, 44, 8, false),
            summon(12, 81225, 48, 8, true),
            summon(268, 81226, 48, 8, false),
            summon(524, 81227, 48, 8, false),
            summon(13, 81228, 52, 8, true),
            summon(269, 81229, 52, 8, false),
            summon(525, 81230, 52, 8, false),
            summon(14, 81231, 56, 10, false),
            summon(270, 81232, 56, 10, false),
            summon(526, 81233, 56, 10, false),
            summon(15, 81234, 60, 12, false),
            summon(271, 81235, 60, 12, false),
            summon(527, 81236, 60, 12, false),
            summon(16, 81237, 64, 20, false),
            summonSpecial(17, 81238, 68, 42),
            summonSpecial(18, 81239, 72, 42),
            summonSpecial(274, 81240, 72, 44)
    );

    private static final Map<Integer, ZombieTemplate> ZOMBIE_TEMPLATES = Map.of(
            45065, new ZombieTemplate(45065, 52, "$57", 6, 45),
            81183, new ZombieTemplate(81183, 52, "$57", 10, 100),
            81184, new ZombieTemplate(81184, 52, "$57", 12, 200),
            81185, new ZombieTemplate(81185, 52, "$57", 14, 400),
            81186, new ZombieTemplate(81186, 52, "$57", 16, 500),
            81187, new ZombieTemplate(81187, 52, "$57", 18, 600),
            81188, new ZombieTemplate(81188, 52, "$57", 20, 800)
    );

    private static final Map<Integer, SleepSpell> SLEEP_SPELLS = Map.of(
            // Exact selected row 66 and LiTo.bin zero-based case 65:
            // row8/column1, cccdhh target/coordinates, native click mode 10,
            // server range10/area3, MP50, 1.5 s reuse, 32 seconds,
            // action19/gfx760 and one Magic Gem. The selected probability
            // row is 50/42/34 + INT*0.35 + MagicHit - MR*0.05 - SleepResist.
            FOG_OF_SLEEPING, new SleepSpell(FOG_OF_SLEEPING, 8, 1, 19, 760,
                    50, 1_500, 10, 3, 32, 297, 280,
                    50, 42, 34, 0.35D, 0.05D,
                    false, true, InventoryCatalog.MAGIC_GEM, 1,
                    "fog-of-sleeping")
    );

    private static final Map<Integer, PolymorphSpell> POLYMORPH_SPELLS = Map.of(
            // Exact selected row 67 and LiTo.bin zero-based case 66:
            // row8/column2, cccd object-only target, native-screen PC target,
            // MP80, 10ms reuse, 7200 seconds, action19/gfx230 and one Magic
            // Gem.  A polymorph-control ring opens standard Text.pak monlist;
            // without it the retained weighted polymorph table is applied.
            SHAPE_CHANGE, new PolymorphSpell(SHAPE_CHANGE, 8, 2, 19, 230,
                    80, 0, 10, 7_200, -1,
                    InventoryCatalog.MAGIC_GEM, 1, 280, "shape-change")
    );

    private static final Map<Integer, BindSpell> BIND_SPELLS = Map.of(
            // Exact selected row 157 and LiTo.bin zero-based case 156:
            // row19/column4, cccdhh target/coordinates, native-screen range,
            // MP50, two Spirit Crystals, 600 ms reuse, 16 seconds,
            // action19/gfx2251 and failure message280.  The selected
            // probability row is 50/42/35 + INT*0.4 + MagicHit -
            // SustainResist; it explicitly excludes MagicLevel and MR.
            EARTH_BIND, new BindSpell(EARTH_BIND, 19, 4, 19, 2251,
                    50, 600, -1, 16, 280,
                    50, 42, 35, 0.4D,
                    false, true, InventoryCatalog.SPIRIT_CRYSTAL, 2,
                    "earth-bind")
    );

    private static final Map<Integer, PoisonSpell> POISON_SPELLS = Map.of(
            // Exact selected row11 and LiTo.bin zero-based case10. The client
            // uses loc_4CB938/sub_4CA6E0 with picker radius3 and cccdhh; the
            // selected database's range1 remains server authority. Retained
            // L1DamagePoison fixes 30s, 3000ms and 5 damage per tick.
            CURSE_POISON, new PoisonSpell(CURSE_POISON, 1, 2, 19, 745,
                    10, 500, 1, 30, 3_000, 5, 12, 0, -10, 280,
                    40318, 1, "curse-poison")
    );

    private static final Map<Integer, ParalysisSpell> PARALYSIS_SPELLS = Map.of(
            // Exact LiTo index-32 producer fixes row 4/column 0 and the
            // object-coordinate cccdhh envelope.  Selected row 33 is the
            // authoritative Tournament resource: MP150, two Magic Gems,
            // range2, 5s reuse, action19/gfx746 and failure message280.  Its
            // lawful column is -500 on every accepted cast.  The selected
            // ConfigSkill defaults and deployed 4-職業_技能設定表.properties
            // both fix CURSE_READY_TIME=5s and CURSE_TIME=10s.  The recovered
            // PE word_635774[32] independently fixes the client clock at5s.
            // negative-skill row is 50/40/30 + INT*0.4 + MagicHit
            // - MR*0.05 - StoneResistance; MagicLevel is disabled.
            CURSE_PARALYZE, new ParalysisSpell(CURSE_PARALYZE, 4, 0, 19, 746,
                    150, 5_000, 5_000, 2, 5_000, 10_000, -500, 280,
                    50, 40, 30, 0.4D, 0.05D, false, true,
                    40318, 2, "curse-paralyze")
    );

    private static final Map<Integer, TamingSpell> TAMING_SPELLS = Map.of(
            // Exact selected row 36 and LiTo.bin zero-based case 35:
            // row4/column3, cccdhh target/coordinates, native-screen range,
            // MP30, HP20, one Magic Gem, 10 ms reuse, action19/gfx228 and
            // failure message280. The selected probability row overrides the
            // older HP-dependent fallback with 40/35/30 + INT*0.5 +
            // MagicLevel + MagicHit - MR*0.05, followed by the outer 90% cap.
            TAMING_MONSTER, new TamingSpell(TAMING_MONSTER, 4, 3, 19, 228,
                    30, 20, 10, -1, 3_600, 280,
                    40, 35, 30, 0.5D, 0.05D, true, true,
                    InventoryCatalog.MAGIC_GEM, 1, "taming-monster")
    );

    private static final Map<Integer, CurseRemovalSpell> CURSE_REMOVAL_SPELLS = Map.of(
            // Exact selected row 9 and LiTo.bin zero-based case 8: row1/
            // column0, target-only cccd envelope, native-screen range,
            // MP15, 10 ms reuse, action19/gfx871 and no reagent. Retained
            // CURE_POISON calls only cha.curePoison: damage/silence/poison-
            // sourced paralysis are eligible, skill33 curse paralysis is not.
            CURE_POISON, new CurseRemovalSpell(CURE_POISON, 1, 0, 19, 871,
                    15, 10, -1, false, "cure-poison"),
            // Exact selected row 37 and LiTo.bin zero-based case 36:
            // row4/column4, target-only cccd envelope, native-screen range,
            // MP20, 10 ms reuse, action19/gfx227 and no reagent. Retained
            // REMOVE_CURSE cures the installed poison object (damage,
            // silence or poison-sourced paralysis) plus curse paralysis.
            REMOVE_CURSE, new CurseRemovalSpell(REMOVE_CURSE, 4, 4, 19, 227,
                    20, 10, -1, true, "remove-curse")
    );

    private static final Map<Integer, InvisibilitySpell> INVISIBILITY_SPELLS = Map.of(
            // Skill 60 takes LiTo.bin's cccd self/tick producer. The selected
            // skills row has no effect gfx and duration 0, so the lease is
            // indefinite until a retained reveal condition occurs.
            INVISIBILITY, new InvisibilitySpell(INVISIBILITY, 7, 3, 19,
                    0, 15, 0, 0, 40318, 1,
                    RequestShape.SELF_TICK, "invisibility"),
            // Exact selected row 97: Dark Elf level 13, MP 10, 9,990 ms
            // reuse, 32-second self lease, action 19 and gfx 2941. LiTo.bin
            // zero-based case 96 enters loc_4CBBD4 and writes only ccc.
            BLIND_HIDING, new InvisibilitySpell(BLIND_HIDING, 12, 0, 19,
                    2941, 10, 9_990, 32, 0, 0,
                    RequestShape.SELECTOR_ONLY, "blind-hiding")
    );

    private static final Map<Integer, EnchantVenomSpell> ENCHANT_VENOM_SPELLS = Map.of(
            // Exact selected row 98 and LiTo.bin zero-based case 97:
            // row12/column1 selector-only ccc, HP1, MP10, one second-level
            // dark stone, no reuse delay, 320 seconds, action19/gfx2942.
            // The retained attack path attempts an ordinary poison proc on
            // every weapon attack with an inclusive one-based 10% roll.
            ENCHANT_VENOM, new EnchantVenomSpell(
                    ENCHANT_VENOM, 12, 1, 19, 2942,
                    10, 1, 0, 320,
                    InventoryCatalog.SECOND_LEVEL_DARK_STONE, 1,
                    10, 30, 3_000, 5, "enchant-venom")
    );

    private static final Map<Integer, BringStoneSpell> BRING_STONE_SPELLS = Map.of(
            // Exact selected row 100 and LiTo.bin zero-based case 99:
            // row12/column3 plus the selected inventory object dword, MP5,
            // no HP/reagent/reuse/duration, action19 and caster gfx2944.
            BRING_STONE, new BringStoneSpell(
                    BRING_STONE, 12, 3, 19, 2944,
                    5, 0, 280, 403, "bring-stone")
    );

    private static final Map<Long, StoneRefinement> STONE_REFINEMENTS = Map.of(
            InventoryCatalog.FIRST_LEVEL_DARK_STONE,
            new StoneRefinement(0, InventoryCatalog.FIRST_LEVEL_DARK_STONE,
                    InventoryCatalog.SECOND_LEVEL_DARK_STONE, "$2475"),
            InventoryCatalog.SECOND_LEVEL_DARK_STONE,
            new StoneRefinement(1, InventoryCatalog.SECOND_LEVEL_DARK_STONE,
                    InventoryCatalog.THIRD_LEVEL_DARK_STONE, "$2476"),
            InventoryCatalog.THIRD_LEVEL_DARK_STONE,
            new StoneRefinement(2, InventoryCatalog.THIRD_LEVEL_DARK_STONE,
                    InventoryCatalog.FOURTH_LEVEL_DARK_STONE, "$2477"),
            InventoryCatalog.FOURTH_LEVEL_DARK_STONE,
            new StoneRefinement(3, InventoryCatalog.FOURTH_LEVEL_DARK_STONE,
                    InventoryCatalog.FIFTH_LEVEL_DARK_STONE, "$2478")
    );

    private static final Map<Integer, TrueTargetSpell> TRUE_TARGET_SPELLS = Map.of(
            // Exact 380_db.sql and 380yiwei_db.sql row 113: royal level 15,
            // selector row 14/column 0, target_to PC|NPC (3), action 19,
            // gfx 1765, MP 1, HP/material/reuse/duration 0, ranged=-1,
            // attribute 0, type 2 and native id 1. Retained L1SkillUse lists
            // 113 in EXCEPT_COUNTER_MAGIC and suppresses the ordinary gfx;
            // the skill mode emits the dedicated target marker instead.
            TRUE_TARGET, new TrueTargetSpell(TRUE_TARGET, 14, 0, 15,
                    19, 1765, 1, 0, 0, 0, -1,
                    3, 0, 2, 1, "$1995", true, "true-target")
    );

    private static final Map<Integer, RoyalAuraSpell> ROYAL_AURA_SPELLS = Map.of(
            // Exact selected/secondary row 114 and retained skill mode:
            // royal level 15, selector row14/column1, MP40, no HP/reagent/reuse,
            // target_to PARTY (8), native-screen area (-1), action19/gfx2277,
            // HIT+5/BOW_HIT+5 from skills_buff_ability plus the retained MR+20.
            GLOWING_AURA, new RoyalAuraSpell(
                    GLOWING_AURA, 14, 1, 15, 19, 2277,
                    40, 0, 0, 640, 0, -1, 8,
                    0, 2, 2, "$1996", 0, 5, 5, 20, 0,
                    true, "glowing-aura"),
            // Exact selected/secondary row 115 and retained skill mode:
            // target_to CLAN|PARTY (12), native-screen area (-1),
            // action19/gfx3941 and AC-8 from skills_buff_ability.
            SHINING_AURA, new RoyalAuraSpell(
                    SHINING_AURA, 14, 2, 15, 19, 3941,
                    40, 0, 0, 640, 0, -1, 12,
                    0, 2, 4, "$1977", -8, 0, 0, 0, 0,
                    true, "shining-aura"),
            // Exact selected/secondary row 117 and retained skill mode:
            // target_to PARTY (8), native-screen area (-1), action19/gfx3942
            // and melee-only DMG+5 from skills_buff_ability.  Unlike the
            // 114/115 pair, BRAVE_AURA is not in REPEATED_SKILLS and coexists.
            BRAVE_AURA, new RoyalAuraSpell(
                    BRAVE_AURA, 14, 4, 15, 19, 3942,
                    40, 0, 0, 640, 0, -1, 8,
                    0, 2, 16, "$3178", 0, 0, 0, 0, 5,
                    true, "brave-aura")
    );

    private static final Map<Integer, CallClanSpell> CALL_CLAN_SPELLS = Map.of(
            // Exact selected/secondary row 116 and LiTo.bin zero-based case
            // 115: cccs row14/column3 name request, royal level15, MP30,
            // action19, no gfx/reagent/reuse/duration and question729.  The
            // retained target mode resolves an online same-clan PC by name
            // and places an accepted member one tile in the captured heading.
            CALL_CLAN, new CallClanSpell(
                    CALL_CLAN, 14, 3, 15, 19,
                    30, 0, 0, 0, 0, 0,
                    0, 2, 8, "", 0,
                    BattleCallClan.QUESTION_ID, 280, true, "call-clan")
    );

    private static final Map<Integer, RunClanSpell> RUN_CLAN_SPELLS = Map.of(
            // Exact selected/secondary row 118 and same-version official
            // zero-based case117 intent: cccs row14/column5 name request,
            // royal level15, MP30, action19, no gfx/reagent/reuse/duration.
            // The selected LiTo callback has a product bug that emits column3;
            // the isolated compatibility proxy repairs that byte in memory.
            RUN_CLAN, new RunClanSpell(
                    RUN_CLAN, 14, 5, 15, 19,
                    30, 0, 0, 0, 0, 0,
                    0, 2, 32, "", 0,
                    280, true, "run-clan")
    );

    private BattleSpellCatalog() {}

    interface TargetNegativeStatusSpell {
        int skillId();
        int row();
        int column();
        int actionId();
        int effectGfx();
        int mpCost();
        int reuseDelayMillis();
        int secondaryReuseDelayMillis();
        int clientCastClockMillis();
        int maximumRange();
        int durationSeconds();
        int attribute();
        int failureMessage();
        boolean includeMagicLevel();
        int bossModifier();
        boolean counterMagicBypass();
        long itemConsumeId();
        int itemConsumeCount();
        int probabilityForTarget(int casterLevel, int targetLevel,
                                 int effectiveIntelligence, int magicLevel,
                                 int magicHit, int targetMagicResistance,
                                 boolean targetBoss);
        int effectiveMpCost(int originalReduction);
    }

    static GroundSpell groundSpell(int skillId) {
        return GROUND_SPELLS.get(skillId);
    }

    static HealingSpell healingSpell(int skillId) {
        return HEALING_SPELLS.get(skillId);
    }

    static GroupHealingSpell groupHealingSpell(int skillId) {
        return GROUP_HEALING_SPELLS.get(skillId);
    }

    static NaturesBlessingSpell naturesBlessingSpell(int skillId) {
        return NATURES_BLESSING_SPELLS.get(skillId);
    }

    static LightSpell lightSpell(int skillId) {
        return LIGHT_SPELLS.get(skillId);
    }

    static ShieldSpell shieldSpell(int skillId) {
        return SHIELD_SPELLS.get(skillId);
    }

    static CounterMagicSpell counterMagicSpell(int skillId) {
        return COUNTER_MAGIC_SPELLS.get(skillId);
    }

    static AbsoluteBarrierSpell absoluteBarrierSpell(int skillId) {
        return ABSOLUTE_BARRIER_SPELLS.get(skillId);
    }

    static AdvanceSpiritSpell advanceSpiritSpell(int skillId) {
        return ADVANCE_SPIRIT_SPELLS.get(skillId);
    }

    static ShockStunSpell shockStunSpell(int skillId) {
        return SHOCK_STUN_SPELLS.get(skillId);
    }

    static ArmorBreakSpell armorBreakSpell(int skillId) {
        return ARMOR_BREAK_SPELLS.get(skillId);
    }

    static ReductionArmorSpell reductionArmorSpell(int skillId) {
        return REDUCTION_ARMOR_SPELLS.get(skillId);
    }

    static BounceAttackSpell bounceAttackSpell(int skillId) {
        return BOUNCE_ATTACK_SPELLS.get(skillId);
    }

    static SolidCarriageSpell solidCarriageSpell(int skillId) {
        return SOLID_CARRIAGE_SPELLS.get(skillId);
    }

    static CounterBarrierSpell counterBarrierSpell(int skillId) {
        return COUNTER_BARRIER_SPELLS.get(skillId);
    }

    static MortalBodySpell mortalBodySpell(int skillId) {
        return MORTAL_BODY_SPELLS.get(skillId);
    }

    static ReservedSkillSlot reservedSkillSlot(int skillId) {
        return RESERVED_SKILL_SLOTS.get(skillId);
    }

    static MagicResistanceSpell magicResistanceSpell(int skillId) {
        return MAGIC_RESISTANCE_SPELLS.get(skillId);
    }

    static BodyToMindSpell bodyToMindSpell(int skillId) {
        return BODY_TO_MIND_SPELLS.get(skillId);
    }

    static WorldTreeCallSpell worldTreeCallSpell(int skillId) {
        return WORLD_TREE_CALL_SPELLS.get(skillId);
    }

    static TripleArrowSpell tripleArrowSpell(int skillId) {
        return TRIPLE_ARROW_SPELLS.get(skillId);
    }

    static ElementalFallDownSpell elementalFallDownSpell(int skillId) {
        return ELEMENTAL_FALL_DOWN_SPELLS.get(skillId);
    }

    static ReturnToNatureSpell returnToNatureSpell(int skillId) {
        return RETURN_TO_NATURE_SPELLS.get(skillId);
    }

    static CounterMirrorSpell counterMirrorSpell(int skillId) {
        return COUNTER_MIRROR_SPELLS.get(skillId);
    }

    static ClearMindSpell clearMindSpell(int skillId) {
        return CLEAR_MIND_SPELLS.get(skillId);
    }

    static ResistElementalSpell resistElementalSpell(int skillId) {
        return RESIST_ELEMENTAL_SPELLS.get(skillId);
    }

    static ElementalProtectionSpell elementalProtectionSpell(int skillId) {
        return ELEMENTAL_PROTECTION_SPELLS.get(skillId);
    }

    static FireWeaponSpell fireWeaponSpell(int skillId) {
        return FIRE_WEAPON_SPELLS.get(skillId);
    }

    static WindShotSpell windShotSpell(int skillId) {
        return WIND_SHOT_SPELLS.get(skillId);
    }

    static FireBlessSpell fireBlessSpell(int skillId) {
        return FIRE_BLESS_SPELLS.get(skillId);
    }

    static StormEyeSpell stormEyeSpell(int skillId) {
        return STORM_EYE_SPELLS.get(skillId);
    }

    static StormShotSpell stormShotSpell(int skillId) {
        return STORM_SHOT_SPELLS.get(skillId);
    }

    static WindShackleSpell windShackleSpell(int skillId) {
        return WIND_SHACKLE_SPELLS.get(skillId);
    }

    static NaturesTouchSpell naturesTouchSpell(int skillId) {
        return NATURES_TOUCH_SPELLS.get(skillId);
    }

    static EarthBlessSpell earthBlessSpell(int skillId) {
        return EARTH_BLESS_SPELLS.get(skillId);
    }

    static IronSkinSpell ironSkinSpell(int skillId) {
        return IRON_SKIN_SPELLS.get(skillId);
    }

    static DragonSkinSpell dragonSkinSpell(int skillId) {
        return DRAGON_SKIN_SPELLS.get(skillId);
    }

    static AwakenSpell awakenSpell(int skillId) {
        return AWAKEN_SPELLS.get(skillId);
    }

    static BloodlustSpell bloodlustSpell(int skillId) {
        return BLOODLUST_SPELLS.get(skillId);
    }

    static FoeSlayerSpell foeSlayerSpell(int skillId) {
        return FOE_SLAYER_SPELLS.get(skillId);
    }

    static BurningSlashSpell burningSlashSpell(int skillId) {
        return BURNING_SLASH_SPELLS.get(skillId);
    }

    static GuardBrakeSpell guardBrakeSpell(int skillId) {
        return GUARD_BRAKE_SPELLS.get(skillId);
    }

    static ResistFearSpell resistFearSpell(int skillId) {
        return RESIST_FEAR_SPELLS.get(skillId);
    }

    static ExoticVitalizeSpell exoticVitalizeSpell(int skillId) {
        return EXOTIC_VITALIZE_SPELLS.get(skillId);
    }

    static AdditionalFireSpell additionalFireSpell(int skillId) {
        return ADDITIONAL_FIRE_SPELLS.get(skillId);
    }

    static WaterLifeSpell waterLifeSpell(int skillId) {
        return WATER_LIFE_SPELLS.get(skillId);
    }

    static AquaProtectSpell aquaProtectSpell(int skillId) {
        return AQUA_PROTECT_SPELLS.get(skillId);
    }

    static AreaSilenceSpell areaSilenceSpell(int skillId) {
        return AREA_SILENCE_SPELLS.get(skillId);
    }

    static AbilityBuffSpell abilityBuffSpell(int skillId) {
        return ABILITY_BUFF_SPELLS.get(skillId);
    }

    static HasteSpell hasteSpell(int skillId) {
        return HASTE_SPELLS.get(skillId);
    }

    static SelfHasteSpell selfHasteSpell(int skillId) {
        return SELF_HASTE_SPELLS.get(skillId);
    }

    static CombatBuffSpell combatBuffSpell(int skillId) {
        return COMBAT_BUFF_SPELLS.get(skillId);
    }

    static DamageProtectionSpell damageProtectionSpell(int skillId) {
        return DAMAGE_PROTECTION_SPELLS.get(skillId);
    }

    static MassTeleportSpell massTeleportSpell(int skillId) {
        return MASS_TELEPORT_SPELLS.get(skillId);
    }

    static MovementBuffSpell movementBuffSpell(int skillId) {
        return MOVEMENT_BUFF_SPELLS.get(skillId);
    }

    static PhysicalProcBuffSpell physicalProcBuffSpell(int skillId) {
        return PHYSICAL_PROC_BUFF_SPELLS.get(skillId);
    }

    static OtherNoEffectSpell otherNoEffectSpell(int skillId) {
        return OTHER_NO_EFFECT_SPELLS.get(skillId);
    }

    static DodgeBuffSpell dodgeBuffSpell(int skillId) {
        return DODGE_BUFF_SPELLS.get(skillId);
    }

    static RangedEvasionBuffSpell rangedEvasionBuffSpell(int skillId) {
        return RANGED_EVASION_BUFF_SPELLS.get(skillId);
    }

    static ItemWeaponEnchantSpell itemWeaponEnchantSpell(int skillId) {
        return ITEM_WEAPON_ENCHANT_SPELLS.get(skillId);
    }

    static ItemArmorEnchantSpell itemArmorEnchantSpell(int skillId) {
        return ITEM_ARMOR_ENCHANT_SPELLS.get(skillId);
    }

    static WeaponBuffSpell weaponBuffSpell(int skillId) {
        return WEAPON_BUFF_SPELLS.get(skillId);
    }

    static CancellationSpell cancellationSpell(int skillId) {
        return CANCELLATION_SPELLS.get(skillId);
    }

    static RegenSpell regenSpell(int skillId) {
        return REGEN_SPELLS.get(skillId);
    }

    static WeightSpell weightSpell(int skillId) {
        return WEIGHT_SPELLS.get(skillId);
    }

    static AttackSpell attackSpell(int skillId) {
        AttackSpell spell = ATTACK_SPELLS.get(skillId);
        if (spell != null) return spell;
        spell = FINAL_BURN_ATTACK_SPELLS.get(skillId);
        if (spell != null) return spell;
        ThunderGrabSpell thunderGrab = THUNDER_GRAB_SPELLS.get(skillId);
        if (thunderGrab != null) return thunderGrab.attackSpell();
        ConfusionSpell confusion = CONFUSION_SPELLS.get(skillId);
        if (confusion != null) return confusion.attackSpell();
        SmashSpell smash = SMASH_SPELLS.get(skillId);
        if (smash != null) return smash.attackSpell();
        MindBreakSpell mindBreak = MIND_BREAK_SPELLS.get(skillId);
        if (mindBreak != null) return mindBreak.attackSpell();
        BoneBreakSpell boneBreak = BONE_BREAK_SPELLS.get(skillId);
        if (boneBreak != null) return boneBreak.attackSpell();
        ArmBreakerSpell armBreaker = ARM_BREAKER_SPELLS.get(skillId);
        return armBreaker == null ? null : armBreaker.attackSpell();
    }

    static ThunderGrabSpell thunderGrabSpell(int skillId) {
        return THUNDER_GRAB_SPELLS.get(skillId);
    }

    static ConfusionSpell confusionSpell(int skillId) {
        return CONFUSION_SPELLS.get(skillId);
    }

    static SmashSpell smashSpell(int skillId) {
        return SMASH_SPELLS.get(skillId);
    }

    static MindBreakSpell mindBreakSpell(int skillId) {
        return MIND_BREAK_SPELLS.get(skillId);
    }

    static BoneBreakSpell boneBreakSpell(int skillId) {
        return BONE_BREAK_SPELLS.get(skillId);
    }

    static IllusionOgreSpell illusionOgreSpell(int skillId) {
        return ILLUSION_OGRE_SPELLS.get(skillId);
    }

    static IllusionLichSpell illusionLichSpell(int skillId) {
        return ILLUSION_LICH_SPELLS.get(skillId);
    }

    static IllusionAvatarSpell illusionAvatarSpell(int skillId) {
        return ILLUSION_AVATAR_SPELLS.get(skillId);
    }

    static CubeIgnitionSpell cubeIgnitionSpell(int skillId) {
        return CUBE_IGNITION_SPELLS.get(skillId);
    }

    static CubeQuakeSpell cubeQuakeSpell(int skillId) {
        return CUBE_QUAKE_SPELLS.get(skillId);
    }

    static CubeShockSpell cubeShockSpell(int skillId) {
        return CUBE_SHOCK_SPELLS.get(skillId);
    }

    static CubeHarmonySpell cubeHarmonySpell(int skillId) {
        return CUBE_HARMONY_SPELLS.get(skillId);
    }

    static PatienceSpell patienceSpell(int skillId) {
        return PATIENCE_SPELLS.get(skillId);
    }

    static PhantasmSpell phantasmSpell(int skillId) {
        return PHANTASM_SPELLS.get(skillId);
    }

    static ArmBreakerSpell armBreakerSpell(int skillId) {
        return ARM_BREAKER_SPELLS.get(skillId);
    }

    static DiamondGolemSpell diamondGolemSpell(int skillId) {
        return DIAMOND_GOLEM_SPELLS.get(skillId);
    }

    static InsightSpell insightSpell(int skillId) {
        return INSIGHT_SPELLS.get(skillId);
    }

    static PanicSpell panicSpell(int skillId) {
        return PANIC_SPELLS.get(skillId);
    }

    static JoyOfPainSpell joyOfPainSpell(int skillId) {
        return JOY_OF_PAIN_SPELLS.get(skillId);
    }

    static FreezeAttackSpell freezeAttackSpell(int skillId) {
        return FREEZE_ATTACK_SPELLS.get(skillId);
    }

    static AreaAttackSpell areaAttackSpell(int skillId) {
        return AREA_ATTACK_SPELLS.get(skillId);
    }

    static LineAttackSpell lineAttackSpell(int skillId) {
        return LINE_ATTACK_SPELLS.get(skillId);
    }

    static TurnUndeadSpell turnUndeadSpell(int skillId) {
        return TURN_UNDEAD_SPELLS.get(skillId);
    }

    static ManaDrainSpell manaDrainSpell(int skillId) {
        return MANA_DRAIN_SPELLS.get(skillId);
    }

    static BlindSpell blindSpell(int skillId) {
        return BLIND_SPELLS.get(skillId);
    }

    static WeaponBreakSpell weaponBreakSpell(int skillId) {
        return WEAPON_BREAK_SPELLS.get(skillId);
    }

    static WeaknessSpell weaknessSpell(int skillId) {
        return WEAKNESS_SPELLS.get(skillId);
    }

    static SilenceSpell silenceSpell(int skillId) {
        return SILENCE_SPELLS.get(skillId);
    }

    static DecayPotionSpell decayPotionSpell(int skillId) {
        return DECAY_POTION_SPELLS.get(skillId);
    }

    static PolluteWaterSpell polluteWaterSpell(int skillId) {
        return POLLUTE_WATER_SPELLS.get(skillId);
    }

    static StrikerGaleSpell strikerGaleSpell(int skillId) {
        return STRIKER_GALE_SPELLS.get(skillId);
    }

    static CounterDetectionSpell counterDetectionSpell(int skillId) {
        return COUNTER_DETECTION_SPELLS.get(skillId);
    }

    static DetectionSpell detectionSpell(int skillId) {
        return DETECTION_SPELLS.get(skillId);
    }

    static RevealWeaknessSpell revealWeaknessSpell(int skillId) {
        return REVEAL_WEAKNESS_SPELLS.get(skillId);
    }

    static MagicalWeaponSpell magicalWeaponSpell(int skillId) {
        return MAGICAL_WEAPON_SPELLS.get(skillId);
    }

    static CreateZombieSpell createZombieSpell(int skillId) {
        return CREATE_ZOMBIE_SPELLS.get(skillId);
    }

    static ResurrectionSpell resurrectionSpell(int skillId) {
        return RESURRECTION_SPELLS.get(skillId);
    }

    static MassSlowSpell massSlowSpell(int skillId) {
        return MASS_SLOW_SPELLS.get(skillId);
    }

    static EntangleSpell entangleSpell(int skillId) {
        return ENTANGLE_SPELLS.get(skillId);
    }

    static EraseMagicSpell eraseMagicSpell(int skillId) {
        return ERASE_MAGIC_SPELLS.get(skillId);
    }

    static ElementalSummonSpell elementalSummonSpell(int skillId) {
        return ELEMENTAL_SUMMON_SPELLS.get(skillId);
    }

    /** Exact {@code npc.cant_resurrect} values for every current PvE actor. */
    static boolean npcAllowsResurrection(int npcId) {
        return switch (npcId) {
            case 45601, 45584, 45610 -> false; // Death Knight and both Time Attack bosses.
            case 45262, 45207, 45289, 45259, 45278, 45322, 45390, 45380,
                    45182, 45812 -> true;
            default -> false; // Do not invent source data for unsupported actors.
        };
    }

    static SummonSpell summonSpell(int skillId) {
        return SUMMON_SPELLS.get(skillId);
    }

    static SummonChoice summonChoice(long selection) {
        if (selection < 0 || selection > Integer.MAX_VALUE) return null;
        return SUMMON_CHOICES.get((int) selection);
    }

    static Map<Integer, SummonChoice> summonChoices() {
        return SUMMON_CHOICES;
    }

    private static Map.Entry<Integer, SummonChoice> summon(
            int selection, int npcId, int requiredLevel, int petCost,
            boolean directChoice) {
        return Map.entry(selection, new SummonChoice(
                selection, npcId, requiredLevel, petCost, 0,
                directChoice, false));
    }

    private static Map.Entry<Integer, SummonChoice> summonSpecial(
            int selection, int npcId, int requiredLevel, int petCost) {
        return Map.entry(selection, new SummonChoice(
                selection, npcId, requiredLevel, petCost, 7,
                false, true));
    }

    /** Retained L1SummonInstance(Create Zombie) wizard/elf level bands. */
    static ZombieTemplate zombieTemplate(int characterType, int level) {
        int npcId = 45065;
        if (characterType == 3) {
            if (level >= 52) npcId = 81188;
            else if (level >= 48) npcId = 81187;
            else if (level >= 44) npcId = 81186;
            else if (level >= 40) npcId = 81185;
            else if (level >= 32) npcId = 81184;
            else if (level >= 24) npcId = 81183;
        } else if (characterType == 2 && level >= 48) {
            npcId = 81183;
        }
        return ZOMBIE_TEMPLATES.get(npcId);
    }

    static SleepSpell sleepSpell(int skillId) {
        return SLEEP_SPELLS.get(skillId);
    }

    static PolymorphSpell polymorphSpell(int skillId) {
        return POLYMORPH_SPELLS.get(skillId);
    }

    static BindSpell bindSpell(int skillId) {
        return BIND_SPELLS.get(skillId);
    }

    static PoisonSpell poisonSpell(int skillId) {
        return POISON_SPELLS.get(skillId);
    }

    static ParalysisSpell paralysisSpell(int skillId) {
        return PARALYSIS_SPELLS.get(skillId);
    }

    static TamingSpell tamingSpell(int skillId) {
        return TAMING_SPELLS.get(skillId);
    }

    static CurseRemovalSpell curseRemovalSpell(int skillId) {
        return CURSE_REMOVAL_SPELLS.get(skillId);
    }

    static InvisibilitySpell invisibilitySpell(int skillId) {
        return INVISIBILITY_SPELLS.get(skillId);
    }

    static EnchantVenomSpell enchantVenomSpell(int skillId) {
        return ENCHANT_VENOM_SPELLS.get(skillId);
    }

    static PoisonResistanceSpell poisonResistanceSpell(int skillId) {
        return POISON_RESISTANCE_SPELLS.get(skillId);
    }

    static BringStoneSpell bringStoneSpell(int skillId) {
        return BRING_STONE_SPELLS.get(skillId);
    }

    static StoneRefinement stoneRefinement(long sourceCatalogId) {
        return STONE_REFINEMENTS.get(sourceCatalogId);
    }

    static TrueTargetSpell trueTargetSpell(int skillId) {
        return TRUE_TARGET_SPELLS.get(skillId);
    }

    static RoyalAuraSpell royalAuraSpell(int skillId) {
        return ROYAL_AURA_SPELLS.get(skillId);
    }

    static CallClanSpell callClanSpell(int skillId) {
        return CALL_CLAN_SPELLS.get(skillId);
    }

    static RunClanSpell runClanSpell(int skillId) {
        return RUN_CLAN_SPELLS.get(skillId);
    }

    /** Exact classic {@code Point.isInScreen}: Manhattan 0..18 plus its 19 edge. */
    static boolean isInNativeScreen(int sourceX, int sourceY, int targetX, int targetY) {
        int distance = Math.abs(targetX - sourceX) + Math.abs(targetY - sourceY);
        if (distance > 19) return false;
        if (distance <= 18) return true;
        int edge = Math.abs(targetX - (sourceX - 18))
                + Math.abs(targetY - (sourceY - 18));
        return edge >= 19 && edge <= 52;
    }

    /** Retained glanceCheck equivalent over the imported Layer3 directional edges. */
    static boolean hasNativeLineOfSight(ArenaMapCatalog.Arena arena,
                                        int sourceX, int sourceY,
                                        int targetX, int targetY) {
        if (arena == null || !arena.nativeStandingFloor(sourceX, sourceY)
                || !arena.nativeStandingFloor(targetX, targetY)) {
            return false;
        }
        int currentX = sourceX;
        int currentY = sourceY;
        int deltaX = Math.abs(targetX - sourceX);
        int deltaY = Math.abs(targetY - sourceY);
        int stepX = Integer.compare(targetX, sourceX);
        int stepY = Integer.compare(targetY, sourceY);
        int error = deltaX - deltaY;
        while (currentX != targetX || currentY != targetY) {
            int nextX = currentX;
            int nextY = currentY;
            int doubled = error << 1;
            if (doubled > -deltaY) {
                error -= deltaY;
                nextX += stepX;
            }
            if (doubled < deltaX) {
                error += deltaX;
                nextY += stepY;
            }
            if (!arena.nativeCanStep(currentX, currentY, nextX, nextY)) return false;
            currentX = nextX;
            currentY = nextY;
        }
        return true;
    }

    /** Exact values from 380_db.sql table 角色_智力成長. */
    static int healDiceBonus(int intelligence) {
        if (NativeSourceHealingFormula.enabled()) {
            return NativeSourceHealingFormula.bonus(intelligence);
        }
        if (intelligence < 1 || intelligence > 127) {
            throw new IllegalArgumentException("intelligence outside native resource table");
        }
        if (intelligence <= 5) return -2;
        if (intelligence <= 8) return -1;
        if (intelligence <= 11) return 0;
        if (intelligence <= 14) return 1;
        if (intelligence <= 17) return 2;
        if (intelligence == 18) return 3;
        if (intelligence <= 25) return intelligence - 15;
        if (intelligence <= 28) return 10;
        return (intelligence - 7) / 2;
    }

    /** Retained {@code L1PcOriginal.resetOriginalMagicConsumeReduction}. */
    static int originalMagicConsumeReduction(int characterType,
                                              int originalIntelligence) {
        if (originalIntelligence < 0) {
            throw new IllegalArgumentException("original intelligence must not be negative");
        }
        return switch (characterType) {
            case 0 -> originalIntelligence >= 13 ? 2
                    : originalIntelligence >= 11 ? 1 : 0; // royal
            case 1 -> originalIntelligence >= 11 ? 2
                    : originalIntelligence >= 9 ? 1 : 0;  // knight
            case 2, 3, 5 -> 0;                            // elf/wizard/dragon knight
            case 4 -> originalIntelligence >= 15 ? 2
                    : originalIntelligence >= 13 ? 1 : 0; // dark elf
            case 6 -> originalIntelligence >= 15 ? 2
                    : originalIntelligence == 14 ? 1 : 0; // illusionist
            default -> throw new IllegalArgumentException(
                    "unsupported classic character type");
        };
    }

    record HealingSpell(int skillId, int row, int column, int actionId,
                        int effectGfx, int mpCost, int reuseDelayMillis,
                        int maximumRange, int baseDiceCount, int dieSides,
                        int attribute, int happenMessage, int failureMessage, String key) {
        public int actionId() {
            return ResourceDatabase.value(this.actionId, this.skillId, "HealingSpell", "actionId");
        }

        public int baseDiceCount() {
            return ResourceDatabase.value(this.baseDiceCount, this.skillId, "HealingSpell", "baseDiceCount");
        }

        public int dieSides() {
            return ResourceDatabase.value(this.dieSides, this.skillId, "HealingSpell", "dieSides");
        }

        public int effectGfx() {
            return ResourceDatabase.value(this.effectGfx, this.skillId, "HealingSpell", "effectGfx");
        }

        public int mpCost() {
            return ResourceDatabase.value(this.mpCost, this.skillId, "HealingSpell", "mpCost");
        }

        public int reuseDelayMillis() {
            return ResourceDatabase.value(this.reuseDelayMillis, this.skillId, "HealingSpell", "reuseDelayMillis");
        }

        HealingSpell {
            if (skillId != (row << 3) + column + 1 || row < 0 || row > 0xFF
                    || column < 0 || column > 7 || actionId < 0 || actionId > 0xFF
                    || effectGfx <= 0 || effectGfx > 0xFFFF || mpCost < 0
                    || reuseDelayMillis < 0 || maximumRange != -1
                    || baseDiceCount <= 0 || dieSides <= 0
                    || attribute < 0 || attribute > 0x0F
                    || happenMessage < 0 || happenMessage > 0xFFFF
                    || failureMessage < 0 || failureMessage > 0xFFFF
                    || key == null || key.isBlank()) {
                throw new IllegalArgumentException("invalid native healing spell metadata");
            }
        }

        int diceCount(int intelligence) {
            return Math.max(0, ResourceDatabase.value(this.baseDiceCount, this.skillId, "HealingSpell", "baseDiceCount") + healDiceBonus(intelligence));
        }

        int applyLawfulRevision(int rolledHealing, long lawful) {
            if (NativeSourceHealingFormula.enabled()) {
                return NativeSourceHealingFormula.lawful(rolledHealing, lawful);
            }
            if (rolledHealing < 0 || lawful < -32768 || lawful > 32767) {
                throw new IllegalArgumentException("invalid native healing inputs");
            }
            double revision = lawful > 0 ? 1.0 + lawful / 32768.0 : 1.0;
            return (int) (rolledHealing * revision);
        }

        /** Exact retained ordering: circle INT reduction, qualified helm, min-one, original INT. */
        int effectiveMpCost(int intelligence, int originalReduction,
                            boolean healingHelmEquipped) {
            if (intelligence < 1 || intelligence > 254
                    || originalReduction < 0 || originalReduction > 2) {
                throw new IllegalArgumentException("invalid native healing MP inputs");
            }
            int result = ResourceDatabase.value(this.mpCost, this.skillId, "HealingSpell", "mpCost");
            if (intelligence > 12 && skillId > 8 && skillId <= 80) result--;
            if (intelligence > 13 && skillId > 16 && skillId <= 80) result--;
            if (intelligence > 14 && skillId > 23 && skillId <= 80) result--;
            if (intelligence > 15 && skillId > 32 && skillId <= 80) result--;
            if (intelligence > 16 && skillId > 40 && skillId <= 80) result--;
            if (intelligence > 17 && skillId > 48 && skillId <= 80) result--;
            if (intelligence > 18 && skillId > 56 && skillId <= 80) result--;
            if (healingHelmEquipped
                    && (skillId == BASIC_HEAL || skillId == EXTRA_HEAL)) {
                result >>= 1;
            }
            if (ResourceDatabase.value(this.mpCost, this.skillId, "HealingSpell", "mpCost") > 0) result = Math.max(result, 1);
            return Math.max(0, result - originalReduction);
        }

        /** Retained TYPE_HEAL target rule: undead1 is damaged; undead3 is unaffected. */
        int npcHpDelta(int undeadType, int healingAmount) {
            if (undeadType < 0 || undeadType > 3 || healingAmount < 0) {
                throw new IllegalArgumentException("invalid native healing target inputs");
            }
            if (undeadType == 1) return -healingAmount;
            if (undeadType == 3) return 0;
            return healingAmount;
        }
    }

    record GroupHealingSpell(int skillId, int row, int column, int actionId,
                             int effectGfx, int mpCost, int hpCost,
                             int reuseDelayMillis, int area,
                             int baseDiceCount, int dieSides, String key) {
        public int actionId() {
            return ResourceDatabase.value(this.actionId, this.skillId, "GroupHealingSpell", "actionId");
        }

        public int baseDiceCount() {
            return ResourceDatabase.value(this.baseDiceCount, this.skillId, "GroupHealingSpell", "baseDiceCount");
        }

        public int dieSides() {
            return ResourceDatabase.value(this.dieSides, this.skillId, "GroupHealingSpell", "dieSides");
        }

        public int effectGfx() {
            return ResourceDatabase.value(this.effectGfx, this.skillId, "GroupHealingSpell", "effectGfx");
        }

        public int hpCost() {
            return ResourceDatabase.value(this.hpCost, this.skillId, "GroupHealingSpell", "hpCost");
        }

        public int mpCost() {
            return ResourceDatabase.value(this.mpCost, this.skillId, "GroupHealingSpell", "mpCost");
        }

        public int reuseDelayMillis() {
            return ResourceDatabase.value(this.reuseDelayMillis, this.skillId, "GroupHealingSpell", "reuseDelayMillis");
        }

        GroupHealingSpell {
            if (skillId != (row << 3) + column + 1 || row < 0 || row > 0xFF
                    || column < 0 || column > 7 || actionId < 0 || actionId > 0xFF
                    || effectGfx <= 0 || effectGfx > 0xFFFF || mpCost < 0
                    || hpCost < 0 || reuseDelayMillis < 0 || area != -1
                    || baseDiceCount <= 0 || dieSides <= 0
                    || key == null || key.isBlank()) {
                throw new IllegalArgumentException(
                        "invalid native group-healing spell metadata");
            }
        }

        int diceCount(int intelligence) {
            return Math.max(0, ResourceDatabase.value(this.baseDiceCount, this.skillId, "GroupHealingSpell", "baseDiceCount") + healDiceBonus(intelligence));
        }

        int rollHealing(int intelligence, IntUnaryOperator dieRoller) {
            if (dieRoller == null) {
                throw new IllegalArgumentException("missing native healing die roller");
            }
            int result = 0;
            for (int index = 0; index < diceCount(intelligence); index++) {
                int roll = dieRoller.applyAsInt(dieSides);
                if (roll < 1 || roll > dieSides) {
                    throw new IllegalArgumentException("invalid native healing die roll");
                }
                result = Math.addExact(result, roll);
            }
            return result;
        }

        int applyLawfulRevision(int rolledHealing, long lawful) {
            if (NativeSourceHealingFormula.enabled()) {
                return NativeSourceHealingFormula.lawful(rolledHealing, lawful);
            }
            if (rolledHealing < 0 || lawful < -32768 || lawful > 32767) {
                throw new IllegalArgumentException("invalid native group-healing inputs");
            }
            double revision = lawful > 0 ? 1.0 + lawful / 32768.0 : 1.0;
            return (int) (rolledHealing * revision);
        }

        int effectiveMpCost(int intelligence, int originalReduction) {
            if (intelligence < 1 || intelligence > 254
                    || originalReduction < 0 || originalReduction > 2) {
                throw new IllegalArgumentException(
                        "invalid native group-healing MP inputs");
            }
            int result = ResourceDatabase.value(this.mpCost, this.skillId, "GroupHealingSpell", "mpCost");
            if (intelligence > 12 && skillId > 8 && skillId <= 80) result--;
            if (intelligence > 13 && skillId > 16 && skillId <= 80) result--;
            if (intelligence > 14 && skillId > 23 && skillId <= 80) result--;
            if (intelligence > 15 && skillId > 32 && skillId <= 80) result--;
            if (intelligence > 16 && skillId > 40 && skillId <= 80) result--;
            if (intelligence > 17 && skillId > 48 && skillId <= 80) result--;
            if (intelligence > 18 && skillId > 56 && skillId <= 80) result--;
            if (ResourceDatabase.value(this.mpCost, this.skillId, "GroupHealingSpell", "mpCost") > 0) result = Math.max(result, 1);
            return Math.max(0, result - originalReduction);
        }
    }

    record NaturesBlessingSpell(
            int skillId, int row, int column, int level, int number,
            int actionId, int effectGfx, int mpCost, int reuseDelayMillis,
            int targetTo, int area, int baseDiceCount, int dieSides,
            int attribute, int type, int nativeId,
            boolean counterMagicBypass, String key) {
        public int actionId() {
            return ResourceDatabase.value(this.actionId, this.skillId, "NaturesBlessingSpell", "actionId");
        }

        public int baseDiceCount() {
            return ResourceDatabase.value(this.baseDiceCount, this.skillId, "NaturesBlessingSpell", "baseDiceCount");
        }

        public int dieSides() {
            return ResourceDatabase.value(this.dieSides, this.skillId, "NaturesBlessingSpell", "dieSides");
        }

        public int effectGfx() {
            return ResourceDatabase.value(this.effectGfx, this.skillId, "NaturesBlessingSpell", "effectGfx");
        }

        public int mpCost() {
            return ResourceDatabase.value(this.mpCost, this.skillId, "NaturesBlessingSpell", "mpCost");
        }

        public int reuseDelayMillis() {
            return ResourceDatabase.value(this.reuseDelayMillis, this.skillId, "NaturesBlessingSpell", "reuseDelayMillis");
        }

        NaturesBlessingSpell {
            if (skillId != NATURES_BLESSING
                    || skillId != (row << 3) + column + 1
                    || row != 20 || column != 3 || level != 21 || number != 3
                    || actionId != 19 || effectGfx != 2_244 || mpCost != 30
                    || reuseDelayMillis != 300 || targetTo != (8 | 16)
                    || area != -1 || baseDiceCount != 10 || dieSides != 12
                    || attribute != ATTRIBUTE_WATER || type != 16
                    || nativeId != 8 || !counterMagicBypass
                    || key == null || key.isBlank()) {
                throw new IllegalArgumentException(
                        "invalid native Nature's Blessing metadata");
            }
        }

        int diceCount(int intelligence) {
            return Math.max(0, ResourceDatabase.value(this.baseDiceCount, this.skillId, "NaturesBlessingSpell", "baseDiceCount") + healDiceBonus(intelligence));
        }

        int rollHealing(int intelligence, IntUnaryOperator dieRoller) {
            if (dieRoller == null) {
                throw new IllegalArgumentException(
                        "missing native Nature's Blessing die roller");
            }
            int result = 0;
            for (int index = 0; index < diceCount(intelligence); index++) {
                int roll = dieRoller.applyAsInt(dieSides);
                if (roll < 1 || roll > dieSides) {
                    throw new IllegalArgumentException(
                            "invalid native Nature's Blessing die roll");
                }
                result = Math.addExact(result, roll);
            }
            return result;
        }

        int applyLawfulRevision(int rolledHealing, long lawful) {
            if (NativeSourceHealingFormula.enabled()) {
                return NativeSourceHealingFormula.lawful(rolledHealing, lawful);
            }
            if (rolledHealing < 0 || lawful < -32768 || lawful > 32767) {
                throw new IllegalArgumentException(
                        "invalid native Nature's Blessing healing inputs");
            }
            double revision = lawful > 0 ? 1.0 + lawful / 32768.0 : 1.0;
            return (int) (rolledHealing * revision);
        }

        int effectiveMpCost(int originalReduction) {
            if (originalReduction < 0 || originalReduction > 2) {
                throw new IllegalArgumentException(
                        "invalid Nature's Blessing original INT reduction");
            }
            return Math.max(0, ResourceDatabase.value(this.mpCost, this.skillId, "NaturesBlessingSpell", "mpCost") - originalReduction);
        }
    }

    record LightSpell(int skillId, int row, int column, int actionId,
                      int effectGfx, int mpCost, int reuseDelayMillis,
                      int durationSeconds, int lightRange, int soundId,
                      String key) {
        public int actionId() {
            return ResourceDatabase.value(this.actionId, this.skillId, "LightSpell", "actionId");
        }

        public int durationSeconds() {
            return ResourceDatabase.value(this.durationSeconds, this.skillId, "LightSpell", "durationSeconds");
        }

        public int effectGfx() {
            return ResourceDatabase.value(this.effectGfx, this.skillId, "LightSpell", "effectGfx");
        }

        public int mpCost() {
            return ResourceDatabase.value(this.mpCost, this.skillId, "LightSpell", "mpCost");
        }

        public int reuseDelayMillis() {
            return ResourceDatabase.value(this.reuseDelayMillis, this.skillId, "LightSpell", "reuseDelayMillis");
        }

        LightSpell {
            if (skillId != (row << 3) + column + 1 || row < 0 || row > 0xFF
                    || column < 0 || column > 7 || actionId < 0 || actionId > 0xFF
                    || effectGfx <= 0 || effectGfx > 0xFFFF || mpCost < 0
                    || reuseDelayMillis < 0 || durationSeconds <= 0
                    || durationSeconds > 0xFFFF || lightRange <= 0 || lightRange > 0xFF
                    || soundId <= 0 || soundId > 0xFFFF
                    || key == null || key.isBlank()) {
                throw new IllegalArgumentException("invalid native light spell metadata");
            }
        }
    }

    enum Ability { STRENGTH, DEXTERITY }

    enum Targeting { SELF_SELECTOR, PLAYER_OBJECT }

    enum RegenEffect { MEDITATION, CONCENTRATION }

    enum WeightEffect { DECREASE_WEIGHT, REGENERATION_BYPASS }

    record WeightSpell(int skillId, int row, int column, int actionId,
                       int effectGfx, int mpCost, int hpCost,
                       int reuseDelayMillis, int durationSeconds,
                       long itemConsumeId, int itemConsumeCount,
                       WeightEffect effect, String key) {
        public long itemConsumeId() {
            return CombatDatabase.longValue(this.itemConsumeId, this.skillId, "itemConsumeId");
        }

        public int actionId() {
            return ResourceDatabase.value(this.actionId, this.skillId, "WeightSpell", "actionId");
        }

        public int durationSeconds() {
            return ResourceDatabase.value(this.durationSeconds, this.skillId, "WeightSpell", "durationSeconds");
        }

        public int effectGfx() {
            return ResourceDatabase.value(this.effectGfx, this.skillId, "WeightSpell", "effectGfx");
        }

        public int hpCost() {
            return ResourceDatabase.value(this.hpCost, this.skillId, "WeightSpell", "hpCost");
        }

        public int itemConsumeCount() {
            return ResourceDatabase.value(this.itemConsumeCount, this.skillId, "WeightSpell", "itemConsumeCount");
        }

        public int mpCost() {
            return ResourceDatabase.value(this.mpCost, this.skillId, "WeightSpell", "mpCost");
        }

        public int reuseDelayMillis() {
            return ResourceDatabase.value(this.reuseDelayMillis, this.skillId, "WeightSpell", "reuseDelayMillis");
        }

        WeightSpell {
            if (skillId != (row << 3) + column + 1 || row < 0 || row > 0xFF
                    || column < 0 || column > 7 || actionId < 0 || actionId > 0xFF
                    || effectGfx <= 0 || effectGfx > 0xFFFF || mpCost < 0
                    || hpCost < 0 || reuseDelayMillis < 0 || durationSeconds <= 0
                    || durationSeconds > 0xFFFF || itemConsumeId < 0
                    || itemConsumeId > 0xFFFF_FFFFL || itemConsumeCount < 0
                    || (itemConsumeId == 0) != (itemConsumeCount == 0)
                    || effect == null || key == null || key.isBlank()) {
                throw new IllegalArgumentException("invalid native weight-spell metadata");
            }
        }
    }

    record ShieldSpell(int skillId, int row, int column, int actionId,
                       int effectGfx, int mpCost, int hpCost,
                       int reuseDelayMillis,
                       int durationSeconds, int iconType,
                       int armorClassModifier,
                       boolean originalIntelligenceReduction,
                       Targeting targeting, int happenMessage, int stopMessage,
                       int failureMessage, String key) {
        public int actionId() {
            return ResourceDatabase.value(this.actionId, this.skillId, "ShieldSpell", "actionId");
        }

        public int durationSeconds() {
            return ResourceDatabase.value(this.durationSeconds, this.skillId, "ShieldSpell", "durationSeconds");
        }

        public int effectGfx() {
            return ResourceDatabase.value(this.effectGfx, this.skillId, "ShieldSpell", "effectGfx");
        }

        public int hpCost() {
            return ResourceDatabase.value(this.hpCost, this.skillId, "ShieldSpell", "hpCost");
        }

        public int iconType() {
            return ResourceDatabase.value(this.iconType, this.skillId, "ShieldSpell", "iconType");
        }

        public int mpCost() {
            return ResourceDatabase.value(this.mpCost, this.skillId, "ShieldSpell", "mpCost");
        }

        public int reuseDelayMillis() {
            return ResourceDatabase.value(this.reuseDelayMillis, this.skillId, "ShieldSpell", "reuseDelayMillis");
        }

        ShieldSpell {
            if (skillId != (row << 3) + column + 1 || row < 0 || row > 0xFF
                    || column < 0 || column > 7 || actionId < 0 || actionId > 0xFF
                    || effectGfx <= 0 || effectGfx > 0xFFFF || mpCost < 0
                    || hpCost < 0
                    || reuseDelayMillis < 0 || durationSeconds <= 0
                    || durationSeconds > 0xFFFF || iconType <= 0 || iconType > 0xFF
                    || armorClassModifier >= 0 || armorClassModifier < -127
                    || targeting == null || happenMessage < 0
                    || happenMessage > 0xFFFF || stopMessage < 0
                    || stopMessage > 0xFFFF || failureMessage < 0
                    || failureMessage > 0xFFFF
                    || key == null || key.isBlank()) {
                throw new IllegalArgumentException("invalid native shield spell metadata");
            }
        }

        int effectiveMpCost(int originalReduction) {
            if (originalReduction < 0 || originalReduction > 2) {
                throw new IllegalArgumentException(
                        "invalid native shield-spell original INT reduction");
            }
            return originalIntelligenceReduction
                    ? Math.max(0, ResourceDatabase.value(this.mpCost, this.skillId, "ShieldSpell", "mpCost") - originalReduction) : ResourceDatabase.value(this.mpCost, this.skillId, "ShieldSpell", "mpCost");
        }
    }

    record CounterMagicSpell(int skillId, int row, int column, int actionId,
                             int effectGfx, int mpCost, int reuseDelayMillis,
                             int durationSeconds, long itemConsumeId,
                             int itemConsumeCount, int consumeEffectGfx,
                             String key) {
        public long itemConsumeId() {
            return CombatDatabase.longValue(this.itemConsumeId, this.skillId, "itemConsumeId");
        }

        public int actionId() {
            return ResourceDatabase.value(this.actionId, this.skillId, "CounterMagicSpell", "actionId");
        }

        public int consumeEffectGfx() {
            return ResourceDatabase.value(this.consumeEffectGfx, this.skillId, "CounterMagicSpell", "consumeEffectGfx");
        }

        public int durationSeconds() {
            return ResourceDatabase.value(this.durationSeconds, this.skillId, "CounterMagicSpell", "durationSeconds");
        }

        public int effectGfx() {
            return ResourceDatabase.value(this.effectGfx, this.skillId, "CounterMagicSpell", "effectGfx");
        }

        public int itemConsumeCount() {
            return ResourceDatabase.value(this.itemConsumeCount, this.skillId, "CounterMagicSpell", "itemConsumeCount");
        }

        public int mpCost() {
            return ResourceDatabase.value(this.mpCost, this.skillId, "CounterMagicSpell", "mpCost");
        }

        public int reuseDelayMillis() {
            return ResourceDatabase.value(this.reuseDelayMillis, this.skillId, "CounterMagicSpell", "reuseDelayMillis");
        }

        CounterMagicSpell {
            if (skillId != (row << 3) + column + 1 || row < 0 || row > 0xFF
                    || column < 0 || column > 7 || actionId < 0 || actionId > 0xFF
                    || effectGfx <= 0 || effectGfx > 0xFFFF || mpCost < 0
                    || reuseDelayMillis < 0 || durationSeconds <= 0
                    || durationSeconds > 0xFFFF || itemConsumeId <= 0
                    || itemConsumeId > 0xFFFF_FFFFL || itemConsumeCount <= 0
                    || consumeEffectGfx < 0 || consumeEffectGfx > 0xFFFF
                    || key == null || key.isBlank()) {
                throw new IllegalArgumentException("invalid native Counter Magic metadata");
            }
        }

        int effectiveMpCost(int originalReduction) {
            if (originalReduction < 0 || originalReduction > 2) {
                throw new IllegalArgumentException(
                        "invalid native Counter Magic original INT reduction");
            }
            return Math.max(0, ResourceDatabase.value(this.mpCost, this.skillId, "CounterMagicSpell", "mpCost") - originalReduction);
        }

    }

    record AbsoluteBarrierSpell(int skillId, int row, int column, int actionId,
                                int effectGfx, int mpCost, int reuseDelayMillis,
                                int durationSeconds, long itemConsumeId,
                                int itemConsumeCount, String key) {
        public long itemConsumeId() {
            return CombatDatabase.longValue(this.itemConsumeId, this.skillId, "itemConsumeId");
        }

        public int actionId() {
            return ResourceDatabase.value(this.actionId, this.skillId, "AbsoluteBarrierSpell", "actionId");
        }

        public int durationSeconds() {
            return ResourceDatabase.value(this.durationSeconds, this.skillId, "AbsoluteBarrierSpell", "durationSeconds");
        }

        public int effectGfx() {
            return ResourceDatabase.value(this.effectGfx, this.skillId, "AbsoluteBarrierSpell", "effectGfx");
        }

        public int itemConsumeCount() {
            return ResourceDatabase.value(this.itemConsumeCount, this.skillId, "AbsoluteBarrierSpell", "itemConsumeCount");
        }

        public int mpCost() {
            return ResourceDatabase.value(this.mpCost, this.skillId, "AbsoluteBarrierSpell", "mpCost");
        }

        public int reuseDelayMillis() {
            return ResourceDatabase.value(this.reuseDelayMillis, this.skillId, "AbsoluteBarrierSpell", "reuseDelayMillis");
        }

        AbsoluteBarrierSpell {
            if (skillId != (row << 3) + column + 1 || row < 0 || row > 0xFF
                    || column < 0 || column > 7 || actionId < 0 || actionId > 0xFF
                    || effectGfx <= 0 || effectGfx > 0xFFFF || mpCost < 0
                    || reuseDelayMillis < 0 || durationSeconds <= 0
                    || durationSeconds > 0xFFFF || itemConsumeId <= 0
                    || itemConsumeId > 0xFFFF_FFFFL || itemConsumeCount <= 0
                    || key == null || key.isBlank()) {
                throw new IllegalArgumentException("invalid native Absolute Barrier metadata");
            }
        }

        int effectiveMpCost(int intelligence, int originalReduction) {
            if (intelligence < 1 || intelligence > 254
                    || originalReduction < 0 || originalReduction > 2) {
                throw new IllegalArgumentException(
                        "invalid native Absolute Barrier MP inputs");
            }
            int result = ResourceDatabase.value(this.mpCost, this.skillId, "AbsoluteBarrierSpell", "mpCost");
            if (intelligence > 12) result--;
            if (intelligence > 13) result--;
            if (intelligence > 14) result--;
            if (intelligence > 15) result--;
            if (intelligence > 16) result--;
            if (intelligence > 17) result--;
            if (intelligence > 18) result--;
            return Math.max(1, result - originalReduction);
        }
    }

    record AdvanceSpiritSpell(int skillId, int row, int column, int actionId,
                              int effectGfx, int mpCost, int reuseDelayMillis,
                              int durationSeconds, int maximumRange, int targetTo,
                              int baseMaximumDivisor, int failureMessage,
                              String key) {
        public int actionId() {
            return ResourceDatabase.value(this.actionId, this.skillId, "AdvanceSpiritSpell", "actionId");
        }

        public int durationSeconds() {
            return ResourceDatabase.value(this.durationSeconds, this.skillId, "AdvanceSpiritSpell", "durationSeconds");
        }

        public int effectGfx() {
            return ResourceDatabase.value(this.effectGfx, this.skillId, "AdvanceSpiritSpell", "effectGfx");
        }

        public int mpCost() {
            return ResourceDatabase.value(this.mpCost, this.skillId, "AdvanceSpiritSpell", "mpCost");
        }

        public int reuseDelayMillis() {
            return ResourceDatabase.value(this.reuseDelayMillis, this.skillId, "AdvanceSpiritSpell", "reuseDelayMillis");
        }

        AdvanceSpiritSpell {
            if (skillId != (row << 3) + column + 1
                    || skillId != ADVANCE_SPIRIT
                    || row < 0 || row > 0xFF || column < 0 || column > 7
                    || actionId < 0 || actionId > 0xFF
                    || effectGfx <= 0 || effectGfx > 0xFFFF || mpCost < 0
                    || reuseDelayMillis < 0 || durationSeconds <= 0
                    || durationSeconds > 0xFFFF || maximumRange != -1
                    || targetTo != 1 || baseMaximumDivisor != 5
                    || failureMessage <= 0 || failureMessage > 0xFFFF
                    || key == null || key.isBlank()) {
                throw new IllegalArgumentException(
                        "invalid native Advance Spirit metadata");
            }
        }

        int maximumBonus(int baseMaximum) {
            int maximum = NumericBreakthrough.enabled()
                    ? NumericBreakthrough.MAX_VITAL : 0x7FFF;
            if (baseMaximum <= 0 || baseMaximum > maximum) {
                throw new IllegalArgumentException(
                        "invalid native Advance Spirit base maximum");
            }
            return baseMaximum / baseMaximumDivisor;
        }

        int maximumWithBonus(int adjustedMaximum, int sourceBaseMaximum) {
            int maximum = NumericBreakthrough.enabled()
                    ? NumericBreakthrough.MAX_VITAL : 0x7FFF;
            if (adjustedMaximum <= 0 || adjustedMaximum > maximum) {
                throw new IllegalArgumentException(
                        "invalid native Advance Spirit adjusted maximum");
            }
            return NumericBreakthrough.clampVital(Math.addExact(
                    (long) adjustedMaximum,
                    (long) maximumBonus(sourceBaseMaximum)));
        }

        int effectiveMpCost(int intelligence, int originalReduction) {
            if (intelligence < 1 || intelligence > 254
                    || originalReduction < 0 || originalReduction > 2) {
                throw new IllegalArgumentException(
                        "invalid native Advance Spirit MP inputs");
            }
            int result = ResourceDatabase.value(this.mpCost, this.skillId, "AdvanceSpiritSpell", "mpCost");
            if (intelligence > 12) result--;
            if (intelligence > 13) result--;
            if (intelligence > 14) result--;
            if (intelligence > 15) result--;
            if (intelligence > 16) result--;
            if (intelligence > 17) result--;
            if (intelligence > 18) result--;
            return Math.max(1, result - originalReduction);
        }
    }

    record ShockStunSpell(
            int skillId, int row, int column, int databaseActionId,
            int impactGfx, int effectNpcId, int effectNpcGfx,
            int mpCost, int reuseDelayMillis, int maximumRange,
            int baseDamage, int diceCount, int dieSides, int attribute,
            int probabilityHigh, int probabilityEqual, int probabilityLow,
            boolean includeMagicHit, boolean includeMagicLevel,
            double magicResistanceMultiplier, int bossModifier,
            int baseStunSeconds, int randomStunSeconds,
            int firstRequiredWalkAction, int secondRequiredWalkAction,
            int failureMessage, String key) {
        public int baseDamage() {
            return ResourceDatabase.value(this.baseDamage, this.skillId, "ShockStunSpell", "baseDamage");
        }

        public int baseStunSeconds() {
            return ResourceDatabase.value(this.baseStunSeconds, this.skillId, "ShockStunSpell", "baseStunSeconds");
        }

        public int diceCount() {
            return ResourceDatabase.value(this.diceCount, this.skillId, "ShockStunSpell", "diceCount");
        }

        public int dieSides() {
            return ResourceDatabase.value(this.dieSides, this.skillId, "ShockStunSpell", "dieSides");
        }

        public int effectNpcGfx() {
            return ResourceDatabase.value(this.effectNpcGfx, this.skillId, "ShockStunSpell", "effectNpcGfx");
        }

        public int impactGfx() {
            return ResourceDatabase.value(this.impactGfx, this.skillId, "ShockStunSpell", "impactGfx");
        }

        public int mpCost() {
            return ResourceDatabase.value(this.mpCost, this.skillId, "ShockStunSpell", "mpCost");
        }

        public int randomStunSeconds() {
            return ResourceDatabase.value(this.randomStunSeconds, this.skillId, "ShockStunSpell", "randomStunSeconds");
        }

        public int reuseDelayMillis() {
            return ResourceDatabase.value(this.reuseDelayMillis, this.skillId, "ShockStunSpell", "reuseDelayMillis");
        }

        ShockStunSpell {
            if (skillId != (row << 3) + column + 1 || skillId != SHOCK_STUN
                    || row < 0 || row > 0xFF || column < 0 || column > 7
                    || databaseActionId < 0 || databaseActionId > 0xFF
                    || impactGfx <= 0 || impactGfx > 0xFFFF
                    || effectNpcId <= 0 || effectNpcGfx <= 0 || effectNpcGfx > 0xFFFF
                    || mpCost <= 0 || reuseDelayMillis < 0 || maximumRange != 1
                    || baseDamage < 0 || diceCount < 0 || dieSides <= 0
                    || attribute < 0
                    || probabilityHigh < 0 || probabilityHigh > 100
                    || probabilityEqual < 0 || probabilityEqual > 100
                    || probabilityLow < 0 || probabilityLow > 100
                    || !Double.isFinite(magicResistanceMultiplier)
                    || magicResistanceMultiplier < 0.0D || bossModifier <= 0
                    || baseStunSeconds <= 0 || randomStunSeconds < 0
                    || firstRequiredWalkAction != 50
                    || secondRequiredWalkAction != 54
                    || failureMessage <= 0 || failureMessage > 0xFFFF
                    || key == null || key.isBlank()) {
                throw new IllegalArgumentException("invalid native Shock Stun metadata");
            }
        }

        boolean supportsWalkAction(int walkAction) {
            return walkAction == firstRequiredWalkAction
                    || walkAction == secondRequiredWalkAction;
        }

        int effectiveMpCost(int effectiveIntelligence, int originalReduction) {
            if (effectiveIntelligence < 1 || effectiveIntelligence > 254
                    || originalReduction < 0 || originalReduction > 2) {
                throw new IllegalArgumentException("invalid Shock Stun MP inputs");
            }
            int result = ResourceDatabase.value(this.mpCost, this.skillId, "ShockStunSpell", "mpCost");
            if (effectiveIntelligence > 12) {
                result -= effectiveIntelligence - 12;
            }
            result = Math.max(result, 1);
            return Math.max(0, result - originalReduction);
        }

        int probability(int casterLevel, int targetLevel, int magicHit,
                        int targetMagicResistance, int targetStunResistance,
                        int stunSuccess, boolean targetBoss) {
            if (casterLevel <= 0 || targetLevel <= 0 || magicHit < 0
                    || targetMagicResistance < 0
                    || targetStunResistance < -128 || targetStunResistance > 127) {
                throw new IllegalArgumentException("invalid Shock Stun probability inputs");
            }
            double result = casterLevel > targetLevel ? probabilityHigh
                    : casterLevel == targetLevel ? probabilityEqual : probabilityLow;
            if (includeMagicHit) result += magicHit;
            if (includeMagicLevel) {
                throw new IllegalStateException(
                        "selected Shock Stun row must not include magic level");
            }
            result -= targetMagicResistance * magicResistanceMultiplier;
            result -= targetStunResistance;
            result = Math.max(1.0D, Math.min(100.0D, result));
            result = Math.max(1.0D, Math.min(100.0D, result + stunSuccess));
            int capped = Math.max(1, Math.min(90, (int) result));
            return targetBoss ? Math.max(1, capped / bossModifier) : capped;
        }

        int rollStunSeconds(IntUnaryOperator oneBasedRoll) {
            if (oneBasedRoll == null) {
                throw new IllegalArgumentException("Shock Stun duration roll is required");
            }
            int random = oneBasedRoll.applyAsInt(randomStunSeconds + 1) - 1;
            if (random < 0 || random > randomStunSeconds) {
                throw new IllegalArgumentException("Shock Stun duration roll is outside range");
            }
            return baseStunSeconds + random;
        }

        int maximumStunSeconds() {
            return baseStunSeconds + randomStunSeconds;
        }
    }

    record ArmorBreakSpell(
            int skillId, int row, int column, int actionId, int effectGfx,
            int mpCost, int hpCost, int reuseDelayMillis, int maximumRange,
            int durationSeconds,
            int probabilityHigh, int probabilityEqual, int probabilityLow,
            int bossModifier, long itemConsumeId, int itemConsumeCount,
            int failureMessage, String key) {
        public long itemConsumeId() {
            return CombatDatabase.longValue(this.itemConsumeId, this.skillId, "itemConsumeId");
        }

        public int actionId() {
            return ResourceDatabase.value(this.actionId, this.skillId, "ArmorBreakSpell", "actionId");
        }

        public int durationSeconds() {
            return ResourceDatabase.value(this.durationSeconds, this.skillId, "ArmorBreakSpell", "durationSeconds");
        }

        public int effectGfx() {
            return ResourceDatabase.value(this.effectGfx, this.skillId, "ArmorBreakSpell", "effectGfx");
        }

        public int hpCost() {
            return ResourceDatabase.value(this.hpCost, this.skillId, "ArmorBreakSpell", "hpCost");
        }

        public int itemConsumeCount() {
            return ResourceDatabase.value(this.itemConsumeCount, this.skillId, "ArmorBreakSpell", "itemConsumeCount");
        }

        public int mpCost() {
            return ResourceDatabase.value(this.mpCost, this.skillId, "ArmorBreakSpell", "mpCost");
        }

        public int reuseDelayMillis() {
            return ResourceDatabase.value(this.reuseDelayMillis, this.skillId, "ArmorBreakSpell", "reuseDelayMillis");
        }

        ArmorBreakSpell {
            if (skillId != (row << 3) + column + 1 || skillId != ARMOR_BREAK
                    || row != 13 || column != 7 || actionId != 19
                    || effectGfx != 3400 || mpCost != 40 || hpCost != 30
                    || reuseDelayMillis != 2_000 || maximumRange != 1
                    || durationSeconds != 8
                    || probabilityHigh != 50 || probabilityEqual != 40
                    || probabilityLow != 30 || bossModifier != 0
                    || itemConsumeId != InventoryCatalog.SECOND_LEVEL_DARK_STONE
                    || itemConsumeCount != 2 || failureMessage != 280
                    || key == null || key.isBlank()) {
                throw new IllegalArgumentException(
                        "invalid native Armor Break metadata");
            }
        }

        int effectiveMpCost(int originalReduction) {
            if (originalReduction < 0 || originalReduction > 2) {
                throw new IllegalArgumentException("invalid Armor Break MP inputs");
            }
            return Math.max(0, Math.max(ResourceDatabase.value(this.mpCost, this.skillId, "ArmorBreakSpell", "mpCost"), 1) - originalReduction);
        }

        int probability(int casterLevel, int targetLevel, boolean bossImmune) {
            return BattleArmorBreak.probability(
                    casterLevel, targetLevel,
                    probabilityHigh, probabilityEqual, probabilityLow,
                    bossImmune);
        }
    }

    record ReductionArmorSpell(
            int skillId, int row, int column, int actionId, int effectGfx,
            int mpCost, int hpCost, int reuseDelayMillis, int durationSeconds,
            int levelThreshold, int levelDivisor,
            int playerPhysicalBaseReduction, int otherBaseReduction,
            String key) {
        public int actionId() {
            return ResourceDatabase.value(this.actionId, this.skillId, "ReductionArmorSpell", "actionId");
        }

        public int durationSeconds() {
            return ResourceDatabase.value(this.durationSeconds, this.skillId, "ReductionArmorSpell", "durationSeconds");
        }

        public int effectGfx() {
            return ResourceDatabase.value(this.effectGfx, this.skillId, "ReductionArmorSpell", "effectGfx");
        }

        public int hpCost() {
            return ResourceDatabase.value(this.hpCost, this.skillId, "ReductionArmorSpell", "hpCost");
        }

        public int mpCost() {
            return ResourceDatabase.value(this.mpCost, this.skillId, "ReductionArmorSpell", "mpCost");
        }

        public int reuseDelayMillis() {
            return ResourceDatabase.value(this.reuseDelayMillis, this.skillId, "ReductionArmorSpell", "reuseDelayMillis");
        }

        ReductionArmorSpell {
            if (skillId != (row << 3) + column + 1
                    || skillId != REDUCTION_ARMOR
                    || row < 0 || row > 0xFF || column < 0 || column > 7
                    || actionId < 0 || actionId > 0xFF
                    || effectGfx <= 0 || effectGfx > 0xFFFF
                    || mpCost <= 0 || hpCost <= 0 || reuseDelayMillis < 0
                    || durationSeconds <= 0 || durationSeconds > 0xFFFF
                    || levelThreshold <= 0 || levelDivisor <= 0
                    || playerPhysicalBaseReduction <= 0
                    || otherBaseReduction <= 0
                    || key == null || key.isBlank()) {
                throw new IllegalArgumentException(
                        "invalid native Reduction Armor metadata");
            }
        }

        int effectiveMpCost(int effectiveIntelligence, int originalReduction) {
            if (effectiveIntelligence < 1 || effectiveIntelligence > 254
                    || originalReduction < 0 || originalReduction > 2) {
                throw new IllegalArgumentException(
                        "invalid Reduction Armor MP inputs");
            }
            int result = ResourceDatabase.value(this.mpCost, this.skillId, "ReductionArmorSpell", "mpCost");
            if (effectiveIntelligence > 12) {
                result -= effectiveIntelligence - 12;
            }
            result = Math.max(result, 1);
            return Math.max(0, result - originalReduction);
        }

        int reduction(int targetLevel, boolean playerPhysicalAttacker) {
            if (targetLevel <= 0 || targetLevel > 0xFF) {
                throw new IllegalArgumentException(
                        "invalid Reduction Armor target level");
            }
            int base = playerPhysicalAttacker
                    ? playerPhysicalBaseReduction : otherBaseReduction;
            return base + (Math.max(targetLevel, levelThreshold)
                    - levelThreshold) / levelDivisor;
        }

        int applyTo(int damage, int targetLevel, boolean playerPhysicalAttacker) {
            if (damage < 0) {
                throw new IllegalArgumentException("negative combat damage");
            }
            return Math.max(0,
                    damage - reduction(targetLevel, playerPhysicalAttacker));
        }
    }

    record BounceAttackSpell(
            int skillId, int row, int column, int actionId, int effectGfx,
            int mpCost, int hpCost, int reuseDelayMillis, int durationSeconds,
            int armorClassModifier, int meleeHitModifier,
            int weaponDamageChance, String key) {
        public int actionId() {
            return ResourceDatabase.value(this.actionId, this.skillId, "BounceAttackSpell", "actionId");
        }

        public int durationSeconds() {
            return ResourceDatabase.value(this.durationSeconds, this.skillId, "BounceAttackSpell", "durationSeconds");
        }

        public int effectGfx() {
            return ResourceDatabase.value(this.effectGfx, this.skillId, "BounceAttackSpell", "effectGfx");
        }

        public int hpCost() {
            return ResourceDatabase.value(this.hpCost, this.skillId, "BounceAttackSpell", "hpCost");
        }

        public int mpCost() {
            return ResourceDatabase.value(this.mpCost, this.skillId, "BounceAttackSpell", "mpCost");
        }

        public int reuseDelayMillis() {
            return ResourceDatabase.value(this.reuseDelayMillis, this.skillId, "BounceAttackSpell", "reuseDelayMillis");
        }

        BounceAttackSpell {
            if (skillId != (row << 3) + column + 1
                    || skillId != BOUNCE_ATTACK
                    || row < 0 || row > 0xFF || column < 0 || column > 7
                    || actionId < 0 || actionId > 0xFF
                    || effectGfx <= 0 || effectGfx > 0xFFFF
                    || mpCost <= 0 || hpCost <= 0 || reuseDelayMillis < 0
                    || durationSeconds <= 0 || durationSeconds > 0xFFFF
                    || armorClassModifier >= 0 || armorClassModifier < -127
                    || meleeHitModifier <= 0 || meleeHitModifier > 127
                    || weaponDamageChance <= 0 || weaponDamageChance > 100
                    || key == null || key.isBlank()) {
                throw new IllegalArgumentException(
                        "invalid native Bounce Attack metadata");
            }
        }

        int effectiveMpCost(int effectiveIntelligence, int originalReduction) {
            if (effectiveIntelligence < 1 || effectiveIntelligence > 254
                    || originalReduction < 0 || originalReduction > 2) {
                throw new IllegalArgumentException(
                        "invalid Bounce Attack MP inputs");
            }
            int result = ResourceDatabase.value(this.mpCost, this.skillId, "BounceAttackSpell", "mpCost");
            if (effectiveIntelligence > 12) result -= effectiveIntelligence - 12;
            result = Math.max(result, 1);
            return Math.max(0, result - originalReduction);
        }
    }

    record SolidCarriageSpell(
            int skillId, int row, int column, int actionId, int effectGfx,
            int mpCost, int hpCost, int reuseDelayMillis, int durationSeconds,
            int evasionRateModifier, String key) {
        public int actionId() {
            return ResourceDatabase.value(this.actionId, this.skillId, "SolidCarriageSpell", "actionId");
        }

        public int durationSeconds() {
            return ResourceDatabase.value(this.durationSeconds, this.skillId, "SolidCarriageSpell", "durationSeconds");
        }

        public int effectGfx() {
            return ResourceDatabase.value(this.effectGfx, this.skillId, "SolidCarriageSpell", "effectGfx");
        }

        public int hpCost() {
            return ResourceDatabase.value(this.hpCost, this.skillId, "SolidCarriageSpell", "hpCost");
        }

        public int mpCost() {
            return ResourceDatabase.value(this.mpCost, this.skillId, "SolidCarriageSpell", "mpCost");
        }

        public int reuseDelayMillis() {
            return ResourceDatabase.value(this.reuseDelayMillis, this.skillId, "SolidCarriageSpell", "reuseDelayMillis");
        }

        SolidCarriageSpell {
            if (skillId != (row << 3) + column + 1
                    || skillId != SOLID_CARRIAGE
                    || row != 11 || column != 1 || actionId != 19
                    || effectGfx != 5831 || mpCost != 10 || hpCost != 100
                    || reuseDelayMillis != 3_000 || durationSeconds != 64
                    || evasionRateModifier != 15
                    || key == null || key.isBlank()) {
                throw new IllegalArgumentException(
                        "invalid native Solid Carriage metadata");
            }
        }

        int effectiveMpCost(int effectiveIntelligence, int originalReduction) {
            if (effectiveIntelligence < 1 || effectiveIntelligence > 254
                    || originalReduction < 0 || originalReduction > 2) {
                throw new IllegalArgumentException(
                        "invalid Solid Carriage MP inputs");
            }
            int result = ResourceDatabase.value(this.mpCost, this.skillId, "SolidCarriageSpell", "mpCost");
            if (effectiveIntelligence > 12) result -= effectiveIntelligence - 12;
            result = Math.max(result, 1);
            return Math.max(0, result - originalReduction);
        }
    }

    record CounterBarrierSpell(
            int skillId, int row, int column, int actionId, int effectGfx,
            int mpCost, int hpCost, int reuseDelayMillis, int durationSeconds,
            int requiredWeaponType,
            int probabilityHigh, int probabilityEqual, int probabilityLow,
            boolean includeMagicLevel, boolean includeMagicHit,
            double magicResistanceMultiplier, int maximumProbability,
            int bossModifier, int triggerEffectGfx,
            double damageMultiplier, String key) {
        public int actionId() {
            return ResourceDatabase.value(this.actionId, this.skillId, "CounterBarrierSpell", "actionId");
        }

        public int durationSeconds() {
            return ResourceDatabase.value(this.durationSeconds, this.skillId, "CounterBarrierSpell", "durationSeconds");
        }

        public int effectGfx() {
            return ResourceDatabase.value(this.effectGfx, this.skillId, "CounterBarrierSpell", "effectGfx");
        }

        public int hpCost() {
            return ResourceDatabase.value(this.hpCost, this.skillId, "CounterBarrierSpell", "hpCost");
        }

        public int mpCost() {
            return ResourceDatabase.value(this.mpCost, this.skillId, "CounterBarrierSpell", "mpCost");
        }

        public int reuseDelayMillis() {
            return ResourceDatabase.value(this.reuseDelayMillis, this.skillId, "CounterBarrierSpell", "reuseDelayMillis");
        }

        public int triggerEffectGfx() {
            return ResourceDatabase.value(this.triggerEffectGfx, this.skillId, "CounterBarrierSpell", "triggerEffectGfx");
        }

        CounterBarrierSpell {
            if (skillId != (row << 3) + column + 1
                    || skillId != COUNTER_BARRIER
                    || row != 11 || column != 2 || actionId != 19
                    || effectGfx != 5832 || mpCost != 10 || hpCost != 100
                    || reuseDelayMillis != 3_000 || durationSeconds != 64
                    || requiredWeaponType != 3
                    || probabilityHigh != 55 || probabilityEqual != 45
                    || probabilityLow != 35 || includeMagicLevel
                    || !includeMagicHit
                    || Double.compare(magicResistanceMultiplier, 0.05D) != 0
                    || maximumProbability != 90 || bossModifier != 0
                    || triggerEffectGfx != 10710
                    || Double.compare(damageMultiplier, 1.0D) != 0
                    || key == null || key.isBlank()) {
                throw new IllegalArgumentException(
                        "invalid native Counter Barrier metadata");
            }
        }

        int effectiveMpCost(int effectiveIntelligence, int originalReduction) {
            if (effectiveIntelligence < 1 || effectiveIntelligence > 254
                    || originalReduction < 0 || originalReduction > 2) {
                throw new IllegalArgumentException(
                        "invalid Counter Barrier MP inputs");
            }
            int result = ResourceDatabase.value(this.mpCost, this.skillId, "CounterBarrierSpell", "mpCost");
            if (effectiveIntelligence > 12) result -= effectiveIntelligence - 12;
            result = Math.max(result, 1);
            return Math.max(0, result - originalReduction);
        }

        boolean supportsWeapon(SourceWeaponPhysicalStats weapon) {
            return weapon != null && weapon.present()
                    && weapon.weaponType() == requiredWeaponType;
        }

        int triggerProbability(int holderLevel, int attackerLevel,
                               int holderMagicHit, int attackerMagicResistance) {
            if (holderLevel <= 0 || holderLevel > 0xFF
                    || attackerLevel <= 0 || attackerLevel > 0xFF
                    || holderMagicHit < 0 || attackerMagicResistance < 0) {
                throw new IllegalArgumentException(
                        "invalid Counter Barrier probability inputs");
            }
            double probability = holderLevel > attackerLevel
                    ? probabilityHigh
                    : holderLevel == attackerLevel
                        ? probabilityEqual : probabilityLow;
            if (includeMagicHit) probability += holderMagicHit;
            probability -= attackerMagicResistance * magicResistanceMultiplier;
            probability = Math.max(1.0D, Math.min(100.0D, probability));
            return Math.max(1, Math.min(maximumProbability, (int) probability));
        }

        int reflectedSourceDamage(SourceWeaponPhysicalStats weapon) {
            if (!supportsWeapon(weapon)) return 0;
            int base = Math.max(0, Math.addExact(
                    Math.addExact(weapon.damageLarge(), weapon.enchantLevel()),
                    weapon.damageModifier()));
            return Math.max(0, (int) ((base << 1) * damageMultiplier));
        }
    }

    enum RequestShape {
        SELECTOR_ONLY,
        OBJECT_TARGET,
        SELF_TICK,
        NO_PRODUCER
    }

    record ReservedSkillSlot(
            int skillId, int row, int column, int skillLevel,
            int durationSeconds, int actionId, int castGfx,
            boolean templatePresent,
            RequestShape requestShape) {
        public int durationSeconds() {
            return ResourceDatabase.value(this.durationSeconds, this.skillId, "ReservedSkillSlot", "durationSeconds");
        }

        ReservedSkillSlot {
            boolean wizardGap = skillId == RESERVED_WIZARD_24
                    && row == 2 && column == 7 && skillLevel == 0
                    && durationSeconds == 0 && actionId == 19
                    && castGfx == 189 && templatePresent
                    && requestShape == RequestShape.SELECTOR_ONLY;
            boolean generalGap = skillId >= RESERVED_GENERAL_81
                    && skillId <= RESERVED_GENERAL_86
                    && row == 10 && column == skillId - RESERVED_GENERAL_81
                    && skillLevel == 0
                    && durationSeconds == switch (skillId) {
                        case RESERVED_GENERAL_85 -> 900;
                        case RESERVED_GENERAL_86 -> 30;
                        default -> 0;
                    }
                    && actionId == 19
                    && castGfx == switch (skillId) {
                        case RESERVED_GENERAL_85 -> 2059;
                        case RESERVED_GENERAL_86 -> 760;
                        default -> 0;
                    }
                    && templatePresent
                    && requestShape == RequestShape.SELECTOR_ONLY;
            boolean knightGap = skillId >= RESERVED_KNIGHT_1
                    && skillId <= RESERVED_KNIGHT_5
                    && row == 11 && column == skillId - 89
                    && skillLevel == 0 && durationSeconds == 1_200
                    && actionId == 19 && castGfx == 5906 + skillId - 92
                    && templatePresent
                    && (skillId == RESERVED_KNIGHT_2)
                            == (requestShape == RequestShape.OBJECT_TARGET);
            boolean elfGap = skillId >= RESERVED_ELF_119
                    && skillId <= RESERVED_ELF_128
                    && row == (skillId - 1) / 8
                    && column == (skillId - 1) % 8
                    && skillLevel == (skillId <= RESERVED_ELF_120 ? 15 : 0)
                    && durationSeconds == 0 && actionId == 19
                    && castGfx == (skillId == RESERVED_ELF_120 ? 2280 : 0)
                    && templatePresent == (skillId != RESERVED_ELF_122)
                    && requestShape == RequestShape.SELECTOR_ONLY;
            boolean laterElfGap = ((skillId >= RESERVED_ELF_135
                            && skillId <= RESERVED_ELF_136)
                        || (skillId >= RESERVED_ELF_139
                            && skillId <= RESERVED_ELF_144))
                    && row == (skillId - 1) / 8
                    && column == (skillId - 1) % 8
                    && skillLevel == 0 && durationSeconds == 0
                    && actionId == 19 && castGfx == 0 && templatePresent
                    && requestShape == RequestShape.NO_PRODUCER;
            boolean dragonGap = skillId >= RESERVED_DRAGON_177
                    && skillId <= RESERVED_DRAGON_180
                    && row == 22
                    && column == skillId - RESERVED_DRAGON_177
                    && skillLevel == 0 && durationSeconds == 0
                    && actionId == 0 && castGfx == 0 && templatePresent
                    && requestShape == RequestShape.SELECTOR_ONLY;
            if (skillId != (row << 3) + column + 1
                    || requestShape == null
                    || (!wizardGap && !generalGap && !knightGap && !elfGap
                            && !laterElfGap && !dragonGap)) {
                throw new IllegalArgumentException(
                        "invalid native reserved skill-slot metadata");
            }
        }
    }

    record MagicResistanceSpell(
            int skillId, int row, int column, int skillLevel, int skillNumber,
            int actionId, int effectGfx, int mpCost, int hpCost,
            int reuseDelayMillis, int durationSeconds,
            int maximumRange, int area, int targetTo,
            int attribute, int sourceType, int nativeId,
            long itemConsumeId, int itemConsumeCount,
            int magicResistance, int stopMessage,
            boolean counterMagicBypass, String key) {
        public long itemConsumeId() {
            return CombatDatabase.longValue(this.itemConsumeId, this.skillId, "itemConsumeId");
        }

        public int actionId() {
            return ResourceDatabase.value(this.actionId, this.skillId, "MagicResistanceSpell", "actionId");
        }

        public int durationSeconds() {
            return ResourceDatabase.value(this.durationSeconds, this.skillId, "MagicResistanceSpell", "durationSeconds");
        }

        public int effectGfx() {
            return ResourceDatabase.value(this.effectGfx, this.skillId, "MagicResistanceSpell", "effectGfx");
        }

        public int hpCost() {
            return ResourceDatabase.value(this.hpCost, this.skillId, "MagicResistanceSpell", "hpCost");
        }

        public int itemConsumeCount() {
            return ResourceDatabase.value(this.itemConsumeCount, this.skillId, "MagicResistanceSpell", "itemConsumeCount");
        }

        public int mpCost() {
            return ResourceDatabase.value(this.mpCost, this.skillId, "MagicResistanceSpell", "mpCost");
        }

        public int reuseDelayMillis() {
            return ResourceDatabase.value(this.reuseDelayMillis, this.skillId, "MagicResistanceSpell", "reuseDelayMillis");
        }

        MagicResistanceSpell {
            if (skillId != RESIST_MAGIC
                    || skillId != (row << 3) + column + 1
                    || row != 16 || column != 0 || skillLevel != 17
                    || skillNumber != 0 || actionId != 19 || effectGfx != 2186
                    || mpCost != 5 || hpCost != 0 || reuseDelayMillis != 0
                    || durationSeconds != 1_200 || maximumRange != 0
                    || area != 0 || targetTo != 0
                    || attribute != ATTRIBUTE_UNSPECIFIED || sourceType != 2
                    || nativeId != 1
                    || itemConsumeId != InventoryCatalog.SPIRIT_CRYSTAL
                    || itemConsumeCount != 1 || magicResistance != 10
                    || stopMessage != 719 || !counterMagicBypass
                    || key == null || key.isBlank()) {
                throw new IllegalArgumentException(
                        "invalid native Resist Magic metadata");
            }
        }

        int effectiveMpCost(int effectiveIntelligence, int originalReduction) {
            if (effectiveIntelligence < 1 || effectiveIntelligence > 254
                    || originalReduction < 0 || originalReduction > 2) {
                throw new IllegalArgumentException(
                        "invalid Resist Magic MP inputs");
            }
            // Skill 129 is outside general-magic circle reductions.  The
            // retained original-INT class reduction is still applied last.
            return Math.max(0, ResourceDatabase.value(this.mpCost, this.skillId, "MagicResistanceSpell", "mpCost") - originalReduction);
        }
    }

    record BodyToMindSpell(
            int skillId, int row, int column, int skillLevel, int skillNumber,
            int actionId, int effectGfx, int mpCost, int hpCost,
            int reuseDelayMillis, int secondaryReuseDelayMillis,
            int durationSeconds, int maximumRange, int area, int targetTo,
            int attribute, int sourceType, int nativeId,
            long itemConsumeId, int itemConsumeCount,
            int happenMessage, int stopMessage, int failureMessage,
            int manaGain, boolean counterMagicBypass, String key) {
        public long itemConsumeId() {
            return CombatDatabase.longValue(this.itemConsumeId, this.skillId, "itemConsumeId");
        }

        public int actionId() {
            return ResourceDatabase.value(this.actionId, this.skillId, "BodyToMindSpell", "actionId");
        }

        public int durationSeconds() {
            return ResourceDatabase.value(this.durationSeconds, this.skillId, "BodyToMindSpell", "durationSeconds");
        }

        public int effectGfx() {
            return ResourceDatabase.value(this.effectGfx, this.skillId, "BodyToMindSpell", "effectGfx");
        }

        public int hpCost() {
            return ResourceDatabase.value(this.hpCost, this.skillId, "BodyToMindSpell", "hpCost");
        }

        public int itemConsumeCount() {
            return ResourceDatabase.value(this.itemConsumeCount, this.skillId, "BodyToMindSpell", "itemConsumeCount");
        }

        public int manaGain() {
            return ResourceDatabase.value(this.manaGain, this.skillId, "BodyToMindSpell", "manaGain");
        }

        public int mpCost() {
            return ResourceDatabase.value(this.mpCost, this.skillId, "BodyToMindSpell", "mpCost");
        }

        public int reuseDelayMillis() {
            return ResourceDatabase.value(this.reuseDelayMillis, this.skillId, "BodyToMindSpell", "reuseDelayMillis");
        }

        BodyToMindSpell {
            boolean bodyToMind = skillId == BODY_TO_MIND
                    && row == 16 && column == 1 && skillLevel == 17
                    && skillNumber == 1 && actionId == 19 && effectGfx == 2179
                    && mpCost == 0 && hpCost == 8 && reuseDelayMillis == 100
                    && secondaryReuseDelayMillis == 400 && durationSeconds == 0
                    && maximumRange == 0 && area == 0 && targetTo == 0
                    && attribute == ATTRIBUTE_UNSPECIFIED && sourceType == 2
                    && nativeId == 2 && itemConsumeId == 0
                    && itemConsumeCount == 0 && happenMessage == 702
                    && stopMessage == 0 && failureMessage == 0
                    && manaGain == 2 && counterMagicBypass
                    && "body-to-mind".equals(key);
            boolean bloodySoul = skillId == BLOODY_SOUL
                    && row == 18 && column == 1 && skillLevel == 19
                    && skillNumber == 1 && actionId == 19 && effectGfx == 2178
                    && mpCost == 0 && hpCost == 40
                    && reuseDelayMillis == 1_200
                    && secondaryReuseDelayMillis == 1_200
                    && durationSeconds == 0 && maximumRange == 0
                    && area == 0 && targetTo == 0
                    && attribute == ATTRIBUTE_UNSPECIFIED && sourceType == 2
                    && nativeId == 2 && itemConsumeId == 0
                    && itemConsumeCount == 0 && happenMessage == 0
                    && stopMessage == 0 && failureMessage == 0
                    && manaGain == 20 && counterMagicBypass
                    && "bloody-soul".equals(key);
            if (skillId != (row << 3) + column + 1
                    || (!bodyToMind && !bloodySoul)) {
                throw new IllegalArgumentException(
                        "invalid native HP-to-MP conversion metadata");
            }
        }

        boolean hasEnoughHitPoints(int currentHp) {
            if (currentHp < 0) {
                throw new IllegalArgumentException("invalid current HP");
            }
            return currentHp >= hpCost + 1;
        }

        int hitPointsAfter(int currentHp) {
            if (!hasEnoughHitPoints(currentHp)) {
                throw new IllegalArgumentException(
                        "Body to Mind must preserve one hit point");
            }
            return currentHp - hpCost;
        }

        int manaAfter(int currentMp, int maximumMp) {
            if (currentMp < 0 || maximumMp < 0 || currentMp > maximumMp) {
                throw new IllegalArgumentException("invalid current/maximum MP");
            }
            return Math.min(maximumMp, currentMp + manaGain);
        }
    }

    record TripleArrowSpell(
            int skillId, int row, int column, int level, int number,
            int databaseActionId, int effectGfx,
            int mpCost, int reuseDelayMillis, int secondaryReuseDelayMillis,
            int durationSeconds, int targetTo, int areaRadius,
            int maximumRange, int through,
            int attribute, int type, int nativeId, int hitCount,
            boolean counterMagicBypass, String key) {
        public int durationSeconds() {
            return ResourceDatabase.value(this.durationSeconds, this.skillId, "TripleArrowSpell", "durationSeconds");
        }

        public int effectGfx() {
            return ResourceDatabase.value(this.effectGfx, this.skillId, "TripleArrowSpell", "effectGfx");
        }

        public int mpCost() {
            return ResourceDatabase.value(this.mpCost, this.skillId, "TripleArrowSpell", "mpCost");
        }

        public int reuseDelayMillis() {
            return ResourceDatabase.value(this.reuseDelayMillis, this.skillId, "TripleArrowSpell", "reuseDelayMillis");
        }

        TripleArrowSpell {
            if (skillId != (row << 3) + column + 1
                    || skillId != TRIPLE_ARROW || row != 16 || column != 3
                    || level != 17 || number != 3
                    || databaseActionId != 18 || effectGfx != 4_394
                    || mpCost != 11 || reuseDelayMillis != 100
                    || secondaryReuseDelayMillis != 400
                    || durationSeconds != 0 || targetTo != 3
                    || areaRadius != 0 || maximumRange != -1 || through != 0
                    || attribute != ATTRIBUTE_UNSPECIFIED || type != 64
                    || nativeId != 8 || hitCount != 3
                    || !counterMagicBypass || key == null || key.isBlank()) {
                throw new IllegalArgumentException(
                        "invalid native Triple Arrow metadata");
            }
        }

        int effectiveMpCost(int intelligence, int originalReduction) {
            if (intelligence < 1 || intelligence > 254
                    || originalReduction < 0 || originalReduction > 2) {
                throw new IllegalArgumentException(
                        "invalid native Triple Arrow MP inputs");
            }
            int result = ResourceDatabase.value(this.mpCost, this.skillId, "TripleArrowSpell", "mpCost");
            if (intelligence > 12) result -= intelligence - 12;
            return Math.max(0, Math.max(1, result) - originalReduction);
        }
    }

    record ElementalFallDownSpell(
            int skillId, int row, int column, int level, int number,
            int actionId, int effectGfx, int mpCost, int reuseDelayMillis,
            int durationSeconds, int targetTo, int maximumRange, int through,
            int attribute, int type, int nativeId, int failureMessage,
            int resistanceDebuff,
            int probabilityHigh, int probabilityEqual, int probabilityLow,
            double intelligenceMultiplier, double magicResistanceMultiplier,
            boolean includeMagicLevel, boolean includeMagicHit,
            int bossModifier, boolean counterMagicBypass, String key) {
        public int actionId() {
            return ResourceDatabase.value(this.actionId, this.skillId, "ElementalFallDownSpell", "actionId");
        }

        public int durationSeconds() {
            return ResourceDatabase.value(this.durationSeconds, this.skillId, "ElementalFallDownSpell", "durationSeconds");
        }

        public int effectGfx() {
            return ResourceDatabase.value(this.effectGfx, this.skillId, "ElementalFallDownSpell", "effectGfx");
        }

        public int mpCost() {
            return ResourceDatabase.value(this.mpCost, this.skillId, "ElementalFallDownSpell", "mpCost");
        }

        public int reuseDelayMillis() {
            return ResourceDatabase.value(this.reuseDelayMillis, this.skillId, "ElementalFallDownSpell", "reuseDelayMillis");
        }

        ElementalFallDownSpell {
            if (skillId != (row << 3) + column + 1
                    || skillId != ELEMENTAL_FALL_DOWN
                    || row != 16 || column != 4 || level != 17 || number != 4
                    || actionId != 19 || effectGfx != 4_396 || mpCost != 10
                    || reuseDelayMillis != 100 || durationSeconds != 32
                    || targetTo != 3 || maximumRange != 10 || through != 0
                    || attribute != ATTRIBUTE_UNSPECIFIED || type != 1
                    || nativeId != 16 || failureMessage != 280
                    || resistanceDebuff != 50
                    || probabilityHigh != 55 || probabilityEqual != 45
                    || probabilityLow != 35
                    || intelligenceMultiplier != 0.30D
                    || magicResistanceMultiplier != 0.05D
                    || includeMagicLevel || !includeMagicHit
                    || bossModifier != 0 || counterMagicBypass
                    || key == null || key.isBlank()) {
                throw new IllegalArgumentException(
                        "invalid native Elemental Fall Down metadata");
            }
        }

        int probability(int casterLevel, int targetLevel,
                        int effectiveIntelligence, int magicLevel,
                        int magicHit, int targetMagicResistance) {
            if (casterLevel <= 0 || targetLevel <= 0 || effectiveIntelligence < 0
                    || magicLevel < 0 || targetMagicResistance < 0) {
                throw new IllegalArgumentException(
                        "invalid Elemental Fall Down probability inputs");
            }
            double result = casterLevel > targetLevel ? probabilityHigh
                    : casterLevel == targetLevel ? probabilityEqual : probabilityLow;
            result += effectiveIntelligence * intelligenceMultiplier;
            if (includeMagicHit) result += magicHit;
            if (includeMagicLevel) result += magicLevel;
            result -= targetMagicResistance * magicResistanceMultiplier;
            result = Math.max(1.0D, Math.min(100.0D, result));
            return Math.max(1, Math.min(90, (int) result));
        }

        int probabilityForTarget(
                int casterLevel, int targetLevel, int effectiveIntelligence,
                int magicLevel, int magicHit, int targetMagicResistance,
                boolean targetBoss) {
            int result = probability(casterLevel, targetLevel,
                    effectiveIntelligence, magicLevel, magicHit,
                    targetMagicResistance);
            if (!targetBoss) return result;
            if (bossModifier == 0) return 0;
            return Math.max(1, result / bossModifier);
        }

        int effectiveMpCost(int originalReduction) {
            if (originalReduction < 0 || originalReduction > 2) {
                throw new IllegalArgumentException(
                        "invalid Elemental Fall Down MP inputs");
            }
            return Math.max(0, ResourceDatabase.value(this.mpCost, this.skillId, "ElementalFallDownSpell", "mpCost") - originalReduction);
        }
    }

    record ReturnToNatureSpell(
            int skillId, int row, int column, int level, int number,
            int actionId, int effectGfx, int mpCost, int reuseDelayMillis,
            int durationSeconds, int targetTo, int maximumRange, int through,
            int attribute, int type, int nativeId,
            long itemConsumeId, int itemConsumeCount,
            int failureMessage, int invalidTargetMessage,
            int probabilityHigh, int probabilityEqual, int probabilityLow,
            double intelligenceMultiplier, double magicResistanceMultiplier,
            boolean includeMagicLevel, boolean includeMagicHit,
            int bossModifier, boolean counterMagicBypass, String key) {
        public long itemConsumeId() {
            return CombatDatabase.longValue(this.itemConsumeId, this.skillId, "itemConsumeId");
        }

        public int actionId() {
            return ResourceDatabase.value(this.actionId, this.skillId, "ReturnToNatureSpell", "actionId");
        }

        public int durationSeconds() {
            return ResourceDatabase.value(this.durationSeconds, this.skillId, "ReturnToNatureSpell", "durationSeconds");
        }

        public int effectGfx() {
            return ResourceDatabase.value(this.effectGfx, this.skillId, "ReturnToNatureSpell", "effectGfx");
        }

        public int itemConsumeCount() {
            return ResourceDatabase.value(this.itemConsumeCount, this.skillId, "ReturnToNatureSpell", "itemConsumeCount");
        }

        public int mpCost() {
            return ResourceDatabase.value(this.mpCost, this.skillId, "ReturnToNatureSpell", "mpCost");
        }

        public int reuseDelayMillis() {
            return ResourceDatabase.value(this.reuseDelayMillis, this.skillId, "ReturnToNatureSpell", "reuseDelayMillis");
        }

        ReturnToNatureSpell {
            if (skillId != (row << 3) + column + 1
                    || skillId != RETURN_TO_NATURE
                    || row != 18 || column != 0 || level != 19 || number != 0
                    || actionId != 19 || effectGfx != 2_245 || mpCost != 30
                    || reuseDelayMillis != 0 || durationSeconds != 0
                    || targetTo != 16 || maximumRange != -1 || through != 0
                    || attribute != ATTRIBUTE_UNSPECIFIED || type != 1
                    || nativeId != 1
                    || itemConsumeId != InventoryCatalog.SPIRIT_CRYSTAL
                    || itemConsumeCount != 2 || failureMessage != 280
                    || invalidTargetMessage != 79
                    || probabilityHigh != 55 || probabilityEqual != 45
                    || probabilityLow != 35
                    || intelligenceMultiplier != 0.30D
                    || magicResistanceMultiplier != 0.05D
                    || includeMagicLevel || !includeMagicHit
                    || bossModifier != 0 || counterMagicBypass
                    || key == null || key.isBlank()) {
                throw new IllegalArgumentException(
                        "invalid native Return to Nature metadata");
            }
        }

        int probability(int casterLevel, int targetOwnerLevel,
                        int effectiveIntelligence, int magicLevel,
                        int magicHit, int targetMagicResistance) {
            if (casterLevel <= 0 || targetOwnerLevel <= 0
                    || effectiveIntelligence < 0 || magicLevel < 0
                    || targetMagicResistance < 0) {
                throw new IllegalArgumentException(
                        "invalid Return to Nature probability inputs");
            }
            double result = casterLevel > targetOwnerLevel ? probabilityHigh
                    : casterLevel == targetOwnerLevel
                        ? probabilityEqual : probabilityLow;
            result += effectiveIntelligence * intelligenceMultiplier;
            if (includeMagicHit) result += magicHit;
            if (includeMagicLevel) result += magicLevel;
            result -= targetMagicResistance * magicResistanceMultiplier;
            result = Math.max(1.0D, Math.min(100.0D, result));
            return Math.max(1, Math.min(90, (int) result));
        }

        int effectiveMpCost(int originalReduction) {
            if (originalReduction < 0 || originalReduction > 2) {
                throw new IllegalArgumentException(
                        "invalid Return to Nature MP inputs");
            }
            return Math.max(0, ResourceDatabase.value(this.mpCost, this.skillId, "ReturnToNatureSpell", "mpCost") - originalReduction);
        }
    }

    record CounterMirrorSpell(
            int skillId, int row, int column, int level, int number,
            int actionId, int effectGfx, int mpCost,
            int reuseDelayMillis, int secondaryReuseDelayMillis,
            int durationSeconds, int maximumRange, int area, int targetTo,
            int attribute, int type, int nativeId,
            long itemConsumeId, int itemConsumeCount,
            int failureMessage, String key) {
        public long itemConsumeId() {
            return CombatDatabase.longValue(this.itemConsumeId, this.skillId, "itemConsumeId");
        }

        public int actionId() {
            return ResourceDatabase.value(this.actionId, this.skillId, "CounterMirrorSpell", "actionId");
        }

        public int durationSeconds() {
            return ResourceDatabase.value(this.durationSeconds, this.skillId, "CounterMirrorSpell", "durationSeconds");
        }

        public int effectGfx() {
            return ResourceDatabase.value(this.effectGfx, this.skillId, "CounterMirrorSpell", "effectGfx");
        }

        public int itemConsumeCount() {
            return ResourceDatabase.value(this.itemConsumeCount, this.skillId, "CounterMirrorSpell", "itemConsumeCount");
        }

        public int mpCost() {
            return ResourceDatabase.value(this.mpCost, this.skillId, "CounterMirrorSpell", "mpCost");
        }

        public int reuseDelayMillis() {
            return ResourceDatabase.value(this.reuseDelayMillis, this.skillId, "CounterMirrorSpell", "reuseDelayMillis");
        }

        CounterMirrorSpell {
            if (skillId != (row << 3) + column + 1
                    || skillId != COUNTER_MIRROR
                    || row != 16 || column != 5 || level != 17 || number != 5
                    || actionId != 19 || effectGfx != 4_395 || mpCost != 10
                    || reuseDelayMillis != 1_000
                    || secondaryReuseDelayMillis != 0
                    || durationSeconds != 16 || maximumRange != 0
                    || area != 0 || targetTo != 0
                    || attribute != ATTRIBUTE_UNSPECIFIED || type != 2
                    || nativeId != 32
                    || itemConsumeId != InventoryCatalog.SPIRIT_CRYSTAL
                    || itemConsumeCount != 1 || failureMessage != 280
                    || key == null || key.isBlank()) {
                throw new IllegalArgumentException(
                        "invalid native Counter Mirror metadata");
            }
        }

        int effectiveMpCost(int originalReduction) {
            if (originalReduction < 0 || originalReduction > 2) {
                throw new IllegalArgumentException(
                        "invalid Counter Mirror MP inputs");
            }
            return Math.max(0, ResourceDatabase.value(this.mpCost, this.skillId, "CounterMirrorSpell", "mpCost") - originalReduction);
        }
    }

    record ClearMindSpell(
            int skillId, int row, int column, int level, int number,
            int actionId, int effectGfx, int mpCost,
            int reuseDelayMillis, int secondaryReuseDelayMillis,
            int durationSeconds, int maximumRange, int area, int targetTo,
            int attribute, int type, int nativeId,
            long itemConsumeId, int itemConsumeCount,
            int wisdomModifier, int happenMessage,
            boolean counterMagicBypass, String key) {
        public long itemConsumeId() {
            return CombatDatabase.longValue(this.itemConsumeId, this.skillId, "itemConsumeId");
        }

        public int actionId() {
            return ResourceDatabase.value(this.actionId, this.skillId, "ClearMindSpell", "actionId");
        }

        public int durationSeconds() {
            return ResourceDatabase.value(this.durationSeconds, this.skillId, "ClearMindSpell", "durationSeconds");
        }

        public int effectGfx() {
            return ResourceDatabase.value(this.effectGfx, this.skillId, "ClearMindSpell", "effectGfx");
        }

        public int itemConsumeCount() {
            return ResourceDatabase.value(this.itemConsumeCount, this.skillId, "ClearMindSpell", "itemConsumeCount");
        }

        public int mpCost() {
            return ResourceDatabase.value(this.mpCost, this.skillId, "ClearMindSpell", "mpCost");
        }

        public int reuseDelayMillis() {
            return ResourceDatabase.value(this.reuseDelayMillis, this.skillId, "ClearMindSpell", "reuseDelayMillis");
        }

        ClearMindSpell {
            if (skillId != (row << 3) + column + 1
                    || skillId != CLEAR_MIND || row != 17 || column != 0
                    || level != 18 || number != 0
                    || actionId != 19 || effectGfx != 2_180 || mpCost != 10
                    || reuseDelayMillis != 0 || secondaryReuseDelayMillis != 0
                    || durationSeconds != 1_200 || maximumRange != 0
                    || area != 0 || targetTo != 0
                    || attribute != ATTRIBUTE_UNSPECIFIED || type != 2
                    || nativeId != 1
                    || itemConsumeId != InventoryCatalog.SPIRIT_CRYSTAL
                    || itemConsumeCount != 1 || wisdomModifier != 3
                    || happenMessage != 722 || !counterMagicBypass
                    || key == null || key.isBlank()) {
                throw new IllegalArgumentException(
                        "invalid native Clear Mind metadata");
            }
        }

        int effectiveMpCost(int originalReduction) {
            if (originalReduction < 0 || originalReduction > 2) {
                throw new IllegalArgumentException(
                        "invalid Clear Mind MP inputs");
            }
            return Math.max(0, ResourceDatabase.value(this.mpCost, this.skillId, "ClearMindSpell", "mpCost") - originalReduction);
        }
    }

    record ResistElementalSpell(
            int skillId, int row, int column, int level, int number,
            int actionId, int effectGfx, int mpCost,
            int reuseDelayMillis, int secondaryReuseDelayMillis,
            int durationSeconds, int maximumRange, int area, int targetTo,
            int attribute, int type, int nativeId,
            long itemConsumeId, int itemConsumeCount,
            int resistanceModifier, int happenMessage,
            boolean counterMagicBypass, String key) {
        public long itemConsumeId() {
            return CombatDatabase.longValue(this.itemConsumeId, this.skillId, "itemConsumeId");
        }

        public int actionId() {
            return ResourceDatabase.value(this.actionId, this.skillId, "ResistElementalSpell", "actionId");
        }

        public int durationSeconds() {
            return ResourceDatabase.value(this.durationSeconds, this.skillId, "ResistElementalSpell", "durationSeconds");
        }

        public int effectGfx() {
            return ResourceDatabase.value(this.effectGfx, this.skillId, "ResistElementalSpell", "effectGfx");
        }

        public int itemConsumeCount() {
            return ResourceDatabase.value(this.itemConsumeCount, this.skillId, "ResistElementalSpell", "itemConsumeCount");
        }

        public int mpCost() {
            return ResourceDatabase.value(this.mpCost, this.skillId, "ResistElementalSpell", "mpCost");
        }

        public int reuseDelayMillis() {
            return ResourceDatabase.value(this.reuseDelayMillis, this.skillId, "ResistElementalSpell", "reuseDelayMillis");
        }

        ResistElementalSpell {
            if (skillId != (row << 3) + column + 1
                    || skillId != RESIST_ELEMENTAL || row != 17 || column != 1
                    || level != 18 || number != 1
                    || actionId != 19 || effectGfx != 2_184 || mpCost != 10
                    || reuseDelayMillis != 0 || secondaryReuseDelayMillis != 0
                    || durationSeconds != 1_200 || maximumRange != 0
                    || area != 0 || targetTo != 0
                    || attribute != ATTRIBUTE_UNSPECIFIED || type != 2
                    || nativeId != 2
                    || itemConsumeId != InventoryCatalog.SPIRIT_CRYSTAL
                    || itemConsumeCount != 1 || resistanceModifier != 10
                    || happenMessage != 721 || !counterMagicBypass
                    || key == null || key.isBlank()) {
                throw new IllegalArgumentException(
                        "invalid native Resist Elemental metadata");
            }
        }

        int effectiveMpCost(int originalReduction) {
            if (originalReduction < 0 || originalReduction > 2) {
                throw new IllegalArgumentException(
                        "invalid Resist Elemental MP inputs");
            }
            return Math.max(0, ResourceDatabase.value(this.mpCost, this.skillId, "ResistElementalSpell", "mpCost") - originalReduction);
        }
    }

    record ElementalProtectionSpell(
            int skillId, int row, int column, int level, int number,
            int actionId, int effectGfx, int mpCost,
            int reuseDelayMillis, int secondaryReuseDelayMillis,
            int durationSeconds, int maximumRange, int area, int targetTo,
            int attribute, int type, int nativeId,
            long itemConsumeId, int itemConsumeCount,
            int resistanceModifier, int happenMessage,
            boolean counterMagicBypass, String key) {
        public long itemConsumeId() {
            return CombatDatabase.longValue(this.itemConsumeId, this.skillId, "itemConsumeId");
        }

        public int actionId() {
            return ResourceDatabase.value(this.actionId, this.skillId, "ElementalProtectionSpell", "actionId");
        }

        public int durationSeconds() {
            return ResourceDatabase.value(this.durationSeconds, this.skillId, "ElementalProtectionSpell", "durationSeconds");
        }

        public int effectGfx() {
            return ResourceDatabase.value(this.effectGfx, this.skillId, "ElementalProtectionSpell", "effectGfx");
        }

        public int itemConsumeCount() {
            return ResourceDatabase.value(this.itemConsumeCount, this.skillId, "ElementalProtectionSpell", "itemConsumeCount");
        }

        public int mpCost() {
            return ResourceDatabase.value(this.mpCost, this.skillId, "ElementalProtectionSpell", "mpCost");
        }

        public int reuseDelayMillis() {
            return ResourceDatabase.value(this.reuseDelayMillis, this.skillId, "ElementalProtectionSpell", "reuseDelayMillis");
        }

        ElementalProtectionSpell {
            if (skillId != (row << 3) + column + 1
                    || skillId != ELEMENTAL_PROTECTION
                    || row != 18 || column != 2 || level != 19 || number != 2
                    || actionId != 19 || effectGfx != 2_285 || mpCost != 6
                    || reuseDelayMillis != 0 || secondaryReuseDelayMillis != 0
                    || durationSeconds != 64 || maximumRange != 0
                    || area != 0 || targetTo != 0
                    || attribute != ATTRIBUTE_UNSPECIFIED || type != 2
                    || nativeId != 4
                    || itemConsumeId != InventoryCatalog.SPIRIT_CRYSTAL
                    || itemConsumeCount != 1 || resistanceModifier != 50
                    || happenMessage != 0 || !counterMagicBypass
                    || key == null || key.isBlank()) {
                throw new IllegalArgumentException(
                        "invalid native Elemental Protection metadata");
            }
        }

        int effectiveMpCost(int originalReduction) {
            if (originalReduction < 0 || originalReduction > 2) {
                throw new IllegalArgumentException(
                        "invalid Elemental Protection MP inputs");
            }
            return Math.max(0, ResourceDatabase.value(this.mpCost, this.skillId, "ElementalProtectionSpell", "mpCost") - originalReduction);
        }
    }

    record FireWeaponSpell(
            int skillId, int row, int column, int level, int number,
            int actionId, int effectGfx, int mpCost,
            int reuseDelayMillis, int secondaryReuseDelayMillis,
            int durationSeconds, int maximumRange, int area, int targetTo,
            int attribute, int type, int nativeId,
            int meleeHitModifier, int meleeDamageModifier,
            int happenMessage, int failureMessage,
            boolean counterMagicBypass, String key) {
        public int actionId() {
            return ResourceDatabase.value(this.actionId, this.skillId, "FireWeaponSpell", "actionId");
        }

        public int durationSeconds() {
            return ResourceDatabase.value(this.durationSeconds, this.skillId, "FireWeaponSpell", "durationSeconds");
        }

        public int effectGfx() {
            return ResourceDatabase.value(this.effectGfx, this.skillId, "FireWeaponSpell", "effectGfx");
        }

        public int mpCost() {
            return ResourceDatabase.value(this.mpCost, this.skillId, "FireWeaponSpell", "mpCost");
        }

        public int reuseDelayMillis() {
            return ResourceDatabase.value(this.reuseDelayMillis, this.skillId, "FireWeaponSpell", "reuseDelayMillis");
        }

        FireWeaponSpell {
            boolean fire = skillId == FIRE_WEAPON
                    && row == 18 && column == 3 && level == 19 && number == 3
                    && effectGfx == 2_182 && mpCost == 15 && targetTo == 1
                    && nativeId == 8 && meleeHitModifier == 0
                    && meleeDamageModifier == 4 && "fire-weapon".equals(key);
            boolean burning = skillId == BURNING_WEAPON
                    && row == 20 && column == 2 && level == 21 && number == 2
                    && effectGfx == 2_242 && mpCost == 30 && targetTo == 0
                    && nativeId == 4 && meleeHitModifier == 3
                    && meleeDamageModifier == 6 && "burning-weapon".equals(key);
            if (skillId != (row << 3) + column + 1 || (!fire && !burning)
                    || actionId != 19
                    || reuseDelayMillis != 0 || secondaryReuseDelayMillis != 0
                    || durationSeconds != 960 || maximumRange != 0
                    || area != 0
                    || (fire && attribute != ATTRIBUTE_UNSPECIFIED)
                    || (burning && attribute != ATTRIBUTE_FIRE) || type != 2
                    || happenMessage != 723 || failureMessage != 280
                    || !counterMagicBypass || key == null || key.isBlank()) {
                throw new IllegalArgumentException(
                        "invalid native fire melee weapon metadata");
            }
        }

        int effectiveMpCost(int originalReduction) {
            if (originalReduction < 0 || originalReduction > 2) {
                throw new IllegalArgumentException(
                        "invalid fire melee weapon MP inputs");
            }
            // Elf skills148/163 are outside general-magic circle reductions.
            // Retained isHPMPConsume applies only the original-INT reduction.
            return Math.max(0, ResourceDatabase.value(this.mpCost, this.skillId, "FireWeaponSpell", "mpCost") - originalReduction);
        }
    }

    record WindShotSpell(
            int skillId, int row, int column, int level, int number,
            int actionId, int effectGfx, int mpCost,
            int reuseDelayMillis, int secondaryReuseDelayMillis,
            int durationSeconds, int maximumRange, int area, int targetTo,
            int attribute, int type, int nativeId,
            int rangedHitModifier, int happenMessage, int failureMessage,
            boolean counterMagicBypass, String key) {
        public int actionId() {
            return ResourceDatabase.value(this.actionId, this.skillId, "WindShotSpell", "actionId");
        }

        public int durationSeconds() {
            return ResourceDatabase.value(this.durationSeconds, this.skillId, "WindShotSpell", "durationSeconds");
        }

        public int effectGfx() {
            return ResourceDatabase.value(this.effectGfx, this.skillId, "WindShotSpell", "effectGfx");
        }

        public int mpCost() {
            return ResourceDatabase.value(this.mpCost, this.skillId, "WindShotSpell", "mpCost");
        }

        public int reuseDelayMillis() {
            return ResourceDatabase.value(this.reuseDelayMillis, this.skillId, "WindShotSpell", "reuseDelayMillis");
        }

        WindShotSpell {
            if (skillId != (row << 3) + column + 1
                    || skillId != WIND_SHOT
                    || row != 18 || column != 4 || level != 19 || number != 4
                    || actionId != 19 || effectGfx != 2_246 || mpCost != 15
                    || reuseDelayMillis != 0 || secondaryReuseDelayMillis != 0
                    || durationSeconds != 960 || maximumRange != 0
                    || area != 0 || targetTo != 1
                    || attribute != ATTRIBUTE_UNSPECIFIED || type != 2
                    || nativeId != 16 || rangedHitModifier != 6
                    || happenMessage != 724 || failureMessage != 280
                    || !counterMagicBypass || key == null || key.isBlank()) {
                throw new IllegalArgumentException(
                        "invalid native Wind Shot metadata");
            }
        }

        int effectiveMpCost(int originalReduction) {
            if (originalReduction < 0 || originalReduction > 2) {
                throw new IllegalArgumentException(
                        "invalid Wind Shot MP inputs");
            }
            // Elf skill149 is outside the general-magic circle reductions.
            return Math.max(0, ResourceDatabase.value(this.mpCost, this.skillId, "WindShotSpell", "mpCost") - originalReduction);
        }
    }

    record FireBlessSpell(
            int skillId, int row, int column, int level, int number,
            int actionId, int effectGfx, int mpCost,
            int reuseDelayMillis, int secondaryReuseDelayMillis,
            int durationSeconds, int maximumRange, int area, int targetTo,
            int attribute, int type, int nativeId,
            int meleeDamageModifier, int happenMessage,
            boolean counterMagicBypass, String key) {
        public int actionId() {
            return ResourceDatabase.value(this.actionId, this.skillId, "FireBlessSpell", "actionId");
        }

        public int durationSeconds() {
            return ResourceDatabase.value(this.durationSeconds, this.skillId, "FireBlessSpell", "durationSeconds");
        }

        public int effectGfx() {
            return ResourceDatabase.value(this.effectGfx, this.skillId, "FireBlessSpell", "effectGfx");
        }

        public int mpCost() {
            return ResourceDatabase.value(this.mpCost, this.skillId, "FireBlessSpell", "mpCost");
        }

        public int reuseDelayMillis() {
            return ResourceDatabase.value(this.reuseDelayMillis, this.skillId, "FireBlessSpell", "reuseDelayMillis");
        }

        FireBlessSpell {
            if (skillId != (row << 3) + column + 1
                    || skillId != FIRE_BLESS
                    || row != 19 || column != 2 || level != 20 || number != 2
                    || actionId != 19 || effectGfx != 2_286 || mpCost != 40
                    || reuseDelayMillis != 0 || secondaryReuseDelayMillis != 0
                    || durationSeconds != 960 || maximumRange != 0
                    || area != -1 || targetTo != 8
                    || attribute != ATTRIBUTE_UNSPECIFIED || type != 2
                    || nativeId != 4 || meleeDamageModifier != 4
                    || happenMessage != 723 || !counterMagicBypass
                    || key == null || key.isBlank()) {
                throw new IllegalArgumentException(
                        "invalid native Fire Bless metadata");
            }
        }

        int effectiveMpCost(int originalReduction) {
            if (originalReduction < 0 || originalReduction > 2) {
                throw new IllegalArgumentException(
                        "invalid Fire Bless MP inputs");
            }
            // Elf skill155 is outside the general-magic circle reductions.
            return Math.max(0, ResourceDatabase.value(this.mpCost, this.skillId, "FireBlessSpell", "mpCost") - originalReduction);
        }
    }

    record StormEyeSpell(
            int skillId, int row, int column, int level, int number,
            int actionId, int effectGfx, int mpCost,
            int reuseDelayMillis, int secondaryReuseDelayMillis,
            int durationSeconds, int maximumRange, int area, int targetTo,
            int attribute, int type, int nativeId,
            int rangedHitModifier, int rangedDamageModifier, int happenMessage,
            boolean counterMagicBypass, String key) {
        public int actionId() {
            return ResourceDatabase.value(this.actionId, this.skillId, "StormEyeSpell", "actionId");
        }

        public int durationSeconds() {
            return ResourceDatabase.value(this.durationSeconds, this.skillId, "StormEyeSpell", "durationSeconds");
        }

        public int effectGfx() {
            return ResourceDatabase.value(this.effectGfx, this.skillId, "StormEyeSpell", "effectGfx");
        }

        public int mpCost() {
            return ResourceDatabase.value(this.mpCost, this.skillId, "StormEyeSpell", "mpCost");
        }

        public int reuseDelayMillis() {
            return ResourceDatabase.value(this.reuseDelayMillis, this.skillId, "StormEyeSpell", "reuseDelayMillis");
        }

        StormEyeSpell {
            if (skillId != (row << 3) + column + 1
                    || skillId != STORM_EYE
                    || row != 19 || column != 3 || level != 20 || number != 3
                    || actionId != 19 || effectGfx != 2_288 || mpCost != 40
                    || reuseDelayMillis != 0 || secondaryReuseDelayMillis != 0
                    || durationSeconds != 960 || maximumRange != 0
                    || area != -1 || targetTo != 8
                    || attribute != ATTRIBUTE_UNSPECIFIED || type != 2
                    || nativeId != 8 || rangedHitModifier != 2
                    || rangedDamageModifier != 3 || happenMessage != 724
                    || !counterMagicBypass || key == null || key.isBlank()) {
                throw new IllegalArgumentException(
                        "invalid native Storm Eye metadata");
            }
        }

        int effectiveMpCost(int originalReduction) {
            if (originalReduction < 0 || originalReduction > 2) {
                throw new IllegalArgumentException(
                        "invalid Storm Eye MP inputs");
            }
            // Elf skill156 is outside the general-magic circle reductions.
            return Math.max(0, ResourceDatabase.value(this.mpCost, this.skillId, "StormEyeSpell", "mpCost") - originalReduction);
        }
    }

    record StormShotSpell(
            int skillId, int row, int column, int level, int number,
            int actionId, int effectGfx, int mpCost,
            int reuseDelayMillis, int secondaryReuseDelayMillis,
            int durationSeconds, int maximumRange, int area, int targetTo,
            int attribute, int type, int nativeId,
            int rangedHitModifier, int rangedDamageModifier,
            int happenMessage, int failureMessage,
            boolean counterMagicBypass, String key) {
        public int actionId() {
            return ResourceDatabase.value(this.actionId, this.skillId, "StormShotSpell", "actionId");
        }

        public int durationSeconds() {
            return ResourceDatabase.value(this.durationSeconds, this.skillId, "StormShotSpell", "durationSeconds");
        }

        public int effectGfx() {
            return ResourceDatabase.value(this.effectGfx, this.skillId, "StormShotSpell", "effectGfx");
        }

        public int mpCost() {
            return ResourceDatabase.value(this.mpCost, this.skillId, "StormShotSpell", "mpCost");
        }

        public int reuseDelayMillis() {
            return ResourceDatabase.value(this.reuseDelayMillis, this.skillId, "StormShotSpell", "reuseDelayMillis");
        }

        StormShotSpell {
            if (skillId != (row << 3) + column + 1
                    || skillId != STORM_SHOT
                    || row != 20 || column != 5 || level != 21 || number != 5
                    || actionId != 19 || effectGfx != 2_248 || mpCost != 30
                    || reuseDelayMillis != 0 || secondaryReuseDelayMillis != 0
                    || durationSeconds != 960 || maximumRange != 0
                    || area != 0 || targetTo != 0
                    || attribute != ATTRIBUTE_UNSPECIFIED || type != 2
                    || nativeId != 32 || rangedHitModifier != -1
                    || rangedDamageModifier != 5 || happenMessage != 724
                    || failureMessage != 280 || !counterMagicBypass
                    || key == null || key.isBlank()) {
                throw new IllegalArgumentException(
                        "invalid native Storm Shot metadata");
            }
        }

        int effectiveMpCost(int originalReduction) {
            if (originalReduction < 0 || originalReduction > 2) {
                throw new IllegalArgumentException(
                        "invalid Storm Shot MP inputs");
            }
            // Elf skill166 is outside the general-magic circle reductions.
            return Math.max(0, ResourceDatabase.value(this.mpCost, this.skillId, "StormShotSpell", "mpCost") - originalReduction);
        }
    }

    record WindShackleSpell(
            int skillId, int row, int column, int level, int number,
            int actionId, int effectGfx, int mpCost,
            int reuseDelayMillis, int secondaryReuseDelayMillis,
            int clientCastClockMillis, int durationSeconds,
            int maximumRange, int clientClickRange, int area, int targetTo,
            int attribute, int type, int nativeId,
            int happenMessage, int stopMessage, int failureMessage,
            int probabilityHigh, int probabilityEqual, int probabilityLow,
            double intelligenceMultiplier, double magicResistanceMultiplier,
            boolean includeMagicLevel, boolean includeMagicHit,
            int bossModifier, boolean counterMagicBypass,
            long itemConsumeId, int itemConsumeCount, String key) {
        public long itemConsumeId() {
            return CombatDatabase.longValue(this.itemConsumeId, this.skillId, "itemConsumeId");
        }

        public int actionId() {
            return ResourceDatabase.value(this.actionId, this.skillId, "WindShackleSpell", "actionId");
        }

        public int durationSeconds() {
            return ResourceDatabase.value(this.durationSeconds, this.skillId, "WindShackleSpell", "durationSeconds");
        }

        public int effectGfx() {
            return ResourceDatabase.value(this.effectGfx, this.skillId, "WindShackleSpell", "effectGfx");
        }

        public int itemConsumeCount() {
            return ResourceDatabase.value(this.itemConsumeCount, this.skillId, "WindShackleSpell", "itemConsumeCount");
        }

        public int mpCost() {
            return ResourceDatabase.value(this.mpCost, this.skillId, "WindShackleSpell", "mpCost");
        }

        public int reuseDelayMillis() {
            return ResourceDatabase.value(this.reuseDelayMillis, this.skillId, "WindShackleSpell", "reuseDelayMillis");
        }

        WindShackleSpell {
            if (skillId != (row << 3) + column + 1
                    || skillId != WIND_SHACKLE
                    || row != 20 || column != 6 || level != 21 || number != 6
                    || actionId != 19 || effectGfx != 1_799 || mpCost != 50
                    || reuseDelayMillis != 100 || secondaryReuseDelayMillis != 0
                    || clientCastClockMillis != 600 || durationSeconds != 16
                    || maximumRange != -1 || clientClickRange != 8
                    || area != -1 || targetTo != 3
                    || attribute != ATTRIBUTE_WIND || type != 1
                    || nativeId != 64 || happenMessage != 1_001
                    || stopMessage != 0 || failureMessage != 280
                    || probabilityHigh != 55 || probabilityEqual != 45
                    || probabilityLow != 35
                    || intelligenceMultiplier != 0.30D
                    || magicResistanceMultiplier != 0.05D
                    || includeMagicLevel || !includeMagicHit
                    || bossModifier != 0 || counterMagicBypass
                    || itemConsumeId != InventoryCatalog.SPIRIT_CRYSTAL
                    || itemConsumeCount != 1 || key == null || key.isBlank()) {
                throw new IllegalArgumentException(
                        "invalid native Wind Shackle metadata");
            }
        }

        int probability(int casterLevel, int targetLevel,
                        int effectiveIntelligence, int magicLevel,
                        int magicHit, int targetMagicResistance) {
            if (casterLevel <= 0 || targetLevel <= 0
                    || effectiveIntelligence < 0 || magicLevel < 0
                    || magicHit < 0 || targetMagicResistance < 0) {
                throw new IllegalArgumentException(
                        "invalid Wind Shackle probability inputs");
            }
            double result = casterLevel > targetLevel ? probabilityHigh
                    : casterLevel == targetLevel
                        ? probabilityEqual : probabilityLow;
            result += effectiveIntelligence * intelligenceMultiplier;
            if (includeMagicHit) result += magicHit;
            if (includeMagicLevel) result += magicLevel;
            result -= targetMagicResistance * magicResistanceMultiplier;
            result = Math.max(1.0D, Math.min(100.0D, result));
            return Math.max(1, Math.min(90, (int) result));
        }

        int probabilityForTarget(
                int casterLevel, int targetLevel, int effectiveIntelligence,
                int magicLevel, int magicHit, int targetMagicResistance,
                boolean targetBoss) {
            int result = probability(casterLevel, targetLevel,
                    effectiveIntelligence, magicLevel, magicHit,
                    targetMagicResistance);
            if (!targetBoss) return result;
            if (bossModifier == 0) return 0;
            return Math.max(1, result / bossModifier);
        }

        int effectiveMpCost(int originalReduction) {
            if (originalReduction < 0 || originalReduction > 2) {
                throw new IllegalArgumentException(
                        "invalid Wind Shackle MP inputs");
            }
            return Math.max(0, ResourceDatabase.value(this.mpCost, this.skillId, "WindShackleSpell", "mpCost") - originalReduction);
        }
    }

    record NaturesTouchSpell(
            int skillId, int row, int column, int level, int number,
            int actionId, int effectGfx, int mpCost,
            int reuseDelayMillis, int secondaryReuseDelayMillis,
            int durationSeconds, int maximumRange, int area, int targetTo,
            int attribute, int type, int nativeId,
            int hpRegenModifier, int happenMessage, int failureMessage,
            boolean counterMagicBypass, String key) {
        public int actionId() {
            return ResourceDatabase.value(this.actionId, this.skillId, "NaturesTouchSpell", "actionId");
        }

        public int durationSeconds() {
            return ResourceDatabase.value(this.durationSeconds, this.skillId, "NaturesTouchSpell", "durationSeconds");
        }

        public int effectGfx() {
            return ResourceDatabase.value(this.effectGfx, this.skillId, "NaturesTouchSpell", "effectGfx");
        }

        public int mpCost() {
            return ResourceDatabase.value(this.mpCost, this.skillId, "NaturesTouchSpell", "mpCost");
        }

        public int reuseDelayMillis() {
            return ResourceDatabase.value(this.reuseDelayMillis, this.skillId, "NaturesTouchSpell", "reuseDelayMillis");
        }

        NaturesTouchSpell {
            if (skillId != (row << 3) + column + 1
                    || skillId != NATURES_TOUCH
                    || row != 19 || column != 5 || level != 20 || number != 5
                    || actionId != 19 || effectGfx != 2_243 || mpCost != 20
                    || reuseDelayMillis != 0 || secondaryReuseDelayMillis != 0
                    || durationSeconds != 320 || maximumRange != -1
                    || area != 0 || targetTo != 1
                    || attribute != ATTRIBUTE_UNSPECIFIED || type != 2
                    || nativeId != 32
                    || hpRegenModifier != BattleNaturesTouchStatus.NATIVE_HPR_BONUS
                    || happenMessage != 0 || failureMessage != 280
                    || !counterMagicBypass || key == null || key.isBlank()) {
                throw new IllegalArgumentException(
                        "invalid native Nature's Touch metadata");
            }
        }

        int effectiveMpCost(int originalReduction) {
            if (originalReduction < 0 || originalReduction > 2) {
                throw new IllegalArgumentException(
                        "invalid Nature's Touch MP inputs");
            }
            return Math.max(0, ResourceDatabase.value(this.mpCost, this.skillId, "NaturesTouchSpell", "mpCost") - originalReduction);
        }
    }

    record EarthBlessSpell(
            int skillId, int row, int column, int level, int number,
            int actionId, int effectGfx, int mpCost,
            int reuseDelayMillis, int secondaryReuseDelayMillis,
            int durationSeconds, int maximumRange, int area, int targetTo,
            int attribute, int type, int nativeId,
            int armorClassModifier, int legacyIconType, int coexistGraphic,
            int happenMessage, int stopMessage, int failureMessage,
            boolean counterMagicBypass, String key) {
        public int actionId() {
            return ResourceDatabase.value(this.actionId, this.skillId, "EarthBlessSpell", "actionId");
        }

        public int durationSeconds() {
            return ResourceDatabase.value(this.durationSeconds, this.skillId, "EarthBlessSpell", "durationSeconds");
        }

        public int effectGfx() {
            return ResourceDatabase.value(this.effectGfx, this.skillId, "EarthBlessSpell", "effectGfx");
        }

        public int legacyIconType() {
            return ResourceDatabase.value(this.legacyIconType, this.skillId, "EarthBlessSpell", "legacyIconType");
        }

        public int mpCost() {
            return ResourceDatabase.value(this.mpCost, this.skillId, "EarthBlessSpell", "mpCost");
        }

        public int reuseDelayMillis() {
            return ResourceDatabase.value(this.reuseDelayMillis, this.skillId, "EarthBlessSpell", "reuseDelayMillis");
        }

        EarthBlessSpell {
            if (skillId != (row << 3) + column + 1
                    || skillId != EARTH_BLESS
                    || row != 19 || column != 6 || level != 20 || number != 6
                    || actionId != 19 || effectGfx != 2_287 || mpCost != 40
                    || reuseDelayMillis != 0 || secondaryReuseDelayMillis != 0
                    || durationSeconds != 960 || maximumRange != 0
                    || area != -1 || targetTo != 8
                    || attribute != ATTRIBUTE_UNSPECIFIED || type != 2
                    || nativeId != 64 || armorClassModifier != -7
                    || legacyIconType != 7 || coexistGraphic != 527
                    || happenMessage != 0 || stopMessage != 725
                    || failureMessage != 0
                    || !counterMagicBypass || key == null || key.isBlank()) {
                throw new IllegalArgumentException(
                        "invalid native Earth Bless metadata");
            }
        }

        int effectiveMpCost(int originalReduction) {
            if (originalReduction < 0 || originalReduction > 2) {
                throw new IllegalArgumentException("invalid Earth Bless MP inputs");
            }
            return Math.max(0, ResourceDatabase.value(this.mpCost, this.skillId, "EarthBlessSpell", "mpCost") - originalReduction);
        }
    }

    record IronSkinSpell(
            int skillId, int row, int column, int level, int number,
            int actionId, int effectGfx, int mpCost,
            int reuseDelayMillis, int secondaryReuseDelayMillis,
            int durationSeconds, int maximumRange, int area, int targetTo,
            int attribute, int type, int nativeId,
            int armorClassModifier, int legacyIconType, int coexistGraphic,
            int happenMessage, int stopMessage, int failureMessage,
            boolean counterMagicBypass, String key) {
        public int actionId() {
            return ResourceDatabase.value(this.actionId, this.skillId, "IronSkinSpell", "actionId");
        }

        public int coexistGraphic() {
            return ResourceDatabase.value(this.coexistGraphic, this.skillId, "IronSkinSpell", "coexistGraphic");
        }

        public int durationSeconds() {
            return ResourceDatabase.value(this.durationSeconds, this.skillId, "IronSkinSpell", "durationSeconds");
        }

        public int effectGfx() {
            return ResourceDatabase.value(this.effectGfx, this.skillId, "IronSkinSpell", "effectGfx");
        }

        public int legacyIconType() {
            return ResourceDatabase.value(this.legacyIconType, this.skillId, "IronSkinSpell", "legacyIconType");
        }

        public int mpCost() {
            return ResourceDatabase.value(this.mpCost, this.skillId, "IronSkinSpell", "mpCost");
        }

        public int reuseDelayMillis() {
            return ResourceDatabase.value(this.reuseDelayMillis, this.skillId, "IronSkinSpell", "reuseDelayMillis");
        }

        IronSkinSpell {
            if (skillId != (row << 3) + column + 1
                    || skillId != IRON_SKIN
                    || row != 20 || column != 7 || level != 21 || number != 7
                    || actionId != 19 || effectGfx != 2_252 || mpCost != 30
                    || reuseDelayMillis != 0 || secondaryReuseDelayMillis != 0
                    || durationSeconds != 960 || maximumRange != 0
                    || area != 0 || targetTo != 0
                    || attribute != ATTRIBUTE_UNSPECIFIED || type != 2
                    || nativeId != 128 || armorClassModifier != -10
                    || legacyIconType != 10 || coexistGraphic != 528
                    || happenMessage != 714 || stopMessage != 725
                    || failureMessage != 280 || !counterMagicBypass
                    || key == null || key.isBlank()) {
                throw new IllegalArgumentException(
                        "invalid native Iron Skin metadata");
            }
        }

        int effectiveMpCost(int originalReduction) {
            if (originalReduction < 0 || originalReduction > 2) {
                throw new IllegalArgumentException("invalid Iron Skin MP inputs");
            }
            return Math.max(0, ResourceDatabase.value(this.mpCost, this.skillId, "IronSkinSpell", "mpCost") - originalReduction);
        }
    }

    record DragonSkinSpell(
            int skillId, int row, int column, int level, int number,
            int actionId, int effectGfx, int mpCost, int hpCost,
            int reuseDelayMillis, int clientCastClockMillis,
            int durationSeconds, int maximumRange, int area, int targetTo,
            int attribute, int type, int nativeId,
            int damageReduction, int happenMessage, int stopMessage,
            int failureMessage, boolean counterMagicBypass, String key) {
        public int actionId() {
            return ResourceDatabase.value(this.actionId, this.skillId, "DragonSkinSpell", "actionId");
        }

        public int durationSeconds() {
            return ResourceDatabase.value(this.durationSeconds, this.skillId, "DragonSkinSpell", "durationSeconds");
        }

        public int effectGfx() {
            return ResourceDatabase.value(this.effectGfx, this.skillId, "DragonSkinSpell", "effectGfx");
        }

        public int hpCost() {
            return ResourceDatabase.value(this.hpCost, this.skillId, "DragonSkinSpell", "hpCost");
        }

        public int mpCost() {
            return ResourceDatabase.value(this.mpCost, this.skillId, "DragonSkinSpell", "mpCost");
        }

        public int reuseDelayMillis() {
            return ResourceDatabase.value(this.reuseDelayMillis, this.skillId, "DragonSkinSpell", "reuseDelayMillis");
        }

        DragonSkinSpell {
            if (skillId != (row << 3) + column + 1
                    || skillId != DRAGON_SKIN
                    || row != 22 || column != 4 || level != 23 || number != 4
                    || actionId != 19 || effectGfx != 6_531
                    || mpCost != 0 || hpCost != 15
                    || reuseDelayMillis != 10 || clientCastClockMillis != 0
                    || durationSeconds != 1_200 || maximumRange != 0
                    || area != 0 || targetTo != 0
                    || attribute != ATTRIBUTE_UNSPECIFIED || type != 2
                    || nativeId != 16 || damageReduction != 10
                    || happenMessage != 0 || stopMessage != 0
                    || failureMessage != 0 || !counterMagicBypass
                    || key == null || key.isBlank()) {
                throw new IllegalArgumentException(
                        "invalid native Dragon Skin metadata");
            }
        }
    }

    record AwakenSpell(
            int skillId, int row, int column, int level, int number,
            int actionId, int effectGfx, int mpCost, int hpCost,
            long itemConsumeId, int itemConsumeCount,
            int reuseDelayMillis, int clientCastClockMillis,
            int durationSeconds, int maximumRange, int area, int targetTo,
            int attribute, int type, int nativeId,
            int armorClassModifier, int maximumHpBonus, int dragonGfx,
            int mpDrainAmount, int mpDrainIntervalSeconds,
            int magicResistanceModifier, int allElementalResistance,
            int allAbilityScoreBonus,
            int happenMessage, int stopMessage, int failureMessage,
            boolean counterMagicBypass, boolean cancellationImmune,
            String key) {
        public long itemConsumeId() {
            return CombatDatabase.longValue(this.itemConsumeId, this.skillId, "itemConsumeId");
        }

        public int actionId() {
            return ResourceDatabase.value(this.actionId, this.skillId, "AwakenSpell", "actionId");
        }

        public int dragonGfx() {
            return ResourceDatabase.value(this.dragonGfx, this.skillId, "AwakenSpell", "dragonGfx");
        }

        public int durationSeconds() {
            return ResourceDatabase.value(this.durationSeconds, this.skillId, "AwakenSpell", "durationSeconds");
        }

        public int effectGfx() {
            return ResourceDatabase.value(this.effectGfx, this.skillId, "AwakenSpell", "effectGfx");
        }

        public int hpCost() {
            return ResourceDatabase.value(this.hpCost, this.skillId, "AwakenSpell", "hpCost");
        }

        public int itemConsumeCount() {
            return ResourceDatabase.value(this.itemConsumeCount, this.skillId, "AwakenSpell", "itemConsumeCount");
        }

        public int mpCost() {
            return ResourceDatabase.value(this.mpCost, this.skillId, "AwakenSpell", "mpCost");
        }

        public int reuseDelayMillis() {
            return ResourceDatabase.value(this.reuseDelayMillis, this.skillId, "AwakenSpell", "reuseDelayMillis");
        }

        AwakenSpell {
            if (skillId != (row << 3) + column + 1
                    || actionId != 19
                    || mpCost != 0 || hpCost != 1
                    || itemConsumeId != InventoryCatalog.AWAKENING_BONE_FRAGMENT
                    || itemConsumeCount <= 0 || reuseDelayMillis != 0
                    || clientCastClockMillis != 0 || durationSeconds != 0
                    || maximumRange != 0 || area != 0 || targetTo != 0
                    || attribute != ATTRIBUTE_UNSPECIFIED || type != 2
                    || mpDrainAmount != 8 || mpDrainIntervalSeconds != 4
                    || happenMessage != 0 || stopMessage != 0
                    || failureMessage != 0 || !counterMagicBypass
                    || !cancellationImmune || key == null || key.isBlank()) {
                throw new IllegalArgumentException(
                        "invalid common native awakening metadata");
            }
            boolean antharas = skillId == AWAKEN_ANTHARAS
                    && row == 23 && column == 0 && level == 24 && number == 0
                    && effectGfx == 6_975 && itemConsumeCount == 1
                    && nativeId == 1 && armorClassModifier == -12
                    && maximumHpBonus == 127 && dragonGfx == 9_362
                    && magicResistanceModifier == 0
                    && allElementalResistance == 0
                    && allAbilityScoreBonus == 0
                    && key.equals("awaken-antharas");
            boolean fafurion = skillId == AWAKEN_FAFURION
                    && row == 23 && column == 5 && level == 24 && number == 5
                    && effectGfx == 6_976 && itemConsumeCount == 1
                    && nativeId == 32 && armorClassModifier == 0
                    && maximumHpBonus == 0 && dragonGfx == 9_364
                    && magicResistanceModifier == 30
                    && allElementalResistance == 30
                    && allAbilityScoreBonus == 0
                    && key.equals("awaken-fafurion");
            boolean valakas = skillId == AWAKEN_VALAKAS
                    && row == 24 && column == 2 && level == 25 && number == 2
                    && effectGfx == 6_922 && itemConsumeCount == 2
                    && nativeId == 4 && armorClassModifier == 0
                    && maximumHpBonus == 0 && dragonGfx == 9_363
                    && magicResistanceModifier == 0
                    && allElementalResistance == 0
                    && allAbilityScoreBonus == 5
                    && key.equals("awaken-valakas");
            if (!antharas && !fafurion && !valakas) {
                throw new IllegalArgumentException(
                        "invalid native awakening variant metadata");
            }
        }

        boolean permitsSkill(int candidateSkillId) {
            return candidateSkillId == skillId || candidateSkillId == MAGMA_BREATH
                    || candidateSkillId == SHOCK_SKIN
                    || candidateSkillId == FREEZING_BREATH
                    || candidateSkillId == THUNDER_GRAB;
        }
    }

    record MortalBodySpell(
            int skillId, int row, int column, int level, int number,
            int actionId, int effectGfx, int mpCost, int hpCost,
            int reuseDelayMillis, int clientCastClockMillis,
            int durationSeconds, int nativeId,
            int probabilityHigh, int probabilityEqual, int probabilityLow,
            int maximumProbability, int bossModifier,
            int baseReflectionDamage,
            boolean counterMagicBypass, boolean cancellationImmune,
            String key) {
        public int actionId() {
            return ResourceDatabase.value(this.actionId, this.skillId, "MortalBodySpell", "actionId");
        }

        public int baseReflectionDamage() {
            return ResourceDatabase.value(this.baseReflectionDamage, this.skillId, "MortalBodySpell", "baseReflectionDamage");
        }

        public int durationSeconds() {
            return ResourceDatabase.value(this.durationSeconds, this.skillId, "MortalBodySpell", "durationSeconds");
        }

        public int effectGfx() {
            return ResourceDatabase.value(this.effectGfx, this.skillId, "MortalBodySpell", "effectGfx");
        }

        public int hpCost() {
            return ResourceDatabase.value(this.hpCost, this.skillId, "MortalBodySpell", "hpCost");
        }

        public int mpCost() {
            return ResourceDatabase.value(this.mpCost, this.skillId, "MortalBodySpell", "mpCost");
        }

        public int reuseDelayMillis() {
            return ResourceDatabase.value(this.reuseDelayMillis, this.skillId, "MortalBodySpell", "reuseDelayMillis");
        }

        MortalBodySpell {
            if (skillId != (row << 3) + column + 1
                    || skillId != MORTAL_BODY
                    || row != 23 || column != 6 || level != 24 || number != 6
                    || actionId != 19 || effectGfx != 6_519
                    || mpCost != 0 || hpCost != 50
                    || reuseDelayMillis != 1_000
                    || clientCastClockMillis != 30
                    || durationSeconds != 300 || nativeId != 64
                    || probabilityHigh != 35 || probabilityEqual != 25
                    || probabilityLow != 15 || maximumProbability != 90
                    || bossModifier != 0 || baseReflectionDamage != 20
                    || !counterMagicBypass || cancellationImmune
                    || key == null || key.isBlank()) {
                throw new IllegalArgumentException(
                        "invalid native Mortal Body metadata");
            }
        }

        int triggerProbability(int holderLevel, int attackerLevel) {
            if (holderLevel <= 0 || holderLevel > 0xFF
                    || attackerLevel <= 0 || attackerLevel > 0xFF) {
                throw new IllegalArgumentException(
                        "invalid Mortal Body probability input");
            }
            int probability = holderLevel > attackerLevel
                    ? probabilityHigh
                    : holderLevel == attackerLevel
                        ? probabilityEqual : probabilityLow;
            return Math.max(1, Math.min(maximumProbability, probability));
        }
    }

    record BloodlustSpell(
            int skillId, int row, int column, int level, int number,
            int actionId, int effectGfx, int mpCost, int hpCost,
            long itemConsumeId, int itemConsumeCount,
            int reuseDelayMillis, int clientCastClockMillis,
            int durationSeconds, int maximumRange, int area, int targetTo,
            int attribute, int type, int nativeId,
            int happenMessage, int stopMessage, int failureMessage,
            boolean counterMagicBypass, boolean cancellationImmune,
            int braveMode, double attackMultiplier, String key) {
        public long itemConsumeId() {
            return CombatDatabase.longValue(this.itemConsumeId, this.skillId, "itemConsumeId");
        }

        public int actionId() {
            return ResourceDatabase.value(this.actionId, this.skillId, "BloodlustSpell", "actionId");
        }

        public int durationSeconds() {
            return ResourceDatabase.value(this.durationSeconds, this.skillId, "BloodlustSpell", "durationSeconds");
        }

        public int effectGfx() {
            return ResourceDatabase.value(this.effectGfx, this.skillId, "BloodlustSpell", "effectGfx");
        }

        public int hpCost() {
            return ResourceDatabase.value(this.hpCost, this.skillId, "BloodlustSpell", "hpCost");
        }

        public int itemConsumeCount() {
            return ResourceDatabase.value(this.itemConsumeCount, this.skillId, "BloodlustSpell", "itemConsumeCount");
        }

        public int mpCost() {
            return ResourceDatabase.value(this.mpCost, this.skillId, "BloodlustSpell", "mpCost");
        }

        public int reuseDelayMillis() {
            return ResourceDatabase.value(this.reuseDelayMillis, this.skillId, "BloodlustSpell", "reuseDelayMillis");
        }

        BloodlustSpell {
            if (skillId != (row << 3) + column + 1 || skillId != BLOODLUST
                    || row != 23 || column != 1 || level != 24 || number != 1
                    || actionId != 19 || effectGfx != 6_523
                    || mpCost != 0 || hpCost != 30
                    || itemConsumeId != InventoryCatalog.AWAKENING_BONE_FRAGMENT
                    || itemConsumeCount != 1 || reuseDelayMillis != 10
                    || clientCastClockMillis != 0 || durationSeconds != 60
                    || maximumRange != 0 || area != 0 || targetTo != 0
                    || attribute != ATTRIBUTE_UNSPECIFIED || type != 2
                    || nativeId != 2 || happenMessage != 0 || stopMessage != 0
                    || failureMessage != 0 || !counterMagicBypass
                    || cancellationImmune
                    || braveMode != BattleTimedStatus.BRAVE_BLOODLUST
                    || Double.compare(attackMultiplier,
                            BattleActionInterval.FAST_ATTACK_MULTIPLIER) != 0
                    || key == null || key.isBlank()) {
                throw new IllegalArgumentException("invalid native Bloodlust metadata");
            }
        }

        int attackIntervalMillis(int baseIntervalMillis) {
            if (baseIntervalMillis <= 0) {
                throw new IllegalArgumentException("invalid base attack interval");
            }
            return Math.max(1, (int) (baseIntervalMillis * attackMultiplier));
        }
    }

    record FoeSlayerSpell(
            int skillId, int row, int column, int level, int number,
            int actionId, int effectGfx, int mpCost, int hpCost,
            int reuseDelayMillis, int clientCastClockMillis,
            int durationSeconds, int maximumRange, int area, int targetTo,
            int attribute, int type, int nativeId,
            int hitCount, int exposureDamageStep,
            int attackerEffectGfx, int targetEffectGfx,
            int happenMessage, int stopMessage, int failureMessage,
            boolean counterMagicBypass, String key) {
        public int actionId() {
            return ResourceDatabase.value(this.actionId, this.skillId, "FoeSlayerSpell", "actionId");
        }

        public int attackerEffectGfx() {
            return ResourceDatabase.value(this.attackerEffectGfx, this.skillId, "FoeSlayerSpell", "attackerEffectGfx");
        }

        public int durationSeconds() {
            return ResourceDatabase.value(this.durationSeconds, this.skillId, "FoeSlayerSpell", "durationSeconds");
        }

        public int exposureDamageStep() {
            return ResourceDatabase.value(this.exposureDamageStep, this.skillId, "FoeSlayerSpell", "exposureDamageStep");
        }

        public int hitCount() {
            return ResourceDatabase.value(this.hitCount, this.skillId, "FoeSlayerSpell", "hitCount");
        }

        public int hpCost() {
            return ResourceDatabase.value(this.hpCost, this.skillId, "FoeSlayerSpell", "hpCost");
        }

        public int mpCost() {
            return ResourceDatabase.value(this.mpCost, this.skillId, "FoeSlayerSpell", "mpCost");
        }

        public int reuseDelayMillis() {
            return ResourceDatabase.value(this.reuseDelayMillis, this.skillId, "FoeSlayerSpell", "reuseDelayMillis");
        }

        public int targetEffectGfx() {
            return ResourceDatabase.value(this.targetEffectGfx, this.skillId, "FoeSlayerSpell", "targetEffectGfx");
        }

        FoeSlayerSpell {
            if (skillId != (row << 3) + column + 1 || skillId != FOE_SLAYER
                    || row != 23 || column != 2 || level != 24 || number != 2
                    || actionId != 18 || effectGfx != 0
                    || mpCost != 0 || hpCost != 20
                    || reuseDelayMillis != 2_000
                    || clientCastClockMillis != 0
                    || durationSeconds != 0 || maximumRange != 2
                    || area != 0 || targetTo != 3
                    || attribute != ATTRIBUTE_UNSPECIFIED || type != 64
                    || nativeId != 4 || hitCount != 3
                    || exposureDamageStep != 20
                    || attackerEffectGfx != 7_020
                    || targetEffectGfx != 6_509
                    || happenMessage != 0 || stopMessage != 0
                    || failureMessage != 0 || !counterMagicBypass
                    || key == null || key.isBlank()) {
                throw new IllegalArgumentException(
                        "invalid native Foe Slayer metadata");
            }
        }

        int damageForExposureLevel(int level) {
            if (level < 0 || level > 3) {
                throw new IllegalArgumentException("invalid exposure level");
            }
            return level * exposureDamageStep;
        }
    }

    record BurningSlashSpell(
            int skillId, int row, int column, int level, int number,
            int actionId, int effectGfx, int mpCost, int hpCost,
            int reuseDelayMillis, int clientCastClockMillis,
            int durationSeconds, int maximumRange, int area, int targetTo,
            int attribute, int type, int nativeId,
            int nextMeleeDamage, int impactGfx, int happenMessage,
            int stopMessage, int failureMessage,
            boolean counterMagicBypass, String key) {
        public int actionId() {
            return ResourceDatabase.value(this.actionId, this.skillId, "BurningSlashSpell", "actionId");
        }

        public int durationSeconds() {
            return ResourceDatabase.value(this.durationSeconds, this.skillId, "BurningSlashSpell", "durationSeconds");
        }

        public int effectGfx() {
            return ResourceDatabase.value(this.effectGfx, this.skillId, "BurningSlashSpell", "effectGfx");
        }

        public int hpCost() {
            return ResourceDatabase.value(this.hpCost, this.skillId, "BurningSlashSpell", "hpCost");
        }

        public int impactGfx() {
            return ResourceDatabase.value(this.impactGfx, this.skillId, "BurningSlashSpell", "impactGfx");
        }

        public int mpCost() {
            return ResourceDatabase.value(this.mpCost, this.skillId, "BurningSlashSpell", "mpCost");
        }

        public int reuseDelayMillis() {
            return ResourceDatabase.value(this.reuseDelayMillis, this.skillId, "BurningSlashSpell", "reuseDelayMillis");
        }

        BurningSlashSpell {
            if (skillId != (row << 3) + column + 1
                    || skillId != BURNING_SLASH
                    || row != 22 || column != 5 || level != 23 || number != 5
                    || actionId != 19 || effectGfx != 6_590
                    || mpCost != 0 || hpCost != 10
                    || reuseDelayMillis != 2_000
                    || clientCastClockMillis != 3_600
                    || durationSeconds != 60 || maximumRange != 0
                    || area != 0 || targetTo != 0 || attribute != ATTRIBUTE_FIRE
                    || type != 2 || nativeId != 32 || nextMeleeDamage != 10
                    || impactGfx != 6_591 || happenMessage != 0
                    || stopMessage != 0 || failureMessage != 0
                    || !counterMagicBypass || key == null || key.isBlank()) {
                throw new IllegalArgumentException(
                        "invalid native Burning Slash metadata");
            }
        }
    }

    record GuardBrakeSpell(
            int skillId, int row, int column, int level, int number,
            int actionId, int effectGfx, int mpCost, int hpCost,
            int reuseDelayMillis, int clientCastClockMillis,
            int durationSeconds, int maximumRange, int area, int targetTo,
            int attribute, int type, int nativeId,
            int probabilityHigh, int probabilityEqual, int probabilityLow,
            double intelligenceMultiplier, boolean includeMagicHit,
            boolean includeMagicLevel, double magicResistanceMultiplier,
            int bossModifier, int armorClassModifier,
            int happenMessage, int stopMessage, int failureMessage,
            boolean counterMagicBypass, String key) {
        public int actionId() {
            return ResourceDatabase.value(this.actionId, this.skillId, "GuardBrakeSpell", "actionId");
        }

        public int durationSeconds() {
            return ResourceDatabase.value(this.durationSeconds, this.skillId, "GuardBrakeSpell", "durationSeconds");
        }

        public int effectGfx() {
            return ResourceDatabase.value(this.effectGfx, this.skillId, "GuardBrakeSpell", "effectGfx");
        }

        public int hpCost() {
            return ResourceDatabase.value(this.hpCost, this.skillId, "GuardBrakeSpell", "hpCost");
        }

        public int mpCost() {
            return ResourceDatabase.value(this.mpCost, this.skillId, "GuardBrakeSpell", "mpCost");
        }

        public int reuseDelayMillis() {
            return ResourceDatabase.value(this.reuseDelayMillis, this.skillId, "GuardBrakeSpell", "reuseDelayMillis");
        }

        GuardBrakeSpell {
            boolean guardBrake = skillId == GUARD_BRAKE
                    && row == 22 && column == 6 && level == 23 && number == 6
                    && effectGfx == 6_508 && hpCost == 25
                    && clientCastClockMillis == 1_800 && nativeId == 64
                    && armorClassModifier == 15
                    && "guard-brake".equals(key);
            boolean horrorOfDeath = skillId == HORROR_OF_DEATH
                    && row == 24 && column == 0 && level == 25 && number == 0
                    && effectGfx == 6_588 && hpCost == 50
                    && clientCastClockMillis == 0 && nativeId == 1
                    && armorClassModifier == 0
                    && "horror-of-death".equals(key);
            if (skillId != (row << 3) + column + 1
                    || (!guardBrake && !horrorOfDeath)
                    || actionId != 19 || mpCost != 0
                    || reuseDelayMillis != 1_000
                    || durationSeconds != 60 || maximumRange != 3
                    || area != 0 || targetTo != 26
                    || attribute != ATTRIBUTE_UNSPECIFIED || type != 3
                    || probabilityHigh != 55 || probabilityEqual != 45
                    || probabilityLow != 35
                    || Double.compare(intelligenceMultiplier, 0.35D) != 0
                    || !includeMagicHit || includeMagicLevel
                    || Double.compare(magicResistanceMultiplier, 0.05D) != 0
                    || bossModifier != 0
                    || happenMessage != 0 || stopMessage != 0
                    || failureMessage != 280 || counterMagicBypass
                    || key == null || key.isBlank()) {
                throw new IllegalArgumentException(
                        "invalid native Guard Brake/Horror of Death metadata");
            }
        }

        int effectiveMpCost(int originalReduction) {
            if (originalReduction < 0 || originalReduction > 2) {
                throw new IllegalArgumentException("invalid Guard Brake MP inputs");
            }
            return Math.max(0, ResourceDatabase.value(this.mpCost, this.skillId, "GuardBrakeSpell", "mpCost") - originalReduction);
        }

        int probabilityForTarget(
                int casterLevel, int targetLevel, int effectiveIntelligence,
                int magicHit, int targetMagicResistance, boolean targetBoss) {
            if (casterLevel <= 0 || targetLevel <= 0
                    || effectiveIntelligence < 1 || effectiveIntelligence > 254
                    || magicHit < 0 || targetMagicResistance < 0) {
                throw new IllegalArgumentException(
                        "invalid Guard Brake probability inputs");
            }
            if (targetBoss && bossModifier == 0) return 0;
            double result = casterLevel > targetLevel ? probabilityHigh
                    : casterLevel == targetLevel ? probabilityEqual : probabilityLow;
            result += effectiveIntelligence * intelligenceMultiplier;
            if (includeMagicHit) result += magicHit;
            if (includeMagicLevel) {
                throw new IllegalStateException(
                        "selected Guard Brake row must not include magic level");
            }
            result -= targetMagicResistance * magicResistanceMultiplier;
            return Math.max(1, Math.min(100, (int) result));
        }

        boolean horrorOfDeath() {
            return skillId == HORROR_OF_DEATH;
        }

        int strengthModifier() {
            return horrorOfDeath() ? -5 : 0;
        }

        int intelligenceModifier() {
            return horrorOfDeath() ? -5 : 0;
        }
    }

    record ResistFearSpell(
            int skillId, int row, int column, int level, int number,
            int actionId, int effectGfx, int mpCost, int hpCost,
            int reuseDelayMillis, int clientCastClockMillis,
            int durationSeconds, int maximumRange, int area, int targetTo,
            int attribute, int type, int nativeId,
            int probabilityHigh, int probabilityEqual, int probabilityLow,
            double intelligenceMultiplier, boolean includeMagicHit,
            boolean includeMagicLevel, double magicResistanceMultiplier,
            int bossModifier, int dodgeDownModifier,
            int happenMessage, int stopMessage, int failureMessage,
            boolean counterMagicBypass, String key) {
        public int actionId() {
            return ResourceDatabase.value(this.actionId, this.skillId, "ResistFearSpell", "actionId");
        }

        public int durationSeconds() {
            return ResourceDatabase.value(this.durationSeconds, this.skillId, "ResistFearSpell", "durationSeconds");
        }

        public int effectGfx() {
            return ResourceDatabase.value(this.effectGfx, this.skillId, "ResistFearSpell", "effectGfx");
        }

        public int hpCost() {
            return ResourceDatabase.value(this.hpCost, this.skillId, "ResistFearSpell", "hpCost");
        }

        public int mpCost() {
            return ResourceDatabase.value(this.mpCost, this.skillId, "ResistFearSpell", "mpCost");
        }

        public int reuseDelayMillis() {
            return ResourceDatabase.value(this.reuseDelayMillis, this.skillId, "ResistFearSpell", "reuseDelayMillis");
        }

        ResistFearSpell {
            if (skillId != (row << 3) + column + 1
                    || skillId != RESIST_FEAR
                    || row != 23 || column != 3 || level != 24 || number != 3
                    || actionId != 19 || effectGfx != 6_586
                    || mpCost != 0 || hpCost != 30
                    || reuseDelayMillis != 1_000
                    || clientCastClockMillis != 1_200
                    || durationSeconds != 60 || maximumRange != 3
                    || area != 0 || targetTo != 35
                    || attribute != ATTRIBUTE_UNSPECIFIED || type != 3
                    || nativeId != 8
                    || probabilityHigh != 45 || probabilityEqual != 38
                    || probabilityLow != 30
                    || Double.compare(intelligenceMultiplier, 0.40D) != 0
                    || !includeMagicHit || includeMagicLevel
                    || Double.compare(magicResistanceMultiplier, 0.05D) != 0
                    || bossModifier != 0 || dodgeDownModifier != 5
                    || happenMessage != 0 || stopMessage != 0
                    || failureMessage != 280 || !counterMagicBypass
                    || key == null || key.isBlank()) {
                throw new IllegalArgumentException(
                        "invalid native Resist Fear metadata");
            }
        }

        int effectiveMpCost(int originalReduction) {
            if (originalReduction < 0 || originalReduction > 2) {
                throw new IllegalArgumentException("invalid Resist Fear MP inputs");
            }
            return Math.max(0, ResourceDatabase.value(this.mpCost, this.skillId, "ResistFearSpell", "mpCost") - originalReduction);
        }

        int probabilityForTarget(
                int casterLevel, int targetLevel, int effectiveIntelligence,
                int magicHit, int targetMagicResistance, boolean targetBoss) {
            if (casterLevel <= 0 || targetLevel <= 0
                    || effectiveIntelligence < 1 || effectiveIntelligence > 254
                    || magicHit < 0 || targetMagicResistance < 0) {
                throw new IllegalArgumentException(
                        "invalid Resist Fear probability inputs");
            }
            if (targetBoss && bossModifier == 0) return 0;
            double result = casterLevel > targetLevel ? probabilityHigh
                    : casterLevel == targetLevel ? probabilityEqual : probabilityLow;
            result += effectiveIntelligence * intelligenceMultiplier;
            if (includeMagicHit) result += magicHit;
            if (includeMagicLevel) {
                throw new IllegalStateException(
                        "selected Resist Fear row must not include magic level");
            }
            result -= targetMagicResistance * magicResistanceMultiplier;
            return Math.max(1, Math.min(100, (int) result));
        }
    }

    record ExoticVitalizeSpell(
            int skillId, int row, int column, int level, int number,
            int actionId, int effectGfx, int mpCost,
            int reuseDelayMillis, int secondaryReuseDelayMillis,
            int clientCastClockMillis, int durationSeconds,
            int maximumRange, int area, int targetTo,
            int attribute, int type, int nativeId,
            long itemConsumeId, int itemConsumeCount,
            int happenMessage, int stopMessage, int failureMessage,
            boolean counterMagicBypass, String key) {
        public long itemConsumeId() {
            return CombatDatabase.longValue(this.itemConsumeId, this.skillId, "itemConsumeId");
        }

        public int actionId() {
            return ResourceDatabase.value(this.actionId, this.skillId, "ExoticVitalizeSpell", "actionId");
        }

        public int durationSeconds() {
            return ResourceDatabase.value(this.durationSeconds, this.skillId, "ExoticVitalizeSpell", "durationSeconds");
        }

        public int effectGfx() {
            return ResourceDatabase.value(this.effectGfx, this.skillId, "ExoticVitalizeSpell", "effectGfx");
        }

        public int itemConsumeCount() {
            return ResourceDatabase.value(this.itemConsumeCount, this.skillId, "ExoticVitalizeSpell", "itemConsumeCount");
        }

        public int mpCost() {
            return ResourceDatabase.value(this.mpCost, this.skillId, "ExoticVitalizeSpell", "mpCost");
        }

        public int reuseDelayMillis() {
            return ResourceDatabase.value(this.reuseDelayMillis, this.skillId, "ExoticVitalizeSpell", "reuseDelayMillis");
        }

        ExoticVitalizeSpell {
            if (skillId != (row << 3) + column + 1
                    || skillId != EXOTIC_VITALIZE
                    || row != 21 || column != 0 || level != 22 || number != 0
                    || actionId != 19 || effectGfx != 4_400 || mpCost != 30
                    || reuseDelayMillis != 100 || secondaryReuseDelayMillis != 0
                    || clientCastClockMillis != 100 || durationSeconds != 960
                    || maximumRange != 0 || area != 0 || targetTo != 0
                    || attribute != ATTRIBUTE_UNSPECIFIED || type != 2
                    || nativeId != 1
                    || itemConsumeId != InventoryCatalog.SPIRIT_CRYSTAL
                    || itemConsumeCount != 1 || happenMessage != 0
                    || stopMessage != 0 || failureMessage != 0
                    || !counterMagicBypass || key == null || key.isBlank()) {
                throw new IllegalArgumentException(
                        "invalid native Exotic Vitalize metadata");
            }
        }

        int effectiveMpCost(int originalReduction) {
            if (originalReduction < 0 || originalReduction > 2) {
                throw new IllegalArgumentException(
                        "invalid Exotic Vitalize MP inputs");
            }
            return Math.max(0, ResourceDatabase.value(this.mpCost, this.skillId, "ExoticVitalizeSpell", "mpCost") - originalReduction);
        }
    }

    record AdditionalFireSpell(
            int skillId, int row, int column, int level, int number,
            int actionId, int effectGfx, int mpCost,
            int reuseDelayMillis, int secondaryReuseDelayMillis,
            int clientCastClockMillis, int durationSeconds,
            int maximumRange, int area, int targetTo,
            int attribute, int type, int nativeId,
            long itemConsumeId, int itemConsumeCount,
            int happenMessage, int stopMessage, int failureMessage,
            boolean counterMagicBypass, String key) {
        public long itemConsumeId() {
            return CombatDatabase.longValue(this.itemConsumeId, this.skillId, "itemConsumeId");
        }

        public int actionId() {
            return ResourceDatabase.value(this.actionId, this.skillId, "AdditionalFireSpell", "actionId");
        }

        public int durationSeconds() {
            return ResourceDatabase.value(this.durationSeconds, this.skillId, "AdditionalFireSpell", "durationSeconds");
        }

        public int effectGfx() {
            return ResourceDatabase.value(this.effectGfx, this.skillId, "AdditionalFireSpell", "effectGfx");
        }

        public int itemConsumeCount() {
            return ResourceDatabase.value(this.itemConsumeCount, this.skillId, "AdditionalFireSpell", "itemConsumeCount");
        }

        public int mpCost() {
            return ResourceDatabase.value(this.mpCost, this.skillId, "AdditionalFireSpell", "mpCost");
        }

        public int reuseDelayMillis() {
            return ResourceDatabase.value(this.reuseDelayMillis, this.skillId, "AdditionalFireSpell", "reuseDelayMillis");
        }

        AdditionalFireSpell {
            if (skillId != (row << 3) + column + 1
                    || skillId != ADDITIONAL_FIRE
                    || row != 21 || column != 7 || level != 22 || number != 7
                    || actionId != 19 || effectGfx != 5_825 || mpCost != 30
                    || reuseDelayMillis != 100 || secondaryReuseDelayMillis != 0
                    || clientCastClockMillis != 100 || durationSeconds != 960
                    || maximumRange != 0 || area != 0 || targetTo != 0
                    || attribute != ATTRIBUTE_UNSPECIFIED || type != 2
                    || nativeId != 128
                    || itemConsumeId != InventoryCatalog.SPIRIT_CRYSTAL
                    || itemConsumeCount != 1 || happenMessage != 0
                    || stopMessage != 0 || failureMessage != 0
                    || !counterMagicBypass || key == null || key.isBlank()) {
                throw new IllegalArgumentException(
                        "invalid native Additional Fire metadata");
            }
        }

        int effectiveMpCost(int originalReduction) {
            if (originalReduction < 0 || originalReduction > 2) {
                throw new IllegalArgumentException(
                        "invalid Additional Fire MP inputs");
            }
            return Math.max(0, ResourceDatabase.value(this.mpCost, this.skillId, "AdditionalFireSpell", "mpCost") - originalReduction);
        }
    }

    record WaterLifeSpell(
            int skillId, int row, int column, int level, int number,
            int actionId, int effectGfx, int mpCost,
            int reuseDelayMillis, int secondaryReuseDelayMillis,
            int clientCastClockMillis, int durationSeconds,
            int maximumRange, int area, int targetTo,
            int attribute, int type, int nativeId,
            long itemConsumeId, int itemConsumeCount,
            int happenMessage, int stopMessage, int failureMessage,
            boolean counterMagicBypass, String key) {
        public long itemConsumeId() {
            return CombatDatabase.longValue(this.itemConsumeId, this.skillId, "itemConsumeId");
        }

        public int actionId() {
            return ResourceDatabase.value(this.actionId, this.skillId, "WaterLifeSpell", "actionId");
        }

        public int durationSeconds() {
            return ResourceDatabase.value(this.durationSeconds, this.skillId, "WaterLifeSpell", "durationSeconds");
        }

        public int effectGfx() {
            return ResourceDatabase.value(this.effectGfx, this.skillId, "WaterLifeSpell", "effectGfx");
        }

        public int itemConsumeCount() {
            return ResourceDatabase.value(this.itemConsumeCount, this.skillId, "WaterLifeSpell", "itemConsumeCount");
        }

        public int mpCost() {
            return ResourceDatabase.value(this.mpCost, this.skillId, "WaterLifeSpell", "mpCost");
        }

        public int reuseDelayMillis() {
            return ResourceDatabase.value(this.reuseDelayMillis, this.skillId, "WaterLifeSpell", "reuseDelayMillis");
        }

        WaterLifeSpell {
            if (skillId != (row << 3) + column + 1
                    || skillId != WATER_LIFE
                    || row != 21 || column != 1 || level != 22 || number != 1
                    || actionId != 19 || effectGfx != 4_401 || mpCost != 1
                    || reuseDelayMillis != 100 || secondaryReuseDelayMillis != 0
                    || clientCastClockMillis != 400 || durationSeconds != 64
                    || maximumRange != -1 || area != 0
                    || targetTo != 1
                    || attribute != ATTRIBUTE_WATER || type != 2
                    || nativeId != 2
                    || itemConsumeId != InventoryCatalog.SPIRIT_CRYSTAL
                    || itemConsumeCount != 1 || happenMessage != 0
                    || stopMessage != 0 || failureMessage != 280
                    || !counterMagicBypass || key == null || key.isBlank()) {
                throw new IllegalArgumentException(
                        "invalid native Water Life metadata");
            }
        }

        int effectiveMpCost(int originalReduction) {
            if (originalReduction < 0 || originalReduction > 2) {
                throw new IllegalArgumentException(
                        "invalid Water Life MP inputs");
            }
            return Math.max(0, ResourceDatabase.value(this.mpCost, this.skillId, "WaterLifeSpell", "mpCost") - originalReduction);
        }
    }

    record AquaProtectSpell(
            int skillId, int row, int column, int level, int number,
            int actionId, int effectGfx, int mpCost,
            int reuseDelayMillis, int secondaryReuseDelayMillis,
            int durationSeconds, int maximumRange, int area, int targetTo,
            int attribute, int type, int nativeId,
            int evasionRateModifier, boolean counterMagicBypass, String key) {
        public int actionId() {
            return ResourceDatabase.value(this.actionId, this.skillId, "AquaProtectSpell", "actionId");
        }

        public int durationSeconds() {
            return ResourceDatabase.value(this.durationSeconds, this.skillId, "AquaProtectSpell", "durationSeconds");
        }

        public int effectGfx() {
            return ResourceDatabase.value(this.effectGfx, this.skillId, "AquaProtectSpell", "effectGfx");
        }

        public int mpCost() {
            return ResourceDatabase.value(this.mpCost, this.skillId, "AquaProtectSpell", "mpCost");
        }

        public int reuseDelayMillis() {
            return ResourceDatabase.value(this.reuseDelayMillis, this.skillId, "AquaProtectSpell", "reuseDelayMillis");
        }

        AquaProtectSpell {
            if (skillId != (row << 3) + column + 1
                    || skillId != AQUA_PROTECTER
                    || row != 19 || column != 7 || level != 20 || number != 7
                    || actionId != 19 || effectGfx != 5_829 || mpCost != 30
                    || reuseDelayMillis != 0 || secondaryReuseDelayMillis != 0
                    || durationSeconds != 960 || maximumRange != -1
                    || area != 0 || targetTo != 1
                    || attribute != ATTRIBUTE_UNSPECIFIED || type != 2
                    || nativeId != 128
                    || evasionRateModifier != BattleAquaProtectStatus.NATIVE_ER_BONUS
                    || !counterMagicBypass || key == null || key.isBlank()) {
                throw new IllegalArgumentException(
                        "invalid native Aqua Protecter metadata");
            }
        }

        int effectiveMpCost(int originalReduction) {
            if (originalReduction < 0 || originalReduction > 2) {
                throw new IllegalArgumentException(
                        "invalid Aqua Protecter MP inputs");
            }
            return Math.max(0, ResourceDatabase.value(this.mpCost, this.skillId, "AquaProtectSpell", "mpCost") - originalReduction);
        }
    }

    record AreaSilenceSpell(
            int skillId, int row, int column, int level, int number,
            int actionId, int effectGfx, int mpCost, int reuseDelayMillis,
            int durationSeconds, int maximumRange, int area, int targetTo,
            int through, int attribute, int type, int nativeId,
            int probabilityHigh, int probabilityEqual, int probabilityLow,
            double intelligenceMultiplier, double magicResistanceMultiplier,
            boolean includeMagicHit, int bossModifier,
            long itemConsumeId, int itemConsumeCount, String key) {
        public long itemConsumeId() {
            return CombatDatabase.longValue(this.itemConsumeId, this.skillId, "itemConsumeId");
        }

        public int actionId() {
            return ResourceDatabase.value(this.actionId, this.skillId, "AreaSilenceSpell", "actionId");
        }

        public int durationSeconds() {
            return ResourceDatabase.value(this.durationSeconds, this.skillId, "AreaSilenceSpell", "durationSeconds");
        }

        public int effectGfx() {
            return ResourceDatabase.value(this.effectGfx, this.skillId, "AreaSilenceSpell", "effectGfx");
        }

        public int itemConsumeCount() {
            return ResourceDatabase.value(this.itemConsumeCount, this.skillId, "AreaSilenceSpell", "itemConsumeCount");
        }

        public int mpCost() {
            return ResourceDatabase.value(this.mpCost, this.skillId, "AreaSilenceSpell", "mpCost");
        }

        public int reuseDelayMillis() {
            return ResourceDatabase.value(this.reuseDelayMillis, this.skillId, "AreaSilenceSpell", "reuseDelayMillis");
        }

        AreaSilenceSpell {
            if (skillId != (row << 3) + column + 1
                    || skillId != AREA_OF_SILENCE || row != 20 || column != 0
                    || level != 21 || number != 0 || actionId != 19
                    || effectGfx != 2_241 || mpCost != 40
                    || reuseDelayMillis != 9_000 || durationSeconds != 16
                    || maximumRange != 0 || area != -1 || targetTo != 3
                    || through != 1 || attribute != ATTRIBUTE_UNSPECIFIED
                    || type != 1 || nativeId != 1
                    || probabilityHigh != 55 || probabilityEqual != 45
                    || probabilityLow != 35
                    || intelligenceMultiplier != 0.3D
                    || magicResistanceMultiplier != 0.05D
                    || !includeMagicHit || bossModifier != 0
                    || itemConsumeId != InventoryCatalog.SPIRIT_CRYSTAL
                    || itemConsumeCount != 8 || key == null || key.isBlank()) {
                throw new IllegalArgumentException(
                        "invalid native Area of Silence metadata");
            }
        }

        int effectiveMpCost(int originalReduction) {
            if (originalReduction < 0 || originalReduction > 2) {
                throw new IllegalArgumentException(
                        "invalid Area of Silence MP inputs");
            }
            return Math.max(0, ResourceDatabase.value(this.mpCost, this.skillId, "AreaSilenceSpell", "mpCost") - originalReduction);
        }

        int probability(int casterLevel, int targetLevel,
                        int effectiveIntelligence, int magicHit,
                        int targetMagicResistance, boolean targetBoss) {
            if (casterLevel <= 0 || targetLevel <= 0
                    || effectiveIntelligence < 0 || targetMagicResistance < 0) {
                throw new IllegalArgumentException(
                        "invalid Area of Silence probability inputs");
            }
            if (targetBoss && bossModifier == 0) return 0;
            double result = casterLevel > targetLevel ? probabilityHigh
                    : casterLevel == targetLevel ? probabilityEqual : probabilityLow;
            result += effectiveIntelligence * intelligenceMultiplier;
            if (includeMagicHit) result += magicHit;
            result -= targetMagicResistance * magicResistanceMultiplier;
            return Math.max(1, Math.min(90, (int) result));
        }
    }

    record WorldTreeCallSpell(
            int skillId, int row, int column, int skillLevel, int skillNumber,
            int actionId, int effectGfx, int mpCost, int hpCost,
            int reuseDelayMillis, int durationSeconds, int maximumRange,
            int area, int targetTo, int attribute, int sourceType, int nativeId,
            long itemConsumeId, int itemConsumeCount, int destinationMapId,
            int destinationX, int destinationY, int destinationHeading,
            int mapRejectedMessage, boolean counterMagicBypass, String key) {
        public long itemConsumeId() {
            return CombatDatabase.longValue(this.itemConsumeId, this.skillId, "itemConsumeId");
        }

        public int actionId() {
            return ResourceDatabase.value(this.actionId, this.skillId, "WorldTreeCallSpell", "actionId");
        }

        public int durationSeconds() {
            return ResourceDatabase.value(this.durationSeconds, this.skillId, "WorldTreeCallSpell", "durationSeconds");
        }

        public int effectGfx() {
            return ResourceDatabase.value(this.effectGfx, this.skillId, "WorldTreeCallSpell", "effectGfx");
        }

        public int hpCost() {
            return ResourceDatabase.value(this.hpCost, this.skillId, "WorldTreeCallSpell", "hpCost");
        }

        public int itemConsumeCount() {
            return ResourceDatabase.value(this.itemConsumeCount, this.skillId, "WorldTreeCallSpell", "itemConsumeCount");
        }

        public int mpCost() {
            return ResourceDatabase.value(this.mpCost, this.skillId, "WorldTreeCallSpell", "mpCost");
        }

        public int reuseDelayMillis() {
            return ResourceDatabase.value(this.reuseDelayMillis, this.skillId, "WorldTreeCallSpell", "reuseDelayMillis");
        }

        WorldTreeCallSpell {
            if (skillId != WORLD_TREE_CALL
                    || skillId != (row << 3) + column + 1
                    || row != 16 || column != 2 || skillLevel != 17
                    || skillNumber != 2 || actionId != 19 || effectGfx != 169
                    || mpCost != 10 || hpCost != 0 || reuseDelayMillis != 0
                    || durationSeconds != 0 || maximumRange != 0 || area != 0
                    || targetTo != 0 || attribute != ATTRIBUTE_UNSPECIFIED
                    || sourceType != 128 || nativeId != 4
                    || itemConsumeId != 0 || itemConsumeCount != 0
                    || destinationMapId != 4 || destinationX != 33_051
                    || destinationY != 32_337 || destinationHeading != 5
                    || mapRejectedMessage != 276 || !counterMagicBypass
                    || key == null || key.isBlank()) {
                throw new IllegalArgumentException(
                        "invalid native World Tree's Call metadata");
            }
        }

        int effectiveMpCost(int effectiveIntelligence, int originalReduction) {
            if (effectiveIntelligence < 1 || effectiveIntelligence > 254
                    || originalReduction < 0 || originalReduction > 2) {
                throw new IllegalArgumentException(
                        "invalid World Tree's Call MP inputs");
            }
            // Skill 131 is outside general-magic circle reductions.  The
            // retained original-INT class reduction is still applied last.
            return Math.max(0, ResourceDatabase.value(this.mpCost, this.skillId, "WorldTreeCallSpell", "mpCost") - originalReduction);
        }
    }

    record AbilityBuffSpell(int skillId, int row, int column, int actionId,
                            int effectGfx, int mpCost, int reuseDelayMillis,
                            int durationSeconds, Ability ability, int modifier,
                            int activeType, int stopType,
                            int happenMessage, int stopMessage,
                            RequestShape requestShape, String key) {
        public int actionId() {
            return ResourceDatabase.value(this.actionId, this.skillId, "AbilityBuffSpell", "actionId");
        }

        public int durationSeconds() {
            return ResourceDatabase.value(this.durationSeconds, this.skillId, "AbilityBuffSpell", "durationSeconds");
        }

        public int effectGfx() {
            return ResourceDatabase.value(this.effectGfx, this.skillId, "AbilityBuffSpell", "effectGfx");
        }

        public int mpCost() {
            return ResourceDatabase.value(this.mpCost, this.skillId, "AbilityBuffSpell", "mpCost");
        }

        public int reuseDelayMillis() {
            return ResourceDatabase.value(this.reuseDelayMillis, this.skillId, "AbilityBuffSpell", "reuseDelayMillis");
        }

        AbilityBuffSpell {
            if (skillId != (row << 3) + column + 1 || row < 0 || row > 0xFF
                    || column < 0 || column > 7 || actionId < 0 || actionId > 0xFF
                    || effectGfx <= 0 || effectGfx > 0xFFFF || mpCost < 0
                    || reuseDelayMillis < 0 || durationSeconds <= 0
                    || durationSeconds > 0xFFFF || ability == null
                    || modifier <= 0 || modifier > 127
                    || activeType <= 0 || activeType > 0xFF
                    || stopType <= 0 || stopType > 0xFF
                    || happenMessage < 0 || happenMessage > 0xFFFF
                    || stopMessage < 0 || stopMessage > 0xFFFF
                    || requestShape == null
                    || key == null || key.isBlank()) {
                throw new IllegalArgumentException("invalid native ability-buff metadata");
            }
        }

        /** Retained ordering: ordinary circle reductions, matching helm, floor, original INT. */
        int effectiveMpCost(int intelligence, int originalReduction,
                            boolean matchingMagicHelmEquipped) {
            if (intelligence < 1 || intelligence > 254
                    || originalReduction < 0 || originalReduction > 2) {
                throw new IllegalArgumentException(
                        "invalid native ability-buff MP inputs");
            }
            int result = ResourceDatabase.value(this.mpCost, this.skillId, "AbilityBuffSpell", "mpCost");
            if (intelligence > 12 && skillId > 8 && skillId <= 80) result--;
            if (intelligence > 13 && skillId > 16 && skillId <= 80) result--;
            if (intelligence > 14 && skillId > 24 && skillId <= 80) result--;
            if (intelligence > 15 && skillId > 32 && skillId <= 80) result--;
            if (intelligence > 16 && skillId > 40 && skillId <= 80) result--;
            if (intelligence > 17 && skillId > 48 && skillId <= 80) result--;
            if (intelligence > 18 && skillId > 56 && skillId <= 80) result--;
            if (matchingMagicHelmEquipped
                    && (skillId == PHYSICAL_ENCHANT_DEX
                        || skillId == PHYSICAL_ENCHANT_STR)) result >>= 1;
            if (ResourceDatabase.value(this.mpCost, this.skillId, "AbilityBuffSpell", "mpCost") > 0) result = Math.max(result, 1);
            return Math.max(0, result - originalReduction);
        }
    }

    record HasteSpell(int skillId, int row, int column, int actionId,
                      int effectGfx, int mpCost, int reuseDelayMillis,
                      int durationSeconds, int maximumRange,
                      int happenMessage, int stopMessage,
                      int failureMessage, String key) {
        public int actionId() {
            return ResourceDatabase.value(this.actionId, this.skillId, "HasteSpell", "actionId");
        }

        public int durationSeconds() {
            return ResourceDatabase.value(this.durationSeconds, this.skillId, "HasteSpell", "durationSeconds");
        }

        public int effectGfx() {
            return ResourceDatabase.value(this.effectGfx, this.skillId, "HasteSpell", "effectGfx");
        }

        public int mpCost() {
            return ResourceDatabase.value(this.mpCost, this.skillId, "HasteSpell", "mpCost");
        }

        public int reuseDelayMillis() {
            return ResourceDatabase.value(this.reuseDelayMillis, this.skillId, "HasteSpell", "reuseDelayMillis");
        }

        HasteSpell {
            if (skillId != (row << 3) + column + 1 || row < 0 || row > 0xFF
                    || column < 0 || column > 7 || actionId < 0 || actionId > 0xFF
                    || effectGfx <= 0 || effectGfx > 0xFFFF || mpCost < 0
                    || reuseDelayMillis < 0 || durationSeconds <= 0
                    || durationSeconds > 0xFFFF || maximumRange != -1
                    || happenMessage <= 0 || happenMessage > 0xFFFF
                    || stopMessage <= 0 || stopMessage > 0xFFFF
                    || failureMessage <= 0 || failureMessage > 0xFFFF
                    || key == null || key.isBlank()) {
                throw new IllegalArgumentException("invalid native Haste metadata");
            }
        }

        /** Retained ordering: circle INT reduction, qualified helm, min-one, original INT. */
        int effectiveMpCost(int intelligence, int originalReduction,
                            boolean hasteCostHalvingHelmEquipped) {
            if (intelligence < 1 || intelligence > 254
                    || originalReduction < 0 || originalReduction > 2) {
                throw new IllegalArgumentException("invalid native Haste MP inputs");
            }
            int result = ResourceDatabase.value(this.mpCost, this.skillId, "HasteSpell", "mpCost");
            if (intelligence > 12 && skillId > 8 && skillId <= 80) result--;
            if (intelligence > 13 && skillId > 16 && skillId <= 80) result--;
            if (intelligence > 14 && skillId > 24 && skillId <= 80) result--;
            if (intelligence > 15 && skillId > 32 && skillId <= 80) result--;
            if (intelligence > 16 && skillId > 40 && skillId <= 80) result--;
            if (intelligence > 17 && skillId > 48 && skillId <= 80) result--;
            if (intelligence > 18 && skillId > 56 && skillId <= 80) result--;
            if (hasteCostHalvingHelmEquipped) result >>= 1;
            if (ResourceDatabase.value(this.mpCost, this.skillId, "HasteSpell", "mpCost") > 0) result = Math.max(result, 1);
            return Math.max(0, result - originalReduction);
        }
    }

    record SelfHasteSpell(int skillId, int row, int column, int actionId,
                          int effectGfx, int mpCost, int reuseDelayMillis,
                          int durationSeconds, int happenMessage, int stopMessage,
                          String key) {
        public int actionId() {
            return ResourceDatabase.value(this.actionId, this.skillId, "SelfHasteSpell", "actionId");
        }

        public int durationSeconds() {
            return ResourceDatabase.value(this.durationSeconds, this.skillId, "SelfHasteSpell", "durationSeconds");
        }

        public int effectGfx() {
            return ResourceDatabase.value(this.effectGfx, this.skillId, "SelfHasteSpell", "effectGfx");
        }

        public int mpCost() {
            return ResourceDatabase.value(this.mpCost, this.skillId, "SelfHasteSpell", "mpCost");
        }

        public int reuseDelayMillis() {
            return ResourceDatabase.value(this.reuseDelayMillis, this.skillId, "SelfHasteSpell", "reuseDelayMillis");
        }

        SelfHasteSpell {
            if (skillId != (row << 3) + column + 1 || skillId != GREATER_HASTE
                    || row < 0 || row > 0xFF || column < 0 || column > 7
                    || actionId < 0 || actionId > 0xFF
                    || effectGfx <= 0 || effectGfx > 0xFFFF || mpCost < 0
                    || reuseDelayMillis < 0 || durationSeconds <= 0
                    || durationSeconds > 0xFFFF
                    || happenMessage <= 0 || happenMessage > 0xFFFF
                    || stopMessage <= 0 || stopMessage > 0xFFFF
                    || key == null || key.isBlank()) {
                throw new IllegalArgumentException("invalid native self-Haste metadata");
            }
        }

        /** Retained ordering: circle reductions, Wind Helm 20023, floor, original INT. */
        int effectiveMpCost(int intelligence, int originalReduction,
                            boolean windHelmEquipped) {
            if (intelligence < 1 || intelligence > 254
                    || originalReduction < 0 || originalReduction > 2) {
                throw new IllegalArgumentException("invalid native self-Haste MP inputs");
            }
            int result = ResourceDatabase.value(this.mpCost, this.skillId, "SelfHasteSpell", "mpCost");
            if (intelligence > 12 && skillId > 8 && skillId <= 80) result--;
            if (intelligence > 13 && skillId > 16 && skillId <= 80) result--;
            if (intelligence > 14 && skillId > 24 && skillId <= 80) result--;
            if (intelligence > 15 && skillId > 32 && skillId <= 80) result--;
            if (intelligence > 16 && skillId > 40 && skillId <= 80) result--;
            if (intelligence > 17 && skillId > 48 && skillId <= 80) result--;
            if (intelligence > 18 && skillId > 56 && skillId <= 80) result--;
            if (windHelmEquipped) result >>= 1;
            if (ResourceDatabase.value(this.mpCost, this.skillId, "SelfHasteSpell", "mpCost") > 0) result = Math.max(result, 1);
            return Math.max(0, result - originalReduction);
        }
    }

    record CombatBuffSpell(int skillId, int row, int column, int actionId,
                           int effectGfx, int mpCost, int reuseDelayMillis,
                           int durationSeconds, int maximumRange, int targetTo,
                           int armorClass, int meleeHit, int meleeDamage,
                           int failureMessage, String key) {
        public int actionId() {
            return ResourceDatabase.value(this.actionId, this.skillId, "CombatBuffSpell", "actionId");
        }

        public int durationSeconds() {
            return ResourceDatabase.value(this.durationSeconds, this.skillId, "CombatBuffSpell", "durationSeconds");
        }

        public int effectGfx() {
            return ResourceDatabase.value(this.effectGfx, this.skillId, "CombatBuffSpell", "effectGfx");
        }

        public int mpCost() {
            return ResourceDatabase.value(this.mpCost, this.skillId, "CombatBuffSpell", "mpCost");
        }

        public int reuseDelayMillis() {
            return ResourceDatabase.value(this.reuseDelayMillis, this.skillId, "CombatBuffSpell", "reuseDelayMillis");
        }

        CombatBuffSpell {
            if (skillId != (row << 3) + column + 1 || skillId != BERSERKERS
                    || row < 0 || row > 0xFF || column < 0 || column > 7
                    || actionId < 0 || actionId > 0xFF
                    || effectGfx <= 0 || effectGfx > 0xFFFF || mpCost < 0
                    || reuseDelayMillis < 0 || durationSeconds <= 0
                    || durationSeconds > 0xFFFF || maximumRange != -1
                    || targetTo != 12 || armorClass <= 0 || armorClass > 127
                    || meleeHit <= 0 || meleeHit > 127
                    || meleeDamage <= 0 || meleeDamage > 127
                    || failureMessage <= 0 || failureMessage > 0xFFFF
                    || key == null || key.isBlank()) {
                throw new IllegalArgumentException("invalid native combat-buff metadata");
            }
        }

        int effectiveMpCost(int intelligence, int originalReduction) {
            if (intelligence < 1 || intelligence > 254
                    || originalReduction < 0 || originalReduction > 2) {
                throw new IllegalArgumentException("invalid native combat-buff MP inputs");
            }
            int result = ResourceDatabase.value(this.mpCost, this.skillId, "CombatBuffSpell", "mpCost");
            if (intelligence > 12 && skillId > 8 && skillId <= 80) result--;
            if (intelligence > 13 && skillId > 16 && skillId <= 80) result--;
            if (intelligence > 14 && skillId > 24 && skillId <= 80) result--;
            if (intelligence > 15 && skillId > 32 && skillId <= 80) result--;
            if (intelligence > 16 && skillId > 40 && skillId <= 80) result--;
            if (intelligence > 17 && skillId > 48 && skillId <= 80) result--;
            if (intelligence > 18 && skillId > 56 && skillId <= 80) result--;
            if (ResourceDatabase.value(this.mpCost, this.skillId, "CombatBuffSpell", "mpCost") > 0) result = Math.max(result, 1);
            return Math.max(0, result - originalReduction);
        }
    }

    record DamageProtectionSpell(
            int skillId, int row, int column, int actionId, int effectGfx,
            int mpCost, int reuseDelayMillis, int durationSeconds,
            int maximumRange, int targetTo, int damageDivisor,
            int happenMessage, int stopMessage, int failureMessage,
            long itemConsumeId, int itemConsumeCount, String key) {
        public long itemConsumeId() {
            return CombatDatabase.longValue(this.itemConsumeId, this.skillId, "itemConsumeId");
        }

        public int actionId() {
            return ResourceDatabase.value(this.actionId, this.skillId, "DamageProtectionSpell", "actionId");
        }

        public int durationSeconds() {
            return ResourceDatabase.value(this.durationSeconds, this.skillId, "DamageProtectionSpell", "durationSeconds");
        }

        public int effectGfx() {
            return ResourceDatabase.value(this.effectGfx, this.skillId, "DamageProtectionSpell", "effectGfx");
        }

        public int itemConsumeCount() {
            return ResourceDatabase.value(this.itemConsumeCount, this.skillId, "DamageProtectionSpell", "itemConsumeCount");
        }

        public int mpCost() {
            return ResourceDatabase.value(this.mpCost, this.skillId, "DamageProtectionSpell", "mpCost");
        }

        public int reuseDelayMillis() {
            return ResourceDatabase.value(this.reuseDelayMillis, this.skillId, "DamageProtectionSpell", "reuseDelayMillis");
        }

        DamageProtectionSpell {
            if (skillId != (row << 3) + column + 1
                    || skillId != IMMUNE_TO_HARM
                    || row < 0 || row > 0xFF || column < 0 || column > 7
                    || actionId < 0 || actionId > 0xFF
                    || effectGfx <= 0 || effectGfx > 0xFFFF || mpCost < 0
                    || reuseDelayMillis < 0 || durationSeconds <= 0
                    || durationSeconds > 0xFFFF || maximumRange != -1
                    || targetTo != 4 || damageDivisor != 2
                    || happenMessage <= 0 || happenMessage > 0xFFFF
                    || stopMessage <= 0 || stopMessage > 0xFFFF
                    || failureMessage <= 0 || failureMessage > 0xFFFF
                    || itemConsumeId <= 0 || itemConsumeCount <= 0
                    || key == null || key.isBlank()) {
                throw new IllegalArgumentException(
                        "invalid native damage-protection metadata");
            }
        }

        int effectiveMpCost(int intelligence, int originalReduction) {
            if (intelligence < 1 || intelligence > 254
                    || originalReduction < 0 || originalReduction > 2) {
                throw new IllegalArgumentException(
                        "invalid native damage-protection MP inputs");
            }
            int result = ResourceDatabase.value(this.mpCost, this.skillId, "DamageProtectionSpell", "mpCost");
            if (intelligence > 12 && skillId > 8 && skillId <= 80) result--;
            if (intelligence > 13 && skillId > 16 && skillId <= 80) result--;
            if (intelligence > 14 && skillId > 24 && skillId <= 80) result--;
            if (intelligence > 15 && skillId > 32 && skillId <= 80) result--;
            if (intelligence > 16 && skillId > 40 && skillId <= 80) result--;
            if (intelligence > 17 && skillId > 48 && skillId <= 80) result--;
            if (intelligence > 18 && skillId > 56 && skillId <= 80) result--;
            if (ResourceDatabase.value(this.mpCost, this.skillId, "DamageProtectionSpell", "mpCost") > 0) result = Math.max(result, 1);
            return Math.max(0, result - originalReduction);
        }

        int damageAfterProtection(int damage) {
            if (damage < 0) throw new IllegalArgumentException("negative combat damage");
            return damage / damageDivisor;
        }
    }

    record MassTeleportSpell(
            int skillId, int row, int column, int actionId, int effectGfx,
            int mpCost, int reuseDelayMillis, long itemConsumeId,
            int itemConsumeCount, int targetTo, int randomRange,
            int inviteRange, int questionId, int failureMessage, String key) {
        public long itemConsumeId() {
            return CombatDatabase.longValue(this.itemConsumeId, this.skillId, "itemConsumeId");
        }

        public int actionId() {
            return ResourceDatabase.value(this.actionId, this.skillId, "MassTeleportSpell", "actionId");
        }

        public int effectGfx() {
            return ResourceDatabase.value(this.effectGfx, this.skillId, "MassTeleportSpell", "effectGfx");
        }

        public int itemConsumeCount() {
            return ResourceDatabase.value(this.itemConsumeCount, this.skillId, "MassTeleportSpell", "itemConsumeCount");
        }

        public int mpCost() {
            return ResourceDatabase.value(this.mpCost, this.skillId, "MassTeleportSpell", "mpCost");
        }

        public int reuseDelayMillis() {
            return ResourceDatabase.value(this.reuseDelayMillis, this.skillId, "MassTeleportSpell", "reuseDelayMillis");
        }

        MassTeleportSpell {
            if (skillId != (row << 3) + column + 1
                    || skillId != MASS_TELEPORT
                    || row < 0 || row > 0xFF || column < 0 || column > 7
                    || actionId < 0 || actionId > 0xFF
                    || effectGfx <= 0 || effectGfx > 0xFFFF || mpCost < 0
                    || reuseDelayMillis < 0 || itemConsumeId <= 0
                    || itemConsumeCount <= 0 || targetTo != 4
                    || randomRange != BattleMassTeleport.RANDOM_RANGE
                    || inviteRange != BattleMassTeleport.INVITE_RANGE
                    || questionId != 748 || failureMessage != 276
                    || key == null || key.isBlank()) {
                throw new IllegalArgumentException("invalid native mass-teleport metadata");
            }
        }

        int effectiveMpCost(int intelligence, int originalReduction) {
            if (intelligence < 1 || intelligence > 254
                    || originalReduction < 0 || originalReduction > 2) {
                throw new IllegalArgumentException(
                        "invalid native mass-teleport MP inputs");
            }
            int result = ResourceDatabase.value(this.mpCost, this.skillId, "MassTeleportSpell", "mpCost");
            if (intelligence > 12 && skillId > 8 && skillId <= 80) result--;
            if (intelligence > 13 && skillId > 16 && skillId <= 80) result--;
            if (intelligence > 14 && skillId > 24 && skillId <= 80) result--;
            if (intelligence > 15 && skillId > 32 && skillId <= 80) result--;
            if (intelligence > 16 && skillId > 40 && skillId <= 80) result--;
            if (intelligence > 17 && skillId > 48 && skillId <= 80) result--;
            if (intelligence > 18 && skillId > 56 && skillId <= 80) result--;
            if (ResourceDatabase.value(this.mpCost, this.skillId, "MassTeleportSpell", "mpCost") > 0) result = Math.max(result, 1);
            return Math.max(0, result - originalReduction);
        }
    }

    record MovementBuffSpell(int skillId, int row, int column, int actionId,
                             int effectGfx, int mpCost, int reuseDelayMillis,
                             int durationSeconds, int attribute, int sourceType,
                             int braveMode, double movementMultiplier,
                             String key) {
        public int actionId() {
            return ResourceDatabase.value(this.actionId, this.skillId, "MovementBuffSpell", "actionId");
        }

        public int durationSeconds() {
            return ResourceDatabase.value(this.durationSeconds, this.skillId, "MovementBuffSpell", "durationSeconds");
        }

        public int effectGfx() {
            return ResourceDatabase.value(this.effectGfx, this.skillId, "MovementBuffSpell", "effectGfx");
        }

        public int mpCost() {
            return ResourceDatabase.value(this.mpCost, this.skillId, "MovementBuffSpell", "mpCost");
        }

        public int reuseDelayMillis() {
            return ResourceDatabase.value(this.reuseDelayMillis, this.skillId, "MovementBuffSpell", "reuseDelayMillis");
        }

        MovementBuffSpell {
            if (skillId != (row << 3) + column + 1 || row < 0 || row > 0xFF
                    || column < 0 || column > 7 || actionId < 0 || actionId > 0xFF
                    || effectGfx <= 0 || effectGfx > 0xFFFF || mpCost < 0
                    || reuseDelayMillis < 0 || durationSeconds <= 0
                    || durationSeconds > 0xFFFF
                    || (attribute != ATTRIBUTE_WIND
                            && attribute != ATTRIBUTE_UNSPECIFIED)
                    || sourceType != 2 || braveMode != BattleTimedStatus.BRAVE_HOLY_WALK
                    || Double.compare(movementMultiplier,
                            BattleActionInterval.FAST_MOVEMENT_MULTIPLIER) != 0
                    || key == null || key.isBlank()) {
                throw new IllegalArgumentException("invalid native movement-buff metadata");
            }
        }

        /** Exact retained circle reductions, positive floor, then original INT. */
        int effectiveMpCost(int intelligence, int originalReduction) {
            if (intelligence < 1 || intelligence > 254
                    || originalReduction < 0 || originalReduction > 2) {
                throw new IllegalArgumentException("invalid movement-buff MP inputs");
            }
            int result = ResourceDatabase.value(this.mpCost, this.skillId, "MovementBuffSpell", "mpCost");
            if (intelligence > 12 && skillId > 8 && skillId <= 80) result--;
            if (intelligence > 13 && skillId > 16 && skillId <= 80) result--;
            if (intelligence > 14 && skillId > 24 && skillId <= 80) result--;
            if (intelligence > 15 && skillId > 32 && skillId <= 80) result--;
            if (intelligence > 16 && skillId > 40 && skillId <= 80) result--;
            if (intelligence > 17 && skillId > 48 && skillId <= 80) result--;
            if (intelligence > 18 && skillId > 56 && skillId <= 80) result--;
            if (ResourceDatabase.value(this.mpCost, this.skillId, "MovementBuffSpell", "mpCost") > 0) result = Math.max(result, 1);
            return Math.max(0, result - originalReduction);
        }

        int movementIntervalMillis(int baseIntervalMillis) {
            if (baseIntervalMillis <= 0) {
                throw new IllegalArgumentException("invalid base movement interval");
            }
            return Math.max(1, (int) (baseIntervalMillis * movementMultiplier));
        }
    }

    record PhysicalProcBuffSpell(
            int skillId, int row, int column, int actionId, int effectGfx,
            int mpCost, int reuseDelayMillis, int durationSeconds,
            int attribute, int sourceType, int procChance,
            double damageMultiplier, int nativeId, int happenMessage,
            int stopMessage, long itemConsumeId, int itemConsumeCount,
            String key) {
        public long itemConsumeId() {
            return CombatDatabase.longValue(this.itemConsumeId, this.skillId, "itemConsumeId");
        }

        public int actionId() {
            return ResourceDatabase.value(this.actionId, this.skillId, "PhysicalProcBuffSpell", "actionId");
        }

        public int durationSeconds() {
            return ResourceDatabase.value(this.durationSeconds, this.skillId, "PhysicalProcBuffSpell", "durationSeconds");
        }

        public int effectGfx() {
            return ResourceDatabase.value(this.effectGfx, this.skillId, "PhysicalProcBuffSpell", "effectGfx");
        }

        public int itemConsumeCount() {
            return ResourceDatabase.value(this.itemConsumeCount, this.skillId, "PhysicalProcBuffSpell", "itemConsumeCount");
        }

        public int mpCost() {
            return ResourceDatabase.value(this.mpCost, this.skillId, "PhysicalProcBuffSpell", "mpCost");
        }

        public int reuseDelayMillis() {
            return ResourceDatabase.value(this.reuseDelayMillis, this.skillId, "PhysicalProcBuffSpell", "reuseDelayMillis");
        }

        PhysicalProcBuffSpell {
            boolean common = skillId == (row << 3) + column + 1
                    && actionId == 19
                    && (attribute == ATTRIBUTE_UNSPECIFIED
                        || attribute == ATTRIBUTE_FIRE)
                    && sourceType == 2 && key != null && !key.isBlank()
                    && itemConsumeId >= 0 && itemConsumeId <= 0xFFFF_FFFFL
                    && itemConsumeCount >= 0
                    && ((itemConsumeId == 0) == (itemConsumeCount == 0));
            boolean burningSpirit = skillId == BURNING_SPIRIT
                    && row == 12 && column == 5 && effectGfx == 2946
                    && mpCost == 20 && reuseDelayMillis == 2_990
                    && durationSeconds == 320
                    && procChance == BattlePhysicalProcBuff.BURNING_SPIRIT_CHANCE
                    && Double.compare(damageMultiplier,
                            BattlePhysicalProcBuff.BURNING_SPIRIT_MULTIPLIER) == 0
                    && nativeId == 32 && happenMessage == 0 && stopMessage == 0
                    && itemConsumeId == 0 && itemConsumeCount == 0;
            boolean doubleBrake = skillId == DOUBLE_BRAKE
                    && row == 13 && column == 0 && effectGfx == 2949
                    && mpCost == 30 && reuseDelayMillis == 0
                    && durationSeconds == 192
                    && procChance == BattlePhysicalProcBuff.DOUBLE_BRAKE_CHANCE
                    && Double.compare(damageMultiplier,
                            BattlePhysicalProcBuff.DOUBLE_BRAKE_MULTIPLIER) == 0
                    && nativeId == 1 && happenMessage == 823 && stopMessage == 829
                    && itemConsumeId == InventoryCatalog.SECOND_LEVEL_DARK_STONE
                    && itemConsumeCount == 1;
            boolean elementalFire = skillId == ELEMENTAL_FIRE
                    && row == 21 && column == 2 && effectGfx == 4402
                    && mpCost == 20 && reuseDelayMillis == 100
                    && durationSeconds == 192 && attribute == ATTRIBUTE_FIRE
                    && procChance == BattlePhysicalProcBuff.ELEMENTAL_FIRE_CHANCE
                    && Double.compare(damageMultiplier,
                            BattlePhysicalProcBuff.ELEMENTAL_FIRE_MULTIPLIER) == 0
                    && nativeId == 4 && happenMessage == 0 && stopMessage == 0
                    && itemConsumeId == InventoryCatalog.SPIRIT_CRYSTAL
                    && itemConsumeCount == 1;
            boolean soulOfFlame = skillId == SOUL_OF_FLAME
                    && row == 21 && column == 6 && effectGfx == 5833
                    && mpCost == 30 && reuseDelayMillis == 300
                    && durationSeconds == 64 && attribute == ATTRIBUTE_FIRE
                    && procChance == 0
                    && Double.compare(damageMultiplier,
                            BattlePhysicalProcBuff.SOUL_OF_FLAME_MULTIPLIER) == 0
                    && nativeId == 64 && happenMessage == 0 && stopMessage == 0
                    && itemConsumeId == InventoryCatalog.SPIRIT_CRYSTAL
                    && itemConsumeCount == 3;
            if (!common || (!burningSpirit && !doubleBrake && !elementalFire
                    && !soulOfFlame)) {
                throw new IllegalArgumentException(
                        "invalid native physical-proc-buff metadata");
            }
        }

        /** Exact retained circle reductions, positive floor, then original INT. */
        int effectiveMpCost(int intelligence, int originalReduction) {
            if (intelligence < 1 || intelligence > 254
                    || originalReduction < 0 || originalReduction > 2) {
                throw new IllegalArgumentException(
                        "invalid physical-proc-buff MP inputs");
            }
            int result = ResourceDatabase.value(this.mpCost, this.skillId, "PhysicalProcBuffSpell", "mpCost");
            if (intelligence > 12 && skillId > 8 && skillId <= 80) result--;
            if (intelligence > 13 && skillId > 16 && skillId <= 80) result--;
            if (intelligence > 14 && skillId > 24 && skillId <= 80) result--;
            if (intelligence > 15 && skillId > 32 && skillId <= 80) result--;
            if (intelligence > 16 && skillId > 40 && skillId <= 80) result--;
            if (intelligence > 17 && skillId > 48 && skillId <= 80) result--;
            if (intelligence > 18 && skillId > 56 && skillId <= 80) result--;
            if (ResourceDatabase.value(this.mpCost, this.skillId, "PhysicalProcBuffSpell", "mpCost") > 0) result = Math.max(result, 1);
            return Math.max(0, result - originalReduction);
        }
    }

    /**
     * A native selector that commits the retained cast economy but whose
     * selected database row and retained executor establish no server effect.
     */
    record OtherNoEffectSpell(
            int skillId, int row, int column, int actionId, int effectGfx,
            int mpCost, int reuseDelayMillis, int durationSeconds,
            int attribute, int sourceType, int nativeId,
            int happenMessage, int stopMessage, int failureMessage,
            long itemConsumeId, int itemConsumeCount, String key) {
        public long itemConsumeId() {
            return CombatDatabase.longValue(this.itemConsumeId, this.skillId, "itemConsumeId");
        }

        public int actionId() {
            return ResourceDatabase.value(this.actionId, this.skillId, "OtherNoEffectSpell", "actionId");
        }

        public int durationSeconds() {
            return ResourceDatabase.value(this.durationSeconds, this.skillId, "OtherNoEffectSpell", "durationSeconds");
        }

        public int itemConsumeCount() {
            return ResourceDatabase.value(this.itemConsumeCount, this.skillId, "OtherNoEffectSpell", "itemConsumeCount");
        }

        public int mpCost() {
            return ResourceDatabase.value(this.mpCost, this.skillId, "OtherNoEffectSpell", "mpCost");
        }

        public int reuseDelayMillis() {
            return ResourceDatabase.value(this.reuseDelayMillis, this.skillId, "OtherNoEffectSpell", "reuseDelayMillis");
        }

        OtherNoEffectSpell {
            if (skillId != STORM_WALK
                    || skillId != (row << 3) + column + 1
                    || row != 21 || column != 3 || actionId != 19
                    || effectGfx != 0 || mpCost != 10
                    || reuseDelayMillis != 0 || durationSeconds != 0
                    || attribute != ATTRIBUTE_WIND || sourceType != 128
                    || nativeId != 8 || happenMessage != 0
                    || stopMessage != 0 || failureMessage != 0
                    || itemConsumeId != 0 || itemConsumeCount != 0
                    || key == null || key.isBlank()) {
                throw new IllegalArgumentException(
                        "invalid native other/no-effect metadata");
            }
        }

        /** Exact retained positive floor, then original-INT reduction. */
        int effectiveMpCost(int intelligence, int originalReduction) {
            if (intelligence < 1 || intelligence > 254
                    || originalReduction < 0 || originalReduction > 2) {
                throw new IllegalArgumentException(
                        "invalid other/no-effect MP inputs");
            }
            int result = ResourceDatabase.value(this.mpCost, this.skillId, "OtherNoEffectSpell", "mpCost");
            if (intelligence > 12 && skillId > 8 && skillId <= 80) result--;
            if (intelligence > 13 && skillId > 16 && skillId <= 80) result--;
            if (intelligence > 14 && skillId > 24 && skillId <= 80) result--;
            if (intelligence > 15 && skillId > 32 && skillId <= 80) result--;
            if (intelligence > 16 && skillId > 40 && skillId <= 80) result--;
            if (intelligence > 17 && skillId > 48 && skillId <= 80) result--;
            if (intelligence > 18 && skillId > 56 && skillId <= 80) result--;
            if (ResourceDatabase.value(this.mpCost, this.skillId, "OtherNoEffectSpell", "mpCost") > 0) result = Math.max(result, 1);
            return Math.max(0, result - originalReduction);
        }
    }

    record PoisonResistanceSpell(
            int skillId, int row, int column, int actionId, int effectGfx,
            int mpCost, int reuseDelayMillis, int durationSeconds,
            int attribute, int sourceType, int nativeId,
            int happenMessage, int stopMessage, int failureMessage,
            String key) {
        public int actionId() {
            return ResourceDatabase.value(this.actionId, this.skillId, "PoisonResistanceSpell", "actionId");
        }

        public int durationSeconds() {
            return ResourceDatabase.value(this.durationSeconds, this.skillId, "PoisonResistanceSpell", "durationSeconds");
        }

        public int effectGfx() {
            return ResourceDatabase.value(this.effectGfx, this.skillId, "PoisonResistanceSpell", "effectGfx");
        }

        public int mpCost() {
            return ResourceDatabase.value(this.mpCost, this.skillId, "PoisonResistanceSpell", "mpCost");
        }

        public int reuseDelayMillis() {
            return ResourceDatabase.value(this.reuseDelayMillis, this.skillId, "PoisonResistanceSpell", "reuseDelayMillis");
        }

        PoisonResistanceSpell {
            if (skillId != VENOM_RESIST || skillId != (row << 3) + column + 1
                    || row != 12 || column != 7 || actionId != 19
                    || effectGfx != 2948 || mpCost != 20
                    || reuseDelayMillis != 0 || durationSeconds != 320
                    || attribute != ATTRIBUTE_UNSPECIFIED || sourceType != 2
                    || nativeId != 128 || happenMessage != 0
                    || stopMessage != 831 || failureMessage != 0
                    || key == null || key.isBlank()) {
                throw new IllegalArgumentException(
                        "invalid native poison-resistance metadata");
            }
        }

        int effectiveMpCost(int effectiveIntelligence, int originalReduction) {
            if (effectiveIntelligence < 1 || effectiveIntelligence > 254
                    || originalReduction < 0 || originalReduction > 2) {
                throw new IllegalArgumentException(
                        "invalid poison-resistance MP inputs");
            }
            // Skill 104 is above the general-magic circle reductions. The
            // retained original-INT class reduction is still applied last.
            return Math.max(0, ResourceDatabase.value(this.mpCost, this.skillId, "PoisonResistanceSpell", "mpCost") - originalReduction);
        }
    }

    record DodgeBuffSpell(
            int skillId, int row, int column, int actionId, int effectGfx,
            int mpCost, int reuseDelayMillis, int clientCastClockMillis,
            int durationSeconds,
            int attribute, int sourceType, int nativeId, int dodgeModifier,
            String key) {
        public int actionId() {
            return ResourceDatabase.value(this.actionId, this.skillId, "DodgeBuffSpell", "actionId");
        }

        public int durationSeconds() {
            return ResourceDatabase.value(this.durationSeconds, this.skillId, "DodgeBuffSpell", "durationSeconds");
        }

        public int effectGfx() {
            return ResourceDatabase.value(this.effectGfx, this.skillId, "DodgeBuffSpell", "effectGfx");
        }

        public int mpCost() {
            return ResourceDatabase.value(this.mpCost, this.skillId, "DodgeBuffSpell", "mpCost");
        }

        public int reuseDelayMillis() {
            return ResourceDatabase.value(this.reuseDelayMillis, this.skillId, "DodgeBuffSpell", "reuseDelayMillis");
        }

        DodgeBuffSpell {
            if (skillId != (row << 3) + column + 1 || actionId != 19
                    || attribute != ATTRIBUTE_UNSPECIFIED || sourceType != 2
                    || dodgeModifier != 5
                    || key == null || key.isBlank()) {
                throw new IllegalArgumentException(
                        "invalid native dodge-buff metadata");
            }
            boolean uncanny = skillId == UNCANNY_DODGE
                    && row == 13 && column == 1 && effectGfx == 11765
                    && mpCost == 20 && reuseDelayMillis == 0
                    && clientCastClockMillis == 0 && durationSeconds == 192
                    && nativeId == 2 && key.equals("uncanny-dodge");
            boolean mirror = skillId == MIRROR_IMAGE
                    && row == 25 && column == 0 && effectGfx == 6813
                    && mpCost == 10 && reuseDelayMillis == 10
                    && clientCastClockMillis == 1_800
                    && durationSeconds == 1_200 && nativeId == 1
                    && key.equals("mirror-image");
            if (!uncanny && !mirror) {
                throw new IllegalArgumentException(
                        "invalid native dodge-buff variant metadata");
            }
        }

        int effectiveMpCost(int effectiveIntelligence, int originalReduction) {
            if (effectiveIntelligence < 1 || effectiveIntelligence > 254
                    || originalReduction < 0 || originalReduction > 2) {
                throw new IllegalArgumentException(
                        "invalid dodge-buff MP inputs");
            }
            // Skills106/201 are outside general-magic circle reductions. The
            // retained original-INT class reduction remains the final step.
            return Math.max(0, ResourceDatabase.value(this.mpCost, this.skillId, "DodgeBuffSpell", "mpCost") - originalReduction);
        }
    }

    record RangedEvasionBuffSpell(
            int skillId, int row, int column, int actionId, int effectGfx,
            int mpCost, int reuseDelayMillis, int durationSeconds,
            int attribute, int sourceType, int nativeId,
            int evasionRateModifier, String key) {
        public int actionId() {
            return ResourceDatabase.value(this.actionId, this.skillId, "RangedEvasionBuffSpell", "actionId");
        }

        public int durationSeconds() {
            return ResourceDatabase.value(this.durationSeconds, this.skillId, "RangedEvasionBuffSpell", "durationSeconds");
        }

        public int effectGfx() {
            return ResourceDatabase.value(this.effectGfx, this.skillId, "RangedEvasionBuffSpell", "effectGfx");
        }

        public int mpCost() {
            return ResourceDatabase.value(this.mpCost, this.skillId, "RangedEvasionBuffSpell", "mpCost");
        }

        public int reuseDelayMillis() {
            return ResourceDatabase.value(this.reuseDelayMillis, this.skillId, "RangedEvasionBuffSpell", "reuseDelayMillis");
        }

        RangedEvasionBuffSpell {
            if (skillId != DRESS_EVASION
                    || skillId != (row << 3) + column + 1
                    || row != 13 || column != 6 || actionId != 19
                    || effectGfx != 11766 || mpCost != 20
                    || reuseDelayMillis != 0 || durationSeconds != 32
                    || attribute != ATTRIBUTE_UNSPECIFIED || sourceType != 2
                    || nativeId != 64 || evasionRateModifier != 12
                    || key == null || key.isBlank()) {
                throw new IllegalArgumentException(
                        "invalid native ranged-evasion-buff metadata");
            }
        }

        int effectiveMpCost(int effectiveIntelligence, int originalReduction) {
            if (effectiveIntelligence < 1 || effectiveIntelligence > 254
                    || originalReduction < 0 || originalReduction > 2) {
                throw new IllegalArgumentException(
                        "invalid ranged-evasion-buff MP inputs");
            }
            // Skill 111 is outside the general-magic circle reductions. The
            // retained original-INT class reduction is still applied last.
            return Math.max(0, ResourceDatabase.value(this.mpCost, this.skillId, "RangedEvasionBuffSpell", "mpCost") - originalReduction);
        }
    }

    record ItemWeaponEnchantSpell(
            int skillId, int row, int column, int actionId, int effectGfx,
            int mpCost, int reuseDelayMillis, int durationSeconds,
            int attribute, int sourceType, int nativeId,
            int meleeDamage, int rangedDamage,
            int invalidItemMessage, int successMessage, int expirationMessage,
            long itemConsumeId, int itemConsumeCount, String key) {
        public long itemConsumeId() {
            return CombatDatabase.longValue(this.itemConsumeId, this.skillId, "itemConsumeId");
        }

        public int actionId() {
            return ResourceDatabase.value(this.actionId, this.skillId, "ItemWeaponEnchantSpell", "actionId");
        }

        public int durationSeconds() {
            return ResourceDatabase.value(this.durationSeconds, this.skillId, "ItemWeaponEnchantSpell", "durationSeconds");
        }

        public int effectGfx() {
            return ResourceDatabase.value(this.effectGfx, this.skillId, "ItemWeaponEnchantSpell", "effectGfx");
        }

        public int itemConsumeCount() {
            return ResourceDatabase.value(this.itemConsumeCount, this.skillId, "ItemWeaponEnchantSpell", "itemConsumeCount");
        }

        public int mpCost() {
            return ResourceDatabase.value(this.mpCost, this.skillId, "ItemWeaponEnchantSpell", "mpCost");
        }

        public int reuseDelayMillis() {
            return ResourceDatabase.value(this.reuseDelayMillis, this.skillId, "ItemWeaponEnchantSpell", "reuseDelayMillis");
        }

        ItemWeaponEnchantSpell {
            boolean enchantWeapon = skillId == ENCHANT_WEAPON
                    && row == 1 && column == 3 && actionId == 19
                    && effectGfx == 747 && mpCost == 20
                    && reuseDelayMillis == 10 && durationSeconds == 1_800
                    && attribute == ATTRIBUTE_FIRE && sourceType == 2
                    && nativeId == 8 && meleeDamage == 2 && rangedDamage == 2
                    && invalidItemMessage == 79 && successMessage == 161
                    && expirationMessage == 308
                    && itemConsumeId == 0 && itemConsumeCount == 0;
            boolean shadowFang = skillId == SHADOW_FANG
                    && row == 13 && column == 2 && actionId == 19
                    && effectGfx == 2951 && mpCost == 30
                    && reuseDelayMillis == 0 && durationSeconds == 192
                    && attribute == ATTRIBUTE_UNSPECIFIED && sourceType == 2
                    && nativeId == 4 && meleeDamage == 5 && rangedDamage == 5
                    && invalidItemMessage == 79 && successMessage == 0
                    && expirationMessage == 308
                    && itemConsumeId == InventoryCatalog.SECOND_LEVEL_DARK_STONE
                    && itemConsumeCount == 1;
            if (skillId != (row << 3) + column + 1
                    || !enchantWeapon && !shadowFang
                    || key == null || key.isBlank()) {
                throw new IllegalArgumentException(
                        "invalid native item weapon-enchant metadata");
            }
        }

        int effectiveMpCost(int effectiveIntelligence, int originalReduction,
                            boolean strengthMagicHelmEquipped) {
            if (effectiveIntelligence < 1 || effectiveIntelligence > 254
                    || originalReduction < 0 || originalReduction > 2) {
                throw new IllegalArgumentException(
                        "invalid item weapon-enchant MP inputs");
            }
            int result = ResourceDatabase.value(this.mpCost, this.skillId, "ItemWeaponEnchantSpell", "mpCost");
            if (skillId == ENCHANT_WEAPON) {
                // Retained isHPMPConsume order: general-magic INT reduction,
                // Strength Magic Helm 20015 halving, positive-cost floor,
                // then the class original-INT reduction.
                if (effectiveIntelligence > 12) result--;
                if (strengthMagicHelmEquipped) result >>= 1;
                result = Math.max(result, 1);
            }
            // Skill107 is outside general-magic reductions and has no
            // qualifying helm, but retains the same final class reduction.
            return Math.max(0, result - originalReduction);
        }

        int effectiveMpCost(int effectiveIntelligence, int originalReduction) {
            return effectiveMpCost(
                    effectiveIntelligence, originalReduction, false);
        }

        boolean consumesItem() {
            return itemConsumeId > 0 && itemConsumeCount > 0;
        }

        String displayName() {
            return skillId == ENCHANT_WEAPON ? "Enchant-Weapon" : "Shadow-Fang";
        }
    }

    record ItemArmorEnchantSpell(
            int skillId, int row, int column, int actionId, int effectGfx,
            int mpCost, int reuseDelayMillis, int durationSeconds,
            int attribute, int sourceType, int nativeId, int armorClass,
            int invalidItemMessage, int successMessage, int expirationMessage,
            String key) {
        public int actionId() {
            return ResourceDatabase.value(this.actionId, this.skillId, "ItemArmorEnchantSpell", "actionId");
        }

        public int durationSeconds() {
            return ResourceDatabase.value(this.durationSeconds, this.skillId, "ItemArmorEnchantSpell", "durationSeconds");
        }

        public int effectGfx() {
            return ResourceDatabase.value(this.effectGfx, this.skillId, "ItemArmorEnchantSpell", "effectGfx");
        }

        public int mpCost() {
            return ResourceDatabase.value(this.mpCost, this.skillId, "ItemArmorEnchantSpell", "mpCost");
        }

        public int reuseDelayMillis() {
            return ResourceDatabase.value(this.reuseDelayMillis, this.skillId, "ItemArmorEnchantSpell", "reuseDelayMillis");
        }

        ItemArmorEnchantSpell {
            if (skillId != BLESSED_ARMOR
                    || skillId != (row << 3) + column + 1
                    || row != 2 || column != 4 || actionId != 19
                    || effectGfx != 748 || mpCost != 20
                    || reuseDelayMillis != 10 || durationSeconds != 1_800
                    || attribute != ATTRIBUTE_UNSPECIFIED || sourceType != 2
                    || nativeId != 16 || armorClass != -3
                    || invalidItemMessage != 79 || successMessage != 161
                    || expirationMessage != 308
                    || key == null || key.isBlank()) {
                throw new IllegalArgumentException(
                        "invalid native item armor-enchant metadata");
            }
        }

        int effectiveMpCost(int effectiveIntelligence, int originalReduction) {
            if (effectiveIntelligence < 1 || effectiveIntelligence > 254
                    || originalReduction < 0 || originalReduction > 2) {
                throw new IllegalArgumentException(
                        "invalid item armor-enchant MP inputs");
            }
            int result = ResourceDatabase.value(this.mpCost, this.skillId, "ItemArmorEnchantSpell", "mpCost");
            if (effectiveIntelligence > 12) result--;
            if (effectiveIntelligence > 13) result--;
            result = Math.max(result, 1);
            return Math.max(0, result - originalReduction);
        }

        String displayName() {
            return "Blessed-Armor";
        }
    }

    record WeaponBuffSpell(int skillId, int row, int column, int actionId,
                           int effectGfx, int mpCost, int reuseDelayMillis,
                           int durationSeconds, int maximumRange,
                           int meleeHit, int meleeDamage,
                           int rangedHit, int rangedDamage,
                           int missingWeaponMessage, int successMessage,
                           String key) {
        public int actionId() {
            return ResourceDatabase.value(this.actionId, this.skillId, "WeaponBuffSpell", "actionId");
        }

        public int durationSeconds() {
            return ResourceDatabase.value(this.durationSeconds, this.skillId, "WeaponBuffSpell", "durationSeconds");
        }

        public int effectGfx() {
            return ResourceDatabase.value(this.effectGfx, this.skillId, "WeaponBuffSpell", "effectGfx");
        }

        public int mpCost() {
            return ResourceDatabase.value(this.mpCost, this.skillId, "WeaponBuffSpell", "mpCost");
        }

        public int reuseDelayMillis() {
            return ResourceDatabase.value(this.reuseDelayMillis, this.skillId, "WeaponBuffSpell", "reuseDelayMillis");
        }

        WeaponBuffSpell {
            if (skillId != (row << 3) + column + 1 || row < 0 || row > 0xFF
                    || column < 0 || column > 7 || actionId < 0 || actionId > 0xFF
                    || effectGfx <= 0 || effectGfx > 0xFFFF || mpCost < 0
                    || reuseDelayMillis < 0 || durationSeconds <= 0
                    || durationSeconds > 0xFFFF || maximumRange != -1
                    || meleeHit < -127 || meleeHit > 127
                    || meleeDamage < -127 || meleeDamage > 127
                    || rangedHit < -127 || rangedHit > 127
                    || rangedDamage < -127 || rangedDamage > 127
                    || (meleeHit == 0 && meleeDamage == 0
                        && rangedHit == 0 && rangedDamage == 0)
                    || missingWeaponMessage <= 0 || missingWeaponMessage > 0xFFFF
                    || successMessage <= 0 || successMessage > 0xFFFF
                    || key == null || key.isBlank()) {
                throw new IllegalArgumentException("invalid native weapon-buff metadata");
            }
        }

        /** Exact retained circle reductions, positive floor, then original INT. */
        int effectiveMpCost(int effectiveIntelligence, int originalReduction) {
            if (effectiveIntelligence < 1 || effectiveIntelligence > 254
                    || originalReduction < 0 || originalReduction > 2) {
                throw new IllegalArgumentException("invalid weapon-buff MP inputs");
            }
            int result = ResourceDatabase.value(this.mpCost, this.skillId, "WeaponBuffSpell", "mpCost");
            if (effectiveIntelligence > 12 && skillId > 8 && skillId <= 80) result--;
            if (effectiveIntelligence > 13 && skillId > 16 && skillId <= 80) result--;
            if (effectiveIntelligence > 14 && skillId > 24 && skillId <= 80) result--;
            if (effectiveIntelligence > 15 && skillId > 32 && skillId <= 80) result--;
            if (effectiveIntelligence > 16 && skillId > 40 && skillId <= 80) result--;
            if (effectiveIntelligence > 17 && skillId > 48 && skillId <= 80) result--;
            if (effectiveIntelligence > 18 && skillId > 56 && skillId <= 80) result--;
            if (ResourceDatabase.value(this.mpCost, this.skillId, "WeaponBuffSpell", "mpCost") > 0) result = Math.max(result, 1);
            return Math.max(0, result - originalReduction);
        }
    }

    record CancellationSpell(int skillId, int row, int column, int actionId,
                             int effectGfx, int mpCost, int reuseDelayMillis,
                             int maximumRange, int failureMessage,
                             long itemConsumeId, int itemConsumeCount,
                             String key) {
        public long itemConsumeId() {
            return CombatDatabase.longValue(this.itemConsumeId, this.skillId, "itemConsumeId");
        }

        public int nativeSourceVisibilityOriginalRange() {
            return this.maximumRange;
        }

        public int maximumRange() {
            return NativeSourceVisibilityRange.value(this.skillId(), this.nativeSourceVisibilityOriginalRange());
        }

        public int actionId() {
            return ResourceDatabase.value(this.actionId, this.skillId, "CancellationSpell", "actionId");
        }

        public int effectGfx() {
            return ResourceDatabase.value(this.effectGfx, this.skillId, "CancellationSpell", "effectGfx");
        }

        public int itemConsumeCount() {
            return ResourceDatabase.value(this.itemConsumeCount, this.skillId, "CancellationSpell", "itemConsumeCount");
        }

        public int mpCost() {
            return ResourceDatabase.value(this.mpCost, this.skillId, "CancellationSpell", "mpCost");
        }

        public int reuseDelayMillis() {
            return ResourceDatabase.value(this.reuseDelayMillis, this.skillId, "CancellationSpell", "reuseDelayMillis");
        }

        CancellationSpell {
            if (skillId != (row << 3) + column + 1 || row < 0 || row > 0xFF
                    || column < 0 || column > 7 || actionId < 0 || actionId > 0xFF
                    || effectGfx <= 0 || effectGfx > 0xFFFF || mpCost < 0
                    || reuseDelayMillis < 0 || maximumRange != 3
                    || failureMessage <= 0 || failureMessage > 0xFFFF
                    || itemConsumeId != InventoryCatalog.MAGIC_GEM
                    || itemConsumeCount != 2 || key == null || key.isBlank()) {
                throw new IllegalArgumentException("invalid native Cancellation metadata");
            }
        }

        /** Exact retained circle reductions, positive floor, then original INT. */
        int effectiveMpCost(int intelligence, int originalReduction) {
            if (intelligence < 1 || intelligence > 254
                    || originalReduction < 0 || originalReduction > 2) {
                throw new IllegalArgumentException("invalid native Cancellation MP inputs");
            }
            int result = ResourceDatabase.value(this.mpCost, this.skillId, "CancellationSpell", "mpCost");
            if (intelligence > 12 && skillId > 8 && skillId <= 80) result--;
            if (intelligence > 13 && skillId > 16 && skillId <= 80) result--;
            if (intelligence > 14 && skillId > 24 && skillId <= 80) result--;
            if (intelligence > 15 && skillId > 32 && skillId <= 80) result--;
            if (intelligence > 16 && skillId > 40 && skillId <= 80) result--;
            if (intelligence > 17 && skillId > 48 && skillId <= 80) result--;
            if (intelligence > 18 && skillId > 56 && skillId <= 80) result--;
            if (ResourceDatabase.value(this.mpCost, this.skillId, "CancellationSpell", "mpCost") > 0) result = Math.max(result, 1);
            return Math.max(0, result - originalReduction);
        }
    }

    record RegenSpell(int skillId, int row, int column, int actionId,
                      int effectGfx, int mpCost, int hpCost,
                      int reuseDelayMillis, int clientCastClockMillis,
                      int durationSeconds, RequestShape requestShape,
                      Targeting targeting, int maximumRange,
                      RegenEffect effect, int mprBonus,
                      boolean breaksOnActivity, String key) {
        public int actionId() {
            return ResourceDatabase.value(this.actionId, this.skillId, "RegenSpell", "actionId");
        }

        public int durationSeconds() {
            return ResourceDatabase.value(this.durationSeconds, this.skillId, "RegenSpell", "durationSeconds");
        }

        public int effectGfx() {
            return ResourceDatabase.value(this.effectGfx, this.skillId, "RegenSpell", "effectGfx");
        }

        public int hpCost() {
            return ResourceDatabase.value(this.hpCost, this.skillId, "RegenSpell", "hpCost");
        }

        public int mpCost() {
            return ResourceDatabase.value(this.mpCost, this.skillId, "RegenSpell", "mpCost");
        }

        public int reuseDelayMillis() {
            return ResourceDatabase.value(this.reuseDelayMillis, this.skillId, "RegenSpell", "reuseDelayMillis");
        }

        RegenSpell {
            if (skillId != (row << 3) + column + 1 || row < 0 || row > 0xFF
                    || column < 0 || column > 7 || actionId < 0 || actionId > 0xFF
                    || effectGfx <= 0 || effectGfx > 0xFFFF || mpCost < 0
                    || hpCost < 0 || reuseDelayMillis < 0
                    || clientCastClockMillis < 0
                    || clientCastClockMillis > 0xFFFF || durationSeconds <= 0
                    || durationSeconds > 0xFFFF || requestShape == null
                    || targeting == null
                    || maximumRange < 0 || maximumRange > 0xFF || effect == null
                    || mprBonus <= 0 || mprBonus > 127
                    || (targeting == Targeting.SELF_SELECTOR) != (maximumRange == 0)
                    || breaksOnActivity != (effect == RegenEffect.MEDITATION)
                    || key == null || key.isBlank()) {
                throw new IllegalArgumentException("invalid native regen-spell metadata");
            }
        }

        /** Exact retained ordering: circle INT reductions, min-one, then original INT. */
        int effectiveMpCost(int effectiveIntelligence, int originalReduction) {
            if (effectiveIntelligence < 1 || effectiveIntelligence > 254
                    || originalReduction < 0 || originalReduction > 2) {
                throw new IllegalArgumentException(
                        "invalid native regen-spell MP inputs");
            }
            int result = ResourceDatabase.value(this.mpCost, this.skillId, "RegenSpell", "mpCost");
            if (effectiveIntelligence > 12 && skillId > 8 && skillId <= 80) result--;
            if (effectiveIntelligence > 13 && skillId > 16 && skillId <= 80) result--;
            if (effectiveIntelligence > 14 && skillId > 24 && skillId <= 80) result--;
            if (effectiveIntelligence > 15 && skillId > 32 && skillId <= 80) result--;
            if (effectiveIntelligence > 16 && skillId > 40 && skillId <= 80) result--;
            if (effectiveIntelligence > 17 && skillId > 48 && skillId <= 80) result--;
            if (effectiveIntelligence > 18 && skillId > 56 && skillId <= 80) result--;
            if (ResourceDatabase.value(this.mpCost, this.skillId, "RegenSpell", "mpCost") > 0) result = Math.max(result, 1);
            return Math.max(0, result - originalReduction);
        }
    }

    record GroundSpell(int skillId, int row, int column, int actionId,
                       int effectGfx, int maximumRange, int mpCost,
                       int reuseDelayMillis, int durationSeconds,
                       int pulseIntervalMillis, int effectRadius, int segmentCount,
                       long itemConsumeId, int itemConsumeCount, String key) {
        public long itemConsumeId() {
            return CombatDatabase.longValue(this.itemConsumeId, this.skillId, "itemConsumeId");
        }

        public int actionId() {
            return ResourceDatabase.value(this.actionId, this.skillId, "GroundSpell", "actionId");
        }

        public int durationSeconds() {
            return ResourceDatabase.value(this.durationSeconds, this.skillId, "GroundSpell", "durationSeconds");
        }

        public int effectGfx() {
            return ResourceDatabase.value(this.effectGfx, this.skillId, "GroundSpell", "effectGfx");
        }

        public int itemConsumeCount() {
            return ResourceDatabase.value(this.itemConsumeCount, this.skillId, "GroundSpell", "itemConsumeCount");
        }

        public int mpCost() {
            return ResourceDatabase.value(this.mpCost, this.skillId, "GroundSpell", "mpCost");
        }

        public int reuseDelayMillis() {
            return ResourceDatabase.value(this.reuseDelayMillis, this.skillId, "GroundSpell", "reuseDelayMillis");
        }

        GroundSpell {
            if (skillId != (row << 3) + column + 1 || row < 0 || row > 0xFF
                    || column < 0 || column > 7 || actionId < 0 || actionId > 0xFF
                    || effectGfx <= 0 || effectGfx > 0xFFFF || maximumRange < -1
                    || mpCost < 0 || reuseDelayMillis < 0 || durationSeconds <= 0
                    || pulseIntervalMillis < 0 || effectRadius < 0 || segmentCount <= 0
                    || itemConsumeId < 0 || itemConsumeId > 0xFFFF_FFFFL
                    || itemConsumeCount < 0
                    || (itemConsumeId == 0) != (itemConsumeCount == 0)
                    || key == null || key.isBlank()) {
                throw new IllegalArgumentException("invalid native ground spell metadata");
            }
        }

        /** Retained circle INT reductions, positive floor, then original INT. */
        int effectiveMpCost(int effectiveIntelligence, int originalReduction) {
            if (effectiveIntelligence < 1 || effectiveIntelligence > 254
                    || originalReduction < 0 || originalReduction > 2) {
                throw new IllegalArgumentException("invalid ground-spell MP inputs");
            }
            int result = ResourceDatabase.value(this.mpCost, this.skillId, "GroundSpell", "mpCost");
            if (effectiveIntelligence > 12 && skillId > 8 && skillId <= 80) result--;
            if (effectiveIntelligence > 13 && skillId > 16 && skillId <= 80) result--;
            if (effectiveIntelligence > 14 && skillId > 24 && skillId <= 80) result--;
            if (effectiveIntelligence > 15 && skillId > 32 && skillId <= 80) result--;
            if (effectiveIntelligence > 16 && skillId > 40 && skillId <= 80) result--;
            if (effectiveIntelligence > 17 && skillId > 48 && skillId <= 80) result--;
            if (effectiveIntelligence > 18 && skillId > 56 && skillId <= 80) result--;
            if (effectiveIntelligence > 19 && skillId > 64 && skillId <= 80) result--;
            if (effectiveIntelligence > 20 && skillId > 72 && skillId <= 80) result--;
            if (ResourceDatabase.value(this.mpCost, this.skillId, "GroundSpell", "mpCost") > 0) result = Math.max(result, 1);
            return Math.max(0, result - originalReduction);
        }
    }

    record AttackSpell(int skillId, int row, int column, int actionId,
                       int effectGfx, int mpCost, int reuseDelayMillis,
                       int maximumRange, int baseDamage, int diceCount,
                       int dieSides, int attribute, int minimumLawful,
                       int specialCriticalChancePercent, int specialCriticalGfx,
                       boolean finalBurn, String key) {
        public int actionId() {
            return ResourceDatabase.value(this.actionId, this.skillId, "AttackSpell", "actionId");
        }

        public int baseDamage() {
            return ResourceDatabase.value(this.baseDamage, this.skillId, "AttackSpell", "baseDamage");
        }

        public int diceCount() {
            return ResourceDatabase.value(this.diceCount, this.skillId, "AttackSpell", "diceCount");
        }

        public int dieSides() {
            return ResourceDatabase.value(this.dieSides, this.skillId, "AttackSpell", "dieSides");
        }

        public int effectGfx() {
            return ResourceDatabase.value(this.effectGfx, this.skillId, "AttackSpell", "effectGfx");
        }

        public int mpCost() {
            return ResourceDatabase.value(this.mpCost, this.skillId, "AttackSpell", "mpCost");
        }

        public int reuseDelayMillis() {
            return ResourceDatabase.value(this.reuseDelayMillis, this.skillId, "AttackSpell", "reuseDelayMillis");
        }

        public int specialCriticalChancePercent() {
            return ResourceDatabase.value(this.specialCriticalChancePercent, this.skillId, "AttackSpell", "specialCriticalChancePercent");
        }

        public int specialCriticalGfx() {
            return ResourceDatabase.value(this.specialCriticalGfx, this.skillId, "AttackSpell", "specialCriticalGfx");
        }

        AttackSpell(int skillId, int row, int column, int actionId,
                    int effectGfx, int mpCost, int reuseDelayMillis,
                    int maximumRange, int baseDamage, int diceCount,
                    int dieSides, int attribute, String key) {
            this(skillId, row, column, actionId, effectGfx, mpCost,
                    reuseDelayMillis, maximumRange, baseDamage, diceCount,
                    dieSides, attribute, Short.MIN_VALUE, 0, 0, false, key);
        }

        AttackSpell(int skillId, int row, int column, int actionId,
                    int effectGfx, int mpCost, int reuseDelayMillis,
                    int maximumRange, int baseDamage, int diceCount,
                    int dieSides, int attribute, int minimumLawful,
                    int specialCriticalChancePercent, int specialCriticalGfx,
                    String key) {
            this(skillId, row, column, actionId, effectGfx, mpCost,
                    reuseDelayMillis, maximumRange, baseDamage, diceCount,
                    dieSides, attribute, minimumLawful,
                    specialCriticalChancePercent, specialCriticalGfx, false, key);
        }

        AttackSpell {
            boolean noDamageCurse = skillId == CONFUSION;
            boolean mindBreak = skillId == MIND_BREAK;
            boolean boneBreak = skillId == BONE_BREAK;
            boolean armBreaker = skillId == ARM_BREAKER;
            if (skillId != (row << 3) + column + 1 || row < 0 || row > 0xFF
                    || column < 0 || column > 7 || actionId < 0 || actionId > 0xFF
                    || effectGfx <= 0 || effectGfx > 0xFFFF || mpCost < 0
                    || reuseDelayMillis < 0 || maximumRange < 0
                    || baseDamage < 0
                    || (!finalBurn && !noDamageCurse && !mindBreak && !boneBreak
                            && !armBreaker
                            && (diceCount <= 0 || dieSides <= 0))
                    || (noDamageCurse && (baseDamage != 0 || diceCount != 0
                            || dieSides != 0 || finalBurn))
                    || (mindBreak && (baseDamage != 5 || diceCount != 0
                            || dieSides != 0 || finalBurn))
                    || (boneBreak && (baseDamage != 10 || diceCount != 0
                            || dieSides != 0 || finalBurn))
                    || (armBreaker && (baseDamage != 15 || diceCount != 0
                            || dieSides != 0 || finalBurn))
                    || (finalBurn && (skillId != FINAL_BURN || row != 13
                            || column != 3 || actionId != 18 || effectGfx != 2952
                            || mpCost != 1 || reuseDelayMillis != 0
                            || maximumRange != 1 || baseDamage != 0
                            || diceCount != 0 || dieSides != 0
                            || attribute != ATTRIBUTE_NONE))
                    || attribute < 0 || minimumLawful < Short.MIN_VALUE
                    || minimumLawful > Short.MAX_VALUE
                    || specialCriticalChancePercent < 0
                    || specialCriticalChancePercent > 100
                    || (specialCriticalChancePercent == 0) != (specialCriticalGfx == 0)
                    || specialCriticalGfx < 0 || specialCriticalGfx > 0xFFFF
                    || key == null || key.isBlank()) {
                throw new IllegalArgumentException("invalid native attack spell metadata");
            }
        }

        boolean canCastAtLawful(int lawful) {
            if (lawful < Short.MIN_VALUE || lawful > Short.MAX_VALUE) {
                throw new IllegalArgumentException("lawful is outside signed-word range");
            }
            return lawful >= minimumLawful;
        }

        boolean specialCritical(IntUnaryOperator percentileRoll) {
            if (specialCriticalChancePercent == 0) return false;
            int roll = percentileRoll.applyAsInt(100);
            if (roll < 1 || roll > 100) {
                throw new IllegalArgumentException(
                        "critical roll is outside percentile range");
            }
            return roll <= specialCriticalChancePercent;
        }

        int damageAfterSpecialCritical(int damage, boolean critical) {
            if (damage < 0) {
                throw new IllegalArgumentException("damage cannot be negative");
            }
            if (!critical) return damage;
            return (int) Math.min(Integer.MAX_VALUE,
                    Math.multiplyExact((long) damage, 2L));
        }

        int effectGfx(boolean critical) {
            return critical ? specialCriticalGfx : effectGfx;
        }

        /** Retained L1SkillUse assigns the final calculated damage to heal. */
        boolean drainsLife() {
            return skillId == CHILL_TOUCH || skillId == VAMPIRIC_TOUCH;
        }

        /** Exact database HP admission cost; Final Burn commits HP=1 instead of subtraction. */
        int hpCost() {
            return finalBurn ? 1 : 0;
        }

        /** Retained circle INT reductions, positive floor, then original INT. */
        int effectiveMpCost(int effectiveIntelligence, int originalReduction) {
            if (effectiveIntelligence < 1 || effectiveIntelligence > 254
                    || originalReduction < 0 || originalReduction > 2) {
                throw new IllegalArgumentException("invalid attack-spell MP inputs");
            }
            int result = ResourceDatabase.value(this.mpCost, this.skillId, "AttackSpell", "mpCost");
            if (effectiveIntelligence > 12 && skillId > 8 && skillId <= 80) result--;
            if (effectiveIntelligence > 13 && skillId > 16 && skillId <= 80) result--;
            if (effectiveIntelligence > 14 && skillId > 24 && skillId <= 80) result--;
            if (effectiveIntelligence > 15 && skillId > 32 && skillId <= 80) result--;
            if (effectiveIntelligence > 16 && skillId > 40 && skillId <= 80) result--;
            if (effectiveIntelligence > 17 && skillId > 48 && skillId <= 80) result--;
            if (effectiveIntelligence > 18 && skillId > 56 && skillId <= 80) result--;
            if (effectiveIntelligence > 19 && skillId > 64 && skillId <= 80) result--;
            if (effectiveIntelligence > 20 && skillId > 72 && skillId <= 80) result--;
            if (ResourceDatabase.value(this.mpCost, this.skillId, "AttackSpell", "mpCost") > 0) result = Math.max(result, 1);
            return Math.max(0, result - originalReduction);
        }
    }

    /** Selected primary skill213 damage plus non-refreshing DMGUP2 contract. */
    record ArmBreakerSpell(
            AttackSpell attackSpell, int clientCastClockMillis,
            int durationSeconds, int targetTo, int type, int nativeSkillId,
            int meleeDamageModifier, int retainedStatusId,
            long skillBookItemId, String skillBookNameId,
            String skillRowNameId, int dormantBossModifier,
            boolean counterMagicBypass, String key) {
        public int durationSeconds() {
            return ResourceDatabase.value(this.durationSeconds, skillId(), "ArmBreakerSpell", "durationSeconds");
        }

        ArmBreakerSpell {
            if (attackSpell == null || attackSpell.skillId() != ARM_BREAKER
                    || attackSpell.row() != 26 || attackSpell.column() != 4
                    || attackSpell.actionId() != 18
                    || attackSpell.effectGfx() != 6_551
                    || attackSpell.mpCost() != 25
                    || attackSpell.reuseDelayMillis() != 1_200
                    || attackSpell.maximumRange() != 3
                    || attackSpell.baseDamage() != 15
                    || attackSpell.diceCount() != 0
                    || attackSpell.dieSides() != 0
                    || attackSpell.attribute() != ATTRIBUTE_UNSPECIFIED
                    || clientCastClockMillis != 32 || durationSeconds != 12
                    || targetTo != 3 || type != 64 || nativeSkillId != 16
                    || meleeDamageModifier != -2 || retainedStatusId != 1_026
                    || skillBookItemId != 49_129L
                    || !"$5693".equals(skillBookNameId)
                    || !"$5947".equals(skillRowNameId)
                    || dormantBossModifier != 0 || counterMagicBypass
                    || key == null || key.isBlank()) {
                throw new IllegalArgumentException(
                        "invalid native Arm Breaker metadata");
            }
        }

        int skillId() { return attackSpell.skillId(); }
        int row() { return attackSpell.row(); }
        int column() { return attackSpell.column(); }
        int maximumRange() { return attackSpell.maximumRange(); }
        RequestShape requestShape() { return RequestShape.SELECTOR_ONLY; }
    }

    /** Selected primary skill202 contract after probability-table override. */
    record ConfusionSpell(
            AttackSpell attackSpell, int hpCost, int clientCastClockMillis,
            int durationSeconds, int successChancePercent, int bossModifier,
            boolean counterMagicBypass, int targetMessageCode,
            int dormantDatabaseDamageValue, long skillBookItemId,
            String skillBookNameId, String skillRowNameId, String key) {
        public int durationSeconds() {
            return ResourceDatabase.value(this.durationSeconds, skillId(), "ConfusionSpell", "durationSeconds");
        }

        public int hpCost() {
            return ResourceDatabase.value(this.hpCost, skillId(), "ConfusionSpell", "hpCost");
        }

        ConfusionSpell {
            if (attackSpell == null || attackSpell.skillId() != CONFUSION
                    || attackSpell.row() != 25 || attackSpell.column() != 1
                    || attackSpell.actionId() != 18
                    || attackSpell.effectGfx() != 6_525
                    || attackSpell.mpCost() != 15
                    || attackSpell.reuseDelayMillis() != 3_000
                    || attackSpell.maximumRange() != 3
                    || attackSpell.baseDamage() != 0
                    || attackSpell.diceCount() != 0
                    || attackSpell.dieSides() != 0
                    || attackSpell.attribute() != ATTRIBUTE_NONE
                    || hpCost != 10 || clientCastClockMillis != 0
                    || durationSeconds != 8 || successChancePercent != 25
                    || bossModifier != 0 || counterMagicBypass
                    || targetMessageCode != 1_339
                    || dormantDatabaseDamageValue != 11
                    || skillBookItemId != 49_118L
                    || !"$5682".equals(skillBookNameId)
                    || !"$5936".equals(skillRowNameId)
                    || key == null || key.isBlank()) {
                throw new IllegalArgumentException(
                        "invalid native Confusion metadata");
            }
        }

        int skillId() { return attackSpell.skillId(); }
        int row() { return attackSpell.row(); }
        int column() { return attackSpell.column(); }
        int maximumRange() { return attackSpell.maximumRange(); }

        int chancePercent(boolean bossTarget) {
            return bossTarget && bossModifier == 0 ? 0 : successChancePercent;
        }

        boolean succeeds(boolean bossTarget, int percentileRoll) {
            if (percentileRoll < 1 || percentileRoll > 100) {
                throw new IllegalArgumentException(
                        "Confusion roll is outside percentile range");
            }
            int chance = chancePercent(bossTarget);
            return chance > 0 && percentileRoll <= chance;
        }
    }

    /** Selected primary skill203 direct-damage contract. */
    record SmashSpell(
            AttackSpell attackSpell, int clientCastClockMillis,
            int targetTo, int type, int nativeSkillId,
            long skillBookItemId, String skillBookNameId,
            String skillRowNameId, String skillsItemLabel, String key) {
        SmashSpell {
            if (attackSpell == null || attackSpell.skillId() != SMASH
                    || attackSpell.row() != 25 || attackSpell.column() != 2
                    || attackSpell.actionId() != 18
                    || attackSpell.effectGfx() != 6_526
                    || attackSpell.mpCost() != 7
                    || attackSpell.reuseDelayMillis() != 600
                    || attackSpell.maximumRange() != 1
                    || attackSpell.baseDamage() != 50
                    || attackSpell.diceCount() != 2
                    || attackSpell.dieSides() != 10
                    || attackSpell.attribute() != ATTRIBUTE_UNSPECIFIED
                    || clientCastClockMillis != 0 || targetTo != 3
                    || type != 64 || nativeSkillId != 4
                    || skillBookItemId != 49_119L
                    || !"$5683".equals(skillBookNameId)
                    || !"$5937".equals(skillRowNameId)
                    || !"暴擊".equals(skillsItemLabel)
                    || key == null || key.isBlank()
                    || !key.equals(attackSpell.key())) {
                throw new IllegalArgumentException(
                        "invalid native Smash Energy metadata");
            }
        }

        int skillId() { return attackSpell.skillId(); }
        int row() { return attackSpell.row(); }
        int column() { return attackSpell.column(); }
        int maximumRange() { return attackSpell.maximumRange(); }
    }

    /** Selected primary skill207 target-MP-gated direct-damage contract. */
    record MindBreakSpell(
            AttackSpell attackSpell, int hpCost, int clientCastClockMillis,
            int targetTo, int type, int nativeSkillId,
            int minimumTargetMpDrain, int maximumTargetMpDrain,
            int damageMultiplier, long skillBookItemId,
            String skillBookNameId, String skillRowNameId,
            boolean counterMagicBypass, String key) {
        public int hpCost() {
            return ResourceDatabase.value(this.hpCost, skillId(), "MindBreakSpell", "hpCost");
        }

        MindBreakSpell {
            if (attackSpell == null || attackSpell.skillId() != MIND_BREAK
                    || attackSpell.row() != 25 || attackSpell.column() != 6
                    || attackSpell.actionId() != 18
                    || attackSpell.effectGfx() != 6_553
                    || attackSpell.mpCost() != 20
                    || attackSpell.reuseDelayMillis() != 3_000
                    || attackSpell.maximumRange() != 3
                    || attackSpell.baseDamage() != 5
                    || attackSpell.diceCount() != 0
                    || attackSpell.dieSides() != 0
                    || attackSpell.attribute() != ATTRIBUTE_UNSPECIFIED
                    || hpCost != 15 || clientCastClockMillis != 0
                    || targetTo != 3 || type != 64 || nativeSkillId != 64
                    || minimumTargetMpDrain != 5
                    || maximumTargetMpDrain != 50
                    || damageMultiplier != 4
                    || skillBookItemId != 49_123L
                    || !"$5687".equals(skillBookNameId)
                    || !"$5941".equals(skillRowNameId)
                    || counterMagicBypass
                    || key == null || key.isBlank()
                    || !key.equals(attackSpell.key())) {
                throw new IllegalArgumentException(
                        "invalid native Mind Break metadata");
            }
        }

        int skillId() { return attackSpell.skillId(); }
        int row() { return attackSpell.row(); }
        int column() { return attackSpell.column(); }
        int maximumRange() { return attackSpell.maximumRange(); }

        boolean validTargetMpDrain(int amount) {
            return amount >= minimumTargetMpDrain
                    && amount <= maximumTargetMpDrain;
        }

        boolean targetHasRequiredMp(int currentMp, int amount) {
            if (currentMp < 0 || !validTargetMpDrain(amount)) {
                throw new IllegalArgumentException(
                        "invalid Mind Break target MP input");
            }
            return currentMp > amount;
        }

        int damageAfterMultiplier(int damage) {
            if (damage < 0) {
                throw new IllegalArgumentException(
                        "Mind Break damage cannot be negative");
            }
            return Math.toIntExact(Math.min(Integer.MAX_VALUE,
                    Math.multiplyExact((long) damage, damageMultiplier)));
        }
    }

    /** Selected skill208 mode contract, including intentionally dormant DB controls. */
    record BoneBreakSpell(
            AttackSpell attackSpell, int clientCastClockMillis,
            int targetTo, int type, int nativeSkillId, int lawfulDelta,
            int durationSeconds, int minimumRoll, int maximumRoll,
            int failureThreshold, int effectNpcId, int effectNpcGfx,
            long skillBookItemId, String skillBookNameId,
            String skillRowNameId,
            int dormantProbabilityHigh, int dormantProbabilityEqual,
            int dormantProbabilityLow, double dormantIntelligenceMultiplier,
            int dormantMagicLevelMultiplier,
            double dormantMagicResistanceMultiplier,
            int primaryBossModifier, int secondaryBossModifier,
            boolean counterMagicBypass, String key) {
        public int durationSeconds() {
            return ResourceDatabase.value(this.durationSeconds, skillId(), "BoneBreakSpell", "durationSeconds");
        }

        BoneBreakSpell {
            if (attackSpell == null || attackSpell.skillId() != BONE_BREAK
                    || attackSpell.row() != 25 || attackSpell.column() != 7
                    || attackSpell.actionId() != 18
                    || attackSpell.effectGfx() != 7_020
                    || attackSpell.mpCost() != 20
                    || attackSpell.reuseDelayMillis() != 2_000
                    || attackSpell.maximumRange() != 3
                    || attackSpell.baseDamage() != 10
                    || attackSpell.diceCount() != 0
                    || attackSpell.dieSides() != 0
                    || attackSpell.attribute() != ATTRIBUTE_UNSPECIFIED
                    || clientCastClockMillis != 0 || targetTo != 3
                    || type != 64 || nativeSkillId != 128
                    || lawfulDelta != 45 || durationSeconds != 2
                    || minimumRoll != 1 || maximumRoll != 1_000
                    || failureThreshold != 800
                    || effectNpcId != 86_123 || effectNpcGfx != 6_552
                    || skillBookItemId != 49_124L
                    || !"$5688".equals(skillBookNameId)
                    || !"$5942".equals(skillRowNameId)
                    || dormantProbabilityHigh != 55
                    || dormantProbabilityEqual != 45
                    || dormantProbabilityLow != 35
                    || dormantIntelligenceMultiplier != 0.35D
                    || dormantMagicLevelMultiplier != 5
                    || dormantMagicResistanceMultiplier != 0.05D
                    || primaryBossModifier != 10
                    || secondaryBossModifier != 0
                    || counterMagicBypass
                    || key == null || key.isBlank()
                    || !key.equals(attackSpell.key())) {
                throw new IllegalArgumentException(
                        "invalid native Bone Break metadata");
            }
        }

        int skillId() { return attackSpell.skillId(); }
        int row() { return attackSpell.row(); }
        int column() { return attackSpell.column(); }
        int maximumRange() { return attackSpell.maximumRange(); }

        boolean succeeds(int oneBasedRoll) {
            if (oneBasedRoll < minimumRoll || oneBasedRoll > maximumRoll) {
                throw new IllegalArgumentException(
                        "Bone Break roll is outside 1..1000");
            }
            return oneBasedRoll > failureThreshold;
        }

        int successPermille() {
            return maximumRoll - failureThreshold;
        }
    }

    /** Selected primary skill204 melee HIT/DMG ability contract. */
    record IllusionOgreSpell(
            int skillId, int row, int column, int actionId, int effectGfx,
            int mpCost, int reuseDelayMillis, int clientCastClockMillis,
            int durationSeconds, int maximumRange, int targetTo, int type,
            int nativeSkillId, int meleeHit, int meleeDamage,
            long skillBookItemId, String skillBookNameId,
            String skillRowNameId, boolean counterMagicBypass, String key) {
        public int actionId() {
            return ResourceDatabase.value(this.actionId, this.skillId, "IllusionOgreSpell", "actionId");
        }

        public int durationSeconds() {
            return ResourceDatabase.value(this.durationSeconds, this.skillId, "IllusionOgreSpell", "durationSeconds");
        }

        public int effectGfx() {
            return ResourceDatabase.value(this.effectGfx, this.skillId, "IllusionOgreSpell", "effectGfx");
        }

        public int mpCost() {
            return ResourceDatabase.value(this.mpCost, this.skillId, "IllusionOgreSpell", "mpCost");
        }

        public int reuseDelayMillis() {
            return ResourceDatabase.value(this.reuseDelayMillis, this.skillId, "IllusionOgreSpell", "reuseDelayMillis");
        }

        IllusionOgreSpell {
            if (skillId != ILLUSION_OGRE
                    || skillId != (row << 3) + column + 1
                    || row != 25 || column != 3 || actionId != 19
                    || effectGfx != 6_578 || mpCost != 15
                    || reuseDelayMillis != 2_000
                    || clientCastClockMillis != 0 || durationSeconds != 64
                    || maximumRange != 3 || targetTo != 1 || type != 2
                    || nativeSkillId != 8 || meleeHit != 4 || meleeDamage != 4
                    || skillBookItemId != 49_120L
                    || !"$5684".equals(skillBookNameId)
                    || !"$5938".equals(skillRowNameId)
                    || !counterMagicBypass
                    || key == null || key.isBlank()) {
                throw new IllegalArgumentException(
                        "invalid native Illusion: Ogre metadata");
            }
        }

        RequestShape requestShape() { return RequestShape.SELECTOR_ONLY; }

        int effectiveMpCost(int originalReduction) {
            if (originalReduction < 0 || originalReduction > 2) {
                throw new IllegalArgumentException(
                        "invalid Illusion: Ogre original INT reduction");
            }
            // Skill204 is outside the general-magic circle reductions. The
            // retained original-INT class reduction is applied last.
            return Math.max(0, ResourceDatabase.value(this.mpCost, this.skillId, "IllusionOgreSpell", "mpCost") - originalReduction);
        }
    }

    /** Selected primary skill209 SP ability contract. */
    record IllusionLichSpell(
            int skillId, int row, int column, int actionId, int effectGfx,
            int mpCost, int reuseDelayMillis, int clientCastClockMillis,
            int durationSeconds, int maximumRange, int targetTo, int type,
            int nativeSkillId, int spellPower,
            long skillBookItemId, String skillBookNameId,
            String skillRowNameId, boolean counterMagicBypass, String key) {
        public int actionId() {
            return ResourceDatabase.value(this.actionId, this.skillId, "IllusionLichSpell", "actionId");
        }

        public int durationSeconds() {
            return ResourceDatabase.value(this.durationSeconds, this.skillId, "IllusionLichSpell", "durationSeconds");
        }

        public int effectGfx() {
            return ResourceDatabase.value(this.effectGfx, this.skillId, "IllusionLichSpell", "effectGfx");
        }

        public int mpCost() {
            return ResourceDatabase.value(this.mpCost, this.skillId, "IllusionLichSpell", "mpCost");
        }

        public int reuseDelayMillis() {
            return ResourceDatabase.value(this.reuseDelayMillis, this.skillId, "IllusionLichSpell", "reuseDelayMillis");
        }

        IllusionLichSpell {
            if (skillId != ILLUSION_LICH
                    || skillId != (row << 3) + column + 1
                    || row != 26 || column != 0 || actionId != 19
                    || effectGfx != 6_577 || mpCost != 15
                    || reuseDelayMillis != 2_000
                    || clientCastClockMillis != 64 || durationSeconds != 64
                    || maximumRange != 3 || targetTo != 1 || type != 2
                    || nativeSkillId != 1 || spellPower != 2
                    || skillBookItemId != 49_125L
                    || !"$5689".equals(skillBookNameId)
                    || !"$5943".equals(skillRowNameId)
                    || !counterMagicBypass
                    || key == null || key.isBlank()) {
                throw new IllegalArgumentException(
                        "invalid native Illusion: Lich metadata");
            }
        }

        RequestShape requestShape() { return RequestShape.SELECTOR_ONLY; }

        int effectiveMpCost(int originalReduction) {
            if (originalReduction < 0 || originalReduction > 2) {
                throw new IllegalArgumentException(
                        "invalid Illusion: Lich original INT reduction");
            }
            return Math.max(0, ResourceDatabase.value(this.mpCost, this.skillId, "IllusionLichSpell", "mpCost") - originalReduction);
        }
    }

    /** Selected primary skill219 outgoing/incoming damage and SP contract. */
    record IllusionAvatarSpell(
            int skillId, int row, int column, int actionId, int effectGfx,
            int mpCost, int reuseDelayMillis, int clientCastClockMillis,
            int durationSeconds, int maximumRange, int targetTo, int type,
            int nativeSkillId, int meleeDamage, int rangedDamage,
            int spellPower, double incomingDamageRate,
            long skillBookItemId, String skillBookNameId,
            String skillRowNameId, boolean counterMagicBypass,
            boolean persisted, String key) {
        public int actionId() {
            return ResourceDatabase.value(this.actionId, this.skillId, "IllusionAvatarSpell", "actionId");
        }

        public int durationSeconds() {
            return ResourceDatabase.value(this.durationSeconds, this.skillId, "IllusionAvatarSpell", "durationSeconds");
        }

        public int effectGfx() {
            return ResourceDatabase.value(this.effectGfx, this.skillId, "IllusionAvatarSpell", "effectGfx");
        }

        public int mpCost() {
            return ResourceDatabase.value(this.mpCost, this.skillId, "IllusionAvatarSpell", "mpCost");
        }

        public int reuseDelayMillis() {
            return ResourceDatabase.value(this.reuseDelayMillis, this.skillId, "IllusionAvatarSpell", "reuseDelayMillis");
        }

        IllusionAvatarSpell {
            if (skillId != ILLUSION_AVATAR
                    || skillId != (row << 3) + column + 1
                    || row != 27 || column != 2 || actionId != 19
                    || effectGfx != 6_575 || mpCost != 35
                    || reuseDelayMillis != 5_000
                    || clientCastClockMillis != 0 || durationSeconds != 64
                    || maximumRange != 3 || targetTo != 1 || type != 2
                    || nativeSkillId != 4
                    || meleeDamage != 10 || rangedDamage != 10
                    || spellPower != 8
                    || Double.compare(incomingDamageRate, 1.5D) != 0
                    || skillBookItemId != 49_135L
                    || !"$5699".equals(skillBookNameId)
                    || !"$5953".equals(skillRowNameId)
                    || !counterMagicBypass || persisted
                    || key == null || key.isBlank()) {
                throw new IllegalArgumentException(
                        "invalid native Illusion: Avatar metadata");
            }
        }

        RequestShape requestShape() { return RequestShape.SELECTOR_ONLY; }

        int effectiveMpCost(int originalReduction) {
            if (originalReduction < 0 || originalReduction > 2) {
                throw new IllegalArgumentException(
                        "invalid Illusion: Avatar original INT reduction");
            }
            return Math.max(0, ResourceDatabase.value(this.mpCost, this.skillId, "IllusionAvatarSpell", "mpCost") - originalReduction);
        }
    }

    /** Selected primary skill214 AC ability contract. */
    record DiamondGolemSpell(
            int skillId, int row, int column, int actionId, int effectGfx,
            int mpCost, int reuseDelayMillis, int clientCastClockMillis,
            int durationSeconds, int maximumRange, int targetTo, int type,
            int nativeSkillId, int armorClass,
            long skillBookItemId, String skillBookNameId,
            String skillRowNameId, boolean counterMagicBypass,
            boolean persisted, String key) {
        public int actionId() {
            return ResourceDatabase.value(this.actionId, this.skillId, "DiamondGolemSpell", "actionId");
        }

        public int durationSeconds() {
            return ResourceDatabase.value(this.durationSeconds, this.skillId, "DiamondGolemSpell", "durationSeconds");
        }

        public int effectGfx() {
            return ResourceDatabase.value(this.effectGfx, this.skillId, "DiamondGolemSpell", "effectGfx");
        }

        public int mpCost() {
            return ResourceDatabase.value(this.mpCost, this.skillId, "DiamondGolemSpell", "mpCost");
        }

        public int reuseDelayMillis() {
            return ResourceDatabase.value(this.reuseDelayMillis, this.skillId, "DiamondGolemSpell", "reuseDelayMillis");
        }

        DiamondGolemSpell {
            if (skillId != ILLUSION_DIAMOND_GOLEM
                    || skillId != (row << 3) + column + 1
                    || row != 26 || column != 5 || actionId != 19
                    || effectGfx != 6_576 || mpCost != 25
                    || reuseDelayMillis != 2_000
                    || clientCastClockMillis != 0 || durationSeconds != 64
                    || maximumRange != 3 || targetTo != 1 || type != 2
                    || nativeSkillId != 32 || armorClass != -20
                    || skillBookItemId != 49_130L
                    || !"$5694".equals(skillBookNameId)
                    || !"$5948".equals(skillRowNameId)
                    || !counterMagicBypass || persisted
                    || key == null || key.isBlank()) {
                throw new IllegalArgumentException(
                        "invalid native Illusion: Diamond Golem metadata");
            }
        }

        RequestShape requestShape() { return RequestShape.SELECTOR_ONLY; }

        int effectiveMpCost(int originalReduction) {
            if (originalReduction < 0 || originalReduction > 2) {
                throw new IllegalArgumentException(
                        "invalid Diamond Golem original INT reduction");
            }
            return Math.max(0, ResourceDatabase.value(this.mpCost, this.skillId, "DiamondGolemSpell", "mpCost") - originalReduction);
        }
    }

    /** Selected primary skill216 five-ability PC buff contract. */
    record InsightSpell(
            int skillId, int row, int column, int actionId, int effectGfx,
            int mpCost, int reuseDelayMillis, int clientCastClockMillis,
            int durationSeconds, int maximumRange, int targetTo, int type,
            int nativeSkillId, int abilityScoreModifier,
            long skillBookItemId, String skillBookNameId,
            String skillRowNameId, boolean counterMagicBypass,
            boolean persisted, String key) {
        public int actionId() {
            return ResourceDatabase.value(this.actionId, this.skillId, "InsightSpell", "actionId");
        }

        public int durationSeconds() {
            return ResourceDatabase.value(this.durationSeconds, this.skillId, "InsightSpell", "durationSeconds");
        }

        public int effectGfx() {
            return ResourceDatabase.value(this.effectGfx, this.skillId, "InsightSpell", "effectGfx");
        }

        public int mpCost() {
            return ResourceDatabase.value(this.mpCost, this.skillId, "InsightSpell", "mpCost");
        }

        public int reuseDelayMillis() {
            return ResourceDatabase.value(this.reuseDelayMillis, this.skillId, "InsightSpell", "reuseDelayMillis");
        }

        InsightSpell {
            if (skillId != INSIGHT
                    || skillId != (row << 3) + column + 1
                    || row != 26 || column != 7 || actionId != 19
                    || effectGfx != 6_505 || mpCost != 40
                    || reuseDelayMillis != 1_000
                    || clientCastClockMillis != 3_600
                    || durationSeconds != 600 || maximumRange != 3
                    || targetTo != 1 || type != 2 || nativeSkillId != 128
                    || abilityScoreModifier != 1
                    || skillBookItemId != 49_132L
                    || !"$5696".equals(skillBookNameId)
                    || !"$5950".equals(skillRowNameId)
                    || !counterMagicBypass || persisted
                    || key == null || key.isBlank()) {
                throw new IllegalArgumentException(
                        "invalid native Insight metadata");
            }
        }

        RequestShape requestShape() { return RequestShape.SELECTOR_ONLY; }

        int effectiveMpCost(int originalReduction) {
            if (originalReduction < 0 || originalReduction > 2) {
                throw new IllegalArgumentException(
                        "invalid Insight original INT reduction");
            }
            return Math.max(0, ResourceDatabase.value(this.mpCost, this.skillId, "InsightSpell", "mpCost") - originalReduction);
        }
    }

    /** Selected primary skill217 five-ability PC/NPC curse contract. */
    record PanicSpell(
            int skillId, int row, int column, int actionId, int effectGfx,
            int mpCost, int hpCost, int reuseDelayMillis,
            int clientCastClockMillis, int durationSeconds, int maximumRange,
            int targetTo, int type, int nativeSkillId,
            int abilityScoreModifier,
            int probabilityHigh, int probabilityEqual, int probabilityLow,
            double intelligenceMultiplier, boolean includeMagicLevel,
            boolean includeMagicHit, double magicResistanceMultiplier,
            int maximumProbability, int bossModifier,
            long skillBookItemId, String skillBookNameId,
            String skillRowNameId, boolean counterMagicBypass,
            boolean persisted, String key) {
        public int actionId() {
            return ResourceDatabase.value(this.actionId, this.skillId, "PanicSpell", "actionId");
        }

        public int durationSeconds() {
            return ResourceDatabase.value(this.durationSeconds, this.skillId, "PanicSpell", "durationSeconds");
        }

        public int effectGfx() {
            return ResourceDatabase.value(this.effectGfx, this.skillId, "PanicSpell", "effectGfx");
        }

        public int hpCost() {
            return ResourceDatabase.value(this.hpCost, this.skillId, "PanicSpell", "hpCost");
        }

        public int mpCost() {
            return ResourceDatabase.value(this.mpCost, this.skillId, "PanicSpell", "mpCost");
        }

        public int reuseDelayMillis() {
            return ResourceDatabase.value(this.reuseDelayMillis, this.skillId, "PanicSpell", "reuseDelayMillis");
        }

        PanicSpell {
            if (skillId != PANIC
                    || skillId != (row << 3) + column + 1
                    || row != 27 || column != 0 || actionId != 19
                    || effectGfx != 6_506 || mpCost != 30 || hpCost != 30
                    || reuseDelayMillis != 4_000
                    || clientCastClockMillis != 0
                    || durationSeconds != 64 || maximumRange != 3
                    || targetTo != 3 || type != 4 || nativeSkillId != 1
                    || abilityScoreModifier != -1
                    || probabilityHigh != 55 || probabilityEqual != 45
                    || probabilityLow != 35
                    || Double.compare(intelligenceMultiplier, 0.25D) != 0
                    || includeMagicLevel || !includeMagicHit
                    || Double.compare(magicResistanceMultiplier, 0.05D) != 0
                    || maximumProbability != 90 || bossModifier != 0
                    || skillBookItemId != 49_133L
                    || !"$5697".equals(skillBookNameId)
                    || !"$5951".equals(skillRowNameId)
                    || counterMagicBypass || persisted
                    || key == null || key.isBlank()) {
                throw new IllegalArgumentException(
                        "invalid native Panic metadata");
            }
        }

        RequestShape requestShape() { return RequestShape.SELECTOR_ONLY; }

        int effectiveMpCost(int originalReduction) {
            if (originalReduction < 0 || originalReduction > 2) {
                throw new IllegalArgumentException(
                        "invalid Panic original INT reduction");
            }
            return Math.max(0, ResourceDatabase.value(this.mpCost, this.skillId, "PanicSpell", "mpCost") - originalReduction);
        }

        int probability(int casterLevel, int targetLevel,
                        int effectiveIntelligence, int totalMagicHit,
                        int targetMagicResistance, boolean targetBoss) {
            if (casterLevel <= 0 || targetLevel <= 0
                    || effectiveIntelligence < 0 || totalMagicHit < -127
                    || totalMagicHit > 127 || targetMagicResistance < 0) {
                throw new IllegalArgumentException(
                        "invalid native Panic probability input");
            }
            if (targetBoss && bossModifier == 0) return 0;
            double result = casterLevel > targetLevel ? probabilityHigh
                    : casterLevel == targetLevel ? probabilityEqual
                    : probabilityLow;
            result += effectiveIntelligence * intelligenceMultiplier;
            if (includeMagicHit) result += totalMagicHit;
            result -= targetMagicResistance * magicResistanceMultiplier;
            return Math.max(1, Math.min(maximumProbability, (int) result));
        }
    }

    /** Selected primary skill218 refreshable PC/NPC feedback-mark contract. */
    record JoyOfPainSpell(
            int skillId, int row, int column, int actionId, int effectGfx,
            int mpCost, int hpCost, int reuseDelayMillis,
            int clientCastClockMillis, int durationSeconds, int maximumRange,
            int targetTo, int type, int nativeSkillId,
            long skillBookItemId, String skillBookNameId,
            String skillRowNameId, boolean counterMagicBypass,
            boolean persisted, String key) {
        public int actionId() {
            return ResourceDatabase.value(this.actionId, this.skillId, "JoyOfPainSpell", "actionId");
        }

        public int durationSeconds() {
            return ResourceDatabase.value(this.durationSeconds, this.skillId, "JoyOfPainSpell", "durationSeconds");
        }

        public int effectGfx() {
            return ResourceDatabase.value(this.effectGfx, this.skillId, "JoyOfPainSpell", "effectGfx");
        }

        public int hpCost() {
            return ResourceDatabase.value(this.hpCost, this.skillId, "JoyOfPainSpell", "hpCost");
        }

        public int mpCost() {
            return ResourceDatabase.value(this.mpCost, this.skillId, "JoyOfPainSpell", "mpCost");
        }

        public int reuseDelayMillis() {
            return ResourceDatabase.value(this.reuseDelayMillis, this.skillId, "JoyOfPainSpell", "reuseDelayMillis");
        }

        JoyOfPainSpell {
            if (skillId != JOY_OF_PAIN
                    || skillId != (row << 3) + column + 1
                    || row != 27 || column != 1 || actionId != 19
                    || effectGfx != 6_528 || mpCost != 40 || hpCost != 0
                    || reuseDelayMillis != 3_000
                    || clientCastClockMillis != 0
                    || durationSeconds != 2 || maximumRange != 3
                    || targetTo != 3 || type != 2 || nativeSkillId != 2
                    || skillBookItemId != 49_134L
                    || !"$5698".equals(skillBookNameId)
                    || !"$5952".equals(skillRowNameId)
                    || !counterMagicBypass || persisted
                    || key == null || key.isBlank()) {
                throw new IllegalArgumentException(
                        "invalid native Joy of Pain metadata");
            }
        }

        RequestShape requestShape() { return RequestShape.SELECTOR_ONLY; }

        int effectiveMpCost(int originalReduction) {
            if (originalReduction < 0 || originalReduction > 2) {
                throw new IllegalArgumentException(
                        "invalid Joy of Pain original INT reduction");
            }
            return Math.max(0, ResourceDatabase.value(this.mpCost, this.skillId, "JoyOfPainSpell", "mpCost") - originalReduction);
        }
    }

    /** Selected primary skill205 object-backed Cube: Ignition contract. */
    record CubeIgnitionSpell(
            int skillId, int row, int column, int actionId,
            int cubeGfx, int allyEffectGfx, int enemyEffectGfx,
            int mpCost, int reuseDelayMillis, int clientCastClockMillis,
            int durationSeconds, int effectRadius, int targetTo, int type,
            int nativeSkillId, int damageValue,
            long itemConsumeId, int itemConsumeCount, int effectNpcId,
            long skillBookItemId, String skillBookNameId,
            String skillRowNameId, String key) {
        public long itemConsumeId() {
            return CombatDatabase.longValue(this.itemConsumeId, this.skillId, "itemConsumeId");
        }

        public int actionId() {
            return ResourceDatabase.value(this.actionId, this.skillId, "CubeIgnitionSpell", "actionId");
        }

        public int allyEffectGfx() {
            return ResourceDatabase.value(this.allyEffectGfx, this.skillId, "CubeIgnitionSpell", "allyEffectGfx");
        }

        public int cubeGfx() {
            return ResourceDatabase.value(this.cubeGfx, this.skillId, "CubeIgnitionSpell", "cubeGfx");
        }

        public int durationSeconds() {
            return ResourceDatabase.value(this.durationSeconds, this.skillId, "CubeIgnitionSpell", "durationSeconds");
        }

        public int enemyEffectGfx() {
            return ResourceDatabase.value(this.enemyEffectGfx, this.skillId, "CubeIgnitionSpell", "enemyEffectGfx");
        }

        public int itemConsumeCount() {
            return ResourceDatabase.value(this.itemConsumeCount, this.skillId, "CubeIgnitionSpell", "itemConsumeCount");
        }

        public int mpCost() {
            return ResourceDatabase.value(this.mpCost, this.skillId, "CubeIgnitionSpell", "mpCost");
        }

        public int reuseDelayMillis() {
            return ResourceDatabase.value(this.reuseDelayMillis, this.skillId, "CubeIgnitionSpell", "reuseDelayMillis");
        }

        CubeIgnitionSpell {
            if (skillId != CUBE_IGNITION
                    || skillId != (row << 3) + column + 1
                    || row != 25 || column != 4 || actionId != 19
                    || cubeGfx != BattleCubeIgnitionEffects.CUBE_GFX
                    || allyEffectGfx != BattleCubeIgnitionEffects.ALLY_GFX
                    || enemyEffectGfx != BattleCubeIgnitionEffects.ENEMY_GFX
                    || mpCost != 30 || reuseDelayMillis != 5_000
                    || clientCastClockMillis != 0
                    || durationSeconds != BattleCubeIgnitionEffects.DURATION_SECONDS
                    || effectRadius != BattleCubeIgnitionEffects.EFFECT_RADIUS
                    || targetTo != 32 || type != 2 || nativeSkillId != 16
                    || damageValue != BattleCubeIgnitionEffects.ENEMY_DAMAGE
                    || itemConsumeId != 49_156L || itemConsumeCount != 1
                    || effectNpcId != BattleCubeIgnitionEffects.NPC_ID
                    || skillBookItemId != 49_121L
                    || !"$5685".equals(skillBookNameId)
                    || !"$5939".equals(skillRowNameId)
                    || key == null || key.isBlank()) {
                throw new IllegalArgumentException(
                        "invalid native Cube: Ignition metadata");
            }
        }

        RequestShape requestShape() { return RequestShape.SELECTOR_ONLY; }

        int effectiveMpCost(int originalReduction) {
            if (originalReduction < 0 || originalReduction > 2) {
                throw new IllegalArgumentException(
                        "invalid Cube: Ignition original INT reduction");
            }
            return Math.max(0, ResourceDatabase.value(this.mpCost, this.skillId, "CubeIgnitionSpell", "mpCost") - originalReduction);
        }
    }

    /** Selected primary skill210 object-backed Cube: Quake contract. */
    record CubeQuakeSpell(
            int skillId, int row, int column, int actionId,
            int cubeGfx, int allyEffectGfx, int enemyEffectGfx,
            int mpCost, int reuseDelayMillis, int clientCastClockMillis,
            int durationSeconds, int effectRadius, int targetTo, int type,
            int nativeSkillId, int resistanceValue,
            long itemConsumeId, int itemConsumeCount, int effectNpcId,
            long skillBookItemId, String skillBookNameId,
            String skillRowNameId, String key) {
        public long itemConsumeId() {
            return CombatDatabase.longValue(this.itemConsumeId, this.skillId, "itemConsumeId");
        }

        public int actionId() {
            return ResourceDatabase.value(this.actionId, this.skillId, "CubeQuakeSpell", "actionId");
        }

        public int allyEffectGfx() {
            return ResourceDatabase.value(this.allyEffectGfx, this.skillId, "CubeQuakeSpell", "allyEffectGfx");
        }

        public int cubeGfx() {
            return ResourceDatabase.value(this.cubeGfx, this.skillId, "CubeQuakeSpell", "cubeGfx");
        }

        public int durationSeconds() {
            return ResourceDatabase.value(this.durationSeconds, this.skillId, "CubeQuakeSpell", "durationSeconds");
        }

        public int enemyEffectGfx() {
            return ResourceDatabase.value(this.enemyEffectGfx, this.skillId, "CubeQuakeSpell", "enemyEffectGfx");
        }

        public int itemConsumeCount() {
            return ResourceDatabase.value(this.itemConsumeCount, this.skillId, "CubeQuakeSpell", "itemConsumeCount");
        }

        public int mpCost() {
            return ResourceDatabase.value(this.mpCost, this.skillId, "CubeQuakeSpell", "mpCost");
        }

        public int reuseDelayMillis() {
            return ResourceDatabase.value(this.reuseDelayMillis, this.skillId, "CubeQuakeSpell", "reuseDelayMillis");
        }

        CubeQuakeSpell {
            if (skillId != CUBE_QUAKE
                    || skillId != (row << 3) + column + 1
                    || row != 26 || column != 1 || actionId != 19
                    || cubeGfx != BattleCubeQuakeEffects.CUBE_GFX
                    || allyEffectGfx != BattleCubeQuakeEffects.ALLY_GFX
                    || enemyEffectGfx != BattleCubeQuakeEffects.ENEMY_GFX
                    || mpCost != 35 || reuseDelayMillis != 5_000
                    || clientCastClockMillis != 0
                    || durationSeconds != BattleCubeQuakeEffects.DURATION_SECONDS
                    || effectRadius != BattleCubeQuakeEffects.EFFECT_RADIUS
                    || targetTo != 32 || type != 2 || nativeSkillId != 2
                    || resistanceValue != BattleCubeQuakeEffects.ALLY_EARTH_RESISTANCE
                    || itemConsumeId != 49_156L || itemConsumeCount != 1
                    || effectNpcId != BattleCubeQuakeEffects.NPC_ID
                    || skillBookItemId != 49_126L
                    || !"$5690".equals(skillBookNameId)
                    || !"$5944".equals(skillRowNameId)
                    || key == null || key.isBlank()) {
                throw new IllegalArgumentException(
                        "invalid native Cube: Quake metadata");
            }
        }

        RequestShape requestShape() { return RequestShape.SELECTOR_ONLY; }

        int effectiveMpCost(int originalReduction) {
            if (originalReduction < 0 || originalReduction > 2) {
                throw new IllegalArgumentException(
                        "invalid Cube: Quake original INT reduction");
            }
            return Math.max(0, ResourceDatabase.value(this.mpCost, this.skillId, "CubeQuakeSpell", "mpCost") - originalReduction);
        }
    }

    /** Selected primary skill215 object-backed Cube: Shock contract. */
    record CubeShockSpell(
            int skillId, int row, int column, int actionId,
            int cubeGfx, int allyEffectGfx, int enemyEffectGfx,
            int mpCost, int reuseDelayMillis, int clientCastClockMillis,
            int durationSeconds, int effectRadius, int targetTo, int type,
            int nativeSkillId, int resistanceValue, int enemyMrModifier,
            long itemConsumeId, int itemConsumeCount, int effectNpcId,
            long skillBookItemId, String skillBookNameId,
            String skillRowNameId, String key) {
        public long itemConsumeId() {
            return CombatDatabase.longValue(this.itemConsumeId, this.skillId, "itemConsumeId");
        }

        public int actionId() {
            return ResourceDatabase.value(this.actionId, this.skillId, "CubeShockSpell", "actionId");
        }

        public int allyEffectGfx() {
            return ResourceDatabase.value(this.allyEffectGfx, this.skillId, "CubeShockSpell", "allyEffectGfx");
        }

        public int cubeGfx() {
            return ResourceDatabase.value(this.cubeGfx, this.skillId, "CubeShockSpell", "cubeGfx");
        }

        public int durationSeconds() {
            return ResourceDatabase.value(this.durationSeconds, this.skillId, "CubeShockSpell", "durationSeconds");
        }

        public int enemyEffectGfx() {
            return ResourceDatabase.value(this.enemyEffectGfx, this.skillId, "CubeShockSpell", "enemyEffectGfx");
        }

        public int itemConsumeCount() {
            return ResourceDatabase.value(this.itemConsumeCount, this.skillId, "CubeShockSpell", "itemConsumeCount");
        }

        public int mpCost() {
            return ResourceDatabase.value(this.mpCost, this.skillId, "CubeShockSpell", "mpCost");
        }

        public int reuseDelayMillis() {
            return ResourceDatabase.value(this.reuseDelayMillis, this.skillId, "CubeShockSpell", "reuseDelayMillis");
        }

        CubeShockSpell {
            if (skillId != CUBE_SHOCK
                    || skillId != (row << 3) + column + 1
                    || row != 26 || column != 6 || actionId != 19
                    || cubeGfx != BattleCubeShockEffects.CUBE_GFX
                    || allyEffectGfx != BattleCubeShockEffects.ALLY_GFX
                    || enemyEffectGfx != BattleCubeShockEffects.ENEMY_GFX
                    || mpCost != 55 || reuseDelayMillis != 5_000
                    || clientCastClockMillis != 0
                    || durationSeconds != BattleCubeShockEffects.DURATION_SECONDS
                    || effectRadius != BattleCubeShockEffects.EFFECT_RADIUS
                    || targetTo != 32 || type != 2 || nativeSkillId != 64
                    || resistanceValue
                        != BattleCubeShockEffects.ALLY_WIND_RESISTANCE
                    || enemyMrModifier
                        != BattleCubeShockEffects.ENEMY_MAGIC_RESISTANCE
                    || itemConsumeId != 49_156L || itemConsumeCount != 1
                    || effectNpcId != BattleCubeShockEffects.NPC_ID
                    || skillBookItemId != 49_131L
                    || !"$5695".equals(skillBookNameId)
                    || !"$5949".equals(skillRowNameId)
                    || key == null || key.isBlank()) {
                throw new IllegalArgumentException(
                        "invalid native Cube: Shock metadata");
            }
        }

        RequestShape requestShape() { return RequestShape.SELECTOR_ONLY; }

        int effectiveMpCost(int originalReduction) {
            if (originalReduction < 0 || originalReduction > 2) {
                throw new IllegalArgumentException(
                        "invalid Cube: Shock original INT reduction");
            }
            return Math.max(0, ResourceDatabase.value(this.mpCost, this.skillId, "CubeShockSpell", "mpCost") - originalReduction);
        }
    }

    /** Selected primary skill220 object-backed Cube: Harmony contract. */
    record CubeHarmonySpell(
            int skillId, int row, int column, int actionId,
            int cubeGfx, int effectGfx,
            int mpCost, int reuseDelayMillis, int clientCastClockMillis,
            int durationSeconds, int effectRadius, int targetTo, int type,
            int nativeSkillId, int mpRecovery, int hpDamage,
            long itemConsumeId, int itemConsumeCount, int effectNpcId,
            long skillBookItemId, String skillBookNameId,
            String skillRowNameId, String key) {
        public long itemConsumeId() {
            return CombatDatabase.longValue(this.itemConsumeId, this.skillId, "itemConsumeId");
        }

        public int actionId() {
            return ResourceDatabase.value(this.actionId, this.skillId, "CubeHarmonySpell", "actionId");
        }

        public int cubeGfx() {
            return ResourceDatabase.value(this.cubeGfx, this.skillId, "CubeHarmonySpell", "cubeGfx");
        }

        public int durationSeconds() {
            return ResourceDatabase.value(this.durationSeconds, this.skillId, "CubeHarmonySpell", "durationSeconds");
        }

        public int effectGfx() {
            return ResourceDatabase.value(this.effectGfx, this.skillId, "CubeHarmonySpell", "effectGfx");
        }

        public int itemConsumeCount() {
            return ResourceDatabase.value(this.itemConsumeCount, this.skillId, "CubeHarmonySpell", "itemConsumeCount");
        }

        public int mpCost() {
            return ResourceDatabase.value(this.mpCost, this.skillId, "CubeHarmonySpell", "mpCost");
        }

        public int reuseDelayMillis() {
            return ResourceDatabase.value(this.reuseDelayMillis, this.skillId, "CubeHarmonySpell", "reuseDelayMillis");
        }

        CubeHarmonySpell {
            if (skillId != CUBE_HARMONY
                    || skillId != (row << 3) + column + 1
                    || row != 27 || column != 3 || actionId != 19
                    || cubeGfx != BattleCubeHarmonyEffects.CUBE_GFX
                    || effectGfx != BattleCubeHarmonyEffects.EFFECT_GFX
                    || mpCost != 55 || reuseDelayMillis != 5_000
                    || clientCastClockMillis != 32
                    || durationSeconds != BattleCubeHarmonyEffects.DURATION_SECONDS
                    || effectRadius != BattleCubeHarmonyEffects.EFFECT_RADIUS
                    || targetTo != 32 || type != 2 || nativeSkillId != 8
                    || mpRecovery != BattleCubeHarmonyEffects.MP_RECOVERY
                    || hpDamage != BattleCubeHarmonyEffects.HP_DAMAGE
                    || itemConsumeId != 49_156L || itemConsumeCount != 2
                    || effectNpcId != BattleCubeHarmonyEffects.NPC_ID
                    || skillBookItemId != 49_136L
                    || !"$5700".equals(skillBookNameId)
                    || !"$5954".equals(skillRowNameId)
                    || key == null || key.isBlank()) {
                throw new IllegalArgumentException(
                        "invalid native Cube: Harmony metadata");
            }
        }

        RequestShape requestShape() { return RequestShape.SELECTOR_ONLY; }

        int effectiveMpCost(int originalReduction) {
            if (originalReduction < 0 || originalReduction > 2) {
                throw new IllegalArgumentException(
                        "invalid Cube: Harmony original INT reduction");
            }
            return Math.max(0, ResourceDatabase.value(this.mpCost, this.skillId, "CubeHarmonySpell", "mpCost") - originalReduction);
        }
    }

    /** Selected primary skill211 ordinary PC/NPC Patience contract. */
    record PatienceSpell(
            int skillId, int row, int column, int actionId, int effectGfx,
            int mpCost, int reuseDelayMillis, int clientCastClockMillis,
            int durationSeconds, int maximumRange, int targetTo, int type,
            int nativeSkillId, int physicalDamageReduction,
            boolean counterMagicBypass, boolean persisted,
            long skillBookItemId, String skillBookNameId,
            String skillRowNameId, String key) {
        public int actionId() {
            return ResourceDatabase.value(this.actionId, this.skillId, "PatienceSpell", "actionId");
        }

        public int durationSeconds() {
            return ResourceDatabase.value(this.durationSeconds, this.skillId, "PatienceSpell", "durationSeconds");
        }

        public int effectGfx() {
            return ResourceDatabase.value(this.effectGfx, this.skillId, "PatienceSpell", "effectGfx");
        }

        public int mpCost() {
            return ResourceDatabase.value(this.mpCost, this.skillId, "PatienceSpell", "mpCost");
        }

        public int reuseDelayMillis() {
            return ResourceDatabase.value(this.reuseDelayMillis, this.skillId, "PatienceSpell", "reuseDelayMillis");
        }

        PatienceSpell {
            if (skillId != PATIENCE
                    || skillId != (row << 3) + column + 1
                    || row != 26 || column != 2 || actionId != 19
                    || effectGfx != 6_507 || mpCost != 25
                    || reuseDelayMillis != 1_000
                    || clientCastClockMillis != 16
                    || durationSeconds != BattlePatienceStatus.DURATION_SECONDS
                    || maximumRange != 3 || targetTo != 3 || type != 2
                    || nativeSkillId != 4
                    || physicalDamageReduction
                        != BattlePatienceStatus.PHYSICAL_REDUCTION
                    || !counterMagicBypass || persisted
                    || skillBookItemId != 49_127L
                    || !"$5691".equals(skillBookNameId)
                    || !"$5945".equals(skillRowNameId)
                    || key == null || key.isBlank()) {
                throw new IllegalArgumentException(
                        "invalid native Patience metadata");
            }
        }

        RequestShape requestShape() { return RequestShape.SELECTOR_ONLY; }

        int effectiveMpCost(int originalReduction) {
            if (originalReduction < 0 || originalReduction > 2) {
                throw new IllegalArgumentException(
                        "invalid Patience original INT reduction");
            }
            return Math.max(0, ResourceDatabase.value(this.mpCost, this.skillId, "PatienceSpell", "mpCost") - originalReduction);
        }
    }

    /** Selected primary skill212 probability-to-FOG sleep contract. */
    record PhantasmSpell(
            int skillId, int row, int column, int actionId, int effectGfx,
            int mpCost, int hpCost, int reuseDelayMillis,
            int clientCastClockMillis, int durationSeconds, int maximumRange,
            int targetTo, int type, int nativeSkillId,
            int probabilityHigh, int probabilityEqual, int probabilityLow,
            boolean checkMagicHit, boolean checkStoneResistance,
            boolean persisted,
            long skillBookItemId, String skillBookNameId,
            String skillRowNameId, String key) {
        public int actionId() {
            return ResourceDatabase.value(this.actionId, this.skillId, "PhantasmSpell", "actionId");
        }

        public int durationSeconds() {
            return ResourceDatabase.value(this.durationSeconds, this.skillId, "PhantasmSpell", "durationSeconds");
        }

        public int effectGfx() {
            return ResourceDatabase.value(this.effectGfx, this.skillId, "PhantasmSpell", "effectGfx");
        }

        public int hpCost() {
            return ResourceDatabase.value(this.hpCost, this.skillId, "PhantasmSpell", "hpCost");
        }

        public int mpCost() {
            return ResourceDatabase.value(this.mpCost, this.skillId, "PhantasmSpell", "mpCost");
        }

        public int reuseDelayMillis() {
            return ResourceDatabase.value(this.reuseDelayMillis, this.skillId, "PhantasmSpell", "reuseDelayMillis");
        }

        PhantasmSpell {
            if (skillId != PHANTASM
                    || skillId != (row << 3) + column + 1
                    || row != 26 || column != 3 || actionId != 19
                    || effectGfx != 6_530 || mpCost != 30 || hpCost != 25
                    || reuseDelayMillis != 3_000
                    || clientCastClockMillis != 640
                    || durationSeconds != 5 || maximumRange != 3
                    || targetTo != 3 || type != 4 || nativeSkillId != 8
                    || probabilityHigh != 35 || probabilityEqual != 35
                    || probabilityLow != 35 || !checkMagicHit
                    || !checkStoneResistance || persisted
                    || skillBookItemId != 49_128L
                    || !"$5692".equals(skillBookNameId)
                    || !"$5946".equals(skillRowNameId)
                    || key == null || key.isBlank()) {
                throw new IllegalArgumentException(
                        "invalid native Phantasm metadata");
            }
        }

        RequestShape requestShape() { return RequestShape.SELECTOR_ONLY; }

        int effectiveMpCost(int originalReduction) {
            if (originalReduction < 0 || originalReduction > 2) {
                throw new IllegalArgumentException(
                        "invalid Phantasm original INT reduction");
            }
            return Math.max(0, ResourceDatabase.value(this.mpCost, this.skillId, "PhantasmSpell", "mpCost") - originalReduction);
        }

        int probability(int casterLevel, int targetLevel,
                        int totalMagicHit, int targetStoneResistance) {
            if (casterLevel <= 0 || targetLevel <= 0
                    || targetStoneResistance < -128
                    || targetStoneResistance > 127) {
                throw new IllegalArgumentException(
                        "invalid native Phantasm probability input");
            }
            int base = casterLevel > targetLevel ? probabilityHigh
                    : casterLevel == targetLevel ? probabilityEqual
                    : probabilityLow;
            int result = Math.addExact(base, totalMagicHit);
            result = Math.subtractExact(result, targetStoneResistance);
            return Math.max(1, Math.min(90, result));
        }
    }

    /** Selected retained source contract for Dragon Knight skill 192. */
    record ThunderGrabSpell(
            AttackSpell attackSpell, int hpCost, int clientCastClockMillis,
            int minimumBindSeconds, int maximumBindSeconds,
            int higherLevelChancePercent, int equalLevelChancePercent,
            int lowerLevelChancePercent, int maximumChancePercent,
            boolean counterMagicBypass, long skillBookItemId,
            String skillBookNameId, String key) {
        public int hpCost() {
            return ResourceDatabase.value(this.hpCost, skillId(), "ThunderGrabSpell", "hpCost");
        }

        public int maximumBindSeconds() {
            return ResourceDatabase.value(this.maximumBindSeconds, skillId(), "ThunderGrabSpell", "maximumBindSeconds");
        }

        public int minimumBindSeconds() {
            return ResourceDatabase.value(this.minimumBindSeconds, skillId(), "ThunderGrabSpell", "minimumBindSeconds");
        }

        ThunderGrabSpell {
            if (attackSpell == null
                    || attackSpell.skillId() != THUNDER_GRAB
                    || attackSpell.row() != 23 || attackSpell.column() != 7
                    || attackSpell.actionId() != 18
                    || attackSpell.effectGfx() != 6512
                    || attackSpell.mpCost() != 0
                    || attackSpell.reuseDelayMillis() != 4_000
                    || attackSpell.maximumRange() != 8
                    || attackSpell.baseDamage() != 80
                    || attackSpell.diceCount() != 2
                    || attackSpell.dieSides() != 10
                    || attackSpell.attribute() != ATTRIBUTE_WIND
                    || hpCost != 45 || clientCastClockMillis != 1_800
                    || minimumBindSeconds != 2 || maximumBindSeconds != 4
                    || higherLevelChancePercent != 50
                    || equalLevelChancePercent != 30
                    || lowerLevelChancePercent != 15
                    || maximumChancePercent != 90
                    || !counterMagicBypass || skillBookItemId != 49_113L
                    || !"$5711".equals(skillBookNameId)
                    || key == null || key.isBlank()) {
                throw new IllegalArgumentException(
                        "invalid native Thunder Grab metadata");
            }
        }

        int skillId() { return attackSpell.skillId(); }
        int row() { return attackSpell.row(); }
        int column() { return attackSpell.column(); }
        int maximumRange() { return attackSpell.maximumRange(); }

        int successChancePercent(int casterLevel, int targetLevel,
                                 int targetSustainResistance,
                                 boolean bossTarget) {
            if (casterLevel <= 0 || targetLevel <= 0) {
                throw new IllegalArgumentException(
                        "Thunder Grab levels must be positive");
            }
            if (bossTarget) return 0;
            int base = casterLevel > targetLevel ? higherLevelChancePercent
                    : casterLevel == targetLevel ? equalLevelChancePercent
                    : lowerLevelChancePercent;
            return Math.min(maximumChancePercent,
                    Math.max(1, base - targetSustainResistance));
        }

        boolean succeeds(int casterLevel, int targetLevel,
                         int targetSustainResistance, boolean bossTarget,
                         int percentileRoll) {
            if (percentileRoll < 1 || percentileRoll > 100) {
                throw new IllegalArgumentException(
                        "Thunder Grab roll is outside percentile range");
            }
            int chance = successChancePercent(casterLevel, targetLevel,
                    targetSustainResistance, bossTarget);
            return chance > 0 && percentileRoll <= chance;
        }

        int bindDurationSeconds(int inclusiveRandomOffset) {
            int maximumOffset = maximumBindSeconds - minimumBindSeconds;
            if (inclusiveRandomOffset < 0
                    || inclusiveRandomOffset > maximumOffset) {
                throw new IllegalArgumentException(
                        "Thunder Grab duration offset is outside source range");
            }
            return minimumBindSeconds + inclusiveRandomOffset;
        }
    }

    interface DirectionalLineSpell {
        int skillId();
        int row();
        int column();
        int actionId();
        int effectGfx();
        int mpCost();
        int reuseDelayMillis();
        int maximumRange();
        int baseDamage();
        int diceCount();
        int dieSides();
        int attribute();
        String key();

        default int hpCost() { return 0; }

        default int effectiveMpCost(int effectiveIntelligence,
                                    int originalReduction) {
            return mpCost();
        }
    }

    record FreezeAttackSpell(int skillId, int row, int column, int actionId,
                             int effectGfx, int mpCost, int hpCost,
                             int reuseDelayMillis, int clientCastClockMillis,
                             int maximumRange, int areaRadius,
                             boolean lineTargeted, int durationSeconds,
                             int baseDamage, int diceCount, int dieSides,
                             int attribute, int lawfulDelta, int probabilityHigh,
                             int probabilityEqual, int probabilityLow,
                             double intelligenceMultiplier,
                             double magicResistanceMultiplier,
                             boolean includeMagicLevel, boolean includeMagicHit,
                             String key) implements DirectionalLineSpell {
        int nativeSourceOriginalMaximumRange() {
            return this.maximumRange;
        }

        @Override
        public int maximumRange() {
            return NativeSourceFreezeTarget.range(this);
        }

        public int actionId() {
            return ResourceDatabase.value(this.actionId, this.skillId, "FreezeAttackSpell", "actionId");
        }

        public int baseDamage() {
            return ResourceDatabase.value(this.baseDamage, this.skillId, "FreezeAttackSpell", "baseDamage");
        }

        public int diceCount() {
            return ResourceDatabase.value(this.diceCount, this.skillId, "FreezeAttackSpell", "diceCount");
        }

        public int dieSides() {
            return ResourceDatabase.value(this.dieSides, this.skillId, "FreezeAttackSpell", "dieSides");
        }

        public int durationSeconds() {
            return ResourceDatabase.value(this.durationSeconds, this.skillId, "FreezeAttackSpell", "durationSeconds");
        }

        public int effectGfx() {
            return ResourceDatabase.value(this.effectGfx, this.skillId, "FreezeAttackSpell", "effectGfx");
        }

        public int hpCost() {
            return ResourceDatabase.value(this.hpCost, this.skillId, "FreezeAttackSpell", "hpCost");
        }

        public int mpCost() {
            return ResourceDatabase.value(this.mpCost, this.skillId, "FreezeAttackSpell", "mpCost");
        }

        public int reuseDelayMillis() {
            return ResourceDatabase.value(this.reuseDelayMillis, this.skillId, "FreezeAttackSpell", "reuseDelayMillis");
        }

        FreezeAttackSpell {
            if (skillId != (row << 3) + column + 1 || row < 0 || row > 0xFF
                    || column < 0 || column > 7 || actionId < 0 || actionId > 0xFF
                    || effectGfx <= 0 || effectGfx > 0xFFFF || mpCost < 0
                    || hpCost < 0 || mpCost == 0 && hpCost == 0
                    || reuseDelayMillis < 0 || clientCastClockMillis < 0
                    || maximumRange < 0 || areaRadius < 0
                    || (maximumRange == 0) == (areaRadius == 0)
                    || lineTargeted && (maximumRange <= 0 || areaRadius != 0)
                    || durationSeconds <= 0 || durationSeconds > 0xFFFF
                    || baseDamage < 0 || diceCount < 0 || dieSides < 0
                    || (diceCount == 0) != (dieSides == 0)
                    || baseDamage == 0 && diceCount == 0
                    || attribute < 0 || lawfulDelta > 0 || lawfulDelta < -0x7FFF
                    || probabilityHigh < 0 || probabilityHigh > 100
                    || probabilityEqual < 0 || probabilityEqual > 100
                    || probabilityLow < 0 || probabilityLow > 100
                    || !Double.isFinite(intelligenceMultiplier)
                    || intelligenceMultiplier < 0.0D
                    || !Double.isFinite(magicResistanceMultiplier)
                    || magicResistanceMultiplier < 0.0D
                    || key == null || key.isBlank()) {
                throw new IllegalArgumentException("invalid native freeze-attack metadata");
            }
        }

        RequestShape requestShape() {
            return areaRadius > 0 || lineTargeted
                    ? RequestShape.SELECTOR_ONLY : RequestShape.OBJECT_TARGET;
        }

        int probability(int casterLevel, int targetLevel, int effectiveIntelligence,
                        int magicLevel, int magicHit,
                        int targetMagicResistance, int targetFreezeResistance) {
            if (casterLevel <= 0 || targetLevel <= 0 || effectiveIntelligence < 0
                    || magicLevel < 0 || targetMagicResistance < 0
                    || targetFreezeResistance < -128 || targetFreezeResistance > 127) {
                throw new IllegalArgumentException("invalid freeze probability inputs");
            }
            double result = casterLevel > targetLevel ? probabilityHigh
                    : casterLevel == targetLevel ? probabilityEqual : probabilityLow;
            result += effectiveIntelligence * intelligenceMultiplier;
            if (includeMagicHit) result += magicHit;
            if (includeMagicLevel) result += magicLevel;
            result -= targetMagicResistance * magicResistanceMultiplier;
            result -= targetFreezeResistance;
            result = Math.max(1.0D, Math.min(100.0D, result));
            return Math.max(1, Math.min(90, (int) result));
        }

        /** Retained spell-circle reductions through skill 80 apply before the MP floor. */
        @Override
        public int effectiveMpCost(int effectiveIntelligence, int originalReduction) {
            if (effectiveIntelligence < 1 || effectiveIntelligence > 254
                    || originalReduction < 0 || originalReduction > 2) {
                throw new IllegalArgumentException("invalid freeze MP inputs");
            }
            int result = ResourceDatabase.value(this.mpCost, this.skillId, "FreezeAttackSpell", "mpCost");
            if (effectiveIntelligence > 12 && skillId > 8 && skillId <= 80) result--;
            if (effectiveIntelligence > 13 && skillId > 16 && skillId <= 80) result--;
            if (effectiveIntelligence > 14 && skillId > 24 && skillId <= 80) result--;
            if (effectiveIntelligence > 15 && skillId > 32 && skillId <= 80) result--;
            if (effectiveIntelligence > 16 && skillId > 40 && skillId <= 80) result--;
            if (effectiveIntelligence > 17 && skillId > 48 && skillId <= 80) result--;
            if (effectiveIntelligence > 18 && skillId > 56 && skillId <= 80) result--;
            if (ResourceDatabase.value(this.mpCost, this.skillId, "FreezeAttackSpell", "mpCost") > 0) result = Math.max(result, 1);
            return Math.max(0, result - originalReduction);
        }
    }

    record AreaAttackSpell(int skillId, int row, int column, int actionId,
                           int effectGfx, int mpCost, int hpCost,
                           int reuseDelayMillis,
                           int maximumRange, int areaRadius, int baseDamage,
                           int diceCount, int dieSides, int attribute,
                           int lawfulDelta, String key) {
        public int actionId() {
            return ResourceDatabase.value(this.actionId, this.skillId, "AreaAttackSpell", "actionId");
        }

        public int baseDamage() {
            return ResourceDatabase.value(this.baseDamage, this.skillId, "AreaAttackSpell", "baseDamage");
        }

        public int diceCount() {
            return ResourceDatabase.value(this.diceCount, this.skillId, "AreaAttackSpell", "diceCount");
        }

        public int dieSides() {
            return ResourceDatabase.value(this.dieSides, this.skillId, "AreaAttackSpell", "dieSides");
        }

        public int effectGfx() {
            return ResourceDatabase.value(this.effectGfx, this.skillId, "AreaAttackSpell", "effectGfx");
        }

        public int hpCost() {
            return ResourceDatabase.value(this.hpCost, this.skillId, "AreaAttackSpell", "hpCost");
        }

        public int mpCost() {
            return ResourceDatabase.value(this.mpCost, this.skillId, "AreaAttackSpell", "mpCost");
        }

        public int reuseDelayMillis() {
            return ResourceDatabase.value(this.reuseDelayMillis, this.skillId, "AreaAttackSpell", "reuseDelayMillis");
        }

        AreaAttackSpell {
            if (skillId != (row << 3) + column + 1 || row < 0 || row > 0xFF
                    || column < 0 || column > 7 || actionId < 0 || actionId > 0xFF
                    || effectGfx <= 0 || effectGfx > 0xFFFF || mpCost < 0
                    || hpCost < 0
                    || reuseDelayMillis < 0 || maximumRange < 0 || areaRadius <= 0
                    || baseDamage < 0 || diceCount < 0 || dieSides < 0
                    || (diceCount == 0) != (dieSides == 0)
                    || baseDamage == 0 && diceCount == 0
                    || attribute < 0 || lawfulDelta > 0 || lawfulDelta < -0x7FFF
                    || key == null || key.isBlank()) {
                throw new IllegalArgumentException("invalid native area-attack spell metadata");
            }
        }

        /**
         * The selected executable's generic Dragon Knight producer omits the
         * retained target tail for skill 184. Other ranged area spells retain
         * the ordinary object-target producer; range-zero spells are centred
         * on the caster and therefore selector-only as well.
         */
        RequestShape requestShape() {
            return maximumRange == 0 || skillId == MAGMA_BREATH
                    ? RequestShape.SELECTOR_ONLY
                    : RequestShape.OBJECT_TARGET;
        }

        /** Retained spell-circle reductions through skill 80, then original INT. */
        int effectiveMpCost(int effectiveIntelligence, int originalReduction) {
            if (effectiveIntelligence < 1 || effectiveIntelligence > 254
                    || originalReduction < 0 || originalReduction > 2) {
                throw new IllegalArgumentException("invalid area-attack MP inputs");
            }
            int result = ResourceDatabase.value(this.mpCost, this.skillId, "AreaAttackSpell", "mpCost");
            if (effectiveIntelligence > 12 && skillId > 8 && skillId <= 80) result--;
            if (effectiveIntelligence > 13 && skillId > 16 && skillId <= 80) result--;
            if (effectiveIntelligence > 14 && skillId > 24 && skillId <= 80) result--;
            if (effectiveIntelligence > 15 && skillId > 32 && skillId <= 80) result--;
            if (effectiveIntelligence > 16 && skillId > 40 && skillId <= 80) result--;
            if (effectiveIntelligence > 17 && skillId > 48 && skillId <= 80) result--;
            if (effectiveIntelligence > 18 && skillId > 56 && skillId <= 80) result--;
            if (ResourceDatabase.value(this.mpCost, this.skillId, "AreaAttackSpell", "mpCost") > 0) result = Math.max(result, 1);
            return Math.max(0, result - originalReduction);
        }
    }

    record LineAttackSpell(int skillId, int row, int column, int actionId,
                           int effectGfx, int mpCost, int reuseDelayMillis,
                           int maximumRange, int sourceAreaValue, int baseDamage,
                           int diceCount, int dieSides, int attribute, String key)
            implements DirectionalLineSpell {
        public int baseDamage() {
            return ResourceDatabase.value(this.baseDamage, this.skillId, "LineAttackSpell", "baseDamage");
        }

        public int diceCount() {
            return ResourceDatabase.value(this.diceCount, this.skillId, "LineAttackSpell", "diceCount");
        }

        public int dieSides() {
            return ResourceDatabase.value(this.dieSides, this.skillId, "LineAttackSpell", "dieSides");
        }

        public int effectGfx() {
            return ResourceDatabase.value(this.effectGfx, this.skillId, "LineAttackSpell", "effectGfx");
        }

        LineAttackSpell {
            if (skillId != (row << 3) + column + 1 || row < 0 || row > 0xFF
                    || column < 0 || column > 7 || actionId < 0 || actionId > 0xFF
                    || effectGfx <= 0 || effectGfx > 0xFFFF || mpCost < 0
                    || reuseDelayMillis < 0 || maximumRange < 0 || sourceAreaValue <= 0
                    || baseDamage < 0 || diceCount <= 0 || dieSides <= 0
                    || attribute < 0 || key == null || key.isBlank()) {
                throw new IllegalArgumentException("invalid native line-attack spell metadata");
            }
        }
    }

    record TurnUndeadSpell(int skillId, int row, int column, int actionId,
                           int effectGfx, int mpCost, int reuseDelayMillis,
                           int maximumRange, int probabilityDice,
                           int probabilityValue, int failureMessage, String key) {
        public int actionId() {
            return ResourceDatabase.value(this.actionId, this.skillId, "TurnUndeadSpell", "actionId");
        }

        public int mpCost() {
            return ResourceDatabase.value(this.mpCost, this.skillId, "TurnUndeadSpell", "mpCost");
        }

        public int probabilityDice() {
            return ResourceDatabase.value(this.probabilityDice, this.skillId, "TurnUndeadSpell", "probabilityDice");
        }

        public int probabilityValue() {
            return ResourceDatabase.value(this.probabilityValue, this.skillId, "TurnUndeadSpell", "probabilityValue");
        }

        public int reuseDelayMillis() {
            return ResourceDatabase.value(this.reuseDelayMillis, this.skillId, "TurnUndeadSpell", "reuseDelayMillis");
        }

        TurnUndeadSpell {
            if (skillId != (row << 3) + column + 1 || row < 0 || row > 0xFF
                    || column < 0 || column > 7 || actionId < 0 || actionId > 0xFF
                    || effectGfx <= 0 || effectGfx > 0xFFFF || mpCost < 0
                    || reuseDelayMillis < 0 || maximumRange < 0
                    || probabilityDice <= 0 || probabilityValue < 0
                    || failureMessage <= 0 || failureMessage > 0xFFFF
                    || key == null || key.isBlank()) {
                throw new IllegalArgumentException("invalid native turn-undead spell metadata");
            }
        }
    }

    record ManaDrainSpell(int skillId, int row, int column, int actionId,
                          int effectGfx, int mpCost, int hpCost,
                          int reuseDelayMillis, int maximumRange,
                          int probabilityDice, int bossModifier,
                          int failureMessage, String key) {
        public int actionId() {
            return ResourceDatabase.value(this.actionId, this.skillId, "ManaDrainSpell", "actionId");
        }

        public int effectGfx() {
            return ResourceDatabase.value(this.effectGfx, this.skillId, "ManaDrainSpell", "effectGfx");
        }

        public int hpCost() {
            return ResourceDatabase.value(this.hpCost, this.skillId, "ManaDrainSpell", "hpCost");
        }

        public int mpCost() {
            return ResourceDatabase.value(this.mpCost, this.skillId, "ManaDrainSpell", "mpCost");
        }

        public int probabilityDice() {
            return ResourceDatabase.value(this.probabilityDice, this.skillId, "ManaDrainSpell", "probabilityDice");
        }

        public int reuseDelayMillis() {
            return ResourceDatabase.value(this.reuseDelayMillis, this.skillId, "ManaDrainSpell", "reuseDelayMillis");
        }

        ManaDrainSpell {
            if (skillId != (row << 3) + column + 1 || row < 0 || row > 0xFF
                    || column < 0 || column > 7 || actionId < 0 || actionId > 0xFF
                    || effectGfx <= 0 || effectGfx > 0xFFFF || mpCost <= 0
                    || hpCost < 0 || reuseDelayMillis < 0 || maximumRange < 0
                    || probabilityDice <= 0 || bossModifier <= 0
                    || failureMessage <= 0 || failureMessage > 0xFFFF
                    || key == null || key.isBlank()) {
                throw new IllegalArgumentException("invalid native mana-drain metadata");
            }
        }

        int effectiveMpCost(int effectiveIntelligence, int originalReduction) {
            if (effectiveIntelligence < 1 || effectiveIntelligence > 254
                    || originalReduction < 0 || originalReduction > 2) {
                throw new IllegalArgumentException("invalid mana-drain MP inputs");
            }
            int result = ResourceDatabase.value(this.mpCost, this.skillId, "ManaDrainSpell", "mpCost");
            if (effectiveIntelligence > 12 && skillId > 8 && skillId <= 80) result--;
            if (effectiveIntelligence > 13 && skillId > 16 && skillId <= 80) result--;
            if (effectiveIntelligence > 14 && skillId > 24 && skillId <= 80) result--;
            if (effectiveIntelligence > 15 && skillId > 32 && skillId <= 80) result--;
            result = Math.max(result, 1);
            return Math.max(0, result - originalReduction);
        }
    }

    record BlindSpell(int skillId, int row, int column, int actionId,
                      int effectGfx, int mpCost, int reuseDelayMillis,
                      int maximumRange, int durationSeconds, int failureMessage,
                      int probabilityHigh, int probabilityEqual, int probabilityLow,
                      double intelligenceMultiplier, double magicResistanceMultiplier,
                      boolean includeMagicLevel, boolean includeMagicHit,
                      int bossModifier, long itemConsumeId, int itemConsumeCount,
                      String key) {
        public long itemConsumeId() {
            return CombatDatabase.longValue(this.itemConsumeId, this.skillId, "itemConsumeId");
        }

        public int actionId() {
            return ResourceDatabase.value(this.actionId, this.skillId, "BlindSpell", "actionId");
        }

        public int durationSeconds() {
            return ResourceDatabase.value(this.durationSeconds, this.skillId, "BlindSpell", "durationSeconds");
        }

        public int effectGfx() {
            return ResourceDatabase.value(this.effectGfx, this.skillId, "BlindSpell", "effectGfx");
        }

        public int itemConsumeCount() {
            return ResourceDatabase.value(this.itemConsumeCount, this.skillId, "BlindSpell", "itemConsumeCount");
        }

        public int mpCost() {
            return ResourceDatabase.value(this.mpCost, this.skillId, "BlindSpell", "mpCost");
        }

        public int reuseDelayMillis() {
            return ResourceDatabase.value(this.reuseDelayMillis, this.skillId, "BlindSpell", "reuseDelayMillis");
        }

        BlindSpell {
            if (skillId != (row << 3) + column + 1 || row < 0 || row > 0xFF
                    || column < 0 || column > 7 || actionId < 0 || actionId > 0xFF
                    || effectGfx <= 0 || effectGfx > 0xFFFF || mpCost <= 0
                    || reuseDelayMillis < 0 || maximumRange < 0
                    || durationSeconds <= 0 || failureMessage <= 0
                    || failureMessage > 0xFFFF
                    || probabilityHigh < 0 || probabilityHigh > 100
                    || probabilityEqual < 0 || probabilityEqual > 100
                    || probabilityLow < 0 || probabilityLow > 100
                    || !Double.isFinite(intelligenceMultiplier)
                    || intelligenceMultiplier < 0.0D
                    || !Double.isFinite(magicResistanceMultiplier)
                    || magicResistanceMultiplier < 0.0D
                    || bossModifier < 0 || itemConsumeId < 0
                    || itemConsumeId > 0xFFFF_FFFFL || itemConsumeCount < 0
                    || (itemConsumeId == 0) != (itemConsumeCount == 0)
                    || key == null || key.isBlank()) {
                throw new IllegalArgumentException("invalid native blind-spell metadata");
            }
        }

        /** Exact selected skills_負面技能控制 row followed by the outer 90% cap. */
        int probability(int casterLevel, int targetLevel, int effectiveIntelligence,
                        int magicLevel, int magicHit,
                        int targetMagicResistance, int targetBlindResistance) {
            if (casterLevel <= 0 || targetLevel <= 0 || effectiveIntelligence < 0
                    || magicLevel < 0 || targetMagicResistance < 0
                    || targetBlindResistance < -128 || targetBlindResistance > 127) {
                throw new IllegalArgumentException("invalid blind probability inputs");
            }
            double result = casterLevel > targetLevel ? probabilityHigh
                    : casterLevel == targetLevel ? probabilityEqual : probabilityLow;
            result += effectiveIntelligence * intelligenceMultiplier;
            if (includeMagicHit) result += magicHit;
            if (includeMagicLevel) result += magicLevel;
            result -= targetMagicResistance * magicResistanceMultiplier;
            result -= targetBlindResistance;
            result = Math.max(1.0D, Math.min(100.0D, result));
            return Math.max(1, Math.min(90, (int) result));
        }

        int probabilityForTarget(int casterLevel, int targetLevel,
                                 int effectiveIntelligence, int magicLevel,
                                 int magicHit, int targetMagicResistance,
                                 int targetBlindResistance, boolean targetBoss) {
            int result = probability(casterLevel, targetLevel, effectiveIntelligence,
                    magicLevel, magicHit, targetMagicResistance,
                    targetBlindResistance);
            if (!targetBoss) return result;
            if (bossModifier == 0) return 0;
            return Math.max(1, result / bossModifier);
        }

        /** Exact retained circle reductions, positive-MP floor, then original-INT reduction. */
        int effectiveMpCost(int effectiveIntelligence, int originalReduction) {
            if (effectiveIntelligence < 1 || effectiveIntelligence > 254
                    || originalReduction < 0 || originalReduction > 2) {
                throw new IllegalArgumentException("invalid blind MP inputs");
            }
            int result = ResourceDatabase.value(this.mpCost, this.skillId, "BlindSpell", "mpCost");
            if (effectiveIntelligence > 12 && skillId > 8 && skillId <= 80) result--;
            if (effectiveIntelligence > 13 && skillId > 16 && skillId <= 80) result--;
            if (effectiveIntelligence > 14 && skillId > 24 && skillId <= 80) result--;
            if (effectiveIntelligence > 15 && skillId > 32 && skillId <= 80) result--;
            result = Math.max(result, 1);
            return Math.max(0, result - originalReduction);
        }
    }

    record WeaponBreakSpell(int skillId, int row, int column, int actionId,
                            int effectGfx, int mpCost, int reuseDelayMillis,
                            int maximumRange, int failureMessage,
                            int probabilityHigh, int probabilityEqual,
                            int probabilityLow, double intelligenceMultiplier,
                            double magicResistanceMultiplier,
                            boolean includeMagicLevel, boolean includeMagicHit,
                            int bossModifier, long itemConsumeId,
                            int itemConsumeCount, String key) {
        public long itemConsumeId() {
            return CombatDatabase.longValue(this.itemConsumeId, this.skillId, "itemConsumeId");
        }

        public int actionId() {
            return ResourceDatabase.value(this.actionId, this.skillId, "WeaponBreakSpell", "actionId");
        }

        public int effectGfx() {
            return ResourceDatabase.value(this.effectGfx, this.skillId, "WeaponBreakSpell", "effectGfx");
        }

        public int itemConsumeCount() {
            return ResourceDatabase.value(this.itemConsumeCount, this.skillId, "WeaponBreakSpell", "itemConsumeCount");
        }

        public int mpCost() {
            return ResourceDatabase.value(this.mpCost, this.skillId, "WeaponBreakSpell", "mpCost");
        }

        public int reuseDelayMillis() {
            return ResourceDatabase.value(this.reuseDelayMillis, this.skillId, "WeaponBreakSpell", "reuseDelayMillis");
        }

        WeaponBreakSpell {
            if (skillId != WEAPON_BREAK || skillId != (row << 3) + column + 1
                    || row != 3 || column != 2 || actionId != 19
                    || effectGfx != 172 || mpCost != 32
                    || reuseDelayMillis != 3_000 || maximumRange != 4
                    || failureMessage != 280
                    || probabilityHigh != 55 || probabilityEqual != 45
                    || probabilityLow != 35
                    || intelligenceMultiplier != 0.30D
                    || magicResistanceMultiplier != 0.05D
                    || includeMagicLevel || !includeMagicHit
                    || bossModifier != 3
                    || itemConsumeId != InventoryCatalog.MAGIC_GEM
                    || itemConsumeCount != 1
                    || key == null || key.isBlank()) {
                throw new IllegalArgumentException(
                        "invalid native Weapon Break metadata");
            }
        }

        int probabilityForTarget(int casterLevel, int targetLevel,
                                 int effectiveIntelligence, int magicLevel,
                                 int magicHit, int targetMagicResistance,
                                 boolean targetBoss) {
            if (casterLevel <= 0 || targetLevel <= 0
                    || effectiveIntelligence < 0 || magicLevel < 0
                    || targetMagicResistance < 0) {
                throw new IllegalArgumentException(
                        "invalid Weapon Break probability inputs");
            }
            double result = casterLevel > targetLevel ? probabilityHigh
                    : casterLevel == targetLevel ? probabilityEqual : probabilityLow;
            result += effectiveIntelligence * intelligenceMultiplier;
            if (includeMagicHit) result += magicHit;
            if (includeMagicLevel) result += magicLevel;
            result -= targetMagicResistance * magicResistanceMultiplier;
            result = Math.max(1.0D, Math.min(100.0D, result));
            int probability = Math.max(1, Math.min(90, (int) result));
            return targetBoss ? Math.max(1, probability / bossModifier) : probability;
        }

        int effectiveMpCost(int effectiveIntelligence, int originalReduction) {
            if (effectiveIntelligence < 1 || effectiveIntelligence > 254
                    || originalReduction < 0 || originalReduction > 2) {
                throw new IllegalArgumentException("invalid Weapon Break MP inputs");
            }
            int result = ResourceDatabase.value(this.mpCost, this.skillId, "WeaponBreakSpell", "mpCost");
            if (effectiveIntelligence > 12) result--;
            if (effectiveIntelligence > 13) result--;
            if (effectiveIntelligence > 14) result--;
            result = Math.max(result, 1);
            return Math.max(0, result - originalReduction);
        }
    }

    record WeaknessSpell(int skillId, int row, int column, int actionId,
                         int effectGfx, int mpCost, int reuseDelayMillis,
                         int maximumRange, int durationSeconds,
                         int probabilityDice, int bossModifier,
                         int meleeDamageModifier, int meleeHitModifier,
                         int armorClassModifier, int lawfulDelta,
                         int happenMessage, int failureMessage,
                         long itemConsumeId, int itemConsumeCount, String key) {
        public long itemConsumeId() {
            return CombatDatabase.longValue(this.itemConsumeId, this.skillId, "itemConsumeId");
        }

        public int actionId() {
            return ResourceDatabase.value(this.actionId, this.skillId, "WeaknessSpell", "actionId");
        }

        public int durationSeconds() {
            return ResourceDatabase.value(this.durationSeconds, this.skillId, "WeaknessSpell", "durationSeconds");
        }

        public int effectGfx() {
            return ResourceDatabase.value(this.effectGfx, this.skillId, "WeaknessSpell", "effectGfx");
        }

        public int itemConsumeCount() {
            return ResourceDatabase.value(this.itemConsumeCount, this.skillId, "WeaknessSpell", "itemConsumeCount");
        }

        public int mpCost() {
            return ResourceDatabase.value(this.mpCost, this.skillId, "WeaknessSpell", "mpCost");
        }

        public int probabilityDice() {
            return ResourceDatabase.value(this.probabilityDice, this.skillId, "WeaknessSpell", "probabilityDice");
        }

        public int reuseDelayMillis() {
            return ResourceDatabase.value(this.reuseDelayMillis, this.skillId, "WeaknessSpell", "reuseDelayMillis");
        }

        WeaknessSpell {
            if (skillId != (row << 3) + column + 1 || row < 0 || row > 0xFF
                    || column < 0 || column > 7 || actionId < 0 || actionId > 0xFF
                    || effectGfx <= 0 || effectGfx > 0xFFFF || mpCost <= 0
                    || reuseDelayMillis < 0 || maximumRange < 0
                    || durationSeconds <= 0 || durationSeconds > 0xFFFF
                    || probabilityDice <= 0 || bossModifier <= 0
                    || meleeDamageModifier >= 0 || meleeHitModifier > 0
                    || armorClassModifier < 0 || armorClassModifier > 0x7FFF
                    || lawfulDelta > 0 || lawfulDelta < -0x7FFF
                    || happenMessage < 0 || happenMessage > 0xFFFF
                    || failureMessage <= 0 || failureMessage > 0xFFFF
                    || itemConsumeId <= 0 || itemConsumeId > 0xFFFF_FFFFL
                    || itemConsumeCount <= 0 || key == null || key.isBlank()) {
                throw new IllegalArgumentException("invalid native Weakness metadata");
            }
        }

        int probability(int characterType, int casterLevel,
                        int effectiveIntelligence, int originalMagicHit,
                        int equipmentMagicHit, int targetMagicResistance,
                        int targetBossModifier, IntUnaryOperator dieRoll) {
            if (targetBossModifier != -1 && targetBossModifier != bossModifier) {
                throw new IllegalArgumentException("invalid Weakness boss modifier");
            }
            return BattleTurnUndead.probability(
                    characterType, casterLevel, effectiveIntelligence,
                    originalMagicHit, equipmentMagicHit, targetMagicResistance,
                    probabilityDice, targetBossModifier, dieRoll);
        }

        int effectiveMpCost(int effectiveIntelligence, int originalReduction) {
            if (effectiveIntelligence < 1 || effectiveIntelligence > 254
                    || originalReduction < 0 || originalReduction > 2) {
                throw new IllegalArgumentException("invalid Weakness MP inputs");
            }
            int result = ResourceDatabase.value(this.mpCost, this.skillId, "WeaknessSpell", "mpCost");
            if (effectiveIntelligence > 12 && skillId > 8 && skillId <= 80) result--;
            if (effectiveIntelligence > 13 && skillId > 16 && skillId <= 80) result--;
            if (effectiveIntelligence > 14 && skillId > 24 && skillId <= 80) result--;
            if (effectiveIntelligence > 15 && skillId > 32 && skillId <= 80) result--;
            if (effectiveIntelligence > 16 && skillId > 40 && skillId <= 80) result--;
            result = Math.max(result, 1);
            return Math.max(0, result - originalReduction);
        }
    }

    record MassSlowSpell(int skillId, int row, int column, int actionId,
                         int effectGfx, int mpCost, int reuseDelayMillis,
                         int maximumRange, int areaRadius, int durationSeconds,
                         int probabilityDice, int bossModifier,
                         int failureMessage, long itemConsumeId,
                         int itemConsumeCount, String key) {
        public long itemConsumeId() {
            return CombatDatabase.longValue(this.itemConsumeId, this.skillId, "itemConsumeId");
        }

        public int nativeSourceVisibilityOriginalRange() {
            return this.maximumRange;
        }

        public int maximumRange() {
            return NativeSourceVisibilityRange.value(this.skillId(), this.nativeSourceVisibilityOriginalRange());
        }

        public int actionId() {
            return ResourceDatabase.value(this.actionId, this.skillId, "MassSlowSpell", "actionId");
        }

        public int durationSeconds() {
            return ResourceDatabase.value(this.durationSeconds, this.skillId, "MassSlowSpell", "durationSeconds");
        }

        public int effectGfx() {
            return ResourceDatabase.value(this.effectGfx, this.skillId, "MassSlowSpell", "effectGfx");
        }

        public int itemConsumeCount() {
            return ResourceDatabase.value(this.itemConsumeCount, this.skillId, "MassSlowSpell", "itemConsumeCount");
        }

        public int mpCost() {
            return ResourceDatabase.value(this.mpCost, this.skillId, "MassSlowSpell", "mpCost");
        }

        public int probabilityDice() {
            return ResourceDatabase.value(this.probabilityDice, this.skillId, "MassSlowSpell", "probabilityDice");
        }

        public int reuseDelayMillis() {
            return ResourceDatabase.value(this.reuseDelayMillis, this.skillId, "MassSlowSpell", "reuseDelayMillis");
        }

        MassSlowSpell {
            if (skillId != MASS_SLOW || skillId != (row << 3) + column + 1
                    || row != 9 || column != 3 || actionId != 19
                    || effectGfx <= 0 || effectGfx > 0xFFFF || mpCost <= 0
                    || reuseDelayMillis < 0 || maximumRange != 10
                    || areaRadius != 3 || durationSeconds <= 0
                    || durationSeconds > 0xFFFF || probabilityDice <= 0
                    || bossModifier <= 0
                    || failureMessage <= 0 || failureMessage > 0xFFFF
                    || itemConsumeId != InventoryCatalog.MAGIC_GEM
                    || itemConsumeCount != 2 || key == null || key.isBlank()) {
                throw new IllegalArgumentException("invalid native Mass Slow metadata");
            }
        }

        int probability(int characterType, int casterLevel,
                        int effectiveIntelligence, int originalMagicHit,
                        int equipmentMagicHit, int targetMagicResistance,
                        boolean targetBoss, IntUnaryOperator dieRoll) {
            return BattleTurnUndead.probability(
                    characterType, casterLevel, effectiveIntelligence,
                    originalMagicHit, equipmentMagicHit, targetMagicResistance,
                    probabilityDice, targetBoss ? bossModifier : -1, dieRoll);
        }

        int effectiveMpCost(int effectiveIntelligence, int originalReduction) {
            if (effectiveIntelligence < 1 || effectiveIntelligence > 254
                    || originalReduction < 0 || originalReduction > 2) {
                throw new IllegalArgumentException("invalid Mass Slow MP inputs");
            }
            int result = ResourceDatabase.value(this.mpCost, this.skillId, "MassSlowSpell", "mpCost");
            if (effectiveIntelligence > 12 && skillId > 8 && skillId <= 80) result--;
            if (effectiveIntelligence > 13 && skillId > 16 && skillId <= 80) result--;
            if (effectiveIntelligence > 14 && skillId > 24 && skillId <= 80) result--;
            if (effectiveIntelligence > 15 && skillId > 32 && skillId <= 80) result--;
            if (effectiveIntelligence > 16 && skillId > 40 && skillId <= 80) result--;
            if (effectiveIntelligence > 17 && skillId > 48 && skillId <= 80) result--;
            if (effectiveIntelligence > 18 && skillId > 56 && skillId <= 80) result--;
            result = Math.max(result, 1);
            return Math.max(0, result - originalReduction);
        }
    }

    record EntangleSpell(
            int skillId, int row, int column, int level, int number,
            int actionId, int effectGfx, int mpCost, int reuseDelayMillis,
            int durationSeconds, int targetTo, int maximumRange,
            int areaRadius, int through, int attribute, int type, int nativeId,
            int happenMessage, int stopMessage, int failureMessage,
            int probabilityHigh, int probabilityEqual, int probabilityLow,
            double intelligenceMultiplier, double magicResistanceMultiplier,
            boolean includeMagicLevel, boolean includeMagicHit,
            int bossModifier, boolean counterMagicBypass,
            long itemConsumeId, int itemConsumeCount, String key) {
        public long itemConsumeId() {
            return CombatDatabase.longValue(this.itemConsumeId, this.skillId, "itemConsumeId");
        }

        public int nativeSourceVisibilityOriginalRange() {
            return this.maximumRange;
        }

        public int maximumRange() {
            return NativeSourceVisibilityRange.value(this.skillId(), this.nativeSourceVisibilityOriginalRange());
        }

        public int actionId() {
            return ResourceDatabase.value(this.actionId, this.skillId, "EntangleSpell", "actionId");
        }

        public int durationSeconds() {
            return ResourceDatabase.value(this.durationSeconds, this.skillId, "EntangleSpell", "durationSeconds");
        }

        public int effectGfx() {
            return ResourceDatabase.value(this.effectGfx, this.skillId, "EntangleSpell", "effectGfx");
        }

        public int itemConsumeCount() {
            return ResourceDatabase.value(this.itemConsumeCount, this.skillId, "EntangleSpell", "itemConsumeCount");
        }

        public int mpCost() {
            return ResourceDatabase.value(this.mpCost, this.skillId, "EntangleSpell", "mpCost");
        }

        public int reuseDelayMillis() {
            return ResourceDatabase.value(this.reuseDelayMillis, this.skillId, "EntangleSpell", "reuseDelayMillis");
        }

        EntangleSpell {
            boolean nativeSlow = skillId == SLOW
                    && row == 3 && column == 4 && level == 4 && number == 4
                    && actionId == 19 && effectGfx == 752 && mpCost == 20
                    && reuseDelayMillis == 500 && durationSeconds == 64
                    && targetTo == 3 && maximumRange == 8 && areaRadius == 0
                    && through == 0 && attribute == ATTRIBUTE_UNSPECIFIED
                    && type == 1 && nativeId == 16 && happenMessage == 0
                    && stopMessage == 277 && failureMessage == 280
                    && bossModifier == 5
                    && itemConsumeId == InventoryCatalog.MAGIC_GEM;
            boolean nativeEntangle = skillId == ENTANGLE
                    && row == 18 && column == 7 && level == 19 && number == 7
                    && actionId == 19 && effectGfx == 2_250 && mpCost == 20
                    && reuseDelayMillis == 0 && durationSeconds == 64
                    && targetTo == 3 && maximumRange == 10 && areaRadius == 0
                    && through == 0 && attribute == ATTRIBUTE_UNSPECIFIED
                    && type == 1 && nativeId == 128 && happenMessage == 0
                    && stopMessage == 711 && failureMessage == 280
                    && bossModifier == 0
                    && itemConsumeId == InventoryCatalog.SPIRIT_CRYSTAL;
            if (skillId != (row << 3) + column + 1
                    || !nativeSlow && !nativeEntangle
                    || probabilityHigh != 55 || probabilityEqual != 45
                    || probabilityLow != 35
                    || intelligenceMultiplier != 0.30D
                    || magicResistanceMultiplier != 0.05D
                    || includeMagicLevel || !includeMagicHit
                    || counterMagicBypass || itemConsumeCount != 1
                    || key == null || key.isBlank()) {
                throw new IllegalArgumentException(
                        "invalid native Slow/Entangle metadata");
            }
        }

        int probability(int casterLevel, int targetLevel,
                        int effectiveIntelligence, int magicLevel,
                        int magicHit, int targetMagicResistance) {
            if (casterLevel <= 0 || targetLevel <= 0 || effectiveIntelligence < 0
                    || magicLevel < 0 || magicHit < 0
                    || targetMagicResistance < 0) {
                throw new IllegalArgumentException(
                        "invalid Entangle probability inputs");
            }
            double result = casterLevel > targetLevel ? probabilityHigh
                    : casterLevel == targetLevel ? probabilityEqual : probabilityLow;
            result += effectiveIntelligence * intelligenceMultiplier;
            if (includeMagicHit) result += magicHit;
            if (includeMagicLevel) result += magicLevel;
            result -= targetMagicResistance * magicResistanceMultiplier;
            result = Math.max(1.0D, Math.min(100.0D, result));
            return Math.max(1, Math.min(90, (int) result));
        }

        int probabilityForTarget(
                int casterLevel, int targetLevel, int effectiveIntelligence,
                int magicLevel, int magicHit, int targetMagicResistance,
                boolean targetBoss) {
            int result = probability(casterLevel, targetLevel,
                    effectiveIntelligence, magicLevel, magicHit,
                    targetMagicResistance);
            if (!targetBoss) return result;
            if (bossModifier == 0) return 0;
            return Math.max(1, result / bossModifier);
        }

        int effectiveMpCost(int effectiveIntelligence, int originalReduction) {
            if (effectiveIntelligence < 1 || effectiveIntelligence > 254
                    || originalReduction < 0 || originalReduction > 2) {
                throw new IllegalArgumentException("invalid Slow/Entangle MP inputs");
            }
            int result = ResourceDatabase.value(this.mpCost, this.skillId, "EntangleSpell", "mpCost");
            if (effectiveIntelligence > 12 && skillId > 8 && skillId <= 80) result--;
            if (effectiveIntelligence > 13 && skillId > 16 && skillId <= 80) result--;
            if (effectiveIntelligence > 14 && skillId > 24 && skillId <= 80) result--;
            if (effectiveIntelligence > 15 && skillId > 32 && skillId <= 80) result--;
            if (effectiveIntelligence > 16 && skillId > 40 && skillId <= 80) result--;
            if (effectiveIntelligence > 17 && skillId > 48 && skillId <= 80) result--;
            if (effectiveIntelligence > 18 && skillId > 56 && skillId <= 80) result--;
            result = Math.max(result, 1);
            return Math.max(0, result - originalReduction);
        }

        /** Skill152 is outside the retained circle-INT reduction range. */
        int effectiveMpCost(int originalReduction) {
            if (originalReduction < 0 || originalReduction > 2) {
                throw new IllegalArgumentException("invalid Entangle MP inputs");
            }
            return effectiveMpCost(1, originalReduction);
        }
    }

    record EraseMagicSpell(
            int skillId, int row, int column, int level, int number,
            int actionId, int effectGfx, int mpCost, int reuseDelayMillis,
            int durationSeconds, int targetTo, int maximumRange,
            int areaRadius, int through, int attribute, int type, int nativeId,
            int happenMessage, int stopMessage, int failureMessage,
            int probabilityHigh, int probabilityEqual, int probabilityLow,
            double intelligenceMultiplier, double magicResistanceMultiplier,
            boolean includeMagicLevel, boolean includeMagicHit,
            int bossModifier, boolean counterMagicBypass,
            long itemConsumeId, int itemConsumeCount, String key) {
        public long itemConsumeId() {
            return CombatDatabase.longValue(this.itemConsumeId, this.skillId, "itemConsumeId");
        }

        int nativeSourceEraseOriginalSelectionRange() {
            return 10;
        }

        public int actionId() {
            return ResourceDatabase.value(this.actionId, this.skillId, "EraseMagicSpell", "actionId");
        }

        public int durationSeconds() {
            return ResourceDatabase.value(this.durationSeconds, this.skillId, "EraseMagicSpell", "durationSeconds");
        }

        public int effectGfx() {
            return ResourceDatabase.value(this.effectGfx, this.skillId, "EraseMagicSpell", "effectGfx");
        }

        public int itemConsumeCount() {
            return ResourceDatabase.value(this.itemConsumeCount, this.skillId, "EraseMagicSpell", "itemConsumeCount");
        }

        public int mpCost() {
            return ResourceDatabase.value(this.mpCost, this.skillId, "EraseMagicSpell", "mpCost");
        }

        public int reuseDelayMillis() {
            return ResourceDatabase.value(this.reuseDelayMillis, this.skillId, "EraseMagicSpell", "reuseDelayMillis");
        }

        EraseMagicSpell {
            if (skillId != ERASE_MAGIC || skillId != (row << 3) + column + 1
                    || row != 19 || column != 0 || level != 20 || number != 0
                    || actionId != 19 || effectGfx != 10_706 || mpCost != 10
                    || reuseDelayMillis != 600 || durationSeconds != 32
                    || targetTo != 3 || maximumRange != -1 || areaRadius != 0
                    || through != 0 || attribute != ATTRIBUTE_UNSPECIFIED
                    || type != 1 || nativeId != 1 || happenMessage != 0
                    || stopMessage != 0 || failureMessage != 280
                    || probabilityHigh != 55 || probabilityEqual != 45
                    || probabilityLow != 35
                    || intelligenceMultiplier != 0.35D
                    || magicResistanceMultiplier != 0.05D
                    || includeMagicLevel || !includeMagicHit
                    || bossModifier != 0 || !counterMagicBypass
                    || itemConsumeId != InventoryCatalog.SPIRIT_CRYSTAL
                    || itemConsumeCount != 1 || key == null || key.isBlank()) {
                throw new IllegalArgumentException(
                        "invalid native Erase Magic metadata");
            }
        }

        int probability(int casterLevel, int targetLevel,
                        int effectiveIntelligence, int magicLevel,
                        int magicHit, int targetMagicResistance) {
            if (casterLevel <= 0 || targetLevel <= 0 || effectiveIntelligence < 0
                    || magicLevel < 0 || magicHit < 0
                    || targetMagicResistance < 0) {
                throw new IllegalArgumentException(
                        "invalid Erase Magic probability inputs");
            }
            double result = casterLevel > targetLevel ? probabilityHigh
                    : casterLevel == targetLevel ? probabilityEqual : probabilityLow;
            result += effectiveIntelligence * intelligenceMultiplier;
            if (includeMagicHit) result += magicHit;
            if (includeMagicLevel) result += magicLevel;
            result -= targetMagicResistance * magicResistanceMultiplier;
            result = Math.max(1.0D, Math.min(100.0D, result));
            return Math.max(1, Math.min(90, (int) result));
        }

        int probabilityForTarget(
                int casterLevel, int targetLevel, int effectiveIntelligence,
                int magicLevel, int magicHit, int targetMagicResistance,
                boolean targetBoss) {
            int result = probability(casterLevel, targetLevel,
                    effectiveIntelligence, magicLevel, magicHit,
                    targetMagicResistance);
            if (!targetBoss) return result;
            if (bossModifier == 0) return 0;
            return Math.max(1, result / bossModifier);
        }

        /** Skill153 is outside the retained circle-INT reduction range. */
        int effectiveMpCost(int originalReduction) {
            if (originalReduction < 0 || originalReduction > 2) {
                throw new IllegalArgumentException(
                        "invalid Erase Magic MP inputs");
            }
            return Math.max(0, ResourceDatabase.value(this.mpCost, this.skillId, "EraseMagicSpell", "mpCost") - originalReduction);
        }

        /** LiTo.bin sub_4CA6E0 rejects the case152 target beyond ten cells. */
        int selectionRange() {
            return NativeSourceVisibilityRange.value(this.skillId(), this.nativeSourceEraseOriginalSelectionRange());
        }
    }

    record ElementalSummonSpell(
            int skillId, int row, int column, int level, int number,
            int actionId, int effectGfx, int mpCost, int reuseDelayMillis,
            int targetTo, int maximumRange, int areaRadius, int through,
            int attribute, int type, int nativeId,
            long itemConsumeId, int itemConsumeCount,
            int mapFailureMessage, int spawnEffectGfx, int lifetimeSeconds,
            boolean counterMagicBypass, String key) {
        public long itemConsumeId() {
            return CombatDatabase.longValue(this.itemConsumeId, this.skillId, "itemConsumeId");
        }

        public int actionId() {
            return ResourceDatabase.value(this.actionId, this.skillId, "ElementalSummonSpell", "actionId");
        }

        public int effectGfx() {
            return ResourceDatabase.value(this.effectGfx, this.skillId, "ElementalSummonSpell", "effectGfx");
        }

        int effectiveMpCost() {
            return ResourceDatabase.value(this.mpCost, this.skillId, "ElementalSummonSpell", "mpCost");
        }

        public int itemConsumeCount() {
            return ResourceDatabase.value(this.itemConsumeCount, this.skillId, "ElementalSummonSpell", "itemConsumeCount");
        }

        public int mpCost() {
            return ResourceDatabase.value(this.mpCost, this.skillId, "ElementalSummonSpell", "mpCost");
        }

        public int reuseDelayMillis() {
            return ResourceDatabase.value(this.reuseDelayMillis, this.skillId, "ElementalSummonSpell", "reuseDelayMillis");
        }

        public int spawnEffectGfx() {
            return ResourceDatabase.value(this.spawnEffectGfx, this.skillId, "ElementalSummonSpell", "spawnEffectGfx");
        }

        ElementalSummonSpell {
            boolean lesser = skillId == LESSER_ELEMENTAL
                    && row == 19 && column == 1 && level == 20
                    && itemConsumeCount == 2 && "lesser-elemental".equals(key);
            boolean greater = skillId == GREATER_ELEMENTAL
                    && row == 20 && column == 1 && level == 21
                    && itemConsumeCount == 4 && "greater-elemental".equals(key);
            if ((!lesser && !greater)
                    || skillId != (row << 3) + column + 1 || number != 1
                    || actionId != 19 || effectGfx != 2510 || mpCost != 20
                    || reuseDelayMillis != 0 || targetTo != 0
                    || maximumRange != 0 || areaRadius != 0 || through != 0
                    || attribute != ATTRIBUTE_UNSPECIFIED
                    || type != 128 || nativeId != 2
                    || itemConsumeId != InventoryCatalog.SPIRIT_CRYSTAL
                    || mapFailureMessage != 353
                    || spawnEffectGfx != 755 || lifetimeSeconds != 1_800
                    || !counterMagicBypass || key == null || key.isBlank()) {
                throw new IllegalArgumentException(
                        "invalid native elemental summon metadata");
            }
        }

        /** Retained dual-attribute behavior: the lowest known bit wins. */
        int summonNpcId(int elfAttribute) {
            boolean greater = skillId == GREATER_ELEMENTAL;
            if ((elfAttribute & ATTRIBUTE_EARTH) != 0) return greater ? 81053 : 45306;
            if ((elfAttribute & ATTRIBUTE_FIRE) != 0) return greater ? 81050 : 45303;
            if ((elfAttribute & ATTRIBUTE_WATER) != 0) return greater ? 81051 : 45304;
            if ((elfAttribute & ATTRIBUTE_WIND) != 0) return greater ? 81052 : 45305;
            return 0;
        }

        int petCost(int charisma) {
            if (charisma < 0 || charisma > 0xFF) {
                throw new IllegalArgumentException("invalid elemental charisma");
            }
            return Math.addExact(charisma, 7);
        }
    }

    record SilenceSpell(int skillId, int row, int column, int actionId,
                        int effectGfx, int mpCost, int reuseDelayMillis,
                        int maximumRange, int durationSeconds, int failureMessage,
                        int probabilityHigh, int probabilityEqual, int probabilityLow,
                        double intelligenceMultiplier,
                        double magicResistanceMultiplier,
                        boolean includeMagicLevel, boolean includeMagicHit,
                        int bossModifier, long itemConsumeId, int itemConsumeCount,
                        String key) {
        public long itemConsumeId() {
            return CombatDatabase.longValue(this.itemConsumeId, this.skillId, "itemConsumeId");
        }

        public int actionId() {
            return ResourceDatabase.value(this.actionId, this.skillId, "SilenceSpell", "actionId");
        }

        public int durationSeconds() {
            return ResourceDatabase.value(this.durationSeconds, this.skillId, "SilenceSpell", "durationSeconds");
        }

        public int effectGfx() {
            return ResourceDatabase.value(this.effectGfx, this.skillId, "SilenceSpell", "effectGfx");
        }

        public int itemConsumeCount() {
            return ResourceDatabase.value(this.itemConsumeCount, this.skillId, "SilenceSpell", "itemConsumeCount");
        }

        public int mpCost() {
            return ResourceDatabase.value(this.mpCost, this.skillId, "SilenceSpell", "mpCost");
        }

        public int reuseDelayMillis() {
            return ResourceDatabase.value(this.reuseDelayMillis, this.skillId, "SilenceSpell", "reuseDelayMillis");
        }

        SilenceSpell {
            if (skillId != (row << 3) + column + 1 || row < 0 || row > 0xFF
                    || column < 0 || column > 7 || actionId < 0 || actionId > 0xFF
                    || effectGfx <= 0 || effectGfx > 0xFFFF || mpCost <= 0
                    || reuseDelayMillis < 0 || maximumRange < 0
                    || durationSeconds <= 0 || durationSeconds > 0xFFFF
                    || failureMessage <= 0 || failureMessage > 0xFFFF
                    || probabilityHigh < 0 || probabilityHigh > 100
                    || probabilityEqual < 0 || probabilityEqual > 100
                    || probabilityLow < 0 || probabilityLow > 100
                    || !Double.isFinite(intelligenceMultiplier)
                    || intelligenceMultiplier < 0.0D
                    || !Double.isFinite(magicResistanceMultiplier)
                    || magicResistanceMultiplier < 0.0D
                    || bossModifier < 0 || itemConsumeId <= 0
                    || itemConsumeId > 0xFFFF_FFFFL || itemConsumeCount <= 0
                    || key == null || key.isBlank()) {
                throw new IllegalArgumentException("invalid native Silence metadata");
            }
        }

        int probability(int casterLevel, int targetLevel,
                        int effectiveIntelligence, int magicLevel,
                        int magicHit, int targetMagicResistance) {
            if (casterLevel <= 0 || targetLevel <= 0 || effectiveIntelligence < 0
                    || magicLevel < 0 || targetMagicResistance < 0) {
                throw new IllegalArgumentException("invalid Silence probability inputs");
            }
            double result = casterLevel > targetLevel ? probabilityHigh
                    : casterLevel == targetLevel ? probabilityEqual : probabilityLow;
            result += effectiveIntelligence * intelligenceMultiplier;
            if (includeMagicHit) result += magicHit;
            if (includeMagicLevel) result += magicLevel;
            result -= targetMagicResistance * magicResistanceMultiplier;
            result = Math.max(1.0D, Math.min(100.0D, result));
            return Math.max(1, Math.min(90, (int) result));
        }

        int probabilityForTarget(int casterLevel, int targetLevel,
                                 int effectiveIntelligence, int magicLevel,
                                 int magicHit, int targetMagicResistance,
                                 boolean targetBoss) {
            int result = probability(casterLevel, targetLevel, effectiveIntelligence,
                    magicLevel, magicHit, targetMagicResistance);
            if (!targetBoss) return result;
            if (bossModifier == 0) return 0;
            return Math.max(1, result / bossModifier);
        }

        int effectiveMpCost(int effectiveIntelligence, int originalReduction) {
            if (effectiveIntelligence < 1 || effectiveIntelligence > 254
                    || originalReduction < 0 || originalReduction > 2) {
                throw new IllegalArgumentException("invalid Silence MP inputs");
            }
            int result = ResourceDatabase.value(this.mpCost, this.skillId, "SilenceSpell", "mpCost");
            if (effectiveIntelligence > 12 && skillId > 8 && skillId <= 80) result--;
            if (effectiveIntelligence > 13 && skillId > 16 && skillId <= 80) result--;
            if (effectiveIntelligence > 14 && skillId > 24 && skillId <= 80) result--;
            if (effectiveIntelligence > 15 && skillId > 32 && skillId <= 80) result--;
            if (effectiveIntelligence > 16 && skillId > 40 && skillId <= 80) result--;
            if (effectiveIntelligence > 17 && skillId > 48 && skillId <= 80) result--;
            if (effectiveIntelligence > 18 && skillId > 56 && skillId <= 80) result--;
            result = Math.max(result, 1);
            return Math.max(0, result - originalReduction);
        }
    }

    record DecayPotionSpell(int skillId, int row, int column, int actionId,
                            int effectGfx, int mpCost, int reuseDelayMillis,
                            int clientCastClockMillis, int maximumRange,
                            int durationSeconds,
                            int probabilityDice, int bossModifier,
                            int failureMessage, long itemConsumeId,
                            int itemConsumeCount, String key) {
        public long itemConsumeId() {
            return CombatDatabase.longValue(this.itemConsumeId, this.skillId, "itemConsumeId");
        }

        public int actionId() {
            return ResourceDatabase.value(this.actionId, this.skillId, "DecayPotionSpell", "actionId");
        }

        public int durationSeconds() {
            return ResourceDatabase.value(this.durationSeconds, this.skillId, "DecayPotionSpell", "durationSeconds");
        }

        public int effectGfx() {
            return ResourceDatabase.value(this.effectGfx, this.skillId, "DecayPotionSpell", "effectGfx");
        }

        public int itemConsumeCount() {
            return ResourceDatabase.value(this.itemConsumeCount, this.skillId, "DecayPotionSpell", "itemConsumeCount");
        }

        public int mpCost() {
            return ResourceDatabase.value(this.mpCost, this.skillId, "DecayPotionSpell", "mpCost");
        }

        public int probabilityDice() {
            return ResourceDatabase.value(this.probabilityDice, this.skillId, "DecayPotionSpell", "probabilityDice");
        }

        public int reuseDelayMillis() {
            return ResourceDatabase.value(this.reuseDelayMillis, this.skillId, "DecayPotionSpell", "reuseDelayMillis");
        }

        DecayPotionSpell {
            if (skillId != (row << 3) + column + 1 || row < 0 || row > 0xFF
                    || column < 0 || column > 7 || actionId < 0 || actionId > 0xFF
                    || effectGfx <= 0 || effectGfx > 0xFFFF || mpCost <= 0
                    || reuseDelayMillis < 0 || clientCastClockMillis < 0
                    || clientCastClockMillis > 0xFFFF || maximumRange < 0
                    || durationSeconds <= 0 || durationSeconds > 0xFFFF
                    || probabilityDice <= 0 || bossModifier < 0
                    || failureMessage <= 0 || failureMessage > 0xFFFF
                    || itemConsumeId <= 0 || itemConsumeId > 0xFFFF_FFFFL
                    || itemConsumeCount <= 0 || key == null || key.isBlank()) {
                throw new IllegalArgumentException(
                        "invalid native Decay Potion metadata");
            }
        }

        int probability(int characterType, int casterLevel,
                        int effectiveIntelligence, int originalMagicHit,
                        int equipmentMagicHit, int targetMagicResistance,
                        boolean targetBoss, IntUnaryOperator dieRoll) {
            return BattleTurnUndead.probability(
                    characterType, casterLevel, effectiveIntelligence,
                    originalMagicHit, equipmentMagicHit, targetMagicResistance,
                    probabilityDice, targetBoss ? bossModifier : -1, dieRoll);
        }

        int effectiveMpCost(int effectiveIntelligence, int originalReduction) {
            if (effectiveIntelligence < 1 || effectiveIntelligence > 254
                    || originalReduction < 0 || originalReduction > 2) {
                throw new IllegalArgumentException("invalid Decay Potion MP inputs");
            }
            int result = ResourceDatabase.value(this.mpCost, this.skillId, "DecayPotionSpell", "mpCost");
            if (effectiveIntelligence > 12 && skillId > 8 && skillId <= 80) result--;
            if (effectiveIntelligence > 13 && skillId > 16 && skillId <= 80) result--;
            if (effectiveIntelligence > 14 && skillId > 24 && skillId <= 80) result--;
            if (effectiveIntelligence > 15 && skillId > 32 && skillId <= 80) result--;
            if (effectiveIntelligence > 16 && skillId > 40 && skillId <= 80) result--;
            if (effectiveIntelligence > 17 && skillId > 48 && skillId <= 80) result--;
            if (effectiveIntelligence > 18 && skillId > 56 && skillId <= 80) result--;
            result = Math.max(result, 1);
            return Math.max(0, result - originalReduction);
        }
    }

    record PolluteWaterSpell(
            int skillId, int row, int column, int actionId, int effectGfx,
            int mpCost, int reuseDelayMillis, int secondaryReuseDelayMillis,
            int clientCastClockMillis, int maximumRange,
            int durationSeconds, int attribute, int failureMessage,
            int probabilityHigh, int probabilityEqual, int probabilityLow,
            double intelligenceMultiplier, double magicResistanceMultiplier,
            boolean includeMagicLevel, boolean includeMagicHit,
            int bossModifier, boolean counterMagicBypass,
            long itemConsumeId, int itemConsumeCount, String key)
            implements TargetNegativeStatusSpell {
        public long itemConsumeId() {
            return CombatDatabase.longValue(this.itemConsumeId, this.skillId, "itemConsumeId");
        }

        public int actionId() {
            return ResourceDatabase.value(this.actionId, this.skillId, "PolluteWaterSpell", "actionId");
        }

        public int durationSeconds() {
            return ResourceDatabase.value(this.durationSeconds, this.skillId, "PolluteWaterSpell", "durationSeconds");
        }

        public int effectGfx() {
            return ResourceDatabase.value(this.effectGfx, this.skillId, "PolluteWaterSpell", "effectGfx");
        }

        public int itemConsumeCount() {
            return ResourceDatabase.value(this.itemConsumeCount, this.skillId, "PolluteWaterSpell", "itemConsumeCount");
        }

        public int mpCost() {
            return ResourceDatabase.value(this.mpCost, this.skillId, "PolluteWaterSpell", "mpCost");
        }

        public int reuseDelayMillis() {
            return ResourceDatabase.value(this.reuseDelayMillis, this.skillId, "PolluteWaterSpell", "reuseDelayMillis");
        }

        PolluteWaterSpell {
            if (skillId != (row << 3) + column + 1 || skillId != POLLUTE_WATER
                    || row < 0 || row > 0xFF || column < 0 || column > 7
                    || actionId < 0 || actionId > 0xFF
                    || effectGfx <= 0 || effectGfx > 0xFFFF || mpCost <= 0
                    || reuseDelayMillis < 0 || secondaryReuseDelayMillis < 0
                    || clientCastClockMillis < 0 || maximumRange < 0
                    || durationSeconds <= 0 || durationSeconds > 0xFFFF
                    || attribute != ATTRIBUTE_WATER
                    || failureMessage <= 0 || failureMessage > 0xFFFF
                    || probabilityHigh < 0 || probabilityHigh > 100
                    || probabilityEqual < 0 || probabilityEqual > 100
                    || probabilityLow < 0 || probabilityLow > 100
                    || !Double.isFinite(intelligenceMultiplier)
                    || intelligenceMultiplier < 0.0D
                    || !Double.isFinite(magicResistanceMultiplier)
                    || magicResistanceMultiplier < 0.0D
                    || bossModifier < 0 || !counterMagicBypass
                    || itemConsumeId <= 0 || itemConsumeId > 0xFFFF_FFFFL
                    || itemConsumeCount <= 0 || key == null || key.isBlank()) {
                throw new IllegalArgumentException(
                        "invalid native Pollute Water metadata");
            }
        }

        int probability(int casterLevel, int targetLevel,
                        int effectiveIntelligence, int magicLevel,
                        int magicHit, int targetMagicResistance) {
            if (casterLevel <= 0 || targetLevel <= 0 || effectiveIntelligence < 0
                    || magicLevel < 0 || targetMagicResistance < 0) {
                throw new IllegalArgumentException(
                        "invalid Pollute Water probability inputs");
            }
            double result = casterLevel > targetLevel ? probabilityHigh
                    : casterLevel == targetLevel ? probabilityEqual : probabilityLow;
            result += effectiveIntelligence * intelligenceMultiplier;
            if (includeMagicHit) result += magicHit;
            if (includeMagicLevel) result += magicLevel;
            result -= targetMagicResistance * magicResistanceMultiplier;
            result = Math.max(1.0D, Math.min(100.0D, result));
            return Math.max(1, Math.min(90, (int) result));
        }

        public int probabilityForTarget(
                int casterLevel, int targetLevel, int effectiveIntelligence,
                int magicLevel, int magicHit, int targetMagicResistance,
                boolean targetBoss) {
            int result = probability(casterLevel, targetLevel,
                    effectiveIntelligence, magicLevel, magicHit,
                    targetMagicResistance);
            if (!targetBoss) return result;
            if (bossModifier == 0) return 0;
            return Math.max(1, result / bossModifier);
        }

        /** Skill173 is outside the retained circle-INT reduction range. */
        public int effectiveMpCost(int originalReduction) {
            if (originalReduction < 0 || originalReduction > 2) {
                throw new IllegalArgumentException(
                        "invalid Pollute Water MP inputs");
            }
            return Math.max(0, ResourceDatabase.value(this.mpCost, this.skillId, "PolluteWaterSpell", "mpCost") - originalReduction);
        }
    }

    record StrikerGaleSpell(
            int skillId, int row, int column, int actionId, int effectGfx,
            int mpCost, int reuseDelayMillis, int secondaryReuseDelayMillis,
            int clientCastClockMillis, int clientClickRange, int serverRange,
            int durationSeconds, int attribute, int failureMessage,
            int probabilityHigh, int probabilityEqual, int probabilityLow,
            double intelligenceMultiplier, double magicResistanceMultiplier,
            boolean includeMagicLevel, boolean includeMagicHit,
            int bossModifier, boolean counterMagicBypass,
            long itemConsumeId, int itemConsumeCount, String key)
            implements TargetNegativeStatusSpell {
        public long itemConsumeId() {
            return CombatDatabase.longValue(this.itemConsumeId, this.skillId, "itemConsumeId");
        }

        public int actionId() {
            return ResourceDatabase.value(this.actionId, this.skillId, "StrikerGaleSpell", "actionId");
        }

        public int durationSeconds() {
            return ResourceDatabase.value(this.durationSeconds, this.skillId, "StrikerGaleSpell", "durationSeconds");
        }

        public int effectGfx() {
            return ResourceDatabase.value(this.effectGfx, this.skillId, "StrikerGaleSpell", "effectGfx");
        }

        public int itemConsumeCount() {
            return ResourceDatabase.value(this.itemConsumeCount, this.skillId, "StrikerGaleSpell", "itemConsumeCount");
        }

        public int mpCost() {
            return ResourceDatabase.value(this.mpCost, this.skillId, "StrikerGaleSpell", "mpCost");
        }

        public int reuseDelayMillis() {
            return ResourceDatabase.value(this.reuseDelayMillis, this.skillId, "StrikerGaleSpell", "reuseDelayMillis");
        }

        StrikerGaleSpell {
            if (skillId != (row << 3) + column + 1 || skillId != STRIKER_GALE
                    || row < 0 || row > 0xFF || column < 0 || column > 7
                    || actionId < 0 || actionId > 0xFF
                    || effectGfx <= 0 || effectGfx > 0xFFFF || mpCost <= 0
                    || reuseDelayMillis < 0 || secondaryReuseDelayMillis < 0
                    || clientCastClockMillis < 0 || clientClickRange <= 0
                    || serverRange != -1
                    || durationSeconds <= 0 || durationSeconds > 0xFFFF
                    || attribute != ATTRIBUTE_WIND
                    || failureMessage <= 0 || failureMessage > 0xFFFF
                    || probabilityHigh < 0 || probabilityHigh > 100
                    || probabilityEqual < 0 || probabilityEqual > 100
                    || probabilityLow < 0 || probabilityLow > 100
                    || !Double.isFinite(intelligenceMultiplier)
                    || intelligenceMultiplier < 0.0D
                    || !Double.isFinite(magicResistanceMultiplier)
                    || magicResistanceMultiplier < 0.0D
                    || bossModifier < 0 || !counterMagicBypass
                    || itemConsumeId <= 0 || itemConsumeId > 0xFFFF_FFFFL
                    || itemConsumeCount <= 0 || key == null || key.isBlank()) {
                throw new IllegalArgumentException(
                        "invalid native Striker Gale metadata");
            }
        }

        int probability(int casterLevel, int targetLevel,
                        int effectiveIntelligence, int magicLevel,
                        int magicHit, int targetMagicResistance) {
            if (casterLevel <= 0 || targetLevel <= 0 || effectiveIntelligence < 0
                    || magicLevel < 0 || targetMagicResistance < 0) {
                throw new IllegalArgumentException(
                        "invalid Striker Gale probability inputs");
            }
            double result = casterLevel > targetLevel ? probabilityHigh
                    : casterLevel == targetLevel ? probabilityEqual : probabilityLow;
            result += effectiveIntelligence * intelligenceMultiplier;
            if (includeMagicHit) result += magicHit;
            if (includeMagicLevel) result += magicLevel;
            result -= targetMagicResistance * magicResistanceMultiplier;
            result = Math.max(1.0D, Math.min(100.0D, result));
            return Math.max(1, Math.min(90, (int) result));
        }

        /** Retained ranged=-1 delegates authority to Point.isInScreen. */
        @Override public int maximumRange() {
            return 19;
        }

        public int probabilityForTarget(
                int casterLevel, int targetLevel, int effectiveIntelligence,
                int magicLevel, int magicHit, int targetMagicResistance,
                boolean targetBoss) {
            int result = probability(casterLevel, targetLevel,
                    effectiveIntelligence, magicLevel, magicHit,
                    targetMagicResistance);
            if (!targetBoss) return result;
            if (bossModifier == 0) return 0;
            return Math.max(1, result / bossModifier);
        }

        /** Skill174 is outside the retained circle-INT reduction range. */
        public int effectiveMpCost(int originalReduction) {
            if (originalReduction < 0 || originalReduction > 2) {
                throw new IllegalArgumentException(
                        "invalid Striker Gale MP inputs");
            }
            return Math.max(0, ResourceDatabase.value(this.mpCost, this.skillId, "StrikerGaleSpell", "mpCost") - originalReduction);
        }
    }

    record CounterDetectionSpell(int skillId, int row, int column, int actionId,
                                 int effectGfx, int mpCost,
                                 int reuseDelayMillis,
                                 int clientCastClockMillis, int nativeArea,
                                 int baseDamage, int diceCount, int dieSides,
                                 int attribute, String key) {
        public int actionId() {
            return ResourceDatabase.value(this.actionId, this.skillId, "CounterDetectionSpell", "actionId");
        }

        public int baseDamage() {
            return ResourceDatabase.value(this.baseDamage, this.skillId, "CounterDetectionSpell", "baseDamage");
        }

        public int diceCount() {
            return ResourceDatabase.value(this.diceCount, this.skillId, "CounterDetectionSpell", "diceCount");
        }

        public int dieSides() {
            return ResourceDatabase.value(this.dieSides, this.skillId, "CounterDetectionSpell", "dieSides");
        }

        public int effectGfx() {
            return ResourceDatabase.value(this.effectGfx, this.skillId, "CounterDetectionSpell", "effectGfx");
        }

        public int mpCost() {
            return ResourceDatabase.value(this.mpCost, this.skillId, "CounterDetectionSpell", "mpCost");
        }

        public int reuseDelayMillis() {
            return ResourceDatabase.value(this.reuseDelayMillis, this.skillId, "CounterDetectionSpell", "reuseDelayMillis");
        }

        CounterDetectionSpell {
            if (skillId != (row << 3) + column + 1 || row < 0 || row > 0xFF
                    || column < 0 || column > 7 || actionId < 0 || actionId > 0xFF
                    || effectGfx <= 0 || effectGfx > 0xFFFF || mpCost <= 0
                    || reuseDelayMillis < 0 || clientCastClockMillis < 0
                    || nativeArea != -1
                    || baseDamage < 0 || diceCount <= 0 || dieSides <= 0
                    || attribute < 0 || key == null || key.isBlank()) {
                throw new IllegalArgumentException(
                        "invalid native Counter Detection metadata");
            }
        }

        int rollDamage(IntUnaryOperator dieRoller) {
            if (dieRoller == null) {
                throw new IllegalArgumentException(
                        "missing Counter Detection die roller");
            }
            int damage = baseDamage;
            for (int index = 0; index < diceCount; index++) {
                int roll = dieRoller.applyAsInt(dieSides);
                if (roll < 1 || roll > dieSides) {
                    throw new IllegalArgumentException(
                            "invalid Counter Detection die roll");
                }
                damage += roll;
            }
            return damage;
        }

        int effectiveMpCost(int effectiveIntelligence, int originalReduction) {
            if (effectiveIntelligence < 1 || effectiveIntelligence > 254
                    || originalReduction < 0 || originalReduction > 2) {
                throw new IllegalArgumentException(
                        "invalid Counter Detection MP inputs");
            }
            int result = ResourceDatabase.value(this.mpCost, this.skillId, "CounterDetectionSpell", "mpCost");
            if (effectiveIntelligence > 12 && skillId > 8 && skillId <= 80) result--;
            if (effectiveIntelligence > 13 && skillId > 16 && skillId <= 80) result--;
            if (effectiveIntelligence > 14 && skillId > 24 && skillId <= 80) result--;
            if (effectiveIntelligence > 15 && skillId > 32 && skillId <= 80) result--;
            if (effectiveIntelligence > 16 && skillId > 40 && skillId <= 80) result--;
            if (effectiveIntelligence > 17 && skillId > 48 && skillId <= 80) result--;
            if (effectiveIntelligence > 18 && skillId > 56 && skillId <= 80) result--;
            result = Math.max(result, 1);
            return Math.max(0, result - originalReduction);
        }
    }

    record RevealWeaknessSpell(
            int skillId, int row, int column, int actionId, int effectGfx,
            int mpCost, int reuseDelayMillis, int maximumRange, int targetTo,
            int areaRadius, int sourceType, int nativeId, int failureMessage,
            int earthEffectGfx, int fireEffectGfx, int waterEffectGfx,
            int windEffectGfx, String key) {
        public int actionId() {
            return ResourceDatabase.value(this.actionId, this.skillId, "RevealWeaknessSpell", "actionId");
        }

        public int earthEffectGfx() {
            return ResourceDatabase.value(this.earthEffectGfx, this.skillId, "RevealWeaknessSpell", "earthEffectGfx");
        }

        public int effectGfx() {
            return ResourceDatabase.value(this.effectGfx, this.skillId, "RevealWeaknessSpell", "effectGfx");
        }

        public int fireEffectGfx() {
            return ResourceDatabase.value(this.fireEffectGfx, this.skillId, "RevealWeaknessSpell", "fireEffectGfx");
        }

        public int mpCost() {
            return ResourceDatabase.value(this.mpCost, this.skillId, "RevealWeaknessSpell", "mpCost");
        }

        public int reuseDelayMillis() {
            return ResourceDatabase.value(this.reuseDelayMillis, this.skillId, "RevealWeaknessSpell", "reuseDelayMillis");
        }

        public int waterEffectGfx() {
            return ResourceDatabase.value(this.waterEffectGfx, this.skillId, "RevealWeaknessSpell", "waterEffectGfx");
        }

        public int windEffectGfx() {
            return ResourceDatabase.value(this.windEffectGfx, this.skillId, "RevealWeaknessSpell", "windEffectGfx");
        }

        RevealWeaknessSpell {
            if (skillId != REVEAL_WEAKNESS
                    || skillId != (row << 3) + column + 1
                    || row != 2 || column != 6 || actionId != 19
                    || effectGfx != 2_510 || mpCost != 8
                    || reuseDelayMillis != 10 || maximumRange != -1
                    || targetTo != 2 || areaRadius != 0
                    || sourceType != 128 || nativeId != 64
                    || failureMessage != 280
                    || earthEffectGfx != 2_169 || fireEffectGfx != 2_167
                    || waterEffectGfx != 2_166 || windEffectGfx != 2_168
                    || key == null || key.isBlank()) {
                throw new IllegalArgumentException(
                        "invalid native Reveal Weakness metadata");
            }
        }

        RequestShape requestShape() {
            return RequestShape.OBJECT_TARGET;
        }

        int[] weaknessEffects(int weakAttribute) {
            int count = Integer.bitCount(weakAttribute & 0x0F);
            int[] effects = new int[count];
            int index = 0;
            if ((weakAttribute & ATTRIBUTE_EARTH) == ATTRIBUTE_EARTH) {
                effects[index++] = earthEffectGfx;
            }
            if ((weakAttribute & ATTRIBUTE_FIRE) == ATTRIBUTE_FIRE) {
                effects[index++] = fireEffectGfx;
            }
            if ((weakAttribute & ATTRIBUTE_WATER) == ATTRIBUTE_WATER) {
                effects[index++] = waterEffectGfx;
            }
            if ((weakAttribute & ATTRIBUTE_WIND) == ATTRIBUTE_WIND) {
                effects[index] = windEffectGfx;
            }
            return effects;
        }

        /** Retained circle reductions, positive floor, then original INT. */
        int effectiveMpCost(int effectiveIntelligence, int originalReduction) {
            if (effectiveIntelligence < 1 || effectiveIntelligence > 254
                    || originalReduction < 0 || originalReduction > 2) {
                throw new IllegalArgumentException(
                        "invalid Reveal Weakness MP inputs");
            }
            int result = ResourceDatabase.value(this.mpCost, this.skillId, "RevealWeaknessSpell", "mpCost");
            if (effectiveIntelligence > 12) result--;
            if (effectiveIntelligence > 13) result--;
            // The retained third-circle boundary is strictly skillId > 23,
            // so skill23 itself receives no further circle reduction.
            result = Math.max(result, 1);
            return Math.max(0, result - originalReduction);
        }
    }

    record DetectionSpell(int skillId, int row, int column, int actionId,
                          int effectGfx, int mpCost, int reuseDelayMillis,
                          int nativeArea, int sourceType, int nativeId,
                          String key) {
        public int actionId() {
            return ResourceDatabase.value(this.actionId, this.skillId, "DetectionSpell", "actionId");
        }

        public int effectGfx() {
            return ResourceDatabase.value(this.effectGfx, this.skillId, "DetectionSpell", "effectGfx");
        }

        public int mpCost() {
            return ResourceDatabase.value(this.mpCost, this.skillId, "DetectionSpell", "mpCost");
        }

        public int reuseDelayMillis() {
            return ResourceDatabase.value(this.reuseDelayMillis, this.skillId, "DetectionSpell", "reuseDelayMillis");
        }

        DetectionSpell {
            if (skillId != DETECTION || skillId != (row << 3) + column + 1
                    || row != 1 || column != 4 || actionId != 19
                    || effectGfx != 749 || mpCost != 15
                    || reuseDelayMillis != 500 || nativeArea != -1
                    || sourceType != 2 || nativeId != 16
                    || key == null || key.isBlank()) {
                throw new IllegalArgumentException(
                        "invalid native Detection metadata");
            }
        }

        int effectiveMpCost(int effectiveIntelligence, int originalReduction,
                            boolean strengthMagicHelmEquipped) {
            if (effectiveIntelligence < 1 || effectiveIntelligence > 254
                    || originalReduction < 0 || originalReduction > 2) {
                throw new IllegalArgumentException(
                        "invalid Detection MP inputs");
            }
            int result = ResourceDatabase.value(this.mpCost, this.skillId, "DetectionSpell", "mpCost");
            // Retained isHPMPConsume order: circle-2 INT reduction, Strength
            // Magic Helm 20015 halving, positive-cost floor, then the class
            // original-INT reduction.
            if (effectiveIntelligence > 12) result--;
            if (strengthMagicHelmEquipped) result >>= 1;
            result = Math.max(result, 1);
            return Math.max(0, result - originalReduction);
        }

        int effectiveMpCost(int effectiveIntelligence, int originalReduction) {
            return effectiveMpCost(
                    effectiveIntelligence, originalReduction, false);
        }
    }

    record MagicalWeaponSpell(int skillId, int row, int column, int actionId,
                              int effectGfx, int mpCost, int reuseDelayMillis,
                              long itemConsumeId, int itemConsumeCount,
                              int happenMessage, int failureMessage,
                              int successMessage, String key) {
        public long itemConsumeId() {
            return CombatDatabase.longValue(this.itemConsumeId, this.skillId, "itemConsumeId");
        }

        public int actionId() {
            return ResourceDatabase.value(this.actionId, this.skillId, "MagicalWeaponSpell", "actionId");
        }

        public int effectGfx() {
            return ResourceDatabase.value(this.effectGfx, this.skillId, "MagicalWeaponSpell", "effectGfx");
        }

        public int itemConsumeCount() {
            return ResourceDatabase.value(this.itemConsumeCount, this.skillId, "MagicalWeaponSpell", "itemConsumeCount");
        }

        public int mpCost() {
            return ResourceDatabase.value(this.mpCost, this.skillId, "MagicalWeaponSpell", "mpCost");
        }

        public int reuseDelayMillis() {
            return ResourceDatabase.value(this.reuseDelayMillis, this.skillId, "MagicalWeaponSpell", "reuseDelayMillis");
        }

        MagicalWeaponSpell {
            if (skillId != (row << 3) + column + 1 || row != 9 || column != 0
                    || actionId != 19 || effectGfx != 763 || mpCost != 200
                    || reuseDelayMillis != 0
                    || itemConsumeId != InventoryCatalog.MAGIC_GEM
                    || itemConsumeCount != 10 || happenMessage != 685
                    || failureMessage != 79 || successMessage != 161
                    || key == null || key.isBlank()) {
                throw new IllegalArgumentException(
                        "invalid native Create Magical Weapon metadata");
            }
        }

        /** Exact retained circle reductions, positive floor, then original INT. */
        int effectiveMpCost(int effectiveIntelligence, int originalReduction) {
            if (effectiveIntelligence < 1 || effectiveIntelligence > 254
                    || originalReduction < 0 || originalReduction > 2) {
                throw new IllegalArgumentException(
                        "invalid Create Magical Weapon MP inputs");
            }
            int result = ResourceDatabase.value(this.mpCost, this.skillId, "MagicalWeaponSpell", "mpCost");
            if (effectiveIntelligence > 12 && skillId > 8 && skillId <= 80) result--;
            if (effectiveIntelligence > 13 && skillId > 16 && skillId <= 80) result--;
            if (effectiveIntelligence > 14 && skillId > 24 && skillId <= 80) result--;
            if (effectiveIntelligence > 15 && skillId > 32 && skillId <= 80) result--;
            if (effectiveIntelligence > 16 && skillId > 40 && skillId <= 80) result--;
            if (effectiveIntelligence > 17 && skillId > 48 && skillId <= 80) result--;
            if (effectiveIntelligence > 18 && skillId > 56 && skillId <= 80) result--;
            result = Math.max(result, 1);
            return Math.max(0, result - originalReduction);
        }
    }

    record CreateZombieSpell(int skillId, int row, int column, int actionId,
                             int effectGfx, int mpCost, int hpCost,
                             int reuseDelayMillis, int maximumRange,
                             int failureMessage, long itemConsumeId,
                             int itemConsumeCount, int petCost, String key) {
        public long itemConsumeId() {
            return CombatDatabase.longValue(this.itemConsumeId, this.skillId, "itemConsumeId");
        }

        public int actionId() {
            return ResourceDatabase.value(this.actionId, this.skillId, "CreateZombieSpell", "actionId");
        }

        public int effectGfx() {
            return ResourceDatabase.value(this.effectGfx, this.skillId, "CreateZombieSpell", "effectGfx");
        }

        public int hpCost() {
            return ResourceDatabase.value(this.hpCost, this.skillId, "CreateZombieSpell", "hpCost");
        }

        public int itemConsumeCount() {
            return ResourceDatabase.value(this.itemConsumeCount, this.skillId, "CreateZombieSpell", "itemConsumeCount");
        }

        public int mpCost() {
            return ResourceDatabase.value(this.mpCost, this.skillId, "CreateZombieSpell", "mpCost");
        }

        public int reuseDelayMillis() {
            return ResourceDatabase.value(this.reuseDelayMillis, this.skillId, "CreateZombieSpell", "reuseDelayMillis");
        }

        CreateZombieSpell {
            if (skillId != (row << 3) + column + 1 || row < 0 || row > 0xFF
                    || column < 0 || column > 7 || actionId < 0 || actionId > 0xFF
                    || effectGfx <= 0 || effectGfx > 0xFFFF || mpCost <= 0
                    || hpCost < 0 || reuseDelayMillis < 0 || maximumRange < -1
                    || failureMessage <= 0 || failureMessage > 0xFFFF
                    || itemConsumeId <= 0 || itemConsumeId > 0xFFFF_FFFFL
                    || itemConsumeCount <= 0 || petCost <= 0
                    || key == null || key.isBlank()) {
                throw new IllegalArgumentException("invalid native create-zombie metadata");
            }
        }

        int effectiveMpCost(int effectiveIntelligence, int originalReduction) {
            if (effectiveIntelligence < 1 || effectiveIntelligence > 254
                    || originalReduction < 0 || originalReduction > 2) {
                throw new IllegalArgumentException("invalid create-zombie MP inputs");
            }
            int result = ResourceDatabase.value(this.mpCost, this.skillId, "CreateZombieSpell", "mpCost");
            if (effectiveIntelligence > 12 && skillId > 8 && skillId <= 80) result--;
            if (effectiveIntelligence > 13 && skillId > 16 && skillId <= 80) result--;
            if (effectiveIntelligence > 14 && skillId > 24 && skillId <= 80) result--;
            if (effectiveIntelligence > 15 && skillId > 32 && skillId <= 80) result--;
            if (effectiveIntelligence > 16 && skillId > 40 && skillId <= 80) result--;
            result = Math.max(result, 1);
            return Math.max(0, result - originalReduction);
        }
    }

    record ResurrectionSpell(int skillId, int row, int column, int actionId,
                             int effectGfx, int mpCost, int reuseDelayMillis,
                             int maximumRange, int failureMessage,
                             long itemConsumeId, int itemConsumeCount,
                             int questionId, int playerHpDivisor,
                             int npcHpDivisor, boolean restoresExperience,
                             int requiredElfAttribute, String key) {
        public long itemConsumeId() {
            return CombatDatabase.longValue(this.itemConsumeId, this.skillId, "itemConsumeId");
        }

        public int actionId() {
            return ResourceDatabase.value(this.actionId, this.skillId, "ResurrectionSpell", "actionId");
        }

        public int effectGfx() {
            return ResourceDatabase.value(this.effectGfx, this.skillId, "ResurrectionSpell", "effectGfx");
        }

        public int itemConsumeCount() {
            return ResourceDatabase.value(this.itemConsumeCount, this.skillId, "ResurrectionSpell", "itemConsumeCount");
        }

        public int mpCost() {
            return ResourceDatabase.value(this.mpCost, this.skillId, "ResurrectionSpell", "mpCost");
        }

        public int reuseDelayMillis() {
            return ResourceDatabase.value(this.reuseDelayMillis, this.skillId, "ResurrectionSpell", "reuseDelayMillis");
        }

        ResurrectionSpell {
            if (skillId != (row << 3) + column + 1 || row < 0 || row > 0xFF
                    || column < 0 || column > 7 || actionId < 0 || actionId > 0xFF
                    || effectGfx <= 0 || effectGfx > 0xFFFF || mpCost <= 0
                    || reuseDelayMillis < 0 || maximumRange < -1
                    || failureMessage <= 0 || failureMessage > 0xFFFF
                    || ((itemConsumeId == 0) != (itemConsumeCount == 0))
                    || itemConsumeId < 0 || itemConsumeId > 0xFFFF_FFFFL
                    || itemConsumeCount < 0 || itemConsumeCount > 0xFFFF
                    || (questionId != 321 && questionId != 322)
                    || (playerHpDivisor != 1 && playerHpDivisor != 2)
                    || (npcHpDivisor != 1 && npcHpDivisor != 4)
                    || (questionId == 321 && (playerHpDivisor != 2
                        || restoresExperience))
                    || (restoresExperience && questionId != 322)
                    || (requiredElfAttribute != 0
                        && requiredElfAttribute != ATTRIBUTE_WATER)
                    || key == null || key.isBlank()) {
                throw new IllegalArgumentException("invalid native resurrection metadata");
            }
        }

        int restoredPlayerHp(int maximumHp) {
            if (maximumHp <= 0 || maximumHp > 0xFFFF) {
                throw new IllegalArgumentException("invalid resurrection maximum HP");
            }
            return Math.max(1, maximumHp / playerHpDivisor);
        }

        int restoredNpcHp(int maximumHp) {
            if (maximumHp <= 0 || maximumHp > 0xFFFF) {
                throw new IllegalArgumentException("invalid resurrection NPC maximum HP");
            }
            return Math.max(1, maximumHp / npcHpDivisor);
        }

        int effectiveMpCost(int effectiveIntelligence, int originalReduction) {
            if (effectiveIntelligence < 1 || effectiveIntelligence > 254
                    || originalReduction < 0 || originalReduction > 2) {
                throw new IllegalArgumentException("invalid resurrection MP inputs");
            }
            int result = ResourceDatabase.value(this.mpCost, this.skillId, "ResurrectionSpell", "mpCost");
            if (effectiveIntelligence > 12 && skillId > 8 && skillId <= 80) result--;
            if (effectiveIntelligence > 13 && skillId > 16 && skillId <= 80) result--;
            if (effectiveIntelligence > 14 && skillId > 24 && skillId <= 80) result--;
            if (effectiveIntelligence > 15 && skillId > 32 && skillId <= 80) result--;
            if (effectiveIntelligence > 16 && skillId > 40 && skillId <= 80) result--;
            result = Math.max(result, 1);
            return Math.max(0, result - originalReduction);
        }
    }

    record SummonSpell(int skillId, int row, int column, int actionId,
                       int effectGfx, int mpCost, int hpCost,
                       int reuseDelayMillis, long itemConsumeId,
                       int itemConsumeCount, int lawfulDelta,
                       int mapFailureMessage, int capacityFailureMessage,
                       int levelFailureMessage, int spawnEffectGfx,
                       int lifetimeSeconds, String key) {
        public long itemConsumeId() {
            return CombatDatabase.longValue(this.itemConsumeId, this.skillId, "itemConsumeId");
        }

        public int actionId() {
            return ResourceDatabase.value(this.actionId, this.skillId, "SummonSpell", "actionId");
        }

        public int effectGfx() {
            return ResourceDatabase.value(this.effectGfx, this.skillId, "SummonSpell", "effectGfx");
        }

        public int hpCost() {
            return ResourceDatabase.value(this.hpCost, this.skillId, "SummonSpell", "hpCost");
        }

        public int itemConsumeCount() {
            return ResourceDatabase.value(this.itemConsumeCount, this.skillId, "SummonSpell", "itemConsumeCount");
        }

        public int mpCost() {
            return ResourceDatabase.value(this.mpCost, this.skillId, "SummonSpell", "mpCost");
        }

        public int reuseDelayMillis() {
            return ResourceDatabase.value(this.reuseDelayMillis, this.skillId, "SummonSpell", "reuseDelayMillis");
        }

        public int spawnEffectGfx() {
            return ResourceDatabase.value(this.spawnEffectGfx, this.skillId, "SummonSpell", "spawnEffectGfx");
        }

        SummonSpell {
            if (skillId != (row << 3) + column + 1 || row < 0 || row > 0xFF
                    || column < 0 || column > 7 || actionId < 0 || actionId > 0xFF
                    || effectGfx <= 0 || effectGfx > 0xFFFF || mpCost <= 0
                    || hpCost < 0 || reuseDelayMillis < 0
                    || itemConsumeId <= 0 || itemConsumeId > 0xFFFF_FFFFL
                    || itemConsumeCount <= 0 || lawfulDelta >= 0
                    || mapFailureMessage <= 0 || mapFailureMessage > 0xFFFF
                    || capacityFailureMessage <= 0 || capacityFailureMessage > 0xFFFF
                    || levelFailureMessage <= 0 || levelFailureMessage > 0xFFFF
                    || spawnEffectGfx <= 0 || spawnEffectGfx > 0xFFFF
                    || lifetimeSeconds <= 0 || lifetimeSeconds > 0xFFFF
                    || key == null || key.isBlank()) {
                throw new IllegalArgumentException("invalid native summon metadata");
            }
        }

        int directGrade(int level) {
            int grade = ((level - 28) / 4) + 7;
            return Math.max(7, Math.min(13, grade));
        }

        /**
         * Native summonlist callback {@code loc_40A08B} compares the signed
         * low selection byte with the unclamped level grade and rejects only
         * when the grade is lower.  Higher-level ring users may therefore
         * choose an earlier row, including the three special rows at grades
         * 17/18; the direct producer remains independently clamped to 7..13.
         */
        boolean permitsControlSelection(int level, long selection) {
            if (level <= 0 || selection < 0 || selection > 0xFFFF_FFFFL) {
                return false;
            }
            int grade = ((level - 28) / 4) + 7;
            int selectedGrade = (byte) (selection & 0xFF);
            return selectedGrade >= 0 && grade >= selectedGrade;
        }

        int effectiveMpCost(int effectiveIntelligence, int originalReduction) {
            if (effectiveIntelligence < 1 || effectiveIntelligence > 254
                    || originalReduction < 0 || originalReduction > 2) {
                throw new IllegalArgumentException("invalid summon MP inputs");
            }
            int result = ResourceDatabase.value(this.mpCost, this.skillId, "SummonSpell", "mpCost");
            if (effectiveIntelligence > 12 && skillId > 8 && skillId <= 80) result--;
            if (effectiveIntelligence > 13 && skillId > 16 && skillId <= 80) result--;
            if (effectiveIntelligence > 14 && skillId > 24 && skillId <= 80) result--;
            if (effectiveIntelligence > 15 && skillId > 32 && skillId <= 80) result--;
            if (effectiveIntelligence > 16 && skillId > 40 && skillId <= 80) result--;
            result = Math.max(result, 1);
            return Math.max(0, result - originalReduction);
        }
    }

    record SummonChoice(int selection, int npcId, int requiredLevel,
                        int petCost, int petCostAdjustment,
                        boolean directChoice, boolean exclusive) {
        SummonChoice {
            if (selection < 7 || npcId < 81210 || npcId > 81240
                    || requiredLevel < 28 || requiredLevel > 72
                    || petCost <= 0 || petCostAdjustment < 0
                    || (exclusive != (petCostAdjustment > 0))) {
                throw new IllegalArgumentException("invalid native summon choice");
            }
        }

        int effectivePetCost() {
            return Math.addExact(petCost, petCostAdjustment);
        }
    }

    record ZombieTemplate(int npcId, int gfx, String name,
                          int level, int maxHp) {
        ZombieTemplate {
            if (npcId <= 0 || gfx <= 0 || gfx > 0xFFFF
                    || name == null || name.isBlank()
                    || level <= 0 || level > 0xFF || maxHp <= 0) {
                throw new IllegalArgumentException("invalid native zombie template");
            }
        }
    }

    record SleepSpell(int skillId, int row, int column, int actionId,
                      int effectGfx, int mpCost, int reuseDelayMillis,
                      int maximumRange, int areaRadius, int durationSeconds,
                      int targetFailureMessage, int casterFailureMessage,
                      int probabilityHigh, int probabilityEqual, int probabilityLow,
                      double intelligenceMultiplier, double magicResistanceMultiplier,
                      boolean includeMagicLevel, boolean includeMagicHit,
                      long itemConsumeId, int itemConsumeCount, String key) {
        public long itemConsumeId() {
            return CombatDatabase.longValue(this.itemConsumeId, this.skillId, "itemConsumeId");
        }

        public int actionId() {
            return ResourceDatabase.value(this.actionId, this.skillId, "SleepSpell", "actionId");
        }

        public int durationSeconds() {
            return ResourceDatabase.value(this.durationSeconds, this.skillId, "SleepSpell", "durationSeconds");
        }

        public int effectGfx() {
            return ResourceDatabase.value(this.effectGfx, this.skillId, "SleepSpell", "effectGfx");
        }

        public int itemConsumeCount() {
            return ResourceDatabase.value(this.itemConsumeCount, this.skillId, "SleepSpell", "itemConsumeCount");
        }

        public int mpCost() {
            return ResourceDatabase.value(this.mpCost, this.skillId, "SleepSpell", "mpCost");
        }

        public int reuseDelayMillis() {
            return ResourceDatabase.value(this.reuseDelayMillis, this.skillId, "SleepSpell", "reuseDelayMillis");
        }

        SleepSpell {
            if (skillId != (row << 3) + column + 1 || row < 0 || row > 0xFF
                    || column < 0 || column > 7 || actionId < 0 || actionId > 0xFF
                    || effectGfx <= 0 || effectGfx > 0xFFFF || mpCost <= 0
                    || reuseDelayMillis < 0 || maximumRange < 0 || areaRadius <= 0
                    || durationSeconds <= 0 || targetFailureMessage <= 0
                    || targetFailureMessage > 0xFFFF || casterFailureMessage <= 0
                    || casterFailureMessage > 0xFFFF
                    || probabilityHigh < 0 || probabilityHigh > 100
                    || probabilityEqual < 0 || probabilityEqual > 100
                    || probabilityLow < 0 || probabilityLow > 100
                    || !Double.isFinite(intelligenceMultiplier)
                    || intelligenceMultiplier < 0.0D
                    || !Double.isFinite(magicResistanceMultiplier)
                    || magicResistanceMultiplier < 0.0D
                    || itemConsumeId <= 0 || itemConsumeId > 0xFFFF_FFFFL
                    || itemConsumeCount <= 0 || key == null || key.isBlank()) {
                throw new IllegalArgumentException("invalid native sleep-spell metadata");
            }
        }

        int probability(int casterLevel, int targetLevel, int effectiveIntelligence,
                        int magicLevel, int magicHit,
                        int targetMagicResistance, int targetSleepResistance) {
            if (casterLevel <= 0 || targetLevel <= 0 || effectiveIntelligence < 0
                    || magicLevel < 0 || targetMagicResistance < 0
                    || targetSleepResistance < -128 || targetSleepResistance > 127) {
                throw new IllegalArgumentException("invalid sleep probability inputs");
            }
            double result = casterLevel > targetLevel ? probabilityHigh
                    : casterLevel == targetLevel ? probabilityEqual : probabilityLow;
            result += effectiveIntelligence * intelligenceMultiplier;
            if (includeMagicHit) result += magicHit;
            if (includeMagicLevel) result += magicLevel;
            result -= targetMagicResistance * magicResistanceMultiplier;
            result -= targetSleepResistance;
            result = Math.max(1.0D, Math.min(100.0D, result));
            return Math.max(1, Math.min(90, (int) result));
        }

        /** Circle reductions precede the positive floor; original-INT follows it. */
        int effectiveMpCost(int effectiveIntelligence, int originalReduction) {
            if (effectiveIntelligence < 1 || effectiveIntelligence > 254
                    || originalReduction < 0 || originalReduction > 2) {
                throw new IllegalArgumentException("invalid sleep MP inputs");
            }
            int result = ResourceDatabase.value(this.mpCost, this.skillId, "SleepSpell", "mpCost");
            if (effectiveIntelligence > 12 && skillId > 8 && skillId <= 80) result--;
            if (effectiveIntelligence > 13 && skillId > 16 && skillId <= 80) result--;
            if (effectiveIntelligence > 14 && skillId > 24 && skillId <= 80) result--;
            if (effectiveIntelligence > 15 && skillId > 32 && skillId <= 80) result--;
            if (effectiveIntelligence > 16 && skillId > 40 && skillId <= 80) result--;
            if (effectiveIntelligence > 17 && skillId > 48 && skillId <= 80) result--;
            if (effectiveIntelligence > 18 && skillId > 56 && skillId <= 80) result--;
            result = Math.max(result, 1);
            return Math.max(0, result - originalReduction);
        }
    }

    record PolymorphSpell(int skillId, int row, int column, int actionId,
                          int effectGfx, int mpCost, int hpCost,
                          int reuseDelayMillis, int durationSeconds,
                          int maximumRange, long itemConsumeId,
                          int itemConsumeCount, int failureMessage, String key) {
        public long itemConsumeId() {
            return CombatDatabase.longValue(this.itemConsumeId, this.skillId, "itemConsumeId");
        }

        public int actionId() {
            return ResourceDatabase.value(this.actionId, this.skillId, "PolymorphSpell", "actionId");
        }

        public int durationSeconds() {
            return ResourceDatabase.value(this.durationSeconds, this.skillId, "PolymorphSpell", "durationSeconds");
        }

        public int effectGfx() {
            return ResourceDatabase.value(this.effectGfx, this.skillId, "PolymorphSpell", "effectGfx");
        }

        public int hpCost() {
            return ResourceDatabase.value(this.hpCost, this.skillId, "PolymorphSpell", "hpCost");
        }

        public int itemConsumeCount() {
            return ResourceDatabase.value(this.itemConsumeCount, this.skillId, "PolymorphSpell", "itemConsumeCount");
        }

        public int mpCost() {
            return ResourceDatabase.value(this.mpCost, this.skillId, "PolymorphSpell", "mpCost");
        }

        public int reuseDelayMillis() {
            return ResourceDatabase.value(this.reuseDelayMillis, this.skillId, "PolymorphSpell", "reuseDelayMillis");
        }

        PolymorphSpell {
            if (skillId != (row << 3) + column + 1 || skillId != SHAPE_CHANGE
                    || row < 0 || row > 0xFF || column < 0 || column > 7
                    || actionId < 0 || actionId > 0xFF
                    || effectGfx <= 0 || effectGfx > 0xFFFF
                    || mpCost < 0 || hpCost < 0 || reuseDelayMillis < 0
                    || durationSeconds <= 0 || durationSeconds > 0xFFFF
                    || maximumRange != -1 || itemConsumeId <= 0
                    || itemConsumeCount <= 0 || failureMessage < 0
                    || failureMessage > 0xFFFF || key == null || key.isBlank()) {
                throw new IllegalArgumentException("invalid native polymorph spell metadata");
            }
        }

        int effectiveMpCost(int effectiveIntelligence, int originalReduction) {
            if (effectiveIntelligence < 1 || effectiveIntelligence > 254
                    || originalReduction < 0 || originalReduction > 2) {
                throw new IllegalArgumentException("invalid native polymorph MP inputs");
            }
            int result = ResourceDatabase.value(this.mpCost, this.skillId, "PolymorphSpell", "mpCost");
            if (effectiveIntelligence > 12 && skillId > 8 && skillId <= 80) result--;
            if (effectiveIntelligence > 13 && skillId > 16 && skillId <= 80) result--;
            if (effectiveIntelligence > 14 && skillId > 23 && skillId <= 80) result--;
            if (effectiveIntelligence > 15 && skillId > 32 && skillId <= 80) result--;
            if (effectiveIntelligence > 16 && skillId > 40 && skillId <= 80) result--;
            if (effectiveIntelligence > 17 && skillId > 48 && skillId <= 80) result--;
            if (effectiveIntelligence > 18 && skillId > 56 && skillId <= 80) result--;
            if (ResourceDatabase.value(this.mpCost, this.skillId, "PolymorphSpell", "mpCost") > 0) result = Math.max(result, 1);
            return Math.max(0, result - originalReduction);
        }
    }

    record BindSpell(int skillId, int row, int column, int actionId,
                     int effectGfx, int mpCost, int reuseDelayMillis,
                     int maximumRange, int durationSeconds, int failureMessage,
                     int probabilityHigh, int probabilityEqual, int probabilityLow,
                     double intelligenceMultiplier,
                     boolean includeMagicLevel, boolean includeMagicHit,
                     long itemConsumeId, int itemConsumeCount, String key) {
        public long itemConsumeId() {
            return CombatDatabase.longValue(this.itemConsumeId, this.skillId, "itemConsumeId");
        }

        public int actionId() {
            return ResourceDatabase.value(this.actionId, this.skillId, "BindSpell", "actionId");
        }

        public int durationSeconds() {
            return ResourceDatabase.value(this.durationSeconds, this.skillId, "BindSpell", "durationSeconds");
        }

        public int effectGfx() {
            return ResourceDatabase.value(this.effectGfx, this.skillId, "BindSpell", "effectGfx");
        }

        public int itemConsumeCount() {
            return ResourceDatabase.value(this.itemConsumeCount, this.skillId, "BindSpell", "itemConsumeCount");
        }

        public int mpCost() {
            return ResourceDatabase.value(this.mpCost, this.skillId, "BindSpell", "mpCost");
        }

        public int reuseDelayMillis() {
            return ResourceDatabase.value(this.reuseDelayMillis, this.skillId, "BindSpell", "reuseDelayMillis");
        }

        BindSpell {
            if (skillId != (row << 3) + column + 1 || row < 0 || row > 0xFF
                    || column < 0 || column > 7 || actionId < 0 || actionId > 0xFF
                    || effectGfx <= 0 || effectGfx > 0xFFFF || mpCost <= 0
                    || reuseDelayMillis < 0 || maximumRange != -1
                    || durationSeconds <= 0 || durationSeconds > 0xFFFF
                    || failureMessage <= 0 || failureMessage > 0xFFFF
                    || probabilityHigh < 0 || probabilityHigh > 100
                    || probabilityEqual < 0 || probabilityEqual > 100
                    || probabilityLow < 0 || probabilityLow > 100
                    || intelligenceMultiplier < 0.0D
                    || itemConsumeId <= 0 || itemConsumeId > 0xFFFF_FFFFL
                    || itemConsumeCount <= 0 || key == null || key.isBlank()) {
                throw new IllegalArgumentException("invalid native Earth Bind metadata");
            }
        }

        int probability(int casterLevel, int targetLevel, int intelligence,
                        int magicLevel, int magicHit, int sustainResistance) {
            if (casterLevel <= 0 || targetLevel <= 0 || intelligence < 1
                    || intelligence > 254 || sustainResistance < -128
                    || sustainResistance > 127) {
                throw new IllegalArgumentException("invalid Earth Bind probability inputs");
            }
            double probability = casterLevel > targetLevel ? probabilityHigh
                    : casterLevel == targetLevel ? probabilityEqual : probabilityLow;
            probability += intelligence * intelligenceMultiplier;
            if (includeMagicLevel) probability += magicLevel;
            if (includeMagicHit) probability += magicHit;
            probability -= sustainResistance;
            return Math.max(1, Math.min(90, (int) probability));
        }

        /** Skill 157 is outside the retained circle-INT reduction range. */
        int effectiveMpCost(int originalReduction) {
            if (originalReduction < 0 || originalReduction > 2) {
                throw new IllegalArgumentException("invalid Earth Bind MP inputs");
            }
            return Math.max(0, ResourceDatabase.value(this.mpCost, this.skillId, "BindSpell", "mpCost") - originalReduction);
        }
    }

    record PoisonSpell(int skillId, int row, int column, int actionId,
                       int effectGfx, int mpCost, int reuseDelayMillis,
                       int maximumRange, int durationSeconds,
                       int tickIntervalMillis, int tickDamage,
                       int probabilityDice, int bossModifier,
                       int lawfulDelta, int failureMessage,
                       long itemConsumeId, int itemConsumeCount, String key) {
        public long itemConsumeId() {
            return CombatDatabase.longValue(this.itemConsumeId, this.skillId, "itemConsumeId");
        }

        public int actionId() {
            return ResourceDatabase.value(this.actionId, this.skillId, "PoisonSpell", "actionId");
        }

        public int durationSeconds() {
            return ResourceDatabase.value(this.durationSeconds, this.skillId, "PoisonSpell", "durationSeconds");
        }

        public int effectGfx() {
            return ResourceDatabase.value(this.effectGfx, this.skillId, "PoisonSpell", "effectGfx");
        }

        public int itemConsumeCount() {
            return ResourceDatabase.value(this.itemConsumeCount, this.skillId, "PoisonSpell", "itemConsumeCount");
        }

        public int mpCost() {
            return ResourceDatabase.value(this.mpCost, this.skillId, "PoisonSpell", "mpCost");
        }

        public int probabilityDice() {
            return ResourceDatabase.value(this.probabilityDice, this.skillId, "PoisonSpell", "probabilityDice");
        }

        public int reuseDelayMillis() {
            return ResourceDatabase.value(this.reuseDelayMillis, this.skillId, "PoisonSpell", "reuseDelayMillis");
        }

        PoisonSpell {
            if (skillId != (row << 3) + column + 1 || row < 0 || row > 0xFF
                    || column < 0 || column > 7 || actionId < 0 || actionId > 0xFF
                    || effectGfx <= 0 || effectGfx > 0xFFFF || mpCost < 0
                    || reuseDelayMillis < 0 || maximumRange < 0
                    || durationSeconds <= 0 || tickIntervalMillis <= 0 || tickDamage <= 0
                    || probabilityDice <= 0 || bossModifier != 0
                    || lawfulDelta != -10
                    || failureMessage <= 0 || failureMessage > 0xFFFF
                    || itemConsumeId <= 0 || itemConsumeId > 0xFFFF_FFFFL
                    || itemConsumeCount <= 0 || key == null || key.isBlank()) {
                throw new IllegalArgumentException("invalid native poison spell metadata");
            }
        }

        int probability(int characterType, int casterLevel,
                        int effectiveIntelligence, int originalMagicHit,
                        int equipmentMagicHit, int targetMagicResistance,
                        boolean targetBoss, IntUnaryOperator dieRoll) {
            return BattleTurnUndead.probability(
                    characterType, casterLevel, effectiveIntelligence,
                    originalMagicHit, equipmentMagicHit, targetMagicResistance,
                    probabilityDice, targetBoss ? bossModifier : -1, dieRoll);
        }

        int effectiveMpCost(int effectiveIntelligence, int originalReduction) {
            if (effectiveIntelligence < 1 || effectiveIntelligence > 254
                    || originalReduction < 0 || originalReduction > 2) {
                throw new IllegalArgumentException("invalid Curse Poison MP inputs");
            }
            int result = ResourceDatabase.value(this.mpCost, this.skillId, "PoisonSpell", "mpCost");
            if (effectiveIntelligence > 12 && skillId > 8 && skillId <= 80) result--;
            result = Math.max(result, 1);
            return Math.max(0, result - originalReduction);
        }
    }

    record ParalysisSpell(int skillId, int row, int column, int actionId,
                          int effectGfx, int mpCost, int reuseDelayMillis,
                          int clientCastClockMillis, int maximumRange,
                          int warningDelayMillis,
                          int paralysisDurationMillis, int lawfulDelta,
                          int failureMessage,
                          int probabilityHigh, int probabilityEqual,
                          int probabilityLow, double intelligenceMultiplier,
                          double magicResistanceMultiplier,
                          boolean includeMagicLevel, boolean includeMagicHit,
                          long itemConsumeId, int itemConsumeCount, String key) {
        public long itemConsumeId() {
            return CombatDatabase.longValue(this.itemConsumeId, this.skillId, "itemConsumeId");
        }

        public int actionId() {
            return ResourceDatabase.value(this.actionId, this.skillId, "ParalysisSpell", "actionId");
        }

        public int effectGfx() {
            return ResourceDatabase.value(this.effectGfx, this.skillId, "ParalysisSpell", "effectGfx");
        }

        public int itemConsumeCount() {
            return ResourceDatabase.value(this.itemConsumeCount, this.skillId, "ParalysisSpell", "itemConsumeCount");
        }

        public int mpCost() {
            return ResourceDatabase.value(this.mpCost, this.skillId, "ParalysisSpell", "mpCost");
        }

        public int paralysisDurationMillis() {
            return ResourceDatabase.value(this.paralysisDurationMillis, this.skillId, "ParalysisSpell", "paralysisDurationMillis");
        }

        public int reuseDelayMillis() {
            return ResourceDatabase.value(this.reuseDelayMillis, this.skillId, "ParalysisSpell", "reuseDelayMillis");
        }

        public int warningDelayMillis() {
            return ResourceDatabase.value(this.warningDelayMillis, this.skillId, "ParalysisSpell", "warningDelayMillis");
        }

        ParalysisSpell {
            if (skillId != (row << 3) + column + 1 || row < 0 || row > 0xFF
                    || column < 0 || column > 7 || actionId < 0 || actionId > 0xFF
                    || effectGfx <= 0 || effectGfx > 0xFFFF || mpCost <= 0
                    || reuseDelayMillis < 0 || clientCastClockMillis < 0
                    || clientCastClockMillis > 0xFFFF || maximumRange < 0
                    || warningDelayMillis <= 0 || paralysisDurationMillis <= 0
                    || lawfulDelta != -500
                    || failureMessage <= 0 || failureMessage > 0xFFFF
                    || probabilityHigh < 0 || probabilityHigh > 100
                    || probabilityEqual < 0 || probabilityEqual > 100
                    || probabilityLow < 0 || probabilityLow > 100
                    || !Double.isFinite(intelligenceMultiplier)
                    || intelligenceMultiplier < 0.0D
                    || !Double.isFinite(magicResistanceMultiplier)
                    || magicResistanceMultiplier < 0.0D
                    || itemConsumeId <= 0 || itemConsumeId > 0xFFFF_FFFFL
                    || itemConsumeCount <= 0 || key == null || key.isBlank()) {
                throw new IllegalArgumentException("invalid native paralysis spell metadata");
            }
        }

        /** Selected skills_負面技能控制 row followed by the retained outer 90% cap. */
        int probability(int casterLevel, int targetLevel, int effectiveIntelligence,
                        int magicLevel, int magicHit,
                        int targetMagicResistance, int targetStoneResistance) {
            if (casterLevel <= 0 || targetLevel <= 0 || effectiveIntelligence < 0
                    || magicLevel < 0 || targetMagicResistance < 0
                    || targetStoneResistance < -128 || targetStoneResistance > 127) {
                throw new IllegalArgumentException("invalid paralysis probability inputs");
            }
            double result = casterLevel > targetLevel ? probabilityHigh
                    : casterLevel == targetLevel ? probabilityEqual : probabilityLow;
            result += effectiveIntelligence * intelligenceMultiplier;
            if (includeMagicHit) result += magicHit;
            if (includeMagicLevel) result += magicLevel;
            result -= targetMagicResistance * magicResistanceMultiplier;
            result -= targetStoneResistance;
            result = Math.max(1.0D, Math.min(100.0D, result));
            return Math.max(1, Math.min(90, (int) result));
        }

        /** Circle reductions precede the positive floor; original-INT follows it. */
        int effectiveMpCost(int effectiveIntelligence, int originalReduction) {
            if (effectiveIntelligence < 1 || effectiveIntelligence > 254
                    || originalReduction < 0 || originalReduction > 2) {
                throw new IllegalArgumentException("invalid paralysis MP inputs");
            }
            int result = ResourceDatabase.value(this.mpCost, this.skillId, "ParalysisSpell", "mpCost");
            if (effectiveIntelligence > 12 && skillId > 8 && skillId <= 80) result--;
            if (effectiveIntelligence > 13 && skillId > 16 && skillId <= 80) result--;
            if (effectiveIntelligence > 14 && skillId > 24 && skillId <= 80) result--;
            if (effectiveIntelligence > 15 && skillId > 32 && skillId <= 80) result--;
            result = Math.max(result, 1);
            return Math.max(0, result - originalReduction);
        }
    }

    record TamingSpell(int skillId, int row, int column, int actionId,
                       int effectGfx, int mpCost, int hpCost,
                       int reuseDelayMillis, int maximumRange,
                       int durationSeconds, int failureMessage,
                       int probabilityHigh, int probabilityEqual,
                       int probabilityLow, double intelligenceMultiplier,
                       double magicResistanceMultiplier,
                       boolean includeMagicLevel, boolean includeMagicHit,
                       long itemConsumeId, int itemConsumeCount, String key) {
        public long itemConsumeId() {
            return CombatDatabase.longValue(this.itemConsumeId, this.skillId, "itemConsumeId");
        }

        public int actionId() {
            return ResourceDatabase.value(this.actionId, this.skillId, "TamingSpell", "actionId");
        }

        public int durationSeconds() {
            return ResourceDatabase.value(this.durationSeconds, this.skillId, "TamingSpell", "durationSeconds");
        }

        public int effectGfx() {
            return ResourceDatabase.value(this.effectGfx, this.skillId, "TamingSpell", "effectGfx");
        }

        public int hpCost() {
            return ResourceDatabase.value(this.hpCost, this.skillId, "TamingSpell", "hpCost");
        }

        public int itemConsumeCount() {
            return ResourceDatabase.value(this.itemConsumeCount, this.skillId, "TamingSpell", "itemConsumeCount");
        }

        public int mpCost() {
            return ResourceDatabase.value(this.mpCost, this.skillId, "TamingSpell", "mpCost");
        }

        public int reuseDelayMillis() {
            return ResourceDatabase.value(this.reuseDelayMillis, this.skillId, "TamingSpell", "reuseDelayMillis");
        }

        TamingSpell {
            if (skillId != (row << 3) + column + 1 || row < 0 || row > 0xFF
                    || column < 0 || column > 7 || actionId < 0 || actionId > 0xFF
                    || effectGfx <= 0 || effectGfx > 0xFFFF || mpCost <= 0
                    || hpCost < 0 || reuseDelayMillis < 0 || maximumRange != -1
                    || durationSeconds <= 0 || durationSeconds > 0xFFFF
                    || failureMessage <= 0 || failureMessage > 0xFFFF
                    || probabilityHigh < 0 || probabilityHigh > 100
                    || probabilityEqual < 0 || probabilityEqual > 100
                    || probabilityLow < 0 || probabilityLow > 100
                    || !Double.isFinite(intelligenceMultiplier)
                    || intelligenceMultiplier < 0.0D
                    || !Double.isFinite(magicResistanceMultiplier)
                    || magicResistanceMultiplier < 0.0D
                    || itemConsumeId <= 0 || itemConsumeId > 0xFFFF_FFFFL
                    || itemConsumeCount <= 0 || key == null || key.isBlank()) {
                throw new IllegalArgumentException("invalid native taming spell metadata");
            }
        }

        int probability(int casterLevel, int targetLevel, int effectiveIntelligence,
                        int magicLevel, int magicHit, int targetMagicResistance) {
            if (casterLevel <= 0 || targetLevel <= 0 || effectiveIntelligence < 0
                    || magicLevel < 0 || targetMagicResistance < 0) {
                throw new IllegalArgumentException("invalid taming probability inputs");
            }
            double result = casterLevel > targetLevel ? probabilityHigh
                    : casterLevel == targetLevel ? probabilityEqual : probabilityLow;
            result += effectiveIntelligence * intelligenceMultiplier;
            if (includeMagicHit) result += magicHit;
            if (includeMagicLevel) result += magicLevel;
            result -= targetMagicResistance * magicResistanceMultiplier;
            result = Math.max(1.0D, Math.min(100.0D, result));
            return Math.max(1, Math.min(90, (int) result));
        }

        int effectiveMpCost(int effectiveIntelligence, int originalReduction) {
            if (effectiveIntelligence < 1 || effectiveIntelligence > 254
                    || originalReduction < 0 || originalReduction > 2) {
                throw new IllegalArgumentException("invalid taming MP inputs");
            }
            int result = ResourceDatabase.value(this.mpCost, this.skillId, "TamingSpell", "mpCost");
            if (effectiveIntelligence > 12 && skillId > 8 && skillId <= 80) result--;
            if (effectiveIntelligence > 13 && skillId > 16 && skillId <= 80) result--;
            if (effectiveIntelligence > 14 && skillId > 24 && skillId <= 80) result--;
            if (effectiveIntelligence > 15 && skillId > 32 && skillId <= 80) result--;
            result = Math.max(result, 1);
            return Math.max(0, result - originalReduction);
        }
    }

    record CurseRemovalSpell(int skillId, int row, int column, int actionId,
                             int effectGfx, int mpCost, int reuseDelayMillis,
                             int maximumRange, boolean curesCurseParalysis,
                             String key) {
        public int actionId() {
            return ResourceDatabase.value(this.actionId, this.skillId, "CurseRemovalSpell", "actionId");
        }

        public int effectGfx() {
            return ResourceDatabase.value(this.effectGfx, this.skillId, "CurseRemovalSpell", "effectGfx");
        }

        public int mpCost() {
            return ResourceDatabase.value(this.mpCost, this.skillId, "CurseRemovalSpell", "mpCost");
        }

        public int reuseDelayMillis() {
            return ResourceDatabase.value(this.reuseDelayMillis, this.skillId, "CurseRemovalSpell", "reuseDelayMillis");
        }

        CurseRemovalSpell {
            if (skillId != (row << 3) + column + 1 || row < 0 || row > 0xFF
                    || (skillId != CURE_POISON && skillId != REMOVE_CURSE)
                    || column < 0 || column > 7 || actionId < 0 || actionId > 0xFF
                    || effectGfx <= 0 || effectGfx > 0xFFFF || mpCost <= 0
                    || reuseDelayMillis < 0 || maximumRange != -1
                    || key == null || key.isBlank()) {
                throw new IllegalArgumentException(
                        "invalid native poison/curse cure spell metadata");
            }
        }

        /** Circle reductions precede the positive floor; original-INT follows it. */
        int effectiveMpCost(int effectiveIntelligence, int originalReduction) {
            if (effectiveIntelligence < 1 || effectiveIntelligence > 254
                    || originalReduction < 0 || originalReduction > 2) {
                throw new IllegalArgumentException("invalid poison/curse cure MP inputs");
            }
            int result = ResourceDatabase.value(this.mpCost, this.skillId, "CurseRemovalSpell", "mpCost");
            if (effectiveIntelligence > 12 && skillId > 8 && skillId <= 80) result--;
            if (effectiveIntelligence > 13 && skillId > 16 && skillId <= 80) result--;
            if (effectiveIntelligence > 14 && skillId > 24 && skillId <= 80) result--;
            if (effectiveIntelligence > 15 && skillId > 32 && skillId <= 80) result--;
            result = Math.max(result, 1);
            return Math.max(0, result - originalReduction);
        }
    }

    /** Native invisibility family: skill 60 is indefinite; skill 97 lasts 32 seconds. */
    record InvisibilitySpell(int skillId, int row, int column, int actionId,
                             int effectGfx, int mpCost, int reuseDelayMillis,
                             int durationSeconds, long itemConsumeId,
                             int itemConsumeCount, RequestShape requestShape,
                             String key) {
        public long itemConsumeId() {
            return CombatDatabase.longValue(this.itemConsumeId, this.skillId, "itemConsumeId");
        }

        public int actionId() {
            return ResourceDatabase.value(this.actionId, this.skillId, "InvisibilitySpell", "actionId");
        }

        public int durationSeconds() {
            return ResourceDatabase.value(this.durationSeconds, this.skillId, "InvisibilitySpell", "durationSeconds");
        }

        public int itemConsumeCount() {
            return ResourceDatabase.value(this.itemConsumeCount, this.skillId, "InvisibilitySpell", "itemConsumeCount");
        }

        public int mpCost() {
            return ResourceDatabase.value(this.mpCost, this.skillId, "InvisibilitySpell", "mpCost");
        }

        public int reuseDelayMillis() {
            return ResourceDatabase.value(this.reuseDelayMillis, this.skillId, "InvisibilitySpell", "reuseDelayMillis");
        }

        InvisibilitySpell {
            if (skillId != (row << 3) + column + 1 || row < 0 || row > 0xFF
                    || column < 0 || column > 7 || actionId < 0 || actionId > 0xFF
                    || effectGfx < 0 || effectGfx > 0xFFFF
                    || mpCost < 0 || reuseDelayMillis < 0
                    || durationSeconds < 0 || durationSeconds > 0xFFFF
                    || itemConsumeId < 0 || itemConsumeId > 0xFFFF_FFFFL
                    || itemConsumeCount < 0
                    || ((itemConsumeId == 0) != (itemConsumeCount == 0))
                    || requestShape == null
                    || requestShape == RequestShape.OBJECT_TARGET
                    || key == null || key.isBlank()) {
                throw new IllegalArgumentException("invalid native invisibility spell metadata");
            }
        }

        /** Retained circle reductions, positive-MP floor, then original-INT reduction. */
        int effectiveMpCost(int effectiveIntelligence, int originalReduction) {
            if (effectiveIntelligence < 1 || effectiveIntelligence > 254
                    || originalReduction < 0 || originalReduction > 2) {
                throw new IllegalArgumentException("invalid invisibility MP inputs");
            }
            int result = ResourceDatabase.value(this.mpCost, this.skillId, "InvisibilitySpell", "mpCost");
            if (effectiveIntelligence > 12 && skillId > 8 && skillId <= 80) result--;
            if (effectiveIntelligence > 13 && skillId > 16 && skillId <= 80) result--;
            if (effectiveIntelligence > 14 && skillId > 24 && skillId <= 80) result--;
            if (effectiveIntelligence > 15 && skillId > 32 && skillId <= 80) result--;
            result = Math.max(result, 1);
            return Math.max(0, result - originalReduction);
        }
    }

    record EnchantVenomSpell(
            int skillId, int row, int column, int actionId, int effectGfx,
            int mpCost, int hpCost, int reuseDelayMillis, int durationSeconds,
            long itemConsumeId, int itemConsumeCount, int procPercent,
            int poisonDurationSeconds, int tickIntervalMillis,
            int tickDamage, String key) {
        public long itemConsumeId() {
            return CombatDatabase.longValue(this.itemConsumeId, this.skillId, "itemConsumeId");
        }

        public int actionId() {
            return ResourceDatabase.value(this.actionId, this.skillId, "EnchantVenomSpell", "actionId");
        }

        public int durationSeconds() {
            return ResourceDatabase.value(this.durationSeconds, this.skillId, "EnchantVenomSpell", "durationSeconds");
        }

        public int effectGfx() {
            return ResourceDatabase.value(this.effectGfx, this.skillId, "EnchantVenomSpell", "effectGfx");
        }

        public int hpCost() {
            return ResourceDatabase.value(this.hpCost, this.skillId, "EnchantVenomSpell", "hpCost");
        }

        public int itemConsumeCount() {
            return ResourceDatabase.value(this.itemConsumeCount, this.skillId, "EnchantVenomSpell", "itemConsumeCount");
        }

        public int mpCost() {
            return ResourceDatabase.value(this.mpCost, this.skillId, "EnchantVenomSpell", "mpCost");
        }

        public int reuseDelayMillis() {
            return ResourceDatabase.value(this.reuseDelayMillis, this.skillId, "EnchantVenomSpell", "reuseDelayMillis");
        }

        EnchantVenomSpell {
            if (skillId != (row << 3) + column + 1 || row < 0 || row > 0xFF
                    || column < 0 || column > 7
                    || actionId < 0 || actionId > 0xFF
                    || effectGfx <= 0 || effectGfx > 0xFFFF
                    || mpCost < 0 || hpCost <= 0 || reuseDelayMillis < 0
                    || durationSeconds <= 0 || durationSeconds > 0xFFFF
                    || itemConsumeId <= 0 || itemConsumeId > 0xFFFF_FFFFL
                    || itemConsumeCount <= 0
                    || procPercent != BattleVenomEnchantStatus.PROC_PERCENT
                    || poisonDurationSeconds
                            != BattleVenomEnchantStatus.POISON_DURATION_SECONDS
                    || tickIntervalMillis
                            != BattleVenomEnchantStatus.POISON_TICK_MILLIS
                    || tickDamage != BattleVenomEnchantStatus.POISON_TICK_DAMAGE
                    || key == null || key.isBlank()) {
                throw new IllegalArgumentException(
                        "invalid native Enchant Venom metadata");
            }
        }

        /** Skill 98 is outside circle reductions; original-INT still applies. */
        int effectiveMpCost(int originalReduction) {
            if (originalReduction < 0 || originalReduction > 2) {
                throw new IllegalArgumentException(
                        "invalid Enchant Venom MP inputs");
            }
            return Math.max(0, Math.max(ResourceDatabase.value(this.mpCost, this.skillId, "EnchantVenomSpell", "mpCost"), 1) - originalReduction);
        }
    }

    record BringStoneSpell(
            int skillId, int row, int column, int actionId, int effectGfx,
            int mpCost, int reuseDelayMillis, int failureMessage,
            int successMessage, String key) {
        public int actionId() {
            return ResourceDatabase.value(this.actionId, this.skillId, "BringStoneSpell", "actionId");
        }

        public int effectGfx() {
            return ResourceDatabase.value(this.effectGfx, this.skillId, "BringStoneSpell", "effectGfx");
        }

        public int mpCost() {
            return ResourceDatabase.value(this.mpCost, this.skillId, "BringStoneSpell", "mpCost");
        }

        public int reuseDelayMillis() {
            return ResourceDatabase.value(this.reuseDelayMillis, this.skillId, "BringStoneSpell", "reuseDelayMillis");
        }

        BringStoneSpell {
            if (skillId != (row << 3) + column + 1 || skillId != BRING_STONE
                    || row < 0 || row > 0xFF || column < 0 || column > 7
                    || actionId < 0 || actionId > 0xFF
                    || effectGfx <= 0 || effectGfx > 0xFFFF
                    || mpCost <= 0 || reuseDelayMillis < 0
                    || failureMessage <= 0 || failureMessage > 0xFFFF
                    || successMessage <= 0 || successMessage > 0xFFFF
                    || key == null || key.isBlank()) {
                throw new IllegalArgumentException(
                        "invalid native Bring Stone metadata");
            }
        }

        /** Skill 100 is outside circle reductions; original-INT still applies. */
        int effectiveMpCost(int originalReduction) {
            if (originalReduction < 0 || originalReduction > 2) {
                throw new IllegalArgumentException("invalid Bring Stone MP inputs");
            }
            return Math.max(0, Math.max(ResourceDatabase.value(this.mpCost, this.skillId, "BringStoneSpell", "mpCost"), 1) - originalReduction);
        }

        int successThreshold(int tier, int level, int wisdom) {
            if (tier < 0 || tier > 3 || level < 0 || level > 127
                    || wisdom < 0 || wisdom > 0xFF) {
                throw new IllegalArgumentException(
                        "invalid Bring Stone probability inputs");
            }
            int dark = (int) (10 + level * 0.8 + (wisdom - 6) * 1.2);
            int brave = (int) (dark / 2.1);
            int wise = (int) (brave / 2.0);
            int kayser = (int) (wise / 1.9);
            return switch (tier) {
                case 0 -> dark;
                case 1 -> brave;
                case 2 -> wise;
                case 3 -> kayser;
                default -> throw new IllegalStateException("unreachable stone tier");
            };
        }

        boolean succeeds(int threshold, int oneBasedRoll) {
            if (oneBasedRoll < 1 || oneBasedRoll > 100) {
                throw new IllegalArgumentException("invalid Bring Stone roll");
            }
            return threshold >= oneBasedRoll;
        }
    }

    record StoneRefinement(int tier, long sourceCatalogId,
                           long targetCatalogId, String targetNameId) {
        StoneRefinement {
            if (tier < 0 || tier > 3 || sourceCatalogId <= 0
                    || sourceCatalogId > 0xFFFF_FFFFL || targetCatalogId <= 0
                    || targetCatalogId > 0xFFFF_FFFFL
                    || targetCatalogId != sourceCatalogId + 1
                    || targetNameId == null || !targetNameId.matches("\\$[0-9]+")) {
                throw new IllegalArgumentException(
                        "invalid native dark-stone refinement");
            }
        }
    }

    record TrueTargetSpell(int skillId, int row, int column, int skillLevel,
                           int actionId, int effectGfx, int mpCost, int hpCost,
                           int reuseDelayMillis, int durationSeconds,
                           int maximumRange, int targetTo, int attribute,
                           int sourceType, int nativeId, String nameId,
                           boolean counterMagicBypass, String key) {
        public int actionId() {
            return ResourceDatabase.value(this.actionId, this.skillId, "TrueTargetSpell", "actionId");
        }

        public int durationSeconds() {
            return ResourceDatabase.value(this.durationSeconds, this.skillId, "TrueTargetSpell", "durationSeconds");
        }

        public int effectGfx() {
            return ResourceDatabase.value(this.effectGfx, this.skillId, "TrueTargetSpell", "effectGfx");
        }

        public int hpCost() {
            return ResourceDatabase.value(this.hpCost, this.skillId, "TrueTargetSpell", "hpCost");
        }

        public int mpCost() {
            return ResourceDatabase.value(this.mpCost, this.skillId, "TrueTargetSpell", "mpCost");
        }

        public int reuseDelayMillis() {
            return ResourceDatabase.value(this.reuseDelayMillis, this.skillId, "TrueTargetSpell", "reuseDelayMillis");
        }

        TrueTargetSpell {
            if (skillId != (row << 3) + column + 1 || row < 0 || row > 0xFF
                    || column < 0 || column > 7 || skillLevel != 15
                    || actionId < 0 || actionId > 0xFF
                    || effectGfx <= 0 || effectGfx > 0xFFFF || mpCost != 1
                    || hpCost != 0 || reuseDelayMillis != 0 || durationSeconds != 0
                    || maximumRange != -1 || targetTo != 3 || attribute != 0
                    || sourceType != 2 || nativeId != 1
                    || nameId == null || !nameId.matches("\\$[0-9]+")
                    || !counterMagicBypass
                    || key == null || key.isBlank()) {
                throw new IllegalArgumentException("invalid native True Target metadata");
            }
        }

        int effectiveMpCost(int originalReduction) {
            if (originalReduction < 0 || originalReduction > 2) {
                throw new IllegalArgumentException("invalid True Target MP inputs");
            }
            // Retained isHPMPConsume keeps every positive native MP cost at
            // least one after the class/original-INT reduction.
            return Math.max(1, ResourceDatabase.value(this.mpCost, this.skillId, "TrueTargetSpell", "mpCost") - originalReduction);
        }
    }

    record RoyalAuraSpell(
            int skillId, int row, int column, int skillLevel,
            int actionId, int effectGfx, int mpCost, int hpCost,
            int reuseDelayMillis, int durationSeconds,
            int maximumRange, int area, int targetTo,
            int attribute, int sourceType, int nativeId, String nameId,
            int armorClass, int meleeHit, int rangedHit, int magicResistance,
            int meleeDamage,
            boolean counterMagicBypass, String key) {
        public int actionId() {
            return ResourceDatabase.value(this.actionId, this.skillId, "RoyalAuraSpell", "actionId");
        }

        public int durationSeconds() {
            return ResourceDatabase.value(this.durationSeconds, this.skillId, "RoyalAuraSpell", "durationSeconds");
        }

        public int effectGfx() {
            return ResourceDatabase.value(this.effectGfx, this.skillId, "RoyalAuraSpell", "effectGfx");
        }

        public int hpCost() {
            return ResourceDatabase.value(this.hpCost, this.skillId, "RoyalAuraSpell", "hpCost");
        }

        public int mpCost() {
            return ResourceDatabase.value(this.mpCost, this.skillId, "RoyalAuraSpell", "mpCost");
        }

        public int reuseDelayMillis() {
            return ResourceDatabase.value(this.reuseDelayMillis, this.skillId, "RoyalAuraSpell", "reuseDelayMillis");
        }

        RoyalAuraSpell {
            boolean glowing = skillId == GLOWING_AURA
                    && column == 1 && effectGfx == 2277 && targetTo == 8
                    && nativeId == 2 && "$1996".equals(nameId)
                    && armorClass == 0 && meleeHit == 5 && rangedHit == 5
                    && magicResistance == 20 && meleeDamage == 0;
            boolean shining = skillId == SHINING_AURA
                    && column == 2 && effectGfx == 3941 && targetTo == 12
                    && nativeId == 4 && "$1977".equals(nameId)
                    && armorClass == -8 && meleeHit == 0 && rangedHit == 0
                    && magicResistance == 0 && meleeDamage == 0;
            boolean brave = skillId == BRAVE_AURA
                    && column == 4 && effectGfx == 3942 && targetTo == 8
                    && nativeId == 16 && "$3178".equals(nameId)
                    && armorClass == 0 && meleeHit == 0 && rangedHit == 0
                    && magicResistance == 0 && meleeDamage == 5;
            if (skillId != (row << 3) + column + 1
                    || (!glowing && !shining && !brave)
                    || row < 0 || row > 0xFF || column < 0 || column > 7
                    || skillLevel != 15 || actionId < 0 || actionId > 0xFF
                    || effectGfx <= 0 || effectGfx > 0xFFFF
                    || mpCost != 40 || hpCost != 0 || reuseDelayMillis != 0
                    || durationSeconds != 640 || maximumRange != 0 || area != -1
                    || attribute != 0 || sourceType != 2 || nameId == null
                    || !nameId.matches("\\$[0-9]+")
                    || !counterMagicBypass || key == null || key.isBlank()) {
                throw new IllegalArgumentException("invalid native royal-aura metadata");
            }
        }

        /** Royal aura skills are outside circle reductions; original-INT follows the floor. */
        int effectiveMpCost(int originalReduction) {
            if (originalReduction < 0 || originalReduction > 2) {
                throw new IllegalArgumentException("invalid royal-aura MP inputs");
            }
            return Math.max(0, Math.max(ResourceDatabase.value(this.mpCost, this.skillId, "RoyalAuraSpell", "mpCost"), 1) - originalReduction);
        }
    }

    record CallClanSpell(
            int skillId, int row, int column, int skillLevel, int actionId,
            int mpCost, int hpCost, int reuseDelayMillis, int targetTo,
            int maximumRange, int area, int attribute, int sourceType,
            int nativeId, String nameId, int effectGfx, int questionId,
            int failureMessage, boolean counterMagicBypass, String key) {
        public int actionId() {
            return ResourceDatabase.value(this.actionId, this.skillId, "CallClanSpell", "actionId");
        }

        public int hpCost() {
            return ResourceDatabase.value(this.hpCost, this.skillId, "CallClanSpell", "hpCost");
        }

        public int mpCost() {
            return ResourceDatabase.value(this.mpCost, this.skillId, "CallClanSpell", "mpCost");
        }

        public int reuseDelayMillis() {
            return ResourceDatabase.value(this.reuseDelayMillis, this.skillId, "CallClanSpell", "reuseDelayMillis");
        }

        CallClanSpell {
            if (skillId != CALL_CLAN || skillId != (row << 3) + column + 1
                    || row != 14 || column != 3 || skillLevel != 15
                    || actionId != 19 || mpCost != 30 || hpCost != 0
                    || reuseDelayMillis != 0 || targetTo != 0
                    || maximumRange != 0 || area != 0 || attribute != 0
                    || sourceType != 2 || nativeId != 8
                    || nameId == null || !nameId.isEmpty() || effectGfx != 0
                    || questionId != BattleCallClan.QUESTION_ID
                    || failureMessage != 280 || !counterMagicBypass
                    || key == null || key.isBlank()) {
                throw new IllegalArgumentException("invalid native Call Clan metadata");
            }
        }

        /** Royal circle-15 skill: only retained original-stat reduction applies. */
        int effectiveMpCost(int originalReduction) {
            if (originalReduction < 0 || originalReduction > 2) {
                throw new IllegalArgumentException("invalid Call Clan MP inputs");
            }
            return Math.max(0, Math.max(ResourceDatabase.value(this.mpCost, this.skillId, "CallClanSpell", "mpCost"), 1) - originalReduction);
        }
    }

    record RunClanSpell(
            int skillId, int row, int column, int skillLevel, int actionId,
            int mpCost, int hpCost, int reuseDelayMillis, int targetTo,
            int maximumRange, int area, int attribute, int sourceType,
            int nativeId, String nameId, int effectGfx,
            int failureMessage, boolean counterMagicBypass, String key) {
        public int actionId() {
            return ResourceDatabase.value(this.actionId, this.skillId, "RunClanSpell", "actionId");
        }

        public int hpCost() {
            return ResourceDatabase.value(this.hpCost, this.skillId, "RunClanSpell", "hpCost");
        }

        public int mpCost() {
            return ResourceDatabase.value(this.mpCost, this.skillId, "RunClanSpell", "mpCost");
        }

        public int reuseDelayMillis() {
            return ResourceDatabase.value(this.reuseDelayMillis, this.skillId, "RunClanSpell", "reuseDelayMillis");
        }

        RunClanSpell {
            if (skillId != RUN_CLAN || skillId != (row << 3) + column + 1
                    || row != 14 || column != 5 || skillLevel != 15
                    || actionId != 19 || mpCost != 30 || hpCost != 0
                    || reuseDelayMillis != 0 || targetTo != 0
                    || maximumRange != 0 || area != 0 || attribute != 0
                    || sourceType != 2 || nativeId != 32
                    || nameId == null || !nameId.isEmpty() || effectGfx != 0
                    || failureMessage != 280 || !counterMagicBypass
                    || key == null || key.isBlank()) {
                throw new IllegalArgumentException("invalid native Run Clan metadata");
            }
        }

        /** Royal circle-15 skill: only retained original-stat reduction applies. */
        int effectiveMpCost(int originalReduction) {
            if (originalReduction < 0 || originalReduction > 2) {
                throw new IllegalArgumentException("invalid Run Clan MP inputs");
            }
            return Math.max(0, Math.max(ResourceDatabase.value(this.mpCost, this.skillId, "RunClanSpell", "mpCost"), 1) - originalReduction);
        }
    }
}
