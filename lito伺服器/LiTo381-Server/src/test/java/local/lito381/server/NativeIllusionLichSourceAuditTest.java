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

/** Cross-source proof for selected Illusionist skill209 Illusion: Lich. */
final class NativeIllusionLichSourceAuditTest {
    private static final Path RETAINED = Path.of("..", "1-伺服器")
            .toAbsolutePath().normalize();
    private static final Path PRIMARY = RETAINED.resolve("db/380_db.sql");
    private static final Path SECONDARY = RETAINED.resolve(
            "其他src參考用/0507備份/yiwei380/db/380yiwei_db.sql");
    private static final Path MODE = RETAINED.resolve(
            "src/com/lineage/server/model/skill/skillmode/ILLUSION_LICH.java");
    private static final Path SELECTED_CLIENT = Path.of(
            "D:/380/天堂(Lineage 3.81C) (1)/LiTo.bin");
    private static final Path UNPACKED = Path.of(
            "D:/lineage開發/LiTo381-開發/runtime/LiTo.unpacked-oep-current.exe");
    private static final Path DISASSEMBLY = Path.of(
            "D:/lineage開發/LiTo381-開發/ida-client-oep-v2/full-disassembly.asm");

    @Test void selectedRowsPinPcBuffSpAndNonPersistence() throws Exception {
        String primary = read(PRIMARY);
        String secondary = read(SECONDARY);
        assertEquals("INSERT INTO `skills` VALUES (209, 0, '幻覺：巫妖', 27, 0,"
                + " 15, 0, 0, 0, 2000, 64, 'buff', 1, 0, 0, 0, 0, 0, 0,"
                + " 2, 0, 3, 0, 0, 1, '$5943', 19, 6577, 0, 0, 0, 280);",
                skillRow(primary, 209));
        assertEquals("INSERT INTO `skills` VALUES (209, '幻覺：巫妖', 27, 0, 15,"
                + " 0, 0, 0, 0, 64, 'buff', 1, 0, 0, 0, 0, 0, 0, 2,"
                + " 0, 3, 0, 0, 1, '$5943', 19, 6577, 0, 0, 0, 280);",
                skillRow(secondary, 209));
        for (String sql : new String[] {primary, secondary}) {
            assertTrue(sql.contains("INSERT INTO `skills_item` VALUES"
                    + " (209, '幻覺：巫妖', NULL, NULL);"));
            assertTrue(sql.contains("49125, '記憶水晶(幻覺：巫妖)',"
                    + " 'skill.Skill_Illusionist', '$5689'"));
            assertFalse(sql.contains("INSERT INTO `skills_buff_ability` VALUES"
                    + " (209,"), "SP+2 is hardcoded in the retained skill mode");
            assertTrue(sql.contains("INSERT INTO `skill_狀態天賦設置` VALUES"
                    + " (2704, 209, '幻覺：巫妖'"),
                    "custom talent rows remain evidence, not native base effect");
        }

        assertEquals("5D49D9C2E2F87CD73080FB0C04383B970DBE1B8FB93CFF09E9C67EA124ED7359",
                sha256(MODE));
        String mode = read(MODE);
        for (String fragment : new String[] {
                "!cha.hasSkillEffect(L1SkillId.ILLUSION_LICH)",
                "pc.addSp(2)",
                "pc.sendPackets(new S_SPMR(pc))",
                "pc.setSkillEffect(L1SkillId.ILLUSION_LICH, integer * 1000)",
                "tgnpc.addSp(2)",
                "pc.addSp(-2)",
                "tgnpc.addSp(-2)"}) {
            assertTrue(mode.contains(fragment),
                    "missing retained Illusion: Lich mode evidence: " + fragment);
        }
        String npcOverload = mode.substring(mode.indexOf(
                "public int start(final L1NpcInstance npc"), mode.indexOf(
                "public void start(final L1PcInstance srcpc"));
        assertTrue(npcOverload.contains("final int dmg = 0"));
        assertTrue(npcOverload.contains("return dmg"));

        String charBuff = read(RETAINED.resolve(
                "src/com/lineage/server/datatables/user/CharBuffTable.java"));
        String persisted = charBuff.substring(charBuff.indexOf("_buffSkill ="),
                charBuff.indexOf("private static CharBuffTable"));
        assertFalse(persisted.contains("ILLUSION_LICH"),
                "skill209 is not restored by retained login persistence");
    }

    @Test void selectedLiToEmitsOnly141a00AndPins64MsClientClock()
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
        assertEquals(64, readPeWordAtVa(UNPACKED, 0x635914L));
        assertEquals("14 1A 00", Protocol.hex(
                new byte[] {0x14, 0x1A, 0x00}));
    }

    @Test void retainedRuntimePinsPcTargetCounterBypassRefreshAndSpLifecycle()
            throws Exception {
        String use = read(RETAINED.resolve(
                "src/com/lineage/server/model/skill/L1SkillUse.java"));
        String exceptCounter = use.substring(use.indexOf("EXCEPT_COUNTER_MAGIC"),
                use.indexOf("};", use.indexOf("EXCEPT_COUNTER_MAGIC")));
        assertTrue(exceptCounter.contains("ILLUSION_LICH"));
        String repeated = use.substring(use.indexOf("ALWAYS_REPEATED_SKILLS"),
                use.indexOf("private void deleteRepeatedSkills"));
        assertFalse(repeated.contains("ILLUSION_LICH"),
                "skill209 refreshes itself but replaces no other illusion");
        for (String fragment : new String[] {
                "(_skill.getTargetTo() & L1Skills.TARGET_TO_PC)",
                "(cha.getId() == _user.getId())",
                "this._user.getLocation().getTileLineDistance",
                "this._user.glanceCheck(this._target.getX(), this._target.getY())"}) {
            assertTrue(use.contains(fragment),
                    "missing retained target evidence: " + fragment);
        }
        String skillMode = read(RETAINED.resolve(
                "src/com/lineage/server/model/skill/L1SkillMode.java"));
        assertTrue(skillMode.contains("_skillMode.put(ILLUSION_LICH,"
                + " new ILLUSION_LICH())"));
    }

    @Test void implementationPinsBothPhasesSelectorRestorationAndSpStatus()
            throws Exception {
        String catalog = read(Path.of(
                "src/main/java/local/lito381/server/BattleSpellCatalog.java"));
        String status = read(Path.of(
                "src/main/java/local/lito381/server/BattleAbilityStatus.java"));
        String session = read(Path.of(
                "src/main/java/local/lito381/server/ClientSession.java"));
        for (String fragment : new String[] {
                "ILLUSION_LICH, new IllusionLichSpell(",
                "ILLUSION_LICH, 26, 0, 19, 6_577",
                "15, 2_000, 64, 64, 3",
                "1, 2, 1, 2",
                "49_125L, \"$5689\", \"$5943\", true",
                "RequestShape.SELECTOR_ONLY"}) {
            assertTrue(catalog.contains(fragment),
                    "missing Illusion: Lich catalog implementation: " + fragment);
        }
        for (String fragment : new String[] {
                "activateIllusionLich(",
                "illusionLich(long nowNanos)",
                "expireIllusionLich(",
                "clearIllusionLich();",
                "boolean illusionLich,"}) {
            assertTrue(status.contains(fragment),
                    "missing Illusion: Lich lifecycle implementation: " + fragment);
        }
        for (String fragment : new String[] {
                "BattleSpellCatalog.illusionLichSpell(",
                "useIllusionLichSpell(payload,",
                "resolveSelectorOnlyIllusionLichTarget(",
                "Protocol.selectorOnlySpellRequest(payload)",
                "target.abilityStatus.activateIllusionLich(",
                "target.sendMagicStatus(castNow, \"illusion-lich-start\")",
                "abilityStatus.illusionLich(nowNanos).spellPower()",
                "abilityStatus.expireIllusionLich(nowNanos)"}) {
            assertTrue(session.contains(fragment),
                    "missing Illusion: Lich runtime implementation: " + fragment);
        }
    }

    private static String skillRow(String sql, int skillId) {
        return sql.lines().filter(line -> line.startsWith(
                "INSERT INTO `skills` VALUES (" + skillId + ","))
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
