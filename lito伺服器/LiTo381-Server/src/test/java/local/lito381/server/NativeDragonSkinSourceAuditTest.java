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

/** Cross-source proof for native Dragon Knight skill181 Dragon Skin. */
final class NativeDragonSkinSourceAuditTest {
    @BeforeAll
    static void assumeLocalResourcesPresent() {
        LocalResources.assumeLineageSources();
        LocalResources.assumeLitoBin();
        LocalResources.assumeIdaClient();
    }

    private static final Path RETAINED = Path.of("..", "1-伺服器")
            .toAbsolutePath().normalize();
    private static final Path PRIMARY = RETAINED.resolve("db/380_db.sql");
    private static final Path SECONDARY = RETAINED.resolve(
            "其他src參考用/0507備份/yiwei380/db/380yiwei_db.sql");
    private static final Path SELECTED_CLIENT = Path.of(
            "D:/380/天堂(Lineage 3.81C) (1)/LiTo.bin");
    private static final Path UNPACKED = Path.of(
            "D:/lineage開發/LiTo381-開發/runtime/LiTo.unpacked-oep-current.exe");
    private static final Path DISASSEMBLY = Path.of(
            "D:/lineage開發/LiTo381-開發/ida-client-oep-v2/full-disassembly.asm");

    @Test void databasesSeparateFourReservedSlotsFromExactSkill181()
            throws Exception {
        String primary = read(PRIMARY);
        String secondary = read(SECONDARY);
        for (int skillId = 177; skillId <= 180; skillId++) {
            String primaryRow = skillRow(primary, skillId);
            String secondaryRow = skillRow(secondary, skillId);
            assertTrue(primaryRow.contains("'none', 0, " + (skillId - 177)));
            assertTrue(secondaryRow.contains("'none', 0, " + (skillId - 177)));
            assertTrue(primaryRow.endsWith(", 0, 0, 0, 0, 0, 0);"));
            assertTrue(secondaryRow.endsWith(", 0, 0, 0, 0, 0, 0);"));
        }
        assertEquals("INSERT INTO `skills` VALUES (181, 0, '龍之護鎧', 23, 4,"
                + " 0, 15, 0, 0, 10, 1200, 'none', 0, 0, 0, 0, 0, 0, 0,"
                + " 2, 0, 0, 0, 0, 16, '$5920', 19, 6531, 0, 0, 0, 0);",
                skillRow(primary, 181));
        assertEquals("INSERT INTO `skills` VALUES (181, '龍之護鎧', 23, 4,"
                + " 0, 15, 0, 0, 0, 1200, 'none', 0, 0, 0, 0, 0, 0, 0,"
                + " 2, 0, 0, 0, 0, 16, '$5920', 19, 6531, 0, 0, 0, 0);",
                skillRow(secondary, 181));
        assertTrue(primary.contains(
                "INSERT INTO `skills_item` VALUES (181, '龍之護鎧', NULL, NULL);"));
        assertFalse(primary.contains(
                "INSERT INTO `skills_相消免疫技能` VALUES (181,"));
    }

    @Test void selectedLiToDefaultBranchEmitsExactSelectorAndZeroClock()
            throws Exception {
        assertEquals("CE6EE68ADA8FD37E6C096B1A27E46CB6888F1179F85DEFB5007AC60287360292",
                sha256(SELECTED_CLIENT));
        String asm = read(DISASSEMBLY).replace("\r\n", "\n");
        String use = asm.substring(asm.indexOf("sub_4CB5F0 proc near"),
                asm.indexOf("sub_4CB5F0 endp") + 16);
        assertTrue(use.contains("cmp     esi, 0AFh"));
        assertTrue(use.contains("ja      def_4CB66E"));
        String generic = use.substring(use.indexOf("def_4CB66E:"));
        assertTrue(generic.contains("mov     edx, esi"));
        assertTrue(generic.contains("and     edx, 7"));
        assertTrue(generic.contains("sar     esi, 3"));
        assertTrue(generic.contains("push    edx\npush    esi\npush    14h\n"
                + "push    offset off_5028CC\ncall    sub_4C6AE0"));
        assertEquals(0, readPeWordAtVa(UNPACKED,
                0x635774L + (long) (BattleSpellCatalog.DRAGON_SKIN - 1) * 2L));
    }

    @Test void retainedEnginePinsFlatPhysicalOrderCounterBypassAndNoPersistence()
            throws Exception {
        String list = read(RETAINED.resolve(
                "src/com/lineage/server/model/L1AttackList.java"));
        assertTrue(list.contains("SKD3.put(DRAGON_SKIN, -10)"));
        for (String file : new String[] {"L1AttackPc.java", "L1AttackNpc.java"}) {
            String attack = read(RETAINED.resolve("src/com/lineage/server/model/" + file));
            int flat = attack.indexOf("getCombatDmgBonus()");
            int halve = attack.indexOf("isCombatDmgHalve()", flat);
            assertTrue(flat >= 0 && halve > flat, file);
        }
        String use = read(RETAINED.resolve(
                "src/com/lineage/server/model/skill/L1SkillUse.java"));
        assertTrue(use.contains("DRAGON_SKIN, AWAKEN_ANTHARAS"));
        String buffs = read(RETAINED.resolve(
                "src/com/lineage/server/datatables/user/CharBuffTable.java"));
        String buffArray = buffs.substring(buffs.indexOf("_buffSkill = {"),
                buffs.indexOf("};", buffs.indexOf("_buffSkill = {")));
        assertFalse(buffArray.contains("DRAGON_SKIN"));
    }

    @Test void implementationRoutesBothPhasesCancellationExpiryAndPhysicalOnlyOrder()
            throws Exception {
        String session = read(Path.of(
                "src/main/java/local/lito381/server/ClientSession.java"));
        assertEquals(2, occurrences(session,
                "BattleSpellCatalog.dragonSkinSpell(selector.skillId())"));
        assertTrue(session.contains("Protocol.selectorOnlySpellRequest(payload)"));
        assertTrue(session.contains("dragonSkinStatus.activate("));
        assertTrue(session.contains("dragonSkinStatus.cancel()"));
        assertTrue(session.contains("dragonSkinStatus.expire("));
        assertFalse(session.contains("saveTimedDragonSkinBuff"));
        int armorBreak = session.indexOf("BattleArmorBreak.damageAfterBreak(",
                session.indexOf("private int combatDamageAfterCooking("));
        int dragonSkin = session.indexOf("dragonSkinStatus.snapshot(now)", armorBreak);
        int immune = session.indexOf("abilityStatus.immuneToHarm(now)", dragonSkin);
        assertTrue(armorBreak >= 0 && dragonSkin > armorBreak && immune > dragonSkin);
        assertTrue(session.contains(
                "afterArmor, source, false, 0, true, true"));
    }

    private static String skillRow(String sql, int skillId) {
        String prefix = "INSERT INTO `skills` VALUES (" + skillId + ",";
        return sql.lines().filter(line -> line.startsWith(prefix))
                .findFirst().orElseThrow();
    }

    private static int occurrences(String text, String needle) {
        int count = 0;
        for (int at = 0; (at = text.indexOf(needle, at)) >= 0;
             at += needle.length()) count++;
        return count;
    }

    private static int readPeWordAtVa(Path path, long va) throws Exception {
        byte[] bytes = Files.readAllBytes(path);
        ByteBuffer image = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN);
        int pe = image.getInt(0x3C);
        int sections = Short.toUnsignedInt(image.getShort(pe + 6));
        int optionalSize = Short.toUnsignedInt(image.getShort(pe + 20));
        int optional = pe + 24;
        long imageBase = Integer.toUnsignedLong(image.getInt(optional + 28));
        long rva = va - imageBase;
        int table = optional + optionalSize;
        for (int i = 0; i < sections; i++) {
            int section = table + i * 40;
            long virtualSize = Integer.toUnsignedLong(image.getInt(section + 8));
            long sectionRva = Integer.toUnsignedLong(image.getInt(section + 12));
            long rawSize = Integer.toUnsignedLong(image.getInt(section + 16));
            long rawOffset = Integer.toUnsignedLong(image.getInt(section + 20));
            if (rva >= sectionRva
                    && rva + 2 <= sectionRva + Math.max(virtualSize, rawSize)) {
                return Short.toUnsignedInt(image.getShort(Math.toIntExact(
                        rawOffset + rva - sectionRva)));
            }
        }
        throw new IllegalArgumentException("unmapped PE VA");
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
