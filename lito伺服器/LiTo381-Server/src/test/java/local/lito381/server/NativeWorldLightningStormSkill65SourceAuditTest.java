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

/** Locks selected skill65 producer, source rows and both target-area paths. */
final class NativeWorldLightningStormSkill65SourceAuditTest {
    private static final Path RETAINED = Path.of("..", "1-伺服器")
            .toAbsolutePath().normalize();
    private static final Path SELECTED_CLIENT = Path.of(
            "D:/380/天堂(Lineage 3.81C) (1)/LiTo.bin");
    private static final Path UNPACKED = Path.of(
            "D:/lineage開發/LiTo381-開發/runtime/LiTo.unpacked-oep-current.exe");
    private static final Path DISASSEMBLY = Path.of(
            "D:/lineage開發/LiTo381-開發/ida-client-oep-v2/full-disassembly.asm");

    @Test
    void selectedAndSecondaryRowsPinLightningStormReuseDivergence()
            throws Exception {
        String selected = read(RETAINED.resolve("db/380_db.sql"));
        String secondary = read(RETAINED.resolve(
                "其他src參考用/0507備份/yiwei380/db/380yiwei_db.sql"));
        assertEquals("INSERT INTO `skills` VALUES (65, 0, '雷霆風暴', 9, 0,"
                + " 48, 0, 0, 0, 2000, 0, 'attack', 3, 20, 10, 5, 0, 0,"
                + " 8, 64, 0, 10, 3, 0, 1, '$1476', 18, 3924, 0, 0, 0,"
                + " 0);", skillRow(selected, 65));
        assertEquals("INSERT INTO `skills` VALUES (65, '雷霆風暴', 9, 0, 48,"
                + " 0, 0, 0, 500, 0, 'attack', 3, 20, 10, 5, 0, 0, 8,"
                + " 64, 0, 10, 3, 0, 1, '$1476', 18, 3924, 0, 0, 0, 0);",
                skillRow(secondary, 65));
        for (String sql : new String[] {selected, secondary}) {
            assertTrue(sql.contains("INSERT INTO `skills_item` VALUES"
                    + " (65, '雷霆風暴', NULL, NULL);"));
            assertFalse(sql.contains(
                    "INSERT INTO `skills_負面技能控制` VALUES (65,"));
            assertFalse(sql.contains(
                    "INSERT INTO `skills_負面boss免疫` VALUES (65,"));
            assertFalse(sql.contains(
                    "INSERT INTO `skills_buff_ability` VALUES (65,"));
        }
    }

    @Test
    void selectedLiToCase64IsCccdhhWithFiveHundredMillisecondClock()
            throws Exception {
        assertEquals("CE6EE68ADA8FD37E6C096B1A27E46CB6888F1179F85DEFB5007AC60287360292",
                sha256(SELECTED_CLIENT));
        String assembly = read(DISASSEMBLY).replace("\r\n", "\n");
        String producer = between(assembly, "loc_4CB755:", "loc_4CB78B:");
        for (String anchor : new String[] {
                "jumptable 004CB66E cases 3,5,6,17,28,37,44,49,64,73,76,132,151,152,172,173",
                "mov     edx, 0Ah", "call    sub_4CA6E0"
        }) assertTrue(producer.contains(anchor), () -> "missing producer " + anchor);
        String shared = between(assembly,
                "sub_4CA6E0 proc near", "sub_4CA6E0 endp");
        assertTrue(shared.contains("push    offset aCccdhh"));
        assertTrue(shared.contains("push    14h"));
        assertEquals("cccdhh\0", new String(
                peBytesAtVa(UNPACKED, 0x00636028L, 7),
                StandardCharsets.US_ASCII));
        assertEquals(1, Byte.toUnsignedInt(
                peBytesAtVa(UNPACKED, 0x00635A44L + 64L, 1)[0]));
        assertEquals(500,
                readPeWordAtVa(UNPACKED, 0x00635774L + 64L * 2L));
    }

    @Test
    void catalogRetainedTargetListAndTypeEightPacketStayExact()
            throws Exception {
        BattleSpellCatalog.AreaAttackSpell spell =
                BattleSpellCatalog.areaAttackSpell(
                        BattleSpellCatalog.LIGHTNING_STORM);
        assertEquals(65, spell.skillId());
        assertEquals(8, spell.row());
        assertEquals(0, spell.column());
        assertEquals(48, spell.mpCost());
        assertEquals(0, spell.hpCost());
        assertEquals(2_000, spell.reuseDelayMillis());
        assertEquals(10, spell.maximumRange());
        assertEquals(3, spell.areaRadius());
        assertEquals(20, spell.baseDamage());
        assertEquals(5, spell.diceCount());
        assertEquals(10, spell.dieSides());
        assertEquals(BattleSpellCatalog.ATTRIBUTE_WIND, spell.attribute());
        assertEquals(0, spell.lawfulDelta());
        assertEquals(18, spell.actionId());
        assertEquals(3924, spell.effectGfx());
        assertEquals(BattleSpellCatalog.RequestShape.OBJECT_TARGET,
                spell.requestShape());

        byte[] request = new Protocol.Writer().c(Protocol.C_USE_SPELL)
                .c(spell.row()).c(spell.column()).d(16)
                .h(32_820).h(32_766).bytes();
        assertEquals(11, request.length);
        Protocol.AttackSpellRequest parsed = Protocol.attackSpellRequest(request);
        assertEquals(spell.skillId(), parsed.skillId());
        assertEquals(16, parsed.targetObjectId());
        assertEquals(32_820, parsed.x());
        assertEquals(32_766, parsed.y());
        byte[] response = Protocol.attackMany(spell.actionId(), 3,
                32_819, 32_766, 2, 1, spell.effectGfx(),
                List.of(new Protocol.AttackManyTarget(16, true)));
        assertEquals(Protocol.S_ATTACK_MANY, Byte.toUnsignedInt(response[0]));
        assertEquals(8, Byte.toUnsignedInt(response[17]));

        String useSkill = read(RETAINED.resolve(
                "src/com/lineage/server/model/skill/L1SkillUse.java"));
        for (String anchor : new String[] {
                "objects = World.get().getVisibleObjects(this._target, this._skill.getArea());",
                "this.isUseCounterMagic(cha)",
                "S_RangeSkill.TYPE_DIR, true",
                "S_RangeSkill.TYPE_DIR, false"
        }) assertTrue(useSkill.contains(anchor), () -> "missing retained use " + anchor);
        String range = read(RETAINED.resolve(
                "src/com/lineage/server/serverpackets/S_RangeSkill.java"));
        for (String anchor : new String[] {
                "public static final int TYPE_DIR = 8;",
                "this.writeC(actionId);", "this.writeD(cha.getId());",
                "this.writeH(spellgfx);", "this.writeC(type);",
                "this.writeH(targetList.size());", "this.writeD(resolvedId);"
        }) assertTrue(range.contains(anchor), () -> "missing retained range " + anchor);
    }

    @Test
    void bothPhasesLockAndRevalidatePrimaryThenCommitBeforeCleanupAndPublish()
            throws Exception {
        String session = read(Path.of(
                "src/main/java/local/lito381/server/ClientSession.java"));
        String worldRouter = between(session,
                "BattleSpellCatalog.AreaAttackSpell worldAreaAttack =",
                "BattleSpellCatalog.SummonSpell worldSummon =");
        assertTrue(worldRouter.contains(
                "useClassicWorldAreaAttackSpell(payload, worldAreaAttack)"));

        String battle = between(session, "private void useAreaAttackSpell(",
                "private void useCenteredAreaAttackSpell(");
        for (String anchor : new String[] {
                "long originRoomId = currentRoomId;",
                "int originMapId = hub.battleMapId(originRoomId, this);",
                "int originX = positionX;", "int originY = positionY;",
                "ClientSession firstLock", "synchronized (firstLock)",
                "synchronized (secondLock)", "primaryStillAvailable",
                "endpointStillAllowed", "currentRoomId == originRoomId",
                "positionX == originX && positionY == originY",
                "BattleSpellCatalog.hasNativeLineOfSight(",
                "BattleAbsoluteBarrierRules.blocksIncomingSpell("
        }) assertTrue(battle.contains(anchor), () -> "missing BATTLE lease " + anchor);
        assertCommitCleanupPublishOrder(battle,
                "cancelMeditation(\"spell-\" + spell.skillId())",
                "cancelAbsoluteBarrier(\"spell-\" + spell.skillId())",
                "revealInvisibility(\"spell-\" + spell.skillId())");
        assertFalse(battle.contains("commitSpellReady("));

        String world = between(session,
                "private void useClassicWorldAreaAttackSpell(",
                "private void publishClassicWorldAreaAttackSpellPacket(");
        for (String anchor : new String[] {
                "int originMapId = classicWorldMapId;",
                "int originInstanceId = classicWorldInstanceId;",
                "int originX = positionX;", "int originY = positionY;",
                "ClientSession firstLock", "synchronized (firstLock)",
                "synchronized (secondLock)", "lockedOrdinary",
                "lockedTrebuchet", "lockedCastleTower",
                "lockedCastleDoor", "primaryStillAvailable",
                "endpointStillAllowed", "positionX == originX && positionY == originY",
                "primaryPlayer.positionX == targetX",
                "BattleAbsoluteBarrierRules.blocksIncomingSpell("
        }) assertTrue(world.contains(anchor), () -> "missing WORLD lease " + anchor);
        assertCommitCleanupPublishOrder(world,
                "cancelMeditation(\"classic-world-spell-\" + spell.skillId())",
                "cancelAbsoluteBarrier(\"classic-world-spell-\" + spell.skillId())",
                "revealInvisibility(\"classic-world-spell-\" + spell.skillId())");
        assertFalse(world.contains("commitSpellReady("));
    }

    private static void assertCommitCleanupPublishOrder(
            String method, String meditationText, String barrierText,
            String invisibilityText) {
        int hp = method.indexOf("playerHp -= spell.hpCost()");
        int mp = method.indexOf("playerMp -= mpCost");
        int resources = Math.max(hp, mp);
        int reuse = method.indexOf("spellReadyNanos.put(spell.skillId()", resources);
        int meditation = method.indexOf(meditationText, reuse);
        int barrier = method.indexOf(barrierText, meditation);
        int invisibility = method.indexOf(invisibilityText, barrier);
        int publish = method.indexOf("Protocol.attackMany(", invisibility);
        assertTrue(hp >= 0 && mp >= 0 && reuse > resources && meditation > reuse
                && barrier > meditation && invisibility > barrier
                && publish > invisibility,
                () -> "order=" + List.of(hp, mp, reuse, meditation,
                        barrier, invisibility, publish));
    }

    private static String skillRow(String sql, int skillId) {
        String prefix = "INSERT INTO `skills` VALUES (" + skillId + ",";
        return sql.lines().filter(line -> line.startsWith(prefix))
                .findFirst().orElseThrow();
    }

    private static String between(String text, String start, String end) {
        int from = text.indexOf(start);
        int to = text.indexOf(end, from + start.length());
        assertTrue(from >= 0 && to > from,
                () -> "missing source boundary " + start + " -> " + end);
        return text.substring(from, to);
    }

    private static byte[] peBytesAtVa(Path path, long va, int length)
            throws Exception {
        byte[] bytes = Files.readAllBytes(path);
        ByteBuffer little = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN);
        int pe = little.getInt(0x3C);
        int sectionCount = Short.toUnsignedInt(little.getShort(pe + 6));
        int optionalSize = Short.toUnsignedInt(little.getShort(pe + 20));
        int optional = pe + 24;
        long imageBase = Integer.toUnsignedLong(little.getInt(optional + 28));
        long rva = va - imageBase;
        int table = optional + optionalSize;
        for (int index = 0; index < sectionCount; index++) {
            int section = table + index * 40;
            long virtualSize = Integer.toUnsignedLong(little.getInt(section + 8));
            long sectionRva = Integer.toUnsignedLong(little.getInt(section + 12));
            long rawSize = Integer.toUnsignedLong(little.getInt(section + 16));
            long rawOffset = Integer.toUnsignedLong(little.getInt(section + 20));
            if (sectionRva <= rva
                    && rva + length <= sectionRva + Math.max(virtualSize, rawSize)) {
                int offset = Math.toIntExact(rawOffset + rva - sectionRva);
                byte[] result = new byte[length];
                System.arraycopy(bytes, offset, result, 0, length);
                return result;
            }
        }
        throw new IllegalArgumentException("unmapped PE address " + Long.toHexString(va));
    }

    private static int readPeWordAtVa(Path path, long va) throws Exception {
        return Short.toUnsignedInt(ByteBuffer.wrap(peBytesAtVa(path, va, 2))
                .order(ByteOrder.LITTLE_ENDIAN).getShort());
    }

    private static String read(Path path) throws Exception {
        return Files.readString(path, StandardCharsets.UTF_8);
    }

    private static String sha256(Path path) throws Exception {
        return HexFormat.of().withUpperCase().formatHex(
                MessageDigest.getInstance("SHA-256")
                        .digest(Files.readAllBytes(path)));
    }
}
