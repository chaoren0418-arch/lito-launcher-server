package local.lito381.server;

import java.util.List;
import java.util.stream.Stream;

/** Retained quest117 map1011 monster templates and per-show population. */
final class ClassicWorldWaterDragonMonsterCatalog {
    static final int DEFINITION_COUNT = 5;
    static final int INITIAL_POPULATION = 83;
    static final String RETAINED_DATABASE_SHA256 =
            ClassicWorldWaterDragonCatalog.RETAINED_DATABASE_SHA256;

    private static final ClassicWorldMonsterCatalog.MonsterTemplate MEGA =
            template(70974, "$8418", 7745, 58, 2400, 150, -40,
                    25, 13, 13, 13, 18, 60, 3365, -100,
                    "large", 0, 2, 800, 640, 0, false);
    private static final ClassicWorldMonsterCatalog.MonsterTemplate FIS =
            template(70975, "$8419", 7764, 58, 2100, 2000, -40,
                    18, 13, 13, 13, 30, 90, 3365, -100,
                    "large", 0, 1, 640, 640, 960, false);
    private static final ClassicWorldMonsterCatalog.MonsterTemplate NERO_A =
            template(70976, "$8417", 7784, 62, 3700, 700, -50,
                    30, 13, 13, 13, 35, 80, 3845, -300,
                    "large", 0, 2, 1120, 960, 1400, false);
    private static final ClassicWorldMonsterCatalog.MonsterTemplate NERO_B =
            template(70977, "$8417", 7786, 64, 4700, 800, -50,
                    30, 13, 13, 13, 38, 80, 4097, -300,
                    "large", 0, 2, 1120, 960, 1400, false);
    private static final ClassicWorldMonsterCatalog.MonsterTemplate NERO_C =
            template(70978, "$8417", 7788, 66, 5700, 900, -50,
                    30, 13, 13, 13, 40, 80, 4357, -300,
                    "large", 0, 2, 1120, 960, 1400, false);

    private static final ClassicWorldMonsterCatalog.MonsterTemplate FAFURION_1 =
            template(71026, "$1426", 7864, 85, 40_000, 5_000, -120,
                    127, 20, 45, 20, 127, 180, 6000, -8000,
                    "large", 0, 3, 480, 1880, 2560, true);
    private static final ClassicWorldMonsterCatalog.MonsterTemplate FAFURION_2 =
            template(71027, "$1426", 7869, 95, 40_000, 5_000, -120,
                    127, 20, 40, 22, 127, 190, 12000, -8000,
                    "large", 0, 3, 480, 1880, 2560, true);
    private static final ClassicWorldMonsterCatalog.MonsterTemplate FAFURION_3 =
            template(71028, "$1426", 7870, 105, 40_000, 5_000, -120,
                    127, 25, 45, 25, 127, 200, 18000, -8000,
                    "large", 0, 3, 480, 1880, 2560, true);

    private static final List<ClassicWorldMonsterCatalog.SpawnDefinition> DEFINITIONS =
            List.of(
                    area(50207, "阿爾波斯 麥嘉-法利昂洞穴", 40,
                            32721, 32657, 32876, 32753, 5, MEGA),
                    area(50208, "阿爾波斯 菲斯-法利昂洞穴", 40,
                            32721, 32657, 32876, 32753, 5, FIS),
                    fixed(50209, "阿爾波斯 耐爾(A)-法利昂洞穴",
                            32739, 32716, 4, NERO_A),
                    fixed(50210, "阿爾波斯 耐爾(B)-法利昂洞穴",
                            32776, 32682, 6, NERO_B),
                    fixed(50211, "阿爾波斯 耐爾(C)-法利昂洞穴",
                            32861, 32712, 4, NERO_C));

    private ClassicWorldWaterDragonMonsterCatalog() {}

    static List<ClassicWorldMonsterCatalog.SpawnDefinition> definitions() {
        return DEFINITIONS;
    }

    static ClassicWorldMonsterCatalog.MonsterTemplate templateForNpc(int npcId) {
        return Stream.concat(DEFINITIONS.stream().map(
                        ClassicWorldMonsterCatalog.SpawnDefinition::template),
                Stream.of(FAFURION_1, FAFURION_2, FAFURION_3))
                .filter(template -> template.npcId() == npcId)
                .findFirst().orElse(null);
    }

    private static ClassicWorldMonsterCatalog.SpawnDefinition area(
            int spawnId, String location, int count,
            int x1, int y1, int x2, int y2, int heading,
            ClassicWorldMonsterCatalog.MonsterTemplate template) {
        return new ClassicWorldMonsterCatalog.SpawnDefinition(
                ClassicWorldWaterDragonCatalog.MAP_ID, spawnId, location,
                count, 0, 0, 0, 0, 0, x1, y1, x2, y2, heading,
                0, 0, false, 30, false, 0, template);
    }

    private static ClassicWorldMonsterCatalog.SpawnDefinition fixed(
            int spawnId, String location, int x, int y, int heading,
            ClassicWorldMonsterCatalog.MonsterTemplate template) {
        return new ClassicWorldMonsterCatalog.SpawnDefinition(
                ClassicWorldWaterDragonCatalog.MAP_ID, spawnId, location,
                1, 0, x, y, 0, 0, 0, 0, 0, 0, heading,
                30, 30, false, 30, false, 0, template);
    }

    private static ClassicWorldMonsterCatalog.MonsterTemplate template(
            int npcId, String nameId, int gfxId, int level, int hp, int mp,
            int armorClass, int strength, int constitution, int dexterity,
            int wisdom, int intelligence, int magicResistance, long experience,
            int lawful, String size, int weakAttribute, int ranged,
            int passispeed, int atkspeed, int bowActionId, boolean boss) {
        return new ClassicWorldMonsterCatalog.MonsterTemplate(
                npcId, nameId, "L1Monster", gfxId, level, hp, mp, armorClass,
                strength, constitution, dexterity, wisdom, intelligence,
                magicResistance, experience, lawful, size, weakAttribute,
                ranged, passispeed, atkspeed, bowActionId, 0,
                0, 0, true, 0, false, false, boss, 0, false);
    }
}
