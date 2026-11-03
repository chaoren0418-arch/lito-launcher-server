package local.lito381.server;

/**
 * Hash-locked selected-source contract for the Elf level-50 Dspy escort.
 *
 * <p>The static quest actor is NPC 80012 on map 400.  Its retained executor
 * rolls {@code Random.nextInt(100) <= 2} from every follower no-target pass,
 * emits location effect 3992, and links one dynamically spawned NPC 80011 to
 * the follower.  NPC 80011 has no selected mobskill row, so it uses the
 * ordinary monster movement and physical-attack path.</p>
 */
final class ClassicWorldDspyCatalog {
    static final String DSPY_SOURCE_SHA256 =
            "54F2E88A5DA8EFE086EDAA325824CDA3912A23F57FD03544BB63625D7D27865C";
    static final String ELF_LV50_SOURCE_SHA256 =
            "F6084A4AECEA3149AB6402B90B72000DABB18317FEC9438E96D0A38ECD1CCFD1";
    static final String FOLLOWER_SOURCE_SHA256 =
            "C5629C56762D05A98F7C15C5284B7132F3EE7AB9C00492582AB25E4F774F32A2";
    static final String FOLLOWER_PACKET_SOURCE_SHA256 =
            "FB07F668B6CA254C17FE2F93707378D1A09CB7C0FCD0377A78FE88D83D24AD3B";

    static final int MAP_ID = 400;
    static final int DSPY_NPC_ID = 80012;
    static final int DSPY_GFX_ID = 2779;
    static final String DSPY_NAME_ID = "$5800";
    static final int DSPY_MAX_HP = 2500;
    static final int DSPY_ARMOR_CLASS = -15;
    static final int DSPY_PASSISPEED_MILLIS = 960;

    static final int ASSASSIN_NPC_ID = 80011;
    static final int ASSASSIN_APPEARANCE_GFX = 3992;
    static final int ASSASSIN_RADIUS = 4;
    static final int ASSASSIN_ROLL_BOUND = 100;
    static final int ASSASSIN_ROLL_MAX_INCLUSIVE = 2;

    static final int QUEST_ID = 11603;
    static final int COMPLETION_EVENT_ID = 40000261;
    static final long REWARD_ITEM_ID = 49163;
    static final String DEFAULT_HTML = "dspy1";
    static final String START_HTML = "dspy2";
    static final String OWNER_HTML = "dspy3";

    static final int ARRIVAL_MIN_X = 32557;
    static final int ARRIVAL_MAX_X = 32576;
    static final int ARRIVAL_MIN_Y = 32656;
    static final int ARRIVAL_MAX_Y = 32687;

    static final ClassicWorldMonsterCatalog.MonsterTemplate ASSASSIN =
            new ClassicWorldMonsterCatalog.MonsterTemplate(
                    ASSASSIN_NPC_ID, "$5801", "L1Monster", 3989,
                    53, 1800, 300, -58,
                    55, 21, 11, 21, 18, 70, 1, 0,
                    "small", 0, 2, 720, 1040, 0, 3,
                    0, 0, true, 0, false, false, false, 14, false);

    private static final ClassicWorldMonsterCatalog.MonsterTemplate DSPY_COMBAT =
            new ClassicWorldMonsterCatalog.MonsterTemplate(
                    DSPY_NPC_ID, DSPY_NAME_ID, "L1Monster", DSPY_GFX_ID,
                    1, DSPY_MAX_HP, 1, DSPY_ARMOR_CLASS,
                    0, 0, 0, 0, 0, 0, 1, 0,
                    "small", 0, 0, DSPY_PASSISPEED_MILLIS, 0, 0, 0,
                    0, 0, false, 0, false, false, false, 0, false);
    static final SourceNpcPhysicalStats DSPY_PHYSICAL =
            new SourceNpcPhysicalStats(
                    DSPY_NPC_ID, DSPY_ARMOR_CLASS, false, 0, false,
                    0, 0, 0, false, 0, false, false,
                    1, 0, 0, false, 0, 0);

    private ClassicWorldDspyCatalog() {}

    static boolean arrival(int mapId, int x, int y) {
        return mapId == MAP_ID
                && x >= ARRIVAL_MIN_X && x <= ARRIVAL_MAX_X
                && y >= ARRIVAL_MIN_Y && y <= ARRIVAL_MAX_Y;
    }

    static boolean sourceNpc(ClassicWorldNpcCatalog.StaticNpc npc) {
        return npc != null && npc.mapId() == MAP_ID
                && npc.npcId() == DSPY_NPC_ID;
    }

    static ClassicWorldMonsterPopulation.Snapshot combatProjection(
            ClassicWorldDspyEscorts.Snapshot follower) {
        if (follower == null || follower.mapId() != MAP_ID) return null;
        ClassicWorldMonsterCatalog.SpawnDefinition definition =
                new ClassicWorldMonsterCatalog.SpawnDefinition(
                        MAP_ID, DSPY_NPC_ID, "Dspy-follower", 1, 0,
                        follower.x(), follower.y(), 0, 0, 0, 0, 0, 0,
                        follower.heading(), 0, 0, false, 0, false, 0,
                        DSPY_COMBAT);
        return new ClassicWorldMonsterPopulation.Snapshot(
                MAP_ID, DSPY_NPC_ID, 1, follower.objectId(), DSPY_NPC_ID,
                follower.x(), follower.y(), follower.heading(),
                follower.currentHp(), follower.maximumHp(), 1, 1,
                follower.alive(), true, definition);
    }
}
