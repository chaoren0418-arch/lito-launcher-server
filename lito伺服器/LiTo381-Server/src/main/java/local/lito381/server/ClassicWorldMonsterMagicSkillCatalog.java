package local.lito381.server;

import java.util.List;
import java.util.stream.Stream;

/** Exact active-magic rows used by the selected-source map-4 monster population. */
final class ClassicWorldMonsterMagicSkillCatalog {
    static final String RETAINED_SQL_SHA256 =
            "DA185BCA20CC254CB175EA2C367A2F5270514F4957B3AC99343C6101A2593766";
    static final String RETAINED_MOB_SKILL_USE_SHA256 =
            "7167D0899729DBBA7A12E1EDCEC5D8DDE0570B94DA7539D06A1417C9465C90B7";
    static final int MAP4_MAGIC_NPC_TYPES = 3;
    static final int MAP4_MAGIC_POPULATION = 217;
    static final int CKEW_MAGIC_NPC_TYPES = 8;
    static final int CKEW_MAGIC_ROWS = 16;

    enum Targeting {
        DIRECTIONAL,
        TARGET_CENTERED,
        CASTER_CENTERED
    }

    /**
     * One retained {@code mobskill} Type=2 row plus the NPC animation clocks
     * needed by {@code L1MobSkillUse.magicAttack}.
     */
    record Definition(
            int npcId, int actNo, int triggerChance, int triggerRange,
            int leverage, int skillId, int attackMagicSpeedMillis,
            int supportMagicSpeedMillis, Targeting targeting,
            int mpCost, int reuseDelayMillis, int actionId, int effectGfx,
            int maximumRange, int areaRadius, int baseDamage,
            int diceCount, int dieSides, int attribute) {
        Definition {
            if (npcId <= 0 || actNo < 0 || triggerChance <= 0
                    || triggerChance > 100 || triggerRange == 0
                    || leverage < 0 || skillId <= 0
                    || attackMagicSpeedMillis <= 0
                    || supportMagicSpeedMillis <= 0 || targeting == null
                    || mpCost < 0 || reuseDelayMillis < 0
                    || actionId < 0 || actionId > 0xFF
                    || effectGfx <= 0 || effectGfx > 0xFFFF
                    || maximumRange < 0 || areaRadius < 0 || baseDamage < 0
                    || diceCount <= 0 || dieSides <= 0
                    || !switch (attribute) {
                        case 0, 1, 2, 4, 8, 16 -> true;
                        default -> false;
                    }) {
                throw new IllegalArgumentException("invalid classic mob magic row");
            }
            boolean validTargeting = switch (targeting) {
                case DIRECTIONAL -> maximumRange > 0 && areaRadius == 0;
                case TARGET_CENTERED -> maximumRange > 0 && areaRadius > 0;
                case CASTER_CENTERED -> maximumRange == 0 && areaRadius > 0;
            };
            if (!validTargeting) {
                throw new IllegalArgumentException(
                        "classic mob magic targeting disagrees with skill row " + skillId);
            }
        }

        int effectiveLeverage() {
            return leverage > 0 ? leverage : 10;
        }

        int actionDelayMillis() {
            return targeting != Targeting.CASTER_CENTERED
                    ? attackMagicSpeedMillis : supportMagicSpeedMillis;
        }

        boolean triggerDistance(int distance) {
            if (distance < 0) throw new IllegalArgumentException("negative mob target distance");
            return triggerRange < 0
                    ? distance <= Math.abs(triggerRange)
                    : distance >= triggerRange;
        }

        boolean triggerRoll(int oneBasedRoll) {
            if (oneBasedRoll < 1 || oneBasedRoll > 100) {
                throw new IllegalArgumentException("mob skill roll outside percentile range");
            }
            // Retained L1MobSkillUse uses the strict comparison chance < TriRnd.
            return oneBasedRoll < triggerChance;
        }
    }

    private static final List<Definition> MAP4_DEFINITIONS = List.of(
            // mobskill(45049,0): TriRnd100, TriRange-6, leverage5, Ice Cone.
            // npc(45049): atk_magic_speed/sub_magic_speed 1040/1040.
            standard(45049, 0, 100, -6, 5,
                    BattleSpellCatalog.CONE_OF_COLD, 1040, 1040,
                    Targeting.DIRECTIONAL),
            // mobskill(45224,0): default leverage10, Ice Cone.
            // npc(45224): atk_magic_speed/sub_magic_speed 1920/1920.
            standard(45224, 0, 100, -6, 0,
                    BattleSpellCatalog.CONE_OF_COLD, 1920, 1920,
                    Targeting.DIRECTIONAL),
            // mobskill(45265,0): TriRnd80, TriRange-4, leverage15, Tornado.
            // npc(45265): atk_magic_speed/sub_magic_speed 1920/1920.
            standard(45265, 0, 80, -4, 15,
                    BattleSpellCatalog.TORNADO, 1920, 1920,
                    Targeting.CASTER_CENTERED));

    private static final List<Definition> CKEW_DEFINITIONS =
            Stream.concat(
                    Stream.of(87007, 87008, 87009, 87010)
                            .flatMap(npcId -> fighterSkills(npcId).stream()),
                    Stream.of(87015, 87016, 87017, 87018)
                            .flatMap(npcId -> chinoSkills(npcId).stream()))
                    .toList();

    private ClassicWorldMonsterMagicSkillCatalog() {}

    static List<Definition> definitions() {
        return MAP4_DEFINITIONS;
    }

    static List<Definition> ckewDefinitions() {
        return CKEW_DEFINITIONS;
    }

    static List<Definition> forNpcAll(int npcId) {
        return Stream.concat(MAP4_DEFINITIONS.stream(), CKEW_DEFINITIONS.stream())
                .filter(definition -> definition.npcId() == npcId)
                .toList();
    }

    static Definition forNpc(int npcId) {
        List<Definition> definitions = forNpcAll(npcId);
        return definitions.isEmpty() ? null : definitions.get(0);
    }

    private static Definition standard(
            int npcId, int actNo, int triggerChance, int triggerRange,
            int leverage, int skillId, int attackMagicSpeedMillis,
            int supportMagicSpeedMillis, Targeting expectedTargeting) {
        BattleSpellCatalog.AttackSpell attack =
                BattleSpellCatalog.attackSpell(skillId);
        if (attack != null) {
            if (expectedTargeting != Targeting.DIRECTIONAL) {
                throw new IllegalArgumentException("standard attack targeting mismatch " + skillId);
            }
            return new Definition(npcId, actNo, triggerChance, triggerRange,
                    leverage, skillId, attackMagicSpeedMillis,
                    supportMagicSpeedMillis, expectedTargeting,
                    attack.mpCost(), attack.reuseDelayMillis(), attack.actionId(),
                    attack.effectGfx(), attack.maximumRange(), 0,
                    attack.baseDamage(), attack.diceCount(), attack.dieSides(),
                    attack.attribute());
        }
        BattleSpellCatalog.AreaAttackSpell area =
                BattleSpellCatalog.areaAttackSpell(skillId);
        if (area == null) {
            throw new IllegalArgumentException("missing standard attack spell " + skillId);
        }
        Targeting actualTargeting = area.maximumRange() == 0
                ? Targeting.CASTER_CENTERED : Targeting.TARGET_CENTERED;
        if (expectedTargeting != actualTargeting) {
            throw new IllegalArgumentException("standard area targeting mismatch " + skillId);
        }
        return new Definition(npcId, actNo, triggerChance, triggerRange,
                leverage, skillId, attackMagicSpeedMillis,
                supportMagicSpeedMillis, expectedTargeting,
                area.mpCost(), area.reuseDelayMillis(), area.actionId(),
                area.effectGfx(), area.maximumRange(), area.areaRadius(),
                area.baseDamage(), area.diceCount(), area.dieSides(),
                area.attribute());
    }

    private static List<Definition> fighterSkills(int npcId) {
        return List.of(
                // Exact retained skill10038: target none, MP5, reuse0,
                // area4, 100+10d10 earth damage, action18, gfx2510.
                new Definition(npcId, 0, 30, -6, 10, 10038,
                        880, 1760, Targeting.CASTER_CENTERED,
                        5, 0, 18, 2510, 0, 4,
                        100, 10, 10, BattleSpellCatalog.ATTRIBUTE_EARTH),
                // Same retained DB as this mobskill: row25 reuse is1000 ms,
                // not the selected live DB's later 500 ms variant.
                new Definition(npcId, 1, 30, -6, 50,
                        BattleSpellCatalog.BURNING_FIREBALL,
                        880, 1760, Targeting.TARGET_CENTERED,
                        20, 1000, 18, 171, 6, 3,
                        0, 5, 9, BattleSpellCatalog.ATTRIBUTE_FIRE));
    }

    private static List<Definition> chinoSkills(int npcId) {
        return List.of(
                // Exact retained skill10010: target none, MP5, reuse0,
                // area3, 100+10d10 neutral damage, action18, gfx2510.
                new Definition(npcId, 0, 50, -15, 10, 10010,
                        1600, 1240, Targeting.CASTER_CENTERED,
                        5, 0, 18, 2510, 0, 3,
                        100, 10, 10, 0),
                // Exact retained skill10061: target attack, MP5, reuse0,
                // range8/area3, 100+10d10 neutral damage, action18, gfx2546.
                new Definition(npcId, 1, 50, -8, 10, 10061,
                        1600, 1240, Targeting.TARGET_CENTERED,
                        5, 0, 18, 2546, 8, 3,
                        100, 10, 10, 0));
    }
}
