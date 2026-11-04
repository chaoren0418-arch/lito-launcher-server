package local.lito381.server;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/** Locks native skill98 and its ordinary-WORLD player/monster poison paths. */
final class NativeWorldEnchantVenomSourceAuditTest {
    @BeforeAll
    static void assumeLocalResourcesPresent() {
        LocalResources.assumeLineageSources();
        LocalResources.assumeIdaClient();
    }

    private static final Path RETAINED = Path.of(
            "../1-伺服器/src/com/lineage/server");

    @Test
    void nativeProducerDatabaseAndRetainedPoisonRulesStayExact()
            throws Exception {
        String database = Files.readString(
                Path.of("../1-伺服器/db/380_db.sql"),
                StandardCharsets.UTF_8);
        assertTrue(database.contains(
                "INSERT INTO `skills` VALUES (98, 0, '附加劇毒', 13, 1, 10, 1, 40321, 1, 0, 320, 'none', 0, 0, 0, 0, 0, 0, 0, 2, 0, 0, 0, 0, 2, '$2505', 19, 2942, 0, 814, 827, 0);"));
        assertTrue(database.contains(
                "INSERT INTO `skills_item` VALUES (98, '附加劇毒', NULL, NULL);"));

        String assembly = Files.readString(Path.of(
                "../LiTo381-開發/ida-client-oep-v2/full-disassembly.asm"),
                StandardCharsets.UTF_8);
        assertTrue(assembly.contains(
                "jumptable 004CB66E cases 1,2,12,13,30,31,48,51,53,71,77,81,96-98"));
        int producer = assembly.indexOf("loc_4CBBD4:");
        int producerEnd = assembly.indexOf("loc_4CBBED:", producer);
        String route = assembly.substring(producer, producerEnd);
        assertTrue(route.contains("push    14h"));
        assertTrue(route.contains("push    offset off_5028CC"));

        String attack = Files.readString(RETAINED.resolve(
                "model/L1AttackPc.java"), StandardCharsets.UTF_8);
        assertTrue(attack.contains("_pc.hasSkillEffect(ENCHANT_VENOM)"));
        assertTrue(attack.contains("if (chance <= 10)"));
        assertTrue(attack.contains(
                "L1DamagePoison.doInfection(_pc, target, 3000, 5)"));
        String poison = Files.readString(RETAINED.resolve(
                "model/poison/L1DamagePoison.java"), StandardCharsets.UTF_8);
        assertTrue(poison.contains("_target.setSkillEffect(STATUS_POISON, 30000)"));
        assertTrue(poison.contains(
                "pool.pcScheduleAtFixedRate(tick, _damageSpan, _damageSpan)"));
    }

    @Test
    void worldCastPlayerAndOrdinaryMonsterProcStayConnected()
            throws Exception {
        String session = Files.readString(Path.of(
                "src/main/java/local/lito381/server/ClientSession.java"),
                StandardCharsets.UTF_8);
        int worldStart = session.indexOf("if (phase == Phase.WORLD) {");
        int dispatch = session.indexOf(
                "BattleSpellCatalog.EnchantVenomSpell worldEnchantVenom",
                worldStart);
        int unsupported = session.indexOf(
                "rejected unsupported classic-world skill", dispatch);
        assertTrue(dispatch > worldStart && unsupported > dispatch);
        assertTrue(session.substring(dispatch, unsupported).contains(
                "useEnchantVenomSpell(payload, worldEnchantVenom)"));

        int castStart = session.indexOf("private void useEnchantVenomSpell(");
        int castEnd = session.indexOf(
                "private void useBringStoneSpell(", castStart);
        String cast = session.substring(castStart, castEnd);
        assertTrue(cast.contains("Protocol.selectorOnlySpellRequest(payload)"));
        assertTrue(cast.contains("boolean validWorld = originPhase == Phase.WORLD"));
        assertTrue(cast.contains("venomEnchantStatus.activate("));
        assertTrue(cast.contains("nextInventory.removeCatalog("));
        assertTrue(cast.contains("commitSpellReady(spell.skillId()"));
        assertTrue(cast.contains("classicWorldMembers(originMapId)"));
        assertTrue(cast.contains("member.classicWorldInstanceId != originInstanceId"));

        int monsterStart = session.indexOf("private void attackClassicWorld(");
        int playerStart = session.indexOf(
                "private void attackClassicWorldPlayer(", monsterStart);
        String monster = session.substring(monsterStart, playerStart);
        int monsterProc = monster.indexOf("attemptEnchantVenomPoison(");
        int monsterDamageGate = monster.indexOf("if (damage <= 0) return;");
        assertTrue(monsterProc > 0 && monsterProc < monsterDamageGate);
        assertTrue(monster.contains("null, null, null, target,"));

        int playerEnd = session.indexOf(
                "private ClassicWorldProjectileDebit", playerStart);
        String player = session.substring(playerStart, playerEnd);
        int playerProc = player.indexOf("attemptEnchantVenomPoison(");
        int playerDamageGate = player.indexOf("if (damage > 0)");
        assertTrue(playerProc > 0 && playerProc < playerDamageGate);
        assertTrue(player.contains("target, null, null, null,"));

        int procStart = session.indexOf(
                "private void attemptEnchantVenomPoison(");
        int procEnd = session.indexOf(
                "private void takePlayerDamage(", procStart);
        String proc = session.substring(procStart, procEnd);
        assertTrue(proc.contains("boolean sameWorld = phase == Phase.WORLD"));
        assertTrue(proc.contains("hub.poisonClassicWorldMonster("));
        assertTrue(proc.contains("hub.provokeClassicWorldMonster("));
        assertTrue(proc.contains("classic-world-monster="));
        assertTrue(proc.contains("reason=Enchant-Venom-proc"));
    }
}
