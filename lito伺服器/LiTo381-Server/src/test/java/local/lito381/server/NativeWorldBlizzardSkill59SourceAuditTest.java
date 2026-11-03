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

/** Locks selected skill59 producer, source rows and both centered-area paths. */
final class NativeWorldBlizzardSkill59SourceAuditTest {
    private static final Path RETAINED = Path.of("..", "1-伺服器")
            .toAbsolutePath().normalize();
    private static final Path SELECTED_CLIENT = Path.of(
            "D:/380/天堂(Lineage 3.81C) (1)/LiTo.bin");
    private static final Path UNPACKED = Path.of(
            "D:/lineage開發/LiTo381-開發/runtime/LiTo.unpacked-oep-current.exe");
    private static final Path DISASSEMBLY = Path.of(
            "D:/lineage開發/LiTo381-開發/ida-client-oep-v2/full-disassembly.asm");

    @Test
    void selectedAndSecondaryRowsAgreeOnNativeBlizzardContract()
            throws Exception {
        String selected = read(RETAINED.resolve("db/380_db.sql"));
        String secondary = read(RETAINED.resolve(
                "其他src參考用/0507備份/yiwei380/db/380yiwei_db.sql"));
        assertEquals("INSERT INTO `skills` VALUES (59, 0, '冰雪暴', 8, 2,"
                + " 60, 10, 0, 0, 1500, 0, 'none', 3, 2, 10, 3, 0, 0,"
                + " 4, 64, -10, 0, 10, 0, 4, '$1473', 18, 757, 0, 0,"
                + " 0, 0);", skillRow(selected, 59));
        assertEquals("INSERT INTO `skills` VALUES (59, '冰雪暴', 8, 2, 60,"
                + " 10, 0, 0, 1500, 0, 'none', 3, 2, 10, 3, 0, 0, 4,"
                + " 64, -10, 0, 10, 0, 4, '$1473', 18, 757, 0, 0, 0,"
                + " 0);", skillRow(secondary, 59));
        assertTrue(selected.contains(
                "INSERT INTO `skills_item` VALUES (59, '冰雪暴', NULL, NULL);"));
        assertTrue(secondary.contains(
                "INSERT INTO `skills_item` VALUES (59, '冰雪暴', NULL, NULL);"));
    }

    @Test
    void selectedLiToCase58IsSelectorOnlyAndUsesExact1500msClock()
            throws Exception {
        assertEquals("CE6EE68ADA8FD37E6C096B1A27E46CB6888F1179F85DEFB5007AC60287360292",
                sha256(SELECTED_CLIENT));
        String assembly = read(DISASSEMBLY).replace("\r\n", "\n");
        String producer = between(assembly, "loc_4CBB81:", "loc_4CBB9A:");
        for (String anchor : new String[] {
                "jumptable 004CB66E cases 52,58,61,69,79",
                "mov     al, byte_635A44[esi]", "call    sub_4DFDC0",
                "mov     eax, esi", "and     eax, 7",
                "jmp     short loc_4CBBDA"
        }) assertTrue(producer.contains(anchor), () -> "missing producer " + anchor);
        String tail = between(assembly, "loc_4CBBDA:", "loc_4CBBED:");
        for (String anchor : new String[] {
                "sar     esi, 3", "push    esi", "push    14h",
                "push    offset off_5028CC", "call    sub_4C6AE0"
        }) assertTrue(tail.contains(anchor), () -> "missing producer tail " + anchor);
        assertEquals("ccc\0", new String(
                peBytesAtVa(UNPACKED, 0x005028CCL, 4), StandardCharsets.US_ASCII));
        assertEquals(1, Byte.toUnsignedInt(
                peBytesAtVa(UNPACKED, 0x00635A44L + 58L, 1)[0]));
        assertEquals(1_500,
                readPeWordAtVa(UNPACKED, 0x00635774L + 58L * 2L));
    }

    @Test
    void catalogAndRetainedServerPinSkill59TypeZeroContract()
            throws Exception {
        BattleSpellCatalog.AreaAttackSpell spell =
                BattleSpellCatalog.areaAttackSpell(BattleSpellCatalog.BLIZZARD);
        assertEquals(59, spell.skillId());
        assertEquals(7, spell.row());
        assertEquals(2, spell.column());
        assertEquals(60, spell.mpCost());
        assertEquals(10, spell.hpCost());
        assertEquals(1_500, spell.reuseDelayMillis());
        assertEquals(0, spell.maximumRange());
        assertEquals(10, spell.areaRadius());
        assertEquals(2, spell.baseDamage());
        assertEquals(3, spell.diceCount());
        assertEquals(10, spell.dieSides());
        assertEquals(BattleSpellCatalog.ATTRIBUTE_WATER, spell.attribute());
        assertEquals(-10, spell.lawfulDelta());
        assertEquals(18, spell.actionId());
        assertEquals(757, spell.effectGfx());
        assertEquals(BattleSpellCatalog.RequestShape.SELECTOR_ONLY,
                spell.requestShape());

        byte[] request = new Protocol.Writer().c(Protocol.C_USE_SPELL)
                .c(spell.row()).c(spell.column()).bytes();
        assertEquals(3, request.length);
        assertEquals(spell.skillId(),
                Protocol.selectorOnlySpellRequest(request).skillId());
        byte[] response = Protocol.attackMany(spell.actionId(), 3,
                32_801, 32_885, 0, 1, spell.effectGfx(), 0,
                java.util.List.of(new Protocol.AttackManyTarget(16, true)));
        assertEquals(Protocol.S_ATTACK_MANY, Byte.toUnsignedInt(response[0]));
        assertEquals(0, Byte.toUnsignedInt(response[17]));

        String useSkill = read(RETAINED.resolve(
                "src/com/lineage/server/model/skill/L1SkillUse.java"));
        assertTrue(useSkill.contains("this._skill.getTarget().equals(\"none\")"));
        assertTrue(useSkill.contains(
                "new S_RangeSkill(_player, _targetList, castgfx, actionId,"));
        assertTrue(useSkill.contains("S_RangeSkill.TYPE_NODIR, true)"));
        String range = read(RETAINED.resolve(
                "src/com/lineage/server/serverpackets/S_RangeSkill.java"));
        for (String anchor : new String[] {
                "public static final int TYPE_NODIR = 0;",
                "this.writeC(S_OPCODE_RANGESKILLS);", "this.writeC(actionId);",
                "this.writeD(cha.getId());", "this.writeH(spellgfx);",
                "this.writeC(type);", "this.writeH(0x0000);",
                "this.writeH(targetList.size());", "this.writeH(0x20);"
        }) assertTrue(range.contains(anchor), () -> "missing retained range " + anchor);
    }

    @Test
    void battleAndWorldCommitFrozenOriginAndRevalidateLiveTargets()
            throws Exception {
        String session = read(Path.of(
                "src/main/java/local/lito381/server/ClientSession.java"));
        String battle = between(session, "private void useCenteredAreaAttackSpell(",
                "private void useCenteredFreezeAttackSpell(");
        for (String anchor : new String[] {
                "long originRoomId = currentRoomId;",
                "int originX = positionX;", "int originY = positionY;",
                "int originHeading = playerHeading & 7;",
                "currentRoomId != originRoomId",
                "positionX != originX || positionY != originY",
                "target.currentRoomId != originRoomId",
                "hub.canAttack(originRoomId, this, target)",
                "originX, originY, originHeading, sequence",
                "spell.effectGfx(), 0, wireTargets)"
        }) assertTrue(battle.contains(anchor), () -> "missing BATTLE lease " + anchor);
        assertCommitCleanupPublishOrder(battle,
                "cancelMeditation(\"spell-\" + spell.skillId())",
                "cancelAbsoluteBarrier(\"spell-\" + spell.skillId())",
                "revealInvisibility(\"spell-\" + spell.skillId())");
        assertFalse(battle.contains("commitSpellReady("));

        String world = between(session,
                "private void useClassicWorldCenteredAreaAttackSpell(",
                "private void useClassicWorldAreaAttackSpell(");
        for (String anchor : new String[] {
                "int originHeading = playerHeading & 7;",
                "classicWorldMembers(originMapId)",
                "classicWorldCompanionSnapshots()",
                "classicWorldMonstersInScreen(",
                "classicCastleTrebuchetsInScreen(",
                "classicCastleTowersInScreen(",
                "classicCastleDoorMagicTargets(",
                "!BattleSpellCatalog.isInNativeScreen(",
                "!map.attackPosition(originX, originY,",
                "originHeading, sequence, spell.effectGfx(), 0,"
        }) assertTrue(world.contains(anchor), () -> "missing WORLD lease " + anchor);
        assertTrue(count(world, "map.attackPosition(originX, originY,") >= 12,
                "WORLD path must preflight and revalidate LOS for every actor family");
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
                && publish > invisibility);
    }

    private static int count(String text, String needle) {
        int result = 0;
        for (int at = 0; (at = text.indexOf(needle, at)) >= 0;
                at += needle.length()) result++;
        return result;
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
        ByteBuffer image = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN);
        int pe = image.getInt(0x3C);
        int sections = Short.toUnsignedInt(image.getShort(pe + 6));
        int optionalSize = Short.toUnsignedInt(image.getShort(pe + 20));
        int optional = pe + 24;
        long imageBase = Integer.toUnsignedLong(image.getInt(optional + 28));
        long rva = va - imageBase;
        int table = optional + optionalSize;
        for (int index = 0; index < sections; index++) {
            int section = table + index * 40;
            long virtualSize = Integer.toUnsignedLong(image.getInt(section + 8));
            long sectionRva = Integer.toUnsignedLong(image.getInt(section + 12));
            long rawSize = Integer.toUnsignedLong(image.getInt(section + 16));
            long rawOffset = Integer.toUnsignedLong(image.getInt(section + 20));
            if (rva >= sectionRva && rva + length <= sectionRva
                    + Math.max(virtualSize, rawSize)) {
                int offset = Math.toIntExact(rawOffset + rva - sectionRva);
                return java.util.Arrays.copyOfRange(bytes, offset, offset + length);
            }
        }
        throw new IllegalArgumentException("unmapped PE VA " + Long.toHexString(va));
    }

    private static int readPeWordAtVa(Path path, long va) throws Exception {
        return Short.toUnsignedInt(ByteBuffer.wrap(peBytesAtVa(path, va, 2))
                .order(ByteOrder.LITTLE_ENDIAN).getShort());
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
