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

/** Locks selected skill71 producer, sources and atomic BATTLE/WORLD routes. */
final class NativeWorldDecayPotionSkill71SourceAuditTest {
    @BeforeAll
    static void assumeLocalResourcesPresent() {
        LocalResources.assumeLineageSources();
        LocalResources.assumeLitoBin();
        LocalResources.assumeIdaClient(LocalResources.IDA_DISASSEMBLY_ABSOLUTE);
        LocalResources.assumeExisting("LiTo.unpacked", LocalResources.LITO_UNPACKED);
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
    void selectedAndSecondaryRowsAgreeWithNativeFiveHundredMillisecondClock()
            throws Exception {
        String selected = read(RETAINED.resolve("db/380_db.sql"));
        String secondary = read(RETAINED.resolve(
                "其他src參考用/0507備份/yiwei380/db/380yiwei_db.sql"));
        assertEquals("INSERT INTO `skills` VALUES (71, 0, '藥水霜化術', 9, 6,"
                + " 80, 0, 40318, 2, 500, 16, 'buff', 3, 0, 0, 0, 0, 8, 0,"
                + " 1, 0, 3, 0, 0, 64, '$1990', 19, 2232, 0, 0, 0, 280);",
                skillRow(selected, 71));
        assertEquals("INSERT INTO `skills` VALUES (71, '藥水霜化術', 9, 6, 80,"
                + " 0, 40318, 2, 500, 16, 'buff', 3, 0, 0, 0, 0, 8, 0, 1,"
                + " 0, 3, 0, 0, 64, '$1990', 19, 2232, 0, 0, 0, 280);",
                skillRow(secondary, 71));
        String item = "INSERT INTO `skills_item` VALUES (71, '藥水霜化術', NULL, NULL);";
        String boss = "INSERT INTO `skills_負面boss免疫` VALUES (71, '藥水腐壞術', 0);";
        for (String source : new String[] {selected, secondary}) {
            assertEquals(1, count(source, item));
            assertEquals(1, count(source, boss));
            assertFalse(source.contains(
                    "INSERT INTO `skills_buff_ability` VALUES (71,"));
            assertFalse(source.contains(
                    "INSERT INTO `skills_負面技能控制` VALUES (71,"));
        }

        assertEquals("CE6EE68ADA8FD37E6C096B1A27E46CB6888F1179F85DEFB5007AC60287360292",
                sha256(SELECTED_CLIENT));
        String assembly = read(DISASSEMBLY).replace("\r\n", "\n");
        String dispatch = between(assembly, "loc_4CB938:", "loc_4CB952:");
        for (String anchor : new String[] {
                "jumptable 004CB66E cases 10,19,27,39,46,55,63,70,102",
                "mov     edx, 3", "mov     [edi+68h], esi",
                "call    sub_4CA6E0"
        }) assertTrue(dispatch.contains(anchor), () -> "missing dispatch " + anchor);
        String producer = between(assembly,
                "sub_4CA6E0 proc near", "sub_4CA6E0 endp");
        for (String anchor : new String[] {
                "mov     al, byte_635A44[ebx]", "mov     edx, [edi+2Ch]",
                "mov     ecx, [edi+30h]", "push    14h",
                "push    offset aCccdhh"
        }) assertTrue(producer.contains(anchor), () -> "missing producer " + anchor);
        assertEquals(1, countBytes(Files.readAllBytes(UNPACKED),
                "cccdhh\0".getBytes(StandardCharsets.US_ASCII)));
        assertEquals(1, Byte.toUnsignedInt(
                peBytesAtVa(UNPACKED, 0x00635A44L + 70L, 1)[0]));
        assertEquals(500, readPeWordAtVa(
                UNPACKED, 0x00635774L + 70L * 2L));
    }

    @Test
    void catalogAndRetainedLifecyclePinTheNonRefreshingPotionBlock() throws Exception {
        BattleSpellCatalog.DecayPotionSpell spell =
                BattleSpellCatalog.decayPotionSpell(BattleSpellCatalog.DECAY_POTION);
        assertEquals(71, spell.skillId());
        assertEquals(8, spell.row());
        assertEquals(6, spell.column());
        assertEquals(80, spell.mpCost());
        assertEquals(500, spell.reuseDelayMillis());
        assertEquals(500, spell.clientCastClockMillis());
        assertEquals(3, spell.maximumRange());
        assertEquals(16, spell.durationSeconds());
        assertEquals(8, spell.probabilityDice());
        assertEquals(0, spell.bossModifier());
        assertEquals(19, spell.actionId());
        assertEquals(2232, spell.effectGfx());
        assertEquals(40318L, spell.itemConsumeId());
        assertEquals(2, spell.itemConsumeCount());

        String use = read(RETAINED.resolve(
                "src/com/lineage/server/model/skill/L1SkillUse.java"));
        String probability = between(use,
                "case L1Skills.TYPE_CURSE:", "case L1Skills.TYPE_HEAL:");
        assertTrue(probability.contains("calcProbabilityMagic(this._skillId)"));
        assertTrue(probability.contains("cha.removeSkillEffect(ERASE_MAGIC)"));
        assertTrue(probability.contains("this.isUseCounterMagic(cha)"));
        String counterExceptions = between(use,
                "private static final int[] EXCEPT_COUNTER_MAGIC", "};");
        assertFalse(counterExceptions.matches("(?s).*\\b71\\b.*"));
        String mode = read(RETAINED.resolve(
                "src/com/lineage/server/model/skill/skillmode/DECAY_POTION.java"));
        assertOrdered(mode, "if (cha.hasSkillEffect(L1SkillId.DECAY_POTION))",
                "cha.set_decay_potion(true)",
                "cha.setSkillEffect(L1SkillId.DECAY_POTION, integer * 1000)",
                "cha.set_decay_potion(false)");
        String buff = read(RETAINED.resolve(
                "src/com/lineage/server/model/skill/L1BuffUtil.java"));
        assertTrue(buff.contains("if (pc.is_decay_potion())"));
        assertTrue(buff.contains("new S_ServerMessage(698)"));
    }

    @Test
    void battleAndWorldRevalidateCasterTargetAndEndpointsBeforeCommit()
            throws Exception {
        String session = read(Path.of(
                "src/main/java/local/lito381/server/ClientSession.java"));
        String helper = between(session,
                "private boolean decayPotionCasterStillAdmitted(",
                "private void useNaturesBlessingSpell(");
        for (String anchor : new String[] {
                "timedStatus.actionLocked(now)", "timedStatus.silenceActive(now)",
                "currentWeight240(now)", "learnedSkills.contains(spell.skillId())",
                "active != null && !active.skillUsable()", "supportsTimedAction("
        }) assertTrue(helper.contains(anchor), () -> "missing caster gate " + anchor);

        String world = between(session,
                "private void useClassicWorldDecayPotionSpell(",
                "private void useDecayPotionSpell(");
        for (String anchor : new String[] {
                "sameClassicWorldMap(candidate)", "map.isNormalZone(originX, originY)",
                "map.attackPosition(originX, originY,", "absoluteBarrierActive(castNow)",
                "playerTarget.positionX == targetX", "lockedMonster.x() == targetX",
                "classicWorldCkewAttackAllowed(npcId, castNow)",
                "decayPotionCasterStillAdmitted(", "Phase.WORLD, spell, castNow",
                "consumeEraseMagicAfterProbability(", "consumeCounterMagic(castNow)",
                "activateDecayPotion(", "decayPotionClassicWorldMonster("
        }) assertTrue(world.contains(anchor), () -> "missing WORLD route " + anchor);
        assertCommitOrder(world,
                "cancelMeditation(\"classic-world-spell-\" + spell.skillId())",
                "cancelAbsoluteBarrier(\"classic-world-spell-\" + spell.skillId())",
                "revealInvisibility(\"classic-world-spell-\" + spell.skillId())");

        String battle = between(session, "private void useDecayPotionSpell(",
                "/**\n     * Exact retained CREATE_MAGICAL_WEAPON flow.");
        for (String anchor : new String[] {
                "hub.battleMapId(originRoomId, target) == originMapId",
                "BattleSpellCatalog.isInNativeScreen(",
                "BattleSpellCatalog.hasNativeLineOfSight(",
                "target.positionX == targetX", "lockedQuest.x() == targetX",
                "lockedTime.x() == targetX", "absoluteBarrierActive(castNow)",
                "decayPotionCasterStillAdmitted(", "Phase.BATTLE, spell, castNow",
                "config.decayPotionFixtureDieRoll()",
                "config.decayPotionFixturePercentileRoll()",
                "consumeEraseMagicAfterProbability(", "consumeCounterMagic(castNow)",
                "activateDecayPotion(", "decayPotionQuestMonster(",
                "decayPotionTimeAttackActor("
        }) assertTrue(battle.contains(anchor), () -> "missing BATTLE route " + anchor);
        assertCommitOrder(battle,
                "cancelMeditation(\"spell-\" + spell.skillId())",
                "cancelAbsoluteBarrier(\"spell-\" + spell.skillId())",
                "revealInvisibility(\"spell-\" + spell.skillId())");
    }

    private static void assertCommitOrder(String method, String meditation,
                                          String barrier, String invisibility) {
        assertFalse(method.contains("commitSpellReady("));
        assertOrdered(method, "decayPotionCasterStillAdmitted(",
                "inventory = nextInventory", "playerMp -= mpCost",
                "spellReadyNanos.put(spell.skillId()", meditation, barrier,
                invisibility, "Protocol.objectAction(",
                "consumeEraseMagicAfterProbability(",
                "consumeCounterMagic(castNow)", "Protocol.effectObject(",
                "Protocol.manaPoint(");
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

    private static int countBytes(byte[] source, byte[] fragment) {
        int result = 0;
        for (int at = 0; at + fragment.length <= source.length; at++) {
            int index = 0;
            while (index < fragment.length
                    && source[at + index] == fragment[index]) index++;
            if (index == fragment.length) result++;
        }
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
