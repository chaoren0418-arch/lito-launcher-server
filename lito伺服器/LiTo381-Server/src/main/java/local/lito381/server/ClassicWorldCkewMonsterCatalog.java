package local.lito381.server;

import java.util.List;
import java.util.stream.Stream;

/** Retained quest122 per-show actors plus the corroborating classic 3.80 spawn rows. */
final class ClassicWorldCkewMonsterCatalog {
    static final int DEFINITION_COUNT = 33;
    static final int POPULATION = 195;
    static final String RETAINED_DATABASE_SHA256 =
            "3BF9B04B6E1D788E361534A79F3AA49CEEEF84FAE5CE04471EDF1EC9764CA423";
    static final String UPSTREAM_SPAWNLIST_SHA256 =
            "4656007EB8E011C02319FBDCC57F2A89C696AE992142D9565E777C8850191AFB";
    static final String UPSTREAM_NPC_SHA256 =
            "E14D2A0C074A842C0ACBBB4CCC066D6C555AC08284E403CB89C14E977668896E";
    static final String UPSTREAM_MOBSKILL_SHA256 =
            "9C781E5F79052A34FC92C8FB8967AB958B129097850BB096D10CA548B2C7C311";

    private static final ClassicWorldMonsterCatalog.MonsterTemplate GUARDIAN =
            template(87004, "$5770", 4615, 25, 1274, 318, -20,
                    20, 14, 16, 10, 12, 65, -25, "small", 2, 2,
                    960, 1200, 0, 4);
    private static final ClassicWorldMonsterCatalog.MonsterTemplate WATCHER =
            template(87005, "$5751", 1123, 30, 3000, 500, -70,
                    18, 14, 16, 10, 12, 90, -30, "small", 1, 1,
                    960, 1120, 0, 4);
    private static final ClassicWorldMonsterCatalog.MonsterTemplate GLADIATOR =
            template(87006, "$5748", 2308, 35, 450, 0, -20,
                    20, 14, 16, 10, 12, 50, -30, "large", 2, 1,
                    960, 1240, 0, 4);
    private static final ClassicWorldMonsterCatalog.MonsterTemplate ABANDONED_FEMALE =
            template(87012, "$5752", 5412, 30, 200, 120, -30,
                    18, 14, 16, 10, 12, 10, -50, "small", 8, 8,
                    640, 1280, 5415, 1);
    private static final ClassicWorldMonsterCatalog.MonsterTemplate ABANDONED_MALE =
            template(87013, "$5752", 5484, 30, 200, 30, -30,
                    18, 14, 16, 10, 12, 10, -50, "small", 8, 2,
                    960, 720, 0, 1);
    private static final ClassicWorldMonsterCatalog.MonsterTemplate SAGE =
            template(87014, "$5749", 1576, 40, 550, 100, -35,
                    20, 14, 16, 10, 12, 50, -100, "small", 2, 3,
                    640, 2280, 0, 4);

    private static final ClassicWorldMonsterCatalog.MonsterTemplate ALTAR =
            template(87000, "$5755", 6892, 40, 5000, 50, -20,
                    18, 14, 16, 10, 12, 60, -5000, "large", 0, 15,
                    0, 1440, 0, 0, false);
    private static final ClassicWorldMonsterCatalog.MonsterTemplate FIRST_GUARDIAN =
            template(87001, "$5770", 2513, 25, 300, 0, -20,
                    18, 18, 20, 15, 17, 40, -39, "small", 0, 2,
                    480, 1120, 0, 0);
    private static final ClassicWorldMonsterCatalog.MonsterTemplate WILL_SOLDIER =
            template(87002, "$5753", 4664, 30, 400, 50, -21,
                    18, 14, 16, 10, 12, 30, -100, "small", 0, 2,
                    640, 1160, 0, 0);
    private static final ClassicWorldMonsterCatalog.MonsterTemplate CKEW_SOLDIER =
            template(87003, "$5747", 1578, 31, 400, 50, -25,
                    18, 14, 16, 10, 12, 90, -35, "small", 2, 2,
                    1440, 1920, 0, 0);
    private static final ClassicWorldMonsterCatalog.MonsterTemplate ELF_FIGHTER =
            fighter(87007, 1200, 4);
    private static final ClassicWorldMonsterCatalog.MonsterTemplate WIZARD_FIGHTER =
            fighter(87008, 1000, 1);
    private static final ClassicWorldMonsterCatalog.MonsterTemplate ROYAL_FIGHTER =
            fighter(87009, 1000, 0);
    private static final ClassicWorldMonsterCatalog.MonsterTemplate KNIGHT_FIGHTER =
            fighter(87010, 1600, 2);
    private static final ClassicWorldMonsterCatalog.MonsterTemplate THIRD_GUARDIAN =
            template(87011, "$5770", 2501, 25, 300, 15, -20,
                    16, 14, 16, 10, 12, 30, -14, "small", 2, 1,
                    480, 1160, 0, 0);

    /** Runtime-only flute summons; they are not part of the admission population. */
    private static final List<ClassicWorldMonsterCatalog.MonsterTemplate>
            FLUTE_SUMMON_TEMPLATES = List.of(
                    chino(87015, 50, 1800, 4),
                    chino(87016, 30, 3000, 1),
                    chino(87017, 30, 3000, 0),
                    chino(87018, 30, 3000, 2));

    private static final List<ClassicWorldMonsterCatalog.SpawnDefinition>
            RETAINED_DEFINITIONS = List.of(
                    spawn(50143, "被遺棄的肉身(女)(再生聖殿 3樓)", 10,
                            32727, 32740, 32774, 32796, 5, ABANDONED_FEMALE),
                    spawn(50144, "被遺棄的肉身(男)(再生聖殿 3樓)", 10,
                            32727, 32740, 32774, 32796, 5, ABANDONED_MALE),
                    spawn(50145, "奇諾之賢者(再生聖殿 3樓)", 10,
                            32727, 32740, 32774, 32796, 5, SAGE),
                    spawn(50150, "奇諾之監視者 (再生聖殿 2樓)", 5,
                            32878, 32816, 32906, 32822, 0, WATCHER),
                    spawn(50151, "奇諾之鬥士 (再生聖殿 2樓)", 8,
                            32921, 32731, 32937, 32774, 0, GLADIATOR),
                    spawn(50152, "高潔意志的守護者 (再生聖殿 2樓)", 8,
                            32921, 32731, 32937, 32774, 0, GUARDIAN),
                    spawn(50153, "奇諾之監視者 (再生聖殿 2樓)", 3,
                            32926, 32777, 32930, 32805, 0, WATCHER),
                    spawn(50154, "奇諾之鬥士 (再生聖殿 2樓)", 6,
                            32910, 32806, 32939, 32834, 0, GLADIATOR),
                    spawn(50155, "高潔意志的守護者 (再生聖殿 2樓)", 6,
                            32910, 32806, 32939, 32834, 0, GUARDIAN),
                    spawn(50156, "奇諾之監視者(再生聖殿 2樓)", 15,
                            32848, 32791, 32901, 32928, 0, WATCHER),
                    spawn(50157, "奇諾之鬥士(再生聖殿 2樓)", 15,
                            32848, 32791, 32901, 32928, 0, GLADIATOR),
                    spawn(50158, "高潔意志的守護者(再生聖殿 2樓)", 15,
                            32848, 32791, 32901, 32928, 0, GUARDIAN));

    /*
     * Exact old spawn rows 400343/400347/400351..400364/400415/400420/
     * 400425/400430/400433. The four class fighters formerly used maps
     * 2000..2003; retained quest122 owns one map-2000 show containing all four
     * classes, so only those four class-isolated rows are merged into the show.
     */
    private static final List<ClassicWorldMonsterCatalog.SpawnDefinition>
            SPECIAL_DEFINITIONS = List.of(
                    fixedSpawn(400343, "再生之祭壇", 32816, 32773, 0,
                            15, 30, 1, ALTAR),
                    sourceSpawn(400347, "高潔意志的守護者", 10,
                            32750, 32873, 32793, 32914, 0,
                            15, 30, 0, FIRST_GUARDIAN),
                    sourceSpawn(400351, "高潔的意志戰士", 1,
                            32807, 32881, 32826, 32909, 0, WILL_SOLDIER),
                    sourceSpawn(400352, "高潔的意志戰士", 1,
                            32787, 32925, 32813, 32942, 0, WILL_SOLDIER),
                    sourceSpawn(400353, "高潔的意志戰士", 1,
                            32756, 32931, 32774, 32942, 0, WILL_SOLDIER),
                    sourceSpawn(400354, "高潔的意志戰士", 1,
                            32721, 32923, 32741, 32942, 0, WILL_SOLDIER),
                    sourceSpawn(400355, "高潔的意志戰士", 1,
                            32720, 32889, 32733, 32908, 0, WILL_SOLDIER),
                    sourceSpawn(400356, "高潔的意志戰士", 1,
                            32720, 32849, 32743, 32876, 0, WILL_SOLDIER),
                    sourceSpawn(400357, "高潔的意志戰士", 1,
                            32755, 32840, 32784, 32858, 0, WILL_SOLDIER),
                    sourceSpawn(400358, "奇諾之戰士", 8,
                            32807, 32881, 32826, 32909, 0, CKEW_SOLDIER),
                    sourceSpawn(400359, "奇諾之戰士", 8,
                            32787, 32925, 32813, 32942, 0, CKEW_SOLDIER),
                    sourceSpawn(400360, "奇諾之戰士", 8,
                            32756, 32931, 32774, 32942, 0, CKEW_SOLDIER),
                    sourceSpawn(400361, "奇諾之戰士", 8,
                            32721, 32923, 32741, 32942, 0, CKEW_SOLDIER),
                    sourceSpawn(400362, "奇諾之戰士", 8,
                            32720, 32889, 32733, 32908, 0, CKEW_SOLDIER),
                    sourceSpawn(400363, "奇諾之戰士", 8,
                            32720, 32849, 32743, 32876, 0, CKEW_SOLDIER),
                    sourceSpawn(400364, "奇諾之戰士", 8,
                            32755, 32840, 32784, 32858, 0, CKEW_SOLDIER),
                    sourceSpawn(400415, "高潔的意志鬥士 (王族)", 1,
                            32863, 32890, 32901, 32929, 0, ROYAL_FIGHTER),
                    sourceSpawn(400420, "高潔的意志鬥士 (騎士)", 1,
                            32863, 32890, 32901, 32929, 0, KNIGHT_FIGHTER),
                    sourceSpawn(400425, "高潔的意志鬥士 (妖精)", 1,
                            32863, 32890, 32901, 32929, 0, ELF_FIGHTER),
                    sourceSpawn(400430, "高潔的意志鬥士 (法師)", 1,
                            32863, 32890, 32901, 32929, 0, WIZARD_FIGHTER),
                    sourceSpawn(400433, "高潔意志的守護者 (再生聖殿 3樓)", 6,
                            32727, 32740, 32775, 32804, 0, THIRD_GUARDIAN));

    private static final List<ClassicWorldMonsterCatalog.SpawnDefinition> DEFINITIONS =
            Stream.concat(RETAINED_DEFINITIONS.stream(), SPECIAL_DEFINITIONS.stream())
                    .toList();

    private static final List<ClassicWorldMonsterPhysicalSkillCatalog.Definition>
            PHYSICAL_SKILLS = List.of(
                    new ClassicWorldMonsterPhysicalSkillCatalog.Definition(
                            87004, 0, 30, -3, 0,
                            3, 0, 0, 30, 0, 18),
                    new ClassicWorldMonsterPhysicalSkillCatalog.Definition(
                            87004, 1, 50, -1, 0,
                            1, 0, 0, 10, 0, 30),
                    new ClassicWorldMonsterPhysicalSkillCatalog.Definition(
                            87006, 1, 50, -1, 0,
                            1, 0, 0, 10, 0, 30),
                    new ClassicWorldMonsterPhysicalSkillCatalog.Definition(
                            87014, 0, 50, -3, 0,
                            3, 0, 0, 20, 0, 30),
                    new ClassicWorldMonsterPhysicalSkillCatalog.Definition(
                            87007, 2, 50, -3, 0,
                            3, 0, 0, 20, 0, 19),
                    new ClassicWorldMonsterPhysicalSkillCatalog.Definition(
                            87008, 2, 50, -3, 0,
                            3, 0, 0, 20, 0, 19),
                    new ClassicWorldMonsterPhysicalSkillCatalog.Definition(
                            87009, 2, 50, -3, 0,
                            3, 0, 0, 20, 0, 19),
                    new ClassicWorldMonsterPhysicalSkillCatalog.Definition(
                            87010, 2, 50, -3, 0,
                            3, 0, 0, 20, 0, 19),
                    new ClassicWorldMonsterPhysicalSkillCatalog.Definition(
                            87015, 2, 100, -2, 0,
                            2, 0, 0, 50, 0, 0),
                    new ClassicWorldMonsterPhysicalSkillCatalog.Definition(
                            87016, 2, 100, -2, 0,
                            2, 0, 0, 50, 0, 0),
                    new ClassicWorldMonsterPhysicalSkillCatalog.Definition(
                            87017, 2, 100, -2, 0,
                            2, 0, 0, 50, 0, 0),
                    new ClassicWorldMonsterPhysicalSkillCatalog.Definition(
                            87018, 2, 100, -2, 0,
                            2, 0, 0, 50, 0, 0));

    private ClassicWorldCkewMonsterCatalog() {}

    static List<ClassicWorldMonsterCatalog.SpawnDefinition> definitions() {
        return DEFINITIONS;
    }

    static List<ClassicWorldMonsterPhysicalSkillCatalog.Definition>
            physicalSkills(int npcId) {
        return PHYSICAL_SKILLS.stream()
                .filter(definition -> definition.npcId() == npcId)
                .toList();
    }

    static ClassicWorldMonsterCatalog.MonsterTemplate templateForNpc(int npcId) {
        return Stream.concat(
                        DEFINITIONS.stream().map(
                                ClassicWorldMonsterCatalog.SpawnDefinition::template),
                        FLUTE_SUMMON_TEMPLATES.stream())
                .filter(template -> template.npcId() == npcId)
                .findFirst().orElse(null);
    }

    private static ClassicWorldMonsterCatalog.SpawnDefinition spawn(
            int spawnId, String location, int count,
            int x1, int y1, int x2, int y2, int heading,
            ClassicWorldMonsterCatalog.MonsterTemplate template) {
        return new ClassicWorldMonsterCatalog.SpawnDefinition(
                ClassicWorldCkewCatalog.MAP_ID, spawnId, location, count, 0,
                0, 0, 0, 0, x1, y1, x2, y2, heading,
                30, 30, false, 0, false, 0, template);
    }

    private static ClassicWorldMonsterCatalog.SpawnDefinition sourceSpawn(
            int spawnId, String location, int count,
            int x1, int y1, int x2, int y2, int heading,
            ClassicWorldMonsterCatalog.MonsterTemplate template) {
        return sourceSpawn(spawnId, location, count, x1, y1, x2, y2, heading,
                0, 0, 0, template);
    }

    private static ClassicWorldMonsterCatalog.SpawnDefinition sourceSpawn(
            int spawnId, String location, int count,
            int x1, int y1, int x2, int y2, int heading,
            int minimumRespawn, int maximumRespawn, int movementDistance,
            ClassicWorldMonsterCatalog.MonsterTemplate template) {
        return new ClassicWorldMonsterCatalog.SpawnDefinition(
                ClassicWorldCkewCatalog.MAP_ID, spawnId, location, count, 0,
                0, 0, 5, 5, x1, y1, x2, y2, heading,
                minimumRespawn, maximumRespawn, false, movementDistance,
                false, 0, template);
    }

    private static ClassicWorldMonsterCatalog.SpawnDefinition fixedSpawn(
            int spawnId, String location, int x, int y, int heading,
            int minimumRespawn, int maximumRespawn, int movementDistance,
            ClassicWorldMonsterCatalog.MonsterTemplate template) {
        return new ClassicWorldMonsterCatalog.SpawnDefinition(
                ClassicWorldCkewCatalog.MAP_ID, spawnId, location, 1, 0,
                x, y, 0, 0, 0, 0, 0, 0, heading,
                minimumRespawn, maximumRespawn, false, movementDistance,
                false, 0, template);
    }

    private static ClassicWorldMonsterCatalog.MonsterTemplate fighter(
            int npcId, int hp, int weakAttribute) {
        return template(npcId, "$5754", 4034, 45, hp, 200, -35,
                25, 14, 16, 10, 12, 90, -100, "large", weakAttribute, 2,
                640, 720, 880, 0);
    }

    private static ClassicWorldMonsterCatalog.MonsterTemplate chino(
            int npcId, int level, int hp, int weakAttribute) {
        return template(npcId, "$5746", 2495, level, hp, 500, -40,
                25, 14, 16, 10, 12, 90, -100, "large", weakAttribute, 2,
                1920, 1440, 1600, 0);
    }

    private static ClassicWorldMonsterCatalog.MonsterTemplate template(
            int npcId, String nameId, int gfxId, int level, int hp, int mp,
            int armorClass, int strength, int constitution, int dexterity,
            int wisdom, int intelligence, int magicResistance, int lawful,
            String size, int weakAttribute, int ranged, int passispeed,
            int atkspeed, int bowActionId, int undead) {
        return template(npcId, nameId, gfxId, level, hp, mp, armorClass,
                strength, constitution, dexterity, wisdom, intelligence,
                magicResistance, lawful, size, weakAttribute, ranged,
                passispeed, atkspeed, bowActionId, undead, true);
    }

    private static ClassicWorldMonsterCatalog.MonsterTemplate template(
            int npcId, String nameId, int gfxId, int level, int hp, int mp,
            int armorClass, int strength, int constitution, int dexterity,
            int wisdom, int intelligence, int magicResistance, int lawful,
            String size, int weakAttribute, int ranged, int passispeed,
            int atkspeed, int bowActionId, int undead, boolean aggressive) {
        return new ClassicWorldMonsterCatalog.MonsterTemplate(
                npcId, nameId, "L1Monster", gfxId, level, hp, mp, armorClass,
                strength, constitution, dexterity, wisdom, intelligence,
                magicResistance, 1, lawful, size, weakAttribute, ranged,
                passispeed, atkspeed, bowActionId, undead,
                0, 0, aggressive, 0, false, false, false, 0, false);
    }
}
