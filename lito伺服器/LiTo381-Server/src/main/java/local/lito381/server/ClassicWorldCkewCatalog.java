package local.lito381.server;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Hash-pinned native contract for quest 122, the four-class level-50
 * regenerated-temple instance (map 2000).
 */
final class ClassicWorldCkewCatalog {
    static final int MAP_ID = 2000;
    static final int QUEST_ID = 122;
    /** Selected 380_db compatibility row for the class entrance stage. */
    static final int SELECTED_ENTRY_QUEST_ID = 12201;
    /** Selected 380_db compatibility row for altar proof and final report. */
    static final int SELECTED_COMPLETION_QUEST_ID = 12202;
    static final int QUEST_STARTED = 1;
    static final int QUEST_ENTERED = 2;
    static final int QUEST_COMPLETED = 3;
    static final int QUEST_ENDED = 255;
    static final int INSTANCE_SECONDS = 1_800;
    static final int FIRST_SHOW_ID = 100;
    static final int REQUIRED_CLASS_MASK = 15;

    static final String QUEST_SOURCE_SHA256 =
            "28C28A8CD4E2599617C8254A3F6A37E7BC1E6492DCDEC3510980D300ED6D5A07";
    static final String ENTRANCE_SOURCE_SHA256 =
            "23D7EA0E5793D4061F70FB4251204A3BF71AC61E28CBD37F9DA36D2579B122C7";
    static final String DICARDING_BRIDGE_SOURCE_SHA256 =
            "43848C0BBB7E757964D54A43753B2B0822646ACCE47FA7E13C382EE9B7B3482C";
    static final String SELECTED_DATABASE_SHA256 =
            "DA185BCA20CC254CB175EA2C367A2F5270514F4957B3AC99343C6101A2593766";
    static final String QUEST_TIMER_SOURCE_SHA256 =
            "954F0BCF757B40293BB73F7AB64A0C61474FCF8AE4B2BB4353552922F5C04E7C";
    static final String RETAINED_DATABASE_SHA256 =
            "3BF9B04B6E1D788E361534A79F3AA49CEEEF84FAE5CE04471EDF1EC9764CA423";
    static final String SECOND_FLOOR_KEY_SOURCE_SHA256 =
            "2A68CB06C5ECACF0F46ADB4AECC378647A08215C6ABA0BACEF2A2C598D8BEB87";
    static final String THIRD_FLOOR_KEY_SOURCE_SHA256 =
            "81CCCB60BE6B5205978CD473E07DA267B3C1E04D59D375C49A9A02EF3444206A";
    static final String WILL_OF_DESTRUCTION_SOURCE_SHA256 =
            "5CBA4A1C963FE5607A8DEEBEEE8803992DE8AF6A82C5E4231FC4D0E6BC91F229";
    static final String DESTRUCTION_MEDICINE_SOURCE_SHA256 =
            "D82C752A8BAE580F895E3550F6D8FDB875C6800AEC1F99586426FEDE2D3B9194";
    static final String MAGIC_FLUTE_SOURCE_SHA256 =
            "4F0EC883D437D10AF569776B49B269369EE97BC23EC6C5149C91FB3B60C93AD7";
    static final String ALTAR_DEATH_SOURCE_SHA256 =
            "E3979B8579799F74883653868B527CE977EAE49277422AB72D86B0A045F50A62";
    static final String ALTAR_ATTACK_GATE_SOURCE_SHA256 =
            "71621CCFE5046410C8175A31B6CD28B10701910671473C7CFD5BE54D6E8D2971";

    static final int SECOND_FLOOR_KEY = 49165;
    static final int THIRD_FLOOR_KEY = 49166;
    static final int MAGIC_FLUTE = 49167;
    static final int DESTRUCTION_POTION = 49168;
    static final int WILL_OF_DESTRUCTION = 49239;
    static final int ALTAR_PROOF = 49241;
    static final int DESTRUCTION_MEDICINE_EFFECT = 4007;
    static final int DESTRUCTION_MEDICINE_DURATION_MILLIS = 1_500_000;
    static final int DESTRUCTION_MEDICINE_GFX = 7248;
    static final int MAGIC_FLUTE_GFX = 3992;
    static final int MAGIC_FLUTE_EFFECT_RANGE = 8;
    static final Set<Integer> CHINO_NPC_IDS = Set.of(87015, 87016, 87017, 87018);

    static final Location FAILURE_EXIT = new Location(4, 33430, 32814, 4);
    static final int ROTTING_CORPSE_NPC_ID = 80137;
    static final Location ROTTING_CORPSE = new Location(MAP_ID, 32775, 32773, 6);

    /**
     * Retained {@code server_quest_spawn} has no fixed rows for these native
     * actors. Their Java behaviours exist, so coordinates must not be invented.
     */
    static final Set<Integer> ACTORS_WITHOUT_RETAINED_FIXED_SPAWN = Set.of(
            87000, 87001, 87002, 87003, 87007, 87008, 87009, 87010, 87011);

    enum CharacterClass {
        CROWN(0, 1, 80135, "50quest_p",
                new Location(MAP_ID, 32720, 32900, 2),
                new Location(MAP_ID, 32877, 32901, 5),
                new Location(MAP_ID, 32741, 32776, 2),
                new Location(MAP_ID, 32790, 32773, 2), 87017),
        KNIGHT(1, 2, 80136, "50quest_k",
                new Location(MAP_ID, 32721, 32853, 3),
                new Location(MAP_ID, 32877, 32910, 5),
                new Location(MAP_ID, 32741, 32771, 2),
                new Location(MAP_ID, 32788, 32773, 2), 87018),
        ELF(2, 4, 80133, "50quest_e",
                new Location(MAP_ID, 32725, 32940, 1),
                new Location(MAP_ID, 32888, 32910, 5),
                new Location(MAP_ID, 32735, 32771, 2),
                new Location(MAP_ID, 32788, 32771, 2), 87015),
        WIZARD(3, 8, 80134, "50quest_w",
                new Location(MAP_ID, 32810, 32941, 7),
                new Location(MAP_ID, 32888, 32901, 5),
                new Location(MAP_ID, 32735, 32776, 2),
                new Location(MAP_ID, 32788, 32775, 2), 87016);

        private final int characterType;
        private final int mask;
        private final int entranceNpcId;
        private final String entranceHtml;
        private final Location firstFloor;
        private final Location secondFloor;
        private final Location thirdFloor;
        private final Location altarRoom;
        private final int chinoNpcId;

        CharacterClass(int characterType, int mask, int entranceNpcId,
                       String entranceHtml, Location firstFloor,
                       Location secondFloor, Location thirdFloor,
                       Location altarRoom, int chinoNpcId) {
            this.characterType = characterType;
            this.mask = mask;
            this.entranceNpcId = entranceNpcId;
            this.entranceHtml = entranceHtml;
            this.firstFloor = firstFloor;
            this.secondFloor = secondFloor;
            this.thirdFloor = thirdFloor;
            this.altarRoom = altarRoom;
            this.chinoNpcId = chinoNpcId;
        }

        int characterType() { return characterType; }
        int mask() { return mask; }
        int entranceNpcId() { return entranceNpcId; }
        String entranceHtml() { return entranceHtml; }
        Location firstFloor() { return firstFloor; }
        Location secondFloor() { return secondFloor; }
        Location thirdFloor() { return thirdFloor; }
        Location altarRoom() { return altarRoom; }
        int chinoNpcId() { return chinoNpcId; }
    }

    record Location(int mapId, int x, int y, int heading) {
        Location { heading &= 7; }
    }

    record SpawnRow(int spawnId, int npcId, int count, int mapId,
                    int minX, int maxX, int minY, int maxY, int heading,
                    boolean staticSpawn) {}

    enum Stage {
        SECOND_FLOOR, THIRD_FLOOR, ALTAR_ROOM
    }

    record StageItem(int itemId, Stage stage, String sourceSha256) {}

    /**
     * Exact four class-specific back-half completions which start native quest
     * 122 in retained {@code Npc_Dicarding}.  The selected database represents
     * the same boundary as quest 12201, so both durable views must advance in
     * one transaction.
     */
    record PredecessorBridge(int characterType, int questId, int eventId) {}

    private static final Map<Integer, CharacterClass> BY_TYPE = Map.of(
            0, CharacterClass.CROWN, 1, CharacterClass.KNIGHT,
            2, CharacterClass.ELF, 3, CharacterClass.WIZARD);

    private static final Map<Integer, StageItem> STAGE_ITEMS = Map.of(
            SECOND_FLOOR_KEY, new StageItem(
                    SECOND_FLOOR_KEY, Stage.SECOND_FLOOR,
                    SECOND_FLOOR_KEY_SOURCE_SHA256),
            THIRD_FLOOR_KEY, new StageItem(
                    THIRD_FLOOR_KEY, Stage.THIRD_FLOOR,
                    THIRD_FLOOR_KEY_SOURCE_SHA256),
            WILL_OF_DESTRUCTION, new StageItem(
                    WILL_OF_DESTRUCTION, Stage.ALTAR_ROOM,
                    WILL_OF_DESTRUCTION_SOURCE_SHA256));

    private static final Map<Integer, PredecessorBridge> PREDECESSOR_BY_EVENT = Map.of(
            40000132, new PredecessorBridge(0, 10402, 40000132),
            40000171, new PredecessorBridge(1, 10903, 40000171),
            40000262, new PredecessorBridge(2, 11604, 40000262),
            40000295, new PredecessorBridge(3, 12102, 40000295));

    private static final List<SpawnRow> RETAINED_SPAWN_ROWS = List.of(
            new SpawnRow(50143, 87012, 10, MAP_ID, 32727, 32740, 32774, 32796, 5, false),
            new SpawnRow(50144, 87013, 10, MAP_ID, 32727, 32740, 32774, 32796, 5, false),
            new SpawnRow(50145, 87014, 10, MAP_ID, 32727, 32740, 32774, 32796, 5, false),
            new SpawnRow(50150, 87005, 5, MAP_ID, 32878, 32906, 32816, 32822, 0, false),
            new SpawnRow(50151, 87006, 8, MAP_ID, 32921, 32937, 32731, 32774, 0, false),
            new SpawnRow(50152, 87004, 8, MAP_ID, 32921, 32937, 32731, 32774, 0, false),
            new SpawnRow(50153, 87005, 3, MAP_ID, 32926, 32930, 32777, 32805, 0, false),
            new SpawnRow(50154, 87006, 6, MAP_ID, 32910, 32939, 32806, 32834, 0, false),
            new SpawnRow(50155, 87004, 6, MAP_ID, 32910, 32939, 32806, 32834, 0, false),
            new SpawnRow(50156, 87005, 15, MAP_ID, 32848, 32901, 32791, 32928, 0, false),
            new SpawnRow(50157, 87006, 15, MAP_ID, 32848, 32901, 32791, 32928, 0, false),
            new SpawnRow(50158, 87004, 15, MAP_ID, 32848, 32901, 32791, 32928, 0, false),
            new SpawnRow(50162, 80135, 1, 410, 32926, 32926, 32828, 32828, 0, true),
            new SpawnRow(50163, 80136, 1, 410, 32849, 32849, 32830, 32830, 0, true),
            new SpawnRow(50164, 80134, 1, 410, 32854, 32854, 32924, 32924, 0, true),
            new SpawnRow(50165, 80133, 1, 410, 32884, 32884, 32951, 32951, 0, true),
            new SpawnRow(50166, 80137, 1, MAP_ID, 32775, 32775, 32773, 32773, 6, true));

    private ClassicWorldCkewCatalog() {}

    static CharacterClass characterClass(int characterType) {
        return BY_TYPE.get(characterType);
    }

    static int classMask(List<Integer> characterTypes) {
        if (characterTypes == null) return 0;
        int mask = 0;
        for (Integer type : characterTypes) {
            CharacterClass value = type == null ? null : characterClass(type);
            if (value != null) mask += value.mask();
        }
        return mask;
    }

    static boolean exactNativeParty(List<Integer> characterTypes) {
        return characterTypes != null && characterTypes.size() == 4
                && classMask(characterTypes) == REQUIRED_CLASS_MASK;
    }

    static boolean matchesEntrance(int characterType, int npcId) {
        CharacterClass value = characterClass(characterType);
        return value != null && value.entranceNpcId() == npcId;
    }

    static boolean isEntranceNpc(int npcId) {
        return BY_TYPE.values().stream()
                .anyMatch(value -> value.entranceNpcId() == npcId);
    }

    static PredecessorBridge predecessorBridge(int eventId) {
        return PREDECESSOR_BY_EVENT.get(eventId);
    }

    /**
     * Materializes retained quest 122 only for the exact class/event/quest
     * tuple recovered from the native executor.  Existing active or ended
     * quest-122 state is deliberately never rewound.
     */
    static Map<Integer, Integer> bridgePredecessorCompletion(
            ClassicWorldStoryQuestCatalog.Event event, int characterType,
            Map<Integer, Integer> nextQuestSteps) {
        if (event == null || nextQuestSteps == null) {
            throw new IllegalArgumentException("CKEW predecessor state is required");
        }
        PredecessorBridge bridge = predecessorBridge(event.id());
        if (bridge == null || bridge.characterType() != characterType
                || bridge.questId() != event.questId()
                || event.setStep() != 1
                || event.chainedQuestId() != SELECTED_ENTRY_QUEST_ID) {
            return Map.copyOf(nextQuestSteps);
        }
        Map<Integer, Integer> result = new java.util.LinkedHashMap<>(nextQuestSteps);
        result.putIfAbsent(SELECTED_ENTRY_QUEST_ID, 0);
        int retained = result.getOrDefault(QUEST_ID, 0);
        if (retained == 0) result.put(QUEST_ID, QUEST_STARTED);
        return Map.copyOf(result);
    }

    /** Mirrors selected row 12201 while retained quest 122 owns the instance. */
    static Map<Integer, Integer> synchronizeSelectedEntry(
            Map<Integer, Integer> current) {
        if (current == null) throw new IllegalArgumentException("CKEW state is required");
        if (!current.containsKey(SELECTED_ENTRY_QUEST_ID)) return Map.copyOf(current);
        Map<Integer, Integer> result = new java.util.LinkedHashMap<>(current);
        result.put(SELECTED_ENTRY_QUEST_ID, 1);
        result.putIfAbsent(SELECTED_COMPLETION_QUEST_ID, 0);
        return Map.copyOf(result);
    }

    /** Mirrors selected row 12202 after the retained final reward commits. */
    static Map<Integer, Integer> synchronizeSelectedCompletion(
            Map<Integer, Integer> current) {
        if (current == null) throw new IllegalArgumentException("CKEW state is required");
        if (!current.containsKey(SELECTED_COMPLETION_QUEST_ID)) return Map.copyOf(current);
        Map<Integer, Integer> result = new java.util.LinkedHashMap<>(current);
        result.put(SELECTED_ENTRY_QUEST_ID, 1);
        result.put(SELECTED_COMPLETION_QUEST_ID, 1);
        return Map.copyOf(result);
    }

    static StageItem stageItem(long itemId) {
        if (itemId < Integer.MIN_VALUE || itemId > Integer.MAX_VALUE) return null;
        return STAGE_ITEMS.get((int) itemId);
    }

    static Location stageDestination(long itemId, int characterType) {
        StageItem item = stageItem(itemId);
        CharacterClass value = characterClass(characterType);
        if (item == null || value == null) return null;
        return switch (item.stage()) {
            case SECOND_FLOOR -> value.secondFloor();
            case THIRD_FLOOR -> value.thirdFloor();
            case ALTAR_ROOM -> value.altarRoom();
        };
    }

    static List<SpawnRow> retainedSpawnRows() {
        return RETAINED_SPAWN_ROWS;
    }
}
