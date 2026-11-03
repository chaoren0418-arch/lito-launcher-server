package local.lito381.server;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.InputStream;
import java.security.MessageDigest;
import java.util.HexFormat;
import org.junit.jupiter.api.Test;

final class ClassicWorldHartinCatalogTest {
    @Test
    void selectedMap16KeepsExactCollisionBoundsAndFlags() throws Exception {
        ClassicWorldMapCatalog.ClassicMap map = ClassicWorldMapCatalog.forMap(16);
        assertEquals(32704, map.startX());
        assertEquals(32767, map.endX());
        assertEquals(32768, map.startY());
        assertEquals(32831, map.endY());
        assertEquals(64, map.width());
        assertEquals(64, map.height());
        assertEquals(ClassicWorldMapCatalog.MAP16_SOURCE_SHA256,
                map.sourceSha256());
        assertEquals("2D71D90845F149A26A077889151D470EAE010E25D768821A9F038995B2ABA029",
                ClassicWorldMapCatalog.MAP16_FLAGS_SOURCE_SHA256);
        assertEquals("C07F37209557A17829E4C5CF9EB1248CAFF4F7B6B60F2140539347D3D53740F3",
                ClassicWorldMapCatalog.MAP16_FLAGS_SECONDARY_SOURCE_SHA256);
        assertTrue(map.allowsEscape());
        assertTrue(map.allowsPetRecall());
        assertTrue(map.standingPassable(32744, 32813));
        assertTrue(map.standingPassable(32744, 32815));
        assertTrue(map.standingPassable(32730, 32788));
        assertEquals("D3CA89488857F398AFD8B2323118F130380390E90D70368061A5F3279210E54E",
                resourceHash("/maps/16.l38.gz"));
    }

    @Test
    void selectedMap16KeepsExactHorunActorAndEmptyCombatTables() throws Exception {
        assertEquals(1, ClassicWorldNpcCatalog.all(16).size());
        ClassicWorldNpcCatalog.StaticNpc horun = ClassicWorldNpcCatalog.npc(
                16, ClassicWorldNpcCatalog.objectId(588));
        assertNotNull(horun);
        assertEquals(588, horun.spawnId());
        assertEquals(70080, horun.npcId());
        assertEquals(32730, horun.x());
        assertEquals(32788, horun.y());
        assertEquals(4, horun.heading());
        assertEquals(0, horun.movementDistance());
        assertEquals("$751", horun.nameId());
        assertEquals("L1Merchant", horun.implementation());
        assertEquals(834, horun.gfxId());
        assertEquals("horunev1", horun.initialHtml(0));
        assertEquals("horunev1", horun.initialHtml(-10_000));
        assertEquals(0x6400_024CL, horun.objectId());
        byte[] actor = Protocol.classicFixedMerchant(horun, horun.heading());
        assertEquals(Protocol.S_PUT_OBJECT, actor[0] & 0xFF);
        assertEquals(32730, (actor[1] & 0xFF) | ((actor[2] & 0xFF) << 8));
        assertEquals(32788, (actor[3] & 0xFF) | ((actor[4] & 0xFF) << 8));
        assertEquals(horun.objectId(), Integer.toUnsignedLong(
                (actor[5] & 0xFF) | ((actor[6] & 0xFF) << 8)
                        | ((actor[7] & 0xFF) << 16) | ((actor[8] & 0xFF) << 24)));
        assertEquals(834, (actor[9] & 0xFF) | ((actor[10] & 0xFF) << 8));

        assertTrue(ClassicWorldMonsterCatalog.definitions(16).isEmpty());
        assertEquals(0, ClassicWorldDropCatalog.dropCount(16));
        assertTrue(ClassicWorldDropCatalog.items(16).isEmpty());
        assertEquals("4BCC2BCAB09BE98B6AC8657675CDA06D8BC60CEB072269AE84FF6588643C0194",
                resourceHash("/world/map16-static-npcs.lwn.gz"));
        assertEquals(ClassicWorldMonsterCatalog.MAP16_RESOURCE_SHA256,
                resourceHash("/world/map16-monsters.lwm.gz"));
        assertEquals(ClassicWorldDropCatalog.MAP16_RESOURCE_SHA256,
                resourceHash("/world/map16-drops.lwd.gz"));
    }

    @Test
    void selectedMap16GetbackReturnsToExactWorldCoordinateWithoutTownRandomization() {
        assertEquals("FD74F2E14075C56686548E64593B77B2096ED50073E4637A90B9C641367D3AA6",
                ClassicWorldGetbackCatalog.MAP16_SOURCE_SHA256);
        for (int characterType : new int[] {0, 1, 2, 4}) {
            assertEquals(new ClassicWorldGetbackCatalog.Destination(
                            4, 33051, 32343, 0),
                    ClassicWorldGetbackCatalog.resolve(
                            16, 32730, 32788, characterType, 12345));
        }
    }

    private String resourceHash(String name) throws Exception {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        try (InputStream in = getClass().getResourceAsStream(name)) {
            assertNotNull(in);
            digest.update(in.readAllBytes());
        }
        return HexFormat.of().withUpperCase().formatHex(digest.digest());
    }
}
