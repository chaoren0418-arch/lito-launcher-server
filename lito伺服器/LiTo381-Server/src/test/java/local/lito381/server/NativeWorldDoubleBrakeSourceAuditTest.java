package local.lito381.server;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/** Locks native skill105 and its ordinary-WORLD player/monster damage paths. */
final class NativeWorldDoubleBrakeSourceAuditTest {
    @BeforeAll
    static void assumeLocalResourcesPresent() {
        LocalResources.assumeLineageSources();
        LocalResources.assumeIdaClient();
    }

    private static final Path RETAINED = Path.of("../1-伺服器");

    @Test
    void nativeProducerDatabaseAndRetainedMultiplierRulesStayExact()
            throws Exception {
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
                "INSERT INTO `skills` VALUES (105, 0, '雙重破壞', 14, 0, 30, 0, 40321, 1, 0, 192, 'none', 0, 0, 0, 0, 0, 0, 0, 2, 0, 0, 0, 0, 1, '$2512', 19, 2949, 0, 823, 829, 0);"));
        assertTrue(database.contains(
                "INSERT INTO `skills_item` VALUES (105, '雙重破壞', NULL, NULL);"));

        String attack = Files.readString(RETAINED.resolve(
                "src/com/lineage/server/model/L1AttackPc.java"),
                StandardCharsets.UTF_8);
        assertTrue(attack.contains("case 11:// 鋼爪"));
        assertTrue(attack.contains("case 12:// 雙刀"));
        assertTrue(attack.contains("hasSkillEffect(DOUBLE_BRAKE)"));
        assertTrue(attack.contains("<= ConfigSkill.DOUBLE_BREAK_CHANCE"));
        assertTrue(attack.contains(
                "Math.max(_damageMultiplier, ConfigSkill.DOUBLE_BREAK_DMG)"));
        assertTrue(attack.contains("dmg *= _damageMultiplier"));
    }

    @Test
    void worldDispatchMaximumFamilyAndExpiryStayConnected()
            throws Exception {
        String session = Files.readString(Path.of(
                "src/main/java/local/lito381/server/ClientSession.java"),
                StandardCharsets.UTF_8);
        int worldStart = session.indexOf("if (phase == Phase.WORLD) {");
        int dispatch = session.indexOf(
                "BattleSpellCatalog.PhysicalProcBuffSpell worldPhysicalProcBuff",
                worldStart);
        int unsupported = session.indexOf(
                "rejected unsupported classic-world skill", dispatch);
        String worldRoute = session.substring(dispatch, unsupported);
        assertTrue(worldRoute.contains("BattleSpellCatalog.DOUBLE_BRAKE"));
        assertTrue(worldRoute.contains(
                "usePhysicalProcBuffSpell(payload, worldPhysicalProcBuff)"));

        int castStart = session.indexOf(
                "private void usePhysicalProcBuffSpell(");
        int castEnd = session.indexOf(
                "private void useOtherNoEffectSpell(", castStart);
        String cast = session.substring(castStart, castEnd);
        assertTrue(cast.contains(
                "originPhase != Phase.BATTLE && originPhase != Phase.WORLD"));
        assertTrue(cast.contains("nextInventory.removeCatalog("));
        assertTrue(cast.contains(
                "spell.itemConsumeId(), spell.itemConsumeCount()"));
        assertTrue(cast.contains("timedStatus.activateDoubleBrake("));
        assertTrue(cast.contains("classicWorldMembers(originMapId)"));
        assertTrue(cast.contains("member.classicWorldInstanceId != originInstanceId"));
        assertTrue(cast.contains("revealInvisibility("));
        assertTrue(cast.contains("cancelAbsoluteBarrier("));

        int helperStart = session.indexOf(
                "private ClassicWorldPhysicalProc classicWorldPhysicalProc(");
        int helperEnd = session.indexOf("private void attack(", helperStart);
        String helper = session.substring(helperStart, helperEnd);
        assertTrue(helper.contains("config.doubleBrakeProcFixtureRoll()"));
        assertTrue(helper.contains("BattlePhysicalProcBuff.doubleBrake("));
        assertTrue(helper.contains("Math.max("));

        int monsterStart = session.indexOf("private void attackClassicWorld(");
        int playerStart = session.indexOf(
                "private void attackClassicWorldPlayer(", monsterStart);
        int playerEnd = session.indexOf(
                "private ClassicWorldProjectileDebit", playerStart);
        for (String route : new String[] {
                session.substring(monsterStart, playerStart),
                session.substring(playerStart, playerEnd)}) {
            assertTrue(route.contains("classicWorldPhysicalProc("));
            assertTrue(route.contains("BattleWeaponCritical.apply("));
            assertTrue(route.contains("physicalProc.multiplier()"));
            assertTrue(route.contains("double-brake-triggered="));
            assertTrue(route.contains("double-damage-multiplier="));
        }

        assertTrue(session.contains(
                "timedStatus.expireDoubleBrake(nowNanos)\n"
                        + "                && (inBattle || inClassicWorld)"));
    }
}
