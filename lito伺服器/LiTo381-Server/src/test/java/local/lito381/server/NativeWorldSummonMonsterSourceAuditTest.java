package local.lito381.server;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/** Locks selected skill51 producer, resources and BATTLE/WORLD cast order. */
final class NativeWorldSummonMonsterSourceAuditTest {
    @BeforeAll
    static void assumeLocalResourcesPresent() {
        LocalResources.assumeLineageSources();
        LocalResources.assumeIdaClient(LocalResources.IDA_DISASSEMBLY);
    }

    private static String source(Path path) throws Exception {
        return Files.readString(path, StandardCharsets.UTF_8);
    }

    private static String between(String text, String start, String end) {
        int from = text.indexOf(start);
        int to = text.indexOf(end, from + start.length());
        assertTrue(from >= 0 && to > from,
                () -> "missing source boundary " + start + " -> " + end);
        return text.substring(from, to);
    }

    @Test
    void selectedDatabaseProducerAndRingCallbackRemainExact() throws Exception {
        String selected = source(Path.of("../1-伺服器/db/380_db.sql"));
        assertTrue(selected.contains(
                "INSERT INTO `skills` VALUES (51, 0, '召喚術', 7, 2, 30, 30, 40318, 3, 1000, 0, 'none', 0, 0, 0, 0, 0, 0, 1, 128, -10, 0, 0, 0, 4, '$1468', 19, 2510, 0, 0, 0, 0);"));
        assertTrue(selected.contains(
                "INSERT INTO `skills_item` VALUES (51, '召喚術', NULL, NULL);"));

        String assembly = source(Path.of(
                "../LiTo381-開發/ida-client-oep-v2/full-disassembly.asm"));
        String direct = between(assembly, "loc_4CBB0C:", "loc_4CBB81:");
        for (String anchor : new String[] {
                "jumptable 004CB66E case 50", "call    sub_407A20",
                "sub     eax, 1Ch", "sar     eax, 2", "add     eax, 7",
                "cmp     eax, 7", "cmp     eax, 0Dh", "\"cccd\""
        }) assertTrue(direct.contains(anchor), () -> "missing direct " + anchor);
        String callback = between(assembly, "loc_40A08B:", "loc_40A11A:");
        for (String anchor : new String[] {
                "cmp     dword_63D604, 1", "sub     eax, 1Ch",
                "sar     esi, 2", "add     esi, 7", "and     eax, 800000FFh",
                "cmp     esi, eax", "jl      short loc_40A0F6", "\"cccd\"",
                "mov     ecx, 2E7h"
        }) assertTrue(callback.contains(anchor), () -> "missing callback " + anchor);

        BattleSpellCatalog.SummonSpell spell =
                BattleSpellCatalog.summonSpell(
                        BattleSpellCatalog.SUMMON_MONSTER);
        assertEquals(51, spell.skillId());
        assertEquals(6, spell.row());
        assertEquals(2, spell.column());
        assertEquals(30, spell.mpCost());
        assertEquals(30, spell.hpCost());
        assertEquals(1_000, spell.reuseDelayMillis());
        assertEquals(3, spell.itemConsumeCount());
        assertEquals(19, spell.actionId());
        assertEquals(2510, spell.effectGfx());
        assertTrue(spell.permitsControlSelection(47, 523));
        assertFalse(spell.permitsControlSelection(47, 12));
    }

    @Test
    void sharedBattleWorldConsumerCommitsBeforeCleanupAndMapResult()
            throws Exception {
        String session = source(Path.of(
                "src/main/java/local/lito381/server/ClientSession.java"));
        String worldDispatch = between(session,
                "BattleSpellCatalog.SummonSpell worldSummon",
                "long spellRequestNow");
        assertTrue(worldDispatch.contains("useSummonSpell(payload, worldSummon)"));
        String battleDispatch = between(session,
                "BattleSpellCatalog.SummonSpell summonSpell",
                "BattleSpellCatalog.ElementalSummonSpell elementalSummonSpell");
        assertTrue(battleDispatch.contains("useSummonSpell(payload, summonSpell)"));

        String method = between(session, "private void useSummonSpell(",
                "private void closeSummonList(");
        for (String anchor : new String[] {
                "Protocol.summonSpellRequest(payload)",
                "permitsControlSelection(", "directGrade(casterLevel)",
                "allowsPetRecall()", "removeCatalog(",
                "spellReadyNanos.put(spell.skillId()",
                "cancelMeditation(", "cancelAbsoluteBarrier(",
                "revealInvisibility(", "applySpellLawfulDelta(",
                "summon committed without summon:",
                "S_MESSAGE_CODE summon-map=", "capacity rejected",
                "BattleCompanion.summoned(", "publishSummonedCompanion("
        }) assertTrue(method.contains(anchor), () -> "missing consumer " + anchor);
        assertFalse(method.contains("commitSpellReady(spell.skillId()"));

        int commit = method.indexOf("spellReadyNanos.put(spell.skillId()");
        int meditation = method.indexOf("cancelMeditation(", commit);
        int barrier = method.indexOf("cancelAbsoluteBarrier(", meditation);
        int invisible = method.indexOf("revealInvisibility(", barrier);
        int action = method.indexOf("Protocol.objectAction(", invisible);
        int lawful = method.indexOf("applySpellLawfulDelta(", action);
        int mapResult = method.indexOf("if (!mapAllowsPetRecall)", lawful);
        int capacity = method.indexOf("capacity rejected", mapResult);
        assertTrue(commit >= 0 && meditation > commit && barrier > meditation
                && invisible > barrier && action > invisible && lawful > action
                && mapResult > lawful && capacity > mapResult);
        assertEquals(-1, method.substring(0, commit)
                .indexOf("if (!mapAllowsPetRecall)"));
    }
}
