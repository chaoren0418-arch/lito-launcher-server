package local.lito381.server;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.BufferedReader;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.HexFormat;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/** Cross-source proof for native LiTo selector19/1 and retained skill154. */
final class NativeLesserElementalSourceAuditTest {
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

    @Test void databasesPinSelectorResourcesAndFourSelectedNpcTemplates()
            throws Exception {
        String primary = read(PRIMARY_SQL);
        String secondary = read(SECONDARY_SQL);
        assertTrue(primary.contains("INSERT INTO `skills` VALUES (154, 0,"
                + " '召喚屬性精靈', 20, 1, 20, 0, 40319, 2, 0, 0, 'none',"
                + " 0, 0, 0, 0, 0, 0, 0, 128, 0, 0, 0, 0, 2, '', 19,"
                + " 2510, 0, 0, 0, 0);"));
        assertTrue(secondary.contains("INSERT INTO `skills` VALUES (154,"
                + " '召喚屬性精靈', 20, 1, 20, 0, 40319, 2, 0, 0, 'none',"
                + " 0, 0, 0, 0, 0, 0, 0, 128, 0, 0, 0, 0, 2, '', 19,"
                + " 2510, 0, 0, 0, 0);"));
        for (String sql : new String[] {primary, secondary}) {
            assertTrue(sql.contains("INSERT INTO `skills_item` VALUES"
                    + " (154, '召喚屬性精靈', NULL, NULL);"));
            assertTrue(sql.contains("INSERT INTO `npc` VALUES (45306, '土之精靈',"
                    + " '$2007', '0', '召喚屬性精靈', 'L1Monster', 2273, 30, 400"));
            assertTrue(sql.contains("INSERT INTO `npc` VALUES (45303, '火之精靈',"
                    + " '$2004', '0', '召喚屬性精靈', 'L1Monster', 2276, 30, 400"));
            assertTrue(sql.contains("INSERT INTO `npc` VALUES (45304, '水之精靈',"
                    + " '$2005', '0', '召喚屬性精靈', 'L1Monster', 2274, 30, 400"));
            assertTrue(sql.contains("INSERT INTO `npc` VALUES (45305, '風之精靈',"
                    + " '$2006', '0', '召喚屬性精靈', 'L1Monster', 2275, 30, 400"));
        }
    }

    @Test void retainedModePinsAttributePriorityMapGatePetCostAndSpawn()
            throws Exception {
        String mode = read(RETAINED.resolve(
                "src/com/lineage/server/model/skill/skillmode/LESSER_ELEMENTAL.java"));
        assertTrue(mode.contains("final int attr = pc.getElfAttr();"));
        assertTrue(mode.contains("if (attr != 0)"));
        assertTrue(mode.contains("if (!pc.getMap().isRecallPets())"));
        assertTrue(mode.contains("new S_ServerMessage(353)"));
        assertTrue(mode.contains("if (petcost == 0)"));
        assertTrue(mode.contains("if ((attr & 1) != 0)"));
        assertTrue(mode.contains("summonid = 45306"));
        assertTrue(mode.contains("else if ((attr & 2) != 0)"));
        assertTrue(mode.contains("summonid = 45303"));
        assertTrue(mode.contains("else if ((attr & 4) != 0)"));
        assertTrue(mode.contains("summonid = 45304"));
        assertTrue(mode.contains("else if ((attr & 8) != 0)"));
        assertTrue(mode.contains("summonid = 45305"));
        assertTrue(mode.contains("new L1SummonInstance(npcTemp, pc)"));
        assertTrue(mode.contains("summon.setPetcost(pc.getCha() + 7)"));

        String summon = read(RETAINED.resolve(
                "src/com/lineage/server/model/Instance/L1SummonInstance.java"));
        assertTrue(summon.contains("this.set_time(1800);"));
        assertTrue(summon.contains("this.setMoveSpeed(1)"));
        assertTrue(summon.contains("new S_SkillSound(getId(), 755)"));
    }

    @Test void selectedLiToPinsCase153SelectorOnlyAndZeroCastClock()
            throws Exception {
        assertEquals("CE6EE68ADA8FD37E6C096B1A27E46CB6888F1179F85DEFB5007AC60287360292",
                sha256(SELECTED_CLIENT));
        String selector = windowAfter(DISASSEMBLY, "loc_4CBBD4:", 24);
        assertTrue(selector.contains("cases 1,2,12,13,30,31,48,51,53,71,77,81,96-98,100,101,103-105,108-110,113,114,116,118,119,128,129,133,136,137,145,146,149,153-155,158,160,161,163,168,170"));
        assertTrue(selector.contains("and     ecx, 7"));
        assertTrue(selector.contains("sar     esi, 3"));
        assertTrue(selector.contains("push    14h"));
        assertTrue(selector.contains("call    sub_4C6AE0"));
        assertEquals(0, readPeWordAtVa(UNPACKED_CLIENT,
                0x635774L + (long) (BattleSpellCatalog.LESSER_ELEMENTAL - 1) * 2L));
    }

    @Test void implementationRoutesBothPhasesAndUsesSourceCompanionAuthority()
            throws Exception {
        String session = read(Path.of(
                "src/main/java/local/lito381/server/ClientSession.java"));
        assertEquals(2, occurrences(session,
                "BattleSpellCatalog.elementalSummonSpell(selector.skillId())"));
        assertTrue(session.contains("private void useElementalSummonSpell("));
        assertTrue(session.contains("Protocol.selectorOnlySpellRequest(payload)"));
        assertTrue(session.contains("spell.summonNpcId(elfAttribute)"));
        assertTrue(session.contains("usedPetCost != 0"));
        assertTrue(session.contains("BattleCompanion.lesserElemental("));
        String companion = read(Path.of(
                "src/main/java/local/lito381/server/BattleCompanion.java"));
        assertTrue(companion.contains("static BattleCompanion lesserElemental("));
        assertTrue(companion.contains("BattleTimedStatus.HASTE, petCost"));
        String accounts = read(Path.of(
                "src/main/java/local/lito381/server/Accounts.java"));
        assertTrue(accounts.contains("case 45303 -> source"));
        assertTrue(accounts.contains("case 45306 -> source"));
    }

    private static int occurrences(String text, String needle) {
        int count = 0;
        for (int at = 0; (at = text.indexOf(needle, at)) >= 0;
             at += needle.length()) count++;
        return count;
    }

    private static int readPeWordAtVa(Path path, long virtualAddress)
            throws Exception {
        byte[] bytes = Files.readAllBytes(path);
        ByteBuffer image = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN);
        int pe = image.getInt(0x3C);
        assertEquals(0x0000_4550, image.getInt(pe), "invalid PE signature");
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
                int fileOffset = Math.toIntExact(rawOffset + rva - sectionRva);
                return Short.toUnsignedInt(image.getShort(fileOffset));
            }
        }
        throw new IllegalArgumentException("unmapped PE virtual address");
    }

    private static String read(Path path) throws Exception {
        assertTrue(Files.isRegularFile(path), "missing source evidence: " + path);
        return Files.readString(path, StandardCharsets.UTF_8);
    }

    private static String windowAfter(Path path, String marker, int followingLines)
            throws Exception {
        StringBuilder result = new StringBuilder();
        boolean found = false;
        int remaining = 0;
        try (BufferedReader reader = Files.newBufferedReader(
                path, StandardCharsets.UTF_8)) {
            for (String line; (line = reader.readLine()) != null;) {
                if (!found && line.contains(marker)) {
                    found = true;
                    remaining = followingLines;
                }
                if (found) {
                    result.append(line).append('\n');
                    if (remaining-- == 0) break;
                }
            }
        }
        assertTrue(found, "missing IDA marker: " + marker);
        return result.toString();
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
