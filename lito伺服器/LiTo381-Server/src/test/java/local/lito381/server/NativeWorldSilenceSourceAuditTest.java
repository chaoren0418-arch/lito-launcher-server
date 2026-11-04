package local.lito381.server;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/** Locks native skill64 and its ordinary-WORLD one-attempt NPC semantics. */
final class NativeWorldSilenceSourceAuditTest {
    @BeforeAll
    static void assumeLocalResourcesPresent() {
        LocalResources.assumeLineageSources(
                Path.of("../1-伺服器/db/380_db.sql"),
                Path.of("../1-伺服器/src/com/lineage/server/model/skill/L1SkillUse.java"),
                Path.of("../1-伺服器/其他src參考用/0507備份/yiwei380/db/380yiwei_db.sql"));
        LocalResources.assumeIdaClient(
                Path.of("../LiTo381-開發/ida-client-oep-v2/full-disassembly.asm"));
        LocalResources.assumeFiles(
                Path.of("../LiTo381-開發/runtime/LiTo.unpacked-oep-current.exe"));
    }

    private static final Path RETAINED = Path.of(
            "../1-伺服器/src/com/lineage/server");
    private static final Path SELECTED_DATABASE = Path.of(
            "../1-伺服器/db/380_db.sql");
    private static final Path SECONDARY_DATABASE = Path.of(
            "../1-伺服器/其他src參考用/0507備份/yiwei380/db/380yiwei_db.sql");
    private static final Path UNPACKED = Path.of(
            "../LiTo381-開發/runtime/LiTo.unpacked-oep-current.exe");
    private static final Path ASSEMBLY = Path.of(
            "../LiTo381-開發/ida-client-oep-v2/full-disassembly.asm");

    @Test
    void selectedSecondaryPeAndExactRequestStayDistinct() throws Exception {
        String selected = LocalResources.readString(
                SELECTED_DATABASE, StandardCharsets.UTF_8);
        String secondary = LocalResources.readString(
                SECONDARY_DATABASE, StandardCharsets.UTF_8);
        assertEquals("INSERT INTO `skills` VALUES (64, 0, '魔法封印', 8, 7,"
                + " 50, 0, 40318, 1, 500, 16, 'buff', 3, 0, 0, 0, 0, 10,"
                + " 0, 1, 0, 3, 0, 0, 128, '$1989', 19, 2177, 0, 0, 0,"
                + " 280);", skillRow(selected, 64));
        assertEquals("INSERT INTO `skills` VALUES (64, '魔法封印', 8, 7,"
                + " 50, 0, 40318, 1, 0, 16, 'buff', 3, 0, 0, 0, 0, 10,"
                + " 0, 1, 0, 3, 0, 0, 128, '$1989', 19, 2177, 0, 0, 0,"
                + " 280);", skillRow(secondary, 64));
        for (String source : new String[] {selected, secondary}) {
            assertTrue(source.contains("INSERT INTO `skills_負面技能控制` VALUES"
                    + " (64, '魔法封印', 55, 45, 35, 0, 0, 0.35, 0, 0,"
                    + " 1, 0.05, 0, 0);"));
            assertTrue(source.contains("INSERT INTO `skills_負面boss免疫` VALUES"
                    + " (64, '魔法封印', 0);"));
            assertTrue(source.contains("INSERT INTO `skills_item` VALUES"
                    + " (64, '魔法封印', NULL, NULL);"));
        }
        assertArrayEquals("cccdhh\0".getBytes(StandardCharsets.US_ASCII),
                peBytes(UNPACKED, 0x00636028L, 7));
        assertEquals(1, Byte.toUnsignedInt(
                peBytes(UNPACKED, 0x00635A44L + 63L, 1)[0]));
        assertEquals(500, Short.toUnsignedInt(ByteBuffer.wrap(
                peBytes(UNPACKED, 0x00635774L + 63L * 2L, 2))
                .order(ByteOrder.LITTLE_ENDIAN).getShort()));
        byte[] request = new Protocol.Writer().c(Protocol.C_USE_SPELL)
                .c(7).c(7).d(0x6500_0696L).h(0x8062).h(0x7F73).bytes();
        assertArrayEquals(new byte[] {
                0x14, 0x07, 0x07, (byte) 0x96, 0x06, 0x00, 0x65,
                0x62, (byte) 0x80, 0x73, 0x7F
        }, request);
    }

    @Test
    void nativeProducerSelectedRowsBarrierAndNpcConsumptionStayExact()
            throws Exception {
        String database = LocalResources.readString(
                Path.of("../1-伺服器/db/380_db.sql"),
                StandardCharsets.UTF_8);
        assertTrue(database.contains(
                "INSERT INTO `skills` VALUES (64, 0, '魔法封印', 8, 7, 50, 0, 40318, 1, 500, 16, 'buff', 3, 0, 0, 0, 0, 10, 0, 1, 0, 3, 0, 0, 128, '$1989', 19, 2177, 0, 0, 0, 280);"));
        assertTrue(database.contains(
                "INSERT INTO `skills_負面技能控制` VALUES (64, '魔法封印', 55, 45, 35, 0, 0, 0.35, 0, 0, 1, 0.05, 0, 0);"));
        assertTrue(database.contains(
                "INSERT INTO `skills_負面boss免疫` VALUES (64, '魔法封印', 0);"));
        assertTrue(database.contains(
                "INSERT INTO `skills_item` VALUES (64, '魔法封印', NULL, NULL);"));

        String assembly = LocalResources.readString(Path.of(
                ASSEMBLY.toString()),
                StandardCharsets.UTF_8);
        int dispatch = assembly.indexOf("loc_4CB938:");
        String nativeCases = assembly.substring(dispatch, dispatch + 420);
        assertTrue(nativeCases.contains(
                "jumptable 004CB66E cases 10,19,27,39,46,55,63,70,102"));
        assertTrue(nativeCases.contains("mov     edx, 3"));
        assertTrue(nativeCases.contains("call    sub_4CA6E0"));
        String producer = assembly.substring(
                assembly.indexOf("sub_4CA6E0 proc near"),
                assembly.indexOf("sub_4CA6E0 endp"));
        assertTrue(producer.contains("push    14h"));
        assertTrue(producer.contains("offset aCccdhh"));

        String skillUse = LocalResources.readString(
                RETAINED.resolve("model/skill/L1SkillUse.java"),
                StandardCharsets.UTF_8);
        int barrier = skillUse.indexOf(
                "if (pc.hasSkillEffect(ABSOLUTE_BARRIER))");
        assertTrue(skillUse.substring(barrier, barrier + 1_100)
                .contains("case SILENCE:"));
        assertTrue(skillUse.contains("this._user.hasSkillEffect(SILENCE)"));
        assertTrue(skillUse.contains("this._user.removeSkillEffect(SILENCE)"));
    }

    @Test
    void worldDispatcherProbabilityStatusCancellationAndAiStayConnected()
            throws Exception {
        String session = LocalResources.readString(Path.of(
                "src/main/java/local/lito381/server/ClientSession.java"),
                StandardCharsets.UTF_8);
        int worldStart = session.indexOf("if (phase == Phase.WORLD) {");
        int dispatch = session.indexOf(
                "BattleSpellCatalog.SilenceSpell worldSilence", worldStart);
        int unsupported = session.indexOf(
                "rejected unsupported classic-world skill", dispatch);
        assertTrue(dispatch > worldStart && unsupported > dispatch);
        assertTrue(session.substring(dispatch, unsupported).contains(
                "useClassicWorldSilenceSpell(payload, worldSilence)"));

        int methodStart = session.indexOf(
                "private void useClassicWorldSilenceSpell(");
        int methodEnd = session.indexOf(
                "private void useSilenceSpell(", methodStart);
        String method = session.substring(methodStart, methodEnd);
        assertTrue(method.contains("admitClassicWorldSpellFamily("));
        assertTrue(method.contains(
                "BattleAbsoluteBarrierRules.blocksIncomingSpell("));
        assertTrue(method.contains("spell.probabilityForTarget("));
        assertTrue(method.contains("config.silenceFixturePercentileRoll()"));
        assertTrue(method.contains("classicWorldCkewAttackAllowed("));
        assertTrue(method.contains("consumeEraseMagicAfterProbability("));
        assertTrue(method.contains("consumeCounterMagic(castNow)"));
        assertTrue(method.contains("hub.silenceClassicWorldMonster("));
        assertTrue(method.contains("hub.provokeClassicWorldMonster("));
        assertTrue(method.contains("Protocol.effectObject("));
        assertTrue(method.contains(
                "Protocol.messageCode(spell.failureMessage())"));

        String timed = LocalResources.readString(Path.of(
                "src/main/java/local/lito381/server/BattleTimedStatus.java"),
                StandardCharsets.UTF_8);
        assertTrue(timed.contains(
                "boolean consumeSilenceForNpcSpell(long nowNanos)"));
        String population = LocalResources.readString(Path.of(
                "src/main/java/local/lito381/server/ClassicWorldMonsterPopulation.java"),
                StandardCharsets.UTF_8);
        assertTrue(population.contains("synchronized boolean silence("));
        assertTrue(population.contains(
                "activateSilence(nowNanos, durationSeconds)"));
        assertTrue(population.contains(
                "consumeSilenceForSpell(mapId, objectId, nowNanos)"));
        assertTrue(population.contains(
                "actor.damagePoisonStatus.consumeSilenceForNpcSpell(nowNanos)"));
        assertTrue(population.contains(
                "silence || timed.silence()"));
    }

    @Test
    void bothPhasesRevalidateEndpointsAndCommitBeforeStatusCleanup()
            throws Exception {
        String session = LocalResources.readString(Path.of(
                "src/main/java/local/lito381/server/ClientSession.java"),
                StandardCharsets.UTF_8);
        String world = between(session,
                "private void useClassicWorldSilenceSpell(",
                "private void useSilenceSpell(");
        String battle = between(session, "private void useSilenceSpell(",
                "private void useElementalFallDownSpell(");
        for (String method : new String[] {world, battle}) {
            assertTrue(method.contains("positionX == originX"));
            assertTrue(method.contains("positionY == originY"));
            assertTrue(method.contains("target.positionX == targetX")
                    || method.contains("playerTarget.positionX == targetX"));
            assertTrue(method.contains("BattleAbsoluteBarrierRules"));
            assertTrue(method.contains("BattleSpellCatalog.isInNativeScreen("));
            assertTrue(method.contains("silenceFixturePercentileRoll()"));
            assertFalse(method.contains("commitSpellReady("));
        }
        assertCommitOrder(world, "classic-world-spell-");
        assertCommitOrder(battle, "spell-");
    }

    private static void assertCommitOrder(String method, String prefix) {
        int inventory = method.indexOf("inventory = nextInventory");
        int mp = method.indexOf("playerMp -= mpCost", inventory);
        int reuse = method.indexOf(
                "spellReadyNanos.put(spell.skillId()", mp);
        int meditation = method.indexOf(
                "cancelMeditation(\"" + prefix + "\"", reuse);
        int barrier = method.indexOf(
                "cancelAbsoluteBarrier(\"" + prefix + "\"", meditation);
        int reveal = method.indexOf(
                "revealInvisibility(\"" + prefix + "\"", barrier);
        int publish = method.indexOf("Protocol.objectAction(", reveal);
        assertTrue(inventory >= 0 && mp > inventory && reuse > mp
                && meditation > reuse && barrier > meditation
                && reveal > barrier && publish > reveal,
                () -> "commit/cleanup/publish order changed: "
                        + inventory + "," + mp + "," + reuse + ","
                        + meditation + "," + barrier + "," + reveal + ","
                        + publish);
    }

    private static String skillRow(String source, int skillId) {
        String prefix = "INSERT INTO `skills` VALUES (" + skillId + ",";
        return source.lines().filter(line -> line.startsWith(prefix))
                .findFirst().orElseThrow();
    }

    private static String between(String source, String start, String end) {
        int from = source.indexOf(start);
        int to = source.indexOf(end, from + start.length());
        assertTrue(from >= 0 && to > from, start + " -> " + end);
        return source.substring(from, to);
    }

    private static byte[] peBytes(Path path, long va, int length)
            throws Exception {
        byte[] bytes = LocalResources.readAllBytes(path);
        ByteBuffer image = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN);
        int pe = image.getInt(0x3C);
        int sections = Short.toUnsignedInt(image.getShort(pe + 6));
        int optional = pe + 24;
        int optionalSize = Short.toUnsignedInt(image.getShort(pe + 20));
        long rva = va - Integer.toUnsignedLong(image.getInt(optional + 28));
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
                return java.util.Arrays.copyOfRange(
                        bytes, offset, offset + length);
            }
        }
        throw new IllegalArgumentException("unmapped PE VA "
                + Long.toHexString(va));
    }
}
