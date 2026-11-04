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
import java.util.List;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/** Cross-source proof for native Dragon Knight skill187 Foe Slayer. */
final class NativeFoeSlayerSourceAuditTest {
    @BeforeAll
    static void assumeLocalResourcesPresent() {
        LocalResources.assumeLineageSources(
                Path.of("../1-伺服器/db/380_db.sql"),
                Path.of("../1-伺服器/src/com/lineage/server/model/skill/skillmode/FOE_SLAYER.java"),
                Path.of("../1-伺服器/其他src參考用/0507備份/yiwei380/db/380yiwei_db.sql"),
                Path.of("../1-伺服器/其他src參考用/rk760src/com/lineage/server/model/L1AttackPc.java"),
                Path.of("../1-伺服器/其他src參考用/rk760src/com/lineage/server/model/skill/skillmode/FOE_SLAYER.java"));
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
    private static final Path SELECTED_CLIENT = Path.of(
            "D:/380/天堂(Lineage 3.81C) (1)/LiTo.bin");
    private static final Path UNPACKED = Path.of(
            "D:/lineage開發/LiTo381-開發/runtime/LiTo.unpacked-oep-current.exe");
    private static final Path DISASSEMBLY = Path.of(
            "D:/lineage開發/LiTo381-開發/ida-client-oep-v2/full-disassembly.asm");

    @Test void selectedDatabasesPinPrimaryMetadataAndOptionalTalentBoundary()
            throws Exception {
        String primary = read(PRIMARY);
        String secondary = read(SECONDARY);
        assertEquals("INSERT INTO `skills` VALUES (187, 0, '屠宰者', 24, 2,"
                + " 0, 20, 0, 0, 2000, 0, 'attack', 3, 0, 0, 0, 0, 0, 0,"
                + " 64, 0, 2, 0, 0, 4, '$5926', 18, 0, 0, 0, 0, 0);",
                skillRow(primary, 187));
        assertEquals("INSERT INTO `skills` VALUES (187, '屠宰者', 24, 2, 0,"
                + " 20, 0, 0, 300, 0, 'attack', 3, 0, 0, 0, 0, 0, 0, 64,"
                + " 0, 2, 0, 0, 4, '$5926', 18, 0, 0, 0, 0, 0);",
                skillRow(secondary, 187));
        assertTrue(primary.contains("INSERT INTO `skills_item` VALUES"
                + " (187, '屠宰者', NULL, NULL);"));
        assertTrue(secondary.contains("INSERT INTO `skills_item` VALUES"
                + " (187, '屠宰者', NULL, NULL);"));
        assertTrue(primary.contains("INSERT INTO `skills_魔屏無效技能` VALUES"
                + " (187, '屠宰者');"));
        assertTrue(primary.contains("49108, '龍騎士書版(屠宰者)'"));

        List<String> talentRows = primary.lines()
                .filter(line -> line.startsWith(
                        "INSERT INTO `skill_傷害天賦設置` VALUES (1602, 187,"))
                .toList();
        assertEquals(9, talentRows.size());
        for (int level = 1; level <= 9; level++) {
            assertTrue(talentRows.get(level - 1).contains(
                    ", " + level + ", " + level + ", " + (level * 110) + ","));
        }
        // Talent 1602 is a selected-server extension without a proven native
        // LiTo producer and is deliberately outside the native Foe Slayer path.
        assertFalse(read(Path.of(
                "src/main/java/local/lito381/server/ClientSession.java"))
                .contains("1602"));
    }

    @Test void selectedLiToReachesSkill187ThroughGenericSelectorOnlyProducer()
            throws Exception {
        assertEquals("CE6EE68ADA8FD37E6C096B1A27E46CB6888F1179F85DEFB5007AC60287360292",
                sha256(SELECTED_CLIENT));
        String asm = read(DISASSEMBLY).replace("\r\n", "\n");
        String use = asm.substring(asm.indexOf("sub_4CB5F0 proc near"),
                asm.indexOf("sub_4CB5F0 endp") + 16);
        assertTrue(use.contains("cmp     esi, 0AFh"));
        assertTrue(use.contains("ja      def_4CB66E"));
        String generic = use.substring(use.indexOf("def_4CB66E:"));
        assertTrue(generic.contains("and     edx, 7"));
        assertTrue(generic.contains("sar     esi, 3"));
        assertTrue(generic.contains("push    edx\npush    esi\npush    14h\n"
                + "push    offset off_5028CC\ncall    sub_4C6AE0"));
        assertEquals(0, readPeWordAtVa(UNPACKED,
                0x635774L + (long) (BattleSpellCatalog.FOE_SLAYER - 1) * 2L));

        String receiver = asm.substring(asm.indexOf("sub_4B07C0 proc near"),
                asm.indexOf("sub_4B07C0 endp") + 16);
        assertTrue(receiver.contains("cmp     eax, 28h"));
        assertTrue(asm.contains("aSLitoClan db 'S_LITO_CLAN',0"));
        assertTrue(asm.contains("aDdsd db 'ddsd',0"));
        assertEquals(0xFA, Protocol.S_LITO_CLAN);
    }

    @Test void retainedClassicModePinsThreeAttacksExposureDamageAndEffects()
            throws Exception {
        Path classicRoot = RETAINED.resolve("其他src參考用/rk760src");
        String mode = read(classicRoot.resolve(
                "com/lineage/server/model/skill/skillmode/FOE_SLAYER.java"));
        assertTrue(mode.contains("srcpc.setFoeSlayer(true)"));
        assertTrue(mode.contains("for (int i = 0; i < 3; i++)"));
        assertTrue(mode.contains("cha.onAction(srcpc)"));
        assertTrue(mode.contains("srcpc.setFoeSlayer(false)"));
        assertTrue(mode.contains("srcpc.set_weaknss(0, 0)"));
        assertTrue(mode.contains("srcpc.getId(), 7020"));
        assertTrue(mode.contains("cha.getId(), 12119"));

        String attack = read(classicRoot.resolve(
                "com/lineage/server/model/L1AttackPc.java"));
        assertTrue(attack.contains("if (_pc.isFoeSlayer())"));
        assertTrue(attack.contains("_damage += 20 + doll"));
        assertTrue(attack.contains("_damage += 40 + doll"));
        assertTrue(attack.contains("_damage += 60 + doll"));
        assertTrue(attack.contains("if (_pc.isFoeSlayer()) {// 使用屠宰者"));
        assertTrue(attack.contains("return 0;"));

        String customized = read(RETAINED.resolve(
                "src/com/lineage/server/model/skill/skillmode/FOE_SLAYER.java"));
        assertTrue(customized.contains("for (int i = 0; i < 5; i++)"));
        assertTrue(customized.contains("ConfigSkill.FOE_SLAYER_DMG"));
        assertFalse(mode.contains("ConfigSkill.FOE_SLAYER_DMG"));
    }

    @Test void implementationConnectsBattleWorldResourcesAndThreeStrikeLease()
            throws Exception {
        BattleSpellCatalog.FoeSlayerSpell spell =
                BattleSpellCatalog.foeSlayerSpell(187);
        assertEquals(20, spell.hpCost());
        assertEquals(2_000, spell.reuseDelayMillis());
        assertEquals(3, spell.hitCount());
        assertEquals(60, spell.damageForExposureLevel(3));
        assertTrue(spell.counterMagicBypass());

        String session = read(Path.of(
                "src/main/java/local/lito381/server/ClientSession.java"));
        for (String fragment : new String[] {
                "useFoeSlayerSpell(payload, worldFoeSlayer)",
                "useFoeSlayerSpell(payload, foeSlayerSpell)",
                "resolveFoeSlayerTarget(",
                "playerHp -= spell.hpCost()",
                "chainswordWeakness.consumeFoeSlayer(",
                "foeSlayerSequence.begin(",
                "for (int strike = 0; strike < spell.hitCount(); strike++)",
                "attack(physical, AttackEnvelope.DIRECT)",
                "spell.attackerEffectGfx()",
                "spell.targetEffectGfx()",
                "foe-slayer-damage-bonus=",
                "selected-client-weakness-icon=unavailable"}) {
            assertTrue(session.contains(fragment), "missing implementation: " + fragment);
        }
        String legacyAudit = read(Path.of("tools/native_chainsword_audit.py"));
        assertFalse(legacyAudit.contains(
                "spell selector dispatches only zero-based indices 0..175; skill 187 is unreachable"));
    }

    private static String skillRow(String sql, int skillId) {
        String prefix = "INSERT INTO `skills` VALUES (" + skillId + ",";
        return sql.lines().filter(line -> line.startsWith(prefix))
                .findFirst().orElseThrow();
    }

    private static int readPeWordAtVa(Path path, long va) throws Exception {
        byte[] bytes = LocalResources.readAllBytes(path);
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
        assertTrue(Files.isRegularFile(LocalResources.assumeExternalFile(path)), "missing source: " + path);
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
