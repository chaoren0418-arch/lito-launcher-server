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

/** Cross-source proof for selected Illusionist skill219 Illusion: Avatar. */
final class NativeIllusionAvatarSourceAuditTest {
    @BeforeAll
    static void assumeLocalResourcesPresent() {
        LocalResources.assumeLineageSources(
                Path.of("../1-伺服器/db/380_db.sql"),
                Path.of("../1-伺服器/src/com/lineage/config/ConfigSkill.java"),
                Path.of("../1-伺服器/src/com/lineage/server/datatables/user/CharBuffTable.java"),
                Path.of("../1-伺服器/src/com/lineage/server/model/Instance/L1PcInstance.java"),
                Path.of("../1-伺服器/src/com/lineage/server/model/skill/L1SkillUse.java"),
                Path.of("../1-伺服器/src/com/lineage/server/model/skill/skillmode/ILLUSION_AVATAR.java"),
                Path.of("../1-伺服器/其他src參考用/0507備份/yiwei380/db/380yiwei_db.sql"));
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
            "src/com/lineage/server/model/skill/skillmode/ILLUSION_AVATAR.java");
    private static final Path PC = RETAINED.resolve(
            "src/com/lineage/server/model/Instance/L1PcInstance.java");
    private static final Path USE = RETAINED.resolve(
            "src/com/lineage/server/model/skill/L1SkillUse.java");
    private static final Path CONFIG = RETAINED.resolve(
            "src/com/lineage/config/ConfigSkill.java");
    private static final Path SELECTED_CLIENT = Path.of(
            "D:/380/天堂(Lineage 3.81C) (1)/LiTo.bin");
    private static final Path UNPACKED = Path.of(
            "D:/lineage開發/LiTo381-開發/runtime/LiTo.unpacked-oep-current.exe");
    private static final Path DISASSEMBLY = Path.of(
            "D:/lineage開發/LiTo381-開發/ida-client-oep-v2/full-disassembly.asm");

    @Test void hashesRowsAbilitiesBookAndNoMaterialOrProbabilityAreLocked()
            throws Exception {
        assertEquals("DA185BCA20CC254CB175EA2C367A2F5270514F4957B3AC99343C6101A2593766",
                sha256(PRIMARY));
        assertEquals("3BF9B04B6E1D788E361534A79F3AA49CEEEF84FAE5CE04471EDF1EC9764CA423",
                sha256(SECONDARY));
        assertEquals("7CD9E27677ABD5385EBD69D3E0F51C7F4C2A51260928C46A9F025D1FD931C13D",
                sha256(MODE));
        assertEquals("D652D175C31ED3A7D1F6682792BD447BC36AB8C0211A2C7A56B070C0D630C19F",
                sha256(PC));
        assertEquals("D2FD22F2DD76811579DE46F32B8482B3CBA23F45235B11DF50B5E3F6220D3B0A",
                sha256(USE));
        assertEquals("FFC81F59CDD57D18371538B92AB0D6B591009FBB37355BE86737ADBC414E7D7E",
                sha256(CONFIG));

        String primary = read(PRIMARY);
        String secondary = read(SECONDARY);
        assertEquals("INSERT INTO `skills` VALUES (219, 0, '幻覺：化身', 28, 2,"
                        + " 35, 0, 0, 0, 5000, 64, 'buff', 1, 0, 0, 0, 0, 0, 0,"
                        + " 2, 0, 3, 0, 0, 4, '$5953', 19, 6575, 0, 0, 0, 0);",
                skillRow(primary, 219));
        assertEquals("INSERT INTO `skills` VALUES (219, '幻覺：化身', 28, 2, 35,"
                        + " 0, 0, 0, 0, 64, 'buff', 1, 0, 0, 0, 0, 0, 0, 2, 0,"
                        + " 3, 0, 0, 4, '$5953', 19, 6575, 0, 0, 0, 0);",
                skillRow(secondary, 219));
        for (String sql : new String[] {primary, secondary}) {
            assertTrue(sql.contains("INSERT INTO `skills_buff_ability` VALUES"
                    + " (219, '幻覺：化身', 'BOW_DMG', 10);"));
            assertTrue(sql.contains("INSERT INTO `skills_buff_ability` VALUES"
                    + " (219, '幻覺：化身', 'DMG', 10);"));
            assertTrue(sql.contains("INSERT INTO `skills_item` VALUES"
                    + " (219, '幻覺：化身', NULL, NULL);"));
            assertTrue(sql.contains("49135, '記憶水晶(幻覺：化身)',"
                    + " 'skill.Skill_Illusionist', '$5699'"));
            assertFalse(sql.contains("`skills_負面技能控制` VALUES"
                    + " (219, '幻覺：化身'"));
            assertFalse(sql.contains("`skills_負面boss免疫` VALUES"
                    + " (219, '幻覺：化身'"));
        }
    }

    @Test void selectedClientEmits141b02WithZeroClock() throws Exception {
        assertEquals("CE6EE68ADA8FD37E6C096B1A27E46CB6888F1179F85DEFB5007AC60287360292",
                sha256(SELECTED_CLIENT));
        assertEquals(0, readPeWordAtVa(UNPACKED, 0x635928L));
        assertEquals(219, Protocol.selectorOnlySpellRequest(
                new byte[] {0x14, 0x1B, 0x02}).skillId());
        String asm = read(DISASSEMBLY).replace("\r\n", "\n");
        String use = asm.substring(asm.indexOf("sub_4CB5F0 proc near"),
                asm.indexOf("sub_4CB5F0 endp") + 16);
        String generic = use.substring(use.indexOf("def_4CB66E:"));
        assertTrue(use.contains("cmp     esi, 0AFh"));
        assertTrue(generic.contains("and     edx, 7"));
        assertTrue(generic.contains("sar     esi, 3"));
        assertTrue(generic.contains("push    edx\npush    esi\npush    14h\n"
                + "push    offset off_5028CC\ncall    sub_4C6AE0"));
    }

    @Test void retainedPcModeRefreshesWithoutStackAndMultipliesBeforeIntCast()
            throws Exception {
        String mode = read(MODE);
        for (String fragment : new String[] {
                "if (cha instanceof L1PcInstance)",
                "!cha.hasSkillEffect(L1SkillId.ILLUSION_AVATAR)",
                "pc.addSp(8)",
                "pc.sendPackets(new S_SPMR(pc))",
                "pc.updateBuffStatus()",
                "cha.setSkillEffect(L1SkillId.ILLUSION_AVATAR, integer * 1000)",
                "pc.addSp(-8)"}) {
            assertTrue(mode.contains(fragment), fragment);
        }
        String pc = read(PC);
        assertTrue(pc.contains("if (this.hasSkillEffect(ILLUSION_AVATAR))"));
        assertTrue(pc.contains("damage *= ConfigSkill.ILLUSION_AVATAR_DAMAGE_RATE"));
        assertTrue(pc.contains("new DamageEvent(attacker, this, (int) damage"));
        String config = read(CONFIG);
        assertTrue(config.contains("ILLUSION_AVATAR_DAMAGE_RATE = _getDouble(p,"
                + " \"ILLUSION_AVATAR_DAMAGE_RATE\", 1.5)"));

        String use = read(USE);
        String exceptCounter = use.substring(use.indexOf("EXCEPT_COUNTER_MAGIC"),
                use.indexOf("};", use.indexOf("EXCEPT_COUNTER_MAGIC")));
        assertTrue(exceptCounter.contains("ILLUSION_AVATAR"));
        String repeated = use.substring(use.indexOf("ALWAYS_REPEATED_SKILLS"),
                use.indexOf("private void deleteRepeatedSkills"));
        assertFalse(repeated.contains("ILLUSION_AVATAR"));
        assertTrue(use.contains("this.addMagicList(cha, true)"));

        String charBuff = read(RETAINED.resolve(
                "src/com/lineage/server/datatables/user/CharBuffTable.java"));
        String persisted = charBuff.substring(charBuff.indexOf("_buffSkill ="),
                charBuff.indexOf("private static CharBuffTable"));
        assertFalse(persisted.contains("ILLUSION_AVATAR"));
    }

    @Test void implementationCoversBothPhasesAllFourEffectsAndLifecycle()
            throws Exception {
        String catalog = read(Path.of(
                "src/main/java/local/lito381/server/BattleSpellCatalog.java"));
        String status = read(Path.of(
                "src/main/java/local/lito381/server/BattleAbilityStatus.java"));
        String session = read(Path.of(
                "src/main/java/local/lito381/server/ClientSession.java"));
        for (String fragment : new String[] {
                "ILLUSION_AVATAR, new IllusionAvatarSpell(",
                "ILLUSION_AVATAR, 27, 2, 19, 6_575",
                "35, 5_000, 0, 64, 3",
                "1, 2, 4, 10, 10, 8, 1.5D",
                "49_135L, \"$5699\", \"$5953\", true, false"}) {
            assertTrue(catalog.contains(fragment), fragment);
        }
        for (String fragment : new String[] {
                "activateIllusionAvatar(", "illusionAvatar(long nowNanos)",
                "expireIllusionAvatar(", "clearIllusionAvatar();",
                "boolean illusionAvatar,"}) {
            assertTrue(status.contains(fragment), fragment);
        }
        for (String fragment : new String[] {
                "BattleSpellCatalog.illusionAvatarSpell(",
                "useIllusionAvatarSpell(payload,",
                "resolveSelectorOnlyIllusionAvatarTarget(",
                "target.abilityStatus.activateIllusionAvatar(",
                "abilityStatus.illusionAvatar(physicalAttackNowNanos)",
                "abilityStatus.illusionAvatar(nowNanos)",
                ".applyIncomingDamage(damage)",
                "abilityStatus.expireIllusionAvatar(nowNanos)",
                "abilityStatus.illusionAvatar(nowNanos).spellPower()"}) {
            assertTrue(session.contains(fragment), fragment);
        }
    }

    private static String skillRow(String sql, int skillId) {
        return sql.lines().filter(line -> line.startsWith(
                "INSERT INTO `skills` VALUES (" + skillId + ","))
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
