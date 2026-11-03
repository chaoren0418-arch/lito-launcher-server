package local.lito381.server;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.InputStream;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;

final class ClassicWorldCkewCatalogTest {
    @Test
    void map2000IsHashLockedAndEveryNativeLandingIsWalkable() throws Exception {
        ClassicWorldMapCatalog.ClassicMap map = ClassicWorldMapCatalog.forMap(2000);
        assertEquals(2000, map.mapId());
        assertEquals(32640, map.startX());
        assertEquals(32959, map.endX());
        assertEquals(32704, map.startY());
        assertEquals(33023, map.endY());
        assertEquals(320, map.width());
        assertEquals(320, map.height());
        assertEquals(ClassicWorldMapCatalog.MAP2000_SOURCE_SHA256,
                map.sourceSha256());
        assertEquals(ClassicWorldMapCatalog.MAP2000_RESOURCE_SHA256,
                resourceHash("/maps/2000.l38.gz"));
        assertTrue(ClassicWorldMapCatalog.supportedMapIds().contains(2000));
        assertTrue(map.allowsEscape());
        assertTrue(map.allowsPetRecall());
        assertFalse(map.allowsRandomTeleport());

        for (ClassicWorldCkewCatalog.CharacterClass value
                : ClassicWorldCkewCatalog.CharacterClass.values()) {
            for (ClassicWorldCkewCatalog.Location location : List.of(
                    value.firstFloor(), value.secondFloor(), value.thirdFloor(),
                    value.altarRoom())) {
                assertTrue(map.standingPassable(location.x(), location.y()),
                        value + " landing is blocked: " + location);
                assertTrue(map.hasWalkableExit(location.x(), location.y()),
                        value + " landing has no exit: " + location);
            }
        }
        assertTrue(map.standingPassable(
                ClassicWorldCkewCatalog.ROTTING_CORPSE.x(),
                ClassicWorldCkewCatalog.ROTTING_CORPSE.y()));
    }

    @Test
    void retainedSecondFloorRowStaysFailClosedBecauseItsSourceIsDead() {
        ClassicWorldMapCatalog.ClassicMap map = ClassicWorldMapCatalog.forMap(2000);
        assertFalse(map.standingPassable(32735, 32991));
        assertFalse(map.hasWalkableExit(32735, 32991));
        assertFalse(ClassicWorldMapCatalog.supports(2001));
        assertEquals(null, ClassicWorldDungeonCatalog.find(2000, 32735, 32991));
    }

    @Test
    void nativePartyIsExactlyOneOfEachOriginalClass() {
        assertTrue(ClassicWorldCkewCatalog.exactNativeParty(List.of(0, 1, 2, 3)));
        assertEquals(15, ClassicWorldCkewCatalog.classMask(List.of(0, 1, 2, 3)));
        assertFalse(ClassicWorldCkewCatalog.exactNativeParty(List.of(0, 1, 2)));
        assertFalse(ClassicWorldCkewCatalog.exactNativeParty(List.of(0, 1, 2, 2)));
        assertFalse(ClassicWorldCkewCatalog.exactNativeParty(List.of(0, 1, 2, 3, 3)));

        assertClass(0, 1, 80135, "50quest_p", 32720, 32900, 2, 87017);
        assertClass(1, 2, 80136, "50quest_k", 32721, 32853, 3, 87018);
        assertClass(2, 4, 80133, "50quest_e", 32725, 32940, 1, 87015);
        assertClass(3, 8, 80134, "50quest_w", 32810, 32941, 7, 87016);
    }

    @Test
    void retainedSpawnRowsStayExactAndEvidenceGapRemainsExplicit() {
        List<ClassicWorldCkewCatalog.SpawnRow> rows =
                ClassicWorldCkewCatalog.retainedSpawnRows();
        assertEquals(17, rows.size());
        assertEquals(111, rows.stream().filter(row -> !row.staticSpawn())
                .mapToInt(ClassicWorldCkewCatalog.SpawnRow::count).sum());
        assertEquals(List.of(50143, 50144, 50145, 50150, 50151, 50152,
                        50153, 50154, 50155, 50156, 50157, 50158,
                        50162, 50163, 50164, 50165, 50166),
                rows.stream().map(ClassicWorldCkewCatalog.SpawnRow::spawnId).toList());
        assertEquals(Set.of(87000, 87001, 87002, 87003, 87007,
                        87008, 87009, 87010, 87011),
                ClassicWorldCkewCatalog.ACTORS_WITHOUT_RETAINED_FIXED_SPAWN);
        assertTrue(rows.stream().noneMatch(row ->
                ClassicWorldCkewCatalog.ACTORS_WITHOUT_RETAINED_FIXED_SPAWN
                        .contains(row.npcId())));
    }

    @Test
    void questAndTimeoutConstantsFollowTheHashPinnedNativeSources() {
        assertEquals(122, ClassicWorldCkewCatalog.QUEST_ID);
        assertEquals(1_800, ClassicWorldCkewCatalog.INSTANCE_SECONDS);
        assertEquals(100, ClassicWorldCkewCatalog.FIRST_SHOW_ID);
        assertEquals(new ClassicWorldCkewCatalog.Location(4, 33430, 32814, 4),
                ClassicWorldCkewCatalog.FAILURE_EXIT);
        assertEquals("28C28A8CD4E2599617C8254A3F6A37E7BC1E6492DCDEC3510980D300ED6D5A07",
                ClassicWorldCkewCatalog.QUEST_SOURCE_SHA256);
        assertEquals("23D7EA0E5793D4061F70FB4251204A3BF71AC61E28CBD37F9DA36D2579B122C7",
                ClassicWorldCkewCatalog.ENTRANCE_SOURCE_SHA256);
        assertEquals("954F0BCF757B40293BB73F7AB64A0C61474FCF8AE4B2BB4353552922F5C04E7C",
                ClassicWorldCkewCatalog.QUEST_TIMER_SOURCE_SHA256);
    }

    @Test
    void exactFourPredecessorReportsBridgeSelectedRowsToNativeQuest122() {
        Map<Integer, List<Integer>> expected = Map.of(
                40000132, List.of(0, 10402),
                40000171, List.of(1, 10903),
                40000262, List.of(2, 11604),
                40000295, List.of(3, 12102));
        for (Map.Entry<Integer, List<Integer>> row : expected.entrySet()) {
            ClassicWorldStoryQuestCatalog.Event event =
                    ClassicWorldStoryQuestCatalog.all().stream()
                            .filter(candidate -> candidate.id() == row.getKey())
                            .findFirst().orElseThrow();
            int type = row.getValue().get(0);
            int predecessor = row.getValue().get(1);
            ClassicWorldCkewCatalog.PredecessorBridge bridge =
                    ClassicWorldCkewCatalog.predecessorBridge(event.id());
            assertNotNull(bridge);
            assertEquals(List.of(type, predecessor, event.id()), List.of(
                    bridge.characterType(), bridge.questId(), bridge.eventId()));

            Map<Integer, Integer> selected =
                    ClassicWorldStoryQuestCatalog.applyQuestMutation(
                            event, Map.of(predecessor, 0));
            Map<Integer, Integer> bridged =
                    ClassicWorldCkewCatalog.bridgePredecessorCompletion(
                            event, type, selected);
            assertEquals(1, bridged.get(predecessor));
            assertEquals(0, bridged.get(
                    ClassicWorldCkewCatalog.SELECTED_ENTRY_QUEST_ID));
            assertEquals(ClassicWorldCkewCatalog.QUEST_STARTED,
                    bridged.get(ClassicWorldCkewCatalog.QUEST_ID));

            Map<Integer, Integer> wrongClass =
                    ClassicWorldCkewCatalog.bridgePredecessorCompletion(
                            event, (type + 1) & 3, selected);
            assertFalse(wrongClass.containsKey(ClassicWorldCkewCatalog.QUEST_ID));
        }

        ClassicWorldStoryQuestCatalog.Event unrelated =
                ClassicWorldStoryQuestCatalog.all().stream()
                        .filter(event -> event.id() == 40000261)
                        .findFirst().orElseThrow();
        assertFalse(ClassicWorldCkewCatalog.bridgePredecessorCompletion(
                unrelated, 2, Map.of(11603, 1))
                .containsKey(ClassicWorldCkewCatalog.QUEST_ID));

        ClassicWorldStoryQuestCatalog.Event elf =
                ClassicWorldStoryQuestCatalog.all().stream()
                        .filter(event -> event.id() == 40000262)
                        .findFirst().orElseThrow();
        Map<Integer, Integer> ended = new LinkedHashMap<>(Map.of(
                11604, 1,
                ClassicWorldCkewCatalog.SELECTED_ENTRY_QUEST_ID, 0,
                ClassicWorldCkewCatalog.QUEST_ID,
                ClassicWorldCkewCatalog.QUEST_ENDED));
        assertEquals(ClassicWorldCkewCatalog.QUEST_ENDED,
                ClassicWorldCkewCatalog.bridgePredecessorCompletion(
                        elf, 2, ended).get(ClassicWorldCkewCatalog.QUEST_ID));
    }

    @Test
    void selectedCompatibilityLifecycleNeverTouchesLegacyOnlyQuest122() {
        Map<Integer, Integer> legacy = Map.of(
                ClassicWorldCkewCatalog.QUEST_ID,
                ClassicWorldCkewCatalog.QUEST_STARTED);
        assertEquals(legacy,
                ClassicWorldCkewCatalog.synchronizeSelectedEntry(legacy));
        assertEquals(legacy,
                ClassicWorldCkewCatalog.synchronizeSelectedCompletion(legacy));

        Map<Integer, Integer> entered =
                ClassicWorldCkewCatalog.synchronizeSelectedEntry(Map.of(
                        ClassicWorldCkewCatalog.QUEST_ID,
                        ClassicWorldCkewCatalog.QUEST_ENTERED,
                        ClassicWorldCkewCatalog.SELECTED_ENTRY_QUEST_ID, 0));
        assertEquals(1, entered.get(
                ClassicWorldCkewCatalog.SELECTED_ENTRY_QUEST_ID));
        assertEquals(0, entered.get(
                ClassicWorldCkewCatalog.SELECTED_COMPLETION_QUEST_ID));

        Map<Integer, Integer> completed =
                ClassicWorldCkewCatalog.synchronizeSelectedCompletion(entered);
        assertEquals(1, completed.get(
                ClassicWorldCkewCatalog.SELECTED_ENTRY_QUEST_ID));
        assertEquals(1, completed.get(
                ClassicWorldCkewCatalog.SELECTED_COMPLETION_QUEST_ID));
        assertEquals(ClassicWorldCkewCatalog.QUEST_ENTERED,
                completed.get(ClassicWorldCkewCatalog.QUEST_ID));
    }

    @Test
    void retainedStageItemsResolveEveryClassLandingAndInventoryMetadata() {
        assertStageItem(49165, ClassicWorldCkewCatalog.Stage.SECOND_FLOOR,
                "2A68CB06C5ECACF0F46ADB4AECC378647A08215C6ABA0BACEF2A2C598D8BEB87",
                "$5585", 59, 151, 0);
        assertStageItem(49166, ClassicWorldCkewCatalog.Stage.THIRD_FLOOR,
                "81CCCB60BE6B5205978CD473E07DA267B3C1E04D59D375C49A9A02EF3444206A",
                "$5586", 59, 151, 0);
        assertStageItem(49239, ClassicWorldCkewCatalog.Stage.ALTAR_ROOM,
                "5CBA4A1C963FE5607A8DEEBEEE8803992DE8AF6A82C5E4231FC4D0E6BC91F229",
                "$5735", 990, 3963, 1_000);
        assertEquals(null, ClassicWorldCkewCatalog.stageItem(49167));
        assertEquals(null, ClassicWorldCkewCatalog.stageDestination(49165, 4));

        InventoryCatalog.Definition flute = InventoryCatalog.find(49167);
        assertNotNull(flute);
        assertEquals(0, flute.useType());
        assertEquals(2992, flute.icon());
        assertEquals(906, flute.groundGfx());
        assertEquals("$5587", flute.name());
        assertEquals(8_000, flute.unitWeight());
        assertTrue(flute.stackable());
        assertFalse(flute.tradable());
        assertEquals(InventoryCatalog.Effect.CKEW_MAGIC_FLUTE, flute.effect());
        assertEquals(3992, ClassicWorldCkewCatalog.MAGIC_FLUTE_GFX);
        assertEquals(8, ClassicWorldCkewCatalog.MAGIC_FLUTE_EFFECT_RANGE);
        assertEquals("4F0EC883D437D10AF569776B49B269369EE97BC23EC6C5149C91FB3B60C93AD7",
                ClassicWorldCkewCatalog.MAGIC_FLUTE_SOURCE_SHA256);

        InventoryCatalog.Definition altarProof = InventoryCatalog.find(49241);
        assertNotNull(altarProof);
        assertEquals(0xFF, altarProof.useType());
        assertEquals(1997, altarProof.icon());
        assertEquals(5048, altarProof.groundGfx());
        assertEquals("$5799", altarProof.name());
        assertEquals(0, altarProof.unitWeight());
        assertTrue(altarProof.stackable());
        assertFalse(altarProof.tradable());
        assertEquals("E3979B8579799F74883653868B527CE977EAE49277422AB72D86B0A045F50A62",
                ClassicWorldCkewCatalog.ALTAR_DEATH_SOURCE_SHA256);
        assertEquals("71621CCFE5046410C8175A31B6CD28B10701910671473C7CFD5BE54D6E8D2971",
                ClassicWorldCkewCatalog.ALTAR_ATTACK_GATE_SOURCE_SHA256);
    }

    @Test
    void destructionMedicineMetadataAndExecutorConstantsRemainExact() {
        InventoryCatalog.Definition medicine = InventoryCatalog.find(49168);
        assertNotNull(medicine);
        assertEquals(0, medicine.useType());
        assertEquals(2996, medicine.icon());
        assertEquals(3714, medicine.groundGfx());
        assertEquals("$5588", medicine.name());
        assertEquals(0, medicine.unitWeight());
        assertTrue(medicine.stackable());
        assertFalse(medicine.tradable());
        assertEquals(InventoryCatalog.Effect.CKEW_DESTRUCTION_MEDICINE,
                medicine.effect());
        assertEquals(4007,
                ClassicWorldCkewCatalog.DESTRUCTION_MEDICINE_EFFECT);
        assertEquals(1_500_000,
                ClassicWorldCkewCatalog.DESTRUCTION_MEDICINE_DURATION_MILLIS);
        assertEquals(7248, ClassicWorldCkewCatalog.DESTRUCTION_MEDICINE_GFX);
        assertEquals("D82C752A8BAE580F895E3550F6D8FDB875C6800AEC1F99586426FEDE2D3B9194",
                ClassicWorldCkewCatalog.DESTRUCTION_MEDICINE_SOURCE_SHA256);
    }

    private static void assertStageItem(
            int itemId, ClassicWorldCkewCatalog.Stage stage, String sourceHash,
            String nameId, int icon, int groundGfx, int unitWeight) {
        ClassicWorldCkewCatalog.StageItem stageItem =
                ClassicWorldCkewCatalog.stageItem(itemId);
        assertNotNull(stageItem);
        assertEquals(stage, stageItem.stage());
        assertEquals(sourceHash, stageItem.sourceSha256());
        InventoryCatalog.Definition definition = InventoryCatalog.find(itemId);
        assertNotNull(definition);
        assertEquals(0, definition.useType());
        assertEquals(nameId, definition.name());
        assertEquals(icon, definition.icon());
        assertEquals(groundGfx, definition.groundGfx());
        assertEquals(unitWeight, definition.unitWeight());
        assertTrue(definition.stackable());
        assertFalse(definition.tradable());
        assertEquals(InventoryCatalog.Effect.CKEW_STAGE_TELEPORT,
                definition.effect());
        for (int type = 0; type < 4; type++) {
            ClassicWorldCkewCatalog.CharacterClass characterClass =
                    ClassicWorldCkewCatalog.characterClass(type);
            ClassicWorldCkewCatalog.Location expected = switch (stage) {
                case SECOND_FLOOR -> characterClass.secondFloor();
                case THIRD_FLOOR -> characterClass.thirdFloor();
                case ALTAR_ROOM -> characterClass.altarRoom();
            };
            assertEquals(expected,
                    ClassicWorldCkewCatalog.stageDestination(itemId, type));
        }
    }

    private static void assertClass(int type, int mask, int npcId, String html,
                                    int x, int y, int heading, int chinoNpcId) {
        ClassicWorldCkewCatalog.CharacterClass value =
                ClassicWorldCkewCatalog.characterClass(type);
        assertNotNull(value);
        assertEquals(mask, value.mask());
        assertEquals(npcId, value.entranceNpcId());
        assertEquals(html, value.entranceHtml());
        assertEquals(new ClassicWorldCkewCatalog.Location(2000, x, y, heading),
                value.firstFloor());
        assertEquals(chinoNpcId, value.chinoNpcId());
        assertTrue(ClassicWorldCkewCatalog.matchesEntrance(type, npcId));
    }

    private static String resourceHash(String name) throws Exception {
        try (InputStream stream = ClassicWorldCkewCatalogTest.class
                .getResourceAsStream(name)) {
            assertNotNull(stream);
            return HexFormat.of().withUpperCase().formatHex(
                    MessageDigest.getInstance("SHA-256").digest(stream.readAllBytes()));
        }
    }
}
