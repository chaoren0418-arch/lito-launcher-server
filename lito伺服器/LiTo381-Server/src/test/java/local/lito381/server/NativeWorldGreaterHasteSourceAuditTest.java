package local.lito381.server;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
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

/** Locks selected skill54 producer, source rows and BATTLE/WORLD lifecycle. */
final class NativeWorldGreaterHasteSourceAuditTest {
    @BeforeAll
    static void assumeLocalResourcesPresent() {
        LocalResources.assumeLineageSources();
        LocalResources.assumeLitoBin();
        LocalResources.assumeIdaClient(LocalResources.IDA_DISASSEMBLY_ABSOLUTE);
        LocalResources.assumeExisting("LiTo.unpacked", LocalResources.LITO_UNPACKED);
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
    void selectedAndSecondaryRowsRetainTheirIntentionalReuseDifference()
            throws Exception {
        String selected = read(RETAINED.resolve("db/380_db.sql"));
        String secondary = read(RETAINED.resolve(
                "其他src參考用/0507備份/yiwei380/db/380yiwei_db.sql"));
        assertEquals("INSERT INTO `skills` VALUES (54, 0, '強力加速術', 7, 5,"
                + " 60, 0, 0, 0, 10, 2400, 'none', 0, 0, 0, 0, 0, 0, 8,"
                + " 2, 0, 0, 0, 0, 32, '$1976', 19, 3104, 0, 184, 185, 0);",
                skillRow(selected, 54));
        assertEquals("INSERT INTO `skills` VALUES (54, '強力加速術', 7, 5, 60,"
                + " 0, 0, 0, 0, 2400, 'none', 0, 0, 0, 0, 0, 0, 8, 2, 0,"
                + " 0, 0, 0, 32, '$1976', 19, 3104, 0, 184, 185, 0);",
                skillRow(secondary, 54));
        for (String source : new String[] { selected, secondary }) {
            assertTrue(source.contains(
                    "INSERT INTO `skills_item` VALUES (54, '強力加速術', NULL, NULL);"));
        }
    }

    @Test
    void selectedLiToCase53IsSelectorOnlyAndUsesTenMillisecondClock()
            throws Exception {
        assertEquals("CE6EE68ADA8FD37E6C096B1A27E46CB6888F1179F85DEFB5007AC60287360292",
                sha256(SELECTED_CLIENT));
        String assembly = read(DISASSEMBLY).replace("\r\n", "\n");
        String producer = between(assembly, "loc_4CBBD4:", "loc_4CBBED:");
        for (String anchor : new String[] {
                "jumptable 004CB66E cases 1,2,12,13,30,31,48,51,53,71,77,81,96-98,100,101,103-105,108-110,113,114,116,118,119,128,129,133,136,137,145,146,149,153-155,158,160,161,163,168,170",
                "and     ecx, 7", "sar     esi, 3", "push    esi",
                "push    14h", "push    offset off_5028CC", "call    sub_4C6AE0"
        }) assertTrue(producer.contains(anchor), () -> "missing producer " + anchor);
        assertFalse(producer.contains("push    4"));
        assertFalse(producer.contains("push    8"));
        assertEquals("ccc\0", new String(
                peBytesAtVa(UNPACKED, 0x005028CCL, 4), StandardCharsets.US_ASCII));
        assertEquals(0, Byte.toUnsignedInt(
                peBytesAtVa(UNPACKED, 0x00635A44L + 53L, 1)[0]));
        assertEquals(10, readPeWordAtVa(UNPACKED, 0x00635774L + 53L * 2L));
    }

    @Test
    void catalogProtocolAndRetainedServerPinNativeSelfHasteContract()
            throws Exception {
        BattleSpellCatalog.SelfHasteSpell spell =
                BattleSpellCatalog.selfHasteSpell(BattleSpellCatalog.GREATER_HASTE);
        assertEquals(54, spell.skillId());
        assertEquals(6, spell.row());
        assertEquals(5, spell.column());
        assertEquals(19, spell.actionId());
        assertEquals(3104, spell.effectGfx());
        assertEquals(60, spell.mpCost());
        assertEquals(10, spell.reuseDelayMillis());
        assertEquals(2_400, spell.durationSeconds());
        assertEquals(184, spell.happenMessage());
        assertEquals(185, spell.stopMessage());
        assertEquals(60, spell.effectiveMpCost(12, 0, false));
        assertEquals(30, spell.effectiveMpCost(12, 0, true));

        byte[] request = new Protocol.Writer().c(Protocol.C_USE_SPELL)
                .c(spell.row()).c(spell.column()).bytes();
        assertEquals(3, request.length);
        assertEquals(spell.skillId(), Protocol.selectorOnlySpellRequest(request).skillId());
        assertArrayEquals(new byte[] {
                0x29, 0x03, 0, 0, 0, 0x01, 0x60, 0x09
        }, Protocol.speed(3, Protocol.SpeedMode.HASTE, 2_400));

        String useSkill = read(RETAINED.resolve(
                "src/com/lineage/server/model/skill/L1SkillUse.java"));
        for (String anchor : new String[] {
                "case GREATER_HASTE:// 強力加速術", "checkEquipped(20023)",
                "this._mpConsume = this._mpConsume >> 1",
                "this._skillId == GREATER_HASTE", "cha.getMoveSpeed() != 2",
                "pc.setDrink(false)",
                "new S_SkillHaste(pc.getId(), 1, this._getBuffIconDuration)",
                "cha.removeSkillEffect(GREATER_HASTE)", "cha.setMoveSpeed(0)"
        }) assertTrue(useSkill.contains(anchor), () -> "missing retained rule " + anchor);
        String stop = read(RETAINED.resolve(
                "src/com/lineage/server/model/skill/L1SkillStop.java"));
        assertTrue(stop.contains("case GREATER_HASTE: // 加速、高等加速"));
        assertTrue(stop.contains("new S_SkillHaste(pc.getId(), 0, 0)"));
    }

    @Test
    void sharedBattleWorldConsumerCommitsBeforeCleanupAndKeepsSlowBranchSoberRule()
            throws Exception {
        String session = read(Path.of(
                "src/main/java/local/lito381/server/ClientSession.java"));
        String worldDispatch = between(session,
                "BattleSpellCatalog.SelfHasteSpell worldSelfHaste",
                "BattleSpellCatalog.FreezeAttackSpell worldFreeze");
        assertTrue(worldDispatch.contains(
                "useSelfHasteSpell(payload, worldSelfHaste)"));
        String battleDispatch = between(session,
                "BattleSpellCatalog.SelfHasteSpell selfHasteSpell",
                "BattleSpellCatalog.CombatBuffSpell combatBuffSpell");
        assertTrue(battleDispatch.contains(
                "useSelfHasteSpell(payload, selfHasteSpell)"));

        String method = between(session, "private void useSelfHasteSpell(",
                "private void useCombatBuffSpell(");
        for (String anchor : new String[] {
                "Protocol.selectorOnlySpellRequest(payload)",
                "greaterHasteCostHalvingHelmEquipped()",
                "spellReadyNanos.put(spell.skillId()",
                "timedStatus.activateHaste(",
                "if (speedChange.type() == BattleTimedStatus.HASTE)",
                "timedStatus.clearDrunken()", "cancelMeditation(",
                "cancelAbsoluteBarrier(", "revealInvisibility(",
                "Protocol.objectAction(", "Protocol.effectObject(",
                "Protocol.messageCode(spell.happenMessage())",
                "broadcastMovementSpeed(", "sendHasteStopMessage("
        }) assertTrue(session.contains(anchor) || method.contains(anchor),
                () -> "missing consumer/lifecycle " + anchor);
        int mp = method.indexOf("playerMp -= mpCost");
        int reuse = method.indexOf("spellReadyNanos.put(spell.skillId()", mp);
        int lease = method.indexOf("timedStatus.activateHaste(", reuse);
        int drunkBranch = method.indexOf(
                "if (speedChange.type() == BattleTimedStatus.HASTE)", lease);
        int drunk = method.indexOf("timedStatus.clearDrunken()", drunkBranch);
        int meditation = method.indexOf("cancelMeditation(", drunk);
        int barrier = method.indexOf("cancelAbsoluteBarrier(", meditation);
        int invisible = method.indexOf("revealInvisibility(", barrier);
        int action = method.indexOf("Protocol.objectAction(", invisible);
        assertTrue(mp >= 0 && reuse > mp && lease > reuse
                && drunkBranch > lease && drunk > drunkBranch
                && meditation > drunk && barrier > meditation
                && invisible > barrier && action > invisible);
        assertFalse(method.contains("commitSpellReady("));
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
