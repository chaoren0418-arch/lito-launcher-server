package local.lito381.server;

import static org.junit.jupiter.api.Assertions.assertEquals;
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

/** Cross-source proof for selected Illusionist skill213 Arm Breaker. */
final class NativeArmBreakerSourceAuditTest {
    @BeforeAll
    static void assumeLocalResourcesPresent() {
        LocalResources.assumeLineageSources(
                Path.of("../1-伺服器/db/380_db.sql"),
                Path.of("../1-伺服器/src/com/lineage/server/model/skill/L1SkillId.java"),
                Path.of("../1-伺服器/src/com/lineage/server/model/skill/L1SkillUse.java"),
                Path.of("../1-伺服器/src/com/lineage/server/model/skill/skillmode/ARM_BREAKER.java"),
                Path.of("../1-伺服器/src/com/lineage/server/model/skill/skillmode/CANCELLATION.java"),
                Path.of("../1-伺服器/其他src參考用/0507備份/yiwei380/db/380yiwei_db.sql"),
                Path.of("../1-伺服器/其他src參考用/rk760src/com/lineage/server/model/skill/skillmode/ARM_BREAKER.java"));
        LocalResources.assumeLitoBin(
                Path.of("D:/380/天堂(Lineage 3.81C) (1)/LiTo.bin"));
        LocalResources.assumeIdaClient(
                Path.of("D:/lineage開發/LiTo381-開發/ida-client-oep-v2/full-disassembly.asm"));
        LocalResources.assumeFiles(
                Path.of("D:/lineage開發/LiTo381-開發/runtime/LiTo.unpacked-oep-current.exe"));
    }

    private static final Path RETAINED = Path.of("..", "1-伺服器")
            .toAbsolutePath().normalize();
    private static final Path PRIMARY = RETAINED.resolve("db/380_db.sql");
    private static final Path SECONDARY = RETAINED.resolve(
            "其他src參考用/0507備份/yiwei380/db/380yiwei_db.sql");
    private static final Path MODE = RETAINED.resolve(
            "src/com/lineage/server/model/skill/skillmode/ARM_BREAKER.java");
    private static final Path ORIGINAL_MODE = RETAINED.resolve(
            "其他src參考用/rk760src/com/lineage/server/model/skill/skillmode/ARM_BREAKER.java");
    private static final Path USE = RETAINED.resolve(
            "src/com/lineage/server/model/skill/L1SkillUse.java");
    private static final Path CANCELLATION = RETAINED.resolve(
            "src/com/lineage/server/model/skill/skillmode/CANCELLATION.java");
    private static final Path SELECTED_CLIENT = Path.of(
            "D:/380/天堂(Lineage 3.81C) (1)/LiTo.bin");
    private static final Path UNPACKED = Path.of(
            "D:/lineage開發/LiTo381-開發/runtime/LiTo.unpacked-oep-current.exe");
    private static final Path DISASSEMBLY = Path.of(
            "D:/lineage開發/LiTo381-開發/ida-client-oep-v2/full-disassembly.asm");

    @Test void retainedInputsHaveLockedHashesAndExactRows() throws Exception {
        assertEquals("DA185BCA20CC254CB175EA2C367A2F5270514F4957B3AC99343C6101A2593766",
                sha256(PRIMARY));
        assertEquals("3BF9B04B6E1D788E361534A79F3AA49CEEEF84FAE5CE04471EDF1EC9764CA423",
                sha256(SECONDARY));
        assertEquals("1DCFF292D2E20F2C22ED5D04FDFE607D68A2F21E3C5F20D61054FDB3FAD6B636",
                sha256(MODE));
        assertEquals("3D9DC0F3821911BBDF5D6B48AE4A3A00F72C4DB4BB76F7A3A9B9AC8BE8401293",
                sha256(ORIGINAL_MODE));
        assertEquals("D2FD22F2DD76811579DE46F32B8482B3CBA23F45235B11DF50B5E3F6220D3B0A",
                sha256(USE));
        assertEquals("BF802004D6538C879EBD71FA192F93B6357CB3FA063CBD3CCBC7F79800359FB7",
                sha256(CANCELLATION));

        String primary = read(PRIMARY);
        String secondary = read(SECONDARY);
        assertEquals("INSERT INTO `skills` VALUES (213, 0, '武器破壞者', 27, 4,"
                + " 25, 0, 0, 0, 1200, 12, 'attack', 3, 15, 0, 0, 0, 0, 0,"
                + " 64, 0, 3, 0, 0, 16, '$5947', 18, 6551, 0, 0, 0, 0);",
                skillRow(primary));
        assertEquals("INSERT INTO `skills` VALUES (213, '武器破壞者', 27, 4, 25,"
                + " 0, 0, 0, 0, 12, 'attack', 3, 15, 0, 0, 0, 0, 0, 64, 0,"
                + " 3, 0, 0, 16, '$5947', 18, 6551, 0, 0, 0, 0);",
                skillRow(secondary));
        for (String sql : new String[] {primary, secondary}) {
            assertTrue(sql.contains("INSERT INTO `skills_buff_ability` VALUES"
                    + " (213, '武器破壞者', 'DMG', -2);"));
            assertTrue(sql.contains("INSERT INTO `skills_item` VALUES"
                    + " (213, '武器破壞者', NULL, NULL);"));
            assertTrue(sql.contains("49129, '記憶水晶(武器破壞者)',"
                    + " 'skill.Skill_Illusionist', '$5693'"));
        }
        assertTrue(primary.contains("INSERT INTO `skills_負面boss免疫`"
                + " VALUES (213, '武器破壞者', 0);"));
        assertTrue(secondary.contains("INSERT INTO `skills_負面boss免疫`"
                + " VALUES (213, '斷臂', 0);"));
    }

    @Test void selectedClientEmitsOnly141a04AndUses32msClock()
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
        assertEquals(32, readPeWordAtVa(UNPACKED, 0x63591CL));
        byte[] exact = new byte[] {0x14, 0x1A, 0x04};
        assertEquals("14 1A 04", Protocol.hex(exact));
        assertEquals(BattleSpellCatalog.ARM_BREAKER,
                Protocol.selectorOnlySpellRequest(exact).skillId());
    }

    @Test void retainedOrderAndOriginalModePinExactLifecycle()
            throws Exception {
        String migrated = read(MODE);
        String original = read(ORIGINAL_MODE);
        for (String fragment : new String[] {
                "magic.calcMagicDamage(L1SkillId.ARM_BREAKER)",
                "!cha.hasSkillEffect(L1SkillId.DMGUP2)",
                "cha.setSkillEffect(L1SkillId.DMGUP2, integer * 1000)",
                "final int dmg = 0;"}) {
            assertTrue(migrated.contains(fragment), fragment);
        }
        for (String fragment : new String[] {
                "magic.calcMagicDamage(213)",
                "!cha.hasSkillEffect(1026)",
                "cha.setSkillEffect(1026, integer * 1000)",
                "cha.addDmgup(-2)", "cha.addDmgup(2)"}) {
            assertTrue(original.contains(fragment), fragment);
        }
        String use = read(USE);
        int attack = use.indexOf("case L1Skills.TYPE_ATTACK://");
        int counter = use.indexOf("if (isUseCounterMagic(cha))", attack);
        int genericDamage = use.indexOf("_dmg = magic.calcMagicDamage(_skillId)",
                counter);
        int modeStart = use.indexOf("this._dmg = mode.start(", genericDamage);
        int probabilityBranch = use.indexOf("case L1Skills.TYPE_CURSE:", attack);
        assertTrue(attack < counter && counter < genericDamage
                && genericDamage < modeStart && genericDamage < probabilityBranch,
                "TYPE_ATTACK counters, damages and runs the mode without probability");
        String cancellation = read(CANCELLATION);
        assertTrue(cancellation.contains(
                "for (int skillNum = STATUS_BEGIN; skillNum <= STATUS_END; skillNum++)"));
        assertTrue(read(RETAINED.resolve(
                "src/com/lineage/server/model/skill/L1SkillId.java"))
                .contains("public static final int STATUS_END = 1026;"));
    }

    private static String skillRow(String sql) {
        return sql.lines().filter(line -> line.startsWith(
                "INSERT INTO `skills` VALUES (213,"))
                .findFirst().orElseThrow();
    }

    private static int readPeWordAtVa(Path path, long va) throws Exception {
        byte[] bytes = LocalResources.readAllBytes(path);
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
        assertTrue(Files.isRegularFile(LocalResources.assumeExternalFile(path)), "missing source evidence: " + path);
        return LocalResources.readString(path, StandardCharsets.UTF_8);
    }

    private static String sha256(Path path) throws Exception {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        try (var stream = LocalResources.newInputStream(path)) {
            byte[] buffer = new byte[64 * 1024];
            for (int count; (count = stream.read(buffer)) >= 0;) {
                if (count > 0) digest.update(buffer, 0, count);
            }
        }
        return HexFormat.of().withUpperCase().formatHex(digest.digest());
    }
}
