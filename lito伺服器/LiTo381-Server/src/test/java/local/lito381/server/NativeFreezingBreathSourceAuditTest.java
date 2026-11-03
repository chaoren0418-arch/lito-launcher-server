package local.lito381.server;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

/** Cross-source proof for native Dragon Knight skill194 Freezing Breath. */
final class NativeFreezingBreathSourceAuditTest {
    private static final Path RETAINED = Path.of("..", "1-伺服器")
            .toAbsolutePath().normalize();
    private static final Path PRIMARY = RETAINED.resolve("db/380_db.sql");
    private static final Path SECONDARY = RETAINED.resolve(
            "其他src參考用/0507備份/yiwei380/db/380yiwei_db.sql");
    private static final Path UNPACKED = Path.of(
            "D:/lineage開發/LiTo381-開發/runtime/LiTo.unpacked-oep-current.exe");
    private static final Path DISASSEMBLY = Path.of(
            "D:/lineage開發/LiTo381-開發/ida-client-oep-v2/full-disassembly.asm");

    @Test void selectedRowsPinHpReuseLineDamageProbabilityWardAndCancellation()
            throws Exception {
        String primary = read(PRIMARY);
        String secondary = read(SECONDARY);
        assertEquals("INSERT INTO `skills` VALUES (194, 0, '暴龍之眼', 25, 1,"
                + " 0, 50, 0, 0, 500, 3, 'attack', 3, 45, 0, 0, 0, 5, 4,"
                + " 64, 0, 10, 0, 0, 2, '$5933', 18, 6988, 0, 0, 0, 0);",
                skillRow(primary, 194));
        assertEquals("INSERT INTO `skills` VALUES (194, '暴龍之眼', 25, 1,"
                + " 0, 50, 0, 0, 0, 3, 'attack', 3, 45, 0, 0, 0, 5, 4,"
                + " 64, 0, 10, 0, 0, 2, '$5933', 18, 6988, 0, 0, 0, 0);",
                skillRow(secondary, 194));
        String probability = "INSERT INTO `skills_負面技能控制` VALUES"
                + " (194, '寒冰噴吐-寒冰耐性', 52, 44, 36, 0, 0, 0.38,"
                + " 1, 0, 1, 0.05, 0, 0);";
        assertTrue(primary.contains(probability));
        assertTrue(secondary.contains(probability));
        assertTrue(primary.contains("INSERT INTO `skills_負面boss免疫` VALUES"
                + " (194, '寒冰噴吐', 0);"));
        assertTrue(primary.contains("INSERT INTO `skills_item` VALUES"
                + " (194, '寒冰噴吐', NULL, NULL);"));
        assertFalse(primary.contains(
                "INSERT INTO `skills_魔屏無效技能` VALUES (194,"));
        assertFalse(primary.contains(
                "INSERT INTO `skills_相消無效技能` VALUES (194,"));
    }

    @Test void selectedClientEmits141801AndUses1800MillisecondClock()
            throws Exception {
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
        assertEquals(1_800, readPeWordAtVa(UNPACKED,
                0x635774L
                        + (long) (BattleSpellCatalog.FREEZING_BREATH - 1) * 2L));
        assertEquals("14 18 01", Protocol.hex(
                new byte[] {0x14, 0x18, 0x01}));
    }

    @Test void retained381PinsTailDivergenceVisibleLineFreezeEffectAndLocks()
            throws Exception {
        String packet = read(RETAINED.resolve(
                "其他src參考用/381n8src/com/lineage/server/clientpackets/C_UseSkill.java"));
        int skill = packet.indexOf("case 194:// 寒冰噴吐");
        int targetRead = packet.indexOf("targetId = readD();", skill);
        assertTrue(skill >= 0);
        assertTrue(targetRead > skill);

        String retained = read(RETAINED.resolve(
                "其他src參考用/381n8src/com/lineage/server/model/skill/L1SkillUse.java"));
        assertTrue(retained.contains("case FREEZING_BREATH:"));
        assertTrue(retained.contains(
                "getVisibleLineObjects(this._user, this._target)"));
        assertTrue(retained.contains("L1SpawnUtil.spawnEffect(81168,"));
        assertTrue(retained.contains("new S_Poison(pc.getId(), 2)"));
        assertTrue(retained.contains("S_Paralysis.TYPE_FREEZE, true"));
        assertTrue(retained.contains("npc.setParalyzed(true);"));
    }

    @Test void implementationConnectsSelectorLineResourcesWardFreezeAndLifecycle()
            throws Exception {
        String catalog = read(Path.of(
                "src/main/java/local/lito381/server/BattleSpellCatalog.java"));
        assertTrue(catalog.contains("FREEZING_BREATH = 194"));
        assertTrue(catalog.contains("0, 50, 500, 1_800, 10, 0, true"));
        assertTrue(catalog.contains("\"freezing-breath\""));
        String session = read(Path.of(
                "src/main/java/local/lito381/server/ClientSession.java"));
        assertTrue(session.contains(
                "useSelectorOnlyTargetedFreezeAttackSpell(payload, worldFreeze)"));
        assertTrue(session.contains(
                "useClassicWorldLineFreezeAttackSpell(request, spell)"));
        assertTrue(session.contains("resolveSelectorOnlyPrimary("));
        assertTrue(session.contains("BattleStraightLine.points("));
        assertTrue(session.contains("config.freezingBreathFixtureD100()"));
        assertTrue(session.contains("target.timedStatus.consumeCounterMagic(castNow)"));
        assertTrue(session.contains("target.timedStatus.activateFreeze("));
        assertTrue(session.contains("hub.freezeQuestMonster("));
        assertTrue(session.contains("hub.freezeTimeAttackActor("));
        assertTrue(session.contains("hub.freezeClassicWorldMonster("));
        assertTrue(session.contains("placeBattleFreezeEffect("));
        assertTrue(session.contains("placeClassicWorldFreezeEffect("));
        assertTrue(session.contains("removeBattleFreezeEffectAndPublish("));
        assertTrue(session.contains("playerCancellation.timed().freeze()"));
        assertTrue(session.contains("freezeCancelled = questCancellation != null"));
        String effects = read(Path.of(
                "src/main/java/local/lito381/server/BattleFreezeEffects.java"));
        assertTrue(effects.contains("static final int NPC_ID = 81168"));
        assertTrue(effects.contains("static final int GFX = 176"));
        assertTrue(session.contains("hub.expireFreezeEffects("));
        assertTrue(session.contains("hub.expireClassicWorldFreezeEffects("));
        String coverage = read(Path.of("tools/protocol_coverage.py"));
        assertTrue(coverage.contains(
                "skill194 Freezing Breath is selected-client selector-only"
                        + " C_USE_SPELL 14 18 01"));
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
}
