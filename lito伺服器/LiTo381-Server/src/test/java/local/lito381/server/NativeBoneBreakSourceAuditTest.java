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
import org.junit.jupiter.api.Test;

/** Cross-source proof for selected Illusionist skill208 Bone Break. */
final class NativeBoneBreakSourceAuditTest {
    private static final Path RETAINED = Path.of("..", "1-伺服器")
            .toAbsolutePath().normalize();
    private static final Path PRIMARY = RETAINED.resolve("db/380_db.sql");
    private static final Path SECONDARY = RETAINED.resolve(
            "其他src參考用/0507備份/yiwei380/db/380yiwei_db.sql");
    private static final Path MODE = RETAINED.resolve(
            "src/com/lineage/server/model/skill/skillmode/BONE_BREAK.java");
    private static final Path SELECTED_CLIENT = Path.of(
            "D:/380/天堂(Lineage 3.81C) (1)/LiTo.bin");
    private static final Path UNPACKED = Path.of(
            "D:/lineage開發/LiTo381-開發/runtime/LiTo.unpacked-oep-current.exe");
    private static final Path DISASSEMBLY = Path.of(
            "D:/lineage開發/LiTo381-開發/ida-client-oep-v2/full-disassembly.asm");

    @Test void retainedInputsHaveLockedHashesAndExactSelectedRows()
            throws Exception {
        assertEquals("DA185BCA20CC254CB175EA2C367A2F5270514F4957B3AC99343C6101A2593766",
                sha256(PRIMARY));
        assertEquals("3BF9B04B6E1D788E361534A79F3AA49CEEEF84FAE5CE04471EDF1EC9764CA423",
                sha256(SECONDARY));
        assertEquals("E1EDB07B4B6A1CDAB4760531FDCD1A11A44C07821B5D49FCF16219A6E8CA304E",
                sha256(MODE));
        String primary = read(PRIMARY);
        String secondary = read(SECONDARY);
        assertEquals("INSERT INTO `skills` VALUES (208, 0, '骷髏毀壞', 26, 7,"
                + " 20, 0, 0, 0, 2000, 3, 'attack', 3, 10, 0, 0, 45, 0, 0,"
                + " 64, 0, 3, 0, 0, 128, '$5942', 18, 7020, 0, 0, 0, 0);",
                skillRow(primary));
        assertEquals("INSERT INTO `skills` VALUES (208, '骷髏毀壞', 26, 7, 20,"
                + " 0, 0, 0, 0, 3, 'attack', 3, 10, 0, 0, 45, 0, 0, 64, 0,"
                + " 3, 0, 0, 128, '$5942', 18, 7020, 0, 0, 0, 0);",
                skillRow(secondary));
        String negative = "INSERT INTO `skills_負面技能控制` VALUES (208,"
                + " '骷髏毀壞-昏迷耐性', 55, 45, 35, 0, 0, 0.35, 5, 0, 1,"
                + " 0.05, 0, 0);";
        for (String sql : new String[] {primary, secondary}) {
            assertTrue(sql.contains(negative));
            assertTrue(sql.contains("INSERT INTO `skills_item` VALUES"
                    + " (208, '骷髏毀壞', NULL, NULL);"));
            assertTrue(sql.contains("49124, '記憶水晶(骷髏毀壞)',"
                    + " 'skill.Skill_Illusionist', '$5688'"));
            assertTrue(sql.contains("INSERT INTO `npc` VALUES (86123,"
                    + " '幻術師技能(骷髏毀壞)', '', '0', '', 'L1Effect', 6552,"));
        }
        assertTrue(primary.contains("INSERT INTO `skills_負面boss免疫`"
                + " VALUES (208, '骷髏毀壞', 10);"));
        assertTrue(secondary.contains("INSERT INTO `skills_負面boss免疫`"
                + " VALUES (208, '骷髏毀壞', 0);"));
    }

    @Test void selectedLiToEmitsOnly141907AndHasZeroCastClock()
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
        assertEquals(0, readPeWordAtVa(UNPACKED, 0x635912L));
        byte[] exact = new byte[] {0x14, 0x19, 0x07};
        assertEquals("14 19 07", Protocol.hex(exact));
        assertEquals(BattleSpellCatalog.BONE_BREAK,
                Protocol.selectorOnlySpellRequest(exact).skillId());
    }

    @Test void retainedModePinsDoubleDamagePrivateTwentyPercentAndFixedActorLock()
            throws Exception {
        String mode = read(MODE);
        for (String fragment : new String[] {
                "magic.calcMagicDamage(L1SkillId.BONE_BREAK)",
                "!cha.hasSkillEffect(L1SkillId.BONE_BREAK)",
                "random.nextInt(1000) + 1",
                "if (rad > 800)",
                "final int time = 2;",
                "cha.setSkillEffect(L1SkillId.BONE_BREAK, time * 1000)",
                "L1SpawnUtil.spawnEffect(86123, time",
                "new S_Paralysis(S_Paralysis.TYPE_STUN, true)",
                "npc.setParalyzed(true)",
                "final int dmg = 0;",
                "new S_Paralysis(S_Paralysis.TYPE_STUN, false)",
                "npc.setParalyzed(false)"}) {
            assertTrue(mode.contains(fragment), fragment);
        }
        String use = read(RETAINED.resolve(
                "src/com/lineage/server/model/skill/L1SkillUse.java"));
        int attack = use.indexOf("case L1Skills.TYPE_ATTACK://");
        int counter = use.indexOf("if (isUseCounterMagic(cha))", attack);
        int genericDamage = use.indexOf("_dmg = magic.calcMagicDamage(_skillId)",
                counter);
        int modeStart = use.indexOf("this._dmg = mode.start(", genericDamage);
        int probabilityBranch = use.indexOf("case L1Skills.TYPE_CURSE:", attack);
        int probability = use.indexOf("calcProbabilityMagic(this._skillId)",
                probabilityBranch);
        assertTrue(attack < counter && counter < genericDamage
                && genericDamage < modeStart);
        assertTrue(genericDamage < probabilityBranch && probabilityBranch < probability,
                "row208 TYPE_ATTACK never reaches the DB probability branch");

        String catalog = read(Path.of(
                "src/main/java/local/lito381/server/BattleSpellCatalog.java"));
        String session = read(Path.of(
                "src/main/java/local/lito381/server/ClientSession.java"));
        for (String fragment : new String[] {
                "BONE_BREAK, new BoneBreakSpell(",
                "new AttackSpell(BONE_BREAK, 25, 7, 18, 7_020",
                "20, 2_000, 3, 10, 0, 0",
                "2, 1, 1_000, 800",
                "86_123, 6_552, 49_124L"}) {
            assertTrue(catalog.contains(fragment), fragment);
        }
        for (String fragment : new String[] {
                "BattleSpellCatalog.boneBreakSpell(",
                "applySpellLawfulDelta(",
                "bone-break-first-player=",
                "bone-break-mode-player=",
                "applyBattleBoneBreak(",
                "applyClassicWorldBoneBreak(",
                "Protocol.boneBreakEffect(",
                "Protocol.paralysis(Protocol.ParalysisMode.STUN_APPLY)",
                "Protocol.paralysis(Protocol.ParalysisMode.STUN_RELEASE)"}) {
            assertTrue(session.contains(fragment), fragment);
        }
        assertTrue(read(Path.of("tools/protocol_coverage.py")).contains(
                "skill208 Bone Break is selected-client selector-only"));
    }

    private static String skillRow(String sql) {
        return sql.lines().filter(line -> line.startsWith(
                "INSERT INTO `skills` VALUES (208,"))
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
