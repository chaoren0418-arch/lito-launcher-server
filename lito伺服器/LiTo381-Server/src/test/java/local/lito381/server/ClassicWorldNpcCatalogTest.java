package local.lito381.server;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.InputStream;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.List;
import org.junit.jupiter.api.Test;

final class ClassicWorldNpcCatalogTest {
    @Test
    void selectedMap4ResourceKeepsEveryEnabledStaticNpcRow() throws Exception {
        List<ClassicWorldNpcCatalog.StaticNpc> all =
                ClassicWorldNpcCatalog.all(ClassicWorldMapCatalog.WORLD_TREE_MAP_ID);
        assertEquals(ClassicWorldNpcCatalog.MAP4_ROW_COUNT
                + ClassicWorldNpcCatalog.MAP4_SOURCE_RECONSTRUCTED_ROW_COUNT
                + ClassicWorldScarecrowCatalog.MAP4_SPAWN_COUNT
                + ClassicWorldMonsterGuardCatalog.SOURCE_SPAWN_COUNT,
                all.size());
        assertEquals(84, ClassicWorldNpcCatalog.fixedMerchants(4).size());

        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        try (InputStream in = getClass().getResourceAsStream(
                "/world/map4-static-npcs.lwn.gz")) {
            assertNotNull(in);
            digest.update(in.readAllBytes());
        }
        assertEquals(ClassicWorldNpcCatalog.MAP4_RESOURCE_SHA256,
                HexFormat.of().withUpperCase().formatHex(digest.digest()));
    }

    @Test
    void sourceReconstructsDukeSoldierRequiredBySearcherEscort() {
        ClassicWorldNpcCatalog.StaticNpc soldier = ClassicWorldNpcCatalog.all(4)
                .stream()
                .filter(npc -> npc.npcId()
                        == ClassicWorldRescueFollowers.DUKE_SOLDIER_NPC_ID)
                .findFirst().orElseThrow();
        assertEquals(70740, soldier.spawnId());
        assertEquals(34043, soldier.x());
        assertEquals(32234, soldier.y());
        assertEquals(51, soldier.gfxId());
        assertEquals("$2267", soldier.nameId());
        assertEquals("L1Merchant", soldier.implementation());
        assertTrue(ClassicWorldNpcCatalog.occupies(4, 34043, 32234));
    }

    @Test
    void worldTreeDestinationPerceivesExactRetainedMotherMerchant() {
        List<ClassicWorldNpcCatalog.StaticNpc> visible =
                ClassicWorldNpcCatalog.fixedMerchantsInScreen(4, 33051, 32337);
        assertEquals(1, visible.size());
        ClassicWorldNpcCatalog.StaticNpc mother = visible.get(0);
        assertEquals(1276, mother.spawnId());
        assertEquals(70844, mother.npcId());
        assertEquals(33054, mother.x());
        assertEquals(32337, mother.y());
        assertEquals(5, mother.heading());
        assertEquals(0, mother.movementDistance());
        assertEquals("$748", mother.nameId());
        assertEquals("L1Merchant", mother.implementation());
        assertEquals(869, mother.gfxId());
        assertEquals(14, mother.lightSize());
        assertTrue(mother.changeHead());
        assertEquals("motherm1", mother.initialHtml(0));
        assertEquals("motherm1", mother.initialHtml(-10_000));
        assertEquals(0x6400_04FCL, mother.objectId());
        assertTrue(ClassicWorldNpcCatalog.fixedMerchantOccupies(4, 33054, 32337));
        assertFalse(ClassicWorldNpcCatalog.fixedMerchantOccupies(4, 33053, 32337));
    }

    @Test
    void fixedMerchantPacketMatchesRetainedFieldsAndSelectedNativeTail() {
        ClassicWorldNpcCatalog.StaticNpc mother =
                ClassicWorldNpcCatalog.fixedMerchant(4, 0x6400_04FCL);
        assertNotNull(mother);
        byte[] packet = Protocol.classicFixedMerchant(mother, 5);
        assertEquals(
                "0B1E81517EFC040064650300050E0000000000000024373438000000"
                        + "00000000000000FF000000FFFF00",
                HexFormat.of().withUpperCase().formatHex(packet));
    }

    @Test
    void selectedMap350KeepsItsTwoEnabledDwarvesAndNoDisabledTeleporters()
            throws Exception {
        List<ClassicWorldNpcCatalog.StaticNpc> all = ClassicWorldNpcCatalog.all(
                ClassicWorldMapCatalog.GIRAN_TRADE_ZONE_MAP_ID);
        assertEquals(ClassicWorldNpcCatalog.MAP350_ROW_COUNT, all.size());
        assertEquals(List.of(271, 272), all.stream()
                .map(ClassicWorldNpcCatalog.StaticNpc::spawnId).toList());
        assertEquals(List.of(60010, 60011), all.stream()
                .map(ClassicWorldNpcCatalog.StaticNpc::npcId).toList());
        assertTrue(all.stream().allMatch(npc ->
                npc.implementation().equals("L1Dwarf")
                        && npc.initialHtml(0).equals("storage")
                        && npc.fixedWarehouse()));
        assertEquals(List.of(271, 272),
                ClassicWorldNpcCatalog.fixedWarehouses(350).stream()
                        .map(ClassicWorldNpcCatalog.StaticNpc::spawnId).toList());
        assertEquals(60010, ClassicWorldNpcCatalog.fixedWarehouse(
                350, ClassicWorldNpcCatalog.objectId(271)).npcId());
        assertEquals(null, ClassicWorldNpcCatalog.fixedWarehouse(
                4, ClassicWorldNpcCatalog.objectId(75)));
        assertFalse(all.stream().anyMatch(npc ->
                npc.npcId() == 50025 || npc.npcId() == 50026));

        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        try (InputStream in = getClass().getResourceAsStream(
                "/world/map350-static-npcs.lwn.gz")) {
            assertNotNull(in);
            digest.update(in.readAllBytes());
        }
        assertEquals(ClassicWorldNpcCatalog.MAP350_RESOURCE_SHA256,
                HexFormat.of().withUpperCase().formatHex(digest.digest()));
    }

    @Test
    void selectedFairyDungeonsPreserveTheirExplicitlyEmptyStaticNpcResources()
            throws Exception {
        int[] mapIds = {209, 213};
        String[] resourceHashes = {
            ClassicWorldNpcCatalog.MAP209_RESOURCE_SHA256,
            ClassicWorldNpcCatalog.MAP213_RESOURCE_SHA256};
        for (int index = 0; index < mapIds.length; index++) {
            int mapId = mapIds[index];
            assertTrue(ClassicWorldNpcCatalog.all(mapId).isEmpty());
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            try (InputStream in = getClass().getResourceAsStream(
                    "/world/map" + mapId + "-static-npcs.lwn.gz")) {
                assertNotNull(in);
                digest.update(in.readAllBytes());
            }
            assertEquals(resourceHashes[index],
                    HexFormat.of().withUpperCase().formatHex(digest.digest()));
        }
        assertEquals(ClassicWorldNpcCatalog.MAP209_SOURCE_SHA256,
                ClassicWorldNpcCatalog.MAP213_SOURCE_SHA256);
    }

    @Test
    void selectedSleepingDragonFloorsPreserveEmptyStaticNpcResources()
            throws Exception {
        int[] mapIds = {19, 20};
        String[] resourceHashes = {
            ClassicWorldNpcCatalog.MAP19_RESOURCE_SHA256,
            ClassicWorldNpcCatalog.MAP20_RESOURCE_SHA256};
        for (int index = 0; index < mapIds.length; index++) {
            assertTrue(ClassicWorldNpcCatalog.all(mapIds[index]).isEmpty());
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            try (InputStream in = getClass().getResourceAsStream(
                    "/world/map" + mapIds[index] + "-static-npcs.lwn.gz")) {
                assertNotNull(in);
                digest.update(in.readAllBytes());
            }
            assertEquals(resourceHashes[index],
                    HexFormat.of().withUpperCase().formatHex(digest.digest()));
        }
        assertEquals(ClassicWorldNpcCatalog.MAP19_SOURCE_SHA256,
                ClassicWorldNpcCatalog.MAP20_SOURCE_SHA256);
    }

    @Test
    void selectedMap32PreservesBothOverlappingSepiaSourceRows()
            throws Exception {
        List<ClassicWorldNpcCatalog.StaticNpc> all =
                ClassicWorldNpcCatalog.all(32);
        assertEquals(ClassicWorldNpcCatalog.MAP32_ROW_COUNT, all.size());
        assertEquals(List.of(65, 1156), all.stream()
                .map(ClassicWorldNpcCatalog.StaticNpc::spawnId).toList());
        assertTrue(all.stream().allMatch(npc ->
                npc.npcId() == 50031
                        && npc.x() == 32713 && npc.y() == 32842
                        && npc.heading() == 6
                        && npc.nameId().equals("$2259")
                        && npc.implementation().equals("L1Merchant")
                        && npc.gfxId() == 2712
                        && npc.initialHtml(0).equals("sepia2")
                        && npc.fixedMerchant()));
        assertEquals(List.of(0x6400_0041L, 0x6400_0484L), all.stream()
                .map(ClassicWorldNpcCatalog.StaticNpc::objectId).toList());
        assertEquals(2, ClassicWorldNpcCatalog.fixedMerchants(32).size());
        assertTrue(ClassicWorldNpcCatalog.fixedMerchantOccupies(
                32, 32713, 32842));

        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        try (InputStream in = getClass().getResourceAsStream(
                "/world/map32-static-npcs.lwn.gz")) {
            assertNotNull(in);
            digest.update(in.readAllBytes());
        }
        assertEquals(ClassicWorldNpcCatalog.MAP32_RESOURCE_SHA256,
                HexFormat.of().withUpperCase().formatHex(digest.digest()));
    }

    @Test
    void selectedDragonValleyFloorsAndLairPreserveExactEmptyStaticNpcResources()
            throws Exception {
        int[] mapIds = {30, 31, 33, 35, 36, 37};
        String[] resourceHashes = {
                ClassicWorldNpcCatalog.MAP30_RESOURCE_SHA256,
                ClassicWorldNpcCatalog.MAP31_RESOURCE_SHA256,
                ClassicWorldNpcCatalog.MAP33_RESOURCE_SHA256,
                ClassicWorldNpcCatalog.MAP35_RESOURCE_SHA256,
                ClassicWorldNpcCatalog.MAP36_RESOURCE_SHA256,
                ClassicWorldNpcCatalog.MAP37_RESOURCE_SHA256};
        for (int index = 0; index < mapIds.length; index++) {
            assertTrue(ClassicWorldNpcCatalog.all(mapIds[index]).isEmpty());
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            try (InputStream in = getClass().getResourceAsStream(
                    "/world/map" + mapIds[index] + "-static-npcs.lwn.gz")) {
                assertNotNull(in);
                digest.update(in.readAllBytes());
            }
            assertEquals(resourceHashes[index],
                    HexFormat.of().withUpperCase().formatHex(digest.digest()));
        }
        assertEquals(ClassicWorldNpcCatalog.MAP31_SOURCE_SHA256,
                ClassicWorldNpcCatalog.MAP33_SOURCE_SHA256);
        assertEquals(ClassicWorldNpcCatalog.MAP30_SOURCE_SHA256,
                ClassicWorldNpcCatalog.MAP31_SOURCE_SHA256);
    }

    @Test
    void selectedMap445KeepsExactDuranMerchant() throws Exception {
        List<ClassicWorldNpcCatalog.StaticNpc> all = ClassicWorldNpcCatalog.all(445);
        assertEquals(1, all.size());
        ClassicWorldNpcCatalog.StaticNpc duran = all.get(0);
        assertEquals(601, duran.spawnId());
        assertEquals(70094, duran.npcId());
        assertEquals(0x6400_0259L, duran.objectId());
        assertEquals(32753, duran.x());
        assertEquals(32860, duran.y());
        assertEquals(6, duran.heading());
        assertEquals("$3437", duran.nameId());
        assertEquals("L1Merchant", duran.implementation());
        assertEquals(1515, duran.gfxId());
        assertEquals("duran", duran.initialHtml(0));
        assertTrue(duran.fixedMerchant());
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        try (InputStream in = getClass().getResourceAsStream(
                "/world/map445-static-npcs.lwn.gz")) {
            assertNotNull(in);
            digest.update(in.readAllBytes());
        }
        assertEquals(ClassicWorldNpcCatalog.MAP445_RESOURCE_SHA256,
                HexFormat.of().withUpperCase().formatHex(digest.digest()));
    }

    @Test
    void selectedMap447PreservesItsExplicitlyEmptyStaticNpcResource()
            throws Exception {
        assertTrue(ClassicWorldNpcCatalog.all(447).isEmpty());
        assertEquals(ClassicWorldNpcCatalog.MAP209_SOURCE_SHA256,
                ClassicWorldNpcCatalog.MAP447_SOURCE_SHA256);
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        try (InputStream in = getClass().getResourceAsStream(
                "/world/map447-static-npcs.lwn.gz")) {
            assertNotNull(in);
            digest.update(in.readAllBytes());
        }
        assertEquals(ClassicWorldNpcCatalog.MAP447_RESOURCE_SHA256,
                HexFormat.of().withUpperCase().formatHex(digest.digest()));
    }

    @Test
    void selectedMap17KeepsExactNerupaMerchant() throws Exception {
        List<ClassicWorldNpcCatalog.StaticNpc> all = ClassicWorldNpcCatalog.all(17);
        assertEquals(1, all.size());
        ClassicWorldNpcCatalog.StaticNpc nerupa = all.get(0);
        assertEquals(1327, nerupa.spawnId());
        assertEquals(70838, nerupa.npcId());
        assertEquals(0x6400_052FL, nerupa.objectId());
        assertEquals(32741, nerupa.x());
        assertEquals(32787, nerupa.y());
        assertEquals(5, nerupa.heading());
        assertEquals("$749", nerupa.nameId());
        assertEquals(895, nerupa.gfxId());
        assertEquals("nerupaE1", nerupa.initialHtml(0));
        assertEquals("nerupaE1", nerupa.initialHtml(-10_000));
        assertTrue(nerupa.fixedMerchant());
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        try (InputStream in = getClass().getResourceAsStream(
                "/world/map17-static-npcs.lwn.gz")) {
            assertNotNull(in);
            digest.update(in.readAllBytes());
        }
        assertEquals(ClassicWorldNpcCatalog.MAP17_RESOURCE_SHA256,
                HexFormat.of().withUpperCase().formatHex(digest.digest()));
    }
}
