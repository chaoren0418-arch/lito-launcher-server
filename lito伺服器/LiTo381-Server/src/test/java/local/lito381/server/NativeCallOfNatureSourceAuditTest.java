package local.lito381.server;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.BufferedReader;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/** Cross-source proof for native Elf skill165 Call of Nature. */
final class NativeCallOfNatureSourceAuditTest {
    @BeforeAll
    static void assumeLocalResourcesPresent() {
        LocalResources.assumeLineageSources();
        LocalResources.assumeLitoBin();
        LocalResources.assumeIdaClient(LocalResources.IDA_DISASSEMBLY_ABSOLUTE);
        LocalResources.assumeExisting("LiTo.unpacked", LocalResources.LITO_UNPACKED);
    }

    private static final Path RETAINED = Path.of("..", "1-伺服器")
            .toAbsolutePath().normalize();
    private static final Path PRIMARY_SQL = RETAINED.resolve("db/380_db.sql");
    private static final Path SECONDARY_SQL = RETAINED.resolve(
            "其他src參考用/0507備份/yiwei380/db/380yiwei_db.sql");
    private static final Path SELECTED_CLIENT = Path.of(
            "D:/380/天堂(Lineage 3.81C) (1)/LiTo.bin");
    private static final Path UNPACKED_CLIENT = Path.of(
            "D:/lineage開發/LiTo381-開發/runtime/LiTo.unpacked-oep-current.exe");
    private static final Path DISASSEMBLY = Path.of(
            "D:/lineage開發/LiTo381-開發/ida-client-oep-v2/full-disassembly.asm");

    @Test
    void retainedRowsPinWaterRestoreFullHpReagentAndNoReuse() throws Exception {
        String primary = read(PRIMARY_SQL);
        String secondary = read(SECONDARY_SQL);
        assertTrue(primary.contains("INSERT INTO `skills` VALUES (165, 0,"
                + " '生命呼喚', 21, 4, 50, 0, 40319, 1, 0, 0, 'buff', 3,"
                + " 0, 0, 0, 0, 0, 4, 32, 0, 10, 0, 0, 16, '', 19, 2245,"
                + " 0, 0, 0, 280);"));
        assertTrue(secondary.contains("INSERT INTO `skills` VALUES (165,"
                + " '生命呼喚', 21, 4, 50, 0, 40319, 1, 0, 0, 'buff', 3,"
                + " 0, 0, 0, 0, 0, 4, 32, 0, 10, 0, 0, 16, '', 19, 2245,"
                + " 0, 0, 0, 280);"));
        for (String sql : new String[] {primary, secondary}) {
            assertTrue(sql.contains("INSERT INTO `skills_item` VALUES"
                    + " (165, '生命呼喚', NULL, NULL);"));
            assertFalse(tableBlock(sql, "skills_buff_ability")
                    .contains("VALUES (165,"));
        }
    }

    @Test
    void selectedClientPinsObjectOnlyCase164AndZeroReuse() throws Exception {
        assertEquals("CE6EE68ADA8FD37E6C096B1A27E46CB6888F1179F85DEFB5007AC60287360292",
                sha256(SELECTED_CLIENT));
        String router = windowAfter(DISASSEMBLY, "loc_4CB71D:", 30);
        assertTrue(router.contains("cases 7,8,22,36,42,43,47,54,66,78,147,148,150,157,159,162,164,165,167,169"));
        assertTrue(router.contains("call    sub_4C9880"));
        String producer = function(DISASSEMBLY, "sub_4C9880 proc near",
                "sub_4C9880 endp");
        assertTrue(producer.contains("and     ecx, 7"));
        assertTrue(producer.contains("sar     esi, 3"));
        assertTrue(producer.contains("push    14h"));
        assertTrue(producer.contains("push    offset aCccd"));
        assertEquals(0, readPeWordAtVa(UNPACKED_CLIENT,
                0x635774L + (long) (BattleSpellCatalog.CALL_OF_NATURE - 1) * 2L));
    }

    @Test
    void retainedModesPinQuestion322FullHpNoGresAndNpcFullHp() throws Exception {
        String mode = read(RETAINED.resolve(
                "src/com/lineage/server/model/skill/skillmode/CALL_OF_NATURE.java"));
        assertTrue(mode.contains("pc.setTempID(srcpc.getId());"));
        assertTrue(mode.contains("new S_Message_YN(322)"));
        assertTrue(mode.contains("npc.resurrect(cha.getMaxHp())"));
        assertTrue(mode.contains("isCantResurrect()"));
        assertFalse(mode.contains("setGres(true)"));

        String answer = read(RETAINED.resolve(
                "src/com/lineage/server/clientpackets/C_Attr.java"));
        String question322 = answer.substring(answer.indexOf("case 322:"),
                answer.indexOf("case 325:"));
        assertTrue(question322.contains("pc.resurrect(pc.getMaxHp())"));
        assertTrue(question322.contains("pc.setCurrentHp(pc.getMaxHp())"));
        assertTrue(question322.contains("pc.isGres() && pc.isGresValid()"));

        String use = read(RETAINED.resolve(
                "src/com/lineage/server/model/skill/L1SkillUse.java"));
        assertTrue(use.contains("(_skillId != GREATER_RESURRECTION)"
                + " && (_skillId != CALL_OF_NATURE)"));
        assertTrue(use.contains("|| (_skillId == CALL_OF_NATURE)"));
        assertTrue(use.contains("_skill.getType() == L1Skills.TYPE_RESTORE"));
    }

    @Test
    void selectedCantResurrectColumnIsReproducedExactly() throws Exception {
        assertEquals(ClassicWorldResurrectionCatalog.SOURCE_SHA256,
                sha256(PRIMARY_SQL));
        Pattern row = Pattern.compile(
                "^INSERT INTO `npc` VALUES \\((\\d+),.*?,\\s*(\\d+),\\s*\\d+\\);$",
                Pattern.MULTILINE);
        Matcher matcher = row.matcher(read(PRIMARY_SQL));
        int matched = 0;
        int blocked = 0;
        while (matcher.find()) {
            int npcId = Integer.parseInt(matcher.group(1));
            boolean sourceBlocked = Integer.parseInt(matcher.group(2)) == 1;
            assertEquals(sourceBlocked,
                    ClassicWorldResurrectionCatalog.blocked(npcId),
                    "cant_resurrect mismatch npc=" + npcId);
            assertEquals(!sourceBlocked,
                    ClassicWorldResurrectionCatalog.allowsSelectedTemplate(
                            npcId, true));
            matched++;
            if (sourceBlocked) blocked++;
        }
        assertEquals(4_138, matched);
        assertEquals(ClassicWorldResurrectionCatalog.BLOCKED_COUNT, blocked);
        assertFalse(ClassicWorldResurrectionCatalog.allowsSelectedTemplate(
                999_999, false));
    }

    @Test
    void implementationRoutesBothPhasesAndKeepsExperienceDisabled()
            throws Exception {
        String session = read(Path.of(
                "src/main/java/local/lito381/server/ClientSession.java"));
        assertTrue(session.contains("useCallOfNatureSpell(payload"));
        assertTrue(session.contains("originPhase != Phase.BATTLE"
                + " && originPhase != Phase.WORLD"));
        assertTrue(session.contains("ClassicWorldResurrectionCatalog"
                + ".allowsSelectedTemplate"));
        assertTrue(session.contains("spell.restoredNpcHp"));
        assertTrue(session.contains("Protocol.greaterResurrectionAsk()"));
        BattleSpellCatalog.ResurrectionSpell spell =
                BattleSpellCatalog.resurrectionSpell(
                        BattleSpellCatalog.CALL_OF_NATURE);
        assertEquals(322, spell.questionId());
        assertFalse(spell.restoresExperience());
        assertEquals(1, spell.npcHpDivisor());
    }

    private static int readPeWordAtVa(Path path, long virtualAddress)
            throws Exception {
        byte[] bytes = Files.readAllBytes(path);
        ByteBuffer image = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN);
        int pe = image.getInt(0x3C);
        assertEquals(0x0000_4550, image.getInt(pe));
        int sectionCount = Short.toUnsignedInt(image.getShort(pe + 6));
        int optionalSize = Short.toUnsignedInt(image.getShort(pe + 20));
        int optional = pe + 24;
        long imageBase = Integer.toUnsignedLong(image.getInt(optional + 28));
        long rva = virtualAddress - imageBase;
        int sections = optional + optionalSize;
        for (int index = 0; index < sectionCount; index++) {
            int section = sections + index * 40;
            long virtualSize = Integer.toUnsignedLong(image.getInt(section + 8));
            long sectionRva = Integer.toUnsignedLong(image.getInt(section + 12));
            long rawSize = Integer.toUnsignedLong(image.getInt(section + 16));
            long rawOffset = Integer.toUnsignedLong(image.getInt(section + 20));
            if (rva >= sectionRva
                    && rva + 2 <= sectionRva + Math.max(virtualSize, rawSize)) {
                return Short.toUnsignedInt(image.getShort(
                        Math.toIntExact(rawOffset + rva - sectionRva)));
            }
        }
        throw new IllegalArgumentException("unmapped PE virtual address");
    }

    private static String function(Path path, String start, String end)
            throws Exception {
        String text = read(path);
        int from = text.indexOf(start);
        int to = text.indexOf(end, from);
        assertTrue(from >= 0 && to > from);
        return text.substring(from, to);
    }

    private static String windowAfter(Path path, String marker, int lines)
            throws Exception {
        StringBuilder result = new StringBuilder();
        boolean found = false;
        try (BufferedReader reader = Files.newBufferedReader(
                path, StandardCharsets.UTF_8)) {
            for (String line; (line = reader.readLine()) != null;) {
                if (!found && line.contains(marker)) found = true;
                if (found) {
                    result.append(line).append('\n');
                    if (lines-- == 0) break;
                }
            }
        }
        assertTrue(found);
        return result.toString();
    }

    private static String tableBlock(String sql, String table) {
        String marker = "-- Table structure for " + table;
        int start = sql.indexOf(marker);
        assertTrue(start >= 0);
        int end = sql.indexOf("-- Table structure for ", start + marker.length());
        return sql.substring(start, end < 0 ? sql.length() : end);
    }

    private static String read(Path path) throws Exception {
        assertTrue(Files.isRegularFile(path), "missing source evidence: " + path);
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
