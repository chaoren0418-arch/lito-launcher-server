package local.lito381.server;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.HexFormat;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/** Locks selected skill60 producer, DB rows and the shared visibility lifecycle. */
final class NativeWorldInvisibilitySkill60SourceAuditTest {
    @BeforeAll
    static void assumeLocalResourcesPresent() {
        LocalResources.assumeLineageSources();
        LocalResources.assumeLitoBin();
        LocalResources.assumeIdaClient();
    }

    private static final Path RETAINED = Path.of("..", "1-伺服器")
            .toAbsolutePath().normalize();
    private static final Path SELECTED_CLIENT = Path.of(
            "D:/380/天堂(Lineage 3.81C) (1)/LiTo.bin");
    private static final Path UNPACKED = Path.of(
            "D:/lineage開發/LiTo381-開發/runtime/LiTo.unpacked-oep-current.exe");
    private static final Path DISASSEMBLY = Path.of(
            "D:/lineage開發/LiTo381-開發/ida-client-oep-v2/full-disassembly.asm");

    @Test
    void selectedAndSecondaryRowsAgreeOnNativeInvisibilityContract()
            throws Exception {
        String selected = read(RETAINED.resolve("db/380_db.sql"));
        String secondary = read(RETAINED.resolve(
                "其他src參考用/0507備份/yiwei380/db/380yiwei_db.sql"));
        assertEquals("INSERT INTO `skills` VALUES (60, 0, '隱身術', 8, 3,"
                + " 15, 0, 40318, 1, 0, 0, 'none', 0, 0, 0, 0, 0, 0,"
                + " 0, 2, 0, 0, 0, 0, 8, '$1474', 19, 0, 0, 0, 0, 0);",
                skillRow(selected, 60));
        assertEquals("INSERT INTO `skills` VALUES (60, '隱身術', 8, 3, 15,"
                + " 0, 40318, 1, 0, 0, 'none', 0, 0, 0, 0, 0, 0, 0, 2,"
                + " 0, 0, 0, 0, 8, '$1474', 19, 0, 0, 0, 0, 0);",
                skillRow(secondary, 60));
        assertTrue(selected.contains(
                "INSERT INTO `skills_item` VALUES (60, '隱身術', NULL, NULL);"));
        assertTrue(secondary.contains(
                "INSERT INTO `skills_item` VALUES (60, '隱身術', NULL, NULL);"));
    }

    @Test
    void selectedLiToCase59EmitsExactSelfTickAndZeroLocalClock()
            throws Exception {
        assertEquals("CE6EE68ADA8FD37E6C096B1A27E46CB6888F1179F85DEFB5007AC60287360292",
                sha256(SELECTED_CLIENT));
        String assembly = read(DISASSEMBLY).replace("\r\n", "\n");
        String producer = between(assembly, "loc_4CBAAD:", "loc_4CBACB:");
        for (String anchor : new String[] {
                "jumptable 004CB66E case 59", "call    sub_4D69F0",
                "push    eax", "push    3", "push    7", "push    14h",
                "push    offset aCccd", "call    sub_4C6AE0",
                "jmp     loc_4CBBED"
        }) assertTrue(producer.contains(anchor), () -> "missing producer " + anchor);
        assertEquals("cccd\0", new String(
                peBytesAtVa(UNPACKED, 0x00502CE0L, 5), StandardCharsets.US_ASCII));
        assertEquals(0, readPeWordAtVa(UNPACKED, 0x00635774L + 59L * 2L));
    }

    @Test
    void catalogAndProtocolPinTheIndefiniteMaterialSelfTickContract() {
        BattleSpellCatalog.InvisibilitySpell spell =
                BattleSpellCatalog.invisibilitySpell(BattleSpellCatalog.INVISIBILITY);
        assertEquals(60, spell.skillId());
        assertEquals(7, spell.row());
        assertEquals(3, spell.column());
        assertEquals(15, spell.mpCost());
        assertEquals(0, spell.reuseDelayMillis());
        assertEquals(40318, spell.itemConsumeId());
        assertEquals(1, spell.itemConsumeCount());
        assertEquals(19, spell.actionId());
        assertEquals(0, spell.effectGfx());
        assertEquals(0, spell.durationSeconds());
        assertEquals(BattleSpellCatalog.RequestShape.SELF_TICK,
                spell.requestShape());

        byte[] request = new Protocol.Writer().c(Protocol.C_USE_SPELL)
                .c(spell.row()).c(spell.column()).d(0x1122_3344L).bytes();
        Protocol.SelfSpellRequest parsed = Protocol.selfSpellRequest(request);
        assertEquals(spell.skillId(), parsed.skillId());
        assertEquals(0x1122_3344L, parsed.clientTick());
        assertEquals(7, request.length);
        assertEquals(6, Protocol.invisible(3, 1).length);
        assertEquals(5, Protocol.removeObject(3).length);
    }

    @Test
    void sharedBattleAndWorldPathCommitsThenTogglesAndPublishesNativeAoi()
            throws Exception {
        String session = read(Path.of(
                "src/main/java/local/lito381/server/ClientSession.java"));
        String method = between(session, "private void useInvisibilitySpell(",
                "private void useEnchantVenomSpell(");
        for (String anchor : new String[] {
                "Protocol.selfSpellRequest(payload)",
                "request.clientTick()", "nextInventory.removeCatalog(",
                "spell.itemConsumeId(), spell.itemConsumeCount()",
                "toggleOff = previouslyInvisible",
                "invisibilityStatus.skillId() == spell.skillId()",
                "activateInvisibilityState(spell.skillId(), now, spell.durationSeconds())",
                "hub.clearClassicWorldMonsterTargets(playerObjectId())",
                "publishInvisibilityReveal(\"invisibility-recast-\" + spell.skillId())",
                "Protocol.invisible(playerObjectId(), 1)",
                "Protocol.removeObject(playerObjectId())",
                "BattleSpellCatalog.isInNativeScreen("
        }) assertTrue(method.contains(anchor), () -> "missing lifecycle " + anchor);

        int material = method.indexOf("inventory = nextInventory");
        int mp = method.indexOf("playerMp -= mpCost", material);
        int reuse = method.indexOf("commitSpellReady(spell.skillId()", mp);
        int interrupted = method.indexOf("cancelMeditation(\"spell-\"", reuse);
        int prior = method.indexOf("previouslyInvisible = invisible", interrupted);
        int cancel = method.indexOf("cancelInvisibilityState()", prior);
        int activate = method.indexOf("activateInvisibilityState(", cancel);
        int inventoryPublish = method.indexOf("sendInventoryDelta(", activate);
        int reveal = method.indexOf("publishInvisibilityReveal(", inventoryPublish);
        int action = method.indexOf("new Protocol.Writer().c(Protocol.S_ACTION)", reveal);
        int remove = method.indexOf("Protocol.removeObject(", action);
        int mana = method.indexOf("Protocol.manaPoint(", remove);
        assertTrue(material >= 0 && mp > material && reuse > mp
                && interrupted > reuse && prior > interrupted && cancel > prior
                && activate > cancel && inventoryPublish > activate
                && reveal > inventoryPublish && action > reveal
                && remove > action && mana > remove);
        assertFalse(method.contains("Protocol.objectSpellRequest(payload)"));

        String revealMethod = between(session,
                "private void publishInvisibilityReveal(",
                "private void serverClassicWorldCompanionTick(");
        assertTrue(revealMethod.contains("Protocol.invisible(playerObjectId(), 0)"));
        assertTrue(revealMethod.contains("recipient.sendBattleObject("));
        assertTrue(revealMethod.contains("BattleSpellCatalog.isInNativeScreen("));
        assertTrue(revealMethod.contains("Protocol.battleClanMetadata("));
        assertTrue(revealMethod.contains("Protocol.poison(playerObjectId(), palette)"));
    }

    private static String skillRow(String sql, int skillId) {
        String prefix = "INSERT INTO `skills` VALUES (" + skillId + ",";
        return sql.lines().filter(line -> line.startsWith(prefix))
                .findFirst().orElseThrow();
    }

    private static String between(String text, String start, String end) {
        int from = text.indexOf(start);
        int to = text.indexOf(end, from + start.length());
        assertTrue(from >= 0 && to > from,
                () -> "missing source boundary " + start + " -> " + end);
        return text.substring(from, to);
    }

    private static byte[] peBytesAtVa(Path path, long va, int length)
            throws Exception {
        byte[] bytes = Files.readAllBytes(path);
        ByteBuffer image = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN);
        int pe = image.getInt(0x3C);
        int sections = Short.toUnsignedInt(image.getShort(pe + 6));
        int optionalSize = Short.toUnsignedInt(image.getShort(pe + 20));
        int optional = pe + 24;
        long imageBase = Integer.toUnsignedLong(image.getInt(optional + 28));
        long rva = va - imageBase;
        int table = optional + optionalSize;
        for (int index = 0; index < sections; index++) {
            int section = table + index * 40;
            long virtualSize = Integer.toUnsignedLong(image.getInt(section + 8));
            long sectionRva = Integer.toUnsignedLong(image.getInt(section + 12));
            long rawSize = Integer.toUnsignedLong(image.getInt(section + 16));
            long rawOffset = Integer.toUnsignedLong(image.getInt(section + 20));
            if (rva >= sectionRva && rva + length <= sectionRva
                    + Math.max(virtualSize, rawSize)) {
                int offset = Math.toIntExact(rawOffset + rva - sectionRva);
                return java.util.Arrays.copyOfRange(bytes, offset, offset + length);
            }
        }
        throw new IllegalArgumentException("unmapped PE VA " + Long.toHexString(va));
    }

    private static int readPeWordAtVa(Path path, long va) throws Exception {
        return Short.toUnsignedInt(ByteBuffer.wrap(peBytesAtVa(path, va, 2))
                .order(ByteOrder.LITTLE_ENDIAN).getShort());
    }

    private static String read(Path path) throws Exception {
        assertTrue(Files.isRegularFile(path), "missing source: " + path);
        return Files.readString(path, StandardCharsets.UTF_8);
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
