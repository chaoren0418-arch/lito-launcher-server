package local.lito381.server;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

/** Locks selected skill 50 and its single-target BATTLE/WORLD freeze paths. */
final class NativeWorldIceLanceSourceAuditTest {
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
    void selectedDatabaseClientProducerAndCatalogRemainDistinct() throws Exception {
        String selected = source(Path.of("../1-伺服器/db/380_db.sql"));
        assertTrue(selected.contains(
                "INSERT INTO `skills` VALUES (50, 0, '冰矛圍籬', 7, 1, 100, 0, 0, 0, 500, 16, 'attack', 3, 40, 5, 6, 0, 4, 4, 64, 0, 6, 0, 0, 2, '$1467', 18, 756, 0, 0, 0, 0);"));
        assertTrue(selected.contains(
                "INSERT INTO `skills_負面技能控制` VALUES (50, '冰矛圍籬-寒冰耐性', 55, 45, 35, 0, 0, 0.4, 1, 0, 1, 0.05, 0, 0);"));
        assertTrue(selected.contains(
                "INSERT INTO `skills_負面boss免疫` VALUES (50, '冰矛圍籬', 0);"));
        assertTrue(selected.contains(
                "INSERT INTO `skills_item` VALUES (50, '冰矛圍籬', NULL, NULL);"));

        String assembly = source(Path.of(
                "../LiTo381-開發/ida-client-oep-v2/full-disassembly.asm"));
        String route = between(assembly, "loc_4CB755:", "loc_4CB78B:");
        assertTrue(route.contains(
                "jumptable 004CB66E cases 3,5,6,17,28,37,44,49"));
        assertTrue(route.contains("mov     edx, 0Ah"));
        assertTrue(route.contains("call    sub_4CA6E0"));
        String producer = between(
                assembly, "sub_4CA6E0 proc near", "sub_4CA6E0 endp");
        assertTrue(producer.contains("\"cccdhh\""));

        BattleSpellCatalog.FreezeAttackSpell spell =
                BattleSpellCatalog.freezeAttackSpell(
                        BattleSpellCatalog.ICE_LANCE);
        assertEquals(BattleSpellCatalog.RequestShape.OBJECT_TARGET,
                spell.requestShape());
        assertEquals(6, spell.maximumRange());
        assertEquals(500, spell.reuseDelayMillis());
        assertEquals(1_000, spell.clientCastClockMillis());
        assertEquals(100, spell.mpCost());
        assertEquals(16, spell.durationSeconds());
        assertEquals(18, spell.actionId());
        assertEquals(756, spell.effectGfx());
    }

    @Test
    void dispatcherAndBothPhasesTargetPlayersCompanionsAndMonsters()
            throws Exception {
        String session = source(Path.of(
                "src/main/java/local/lito381/server/ClientSession.java"));
        String worldDispatch = between(session,
                "BattleSpellCatalog.FreezeAttackSpell worldFreeze",
                "BattleSpellCatalog.ShockStunSpell worldShockStun");
        assertTrue(worldDispatch.contains(
                "useClassicWorldFreezeAttackSpell(payload, worldFreeze)"));

        String world = between(session,
                "private void useClassicWorldFreezeAttackSpell(",
                "private void useFreezeAttackSpell(");
        for (String anchor : new String[] {
                "Protocol.attackSpellRequest(payload)",
                "admitClassicWorldSpellFamily(",
                "classicWorldCompanionSnapshots()",
                "hub.classicWorldMonster(",
                "map.attackPosition(",
                "freezeActive(requestNow)",
                "classicWorldMonsterFreezeActive(",
                "spellReadyNanos.put(spell.skillId()",
                "cancelMeditation(",
                "cancelAbsoluteBarrier(",
                "revealInvisibility(",
                "consumeCounterMagic(castNow)",
                "captureEraseMagic(castNow)",
                "totalFreezeResistance()",
                "counterMirrorDecision(",
                "takeClassicWorldPlayerDamage(",
                "applyClassicWorldMonsterCompanionDamage(",
                "hub.damageClassicWorldMonster(",
                "controlledCompanion.activateFreeze(",
                "hub.freezeClassicWorldMonster(",
                "Protocol.ParalysisMode.FREEZE_APPLY",
                "ignored stale classic-world"
        }) assertTrue(world.contains(anchor), () -> "missing WORLD " + anchor);
        int commit = world.indexOf("spellReadyNanos.put(spell.skillId()");
        assertTrue(commit < world.indexOf("cancelMeditation(", commit));
        assertTrue(world.indexOf("cancelMeditation(", commit)
                < world.indexOf("cancelAbsoluteBarrier(", commit));
        assertTrue(world.indexOf("cancelAbsoluteBarrier(", commit)
                < world.indexOf("revealInvisibility(", commit));

        String battle = between(session,
                "private void useFreezeAttackSpell(",
                "private void useShockStunSpell(");
        for (String anchor : new String[] {
                "controlledCompanionSnapshots()",
                "controlledCompanion.effectiveMagicResistance(",
                "controlledCompanion.captureEraseMagic(castNow)",
                "applyBattleCompanionFeedbackDamage(",
                "controlledCompanion.activateFreeze(",
                "spellReadyNanos.put(spell.skillId()"
        }) assertTrue(battle.contains(anchor), () -> "missing BATTLE " + anchor);
        int battleCommit = battle.indexOf("spellReadyNanos.put(spell.skillId()");
        assertTrue(battleCommit < battle.indexOf("cancelMeditation(", battleCommit));
        assertTrue(battle.indexOf("cancelMeditation(", battleCommit)
                < battle.indexOf("cancelAbsoluteBarrier(", battleCommit));
        assertTrue(battle.indexOf("cancelAbsoluteBarrier(", battleCommit)
                < battle.indexOf("revealInvisibility(", battleCommit));
    }
}
