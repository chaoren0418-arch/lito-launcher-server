package local.lito381.server;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

/** Locks native skill106 and both ordinary-WORLD physical defense paths. */
final class NativeWorldUncannyDodgeSourceAuditTest {
    private static final Path RETAINED = Path.of("../1-伺服器");

    @Test
    void nativeProducerDatabaseAndRetainedLeaseStayExact() throws Exception {
        String assembly = Files.readString(Path.of(
                "../LiTo381-開發/ida-client-oep-v2/full-disassembly.asm"),
                StandardCharsets.UTF_8);
        assertTrue(assembly.contains(
                "jumptable 004CB66E cases 1,2,12,13,30,31,48,51,53,71,77,81,96-98,100,101,103-105"));
        int producer = assembly.indexOf("loc_4CBBD4:");
        int producerEnd = assembly.indexOf("loc_4CBBED:", producer);
        String route = assembly.substring(producer, producerEnd);
        assertTrue(route.contains("and     ecx, 7"));
        assertTrue(route.contains("sar     esi, 3"));
        assertTrue(route.contains("push    14h"));
        assertTrue(route.contains("push    offset off_5028CC"));

        String database = Files.readString(
                RETAINED.resolve("db/380_db.sql"), StandardCharsets.UTF_8);
        assertTrue(database.contains(
                "INSERT INTO `skills` VALUES (106, 0, '暗影閃避', 14, 1, 20, 0, 0, 0, 0, 192, 'none', 0, 0, 0, 0, 0, 0, 0, 2, 0, 0, 0, 0, 2, '$2513', 19, 11765, 0, 0, 0, 0);"));
        assertTrue(database.contains(
                "INSERT INTO `skills_item` VALUES (106, '暗影閃避', NULL, NULL);"));

        String mode = Files.readString(RETAINED.resolve(
                "src/com/lineage/server/model/skill/skillmode/UNCANNY_DODGE.java"),
                StandardCharsets.UTF_8);
        assertTrue(mode.contains("if (!srcpc.hasSkillEffect(UNCANNY_DODGE))"));
        assertTrue(mode.contains(
                "srcpc.setSkillEffect(UNCANNY_DODGE, integer * 1000)"));
    }

    @Test
    void worldDispatchPlayerMonsterHitAndExpiryStayConnected()
            throws Exception {
        String session = Files.readString(Path.of(
                "src/main/java/local/lito381/server/ClientSession.java"),
                StandardCharsets.UTF_8);
        int worldStart = session.indexOf("if (phase == Phase.WORLD) {");
        int dispatch = session.indexOf(
                "BattleSpellCatalog.DodgeBuffSpell worldDodgeBuff", worldStart);
        int unsupported = session.indexOf(
                "rejected unsupported classic-world skill", dispatch);
        String worldRoute = session.substring(dispatch, unsupported);
        assertTrue(worldRoute.contains(
                "BattleSpellCatalog.dodgeBuffSpell(selector.skillId())"));
        assertTrue(worldRoute.contains(
                "useDodgeBuffSpell(payload, worldDodgeBuff)"));

        int castStart = session.indexOf("private void useDodgeBuffSpell(");
        int castEnd = session.indexOf(
                "private void useRangedEvasionBuffSpell(", castStart);
        String cast = session.substring(castStart, castEnd);
        assertTrue(cast.contains(
                "originPhase != Phase.BATTLE && originPhase != Phase.WORLD"));
        assertTrue(cast.contains("dodge-buff-polymorph-rejected"));
        assertTrue(cast.contains("dodge-buff-Silence-rejected"));
        assertTrue(cast.contains("dodge-buff-overweight=316"));
        assertTrue(cast.contains("rejected unlearned dodge buff"));
        assertTrue(cast.contains("abilityStatus.activateUncannyDodge("));
        assertTrue(cast.contains("active-recast="));
        assertTrue(cast.contains("lease-refreshed=false"));
        assertTrue(cast.contains("classicWorldMembers(originMapId)"));
        assertTrue(cast.contains("member.classicWorldInstanceId != originInstanceId"));
        assertTrue(cast.contains("BattleSpellCatalog.isInNativeScreen("));
        assertTrue(cast.contains("revealInvisibility("));
        assertTrue(cast.contains("cancelAbsoluteBarrier("));
        assertTrue(cast.contains("phase=\" + originPhase + \" persisted=false"));

        int playerStart = session.indexOf(
                "private void attackClassicWorldPlayer(");
        int playerEnd = session.indexOf(
                "private ClassicWorldProjectileDebit", playerStart);
        String player = session.substring(playerStart, playerEnd);
        assertTrue(player.contains("target.effectivePhysicalDodge(nowNanos)"));
        assertTrue(player.contains("target-dodge-modifier="));

        int monsterStart = session.indexOf(
                "void serverClassicWorldMonsterAttack(");
        int monsterEnd = session.indexOf(
                "private void commitClassicWorldMonsterCounterBarrier(",
                monsterStart);
        String monster = session.substring(monsterStart, monsterEnd);
        assertTrue(monster.contains("effectivePhysicalDodge(attackNowNanos)"));
        assertTrue(monster.contains("BattleNpcPhysicalAttack.hitChance("));
        assertTrue(monster.contains("dodge=\" + dodgeModifier"));

        assertTrue(session.contains(
                "abilityStatus.expireUncannyDodge(nowNanos)\n"
                        + "                && (inBattle || inClassicWorld)"));
    }

    @Test
    void hiddenWorldPlayersCanRemainProvokedMonsterSkillTargets()
            throws Exception {
        String session = Files.readString(Path.of(
                "src/main/java/local/lito381/server/ClientSession.java"),
                StandardCharsets.UTF_8);
        assertTrue(session.contains("private volatile boolean invisible;"));

        int invisibilityStart = session.indexOf(
                "private void useInvisibilitySpell(");
        int invisibilityEnd = session.indexOf(
                "private void useEnchantVenomSpell(", invisibilityStart);
        String invisibility = session.substring(invisibilityStart, invisibilityEnd);
        assertTrue(invisibility.contains(
                "hub.clearClassicWorldMonsterTargets(playerObjectId())"));

        for (String method : new String[] {
                "void serverClassicWorldMonsterMagicAttack(",
                "void serverClassicWorldMonsterStatusSkillAttack(\n"
                        + "            ClassicWorldMonsterPopulation.StatusSkillAttack attack,\n"
                        + "            int probabilityRollFixture)",
                "void serverClassicWorldMonsterPhysicalSkillAttack("
        }) {
            int start = session.indexOf(method);
            assertTrue(start >= 0, method);
            int gate = session.indexOf(
                    "!classicWorldMonsterRetainedTargetEligible(actor.mapId())", start);
            assertTrue(gate >= start && gate - start < 900, method);
        }
    }
}
