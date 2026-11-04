package local.lito381.server;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/** Locks native skill102 and its retained ordinary-WORLD damage route. */
final class NativeWorldBurningSpiritSourceAuditTest {
    @BeforeAll
    static void assumeLocalResourcesPresent() {
        LocalResources.assumeLineageSources();
        LocalResources.assumeIdaClient(LocalResources.IDA_DISASSEMBLY);
    }

    private static final Path RETAINED = Path.of("../1-伺服器");

    @Test
    void nativeProducerDatabaseAndRetainedMultiplierRulesStayExact()
            throws Exception {
        String assembly = Files.readString(Path.of(
                "../LiTo381-開發/ida-client-oep-v2/full-disassembly.asm"),
                StandardCharsets.UTF_8);
        assertTrue(assembly.contains(
                "jumptable 004CB66E cases 1,2,12,13,30,31,48,51,53,71,77,81,96-98,100,101"));
        int producer = assembly.indexOf("loc_4CBBD4:");
        int producerEnd = assembly.indexOf("loc_4CBBED:", producer);
        String route = assembly.substring(producer, producerEnd);
        assertTrue(route.contains("push    14h"));
        assertTrue(route.contains("push    offset off_5028CC"));
        assertTrue(assembly.contains("off_5028CC dd offset byte_636363"));

        String database = Files.readString(
                RETAINED.resolve("db/380_db.sql"), StandardCharsets.UTF_8);
        assertTrue(database.contains(
                "INSERT INTO `skills` VALUES (102, 0, '燃燒鬥志', 13, 5, 20, 0, 0, 0, 2990, 320, 'none', 0, 0, 0, 0, 0, 0, 0, 2, 0, 0, 0, 0, 32, '$2509', 19, 2946, 0, 0, 0, 0);"));
        assertTrue(database.contains(
                "INSERT INTO `skills_item` VALUES (102, '燃燒鬥志', NULL, NULL);"));

        String attack = Files.readString(RETAINED.resolve(
                "src/com/lineage/server/model/L1AttackPc.java"),
                StandardCharsets.UTF_8);
        assertTrue(attack.contains("if (_weaponType == 20)"));
        assertTrue(attack.contains("if (_weaponType == 62)"));
        assertTrue(attack.contains("if (_weaponType2 == 17)"));
        assertTrue(attack.contains("hasSkillEffect(BURNING_SPIRIT)"));
        assertTrue(attack.contains("random <= ConfigSkill.BURNING_CHANCE"));
        assertTrue(attack.contains(
                "Math.max(_damageMultiplier, ConfigSkill.BURNING_DMG)"));
        assertTrue(attack.contains("dmg *= _damageMultiplier"));
    }

    @Test
    void worldDispatchSharedRollMaximumFamilyAndExpiryStayConnected()
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
        assertTrue(worldRoute.contains("BattleSpellCatalog.BURNING_SPIRIT"));
        assertTrue(worldRoute.contains(
                "usePhysicalProcBuffSpell(payload, worldPhysicalProcBuff)"));

        int helperStart = session.indexOf(
                "private ClassicWorldPhysicalProc classicWorldPhysicalProc(");
        int helperEnd = session.indexOf("private void attack(", helperStart);
        String helper = session.substring(helperStart, helperEnd);
        assertTrue(helper.contains(
                "weapon.weaponType() == BattleKiringkuDamage.WEAPON_TYPE"));
        assertTrue(helper.contains(
                "physicalHit && !ranged && !kiringkuAttack"));
        assertTrue(helper.contains("config.burningSpiritProcFixtureRoll()"));
        assertTrue(helper.contains("BattlePhysicalProcBuff.burningSpirit("));
        assertTrue(helper.contains("BattlePhysicalProcBuff.elementalFire("));
        assertTrue(helper.contains("BattlePhysicalProcBuff.doubleBrake("));
        assertTrue(helper.contains(
                "BattlePhysicalProcBuff.soulOfFlameMultiplier("));

        int monsterStart = session.indexOf("private void attackClassicWorld(");
        int playerStart = session.indexOf(
                "private void attackClassicWorldPlayer(", monsterStart);
        int playerEnd = session.indexOf(
                "private ClassicWorldProjectileDebit", playerStart);
        String monster = session.substring(monsterStart, playerStart);
        String player = session.substring(playerStart, playerEnd);
        for (String route : new String[] {monster, player}) {
            assertTrue(route.contains("classicWorldPhysicalProc("));
            assertTrue(route.contains("BattleWeaponCritical.apply("));
            assertTrue(route.contains("physicalProc.multiplier()"));
            assertTrue(route.contains("burning-spirit-triggered="));
            assertTrue(route.contains("double-damage-multiplier="));
        }

        assertTrue(session.contains(
                "timedStatus.expireBurningSpirit(nowNanos)\n"
                        + "                && (inBattle || inClassicWorld)"));
    }
}
