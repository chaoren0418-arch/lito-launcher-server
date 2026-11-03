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
import org.junit.jupiter.api.Test;

/** Cross-source proof for selected Illusionist skill207 Mind Break. */
final class NativeMindBreakSourceAuditTest {
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

    @Test void selectedRowsPinPcNpcCostBaseValueRangeAndBook()
            throws Exception {
        String primary = read(PRIMARY);
        String secondary = read(SECONDARY);
        assertEquals("INSERT INTO `skills` VALUES (207, 0, '心靈破壞', 26, 6,"
                + " 20, 15, 0, 0, 3000, 0, 'attack', 3, 5, 0, 0, 0, 0, 0,"
                + " 64, 0, 3, 0, 0, 64, '$5941', 18, 6553, 0, 0, 0, 0);",
                skillRow(primary));
        assertEquals("INSERT INTO `skills` VALUES (207, '心靈破壞', 26, 6, 20,"
                + " 15, 0, 0, 0, 0, 'attack', 3, 5, 0, 0, 0, 0, 0, 64, 0,"
                + " 3, 0, 0, 64, '$5941', 18, 6553, 0, 0, 0, 0);",
                skillRow(secondary));
        for (String sql : new String[] {primary, secondary}) {
            assertTrue(sql.contains("INSERT INTO `skills_item` VALUES"
                    + " (207, '心靈破壞', NULL, NULL);"));
            assertTrue(sql.contains("49123, '記憶水晶(心靈破壞)',"
                    + " 'skill.Skill_Illusionist', '$5687'"));
            assertFalse(sql.contains("INSERT INTO `skills_buff_ability` VALUES"
                    + " (207,"));
        }
    }

    @Test void selectedLiToEmitsOnly141906AndHasZeroCastClock()
            throws Exception {
        assertEquals("CE6EE68ADA8FD37E6C096B1A27E46CB6888F1179F85DEFB5007AC60287360292",
                sha256(SELECTED_CLIENT));
        String asm = read(DISASSEMBLY).replace("\r\n", "\n");
        String use = asm.substring(asm.indexOf("sub_4CB5F0 proc near"),
                asm.indexOf("sub_4CB5F0 endp") + 16);
        String generic = use.substring(use.indexOf("def_4CB66E:"));
        assertTrue(use.contains("cmp     esi, 0AFh"));
        assertTrue(use.contains("ja      def_4CB66E"));
        assertTrue(generic.contains("and     edx, 7"));
        assertTrue(generic.contains("sar     esi, 3"));
        assertTrue(generic.contains("push    edx\npush    esi\npush    14h\n"
                + "push    offset off_5028CC\ncall    sub_4C6AE0"));
        assertEquals(0, readPeWordAtVa(UNPACKED, 0x635910L));
        byte[] exact = new byte[] {0x14, 0x19, 0x06};
        assertEquals("14 19 06", Protocol.hex(exact));
        assertEquals(BattleSpellCatalog.MIND_BREAK,
                Protocol.selectorOnlySpellRequest(exact).skillId());
    }

    @Test void retainedModePinsCounterFirstStrictDebitAndPostMagicX4()
            throws Exception {
        String mode = read(RETAINED.resolve(
                "src/com/lineage/server/model/skill/skillmode/MIND_BREAK.java"));
        for (String fragment : new String[] {
                "final int reMp = random.nextInt(46) + 5;",
                "if (cha.getCurrentMp() > reMp",
                "cha.debitCurrentMpUpToIfAlive(reMp) == reMp",
                "magic.calcMagicDamage(L1SkillId.MIND_BREAK) << 2"}) {
            assertTrue(mode.contains(fragment), fragment);
        }
        String use = read(RETAINED.resolve(
                "src/com/lineage/server/model/skill/L1SkillUse.java"));
        int attack = use.indexOf("case L1Skills.TYPE_ATTACK://");
        int counter = use.indexOf("if (isUseCounterMagic(cha))", attack);
        int genericDamage = use.indexOf("_dmg = magic.calcMagicDamage(_skillId)",
                counter);
        int modeStart = use.indexOf("this._dmg = mode.start(", genericDamage);
        assertTrue(attack < counter && counter < genericDamage
                && genericDamage < modeStart);

        String catalog = read(Path.of(
                "src/main/java/local/lito381/server/BattleSpellCatalog.java"));
        String session = read(Path.of(
                "src/main/java/local/lito381/server/ClientSession.java"));
        for (String fragment : new String[] {
                "MIND_BREAK, new MindBreakSpell(",
                "new AttackSpell(MIND_BREAK, 25, 6, 18, 6_553",
                "20, 3_000, 3, 5, 0, 0",
                "15, 0, 3, 64, 64",
                "5, 50, 4, 49_123L"}) {
            assertTrue(catalog.contains(fragment), fragment);
        }
        for (String fragment : new String[] {
                "BattleSpellCatalog.mindBreakSpell(",
                "debitBattleMindBreakTargetMp(",
                "debitClassicWorldMindBreakTargetMp(",
                "drainQuestMonsterManaIfStrictlyAbove(",
                "drainTimeAttackActorManaIfStrictlyAbove(",
                "drainClassicWorldMonsterManaIfStrictlyAbove(",
                "mindBreak.damageAfterMultiplier("}) {
            assertTrue(session.contains(fragment), fragment);
        }
    }

    private static String skillRow(String sql) {
        return sql.lines().filter(line -> line.startsWith(
                "INSERT INTO `skills` VALUES (207,"))
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
        for (int index = 0; index < sections; index++) {
            int section = table + index * 40;
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
