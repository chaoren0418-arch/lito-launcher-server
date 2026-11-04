package local.lito381.server;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/** Locks selected skill52 producer, database rule and retained cast order. */
final class NativeWorldHolyWalkSourceAuditTest {
    @BeforeAll
    static void assumeLocalResourcesPresent() {
        LocalResources.assumeLineageSources(
                Path.of("../1-伺服器/db/380_db.sql"),
                Path.of("../1-伺服器/其他src參考用/0507備份/yiwei380/db/380yiwei_db.sql"));
        LocalResources.assumeIdaClient(
                Path.of("../LiTo381-開發/ida-client-oep-v2/full-disassembly.asm"));
    }

    private static String source(Path path) throws Exception {
        return LocalResources.readString(path, StandardCharsets.UTF_8);
    }

    private static String between(String text, String start, String end) {
        int from = text.indexOf(start);
        int to = text.indexOf(end, from + start.length());
        assertTrue(from >= 0 && to > from,
                () -> "missing source boundary " + start + " -> " + end);
        return text.substring(from, to);
    }

    @Test
    void selectedDatabaseAndSelectorOnlyProducerRemainExact() throws Exception {
        String selected = source(Path.of("../1-伺服器/db/380_db.sql"));
        assertTrue(selected.contains(
                "INSERT INTO `skills` VALUES (52, 0, '神聖疾走', 7, 3, 15, 0, 0, 0, 10, 32, 'none', 0, 0, 0, 0, 0, 0, 8, 2, 0, 0, 0, 0, 8, '$1469', 19, 3936, 0, 0, 0, 0);"));
        assertTrue(selected.contains(
                "INSERT INTO `skills_item` VALUES (52, '神聖疾走', NULL, NULL);"));
        String secondary = source(Path.of(
                "../1-伺服器/其他src參考用/0507備份/yiwei380/db/380yiwei_db.sql"));
        assertTrue(secondary.contains(
                "INSERT INTO `skills` VALUES (52, '神聖疾走', 7, 3, 15, 0, 0, 0, 0, 32, 'none', 0, 0, 0, 0, 0, 0, 8, 2, 0, 0, 0, 0, 8, '$1469', 19, 3936, 0, 0, 0, 0);"));

        String assembly = source(Path.of(
                "../LiTo381-開發/ida-client-oep-v2/full-disassembly.asm"));
        String producer = between(assembly, "loc_4CBBD4:", "loc_4CBBED:");
        for (String anchor : new String[] {
                "jumptable 004CB66E cases 1,2,12,13,30,31,48,51,53,71,77,81,96-98,100,101,103-105,108-110,113,114,116,118,119,128,129,133,136,137,145,146,149,153-155,158,160,161,163,168,170",
                "and     ecx, 7", "sar     esi, 3", "push    14h",
                "push    offset off_5028CC", "call    sub_4C6AE0"
        }) assertTrue(producer.contains(anchor), () -> "missing producer " + anchor);
        assertTrue(!producer.contains("push    4") && !producer.contains("push    8"));

        BattleSpellCatalog.MovementBuffSpell spell =
                BattleSpellCatalog.movementBuffSpell(BattleSpellCatalog.HOLY_WALK);
        assertEquals(52, spell.skillId());
        assertEquals(6, spell.row());
        assertEquals(3, spell.column());
        assertEquals(15, spell.mpCost());
        assertEquals(10, spell.reuseDelayMillis());
        assertEquals(32, spell.durationSeconds());
        assertEquals(4, spell.braveMode());
        assertEquals(0.665D, spell.movementMultiplier());
    }

    @Test
    void sharedBattleWorldConsumerCommitsBeforeInterruptedStateCleanup()
            throws Exception {
        String session = source(Path.of(
                "src/main/java/local/lito381/server/ClientSession.java"));
        String worldDispatch = between(session,
                "BattleSpellCatalog.MovementBuffSpell worldMovementBuff",
                "BattleSpellCatalog.ShieldSpell worldArmorProtection");
        assertTrue(worldDispatch.contains(
                "useMovementBuffSpell(payload, worldMovementBuff)"));
        String method = between(session, "private void useMovementBuffSpell(",
                "private void usePhysicalProcBuffSpell(");
        for (String anchor : new String[] {
                "Protocol.selectorOnlySpellRequest(payload)",
                "spellReadyNanos.put(spell.skillId()",
                "timedStatus.activateBrave(", "cancelMeditation(",
                "cancelAbsoluteBarrier(", "revealInvisibility(",
                "Protocol.objectAction(", "broadcastBraveSpeed("
        }) assertTrue(method.contains(anchor), () -> "missing consumer " + anchor);
        int mp = method.indexOf("playerMp -= mpCost");
        int reuse = method.indexOf("spellReadyNanos.put(spell.skillId()", mp);
        int lease = method.indexOf("timedStatus.activateBrave(", reuse);
        int meditation = method.indexOf("cancelMeditation(", lease);
        int barrier = method.indexOf("cancelAbsoluteBarrier(", meditation);
        int invisible = method.indexOf("revealInvisibility(", barrier);
        int action = method.indexOf("Protocol.objectAction(", invisible);
        assertTrue(mp >= 0 && reuse > mp && lease > reuse
                && meditation > lease && barrier > meditation
                && invisible > barrier && action > invisible);
        assertEquals(-1, method.indexOf("commitSpellReady("));
    }
}
