package local.lito381.server;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/** Locks native skill104 and both ordinary-WORLD player/monster poison gates. */
final class NativeWorldVenomResistSourceAuditTest {
    @BeforeAll
    static void assumeLocalResourcesPresent() {
        LocalResources.assumeLineageSources();
        LocalResources.assumeIdaClient(LocalResources.IDA_DISASSEMBLY);
    }

    private static final Path RETAINED = Path.of(
            "../1-伺服器/src/com/lineage/server");

    @Test
    void nativeProducerDatabaseAndRetainedAdmissionOnlyRuleStayExact()
            throws Exception {
        String database = Files.readString(
                Path.of("../1-伺服器/db/380_db.sql"),
                StandardCharsets.UTF_8);
        assertTrue(database.contains(
                "INSERT INTO `skills` VALUES (104, 0, '毒性抵抗', 13, 7, 20, 0, 0, 0, 0, 320, 'none', 0, 0, 0, 0, 0, 0, 0, 2, 0, 0, 0, 0, 128, '$2511', 19, 2948, 0, 0, 831, 0);"));
        assertTrue(database.contains(
                "INSERT INTO `skills_item` VALUES (104, '毒性抵抗', NULL, NULL);"));

        String assembly = Files.readString(Path.of(
                "../LiTo381-開發/ida-client-oep-v2/full-disassembly.asm"),
                StandardCharsets.UTF_8);
        int producer = assembly.indexOf("loc_4CBBD4:");
        int producerEnd = assembly.indexOf("loc_4CBBED:", producer);
        String route = assembly.substring(producer, producerEnd);
        assertTrue(route.contains("and     ecx, 7"));
        assertTrue(route.contains("sar     esi, 3"));
        assertTrue(route.contains("push    14h"));
        assertTrue(route.contains("push    offset off_5028CC"));

        String poisonGate = Files.readString(RETAINED.resolve(
                "model/poison/L1Poison.java"), StandardCharsets.UTF_8);
        assertTrue(poisonGate.contains("get_venom_resist() > 0"));
        assertTrue(poisonGate.contains("hasSkillEffect(L1SkillId.VENOM_RESIST)"));
        assertTrue(poisonGate.contains("getPoisonResist() > 0"));
        for (String family : new String[] {
                "L1DamagePoison.java", "L1SilencePoison.java",
                "L1ParalysisPoison.java"}) {
            String source = Files.readString(RETAINED.resolve(
                    "model/poison/" + family), StandardCharsets.UTF_8);
            assertTrue(source.contains("isValidTarget"));
        }
        String use = Files.readString(RETAINED.resolve(
                "model/skill/L1SkillUse.java"), StandardCharsets.UTF_8);
        assertTrue(use.contains(
                "cha.setSkillEffect(getEffectSkillId(), _getBuffDuration)"));
        assertTrue(!use.contains("case VENOM_RESIST"));
    }

    @Test
    void worldCastExpiryAndPlayerMonsterPoisonGatesStayConnected()
            throws Exception {
        String session = Files.readString(Path.of(
                "src/main/java/local/lito381/server/ClientSession.java"),
                StandardCharsets.UTF_8);
        int worldStart = session.indexOf("if (phase == Phase.WORLD) {");
        int dispatch = session.indexOf(
                "BattleSpellCatalog.PoisonResistanceSpell worldPoisonResistance",
                worldStart);
        int unsupported = session.indexOf(
                "rejected unsupported classic-world skill", dispatch);
        assertTrue(dispatch > worldStart && unsupported > dispatch);
        assertTrue(session.substring(dispatch, unsupported).contains(
                "usePoisonResistanceSpell(payload, worldPoisonResistance)"));

        int castStart = session.indexOf(
                "private void usePoisonResistanceSpell(");
        int castEnd = session.indexOf(
                "private void useMagicResistanceSpell(", castStart);
        String cast = session.substring(castStart, castEnd);
        assertTrue(cast.contains("Protocol.selectorOnlySpellRequest(payload)"));
        assertTrue(cast.contains(
                "originPhase != Phase.BATTLE && originPhase != Phase.WORLD"));
        assertTrue(cast.contains("timedStatus.activateVenomResist("));
        assertTrue(cast.contains("classicWorldMembers(originMapId)"));
        assertTrue(cast.contains("member.classicWorldInstanceId != originInstanceId"));
        assertTrue(cast.contains("BattleSpellCatalog.isInNativeScreen("));
        assertTrue(cast.contains("revealInvisibility("));
        assertTrue(cast.contains("cancelAbsoluteBarrier("));

        assertTrue(session.contains(
                "if (timedStatus.expireVenomResist(nowNanos)\n"
                + "                && (inBattle || inClassicWorld))"));
        assertTrue(session.contains(
                "S_MESSAGE_CODE Venom-Resist-expired="));

        int monsterStart = session.indexOf(
                "private void applyClassicWorldMonsterPoison(");
        int monsterEnd = session.indexOf(
                "void serverClassicWorldMonsterMagicAttack(", monsterStart);
        String monster = session.substring(monsterStart, monsterEnd);
        assertTrue(monster.contains("timedStatus.acceptsPoison(nowNanos"));
        assertTrue(monster.contains("case DAMAGE -> BattleTimedStatus.PoisonKind.DAMAGE"));
        assertTrue(monster.contains("case SILENCE -> BattleTimedStatus.PoisonKind.SILENCE"));
        assertTrue(monster.contains(
                "case PARALYSIS -> BattleTimedStatus.PoisonKind.PARALYSIS"));

        int procStart = session.indexOf(
                "private void attemptEnchantVenomPoison(");
        int procEnd = session.indexOf(
                "private void takePlayerDamage(", procStart);
        String player = session.substring(procStart, procEnd);
        assertTrue(player.contains("target.timedStatus.acceptsPoison("));
        assertTrue(player.contains("BattleTimedStatus.PoisonKind.DAMAGE"));
    }
}
