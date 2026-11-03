package local.lito381.server;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

final class ClassicWorldGetbackCatalogTest {
    @Test
    void selectedMap4KeepsAllFifteenRetainedGetbackAreas() {
        assertEquals(15, ClassicWorldGetbackCatalog.map4RowCount());
        assertEquals("186F017F608949988F8951C62A362E89E95ECDC72A8199895CDE0285FA181938",
                ClassicWorldGetbackCatalog.GETBACK_TABLE_SOURCE_SHA256);
        assertEquals("DAEF00602E793678BFD1C9BC0E210D2E9856F2A7C45FC38D9D322A5A22A5EDE3",
                ClassicWorldGetbackCatalog.TOWN_LOCATION_SOURCE_SHA256);
    }

    @Test
    void worldTreeDeathUsesClassSpecificElfTownOrGenericOrcTown() {
        ClassicWorldGetbackCatalog.Destination elf =
                ClassicWorldGetbackCatalog.resolve(4, 33051, 32337, 2, 0);
        ClassicWorldGetbackCatalog.Destination knight =
                ClassicWorldGetbackCatalog.resolve(4, 33051, 32337, 1, 0);
        assertEquals(new ClassicWorldGetbackCatalog.Destination(
                4, 33065, 32358, 11), elf);
        assertEquals(new ClassicWorldGetbackCatalog.Destination(
                4, 32750, 32435, 4), knight);
        assertTrue(ClassicWorldMapCatalog.forMap(4)
                .standingPassable(elf.x(), elf.y()));
        assertTrue(ClassicWorldMapCatalog.forMap(4)
                .standingPassable(knight.x(), knight.y()));
    }

    @Test
    void unsupportedAreaKeepsRetainedSilverKnightFallback() {
        assertEquals(new ClassicWorldGetbackCatalog.Destination(
                        4, 33089, 33397, 2),
                ClassicWorldGetbackCatalog.resolve(4, 1, 1, 0, 0));
    }

    @Test
    void map303HomeScrollUsesGetbackAbsenceFallbackNotRestartRow() {
        assertEquals("89E9FB154D6653B25664DF98A22A6C5917C13006C5FFAEEE7DCC754C05A41E65",
                ClassicWorldGetbackCatalog.MAP303_ABSENCE_SOURCE_SHA256);
        assertEquals(new ClassicWorldGetbackCatalog.Destination(
                        4, 33089, 33397, 2),
                ClassicWorldGetbackCatalog.resolve(303, 32635, 32817, 3, 0));
        assertTrue(ClassicWorldMapCatalog.forMap(4)
                .standingPassable(33089, 33397));
        assertTrue(ClassicWorldMapCatalog.forMap(303).allowsEscape());
        assertEquals("A1F65C00C884BE5B8E8F7B05CB416C6E3F611D5D6B3FFA179A539F4FC4685ECA",
                ClassicWorldMapCatalog.ESCAPE_FLAGS_SOURCE_SHA256);
    }

    @Test
    void map350HomeScrollUsesItsMapWideGiranTownRow() {
        assertEquals("76ABEEEB87228ED09A37E98CDB4177C4437AA90626EEB1D4BFC892992708E01D",
                ClassicWorldGetbackCatalog.MAP350_SOURCE_SHA256);
        ClassicWorldGetbackCatalog.Destination destination =
                ClassicWorldGetbackCatalog.resolve(350, 32702, 32842, 1, 0);
        assertEquals(new ClassicWorldGetbackCatalog.Destination(
                4, 33435, 32803, 7), destination);
        assertTrue(ClassicWorldMapCatalog.forMap(4)
                .standingPassable(destination.x(), destination.y()));
        assertTrue(ClassicWorldMapCatalog.forMap(350).allowsEscape());
        assertFalse(ClassicWorldMapCatalog.forMap(350).allowsPetRecall());
    }

    @Test
    void map21HomeScrollUsesItsClassSpecificMapWideReturnRow() {
        assertEquals("741FF6D02AAAC1D5E8A311071E6133AEF6FACF87A34C3ACDE9E09142128001FE",
                ClassicWorldGetbackCatalog.MAP21_SOURCE_SHA256);
        ClassicWorldGetbackCatalog.Destination elf =
                ClassicWorldGetbackCatalog.resolve(21, 32760, 32761, 2, 0);
        ClassicWorldGetbackCatalog.Destination knight =
                ClassicWorldGetbackCatalog.resolve(21, 32760, 32761, 1, 0);
        assertEquals(new ClassicWorldGetbackCatalog.Destination(
                4, 33065, 32358, 11), elf);
        assertEquals(new ClassicWorldGetbackCatalog.Destination(
                4, 32750, 32435, 4), knight);
        assertTrue(ClassicWorldMapCatalog.forMap(4)
                .standingPassable(elf.x(), elf.y()));
        assertTrue(ClassicWorldMapCatalog.forMap(4)
                .standingPassable(knight.x(), knight.y()));
    }

    @Test
    void map32HomeScrollUsesItsExactMapWideGiranTownRow() {
        assertEquals("205460B3D345E19BD070C6FCD1CE2B4FCC29109918E8CD9AFF36A4709B8AE31D",
                ClassicWorldGetbackCatalog.MAP32_SOURCE_SHA256);
        ClassicWorldGetbackCatalog.Destination destination =
                ClassicWorldGetbackCatalog.resolve(32, 32657, 32872, 1, 0);
        assertEquals(new ClassicWorldGetbackCatalog.Destination(
                4, 33435, 32803, 7), destination);
        assertTrue(ClassicWorldMapCatalog.forMap(4)
                .standingPassable(destination.x(), destination.y()));
        assertTrue(ClassicWorldMapCatalog.forMap(32).allowsEscape());
        assertTrue(ClassicWorldMapCatalog.forMap(32).allowsPetRecall());
    }

    @Test
    void map30Map31And33HomeScrollUseTheirExactMapWideGiranRows() {
        assertEquals("506BA4A75FC53F989FA9E2D132FD5A6AD6BA15EBC927DB1EADF7EC696D4BD341",
                ClassicWorldGetbackCatalog.MAP30_SOURCE_SHA256);
        assertEquals("210D9A00C765269EA79414A34961021A71985770DE2609755A1EED940A00C764",
                ClassicWorldGetbackCatalog.MAP31_SOURCE_SHA256);
        assertEquals("7A3D41081387CD25AC4DD52280F3D76614ADD7483A97C4D2FF50FF13AD52E4BB",
                ClassicWorldGetbackCatalog.MAP33_SOURCE_SHA256);
        for (int mapId : new int[] {30, 31, 33}) {
            ClassicWorldGetbackCatalog.Destination destination =
                    ClassicWorldGetbackCatalog.resolve(mapId, 32700, 32800, 1, 0);
            assertEquals(new ClassicWorldGetbackCatalog.Destination(
                    4, 33435, 32803, 7), destination);
            assertTrue(ClassicWorldMapCatalog.forMap(4)
                    .standingPassable(destination.x(), destination.y()));
        }
    }

    @Test
    void deepDragonValleyAndLairUseTheirExactMapWideGiranRows() {
        assertEquals("0907AB320E588CE76BBE61A4D9E78F3CE0E2C624B37355CBCF038613EC00ED63",
                ClassicWorldGetbackCatalog.DRAGON_VALLEY_DEEP_SOURCE_SHA256);
        String[] hashes = {ClassicWorldGetbackCatalog.MAP35_SOURCE_SHA256,
                ClassicWorldGetbackCatalog.MAP36_SOURCE_SHA256,
                ClassicWorldGetbackCatalog.MAP37_SOURCE_SHA256};
        int[] mapIds = {35, 36, 37};
        for (int index = 0; index < mapIds.length; index++) {
            assertEquals(64, hashes[index].length());
            ClassicWorldGetbackCatalog.Destination destination =
                    ClassicWorldGetbackCatalog.resolve(
                            mapIds[index], 32670, 32840, 1, 0);
            assertEquals(new ClassicWorldGetbackCatalog.Destination(
                    4, 33435, 32803, 7), destination);
            assertTrue(ClassicWorldMapCatalog.forMap(mapIds[index]).allowsEscape());
            assertTrue(ClassicWorldMapCatalog.forMap(mapIds[index]).allowsPetRecall());
        }
    }

    @Test
    void map17HomeScrollUsesItsClassSpecificMapWideReturnRow() {
        assertEquals("6AF1307BD1298612896C24808E084D78D5C61977AC9058AF37C97FF3B946A05B",
                ClassicWorldGetbackCatalog.MAP17_SOURCE_SHA256);
        ClassicWorldGetbackCatalog.Destination elf =
                ClassicWorldGetbackCatalog.resolve(17, 32747, 32809, 2, 0);
        ClassicWorldGetbackCatalog.Destination knight =
                ClassicWorldGetbackCatalog.resolve(17, 32747, 32809, 1, 0);
        assertEquals(new ClassicWorldGetbackCatalog.Destination(
                4, 33065, 32358, 11), elf);
        assertEquals(new ClassicWorldGetbackCatalog.Destination(
                4, 32750, 32435, 4), knight);
        assertTrue(ClassicWorldMapCatalog.forMap(4)
                .standingPassable(elf.x(), elf.y()));
        assertTrue(ClassicWorldMapCatalog.forMap(4)
                .standingPassable(knight.x(), knight.y()));
    }

    @Test
    void sleepingDragonFloorsUseTheirClassSpecificMapWideReturnRows() {
        assertEquals("D7055E3189CA0740132CAEC9877C3F80DEF08D3AAC1B1093358B21DCB04B82FE",
                ClassicWorldGetbackCatalog.SLEEPING_DRAGON_SOURCE_SHA256);
        for (int mapId : new int[] {19, 20}) {
            ClassicWorldGetbackCatalog.Destination elf =
                    ClassicWorldGetbackCatalog.resolve(mapId, 32772, 32782, 2, 0);
            ClassicWorldGetbackCatalog.Destination knight =
                    ClassicWorldGetbackCatalog.resolve(mapId, 32772, 32782, 1, 0);
            assertEquals(new ClassicWorldGetbackCatalog.Destination(
                    4, 33065, 32358, 11), elf);
            assertEquals(new ClassicWorldGetbackCatalog.Destination(
                    4, 32750, 32435, 4), knight);
            assertTrue(ClassicWorldMapCatalog.forMap(mapId).allowsEscape());
            assertTrue(ClassicWorldMapCatalog.forMap(mapId).allowsPetRecall());
        }
    }

    @Test
    void fairyDungeonHomeScrollsUseSelectedGetbackAbsenceFallback() {
        assertEquals("71071DF1C2501FC6C04A10F97F22B540CAB0DFA935C5F568ECF0220311738CA4",
                ClassicWorldGetbackCatalog.FAIRY_DUNGEONS_ABSENCE_SOURCE_SHA256);
        for (int mapId : new int[] {209, 213}) {
            assertEquals(new ClassicWorldGetbackCatalog.Destination(
                            4, 33089, 33397, 2),
                    ClassicWorldGetbackCatalog.resolve(
                            mapId, 32727, 32799, 4, 0));
            assertTrue(ClassicWorldMapCatalog.forMap(mapId).allowsEscape());
        }
    }

    @Test
    void hiddenDockHomeScrollUsesSelectedGetbackAbsenceFallback() {
        assertEquals("14EEDA7E06DAAEE06F7758DF68BCA61541765A0B11C8541A85CF2FEF3FB773CF",
                ClassicWorldGetbackCatalog.MAP445_ABSENCE_SOURCE_SHA256);
        assertEquals(new ClassicWorldGetbackCatalog.Destination(
                        4, 33089, 33397, 2),
                ClassicWorldGetbackCatalog.resolve(445, 32738, 32857, 1, 0));
        assertTrue(ClassicWorldMapCatalog.forMap(445).allowsEscape());
    }
}
