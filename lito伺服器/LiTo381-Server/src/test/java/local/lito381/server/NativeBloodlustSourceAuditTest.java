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
import org.junit.jupiter.api.Test;

/** Cross-source proof for native Dragon Knight skill186 Bloodlust. */
final class NativeBloodlustSourceAuditTest {
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

    @Test void selectedDatabasesPinPrimaryReuseResourcesAndOptionalTalentRows()
            throws Exception {
        String primary = read(PRIMARY);
        String secondary = read(SECONDARY);
        assertEquals("INSERT INTO `skills` VALUES (186, 0, '血之渴望', 24, 1,"
                + " 0, 30, 49157, 1, 10, 60, 'none', 0, 0, 0, 0, 0, 0, 0,"
                + " 2, 0, 0, 0, 0, 2, '$5925', 19, 6523, 0, 0, 0, 0);",
                skillRow(primary, 186));
        assertEquals("INSERT INTO `skills` VALUES (186, '血之渴望', 24, 1, 0,"
                + " 30, 49157, 1, 0, 60, 'none', 0, 0, 0, 0, 0, 0, 0, 2,"
                + " 0, 0, 0, 0, 2, '$5925', 19, 6523, 0, 0, 0, 0);",
                skillRow(secondary, 186));
        assertTrue(primary.contains("INSERT INTO `skills_item` VALUES"
                + " (186, '血之渴望', NULL, NULL);"));
        assertTrue(secondary.contains("INSERT INTO `skills_item` VALUES"
                + " (186, '血之渴望', NULL, NULL);"));
        assertTrue(primary.contains("49107, '龍騎士書版(血之渴望)'"));

        List<String> talentRows = primary.lines()
                .filter(line -> line.startsWith(
                        "INSERT INTO `skill_狀態天賦設置` VALUES (2603, 186,"))
                .toList();
        assertEquals(9, talentRows.size());
        for (int level = 1; level <= 9; level++) {
            assertTrue(talentRows.get(level - 1).contains(
                    ", " + level + ", " + level + ", " + (level * 10) + ","));
        }
        // These nine rows belong to the selected server's optional talent
        // extension.  They are evidence, but not a native LiTo.bin producer.
        String session = read(Path.of(
                "src/main/java/local/lito381/server/ClientSession.java"));
        assertFalse(session.contains("2603"));
    }

    @Test void selectedLiToUsesGenericSelectorOnlyProducerAndZeroClientClock()
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
                0x635774L + (long) (BattleSpellCatalog.BLOODLUST - 1) * 2L));
    }

    @Test void retainedRuntimePinsBraveModeAttackOnlyRateAndPacketShape()
            throws Exception {
        String mode = read(RETAINED.resolve(
                "src/com/lineage/server/model/skill/skillmode/BLOODLUST.java"));
        assertTrue(mode.contains("L1BuffUtil.braveStart(pc)"));
        assertTrue(mode.contains("setSkillEffect(L1SkillId.BLOODLUST, integer * 1000)"));
        assertTrue(mode.contains("setBraveSpeed(6)"));
        assertTrue(mode.contains("new S_SkillBrave(pc.getId(), 6, integer)"));
        assertTrue(mode.contains("new S_SkillBrave(pc.getId(), 6, 0)"));
        assertTrue(mode.contains("new S_SkillBrave(pc.getId(), 0, 0)"));

        String packet = read(RETAINED.resolve(
                "src/com/lineage/server/serverpackets/S_SkillBrave.java"));
        int constructor = packet.indexOf("public S_SkillBrave(final int objid,");
        String wire = packet.substring(constructor,
                packet.indexOf("public S_SkillBrave(final int objid, final int mode)",
                        constructor));
        assertTrue(wire.contains("writeC(S_OPCODE_SKILLBRAVE)"));
        assertTrue(wire.contains("writeD(objid)"));
        assertTrue(wire.contains("writeC(mode)"));
        assertTrue(wire.contains("writeH(time)"));

        String interval = read(RETAINED.resolve(
                "src/com/lineage/server/model/ActionIntervalCalculator.java"));
        assertTrue(interval.contains(
                "type == ActionType.ATTACK && state.fastAttackable()"));
        assertTrue(interval.contains("interval = multiply(interval, 0.775)"));
        String pc = read(RETAINED.resolve(
                "src/com/lineage/server/model/Instance/L1PcInstance.java"));
        assertTrue(pc.contains("boolean isFastAttackable()"));
        assertTrue(pc.contains("return this.hasSkillEffect(BLOODLUST)"));

        String use = read(RETAINED.resolve(
                "src/com/lineage/server/model/skill/L1SkillUse.java"));
        assertTrue(use.contains("STATUS_ELFBRAVE, STATUS_RIBRAVE, BLOODLUST"));
        assertTrue(use.indexOf("commitMaterialAndAcquireCastLocks()")
                < use.indexOf("runSkill();", use.indexOf(
                        "commitMaterialAndAcquireCastLocks()")));
        assertTrue(use.indexOf("runSkill();", use.indexOf(
                        "commitMaterialAndAcquireCastLocks()"))
                < use.indexOf("useConsume();"));
        assertFalse(primaryContainsCancellationImmunity());
    }

    @Test void implementationKeepsWorldBattleResourcesCancellationAndTimingConnected()
            throws Exception {
        BattleSpellCatalog.BloodlustSpell spell =
                BattleSpellCatalog.bloodlustSpell(186);
        assertEquals(10, spell.reuseDelayMillis());
        assertEquals(30, spell.hpCost());
        assertEquals(49_157, spell.itemConsumeId());
        assertEquals(60, spell.durationSeconds());
        assertEquals(6, spell.braveMode());
        assertEquals(775, spell.attackIntervalMillis(1_000));
        assertTrue(spell.counterMagicBypass());
        assertFalse(spell.cancellationImmune());

        String session = read(Path.of(
                "src/main/java/local/lito381/server/ClientSession.java"));
        for (String fragment : new String[] {
                "useBloodlustSpell(payload, worldBloodlust)",
                "useBloodlustSpell(payload, bloodlustSpell)",
                "Protocol.selectorOnlySpellRequest(payload)",
                "nextInventory.removeCatalog(",
                "playerHp -= spell.hpCost()",
                "timedStatus.activateBrave(",
                "activeMovementBuffSkillId = spell.skillId()",
                "broadcastBraveSpeed(Protocol.BraveMode.fromWire(spell.braveMode()),",
                "GameStateStore.TimedMovementBuff.none()",
                "target.activeMovementBuffSkillId = 0",
                "timedStatus.expireBrave(nowNanos)"}) {
            assertTrue(session.contains(fragment), "missing implementation: " + fragment);
        }
        assertEquals(1_240, BattleActionInterval.attackMillis(
                61, 1, false, true, true));
    }

    private static boolean primaryContainsCancellationImmunity() throws Exception {
        return read(PRIMARY).contains("INSERT INTO `skills_相消免疫技能` VALUES"
                + " (186, '血之渴望');");
    }

    private static String skillRow(String sql, int skillId) {
        String prefix = "INSERT INTO `skills` VALUES (" + skillId + ",";
        return sql.lines().filter(line -> line.startsWith(prefix))
                .findFirst().orElseThrow();
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
