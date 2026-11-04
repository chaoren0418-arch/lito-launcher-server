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
import java.util.Set;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/** Cross-source proof for selected Illusionist skill211 Patience. */
final class NativePatienceSourceAuditTest {
    @BeforeAll
    static void assumeLocalResourcesPresent() {
        LocalResources.assumeLineageSources();
        LocalResources.assumeLitoBin();
        LocalResources.assumeIdaClient(LocalResources.IDA_DISASSEMBLY_ABSOLUTE);
        LocalResources.assumeExisting("LiTo.unpacked", LocalResources.LITO_UNPACKED);
    }

    private static final Set<String> RETAINED_ATTACK_PC_SNAPSHOTS = Set.of(
            "1FFC0BFEDDE7C47F654FB821386FA8C85B570A9C1A286627BE0697D71936FD30",
            "3FDC94CEDC6C3EC803E731BE238093BD7FE55C432CF84BE6822A079A1627DDFC");
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

    @Test void selectedRowsPinOrdinaryBuffWithoutReagent() throws Exception {
        String primary = read(PRIMARY);
        String secondary = read(SECONDARY);
        assertEquals("INSERT INTO `skills` VALUES (211, 0, '耐力', 27, 2, 25,"
                + " 0, 0, 0, 1000, 600, 'buff', 3, 0, 0, 0, 0, 0, 0, 2,"
                + " 0, 3, 0, 0, 4, '$5945', 19, 6507, 0, 0, 0, 0);",
                skillRow(primary));
        assertEquals("INSERT INTO `skills` VALUES (211, '耐力', 27, 2, 25, 0,"
                + " 0, 0, 0, 600, 'buff', 3, 0, 0, 0, 0, 0, 0, 2, 0, 3,"
                + " 0, 0, 4, '$5945', 19, 6507, 0, 0, 0, 0);",
                skillRow(secondary));
        for (String sql : new String[] {primary, secondary}) {
            assertTrue(sql.contains("INSERT INTO `skills_item` VALUES"
                    + " (211, '耐力', NULL, NULL);"));
            assertTrue(sql.contains("49127, '記憶水晶(耐力)',"
                    + " 'skill.Skill_Illusionist', '$5691'"));
        }
    }

    @Test void selectedLiToEmitsOnly141a02AndHasSixteenMsClock()
            throws Exception {
        assertEquals("CE6EE68ADA8FD37E6C096B1A27E46CB6888F1179F85DEFB5007AC60287360292",
                sha256(SELECTED_CLIENT));
        String asm = read(DISASSEMBLY).replace("\r\n", "\n");
        String use = asm.substring(asm.indexOf("sub_4CB5F0 proc near"),
                asm.indexOf("sub_4CB5F0 endp") + 16);
        String generic = use.substring(use.indexOf("def_4CB66E:"));
        assertTrue(use.contains("cmp     esi, 0AFh"));
        assertTrue(generic.contains("and     edx, 7"));
        assertTrue(generic.contains("sar     esi, 3"));
        assertTrue(generic.contains("push    edx\npush    esi\npush    14h\n"
                + "push    offset off_5028CC\ncall    sub_4C6AE0"));
        assertEquals(16, readPeWordAtVa(UNPACKED, 0x635918L));
        assertEquals("14 1A 02", Protocol.hex(
                new byte[] {0x14, 0x1A, 0x02}));
    }

    @Test void retainedAttackSourcePinsOriginalMinusTwoAndPcOnlyConsumers()
            throws Exception {
        Path attack = RETAINED.resolve(
                "src/com/lineage/server/model/L1AttackList.java");
        Path pc = RETAINED.resolve(
                "src/com/lineage/server/model/L1AttackPc.java");
        Path npc = RETAINED.resolve(
                "src/com/lineage/server/model/L1AttackNpc.java");
        assertEquals("E78EBC48D4CF566B5382EBBE3F123F0D8DFD6621D72D110140705A288DFA087D",
                sha256(attack));
        assertTrue(RETAINED_ATTACK_PC_SNAPSHOTS.contains(sha256(pc)),
                "unrecognized retained L1AttackPc snapshot");
        assertEquals("7BA897448013D57FD77BA05C4213E1A09E9C709CD9199271A2C14F5D2F65C3E8",
                sha256(npc));
        String attackText = read(attack);
        assertTrue(attackText.contains(
                "SKD3.put(PATIENCE, -15);// 原始 -2"));
        assertTrue(attackText.contains(
                "int skd3FlatModifier, boolean skd3HalveDamage"));
        assertTrue(attackText.contains("flat += d3;"));
        assertTrue(read(pc).contains("dmg += _targetPc.getCombatDmgBonus();"));
        assertTrue(read(npc).contains("dmg += _targetPc.getCombatDmgBonus();"));
    }

    @Test void implementationKeepsNpcLeaseButQueriesReductionForPlayersOnly()
            throws Exception {
        String status = read(Path.of(
                "src/main/java/local/lito381/server/BattlePatienceStatus.java"));
        String hub = read(Path.of(
                "src/main/java/local/lito381/server/GameHub.java"));
        String session = read(Path.of(
                "src/main/java/local/lito381/server/ClientSession.java"));
        assertTrue(status.contains("PHYSICAL_REDUCTION = 2"));
        assertTrue(status.contains("DURATION_SECONDS = 600"));
        assertTrue(hub.contains("activateClassicWorldPatience("));
        assertTrue(hub.contains("activatePatience("));
        assertTrue(session.contains("usePatienceSpell(payload,"));
        assertTrue(session.contains("BattleTrueTarget.Kind.WORLD_MONSTER"));
        assertTrue(session.contains("effectivePatiencePhysicalReduction(now)"));
        assertTrue(session.contains("icon=omitted-no-selected-LiTo-packetbox-receiver"));
        assertFalse(session.contains("Protocol.iconsPatience"));
    }

    private static String skillRow(String sql) {
        return sql.lines().filter(line -> line.startsWith(
                "INSERT INTO `skills` VALUES (211,"))
                .findFirst().orElseThrow();
    }

    private static int readPeWordAtVa(Path path, long va) throws Exception {
        byte[] bytes = Files.readAllBytes(path);
        ByteBuffer image = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN);
        int pe = image.getInt(0x3C);
        int sections = Short.toUnsignedInt(image.getShort(pe + 6));
        int optional = pe + 24;
        int table = optional + Short.toUnsignedInt(image.getShort(pe + 20));
        long rva = va - Integer.toUnsignedLong(image.getInt(optional + 28));
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
