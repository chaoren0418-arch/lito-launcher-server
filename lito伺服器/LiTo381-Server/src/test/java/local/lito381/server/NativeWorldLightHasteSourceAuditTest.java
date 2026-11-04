package local.lito381.server;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/** Locks the ordinary-WORLD dispatch and native actor-family speed boundary. */
final class NativeWorldLightHasteSourceAuditTest {
    @BeforeAll
    static void assumeLocalResourcesPresent() {
        LocalResources.assumeLineageSources();
        LocalResources.assumeIdaClient();
    }

    private static final Path RETAINED = Path.of("../1-伺服器");
    private static final Path SESSION = Path.of(
            "src/main/java/local/lito381/server/ClientSession.java");
    private static final Path MONSTERS = Path.of(
            "src/main/java/local/lito381/server/ClassicWorldMonsterPopulation.java");
    private static final Path COMPANION = Path.of(
            "src/main/java/local/lito381/server/BattleCompanion.java");

    @Test
    void worldDispatcherReachesLightHasteAndGreaterHasteBeforeUnsupportedGate()
            throws Exception {
        String source = Files.readString(SESSION, StandardCharsets.UTF_8);
        int worldStart = source.indexOf("if (phase == Phase.WORLD)");
        int worldEnd = source.indexOf("long spellRequestNow", worldStart);
        String world = source.substring(worldStart, worldEnd);
        int light = world.indexOf(
                "BattleSpellCatalog.lightSpell(selector.skillId())");
        int haste = world.indexOf(
                "BattleSpellCatalog.hasteSpell(selector.skillId())");
        int greater = world.indexOf(
                "BattleSpellCatalog.selfHasteSpell(selector.skillId())");
        int unsupported = world.indexOf(
                "rejected unsupported classic-world skill=");
        assertTrue(light >= 0 && haste > light && greater > haste
                && unsupported > greater);
        assertTrue(world.contains("useLightSpell(payload, worldLight)"));
        assertTrue(world.contains("useHasteSpell(payload, worldHaste)"));
        assertTrue(world.contains(
                "useSelfHasteSpell(payload, worldSelfHaste)"));
    }

    @Test
    void hasteResolvesEveryWorldCharacterFamilyAndOwnsServerIntervals()
            throws Exception {
        String session = Files.readString(SESSION, StandardCharsets.UTF_8);
        int start = session.indexOf("private void useHasteSpell(");
        int end = session.indexOf("private void useSelfHasteSpell(", start);
        String method = session.substring(start, end);
        assertTrue(method.contains("classicWorldMembers(originMapId)"));
        assertTrue(method.contains("classicWorldCompanionSnapshots()"));
        assertTrue(method.contains("hub.classicWorldMonster(originMapId"));
        assertTrue(method.contains("controlledCompanion.activateHaste("));
        assertTrue(method.contains("hub.hasteClassicWorldMonster("));
        assertTrue(method.contains("classicWorldInstanceId != originInstanceId"));
        assertTrue(method.contains("S_SPEED Haste target="));
        assertTrue(method.contains("S_MESSAGE_CODE Haste-applied="));
        assertTrue(method.contains("cancelAbsoluteBarrier(\"spell-\""));
        assertTrue(method.contains("commitSpellReady(spell.skillId()"));

        String database = Files.readString(
                RETAINED.resolve("db/380_db.sql"), StandardCharsets.UTF_8);
        assertTrue(database.contains(
                "INSERT INTO `skills` VALUES (43, 0, '加速術', 6, 2, 40, 0, 0, 0, 10, 1200, 'buff', 3, 0, 0, 0, 0, 0, 8, 2, 0, -1, 0, 0, 4, '$1463', 19, 755, 0, 184, 185, 280);"));
        assertTrue(database.contains(
                "INSERT INTO `skills_item` VALUES (43, '加速術', NULL, NULL);"));
        String secondary = Files.readString(RETAINED.resolve(
                "其他src參考用/0507備份/yiwei380/db/380yiwei_db.sql"),
                StandardCharsets.UTF_8);
        assertTrue(secondary.contains(
                "INSERT INTO `skills` VALUES (43, '加速術', 6, 2, 40, 0, 0, 0, 0, 1200, 'buff', 3, 0, 0, 0, 0, 0, 8, 2, 0, -1, 0, 0, 4, '$1463', 19, 755, 0, 184, 185, 280);"));
        assertTrue(secondary.contains(
                "INSERT INTO `skills_item` VALUES (43, '加速術', NULL, NULL);"));

        String retainedUse = Files.readString(RETAINED.resolve(
                "src/com/lineage/server/model/skill/L1SkillUse.java"),
                StandardCharsets.UTF_8);
        assertTrue(retainedUse.contains("case HASTE:// 加速術"));
        assertTrue(retainedUse.contains("checkEquipped(20013)"));
        assertTrue(retainedUse.contains("checkEquipped(20008)"));
        assertTrue(retainedUse.contains("breakInterruptedCastEffects();"));
        assertTrue(retainedUse.contains("breakInvisibilityOnCast();"));

        String assembly = Files.readString(Path.of(
                "../LiTo381-開發/ida-client-oep-v2/full-disassembly.asm"),
                StandardCharsets.UTF_8);
        String dispatch = between(assembly, "loc_4CB71D:", "loc_4CB741:");
        assertTrue(dispatch.contains("cases 7,8,22,36,42,43"));
        assertTrue(dispatch.contains("call    sub_4C9880"));
        String producer = between(assembly,
                "sub_4C9880 proc near", "sub_4C9880 endp");
        assertTrue(producer.contains("push    14h"));
        assertTrue(producer.contains("push    offset aCccd"));

        BattleSpellCatalog.HasteSpell spell = BattleSpellCatalog.hasteSpell(
                BattleSpellCatalog.HASTE);
        assertTrue(spell.happenMessage() == 184);
        assertTrue(spell.stopMessage() == 185);
        assertTrue(sourceContainsStopRoute(session));

        String monsters = Files.readString(MONSTERS, StandardCharsets.UTF_8);
        assertTrue(monsters.contains("synchronized boolean haste("));
        assertTrue(monsters.contains("actor.slowExpiresAtNanos = 0;"));
        assertTrue(monsters.contains(
                "hasteActive(mapId, objectId, nowNanos)"));
        assertTrue(monsters.contains("synchronized List<Snapshot> expireHastes("));

        String companion = Files.readString(COMPANION, StandardCharsets.UTF_8);
        assertTrue(companion.contains("synchronized boolean activateHaste("));
        assertTrue(companion.contains("effectiveMoveIntervalMillis()"));
        assertTrue(companion.contains("effectiveAttackIntervalMillis()"));
        assertTrue(companion.contains("profile.moveSpeed() != BattleTimedStatus.HASTE"));
    }

    @Test
    void lightPublicationAndExpiryAreWorldAware() throws Exception {
        String source = Files.readString(SESSION, StandardCharsets.UTF_8);
        int start = source.indexOf("private void useLightSpell(");
        int end = source.indexOf("private void useShieldSpell(", start);
        String method = source.substring(start, end);
        assertTrue(method.contains("admitClassicWorldSpellFamily("));
        assertTrue(method.contains("classicWorldMembers(classicWorldMapId)"));
        assertTrue(method.contains("BattleSpellCatalog.isInNativeScreen("));
        assertTrue(source.contains(
                "timedStatus.expireLight(nowNanos) && (inBattle || inClassicWorld)"));

        int hasteStart = source.indexOf("private void useSelfHasteSpell(");
        int hasteEnd = source.indexOf(
                "private void useCombatBuffSpell(", hasteStart);
        String selfHaste = source.substring(hasteStart, hasteEnd);
        assertTrue(selfHaste.contains("cancelAbsoluteBarrier(\"spell-\""));
        assertTrue(selfHaste.contains("spellReadyNanos.put(spell.skillId()"));
        assertTrue(selfHaste.contains("revealInvisibility(\"spell-\""));
        assertTrue(!selfHaste.contains("commitSpellReady("));
        int mp = selfHaste.indexOf("playerMp -= mpCost");
        int reuse = selfHaste.indexOf("spellReadyNanos.put(spell.skillId()", mp);
        int lease = selfHaste.indexOf("timedStatus.activateHaste(", reuse);
        int meditation = selfHaste.indexOf("cancelMeditation(", lease);
        int barrier = selfHaste.indexOf("cancelAbsoluteBarrier(", meditation);
        int reveal = selfHaste.indexOf("revealInvisibility(", barrier);
        assertTrue(mp >= 0 && reuse > mp && lease > reuse
                && meditation > lease && barrier > meditation && reveal > barrier);
        int commitStart = source.indexOf("private void commitSpellReady(");
        int commitEnd = source.indexOf(
                "private synchronized void activateInvisibilityState(", commitStart);
        String commit = source.substring(commitStart, commitEnd);
        assertTrue(commit.contains("revealInvisibility(\"spell-\" + skillId)"));
    }

    private static String between(String source, String start, String end) {
        int begin = source.indexOf(start);
        int finish = source.indexOf(end, begin);
        assertTrue(begin >= 0, start);
        assertTrue(finish > begin, end);
        return source.substring(begin, finish);
    }

    private static boolean sourceContainsStopRoute(String source) {
        int start = source.indexOf("private void sendHasteStopMessage(");
        int end = source.indexOf("private void broadcastBraveSpeed(", start);
        String method = source.substring(start, end);
        return method.contains("BattleSpellCatalog.hasteSpell(skillId)")
                && method.contains("ordinary.stopMessage()")
                && method.contains("S_MESSAGE_CODE Haste-stopped=");
    }
}
