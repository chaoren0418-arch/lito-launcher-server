package local.lito381.server;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.HexFormat;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/** Pins the bounded photo/resource proof for Quest Battle room one. */
final class NativeQuestFukuokaRoomOnePhotoSourceAuditTest {
    @BeforeAll
    static void assumeLocalResourcesPresent() {
        LocalResources.assumeExisting("LiTo381-開發 local file", Path.of("D:/lineage開發/LiTo381-開發/analysis/quest-fukuoka-room1-0.7.638"));
        LocalResources.assumeExisting("selected client Sprite.idx",
                Path.of("D:/380/天堂(Lineage 3.81C) (1)/Sprite.idx"));
    }

    private static final Path EVIDENCE = Path.of(
            "D:/lineage開發/LiTo381-開發/analysis/quest-fukuoka-room1-0.7.638");
    private static final Path SELECTED_CLIENT = Path.of(
            "D:/380/天堂(Lineage 3.81C) (1)");

    @Test void contemporaneousRoomOnePhotosRemainByteExact() throws Exception {
        assertHash("20050819182443_32.jpg",
                "343F2A213DEC0471D3021216E5A3830D20510DDDA78716D9321CF0406E3562F1");
        assertHash("20050819182443_33.jpg",
                "02E0244958B43DC0FD086EA33A8D0B17FC20964F99AFEA58DC34B12A60C75890");
        assertEquals("https://www.4gamer.net/games/001/G000189/20050819182443/",
                BattleQuestEncounter.ROOM_ONE_PHOTO_SOURCE_URL);
    }

    @Test void selectedClientGfx1649ExtractionRemainsByteExact() throws Exception {
        assertEquals("327C06372D87F6CF6D5176DA51DA5EF1F94DBF388D1588B735C5965B96B6A52B",
                sha256(SELECTED_CLIENT.resolve("Sprite.idx")));
        assertHash("1649-0.spr",
                "3DD537B3A0BEC8400162FA81BF586C459AEE8F9CC7CD920343B115D9699706C6");
        assertHash("1649-1.spr",
                "4C8D1DC6AECB7F2BEE75AFCA27A6699F28CF6403A636846E0CA58C9BF281CF06");
        assertHash("1649-0-f000.png",
                "5261D26BF500C07CE06A912596484B01AB764899CEF6C6D05DCD2D31C4E98E03");
        assertHash("1649-1-f000.png",
                "3639B1058B03325FACDC1482F221C0A82B0D6B3A11EA2111305A7D02C98DF38F");
    }

    @Test void implementationUsesOnlyThePhotoVisibleLowerBound() {
        assertEquals(2, BattleQuestEncounter.ROOM_ONE_PHOTO_LOWER_BOUND);
        assertEquals(2, BattleQuestEncounter.waves().get(0).size());
        assertTrue(BattleQuestEncounter.waves().get(0).stream()
                .allMatch(template -> template.npcId() == 45262
                        && template.gfx() == 1649));
        assertTrue(BattleQuestEncounter.COUNT_SOURCE.contains(
                "room1-two-visible-native-gfx1649-lower-bound"));
        assertTrue(BattleQuestEncounter.COUNT_SOURCE.contains("provisional"));
    }

    private static void assertHash(String name, String expected) throws Exception {
        assertEquals(expected, sha256(EVIDENCE.resolve(name)), name);
    }

    private static String sha256(Path path) throws Exception {
        assertTrue(Files.isRegularFile(path), "missing source evidence: " + path);
        return HexFormat.of().withUpperCase().formatHex(
                MessageDigest.getInstance("SHA-256").digest(Files.readAllBytes(path)));
    }
}
