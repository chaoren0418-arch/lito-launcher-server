package local.lito381.server;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.List;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/** Cross-source proof for the selected map-4 L1Guardian native flow. */
final class NativeClassicWorldGuardianSourceAuditTest {
    @BeforeAll
    static void assumeLocalResourcesPresent() {
        LocalResources.assumeLineageSources();
        LocalResources.assumeLitoBin();
        LocalResources.assumeIdaClient(LocalResources.IDA_DISASSEMBLY_ABSOLUTE);
    }

    private static final Path RETAINED = Path.of("..", "1-伺服器")
            .toAbsolutePath().normalize();
    private static final Path CURRENT = RETAINED.resolve("src/com/lineage/server");
    private static final Path SECONDARY = RETAINED.resolve(
            "其他src參考用/381n8src/com/lineage/server");
    private static final Path DATABASE = RETAINED.resolve("db/380_db.sql");
    private static final Path CLIENT = Path.of(
            "D:/380/天堂(Lineage 3.81C) (1)/LiTo.bin");
    private static final Path ASM = Path.of(
            "D:/lineage開發/LiTo381-開發/ida-client-oep-v2/full-disassembly.asm");

    @Test
    void retainedSourceAndSelectedDatabaseHashesStayPinned() throws Exception {
        assertEquals(ClassicWorldGuardianCatalog.SELECTED_DATABASE_SHA256,
                sha256(DATABASE));
        assertEquals(ClassicWorldGuardianCatalog.RETAINED_GUARDIAN_SOURCE_SHA256,
                sha256(CURRENT.resolve("model/Instance/L1GuardianInstance.java")));
        assertEquals(ClassicWorldGuardianCatalog.SECONDARY_GUARDIAN_SOURCE_SHA256,
                sha256(SECONDARY.resolve("model/Instance/L1GuardianInstance.java")));
        assertEquals(ClassicWorldGuardianCatalog.RETAINED_NPC_CHAT_SOURCE_SHA256,
                sha256(CURRENT.resolve("serverpackets/S_NpcChat.java")));
        assertEquals(ClassicWorldGuardianCatalog.RETAINED_NPC_SHOUT_SOURCE_SHA256,
                sha256(CURRENT.resolve("serverpackets/S_NpcChatShouting.java")));
        assertEquals(ClassicWorldGuardianCatalog.RETAINED_CHANGE_HEADING_SOURCE_SHA256,
                sha256(CURRENT.resolve("serverpackets/S_ChangeHeading.java")));
        assertEquals("CE6EE68ADA8FD37E6C096B1A27E46CB6888F1179F85DEFB5007AC60287360292",
                sha256(CLIENT));
        assertEquals("2A3A6E1026EE65288C07EFF00990BDDDBEFA5ACEC9EED551F528A0CC0A7DEA9A",
                sha256(ASM));
    }

    @Test
    void retainedGuardianPinsTargetHarvestTalkDamageAndDeathSemantics()
            throws Exception {
        String guardian = readLegacy(
                CURRENT.resolve("model/Instance/L1GuardianInstance.java"));
        String player = readLegacy(
                CURRENT.resolve("model/Instance/L1PcInstance.java"));
        ordered(guardian,
                "if (!pc.isElf())",
                "new S_NpcChatShouting(npc, \"$804\")",
                "pc.isWantedForElf()",
                "new S_NpcChat(npc, \"$815\")");
        assertTrue(guardian.contains(
                "(player.getType() == 2) && (player.getCurrentWeapon() == 0) && player.isElf()"));
        assertTrue(guardian.contains("this._random.nextInt(100) + 1"));
        assertTrue(guardian.contains("storeItem(40506, 1)"));
        assertTrue(guardian.contains("storeItem(40507, 1)"));
        assertTrue(guardian.contains("storeItem(40505, 1)"));
        assertTrue(guardian.contains("storeItem(40519, 5)"));
        assertTrue(guardian.contains("storeItem(40503, 1)"));
        ordered(guardian, "attack.calcDamage();", "attack.action();", "attack.commit();");
        assertTrue(guardian.contains("player.getLawful() < -1000"));
        assertTrue(guardian.contains("new S_ChangeHeading(this)"));
        assertTrue(guardian.contains("set_stop_time(REST_MILLISEC)"));
        assertTrue(guardian.contains("(pc.getCurrentWeapon() == 0)"));

        ordered(player,
                "lastAttacker instanceof L1GuardianInstance",
                "setPkCountForElf(L1PcInstance.this.getPkCountForElf() - 1)",
                "setLastPkForElf(null)");
        ordered(player,
                "fightPc.isElf() && L1PcInstance.this.isElf()",
                "fightPc.setPkCountForElf(fightPc.getPkCountForElf() + 1)",
                "fightPc.setLastPkForElf()");
        assertTrue(player.contains("24 * 3600 * 1000"));
    }

    @Test
    void selectedDatabasePinsFiftyFourSpawnsDialogueRowsAndHarvestItems()
            throws Exception {
        String sql = readLegacy(DATABASE);
        List<String> spawns = sql.lines()
                .filter(line -> line.startsWith(
                        "INSERT INTO `spawnlist_npc` VALUES ("))
                .filter(line -> List.of(70846, 70848, 70850, 70851, 70852)
                        .stream().anyMatch(id -> line.contains(", " + id + ",")))
                .toList();
        assertEquals(54, spawns.size());
        assertEquals(10, count(spawns, 70846));
        assertEquals(19, count(spawns, 70848));
        assertEquals(10, count(spawns, 70850));
        assertEquals(14, count(spawns, 70851));
        assertEquals(1, count(spawns, 70852));
        assertTrue(spawns.stream().allMatch(row -> row.endsWith(", 4, 100);")));

        assertTrue(sql.contains(
                "INSERT INTO `npcaction` VALUES (70846, 'arachnee1', 'arachnee1', 'arachnem1', '');"));
        assertTrue(sql.contains(
                "INSERT INTO `npcaction` VALUES (70848, 'ente1', 'ente1', 'entm1', '');"));
        assertTrue(sql.contains(
                "INSERT INTO `npcaction` VALUES (70850, 'pane1', 'pane1', 'panm1', '');"));
        assertTrue(sql.contains(
                "INSERT INTO `npcaction` VALUES (70851, 'fairye1', 'fairye1', 'fairym1', '');"));
        assertTrue(sql.contains(
                "INSERT INTO `npcaction` VALUES (70852, 'fairyqe1', 'fairyqe1', 'fairyqm1', '');"));
        for (int itemId : List.of(40503, 40505, 40506, 40507, 40519)) {
            assertTrue(sql.contains(
                    "INSERT INTO `etcitem` VALUES (NULL, " + itemId + ","),
                    "missing selected harvest item " + itemId);
        }
    }

    @Test
    void selectedLiToReceiverUsesSayCodeCdshhInsteadOfRetainedTwOpcode()
            throws Exception {
        String asm = Files.readString(ASM, StandardCharsets.UTF_8);
        String registration = function(asm, "sub_41E620");
        int opcode = registration.indexOf("mov     [ebp+var_1], 2Dh ; '-'");
        int name = registration.indexOf("offset aSSayCode", opcode);
        assertTrue(opcode >= 0 && name > opcode);
        String receiver = function(asm, "sub_4A3140");
        assertTrue(receiver.contains("offset aCdshh"));
        assertEquals(0x2D, Protocol.S_SAY_CODE);
        assertEquals(0x1C, Protocol.S_CHANGE_DIRECTION);

        String protocol = Files.readString(Path.of(
                "src/main/java/local/lito381/server/Protocol.java"));
        assertTrue(protocol.contains(
                "new Writer().c(S_SAY_CODE).c(type).d(objectId).s(message).h(x).h(y)"));
        assertTrue(protocol.contains(
                "new Writer().c(S_CHANGE_DIRECTION).d(objectId).c(heading)"));
        assertFalse(protocol.contains("S_OPCODE_NPCSHOUT"));
    }

    private static long count(List<String> rows, int npcId) {
        return rows.stream().filter(row -> row.contains(", " + npcId + ",")).count();
    }

    private static void ordered(String source, String... fragments) {
        int position = -1;
        for (String fragment : fragments) {
            position = source.indexOf(fragment, position + 1);
            assertTrue(position >= 0,
                    "missing/out-of-order source fragment: " + fragment);
        }
    }

    private static String function(String source, String name) {
        int start = source.indexOf(name + " proc near");
        if (start < 0) start = source.indexOf(name);
        int end = source.indexOf(name + " endp", start);
        assertTrue(start >= 0 && end > start, "missing function " + name);
        return source.substring(start, end);
    }

    private static String readLegacy(Path path) throws Exception {
        byte[] bytes = Files.readAllBytes(path);
        for (Charset charset : List.of(StandardCharsets.UTF_8,
                Charset.forName("Big5"), StandardCharsets.UTF_16)) {
            try {
                return charset.newDecoder().decode(ByteBuffer.wrap(bytes)).toString();
            } catch (CharacterCodingException ignored) {
                // Try the next retained encoding.
            }
        }
        throw new IllegalArgumentException("unsupported retained source " + path);
    }

    private static String sha256(Path path) throws Exception {
        return HexFormat.of().withUpperCase().formatHex(
                MessageDigest.getInstance("SHA-256")
                        .digest(Files.readAllBytes(path)));
    }
}
