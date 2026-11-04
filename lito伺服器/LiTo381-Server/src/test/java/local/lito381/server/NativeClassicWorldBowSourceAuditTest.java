package local.lito381.server;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.List;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/** Cross-source proof for the native map2 L1Bow fixed-arrow apertures. */
final class NativeClassicWorldBowSourceAuditTest {
    @BeforeAll
    static void assumeLocalResourcesPresent() {
        LocalResources.assumeLineageSources();
        LocalResources.assumeLitoBin();
        LocalResources.assumeIdaClient(LocalResources.IDA_DISASSEMBLY_ABSOLUTE);
    }

    private static final Path RETAINED = Path.of("..", "1-伺服器")
            .toAbsolutePath().normalize();
    private static final Path CURRENT = RETAINED.resolve("src/com/lineage/server");
    private static final Path OLDER = RETAINED.resolve(
            "其他src參考用/381n8src/com/lineage/server");
    private static final Path PRIMARY = RETAINED.resolve("db/380_db.sql");
    private static final Path SELECTED_CLIENT = Path.of(
            "D:/380/天堂(Lineage 3.81C) (1)/LiTo.bin");
    private static final Path DISASSEMBLY = Path.of(
            "D:/lineage開發/LiTo381-開發/ida-client-oep-v2/full-disassembly.asm");

    @Test
    void selectedReceiverPinsOneByteDamageAndExtendedProjectileTail()
            throws Exception {
        assertEquals("CE6EE68ADA8FD37E6C096B1A27E46CB6888F1179F85DEFB5007AC60287360292",
                sha256(SELECTED_CLIENT));
        String asm = read(DISASSEMBLY, StandardCharsets.UTF_8)
                .replace("\r\n", "\n");
        String receiver = function(asm, "sub_4A5ED0");
        assertTrue(receiver.contains("offset aCddccd                  ; \"cddccd\""));
        assertTrue(receiver.contains("offset aHchhhhcs                ; \"hchhhhcs\""));
        assertTrue(receiver.indexOf("offset aCddccd")
                < receiver.indexOf("offset aHchhhhcs"));
        // c d d c c d means damage is the first byte after both object IDs.
        assertEquals(29, Protocol.classicWorldBowAttack(
                0x6400_02A7L, 0, 4, 32678, 32856, 32678, 32859).length);
    }

    @Test
    void selectedDatabasePinsTemplateAndExactlyTwentyNineMapTwoRows()
            throws Exception {
        assertEquals("DA185BCA20CC254CB175EA2C367A2F5270514F4957B3AC99343C6101A2593766",
                sha256(PRIMARY));
        String sql = readLegacy(PRIMARY);
        assertTrue(sql.contains("INSERT INTO `npc` VALUES (70558, '攻擊箭孔', '', '0', '', 'L1Bow',"));
        List<String> rows = sql.lines()
                .filter(line -> line.startsWith("INSERT INTO `spawnlist_npc` VALUES ("))
                .filter(line -> line.contains(", 70558,"))
                .toList();
        assertEquals(29, rows.size());
        assertEquals(List.of(
                679, 680, 681, 682, 683, 684, 685, 686, 687, 688,
                689, 690, 691, 692, 693, 694, 695, 696, 697, 698,
                699, 700, 701, 702, 703, 704, 705, 706, 709),
                rows.stream().map(NativeClassicWorldBowSourceAuditTest::spawnId)
                        .toList());
        assertTrue(rows.stream().allMatch(line -> line.endsWith(", 2, 0);")));
    }

    @Test
    void retainedImplementationsPinHiddenActorTimerTargetFamiliesAndProjectile()
            throws Exception {
        Path currentBow = CURRENT.resolve("model/Instance/L1BowInstance.java");
        Path currentTimer = CURRENT.resolve("timecontroller/npc/NpcBowTimer.java");
        Path currentPacket = CURRENT.resolve("serverpackets/S_UseArrowSkill.java");
        Path olderBow = OLDER.resolve("model/Instance/L1BowInstance.java");
        Path olderTimer = OLDER.resolve("timecontroller/npc/NpcBowTimer.java");
        Path olderPacket = OLDER.resolve("serverpackets/S_UseArrowSkill.java");
        assertEquals(ClassicWorldBowCatalog.CURRENT_BOW_SOURCE_SHA256,
                sha256(currentBow));
        assertEquals(ClassicWorldBowCatalog.CURRENT_TIMER_SOURCE_SHA256,
                sha256(currentTimer));
        assertEquals("870D1757E281E346A635C52E6F3B4CF8B5DC22A06959FF5C88A1029BC6A0A769",
                sha256(currentPacket));
        assertEquals(ClassicWorldBowCatalog.OLDER_BOW_SOURCE_SHA256,
                sha256(olderBow));
        assertEquals(ClassicWorldBowCatalog.OLDER_TIMER_SOURCE_SHA256,
                sha256(olderTimer));
        assertEquals("6103A129E9508086109E40D06F85E274FF83D2593B6AEDEEDAF808AF9457C2BF",
                sha256(olderPacket));

        String bow = readLegacy(currentBow);
        String timer = readLegacy(currentTimer);
        String packet = readLegacy(currentPacket);
        assertTrue(bow.contains("private int _bowid = 66"));
        assertTrue(bow.contains("private int _dmg = 15"));
        assertTrue(bow.contains("public void onPerceive"));
        assertTrue(!function(bow, "onPerceive").contains("super.onPerceive"));
        assertTrue(bow.contains("trag.receiveDamage(null, _dmg"));
        assertTrue(bow.contains("cha instanceof L1PcInstance"));
        assertTrue(bow.contains("cha instanceof L1PetInstance"));
        assertTrue(bow.contains("cha instanceof L1SummonInstance"));
        assertTrue(bow.contains("cha instanceof L1MonsterInstance"));
        assertTrue(bow.contains("getOriginalTile(x, y--)"));
        assertTrue(bow.contains("getOriginalTile(x++, y)"));
        assertTrue(bow.contains("getOriginalTile(x, y++)"));
        assertTrue(bow.contains("getOriginalTile(x--, y)"));
        assertTrue(timer.contains("final int timeMillis = 1500"));
        assertTrue(timer.contains("if (bowNpc.checkPc())"));
        assertTrue(timer.contains("bowNpc.atkTrag()"));
        assertTrue(packet.contains("this.writeD(0x00000152)"));
        assertTrue(packet.contains("this.writeH(spellgfx)"));
        assertTrue(packet.contains("this.writeC(0x7f)"));
    }

    @Test
    void implementationKeepsSelectedClientWidthAndIndependentDamageFirstClock()
            throws Exception {
        String protocol = read(Path.of(
                "src/main/java/local/lito381/server/Protocol.java"),
                StandardCharsets.UTF_8);
        String hub = read(Path.of(
                "src/main/java/local/lito381/server/GameHub.java"),
                StandardCharsets.UTF_8);
        String session = read(Path.of(
                "src/main/java/local/lito381/server/ClientSession.java"),
                StandardCharsets.UTF_8);
        String catalog = read(Path.of(
                "src/main/java/local/lito381/server/ClassicWorldBowCatalog.java"),
                StandardCharsets.UTF_8);
        assertTrue(protocol.contains(".c(ClassicWorldBowCatalog.DAMAGE).c(heading)"));
        assertTrue(protocol.contains(".d(ClassicWorldBowCatalog.NATIVE_PROJECTILE_SEQUENCE)"));
        assertTrue(catalog.contains("TICK_INTERVAL_MILLIS = 1_500"));
        assertTrue(catalog.contains("PROJECTILE_GFX = 66"));
        assertTrue(hub.contains("scheduleWithFixedDelay(this::classicWorldBowTickSafely"));
        assertTrue(hub.indexOf("serverClassicWorldBowPlayerDamage(bow)")
                < hub.indexOf("serverClassicWorldBowProjectile("));
        assertTrue(session.contains("serverClassicWorldBowPlayerDamage("));
        assertTrue(session.contains("serverClassicWorldBowCompanionDamage("));
        assertTrue(session.contains("serverClassicWorldBowMonsterDamage("));
        assertTrue(session.contains("serverClassicWorldBowProjectile("));
    }

    private static int spawnId(String row) {
        int start = row.indexOf('(') + 1;
        return Integer.parseInt(row.substring(start, row.indexOf(',', start)));
    }

    private static String function(String source, String name) {
        int start = source.indexOf(name);
        assertTrue(start >= 0, "missing function: " + name);
        int asmEnd = source.indexOf(name + " endp", start);
        if (asmEnd >= 0) return source.substring(start, asmEnd + name.length() + 5);
        int nextMethod = source.indexOf("\n\tpublic ", start + name.length());
        if (nextMethod < 0) nextMethod = source.indexOf("\n    public ", start + name.length());
        return source.substring(start, nextMethod < 0 ? source.length() : nextMethod);
    }

    private static String readLegacy(Path path) throws Exception {
        byte[] bytes = Files.readAllBytes(path);
        for (Charset charset : List.of(StandardCharsets.UTF_8,
                Charset.forName("Big5"), StandardCharsets.UTF_16)) {
            try {
                return charset.newDecoder().decode(java.nio.ByteBuffer.wrap(bytes)).toString();
            } catch (java.nio.charset.CharacterCodingException ignored) {
                // Try the next retained-source encoding.
            }
        }
        throw new IllegalArgumentException("unsupported retained source: " + path);
    }

    private static String read(Path path, Charset charset) throws Exception {
        assertTrue(Files.isRegularFile(path), "missing source evidence: " + path);
        return Files.readString(path, charset);
    }

    private static String sha256(Path path) throws Exception {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        try (var stream = Files.newInputStream(path)) {
            byte[] buffer = new byte[64 * 1024];
            for (int count; (count = stream.read(buffer)) >= 0;) {
                if (count > 0) digest.update(buffer, 0, count);
            }
        }
        return HexFormat.of().withUpperCase().formatHex(digest.digest());
    }
}
