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

/** Cross-source proof for native Dragon Knight skill190 Awaken: Fafurion. */
final class NativeAwakenFafurionSourceAuditTest {
    @BeforeAll
    static void assumeLocalResourcesPresent() {
        LocalResources.assumeLineageSources(
                Path.of("../1-伺服器/db/380_db.sql"),
                Path.of("../1-伺服器/src/com/lineage/server/clientpackets/C_UseSkill.java"),
                Path.of("../1-伺服器/src/com/lineage/server/model/skill/L1SkillUse.java"),
                Path.of("../1-伺服器/src/com/lineage/server/model/skill/skillmode/AWAKEN_FAFURION.java"),
                Path.of("../1-伺服器/src/com/lineage/server/timecontroller/skill/Skill_Awake_Timer.java"),
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
    private static final Path SELECTED_CLIENT = Path.of(
            "D:/380/天堂(Lineage 3.81C) (1)/LiTo.bin");
    private static final Path UNPACKED = Path.of(
            "D:/lineage開發/LiTo381-開發/runtime/LiTo.unpacked-oep-current.exe");
    private static final Path DISASSEMBLY = Path.of(
            "D:/lineage開發/LiTo381-開發/ida-client-oep-v2/full-disassembly.asm");

    @Test void selectedRowsPinResourcesImmunityAndDragonActions() throws Exception {
        String primary = read(PRIMARY);
        String secondary = read(SECONDARY);
        assertEquals("INSERT INTO `skills` VALUES (190, 0, '覺醒：法利昂',"
                + " 24, 5, 0, 1, 49157, 1, 0, 0, 'none', 0, 0, 0, 0, 0,"
                + " 0, 0, 2, 0, 0, 0, 0, 32, '$5929', 19, 6976, 0, 0, 0, 0);",
                skillRow(primary, 190));
        assertEquals("INSERT INTO `skills` VALUES (190, '覺醒：法利昂', 24,"
                + " 5, 0, 1, 49157, 1, 0, 0, 'none', 0, 0, 0, 0, 0, 0, 0,"
                + " 2, 0, 0, 0, 0, 32, '$5929', 19, 6976, 0, 0, 0, 0);",
                skillRow(secondary, 190));
        assertTrue(primary.contains("INSERT INTO `skills_相消免疫技能` VALUES"
                + " (190, '覺醒：法利昂');"));
        assertTrue(primary.contains("INSERT INTO `skills_item` VALUES"
                + " (190, '覺醒：法利昂', NULL, NULL);"));
        assertTrue(primary.contains("NULL, 49111, '龍騎士書版(覺醒：法利昂)'"));
        assertFalse(primary.contains("INSERT INTO `skills_buff_ability` VALUES"
                + " (190,"));
        for (int action : new int[] {0, 1, 18, 19, 4, 5, 11, 12,
                24, 25, 50, 51, 62, 63}) {
            assertTrue(primary.contains("INSERT INTO `spr_action` VALUES (9364, "
                    + action + ","), "missing dragon action " + action);
        }
    }

    @Test void selectedLiToUsesSelectorOnlyProducerAndZeroClientClock()
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
                0x635774L + (long) (BattleSpellCatalog.AWAKEN_FAFURION - 1) * 2L));
    }

    @Test void retained381RuntimePinsStatsToggleDrainWhitelistAndOrdering()
            throws Exception {
        String packet = read(RETAINED.resolve(
                "src/com/lineage/server/clientpackets/C_UseSkill.java"));
        int awaken = packet.indexOf("case 190:// 覺醒：法利昂");
        String targetFamily = packet.substring(awaken,
                packet.indexOf("break;", awaken));
        assertTrue(targetFamily.contains("targetId = readD();"));
        assertFalse(targetFamily.contains("targetX = readH();"));

        String mode = read(RETAINED.resolve(
                "src/com/lineage/server/model/skill/skillmode/AWAKEN_FAFURION.java"));
        for (String fragment : new String[] {
                "getAwakeSkillId() == L1SkillId.AWAKEN_FAFURION",
                "L1SkillStop.stopSkill", "addMr(30)", "addWind(30)",
                "addWater(30)", "addFire(30)", "addEarth(30)",
                "setAwakeSkillId(L1SkillId.AWAKEN_FAFURION)",
                "L1BuffUtil.doPoly(srcpc)", "startMpReductionByAwake()",
                "setAwakeSkillId(0)", "L1BuffUtil.undoPoly(pc)"}) {
            assertTrue(mode.contains(fragment), "missing retained mode: " + fragment);
        }
        String timer = read(RETAINED.resolve(
                "src/com/lineage/server/timecontroller/skill/Skill_Awake_Timer.java"));
        assertTrue(timer.contains("tgpc.debitCurrentMpUpTo(8)"));
        String use = read(RETAINED.resolve(
                "src/com/lineage/server/model/skill/L1SkillUse.java"));
        assertTrue(use.contains("pc.sendPackets(new S_ServerMessage(1385))"));
        String exceptCounter = use.substring(use.indexOf("EXCEPT_COUNTER_MAGIC"),
                use.indexOf("};", use.indexOf("EXCEPT_COUNTER_MAGIC")));
        assertTrue(exceptCounter.contains("AWAKEN_FAFURION"));
        assertTrue(use.indexOf("commitMaterialAndAcquireCastLocks()")
                < use.indexOf("runSkill();", use.indexOf(
                        "commitMaterialAndAcquireCastLocks()")));
    }

    @Test void implementationConnectsStatsWireCombatAndBothPhaseDispatchers()
            throws Exception {
        BattleSpellCatalog.AwakenSpell spell = BattleSpellCatalog.awakenSpell(190);
        assertEquals(30, spell.magicResistanceModifier());
        assertEquals(30, spell.allElementalResistance());
        String session = read(Path.of(
                "src/main/java/local/lito381/server/ClientSession.java"));
        for (String fragment : new String[] {
                "useAwakenSpell(payload, worldAwaken)",
                "useAwakenSpell(payload, awakenSpell)",
                "spell.magicResistanceModifier()",
                "spell.allElementalResistance()",
                "awakenStatus.snapshot().magicResistanceModifier()",
                "awakenStatus.snapshot().allElementalResistance()",
                "sendMagicStatus(castNow, awakenName)",
                "serverAwakeningMpDrain(nowNanos)",
                "S_MESSAGE_CODE awakening-spell-rejected=1385"}) {
            assertTrue(session.contains(fragment), "missing implementation: " + fragment);
        }
        int cancellation = session.indexOf("private void useCancellationSpell(");
        int nextMethod = session.indexOf("\n    private void ", cancellation + 10);
        assertFalse(session.substring(cancellation, nextMethod)
                .contains("awakenStatus.clear"));
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
