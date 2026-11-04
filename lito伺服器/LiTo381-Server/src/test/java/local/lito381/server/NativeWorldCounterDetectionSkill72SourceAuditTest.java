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

/** Locks selected skill72 producer, sources and atomic BATTLE/WORLD routes. */
final class NativeWorldCounterDetectionSkill72SourceAuditTest {
    @BeforeAll
    static void assumeLocalResourcesPresent() {
        LocalResources.assumeLineageSources();
        LocalResources.assumeLitoBin();
        LocalResources.assumeIdaClient();
    }

    private static final Path RETAINED = Path.of("..", "1-伺服器")
            .toAbsolutePath().normalize();
    private static final Path SELECTED_CLIENT = Path.of(
            "D:/380/天堂(Lineage 3.81C) (1)/LiTo.bin");
    private static final Path UNPACKED = Path.of(
            "D:/lineage開發/LiTo381-開發/runtime/LiTo.unpacked-oep-current.exe");
    private static final Path DISASSEMBLY = Path.of(
            "D:/lineage開發/LiTo381-開發/ida-client-oep-v2/full-disassembly.asm");

    @Test
    void nativeSelectorOnlyProducerAndFiveHundredMillisecondClockStayExact()
            throws Exception {
        assertEquals("CE6EE68ADA8FD37E6C096B1A27E46CB6888F1179F85DEFB5007AC60287360292",
                sha256(SELECTED_CLIENT));
        String assembly = read(DISASSEMBLY).replace("\r\n", "\n");
        String producer = between(assembly, "loc_4CBBD4:", "loc_4CBBED:");
        for (String anchor : new String[] {
                "jumptable 004CB66E cases 1,2,12,13,30,31,48,51,53,71,77,81",
                "and     ecx, 7", "sar     esi, 3", "push    14h",
                "push    offset off_5028CC", "call    sub_4C6AE0"
        }) assertTrue(producer.contains(anchor), () -> "missing producer " + anchor);
        assertEquals("ccc\0", new String(
                peBytesAtVa(UNPACKED, 0x005028CCL, 4),
                StandardCharsets.US_ASCII));
        assertEquals(0, Byte.toUnsignedInt(
                peBytesAtVa(UNPACKED, 0x00635A44L + 71L, 1)[0]));
        assertEquals(500, readPeWordAtVa(
                UNPACKED, 0x00635774L + 71L * 2L));
    }

    @Test
    void selectedRowsCatalogAndRetainedExceptionsStayExact() throws Exception {
        String selected = read(RETAINED.resolve("db/380_db.sql"));
        String secondary = read(RETAINED.resolve(
                "其他src參考用/0507備份/yiwei380/db/380yiwei_db.sql"));
        assertEquals("INSERT INTO `skills` VALUES (72, 0, '強力無所遁形術', 9, 7,"
                + " 30, 0, 0, 0, 500, 0, 'none', 3, 24, 10, 4, 0, 0, 0,"
                + " 64, 0, 0, -1, 0, 128, '$1991', 19, 3934, 0, 0, 0, 0);",
                skillRow(selected, 72));
        assertEquals("INSERT INTO `skills` VALUES (72, '強力無所遁形術', 9, 7, 30,"
                + " 0, 0, 0, 0, 0, 'none', 3, 24, 10, 4, 0, 0, 0, 64, 0,"
                + " 0, -1, 0, 128, '$1991', 19, 3934, 0, 0, 0, 0);",
                skillRow(secondary, 72));
        String item = "INSERT INTO `skills_item` VALUES (72, '強力無所遁形術', NULL, NULL);";
        for (String source : new String[] {selected, secondary}) {
            assertEquals(1, count(source, item));
            assertFalse(source.contains(
                    "INSERT INTO `skills_負面技能控制` VALUES (72,"));
            assertFalse(source.contains(
                    "INSERT INTO `skills_負面boss免疫` VALUES (72,"));
        }

        BattleSpellCatalog.CounterDetectionSpell spell =
                BattleSpellCatalog.counterDetectionSpell(
                        BattleSpellCatalog.COUNTER_DETECTION);
        assertEquals(72, spell.skillId());
        assertEquals(8, spell.row());
        assertEquals(7, spell.column());
        assertEquals(19, spell.actionId());
        assertEquals(3934, spell.effectGfx());
        assertEquals(30, spell.mpCost());
        assertEquals(500, spell.reuseDelayMillis());
        assertEquals(500, spell.clientCastClockMillis());
        assertEquals(-1, spell.nativeArea());
        assertEquals(24, spell.baseDamage());
        assertEquals(4, spell.diceCount());
        assertEquals(10, spell.dieSides());
        assertEquals(0, spell.attribute());

        String parser = read(RETAINED.resolve(
                "src/com/lineage/server/clientpackets/C_UseSkill.java"));
        int case72 = parser.indexOf("case 72:// 強力無所遁形術 1650");
        String optionalDword = parser.substring(case72,
                parser.indexOf("break;", case72));
        assertTrue(optionalDword.contains("targetId = readD();"));
        assertTrue(optionalDword.contains("catch (final Exception e)"));

        String use = read(RETAINED.resolve(
                "src/com/lineage/server/model/skill/L1SkillUse.java"));
        String counterExceptions = between(use,
                "private static final int[] EXCEPT_COUNTER_MAGIC", "};");
        assertTrue(counterExceptions.matches("(?s).*\\b72\\b.*"));
        for (String anchor : new String[] {
                "case COUNTER_DETECTION:",
                "case L1NpcInstance.HIDDEN_STATUS_SINK:",
                "this._dmg = magic.calcMagicDamage(this._skillId);",
                "npc.appearOnGround(this._player);", "this._dmg = 0;",
                "World.get().getVisiblePlayer(pc)", "tgt.delInvis();",
                "WorldTrap.get().onDetection(pc);"
        }) assertTrue(use.contains(anchor), () -> "missing retained semantic " + anchor);
    }

    @Test
    void battleAndWorldRevalidateCasterTargetsAndAudienceBeforeCommit()
            throws Exception {
        String session = read(Path.of(
                "src/main/java/local/lito381/server/ClientSession.java"));
        String helper = between(session,
                "private boolean counterDetectionCasterStillAdmitted(",
                "private void useNaturesBlessingSpell(");
        for (String anchor : new String[] {
                "timedStatus.actionLocked(now)", "timedStatus.silenceActive(now)",
                "currentWeight240(now)", "learnedSkills.contains(spell.skillId())",
                "active != null && !active.skillUsable()", "supportsTimedAction("
        }) assertTrue(helper.contains(anchor), () -> "missing caster gate " + anchor);

        String world = between(session,
                "private void useClassicWorldCounterDetectionSpell(",
                "private void useCounterDetectionSpell(");
        for (String anchor : new String[] {
                "classicWorldInstanceId != originInstanceId",
                "positionX != originX || positionY != originY",
                "counterDetectionCasterStillAdmitted(", "Phase.WORLD, spell, castNow",
                "target.positionX == cell.x()", "target.invisible",
                "map.isNormalZone(originX, originY)",
                "hub.classicWorldMonster(", "target.x() != identity.x()",
                "map.attackPosition(originX, originY,",
                "hub.classicWorldMembers(", "Protocol.attackMany(",
                "takeClassicWorldPlayerDamage(", "target.cancelInvisibilityState()"
        }) assertTrue(world.contains(anchor), () -> "missing WORLD route " + anchor);
        assertCommitOrder(world,
                "cancelMeditation(\"classic-world-spell-\" + spell.skillId())",
                "cancelAbsoluteBarrier(\"classic-world-spell-\" + spell.skillId())",
                "revealInvisibility(\"classic-world-spell-\" + spell.skillId())");

        String battle = between(session,
                "private void useCounterDetectionSpell(",
                "private void useWeaponBreakSpell(");
        for (String anchor : new String[] {
                "currentRoomId != originRoomId",
                "hub.battleMapId(originRoomId, this) != originMapId",
                "positionX != originX || positionY != originY",
                "counterDetectionCasterStillAdmitted(", "Phase.BATTLE, spell, castNow",
                "target.currentRoomId == originRoomId",
                "target.positionX == cell.x()", "boolean hidden = target.invisible",
                "boolean lineOfSight = hidden",
                "|| BattleSpellCatalog.hasNativeLineOfSight(",
                "(!hidden", "&& !hub.canAttack(originRoomId, this, target))",
                "hub.guardianTower(", "hub.questMonster(", "hub.timeAttackActor(",
                "currentBot.equals(botIdentity)", "hub.roomMembers(originRoomId)",
                "Protocol.attackMany(", "takePlayerDamage(",
                "target.cancelInvisibilityState()"
        }) assertTrue(battle.contains(anchor), () -> "missing BATTLE route " + anchor);
        assertCommitOrder(battle,
                "cancelMeditation(\"spell-\" + spell.skillId())",
                "cancelAbsoluteBarrier(\"spell-\" + spell.skillId())",
                "revealInvisibility(\"spell-\" + spell.skillId())");
    }

    private static void assertCommitOrder(String method, String meditation,
                                          String barrier, String invisibility) {
        assertFalse(method.contains("commitSpellReady("));
        assertFalse(method.contains("consumeCounterMagic("));
        assertOrdered(method, "counterDetectionCasterStillAdmitted(",
                "playerMp -= mpCost", "spellReadyNanos.put(spell.skillId()",
                meditation, barrier, invisibility, "Protocol.objectAction(",
                "Protocol.attackMany(", "Protocol.manaPoint(");
    }

    private static String skillRow(String sql, int skillId) {
        String prefix = "INSERT INTO `skills` VALUES (" + skillId + ",";
        return sql.lines().filter(line -> line.startsWith(prefix))
                .findFirst().orElseThrow();
    }

    private static int count(String text, String fragment) {
        int result = 0;
        for (int at = 0; (at = text.indexOf(fragment, at)) >= 0;
             at += fragment.length()) result++;
        return result;
    }

    private static void assertOrdered(String source, String... anchors) {
        int cursor = 0;
        for (String anchor : anchors) {
            int at = source.indexOf(anchor, cursor);
            assertTrue(at >= 0, () -> "missing ordered anchor " + anchor);
            cursor = at + anchor.length();
        }
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
