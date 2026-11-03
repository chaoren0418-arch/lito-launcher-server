package local.lito381.server;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;

final class ClassicWorldMapCatalogTest {
    @Test
    void retainedMap4ZoneBitsExposeNormalSafetyAndCombatCells() {
        ClassicWorldMapCatalog.ClassicMap map =
                ClassicWorldMapCatalog.forMap(ClassicWorldMapCatalog.WORLD_TREE_MAP_ID);

        assertEquals(0x1F, map.rawCell(33051, 32337));
        assertTrue(map.isSafetyZone(33051, 32337));
        assertEquals(0x0F, map.rawCell(33313, 32339));
        assertEquals(0x0F, map.rawCell(33312, 32339));
        assertTrue(map.isNormalZone(33313, 32339));
        assertTrue(map.isNormalZone(33312, 32339));

        assertEquals(11, map.rawCell(33664, 32065));
        assertEquals(ClassicWorldMapCatalog.ClassicMap.ZoneType.NORMAL,
                map.zoneType(33664, 32065));
        assertTrue(map.isNormalZone(33664, 32065));
        assertFalse(map.isSafetyZone(33664, 32065));
        assertFalse(map.isCombatZone(33664, 32065));

        assertEquals(31, map.rawCell(33706, 32129));
        assertEquals(ClassicWorldMapCatalog.ClassicMap.ZoneType.SAFETY,
                map.zoneType(33706, 32129));
        assertTrue(map.isSafetyZone(33706, 32129));
        assertFalse(map.isCombatZone(33706, 32129));

        assertEquals(39, map.rawCell(34053, 32230));
        assertEquals(ClassicWorldMapCatalog.ClassicMap.ZoneType.COMBAT,
                map.zoneType(34053, 32230));
        assertTrue(map.isCombatZone(34053, 32230));
        assertFalse(map.isSafetyZone(34053, 32230));
    }

    @Test
    void retainedMap4ResourceHasExactBoundsSourceAndDestinationCells() throws Exception {
        ClassicWorldMapCatalog.ClassicMap map =
                ClassicWorldMapCatalog.forMap(ClassicWorldMapCatalog.WORLD_TREE_MAP_ID);
        assertEquals(4, map.mapId());
        assertEquals(32448, map.startX());
        assertEquals(34303, map.endX());
        assertEquals(32064, map.startY());
        assertEquals(33599, map.endY());
        assertEquals(1856, map.width());
        assertEquals(1536, map.height());
        assertEquals(ClassicWorldMapCatalog.MAP4_SOURCE_SHA256, map.sourceSha256());
        assertTrue(map.allowsPetRecall());
        assertEquals(31, map.rawCell(33051, 32337));
        assertTrue(map.standingPassable(33051, 32337));

        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        try (InputStream in = getClass().getResourceAsStream("/maps/4.l38.gz")) {
            assertTrue(in != null);
            digest.update(in.readAllBytes());
        }
        assertEquals("960573927894893B5DAF286DE36112EF7C70DA33DCCACF7590EC332A2AC530D4",
                HexFormat.of().withUpperCase().formatHex(digest.digest()));
    }

    @Test
    void boundaryAuditUsesExactRuntimeSchedulerMapOrder() throws Exception {
        Path source = Path.of(System.getProperty("user.dir"), "tools",
                "classic_world_supported_maps.py");
        String python = Files.readString(source, StandardCharsets.UTF_8);
        Matcher tuple = Pattern.compile(
                "(?s)SUPPORTED_MAP_IDS\\s*=\\s*\\((.*?)\\)").matcher(python);
        assertTrue(tuple.find(), "Python boundary audit map tuple is missing");

        List<Integer> audited = new ArrayList<>();
        for (String line : tuple.group(1).split("\\R")) {
            String withoutComment = line.replaceFirst("#.*$", "");
            for (String token : withoutComment.split("[,\\s]+")) {
                if (!token.isBlank()) {
                    audited.add(Integer.parseInt(token));
                }
            }
        }
        assertEquals(ClassicWorldMapCatalog.supportedMapIds(), audited,
                "Python boundary audit drifted from the runtime scheduler");
    }

    @Test
    void selectedMap32ResourceHasExactBoundsFlagsAndDragonValleyLanding()
            throws Exception {
        ClassicWorldMapCatalog.ClassicMap map = ClassicWorldMapCatalog.forMap(
                ClassicWorldMapCatalog.DRAGON_VALLEY_DUNGEON_THIRD_FLOOR_MAP_ID);
        assertEquals(32, map.mapId());
        assertEquals(32640, map.startX());
        assertEquals(32767, map.endX());
        assertEquals(32768, map.startY());
        assertEquals(32895, map.endY());
        assertEquals(128, map.width());
        assertEquals(128, map.height());
        assertEquals(ClassicWorldMapCatalog.MAP32_SOURCE_SHA256,
                map.sourceSha256());
        assertEquals("B71315FA5C1C09246CD2DF03CD8BC7377DD16003653D0BEF03A7F05F3D47F745",
                ClassicWorldMapCatalog.MAP32_FLAGS_SOURCE_SHA256);
        assertTrue(map.allowsEscape());
        assertTrue(map.allowsPetRecall());
        assertTrue(map.standingPassable(32657, 32872));

        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        try (InputStream in = getClass().getResourceAsStream("/maps/32.l38.gz")) {
            assertTrue(in != null);
            digest.update(in.readAllBytes());
        }
        assertEquals("11F0798313A1F2C8BD290B3DEA941A88C04756D157D51BD7C0ACE74F2D3FCD2D",
                HexFormat.of().withUpperCase().formatHex(digest.digest()));
    }

    @Test
    void selectedMap30Through37ResourcesLockDragonValleyFloorsAndLair()
            throws Exception {
        int[] mapIds = {30, 31, 33, 35, 36, 37};
        int[][] bounds = {{32704, 32831, 32704, 32831},
                {32704, 32831, 32704, 32831},
                {32640, 32767, 32768, 32895},
                {32640, 32767, 32768, 32895},
                {32640, 32767, 32768, 32895},
                {32640, 32767, 32768, 32895}};
        int[][] landings = {{32727, 32726}, {32773, 32747}, {32670, 32868},
                {32746, 32801}, {32664, 32846}, {32667, 32868}};
        String[] sourceHashes = {ClassicWorldMapCatalog.MAP30_SOURCE_SHA256,
                ClassicWorldMapCatalog.MAP31_SOURCE_SHA256,
                ClassicWorldMapCatalog.MAP33_SOURCE_SHA256,
                ClassicWorldMapCatalog.MAP35_SOURCE_SHA256,
                ClassicWorldMapCatalog.MAP36_SOURCE_SHA256,
                ClassicWorldMapCatalog.MAP37_SOURCE_SHA256};
        String[] flagHashes = {ClassicWorldMapCatalog.MAP30_FLAGS_SOURCE_SHA256,
                ClassicWorldMapCatalog.MAP31_FLAGS_SOURCE_SHA256,
                ClassicWorldMapCatalog.MAP33_FLAGS_SOURCE_SHA256,
                ClassicWorldMapCatalog.MAP35_FLAGS_SOURCE_SHA256,
                ClassicWorldMapCatalog.MAP36_FLAGS_SOURCE_SHA256,
                ClassicWorldMapCatalog.MAP37_FLAGS_SOURCE_SHA256};
        String[] resourceHashes = {
                "28C29B4C4AAD7ADF8726943CE5637AB4BFCC4C5063D397D5C427B19F7FCEC1EF",
                "3ED8391C3A23F1BF38775846EE4FA2382E422546E74B2E1CFD8DC8BB8B567768",
                "53AA9F25EF300B2440250082EEF1423CC45D789A9B9C3832EAF5BF4190A36E94",
                "46823A479A3A60E781356B6258483EB9309EB7DE20FCCE52B792E201FF4F824C",
                "7225D3CDD7586E74D1CFB4C45B812334C2359B8B48518E38BF851CF03D8FC530",
                "DE1D69CA7C9691AF3853DC71897325A828728EE23B9A13B3AE58DC56116ED70D"};
        for (int index = 0; index < mapIds.length; index++) {
            ClassicWorldMapCatalog.ClassicMap map =
                    ClassicWorldMapCatalog.forMap(mapIds[index]);
            assertEquals(bounds[index][0], map.startX());
            assertEquals(bounds[index][1], map.endX());
            assertEquals(bounds[index][2], map.startY());
            assertEquals(bounds[index][3], map.endY());
            assertEquals(128, map.width());
            assertEquals(128, map.height());
            assertEquals(sourceHashes[index], map.sourceSha256());
            assertEquals(64, flagHashes[index].length());
            assertTrue(map.allowsEscape());
            assertTrue(map.allowsPetRecall());
            assertTrue(map.standingPassable(
                    landings[index][0], landings[index][1]));
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            try (InputStream in = getClass().getResourceAsStream(
                    "/maps/" + mapIds[index] + ".l38.gz")) {
                assertTrue(in != null);
                digest.update(in.readAllBytes());
            }
            assertEquals(resourceHashes[index],
                    HexFormat.of().withUpperCase().formatHex(digest.digest()));
        }
    }

    @Test
    void retainedEightDirectionRuleUsesActualMap4CellBits() {
        ClassicWorldMapCatalog.ClassicMap map =
                ClassicWorldMapCatalog.forMap(ClassicWorldMapCatalog.WORLD_TREE_MAP_ID);
        int x = ClassicWorldMapCatalog.WORLD_TREE_X;
        int y = ClassicWorldMapCatalog.WORLD_TREE_Y;
        assertTrue(map.canStep(x, y, x, y - 1));
        assertFalse(map.canStep(x, y, x + 1, y - 1));
        assertTrue(map.canStep(x, y, x + 1, y));
        assertFalse(map.canStep(x, y, x + 1, y + 1));
        assertTrue(map.canStep(x, y, x, y + 1));
        assertTrue(map.canStep(x, y, x - 1, y + 1));
        assertTrue(map.canStep(x, y, x - 1, y));
        assertTrue(map.canStep(x, y, x - 1, y - 1));
        assertFalse(map.canStep(x, y, x + 2, y));
        assertFalse(map.canStep(x, y, 32447, y));
    }

    @Test
    void selectedMap21ResourceHasExactBoundsFlagsAndOrcRouteLanding()
            throws Exception {
        ClassicWorldMapCatalog.ClassicMap map = ClassicWorldMapCatalog.forMap(
                ClassicWorldMapCatalog.ORC_ELVEN_DUNGEON_MAP_ID);
        assertEquals(21, map.mapId());
        assertEquals(32704, map.startX());
        assertEquals(32831, map.endX());
        assertEquals(32704, map.startY());
        assertEquals(32831, map.endY());
        assertEquals(128, map.width());
        assertEquals(128, map.height());
        assertEquals(ClassicWorldMapCatalog.MAP21_SOURCE_SHA256,
                map.sourceSha256());
        assertTrue(map.allowsEscape());
        assertTrue(map.allowsPetRecall());
        assertTrue(map.standingPassable(
                ClassicWorldMapCatalog.ORC_ELVEN_DUNGEON_X,
                ClassicWorldMapCatalog.ORC_ELVEN_DUNGEON_Y));

        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        try (InputStream in = getClass().getResourceAsStream("/maps/21.l38.gz")) {
            assertTrue(in != null);
            digest.update(in.readAllBytes());
        }
        assertEquals("9185C4B30FCBCC70D6BC4B3AF14AB3B0EFEB741824019B13FD3D696D3358B463",
                HexFormat.of().withUpperCase().formatHex(digest.digest()));
    }

    @Test
    void selectedMap303ResourceHasExactBoundsHashAndNpcTeleportLanding()
            throws Exception {
        ClassicWorldMapCatalog.ClassicMap map =
                ClassicWorldMapCatalog.forMap(
                        ClassicWorldMapCatalog.PHANTASM_ISLAND_MAP_ID);
        assertEquals(303, map.mapId());
        assertEquals(32576, map.startX());
        assertEquals(32959, map.endX());
        assertEquals(32512, map.startY());
        assertEquals(32895, map.endY());
        assertEquals(384, map.width());
        assertEquals(384, map.height());
        assertEquals(ClassicWorldMapCatalog.MAP303_SOURCE_SHA256,
                map.sourceSha256());
        assertTrue(map.allowsPetRecall());
        assertTrue(map.standingPassable(
                ClassicWorldMapCatalog.PHANTASM_ISLAND_X,
                ClassicWorldMapCatalog.PHANTASM_ISLAND_Y));
        assertTrue(ClassicWorldMapCatalog.supports(303));
        assertEquals(java.util.List.of(
                        3, 4, 16, 17, 19, 20, 21, 22, 25, 26, 27, 28,
                        43, 44, 45, 46, 47, 48, 49, 50, 51, 541, 542, 543,
                        30, 31, 32, 33, 35, 36, 37, 67,
                        209, 213, 302, 303, 244, 350, 445, 447, 0, 5, 6,
                        70, 83, 84, 1, 2, 14, 13, 237,
                        7, 8, 9, 10, 11, 12, 53, 54, 55, 56, 52,
                        23, 24, 29, 15, 240, 241, 242, 243,
                        75, 76, 77, 78, 79, 80, 81, 82, 66,
                        101, 102, 103, 104, 105, 106, 107, 108, 109, 110,
                        300, 301, 59, 60, 61, 62, 63, 64, 65,
                        252, 253, 254,
                        304, 305, 307, 308, 309, 310, 320, 330,
                        400, 401, 410, 2000, 450, 1011, 613,
                        420, 430, 440, 441, 442, 443, 444,
                        480, 481, 482, 483, 484,
                        550, 551, 552, 553, 554, 555, 556, 557, 558,
                        446,
                        16384, 16896, 17408, 17920, 18432, 18944,
                        19456, 19968, 20480, 20992, 21504, 22016,
                        22528, 23040, 23552, 24064, 24576, 25088,
                        600, 601, 602, 603, 604, 605, 606, 607, 778, 779,
                        1000, 1001, 666),
                ClassicWorldMapCatalog.supportedMapIds());
        for (int mapId = 0; mapId <= 2000; mapId++) {
            assertEquals(ClassicWorldMapCatalog.supports(mapId),
                    ClassicWorldMapCatalog.supportedMapIds().contains(mapId),
                    "scheduler enumeration drifted from loaded map " + mapId);
        }

        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        try (InputStream in = getClass().getResourceAsStream("/maps/303.l38.gz")) {
            assertTrue(in != null);
            digest.update(in.readAllBytes());
        }
        assertEquals("2736D2C4BDC0D8BBDB2960DA81863FDFAFE3B27605331E30BC848327F996271E",
                HexFormat.of().withUpperCase().formatHex(digest.digest()));
    }

    @Test
    void selectedSleepingDragonFloorsKeepExactBoundsFlagsAndPortalLandings()
            throws Exception {
        int[] mapIds = {19, 20};
        String[] sourceHashes = {
            ClassicWorldMapCatalog.MAP19_SOURCE_SHA256,
            ClassicWorldMapCatalog.MAP20_SOURCE_SHA256};
        String[] resourceHashes = {
            "39C9AAA0D1BE39500A5D285B819A63FCEA2BAC1AF9094CC9E7C12CDB3D529723",
            "AA807A42D789EB4464C90AFB86470C9EDBD2172BF58473C2CE1FA3A09A747F79"};
        int[][] landings = {{32805, 32724}, {32766, 32796}};
        for (int index = 0; index < mapIds.length; index++) {
            ClassicWorldMapCatalog.ClassicMap map =
                    ClassicWorldMapCatalog.forMap(mapIds[index]);
            assertEquals(32704, map.startX());
            assertEquals(32831, map.endX());
            assertEquals(32704, map.startY());
            assertEquals(32831, map.endY());
            assertEquals(128, map.width());
            assertEquals(128, map.height());
            assertEquals(sourceHashes[index], map.sourceSha256());
            assertTrue(map.allowsEscape());
            assertTrue(map.allowsPetRecall());
            assertTrue(map.standingPassable(
                    landings[index][0], landings[index][1]));
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            try (InputStream in = getClass().getResourceAsStream(
                    "/maps/" + mapIds[index] + ".l38.gz")) {
                assertTrue(in != null);
                digest.update(in.readAllBytes());
            }
            assertEquals(resourceHashes[index],
                    HexFormat.of().withUpperCase().formatHex(digest.digest()));
        }
        assertEquals("B5506FADA78E41375E2796DF4539021D17C7670755009031078B49844CE0C57B",
                ClassicWorldMapCatalog.SLEEPING_DRAGON_FLAGS_SOURCE_SHA256);
    }

    @Test
    void selectedMap17KeepsExactBoundsHashAndNerupaPortalLandings()
            throws Exception {
        ClassicWorldMapCatalog.ClassicMap map = ClassicWorldMapCatalog.forMap(17);
        assertEquals(32704, map.startX());
        assertEquals(32767, map.endX());
        assertEquals(32768, map.startY());
        assertEquals(32831, map.endY());
        assertEquals(64, map.width());
        assertEquals(64, map.height());
        assertEquals(ClassicWorldMapCatalog.MAP17_SOURCE_SHA256,
                map.sourceSha256());
        assertTrue(map.allowsEscape());
        assertTrue(map.allowsPetRecall());
        assertTrue(map.standingPassable(32747, 32809));
        assertTrue(map.standingPassable(32748, 32808));
        assertTrue(map.standingPassable(32748, 32809));
        assertEquals("0A0FBD109734A88039EDDE821D1BB8DBC55C3821E407F4073134BFFD7D28BD68",
                ClassicWorldMapCatalog.MAP17_FLAGS_SOURCE_SHA256);
        assertEquals("6AF1307BD1298612896C24808E084D78D5C61977AC9058AF37C97FF3B946A05B",
                ClassicWorldMapCatalog.MAP17_GETBACK_SOURCE_SHA256);
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        try (InputStream in = getClass().getResourceAsStream("/maps/17.l38.gz")) {
            assertTrue(in != null);
            digest.update(in.readAllBytes());
        }
        assertEquals("6F9997C4666ED3F5AF6675419AC8821B8B74BBE32AF607F639C479075819FDB2",
                HexFormat.of().withUpperCase().formatHex(digest.digest()));
    }

    @Test
    void selectedMap350ResourceHasExactBoundsFlagsAndFreeRouteLanding()
            throws Exception {
        ClassicWorldMapCatalog.ClassicMap map = ClassicWorldMapCatalog.forMap(
                ClassicWorldMapCatalog.GIRAN_TRADE_ZONE_MAP_ID);
        assertEquals(350, map.mapId());
        assertEquals(32640, map.startX());
        assertEquals(32767, map.endX());
        assertEquals(32768, map.startY());
        assertEquals(32895, map.endY());
        assertEquals(128, map.width());
        assertEquals(128, map.height());
        assertEquals(ClassicWorldMapCatalog.MAP350_SOURCE_SHA256,
                map.sourceSha256());
        assertTrue(map.allowsEscape());
        assertFalse(map.allowsPetRecall());
        assertFalse(ClassicWorldMapCatalog.supports(340),
                "disabled route175/171 must not make map340 reachable");
        assertTrue(map.standingPassable(
                ClassicWorldMapCatalog.GIRAN_TRADE_ZONE_X,
                ClassicWorldMapCatalog.GIRAN_TRADE_ZONE_Y));

        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        try (InputStream in = getClass().getResourceAsStream("/maps/350.l38.gz")) {
            assertTrue(in != null);
            digest.update(in.readAllBytes());
        }
        assertEquals("699F01A7F50EB1F3F025C5A997311ABC2BD4FFBB86DCDAB37DBEFB0E3FA0A288",
                HexFormat.of().withUpperCase().formatHex(digest.digest()));
    }

    @Test
    void selectedFairyDungeonMapsKeepExactBoundsFlagsAndSharedLanding()
            throws Exception {
        int[] mapIds = {209, 213};
        String[] sourceHashes = {
            ClassicWorldMapCatalog.MAP209_SOURCE_SHA256,
            ClassicWorldMapCatalog.MAP213_SOURCE_SHA256};
        String[] resourceHashes = {
            "58FE95124EDC0E3198F3899A98B1148D2C585C93FBF47CE2214243A4086416CE",
            "4D1D1EB3EB3B68726FC94B771455E84998A4F84BD96D218048C98ED3950FF81E"};
        for (int index = 0; index < mapIds.length; index++) {
            int mapId = mapIds[index];
            ClassicWorldMapCatalog.ClassicMap map =
                    ClassicWorldMapCatalog.forMap(mapId);
            assertEquals(32704, map.startX());
            assertEquals(32767, map.endX());
            assertEquals(32768, map.startY());
            assertEquals(32831, map.endY());
            assertEquals(64, map.width());
            assertEquals(64, map.height());
            assertEquals(sourceHashes[index], map.sourceSha256());
            assertTrue(map.allowsEscape());
            assertTrue(map.allowsPetRecall());
            assertTrue(map.standingPassable(
                    ClassicWorldMapCatalog.FAIRY_DUNGEON_X,
                    ClassicWorldMapCatalog.FAIRY_DUNGEON_Y));

            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            try (InputStream in = getClass().getResourceAsStream(
                    "/maps/" + mapId + ".l38.gz")) {
                assertTrue(in != null);
                digest.update(in.readAllBytes());
            }
            assertEquals(resourceHashes[index],
                    HexFormat.of().withUpperCase().formatHex(digest.digest()));
        }
        assertEquals("D02C8CE782A94E0C30AC6FB2F0155BCC3B36F5522E7025ED1D630069277855F2",
                ClassicWorldMapCatalog.FAIRY_DUNGEON_FLAGS_SOURCE_SHA256);
    }

    @Test
    void selectedMap445KeepsExactBoundsFlagsAndHiddenDockLanding()
            throws Exception {
        ClassicWorldMapCatalog.ClassicMap map = ClassicWorldMapCatalog.forMap(
                ClassicWorldMapCatalog.HIDDEN_DOCK_MAP_ID);
        assertEquals(32704, map.startX());
        assertEquals(32831, map.endX());
        assertEquals(32832, map.startY());
        assertEquals(32895, map.endY());
        assertEquals(128, map.width());
        assertEquals(64, map.height());
        assertEquals(ClassicWorldMapCatalog.MAP445_SOURCE_SHA256,
                map.sourceSha256());
        assertTrue(map.allowsEscape());
        assertTrue(map.allowsPetRecall());
        assertTrue(map.standingPassable(
                ClassicWorldMapCatalog.HIDDEN_DOCK_X,
                ClassicWorldMapCatalog.HIDDEN_DOCK_Y));
        assertEquals("74F54017AC7C6CDB19B8469A97DAF2177813F64F1AE5EA4617483511861F251D",
                ClassicWorldMapCatalog.MAP445_FLAGS_SOURCE_SHA256);
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        try (InputStream in = getClass().getResourceAsStream("/maps/445.l38.gz")) {
            assertTrue(in != null);
            digest.update(in.readAllBytes());
        }
        assertEquals("864CB765301E386E32104139676E2F98EDC8C6D1A3DB30CEE49DE05F1E97E006",
                HexFormat.of().withUpperCase().formatHex(digest.digest()));
    }

    @Test
    void selectedMap447KeepsExactBoundsFlagsAndPortalLandings()
            throws Exception {
        ClassicWorldMapCatalog.ClassicMap map = ClassicWorldMapCatalog.forMap(
                ClassicWorldMapCatalog.PIRATE_ISLAND_SHIP_MAP_ID);
        assertEquals(32704, map.startX());
        assertEquals(32767, map.endX());
        assertEquals(32768, map.startY());
        assertEquals(32831, map.endY());
        assertEquals(64, map.width());
        assertEquals(64, map.height());
        assertEquals(ClassicWorldMapCatalog.MAP447_SOURCE_SHA256,
                map.sourceSha256());
        assertEquals(ClassicWorldDungeonCatalog.MAP447_FLAGS_SOURCE_SHA256,
                ClassicWorldMapCatalog.MAP447_FLAGS_SOURCE_SHA256);
        assertTrue(map.allowsEscape());
        assertTrue(map.allowsPetRecall());
        assertTrue(map.standingPassable(32732, 32797));
        assertTrue(map.standingPassable(32731, 32796));

        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        try (InputStream in = getClass().getResourceAsStream("/maps/447.l38.gz")) {
            assertTrue(in != null);
            digest.update(in.readAllBytes());
        }
        assertEquals("E0C85A8D3AA4A8B0836DCBDC932AA37E97236ADB52BC9C590C9A028BFF4B1BDD",
                HexFormat.of().withUpperCase().formatHex(digest.digest()));
    }
}
